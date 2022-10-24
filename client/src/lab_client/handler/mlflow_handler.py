import os

import requests
from contaxy.schema import Extension


class MLFlowHandler:
    def __init__(self, env, extension_client):
        # Initialize variables
        self.env = env
        self._extension_client = extension_client

    def setup_mlflow(self) -> None:
        """
        Sets up the MLflow environment.

        * Uses the contaxy API to check if the ML Flow extension is installed
        * Uses the ML Flow extension API to check if the ML Flow server is running (and if not, starts it)
        * Sets the tracking URI and token environment variables
        """
        extension_display_name = "MLflow Experiments"
        mlflow_extension = self._get_extension(extension_display_name)
        tracking_uri = self._get_mlflow_tracking_uri(mlflow_extension)
        self._set_mlflow_env_vars(tracking_uri=tracking_uri, token=self.env.lab_api_token)

    def _get_extension(self, target_extension: str) -> Extension:
        global_project_name = "ctxy-global"
        all_extensions = self._extension_client.list_extensions(global_project_name)
        for extension in all_extensions:
            if extension.display_name == target_extension:
                if extension.status != "running":
                    raise ExtensionNotRunningError("MLflow extension is installed but not running.")
                return extension
        raise ExtensionNotInstalledError(
            "ML Flow extension is not installed on this ML Lab instance.")

    def _get_mlflow_server(self, extension: Extension) -> dict:
        endpoint = self._get_mlflow_backend_api_endpoint(
            extension) + "/projects/" + self.env.project + "/mlflow-server"
        response = requests.get(endpoint, headers={"Authorization": "Bearer " + self.env.lab_api_token})
        mlflow_server_list = response.json()
        return mlflow_server_list[0] if len(mlflow_server_list) > 0 else None

    def _create_mlflow_server(self, extension: Extension) -> dict:
        endpoint = self._get_mlflow_backend_api_endpoint(
            extension) + "/projects/" + self.env.project + "/mlflow-server"
        body = {"is_stopped": False}
        response = requests.post(
            endpoint, json=body, headers={"Authorization": "Bearer " + self.env.lab_api_token})
        return response.json()

    def _start_mlflow_server(self, extension: Extension, server_id: str) -> None:
        endpoint = self._get_mlflow_backend_api_endpoint(
            extension) + "/projects/" + self.env.project + "/mlflow-server/" + server_id + "/start"
        requests.post(endpoint, headers={
            "Authorization": "Bearer " + self.env.lab_api_token})

    def _get_mlflow_backend_api_endpoint(self, extension: Extension) -> str:
        base_url = self.env.lab_endpoint.replace("/api", "")
        return base_url + extension.api_extension_endpoint

    def _get_mlflow_tracking_uri(self, extension: Extension) -> str:
        endpoint = self._get_mlflow_backend_api_endpoint(
            extension) + "/projects/" + self.env.project + "/mlflow-server"
        response = requests.get(
            endpoint, headers={"Authorization": "Bearer " + self.env.lab_api_token}).json()

        if len(response) == 0:
            mlflow_server = self._create_mlflow_server(extension)
        else:
            mlflow_server = response[0]

        if mlflow_server["status"] == "stopped":
            self._start_mlflow_server(extension, mlflow_server["id"])

        base_url = self.env.lab_endpoint.replace("/api", "")
        # [:-2] removes trailing slash and 'b' from the url. Removing 'b' uses nginx to talk to the api with the full path
        return base_url + mlflow_server["access_url"][:-2]

    def _set_mlflow_env_vars(self, tracking_uri: str, token: str) -> None:
        self._set_mlflow_tracking_uri_env_var(tracking_uri)
        self._set_mlflow_token_env_var(token)

    def _set_mlflow_tracking_uri_env_var(self, tracking_uri: str) -> None:
        os.environ["MLFLOW_TRACKING_URI"] = tracking_uri

    def _set_mlflow_token_env_var(self, token: str) -> None:
        os.environ["MLFLOW_TRACKING_TOKEN"] = token


class ExtensionNotInstalledError(Exception):
    """
    Exception raised when the ML Flow extension is not installed on the Lab instance.
    """

    def __init__(self, message):
        super().__init__(message)


class ExtensionNotRunningError(Exception):
    """
    Exception raised when the MLflow extension is installed but not running
    """

    def __init__(self, message):
        super().__init__(message)

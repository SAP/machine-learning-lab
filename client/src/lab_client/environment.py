import os
import requests
from typing import Optional

from contaxy.clients import AuthClient, FileClient, ExtensionClient
from contaxy.clients.shared import BaseUrlSession
from contaxy.config import API_TOKEN_NAME
from contaxy.schema.extension import Extension

from .handler.file_handler import FileHandler
from .utils.text_utils import simplify


class Environment:
    """
    Initialize environment from a Lab instance. The Environment manages all files (models & datasets), services, experiments
    and provides access to the Lab API. Locally, it has a dedicated folder structure to save models, datasets, analysis and experiment data.

    # Arguments
        project (string): Selected project (optional).
        root_folder (string): Root folder of the environment. If not provided, it will use `DATA_ENVIRONMENT` value
         as the root folder. If `temp`, a temporary folder will be used as root folder and removed on exit (optional).
        lab_endpoint (string): Endpoint URL of a Lab instance (optional).
        lab_api_token (string): API Token for accessing the selected Lab instance (optional).
    """

    _ENV_NAME_ENV_ROOT_PATH = "DATA_ENVIRONMENT"
    _TEMP_ROOT_FOLDER = "temp"

    # Lab related environment variables
    _ENV_NAME_LAB_ENDPOINT = "LAB_API_ENDPOINT"
    _ENV_NAME_LAB_PROJECT = "LAB_PROJECT"
    _ENV_NAME_LAB_API_TOKEN = "LAB_API_TOKEN"

    # local folders
    _LOCAL_ENV_FOLDER_NAME = "environment"
    _DATASETS_FOLDER_NAME = "datasets"
    _MODELS_FOLDER_NAME = "models"
    _DOWNLOADS_FOLDER_NAME = "downloads"

    _LOCAL_OPERATOR = "local"
    _LOCAL_PROJECT = "local"

    _LAB_USER_PROJECT_PREFIX = "lab-user-"

    class DataType:
        MODEL = "model"
        DATASET = "dataset"
        BACKUP = "backup"

    def __init__(
        self,
        project: str = None,
        root_folder: str = None,
        lab_endpoint: str = None,
        lab_api_token: str = None,
    ):
        self._connected = False

        if lab_endpoint is None:
            lab_endpoint = os.getenv(self._ENV_NAME_LAB_ENDPOINT)

        if lab_api_token is None:
            lab_api_token = os.getenv(self._ENV_NAME_LAB_API_TOKEN)

        self.project = project
        self.lab_api_token = lab_api_token
        self.lab_endpoint = lab_endpoint

        # Initialize Contaxy clients
        self._endpoint_client = BaseUrlSession(lab_endpoint)
        # TODO: Enable SSL verification!
        self._endpoint_client.verify = False
        self._auth_client = AuthClient(self._endpoint_client)
        self._file_client = FileClient(self._endpoint_client)
        self._extension_client = ExtensionClient(self._endpoint_client)

        self._file_handler = None

        self._check_connection()

        # Set root folder
        if not root_folder:
            # use environment variable
            root_folder = os.getenv(self._ENV_NAME_ENV_ROOT_PATH)

        if not root_folder:
            # create local environment
            root_folder = self._LOCAL_ENV_FOLDER_NAME

        if root_folder == self._TEMP_ROOT_FOLDER:
            # if folder is temp -> create temporary folder that will be removed on exit
            import atexit
            import shutil
            import tempfile

            root_folder = tempfile.mkdtemp()

            # automatically remove temp directory if process exits
            def cleanup():
                shutil.rmtree(root_folder)

            atexit.register(cleanup)

        if not os.path.exists(root_folder):
            os.makedirs(root_folder)

        self._root_folder = root_folder

    def is_connected(self) -> bool:
        """
        Returns `True`, if the environment is connected to a Lab instance.
        """
        return self._connected

    def print_info(self, host_info: bool = False):
        """
        Prints out a summary of the configuration of the environment instance. Can be used as watermark for notebooks.
        """
        print("Environment Info:")
        print("")
        if self._connected:
            print("Lab Endpoint: " + self.lab_endpoint)
        else:
            print("Lab Endpoint: Not connected!")
        print("")
        from lab_client._about import __version__

        print("Client Version: " + str(__version__))
        print("Configured Project: " + self.project)
        print("")
        print("Folder Structure: ")
        print("- Root folder: " + os.path.abspath(self.root_folder))
        print(" - Project folder: " + self.project_folder)
        print(" - Datasets folder: " + self.datasets_folder)
        print(" - Models folder: " + self.models_folder)

    def _check_connection(self):
        # We check that everything is working by querying the endpoint to list the files.
        token_information = self._auth_client.introspect_token(self.lab_api_token)
        if not token_information.active:
            raise ConnectionError("Not valid token")
        # Set token as a cookie for all other requests
        self._endpoint_client.cookies.set(API_TOKEN_NAME, self.lab_api_token)
        # Will raise exception if connection fails.
        self._file_client.list_files(self.project)
        self._connected = True

    @property
    def root_folder(self) -> str:
        """
        Returns the path to the root folder of the environment.
        """

        return self._root_folder

    @property
    def project_folder(self) -> str:
        """
        Returns the path to the project folder of the environment.
        """
        folder = os.path.join(self.root_folder, simplify(self.project))

        if not os.path.exists(folder):
            os.makedirs(folder)

        return folder

    @property
    def datasets_folder(self) -> str:
        """
        Returns the path to the datasets folder of the selected project.
        """
        folder = os.path.join(self.project_folder, self._DATASETS_FOLDER_NAME)

        if not os.path.exists(folder):
            os.makedirs(folder)

        return folder

    @property
    def models_folder(self) -> str:
        """
        Returns the path to the models folder of the selected project.
        """
        folder = os.path.join(self.project_folder, self._MODELS_FOLDER_NAME)

        if not os.path.exists(folder):
            os.makedirs(folder)

        return folder

    @property
    def file_handler(self) -> FileHandler:
        """
        Returns the file handler. The file handler provides additional functionality for interacting with the remote storage.
        """

        if self._file_handler is None:
            self._file_handler = FileHandler(self, self._file_client)

        return self._file_handler

    def upload_file(
        self,
        file_path: str,
        data_type: str,
        metadata: dict = None,
        file_name: str = None,
    ) -> str:
        """Uploads a file to the storage of the Lab Instance.

        Args:
            file_path: Local file path to the file you want ot upload.
            data_type: Data type of the file. Possible values are `model`, `dataset`, `experiment`.
            metadata: Adds additional metadata to remote storage (optional).
            file_name: File name to use in the remote storage. If not provided, the name will be extracted from the provided path (optional)
        Returns:
            Key of the uploaded file.
        """
        return self.file_handler.upload_file(file_path, data_type, metadata, file_name)

    def get_file(
        self, key: str, version: Optional[str] = None, force_download: bool = False
    ) -> str:
        """Returns local path to the file for the given `key`.
        If the file is not available locally, download it from the storage of the Lab Instance.

        Args:
            key: Key or url of the requested file.
            version: Version of the file to return. If `None` (default) the latest version will be returned.
            force_download: If `True`, the file will always be downloaded and not loaded locally (optional).
        Returns:
            Local path to the requested file or `None` if file is not available.
        """
        return self.file_handler.get_file(key, version, force_download)
    
    def setup_mlflow(self) -> None:
        """
        Sets up the MLflow environment.

        * Uses the contaxy API to check if the ML Flow extension is installed
        * Uses the ML Flow extension API to check if the ML Flow server is running (and if not, starts it)
        * Sets the tracking URI and token environment variables
        """
        extension_display_name = "MLFlow Server"
        mlflow_extension = self._get_extension(extension_display_name)
        print(mlflow_extension)
        if not self._is_extension_running(mlflow_extension):
            self._create_mlflow_server(mlflow_extension)
        tracking_uri = self._get_mlflow_tracking_uri(mlflow_extension)
        self._set_mlflow_env_vars(tracking_uri=tracking_uri, token=self.lab_api_token)
    
    def _get_extension(self, target_extension: str) -> Extension:
        global_project_name = "ctxy-global"
        all_extensions = self._extension_client.list_extensions(global_project_name)
        for extension in all_extensions:
            if extension.display_name == target_extension:
                return extension
        raise ExtensionNotInstalledError("ML Flow extension is not installed on this ML Lab instance.")
    
    def _is_extension_running(self, extension: Extension) -> bool:
        return extension.status == "running"
    
    def _create_mlflow_server(self, extension: Extension) -> None:
        endpoint = self._get_mlflow_backend_api_endpoint(extension) + "/projects/" + self.project + "/mlflow-server"
        requests.post(endpoint, headers={"Authorization": "Bearer " + self.lab_api_token})
    
    def _start_mlflow_server(self, extension: Extension, server_id: str) -> None:
        endpoint = self._get_mlflow_backend_api_endpoint(extension) + "/projects/" + self.project + "/mlflow-server" + server_id + "/start"
        requests.post(endpoint, headers={"Authorization": "Bearer " + self.lab_api_token})

    def _get_mlflow_backend_api_endpoint(self, extension: Extension) -> str:
        base_url = self.lab_endpoint.replace("/api", "")
        return base_url + extension.api_extension_endpoint
    
    def _get_mlflow_tracking_uri(self, extension: Extension) -> str:
        endpoint = self._get_mlflow_backend_api_endpoint(extension) + "/projects/" + self.project + "/mlflow-server"
        response = requests.get(endpoint, headers={"Authorization": "Bearer " + self.lab_api_token}).json()
        
        if len(response) == 0:
            self._create_mlflow_server(extension)
            return self._get_mlflow_tracking_uri(extension)
        
        mlflow_server = response[0]
        if mlflow_server["status"] == "stopped":
            self._start_mlflow_server(extension, mlflow_server["id"])
            return self._get_mlflow_tracking_uri(extension)

        base_url = self.lab_endpoint.replace("/api", "")
        return base_url + mlflow_server["access_url"]

    def _set_mlflow_env_vars(self, tracking_uri: str, token: str) -> None:
        self._set_tracking_uri_env_var(tracking_uri)
        self._set_token_env_var(token)
    
    def _set_tracking_uri_env_var(self, tracking_uri: str) -> None:
        os.environ["MLFLOW_TRACKING_URI"] = tracking_uri
    
    def _set_token_env_var(self, token: str) -> None:
        os.environ["MLFLOW_TRACKING_TOKEN"] = token
        

class ExtensionNotInstalledError(Exception):
    """
    Exception raised when the ML Flow extension is not installed on the Lab instance.
    """

    def __init__(self, message):
        super().__init__(message)
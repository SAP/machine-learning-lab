import os

from contaxy.clients import FileClient, AuthClient
from contaxy.clients.shared import BaseUrlSession
from contaxy.config import API_TOKEN_NAME

from .utils import simplify


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

    def __init__(self, project: str = None, root_folder: str = None, lab_endpoint: str = None,
                 lab_api_token: str = None):
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
        self._auth_manager = AuthClient(self._endpoint_client)
        self._file_manager = FileClient(self._endpoint_client)

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
            import tempfile
            import atexit
            import shutil

            root_folder = tempfile.mkdtemp()

            # automatically remove temp directory if process exits
            def cleanup():
                shutil.rmtree(root_folder)

            atexit.register(cleanup)

        if not os.path.exists(root_folder):
            os.makedirs(root_folder)

        self._root_folder = root_folder

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
        token_information = self._auth_manager.introspect_token(self.lab_api_token)
        if not token_information.active:
            raise ConnectionError("Not valid token")
        # Set token as a cookie for all other requests
        self._endpoint_client.cookies.set(API_TOKEN_NAME, self.lab_api_token)
        # Will raise exception if connection fails.
        self._file_manager.list_files(self.project)
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

    def upload_file(self, file_path: str, data_type: str, metadata: dict = None,
                    file_name: str = None, track_event: bool = True) -> str:
        """
        Uploads a file to the remote storage.

        # Arguments
            file_path (string): Local file path to the file you want ot upload.
            data_type (string): Data type of the file. Possible values are `model`, `dataset`, `experiment`.
            metadata (dict): Adds additional metadata to remote storage (optional).
            file_name (str): File name to use in the remote storage. If not provided, the name will be extracted from the provided path (optional)
            track_event (bool): If `True`, this file operation will be tracked and registered experiments will be notified (optional)

        # Returns
        Key of the uploaded file.

        # Raises
        Exception if file does not exist locally.
        """
        if file_name is None:
            file_name = file_path.split(os.sep)[-1]
        with open(file_path, 'rb') as f:
            file = self._file_manager.upload_file(self.project, f"{data_type}s/{file_name}", f)
        return file.key

    def get_file(self, key: str, force_download: bool = False, unpack: bool = False, track_event: bool = True) -> str:
        """
        Returns local path to the file for the given `key`. If the file is not available locally, download it from the remote storage.

        # Arguments
            key (string): Key or url of the requested file.
            force_download (boolean): If `True`, the file will always be downloaded and not loaded locally (optional).
            unpack (boolean): If `True`, the file - if a valid ZIP - will be unpacked within the data folder
                and the folder path will be returned (optional).
            track_event (bool): If `True`, this file operation will be tracked and registered experiments will be notified (optional)

        # Returns
        Local path to the requested file or `None` if file is not available.
        """
        prefix, file_name = key.split('/')[0], key.split('/')[-1]
        if prefix == "datasets":
            folder = self.datasets_folder
        elif prefix == "models":
            folder = self.models_folder
        else:
            folder = self.root_folder
        local_destination_path = os.path.join(folder, file_name)

        if not os.path.exists(local_destination_path) or force_download:
            bytes_iterator = self._file_manager.download_file(self.project, key)
            with open(local_destination_path, 'wb') as f:
                for byte in bytes_iterator:
                    f.write(byte)

        return local_destination_path

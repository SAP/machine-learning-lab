from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import logging
import os

from lab_api.swagger_client.rest import ApiException
from lab_client.commons import text_utils
from lab_client.handler import LabApiHandler, FileHandler
from lab_client.handler.experiment_handler import Experiment
from lab_client.utils import experiment_utils


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
    _ENV_NAME_LAB_ENDPOINT = "LAB_ENDPOINT"
    _ENV_NAME_LAB_PROJECT = "LAB_PROJECT"
    _ENV_NAME_LAB_API_TOKEN = "LAB_API_TOKEN"

    # local folders
    _LOCAL_ENV_FOLDER_NAME = "environment"
    _EXPERIMENTS_FOLDER_NAME = "experiments"
    _DATASETS_FOLDER_NAME = "datasets"
    _MODELS_FOLDER_NAME = "models"
    _DOWNLOADS_FOLDER_NAME = "downloads"

    _LOCAL_OPERATOR = "local"
    _LOCAL_PROJECT = "local"

    _LAB_USER_PROJECT_PREFIX = "lab-user-"

    class DataType:
        MODEL = "model"
        DATASET = "dataset"
        EXPERIMENT = "experiment"
        BACKUP = "backup"

    def __init__(self, project: str = None, root_folder: str = None, lab_endpoint: str = None,
                 lab_api_token: str = None):

        # Create the Logger
        self.log = logging.getLogger(__name__)

        # Initialize parameters
        self.active_exp = None
        self._connected = False  # connected to lab

        # Set root folder
        if not root_folder:
            # use environment variable
            root_folder = os.getenv(self._ENV_NAME_ENV_ROOT_PATH)

        if not root_folder:
            # that current git root as environment folder (add to gitignore?)
            root_folder = experiment_utils.get_git_root()

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
                self.log.info("Removing temp directory: " + root_folder)
                shutil.rmtree(root_folder)
                self.log.info("Temp directory removed")

            atexit.register(cleanup)

        if not os.path.exists(root_folder):
            os.makedirs(root_folder)

        self._root_folder = root_folder

        self._operator = None

        self._project = project
        if not self._project:
            self._project = os.getenv(self._ENV_NAME_LAB_PROJECT)

        if lab_endpoint is None:
            lab_endpoint = os.getenv(self._ENV_NAME_LAB_ENDPOINT)

        if lab_api_token is None:
            lab_api_token = os.getenv(self._ENV_NAME_LAB_API_TOKEN)

        if lab_endpoint and not lab_api_token:
            self.log.warning("lab_endpoint is provided but no lab_api_token")

        # Initialize handlers
        self._file_handler = None
        self._lab_handler = None

        if lab_endpoint and lab_api_token:
            self._lab_handler = LabApiHandler(lab_endpoint=lab_endpoint,
                                              lab_api_token=lab_api_token)

        if self._lab_handler is not None and self.lab_handler.is_connected():
            self.log.info("Initializing environment with Lab API: " + self.lab_handler.lab_endpoint)

            operator_user = self.lab_handler.auth_api.get_me()
            if operator_user and operator_user.data and operator_user.data.id:
                self._operator = operator_user.data.id
                self._connected = True
            else:
                self.log.warning("Failed to get user information from Lab Instance. Initializing local environment.")
                self._connected = False

            if not self._project:
                if self._operator:
                    self._project = self._LAB_USER_PROJECT_PREFIX + self._operator
                else:
                    self._project = self._LOCAL_PROJECT
                self.log.info("No project was selected. Will fallback to " + self._project)
            elif self._connected:
                try:
                    # check if project is accessible
                    project_info = self.lab_handler.lab_api.get_project(self._project)
                    if not self.lab_handler.request_successful(project_info):
                        self._connected = False
                    # TODO self._project = project_info.data.id # set to project id?
                except Exception as e:
                    if isinstance(e, ApiException):
                        self.log.warning(
                            "Failed to connect to lab. Reason: " + str(e.reason) + " (" + str(e.status) + ")")
                    self._connected = False
                if not self._connected:
                    self.log.warning(
                        "Failed to access project " + str(self._project) + " on Lab Instance. "
                                                                           "Initializing local environment.")
        else:
            self.log.info("Initializing local environment.")
            self._connected = False

    def print_info(self, host_info: bool = False):
        """
        Prints out a summary of the configuration of the environment instance. Can be used as watermark for notebooks.
        """
        print("Environment Info:")
        print("")
        if self.is_connected():
            print("Lab Endpoint: " + self.lab_handler.lab_endpoint)
            lab_info = self.lab_handler.admin_api.get_lab_info()
            print("Lab Version: " + lab_info.data.version)
        else:
            print("Lab Endpoint: Not connected!")
        print("")
        from lab_client.__version__ import __version__
        print("Client Version: " + str(__version__))
        print("Configured Project: " + self.project)
        print("Configured Operator: " + self.operator)
        print("")
        print("Folder Structure: ")
        print("- Root folder: " + os.path.abspath(self.root_folder))
        print(" - Project folder: " + self.project_folder)
        print(" - Datasets folder: " + self.datasets_folder)
        print(" - Models folder: " + self.models_folder)
        print(" - Experiments folder: " + self.experiments_folder)
        if host_info:
            print("")
            print("Host Info: ")
            import yaml
            print(yaml.safe_dump(experiment_utils.get_host_info().to_dict(),
                                 allow_unicode=True,
                                 default_flow_style=False))

    def is_connected(self) -> bool:
        """
        Returns `True`, if the environment is connected to a Lab instance.
        """

        return self._lab_handler is not None and self._connected

    def cleanup(self, only_selected_project: bool = False, max_file_size_mb: int = 50, last_file_usage: int = 3,
                replace_with_info: bool = True, excluded_folders: list = None):
        """
        Cleanup environment folder to reduce disk space usage.
        Removes all files with more than 50 MB that haven't been used for the last 3 days.

        # Arguments
            only_selected_project (bool): If 'True', only the currently selected project will be cleaned up.
             Otherwise all projects will be cleaned. Default: False.
             max_file_size_mb (int): Max size of files in MB that should be deleted. Default: 50.
            replace_with_info (bool): Replace removed files with `.removed.txt` files with file removal reason. Default: True.
            last_file_usage (int): Number of days a file wasn't used to allow the file to be removed. Default: 3.
            excluded_folders (list[str]): List of folders to exclude from removal (optional).
        """
        from lab_client.utils import file_handler_utils

        folder = self.root_folder

        if only_selected_project:
            folder = self.project_folder

        file_handler_utils.cleanup_folder(folder,
                                          max_file_size_mb=max_file_size_mb,
                                          last_file_usage=last_file_usage,
                                          replace_with_info=replace_with_info,
                                          excluded_folders=excluded_folders)

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
        folder = os.path.join(self.root_folder, text_utils.simplify(self.project))

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
    def downloads_folder(self) -> str:
        """
        Returns the path to the downloads folder of the selected project. This folder contains downloaded via url.
        """
        folder = os.path.join(self.project_folder, self._DOWNLOADS_FOLDER_NAME)
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
    def experiments_folder(self) -> str:
        """
        Returns the path to the experiment folder of the environment.
        """
        folder = os.path.join(self.project_folder, self._EXPERIMENTS_FOLDER_NAME)
        if not os.path.exists(folder):
            os.makedirs(folder)
        return folder

    @property
    def project(self) -> str:
        """
        Returns the name of the configured project.
        """

        if self._project is None:
            self._project = self._LOCAL_PROJECT

        return self._project

    @property
    def operator(self) -> str:
        """
        Returns the operator (user) of this environment.
        """

        if self._operator is None:
            self._operator = self._LOCAL_OPERATOR

        return self._operator

    # Handlers

    @property
    def file_handler(self) -> FileHandler:
        """
        Returns the file handler. The file handler provides additional functionality for interacting with the remote storage.
        """

        if self._file_handler is None:
            self._file_handler = FileHandler(self)

        return self._file_handler

    @property
    def lab_handler(self) -> LabApiHandler:
        """
        Returns the lab handler. The lab handler provides access to the REST API of the configured Lab Instance.
        """

        if self._lab_handler is None:
            self.log.debug("Lab Handler is not initialized.")

        return self._lab_handler

    # Default file operations, for more operations use file handler

    def upload_folder(self, folder_path: str, data_type: str, metadata: dict = None,
                      file_name: str = None, track_event: bool = True) -> str:
        """
        Packages (via ZIP) and uploads the specified folder to the remote storage.

        # Arguments
            folder_path (string): Local path to the folder you want ot upload.
            data_type (string): Data type of the resulting zip-file. Possible values are `model`, `dataset`, `experiment`.
            metadata (dict): Adds additional metadata to remote storage (optional).
            file_name (str): File name to use in the remote storage. If not provided, the name will be extracted from the provided path (optional)
            track_event (bool): If `True`, this file operation will be tracked and registered experiments will be notified (optional)

        # Returns
        Key of the uploaded (zipped) folder.

        # Raises
        Exception if folder does not exist locally
        """

        return self.file_handler.upload_folder(folder_path, data_type,
                                               metadata=metadata,
                                               file_name=file_name,
                                               track_event=track_event)

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

        return self.file_handler.upload_file(file_path, data_type,
                                             metadata=metadata,
                                             file_name=file_name,
                                             track_event=track_event)

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

        return self.file_handler.get_file(key, force_download=force_download, unpack=unpack, track_event=track_event)

    # experiment handling
    def create_file_path(self, filename: str) -> str or None:
        """
        Returns the path for a new file in the experiment folder.

        # Arguments
            filename (string): Name for the new file.

        # Returns
        Local path in experiment folder for the new file.
        """
        if not self.active_exp:
            self.log.info("This environment does not have an active experiment. "
                          " Creating a temporary experiment.")

            self.active_exp = Experiment(self, "local temp experiment",
                                         auto_sync=False,
                                         track_file_events=False,
                                         redirect_logs=False,
                                         upload_code_repo=False,
                                         upload_code_script=False)

        return self.active_exp.create_file_path(filename)

    def create_experiment(self, name: str) -> Experiment:
        """
        Creates a new #Experiment and save it as active experiment.

        # Arguments
            name (string): Short description of the experiment.

        # Returns
        The created #Experiment instance.
        """
        self.active_exp = Experiment(self, name, context_symbol_table=experiment_utils.get_caller_symbol_table())
        return self.active_exp

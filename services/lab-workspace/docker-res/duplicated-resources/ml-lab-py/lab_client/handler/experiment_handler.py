from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import datetime
import gc
import json
import logging
import os
import sys
import time
import traceback

import six
from tensorboardX import SummaryWriter

import lab_client
from lab_api.swagger_client import LabExperiment, ExperimentResources
from lab_client.commons import text_utils, file_utils, notebook_utils
from lab_client.utils import experiment_utils, file_handler_utils
from lab_client.utils.experiment_utils import IntervalTimer


def current_milli_time() -> int:
    return int(round(time.time() * 1000))


def call_function(func, **kwargs: dict):
    func_params = func.__code__.co_varnames[:func.__code__.co_argcount]
    func_args = {}
    for arg in kwargs:
        # arg or _arg in function parameters
        if arg in func_params or "_" + arg in func_params:
            func_args[arg] = kwargs[arg]
    return func(**func_args)


class ExperimentState:
    INITIALIZED = "initialized"
    QUEUED = "queued"  # deprecated
    RUNNING = "running"
    COMPLETED = "completed"
    FAILED = "failed"
    INTERRUPTED = "interrupted"
    UNKNOWN = "unknown"


class Experiment:
    """
    Initializes a new experiment with own output folder based on this naming schema:
    `YYYY-MM-DD-hh-mm-ss_<name>`

    You can create multiple experiments in one script/notebook.

    # Arguments
        env (#Environment): Environment instance.
        name (string): Short description of the experiment. The name cannot be changed after initialization.
        auto_sync (bool): If 'True', the experiment will automatically sync its state with the Lab Instance.
        Otherwise, you will have to manually call `sync_exp` whenever you want to sync the experiment state (optional).
        track_file_events (bool): If `True`, file events in the environment (e.g. requesting or uploading files), will be
        automatically tracked and added to the active experiment (optional).
        upload_code_script (bool): If `True`, upload main code (script or notebook) to Lab Instance. Default: True.
        upload_code_repo (bool): If `True`, upload git repository (if it can be determined) as a zipped folder to Lab Instance. Default: False.
        redirect_logs (bool): If `True`, redirect stdout/stderr logs to a file (optional).
        context_symbol_table (dict): Provide the symbol table of the experiment context, e.g. the calling script (optional).
        group_key (string): Group key shared between multiple experiments (optional).
        experiment_key (string): Provide a custom or existing key for the experiment. This can be used to overwrite or use data from an existing experiment (optional).
    """

    _EXPERIMENT_INFO_FILE_NAME = "experiment.json"
    _RUN_INFO_FILE_NAME = "{}_run.json"
    _TENSORBOARD_LOG_DIR = "tensorboard"
    _FULL_EXPERIMENT_BACKUP = "experiment.zip"
    _SOURCE_CODE_PACKAGE_NAME = "experiment-code.zip"
    _STDOUT_FILE_NAME = "stdout.txt"

    def __init__(self, env, name: str,
                 auto_sync: bool = True,
                 track_file_events: bool = True,
                 upload_code_script: bool = True,
                 upload_code_repo: bool = False,
                 redirect_logs: bool = True,
                 context_symbol_table: dict = None,
                 root_folder: dict = None,
                 group_key: str = None,
                 experiment_key: str = None):
        # TODO option to track file events only during run
        # Initialize logger
        self.log = logging.getLogger(__name__)

        # Initialize internal variables -> Lazy initialization
        self._track_file_events = None
        self._running = False
        self._has_run = False
        self._syncing = False
        self._output_path = None
        self._tensorboard_path = None

        # Initialize components -> Lazy initialization
        self._tensorboard_logger = None
        self._stdout_file_redirect = None

        # Initialize internal variables from parameters
        self._root_folder = root_folder
        self._name = name
        self._env = env
        self._context_symbol_table = context_symbol_table

        # Initialize public variables from parameters
        self.auto_sync = auto_sync
        self.redirect_logs = redirect_logs
        self.upload_code_script = upload_code_script
        self.upload_code_repo = upload_code_repo
        self.track_file_events = track_file_events  # use setter to add event listeners
        self.init_time = datetime.datetime.now()
        self._artifacts = {}

        # Initialize internal variables
        self._exp_script_dir = os.getcwd()

        if experiment_key:
            # use provided key
            self._key = experiment_key
        else:
            # Generate key
            timestamp = self.init_time.strftime('%Y-%m-%d-%H-%M-%S')
            self._key = '{}_{}_{}'.format(timestamp,
                                          text_utils.simplify(self._env.operator),
                                          text_utils.simplify(self.name))

        # set context (symbol table)
        if not self._context_symbol_table:
            self._context_symbol_table = experiment_utils.get_caller_symbol_table()

        # Initialize experiment metadata
        self._exp_metadata = self._init_default_metadata()

        if group_key:
            self.exp_metadata.group_key = group_key

        # TODO set as active experiment?

        self.log.info("Experiment " + self.name + " is initialized.")
        self.log.debug("Experiment key: " + self.key)

        if not self._env.is_connected():
            self.log.info("Environment is not connected to Lab. Experiment data will only be synced locally.")

    def _init_default_metadata(self) -> LabExperiment:
        """
        Initialize experiment metadata.
        """
        exp_metadata = LabExperiment()
        exp_metadata.key = self._key
        exp_metadata.name = self.name
        exp_metadata.project = self._env.project
        exp_metadata.operator = self._env.operator
        exp_metadata.started_at = current_milli_time()
        exp_metadata.status = ExperimentState.INITIALIZED
        exp_metadata.client_version = lab_client.__version__

        exp_metadata.host = experiment_utils.get_host_info()
        exp_metadata.git = experiment_utils.get_git_info(self._exp_script_dir)
        exp_metadata.dependencies = experiment_utils.get_python_dependencies(self._context_symbol_table)
        exp_metadata.resources = ExperimentResources()

        # TODO get jupyter session information?

        return exp_metadata

    def print_info(self):
        """
        Prints out a summary of the experiment configuration and state.
        """
        print("Experiment key: " + self.key)
        print("Experiment name: " + self.name)
        print("Experiment path: " + self.output_path)
        print("Auto-sync activated: " + str(self.auto_sync))
        print("")
        print("Experiment metadata: ")
        print(self.exp_metadata.to_str())

    # Getter and Setter

    @property
    def output_path(self) -> str:
        """
        Returns the path to the root folder of the experiment.
        """
        if self._output_path is None:
            if not self._root_folder:
                self._root_folder = self._env.experiments_folder
            folder = os.path.join(self._root_folder, self.key)

            if not os.path.exists(folder):
                os.makedirs(folder)

            self._output_path = folder

        return self._output_path

    @property
    def exp_metadata(self) -> LabExperiment:
        """
        Returns the current state of experiment metadata.
        """

        return self._exp_metadata

    @property
    def name(self) -> str:
        """
        Returns the name of the experiment.
        """

        return self._name

    @property
    def key(self) -> str:
        """
        Returns the key of the experiment.
        """

        return self._key

    @property
    def track_file_events(self) -> bool:
        """
        Returns `True`, if file events (e.g. requesting, creating or uploading files) will be automatically tracked
        """

        return self._track_file_events

    @track_file_events.setter
    def track_file_events(self, value: bool):
        """
        Activate or deactivate automatic file tracking for this experiment.
        """

        self._track_file_events = value

        # add or remove listeners from file handler
        file_requested_event = self._env.file_handler.on_file_requested
        file_uploaded_event = self._env.file_handler.on_file_uploaded

        if self._track_file_events:
            if self.log_input_file not in file_requested_event:
                file_requested_event.append(self.log_input_file)

            if self.log_output_file not in file_uploaded_event:
                file_uploaded_event.append(self.log_output_file)
        else:
            if self.log_input_file in file_requested_event:
                file_requested_event.remove(self.log_input_file)

            if self.log_output_file in file_uploaded_event:
                file_uploaded_event.remove(self.log_output_file)

    # Core API
    def create_file_path(self, filename: str, create_folders=True) -> str:
        """
        Returns the path for a new file in the experiment folder.

        # Arguments
            filename (string): Name for the new file.
            create_folders (boolean): If `True`, all missing folders will be created for the path (optional).

        # Returns
        Local path in experiment folder for the new file.
        """

        created_file = os.path.join(self.output_path, filename)
        created_file_dir = os.path.dirname(created_file)
        if create_folders:
            if not os.path.exists(created_file_dir):
                os.makedirs(created_file_dir)

        if self.track_file_events:
            # track new created file as artifact
            self.log_artifact(filename, "Type: file-path")  # Manually put type info

        return os.path.join(self.output_path, filename)

    def backup_exp(self) -> str or None:
        """
        Packages and uploads the full experiment to the remote storage as data type experiment.
        """
        if not self._env.is_connected():
            self.log.warning("Environment is not connected to Lab. Experiment data cannot be backuped.")
            return None

        # set file_name based on operator and experiment folder name
        file_name = os.path.join(self.key, self._FULL_EXPERIMENT_BACKUP)
        backup_key = self._env.upload_folder(self.output_path,
                                             self._env.DataType.EXPERIMENT,
                                             file_name=file_name,
                                             track_event=False)

        if backup_key:
            self.exp_metadata.resources.experiment_backup = backup_key

        self._sync_log_event()

        return backup_key

    def upload_file(self, path, log_as_output: bool = True):
        """
        Upload a experiment-related file or folder to the storage of the connected Lab Instance and add it to the experiment.

        # Arguments
            path (string): Local file or folder path to the resource you want to upload.
            log_as_output (bool): If `True`, the file will be logged as output file. Default: True.

        # Returns
        Key of the uploaded file.

        # Raises
        Exception if file does not exist locally.
        """

        if os.path.isfile(path):
            remote_path = os.path.join(self.key, file_utils.get_filename(path, exclude_extension=False))
            res_key = self._env.upload_file(path,
                                            self._env.DataType.EXPERIMENT,
                                            file_name=remote_path,
                                            track_event=False)
            if log_as_output:
                # Track as output
                self.log_output_file(res_key)
            return res_key
        elif os.path.isdir(path):
            remote_path = os.path.join(self.key, file_utils.get_folder_name(path) + ".zip")
            res_key = self._env.upload_folder(path,
                                              self._env.DataType.EXPERIMENT,
                                              file_name=remote_path,
                                              track_event=False)
            if log_as_output:
                # Track as output
                self.log_output_file(res_key)
            return res_key
        else:
            self.log.warning("Provided path is not a file or folder. " + str(path))
            return None

    @property
    def params(self) -> dict:
        """
        Get all parameters saved in experiment metadata

        # Returns
        Dictionary of parameters in experiment metadata.
        """

        if not self.exp_metadata.parameters:
            self.exp_metadata.parameters = {}
        return self.exp_metadata.parameters

    def log_param(self, name: str, value):
        """
        Adds or updates a single parameter (hyperparameter) to the experiment metadata

        # Arguments
            name (string): Name of the parameter
            value: Value of the parameter (should be string or numeric value)
        """
        self.params[name] = value

        self._sync_log_event()

    def log_params(self, params: dict):
        """
        Adds or updates multiple parameters (hyperparameters) to the experiment metadata

        # Arguments
            params (dict): Dictionary of parameters
        """
        self.params.update(params)

        self._sync_log_event()

    @property
    def metrics(self) -> dict:
        """
        Get all metrics saved in experiment metadata

        # Returns
        Dictionary of metrics in experiment metadata.
        """
        if not self.exp_metadata.metrics:
            self.exp_metadata.metrics = {}
        return self.exp_metadata.metrics

    def log_metric(self, name: str, value):
        """
        Adds or updates a single metric to the experiment metadata

        # Arguments
            name (string): Name of the metric
            value: Value of the metric (should be string or numeric value)
        """
        self.metrics[name] = value

        self._sync_log_event()

    def log_metrics(self, metrics: dict):
        """
        Adds or updates multiple metrics to the experiment metadata

        # Arguments
            metrics (dict): Dictionary of metrics
        """
        self.metrics.update(metrics)

        self._sync_log_event()

    def log_input_file(self, file_key: str):
        """
        Add a file key to input files.

        # Arguments
            file_key (string): File key
        """
        if not self.exp_metadata.resources.input:
            self.exp_metadata.resources.input = []

        if file_key not in self.exp_metadata.resources.input:
            self.exp_metadata.resources.input.append(file_key)

            self._sync_log_event()

    def log_output_file(self, file_key: str):
        """
        Add a file key to output files.

        # Arguments
            file_key (string): File key
        """
        if not self.exp_metadata.resources.output:
            self.exp_metadata.resources.output = []

        if file_key not in self.exp_metadata.resources.output:
            self.exp_metadata.resources.output.append(file_key)

            self._sync_log_event()

    def log_artifact(self, artifact_key: str, artifact=None):

        """
        Add information about an artifact to the experiment metadata. Use `add_artifact`
        to add the artifact to the experiment (make it able to retrieve the artifact instance).

        # Arguments
            artifact_key (string): Key of the artifact
            artifact: Artifact instance, could be any type of object (optional)
        """
        if not self.exp_metadata.resources.artifacts:
            self.exp_metadata.resources.artifacts = []

        if artifact is not None:
            artifact_desc = ""
            if isinstance(artifact, six.string_types):
                if os.path.isfile(artifact):
                    try:
                        file_size = text_utils.simplify_bytes(os.path.getsize(artifact))
                    except:
                        file_size = "?"

                    artifact_desc += "Type: file, Size: " + file_size + ", Path: " + str(artifact)
                else:
                    artifact_desc += str(artifact)
            else:
                import hashlib
                artifact_type = experiment_utils.get_class_name(artifact)
                artifact_hash = hashlib.md5(str(artifact).encode("utf-8")).hexdigest()

                artifact_desc = "Type: " + str(artifact_type) + ", Hash: " + str(artifact_hash)

                import pandas as pd
                if isinstance(artifact, pd.DataFrame):
                    artifact_desc += ", Shape: " + str(artifact.shape[0]) + ", " + str(artifact.shape[1])
                else:
                    try:
                        # if object has a length, add it here
                        artifact_desc += ", Length: " + str(len(artifact))
                    except:
                        pass
            artifact_key += " (" + str(artifact_desc) + ")"

        if artifact_key not in self.exp_metadata.resources.artifacts:
            self.exp_metadata.resources.artifacts.append(artifact_key)
            self._sync_log_event()

    def log_figure(self, figure, name: str = None):
        try:
            if not name:
                timestamp = datetime.datetime.now().strftime('%Y-%m-%d-%H-%M-%S')
                name = timestamp + "-figure"

            figure_file_path = self.create_file_path(name + ".jpg")
            figure.savefig(figure_file_path)
            self.upload_file(figure_file_path, True)
        except Exception as e:
            self.log.warning("Failed to upload figure.", e)

    def add_artifact(self, artifact_key: str, artifact):
        """
        Add an artifact to the experiment. The artifact can be always retrieved from the experiment instance.

        # Arguments
            artifact_key (string): Key of the artifact
            artifact: Artifact instance (could be any type of object)
        """
        self._artifacts[artifact_key] = artifact
        self.log_artifact(artifact_key, artifact)

    def get_artifact(self, artifact_key: str) -> object:
        """
        Returns the artifact based on the given key.

        # Arguments
            artifact_key (string): Key of the artifact
        """
        return self._artifacts[artifact_key]

    @property
    def _other_metadata(self) -> dict:
        """
        Get all metadata save in others section of the experiment metadata

        # Returns
        Dictionary of metadata.
        """

        if not self.exp_metadata.others:
            self.exp_metadata.others = {}
        return self.exp_metadata.others

    def log_other(self, name: str, value):
        """
        Adds or updates a single metadata item to the others section of the experiment metadata

        # Arguments
            name (string): Name of the metadata item
            value: Value of the metadata
        """
        self._other_metadata[name] = value

        self._sync_log_event()

    def log_others(self, others: dict):
        """
        Adds or updates a multiple metadata items to the others section of the experiment metadata

        # Arguments
            params (dict): Dictionary of metadata items
        """
        self._other_metadata.update(others)

        self._sync_log_event()

    def log_dependencies(self, dependencies: list):
        """
        Adds multiple dependencies to the experiment metadata

        # Arguments
            dependencies (list[str]): Dictionary of metadata items
        """
        if not self.exp_metadata.dependencies:
            self.exp_metadata.dependencies = []
        for dependency in dependencies:
            if dependency not in self.exp_metadata.dependencies:
                self.exp_metadata.dependencies.append(dependency)

        self._sync_log_event()

    def _sync_log_event(self):
        """
        Syncs experiment metadata after a manual log event only if auto sync is activated,
        the experiment is not currently running and already has been run at least one time
        """
        # sync only after first run and if not currently running
        if self.auto_sync and not self._running and self._has_run:
            self.sync_exp(upload_resources=False)

    def upload_resources(self):
        """
        Collect and upload various experiment resources (stdout logs, tensorboard logs, source code...) to storage on Lab Instance
        """
        try:
            script_name, script_type, script_content = experiment_utils.get_source_script(self._context_symbol_table)

            if script_name:
                self.exp_metadata.script_name = script_name

                if script_content:
                    file_utils.save_string(os.path.join(self.output_path, script_name), script_content)

            if script_type:
                self.exp_metadata.script_type = script_type

        except Exception as e:
            self.log.info("Failed to get script: " + str(e))

        # TODO zip git directory here as well?

        if not self._env.is_connected():
            self.log.warning("Environment is not connected to Lab. Experiment data cannot be uploaded.")
            return

        if self._tensorboard_path and os.path.isdir(self._tensorboard_path):
            remote_path = os.path.join(self.key,
                                       file_utils.get_folder_name(self._tensorboard_path) + ".zip")
            tensorboard_key = self._env.upload_folder(self._tensorboard_path,
                                                      self._env.DataType.EXPERIMENT,
                                                      file_name=remote_path,
                                                      track_event=False)
            if tensorboard_key:
                self.exp_metadata.resources.tensorboard_logs = tensorboard_key
                self.exp_metadata.resources.experiment_dir = os.path.dirname(tensorboard_key)

        if os.path.isfile(self.stdout_path):
            remote_path = os.path.join(self.key, file_utils.get_filename(self.stdout_path, exclude_extension=False))
            stdout_key = self._env.upload_file(self.stdout_path,
                                               self._env.DataType.EXPERIMENT,
                                               file_name=remote_path,
                                               track_event=False)

            if stdout_key:
                self.exp_metadata.resources.stdout = stdout_key
                self.exp_metadata.resources.experiment_dir = os.path.dirname(stdout_key)

        if self.upload_code_script:
            # upload script file if available -> if file name was set and file exists in local folder
            if self.exp_metadata.script_name:
                script_path = os.path.join(self.output_path, self.exp_metadata.script_name)
                if os.path.isfile(script_path):
                    remote_path = os.path.join(self.key, file_utils.get_filename(script_path, exclude_extension=False))
                    script_file_key = self._env.upload_file(script_path,
                                                            self._env.DataType.EXPERIMENT,
                                                            file_name=remote_path,
                                                            track_event=False)

                    if script_file_key:
                        self.exp_metadata.resources.source_script = script_file_key
                        self.exp_metadata.resources.experiment_dir = os.path.dirname(script_file_key)

        if self.upload_code_repo:
            # upload git repository if available
            git_root_dir = experiment_utils.get_git_root(self._exp_script_dir)
            if git_root_dir:
                # zip git repository with all files under 50 MB and ignore .git and environment folder
                zipped_repo = file_handler_utils.zip_folder(git_root_dir, max_file_size=50,
                                                            excluded_folders=["environment", ".git"])
                if zipped_repo:
                    remote_path = os.path.join(self.key,
                                               self._SOURCE_CODE_PACKAGE_NAME)  # use original folder name?
                    source_code_key = self._env.upload_file(zipped_repo,
                                                            self._env.DataType.EXPERIMENT,
                                                            file_name=remote_path,
                                                            track_event=False)

                    if source_code_key:
                        self.exp_metadata.resources.source_code = source_code_key
                        self.exp_metadata.resources.experiment_dir = os.path.dirname(source_code_key)

    def get_metadata_as_dict(self):
        """
        Returns the experiment metadata as a dictionary
        """
        try:
            return json.loads(self._env.lab_handler.lab_obj_to_json(self.exp_metadata))
        except:
            # lab handler not initialized
            import lab_api.swagger_client as swagger_client
            json.loads(json.dumps(swagger_client.ApiClient().sanitize_for_serialization(self.exp_metadata)))

    def sync_exp(self, upload_resources: bool = False):
        """
        Synchronizes the current state of this experiment with the connected Lab instance and/or the local experiment folder.
        # Arguments
            upload_resources (bool): If `true`, various files (stdout logs, tensorboard logs, source code...) are uploaded to storage on lab (optional).
        """

        if self._syncing:
            # If already syncing, do not sync again
            self.log.debug("Experiment is already syncing.")
            return

        self.log.debug("Syncing experiment. Upload resources: " + str(upload_resources))

        self._syncing = True
        self.exp_metadata.updated_at = current_milli_time()
        self.exp_metadata.duration = int(round(self.exp_metadata.updated_at - self.exp_metadata.started_at))

        if upload_resources:
            self.upload_resources()

        if self._env.is_connected():
            self.log.debug("Synchronizing experiment data with connected Lab instance. "
                           "Experiment status: " + self.exp_metadata.status)
            try:
                response = self._env.lab_handler.lab_api.sync_experiment(self.exp_metadata, self._env.project)
                if not self._env.lab_handler.request_successful(response):
                    self.log.warning("Failed to synchronize experiment data to Lab instance.")
            except:
                self.log.warning("Failed to synchronize experiment data to Lab instance.")
        else:
            self.log.debug("Environment is not connected to Lab. Experiment data will only be synced locally.")

        # save experiment json to local experiment folder
        experiment_info_path = os.path.join(self.output_path, self._EXPERIMENT_INFO_FILE_NAME)
        file_utils.save_dict_json(experiment_info_path, self.get_metadata_as_dict())
        self._syncing = False

    def run_exp(self, exp_function: list or function, params: dict = None, artifacts: dict = None):
        """
        Runs the given experiment function or list of functions and updates the experiment metadata

        # Arguments
            exp_function (function or list): Method that implements the experiment.
            params (dict): Dictionary that contains the configuration (e.g. hyperparameters) for the experiment (optional).
            artifacts (dict): Dictionary that contains artifacts (any kind of python object) required for the experiment (optional).
        """
        # TODO track file events only during run?

        if self._running:
            self.log.warning("Experiment is already running. Running same experiment in parallel "
                             "might give some trouble.")
        elif self._has_run:
            self.log.info("This experiment has already been run. Metadata will be overwritten! "
                          "It is suggested to initialize a new experiment. "
                          "The metadata of the last run is still saved in a run.json in the local exp folder.")

        # Redirect stdout/sterr to file
        if self._stdout_file_redirect is None:
            self._stdout_file_redirect = experiment_utils.StdoutFileRedirect(log_path=self.stdout_path)

        if self.redirect_logs:
            self._stdout_file_redirect.redirect()

        # Executing experiment functions
        self.log.info("Running experiment: " + self.name)
        self._running = True
        self._has_run = True  # even
        self._syncing = False

        # add artifacts
        if artifacts is not None:
            for artifact_key in artifacts:
                self.add_artifact(artifact_key, artifacts[artifact_key])

        # Log params
        if params is None:
            params = {}
        else:
            params = params.copy()

        self.log_params(params)

        # Wraps the experiment function into another function for more control
        def exp_wrapper():
            result_value = None
            kwargs = {"exp": self,
                      "params": self.params,
                      "artifacts": self._artifacts,
                      "config": self.params,
                      "log": self.log}

            if type(exp_function) is list:
                for exp_func in exp_function:
                    if not callable(exp_func):
                        self.log.warning(str(exp_func) + " is not a function.")
                        continue
                    self.log.info("Running experiment function: " + exp_func.__name__)
                    if not self.exp_metadata.command:
                        self.exp_metadata.command = exp_func.__name__
                    else:
                        self.exp_metadata.command += " -> " + exp_func.__name__

                    result_value = call_function(exp_func, **kwargs)
            elif callable(exp_function):
                self.log.info("Running experiment function: " + exp_function.__name__)
                self.exp_metadata.command = exp_function.__name__
                result_value = call_function(exp_function, **kwargs)
            else:
                self.log.error(str(exp_function) + " is not a function.")

            if result_value:
                return result_value

        sync_heartbeat = None
        heartbeat_stop_event = None

        if self.auto_sync:
            # only required for auto sync
            # sync hearbeat for every 20 seconds - make configurable?
            # run sync exp without uploading resources
            heartbeat_stop_event, sync_heartbeat = IntervalTimer.create(self.sync_exp, 20)
            # Initial sync of metadata
            self.sync_exp(upload_resources=False)

        try:
            if sync_heartbeat:
                # Start heartbeat if initialized
                sync_heartbeat.start()
            self.exp_metadata.status = ExperimentState.RUNNING
            self.set_completed(exp_wrapper())
        except:
            ex_type, val, tb = sys.exc_info()

            if ex_type is KeyboardInterrupt:
                # KeyboardInterrupt cannot be catched via except Exception: https://stackoverflow.com/questions/4990718/about-catching-any-exception
                self.exp_metadata.status = ExperimentState.INTERRUPTED
            else:
                self.exp_metadata.status = ExperimentState.FAILED

            self._finish_exp_run()

            # TODO: Move this part into utils?
            if notebook_utils.in_ipython_environment() and len(self.exp_metadata.host.gpus) > 0:
                # TODO only call on keyboard interrupt and CUDA problems? debug modus will not be available
                # if in jupyter and gpus available -> clear stack trace to free GPU memory
                # https://docs.fast.ai/troubleshoot.html#custom-solutions
                # https://github.com/fastai/fastai/blob/master/fastai/utils/ipython.py
                traceback.clear_frames(tb)

                # Collect Garbage
                gc.collect()

                # https://github.com/stas00/ipyexperiments/blob/master/ipyexperiments/ipyexperiments.py
                # now we can attempt to reclaim GPU memory - not needed?
                # try:
                #    import torch
                #    torch.cuda.empty_cache()
                # except:
                #    pass

                raise ex_type(val).with_traceback(tb) from None

            raise
        finally:
            if sync_heartbeat:
                # Always stop heartbeat if initialized
                heartbeat_stop_event.set()
                sync_heartbeat.join(timeout=2)

    def _finish_exp_run(self):
        self.exp_metadata.finished_at = current_milli_time()
        self.exp_metadata.duration = int(round(self.exp_metadata.finished_at - self.exp_metadata.started_at))

        state_desc = "finished"
        if self.exp_metadata.status:
            state_desc = self.exp_metadata.status

        self.log.info("Experiment run " + state_desc + ": " + self.name + "." +
                      " Duration: " + text_utils.simplify_duration(self.exp_metadata.duration))

        self._running = False
        if self.redirect_logs and self._stdout_file_redirect:
            self._stdout_file_redirect.reset()

        # save experiment json for every run to local experiment folder
        run_json_name = self._RUN_INFO_FILE_NAME.format(datetime.datetime.now().strftime('%Y-%m-%d-%H-%M-%S'))
        file_utils.save_dict_json(os.path.join(self.output_path, run_json_name), self.get_metadata_as_dict())
        # sync experiment
        if self.auto_sync:
            self.sync_exp(upload_resources=True)

    def set_completed(self, result: str = None):
        """
        Set experiment to completed and sync metadata and files if auto sync is enabled.
        Only required to manually complete experiment for example if run_exp is not used.
        # Arguments
            result (str): Final result metric (optional).
        """
        self._has_run = True
        self.exp_metadata.result = result
        self.exp_metadata.status = ExperimentState.COMPLETED

        self._finish_exp_run()

    # Components
    @property
    def stdout_path(self):
        return os.path.join(self.output_path, self._STDOUT_FILE_NAME)

    def write_stdout(self, stdout: str):
        if stdout and len(stdout.strip()) > 0:
            # only write if stdout actually contains logs
            with open(self.stdout_path, "a", encoding='utf-8') as stdout_file:
                stdout_file.write(stdout)

    @property
    def tensorboard_path(self) -> str:
        """
        Returns the tensorboard log dir of the experiment.
        """
        if self._tensorboard_path is None:
            folder = os.path.join(self.output_path, self._TENSORBOARD_LOG_DIR)

            if not os.path.exists(folder):
                os.makedirs(folder)

            self._tensorboard_path = folder

        return self._tensorboard_path

    def init_tensorboard_logger(self, **kwargs):
        """
        Initialize the tensorboard logger via tensorboardX Summary Writer. Should be only used if manual changes on the
        Summary Writer configurations are required.
        """
        self._tensorboard_logger = SummaryWriter(logdir=self.tensorboard_path, **kwargs)
        self.log.info("Tensorboard Logger initialized in: " + str(self.tensorboard_path))
        return self._tensorboard_logger

    @property
    def tensorboard_logger(self) -> SummaryWriter:
        """
        Get tensorboard logger. See tensorboardX documentation for more details.
        """

        if self._tensorboard_logger is None:
            self.init_tensorboard_logger()
        return self._tensorboard_logger


class ExperimentGroup:

    def __init__(self, base_exp: Experiment,
                 group_key: str = None,
                 use_group_folder: bool = True,
                 context_symbol_table: dict = None):
        # Initialize logger
        self.log = logging.getLogger(__name__)

        # use output folder fo base experiment
        self._base_exp = base_exp
        self._experiments = []
        self._use_group_folder = use_group_folder
        self._context_symbol_table = context_symbol_table

        self.group_key = group_key
        if not self.group_key:
            # use base exp key as group key
            self.group_key = base_exp.key

        base_exp.exp_metadata.group_key = self.group_key

        # set group key of base experiment
        self._base_exp.exp_metadata.group_key = self.group_key

    def create_exp(self, name_suffix: str = None, context_symbol_table: dict = None) -> Experiment:
        # TODO require name suffix
        name = self.base_exp.name
        if name_suffix:
            # Add name suffix to experiment name
            name = name + " - " + name_suffix

        if context_symbol_table:
            self._context_symbol_table = context_symbol_table
        if not self._context_symbol_table:
            self._context_symbol_table = experiment_utils.get_caller_symbol_table()
        exp_copy = Experiment(self.base_exp._env,
                              name,
                              auto_sync=self.base_exp.auto_sync,
                              track_file_events=False,  # Do not track file events for sub experiments
                              upload_code_script=self.base_exp.upload_code_script,
                              upload_code_repo=self.base_exp.upload_code_repo,
                              redirect_logs=self.base_exp.redirect_logs,
                              context_symbol_table=self._context_symbol_table,
                              root_folder=self.base_exp.output_path if self._use_group_folder else None,
                              group_key=self.group_key)

        # Initialize with all input files and artifacts
        exp_copy.exp_metadata.resources.input = self.base_exp.exp_metadata.resources.input
        exp_copy.exp_metadata.resources.artifacts = self.base_exp.exp_metadata.resources.artifacts
        self._experiments.append(exp_copy)
        return exp_copy

    @property
    def base_exp(self) -> Experiment:
        return self._base_exp

    def experiments(self, reverse_sort=True) -> list:
        # Rename reverse_sort to highest first
        try:
            return sorted(self._experiments, key=lambda x: x.exp_metadata.result, reverse=reverse_sort)
        except:
            return self._experiments

    def plot_results(self):
        """
        Runs the given experiment function or list of functions with various parameter configuration
        """
        experiment_utils.plot_exp_metric_comparison(self.experiments(reverse_sort=False))

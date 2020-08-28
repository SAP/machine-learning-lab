import abc
import atexit
import datetime
import inspect
import json
import logging
import os
import platform
import shutil
import subprocess
import sys
import tempfile
import zipfile

import cloudpickle as pickle
import pandas as pd
import six

from unified_model.__version__ import __version__ as unfied_model_version
from unified_model.utils import simplify, get_file_name


class NotSupportedException(Exception):
    pass


log = logging.getLogger(__name__)


class UnifiedModel(object):
    __metaclass__ = abc.ABCMeta

    _PICKLE_FILENAME = "unified_model.pkl"
    _REQUIREMENTS_FILENAME = "requirements.txt"
    _SETUP_SCRIPT_FILENAME = "setup.sh"
    _INFO_FILENAME = "info.json"
    _CODE_BASE_DIR = "code"
    _DATA_BASE_DIR = "data"

    _DATA_COLUMN = "data"

    # put modules and requirements in requirements list? call it dependencies
    def __init__(self, name: str = None, requirements: list = None, info_dict: dict = None, setup_script: str = None):
        """
        Initialize unified model

        # Arguments
            name (string): Name of model (optional).
            requirements (list): List of dependencies. Can be either an imported module or a requirement installable via pip (optional)
            info_dict (dict): Dictionary to add additional metadata about the model (optional)
            setup_script (string): Add additional setup commands that will be executed via /bin/sh before initialization (optional)
        """
        self._log = logging.getLogger(__name__)

        self._requirements = []
        self.add_requirements(requirements)

        self._info_dict = {}
        if info_dict:
            self._info_dict = info_dict

        self._setup_script = setup_script

        self._virtual_files = {}
        self._stored_files = {}

        self.name = name

        self._log = logging.getLogger(str(self))

        self._update_default_metadata()

    @staticmethod
    def load(model_path: str, install_requirements: bool = False):
        """
        Load a pickled unified model.

        # Arguments
            model_path (string): Path to pickled unified model.
            install_requirements (boolean): If `True`, requirements will be automatically installed (optional).

        # Returns
        Initialized unified model.
        """
        log.info("Loading unified model: " + model_path)

        if os.path.isfile(model_path):
            unified_model_zip = zipfile.ZipFile(model_path, 'r')

            model_temp_folder = tempfile.mkdtemp()
            log.debug("Unpacking into temp path: " + model_temp_folder)

            # automatically remove temp directory if process exits
            def cleanup():
                log.info("Removing temp directory: " + model_temp_folder)
                shutil.rmtree(model_temp_folder)

            atexit.register(cleanup)

            # extract all files to temp folder
            unified_model_zip.extractall(model_temp_folder)
            unified_model_zip.close()
            model = UnifiedModel._load_from_folder(model_temp_folder, install_requirements=install_requirements)

            return model
        elif os.path.isdir(model_path):
            return UnifiedModel._load_from_folder(model_path, install_requirements=install_requirements)
        else:
            log.warning("Failed to load the model! Path is either a file nor a dir: " + str(model_path))
            return None

    @staticmethod
    def _load_from_folder(model_folder: str, install_requirements: bool = False):

        code_temp_dir = os.path.join(model_folder, UnifiedModel._CODE_BASE_DIR)
        data_temp_dir = os.path.join(model_folder, UnifiedModel._DATA_BASE_DIR)
        model_pkl_path = os.path.join(data_temp_dir, UnifiedModel._PICKLE_FILENAME)

        if not os.path.isfile(model_pkl_path):
            log.error("Model pickle file does not exist at path: " + str(model_pkl_path))
            return None

        # TODO remove modules from _requirements...?
        if install_requirements:
            log.info("Installing model requirements")
            # Execute setup.sh script if available
            setup_script = os.path.join(model_folder, UnifiedModel._SETUP_SCRIPT_FILENAME)
            if os.path.isfile(setup_script):
                os.chmod(setup_script, 0o777)
                if subprocess.check_call([setup_script]) > 0:
                    log.warning("Failed to execute setup script")
            else:
                log.debug("Setup script does not exist.")

            # Install dependencies from requirements.txt if available
            requirements_file = os.path.join(model_folder, UnifiedModel._REQUIREMENTS_FILENAME)
            if os.path.isfile(requirements_file):
                if subprocess.check_call([sys.executable, '-m', 'pip', 'install', '-r', requirements_file]) > 0:
                    log.warning("Failed to install requirements")
            else:
                log.debug("requirements file does not exist.")

        # extract code modules into temp folder

        if os.path.isdir(code_temp_dir):
            # append core modules to python path
            sys.path.append(code_temp_dir)
        else:
            log.debug("Code directory does not exist.")

        # add all data to stored files
        stored_files = {}

        if os.path.isdir(data_temp_dir):
            for dir_, _, files in os.walk(data_temp_dir):
                for file_name in files:
                    rel_dir = os.path.relpath(dir_, data_temp_dir)
                    if rel_dir != '.':
                        file_name = os.path.join(rel_dir, file_name)

                    if file_name != UnifiedModel._PICKLE_FILENAME:
                        # don't save the unified_model pickle file as stored file
                        stored_files[file_name] = os.path.join(dir_, file_name)
        else:
            log.debug("Data directory does not exist.")

        try:
            # ,  encoding='latin1', fix_imports=True
            model = pickle.load(open(model_pkl_path, "rb"))
            # provide information if requirements should be installed to loaded model
            model._install_requirements = install_requirements
        except Exception as e:
            log.error("Failed to unpickle model: " + str(e), e)
            if not install_requirements:
                log.info("Try to set install_requirements flag to true.")
            return None

        if not hasattr(model, 'predict'):
            log.warning("Model is not valid: predict method is missing!")

        model._stored_files = stored_files

        model._after_load()

        return model

    def save(self, output_path: str, compress: bool = False, executable: bool = False) -> str:
        """
        Save unified model as a single file to a given path.

        # Arguments
            output_path (string): Path to save the model.
            compress (boolean): If 'True', the model file will be compressed using zip deflated method (optional).
            executable (boolean): If 'True', the model file will be converted to an executable pyz file (optional).
        # Returns
        Full path to the saved model file.
        """

        self._save_model(output_path)

        self._log.info("Saving unified model to: " + output_path)

        # Set name based on save path if not already set
        if self.name is None:
            # use filename without extension as name
            self.name = simplify(get_file_name(output_path))

        # default is not compression
        compression_mode = zipfile.ZIP_STORED
        if compress:
            compression_mode = zipfile.ZIP_DEFLATED

        unified_model = zipfile.ZipFile(output_path, 'w', compression_mode)

        # save requirements
        pip_requirements = []
        for requirement in self._requirements:
            if isinstance(requirement, six.string_types):
                # add to pip requirements
                pip_requirements.append(requirement)
            else:
                # if not, assume that it is an import where we can get the module
                try:
                    req_module = inspect.getmodule(requirement)
                    if req_module:
                        # add module code into zipfile
                        absolute_path = self._get_path_to_library(req_module)
                        relative_path = os.path.basename(os.path.normpath(absolute_path))
                        for root, dirs, files in os.walk(absolute_path):
                            for filename in files:
                                file_path = os.path.join(root, filename)
                                destination_path = os.path.join(UnifiedModel._CODE_BASE_DIR,
                                                                relative_path,
                                                                os.path.relpath(file_path, absolute_path))
                                unified_model.write(file_path, arcname=destination_path)
                except Exception as ex:
                    self._log.info("Failed to add requirement: " + str(requirement), ex)
                    continue

        # save requirements to requirements text if provided
        if pip_requirements:
            requirements_str = "\n".join(pip_requirements)
            unified_model.writestr(UnifiedModel._REQUIREMENTS_FILENAME, requirements_str)

        # save info dict
        self._update_default_metadata()  # update info dict with default metadata before saving it
        info_dict_str = ""
        if self._info_dict:
            info_dict_str = json.dumps(self._info_dict, sort_keys=True, indent=4)
        unified_model.writestr(UnifiedModel._INFO_FILENAME, info_dict_str)

        # save setup script if provided
        if self._setup_script:
            setup_script_text = "#!/bin/sh\n" + self._setup_script
            unified_model.writestr(UnifiedModel._SETUP_SCRIPT_FILENAME, setup_script_text)

        # add __main__.py -> makes model zip executable
        unified_model.writestr("__main__.py",
                               "import os" +
                               "\nfrom unified_model import cli_handler, model_handler" +
                               "\nos.environ[model_handler.DEFAULT_MODEL_ENV] = os.path.dirname(__file__)" +
                               "\ncli_handler.cli()")

        # store saved files in unified model
        for file_name in self._stored_files:
            destination_path = os.path.join(UnifiedModel._DATA_BASE_DIR, file_name)
            unified_model.write(self._stored_files[file_name], arcname=destination_path)

        # Save model as pickle
        # remove logging
        self._log = None
        # recurse=True -> also serialize dependencies
        # recurse=False, byref=False, protocol=4
        unified_model.writestr(os.path.join(UnifiedModel._DATA_BASE_DIR, UnifiedModel._PICKLE_FILENAME),
                               pickle.dumps(self))

        # Finish writing model
        unified_model.close()

        if executable:
            temp_model = output_path + ".temp"
            os.rename(output_path, temp_model)
            self._convert_to_pyz(temp_model, output_path)
            os.remove(temp_model)

        # reinitialize the model so that it still can be used
        self._after_load()

        self._log.info("Successfully saved unified model.")
        return output_path

    def add_file(self, file_key: str, file_path: str):
        """
        Add a file to the model. The file will be bundled into the model file when the model is saved.

        # Arguments
            file_key (string): File name to identify the added file.
            file_path (string): Path to the file that should be added.
        """
        self._stored_files[file_key] = file_path

    def get_file(self, file_key: str) -> str or None:
        """
        Get a file by name that is bundled with the model.

        # Arguments
            file_key (string): File name to identify the added file.

        # Returns
        Full path to the requested file or `None` if the file does not exist.
        """
        if file_key in self._stored_files:
            return self._stored_files[file_key]
        else:
            self._log.warning(file_key + " could not be found in stored files.")
            return None

    def update_info(self, info_dict: dict):
        """
        Update the info dictionary.

        # Arguments
            info_dict (dict): Dictionary to add additional metadata about the model (optional)

        # Returns
        Info dictionary.
        """
        if self._info_dict is None:
            self._info_dict = {}
        self._info_dict.update(info_dict)

    def info(self):
        """
        Get the info dictionary that contains additional metadata about the model.

        # Returns
        Info dictionary.
        """
        if self._info_dict is None:
            return {}
        return self._info_dict

    @abc.abstractmethod
    def _init_model(self):
        """
        Called after the model is unpickled. Overwrite this method if additional initialization is required.
        """
        pass

    @abc.abstractmethod
    def _save_model(self, output_path: str):
        """
        Preparation for model saving. Overwrite this method if additional processing is required before the model is saved.
        """
        pass

    @abc.abstractmethod
    def _predict(self, data, **kwargs):
        """
        Implement this function with the logic to make a prediction on the given data item with the given model.
        # Arguments
            data (string or bytes): Input data.
            **kwargs: Provide additional keyword-based parameters.

        # Returns
        Predictions for the input data.

        # Raises
            NotImplementedError: Method is not implemented (please implement).
        """
        raise NotImplementedError('Method not implemented')

    def _validate_and_transform_input(self, data):
        if isinstance(data, pd.DataFrame):
            if self._DATA_COLUMN in data.columns and data.shape == (1, 1):
                # if dataframe only contains the data item, extract and return as string
                return str(data.iat[0, 0])
        return data

    def _validate_prediction_result(self, result):
        pass

    def predict(self, data, **kwargs):
        """
        Make a prediction on the given data item.

        # Arguments
            data (string or bytes): Input data.
            **kwargs: Provide additional keyword-based parameters.

        # Returns
        Predictions for the input data.

        # Raises
            NotImplementedError: Method is not implemented (please implement).
        """
        data = self._validate_and_transform_input(data)
        prediction = self._predict(data, **kwargs)
        self._validate_prediction_result(prediction)
        return prediction

    def predict_batch(self, data_batch: list, **kwargs):
        """
        Make a predictions on a batch of data items.

        # Arguments
            data_batch (list): List of data items
            **kwargs: Provide additional keyword-based parameters.

        # Returns
        List of predictions for a batch of data items.
        """
        # Gets in array - returns array
        if not hasattr(data_batch, "__len__"):
            raise ValueError("data_batch must be a list.")

        result = []
        for item in data_batch:
            result.append(self.predict(item, **kwargs))
        return result

    def evaluate(self, test_data: list, target_predictions: list, **kwargs) -> dict:
        """
        Evaluate this model with given test dataset.

        # Arguments
            test_data (list): List of data items used for the evaluations
            target_predictions (list): List of true predictions for test data
            **kwargs: Provide additional keyword-based parameters.

        # Returns
        Dictionary of evaluation metrics
        """

        raise NotImplementedError('Method not implemented. Please extend from the appropriate model type.')

    def _after_load(self):
        """
        Lifecycle method called after the model is unpickled.
        """
        # TODO do this directly in load method -> no need for this load method
        self._log = logging.getLogger(str(self))
        self._init_model()

    def add_requirements(self, requirements: list):
        """
        Add requirements to model.

        # Arguments
             requirements (list): List of dependencies. Can be either an imported module or a requirement installable via pip
        """
        if not self._requirements:
            self._requirements = []

        if not requirements:
            return

        for requirement in requirements:
            if requirement not in self._requirements:
                self._requirements.append(requirement)

    def _get_path_to_library(self, module) -> str:
        """
        Get the path to a imported module.

        # Arguments
            module (module): Imported python module

        # Returns
            Full path to the provided module.
        """
        try:
            root_package = module.__name__.split(".")[0]
            return module.__file__.split(root_package)[0] + root_package
        except Exception as e:
            self._log.warning("Failed to resolve path to module " + module.__name__ + ": " + str(e))

    def _update_default_metadata(self):
        if self._info_dict is None:
            self._info_dict = {}
        if self.name:
            self._info_dict["name"] = self.name
        self._info_dict["type"] = type(self).__name__
        self._info_dict["sub_type"] = self.__class__.__bases__[0].__name__

        self._info_dict["modification_date"] = datetime.datetime.now().strftime("%I:%M %p on %B %d, %Y")

        # keep creation date if it already exists
        if "creation_date" not in self._info_dict:
            self._info_dict["creation_date"] = self._info_dict["modification_date"]

        build_info = {"os": platform.platform(),
                      "lib_version": str(unfied_model_version),
                      "cloudpickle_version": str(pickle.__version__),
                      "python_version": platform.python_version(),
                      "python_compiler": platform.python_compiler(),
                      "python_impl": platform.python_implementation()}

        self._info_dict["build"] = build_info

        requirements = []
        for requirement in self._requirements:
            if isinstance(requirement, six.string_types):
                requirements.append(str(requirement) + " (pip)")
            else:
                try:
                    requirements.append(str(requirement.__name__) + " (import)")
                except:
                    requirements.append(str(requirement) + " (undefined)")

        self._info_dict["requirements"] = requirements
        self._info_dict["stored_files"] = list(self._stored_files.keys())

        # model signature
        input_sign = {
            "type": "undefined"
        }

        output_sign = {
            "type": "undefined"
        }

        self._info_dict["signature"] = {
            "input": input_sign,
            "output": output_sign
        }

    @staticmethod
    def _convert_to_pyz(model: zipfile.ZipFile, output_file_path: str, interpreter="/usr/bin/env python3") -> str:
        """
        Convert the given zip model into an valid executable Python ZIP Application

        # Arguments
            model (ZipFile): Model zip file
            output_file_path (string): Path to save the model.
            interpreter (string): Interpreter to use as shebang (optional)

        # Returns
        Full path to the converted model
        """
        # https://legacy.python.org/dev/peps/pep-0441/
        log.debug("Start conversion to PYZ")  # Silent logging since used as default
        model_temp_folder = tempfile.mkdtemp()

        if isinstance(model, UnifiedModel):
            saved_model = model.save(os.path.join(model_temp_folder, str(model)))
        elif os.path.exists(str(model)):
            saved_model = str(model)
        else:
            raise Exception("Could not find model for: " + str(model))

        if sys.version_info >= (3, 0):
            import zipapp
            zipapp.create_archive(saved_model, output_file_path, interpreter)
        else:
            # add shebang to model zip manually
            with open(output_file_path, 'wb') as pyz_file:
                pyz_file.write('#!' + str(interpreter) + "\n")
                with open(saved_model, 'rb') as model_zip:
                    shutil.copyfileobj(model_zip, pyz_file)
            os.chmod(output_file_path, 0o755)
            os.unlink(saved_model)

        shutil.rmtree(model_temp_folder)

        log.debug("Conversion to PYZ successful: " + output_file_path)  # Silent logging since used as default
        return output_file_path

    def __str__(self):
        if not self.name:
            return simplify(type(self).__name__)

        try:
            return self.name  # text_utils.truncate_middle(self.name, 50)
        except AttributeError:
            return simplify(type(self).__name__)

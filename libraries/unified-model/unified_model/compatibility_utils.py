import logging
import os
import shutil
import tempfile
import zipfile

import pandas as pd
import six

from unified_model import NotSupportedException, UnifiedModel
from unified_model.utils import ITEM_COLUMN, SCORE_COLUMN

log = logging.getLogger(__name__)

UNIFIED_MODEL_REPO_URL = "TODO"


class SklearnWrapper:
    # https://github.com/scikit-learn-contrib/sklearn-pandas
    # http://docs.seldon.io/prediction-pipeline.html
    # http://docs.seldon.io/python/modules/seldon/sklearn_estimator.html#SKLearnClassifier
    # http://docs.seldon.io/python/modules/seldon/pipeline/pandas_pipelines.html
    # https://keras.io/scikit-learn-api/
    # TODO add support for seldon sklearn pandas models?

    def __init__(self, unified_model):
        self.unified_model = unified_model
        self.classes_ = []

    def predict(self, X):
        """
        Perform prediction task on X.

        # Arguments
            X: ({array-like, sparse matrix}, shape = [n_samples, n_features]) Input vectors, where n_samples is the number of samples and n_features is the number of features.
            output_file_path (string): Path to save the model.

        # Returns
         y : array, shape = [n_samples]  or [n_samples, n_outputs]
            Predicted target values for X.
        """

        result = []
        for prediction in self.unified_model.predict_batch(X):
            if isinstance(prediction, pd.DataFrame) \
                    and ITEM_COLUMN in prediction.columns:
                if prediction.shape[0] == 0:
                    result.append(None)
                else:
                    # only return the best item
                    result.append(prediction[ITEM_COLUMN][0])
            else:
                result.append(prediction)

        return result

    def predict_proba(self, X):
        """
        Return probability estimates for the tests vectors X.

        # Arguments
            X: {array-like, sparse matrix}, shape = [n_samples, n_features] Input vectors, where n_samples is the number of samples and n_features is the number of features.
        # Returns
        P : array-like or list of array-lke of shape = [n_samples, n_classes] Returns the probability of the sample for each class in the model, where classes are ordered arithmetically, for each output.
        """

        result = []
        # get all
        for prediction in self.unified_model.predict_batch(X, limit=None):
            if isinstance(prediction, pd.DataFrame) \
                    and ITEM_COLUMN in prediction.columns \
                    and SCORE_COLUMN in prediction.columns:
                if prediction.shape[0] == 0:
                    result.append(None)
                result.append(prediction[SCORE_COLUMN].tolist())
                self.classes_ = prediction[ITEM_COLUMN].tolist()
            else:
                result.append(prediction)

        return result

    def score(self, X, y, **kwargs):
        # return self.unified_model.evaluate()
        # TODO implement scoring
        raise NotSupportedException("Model is not supported for this method")

    def get_params(self, deep=True):
        return self.unified_model.info()

    def set_params(self, **params):
        raise NotSupportedException("Not supported for Unified Models")

    def fit(self, X, y, sample_weight=None):
        raise NotSupportedException("Training is not supported for Unified Models")


class PickleWrapper:

    def __init__(self, unified_model):
        self.unified_model = unified_model

    def save(self, output_path):
        import cloudpickle
        with open(output_path, 'wb') as file:
            cloudpickle.dump(PickleWrapper(self.unified_model), file)
        log.info("Successfully saved model as pickle.")
        return output_path

    def __getstate__(self):
        temp_file = tempfile.NamedTemporaryFile()
        self.unified_model.save(temp_file.name)

        with open(temp_file.name, mode='rb') as model_file:
            self.virtual_model_file = model_file.read()

        del self.unified_model
        temp_file.close()
        return self.__dict__

    def __setstate__(self, d):
        self.__dict__ = d

        temp_file = tempfile.NamedTemporaryFile()
        temp_file.write(self.virtual_model_file)
        temp_file.flush()
        os.fsync(temp_file.fileno())

        self.unified_model = UnifiedModel.load(temp_file.name)
        del self.virtual_model_file

        temp_file.close()

    def model(self):
        return self.unified_model


def convert_to_mlflow(unified_model, output_path: str) -> str:
    """
    Convert the given unified model into a mlflow model.

    # Arguments
        unified_model (UnifiedModel or str): Unified model instance or path to model file
        output_path (string): Path to save the model.

    # Returns
    Full path to the converted model
    """
    # https://mlflow.org/docs/latest/models.html
    log.info("Start conversion to MLFlow")

    if isinstance(unified_model, UnifiedModel):
        pass
    elif os.path.exists(str(unified_model)):
        # load model instance
        unified_model = UnifiedModel.load(str(unified_model))
    else:
        raise Exception("Could not find model for: " + str(unified_model))

    if os.path.isdir(output_path) and len(os.listdir(output_path)) > 0:
        log.warning("Aborting conversion. Output directory is not empty: " + output_path)
        return output_path
    else:
        os.makedirs(output_path)

    model_temp_folder = tempfile.mkdtemp()
    # Save model to temp
    model_zip_path = unified_model.save(os.path.join(model_temp_folder, str(unified_model)))

    # extract to output path
    unified_model_zip = zipfile.ZipFile(model_zip_path, 'r')
    unified_model_zip.extractall(output_path)
    unified_model_zip.close()

    # unified_model_data:
    UNIFIED_MODEL_FOLDER = "unified_model"
    unified_model_folder_path = os.path.join(output_path, UNIFIED_MODEL_FOLDER)
    if not os.path.isdir(unified_model_folder_path):
        os.makedirs(unified_model_folder_path)

    for f in os.listdir(output_path):
        if f != UnifiedModel._CODE_BASE_DIR:
            shutil.move(os.path.join(output_path, f), unified_model_folder_path)

    # create necessary files
    CONDA_ENV = "conda.yml"
    LOADER_MODULE = "unified_model_loader"

    with open(os.path.join(output_path, "MLmodel"), "w") as text_file:
        text_file.write("flavors:"
                        "\n  python_function:"
                        "\n    code: " + UnifiedModel._CODE_BASE_DIR +
                        "\n    data: " + UNIFIED_MODEL_FOLDER +
                        "\n    env: " + CONDA_ENV +
                        "\n    loader_module: " + LOADER_MODULE)

    requirement_string = ""

    # req_file_path = os.path.join(output_path, UnifiedModel._REQUIREMENTS_FILENAME)
    # if os.path.isfile(req_file_path):
    #    with open(req_file_path, encoding='utf-8') as f:
    #        for line in f.readlines():
    #            requirement_string += "\n\t- " + str(line.rstrip())

    for requirement in unified_model._requirements:
        if isinstance(requirement, six.string_types):
            requirement_string += "\n - " + str(requirement)

    with open(os.path.join(output_path, CONDA_ENV), "w") as text_file:
        text_file.write("name: " + str(unified_model).lower().replace(" ", "_") +
                        "\nchannels:\n- conda-forge\n- defaults" +
                        "\ndependencies: " +
                        "\n- python>=3.6 " +
                        "\n- pip:\n - git+" + UNIFIED_MODEL_REPO_URL + requirement_string)

        code_folder = os.path.join(output_path, UnifiedModel._CODE_BASE_DIR)

        if not os.path.isdir(code_folder):
            os.makedirs(code_folder)

        # TODO adapt script
        with open(os.path.join(code_folder, LOADER_MODULE + ".py"), "w") as text_file:
            text_file.write("from unified_model import UnifiedModel\n\n" +
                            "def load_pyfunc(path):\n\treturn UnifiedModel.load(path, install_requirements=False)\n")

        shutil.rmtree(model_temp_folder)
        log.info("Conversion to MLFlow successful: " + output_path)
        return output_path


def convert_to_pipelineai(unified_model, output_path: str) -> str:
    """
    Convert the given unified model into a pipelineai model.

    # Arguments
        unified_model (UnifiedModel or str): Unified model instance or path to model file
        output_path (string): Path to save the model.

    # Returns
    Full path to the converted model
    """
    # https://github.com/PipelineAI/pipeline/tree/master/docs/quickstart/docker
    # https://github.com/PipelineAI/models/tree/master/scikit/mnist/model
    log.info("Start conversion to PipelineAI")

    if isinstance(unified_model, UnifiedModel):
        pass
    elif os.path.exists(str(unified_model)):
        # load model instance
        unified_model = UnifiedModel.load(str(unified_model))
    else:
        raise Exception("Could not find model for: " + str(unified_model))

    if os.path.isdir(output_path) and len(os.listdir(output_path)) > 0:
        log.warning("Aborting conversion. Output directory is not empty: " + output_path)
        return output_path
    else:
        os.makedirs(output_path)

    # Save model into folder
    unified_model_filename = str(unified_model)
    unified_model.save(os.path.join(output_path, unified_model_filename))

    # Required, but Empty is OK.
    with open(os.path.join(output_path, "pipeline_condarc"), "w") as text_file:
        text_file.write("")

    # Required, but Empty is OK.
    with open(os.path.join(output_path, "pipeline_modelserver.properties"), "w") as text_file:
        text_file.write("")

    # Required, but Empty is OK.
    setup_script = ""
    if unified_model._setup_script:
        setup_script = unified_model._setup_script
    with open(os.path.join(output_path, "pipeline_setup.sh"), "w") as text_file:
        text_file.write(setup_script)

    requirement_string = ""
    for requirement in unified_model._requirements:
        if isinstance(requirement, six.string_types):
            requirement_string += "\n - " + str(requirement)

    with open(os.path.join(output_path, "pipeline_conda_environment.yaml"), "w") as text_file:
        text_file.write("name: " + str(unified_model).lower().replace(" ", "_") +
                        "\nchannels:\n- conda-forge\n- defaults" +
                        "\ndependencies: " +
                        "\n- python>=3.6 " +
                        "\n- pip:\n - git+" + UNIFIED_MODEL_REPO_URL +
                        requirement_string)

    with open(os.path.join(output_path, "pipeline_invoke_python.py"), "w") as text_file:
        text_file.write("import os" +
                        "\nfrom unified_model import UnifiedModel" +
                        "\n\n_model = UnifiedModel.load(os.path.join(os.path.dirname(os.path.abspath(__file__)), '" + unified_model_filename + "'))" +
                        "\n\ndef invoke(request):\n\treturn _model.predict(request)")

    log.info("Conversion to PipelineAI successful: " + output_path)
    return output_path


def convert_to_pex(unified_model, output_file_path: str) -> str:
    """
    Convert the given unified model into an executable PEX file.

    # Arguments
        unified_model (UnifiedModel or str): Unified model instance or path to model file
        output_file_path (string): Path to save the model.

    # Returns
    Full path to the converted model
    """
    # https://gist.github.com/simeonf/062af826e79259bc7686
    log.info("Start conversion to PEX")

    if isinstance(unified_model, UnifiedModel):
        pass
    elif os.path.exists(str(unified_model)):
        # load model instance
        unified_model = UnifiedModel.load(str(unified_model))
    else:
        raise Exception("Could not find model for: " + str(unified_model))

    from git import Repo
    from pex.bin import pex

    model_temp_folder = tempfile.mkdtemp()

    model_instance_name = "model_instance"
    model_instance_folder = os.path.join(model_temp_folder, model_instance_name)
    os.makedirs(model_instance_folder)

    with open(os.path.join(model_instance_folder, "setup.py"), "w") as text_file:
        text_file.write("from distutils.core import setup " +
                        "\nsetup(name='" + model_instance_name + "'," +
                        "\n\tpackages=['" + model_instance_name + "']," +
                        "\n\tversion='1.0'," +
                        "\n\tpackage_data={'" + model_instance_name + "': ['" + model_instance_name + "']})")

    model_instance_package = os.path.join(model_instance_folder, model_instance_name)
    os.makedirs(model_instance_package)

    with open(os.path.join(model_instance_package, "__init__.py"), "w") as text_file:
        text_file.write("import os, pkg_resources" +
                        "\nfrom unified_model import cli_handler" +
                        "\nos.environ[cli_handler.DEFAULT_MODEL_PATH_ENV] = pkg_resources.resource_filename(__name__, '" + model_instance_name + "')" +
                        "\ncli_handler.cli()")

    with open(os.path.join(model_instance_package, "__main__.py"), "w") as text_file:
        text_file.write("")

    unified_model.save(os.path.join(model_instance_package, model_instance_name))

    lib_repo_folder = os.path.join(model_temp_folder, "unified-model")
    Repo.clone_from(UNIFIED_MODEL_REPO_URL, lib_repo_folder)

    parser, resolver_options_builder = pex.configure_clp()
    args = [lib_repo_folder, model_instance_folder, "--disable-cache"]
    for req in unified_model._requirements:
        if isinstance(req, six.string_types):
            args.append(req)
    options, reqs = parser.parse_args(args=args)
    pex_builder = pex.build_pex(reqs, options, resolver_options_builder)
    pex_builder.set_entry_point(model_instance_name)
    pex_builder.build(output_file_path)

    shutil.rmtree(model_temp_folder)

    log.info("Conversion to PEX successful: " + output_file_path)
    return output_file_path

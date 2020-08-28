from __future__ import print_function

import logging
import os

from unified_model import UnifiedModel

DEFAULT_MODEL_ENV = "MODEL_KEY"
INSTALL_REQUIREMENTS_ENV = "INSTALL_REQUIREMENTS"

log = logging.getLogger(__name__)


# https://stackoverflow.com/questions/31875/is-there-a-simple-elegant-way-to-define-singletons


def resolve_by_path(model_key):
    # mdoel key is same as model path
    return model_key


loaded_resources = {}
resource_loading = {}

default_model = None
key_resolver = resolve_by_path
install_req = True
initialized = False


def init(default_model_key: str = None, install_requirements: bool = True):
    """
    Initialize model handler. The model handler can be used to load, hold, and use multiple model instances.

    # Arguments
        default_model_key (string): Key of the default model (optional)
        install_requirements (boolean): If 'True', requirements of the model will be automatically installed (optional)
    """

    global default_model
    global install_req
    global initialized

    log.info("Initializing Model Handler")

    if default_model_key:
        log.info("Preload model: " + default_model_key)
        default_model = get_model(default_model_key)
    elif DEFAULT_MODEL_ENV in os.environ:
        model_key = os.environ[DEFAULT_MODEL_ENV]
        log.info("Preload model: " + model_key)
        default_model = get_model(model_key)

    install_req = install_requirements
    if INSTALL_REQUIREMENTS_ENV in os.environ:
        if not bool(os.environ[INSTALL_REQUIREMENTS_ENV]):
            install_req = False

    initialized = True


def get_model(model_key: str = None) -> UnifiedModel:
    """
    Get the model instance for the given key.

    # Arguments
        model_key (string): Key of the model. If 'None', return the default model.

    # Returns
    Unified model instance.

    # Raises
        Exception: Model failed to load.
    """

    global loaded_resources
    global resource_loading
    global install_req

    if not model_key:
        if default_model is None:
            raise Exception("Model key not provided and no default model is set.")
        else:
            return default_model

    if model_key in loaded_resources:
        return loaded_resources[model_key]

    if model_key in resource_loading and resource_loading[model_key]:
        raise Exception("Model is currently loading. Please try again.")

    model_path = key_resolver(model_key)
    if not model_path:
        raise Exception("Model could not be loaded: " + model_key)

    if not os.path.exists(model_path):
        raise Exception("Could not find model at path: " + model_path)

    resource_loading[model_key] = True
    model_instance = UnifiedModel.load(model_path, install_requirements=install_req)

    if model_instance is None:
        resource_loading[model_key] = False
        raise Exception("Failed to load model from path: " + model_path)

    loaded_resources[model_key] = model_instance
    resource_loading[model_key] = False
    return model_instance


def predict(data, model: str = None, **kwargs):
    """
    Make a prediction on the given data item.

    # Arguments
        data (string or bytes): Input data.
        model (string): Key of the selected model. If 'None', the default model will be used (optional)
        **kwargs: Provide additional keyword-based parameters.

    # Returns
    Predictions for the input data.
    """

    model_instance = get_model(model)
    log.debug("Predict with " + str(model_instance))
    return model_instance.predict(data, **kwargs)


def info(model: str = None) -> dict:
    """
    Get the info dictionary that contains additional metadata about the model.

    # Arguments
        model (string): Key of the selected model. If 'None', the default model will be used (optional)

    # Returns
    Info dictionary.
    """
    model_instance = get_model(model)
    log.debug("Get info for " + str(model_instance))
    return model_instance.info()

from datetime import timedelta
from typing import Any, List, Optional

import pydantic
from contaxy.schema.deployment import DeploymentStatus
from contaxy.schema.shared import MAX_DISPLAY_NAME_LENGTH
from pydantic import BaseModel, Field

from mlflow.config import MLFlowImage, settings


def check_if_in_options(field_name: str, options: List) -> classmethod:
    """Create a pydantic validator that checks if the given field is one of the valid options.

    :param field_name: The name of the field to validate
    :param options: The list of valid options. If it is emtpy, all values are allowed!
    :return:
    """

    def validate(value: Any) -> Any:
        if value is None:
            value = timedelta(seconds=0)
        if len(options) > 0 and value not in options:
            raise ValueError(
                f"The field '{field_name}' has value {value} which is not "
                f"in the list of the allowed values {options}!"
            )
        return value

    return pydantic.validator(field_name, allow_reuse=True)(validate)


class MLFlowCompute(BaseModel):
    cpus: int = Field(
        settings.MLFLOW_CPUS_DEFAULT,
        example=2,
        ge=0,
        le=settings.MLFLOW_CPUS_MAX,
        description="CPUs available to MLFlow server.",
    )
    _validate_cpu_options: classmethod = check_if_in_options(
        "cpus", settings.MLFLOW_CPUS_OPTIONS  # type: ignore
    )

    memory: int = Field(
        settings.MLFLOW_MEMORY_MB_DEFAULT,
        example=4000,
        ge=5,  # 4 is the minimal RAM needed for containers
        le=settings.MLFLOW_MEMORY_MB_MAX,
        description="Memory available to this MLFlow server.",
    )
    _validate_memory_options: classmethod = check_if_in_options(
        "memory", settings.MLFLOW_MEMORY_MB_OPTIONS  # type: ignore
    )


class MLFlowBase(BaseModel):
    display_name: str = Field(
        "Default MLFlow Server",
        max_length=MAX_DISPLAY_NAME_LENGTH,
        description="A user-defined human-readable name of the MLFlow server. The name can be up to 128 characters long and can consist of any UTF-8 character.",
    )
    container_image: str = Field(
        settings.MLFLOW_IMAGE_DEFAULT.image,
        example="mlserver",
        max_length=2000,
        description="The container image used for this workspace.",
    )
    compute: MLFlowCompute = Field(
        MLFlowCompute(),
        description="Compute requirements for this workspace.",
    )
    idle_timeout: Optional[timedelta] = Field(
        timedelta(settings.MLFLOW_IDLE_TIMEOUT_DEFAULT),
        description="Time after which the ML Flow server is considered idling and will be stopped during the next idle check."
        "If set to None, the ML Flow server will never be considered idling."
        "Can be specified as seconds or ISO 8601 time delta.",
    )
    _validate_idle_timeout_options: classmethod = check_if_in_options(
        "idle_timeout",
        [timedelta(seconds=t)
         for t in settings.MLFLOW_IDLE_TIMEOUT_OPTIONS],  # type: ignore
    )
    clear_volume_on_stop: bool = Field(
        False,
        description="If set to true, the /mlflow volume will be cleared on service stop.",
    )


class MLFlowInput(MLFlowBase):
    is_stopped: bool = Field(
        False,
        description="If set to true, the mlflow server will be created in the 'stopped' status.",
    )


class MLFlowUpdate(MLFlowBase):
    pass


class MLFlow(MLFlowBase):
    id: str = Field(
        ...,
        example="ac9ldprwdi68oihk34jli3kdp",
        description="The mlflow server id uniquely identifies a mlflow server.",
    )
    status: Optional[DeploymentStatus] = Field(
        None,
        example=DeploymentStatus.RUNNING,
        description="The status of this mlflow server.",
    )
    access_url: Optional[str] = Field(
        None,
        example="/projects/project-id/services/mlflow-server--id/access/8080b",
        description="Relative contaxy url that provides access to the mlflow server.",
    )


class MLFlowConfigOptions(BaseModel):
    display_name_default: str = Field(
        ...,
        description="The default display name that should be used for new mlflow servers.",
    )

    container_image_default: MLFlowImage = Field(
        ..., description="The default image that should be used for new mlflow servers."
    )
    container_image_options: List[MLFlowImage] = Field(
        [],
        description="The list of images that are allowed for mlflow servers. An empty list means no restrictions!",
    )

    cpus_default: int = Field(
        ...,
        description="The default number of cpus that should be used for new mlflow servers.",
    )
    cpus_max: int = Field(
        ..., description="The maximum number of cpus that are allowed for mlflow servers."
    )
    cpus_options: List[int] = Field(
        [],
        description="The list of CPU configurations that are allowed for mlflow servers. An empty list means no restrictions!",
    )

    memory_default: int = Field(
        ...,
        description="The default amount of memory that should be used for new mlflow servers. Unit MB.",
    )
    memory_max: int = Field(
        ...,
        description="The maximum amount of memory that is allowed for mlflow servers. Unit MB.",
    )
    memory_options: List[int] = Field(
        [],
        description="The list of memory configurations that are allowed for mlflow servers. An empty list means no restrictions! Unit MB.",
    )

    idle_timeout_default: int = Field(
        ...,
        description="The default idle timeout that should be used for new mlflow servers. Zero means no timeout.",
    )
    idle_timeout_options: List[int] = Field(
        ...,
        description="The list of idle timeout configurations that are allowed for mlflow servers."
        "An empty list means mlflow never have an idle timeout set!",
    )
    always_clear_volume_on_stop: bool = Field(
        ...,
        description="If set to true, the volume of the mlflow server will always be cleared when the server is stopped.",
    )

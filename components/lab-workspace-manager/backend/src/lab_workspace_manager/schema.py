from datetime import timedelta
from typing import Any, List, Optional

import pydantic
from contaxy.schema.deployment import DeploymentStatus
from contaxy.schema.shared import MAX_DISPLAY_NAME_LENGTH
from pydantic import BaseModel, Field

from lab_workspace_manager.config import WorkspaceImage, settings


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


class WorkspaceCompute(BaseModel):
    cpus: int = Field(
        settings.WORKSPACE_CPUS_DEFAULT,
        example=2,
        ge=0,
        le=settings.WORKSPACE_CPUS_MAX,
        description="CPUs available to this workspace.",
    )
    _validate_cpu_options: classmethod = check_if_in_options(
        "cpus", settings.WORKSPACE_CPUS_OPTIONS  # type: ignore
    )

    memory: int = Field(
        settings.WORKSPACE_MEMORY_MB_DEFAULT,
        example=4000,
        ge=5,  # 4 is the minimal RAM needed for containers
        le=settings.WORKSPACE_MEMORY_MB_MAX,
        description="Memory available to this workspace.",
    )
    _validate_memory_options: classmethod = check_if_in_options(
        "memory", settings.WORKSPACE_MEMORY_MB_OPTIONS  # type: ignore
    )


class WorkspaceBase(BaseModel):
    display_name: str = Field(
        "Default Workspace",
        max_length=MAX_DISPLAY_NAME_LENGTH,
        description="A user-defined human-readable name of the workspace. The name can be up to 128 characters long and can consist of any UTF-8 character.",
    )
    container_image: str = Field(
        settings.WORKSPACE_IMAGE_DEFAULT.image,
        example="mltooling/ml-workspace-minimal:latest",
        max_length=2000,
        description="The container image used for this workspace.",
    )
    compute: WorkspaceCompute = Field(
        WorkspaceCompute(),
        description="Compute requirements for this workspace.",
    )
    idle_timeout: Optional[timedelta] = Field(
        timedelta(seconds=settings.WORKSPACE_IDLE_TIMEOUT_DEFAULT),
        description="Time after which the workspace is considered idling and will be stopped during the next idle check."
        "If set to None, the workspace will never be considered idling."
        "Can be specified as seconds or ISO 8601 time delta.",
    )
    _validate_idle_timeout_options: classmethod = check_if_in_options(
        "idle_timeout",
        [timedelta(seconds=t) for t in settings.WORKSPACE_IDLE_TIMEOUT_OPTIONS],  # type: ignore
    )
    clear_volume_on_stop: bool = Field(
        False,
        description="If set to true, the /workspace volume will be cleared on service stop.",
    )


class WorkspaceInput(WorkspaceBase):
    is_stopped: bool = Field(
        False,
        description="If set to true, the workspace will be created in the 'stopped' status.",
    )


class WorkspaceUpdate(WorkspaceBase):
    pass


class Workspace(WorkspaceBase):
    id: str = Field(
        ...,
        example="ac9ldprwdi68oihk34jli3kdp",
        description="The workspace id uniquely identifies a workspace.",
    )
    status: Optional[DeploymentStatus] = Field(
        None,
        example=DeploymentStatus.RUNNING,
        description="The status of this workspace.",
    )
    access_url: Optional[str] = Field(
        None,
        example="/projects/project-id/services/workspace-id/access/8080b",
        description="Relative contaxy url that provides access to the workspace.",
    )


class WorkspaceConfigOptions(BaseModel):
    display_name_default: str = Field(
        ...,
        description="The default display name that should be used for new workspaces.",
    )

    container_image_default: WorkspaceImage = Field(
        ..., description="The default image that should be used for new workspaces."
    )
    container_image_options: List[WorkspaceImage] = Field(
        [],
        description="The list of images that are allowed for workspaces. An empty list means no restrictions!",
    )

    cpus_default: int = Field(
        ...,
        description="The default number of cpus that should be used for new workspaces.",
    )
    cpus_max: int = Field(
        ..., description="The maximum number of cpus that are allowed for workspaces."
    )
    cpus_options: List[int] = Field(
        [],
        description="The list of CPU configurations that are allowed for workspaces. An empty list means no restrictions!",
    )

    memory_default: int = Field(
        ...,
        description="The default amount of memory that should be used for new workspaces. Unit MB.",
    )
    memory_max: int = Field(
        ...,
        description="The maximum amount of memory that is allowed for workspaces. Unit MB.",
    )
    memory_options: List[int] = Field(
        [],
        description="The list of memory configurations that are allowed for workspaces. An empty list means no restrictions! Unit MB.",
    )

    idle_timeout_default: int = Field(
        ...,
        description="The default idle timeout that should be used for new workspace. Zero means no timeout.",
    )
    idle_timeout_options: List[int] = Field(
        ...,
        description="The list of idle timeout configurations that are allowed for workspaces."
        "An empty list means workspaces never have an idle timeout set!",
    )
    always_clear_volume_on_stop: bool = Field(
        ...,
        description="If set to true, the volume of the workspace will always be cleared when the workspace is stopped.",
    )

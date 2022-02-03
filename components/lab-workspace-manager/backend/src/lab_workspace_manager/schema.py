from typing import List, Optional

from contaxy.schema.deployment import DeploymentStatus
from contaxy.schema.shared import MAX_DISPLAY_NAME_LENGTH
from pydantic import BaseModel, Field

from lab_workspace_manager.config import settings


class WorkspaceCompute(BaseModel):
    cpus: int = Field(
        settings.WORKSPACE_CPUS_DEFAULT,
        example=2,
        ge=0,
        description="CPUs available to this workspace.",
    )
    memory: int = Field(
        settings.WORKSPACE_MEMORY_MB_DEFAULT,
        example=4000,
        ge=5,  # 4 is the minimal RAM needed for containers
        description="Memory available to this workspace.",
    )


class WorkspaceBase(BaseModel):
    display_name: str = Field(
        "Default Workspace",
        max_length=MAX_DISPLAY_NAME_LENGTH,
        description="A user-defined human-readable name of the workspace. The name can be up to 128 characters long and can consist of any UTF-8 character.",
    )
    container_image: str = Field(
        "mltooling/ml-workspace-minimal:latest",
        example="mltooling/ml-workspace-minimal:latest",
        max_length=2000,
        description="The container image used for this workspace.",
    )
    compute: WorkspaceCompute = Field(
        WorkspaceCompute(),
        description="Compute requirements for this workspace.",
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

    container_image_default: str = Field(
        ..., description="The default image that should be used for new workspaces."
    )
    container_image_options: List[str] = Field(
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

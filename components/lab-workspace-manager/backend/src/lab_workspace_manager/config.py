from typing import List, Union

from pydantic import BaseModel, BaseSettings, Field, validator


class WorkspaceImage(BaseModel):
    image: str
    display_name: str


class WorkspaceManagerSettings(BaseSettings):
    # Settings passed by contaxy
    SELF_ACCESS_URL: str = Field("", env="CONTAXY_SERVICE_URL")
    SELF_DEPLOYMENT_NAME: str = Field("", env="CONTAXY_DEPLOYMENT_NAME")

    # Settings that can be configured by the user when creating a workspace
    # Should contain a list of allowed values either as json or comma separated list
    WORKSPACE_MEMORY_MB_DEFAULT: int = 1000
    WORKSPACE_MEMORY_MB_MAX: int = 8000
    WORKSPACE_MEMORY_MB_OPTIONS: Union[str, List[int]] = []

    WORKSPACE_CPUS_DEFAULT: int = 1
    WORKSPACE_CPUS_MAX: int = 4
    WORKSPACE_CPUS_OPTIONS: Union[str, List[int]] = []

    WORKSPACE_IDLE_TIMEOUT_DEFAULT: int = 0  # Zero means no idle timeout
    WORKSPACE_IDLE_TIMEOUT_OPTIONS: Union[str, List[int]] = []
    WORKSPACE_ALWAYS_CLEAR_VOLUME_ON_STOP: bool = False

    WORKSPACE_VOLUME_SIZE: int = 5000
    WORKSPACE_CONTAINER_SIZE: int = 5000

    WORKSPACE_IMAGE_DEFAULT: WorkspaceImage = WorkspaceImage(
        image="mltooling/ml-workspace-minimal", display_name="Default Minimal Workspace"
    )
    WORKSPACE_IMAGE_OPTIONS: List[WorkspaceImage] = []

    @validator(
        "WORKSPACE_MEMORY_MB_OPTIONS",
        "WORKSPACE_CPUS_OPTIONS",
        "WORKSPACE_IDLE_TIMEOUT_OPTIONS",
        pre=True,
        allow_reuse=True,
    )
    def _parse_int_list(cls, int_list: Union[str, List[int]]) -> Union[str, List[int]]:
        if isinstance(int_list, str):
            int_list = [int(item.strip()) for item in int_list.split(",")]  # type: ignore
        return sorted(int_list)


settings = WorkspaceManagerSettings()

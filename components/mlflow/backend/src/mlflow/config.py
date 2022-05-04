from typing import List, Union

from pydantic import BaseModel, BaseSettings, Field, validator


class MLFlowImage(BaseModel):
    image: str
    display_name: str


class MLFlowManagerSettings(BaseSettings):
    # Settings passed by contaxy
    SELF_ACCESS_URL: str = Field("", env="CONTAXY_SERVICE_URL")
    SELF_DEPLOYMENT_NAME: str = Field("", env="CONTAXY_DEPLOYMENT_NAME")

    # Settings that can be configured by the user when creating a mlflow server
    # Should contain a list of allowed values either as json or comma separated list
    MLFLOW_MEMORY_MB_DEFAULT: int = 1000
    MLFLOW_MEMORY_MB_MAX: int = 8000
    MLFLOW_MEMORY_MB_OPTIONS: Union[str, List[int]] = []

    MLFLOW_CPUS_DEFAULT: int = 1
    MLFLOW_CPUS_MAX: int = 4
    MLFLOW_CPUS_OPTIONS: Union[str, List[int]] = []

    MLFLOW_IDLE_TIMEOUT_DEFAULT: int = 0  # Zero means no idle timeout
    MLFLOW_IDLE_TIMEOUT_OPTIONS: Union[str, List[int]] = []
    MLFLOW_ALWAYS_CLEAR_VOLUME_ON_STOP: bool = False

    MLFLOW_VOLUME_SIZE: int = 5000
    MLFLOW_CONTAINER_SIZE: int = 5000

    MLFLOW_IMAGE_DEFAULT: MLFlowImage = MLFlowImage(
        image="mlserver", display_name="Default ML Flow Server image"
    )
    MLFLOW_IMAGE_OPTIONS: List[MLFlowImage] = []

    @validator(
        "MLFLOW_MEMORY_MB_OPTIONS",
        "MLFLOW_CPUS_OPTIONS",
        "MLFLOW_IDLE_TIMEOUT_OPTIONS",
        pre=True,
        allow_reuse=True,
    )
    def _parse_int_list(cls, int_list: Union[str, List[int]]) -> Union[str, List[int]]:
        if isinstance(int_list, str):
            int_list = [int(item.strip())
                        for item in int_list.split(",")]  # type: ignore
        return sorted(int_list)


settings = MLFlowManagerSettings()

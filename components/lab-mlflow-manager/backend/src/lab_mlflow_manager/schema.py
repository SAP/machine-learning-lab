from datetime import timedelta
from typing import Optional

from contaxy.schema.deployment import DeploymentStatus
from pydantic import BaseModel, Field

from lab_mlflow_manager.config import settings


class MLFlowServer(BaseModel):
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
        example="/projects/project-id/services/mlflow-server-id/access/8080b",
        description="Relative contaxy url that provides access to the mlflow server.",
    )
    container_image: str = Field(
        settings.MLFLOW_SERVER_IMAGE,
        example="mlserver",
        max_length=2000,
        description="The container image used for this workspace.",
    )
    idle_timeout: Optional[timedelta] = Field(
        timedelta(settings.MLFLOW_SERVER_IDLE_TIMEOUT),
        description="Time after which the MLflow server is considered idling and will be stopped during the next idle check."
        "If set to None, the MLflow server will never be considered idling."
        "Can be specified as seconds or ISO 8601 time delta.",
    )

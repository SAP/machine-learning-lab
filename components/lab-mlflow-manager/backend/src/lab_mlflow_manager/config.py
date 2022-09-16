from pydantic import BaseSettings


class MLFlowManagerSettings(BaseSettings):
    # Name of the image that should be started for each project and runs
    # the mlflow server with the mlflow-mllab plugin installed
    MLFLOW_SERVER_IMAGE: str = "lab-mlflow-server"
    # Time in seconds after which the MLflow server should be stopped if there is no interaction
    MLFLOW_SERVER_IDLE_TIMEOUT: int = (
        60 * 60 * 24 * 1
    )  # Shut down after 1 day of inactivity


settings = MLFlowManagerSettings()

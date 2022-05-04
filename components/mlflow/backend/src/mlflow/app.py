from datetime import timedelta
import os
from typing import Any

from contaxy.operations.components import ComponentOperations
from contaxy.operations import AuthOperations
from contaxy.schema.auth import USER_ID_PARAM, AccessLevel
from contaxy.schema import Service, ServiceInput
from contaxy.schema.exceptions import CREATE_RESOURCE_RESPONSES
from contaxy.utils import fastapi_utils
from contaxy.schema.exceptions import (
    ResourceAlreadyExistsError,
)

from fastapi import Depends, FastAPI, status
from loguru import logger
from starlette.middleware.cors import CORSMiddleware

from mlflow.utils import CONTAXY_API_ENDPOINT, get_component_manager
from mlflow.config import settings
from mlflow.schema import (
    MLFlow,
    MLFlowInput,
    MLFlowCompute
)


LABEL_EXTENSION_DEPLOYMENT_TYPE = "ctxy.workspaceExtension.deploymentType"

app = FastAPI()
# Patch FastAPI to allow relative path resolution.
fastapi_utils.patch_fastapi(app)
# Allow CORS configuration
if "BACKEND_CORS_ORIGINS" in os.environ:
    app.add_middleware(
        CORSMiddleware,
        allow_origins=os.environ["BACKEND_CORS_ORIGINS"].split(","),
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )


@app.get(
    "/users/{user_id}/test",
    summary="Test endpoint",
    status_code=status.HTTP_200_OK,
    responses={**CREATE_RESOURCE_RESPONSES},
)
def example_endpoint(
    user_id: str = USER_ID_PARAM,
    component_manager: ComponentOperations = Depends(get_component_manager),
) -> Any:
    logger.info(user_id)
    user = component_manager.get_auth_manager().get_user(user_id)
    return user.username


@app.post(
    "/users/{user_id}/mlserver",
    summary="Create a new ML server for the user.",
    status_code=status.HTTP_200_OK,
    response_model=MLFlow,
    responses={**CREATE_RESOURCE_RESPONSES},
)
def deploy_workspace(
    mlflow_input: MLFlowInput,
    user_id: str = USER_ID_PARAM,
    component_manager: ComponentOperations = Depends(get_component_manager),
) -> Any:
    """Create a new ML server by creating a Contaxy service with a mlflow server image in the personal project."""
    logger.debug(f"Deploy workspace request for user {user_id}: {mlflow_input}")
    user_token = request_user_token(user_id, component_manager.get_auth_manager())
    service_input = create_mlflow_server_service_input(mlflow_input, user_token)
    try:
        # Use the user's project which has the same id as the user
        service = component_manager.get_service_manager().deploy_service(
            project_id=user_id, service_input=service_input
        )
        logger.debug(
            f"Successfully created workspace service with name "
            f"{service.display_name} and id {service.id}"
        )
        return create_mlflow_server_from_service(service)
    except ResourceAlreadyExistsError:
        raise ResourceAlreadyExistsError(
            f"A workspace with the name {mlflow_input.display_name} already exists for user {user_id}!"
        )


def request_user_token(user_id: str, auth_manager: AuthOperations) -> str:
    return auth_manager.get_user_token(user_id=user_id, access_level=AccessLevel.WRITE)


def create_mlflow_server_service_input(
    mlflow_input: MLFlowInput, user_token: str
) -> ServiceInput:
    return ServiceInput(
        container_image=mlflow_input.container_image,
        display_name=f"ML Flow {mlflow_input.display_name}",
        endpoints=["8080b"],
        parameters={
            "WORKSPACE_BASE_URL": "{env.CONTAXY_SERVICE_URL}",
            "SSH_JUMPHOST_TARGET": "{env.CONTAXY_DEPLOYMENT_NAME}",
            "SELF_ACCESS_TOKEN": "{env.CONTAXY_API_TOKEN}",
            "LAB_API_ENDPOINT": "{env.CONTAXY_API_ENDPOINT}",
            "LAB_API_TOKEN": user_token,
        },
        metadata={LABEL_EXTENSION_DEPLOYMENT_TYPE: "mlflow"},
        compute={
            "volume_path": "/mlflow",
            "max_cpus": mlflow_input.compute.cpus,
            "min_cpus": compute_min_cpu(mlflow_input.compute.cpus),
            "max_memory": mlflow_input.compute.memory,
            "min_memory": compute_min_memory(mlflow_input.compute.memory),
            "max_volume_size": settings.MLFLOW_VOLUME_SIZE,
            "max_container_size": settings.MLFLOW_CONTAINER_SIZE,
        },
        is_stopped=mlflow_input.is_stopped,
        idle_timeout=mlflow_input.idle_timeout
        if mlflow_input.idle_timeout != timedelta(0)
        else None,
        clear_volume_on_stop=True
        if settings.MLFLOW_ALWAYS_CLEAR_VOLUME_ON_STOP
        else mlflow_input.clear_volume_on_stop,
    )


def create_mlflow_server_from_service(service: Service) -> MLFlow:
    access_url = None
    if service.status == "running":
        project_id = service.metadata["ctxy.projectName"]
        workspace_id = service.id
        access_url = f"/projects/{project_id}/services/{workspace_id}/access/8080b"
    compute = MLFlowCompute()
    if service.compute.max_cpus:
        compute.cpus = service.compute.max_cpus
    if service.compute.max_memory:
        compute.memory = service.compute.max_memory
    return MLFlow(
        id=service.id,
        display_name=service.display_name[len("ML Flow ") :],
        container_image=service.container_image,
        compute=compute,
        idle_timeout=service.idle_timeout,
        clear_volume_on_stop=service.clear_volume_on_stop,
        status=service.status,
        access_url=access_url,
    )


def compute_min_cpu(max_cpus: int) -> float:
    # Only reserve halve of the CPU per workspace as CPU resources can be easily shared
    return max_cpus * 0.5


def compute_min_memory(max_memory: int) -> int:
    # Reserve 80% of the workspace memory. This allows some over committing on the node but hopefully does not cause
    # pods to be evicted (would happen if many workspaces use their max memory limit).
    return int(max_memory * 0.75)


if __name__ == "__main__":
    import uvicorn

    if not CONTAXY_API_ENDPOINT:
        raise RuntimeError("CONTAXY_API_ENDPOINT must be set")

    # Prevent duplicated logs
    log_config = uvicorn.config.LOGGING_CONFIG
    log_config["loggers"]["uvicorn"]["propagate"] = False
    uvicorn.run(
        "mlflow.app:app",
        host="localhost",
        port=int(os.getenv("PORT", 8080)),
        log_level="info",
        reload=True,
        log_config=log_config,
    )

from datetime import timedelta
import os
from typing import Any, List, Optional
from urllib import parse

from contaxy.operations.components import ComponentOperations
from contaxy.utils.auth_utils import get_api_token

from contaxy.schema import Service, ServiceInput
from contaxy.schema.auth import USER_ID_PARAM
from contaxy.schema.deployment import (
    ACTION_START,
    SERVICE_ID_PARAM,
)
from contaxy.schema.exceptions import (
    CREATE_RESOURCE_RESPONSES,
    ClientValueError,
    ResourceAlreadyExistsError,
)

from contaxy.utils import fastapi_utils

from fastapi import Depends, FastAPI, Query, Response, status
from loguru import logger
from starlette.middleware.cors import CORSMiddleware

from lab_mlflow_manager.utils import CONTAXY_API_ENDPOINT, get_component_manager
from lab_mlflow_manager.config import settings
from lab_mlflow_manager.schema import MLFlow, MLFlowInput, MLFlowCompute

LABEL_EXTENSION_DEPLOYMENT_TYPE = "ctxy.mlflowExtension.deploymentType"

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
    "/projects/{project_id}/mlflow-server",
    summary="Create a new ML Flow server for project.",
    status_code=status.HTTP_200_OK,
    response_model=MLFlow,
    responses={**CREATE_RESOURCE_RESPONSES},
)
def deploy_mlflow_server(
    mlflow_input: MLFlowInput,
    project_id: str,
    component_manager: ComponentOperations = Depends(get_component_manager),
    token: str = Depends(get_api_token),
) -> Any:
    """Create a new ML server by creating a Contaxy service with a mlflow server image in the personal project."""
    logger.debug(
        f"Deploy ML Flow server request for project {project_id}: {mlflow_input}"
    )
    host = parse.urlparse(os.getenv("CONTAXY_API_ENDPOINT")).netloc
    service_input = create_mlflow_server_service_input(
        mlflow_input, token, project_id, host
    )  # type: ignore
    try:
        service = component_manager.get_service_manager().deploy_service(
            project_id=project_id, service_input=service_input
        )
        logger.debug(
            f"Successfully created workspace service with name "
            f"{service.display_name} and id {service.id}"
        )
        return create_mlflow_server_from_service(service)
    except ResourceAlreadyExistsError:
        raise ResourceAlreadyExistsError(
            f"An ML Flow server with the name {mlflow_input.display_name} already exists for project {project_id}!"
        )


@app.get(
    "/projects/{project_id}/mlserver/{mlserver_id}",
    summary="Get information about a specific ML Flow server.",
    status_code=status.HTTP_200_OK,
    response_model=MLFlow,
)
def get_mlflow_server(
    project_id: str,
    mlserver_id: str = SERVICE_ID_PARAM,
    component_manager: ComponentOperations = Depends(get_component_manager),
) -> Any:
    logger.info(
        f"Get ML Flow server for project {project_id} with server id {mlserver_id}."
    )

    service = component_manager.get_service_manager().get_service_metadata(
        project_id, mlserver_id
    )
    if not is_mlflow_service(service):
        raise ClientValueError(
            f"The service with id {mlserver_id} is not an ML Flow server!"
        )

    return create_mlflow_server_from_service(service)


@app.post(
    "/projects/{project_id}/mlflow-server/{mlflow_server_id}/start",
    summary="Start the specified ML Flow server if it is stopped.",
    status_code=status.HTTP_204_NO_CONTENT,
)
def start_mlflow_server(
    project_id: str,
    mlflow_server_id: str = SERVICE_ID_PARAM,
    component_manager: ComponentOperations = Depends(get_component_manager),
) -> Any:
    logger.debug(
        "Start ML Flow server request for project {} and mlflow server {}".format(
            project_id, mlflow_server_id
        )
    )
    component_manager.get_service_manager().execute_service_action(
        project_id=project_id, service_id=mlflow_server_id, action_id=ACTION_START
    )
    return Response(status_code=status.HTTP_204_NO_CONTENT)


@app.get(
    "/projects/{project_id}/mlflow-server",
    summary="Get a list of all ML Flow servers for the project",
    status_code=status.HTTP_200_OK,
    response_model=List[MLFlow],
)
def list_mlflow_servers(
    project_id: str,
    component_manager: ComponentOperations = Depends(get_component_manager),
) -> Any:
    logger.info(f"List ML Flow servers for project {project_id}")

    services = component_manager.get_service_manager().list_services(
        project_id=project_id
    )
    workspaces = [
        create_mlflow_server_from_service(service)
        for service in services
        if is_mlflow_service(service)
    ]
    return workspaces


@app.delete(
    "/projects/{project_id}/mlflow-server/{service_id}",
    summary="Delete the specified ML Flow server.",
    status_code=status.HTTP_204_NO_CONTENT,
)
def delete_mlflow_server(
    project_id: str,
    mlflow_server_id: str = SERVICE_ID_PARAM,
    delete_volumes: Optional[bool] = Query(
        False, description="Delete all volumes associated with the deployment."
    ),
    component_manager: ComponentOperations = Depends(get_component_manager),
) -> Any:
    logger.debug(
        f"Delete workspace request for project {project_id} with workspace id {mlflow_server_id}."
    )

    workspace = get_mlflow_server(project_id, mlflow_server_id, component_manager)

    component_manager.get_service_manager().delete_service(
        project_id, workspace.id, delete_volumes
    )
    return Response(status_code=status.HTTP_204_NO_CONTENT)


def create_mlflow_server_service_input(
    mlflow_input: MLFlowInput,
    lab_api_token: str,
    project_id: str,
    host: str,
) -> ServiceInput:
    return ServiceInput(
        container_image=mlflow_input.container_image,
        display_name=f"ML Flow {mlflow_input.display_name}",
        endpoints=["5001b"],
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
        parameters={
            "LAB_API_TOKEN": lab_api_token,
            "PROJECT_ID": project_id,
            "HOST": host,
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
        mlflow_server_id = service.id
        access_url = f"/projects/{project_id}/services/{mlflow_server_id}/access/5001b/"
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


def is_mlflow_service(service: Service) -> bool:
    if service.metadata is None:
        return False
    return service.metadata.get(LABEL_EXTENSION_DEPLOYMENT_TYPE, "") == "mlflow"


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
        "lab_mlflow_manager.app:app",
        host="localhost",
        port=int(os.getenv("PORT", 8080)),
        log_level="info",
        reload=True,
        log_config=log_config,
    )

import os
from datetime import timedelta
from typing import Any, List, Optional

from contaxy.operations import AuthOperations
from contaxy.operations.components import ComponentOperations
from contaxy.schema import Service, ServiceInput
from contaxy.schema.auth import USER_ID_PARAM, AccessLevel
from contaxy.schema.deployment import (
    ACTION_START,
    SERVICE_ID_PARAM,
    DeploymentCompute,
    ServiceUpdate,
)
from contaxy.schema.exceptions import (
    CREATE_RESOURCE_RESPONSES,
    UPDATE_RESOURCE_RESPONSES,
    ClientValueError,
    ResourceAlreadyExistsError,
)
from contaxy.utils import fastapi_utils
from fastapi import Depends, FastAPI, Query, Response, status
from loguru import logger
from starlette.middleware.cors import CORSMiddleware

from lab_workspace_manager.config import settings
from lab_workspace_manager.schema import (
    Workspace,
    WorkspaceCompute,
    WorkspaceConfigOptions,
    WorkspaceInput,
    WorkspaceUpdate,
)
from lab_workspace_manager.utils import CONTAXY_API_ENDPOINT, get_component_manager

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


def is_ws_service(service: Service) -> bool:
    if service.metadata is None:
        return False
    return service.metadata.get(LABEL_EXTENSION_DEPLOYMENT_TYPE, "") == "workspace"


def create_ws_service_input(
    workspace_input: WorkspaceInput, user_token: str
) -> ServiceInput:
    return ServiceInput(
        container_image=workspace_input.container_image,
        display_name=f"WS {workspace_input.display_name}",
        endpoints=["8080b"],
        parameters={
            "WORKSPACE_BASE_URL": "{env.CONTAXY_SERVICE_URL}",
            "SSH_JUMPHOST_TARGET": "{env.CONTAXY_DEPLOYMENT_NAME}",
            "SELF_ACCESS_TOKEN": "{env.CONTAXY_API_TOKEN}",
            "LAB_API_ENDPOINT": "{env.CONTAXY_API_ENDPOINT}",
            "LAB_API_TOKEN": user_token,
        },
        metadata={LABEL_EXTENSION_DEPLOYMENT_TYPE: "workspace"},
        compute={
            "volume_path": "/workspace",
            "max_cpus": workspace_input.compute.cpus,
            "min_cpus": compute_min_cpu(workspace_input.compute.cpus),
            "max_memory": workspace_input.compute.memory,
            "min_memory": compute_min_memory(workspace_input.compute.memory),
            "max_volume_size": settings.WORKSPACE_VOLUME_SIZE,
            "max_container_size": settings.WORKSPACE_CONTAINER_SIZE,
        },
        is_stopped=workspace_input.is_stopped,
        idle_timeout=workspace_input.idle_timeout
        if workspace_input.idle_timeout != timedelta(0)
        else None,
        clear_volume_on_stop=True
        if settings.WORKSPACE_ALWAYS_CLEAR_VOLUME_ON_STOP
        else workspace_input.clear_volume_on_stop,
    )


def create_ws_service_update(workspace_update: WorkspaceUpdate) -> ServiceUpdate:
    workspace_update_dict = workspace_update.dict(exclude_unset=True)
    if "idle_timeout" in workspace_update_dict:
        if workspace_update_dict["idle_timeout"] == timedelta(0):
            workspace_update_dict["idle_timeout"] = None
    if "clear_volume_on_stop" in workspace_update_dict:
        if settings.WORKSPACE_ALWAYS_CLEAR_VOLUME_ON_STOP:
            workspace_update_dict["clear_volume_on_stop"] = True
    service_update = ServiceUpdate(**workspace_update_dict)
    if "compute" in workspace_update_dict:
        service_update.compute = DeploymentCompute()
        if "cpus" in workspace_update_dict["compute"]:
            service_update.compute.max_cpus = workspace_update.compute.cpus
            service_update.compute.min_cpus = compute_min_cpu(
                workspace_update.compute.cpus
            )
        if "memory" in workspace_update_dict["compute"]:
            service_update.compute.max_memory = workspace_update.compute.memory
            service_update.compute.min_memory = compute_min_memory(
                workspace_update.compute.memory
            )
    return service_update


def compute_min_cpu(max_cpus: int) -> float:
    # Only reserve halve of the CPU per workspace as CPU resources can be easily shared
    return max_cpus * 0.5


def compute_min_memory(max_memory: int) -> int:
    # Reserve 80% of the workspace memory. This allows some over committing on the node but hopefully does not cause
    # pods to be evicted (would happen if many workspaces use their max memory limit).
    return int(max_memory * 0.75)


def create_ws_from_service(service: Service) -> Workspace:
    access_url = None
    if service.status == "running":
        project_id = service.metadata["ctxy.projectName"]
        workspace_id = service.id
        access_url = f"/projects/{project_id}/services/{workspace_id}/access/8080b"
    compute = WorkspaceCompute()
    if service.compute.max_cpus:
        compute.cpus = service.compute.max_cpus
    if service.compute.max_memory:
        compute.memory = service.compute.max_memory
    return Workspace(
        id=service.id,
        display_name=service.display_name[len("WS ") :],
        container_image=service.container_image,
        compute=compute,
        idle_timeout=service.idle_timeout,
        clear_volume_on_stop=service.clear_volume_on_stop,
        status=service.status,
        access_url=access_url,
    )


def request_user_token(user_id: str, auth_manager: AuthOperations) -> str:
    return auth_manager.get_user_token(user_id=user_id, access_level=AccessLevel.WRITE)


@app.post(
    "/users/{user_id}/workspace",
    summary="Create a new personal workspace for the user.",
    status_code=status.HTTP_200_OK,
    response_model=Workspace,
    responses={**CREATE_RESOURCE_RESPONSES},
)
def deploy_workspace(
    workspace_input: WorkspaceInput,
    user_id: str = USER_ID_PARAM,
    component_manager: ComponentOperations = Depends(get_component_manager),
) -> Any:
    """Create a new personal workspace by creating a Contaxy service with a workspace image in the personal project."""
    logger.debug(f"Deploy workspace request for user {user_id}: {workspace_input}")
    user_token = request_user_token(user_id, component_manager.get_auth_manager())
    service_input = create_ws_service_input(workspace_input, user_token)
    try:
        # Use the user's project which has the same id as the user
        service = component_manager.get_service_manager().deploy_service(
            project_id=user_id, service_input=service_input
        )
        logger.debug(
            f"Successfully created workspace service with name "
            f"{service.display_name} and id {service.id}"
        )
        return create_ws_from_service(service)
    except ResourceAlreadyExistsError:
        raise ResourceAlreadyExistsError(
            f"A workspace with the name {workspace_input.display_name} already exists for user {user_id}!"
        )


@app.patch(
    "/users/{user_id}/workspace/{workspace_id}",
    summary="Updates the workspace for the user.",
    status_code=status.HTTP_200_OK,
    response_model=Workspace,
    responses={**UPDATE_RESOURCE_RESPONSES},
)
def update_workspace(
    workspace_update: WorkspaceUpdate,
    user_id: str = USER_ID_PARAM,
    workspace_id: str = SERVICE_ID_PARAM,
    component_manager: ComponentOperations = Depends(get_component_manager),
) -> Any:
    logger.debug(
        f"Update workspace request for user {user_id} "
        f"and workspace {workspace_id}: {workspace_update}"
    )
    service_update = create_ws_service_update(workspace_update)
    service = component_manager.get_service_manager().update_service(
        project_id=user_id, service_id=workspace_id, service=service_update
    )
    return create_ws_from_service(service)


@app.post(
    "/users/{user_id}/workspace/{workspace_id}:start",
    summary="Start the specified workspace if it is stopped.",
    status_code=status.HTTP_204_NO_CONTENT,
)
def start_workspace(
    user_id: str = USER_ID_PARAM,
    workspace_id: str = SERVICE_ID_PARAM,
    component_manager: ComponentOperations = Depends(get_component_manager),
) -> Any:
    logger.debug(
        f"Start workspace request for user {user_id} " f"and workspace {workspace_id}"
    )
    component_manager.get_service_manager().execute_service_action(
        project_id=user_id, service_id=workspace_id, action_id=ACTION_START
    )
    return Response(status_code=status.HTTP_204_NO_CONTENT)


@app.get(
    "/users/{user_id}/workspace",
    summary="Get a list of all workspaces for the user",
    status_code=status.HTTP_200_OK,
    response_model=List[Workspace],
)
def list_workspaces(
    user_id: str = USER_ID_PARAM,
    component_manager: ComponentOperations = Depends(get_component_manager),
) -> Any:
    logger.info(f"List workspaces request for user {user_id}")

    services = component_manager.get_service_manager().list_services(project_id=user_id)
    workspaces = [
        create_ws_from_service(service)
        for service in services
        if is_ws_service(service)
    ]
    return workspaces


@app.get(
    "/users/{user_id}/workspace/{workspace_id}",
    summary="Get information about a specific workspace",
    status_code=status.HTTP_200_OK,
    response_model=Workspace,
)
def get_workspace(
    user_id: str = USER_ID_PARAM,
    workspace_id: str = SERVICE_ID_PARAM,
    component_manager: ComponentOperations = Depends(get_component_manager),
) -> Any:
    logger.info(
        f"Get workspace request for user {user_id} with workspace id {workspace_id}."
    )

    service = component_manager.get_service_manager().get_service_metadata(
        user_id, workspace_id
    )
    if not is_ws_service(service):
        raise ClientValueError(
            f"The service with id {workspace_id} is not a workspace!"
        )

    return create_ws_from_service(service)


@app.delete(
    "/users/{user_id}/workspace/{service_id}",
    summary="Delete the specified workspace",
    status_code=status.HTTP_204_NO_CONTENT,
)
def delete_workspace(
    user_id: str = USER_ID_PARAM,
    service_id: str = SERVICE_ID_PARAM,
    delete_volumes: Optional[bool] = Query(
        False, description="Delete all volumes associated with the deployment."
    ),
    component_manager: ComponentOperations = Depends(get_component_manager),
) -> Any:
    logger.debug(
        f"Delete workspace request for user {user_id} with workspace id {service_id}."
    )

    workspace = get_workspace(user_id, service_id, component_manager)

    component_manager.get_service_manager().delete_service(
        user_id, workspace.id, delete_volumes
    )
    return Response(status_code=status.HTTP_204_NO_CONTENT)


@app.get(
    "/config",
    summary="Get workspace manager config.",
    response_model=WorkspaceConfigOptions,
    status_code=status.HTTP_200_OK,
)
def get_workspace_config(
    component_manager: ComponentOperations = Depends(get_component_manager),
) -> Any:
    return WorkspaceConfigOptions(
        display_name_default="Default Workspace",
        container_image_default=settings.WORKSPACE_IMAGE_DEFAULT,
        container_image_options=settings.WORKSPACE_IMAGE_OPTIONS,
        cpus_default=settings.WORKSPACE_CPUS_DEFAULT,
        cpus_max=settings.WORKSPACE_CPUS_MAX,
        cpus_options=settings.WORKSPACE_CPUS_OPTIONS,  # type: ignore
        memory_default=settings.WORKSPACE_MEMORY_MB_DEFAULT,
        memory_max=settings.WORKSPACE_MEMORY_MB_MAX,
        memory_options=settings.WORKSPACE_MEMORY_MB_OPTIONS,  # type: ignore
        idle_timeout_default=settings.WORKSPACE_IDLE_TIMEOUT_DEFAULT,
        idle_timeout_options=settings.WORKSPACE_IDLE_TIMEOUT_OPTIONS,  # type: ignore
        always_clear_volume_on_stop=settings.WORKSPACE_ALWAYS_CLEAR_VOLUME_ON_STOP,
    )


if __name__ == "__main__":
    import uvicorn

    if not CONTAXY_API_ENDPOINT:
        raise RuntimeError("CONTAXY_API_ENDPOINT must be set")
    import logging

    logging.getLogger("uvicorn").propagate = False
    # Prevent duplicated logs
    log_config = uvicorn.config.LOGGING_CONFIG
    log_config["loggers"]["uvicorn"]["propagate"] = False
    uvicorn.run(
        "lab_workspace_manager.app:app",
        host="localhost",
        port=int(os.getenv("PORT", 8080)),
        log_level="info",
        reload=True,
        log_config=log_config,
    )

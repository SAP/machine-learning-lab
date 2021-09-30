import os
from typing import Any, Optional

from fastapi import Depends, FastAPI, status, Query, Response
from loguru import logger

from contaxy.managers.components import ComponentManager
from contaxy.schema import ServiceInput, Service
from contaxy.schema.auth import USER_ID_PARAM
from contaxy.schema.deployment import SERVICE_ID_PARAM
from contaxy.schema.exceptions import (
    CREATE_RESOURCE_RESPONSES,
    ClientValueError,
    ResourceAlreadyExistsError,
)
from contaxy.utils import fastapi_utils
from lab_workspace_manager.utils import get_component_manager, CONTAXY_API_ENDPOINT

SELF_ACCESS_URL = os.getenv("CONTAXY_SERVICE_URL", "")

SELF_DEPLOYMENT_NAME = os.getenv("CONTAXY_DEPLOYMENT_NAME", "")

LABEL_EXTENSION_DEPLOYMENT_TYPE = "ctxy.workspaceExtension.deploymentType"

WORKSPACE_MAX_MEMORY_MB = int(os.getenv("WORKSPACE_MAX_MEMORY_MB", "500"))

WORKSPACE_MAX_CPUS = int(os.getenv("WORKSPACE_MAX_CPUS", "1"))

print(
    f"WORKSPACE_MAX_MEMORY_MB: {WORKSPACE_MAX_MEMORY_MB} WORKSPACE_MAX_CPUS:{WORKSPACE_MAX_CPUS}"
)

app = FastAPI()
# Patch FastAPI to allow relative path resolution.
fastapi_utils.patch_fastapi(app)


def is_workspace(service: Service):
    if service.metadata is None:
        return False
    return service.metadata.get(LABEL_EXTENSION_DEPLOYMENT_TYPE, "") == "workspace"


@app.post(
    "/users/{user_id}/workspace",
    summary="Create a new personal workspace for the user.",
    status_code=status.HTTP_200_OK,
    responses={**CREATE_RESOURCE_RESPONSES},
)
def create_workspace(
    service: ServiceInput,
    user_id: str = USER_ID_PARAM,
    component_manager: ComponentManager = Depends(get_component_manager),
) -> Any:
    """Create a new personal workspace by creating a Contaxy service with a workspace image in the personal project."""
    if service.display_name is None:
        raise ClientValueError("The display_name field needs to be set!")
    logger.info(
        f"Create workspace request for user {user_id} with workspace name "
        f"{service.display_name} and image {service.container_image}"
    )

    # TODO: Check which fields should be taken from the input (compute, endpoints, metadata, etc.)
    if service.parameters is None:
        service.parameters = {}
    workspace_service_config = ServiceInput(
        container_image=service.container_image,
        display_name=f"ws-{service.display_name}",
        endpoints=["8080b"],
        parameters={
            **service.parameters,
            "WORKSPACE_BASE_URL": "{env.CONTAXY_SERVICE_URL}",
        },
        metadata={LABEL_EXTENSION_DEPLOYMENT_TYPE: "workspace"},
        compute={
            "volume_path": "/workspace",
            "max_memory": WORKSPACE_MAX_MEMORY_MB,
            "max_cpus": WORKSPACE_MAX_CPUS,
        },
    )

    try:
        workspace_service = component_manager.get_service_manager().deploy_service(
            project_id=user_id, service=workspace_service_config
        )
        logger.info(
            f"Successfully created workspace service with name "
            f"{workspace_service.display_name} and id {workspace_service.id}"
        )
        return workspace_service
    except ResourceAlreadyExistsError:
        raise ResourceAlreadyExistsError(
            f"A workspace with the name {service.display_name} already exists for user {user_id}!"
        )


@app.get(
    "/users/{user_id}/workspace",
    summary="Get a list of all workspaces for the user",
    status_code=status.HTTP_200_OK,
)
def list_workspaces(
    user_id: str = USER_ID_PARAM,
    component_manager: ComponentManager = Depends(get_component_manager),
) -> Any:
    logger.info(f"List workspaces request for user {user_id}")

    services = component_manager.get_service_manager().list_services(project_id=user_id)
    workspaces = [service.dict() for service in services if is_workspace(service)]
    return workspaces


@app.get(
    "/users/{user_id}/workspace/{service_id}",
    summary="Get information about a specific workspace",
    status_code=status.HTTP_200_OK,
)
def get_workspace(
    user_id: str = USER_ID_PARAM,
    service_id: str = SERVICE_ID_PARAM,
    component_manager: ComponentManager = Depends(get_component_manager),
) -> Any:
    logger.info(
        f"Get workspace request for user {user_id} with workspace id {service_id}."
    )

    service = component_manager.get_service_manager().get_service_metadata(
        user_id, service_id
    )
    if not is_workspace(service):
        raise ClientValueError(f"The service with id {service_id} is not a workspace!")

    return service


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
    component_manager: ComponentManager = Depends(get_component_manager),
) -> Any:
    logger.info(
        f"Delete workspace request for user {user_id} with workspace id {service_id}."
    )

    service = get_workspace(user_id, service_id, component_manager)

    component_manager.get_service_manager().delete_service(
        user_id, service.id, delete_volumes
    )
    return Response(status_code=status.HTTP_204_NO_CONTENT)


if __name__ == "__main__":
    import uvicorn

    if not CONTAXY_API_ENDPOINT:
        raise RuntimeError("CONTAXY_API_ENDPOINT must be set")
    uvicorn.run(app, host="localhost", port=8080, log_level="info", reload=True)

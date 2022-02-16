import os
from typing import Any

from contaxy.managers.components import ComponentManager
from contaxy.schema.exceptions import (
    CREATE_RESOURCE_RESPONSES,
    GET_RESOURCE_RESPONSES,
    UPDATE_RESOURCE_RESPONSES,
)
from contaxy.schema.project import PROJECT_ID_PARAM
from contaxy.utils import fastapi_utils
from fastapi import Depends, FastAPI, Path, status
from starlette.middleware.cors import CORSMiddleware

from lab_secret_store.helper import display_name_to_id
from lab_secret_store.schema import Secret, SecretInput, SecretUpdate
from lab_secret_store.secret_store.json_db_secret_store import JsonDbSecretStore
from lab_secret_store.utils import CONTAXY_API_ENDPOINT, get_component_manager

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
    "/projects/{project_id}/secrets/{secret_id}",
    summary="Get a specific secret via its secret id",
    status_code=status.HTTP_200_OK,
    response_model=Secret,
    responses={**CREATE_RESOURCE_RESPONSES},
)
def get_secret(
    project_id: str = PROJECT_ID_PARAM,
    secret_id: str = Path(
        ...,
        title="Secret ID",
        example="my-secret",
        description="A valid secret ID.",
    ),
    component_manager: ComponentManager = Depends(get_component_manager),
) -> Any:
    secret_store = JsonDbSecretStore(component_manager.get_json_db_manager())
    return secret_store.get_secret(project_id, secret_id)


@app.get(
    "/projects/{project_id}/secrets",
    summary="List all secrets of a project",
    status_code=status.HTTP_200_OK,
    response_model=list,
    responses={**GET_RESOURCE_RESPONSES},
)
def list_secrets(
    project_id: str = PROJECT_ID_PARAM,
    component_manager: ComponentManager = Depends(get_component_manager),
) -> Any:
    secret_store = JsonDbSecretStore(component_manager.get_json_db_manager())
    return secret_store.get_secrets(project_id)


@app.post(
    "/projects/{project_id}/secrets",
    summary="Create a new secret",
    status_code=status.HTTP_200_OK,
)
def create_secret(
    post_body: SecretInput,
    project_id: str = PROJECT_ID_PARAM,
    component_manager: ComponentManager = Depends(get_component_manager),
) -> Any:
    secret_store = JsonDbSecretStore(component_manager.get_json_db_manager())
    secretJson = Secret(
        id=display_name_to_id(post_body.display_name),
        display_name=post_body.display_name,
        metadata=post_body.metadata,
        secret_text=post_body.secret_text,
    )
    return secret_store.create_secret(
        project_id,
        secretJson.id,
        secretJson,
    )


@app.patch(
    "/projects/{project_id}/secrets/{secret_id}",
    summary="Update the specified secret with a new secret text",
    status_code=status.HTTP_204_NO_CONTENT,
    responses={**UPDATE_RESOURCE_RESPONSES},
)
def update_secret(
    post_body: SecretUpdate,
    project_id: str = PROJECT_ID_PARAM,
    secret_id: str = Path(
        ...,
        title="Secret ID",
        example="my-secret",
        description="A valid secret ID.",
    ),
    component_manager: ComponentManager = Depends(get_component_manager),
) -> Any:
    secret_store = JsonDbSecretStore(component_manager.get_json_db_manager())
    secret_store.update_secret(
        project_id,
        secret_id,
        post_body,
    )


@app.delete(
    "/projects/{project_id}/secrets/{secret_id}",
    summary="Delete the secret",
    status_code=status.HTTP_204_NO_CONTENT,
)
def delete_secret(
    project_id: str = PROJECT_ID_PARAM,
    secret_id: str = Path(
        ...,
        title="Secret ID",
        example="my-secret",
        description="A valid secret ID.",
    ),
    component_manager: ComponentManager = Depends(get_component_manager),
) -> Any:
    secret_store = JsonDbSecretStore(component_manager.get_json_db_manager())
    secret_store.delete_secret(project_id, secret_id)


if __name__ == "__main__":
    import uvicorn

    if not CONTAXY_API_ENDPOINT:
        raise RuntimeError("CONTAXY_API_ENDPOINT must be set")

    # Prevent duplicated logs
    log_config = uvicorn.config.LOGGING_CONFIG
    log_config["loggers"]["uvicorn"]["propagate"] = False
    uvicorn.run(
        "lab_secret_store.app:app",
        host="localhost",
        port=int(os.getenv("PORT", 8080)),
        log_level="info",
        reload=True,
        log_config=log_config,
    )

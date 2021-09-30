import os
from typing import Generator

from fastapi import Request, Depends

from contaxy.api.dependencies import get_api_token
from contaxy.clients import (
    ExtensionClient,
    ProjectClient,
    SystemClient,
    JsonDocumentClient,
    DeploymentManagerClient,
    FileClient,
    AuthClient,
)
from contaxy.clients.shared import BaseUrlSession
from contaxy.managers.components import ComponentManager

CONTAXY_API_ENDPOINT = os.getenv("CONTAXY_API_ENDPOINT", None)


def get_component_manager(
    request: Request,
    token: str = Depends(get_api_token),
) -> Generator[ComponentManager, None, None]:
    """Returns the initialized component manager.

    This is used as FastAPI dependency and called for every request.
    """
    session = BaseUrlSession(base_url=CONTAXY_API_ENDPOINT)
    session.headers = {"Authorization": f"Bearer {token}"}
    with ComponentManager(request) as component_manager:
        component_manager._auth_manager = AuthClient(session)
        component_manager._extension_manager = ExtensionClient(session)
        component_manager._project_manager = ProjectClient(session)
        component_manager._system_manager = SystemClient(session)
        component_manager._json_db_manager = JsonDocumentClient(session)
        component_manager._deployment_manager = DeploymentManagerClient(session)
        component_manager._file_manager = FileClient(session)
        yield component_manager

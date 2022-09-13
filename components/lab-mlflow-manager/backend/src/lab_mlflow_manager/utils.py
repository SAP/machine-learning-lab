import os
from typing import Generator

from contaxy.clients.components import ComponentClient
from contaxy.clients.shared import BaseUrlSession
from contaxy.operations.components import ComponentOperations
from contaxy.utils.auth_utils import get_api_token
from fastapi import Depends

CONTAXY_API_ENDPOINT = os.getenv("CONTAXY_API_ENDPOINT", None)


def get_component_manager(
    token: str = Depends(get_api_token),
) -> Generator[ComponentOperations, None, None]:
    """Returns the initialized component manager.

    This is used as FastAPI dependency and called for every request.
    """
    session = BaseUrlSession(base_url=CONTAXY_API_ENDPOINT)
    session.headers = {"Authorization": f"Bearer {token}"}
    return ComponentClient(session)

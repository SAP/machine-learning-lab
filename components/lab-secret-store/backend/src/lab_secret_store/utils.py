import os

from contaxy.clients.components import ComponentClient
from contaxy.clients.shared import BaseUrlSession
from contaxy.operations.components import ComponentOperations
from contaxy.utils.auth_utils import get_api_token
from lab_secret_store.secret_store.abstract_secret_store import AbstractSecretStore
from lab_secret_store.secret_store.json_db_secret_store import JsonDbSecretStore
from lab_secret_store.secret_store.vault_secret_store import VaultSecretStore

from fastapi import Depends
from lab_secret_store.config import settings

CONTAXY_API_ENDPOINT = os.getenv("CONTAXY_API_ENDPOINT", None)


def get_component_manager(
    token: str = Depends(get_api_token),
) -> ComponentOperations:
    """Returns the initialized component manager.

    This is used as FastAPI dependency and called for every request.
    """
    session = BaseUrlSession(base_url=CONTAXY_API_ENDPOINT)
    session.headers = {"Authorization": f"Bearer {token}"}
    return ComponentClient(session)


def get_secret_store(
    component_manager: ComponentOperations = Depends(get_component_manager)
) -> AbstractSecretStore:
    if (BaseUrlSession(base_url=settings.SECRETSTORE_USING_VAULT)):
        return VaultSecretStore(component_manager.get_json_db_manager())
    else:
        return JsonDbSecretStore(component_manager.get_json_db_manager())

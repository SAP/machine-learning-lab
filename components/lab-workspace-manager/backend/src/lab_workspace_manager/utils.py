import os
from typing import Generator, Optional

from contaxy import config
from contaxy.clients.components import ComponentClient
from contaxy.clients.shared import BaseUrlSession
from contaxy.operations.components import ComponentOperations
from contaxy.schema import UnauthenticatedError
from fastapi import Depends, Security
from fastapi.security import (
    APIKeyCookie,
    APIKeyHeader,
    APIKeyQuery,
    OAuth2PasswordBearer,
)

CONTAXY_API_ENDPOINT = os.getenv("CONTAXY_API_ENDPOINT", None)


class APITokenExtractor:
    def __init__(self, *, auto_error: bool = True):
        self.auto_error = auto_error

    async def __call__(
        self,
        api_token_query: str = Security(
            APIKeyQuery(name=config.API_TOKEN_NAME, auto_error=False)
        ),
        api_token_header: str = Security(
            APIKeyHeader(name=config.API_TOKEN_NAME, auto_error=False)
        ),
        bearer_token: str = Security(
            OAuth2PasswordBearer(tokenUrl="auth/oauth/token", auto_error=False)
        ),
        api_token_cookie: str = Security(
            APIKeyCookie(name=config.API_TOKEN_NAME, auto_error=False)
        ),
    ) -> Optional[str]:
        # TODO: already check token validity here?
        if api_token_query:
            return api_token_query
        elif api_token_header:
            return api_token_header
        elif bearer_token:
            # TODO: move the bearer token under the cookie?
            return bearer_token
        elif api_token_cookie:
            return api_token_cookie
        else:
            if self.auto_error:
                raise UnauthenticatedError("No API token was provided.")
            else:
                return None


get_api_token = APITokenExtractor(auto_error=True)


def get_component_manager(
    token: str = Depends(get_api_token),
) -> Generator[ComponentOperations, None, None]:
    """Returns the initialized component manager.

    This is used as FastAPI dependency and called for every request.
    """
    session = BaseUrlSession(base_url=CONTAXY_API_ENDPOINT)
    session.headers = {"Authorization": f"Bearer {token}"}
    return ComponentClient(session)

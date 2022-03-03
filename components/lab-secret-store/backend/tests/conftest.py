from typing import Optional

import pytest
import requests
from contaxy.clients.shared import BaseUrlSession
from contaxy.config import settings
from contaxy.utils.state_utils import GlobalState, RequestState
from pydantic import BaseSettings
from starlette.datastructures import State


class TestSettings(BaseSettings):
    """Test Settings."""

    REMOTE_CONTAXY_ENDPOINT: Optional[str] = "http://localhost:30010/api"
    REMOTE_BACKEND_TESTS: bool = False
    REMOTE_BACKEND_ENDPOINT: Optional[str] = "http://localhost:8080"


test_settings = TestSettings()


@pytest.fixture()
def remote_client() -> requests.Session:
    """Initializes a remote client using the configured remote backend endpoint."""
    return BaseUrlSession(base_url=test_settings.REMOTE_BACKEND_ENDPOINT)


@pytest.fixture()
def contaxy_remote_client() -> requests.Session:
    """Initializes a remote client using the configured remote backend endpoint."""
    return BaseUrlSession(base_url=test_settings.REMOTE_CONTAXY_ENDPOINT)


@pytest.fixture()
def global_state() -> GlobalState:
    """Initializes global state."""
    state = GlobalState(State())
    state.settings = settings
    return state


@pytest.fixture()
def request_state() -> RequestState:
    """Initializes request state."""
    request_state = RequestState(State())
    return request_state

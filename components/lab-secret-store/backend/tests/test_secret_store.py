from abc import ABC, abstractmethod

import pytest
import requests
from contaxy import config
from contaxy.clients import AuthClient
from contaxy.managers.json_db.inmemory_dict import InMemoryDictJsonDocumentManager
from contaxy.schema import OAuth2TokenRequestFormNew
from contaxy.schema.auth import AccessLevel, OAuth2TokenGrantTypes
from contaxy.utils import auth_utils
from contaxy.utils.state_utils import GlobalState, RequestState

from lab_secret_store.client import SecretClient
from lab_secret_store.schema import SecretInput, SecretUpdate
from lab_secret_store.secret_store.abstract_secret_store import AbstractSecretStore
from lab_secret_store.secret_store.json_db_secret_store import JsonDbSecretStore
from lab_secret_store.secret_store.vault_secret_store import VaultSecretStore

from .conftest import test_settings


class SecretStoreTests(ABC):
    @property
    @abstractmethod
    def secret_store(self) -> AbstractSecretStore:
        pass

    @property
    @abstractmethod
    def project_id(self) -> str:
        pass

    def test_secret_crud(self):
        # Create new secrets
        secret1 = self.secret_store.create_secret(
            project_id=self.project_id,
            value=SecretInput(
                display_name="My Secret",
                metadata={"username": "test"},
                secret_text="test",
            ),
        )
        secret2 = self.secret_store.create_secret(
            project_id=self.project_id,
            value=SecretInput(
                display_name="My Secret 2",
                metadata={"username": "test2"},
                secret_text="test2",
            ),
        )
        # Read secret
        read_secret = self.secret_store.get_secret(
            project_id=self.project_id,
            secret_id=secret1.id,
        )
        assert read_secret.display_name == secret1.display_name
        assert read_secret.metadata == secret1.metadata
        assert read_secret.secret_text == "test"

        # List secrets
        secrets = self.secret_store.list_secrets(self.project_id)
        assert len(secrets) == 2
        secrets_ids = [secret.id for secret in secrets]
        assert secret1.id in secrets_ids and secret2.id in secrets_ids

        # update
        update = SecretUpdate(metadata={"username": "updated_username"})
        self.secret_store.update_secret(self.project_id, secret1.id, update)
        read_secret = self.secret_store.get_secret(
            project_id=self.project_id,
            secret_id=secret1.id,
        )
        assert read_secret.secret_text == "test"
        assert read_secret.metadata == {"username": "updated_username"}

        # in secret 2
        update = SecretUpdate(secret_text="updated_secret_text")
        self.secret_store.update_secret(self.project_id, secret2.id, update)
        read_secret = self.secret_store.get_secret(
            project_id=self.project_id,
            secret_id=secret2.id,
        )
        assert read_secret.secret_text == "updated_secret_text"
        assert read_secret.metadata == {"username": "test2"}

        # Delete secrets
        self.secret_store.delete_secret(self.project_id, secret1.id)
        self.secret_store.delete_secret(self.project_id, secret2.id)
        assert len(self.secret_store.list_secrets(self.project_id)) == 0


@pytest.mark.unit
class TestJsonDbSecretStoreWithInMemoryDB(SecretStoreTests):
    @pytest.fixture(autouse=True)
    def _init_secret_store(
        self, global_state: GlobalState, request_state: RequestState
    ) -> None:
        json_db = InMemoryDictJsonDocumentManager(global_state, request_state)
        self._secret_store = JsonDbSecretStore(json_db)

    @property
    def secret_store(self) -> AbstractSecretStore:
        return self._secret_store

    @property
    def project_id(self) -> str:
        return "secret-store-test"


@pytest.mark.unit
class TestVaultSecretStoreWithInMemoryDB(SecretStoreTests):
    @pytest.fixture(autouse=True)
    def _init_secret_store(
        self, global_state: GlobalState, request_state: RequestState
    ) -> None:
        json_db = InMemoryDictJsonDocumentManager(global_state, request_state)
        self._secret_store = VaultSecretStore(json_db)

    @property
    def secret_store(self) -> AbstractSecretStore:
        return self._secret_store

    @property
    def project_id(self) -> str:
        return "secret-store-test"


@pytest.mark.skipif(
    not test_settings.REMOTE_BACKEND_ENDPOINT,
    reason="No remote backend is configured (via REMOTE_CONTAXY_ENDPOINT).",
)
@pytest.mark.skipif(
    not test_settings.REMOTE_BACKEND_TESTS,
    reason="Remote Backend Tests are deactivated, use REMOTE_CONTAXY_TESTS to activate.",
)
@pytest.mark.integration
class TestSecretStoreWithContaxyEndpoint(SecretStoreTests):
    @pytest.fixture(autouse=True)
    def _init_secret_store(
        self,
        remote_client: requests.Session,
        contaxy_remote_client: requests.Session,
    ) -> None:
        self._secret_store = SecretClient(remote_client)
        self._auth_manager = AuthClient(contaxy_remote_client)
        self.login_user(
            config.SYSTEM_ADMIN_USERNAME, config.SYSTEM_ADMIN_INITIAL_PASSWORD
        )
        remote_client.cookies = contaxy_remote_client.cookies

    @property
    def secret_store(self) -> AbstractSecretStore:
        return self._secret_store

    @property
    def project_id(self) -> str:
        return "secret-store-test"

    def login_user(self, username: str, password: str) -> None:
        self._auth_manager.request_token(
            OAuth2TokenRequestFormNew(
                grant_type=OAuth2TokenGrantTypes.PASSWORD,
                username=username,
                password=password,
                scope=auth_utils.construct_permission(
                    "*", AccessLevel.ADMIN
                ),  # Get full scope
                set_as_cookie=True,
            )
        )

from typing import Dict, List

import requests
from contaxy.clients.shared import handle_errors
from pydantic import parse_raw_as

from lab_secret_store.schema import Secret, SecretInput, SecretMetadata, SecretUpdate
from lab_secret_store.secret_store.abstract_secret_store import AbstractSecretStore


class SecretClient(AbstractSecretStore):
    def __init__(self, client: requests.Session):
        self._client = client

    def list_secrets(
        self,
        project_id: str,
        request_kwargs: Dict = {},
    ) -> List[SecretMetadata]:
        response = self._client.get(f"/projects/{project_id}/secrets", **request_kwargs)
        handle_errors(response)
        return parse_raw_as(List[SecretMetadata], response.text)

    def create_secret(
        self,
        project_id: str,
        value: SecretInput,
        request_kwargs: Dict = {},
    ) -> SecretMetadata:
        resource = self._client.post(
            f"/projects/{project_id}/secrets",
            data=value.json(exclude_unset=True),
            **request_kwargs,
        )
        handle_errors(resource)
        return parse_raw_as(SecretMetadata, resource.text)

    def get_secret(
        self, project_id: str, secret_id: str, request_kwargs: Dict = {}
    ) -> Secret:
        resource = self._client.get(
            f"/projects/{project_id}/secrets/{secret_id}", **request_kwargs
        )
        handle_errors(resource)
        return parse_raw_as(Secret, resource.text)

    def update_secret(
        self,
        project_id: str,
        secret_id: str,
        value: SecretUpdate,
        request_kwargs: Dict = {},
    ) -> None:
        response = self._client.patch(
            f"/projects/{project_id}/secrets/{secret_id}",
            data=value.json(exclude_unset=True),
            **request_kwargs,
        )
        handle_errors(response)

    def delete_secret(
        self,
        project_id: str,
        secret_id: str,
        request_kwargs: Dict = {},
    ) -> None:
        response = self._client.delete(
            f"/projects/{project_id}/secrets/{secret_id}",
            **request_kwargs,
        )
        handle_errors(response)

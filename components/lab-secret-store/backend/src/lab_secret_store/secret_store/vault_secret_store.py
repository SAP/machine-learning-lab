from typing import List

import hvac
import requests
from contaxy.operations import JsonDocumentOperations

from lab_secret_store.config import settings
from lab_secret_store.helper import display_name_to_id
from lab_secret_store.schema import Secret, SecretInput, SecretMetadata, SecretUpdate
from lab_secret_store.secret_store.abstract_secret_store import AbstractSecretStore

SECRET_COLLECTION: str = "_lab_secret_store_secrets"


class VaultSecretStore(AbstractSecretStore):
    def __init__(self, json_db: JsonDocumentOperations):
        self.client = hvac.Client(
            url=settings.SECRETSTORE_VAULT_URL, token=settings.SECRETSTORE_VAULT_TOKEN
        )
        self.json_db = json_db

    def get_secret(self, project_id: str, secret_id: str) -> Secret:
        if self.client.is_authenticated():
            response = self.client.secrets.kv.v2.read_secret(
                path=project_id + "/" + secret_id
            )
            secret_text = response["data"]["data"]["key"]
            custom_metadata = response["data"]["metadata"]["custom_metadata"]
            display_name = custom_metadata["display_name"]
            del custom_metadata["display_name"]
            metadata = custom_metadata
        else:
            raise RuntimeError("Not Authenticated")

        return Secret(
            id=secret_id,
            display_name=display_name,
            metadata=metadata,
            secret_text=secret_text,
        )

    def list_secrets(self, project_id: str) -> List[SecretMetadata]:
        response = self.client.secrets.kv.v2.list_secrets(path=project_id)
        secret_docs = []
        for secret in response["data"]["keys"]:
            response = self.client.secrets.kv.v2.read_secret(
                path=project_id + "/" + secret
            )

            custom_metadata = response["data"]["metadata"]["custom_metadata"]
            display_name = custom_metadata["display_name"]
            del custom_metadata["display_name"]
            metadata = custom_metadata

            secret_docs.append(
                SecretMetadata(id=secret, display_name=display_name, metadata=metadata)
            )

        return secret_docs

    def create_secret(
        self, project_id: str, value: SecretInput, key: bytes = b""
    ) -> SecretMetadata:
        id = display_name_to_id(value.display_name)
        secretJson = SecretMetadata(
            id=id,
            display_name=value.display_name,
            metadata=value.metadata,
        )
        secret = {
            "key": value.secret_text,
        }
        self.client.secrets.kv.v2.create_or_update_secret(
            path=project_id + "/" + id,
            secret=secret,
        )
        metadata = value.metadata
        metadata["display_name"] = value.display_name
        test = requests.post(
            settings.SECRETSTORE_VAULT_URL
            + "v1/secret/metadata/"
            + project_id
            + "/"
            + id,
            json={"custom_metadata": metadata},
            headers={"X-Vault-Token": settings.SECRETSTORE_VAULT_TOKEN},
        )
        print(test)
        return secretJson

    def update_secret(
        self, project_id: str, secret_id: str, value: SecretUpdate
    ) -> None:
        if value.secret_text != "":
            secret = {
                "key": value.secret_text,
            }
            self.client.secrets.kv.v2.create_or_update_secret(
                path=project_id + "/" + secret_id,
                secret=secret,
            )
        else:
            metadata = value.metadata
            metadata["display_name"] = self.client.secrets.kv.v2.read_secret(
                path=project_id + "/" + secret_id
            )["data"]["metadata"]["custom_metadata"]["display_name"]
            requests.post(
                settings.SECRETSTORE_VAULT_URL + project_id + "/" + secret_id,
                json={"custom_metadata": metadata},
                headers={"X-Vault-Token": settings.SECRETSTORE_VAULT_TOKEN},
            )

    def delete_secret(self, project_id: str, secret_id: str) -> None:
        self.client.secrets.kv.v2.delete_metadata_and_all_versions(
            path=project_id,
        )

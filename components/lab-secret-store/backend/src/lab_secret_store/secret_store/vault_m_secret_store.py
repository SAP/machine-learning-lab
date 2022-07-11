from typing import List
import hvac
import os

from contaxy.operations import JsonDocumentOperations

from lab_secret_store.helper import display_name_to_id
from lab_secret_store.schema import Secret, SecretInput, SecretMetadata, SecretUpdate
from lab_secret_store.secret_store.abstract_secret_store import AbstractSecretStore

from lab_secret_store.config import settings

SECRET_COLLECTION: str = "_lab_secret_store_secrets"


class VaultSecretStore(AbstractSecretStore):
    def __init__(self, json_db: JsonDocumentOperations):
        self.client = hvac.Client(url=settings.SECRETSTORE_VAULT_URL,token=settings.SECRETSTORE_VAULT_TOKEN)
        self.json_db = json_db


    def get_secret(self, project_id: str, secret_id: str) -> Secret:
        secret_doc = self.json_db.get_json_document(
            project_id=project_id,
            collection_id=SECRET_COLLECTION,
            key=secret_id,
        )
        res = SecretMetadata.parse_raw(secret_doc.json_value)
        if self.client.is_authenticated():
            response = self.client.secrets.kv.v2.update_metadata(path=project_id + "/" + secret_id)
            response = self.client.secrets.kv.v2.read_secret(path=project_id + "/" + secret_id)
            secret_text = response['data']["data"]["key"]
        else: 
            raise RuntimeError("Not Authenticated")
        
        return Secret(id=res.id,display_name=res.display_name,metadata=res.metadata,secret_text=secret_text)

    def list_secrets(self, project_id: str) -> List[SecretMetadata]:
        secret_docs = self.json_db.list_json_documents(
            project_id=project_id,
            collection_id=SECRET_COLLECTION,
        )
        return [
            SecretMetadata.parse_raw(secret_doc.json_value)
            for secret_doc in secret_docs
        ]

    def create_secret(
        self, project_id: str, value: SecretInput, key: bytes = b""
    ) -> SecretMetadata:
        secretJson = SecretMetadata(
            id=display_name_to_id(value.display_name),
            display_name=value.display_name,
            metadata=value.metadata,
        )
        secret = {
            'key': value.secret_text,
        }   
        self.client.secrets.kv.v2.create_or_update_secret(
        path=project_id + "/" + secretJson.id,
        secret=secret,
        )

        self.json_db.create_json_document(
             project_id=project_id,
             collection_id=SECRET_COLLECTION,
             key=secretJson.id,
             json_document=secretJson.json(),
             upsert=True,
            )
        
        return secretJson

    def update_secret(
        self, project_id: str, secret_id: str, value: SecretUpdate
    ) -> None:
        if value.secret_text != "":
            secret = {
                'key': value.secret_text,
            }   
            self.client.secrets.kv.v2.create_or_update_secret(
            path=project_id + "/" + secret_id,
            secret=secret,
            )
        else:
            self.json_db.update_json_document(
                project_id=project_id,
                collection_id=SECRET_COLLECTION,
                key=secret_id,
                json_document=value.json(exclude_unset=True),
            )

    def delete_secret(self, project_id: str, secret_id: str) -> None:
        self.json_db.delete_json_document(
            project_id=project_id,
            collection_id=SECRET_COLLECTION,
            key=secret_id,
        )
        self.client.secrets.kv.v2.delete_metadata_and_all_versions(
        path=project_id,
)


        

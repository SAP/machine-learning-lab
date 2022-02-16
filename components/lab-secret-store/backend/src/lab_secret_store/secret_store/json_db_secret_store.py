from black import List
from contaxy.operations import JsonDocumentOperations
from contaxy.schema.json_db import JsonDocument

from lab_secret_store.schema import Secret, SecretMetadata, SecretUpdate
from lab_secret_store.secret_store.abstract_secret_store import AbstractSecretStore


class JsonDbSecretStore(AbstractSecretStore):
    def __init__(self, json_db: JsonDocumentOperations):
        self.json_db = json_db

    def get_secret(self, project_id: str, secret_id: str) -> Secret:
        secret_doc = self.json_db.get_json_document(
            project_id=project_id,
            collection_id="_lab_secret_store_secrets",
            key=secret_id,
        )
        return Secret.parse_raw(secret_doc.json_value)

    def get_secrets(self, project_id: str) -> List[SecretMetadata]:
        secret_docs = self.json_db.list_json_documents(
            project_id=project_id,
            collection_id="_lab_secret_store_secrets",
        )
        return [
            SecretMetadata.parse_raw(secret_doc.json_value)
            for secret_doc in secret_docs
        ]

    def create_secret(
        self, project_id: str, secret_id: str, value: Secret
    ) -> SecretMetadata:
        return SecretMetadata.parse_raw(
            self.json_db.create_json_document(
                project_id=project_id,
                collection_id="_lab_secret_store_secrets",
                key=secret_id,
                json_document=value.json(),
                upsert=True,
            ).json_value
        )

    def update_secret(
        self, project_id: str, secret_id: str, value: SecretUpdate
    ) -> JsonDocument:
        return self.json_db.update_json_document(
            project_id=project_id,
            collection_id="_lab_secret_store_secrets",
            key=secret_id,
            json_document=value.json(exclude_unset=True),
        )

    def delete_secret(self, project_id: str, secret_id: str) -> None:
        self.json_db.delete_json_document(
            project_id=project_id,
            collection_id="_lab_secret_store_secrets",
            key=secret_id,
        )

from black import List
from contaxy.operations import JsonDocumentOperations

from lab_secret_store.helper import decrypt, display_name_to_id, encrypt
from lab_secret_store.schema import Secret, SecretInput, SecretMetadata, SecretUpdate
from lab_secret_store.secret_store.abstract_secret_store import AbstractSecretStore

SECRET_COLLECTION: str = "_lab_secret_store_secrets"


class JsonDbSecretStore(AbstractSecretStore):
    def __init__(self, json_db: JsonDocumentOperations):
        self.json_db = json_db

    def get_secret(self, project_id: str, secret_id: str) -> Secret:
        secret_doc = self.json_db.get_json_document(
            project_id=project_id,
            collection_id=SECRET_COLLECTION,
            key=secret_id,
        )
        res = Secret.parse_raw(secret_doc.json_value)
        res.secret_text = decrypt(res.secret_text)
        return res

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
        secretJson = Secret(
            id=display_name_to_id(value.display_name),
            display_name=value.display_name,
            metadata=value.metadata,
            secret_text=value.secret_text,
        )
        if key == b"":
            secretJson.secret_text = encrypt(secretJson.secret_text)
        else:
            secretJson.secret_text = encrypt(secretJson.secret_text, key)
        return SecretMetadata.parse_raw(
            self.json_db.create_json_document(
                project_id=project_id,
                collection_id=SECRET_COLLECTION,
                key=secretJson.id,
                json_document=secretJson.json(),
                upsert=True,
            ).json_value
        )

    def update_secret(
        self, project_id: str, secret_id: str, value: SecretUpdate
    ) -> None:
        if value.secret_text != "":
            value.secret_text = encrypt(value.secret_text)
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

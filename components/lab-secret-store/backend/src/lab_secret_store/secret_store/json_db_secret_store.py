from contaxy.operations import JsonDocumentOperations

from lab_secret_store.schema import Secret
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

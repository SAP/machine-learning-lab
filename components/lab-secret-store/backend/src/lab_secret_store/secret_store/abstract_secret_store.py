from abc import ABC, abstractmethod

from black import List

from lab_secret_store.schema import Secret, SecretInput, SecretMetadata, SecretUpdate


class AbstractSecretStore(ABC):
    @abstractmethod
    def list_secrets(self, project_id: str) -> List[SecretMetadata]:
        pass

    def create_secret(
        self, project_id: str, value: SecretInput, key: bytes = b""
    ) -> SecretMetadata:
        pass

    def get_secret(self, project_id: str, secret_id: str) -> Secret:
        pass

    def delete_secret(self, project_id: str, secret_id: str) -> None:
        pass

    def update_secret(
        self, project_id: str, secret_id: str, value: SecretUpdate
    ) -> None:
        pass

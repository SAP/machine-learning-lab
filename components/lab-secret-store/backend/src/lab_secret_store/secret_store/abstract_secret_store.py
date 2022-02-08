from abc import ABC, abstractmethod

from lab_secret_store.schema import Secret


class AbstractSecretStore(ABC):
    @abstractmethod
    def get_secret(self, project_id: str, secret_id: str) -> Secret:
        pass

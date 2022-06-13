from lab_client.handler.job_handler import JobHandler
from lab_client.handler.service_handler import ServiceHandler


class DeploymentEnvironment:

    @property
    def job_handler(self) -> JobHandler:
        """
        Returns the file handler. The file handler provides additional functionality for interacting with the remote storage.
        """

        if self._job_handler is None:
            self._job_handler = JobHandler(self, self._deployment_client)

        return self._job_handler

    @property
    def service_handler(self) -> ServiceHandler:
        """
        Returns the file handler. The file handler provides additional functionality for interacting with the remote storage.
        """

        if self._service_handler is None:
            self._service_handler = ServiceHandler(self, self._deployment_client)

        return self._service_handler

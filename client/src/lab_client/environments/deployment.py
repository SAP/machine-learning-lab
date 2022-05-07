from datetime import datetime
from typing import Dict, Optional, List
from lab_client.handler.job_handler import JobHandler
from lab_client.handler.service_handler import ServiceHandler
from contaxy.schema import Job, JobInput, Service, ServiceInput, ResourceAction
from contaxy.schema.deployment import DeploymentType, ServiceUpdate

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
    
    def list_jobs(self) -> List[Job]:
        return self.job_handler.list_jobs()
    
    def deploy_job(
        self,
        job_input: dict = None,
        action_id: Optional[str] = None,
        wait: bool = False) -> Job:
        return self.job_handler.deploy_job(
            job_input=job_input, 
            action_id=action_id, 
            wait=wait
        )

    def get_job_metadata(self, job_id: str) -> Job:
        return self.job_handler.get_job_metadata(job_id)
    
    def delete_job(self, job_id: str) -> None:
        self.job_handler.delete_job(
            job_id=job_id
        )
    
    def delete_jobs(self) -> None:
        self.job_handler.delete_jobs()
    
    def get_job_logs(
        self,
        job_id: str,
        lines: Optional[int] = None,
        since: Optional[datetime] = None) -> str:
        return self.job_handler.get_job_logs( 
            job_id=job_id, 
            lines=lines, 
            since=since
        )
    
    def list_deploy_job_actions(
        self,
        job_input: dict = None
    ) -> List[ResourceAction]:
        return self.job_handler.list_deploy_job_actions(
            job_input=job_input
        )

    def suggest_job_config(
        self,
        container_image: str,
    ) -> JobInput:

        return self.job_handler.suggest_job_config(
            container_image=container_image
        )
    
    def list_job_actions(
        self,
        job_id: str
    ) -> List[ResourceAction]:

        return self.job_handler.list_job_actions(
            job_id=job_id
        )
    
    def list_services(self) -> List[Service]:
        return self.service_handler.list_services()
    
    def deploy_service(
        self,
        service_input: ServiceInput,
        action_id: Optional[str] = None,
        wait: bool = False) -> Service:
        return self.service_handler.deploy_service(
            service_input=service_input, 
            action_id=action_id, 
            wait=wait
        )

    def get_service_metadata(self, service_id: str) -> Service:
        """
        Returns the service metadata for the given Service ID
        # Arguments
            service_id (string): Service Docker ID or Name.
        # Returns
        'Service' if service was found or 'None'.
        """
        return self.service_handler.get_service_metadata(
            service_id=service_id
        )
    
    def delete_service(self, service_id: str) -> None:
        self.service_handler.delete_service(
            service_id=service_id
        )
    
    def delete_services(self) -> None:
        self.service_handler.delete_services()
    
    def get_service_logs(
        self,
        service_id: str,
        lines: Optional[int] = None,
        since: Optional[datetime] = None) -> str:
        return self.service_handler.get_service_logs(
            service_id=service_id, 
            lines=lines, 
            since=since
        )
    
    def update_service(
        self,
        service_id: str,
        service_update: dict = None
    ) -> Service:

        return self.service_handler.update_service(
            service_id=service_id,
            service_update=service_update
        )

    def update_service_access(
        self,
        service_id: str
    ) -> None:
        return self.service_handler.update_service_access(
            service_id=service_id
        )

    def list_deploy_service_actions(
        self,
        service_input: dict = None
    ) -> List[ResourceAction]:
        return self.service_handler.list_deploy_service_actions(
            service_input=service_input
        )
    
    def suggest_service_config(
        self,
        container_image: str,
    ) -> ServiceInput:
        return self.service_handler.suggest_service_config(
            container_image=container_image
        )

    def list_service_actions(
        self,
        service_id: str
    ) -> List[ResourceAction]:
        return self.service_handler.list_service_actions(
            service_id=service_id
        )
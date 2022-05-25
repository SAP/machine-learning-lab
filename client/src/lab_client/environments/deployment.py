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
        job_input: JobInput,
        action_id: Optional[str] = None,
        wait: bool = False) -> str:
        return self.job_handler.deploy_job(
            job_input=job_input, 
            action_id=action_id, 
            wait=wait
        )
    
    def deploy_jobs(
        self,
        job_inputs: List[JobInput],
        action_id: Optional[str] = None,
        wait: bool = False) -> List[str]:
        return self.job_handler.deploy_jobs(
            job_input=job_inputs, 
            action_id=action_id, 
            wait=wait
        )

    def get_job_metadata(self, job_id: str) -> Dict:
        return self.job_handler.get_job_metadata(job_id)
    
    def check_job_status(self, job_id: str) -> str:
        return self.job_handler.check_job_status(job_id)
    
    def wait_for_job_completion(self, job_id: str) -> bool:
        return self.job_handler.wait_for_job_completion(job_id)

    def wait_for_jobs_completion(self, job_ids: List[str]) -> bool:
        return self.job_handler.wait_for_jobs_completion(job_ids)
    
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
    
    def list_services(self) -> List[Service]:
        return self.service_handler.list_services()
    
    def deploy_service(
        self,
        service_input: ServiceInput,
        action_id: Optional[str] = None,
        wait: bool = False) -> str:
        return self.service_handler.deploy_service(
            service_input=service_input, 
            action_id=action_id, 
            wait=wait
        )
    
    def deploy_services(
        self,
        service_inputs: List[ServiceInput],
        action_id: Optional[str] = None,
        wait: bool = False) -> List[str]:
        return self.service_handler.deploy_services(
            service_input=service_inputs, 
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
    
    def check_service_status(self, service_id: str) -> str:
        return self.service_handler.check_service_status(
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
        service_update: ServiceUpdate
    ) -> Service:

        return self.service_handler.update_service(
            service_id=service_id,
            service_update=service_update
        )
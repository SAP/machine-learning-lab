from contaxy.clients.deployment import DeploymentClient
from contaxy.schema import Job, JobInput, ResourceAction
from datetime import datetime

from typing import Dict, Iterator, List, Optional
from contaxy.schema.shared import ResourceActionExecution

class JobHandler:
    def __init__(self, env, deployment_client: DeploymentClient):
        # Initialize variables
        self.env = env
        self.deployment_client = deployment_client
        if not self.env.is_connected():
            raise RuntimeError("Environment is not connected to Lab!")
    
    def list_jobs(self) -> List[Job]:
        return self.deployment_client.list_jobs(
            project_id=self.env.project
        )
    
    def deploy_job(
        self,
        job_input: dict = None,
        action_id: Optional[str] = None,
        wait: bool = False) -> Job:

        job_input_obj = JobInput.parse_obj(job_input)

        return self.deployment_client.deploy_job(
            project_id=self.env.project, 
            job_input=job_input_obj, 
            action_id=action_id, 
            wait=wait
        )

    def get_job_metadata(self, job_id: str) -> Job:
        """
        Returns the job metadata for the given Job ID
        # Arguments
            job_id (string): Job Docker ID or Name.
        # Returns
        'LabJob' if job was found or 'None'.
        """
        return self.deployment_client.get_job_metadata(
            project_id=self.env.project, 
            job_id=job_id
        )
    
    def delete_job(self, job_id: str) -> None:
        self.deployment_client.delete_job(
            project_id=self.env.project, 
            job_id=job_id
        )
    
    def delete_jobs(self) -> None:
        self.deployment_client.delete_jobs(
            project_id=self.env.project
        )
    
    def get_job_logs(
        self,
        job_id: str,
        lines: Optional[int] = None,
        since: Optional[datetime] = None) -> str:
        return self.deployment_client.get_job_logs(
            project_id=self.env.project, 
            job_id=job_id, 
            lines=lines, 
            since=since
        )
    
    def list_deploy_job_actions(
        self,
        job_input: dict = None
    ) -> List[ResourceAction]:

        job_input_obj = JobInput.parse_obj(job_input)

        return self.deployment_client.list_deploy_job_actions(
            project_id=self.env.project,
            job=job_input_obj
        )

    def suggest_job_config(
        self,
        container_image: str,
    ) -> JobInput:

        return self.deployment_client.suggest_job_config(
            project_id=self.env.project,
            container_image=container_image
        )
    
    def list_job_actions(
        self,
        job_id: str
    ) -> List[ResourceAction]:

        return self.deployment_client.list_job_actions(
            project_id=self.env.project,
            job_id=job_id
        )

    def execute_job_action(
        self,
        job_id: str,
        action_id: str,
        action_execution: ResourceActionExecution = ResourceActionExecution()
    ) -> None:
        pass
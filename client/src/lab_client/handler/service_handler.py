from contaxy.clients.deployment import DeploymentClient
from contaxy.schema import Service, ServiceInput, ResourceAction
from datetime import datetime

from typing import Dict, List, Optional
from contaxy.schema.deployment import ServiceUpdate
from contaxy.schema.shared import ResourceActionExecution

class ServiceHandler:
    def __init__(self, env, deployment_client: DeploymentClient):
        # Initialize variables
        self.env = env
        self.deployment_client = deployment_client
        if not self.env.is_connected():
            raise RuntimeError("Environment is not connected to Lab!")
    
    def list_services(self) -> List[Service]:
        return self.deployment_client.list_services(
            project_id=self.env.project
        )
    
    def deploy_service(
        self,
        service_input: dict = None,
        action_id: Optional[str] = None,
        wait: bool = False) -> Service:

        service_input_obj = ServiceInput.parse_obj(service_input)

        return self.deployment_client.deploy_service(
            project_id=self.env.project, 
            service_input=service_input_obj, 
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
        return self.deployment_client.get_service_metadata(
            project_id=self.env.project, 
            service_id=service_id
        )
    
    def delete_service(self, service_id: str) -> None:
        self.deployment_client.delete_service(
            project_id=self.env.project, 
            service_id=service_id
        )
    
    def delete_services(self) -> None:
        self.deployment_client.delete_services(
            project_id=self.env.project
        )
    
    def get_service_logs(
        self,
        service_id: str,
        lines: Optional[int] = None,
        since: Optional[datetime] = None) -> str:
        return self.deployment_client.get_service_logs(
            project_id=self.env.project, 
            service_id=service_id, 
            lines=lines, 
            since=since
        )
    
    def update_service(
        self,
        service_id: str,
        service_update: dict = None
    ) -> Service:

        service_update_obj = ServiceUpdate.parse_obj(service_update)
        return self.deployment_client.update_service(
            project_id=self.env.project,
            service_id=service_id,
            service=service_update_obj
        )

    def update_service_access(
        self,
        service_id: str
    ) -> None:
        return self.deployment_client.update_service_access(
            project_id=self.env.project,
            service_id=service_id
        )

    def list_deploy_service_actions(
        self,
        service_input: dict = None
    ) -> List[ResourceAction]:
        service_input_obj = ServiceInput.parse_obj(service_input)

        return self.deployment_client.list_deploy_service_actions(
            project_id=self.env.project,
            service=service_input_obj
        )
    

    def suggest_service_config(
        self,
        container_image: str,
    ) -> ServiceInput:
        return self.deployment_client.suggest_service_config(
            project_id=self.env.project,
            container_image=container_image
        )

    def list_service_actions(
        self,
        service_id: str
    ) -> List[ResourceAction]:
        return self.deployment_client.list_service_actions(
            project_id=self.env.project,
            service_id=service_id
        )

    def access_service(
        self,
        service_id: str,
        endpoint: str,
    ) -> None:
        pass
    
    def execute_service_action(
        self,
        service_id: str,
        action_id: str,
        action_execution: ResourceActionExecution = ResourceActionExecution()
    ) -> None:
        pass
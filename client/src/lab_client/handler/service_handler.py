from contaxy.clients.deployment import DeploymentClient
from contaxy.schema import Service, ServiceInput
from datetime import datetime

from typing import List, Optional
from contaxy.schema.deployment import ServiceUpdate

from loguru import logger


class ServiceHandler:
    def __init__(self, env, deployment_client: DeploymentClient):
        # Initialize variables
        self.env = env
        self.deployment_client = deployment_client
        if not self.env.is_connected():
            raise RuntimeError("Environment is not connected to Lab!")

    def list_services(self) -> List[Service]:
        """List all services under the current project.

        Returns:
            List[Service]: List of all services.
        """
        logger.info('Listing all services under project : ' + self.env.project)
        services_list = self.deployment_client.list_services(
            project_id=self.env.project
        )
        logger.info('Found ' + str(len(services_list)) + ' services!')
        return services_list

    def deploy_service(
        self,
        service_input: ServiceInput,
        action_id: Optional[str] = None,
        wait: bool = False) -> str:
        """Deploys a service under a specific project.

        Args:
            service_input (ServiceInput): Service input with their container-image and display-name.
            action_id (Optional[str], optional): The ID of the action. Defaults to None.
            wait (bool, optional): If 'True', the return will be until service completion.. Defaults to False.

        Returns:
            str: Service ID.
        """
        logger.info('Deploying service ' + service_input.display_name + ' under project : ' + self.env.project)
        deployed_service = self.deployment_client.deploy_service(
            project_id=self.env.project,
            service_input=service_input,
            action_id=action_id,
            wait=wait
        )
        if deployed_service is None:
            logger.info('Service ' + service_input.display_name + ' could not be deployed successfully!')
            return None
        return deployed_service.id

    def deploy_services(
        self,
        service_inputs: List[ServiceInput],
        action_id: Optional[str] = None,
        wait: bool = False) -> List[str]:
        """Deploys multiple services under a specific project.

        Args:
            service_inputs (List[ServiceInput]): The list of service inputs with their container-image and display-name.
            action_id (Optional[str], optional): The ID of the action. Defaults to None.
            wait (bool, optional): If 'True', the return will be until service completion. Defaults to False.

        Returns:
            List[str]: The Service IDs of all services.
        """
        all_services = []
        logger.info('Deploying ' + len(service_inputs) + ' services under project : ' + self.env.project)
        for service_input in service_inputs:
            deployed_serv_id = self.deploy_service(
                service_input=service_input,
                action_id=action_id,
                wait=wait
            )
            if deployed_serv_id:
                all_services.append(deployed_serv_id)
        logger.info('Successfully deployed ' + len(all_services) + ' services!')
        return all_services

    def get_service(self, service_id: str) -> Service:
        """
        Returns the service metadata for the given Service ID
        # Arguments
            service_id (string): Service Docker ID or Name.
        # Returns
        'Service' if service was found or 'None'.
        """
        logger.info('Fetching metadata of service ' + service_id)
        service = self.deployment_client.get_service_metadata(
            project_id=self.env.project,
            service_id=service_id
        )
        if service:
            return service
        return None

    def check_service_status(self, service_id: str) -> str:
        """Check service status.

        Args:
            service_id (str): Job ID.

        Returns:
            str: Status of service
        """
        logger.info('Checking status of service ' + service_id)
        service = self.deployment_client.get_service_metadata(
            project_id=self.env.project,
            service_id=service_id
        )
        if service:
            logger.info('Fetching service status!')
            return service.status
        return None

    def delete_service(self, service_id: str) -> None:
        """Delete a specific service

        Args:
            service_id (str): Service ID.
        """
        logger.info('Deleting service ' + service_id)
        self.deployment_client.delete_service(
            project_id=self.env.project,
            service_id=service_id
        )

    def delete_services(self) -> None:
        """Deletes all services of a project.
        """
        self.deployment_client.delete_services(
            project_id=self.env.project
        )

    def get_service_logs(
        self,
        service_id: str,
        lines: Optional[int] = None,
        since: Optional[datetime] = None) -> str:
        """Get the logs of a service.

        Args:
            service_id (str): Service ID.
            lines (Optional[int], optional): The number of lines in the logs to be returned. Defaults to None.
            since (Optional[datetime], optional): The date time. Defaults to None.

        Returns:
            str: Logs as a string
        """
        return self.deployment_client.get_service_logs(
            project_id=self.env.project,
            service_id=service_id,
            lines=lines,
            since=since
        )

    def update_service(
        self,
        service_id: str,
        service_update: ServiceUpdate
    ) -> Service:
        """Update an already deployed service.

        Args:
            service_id (str): Service ID.
            service_update (dict, optional): _description_. Defaults to None.

        Returns:
            Service: _description_
        """
        service = self.deployment_client.update_service(
            project_id=self.env.project,
            service_id=service_id,
            service=service_update
        )

        if service is None:
            logger.info('Service could not be updated successfully!')
            return None
        logger.info('Updated service ' + service.display_name + ' under project : ' + self.env.project)
        return service.id

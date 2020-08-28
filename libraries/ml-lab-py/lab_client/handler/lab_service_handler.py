import logging
import time

from tqdm import tqdm

from lab_api.swagger_client import LabService
from lab_api.swagger_client.rest import ApiException
from lab_client.handler import lab_api_handler


class LabServiceConfig:
    """
    Service configuration for deployment.

    # Arguments
        image (string): Docker image for the service.
        name (string): Name of the service (optional).
        params (dict): Environment variables passed to the service (optional).
    """

    def __init__(self, image: str, name: str = None, params: dict = None):
        # Initialize logger
        self.log = logging.getLogger(__name__)

        # Initialize variables
        self.image = image
        self.name = name
        self.params = params


class LabServiceHandler:
    def __init__(self, env, check_wait_time: int = 10):
        # Initialize logger
        self.log = logging.getLogger(__name__)

        # Initialize variables
        self.env = env
        self.check_wait_time = check_wait_time

    def _lab_handler(self) -> lab_api_handler.LabApiHandler:
        return self.env.lab_handler

    def deploy_services(self, service_configs: list, check_status: bool = True, parallel: bool = False):
        """
        Deploy multiple services in a queue or in parallel and waits for those services to become healthy.

        # Arguments
            service_configs (list): List of service configurations for deployment.
            check_status (bool): If 'True', this method checks the service status and waits until it is healthy (optional).
            parallel (bool): If `True`, all services will be deployed in parallel (optional)

        # Returns
        'True', if services have been deployed successfully. 'False', if the service deployment has failed.
        """
        if not parallel and not check_status:
            self.log.error("Cannot run as queue (not parallel) without checking the status.")
            return False

        if not parallel:
            for service_config in service_configs:
                if not self.deploy_service(service_config, True):
                    return False
            return True

        deployed_services = []
        for service_config in service_configs:
            deployed_service = self._deploy_service(service_config)
            if deployed_service:
                deployed_services.append(deployed_service)

        if check_status:
            self.check_services_status(deployed_services)

        return True

    def deploy_service(self, service_config: LabServiceConfig, check_status: bool = True) -> bool:
        """
        Deploy a service and wait for it be shown as healthy.

        # Arguments
            service_config (LabServiceConfig): Lab service configuration for deployment.
            check_status (bool): If 'True', this method checks the service status and waits until it healthy (optional).

        # Returns
        'True', if service was deployed successfully. 'False', if service deployment has failed.
        """
        deployed_service = self._deploy_service(service_config)

        if not deployed_service:
            return False

        if check_status:
            return self.check_service_status(deployed_service)

        return True

    def update_service(self, service_config: LabServiceConfig, check_status: bool = True) -> bool:
        """
        Update a service with the same name and wait for it be shown as healthy.

        # Arguments
            service_config (LabServiceConfig): Lab service configuration for deployment.
            check_status (bool): If 'True', this method checks the service status and waits until it healthy (optional).

        # Returns
        'True', if service was updated successfully. 'False', if service update has failed.
        """
        self.log.info("Updating " + service_config.name + "...")
        deployed_service = None
        try:
            deployed_service = self.get_service(service_config.name)
        except ApiException as ex:
            # Service not found (most likely) -> nothing to delete
            deployed_service = None

        if deployed_service:
            try:
                self.log.info("Deleting existing " + deployed_service.name + "...")
                deletion_response = self._lab_handler().lab_api.delete_service(
                    project=self.env.project,
                    service=deployed_service.name)

                if deletion_response is None or not self._lab_handler().request_successful(deletion_response):
                    self.log.error("Failed to delete service: " + deployed_service.name)
                    return False
            except ApiException as ex:
                self.log.error("Failed to delete service: " + deployed_service.name, exc_info=ex)
                return False
        else:
            self.log.info("No existing service was found with the name: " + service_config.name)

        # wait a few seconds before trying deployment
        # TODO: required? higher waittime?
        time.sleep(10)
        return self.deploy_service(service_config, check_status=check_status)

    def get_service(self, service_id: str) -> LabService or None:
        """
        Returns the service for the given Service ID

        # Arguments
            service_id (string): Service Docker ID or Name.
        # Returns
        'LabService' if service was found or 'None'.
        """

        deployed_service_status = self._lab_handler().lab_api.get_service(project=self.env.project,
                                                                          service=service_id)
        if deployed_service_status is None or not deployed_service_status.data \
                or not self._lab_handler().request_successful(deployed_service_status):
            self.log.info("Failed to get service: " + service_id)
            return None

        return deployed_service_status.data

    def check_service_status(self, service: LabService):
        return self.check_services_status([service])

    def check_services_status(self, services: list):
        if len(services) == 1:
            pbar = tqdm(desc="Deploying service: " + services[0].name, unit="check")
        else:
            services_str = ""
            for service in services:
                services_str += service.name + "; "
            pbar = tqdm(desc="Deploying services: " + services_str, unit="check")

        deployed_services = services

        successful = True
        while len(deployed_services) > 0:
            time.sleep(self.check_wait_time)  # Wait X seconds for every check
            pbar.update()

            services_to_check = deployed_services
            for service in services_to_check:
                deployed_service_status = None
                try:
                    deployed_service_status = self.get_service(service.docker_name)
                except ApiException as ex:
                    self.log.warning("Failed to get service: " + service.name, exc_info=ex)
                    
                if deployed_service_status is None or deployed_service_status.is_healthy is None:
                    self.log.info("Failed to check if " + service.name + " is healthy.")
                    deployed_services.remove(service)

                if str(deployed_service_status.is_healthy).lower() == "false":
                    # TODO: max wait time
                    # successful = False
                    # self.log.info(service.name + " has failed with status: " + str(deployed_service_status.status))
                    # deployed_services.remove(service)
                    # try:
                    #     service_logs = self._lab_handler().lab_api.get_service_logs(service.docker_name)
                    #    if service_logs and service_logs.data:
                    #        self.log.info("Last log output of " + services.name + ": " + str(
                    #            service_logs.data[-min(service_logs.data, 1000):]))
                    # except:
                    #    pass

                    continue
                else:
                    self.log.info(service.name + " is healthy.")
                    deployed_services.remove(service)

        pbar.close()
        return successful

    def _deploy_service(self, service_config: LabServiceConfig):
        args = {}
        if service_config.name:
            args["name"] = service_config.name

        if service_config.params:
            args["body"] = service_config.params

        if not service_config.image:
            self.log.error("No image provided for the service.")
            return None

        deployed_service = self._lab_handler().lab_api.deploy_service(
            project=self.env.project,
            image=service_config.image,
            **args)
        if deployed_service is None or not deployed_service.data or not self._lab_handler().request_successful(
                deployed_service):
            self.log.info("Failed to deploy the service.")
            return None

        self.log.info("Successfully deployed the service with id: " + str(deployed_service.data.docker_id)
                      + "; Parameters: " + str(service_config.params))

        return deployed_service.data

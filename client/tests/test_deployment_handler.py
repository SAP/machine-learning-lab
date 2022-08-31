from contaxy.schema.deployment import JobInput, ServiceInput, ServiceUpdate

from lab_client import Environment
from .conftest import test_settings
import pytest


@pytest.mark.integration
class TestJob:

    def test_deploy_job_and_delete(self) -> None:
        env = Environment(lab_endpoint=test_settings.LAB_BACKEND,
                          lab_api_token=test_settings.LAB_TOKEN,
                          project=test_settings.LAB_PROJECT)

        input = JobInput(
            container_image='hello-world:latest',
            display_name='Job1'
        )
        job_id = env.job_handler.deploy_job(input, wait=True)
        assert job_id is not None

        env.job_handler.delete_job(job_id)

    def test_list_jobs_get_job_metadata(self) -> None:
        env = Environment(lab_endpoint=test_settings.LAB_BACKEND,
                          lab_api_token=test_settings.LAB_TOKEN,
                          project=test_settings.LAB_PROJECT)

        input = JobInput(
            container_image='ubuntu:latest',
            display_name='Job2',
            command=['/bin/bash', '-c', '--'],
            args=['sleep 5']
        )
        job_id = env.job_handler.deploy_job(input)

        job_metadata = env.job_handler.get_job_metadata(job_id)
        assert job_metadata['ctxy.deploymentId'] == job_id

        job_list = env.job_handler.list_jobs()
        assert len(job_list) > 0

        job_status = env.job_handler.check_job_status(job_id)
        assert job_status == "running"

        status = env.job_handler.wait_for_job_completion(job_id)
        assert status == True

        env.job_handler.delete_jobs()
        job_list = env.job_handler.list_jobs()
        assert len(job_list) == 0

    def test_job_logs(self) -> None:
        env = Environment(lab_endpoint=test_settings.LAB_BACKEND,
                          lab_api_token=test_settings.LAB_TOKEN,
                          project=test_settings.LAB_PROJECT)
        job_input = JobInput(
            container_image='hello-world:latest',
            display_name='Job3'
        )
        job_id = env.job_handler.deploy_job(job_input)

        job_logs = env.job_handler.get_job_logs(job_id)
        assert job_logs[-min(len(job_logs), 1000):] is not None

        status = env.job_handler.wait_for_job_completion(job_id)
        assert status == True

        env.job_handler.delete_jobs()


@pytest.mark.integration
class TestService:

    def test_deploy_service_update_and_delete(self) -> None:
        env = Environment(lab_endpoint=test_settings.LAB_BACKEND,
                          lab_api_token=test_settings.LAB_TOKEN,
                          project=test_settings.LAB_PROJECT)

        service_input = ServiceInput(
            container_image='hello-world:latest',
            display_name='Service1'
        )
        service_id = env.service_handler.deploy_service(service_input)
        assert service_id is not None

        env.service_handler.delete_service(service_id)

    def test_get_service_list_and_metadata(self) -> None:
        env = Environment(lab_endpoint=test_settings.LAB_BACKEND,
                          lab_api_token=test_settings.LAB_TOKEN,
                          project=test_settings.LAB_PROJECT)
        service_input = ServiceInput(
            container_image='hello-world:latest',
            display_name='Service2'
        )
        service_id = env.service_handler.deploy_service(service_input)

        serv_status = env.service_handler.check_service_status(service_id)
        assert serv_status == 'succeeded'

        service_meta = dict()
        service_meta['description'] = 'This is hello world!'
        service_input = ServiceUpdate(
            metadata=service_meta
        )
        service_id = env.service_handler.update_service(service_id, service_input)
        assert service_id is not None

        service = env.service_handler.get_service(service_id)
        assert service_meta['description'] == service.metadata['description']

        service_list = env.service_handler.list_services()
        assert len(service_list) > 0

        env.service_handler.delete_services()

    def test_service_logs(self) -> None:
        env = Environment(lab_endpoint=test_settings.LAB_BACKEND,
                          lab_api_token=test_settings.LAB_TOKEN,
                          project=test_settings.LAB_PROJECT)
        service_input = ServiceInput(
            container_image='hello-world:latest',
            display_name='Service3'
        )
        service_id = env.service_handler.deploy_service(service_input)

        service_logs = env.service_handler.get_service_logs(service_id)
        assert service_logs[-min(len(service_logs), 1000):] is not None

        env.service_handler.delete_services()

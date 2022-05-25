from importlib.metadata import metadata
from contaxy.schema.deployment import JobInput, ServiceInput, ServiceUpdate

from lab_client import Environment
from .conftest import test_settings
import requests
import pytest

@pytest.mark.integration
class TestJob:

    def test_creation(self) -> None:
        env = Environment(lab_endpoint=test_settings.LAB_BACKEND,
                          lab_api_token=test_settings.LAB_TOKEN,
                          project=test_settings.LAB_PROJECT)
        assert env._LOCAL_OPERATOR == "local"

    def test_exceptions(self) -> None:
        with pytest.raises(requests.exceptions.ConnectionError):
            Environment(lab_endpoint='http://not-an-endpoint',
                        lab_api_token=test_settings.LAB_TOKEN,
                        project=test_settings.LAB_PROJECT)
        with pytest.raises(ConnectionError):
            Environment(lab_endpoint=test_settings.LAB_BACKEND,
                        lab_api_token='not-valid-token',
                        project=test_settings.LAB_PROJECT)
    
    def test_deploy_job_and_delete(self) -> None:
        env = Environment(lab_endpoint=test_settings.LAB_BACKEND,
                          lab_api_token=test_settings.LAB_TOKEN,
                          project=test_settings.LAB_PROJECT)
        
        input = JobInput(
            container_image = 'hello-world:latest',
            display_name = 'Job1'
        )
        job_id = env.deploy_job(input, wait=True)
        assert job_id is not None
        
        env.delete_job(job_id)

    def test_list_jobs_get_job_metadata(self) -> None:
        env = Environment(lab_endpoint=test_settings.LAB_BACKEND,
                          lab_api_token=test_settings.LAB_TOKEN,
                          project=test_settings.LAB_PROJECT)
        
        input = JobInput(
            container_image = 'ubuntu:latest',
            display_name = 'Job2',
            command=['/bin/bash','-c','--'],
            args=['sleep 30']
        )
        job_id = env.deploy_job(input)

        job_metadata = env.get_job_metadata(job_id)
        assert job_metadata['ctxy.deploymentId'] == job_id

        job_list = env.list_jobs()
        assert len(job_list) > 0

        job_status = env.check_job_status(job_id)
        assert job_status == "running"

        status = env.wait_for_job_completion(job_id)
        assert status == True

        env.delete_jobs()
        job_list = env.list_jobs()
        assert len(job_list) == 0
    
    def test_job_logs(self) -> None:
        env = Environment(lab_endpoint=test_settings.LAB_BACKEND,
                          lab_api_token=test_settings.LAB_TOKEN,
                          project=test_settings.LAB_PROJECT)
        job_input = JobInput(
            container_image = 'hello-world:latest',
            display_name = 'Job3'
        )
        job_id = env.deploy_job(job_input)

        job_logs = env.get_job_logs(job_id)
        assert job_logs[-min(len(job_logs), 1000):] is not None

        status = env.wait_for_job_completion(job_id)
        assert status == True

        env.delete_jobs()

@pytest.mark.integration
class TestService:

    def test_creation(self) -> None:
        env = Environment(lab_endpoint=test_settings.LAB_BACKEND,
                          lab_api_token=test_settings.LAB_TOKEN,
                          project=test_settings.LAB_PROJECT)
        assert env._LOCAL_OPERATOR == "local"

    def test_exceptions(self) -> None:
        with pytest.raises(requests.exceptions.ConnectionError):
            Environment(lab_endpoint='http://not-an-endpoint',
                        lab_api_token=test_settings.LAB_TOKEN,
                        project=test_settings.LAB_PROJECT)
        with pytest.raises(ConnectionError):
            Environment(lab_endpoint=test_settings.LAB_BACKEND,
                        lab_api_token='not-valid-token',
                        project=test_settings.LAB_PROJECT)
    
    def test_deploy_service_update_and_delete(self) -> None:
        env = Environment(lab_endpoint=test_settings.LAB_BACKEND,
                          lab_api_token=test_settings.LAB_TOKEN,
                          project=test_settings.LAB_PROJECT)

        service_input = ServiceInput(
            container_image = 'hello-world:latest',
            display_name = 'Service1'
        )
        service_id = env.deploy_service(service_input)
        assert service_id is not None

        env.delete_service(service_id)
        
    def test_get_service_list_and_metadata(self) -> None:
        env = Environment(lab_endpoint=test_settings.LAB_BACKEND,
                          lab_api_token=test_settings.LAB_TOKEN,
                          project=test_settings.LAB_PROJECT)
        service_input = ServiceInput(
            container_image = 'hello-world:latest',
            display_name = 'Service2'
        )
        service_id = env.deploy_service(service_input)
        
        serv_status = env.check_service_status(service_id)
        assert serv_status == 'succeeded'

        service_meta = dict()
        service_meta['desciption'] = 'This is hello world!'
        service_input = ServiceUpdate(
            metadata=service_meta
        )
        service_id = env.update_service(service_id, service_input)
        assert service_id is not None

        service_metadata = env.get_service_metadata(service_id)
        assert service_meta.items() <= service_metadata.items()

        service_list = env.list_services()
        assert len(service_list) > 0

        env.delete_services()
    
    def test_service_logs(self) -> None:
        env = Environment(lab_endpoint=test_settings.LAB_BACKEND,
                          lab_api_token=test_settings.LAB_TOKEN,
                          project=test_settings.LAB_PROJECT)
        service_input = ServiceInput(
            container_image = 'hello-world:latest',
            display_name = 'Service3'
        )
        service_id = env.deploy_service(service_input)

        service_logs = env.get_service_logs(service_id)
        assert service_logs[-min(len(service_logs), 1000):] is not None

        env.delete_services()
    

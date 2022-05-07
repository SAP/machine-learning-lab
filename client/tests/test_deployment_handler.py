from importlib.metadata import metadata
import os
import json

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
        job_input = dict()
        job_input['container_image'] = 'hello-world:latest'
        job_input['display_name'] = 'Job1'
        job_data = env.deploy_job(job_input)

        env.delete_job(job_data.id)

    def test_list_jobs_get_job_metadata(self) -> None:
        env = Environment(lab_endpoint=test_settings.LAB_BACKEND,
                          lab_api_token=test_settings.LAB_TOKEN,
                          project=test_settings.LAB_PROJECT)
        job_input = dict()
        job_input['container_image'] = 'hello-world:latest'
        job_input['display_name'] = 'Job2'
        job_data = env.deploy_job(job_input)
        assert job_data.container_image == job_input['container_image']

        job_metadata = env.get_job_metadata(job_data.id)
        assert job_metadata.id == job_data.id

        job_list = env.list_jobs()
        assert len(job_list) > 0

        env.delete_jobs()
        job_list = env.list_jobs()
        assert len(job_list) == 0
    
    def test_job_logs(self) -> None:
        env = Environment(lab_endpoint=test_settings.LAB_BACKEND,
                          lab_api_token=test_settings.LAB_TOKEN,
                          project=test_settings.LAB_PROJECT)
        job_input = dict()
        job_input['container_image'] = 'hello-world:latest'
        job_input['display_name'] = 'Job3'
        job_data = env.deploy_job(job_input)
        assert job_data.container_image == job_input['container_image']

        logs = env.get_job_logs(job_data.id)
        assert logs is not None

        actions = env.list_deploy_job_actions(job_input)
        assert len(actions) > 0

        config = env.suggest_job_config(job_input['container_image'])
        assert config is not None

        act = env.list_job_actions(job_data.id)
        assert act is not None

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
    
    def test_deploy_service(self) -> None:
        env = Environment(lab_endpoint=test_settings.LAB_BACKEND,
                          lab_api_token=test_settings.LAB_TOKEN,
                          project=test_settings.LAB_PROJECT)
        service_input = dict()
        service_input['container_image'] = 'hello-world:latest'
        service_input['display_name'] = 'Service1'
        service = env.deploy_service(service_input)
        assert service.container_image == service_input['container_image']
        
    def test_get_service_list_and_metadata(self) -> None:
        env = Environment(lab_endpoint=test_settings.LAB_BACKEND,
                          lab_api_token=test_settings.LAB_TOKEN,
                          project=test_settings.LAB_PROJECT)
        service_input = dict()
        service_input['container_image'] = 'hello-world:latest'
        service_input['display_name'] = 'Service2'
        service = env.deploy_service(service_input)
        assert service.container_image == service_input['container_image']

        service_metadata = env.get_service_metadata(service.id)
        assert service_metadata.id == service.id

        service_list = env.list_services()
        assert len(service_list) > 0
    
    def test_service_logs(self) -> None:
        env = Environment(lab_endpoint=test_settings.LAB_BACKEND,
                          lab_api_token=test_settings.LAB_TOKEN,
                          project=test_settings.LAB_PROJECT)
        service_input = dict()
        service_input['container_image'] = 'hello-world:latest'
        service_input['display_name'] = 'Service3'
        service_data = env.deploy_service(service_input)
        assert service_data.container_image == service_input['container_image']

        logs = env.get_service_logs(service_data.id)
        assert logs is not None

        actions = env.list_deploy_service_actions(service_input)
        assert len(actions) > 0

        config = env.suggest_service_config(service_input['container_image'])
        assert config is not None

        act = env.list_service_actions(service_data.id)
        assert act is not None
    

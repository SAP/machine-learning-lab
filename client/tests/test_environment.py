import os
import tempfile

import contaxy.schema.exceptions

from lab_client import Environment
from .conftest import test_settings
import requests
import pytest


@pytest.mark.integration
class TestEnvironment:

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
        with pytest.raises(contaxy.schema.exceptions.PermissionDeniedError):
            Environment(lab_endpoint=test_settings.LAB_BACKEND,
                        lab_api_token=test_settings.LAB_TOKEN,
                        project='not-existent-project')

    def test_upload(self) -> None:
        env = Environment(lab_endpoint=test_settings.LAB_BACKEND,
                          lab_api_token=test_settings.LAB_TOKEN,
                          project=test_settings.LAB_PROJECT)
        tf = tempfile.NamedTemporaryFile()
        file_key = env.upload_file(tf.name, "dataset")
        assert file_key == f"datasets/{tf.name.split(os.sep)[-1]}"

    def test_download(self) -> None:
        env = Environment(lab_endpoint=test_settings.LAB_BACKEND,
                          lab_api_token=test_settings.LAB_TOKEN,
                          project=test_settings.LAB_PROJECT)
        tf = tempfile.NamedTemporaryFile()
        sample_text = "This is a sample text\nWith two lines"
        with open(tf.name, 'w') as f:
            f.write(sample_text)
            f.seek(0)
            file_key = env.upload_file(tf.name, "dataset")

        local_path = env.get_file(file_key)
        assert os.path.exists(local_path)
        with open(local_path, 'r') as f:
            received_file = f.read()
        assert received_file == sample_text

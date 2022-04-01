from importlib.metadata import metadata
import os
import tempfile

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

    def test_upload_download_file(self) -> None:
        env = Environment(lab_endpoint=test_settings.LAB_BACKEND,
                          lab_api_token=test_settings.LAB_TOKEN,
                          project=test_settings.LAB_PROJECT)
        tf = tempfile.NamedTemporaryFile()
        sample_text = "This is a sample text\nWith two lines"
        with open(tf.name, 'w') as f:
            f.write(sample_text)
            f.seek(0)
            file_key = env.upload_file(tf.name, "dataset")
        assert file_key == f"datasets/{tf.name.split(os.sep)[-1]}"

        local_path = env.get_file(file_key)
        assert os.path.exists(local_path)
        with open(local_path, 'r') as f:
            received_file = f.read()
        assert received_file == sample_text

    def test_upload_download_folder(self) -> None:
        env = Environment(lab_endpoint=test_settings.LAB_BACKEND,
                          lab_api_token=test_settings.LAB_TOKEN,
                          project=test_settings.LAB_PROJECT)
        tf_dir = tempfile.TemporaryDirectory()

        tf_file = 'test.txt'
        sample_text = "This is a sample text\nWith two lines"
        with open(os.path.join(tf_dir.name, tf_file), 'w') as f:
            f.write(sample_text)
            f.seek(0)
        
        file_key = env.upload_folder(tf_dir.name, "dataset")
        assert file_key == f"datasets/{tf_dir.name.split(os.sep)[-1]}.zip"

        #Test to download zip file as a folder and return the folder path with unpack=True
        local_path = env.get_file(file_key, unpack=True)
        assert os.path.exists(local_path)
        
        for _, _, files in os.walk(local_path):
            for filename in files:
                assert filename == tf_file

        #Test to upload folder with custom name
        file_key = env.upload_folder(tf_dir.name, "dataset", None, "abc")
        assert file_key == f"datasets/abc.zip"
    
    def test_upload_download_file_metadata(self) -> None:
        env = Environment(lab_endpoint=test_settings.LAB_BACKEND,
                          lab_api_token=test_settings.LAB_TOKEN,
                          project=test_settings.LAB_PROJECT)
        tf = tempfile.NamedTemporaryFile()
        sample_text = "This is a sample text\nWith two lines"
        metadata_input = dict()
        metadata_input['description'] = 'This is test file'
        with open(tf.name, 'w') as f:
            f.write(sample_text)
            f.seek(0)
            file_key = env.upload_file(tf.name, "dataset", metadata=metadata_input)
        assert file_key == f"datasets/{tf.name.split(os.sep)[-1]}"

        file_metadata = env.get_file_metadata(env.project, file_key)
        for key, value in file_metadata.items():
            if 'description' in key:
                assert value == metadata_input['description']

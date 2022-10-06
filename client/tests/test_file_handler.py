import os
import tempfile

from lab_client import Environment
from .conftest import test_settings
import requests
import pytest


@pytest.mark.integration
class TestFile:

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

    def test_list_file(self):
        env = Environment(lab_endpoint=test_settings.LAB_BACKEND,
                          lab_api_token=test_settings.LAB_TOKEN,
                          project=test_settings.LAB_PROJECT)
        tf = tempfile.NamedTemporaryFile()
        with open(tf.name, 'w') as f:
            f.write("content")
        file_key_1 = env.upload_file(tf.name, "dataset")
        file_key_2 = env.upload_file(tf.name, "model")
        file_key_3 = env.upload_file(tf.name, "dataset", file_name="test/test.txt")

        datasets = [f.key for f in env.file_handler.list_remote_files(data_type="dataset")]
        assert file_key_1 in datasets
        assert file_key_2 not in datasets
        assert file_key_3 in datasets

        test_datasets = [f.key for f in env.file_handler.list_remote_files(data_type="dataset", prefix="test/")]
        assert file_key_1 not in test_datasets
        assert file_key_2 not in test_datasets
        assert file_key_3 in test_datasets

        all_files = [f.key for f in env.file_handler.list_remote_files()]
        assert file_key_1 in all_files
        assert file_key_2 in all_files
        assert file_key_3 in all_files

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

        # Test to download zip file as a folder and return the folder path with unpack=True
        local_path = env.get_file(file_key, unpack=True)
        assert os.path.exists(local_path)

        local_path = env.get_file(file_key, unpack=True)
        assert os.path.exists(local_path)

        for _, _, files in os.walk(local_path):
            for filename in files:
                assert filename == tf_file

        # Test to upload folder with custom name
        file_key = env.upload_folder(tf_dir.name, "dataset", None, "abc")
        assert file_key == f"datasets/abc.zip"

    def test_upload_download_file_metadata(self) -> None:
        env = Environment(lab_endpoint=test_settings.LAB_BACKEND,
                          lab_api_token=test_settings.LAB_TOKEN,
                          project=test_settings.LAB_PROJECT)
        tf = tempfile.NamedTemporaryFile()
        sample_text = "This is a sample text\nWith two lines"
        metadata_input = dict()
        metadata_input['custom_description'] = 'This is test file'
        with open(tf.name, 'w') as f:
            f.write(sample_text)
            f.seek(0)
            file_key = env.upload_file(tf.name, "dataset", metadata=metadata_input)
        assert file_key == f"datasets/{tf.name.split(os.sep)[-1]}"

        file_metadata = env.get_file_metadata(env.project, file_key)
        assert len(file_metadata.metadata.items()) == 1

    def test_download_folder_multiple_times(self) -> None:
        env = Environment(lab_endpoint=test_settings.LAB_BACKEND,
                          lab_api_token=test_settings.LAB_TOKEN,
                          project=test_settings.LAB_PROJECT)
        tf_dir = tempfile.TemporaryDirectory()
        dirname = tf_dir.name

        tf_file_1 = 'test.txt'
        sample_text = "This is a sample text\nWith two lines"
        with open(os.path.join(tf_dir.name, tf_file_1), 'w') as f:
            f.write(sample_text)
            f.seek(0)

        file_key_1 = env.upload_folder(tf_dir.name, "dataset")

        local_path = env.get_file(file_key_1, unpack=True)
        assert os.path.exists(local_path)

        tf_dir = tempfile.TemporaryDirectory()

        tf_file_2 = 'sample.txt'
        sample_text = "This is a sample text with one line"
        with open(os.path.join(tf_dir.name, tf_file_2), 'w') as f:
            f.write(sample_text)
            f.seek(0)

        file_key_2 = env.upload_folder(tf_dir.name, "dataset", None, os.path.basename(dirname))

        local_path = env.get_file(file_key_2, unpack=True)
        assert os.path.exists(local_path)

        for _, _, files in os.walk(local_path):
            for filename in files:
                assert filename == tf_file_2

    @pytest.mark.xfail(raises=Exception)
    def test_file_upload_with_invalid_data_type(self) -> None:
        env = Environment(lab_endpoint=test_settings.LAB_BACKEND,
                          lab_api_token=test_settings.LAB_TOKEN,
                          project=test_settings.LAB_PROJECT)
        tf = tempfile.NamedTemporaryFile()
        sample_text = "This is a sample text\nWith two lines"
        with open(tf.name, 'w') as f:
            f.write(sample_text)
            f.seek(0)
            env.upload_file(tf.name, "datasets")

    @pytest.mark.xfail(raises=Exception)
    def test_folder_upload_with_invalid_data_type(self) -> None:
        env = Environment(lab_endpoint=test_settings.LAB_BACKEND,
                          lab_api_token=test_settings.LAB_TOKEN,
                          project=test_settings.LAB_PROJECT)
        tf_dir = tempfile.TemporaryDirectory()

        env.upload_folder(tf_dir.name, "models")

    @pytest.mark.xfail(raises=Exception)
    def test_list_file_with_invalid_data_type(self) -> None:
        env = Environment(lab_endpoint=test_settings.LAB_BACKEND,
                          lab_api_token=test_settings.LAB_TOKEN,
                          project=test_settings.LAB_PROJECT)
        tf = tempfile.NamedTemporaryFile()
        with open(tf.name, 'w') as f:
            f.write("content")
        env.upload_file(tf.name, "dataset")

        env.file_handler.list_remote_files(data_type="datasets")

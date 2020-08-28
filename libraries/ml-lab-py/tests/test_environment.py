import logging
import os
import tempfile
import time

from lab_api.swagger_client import LabProjectConfig
from lab_client import Environment
from lab_client.handler import lab_api_handler
from lab_client.handler.experiment_handler import Experiment, ExperimentState

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')

env = None
auth_token = None
# Install lab first on port 8099
LAB_ENDPOINT = "http://localhost:8099"

# Create the Logger
log = logging.getLogger(__name__)


def setup_module(module):
    global env
    global auth_token

    log.info("setup_module - module:%s" % module.__name__)

    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(name)s %(lineno)d - %(levelname)s - %(message)s')

    log.info("Log in with default admin user and get token")
    lab_handler = lab_api_handler.LabApiHandler(lab_endpoint=LAB_ENDPOINT)
    # use default admin user to login and get jwt token
    auth_token = lab_handler.auth_api.login_user(
        authorization=lab_api_handler.get_basic_auth_token("admin", "admin")).data

    log.info("Requested auth token: " + auth_token)


def setup_function(function):
    global env
    log.info("Initialize environment")
    test_project = "test-" + str(int(round(time.time() * 1000)))
    env = Environment(project=test_project,
                      root_folder="temp",
                      lab_endpoint=LAB_ENDPOINT,
                      lab_api_token=auth_token)
    env.print_info()

    log.info("Create test project: " + test_project)
    env.lab_handler.lab_api.create_project(LabProjectConfig(name=test_project))
    log.info("Project created.")

    env = Environment(project=test_project,
                      root_folder="temp",
                      lab_endpoint=LAB_ENDPOINT,
                      lab_api_token=auth_token)
    env.print_info()


def teardown_function(function):
    log.info("Delete project")
    env.lab_handler.lab_api.delete_project(env.project)


def teardown_module(module):
    log.info("teardown_module - module:%s" % module.__name__)


def test_experiment_tracking():
    exp = env.create_experiment("this is a test")
    exp.print_info()

    # Create test resources
    temp_folder = tempfile.mkdtemp()

    file_1_name = "file-1.txt"
    file_1_path = os.path.join(temp_folder, file_1_name)
    file_1_content = "test"

    with open(file_1_path, 'w') as f:
        f.write(file_1_content)

    uploaded_file_1_key = env.upload_file(file_1_path, Environment.DataType.DATASET)
    assert uploaded_file_1_key

    TEST_PARAM_NAME = "test"
    TEST_PARAM_VALUE = "test"

    TEST_METRIC_NAME = "score"
    TEST_METRIC_VALUE = 1

    def my_exp(exp: Experiment):
        log.info("do something")
        assert exp.create_file_path("my_file.txt")
        exp.tensorboard_logger.add_scalar("test", 5)

        exp.log_metric(TEST_PARAM_NAME, exp.params[TEST_PARAM_NAME])
        exp.log_metric(TEST_METRIC_NAME, TEST_METRIC_VALUE)
        assert exp.exp_metadata.status == ExperimentState.RUNNING
        assert exp._running
        assert exp._has_run

    exp.log_other(TEST_PARAM_NAME, TEST_PARAM_VALUE)

    params = {
        TEST_PARAM_NAME: TEST_PARAM_VALUE,
        "test2": "test"
    }

    # run via environment as active experiment
    env.active_exp.run_exp(my_exp, params)

    assert exp.exp_metadata.status == ExperimentState.COMPLETED
    assert exp.params[TEST_PARAM_NAME] == TEST_PARAM_VALUE
    assert exp.metrics[TEST_PARAM_NAME] == TEST_PARAM_VALUE
    assert exp.exp_metadata.metrics[TEST_METRIC_NAME] == TEST_METRIC_VALUE
    assert exp._other_metadata[TEST_PARAM_NAME] == TEST_PARAM_VALUE
    assert len(exp.exp_metadata.resources.artifacts) == 1
    assert exp.exp_metadata.operator
    assert exp.exp_metadata.host.cpu_cores > 0
    assert exp.exp_metadata.resources.tensorboard_logs
    assert exp.exp_metadata.resources.stdout

    assert env.get_file(uploaded_file_1_key)

    assert uploaded_file_1_key in exp.exp_metadata.resources.input
    assert uploaded_file_1_key in exp.exp_metadata.resources.output

    exp.track_file_events = False

    assert env.upload_file(file_1_path, Environment.DataType.DATASET)
    assert len(exp.exp_metadata.resources.input) == 1

    assert exp.backup_exp()

    # Create second experiment without auto sync and file tracking
    exp2 = env.create_experiment("this is a second experiment")
    exp2.auto_sync = False
    exp2.track_file_events = False

    def my_exp_2(exp: Experiment):
        log.info("do something")
        assert exp.create_file_path("my_file.txt")
        exp.tensorboard_logger.add_scalar("test", 5)
        exp.log_metric(TEST_PARAM_NAME, exp.params[TEST_PARAM_NAME])
        exp.log_metric(TEST_METRIC_NAME, TEST_METRIC_VALUE)
        assert exp.exp_metadata.status == ExperimentState.RUNNING
        assert exp._running
        assert exp.auto_sync is False
        assert exp._has_run

    exp2.run_exp(my_exp_2, params)

    assert exp2.exp_metadata.status == ExperimentState.COMPLETED
    assert exp2.params[TEST_PARAM_NAME] == TEST_PARAM_VALUE
    assert exp2.metrics[TEST_PARAM_NAME] == TEST_PARAM_VALUE
    assert exp2.exp_metadata.metrics[TEST_METRIC_NAME] == TEST_METRIC_VALUE
    assert exp2.exp_metadata.resources.artifacts is None
    assert exp2.exp_metadata.operator
    assert exp2.exp_metadata.host.cpu_cores > 0
    assert exp2.exp_metadata.resources.tensorboard_logs is None
    assert exp2.exp_metadata.resources.stdout is None
    assert exp2.exp_metadata.resources.input is None
    assert exp2.exp_metadata.resources.output is None

    exp2.sync_exp(upload_data=True)

    assert exp2.exp_metadata.resources.tensorboard_logs
    assert exp2.exp_metadata.resources.stdout



def test_remote_file_handling():
    log.info("Start remote file handling tests.")

    log.info("Create test resources (files & folders)")
    temp_folder = tempfile.mkdtemp()

    file_1_name = "file-1.txt"
    file_1_path = os.path.join(temp_folder, file_1_name)
    file_1_content = "test"

    with open(file_1_path, 'w') as f:
        f.write(file_1_content)

    folder_1_name = "folder-1"
    folder_1_path = os.path.join(temp_folder, folder_1_name)
    os.makedirs(folder_1_path)

    file_2_name = "file-2.txt"
    file_2_path = os.path.join(folder_1_path, file_2_name)
    file_2_content = "content"
    with open(file_2_path, 'w') as f:
        f.write(file_2_content)

    # upload file file-1.txt to lab
    log.info("Upload file " + file_1_name + " to remote storage.")
    _TEST_METADATA = "test"
    uploaded_file_1_key = env.upload_file(file_1_path, Environment.DataType.DATASET,
                                          metadata={_TEST_METADATA: _TEST_METADATA})
    assert uploaded_file_1_key

    # get file info
    remote_file = env.file_handler.get_file_info(uploaded_file_1_key)
    assert (remote_file.version == 1)
    assert (remote_file.metadata[_TEST_METADATA] == _TEST_METADATA)

    # get file from lab
    log.info("Download file with key " + uploaded_file_1_key)
    downloaded_file = env.get_file(uploaded_file_1_key)
    assert downloaded_file
    # check file content
    with open(downloaded_file, 'r') as f:
        file_content = f.read()
        assert (file_content == file_1_content)

    # update file 1 content
    file_1_content_updated = "updated"

    with open(file_1_path, 'w') as f:
        f.write(file_1_content_updated)

    # Test versioning

    log.info("Upload updated file " + file_1_path)
    uploaded_key = env.upload_file(file_1_path, Environment.DataType.DATASET,
                                   metadata={_TEST_METADATA: _TEST_METADATA})

    # get file info
    remote_file = env.file_handler.get_file_info(uploaded_key)
    assert (remote_file.version == 2)
    assert (remote_file.metadata[_TEST_METADATA] == _TEST_METADATA)

    # get file from lab
    log.info("Download updated file " + uploaded_key)
    downloaded_file = env.get_file(uploaded_key)
    assert downloaded_file
    # check file content
    with open(downloaded_file, 'r') as f:
        file_content = f.read()
        assert (file_content == file_1_content_updated)

    # get version 1 of file
    log.info("Get file with specific version")
    key_without_version = env.file_handler.remove_version_from_key(uploaded_file_1_key)

    remote_file = env.file_handler.get_file_info(key_without_version + ".v1")
    assert (remote_file.version == 1)
    assert (remote_file.metadata[_TEST_METADATA] == _TEST_METADATA)

    # get version 1 file from lab
    downloaded_file = env.get_file(remote_file.key)
    assert downloaded_file
    # check file content
    with open(downloaded_file, 'r') as f:
        file_content = f.read()
        assert (file_content == file_1_content)

    # check if newest file is also loaded locally
    log.info("Load local file with key: " + key_without_version)
    local_file = env.file_handler.load_local_file(key_without_version)
    assert local_file
    log.info("Local file loaded: " + local_file)
    assert (env.file_handler.get_version_from_key(local_file) == 2)

    # upload folder folder-1 to lab
    log.info("Upload folder to remote storage " + folder_1_name)
    uploaded_folder_key = env.upload_folder(folder_1_path, Environment.DataType.DATASET,
                                            metadata={_TEST_METADATA: _TEST_METADATA})
    assert uploaded_folder_key
    # get file info
    remote_file = env.file_handler.get_file_info(uploaded_folder_key)
    assert (remote_file.version == 1)
    assert (remote_file.metadata[_TEST_METADATA] == _TEST_METADATA)

    # get folder folder-1 from lab
    log.info("Download and unpack folder with key " + uploaded_folder_key)
    downloaded_folder = env.get_folder(uploaded_folder_key)
    assert downloaded_folder
    # check folder content
    with open(os.path.join(downloaded_folder, file_2_name), 'r') as f:
        file_content = f.read()
        # check if folder contains file-1.txt
        assert (file_content == file_2_content)

    # Test remote interactions

    # test list datasets
    log.info("List remote datasets")
    assert (len(env.file_handler.list_remote_files(data_type=Environment.DataType.DATASET)) == 2)
    # delete remote file
    log.info("Delete remote file with key " + key_without_version)
    assert env.file_handler.delete_remote_file(key_without_version)

    # test list datasets
    log.info("test list datasets - should be one less")
    assert (len(env.file_handler.list_remote_files(data_type=Environment.DataType.DATASET)) == 1)


def test_local_file_handling():
    log.info("Start local file handling tests.")
    local_env = Environment(root_folder="temp")
    local_env.print_info()

    log.info("Create test resources (files & folders)")

    file_1_name = "file-1.txt"
    file_1_path = os.path.join(local_env.datasets_folder, file_1_name)
    file_1_content = "test"

    with open(file_1_path, 'w') as f:
        f.write(file_1_content)

    # get file from local
    local_file_key = "datasets/"+file_1_name
    log.info("Get local file with key " + local_file_key)
    local_file = local_env.get_file(local_file_key)
    assert local_file
    # check file content
    with open(local_file, 'r') as f:
        file_content = f.read()
        assert (file_content == file_1_content)



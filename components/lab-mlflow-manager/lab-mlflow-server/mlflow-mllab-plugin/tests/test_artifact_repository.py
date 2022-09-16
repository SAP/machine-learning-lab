import mlflow
from mlflow.tracking import MlflowClient
from mlflow.entities import Run
import pytest
import os
import uuid
import shutil
import pathlib
from helpers import get_safe_port, launch_artifact_repository_test_server


@pytest.fixture(scope="module", autouse=True)
def artifacts_server():
    """
    Starts and stops mlflow server and sets the tracking_uri.
    """
    api_key = "4239a609f81848440c1a4479492cc8fb5a320ccc"
    project_id = "test-project-id"
    port = get_safe_port()
    os.environ["LAB_API_TOKEN"] = api_key
    artifact_uri = "ml-lab://localhost:30010/{}".format(
        project_id)
    # artifact_uri = "."
    process = launch_artifact_repository_test_server(artifact_uri, port)
    mlflow.set_tracking_uri("http://localhost:{}".format(port))
    yield
    process.kill()
    if os.listdir("mlruns"):
        shutil.rmtree("mlruns")


@pytest.fixture(scope="function")
def run(artifacts_server):
    """
    Creates a run and returns the run object.
    """
    mlflow.start_run()
    yield mlflow.active_run()
    mlflow.end_run()


@pytest.fixture(scope="module")
def client() -> MlflowClient:
    """"
    Returns an instance of MlflowClient.
    """
    return MlflowClient()


def test_zero_artifacts(client: MlflowClient, run: Run) -> None:
    assert len(client.list_artifacts(run.info.run_id)) == 0


def test_log_one_artifact(client: MlflowClient, run: Run, tmp_path: pathlib.Path) -> None:
    text_file_name = str(uuid.uuid4())
    file = create_text_file(tmp_path, text_file_name)
    mlflow.log_artifact(file)
    assert len(client.list_artifacts(run.info.run_id)) == 1
    assert client.list_artifacts(run.info.run_id)[0].path == text_file_name


def create_text_file(tmp_path: pathlib.Path, text_file_name: str):
    file = tmp_path / text_file_name
    file.write_text("hello world!")
    return file


def test_log_multiple_artifacts(client: MlflowClient, run: Run, tmp_path: pathlib.Path) -> None:
    text_file1_name = str(uuid.uuid4())
    text_file2_name = str(uuid.uuid4())
    file1 = create_text_file(tmp_path, text_file1_name)
    file2 = create_text_file(tmp_path, text_file2_name)
    mlflow.log_artifact(file1)
    mlflow.log_artifact(file2)
    assert len(client.list_artifacts(run.info.run_id)) == 2
    for artifact in client.list_artifacts(run.info.run_id):
        assert artifact.path == text_file1_name or artifact.path == text_file2_name


def test_log_no_artifacts_inside_directory(client: MlflowClient, run: Run, tmp_path: pathlib.Path) -> None:
    new_dir = tmp_path / "new_dir"
    new_dir.mkdir()
    mlflow.log_artifacts(new_dir)
    assert len(client.list_artifacts(run.info.run_id)) == 0


def test_log_one_artifact_inside_directory(client: MlflowClient, run: Run, tmp_path: pathlib.Path) -> None:
    new_dir = tmp_path / "new_dir"
    new_dir.mkdir()
    text_file_name = str(uuid.uuid4())
    create_text_file(new_dir, text_file_name)
    mlflow.log_artifacts(new_dir)
    assert len(client.list_artifacts(run.info.run_id)) == 1
    assert client.list_artifacts(run.info.run_id)[0].path == text_file_name


def test_log_multiple_artifacts_inside_directory(client: MlflowClient, run: Run, tmp_path: pathlib.Path) -> None:
    new_dir = tmp_path / "new_dir"
    new_dir.mkdir()
    text_file1_name = str(uuid.uuid4())
    text_file2_name = str(uuid.uuid4())
    create_text_file(new_dir, text_file1_name)
    create_text_file(new_dir, text_file2_name)
    mlflow.log_artifacts(new_dir)
    assert len(client.list_artifacts(run.info.run_id)) == 2
    for artifact in client.list_artifacts(run.info.run_id):
        assert artifact.path == text_file1_name or artifact.path == text_file2_name


def test_log_one_artifact_inside_nested_directory(client: MlflowClient, run: Run, tmp_path: pathlib.Path) -> None:
    new_dir = tmp_path / "new_dir"
    new_dir.mkdir()
    new_dir2 = new_dir / "new_dir2"
    new_dir2.mkdir()
    text_file_name = str(uuid.uuid4())
    create_text_file(new_dir2, text_file_name)
    mlflow.log_artifacts(new_dir)
    assert len(client.list_artifacts(run.info.run_id)) == 1
    assert client.list_artifacts(run.info.run_id)[0].path == "new_dir2"
    assert client.list_artifacts(run.info.run_id)[0].is_dir == True


def test_log_multiple_artifacts_inside_nested_directory(client: MlflowClient, run: Run, tmp_path: pathlib.Path) -> None:
    new_dir = tmp_path / "new_dir"
    new_dir.mkdir()
    new_dir2 = new_dir / "new_dir2"
    new_dir2.mkdir()
    text_file1_name = str(uuid.uuid4())
    text_file2_name = str(uuid.uuid4())
    create_text_file(new_dir2, text_file1_name)
    create_text_file(new_dir2, text_file2_name)
    mlflow.log_artifacts(new_dir)
    assert len(client.list_artifacts(run.info.run_id)) == 1
    assert client.list_artifacts(run.info.run_id)[0].path == "new_dir2"
    assert client.list_artifacts(run.info.run_id)[0].is_dir == True


def test_log_artifact_with_artifact_path(client: MlflowClient, run: Run, tmp_path: pathlib.Path) -> None:
    text_file_name = str(uuid.uuid4())
    file = create_text_file(tmp_path, text_file_name)
    mlflow.log_artifact(file, "artifact_path")
    assert len(client.list_artifacts(run.info.run_id)) == 1
    assert client.list_artifacts(run.info.run_id)[0].path == "artifact_path"
    assert client.list_artifacts(run.info.run_id)[0].is_dir == True


def test_log_artifact_with_nested_artifact_path(client: MlflowClient, run: Run, tmp_path: pathlib.Path) -> None:
    text_file_name = str(uuid.uuid4())
    file = create_text_file(tmp_path, text_file_name)
    mlflow.log_artifact(file, "artifact_path/nested_artifact_path")
    assert len(client.list_artifacts(run.info.run_id)) == 1
    assert client.list_artifacts(run.info.run_id)[
        0].path == "artifact_path"
    assert client.list_artifacts(run.info.run_id)[0].is_dir == True


def test_download_one_artifact(client: MlflowClient, run: Run, tmp_path: pathlib.Path) -> None:
    text_file_name = str(uuid.uuid4())
    file = create_text_file(tmp_path, text_file_name)
    mlflow.log_artifact(file)
    artifact_path = client.list_artifacts(run.info.run_id)[0].path
    download_dir = tmp_path / "download_dir"
    download_dir.mkdir()
    client.download_artifacts(run.info.run_id, artifact_path, download_dir)
    downloaded_file = download_dir / artifact_path
    assert downloaded_file.exists()


def test_download_multiple_artifacts(client: MlflowClient, run: Run, tmp_path: pathlib.Path) -> None:
    text_file1_name = str(uuid.uuid4())
    text_file2_name = str(uuid.uuid4())
    file1 = create_text_file(tmp_path, text_file1_name)
    file2 = create_text_file(tmp_path, text_file2_name)
    mlflow.log_artifact(file1, "features")
    mlflow.log_artifact(file2, "features")
    download_dir = tmp_path / "download_dir"
    download_dir.mkdir()
    client.download_artifacts(run.info.run_id, "features", download_dir)
    assert (download_dir / "features" / text_file1_name).exists()
    assert (download_dir / "features" / text_file2_name).exists()


def test_download_directory(client: MlflowClient, run: Run, tmp_path: pathlib.Path) -> None:
    artifact_dir = tmp_path / "artifact_dir"
    artifact_dir.mkdir()
    text_file_name = str(uuid.uuid4())
    create_text_file(artifact_dir, text_file_name)
    mlflow.log_artifacts(artifact_dir)
    download_dir = tmp_path / "download_dir"
    download_dir.mkdir()
    client.download_artifacts(run.info.run_id, "", download_dir)
    assert (download_dir / text_file_name).exists()


def test_download_nested_directory(client: MlflowClient, run: Run, tmp_path: pathlib.Path) -> None:
    artifact_dir = tmp_path / "artifact_dir"
    artifact_dir.mkdir()
    artifact_dir2 = artifact_dir / "artifact_dir2"
    artifact_dir2.mkdir()
    text_file_name = str(uuid.uuid4())
    create_text_file(artifact_dir2, text_file_name)
    mlflow.log_artifacts(artifact_dir)
    download_dir = tmp_path / "download_dir"
    download_dir.mkdir()
    client.download_artifacts(run.info.run_id, "artifact_dir2", download_dir)
    assert (download_dir / "artifact_dir2" / text_file_name).exists()


def test_download_multiple_artifacts_in_nested_directory(client: MlflowClient, run: Run, tmp_path: pathlib.Path) -> None:
    artifact_dir = tmp_path / "artifact_dir"
    artifact_dir.mkdir()
    artifact_dir2 = artifact_dir / "artifact_dir2"
    artifact_dir2.mkdir()
    text_file1_name = str(uuid.uuid4())
    text_file2_name = str(uuid.uuid4())
    create_text_file(artifact_dir2, text_file1_name)
    create_text_file(artifact_dir2, text_file2_name)
    mlflow.log_artifacts(artifact_dir)
    download_dir = tmp_path / "download_dir"
    download_dir.mkdir()
    client.download_artifacts(run.info.run_id, "artifact_dir2", download_dir)
    assert (download_dir / "artifact_dir2" / text_file1_name).exists()
    assert (download_dir / "artifact_dir2" / text_file2_name).exists()


def test_download_artifact_with_no_dst_dir(client: MlflowClient, run: Run, tmp_path: pathlib.Path) -> None:
    text_file_name = str(uuid.uuid4())
    file = create_text_file(tmp_path, text_file_name)
    mlflow.log_artifact(file)
    artifact_path = client.list_artifacts(run.info.run_id)[0].path
    client.download_artifacts(run.info.run_id, artifact_path, None)
    downloaded_file = tmp_path / artifact_path
    assert downloaded_file.exists()


def test_list_artifacts_on_file_returns_empty_list(client: MlflowClient, run: Run, tmp_path: pathlib.Path) -> None:
    text_file_name = str(uuid.uuid4())
    file = create_text_file(tmp_path, text_file_name)
    mlflow.log_artifact(file)
    assert len(client.list_artifacts(run.info.run_id, text_file_name)) == 0

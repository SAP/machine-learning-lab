import pytest
import mlflow
import os
import shutil
import uuid
from helpers import get_safe_port, launch_tracking_store_test_server
from mlflow.tracking import MlflowClient
from mlflow.entities import Run
from mlflow.exceptions import MlflowException
from mlflow.entities.lifecycle_stage import LifecycleStage


@pytest.fixture(scope="module", autouse=True)
def artifacts_server():
    """
    Starts and stops mlflow server and sets the tracking_uri.
    """
    api_key = "4239a609f81848440c1a4479492cc8fb5a320ccc"
    project_id = "test-project-id"
    port = get_safe_port()
    os.environ["LAB_API_TOKEN"] = api_key
    store_uri = "ml-lab://localhost:30010/{}".format(
        project_id)
    # store_uri = "./mlruns"
    process = launch_tracking_store_test_server(store_uri, port)
    mlflow.set_tracking_uri("http://localhost:{}".format(port))
    yield
    process.kill()
    if os.path.isdir("mlruns"):
        shutil.rmtree("mlruns")


@pytest.fixture(scope="function")
def run(artifacts_server) -> Run:
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


def test_zero_metrics(client: MlflowClient, run: Run) -> None:
    assert len(client.get_run(run.info.run_id).data.metrics) == 0


def test_zero_params(client: MlflowClient, run: Run) -> None:
    assert len(client.get_run(run.info.run_id).data.params) == 0


def test_default_tags(client: MlflowClient, run: Run) -> None:
    tags = client.get_run(run.info.run_id).data.tags
    # might have mlflow.source.git.commit
    assert len(tags) == 3 or len(tags) == 4
    assert tags["mlflow.user"]
    assert tags["mlflow.source.name"]
    assert tags["mlflow.source.type"]


def test_log_one_metric(client: MlflowClient, run: Run) -> None:
    mlflow.log_metric("metric", 5)
    assert len(client.get_run(run.info.run_id).data.metrics) == 1
    assert client.get_run(run.info.run_id).data.metrics["metric"] == 5


def test_log_one_param(client: MlflowClient, run: Run) -> None:
    mlflow.log_param("param", "value")
    assert len(client.get_run(run.info.run_id).data.params) == 1
    assert client.get_run(run.info.run_id).data.params["param"] == "value"


def test_log_one_tag(client: MlflowClient, run: Run) -> None:
    previous_tags = client.get_run(run.info.run_id).data.tags
    mlflow.set_tag("tag", "value")
    new_tags = client.get_run(run.info.run_id).data.tags
    assert len(new_tags) == len(previous_tags) + 1
    assert new_tags["tag"] == "value"


def test_log_multiple_metrics(client: MlflowClient, run: Run) -> None:
    mlflow.log_metric("metric1", 5)
    mlflow.log_metric("metric2", 10)
    assert len(client.get_run(run.info.run_id).data.metrics) == 2
    assert client.get_run(run.info.run_id).data.metrics["metric1"] == 5
    assert client.get_run(run.info.run_id).data.metrics["metric2"] == 10


def test_log_multiple_params(client: MlflowClient, run: Run) -> None:
    mlflow.log_param("param1", "value1")
    mlflow.log_param("param2", "value2")
    assert len(client.get_run(run.info.run_id).data.params) == 2
    assert client.get_run(run.info.run_id).data.params["param1"] == "value1"
    assert client.get_run(run.info.run_id).data.params["param2"] == "value2"


def test_log_multiple_tags(client: MlflowClient, run: Run) -> None:
    previous_tags = client.get_run(run.info.run_id).data.tags
    mlflow.set_tag("tag1", "value1")
    mlflow.set_tag("tag2", "value2")
    new_tags = client.get_run(run.info.run_id).data.tags
    assert len(new_tags) == len(previous_tags) + 2
    assert new_tags["tag1"] == "value1"
    assert new_tags["tag2"] == "value2"


def test_log_multiple_tags_with_same_key(client: MlflowClient, run: Run) -> None:
    previous_tags = client.get_run(run.info.run_id).data.tags
    mlflow.set_tag("tag", "value1")
    mlflow.set_tag("tag", "value2")
    new_tags = client.get_run(run.info.run_id).data.tags
    assert len(new_tags) == len(previous_tags) + 1
    assert new_tags["tag"] == "value2"


def test_create_one_experiment(client: MlflowClient) -> None:
    old_experiments = client.list_experiments()
    experiment_name = str(uuid.uuid4())
    experiment_id = client.create_experiment(experiment_name, "./mlruns")
    new_experiments = client.list_experiments()
    assert len(new_experiments) == len(old_experiments) + 1
    assert experiment_id in [
        e.experiment_id for e in new_experiments]


def test_create_experiments_with_existing_name(client: MlflowClient) -> None:
    old_experiments = client.list_experiments()
    experiment_name = str(uuid.uuid4())
    client.create_experiment(experiment_name, "./mlruns")
    with pytest.raises(MlflowException):
        client.create_experiment(experiment_name, "./mlruns")
    new_experiments = client.list_experiments()
    assert len(new_experiments) == len(old_experiments) + 1


def test_create_multiple_experiments(client: MlflowClient) -> None:
    old_experiments = client.list_experiments()
    experiment_name_1 = str(uuid.uuid4())
    experiment_name_2 = str(uuid.uuid4())
    experiment_id_1 = client.create_experiment(experiment_name_1, "./mlruns")
    experiment_id_2 = client.create_experiment(experiment_name_2, "./mlruns")
    new_experiments = client.list_experiments()
    assert len(new_experiments) == len(old_experiments) + 2
    assert experiment_id_1 in [e.experiment_id for e in new_experiments]
    assert experiment_id_2 in [e.experiment_id for e in new_experiments]


def test_create_experiment_with_tags(client: MlflowClient) -> None:
    experiment_name = str(uuid.uuid4())
    experiment_id = client.create_experiment(
        experiment_name, "./mlruns", tags={"tag1": "value1", "tag2": "value2"})
    experiment = client.get_experiment(experiment_id)
    assert experiment.tags["tag1"] == "value1"
    assert experiment.tags["tag2"] == "value2"


def test_delete_experiment(client: MlflowClient) -> None:
    experiment_name = str(uuid.uuid4())
    experiment_id = client.create_experiment(experiment_name, "./mlruns")
    client.delete_experiment(experiment_id)
    assert experiment_id not in [
        e.experiment_id for e in client.list_experiments()]


def test_restore_experiment(client: MlflowClient) -> None:
    experiment_name = str(uuid.uuid4())
    experiment_id = client.create_experiment(experiment_name, "./mlruns")
    client.delete_experiment(experiment_id)
    client.restore_experiment(experiment_id)
    assert experiment_id in [
        e.experiment_id for e in client.list_experiments()]


def test_rename_experiment(client: MlflowClient) -> None:
    experiment_name = str(uuid.uuid4())
    experiment_id = client.create_experiment(experiment_name, "./mlruns")
    new_experiment_name = str(uuid.uuid4())
    client.rename_experiment(experiment_id, new_experiment_name)
    experiment = client.get_experiment(experiment_id)
    assert experiment.name == new_experiment_name


def test_get_run(client: MlflowClient, run: Run) -> None:
    run_id = run.info.run_id
    run = client.get_run(run_id)
    assert run.info.run_id == run_id
    assert len(run.data.params) == 0
    assert len(run.data.metrics) == 0
    assert run.info.lifecycle_stage == LifecycleStage.ACTIVE


def test_single_metric_history(client: MlflowClient, run: Run) -> None:
    run_id = run.info.run_id
    client.log_metric(run_id, "metric1", 1)
    metric_history = client.get_metric_history(run_id, "metric1")
    assert len(metric_history) == 1
    assert metric_history[0].step == 0
    assert metric_history[0].value == 1


def test_multiple_metric_history_with_same_key(client: MlflowClient, run: Run) -> None:
    run_id = run.info.run_id
    client.log_metric(run_id, "metric1", 1)
    client.log_metric(run_id, "metric1", 2)
    client.log_metric(run_id, "metric1", 3)
    metric_history = client.get_metric_history(run_id, "metric1")
    assert len(metric_history) == 3
    assert metric_history[0].step == 0
    assert metric_history[0].value == 1
    assert metric_history[1].step == 0
    assert metric_history[1].value == 2
    assert metric_history[2].step == 0
    assert metric_history[2].value == 3


def test_multiple_metric_history_with_different_keys(client: MlflowClient, run: Run) -> None:
    run_id = run.info.run_id
    client.log_metric(run_id, "metric1", 1)
    client.log_metric(run_id, "metric2", 2)
    metric_history = client.get_metric_history(run_id, "metric1")
    assert len(metric_history) == 1
    assert metric_history[0].step == 0
    assert metric_history[0].value == 1
    metric_history = client.get_metric_history(run_id, "metric2")
    assert len(metric_history) == 1
    assert metric_history[0].step == 0
    assert metric_history[0].value == 2


def test_multiple_metric_history(client: MlflowClient, run: Run) -> None:
    run_id = run.info.run_id
    client.log_metric(run_id, "metric1", 1)
    client.log_metric(run_id, "metric2", 2)
    client.log_metric(run_id, "metric1", 3)
    metric_history = client.get_metric_history(run_id, "metric1")
    assert len(metric_history) == 2
    assert metric_history[0].step == 0
    assert metric_history[0].value == 1
    assert metric_history[1].step == 0
    assert metric_history[1].value == 3
    metric_history = client.get_metric_history(run_id, "metric2")
    assert len(metric_history) == 1
    assert metric_history[0].step == 0
    assert metric_history[0].value == 2

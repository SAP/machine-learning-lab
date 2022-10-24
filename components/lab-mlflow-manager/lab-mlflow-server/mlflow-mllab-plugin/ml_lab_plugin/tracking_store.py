import json
import logging
import uuid
from typing import Optional
from urllib import parse
import os

import mlflow.protos.databricks_pb2 as databricks_pb2
from contaxy.clients import JsonDocumentClient
from contaxy.clients.shared import BaseUrlSession
from contaxy.schema.exceptions import ResourceNotFoundError
from mlflow.entities import (Experiment, ExperimentTag, Metric, Param, Run,
                             RunData, RunInfo, RunStatus, RunTag, ViewType)
from mlflow.entities.lifecycle_stage import LifecycleStage
from mlflow.entities.run_info import check_run_is_active
from mlflow.exceptions import MissingConfigException, MlflowException
from mlflow.protos.databricks_pb2 import (INTERNAL_ERROR)
from mlflow.store.entities.paged_list import PagedList
from mlflow.store.tracking import SEARCH_MAX_RESULTS_THRESHOLD
from mlflow.store.tracking.abstract_store import AbstractStore
from mlflow.utils.search_utils import SearchUtils
from mlflow.utils.uri import append_to_uri_path
from mlflow.utils.validation import (_validate_batch_log_data,
                                     _validate_batch_log_limits,
                                     _validate_experiment_id,
                                     _validate_experiment_name,
                                     _validate_list_experiments_max_results,
                                     _validate_metric_name,
                                     _validate_param_keys_unique,
                                     _validate_run_id,
                                     _validate_tag_name)


class MlLabTrackingStore(AbstractStore):
    DEFAULT_EXPERIMENT_ID = "0"

    def __init__(self, store_uri: str = None, artifact_uri: str = None) -> None:
        # TODO: find a solution for not hardcoding the url and token
        print("===== __init__ =====")
        print(f"store_uri: {store_uri}")
        print(f"artifact_uri: {artifact_uri}")
        print("=============================")
        self.store_uri = store_uri
        self.artifact_root_uri = artifact_uri
        parse_result = parse.urlparse(self.store_uri)
        url = "http://{}/api".format(parse_result.netloc)
        if "CONTAXY_API_ENDPOINT" in os.environ:
            url = os.environ["CONTAXY_API_ENDPOINT"]
        session = BaseUrlSession(base_url=url)
        self.project_id = parse_result.path.split("/")[1]
        token = os.environ["LAB_API_TOKEN"]
        session.headers = {"Authorization": f"Bearer {token}"}
        session.verify = False  # Workaround for development if SAP certificate is not installed
        json_client = JsonDocumentClient(session)
        self.json_client = json_client

    def list_experiments(self, view_type: ViewType = ViewType.ACTIVE_ONLY, max_results: int = None, page_token: bytes = None) -> PagedList:
        print("===== list_experiments =====")
        print(f"view_type: {view_type}")
        print(f"max_results: {max_results}")
        print(f"page_token: {page_token}")
        print("=============================")
        _validate_list_experiments_max_results(max_results)
        rsl = []
        if view_type == ViewType.ACTIVE_ONLY or view_type == ViewType.ALL:
            rsl += self._get_active_experiments()
        if view_type == ViewType.DELETED_ONLY or view_type == ViewType.ALL:
            rsl += self._get_deleted_experiments()

        experiments = []
        for exp_id in rsl:
            try:
                # trap and warn known issues, will raise unexpected exceptions to caller
                experiment = self._get_experiment(exp_id, view_type)
                if experiment:
                    experiments.append(experiment)
            except MissingConfigException as rnfe:
                # Trap malformed experiments and log warnings.
                logging.warning(
                    "Malformed experiment '%s'. Detailed error %s",
                    str(exp_id),
                    str(rnfe),
                    exc_info=True,
                )
        if max_results is not None:
            experiments, next_page_token = SearchUtils.paginate(
                experiments, page_token, max_results
            )
            return PagedList(experiments, next_page_token)
        else:
            return PagedList(experiments, None)

    def _get_active_experiments(self) -> list[str]:
        print("===== _get_active_experiments =====")
        print("=============================")
        json_docs = self.json_client.list_json_documents(
            self.project_id, "experiments")

        experiments = []
        for json_doc in json_docs:
            json_value = json.loads(json_doc.json_value)
            if json_value["lifecycle_stage"] == LifecycleStage.ACTIVE:
                exp_id = json_doc.key
                experiments.append(exp_id)
        return experiments

    def _get_deleted_experiments(self) -> list[str]:
        print("===== _get_deleted_experiments =====")
        print("=============================")
        json_docs = self.json_client.list_json_documents(
            self.project_id, "experiments")

        experiments = []
        for json_doc in json_docs:
            json_value = json.loads(json_doc.json_value)
            if json_value["lifecycle_stage"] == LifecycleStage.DELETED:
                exp_id = json_doc.key
                experiments.append(exp_id)
        return experiments

    def create_experiment(self, name: str, artifact_location: str = None, tags: Optional[list[ExperimentTag]] = None) -> str:
        print("===== create_experiment =====")
        print(f"name: {name}")
        print(f"artifact_location: {artifact_location}")
        print(f"tags: {tags}")
        print("=============================")
        _validate_experiment_name(name)
        self._validate_experiment_does_not_exist(name)
        # Get all existing experiments and find the one with largest numerical ID.
        # len(list_all(..)) would not work when experiments are deleted.
        experiments_ids = [
            int(e.experiment_id)
            for e in self.list_experiments(ViewType.ALL)
            if e.experiment_id.isdigit()
        ]
        experiment_id = max(experiments_ids) + 1 if experiments_ids else 0
        return self._create_experiment_with_id(name, str(experiment_id), artifact_location, tags)

    def _create_experiment_with_id(self, name: str, experiment_id: str, artifact_uri: str, tags: list[ExperimentTag]) -> str:
        artifact_uri = artifact_uri or append_to_uri_path(
            self.artifact_root_uri, str(experiment_id)
        )
        print("===== _create_experiment_with_id =====")
        print(f"name: {name}")
        print(f"experiment_id: {experiment_id}")
        print(f"artifact_uri: {artifact_uri}")
        print(f"tags: {tags}")
        print("=============================")
        experiment_dict = {
            "experiment_id": experiment_id,
            "name": name,
            "artifact_location": artifact_uri,
            "lifecycle_stage": LifecycleStage.ACTIVE,
            "tags": {tag.key: tag.value for tag in tags}
        }
        self.json_client.create_json_document(
            self.project_id, "experiments", experiment_id, json.dumps(experiment_dict))
        return experiment_id

    def set_experiment_tag(self, experiment_id: str, tag: ExperimentTag) -> None:
        print("===== set_experiment_tag =====")
        print(f"experiment_id: {experiment_id}")
        print(f"tag: {tag}")
        print("=============================")
        _validate_tag_name(tag.key)
        experiment = self.get_experiment(experiment_id)
        if experiment.lifecycle_stage != LifecycleStage.ACTIVE:
            raise MlflowException(
                "The experiment {} must be in the 'active'"
                "lifecycle_stage to set tags".format(experiment.experiment_id),
                error_code=databricks_pb2.INVALID_PARAMETER_VALUE,
            )
        tags: list[dict[str, str]] = experiment.tags
        tag_dict = {tag.key: tag.value}
        tags.append(tag_dict)

        self.json_client.update_json_document(
            self.project_id, "experiments", experiment_id, json.dumps({"tags": tags}))

    def _validate_experiment_does_not_exist(self, name: str) -> None:
        print("===== _validate_experiment_does_not_exist =====")
        print(f"name: {name}")
        print("=============================")
        experiment = self.get_experiment_by_name(name)
        if experiment is not None:
            if experiment.lifecycle_stage == LifecycleStage.DELETED:
                raise MlflowException(
                    "Experiment '%s' already exists in deleted state. "
                    "You can restore the experiment, or permanently delete the experiment "
                    "from the .trash folder (under tracking server's root folder) in order to "
                    "use this experiment name again." % experiment.name,
                    databricks_pb2.RESOURCE_ALREADY_EXISTS,
                )
            else:
                raise MlflowException(
                    "Experiment '%s' already exists." % experiment.name,
                    databricks_pb2.RESOURCE_ALREADY_EXISTS,
                )

    def get_experiment(self, experiment_id: str) -> Experiment:
        """
        Fetch the experiment.
        Note: This API will search for active as well as deleted experiments.

        :param experiment_id: Integer id for the experiment
        :return: A single Experiment object if it exists, otherwise raises an Exception.
        """
        print("===== get_experiment =====")
        print(f"experiment_id: {experiment_id}")
        print("=============================")

        experiment_id = MlLabTrackingStore.DEFAULT_EXPERIMENT_ID if experiment_id is None else experiment_id
        experiment = self._get_experiment(experiment_id)
        if experiment is None:
            raise MlflowException(
                "Experiment '%s' does not exist." % experiment_id,
                databricks_pb2.RESOURCE_DOES_NOT_EXIST,
            )
        return experiment

    def _get_experiment(self, experiment_id: str, view_type: ViewType = ViewType.ALL) -> Optional[Experiment]:
        print("===== _get_experiment =====")
        print(f"experiment_id: {experiment_id}")
        print(f"view_type: {view_type}")
        print("=============================")
        _validate_experiment_id(experiment_id)
        meta = self._get_experiment_metadata(experiment_id)
        meta["tags"] = [ExperimentTag(key, value)
                        for key, value in meta["tags"].items()]
        experiment = _read_persisted_experiment_dict(meta)
        if experiment_id != experiment.experiment_id:
            logging.warning(
                "Experiment ID mismatch for exp %s. ID recorded as '%s' in meta data. "
                "Experiment will be ignored.",
                experiment_id,
                experiment.experiment_id,
                exc_info=True,
            )
            return None
        return experiment

    def _get_experiment_metadata(self, experiment_id: str) -> dict:
        print("===== _get_experiment_metadata =====")
        print(f"experiment_id: {experiment_id}")
        print("=============================")

        response = self.json_client.get_json_document(
            project_id=self.project_id, collection_id="experiments", key=experiment_id)
        return json.loads(response.json_value)

    def _get_experiment_tags(self, experiment_id: str) -> list[dict[str, str]]:
        print("===== _get_experiment_tags =====")
        print(f"experiment_id: {experiment_id}")
        print("=============================")
        try:
            response = self.json_client.get_json_document(
                project_id=self.project_id, collection_id="experiments_tags", key=experiment_id)
        except ResourceNotFoundError:
            return []

        tags = response.json_value
        if type(tags) is dict:
            tags = [tags]

        return tags

    def delete_experiment(self, experiment_id: str) -> None:
        print("===== delete_experiment =====")
        print(f"experiment_id: {experiment_id}")
        print("=============================")
        json_document = self.json_client.get_json_document(
            self.project_id, "experiments", experiment_id)
        actual_json = json.loads(json_document.json_value)
        if actual_json["lifecycle_stage"] == LifecycleStage.DELETED:
            raise MlflowException(
                "Experiment '%s' already deleted." % experiment_id,
                databricks_pb2.RESOURCE_DOES_NOT_EXIST,
            )
        actual_json["lifecycle_stage"] = LifecycleStage.DELETED
        self.json_client.update_json_document(
            self.project_id, "experiments", experiment_id, json.dumps(actual_json))

    def restore_experiment(self, experiment_id: str) -> None:
        print("===== restore_experiment =====")
        print(f"experiment_id: {experiment_id}")
        print("=============================")
        json_document = self.json_client.get_json_document(
            self.project_id, "experiments", experiment_id)
        actual_json = json.loads(json_document.json_value)
        if actual_json["lifecycle_stage"] == LifecycleStage.ACTIVE:
            raise MlflowException(
                "Experiment '%s' already active." % experiment_id,
                databricks_pb2.RESOURCE_DOES_NOT_EXIST,
            )
        actual_json["lifecycle_stage"] = LifecycleStage.ACTIVE
        self.json_client.update_json_document(
            self.project_id, "experiments", experiment_id, json.dumps(actual_json))

    def rename_experiment(self, experiment_id: str, new_name: str) -> None:
        print("===== rename_experiment =====")
        print(f"experiment_id: {experiment_id}")
        print(f"new_name: {new_name}")
        print("=============================")
        json_document = self.json_client.get_json_document(
            self.project_id, "experiments", experiment_id)
        actual_json = json.loads(json_document.json_value)
        if actual_json["lifecycle_stage"] == LifecycleStage.DELETED:
            raise MlflowException(
                "Cannot rename experiment in non-active lifecycle stage."
                " Current stage: %s" % actual_json["lifecycle_stage"]
            )
        actual_json["name"] = new_name
        self.json_client.update_json_document(
            self.project_id, "experiments", experiment_id, json.dumps(actual_json))

    def get_run(self, run_id: str) -> Run:
        """
        Note: Will get both active and deleted runs.
        """
        print("===== get_run =====")
        print(f"run_id: {run_id}")
        print("=============================")

        _validate_run_id(run_id)
        run_info = self._get_run_info(run_id)
        if run_info is None:
            raise MlflowException(
                "Run '%s' metadata is in invalid state." % run_id, databricks_pb2.INVALID_STATE
            )
        (run_info)
        return self._get_run_from_info(run_info)

    def _get_run_from_info(self, run_info: RunInfo) -> Run:
        print("===== _get_run_from_info =====")
        print(f"run_info: {run_info}")
        print("=============================")

        metrics = self._get_all_metrics(run_info)
        params = self._get_all_params(run_info)
        tags = self._get_all_tags(run_info)
        return Run(run_info, RunData(metrics, params, tags))

    def _get_all_metrics(self, run_info: RunInfo) -> list[Metric]:
        print("===== _get_all_metrics =====")
        print(f"run_info: {run_info}")
        print("=============================")

        try:
            response = self.json_client.get_json_document(
                self.project_id, "runs", run_info.run_id)
        except ResourceNotFoundError:
            return []

        response_json = json.loads(response.json_value)
        metrics_dict: dict = response_json["metrics"]
        latest_metrics: list[Metric] = []
        for metric_key, infos in metrics_dict.items():
            if len(infos) > 0:
                latest_metric = infos[-1]
            latest_metrics.append(
                Metric(metric_key, latest_metric["value"], latest_metric["timestamp"], latest_metric["step"]))
        return latest_metrics

    def _get_all_params(self, run_info: RunInfo) -> list[Param]:
        print("===== _get_all_params =====")
        print(f"run_info: {run_info}")
        print("=============================")

        try:
            response = self.json_client.get_json_document(
                self.project_id, "runs", run_info.run_id)
        except ResourceNotFoundError:
            return []

        response_json = json.loads(response.json_value)
        params_list: dict = response_json["params"]
        params: list[Param] = []
        for key, value in params_list.items():
            params.append(Param(key, value))
        return params

    def _get_all_tags(self, run_info: RunInfo) -> list[RunTag]:
        print("===== _get_all_tags =====")
        print(f"run_info: {run_info}")
        print("=============================")

        try:
            response = self.json_client.get_json_document(
                self.project_id, "runs", run_info.run_id)
        except ResourceNotFoundError:
            return []

        response_json = json.loads(response.json_value)
        tags_list: dict = response_json["tags"]
        tags: list[RunTag] = []
        for key, value in tags_list.items():
            tags.append(RunTag(key, value))
        return tags

    def _get_run_info(self, run_uuid: str) -> RunInfo:
        """
        Note: Will get both active and deleted runs.
        """
        print("===== _get_run_info =====")
        print(f"run_uuid: {run_uuid}")
        print("=============================")
        response = self.json_client.get_json_document(
            project_id=self.project_id, collection_id="runs", key=run_uuid)
        return RunInfo.from_dictionary(json.loads(response.json_value))

    def update_run_info(self, run_id: str, run_status: RunStatus, end_time: str) -> RunInfo:
        print("===== update_run_info =====")
        print(f"run_id: {run_id}")
        print(f"run_status: {run_status}")
        print(f"end_time: {end_time}")
        print("=============================")
        _validate_run_id(run_id)
        json_document = self.json_client.get_json_document(
            self.project_id, "runs", run_id)
        actual_json = json.loads(json_document.json_value)
        actual_json["status"] = RunStatus.to_string(run_status)
        actual_json["end_time"] = end_time
        response = self.json_client.update_json_document(
            self.project_id, "runs", run_id, json.dumps(actual_json))
        response_json = json.loads(response.json_value)
        return RunInfo.from_dictionary(response_json)

    def create_run(self, experiment_id: str, user_id: str, start_time: str, tags: list[RunTag] = None) -> Run:
        print("===== create_run =====")
        print(f"experiment_id: {experiment_id}")
        print(f"user_id: {user_id}")
        print(f"start_time: {start_time}")
        print(f"tags: {tags}")
        print("=============================")
        experiment_id = MlLabTrackingStore.DEFAULT_EXPERIMENT_ID if experiment_id is None else experiment_id
        run_uuid = uuid.uuid4().hex
        tags_dict = dict()
        for tag in tags:
            tags_dict[tag.key] = tag.value

        data = {
            "run_uuid": run_uuid,
            "experiment_id": experiment_id,
            "user_id": user_id,
            "status": RunStatus.to_string(RunStatus.RUNNING),
            "start_time": start_time,
            "end_time": None,
            "lifecycle_stage": LifecycleStage.ACTIVE,
            "metrics": {},
            "params": {},
            "tags": tags_dict,
            "artifact_uri": os.path.join(self.artifact_root_uri, experiment_id, run_uuid, "artifacts")
        }
        json_data = json.dumps(data)
        self.json_client.create_json_document(
            project_id=self.project_id, collection_id="runs", key=run_uuid, json_document=json_data)

        return self.get_run(run_uuid)

    def delete_run(self, run_id: str) -> None:
        print("===== delete_run =====")
        print(f"run_id: {run_id}")
        print("=============================")

        json_document = self.json_client.get_json_document(
            self.project_id, "runs", run_id)
        actual_json = json.loads(json_document.json_value)
        if actual_json["lifecycle_stage"] == LifecycleStage.DELETED:
            raise MlflowException(
                "Run '%s' already deleted." % run_id,
                databricks_pb2.RESOURCE_DOES_NOT_EXIST,
            )
        actual_json["lifecycle_stage"] = LifecycleStage.DELETED
        self.json_client.update_json_document(
            self.project_id, "runs", run_id, json.dumps(actual_json))

    def restore_run(self, run_id: str) -> None:
        print("===== restore_run =====")
        print(f"run_id: {run_id}")
        print("=============================")

        json_document = self.json_client.get_json_document(
            self.project_id, "runs", run_id)
        actual_json = json.loads(json_document.json_value)
        if actual_json["lifecycle_stage"] == LifecycleStage.ACTIVE:
            raise MlflowException(
                "Run '%s' already active." % run_id,
                databricks_pb2.RESOURCE_DOES_NOT_EXIST,
            )
        actual_json["lifecycle_stage"] = LifecycleStage.ACTIVE
        self.json_client.update_json_document(
            self.project_id, "runs", run_id, json.dumps(actual_json))

    def get_metric_history(self, run_id: str, metric_key: str) -> list[Metric]:
        print("===== get_metric_history =====")
        print(f"run_id: {run_id}")
        print(f"metric_key: {metric_key}")
        print("=============================")

        _validate_run_id(run_id)
        _validate_metric_name(metric_key)
        run_info = self._get_run_info(run_id)
        return self._get_metric_history(run_info, metric_key)

    def _get_metric_history(self, run_info: RunInfo, metric_key: str) -> list[Metric]:
        print("===== _get_metric_history =====")
        print(f"run_info: {run_info}")
        print(f"metric_key: {metric_key}")
        print("=============================")
        response = self.json_client.get_json_document(
            self.project_id, "runs", run_info.run_uuid)
        metrics_dict = json.loads(response.json_value)["metrics"]
        metrics_list = metrics_dict.get(metric_key, [])
        metrics_history = []
        for metric_info in metrics_list:
            metrics_history.append(
                Metric(metric_key, metric_info["value"], metric_info["timestamp"], metric_info["step"]))
        return metrics_history

    def _search_runs(self, experiment_ids: list[str], filter_string: str, run_view_type: ViewType, max_results: int, order_by: list, page_token: Optional[bytes]) -> tuple[list[Run], Optional[bytes]]:
        print("===== _search_runs =====")
        print(f"experiment_ids: {experiment_ids}")
        print(f"filter_string: {filter_string}")
        print(f"run_view_type: {run_view_type}")
        print(f"max_results: {max_results}")
        print(f"order_by: {order_by}")
        print(f"page_token: {page_token}")
        print("=============================")
        if max_results > SEARCH_MAX_RESULTS_THRESHOLD:
            raise MlflowException(
                "Invalid value for request parameter max_results. It must be at "
                "most {}, but got value {}".format(
                    SEARCH_MAX_RESULTS_THRESHOLD, max_results),
                databricks_pb2.INVALID_PARAMETER_VALUE,
            )
        runs = []
        for experiment_id in experiment_ids:
            run_infos = self._list_run_infos(experiment_id, run_view_type)
            runs.extend(self._get_run_from_info(r) for r in run_infos)
        filtered = SearchUtils.filter(runs, filter_string)
        sorted_runs = SearchUtils.sort(filtered, order_by)
        runs, next_page_token = SearchUtils.paginate(
            sorted_runs, page_token, max_results)
        return runs, next_page_token

    def _list_run_infos(self, experiment_id: str, view_type: ViewType) -> list[RunInfo]:
        print("===== _list_run_infos =====")
        print(f"experiment_id: {experiment_id}")
        print(f"view_type: {view_type}")
        print("=============================")
        response = self.json_client.list_json_documents(
            self.project_id, "runs")
        runs = []
        for json_document in response:
            run_info = RunInfo.from_dictionary(
                json.loads(json_document.json_value))
            if run_info.experiment_id == experiment_id and LifecycleStage.matches_view_type(view_type, run_info.lifecycle_stage):
                runs.append(run_info)
        return runs

    def log_batch(self, run_id: str, metrics: list[Metric], params: list[Param], tags: list[RunTag]) -> None:
        print("===== log_batch =====")
        print(f"run_id: {run_id}")
        print(f"metrics: {metrics}")
        print(f"params: {params}")
        print(f"tags: {tags}")
        print("=============================")
        _validate_run_id(run_id)
        _validate_batch_log_data(metrics, params, tags)
        _validate_batch_log_limits(metrics, params, tags)
        _validate_param_keys_unique(params)
        run_info = self._get_run_info(run_id)
        check_run_is_active(run_info)
        try:
            for param in params:
                self._log_run_param(run_info, param)
            for metric in metrics:
                self._log_run_metric(run_info, metric)
            for tag in tags:
                self._set_run_tag(run_info, tag)
        except Exception as e:
            raise MlflowException(e, INTERNAL_ERROR)

    def _log_run_param(self, run_info: RunInfo, param: Param) -> None:
        print("===== _log_run_param =====")
        print(f"run_info: {run_info}")
        print(f"param: {param}")
        print("=============================")

        params: list[Param] = self._get_all_params(run_info)
        params.append(Param(param.key, param.value))
        params_dict: dict = {p.key: p.value for p in params}
        self.json_client.update_json_document(
            self.project_id, "runs", run_info.run_uuid, json.dumps({"params": params_dict}))

    def _log_run_metric(self, run_info: RunInfo, metric: Metric) -> None:
        print("===== _log_run_metric =====")
        print(f"run_info: {run_info}")
        print(f"metric: {metric}")
        print("=============================")

        run: dict = json.loads(self.json_client.get_json_document(
            self.project_id, "runs", run_info.run_uuid).json_value)
        run_metrics = run["metrics"]
        if metric.key not in run_metrics:
            run_metrics[metric.key] = []
        run_metrics[metric.key].append(
            {"value": metric.value, "timestamp": metric.timestamp, "step": metric.step})
        self.json_client.update_json_document(
            self.project_id, "runs", run_info.run_uuid, json.dumps({"metrics": {metric.key: run_metrics[metric.key]}}))

    def _set_run_tag(self, run_info: RunInfo, tag: RunTag) -> None:
        print("===== _set_run_tag =====")
        print(f"run_info: {run_info}")
        print(f"tag: {tag}")
        print("=============================")
        tags: list[RunTag] = self._get_all_tags(run_info)
        tags.append(RunTag(tag.key, tag.value))
        tags_dict: dict = {t.key: t.value for t in tags}
        self.json_client.update_json_document(
            self.project_id, "runs", run_info.run_uuid, json.dumps({"tags": tags_dict}))

    def delete_tag(self, run_id: str, key: str) -> None:
        print("===== delete_tag =====")
        print(f"run_id: {run_id}")
        print(f"key: {key}")
        print("=============================")
        _validate_run_id(run_id)
        run_info = self._get_run_info(run_id)
        check_run_is_active(run_info)
        run: dict = json.loads(self.json_client.get_json_document(
            self.project_id, "runs", run_info.run_uuid).json_value)
        tags: dict = run["tags"]
        if key in tags:
            del tags[key]
            self.json_client.update_json_document(
                self.project_id, "runs", run_info.run_uuid, json.dumps({"tags": tags}))


def _read_persisted_experiment_dict(experiment_dict: dict) -> Experiment:
    print("===== _read_persisted_experiment_dict =====")
    print(f"experiment_dict: {experiment_dict}")
    print("=============================")

    dict_copy = experiment_dict.copy()

    # 'experiment_id' was changed from int to string, so we must cast to string
    # when reading legacy experiments
    if isinstance(dict_copy["experiment_id"], int):
        dict_copy["experiment_id"] = str(dict_copy["experiment_id"])
    return Experiment.from_dictionary(dict_copy)

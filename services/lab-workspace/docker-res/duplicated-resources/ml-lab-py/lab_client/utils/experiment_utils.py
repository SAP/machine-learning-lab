"""Utilities for experiment tracking."""

from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import inspect
import json
import logging
import os
import sys
import threading

from lab_api.swagger_client import GitInfo, HostInfo
from lab_client.commons import text_utils, system_utils, dependency_utils, notebook_utils, gpu_utils

IGNORED_DEPENDENCIES = ["ipython", "ipykernel", "tqdm"]

log = logging.getLogger(__name__)


def get_git_root(directory: str = None) -> str or None:
    from git import Repo, InvalidGitRepositoryError

    if not directory:
        directory = os.getcwd()

    try:
        git_repo = Repo(directory, search_parent_directories=True)
        git_root = git_repo.git.rev_parse("--show-toplevel")
        return os.path.abspath(git_root)
    except (InvalidGitRepositoryError, ValueError):
        return None


def get_git_info(directory: str = None) -> GitInfo:
    from git import Repo, InvalidGitRepositoryError

    if not directory:
        directory = os.getcwd()

    git_info = GitInfo()

    try:

        repo = Repo(directory, search_parent_directories=True)

        try:
            path = repo.remote().url
        except ValueError:
            path = 'git:/' + repo.working_dir
            is_dirty = repo.is_dirty()

        git_info.remote_url = path
        git_info.commit = repo.head.commit.hexsha
        git_info.branch = repo.active_branch.name

        reader = repo.config_reader()
        try:
            git_info.user_email = reader.get_value("user", "email")
            git_info.user_name = reader.get_value("user", "name")
        except:
            pass
    except (InvalidGitRepositoryError, ValueError):
        pass

    return git_info


def get_source_script(symbol_table: dict) -> (str, str, str):
    """
    Get the current executing script name, type (jupyter-notebook, python-script), and content
    """
    script_name = None
    script_content = None
    script_type = None

    if notebook_utils.in_jupyter_environment():
        jupyter_session = notebook_utils.jupyter_session_info()
        if jupyter_session:
            if "name" in jupyter_session:
                script_name = jupyter_session["name"]

            if "content" in jupyter_session and jupyter_session["content"]:
                script_content = json.dumps(jupyter_session["content"], indent=4)

            script_type = "jupyter-notebook"

    else:
        main_script, _, _ = dependency_utils.gather_sources_and_dependencies(symbol_table)
        if main_script and main_script.filename:
            script_name = os.path.basename(main_script.filename)

            with open(main_script.filename) as f:
                script_content = f.read()

            script_type = "python-script"

    return script_name, script_type, script_content


def get_python_dependencies(symbol_table: dict):
    # all dependencies with versions
    deps = []
    _, _, found_deps = dependency_utils.gather_sources_and_dependencies(symbol_table)

    if found_deps:
        for dependency in found_deps:
            if dependency.name not in IGNORED_DEPENDENCIES:
                deps.append(str(dependency.name) + "==" + str(dependency.version))

    return deps


def get_host_info() -> HostInfo:
    # https://github.com/workhorsy/py-cpuinfo
    # from psutil import virtual_memory, cpu_count
    import platform

    host_info = HostInfo()
    host_info.os = platform.platform()  # [platform.system(), platform.platform()]
    host_info.hostname = platform.node()  # alternative socket.gethostname()

    host_info.cpu = system_utils.cpu_info()
    host_info.cpu_cores = system_utils.cpu_count()
    host_info.memory_size = system_utils.total_memory()

    host_info.python_version = platform.python_version()
    host_info.python_compiler = platform.python_compiler()
    host_info.python_impl = platform.python_implementation()

    host_info.workspace_version = os.getenv("WORKSPACE_VERSION", None)

    try:
        # TODO get gpu information from sacred? https://github.com/IDSIA/sacred/blob/master/sacred/host_info.py
        host_info.gpus = gpu_utils.get_gpu_info()
    except:
        pass

    return host_info


def get_caller_symbol_table():
    return inspect.stack()[2][0].f_globals


def get_class_name(obj):
    module = obj.__class__.__module__
    if module is None or module == str.__class__.__module__:
        return obj.__class__.__name__
    return module + '.' + obj.__class__.__name__


# Copied from: https://github.com/IDSIA/sacred/blob/master/sacred/utils.py#L678
class IntervalTimer(threading.Thread):
    @classmethod
    def create(cls, func, interval=10):
        stop_event = threading.Event()
        timer_thread = cls(stop_event, func, interval)
        return stop_event, timer_thread

    def __init__(self, event, func, interval=10.):
        # TODO use super here.
        threading.Thread.__init__(self)
        self.stopped = event
        self.func = func
        self.interval = interval

    def run(self):
        while not self.stopped.wait(self.interval):
            self.func()
        self.func()


class StdoutFileRedirect:
    def __init__(self, log_path: str):
        # TODO check if sys already redirected sys.stdout.write.__name__
        self._stdout_current = sys.stdout.write
        self._stderr_current = sys.stderr.write
        self._log_path = log_path
        self._log_file = None

        def write_stdout(message):
            self._stdout_current(message)
            if message and len(message) > 1 and "varName" not in message:
                if not message.startswith('\n') and not message.startswith('\r'):
                    message = '\n' + message
                self.log_file.write(text_utils.safe_str(message))
                self.log_file.flush()

        self._write_stdout = write_stdout

        def write_stderr(message):
            self._stderr_current(message)
            if message and len(message) > 1 and "varName" not in message:
                if not message.startswith('\n') and not message.startswith('\r'):
                    message = '\n' + message
                self.log_file.write(text_utils.safe_str(message))
                self.log_file.flush()

        self._write_stderr = write_stderr

    @property
    def log_file(self):
        if self._log_file is None:
            if not os.path.exists(os.path.dirname(self._log_path)):
                os.makedirs(os.path.dirname(self._log_path))
            self._log_file = open(self._log_path, 'a', encoding='utf-8')
        return self._log_file

    def redirect(self):
        sys.stdout.write = self._write_stdout
        sys.stderr.write = self._write_stderr

    def reset(self):
        # Reset writers
        sys.stdout.write = self._stdout_current
        sys.stderr.write = self._stderr_current

        # Close file and set None
        if self._log_file:
            try:
                self._log_file.close()
            except ValueError:
                pass
            self._log_file = None


def get_keys_with_diff_values(dict_list: list):
    keys_with_diff_values = set()
    common_keys = set()
    for d in dict_list:
        if not common_keys:
            common_keys = set(d)
        else:
            common_keys = common_keys.intersection(set(d))

    combined_dict = {}
    for d in dict_list:
        for key in d:
            if key not in combined_dict:
                combined_dict[key] = []
            combined_dict[key].append(d[key])

    for key in common_keys:
        if len(set(combined_dict[key])) > 1:
            keys_with_diff_values.add(key)

    return keys_with_diff_values


def plot_exp_metric_comparison(exp_list: list, metric_keys: list or str = None, param_keys: list or str = None,
                               title="Experiment Results", interactive=False):
    """
    Comparison visualization of metrics of various experiments

    # Arguments
        exp_list (list): List of experiments.
        metric_keys (list or str): Metric keys to select for visualization. If no metrics_key are provided, the main result metric will be used (optional).
        param_keys (list or str): Param keys to use for label for visualization. If no param keys are provided, the experiment name will be used as label (optional).
        title (string): Title of the visualization (optional).
        interactive (boolean): If `True`, the visualization will be rendered with plotly (optional).
    """
    # TODO sort metric?
    labels = []
    data_traces = {}

    if metric_keys:
        if type(metric_keys) is not list:
            metric_keys = [metric_keys]

        for key in metric_keys:
            data_traces[key] = []
    else:
        data_traces["result"] = []

    if param_keys:
        if type(param_keys) is not list:
            param_keys = [param_keys]
    else:
        param_dicts = []
        for exp in exp_list:
            param_dicts.append(exp.params)
        try:
            # automatically select params that have changed values
            param_keys = get_keys_with_diff_values(param_dicts)
        except:
            # do nothing
            pass

    for exp in exp_list:
        if param_keys:
            param_dict = {}
            for param in param_keys:
                if param in exp.params:
                    param_dict[param] = exp.params[param]
            labels.append(text_utils.simplify_dict_to_str(param_dict))
        else:
            labels.append(text_utils.truncate_middle(exp.name, 30))

        if metric_keys:
            for key in metric_keys:
                if key in exp.metrics:
                    data_traces[key].append(exp.metrics[key])
                else:
                    log.warning("Metric " + key + "couldn't be found in experiment: " + str(exp.name))
        else:
            if exp.exp_metadata.result:
                data_traces["result"].append(exp.exp_metadata.result)
            else:
                log.warning("Result metric wasn't returned for experiment: " + str(exp.name))

    if interactive:
        import plotly
        import plotly.graph_objs as go

        plotly.offline.init_notebook_mode()

        data = []
        for key in data_traces:
            trace = go.Scatter(
                x=labels,
                y=data_traces[key],
                mode='lines+markers',
                name=key
            )
            data.append(trace)

        plotly.offline.iplot(data, filename=title)
    else:
        import pylab

        for key in data_traces:
            y = data_traces[key]
            pylab.plot(range(len(y)), y, label=key)
        pylab.legend(loc='best')
        pylab.xticks(range(len(labels)), labels, size='small', rotation='vertical')
        pylab.show()

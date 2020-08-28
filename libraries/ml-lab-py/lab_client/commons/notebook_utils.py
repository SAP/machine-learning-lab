from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import json
import os
import sys


def in_ipython_environment():
    "Is the code running in the ipython environment (jupyter including)"

    program_name = os.path.basename(os.getenv('_', ''))

    if ('jupyter-notebook' in program_name or  # jupyter-notebook
            'ipython' in program_name or  # ipython
            'JPY_PARENT_PID' in os.environ):  # ipython-notebook
        return True
    else:
        return False


def in_jupyter_environment():
    return "ipykernel" in sys.modules


def in_colab_environment():
    "Is the code running in Google Colaboratory?"
    if not in_ipython_environment(): return False
    try:
        from google import colab
        return True
    except:
        return False


def jupyter_session_info() -> dict or None:
    """
    Returns information about the running jupyter sessions, such as the name and content of the notebook
    """

    # https://stackoverflow.com/questions/12544056/how-do-i-get-the-current-ipython-notebook-name
    # https://github.com/jupyter/notebook/issues/1000

    if not in_jupyter_environment():
        raise Exception('Not in a Jupyter environment.')

    try:
        import ipykernel
    except ImportError:
        raise Exception('Failed to import Jupyter kernel.')

    try:
        from urllib.request import urlopen, pathname2url
    except ImportError:
        from urllib import urlopen, pathname2url

    try:  # Python 3 (see Edit2 below for why this may not work in Python 2)
        from notebook.notebookapp import list_running_servers
    except ImportError:  # Python 2
        import warnings
        from IPython.utils.shimmodule import ShimWarning
        with warnings.catch_warnings():
            warnings.simplefilter("ignore", category=ShimWarning)
            from IPython.html.notebookapp import list_running_servers

    connection_file_path = ipykernel.get_connection_file()
    connection_file = os.path.basename(connection_file_path)
    kernel_id = connection_file.split('-', 1)[1].split('.')[0]

    for server in list(list_running_servers()):
        api_url = server['url']
        try:
            token_param = ""
            if server['token'] == '' and not server['password']:  # No token and no password
                token_param = ""
            else:
                token_param = '?token=' + server['token']

            response = urlopen(api_url + 'api/sessions' + token_param)
            sessions = json.loads(response.read().decode())
            for sess in sessions:
                if sess['kernel']['id'] == kernel_id:
                    notebook_path = sess['notebook']['path']
                    content_url = api_url + 'api/contents/' + pathname2url(notebook_path).lstrip("/") + token_param
                    return json.loads(urlopen(content_url).read().decode())
        except:
            pass  # There may be stale entries in the runtime directory
    return None

import os, json
from setuptools import setup
from setuptools.command.install import install
from subprocess import call

from notebook.nbextensions import install_nbextension
from notebook.services.config import ConfigManager
from jupyter_core.paths import jupyter_config_dir

EXTENSION_NAME = "jupyterdiskcheck"
HANDLER_NAME = "jupyterdiskcheck_plugin"
WIDGET_NOTEBOOK = "notebook"
WIDGET_TREE = "tree"
EXT_DIR = os.path.join(os.path.dirname(__file__), EXTENSION_NAME)


class InstallCommand(install):
    def run(self):
        server_extension_name = EXTENSION_NAME+"."+HANDLER_NAME
        frontend_extension_notebook = EXTENSION_NAME+"/"+WIDGET_NOTEBOOK
        frontend_extension_tree = EXTENSION_NAME+"/"+WIDGET_TREE
        # Install Python package
        install.run(self)

        # Install JavaScript extensions to ~/.local/jupyter/
        #nbex_dir = os.path.expanduser('~/.ipython/nbextensions')
        install_nbextension(EXT_DIR, overwrite=True, user=True)# user=True,)

        # Activate the JS extensions on the notebook, tree, and edit screens
        js_cm = ConfigManager()
        js_cm.update('notebook', {"load_extensions": {frontend_extension_notebook: True}})
        js_cm.update('tree', {"load_extensions": {frontend_extension_tree: True}})

        # Activate the Python server extension
        jupyter_config_file = os.path.join(jupyter_config_dir(), "jupyter_notebook_config.json")
        if not os.path.isfile(jupyter_config_file):
            with open(jupyter_config_file, "w") as jsonFile:
                initial_data = {
                    "NotebookApp":{
                        "nbserver_extensions": {},
                        "server_extensions": []
                    }
                }
                json.dump(initial_data, jsonFile, indent=4)

        with open(jupyter_config_file, "r") as jsonFile:
            data = json.load(jsonFile)
            
        if 'server_extensions' not in data['NotebookApp']:
            data['NotebookApp']['server_extensions'] = []
        
        if 'nbserver_extensions' not in data['NotebookApp']:
            data['NotebookApp']['nbserver_extensions'] = {}
            
        if server_extension_name not in data['NotebookApp']['server_extensions']:
            data['NotebookApp']['server_extensions'] += [server_extension_name]
        
        data['NotebookApp']['nbserver_extensions'][server_extension_name] = True

        with open(jupyter_config_file, "w") as jsonFile:
            json.dump(data, jsonFile, indent=4)




setup(
    name='jupyterdiskcheck',
    version='0.1',
    packages=['jupyterdiskcheck'],
    #package_data={'.' : ['jupytergit/mydialog.js']},
    #include_package_data=True,
    cmdclass={
        'install': InstallCommand
    }
    
    # ,
    # install_requires=[
    #     'GitPython'
    # ]
)


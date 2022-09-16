# Python Client Library for ML Lab

This library is a high-level abstraction of Contaxy's client library.

## How to install

```shell
pip install 'git+https://github.com/SAP/machine-learning-lab.git#egg=lab-client&subdirectory=client'
```

## Usage

After installation, the package can be imported:

```python
from lab_client import Environment

# Initialize environment
env = Environment(project="project-id", lab_endpoint="https://ml-lab-deployment/api", lab_token="project-token")

env.file_handler.list_remote_files() 

```

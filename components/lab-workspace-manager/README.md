# ML Lab Workspaces Manager
The workspace manager is an ML Lab component which allows each user to spawn one or multiple machine learning workspaces.
Using the workspace manager UI, the users can view their workspaces, create new ones and access the workspace environment.

## Installation
Right now the docker image workspace manager is not published in a docker registry, so it need to be build locally.
For this, the build.py script can be used:
```
python build.py --make
```
After the image was successfully build, the workspace manager can be installed using the extension API of the ML backend. 
Assuming you have a local instance of ML Lab running on on port 30010, you should make the following request to register the workspace manager:
```
POST http://localhost:30010/projects/ctxy-global/extensions
{
  "api_extension_endpoint": "8080/api",
  "ui_extension_endpoint": "8080/app#/users/{env.userId}/workspace",
  "extension_type": "global-extension",
  "container_image": "lab-workspace-manager:latest",
  "parameters": {
    "WORKSPACE_MAX_MEMORY_MB": "2000",
    "WORKSPACE_MAX_CPUS": "2",
    "WORKSPACE_MAX_VOLUME_SIZE": "5000",
    "WORKSPACE_MAX_CONTAINER_SIZE": "5000"
  },
  "endpoints": [
    "8080",
  ],
  "display_name": "Workspaces",
  "icon": "code",
}
```
Alternatively, the workspace manager container can be started manually or using docker compose.
The ML Lab backend will discover it via Docker/Kubernetes labels.
Reference the [docker compose deployment file](../../deployment/mllab-docker/docker-compose.yml) for an example


## Local Development
When developing the workspace manager it can be helpful to start the backend and frontend locally instead of running them through the docker image.
### Local Backend Development
Start the backend locally using the following command:
```
cd backend
export CONTAXY_API_ENDPOINT=http://localhost:30010/api
export BACKEND_CORS_ORIGINS=http://localhost:3000
export PORT=8080
pipenv run python -m lab_workspace_manager.app
# Visit http://localhost:8080/docs
```
The environment variable CONTAXY_API_ENDPOINT needs to be set to a valid ML Lab backend which provides the contaxy API.
Alternatively, the app.py file can also be directly run from an IDE like PyCharm or VS Code.

### Local Frontend Development
Start the frontend locally using the following command:
```
cd webapp
export REACT_APP_CONTAXY_ENDPOINT=http://localhost:30010/api
export REACT_APP_EXTENSION_ENDPOINT=http://localhost:8080 
yarn start
```
The contaxy endpoint (ML Lab backend) and the workspace manager endpoint need to be specified.
If ML Lab or the workspace manager are run via a docker container, the /api must be added to the URL as the nginx in the container forwards the requests under this path to the actual backend.
If the backend is run locally, the /api must be omitted. 
**Important:** Cross Origin Requests (CORS) must be allowed by the backends.
This can be configured using the BACKEND_CORS_ORIGINS environment variable.

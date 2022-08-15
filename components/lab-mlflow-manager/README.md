# ML Lab Component Template
This is a template that can be used for creating new ML Lab components.
To create a new component, the template folder should be duplicated and renamed.
Inside the new folder, all occurrences of the string "insert-component-name-here" and "insert_component_name_here" need to be replaced with the name of the new component.  

## Local Development
### Local Backend Development
Start the backend locally using the following command:
```
cd backend
export CONTAXY_API_ENDPOINT=http://localhost:30010/api
export BACKEND_CORS_ORIGINS=http://localhost:3000
export PORT=8080
pipenv run python -m insert_component_name_here.app
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
The contaxy endpoint (ML Lab backend) and the ML Lab component endpoint need to be specified.
If the ML Lab backend or the ML Lab component are run via a docker container, the /api must be added to the URL as the nginx in the container forwards the requests under this path to the actual backend.
If the backend is run locally, the /api must be omitted. 
**Important:** Cross Origin Requests (CORS) must be allowed by the backends.
This can be configured using the BACKEND_CORS_ORIGINS environment variable.

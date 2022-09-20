<!-- markdownlint-disable -->

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-mlflow-manager/backend/src/lab_mlflow_manager/app.py#L0"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

# <kbd>module</kbd> `lab_mlflow_manager.app`




**Global Variables**
---------------
- **ACTION_START**
- **CREATE_RESOURCE_RESPONSES**
- **CONTAXY_API_ENDPOINT**
- **LABEL_EXTENSION_DEPLOYMENT_TYPE**

---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-mlflow-manager/backend/src/lab_mlflow_manager/app.py#L39"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>function</kbd> `deploy_mlflow_server`

```python
deploy_mlflow_server(
    project_id: str,
    component_manager: ComponentOperations = Depends(get_component_manager),
    token: str = Depends(APITokenExtractor)
) → Any
```

Create a new ML server by creating a Contaxy service with a mlflow server image in the personal project. 


---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-mlflow-manager/backend/src/lab_mlflow_manager/app.py#L70"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>function</kbd> `get_mlflow_server`

```python
get_mlflow_server(
    project_id: str,
    mlserver_id: str = Path(Ellipsis),
    component_manager: ComponentOperations = Depends(get_component_manager)
) → Any
```






---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-mlflow-manager/backend/src/lab_mlflow_manager/app.py#L96"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>function</kbd> `start_mlflow_server`

```python
start_mlflow_server(
    project_id: str,
    mlflow_server_id: str = Path(Ellipsis),
    component_manager: ComponentOperations = Depends(get_component_manager)
) → Any
```






---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-mlflow-manager/backend/src/lab_mlflow_manager/app.py#L117"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>function</kbd> `list_mlflow_servers`

```python
list_mlflow_servers(
    project_id: str,
    component_manager: ComponentOperations = Depends(get_component_manager)
) → Any
```






---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-mlflow-manager/backend/src/lab_mlflow_manager/app.py#L140"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>function</kbd> `delete_mlflow_server`

```python
delete_mlflow_server(
    project_id: str,
    mlflow_server_id: str = Path(Ellipsis),
    delete_volumes: Optional[bool] = Query(False),
    component_manager: ComponentOperations = Depends(get_component_manager)
) → Any
```






---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-mlflow-manager/backend/src/lab_mlflow_manager/app.py#L165"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>function</kbd> `create_mlflow_server_service_input`

```python
create_mlflow_server_service_input(
    lab_api_token: str,
    project_id: str,
    host: str
) → ServiceInput
```






---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-mlflow-manager/backend/src/lab_mlflow_manager/app.py#L189"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>function</kbd> `create_mlflow_server_from_service`

```python
create_mlflow_server_from_service(service: Service) → MLFlowServer
```






---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-mlflow-manager/backend/src/lab_mlflow_manager/app.py#L202"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>function</kbd> `is_mlflow_service`

```python
is_mlflow_service(service: Service) → bool
```








---

_This file was automatically generated via [lazydocs](https://github.com/ml-tooling/lazydocs)._

<!-- markdownlint-disable -->

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-workspace-manager/backend/src/lab_workspace_manager/app.py#L0"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

# <kbd>module</kbd> `lab_workspace_manager.app`




**Global Variables**
---------------
- **ACTION_START**
- **CREATE_RESOURCE_RESPONSES**
- **UPDATE_RESOURCE_RESPONSES**
- **CONTAXY_API_ENDPOINT**
- **LABEL_EXTENSION_DEPLOYMENT_TYPE**

---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-workspace-manager/backend/src/lab_workspace_manager/app.py#L52"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>function</kbd> `is_ws_service`

```python
is_ws_service(service: Service) → bool
```






---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-workspace-manager/backend/src/lab_workspace_manager/app.py#L58"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>function</kbd> `create_ws_service_input`

```python
create_ws_service_input(
    workspace_input: WorkspaceInput,
    user_token: str
) → ServiceInput
```






---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-workspace-manager/backend/src/lab_workspace_manager/app.py#L92"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>function</kbd> `create_ws_service_update`

```python
create_ws_service_update(workspace_update: WorkspaceUpdate) → ServiceUpdate
```






---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-workspace-manager/backend/src/lab_workspace_manager/app.py#L116"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>function</kbd> `compute_min_cpu`

```python
compute_min_cpu(max_cpus: int) → float
```






---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-workspace-manager/backend/src/lab_workspace_manager/app.py#L121"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>function</kbd> `compute_min_memory`

```python
compute_min_memory(max_memory: int) → int
```






---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-workspace-manager/backend/src/lab_workspace_manager/app.py#L127"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>function</kbd> `create_ws_from_service`

```python
create_ws_from_service(service: Service) → Workspace
```






---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-workspace-manager/backend/src/lab_workspace_manager/app.py#L150"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>function</kbd> `request_user_token`

```python
request_user_token(user_id: str, auth_manager: AuthOperations) → str
```






---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-workspace-manager/backend/src/lab_workspace_manager/app.py#L154"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>function</kbd> `deploy_workspace`

```python
deploy_workspace(
    workspace_input: WorkspaceInput,
    user_id: str = Path(Ellipsis),
    component_manager: ComponentOperations = Depends(get_component_manager)
) → Any
```

Create a new personal workspace by creating a Contaxy service with a workspace image in the personal project. 


---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-workspace-manager/backend/src/lab_workspace_manager/app.py#L186"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>function</kbd> `update_workspace`

```python
update_workspace(
    workspace_update: WorkspaceUpdate,
    user_id: str = Path(Ellipsis),
    workspace_id: str = Path(Ellipsis),
    component_manager: ComponentOperations = Depends(get_component_manager)
) → Any
```






---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-workspace-manager/backend/src/lab_workspace_manager/app.py#L210"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>function</kbd> `start_workspace`

```python
start_workspace(
    user_id: str = Path(Ellipsis),
    workspace_id: str = Path(Ellipsis),
    component_manager: ComponentOperations = Depends(get_component_manager)
) → Any
```






---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-workspace-manager/backend/src/lab_workspace_manager/app.py#L229"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>function</kbd> `list_workspaces`

```python
list_workspaces(
    user_id: str = Path(Ellipsis),
    component_manager: ComponentOperations = Depends(get_component_manager)
) → Any
```






---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-workspace-manager/backend/src/lab_workspace_manager/app.py#L250"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>function</kbd> `get_workspace`

```python
get_workspace(
    user_id: str = Path(Ellipsis),
    workspace_id: str = Path(Ellipsis),
    component_manager: ComponentOperations = Depends(get_component_manager)
) → Any
```






---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-workspace-manager/backend/src/lab_workspace_manager/app.py#L276"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>function</kbd> `delete_workspace`

```python
delete_workspace(
    user_id: str = Path(Ellipsis),
    service_id: str = Path(Ellipsis),
    delete_volumes: Optional[bool] = Query(False),
    component_manager: ComponentOperations = Depends(get_component_manager)
) → Any
```






---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-workspace-manager/backend/src/lab_workspace_manager/app.py#L301"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>function</kbd> `get_workspace_config`

```python
get_workspace_config(
    component_manager: ComponentOperations = Depends(get_component_manager)
) → Any
```








---

_This file was automatically generated via [lazydocs](https://github.com/ml-tooling/lazydocs)._

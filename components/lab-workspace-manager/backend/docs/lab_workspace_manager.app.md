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

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-workspace-manager/backend/src/lab_workspace_manager/app.py#L48"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>function</kbd> `is_ws_service`

```python
is_ws_service(service: Service) → bool
```






---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-workspace-manager/backend/src/lab_workspace_manager/app.py#L54"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>function</kbd> `create_ws_service_input`

```python
create_ws_service_input(
    workspace_input: WorkspaceInput,
    user_token: str
) → ServiceInput
```






---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-workspace-manager/backend/src/lab_workspace_manager/app.py#L86"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>function</kbd> `create_ws_service_update`

```python
create_ws_service_update(workspace_update: WorkspaceUpdate) → ServiceUpdate
```






---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-workspace-manager/backend/src/lab_workspace_manager/app.py#L104"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>function</kbd> `create_ws_from_service`

```python
create_ws_from_service(service: Service) → Workspace
```






---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-workspace-manager/backend/src/lab_workspace_manager/app.py#L127"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>function</kbd> `request_user_token`

```python
request_user_token(
    user_id: str,
    workspace_name: str,
    auth_manager: AuthOperations
) → str
```






---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-workspace-manager/backend/src/lab_workspace_manager/app.py#L139"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>function</kbd> `deploy_workspace`

```python
deploy_workspace(
    workspace_input: WorkspaceInput,
    user_id: str = Path(Ellipsis),
    component_manager: ComponentManager = Depends(get_component_manager)
) → Any
```

Create a new personal workspace by creating a Contaxy service with a workspace image in the personal project. 


---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-workspace-manager/backend/src/lab_workspace_manager/app.py#L173"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>function</kbd> `update_workspace`

```python
update_workspace(
    workspace_update: WorkspaceUpdate,
    user_id: str = Path(Ellipsis),
    workspace_id: str = Path(Ellipsis),
    component_manager: ComponentManager = Depends(get_component_manager)
) → Any
```






---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-workspace-manager/backend/src/lab_workspace_manager/app.py#L197"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>function</kbd> `start_workspace`

```python
start_workspace(
    user_id: str = Path(Ellipsis),
    workspace_id: str = Path(Ellipsis),
    component_manager: ComponentManager = Depends(get_component_manager)
) → Any
```






---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-workspace-manager/backend/src/lab_workspace_manager/app.py#L216"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>function</kbd> `list_workspaces`

```python
list_workspaces(
    user_id: str = Path(Ellipsis),
    component_manager: ComponentManager = Depends(get_component_manager)
) → Any
```






---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-workspace-manager/backend/src/lab_workspace_manager/app.py#L237"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>function</kbd> `get_workspace`

```python
get_workspace(
    user_id: str = Path(Ellipsis),
    workspace_id: str = Path(Ellipsis),
    component_manager: ComponentManager = Depends(get_component_manager)
) → Any
```






---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-workspace-manager/backend/src/lab_workspace_manager/app.py#L263"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>function</kbd> `delete_workspace`

```python
delete_workspace(
    user_id: str = Path(Ellipsis),
    service_id: str = Path(Ellipsis),
    delete_volumes: Optional[bool] = Query(False),
    component_manager: ComponentManager = Depends(get_component_manager)
) → Any
```






---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-workspace-manager/backend/src/lab_workspace_manager/app.py#L288"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>function</kbd> `get_workspace_config`

```python
get_workspace_config(
    component_manager: ComponentManager = Depends(get_component_manager)
) → Any
```








---

_This file was automatically generated via [lazydocs](https://github.com/ml-tooling/lazydocs)._

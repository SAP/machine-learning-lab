<!-- markdownlint-disable -->

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-workspace-manager/backend/src/lab_workspace_manager/app.py#L0"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

# <kbd>module</kbd> `lab_workspace_manager.app`




**Global Variables**
---------------
- **CREATE_RESOURCE_RESPONSES**
- **CONTAXY_API_ENDPOINT**
- **SELF_ACCESS_URL**
- **SELF_DEPLOYMENT_NAME**
- **LABEL_EXTENSION_DEPLOYMENT_TYPE**
- **WORKSPACE_MAX_MEMORY_MB**
- **WORKSPACE_MAX_CPUS**
- **WORKSPACE_MAX_VOLUME_SIZE**
- **WORKSPACE_MAX_CONTAINER_SIZE**

---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-workspace-manager/backend/src/lab_workspace_manager/app.py#L45"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>function</kbd> `is_workspace`

```python
is_workspace(service: Service) → bool
```






---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-workspace-manager/backend/src/lab_workspace_manager/app.py#L51"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>function</kbd> `create_workspace`

```python
create_workspace(
    service: ServiceInput,
    user_id: str = Path(Ellipsis),
    component_manager: ComponentManager = Depends(get_component_manager)
) → Any
```

Create a new personal workspace by creating a Contaxy service with a workspace image in the personal project. 


---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-workspace-manager/backend/src/lab_workspace_manager/app.py#L113"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>function</kbd> `list_workspaces`

```python
list_workspaces(
    user_id: str = Path(Ellipsis),
    component_manager: ComponentManager = Depends(get_component_manager)
) → Any
```






---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-workspace-manager/backend/src/lab_workspace_manager/app.py#L129"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>function</kbd> `get_workspace`

```python
get_workspace(
    user_id: str = Path(Ellipsis),
    service_id: str = Path(Ellipsis),
    component_manager: ComponentManager = Depends(get_component_manager)
) → Any
```






---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-workspace-manager/backend/src/lab_workspace_manager/app.py#L152"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

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

_This file was automatically generated via [lazydocs](https://github.com/ml-tooling/lazydocs)._

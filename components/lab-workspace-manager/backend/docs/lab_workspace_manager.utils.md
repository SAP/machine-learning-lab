<!-- markdownlint-disable -->

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-workspace-manager/backend/src/lab_workspace_manager/utils.py#L0"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

# <kbd>module</kbd> `lab_workspace_manager.utils`




**Global Variables**
---------------
- **CONTAXY_API_ENDPOINT**
- **get_api_token**

---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-workspace-manager/backend/src/lab_workspace_manager/utils.py#L59"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>function</kbd> `get_component_manager`

```python
get_component_manager(
    token: str = Depends(APITokenExtractor)
) → Generator[ComponentOperations, NoneType, NoneType]
```

Returns the initialized component manager. 

This is used as FastAPI dependency and called for every request. 


---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-workspace-manager/backend/src/lab_workspace_manager/utils.py#L20"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>class</kbd> `APITokenExtractor`




<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-workspace-manager/backend/src/lab_workspace_manager/utils.py#L21"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

### <kbd>method</kbd> `__init__`

```python
__init__(auto_error: bool = True)
```











---

_This file was automatically generated via [lazydocs](https://github.com/ml-tooling/lazydocs)._

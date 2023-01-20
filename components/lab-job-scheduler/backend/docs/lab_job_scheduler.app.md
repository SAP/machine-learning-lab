<!-- markdownlint-disable -->

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-job-scheduler/backend/src/lab_job_scheduler/app.py#L0"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

# <kbd>module</kbd> `lab_job_scheduler.app`




**Global Variables**
---------------
- **CREATE_RESOURCE_RESPONSES**
- **JOB_INTERVAL**
- **CONTAXY_API_ENDPOINT**
- **cached_scheduled_jobs**
- **lock**

---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-job-scheduler/backend/src/lab_job_scheduler/app.py#L41"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>function</kbd> `on_startup`

```python
on_startup() → None
```






---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-job-scheduler/backend/src/lab_job_scheduler/app.py#L71"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>function</kbd> `create_schedule`

```python
create_schedule(
    project_id: str,
    job_input: ScheduledJobInput,
    component_manager: ComponentOperations = Depends(get_component_manager)
) → Any
```






---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-job-scheduler/backend/src/lab_job_scheduler/app.py#L99"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>function</kbd> `list_schedules`

```python
list_schedules(project_id: str) → Any
```






---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-job-scheduler/backend/src/lab_job_scheduler/app.py#L118"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>function</kbd> `list_schedule`

```python
list_schedule(project_id: str, job_id: str) → Any
```






---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-job-scheduler/backend/src/lab_job_scheduler/app.py#L141"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>function</kbd> `delete_schedules`

```python
delete_schedules(
    project_id: str,
    component_manager: ComponentOperations = Depends(get_component_manager)
) → Any
```






---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-job-scheduler/backend/src/lab_job_scheduler/app.py#L158"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>function</kbd> `delete_schedule`

```python
delete_schedule(
    project_id: str,
    job_id: str,
    component_manager: ComponentOperations = Depends(get_component_manager)
) → Any
```






---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-job-scheduler/backend/src/lab_job_scheduler/app.py#L182"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>function</kbd> `update_schedule`

```python
update_schedule(
    project_id: str,
    job_input: ScheduledJobInput,
    job_id: str,
    component_manager: ComponentOperations = Depends(get_component_manager)
) → Any
```






---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-job-scheduler/backend/src/lab_job_scheduler/app.py#L212"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>function</kbd> `executor_info`

```python
executor_info() → Any
```






---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-job-scheduler/backend/src/lab_job_scheduler/app.py#L224"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>function</kbd> `get_all_scheduled_jobs_from_db`

```python
get_all_scheduled_jobs_from_db(
    component_manager: ComponentOperations,
    project_id: str
) → List[ScheduledJob]
```

Returns all jobs from the database. 


---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-job-scheduler/backend/src/lab_job_scheduler/app.py#L233"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>function</kbd> `get_job_from_job_input`

```python
get_job_from_job_input(
    job_schedule: ScheduledJobInput,
    job_id: Optional[str] = None
) → ScheduledJob
```






---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-job-scheduler/backend/src/lab_job_scheduler/app.py#L246"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>function</kbd> `get_next_run_time`

```python
get_next_run_time(job_schedule: ScheduledJobInput) → datetime
```

Returns the next run time of a job. 




---

_This file was automatically generated via [lazydocs](https://github.com/ml-tooling/lazydocs)._

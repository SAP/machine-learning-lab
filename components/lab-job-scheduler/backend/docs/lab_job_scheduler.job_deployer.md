<!-- markdownlint-disable -->

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-job-scheduler/backend/src/lab_job_scheduler/job_deployer.py#L0"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

# <kbd>module</kbd> `lab_job_scheduler.job_deployer`





---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-job-scheduler/backend/src/lab_job_scheduler/job_deployer.py#L13"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>function</kbd> `run_scheduled_jobs`

```python
run_scheduled_jobs(
    cached_scheduled_jobs: Dict[str, Dict[str, ScheduledJob]],
    lock: <built-in function allocate_lock>,
    component_manager: ComponentOperations
) → None
```

Runs all scheduled jobs. 


---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-job-scheduler/backend/src/lab_job_scheduler/job_deployer.py#L28"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>function</kbd> `is_due`

```python
is_due(job: ScheduledJob, reference_time: Optional[datetime] = None) → bool
```

Checks if a job is due. 


---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-job-scheduler/backend/src/lab_job_scheduler/job_deployer.py#L36"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>function</kbd> `get_next_run_time`

```python
get_next_run_time(job: ScheduledJob) → datetime
```

Returns the next run time of a job. 


---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-job-scheduler/backend/src/lab_job_scheduler/job_deployer.py#L46"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>function</kbd> `deploy_job`

```python
deploy_job(
    job: ScheduledJob,
    component_manager: ComponentOperations,
    project_id: str
) → None
```

Executes a job. 


---

<a href="https://github.com/ml-tooling/contaxy/blob/main/components/lab-job-scheduler/backend/src/lab_job_scheduler/job_deployer.py#L55"><img align="right" style="float:right;" src="https://img.shields.io/badge/-source-cccccc?style=flat-square"></a>

## <kbd>function</kbd> `update_db`

```python
update_db(
    job: ScheduledJob,
    component_manager: ComponentOperations,
    project_id: str,
    cached_scheduled_jobs: Dict[str, Dict[str, ScheduledJob]]
) → None
```

Updates the last run and next run of a job. 




---

_This file was automatically generated via [lazydocs](https://github.com/ml-tooling/lazydocs)._

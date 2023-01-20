<!-- markdownlint-disable -->

# API Overview

## Modules

- [`lab_job_scheduler.app`](./lab_job_scheduler.app.md#module-lab_job_schedulerapp)
- [`lab_job_scheduler.config`](./lab_job_scheduler.config.md#module-lab_job_schedulerconfig)
- [`lab_job_scheduler.job_deployer`](./lab_job_scheduler.job_deployer.md#module-lab_job_schedulerjob_deployer)
- [`lab_job_scheduler.schema`](./lab_job_scheduler.schema.md#module-lab_job_schedulerschema)
- [`lab_job_scheduler.utils`](./lab_job_scheduler.utils.md#module-lab_job_schedulerutils)

## Classes

- [`schema.ScheduledJob`](./lab_job_scheduler.schema.md#class-scheduledjob)
- [`schema.ScheduledJobInput`](./lab_job_scheduler.schema.md#class-scheduledjobinput)

## Functions

- [`app.create_schedule`](./lab_job_scheduler.app.md#function-create_schedule)
- [`app.delete_schedule`](./lab_job_scheduler.app.md#function-delete_schedule)
- [`app.delete_schedules`](./lab_job_scheduler.app.md#function-delete_schedules)
- [`app.executor_info`](./lab_job_scheduler.app.md#function-executor_info)
- [`app.get_all_scheduled_jobs_from_db`](./lab_job_scheduler.app.md#function-get_all_scheduled_jobs_from_db): Returns all jobs from the database.
- [`app.get_job_from_job_input`](./lab_job_scheduler.app.md#function-get_job_from_job_input)
- [`app.get_next_run_time`](./lab_job_scheduler.app.md#function-get_next_run_time): Returns the next run time of a job.
- [`app.list_schedule`](./lab_job_scheduler.app.md#function-list_schedule)
- [`app.list_schedules`](./lab_job_scheduler.app.md#function-list_schedules)
- [`app.on_startup`](./lab_job_scheduler.app.md#function-on_startup)
- [`app.update_schedule`](./lab_job_scheduler.app.md#function-update_schedule)
- [`job_deployer.deploy_job`](./lab_job_scheduler.job_deployer.md#function-deploy_job): Executes a job.
- [`job_deployer.get_next_run_time`](./lab_job_scheduler.job_deployer.md#function-get_next_run_time): Returns the next run time of a job.
- [`job_deployer.is_due`](./lab_job_scheduler.job_deployer.md#function-is_due): Checks if a job is due.
- [`job_deployer.run_scheduled_jobs`](./lab_job_scheduler.job_deployer.md#function-run_scheduled_jobs): Runs all scheduled jobs.
- [`job_deployer.update_db`](./lab_job_scheduler.job_deployer.md#function-update_db): Updates the last run and next run of a job.
- [`utils.get_component_manager`](./lab_job_scheduler.utils.md#function-get_component_manager): Returns the initialized component manager.


---

_This file was automatically generated via [lazydocs](https://github.com/ml-tooling/lazydocs)._

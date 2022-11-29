<!-- markdownlint-disable -->

# API Overview

## Modules

- [`lab_job_scheduler.config`](./lab_job_scheduler.config.md#module-lab_job_schedulerconfig)
- [`lab_job_scheduler.job_deployer`](./lab_job_scheduler.job_deployer.md#module-lab_job_schedulerjob_deployer)
- [`lab_job_scheduler.schema`](./lab_job_scheduler.schema.md#module-lab_job_schedulerschema)

## Classes

- [`schema.ScheduledJob`](./lab_job_scheduler.schema.md#class-scheduledjob)
- [`schema.ScheduledJobInput`](./lab_job_scheduler.schema.md#class-scheduledjobinput)

## Functions

- [`job_deployer.deploy_job`](./lab_job_scheduler.job_deployer.md#function-deploy_job): Executes a job.
- [`job_deployer.get_next_run_time`](./lab_job_scheduler.job_deployer.md#function-get_next_run_time): Returns the next run time of a job.
- [`job_deployer.is_due`](./lab_job_scheduler.job_deployer.md#function-is_due): Checks if a job is due.
- [`job_deployer.run_scheduled_jobs`](./lab_job_scheduler.job_deployer.md#function-run_scheduled_jobs): Runs all scheduled jobs.
- [`job_deployer.update_db`](./lab_job_scheduler.job_deployer.md#function-update_db): Updates the last run and next run of a job.


---

_This file was automatically generated via [lazydocs](https://github.com/ml-tooling/lazydocs)._

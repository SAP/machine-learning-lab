import json
import threading
from datetime import datetime
from typing import Dict, Optional

from contaxy.operations.components import ComponentOperations
from croniter import croniter
from loguru import logger

from lab_job_scheduler.schema import ScheduledJob


def run_scheduled_jobs(
    cached_scheduled_jobs: Dict[str, Dict[str, ScheduledJob]],
    lock: threading.Lock,
    component_manager: ComponentOperations,
) -> None:
    """Runs all scheduled jobs."""
    with lock:
        for project_id, jobs in cached_scheduled_jobs.items():
            for job_id, job in jobs.items():
                if is_due(job):
                    logger.info(f"Deploying job {job_id}")
                    deploy_job(job, component_manager, project_id)
                    update_db(job, component_manager, project_id, cached_scheduled_jobs)


def is_due(job: ScheduledJob, reference_time: Optional[datetime] = None) -> bool:
    """Checks if a job is due."""
    if not reference_time:
        reference_time = datetime.now()
    next_run_time = get_next_run_time(job)
    return next_run_time <= reference_time


def get_next_run_time(job: ScheduledJob) -> datetime:
    """Returns the next run time of a job."""
    if job.last_run:
        base = datetime.fromisoformat(job.last_run)
    else:
        base = datetime.fromisoformat(job.created)
    cron = croniter(job.cron_string, base)
    return cron.get_next(datetime)


def deploy_job(
    job: ScheduledJob, component_manager: ComponentOperations, project_id: str
) -> None:
    """Executes a job."""
    component_manager.get_job_manager().deploy_job(
        project_id=project_id, job_input=job.job_input
    )


def update_db(
    job: ScheduledJob,
    component_manager: ComponentOperations,
    project_id: str,
    cached_scheduled_jobs: Dict[str, Dict[str, ScheduledJob]],
) -> None:
    """Updates the last run and next run of a job."""
    job.last_run = datetime.now().isoformat()
    job.next_run = get_next_run_time(job).isoformat()
    db = component_manager.get_json_db_manager()
    db.update_json_document(
        project_id=project_id,
        collection_id="schedules",
        key=job.job_id,
        json_document=json.dumps(job.dict()),
    )
    cached_scheduled_jobs[project_id][job.job_id].last_run = job.last_run
    cached_scheduled_jobs[project_id][job.job_id].next_run = job.next_run

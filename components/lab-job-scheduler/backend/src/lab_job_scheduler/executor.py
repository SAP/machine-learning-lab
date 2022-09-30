from datetime import datetime
from croniter import croniter
from contaxy.operations.components import ComponentOperations
from typing import List
from lab_job_scheduler.schema import ScheduledJob
import json
from loguru import logger


def run_scheduled_jobs(component_manager: ComponentOperations):
    """Runs all scheduled jobs."""
    projects = component_manager.get_project_manager().list_projects()
    for project in projects:
        jobs = get_all_jobs_from_db(component_manager, project.id)
        for job in jobs:
            if is_due(job):
                logger.info(f"Deploying job {job.job_id}")
                deploy_job(job, component_manager, project.id)
                update_last_run(job, component_manager, project.id)


def get_all_jobs_from_db(component_manager: ComponentOperations, project_id: str) -> List[ScheduledJob]:
    """Returns all jobs from the database."""
    db = component_manager.get_json_db_manager()
    documents = db.list_json_documents(
        project_id=project_id, collection_id="schedules"
    )
    return [ScheduledJob(**json.loads(document.json_value)) for document in documents]


def is_due(job: ScheduledJob) -> bool:
    """Checks if a job is due."""
    next_run_time = get_next_run_time(job)
    current_time = datetime.now()
    if next_run_time <= current_time:
        return True
    return False


def get_next_run_time(job: ScheduledJob) -> datetime:
    """Returns the next run time of a job."""
    if job.last_run:
        base = datetime.fromisoformat(job.last_run)
    else:
        base = datetime.fromisoformat(job.created)
    cron = croniter(job.cron_string, base)
    return cron.get_next(datetime)


def deploy_job(job: ScheduledJob, component_manager: ComponentOperations, project_id: str):
    """Executes a job."""
    # TODO: Understand how job execution works with contaxy and implement it here.
    component_manager.get_job_manager().deploy_job(
        project_id=project_id, job_input=job.job_input)


def update_last_run(job: ScheduledJob, component_manager: ComponentOperations, project_id: str):
    """Updates the last run of a job."""
    job.last_run = datetime.now().isoformat()
    db = component_manager.get_json_db_manager()
    db.update_json_document(
        project_id=project_id,
        collection_id="schedules",
        key=job.job_id,
        json_document=json.dumps(job.dict()),
    )

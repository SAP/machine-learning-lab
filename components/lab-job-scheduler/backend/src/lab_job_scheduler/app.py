import datetime
import functools
import json
import os
import threading
from typing import Any, Dict, List

from contaxy.operations.components import ComponentOperations
from contaxy.schema.exceptions import CREATE_RESOURCE_RESPONSES
from contaxy.utils import fastapi_utils, id_utils
from croniter import croniter
from fastapi import Depends, FastAPI, HTTPException, status
from starlette.middleware.cors import CORSMiddleware

from lab_job_scheduler import job_deployer
from lab_job_scheduler.config import JOB_INTERVAL
from lab_job_scheduler.schema import ScheduledJob, ScheduledJobInput
from lab_job_scheduler.utils import CONTAXY_API_ENDPOINT, get_component_manager

app = FastAPI()
# Patch FastAPI to allow relative path resolution.
fastapi_utils.patch_fastapi(app)
# Allow CORS configuration
if "BACKEND_CORS_ORIGINS" in os.environ:
    app.add_middleware(
        CORSMiddleware,
        allow_origins=os.environ["BACKEND_CORS_ORIGINS"].split(","),
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

# maps project_id to job_id to job. Used to minimize database calls.
cached_scheduled_jobs: Dict[str, Dict[str, ScheduledJob]] = {}

lock = threading.Lock()

# Startup event to run scheduled jobs


@app.on_event("startup")
def on_startup() -> None:
    token = os.environ["CONTAXY_API_TOKEN"]  # TODO: Use a better way to get the token.
    component_manager: ComponentOperations = get_component_manager(token=token)

    with lock:

        # cache all scheduled jobs from the database
        for project in component_manager.get_project_manager().list_projects():
            scheduled_jobs = get_all_scheduled_jobs_from_db(
                component_manager, project.id
            )

            if project.id not in cached_scheduled_jobs:
                cached_scheduled_jobs[project.id] = {}

            for job in scheduled_jobs:
                cached_scheduled_jobs[project.id][job.job_id] = job

    fastapi_utils.schedule_call(
        func=functools.partial(
            job_deployer.run_scheduled_jobs,
            cached_scheduled_jobs,
            lock,
            component_manager,
        ),
        interval=datetime.timedelta(seconds=JOB_INTERVAL),
    )


@app.post(
    "/project/{project_id}/schedule",
    summary="Creates a new scheduled job.",
    status_code=status.HTTP_200_OK,
    responses={**CREATE_RESOURCE_RESPONSES},
)
def create_schedule(
    project_id: str,
    job_input: ScheduledJobInput,
    component_manager: ComponentOperations = Depends(get_component_manager),
) -> Any:
    job = get_job_from_job_input(job_input)
    db = component_manager.get_json_db_manager()
    resp = db.create_json_document(
        project_id=project_id,
        collection_id="schedules",
        key=job.job_id,
        json_document=json.dumps((job.dict())),
    )

    with lock:
        if project_id not in cached_scheduled_jobs:
            cached_scheduled_jobs[project_id] = {}
        cached_scheduled_jobs[project_id][job.job_id] = job

    return resp


@app.get(
    "/project/{project_id}/schedules",
    summary="List all job schedules",
    status_code=status.HTTP_200_OK,
    responses={**CREATE_RESOURCE_RESPONSES},
)
def list_schedules(
    project_id: str,
) -> Any:

    with lock:
        if project_id not in cached_scheduled_jobs:
            return []

        output = list(cached_scheduled_jobs[project_id].values())

    return output


@app.get(
    "/project/{project_id}/schedule/{job_id}",
    summary="Get a job schedule",
    status_code=status.HTTP_200_OK,
    responses={**CREATE_RESOURCE_RESPONSES},
)
def list_schedule(
    project_id: str,
    job_id: str,
) -> Any:

    with lock:
        if project_id not in cached_scheduled_jobs:
            raise HTTPException(status_code=404, detail="Project not found.")

        if job_id not in cached_scheduled_jobs[project_id]:
            raise HTTPException(status_code=404, detail="Job not found.")

        output = cached_scheduled_jobs[project_id][job_id]

    return output


@app.delete(
    "/project/{project_id}/schedules",
    summary="Deletes all job schedules",
    status_code=status.HTTP_200_OK,
    responses={**CREATE_RESOURCE_RESPONSES},
)
def delete_schedules(
    project_id: str,
    component_manager: ComponentOperations = Depends(get_component_manager),
) -> Any:
    db = component_manager.get_json_db_manager()
    db.delete_json_collection(project_id=project_id, collection_id="schedules")

    with lock:
        cached_scheduled_jobs[project_id] = {}


@app.delete(
    "/project/{project_id}/schedule/{job_id}",
    summary="Deletes a job schedule",
    status_code=status.HTTP_200_OK,
    responses={**CREATE_RESOURCE_RESPONSES},
)
def delete_schedule(
    project_id: str,
    job_id: str,
    component_manager: ComponentOperations = Depends(get_component_manager),
) -> Any:
    db = component_manager.get_json_db_manager()
    db.delete_json_document(
        project_id=project_id, collection_id="schedules", key=job_id
    )

    with lock:
        if (
            project_id in cached_scheduled_jobs
            and job_id in cached_scheduled_jobs[project_id]
        ):
            del cached_scheduled_jobs[project_id][job_id]


@app.post(
    "/project/{project_id}/schedule/{job_id}",
    summary="Updates a job schedule",
    status_code=status.HTTP_200_OK,
    responses={**CREATE_RESOURCE_RESPONSES},
)
def update_schedule(
    project_id: str,
    job_input: ScheduledJobInput,
    job_id: str,
    component_manager: ComponentOperations = Depends(get_component_manager),
) -> Any:
    job = get_job_from_job_input(job_input, job_id)
    db = component_manager.get_json_db_manager()
    resp = db.update_json_document(
        project_id=project_id,
        collection_id="schedules",
        key=job_id,
        json_document=json.dumps((job.dict())),
    )

    with lock:
        if project_id not in cached_scheduled_jobs:
            cached_scheduled_jobs[project_id] = {}

        cached_scheduled_jobs[project_id][job_id] = job

    return resp


@app.get(
    "/executor/info",
    summary="Display information about the executor",
    status_code=status.HTTP_200_OK,
    responses={**CREATE_RESOURCE_RESPONSES},
)
def executor_info() -> Any:
    return {
        "execution_frequency": JOB_INTERVAL,
    }


def get_all_scheduled_jobs_from_db(
    component_manager: ComponentOperations, project_id: str
) -> List[ScheduledJob]:
    """Returns all jobs from the database."""
    db = component_manager.get_json_db_manager()
    documents = db.list_json_documents(project_id=project_id, collection_id="schedules")
    return [ScheduledJob(**json.loads(document.json_value)) for document in documents]


def get_job_from_job_input(
    job_schedule: ScheduledJobInput, job_id: str = None
) -> ScheduledJob:

    return ScheduledJob(
        cron_string=job_schedule.cron_string,
        job_input=job_schedule.job_input,
        created=datetime.datetime.now().isoformat(),
        job_id=id_utils.generate_short_uuid() if job_id is None else job_id,
        next_run=get_next_run_time(job_schedule).isoformat(),
    )


def get_next_run_time(job_schedule: ScheduledJobInput) -> datetime.datetime:
    """Returns the next run time of a job."""
    base = datetime.datetime.now()
    cron = croniter(job_schedule.cron_string, base)
    return cron.get_next(datetime.datetime)


if __name__ == "__main__":
    import uvicorn

    if not CONTAXY_API_ENDPOINT:
        raise RuntimeError("CONTAXY_API_ENDPOINT must be set")

    # Prevent duplicated logs
    log_config = uvicorn.config.LOGGING_CONFIG
    log_config["loggers"]["uvicorn"]["propagate"] = False
    uvicorn.run(
        "lab_job_scheduler.app:app",
        host="localhost",
        port=int(os.getenv("PORT", 8080)),
        log_level="info",
        reload=True,
        log_config=log_config,
    )

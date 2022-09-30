import os
import json
from textwrap import indent
import uuid
from typing import Any
import datetime
import functools

from contaxy.operations.components import ComponentOperations
from contaxy.schema.exceptions import CREATE_RESOURCE_RESPONSES
from contaxy.utils import fastapi_utils
from fastapi import Depends, FastAPI, status
from starlette.middleware.cors import CORSMiddleware

from lab_job_scheduler.utils import CONTAXY_API_ENDPOINT, get_component_manager
from lab_job_scheduler.schema import ScheduledJob, ScheduledJobInput
from lab_job_scheduler.config import JOB_INTERVAL
from lab_job_scheduler import executor

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


# Startup event to run scheduled jobs
@app.on_event("startup")
def on_startup() -> None:
    token = os.environ["CONTAXY_API_TOKEN"]  # TODO: Use a better way to get the token.
    component_manager: ComponentOperations = get_component_manager(token=token)
    fastapi_utils.schedule_call(
        func=functools.partial(executor.run_scheduled_jobs, component_manager),
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
    return db.create_json_document(project_id=project_id,
                                   collection_id="schedules", key=job.job_id, json_document=json.dumps((job.dict())))


@app.get(
    "/project/{project_id}/schedules",
    summary="List all job schedules",
    status_code=status.HTTP_200_OK,
    responses={**CREATE_RESOURCE_RESPONSES},
)
def list_schedules(
    project_id: str,
    component_manager: ComponentOperations = Depends(get_component_manager),
) -> Any:
    db = component_manager.get_json_db_manager()
    documents = db.list_json_documents(project_id=project_id, collection_id="schedules")
    return [json.loads(document.json_value) for document in documents]


@app.get(
    "/project/{project_id}/schedule/{job_id}",
    summary="List a job schedule",
    status_code=status.HTTP_200_OK,
    responses={**CREATE_RESOURCE_RESPONSES},
)
def list_schedule(
    project_id: str,
    job_id: str,
    component_manager: ComponentOperations = Depends(get_component_manager),
) -> Any:
    db = component_manager.get_json_db_manager()
    document = db.get_json_document(
        project_id=project_id, collection_id="schedules", key=job_id)
    return json.loads(document.json_value)


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
    return db.delete_json_collection(project_id=project_id, collection_id="schedules")


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
    return db.delete_json_document(
        project_id=project_id, collection_id="schedules", key=job_id)


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
    job = get_job_from_job_input(job_input)
    db = component_manager.get_json_db_manager()
    return db.update_json_document(project_id=project_id,
                                   collection_id="schedules", key=job_id, json_document=json.dumps((job.dict())))


def get_job_from_job_input(job_schedule: ScheduledJobInput) -> ScheduledJob:
    return ScheduledJob(
        cron_string=job_schedule.cron_string,
        job_input=job_schedule.job_input,
        created=datetime.datetime.now().isoformat(),
        job_id=str(uuid.uuid4()),
    )


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

from contaxy.schema.deployment import JobInput
from pydantic import BaseModel, Field


class ScheduledJobInput(BaseModel):
    cron_string: str = Field(
        ...,
        description="The cron string that defines the schedule.",
        example="0 1 * * *",
    )
    job_input: JobInput = Field(
        ...,
        description="The job input that defines the job to be scheduled.",
    )


class ScheduledJob(ScheduledJobInput):
    job_id: str = Field(
        ...,
        description="The ID of the job.",
        example="8c99e85a-d7b8-4b4e-87a0-f582b840c52b",
    )
    created: str = Field(
        ...,
        description="The time the job was created in ISO format.",
        example="2022-09-29T11:39:52.441287",
    )
    last_run: str = Field(
        None,
        description="The last time the job was run in ISO format.",
        example="2022-09-29T11:39:52.441287",
    )
    next_run: str = Field(
        None,
        description="The next time the job will be run in ISO format.",
        example="2022-09-29T11:39:52.441287",
    )

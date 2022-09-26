from pydantic import BaseModel, Field
import uuid
import datetime


class ScheduledJobInput(BaseModel):
    cron_string: str = Field(
        ...,
        description="The cron string that defines the schedule.",
        example="0 1 * * *"
    )


class ScheduledJob(BaseModel):
    cron_string: str = Field(
        ...,
        description="The cron string that defines the schedule.",
        example="0 1 * * *"
    )
    job_id: str = Field(
        ...,
        description="The ID of the job.",
        example="8c99e85a-d7b8-4b4e-87a0-f582b840c52b"
    )
    last_run: str = Field(
        None,
        description="The last time the job was run.",
        example="2022-09-26 02:23:43.189446"
    )

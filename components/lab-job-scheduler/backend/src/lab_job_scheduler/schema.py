from pydantic import BaseModel, Field
from contaxy.schema.shared import MAX_DISPLAY_NAME_LENGTH
from contaxy.schema.shared import MIN_DISPLAY_NAME_LENGTH


class ScheduledJobInput(BaseModel):
    cron_string: str = Field(
        ...,
        description="The cron string that defines the schedule.",
        example="0 1 * * *"
    )
    container_image: str = Field(
        ...,
        example="mltooling/ml-workspace-minimal:latest",
        max_length=2000,
        description="The container image used for executing this job.",
    )
    display_name: str = Field(
        "Default Job",
        max_length=MAX_DISPLAY_NAME_LENGTH,
        description="A user-defined human-readable name of the job. The name can be up to 128 characters long and can consist of any UTF-8 character.",
    )


class ScheduledJob(ScheduledJobInput):
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

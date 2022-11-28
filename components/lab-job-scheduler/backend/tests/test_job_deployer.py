import datetime
import uuid

import pytest
from contaxy.schema.deployment import JobInput

import lab_job_scheduler.job_deployer as jd
from lab_job_scheduler.schema import ScheduledJob


@pytest.fixture
def job_input():
    return JobInput(container_image="hello-world", display_name="HelloWorld")


@pytest.fixture
def job_without_last_run(job_input: JobInput):
    return ScheduledJob(
        job_id=str(uuid.uuid4()),
        created=datetime.datetime(2021, 1, 1, 0, 0, 0).isoformat(),
        last_run=None,
        cron_string="0 1 * * *",
        job_input=job_input,
    )


@pytest.fixture
def job_with_last_run(job_input: JobInput):
    return ScheduledJob(
        job_id=str(uuid.uuid4()),
        created=datetime.datetime(2020, 12, 31, 0, 0, 0).isoformat(),
        last_run=datetime.datetime(2021, 1, 1, 1, 0, 0).isoformat(),
        cron_string="0 1 * * *",
        job_input=job_input,
    )


@pytest.mark.unit
class TestJobDeployerWithoutLastRun:
    def test_is_job_due_before_creation_of_job(
        self, job_without_last_run: ScheduledJob
    ):
        assert not jd.is_due(
            job_without_last_run,
            datetime.datetime(2020, 1, 1, hour=0, minute=0, second=0, microsecond=0),
        )

    def test_is_job_due_on_creation_of_job(self, job_without_last_run: ScheduledJob):
        assert not jd.is_due(
            job_without_last_run,
            datetime.datetime(2021, 1, 1, hour=0, minute=0, microsecond=0),
        )

    def test_is_job_due_before_next_scheduled_run_time(
        self, job_without_last_run: ScheduledJob
    ):
        assert not jd.is_due(
            job_without_last_run,
            datetime.datetime(2021, 1, 1, hour=0, minute=59, second=59, microsecond=0),
        )

    def test_is_job_due_at_next_scheduled_run_time(
        self, job_without_last_run: ScheduledJob
    ):
        assert jd.is_due(
            job_without_last_run,
            datetime.datetime(2021, 1, 1, hour=1, minute=0, second=0, microsecond=0),
        )

    def test_is_job_due_after_next_scheduled_run_time(
        self, job_without_last_run: ScheduledJob
    ):
        assert jd.is_due(
            job_without_last_run,
            datetime.datetime(2021, 1, 1, hour=1, minute=0, second=1, microsecond=0),
        )


@pytest.mark.unit
class TestJobDeployerWithLastRun:
    def test_is_job_due_at_last_run_time(self, job_with_last_run: ScheduledJob):
        assert not jd.is_due(
            job_with_last_run,
            datetime.datetime(2021, 1, 1, hour=1, minute=0, second=0, microsecond=0),
        )

    def test_is_job_due_before_creation_of_job(self, job_with_last_run: ScheduledJob):
        assert not jd.is_due(
            job_with_last_run,
            datetime.datetime(2020, 1, 1, hour=0, minute=0, second=0, microsecond=0),
        )

    def test_is_job_due_before_next_scheduled_run_time(
        self, job_with_last_run: ScheduledJob
    ):
        assert not jd.is_due(
            job_with_last_run,
            datetime.datetime(2021, 1, 2, hour=0, minute=59, second=59, microsecond=0),
        )

    def test_is_job_due_at_next_scheduled_run_time(
        self, job_with_last_run: ScheduledJob
    ):
        assert jd.is_due(
            job_with_last_run,
            datetime.datetime(2021, 1, 2, hour=1, minute=0, second=0, microsecond=0),
        )

    def test_is_job_due_after_next_scheduled_run_time(
        self, job_with_last_run: ScheduledJob
    ):
        assert jd.is_due(
            job_with_last_run,
            datetime.datetime(2021, 1, 2, hour=1, minute=0, second=1, microsecond=0),
        )

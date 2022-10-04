import pytest

import lab_job_scheduler.job_deployer as jd
from lab_job_scheduler.schema import ScheduledJob
from contaxy.schema.deployment import JobInput
import uuid
import datetime


@pytest.mark.unit
class TestJobDeployer:
    def test_run_scheduled_job(self):
        date = datetime.datetime(2021, 1, 1, hour=0, minute=0, second=0, microsecond=0)
        job_input = JobInput(container_image="hello-world", display_name="HelloWorld")
        job = ScheduledJob(
            job_id=str(uuid.uuid4()),
            created=date.isoformat(),
            last_run=None,
            cron_string="0 1 * * *",
            job_input=job_input
        )
        assert not jd.is_due(job, date)
        assert not jd.is_due(job, datetime.datetime(
            2020, 1, 1, hour=0, minute=0, second=0, microsecond=0))
        assert not jd.is_due(job, datetime.datetime(
            2021, 1, 1, hour=0, minute=59, second=59, microsecond=0))
        assert jd.is_due(job, datetime.datetime(
            2021, 1, 1, hour=1, minute=0, second=0, microsecond=0))
        assert jd.is_due(job, datetime.datetime(
            2021, 1, 1, hour=1, minute=0, second=1, microsecond=0))

    def test_run_job_that_has_run_before(self):
        created_date = datetime.datetime(
            2020, 12, 31, hour=0, minute=0, second=0, microsecond=0)
        last_run_date = datetime.datetime(
            2021, 1, 1, hour=1, minute=0, second=0, microsecond=0)
        job_input = JobInput(container_image="hello-world", display_name="HelloWorld")
        job = ScheduledJob(
            job_id=str(uuid.uuid4()),
            created=created_date.isoformat(),
            last_run=last_run_date.isoformat(),
            cron_string="0 1 * * *",
            job_input=job_input
        )
        assert not jd.is_due(job, last_run_date)
        assert not jd.is_due(job, datetime.datetime(
            2020, 1, 1, hour=0, minute=0, second=0, microsecond=0))
        assert not jd.is_due(job, datetime.datetime(
            2021, 1, 2, hour=0, minute=59, second=59, microsecond=0))
        assert jd.is_due(job, datetime.datetime(
            2021, 1, 2, hour=1, minute=0, second=0, microsecond=0))
        assert jd.is_due(job, datetime.datetime(
            2021, 1, 2, hour=1, minute=0, second=1, microsecond=0))

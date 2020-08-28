from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import logging
import time

from tqdm import tqdm

from lab_api.swagger_client import LabJob
from lab_api.swagger_client.rest import ApiException
from lab_client.handler import lab_api_handler


class LabJobConfig:
    """
    Job configuration for deployment.

    # Arguments
        image (string): Docker image for the job.
        name (string): Name of the job (optional).
        params (dict): Environment variables passed to the job (optional).
    """

    def __init__(self, image: str, name: str = None, params: dict = None):
        # Initialize logger
        self.log = logging.getLogger(__name__)

        # Initialize variables
        self.image = image
        self.name = name
        self.params = params


class LabJobHandler:
    def __init__(self, env, check_wait_time: int = 10):
        # Initialize logger
        self.log = logging.getLogger(__name__)

        # Initialize variables
        self.env = env
        self.check_wait_time = check_wait_time

    def _lab_handler(self) -> lab_api_handler.LabApiHandler:
        return self.env.lab_handler

    def run_jobs(self, job_configs: list, check_status: bool = True, parallel: bool = False):
        """
        Run multiple jobs in pipeline or parallel and waits for it to finish.

        # Arguments
            job_configs (list): List of job configurations for deployment.
            check_status (bool): If 'True', this method check the job status and wait until it is finished (optional).
            parallel (bool): If `True`, all jobs will be run in parallel instead of as a pipeline (optional)

        # Returns
        'True', if jobs have finished successfully. 'False', if job has failed.
        """
        if not parallel and not check_status:
            self.log.error("Cannot run as pipeline (not parallel) without checking the status.")
            return False

        if not parallel:
            for job_config in job_configs:
                if not self.run_job(job_config, True):
                    return False
            return True

        running_jobs = []
        for job_config in job_configs:
            deployed_job = self._deploy_job(job_config)
            if deployed_job:
                running_jobs.append(deployed_job)

        if check_status:
            self.check_jobs_status(running_jobs)

        return True

    def run_job(self, job_config: LabJobConfig, check_status: bool = True) -> bool:
        """
        Run a job and waits for it to finish.

        # Arguments
            job_config (LabJobConfig): Lab job configuration for deployment.
            check_status (bool): If 'True', this method check the job status and wait until it is finished (optional).

        # Returns
        'True', if job has finished successfully. 'False', if job has failed.
        """
        deployed_job = self._deploy_job(job_config)

        if not deployed_job:
            return False

        if check_status:
            return self.check_job_status(deployed_job)

        return True

    def get_job(self, job_id: str) -> LabJob or None:
        """
        Returns the job for the given Job ID

        # Arguments
            job_id (string): Job Docker ID or Name.

        # Returns
        'LabJob' if job was found or 'None'.
        """

        deployed_job_status = self._lab_handler().lab_api.get_job(project=self.env.project,
                                                                  job=job_id)
        if deployed_job_status is None or not deployed_job_status.data \
                or not self._lab_handler().request_successful(deployed_job_status):
            self.log.info("Failed to get job: " + job_id)
            return None

        return deployed_job_status.data

    def check_job_status(self, job: LabJob):
        return self.check_jobs_status([job])

    def check_jobs_status(self, jobs: list):
        if len(jobs) == 1:
            pbar = tqdm(desc="Running job: " + jobs[0].name, unit="check")
        else:
            jobs_str = ""
            for job in jobs:
                jobs_str += job.name + "; "
            pbar = tqdm(desc="Running jobs: " + jobs_str, unit="check")

        running_jobs = jobs

        successful = True
        while len(running_jobs) > 0:
            time.sleep(self.check_wait_time)  # Wait X seconds for every check
            pbar.update()

            jobs_to_check = running_jobs
            for job in jobs_to_check:
                deployed_job_status = None
                try:
                    deployed_job_status = self.get_job(job.docker_name)
                except ApiException as ex:
                    self.log.warning("Failed to get job " + job.name, exc_info=ex)

                if deployed_job_status is None:
                    self.log.info("Failed to check if " + job.name + " is running.")
                    running_jobs.remove(job)

                if deployed_job_status.status.lower() == "running":
                    continue
                elif deployed_job_status.status.lower() == "succeeded":
                    self.log.info(job.name + " has finished successfully.")
                    running_jobs.remove(job)
                else:
                    successful = False
                    self.log.info(job.name + " has failed with status: " + str(deployed_job_status.status))
                    running_jobs.remove(job)
                    try:
                        job_logs = self._lab_handler().lab_api.get_job_logs(job.docker_name)
                        if job_logs and job_logs.data:
                            self.log.info("Last log output of " + job.name + ": " + str(
                                job_logs.data[-min(job_logs.data, 1000):]))
                    except:
                        pass

        pbar.close()
        return successful

    def _deploy_job(self, job_config: LabJobConfig):
        args = {}
        if job_config.name:
            args["name"] = job_config.name

        if job_config.params:
            args["body"] = job_config.params

        if not job_config.image:
            self.log.error("No image provided for the job.")
            return None

        deployed_job = self._lab_handler().lab_api.deploy_job(project=self.env.project, image=job_config.image,
                                                              **args)
        if deployed_job is None or not deployed_job.data or not self._lab_handler().request_successful(deployed_job):
            self.log.info("Failed to deploy the job.")
            return None

        self.log.info("Successfully deployed the job with id: " + str(deployed_job.data.docker_id)
                      + "; Parameters: " + str(job_config.params))

        return deployed_job.data

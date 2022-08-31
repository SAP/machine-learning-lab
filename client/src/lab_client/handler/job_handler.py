from contaxy.clients.deployment import DeploymentClient
from contaxy.schema import Job, JobInput, ResourceAction
from datetime import datetime

from typing import Dict, Iterator, List, Optional
from contaxy.schema.shared import ResourceActionExecution

from loguru import logger
from tqdm import tqdm
import time


class JobHandler:
    def __init__(self, env, deployment_client: DeploymentClient, check_wait_time: int = 10):
        # Initialize variables
        self.env = env
        self.deployment_client = deployment_client
        self.check_wait_time = check_wait_time
        if not self.env.is_connected():
            raise RuntimeError("Environment is not connected to Lab!")

    def list_jobs(self) -> List[Job]:
        """List all jobs under the current project

        Returns:
            List[Job]: List of all jobs.
        """
        logger.info('Listing all jobs under project : ' + self.env.project)
        jobs_list = self.deployment_client.list_jobs(
            project_id=self.env.project
        )
        logger.info('Found ' + str(len(jobs_list)) + ' jobs!')
        return jobs_list

    def deploy_job(
        self,
        job_input: JobInput,
        action_id: Optional[str] = None,
        wait: bool = False) -> str:
        """Deploys a job under a specific project.

        Args:
            job_input (JobInput): Job input with their container-image and display-name.
            action_id (Optional[str], optional): The ID of the action. Defaults to None.
            wait (bool, optional): If 'True', the return will be until job completion.. Defaults to False.

        Returns:
            str: Job ID.
        """

        logger.info('Deploying job ' + job_input.display_name + ' under project : ' + self.env.project)
        deployed_job = self.deployment_client.deploy_job(
            project_id=self.env.project,
            job_input=job_input,
            action_id=action_id
        )
        if deployed_job is None:
            logger.info('Job ' + job_input.display_name + ' could not be deployed successfully!')
            return None

        if wait:
            return deployed_job.id if self.wait_for_job_completion(deployed_job.id) else None

        return deployed_job.id

    def deploy_jobs(
        self,
        job_inputs: List[JobInput],
        action_id: Optional[str] = None,
        wait: bool = False) -> List[str]:
        """Deploys multiple jobs under a specific project.

        Args:
            job_inputs (List[JobInput]): The list of job inputs with their container-image and display-name.
            action_id (Optional[str], optional): The ID of the action. Defaults to None.
            wait (bool, optional): If 'True', the return will be until job completion. Defaults to False.

        Returns:
            List[str]: The Job IDs of all jobs.
        """
        all_jobs = []
        logger.info('Deploying ' + len(job_inputs) + ' jobs under project : ' + self.env.project)
        for job_input in job_inputs:
            deployed_job_id = self.deploy_job(
                job_input=job_input,
                action_id=action_id,
                wait=wait
            )
            if deployed_job_id:
                all_jobs.append(deployed_job_id)
        logger.info('Successfully deployed ' + len(all_jobs) + ' jobs!')
        return all_jobs

    def get_job_metadata(self, job_id: str) -> Dict:
        """
        Returns the job metadata for the given Job ID
        # Arguments
            job_id (string): Job Docker ID or Name.
        # Returns
        'Metadata dict' if job was found or 'None'.
        """
        logger.info('Fetching metadata of job ' + job_id)
        job = self.deployment_client.get_job_metadata(
            project_id=self.env.project,
            job_id=job_id
        )
        if job:
            logger.info('Fetching job metadata!')
            return job.metadata
        return None

    def check_job_status(self, job_id: str) -> str:
        """Check job status.

        Args:
            job_id (str): Job ID.

        Returns:
            str: Status of job
        """
        logger.info('Checking status of job ' + job_id)
        job = self.deployment_client.get_job_metadata(
            project_id=self.env.project,
            job_id=job_id
        )
        if job:
            logger.info('Fetching job status!')
            return job.status
        return None

    def wait_for_job_completion(self, job_id: str) -> bool:
        """Wait for job completion.

        Args:
            job_id (str): Job ID.

        Returns:
            bool: Job successfully run or not.
        """
        return self.wait_for_jobs_completion([job_id])

    def wait_for_jobs_completion(self, job_ids: List[str]) -> bool:
        """Wait for job completion.

        Args:
            job_id (str): Job ID.

        Returns:
            bool: Job successfully run or not.
        """
        if len(job_ids) == 1:
            pbar = tqdm(desc="Running job: " + job_ids[0], unit="check")
        else:
            jobs_str = ""
            for job in job_ids:
                jobs_str += job + "; "
            pbar = tqdm(desc="Running jobs: " + jobs_str, unit="check")

        successful = True
        running_jobs = job_ids
        while len(running_jobs) > 0:
            time.sleep(self.check_wait_time)  # Wait X seconds for every check
            pbar.update()

            jobs_to_check = running_jobs
            for job in jobs_to_check:
                deployed_job_status = None

                deployed_job_status = self.check_job_status(job_id=job)

                if deployed_job_status is None:
                    logger.info("Failed to check if " + job + " is running.")
                    running_jobs.remove(job)

                if deployed_job_status.lower() == "running":
                    logger.info("Job " + job + " is still running!")
                    continue
                elif deployed_job_status.lower() == "succeeded":
                    logger.info("Job " + job + " has finished successfully.")
                    running_jobs.remove(job)
                else:
                    successful = False
                    logger.info("Job " + job + " has failed with status: " + str(deployed_job_status))
                    running_jobs.remove(job)
                    try:
                        job_logs = self.get_job_logs(job_id=job)
                        if job_logs:
                            logger.info("Last log output of " + job + ": " + str(
                                job_logs[-min(len(job_logs), 1000):]))
                    except:
                        pass

        pbar.close()
        return successful

    def delete_job(self, job_id: str) -> None:
        """Delete a specific job

        Args:
            job_id (str): Job ID.
        """
        logger.info('Deleting job ' + job_id)
        self.deployment_client.delete_job(
            project_id=self.env.project,
            job_id=job_id
        )

    def delete_jobs(self) -> None:
        """Deletes all jobs of a project.
        """
        self.deployment_client.delete_jobs(
            project_id=self.env.project
        )

    def get_job_logs(
        self,
        job_id: str,
        lines: Optional[int] = None,
        since: Optional[datetime] = None) -> str:
        """Get the logs of a job.

        Args:
            job_id (str): Job ID.
            lines (Optional[int], optional): The number of lines in the logs to be returned. Defaults to None.
            since (Optional[datetime], optional): The date time. Defaults to None.

        Returns:
            str: Logs as a string
        """
        return self.deployment_client.get_job_logs(
            project_id=self.env.project,
            job_id=job_id,
            lines=lines,
            since=since
        )

    def list_deploy_job_actions(
        self,
        job_input: JobInput
    ) -> List[ResourceAction]:
        """Get job actions.

        Args:
            job_input (JobInput): Job Input.

        Returns:
            List[ResourceAction]: List of actions.
        """
        return self.deployment_client.list_deploy_job_actions(
            project_id=self.env.project,
            job=job_input
        )

    def suggest_job_config(
        self,
        container_image: str,
    ) -> JobInput:
        """Get the Job Config.

        Args:
            container_image (str): Image

        Returns:
            JobInput: Job input configuration.
        """
        return self.deployment_client.suggest_job_config(
            project_id=self.env.project,
            container_image=container_image
        )

    def list_job_actions(
        self,
        job_id: str
    ) -> List[ResourceAction]:
        """List job actions.

        Args:
            job_id (str): Job ID.

        Returns:
            List[ResourceAction]: List of actions.
        """
        return self.deployment_client.list_job_actions(
            project_id=self.env.project,
            job_id=job_id
        )

    def execute_job_action(
        self,
        job_id: str,
        action_id: str,
        action_execution: ResourceActionExecution = ResourceActionExecution()
    ) -> None:
        # TODO: Implement this method after the backend changes
        raise NotImplementedError

"""
Script to delete all containers that match the label filter defined by the environment variable LABEL_FILTER. 
LABEL_FILTER: environment variable by which containers are filted. Must be in the form of LABEL_FILTER='foo=bar,foo2=bar2'.
CLEAN_MODE: determine which clean functions to execute. One of 'storage | exited'

Currently implemented:
- disk storage check: check for containers that are too big.
  Environment variables:
  - MAX_CONTAINER_SIZE: the container size (ephemeral storage) in GB that a container is not allowed to exceed
- exited containers: check for containers that are exited for a time longer than MAX_AGE
  Environment variables:
  - MAX_AGE: the container age (based on FinishedAt). If not provided, its considered to be a dry run! Default unit is seconds, can be suffixed with "d", "h", and "m" for days, hours, and minutes, respectively.
  - DRY_RUN: if true, containers that would be removed are listed but not actually deleted.
"""

import os
import sys
import datetime
import logging
import json
import docker
from docker.utils import kwargs_from_env
# from kubernetes import client, config, stream

ENV_NAME_EXECUTION_MODE = "EXECUTION_MODE"
EXECUTION_MODE_LOCAL = "local"
# EXECUTION_MODE_KUBERNETES = "k8s"

max_container_size = int(os.environ.get("MAX_CONTAINER_SIZE", -1))
docker_api_client = docker.APIClient()

label_filter = os.getenv("LABEL_FILTER")

ENV_DRY_RUN = os.environ.get("DRY_RUN", False)

execution_mode = os.environ.get(ENV_NAME_EXECUTION_MODE, EXECUTION_MODE_LOCAL)
if execution_mode == EXECUTION_MODE_LOCAL:
    docker_client_kwargs = json.loads(os.environ.get("DOCKER_CLIENT_KWARGS", '{}'))
    docker_tls_kwargs = json.loads(os.environ.get("DOCKER_TLS_CONFIG", '{}'))

    kwargs = {"version": "auto"}
    if docker_tls_kwargs:
        kwargs["tls"] = docker.tls.TLSConfig(**docker_tls_kwargs)
    kwargs.update(kwargs_from_env())
    if docker_client_kwargs:
        kwargs.update(docker_client_kwargs)

    docker_client = docker.DockerClient(**kwargs)
    docker_api_client = docker.APIClient(**kwargs)
# elif execution_mode == EXECUTION_MODE_KUBERNETES:
#     # incluster config is the config given by a service account and it's role permissions
#     config.load_incluster_config()
#     kubernetes_client = client.CoreV1Api()

if label_filter is None:
    print("The environment variable $LABEL_FILTER has to be set; e.g. `--env LABEL_FILTER='foo=bar,foo2=bar2'`.")
    sys.exit(-1)

def clean_storage_exceeding_containers():
    """Remove containers which exceeds the max container size defined by $MAX_CONTAINER_SIZE
    """

    if max_container_size == -1:
        logging.info("The environment variable MAX_CONTAINER_SIZE was not set.")
        return

    container_size_field = "SizeRw"
    containers = docker_api_client.containers(all=True, size=True, filters={"label": label_filter})
    for container in containers:
        if container_size_field in container:
            container_size_in_gb = container[container_size_field]/1000/1000/1000
            container_id = container["Id"]

            try:
                if max_container_size < container_size_in_gb:
                    logging.info("Delete storage exceeding container " + container["Names"][0])
                    docker_api_client.remove_container(container_id, force=True)

            except docker.errors.APIError as e:
                logging.error("Could not remove / re-create the container.", e)

def get_max_age_in_seconds():
    max_age = os.environ.get("MAX_AGE", "-1")

    max_age_in_seconds = None
    if "d" in max_age:
        max_age_in_seconds = int(max_age.replace("d", "").strip()) * 24 * 60 * 60
    elif "h" in max_age:
        max_age_in_seconds = int(max_age.replace("h", "").strip()) * 60 * 60
    elif "m" in max_age:
        max_age_in_seconds = int(max_age.replace("m", "").strip()) * 60
    else:
        max_age_in_seconds = int(max_age)

    return max_age_in_seconds

def clean_stopped_containers():
    # TODO:
    # label: studio.feature.type=project-job
    # containers = docker_api_client.containers(all=True, filters={"label": label_filter})
    is_dry_run = True if str(ENV_DRY_RUN).lower() == "true" else False
    max_age_in_seconds = get_max_age_in_seconds()
    containers = docker_client.containers.list(all=True, filters={"label": label_filter, "status": "exited"})
    print('Found {} containers to remove.'.format(str(len(containers))))
    for container in containers:
        finished_at = container.attrs['State']['FinishedAt']
        finished_at_in_seconds = datetime.datetime.strptime(finished_at.split('.', 1)[0], "%Y-%m-%dT%H:%M:%S").timestamp()
        if  (datetime.datetime.now().timestamp() - finished_at_in_seconds) > max_age_in_seconds:

            if is_dry_run or max_age_in_seconds == -1:
                print("DryRun. Would remove container: " + container.id)
                continue

            print("Remove container: " + container.id)
            container.remove()

clean_mode = os.environ.get("CLEAN_MODE", "").split(",")
if len(clean_mode) == 1 and clean_mode[0] == '':
    print("Provide which functions to execute, e.g. via CLEAN_MODE='exited'")

if "storage" in clean_mode:
    clean_storage_exceeding_containers()
elif "exited" in clean_mode:
    clean_stopped_containers()

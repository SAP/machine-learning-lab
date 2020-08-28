#!/usr/bin/python

"""
Configure and run custom scripts
"""

from subprocess import call
import os
import sys
import logging

logging.basicConfig(stream=sys.stdout, format='%(asctime)s : %(levelname)s : %(message)s', level=logging.INFO)
log = logging.getLogger(__name__)

ENV_RESOURCES_PATH = os.getenv("RESOURCES_PATH", "/resources")
ENV_WORKSPACE_HOME = os.getenv("WORKSPACE_HOME", "/workspace")

log.info("Running Lab Workspace Custom Script")

# start backup process
call("python " + ENV_RESOURCES_PATH + "/scripts/lab-backup-restore.py schedule", shell=True)
call("python " + ENV_RESOURCES_PATH + "/scripts/storage-cleanup.py schedule", shell=True)

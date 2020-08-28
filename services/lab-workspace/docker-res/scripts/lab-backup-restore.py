# System libraries
from __future__ import absolute_import, division, print_function

import argparse
import logging
import os
import random
import sys
import time

# Enable logging
logging.basicConfig(
    format='%(asctime)s [%(levelname)s] %(message)s', 
    level=logging.INFO, 
    stream=sys.stdout)

log = logging.getLogger(__name__)

# Lab Libraries
from lab_client.utils import backup_utils

parser = argparse.ArgumentParser()
parser.add_argument('mode', type=str, default="backup", help='Either backup or restore the workspace.',
                    choices=["backup", "restore", "schedule"])

args, unknown = parser.parse_known_args()
if unknown:
    log.info("Unknown arguments " + str(unknown))

WORKSPACE_HOME = os.getenv('WORKSPACE_HOME')
RESOURCE_FOLDER = os.getenv('RESOURCES_PATH')
LAB_BACKUP = os.getenv('LAB_BACKUP')

if args.mode == "restore":
    if LAB_BACKUP is None or LAB_BACKUP.lower() == "false" or LAB_BACKUP.lower() == "off":
        log.info("Lab Backup is not activated. Restore process will not be started.")
        sys.exit()

    log.info("Starting backup restore.")
    backup_utils.restore_backup(WORKSPACE_HOME)
elif args.mode == "backup":
    log.info("Starting folder backup.")
    # Wait for random time (up to 1 hour) so that not all workspaces backup at the same time
    wait_minutes = random.randint(0, 60)
    log.info("Waiting for " + str(wait_minutes) + " minutes.")
    time.sleep(wait_minutes * 60)
    backup_utils.backup_folder(WORKSPACE_HOME, RESOURCE_FOLDER)
elif args.mode == "schedule":
    DEFAULT_CRON = "0 4 * * *"  # every day at 4

    if LAB_BACKUP is None or LAB_BACKUP.lower() == "false" or LAB_BACKUP.lower() == "off":
        log.info("LAB Backup is not activated.")
        sys.exit()

    from crontab import CronTab, CronSlices

    cron_schedule = DEFAULT_CRON

    if CronSlices.is_valid(LAB_BACKUP):
        cron_schedule = LAB_BACKUP

    # Cron does not provide enviornment variables, source them manually
    environment_file = os.path.join(RESOURCE_FOLDER, "environment.sh")
    with open(environment_file, 'w') as fp:
        for env in os.environ:
            if env != "LS_COLORS":
                fp.write("export " + env + "=\"" + os.environ[env] + "\"\n")

    os.chmod(environment_file, 0o777)

    script_file_path = os.path.realpath(__file__)
    command = ". " + environment_file + "; " + sys.executable + " '" + script_file_path + "' backup> /proc/1/fd/1 2>/proc/1/fd/2"

    cron = CronTab(user=True)

    # remove all other backup tasks
    cron.remove_all(command=command)

    job = cron.new(command=command)
    if CronSlices.is_valid(cron_schedule):
        log.info("Scheduling cron backup task with with cron: " + cron_schedule)
        job.setall(cron_schedule)
        job.enable()
        cron.write()
    else:
        log.info("Failed to schedule backup. Cron is not valid.")

    log.info("Running cron jobs:")
    for job in cron:
        log.info(job)

# System libraries
from __future__ import absolute_import, division, print_function

import argparse
import logging
import os
import random
import sys
import time
import json
from datetime import datetime

from jupyterdiskcheck import jupyterdiskcheck_plugin

# Enable logging
logging.basicConfig(
    format='%(asctime)s [%(levelname)s] %(message)s', 
    level=logging.INFO, 
    stream=sys.stdout)

log = logging.getLogger(__name__)

parser = argparse.ArgumentParser()
parser.add_argument('mode', type=str, default="clean", help='Automatically clean workspace storage if user is inactive.',
                    choices=["clean", "schedule"])

args, unknown = parser.parse_known_args()
if unknown:
    log.info("Unknown arguments " + str(unknown))

WORKSPACE_HOME = os.environ.get("WORKSPACE_HOME")
RESOURCE_FOLDER = os.getenv('RESOURCES_PATH')
WORKSPACE_STORAGE_LIMIT = os.environ.get("WORKSPACE_STORAGE_LIMIT")
WORKSPACE_CONFIG_FOLDER = os.path.join(WORKSPACE_HOME, ".workspace")

MAX_FILE_SIZE_MB = 200
LAST_FILE_USAGE = 10 # in days
LAST_USER_ACTIVITY = 10 # in days
STORAGE_CLEANUP_THRESHOLD = 0.5 # percentage of max storage

if args.mode == "clean":
    if WORKSPACE_STORAGE_LIMIT != None:
        try:
            # Wait for random time (up to 1 hour) so that not all workspaces check at the same time
            time.sleep(random.randint(0, 60) * 60)
    
            log.info("Run storage cleanup check.")
            max_disk_storage_gb = int(WORKSPACE_STORAGE_LIMIT)
            inactive_days = jupyterdiskcheck_plugin.get_inactive_days()
            size_in_gb = jupyterdiskcheck_plugin.get_workspace_size()

            if inactive_days <= 1:
                # Backup workspace metadata if user is active -> used in Lab for tracking of activity
                try:
                    from lab_client import Environment
                    env = Environment(project=None, root_folder=Environment._TEMP_ROOT_FOLDER)
                    # Only backup if environment is connected
                    if not env.is_connected():
                        log.warning("Failed to connect to Lab Instance. Cannot upload metadata backup file.")
                        env.print_info()
                    else:
                        env.upload_file(os.path.join(WORKSPACE_CONFIG_FOLDER , "metadata.json"), data_type=env.DataType.BACKUP, track_event=False)
                except Exception as e:
                    # Failsafe backup
                    print("Failed to backup workspace metadata.")
                    print(e)
                    pass

            # only use inactive cleanup if more than 50% of actual limit
            if size_in_gb and size_in_gb > (max_disk_storage_gb * STORAGE_CLEANUP_THRESHOLD) and inactive_days and inactive_days > LAST_USER_ACTIVITY:
                # Automatic cleanup
                log.info("Automatic storage cleanup. Workspace size: " + str(round(size_in_gb)) + " GB. "
                    "Max size: " + str(max_disk_storage_gb) + " GB. Last activity: " + str(inactive_days) + " days ago.")
                try:
                    from lab_client import Environment
                    Environment().cleanup(max_file_size_mb=MAX_FILE_SIZE_MB, last_file_usage=LAST_FILE_USAGE)
                except Exception as ex:
                    log.info("Failed to cleanup enviornment", ex)
                    # TODO: Do not do a full workspace cleanup -> bad side effects
                    # Fallback - clean full workspace folder
                    # cleanup_folder(WORKSPACE_HOME, max_file_size_mb=MAX_FILE_SIZE_MB, last_file_usage=LAST_FILE_USAGE)
                jupyterdiskcheck_plugin.update_workspace_metadata()
        except:
            pass

elif args.mode == "schedule":
    DEFAULT_CRON = "0 3 * * *"  # every day at 3

    if WORKSPACE_STORAGE_LIMIT is None:
        log.info("Storage cleanup is not activated.")
        sys.exit()

    from crontab import CronTab, CronSlices

    cron_schedule = DEFAULT_CRON

    # Cron does not provide enviornment variables, source them manually
    environment_file = os.path.join(RESOURCE_FOLDER, "environment.sh")
    with open(environment_file, 'w') as fp:
        for env in os.environ:
            if env != "LS_COLORS":
                fp.write("export " + env + "=\"" + os.environ[env] + "\"\n")

    os.chmod(environment_file, 0o777)

    script_file_path = os.path.realpath(__file__)
    command = ". " + environment_file + "; " + sys.executable + " '" + script_file_path + "' clean> /proc/1/fd/1 2>/proc/1/fd/2"

    cron = CronTab(user=True)

    # remove all other backup tasks
    cron.remove_all(command=command)

    job = cron.new(command=command)
    if CronSlices.is_valid(cron_schedule):
        log.info("Scheduling storage cleaning task with with cron: " + cron_schedule)
        job.setall(cron_schedule)
        job.enable()
        cron.write()
    else:
        log.info("Failed to schedule storage cleaning. Cron is not valid.")

    log.info("Running cron jobs:")
    for job in cron:
        log.info(job)

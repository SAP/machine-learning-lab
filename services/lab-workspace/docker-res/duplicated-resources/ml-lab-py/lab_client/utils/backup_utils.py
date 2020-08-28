"""Functionality to backup a folder to Lab."""

from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import logging
import os
import time

from lab_client import Environment
from lab_client.commons import file_utils
from lab_client.utils import file_handler_utils

KEEP_N_LATEST_BACKUPS = 3
MAX_FILE_SIZE_IN_MB = 50

log = logging.getLogger(__name__)


def backup_folder(folder_to_backup: str, cache_folder: str):
    """Backup a folder to the user project."""

    if not os.path.isdir(folder_to_backup):
        log.warning("Folder does not exist: " + folder_to_backup)

    # heck if dir size has changed
    metadata_dict = {
        "folder_size": 0,
        "last_backup": 0,
        "folder": folder_to_backup
    }

    metadata_file = os.path.join(cache_folder, os.path.basename(folder_to_backup) + ".json")
    if os.path.isfile(metadata_file):
        metadata_dict = file_utils.load_dict_json(metadata_file)

    current_folder_size = file_utils.folder_size(folder_to_backup)
    if metadata_dict["folder_size"] == current_folder_size:
        log.info("No Backup since folder size has not changed.")
        return

    metadata_dict["folder_size"] = current_folder_size
    metadata_dict["last_backup"] = int(round(time.time() * 1000))

    # Initialize environment with user project and in temp directory
    env = Environment(project=None, root_folder=Environment._TEMP_ROOT_FOLDER)

    # Only backup if environment is connected
    if not env.is_connected():
        log.warning("Failed to connect to Lab Instance. Cannot backup folder.")
        env.print_info()
        return

    archive_file_path = None
    backup_key = None

    try:
        # If so, package folder to temp dir but ignore files with more than 50 MB and environment directory
        archive_file_path = file_handler_utils.tar_folder(folder_to_backup, max_file_size=MAX_FILE_SIZE_IN_MB,
                                                      exclude=["**/environment/*"])
        backup_key = env.upload_file(archive_file_path, data_type=env.DataType.BACKUP, track_event=False)
    except Exception as e:
        # Failsafe backup
        print("Failed to backup workspace")
        print(e)
        pass

    if archive_file_path:
        os.remove(archive_file_path)  # remove zip file after upload

    if backup_key:
        print(backup_key)
        # Backup successful
        # Save folder metadata
        file_utils.save_dict_json(metadata_file, metadata_dict)

        # 4. delete backups, keep 3 latest versions
        env.file_handler.delete_remote_file(env.file_handler.remove_version_from_key(backup_key),
                                            keep_latest_versions=KEEP_N_LATEST_BACKUPS)


def restore_backup(folder_to_restore: str):
    """Restore a folder from a backup."""

    if not os.path.exists(folder_to_restore):
        os.makedirs(folder_to_restore)

    # Check if folder is empty
    if os.listdir(folder_to_restore):
        log.info("Folder " + folder_to_restore + " is not empty, will not attempt to restore backup.")
        return

    log.info("Folder " + folder_to_restore + " is empty, will try to restore backup.")

    # Initialize environment with user project and in temp directory
    env = Environment(project=None, root_folder=Environment._TEMP_ROOT_FOLDER)

    # 2. If so, download latest backup and unpack into specified folder
    backup_key = "backups/" + os.path.basename(folder_to_restore) + ".tar"
    file_path = env.get_file(backup_key)
    if file_path:
        file_handler_utils.extract_tar(file_path=file_path, unpack_path=folder_to_restore, remove_if_exists=False)
    else:
        log.warning("Failed to get backup with key " + backup_key)

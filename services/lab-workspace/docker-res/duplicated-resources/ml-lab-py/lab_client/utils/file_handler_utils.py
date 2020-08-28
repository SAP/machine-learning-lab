"""Utilities for file handler operation."""

from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import atexit
import logging
import os
import re
import shutil
import sys
import tarfile
import tempfile
import zipfile
from datetime import datetime

import requests
import tqdm

from lab_client.commons import file_utils, system_utils
from lab_client.commons.request_utils import url2filename

log = logging.getLogger(__name__)


def cleanup_folder(folder_path: str, max_file_size_mb: int = 50, last_file_usage: int = 3,
                   replace_with_info: bool = True, excluded_folders: list = None):
    """
    Cleanup folder to reduce disk space usage.

    # Arguments
        folder_path (str): Folder that should be cleaned.
        max_file_size_mb (int): Max size of files in MB that should be deleted. Default: 50.
        replace_with_info (bool): Replace removed files with `.removed.txt` files with file removal reason. Default: True.
        last_file_usage (int): Number of days a file wasn't used to allow the file to be removed. Default: 3.
        excluded_folders (list[str]): List of folders to exclude from removal (optional)
    """
    total_cleaned_up_mb = 0
    removed_files = 0

    for dirname, subdirs, files in os.walk(folder_path):
        if excluded_folders:
            for excluded_folder in excluded_folders:
                if excluded_folder in subdirs:
                    log.debug("Ignoring folder because of name: " + excluded_folder)
                    subdirs.remove(excluded_folder)
        for filename in files:
            file_path = os.path.join(dirname, filename)

            file_size_mb = int(os.path.getsize(file_path) / (1024.0 * 1024.0))
            if max_file_size_mb and max_file_size_mb > file_size_mb:
                # File will not be deleted since it is less than the max size
                continue

            last_file_usage_days = None
            if file_utils.get_last_usage_date(file_path):
                last_file_usage_days = (datetime.now() - file_utils.get_last_usage_date(file_path)).days

            if last_file_usage_days and last_file_usage_days <= last_file_usage:
                continue

            current_date_str = datetime.now().strftime("%B %d, %Y")
            removal_reason = "File has been removed during folder cleaning (" + folder_path + ") on " + current_date_str + ". "
            if file_size_mb and max_file_size_mb:
                removal_reason += "The file size was " + str(file_size_mb) + " MB (max " + str(max_file_size_mb) + "). "

            if last_file_usage_days and last_file_usage:
                removal_reason += "The last usage was " + str(last_file_usage_days) + " days ago (max " + str(
                    last_file_usage) + "). "

            log.info(filename + ": " + removal_reason)

            # Remove file
            try:
                os.remove(file_path)

                if replace_with_info:
                    with open(file_path + ".removed.txt", "w") as file:
                        file.write(removal_reason)

                if file_size_mb:
                    total_cleaned_up_mb += file_size_mb

                removed_files += 1

            except Exception as e:
                log.info("Failed to remove file: " + file_path, e)

    log.info("Finished cleaning. Removed " + str(removed_files) + " files with a total disk space of " + str(
        total_cleaned_up_mb) + " MB.")


def extract_zip(file_path: str, unpack_path: str = None, remove_if_exists: bool = False) -> str or None:
    """
    Unzip a file.

    # Arguments
        file_path (string): Path to zipped file.
        unpack_path (string): Path to unpack the file (optional)
        remove_if_exists (bool): If `True`, the directory will be removed if it already exists (optional)

    # Returns
    Path to the unpacked folder or `None` if unpacking failed.
    """
    if not os.path.exists(file_path):
        log.warning(file_path + " does not exist.")
        return None

    if not zipfile.is_zipfile(file_path):
        log.warning(file_path + " is not a zip file.")
        return None

    if not unpack_path:
        unpack_path = os.path.join(os.path.dirname(file_path), os.path.splitext(os.path.basename(file_path))[0])

    if os.path.isdir(unpack_path):
        log.info("Unpack directory already exists " + unpack_path)

        if not os.listdir(unpack_path):
            log.info("Directory is empty. Unpacking...")
            shutil.rmtree(unpack_path)
        elif remove_if_exists:
            log.info('Removing existing unpacked dir: ' + unpack_path)
            shutil.rmtree(unpack_path)
        else:
            return unpack_path

    log.info("Unpacking file " + os.path.basename(file_path) + " to: " + unpack_path)
    zip_ref = zipfile.ZipFile(file_path, 'r')
    zip_ref.extractall(unpack_path)
    zip_ref.close()

    if not os.path.exists(unpack_path):
        log.warning("Failed to extract zip file: " + file_path)

    return unpack_path


def zip_folder(folder_path: str, archive_file_name: str = None, max_file_size: int = None,
               excluded_folders: list = None, compression: int = zipfile.ZIP_STORED) -> str or None:
    """
    Zip a folder (via `zipfile`). The folder will be zipped to an archive file in a temp directory.

    # Arguments
        folder_path (string): Path to the folder.
        archive_file_name (string): Name of the resulting zip package file (optional)
        max_file_size (bool): Max file size in `MB` to be included in the archive (optional)
        excluded_folders (list[str]): List of folders to exclude from the archive (optional)
        compression (int): Compression mode. Please see the `zipfile` documentation for supported compression modes (optional)

    # Returns
    Path to the zipped archive file or `None` if zipping failed.
    """
    # TODO accept names with wildcards in exclude like for tar
    if not os.path.isdir(folder_path):
        log.info("Failed to zip (not a directory): " + folder_path)
        return None

    temp_folder = tempfile.mkdtemp()

    if max_file_size:
        max_file_size = max_file_size * 1000000  # MB ro bytes

    def cleanup():
        log.info("Removing temp dir: " + temp_folder)
        shutil.rmtree(temp_folder)
        log.info("Temp directory removed")

    atexit.register(cleanup)

    if not archive_file_name:
        archive_file_name = os.path.basename(folder_path) + ".zip"

    zip_file_path = os.path.join(temp_folder, archive_file_name)
    log.debug("Zipping folder: " + folder_path + " to " + zip_file_path)
    zip_file = zipfile.ZipFile(zip_file_path, "w", compression)

    # dont packge folder inside, only package everything inside folder
    for dirname, subdirs, files in os.walk(folder_path):
        if excluded_folders:
            for excluded_folder in excluded_folders:
                if excluded_folder in subdirs:
                    log.debug("Ignoring folder because of name: " + excluded_folder)
                    subdirs.remove(excluded_folder)
        if dirname != folder_path:
            # only write if dirname is not the root folder
            zip_file.write(dirname, os.path.relpath(dirname, folder_path))
        for filename in files:
            if max_file_size and max_file_size < os.path.getsize(os.path.join(dirname, filename)):
                # do not write file if it is bigger than
                log.debug("Ignoring file because of file size: " + filename)
                continue
            file_path = os.path.join(dirname, filename)
            zip_file.write(file_path, os.path.relpath(file_path, folder_path))
    zip_file.close()

    return zip_file_path


def tar_folder(folder_path: str, archive_file_name: str = None, max_file_size: int = None,
               exclude: list = None, compression: bool = False) -> str or None:
    """
    Tar a folder (via tar). The folder will be packaged to an archive file in a temp directory.

    # Arguments
        folder_path (string): Path to the folder.
        archive_file_name (string): Name of the resulting tar package file (optional)
        max_file_size (bool): Max file size in `MB` to be included in the archive (optional)
        exclude (list[str]): List of files or folders to exclude from the archive. This also supports wildcards (optional)
        compression (int): If `True`, compression will be applied (optional)

    # Returns
    Path to the packaged archive file or `None` if tar-process failed.
    """
    if not os.path.isdir(folder_path):
        log.info("Failed to package to tar (not a directory): " + folder_path)
        return None

    temp_folder = tempfile.mkdtemp()

    def cleanup():
        log.info("Removing temp directory: " + temp_folder)
        shutil.rmtree(temp_folder)
        log.info("Temp directory removed")

    atexit.register(cleanup)

    if not archive_file_name:
        archive_file_name = os.path.basename(folder_path) + ".tar"

    archive_file_path = os.path.join(temp_folder, archive_file_name)
    tar_options = " --ignore-failed-read "
    if max_file_size:
        tar_options += " --exclude-from <(find '" + folder_path + "' -size +" + str(max_file_size) + "M)"
    if exclude:
        for excluded in exclude:
            tar_options += " --exclude='" + excluded + "' "

    tar_mode = " -cf "  # no compression
    if compression:
        tar_mode = " -czf "

    log.debug("Packaging (via tar) folder: " + folder_path + " to " + archive_file_path)

    tar_command = "tar " + tar_options + tar_mode + " '" + archive_file_path + "' -C '" + folder_path + "' ."
    log.info("Executing: " + tar_command)
    # exclude only works with bash
    exit_code = system_utils.bash_command(tar_command)
    log.info("Finished with exit code: " + str(exit_code))

    # TODO check call return if successful
    if not os.path.isfile(archive_file_path):
        log.warning("Failed to tar folder: " + archive_file_path)

    return archive_file_path


def extract_tar(file_path: str, unpack_path: str = None, remove_if_exists: bool = False) -> str or None:
    """
    Extract a tar file.

    # Arguments
        file_path (string): Path to tar file.
        unpack_path (string): Path to unpack the file (optional)
        remove_if_exists (bool): If `True`, the directory will be removed if it already exists (optional)

    # Returns
    Path to the unpacked folder or `None` if unpacking failed.
    """

    if not os.path.exists(file_path):
        log.warning(file_path + " does not exist.")
        return None

    if not tarfile.is_tarfile(file_path):
        log.warning(file_path + " is not a tar file.")
        return None

    if not unpack_path:
        unpack_path = os.path.join(os.path.dirname(file_path), os.path.splitext(os.path.basename(file_path))[0])

    if os.path.isdir(unpack_path):
        log.info("Unpack directory already exists " + unpack_path)
        if not os.listdir(unpack_path):
            log.info("Directory is empty. Unpacking...")
        elif remove_if_exists:
            log.info('Removing existing unpacked dir: ' + unpack_path)
            shutil.rmtree(unpack_path)
        else:
            return unpack_path

    log.info("Unpacking file " + os.path.basename(file_path) + " to: " + unpack_path)
    compression = file_utils.identify_compression(file_path)
    if not compression:
        mode = 'r'
    elif compression == "gz":
        mode = 'r:gz'
    elif compression == "bz2":
        mode = 'r:bz2'
    else:
        mode = 'r'

    tar = tarfile.open(file_path, mode)
    tar.extractall(unpack_path)
    tar.close()

    # Tar unpacking via tar command
    # tar needs empty directory
    # if not os.path.exists(unpack_path):
    #    os.makedirs(unpack_path)
    # log.info("Unpacking (via tar command) file " + os.path.basename(file_path) + " to: " + unpack_path)
    # handle compression with -zvxf
    # cmd = "tar -xf " + file_path + " -C " + unpack_path
    # log.debug("Executing: " + cmd)
    # exit_code = system_utils.bash_command(cmd)
    # log.info("Finished with exit code: " + str(exit_code))

    if not os.path.exists(unpack_path):
        log.warning("Failed to extract tar file: " + file_path)

    return unpack_path


def extract_via_patoolib(file_path: str, unpack_path: str = None, remove_if_exists: bool = False) -> str or None:
    """
    Extract an archive file via patoolib

    # Arguments
        file_path (string): Path to an archive file.
        unpack_path (string): Path to unpack the file (optional)
        remove_if_exists (bool): If `True`, the directory will be removed if it already exists (optional)

    # Returns
    Path to the unpacked folder or `None` if unpacking failed.
    """
    # TODO handle compression with -zvxf
    if not os.path.exists(file_path):
        log.warning(file_path + " does not exist.")
        return None

    try:
        import patoolib
    except ImportError:
        log.warning("patoolib is not installed: Run pip install patool")
        return None

    if not unpack_path:
        unpack_path = os.path.join(os.path.dirname(file_path), os.path.splitext(os.path.basename(file_path))[0])

    if os.path.isdir(unpack_path):
        log.info("Unpack directory already exists " + unpack_path)
        if not os.listdir(unpack_path):
            log.info("Directory is empty. Unpacking...")
        elif remove_if_exists:
            log.info('Removing existing unpacked dir: ' + unpack_path)
            shutil.rmtree(unpack_path)
        else:
            return unpack_path

    try:
        patoolib.extract_archive(file_path, outdir=unpack_path)
    except Exception as e:
        log.warning("Failed to unpack via patoolib: ", e)
        return None

    return unpack_path


def download_file(url: str, folder_path: str, file_name: str = None, force_download: bool = False) -> str and bool:
    # NOTE the stream=True parameter below
    with requests.get(url, stream=True, allow_redirects=True) as r:
        content_type = r.headers.get('Content-Type')
        if content_type and 'html' in content_type.lower():
            log.warning("The url is pointing to an HTML page. Are you sure you want to download this as file?")
        try:
            total_length = int(r.headers.get('Content-Disposition').split('size=')[1])
        except:
            try:
                total_length = int(r.headers.get('Content-Length'))
            except:
                log.warning("Failed to figure out size of file.")
                total_length = 0

        if not file_name:
            # if file name is not provided use filename from url
            file_name = url2filename(url)  # url.split('/')[-1]
            try:
                # Try to use filename from content disposition
                file_name = re.findall('filename=(.+)', r.headers.get('Content-Disposition'))[0]
            except:
                pass

        file_path = os.path.join(folder_path, file_name)
        if not force_download and os.path.isfile(file_path):
            if total_length == os.path.getsize(file_path):
                log.info("File " + file_name + " already exists with same size and will not be downloaded.")
                # file already exists and has same size -> do not download
                return file_path, False

        if not os.path.exists(os.path.dirname(file_path)):
            os.makedirs(os.path.dirname(file_path))

        with open(file_path, 'wb') as f:
            pbar = tqdm.tqdm(total=total_length, initial=0, mininterval=0.3, unit='B',
                             unit_scale=True, desc="Downloading " + str(file_name), file=sys.stdout)
            for chunk in r.iter_content(chunk_size=8192):
                if chunk:  # filter out keep-alive new chunks
                    pbar.update(len(chunk))
                    f.write(chunk)
            pbar.close()
        return file_path, True


def unpack_archive(file_path: str, unpack_path: str = None, remove_if_exists: bool = False) -> str or None:
    import tarfile
    import zipfile

    if not os.path.isfile(file_path):
        log.warning("File does not exist: " + file_path)
        return None

    if zipfile.is_zipfile(file_path):
        return extract_zip(file_path, unpack_path, remove_if_exists)
    elif tarfile.is_tarfile(file_path):
        return extract_tar(file_path, unpack_path, remove_if_exists)
    else:
        return extract_via_patoolib(file_path, unpack_path, remove_if_exists)

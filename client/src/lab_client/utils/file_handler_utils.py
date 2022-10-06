import os
import re

import requests
import tqdm as tqdm
from loguru import logger
import tempfile, zipfile
import atexit
import shutil
import sys

from lab_client.utils.request_utils import url2filename


def download_file(
    url: str, folder_path: str, file_name: str = None, force_download: bool = False
) -> str and bool:
    # NOTE the stream=True parameter below

    with requests.get(url, stream=True, allow_redirects=True) as r:
        content_type = r.headers.get("Content-Type")
        if content_type and "html" in content_type.lower():
            logger.warning(
                "The url is pointing to an HTML page. Are you sure you want to download this as file?"
            )
        try:
            total_length = int(r.headers.get("Content-Disposition").split("size=")[1])
        except:
            try:
                total_length = int(r.headers.get("Content-Length"))
            except:
                logger.warning("Failed to figure out size of file.")
                total_length = 0

        if not file_name:
            # if file name is not provided use filename from url
            file_name = url2filename(url)  # url.split('/')[-1]
            try:
                # Try to use filename from content disposition
                file_name = re.findall(
                    "filename=(.+)", r.headers.get("Content-Disposition")
                )[0]
            except:
                pass

        file_path = os.path.join(folder_path, file_name)
        if not force_download and os.path.isfile(file_path):
            if total_length == os.path.getsize(file_path):
                logger.info(
                    "File "
                    + file_name
                    + " already exists with same size and will not be downloaded."
                )
                # file already exists and has same size -> do not download
                return file_path, False

        if not os.path.exists(os.path.dirname(file_path)):
            os.makedirs(os.path.dirname(file_path))

        with open(file_path, "wb") as f:
            pbar = tqdm.tqdm(
                total=total_length,
                initial=0,
                mininterval=0.3,
                unit="B",
                unit_scale=True,
                desc="Downloading " + str(file_name),
                file=sys.stdout,
            )
            for chunk in r.iter_content(chunk_size=8192):
                if chunk:  # filter out keep-alive new chunks
                    pbar.update(len(chunk))
                    f.write(chunk)
            pbar.close()
        return file_path, True

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
        logger.info("Failed to zip (not a directory): " + folder_path)
        return None

    temp_folder = tempfile.mkdtemp()

    if max_file_size:
        max_file_size = max_file_size * 1000000  # MB ro bytes

    def cleanup():
        logger.info("Removing temp dir: " + temp_folder)
        shutil.rmtree(temp_folder)
        logger.info("Temp directory removed")

    atexit.register(cleanup)

    if not archive_file_name:
        archive_file_name = os.path.basename(folder_path) + ".zip"

    zip_file_path = os.path.join(temp_folder, archive_file_name)
    logger.debug("Zipping folder: " + folder_path + " to " + zip_file_path)
    zip_file = zipfile.ZipFile(zip_file_path, "w", compression)

    # dont packge folder inside, only package everything inside folder
    for dirname, subdirs, files in os.walk(folder_path):
        if excluded_folders:
            for excluded_folder in excluded_folders:
                if excluded_folder in subdirs:
                    logger.debug("Ignoring folder because of name: " + excluded_folder)
                    subdirs.remove(excluded_folder)
        if dirname != folder_path:
            # only write if dirname is not the root folder
            zip_file.write(dirname, os.path.relpath(dirname, folder_path))
        for filename in files:
            if max_file_size and max_file_size < os.path.getsize(os.path.join(dirname, filename)):
                # do not write file if it is bigger than
                logger.debug("Ignoring file because of file size: " + filename)
                continue
            file_path = os.path.join(dirname, filename)
            zip_file.write(file_path, os.path.relpath(file_path, folder_path))
    zip_file.close()

    return zip_file_path

from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import logging
import os
import re
import sys

from tqdm import tqdm
from urllib3 import HTTPResponse

from lab_api.swagger_client import LabFile
from lab_client.commons import text_utils, file_utils, request_utils
from lab_client.commons.event_listener import Event
from lab_client.handler import lab_api_handler
from lab_client.utils import file_handler_utils


class FileHandler:
    _FILE_VERSION_SUFFIX_PATTERN = re.compile(r"\.v(\d+)$")

    _FILE_METADATA_PREFIX = "x-amz-meta-"
    _FILE_METADATA_EXPERIMENT = "experiment"

    _BACKUP_PACKAGE_NAME = "backup.zip"

    def __init__(self, env):
        # Initialize logger
        self.log = logging.getLogger(__name__)

        # Initialize variables
        self.env = env

        # Files
        self._requested_files = []
        self._uploaded_files = []

        # File events
        self.on_file_requested = Event()
        self.on_file_uploaded = Event()

    def get_file(self, key: str, force_download: bool = False,
                 unpack: bool or None = None,
                 track_event: bool = True) -> str or None:
        """
        Returns local path to the file for the given `key`. If the file is not available locally, download it from the storage of the Lab Instance.

        # Arguments
            key (string): Key or url of the requested file.
            force_download (boolean): If `True`, the file will always be downloaded and not loaded locally (optional).
            unpack (boolean or string): If `True`, the file - if a valid ZIP - will be unpacked within the data folder.
            If a path is provided, the ZIP compatible file will automatically be unpacked at the given path (optional).
            track_event (bool): If `True`, this file operation will be tracked and registered listeners will be notified (optional)

        # Returns
        Local path to the requested file or `None` if file is not available.
        """

        file_updated = False
        resolved_key = key

        if request_utils.is_valid_url(key):
            # key == url -> download from url
            local_file_path, file_updated = file_handler_utils.download_file(key,
                                                                             self.env.downloads_folder,
                                                                             force_download=force_download)
        elif os.path.isdir(self.resolve_path_from_key(key)):
            # If key resolves to a directory and not a file -> directly return directory
            return self.resolve_path_from_key(key)
        else:
            local_file_path = self.load_local_file(key)

            if not local_file_path or force_download or not os.path.isfile(local_file_path):
                # no local file found -> try to directly download file
                try:
                    local_file_path = self.download_file(key)

                    if local_file_path:
                        file_updated = True
                except:
                    self.log.warning("Failed to request file info from remote for key " + key)
            elif not self.env.is_connected():
                self.log.info("Environment is not connected to Lab. Only local file operations are supported.")
            else:
                try:
                    file_info = self.get_file_info(key=key)
                    if not file_info:
                        self.log.info("Could not request file from remote with " + key)
                    else:
                        local_file_version = self.get_version_from_key(local_file_path)
                        local_file_size = os.path.getsize(local_file_path)

                        resolved_key = file_info.key

                        remote_file_version = file_info.version
                        remote_file_size = file_info.size

                        if remote_file_version > local_file_version:
                            self.log.info(
                                "Local file has a lower version compared to remote file. Updating file from remote.")
                            downloaded_file = self.download_file(key)
                            if downloaded_file:
                                file_updated = True
                                local_file_path = downloaded_file
                        elif local_file_version == remote_file_version and local_file_size != remote_file_size:
                            self.log.info(
                                "Local file has another size compared to remote file. Updating file from remote.")
                            downloaded_file = self.download_file(key)
                            if downloaded_file:
                                file_updated = True
                                local_file_path = downloaded_file
                except:
                    self.log.warning("Failed to request file info from remote for key " + key)

        if not local_file_path:
            self.log.warning("Failed to find file for key: " + key)
            return None

        if not os.path.isfile(local_file_path):
            self.log.warning("File does not exist locally: " + local_file_path)
            return None

        if track_event:
            # use resolved key to get the dataset with version
            self._requested_files.append(resolved_key)
            self.on_file_requested(resolved_key)

        if unpack:
            remove_existing_folder = file_updated

            if isinstance(unpack, bool):
                unpack = os.path.join(os.path.dirname(os.path.realpath(local_file_path)),
                                      os.path.basename(local_file_path).split('.')[0])

            # unpack is a path
            if not file_utils.is_subdir(unpack, self.env.root_folder):
                remove_existing_folder = False

            unpack_path = file_handler_utils.unpack_archive(local_file_path, unpack, remove_existing_folder)
            if unpack_path and os.path.exists(unpack_path):
                return unpack_path
            else:
                self.log.warning("Unable to unpack file, its not a supported archive format: " + local_file_path)
                return local_file_path

        return local_file_path

    def upload_file(self, file_path: str,
                    data_type: str,
                    metadata: dict = None,
                    file_name: str = None,
                    versioning: bool = True,
                    track_event: bool = True) -> str or None:
        """
        Uploads a file to the storage of the Lab Instance.

        # Arguments
            file_path (string): Local file path to the file you want ot upload.
            data_type (string): Data type of the file. Possible values are `model`, `dataset`, `experiment`.
            metadata (dict): Adds additional metadata to remote storage (optional).
            file_name (str): File name to use in the remote storage. If not provided, the name will be extracted from the provided path (optional)
            versioning (bool): If `False`, the file will be uploaded without using the versioning (optional)
            track_event (bool): If `True`, this file operation will be tracked and registered listeners will be notified (optional)

        # Returns
        Key of the uploaded file.

        # Raises
        Exception if file does not exist locally.
        """

        if os.path.isdir(file_path):
            self.log.info("Path is a folder, uploading as folder instead.")
            return self.upload_folder(file_path,
                                      data_type=data_type,
                                      metadata=metadata,
                                      versioning=versioning,
                                      file_name=file_name)
        if not os.path.isfile(file_path):
            raise Exception('File does not exist locally:' + file_path)

        if not self.env.is_connected():
            self.log.warning("Environment is not connected to Lab. Only local file operations are supported.")
            return None

        # TODO check if file on remote storage has same size -> if so, don't upload again

        processed_metadata = {}

        # add experiment id of active experiment as metadata
        # TODO check if has run?
        if self.env.active_exp and self.env.active_exp.key:
            processed_metadata[
                self._FILE_METADATA_PREFIX + self._FILE_METADATA_EXPERIMENT] = self.env.active_exp.key

        if metadata:
            # Add metadata as headers, to be valid, metadata needs to have the x-amz-meta prefix (S3 conform)
            for key in metadata:
                value = metadata[key]
                if not key.startswith(self._FILE_METADATA_PREFIX):
                    key = self._FILE_METADATA_PREFIX + key
                processed_metadata[key] = value
        optional_args = {}
        if versioning is not None:
            optional_args['versioning'] = versioning
        if file_name:
            optional_args['file_name'] = file_name

        response = self._get_lab_handler().upload_file_chunked(project=self.env.project,
                                                               file=file_path,
                                                               data_type=data_type,
                                                               headers=processed_metadata,
                                                               **optional_args)

        if self._get_lab_handler().request_successful(response):
            key = response.data
            self.log.info("Uploaded file with key " + key + " into project " + self.env.project)
            if track_event:
                self._uploaded_files.append(key)
                self.on_file_uploaded(key)
            return key
        else:
            self.log.error("Failed to upload file " + file_utils.get_filename(file_path, exclude_extension=False)
                           + " into project " + self.env.project)
            return None

    def upload_folder(self, folder_path: str,
                      data_type: str,
                      metadata: dict = None,
                      file_name: str = None,
                      versioning: bool = True,
                      track_event: bool = True) -> str or None:
        """
        Packages the folder (via `zipfile`) and uploads the specified folder to the storage of the Lab instance.

        # Arguments
            folder_path (string): Local path to the folder you want ot upload.
            data_type (string): Data type of the resulting zip-file. Possible values are `model`, `dataset`, `experiment`.
            metadata (dict): Adds additional metadata to remote storage (optional).
            file_name (str): File name to use in the remote storage. If not provided, the name will be extracted from the provided path (optional)
            versioning (bool): If `False`, the file will be uploaded without using the versioning (optional)
            track_event (bool): If `True`, this file operation will be tracked and registered listeners will be notified (optional)

        # Returns
        Key of the uploaded (zipped) folder.

        # Raises
        Exception if folder does not exist locally
        """
        if os.path.isfile(folder_path):
            self.log.info("Path is a file, uploading as file instead.")
            return self.upload_file(folder_path,
                                    data_type=data_type,
                                    metadata=metadata,
                                    versioning=versioning,
                                    file_name=file_name)

        if not os.path.isdir(folder_path):
            raise Exception('Folder does not exist locally:' + folder_path)

        if not self.env.is_connected():
            self.log.warning("Environment is not connected to Lab. Only local file operations are supported.")
            return None

        self.log.info("Zipping folder: " + folder_path)
        zip_file_path = file_handler_utils.zip_folder(folder_path)

        key = self.upload_file(zip_file_path, data_type, metadata=metadata, versioning=versioning,
                               file_name=file_name, track_event=track_event)
        os.remove(zip_file_path)  # remove file after upload
        return key

    def download_file(self, key: str) -> str or None:
        """
        Loads a local file with the given key. Will also try to find the newest version if multiple versions exists locally for the key.

        # Arguments
            key (string): Key of the file.

        # Returns
        Path to file or `None` if no local file was found.
        """
        if not self.env.is_connected():
            self.log.warning("Environment is not connected to Lab. Only local file operations are supported.")
            return None

        file_info = self._get_lab_handler().lab_api.get_file_info(project=self.env.project,
                                                                  file_key=key)

        if not self._get_lab_handler().request_successful(file_info):
            self.log.warning("Failed to download file for key " + key)
            return None

        file_key = file_info.data.key
        downloaded_file_path = self.resolve_path_from_key(file_key)
        file_download = self._get_lab_handler().lab_api.download_file(project=self.env.project,
                                                                      file_key=key,
                                                                      _preload_content=False)
        if file_download.status != 200:
            self.log.warning("Failed to download file for key " + key + ". Status code: " + str(file_download.status))
            return None

        self.log.info("Downloading remote file: " + file_key)
        is_successful = self._download_file_stream(file_download, downloaded_file_path)

        if not is_successful:
            self.log.warning("Could not write remote file to path " + downloaded_file_path)
            return None

        return downloaded_file_path

    def load_local_file(self, key: str) -> str or None:
        """
        Loads a local file with the given key. Will also try to find the newest version if multiple versions exists locally for the key.

        # Arguments
            key (string): Key of the file.

        # Returns
        Path to file or `None` if no local file was found.
        """
        # always load latest version also from local
        local_file_path = self.resolve_path_from_key(key)

        file_dir = os.path.dirname(local_file_path)
        file_name = os.path.basename(local_file_path)

        if not os.path.isdir(file_dir):
            return None

        latest_file_path = None
        latest_file_version = 0

        for file in os.listdir(file_dir):
            if file.startswith(file_name):
                if self.get_version_from_key(file) > latest_file_version:
                    latest_file_path = os.path.abspath(os.path.join(file_dir, file))
                    latest_file_version = self.get_version_from_key(file)

        if latest_file_path and os.path.isfile(latest_file_path):
            self.log.debug("Loading latest version (" + str(latest_file_version) + ") for " + key + " from local.")
            return latest_file_path

        return None

    def list_remote_files(self, data_type: str = None, prefix: str = None) -> list:
        """
        List remote files from Lab instance.

        # Arguments
            data_type (string): Data type to filter files (dataset, model...)
            prefix (string): Key prefix to filter files. If `data_type` is provided as well, the prefix will be used to filter files from the data type.

        # Returns
        List of found `LabFiles`
        """
        if not self.env.is_connected():
            self.log.warning("Environment is not connected to Lab. Only local file operations are supported.")
            return []

        if data_type or prefix:
            response = None
            if data_type and prefix:
                response = self._get_lab_handler().lab_api.get_files(self.env.project,
                                                                     data_type=data_type,
                                                                     prefix=prefix)
            elif data_type:
                response = self._get_lab_handler().lab_api.get_files(self.env.project,
                                                                     data_type=data_type)
            elif prefix:
                response = self._get_lab_handler().lab_api.get_files(self.env.project,
                                                                     prefix=prefix)

            if self._get_lab_handler().request_successful(response):
                return response.data
            else:
                return []
        else:
            response = self._get_lab_handler().lab_api.get_files(self.env.project)
            if self._get_lab_handler().request_successful(response):
                return response.data
            else:
                return []

    def delete_remote_file(self, key: str, keep_latest_versions: int = 0) -> bool:
        """
        Delete a file from remote storage.

        # Arguments
            key (string): Key of the file.
            keep_latest_versions (string): Keep the n-latest file versions.

        # Returns
        'True' if file was deleted successfully.
        """
        if not self.env.is_connected():
            self.log.warning("Environment is not connected to Lab. Only local file operations are supported.")
            return False

        response = self._get_lab_handler().lab_api.delete_file(self.env.project, key,
                                                               keep_latest_versions=keep_latest_versions)

        success = self._get_lab_handler().request_successful(response)
        if success:
            self.log.info("Deleted remote file with key: " + key)
        return success

    def get_file_info(self, key: str) -> LabFile or None:
        """
        Get file information from the Lab instance.

        # Arguments
            key (string): Key of the file.

        # Returns
        `LabFile` or `None` if file is not found.
        """
        if not self.env.is_connected():
            self.log.warning("Environment is not connected to Lab. Only local file operations are supported.")
            return None

        response = self._get_lab_handler().lab_api.get_file_info(self.env.project, key)
        if self._get_lab_handler().request_successful(response):
            return response.data

        return None

    def resolve_path_from_key(self, key: str) -> str:
        """
        Return the local path for a given key.
        """
        return os.path.abspath(os.path.join(self.env.project_folder, key))

    def get_version_from_key(self, key: str) -> int:
        """
        Return the version extracted from the key. Or 1 if no version is attached.
        """
        version_suffix = self._FILE_VERSION_SUFFIX_PATTERN.search(key)
        if version_suffix:
            return int(version_suffix.group(1))
        else:
            return 1

    def remove_version_from_key(self, key: str) -> str:
        """
        Return the key without the version suffix.
        """
        return self._FILE_VERSION_SUFFIX_PATTERN.sub("", key)

    @property
    def requested_files(self) -> list:
        """
        Return the list of requested files (files requested via 'get_file()')

        # Returns
        A list of requested file keys
        """

        return self._requested_files

    @property
    def uploaded_files(self) -> list:
        """
        Return the list of uploaded files (files uploaded via 'upload_file()')

        # Returns
        A list of uploaded file keys
        """
        return self._uploaded_files

    def _get_lab_handler(self) -> lab_api_handler.LabApiHandler:
        return self.env.lab_handler

    def _download_file_stream(self, response: HTTPResponse, file_path: str) -> bool:
        """
        Stream the requested file to file_path. This way, even large files can be downloaded.

        # Arguments
            response (#HttpResponse): the http response of the server. Should contain an output stream.
            file_path (string): the path where the file will be saved

        # Returns
        True if the download was successful, False otherwise
        """
        if not os.path.exists(os.path.dirname(file_path)):
            os.makedirs(os.path.dirname(file_path))

        try:
            # set chunk_size to same size as server's output size
            chunk_size = 16384
            downloaded = 0
            total_length = int(response.headers['Content-Disposition'].split('size=')[1])

            desc = "Downloading " + text_utils.truncate_middle(os.path.basename(file_path), 20)
            pbar = tqdm(
                total=total_length, initial=downloaded, mininterval=0.3,
                unit='B', unit_scale=True, desc=desc, file=sys.stdout)

            with open(file_path, 'wb') as out:
                while True:
                    data = response.read(chunk_size)
                    downloaded += len(data)
                    if not data:
                        break
                    out.write(data)
                    pbar.update(len(data))

            pbar.close()

            response.release_conn()
            if total_length != 0 and downloaded != total_length:
                print("Failed to download full file")

        except Exception as e:
            self.log.warning("Could not stream the data, so write it in once", e)
            try:
                with open(file_path, 'wb') as f:
                    f.write(response.data)
            except:
                return False

        return True

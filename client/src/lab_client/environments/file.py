from typing import Optional

from contaxy.schema import File

from lab_client.handler.file_handler import FileHandler


class FileEnvironment:
    @property
    def file_handler(self) -> FileHandler:
        """
        Returns the file handler. The file handler provides additional functionality for interacting with the remote storage.
        """

        if self._file_handler is None:
            self._file_handler = FileHandler(self, self._file_client)

        return self._file_handler

    def upload_file(
        self,
        file_path: str,
        data_type: str,
        metadata: dict = None,
        file_name: str = None,
    ) -> str:
        """Uploads a file to the storage of the Lab Instance.

        Args:
            file_path: Local file path to the file you want ot upload.
            data_type: Data type of the file. Possible values are `model`, `dataset`, `experiment`.
            metadata: Adds additional metadata to remote storage (optional).
            file_name: File name to use in the remote storage. If not provided, the name will be extracted from the provided path (optional)
        Returns:
            Key of the uploaded file.
        """
        return self.file_handler.upload_file(file_path, data_type, metadata, file_name)

    def get_file(
        self, key: str, version: Optional[str] = None, force_download: bool = False, unpack: bool = False
    ) -> str:
        """Returns local path to the file for the given `key`.
        If the file is not available locally, download it from the storage of the Lab Instance.

        Args:
            key: Key or url of the requested file.
            version: Version of the file to return. If `None` (default) the latest version will be returned.
            force_download: If `True`, the file will always be downloaded and not loaded locally (optional).
        Returns:
            Local path to the requested file or `None` if file is not available.
        """
        return self.file_handler.get_file(key, version, force_download, unpack)

    def upload_folder(
        self,
        folder_path: str,
        data_type: str,
        metadata: dict = None,
        file_name: str = None,
    ) -> str:
        """Packages the folder (via `zipfile`) and uploads the specified folder to the storage of the Lab instance.

        Args:
            folder_path: Local path to the folder you want ot upload.
            data_type: Data type of the resulting zip-file. Possible values are `model`, `dataset`, `experiment`.
            metadata: Adds additional metadata to remote storage (optional).
            file_name: File name to use in the remote storage. If not provided, the name will be extracted from the provided path (optional)
        Returns:
            Key of the uploaded (zipped) folder.
        """
        return self.file_handler.upload_folder(folder_path, data_type, metadata, file_name)

    def get_file_metadata(
        self, project: str, key: str, version: Optional[str] = None
    ) -> File:
        """Returns file metadata to the file for the given `key`.

        Args:
            project: Project ID.
            key: Key or url of the requested file.
            version: Version of the file whose metadata is to be returned. If `None` (default) the latest version metadata will be returned.

        Returns:
            The metadata Dictionary of the file.
        """
        return self.file_handler.get_file_metadata(project, key, version)

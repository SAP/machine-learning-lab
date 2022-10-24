from mlflow.store.artifact.artifact_repo import ArtifactRepository, verify_artifact_path
from mlflow.entities import FileInfo
from mlflow.utils.file_utils import relative_path_to_artifact_path
from contaxy.clients import FileClient
from contaxy.clients.shared import BaseUrlSession
from mlflow.exceptions import MlflowException
import tempfile
from mlflow.protos.databricks_pb2 import INVALID_PARAMETER_VALUE, RESOURCE_DOES_NOT_EXIST
from collections import namedtuple
from urllib import parse

import os
import posixpath


class MlLabArtifactRepository(ArtifactRepository):
    is_plugin = True

    def __init__(self, artifact_uri):
        print("========================")
        print("INITIALIZED ARTIFACT REPO")
        print("artifact_uri:", artifact_uri)
        print("========================")
        # TODO: find a solution for not hardcoding the url and token
        super().__init__(artifact_uri)
        parse_result = parse.urlparse(artifact_uri)
        url = "http://{}/api".format(parse_result.netloc)
        if "CONTAXY_API_ENDPOINT" in os.environ:
            url = os.environ["CONTAXY_API_ENDPOINT"]
        session = BaseUrlSession(base_url=url)
        self.project_id = parse_result.path.split("/")[1]
        token = os.environ["LAB_API_TOKEN"]
        session.headers = {"Authorization": f"Bearer {token}"}
        session.verify = False  # Workaround for development if SAP certificate is not installed
        file_client = FileClient(session)
        self.file_client = file_client
        self.artifact_uri = artifact_uri[len(parse_result.scheme + "://"):]
        self.default_artifact_root = 'projects/{}/services/{}/access/5001/'.format(
            self.project_id,
            os.getenv("CONTAXY_DEPLOYMENT_NAME", "pylab-p-{}-s-ml-flow-1c457".format(self.project_id))
        )
        self.store_prefix = 'mlflow-data'

    def log_artifact(self, local_file, artifact_path=None):
        print("========================")
        print("LOG ARTIFACT")
        print("local_file:", local_file)
        print("artifact_path:", artifact_path)
        print("========================")
        artifact_path = artifact_path[len(self.default_artifact_root):]
        verify_artifact_path(artifact_path)
        file_name = os.path.basename(local_file)
        if artifact_path:
            artifact_path = os.path.join(
                self.store_prefix, artifact_path, file_name)
        else:
            artifact_path = os.path.join(self.store_prefix, file_name)

        # NOTE: The artifact_path is expected to be in posix format.
        # Posix paths work fine on windows but just in case we normalize it here.
        if artifact_path:
            artifact_path = os.path.normpath(artifact_path)
        with open(local_file, "rb") as f:
            self.file_client.upload_file(
                project_id=self.project_id, file_key=artifact_path, file_stream=f)

    def log_artifacts(self, local_dir, artifact_path=None):
        print("========================")
        print("LOG ARTIFACTS")
        print("local_dir:", local_dir)
        print("artifact_path:", artifact_path)
        print("========================")
        local_dir = os.path.abspath(local_dir)
        for root, _, filenames in os.walk(local_dir):
            if root == local_dir:
                artifact_dir = artifact_path
            else:
                rel_path = os.path.relpath(root, local_dir)
                rel_path = relative_path_to_artifact_path(rel_path)
                artifact_dir = (
                    posixpath.join(
                        artifact_path, rel_path) if artifact_path else rel_path
                )
            for f in filenames:
                self.log_artifact(os.path.join(root, f), artifact_dir)

    def list_artifacts(self, path):
        # NOTE: The path is expected to be in posix format.
        # Posix paths work fine on windows but just in case we normalize it here.
        print("========================")
        print("LIST ARTIFACTS")
        print("path:", path)
        print("========================")
        if path:
            path = path[len(self.default_artifact_root):]
            path = os.path.normpath(path)
            prefix = os.path.join(self.store_prefix, path) + "/"
        else:
            prefix = self.store_prefix
        files = self.file_client.list_files(
            project_id=self.project_id, prefix=prefix)

        infos = []
        for file in files:
            characters_to_remove = len(self.store_prefix) + 1
            if path:
                # remove path + trailing slash
                characters_to_remove += len(path) + 1
            file_path = file.key[characters_to_remove:]
            if "/" in file_path:
                folder_name = file_path.split("/")[0]
                info = FileInfo(folder_name, is_dir=True, file_size=None)
            else:
                info = FileInfo(file.display_name, False, file.file_size)
            if info not in infos:
                infos.append(info)
        return infos

    def _download_file(self, remote_file_path, local_path):
        print("========================")
        print("DOWNLOAD FILE")
        print("remote_file_path:", remote_file_path)
        print("local_path:", local_path)
        print("========================")
        print("PROJECT ID: ", self.project_id)
        remote_file_path = remote_file_path[len(self.default_artifact_root):]
        print("FILE KEY: ", os.path.join(self.store_prefix, remote_file_path))
        stream = self.file_client.download_file(
            project_id=self.project_id, file_key=os.path.join(self.store_prefix, remote_file_path))
        print("Downloaded file from contaxy")
        with open(local_path, "wb") as f:
            for chunk in stream[0]:
                print("CHUNK: ", chunk)
                f.write(chunk)

    def download_artifacts(self, artifact_path, dst_path=None):
        """
        Download an artifact file or directory to a local directory if applicable, and return a
        local path for it.
        The caller is responsible for managing the lifecycle of the downloaded artifacts.

        :param artifact_path: Relative source path to the desired artifacts.
        :param dst_path: Absolute path of the local filesystem destination directory to which to
                         download the specified artifacts. This directory must already exist.
                         If unspecified, the artifacts will either be downloaded to a new
                         uniquely-named directory on the local filesystem or will be returned
                         directly in the case of the LocalArtifactRepository.

        :return: Absolute path of the local filesystem location containing the desired artifacts.
        """
        print("========================")
        print("DOWNLOAD ARTIFACTS")
        print("artifact_path:", artifact_path)
        print("dst_path:", dst_path)
        print("========================")
        # Represents an in-progress file artifact download to a local filesystem location
        InflightDownload = namedtuple(
            "InflightDownload",
            [
                # The artifact path, given relative to the repository's artifact root location
                "src_artifact_path",
                # The local filesystem destination path to which artifacts are being downloaded
                "dst_local_path",
                # A future representing the artifact download operation
                "download_future",
            ],
        )

        def async_download_artifact(src_artifact_path, dst_local_dir_path):
            """
            Download the file artifact specified by `src_artifact_path` to the local filesystem
            directory specified by `dst_local_dir_path`.
            :param src_artifact_path: A relative, POSIX-style path referring to a file artifact
                                      stored within the repository's artifact root location.
                                      `src_artifact_path` should be specified relative to the
                                      repository's artifact root location.
            :param dst_local_dir_path: Absolute path of the local filesystem destination directory
                                       to which to download the specified artifact. The downloaded
                                       artifact may be written to a subdirectory of
                                       `dst_local_dir_path` if `src_artifact_path` contains
                                       subdirectories.
            :return: A local filesystem path referring to the downloaded file.
            """
            print("========================")
            print("ASYNC DOWNLOAD ARTIFACT")
            print("src_artifact_path:", src_artifact_path)
            print("dst_local_dir_path:", dst_local_dir_path)
            print("========================")
            inflight_downloads = []
            local_destination_file_path = self._create_download_destination(
                src_artifact_path=src_artifact_path, dst_local_dir_path=dst_local_dir_path
            )
            download_future = self.thread_pool.submit(
                self._download_file,
                remote_file_path=src_artifact_path,
                local_path=local_destination_file_path,
            )
            inflight_downloads.append(
                InflightDownload(
                    src_artifact_path=src_artifact_path,
                    dst_local_path=local_destination_file_path,
                    download_future=download_future,
                )
            )
            return inflight_downloads

        def async_download_artifact_dir(src_artifact_dir_path, dst_local_dir_path):
            """
            Initiate an asynchronous download of the artifact directory specified by
            `src_artifact_dir_path` to the local filesystem directory specified by
            `dst_local_dir_path`.

            This implementation is adapted from
            https://github.com/mlflow/mlflow/blob/a776b54fa8e1beeca6a984864c6375e9ed38f8c0/mlflow/
            store/artifact/artifact_repo.py#L93.

            :param src_artifact_dir_path: A relative, POSIX-style path referring to a directory of
                                          of artifacts stored within the repository's artifact root
                                          location. `src_artifact_dir_path` should be specified
                                          relative to the repository's artifact root location.
            :param dst_local_dir_path: Absolute path of the local filesystem destination directory
                                       to which to download the specified artifact directory. The
                                       downloaded artifacts may be written to a subdirectory of
                                       `dst_local_dir_path` if `src_artifact_dir_path` contains
                                       subdirectories.
            :return: A tuple whose first element is the destination directory of the downloaded
                     artifacts on the local filesystem and whose second element is a list of
                     `InflightDownload` objects, each of which represents an inflight asynchronous
                     download operation for a file in the specified artifact directory.
            """
            print("========================")
            print("ASYNC DOWNLOAD ARTIFACT DIR")
            print("src_artifact_dir_path:", src_artifact_dir_path)
            print("dst_local_dir_path:", dst_local_dir_path)
            print("========================")
            local_dir = os.path.join(dst_local_dir_path, src_artifact_dir_path)
            inflight_downloads = []
            dir_content = [  # prevent infinite loop, sometimes the dir is recursively included
                file_info
                for file_info in self.list_artifacts(src_artifact_dir_path)
                if file_info.path != "." and file_info.path != src_artifact_dir_path
            ]
            if not dir_content:  # empty dir
                if not os.path.exists(local_dir):
                    os.makedirs(local_dir, exist_ok=True)
            else:
                for file_info in dir_content:
                    if file_info.is_dir:
                        inflight_downloads += async_download_artifact_dir(
                            src_artifact_dir_path=file_info.path,
                            dst_local_dir_path=dst_local_dir_path,
                        )[1]
                    else:
                        inflight_downloads += async_download_artifact(
                            src_artifact_path=os.path.join(
                                src_artifact_dir_path, file_info.path),
                            dst_local_dir_path=dst_local_dir_path,
                        )
            return local_dir, inflight_downloads

        if dst_path is None:
            dst_path = tempfile.mkdtemp()
        dst_path = os.path.abspath(dst_path)

        if not os.path.exists(dst_path):
            raise MlflowException(
                message=(
                    "The destination path for downloaded artifacts does not"
                    " exist! Destination path: {dst_path}".format(
                        dst_path=dst_path)
                ),
                error_code=RESOURCE_DOES_NOT_EXIST,
            )
        elif not os.path.isdir(dst_path):
            raise MlflowException(
                message=(
                    "The destination path for downloaded artifacts must be a directory!"
                    " Destination path: {dst_path}".format(dst_path=dst_path)
                ),
                error_code=INVALID_PARAMETER_VALUE,
            )

        # Check if the artifacts points to a directory
        if self._is_directory(artifact_path):
            dst_local_path, inflight_downloads = async_download_artifact_dir(
                src_artifact_dir_path=artifact_path, dst_local_dir_path=dst_path
            )
        else:
            inflight_downloads = async_download_artifact(
                src_artifact_path=artifact_path, dst_local_dir_path=dst_path
            )
            assert (
                len(inflight_downloads) == 1
            ), "Expected one inflight download for a file artifact, got {} downloads".format(
                len(inflight_downloads)
            )
            dst_local_path = inflight_downloads[0].dst_local_path

        # Join futures to ensure that all artifacts have been downloaded prior to returning
        failed_downloads = {}
        for inflight_download in inflight_downloads:
            try:
                inflight_download.download_future.result()
            except Exception as e:
                failed_downloads[inflight_download.src_artifact_path] = repr(e)

        if len(failed_downloads) > 0:
            raise MlflowException(
                message=(
                    "The following failures occurred while downloading one or more"
                    " artifacts from {artifact_root}: {failures}".format(
                        artifact_root=self.artifact_uri,
                        failures=failed_downloads,
                    )
                )
            )
        return dst_local_path

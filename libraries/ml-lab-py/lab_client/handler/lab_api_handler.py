from __future__ import absolute_import
from __future__ import division
from __future__ import print_function

import json
import logging
import mimetypes
import os
import sys
from base64 import b64encode

import requests
from requests_toolbelt import MultipartEncoder, MultipartEncoderMonitor
from tqdm import tqdm

import lab_api.swagger_client as swagger_client
from lab_api.swagger_client.rest import ApiException
from lab_client.commons import text_utils


def get_basic_auth_token(user: str, password: str) -> str:
    return "Basic " + b64encode((user + ":" + password).encode('ascii')).decode("ascii")


def parse_url(url: str):
    from future.standard_library import install_aliases
    install_aliases()
    from urllib.parse import urlparse

    if '//' not in url:
        url = '%s%s' % ('http://', url)
    return urlparse(url)


class LabApiHandler:
    _AUTHORIZATION_HEADER = "Authorization"
    _API_TOKEN_COOKIE = "lab_access_token={token}"

    def __init__(self, lab_endpoint: str, lab_api_token: str or None = None, verify_ssl=False):
        # Initialize logger
        self.log = logging.getLogger(__name__)

        # Initialize variables
        self._api_client = None
        self._is_connected = False

        if lab_endpoint is None:
            raise Exception('The lab endpoint has to be set for lab initialization.')

        parsed_endpoint = parse_url(lab_endpoint)

        self.lab_endpoint = parsed_endpoint.scheme + "://" + parsed_endpoint.netloc
        self.lab_api_token = lab_api_token

        self.verify_ssl = verify_ssl

        config = swagger_client.Configuration()
        config.host = self.lab_endpoint

        # TODO Do not verify for now, otherwise it currently causes problems
        config.verify_ssl = self.verify_ssl
        if not self.verify_ssl:
            import urllib3
            # TODO Disable SSL warning?
            urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

        if self.lab_api_token is None:
            self._api_client = swagger_client.ApiClient(config)
        else:
            self._api_client = swagger_client.ApiClient(config,
                                                        header_name=self._AUTHORIZATION_HEADER,
                                                        header_value=self.lab_api_token)

        self._lab_api = swagger_client.api.ProjectsApi(self._api_client)
        self._auth_api = swagger_client.api.AuthorizationApi(self._api_client)
        self._admin_api = swagger_client.api.AdministrationApi(self._api_client)

        # overwrite upload file method?
        # self._lab_api.upload_file_with_http_info = self.upload_file_chunked

        self.check_availability()

    @property
    def lab_api(self) -> swagger_client.api.ProjectsApi:
        if not self.is_connected():
            self.log.warning("API Client is not connected.")
        return self._lab_api

    @property
    def auth_api(self) -> swagger_client.api.AuthorizationApi:
        if not self.is_connected():
            self.log.warning("API Client is not connected.")
        return self._auth_api

    @property
    def admin_api(self) -> swagger_client.api.AdministrationApi:
        if not self.is_connected():
            self.log.warning("API Client is not connected.")
        return self._admin_api

    def check_availability(self) -> bool:
        # always check availability
        if not self._lab_api or not self._auth_api:
            self._is_connected = False
            return self._is_connected

        try:
            admin_api_response = self._admin_api.get_lab_info()
            lab_api_response = self._lab_api.get_projects()
            auth_api_response = self._auth_api.get_me()
            self._is_connected = self.request_successful(lab_api_response) \
                                 and self.request_successful(auth_api_response) \
                                 and self.request_successful(admin_api_response)
            return self._is_connected
        except Exception as e:
            if isinstance(e, ApiException):
                self.log.warning("Failed to connect to lab. Reason: " + str(e.reason) + " (" + str(e.status) + ")")
            self._is_connected = False
            return self._is_connected

    def lab_obj_to_json(self, obj) -> str:
        return json.dumps(self._api_client.sanitize_for_serialization(obj))

    def is_connected(self):
        return self._is_connected

    def request_successful(self, response) -> bool:
        if not response:
            self.log.warning("Request failed. Response is None.")
            return False

        if response.errors:
            self.log.warning("Request failed. Error: " + str(response.errors))
            return False

        if not response.metadata:
            self.log.warning("Request failed. No metadata was send from server.")
            self.log.debug("Response of failed request: " + str(response))
            return False

        if 200 > response.metadata.status >= 300:
            self.log.warning("Request failed. Metadata: " + str(response.metadata))
            return False

        return True

    def upload_file_chunked(self, project: str, data_type: str, file: str, file_name: str = None,
                            versioning: bool = None, headers: dict = None, **kwargs):

        def callback(monitor):
            if monitor.pbar is None:
                if file_name:
                    desc = "Uploading " + text_utils.truncate_middle(os.path.basename(file_name), 20)
                else:
                    desc = "Uploading " + text_utils.truncate_middle(os.path.basename(file), 20)
                monitor.pbar = tqdm(total=monitor.len, initial=0, unit='B', unit_scale=True,
                                    desc=desc, mininterval=0.3, file=sys.stdout)
            monitor.pbar.update(monitor.bytes_read - monitor.pbar.n)

        filename = os.path.basename(file)
        mimetype = mimetypes.guess_type(filename)[0] or 'application/octet-stream'

        encoder = MultipartEncoder(fields={'file': (filename, open(file, 'rb'), mimetype)})

        multipart_monitor = MultipartEncoderMonitor(
            encoder,
            callback=callback
        )

        # set pbar variable to None
        multipart_monitor.pbar = None

        params = {'dataType': data_type}
        if file_name:
            params['fileName'] = file_name

        if versioning is not None:
            params['versioning'] = str(versioning)

        if not headers:
            headers = {}

        headers['Accept'] = 'application/json'
        headers['Content-Type'] = multipart_monitor.content_type
        if self.lab_api_token:
            headers['Authorization'] = self.lab_api_token

        response = requests.post(self.lab_endpoint + '/api/projects/' + project + '/files/upload',
                                 params=params, data=multipart_monitor, headers=headers, verify=self.verify_ssl)

        multipart_monitor.pbar.close()
        self.log.debug("Upload response text: " + str(response.text) + "; Status code: " + str(response.status_code))

        response.data = response.text
        return self._api_client.deserialize(response, "StringResponse")

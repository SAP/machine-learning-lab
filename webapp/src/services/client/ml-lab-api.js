import { toast } from "react-toastify";

import { ENDPOINTS } from "../handler/constants";

import LabApi from "./lab-api";

let endpoint = ENDPOINTS.newLabApi;
if (endpoint.endsWith("/")) {
  endpoint = endpoint.substring(0, endpoint.length - 1);
}

const URL_PROJECTS_ENDPOINT = endpoint + "/api/projects/{project}";
const URL_FILES_ENDPOINT = URL_PROJECTS_ENDPOINT + "/files";

function toastErrorMessage(prefix, error) {
  try {
    error.json().then(errorMessage => {
      toast.error(prefix + errorMessage.errors.message);
    });
  } catch (no_json) {
    toast.error(prefix + error.message);
  }
}

function toastErrorType(prefix, error) {
  if (typeof error === "string") {
    toast.error(prefix + error);
  } else if (prefix && !error) {
    toast.error(prefix);
  } else {
    error.json().then(errorMessage => {
      toast.error(prefix + errorMessage.errors.type);
    });
  }
}

let infoToastId = null;
function toastInfo(info) {
  if (toast.isActive(infoToastId)) {
    toast.dismiss(infoToastId);
  }

  infoToastId = toast.info(info);
}

let successToastId = null;
function toastSuccess(message) {
  if (toast.isActive(successToastId)) {
    toast.dismiss(successToastId);
  }

  successToastId = toast.success(message);
}

const getFileDownloadUrl = function(project, fileKey) {
  return (URL_FILES_ENDPOINT + "/download?fileKey=" + encodeURIComponent(fileKey)).replace(
    "{project}",
    project
  );
};

const getFileUploadUrl = function(project, dataType) {
  return (URL_FILES_ENDPOINT + "/upload?dataType={dataType}")
    .replace("{project}", project)
    .replace("{dataType}", dataType);
};

const getServiceUrl = function(project, serviceName, port) {
  return (URL_PROJECTS_ENDPOINT + "/services/{serviceName}/{port}/")
    .replace("{project}", project)
    .replace("{serviceName}", serviceName)
    .replace("{port}", port);
};

const labApiClient = new LabApi.ApiClient();
labApiClient.basePath = endpoint;
labApiClient.enableCookies = true; // to send authorization cookie with the requests

const administrationApi = new LabApi.AdministrationApi(labApiClient);
const authorizationApi = new LabApi.AuthorizationApi(labApiClient);
const projectsApi = new LabApi.ProjectsApi(labApiClient);

function getDefaultApiCallback(resolve, reject, isToastUnauthorized) {
  const UNAUTHORIZED_STATUS_CODE = 401;
  const FORBIDDEN_STATUS_CODE = 403;
  const LOGIN_PATH = "#/login";
  isToastUnauthorized =
    isToastUnauthorized === undefined || isToastUnauthorized === null;

  return function(error, data, response) {
    if (
      response !== undefined &&
      (response.status === UNAUTHORIZED_STATUS_CODE ||
        response.status === FORBIDDEN_STATUS_CODE)
    ) {
      if (
        window.location.href.indexOf(LOGIN_PATH) === -1 &&
        isToastUnauthorized
      ) {
        // don't show this toast in the login screen. Sometimes, the path is not correct, so enforce not showing the toast.
        toast.error("Not authorized.");
      }

      response.isAuthError = true;

      if (reject) {
        reject({ error: error, httpResponse: response });
      }
    } else if (error) {
      response = response || { body: {} };

      response.isAuthError = false;
      let errorBody = (response.body && response.body.errors) ? response.body.errors : {};

      if (reject) {
        reject({ error: error, errorBody: errorBody, httpResponse: response });
      }
    } else {
      if (resolve) {
        resolve({ result: data, httpResponse: response });
      }
    }
  };
}

export {
  toastInfo,
  toastSuccess,
  toastErrorMessage,
  toastErrorType,
  LabApi,
  labApiClient,
  administrationApi,
  authorizationApi,
  projectsApi,
  getDefaultApiCallback,
  getFileDownloadUrl,
  getFileUploadUrl,
  getServiceUrl
};

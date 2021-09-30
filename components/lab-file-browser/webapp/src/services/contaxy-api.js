import { ENDPOINT } from '../utils/config';
// eslint-disable import/prefer-default-export
import * as Api from './contaxy-client';

export const ENDPOINT_PROJECTS = `${ENDPOINT}/projects/{project_id}`;
export const ENDPOINT_AUTH = `${ENDPOINT}/auth`;
export const SELECTED_PROJECT_LOCAL_STORAGE_KEY = 'ctx_selected_project_id';

export function getFileDownloadUrl(projectId, fileKey) {
  return `${ENDPOINT_PROJECTS}/files/{file_key:path}:download`
    .replace('{project_id}', projectId)
    .replace('{file_key:path}', fileKey);
}

export function getFileUploadUrl(projectId, fileKey) {
  if (fileKey) {
    return `${ENDPOINT_PROJECTS}/files/{file_key}`
      .replace('{project_id}', projectId)
      .replace('{file_key}', fileKey);
  }

  return `${ENDPOINT_PROJECTS}/multipart-upload`.replace(
    '{project_id}',
    projectId
  );
}

export function getExternalLoginPageUrl() {
  return `${ENDPOINT_AUTH}/login-page`;
}

const apiClient = new Api.ApiClient();
apiClient.basePath = ENDPOINT;
apiClient.enableCookies = true;
// the generated client includes an User-Agent header which is not allowed to set as it is controlled by the browser
delete apiClient.defaultHeaders['User-Agent'];

export const authApi = new Api.AuthApi(apiClient);
export const extensionsApi = new Api.ExtensionsApi(apiClient);
export const filesApi = new Api.FilesApi(apiClient);
export const jobsApi = new Api.JobsApi(apiClient);
export const jsonApi = new Api.JsonApi(apiClient);
export const projectsApi = new Api.ProjectsApi(apiClient);
export const servicesApi = new Api.ServicesApi(apiClient);
export const systemApi = new Api.SystemApi(apiClient);
export const usersApi = new Api.UsersApi(apiClient);

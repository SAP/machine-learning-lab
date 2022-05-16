import superagent from 'superagent';

import { EXTENSION_ENDPOINT } from '../utils/config';
import showStandardSnackbar from '../app/showStandardSnackbar';

export async function deployServer(projectId, workspaceInfo) {
  try {
    await superagent
      .post(`${EXTENSION_ENDPOINT}/projects/${projectId}/mlflow-server`)
      .withCredentials()
      .send(workspaceInfo);
  } catch (err) {
    let message = err.response?.body?.detail;
    if (typeof message === 'undefined') {
      message = 'Unknown error.';
    }
    showStandardSnackbar(`Failed to deploy ML Flow server! ${message}`);
    throw err;
  }
}

export async function startServer(projectId, workspaceId) {
  try {
    await superagent
      .post(
        `${EXTENSION_ENDPOINT}/projects/${projectId}/mlflow-server/${workspaceId}:start`
      )
      .withCredentials()
      .send();
  } catch (err) {
    let message = err.response?.body?.detail;
    if (typeof message === 'undefined') {
      message = 'Unknown error.';
    }
    showStandardSnackbar(`Failed to start ML Flow server! ${message}`);
  }
}

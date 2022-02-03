import superagent from 'superagent';

import { EXTENSION_ENDPOINT } from '../utils/config';
import showStandardSnackbar from '../app/showStandardSnackbar';

export async function deployWorkspace(userId, workspaceInfo) {
  try {
    await superagent
      .post(`${EXTENSION_ENDPOINT}/users/${userId}/workspace`)
      .withCredentials()
      .send(workspaceInfo);
  } catch (err) {
    let message = err.response?.body?.detail;
    if (typeof message === 'undefined') {
      message = 'Unknown error.';
    }
    showStandardSnackbar(`Failed to deploy workspace! ${message}`);
    throw err;
  }
}

export async function updateWorkspace(userId, workspaceId, workspaceInfo) {
  try {
    await superagent
      .patch(`${EXTENSION_ENDPOINT}/users/${userId}/workspace/${workspaceId}`)
      .withCredentials()
      .send(workspaceInfo);
  } catch (err) {
    let message = err.response?.body?.detail;
    if (typeof message === 'undefined') {
      message = 'Unknown error.';
    }
    showStandardSnackbar(`Failed to update workspace! ${message}`);
  }
}

export async function startWorkspace(userId, workspaceId) {
  try {
    await superagent
      .post(
        `${EXTENSION_ENDPOINT}/users/${userId}/workspace/${workspaceId}:start`
      )
      .withCredentials()
      .send();
  } catch (err) {
    let message = err.response?.body?.detail;
    if (typeof message === 'undefined') {
      message = 'Unknown error.';
    }
    showStandardSnackbar(`Failed to start workspace! ${message}`);
  }
}

export async function getWorkspaceManagerConfig() {
  try {
    const response = await superagent
      .get(`${EXTENSION_ENDPOINT}/config`)
      .withCredentials()
      .send();
    return response.body;
  } catch (err) {
    let message = err.response?.body?.detail;
    if (typeof message === 'undefined') {
      message = 'Unknown error.';
    }
    showStandardSnackbar(`Failed to retrieve workspace config! ${message}`);
    return null;
  }
}

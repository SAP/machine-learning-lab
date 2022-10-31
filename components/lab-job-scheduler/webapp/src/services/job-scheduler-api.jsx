import superagent from 'superagent';

import { EXTENSION_ENDPOINT } from '../utils/config';
import showStandardSnackbar from '../components/showStandardSnackbar';

export async function scheduleJob(projectId, scheduleJobInput) {
  try {
    await superagent
      .post(`${EXTENSION_ENDPOINT}/project/${projectId}/schedule`)
      .withCredentials()
      .send(scheduleJobInput)
      .set('Content-Type', 'application/json');
  } catch (err) {
    let message = err.response?.body?.detail;
    if (typeof message === 'undefined') {
      message = 'Unknown error.';
    }
    showStandardSnackbar(`Failed to schedule job! ${message}`);
    throw err;
  }
}

export async function editScheduledJob(projectId, jobId, scheduleJobInput) {
  try {
    await superagent
      .post(`${EXTENSION_ENDPOINT}/project/${projectId}/schedule/${jobId}`)
      .withCredentials()
      .send(scheduleJobInput)
      .set('Content-Type', 'application/json');
  } catch (err) {
    let message = err.response?.body?.detail;
    if (typeof message === 'undefined') {
      message = 'Unknown error.';
    }
    showStandardSnackbar(`Failed to edit scheduled job! ${message}`);
    throw err;
  }
}

export async function listScheduledJobs(projectId) {
  try {
    const resp = await superagent
      .get(`${EXTENSION_ENDPOINT}/project/${projectId}/schedules`)
      .withCredentials();
    return resp.body;
  } catch (err) {
    let message = err.response?.body?.detail;
    if (typeof message === 'undefined') {
      message = 'Unknown error.';
    }
    showStandardSnackbar(`Failed to get jobs! ${message}`);
    throw err;
  }
}

export async function deleteScheduledJob(projectId, jobId) {
  try {
    await superagent
      .delete(`${EXTENSION_ENDPOINT}/project/${projectId}/schedule/${jobId}`)
      .withCredentials();
  } catch (err) {
    let message = err.response?.body?.detail;
    if (typeof message === 'undefined') {
      message = 'Unknown error.';
    }
    showStandardSnackbar(`Failed to delete job schedule! ${message}`);
    throw err;
  }
}

export async function getExecutorInfo() {
  try {
    const resp = await superagent
      .get(`${EXTENSION_ENDPOINT}/executor/info`)
      .withCredentials();
    return resp.body;
  } catch (err) {
    let message = err.response?.body?.detail;
    if (typeof message === 'undefined') {
      message = 'Unknown error.';
    }
    throw err;
  }
}

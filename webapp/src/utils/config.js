/* eslint-disable import/prefer-default-export */
export const APP_NAME = 'Machine Learning Lab';

export const ENDPOINT =
  process.env.REACT_APP_ENDPOINT === undefined
    ? (
        document.location.origin.toString() +
        document.location.pathname.toString()
      ).replace('/app/', '/api')
    : process.env.REACT_APP_ENDPOINT;

export const DOCUMENTATION_URL = '';
export const API_EXPLORER_URL = `${ENDPOINT}/docs`;

export const ENDPOINTS = {
  research: {
    name: 'research-workspace',
    url: `${ENDPOINT}workspace`,
  },
  userWorkspace: {
    name: 'user-workspace',
    url: `${ENDPOINT}workspace/id/{user}`,
  },
  serviceAdmin: {
    name: 'service-admin',
    url: `${ENDPOINT}service-admin`,
  },
  monitoringDashboard: {
    name: 'monitoring-dashboard',
    url: `${ENDPOINT}netdata`,
  },
  documentation: {
    name: 'documentation',
    url: `${ENDPOINT}docs/`,
  },
  apiExplorer: {
    name: 'api-explorer',
    url: `${ENDPOINT}api-docs/`,
  },
};

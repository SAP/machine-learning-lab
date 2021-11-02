/* eslint-disable import/prefer-default-export */
export const EXTENSION_ENDPOINT =
  process.env.REACT_APP_EXTENSION_ENDPOINT === undefined
    ? '../api'
    : process.env.REACT_APP_EXTENSION_ENDPOINT;

export const CONTAXY_ENDPOINT =
  process.env.REACT_APP_CONTAXY_ENDPOINT === undefined
    ? '/api'
    : process.env.REACT_APP_CONTAXY_ENDPOINT;

export const DEFAULT_WORKSPACE_IMAGE =
  process.env.REACT_APP_DEFAULT_WORKSPACE_IMAGE === undefined
    ? 'mltooling/ml-workspace-minimal'
    : process.env.REACT_APP_DEFAULT_WORKSPACE_IMAGE;

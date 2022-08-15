/* eslint-disable import/prefer-default-export */
export const EXTENSION_ENDPOINT =
  process.env.REACT_APP_EXTENSION_ENDPOINT === undefined
    ? '../api'
    : process.env.REACT_APP_EXTENSION_ENDPOINT;

export const CONTAXY_ENDPOINT =
  process.env.REACT_APP_CONTAXY_ENDPOINT === undefined
    ? ''
    : process.env.REACT_APP_CONTAXY_ENDPOINT;

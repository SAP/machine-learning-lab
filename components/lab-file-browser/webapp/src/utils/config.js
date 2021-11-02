/* eslint-disable import/prefer-default-export */
export const CONTAXY_ENDPOINT =
  process.env.REACT_APP_CONTAXY_ENDPOINT === undefined
    ? '/api'
    : process.env.REACT_APP_CONTAXY_ENDPOINT;

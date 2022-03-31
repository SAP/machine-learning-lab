import React from 'react';

import PropTypes from 'prop-types';

import { Navigate, useLocation } from 'react-router-dom';

/* eslint-disable react/jsx-props-no-spreading */
function DefaultLoginRoute({
  element: Component,
  isAuthenticated,
  componentProps,
}) {
  const location = useLocation();
  if (!isAuthenticated) {
    return <Component {...componentProps} />;
  }
  return <Navigate to="/" state={{ from: location }} />;
}

DefaultLoginRoute.propTypes = {
  element: PropTypes.instanceOf(Object).isRequired,
  isAuthenticated: PropTypes.bool,
  componentProps: PropTypes.instanceOf(Object),
};

DefaultLoginRoute.defaultProps = {
  isAuthenticated: false,
  componentProps: {},
};

export default DefaultLoginRoute;

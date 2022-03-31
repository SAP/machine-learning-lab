import React from 'react';

import { Navigate, useLocation } from 'react-router-dom';
import { PropTypes } from 'prop-types';

/* eslint-disable react/jsx-props-no-spreading */
function PrivateRoute({ element: Component, isAuthenticated, componentProps }) {
  const location = useLocation();

  if (isAuthenticated) {
    return <Component {...componentProps} />;
  }
  return <Navigate to="/login" state={{ from: location }} />;
}

PrivateRoute.propTypes = {
  element: PropTypes.oneOfType([PropTypes.func, PropTypes.object]).isRequired,
  componentProps: PropTypes.instanceOf(Object),
  isAuthenticated: PropTypes.bool,
  useDefaultLogin: PropTypes.bool,
};

PrivateRoute.defaultProps = {
  componentProps: {},
  isAuthenticated: false,
  useDefaultLogin: true,
};

export default PrivateRoute;

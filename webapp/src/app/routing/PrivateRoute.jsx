import React from 'react';

import { Redirect, Route } from 'react-router-dom';
import PropTypes from 'prop-types';

/* eslint-disable react/jsx-props-no-spreading */
function PrivateRoute({
  component: Component,
  isAuthenticated,
  useDefaultLogin,
  componentProps,
  ...rest
}) {
  return (
    <Route
      {...rest}
      render={(props) =>
        isAuthenticated ? (
          <Component {...props} {...componentProps} />
        ) : (
          <Redirect
            to={{ pathname: '/login', state: { from: props.location } }} // eslint-disable-line react/prop-types
          />
        )
      }
    />
  );
}

PrivateRoute.propTypes = {
  component: PropTypes.oneOfType([PropTypes.func, PropTypes.object]).isRequired,
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

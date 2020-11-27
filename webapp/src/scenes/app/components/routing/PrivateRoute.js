import React from 'react';
import { Route, Redirect } from 'react-router-dom';
import { connect } from 'react-redux';
import { mapStateToProps } from '../../../../services/handler/reduxUtils';

// component: Component => renames the obj property component to Component
// The 'isAuthenticated' props value is passed via the ReduxStore (mapStateToProps)
const PrivateRoute = ({ component: Component, isAuthenticated, ...rest }) => (
  <Route
    {...rest}
    render={(props) =>
      isAuthenticated ? (
        <Component {...props} />
      ) : (
        <Redirect
          to={{ pathname: '/login', state: { from: props.location } }}
        />
      )
    }
  />
);

export default connect(mapStateToProps)(PrivateRoute);

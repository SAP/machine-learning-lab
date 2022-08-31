import React from 'react';

import { Route, Routes } from 'react-router-dom';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import Toolbar from '@material-ui/core/Toolbar';

import APP_PAGES, { APP_DRAWER_ITEM_TYPES } from '../../utils/app-pages';
import DefaultLoginRoute from './DefaultLoginRoute';
import GlobalStateContainer from '../store';
import PrivateRoute from './PrivateRoute';

function ContentContainer(props) {
  const { oauthEnabled } = GlobalStateContainer.useContainer();
  const { additionalPages, className, isAuthenticated } = props;
  const routes = [...APP_PAGES, ...additionalPages]
    .filter((item) => item.TYPE === APP_DRAWER_ITEM_TYPES.link)
    .map((item) => {
      const RouteElement = item.REQUIRE_LOGIN
        ? PrivateRoute
        : DefaultLoginRoute;

      return (
        <Route
          key={item.NAME}
          path={item.PATH}
          exact={item.name === 'home'}
          element={
            <RouteElement
              element={item.COMPONENT}
              isAuthenticated={isAuthenticated}
              useDefaultLogin={!oauthEnabled}
              componentProps={item.PROPS}
            />
          }
        />
      );
    });

  return (
    <div className={`${className} root`}>
      {/* Adding toolbar makes the drawer "clip" below the web app's top bar as the Toolbar has the same height */}
      <Toolbar />
      <Routes>{routes}</Routes>
    </div>
  );
}

ContentContainer.propTypes = {
  additionalPages: PropTypes.instanceOf(Array),
  className: PropTypes.string,
  isAuthenticated: PropTypes.bool.isRequired,
};

ContentContainer.defaultProps = {
  additionalPages: [],
  className: '',
};

const StyledContentContainer = styled(ContentContainer)`
  &.root {
    display: flex;
    flex-direction: column;
    width: 100%;
  }
`;

export default StyledContentContainer;

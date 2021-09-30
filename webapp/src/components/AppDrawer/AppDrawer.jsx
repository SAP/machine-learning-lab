import React from 'react';

import PropTypes from 'prop-types';
import styled from 'styled-components';

import Drawer from '@material-ui/core/Drawer';
import List from '@material-ui/core/List';
import Toolbar from '@material-ui/core/Toolbar';

import APP_PAGES from '../../utils/app-pages';
import AppDrawerItem from './AppDrawerItem';

const DRAWER_WIDTH = 230;

function AppDrawer(props) {
  const { additionalPages, className, isAdmin, open } = props;

  const linkItems = [...APP_PAGES, ...additionalPages]
    .filter((item) => item.APP_DRAWER_ITEM && (isAdmin || !item.REQUIRE_ADMIN))
    .sort((item1, item2) => item2.DISPLAY_PRIORITY - item1.DISPLAY_PRIORITY)
    .map((item) => <AppDrawerItem key={item.NAME} item={item} />);

  return (
    <Drawer
      aria-label="appdrawer"
      variant="permanent"
      open={open}
      classes={{ paper: `${className} drawer ${!open ? 'drawerClose' : ''}` }}
    >
      {/* Adding toolbar makes the drawer "clip" below the web app's top bar as the Toolbar has the same height */}
      <Toolbar />
      <div className={`${className} drawerInner`}>
        <List>
          {linkItems}
          {/* Uncomment when UI implementation of plugin installation is completed
          <AppDrawerItem
            item={{ TYPE: 'button', TOOLTIP: 'Add Plugin', NAME: 'Add Plugin' }}
          /> */}
        </List>
      </div>
    </Drawer>
  );
}

AppDrawer.propTypes = {
  additionalPages: PropTypes.instanceOf(Array),
  className: PropTypes.string,
  isAdmin: PropTypes.bool,
  open: PropTypes.bool,
};

AppDrawer.defaultProps = {
  additionalPages: [],
  className: '',
  isAdmin: false,
  open: false,
};

const StyledAppDrawer = styled(AppDrawer)`
  ${({ theme }) => `
    &.drawer {
        position: relative;
        height: 100%;
        min-height: 100vh;
        width: ${DRAWER_WIDTH}px;
        transition: ${theme.transitions.create('width', {
          easing: theme.transitions.easing.sharp,
          duration: theme.transitions.duration.enteringScreen,
        })}
    }

    &.drawerInner {
      // Make the items inside not wrap when transitioning:
      width: ${DRAWER_WIDTH}px;
    }

    &.drawerClose {
      overflow-X: hidden;
      transition: ${theme.transitions.create('width', {
        easing: theme.transitions.easing.sharp,
        duration: theme.transitions.duration.leavingScreen,
      })};
      width: ${theme.spacing(7)}px;
      ${theme.breakpoints.up('sm')} {
          width: ${theme.spacing(9)}px;
      }
    }
    `}
`;

export default StyledAppDrawer;

import React from 'react';

import PropTypes from 'prop-types';
import styled from 'styled-components';

import IconButton from '@material-ui/core/IconButton';
import MaterialUiAppBar from '@material-ui/core/AppBar';
import MenuIcon from '@material-ui/icons/Menu';
import Toolbar from '@material-ui/core/Toolbar';
import Typography from '@material-ui/core/Typography';

import { APP_NAME } from '../../utils/config';
import { useProjectSelector } from '../../utils/app-utils';
import GlobalStateContainer from '../../app/store';
import ProjectSelector from './ProjectSelector';
import UserMenu from './UserMenu';

function AppBar(props) {
  const { activeProject, projects, user } = GlobalStateContainer.useContainer();
  const onProjectSelect = useProjectSelector();
  const { className, isAuthenticated, onDrawerOpen } = props;

  const menuIconElement = (
    <IconButton
      color="inherit"
      aria-label="open drawer"
      onClick={onDrawerOpen}
      className={`${className} menuButton`}
    >
      <MenuIcon />
    </IconButton>
  );

  const projectSelectorElement = (
    <ProjectSelector
      activeProject={activeProject}
      projects={projects}
      onProjectChange={onProjectSelect}
    />
  );

  const userNameElement = user ? (
    <Typography className={`${className} user`}>{user.name}</Typography>
  ) : (
    false
  );

  return (
    <MaterialUiAppBar className={`${className} root`}>
      <Toolbar disableGutters>
        {isAuthenticated ? menuIconElement : false}
        <Typography
          variant="h6"
          color="inherit"
          className={`${className} title`}
        >
          {APP_NAME}
        </Typography>
        {isAuthenticated ? projectSelectorElement : false}
        {isAuthenticated ? userNameElement : false}
        <UserMenu
          isAuthenticated={isAuthenticated}
          user={user}
          activeProject={activeProject}
        />
      </Toolbar>
    </MaterialUiAppBar>
  );
}

AppBar.propTypes = {
  className: PropTypes.string, // passed by styled-components
  isAuthenticated: PropTypes.bool,
  onDrawerOpen: PropTypes.func.isRequired,
};

AppBar.defaultProps = {
  className: '',
  isAuthenticated: false,
};

const StyledAppBar = styled(AppBar)`
  &.root {
    z-index: ${(props) => props.theme.zIndex.drawer + 1};
  }

  &.title {
    flex: 1;
    margin-left: ${(props) => (props.isAuthenticated ? '0px' : '24px')};
    font-weight: 300;
    text-align: left;
  }
  &.menuButton {
    margin-right: 36px;
    margin-left: 12px;
  }
`;

export default StyledAppBar;

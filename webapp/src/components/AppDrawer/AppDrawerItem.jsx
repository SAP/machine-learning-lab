import React, { useMemo } from 'react';

import { Link, useLocation } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import Divider from '@material-ui/core/Divider';
import Icon from '@material-ui/core/Icon';
import IconButton from '@material-ui/core/IconButton';
import ListItem from '@material-ui/core/ListItem';
import ListItemIcon from '@material-ui/core/ListItemIcon';
import ListItemSecondaryAction from '@material-ui/core/ListItemSecondaryAction';
import ListItemText from '@material-ui/core/ListItemText';
import Tooltip from '@material-ui/core/Tooltip';

import { useShowAppDialog } from '../../app/AppDialogServiceProvider';
import AddPluginDialog from '../Dialogs/AddPluginDialog';

function AppDrawerItem(props) {
  const { t } = useTranslation();
  const location = useLocation();
  const { className, item } = props;
  const showAppDialog = useShowAppDialog();

  const path = item.PATH ? item.PATH : '';

  // for why to use useMemo and React.forwardRef see documentation: https://material-ui.com/guides/composition/#caveat-with-inlining
  const CustomLink = useMemo(
    () =>
      React.forwardRef((linkProps, ref) => (
        <Link ref={ref} to={path} {...linkProps} replace /> // eslint-disable-line react/jsx-props-no-spreading
      )),
    [path]
  );

  // TODO: split different types into different components
  let element = null;

  if (item.TYPE === 'link') {
    const isActive = location.pathname === item.PATH;

    const isActiveClassName = isActive
      ? `${className} isActive`
      : `${className} isInactive`;

    let newTabButton = null;
    if (item.NEW_TAB_LINK) {
      const newTabText = t('new_tab');
      newTabButton = (
        <Tooltip
          title={<div className={`${className} tooltip`}>{newTabText}</div>}
          placement="bottom"
        >
          <IconButton
            aria-label={newTabText}
            href={item.NEW_TAB_LINK}
            target="_blank"
            rel="noopener"
          >
            <Icon className={`${className} tooltipIcon`}>open_in_new</Icon>
          </IconButton>
        </Tooltip>
      );
    }

    element = (
      <ListItem
        component={CustomLink}
        className={`${className} listItem`}
        button
      >
        <ListItemIcon className={isActiveClassName}>
          <Icon>{item.ICON}</Icon>
        </ListItemIcon>
        <ListItemText primaryTypographyProps={{ className: isActiveClassName }}>
          {item.NAME}
        </ListItemText>
        {newTabButton && (
          <ListItemSecondaryAction>{newTabButton}</ListItemSecondaryAction>
        )}
      </ListItem>
    );
  }

  if (item.TYPE === 'divider') {
    element = (
      <div>
        <Divider component="div" />
      </div>
    );
  }

  if (item.TYPE === 'button') {
    // TODO: use dynamic information for content etc.
    element = (
      <>
        <ListItem
          button
          onClick={() => showAppDialog(AddPluginDialog)}
          className={`${className} listItem`}
        >
          <Tooltip
            title={<div className={`${className} tooltip`}>{item.TOOLTIP}</div>}
            placement="bottom"
          >
            <ListItemIcon>
              <Icon className={`${className} tooltipIcon addIcon`}>add</Icon>
            </ListItemIcon>
          </Tooltip>
          <ListItemText
            primaryTypographyProps={{ className: `${className} isInactive` }}
          >
            {item.NAME}
          </ListItemText>
        </ListItem>
      </>
    );
  }

  return <>{element}</>;
}

AppDrawerItem.propTypes = {
  className: PropTypes.string,
  item: PropTypes.instanceOf(Object).isRequired,
};

AppDrawerItem.defaultProps = {
  className: '',
};

const StyledAppDrawerItem = styled(AppDrawerItem)`
  &.isActive {
    color: #3f51b5;
    font-weight: 500;
  }

  &.isInactive {
    color: rgba(0, 0, 0, 0.54);
  }

  &.tooltip {
    width: 35px;
    font-size: 0.525rem;
  }

  &.tooltipIcon {
    font-size: 18px;
  }

  &.addIcon {
    padding-left: 3px;
  }

  @media (min-width: 600px) {
    &.listItem {
      padding-left: 24px;
    }
  }
`;

export default StyledAppDrawerItem;

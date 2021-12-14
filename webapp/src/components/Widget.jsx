import React from 'react';

import PropTypes from 'prop-types';
import styled from 'styled-components';

import Icon from '@material-ui/core/Icon';
import Link from '@material-ui/core/Link';
import Typography from '@material-ui/core/Typography';

import '../assets/colors.css';

function Widget(props) {
  const { className, classes, color, icon, link, name, value } = props;

  /* eslint-disable react/jsx-props-no-spreading */
  const ContainerElement = link
    ? ({ children, ..._props }) => (
        <Link to={link} {..._props}>
          {children}
        </Link>
      )
    : ({ children, ..._props }) => <div {..._props}>{children}</div>;
  /* eslint-enable react/jsx-props-no-spreading */

  return (
    <ContainerElement
      className={`${className} root hover-expand-effect bg-${color} ${classes.root}`}
    >
      <div className={`${className} iconContainer`}>
        <Icon className={`${className} icon widgetText`}>{icon}</Icon>
      </div>
      <div className={`${className} content widgetText`}>
        <Typography variant="body1">{name}</Typography>
        <Typography variant="h6">{value}</Typography>
      </div>
    </ContainerElement>
  );
}

Widget.propTypes = {
  className: PropTypes.string,
  classes: PropTypes.instanceOf(Object),
  color: PropTypes.string,
  icon: PropTypes.string,
  link: PropTypes.string,
  name: PropTypes.string.isRequired,
  value: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
};

Widget.defaultProps = {
  className: '',
  classes: {},
  color: 'white',
  icon: '',
  link: '',
};

const StyledWidget = styled(Widget)`
  &.root {
    position: relative;
    display: flex;
    height: 80px;
    overflow: hidden;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.2);
    cursor: default;
  }

  &.iconContainer {
    width: 80px;
    text-align: center;
    background-color: rgba(0, 0, 0, 0.12);
  }

  &.icon {
    position: relative;
    top: 16px;
    font-size: 50px;
  }

  &.content {
    display: inline-block;
    padding: 18px 7px 10px 7px;
  }

  &.widgetText {
    color: white;
  }

  /* adds ripple effect */
  &.hover-expand-effect:after {
    position: absolute;
    left: 80px; /* same as iconContainer-width */
    width: 0;
    height: 100%;
    color: transparent;
    background-color: rgba(0, 0, 0, 0.05);
    -moz-transition: all 0.95s;
    -o-transition: all 0.95s;
    -webkit-transition: all 0.95s;
    transition: all 0.95s;
    content: '.';
  }

  &.hover-expand-effect:hover:after {
    width: 100%;
  }
`;

export default StyledWidget;

import React from 'react';

import PropTypes from 'prop-types';

import styled from 'styled-components';

import Grid from '@material-ui/core/Grid';
import Widget from './Widget';

function WidgetsGrid(props) {
  const { className, children, spacing } = props;
  const columnSize = 12 / children.length <= 3 ? 3 : 4;
  const widgets = !Array.isArray(children)
    ? children
    : children.map((child) => (
        <Grid
          key={child.props.name}
          item
          xs={12}
          sm={6}
          md={columnSize}
          lg={columnSize}
        >
          {child}
        </Grid>
      ));

  return (
    <Grid container spacing={spacing} className={`${className} root`}>
      {widgets}
    </Grid>
  );
}

WidgetsGrid.propTypes = {
  children: PropTypes.oneOfType([
    PropTypes.shape({
      type: PropTypes.oneOf([Widget]),
    }),
    PropTypes.arrayOf(
      PropTypes.shape({
        type: PropTypes.oneOf([Widget]),
      })
    ),
  ]).isRequired,
  className: PropTypes.string,
  spacing: PropTypes.number,
};

WidgetsGrid.defaultProps = {
  className: '',
  spacing: 3,
};

const StyledWidgetsGrid = styled(WidgetsGrid)`
  &.root {
    margin-bottom: 8px;
  }
`;

export default StyledWidgetsGrid;

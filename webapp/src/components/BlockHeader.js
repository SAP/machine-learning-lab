import React, { Component } from 'react';
import Typography from '@material-ui/core/Typography';

var styles = {
  marginBottom: '16px',
  marginTop: '16px',
  textTransform: 'uppercase',
};

class BlockHeader extends Component {
  render() {
    return (
      <Typography style={styles} variant="subtitle1">
        {' '}
        {this.props.name}{' '}
      </Typography>
    );
  }
}

export default BlockHeader;

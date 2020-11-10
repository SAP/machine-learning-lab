import React, { Component } from 'react';
import Icon from '@material-ui/core/Icon';
import IconButton from '@material-ui/core/IconButton';
import PropTypes from 'prop-types';
import Tooltip from '@material-ui/core/Tooltip';

class DownloadItemButton extends Component {
  render() {
    return (
      <Tooltip title="Download" placement="bottom">
        <IconButton href={this.props.downloadUrl} target="_blank">
          <Icon>file_download</Icon>
        </IconButton>
      </Tooltip>
    );
  }
}

DownloadItemButton.propTypes = {
  downloadUrl: PropTypes.string.isRequired,
};

export default DownloadItemButton;

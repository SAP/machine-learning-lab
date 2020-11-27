import React, { Component } from 'react';
import IconButton from '@material-ui/core/IconButton';
import Icon from '@material-ui/core/Icon';
import ReactJson from 'react-json-view';
import Tooltip from '@material-ui/core/Tooltip';

import CustomDialog from '../../../components/CustomDialog';

class DisplayJsonButton extends Component {
  state = {
    open: false,
  };

  handleClickOpen = () => {
    this.setState({ open: true });
  };

  handleClose = () => {
    this.setState({ open: false });
  };

  render() {
    const title = 'View JSON for ' + this.props.projName;
    const contentText = '';
    const hideCancelBtn = true;
    const primaryActionBtnDisabled = false;
    const primaryActionBtnLabel = 'Close';
    const customComponent = <ReactJson src={this.props.jsonObj} />;

    return (
      <div>
        <Tooltip title="Service Configuration" placement="bottom">
          <IconButton onClick={this.handleClickOpen}>
            <Icon>info</Icon>
          </IconButton>
        </Tooltip>

        <CustomDialog
          open={this.state.open}
          title={title}
          contentText={contentText}
          hideCancelBtn={hideCancelBtn}
          primaryActionBtnDisabled={primaryActionBtnDisabled}
          primaryActionBtnLabel={primaryActionBtnLabel}
          handleRequestClose={this.handleClose}
          handlePrimaryAction={this.handleClose}
          CustomComponent={customComponent}
        />
      </div>
    );
  }
}

export default DisplayJsonButton;

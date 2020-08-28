import React, { Component } from "react";
import Icon from "@material-ui/core/Icon";
import IconButton from "@material-ui/core/IconButton";
import PropTypes from "prop-types";
import ReactJson from "react-json-view";
import Tooltip from "@material-ui/core/Tooltip";

import CustomDialog from "../../CustomDialog";

class InfoButton extends Component {
  state = {
    open: false
  };

  handleClickOpen = () => {
    this.setState({ open: true });
  };

  handleClose = () => {
    this.setState({ open: false });
  };

  render() {
    const title = "View JSON for job";
    const contentText = "";
    const hideCancelBtn = true;
    const primaryActionBtnDisabled = false;
    const primaryActionBtnLabel = "Close";
    const customComponent = <ReactJson src={this.props.jsonObj} />;

    return (
      <div style={{ display: "inline" }}>
        <Tooltip title="Info" placement="bottom">
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

InfoButton.propTypes = {
  jsonObj: PropTypes.object.isRequired
};

export default InfoButton;

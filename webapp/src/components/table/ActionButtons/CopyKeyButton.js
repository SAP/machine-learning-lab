import React, { Component } from "react";
import { toast } from "react-toastify";
import Icon from "@material-ui/core/Icon";
import IconButton from "@material-ui/core/IconButton";
import PropTypes from "prop-types";
import Tooltip from "@material-ui/core/Tooltip";

import * as Parser from "../../../services/handler/parser";

class CopyKeyButton extends Component {

  onCellClick = itemKey => {
    Parser.setClipboardText(itemKey);
    if (toast.isActive(this.toastId)) {
      toast.dismiss(this.toastId);
    }
    this.toastId = toast.info("Copied to Clipboard");
  };
  //() => this.props.onCopy(this.props.copyKey)}
  render() {
    return (
      <Tooltip title="Copy" placement="bottom">
        <IconButton onClick={() => this.onCellClick(this.props.copyKey)}>
          <Icon>content_copy</Icon>
        </IconButton>
      </Tooltip>
    );
  }
}

CopyKeyButton.propTypes = {
  copyKey: PropTypes.string.isRequired,
};

export default CopyKeyButton;

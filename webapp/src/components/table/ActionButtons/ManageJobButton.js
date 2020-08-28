import React, { Component } from "react";
import Icon from "@material-ui/core/Icon";
import IconButton from "@material-ui/core/IconButton";
import PropTypes from "prop-types";
import Tooltip from "@material-ui/core/Tooltip";

class ManageJobButton extends Component {
  render() {
    return (
      <Tooltip title="Manage" placement="bottom">
        <IconButton href={this.props.url} target="_blank">
          <Icon>settings</Icon>
        </IconButton>
      </Tooltip>
    );
  }
}

ManageJobButton.propTypes = {
  url: PropTypes.string.isRequired
};

export default ManageJobButton;

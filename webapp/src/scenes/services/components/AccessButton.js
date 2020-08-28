import React, { Component } from "react";
import PropTypes from "prop-types";

// material-ui components
import Tooltip from "@material-ui/core/Tooltip";
import Button from "@material-ui/core/Button";
import Dialog from "@material-ui/core/Dialog";
import DialogActions from "@material-ui/core/DialogActions";
import DialogTitle from "@material-ui/core/DialogTitle";
import List from "@material-ui/core/List";

import { getServiceUrl } from "../../../services/client/ml-lab-api";
import { ListItem, Typography } from "@material-ui/core";

class AccessButton extends Component {
  state = {
    open: false
  };

  handleClickOpen = () => {
    if (this.props.exposedPorts.length === 1) {
      window.open(
        getServiceUrl(
          this.props.projectId,
          this.props.serviceName,
          this.props.exposedPorts[0]
        ),
        "_blank"
      );
    } else {
      this.setState({ open: true });
    }
  };

  handleClickClose = () => {
    this.setState({ open: false });
  };

  handleListItemClick = (port) => {
    window.open(
      getServiceUrl(
        this.props.projectId,
        this.props.serviceName,
        port
      ),
      "_blank"
    );
    this.setState({ open: false });
  }

  render() {
    const title = "Service Ports";

    return (
      <div style={{ display: "inline" }}>
        <Tooltip title={title} placement="bottom">
          <Button onClick={this.handleClickOpen}>ACCESS</Button>
        </Tooltip>

        <Dialog
          open={this.state.open}
          onClose={this.handleClose}
          scroll="paper"
          aria-labelledby="scroll-dialog-title"
          style={{ whiteSpace: "pre-wrap" }}
        >
          <DialogTitle id="scroll-dialog-title">{title}</DialogTitle>

          <List>
            {this.props.exposedPorts.map(port => (
                <ListItem button onClick={() => this.handleListItemClick(port)} key={port}>
                  <Typography>{port}</Typography>
                </ListItem>
            ))}
          </List>
          <DialogActions>
            <Button onClick={this.handleClickClose} color="primary">
              Ok
            </Button>
          </DialogActions>
        </Dialog>
      </div>
    );
  }
}

AccessButton.propTypes = {
  projectId: PropTypes.string.isRequired,
  serviceName: PropTypes.string.isRequired,
  exposedPorts: PropTypes.array.isRequired
};

export default AccessButton;

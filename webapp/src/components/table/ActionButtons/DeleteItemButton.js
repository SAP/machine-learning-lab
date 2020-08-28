import React, { Component } from "react";
import { connect } from "react-redux";
import Icon from "@material-ui/core/Icon";
import IconButton from "@material-ui/core/IconButton";
import PropTypes from "prop-types";
import Tooltip from "@material-ui/core/Tooltip";

//base components
import CustomDialog from "../../../components/CustomDialog";

import * as ReduxUtils from "../../../services/handler/reduxUtils";

class DeleteItemButton extends Component {
  constructor(props) {
    super(props);
    this.state = {
      open: false,
      bKeepLatestVersion: false
    };

    this.onOpenDeleteDialog = this.onOpenDeleteDialog.bind(this);
    this.handleRequestClose = this.handleRequestClose.bind(this);
    this.handleCheckChange = this.handleCheckChange.bind(this);
  }

  handleCheckChange(event) {
    this.setState({
      bKeepLatestVersion: event.target.checked
    });
  }

  onOpenDeleteDialog(item) {
    this.setState({ open: true });
  }

  handleRequestClose(item) {
    this.setState({ open: false });
  }

  render() {
    //CustomDialog
    const title = "Delete Item";
    const cancelBtnDisabled = false;
    const primaryActionBtnDisabled = false;
    const primaryActionBtnLabel = "Delete";
    const onDeleteText =
      "Do you want to delete the item " + this.props.item.name + " ?";

    return (
      <div style={{ display: "inline" }}>
        <Tooltip title="Delete" placement="bottom">
          <IconButton onClick={this.onOpenDeleteDialog}>
            <Icon>delete</Icon>
          </IconButton>
        </Tooltip>
        <CustomDialog
          open={this.state.open}
          title={title}
          contentText={onDeleteText}
          cancelBtnDisabled={cancelBtnDisabled}
          primaryActionBtnDisabled={primaryActionBtnDisabled}
          primaryActionBtnLabel={primaryActionBtnLabel}
          handleRequestClose={this.handleRequestClose}
          handlePrimaryAction={e => this.props.onItemDelete(this.props.item)}
        />
      </div>
    );
  }
}

DeleteItemButton.propTypes = {
  item: PropTypes.object.isRequired,
  onItemDelete: PropTypes.func.isRequired,
  currentProject: PropTypes.string.isRequired
};

export default connect(ReduxUtils.mapStateToProps)(DeleteItemButton);

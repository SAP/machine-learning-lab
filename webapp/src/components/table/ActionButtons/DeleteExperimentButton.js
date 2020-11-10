import React, { Component } from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';

// material-ui components
import { withStyles } from '@material-ui/core/styles';
import Icon from '@material-ui/core/Icon';
import IconButton from '@material-ui/core/IconButton';
import green from '@material-ui/core/colors/green';

import Tooltip from '@material-ui/core/Tooltip';

//base components
import CustomDialog from '../../../components/CustomDialog';

import * as ReduxUtils from '../../../services/handler/reduxUtils';

const styles = (theme) => ({
  checked: {
    color: green[500],
    '& + $bar': {
      backgroundColor: green[500],
    },
  },
  bar: {},
});

class DeleteExperimentButton extends Component {
  constructor(props) {
    super(props);
    this.state = {
      open: false,
      bKeepLatestVersion: false,
    };

    this.onOpenDeleteDialog = this.onOpenDeleteDialog.bind(this);
    this.handleRequestClose = this.handleRequestClose.bind(this);
    this.onDeleteFile = this.onDeleteFile.bind(this);
    this.handleCheckChange = this.handleCheckChange.bind(this);
  }

  handleCheckChange(event) {
    this.setState({
      bKeepLatestVersion: event.target.checked,
    });
  }

  onOpenDeleteDialog() {
    this.setState({ open: true });
  }

  handleRequestClose() {
    this.setState({ open: false });
  }

  // TODO: remove unused param
  onDeleteFile() {
    this.setState({
      open: false,
    });

    this.props.onItemDelete();
  }

  render() {
    //CustomDialog
    const title = 'Delete Experiment';
    const cancelBtnDisabled = false;
    const primaryActionBtnDisabled = false;
    const primaryActionBtnLabel = 'Delete';
    const onDeleteText = 'Do you want to delete this experiment?';

    return (
      <div style={{ display: 'inline' }}>
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
          handlePrimaryAction={(e) => this.onDeleteFile()}
        />
      </div>
    );
  }
}

DeleteExperimentButton.propTypes = {
  onItemDelete: PropTypes.func.isRequired,
};

export default connect(ReduxUtils.mapStateToProps)(
  withStyles(styles)(DeleteExperimentButton)
);

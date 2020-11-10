import React, { Component } from 'react';
import { connect } from 'react-redux';
import { toast } from 'react-toastify';
import PropTypes from 'prop-types';

// material-ui components
import { withStyles } from '@material-ui/core/styles';
import Icon from '@material-ui/core/Icon';
import IconButton from '@material-ui/core/IconButton';
import green from '@material-ui/core/colors/green';
import FormGroup from '@material-ui/core/FormGroup';
import FormControlLabel from '@material-ui/core/FormControlLabel';
import Switch from '@material-ui/core/Switch';
import Tooltip from '@material-ui/core/Tooltip';

//base components
import CustomDialog from '../../../components/CustomDialog';
import * as ProcessToast from '../../../components/ProcessToast';

import * as ReduxUtils from '../../../services/handler/reduxUtils';
import {
  projectsApi,
  getDefaultApiCallback,
  toastErrorMessage,
} from '../../../services/client/ml-lab-api';

const styles = (theme) => ({
  checked: {
    color: green[500],
    '& + $bar': {
      backgroundColor: green[500],
    },
  },
  bar: {},
});

const VERSION_SUFFIX_REGEX = /\.v[0-9]+$/;

class DeleteFileButton extends Component {
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

  onOpenDeleteDialog(item) {
    this.setState({ open: true });
  }

  handleRequestClose(item) {
    this.setState({ open: false });
  }

  onDeleteFile(item) {
    var toastID = ProcessToast.showProcessToast('File will be deleted...');
    this.setState({
      open: false,
    });

    // strip the version suffix from the file key before calling the delete API
    let fileKey = item.key;
    const versionSuffixPosition = fileKey.search(VERSION_SUFFIX_REGEX);
    if (versionSuffixPosition > -1) {
      fileKey = fileKey.substr(0, versionSuffixPosition);
    }

    projectsApi.deleteFile(
      this.props.currentProject,
      fileKey,
      { keepLatestVersions: this.state.bKeepLatestVersion ? 1 : 0 }, // the last n-versions can be kept; here n is set to 1
      getDefaultApiCallback(
        () => {
          toast.dismiss(toastID);
          toast.success('File deleted');
          this.props.onItemDelete();
        },
        ({ error }) => {
          toast.dismiss(toastID);
          toastErrorMessage('Delete File: ', error);
        }
      )
    );
  }

  render() {
    const { classes } = this.props;

    //CustomDialog
    const title = 'Delete File';
    const cancelBtnDisabled = false;
    const primaryActionBtnDisabled = false;
    const primaryActionBtnLabel = 'Delete';
    const onDeleteText =
      'Do you want to delete the File ' + this.props.item.name + ' ?';

    const checkKeepLatestVersion = (
      <FormGroup row>
        <FormControlLabel
          control={
            <Switch
              checked={this.state.bKeepLatestVersion}
              onChange={(e) => this.handleCheckChange(e)}
              value="keepLatestVersion"
              classes={{
                checked: classes.checked,
                bar: classes.bar,
              }}
            />
          }
          label="Keep latest version"
        />
      </FormGroup>
    );

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
          handlePrimaryAction={(e) => this.onDeleteFile(this.props.item)}
          CustomComponent={checkKeepLatestVersion}
        />
      </div>
    );
  }
}

DeleteFileButton.propTypes = {
  item: PropTypes.object.isRequired,
  onItemDelete: PropTypes.func.isRequired,
  currentProject: PropTypes.string.isRequired,
};

export default connect(ReduxUtils.mapStateToProps)(
  withStyles(styles)(DeleteFileButton)
);

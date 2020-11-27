import React from 'react';
import PropTypes from 'prop-types';
import IconButton from '@material-ui/core/IconButton';
import Icon from '@material-ui/core/Icon';
import Tooltip from '@material-ui/core/Tooltip';
import { toast } from 'react-toastify';

import CustomDialog from '../../../components/CustomDialog';
import {
  projectsApi,
  getDefaultApiCallback,
  toastErrorMessage,
} from '../../../services/client/ml-lab-api';
import * as ProcessToast from '../../../components/ProcessToast';

class DeleteServiceButton extends React.Component {
  state = {
    open: false,
  };

  handleClickOpen = () => {
    this.setState({ open: true });
  };

  handleClose = () => {
    this.setState({ open: false });
  };

  deleteService = () => {
    let { project, serviceName, onServiceDeleted } = this.props;
    let toastId = ProcessToast.showProcessToast('Service will be deleted...');

    projectsApi.deleteService(
      project,
      serviceName,
      {},
      getDefaultApiCallback(
        () => {
          toast.dismiss(toastId);
          toast.success('Service ' + serviceName + ' deleted.');
          onServiceDeleted();
        },
        ({ error }) => {
          toast.dismiss(toastId);
          toastErrorMessage('Error when deleting service : ', error);
        }
      )
    );
    this.handleClose();
  };

  render() {
    const title = 'Do you really want to delete the service?';
    const primaryActionBtnLabel = 'Yes';

    return (
      <div>
        <Tooltip title="Delete" placement="bottom">
          <IconButton onClick={this.handleClickOpen}>
            <Icon>delete</Icon>
          </IconButton>
        </Tooltip>

        <CustomDialog
          open={this.state.open}
          title={title}
          contentText={''}
          hideCancelBtn={false}
          primaryActionBtnDisabled={false}
          primaryActionBtnLabel={primaryActionBtnLabel}
          handleRequestClose={this.handleClose}
          handlePrimaryAction={this.deleteService}
        />
      </div>
    );
  }
}

DeleteServiceButton.propTypes = {
  project: PropTypes.string.isRequired,
  serviceName: PropTypes.string.isRequired,
  onServiceDeleted: PropTypes.func,
};

export default DeleteServiceButton;

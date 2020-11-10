import React, { Component } from 'react';
import PropTypes from 'prop-types';

// material-ui components
import Icon from '@material-ui/core/Icon';
import IconButton from '@material-ui/core/IconButton';
import Tooltip from '@material-ui/core/Tooltip';
import Button from '@material-ui/core/Button';
import Dialog from '@material-ui/core/Dialog';
import DialogActions from '@material-ui/core/DialogActions';
import DialogContent from '@material-ui/core/DialogContent';
import DialogContentText from '@material-ui/core/DialogContentText';
import DialogTitle from '@material-ui/core/DialogTitle';
import { withStyles } from '@material-ui/core/styles';

import {
  projectsApi,
  getDefaultApiCallback,
} from '../../../services/client/ml-lab-api';

const styles = (theme) => ({
  dialogWidth: {
    maxWidth: '72%',
  },
});

class LogsButton extends Component {
  state = {
    open: false,
    logs: '',
  };

  log = (project, id, callback) => {
    this.props.type === 'job'
      ? projectsApi.getJobLogs(project, id, {}, callback)
      : projectsApi.getServiceLogs(project, id, {}, callback);
  };

  handleLoadingLogs = () => {
    const { project, id } = this.props;
    this.log(
      project,
      id,
      getDefaultApiCallback(({ result }) => {
        const logs = result.data === '' ? 'No logs available' : result.data;
        this.setState({ logs: logs });
      })
    );
  };

  handleClickOpen = () => {
    this.setState({ open: true });
    this.handleLoadingLogs();
  };

  handleClose = () => {
    this.setState({ open: false });
  };

  render() {
    const title = 'Logs';
    const { classes } = this.props;

    return (
      <div style={{ display: 'inline' }}>
        <Tooltip title="Logs" placement="bottom">
          <IconButton onClick={this.handleClickOpen}>
            <Icon>assignment</Icon>
          </IconButton>
        </Tooltip>

        <Dialog
          open={this.state.open}
          onClose={this.handleClose}
          classes={{
            paper: classes.dialogWidth,
          }}
          scroll="paper"
          aria-labelledby="scroll-dialog-title"
          style={{ whiteSpace: 'pre-wrap' }}
        >
          <DialogTitle id="scroll-dialog-title">{title}</DialogTitle>
          <DialogContent>
            <DialogContentText style={{ fontSize: '0.75rem' }}>
              {this.state.logs}
            </DialogContentText>
          </DialogContent>
          <DialogActions>
            <Button onClick={this.handleLoadingLogs} color="secondary">
              Reload
            </Button>
            <Button onClick={this.handleClose} color="primary">
              Cancel
            </Button>
          </DialogActions>
        </Dialog>
      </div>
    );
  }
}

LogsButton.propTypes = {
  project: PropTypes.string.isRequired,
  id: PropTypes.string.isRequired,
  type: PropTypes.oneOf(['job', 'service']).isRequired,
};

export default withStyles(styles)(LogsButton);

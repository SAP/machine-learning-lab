import React, { useEffect, useState } from 'react';

import PropTypes from 'prop-types';

import Button from '@material-ui/core/Button';
import Dialog from '@material-ui/core/Dialog';
import DialogActions from '@material-ui/core/DialogActions';
import DialogContent from '@material-ui/core/DialogContent';
import DialogContentText from '@material-ui/core/DialogContentText';
import DialogTitle from '@material-ui/core/DialogTitle';

import { servicesApi } from '../../services/contaxy-api';

function LogsDialog(props) {
  const { onClose, title, projectId, serviceId } = props;

  const [logs, setLogs] = useState(null);

  useEffect(() => {
    servicesApi.getServiceLogs(projectId, serviceId).then((l) => {
      setLogs(l);
    });
  }, [projectId, serviceId]);

  const contentElement = (
    <DialogContentText style={{ whiteSpace: 'pre-line' }}>
      {logs}
    </DialogContentText>
  );

  const onRefresh = async () => {
    setLogs(await servicesApi.getServiceLogs(projectId, serviceId));
  };

  return (
    <Dialog open>
      <DialogTitle>{title}</DialogTitle>
      <DialogContent>{contentElement}</DialogContent>
      <DialogActions>
        <Button onClick={onRefresh} color="primary">
          REFRESH
        </Button>
        <Button onClick={onClose} color="primary">
          CLOSE
        </Button>
      </DialogActions>
    </Dialog>
  );
}

LogsDialog.propTypes = {
  title: PropTypes.string,
  onClose: PropTypes.func.isRequired,
  projectId: PropTypes.string.isRequired,
  serviceId: PropTypes.string.isRequired,
};

LogsDialog.defaultProps = {
  title: '',
};

export default LogsDialog;

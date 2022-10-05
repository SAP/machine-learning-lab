import React, { useEffect, useState } from 'react';

import PropTypes from 'prop-types';

import Button from '@material-ui/core/Button';
import Checkbox from '@material-ui/core/Checkbox';
import Dialog from '@material-ui/core/Dialog';
import DialogActions from '@material-ui/core/DialogActions';
import DialogContent from '@material-ui/core/DialogContent';
import DialogContentText from '@material-ui/core/DialogContentText';
import DialogTitle from '@material-ui/core/DialogTitle';
import FormControlLabel from '@material-ui/core/FormControlLabel';
import FormGroup from '@material-ui/core/FormGroup';

import { jobsApi, servicesApi } from '../../services/contaxy-api';

function LogsDialog(props) {
  const { onClose, title, projectId, id, type } = props;

  const [logs, setLogs] = useState(null);
  const [checked, setChecked] = useState(false);

  useEffect(() => {
    if (type === 'service') {
      servicesApi.getServiceLogs(projectId, id).then((l) => {
        setLogs(l);
      });
    } else if (type === 'job') {
      jobsApi.getJobLogs(projectId, id).then((l) => {
        setLogs(l);
      });
    }
  }, [projectId, id, type]);

  const contentElement = (
    <DialogContentText style={{ whiteSpace: 'pre-line' }}>
      {logs}
    </DialogContentText>
  );

  const onRefresh = async () => {
    if (type === 'service') {
      setLogs(await servicesApi.getServiceLogs(projectId, id));
    } else if (type === 'job') {
      setLogs(await jobsApi.getJobLogs(projectId, id));
    }
  };

  const handleCheck = (event) => {
    setChecked(event.target.checked);
  };

  useEffect(() => {
    let intervalId = null;
    if (checked) {
      intervalId = setInterval(async () => {
        if (type === 'service') {
          setLogs(await servicesApi.getServiceLogs(projectId, id));
        } else if (type === 'job') {
          setLogs(await jobsApi.getJobLogs(projectId, id));
        }
      }, 5000);
    }

    return () => {
      clearInterval(intervalId);
    };
  }, [checked, projectId, id, type]);

  return (
    <Dialog open>
      <DialogTitle>{title}</DialogTitle>
      <DialogContent>{contentElement}</DialogContent>
      <DialogActions>
        <FormGroup>
          <FormControlLabel
            control={<Checkbox checked={checked} onChange={handleCheck} />}
            label="Refresh automatically every 5 seconds"
          />
        </FormGroup>
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
  id: PropTypes.string.isRequired,
  type: PropTypes.oneOf(['service', 'job']).isRequired,
};

LogsDialog.defaultProps = {
  title: '',
};

export default LogsDialog;

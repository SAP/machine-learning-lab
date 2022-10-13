import React, { useState } from 'react';

import PropTypes from 'prop-types';
import styled from 'styled-components';

import Button from '@material-ui/core/Button';
import Dialog from '@material-ui/core/Dialog';
import DialogActions from '@material-ui/core/DialogActions';
import DialogContent from '@material-ui/core/DialogContent';
import DialogContentText from '@material-ui/core/DialogContentText';
import DialogTitle from '@material-ui/core/DialogTitle';
import TextField from '@material-ui/core/TextField';
import Typography from '@material-ui/core/Typography';

import KeyValueInputs from './KeyValueInputs';
import ValueInputs from './ValueInputs';

const cron = require('cron-validator');

const VALID_IMAGE_NAME = new RegExp('[^a-zA-Z0-9-_:/.]');
const SERVICE_NAME_REGEX = new RegExp(
  '^([a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9])?$'
);

function ScheduleJobDialog(props) {
  const { className, onClose, onDeploy } = props;

  const [jobSchedulerInput, setJobSchedulerInput] = useState({
    containerImage: '',
    displayName: '',
    deploymentParameters: {},
    deploymentEndpoints: [],
    cronString: '',
  });

  const onChange = (e) =>
    setJobSchedulerInput({
      ...jobSchedulerInput,
      [e.target.name]: e.target.value,
    });

  const isContainerImageInvalid = VALID_IMAGE_NAME.test(
    jobSchedulerInput.containerImage
  );
  const isDisplayNameInvalid = !SERVICE_NAME_REGEX.test(
    jobSchedulerInput.displayName
  );
  const isCronStringInvalid =
    jobSchedulerInput.cronString === ''
      ? false
      : !cron.isValidCron(jobSchedulerInput.cronString);

  return (
    <Dialog open>
      <DialogTitle>Schedule Job</DialogTitle>
      <DialogContent>
        <DialogContentText>
          Schedule a new job in the selected project based on the specific
          Docker image. Please make sure that the image is a compatible ML Lab
          service.
        </DialogContentText>
        <TextField
          required
          label="Container Image"
          type="text"
          name="containerImage"
          value={jobSchedulerInput.containerImage}
          onChange={onChange}
          autoComplete="on"
          error={isContainerImageInvalid}
          helperText={
            isContainerImageInvalid ? 'Image Name is not valid' : null
          }
          fullWidth
          margin="dense"
        />
        <TextField
          required
          label="Deployment Name"
          type="text"
          name="displayName"
          value={jobSchedulerInput.displayName}
          onChange={onChange}
          autoComplete="on"
          error={isDisplayNameInvalid}
          helperText={isDisplayNameInvalid ? 'Name is not valid' : null}
          fullWidth
          margin="dense"
        />

        <Typography className={`${className} subtitle`} variant="subtitle2">
          Configuration Variables
        </Typography>
        <KeyValueInputs
          onKeyValuePairChange={(keyValuePairs) => {
            setJobSchedulerInput({
              ...jobSchedulerInput,
              deploymentParameters: keyValuePairs,
            });
          }}
        />

        <Typography className={`${className} subtitle`} variant="subtitle2">
          Endpoints
        </Typography>
        <ValueInputs
          onValueInputsChange={(valueInputs) => {
            setJobSchedulerInput({
              ...jobSchedulerInput,
              deploymentEndpoints: valueInputs,
            });
          }}
        />
        <TextField
          required
          label="Cron String"
          type="text"
          name="cronString"
          value={jobSchedulerInput.cronString}
          onChange={onChange}
          autoComplete="on"
          error={isCronStringInvalid}
          helperText={isCronStringInvalid ? 'Cron string is not valid' : null}
          fullWidth
          margin="dense"
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} color="primary">
          CANCEL
        </Button>
        <Button
          disabled={
            isContainerImageInvalid ||
            isDisplayNameInvalid ||
            isCronStringInvalid ||
            !jobSchedulerInput.containerImage
          }
          onClick={() => onDeploy(jobSchedulerInput, onClose)}
          color="primary"
        >
          SCHEDULE
        </Button>
      </DialogActions>
    </Dialog>
  );
}

ScheduleJobDialog.propTypes = {
  className: PropTypes.string,
  onClose: PropTypes.func.isRequired,
  onDeploy: PropTypes.func.isRequired,
};

ScheduleJobDialog.defaultProps = {
  className: '',
};

const StyledDeployContainerDialog = styled(ScheduleJobDialog)`
  &.subtitle {
    margin-top: 16px;
  }
`;

export default StyledDeployContainerDialog;

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

import cronstrue from 'cronstrue';

import KeyValueInputs from './KeyValueInputs';
import ValueInputs from './ValueInputs';

const cron = require('cron-validator');

const VALID_IMAGE_NAME = new RegExp('[^a-zA-Z0-9-_:/.]');
const SERVICE_NAME_REGEX = new RegExp(
  '^([a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9])?$'
);

function EditScheduleJobDialog(props) {
  const { className, onClose, onEdit, defaults, jobId } = props;

  const containerImage = defaults?.containerImage || '';
  const displayName = defaults?.displayName || '';
  const deploymentParameters = defaults?.deploymentParameters || {};
  const deploymentEndpoints = defaults?.deploymentEndpoints || [];
  const cronString = defaults?.cronString || '';

  const [jobSchedulerInput, setJobSchedulerInput] = useState({
    containerImage,
    displayName,
    deploymentParameters,
    deploymentEndpoints,
    cronString,
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
      <DialogTitle>Edit Scheduled Job</DialogTitle>
      <DialogContent>
        <DialogContentText>
          Edit a scheduled job in the selected project based on the specific
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
        <Typography className={`${className} subtitle`} variant="subtitle2">
          {!isCronStringInvalid && jobSchedulerInput.cronString !== ''
            ? `${cronstrue.toString(jobSchedulerInput.cronString)}.`
            : null}
        </Typography>
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
          onClick={() => onEdit(jobSchedulerInput, jobId, onClose)}
          color="primary"
        >
          EDIT
        </Button>
      </DialogActions>
    </Dialog>
  );
}

EditScheduleJobDialog.propTypes = {
  className: PropTypes.string,
  onClose: PropTypes.func.isRequired,
  onEdit: PropTypes.func.isRequired,
  defaults: PropTypes.shape({
    containerImage: PropTypes.string,
    displayName: PropTypes.string,
    // eslint-disable-next-line react/forbid-prop-types
    deploymentParameters: PropTypes.object,
    deploymentEndpoints: PropTypes.arrayOf(PropTypes.string),
    cronString: PropTypes.string,
  }),
  jobId: PropTypes.string,
};

EditScheduleJobDialog.defaultProps = {
  className: '',
  defaults: {},
  jobId: '',
};

const StyledDeployContainerDialog = styled(EditScheduleJobDialog)`
  &.subtitle {
    margin-top: 16px;
  }
`;

export default StyledDeployContainerDialog;

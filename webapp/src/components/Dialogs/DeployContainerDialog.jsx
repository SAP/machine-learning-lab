import React, { useState } from 'react';

import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { Grid } from '@material-ui/core';
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

const VALID_IMAGE_NAME = new RegExp('[^a-zA-Z0-9-_:/.]');
const SERVICE_NAME_REGEX = new RegExp(
  '^([a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9])?$'
);

function DeployContainerDialog(props) {
  const { className, onClose, onDeploy } = props;

  const { t } = useTranslation();

  const [deploymentInput, setDeploymentInput] = useState({
    containerImage: '',
    deploymentName: '',
    deploymentParameters: {},
    deploymentEndpoints: [],
    minCpus: 1,
    minMemory: 1000,
  });

  const onChange = (e) =>
    setDeploymentInput({ ...deploymentInput, [e.target.name]: e.target.value });

  const isContainerImageInvalid = VALID_IMAGE_NAME.test(
    deploymentInput.containerImage
  );
  const isDeploymentNameInvalid = !SERVICE_NAME_REGEX.test(
    deploymentInput.deploymentName
  );

  return (
    <Dialog open>
      <DialogTitle>{`${t('add')} ${t('deployment')}`}</DialogTitle>
      <DialogContent>
        <DialogContentText>
          Make a new deployment in the selected project based on the specific
          Docker image. Please make sure that the image is a compatible ML Lab
          service.
        </DialogContentText>
        <TextField
          required
          label="Container Image"
          type="text"
          name="containerImage"
          value={deploymentInput.containerImage}
          onChange={onChange}
          onBlur={() => {}} // TODO: add here the "caching" logic handling
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
          name="deploymentName"
          value={deploymentInput.deploymentName}
          onChange={onChange}
          autoComplete="on"
          error={isDeploymentNameInvalid}
          helperText={isDeploymentNameInvalid ? 'Name is not valid' : null}
          fullWidth
          margin="dense"
        />
        {/* <Grid item xs={12}>
          <Divider textAlign="left">Workspace Resources</Divider>
        </Grid> */}
        <Grid container spacing={2}>
          <Grid item xs={6}>
            <TextField
              type="number"
              label="Number of CPUs"
              name="minCpus"
              value={deploymentInput.minCpus}
              onChange={onChange}
              margin="dense"
            />
          </Grid>
          <Grid item xs={6}>
            <TextField
              type="number"
              label="Memory in MB"
              name="minMemory"
              value={deploymentInput.minMemory}
              onChange={onChange}
              margin="dense"
            />
          </Grid>
        </Grid>
        <Typography className={`${className} subtitle`} variant="subtitle2">
          Configuration Variables
        </Typography>
        <KeyValueInputs
          onKeyValuePairChange={(keyValuePairs) => {
            setDeploymentInput({
              ...deploymentInput,
              deploymentParameters: keyValuePairs,
            });
          }}
        />

        <Typography className={`${className} subtitle`} variant="subtitle2">
          Endpoints
        </Typography>
        <ValueInputs
          onValueInputsChange={(valueInputs) => {
            setDeploymentInput({
              ...deploymentInput,
              deploymentEndpoints: valueInputs,
            });
          }}
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} color="primary">
          CANCEL
        </Button>
        <Button
          disabled={
            isContainerImageInvalid ||
            isDeploymentNameInvalid ||
            !deploymentInput.containerImage ||
            !deploymentInput.deploymentName
          }
          onClick={() => onDeploy(deploymentInput, onClose)}
          color="primary"
        >
          DEPLOY
        </Button>
      </DialogActions>
    </Dialog>
  );
}

DeployContainerDialog.propTypes = {
  className: PropTypes.string,
  onClose: PropTypes.func.isRequired,
  onDeploy: PropTypes.func.isRequired,
};

DeployContainerDialog.defaultProps = {
  className: '',
};

const StyledDeployContainerDialog = styled(DeployContainerDialog)`
  &.subtitle {
    margin-top: 16px;
  }
`;

export default StyledDeployContainerDialog;

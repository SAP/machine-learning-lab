import React, { useState } from 'react';

import PropTypes from 'prop-types';
import styled from 'styled-components';

import Button from '@material-ui/core/Button';
import Dialog from '@material-ui/core/Dialog';
import DialogActions from '@material-ui/core/DialogActions';
import DialogContent from '@material-ui/core/DialogContent';
import DialogContentText from '@material-ui/core/DialogContentText';
import DialogTitle from '@material-ui/core/DialogTitle';
import MenuItem from '@material-ui/core/MenuItem';
import Select from '@material-ui/core/Select';
import TextField from '@material-ui/core/TextField';

import { DEFAULT_WORKSPACE_IMAGE } from '../utils/config';

const VALID_IMAGE_NAME = new RegExp('[^a-zA-Z0-9-_:/.]');
const SERVICE_NAME_REGEX = new RegExp(
  '^([a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9])?$'
);

function CreateWorkspaceDialog(props) {
  const { className, onClose, onCreate, workspaceImages } = props;

  const [workspaceInput, setDeploymentInput] = useState({
    workspaceImage: DEFAULT_WORKSPACE_IMAGE,
    workspaceName: '',
  });

  const onChange = (e) =>
    setDeploymentInput({ ...workspaceInput, [e.target.name]: e.target.value });

  const isContainerImageInvalid = VALID_IMAGE_NAME.test(
    workspaceInput.workspaceImage
  );
  const isWorkspaceNameInvalid = !SERVICE_NAME_REGEX.test(
    workspaceInput.workspaceName
  );

  let workspaceImageInput;
  if (workspaceImages.length === 0) {
    workspaceImageInput = (
      <TextField
        required
        label="Workspace Image"
        type="text"
        name="workspaceImage"
        value={workspaceInput.workspaceImage}
        onChange={onChange}
        onBlur={() => {}} // TODO: add here the "caching" logic handling
        autoComplete="on"
        error={isContainerImageInvalid}
        helperText={isContainerImageInvalid ? 'Image Name is not valid' : null}
        fullWidth
        margin="dense"
      />
    );
  } else {
    if (
      workspaceInput.workspaceImage !== '' &&
      !workspaceImages.includes(workspaceInput.workspaceImage)
    ) {
      setDeploymentInput({ ...workspaceInput, workspaceImage: '' });
    }
    workspaceImageInput = (
      <Select
        required
        label="Workspace Image"
        id="demo-simple-select"
        name="workspaceImage"
        value={workspaceInput.workspaceImage}
        onChange={onChange}
        fullWidth
        margin="dense"
      >
        {workspaceImages.map((image) => (
          <MenuItem key={image} value={image}>
            {image}
          </MenuItem>
        ))}
      </Select>
    );
  }

  return (
    <Dialog open className={className}>
      <DialogTitle>Create New Workspace</DialogTitle>
      <DialogContent>
        <DialogContentText>
          Create a new personal workspace based on the specified docker image.
          Please make sure the image is a compatible ML Lab workspace (
          <a href="https://github.com/ml-tooling/ml-workspace">
            official workspace images
          </a>
          )
        </DialogContentText>
        {workspaceImageInput}
        <TextField
          required
          label="Workspace Name"
          type="text"
          name="workspaceName"
          value={workspaceInput.workspaceName}
          onChange={onChange}
          autoComplete="on"
          error={isWorkspaceNameInvalid}
          helperText={isWorkspaceNameInvalid ? 'Name is not valid' : null}
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
            isWorkspaceNameInvalid ||
            !workspaceInput.workspaceImage ||
            !workspaceInput.workspaceName
          }
          onClick={() => onCreate(workspaceInput, onClose)}
          color="primary"
        >
          CREATE
        </Button>
      </DialogActions>
    </Dialog>
  );
}

CreateWorkspaceDialog.propTypes = {
  className: PropTypes.string,
  onClose: PropTypes.func.isRequired,
  onCreate: PropTypes.func.isRequired,
  workspaceImages: PropTypes.arrayOf(PropTypes.string),
};

CreateWorkspaceDialog.defaultProps = {
  className: '',
  workspaceImages: [],
};

const StyledCreateWorkspaceDialog = styled(CreateWorkspaceDialog)`
  &.subtitle {
    margin-top: 16px;
  }
`;

export default StyledCreateWorkspaceDialog;

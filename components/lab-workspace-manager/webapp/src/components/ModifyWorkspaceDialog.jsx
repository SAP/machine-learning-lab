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

const VALID_IMAGE_NAME = new RegExp('[^a-zA-Z0-9-_:/.]');

function ModifyWorkspaceDialog(props) {
  const { className, onClose, onCreate } = props;

  const [workspaceInput, setDeploymentInput] = useState({
    workspaceImage: '',
  });

  const onChange = (e) =>
    setDeploymentInput({ ...workspaceInput, [e.target.name]: e.target.value });

  const isContainerImageInvalid = VALID_IMAGE_NAME.test(
    workspaceInput.workspaceImage
  );

  return (
    <Dialog open className={className}>
      <DialogTitle>Modify Workspace</DialogTitle>
      <DialogContent>
        <DialogContentText>
          If you modify the workspace image the workspace container will be
          recreated. This means all data not stored in the /workspace folder
          will be lost!
        </DialogContentText>
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
          helperText={
            isContainerImageInvalid ? 'Image Name is not valid' : null
          }
          fullWidth
          margin="dense"
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} color="primary">
          CANCEL
        </Button>
        <Button
          disabled={isContainerImageInvalid || !workspaceInput.workspaceImage}
          onClick={() => onCreate(workspaceInput, onClose)}
          color="primary"
        >
          MODIFY
        </Button>
      </DialogActions>
    </Dialog>
  );
}

ModifyWorkspaceDialog.propTypes = {
  className: PropTypes.string,
  onClose: PropTypes.func.isRequired,
  onCreate: PropTypes.func.isRequired,
};

ModifyWorkspaceDialog.defaultProps = {
  className: '',
};

const StyledModifyWorkspaceDialog = styled(ModifyWorkspaceDialog)`
  &.subtitle {
    margin-top: 16px;
  }
`;

export default StyledModifyWorkspaceDialog;

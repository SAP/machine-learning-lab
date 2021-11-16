import React, { useState } from 'react';

import PropTypes from 'prop-types';

import Button from '@material-ui/core/Button';
import CircularProgress from '@material-ui/core/CircularProgress';
import Dialog from '@material-ui/core/Dialog';
import DialogActions from '@material-ui/core/DialogActions';
import DialogContent from '@material-ui/core/DialogContent';
import DialogContentText from '@material-ui/core/DialogContentText';
import DialogTitle from '@material-ui/core/DialogTitle';

const ConfirmDeleteDialog = ({
  onClose,
  onDelete,
  dialogTitle,
  dialogText,
}) => {
  const [deletionStarted, setDeletionStarted] = useState(false);
  const handelDeleteClick = () => {
    setDeletionStarted(true);
    onDelete(onClose);
  };
  return (
    <Dialog open>
      <DialogTitle>{dialogTitle}</DialogTitle>
      <DialogContent>
        <DialogContentText>{dialogText}</DialogContentText>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} color="primary">
          CANCEL
        </Button>
        <Button
          disabled={deletionStarted}
          onClick={handelDeleteClick}
          color="primary"
        >
          DELETE
        </Button>
        {deletionStarted ? <CircularProgress size="25px" /> : null}
      </DialogActions>
    </Dialog>
  );
};

ConfirmDeleteDialog.propTypes = {
  onClose: PropTypes.func.isRequired,
  onDelete: PropTypes.func.isRequired,
  dialogTitle: PropTypes.string.isRequired,
  dialogText: PropTypes.string.isRequired,
};

export default ConfirmDeleteDialog;

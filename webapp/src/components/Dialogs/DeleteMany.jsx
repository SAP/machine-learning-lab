import React, { useState } from 'react';

import PropTypes from 'prop-types';

import { Grid } from '@material-ui/core';
import Button from '@material-ui/core/Button';
import CircularProgress from '@material-ui/core/CircularProgress';
import Dialog from '@material-ui/core/Dialog';
import DialogActions from '@material-ui/core/DialogActions';
import DialogContent from '@material-ui/core/DialogContent';
import DialogContentText from '@material-ui/core/DialogContentText';
import DialogTitle from '@material-ui/core/DialogTitle';
import TextField from '@material-ui/core/TextField';

const ConfirmDeleteManyDialog = ({
  onClose,
  onDelete,
  dialogTitle,
  dialogText,
}) => {
  const [deletionStarted, setDeletionStarted] = useState(false);
  const defaultDate = new Date().toISOString().slice(0, 10);
  const [startDate, setStartDate] = useState(defaultDate);
  const [endDate, setEndDate] = useState(defaultDate);
  const handelDeleteClick = () => {
    setDeletionStarted(true);
    onDelete(onClose, startDate, endDate);
  };
  const onSetStartDate = (event) => {
    setStartDate(new Date(event.target.value).toISOString().slice(0, 10));
  };

  const onSetEndDate = (event) => {
    setEndDate(new Date(event.target.value).toISOString().slice(0, 10));
  };

  return (
    <Dialog open>
      <DialogTitle>{dialogTitle}</DialogTitle>
      <DialogContent>
        <DialogContentText>{dialogText}</DialogContentText>
        <Grid container spacing={2}>
          <Grid item xs={6}>
            <TextField
              id="date"
              label="From"
              type="date"
              margin="dense"
              defaultValue={startDate}
              value={startDate}
              onChange={onSetStartDate}
              InputLabelProps={{
                shrink: true,
              }}
            />
          </Grid>
          <Grid item xs={6}>
            <TextField
              id="date"
              label="To"
              type="date"
              margin="dense"
              defaultValue={endDate}
              value={endDate}
              onChange={onSetEndDate}
              InputLabelProps={{
                shrink: true,
              }}
            />
          </Grid>
        </Grid>
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

ConfirmDeleteManyDialog.propTypes = {
  onClose: PropTypes.func.isRequired,
  onDelete: PropTypes.func.isRequired,
  dialogTitle: PropTypes.string.isRequired,
  dialogText: PropTypes.string.isRequired,
};

export default ConfirmDeleteManyDialog;

import React from 'react';

import PropTypes from 'prop-types';

import Button from '@material-ui/core/Button';
import Dialog from '@material-ui/core/Dialog';
import DialogActions from '@material-ui/core/DialogActions';
import DialogContent from '@material-ui/core/DialogContent';
import DialogContentText from '@material-ui/core/DialogContentText';
import DialogTitle from '@material-ui/core/DialogTitle';

const AddPluginDialog = ({ onClose }) => (
  <Dialog open>
    <DialogTitle>ADD PLUGIN</DialogTitle>
    <DialogContent>
      <DialogContentText>CONTENT HERE</DialogContentText>
    </DialogContent>
    <DialogActions>
      <Button onClick={onClose} color="primary">
        OK
      </Button>
    </DialogActions>
  </Dialog>
);

AddPluginDialog.propTypes = {
  onClose: PropTypes.func.isRequired,
};

export default AddPluginDialog;

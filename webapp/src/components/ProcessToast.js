import React from 'react';
import { toast } from 'react-toastify';

// material-ui components
import CircularProgress from '@material-ui/core/CircularProgress';
import DialogContentText from '@material-ui/core/DialogContentText';

import * as Constants from '../services/handler/constants';

const styles = {
  progress: {
    margin: 'auto',
  },
  toast: {
    textAlign: 'center',
  },
};

export function showProcessToast(processText) {
  return toast(
    <div style={styles.toast}>
      <DialogContentText>{processText}</DialogContentText>
      <CircularProgress style={styles.progress} />
    </div>,
    Constants.LOADING_TOAST_OPTIONS
  );
}

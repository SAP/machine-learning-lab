import React, { useState } from 'react';

import PropTypes from 'prop-types';

import Button from '@material-ui/core/Button';
import Dialog from '@material-ui/core/Dialog';
import DialogActions from '@material-ui/core/DialogActions';
import DialogContent from '@material-ui/core/DialogContent';
import DialogContentText from '@material-ui/core/DialogContentText';
import DialogTitle from '@material-ui/core/DialogTitle';
import IconButton from '@material-ui/core/IconButton';
import Input from '@material-ui/core/Input';
import InputAdornment from '@material-ui/core/InputAdornment';
import Visibility from '@material-ui/icons/Visibility';
import VisibilityOff from '@material-ui/icons/VisibilityOff';
import showStandardSnackbar from '../../app/showStandardSnackbar';

import { usersApi } from '../../services/contaxy-api';

function ChangePasswordDialog(props) {
  const { onClose, title, userId } = props;
  const [showNewPassword, setVisibilityNewPassword] = useState(false);
  const [showConfirmPassword, setVisibilityConfimPassword] = useState(false);
  const [newPassword, setNewPasswordValue] = useState('');
  const [confirmPassword, setConfirmPasswordValue] = useState('');

  const handleClickShowNewPassword = () => {
    setVisibilityNewPassword(!showNewPassword);
  };

  const handleClickShowConfirmPassword = () => {
    setVisibilityConfimPassword(!showConfirmPassword);
  };

  const setNewPassword = (e) => {
    setNewPasswordValue(e.target.value);
  };

  const setConfirmPassword = (e) => {
    setConfirmPasswordValue(e.target.value);
  };

  const onClickOk = async () => {
    try {
      usersApi.changePassword(userId, newPassword);
    } catch (e) {
      showStandardSnackbar(`Failed to change password!`);
    }
  };

  const contentElement = (
    <DialogContentText style={{ whiteSpace: 'pre-line' }}>
      <Input
        placeholder="New Password"
        type={showNewPassword ? 'text' : 'password'}
        value={newPassword}
        onChange={setNewPassword}
        endAdornment={
          <InputAdornment position="end">
            <IconButton
              aria-label="Toggle password visibility"
              onClick={handleClickShowNewPassword}
            >
              {showNewPassword ? <Visibility /> : <VisibilityOff />}
            </IconButton>
          </InputAdornment>
        }
      />
      <div>{'\n'}</div>
      <Input
        placeholder="Confirm New Password"
        type={showConfirmPassword ? 'text' : 'password'}
        value={confirmPassword}
        onChange={setConfirmPassword}
        endAdornment={
          <InputAdornment position="end">
            <IconButton
              aria-label="Toggle password visibility"
              onClick={handleClickShowConfirmPassword}
            >
              {showConfirmPassword ? <Visibility /> : <VisibilityOff />}
            </IconButton>
          </InputAdornment>
        }
      />
    </DialogContentText>
  );

  return (
    <Dialog open>
      <DialogTitle>{title}</DialogTitle>
      <DialogContent>{contentElement}</DialogContent>
      <DialogActions>
        <Button onClick={onClickOk} color="primary">
          OK
        </Button>
        <Button onClick={onClose} color="primary">
          CLOSE
        </Button>
      </DialogActions>
    </Dialog>
  );
}
ChangePasswordDialog.propTypes = {
  title: PropTypes.string,
  onClose: PropTypes.func.isRequired,
  userId: PropTypes.string.isRequired,
};

ChangePasswordDialog.defaultProps = {
  title: '',
};

export default ChangePasswordDialog;

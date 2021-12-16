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

import ReactJson from 'react-json-view';

function ChangePasswordDialog(props) {
  const { content, jsonContent, onClose, title } = props;
  const [showPassword, setShowPassword] = useState(false);
  const [newPassword, setNewPassword] = useState('');

  const handleClickShowPassword = () => {
    setShowPassword(!showPassword);
  };

  const setPassword = (e) => {
    setNewPassword(e.target.value);
  };

  const contentElement = content ? (
    <DialogContentText style={{ whiteSpace: 'pre-line' }}>
      <Input
        placeholder="New Password"
        type={showPassword ? 'text' : 'password'}
        value={newPassword}
        onChange={setPassword}
        endAdornment={
          <InputAdornment position="end">
            <IconButton
              aria-label="Toggle password visibility"
              onClick={handleClickShowPassword}
            >
              {showPassword ? <Visibility /> : <VisibilityOff />}
            </IconButton>
          </InputAdornment>
        }
      />
    </DialogContentText>
  ) : (
    <ReactJson src={jsonContent} />
  );

  return (
    <Dialog open>
      <DialogTitle>{title}</DialogTitle>
      <DialogContent>{contentElement}</DialogContent>
      <DialogActions>
        <Button onClick={onClose} color="primary">
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
  content: PropTypes.string,
  jsonContent: PropTypes.instanceOf(Object),
  onClose: PropTypes.func.isRequired,
};

ChangePasswordDialog.defaultProps = {
  title: '',
  content: '',
  jsonContent: {},
};

export default ChangePasswordDialog;

import React, { useState } from 'react';

import PropTypes from 'prop-types';

import styled from 'styled-components';

import {
  Button,
  DialogActions,
  DialogContent,
  IconButton,
  InputAdornment,
  TextField,
} from '@material-ui/core';
import Dialog from '@material-ui/core/Dialog';
import DialogTitle from '@material-ui/core/DialogTitle';
import Visibility from '@material-ui/icons/Visibility';
import VisibilityOff from '@material-ui/icons/VisibilityOff';
import showStandardSnackbar from '../../app/showStandardSnackbar';

const ChangePassword = ({ onClose, onSubmit }) => {
  // eslint-disable-next-line no-unused-vars
  const [password, setPassword] = useState('');
  const [passwordConfirmation, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const handleClickShowPassword = () => setShowPassword(!showPassword);
  const handleMouseDownPassword = () => setShowPassword(!showPassword);

  const changePassword = (e) => {
    setPassword(e.target.value);
  };

  const changeConfirmPassword = (e) => {
    setConfirmPassword(e.target.value);
  };

  const handelSubmitClick = (pass) => {
    onSubmit(onClose, pass);
  };

  const submit = async (pass, passConfirmation) => {
    if (pass !== passConfirmation) {
      showStandardSnackbar('Passwords do not match!');
    } else {
      handelSubmitClick(pass);
    }
  };

  return (
    <Dialog open>
      <DialogTitle>Change Password</DialogTitle>
      <DialogContent>
        <form>
          <TextField
            required
            id="password"
            name="password"
            label="Password"
            type={showPassword ? 'text' : 'password'}
            autoComplete="current-password"
            onChange={changePassword}
            margin="dense"
            fullWidth
            InputProps={{
              endAdornment: (
                <InputAdornment position="end">
                  <IconButton
                    aria-label="toggle password visibility"
                    onClick={handleClickShowPassword}
                    onMouseDown={handleMouseDownPassword}
                    edge="end"
                  >
                    {password.showPassword ? <VisibilityOff /> : <Visibility />}
                  </IconButton>
                </InputAdornment>
              ),
            }}
          />
          <TextField
            required
            id="passwordConfirmation"
            name="passwordConfirmation"
            label="Password Confirm"
            type={showPassword ? 'text' : 'password'}
            onChange={changeConfirmPassword}
            margin="dense"
            fullWidth
            InputProps={{
              endAdornment: (
                <InputAdornment position="end">
                  <IconButton
                    aria-label="toggle password visibility"
                    onClick={handleClickShowPassword}
                    onMouseDown={handleMouseDownPassword}
                    edge="end"
                  >
                    {passwordConfirmation.showPassword ? (
                      <VisibilityOff />
                    ) : (
                      <Visibility />
                    )}
                  </IconButton>
                </InputAdornment>
              ),
            }}
          />
        </form>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} color="primary">
          {}
          CANCEL{}
        </Button>
        <Button
          onClick={() => submit(password, passwordConfirmation)}
          color="primary"
        >
          SUBMIT
        </Button>
      </DialogActions>
    </Dialog>
  );
};
ChangePassword.propTypes = {
  onSubmit: PropTypes.func.isRequired,
  onClose: PropTypes.func.isRequired,
};

ChangePassword.defaultProps = {
  // className: '',
  // submit: () => {},
};

const StyledChangePassword = styled(ChangePassword)`
  &.projectIdFields {
    display: flex;
  }
`;

export default StyledChangePassword;

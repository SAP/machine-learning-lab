import React, { useState } from 'react';

import PropTypes from 'prop-types';

import styled from 'styled-components';

import {
  Button,
  DialogActions,
  DialogContent,
  TextField,
} from '@material-ui/core';
import Dialog from '@material-ui/core/Dialog';
import DialogTitle from '@material-ui/core/DialogTitle';

function CreateUserDialog(props) {
  // eslint-disable-next-line no-unused-vars
  const { className, onAdd, onClose } = props;
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  const changeUsername = (e) => {
    setUsername(e.target.value);
  };

  const changeEmail = (e) => {
    setEmail(e.target.value);
  };

  const changePassword = (e) => {
    setPassword(e.target.value);
  };
  const displayPasswordHelperText =
    'If the password is not provided, the user can only login by using other methods (social login).';

  return (
    <Dialog open>
      <DialogTitle>Create new user</DialogTitle>
      <DialogContent>
        <TextField
          required
          label="Username"
          name="username"
          type="text"
          value={username}
          onChange={changeUsername}
          margin="dense"
          fullWidth
        />
        <TextField
          required
          label="E-mail"
          name="email"
          type="text"
          value={email}
          onChange={changeEmail}
          margin="dense"
          fullWidth
        />
        <TextField
          label="Password"
          name="password"
          type="text"
          onChange={changePassword}
          value={password}
          helperText={displayPasswordHelperText}
          margin="dense"
          fullWidth
        />
      </DialogContent>

      <DialogActions>
        <Button onClick={onClose} color="primary">
          CANCEL
        </Button>
        <Button
          onClick={() =>
            onAdd(
              {
                username,
                email,
                disable: false,
                password,
              },
              onClose
            )
          }
          color="primary"
        >
          ADD
        </Button>
      </DialogActions>
    </Dialog>
  );
}
CreateUserDialog.propTypes = {
  className: PropTypes.string,
  onAdd: PropTypes.func.isRequired,
  onClose: PropTypes.func.isRequired,
};

CreateUserDialog.defaultProps = {
  className: '',
};

const StyledCreateUserDialog = styled(CreateUserDialog)`
  &.projectIdFields {
    display: flex;
  }
`;

export default StyledCreateUserDialog;

import React, { useReducer, useState } from 'react';

import PropTypes from 'prop-types';
import styled from 'styled-components';

import Button from '@material-ui/core/Button';
import TextField from '@material-ui/core/TextField';
import showStandardSnackbar from '../app/showStandardSnackbar';

import {
  authApi,
  getExternalLoginPageUrl,
  usersApi,
} from '../services/contaxy-api';
import GlobalStateContainer from '../app/store';

function Login(props) {
  const { setIsAuthenticated, oauthEnabled } =
    GlobalStateContainer.useContainer();
  const { className } = props;
  const [isRegistration, setIsRegistration] = useState(false);
  const initialFormState = {
    username: '',
    password: '',
    password_confirmation: '',
  };
  const [formInput, setFormInput] = useReducer(
    (state, newState) => ({ ...state, ...newState }),
    initialFormState
  );

  const login = async () => {
    try {
      await authApi.requestToken('password', {
        username: formInput.username,
        password: formInput.password,
        setAsCookie: true,
      });
      setIsAuthenticated(true);
    } catch (e) {
      showStandardSnackbar(`Login failed!`);
    }
  };

  const register = async () => {
    if (formInput.password !== formInput.password_confirmation) {
      showStandardSnackbar('Passwords do not match!');
      return;
    }

    try {
      const user = await usersApi.createUser({
        username: formInput.username,
        password: formInput.password,
      });
      showStandardSnackbar(
        `User ${user.username} successfully registered. You can now login.`
      );
      setIsRegistration(false);
    } catch (e) {
      showStandardSnackbar(
        `Could not register user. Reason: ${e.body.message}`
      );
    }
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (isRegistration) register();
    else login();
    return false;
  };

  const handleInput = (event) => {
    const { name, value } = event.target;
    setFormInput({ [name]: value });
  };

  return (
    <form className={`${className} container`} onSubmit={handleSubmit}>
      <TextField
        required
        className={`${className} input`}
        id="username"
        name="username"
        label="Username"
        variant="filled"
        defaultValue={formInput.username}
        onChange={handleInput}
      />
      <TextField
        required
        className={`${className} input`}
        id="password"
        name="password"
        label="Password"
        type="password"
        autoComplete="current-password"
        variant="filled"
        defaultValue={formInput.password}
        onChange={handleInput}
      />
      {isRegistration && (
        <TextField
          required
          className={`${className} input`}
          id="password_confirmation"
          name="password_confirmation"
          label="Password Confirm"
          type="password"
          variant="filled"
          onChange={handleInput}
        />
      )}
      <Button
        type="submit"
        variant="contained"
        color="primary"
        className={className}
      >
        {!isRegistration && 'Login'}
        {isRegistration && 'Register'}
      </Button>
      {oauthEnabled && <StyledSpan>-- or --</StyledSpan>}
      {oauthEnabled && (
        <Button
          href={getExternalLoginPageUrl()}
          variant="contained"
          color="primary"
          className={className}
        >
          External Login
        </Button>
      )}
      <StyledSpan>-- or --</StyledSpan>
      <Button
        onClick={() => setIsRegistration(!isRegistration)}
        className={className}
        color="primary"
      >
        {!isRegistration && 'Register'}
        {isRegistration && 'Back to Login'}
      </Button>
    </form>
  );
}

Login.propTypes = {
  className: PropTypes.string,
};

Login.defaultProps = {
  className: '',
};

const StyledSpan = styled.span`
  color: ${(props) => props.theme.palette.gray};
  font-size: 0.75rem;
`;

const StyledLogin = styled(Login)`
  &.container {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    margin-top: 24px;
  }

  &.input {
    margin-bottom: 8px;
  }
`;

export default StyledLogin;

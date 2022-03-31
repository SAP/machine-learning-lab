import React, { useReducer, useState } from 'react';

import PropTypes from 'prop-types';

import Button from '@material-ui/core/Button';
import Link from '@material-ui/core/Link';
import TextField from '@material-ui/core/TextField';
import showStandardSnackbar from '../../app/showStandardSnackbar';

import {
  authApi,
  getExternalLoginPageUrl,
  usersApi,
} from '../../services/contaxy-api';
import { useShowAppDialog } from '../../app/AppDialogServiceProvider';
import ContentDialog from '../../components/Dialogs/ContentDialog';
import GlobalStateContainer from '../../app/store';

import './Login.css';
import mlLabBannerImage from '../../assets/images/ml-lab-banner.png';

const { TOS_TEXT } = window.env;

function Login(props) {
  const { setIsAuthenticated, oauthEnabled } =
    GlobalStateContainer.useContainer();
  const { className } = props;
  const showAppDialog = useShowAppDialog();
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

  let tosLink = null;
  if (TOS_TEXT) {
    tosLink = (
      // eslint-disable-next-line jsx-a11y/anchor-is-valid
      <Link
        component="button"
        type="button"
        onClick={() => {
          showAppDialog(ContentDialog, {
            content: TOS_TEXT,
            title: 'ML Lab Terms of Service',
          });
        }}
      >
        By logging in to ML Lab, you agree to the Terms of Service
      </Link>
    );
  }
  return (
    <div className="outer-container">
      <div className="login-container">
        <div className="login-logo">
          <img src={mlLabBannerImage} alt="" />
        </div>
        <div className="login-form-container">
          <form className={`${className} login-form`} onSubmit={handleSubmit}>
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
            {oauthEnabled && <OrSeparator />}
            {oauthEnabled && (
              <Button
                href={getExternalLoginPageUrl()}
                variant="contained"
                color="primary"
                className={className}
              >
                Single Sign-On
              </Button>
            )}
            <OrSeparator />
            <Button
              onClick={() => setIsRegistration(!isRegistration)}
              className={className}
              color="primary"
            >
              {!isRegistration && 'Register'}
              {isRegistration && 'Back to Login'}
            </Button>
            {tosLink}
          </form>
        </div>
      </div>
    </div>
  );
}

Login.propTypes = {
  className: PropTypes.string,
};

Login.defaultProps = {
  className: '',
};

function OrSeparator() {
  return (
    <div className="login-separator">
      <div className="login-separator-line" />
      <div>
        <span>or</span>
      </div>
      <div className="login-separator-line" />
    </div>
  );
}

export default Login;

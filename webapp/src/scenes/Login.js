import React, { Component } from 'react';
import { connect } from 'react-redux';
import * as ReduxUtils from '../services/handler/reduxUtils';
import PropTypes from 'prop-types';

import { toast } from 'react-toastify';

// material-ui components
import Button from '@material-ui/core/Button';
import { withStyles } from '@material-ui/core/styles';
import { TextField, Typography } from '@material-ui/core';

import {
  authorizationApi,
  getDefaultApiCallback,
} from '../services/client/ml-lab-api';
import { parseJwtToken } from '../services/handler/utils';
import TermsOfServiceDialog from './app/components/TermsOfServiceDialog';

//TODO: add 'Pending Authentication State' so Login can be hidden when authentication /refreshLogin request is made.

const steps = {
  checkOidc: 'checkOidc',
  login: 'login',
  register: 'register',
};

const styles = (theme) => ({
  toast: {
    textAlign: 'center',
    margin: 'auto',
  },
});

class Login extends Component {
  constructor(props) {
    super(props);
    this.state = {
      user: '',
      pass: '',
      repeatedPass: '',
      step: steps.checkOidc,
      oidcEnabled: false
    };
    this.checkOidc()
  }

  checkOidc = () => {
    authorizationApi.oidcEnabled(
      getDefaultApiCallback(
        ({ result }) => {
          let oidcIsEnabled = result.data;
          this.setState({
            oidcEnabled: oidcIsEnabled,
            step: steps.login
          })
        },
        ({ httpResponse }) => {
          let errorMessage = '';
          try {
            errorMessage = httpResponse.body.errors.message;
          } catch (err) {
            // do nothing
          }
          console.error("Error checking oidc: " + errorMessage)
          this.setState({ step: steps.login });
        }
      )
    );
  }

  loginUser = (base64Credentials) => {
    authorizationApi.loginUser(
      { authorization: base64Credentials },
      getDefaultApiCallback(
        ({ result }) => {
          let { isAdmin } = parseJwtToken(result.data);
          this.props.onAuthentication(this.state.user, true, isAdmin);
          this.props.history.push('/');
        },
        ({ httpResponse }) => {
          this.props.onAuthentication(null, false);
          let errorMessage = '';
          try {
            errorMessage = httpResponse.body.errors.message;
          } catch (err) {
            // do nothing
          }
          toast.error('Login failed. ' + errorMessage);
        }
      )
    );
  };

  handleLogin = function () {
    let { user, pass } = this.state;

    const authToken = 'Basic ' + btoa(`${user}:${pass}`);

    this.loginUser(authToken);
  };

  handleRegister = function () {
    if (this.state.pass !== this.state.repeatedPass) {
      toast.error("The two passwords don't match");
      return;
    }

    authorizationApi.createUser(
      this.state.user,
      this.state.pass,
      {},
      getDefaultApiCallback(
        () => {
          toast.success('Registration was successful. You can login now.');
          this.setState({ step: steps.login });
        },
        ({ httpResponse }) => {
          let errorMessage = '';
          try {
            errorMessage = httpResponse.body.errors.message;
          } catch (err) {
            // do nothing
          }
          toast.error('Registration failed: ' + errorMessage);
        }
      )
    );
  };

  handleClick = function (e) {
    if (this.state.user === '' || this.state.pass === '') {
      toast.error('User or password cannot be empty.');
      return;
    }
    if (this.state.step === steps.login) {
      this.handleLogin();
    } else {
      this.handleRegister();
    }
  }.bind(this);

  handleChange = function (e) {
    this.setState({
      [e.target.name]: e.target.value,
    });
  }.bind(this);

  handleStepChange = function (e) {
    if (this.state.step === steps.login) {
      this.setState({ step: steps.register });
    } else {
      this.setState({ step: steps.login });
    }
  };

  render() {
    // In case we are still checking whether external OIDC authentication is enabled, a simple loading text is displayed
    if (this.state.step == steps.checkOidc) {
      return <div style={{
        fontFamily: '"Roboto", "Helvetica", "Arial", sans-serif',
        fontSize: '16px',
        color: 'gray',
        margin: 'auto',
        marginTop: '48px',
      }}>
        Redirecting to login...
      </div>
    }

    // Change some variables when we are in register step and OIDC is disabled
    let buttonText = 'Login';
    let repeatPasswordField = null;
    let stepChangeText = 'Register here!';
    if (!this.state.oidcEnabled && this.state.step === steps.register) {
      buttonText = 'Register';
      repeatPasswordField = (
        <div>
          <TextField
            name="repeatedPass"
            label="Repeat Password"
            type="password"
            margin="normal"
            autoComplete="login pass"
            onChange={(e) => this.handleChange(e)}
          />
        </div>
      );
      stepChangeText = 'Go back to login';
    }

    let additionalLoginComponent = null;
    if (this.state.oidcEnabled) {
      // If OIDC is enabled, we show the extra external login button
      additionalLoginComponent = (
        <div>
          <div style={{
            display: 'flex',
            justifyContent: 'space-between',
            fontFamily: '"Roboto", "Helvetica", "Arial", sans-serif',
            margin: "10px 0px"
          }}>
            <div style={{ width: '50px', height: '10px', borderBottom: '1px solid rgb(199, 208, 217)' }}></div>
            <span>or use</span>
            <div style={{ width: '50px', height: '10px', borderBottom: '1px solid rgb(199, 208, 217)' }}></div>
          </div>
          <Button
            variant="text"
            color="primary"
            onClick={(event) => window.location.href = '/api/auth/oidc/login'}
            style={{
              paddingLeft: '0px',
              justifyContent: 'start',
              marginTop: '4px',
            }}
          > External Login
          </Button>
          {/* <div style={{
          margin: '0 auto',
          marginTop: '48px',
          padding: '20px',
          fontFamily: '"Roboto", "Helvetica", "Arial", sans-serif',
          backgroundColor: "#fff",
          border: "1px solid #d7dee1",
          borderRadius: "5px"
        }}>
          <h2 style={{
            textAlign: 'center',
          }}>
            Login
          </h2>
          <p style={{
            fontSize: '14px',
            textAlign: 'center'
          }}>
            This ML Lab instance is configured to use an external authentication provider.
          </p>
          <Button
            variant="text"
            color="primary"
            onClick={(event) => window.location.href = '/api/auth/oidc/login'}
            style={{
              marginTop: '20px',
              textAlign: 'center'
            }}
          >
            Login with external provider
          </Button>
        </div> */}
        </div>);
    } else {
      // If OIDC is disabled, we show the "Register here" or "Back to login" button depending on the step
      additionalLoginComponent = (
        <div>
          {this.state.step === steps.register ? (
          false
          ) : (
          <Typography
            style={{ marginTop: '2px', fontSize: '12px', color: 'gray' }}
          >
            Login or
          </Typography>
        )}
          <div
            style={{
              fontFamily: '"Roboto", "Helvetica", "Arial", sans-serif',
              fontSize: '12px',
              color: 'gray',
              cursor: 'pointer',
              textDecoration: 'underline',
            }}
            onClick={() => this.handleStepChange()}
          >
            {stepChangeText}
          </div>
        </div>);
    }
    const ENTER_KEY = 13;
    if (this.state.step == steps.showOidc) {
    }
    return (
      <div
        style={{ margin: '48px auto auto;', marginTop: '48px' }}
        onKeyPress={(e) =>
          e.charCode === ENTER_KEY ? this.handleClick() : false
        }
      >
        <TermsOfServiceDialog/>
        <div>
          <TextField
            style={{ width: '175px'}}
            name="user"
            label="User"
            margin="normal"
            autoComplete="login user"
            onChange={(e) => this.handleChange(e)}
          />
        </div>
        <div>
          <TextField
            style={{ width: '175px'}}
            name="pass"
            label="Password"
            type="password"
            margin="normal"
            autoComplete="login pass"
            onChange={(e) => this.handleChange(e)}
          />
        </div>
        {repeatPasswordField}
        <div>

          <Button
            variant="text"
            color="primary"
            onClick={(event) => this.handleClick(event)}
            style={{
              paddingLeft: '0px',
              justifyContent: 'start',
              marginTop: '4px',
            }}
          >{buttonText}</Button>
        </div>
        { additionalLoginComponent }
      </div >
    );
  }
}

Login.propTypes = {
  onAuthentication: PropTypes.func.isRequired, // passed by ReduxUtils,
};

export default connect(
  ReduxUtils.mapStateToProps,
  ReduxUtils.mapDispatchToProps
)(withStyles(styles)(Login));

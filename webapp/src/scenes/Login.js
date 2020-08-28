import React, { Component } from "react";
import { connect } from "react-redux";
import * as ReduxUtils from "../services/handler/reduxUtils";
import PropTypes from "prop-types";

import { toast } from "react-toastify";

// material-ui components
import Button from "@material-ui/core/Button";
import { withStyles } from "@material-ui/core/styles";
import { TextField, Typography } from "@material-ui/core";

import {
  authorizationApi,
  getDefaultApiCallback
} from "../services/client/ml-lab-api";
import { parseJwtToken } from "../services/handler/utils";

//TODO: add 'Pending Authentication State' so Login can be hidden when authentication /refreshLogin request is made.

const steps = {
  login: "login",
  register: "register"
};

const styles = theme => ({
  toast: {
    textAlign: "center",
    margin: "auto"
  }
});

class Login extends Component {
  constructor(props) {
    super(props);
    this.state = {
      user: "",
      pass: "",
      repeatedPass: "",
      step: steps.login
    };
  }

  loginUser = base64Credentials => {
      authorizationApi.loginUser(
        { authorization: base64Credentials },
        getDefaultApiCallback(
          ({ result }) => {
            let { isAdmin } = parseJwtToken(result.data);
            this.props.onAuthentication(this.state.user, true, isAdmin);
            this.props.history.push("/");
          },
          ({ httpResponse }) => {
            this.props.onAuthentication(null, false)
            let errorMessage = ""
            try {
              errorMessage = httpResponse.body.errors.message
            } catch(err) {
              // do nothing
            }
            toast.error(
              "Login failed. " + errorMessage
            );
          }
        )
      );
  };

  handleLogin = function() {
    let { user, pass } = this.state;

    const authToken = "Basic " + btoa(`${user}:${pass}`);

    this.loginUser(authToken);
  };

  handleRegister = function() {

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
          toast.success("Registration was successful. You can login now.");
          this.setState({ step: steps.login });
        },
        ({ httpResponse }) => {
          let errorMessage = ""
          try {
            errorMessage = httpResponse.body.errors.message
          } catch(err) {
            // do nothing
          }
          toast.error(
            "Registration failed: " + errorMessage
          );
        }
      )
    );
  };

  handleClick = function(e) {
    if (this.state.user === "" || this.state.pass === "") {
      toast.error("User or password cannot be empty.");
      return;
    }
    if (this.state.step === steps.login) {
      this.handleLogin();
    } else {
      this.handleRegister();
    }
  }.bind(this);

  handleChange = function(e) {
    this.setState({
      [e.target.name]: e.target.value
    });
  }.bind(this);

  handleStepChange = function(e) {
    if (this.state.step === steps.login) {
      this.setState({ step: steps.register });
    } else {
      this.setState({ step: steps.login });
    }
  };

  render() {
    let buttonText = "Login";
    let repeatPasswordField = null;
    let additionalLoginComponent =
      this.state.step === steps.register ? (
        false
      ) : (
        <Typography
          style={{ marginTop: "2px", fontSize: "12px", color: "gray" }}
        >
          Login or
        </Typography>
      );
    let stepChangeText = "Register here!";
    if (this.state.step === steps.register) {
      buttonText = "Register";
      repeatPasswordField = (
        <div>
          <TextField
            name="repeatedPass"
            label="Repeat Password"
            type="password"
            margin="normal"
            autoComplete="login pass"
            onChange={e => this.handleChange(e)}
          />
        </div>
      );
      stepChangeText = "Go back to login";
    }
    const ENTER_KEY = 13;
    return (
      <div
        style={{ margin: "auto", marginTop: "48px" }}
        onKeyPress={e =>
          e.charCode === ENTER_KEY ? this.handleClick() : false
        }
      >
        <div>
          <TextField
            name="user"
            label="User"
            margin="normal"
            autoComplete="login user"
            onChange={e => this.handleChange(e)}
          />
        </div>
        <div>
          <TextField
            name="pass"
            label="Password"
            type="password"
            margin="normal"
            autoComplete="login pass"
            onChange={e => this.handleChange(e)}
          />
        </div>
        {repeatPasswordField}
        <Button
          variant="text"
          color="primary"
          onClick={event => this.handleClick(event)}
          style={{
            paddingLeft: "0px",
            justifyContent: "start",
            marginTop: "4px"
          }}
        >
          {buttonText}
        </Button>
        {additionalLoginComponent}
        <div
          style={{
            fontFamily: '"Roboto", "Helvetica", "Arial", sans-serif',
            fontSize: "12px",
            color: "gray",
            cursor: "pointer",
            textDecoration: "underline"
          }}
          onClick={() => this.handleStepChange()}
        >
          {stepChangeText}
        </div>
      </div>
    );
  }
}

Login.propTypes = {
  onAuthentication: PropTypes.func.isRequired // passed by ReduxUtils,
};

export default connect(
  ReduxUtils.mapStateToProps,
  ReduxUtils.mapDispatchToProps
)(withStyles(styles)(Login));

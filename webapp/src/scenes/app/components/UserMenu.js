import React from "react";
import ReactDOM from "react-dom";
import { connect } from "react-redux";
import { withCookies } from "react-cookie";
import PropTypes from "prop-types";

// material-ui components
import IconButton from "@material-ui/core/IconButton";
import AccountCircle from "@material-ui/icons/AccountCircle";
import Menu from "@material-ui/core/Menu";
import MenuItem from "@material-ui/core/MenuItem";
import Typography from "@material-ui/core/Typography";
import Button from "@material-ui/core/Button";
import TextField from "@material-ui/core/TextField";
import Link from '@material-ui/core/Link';

import CustomDialog from "../../../components/CustomDialog";

import * as Parser from "../../../services/handler/parser";
import * as Constants from "../../../services/handler/constants";

import {
  mapStateToProps,
  mapDispatchToProps
} from "../../../services/handler/reduxUtils";

import {
  authorizationApi,
  getDefaultApiCallback,
  toastInfo,
  toastSuccess,
  toastErrorMessage,
  toastErrorType,
  projectsApi
} from "../../../services/client/ml-lab-api";

const ORIGINAL_DIALOG = {
  isOpen: false,
  title: "",
  contentText: "",
  action: null,
  customComponent: null,
  hideCancelBtn: false,
  isApiTokenDialog: false,
  primaryActionBtnLabel: "Ok"
};

class UserMenu extends React.Component {
  constructor() {
    super();

    this.state = {
      anchorEl: null,
      dialog: { ...ORIGINAL_DIALOG },
      apiToken: "",
      isAnonymousChecked: false
    };
  }

  handleMenu = event => {
    this.setState({ anchorEl: event.currentTarget });
  };

  handleDialogInput = e => {
    this.setState({ [e.target.name]: e.target.value });
  };

  handleSwitchInput = e => {
    this.setState({ [e.target.name]: e.target.checked });
  };

  handleGetUserApiToken = () => {
    authorizationApi.createApiToken(
      this.props.user,
      {},
      getDefaultApiCallback(({ result }) => {
        let TextField = (
          <div>
            <Typography
              paragraph={true}
              variant="subtitle2"
            >
              With this token, all resources can be accessed that can be accessed with your user. 
              In most cases, the more restrictive Project API Token should be used.
            </Typography>
            <Typography
              variant="body1"
              style={{ wordBreak: "break-all", fontWeight: "initial" }}
              ref={node => (this.apiTokenField = node)}
            >
              {result.data}
            </Typography>
          </div>
        );

        this.setState({
          dialog: {
            ...ORIGINAL_DIALOG,
            isOpen: true,
            title: "User API Token",
            customComponent: TextField,
            hideCancelBtn: true,
            isApiTokenDialog: true
          },
          apiToken: result.data
        });
      })
    );
  };

  handleGetProjectApiToken = () => {
    projectsApi.createProjectToken(
      this.props.currentProject,
      {},
      getDefaultApiCallback(({ result }) => {
        let TextField = (
          <div>
            <Typography
              paragraph={true}
              variant="subtitle2"
            >
              With this token, all resources that belong to the project can be accessed. For example, it is possible to
              access this project's services, jobs, and experiment data.
            </Typography>
            <Typography
              variant="body1"
              style={{ wordBreak: "break-all", fontWeight: "initial" }}
              ref={node => (this.apiTokenField = node)}
            >
              {result.data}
            </Typography>
          </div>
        );

        this.setState({
          dialog: {
            ...ORIGINAL_DIALOG,
            isOpen: true,
            title: "Project API Token",
            customComponent: TextField,
            hideCancelBtn: true,
            isApiTokenDialog: true
          },
          apiToken: result.data
        });
      })
    );
  };

  handleUpdateUserPassword = () => {
    const changePasswordComponent = (
      <div>
        <TextField
          // autoFocus // commented because if additionalDialogComponent is rendered, this is annoying
          margin="dense"
          // id={tfId}
          name="newPassword"
          label="New Password"
          type="password"
          onChange={e => this.handleDialogInput(e)}
          fullWidth
        />
        <TextField
          // autoFocus // commented because if additionalDialogComponent is rendered, this is annoying
          margin="dense"
          // id={tfId}
          name="newPasswordRepeat"
          label="Repeat new password"
          type="password"
          onChange={e => this.handleDialogInput(e)}
          fullWidth
        />
      </div>
    );

    const onPasswordSend = () => {
      if (this.state.newPassword !== this.state.newPasswordRepeat) {
        toastErrorType("The two passwords don't match.");
      } else {
        authorizationApi.updateUserPassword(
          this.props.user,
          this.state.newPassword,
          {},
          getDefaultApiCallback(
            () => {
              toastSuccess("Changed password."); 
              this.handleLogout();
            },
            ({ error }) => toastErrorMessage("Change password failed: ", error)
          )
        );
      }
    };

    this.setState({
      dialog: {
        ...ORIGINAL_DIALOG,
        isOpen: true,
        title: "Change your password",
        contentText: "You will get logged out and have to login again!",
        customComponent: changePasswordComponent,
        primaryActionBtnLabel: "Save",
        action: onPasswordSend
      }
    });
  };

  handleAboutDialog = () => {
    const aboutDialogComponent = (
      <div>
        <Typography>Please find the source code on <Link href={Constants.GITHUB_LINK} target="_blank" rel="noopener noreferrer">GitHub</Link>.</Typography>
        <Typography>See third party <Link href={Constants.ABOUT_FILE} target="_blank" rel="noopener noreferrer">credits.</Link></Typography>
      </div>
    );

    this.setState({
      dialog: {
        ...ORIGINAL_DIALOG,
        isOpen: true,
        title: "About",
        hideCancelBtn: true,
        customComponent: aboutDialogComponent
      }
    });
  }

  handleLogout = () => {
    authorizationApi.logoutUser({},
      getDefaultApiCallback(
        () => {
          toastSuccess("Logged out");
          this.props.cookies.remove(
            Constants.COOKIES.project,
            Constants.COOKIES.options
          );
          this.props.onLogout();
        },
        ({ error }) => toastErrorMessage("Logout failed: ", error)
      )
    );
    //this.props.onInpChange({ target: { value: "" } });

    //this.props.onAuthentication(null, false, null);
    this.handleClose();
  };

  handleCopyKey = text => {
    const apiTokenTextField = ReactDOM.findDOMNode(this.apiTokenField);
    Parser.setClipboardText(text, apiTokenTextField);
    toastInfo("Copied to Clipboard");
  };

  handleClose = () => {
    this.setState({ anchorEl: null });
  };

  handleDialogClose = () => {
    // reset the properties of the dialog state, so that spreading operator (...this.state.dialog)
    // returns the right properties next time.
    this.setState({
      dialog: {
        ...this.state.dialog,
        isOpen: false
      },
      anchorEl: null
    });
  };

  render() {
    const { anchorEl } = this.state;
    const open = Boolean(anchorEl);

    const copyButton = this.state.dialog.isApiTokenDialog ? (
      <Button
        color="primary"
        onClick={e => this.handleCopyKey(this.state.apiToken)}
      >
        Copy Token
      </Button>
    ) : (
      false
    );

    return (
      <div>
        {this.props.isAuthenticated && (
          <div style={{ marginRight: 12 }}>
              
            {/* Display user name */}
            <Typography style={{ color: "white", display: "inline-block" }}>
              {this.props.user}
            </Typography>
            <IconButton
              aria-owns={open ? "menu-appbar" : null}
              aria-haspopup="true"
              onClick={this.handleMenu}
              color="inherit"
            >
              <AccountCircle />
            </IconButton>
            <Menu
              id="menu-appbar"
              anchorEl={anchorEl}
              anchorOrigin={{
                vertical: "top",
                horizontal: "right"
              }}
              transformOrigin={{
                vertical: "top",
                horizontal: "right"
              }}
              open={open}
              onClose={this.handleClose}
            >
              <MenuItem>
                <a
                  style={{ textDecoration: "none", color: "initial" }}
                  href={Constants.SERVICES.documentation.url}
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  Documentation
                </a>
              </MenuItem>

              <MenuItem>
                <a
                  style={{ textDecoration: "none", color: "initial" }}
                  href={Constants.SERVICES.apiExplorer.url}
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  API Explorer
                </a>
              </MenuItem>

              <MenuItem onClick={this.handleGetUserApiToken}>
                Get User API Token
              </MenuItem>
              <MenuItem onClick={this.handleGetProjectApiToken}>
                Get Project API Token
              </MenuItem>
              <MenuItem onClick={this.handleUpdateUserPassword}>
                Change Password
              </MenuItem>
              <MenuItem onClick={this.handleAboutDialog}>
                About
              </MenuItem>
              <MenuItem onClick={this.handleLogout}>Logout</MenuItem>
            </Menu>
            <CustomDialog
              open={this.state.dialog.isOpen}
              title={this.state.dialog.title}
              contentText={this.state.dialog.contentText}
              cancelBtnDisabled={false}
              hideCancelBtn={this.state.dialog.hideCancelBtn}
              primaryActionBtnDisabled={false}
              primaryActionBtnLabel={this.state.dialog.primaryActionBtnLabel}
              handleRequestClose={this.handleDialogClose}
              handlePrimaryAction={
                this.state.dialog.action
                  ? this.state.dialog.action
                  : this.handleDialogClose
              }
              CustomComponent={this.state.dialog.customComponent}
              moreButtons={[copyButton]}
            />
          </div>
        )}
      </div>
    );
  }
}

UserMenu.propTypes = {
  user: PropTypes.string, // from redux
  // currentProject: PropTypes.string, //from redux
  onLogout: PropTypes.func.isRequired,
  onAuthentication: PropTypes.func.isRequired, // from redux
  isAuthenticated: PropTypes.bool.isRequired, // from redux
  cookies: PropTypes.object // from withCookies
};

export default withCookies(
  connect(
    mapStateToProps,
    mapDispatchToProps
  )(UserMenu)
);

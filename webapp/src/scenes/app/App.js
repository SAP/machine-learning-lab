import React from "react";
import PropTypes from "prop-types";
import { withCookies, Cookies } from "react-cookie";
import { connect } from "react-redux";
import { withRouter } from "react-router";
import * as ReduxUtils from "../../services/handler/reduxUtils";
import classNames from "classnames";

import { ToastContainer, toast } from "react-toastify";
import 'react-toastify/dist/ReactToastify.css';

// material ui components
import { withStyles } from "@material-ui/core/styles";
import Drawer from "@material-ui/core/Drawer";
import AppBar from "@material-ui/core/AppBar";
import Toolbar from "@material-ui/core/Toolbar";
import List from "@material-ui/core/List";
import Typography from "@material-ui/core/Typography";
import IconButton from "@material-ui/core/IconButton";
import MenuIcon from "@material-ui/icons/Menu";
import Grid from "@material-ui/core/Grid";

//scene components
import MenuSideBar from "./components/MenuSideBar";
import ProjectSelector from "./components/ProjectSelector";
import UserMenu from "./components/UserMenu";
import ContentContainer from "./components/routing/ContentContainer";

import {
  authorizationApi,
  administrationApi,
  getDefaultApiCallback
} from "../../services/client/ml-lab-api";

//controller
import * as Constants from "../../services/handler/constants";
import { parseJwtToken } from "../../services/handler/utils";

//styles
import { APP_STYLES } from "./appStyles.js";

// style({
//   colorInfo: "#9E9E9E",
//   colorSuccess: "#4CAF50",
//   colorWarning: "#FF9800",
//   colorError: "#f44336"
// });

const TIMEOUT_ONE_HOUR = 1000 * 60 * 60;

const APP_NAME = "Machine Learning Lab";

class App extends React.Component {

  constructor(props) {
    super(props);

    this.state = {
      open: (props.cookies.get(Constants.COOKIES.firstTimeLogin) === "true") ? true : false,
      isAdmin: false,
      isAuthenticationChecked: false
    };
  
  }
  
  refreshTimer = null;

  //TODO: maybe that could be moved to the service worker?
  refresh() {
    // set isAuthenticated to true when an auth cookie exists. This is just to prevent brief flashes
    // of the login-page on page refresh in the UI. The true value will be set after return of the refreshToken-API call.
    // Without a valid token all other backend requests will fail, independent of the isAuthenticated value.

    authorizationApi.refreshToken(
      {},
      getDefaultApiCallback(
        ( { result } ) => {
          const LAB_ACCESS_TOKEN = result.data;
          let { username, isAdmin } = parseJwtToken(LAB_ACCESS_TOKEN);
          this.props.onAuthentication(username, true, isAdmin);
          this.refreshTimer = setTimeout(this.refresh, TIMEOUT_ONE_HOUR); // automatically refresh the token every hour so the user never gets logged out
          this.setState({ isAuthenticationChecked: true });
        },
        () => {
          this.props.onAuthentication(null, false, null);
          this.setState({ isAuthenticationChecked: true });
        },
        false
      )
    );
  }

  componentDidMount() {
    this.refresh();
  }

  componentWillUnmount() {
    if (this.refreshTimer) {
      clearTimeout(this.refreshTimer);
      this.refreshTimer = null;
      this.setState({ isAuthenticationChecked: false });
    }
  }

  handleDrawerAction = () => {
    this.setState({
      open: !this.state.open
    });
  };

  componentDidUpdate(prevProps) {
    if (prevProps.isAdmin !== this.props.isAdmin) {
      this.setState({ isAdmin: this.props.isAdmin });
    }

    // When user opens the webapp, start the workspace if necessary so it is (almost) ready when the user wants to enter it
    if (prevProps.isAuthenticated !== this.props.isAuthenticated && this.props.isAuthenticated) {
      administrationApi.checkWorkspace(
        { id: this.props.user },
        getDefaultApiCallback(({ httpResponse }) => {
          this.setState({
            workspaceUpdateDialogOpen: JSON.parse(httpResponse.text).metadata
              .needsUpdate
          });
        })
      );
    }
  }

  render() {
    const { classes } = this.props;

    // the items on the left-side menu
    const linkItems = Constants.NAVBAR_ITEMS.filter(
      item => !item.REQUIRE_ADMIN || this.state.isAdmin
    ).map(item => <MenuSideBar key={item.NAME} item={item} />);

    return (
      <div className={classes.root}>
        <div className={classes.appFrame}>
          <AppBar
            className={classNames(
              classes.appBar,
              this.state.open && classes.appBarShift
            )}
          >
            <Toolbar disableGutters={true}>
              {this.props.isAuthenticated && (
                <IconButton
                  color="inherit"
                  aria-label="open drawer"
                  onClick={this.handleDrawerAction}
                  className={classNames(classes.menuButton)}
                >
                  <MenuIcon />
                </IconButton>
              )}
              <Typography
                variant="h6"
                color="inherit"
                className={classes.appBarTitle}
                style={{ marginLeft: !this.props.isAuthenticated ? 96 : 0 }}
              >
                {APP_NAME}
              </Typography>
              <ProjectSelector />
              <UserMenu />
            </Toolbar>
          </AppBar>
          {/* Only show side menu when the user is logged in */}
          {this.props.isAuthenticated && (
            <Drawer
              variant="permanent"
              classes={{
                paper: classNames(
                  classes.drawerPaper,
                  !this.state.open && classes.drawerPaperClose
                )
              }}
              open={this.state.open}
            >
              <div className={classes.drawerInner}>
                <div className={classes.drawerHeader} />
                <List className={classes.list}>{linkItems}</List>
              </div>
            </Drawer>
          )}
          <main className={classes.content}>
            <Grid container spacing={3} className={classes.grid}>
                {this.state.isAuthenticationChecked && (
                  <ContentContainer />
                )}
            </Grid>
          </main>
        </div>

        <ToastContainer
          position="bottom-center"
          autoClose={2500}
          type={toast.TYPE.INFO}
          hideProgressBar={true}
          newestOnTop={true}
          closeOnClick
        />
      </div>
    );
  }
}

App.propTypes = {
  classes: PropTypes.object.isRequired,
  theme: PropTypes.object.isRequired,
  cookies: PropTypes.instanceOf(Cookies).isRequired
};

export default withRouter(
  withCookies(
    connect(
      ReduxUtils.mapStateToProps,
      ReduxUtils.mapDispatchToProps
    )(withStyles(APP_STYLES, { withTheme: true })(App))
  )
);

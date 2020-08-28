import React, { Component } from "react";
import { connect } from "react-redux";
import PropTypes from "prop-types";
import { toast } from "react-toastify";

// material-ui components
import { withStyles } from "@material-ui/core/styles";
import Button from "@material-ui/core/Button";
import DeployIcon from "@material-ui/icons/Add";
import TextField from "@material-ui/core/TextField";

import KeyValueDialog from "../../app/components/KeyValueDialog";

// base components
import * as ProcessToast from "../../../components/ProcessToast";

// controller
import {
  projectsApi,
  getDefaultApiCallback,
  toastErrorMessage,
  toastErrorType
} from "../../../services/client/ml-lab-api";
import * as ReduxUtils from "../../../services/handler/reduxUtils";
import * as Parser from "../../../services/handler/parser";
import { SERVICE_NAME_REGEX } from "../../../services/handler/constants";

const WAIT_TIME = 3000;
const WAIT_LIMIT = 100000;

const styles = theme => ({
  invalidInput: {
    color: "#bd0000"
  },
  keyValueForm: {
    padding: "10px 0px",
    overflow: "auto",
    maxHeight: "300px"
  },
  keyValueText: {
    margin: "10px 10px 10px 0px"
  },
  keyValueButton: {
    minWidth: "35px",
    padding: "8px 5px"
  },
  button: {
    //margin: theme.spacing.unit,
    marginLeft: 0,
    padding: "6px 16px"
  },
  addParameterButton: {
    //margin: theme.spacing.unit,
    marginLeft: 0,
    paddingLeft: "0px"
  },
  rightIcon: {
    marginLeft: theme.spacing(1)
  }
});

class DeployServiceControl extends Component {
  constructor(props) {
    super(props);
    this.state = {
      open: false,
      deploying: []
    };

    this.handleClickOpen = this.handleClickOpen.bind(this);
    this.handleRequestClose = this.handleRequestClose.bind(this);
    this.handleServiceDeploy = this.handleServiceDeploy.bind(this);
    this.checkDeployingStatus = this.checkDeployingStatus.bind(this);
  }

  handleClickOpen() {
    this.setState({ open: true });
  }

  handleRequestClose = function() {
    this.setState({
      open: false
    });
  }.bind(this);

  handleServiceDeploy(imageName, keyValuePairs, additionalComponentInput) {
    additionalComponentInput = additionalComponentInput || {};

    var deploying = this.state.deploying;
    deploying.push(imageName);
    this.setState({
      deploying: deploying
    });

    var toastID = ProcessToast.showProcessToast("Service will be created...");

    var configuration = {};
    for (var i = 0; i < keyValuePairs.length; i++) {
      var keyValuePair = keyValuePairs[i];
      configuration[keyValuePair.key] = keyValuePair.value;
    }

    projectsApi.deployService(
      this.props.currentProject,
      imageName,
      { name: additionalComponentInput.serviceName, body: configuration },
      getDefaultApiCallback(
        ({ result }) => {
          if (result.data) {
            var deployServiceName = result.data.name;

            setTimeout(
              function() {
                if (this.state.deploying.includes(imageName)) {
                  this.checkDeployingStatus(
                    0,
                    deployServiceName,
                    imageName,
                    toastID
                  );
                }
              }.bind(this),
              WAIT_TIME
            );
          }
        },
        ({ error, errorBody }) => {
          this.spliceDeployingArray(imageName);

          toast.dismiss(toastID);
          let message = errorBody.message || error;
          toastErrorType("Deploy Service: ", message);
        }
      )
    );

    this.handleRequestClose();
  }

  checkDeployingStatus(waited_for_ms, deployServiceName, imageName, toastID) {
    projectsApi.getService(
      this.props.currentProject,
      deployServiceName,
      {},
      getDefaultApiCallback(
        ({ result }) => {
          if (
            !result.data.isHealthy &&
            this.state.deploying.includes(imageName)
          ) {
            // compare waiting time to limit (max waiting time)
            if (waited_for_ms > WAIT_LIMIT) {
              // timed out
              this.spliceDeployingArray(imageName);

              toast.dismiss(toastID);
              toastErrorMessage("Deploy service: ", {
                message: "Timeout"
              });
            } else {
              // check for creation status again after the defined waiting time
              setTimeout(
                function() {
                  this.checkDeployingStatus(
                    waited_for_ms + WAIT_TIME,
                    deployServiceName,
                    imageName,
                    toastID
                  );
                }.bind(this),
                WAIT_TIME
              );
            }
          } else {
            // service successfully deployed
            // dismiss progress toast and show confirmation
            toast.dismiss(toastID);
            toast.success(
              "Service " + Parser.truncate(imageName, 40) + " deployed."
            );
            this.props.onServiceDeploy();

            this.spliceDeployingArray(imageName);
          }
        },
        ({ error }) => {
          this.spliceDeployingArray(imageName);

          toast.dismiss(toastID);
          toastErrorMessage("Deploy Service: ", error);
        }
      )
    );
  }

  spliceDeployingArray(imageName) {
    var deploying = this.state.deploying;
    var index = deploying.indexOf(imageName);

    if (index > -1) {
      deploying.splice(index, 1);
    }

    this.setState({
      deploying: deploying
    });
  }

  render() {
    const { classes } = this.props;

    return (
      <div>
        <Button
          color="primary"
          className={classes.button}
          onClick={this.handleClickOpen}
        >
          Add Service
          <DeployIcon className={classes.rightIcon} />
        </Button>
        <KeyValueDialog
          open={this.state.open}
          title="Deploy project service"
          contentText={
            "Deploy a new service to the selected project based on the specific Docker image." +
            " Please make sure that the image is a compatible ML Lab service."
          }
          primaryActionBtnLabel="Deploy"
          handleRequestClose={this.handleRequestClose}
          handlePrimaryAction={this.handleServiceDeploy}
          additionalDialogComponent={withStyles(styles)(
            NameFieldDialogExtension
          )}
        />
      </div>
    );
  }
}

DeployServiceControl.propTypes = {
  classes: PropTypes.object.isRequired,
  onServiceDeploy: PropTypes.func.isRequired,
  currentProject: PropTypes.string.isRequired
};

class NameFieldDialogExtension extends React.Component {
  state = {
    serviceName: "",
    isInvalidInput: false
  };

  handleInputChange = e => {
    const newServiceName = e.target.value;
    const isInvalidInput = !SERVICE_NAME_REGEX.test(newServiceName);

    this.setState(
      {
        serviceName: newServiceName,
        isInvalidInput: isInvalidInput
      },
      () => {
        this.props.onAction({ ...this.state });
      }
    );
  };

  render() {
    return (
      <TextField
        margin="dense"
        // id={tfId}
        label="Service Name (optional)"
        type="text"
        value={this.state.serviceName}
        name="serviceName"
        autoComplete="on"
        onChange={e => this.handleInputChange(e)}
        fullWidth
        InputProps={{
          classes: {
            input: this.state.isInvalidInput
              ? this.props.classes.invalidInput
              : null
          }
        }}
      />
    );
  }
}

export default connect(ReduxUtils.mapStateToProps)(
  withStyles(styles)(DeployServiceControl)
);

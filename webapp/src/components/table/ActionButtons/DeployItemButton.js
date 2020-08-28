import React, { Component } from "react";
import { connect } from "react-redux";
import { toast } from "react-toastify";
import PropTypes from "prop-types";

// material-ui components
import Icon from "@material-ui/core/Icon";
import IconButton from "@material-ui/core/IconButton";
import Tooltip from "@material-ui/core/Tooltip";
import Select from "@material-ui/core/Select";
import MenuItem from "@material-ui/core/MenuItem";

//base components
import * as ProcessToast from "../../../components/ProcessToast";
import CustomDialog from "../../../components/CustomDialog";
import KeyValueList from "../../../scenes/app/components/KeyValueList";

import * as ReduxUtils from "../../../services/handler/reduxUtils";
import {
  projectsApi,
  administrationApi,
  getDefaultApiCallback,
  toastErrorMessage
} from "../../../services/client/ml-lab-api";
import * as Constants from "../../../services/handler/constants";

const WAIT_TIME = 3000;
const WAIT_LIMIT = 30000;
const DEFAULT_DEPLOYMENT_INFRASTRUCTURE = "mllab";

/**
 * Component that is used in the model view to deploy a model to a model-deployment infrastructure
 */
class DeployItemButton extends Component {
  constructor(props) {
    super(props);

    // infrastructure is used for the DropDown selection when the infrastructure is chosen. Will contain the name of the image-to-be-deployed.
    this.state = {
      deploying: [],
      open: false,
      selectedModel: "",
      deploymentInfrastructure: DEFAULT_DEPLOYMENT_INFRASTRUCTURE,
      keyValuePairs: []
    };

    this.handleDeployButtonClick = this.handleDeployButtonClick.bind(this);
    this.checkDeployingStatus = this.checkDeployingStatus.bind(this);
    this.handleRequestClose = this.handleRequestClose.bind(this);
    this.deploy = this.deploy.bind(this);
    this.handleDropDownChange = this.handleDropDownChange.bind(this);
  }

  deploy() {
    let item = this.state.selectedModel;
    // get list of projects that are being deployed
    var deploying = this.state.deploying;
    // add project to list of deploying projects
    deploying.push(item.key);

    this.setState({
      deploying: deploying
    });

    var toastID = ProcessToast.showProcessToast("Model will be deployed...");

    // remap the keyValuePairs array as it also includes and index field
    var configuration = this.state.keyValuePairs.length > 0 ? {} : undefined;
    for (var i = 0; i < this.state.keyValuePairs.length; i++) {
      var keyValuePair = this.state.keyValuePairs[i];
      configuration[keyValuePair.key] = keyValuePair.value;
    }

    //Deploy Model
    projectsApi.deployModel(
      this.props.currentProject,
      item.key,
      getDefaultApiCallback(
        ({ result }) => {
          var deployFileName = result.data.name;

          // check deploying status
          setTimeout(
            function() {
              if (this.state.deploying.includes(item.key)) {
                this.checkDeployingStatus(0, item, deployFileName, toastID);
              }
            }.bind(this),
            WAIT_TIME
          );
        },
        ({ error }) => {
          this.spliceDeployingArray(item);
          toast.dismiss(toastID);
          toastErrorMessage("Deploy Model: ", error);
        }
      )
    );

    this.handleRequestClose();
  }

  handleDeployButtonClick(item) {
    this.setState({ open: true, selectedModel: item });
  }

  // close dialog
  handleRequestClose() {
    this.setState({ open: false });
  }

  handleDropDownChange(event, child) {
    this.setState({ deploymentInfrastructure: event.target.value });
  }

  checkDeployingStatus(waited_for_ms, item, deployFileName, toastID) {
    projectsApi.getService(
      this.props.currentProject,
      deployFileName,
      {},
      getDefaultApiCallback(
        ({ result }) => {
          if (
            !result.data.isHealthy &&
            this.state.deploying.includes(item.key)
          ) {
            // compare waiting time to limit (max waiting time)
            if (waited_for_ms > WAIT_LIMIT) {
              // timed out
              toast.dismiss(toastID);

              //remove item from deploying array
              this.spliceDeployingArray(item);
              toast.dismiss(toastID);
              toastErrorMessage("Deploy Model: ", {
                message: "Timeout"
              });
            } else {
              // check for creation status again after the defined waiting time
              setTimeout(
                function() {
                  this.checkDeployingStatus(
                    waited_for_ms + WAIT_TIME,
                    item,
                    deployFileName,
                    toastID
                  );
                }.bind(this),
                WAIT_TIME
              );
            }
          } else {
            // file successfully deployed
            // dismiss progress toast and show confirmation
            toast.dismiss(toastID);
            toast.success("Model deployed.");

            // remove item from deploying array
            this.spliceDeployingArray(item);
          }
        },
        ({ error }) => {
          this.spliceDeployingArray(item);
          toast.dismiss(toastID);
          toastErrorMessage("Deploy Model: ", error);
        }
      )
    );
  }

  spliceDeployingArray(item) {
    var deploying = this.state.deploying;
    var index = deploying.indexOf(item.key);
    if (index > -1) {
      deploying.splice(index, 1);
    }
  }

  render() {
    const title = "Deploy Model";
    const contentText = "Select the landscape you want to deploy the model on:";
    const CustomComponent = (
      <div>
        <Select
          value={this.state.deploymentInfrastructure}
          onChange={this.handleDropDownChange}
        >
          <MenuItem value="mllab">ML Lab Infrastructure</MenuItem>
        </Select>
        <KeyValueList
          onKeyValuePairChange={keyValuePairs =>
            this.setState({ keyValuePairs })
          }
        />
      </div>
    );

    return (
      <Tooltip title="Deploy" placement="bottom">
        <span>
          <IconButton
            onClick={e => this.handleDeployButtonClick(this.props.item)}
          >
            <Icon>play_arrow</Icon>
          </IconButton>
          <CustomDialog
            open={this.state.open}
            title={title}
            contentText={contentText}
            cancelBtnDisabled={false}
            primaryActionBtnDisabled={false}
            primaryActionBtnLabel={"Deploy"}
            handleRequestClose={this.handleRequestClose}
            handlePrimaryAction={this.deploy}
            CustomComponent={CustomComponent}
          />
        </span>
      </Tooltip>
    );
  }
}

DeployItemButton.propTypes = {
  item: PropTypes.object.isRequired,
  currentProject: PropTypes.string.isRequired
};

export default connect(ReduxUtils.mapStateToProps)(DeployItemButton);

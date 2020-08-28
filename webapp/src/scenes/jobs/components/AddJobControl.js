import React, { Component } from "react";
import { connect } from "react-redux";
import PropTypes from "prop-types";
import { toast } from "react-toastify";
import { withStyles } from "@material-ui/core/styles";

import Button from "@material-ui/core/Button";
import CreateButton from "@material-ui/icons/Add";
import TextField from "@material-ui/core/TextField";

// base components
import * as ProcessToast from "../../../components/ProcessToast";

// scene components
import KeyValueDialog from "../../app/components/KeyValueDialog";

// controller
import {
  projectsApi,
  getDefaultApiCallback,
  toastErrorType
} from "../../../services/client/ml-lab-api";
import * as ReduxUtils from "../../../services/handler/reduxUtils";
import { SERVICE_NAME_REGEX } from "../../../services/handler/constants";

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
  addParameterButton: {
    //margin: theme.spacing.unit,
    marginLeft: 0,
    paddingLeft: "0px"
  },
  rightIcon: {
    marginLeft: theme.spacing(1)
  }
});

class AddJobControl extends Component {
  constructor(props) {
    super(props);
    this.state = {
      open: false
    };

    this.handleClickOpen = this.handleClickOpen.bind(this);
    this.handleRequestClose = this.handleRequestClose.bind(this);
    this.handleAddJob = this.handleAddJob.bind(this);
  }

  handleClickOpen() {
    this.setState({ open: true });
  }

  handleRequestClose() {
    this.setState({
      open: false,
      image: ""
    });
  }

  handleAddJob(image, keyValuePairs, additionalComponentInput) {
    additionalComponentInput = additionalComponentInput || {};
    var toastID = ProcessToast.showProcessToast("Job will be added...");

    var configuration = {};
    for (var i = 0; i < keyValuePairs.length; i++) {
      var keyValuePair = keyValuePairs[i];
      configuration[keyValuePair.key.toUpperCase()] = keyValuePair.value;
    }

    let schedule = additionalComponentInput.scheduleCron;
    let name = additionalComponentInput.jobName;

    projectsApi.deployJob(
      this.props.currentProject,
      image,
      { schedule: schedule, name: name, body: configuration },
      getDefaultApiCallback(
        () => {
          toast.dismiss(toastID);
          const successMessage = name ? "Job " + name + " added." : "Job added.";
          toast.success(successMessage);
          this.props.onReload();
        },
        ({ httpResponse }) => {
          toast.dismiss(toastID);
          const errorObj = httpResponse.body.errors;
          const errorMessage = errorObj.message + " (" + errorObj.type + ")";
          toastErrorType(errorMessage);
        }
      )
    );

    this.handleRequestClose();
  }

  render() {
    const { classes, type } = this.props;

    let buttonText = "Run Job";
    let contentText =
      "Run a new job in the selected project based on the specific Docker image." +
      " Please make sure that the image is a compatible ML Lab job.";
    let additionalComponent = withStyles(styles)(NameFieldDialogExtension);
    if (type === "scheduledJob") {
      buttonText = "Schedule Job";
      contentText =
        "Schedule a new job for the selected project based on the specific Docker image." +
        " Please make sure that the image is a compatible ML Lab job.";
      additionalComponent = withStyles(styles)(ScheduleJobDialogExtension);
    }

    return (
      <div>
        <Button color="primary" onClick={this.handleClickOpen} style={{ whiteSpace: "nowrap" }}>
          {buttonText}
          <CreateButton className={classes.rightIcon} />
        </Button>

        <KeyValueDialog
          open={this.state.open}
          title={buttonText}
          contentText={contentText}
          primaryActionBtnLabel="Add"
          handleRequestClose={this.handleRequestClose}
          handlePrimaryAction={this.handleAddJob}
          additionalDialogComponent={additionalComponent}
        />
      </div>
    );
  }
}

AddJobControl.propTypes = {
  classes: PropTypes.object.isRequired,
  onReload: PropTypes.func.isRequired,
  currentProject: PropTypes.string.isRequired,
  type: PropTypes.oneOf(["job", "scheduledJob"]).isRequired
};

class NameFieldDialogExtension extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      jobName: "",
      isInvalidInput: false
    };
  }

  handleInputChange = e => {
    const newJobName = e.target.value;
    const isInvalidInput = !SERVICE_NAME_REGEX.test(newJobName);

    this.setState(
      {
        jobName: newJobName,
        isInvalidInput: isInvalidInput
      },
      () => this.props.onAction({ ...this.state })
    );
  };

  render() {
    return (
      <TextField
        margin="dense"
        // id={tfId}
        label="Job Name (optional)"
        type="text"
        value={this.state.jobName}
        name="jobName"
        autoComplete="off"
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

const CRON_FORMAT_REGEX = /^((\*|([0-9]|1[0-9]|2[0-9]|3[0-9]|4[0-9]|5[0-9])|\*\/([0-9]|1[0-9]|2[0-9]|3[0-9]|4[0-9]|5[0-9])) (\*|([0-9]|1[0-9]|2[0-3])|\*\/([0-9]|1[0-9]|2[0-3])) (\*|([1-9]|1[0-9]|2[0-9]|3[0-1])|\*\/([1-9]|1[0-9]|2[0-9]|3[0-1])) (\*|([1-9]|1[0-2])|\*\/([1-9]|1[0-2])) (\*|([0-6])|\*\/([0-6])))?$/;
class ScheduleJobDialogExtension extends React.Component {
  state = {
    jobName: "",
    scheduleCron: "",
    isCronInvalid: false,
    isJobNameInvalid: false
  };

  handleScheduleCronInput = e => {
    const scheduleCron = e.target.value;
    const isCronInvalid = !CRON_FORMAT_REGEX.test(scheduleCron);
    this.setState(
      {
        ...this.state,
        scheduleCron: scheduleCron,
        isCronInvalid: isCronInvalid,
        isInvalidInput: isCronInvalid || this.state.isJobNameInvalid
      },
      () => this.props.onAction({ ...this.state })
    );
  };

  handleNameInput = result => {
    this.setState(
      {
        ...this.state,
        jobName: result.jobName,
        isJobNameInvalid: result.isInvalidInput,
        isInvalidInput: this.state.isCronInvalid || result.isInvalidInput
      },
      () => this.props.onAction(this.state)
    );
  };

  render() {
    const { classes } = this.props;

    return (
      <div>
        <NameFieldDialogExtension
          onAction={e => this.handleNameInput(e)}
          classes={this.props.classes}
        />
        <TextField
          margin="dense"
          // id={tfId}
          name="jobSchedule"
          autoComplete="off"
          label="Schedule via Cron (* * * * *)"
          value={this.state.scheduleCron}
          type="text"
          onChange={e => this.handleScheduleCronInput(e)}
          fullWidth
          InputProps={{
            classes: {
              input: this.state.isCronInvalid ? classes.invalidInput : null
            }
          }}
        />
      </div>
    );
  }
}

export default connect(ReduxUtils.mapStateToProps)(
  withStyles(styles)(AddJobControl)
);

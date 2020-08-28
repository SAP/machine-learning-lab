import React, { Component } from "react";
import { withRouter } from "react-router";
import { connect } from "react-redux";
import PropTypes from "prop-types";
import { toast } from "react-toastify";
import { withCookies, Cookies } from "react-cookie";

// material-ui components
import { withStyles } from "@material-ui/core/styles";
import TextField from "@material-ui/core/TextField";

// base components
import CustomDialog from "../../../components/CustomDialog";
import * as ProcessToast from "../../../components/ProcessToast";

// scene components
import CreateProjectButton from "./CreateProjectButton";

//controller
import {
  LabApi,
  projectsApi,
  getDefaultApiCallback,
  toastErrorMessage,
  toastErrorType
} from "../../../services/client/ml-lab-api";
import * as ReduxUtils from "../../../services/handler/reduxUtils";

const styles = theme => ({
  progress: {
    margin: "auto"
  },
  toast: {
    textAlign: "center"
  },
  invalidInput: {
    color: "#bd0000"
  }
});

class CreateProjectControl extends Component {
  constructor(props) {
    super(props);
    this.state = {
      open: props.open || false,
      projectName: "",
      projectDescription: ""
    };

    this.handleClickOpen = this.handleClickOpen.bind(this);
    this.handleInputChange = this.handleInputChange.bind(this);
    this.handleRequestClose = this.handleRequestClose.bind(this);
    this.handleCreateProject = this.handleCreateProject.bind(this);
  }

  // open dialog
  handleClickOpen(isOpen) {
    this.setState({ open: isOpen });
  }

  handleInputChange(e) {
    this.setState({ [e.target.name]: e.target.value });
  }

  // close dialog
  handleRequestClose() {
    if (this.props.statusCode !== "noProjects") {
      this.setState({
        open: false,
        projectName: ""
      });
    }
  }

  handleCreateProject() {
    let projectName = this.state.projectName;

    projectsApi.isProjectAvailable(projectName, {}, (error, data, response) => {
      if (response.statusCode === 200) {
        // status code 200 => project is available to be created.
        this.createProject();
      } else {
        // project already exists.
        toastErrorType(JSON.parse(response.text).errors.message);
      }
    });
  }

  createProject = () => {
    let projectName = this.state.projectName;

    let toastID = ProcessToast.showProcessToast(
      "Project " + projectName + " will be created..."
    );

    let projectConfig = LabApi.LabProjectConfig.constructFromObject(
      { name: projectName, description: this.state.projectDescription }
    );
    projectsApi.createProject(
      projectConfig,
      {},
      getDefaultApiCallback(
        () => {
          toast.dismiss(toastID);
          toast.success("Project " + projectName + " created.");
          this.setState({
            projectName: "",
            open: false
          });
          this.props.onCreateProject(projectName);
        },
        ({ errorBody }) => {
          this.setState({
            projectName: "",
            open: false
          });

          toast.dismiss(toastID);
          toastErrorMessage("Create project: ", errorBody);
        }
      )
    );
  };

  componentDidUpdate(prevProps) {
    if (prevProps.open !== this.props.open) {
      this.handleClickOpen(this.props.open);
    }
  }

  render() {
    const { classes } = this.props;

    const title = "Create Project";
    const contentText =
      "A project is a digital space for tackling a specific data science use-case. " +
      "It consists of multiple datasets, experiments, models, services, and jobs.";
    const tfId = "projectName";
    const tfLabel = "Project name";
    const isInvalidInput = new RegExp("[^a-zA-Z0-9- ]").test(
      this.state.projectName
    );
    const cancelBtnDisabled = this.props.statusCode === "noProjects";
    const primaryActionBtnDisabled =
      this.state.projectName.length < 3 ||
      new RegExp("[^a-zA-Z0-9- ]").test(this.state.projectName);
    const primaryActionBtnLabel = "Create";

    const Textfield = (
      <TextField
        autoFocus
        margin="dense"
        id={tfId}
        label={tfLabel}
        type="text"
        name="projectName"
        onChange={e => this.handleInputChange(e)}
        fullWidth
        InputProps={{
          classes: { input: isInvalidInput ? classes.invalidInput : null }
        }}
      />
    );

    const DescriptionField = (
      <TextField
        id="standard-textarea"
        label="Project description (optional)"
        multiline
        className={classes.textField}
        name="projectDescription"
        onChange={e => this.handleInputChange(e)}
        margin="dense"
        fullWidth
      />
    );

    const InputFields = (
      <div>
        {Textfield}
        {DescriptionField}
      </div>
    );

    return (
      <div>
        <CreateProjectButton
          handleClickOpen={this.handleClickOpen}
          component={this.props.component}
        />
        <CustomDialog
          open={this.state.open}
          title={title}
          contentText={contentText}
          cancelBtnDisabled={cancelBtnDisabled}
          primaryActionBtnDisabled={primaryActionBtnDisabled}
          primaryActionBtnLabel={primaryActionBtnLabel}
          handleRequestClose={this.handleRequestClose}
          handlePrimaryAction={this.handleCreateProject}
          CustomComponent={InputFields}
        />
      </div>
    );
  }
}

CreateProjectControl.propTypes = {
  classes: PropTypes.object.isRequired,
  statusCode: PropTypes.string.isRequired,
  component: PropTypes.string.isRequired,
  showCreateProjectDialog: PropTypes.bool,
  cookies: PropTypes.instanceOf(Cookies).isRequired // passed by withCookies
};

export default withCookies(
  withRouter(
    connect(
      ReduxUtils.mapStateToProps,
      ReduxUtils.mapDispatchToProps
    )(withStyles(styles)(CreateProjectControl))
  )
);

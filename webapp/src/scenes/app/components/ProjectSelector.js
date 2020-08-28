import React, { Component } from "react";
import { connect } from "react-redux";
import { withCookies, Cookies } from "react-cookie";
import PropTypes from "prop-types";

// material-ui components
import { withStyles } from "@material-ui/core/styles";
import Input from "@material-ui/core/Input";
import MenuItem from "@material-ui/core/MenuItem";
import FormControl from "@material-ui/core/FormControl";
import Select from "@material-ui/core/Select";

//controller
import {
  projectsApi,
  getDefaultApiCallback,
  toastErrorMessage
} from "../../../services/client/ml-lab-api";
import * as ReduxUtils from "../../../services/handler/reduxUtils";
import * as Constants from "../../../services/handler/constants";

const styles = theme => ({
  container: {
    display: "flex",
    flexWrap: "wrap",
    color: "white"
  },
  formControl: {
    margin: theme.spacing(1),
    marginRight: 24,
    minWidth: 120
  },
  select: {
    color: "white"
  }
});

class ProjectSelector extends Component {
  constructor(props) {
    super(props);
    this.state = {
      projects: [],
      nameToProjectIdMapping: {}
    };

    if (this.props.isAuthenticated) {
      this.getProjects();
    }
  }

  getProjects() {
    projectsApi.getProjects(
      {},
      getDefaultApiCallback(
        ({ result }) => {
          let projects = [];
          let nameToProjectIdMapping = {};
          for (let i = 0; i < result.data.length; i++) {
            const projectName = result.data[i].name;
            projects.push(projectName);
            nameToProjectIdMapping[projectName] = result.data[i].id;
          }

          //no projects available
          if (projects.length === 0) {
            //update global state
            this.props.onNoProjectsAvailable();
          } else {
            // get cookies and check whether there is a project saved in a cookie
            const { cookies } = this.props;
            let cookieProject = cookies.get(
              Constants.COOKIES.project,
              Constants.COOKIES.options
            );
            
            this.setState(
              {
                projects: projects.sort(),
                nameToProjectIdMapping: nameToProjectIdMapping
              },
              () => {
                // does the project list contain the project that was stored in a cookie?
                if (projects.includes(cookieProject)) {
                  // set current project (global redux state) to cookie-project
                  this.props.onInpChange({
                    target: {
                      value: cookieProject,
                      projectId: nameToProjectIdMapping[cookieProject]
                    }
                  });
                } else {
                  // select first project of the list
                  cookies.set(
                    Constants.COOKIES.project,
                    projects[0],
                    Constants.COOKIES.options
                  );
                  this.props.onInpChange({
                    target: {
                      value: projects[0],
                      projectId: nameToProjectIdMapping[this.state.projects[0]]
                    }
                  });
                }
              }
            );
          }
        },
        ({ error, httpResponse }) =>
          !httpResponse.isAuthError
            ? toastErrorMessage("Load Projects: ", error)
            : false
      )
    );
  }

  changeProject = e => {
    const project = e.target.value;
    const { cookies } = this.props;
    cookies.set(Constants.COOKIES.project, project, Constants.COOKIES.options);

    this.props.onInpChange({
      target: {
        value: project,
        projectId: this.state.nameToProjectIdMapping[project]
      }
    });
  };

  componentDidUpdate(prevProps) {
    if (this.props.statusCode !== "noProjects" && this.props.isAuthenticated
      && (this.props.currentProject !== prevProps.currentProject || (this.props.currentProject === ""))) {
      // && ((this.props.statusCode === "projectSelected") || (this.props.statusCode === "projectDeleted"))
      // || (prevProps.location && prevProps.location.pathname === "/login")) {
      // const { cookies } = this.props;
      // cookies.set(
      //   Constants.COOKIES.project,
      //   nextProps.currentProject,
      //   Constants.COOKIES.options
      // );
      this.getProjects();
    }
  }

  getOptionElement(projectName) {
    return (
      <MenuItem value={projectName} key={projectName}>
        {projectName}
      </MenuItem>
    );
  }

  render() {
    const { classes } = this.props;
    const oProjectElements = this.state.projects
      .sort()
      .map(project => this.getOptionElement(project));
    const inputForm = <Input id="select-project" />;

    return (
      <div className={classes.container}>
        {this.props.isAuthenticated ? (
          <FormControl className={classes.formControl}>
            <Select
              className={classes.select}
              value={this.props.currentProject}
              onChange={this.changeProject}
              input={inputForm}
            >
              {oProjectElements}
            </Select>
          </FormControl>
        ) : (
          false
        )}
      </div>
    );
  }
}

ProjectSelector.propTypes = {
  classes: PropTypes.object.isRequired,
  cookies: PropTypes.instanceOf(Cookies).isRequired,
  statusCode: PropTypes.string.isRequired,
  currentProject: PropTypes.string.isRequired,
  isAuthenticated: PropTypes.bool.isRequired,
  onInpChange: PropTypes.func.isRequired, // coming from redux
  onNoProjectsAvailable: PropTypes.func.isRequired,
  onCreateProject: PropTypes.func
};

export default withCookies(
  connect(
    ReduxUtils.mapStateToProps,
    ReduxUtils.mapDispatchToProps
  )(withStyles(styles)(ProjectSelector))
);

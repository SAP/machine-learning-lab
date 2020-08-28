import React, { Component } from "react";
import Button from "@material-ui/core/Button";
import {
  Checkbox,
  FormControlLabel,
  FormLabel,
  FormGroup,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  Typography
} from "@material-ui/core";
import { withStyles } from "@material-ui/core/styles";
import {
  authorizationApi,
  projectsApi,
  administrationApi,
  getDefaultApiCallback
} from "../services/client/ml-lab-api";
import { toast } from "react-toastify";
import Card from "@material-ui/core/Card";
import Grid from "@material-ui/core/Grid";

// import { resolveCname } from "dns";

const styles = theme => ({
  button: {
    //margin: theme.spacing.unit,
    marginLeft: 0
  },
  leftIcon: {
    marginRight: theme.spacing(1)
  },
  root: {
    display: "flex",
    flexWrap: "wrap"
  },
  formControl: {
    margin: theme.spacing(1),
    minWidth: 120
  },
  selectEmpty: {
    marginTop: theme.spacing(2)
  },
  projectCheckbox: {
    marginBottom: "-12px"
  }
});

class AdminArea extends Component {
  constructor(props) {
    super(props);
    this.state = {
      user: "",
      userPermissions: [],
      existingUsers: [],
      userProjects: {},
      labProjects: [],
      labProjectsCount: 0,
      labUsers: [],
      labRunExperiments: 0,
      labDownloadedFiles: 0,
      labAdminInfo: [],
      labDatasets: 0,
      labModels: 0,
      labModelsCount: 0,
      labModelsTotalSizeInGB: 0,
      labDatasetsTotalSizeInGB: 0,
      labServices: 0,
      labServicesCount: 0,
      labExperiments: 0,
      labExperimentsCount: 0,
      labJobs: 0,
      labJobsCount: 0,
      labUsersCount: 0,
      labDatasetsCount: 0,
      labSharedProjectsCount: 0,
      labInactiveUserCount: 0,
      labInactiveProjectsCount: 0,
      isAdmin: false,
      labProjectExperimentTuple: []
    };

    this.getProfiles();
    this.getAllProjects();

    this.getStatistics();
  }

  getStatistics() {
    administrationApi.getStatistics(
      {},
      getDefaultApiCallback(
        ({ result }) => {
          if (result !== undefined) {
            let statistics = result.data;
            this.setState({
              labDatasetsCount: statistics["datasetsCount"],
              labDatasetsTotalSize: statistics["datasetsTotalSize"],
              labExperimentsCount: statistics["experimentsCount"],
              labFilesCount: statistics["filesCount"],
              labFilesTotalSize: statistics["filesTotalSize"],
              labJobsCount: statistics["jobsCount"],
              labModelsCount: statistics["modelsCount"],
              labModelsTotalSizeInGB: (
                statistics["modelsTotalSize"] * Math.pow(10, -9)
              )
                .toString()
                .substring(0, 5),
              labDatasetsTotalSizeInGB: (
                statistics["datasetsTotalSize"] * Math.pow(10, -9)
              )
                .toString()
                .substring(0, 5),
              labProjectsCount: statistics["projectsCount"],
              labServicesCount: statistics["servicesCount"],
              labUsersCount: statistics["userCount"],
              labSharedProjectsCount: statistics["sharedProjectsCount"],
              labInactiveUserCount: statistics["inactiveUserCount"],
              labInactiveProjectsCount: statistics["inactiveProjectsCount"],
              labDownloadedFiles: statistics["downloadedFiles"]
            });
            // console.log("Stats getStatistics", result);
          }
        },
        error => {
          console.log("error in getStatistics");
        }
      )
    );
  }

  //lab/admin/info
  handleChange = function(name, value) {
    authorizationApi.getUser(
      value,
      {},
      getDefaultApiCallback(({ result }) => {
        this.setState({
          [name]: value,
          userPermissions: result.data.permissions
        });
      })
    );
  }.bind(this);

  handlePermissionChange = function(e) {
    let permission = e.target.name;
    let userPermissions = this.state.userPermissions;
    if (e.target.checked) {
      userPermissions.push(permission);
    } else {
      var index = userPermissions.indexOf(permission);
      if (index > -1) {
        userPermissions.splice(index, 1);
      }
    }

    this.setState({
      userPermissions: userPermissions
    });
  };

  handleCheck = e => {
    let newProjects = [...this.state.userProjects];
    let target = e.target;
    if (e.target.checked) {
      newProjects = [];
    }
    this.setState(prevState => ({
      [target.name]: target.checked,
      userProjects: newProjects
    }));
  };

  handleAddProject() {
    this.setState(prevState => ({
      userProjects: [...prevState.userProjects, {}]
    }));
  }

  handleSelectProject() {
    this.setState(prevState => ({
      userProjects: [...prevState.userProjects]
    }));
  }

  handleSave() {
    authorizationApi.updatePermissions(
      this.state.user,
      this.state.userPermissions,
      {},
      getDefaultApiCallback(
        () => toast.success("Saving permissions successful."),
        () => toast.error("Saving permissions for user failed.")
      )
    );
  }

  getProfiles() {
    let users = [];

    authorizationApi.getUsers(
      {},
      getDefaultApiCallback(({ result }) => {
        for (let i = 0; i < result.data.length; i++) {
          users.push(result.data[i]);
        }

        this.setState({
          labUsers: users
        });
      })
    );
  }

  getAllProjects() {
    return new Promise((resolve, reject) => {
      projectsApi.getProjects(
        {},
        getDefaultApiCallback(
          ({ result }) => {
            let projects = [];
            let projectsRaw = [];

            for (let i = 0; i < result.data.length; i++) {
              let project_id = "project-" + result.data[i].id;

              projects.push(project_id);
              // console.info('experiment Count', experimentCount);
              // console.info("projectsRaw", projectsRaw);
              // projectsDict[projectsRaw] = {"experimentCount":experimentCount};
              projectsRaw.push(result.data[i].id);
            }
            resolve(projectsRaw);

            this.setState({
              labProjects: projects.sort()
            });
          },
          ({ error }) => {
            toast.error("Could not load projects", error);
          }
        )
      );
    });
  }

  renderProjects() {
    return this.state.labProjects.map((project, index) => {
      let projectChecked =
        this.state.userPermissions.indexOf(project) > -1 ? true : false;

      return (
        <FormControlLabel
          key={index}
          className={this.props.classes.projectCheckbox}
          control={
            <Checkbox
              checked={projectChecked}
              onChange={e => this.handlePermissionChange(e)}
              value={project}
              name={project}
            />
          }
          label={project}
        />
      );
    });
  }

  renderUserSpecificControls() {
    if (this.state.labUsers.length === 0 || this.state.user === "") {
      return false;
    }
    const { classes } = this.props;
    return (
      <div>
        <FormControlLabel
          control={
            <Checkbox
              checked={this.state.userPermissions.indexOf("admin") > -1}
              name="admin"
              onChange={e => this.handlePermissionChange(e)}
              value="admin"
              color="primary"
            />
          }
          label="Admin"
        />
        <div
          style={{
            marginTop: "16px"
          }}
        >
          <FormControl component="fieldset">
            <FormLabel component="legend"> Projects </FormLabel>
            <FormGroup> {this.renderProjects()} </FormGroup>
          </FormControl>
        </div>
        <div>
          <Button
            color="primary"
            variant="contained"
            className={classes.button}
            onClick={e => this.handleSave(e)}
            style={{
              marginTop: "20px"
            }}
          >
            Save
          </Button>
        </div>
      </div>
    );
  }

  render() {
    const { classes } = this.props;

    return (
      // margin: "auto",
      <div
        style={{
          margin: "48px"
        }}
      >
        {/* <p>UserProject: {this.state.userProject.length()}</p>*/}
        {/* float: "left", marginRight: "48px" */}
        <div
          id="userStatisticContainer"
          style={{
            display: "flex"
          }}
        >
          {/* float:"right", marginLeft: "48px" */}
          <div
            id="userManagement"
            style={{
              marginTop: "0px",
              flexGrow: 0,
              flexShrink: 0,
              flexBasis: "30%"
            }}
          >
            <Typography variant="h6"> User Management </Typography>
            <FormControl className={classes.formControl}>
              <InputLabel htmlFor="user"> User </InputLabel>
              <Select
                value={this.state.user}
                onChange={e => this.handleChange(e.target.name, e.target.value)}
                inputProps={{
                  name: "user",
                  id: "user"
                }}
              >
                {this.state.labUsers.map((user, index) => {
                  return (
                    <MenuItem key={index} value={user.id}>
                      {user.id}
                    </MenuItem>
                  );
                })}
              </Select>
            </FormControl>
            {this.renderUserSpecificControls()}
          </div>
          <div
            id="statisticsFeedbackContainer"
            style={{
              flexGrow: 0,
              flexShrink: 0,
              flexBasis: "70%"
            }}
          >
            {/* float: "left", */}
            <div
              style={{
                marginRight: "0px"
              }}
            >
              <Typography variant="h6"> Statistics </Typography>
              {/* <div style={{ marginTop: "20px", padding: "10px", backgroundColor: "#ffffffff" }}> */}
              <Grid container spacing={3}>
                <Grid item xs={2}>
                  <Card>
                    <Typography
                      style={{
                        padding: "5px",
                        color: "#676767de"
                      }}
                    >
                      Users
                    </Typography>
                    <Typography
                      variant="h6"
                      style={{
                        padding: "5px",
                        fontWeight: "400"
                      }}
                    >
                      {this.state.labUsersCount}
                    </Typography>
                  </Card>
                </Grid>
                <Grid item xs={2}>
                  <Card>
                    <Typography
                      style={{
                        padding: "5px",
                        color: "#676767de"
                      }}
                    >
                      Projects
                    </Typography>
                    <Typography
                      variant="h6"
                      style={{
                        padding: "5px",
                        fontWeight: "400"
                      }}
                    >
                      {this.state.labProjectsCount}
                    </Typography>
                  </Card>
                </Grid>
                <Grid item xs={2}>
                  <Card>
                    <Typography
                      style={{
                        padding: "5px",
                        color: "#676767de"
                      }}
                    >
                      Experiments
                    </Typography>
                    <Typography
                      variant="h6"
                      style={{
                        padding: "5px",
                        fontWeight: "400"
                      }}
                    >
                      {this.state.labExperimentsCount}
                    </Typography>
                  </Card>
                </Grid>
                <Grid item xs={2}>
                  <Card>
                    <Typography
                      style={{
                        padding: "5px",
                        color: "#676767de"
                      }}
                    >
                      Downl.Files
                    </Typography>
                    <Typography
                      variant="h6"
                      style={{
                        padding: "5px",
                        fontWeight: "400"
                      }}
                    >
                      {this.state.labDownloadedFiles}
                    </Typography>
                  </Card>
                </Grid>
                <Grid item xs={2}>
                  <Card>
                    <Typography
                      style={{
                        padding: "5px",
                        color: "#676767de"
                      }}
                    >
                      Jobs
                    </Typography>
                    <Typography
                      variant="h6"
                      style={{
                        padding: "5px",
                        fontWeight: "400"
                      }}
                    >
                      {this.state.labJobsCount}
                    </Typography>
                  </Card>
                </Grid>
                <Grid item xs={2}>
                  <Card>
                    <Typography
                      style={{
                        padding: "5px",
                        color: "#676767de"
                      }}
                    >
                      Datasets
                    </Typography>
                    <Typography
                      variant="h6"
                      style={{
                        padding: "5px",
                        fontWeight: "400"
                      }}
                    >
                      {this.state.labDatasetsCount}
                    </Typography>
                  </Card>
                </Grid>
                <Grid item xs={2}>
                  <Card>
                    <Typography
                      style={{
                        padding: "5px",
                        color: "#676767de"
                      }}
                    >
                      Datasets Size in GB
                    </Typography>
                    <Typography
                      variant="h6"
                      style={{
                        padding: "5px",
                        fontWeight: "400"
                      }}
                    >
                      {this.state.labDatasetsTotalSizeInGB}
                    </Typography>
                  </Card>
                </Grid>
                <Grid item xs={2}>
                  <Card>
                    <Typography
                      style={{
                        padding: "5px",
                        color: "#676767de"
                      }}
                    >
                      Models
                    </Typography>
                    <Typography
                      variant="h6"
                      style={{
                        padding: "5px",
                        fontWeight: "400"
                      }}
                    >
                      {this.state.labModelsCount}
                    </Typography>
                  </Card>
                </Grid>
                <Grid item xs={2}>
                  <Card>
                    <Typography
                      style={{
                        padding: "5px",
                        color: "#676767de"
                      }}
                    >
                      Models Size in GB
                    </Typography>
                    <Typography
                      variant="h6"
                      style={{
                        padding: "5px",
                        fontWeight: "400"
                      }}
                    >
                      {this.state.labModelsTotalSizeInGB}
                    </Typography>
                  </Card>
                </Grid>
                <Grid item xs={2}>
                  <Card>
                    <Typography
                      style={{
                        padding: "5px",
                        color: "#676767de"
                      }}
                    >
                      Services
                    </Typography>
                    <Typography
                      variant="h6"
                      style={{
                        padding: "5px",
                        fontWeight: "400"
                      }}
                    >
                      {this.state.labServicesCount}
                    </Typography>
                  </Card>
                </Grid>
                <Grid item xs={2}>
                  <Card>
                    <Typography
                      style={{
                        padding: "5px",
                        color: "#676767de"
                      }}
                    >
                      Shared Projects
                    </Typography>
                    <Typography
                      variant="h6"
                      style={{
                        padding: "5px",
                        fontWeight: "400"
                      }}
                    >
                      {this.state.labSharedProjectsCount}
                    </Typography>
                  </Card>
                </Grid>
                <Grid item xs={2}>
                  <Card>
                    <Typography
                      style={{
                        padding: "5px",
                        color: "#676767de"
                      }}
                    >
                      Inactive User
                    </Typography>
                    <Typography
                      variant="h6"
                      style={{
                        padding: "5px",
                        fontWeight: "400"
                      }}
                    >
                      {this.state.labInactiveUserCount}
                    </Typography>
                  </Card>
                </Grid>
                <Grid item xs={2}>
                  <Card>
                    <Typography
                      style={{
                        padding: "5px",
                        color: "#676767de"
                      }}
                    >
                      Inactive Projects
                    </Typography>
                    <Typography
                      variant="h6"
                      style={{
                        padding: "5px",
                        fontWeight: "400"
                      }}
                    >
                      {this.state.labInactiveProjectsCount}
                    </Typography>
                  </Card>
                </Grid>
                {/* <p>Feedback: { (this.state.labFeedback.metadata !== undefined) ? this.state.labFeedback.metadata["itemCount"]: " "}</p>  */}
              </Grid>
              {/* </div> */}
            </div>
          </div>
        </div>
      </div>
    );
  }
}

// item xs={6} sm={6} spacing={4}
export default withStyles(styles)(AdminArea);

const ACTION_AUTHENTICATION = "AUTHENTICATION";
const ACTION_EXPERIMENTS_CHANGE = "NEWEXPERIMENTS";
const ACTION_LOGOUT = "logout";

const INITIAL_STATE = {
  user: "",
  currentProject: "",
  statusCode: "startApp",
  isAuthenticated: false,
  experiments: {}
};

function reduceFct(state = [], action) {
  switch (action.type) {
    case "CHANGE":
      return Object.assign({}, state, {
        currentProject: action.project,
        currentProjectId: action.currentProjectId,
        statusCode: action.code
      });
    case ACTION_AUTHENTICATION:
      return Object.assign({}, state, {
        user: action.user,
        isAuthenticated: action.isAuthenticated,
        isAdmin: action.isAdmin
      });
    case ACTION_EXPERIMENTS_CHANGE:
      return Object.assign({}, state, {
        experiments: action.experiments
      });
    case ACTION_LOGOUT:
      return Object.assign({}, state, { ...INITIAL_STATE }); // reset the redux state
    default:
      return state;
  }
}

const mapStateToProps = state => ({
  user: state.user,
  currentProject: state.currentProject,
  currentProjectId: state.currentProjectId,
  statusCode: state.statusCode,
  isAuthenticated: state.isAuthenticated,
  isAdmin: state.isAdmin,
  experiments: state.experiments
});

const mapDispatchToProps = dispatch => {
  return {
    onLogout: () => {
      dispatch({
        type: ACTION_LOGOUT
      });
    },
    onInpChange: projectID => {
      dispatch({
        type: "CHANGE",
        project: projectID.target.value,
        currentProjectId: projectID.target.projectId,
        code: "projectSelected"
      });
    },
    onCreateProject: projectID => {
      dispatch({
        type: "CHANGE",
        project: projectID,
        code: "projectSelected"
      });
    },
    onNoProjectsAvailable: () => {
      dispatch({
        type: "CHANGE",
        project: "",
        code: "noProjects"
      });
    },
    onProjectDelete: newProjectID => {
      dispatch({
        type: "CHANGE",
        project: newProjectID,
        code: "projectDeleted"
      });
    },
    onAuthentication: (user, isAuthenticated, isAdmin) => {
      dispatch({
        type: ACTION_AUTHENTICATION,
        user: user,
        isAuthenticated: isAuthenticated,
        isAdmin: isAdmin
      });
    }
  };
};

export { reduceFct, mapStateToProps, mapDispatchToProps, INITIAL_STATE };

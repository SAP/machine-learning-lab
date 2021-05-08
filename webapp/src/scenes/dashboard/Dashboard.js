import React, { Component } from 'react';
import ReactDOM from 'react-dom';
import { connect } from 'react-redux';
import { withCookies } from 'react-cookie';
import { withStyles } from '@material-ui/core/styles';
import PropTypes from 'prop-types';
import { toast } from 'react-toastify';

import Typography from '@material-ui/core/Typography';
import Button from '@material-ui/core/Button';

//base components
import Widgets from '../../components/Widgets';
import BlockHeader from '../../components/BlockHeader';
import BlockSpacing from '../../components/BlockSpacing';
import CustomDialog from '../../components/CustomDialog';
import * as ProcessToast from '../../components/ProcessToast';

//scene components
import ProjectCards from './components/ProjectCards';
import Card from '@material-ui/core/Card';
import CreateProjectControl from './components/CreateProjectControl';
import ManageProjectDialog from './components/ManageProjectDialog';
import WorkspaceCard from './components/WorkspaceCard'

//controller
import * as Constants from '../../services/handler/constants';
import * as ReduxUtils from '../../services/handler/reduxUtils';
import {
  projectsApi,
  administrationApi,
  getDefaultApiCallback,
  toastErrorMessage,
  toastInfo,
} from '../../services/client/ml-lab-api';
import * as Parser from '../../services/handler/parser';

const styles = (theme) => ({
  dialog: {
    minWidth: 550,
  },
  card: {
    minWidth: 275,
    marginTop: 30,
  },
  allprojectTitle: {
    marginTop: 30,
  },
  progress: {
    margin: 'auto',
  },
  toast: {
    textAlign: 'center',
  },
});

const ORIGINAL_DIALOG = {
  isOpen: false,
  title: '',
  contentText: '',
  action: null,
  customComponent: null,
  primaryActionBtnLabel: '',
  hideCancelButton: false,
  width: 500,
  overwriteButton: false,
  isApiTokenDialog: false,
  showCreateProjectDialog: false,
};

class Dashboard extends Component {
  constructor(props) {
    super(props);
    this.state = {
      widgetdata: Constants.WIDGET_ITEMS_DASHBOARD,
      projectCardsData: [],
      dialog: { ...ORIGINAL_DIALOG },
      deleteProject: '',
    };

    this.onOpenDeleteDialog = this.onOpenDeleteDialog.bind(this);
    this.handleRequestClose = this.handleRequestClose.bind(this);
    this.onDeleteProject = this.onDeleteProject.bind(this);
    this.onSelectProject = this.onSelectProject.bind(this);
    this.onManageProject = this.onManageProject.bind(this);
    this.onGetApiToken = this.onGetApiToken.bind(this);

    this.getProjectCards();
  }

  updateData(props) {
    if (props.statusCode === 'startApp' || props.statusCode === 'noProjects') {
      return;
    }

    var widgetdata = this.state.widgetdata;
    projectsApi.getProject(
      props.currentProject,
      { expand: false },
      getDefaultApiCallback(({ result }) => {
        let stats = result.data.statistics;
        if (!stats) return;
        widgetdata.forEach(function (element) {
          element.VALUE = Parser.SetVariableFormat(
            stats[element.KEY],
            element.FORMAT
          );
        }, this);
        this.setState({ widgetdata });
      })
    );

    this.getProjectCards();
  }

  isFirstTimeUser(props) {
    // the user has to create a project when there are no projects available
    // this.props.location.pathname is coming from the react-router (via the withRouter() call)
    // A user without projects is considered to be a first-time user.
    // Note: If a new user registers and is added to a project before first login, the dialogs won't be shown.
    // if(nextProps.statusCode === 'noProjects' && this.props.component === "Navbar" && nextProps.location.pathname !== "/login"){
    if (
      props.statusCode === 'noProjects' &&
      props.location.pathname !== '/login'
    ) {
      // set a cookie which indicates whether the User logged in for the first time. Can be used to show tutorials etc.
      this.props.cookies.set(
        Constants.COOKIES.firstTimeLogin,
        true,
        Constants.COOKIES.options
      );

      administrationApi.getLabInfo(
        {},
        getDefaultApiCallback(
          ({ result }) => {
            if (
              result.data.termsOfService &&
              result.data.termsOfService !== ''
            ) {
              //TODO: refactor: duplicate code in onGetApiToken
              let TextField = (
                <Typography
                  variant="body1"
                  style={{ wordBreak: 'break-all', fontWeight: 'initial' }}
                >
                  {result.data.termsOfService}
                </Typography>
              );

              this.setState({
                dialog: {
                  ...ORIGINAL_DIALOG,
                  isOpen: true,
                  title: 'Terms of Service',
                  customComponent: TextField,
                  hideCancelButton: true,
                  primaryActionBtnLabel: 'Accept',
                },
              });
            }
            this.setState({ showCreateProjectDialog: true });
          },
          ({ error }) => {
            this.setState({ showCreateProjectDialog: true });
          }
        )
      );
    } else {
      this.setState({ showCreateProjectDialog: false });
    }
  }

  getProjectCards() {
    projectsApi.getProjects(
      {},
      getDefaultApiCallback(
        ({ result }) => {
          this.setState({ projectCardsData: result.data });
        },
        ({ error }) => {
          toastErrorMessage('Load all Projects: ', error);
        }
      )
    );
  }

  onSelectProject(project, projectId) {
    const { cookies } = this.props;
    cookies.set(Constants.COOKIES.project, project, Constants.COOKIES.options);
    this.props.onInpChange({
      target: { value: project, projectId: projectId },
    });
  }

  onDeleteProject() {
    this.handleRequestClose();
    var toastId = ProcessToast.showProcessToast('Project will be deleted...');

    var deleteProject = this.state.deleteProject;

    projectsApi.deleteProject(
      deleteProject,
      {},
      getDefaultApiCallback(() => {
        const projectCardsData = this.state.projectCardsData.filter(function (
          el
        ) {
          return el.name !== deleteProject;
        });

        var noProject = projectCardsData.length === 0 ? true : false;
        var currentProjectDeleted =
          deleteProject === this.props.currentProject ? true : false;

        toast.dismiss(toastId);
        toast.success('Project ' + deleteProject + ' deleted.');

        this.setState({
          projectCardsData,
        });

        if (noProject) {
          this.props.onNoProjectsAvailable();
        } else if (currentProjectDeleted) {
          this.props.onProjectDelete(this.state.projectCardsData[0].name);
        } else {
          this.props.onProjectDelete(this.props.currentProject);
        }

        this.setState({
          deleteProject: '',
        });
      })
    );
  }

  onManageProject(project) {
    projectsApi.getProject(
      project,
      { expand: true },
      getDefaultApiCallback(
        ({ result }) => {
          let manageProjectDialog = (
            <ManageProjectDialog
              project={project}
              projectMembers={result.data.members}
              requestClose={this.handleRequestClose}
            />
          );

          this.setState({
            dialog: {
              ...ORIGINAL_DIALOG,
              isOpen: true,
              title: 'Manage Members',
              // primaryActionBtnLabel: "Ok",
              // action: this.handleRequestClose,
              overwriteButton: true,
              hideCancelButton: true,
              customComponent: manageProjectDialog,
            },
          });
        },
        ({ error }) => toastErrorMessage('Load project: ', error)
      )
    );
  }

  onGetApiToken(project, projectId) {
    // TODO: duplicated code in UserMenu. Refactor?
    projectsApi.createProjectToken(
      projectId,
      {},
      getDefaultApiCallback(({ result }) => {
        let TextField = (
          <Typography
            variant="body1"
            style={{ wordBreak: 'break-all', fontWeight: 'initial' }}
            ref={(node) => (this.apiTokenField = node)}
          >
            {result.data}
          </Typography>
        );

        this.setState({
          dialog: {
            ...ORIGINAL_DIALOG,
            isOpen: true,
            title: 'Project API Token',
            customComponent: TextField,
            hideCancelButton: true,
            isApiTokenDialog: true,
            primaryActionBtnLabel: 'Ok',
          },
          apiToken: result.data,
        });
      })
    );
  }

  onOpenDeleteDialog(project) {
    this.setState({
      dialog: {
        ...ORIGINAL_DIALOG,
        isOpen: true,
        title: 'Delete Project',
        contentText:
          'Do you really want to delete the project ' +
          this.state.deleteProject +
          '?',
        primaryActionBtnLabel: 'Delete',
        overwriteButton: false,
        action: this.onDeleteProject,
      },
      deleteProject: project,
    });
  }

  componentDidUpdate(prevProps) {
    if (
      prevProps.currentProject !== this.props.currentProject ||
      prevProps.statusCode !== this.props.statusCode ||
      prevProps.location.pathname === '/login'
    ) {
      this.updateData(this.props);
      this.isFirstTimeUser(this.props);
    }
  }

  componentDidMount() {
    this.updateData(this.props);
    this.isFirstTimeUser(this.props);
  }

  handleRequestClose() {
    this.setState({
      dialog: {
        ...ORIGINAL_DIALOG,
        isOpen: false,
      },
      deleteProject: '',
    });
  }

  handleCopyKey = (text) => {
    const apiTokenTextField = ReactDOM.findDOMNode(this.apiTokenField);
    Parser.setClipboardText(text, apiTokenTextField);
    toastInfo('Copied to Clipboard');
  };

  render() {
    //CustomDialog
    const copyButton = this.state.dialog.isApiTokenDialog ? (
      <Button
        color="primary"
        onClick={(e) => this.handleCopyKey(this.state.apiToken)}
      >
        Copy
      </Button>
    ) : (
        false
      );

    const cancelBtnDisabled = false;
    const primaryActionBtnDisabled = false;
    return (
      <div style={{ width: '100%' }}>
        <BlockHeader name={this.props.currentProject + ' Project'} />
        <Widgets data={this.state.widgetdata} />
        <BlockSpacing />

        <BlockHeader name={'Workspace'} />
        <WorkspaceCard />
        <BlockSpacing />

        <BlockHeader name="My Projects" />
        <ProjectCards
          onDeleteProject={this.onOpenDeleteDialog}
          onSelectProject={this.onSelectProject}
          onManageProject={this.onManageProject}
          onGetApiToken={this.onGetApiToken}
          currentProject={this.props.currentProject}
          data={this.state.projectCardsData}
        />
        <Card
          className={this.props.classes.CreateProjectDialog}
          style={{ display: 'inline-block', marginTop: '20px' }}
        >
          <CreateProjectControl
            component="Dashboard"
            open={this.state.showCreateProjectDialog}
          />
        </Card>
        <CustomDialog
          classes={styles.dialog}
          open={this.state.dialog.isOpen}
          title={this.state.dialog.title}
          contentText={this.state.dialog.contentText}
          cancelBtnDisabled={cancelBtnDisabled}
          hideCancelBtn={this.state.dialog.hideCancelButton}
          primaryActionBtnDisabled={primaryActionBtnDisabled}
          primaryActionBtnLabel={this.state.dialog.primaryActionBtnLabel}
          handleRequestClose={this.handleRequestClose}
          handlePrimaryAction={
            this.state.dialog.action
              ? this.state.dialog.action
              : this.handleRequestClose
          }
          CustomComponent={this.state.dialog.customComponent}
          overwriteButton={this.state.dialog.overwriteButton}
          dialogContentStyle={{ overflow: 'visible' }}
          moreButtons={[copyButton]}
        />
      </div>
    );
  }
}

Dashboard.propTypes = {
  onNoProjectsAvailable: PropTypes.func.isRequired,
  onInpChange: PropTypes.func.isRequired,
  statusCode: PropTypes.string.isRequired,
  currentProject: PropTypes.string.isRequired,
};

export default withCookies(
  connect(
    ReduxUtils.mapStateToProps,
    ReduxUtils.mapDispatchToProps
  )(withStyles(styles)(Dashboard))
);

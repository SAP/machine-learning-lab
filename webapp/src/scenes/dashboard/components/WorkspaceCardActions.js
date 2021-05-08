import React, { useState, useEffect } from 'react';
import { connect } from 'react-redux';
import { Link } from 'react-router-dom';
import { toast } from 'react-toastify';


// material-ui components
import { withStyles } from '@material-ui/core/styles';
import Button from '@material-ui/core/Button';
import Tooltip from '@material-ui/core/Tooltip';
import { ListItem, Typography } from '@material-ui/core';
import ListItemText from '@material-ui/core/ListItemText';
import CardActions from '@material-ui/core/CardActions';

import DisplayJsonButton from '../../services/components/DisplayJsonButton'
import CustomDialog from '../../../components/CustomDialog'
import SimpleDeploymentDialog from '../../app/components/SimpleDeploymentDialog';
import * as ProcessToast from '../../../components/ProcessToast';


import * as ReduxUtils from '../../../services/handler/reduxUtils';

import {
  administrationApi,
  getDefaultApiCallback,
  toastErrorType,
  toastErrorMessage
} from '../../../services/client/ml-lab-api';

const styles = (theme) => ({
  invalidInput: {
    color: '#bd0000',
  },
  controls: {
    display: 'flex',
    alignItems: 'center',
    paddingLeft: theme.spacing(1),
    paddingBottom: theme.spacing(1),
    // overflow: "scroll"
  },
  keyValueForm: {
    padding: '10px 0px',
    overflow: 'auto',
    maxHeight: '300px',
  },
  keyValueText: {
    margin: '10px 10px 10px 0px',
  },
  keyValueButton: {
    minWidth: '35px',
    padding: '8px 5px',
  },
  button: {
    //margin: theme.spacing.unit,
    marginLeft: 0,
    padding: '6px 16px',
  },
  addParameterButton: {
    //margin: theme.spacing.unit,
    marginLeft: 0,
    paddingLeft: '0px',
  },
  paper: { maxWidth: "800px" },
  rightIcon: {
    marginLeft: theme.spacing(1),
  },
});

function AccessWorkspaceButton(props) {
  const { classes } = props;
  const title = 'Access workspace';
  const itemProps = { component: Link, to: '/workspace' };
  return (
    <div style={{ display: 'inline' }}>
      <Tooltip title={title} placement="bottom">
        <ListItem {...itemProps} button>
          <ListItemText
            disableTypography
            primary={<Typography type="body2" style={{ fontWeight: 500 }}>ACCESS</Typography>}
          />
        </ListItem>
      </Tooltip>
    </div>
  );
}

function RestartWorkspaceButton(props) {
  const { classes } = props;
  const [isDialogOpen, setDialogOpen] = useState(false);

  function handleRequestClose() {
    setDialogOpen(false);
  }

  function handleClickOpenDialog() {
    setDialogOpen(true);
  }

  function handleRestartWorkspace() {
    var toastID = ProcessToast.showProcessToast('Workspace will be re-created...');
    administrationApi.resetWorkspace({ id: props.user },
      getDefaultApiCallback(
        ({ result }) => {
          if (result.data) {
            // TODO: Set a timeout if the deploying is taking too long
            toast.dismiss(toastID);
            props.setWorkspaceData(result.data);
          }
        },
        ({ error, errorBody }) => {
          toast.dismiss(toastID);
          let message = errorBody.message || error;
          toastErrorType('Deploy Workspace: ', message);
          props.checkWorkspace();
        }
      ))

    handleRequestClose();
  }


  return (
    <div>
      <Button onClick={handleClickOpenDialog}>
        RESTART
      </Button>
      <CustomDialog
        open={isDialogOpen}
        title="Restart workspace"
        contentText={
          'Removes the running docker container and  spawns a new one with the same image as before. ' +
          'All files stored under /workspace (default Jupyter path) are persisted. Data within other directories will be removed, e.g. installed libraries or machine configuration. The restart should take a few seconds to a few minutes.'
        }
        primaryActionBtnLabel="Restart"
        primaryActionBtnDisabled={false}
        handleRequestClose={handleRequestClose}
        handlePrimaryAction={handleRestartWorkspace}
        additionalDialogComponent={false}
      />
    </div>
  );
}

function CustomizeWorkspaceButton(props) {
  const { classes } = props;
  const [isDialogOpen, setDialogOpen] = useState(false);

  function handleRequestClose() {
    setDialogOpen(false);
  }

  function handleClickOpenDialog() {
    setDialogOpen(true);
  }

  function handleWorkspaceDeploy(imageName, keyValuePairs, additionalComponentInput) {
    var toastID = ProcessToast.showProcessToast('Deploying custom workspace with image: ' + imageName);
    administrationApi.resetWorkspace({ id: props.user, image: imageName },
      getDefaultApiCallback(
        ({ result }) => {
          if (result.data) {
            // TODO: Set a timeout if the deploying is taking too long
            toast.dismiss(toastID);
            props.setWorkspaceData(result.data);
          }
        },
        ({ error, errorBody }) => {
          toast.dismiss(toastID);
          let message = errorBody.message || error;
          toastErrorType('Deploy Workspace: ', message);
          props.checkWorkspace();
        }
      ))

    handleRequestClose();
  }

  return (
    <div>
      <Button
        //   color="primary"
        // className={classes.button}
        onClick={handleClickOpenDialog}
      >
        CUSTOMIZE
    </Button>
      {/* <KeyValueDialog */}
      <SimpleDeploymentDialog
        open={isDialogOpen}
        title="Customize workspace"
        contentText={
          'Deploy a new workspace based on the specific Docker image.' +
          ' Please make sure that the image is a compatible ML Lab workspace.' +
          ' All data under /workspace will be mounted in the new image in the same place.'
        }
        primaryActionBtnLabel="Deploy"
        handleRequestClose={handleRequestClose}
        handlePrimaryAction={handleWorkspaceDeploy}
      />
    </div>
  );
}

function WorkspaceCardActions(props) {
  const { classes } = props;

  return (
    <CardActions className={classes.controls}>
      <AccessWorkspaceButton />
      <RestartWorkspaceButton
        user={props.user}
        setWorkspaceData={props.setWorkspaceData}
        checkWorkspace={props.checkWorkspace}
        />
      <CustomizeWorkspaceButton
        user={props.user}
        setWorkspaceData={props.setWorkspaceData}
        checkWorkspace={props.checkWorkspace}
        />
      <DisplayJsonButton
        jsonObj={{ ...props.workspaceData }}
        projName={props.workspaceData.name}
      />
    </CardActions>
  );
}

export default connect(ReduxUtils.mapStateToProps)(
  withStyles(styles)(WorkspaceCardActions)
);

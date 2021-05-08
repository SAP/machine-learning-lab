import React, { useState, useEffect } from 'react';
import { connect } from 'react-redux';
import { Link } from 'react-router-dom';


// material-ui components
import { withStyles } from '@material-ui/core/styles';
import Grid from '@material-ui/core/Grid';
import Card from '@material-ui/core/Card';
import CardContent from '@material-ui/core/CardContent';
import CardMedia from '@material-ui/core/CardMedia';
import { ListItem, Typography } from '@material-ui/core';


import WorkspaceCardActions from './WorkspaceCardActions';


import * as Parser from '../../../services/handler/parser';
import * as ReduxUtils from '../../../services/handler/reduxUtils';

import {
  administrationApi,
  getDefaultApiCallback,
  toastErrorMessage
} from '../../../services/client/ml-lab-api';

const styles = (theme) => ({
  gridItem: {
    minWidth: 350,
  },
  card: {
    minWidth: 0,
    maxWidth: 425,
    display: 'flex',
    height: '100%',
  },
  content: {
    flex: '1 0 auto',
  },
  details: {
    display: 'flex',
    flexDirection: 'column',
    // overflow: "scroll"
  },
  pos: {
    marginBottom: 12,
    color: theme.palette.text.secondary,
  },
  projectImage: {
    width: 80,
    height: 80,
    marginTop: theme.spacing(2),
    marginRight: theme.spacing(2),
  },
  description: {
    flex: '1 0 auto',
    fontSize: 13,
    fontWeight: 'initial',
    paddingLeft: 16,
    paddingRight: 16,
    color: 'rgba(0, 0, 0, 0.70)',
  },
  CreateProjectDialog: {
    marginTop: '20px',
    display: 'inline-block',
  },
  Avatar: {
    width: 60,
    height: 60,
    border: 'white 1px solid',
  },
  AvatarActive: {
    borderColor: 'green',
  },
  workspaceImage: {
    // minWidth: 60,
    width: 60,
    height: 60,
    marginTop: theme.spacing(2),
    marginRight: theme.spacing(2),
  },
});

function getWorkspaceName(workspaceName) {
  // var newServiceName = workspaceName.replace(regex, '');
  var workspaceNameTruncated = Parser.truncate(workspaceName, 50);
  return workspaceNameTruncated;
};

function getIconFromName(type) {
  console.log('hello');
  return './ml-workspace-logo.svg';
}


function WorkspaceCard(props) {
  const { classes } = props;
  const defaultWorkspaceData = {
    name: 'Loading...',
    dockerImage: 'Loading image...',
    modifiedAt: '01-01-1970'
  };

  const [workspaceData, setWorkspaceData] = useState(defaultWorkspaceData);

  function checkWorkspace() {
    administrationApi.checkWorkspace(
      { id: props.user },
      getDefaultApiCallback(
        (httpResponse) => {
          var data = httpResponse.httpResponse.body.data;
          setWorkspaceData(data);
        },
        ({ error }) => {
          setWorkspaceData(defaultWorkspaceData);
          toastErrorMessage('Load Workspace: ', error);
        }
      )
    );
  }

  // Passing empty array as second argument helps to render only once
  useEffect(() => {
    checkWorkspace();
  }, []);


  return (
    <Grid item xs={12} sm={12} md={6} lg={props.colSize}>
      <Card className={classes.card}>
        <div className={classes.details}>
          <CardContent className={classes.content}>
            <Typography
              style={{ overflow: 'auto', fontSize: '1.0rem' }}
              variant="h5"
              component="h2"
            >
              {getWorkspaceName(
                workspaceData.name
              )}
            </Typography>
            <Typography variant="subtitle2" className={classes.pos}>
              {workspaceData.dockerImage}
            </Typography>
            <Typography variant="subtitle2" className={classes.pos}>
              {Parser.SetVariableFormat(workspaceData.modifiedAt, 'date')}
            </Typography>
          </CardContent>
          <WorkspaceCardActions
            className={classes.controls}
            checkWorkspace={checkWorkspace}
            workspaceData={workspaceData}
            setWorkspaceData={setWorkspaceData}
            user={props.user} />
        </div>
        <CardMedia
          className={classes.workspaceImage}
          image='./ml-workspace-logo.svg'
        />
      </Card>

    </Grid >
  );
}

export default connect(
  ReduxUtils.mapStateToProps,
  ReduxUtils.mapDispatchToProps
)(withStyles(styles)(WorkspaceCard));
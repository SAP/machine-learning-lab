import React, { useCallback } from 'react';

import Button from '@material-ui/core/Button';
import Grid from '@material-ui/core/Grid';
import PropTypes from 'prop-types';
import superagent from 'superagent';

import './WorkspaceOverview.css';
import { EXTENSION_ENDPOINT } from '../../utils/config';
import { useShowAppDialog } from '../../app/AppDialogServiceProvider';
import CreateWorkspaceDialog from '../CreateWorkspaceDialog';
import WorkspaceCard from '../WorkspaceCard';
import showStandardSnackbar from '../../app/showStandardSnackbar';

function WorkspaceOverview(props) {
  const { userId, workspaces, reloadWorkspaces, accessWorkspace } = props;

  const deleteWorkspace = useCallback(
    async (workspace) => {
      try {
        await superagent
          .delete(
            `${EXTENSION_ENDPOINT}/users/${userId}/workspace/${workspace.id}`
          )
          .withCredentials();
        showStandardSnackbar(
          `Successfully deleted workspace ${workspace.display_name}.`
        );
      } catch (err) {
        let message = err.response?.body?.detail;
        if (typeof message === 'undefined') {
          message = 'Unknown error.';
        }
        showStandardSnackbar(`Failed to delete workspace! ${message}`);
      }
      reloadWorkspaces();
    },
    [reloadWorkspaces, userId]
  );

  const showAppDialog = useShowAppDialog();

  const showCreateWorkspaceDialog = useCallback(() => {
    showAppDialog(CreateWorkspaceDialog, {
      onCreate: async ({ workspaceImage, workspaceName }, onClose) => {
        try {
          await superagent
            .post(`${EXTENSION_ENDPOINT}/users/${userId}/workspace`)
            .withCredentials()
            .send({
              display_name: workspaceName,
              container_image: workspaceImage,
            });
          await new Promise((r) => setTimeout(r, 5000));
          showStandardSnackbar(
            `Successfully created workspace ${workspaceName} with image ${workspaceImage}.`
          );
        } catch (err) {
          let message = err.response?.body?.detail;
          if (typeof message === 'undefined') {
            message = 'Unknown error.';
          }
          showStandardSnackbar(`Failed to start workspace! ${message}`);
        }
        reloadWorkspaces();
        onClose();
      },
    });
  }, [userId, reloadWorkspaces, showAppDialog]);

  // const showModifyWorkspaceDialog = useCallback(() => {
  //   showAppDialog(ModifyWorkspaceDialog, {
  //     onCreate: async ({ workspaceImage }, onClose) => {
  //       try {
  //         // await superagent.put(`../api/users/${userId}/workspace`);
  //         showStandardSnackbar(
  //           `Successfully updated workspace to ${workspaceImage}${userId}.`
  //         );
  //       } catch (err) {
  //         showStandardSnackbar('Failed to start workspace!');
  //       }
  //       reloadWorkspaces();
  //       onClose();
  //     },
  //   });
  // }, [userId, reloadWorkspaces, showAppDialog]);

  const workspaceCards = workspaces.map((workspace) => (
    <WorkspaceCard
      key={workspace.id}
      class="workspace-card"
      workspaceService={workspace}
      onAccessClick={accessWorkspace}
      onModifyClick={() => null}
      onDeleteClick={deleteWorkspace}
    />
  ));

  return (
    <div className="WorkspaceOverview pages-native-component">
      <Button
        className="create-workspace-button"
        variant="contained"
        color="primary"
        onClick={showCreateWorkspaceDialog}
      >
        Create Workspace
      </Button>
      <Grid container spacing={4}>
        {workspaceCards}
      </Grid>
    </div>
  );
}

WorkspaceOverview.propTypes = {
  userId: PropTypes.string.isRequired,
  workspaces: PropTypes.instanceOf(Object).isRequired,
  reloadWorkspaces: PropTypes.func.isRequired,
  accessWorkspace: PropTypes.func.isRequired,
};

export default WorkspaceOverview;

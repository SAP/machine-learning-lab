import React, { useEffect, useState } from 'react';

// import { makeStyles } from '@material-ui/core';
import { useParams } from 'react-router-dom';
import ReactIframe from 'react-iframe';
import Stack from '@mui/material/Stack';

import { CONTAXY_ENDPOINT } from '../utils/config';
import {
  deployWorkspace,
  getWorkspaceManagerConfig,
  startWorkspace,
  updateWorkspace,
} from '../services/workspace';
import { useShowAppDialog } from '../app/AppDialogServiceProvider';
import { useWorkspaces } from '../services/api-hooks';
import WorkspaceConfigButton from '../components/WorkspaceConfigButton';
import WorkspaceConfigDialog from '../components/WorkspaceConfigDialog/WorkspaceConfigDialog';
import WorkspaceStatusPending from '../components/WorkspaceStatusPending';
import WorkspaceStatusStopped from '../components/WorkspaceStatusStopped';

// const useStyles = makeStyles({

// })

function Workspace() {
  // Get the id of the user for which the workspace manager should be shown
  const { userId } = useParams();
  // Workspace manager config
  const [workspaceManagerConfig, setWorkspaceManagerConfig] = useState(null);
  // Request list of workspaces
  const [workspaces, reloadWorkspaces] = useWorkspaces(userId);
  // Tracks if start request was send
  const [workspaceLoading, setWorkspaceLoading] = useState(false);
  // Tracks if workspace deployment request was send
  const [workspaceDeploymentLoading, setWorkspaceDeploymentLoading] =
    useState(false);
  // Required for showing the workspace config dialog
  const showAppDialog = useShowAppDialog();

  const openWorkspaceConfigDialog = (workspace) => {
    showAppDialog(WorkspaceConfigDialog, {
      workspaceManagerConfig,
      currentWorkspace: workspace,
      onUpdate: async (workspaceConfig, onClose) => {
        if (workspace.status !== 'stopped') {
          updateWorkspace(userId, workspace.id, workspaceConfig).finally(() => {
            setTimeout(() => {
              setWorkspaceLoading(false);
              reloadWorkspaces();
            }, 10000);
          });
          setWorkspaceLoading(true);
        } else {
          updateWorkspace(userId, workspace.id, workspaceConfig).finally(() => {
            reloadWorkspaces();
          });
        }
        onClose();
      },
    });
  };

  useEffect(() => {
    getWorkspaceManagerConfig().then((config) => {
      setWorkspaceManagerConfig(config);
    });
  }, []);

  useEffect(() => {
    // Deploy default workspace if no workspace is deployed
    if (!workspaceDeploymentLoading && workspaces?.length === 0) {
      deployWorkspace(userId, { is_stopped: true }).then(() => {
        setWorkspaceDeploymentLoading(false);
        reloadWorkspaces();
      });
      setWorkspaceDeploymentLoading(true);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [workspaces]);

  useEffect(() => {
    // Continuously update the workspace list if no workspace is deployed or deployment is pending
    const workspace = workspaces?.length > 0 ? workspaces[0] : null;
    if (workspace?.status === 'pending') {
      const interval = setInterval(() => {
        reloadWorkspaces();
      }, 5000);
      return () => clearInterval(interval);
    }
    return () => {};
  }, [workspaceLoading, workspaces, reloadWorkspaces]);

  let content;
  if (workspaces == null || workspaces.length === 0) {
    content = null;
  } else {
    // Always show first workspace. Multiple workspaces are not implemented for now.
    const workspace = workspaces[0];
    if (workspaceLoading || workspace.status === 'pending') {
      content = <WorkspaceStatusPending className="workspace-content" />;
    } else if (workspace.status === 'stopped') {
      content = (
        <WorkspaceStatusStopped
          className="workspace-content"
          onStartWorkspace={() => {
            startWorkspace(userId, workspace.id).finally(() => {
              setTimeout(() => {
                setWorkspaceLoading(false);
                reloadWorkspaces();
              }, 10000);
            });
            setWorkspaceLoading(true);
          }}
        />
      );
    } else if (workspace.status === 'running') {
      const backend = CONTAXY_ENDPOINT.replace(/\/api$/, '');
      content = (
        <ReactIframe
          key={workspace.display_name}
          url={`${backend}${workspace.access_url}`}
          allowFullScreen
          className="iframe"
          height="100%"
          width="100%"
          frameBorder="0"
        />
      );
    }
  }
  return (
    <Stack sx={{ height: '100%' }} justifyContent="center" alignItems="center">
      {workspaces &&
      workspaces.length > 0 &&
      !workspaceLoading &&
      workspaceManagerConfig ? (
        <WorkspaceConfigButton
          onClick={() => openWorkspaceConfigDialog(workspaces[0])}
        />
      ) : null}
      {content}
    </Stack>
  );
}

export default Workspace;

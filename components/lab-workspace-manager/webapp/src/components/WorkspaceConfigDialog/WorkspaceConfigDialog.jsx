import React, { useState } from 'react';

import PropTypes from 'prop-types';

import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  Divider,
  Grid,
} from '@mui/material';

import ClearDataCheckbox from './ClearDataCheckbox';
import IdleTimeoutSelect from './IdleTimeoutSelect';
import ImageSelect from './ImageSelect';
import NumberSelect from './NumberSelect';

function WorkspaceConfigDialog(props) {
  const { onUpdate, onClose, currentWorkspace, workspaceManagerConfig } = props;

  const [configValues, setConfigValues] = useState({
    container_image: currentWorkspace.container_image,
    cpus: currentWorkspace.compute.cpus,
    memory: currentWorkspace.compute.memory,
    idle_timeout: currentWorkspace.idle_timeout,
    clear_volume_on_stop: currentWorkspace.clear_volume_on_stop,
  });

  const onChange = (e) => {
    setConfigValues({
      ...configValues,
      [e.target.name]: e.target.value,
    });
  };
  const handleUpdateClick = () => {
    onUpdate(
      {
        container_image: configValues.container_image,
        compute: {
          cpus: parseInt(configValues.cpus, 10),
          memory: parseInt(configValues.memory, 10),
        },
        idle_timeout: configValues.idle_timeout,
        clear_volume_on_stop: configValues.clear_volume_on_stop,
      },
      onClose
    );
  };

  return (
    <Dialog open maxWidth="md">
      <DialogTitle>Workspace Configuration</DialogTitle>
      <DialogContent sx={{ maxWidth: '600px' }}>
        <Grid container spacing={2}>
          <Grid item xs={12}>
            <DialogContentText>
              In this dialog you can update the setting of your personal ML Lab
              workspace. <br />
              Note: Only data in the /workspace directory is persistend and will
              be available after a workspace update.
            </DialogContentText>
          </Grid>
          <Grid item xs={12}>
            <ImageSelect
              label="Workspace Image"
              name="container_image"
              availableImages={workspaceManagerConfig.container_image_options}
              value={configValues.container_image}
              onChange={onChange}
            />
          </Grid>
          <Grid item xs={12}>
            <Divider textAlign="left">Workspace Resources</Divider>
          </Grid>
          <Grid item xs={6}>
            <NumberSelect
              label="Number of CPUs"
              name="cpus"
              value={configValues.cpus}
              onChange={onChange}
              min={1}
              max={workspaceManagerConfig.cpus_max}
              allowedNumbers={workspaceManagerConfig.cpus_options}
            />
          </Grid>
          <Grid item xs={6}>
            <NumberSelect
              label="Memory in MB"
              name="memory"
              value={configValues.memory}
              onChange={onChange}
              min={100}
              max={workspaceManagerConfig.memory_max}
              allowedNumbers={workspaceManagerConfig.memory_options}
            />
          </Grid>
          {workspaceManagerConfig.idle_timeout_options.length > 1 ? (
            <>
              <Grid item xs={12}>
                <Divider textAlign="left">Other Settings</Divider>
              </Grid>
              <Grid item xs={6}>
                <IdleTimeoutSelect
                  label="Stop Workspace After Idle Time"
                  name="idle_timeout"
                  value={configValues.idle_timeout}
                  onChange={onChange}
                  allowedIdleTimeouts={
                    workspaceManagerConfig.idle_timeout_options
                  }
                />
              </Grid>
              <Grid item xs={6} container alignItems="center">
                <ClearDataCheckbox
                  label="Clear all data after the workspace is stopped"
                  name="clear_volume_on_stop"
                  value={configValues.clear_volume_on_stop}
                  onChange={onChange}
                  alwaysClearData={
                    workspaceManagerConfig.always_clear_volume_on_stop
                  }
                />
              </Grid>
            </>
          ) : null}
        </Grid>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} color="primary">
          CANCEL
        </Button>
        <Button onClick={handleUpdateClick} color="primary">
          UPDATE
        </Button>
      </DialogActions>
    </Dialog>
  );
}

WorkspaceConfigDialog.propTypes = {
  onClose: PropTypes.func.isRequired,
  onUpdate: PropTypes.func.isRequired,
  // eslint-disable-next-line react/forbid-prop-types
  workspaceManagerConfig: PropTypes.object.isRequired,
  // eslint-disable-next-line react/forbid-prop-types
  currentWorkspace: PropTypes.object.isRequired,
};

export default WorkspaceConfigDialog;

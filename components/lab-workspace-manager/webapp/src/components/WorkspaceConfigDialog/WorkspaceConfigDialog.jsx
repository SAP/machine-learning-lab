import React, { useState } from 'react';

import PropTypes from 'prop-types';

import {
  Button,
  Checkbox,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  Divider,
  FormControl,
  FormControlLabel,
  Grid,
  InputLabel,
  MenuItem,
  Select,
} from '@mui/material';

import ImageSelect from './ImageSelect';
import NumberSelect from './NumberSelect';

function WorkspaceConfigDialog(props) {
  const { onUpdate, onClose, currentWorkspace, workspaceManagerConfig } = props;

  const [configValues, setConfigValues] = useState({
    container_image: currentWorkspace.container_image,
    cpus: currentWorkspace.compute.cpus,
    memory: currentWorkspace.compute.memory,
  });

  const onChange = (e) => {
    setConfigValues({
      ...configValues,
      [e.target.name]:
        e.target.checked !== undefined ? e.target.checked : e.target.value,
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
          <Grid item xs={12}>
            <Divider textAlign="left">Other Settings</Divider>
          </Grid>
          <Grid item xs={6}>
            <FormControl fullWidth>
              <InputLabel id="max_idle_time">
                Stop Workspace After Idle Time
              </InputLabel>
              <Select
                label="Stop Workspace After Idle Time"
                name="max_idle_time"
                value={configValues.max_idle_time || '4h'}
                onChange={onChange}
              >
                <MenuItem value="1h">1 hour</MenuItem>
                <MenuItem value="2h">2 hours</MenuItem>
                <MenuItem value="4h">4 hours</MenuItem>
                <MenuItem value="8h">8 hours</MenuItem>
                <MenuItem value="never">Never Stop Workspace</MenuItem>
              </Select>
            </FormControl>
          </Grid>
          <Grid item xs={6} container alignItems="center">
            <FormControl fullWidth>
              <FormControlLabel
                control={
                  <Checkbox
                    checked={configValues.clear_data_after_max_idle || false}
                    onChange={onChange}
                    name="clear_data_after_max_idle"
                  />
                }
                label="Clear all data after the workspace is stopped"
              />
            </FormControl>
          </Grid>
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

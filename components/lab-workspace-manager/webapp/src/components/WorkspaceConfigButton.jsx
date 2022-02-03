import React from 'react';

import Box from '@mui/material/Box';
import Icon from '@mui/material/Icon';
import IconButton from '@mui/material/IconButton';
import Tooltip from '@mui/material/Tooltip';

import PropTypes from 'prop-types';

function WorkspaceConfigButton(props) {
  const { onClick } = props;

  return (
    <Box
      sx={{
        position: 'absolute',
        top: '6px',
        left: '10px',
      }}
    >
      <Tooltip title="Workspace Settings">
        <IconButton
          aria-label="workspace-settings"
          size="small"
          onClick={onClick}
        >
          <Icon fontSize="medium">settings</Icon>
        </IconButton>
      </Tooltip>
    </Box>
  );
}

WorkspaceConfigButton.propTypes = {
  onClick: PropTypes.func.isRequired,
};

export default WorkspaceConfigButton;

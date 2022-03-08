import React from 'react';

import Box from '@mui/material/Box';
import LinearProgress from '@mui/material/LinearProgress';

function WorkspaceStatusPending() {
  return (
    <Box
      sx={{
        maxWidth: '500px',
        textAlign: 'center',
      }}
    >
      <h1>Your Workspace is Starting</h1>
      <LinearProgress />
    </Box>
  );
}

export default WorkspaceStatusPending;

import React from 'react';

import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import PropTypes from 'prop-types';

function WorkspaceStatusStopped({ onStartWorkspace }) {
  return (
    <Box
      sx={{
        maxWidth: '500px',
        textAlign: 'center',
      }}
    >
      <h1>Your Workspace is Stopped!</h1>
      <p>
        The ML Lab workspace is your personal data science environment. It
        provides different ways of accessing your code. For example you can do
        it via jupyter or via vs code.
      </p>
      <p>
        You can configure the settings of your workspace using the gear icon in
        the top left corner.
      </p>
      <Button
        variant="outlined"
        size="large"
        color="secondary"
        onClick={onStartWorkspace}
      >
        Start Workspace Now
      </Button>
    </Box>
  );
}

WorkspaceStatusStopped.propTypes = {
  onStartWorkspace: PropTypes.func,
};
WorkspaceStatusStopped.defaultProps = {
  onStartWorkspace: () => {},
};

export default WorkspaceStatusStopped;

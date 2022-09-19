import React from 'react';

import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import PropTypes from 'prop-types';

function MlFlowServerStatusStopped({ onStartMlFlowServer }) {
  return (
    <Box
      sx={{
        maxWidth: '500px',
        textAlign: 'center',
      }}
    >
      <h1>Your MLflow Server is Stopped!</h1>
      <p>
        The MLflow server is your project&apos;s experiment tracker. It allows
        you to keep track of metrics, parameters and artifacts of different
        experiments.
      </p>
      <Button
        variant="outlined"
        size="large"
        color="secondary"
        onClick={onStartMlFlowServer}
      >
        Start MLflow Server Now
      </Button>
    </Box>
  );
}

MlFlowServerStatusStopped.propTypes = {
  onStartMlFlowServer: PropTypes.func,
};
MlFlowServerStatusStopped.defaultProps = {
  onStartMlFlowServer: () => null,
};

export default MlFlowServerStatusStopped;

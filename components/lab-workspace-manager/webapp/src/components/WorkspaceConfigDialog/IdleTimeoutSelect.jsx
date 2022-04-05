import React from 'react';

import PropTypes from 'prop-types';

import { FormControl, InputLabel, MenuItem, Select } from '@mui/material';

function formatTimeValue(seconds, defaultText) {
  if (seconds === 0) {
    return defaultText;
  }
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);

  if (h === 0 && m === 0) {
    return `${seconds} seconds`;
  }
  const hDisplay = h > 0 ? h + (h === 1 ? ' hour ' : ' hours ') : '';
  const mDisplay = m > 0 ? m + (m === 1 ? ' minute' : ' minutes') : '';
  return hDisplay + mDisplay;
}

function IdleTimeoutSelect({
  allowedIdleTimeouts,
  value,
  onChange,
  label,
  name,
}) {
  // Return select with allowed numbers
  return (
    <FormControl fullWidth>
      <InputLabel id={name}>{label}</InputLabel>
      <Select
        label={label}
        name={name}
        value={value}
        onChange={onChange}
        fullWidth
      >
        {allowedIdleTimeouts.map((idleTimeout) => (
          <MenuItem key={idleTimeout} value={idleTimeout}>
            {formatTimeValue(idleTimeout, 'Never Stop Workspace')}
          </MenuItem>
        ))}
      </Select>
    </FormControl>
  );
}

IdleTimeoutSelect.propTypes = {
  allowedIdleTimeouts: PropTypes.arrayOf(PropTypes.number).isRequired,
  value: PropTypes.number.isRequired,
  onChange: PropTypes.func.isRequired,
  label: PropTypes.string.isRequired,
  name: PropTypes.string.isRequired,
};

export default IdleTimeoutSelect;

import React from 'react';

import PropTypes from 'prop-types';

import {
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  TextField,
} from '@mui/material';

function NumberSelect({
  allowedNumbers,
  min,
  max,
  value,
  onChange,
  label,
  name,
}) {
  // If no allowed numbers are specified, allow any number in the allowed range
  if (allowedNumbers.length === 0) {
    return (
      <TextField
        label={label}
        type="number"
        name={name}
        value={value}
        onChange={(e) => {
          if (e.target.value > max) e.target.value = max;
          onChange(e);
        }}
        fullWidth
        inputProps={{ min, max, step: 1 }}
      />
    );
  }
  // Return select with allowed numbers
  return (
    <FormControl fullWidth>
      <InputLabel id={name}>{label}</InputLabel>
      <Select
        label={label}
        type="number"
        name={name}
        value={value}
        onChange={onChange}
        fullWidth
      >
        {allowedNumbers.map((number) => (
          <MenuItem key={number} value={number}>
            {number}
          </MenuItem>
        ))}
      </Select>
    </FormControl>
  );
}

NumberSelect.propTypes = {
  allowedNumbers: PropTypes.arrayOf(PropTypes.number).isRequired,
  min: PropTypes.number.isRequired,
  max: PropTypes.number.isRequired,
  value: PropTypes.number.isRequired,
  onChange: PropTypes.func.isRequired,
  label: PropTypes.string.isRequired,
  name: PropTypes.string.isRequired,
};

export default NumberSelect;

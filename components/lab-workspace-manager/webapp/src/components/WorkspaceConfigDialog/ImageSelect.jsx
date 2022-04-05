import React from 'react';

import PropTypes from 'prop-types';

import {
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  TextField,
} from '@mui/material';

function ImageSelect({ availableImages, value, onChange, label, name }) {
  // If no available images are specified, allow free text input
  if (availableImages.length === 0) {
    return (
      <TextField
        label={label}
        type="text"
        name={name}
        value={value}
        onChange={onChange}
        onBlur={() => {}} // TODO: add here the "caching" logic handling
        autoComplete="on"
        fullWidth
      />
    );
  }
  // Return select with available images
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
        {availableImages.map((image) => (
          <MenuItem key={image} value={image}>
            {image}
          </MenuItem>
        ))}
      </Select>
    </FormControl>
  );
}

ImageSelect.propTypes = {
  availableImages: PropTypes.arrayOf(PropTypes.string).isRequired,
  value: PropTypes.string.isRequired,
  onChange: PropTypes.func.isRequired,
  label: PropTypes.string.isRequired,
  name: PropTypes.string.isRequired,
};

export default ImageSelect;

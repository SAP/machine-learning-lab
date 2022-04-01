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
  let imageSelectOptions = availableImages;
  if (!availableImages.some((imageDesc) => imageDesc.image === value)) {
    // If the current image is not in the list of available images, still show it in the select
    imageSelectOptions = [
      ...imageSelectOptions,
      { image: value, display_name: value },
    ];
  }
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
        {imageSelectOptions.map((imageDesc) => (
          <MenuItem key={imageDesc.image} value={imageDesc.image}>
            {imageDesc.display_name}
          </MenuItem>
        ))}
      </Select>
    </FormControl>
  );
}

ImageSelect.propTypes = {
  availableImages: PropTypes.arrayOf(PropTypes.instanceOf(Object)).isRequired,
  value: PropTypes.string.isRequired,
  onChange: PropTypes.func.isRequired,
  label: PropTypes.string.isRequired,
  name: PropTypes.string.isRequired,
};

export default ImageSelect;

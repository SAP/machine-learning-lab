import React from 'react';

import PropTypes from 'prop-types';

import { Checkbox, FormControl, FormControlLabel } from '@mui/material';

function ClearDataCheckbox({ alwaysClearData, value, onChange, label, name }) {
  const onCheckboxChange = (e) => {
    onChange({
      target: {
        name: e.target.name,
        value: e.target.checked,
      },
    });
  };
  return (
    <FormControl fullWidth>
      <FormControlLabel
        control={
          <Checkbox
            name={name}
            checked={alwaysClearData || value}
            disabled={alwaysClearData}
            onChange={onCheckboxChange}
          />
        }
        label={label}
      />
    </FormControl>
  );
}

ClearDataCheckbox.propTypes = {
  alwaysClearData: PropTypes.bool.isRequired,
  value: PropTypes.bool.isRequired,
  onChange: PropTypes.func.isRequired,
  label: PropTypes.string.isRequired,
  name: PropTypes.string.isRequired,
};

export default ClearDataCheckbox;

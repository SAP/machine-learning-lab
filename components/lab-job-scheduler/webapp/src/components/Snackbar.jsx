import React, { useState } from 'react';

import PropTypes from 'prop-types';

import MaterialUiSnackbar from '@material-ui/core/Snackbar';

function Snackbar({ message }) {
  const [isOpen, setIsOpen] = useState(true);
  const hide = () => setIsOpen(false);

  return (
    <MaterialUiSnackbar
      message={message}
      autoHideDuration={5000}
      onClose={hide}
      open={isOpen}
      key={message}
    />
  );
}

Snackbar.propTypes = {
  message: PropTypes.string.isRequired,
};

export default Snackbar;

import React from 'react';
import ReactDOM from 'react-dom';

import Snackbar from '../components/Snackbar';

const showStandardSnackbar = (message) => {
  const domEl = document.getElementById('snackbar-container');
  if (!domEl) return null;

  ReactDOM.render(
    <div>
      <Snackbar message={message} open key={new Date()} />
    </div>,
    domEl
  );
  return true;
};

export default showStandardSnackbar;

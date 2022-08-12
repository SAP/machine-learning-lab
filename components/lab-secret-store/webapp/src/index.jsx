import React from 'react';

import ReactDOM from 'react-dom';

import { HashRouter } from 'react-router-dom';
import { ThemeProvider } from '@mui/material';

import './index.css';
import App from './pages/App';
import AppDialogServiceProvider from './app/AppDialogServiceProvider';
import theme from './utils/theme';

ReactDOM.render(
  <React.StrictMode>
    <ThemeProvider theme={theme}>
      <HashRouter>
        <AppDialogServiceProvider>
          <App />
        </AppDialogServiceProvider>
      </HashRouter>
    </ThemeProvider>
  </React.StrictMode>,
  document.getElementById('root')
);

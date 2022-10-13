import React from 'react';

import ReactDOM from 'react-dom';

import { HashRouter } from 'react-router-dom';
import { ThemeProvider as MuiThemeProvider } from '@material-ui/core/styles';
import { ThemeProvider } from 'styled-components';

import './index.css';
import App from './pages/App';
import AppDialogServiceProvider from './components/AppDialogServiceProvider';
import theme from './utils/theme';

ReactDOM.render(
  <React.StrictMode>
    <MuiThemeProvider theme={theme}>
      <ThemeProvider theme={theme}>
        <HashRouter>
          <AppDialogServiceProvider>
            <App />
          </AppDialogServiceProvider>
        </HashRouter>
      </ThemeProvider>
    </MuiThemeProvider>
  </React.StrictMode>,
  document.getElementById('root')
);

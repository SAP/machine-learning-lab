import React from 'react';

import { HashRouter } from 'react-router-dom';
import ReactDOM from 'react-dom';

import { ThemeProvider as MuiThemeProvider } from '@material-ui/core/styles';
import { ThemeProvider } from 'styled-components';

import './index.css';
import Files from './pages/Files/Files';
import theme from './utils/theme';

const folder = window.envLabFileBrowser.FOLDER || '';

ReactDOM.render(
  <React.StrictMode>
    <MuiThemeProvider theme={theme}>
      <ThemeProvider theme={theme}>
        <HashRouter>
          <Files folder={folder} />
        </HashRouter>
      </ThemeProvider>
    </MuiThemeProvider>
  </React.StrictMode>,
  document.getElementById('root')
);

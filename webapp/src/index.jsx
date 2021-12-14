import React, { Suspense } from 'react';

import { CookiesProvider } from 'react-cookie';
import { HashRouter } from 'react-router-dom';
import ReactDOM from 'react-dom';

import { ThemeProvider as MuiThemeProvider } from '@material-ui/core/styles';
import { ThemeProvider } from 'styled-components';

import './index.css';
import App from './pages/App';
import AppDialogServiceProvider from './app/AppDialogServiceProvider';
import GlobalStateContainer from './app/store';
import reportWebVitals from './utils/reportWebVitals';
import theme from './utils/theme';

// import i18n (needs to be bundled ;))
import './i18n';

ReactDOM.render(
  <React.StrictMode>
    {/* Suspense is used because otherwise i18n will throw an error. See  */}
    <Suspense fallback="">
      <MuiThemeProvider theme={theme}>
        <ThemeProvider theme={theme}>
          <CookiesProvider>
            <HashRouter>
              <GlobalStateContainer.Provider>
                <AppDialogServiceProvider>
                  <App />
                </AppDialogServiceProvider>
              </GlobalStateContainer.Provider>
            </HashRouter>
          </CookiesProvider>
        </ThemeProvider>
      </MuiThemeProvider>
    </Suspense>
  </React.StrictMode>,
  document.getElementById('root')
);

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals();

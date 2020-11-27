import React from 'react';
import ReactDOM from 'react-dom';
import * as redux from 'redux';
import { Provider } from 'react-redux';
import { HashRouter } from 'react-router-dom';
import { CookiesProvider } from 'react-cookie';

import { ThemeProvider, createMuiTheme } from '@material-ui/core/styles';

import './css/App.css';
import App from './scenes/app/App';

import * as ReduxUtils from './services/handler/reduxUtils';

import 'typeface-roboto';

import registerServiceWorker from './registerServiceWorker';

// replace with next major version of material-ui (https://material-ui.com/style/typography/):
window.__MUI_USE_NEXT_TYPOGRAPHY_VARIANTS__ = true;

function configure() {
  let store = redux.createStore(
    ReduxUtils.reduceFct,
    ReduxUtils.INITIAL_STATE,
    window.__REDUX_DEVTOOLS_EXTENSION__ && window.__REDUX_DEVTOOLS_EXTENSION__() // allows seeing the Redux Store with this extension: http://extension.remotedev.io/
  );

  registerServiceWorker();
  // if ("serviceWorker" in navigator) {
  //   navigator.serviceWorker
  //     .register("/app/service-worker.js", {
  //       scope: "/app/"
  //     })
  //     .then(function(registration) {
  //       console.log("Registration successful, scope is:", registration.scope);
  //     })
  //     .catch(function(error) {
  //       console.log("Service worker registration failed, error:", error);
  //     });
  //}
  return store;
}

const store = configure();
//Allows access to store in console.log
window.store = store;

const theme = createMuiTheme({
  overrides: {
    MuiTypography: {
      body1: {
        fontSize: '0.875rem',
      },
    },
    MuiTableRow: {
      root: {
        fontFamily: 'Roboto',
        fontSize: '0.875rem',
      },
    },
    MuiTableCell: {
      root: {
        padding: '16px 24px 16px 24px',
        fontSize: '0.75rem',
      },
    },
  },
});

ReactDOM.render(
  <Provider store={store}>
    <CookiesProvider>
      <HashRouter>
        <ThemeProvider theme={theme}>
          <App />
        </ThemeProvider>
      </HashRouter>
    </CookiesProvider>
  </Provider>,

  document.getElementById('root')
);

// export default store;

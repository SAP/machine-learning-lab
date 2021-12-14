import { HashRouter } from 'react-router-dom';
import { render } from '@testing-library/react';
import PropTypes from 'prop-types';
import React from 'react';

import { I18nextProvider } from 'react-i18next';
import i18n from 'i18next';

import { ThemeProvider as MuiThemeProvider } from '@material-ui/core/styles';
import { ThemeProvider } from 'styled-components';

import { authApi, projectsApi, usersApi } from '../services/contaxy-api';
import AppDialogServiceProvider from '../app/AppDialogServiceProvider';
import GlobalStateContainer from '../app/store';
import theme from './theme';

i18n.init({
  lng: 'en',
  debug: false,
  saveMissing: false,

  interpolation: {
    escapeValue: false, // not needed for react!!
  },

  // react i18next special options (optional)
  react: {
    useSuspense: false,
    wait: true,
    nsMode: 'fallback', // set it to fallback to let passed namespaces to translated hoc act as fallbacks
  },
});

// Mock api calls
jest.mock('../services/contaxy-api.js');
projectsApi.listProjects = async () => [
  { id: 'myFooProject', display_name: 'My Foo Project' },
];
authApi.verifyAccess = async () => true;
usersApi.getMyUser = async () => {
  return {
    username: 'Foo',
    email: 'foo@bar.com',
    disabled: false,
    id: '7uwv4ilewxiju1n0xzpt03n66',
    technical_user: false,
    created_at: '2021-04-02T09:21:33.799915+00:00',
  };
};

const Wrapper = ({ children }) => {
  return (
    <I18nextProvider i18n={i18n}>
      <MuiThemeProvider theme={theme}>
        <ThemeProvider theme={theme}>
          <HashRouter>
            <GlobalStateContainer.Provider>
              <AppDialogServiceProvider>{children}</AppDialogServiceProvider>
            </GlobalStateContainer.Provider>
          </HashRouter>
        </ThemeProvider>
      </MuiThemeProvider>
    </I18nextProvider>
  );
};

Wrapper.propTypes = {
  children: PropTypes.node.isRequired,
};

const customRender = (ui, options) => {
  return render(ui, { wrapper: Wrapper, ...options });
};

// re-export everything
export * from '@testing-library/react';
export { customRender as render };

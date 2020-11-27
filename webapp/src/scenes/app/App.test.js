import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom/extend-expect';
import 'jest-canvas-mock';
import * as redux from 'redux';
import { Provider } from 'react-redux';
import App from './App';
import * as ReduxUtils from '../../services/handler/reduxUtils';

const store = redux.createStore(ReduxUtils.reduceFct, ReduxUtils.INITIAL_STATE);

// UploadFileDialog throws an error when rendered normally, so overwrite it with an empty element for the test.
jest.mock('../../components/table/UploadFileDialog', () => ({
  UploadFileDialog: () => {
    return <div></div>;
  },
}));

// Mock React Router HOC to prevent error when test-rendering the component
jest.mock('react-router', () => ({
  withRouter: (Component) => Component,
}));

test('renders app', () => {
  // Set the getContext function to prevent "undefined" error
  window.HTMLCanvasElement.prototype.getContext = () => {};
  render(
    <Provider store={store}>
      <App />
    </Provider>
  );
  // screen.debug();
  const nameElement = screen.getByText(/machine learning lab/i);
  expect(nameElement).toBeInTheDocument();
});

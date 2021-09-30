// jest-dom adds custom jest matchers for asserting on DOM nodes.
// allows you to do things like:
// expect(element).toHaveTextContent(/react/i)
// learn more: https://github.com/testing-library/jest-dom
import '@testing-library/jest-dom';

// add jest-canvas-mock to prevent a Not implemented error (see https://github.com/hustcc/jest-canvas-mock/issues/2)
import 'jest-canvas-mock';

// Mocking authApi (needed to render App)
import { authApi } from './services/contaxy-api';

jest.mock('./services/contaxy-api');

beforeEach(() => {
  authApi.oauthEnabled.mockResolvedValue({});
});

// add mocks here that are needed for all tests as this file is automatically executed (see https://github.com/facebook/create-react-app/issues/9706)
jest.mock('jdenticon', () => ({
  update: () => 'foo',
}));

import React, { useEffect } from 'react';

import { getByRole, waitFor } from '@testing-library/react';

import { APP_NAME } from '../../utils/config';
import { fireEvent, render, screen } from '../../utils/test-custom-render';
import APP_PAGES, { APP_DRAWER_ITEM_TYPES } from '../../utils/app-pages';
import App from './App';
import GlobalStateContainer from '../../app/store';

test('tests app name', async () => {
  // waitFor is needed here so that the state changes are renderd in the App component
  await waitFor(() => {
    render(<App />);
  });
  const linkElement = screen.getByText(new RegExp(APP_NAME, 'i'));
  expect(linkElement).toBeInTheDocument();
});

test('tests the usermenu and its entries', async () => {
  await waitFor(() => {
    render(<App />);
  });
  expect(screen.queryByText(/documentation/i)).toBeNull();
  fireEvent.click(screen.getByLabelText('usermenu'));
  expect(screen.getByText(/documentation/i)).toBeInTheDocument();
  expect(screen.getByText(/Get User API Token/i)).toBeInTheDocument();
  expect(screen.getByText(/Get Project API Token/i)).toBeInTheDocument();
  expect(screen.getByText(/api_explorer/i)).toBeInTheDocument();
  expect(screen.getByText(/api_tokens/i)).toBeInTheDocument();
});

test('test that all link app drawer link items exist', async () => {
  await waitFor(() => {
    render(<App />);
  });
  APP_PAGES.filter((page) => page.type === APP_DRAWER_ITEM_TYPES.link).forEach(
    (page) => {
      expect(screen.queryAllByText(page.NAME).length > 0);
    }
  );
});

// test('add plugin dialog', async () => {
//   await waitFor(() => {
//     render(<App />);
//   });
//   const addPluginDialogButton = screen.getByText(/add plugin/i);
//   fireEvent.click(addPluginDialogButton);
//   const addPluginDialog = screen.getByRole('dialog');
//   expect(addPluginDialog).toBeInTheDocument();
//   expect(
//     addPluginDialog.getElementsByTagName('h2')[0].innerText === /add plugin/i
//   );
// });

test('tests global state', () => {
  const Component = () => {
    const { projects, setProjects } = GlobalStateContainer.useContainer();

    expect(typeof setProjects).toBe('function');

    useEffect(
      () => setProjects([{ id: 'myFooProject', name: 'My Foo Project' }]),
      [setProjects]
    );
    return (
      <div>
        {projects.map((project) => (
          <div key={project.id}>{project.id}</div>
        ))}
      </div>
    );
  };
  render(<Component />);

  expect(screen.getByText('myFooProject')).toBeInTheDocument();
});

test('tests the project selector and its entries', async () => {
  await waitFor(() => {
    render(<App />);
  });

  const projectSelector = screen.getByLabelText('projectselector');
  expect(projectSelector).toBeInTheDocument();
  expect(screen.getByLabelText('appdrawer')).toBeInTheDocument();

  // See how Material-UI tests it's Select component (https://github.com/mui-org/material-ui/blob/master/packages/material-ui/src/Select/Select.test.js)
  fireEvent.mouseDown(getByRole(projectSelector, 'button'));
  expect(screen.getByText('myFooProject')).toBeInTheDocument();
});

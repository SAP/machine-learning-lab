import React from 'react';

import { Route, Switch } from 'react-router-dom';
import Box from '@mui/material/Box';

import Workspace from './Workspace';

function App() {
  return (
    <Box sx={{ height: '100vh' }}>
      <Switch>
        <Route path="/users/:userId/workspace">
          <Workspace />
        </Route>
        <Route path="*">Not Found. Visit /users/:userId/workspace</Route>
      </Switch>
      <div id="snackbar-container" />
    </Box>
  );
}

export default App;

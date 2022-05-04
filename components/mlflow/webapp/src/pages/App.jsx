import React from 'react';

import { Route, Switch } from 'react-router-dom';

import './App.css';
import Workspace from './Workspace';

function App() {
  return (
    <div className="App">
      <Switch>
        <Route path="/users/:userId/workspace">
          <Workspace />
        </Route>
        <Route path="*">Not Found. Visit /users/:userId/workspace</Route>
      </Switch>
      <div id="snackbar-container" />
    </div>
  );
}

export default App;

import React from 'react';

import { Route, Switch } from 'react-router-dom';

import './App.css';
import WorkspaceTabs from '../WorkspaceTabs';

function App() {
  return (
    <div className="App">
      <Switch>
        <Route path="/users/:userId/workspace">
          <WorkspaceTabs />
        </Route>
        <Route path="*">Not Found</Route>
      </Switch>
      <div id="snackbar-container" />
    </div>
  );
}

export default App;

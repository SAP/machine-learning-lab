import React from 'react';

import { Route, Switch } from 'react-router-dom';

import './App.css';
import MLFlowServer from './MLFlowServer';

function App() {
  return (
    <div className="App">
      <Switch>
        <Route path="/mlflow-server">
          <MLFlowServer />
        </Route>
        <Route path="*">Not Found. Visit /mlflow-server</Route>
      </Switch>
      <div id="snackbar-container" />
    </div>
  );
}

export default App;

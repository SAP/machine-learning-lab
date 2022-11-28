import React from 'react';

import { Route, Switch } from 'react-router-dom';

import JobScheduler from '../JobScheduler';

import './App.css';

function App() {
  return (
    <div className="App">
      <Switch>
        <Route path="/job-scheduler">
          <JobScheduler />
        </Route>
        <Route path="*">Not Found. Visit /job-scheduler?project=zohair</Route>
      </Switch>
      <div id="snackbar-container" />
    </div>
  );
}

export default App;

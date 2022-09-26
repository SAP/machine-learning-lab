import React from 'react';

import { Route, Switch } from 'react-router-dom';

import './App.css';

function App() {
  return (
    <div className="App">
      <Switch>
        <Route path="/users/:userId/example">
          Add a page that shows the UI of the ML Lab component
        </Route>
        <Route path="*">Not Found. Visit /users/:userId/workspace</Route>
      </Switch>
      <div id="snackbar-container" />
    </div>
  );
}

export default App;

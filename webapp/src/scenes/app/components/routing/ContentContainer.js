import React, { Component } from "react";
import { Switch } from "react-router-dom";

import PrivateRoute from "./PrivateRoute";
import PublicRoute from "./PublicRoute";

//scenes
import Dashboard from "../../../dashboard/Dashboard";
import Workspace from "../../../Workspace";
import Datasets from "../../../Datasets";
import ExperimentsNew from "../../../experiments-dashboard/ExperimentsNew";
import Models from "../../../Models";
import Services from "../../../services/Services";
import Jobs from "../../../jobs/Jobs";
import ServiceAdmin from "../../../ServiceAdmin";
import AdminArea from "../../../AdminArea";
import Login from "../../../Login";

class ContentContainer extends Component {
  render() {
    return (
      <Switch>
        <PrivateRoute path="/" exact component={Dashboard} />
        <PrivateRoute path="/datasets" exact component={Datasets} />
        <PrivateRoute path="/workspace" exact component={Workspace} />
        <PrivateRoute path="/experiments" exact component={ExperimentsNew} />
        <PrivateRoute path="/models" exact component={Models} />
        <PrivateRoute path="/services" exact component={Services} />
        <PrivateRoute path="/jobs" exact component={Jobs} />
        <PrivateRoute path="/admin" exact component={ServiceAdmin} />
        <PrivateRoute path="/management" exact component={AdminArea} />
        <PublicRoute path="/login" exact component={Login} />
      </Switch>
    );
  }
}

export default ContentContainer;

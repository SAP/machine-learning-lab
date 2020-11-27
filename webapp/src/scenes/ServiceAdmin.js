import React, { Component } from 'react';
import { connect } from 'react-redux';

//base components
import IFrameComponent from '../components/IFrameComponent';

//controller
import * as ReduxUtils from '../services/handler/reduxUtils';
import * as Constants from '../services/handler/constants';

class ServiceAdmin extends Component {
  render() {
    return (
      <IFrameComponent
        url={Constants.SERVICES.serviceAdmin.url}
        id="service-admin-frame"
      />
    );
  }
}

export default connect(ReduxUtils.mapStateToProps)(ServiceAdmin);

import React, { Component } from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';

//base components
import BlockHeader from '../../components/BlockHeader';

//scene components
import ServiceCards from './components/ServiceCards';

//controller
import {
  projectsApi,
  getDefaultApiCallback,
  toastErrorMessage,
} from '../../services/client/ml-lab-api';
import * as ReduxUtils from '../../services/handler/reduxUtils';

class Services extends Component {
  constructor(props) {
    super(props);
    this.state = {
      serviceCardsData: [],
    };
  }

  updateData(props) {
    if (props.statusCode === 'startApp') {
      return;
    }

    projectsApi.getServices(
      props.currentProject,
      {},
      getDefaultApiCallback(
        ({ result }) => {
          this.setState({
            serviceCardsData: result.data,
          });
        },
        ({ error }) => toastErrorMessage('Load Services: ', error)
      )
    );
  }

  componentDidUpdate(prevProps) {
    if (prevProps.currentProject !== this.props.currentProject) {
      this.updateData(this.props);
    }
  }

  componentDidMount() {
    this.updateData(this.props);
  }

  onServiceUpdate = () => {
    this.updateData(this.props);
  };

  render() {
    return (
      <div style={{ width: '100%' }}>
        <BlockHeader name="Services" />
        <ServiceCards
          onServiceDeploy={this.onServiceUpdate}
          onServiceDeleted={this.onServiceUpdate}
          currentProject={this.props.currentProject}
          data={this.state.serviceCardsData}
        />
      </div>
    );
  }
}

Services.propTypes = {
  statusCode: PropTypes.string.isRequired,
  currentProject: PropTypes.string.isRequired,
};

export default connect(ReduxUtils.mapStateToProps)(Services);

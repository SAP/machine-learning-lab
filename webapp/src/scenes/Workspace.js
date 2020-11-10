import React, { Component } from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';

//base components
import IFrameComponent from '../components/IFrameComponent';

//controller
import * as ReduxUtils from '../services/handler/reduxUtils';
import * as Constants from '../services/handler/constants';
import CustomDialog from '../components/CustomDialog.js';

import {
  administrationApi,
  getDefaultApiCallback,
} from '../services/client/ml-lab-api';

class Workspace extends Component {
  constructor(props) {
    super(props);

    this.state = {
      startButtonDisabled: true,
      updateButtonDisabled: false,
      workspaceUpdateDialogOpen: false,
    };
  }

  componentDidMount() {
    administrationApi.checkWorkspace(
      { id: this.props.user },
      getDefaultApiCallback(({ httpResponse }) => {
        this.setState({
          workspaceUpdateDialogOpen: JSON.parse(httpResponse.text).metadata
            .needsUpdate,
        });
      })
    );
  }

  componentWillUnmount() {
    clearTimeout(this.startButtonEnableTimer);
  }

  renderUpdateDialog() {
    let { updateButtonDisabled } = this.state;
    const title = 'There is a newer workspace version';
    const contentText =
      'Please update the workspace to run the most stable version. Out-dated workspaces might be automatically updated after some time. All files stored under /workspace (default Jupyter path) are persisted. Data within other directories will be removed, e.g. installed libraries or machine configuration. The update should take a few seconds to a few minutes.';
    const primaryActionBtnLabel = 'Update';

    let clickUpdateButton = () => {
      this.setState({ updateButtonDisabled: true });
      administrationApi.resetWorkspace({ id: this.props.user });
      window.setTimeout(() => window.location.reload(), 7000);
    };

    return (
      <CustomDialog
        open={true}
        title={title}
        contentText={contentText}
        dialogContentTextStyle={{ fontSize: '0.9rem' }}
        primaryActionBtnLabel={primaryActionBtnLabel}
        cancelBtnLabel="Later"
        handlePrimaryAction={clickUpdateButton}
        primaryActionBtnDisabled={updateButtonDisabled}
        handleRequestClose={() =>
          this.setState({ workspaceUpdateDialogOpen: false })
        }
      />
    );
  }

  render() {
    return (
      <div>
        {this.state.workspaceUpdateDialogOpen
          ? this.renderUpdateDialog()
          : false}
        <IFrameComponent
          url={Constants.SERVICES.research.url}
          id="research-frame"
        />
      </div>
    );
  }
}

Workspace.propTypes = {
  user: PropTypes.string, // from redux
};

export default connect(ReduxUtils.mapStateToProps)(Workspace);

import React, { Component } from 'react';
import IconButton from '@material-ui/core/IconButton';
import Icon from '@material-ui/core/Icon';
import Typography from '@material-ui/core/Typography';
import Tooltip from '@material-ui/core/Tooltip';

import CustomDialog from '../../../components/CustomDialog';

const BLACKLISTED_ENV_VARIABLES = [
  'PATH',
  'LANGUAGE',
  'LANG',
  'XDG_CACHE_HOME',
  'LC_ALL',
];

class DisplayCommandButton extends Component {
  state = {
    open: false,
  };

  handleClickOpen = () => {
    this.setState({ open: true });
  };

  handleClose = () => {
    this.setState({ open: false });
  };

  render() {
    const title = 'Docker command to deploy service in your own environment';

    let {
      name,
      connectionPort,
      configuration,
      dockerImage,
    } = this.props.jsonObj;

    let modifiedConfiguration = { ...configuration }; // make a copy of the object as otherwise it changes the object passed in 'props' (on all levels)
    if (modifiedConfiguration['LAB_ENDPOINT']) {
      modifiedConfiguration['LAB_ENDPOINT'] = window.location.origin; // otherwise the endpoint is equal to the Docker DNS name
    }
    let mappedEnvVariables = (
      <div>
        {Object.keys(modifiedConfiguration).map((key, index) => {
          // filter out variables that are likely set by the image and, therefore,
          // set for each container based on that image automatically without the need to specify it in the 'docker run' command
          if (BLACKLISTED_ENV_VARIABLES.indexOf(key) > -1) {
            return <span key={index} />;
          }

          return (
            <div key={index}>
              --env {key}={modifiedConfiguration[key]} \
            </div>
          );
        })}
      </div>
    );

    let runCommand = (
      <div
        style={{
          marginTop: '8px',
          marginLeft: '20px',
          overflowY: 'scroll',
          fontFamily: 'monospace',
        }}
      >
        <div>docker run -d \</div>
        <div>--name {name} \</div>
        <div>-p {connectionPort} \</div>
        <div>
          {mappedEnvVariables}{' '}
          {
            Object.keys(configuration).length > 0
              ? ''
              : '\\' /* when no env variables exist, add a \ character to make the shell command valid */
          }
        </div>
        <div>{dockerImage}</div>
      </div>
    );

    const contentText = (
      <div>
        <div>
          This command should run the service with the same configuration as it
          is running in ML Lab. However, infrastructure-related fields such as
          'networks' or 'labels' are not included.
        </div>
        <div style={{ color: 'red' }}>
          Please be aware that the API Token is project-specific which allows
          access to <span style={{ textDecoration: 'underline' }}>all</span>{' '}
          project resources.
        </div>
      </div>
    );

    const TextView = (
      <div>
        <Typography component={'span'}>{contentText}</Typography>
        <Typography component={'span'}>{runCommand}</Typography>
      </div>
    );

    const hideCancelBtn = true;
    const primaryActionBtnDisabled = false;
    const primaryActionBtnLabel = 'Close';

    return (
      <div>
        <Tooltip title="Deploy Command" placement="bottom">
          <IconButton onClick={this.handleClickOpen}>
            <Icon>code</Icon>
          </IconButton>
        </Tooltip>

        <CustomDialog
          open={this.state.open}
          title={title}
          contentText={''}
          hideCancelBtn={hideCancelBtn}
          primaryActionBtnDisabled={primaryActionBtnDisabled}
          primaryActionBtnLabel={primaryActionBtnLabel}
          handleRequestClose={this.handleClose}
          handlePrimaryAction={this.handleClose}
          CustomComponent={TextView}
        />
      </div>
    );
  }
}

export default DisplayCommandButton;

import React, { Component } from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import { withCookies, Cookies } from 'react-cookie';

// material-ui components
import { withStyles } from '@material-ui/core/styles';
import Button from '@material-ui/core/Button';
import CloudUpload from '@material-ui/icons/CloudUpload';

// Uppy
import Uppy from '@uppy/core';
import Dashboard from '@uppy/dashboard';
import XHRUpload from '@uppy/xhr-upload';
import '@uppy/core/dist/style.min.css';
import '@uppy/dashboard/dist/style.min.css';

import { getFileUploadUrl } from '../../services/client/ml-lab-api';
import * as ReduxUtils from '../../services/handler/reduxUtils';

const uppy = new Uppy({
  id: 'uppy',
  meta: {
    onFileUpload: null,
  },
  autoProceed: false,
  debug: false,
  restrictions: {
    maxFileSize: false,
    maxNumberOfFiles: false,
    minNumberOfFiles: false,
    allowedFileTypes: false,
  },
  thumbnailGeneration: true,
})
  .use(Dashboard, {
    target: '#root',
    closeModalOnClickOutside: true,
    showProgressDetails: true,
    hideProgressAfterFinish: false,
  })
  .use(XHRUpload, {
    endpoint: '',
    method: 'post',
    formData: 'true',
    fieldName: 'file',
    timeout: 0,
    withCredentials: 'true',
  });

const styles = (theme) => ({
  button: {
    margin: theme.spacing(1),
    marginLeft: 0,
  },
  leftIcon: {
    marginRight: theme.spacing(1),
  },
  rightIcon: {
    marginLeft: theme.spacing(1),
  },
  dialogContent: {
    paddingBottom: theme.spacing(1),
  },
  droppedFiles: {
    marginTop: theme.spacing(3),
  },
});

class UploadFileDialog extends Component {
  constructor(props) {
    super(props);
    this.state = {
      open: false,
      files: [],
    };

    this.prepareUpload();
    this.reader = new FileReader();

    this.handleClickOpen = this.handleClickOpen.bind(this);
    this.onUploadComplete = this.onUploadComplete.bind(this);
  }

  prepareUpload() {
    let currentProject = this.props.currentProject;

    const uppyAwsXHR = uppy.getPlugin('XHRUpload');
    uppyAwsXHR.opts.endpoint = getFileUploadUrl(
      currentProject,
      this.props.type
    );
    uppy.opts.meta.onFileUpload = this.props.onFileUpload;
    uppy.on('complete', this.onUploadComplete);
    uppy.run();
  }

  onUploadComplete(result) {
    if (result.successful.length > 0) {
      uppy.getPlugin('Dashboard').closeModal();
      uppy.reset();
      uppy.opts.meta.onFileUpload();
    }
  }

  componentDidUpdate(prevProps) {
    if (
      prevProps.currentProject !== this.props.currentProject ||
      prevProps.type !== this.props.type ||
      prevProps.onFileUpload !== this.props.onFileUpload
    ) {
      this.prepareUpload();
    }
  }

  componentWillUnmount() {
    const dashboard = uppy.getPlugin('Dashboard');
    if (dashboard.isModalOpen()) {
      dashboard.closeModal();
    }
    uppy.reset();
    uppy.retryAll();
  }

  handleClickOpen() {
    uppy.getPlugin('Dashboard').openModal();
  }

  render() {
    return (
      <div>
        <Button
          color="primary"
          className={this.props.classes.button}
          onClick={this.handleClickOpen}
        >
          Upload
          <CloudUpload className={this.props.classes.rightIcon} />
        </Button>
      </div>
    );
  }
}

UploadFileDialog.propTypes = {
  onFileUpload: PropTypes.func.isRequired,
  cookies: PropTypes.instanceOf(Cookies).isRequired,
};

export default withCookies(
  connect(
    ReduxUtils.mapStateToProps,
    ReduxUtils.mapDispatchToProps
  )(withStyles(styles)(UploadFileDialog))
);

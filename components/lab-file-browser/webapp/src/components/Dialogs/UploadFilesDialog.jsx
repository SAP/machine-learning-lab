import React from 'react';

import PropTypes from 'prop-types';

// import Button from '@material-ui/core/Button';
// import CloudUpload from '@material-ui/icons/CloudUpload';

import '@uppy/core/dist/style.min.css';
import '@uppy/dashboard/dist/style.min.css';
import { DashboardModal } from '@uppy/react';
import Uppy from '@uppy/core';
import XHRUpload from '@uppy/xhr-upload';

import showStandardSnackbar from '../../app/showStandardSnackbar';

const filesFolder = window.envLabFileBrowser.FOLDER || '';
const uploadMessage = window.envLabFileBrowser.UPLOAD_MESSAGE || '';

const uppy = new Uppy({
  id: 'uppy',
  meta: {
    onFileUpload: null,
  },
  onBeforeFileAdded: (currentFile) => {
    // We have to add the prefix to the file name so that it still appears in the current window
    // and keeps the folder structure.
    const modifiedFile = {
      ...currentFile,
      name: `${filesFolder}/${currentFile.name}`,
    };
    return modifiedFile;
  },
}).use(XHRUpload, {
  method: 'post',
  formData: 'true',
  fieldName: 'file',
  timeout: 0,
  withCredentials: 'true',
});

uppy.on('complete', () => {
  showStandardSnackbar('Upload complete!');
});

function UploadFilesDialog(props) {
  const { endpoint, onFileUpload, open, onClose } = props;

  uppy.getPlugin('XHRUpload').opts.endpoint = endpoint;
  uppy.opts.meta.onFileUpload = onFileUpload;

  return (
    <DashboardModal
      uppy={uppy}
      open={open}
      closeModalOnClickOutside
      showProgressDetails
      hideProgressAfterFinish={false}
      onRequestClose={onClose}
      proudlyDisplayPoweredByUppy={false}
      note={uploadMessage}
    />
  );
}

UploadFilesDialog.propTypes = {
  endpoint: PropTypes.string.isRequired,
  onFileUpload: PropTypes.func,
  onClose: PropTypes.func.isRequired,
  open: PropTypes.bool.isRequired,
};

UploadFilesDialog.defaultProps = {
  onFileUpload: () => {},
};

export default UploadFilesDialog;

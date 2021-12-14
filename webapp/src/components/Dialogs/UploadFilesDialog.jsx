import React, { useMemo } from 'react';

import PropTypes from 'prop-types';

// import Button from '@material-ui/core/Button';
// import CloudUpload from '@material-ui/icons/CloudUpload';

import '@uppy/core/dist/style.min.css';
import '@uppy/dashboard/dist/style.min.css';
import { DashboardModal } from '@uppy/react';
import Uppy from '@uppy/core';
import XHRUpload from '@uppy/xhr-upload';
import showStandardSnackbar from '../../app/showStandardSnackbar';

function createUppyInstance(folder, uploadEnpoint, onFileUpload) {
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
        name: `${folder}/${currentFile.name}`,
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
  uppy.getPlugin('XHRUpload').opts.endpoint = uploadEnpoint;
  uppy.opts.meta.onFileUpload = onFileUpload;
  return uppy;
}

function UploadFilesDialog(props) {
  const { folder, uploadEnpoint, uploadNote, onFileUpload, open, onClose } =
    props;

  const uppy = useMemo(() => {
    return createUppyInstance(folder, uploadEnpoint, onFileUpload);
  }, [folder, uploadEnpoint, onFileUpload]);

  return (
    <DashboardModal
      uppy={uppy}
      open={open}
      closeModalOnClickOutside
      showProgressDetails
      hideProgressAfterFinish={false}
      onRequestClose={onClose}
      proudlyDisplayPoweredByUppy={false}
      note={uploadNote}
    />
  );
}

UploadFilesDialog.propTypes = {
  folder: PropTypes.string.isRequired,
  uploadEnpoint: PropTypes.string.isRequired,
  uploadNote: PropTypes.string,
  onFileUpload: PropTypes.func,
  onClose: PropTypes.func.isRequired,
  open: PropTypes.bool.isRequired,
};

UploadFilesDialog.defaultProps = {
  onFileUpload: () => {},
  uploadNote: '',
};

export default UploadFilesDialog;

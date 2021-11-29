import React from 'react';

import PropTypes from 'prop-types';

import Button from '@material-ui/core/Button';
import Dialog from '@material-ui/core/Dialog';
import DialogActions from '@material-ui/core/DialogActions';
import DialogContent from '@material-ui/core/DialogContent';
import DialogContentText from '@material-ui/core/DialogContentText';
import DialogTitle from '@material-ui/core/DialogTitle';

import ReactJson from 'react-json-view';

function ContentDialog(props) {
  const { content, jsonContent, onClose, title } = props;

  const contentElement = content ? (
    <DialogContentText style={{ whiteSpace: 'pre-line' }}>
      {content}
    </DialogContentText>
  ) : (
    <ReactJson src={jsonContent} />
  );

  return (
    <Dialog open>
      <DialogTitle>{title}</DialogTitle>
      <DialogContent>{contentElement}</DialogContent>
      <DialogActions>
        <Button onClick={onClose} color="primary">
          CLOSE
        </Button>
      </DialogActions>
    </Dialog>
  );
}

ContentDialog.propTypes = {
  title: PropTypes.string,
  content: PropTypes.string,
  jsonContent: PropTypes.instanceOf(Object),
  onClose: PropTypes.func.isRequired,
};

ContentDialog.defaultProps = {
  title: '',
  content: '',
  jsonContent: {},
};

export default ContentDialog;

import React from 'react';
import PropTypes from 'prop-types';
import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
} from '@mui/material';
import DialogContentText from '@mui/material/DialogContentText';
import ReactJson from 'react-json-view';

function ContentDialogForJson(props) {
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

ContentDialogForJson.propTypes = {
  title: PropTypes.string,
  content: PropTypes.string,
  jsonContent: PropTypes.instanceOf(Object),
  onClose: PropTypes.func.isRequired,
};

ContentDialogForJson.defaultProps = {
  title: '',
  content: '',
  jsonContent: {},
};

export default ContentDialogForJson;

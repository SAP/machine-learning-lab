import React, { useState } from 'react';

import PropTypes from 'prop-types';
import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { CircularProgress } from '@material-table/core/node_modules/@mui/material';

import KeyValueInputs from './KeyValueInputs';
import { creatSecret } from '../../services/secret-store-api';

function ContentDialog(props) {
  const { onClose, title, refresh, project } = props;
  const [isLoading, setIsLoading] = useState(false);
  const [input, setInput] = useState({
    secretName: '',
    metadata: {},
    secret: '',
  });
  const onChange = (e) =>
    setInput({ ...input, [e.target.name]: e.target.value });

  return (
    <Dialog open>
      <DialogTitle>{title}</DialogTitle>
      <DialogContent>
        <Stack sx={{ width: 550 }}>
          <TextField
            name="secretName"
            type="text"
            value={input.secretName}
            onChange={onChange}
            label="Secret Name"
            variant="standard"
          />
          <p>metadata</p>
          <KeyValueInputs
            name="metadata"
            onKeyValuePairChange={(keyValuePairs) => {
              setInput({
                ...input,
                metadata: keyValuePairs,
              });
            }}
          />
          <TextField
            name="secret"
            onChange={onChange}
            value={input.secret}
            label="Secret"
            variant="standard"
          />
        </Stack>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} color="primary">
          CLOSE
        </Button>
        <Button
          onClick={async () => {
            setIsLoading(true);
            const pw = await creatSecret(
              project,
              input.secretName,
              input.secret,
              input.metadata
            );
            refresh();
            onClose();
          }}
          color="primary"
        >
          ADD
        </Button>
        {isLoading && <CircularProgress size="2rem" color="secondary" />}
      </DialogActions>
    </Dialog>
  );
}

ContentDialog.propTypes = {
  title: PropTypes.string,
  onClose: PropTypes.func.isRequired,
  refresh: PropTypes.func.isRequired,
  project: PropTypes.isRequired,
};

ContentDialog.defaultProps = {
  title: '',
};

export default ContentDialog;

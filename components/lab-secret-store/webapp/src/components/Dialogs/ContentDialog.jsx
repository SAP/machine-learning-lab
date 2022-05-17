import React, { useState } from 'react';

import PropTypes from 'prop-types';
import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  TextField,
  Typography,
} from '@mui/material';
import KeyValueInputs from './KeyValueInputs';
import { creatSecret } from '../../services/secret-store-api';

function ContentDialog(props) {
  const { onClose, title } = props;

  const [input, setInput] = useState({
    secretName: '',
    methadata: {},
    secret: '',
  });
  const onChange = (e) =>
    setInput({ ...input, [e.target.name]: e.target.value });

  return (
    <Dialog open>
      <DialogTitle>{title}</DialogTitle>
      <DialogContent>test</DialogContent>

      <p>Secret Name</p>
      <TextField
        name="secretName"
        value={input.secretName}
        onChange={onChange}
        label="Secret Name"
        variant="outlined"
      />
      <p>Methadata</p>
      <KeyValueInputs
        name="methadata"
        onKeyValuePairChange={(keyValuePairs) => {
          setInput({
            ...input,
            methadata: keyValuePairs,
          });
        }}
      />
      <p>Secret</p>
      <TextField
        name="secret"
        onChange={onChange}
        value={input.secret}
        label="Secret"
        variant="outlined"
      />

      <DialogActions>
        <Button onClick={onClose} color="primary">
          CLOSE
        </Button>
        <Button
          onClick={async () => {
            const pw = await creatSecret(
              'image-search-engine',
              input.secretName,
              input.secret,
              input.methadata
            );
            onClose();
          }}
          color="primary"
        >
          ADD
        </Button>
      </DialogActions>
    </Dialog>
  );
}

ContentDialog.propTypes = {
  title: PropTypes.string,
  onClose: PropTypes.func.isRequired,
};

ContentDialog.defaultProps = {
  title: '',
};

export default ContentDialog;

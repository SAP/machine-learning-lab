import React, { useRef, useState } from 'react';

import PropTypes from 'prop-types';
import styled from 'styled-components';

import Button from '@material-ui/core/Button';
import CopyIcon from '@material-ui/icons/Assignment';
import DeleteIcon from '@material-ui/icons/Delete';
import DetailsIcon from '@material-ui/icons/Details';
import Dialog from '@material-ui/core/Dialog';
import DialogActions from '@material-ui/core/DialogActions';
import DialogContent from '@material-ui/core/DialogContent';
import DialogTitle from '@material-ui/core/DialogTitle';
import Divider from '@material-ui/core/Divider';
import MenuItem from '@material-ui/core/MenuItem';
import Popover from '@material-ui/core/Popover';
// import Tab from '@material-ui/core/Tab';
// import Tabs from '@material-ui/core/Tabs';
import Select from '@material-ui/core/Select';
import TextField from '@material-ui/core/TextField';
import Tooltip from '@material-ui/core/Tooltip';
import Typography from '@material-ui/core/Typography';

import ReactJson from 'react-json-view';

import { authApi } from '../../services/contaxy-api';
import ApiToken from '../../services/contaxy-client/model/ApiToken';
import ValueInputs from './ValueInputs';
import setClipboardText from '../../utils/clipboard';
import showStandardSnackbar from '../../app/showStandardSnackbar';

function PermissionInput({ className, index, onChange, value }) {
  const handleInputChange = (e) => {
    onChange(index, {
      input: e.target.value,
      level: value.level,
    });
  };

  const handleLevelChange = (e) => {
    onChange(index, {
      input: value.input,
      level: e.target.value,
    });
  };

  const level = value.level || 'read';

  return (
    <div className={`${className} permissionInput`}>
      <TextField
        className={`${className} inputField`}
        placeholder="Scope"
        type="text"
        value={value.input}
        onChange={handleInputChange}
        fullWidth
      />
      <Select
        labelId="demo-simple-select-placeholder-label-label"
        id="demo-simple-select-placeholder-label"
        value={level}
        onChange={handleLevelChange}
      >
        <MenuItem value="read">read</MenuItem>
        <MenuItem value="write">write</MenuItem>
        <MenuItem value="admin">admin</MenuItem>
      </Select>
    </div>
  );
}

PermissionInput.propTypes = {
  className: PropTypes.string,
  index: PropTypes.number,
  onChange: PropTypes.func.isRequired,
  value: PropTypes.shape({
    input: PropTypes.string,
    level: PropTypes.oneOf(['read', 'write', 'admin']),
  }),
};

PermissionInput.defaultProps = {
  className: '',
  index: 0,
  value: {
    input: '',
    level: 'read',
  },
};

const StyledPermissionInput = styled(PermissionInput)`
  &.inputField {
    margin-right: 12px;
  }

  &.permissionInput {
    display: flex;
    align-items: center;
    width: 100%;
  }
`;

function ApiTokenDialog({ className, creationScope, tokens, onClose }) {
  const textFieldRef = useRef();
  const [selectedPanel] = useState(0);
  const [tokenDetails, setTokenDetails] = useState({});
  const [scopes, setScopes] = useState([]);
  const [_tokens, setTokens] = useState(tokens);

  const handleCopyClick = () => {
    setClipboardText(null, textFieldRef.current);
  };
  const onGenerateToken = async () => {
    try {
      const scopeStrings = scopes
        .filter((scope) => scope.input && scope.level)
        .map((scope) => `${scope.input}#${scope.level}`);
      const token = await authApi.createToken({
        scopes: scopeStrings,
        tokenType: 'api-token',
      });
      // TODO: the returned 'token' is a string and not an AccessToken yet
      setTokens([..._tokens, { token }]);
    } catch (e) {
      showStandardSnackbar(`Could not create API token. Reason: ${e.message}`);
    }
  };

  const onShowDetails = (htmlElement, token) => {
    setTokenDetails({ element: htmlElement, token });
  };

  const onDeleteToken = async (token) => {
    try {
      await authApi.revokeToken(token.token);
      setTokens(_tokens.filter((t) => t.token !== token.token));
      showStandardSnackbar(`Revoked token '${token.token}'.`);
    } catch (e) {
      showStandardSnackbar(
        `Could not revoke token '${token.token}'. Reason: ${e.message}`
      );
    }
  };

  const popoverElement = tokenDetails.token && (
    <Popover
      open
      anchorEl={tokenDetails.element}
      onClose={() => setTokenDetails({})}
      anchorOrigin={{
        vertical: 'bottom',
        horizontal: 'left',
      }}
      transformOrigin={{
        vertical: 'top',
        horizontal: 'left',
      }}
    >
      <div className={`${className} popovercontent`}>
        <ReactJson src={tokenDetails.token} />
      </div>
    </Popover>
  );

  const tokenElements = _tokens.map((token) => {
    return (
      <div key={token.token} className={`${className} displaytoken`}>
        <Typography
          className={`${className} displaytoken-token`}
          ref={textFieldRef}
        >
          {token.token}
        </Typography>
        <Tooltip title="Copy" aria-label="copy">
          <Button
            className={`${className} displaytoken-button`}
            onClick={() => handleCopyClick()}
          >
            <CopyIcon fontSize="small" />
          </Button>
        </Tooltip>
        <Tooltip title="Details" aria-label="details">
          <Button
            className={`${className} displaytoken-button`}
            onClick={(event) => onShowDetails(event.currentTarget, token)}
          >
            <DetailsIcon fontSize="small" />
          </Button>
        </Tooltip>
        <Tooltip title="Delete" aria-label="delete">
          <Button
            className={`${className} displaytoken-button`}
            onClick={() => onDeleteToken(token)}
          >
            <DeleteIcon fontSize="small" />
          </Button>
        </Tooltip>
      </div>
    );
  });

  const displayTokensPanel = (
    <>
      <Typography variant="subtitle1">Existing Tokens</Typography>
      {tokenElements && tokenElements.length > 0
        ? tokenElements
        : 'No API tokens exist'}
    </>
  );
  const createTokenPanel = (
    <div>
      <Typography variant="subtitle1">Token Creation</Typography>
      <Typography variant="subtitle2">Scopes</Typography>
      <ValueInputs
        initialValues={[{ input: creationScope, level: 'read' }]}
        inputComponent={StyledPermissionInput}
        inputComponentProps={{ defaultValue: { input: '', level: 'read' } }}
        onValueInputsChange={setScopes}
      />
      <div className={`${className} createtoken`}>
        <Button
          color="secondary"
          aria-label="create-token"
          onClick={() => onGenerateToken()}
        >
          Create API Token
        </Button>
      </div>
    </div>
  );

  return (
    <Dialog classes={{ paper: `${className} dialog` }} open>
      <DialogTitle>API TOKENS</DialogTitle>
      <DialogContent>
        {/* <Tabs
          value={selectedPanel}
          onChange={(event, index) => setSelectedPanel(index)}
          aria-label="token tabs"
        >
          <Tab label="Display Tokens" id="token-tab-0" />
          <Tab label="Create Token" id="token-tab-1" />
        </Tabs> */}
        {selectedPanel === 0 && displayTokensPanel}
        <Divider className={`${className} divider`} />
        {selectedPanel === 0 && createTokenPanel}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} color="primary">
          OK
        </Button>
      </DialogActions>
      {popoverElement}
    </Dialog>
  );
}

ApiTokenDialog.propTypes = {
  className: PropTypes.string,
  creationScope: PropTypes.string,
  tokens: PropTypes.arrayOf(PropTypes.instanceOf(ApiToken)),
  onClose: PropTypes.func.isRequired,
};

ApiTokenDialog.defaultProps = {
  className: '',
  creationScope: '',
  tokens: [],
};

const StyledApiTokenDialog = styled(ApiTokenDialog)`
  &.dialog {
    min-width: 25%;
  }

  &.createtoken {
    display: flex;
    justify-content: flex-end;
  }

  &.displaytoken {
    display: flex;
    align-items: center;
  }

  &.displaytoken-token {
    width: 80%;
    margin-right: 8px;
  }

  &.displaytoken-button {
    min-width: initial;
  }

  &.divider {
    margin-top: 16px;
    margin-bottom: 16px;
  }

  &.popovercontent {
    margin: 8px;
  }
`;

export default StyledApiTokenDialog;

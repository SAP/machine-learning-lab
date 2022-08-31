import React, { useState } from 'react';

import PropTypes from 'prop-types';
import styled from 'styled-components';

import AddIcon from '@material-ui/icons/Add';
import Button from '@material-ui/core/Button';
import DelIcon from '@material-ui/icons/Delete';
import TextField from '@material-ui/core/TextField';

const ENV_NAME_REGEX = new RegExp('^([a-zA-Z_]{1,}[a-zA-Z0-9_]{0,})?$');

const FIELD_KEY = 'Key';
const FIELD_VALUE = 'Value';

function KeyValueInput(props) {
  const [key, setKey] = useState('');
  const [value, setValue] = useState('');

  const { className, index, onChange } = props;

  const handleKeyChange = (e) => {
    setKey(e.target.value);
    onChange(index, e.target.value, value);
  };

  const handleValueChange = (e) => {
    setValue(e.target.value);
    onChange(index, key, e.target.value);
  };

  const isInvalid = !ENV_NAME_REGEX.test(key);

  return (
    <>
      <TextField
        className={`${className} inputField`}
        autoComplete="on"
        placeholder={FIELD_KEY}
        type="text"
        value={key}
        onChange={handleKeyChange}
        error={isInvalid}
        helperText={isInvalid ? 'Key format is not valid' : null}
      />

      <TextField
        className={`${className} inputField`}
        autoComplete="on"
        placeholder={FIELD_VALUE}
        type="text"
        value={value}
        onChange={handleValueChange}
      />
    </>
  );
}

KeyValueInput.propTypes = {
  className: PropTypes.string,
  index: PropTypes.number.isRequired,
  onChange: PropTypes.func.isRequired,
};

KeyValueInput.defaultProps = {
  className: '',
};

const StyledKeyValueInput = styled(KeyValueInput)`
  &.inputField {
    margin: 10px 10px 10px 0px;
  }
`;

function KeyValueInputs(props) {
  const [keyValuePairs, setKeyValuePairs] = useState([]);
  const { onKeyValuePairChange } = props;

  const handleKeyValuePairChange = (index, key, value) => {
    const newKeyValuePairs = keyValuePairs.map((keyValuePair) => {
      if (keyValuePair.index === index) {
        return { index, key, value };
      }
      return keyValuePair;
    });

    setKeyValuePairs(() => [...newKeyValuePairs]);

    // Transform array to object. If the same key existed multiple times, only the last one of them in the array will be used.
    const keyValueInputs = Object.fromEntries(
      newKeyValuePairs.map((e) => [e.key, e.value])
    );
    onKeyValuePairChange(keyValueInputs);
  };

  const handleAddKeyValuePairClick = () => {
    setKeyValuePairs((previousKeyValuePairs) => [
      ...previousKeyValuePairs,
      { index: Date.now(), key: '', value: '' },
    ]);
  };

  const handleDeleteKeyValueClick = (index) => {
    const newKeyValuePairs = keyValuePairs.reduce((result, keyValuePair) => {
      if (keyValuePair.index !== index) {
        result.push(keyValuePair);
      }
      return result;
    }, []);

    setKeyValuePairs(() => [...newKeyValuePairs]);

    const keyValueInputs = Object.fromEntries(
      newKeyValuePairs.map((e) => [e.key, e.value])
    );
    onKeyValuePairChange(keyValueInputs);
  };

  return (
    <>
      {keyValuePairs.map((keyValuePair) => (
        <div key={keyValuePair.index}>
          <StyledKeyValueInput
            index={keyValuePair.index}
            onChange={handleKeyValuePairChange}
          />
          <Button
            // className={classes.keyValueButton}
            color="default"
            aria-label="del"
            onClick={() => handleDeleteKeyValueClick(keyValuePair.index)}
          >
            <DelIcon />
          </Button>
        </div>
      ))}

      <Button color="primary" onClick={handleAddKeyValuePairClick}>
        Add
        <AddIcon />
      </Button>
    </>
  );
}

KeyValueInputs.propTypes = {
  /* Returns an object with the key-value pairs. The same key can only exist one time. */
  onKeyValuePairChange: PropTypes.func.isRequired,
};

export default KeyValueInputs;

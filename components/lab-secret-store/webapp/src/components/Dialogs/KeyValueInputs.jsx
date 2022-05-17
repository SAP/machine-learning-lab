import React, { useRef, useState } from 'react';

import PropTypes from 'prop-types';
import { Button, TextField } from '@mui/material';

const ENV_NAME_REGEX = new RegExp('^([a-zA-Z_]{1,}[a-zA-Z0-9_]{0,})?$');

const FIELD_KEY = 'Key';
const FIELD_VALUE = 'Value';

function KeyValueInput(props) {
  const [key, setKey] = useState('');
  const [value, setValue] = useState('');

  const { className, index, onChange } = props;

  const handleKeyChange = (e) => {
    setKey(e.target.value);
    onChange(index, key, value);
  };

  const handleValueChange = (e) => {
    setValue(e.target.value);
    onChange(index, key, value);
  };

  const isInvalid = !ENV_NAME_REGEX.test(key);

  return <div>test</div>;
}

KeyValueInput.propTypes = {
  className: PropTypes.string,
  index: PropTypes.number.isRequired,
  onChange: PropTypes.func.isRequired,
};

KeyValueInput.defaultProps = {
  className: '',
};

function KeyValueInputs(props) {
  const [keyValuePairs, setKeyValuePairs] = useState([]);
  const { onKeyValuePairChange } = props;

  // Use a ref here so that the `handleKeyValuePairChange` callback
  // access the right state value and not the one it had when the
  // callback was passed to the child
  // TODO: maybe only needed when memoized functions are used?
  const stateRef = useRef();
  stateRef.current = keyValuePairs;

  const handleKeyValuePairChange = (index, key, value) => {
    const newKeyValuePairs = stateRef.current.map((keyValuePair) => {
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
    const newKeyValuePairs = stateRef.current.reduce((result, keyValuePair) => {
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
          <KeyValueInput
            index={keyValuePair.index}
            onChange={handleKeyValuePairChange}
          />
          <Button
            // className={classes.keyValueButton}
            color="default"
            aria-label="del"
            onClick={() => handleDeleteKeyValueClick(keyValuePair.index)}
          >
            del
          </Button>
        </div>
      ))}

      <Button color="primary" onClick={handleAddKeyValuePairClick}>
        Add
      </Button>
    </>
  );
}

KeyValueInputs.propTypes = {
  /* Returns an object with the key-value pairs. The same key can only exist one time. */
  onKeyValuePairChange: PropTypes.func.isRequired,
};

export default KeyValueInputs;

import React, { useEffect, useState } from 'react';

import PropTypes from 'prop-types';
import styled from 'styled-components';

import AddIcon from '@material-ui/icons/Add';
import Button from '@material-ui/core/Button';
import DelIcon from '@material-ui/icons/Delete';
import TextField from '@material-ui/core/TextField';

function ValueInput(props) {
  const { className, index, onChange, placeholder, value } = props;

  const handleKeyChange = (e) => {
    onChange(index, e.target.value);
  };

  return (
    <TextField
      className={`${className} inputField`}
      placeholder={placeholder}
      type="text"
      value={value}
      onChange={handleKeyChange}
      fullWidth
    />
  );
}

ValueInput.propTypes = {
  className: PropTypes.string,
  index: PropTypes.number.isRequired,
  onChange: PropTypes.func.isRequired,
  placeholder: PropTypes.string,
  value: PropTypes.string,
};

ValueInput.defaultProps = {
  className: '',
  placeholder: '',
  value: '',
};

const StyledValueInput = styled(ValueInput)`
  &.inputField {
    margin: 10px 10px 10px 0px;
  }
`;

function ValueInputs(props) {
  const {
    className,
    inputComponent: InputComponent,
    inputComponentProps,
    initialValues,
    onValueInputsChange,
    placeholder,
  } = props;
  const [valueInputs, setValueInputs] = useState(
    initialValues.map((initialValue) => {
      return { index: Date.now(), value: initialValue };
    })
  );

  // make sure that the parent's state is equal to the initial-values state (so run once during initialization)
  useEffect(() => onValueInputsChange(initialValues), []); // eslint-disable-line react-hooks/exhaustive-deps

  const onValueChange = (index, value) => {
    const internalValueInputs = [];
    const externalValueInputs = [];

    valueInputs.forEach((valueInput) => {
      if (valueInput.index === index) {
        internalValueInputs.push({ index, value });
        externalValueInputs.push(value);
      } else {
        internalValueInputs.push(valueInput);
        externalValueInputs.push(valueInput.value);
      }
    });

    setValueInputs(internalValueInputs);
    onValueInputsChange(externalValueInputs);
  };

  const onAddClick = () => {
    setValueInputs((previousValueInputs) => [
      ...previousValueInputs,
      { index: Date.now(), value: inputComponentProps.defaultValue },
    ]);
  };

  const onDeleteValueInput = (index) => {
    const internalValueInputs = [];
    const externalValueInputs = [];
    valueInputs.forEach((valueInput) => {
      if (valueInput.index !== index) {
        internalValueInputs.push(valueInput);
        externalValueInputs.push(valueInput.value);
      }
    });
    setValueInputs(internalValueInputs);
    onValueInputsChange(externalValueInputs);
  };

  return (
    <div>
      {valueInputs.map(({ index, value }) => (
        <div key={index} className={`${className} valueinput`}>
          <InputComponent
            index={index}
            value={value}
            onChange={onValueChange}
            placeholder={placeholder}
            {...inputComponentProps} // eslint-disable-line react/jsx-props-no-spreading
          />
          <Button
            color="default"
            aria-label="del"
            onClick={() => onDeleteValueInput(index)}
          >
            <DelIcon />
          </Button>
        </div>
      ))}
      <Button color="primary" onClick={onAddClick}>
        Add
        <AddIcon />
      </Button>
    </div>
  );
}

ValueInputs.propTypes = {
  className: PropTypes.string,
  initialValues: PropTypes.instanceOf(Array),
  inputComponent: PropTypes.elementType,
  inputComponentProps: PropTypes.shape({
    defaultValue: PropTypes.oneOfType([
      PropTypes.string,
      PropTypes.number,
      PropTypes.instanceOf(Object),
    ]),
    ...{},
  }),
  onValueInputsChange: PropTypes.func.isRequired,
  placeholder: PropTypes.string,
};

ValueInputs.defaultProps = {
  className: '',
  initialValues: [],
  inputComponent: StyledValueInput,
  inputComponentProps: {
    defaultValue: '',
  },
  placeholder: 'Value',
};

const StyledValueInputs = styled(ValueInputs)`
  &.valueinput {
    /* Aligns the inputfield and the deletion icon */
    display: flex;
  }
`;

export default StyledValueInputs;

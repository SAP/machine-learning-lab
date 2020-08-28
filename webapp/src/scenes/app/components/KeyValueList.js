import React, { Component } from "react";
import PropTypes from "prop-types";

import { withStyles } from "@material-ui/core/styles";

import TextField from "@material-ui/core/TextField";
import AddIcon from "@material-ui/icons/Add";
import DelIcon from "@material-ui/icons/Delete";
import Button from "@material-ui/core/Button";
import { ENV_NAME_REGEX } from "../../../services/handler/constants";

const styles = theme => ({
  invalidInput: {
    color: "#bd0000"
  },
  keyValueForm: {
    padding: "10px 0px",
    overflow: "auto",
    maxHeight: "300px"
  },
  keyValueText: {
    margin: "10px 10px 10px 0px"
  },
  keyValueButton: {
    minWidth: "35px",
    padding: "8px 5px"
  },
  addParameterButton: {
    //margin: theme.spacing.unit,
    marginLeft: 0,
    paddingLeft: "0px"
  },
  rightIcon: {
    marginLeft: theme.spacing(1)
  }
});

class KeyValueList extends Component {
  constructor(props) {
    super(props);

    this.state = {
      keyValuePairs: [],
      isInvalidKeys: []
    };
  }

  componentDidUpdate(prevProps) {
    // Only auto-create key value pairs if props changed and 
    // if no key value pairs are added in the dialog yet
    if (prevProps.initialKeyValuePairs !== this.props.initialKeyValuePairs && 
        this.state.keyValuePairs.length === 0
      ) {
      let keyValuePairs = [];

      // NOTE: adding the key-value pairs like this do not validate them.
      for (let i in this.props.initialKeyValuePairs) {
        let keyValuePair = this.props.initialKeyValuePairs[i];
        keyValuePairs = keyValuePairs.concat({
          index: keyValuePair.index,
          key: keyValuePair.key,
          value: keyValuePair.value
        });
      }
      this.setState({
        keyValuePairs
      });
    }
  }

  onAddKeyValueButtonClick = function() {
    let keyValuePairs = this.state.keyValuePairs;

    keyValuePairs = keyValuePairs.concat({
      index: keyValuePairs.length,
      key: "",
      value: ""
    });

    this.setState({
      keyValuePairs
    });
  }.bind(this);

  handleKeyValuePairChange = function(index, change, value) {
    const { keyValuePairs, isInvalidKeys } = this.state;

    let isInvalidKey = false;
    if (change === "key") {
      isInvalidKey = !ENV_NAME_REGEX.test(value);
      isInvalidKeys[index] = isInvalidKey;
    }
    keyValuePairs[index][change] = value;

    this.setState({
      keyValuePairs,
      isInvalidKeys
    });

    this.props.onKeyValuePairChange(keyValuePairs, isInvalidKeys);
  }.bind(this);

  onDelKeyValueButtonClick = function(index) {
    var { keyValuePairs, isInvalidKeys } = this.state;

    keyValuePairs.splice(index, 1);
    var new_index = 0;
    keyValuePairs.forEach(function(obj) {
      obj.index = new_index;
      new_index += 1;
    });

    isInvalidKeys.splice(index, 1);

    this.setState({
      keyValuePairs,
      isInvalidKeys
    });

    this.props.onKeyValuePairChange(keyValuePairs, isInvalidKeys);
  }.bind(this);

  render() {
    const { classes } = this.props;

    const keyValueText = [
      { name: "key", placeholder: "Key" },
      { name: "value", placeholder: "Value" }
    ];

    return (
      <div className={classes.keyValueForm}>
        {this.state.keyValuePairs.map(item => (
          <div key={item.index}>
            {keyValueText.map(text => (
              <TextField
                key={text.name}
                placeholder={text.placeholder}
                className={classes.keyValueText}
                type="text"
                name={text.name === "key" ? "configKey" : "configValue"}
                autoComplete="on"
                value={this.state.keyValuePairs[item.index][text.name]}
                onChange={e =>
                  this.handleKeyValuePairChange(item.index, text.name, e.target.value)
                }
                InputProps={{
                  classes: {
                    input: this.state.isInvalidKeys[item.index]
                      ? this.props.classes.invalidInput
                      : null
                  }
                }}
              />
            ))}
            <Button
              className={classes.keyValueButton}
              color="default"
              aria-label="del"
              onClick={e => this.onDelKeyValueButtonClick(item.index)}
            >
              <DelIcon />
            </Button>
          </div>
        ))}

        <Button
          color="primary"
          className={classes.addParameterButton}
          onClick={this.onAddKeyValueButtonClick}
        >
          Add Parameter
          <AddIcon className={classes.rightIcon} />
        </Button>
      </div>
    );
  }
}

KeyValueList.propTypes = {
  onKeyValuePairChange: PropTypes.func.isRequired,
  initialKeyValuePairs: PropTypes.array
};

export default withStyles(styles)(KeyValueList);

import React, { Component } from 'react';
import PropTypes from 'prop-types';

import { withStyles } from '@material-ui/core/styles';
import TextField from '@material-ui/core/TextField';
import CustomDialog from '../../../components/CustomDialog';
import KeyValueList from './KeyValueList';

const styles = (theme) => ({
  invalidInput: {
    color: '#bd0000',
  },
});

const LOCAL_STORAGE_IMAGE_NAMES = 'imageNames';
let localStoredImageNames =
  localStorage.getItem(LOCAL_STORAGE_IMAGE_NAMES) || '[]';
localStoredImageNames = JSON.parse(localStoredImageNames);

const LOCAL_STORAGE_IMAGE_CONFIGS = 'imageConfigs';
let localStoredImageConfigs =
  localStorage.getItem(LOCAL_STORAGE_IMAGE_CONFIGS) || '{}';
localStoredImageConfigs = JSON.parse(localStoredImageConfigs);

const initialState = {
  imageName: '',
  keyValuePairs: [],
  additionalDialogComponentInput: {},
  isKeyInvalid: false,
};

class SimpleDeploymentDialog extends Component {
  constructor(props) {
    super(props);

    this.state = initialState;
  }

  handleImageInputChange = function (value) {
    this.setState({
      imageName: value,
      //...storedState
    });
  }.bind(this);

  handleAdditionalDialogComponentInput = function (componentInput) {
    this.setState({
      additionalDialogComponentInput: componentInput,
    });
  }.bind(this);

  renderAdditionalDialogComponent = function (e) {
    if (this.props.additionalDialogComponent) {
      const AdditionalDialogComponent = this.props.additionalDialogComponent;
      const additionalDialogComponentProps =
        this.props.additionalDialogComponentProps || {};
      return (
        <AdditionalDialogComponent
          onAction={(componentInput) =>
            this.handleAdditionalDialogComponentInput(componentInput)
          }
          {...additionalDialogComponentProps}
          // TODO: implement passing initial state?
        />
      );
    }
    return false;
  }.bind(this);

  renderInputFields() {
    const { classes } = this.props;

    const isInvalidInput = new RegExp('[^a-zA-Z0-9-_:/.]').test(
      this.state.imageName
    );

    return (
      <div>
        <TextField
          // autoFocus // commented because if additionalDialogComponent is rendered, this is annoying
          margin="dense"
          // id={tfId}
          label="Image Name"
          type="text"
          onChange={(e) => this.handleImageInputChange(e.target.value)}
          onBlur={(e) => {
            let storedState = {};
            if (this.state.imageName in localStoredImageConfigs) {
              storedState = localStoredImageConfigs[this.state.imageName];
            }
            this.setState({ ...storedState });
          }}
          fullWidth
          name="imageName"
          autoComplete="on"
          value={this.state.imageName}
          InputProps={{
            classes: {
              input: isInvalidInput ? classes.invalidInput : null,
            },
          }}
        />
        {this.renderAdditionalDialogComponent()}
      </div>
    );
  }

  componentDidUpdate(prevProps) {
    if (prevProps.open !== this.props.open) {
      this.setState(initialState);
    }
  }

  render() {
    let {
      imageName,
      keyValuePairs,
      additionalDialogComponentInput,
      isKeyInvalid,
    } = this.state;

    const primaryActionBtnDisabled =
      imageName.length === 0 ||
      new RegExp('[^a-zA-Z0-9-_:/.]').test(this.state.imageName) ||
      (additionalDialogComponentInput.isInvalidInput === undefined
        ? false
        : additionalDialogComponentInput.isInvalidInput) ||
      isKeyInvalid;

    return (
      <div>
        <CustomDialog
          open={this.props.open}
          title={this.props.title}
          contentText={this.props.contentText}
          cancelBtnDisabled={false}
          primaryActionBtnDisabled={primaryActionBtnDisabled}
          primaryActionBtnLabel={this.props.primaryActionBtnLabel}
          handleRequestClose={this.props.handleRequestClose}
          handlePrimaryAction={() => {
            // add input to local storage so that a user can re-enter them easily
            localStorage.setItem(
              LOCAL_STORAGE_IMAGE_NAMES,
              JSON.stringify(
                Array.from(new Set([...localStoredImageNames, imageName]))
              )
            );

            localStoredImageConfigs[imageName] = {
              keyValuePairs: keyValuePairs,
              additionalDialogComponentInput: this.state
                .additionalDialogComponentInput,
            };
            localStorage.setItem(
              LOCAL_STORAGE_IMAGE_CONFIGS,
              JSON.stringify(localStoredImageConfigs)
            );

            this.props.handlePrimaryAction(
              imageName,
              keyValuePairs,
              this.state.additionalDialogComponentInput
            );
          }}
          CustomComponent={this.renderInputFields()}
        />
      </div>
    );
  }
}

SimpleDeploymentDialog.propTypes = {
  classes: PropTypes.object.isRequired,
  open: PropTypes.bool.isRequired,
  title: PropTypes.string.isRequired,
  contentText: PropTypes.string.isRequired,
  primaryActionBtnLabel: PropTypes.string.isRequired,
  /**
   * Callback function that is called when clicking on the primary button.
   * Receives two parameters: imageName: string, keyValuePairs: array of objects {key: key, value: value}
   */
  handlePrimaryAction: PropTypes.func.isRequired,
  handleRequestClose: PropTypes.func.isRequired,
  additionalDialogComponent: PropTypes.object, // a Component class reference (not instance!) to the component that should be additionally rendered in the dialog. Should accept an 'onAction' property
  additionalDialogComponentProps: PropTypes.object,
};

export default withStyles(styles)(SimpleDeploymentDialog);

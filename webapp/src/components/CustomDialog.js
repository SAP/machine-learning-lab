import React, { Component } from 'react';
import PropTypes from 'prop-types';

// material-ui components
import { withStyles } from '@material-ui/core/styles';
import Button from '@material-ui/core/Button';
import Dialog from '@material-ui/core/Dialog';
import DialogActions from '@material-ui/core/DialogActions';
import DialogContent from '@material-ui/core/DialogContent';
import DialogContentText from '@material-ui/core/DialogContentText';
import DialogTitle from '@material-ui/core/DialogTitle';

const styles = (theme) => ({
  invalidInput: {
    color: '#bd0000',
  },
});

class CustomDialog extends Component {
  constructor(props) {
    super(props);
    this.onKeyPress = this.onKeyPress.bind(this);
  }
  onKeyPress(event) {
    if (event.charCode === 13) {
      // enter key pressed
      event.preventDefault();
      //Reaction on Enter Key
      if (!this.props.primaryActionBtnDisabled) {
        this.props.handlePrimaryAction();
      }
    }
  }

  render() {
    const {
      title,
      contentText,
      cancelBtnDisabled,
      primaryActionBtnDisabled,
      primaryActionBtnLabel,
      cancelBtnLabel,
      CustomComponent,
    } = this.props;

    return (
      <Dialog
        className="customdialog"
        open={this.props.open}
        onClose={this.props.handleRequestClose}
        onKeyPress={this.onKeyPress}
      >
        <DialogTitle> {title} </DialogTitle>
        <DialogContent style={this.props.dialogContentStyle}>
          <DialogContentText style={this.props.dialogContentTextStyle}>
            {contentText}
          </DialogContentText>
          {CustomComponent ? CustomComponent : null}
        </DialogContent>

        {this.props.overwriteButton === true ? (
          ' '
        ) : (
          <DialogActions>
            {!this.props.moreButtons
              ? false
              : this.props.moreButtons
                  .filter((button) => button !== false)
                  .map((Button, index) => {
                    return <span key={index}>{Button}</span>;
                  })}
            {this.props.hideCancelBtn ? (
              false
            ) : (
              <Button
                onClick={this.props.handleRequestClose}
                color="primary"
                disabled={cancelBtnDisabled}
              >
                {cancelBtnLabel || 'Cancel'}
              </Button>
            )}
            <Button
              onClick={this.props.handlePrimaryAction}
              disabled={primaryActionBtnDisabled}
              color="primary"
            >
              {primaryActionBtnLabel}
            </Button>
          </DialogActions>
        )}
      </Dialog>
    );
  }
}

CustomDialog.propTypes = {
  classes: PropTypes.object.isRequired,
  open: PropTypes.bool.isRequired,
  title: PropTypes.string.isRequired,
  contentText: PropTypes.string.isRequired,
  cancelBtnDisabled: PropTypes.bool,
  overwriteButton: PropTypes.bool,
  hideCancelBtn: PropTypes.bool,
  primaryActionBtnDisabled: PropTypes.bool.isRequired,
  primaryActionBtnLabel: PropTypes.string.isRequired,
  cancelBtnLabel: PropTypes.string,
  handleRequestClose: PropTypes.func.isRequired,
  handlePrimaryAction: PropTypes.func.isRequired,
  moreButtons: PropTypes.array,
  dialogContentStyle: PropTypes.object,
  dialogContentTextStyle: PropTypes.object,
};

export default withStyles(styles)(CustomDialog);

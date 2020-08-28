import React, { Component } from "react";
import PropTypes from "prop-types";

// material-ui
import Icon from "@material-ui/core/Icon";
import IconButton from "@material-ui/core/IconButton";
import { withStyles } from "@material-ui/core/styles";
import CreateButton from "@material-ui/icons/Add";
import Button from "@material-ui/core/Button";


const styles = theme => ({
  button: {
    //margin: theme.spacing.unit,
    marginLeft: 0,
    padding: "6px 16px"
  },
  rightIcon: {
    marginLeft: theme.spacing(1)
  }
});

class CreateProjectButton extends Component {
  render() {
    const { classes } = this.props;

    //TODO: refactor: only Dashboard is used anymore
    if (this.props.component === "Dashboard") {
      return (
        <Button
          color="primary"
          className={classes.button}
          onClick={() => this.props.handleClickOpen(true)}
        >
          Add Project
          <CreateButton className={classes.rightIcon} />
        </Button>
      );
    } else {
      return (
        <IconButton
          color="inherit"
          onClick={this.props.handleClickOpen}
          aria-label="Create Project"
        >
          <Icon>add_circle</Icon>
        </IconButton>
      );
    }
  }
}

CreateProjectButton.propTypes = {
  handleClickOpen: PropTypes.func.isRequired,
  component: PropTypes.string.isRequired
};

export default withStyles(styles)(CreateProjectButton);

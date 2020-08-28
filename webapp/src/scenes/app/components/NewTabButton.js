import React from "react";
import { withStyles } from "@material-ui/core/styles";
import PropTypes from "prop-types";
import IconButton from "@material-ui/core/IconButton";
import Icon from "@material-ui/core/Icon";
import Tooltip from "@material-ui/core/Tooltip";
import { connect } from "react-redux";

//controller
import * as ReduxUtils from "../../../services/handler/reduxUtils";

const styles = theme => ({
  tooltip: {
    width: "35px",
    fontSize: "0.525rem"
  },
  newTabIcon: {
    fontSize: "18px"
  }
});

class NewTabButton extends React.Component {
  render() {
    const { classes, serviceName } = this.props;
    return (
      <Tooltip
        title={<div className={classes.tooltip}>New Tab</div>}
        placement="bottom"
      >
        <IconButton
          aria-label="New Tab"
          href={serviceName}
          target="_blank"
          rel="noopener"
        >
          <Icon className={classes.newTabIcon}>open_in_new</Icon>
        </IconButton>
      </Tooltip>
    );
  }
}

NewTabButton.propTypes = {
  classes: PropTypes.object.isRequired,
  serviceName: PropTypes.string.isRequired,
  isServiceProjectSpecific: PropTypes.bool.isRequired
};

export default connect(ReduxUtils.mapStateToProps)(
  withStyles(styles)(NewTabButton)
);

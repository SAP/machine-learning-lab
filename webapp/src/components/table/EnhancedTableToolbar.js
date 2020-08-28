import React from "react";
import PropTypes from "prop-types";

// material-ui components
import { withStyles } from "@material-ui/core/styles";
import Tooltip from "@material-ui/core/Tooltip";
import Toolbar from "@material-ui/core/Toolbar";
import Typography from "@material-ui/core/Typography";
import IconButton from "@material-ui/core/IconButton";
import FilterListIcon from "@material-ui/icons/FilterList";
import LoopIcon from "@material-ui/icons/Loop";

const toolbarStyles = theme => ({
  root: {
    paddingRight: 2
  },
  highlight:
    theme.palette.type === "light"
      ? {
          color: theme.palette.secondary.A700,
          backgroundColor: theme.palette.secondary.A100
        }
      : {
          color: theme.palette.secondary.A100,
          backgroundColor: theme.palette.secondary.A700
        },
  spacer: {
    flex: "1 1 100%"
  },
  actions: {
    color: theme.palette.text.secondary
  },
  title: {
    flex: "0 0 auto"
  }
});

class EnhancedTableToolbar extends React.Component {
  render() {
    const { classes, title } = this.props;
    const onFilterClick = event => {
      this.props.toggleFilter(event);
    };
    return (
      <Toolbar className={classes.root}>
        <div className={classes.title}>
          <Typography variant="h6">{title}</Typography>
        </div>
        <div className={classes.spacer} />
        <div className={classes.actions}>
          <Tooltip title="Reload">
            <IconButton aria-label="Reload" onClick={this.props.onReload}>
              <LoopIcon />
            </IconButton>
          </Tooltip>
        </div>
        <div className={classes.actions}>
          <Tooltip title="Filter list">
            <IconButton aria-label="Filter list" onClick={onFilterClick}>
              <FilterListIcon />
            </IconButton>
          </Tooltip>
        </div>
      </Toolbar>
    );
  }
}

EnhancedTableToolbar.propTypes = {
  classes: PropTypes.object.isRequired,
  title: PropTypes.string.isRequired,
  toggleFilter: PropTypes.func.isRequired,
  onReload: PropTypes.func.isRequired
};

export default withStyles(toolbarStyles)(EnhancedTableToolbar);

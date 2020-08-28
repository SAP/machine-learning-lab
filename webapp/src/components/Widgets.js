import React, { Component } from "react";
import { withStyles } from "@material-ui/core/styles";
import classNames from "classnames";
import PropTypes from "prop-types";
import Grid from "@material-ui/core/Grid";
import Icon from "@material-ui/core/Icon";
import Typography from "@material-ui/core/Typography";
import { CircularProgress } from "@material-ui/core";
import { Link } from "react-router-dom";

import "../css/widget.css";

const styles = theme => ({
  iconStyles: {
    fontSize: 50,
    position: "relative",
    top: "16px",
    color: "white"
  },
  text: {
    marginTop: 8,
    color: "white"
  },
  value: {
    color: "white"
    // marginTop: 2 - not needed?
  },
  progress: {
    color: "white"
  },
  link: {
    textDecoration: "none"
  },
  pointerCursor: {
    cursor: "pointer"
  }
});

class Widget extends Component {
  formatClassNameInfoBox(color) {
    return "info-box hover-expand-effect bg-" + color;
  }

  render() {
    var { classes } = this.props;
    var displayedValue = this.props.value;
    if (displayedValue === "init") {
      displayedValue = (
        <CircularProgress className={classes.progress} size={30} />
      );
    }

    var classNameInfoBox = this.props.path
      ? classNames(
          this.formatClassNameInfoBox(this.props.color),
          classes.pointerCursor
        )
      : this.formatClassNameInfoBox(this.props.color);

    var widgetContent = (
      <div className={classNameInfoBox}>
        <div className="icon">
          <Icon className={classes.iconStyles}>{this.props.icon}</Icon>
        </div>
        <div className="content">
          <Typography className={classes.text} variant="body1">
            {this.props.name}
          </Typography>
          <Typography className={classes.value} variant="h6">
            {displayedValue}
          </Typography>
        </div>
      </div>
    );

    return (
      <Grid item xs={12} sm={6} md={this.props.colSize} lg={this.props.colSize}>
        {this.props.path ? (
          <Link to={this.props.path} className={classes.link}>
            {widgetContent}
          </Link>
        ) : (
          widgetContent
        )}
      </Grid>
    );
  }
}

class Widgets extends Component {
  render() {
    const data = this.props.data;
    const colSize = 12 / this.props.data.length <= 3 ? 3 : 4;

    const oWidgets = data.map(item => (
      <Widget
        classes={this.props.classes}
        key={item.NAME}
        colSize={colSize}
        color={item.COLOR}
        icon={item.ICON}
        name={item.NAME}
        value={item.VALUE}
        path={item.PATH}
      />
    ));
    return (
      <Grid container spacing={3}>
        {oWidgets}
      </Grid>
    );
  }
}

Widgets.propTypes = {
  classes: PropTypes.object.isRequired,
  data: PropTypes.array.isRequired
};

export default withStyles(styles)(Widgets);

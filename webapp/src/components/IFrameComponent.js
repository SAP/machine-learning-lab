import React, { Component } from "react";
import Iframe from "react-iframe";
import { CircularProgress } from "@material-ui/core";
import { withStyles } from "@material-ui/core/styles";

const styles = theme => ({
  iFrame: {
    height: "100%", 
    width: "100%",
    marginTop: "-12px",
    marginLeft: "-12px",
    position: "absolute",
    border: 0
  }
});

class IFrameComponent extends Component {
  constructor(props) {
    super(props);
    this.state = {
      isLoading: true
    };
  }

  componentDidMount() {
    document.getElementById(this.props.id).addEventListener(
      "load",
      function() {
        this.setState({
          isLoading: false
        });
      }.bind(this)
    );
  }
  render() {
    const styleHideFrame = {
      visibility: "hidden"
    };
    const { classes } = this.props;

    return (
      <div style={{ width: "100%" }}>
        {this.state.isLoading ? (
          <div style={{ textAlign: "center" }}>
            <CircularProgress />
          </div>
        ) : null}

        <Iframe
          url={this.props.url}
          id={this.props.id}
          className={classes.iFrame}
          style={this.state.isLoading ? styleHideFrame : null}
          display="initial"
          allowFullScreen
        />
      </div>
    );
  }
}

export default withStyles(styles)(IFrameComponent);

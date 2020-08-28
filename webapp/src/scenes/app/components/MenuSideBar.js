import React from "react";
import { withStyles } from "@material-ui/core/styles";
import PropTypes from "prop-types";
import { withRouter } from "react-router";
import { Link } from "react-router-dom";

// material-ui components
import ListItem from "@material-ui/core/ListItem";
import ListItemIcon from "@material-ui/core/ListItemIcon";
import ListItemText from "@material-ui/core/ListItemText";
import ListItemSecondaryAction from "@material-ui/core/ListItemSecondaryAction";
import Icon from "@material-ui/core/Icon";
import Typography from "@material-ui/core/Typography";
import Divider from '@material-ui/core/Divider';

// scene components
import NewTabButton from "./NewTabButton";

const styles = theme => ({
  iconActive: {
    color: "#3f51b5"
  },
  typographyActive: {
    color: "#3f51b5",
    fontWeight: "500"
  },
  typographyInactive: {
    color: "rgba(0, 0, 0, 0.80)"
  },
  iconButton: {
    height: "20px",
    position: "relative",
    float: "right",
    top: "-34px",
    right: "5%"
  }
});

class MenuSideBar extends React.Component {
  render() {
    var isActive =
      this.props.location.pathname === this.props.item.PATH;

    let item = this.props.item;

    const primTypography = (
      <Typography
        className={
          isActive
            ? this.props.classes.typographyActive
            : this.props.classes.typographyInactive
        }
      >
        {item.NAME}
      </Typography>
    );
    const newTabButton = item.NEW_TAB_OPTION ? (
      <NewTabButton
        serviceName={item.NEW_TAB_LINK.url}
        isServiceProjectSpecific={item.PROJECT_SPECIFIC}
      />
    ) : null;

    /** itemProps is a JSON defining what should be rendered in the Navbar on the left side
     * the JSON must contain a 'component' field refering to the component that is rendered in the Navbar. For example, the {@link Link} element is rendered below the
     * ListItem's <span> element and, when clicking on the span element, the action of the Link element is executed.
     */
    let itemProps = {};
    let element = null;
    if (item.TYPE === "link") {
      itemProps = { component: Link, to: item.PATH };
      element = (
        <ListItem {...itemProps} className="navBarItem" button>   {/* className={classNames(this.props.classes.navBarItem)} > */}
          <ListItemIcon
            className={isActive ? this.props.classes.iconActive : null}
          >
            <Icon>{item.ICON}</Icon>
          </ListItemIcon>
          <ListItemText disableTypography primary={primTypography} />
          <ListItemSecondaryAction>{newTabButton}</ListItemSecondaryAction>
        </ListItem>
      );
    } else if (item.TYPE === "divider") {
      element = <Divider component="li" />;
    }

    return (
      <div>
        {element}
      </div>
    );
  }
}

MenuSideBar.propTypes = {
  classes: PropTypes.object.isRequired,
  item: PropTypes.object.isRequired
};

export default withRouter(withStyles(styles)(MenuSideBar));

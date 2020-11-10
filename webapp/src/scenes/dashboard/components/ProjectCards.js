import React, { Component } from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';

import * as Jdenticon from 'jdenticon';
import classNames from 'classnames';

// material-ui components
import { withStyles } from '@material-ui/core/styles';
import Typography from '@material-ui/core/Typography';
import Grid from '@material-ui/core/Grid';

import Card from '@material-ui/core/Card';
import CardActions from '@material-ui/core/CardActions';
import CardHeader from '@material-ui/core/CardHeader';
import Button from '@material-ui/core/Button';
import Avatar from '@material-ui/core/Avatar';

//controller
import * as ReduxUtils from '../../../services/handler/reduxUtils';
import * as Parser from '../../../services//handler/parser';

const styles = (theme) => ({
  gridItem: {
    minWidth: 350,
  },
  card: {
    display: 'flex',
    height: '100%',
  },
  content: {
    flex: '1 0 auto',
  },
  details: {
    display: 'flex',
    flexDirection: 'column',
    // overflow: "scroll"
  },
  pos: {
    marginBottom: 12,
    color: theme.palette.text.secondary,
  },
  projectImage: {
    width: 80,
    height: 80,
    marginTop: theme.spacing(2),
    marginRight: theme.spacing(2),
  },
  controls: {
    display: 'flex',
    alignItems: 'center',
    paddingLeft: theme.spacing(1),
    paddingBottom: theme.spacing(1),
    // overflow: "scroll"
  },
  description: {
    flex: '1 0 auto',
    fontSize: 13,
    fontWeight: 'initial',
    paddingLeft: 16,
    paddingRight: 16,
    color: 'rgba(0, 0, 0, 0.70)',
  },
  CreateProjectDialog: {
    marginTop: '20px',
    display: 'inline-block',
  },
  Avatar: {
    width: 60,
    height: 60,
    border: 'white 1px solid',
  },
  AvatarActive: {
    borderColor: 'green',
  },
});

class ProjectCard extends Component {
  constructor(props) {
    super(props);
    this.urlCreator = window.URL || window.webkitURL;
    this.svgRef = React.createRef();
  }

  componentDidMount() {
    Jdenticon.update(this.svgRef.current);
  }

  render() {
    const { classes } = this.props;

    var isActive = this.props.currentProject === this.props.name ? true : false;
    const description = this.props.description ? this.props.description : '';

    return (
      <Grid
        item
        xs={12}
        sm={12}
        md={6}
        lg={this.props.colSize}
        className={classes.gridItem}
      >
        <Card className={classes.card}>
          <div className={classes.details}>
            <CardHeader
              style={{ overflow: 'auto', fontSize: '1.0rem' }}
              component="div"
              avatar={
                <Avatar
                  className={classNames({
                    [classes.Avatar]: true,
                    [classes.AvatarActive]: isActive ? true : false,
                  })}
                  alt={this.props.name}
                  // src={imageUrl}
                  children={
                    <svg
                      ref={this.svgRef}
                      width="60"
                      height="60"
                      style={{ backgroundColor: 'white' }}
                      data-jdenticon-value={this.props.name}
                    />
                  }
                />
              }
              title={this.props.name}
              subheader={this.props.createdAt}
            />
            <Typography className={classes.description} variant="body1">
              {description}
            </Typography>
            <CardActions className={classes.controls}>
              <Button
                size="small"
                target="_blank"
                onClick={() =>
                  this.props.onSelectProject(
                    this.props.name,
                    this.props.item.id
                  )
                }
              >
                SELECT
              </Button>
              <Button
                size="small"
                target="_blank"
                //href={this.props.portainerLink}
                onClick={() => this.props.onManageProject(this.props.name)}
              >
                MEMBERS
              </Button>
              <Button
                size="small"
                target="_blank"
                onClick={() =>
                  this.props.onGetApiToken(this.props.name, this.props.item.id)
                }
              >
                TOKEN
              </Button>
              <Button
                size="small"
                target="_blank"
                onClick={() => this.props.onDeleteProject(this.props.name)}
              >
                DELETE
              </Button>
            </CardActions>
          </div>
        </Card>
      </Grid>
    );
  }
}

ProjectCard.propTypes = {
  classes: PropTypes.object.isRequired,
  colSize: PropTypes.number.isRequired,
  currentProject: PropTypes.string.isRequired,
  name: PropTypes.string.isRequired,
  createdAt: PropTypes.string.isRequired,
  onDeleteProject: PropTypes.func.isRequired,
  onSelectProject: PropTypes.func.isRequired,
  onManageProject: PropTypes.func.isRequired,
  onGetApiToken: PropTypes.func.isRequired,
};

class ProjectCards extends Component {
  constructor(props) {
    super(props);
    this.state = {
      renderedItems: 0,
      sortedData:
        props.data !== null
          ? props.data
              .sort((a, b) => (a.name > b.name ? 1 : b.name > a.name ? -1 : 0))
              .map((item) => item)
          : [],
      projectCards: [],
    };

    this.renderItemsChunkSize = 20;
  }

  componentDidUpdate(prevProps) {
    if (
      (this.props.data !== null &&
        prevProps.data !== null &&
        prevProps.data.length !== this.props.data.length) ||
      this.props.currentProject !== prevProps.currentProject
    ) {
      const sortedData = this.props.data.sort((a, b) =>
        a.name > b.name ? 1 : b.name > a.name ? -1 : 0
      );
      this.setState({
        sortedData: sortedData,
        projectCards: [],
        renderedItems: 0,
      });
    } else if (this.state.renderedItems < this.state.sortedData.length) {
      // load & render the project cards in chunks to not block the web page when a lot of projects exist

      // add next project cards to be rendered before the next animation frame. This way, the loading of many projects does not block the UI.
      this.increaseRenderedItemsCountTimer = window.requestAnimationFrame(
        () => {
          const projectCards = this.getProjectCards();
          this.setState({
            projectCards: [...this.state.projectCards, projectCards],
            renderedItems: this.state.renderedItems + this.renderItemsChunkSize,
          });
        }
      );
    }
  }

  getProjectCards() {
    return this.state.sortedData
      .slice(
        this.state.renderedItems,
        this.state.renderedItems + this.renderItemsChunkSize
      )
      .map((item, index) => {
        return (
          <ProjectCard
            ref={!this.projectCardSampleRef ? this.projectCardSampleRef : null}
            colSize={3}
            currentProject={this.props.currentProject}
            classes={this.props.classes}
            name={item.name}
            description={item.description}
            createdAt={Parser.SetVariableFormat(item.createdAt, 'date')}
            key={item.name}
            portainerLink={item.adminLink}
            onInpChange={this.props.onInpChange}
            onDeleteProject={this.props.onDeleteProject}
            item={item}
            onSelectProject={this.props.onSelectProject}
            onManageProject={this.props.onManageProject}
            onGetApiToken={this.props.onGetApiToken}
          />
        );
      });
  }

  componentWillUnmount() {
    window.cancelAnimationFrame(this.increaseRenderedItemsCountTimer);
  }

  render() {
    //Card Information
    return (
      <div>
        <Grid container spacing={3}>
          {this.state.projectCards}
        </Grid>
      </div>
    );
  }
}

ProjectCards.propTypes = {
  classes: PropTypes.object.isRequired,
  currentProject: PropTypes.string.isRequired,
  data: PropTypes.array.isRequired,
  onDeleteProject: PropTypes.func.isRequired,
  onInpChange: PropTypes.func.isRequired,
  onSelectProject: PropTypes.func.isRequired,
  onManageProject: PropTypes.func.isRequired,
  onGetApiToken: PropTypes.func.isRequired,
};

export default connect(
  ReduxUtils.mapStateToProps,
  ReduxUtils.mapDispatchToProps
)(withStyles(styles)(ProjectCards));

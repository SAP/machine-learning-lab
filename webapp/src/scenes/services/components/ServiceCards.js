import React, { Component } from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';

// material-ui components
import { withStyles } from '@material-ui/core/styles';
import Grid from '@material-ui/core/Grid';
import Button from '@material-ui/core/Button';
import Typography from '@material-ui/core/Typography';
import Card from '@material-ui/core/Card';
import CardActions from '@material-ui/core/CardActions';
import CardContent from '@material-ui/core/CardContent';
import CardMedia from '@material-ui/core/CardMedia';

//scene components
import DeployServiceControl from './DeployServiceControl';
import DisplayJsonButton from './DisplayJsonButton';
import AccessButton from './AccessButton';
import LogsButton from '../../../components/table/ActionButtons/LogsButton';
import DisplayCommandButton from './DisplayCommandButton';

//controller
import * as Parser from '../../../services/handler/parser';
import * as ReduxUtils from '../../../services/handler/reduxUtils';
import DeleteServiceButton from './DeleteServiceButton';

const styles = (theme) => ({
  card: {
    minWidth: 0,
    display: 'flex',
    height: '100%',
  },
  content: {
    flex: '1 0 auto',
  },
  details: {
    display: 'flex',
    flexDirection: 'column',
    width: 'calc(100% - 100px)',
  },
  pos: {
    marginBottom: 12,
    color: theme.palette.text.secondary,
  },
  serviceImage: {
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
  },
  DeployServiceDialog: {
    marginTop: '20px',
    display: 'inline-block',
  },
});

class ServiceCard extends Component {
  getIconFromName(type) {
    return './service_default.png';
  }

  getServiceName(currentProject, serviceName) {
    var regex = new RegExp(currentProject + '-', 'g');
    var newServiceName = serviceName.replace(regex, '');
    var serviceNameTruncated = Parser.truncate(newServiceName, 50);

    return serviceNameTruncated;
  }

  render() {
    const { classes, isAdmin } = this.props;

    return (
      <Grid item xs={12} sm={12} md={6} lg={this.props.colSize}>
        <Card className={classes.card}>
          <div className={classes.details}>
            <CardContent className={classes.content}>
              <Typography
                style={{ overflow: 'auto', fontSize: '1.0rem' }}
                variant="h5"
                component="h2"
              >
                {this.getServiceName(
                  this.props.currentProject,
                  this.props.name
                )}
              </Typography>

              <Typography variant="subtitle2" className={classes.pos}>
                {Parser.SetVariableFormat(this.props.modifiedAt, 'date')}
              </Typography>
            </CardContent>
            <CardActions className={classes.controls}>
              <AccessButton
                projectId={this.props.currentProjectId}
                serviceName={this.props.dockerName}
                exposedPorts={this.props.exposedPorts}
              />
              {isAdmin ? (
                <Button
                  size="small"
                  target="_blank"
                  href={this.props.portainerLink}
                >
                  MANAGE
                </Button>
              ) : (
                false
              )}
              <DisplayCommandButton jsonObj={{ ...this.props.item }} />
              <DisplayJsonButton
                jsonObj={{ ...this.props.item }}
                projName={this.props.name}
              />
              <LogsButton
                project={this.props.currentProject}
                id={this.props.item.dockerId}
                type="service"
              />
              <DeleteServiceButton
                project={this.props.currentProject}
                serviceName={this.props.dockerName}
                onServiceDeleted={this.props.onServiceDeleted}
              />
            </CardActions>
          </div>
          <CardMedia
            className={classes.serviceImage}
            image={this.getIconFromName(this.props.name)}
          />
        </Card>
      </Grid>
    );
  }
}

class ServiceCards extends Component {
  render() {
    //const data = this.props.data;

    const { onServiceDeploy, data, isAdmin } = this.props;
    const colSize = 12 / data.length <= 3 ? 3 : 4;

    const oServiceCards = data
      .sort(
        (a, b) =>
          a.modifiedAt > b.modifiedAt ? -1 : b.modifiedAt > a.modifiedAt ? 1 : 0 // show newest services first
      )
      .map((item) => (
        <ServiceCard
          colSize={colSize}
          currentProject={this.props.currentProject}
          currentProjectId={this.props.currentProjectId}
          onServiceDeleted={this.props.onServiceDeleted}
          classes={this.props.classes}
          key={item.name}
          name={item.name}
          modifiedAt={item.modifiedAt}
          id={item.id}
          endpoint={item.host}
          accessLink={
            '/projects/' +
            this.props.currentProject +
            '/services/' +
            item.dockerName +
            '/8091/webui'
          }
          exposedPorts={item.exposedPorts}
          portainerLink={item.adminLink}
          dockerName={item.dockerName}
          item={item}
          isAdmin={isAdmin}
        />
      ));
    return (
      <div>
        <Grid container spacing={3}>
          {oServiceCards}
        </Grid>
        <Card className={this.props.classes.DeployServiceDialog}>
          <DeployServiceControl onServiceDeploy={onServiceDeploy} />
        </Card>
      </div>
    );
  }
}

ServiceCards.propTypes = {
  classes: PropTypes.object.isRequired,
  currentProject: PropTypes.string.isRequired, // from redux
  currentProjectId: PropTypes.string.isRequired, // from redux
  data: PropTypes.array.isRequired,
  onServiceDeploy: PropTypes.func.isRequired,
  onServiceDeleted: PropTypes.func.isRequired,
};

export default connect(ReduxUtils.mapStateToProps)(
  withStyles(styles)(ServiceCards)
);

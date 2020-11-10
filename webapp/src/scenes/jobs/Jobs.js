import React, { Component } from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import Grid from '@material-ui/core/Grid';
import { toast } from 'react-toastify';

//base components
import Widgets from '../../components/Widgets';
import BlockHeader from '../../components/BlockHeader';
import TableComponent from '../../components/table/TableComponent';
import * as ProcessToast from '../../components/ProcessToast';

import LogsButton from '../../components/table/ActionButtons/LogsButton';
import JobInfoButton from '../../components/table/ActionButtons/JobInfoButton';
import ManageJobButton from '../../components/table/ActionButtons/ManageJobButton';
import DeleteItemButton from '../../components/table/ActionButtons/DeleteItemButton';

//scene components
import AddJobControl from './components/AddJobControl';

//controller
import * as Constants from '../../services/handler/constants';
import {
  projectsApi,
  getDefaultApiCallback,
  toastErrorType,
  toastErrorMessage,
} from '../../services/client/ml-lab-api';
import * as Parser from '../../services/handler/parser';
import * as ReduxUtils from '../../services/handler/reduxUtils';

class Jobs extends Component {
  constructor(props) {
    super(props);
    this.state = {
      widgetdata: Constants.WIDGET_ITEMS_JOBS,
      tabledata: [],
      scheduledJobsTableData: [],
    };

    this.reloadData = this.reloadData.bind(this);

    this.reloadTimer = null;
  }

  updateData(props) {
    if (props.statusCode === 'startApp' || props.statusCode === 'noProjects') {
      return;
    }

    projectsApi.getJobs(
      props.currentProject,
      {},
      getDefaultApiCallback(
        ({ result, httpResponse }) => {
          if (!result) {
            return;
          }

          let stats = httpResponse.body.metadata.stats;
          let widgetdata = this.state.widgetdata;
          widgetdata.forEach(function (element) {
            element.VALUE = Parser.SetVariableFormat(
              stats[element.KEY],
              element.FORMAT
            );
          });
          this.setState({
            widgetdata: widgetdata,
            tabledata: result.data,
          });
        },
        ({ httpResponse }) => {
          var data = [];
          var widgetdata = Constants.WIDGET_ITEMS_JOBS;

          widgetdata.forEach(function (element) {
            element.VALUE = '';
          }, this);

          this.setState({
            tabledata: data,
            widgetdata: widgetdata,
          });

          const errorObj = httpResponse.body.errors;
          const errorMessage = errorObj.message + ' (' + errorObj.type + ')';
          toastErrorType(errorMessage);
        }
      )
    );

    projectsApi.getScheduledJobs(
      props.currentProject,
      {},
      getDefaultApiCallback(({ result }) => {
        let jobsData = result.data.map((job) => {
          // the property "name" is needed in nested components, e.g. the delete button
          job.name = job.jobName;
          return job;
        });
        this.setState({ scheduledJobsTableData: jobsData });
      })
    );
  }

  deleteJob = function (job) {
    const toastID = ProcessToast.showProcessToast('Job will be deleted...');
    projectsApi.deleteJob(
      this.props.currentProject,
      job['dockerId'],
      {},
      getDefaultApiCallback(
        () => {
          toast.dismiss(toastID);
          toast.success('Job deleted');
          this.reloadData();
        },
        ({ error }) => {
          toast.dismiss(toastID);
          toastErrorMessage('Delete Job: ', error);
        }
      )
    );
  }.bind(this);

  deleteScheduledJob = function (job) {
    const toastID = ProcessToast.showProcessToast(
      'Scheduled job will be deleted...'
    );
    projectsApi.deleteScheduledJob(
      this.props.currentProject,
      job['id'],
      getDefaultApiCallback(
        () => {
          toast.dismiss(toastID);
          toast.success('Scheduled job deleted');
          this.reloadData();
        },
        ({ error }) => {
          toast.dismiss(toastID);
          toastErrorMessage('Delete Job: ', error);
        }
      )
    );
  }.bind(this);

  reloadData() {
    this.updateData(this.props);
  }

  componentDidUpdate(prevProps) {
    if (prevProps.currentProject !== this.props.currentProject) {
      this.updateData(this.props);
    }
  }

  componentDidMount() {
    this.updateData(this.props);
    this.reloadTimer = setTimeout(this.reloadData, 30 * 1000); // reload Jobs data every x seconds
  }

  componentWillUnmount() {
    if (this.reloadTimer) {
      clearTimeout(this.reloadTimer);
      this.reloadTimer = null;
    }
  }

  render() {
    let actionButtons = [
      (item) => (
        <LogsButton
          id={item.dockerId}
          project={this.props.currentProject}
          type="job"
        />
      ),
      (item) => <JobInfoButton jsonObj={item} />,
      (item) => <ManageJobButton url={item.adminLink} />,
      (item) => <DeleteItemButton item={item} onItemDelete={this.deleteJob} />,
    ];

    const jobActionBtn = (
      <AddJobControl onReload={this.reloadData} type="job" />
    );
    const scheduledJobActionBtn = (
      <AddJobControl onReload={this.reloadData} type="scheduledJob" />
    );
    return (
      <div style={{ width: '100%' }}>
        <BlockHeader name="Jobs" />
        <Widgets data={this.state.widgetdata} />
        <Grid item xs={12} lg={12}>
          <TableComponent
            orderBy={Constants.JOB_TABLE_COLUMNS[2].id} //sort by startedAt
            actionButtons={actionButtons}
            title="Executed Jobs"
            data={this.state.tabledata}
            columns={Constants.JOB_TABLE_COLUMNS}
            onReload={this.reloadData}
            primaryActionBtn={jobActionBtn}
            enableCellClick={false}
            onDownload={function () {}}
          />
        </Grid>

        <Grid item xs={12} lg={12}>
          <TableComponent
            orderBy="_id"
            actionButtons={[
              'info',
              { key: 'deleteBtn', callback: this.deleteScheduledJob },
            ]}
            title="Scheduled Jobs"
            data={this.state.scheduledJobsTableData}
            columns={Constants.JOB_SCHEDULED_TABLE_COLUMNS}
            onReload={this.reloadData}
            enableCellClick={false}
            primaryActionBtn={scheduledJobActionBtn}
          />
        </Grid>
      </div>
    );
  }
}

Jobs.propTypes = {
  statusCode: PropTypes.string.isRequired,
  currentProject: PropTypes.string.isRequired,
};

export default connect(ReduxUtils.mapStateToProps)(Jobs);

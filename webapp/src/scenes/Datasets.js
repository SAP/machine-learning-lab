import React, { Component } from 'react';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';
import Grid from '@material-ui/core/Grid';

//base components
import Widgets from '../components/Widgets';
import BlockHeader from '../components/BlockHeader';
import TableComponent from '../components/table/TableComponent';
import UploadFileDialog from '../components/table/UploadFileDialog';

import DownloadItemButton from '../components/table/ActionButtons/DownloadItemButton';
import CopyKeyButton from '../components/table/ActionButtons/CopyKeyButton';
import DeleteFileButton from '../components/table/ActionButtons/DeleteFileButton';

//controller
import * as Constants from '../services/handler/constants';
import {
  projectsApi,
  getDefaultApiCallback,
  toastErrorMessage,
  getFileDownloadUrl,
} from '../services/client/ml-lab-api';
import * as Parser from '../services/handler/parser';
import * as ReduxUtils from '../services/handler/reduxUtils';

class Datasets extends Component {
  constructor(props) {
    super(props);
    this.state = {
      widgetdata: Constants.WIDGET_ITEMS_DATASETS,
      tabledata: [],
    };

    this.reloadData = this.reloadData.bind(this);
    //this.onDownload = this.onDownload.bind(this);
  }

  updateData(props) {
    if (props.statusCode === 'startApp' || props.statusCode === 'noProjects') {
      return;
    }

    projectsApi.getFiles(
      props.currentProject,
      {
        dataType: 'dataset',
      },
      getDefaultApiCallback(
        ({ result }) => {
          let stats = result.metadata.stats;
          let widgetdata = this.state.widgetdata;
          widgetdata.forEach(function (element) {
            element.VALUE = Parser.SetVariableFormat(
              stats[element.KEY],
              element.FORMAT
            );
          }, this);
          let data = result.data;
          data.forEach(function (element) {
            if (!element.modifiedBy) {
              element.modifiedBy = '-';
            }
            element.downloadUrl = getFileDownloadUrl(
              props.currentProject,
              element.key
            );
          });
          this.setState({
            widgetdata: widgetdata,
            tabledata: data,
          });
        },
        ({ error }) => {
          var data = [];
          var widgetdata = Constants.WIDGET_ITEMS_DATASETS;

          widgetdata.forEach(function (element) {
            element.VALUE = '';
          }, this);

          this.setState({
            tabledata: data,
            widgetdata: widgetdata,
          });

          toastErrorMessage('Load Datasets: ', error);
        }
      )
    );
  }

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
  }

  render() {
    let actionButtons = [
      (item) => <DownloadItemButton downloadUrl={item.downloadUrl} />,
      (item) => <CopyKeyButton copyKey={item.key} />,
      (item) => <DeleteFileButton onItemDelete={this.reloadData} item={item} />,
    ];

    const primaryActionBtn = (
      <UploadFileDialog onFileUpload={this.reloadData} type="dataset" />
    );

    return (
      <div style={{ width: '100%' }}>
        <BlockHeader name="Datasets" />
        <Widgets data={this.state.widgetdata} />
        <Grid item xs={12} lg={12}>
          <TableComponent
            orderBy={Constants.DATASET_TABLE_COLUMNS[1].id}
            actionButtons={actionButtons}
            title="Datasets"
            data={this.state.tabledata}
            columns={Constants.DATASET_TABLE_COLUMNS}
            onReload={this.reloadData}
            primaryActionBtn={primaryActionBtn}
            enableCellClick={true}
          />
        </Grid>
      </div>
    );
  }
}

Datasets.propTypes = {
  statusCode: PropTypes.string.isRequired,
  currentProject: PropTypes.string.isRequired,
};

export default connect(ReduxUtils.mapStateToProps)(Datasets);

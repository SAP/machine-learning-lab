import React, { Component } from "react";
import { connect } from "react-redux";
import PropTypes from "prop-types";
import Grid from "@material-ui/core/Grid";

//base components
import Widgets from "../components/Widgets";
import BlockHeader from "../components/BlockHeader";
import TableComponent from "../components/table/TableComponent";
import UploadFileDialog from "../components/table/UploadFileDialog";

import DownloadItemButton from "../components/table/ActionButtons/DownloadItemButton";
import CopyKeyButton from "../components/table/ActionButtons/CopyKeyButton";
import DeleteFileButton from "../components/table/ActionButtons/DeleteFileButton";
import DeployItemButton from "../components/table/ActionButtons/DeployItemButton";

//controller
import * as Constants from "../services/handler/constants";
import {
  projectsApi,
  getDefaultApiCallback,
  getFileDownloadUrl,
  toastErrorMessage
} from "../services/client/ml-lab-api";
import * as Parser from "../services/handler/parser";
import * as ReduxUtils from "../services/handler/reduxUtils";

class Models extends Component {
  constructor(props) {
    super(props);
    this.state = {
      widgetdata: Constants.WIDGET_ITEMS_MODELS,
      tabledata: []
    };

    this.reloadData = this.reloadData.bind(this);
    //this.onDownload = this.onDownload.bind(this);
  }

  updateData(props) {
    if (props.statusCode === "startApp" || props.statusCode === "noProjects") {
      return;
    }

    projectsApi.getFiles(props.currentProject, {
      dataType: "model"
    }, getDefaultApiCallback(
      ({ result }) => {
        let stats = result.metadata.stats;
        let widgetdata = this.state.widgetdata;
        widgetdata.forEach(function(element) {
          element.VALUE = Parser.SetVariableFormat(
            stats[element.KEY],
            element.FORMAT
          );
        }, this);
        var data = result.data;
        data.forEach(function(element) {
          if (!element.modifiedBy) {
            element.modifiedBy = "-";
          }
          element.downloadUrl = getFileDownloadUrl(
            props.currentProject,
            element.key
          );
        });
        this.setState({
          widgetdata: widgetdata,
          tabledata: data
        });
      },
      ({ error }) => {
        var data = [];
        var widgetdata = Constants.WIDGET_ITEMS_DATASETS;

        widgetdata.forEach(function(element) {
          element.VALUE = "";
        }, this);

        this.setState({
          tabledata: data,
          widgetdata: widgetdata
        });

        toastErrorMessage("Load Models: ", error);
      }
    ));
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
      (item) => <DeployItemButton item={item} />, // hide one-click deployment until it is more mature
      (item) => <DeleteFileButton onItemDelete={this.reloadData} item={item} />
    ]

    const primaryActionBtn = (
      <UploadFileDialog onFileUpload={this.reloadData} type="model" />
    );

    return (
      <div style={{ width: "100%" }}>
        <BlockHeader name="Models" />
        <Widgets data={this.state.widgetdata} />
        <Grid item xs={12} lg={12}>
          <TableComponent
            orderBy={Constants.MODEL_TABLE_COLUMNS[4].id}
            actionButtons={actionButtons}
            title="Models"
            data={this.state.tabledata}
            columns={Constants.MODEL_TABLE_COLUMNS}
            onReload={this.reloadData}
            primaryActionBtn={primaryActionBtn}
            enableCellClick={true}
          />
        </Grid>
      </div>
    );
  }
}

Models.propTypes = {
  statusCode: PropTypes.string.isRequired,
  currentProject: PropTypes.string.isRequired
};

export default connect(ReduxUtils.mapStateToProps)(Models);

import React from 'react';
import { connect } from 'react-redux';

import PropTypes from 'prop-types';

import Tabs from '@material-ui/core/Tabs';
import Tab from '@material-ui/core/Tab';
import Typography from '@material-ui/core/Typography';
import Paper from '@material-ui/core/Paper';
import { withStyles } from '@material-ui/core/styles';

import MaterialTable from 'material-table';

import * as Parser from '../../services/handler/parser';
import CopyKeyButton from '../../components/table/ActionButtons/CopyKeyButton';
import DownloadItemButton from '../../components/table/ActionButtons/DownloadItemButton';
import * as ReduxUtils from '../../services/handler/reduxUtils';
import {
  getFileDownloadUrl,
  toastInfo,
} from '../../services/client/ml-lab-api';
import * as ColumnInfo from './column-information';
import * as ColumnFormatter from './grid-formatter';

class RowDetailPanel extends React.PureComponent {
  constructor(props) {
    super(props);
    this.state = {
      activeIndex: 0,
    };
  }

  handleChange = (_, activeIndex) => this.setState({ activeIndex });
  render() {
    const { activeIndex } = this.state;

    return (
      <div
        style={{
          display: 'flex',
          flexGrow: 1,
          marging: '0px',
          padding: '0px !important',
        }}
      >
        <VerticalTabs value={activeIndex} onChange={this.handleChange}>
          <MyTab label="overview" />
          <MyTab label="metrics" />
          <MyTab label="dependencies" />
          <MyTab label="artifacts" />
          <MyTab label="host" />
          <MyTab label="git" />
          <MyTab label="parameters" />
          <MyTab label="files" />
        </VerticalTabs>

        {/* {activeIndex === 0 && <TabContainer><p style={{float:'right'}}>Gist of the entire experiment.</p></TabContainer>} */}
        {activeIndex === 0 && (
          <TabContainer>
            <OverviewTable
              experimentsAttributes={this.props.experiments}
              data={this.props.experiment}
            />
          </TabContainer>
        )}
        {activeIndex === 1 && (
          <TabContainer>
            <MetricsTable data={this.props.experiment} />
          </TabContainer>
        )}
        {activeIndex === 2 && (
          <TabContainer>
            <DependenciesTable data={this.props.experiment} />
          </TabContainer>
        )}
        {activeIndex === 3 && (
          <TabContainer>
            <ResourcesTable data={this.props.experiment} />
          </TabContainer>
        )}
        {activeIndex === 4 && (
          <TabContainer>
            <HostTable data={this.props.experiment} />
          </TabContainer>
        )}
        {activeIndex === 5 && (
          <TabContainer>
            <GitTable data={this.props.experiment} />
          </TabContainer>
        )}
        {activeIndex === 6 && (
          <TabContainer>
            <ParametersTable data={this.props.experiment} />
          </TabContainer>
        )}
        {activeIndex === 7 && (
          <TabContainer>
            <FilesTable
              currentProject={this.props.currentProject}
              data={this.props.experiment}
            />
          </TabContainer>
        )}
      </div>
    );
  }
}

const VerticalTabs = withStyles(() => ({
  flexContainer: {
    marginLeft: '10px',
    marginTop: '10px',
    alignItems: 'center',
    flexDirection: 'column',
  },
  indicator: {
    display: 'none',
  },
}))(Tabs);

const MyTab = withStyles(() => ({
  root: {
    width: '175px',
    paddingLeft: '0px',
    textAlign: 'left',
    float: 'left',
    minHeight: '45px',
    color: '#646464',
    // backgroundColor: '#E8E8E8'
  },
  selected: {
    padding: '0px',
    color: '#f50057',
    paddingLeft: '20px',
    borderBottom: '2px solid #f50057',
    // backgroundColor: '#FCFCFC',
    textAlign: 'left',
  },
  wrapper: {
    alignItems: 'flex-start',
    padding: '6px 24px',
  },
}))(Tab);

function TabContainer(props) {
  return (
    <Typography
      component="div"
      style={{ flex: 1, padding: 30, float: 'right', justifyContent: 'right' }}
    >
      {props.children}
    </Typography>
  );
}

const OverviewTable = (params) => {
  //TODO: Currently I'm getting the wished columns from column-information.js. This should be change to experimentsAttributes, once the backend delivers the
  //results that way. ATM the overview section lies flat within the structure and we have to collect, store them in an overview array and then access it again
  let data = params.data;
  let rows = [];
  let overview = ColumnInfo.COLUMNS.OVERVIEW;

  overview.forEach((key) => {
    let val = data[key] || ' ';
    if (ColumnInfo.COLUMNS.DATE.includes(key)) {
      val = ColumnFormatter.secondsToDateAndTime(val);
    } else if (ColumnInfo.COLUMNS.DURATION.includes(key)) {
      val = ColumnFormatter.DurationFormatter({ value: val });
    }

    key = key[0].toUpperCase() + key.substr(1, key.length);
    rows.push({ overview: key, value: val });
  });

  let cols = [ColumnInfo.NAMETITLE.OVERVIEW, ColumnInfo.NAMETITLE.VALUE];

  return (
    <div style={{ maxWidth: '1000px', marginLeft: '200px' }}>
      <Paper>
        <MaterialTable
          columns={cols}
          data={rows}
          title=""
          options={{
            sorting: false,
            paging: false,
            search: false,
            toolbar: false,
            headerStyle: {
              // color: "rgba(0, 0, 0, 0.54)",
              fontSize: '0.75rem',
              fontWeight: 500,
            },
          }}
        />
      </Paper>
    </div>
  );
};

const MetricsTable = ({ data }) => {
  let metrics = data.metrics || {};
  let rows = [];
  Object.entries(metrics).forEach((element) => {
    if (typeof element !== 'undefined') {
      let key = element[0];
      key = key[0].toUpperCase() + key.substr(1, key.length);

      let val = element[1];
      rows.push({ metric: key, value: val });
    }
  });

  let cols = [ColumnInfo.NAMETITLE.METRIC, ColumnInfo.NAMETITLE.VALUE];

  return StandardTable(rows, cols, '900px');
};

const DependenciesTable = ({ data }) => {
  let dependencies = data.dependencies || [];
  let rows = [];
  dependencies.forEach((element) => {
    rows.push({ name: element });
  });

  let cols = [{ name: 'name', title: 'Name' }];
  cols[0].field = 'name';
  return StandardTable(rows, cols, '900px');
};

const ResourcesTable = ({ data }) => {
  let resources = data.resources || {};
  let rowsArtifacts = [];

  for (const [key, val] of Object.entries(resources)) {
    // console.log(key);

    if (key === 'artifacts') {
      val.forEach((artifactItem) => {
        rowsArtifacts.push({ library: artifactItem });
      });
    }
  }

  let cols = [ColumnInfo.NAMETITLE.LIBRARY];

  return StandardTable(rowsArtifacts, cols, '1200px');
};

const HostTable = ({ data }) => {
  let host = data.host || {};
  let rows = [];
  Object.entries(host).forEach((spec) => {
    let key = spec[0];
    let val = spec[1];

    if (typeof val === 'object') {
      val = JSON.stringify(val);
    }

    rows.push({ spec: key, value: val });
  });
  let cols = [ColumnInfo.NAMETITLE.SPEC, ColumnInfo.NAMETITLE.VALUE];

  return StandardTable(rows, cols, '900px');
};

const GitTable = ({ data }) => {
  let git = data.git || {};
  let rows = [];
  Object.entries(git).forEach((info) => {
    let key = info[0];
    let val = info[1];

    if (typeof val === 'object') {
      val = JSON.stringify(val);
    }

    rows.push({ info: key, value: val });
  });
  let cols = [ColumnInfo.NAMETITLE.INFO, ColumnInfo.NAMETITLE.VALUE];

  return StandardTable(rows, cols, '900px');
};

const ParametersTable = ({ data }) => {
  let parameters = data.parameters || {};
  let rows = [];
  Object.entries(parameters).forEach((parameter) => {
    let key = parameter[0];
    let val = parameter[1];

    if (typeof val === 'object') {
      val = JSON.stringify(val);
    }

    rows.push({ parameters: key, value: val });
  });

  let cols = [ColumnInfo.NAMETITLE.PARAMETERS, ColumnInfo.NAMETITLE.VALUE];

  return StandardTable(rows, cols, '900px');
};

const FilesTable = (props) => {
  let { data } = props;

  let resources = data.resources || {};
  let filesList = ColumnInfo.COLUMNS.OUTPUTFILES;
  console.log(resources);
  let rows = [];
  filesList.forEach((type) => {
    if (resources[type] !== undefined) {
      if (Array.isArray(resources[type])) {
        resources[type].forEach((element) => {
          rows.push({
            type: type,
            value: element,
            downloadUrl: getFileDownloadUrl(props.currentProject, element),
          });
        });
      } else {
        rows.push({
          type: type,
          value: resources[type],
          downloadUrl: getFileDownloadUrl(
            props.currentProject,
            resources[type]
          ),
        });
      }
    }
  });
  let cols = [ColumnInfo.NAMETITLE.TYPE, ColumnInfo.NAMETITLE.VALUE];

  // Since we use our own action button components in the table, we have to set our own Action component for MaterialTable
  // See https://github.com/mbrn/material-table/blob/master/src/components/m-table-action.js and https://stackoverflow.com/questions/55846015/how-to-remove-background-ripple-effect-from-custom-action-button-in-material-tab
  const ActionComponent = (props) => {
    const action = props.action.action(props.data);
    const IconComponent = action.icon(action.iconProps);
    return <div {...props}>{IconComponent}</div>;
  };

  return (
    <div style={{ maxWidth: '1000px', marginLeft: '200px' }}>
      <Paper>
        <MaterialTable
          columns={cols}
          data={rows}
          title=""
          options={materialTableOptions}
          components={{
            Action: (props) => <ActionComponent {...props} />,
          }}
          actions={[
            (rowData) => ({
              icon: (props) => (
                <CopyKeyButton
                  key="copyBtn"
                  copyKey={rowData.value}
                  onCopy={onCellClickCopyKey}
                />
              ),
            }),
            (rowData) => ({
              icon: (props) => (
                <DownloadItemButton
                  key="downloadBtn"
                  downloadUrl={rowData.downloadUrl}
                />
              ),
            }),
          ]}
        />
      </Paper>
    </div>
  );
};

const StandardTable = (rows, cols, width) => {
  return (
    <div style={{ maxWidth: width, marginLeft: '200px' }}>
      <Paper>
        <MaterialTable
          columns={cols}
          data={rows}
          title=""
          options={materialTableOptions}
        />
      </Paper>
    </div>
  );
};

const materialTableOptions = {
  sorting: false,
  paging: false,
  search: false,
  toolbar: false,
  actionsColumnIndex: -1,
  headerStyle: {
    // color: "rgba(0, 0, 0, 0.54)",
    fontSize: '0.75rem',
    fontWeight: 500,
  },
};

const onCellClickCopyKey = (itemKey) => {
  // console.log('onCellClick');
  Parser.setClipboardText(itemKey);
  toastInfo('Copied to Clipboard');
};

// TODO: experiment proptype is missing
RowDetailPanel.propTypes = {
  currentProject: PropTypes.string.isRequired,
  experiments: PropTypes.object.isRequired,
};

export default connect(ReduxUtils.mapStateToProps)(RowDetailPanel);

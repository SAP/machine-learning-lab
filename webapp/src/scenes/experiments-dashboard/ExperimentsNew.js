import React from 'react';

import PropTypes from 'prop-types';

import { connect } from 'react-redux';

import { toast } from 'react-toastify';

import MaterialTable from 'material-table';
import Paper from '@material-ui/core/Paper';
import { withStyles } from '@material-ui/core/styles';
import Tooltip from '@material-ui/core/Tooltip';
import IconButton from '@material-ui/core/IconButton';
import Icon from '@material-ui/core/Icon';

// base components
import Widgets from '../../components/Widgets';
import BlockHeader from '../../components/BlockHeader';

// controller
import * as ReduxUtils from '../../services/handler/reduxUtils';
import * as Constants from '../../services/handler/constants';

// table helper
import StatusButton, * as TableComponents from './TableComponents';
import * as GridApiHelper from './grid-api-helper';
// import * as ColumnInfo from "./experiments-dashboard/column-information";
import NewAttributeDialog from './NewAttributeDialog';

import {
  makeAllUpperCaseAndAddWhitespace,
  DurationFormatter,
  secondsToDateAndTime,
  NumberFormatter,
} from './grid-formatter';

import DeleteExperimentButton from '../../components/table/ActionButtons/DeleteExperimentButton';
import * as ProcessToast from '../../components/ProcessToast';

const PAGE_SIZES = [5, 10, 30, 60, 90, 120];

const OVERVIEW_KEY = 'overview';

/**
 * When the Status column is grouped, show the status text instead of the button
 * @param {*} rowdata In 'row' mode it contains the whole row, in 'group' mode it just contains the value of the field
 * @param {*} renderType
 */
const renderStatusCell = (rowdata, renderType) => {
  if (renderType === 'row') {
    return <StatusButton value={rowdata.status} />;
  } else if (renderType === 'group') {
    return rowdata;
  }
};
/**
 * The value contains fields needed by MaterialTable.
 * Special columns are always displayed before other selected columns if activated.
 *
 */
let specialColumns = {
  status: {
    field: 'status',
    title: makeAllUpperCaseAndAddWhitespace('Status'),
    cellStyle: { minWidth: 160 },
    render: renderStatusCell,
  },
  name: {
    field: 'name',
    title: makeAllUpperCaseAndAddWhitespace('Name'),
    cellStyle: { minWidth: 300 },
  },
  key: {
    field: 'key',
    title: makeAllUpperCaseAndAddWhitespace('Key'),
    hidden: false,
    cellStyle: { minWidth: 300 },
  },
  project: {
    field: 'project',
    title: makeAllUpperCaseAndAddWhitespace('Project'),
    hidden: false,
    cellStyle: { width: 160 },
  },
  operator: {
    field: 'operator',
    title: makeAllUpperCaseAndAddWhitespace('Operator'),
    cellStyle: { minWidth: 160 },
  },
  scriptName: {
    field: 'scriptName',
    title: makeAllUpperCaseAndAddWhitespace('Script Name'),
    hidden: false,
    cellStyle: { minWidth: 160 },
  },
  scriptType: {
    field: 'scriptType',
    title: makeAllUpperCaseAndAddWhitespace('Script Type'),
    hidden: false,
    cellStyle: { minWidth: 160 },
  },
  startedAt: {
    field: 'startedAt',
    title: makeAllUpperCaseAndAddWhitespace('startedAt'),
    type: 'date',
    render: (rowData) => secondsToDateAndTime(rowData.startedAt),
    defaultSort: 'desc',
    cellStyle: { minWidth: 160 },
  },
  finishedAt: {
    field: 'finishedAt',
    title: makeAllUpperCaseAndAddWhitespace('finishedAt'),
    type: 'date',
    render: (rowData) => secondsToDateAndTime(rowData.finishedAt),
    cellStyle: { minWidth: 160 },
  },
  updatedAt: {
    field: 'updatedAt',
    title: makeAllUpperCaseAndAddWhitespace('updatedAt'),
    type: 'date',
    hidden: false,
    render: (rowData) => secondsToDateAndTime(rowData.updatedAt),
    cellStyle: { minWidth: 160 },
  },
  duration: {
    field: 'duration',
    title: makeAllUpperCaseAndAddWhitespace('duration'),
    render: (rowData) => DurationFormatter({ value: rowData.duration }),
    cellStyle: { minWidth: 160 },
  },
  command: {
    field: 'command',
    title: makeAllUpperCaseAndAddWhitespace('Command'),
    hidden: false,
    cellStyle: { minWidth: 160 },
  },
  clientVersion: {
    field: 'clientVersion',
    title: makeAllUpperCaseAndAddWhitespace('clientVersion'),
    hidden: false,
    cellStyle: { minWidth: 160 },
  },
  result: {
    field: 'result',
    title: makeAllUpperCaseAndAddWhitespace('Result'),
    render: (rowdata) => NumberFormatter({ value: rowdata.result }),
    cellStyle: { minWidth: 160 },
  },
};
let initiallyVisibleColumns = [
  'status',
  'name',
  'operator',
  'startedAt',
  'finishedAt',
  'updatedAt',
  'duration',
  'result',
];

/**
 * Make fields sortable even when not each row has an entry for that field
 *
 * @param {*} field
 * @param {*} parentField
 */
const customSorting = (field, parentField) => {
  return (a, b) => {
    const aField = parentField ? a[parentField][field] : a[field];
    const bField = parentField ? b[parentField][field] : b[field];

    if (aField !== undefined && bField === undefined) return 1;
    if (aField === undefined && bField !== undefined) return -1;
    return 0;
  };
};

/**
 * Make the nested fields searchable
 * @param {*} field
 * @param {*} parentField
 */
const customFilterAndSearch = (field, parentField) => {
  return (term, rowdata) => {
    const fieldValue = parentField
      ? rowdata[parentField][field]
      : rowdata[field];
    if (!fieldValue) return false;
    return fieldValue.toLowerCase().indexOf(term.toLowerCase()) > -1;
  };
};

const getDefaultMaterialTableColumn = (field, parentField) => {
  let column = {
    field: field,
    title: makeAllUpperCaseAndAddWhitespace(field),
    cellStyle: { minWidth: 160 },
    customSort: customSorting(field, parentField),
    customFilterAndSearch: customFilterAndSearch(field, parentField),
  };

  if (parentField) {
    column.render = (rowdata) => rowdata[parentField][field];
  }

  return column;
};

class ExperimentsNew extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      // currentProject: "",
      widgetdata: Constants.WIDGET_ITEMS_EXPERIMENTS,
      visibleColumns: initiallyVisibleColumns.map((key) => {
        return { ...specialColumns[key] };
      }), // make a copy of the column specifications because material-table modifies the objects directly
      columnsConfiguration: {},
      jsonExperiments: { data: [] },
    };

    if (this.props.currentProject) {
      this.loadExperiments();
    }
  }

  componentDidUpdate(prevProps, prevState) {
    if (
      this.props.currentProject !== prevProps.currentProject ||
      this.state.jsonExperiments.data.length !==
        prevState.jsonExperiments.data.length
    ) {
      this.loadExperiments();
      this.getAttributeColumns();
    }
  }

  /**
   * Loads the experiment data and the widget data
   */
  loadExperiments = () => {
    GridApiHelper.getModifiedExperiments(
      this.props.currentProject,
      this.state.widgetdata
    ).then(
      (resultDict) => {
        this.setState({
          widgetdata: resultDict.widgetdata,
          jsonExperiments: resultDict.json,
        });
      },
      (error) => {
        console.log(
          'componentDidUpdate:callback of set state of processExperiments failed',
          error
        );
      }
    );
  };

  /**
   * This function goes through the experiments data and extracts the tracked fields.
   * It returns an object containing the attributes in hierarchical order (e.g. {category1: {subCategory: {}, category2: ... }})
   * and with the information whether the attribte is visible or not.
   * For flat attributes, those without no parent category, the parent category is set to `OVERVIEW_KEY`
   */
  getAttributeColumns = () => {
    let attributeColumns = {
      [OVERVIEW_KEY]: {}, //[]
    };
    let addedAttributes = [];

    // let hideInitially = ColumnInfo.COLUMNS.INITIALLYHIDDEN;

    // go through all experiments and extract the
    // attribute names (= key of the experiment fields).
    // All first level experiment keys belong to the 'overview' category
    // Nested keys will belong to the parent category.
    // Eg. {"result": 0, "host": {"os": "Linux", "cpu": 4}}
    // => attributeColumns: {"overview": ["result"], "host": ["os", "cpu"]}
    for (let i in this.state.jsonExperiments.data) {
      let dataEntry = this.state.jsonExperiments.data[i];

      for (const [attributeName, value] of Object.entries(dataEntry)) {
        // 'tableData' is coming from Material Table and can be ignored
        if (value === undefined || attributeName === 'tableData') {
          continue;
        }

        // if value contains children it does not belong to the overview section
        if (typeof value === 'object' && !Array.isArray(value)) {
          if (!attributeColumns[attributeName]) {
            attributeColumns[attributeName] = {}; //[];
          }

          let subAttributeNames = Object.keys(value);
          for (const subAttributeName of subAttributeNames) {
            const addedAttribute = attributeName + subAttributeName;
            if (addedAttributes.indexOf(addedAttribute) > -1) {
              continue;
            }

            addedAttributes.push(addedAttribute);

            attributeColumns[attributeName][subAttributeName] = {
              attribute: subAttributeName,
              visible: false,
            };
          }
        } else {
          const isVisible = initiallyVisibleColumns.includes(attributeName); //!hideInitially.includes(attributeName);

          if (addedAttributes.indexOf(attributeName) > -1) {
            continue;
          }
          addedAttributes.push(attributeName);

          attributeColumns[OVERVIEW_KEY][attributeName] = {
            attribute: attributeName,
            visible: isVisible,
          };
        }
      }
    }
    this.setState({ columnsConfiguration: attributeColumns });
  };

  /**
   * Go through all column information in the form of `state.columnsConfiguration` and translate those with visible=true to the
   * format expected by MaterialTable.
   */
  onAttributeActivatedChange = (attributes) => {
    let copiedAttributes = { ...attributes };
    let visibleColumns = [];
    for (let key of Object.keys(specialColumns)) {
      if (
        copiedAttributes[OVERVIEW_KEY][key] &&
        copiedAttributes[OVERVIEW_KEY][key].visible === true
      ) {
        visibleColumns.push(specialColumns[key]);
      }
    }

    for (let attributeCategory of Object.keys(copiedAttributes)) {
      if (attributeCategory === OVERVIEW_KEY) {
        continue;
      }

      for (let attribute of Object.values(
        copiedAttributes[attributeCategory]
      )) {
        if (attribute.visible === true) {
          const parentField =
            attributeCategory !== OVERVIEW_KEY ? attributeCategory : null;
          let visibleColumn = getDefaultMaterialTableColumn(
            attribute.attribute,
            parentField
          );
          visibleColumns.push(visibleColumn);
        }
      }
    }

    this.setState({
      visibleColumns: visibleColumns,
      columnsConfiguration: attributes,
    });
  };

  renderDetailPanel(rowData) {
    return (
      <div
        style={{
          backgroundColor: '#F5F5F5',
          paddingLeft: '0px',
          paddingTop: '0px',
          paddingBottom: '16px',
        }}
      >
        {TableComponents.RowDetail(this.state.jsonExperiments, { ...rowData })}
      </div>
    );
  }

  render() {
    let { widgetdata, visibleColumns, jsonExperiments } = this.state;

    /**
     * Override the ActionComponent of MaterialTable to prevent duplicated ripple effect.
     */
    const ActionComponent = (props) => {
      let action = props.action;

      if (action.action) {
        action = action.action(props.data);
        if (!action) {
          return null;
        }
      }

      const IconComponent = action.icon({ ...action.iconProps });
      return (
        <div style={{ display: 'inline-flex' }} {...props}>
          {IconComponent}
        </div>
      );
    };

    /**
     * The actions that are shown once for the whole table, so not per row!
     * @param {T} props
     */
    const HeaderActions = (props) => {
      if (jsonExperiments.data.length === 0) {
        return <div></div>;
      }

      return (
        <div style={{ display: 'inline-flex' }} {...props}>
          <NewAttributeDialog
            attributeColumns={this.state.columnsConfiguration}
            onAttributeActivatedChange={this.onAttributeActivatedChange}
          />
          <Tooltip title="Reload" placement="bottom">
            <IconButton color="default" onClick={this.loadExperiments}>
              <Icon>autorenew</Icon>
            </IconButton>
          </Tooltip>
        </div>
      );
    };

    return (
      <div style={{ width: '100%' }}>
        <BlockHeader name="Experiments" />
        <Widgets data={widgetdata} />

        <div style={{ paddingTop: '24px' }}>
          <Paper>
            <MaterialTable
              // the key attribute here makes sure that a new MaterialTable element is created rather than updated.
              // This prevents issues when updating the columns.
              key={'' + new Date()}
              title=""
              columns={visibleColumns}
              data={jsonExperiments.data}
              detailPanel={[
                {
                  render: (rowData) => {
                    return this.renderDetailPanel(rowData);
                  },
                  tooltip: 'Details',
                },
              ]}
              options={{
                filtering: true,
                pageSizeOptions: PAGE_SIZES,
                columnsButton: false,
                exportButton: true,
                exportFileName: 'experiments',
                grouping: true,
                detailPanelColumnAlignment: 'left',
                pageSize: 5,
                actionsColumnIndex: -1,
                headerStyle: {
                  // color: "rgba(0, 0, 0, 0.54)",
                  fontSize: '0.75rem',
                  fontWeight: 500,
                  fontFamily: 'Roboto',
                },
                rowStyle: {
                  fontSize: '0.75rem',
                  fontFamily: 'Roboto',
                },
              }}
              components={{
                Action: (props) => <ActionComponent {...props} />,
              }}
              actions={[
                {
                  icon: (props) => <HeaderActions {...props} />,
                  isFreeAction: true,
                  onClick: () => {}, // empty onClick handler here to avoid MaterialTable propTypes error
                },
                (rowData) => ({
                  icon: (props) => (
                    <DeleteExperimentButton
                      key="deleteBtn"
                      onItemDelete={() => {
                        let project = this.props.currentProject;
                        let experimentKey = rowData.key;
                        let toastID = ProcessToast.showProcessToast(
                          'Experiment will be deleted...'
                        );

                        GridApiHelper.deleteExperiment(
                          project,
                          experimentKey
                        ).then(() => {
                          toast.dismiss(toastID);
                          toast.success('Experiment deleted');
                          this.loadExperiments();
                        });
                      }}
                    />
                  ),
                }),
              ]}
              localization={{
                header: {
                  actions: '',
                },
              }}
            />
          </Paper>
        </div>
      </div>
    );
  }
}

ExperimentsNew.propTypes = {
  currentProject: PropTypes.string.isRequired,
};

export default connect(
  ReduxUtils.mapStateToProps,
  ReduxUtils.mapDispatchToProps
)(withStyles(TableComponents.styles)(ExperimentsNew));

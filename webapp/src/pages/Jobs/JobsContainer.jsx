import React from 'react';

import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';

import MaterialTable from 'material-table';

import moment from 'moment';

const COLUMNS = [
  {
    field: 'status',
    title: 'Status',
    numeric: false,
    align: 'left',
  },
  {
    field: 'name',
    title: 'Name',
    numeric: false,
    align: 'left',
    render: (rowData) => rowData.display_name,
  },
  {
    field: 'started_at',
    title: 'Started At',
    align: 'left',
    defaultSort: 'desc',
    render: (rowData) =>
      rowData.started_at ? moment(rowData.started_at).fromNow() : '',
  },
  {
    field: 'finishedAt',
    title: 'Finished At',
    align: 'left',
    render: (rowData) =>
      rowData.stopped_at ? moment(rowData.stopped_at).fromNow() : '',
  },
];

const PAGE_SIZES = [5, 10, 15, 30, 50, 75, 100];

function JobsContainer(props) {
  const {
    data,
    onReload,
    onJobDelete,
    onShowJobActions,
    onShowJobLogs,
    onShowJobMetadata,
  } = props;
  const { t } = useTranslation();

  return (
    <MaterialTable
      title={t('job_plural')}
      columns={COLUMNS}
      data={data}
      options={{
        filtering: true,
        sorting: true,
        columnsButton: false,
        exportButton: true,
        exportFileName: 'data',
        grouping: false,
        pageSize: 5,
        pageSizeOptions: PAGE_SIZES,
        actionsColumnIndex: -1,
        headerStyle: {
          fontSize: '0.75rem',
          fontWeight: 500,
          fontFamily: 'Roboto',
        },
        rowStyle: {
          fontSize: '0.75rem',
          fontFamily: 'Roboto',
        },
      }}
      localization={{ header: { actions: '' } }} // disable localization header name
      actions={[
        {
          icon: 'autorenew',
          isFreeAction: true,
          onClick: onReload,
          tooltip: t('reload'),
        },
        {
          icon: 'login',
          iconProps: { className: `` },
          onClick: (event, rowData) => {
            onShowJobActions(rowData);
          },
          tooltip: 'Access job',
        },
        {
          icon: 'code',
          iconProps: { className: `` },
          onClick: (event, rowData) => {
            onShowJobMetadata(rowData);
          },
          tooltip: 'Show job metadata',
        },
        {
          icon: 'assignment',
          iconProps: { className: `` },
          onClick: (event, rowData) => {
            onShowJobLogs(rowData);
          },
          tooltip: 'Display logs',
        },
        {
          icon: 'delete',
          iconProps: { className: `` },
          onClick: (event, rowData) => {
            onJobDelete(rowData);
          },
          tooltip: 'Delete job',
        },
      ]}
    />
  );
}

JobsContainer.propTypes = {
  data: PropTypes.arrayOf(Object),
  onReload: PropTypes.func,
  onJobDelete: PropTypes.func,
  onShowJobActions: PropTypes.func,
  onShowJobLogs: PropTypes.func,
  onShowJobMetadata: PropTypes.func,
};

JobsContainer.defaultProps = {
  data: [],
  onReload: () => {},
  onJobDelete: () => {},
  onShowJobActions: () => {},
  onShowJobLogs: () => {},
  onShowJobMetadata: () => {},
};

export default JobsContainer;

import MaterialTable from 'material-table';
import React from 'react';

import PropTypes from 'prop-types';

const PAGE_SIZES = [5, 10, 15, 30, 50, 75, 100];

const COLUMNS = [
  {
    field: 'name',
    title: 'Name',
    numeric: false,
    align: 'left',
    render: (rowData) => rowData.job_input?.display_name,
  },
  {
    field: 'image',
    title: 'Image',
    numeric: false,
    align: 'left',
    render: (rowData) => rowData.job_input?.container_image,
  },
  {
    field: 'schedule',
    title: 'Schedule',
    align: 'left',
    render: (rowData) => rowData.cron_string,
  },
  {
    field: 'lastExecuted',
    title: 'Last Executed',
    align: 'left',
    render: (rowData) =>
      rowData.last_run ? new Date(rowData.last_run).toLocaleString() : null,
  },
  {
    field: 'addedAt',
    title: 'Added at',
    align: 'left',
    render: (rowData) => new Date(rowData.created).toLocaleString(),
  },
];

function ScheduledJobsContainer(props) {
  const { data, onReload, onScheduledJobDelete } = props;
  return (
    <MaterialTable
      title="Scheduled Jobs"
      columns={COLUMNS}
      data={data}
      options={{
        filtering: true,
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
          onClick: () => onReload(),
          tooltip: 'Reload',
        },
        {
          icon: 'delete',
          iconProps: { className: `` },
          onClick: (event, rowData) => {
            onScheduledJobDelete(rowData);
          },
          tooltip: 'Delete job',
        },
      ]}
    />
  );
}

ScheduledJobsContainer.propTypes = {
  data: PropTypes.arrayOf(Object),
  onReload: PropTypes.func,
  onScheduledJobDelete: PropTypes.func,
};

ScheduledJobsContainer.defaultProps = {
  data: [],
  onReload: () => {},
  onScheduledJobDelete: () => {},
};

export default ScheduledJobsContainer;

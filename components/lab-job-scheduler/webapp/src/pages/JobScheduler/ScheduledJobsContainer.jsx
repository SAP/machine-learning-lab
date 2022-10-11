import MaterialTable from 'material-table';
import React from 'react';

const PAGE_SIZES = [5, 10, 15, 30, 50, 75, 100];

function ScheduledJobsContainer() {
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
      render: (rowData) => rowData.started_at?.getTime(),
    },
    {
      field: 'finishedAt',
      title: 'Finished At',
      align: 'left',
      render: (rowData) => rowData.stopped_at?.getTime(),
    },
  ];

  return (
    <MaterialTable
      title="Scheduled Jobs"
      columns={COLUMNS}
      data={[]}
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
          onClick: () => console.log('reloaded'),
          tooltip: 'Reload',
        },
        {
          icon: 'login',
          iconProps: { className: `` },
          onClick: (event, rowData) => {
            console.log(event);
            console.log(rowData);
          },
          tooltip: 'Access job',
        },
        {
          icon: 'code',
          iconProps: { className: `` },
          onClick: (event, rowData) => {
            console.log(event);
            console.log(rowData);
          },
          tooltip: 'Show job metadata',
        },
        {
          icon: 'assignment',
          iconProps: { className: `` },
          onClick: (event, rowData) => {
            console.log(event);
            console.log(rowData);
          },
          tooltip: 'Display logs',
        },
        {
          icon: 'delete',
          iconProps: { className: `` },
          onClick: (event, rowData) => {
            console.log(event);
            console.log(rowData);
          },
          tooltip: 'Delete job',
        },
      ]}
    />
  );
}

export default ScheduledJobsContainer;

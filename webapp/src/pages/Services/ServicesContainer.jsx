import React from 'react';

import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';

import CircularProgress from '@mui/material/CircularProgress';
import MaterialTable from 'material-table';

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
    field: 'container_image',
    title: 'Image',
    numeric: false,
    align: 'left',
  },
  {
    field: 'started_at',
    title: 'Started At',
    align: 'left',
    render: (rowData) => rowData.started_at?.getTime(),
  },
];

const PAGE_SIZES = [5, 10, 15, 30, 50, 75, 100];

function ServicesContainer(props) {
  const {
    data,
    onReload,
    onServiceDelete,
    onShowServiceActions,
    onShowServiceLogs,
    onShowServiceMetadata,
  } = props;
  const { t } = useTranslation();
  // https://github.com/facebook/react/issues/19098
  const [state, setState] = React.useState({ deleteIcon: 'delete' });
  console.log('Data');
  console.log(data);
  return (
    <MaterialTable
      title={t('service_plural')}
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
          onClick: onReload,
          tooltip: t('reload'),
        },
        {
          icon: 'login',
          iconProps: { className: `` },
          onClick: (event, rowData) => {
            onShowServiceActions(rowData);
          },
          tooltip: 'Access service',
        },
        {
          icon: 'code',
          iconProps: { className: `` },
          onClick: (event, rowData) => {
            onShowServiceMetadata(rowData);
          },
          tooltip: 'Show service metadata',
        },
        {
          icon: 'assignment',
          iconProps: { className: `` },
          onClick: (event, rowData) => {
            onShowServiceLogs(rowData);
          },
          tooltip: 'Display logs',
        },
        {
          icon: state.deleteIcon,
          iconProps: { className: `` },
          onClick: (_, rowData) => {
            console.log('Row data');
            console.log(rowData);
            setState({
              ...state,
              deleteIcon: () => (
                <CircularProgress
                  size={24}
                  style={{
                    color: '#3f51b5',
                  }}
                />
              ),
            });
            onServiceDelete(rowData);
          },
          tooltip: 'Delete service',
        },
      ]}
    />
  );
}

ServicesContainer.propTypes = {
  data: PropTypes.arrayOf(Object),
  onReload: PropTypes.func,
  onServiceDelete: PropTypes.func,
  onShowServiceActions: PropTypes.func,
  onShowServiceLogs: PropTypes.func,
  onShowServiceMetadata: PropTypes.func,
};

ServicesContainer.defaultProps = {
  data: [],
  onReload: () => {},
  onServiceDelete: () => {},
  onShowServiceActions: () => {},
  onShowServiceLogs: () => {},
  onShowServiceMetadata: () => {},
};

export default ServicesContainer;

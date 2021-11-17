import React from 'react';

import PropTypes from 'prop-types';

import MaterialTable from 'material-table';
import byteSize from 'byte-size';
import setClipboardText from '../../utils/clipboard';
import showStandardSnackbar from '../../app/showStandardSnackbar';

const PAGE_SIZES = [5, 10, 15, 30, 50, 75, 100];

const COLUMNS = [
  {
    field: 'display_name',
    title: 'Name',
    numeric: false,
    align: 'center',
    render: (row) => row.display_name.split('/').pop(),
  },
  {
    field: 'updated_at',
    title: 'Last modified',
    numeric: false,
    type: 'date',
    align: 'center',
  },
  // TODO: Uncomment when updated_by field is pobulated
  // {
  //   field: 'updated_by',
  //   title: 'Modified By',
  //   align: 'center',
  // },
  {
    field: 'version',
    title: 'Version',
    align: 'center',
  },
  {
    field: 'file_size',
    title: 'Size',
    align: 'center',
    render: (row) => byteSize(row.file_size).toString(),
  },
];

function FilesTable(props) {
  const { className, title, data, onFileDelete, onFileDownload, onReload } =
    props;
  return (
    <MaterialTable
      title={title || 'Files'}
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
          tooltip: 'Reload',
        },
        {
          icon: 'download',
          iconProps: { className: `${className} actionIcon` },
          onClick: (event, rowData) => {
            showStandardSnackbar('Download file');
            onFileDownload(rowData);
          },
          tooltip: 'Download File',
        },
        {
          icon: 'content_copy',
          iconProps: { className: `${className} actionIcon` },
          onClick: (event, rowData) => {
            showStandardSnackbar(`Copy file key: '${rowData.key}'`);
            setClipboardText(rowData.key);
          },
          tooltip: 'Copy File Key',
        },
        {
          icon: 'delete',
          iconProps: { className: `${className} actionIcon` },
          onClick: (event, rowData) => {
            showStandardSnackbar('Delete file');
            onFileDelete(rowData);
          },
          tooltip: 'Delete File',
        },
      ]}
    />
  );
}

FilesTable.propTypes = {
  className: PropTypes.string,
  title: PropTypes.string,
  data: PropTypes.arrayOf(Object),
  onFileDownload: PropTypes.func,
  onFileDelete: PropTypes.func,
  onReload: PropTypes.func,
};

FilesTable.defaultProps = {
  className: '',
  title: '',
  data: [],
  onFileDelete: () => {},
  onFileDownload: () => {},
  onReload: () => {},
};

export default React.memo(FilesTable);

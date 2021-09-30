import React from 'react';

import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';

import MaterialTable from 'material-table';
import setClipboardText from '../../utils/clipboard';
import showStandardSnackbar from '../../app/showStandardSnackbar';

const PAGE_SIZES = [5, 10, 15, 30, 50, 75, 100];

const COLUMNS = [
  {
    field: 'display_name',
    title: 'Name',
    numeric: false,
    align: 'center',
  },
  {
    field: 'updated_at',
    title: 'Last modified',
    numeric: false,
    type: 'date',
    align: 'center',
  },
  {
    field: 'updated_by',
    title: 'Modified By',
    align: 'center',
  },
  {
    field: 'version',
    title: 'Version',
    align: 'center',
  },
  {
    field: 'file_size',
    title: 'Size',
    align: 'center',
  },
];

function FilesTable(props) {
  const { t } = useTranslation();
  const { className, data, onFileDelete, onFileDownload, onReload } = props;
  return (
    <MaterialTable
      title={t('file_plural')}
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
          icon: 'download',
          iconProps: { className: `${className} actionIcon` },
          onClick: (event, rowData) => {
            showStandardSnackbar('Download file');
            onFileDownload(rowData);
          },
          tooltip: `${t('download')} ${t('file')}`,
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
  data: PropTypes.arrayOf(Object),
  onFileDownload: PropTypes.func,
  onFileDelete: PropTypes.func,
  onReload: PropTypes.func,
};

FilesTable.defaultProps = {
  className: '',
  data: [
    {
      name: 'Foobar',
      modifiedAt: 'a month ago',
      modifiedBy: 'admin',
      version: 2,
      size: '8.32 mb',
      fileKey: 'datasets%2Fnews-categorized.csv.v1',
    },
  ],
  onFileDelete: () => {},
  onFileDownload: () => {},
  onReload: () => {},
};

export default FilesTable;

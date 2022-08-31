// eslint-disable-next-line camelcase
import { unstable_batchedUpdates } from 'react-dom';
import React, { useCallback, useEffect, useRef, useState } from 'react';

import PropTypes from 'prop-types';
import byteSize from 'byte-size';
import styled from 'styled-components';

import Button from '@material-ui/core/Button';

import moment from 'moment';

import {
  filesApi,
  getFileDownloadUrl,
  getFileUploadUrl,
} from '../../services/contaxy-api';
import FilesTable from './FilesTable';

import GlobalStateContainer from '../../app/store';
import UploadFilesDialog from '../../components/Dialogs/UploadFilesDialog';
import Widget from '../../components/Widget';
import WidgetsGrid from '../../components/WidgetsGrid';
import showStandardSnackbar from '../../app/showStandardSnackbar';

function Files(props) {
  const { className, folder, uploadNote } = props;
  const [data, setData] = useState([]);
  const { activeProject } = GlobalStateContainer.useContainer();
  const projectId = activeProject.id;
  const [widgetData, setWidgetData] = useState({
    totalSize: '0',
    lastUpdated: '-',
  });
  const [isUploadFileDialogOpen, setUploadFileDialogOpen] = useState(false);
  const componentIsMounted = useRef(true);

  // Set component is mounted
  useEffect(() => {
    componentIsMounted.current = true;
    return () => {
      componentIsMounted.current = false;
    };
  }, []);

  const reloadFiles = useCallback(async () => {
    const files = await filesApi.listFiles(projectId, {
      prefix: folder,
    });
    if (componentIsMounted.current) {
      let totalSize = 0;
      let lastUpdated = 0;
      files.forEach((file) => {
        totalSize += file.file_size;
        lastUpdated =
          new Date(file.updated_at) > new Date(lastUpdated)
            ? file.updated_at
            : lastUpdated;
      });
      // Make sure state update only causes 1 rerender.
      // TODO: Check if this is also possible with a restructering of the component
      unstable_batchedUpdates(() => {
        setData(files);
        setWidgetData({
          totalSize: byteSize(totalSize).toString(),
          lastUpdated:
            lastUpdated > 0
              ? moment(lastUpdated).startOf('minute').fromNow()
              : '-',
        });
      });
    }
  }, [folder, projectId]);

  // Make sure files are reloaded if project or folder changes
  useEffect(() => {
    reloadFiles();
  }, [reloadFiles]);

  const onFileDelete = useCallback(
    async (rowData) => {
      try {
        await filesApi.deleteFile(projectId, rowData.key);
        showStandardSnackbar(`Deleted file (${rowData.key})`);
        reloadFiles();
      } catch (err) {
        showStandardSnackbar(`Error in deleting file (${rowData.key})`);
      }
    },
    [projectId, reloadFiles]
  );

  const onFileDownload = useCallback(
    (rowData) => {
      const a = document.createElement('a');
      a.href = getFileDownloadUrl(projectId, rowData.key);
      a.target = '_blank';
      a.download = rowData.display_name || 'download';
      a.click();
    },
    [projectId]
  );

  // const fileTable = useMemo(
  //   () => (
  //     <FilesTable
  //       data={data}
  //       onFileDownload={onFileDownload}
  //       onFileDelete={onFileDelete}
  //       onReload={reloadFiles}
  //     />
  //   ),
  //   [data, onFileDelete, reloadFiles, onFileDownload]
  // );
  const fileTable = (
    <FilesTable
      title={folder.charAt(0).toUpperCase() + folder.slice(1).toLowerCase()}
      data={data}
      onFileDownload={onFileDownload}
      onFileDelete={onFileDelete}
      onReload={reloadFiles}
    />
  );

  // TODO: add correct values to widget
  return (
    <div className="pages-native-component">
      <WidgetsGrid>
        <Widget name="Amount" icon="list" value={data.length} color="pink" />
        <Widget
          name="Total Size"
          icon="cloud"
          value={widgetData.totalSize}
          color="cyan"
        />
        <Widget
          name="Last Modified"
          icon="build"
          value={widgetData.lastUpdated}
          color="light-green"
        />
      </WidgetsGrid>
      <Button
        variant="contained"
        color="primary"
        onClick={() => setUploadFileDialogOpen(true)}
        className={`${className} button`}
      >
        Upload
      </Button>
      {fileTable}
      <UploadFilesDialog
        folder={folder}
        uploadEnpoint={getFileUploadUrl(projectId, '')}
        uploadNote={uploadNote}
        open={isUploadFileDialogOpen}
        onClose={() => {
          setUploadFileDialogOpen(false);
          reloadFiles(projectId);
        }}
      />
    </div>
  );
}

Files.propTypes = {
  className: PropTypes.string,
  // projectId: PropTypes.string.isRequired,
  folder: PropTypes.string.isRequired,
  uploadNote: PropTypes.string,
};

Files.defaultProps = {
  className: '',
  uploadNote: '',
};

const StyledFiles = styled(Files)`
  &.actionIcon {
    color: rgba(0, 0, 0, 0.54);
  }

  &.button {
    margin: 8px 0px;
  }
`;

export default StyledFiles;

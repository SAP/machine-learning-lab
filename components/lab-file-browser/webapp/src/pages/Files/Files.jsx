import React, {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';

import PropTypes from 'prop-types';
import styled from 'styled-components';

import Button from '@material-ui/core/Button';
import Typography from '@material-ui/core/Typography';

import moment from 'moment';

import {
  filesApi,
  getFileDownloadUrl,
  getFileUploadUrl,
} from '../../services/contaxy-api';
import FilesTable from './FilesTable';

import UploadFilesDialog from '../../components/Dialogs/UploadFilesDialog';
import Widget from '../../components/Widget';
import WidgetsGrid from '../../components/WidgetsGrid';
import showStandardSnackbar from '../../app/showStandardSnackbar';

function Files(props) {
  const { className } = props;
  const [data, setData] = useState([]);
  const [widgetData, setWidgetData] = useState({
    totalSize: '0',
    lastUpdated: '-',
  });
  // const { API_URL } = window.docker_env;
  // const { activeProject } = GlobalStateContainer.useContainer();
  const activeProject = {
    id: window.location.pathname.split('/')[2]
      ? window.location.pathname.split('/')[2]
      : '/',
  };

  const filesFolder = window.ctxyExtensionFiles.PREFIX;

  const [isUploadFileDialogOpen, setUploadFileDialogOpen] = useState(false);

  const componentIsMounted = useRef(true);

  const reloadFiles = useCallback(
    async (projectId) => {
      if (!projectId) return;
      const files = await filesApi.listFiles(projectId, {
        prefix: filesFolder,
      });
      if (componentIsMounted.current) {
        setData(files);

        let totalSize = 0;
        let lastUpdated = 0;
        files.forEach((file) => {
          totalSize += file.file_size;
          lastUpdated =
            new Date(file.updated_at) > new Date(lastUpdated)
              ? file.updated_at
              : lastUpdated;
        });
        setWidgetData({
          totalSize: `${(totalSize / 1000 ** 3).toFixed(2)} GB`,
          lastUpdated:
            lastUpdated > 0
              ? moment(lastUpdated).startOf('minute').fromNow()
              : '-',
        });
      }
    },
    [filesFolder]
  );

  useEffect(() => {
    componentIsMounted.current = true;
    // Will trigger inital loading during initial rendering
    reloadFiles(activeProject.id);
    // each useEffect can return a cleanup function
    return () => {
      componentIsMounted.current = false;
    };
  }, [activeProject.id, reloadFiles]);

  const onFileDelete = useCallback(
    async (rowData) => {
      try {
        await filesApi.deleteFile(activeProject.id, rowData.key);
        showStandardSnackbar(`Deleted file (${rowData.key})`);
        reloadFiles(activeProject.id);
      } catch (err) {
        showStandardSnackbar(`Error in deleting file (${rowData.key})`);
      }
    },
    [reloadFiles, activeProject.id]
  );

  const onFileDownload = useCallback(
    (rowData) => {
      const a = document.createElement('a');
      a.href = getFileDownloadUrl(activeProject.id, rowData.key);
      a.target = '_blank';
      a.download = rowData.name || 'download';
      a.click();
    },
    [activeProject.id]
  );

  const fileTable = useMemo(
    () => (
      <FilesTable
        data={data}
        onFileDownload={onFileDownload}
        onFileDelete={onFileDelete}
        onReload={() => reloadFiles(activeProject.id)}
      />
    ),
    [activeProject.id, data, onFileDelete, reloadFiles, onFileDownload]
  );

  // TODO: add correct values to widget
  return (
    <div className="pages-native-component">
      <Typography variant="h5" gutterBottom>
        {filesFolder.toUpperCase()}
      </Typography>
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
        endpoint={getFileUploadUrl(activeProject.id, '')}
        open={isUploadFileDialogOpen}
        onClose={() => {
          setUploadFileDialogOpen(false);
          reloadFiles(activeProject.id);
        }}
      />
    </div>
  );
}

Files.propTypes = {
  className: PropTypes.string,
};

Files.defaultProps = {
  className: '',
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

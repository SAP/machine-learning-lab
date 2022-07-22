import React, { useEffect, useState } from 'react';

import { Route, Routes } from 'react-router-dom';
import PropTypes, { object } from 'prop-types';

import MaterialTable from '@material-table/core';
import Stack from '@mui/material/Stack';
import { Button, CircularProgress, IconButton } from '@mui/material';
import VisibilityIcon from '@mui/icons-material/Visibility';
import LoginIcon from '@mui/icons-material';
import DeleteIcon from '@mui/icons-material/Delete';
import ContentCopyIcon from '@mui/icons-material/ContentCopy';
import { displayNameToId } from '../services/helper';
import {
  creatSecret,
  deleteSecret,
  getSecretsPassword,
  useSecrets,
} from '../services/secret-store-api';
import setClipboardText from '../utils/clipboard';
import { useShowAppDialog } from '../app/AppDialogServiceProvider';
import ContentDialogForJson from '../components/Dialogs/ContentDialogForJson';
import ContentDialog from '../components/Dialogs/ContentDialog';

function MethadataDisplay(props) {
  const { metadata } = props;
  const shown = ['username', 'id'];

  for (const s of shown) {
    for (const i of Object.keys(metadata)) {
      if (s === i) {
        return <p>{`${i}: ${metadata[i]}`}</p>;
      }
    }
  }
  return '';
}
MethadataDisplay.propTypes = {
  metadata: PropTypes.instanceOf(Object).isRequired,
};

function PasswordDisplay(props) {
  const { displayName, project } = props;
  let text;
  const [password, setPassword] = useState('loading...');
  const [passwordShown, setPasswordShown] = useState(false);
  const togglePasswordVisiblity = async () => {
    setPasswordShown(!passwordShown);
  };

  useEffect(() => {
    const getPw = async () => {
      const pw = await getSecretsPassword(
        project,
        displayNameToId(displayName)
      );
      setPassword(pw.secret_text);
    };
    if (passwordShown) {
      getPw();
    }
  }, [project, displayName, passwordShown]);

  if (passwordShown) {
    text = <p>{password}</p>;
  }

  return (
    <Stack direction="row" spacing={1}>
      {text}
      <IconButton onClick={togglePasswordVisiblity}>
        <VisibilityIcon />
      </IconButton>
      <IconButton
        onClick={async () => {
          const pw = await getSecretsPassword(
            project,
            displayNameToId(displayName)
          );
          setClipboardText(pw.secret_text);
        }}
      >
        <ContentCopyIcon />
      </IconButton>
    </Stack>
  );
}
PasswordDisplay.propTypes = {
  displayName: PropTypes.string.isRequired,
  project: PropTypes.string.isRequired,
};

const PAGE_SIZES = [5, 10, 15, 30, 50, 75, 100];

function SecretStore() {
  const queryString = window.location.search;
  const urlParams = new URLSearchParams(queryString);
  const project = urlParams.get('project');

  const [secrets, secretReload] = useSecrets(project);

  const showAppDialog = useShowAppDialog();
  const openDialog = () => {
    showAppDialog(ContentDialog, {
      title: 'Create secret',
      refresh: secretReload,
      project,
    });
  };

  function DeleteDisplay(props) {
    const { displayName } = props;
    let text;
    const [isLoading, setIsLoading] = useState();

    if (isLoading) {
      text = <CircularProgress size="2rem" color="secondary" />;
    } else {
      text = (
        <IconButton
          onClick={async () => {
            setIsLoading(true);
            const pw = await deleteSecret(
              project,
              displayNameToId(displayName)
            );
            setIsLoading(true);
            secretReload();
          }}
        >
          <DeleteIcon />
        </IconButton>
      );
    }

    return (
      <Stack direction="row" spacing={1}>
        {text}
      </Stack>
    );
  }
  DeleteDisplay.propTypes = {
    displayName: PropTypes.string.isRequired,
  };

  const COLUMNS = [
    {
      field: 'display_name',
      title: 'Name',
      numeric: false,
      align: 'left',
      width: '30%',
    },
    {
      title: 'metadata',
      numeric: false,
      sortable: false,
      disableClickEventBubbling: true,
      width: '30%',
      render: (params) => (
        <Stack direction="row" spacing={1}>
          <MethadataDisplay metadata={params.metadata} />
          <IconButton
            onClick={() => {
              showAppDialog(ContentDialogForJson, {
                jsonContent: params.metadata,
                title: 'Service Metadata',
              });
              // return alert(JSON.stringify(params.metadata, null, 4));
            }}
          >
            <VisibilityIcon />
          </IconButton>
        </Stack>
      ),
    },
    {
      title: 'secred',
      numeric: false,
      sortable: false,
      disableClickEventBubbling: true,
      width: '30%',
      render: (params) => {
        return (
          <PasswordDisplay
            displayName={params.display_name}
            project={project}
          />
        );
      },
    },
    {
      field: 'delete',
      title: '',
      width: '5%',
      render: (params) => {
        return <DeleteDisplay displayName={params.display_name} />;
      },
    },
  ];

  return (
    <Stack
      sx={{
        height: '100%',
        ml: 3,
        mr: 3,
        mt: 6,
      }}
    >
      <Button
        onClick={() => openDialog()}
        variant="contained"
        color="primary"
        style={{ maxWidth: '150px' }}
      >
        Add Secret
      </Button>
      <MaterialTable
        title="Secrets"
        columns={COLUMNS}
        data={secrets || []}
        options={{
          filtering: false,
          columnsButton: false,
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
            onClick: () => {
              secretReload();
            },
            tooltip: 'reload',
          },
        ]}
      />
    </Stack>
  );
}

export default SecretStore;

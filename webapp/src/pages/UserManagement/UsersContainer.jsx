import React from 'react';
import moment from 'moment';

import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';

import MaterialTable from 'material-table';

import { useShowAppDialog } from '../../app/AppDialogServiceProvider';
import { usersApi } from '../../services/contaxy-api';
import ChangePasswordDialog from '../../components/Dialogs/ChangePasswordDialog';
import ConfirmDeleteDialog from '../../components/Dialogs/ConfirmDeleteDialog';
import showStandardSnackbar from '../../app/showStandardSnackbar';

const COLUMNS = [
  {
    field: 'username',
    title: 'Username',
    numeric: false,
    align: 'left',
  },
  {
    field: 'id',
    title: 'ID',
    numeric: false,
    align: 'left',
  },
  {
    field: 'email',
    title: 'Email',
    numeric: false,
    align: 'left',
  },
  {
    field: 'created_at',
    title: 'Created At',
    align: 'left',
    render: (rowData) =>
      moment(rowData.created_at).format('DD-MM-YYYY hh:mm:ss'),
  },
];

const PAGE_SIZES = [5, 10, 15, 30, 50, 75, 100];

function UsersContainer(props) {
  const { data, onReload, getUsers } = props;
  const { t } = useTranslation();
  const showAppDialog = useShowAppDialog();

  const onUserDelete = (rowData) => {
    showAppDialog(ConfirmDeleteDialog, {
      dialogTitle: 'Delete User',
      dialogText: `Do you really want to delete the user ${rowData.username}?`,
      onDelete: async (onClose) => {
        try {
          await usersApi.deleteUser(rowData.id);
          showStandardSnackbar(`Deleted user ${rowData.id}`);
          getUsers();
        } catch (err) {
          showStandardSnackbar(
            `Could not delete user ${rowData.id}! ${err.body.message}.`
          );
        }
        onClose();
      },
    });
  };

  const onChangePassword = (rowData) => {
    showAppDialog(ChangePasswordDialog, {
      dialogTitle: 'Change Password',
      onSubmit: async (onClose, password) => {
        try {
          await usersApi.changePassword(rowData.id, `"${password}"`);
          showStandardSnackbar(
            `User ${rowData.username}'s password successfully changed.`
          );
        } catch (e) {
          showStandardSnackbar(
            `Could not change ${rowData.username}'s password. Reason: ${e.body.message}`
          );
        }
        onClose();
      },
    });
  };

  return (
    <MaterialTable
      title={t('user_plural')}
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
          icon: 'delete',
          iconProps: { className: `` },
          onClick: (event, rowData) => {
            onUserDelete(rowData);
          },
          tooltip: 'Delete User',
        },
        {
          icon: 'password',
          iconProps: { className: `` },
          onClick: (event, rowData) => {
            onChangePassword(rowData);
          },
          tooltip: 'Change Password',
        },
      ]}
    />
  );
}

UsersContainer.propTypes = {
  data: PropTypes.arrayOf(Object),
  onReload: PropTypes.func,
  getUsers: PropTypes.func,
};

UsersContainer.defaultProps = {
  data: [],
  onReload: () => {},
  getUsers: () => {},
};

export default UsersContainer;

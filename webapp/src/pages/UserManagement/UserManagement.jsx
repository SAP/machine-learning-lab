import React, { useEffect, useState } from 'react';

import Button from '@material-ui/core/Button';

import PropTypes from 'prop-types';
import styled from 'styled-components';

import { useShowAppDialog } from '../../app/AppDialogServiceProvider';
import { usersApi } from '../../services/contaxy-api';
import CreateUserDialog from '../../components/Dialogs/CreateUserDialog';
// import Dashboard from '../../components/Dashboard';
// import UserCard from '../../components/UserCard';
// import UserSearch from '../../components/UserSearch';
import UsersContainer from './UsersContainer';
import Widget from '../../components/Widget';
import WidgetsGrid from '../../components/WidgetsGrid';
import showStandardSnackbar from '../../app/showStandardSnackbar';

function UserManagement(props) {
  const { className } = props;
  const showAppDialog = useShowAppDialog();
  const [users, setUsers] = useState([]);

  const requestUsers = async () => {
    let response;
    try {
      response = await usersApi.listUsers();
      setUsers(response);
    } catch (e) {
      setUsers([]);
    }
  };

  const onCreateUser = () => {
    showAppDialog(CreateUserDialog, {
      onAdd: async ({ username, email, disabled, password }, onClose) => {
        const user = {
          username,
          email,
          disabled,
          password,
        };
        try {
          await usersApi.createUser(user);
          showStandardSnackbar(`Created user: '${username}' `);
          requestUsers();
          onClose();
        } catch (err) {
          showStandardSnackbar(`Could not create project. ${err.body.message}`);
        }
      },
    });
  };

  useEffect(() => {
    requestUsers();
  }, [setUsers]);

  return (
    <div className="pages-native-component">
      <WidgetsGrid>
        <Widget
          classes={{ root: `${className} widgetProjectsCount` }}
          name="Total users"
          icon="group"
          value={users.length}
          color="light-blue"
        />
      </WidgetsGrid>
      <Button color="primary" variant="contained" onClick={onCreateUser}>
        Create User
      </Button>
      <UsersContainer data={users} getUsers={requestUsers} />
    </div>
  );
}

UserManagement.propTypes = {
  className: PropTypes.string,
};

UserManagement.defaultProps = {
  className: '',
};

const StyledUserManagement = styled(UserManagement)`
  &.button {
    margin: 8px 0px;
  }

  &.widgetProjectsCount {
    flex: 0.3;
  }
`;

export default StyledUserManagement;

import React, { useEffect, useState } from 'react';

import PropTypes from 'prop-types';
import styled from 'styled-components';

import AccountCircle from '@material-ui/icons/AccountCircle';
import Box from '@material-ui/core/Box';
import Button from '@material-ui/core/Button';
import Chip from '@material-ui/core/Chip';
import Dialog from '@material-ui/core/Dialog';
import DialogActions from '@material-ui/core/DialogActions';
import DialogContent from '@material-ui/core/DialogContent';
import DialogTitle from '@material-ui/core/DialogTitle';
import FormControl from '@material-ui/core/FormControl';
import HighlightOff from '@material-ui/icons/HighlightOff';
import IconButton from '@material-ui/core/IconButton';
import List from '@material-ui/core/List';
import ListItem from '@material-ui/core/ListItem';
import ListItemSecondaryAction from '@material-ui/core/ListItemSecondaryAction';
import MenuItem from '@material-ui/core/MenuItem';
import OutlinedInput from '@material-ui/core/OutlinedInput';
import Typography from '@material-ui/core/Typography';

import { Select } from '@material-ui/core';
import { projectsApi, usersApi } from '../../services/contaxy-api';
import { useProjectMembers } from '../../services/api-hooks';
import GlobalStateContainer from '../../app/store';
import UserSearch from '../UserSearch';
import showStandardSnackbar from '../../app/showStandardSnackbar';

function ManageProjectDialog(props) {
  const { className, project, onClose } = props;
  const globalStateContainer = GlobalStateContainer.useContainer();
  const { loadProjects } = globalStateContainer;
  const [users, setUsers] = useState([]);
  const [selectedUser, setSelectedUser] = useState(''); // set to empty object so that material-ui knows that it is a controlled input
  const [permission, setPermission] = useState('read');
  const [projectMembers, reloadProjectMembers] = useProjectMembers(project.id);

  const onUserSelect = (user) => {
    setSelectedUser(user);
  };

  async function requestUsers(projectMembers) {
    let response;
    try {
      response = await usersApi.listUsers();
      const userList = response.filter(
        (a) => !projectMembers.some((b) => a.id === b.id)
      );
      setUsers(userList);
    } catch (e) {
      setUsers([]);
    }
  }

  const handleAddUser = async () => {
    try {
      await projectsApi.addProjectMember(project.id, selectedUser.id, {
        accessLevel: permission,
      });
      showStandardSnackbar(
        `Added user '${selectedUser.username}' to project '${project.id} with permission '${permission}''.`
      );
      onUserSelect('');
      reloadProjectMembers();
    } catch (e) {
      showStandardSnackbar(
        `Could not add user '${selectedUser.username}' to project '${project.id}'.`
      );
    }
  };

  useEffect(() => {
    requestUsers(projectMembers);
  }, [setUsers, projectMembers]);

  const handleRemoveMemberFromProject = async (user) => {
    try {
      await projectsApi.removeProjectMember(project.id, user.id);
      showStandardSnackbar(`Removed member.`);
      reloadProjectMembers();
      loadProjects();
    } catch (err) {
      showStandardSnackbar(
        `Could not remove member. Reason: ${err.body.message}`
      );
    }
  };

  const handleChange = (event) => {
    setPermission(event.target.value);
  };

  return (
    <Dialog open>
      <DialogTitle>{`Manage "${
        project.display_name || project.id
      }"`}</DialogTitle>
      <DialogContent>
        <Typography variant="subtitle1">Project members:</Typography>
        <List>
          {projectMembers.map((member) => {
            return (
              <ListItem key={member.id}>
                <AccountCircle />
                <Box
                  textAlign="right"
                  style={{ paddingRight: 5, paddingLeft: 5 }}
                >
                  {member.username}
                  <Chip
                    label={member.permission}
                    size="small"
                    style={{
                      marginLeft: 10,
                      textTransform: 'uppercase',
                      fontSize: '10px',
                    }}
                    color={
                      member.permission === 'admin' ? 'primary' : 'default'
                    }
                  />
                </Box>
                <ListItemSecondaryAction>
                  <IconButton
                    onClick={() => handleRemoveMemberFromProject(member)}
                  >
                    <HighlightOff color="secondary" />
                  </IconButton>
                </ListItemSecondaryAction>
              </ListItem>
            );
          })}
        </List>
        <Typography
          className={`${className} addUserHeader`}
          variant="subtitle1"
        >
          Add members:
        </Typography>
        <div className={`${className} userInputContainer`}>
          <UserSearch
            userList={users}
            onUserSelect={onUserSelect}
            value={selectedUser}
            selected={selectedUser}
          />
          <div
            style={{ width: 100 }}
            className={`${className} selectPermssion`}
          >
            <FormControl fullWidth>
              <Select
                labelId="permission-select-label"
                label="Permissions"
                id="permission-select"
                onChange={handleChange}
                defaultValue="read"
                input={<OutlinedInput variant="filled" />}
                autoWidth
              >
                <MenuItem value="read">Read</MenuItem>
                <MenuItem value="write">Write</MenuItem>
                <MenuItem value="admin">Admin</MenuItem>
              </Select>
            </FormControl>
          </div>
          <Button
            color="secondary"
            onClick={async () => {
              await handleAddUser();
            }}
            disabled={selectedUser === null}
          >
            ADD
          </Button>
        </div>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} color="primary">
          CANCEL
        </Button>
      </DialogActions>
    </Dialog>
  );
}

ManageProjectDialog.propTypes = {
  className: PropTypes.string,
  onClose: PropTypes.func.isRequired,
  project: PropTypes.instanceOf(Object).isRequired,
};

ManageProjectDialog.defaultProps = {
  className: '',
};

const StyledManageProjectDialog = styled(ManageProjectDialog)`
  &.addUserHeader {
    margin-top: 16px;
  }

  &.userInputContainer {
    display: flex;
  }

  &.userInputTextField {
    flex: 1;
  }

  &.selectPermssion {
    padding-top: 16px;
  }
`;

export default StyledManageProjectDialog;

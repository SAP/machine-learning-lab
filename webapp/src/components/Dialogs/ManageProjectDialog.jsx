import React, { useState } from "react";

import PropTypes from "prop-types";
import styled from "styled-components";

import AccountCircle from "@material-ui/icons/AccountCircle";
import Button from "@material-ui/core/Button";
import Dialog from "@material-ui/core/Dialog";
import DialogActions from "@material-ui/core/DialogActions";
import DialogContent from "@material-ui/core/DialogContent";
import DialogTitle from "@material-ui/core/DialogTitle";
import HighlightOff from "@material-ui/icons/HighlightOff";
import IconButton from "@material-ui/core/IconButton";
import List from "@material-ui/core/List";
import ListItem from "@material-ui/core/ListItem";
import ListItemSecondaryAction from "@material-ui/core/ListItemSecondaryAction";
import ListItemText from "@material-ui/core/ListItemText";
import TextField from "@material-ui/core/TextField";
import Typography from "@material-ui/core/Typography";

import { projectsApi } from "../../services/contaxy-api";
import { useProjectMembers } from "../../services/api-hooks";
import GlobalStateContainer from "../../app/store";
import showStandardSnackbar from "../../app/showStandardSnackbar";

function ManageProjectDialog(props) {
  const { className, project, onClose } = props;
  const globalStateContainer = GlobalStateContainer.useContainer();
  const { loadProjects } = globalStateContainer;
  const [userToAdd, setUserToAdd] = useState(""); // set to empty object so that material-ui knows that it is a controlled input
  const [projectMembers, reloadProjectMembers] = useProjectMembers(project.id);

  const handleSelectUser = (e) => {
    setUserToAdd(e.target.value);
  };

  const handleAddUser = async () => {
    try {
      await projectsApi.addProjectMember(project.id, userToAdd);
      showStandardSnackbar(
        `Added user '${userToAdd}' to project '${project.id}'.`
      );
      reloadProjectMembers();
    } catch (e) {
      showStandardSnackbar(
        `Could not add user '${userToAdd}' to project '${project.id}'.`
      );
    }
  };

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
                <ListItemText primary={member.username} />
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
          <TextField
            className={`${className} userInputTextfield`}
            value={userToAdd}
            onChange={handleSelectUser}
            placeholder="User Id"
          />
          <Button
            color="secondary"
            onClick={handleAddUser}
            disabled={userToAdd === null || Object.keys(userToAdd).length === 0}
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
  className: "",
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
`;

export default StyledManageProjectDialog;

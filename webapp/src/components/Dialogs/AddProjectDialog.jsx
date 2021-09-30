import React, { useEffect, useRef, useState } from 'react';

import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import Button from '@material-ui/core/Button';
import Dialog from '@material-ui/core/Dialog';
import DialogActions from '@material-ui/core/DialogActions';
import DialogContent from '@material-ui/core/DialogContent';
import DialogTitle from '@material-ui/core/DialogTitle';
import TextField from '@material-ui/core/TextField';

import { projectsApi } from '../../services/contaxy-api';
import showStandardSnackbar from '../../app/showStandardSnackbar';

const VALID_PROJECT_ID = new RegExp('^[a-z0-9-:/.]{0,25}$');
const VALID_PROJECT_NAME = new RegExp('^[a-zA-Z0-9-\\s]*$');
function AddProjectDialog(props) {
  const { className, onAdd, onClose } = props;
  const { t } = useTranslation();
  const [projectId, setProjectId] = useState('');
  const [projectName, setProjectName] = useState('');
  const [projectDescription, setProjectDescription] = useState('');

  const requestId = useRef(new Date().getTime());

  useEffect(() => {
    // when the component unmounts, change the requestId so that pending requests don't try to modify the state.
    return () => {
      requestId.current = new Date().getTime();
    };
  }, []);

  const changeProjectId = (e) => {
    setProjectId(e.target.value);
  };

  const [isProjectIdDisabled, setIsProjectIdDisabled] = useState(true);

  const changeDescription = async (e) => {
    setProjectDescription(e.target.value);
  };

  const changeName = (e) => {
    const input = e.target.value;
    requestId.current = new Date().getTime();
    if (input && input.length > 3) {
      // This construct allows to cancel modifying the state when the name already changed before the request succeeds.
      // Also, the request to the backend is made after a delay and, again, only if the name did not change until then.
      ((innerUid) => {
        setTimeout(() => {
          if (requestId.current !== innerUid) return;
          projectsApi
            .suggestProjectId(input)
            .then((id) => {
              if (requestId.current !== innerUid) return;
              setProjectId(id);
            })
            .catch(() => {
              if (requestId.current !== innerUid) return;
              showStandardSnackbar(
                'Error in getting a suggested project id. Please edit it manually.'
              );
            });
        }, 500);
      })(requestId.current);
    }
    setProjectName(input);
    setIsProjectIdDisabled(true);
  };

  const isProjectIdValid = VALID_PROJECT_ID.test(projectId);
  const isProjectNameValid = VALID_PROJECT_NAME.test(projectName);

  let displayNameHelperText = 'The name must be at least 4 characters long.';
  if (!isProjectNameValid) {
    displayNameHelperText = 'The name contains invalid characters.';
  }

  return (
    <Dialog open>
      <DialogTitle>{`${t('add')} ${t('project')}`}</DialogTitle>
      <DialogContent>
        <TextField
          required
          label="Project Displayname"
          name="name"
          type="text"
          value={projectName}
          onChange={changeName}
          error={!isProjectNameValid}
          helperText={displayNameHelperText}
          margin="dense"
          fullWidth
        />
        <div className={`${className} projectIdFields`}>
          <TextField
            required
            disabled={isProjectIdDisabled}
            label="Project Id"
            name="id"
            type="text"
            value={projectId}
            onChange={changeProjectId}
            error={!isProjectIdValid}
            helperText={
              !isProjectIdValid
                ? 'The project id must be at most 25 characters long and contain only valid characters.'
                : 'The project id is auto-generated based on the name. You can also edit it manually.'
            }
            margin="dense"
            fullWidth={!isProjectIdDisabled}
          />
          {isProjectIdDisabled ? (
            <Button onClick={() => setIsProjectIdDisabled(false)}>Edit</Button>
          ) : (
            false
          )}
        </div>
        <TextField
          label="Project Description"
          name="description"
          type="text"
          value={projectDescription}
          onChange={changeDescription}
          margin="dense"
          fullWidth
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} color="primary">
          CANCEL
        </Button>
        <Button
          disabled={!isProjectNameValid || !isProjectIdValid || !projectId}
          onClick={() =>
            onAdd(
              {
                id: projectId,
                name: projectName,
                description: projectDescription,
              },
              onClose
            )
          }
          color="primary"
        >
          ADD
        </Button>
      </DialogActions>
    </Dialog>
  );
}

AddProjectDialog.propTypes = {
  className: PropTypes.string,
  onAdd: PropTypes.func.isRequired,
  onClose: PropTypes.func.isRequired,
};

AddProjectDialog.defaultProps = {
  className: '',
};

const StyledAddProjectDialog = styled(AddProjectDialog)`
  &.projectIdFields {
    display: flex;
  }
`;

export default StyledAddProjectDialog;

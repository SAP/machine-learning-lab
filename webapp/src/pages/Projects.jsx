import React from 'react';

import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import Button from '@material-ui/core/Button';
import Grid from '@material-ui/core/Grid';

import { projectsApi } from '../services/contaxy-api';
import { useProjectSelector } from '../utils/app-utils';
import { useShowAppDialog } from '../app/AppDialogServiceProvider';
import AddProjectDialog from '../components/Dialogs/AddProjectDialog';
import ConfirmDeleteDialog from '../components/Dialogs/ConfirmDeleteDialog';
import GlobalStateContainer from '../app/store';
import ManageProjectDialog from '../components/Dialogs/ManageProjectDialog';
import ProjectCard from '../components/ProjectCard';
import Widget from '../components/Widget';
import WidgetsGrid from '../components/WidgetsGrid';
import showStandardSnackbar from '../app/showStandardSnackbar';

function Projects(props) {
  const { className } = props;
  const { t } = useTranslation();
  const showAppDialog = useShowAppDialog();
  const { activeProject, projects, loadProjects } =
    GlobalStateContainer.useContainer();
  const onProjectSelect = useProjectSelector();

  const onClickManageMembers = (project) => {
    showAppDialog(ManageProjectDialog, { project });
  };

  const onAddProject = () => {
    showAppDialog(AddProjectDialog, {
      onAdd: async ({ id, name, description }, onClose) => {
        const projectInput = {
          id,
          display_name: name,
          description,
        };
        try {
          await projectsApi.createProject(projectInput);
          showStandardSnackbar(`Created project '${id}'`);
          onClose();
          loadProjects();
        } catch (err) {
          showStandardSnackbar(`Could not create project. ${err.body.message}`);
        }
      },
    });
  };

  const onDeleteProject = (project) => {
    showAppDialog(ConfirmDeleteDialog, {
      dialogTitle: 'Delete Project',
      dialogText: `Do you really want to delete the project ${project.display_name}?`,
      onDelete: async (onClose) => {
        try {
          await projectsApi.deleteProject(project.id);
          showStandardSnackbar(`Deleted project ${project.id}`);
          loadProjects();
        } catch (err) {
          showStandardSnackbar(
            `Could not delete project ${project.id}! ${err.body.message}.`
          );
        }
        onClose();
      },
    });
  };

  const projectElements = projects.map((project) => {
    return (
      <ProjectCard
        key={project.id}
        project={project}
        isHighlighted={project.id === activeProject.id}
        onClickManageMembers={onClickManageMembers}
        onDeleteProject={onDeleteProject}
        onSelect={onProjectSelect}
      />
    );
  });

  // TODO: add this again, when the projects endpoint returns metadata
  // const fileNumber = activeProject.metadata
  //   ? activeProject.metadata.fileNumber
  //   : 0;
  // const serviceNumber = activeProject.metadata
  //   ? activeProject.metadata.serviceNumber
  //   : 0;

  return (
    <div className="pages-native-component">
      <WidgetsGrid>
        {/* <Widget
          name={t('file_plural')}
          icon="folder"
          value={fileNumber}
          color="light-green"
        />
        <Widget
          name="Services"
          icon="apps"
          value={serviceNumber}
          color="orange"
        /> */}
        <Widget
          classes={{ root: `${className} widgetProjectsCount` }}
          name="Projects"
          icon="apps"
          value={projectElements.length}
          color="light-green"
        />
      </WidgetsGrid>
      <Button
        variant="contained"
        color="primary"
        onClick={onAddProject}
        className={`${className} button`}
      >
        {`${t('add')} ${t('project')}`}
      </Button>
      <Grid container spacing={3}>
        {projectElements}
      </Grid>
    </div>
  );
}

Projects.propTypes = {
  className: PropTypes.string,
};

Projects.defaultProps = {
  className: '',
};

const StyledProjects = styled(Projects)`
  &.button {
    margin: 8px 0px;
  }

  &.widgetProjectsCount {
    flex: 0.3;
  }
`;

export default StyledProjects;

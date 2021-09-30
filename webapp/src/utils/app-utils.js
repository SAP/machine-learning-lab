/* eslint-disable import/prefer-default-export */
import showStandardSnackbar from '../app/showStandardSnackbar';
/* eslint-disable sort-imports-es6-autofix/sort-imports-es6 */
import GlobalStateContainer from '../app/store';
import { SELECTED_PROJECT_LOCAL_STORAGE_KEY } from '../services/contaxy-api';

export const getProjectPermissionId = (project, permissionLevel) => {
  const level = permissionLevel ? `#${permissionLevel}` : '';
  return `projects/${project.id}${level}`;
};

export const getUserPemissionId = (user, permissionLevel) => {
  const level = permissionLevel ? `#${permissionLevel}` : '';
  return `users/${user.id}${level}`;
};

export const useProjectSelector = () => {
  const { setActiveProject } = GlobalStateContainer.useContainer();

  const onProjectSelect = (project) => {
    const newProject = { ...project };
    // try {
    //   const projectMetadata = await projectsApi.getProject(project.id);
    //   newProject.metadata = projectMetadata;
    // } catch (ignore) {} // eslint-disable-line no-empty

    showStandardSnackbar(`Change to project '${project.id}'`);
    setActiveProject(newProject);
    window.localStorage.setItem(SELECTED_PROJECT_LOCAL_STORAGE_KEY, project.id);
  };

  return onProjectSelect;
};

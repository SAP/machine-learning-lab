// eslint-disable-next-line camelcase
import { unstable_batchedUpdates } from 'react-dom';
import React, { useCallback, useEffect, useState } from 'react';

// import { useTranslation } from 'react-i18next';

import './App.css';
import {
  SELECTED_PROJECT_LOCAL_STORAGE_KEY,
  authApi,
  extensionsApi,
  usersApi,
} from '../../services/contaxy-api';
import { mapExtensionToAppPage } from '../../utils/app-pages';
import AppBar from '../../components/AppBar/AppBar';
import AppDrawer from '../../components/AppDrawer/AppDrawer';
import ContentContainer from '../../app/routing/ContentContainer';
import GlobalStateContainer from '../../app/store';

function App() {
  // const { t } = useTranslation();
  const [isDrawerOpen, setDrawerOpen] = useState(false);
  const [additionalAppDrawerItems, setAdditionalAppDrawerItems] = useState([]);
  const {
    activeProject,
    setActiveProject,
    loadProjects,
    setUser,
    projects,
    user,
    projectExtensions,
    setProjectExtensions,
    setIsAuthenticated,
    isAuthenticated,
    setOauthEnabled,
  } = GlobalStateContainer.useContainer();
  const onDrawerClick = useCallback(
    () => setDrawerOpen(!isDrawerOpen),
    [isDrawerOpen]
  );

  useEffect(() => {
    if (isAuthenticated && user) return;
    async function requestUserInfo() {
      // Check whether the user is logged in currently (the auth cookie - if existing - is sent to the endpoint which returns a user object when a valid token exists and an error otherwise)
      let userInfo;
      try {
        userInfo = await usersApi.getMyUser();
      } catch (e) {
        unstable_batchedUpdates(() => {
          setUser(null);
          setIsAuthenticated(false);
        });
        return;
      }
      try {
        await authApi.verifyAccess({
          permission: '*#admin',
        });
        userInfo.is_admin = true;
      } catch (e) {
        userInfo.is_admin = false;
      }
      unstable_batchedUpdates(() => {
        setUser(userInfo);
        setIsAuthenticated(true);
      });
    }
    requestUserInfo();
  }, [user, isAuthenticated, setUser, setIsAuthenticated]);

  useEffect(() => {
    if (!user) return;
    loadProjects();
  }, [user, loadProjects]);

  useEffect(() => {
    if (activeProject.id || !projects || projects.length === 0) return;

    const prevSelectedProjectId = window.localStorage.getItem(
      SELECTED_PROJECT_LOCAL_STORAGE_KEY
    );

    if (prevSelectedProjectId) {
      const previouslySelectedProject = projects.find(
        (project) => project.id === prevSelectedProjectId
      );
      if (previouslySelectedProject) {
        setActiveProject(previouslySelectedProject);
        return;
      }
      // This project is not accessible anymore
      window.localStorage.removeItem(SELECTED_PROJECT_LOCAL_STORAGE_KEY);
    }
    const userProject = projects.find((project) => project.id === user.id);
    if (!userProject) return;
    setActiveProject(userProject);
    window.localStorage.setItem(
      SELECTED_PROJECT_LOCAL_STORAGE_KEY,
      userProject.id
    );
  }, [activeProject, setActiveProject, projects, user]);

  useEffect(() => {
    // Check whether external authentication is activated
    authApi
      .oauthEnabled()
      .then((res) => {
        setOauthEnabled(res === '1');
      })
      .catch(() => {
        setOauthEnabled(false);
      });
  }, [setOauthEnabled]);

  // TODO: Load project specific extensions
  useEffect(() => {
    if (!isAuthenticated) return;
    extensionsApi
      .listExtensions('ctxy-global')
      .then((res) => setProjectExtensions(res))
      .catch(() => {});
  }, [isAuthenticated, setProjectExtensions]);

  useEffect(() => {
    setAdditionalAppDrawerItems(
      projectExtensions.map((extension) => mapExtensionToAppPage(extension))
    );
  }, [projectExtensions]);

  const [appDrawerElement, setAppDrawerElement] = useState(false);

  useEffect(() => {
    const newState =
      !isAuthenticated || !user ? (
        false
      ) : (
        <AppDrawer
          isAdmin={user.is_admin}
          open={isDrawerOpen}
          additionalPages={additionalAppDrawerItems}
          handleDrawerClose={onDrawerClick}
        />
      );
    setAppDrawerElement(newState);
  }, [
    isAuthenticated,
    setAppDrawerElement,
    additionalAppDrawerItems,
    isDrawerOpen,
    onDrawerClick,
    user,
  ]);

  return (
    <div className="App">
      <AppBar isAuthenticated={isAuthenticated} onDrawerOpen={onDrawerClick} />
      {appDrawerElement}
      <main className="main">
        <ContentContainer
          isAuthenticated={isAuthenticated}
          additionalPages={additionalAppDrawerItems}
        />
      </main>
      <div id="snackbar-container" />
    </div>
  );
}

export default App;

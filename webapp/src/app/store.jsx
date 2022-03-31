// import React, { createContext, useReducer } from 'react';
import { createContainer } from 'unstated-next';
import { useCallback, useState } from 'react';

import { projectsApi, usersApi } from '../services/contaxy-api';

// const initialState = {};
// const store = createContext(initialState);
// const { Provider } = store;

// const StateProvider = ({ children }) => {
// const [state, dispatch] = useReducer((prevState, action) => {
//   switch (action.type) {
//     case 'action description': {
//       const newState = { foo: 'foo' }; // do something with the action
//       return newState;
//     }
//     default: {
//       throw new Error();
//     }
//   }
// }, initialState);
// return <Provider value={{ state, dispatch }}>{children}</Provider>;
// };

// function useContainer() {
//   let value = React.useContext(store);
//   return value;
// }

// const container = {
//   Provider: <Provider value={{ state, dispatch }}>{children}</Provider>,
//   useContainer:
// };

// export { store, StateProvider };

export const initialState = {
  user: null,
  activeProject: {},
  projects: [
    // {
    //   id: 'foobar',
    //   name: 'foobar',
    //   description: '',
    //   creator: 'admin',
    //   visibility: 'private',
    //   createdAt: 1606470094642,
    //   metadata: {
    //     fileNumber: 2,
    //     serviceNumber: 3,
    //   },
    // },
    // {
    //   id: 'ml-lab-demo',
    //   name: 'ml-lab-demo',
    //   description: '',
    //   creator: 'admin',
    //   visibility: 'private',
    //   createdAt: 1607439288065,
    //   metadata: {
    //     fileNumber: 9,
    //     serviceNumber: 4,
    //   },
    // },
  ],
  projectExtensions: [],
  // Check if the ctxy_authorized_user cookie is set which means the user is already logged in
  isAuthenticated: document.cookie.includes('ctxy_authorized_user='),
  oauthEnabled: false,
  users: null,
};

const useGlobalState = (_initialState) => {
  const state = _initialState || initialState;

  const [user, setUser] = useState(state.user);
  const [activeProject, setActiveProject] = useState(state.activeProject);
  const [projects, setProjects] = useState(state.projects);
  const [isAuthenticated, setIsAuthenticated] = useState(state.isAuthenticated);
  const [users, setUsers] = useState(state.users);
  const [oauthEnabled, setOauthEnabled] = useState(state.oauthEnabled);
  const [projectExtensions, setProjectExtensions] = useState(
    state.projectExtensions
  );

  // cache users call so that it is lazy loaded upon first use
  const getUsers = () => {
    if (users) return users;
    usersApi
      .listUsers()
      .then((loadedUsers) => setUsers(loadedUsers))
      .catch(() => setUsers([]));
    return [];
  };

  const userId = user ? user.id : '';
  const loadProjects = useCallback(async () => {
    try {
      let listedProjects = await projectsApi.listProjects();
      const userProject = await projectsApi.getProject(userId);
      if (userProject) {
        listedProjects = listedProjects.filter(
          (project) => project.id !== userProject.id
        );
        listedProjects = [userProject, ...listedProjects];
      }

      setProjects(listedProjects);
    } catch (err) {
      setProjects([]);
    }
  }, [userId]);

  return {
    user,
    setUser,
    activeProject,
    setActiveProject,
    projects,
    setProjects,
    loadProjects,
    isAuthenticated,
    setIsAuthenticated,
    // users,
    // setUsers,
    getUsers,
    oauthEnabled,
    setOauthEnabled,
    projectExtensions,
    setProjectExtensions,
  };
};

export default createContainer(useGlobalState);

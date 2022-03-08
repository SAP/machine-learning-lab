import { ENDPOINT } from './config';
import Files from '../pages/Files';
import Iframe from '../pages/Iframe';
import Jobs from '../pages/Jobs';
import Login from '../pages/Login';
import Projects from '../pages/Projects';
import Services from '../pages/Services';
import UserManagement from '../pages/UserManagement/UserManagement';

export const APP_DRAWER_ITEM_TYPES = {
  divider: 'divider',
  link: 'link',
};

/**
 * The icons are from https://material.io/tools/icons/?style=baseline and embeded via material-ui/icons.
 */
const pages = [
  {
    ICON: 'home',
    NAME: 'Home',
    PATH: '/',
    REQUIRE_LOGIN: true,
    APP_DRAWER_ITEM: true,
    NEW_TAB_OPTION: false,
    TYPE: APP_DRAWER_ITEM_TYPES.link,
    COMPONENT: Projects,
    DISPLAY_PRIORITY: 100,
  },
  {
    NAME: 'project-specific-divider',
    APP_DRAWER_ITEM: true,
    TYPE: APP_DRAWER_ITEM_TYPES.divider,
    DISPLAY_PRIORITY: 90,
  },
  {
    ICON: 'folder',
    NAME: 'Datasets',
    PATH: '/datasets',
    REQUIRE_LOGIN: true,
    APP_DRAWER_ITEM: true,
    TYPE: APP_DRAWER_ITEM_TYPES.link,
    COMPONENT: Files,
    PROPS: {
      folder: 'datasets',
      uploadNote: window.env && window.env.UPLOAD_NOTE,
    },
    DISPLAY_PRIORITY: 70,
  },
  {
    ICON: 'layers',
    NAME: 'Models',
    PATH: '/models',
    REQUIRE_LOGIN: true,
    APP_DRAWER_ITEM: true,
    TYPE: APP_DRAWER_ITEM_TYPES.link,
    COMPONENT: Files,
    PROPS: {
      folder: 'models',
      uploadNote: window.env && window.env.UPLOAD_NOTE,
    },
    DISPLAY_PRIORITY: 70,
  },

  {
    NAME: 'admin-specific-divider',
    APP_DRAWER_ITEM: true,
    REQUIRE_ADMIN: true,
    TYPE: APP_DRAWER_ITEM_TYPES.divider,
    DISPLAY_PRIORITY: 30,
  },
  {
    ICON: 'people',
    NAME: 'User Management',
    PATH: '/user-management',
    REQUIRE_LOGIN: true,
    REQUIRE_ADMIN: true,
    APP_DRAWER_ITEM: true,
    TYPE: APP_DRAWER_ITEM_TYPES.link,
    COMPONENT: UserManagement,
    DISPLAY_PRIORITY: 20,
  },
  {
    NAME: 'login',
    PATH: '/login',
    REQUIRE_LOGIN: false,
    APP_DRAWER_ITEM: false,
    TYPE: APP_DRAWER_ITEM_TYPES.link,
    COMPONENT: Login,
  },
  // TODO: remove this hard-coded embedding of Expyriments
  // {
  //   ICON: 'data_usage',
  //   NAME: 'Expyriments',
  //   PATH: '/iframe',
  //   REQUIRE_LOGIN: true,
  //   APP_DRAWER_ITEM: true,
  //   TYPE: APP_DRAWER_ITEM_TYPES.link,
  //   COMPONENT: Iframe,
  //   PROPS: {
  //     url: 'http://localhost:8081/?appbar=false',
  //     projectSpecific: true,
  //   },
  // },
  // {
  //   ICON: 'data_usage',
  //   NAME: 'Login',
  //   PATH: '/login-test',
  //   REQUIRE_LOGIN: true,
  //   APP_DRAWER_ITEM: true,
  //   TYPE: APP_DRAWER_ITEM_TYPES.link,
  //   COMPONENT: Iframe,
  //   PROPS: {
  //     url: 'http://localhost:8000/login?connector_id=local',
  //     projectSpecific: false,
  //   },
  // },
];

if (!window.env.HIDE_DEPLOYMENTS) {
  pages.push({
    ICON: 'apps',
    NAME: 'Services',
    PATH: '/services',
    REQUIRE_LOGIN: true,
    APP_DRAWER_ITEM: true,
    TYPE: APP_DRAWER_ITEM_TYPES.link,
    COMPONENT: Services,
    DISPLAY_PRIORITY: 50,
  });
  pages.push({
    ICON: 'next_week',
    NAME: 'Jobs',
    PATH: '/jobs',
    REQUIRE_LOGIN: true,
    APP_DRAWER_ITEM: true,
    TYPE: APP_DRAWER_ITEM_TYPES.link,
    COMPONENT: Jobs,
    DISPLAY_PRIORITY: 40,
  });
}

export default pages;
export const mapExtensionToAppPage = (extension) => {
  const backend = new URL(ENDPOINT).origin;
  return {
    ICON: extension.icon ? extension.icon : 'data_usage',
    DISPLAY_PRIORITY: extension.metadata.display_priority
      ? extension.metadata.display_priority
      : 50,
    NAME: extension.display_name
      ? extension.display_name
      : extension.parameters.CONTAXY_DEPLOYMENT_NAME,
    PATH: `/${extension.parameters.CONTAXY_DEPLOYMENT_NAME}`,
    REQUIRE_LOGIN: true,
    APP_DRAWER_ITEM: true,
    TYPE: APP_DRAWER_ITEM_TYPES.link,
    COMPONENT: Iframe,
    PROPS: {
      url: backend + extension.ui_extension_endpoint,
      projectSpecific: true,
    },
  };
};

import Files from '../pages/Files/Files';
import Iframe from '../pages/Iframe';
import Jobs from '../pages/Jobs';
import Login from '../pages/Login';
import Projects from '../pages/Projects';
import Services from '../pages/Services';

export const APP_DRAWER_ITEM_TYPES = {
  divider: 'divider',
  link: 'link',
};

/**
 * The icons are from https://material.io/tools/icons/?style=baseline and embeded via material-ui/icons.
 */
export default [
  {
    ICON: 'home',
    NAME: 'Home',
    PATH: '/',
    REQUIRE_LOGIN: true,
    APP_DRAWER_ITEM: true,
    NEW_TAB_OPTION: false,
    TYPE: APP_DRAWER_ITEM_TYPES.link,
    COMPONENT: Projects,
  },
  {
    NAME: 'project-specific-divider',
    APP_DRAWER_ITEM: true,
    TYPE: APP_DRAWER_ITEM_TYPES.divider,
  },
  {
    ICON: 'folder',
    NAME: 'Files',
    PATH: '/files',
    REQUIRE_LOGIN: true,
    APP_DRAWER_ITEM: true,
    TYPE: APP_DRAWER_ITEM_TYPES.link,
    COMPONENT: Files,
  },
  {
    ICON: 'apps',
    NAME: 'Services',
    PATH: '/services',
    REQUIRE_LOGIN: true,
    APP_DRAWER_ITEM: true,
    TYPE: APP_DRAWER_ITEM_TYPES.link,
    COMPONENT: Services,
  },
  {
    ICON: 'next_week',
    NAME: 'Jobs',
    PATH: '/jobs',
    REQUIRE_LOGIN: true,
    APP_DRAWER_ITEM: true,
    TYPE: APP_DRAWER_ITEM_TYPES.link,
    COMPONENT: Jobs,
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

export const mapExtensionToAppPage = (extension) => {
  return {
    // TODO: make ICON dynamic
    ICON: 'data_usage',
    NAME: extension.parameters.CONTAXY_DEPLOYMENT_NAME,
    PATH: `/${extension.parameters.CONTAXY_DEPLOYMENT_NAME}`,
    REQUIRE_LOGIN: true,
    APP_DRAWER_ITEM: true,
    TYPE: APP_DRAWER_ITEM_TYPES.link,
    COMPONENT: Iframe,
    PROPS: {
      url: extension.ui_extension_endpoint,
      projectSpecific: true,
    },
  };
};

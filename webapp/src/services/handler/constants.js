const ENDPOINTS = {
  // when started via npm, the environment variable REACT_APP_LAB_ENDPOINT is set in the package.json. This is for debugging when you just make changes in the web page.
  labApi:
    process.env.REACT_APP_LAB_ENDPOINT === undefined
      ? "/api"
      : process.env.REACT_APP_LAB_ENDPOINT,
  newLabApi:
    process.env.REACT_APP_LAB_ENDPOINT === undefined
      ? (document.location.origin.toString() + document.location.pathname.toString()).replace("/app/", "/") //""
      : process.env.REACT_APP_LAB_ENDPOINT.replace("/api", "")
};

const SERVICES = {
  research: {
    name: "research-workspace",
    url: ENDPOINTS.newLabApi + "workspace"
  },
  userWorkspace: {
    name: "user-workspace",
    url: ENDPOINTS.newLabApi + "workspace/id/{user}"
  },
  serviceAdmin: {
    name: "service-admin",
    url: ENDPOINTS.newLabApi + "service-admin"
  },
  monitoringDashboard: {
    name: "monitoring-dashboard",
    url: ENDPOINTS.newLabApi + "netdata"
  },
  documentation: {
    name: "documentation",
    url: ENDPOINTS.newLabApi + "docs/"
  },
  apiExplorer: {
    name: "api-explorer",
    url: ENDPOINTS.newLabApi + "api-docs/"
  }
};

/**
 * The icons are from https://material.io/tools/icons/?style=baseline and embeded via material-ui/icons.
 */
const NAVBAR_ITEMS = [
  {
    ICON: "home",
    NAME: "Home",
    PATH: "/",
    NEW_TAB_OPTION: false,
    NEW_TAB_LINK: "",
    PROJECT_SPECIFIC: false,
    TYPE: "link"
  },
  {
    ICON: "code",
    NAME: "Workspace",
    PATH: "/workspace",
    NEW_TAB_OPTION: false,
    NEW_TAB_LINK: "", //SERVICES.research
    PROJECT_SPECIFIC: false,
    TYPE: "link"
  },
  {
    ICON: "developer_board",
    NAME: "Admin",
    PATH: "/management",
    NEW_TAB_OPTION: false,
    NEW_TAB_LINK: "",
    PROJECT_SPECIFIC: false,
    TYPE: "link",
    REQUIRE_ADMIN: true
  },
  {
    ICON: "settings_applications",
    NAME: "Service Admin",
    PATH: "/admin",
    NEW_TAB_OPTION: true,
    NEW_TAB_LINK: SERVICES.serviceAdmin,
    PROJECT_SPECIFIC: false,
    TYPE: "link",
    REQUIRE_ADMIN: true
  },
  {
    TYPE: "divider",
    NAME: "project-specific-divider"
  },
  {
    ICON: "folder",
    NAME: "Datasets",
    PATH: "/datasets",
    NEW_TAB_OPTION: false,
    NEW_TAB_LINK: "",
    PROJECT_SPECIFIC: false,
    TYPE: "link"
  },
  {
    ICON: "equalizer",
    NAME: "Experiments",
    PATH: "/experiments",
    NEW_TAB_OPTION: false,
    NEW_TAB_LINK: "",
    PROJECT_SPECIFIC: true,
    TYPE: "link"
  },
  {
    ICON: "layers",
    NAME: "Models",
    PATH: "/models",
    NEW_TAB_OPTION: false,
    NEW_TAB_LINK: "",
    PROJECT_SPECIFIC: false,
    TYPE: "link"
  },
  {
    ICON: "apps",
    NAME: "Services",
    PATH: "/services",
    NEW_TAB_OPTION: false,
    NEW_TAB_LINK: "",
    PROJECT_SPECIFIC: false,
    TYPE: "link"
  },
  {
    ICON: "next_week",
    NAME: "Jobs",
    PATH: "/jobs",
    NEW_TAB_OPTION: false,
    NEW_TAB_LINK: "",
    PROJECT_SPECIFIC: false,
    TYPE: "link"
  }
];

const WIDGET_ITEMS_EXPERIMENTS = [
  {
    COLOR: "cyan",
    ICON: "autorenew",
    NAME: "Running",
    KEY: "running",
    VALUE: "",
    FORMAT: "running",
    PATH: null
  },
  {
    COLOR: "light-green",
    ICON: "check",
    NAME: "Succeeded",
    VALUE: "",
    KEY: "completed",
    FORMAT: "numb",
    PATH: null
  },
  {
    COLOR: "pink",
    ICON: "error",
    NAME: "Failed",
    VALUE: "",
    KEY: "failed",
    FORMAT: "failed",
    PATH: null
  },
  {
    COLOR: "green",
    ICON: "build",
    NAME: "Last run",
    VALUE: "",
    KEY: "lastRun",
    FORMAT: "date",
    PATH: null
  }
];

const WIDGET_ITEMS_DASHBOARD = [
  {
    COLOR: "light-green",
    ICON: "folder",
    NAME: "Datasets",
    VALUE: "",
    KEY: "datasetsCount",
    FORMAT: "numb",
    PATH: "/datasets"
  },
  {
    COLOR: "cyan",
    ICON: "layers",
    NAME: "Models",
    VALUE: "",
    KEY: "modelsCount",
    FORMAT: "numb",
    PATH: "/models"
  },
  {
    COLOR: "pink",
    ICON: "equalizer",
    NAME: "Experiments",
    VALUE: "",
    KEY: "experimentsCount",
    FORMAT: "numb",
    PATH: "/experiments"
  },
  {
    COLOR: "orange",
    ICON: "apps",
    NAME: "Services",
    VALUE: "",
    KEY: "servicesCount",
    FORMAT: "numb",
    PATH: "/services"
  }
];

const WIDGET_ITEMS_DATASETS = [
  {
    COLOR: "pink",
    ICON: "list",
    NAME: "Datasets",
    VALUE: "",
    KEY: "filesCount",
    FORMAT: "numb",
    PATH: null
  },
  {
    COLOR: "cyan",
    ICON: "cloud",
    NAME: "Total size",
    VALUE: "",
    KEY: "filesTotalSize",
    FORMAT: "size",
    PATH: null
  },
  {
    COLOR: "light-green",
    ICON: "build",
    NAME: "Last modified",
    VALUE: "",
    KEY: "lastModified",
    FORMAT: "date",
    PATH: null
  }
];

const WIDGET_ITEMS_MODELS = [
  {
    COLOR: "pink",
    ICON: "list",
    NAME: "Models",
    VALUE: "",
    KEY: "filesCount",
    FORMAT: "numb",
    PATH: null
  },
  {
    COLOR: "cyan",
    ICON: "cloud",
    NAME: "Total size",
    VALUE: "",
    KEY: "filesTotalSize",
    FORMAT: "size",
    PATH: null
  },
  {
    COLOR: "light-green",
    ICON: "build",
    NAME: "Last modified",
    VALUE: "",
    KEY: "lastModified",
    FORMAT: "date",
    PATH: null
  }
];

const WIDGET_ITEMS_JOBS = [
  {
    COLOR: "cyan",
    ICON: "loop",
    NAME: "Running",
    VALUE: "",
    KEY: "running",
    FORMAT: "numb",
    PATH: null
  },
  {
    COLOR: "light-green",
    ICON: "done",
    NAME: "Succeeded",
    VALUE: "",
    KEY: "succeeded",
    FORMAT: "numb",
    PATH: null
  },
  {
    COLOR: "pink",
    ICON: "error",
    NAME: "Failed",
    VALUE: "",
    KEY: "failed",
    FORMAT: "numb",
    PATH: null
  }
];

const DATASET_TABLE_COLUMNS = [
  {
    id: "name",
    numeric: false,
    label: "Dataset",
    type: "truncate",
    disablePadding: true
  },
  {
    id: "modifiedAt",
    numeric: false,
    label: "Last modified",
    type: "date",
    disablePadding: true
  },
  {
    id: "modifiedBy",
    numeric: false,
    label: "Modified by",
    type: "truncate",
    disablePadding: true
  },
  {
    id: "version",
    numeric: false,
    label: "Version",
    type: "default",
    disablePadding: true
  },
  {
    id: "size",
    numeric: false,
    label: "Size",
    type: "size",
    disablePadding: true
  }
];

const MODEL_TABLE_COLUMNS = [
  {
    id: "name",
    numeric: false,
    label: "Model",
    type: "truncate",
    disablePadding: true
  },
  {
    id: "version",
    numeric: false,
    label: "Version",
    type: "default",
    disablePadding: true
  },
  {
    id: "modifiedBy",
    numeric: false,
    label: "Modified by",
    type: "truncate",
    disablePadding: true
  },
  {
    id: "size",
    numeric: false,
    label: "Size",
    type: "size",
    disablePadding: true
  },
  {
    id: "modifiedAt",
    numeric: false,
    label: "Last modified",
    type: "date",
    disablePadding: true
  }
];

const JOB_TABLE_COLUMNS = [
  {
    id: "name",
    numeric: false,
    label: "Job",
    type: "truncate",
    disablePadding: true
  },
  {
    id: "status",
    numeric: false,
    label: "Status",
    type: "default",
    disablePadding: true
  },
  {
    id: "startedAt",
    numeric: false,
    label: "Started at",
    type: "date",
    disablePadding: true
  },
  {
    id: "finishedAt",
    numeric: false,
    label: "Finished at",
    type: "date",
    disablePadding: true
  }
];

const JOB_SCHEDULED_TABLE_COLUMNS = [
  {
    id: "jobName",
    numeric: false,
    label: "Name",
    type: "truncate",
    disablePadding: true
  },
  {
    id: "dockerImage",
    numeric: false,
    label: "Image",
    type: "truncate",
    disablePadding: true
  },
  {
    id: "schedule",
    numeric: false,
    label: "Schedule",
    type: "truncate",
    disablePadding: true
  },
  {
    id: "lastExecution",
    numeric: false,
    label: "Last executed",
    type: "date",
    disablePadding: true
  },
  {
    id: "addedAt",
    numeric: false,
    label: "Added at",
    type: "date",
    disablePadding: true
  }
];

const COOKIES = {
  project: "project",
  options: {
    path: "/"
  },
  firstTimeLogin: "first_time_login"
};

const LOADING_TOAST_OPTIONS = {
  autoClose: false,
  hideProgressBar: true,
  position: "bottom-center",
  pauseOnHover: false,
  closeButton: false,
  closeOnClick: false
};

const SERVICE_NAME_REGEX = new RegExp(
  "^([a-zA-Z0-9][a-zA-Z0-9-]+[a-zA-Z0-9])?$"
);
const ENV_NAME_REGEX = new RegExp("^([a-zA-Z_]{1,}[a-zA-Z0-9_]{0,})?$");

const GITHUB_LINK = "https://github.com"; // TODO: change
const ABOUT_FILE = "./about.txt";

export {
  NAVBAR_ITEMS,
  WIDGET_ITEMS_DASHBOARD,
  WIDGET_ITEMS_EXPERIMENTS,
  WIDGET_ITEMS_DATASETS,
  WIDGET_ITEMS_MODELS,
  WIDGET_ITEMS_JOBS,
  DATASET_TABLE_COLUMNS,
  MODEL_TABLE_COLUMNS,
  JOB_TABLE_COLUMNS,
  JOB_SCHEDULED_TABLE_COLUMNS,
  SERVICES,
  ENDPOINTS,
  COOKIES,
  LOADING_TOAST_OPTIONS,
  SERVICE_NAME_REGEX,
  ENV_NAME_REGEX,
  GITHUB_LINK,
  ABOUT_FILE
};

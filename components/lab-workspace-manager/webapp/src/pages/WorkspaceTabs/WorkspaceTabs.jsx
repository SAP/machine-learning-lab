import React, { useCallback, useState } from 'react';

import { useParams } from 'react-router-dom';
import PropTypes from 'prop-types';
import ReactIframe from 'react-iframe';
import Tab from '@material-ui/core/Tab';
import Tabs from '@material-ui/core/Tabs';

import './WorkspaceTabs.css';
import { CONTAXY_ENDPOINT } from '../../utils/config';
import { useWorkspaces } from '../../services/api-hooks';
import WorkspaceOverview from '../../components/WorkspaceOverview';

function TabPanel(props) {
  const { children, selectedTabIndex, myTabIndex } = props;

  return (
    <div
      role="tabpanel"
      hidden={selectedTabIndex !== myTabIndex}
      className="TabPanel"
    >
      {children}
    </div>
  );
}
TabPanel.propTypes = {
  children: PropTypes.node.isRequired,
  selectedTabIndex: PropTypes.number.isRequired,
  myTabIndex: PropTypes.number.isRequired,
};

function WorkspaceTabs() {
  // Get the id of the user for which the workspace manager should be shown
  const { userId } = useParams();
  // Request list of workspaces
  const [workspaces, reloadWorkspaces] = useWorkspaces(userId);
  // Setup state for selected tab
  const [selectedTab, setSelectedTab] = useState(0);

  const accessWorkspace = useCallback(
    (workspace) => {
      setSelectedTab(workspaces.indexOf(workspace) + 1);
    },
    [workspaces]
  );

  const workspaceTabs = workspaces.map((workspace) => {
    let title = workspace.display_name || workspace.id;
    // Remove ws- prefix
    if (title.startsWith('ws-')) {
      title = title.substr(3);
    }
    return <Tab key={workspace.id} label={title} />;
  });
  const workspacePanels = workspaces.map((workspace, i) => {
    const projectName = workspace.metadata['ctxy.projectName'];
    const deploymentName = workspace.metadata['ctxy.deploymentName'];
    const backend = CONTAXY_ENDPOINT.replace(/\/api$/, '');
    return (
      <TabPanel
        key={workspace.display_name}
        selectedTabIndex={selectedTab}
        myTabIndex={i + 1}
      >
        {workspace.status === 'running' ? (
          <ReactIframe
            key={workspace.display_name}
            url={`${backend}/projects/${projectName}/services/${deploymentName}/access/8080b`}
            allowFullScreen
            className="iframe"
          />
        ) : (
          <div>Workspace is loading...</div>
        )}
      </TabPanel>
    );
  });
  return (
    <div className="WorkspaceTabs">
      <Tabs
        value={selectedTab}
        onChange={(event, newSelectedTab) => setSelectedTab(newSelectedTab)}
        className="tab-bar"
      >
        <Tab label="Overview" />
        {workspaceTabs}
      </Tabs>
      <TabPanel selectedTabIndex={selectedTab} myTabIndex={0}>
        <WorkspaceOverview
          userId={userId}
          workspaces={workspaces}
          reloadWorkspaces={reloadWorkspaces}
          accessWorkspace={accessWorkspace}
        />
      </TabPanel>
      {workspacePanels}
    </div>
  );
}

export default WorkspaceTabs;

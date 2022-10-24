import { useLocation } from 'react-router-dom';
import React, { useEffect, useState } from 'react';

import Stack from '@mui/material/Stack';

import { deployServer, startServer } from '../services/mlflow-server';
import { useServer } from '../services/api-hooks';
import MlFlowServerStatusPending from '../components/MlFlowServerStatusPending';
import MlFlowServerStatusStopped from '../components/MlFlowServerStatusStopped';

import { CONTAXY_ENDPOINT } from '../utils/config';

// A custom hook that builds on useLocation to parse
// the query string for you.
function useQuery() {
  const { search } = useLocation();

  return React.useMemo(() => new URLSearchParams(search), [search]);
}

function MLFlowServer() {
  const query = useQuery();
  const projectId = query.get('project');

  const [isDeploymentLoading, setIsDeploymentLoading] = useState(false);
  const [isServerLoading, setIsServerLoading] = useState(false);
  const [mlflowServers, reloadMlflowServers] = useServer(projectId);

  useEffect(() => {
    // Deploy default MLflow server if no server is deployed
    if (!isDeploymentLoading && mlflowServers?.length === 0) {
      deployServer(projectId).then(() => {
        setIsDeploymentLoading(false);
        reloadMlflowServers();
      });
      setIsDeploymentLoading(true);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [mlflowServers]);

  useEffect(() => {
    // Continuously update the mlflowServer if no mlflowServer is deployed or deployment is pending
    const mlflowServer = mlflowServers?.length > 0 ? mlflowServers[0] : null;
    if (mlflowServer?.status === 'pending') {
      const interval = setInterval(() => {
        reloadMlflowServers();
      }, 5000);
      return () => clearInterval(interval);
    }
    return () => null;
  }, [isServerLoading, mlflowServers, reloadMlflowServers]);

  let content;
  if (mlflowServers === null || mlflowServers.length === 0) {
    content = null;
  } else {
    // Always show first MLflow server. Multiple MLflow servers are not implemented for now.
    const mlflowServer = mlflowServers[0];
    if (isServerLoading || mlflowServer.status === 'pending') {
      content = <MlFlowServerStatusPending className="mlflow-server-content" />;
    } else if (mlflowServer.status === 'stopped') {
      content = (
        <MlFlowServerStatusStopped
          className="mlflow-server-content"
          onStartMlFlowServer={() => {
            startServer(projectId, mlflowServer.id).finally(() => {
              setTimeout(() => {
                setIsServerLoading(false);
                reloadMlflowServers();
              }, 10000);
            });
            setIsServerLoading(true);
          }}
        />
      );
    } else if (mlflowServer.status === 'running') {
      const backend = CONTAXY_ENDPOINT.replace(/\/api$/, '');
      content = (
        <iframe
          title="MLFlow Server"
          key={mlflowServer.display_name}
          src={`${backend}${mlflowServer.access_url}`}
          allowFullScreen
          className="iframe"
          height="100%"
          width="100%"
          frameBorder="0"
        />
      );
    }
  }
  return (
    <Stack sx={{ height: '100%' }} justifyContent="center" alignItems="center">
      {content}
    </Stack>
  );
}

export default MLFlowServer;

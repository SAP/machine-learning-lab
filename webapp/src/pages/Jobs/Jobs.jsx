import React, { useCallback, useMemo } from 'react';

import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import Button from '@material-ui/core/Button';

import { useJobs } from '../../services/api-hooks';
import { useShowAppDialog } from '../../app/AppDialogServiceProvider';
import DeployServiceDialog from '../../components/Dialogs/DeployContainerDialog';
// import Widget from '../components/Widget';
// import WidgetsGrid from '../components/WidgetsGrid';
import { jobsApi } from '../../services/contaxy-api';
import ContentDialog from '../../components/Dialogs/ContentDialog';
import GlobalStateContainer from '../../app/store';
import JobsContainer from './JobsContainer';
import LogsDialog from '../../components/Dialogs/LogsDialog';
import showStandardSnackbar from '../../app/showStandardSnackbar';

function Jobs(props) {
  const { className } = props;
  const { t } = useTranslation();
  const { activeProject } = GlobalStateContainer.useContainer();
  const showAppDialog = useShowAppDialog();
  const [jobs, reloadJobs] = useJobs(activeProject.id);

  const onJobDeploy = () => {
    showAppDialog(DeployServiceDialog, {
      onDeploy: async (
        {
          containerImage,
          deploymentName,
          deploymentParameters,
          deploymentEndpoints,
          minCpus,
          minMemory,
        },
        onClose
      ) => {
        const jobInput = {
          container_image: containerImage,
          display_name: deploymentName,
          endpoints: deploymentEndpoints,
          parameters: deploymentParameters,
          compute: { min_cpus: minCpus, min_memory: minMemory },
        };
        try {
          await await jobsApi.deployJob(activeProject.id, jobInput);
          showStandardSnackbar(`Deployed job '${deploymentName}'`);
          onClose();
          reloadJobs();
        } catch (err) {
          showStandardSnackbar(`Could not deploy service '${deploymentName}'.`);
        }
      },
    });
  };

  const onShowJobMetadata = useCallback(
    async (projectId, jobId) => {
      try {
        const metadata = await jobsApi.getJobMetadata(projectId, jobId);
        showAppDialog(ContentDialog, {
          jsonContent: metadata,
          title: 'Job Metadata',
        });
      } catch (err) {
        showStandardSnackbar('Could not load Job metadata');
      }
    },
    [showAppDialog]
  );

  const onShowJobLogs = useCallback(
    async (projectId, jobId) => {
      try {
        showAppDialog(LogsDialog, {
          title: 'Logs',
          projectId,
          id: jobId,
          type: 'job',
        });
      } catch (err) {
        showStandardSnackbar('Could not load logs');
      }
    },
    [showAppDialog]
  );

  const onJobDelete = useCallback(
    async (projectId, jobId) => {
      try {
        await jobsApi.deleteJob(projectId, jobId);
        showStandardSnackbar(`Deleted job '${jobId}'`);
        reloadJobs();
      } catch (err) {
        showStandardSnackbar(`Could not delete job '${jobId}'`);
      }
    },
    [reloadJobs]
  );

  // TODO: set correct values in Widgets
  const jobsContainer = useMemo(
    () => (
      <JobsContainer
        data={jobs}
        onReload={reloadJobs}
        onJobDelete={(rowData) => onJobDelete(activeProject.id, rowData.id)}
        onShowJobActions={() => {}}
        onShowJobLogs={(rowData) => onShowJobLogs(activeProject.id, rowData.id)}
        onShowJobMetadata={(rowData) =>
          onShowJobMetadata(activeProject.id, rowData.id)
        }
      />
    ),
    [
      activeProject.id,
      jobs,
      onJobDelete,
      // onShowJobActions,
      onShowJobLogs,
      onShowJobMetadata,
      reloadJobs,
    ]
  );

  return (
    <div className="pages-native-component">
      {/* <WidgetsGrid>
        <Widget name="Running" icon="loop" value="2" color="cyan" />
        <Widget name="Succeeded" icon="done" value="2" color="light-green" />
        <Widget name="Failed" icon="done" value="2" color="pink" />
      </WidgetsGrid> */}
      <Button
        variant="contained"
        color="primary"
        onClick={onJobDeploy}
        className={`${className} button`}
      >
        {`${t('run')} ${t('job')}`}
      </Button>
      {jobsContainer}
    </div>
  );
}

Jobs.propTypes = {
  className: PropTypes.string,
};

Jobs.defaultProps = {
  className: '',
};

const StyledJobs = styled(Jobs)`
  &.button {
    margin: 8px 0px;
  }
`;

export default StyledJobs;

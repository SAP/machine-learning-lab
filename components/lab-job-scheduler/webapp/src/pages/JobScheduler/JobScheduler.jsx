import { useLocation } from 'react-router-dom';
import Button from '@material-ui/core/Button';
import React from 'react';

import { useShowAppDialog } from '../../components/AppDialogServiceProvider';
import ScheduleJobDialog from '../../components/ScheduleJobDialog';
import ScheduledJobsContainer from './ScheduledJobsContainer';
import showStandardSnackbar from '../../components/showStandardSnackbar';

import scheduleJob from '../../services/job-sceduler-api';

const buttonStyle = {
  margin: '8px 0px',
};

// A custom hook that builds on useLocation to parse
// the query string for you.
function useQuery() {
  const { search } = useLocation();
  return React.useMemo(() => new URLSearchParams(search), [search]);
}

function JobScheduler() {
  const showAppDialog = useShowAppDialog();
  const query = useQuery();
  const projectId = query.get('project');

  const onJobSchedule = () => {
    showAppDialog(ScheduleJobDialog, {
      onDeploy: async (
        {
          containerImage,
          displayName,
          deploymentParameters,
          deploymentEndpoints,
          cronString,
        },
        onClose
      ) => {
        const jobInput = {
          container_image: containerImage,
          display_name: displayName,
          endpoints: deploymentEndpoints,
          parameters: deploymentParameters,
        };
        const scheduleJobInput = {
          cron_string: cronString,
          job_input: jobInput,
        };

        onClose();
        try {
          await scheduleJob(projectId, scheduleJobInput);
          showStandardSnackbar(`Scheduled job '${displayName}'`);
          onClose();
          // reloadJobs();
        } catch (err) {
          showStandardSnackbar(`Could not schedule '${displayName}'.`);
        }
      },
    });
  };

  return (
    <div className="pages-native-component">
      <Button
        variant="contained"
        color="primary"
        // className="button"
        style={buttonStyle}
        onClick={onJobSchedule}
      >
        Schedule Job
      </Button>
      <ScheduledJobsContainer />
    </div>
  );
}

export default JobScheduler;

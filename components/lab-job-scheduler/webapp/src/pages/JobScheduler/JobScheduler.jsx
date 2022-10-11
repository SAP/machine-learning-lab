import Button from '@material-ui/core/Button';
import React from 'react';

import { useShowAppDialog } from '../../components/AppDialogServiceProvider';
import ScheduleJobDialog from '../../components/ScheduleJobDialog';
import ScheduledJobsContainer from './ScheduledJobsContainer';

const buttonStyle = {
  margin: '8px 0px',
};

function JobScheduler() {
  const showAppDialog = useShowAppDialog();

  const onJobSchedule = () => {
    showAppDialog(ScheduleJobDialog, {
      onDeploy: async (
        {
          containerImage,
          deploymentName,
          deploymentParameters,
          deploymentEndpoints,
        },
        onClose
      ) => {
        const jobInput = {
          container_image: containerImage,
          display_name: deploymentName,
          endpoints: deploymentEndpoints,
          parameters: deploymentParameters,
        };
        console.log(jobInput);
        onClose();
        // try {
        //   await jobSchedulerApi.scheduleJob(activeProject.id, jobInput);
        //   showStandardSnackbar(`Deployed job '${deploymentName}'`);
        //   onClose();
        //   reloadJobs();
        // } catch (err) {
        //   showStandardSnackbar(`Could not deploy service '${deploymentName}'.`);
        // }
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

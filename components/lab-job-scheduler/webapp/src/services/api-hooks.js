/* eslint-disable import/prefer-default-export */
import { useCallback, useEffect, useState } from 'react';

import { getExecutorInfo, listScheduledJobs } from './job-scheduler-api';

function useApiHook(apiCall, condition) {
  const sanitizedCondition = condition !== undefined ? condition : true;

  const [data, setData] = useState([]);
  const [reloadRequested, setReloadRequested] = useState(new Date().getTime());

  const requestReload = useCallback(() => {
    setReloadRequested(new Date().getTime());
  }, []);

  useEffect(() => {
    let isCanceled = false;

    const load = async () => {
      if (!sanitizedCondition) return;
      try {
        const result = await apiCall();
        if (isCanceled) {
          return;
        }
        setData(result);
      } catch (err) {
        // ignore
      }
    };

    load();

    return () => {
      isCanceled = true;
    };
  }, [sanitizedCondition, reloadRequested, apiCall]);

  return [data, requestReload];
}

export function useScheduledJobs(projectId) {
  const apiCall = useCallback(async () => {
    try {
      const jobs = await listScheduledJobs(projectId);
      return jobs;
    } catch (err) {
      return [];
    }
  }, [projectId]);

  const [data, reload] = useApiHook(apiCall, projectId);
  return [data, reload];
}

export function useExecutorInfo() {
  const apiCall = useCallback(async () => {
    try {
      const info = await getExecutorInfo();
      return info;
    } catch (err) {
      return [];
    }
  }, []);

  const [data, reload] = useApiHook(apiCall);
  return [data, reload];
}

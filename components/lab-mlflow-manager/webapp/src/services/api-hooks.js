/* eslint-disable import/prefer-default-export */
import { useCallback, useEffect, useRef, useState } from 'react';
import superagent from 'superagent';

import { EXTENSION_ENDPOINT } from '../utils/config';

function useApiHook(apiCall, condition) {
  const sanitizedCondition = condition !== undefined ? condition : true;

  const [data, setData] = useState(null);
  const [reloadRequested, setReloadRequested] = useState(new Date().getTime());
  const requestRunning = useRef(false);

  const requestReload = useCallback(() => {
    setReloadRequested(new Date().getTime());
  }, []);

  useEffect(() => {
    let isCanceled = false;

    const load = async () => {
      if (requestRunning.current) return;
      if (!sanitizedCondition) return;
      try {
        requestRunning.current = true;
        const result = await apiCall();
        if (isCanceled) {
          return;
        }
        setData(result);
      } catch (err) {
        // ignore
      }
      requestRunning.current = false;
    };

    load();

    return () => {
      isCanceled = true;
    };
  }, [sanitizedCondition, reloadRequested, apiCall]);

  return [data, requestReload];
}

export function useServer(projectId) {
  const apiCall = useCallback(async () => {
    try {
      const response = await superagent
        .get(`${EXTENSION_ENDPOINT}/projects/${projectId}/mlflow-server`)
        .withCredentials();
      return response.body;
    } catch (err) {
      return [];
    }
  }, [projectId]);

  const [servers, reload] = useApiHook(apiCall, projectId);

  return [servers, reload];
}

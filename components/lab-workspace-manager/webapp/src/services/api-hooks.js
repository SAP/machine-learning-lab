/* eslint-disable import/prefer-default-export */
import { useCallback, useEffect, useState } from 'react';
import superagent from 'superagent';

import { EXTENSION_ENDPOINT } from '../utils/config';

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

export function useWorkspaces(userId) {
  const apiCall = useCallback(async () => {
    try {
      const response = await superagent
        .get(`${EXTENSION_ENDPOINT}/users/${userId}/workspace`)
        .withCredentials();
      return response.body;
    } catch (err) {
      return [];
    }
  }, [userId]);

  const [data, reload] = useApiHook(apiCall, userId);
  return [data, reload];
}

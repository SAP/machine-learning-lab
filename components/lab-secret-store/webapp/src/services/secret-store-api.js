/* eslint-disable import/prefer-default-export */
import { useCallback, useEffect, useState } from 'react';
import superagent from 'superagent';

import { EXTENSION_ENDPOINT } from '../utils/config';

function useApiHook(apiCall, condition) {
  const sanitizedCondition = condition !== undefined ? condition : true;

  const [data, setData] = useState(null);
  const [reloadRequested, setReloadRequested] = useState([]);
  // const requestRunning = useRef(false);

  const requestReload = useCallback(() => {
    setReloadRequested(new Date().getTime());
  }, []);

  useEffect(() => {
    let isCanceled = false;

    const load = async () => {
      //      if (requestRunning.current) return;
      if (!sanitizedCondition) return;
      try {
        //    requestRunning.current = true;
        const result = await apiCall();
        if (isCanceled) {
          return;
        }
        setData(result);
      } catch (err) {
        // ignore
      }
      //    requestRunning.current = false;
    };

    load();

    return () => {
      isCanceled = true;
    };
  }, [sanitizedCondition, reloadRequested, apiCall]);

  return [data, requestReload];
}

export function useSecrets(projectId) {
  const apiCall = useCallback(async () => {
    try {
      const response = await superagent
        .get(`${EXTENSION_ENDPOINT}/projects/${projectId}/secrets`)
        .withCredentials();
      return response.body;
    } catch (err) {
      return [];
    }
  }, [projectId]);

  const [workspaces, reload] = useApiHook(apiCall, projectId);

  return [workspaces, reload];
}

export async function getSecretsPassword(projectId, secretId) {
  try {
    const response = await superagent
      .get(`${EXTENSION_ENDPOINT}/projects/${projectId}/secrets/${secretId}`)
      .withCredentials();
    return response.body;
  } catch (err) {
    window.alert('Cloud not recive secret');
    return [];
  }
}

export async function creatSecret(projectId, name, password, methadata) {
  try {
    const response = await superagent
      .post(`${EXTENSION_ENDPOINT}/projects/${projectId}/secrets`)
      .send({ display_name: name, metadata: methadata, secret_text: password })
      .withCredentials();
  } catch (err) {
    window.alert('Cloud not create secret');
  }
}

export async function deleteSecret(projectId, secretId) {
  try {
    const response = await superagent
      .delete(`${EXTENSION_ENDPOINT}/projects/${projectId}/secrets/${secretId}`)
      .withCredentials();
  } catch (err) {
    window.alert('Cloud not delete secret');
  }
}

import { projectsApi, getDefaultApiCallback } from "../../services/client/ml-lab-api";

import * as Parser from "../../services/handler/parser";

/**
 * Method enders different cards containing meta data such as: lastRun, succeeded, running, and failed
 * @param {*} result 
 * @param {*} state 
 */
const fillMetaDataHeader = (result, widgetdata) => {
	let widgetdatan = widgetdata;
	widgetdatan.forEach(function(element) {
		//console.log(result['metadata']['stats']);
		element.VALUE = Parser.SetVariableFormat(result["metadata"]["stats"][element.KEY], element.FORMAT);
		//console.log('element.VALUE', element.VALUE);
	}, this);

	return [...widgetdatan];
};

/**
 * Deletes a specific experiment of a project based on the experimentkey 
 * @param {*} project 
 * @param {*} experimentkey 
 */
export const deleteExperiment = (project, experimentkey) => {
	// console.log('=========>> delete Experiment ');
	return new Promise((resolve, reject) => {
		projectsApi.deleteExperiment(
			project,
			experimentkey,
			{},
			getDefaultApiCallback(
				() => {
					// console.log('+++ deleteExperiment() experiment deleted key:' , experimentkey);
					// console.log('+++ deleteExperiment() experiment deleted result: ', result);
					resolve();
				},
				({ error }) => {
					console.log("experiment deletion failed", error);
					reject();
				}
			)
		);
	});
};

/**
 * Method fetches all experiments of a specific project
 *  - it transforms seconds of "startedAt", "finishedAt" and "updatedAt"
 *  - it refills the MetaDataHeader populated above the dashboard
 * @param {*} currentProject 
 * @param {*} state 
 */
export const getModifiedExperiments = (currentProject, widgetdata) => {
	let project = currentProject;

	if (widgetdata !== undefined) {
		return new Promise((resolve, reject) => {
			projectsApi.getExperiments(
				project,
				{},
				getDefaultApiCallback(
					({ result }) => {
						let metaData = fillMetaDataHeader(result, widgetdata);

						if (result.data !== undefined) {
							let resultDict = { widgetdata: metaData, json: result };
							resolve(resultDict);
						} else {
							reject(Error("No rows have been fetched!  "));
						}
					},
					() => {}
				)
			);
		});
	}
	console.log("--- processExperiments() widgetdata undefined: ", widgetdata);

	// TODO: return empty promise in case of error so that .then does not throw an exception
	return new Promise((resolve, reject) => {
		reject("widget not set");
	});
};

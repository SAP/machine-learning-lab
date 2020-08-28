import React from "react";

import { Button, withStyles } from "@material-ui/core";

import RowDetailPanel from './RowDetailPanel';

export const styles = theme => ({
	root: {
		marginTop: theme.spacing(3)
	},
	successExperiment: {
		color: "#29a329",
		borderColor: "#29a329",
		"&:hover": {
			backgroundColor: "#ebfaeb !important"
		}
	},
	runningExperiment: {
		color: "#115eca",
		borderColor: "#115eca",
		"&:hover": {
			backgroundColor: "#115eca30 !important"
		}
	},
	failedExperiment: {
		color: "#ff3300",
		borderColor: "#ff3300",
		"&:hover": {
			backgroundColor: "#ffebe6 !important"
		}
	},
	deadExperiment: {
		color: "#021e02",
		borderColor: "#021e02",
		"&:hover": {
			backgroundColor: "#00000042 !important"
		}
	},
	interruptedExperiment: {
		color: "#d9bd00",
		borderColor: "#d9bd00",
		"&:hover": {
			backgroundColor: "#d9bd003b !important"
		}
	},
	defaultExperiment: {
		color: "#878786",
		borderColor: "#878786",
		"&:hover": {
			backgroundColor: "#87878617 !important"
		}
	},
	queuedExperiment: {
		color: "#7732e7",
		borderColor: "#7732e7",
		"&:hover": {
			backgroundColor: "#7732e73b !important"
		}
	}
	//Don't remove as we need orange
	// orange: {
	//   color: "#FB8C00",
	//   borderColor: "#FB8C00",
	//   "&:hover": {
	//     backgroundColor: "#fff4e6 !important"
	//   }
	// }
});

/// Used for custom filter
// export const FilterIcon = ({ type, ...restProps }) => {
// 	if (type === "month") return <DateRange {...restProps} />;
// 	return <TableFilterRow.Icon type={type} {...restProps} />;
// };

export const StatusButton = ({ value, classes }) => {
	let classTheme = classes.defaultExperiment;

	switch (value) {
		case "completed":
			classTheme = classes.successExperiment;
			break;
		case "running":
			classTheme = classes.runningExperiment;
			break;
		case "failed":
			classTheme = classes.failedExperiment;
			break;
		case "dead":
			classTheme = classes.deadExperiment;
			break;
		case "interrupted":
			classTheme = classes.interruptedExperiment;
			break;
		case "queued":
			classTheme = classes.queuedExperiment;
			break;
		default:
			classTheme = classes.defaultExperiment;
	}
	return (
		<Button variant="outlined" className={classTheme} style={{ width: "100px" }} size="small">
			<span style={{}}>{value}</span>
		</Button>
	);
};

//FIXME: performance bottleneck
/**
 * This method return the tabs for the expendable row, including the experiment data
 * @param {*} jsonExperiments 
 * @param {*} props 
 */
// TODO: refactor function. From props, which contains a lot of fields, only two fields are used. 
export const RowDetail = (jsonExperiments, props) => {
	let row = props; // TODO: rename parameter to row
	
	if (jsonExperiments.length === 0) {
		return <div />;
	}

	let { key: experimentKey } = row;

	let experimentData;

	jsonExperiments.data.forEach(experiment => {
		if (experiment.key === experimentKey) experimentData = experiment;
	});

	return <RowDetailPanel experiment={experimentData} currentProject={props.currentProject} />;
};

export default withStyles(styles)(StatusButton);

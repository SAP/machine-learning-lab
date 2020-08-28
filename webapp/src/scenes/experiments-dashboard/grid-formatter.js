
export const NumberFormatter = ({ value }) => {
	try {
		if(value !== undefined){
			let metric = value.toString();
			value = metric.substring(0, 8);
			return value;
		}
		return "";
	} catch (error) {
		console.error("value undefined in Number Formatter", value);
		return null;
	}
};

export const DurationFormatter = ({ value }) => {
	if (value !== undefined) {
		value = Math.floor(value / 1000);

		let seconds = value;
		let minutes = Math.floor(seconds / 60);
		let hours = Math.floor(minutes / 60);
		//TODO: I prefer 96:33:12 over 4 days 33:12
		//console.log('hours ', hours, ' minutes ', minutes , ' seconds ', seconds);

		let timeMinutes = (minutes - hours * 60).toString();
		let timeSeconds = (seconds - minutes * 60).toString();
		let timeHours = hours.toString();

		if (timeSeconds.length < 2) timeSeconds = "0" + timeSeconds;
		if (timeMinutes.length < 2) timeMinutes = "0" + timeMinutes;
		if (timeHours.length < 2) timeHours = "0" + timeHours;

		let duration = [timeHours, timeMinutes, timeSeconds].join(":");
		//console.log('duration:', duration);
		value = duration;
		return value;
	}

	return null;
};

export const secondsToDateAndTime = value => {
	//check if number because of running experiments which do not provide any number at all
	if (typeof value === "number") {
		let d = new Date(value);
		let month = "" + (d.getMonth() + 1);
		let day = "" + d.getDate();
		let year = d.getFullYear();
		let hours = "" + d.getHours();
		let minutes = "" + d.getMinutes();

		if (month.length < 2) month = "0" + month;
		if (day.length < 2) day = "0" + day;
		if (minutes.length < 2) minutes = "0" + minutes;
		//console.log(minutes.length);

		if (hours.length < 2) hours = "0" + hours;

		let date = [year, month, day].join("-");
		//console.log(date);

		date = date + " " + [hours, minutes].join(":");

		value = date.toString();
		//console.log('date',date);
		return value;
	}
	// console.log('empty value', value)
	// If the column does not exist the value is meant for, the value is undefined. We could give " " back here, but the key should be filled in the backend so the value would be default a string or similar
	return value;
};

/**
 * Checks if string contains NOT any special character
 * @param {String} str
 */
const isValid = str => {
	return !(/[_~`!#$%\^&*+=\-\[\]\\';,/{}|\\":<>\?]/g.test(str));
};

/**
 * Looks for camelCased string, makes the first char upper case and adds a whitespace between camelCased style.
 * Checks for validity of the string (adding no whitespace in case of special characters between words)
 * @param {String} childname
 */
export const makeUpperCaseAndAddWhitespace = childname => {
	if (childname === "undefined") {
		return "";
	}
	let character = "";
	let newColumn = "";
	
	for (let i = 0; i < childname.length; i++) {
		character = childname.charAt(i);

		if (character.toUpperCase() === character && isValid(character)) {
			newColumn = newColumn + " " + character;
		} else {
			newColumn = newColumn + character;
		}
	}
	return newColumn;
};

export const makeAllUpperCaseAndAddWhitespace = str => {
	return makeUpperCaseAndAddWhitespace(str).toUpperCase();
}

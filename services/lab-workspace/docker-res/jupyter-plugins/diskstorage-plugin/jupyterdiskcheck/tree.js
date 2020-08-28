define(["base/js/namespace", "jquery", "base/js/dialog", "base/js/utils", "require", "./mydialog", "./jsutils"], function(Jupyter, $, dialog, utils, require, myDialog, jsUtils) {
	// ----------- DIALOGS -------------------------------

	var mydialog = require("./mydialog");
	var mydialog = new myDialog();

	var jsutils = require("./jsutils");
	var jsutils = new jsUtils();
	// -------- GLOBAL VARIABLES -----------------------

	var regex = "^(\/.+?)\/(tree|notebooks)";
	var gUserPath = window.location.pathname.match(regex) == null ? "" : window.location.pathname.match(regex)[1] + "/"; //delivers /user/dXXXXXX

	// ----------- HANDLER -------------------------------

	/**
	 * This is the entrypoint method that is registered as the respective handler after the tree was rendered.
	 */
	function disk_storage_post_handler() {
		$.ajaxSetup(jsutils.ajaxCookieTokenHandling());

		var dirname = "/" + window.document.body.dataset.notebookPath;

		var urlPath = gUserPath + "storage/check";
		// console.info('urlPath: ', urlPath);
		$.ajax({
			type: "POST",
			processData: false,
			url: urlPath,
			dataType: "json",
			data: JSON.stringify({
				path: ""
			}),
			success: function(data) {
				if (data.status === 0) {
					// We don’t need the status attribute anymore, and explicitly deleting it here saves trouble later
					delete data.status;
					var div = mydialog.exceededDiskStorageDialog(data); // Get structure of the commit dialog

					// Hotkeys are disabled here so the user can enter a commit message without unwanted side effects
					// Jupyter.keyboard_manager.disable();

					dialog.modal({
						body: div,
						title: "DISK STORAGE ALARM",
						buttons: {
							'Manual Cleanup': {
							},
							'Auto Cleanup' : {
								class: "btn-danger",
								click: cleanu_storage
							}
						}
					});
				}
			}
		}).fail(function(jqxhr, textStatus, error) {
			var err = textStatus + ", " + error;
			console.info("Request Failed: " + err);
		});
	}

	function cleanu_storage(data){
		$.ajaxSetup(jsutils.ajaxCookieTokenHandling())

		var urlPath = gUserPath + 'storage/cleanup'
		 var settings = {
			 url: urlPath,
			 processData: false,
			 type: 'PUT',
			 success: function(data) {
			 }
		 }
		 $.ajax(settings)
		 Jupyter.keyboard_manager.enable()
	 }
 

	// ------------WRAPPER------------------------------

	//  /**
	// * Wrapper method for commit_post()
	// * @param {data} contains numerous user information
	// * @return commit_post() method
	// */
	// function openCommitDialog_wrapper(data){

	//     return function b(){
	//         commit_post()
	//     }
	// }

	// -----------------------------------------------------

	//---------- REGISTER EXTENSION ------------------------
	/**
	 * Adds the jupyter extension to the tree view (including the respective handler)
	 */
	function load_ipython_extension() {
		// log to console
		console.info("Loaded Jupyter extension: Juypter disk check -v1");
		base_url = utils.get_body_data("base-url");

		disk_storage_post_handler();
	}

	// Loads the extension
	return {
		load_ipython_extension: load_ipython_extension
	};
});

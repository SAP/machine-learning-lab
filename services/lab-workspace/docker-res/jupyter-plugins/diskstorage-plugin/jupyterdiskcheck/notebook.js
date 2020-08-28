define(["base/js/namespace", "jquery", "base/js/dialog", "require", "./mydialog", "./jsutils"], function(Jupyter, $, dialog, require, myDialog, jsUtils) {
	// ----------- DIALOGS -------------------------------

	var mydialog = require("./mydialog");
	var mydialog = new myDialog();

	var jsutils = require("./jsutils");
	var jsutils = new jsUtils();
	console.info("------------------------" + mydialog);

	// -------- GLOBAL VARIABLES ---------------------------
	var regex = "^(\/.+?)\/(tree|notebooks)";
	var gUserPath = window.location.pathname.match(regex) == null ? "" : window.location.pathname.match(regex)[1] + "/"; //delivers /user/dXXXXXX

	if (!gUserPath) {
		gUserPath = "/";
	}

	// ----------- HANDLER -------------------------------

	/**
	 * This is the entrypoint method of the notebook view that is registered as the respective handler
	 * @param {env} contains numerous user information
	 */
	var disk_storage_post_handler = function(env) {
		// console.info(jsutils.ajaxCookieTokenHandling());
		$.ajaxSetup(jsutils.ajaxCookieTokenHandling()); //Due to XSRF vulnerability we always send a token with the request

		// We’re saving manually to make sure the latest changes are written to disk prior to committing.
		Jupyter.notebook.save_notebook();

		// The actual request to the server
		// Always send the path of the current notebook because the server has to figure out the root of the git repo
		var notebookPath = "/" + window.document.body.dataset.notebookPath;
		var urlPath = gUserPath + "storage/check";
		$.ajax({
			type: "POST",
			processData: false,
			url: urlPath,
			dataType: "json",
			data: JSON.stringify({
				path: notebookPath
			}),
			success: function(data) {
				if (data.status == 0) {
					// We don’t need the status attribute anymore, and explicitly deleting it here saves trouble later
					delete data.status;
					var div = mydialog.exceededDiskStorageDialog(data); // Get structure of the commit dialog

					// Hotkeys are disabled here so the user can enter a commit message without unwanted side effects
					Jupyter.keyboard_manager.disable();

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
	};

	/**
	 * Registers the plugin to tornado
	 */
	var handlerinfo = {
		help: "Open disk storage alarm dialogue.",
		icon: "fa-angry",
		help_index: "",
		handler: disk_storage_post_handler
		//mandatory that this variable is under the commit_post_handler() method
	};

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

	//---------- REGISTER EXTENSION ------------------------
	/**
	 * Adds the jupyter extension to the notebook view (including the respective handler)
	 */
	function load_ipython_extension() {
		// var prefix = 'notebook';
		// var action_name = 'storage-check';
		// var full_action_name = Jupyter.actions.register(handlerinfo, action_name, prefix);

		// add button for new action
		// Jupyter.toolbar.add_buttons_group([full_action_name])

        // action_full_name = IPython.keyboard_manager.actions.register(handlerinfo, action_name, prefix);
        if(IPython.notebook !== undefined){
            IPython.notebook.config.loaded.then(disk_storage_post_handler());
        }
	}

	return {
		load_ipython_extension: load_ipython_extension
	};
});

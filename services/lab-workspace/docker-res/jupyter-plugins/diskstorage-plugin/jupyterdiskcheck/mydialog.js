define(['base/js/namespace', 'jquery','base/js/dialog','require','exports','module'], function (Jupyter, $, dialog, require, exports, module) {

/**
* Class that produces different dialogs by returning the html code
*/
class Dialog{

    /**
    * @param {list} contains commitmessage, lastPushDate, email and name
    * @return {string} The html code to commit a single Jupyter notebook file
    */
    exceededDiskStorageDialog(filelist) {

        let size = (filelist["workspaceSize"] == null)? " " : filelist["workspaceSize"];
        let limit = (filelist["restrictedSize"] == null)? " " : filelist["restrictedSize"]; 
        console.log('size', size);
        let div = $('<div/>');
        let form = $('<form id="diskcheck" />');
        let usageanalyzer = "You can choose between two options: <b>Auto Cleanup</b> will remove all files in your /workspace/environment folder with more than 50MB that weren't used for atleast three days. With <b>Manual Cleanup</b> you have to do the job yourself. To analyze your disk we recommend to use VNC and start the program 'Disk Usage Analyzer' under 'Applications -> System' and analyze the `/workspace` folder." 
        //green: #51ce71
        let message = "You have exceeded the limit of available disk storage assigned to your workspace. Please delete unnecessary files from the <b>/workspace</b> folder. An automatic cleanup will be performed if the workspace exceeds " + (parseInt(limit, 10) * 1.5).toString() + " GB. </br> "
        div.append('<div>'+
                        '<div class="alert alert-danger">' +
                        '<div> Size of your workspace: <b>'+size+' GB / '+ limit + ' GB </b> </div> </br> '+
                            message+
                        '</div>'+
                        '<div style="font-size: 11px;">'+
                         usageanalyzer+
                        '</div>'+
                    '</div>'
                    )

        form.appendTo(div);
        return div;
    }

};

    module.exports =  Dialog; // export class in order to create an object of it in another file
 }
);


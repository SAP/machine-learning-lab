define(['base/js/namespace', 'jquery','base/js/dialog','require','exports','module'], function (Jupyter, $, dialog, require, exports, module) {

/**
* Class that provides utils functions for the notebook.js and tree.js
*/
class JSUtils{

    /**
    * @return {String} function which protects the connection agains xsrf attacks. Sets a token into the header which is stored within a cookie (retrieved by the function getCookie('_xsrf')
    */
    ajaxCookieTokenHandling(){
        return {
            beforeSend: function(xhr, settings) {
                function getCookie(name) {
                    // Does exactly what you think it does.
                    var cookieValue = null;
                    if (document.cookie && document.cookie != '') {
                        var cookies = document.cookie.split(';');
                        for (var i = 0; i < cookies.length; i++) {
                            var cookie = jQuery.trim(cookies[i]);
                            // Does this cookie string begin with the name we want?
                            if (cookie.substring(0, name.length + 1) == (name + '=')) {
                                cookieValue = decodeURIComponent(cookie.substring(name.length + 1));
                                break;
                            }
                        }
                    }
                    return cookieValue;
                }
                // Don’t send the token to external URLs
                if (/^https?:/.test(settings.url)) return;
                // GET requests don’t need the token
                if (/GET/.test(settings.type)) return;

                xhr.setRequestHeader('X-XSRF-TOKEN', getCookie('_xsrf'));
                xhr.setRequestHeader('X-XSRFToken', getCookie('_xsrf'));
            }
        }
    }
};

    module.exports =  JSUtils; // export class in order to create an object of it in another file
 }
);


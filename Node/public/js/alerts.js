'use strict';
// public/js/alerts.js
/**
 * Javascript file to display notifications
 * 
 * @author       Yannick Pferr
 * 
 * @version      5.0
 */

var Level = {
	Success: 'alert-success alert-dismissible',
	Info: 'alert-info alert-dismissible',
	Warning: 'alert-warning alert-dismissible',
	Danger: 'alert-danger alert-dismissible'
};
 
/**
 * shows an alert
 * 
 * @param msg - the message which should be displayed
 * @param type - the importance level of this alert
 * @param timeout - time in milliseconds the alert is shown to the user (optional infinite if nothing specified)
 *
 */
function showAlert(msg, type, timeout) {
	if (timeout !== undefined){
		$('#alerts').append('<div id="alertdiv" class="alert ' +  type + '"><strong>' + msg + '</strong></div>');
		setTimeout(function(){$('#alerts').empty();}, timeout);
	}
	else {
		$('#alerts').append('<div id="alertdiv" class="alert ' +  type + '"><a class="close" data-dismiss="alert">×</a><strong>' + msg + '</strong></div>');
	}
}
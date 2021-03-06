'use strict';
// public/js/sockets.js

/**
 * Contains the eventlistener to handle disconnects and reconnects
 * 
 * @author       Yannick Pferr <yannick.pferr@stud.tu-darmstadt.de>
 * @author       Tobias Mahncke <tobias.mahncke@stud.tu-darmstadt.de>
 * @version      5.0
 *
 */
var socket = io();
var connected = true;
socket.on('redis', function(msg){
	if (msg === 'Redis unavailable'){
		if (connected){
			showAlert('Datenbank nicht verfügbar, bitte informieren Sie einen Administrator.', Level.Danger);
		}
		connected = false;
	} else {
		if (!connected){
			showAlert('Datenbank wieder verfügbar!', Level.Success, 2000);
		}
		connected = true;
	}
});
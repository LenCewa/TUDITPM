'use strict';
// public/js/sockets.js

/**
 * Contains the eventlistener to handle disconnects and reconnects
 * 
 * @author       Yannick Pferr <yannick.pferr@stud.tu-darmstadt.de>
 * @author       Tobias Mahncke <tobias.mahncke@stud.tu-darmstadt.de>
 * @version      3.1
 *
 */
var socket = io();
var connected = true;
socket.on('redis', function(msg){
	if (msg === 'Redis unavailable'){
		if(connected){
			$('#alerts').append($('<div>').addClass('alert alert-danger alert-dismissible').attr('role', 'alert').text(msg));
		}
		connected = false;
	} else {
		if(!connected){
			$('#alerts').empty();
			$('#alerts').append($('<div>').addClass('alert alert-success alert-dismissible').attr('role', 'alert').text('Redis reconnected'));
			// TODO: Per Timer ausblenden
		}
		connected = true;
	}
});
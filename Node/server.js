'use strict';
// server.js

/**
 * Main server file to set up the server.
 * 
 * @author       Tobias Mahncke <tobias.mahncke@stud.tu-darmstadt.de>
 * @license      MIT
 * @version      2.2
 *
 * @requires body-parser
 * @requires compression
 * @requires express
 * @requires http
 * @requires path
 */

// Dependencies
var bodyParser = require('body-parser');
var compress = require('compression');
var express = require('express');
var http = require("http");
var path = require('path');

// Create and start the server
var app = module.exports = express();
var server = http.Server(app);

// load configuration
var config = require('./config/server.conf.json');
var connections = require('./config/connections.conf.json');

// set up the port
app.set('port', config.port);
// compress the requests and responses
app.use(compress());

// time in milliseconds
var minute = 1000 * 60;
var hour = (minute * 60);
var day = (hour * 24);
var week = (day * 7);

// Setup serving static assets from /public
app.use(express.static(path.join(__dirname, '/public'), {
	maxAge: week
}));


// Body parsing middleware supporting
// JSON, urlencoded, and multipart requests.
// get all data/stuff of the body (POST) parameters
// parse application/json
app.use(bodyParser.json());

// Routing for the frontend
app.get('/index', function routeIndex(req, res) {
	res.sendFile(path.join(__dirname, '/public/html/index.html'));
});

// API routing
require('./app/companies')(app);

// Start Express server.
server.listen(app.get('port'), function() {
	// Log how we are running
	console.log('listening on port ' + app.get('port').toString());
	console.log('Ctrl+C to shut down.');

	// Exit cleanly on Ctrl+C
	process.on('SIGINT', function() {
		console.log('has shutdown');
		process.exit(0);
	});
});
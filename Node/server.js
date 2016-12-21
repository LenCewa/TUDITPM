'use strict';
// server.js

/**
 * Main server file to set up the server.
 * 
 * @author       Tobias Mahncke <tobias.mahncke@stud.tu-darmstadt.de>
 * @author       Yannick Pferr <yannick.pferr@stud.tu-darmstadt.de>
 * @author       Ludwig Koch <ludwig.koch@stud.tu-darmstadt.de>
 * @license      MIT
 * @version      3.2
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
var http = require('http');
var socket = require('socket.io');
var path = require('path');
var redis = require('redis');
var mongodb = require('mongodb');

// Create and start the server
var app = module.exports = express();
var server = http.Server(app);

// load configuration
var config = require('./config/server.conf.json');
//TODO: use NODE_ENV instead of hardcoded string
var connections = require('./config/connections.conf.json')['dev'];

// Create a redis client
var client = redis.createClient(connections.redis.port, connections.redis.address);

//Create a mongoDB client
var mongoClient = mongodb.MongoClient;

//Import mongoDB collection to Redis
//Connect to mongoDB
mongodb.connect(connections.mongodb.news, function(err, db) {
	if(err) { return console.dir(err); }
	//Open collection
	var collection = db.collection('company_trump', function(err, collcetion){});
	//Store collection in array
	collection.find().toArray(function(err, items) {
		//Build JSONObject with array in it
		var doc = {'Meldungen': items};
		//Convert to JSONString
		var result = JSON.stringify(doc);
		//Save to Redis
		client.set("TestKey", result);
	});
});
// Socket connections
var io = socket(server);
io.on('connection', function(socket){
  console.log('a user connected');
  socket.on('disconnect', function(){
	console.log('a user disconnected');  
  });
});

client.on('error', function (err) {
    console.log('Error ' + err);
	io.emit('redis', 'Redis unavailable');
});

client.on('connect', function(){
	io.emit('redis', 'Redis available');
});

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


// Routing startseite
app.get('/', function routeIndex(req, res) {
	res.sendFile(path.join(__dirname, '/public/html/start.html'));
});

// Routing addcompany
app.get('/addcompany', function routeIndex(req, res) {
	res.sendFile(path.join(__dirname, '/public/html/addcompany.html'));
});

// Routing keywords
app.get('/keywords', function routeIndex(req, res) {
	res.sendFile(path.join(__dirname, '/public/html/keywords.html'));
});

// Routing RSS/Atom
app.get('/rss', function routeIndex(req, res) {
	res.sendFile(path.join(__dirname, '/public/html/rss.html'));
});

// Routing admin
app.get('/admin', function routeIndex(req, res) {
	res.sendFile(path.join(__dirname, '/public/html/admin.html'));
});





// API routing
require('./app/companies')(app);
require('./app/rss')(app);
require('./app/news')(app, client);

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

module.exports.io = io;
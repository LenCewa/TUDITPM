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
var connections = require('./config/connections.conf.json')[process.env.NODE_ENV];

// Create a redis client
var client = redis.createClient(connections.redis.port, connections.redis.address);

//Create a mongoDB client
var mongoClient = mongodb.MongoClient;

// Dependencies
var company = require('./app/companies');

//Kafka Producer
var kafka = require('kafka-node');
var Producer = kafka.Producer;
var kafkaClient = new kafka.Client();
var producer = new Producer(kafkaClient);

// Kafka ready to send messages	
producer.on('ready', function() {
	console.log("Kafka ready");
});

// Kafka error
producer.on('error', function() {
	console.log("kafka error");
});

// Import mongoDB collections to Redis
// Connect to mongoDB
mongodb.connect(connections.mongodb.news, function(err, db) {
	if (err) {
		return console.dir(err);
	}

	client.flushall(function(err) {
		if (err) {
			console.log(err);
		}
		company.getCompanies(mongodb, function(err, companies) {
			var readCollection = function(collections) {
				var company = collections.pop();
				if (company) {
					// Get company key
					var key = company.key;
					//Open company collection
					var collection = db.collection(key, function(err, collection) {
						if (err) {
							console.log(err);
						}
					});
					//Store collection in array
					collection.find().toArray(function(err, news) {
						if (news.length > 0) {
							for (var i = 0; i < news.length; i++) {
								news[i] = JSON.stringify(news[i]);
							}
							var pushNews = function(array) {
								var singleNews = array.pop();
								client.send_command('lpush', [key, singleNews], function(err) {
									if (err) {
										console.log(err);
									}
									if (array.length > 0) {
										pushNews(array);
									} else if (collections.length > 0) {
										readCollection(collections);
									}
								});
							}
							pushNews(news);
						} else {
							readCollection(collections);
						}
					});
				}
			}
			readCollection(companies);
		});
	});
});

// Socket connections
var io = socket(server);
io.on('connection', function(socket) {
	console.log('a user connected');
	socket.on('disconnect', function() {
		console.log('a user disconnected');
	});
});

client.on('error', function(err) {
	console.log('Error ' + err);
	io.emit('redis', 'Redis unavailable');
});

client.on('connect', function() {
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

// Routing statistics
app.get('/statistics', function routeIndex(req, res) {
	res.sendFile(path.join(__dirname, '/public/html/statistics.html'));
});


var busboy = require('connect-busboy'); //middleware for form/file upload
var fs = require('fs-extra');       //File System - for file manipulation

app.use(busboy());
app.use(express.static(path.join(__dirname, 'public')));

app.route('/upload')
    .post(function (req, res, next) {

        var fstream;
        req.pipe(req.busboy);
        req.busboy.on('file', function (fieldname, file, filename) {
            //console.log("Uploading: " + filename);
            //Path where image will be uploaded
            fstream = fs.createWriteStream(__dirname + '/img/upload.csv');
            file.pipe(fstream);
            fstream.on('close', function () {    
                //console.log("Upload Finished of " + filename);              
                res.redirect('back');           //where to go next
            });
        });
    
    });

app.get('/upload', function routeIndex(req, res) {
	res.sendFile(path.join(__dirname, '/img/upload.csv'));
});




// API routing
company.init(app, producer, mongodb);
require('./app/rss')(app, producer, mongodb);
require('./app/keywords')(app, producer, mongodb);
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
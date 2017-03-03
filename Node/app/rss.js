'use strict';
// app/rss.js

/**
 * Contains all functions to manipulate the list of rss feeds
 * 
 * @author       Arne Schmidt
 * @author       Tobias Mahncke
 * 
 * @version      6.0
 * 
 * @requires mongodb
 */

// Dependencies
var mongodb = require('mongodb');

// load configuration
var connections = require('../config/connections.conf.json')[process.env.NODE_ENV];

/**
 * Helper function to read the url list
 * @param callback callback function, gets an error as first element and data as second
 */
function getLinks(callback) {
	mongodb.connect(connections.mongodb.config, function(err, db) {
		if (err) {
			return callback({
				err: {
					de: 'MongoDB Verbindung konnte nicht aufgebaut werden',
					en: 'MongoDB connection could not be established',
					err: err
				}
			});
		}
		//Open collection
		var collection = db.collection('rsslinks', function(err, collection) {});
		//Store collection in array
		collection.find().sort({
			name: 1
		}).toArray(function(err, feeds) {
			callback(null, feeds);
		});
	});
}

/**
 * Helper function to save a rss feed
 * @param link link of the rss feed
 * @param producer kafka producer to reload the kafka services
 * @param callback callback function, gets an error as first element if existing
 */
function addLink(link, producer, callback) {
	mongodb.connect(connections.mongodb.config, function(err, db) {
		if (err) {
			return callback({
				err: {
					de: 'MongoDB Verbindung konnte nicht aufgebaut werden',
					en: 'MongoDB connection could not be established',
					err: err
				}
			});
		}
		//Open collection
		var collection = db.collection('rsslinks', function(err, collection) {});

		// checks if link already exists
		collection.findOne({
			link: link,
		}, function(err, document) {
			if (document !== null) {
				return callback({
					err: {
						de: 'RSS Feed ist bereits vorhanden',
						en: 'RSS feed already exists',
						err: null
					}
				});
			}
			collection.insert({
				link: link,
			}, function(err, records) {});
			var msg = [{
				topic: 'reload',
				messages: '{ "msg": "rss url added", "rss":"' + link + '"}',
				partition: 0
			}];
			producer.send(msg, function(err, data) {});
			callback();
		});
	});
}

exports.init = function(app, producer) {
	console.log('RSS routes loading');
	/**
	 *  Takes a rss link and appends it to the list of rss feeds.
	 *  Expects the request to contain a json with a rss link.
	 *  @param req The HTTP request object
	 *  @param res The HTTP response object
	 */
	app.post('/api/rss', function(req, res) {
		// Check if the request is correctly formed
		if (req.body.link === undefined || req.body.link === null || req.body.link === '') {
			return res.status(400).send({
				err: {
					de: 'Der Rss Link wurde nicht angegeben.',
					en: 'The Link cannot be empty.',
					err: null
				}
			});
		}
		addLink(req.body.link, producer, function(err) {
			if (err) {
				return res.status(400).send(err);
			}
		});
	});

	/**
	 *  Takes a file of line seperated feed URLs that is uploaded and transforms it into multiple rss feed entries
	 *  @param req The HTTP request object
	 *  @param res The HTTP response object
	 */
	app.post('/api/uploadRSSFeeds', function(req, res) {
		var completeData = '';
		req.pipe(req.busboy);
		req.busboy.on('file', function(fieldname, file, filename) {
			file.on('data', function(data) {
				completeData += data;
			});
			file.on('end', function() {
				var lines = completeData.split(/\n/);

				var readFeed = function(array) {
					var link = array.pop().trim();

					if (link === '') {
						if (array.length > 0) {
							readFeed(array);
						} else {
							return res.status(204).send();
						}
					} else {
						addLink(link, producer, function(err) {
							if (err && err.err && err.err.err) {
								return res.status(500).send(err);
							} else {
								if (array.length > 0) {
									readFeed(array);
								} else {
									return res.status(204).send();
								}
							}
						});
					}
				};
				readFeed(lines);
			});
		});
	});

	/**
	 *  Takes a rss link and deletes it.
	 *  Expects the request to contain a json with a rss link.
	 *  @param req The HTTP request object
	 *  @param res The HTTP response object
	 */
	app.delete('/api/rss', function(req, res) {
		// Check if the request is correctly formed
		if (req.body.link === undefined || req.body.link === null || req.body.link === '') {
			return res.status(400).send({
				err: {
					de: 'Der Rss Link wurde nicht angegeben.',
					en: 'The Link cannot be empty.',
					err: null
				}
			});
		}
		mongodb.connect(connections.mongodb.config, function(err, db) {
			if (err) {
				return res.status(500).send({
					err: {
						de: 'MongoDB Verbindung konnte nicht aufgebaut werden',
						en: 'MongoDB connection could not be established',
						err: err
					}
				});
			}
			//Open collection
			var collection = db.collection('rsslinks', function(err, collection) {});

			// checks if doc already exists
			collection.remove({
				link: req.body.link,
			}, function(err, document) {
				if (err) {
					return res.status(500).send({
						err: {
							de: 'Fehler in der MongoDB Verbindung',
							en: 'Error in MongoDB connection',
							err: err
						}
					});
				}
				var msg = [{
					topic: 'reload',
					messages: '{ "msg": "rss url removed", "rss":"' + req.body.link + '"}',
					partition: 0
				}];
				producer.send(msg, function(err, data) {
					if (err) {
						console.log(err);
					}
				});

				return res.status(204).send();
			});
		});
	});

	/**
	 *  Returns all the listed companies via HTTP get.
	 *  @param req The HTTP request object
	 *  @param res The HTTP response object
	 */
	app.get('/api/rss', function(req, res) {
		getLinks(function(err, data) {
			if (err) {
				return res.status(500).send(err);
			}
			return res.json(data);
		});
	});
};
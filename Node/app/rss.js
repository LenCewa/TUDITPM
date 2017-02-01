'use strict';
// app/rss.js
/**
 * Contains all functions to manipulate the list of rss feeds
 * 
 * @author       Arne Schmidt
 * @author       Tobias Mahncke
 * 
 * @version      6.0
 */

// load configuration
var connections = require('../config/connections.conf.json')[process.env.NODE_ENV];

/**
 * Helper function to read the url list
 * @param callback callback function, gets an error as first element and data as second
 */
function getLinks(mongodb, callback) {
	mongodb.connect(connections.mongodb.config, function(err, db) {
		if (err) {
			callback(err);
			return console.log(err);
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

module.exports = function(app, producer, mongodb) {
	console.log('rss routes loading');
	/**
	 *  Takes a rss link and appends it to the kafka list of rss feeds.
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
		mongodb.connect(connections.mongodb.config, function(err, db) {
			if (err) {
				return res.status(500).send({
					err: {
						de: 'MongoDB Verbindung konnte nicht aufgebaut werden',
						en: 'MongoDB connection could not be established',
						err: null
					}
				});
			}
			//Open collection
			var collection = db.collection('rsslinks', function(err, collection) {});

			// checks if doc already exists
			collection.findOne({
				link: req.body.link,
			}, function(err, document) {
				if (document !== null) {
					return res.status(400).send({
						err: {
							de: 'RSS Feed ist bereits vorhanden',
							en: 'RSS feed already exists',
							err: null
						}
					});
				}
				collection.insert({
					link: req.body.link,
				}, function(err, records) {});
				var msg = [{
					topic: 'reload',
					messages: 'rss feed added',
					partition: 0
				}];
				producer.send(msg, function(err, data) {
					console.log(data);
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
		getLinks(mongodb, function(err, data) {
			if (err) {
				return res.status(500).send(err);
			}
			return res.json(data);
		});
	});
};
'use strict';
// app/keywords.js

/**
 * Contains all functions to manipulate the list of keywords
 * 
 * @author       Arne Schmidt
 * @version      1.0
 *
 * @requires fs-extra
 */

// Dependencies
var fs = require('fs-extra');

// load configuration
var connections = require('../config/connections.conf.json')[process.env.NODE_ENV];

/**
 * Helper function to read the url list
 * @param callback callback function, gets an error as first element and data as second
 */
function readKeywords(mongodb, callback) {
	mongodb.connect(connections.mongodb.config, function(err, db) {
		if (err) {
			return console.dir(err);
		}
		//Open collection
		var collection = db.collection('Keywords', function(err, collcetion) {});
		//Store collection in array
		collection.find().toArray(function(err, items) {
			//Build JSONObject with array in it
			var doc = [];
			for (var i = 0; i < items.length; i++) {
				var keywords = items[i];
				//Array with all keys of the given object
				var element = keywords.keyword;
				doc.push(element);
			}
			callback(null, doc);
		});
	});
}

module.exports = function(app, producer, mongodb) {
	console.log('keywords routes loading');
	/**
	 *  Takes a keyword and appends it to the kafka list of keywords.
	 *  Expects the request to contain a json with a company name.
	 *  @param req The HTTP request object
	 *  @param res The HTTP response object
	 */
	app.post('/api/keywords', function(req, res) {
		// Check if the request is correctly formed
		if (req.body.keyword === undefined || req.body.keyword === null || req.body.keyword === '') {
			return res.status(400).send({
				err: {
					de: 'Es wurde kein Schlagwort angegeben.',
					en: 'The keyword cannot be empty.',
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
			var collection = db.collection('Keywords', function(err, collcetion) {});
			//Store collection in array
			var document = {
				keyword: req.body.keyword
			};
			collection.insert(document, function(err, records) {
				if (err) {
					return res.status(500).send({
						err: {
							de: 'MongoDB Verbindung konnte nicht aufgebaut werden',
							en: 'MongoDB connection could not be established',
							err: null
						}
					});
				}
			});

			var msg = [{
				topic: 'reload',
				messages: 'keyword added',
				partition: 0
			}, ];
			producer.send(msg, function(err, data) {
				console.log(data);
			});

			return res.status(204).send();
		});

	});

	/**
	 *  Returns all the listed keywords vie HTTP get.
	 *  @param req The HTTP request object
	 *  @param res The HTTP response object
	 */
	app.get('/api/keywords', function(req, res) {
		readKeywords(mongodb, function(err, data) {
			if (err) {
				return res.status(500).send(err);
			}
			return res.json(data);
		});
	});
};

function deleteKeywords(app, mongodb, producer) {
	app.post('/api/delete', function(req, res) {
		// Check if the request is correctly formed
		if (req.body === undefined || req.body === null || req.body === '') {
			return res.status(400).send({
				err: {
					de: 'Es wurde kein Schlagwort angegeben.',
					en: 'The keyword cannot be empty.',
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
			var collection = db.collection('Keywords', function(err, collcetion){
				collcetion.remove({keyword: req.body}, function(err, result) {
					if (err) {
						console.log(err);
					}
					console.log(result);
					db.close();
				});
			});
			
			var msg = [
					{ topic: 'reload', messages: 'keyword added', partition: 0 },
				];
			producer.send(msg, function (err, data) {
				console.log(data);
			});
			
			return res.status(204).send();
		});
	});
}
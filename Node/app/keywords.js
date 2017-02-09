'use strict';
// app/keywords.js

/**
 * Contains all functions to manipulate the list of keywords
 * 
 * @author       Arne Schmidt
 * @author       Tobias Mahncke
 * @version      6.0
 *
 * @requires mongodb
 */

// Dependencies
var mongodb = require('mongodb');
var companies = require('./companies');

// load configuration
var connections = require('../config/connections.conf.json')[process.env.NODE_ENV];

/**
 * Helper function to read the url list
 * @param callback callback function, gets an error as first element and data as second
 */
function readKeywords(callback) {
	mongodb.connect(connections.mongodb.config, function(err, db) {
		if (err) {
			return callback(err, null);
		}
		//Open collection
		var collection = db.collection('keywords', function(err, collcetion) {});
		//Store collection in array
		collection.find().sort({
			category: 1
		}).toArray(function(err, items) {
			callback(null, items);
		});
	});
}

module.exports = function(app, producer) {
	console.log('Keywords routes loading');
	/**
	 *  Takes a keyword and appends it to the kafka list of keywords.
	 *  Expects the request to contain a json with a company name.
	 *  @param req The HTTP request object
	 *  @param res The HTTP response object
	 */
	app.post('/api/keywords', function(req, res) {
		// Check if the request is correctly formed
		if (req.body.keyword === undefined || req.body.keyword === null || req.body.keyword === '' || req.body.category === undefined || req.body.category === null || req.body.category === '') {
			return res.status(400).send({
				err: {
					de: 'Es wurde kein Schlagwort und/oder Kategorie angegeben.',
					en: 'The keyword and/or category cannot be empty.',
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
			var collection = db.collection('keywords', function(err, collcetion) {});

			// checks if doc already exists
			collection.findOne({
				category: req.body.category,
				keywords: req.body.keyword
			}, function(err, document) {
				if (document !== null) {
					return res.status(400).send({
						err: {
							de: 'Dokument ist bereits enthalten',
							en: 'Document already exists',
							err: null
						}
					});
				}
				collection.findOne({
					category: req.body.category
				}, function(err, category) {
					if (err) {
						return res.status(500).send({
							err: {
								de: 'MongoDB Verbindung konnte nicht aufgebaut werden',
								en: 'MongoDB connection could not be established',
								err: err
							}
						});
					}
					var sortedArray = [req.body.keyword];

					if (category !== null) {
						var array = category.keywords;
						array.push(req.body.keyword);
						sortedArray = array.sort(function(a, b) {
							return a.localeCompare(b);
						});
					}
					collection.update({
						category: req.body.category
					}, {
						category: req.body.category,
						keywords: sortedArray
					}, {
						upsert: true
					}, function(err, records) {
						if (err) {
							return res.status(500).send({
								err: {
									de: 'MongoDB Verbindung konnte nicht aufgebaut werden',
									en: 'MongoDB connection could not be established',
									err: err
								}
							});
						}
						var msg = [{
							topic: 'reload',
							messages: 'keyword added',
							partition: 0
						}];
						producer.send(msg, function(err, data) {});
						if (req.body.clear) {
							companies.emptyCheckedData(function(err) {
								if (err) {
									return res.status(500).send(err);
								}
								return res.status(204).send();
							});
						} else {
							return res.status(204).send();
						}
					});
				});
			});
		});
	});

	/**
	 *  Returns all the listed keywords via HTTP get.
	 *  @param req The HTTP request object
	 *  @param res The HTTP response object
	 */
	app.get('/api/keywords', function(req, res) {
		readKeywords(function(err, data) {
			if (err) {
				return res.status(500).send(err);
			}
			return res.json(data);
		});
	});

	/**
	 *  Deletes a keyword via HTTP delete.
	 *  @param req The HTTP request object
	 *  @param res The HTTP response object
	 */
	app.delete('/api/deleteKeyword', function(req, res) {
		if (req.body.keyword === undefined || req.body.keyword === null || req.body.keyword === '' || req.body.category === undefined || req.body.category === null || req.body.category === '') {
			return res.status(400).send({
				err: {
					de: 'Es wurde kein Schlagwort und/oder Kategorie angegeben.',
					en: 'The keyword and/or category cannot be empty.',
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
			var collection = db.collection('keywords', function(err, collcetion) {});

			collection.update({
				category: req.body.category
			}, {
				$pull: {
					keywords: req.body.keyword
				}
			}, {
				upsert: true
			}, function(err, records) {
				if (records.result.nModified > 0) {
					var msg = [{
						topic: 'reload',
						messages: 'keyword removed',
						partition: 0
					}];
					producer.send(msg, function(err, data) {
						console.log(data);
					});

					return res.status(204).send();
				}
			});
		});
	});


	/**
	 *  Deletes a category via HTTP delete.
	 *  @param req The HTTP request object
	 *  @param res The HTTP response object
	 */
	app.delete('/api/deleteCategory', function(req, res) {
		if (req.body.category === undefined || req.body.category === null || req.body.category === '') {
			return res.status(400).send({
				err: {
					de: 'Es wurde kein Schlagwort und/oder Kategorie angegeben.',
					en: 'The keyword and/or category cannot be empty.',
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
			var collection = db.collection('keywords', function(err, collcetion) {});

			collection.remove({
				category: req.body.category
			}, function(err, result) {
				console.log(result.result.n);
				if (result.result.n > 0) {
					var msg = [{
						topic: 'reload',
						messages: 'category removed',
						partition: 0
					}];
					producer.send(msg, function(err, data) {
						console.log(data);
					});

					return res.status(204).send();
				}
			});
		});
	});
};
'use strict';
// app/companies.js

/**
 * Contains all functions to manipulate the list of companies
 * 
 * @author       Tobias Mahncke <tobias.mahncke@stud.tu-darmstadt.de>
 * @version      5.0
 */

// load configuration
var connections = require('../config/connections.conf.json')[process.env.NODE_ENV];

var fs = require('fs-extra');

/**
 * Helper function to read the company list
 * @param callback callback function, gets an error as first element and data as second
 */
exports.getCompanies = function(mongodb, callback) {
	mongodb.connect(connections.mongodb.config, function(err, db) {
		if (err) {
			return console.dir(err);
		}
		//Open collection
		var collection = db.collection('companies', function(err, collection) {});
		//Store collection in array
		collection.find().toArray(function(err, companies) {
			callback(null, companies);
		});
	});
};

exports.init = function(app, producer, mongodb) {
	console.log('company routes loading');
	/**
	 *  Takes a company name and appends it to the kafka list of companies.
	 *  Expects the request to contain a json with a company name.
	 *  @param req The HTTP request object
	 *  @param res The HTTP response object
	 */
	app.post('/api/company', function(req, res) {
		// Check if the request is correctly formed
		if (req.body.name === undefined || req.body.name === null || req.body.name === '' || req.body.zipCode === undefined || req.body.zipCode === null || req.body.zipCode === '') {
			return res.status(400).send({
				err: {
					de: 'Der Firmenname und/oder Postleitzahl wurde nicht angegeben.',
					en: 'The company name and/or zip-code cannot be empty.',
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
			var collection = db.collection('companies', function(err, collcetion) {});

			var forms = require('../config/legalForms.json');
			var searchName = req.body.name;
			for (var j = 0; j < forms.length; j++) {
				searchName = searchName.replace(forms[j], '').trim();
			}
			var key = searchName.replace(/\./g).trim();

			//Store collection in array
			var document = {
				name: req.body.name,
				searchName: searchName,
				key: key,
				zipCode: req.body.zipCode
			};

			// checks if doc already exists
			collection.findOne({
				name: req.body.name,
				zipCode: req.body.zipCode
			}, function(err, document) {
				if (document !== null) {
					return res.status(400).send({
						err: {
							de: 'Firma ist bereits vorhanden',
							en: 'Company already exists',
							err: null
						}
					});
				}
				collection.insert(document, function(err, records) {});
				var msg = [{
					topic: 'reload',
					messages: 'company added',
					partition: 0
				}, ];
				producer.send(msg, function(err, data) {
					console.log(data);
				});

				return res.status(204).send();
			});
		});
	});

	/**
	 *  Returns all the listed companies vie HTTP get.
	 *  @param req The HTTP request object
	 *  @param res The HTTP response object
	 */
	app.get('/api/company', function(req, res) {
		exports.getCompanies(mongodb, function(err, data) {
			if (err) {
				return res.status(500).send(err);
			}
			return res.json(data);
		});
	});

	/**
	 *  Deletes a company via HTTP delete.
	 *  @param req The HTTP request object
	 *  @param res The HTTP response object
	 */
	app.delete('/api/company', function(req, res) {
		if (req.body.name === undefined || req.body.name === null || req.body.name === '' || req.body.zipCode === undefined || req.body.zipCode === null || req.body.zipCode === '') {
			return res.status(400).send({
				err: {
					de: 'Der Firmenname und/oder Postleitzahl wurde nicht angegeben.',
					en: 'The company name and/or zip-code cannot be empty.',
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
			var collection = db.collection('companies', function(err, collection) {});

			collection.remove({
				name: req.body.name
			}, function(err, result) {
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
				messages: 'company removed',
				partition: 0
			}, ];
			producer.send(msg, function(err, data) {
				console.log(data);
			});

			return res.status(204).send();
		});

	});
};
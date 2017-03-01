'use strict';
// app/companies.js

/**
 * Contains all functions to manipulate the list of companies
 * 
 * @author       Tobias Mahncke <tobias.mahncke@stud.tu-darmstadt.de>
 * @version      6.0
 * 
 * @requires mongodb
 */

// Dependencies
var mongodb = require('mongodb');

// load configuration
var connections = require('../config/connections.conf.json')[process.env.NODE_ENV];

/**
 * Helper function to read the company list
 * @param callback callback function, gets an error as first element and data as second
 */
exports.getCompanies = function(callback) {
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
		var collection = db.collection('companies', function(err, collection) {});
		//Store collection in array
		collection.find().sort({
			name: 1
		}).toArray(function(err, companies) {
			callback(null, companies);
		});
	});
};

/**
 * Helper function to clear the checked data DB
 * @param callback callback function, gets an error as first element if existing
 */
exports.emptyCheckedData = function(callback) {
	mongodb.connect(connections.mongodb.checkeddata, function(err, db) {
		if (err) {
			return callback({
				err: {
					de: 'MongoDB Verbindung konnte nicht aufgebaut werden',
					en: 'MongoDB connection could not be established',
					err: err
				}
			});
		}
		db.dropDatabase(function(err, result) {
			if (err) {
				return callback({
					err: {
						de: 'MongoDB Verbindung konnte nicht aufgebaut werden',
						en: 'MongoDB connection could not be established',
						err: err
					}
				});
			}
			callback();
		});
	});
};

/**
 * Helper function to save a company
 * @param producer kafka producer to reload the kafka services
 * @param name name of the company
 * @param zipCode zip code of the company
 * @param searchTerms addtional search terms for the company
 * @param callback callback function, gets an error as first element if existing
 */
var saveCompany = function(producer, name, zipCode, searchTerms, callback) {
	mongodb.connect(connections.mongodb.config, function(err, db) {
		if (err) {
			return callback({
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
		var searchName = name;
		for (var j = 0; j < forms.length; j++) {
			searchName = searchName.replace(" " + forms[j], '').trim();
		}
		
		var key = searchName.replace(/\./g).trim();

		//Store collection in array
		var doc = {
			name: name,
			searchName: searchName,
			key: key,
			zipCode: zipCode,
			searchTerms: searchTerms
		};

		// checks if doc already exists
		collection.remove({
			name: name,
			zipCode: zipCode
		}, function(err, document) {
			collection.insert(doc, function(err, records) {});
			var msg = [{
				topic: 'reload',
				messages: 'company added',
				partition: 0
			}, ];
			producer.send(msg, function(err, data) {
				if (err) {
					console.log(err);
				}
			});
			callback();
		});
	});
};

exports.init = function(app, producer) {
	console.log('Company routes loading');
	/**
	 *  Takes a company name and appends it to the kafka list of companies.
	 *  Expects the request to contain a json with a company name.
	 *  @param req The HTTP request object
	 *  @param res The HTTP response object
	 */
	app.post('/api/company', function(req, res) {
		console.log(req.body);
		// Check if the request is correctly formed
		if (req.body.name === undefined || req.body.name === null || req.body.name.trim() === '' || req.body.zipCode === undefined || req.body.zipCode === null || req.body.zipCode.trim() === '') {
			return res.status(400).send({
				err: {
					de: 'Der Firmenname und/oder Postleitzahl wurde nicht angegeben.',
					en: 'The company name and/or zip-code cannot be empty.',
					err: null
				}
			});
		}
		var searchTerms = [];
		if (req.body.searchTerms) {
			searchTerms = req.body.searchTerms;
		}
		if (req.body.clear) {
			exports.emptyCheckedData(function(err) {
				if (err) {
					return res.status(500).send(err);
				}
				saveCompany(producer, req.body.name, req.body.zipCode, searchTerms, function(err) {
					if (err) {
						return res.status(500).send(err);
					} else {
						return res.status(204).send();
					}
				});
			});
		} else {
			saveCompany(producer, req.body.name, req.body.zipCode, searchTerms, function(err) {
				if (err) {
					return res.status(500).send(err);
				} else {
					return res.status(204).send();
				}
			});
		}
	});

	/**
	 *  Returns all the listed companies via HTTP get.
	 *  @param req The HTTP request object
	 *  @param res The HTTP response object
	 */
	app.get('/api/company', function(req, res) {
		exports.getCompanies(function(err, data) {
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
				if (err) {
					console.log(err);
				}
			});

			return res.status(204).send();
		});

	});

	/**
	 *  Takes a csv file that is uploaded and transforms it into multiple company entries
	 *  @param req The HTTP request object
	 *  @param res The HTTP response object
	 */
	app.post('/api/uploadCompany', function(req, res) {
		var completeData = '';
		req.pipe(req.busboy);
		req.busboy.on('file', function(fieldname, file, filename) {
			file.on('data', function(data) {
				completeData += data;
			});
			file.on('end', function() {
				var lines = completeData.split(/\n/);

				// Check which colum contains which entry
				var title = lines[0].split(';');
				var namekey = 0;
				if (title[1].toLowerCase().includes("unternehmen")) {
					namekey = 1;
				}

				var readCompany = function(array) {
					var fields = array.pop().split(';');

					// Extract name and zip code
					var name = '';
					if (fields[namekey] !== undefined) {
						name = fields[namekey].trim();
					}

					var zip = '';
					if (fields[1 - namekey] !== undefined) {
						zip = fields[1 - namekey].trim();
					}

					// Skip invalid lines
					if (name === '' || zip === '' || zip.length !== 5) {
						if (array.length > 0) {
							readCompany(array);
						} else {
							return res.status(204).send();
						}
					} else {
						var searchTerms = [];
						saveCompany(producer, name, zip, searchTerms, function(err) {
							if (err) {
								return res.status(500).send(err);
							} else {
								if (array.length > 0) {
									readCompany(array);
								} else {
									return res.status(204).send();
								}
							}
						});
					}
				};
				readCompany(lines);
			});
		});
	});
};
'use strict';
// app/companies.js

/**
 * Contains all functions to manipulate the list of companies
 * 
 * @author       Tobias Mahncke <tobias.mahncke@stud.tu-darmstadt.de>
 * @version      2.4
 *
 * @requires fs-extra
 */

// Dependencies
var fs = require('fs-extra');

// load configuration
// couldnt use ['dev'] because of an error
var connections = require('../config/connections.conf.json').dev;

/**
 * Helper function to read the company list
 * @param callback callback function, gets an error as first element and data as second
 */
function readCompanies(mongodb, callback) {
	mongodb.connect(connections.mongodb.config, function(err, db) {
		if (err) { return console.dir(err); }
		//Open collection
		var collection = db.collection('Companies', function(err, collcetion){});
		//Store collection in array
		collection.find().toArray(function(err, items) {
			//Build JSONObject with array in it
			var doc = [];
			for (var i = 0; i < items.length; i++) {
				var companies = items[i];
				//Array with all keys of the given object
				var element = companies.company;
				doc.push(element);
			}
			callback(null,doc);
		});
	});
}

module.exports = function(app, producer, mongodb) {
	console.log('company routes loading');
	/**
	 *  Takes a company name and appends it to the kafka list of companies.
	 *  Expects the request to contain a json with a company name.
	 *  @param req The HTTP request object
	 *  @param res The HTTP response object
	 */
	app.post('/api/company', function(req, res) {
		// Check if the request is correctly formed
		if (req.body.company === undefined || req.body.company === null || req.body.company === '') {
			return res.status(400).send({
				err: {
					de: 'Der Firmenname wurde nicht angegeben.',
					en: 'The company name cannot be empty.',
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
			var collection = db.collection('Companies', function(err, collcetion){});
			//Store collection in array
			var document = {company: req.body.company};
			collection.insert(document, function(err, records){});
			var msg = [
					{ topic: 'reload', messages: 'company added', partition: 0 },
				];
			producer.send(msg, function (err, data) {
				console.log(data);
			});
			
			return res.status(204).send();
		});
	});

	/**
	 *  Returns all the listed companies vie HTTP get.
	 *  @param req The HTTP request object
	 *  @param res The HTTP response object
	 */
	app.get('/api/company', function(req, res) {
		readCompanies(mongodb,function(err, data) {
			if (err) {
				return res.status(500).send(err);
			}
			return res.json(data);
		});
	});
};
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
var connections = require('../config/connections.conf.json').keys;

/**
 * Helper function to read the url list
 * @param callback callback function, gets an error as first element and data as second
 */
function readKeywords(callback) {
	fs.ensureFile(connections.kafka, function(err) {
		// if the file cannot be created the server isn't set up right
		if (err) {
			callback({
				err: {
					de: 'Fehler beim Zugriff auf die Unternehmensliste. Bitte informieren Sie einen Administrator.',
					en: 'Accessing the companies file failed. Please contact an adminstrator.',
					err: err
				}
			}, null);
		}
		// file has now been created, including the directory it is to be placed in
		fs.readFile(connections.kafka, 'utf8', function(err, data) {
			// if the file cannot be read the user has to contact a adminstrator
			if (err) {
				callback({
					err: {
						de: 'Fehler beim Zugriff auf die Unternehmensliste. Bitte informieren Sie einen Administrator.',
						en: 'Accessing the companies file failed. Please contact an adminstrator.',
						err: err
					}
				}, null);
			}
			callback(null, data);
		});
	});
}

module.exports = function(app, producer) {
	console.log('keywords routes loading');
	/**
	 *  Takes a keyword and appends it to the kafka list of keywords.
	 *  Expects the request to contain a json with a company name.
	 *  @param req The HTTP request object
	 *  @param res The HTTP response object
	 */
	app.post('/api/keywords', function(req, res) {
		// Check if the request is correctly formed
		if (req.body.company === undefined || req.body.company === null || req.body.company === '') {
			return res.status(400).send({
				err: {
					de: 'Es wurde kein Schlagwort angegeben.',
					en: 'The keyword cannot be empty.',
					err: null
				}
			});
		}
		readKeywords(function(err, data) {
			if (err) {
				return res.status(500).send(err);
			}
			// Append the data to existing data
			if (data !== '') {
				data = data + '\n' + req.body.company;
			} else {
				data = req.body.company;
			}
			fs.writeFile(connections.kafka, data, function(err) {
				if (err) {
					// if the file cannot be written the user has to contact an adminstrator
					return res.status(500).send({
						err: {
							de: 'Fehler beim Zugriff auf die Schlagwortliste. Bitte informieren Sie einen Administrator.',
							en: 'Accessing the keyword file failed. Please contact an adminstrator.',
							err: err
						}
					});
				}
				
				//Send reload message to Kafka
				var msg = [
					{ topic: 'reload', messages: 'keyword added', partition: 0 },
				];
				producer.send(msg, function (err, data) {
					console.log(data);
				});
				
				return res.status(204).send();
			});
		});
	});

	/**
	 *  Returns all the listed keywords vie HTTP get.
	 *  @param req The HTTP request object
	 *  @param res The HTTP response object
	 */
	app.get('/api/keywords', function(req, res) {
		readKeywords(function(err, data) {
			if (err) {
				return res.status(500).send(err);
			}
			var array = data.split('\n');
			return res.json(array);
		});
	});
};
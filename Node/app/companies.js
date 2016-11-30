'use strict';
// app/companies.js

/**
 * Contains all functions to manipulate the list of companies
 * 
 * @author       Tobias Mahncke <tobias.mahncke@stud.tu-darmstadt.de>
 * @version      2.1
 *
 * @requires fs-extra
 */

// Dependencies
var fs = require('fs-extra');

// load configuration
var connections = require('../config/connections.conf.json');

module.exports = function(app) {
	console.log('company routes loading');
	/**
	 *  Takes a company name and appends it to the kafka list of companies.
	 *  Excpects the request do contain a json with a company name.
	 *  @param req The HTTP request object
	 *  @param res The HTTP response object
	 */
	app.post('/api/company', function setConfig(req, res) {
		// Check if the request is correctly formed
		if (req.body.company === undefined || req.body.company === null || req.body.company === '') {
			return res.status(400).send({
				err: {
					de: 'Der Firmenname wurde nicht angegeben.',
					en: 'The company name cannot be empty.',
					err: null
				}
			})
		}
		fs.ensureFile(connections.kafka, function(err) {
			// if the file cannot be created the server isn't set up right
			if (err) {
				res.status(500).send({
					err: {
						de: 'Fehler beim Zugriff auf die Unternehmensliste. Bitte informieren Sie einen Administrator.',
						en: 'Accessing the companies file failed. Please contact an adminstrator.',
						err: err
					}
				});
			}
			// file has now been created, including the directory it is to be placed in
			fs.readFile(connections.kafka, 'utf8', function(err, data) {
				// if the file cannot be read the user has to contact a adminstrator
				if (err) {
					res.status(500).send({
						err: {
							de: 'Fehler beim Zugriff auf die Unternehmensliste. Bitte informieren Sie einen Administrator.',
							en: 'Accessing the companies file failed. Please contact an adminstrator.',
							err: err
						}
					});
				}
				// Append the data to existing data
				if (data !== '') {
					data = data + '\n' + req.body.company;
				} else {
					data = req.body.company;
				}
				// if the file cannot be written the user has to contact a adminstrator
				fs.writeFile(connections.kafka, data, function(err) {
					if (err) {
						res.status(500).send({
							err: {
								de: 'Fehler beim Zugriff auf die Unternehmensliste. Bitte informieren Sie einen Administrator.',
								en: 'Accessing the companies file failed. Please contact an adminstrator.',
								err: err
							}
						});
					}
					res.status(204).send();
				});
			});
		})
	});
};
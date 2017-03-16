'use strict';
// app/map.js

/**
 * Contains all functions to create the data for the maps view
 * 
 * @author       Tobias Mahncke <tobias.mahncke@stud.tu-darmstadt.de>
 * @version      6.0
 */

// Dependencies
var server = require('../server');
var fs = require('fs');
var mapshaper = require('mapshaper');

module.exports = function(app, client) {
	console.log('Maps routes loading');
	/**
	 *  Returns the map json
	 *  @param req The HTTP request object
	 *  @param res The HTTP response object
	 */
	app.get('/api/map/:digits/:data', function(req, res) {
		var selected = req.params.data.split(',');
		var inputFiles = [];
		var zipCodes = {};
		var data = [];
		var digits = req.params.digits;

		/** 
		 * Combines a single json
		 */
		var combineJSON = function() {
			// Load germany file
			var comand = '-i config/data/germany.json ';
			// Append all edited zip code files
			for (var i = 0; i < inputFiles.length; i++) {
				comand += 'tmp/' + inputFiles[i] + ' ';
			}
			var tmpMap = new Date().getTime() + '.json';
			comand += 'combine-files -merge-layers -o tmp/' + tmpMap;
			// Combine the file
			mapshaper.runCommands(comand, function(error) {
				// Load final map file into memory
				var map = require('../tmp/' + tmpMap);
				// Delete temporary files
				for (var i = 0; i < inputFiles.length; i++) {
					fs.unlinkSync('./tmp/' + inputFiles[i]);
				}
				fs.unlinkSync('./tmp/' + tmpMap);
				// Return the combined map file
				return res.send(map);
			});
		};

		/**
		 * Creates the temporary ap files
		 */
		var createTmpFiles = function() {
			var singleData;
			while (!singleData) {
				if (data.length === 0) {
					combineJSON();
					return;
				}
				singleData = data.pop();
			}

			var tmpFilename = new Date().getTime();
			// Write the edited file
			fs.writeFile('./tmp/' + tmpFilename, JSON.stringify(singleData), function(err) {
				if (err) {
					return console.log(err);
				}
				inputFiles.push(tmpFilename);
				if (data.length === 0) {
					combineJSON();
				} else {
					createTmpFiles();
				}
			});
		};

		/**
		 * Loads the zip code files
		 */
		var getKey = function(array) {
			var read = array.pop();
			var zip = array.pop();
			var key = array.pop();
			var i;
			// Gets the length for a key from redis, returns null if key is not found
			client.llen(key, function(err, length) {
				console.log(key);
				if (err) {
					return res.status(500).send({
						err: {
							de: 'Fehler beim Zugriff auf die Meldungen. Bitte informieren Sie einen Administrator.',
							en: 'Accessing the news failed. Please contact an adminstrator.',
							err: err
						}
					});
				}
				if (!zipCodes[zip.substring(0, digits)]) {
					// Find the corresponding zip file
					data[zip.substring(0, digits)] = require('../config/data/combined/' + zip.substring(0, digits) + '.json');
				}
				// Set the style if new entries exists
				if (!zipCodes[zip.substring(0, digits)]) {
					zipCodes[zip.substring(0, digits)] = 0;
				}
				if (read < length) {
					zipCodes[zip.substring(0, digits)] += length - read;
				}

				if (array.length === 0) {
					for (i = 0; i < data.length; i++) {
						if (data[i]) {
							if (zipCodes[i] === 0) {
								data[i].features[0].properties.style = 'old';
							} else {
								data[i].features[0].properties.style = 'new';
							}
							data[i].features[0].properties.news = zipCodes[i];
						}
					}
					console.log('Creat tmp files');
					createTmpFiles();
				} else {
					getKey(array);
				}
			});
		};
		getKey(selected);
	});
};
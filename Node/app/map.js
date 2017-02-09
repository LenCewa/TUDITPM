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
	app.get('/api/map/:data', function(req, res) {
		var selected = req.params.data.split(',');
		var inputFiles = [];

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
		 * Loads the zip code files
		 */
		var getKey = function(array) {
			var read = array.pop();
			var zip = array.pop();
			var key = array.pop();
			// Gets the length for a key from redis, returns null if key is not found
			client.llen(key, function(err, length) {
				if (err) {
					return res.status(500).send({
						err: {
							de: 'Fehler beim Zugriff auf die Meldungen. Bitte informieren Sie einen Administrator.',
							en: 'Accessing the news failed. Please contact an adminstrator.',
							err: err
						}
					});
				}
				var found = false;
				var data, i;
				// Find the corresponding zip file
				try {
					data = require('../config/data/' + zip + '.json');
					found = true;
				} catch (err) {}
				// If the file does not exist take the next available zip code
				if (!found) {
					for (i = 0; i < 10; i++) {
						try {
							data = require('../config/data/' + zip.substring(0, 4) + i + '.json');
							found = true;
							break;
						} catch (err) {}
					}
				}
				if (!found) {
					for (i = 0; i < 100; i++) {
						try {
							data = require('../config/data/' + zip.substring(0, 3) + i + '.json');
							found = true;
							break;
						} catch (err) {}
					}
				}
				if (found) {
					var tmpFilename = new Date().getTime();
					// Set the style if new entries exists
					if (read < length) {
						data.features[0].properties.style = 'new';
					} else {
						data.features[0].properties.style = 'old';
					}
					data.features[0].properties.news = length - read;
					// Write the edited file
					fs.writeFile('./tmp/' + tmpFilename, JSON.stringify(data), function(err) {
						if (err) {
							return console.log(err);
						}
						inputFiles.push(tmpFilename);
						if (array.length === 0) {
							combineJSON();
						} else {
							getKey(array);
						}
					});
				} else {
					return res.status(500).send({
						err: {
							de: 'Die PLZ ' + zip + ' könnte keinem Gebiet zugeordnet werden. Bitte informieren Sie einen Administrator.',
							en: 'The zip ' + zip + ' could not be assigned to an area. Please contact an adminstrator.',
							err: err
						}
					});
				}
			});
		};
		getKey(selected);
	});
};
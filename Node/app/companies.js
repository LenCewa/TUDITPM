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
var mongodb = require('mongodb');

var fs = require('fs-extra');

/**
 * Helper function to read the company list
 * @param callback callback function, gets an error as first element and data as second
 */
exports.getCompanies = function(callback) {
	mongodb.connect(connections.mongodb.config, function(err, db) {
		if (err) {
			callback(err);
			return console.log(err);
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
 * Helper function to empty the checkeddata DB
 * @param callback callback function, gets an error as first element and data as second
 */
exports.emptyCheckedData = function(callback) {
	mongodb.connect(connections.mongodb.checkeddata, function(err, db) {
		if (err) {
			callback(err);
			return console.log(err);
		}
		db.dropDatabase(function(err, result) {
			if (err) {
				callback(err);
				return console.log(err);
			}
			callback(null);
		});
	});
};

/**
 * Helper function to empty the checkeddata DB
 * @param callback callback function, gets an error as first element and data as second
 */
var saveCompany = function(name, zipCode, searchTerms, callback) {
	mongodb.connect(connections.mongodb.config, function(err, db) {
		if (err) {
			callback({
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
			searchName = searchName.replace(forms[j], '').trim();
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
				console.log(data);
			});
			callback(null);
		});
	});
};

exports.init = function(app, producer) {
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
		var searchTerms = [];
		if (req.body.searchTerms) {
			searchTerms = req.body.searchTerms;
		}
		saveCompany(req.body.name, req.body.zipCode, searchTerms, function(err) {
			if (err) {
				return res.status(500).send(err);
			} else {
				return res.status(204).send();
			}
		})
	});

	/**
	 *  Returns all the listed companies vie HTTP get.
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
	 *  Emptys the checkeddata DB vie HTTP delete.
	 *  @param req The HTTP request object
	 *  @param res The HTTP response object
	 */
	app.delete('/api/emptyCheckedData', function(req, res) {
		exports.emptyCheckedData(function(err, data) {
			if (err) {
				return res.status(500).send(err);
			}
			return res.status(204).send();
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

exports.uploadCompanies = function (data) {
    var inhalt = "" + data;

    var lines = inhalt.split(/\n/);
    
    var titel = lines[0].split(';');
    var namekey, plzkey;
    if(titel[0].toLowerCase().includes("unternehmen")){
        namekey = 0; plzkey = 1;
    } else {
        namekey = 1; plzkey = 0;
    }
    
    
    for (var i = 1; i < lines.length; i++) {

        var fields = lines[i].split(';');
        
        var name;
        if (fields[namekey] === undefined) {
            name = "";
        } else{
            name = fields[namekey].trim();
        }
        
        var plz;
        if (fields[plzkey] === undefined) {
            plz = "";
        } else {
            plz = fields[plzkey].trim();
        }
            
        var unternehmen = {
            'name': name,
            'plz' : plz
        };
        
        console.log(unternehmen.name);
        console.log(unternehmen.plz);

        if (unternehmen.name === '') {
            console.log('Keine leeren Unternehmensnamen erlaubt.');
        } else if (unternehmen.plz === '' || unternehmen.plz.length !== 5) {
            console.log('Keine valide Postleitzahl eingegeben.');
        } else {
            var searchTerms = [];
            saveCompany(unternehmen.name, unternehmen.plz, searchTerms, function(err) {
                if (err) {
                    return res.status(500).send(err);
                } else {
                    return res.status(204).send();
                }
            });
            console.log("Hinzugefügt - Name: " + unternehmen.name + ", PLZ: " + unternehmen.plz);
		}
    }
};
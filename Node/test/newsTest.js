'use strict';
// test/newsTest.js

/**
 * Contains all test cases for the list of news
 * 
 * @author       Yannick Pferr <yannick.pferr@stud.tu-darmstadt.de>
 * @version      3.0
 *
 * @requires chai
 * @requires fs-extra
 * @requires supertest
 */

// Dependencies
var should = require('chai').should();
var fs = require('fs-extra');
var request = require('supertest');
var mongodb = require('mongodb');
var redis = require('redis');
var mongodb = require('mongodb');

// create a config mockup
var connections = require('../config/connections.conf.json')['test'];

// Set up the server
var app = require('../server.js');
var server = require('supertest')(app);

// Create a redis client
var client = redis.createClient(connections.redis.port, connections.redis.address);

// Start a mongo client
var mongoClient = mongodb.MongoClient;

describe('<Redis configuration>', function() {
	
	// Test case for getting the list of news
	it('should be able to get all entries of redis as json array', function(done) {
		mongoClient.connect(connections.mongodb.news, function (err, db) {
  			if (err) {
    			console.log('Unable to connect to the mongoDB server. Error:', err);
  			} else {
    			console.log('Connection established to', connections.mongodb.news);
				
				 /**
				 * Check the returned data, should contain the list of entries.
				 * @param {Object} err
				 * @param {Object} header
				 */
				var onResponse = function(err, header) {
					header.res.statusCode.should.equal(200);
					header.res.body.length.should.equal(3);
					header.res.body[0].text.should.equal('Company');
					header.res.body[1].text.should.equal('Hello');
					header.res.body[2].text.should.equal('Test');
					done();
				};

				var collection = db.collection('testcollection2');

			    //Create some text
			    var text1 = {text: 'Company'};
			    var text2 = {text: 'Hello'};
			    var text3 = {text: 'Test'};

			    // Insert the text in mongodb
			    collection.insert([text1, text2, text3], function (err, result) {
			    	if (err) {
			      		assert.fail(null, err, 'Could not write to db.');
						db.close();
			    	} else {
						// get the inserted text
						collection.find().toArray(function(err, items){
							if (err){
								assert.fail(null, err, 'Could not write to redis.');
								collection.remove();
								db.close();
							}
							else {
								// insert the fetched text into redis
								var doc = {'Meldungen': items};
								client.set("TestKey2", JSON.stringify(doc));
								server.get('/api/news/TestKey2').end(onResponse);
								collection.remove();
								db.close();
							}
						});
			      	}  	
			    });
  			}
		});
	});
});
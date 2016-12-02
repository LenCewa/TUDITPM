'use strict';
// test/companyTest.js

/**
 * Contains all test cases for the list of companies
 * 
 * @author       Tobias Mahncke <tobias.mahncke@stud.tu-darmstadt.de>
 * @version      2.1
 *
 * @requires chai
 * @requires fs-extra
 * @requires supertest
 */

// Dependencies
var should = require('chai').should();
var fs = require('fs-extra');
var request = require('supertest');

// create a config mockup
var connections = require('../config/connections.conf.json');

// Set up the server
var app = require('../server.js');
var server = require('supertest')(app);

describe('<Company configuration>', function() {
	// Test case for getting the list of companies
	it('should be able to get all entries of the list as json array', function(done) {
		/**
		 * Check the returned data, should contain the list of entries.
		 * @param {Object} err
		 * @param {Object} header
		 */
		var onResponse = function(err, header) {
			header.res.statusCode.should.equal(200);
			header.res.body.length.should.equal(3);
			header.res.body[0].should.equal('Company');
			header.res.body[1].should.equal('Hello');
			header.res.body[2].should.equal('Test');
			done();
		};

		// create the companies file and fill it with testdata
		fs.ensureFile(connections.kafka, function(err) {
			// if the file cannot be created the server isn't set up right
			if (err) {
				assert.fail(null, err, 'File could not be created.');
			}
			fs.writeFile(connections.kafka, 'Company\nHello\nTest', function(err) {
				// if the file cannot be written fail the test
				if (err) {
					assert.fail(null, err, 'File could not be written.');
				}
				server.get('/api/company').end(onResponse);
			});
		});
	});

	// Test case for adding an entry
	it('should be able to add an entry to the list', function(done) {
		/**
		 * Check the list in the file, should contain the new entry.
		 * @param {Object} err
		 * @param {Object} header
		 */
		var onResponse = function(err, header) {
			header.res.statusCode.should.equal(204);
			// file has now been created, including the directory it is to be placed in
			fs.readFile(connections.kafka, 'utf8', function(err, data) {
				// if the file cannot be read the user has to contact a adminstrator
				if (err) {
					assert.fail(null, err, 'File could not be read.');
				}
				data.should.equal('Company\nHello\nTest\nZoo');
				done();
			});
		};

		server.post('/api/company').send({
			company: 'Zoo'
		}).end(onResponse);
	});

	// Clear the companies file
	after(function(done) {
		fs.writeFile(connections.kafka, '', function(err) {
			// if the file cannot be written fail the test
			if (err) {
				assert.fail(null, err, 'File could not be written.');
			}
			done();
		});
	});
});
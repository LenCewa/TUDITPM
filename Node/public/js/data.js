'use strict';
// public/js/alerts.js
/**
 * Javascript file to store cor data
 * 
 * @author       Tobias Mahncke
 * 
 * @version      5.0
 */
var companies;

$.get("/api/company", function(data) {
	companies = data;
	// Call the data loaded function. Implement this function in another js file to react to the finished data load.
	companyDataLoaded(); // jshint ignore:line
});

function reloadCompanies(callback) {
	$.get("/api/company", function(data) {
		companies = data;
		callback();
	});
}

function getCompanyObject(name) {
	if (companies) {
		for (var i = 0; i < companies.length; i++) {
			if (companies[i].name === name) {
				return companies[i];
			}
		}
	}
}
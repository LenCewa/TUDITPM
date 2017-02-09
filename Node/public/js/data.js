'use strict';
// public/js/data.js
/**
 * Javascript file to store core data
 * 
 * @author       Tobias Mahncke
 * 
 * @version      6.0
 */

// Wrapper for all data
var localData = {
	companies: null,
	keywords: null,
	rss: null
};

// Load data on page loading
$.get("/api/company", function(data) {
	localData.companies = data;
	// Call the data loaded function. Implement this function in another js file to react to the finished data load.
	if (companyDataLoaded) { // jshint ignore:line
		companyDataLoaded(); // jshint ignore:line
	}
});

$.get("/api/rss", function(data) {
	localData.rss = data;
	// Call the data loaded function. Implement this function in another js file to react to the finished data load.
	if (rssDataLoaded) { // jshint ignore:line
		rssDataLoaded(); // jshint ignore:line
	}
});

$.get("/api/keywords", function(data) {
	localData.keywords = data;
	// Call the data loaded function. Implement this function in another js file to react to the finished data load.
	if (keywordsDataLoaded) { // jshint ignore:line
		keywordsDataLoaded(); // jshint ignore:line
	}
});

/**
 * Reloads the companies and executes the callback.
 */
localData.reloadCompanies = function(callback) {
	$.get("/api/company", function(data) {
		localData.companies = data;
		callback();
	});
};

/**
 * Reloads the rss feed and executes the callback.
 */
localData.reloadRSS = function(callback) {
	$.get("/api/rss", function(data) {
		localData.rss = data;
		callback();
	});
};

/**
 * Reloads the keywords and executes the callback.
 */
localData.reloadKeywords = function(callback) {
	$.get("/api/keywords", function(data) {
		localData.keywords = data;
		callback();
	});
};


/**
 * Returns the company object to the given company name.
 */
localData.getCompanyObject = function(name) {
	if (localData.companies) {
		for (var i = 0; i < localData.companies.length; i++) {
			if (localData.companies[i].name === name) {
				return localData.companies[i];
			}
		}
	}
};

/**
 * Returns the company object to the given company key.
 */
localData.getCompanyObjectByKey = function(key) {
	if (localData.companies) {
		for (var i = 0; i < localData.companies.length; i++) {
			if (localData.companies[i].key === key) {
				return localData.companies[i];
			}
		}
	}
};
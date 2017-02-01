'use strict';
// public/js/data.js
/**
 * Javascript file to store core data
 * 
 * @author       Tobias Mahncke
 * 
 * @version      6.0
 */
var localData = {
	companies: null,
	keywords: null,
	rss: null
};

$.get("/api/company", function(data) {
	localData.companies = data;
	// Call the data loaded function. Implement this function in another js file to react to the finished data load.
	if(companyDataLoaded){   // jshint ignore:line
		companyDataLoaded(); // jshint ignore:line
	}
});

$.get("/api/rss", function(data) {
	localData.rss = data;
	// Call the data loaded function. Implement this function in another js file to react to the finished data load.
	if(rssDataLoaded){   // jshint ignore:line
		rssDataLoaded(); // jshint ignore:line
	}
});

$.get("/api/keywords", function(data) {
	localData.keywords = data;
	// Call the data loaded function. Implement this function in another js file to react to the finished data load.
	if(keywordsDataLoaded){   // jshint ignore:line
		keywordsDataLoaded(); // jshint ignore:line
	}
});

localData.reloadCompanies = function(callback) {
	$.get("/api/company", function(data) {
		localData.companies = data;
		callback();
	});
};

localData.reloadRSS = function(callback) {
	$.get("/api/rss", function(data) {
		localData.rss = data;
		callback();
	});
};

localData.reloadKeywords = function(callback) {
	$.get("/api/keywords", function(data) {
		localData.keywords = data;
		callback();
	});
};


localData.getCompanyObject = function(name) {
	if (localData.companies) {
		for (var i = 0; i < localData.companies.length; i++) {
			if (localData.companies[i].name === name) {
				return localData.companies[i];
			}
		}
	}
};
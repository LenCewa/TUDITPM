'use strict';
// public/js/addcompany.js
/**
 * Javascript file for all the funtions used in the company configruation page.
 * 
 * @author       Yannick Pferr
 * @author       Tobias Mahncke
 * 
 * @version      5.0
 */

var db = {
	loadData: function(filter) {
		return $.grep(this.clients, function(client) {
			return (!filter.Unternehmen || client.Unternehmen.indexOf(filter.Unternehmen) > -1) && (!filter.Postleitzahl || client.Postleitzahl.indexOf(filter.Postleitzahl) > -1);
		});
	}
};

function readData(data) {
	db.clients = [];
	for (var i = 0; i < data.length; i++) {
		var element = {};
		element.Postleitzahl = data[i].zipCode;
		element.Unternehmen = data[i].name;
		db.clients.push(element);
	}
	$("#jsGrid").jsGrid({
		height: "auto",
		width: "100%",

		filtering: true,
		sorting: true,
		autoload: true,

		paging: true,
		pageIndex: 1,
		pageSize: 20,
		pageButtonCount: 5,
		pagerFormat: "{first} {prev} {pages} {next} {last}    {pageIndex} von {pageCount}",
		pagePrevText: "Zurück",
		pageNextText: "Weiter",
		pageFirstText: "Erste",
		pageLastText: "Letzte",
		pageNavigatorNextText: "...",
		pageNavigatorPrevText: "...",

		controller: db,

		fields: [{
			name: "Unternehmen",
			type: "textarea",
			width: 150
		}, {
			name: "Postleitzahl",
			type: "textarea",
			width: 150
		}, {
			type: "control"
		}]
	});
}



/**
 * Sends the company name and zip-code given in the input fields to the server.
 */
function postUrls() {
	$.ajax({
		type: 'POST',
		url: '/api/company',
		data: '{"name":"' + $('#companyName').val() + '", "zipCode":"' + $('#zipCode').val() + '"}',
		success: function(data) {
			$.get("/api/company", function(data) {
				readData(data);
			});
		},
		contentType: 'application/json'
	});
}
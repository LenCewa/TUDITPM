'use strict';
// public/js/addcompany.js
/**
 * Javascript file for all the funtions used in the keyword configruation page.
 * 
 * @author       Tobias Mahncke
 * 
 * @version      6.0
 */

function readData(data) {
	console.log(data);
	var clients = [];
	for (var i = 0; i < data.length; i++) {
		var element = {};
		element.URL = data[i].link;
		element.ID = i + 1;
		clients.push(element);
	}

	$("#jsGrid").jsGrid({
		width: "100%",
		height: "auto",

		inserting: false,
		editing: false,
		sorting: true,
		autoload: true,

		paging: true,
		pageIndex: 1,
		pageSize: 20,
		pageButtonCount: 5,
		pagerFormat: "{first} {prev} {pages} {next} {last}    {pageIndex} of {pageCount}",
		pagePrevText: "Zurück",
		pageNextText: "Weiter",
		pageFirstText: "Erste",
		pageLastText: "Letzte",
		pageNavigatorNextText: "...",
		pageNavigatorPrevText: "...",

		data: clients,

		fields: [{
				name: "ID",
				type: "number",
				width: 25,
			}, {
				name: "URL",
				type: "text",
				width: 50,
			}
			//{ type: "control" }
		]
	});
}


/**
 * Sends the rss given in the input field "rssName" to the server.
 */
function postUrls() {
	$.ajax({
		type: 'POST',
		url: '/api/rss',
		data: '{"link":"' + $('#rssName').val() + '"}',
		success: function(data) {
			$.get("/api/rss", function(data) {
				readData(data);
			});
		},
		contentType: 'application/json'
	});
}
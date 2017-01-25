'use strict';
// public/js/start.js
/**
 * Javascript file for all the funtions used in the configuration section.
 * 
 * @author       Yannick Pferr
 * @author       Tobias Mahncke
 * 
 * @version      5.0
 */

var selectedCompanies = Cookies.get('selectedCompanies');
if (!selectedCompanies) {
	selectedCompanies = {};
}
var companies;
var showAllCompanies = false;

var db = {
	loadData: function(filter) {
		return $.grep(this.clients, function(client) {
			return (!filter.Inhalt || client.Inhalt.indexOf(filter.Inhalt) > -1) && (!filter.Unternehmen || client.Unternehmen.indexOf(filter.Unternehmen) > -1) && (!filter.Schlagwörter || client.Schlagwörter.indexOf(filter.Schlagwörter) > -1) && (!filter.Quelle || client.Quelle.indexOf(filter.Quelle) > -1) && (!filter.Datum || client.Datum.indexOf(filter.Datum) > -1);
		});
	}
};

function readData(data) {
	if (!data.isArray) {
		data = [data];
	}

	console.log(data);

	db.clients = [];
	for (var i = 0; i < data.length; i++) {
		var element = {
			"Inhalt": data[i].text,
			"Quelle": data[i].link,
			"Datum": data[i].date,
			"Unternehmen": data[i].company,
			"Schlagwörter": data[i].keyword
		};
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
		pagerFormat: "{first} {prev} {pages} {next} {last}    {pageIndex} of {pageCount}",
		pagePrevText: "Zurück",
		pageNextText: "Weiter",
		pageFirstText: "Erste",
		pageLastText: "Letzte",
		pageNavigatorNextText: "...",
		pageNavigatorPrevText: "...",

		controller: db,

		fields: [{
			name: "PLZ",
			type: "number",
			width: 25,
		}, {
			name: "Unternehmen",
			type: "text",
			width: 50,
		}, {
			name: "Kategorie",
			type: "text",
			width: 25,
		}, {
			name: "Schlagwörter",
			type: "text",
			width: 50,
		}, {
			name: "Inhalt",
			type: "text",
			width: 150,
			validate: "required"
		}, {
			name: "Datum",
			type: "number",
			width: 25,
		}, {
			name: "Quelle",
			type: "text",
			width: 25,
		}, {
			type: "control"
		}, ]
	});
}

/**
 * Displays the keywords in a table
 * @param {[type]} data         [description]
 */
function showKeywordStart(data) {
	// Fills the table row by row
	for (var i = 0; i < data.length; i++) {
		$('#keywordStartTable').append('<tr><td>' + data[i].text + '</td></tr>');
	}
}

function reloadCompanyList() {
	$('#companyStartTableBody').empty();
	// Fills the table row by row
	for (var i = 0; i < companies.length; i++) {
		var companySelected = selectedCompanies[companies[i]];
		var btnType;
		if (companySelected || showAllCompanies) {
			if (companySelected) {
				btnType = 'success';
			} else {
				btnType = 'default';
			}
			$('#companyStartTableBody').append('<tr><td>' + companies[i] + '</td><td>' + '<button class="btn btn-' + btnType + '" onClick="postUrls()""><span class="glyphicon glyphicon-ok" aria-hidden="true"></span></button>' + '</td></tr>');
		}
	}
}

function showAll(){
	showAllCompanies = !showAllCompanies;
	$('#showAllBtn').toggleClass('btn-default');
	$('#showAllBtn').toggleClass('btn-success');
	reloadCompanyList();
}

/**
 * Displays the companylist in a table
 * @param {[type]} data         [description]
 */
function showCompaniesStart(data) {
	companies = data;
	reloadCompanyList();
}

/**
 * Sends the company given in the input field "companyName" to the server.
 */
function postCompany() {
	$.ajax({
		type: 'POST',
		url: '/api/company',
		data: '{"company":"' + $('#companyName').val() + '"}',
		success: function(data) {
			$.get("/api/company", function(data) {
				showCompaniesStart(data);
			});
		},
		contentType: 'application/json'
	});
}
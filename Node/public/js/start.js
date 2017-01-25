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
} else {
	selectedCompanies = JSON.parse(selectedCompanies);
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
			width: 40,
		}, {
			name: "Kategorie",
			type: "text",
			width: 30,
		}, {
			name: "Schlagwörter",
			type: "text",
			width: 40,
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
			width: 50,
		}]
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
			$('#companyStartTableBody').append('<tr><td>' + companies[i] + '</td><td>' + '<button id="' + companies[i] + '-btn" class="btn btn-' + btnType + '" onClick="selectCompany(\'' + companies[i] + '\')"><span class="glyphicon glyphicon-ok" aria-hidden="true"></span></button>' + '</td></tr>');
		}
	}
}

function selectCompany(name) {
	$('[id="' + name + '-btn"]').toggleClass('btn-default');
	$('[id="' + name + '-btn"]').toggleClass('btn-success');
	if (selectedCompanies[name]) {
		selectedCompanies[name] = false;
	} else {
		selectedCompanies[name] = true;
	}
	Cookies.set('selectedCompanies', selectedCompanies);
}

function showAll() {
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

function searchCompany() {
	// Declare variables
	var input, filter, table, tr, td, i;
	input = document.getElementById("companyName");
	filter = input.value.toUpperCase();
	table = document.getElementById("companyStartTableBody");
	tr = table.getElementsByTagName("tr");

	// Loop through all table rows, and hide those who don't match the search query
	for (i = 0; i < tr.length; i++) {
		td = tr[i].getElementsByTagName("td")[0];
		if (td) {
			if (td.innerHTML.toUpperCase().indexOf(filter) > -1) {
				tr[i].style.display = "";
			} else {
				tr[i].style.display = "none";
			}
		}
	}
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

function JSONToCSVConvertor(JSONData, ReportTitle, ShowLabel) {
	//If JSONData is not an object then JSON.parse will parse the JSON string in an Object
	var arrData = typeof JSONData !== 'object' ? JSON.parse(JSONData) : JSONData;

	var CSV = '';
	var row, index;
	//Set Report title in first row or line

	CSV += ReportTitle + '\r\n\n';

	//This condition will generate the Label/Header
	if (ShowLabel) {
		row = '';

		//This loop will extract the label from 1st index of on array
		for (index in arrData[0]) {

			//Now convert each value to string and comma-seprated
			row += index + ',';
		}

		row = row.slice(0, -1);

		//append Label row with line break
		CSV += row + '\r\n';
	}

	//1st loop is to extract each row
	for (var i = 0; i < arrData.length; i++) {
		row = '';

		//2nd loop will extract each column and convert it in string comma-seprated
		for (index in arrData[i]) {
			row += '"' + arrData[i][index] + '",';
		}

		row.slice(0, row.length - 1);

		//add a line break after each row
		CSV += row + '\r\n';
	}

	if (CSV === '') {
		alert("Invalid data"); // jshint ignore:line
		return;
	}

	//Generate a file name
	var fileName = "Newsfeed ";
	//this will remove the blank-spaces from the title and replace it with an underscore
	fileName += ReportTitle.replace(/ /g, "_");

	//Initialize file format you want csv or xls
	var uri = 'data:text/csv;charset=utf-8,' + escape(CSV); // jshint ignore:line

	// Now the little tricky part.
	// you can use either>> window.open(uri);
	// but this will not work in some browsers
	// or you will not get the correct file extension    

	//this trick will generate a temp <a /> tag
	var link = document.createElement("a");
	link.href = uri;

	//set the visibility hidden so it will not effect on your web-layout
	link.style = "visibility:hidden";
	link.download = fileName + ".csv";

	//this part will append the anchor tag and remove it after automatic click
	document.body.appendChild(link);
	link.click();
	document.body.removeChild(link);
}

function exportCSV() {
	var data = $('#jsGrid').jsGrid('option', 'data');
	var utc = new Date().toJSON().slice(0, 10).replace(/-/g, '/');
	JSONToCSVConvertor(data, utc, true);
}
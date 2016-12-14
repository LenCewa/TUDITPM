'use strict';
// public/js/index.js
/**
 * Javascript file for all the funtions used in the configuration section.
 * 
 * @author       Yannick Pferr
 * @author       Tobias Mahncke
 * 
 * @version      3.1
 */

/**
 * Appends the given text in a table cell to the given row.
 * @param {tr-node} row  The table row to wich to append
 * @param {String}  text The text wich will be in the table cell
 */
function addTableData(row, text) {
	var cell = document.createElement('td');
	// Perform a null check 
	if (!text) {
		text = '';
	}
	var textNode = document.createTextNode(text);

	cell.appendChild(textNode);
	row.appendChild(cell);
}

/**
 * Adds a single table row to the table. The row contains a given number of columns.
 * @param {[type]} table        [description]
 * @param {[type]} data         [description]
 * @param {[type]} index        [description]
 * @param {[type]} columnNumber [description]
 * @param {[type]} columnLength [description]
 */
function addTableRow(table, data, index, columnNumber, columnLength) {
	var row = document.createElement('tr');
	for (var i = 0; i < columnNumber; i++) {
		addTableData(row, data[index + i * columnLength]);
	}
	table.append(row);
}

/**
 * Displays the given data in a simple table.
 * @param  {[type]} data [description]
 */
function showCompanies(data) {
	var columnNumber = 4;
	var columnLength = Math.ceil(data.length / columnNumber);
	// Clear the current data
	$('#companyTableBody').empty();
	// Fills the table row by row
	for (var i = 0; i < columnLength; i++) {
		addTableRow($('#companyTableBody'), data, i, columnNumber, columnLength);
	}
	$('#tableWrapper').append($('#companyTable'));
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
				showCompanies(data);
			});
		},
		contentType: 'application/json'
	});
}

// TODO: Auf Laden der Seite warten
$.get("/api/company", function(data) {
    showCompanies(data);
});
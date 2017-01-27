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
var firstDataLoad = true;

function createTable() {
	var data = [];

	// Declare variables
	var filter, i, show;
	filter = $('#search').val().toUpperCase().trim();

	// Loop through all table rows, and hide those who don't match the search query
	for (i = 0; i < companies.length; i++) {
		if ((companies[i].zipCode && companies[i].zipCode.toUpperCase().indexOf(filter) > -1) ||
			(companies[i].name && companies[i].name.toUpperCase().indexOf(filter) > -1) ||
			(filter && filter !== '')) {
			data.push(companies[i]);
			data[i].button = '<button class="btn btn-danger pull-right" onClick="deleteCompany(\'' + companies[i].name + '\',\'' + companies[i].zipCode + '\')"><span class="glyphicon glyphicon-minus" aria-hidden="true"></span></button>';
		}
	}

	$('#table').bootstrapTable('load', data);
}

function companyDataLoaded() {
	createTable();
}

/**
 * Sends the company name and zip-code given in the input fields to the server.
 */
function postUrls() {
	$.ajax({
		type: 'POST',
		url: '/api/company',
		data: '{"name":"' + $('#companyName').val() + '", "zipCode":"' + $('#zipCode').val() + '"}',
		success: reloadCompanies(function() {
			createTable();
			showAlert($('#companyName').val() + " added!", Level.Success, 2000);
		}),
		statusCode: {
			400: function(error) {
				showAlert(error.responseJSON.err.de, Level.Warning, 4000);
			}
		},
		contentType: 'application/json'
	});
}

function deleteCompany(company, zipCode) {
	$.ajax({
		type: 'DELETE',
		url: '/api/company',
		data: '{"name":"' + company + '", "zipCode":"' + zipCode + '"}',
		success: reloadCompanies(function() {
			createTable();
			showAlert(company + " deleted!", Level.Success, 2000);
		}),
		contentType: 'application/json'
	});
}
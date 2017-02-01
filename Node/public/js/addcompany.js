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
	for (i = 0; i < localData.companies.length; i++) {
		if ((localData.companies[i].zipCode && localData.companies[i].zipCode.toUpperCase().indexOf(filter) > -1) ||
			(localData.companies[i].name && localData.companies[i].name.toUpperCase().indexOf(filter) > -1) ||
			(filter && filter === '')) {
			data.push(localData.companies[i]);
			data[data.length - 1].button = '<button class="btn btn-danger pull-right" onClick="deleteCompany(\'' + localData.companies[i].name + '\',\'' + localData.companies[i].zipCode + '\')"><span class="glyphicon glyphicon-minus" aria-hidden="true"></span></button>';
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
	var companyName = $('#companyName').val().trim();
	var zipCode = $('#zipCode').val().trim();
	if (companyName === '') {
		showAlert('Keine leeren Unternehmensnamen erlaubt.', Level.Warning, 1000);
	} else if(zipCode === '' || zipCode.length !== 5){
		showAlert('Keine valide Postleitzahl eingegeben.', Level.Warning, 1000);
	}else{
		$.ajax({
			type: 'POST',
			url: '/api/company',
			data: '{"name":"' + companyName + '", "zipCode":"' + zipCode + '"}',
			success: localData.reloadCompanies(function() {
				createTable();
				showAlert(companyName + " hinzugefügt!", Level.Success, 2000);
				$('#companyName').val('');
				$('#zipCode').val('');
			}),
			statusCode: {
				400: function(error) {
					showAlert(error.responseJSON.err.de, Level.Warning, 4000);
				}
			},
			contentType: 'application/json'
		});
	}
}

function deleteCompany(company, zipCode) {
	$.ajax({
		type: 'DELETE',
		url: '/api/company',
		data: '{"name":"' + company + '", "zipCode":"' + zipCode + '"}',
		success: localData.reloadCompanies(function() {
			createTable();
			showAlert(company + " gelöscht!", Level.Success, 2000);
		}),
		contentType: 'application/json'
	});
}
'use strict';
// public/js/addcompany.js
/**
 * Javascript file for all the funtions used in the company configruation page.
 * 
 * @author       Yannick Pferr
 * @author       Tobias Mahncke
 * 
 * @version      6.0
 */
var selectedTerms = {};
var i;

function getSelectedTerm(key) {
	if (selectedTerms[key]) {
		return selectedTerms[key];
	} else {
		return localData.getCompanyObjectByKey(key).searchTerms[0];
	}
}

function createTable() {
	var data = [];

	// Declare variables
	var filter, show;
	filter = $('#search').val().toUpperCase().trim();


	// Loop through all table rows, and hide those who don't match the search query
	for (i = 0; i < localData.companies.length; i++) {
		if ((localData.companies[i].zipCode && localData.companies[i].zipCode.toUpperCase().indexOf(filter) > -1) ||
			(localData.companies[i].name && localData.companies[i].name.toUpperCase().indexOf(filter) > -1) ||
			(filter && filter === '')) {
			data.push(localData.companies[i]);
			data[data.length - 1].appendTerm = '';
			if (localData.companies[i].searchTerms && localData.companies[i].searchTerms.length > 0) {
				data[data.length - 1].appendTerm += '<div class="btn-group" style="width:100%"><button type="button" style="border-top-right-radius: 0;border-bottom-right-radius: 0;width:80%" class="btn btn-default dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">';
				data[data.length - 1].appendTerm += getSelectedTerm(localData.companies[i].key);
				data[data.length - 1].appendTerm += '<span class="caret"></span><span class="sr-only">Toggle Dropdown</span></button><ul class="dropdown-menu">';
				for (var j = 0; j < localData.companies[i].searchTerms.length; j++) {
					data[data.length - 1].appendTerm += '<li><a href="#" onClick="selectTerm(\'' + localData.companies[i].searchTerms[j] + '\', \'' + localData.companies[i].key + '\')">' + localData.companies[i].searchTerms[j] + '</a></li>';
				}
				data[data.length - 1].appendTerm += '</ul><button type="button" class="btn btn-danger" style="width:20%" onClick="removeTerm(\'' + localData.companies[i].key + '\')"><span class="glyphicon glyphicon-minus" aria-hidden="true"></span></button></div>';
			}
			data[data.length - 1].removeTerm = '<div class="input-group"><input class="form-control" type="text" ID="term' + localData.companies[i].key + '" placeholder="Neuer Suchbegriff"></input><span class="input-group-btn"><button class="btn btn-success" type="button" onClick="addTerm(\'' + localData.companies[i].key + '\')"><span class="glyphicon glyphicon-plus" aria-hidden="true"></span></button></span></div>';
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
function postCompany(company) {
	$.ajax({
		type: 'POST',
		url: '/api/company',
		data: JSON.stringify(company),
		success: localData.reloadCompanies(function() {
			createTable();
			if (company.searchTerms) {
				showAlert(company.name + " aktualisiert!", Level.Success, 2000);
			} else {
				showAlert(company.name + " hinzugefügt!", Level.Success, 2000);
			}
		}),
		statusCode: {
			400: function(error) {
				showAlert(error.responseJSON.err.de, Level.Warning, 4000);
			}
		},
		contentType: 'application/json'
	});
}

function createCompany() {
	var companyName = $('#companyName').val().trim();
	var zipCode = $('#zipCode').val().trim();
	if (companyName === '') {
		showAlert('Keine leeren Unternehmensnamen erlaubt.', Level.Warning, 1000);
	} else if (zipCode === '' || zipCode.length !== 5) {
		showAlert('Keine valide Postleitzahl eingegeben.', Level.Warning, 1000);
	} else {
		$('#companyName').val('');
		$('#zipCode').val('');
		postCompany({
			name: companyName,
			zipCode: zipCode
		});
	}
}

function selectTerm(term, key) {
	selectedTerms[key] = term;
	createTable();
}

function removeTerm(key) {
	console.log(key);
	var company = localData.getCompanyObjectByKey(key);
	if (company.searchTerms) {
		console.log(selectedTerms[key]);
		if (!selectedTerms[key]) {
			company.searchTerms.splice(0, 1);
			console.log(company.searchTerms);
		} else {
			for (i = 0; i < company.searchTerms.length; i++) {
				if (selectedTerms[key] === company.searchTerms[i]) {
					company.searchTerms.splice(i, 1);
				}
			}
		}
	}
	console.log(company);
	postCompany(company);
}

function addTerm(key) {
	var term = $("[id='term" + key + "']").val().trim();
	if (term === '') {
		showAlert('Keine leeren Suchbegriffe erlaubt.', Level.Warning, 1000);
	} else {
		var company = localData.getCompanyObjectByKey(key);
		var duplicate = false;
		if (company.searchTerms) {
			for (i = 0; i < company.searchTerms.length; i++) {
				if (company.searchTerms[i] === term) {
					showAlert('Suchbegriff bereits enthalten.', Level.Warning, 1000);
					duplicate = true;
				}
			}
		} else {
			company.searchTerms = [];
		}
		if (!duplicate) {
			company.searchTerms.push(term);
			postCompany(company);
		}
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
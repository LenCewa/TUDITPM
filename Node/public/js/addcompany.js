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

/**
 * Gets the currently selected search term for a company idetified by the key.
 */
function getSelectedTerm(key) {
	if (selectedTerms[key]) {
		return selectedTerms[key];
	} else {
		return localData.getCompanyObjectByKey(key).searchTerms[0];
	}
}

/** 
 * Creates the bootstrap table of companies
 */
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
			// Create the Button and Dropdown elements with the corresponding functions
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

/** 
 * Gets called by localData and creates the initial table
 */
function companyDataLoaded() {
	createTable();
}

/** 
 * Reloads the data after the upload finished. As the upload uses the standard HTML5 upload we cannot access the request so we delay by 10s and reload afterwards.
 */
function delayedReload() {
	showAlert("Daten werden aktualisiert...", Level.Info, 10000);
	setTimeout(function() {
		showAlert("Daten erfolgreich aktualisiert.", Level.Success, 1000);
		localData.reloadCompanies(createTable);
	}, 10000);
}

/**
 * Sends the company object to the server.
 */
function postCompany(company) {
	company.clear = ($('#clear').val() === 'on');
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

/**
 * Checks the company name and zip-code given in the input fields and sends them to the server.
 */
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

/**
 * Selects the given term for the given key from the dropdown element.
 */
function selectTerm(term, key) {
	selectedTerms[key] = term;
	createTable();
}

/**
 * Removes the currently selected term for the given company.
 */
function removeTerm(key) {
	var company = localData.getCompanyObjectByKey(key);
	var confirmDelete = false;
	if (company.searchTerms) {
		if (!selectedTerms[key]) {
			if (confirm('Möchten Sie den Suchbegriff "' + company.searchTerms[0] + '" wirklich löschen. Dieser Begriff wird dann vom System nicht mehr für die Suche nach dem Unternehmen genutzt.')) {
				company.searchTerms.splice(0, 1);
				confirmDelete = true;
			}
		} else {
			for (i = 0; i < company.searchTerms.length; i++) {
				if (selectedTerms[key] === company.searchTerms[i]) {
					if (confirm('Möchten Sie den Suchbegriff ' + company.searchTerms[i] + ' wirklich löschen. Dieser Begriff wird dann vom System nicht mehr für die Suche nach dem Unternehmen genutzt.')) {
						company.searchTerms.splice(i, 1);
						confirmDelete = true;
					}
				}
			}
		}
	}
	if (confirmDelete) {
		postCompany(company);
	}
}

/**
 * Appends the term from the input element of the given company to the search terms.
 */
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

/**
 * Deletes the company given by name and zip code.
 */
function deleteCompany(company, zipCode) {
	if (confirm('Möchten Sie das Unternehmen "' + company + '" wirklich löschen. Das System wird dann nicht mehr nach Daten zu diesem Unternehmen suchen. Die Daten bleiben in der Datenbank erhalten und können im Notfall von eine Administrator wiederhergestellt werden.')) {
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
}
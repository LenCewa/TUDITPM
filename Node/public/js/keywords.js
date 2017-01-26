'use strict';
// public/js/addcompany.js
/**
 * Javascript file for all the funtions used in the keyword configruation page.
 * 
 * @author       Tobias Mahncke
 * 
 * @version      5.0
 */

var localData;

function reloadKeywords() {
	$('#keywordTableHead').empty();
	$('#keywordTableBody').empty();

	var total = 0;
	var mapping = [];
	var header = '<tr>';
	var searchBar = '<tr>';
	var i, j;
	var maxEntries = 0;
	if (localData) {
		for (i = 0; i < localData.length; i++) {
			header += '<th>' + localData[i].category + '</th>';
			searchBar += '<td><div class="input-group"><input class="form-control" type="text" ID="' + localData[i].category + '" placeholder="Neues Schlagwort"></input><span class="input-group-btn"><button class="btn btn-success" type="button" onClick="postKeyword(\'' + localData[i].category + '\')"><span class="glyphicon glyphicon-plus" aria-hidden="true"></span></button></span></div></td>';
			if (localData[i].keywords) {
				if (localData[i].keywords.length > maxEntries) {
					maxEntries = localData[i].keywords.length;
				}
			}
		}
		header += '<th><div class="input-group"><input class="form-control" type="text" ID="newCategory" placeholder="Neue Kategorie"></input><span class="input-group-btn"><button class="btn btn-success" type="button" onClick="addCategory()"><span class="glyphicon glyphicon-plus" aria-hidden="true"></span></button></span></div></tr>';
		searchBar += '</tr>';

		for (i = 0; i < localData.length; i++) {
			var category = localData[i];
			if (category.keywords) {
				for (j = 0; j < maxEntries ; j++) {
					if (!mapping[j]) {
						mapping[j] = '<tr>';
					}
					if (category.keywords[j]) {
						mapping[j] += '<td>' + category.keywords[j] + '</td>';
					} else {
						mapping[j] += '<td></td>';
					}
					if (i === localData.length - 1) {
						mapping[j] += '</tr>';
					}
				}
			}
		}

		$('#keywordTableHead').append(header);
		$('#keywordTableHead').append(searchBar);

		// Fills the table row by row
		for (i = 0; i < mapping.length; i++) {
			$('#keywordTableBody').append(mapping[i]);
		}
	}
}

/**
 * Sends the keyword given in the input field "keywordName" to the server.
 */
function postKeyword(category) {
	$.ajax({
		type: 'POST',
		url: '/api/keywords',
		data: '{"keyword":"' + $('#' + category).val() + '", "category":"' + category + '"}',
		success: function(data) {
			$.get("/api/keywords", function(data) {
				showAlert($('#' + category).val() + " added!", Level.Success, 2000);
				localData = data;
				reloadKeywords();
			});
		},
		statusCode: {
			400: function(error) {
				showAlert(error.responseJSON.err.de, Level.Warning, 4000);
			}
		},
		contentType: 'application/json'
	});
}


/**
 * Sends the keyword given in the input field "keywordName" to the server.
 */
function addCategory() {
	localData.push({
		category: $('#newCategory').val(),
		keywords: []
	});
	reloadKeywords();
}

function itemToDelete(data) {
	$.ajax({
		type: 'DELETE',
		url: '/api/keywords',
		data: '',
		success: function(data) {
			$.get("/api/keywords", function(data) {
				showAlert(" deleted!", Level.Danger);
				reloadKeywords(data);
			});
		},
		contentType: 'application/json'
	});
}
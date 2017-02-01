'use strict';
// public/js/addcompany.js
/**
 * Javascript file for all the funtions used in the keyword configruation page.
 * 
 * @author       Tobias Mahncke
 * 
 * @version      5.0
 */

function reloadKeywords() {
	$('#keywordTableHead').empty();
	$('#keywordTableBody').empty();

	var total = 0;
	var mapping = [];
	var header = '<tr>';
	var searchBar = '<tr>';
	var i, j;
	var maxEntries = 0;
	if (localData.keywords) {
		for (i = 0; i < localData.keywords.length; i++) {
			header += '<th>' + localData.keywords[i].category + '<button class="btn btn-danger pull-right" onClick="categoryToDelete(\'' + localData.keywords[i].category + '\')"><span class="glyphicon glyphicon-minus" aria-hidden="true"></span></button></th>';
			searchBar += '<td><div class="input-group"><input class="form-control" type="text" ID="' + localData.keywords[i].category + '" placeholder="Neues Schlagwort"></input><span class="input-group-btn"><button class="btn btn-success" type="button" onClick="postKeyword(\'' + localData.keywords[i].category + '\')"><span class="glyphicon glyphicon-plus" aria-hidden="true"></span></button></span></div></td>';
			if (localData.keywords[i].keywords) {
				if (localData.keywords[i].keywords.length > maxEntries) {
					maxEntries = localData.keywords[i].keywords.length;
				}
			}
		}
		header += '<th><div class="input-group"><input class="form-control" type="text" ID="newCategory" placeholder="Neue Kategorie"></input><span class="input-group-btn"><button class="btn btn-success" type="button" onClick="addCategory()"><span class="glyphicon glyphicon-plus" aria-hidden="true"></span></button></span></div></tr>';
		searchBar += '</tr>';

		for (i = 0; i < localData.keywords.length; i++) {
			var category = localData.keywords[i];
			if (category.keywords) {
				for (j = 0; j < maxEntries; j++) {
					if (!mapping[j]) {
						mapping[j] = '<tr>';
					}
					if (category.keywords[j]) {
						mapping[j] += '<td>' + category.keywords[j] + '<button class="btn btn-danger pull-right" onClick="keywordToDelete(\'' + category.category + '\',\'' + category.keywords[j] + '\')"><span class="glyphicon glyphicon-minus" aria-hidden="true"></span></button></td>';
					} else {
						mapping[j] += '<td></td>';
					}
					if (i === localData.keywords.length - 1) {
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

function keywordsDataLoaded() {
	reloadKeywords();
}

/**
 * Sends the keyword given in the input field "keywordName" to the server.
 */
function postKeyword(category) {
	if ($("[id='" + category + "']").val().trim() === '') {
		showAlert('Keine leeren Schlagwörter erlaubt.', Level.Warning, 1000);
	} else {
		$.ajax({
			type: 'POST',
			url: '/api/keywords',
			data: '{"keyword":"' + $("[id='" + category + "']").val().trim() + '", "category":"' + category + '"}',
			success: function(data) {
				$.get("/api/keywords", function(data) {
					showAlert($("[id='" + category + "']").val() + " hinzugefügt!", Level.Success, 2000);
					$("[id='" + category + "']").val('');
					localData.keywords = data;
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
}


/**
 * Sends the keyword given in the input field "keywordName" to the server.
 */
function addCategory() {
	if ($('#newCategory').val().trim() === '') {
		showAlert('Keine leere Kategorie erlaubt.', Level.Warning, 1000);
	} else {
		localData.keywords.push({
			category: $('#newCategory').val().trim(),
			keywords: []
		});
		$('#newCategory').val('');
		reloadKeywords();
	}
}

function keywordToDelete(category, keyword) {
	$.ajax({
		type: 'DELETE',
		url: '/api/deleteKeyword',
		data: '{"keyword":"' + keyword + '", "category":"' + category + '"}',
		statusCode: {
			204: function() {
				$.get("/api/keywords", function(data) {
					localData.keywords = data;
					reloadKeywords();
					showAlert(keyword + " gelöscht!", Level.Success, 1000);
				});
			},
		},
		contentType: 'application/json'
	});
}

function categoryToDelete(category) {
	$.ajax({
		type: 'DELETE',
		url: '/api/deleteCategory',
		data: '{"category":"' + category + '"}',
		statusCode: {
			204: function() {
				$.get("/api/keywords", function(data) {
					localData.keywords = data;
					reloadKeywords();
					showAlert("Kategorie " + category + " gelöscht!", Level.Success, 1000);
				});
			},
		},
		contentType: 'application/json'
	});
}
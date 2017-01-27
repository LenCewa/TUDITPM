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

var showAllCompanies = false;
var news;

function createTable() {
	var data = [];

	// Declare variables
	var filter, i, show;
	filter = $('#newsSearch').val().toUpperCase().trim();

	if (filter && filter !== '') {
		// Loop through all table rows, and hide those who don't match the search query
		for (i = 0; i < news.length; i++) {
			if ((news[i].zipCode && news[i].zipCode.toUpperCase().indexOf(filter) > -1) ||
				(news[i].company && news[i].company.toUpperCase().indexOf(filter) > -1) ||
				(news[i].category && news[i].category.toUpperCase().indexOf(filter) > -1) ||
				(news[i].keyword && news[i].keyword.toUpperCase().indexOf(filter) > -1) ||
				(news[i].text && news[i].text.toUpperCase().indexOf(filter) > -1) ||
				(news[i].link && news[i].link.toUpperCase().indexOf(filter) > -1) ||
				(news[i].date && news[i].date.toUpperCase().indexOf(filter) > -1)) {
				data.push(news[i]);
			}
		}
	} else {
		data = news;
	}

	$('#table').bootstrapTable('load', data);
}

function reloadData() {
	var queries = [];
	var completeData = [];
	var name;
	var count = 0;
	for (name in selectedCompanies) {
		if (selectedCompanies[name]) {
			count++;
		}
	}
	for (name in selectedCompanies) {
		if (selectedCompanies[name]) {
			$.ajax({
				url: "/api/news/" + name,
				type: 'GET',
				beforeSend: function(xhr) { // jshint ignore:line
					xhr.setRequestHeader('offset', 0);
					xhr.setRequestHeader('length', 20);
				},
				success: function(data) { // jshint ignore:line
					completeData = completeData.concat(data);
					count--;
					if (count === 0) {
						for (var i = 0; i < completeData.length; i++) {
							var zip;
							var companyObj = getCompanyObject(completeData[i].company);
							if (companyObj) {
								zip = companyObj.zipCode;
							}
							completeData[i].zipCode = zip;
						}
						news = completeData;
						createTable();
					}
				}
			});
		}
	}
}

function reloadCompanyList() {
	$('#companyStartTableBody').empty();
	// Fills the table row by row
	if (companies) {
		for (var i = 0; i < companies.length; i++) {
			var companySelected = selectedCompanies[companies[i].key];
			var btnType;
			if (companySelected || showAllCompanies) {
				if (companySelected) {
					btnType = 'success';
				} else {
					btnType = 'default';
				}
				$('#companyStartTableBody').append('<tr><td>' + companies[i].name + '</td><td>' + '<button id="' + companies[i].key + '-btn" class="btn btn-' + btnType + '" onClick="selectCompany(\'' + companies[i].key + '\')"><span class="glyphicon glyphicon-ok" aria-hidden="true"></span></button>' + '</td></tr>');
			}
		}
	}
}

function companyDataLoaded() {
	reloadCompanyList();
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
	reloadData();
}

function showAll() {
	showAllCompanies = !showAllCompanies;
	$('#showAllBtn').toggleClass('btn-default');
	$('#showAllBtn').toggleClass('btn-success');
	reloadCompanyList();
}

/** 
 * Search function for the company list.
 */
function searchCompany() {
	// Declare variables
	var filter, table, tr, td, i, company;
	filter = $('#companyName').val().toUpperCase();
	table = $('#companyStartTableBody');
	tr = table.children('tr');

	// Loop through all table rows, and hide those who don't match the search query
	for (i = 0; i < tr.length; i++) {
		td = tr[i].children;
		if (td[0]) {
			company = getCompanyObject(td[0].innerHTML);
			if (company.name.toUpperCase().indexOf(filter) > -1 || company.zipCode.toUpperCase().indexOf(filter) > -1) {
				tr[i].style.display = '';
			} else {
				tr[i].style.display = 'none';
			}
		}
	}
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
			row += index + ';';
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
			row += '"' + arrData[i][index] + '";';
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
	var utc = new Date().toJSON().slice(0, 10).replace(/-/g, '/');
	JSONToCSVConvertor(news, utc, true);
}
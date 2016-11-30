// public/js/index.js
/**
 * Javascript file for all the funtions used in the configuration section.
 * 
 * @author       Yannick Pferr
 * @author       Tobias Mahncke
 * 
 * @version      2.2
 */

/**
 * Sends the company given in the input field "companyName" to the server.
 */
function postCompany() {
	$.ajax({
		type: 'POST',
		url: '/api/company',
		data: '{"company":"' + $('#companyName').val() + '"}',
		success: function(data) {
			// TODO: reload list of companies
			var table = document.getElementById("companyTable");
			addTableData(table, $("#companyName").val());
		},
		contentType: 'application/json'
	});
}

function showCompanies(data){
	var table = document.getElementById("companyTable");
	console.log(data);
	for (var i = 0; i < data.length; i++){
		addTableData(table, data[i]);
	}
	document.body.appendChild(table);
}

function addTableData(table, data){
	
	var tr = document.createElement('tr');   

	var td1 = document.createElement('td');

	var text1 = document.createTextNode(data);

	td1.appendChild(text1);
	tr.appendChild(td1);
		
	document.getElementById("companyTableBody").appendChild(tr);
}
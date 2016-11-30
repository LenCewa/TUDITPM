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
			$.get("/api/company", function(data){
				showCompanies(data);
			});
		},
		contentType: 'application/json'
	});
}

function showCompanies(){
	
	var table = document.getElementById("companyTable");
	var array = data.split("\n");
	for (var i = 0; i < 3; i++){
		var tr = document.createElement('tr');   

		var td1 = document.createElement('td');

		var text1 = document.createTextNode(array[i]);

		td1.appendChild(text1);
		tr.appendChild(td1);
		
		table.appendChild(tr);
	}
	document.body.appendChild(table);
}
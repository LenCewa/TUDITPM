/**
 * Javascript file for all the funtions used in the configuration section.
 * 
 * @author       Yannick Pferr
 * @author       Tobias Mahncke
 * 
 * @version      2.1
 */

/**
 * Sends the company given in the input field "companyName" to the server.
 */
function postCompany() {
	alert('Click ' + $('#companyName').val());
	$.ajax({
		type: 'POST',
		url: '/api/company',
		data: '{"company":"' + $('#companyName').val() + '"}',
		success: function(data) {
			// TODO: reload list of companies
		},
		contentType: 'application/json'
	});
}
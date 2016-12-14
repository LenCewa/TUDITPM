'use strict';
// app/news.js

/**
 * Contains all functions to manipulate the list of companies
 * 
 * @author       Yannick Pferr <yannick.pferr@stud.tu-darmstadt.de>
 * @author       Tobias Mahncke <tobias.mahncke@stud.tu-darmstadt.de>
 * @version      3.1
 *
 */
module.exports = function(app, client) {
	console.log('news routes loading');
	/**
	 *  Returns all the news
	 *  @param req The HTTP request object
	 *  @param res The HTTP response object
	 */
	app.get('/api/news/:key', function(req, res) {
		// Gets a key from redis, returns null if key is not found
		client.get(req.params.key, function(err, reply) {	
			if (err) {
				return res.status(500).send({
					err: {
						de: 'Fehler beim Zugriff auf die Meldungen. Bitte informieren Sie einen Administrator.',
						en: 'Accessing the news failed. Please contact an adminstrator.',
						err: err
					}
				});
			}
			if(reply === null){
				return res.status(404).send({
					err: {
						de: 'Der angegebene Schlüssel konnte nicht gefunden werden.',
						en: 'No data found for the given key.',
					}
				});
			}
			var newsArray = JSON.parse(reply).Meldungen;
			if (newsArray === undefined){
				return res.status(500).send({
						err: {
							de: 'Fehler beim Zugriff auf die Meldungen. Bitte informieren Sie einen Administrator.',
							en: 'Accessing the news failed. Please contact an adminstrator.',
						}
					});
			}
			return res.send(newsArray);
		});
	});
};
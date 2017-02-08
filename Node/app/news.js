'use strict';
// app/news.js

/**
 * Contains all functions to manipulate the list of news
 * 
 * @author       Yannick Pferr <yannick.pferr@stud.tu-darmstadt.de>
 * @author       Tobias Mahncke <tobias.mahncke@stud.tu-darmstadt.de>
 * @version      6.0
 *
 */
var server = require('../server');
var connections = require('../config/connections.conf.json')[process.env.NODE_ENV];
var mongodb = require('mongodb');

module.exports = function(app, client) {
	console.log('news routes loading');
	/**
	 *  Returns all the news
	 *  @param req The HTTP request object
	 *  @param res The HTTP response object
	 */
	app.get('/api/news/:key', function(req, res) {
		// Gets the length for a key from redis, returns null if key is not found
		client.llen(req.params.key, function(err, length) {
			if (err) {
				return res.status(500).send({
					err: {
						de: 'Fehler beim Zugriff auf die Meldungen. Bitte informieren Sie einen Administrator.',
						en: 'Accessing the news failed. Please contact an adminstrator.',
						err: err
					}
				});
			}
			// Gets a key from redis, returns null if key is not found
			client.lrange([req.params.key, 0, length], function(err, reply) {
				if (err) {
					return res.status(500).send({
						err: {
							de: 'Fehler beim Zugriff auf die Meldungen. Bitte informieren Sie einen Administrator.',
							en: 'Accessing the news failed. Please contact an adminstrator.',
							err: err
						}
					});
				}
				if (reply === null) {
					return res.status(404).send({
						err: {
							de: 'Der angegebene Schlüssel konnte nicht gefunden werden.',
							en: 'No data found for the given key.',
						}
					});
				}
				var newsArray = [];
				for (var i = 0; i < reply.length; i++) {
					newsArray[i] = JSON.parse(reply[i]);
				}
				if (newsArray === undefined) {
					return res.status(500).send({
						err: {
							de: 'Fehler beim Zugriff auf die Meldungen. Bitte informieren Sie einen Administrator.',
							en: 'Accessing the news failed. Please contact an adminstrator.',
						}
					});
				}
				return res.send({
					length: length,
					news: newsArray
				});
			});
		});
	});
	/**
	 *  Deletes single news from the enhanced database
	 *  @param req The HTTP request object
	 *  @param res The HTTP response object
	 */
	app.delete('/api/news/:company/:id', function(req, res) {
		mongodb.connect(connections.mongodb.news, function(err, db) {
			if (err) {
				return res.status(500).send({
					err: {
						de: 'Fehler beim Zugriff auf die Meldungen. Bitte informieren Sie einen Administrator.',
						en: 'Accessing the news failed. Please contact an adminstrator.',
					}
				});
			}
			//Open collection
			var collection = db.collection(req.params.company, function(err, collection) {});

			//Store collection in array
			collection.find().toArray(function(err, news) {
				if (err) {
					return res.status(500).send({
						err: {
							de: 'Fehler beim Zugriff auf die Meldungen. Bitte informieren Sie einen Administrator.',
							en: 'Accessing the news failed. Please contact an adminstrator.',
						}
					});
				}
				var newsToDelete;
				for (var i = 0; i < news.length; i++) {
					if (news[i]._id.equals(req.params.id)) {
						newsToDelete = news[i];
						news.splice(i, 1);
						break;
					}
				}
				if (newsToDelete) {
					collection.remove(newsToDelete, function(err) {
						if (err) {
							return res.status(500).send({
								err: {
									de: 'Fehler beim Zugriff auf die Meldungen. Bitte informieren Sie einen Administrator.',
									en: 'Accessing the news failed. Please contact an adminstrator.',
									err: err
								}
							});
						}
						client.send_command('ltrim', [req.params.company, 1, 0], function(err) {
							if (err) {
								console.log(err);
							}
							if (news.length > 0) {
								var pushNews = function(array) {
									var singleNews = JSON.stringify(array.pop());
									console.log(singleNews);
									client.send_command('lpush', [req.params.company, singleNews], function(err) {
										if (err) {
											console.log(err);
										}
										if (array.length > 0) {
											pushNews(array);
										} else {
											return res.status(204).send();
										}
									});
								};
								pushNews(news);
							} else {
								return res.status(204).send();
							}
						});
					});
				} else {
					return res.status(400).send({
						err: {
							de: 'Die Meldung konnte nicht gelöscht werden, da sie nicht vorhanden ist.',
							en: 'Can not delete the news cause it does not exist.'
						}
					});
				}
			});
		});
	});
};
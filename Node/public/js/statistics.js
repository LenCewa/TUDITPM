'use strict';
// public/js/statistics.js
/**
 * Javascript file for all the funtions used to draw the graph.
 * 
 * @author       Yannick Pferr
 * 
 * @version      6.0
 */

/**
 * Draws a Graph with the given data
 *
 * @param data - the x and y data to be drawn
 * @param isFirst - should be true if data is for companies, else false
 */
function drawDiagram(data, isFirst) {
	var svg = d3.select("svg"),
		margin = {
			top: 50,
			right: 40,
			bottom: 100,
			left: 80,
			label: 30,
		},
		width = +svg.attr("width") / 2 - 2 * margin.left,
		height = +svg.attr("height") - margin.bottom - margin.top - margin.label;

	var x = d3.scaleBand().rangeRound([0, width]).padding(0.1),
		y = d3.scaleLinear().rangeRound([height, 0]);

	var g = svg.append("g");

	if (isFirst) {
		g.attr("transform", "translate(" + margin.left + "," + margin.top + ")");
	} else {
		g.attr("transform", "translate(" + (svg.attr("width") / 2 + margin.left) + "," + margin.top + ")");
	}

	x.domain(data.map(function(d) {
		return d.xData;
	}));
	y.domain([0, d3.max(data, function(d) {
		return d.frequency;
	})]);

	g.append("g")
		.attr("class", "axis axis--x")
		.attr("transform", "translate(0," + height + ")")
		.call(d3.axisBottom(x))
		.selectAll("text")	
			.style("text-anchor", "end")
			.attr("dx", "-.8em")
			.attr("dy", ".15em")
			.attr("transform", function(d) { return "rotate(-65)"; });

	g.append("g")
		.attr("class", "axis axis--y")
		.call(d3.axisLeft(y));

	g.selectAll(".bar")
		.data(data)
		.enter().append("rect")
		.attr("class", "bar")
		.attr("x", function(d) {
			return x(d.xData);
		})
		.attr("y", function(d) {
			return y(d.frequency);
		})
		.attr("width", x.bandwidth())
		.attr("height", function(d) {
			return height - y(d.frequency);
		});
}

/**
 * Loads the monthList key from Redis and counts the appearance of companies and keywords
 */
function loadMonthList() {
	$.ajax({
		url: "/api/news/" + "monthList",
		type: 'GET',
		success: function(news) { // jshint ignore:line
			var mapCompany = {};
			var keyCompany;

			var mapKeyword = {};
			var keyKeyword;
			for (var i = 0; i < news.length; i++) {

				keyCompany = news.news[i].company;
				keyKeyword = news.news[i].keyword;

				console.log(keyCompany);
				
				if (keyCompany in mapCompany) {
					mapCompany[keyCompany] += 1;
				} else {
					mapCompany[keyCompany] = 1;
				}

				if (keyKeyword in mapKeyword) {
					mapKeyword[keyKeyword] += 1;
				} else {
					mapKeyword[keyKeyword] = 1;
				}
			}

			var dataCompany = [];
			for (keyCompany in mapCompany) {
				dataCompany.push({
					xData: keyCompany,
					frequency: mapCompany[keyCompany]
				});
			}

			var dataKeyword = [];
			for (keyKeyword in mapKeyword) {
				dataKeyword.push({
					xData: keyKeyword,
					frequency: mapKeyword[keyKeyword]
				});
			}

			drawDiagram(dataCompany, true);
			drawDiagram(dataKeyword, false);
		}
	});
}
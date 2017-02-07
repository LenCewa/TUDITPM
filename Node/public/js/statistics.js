'use strict';
// public/js/statistics.js
/**
 * Javascript file for all the funtions used to draw the graph.
 * 
 * @author       Yannick Pferr
 * 
 * @version      6.0
 */
function drawDiagram(data, isFirst){
	
			var svg = d3.select("svg"),
				margin = {
					top: 50,
					right: 40,
					bottom: 50,
					left: 80,
					label: 30,
				},
				width = +svg.attr("width")/2 - 2*margin.left,
				height = +svg.attr("height") - margin.bottom - margin.top;

			var x = d3.scaleBand().rangeRound([0, width]).padding(0.1),
				y = d3.scaleLinear().rangeRound([height, 0]);

			var g = svg.append("g");
			
			if (isFirst){
				g.attr("transform", "translate(" + margin.left + "," + margin.top + ")");
				g.append("text")
				.attr("x", margin.left)
				.attr("y", height + margin.label)
				.style("text-anchor", "m")
				.text("Unternehmen");
			}
			else {
				g.attr("transform", "translate(" + (svg.attr("width")/2 + margin.left) + "," + margin.top + ")");
				g.append("text")
				.attr("x", margin.left)
				.attr("y", height + margin.label)
				.style("text-anchor", "m")
				.text("Schlagwörter");
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
				.call(d3.axisBottom(x));

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
				
			g.append("text")
				.attr("x", 0)
				.attr("y", -margin.label)
				.attr("transform", "rotate(-90)")
				.style("text-anchor", "end")
				.text("Vorkommen in den letzten 30 Tagen");
}

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
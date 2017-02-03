'use strict';
// public/js/statistics.js
/**
 * Javascript file for all the funtions used to draw the graph.
 * 
 * @author       Yannick Pferr
 * 
 * @version      6.0
 */
 
function loadDiagram(){
	$.ajax({
		url: "/api/news/" + "monthList",
		type: 'GET',
		beforeSend: function(xhr) { // jshint ignore:line
			xhr.setRequestHeader('offset', 0);
			xhr.setRequestHeader('length', 100);
		},
		success: function(news) { // jshint ignore:line
			var map = {};
			for (var i = 0; i < news.length; i++){
				var key = news[i].company;
				if (key in map){
					map[key] += 1; 
				}
				else {
					map[key] = 1;
				}
			}
			var data = [];
			for (var key in map){
				data.push({company: key, frequency: map[key]});
			}
			
			var svg = d3.select("svg"),
			margin = {top: 20, right: 20, bottom: 30, left: 40},
			width = +svg.attr("width") - margin.left - margin.right,
			height = +svg.attr("height") - margin.top - margin.bottom;

			var x = d3.scaleBand().rangeRound([0, width]).padding(0.1),
				y = d3.scaleLinear().rangeRound([height, 0]);

			var g = svg.append("g")
				.attr("transform", "translate(" + margin.left + "," + margin.top + ")");


			x.domain(data.map(function(d) { return d.company; }));
			y.domain([0, d3.max(data, function(d) { return d.frequency; })]);

		    g.append("g")
				.attr("class", "axis axis--x")
				.attr("transform", "translate(0," + height + ")")
				.call(d3.axisBottom(x));

			g.append("g")
				.attr("class", "axis axis--y")
				.call(d3.axisLeft(y))

			g.selectAll(".bar")
				.data(data)
				.enter().append("rect")
				.attr("class", "bar")
				.attr("x", function(d) { return x(d.company); })
				.attr("y", function(d) { return y(d.frequency); })
				.attr("width", x.bandwidth())
				.attr("height", function(d) { return height - y(d.frequency); });
		}
	});
 }
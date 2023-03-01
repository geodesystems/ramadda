
function D3Skewt(divid, args, jsonData) {
    this.divid = divid;
    this.jsonData = jsonData;
    this.options = {
        skewtWidth: 700,
        skewtHeight: 700,
        hodographWidth: 300,
        showHodograph: true,
        showText: true,
        showTimes: true,
        textPlace:"below",
        windStride: 1,
	barbSkip:-1
    };
    if (args) {
        $.extend(this.options, args);
    }

    var _deg2rad = (Math.PI / 180);
    var _tan = Math.tan(55 * _deg2rad);
    this._margin = _margin = [20, 40, 20, 35];
    //constants
    $.extend(this, {
        deg2rad: _deg2rad,
        tan: _tan,
        margin: _margin,
        skewtWidth: this.options.skewtWidth - _margin[1] - _margin[3],
        skewtHeight: this.options.skewtHeight - _margin[0] - _margin[2],
        hodographWidth: this.options.hodographWidth,
        basep: 1050,
        topp: 100,
        plines: [1000, 850, 700, 500, 300, 200, 100],
        pticks: [950, 900, 800, 750, 650, 600, 550, 450, 400, 350, 250, 150],
        barbsize: 25,
        clipperId: this.divid +"_clipper",
        windStride: this.options.windStride
    });

    this.parseData(this.jsonData);
    this.initUI();
    let skewt = this;
    this.loadData();
}


D3Skewt.prototype = {
    initUI: function() {
	var skewt = this;
	this.mainBoxId = this.divid + "_mainbox";
	this.hodoBoxId = this.divid + "_hodobox";
	this.textBoxId = this.divid + "_textbox";
	var hodoStyle = "vertical-align:top;";
	var textHeight = skewt.skewtHeight-skewt.hodographWidth;
	if (this.options.showHodograph) {
	    hodoStyle += "display:inline-block;";
	} else {
	    hodoStyle += "display:none;";
	}
	var textStyle = "vertical-align:top;width:100%;overflow-y:auto;max-height:" + textHeight+"px;";
	if (this.options.showText) {
	    textStyle += "display:inline-block;";
	} else {
	    textStyle += "display:none;";
	}

	var doTable = this.options.showHodograph || this.options.showText;
	var html = "";
	this.numberOfTimes = 1;
	if(this.numberOfTimes>1) {
	    if (this.options.showTimes)
		html += this.createTimeline(this.numberOfTimes);
	}
	if(doTable) {
	    html+="<table border=0 width=100%><tr valign=top><td align=right>";
	}
	html += "<div style='width:100%;' id='" + this.mainBoxId + "'></div>";
	if(doTable) {
	    html +="</td><td>";
	}
	html += "<div style='" + hodoStyle + "' id='" + this.hodoBoxId + "'></div>";
	if(this.options.textPlace == "below")
	    html += "<br>";
	html += "<div style='" + textStyle + "' id='" + this.textBoxId + "'></div>";
	if(doTable) {
	    html += "</td></tr></table>";
	}
	$("#" + this.divid).html(html);

	this.initSvg();
	skewt.makeBarbTemplates();
	skewt.drawBackground();
	skewt.drawToolTips();

	$(document).ready(function() {
	    t = $("#" + skewt.timelineId + " .skewt-rollover ");
	    $("#" + skewt.timelineId + " .skewt-rollover ").mouseover(function() {
		var i = $(this).attr('id');
		// change class for rollover
		$("#" + skewt.timelineId + " .skewt-rollover ").removeClass("skewt-selected");
		$(this).addClass("skewt-selected");
		skewt.updateData(i);
	    });
	});
    },
    getSkewtWidth: function() {
	return this.skewtWidth;
    },
    getSkewtHeight: function() {
	return this.skewtHeight;
    },
    getHodographWidth: function() {
	return this.hodographWidth;
    },
    initSvg: function() {
	let skewt = this;
	let hw = this.getHodographWidth();
	this.x = d3.scaleLinear().range([0, skewt.getSkewtWidth()]).domain([-45, 50]);
	this.y = d3.scaleLog().range([0, skewt.getSkewtHeight()]).domain([skewt.topp, skewt.basep]);
	
	//0, 150
	this.windSpeedMax = 80;	    
	this.windSpeedStep = 10;
	//10, 80, 10
	//10, 80, 20	

	if(this.windspeedRange) {
	    let max = this.windspeedRange.max;
	    if(max<this.windSpeedMax) {
		this.windSpeedMax = 1.2*max;
		this.windSpeedStep = Math.ceil(this.windSpeedMax/8);
	    }
	}

	this.hodoRange = d3.scaleLinear().range([0, hw]).domain([0, this.windSpeedMax*2]);
	this.y2 = d3.scaleLinear();
	this.xAxis = d3.axisBottom().scale(this.x).tickSize(0, 0).ticks(10);
	this.yAxis = d3.axisLeft().scale(this.y).tickSize(0, 0).tickValues(this.plines)
	    .tickFormat(d3.format(".0d"));
	this.yAxis2 = d3.axisRight().scale(this.y).tickSize(5, 0).tickValues(this.pticks); // just for ticks
	//this.yAxis2 = d3.svg.axis().scale(this.y2).orient("right").tickSize(3,0).tickFormat(d3.format(".0d"));
	this.line = d3.line()
	    .curve(d3.curveLinear)
	    .x(function(d, i) {
		return skewt.x(d.temperature) + (skewt.y(skewt.basep) - skewt.y(d.pressure)) / skewt.tan;
	    })
        //.x(function(d,i) { return skewt.x(d.temperature); })
	    .y(function(d, i) {
		return skewt.y(d.pressure);
	    });
	
	this.line2 = d3.line()
	    .curve(d3.curveLinear)
	    .x(function(d, i) {
		return skewt.x(d.dewpoint) + (skewt.y(skewt.basep) - skewt.y(d.pressure)) / skewt.tan;
	    })
	    .y(function(d, i) {
		return skewt.y(d.pressure);
	    });

	this.hodoline = d3.radialLine()
	    .radius(function(d) {
		return skewt.hodoRange(d.windspeed);
	    })
	    .angle(function(d) {
		return (d.winddir + 180) * (Math.PI / 180);
	    });

	// bisector function for tooltips    
	this.bisectTemp = d3.bisector(function(d) {
	    return d.pressure;
	}).left;


	this.svg = d3.select("#" + this.mainBoxId).append("svg")
	    .attr("width", skewt.getSkewtWidth() + skewt.margin[1] + skewt.margin[3])
	    .attr("height", skewt.getSkewtHeight() + skewt.margin[0] + skewt.margin[2])
	    .append("g")
	    .attr("transform", "translate(" + skewt.margin[3] + "," + skewt.margin[0] + ")");

	//xxxx
	// create svg container for hodograph
	this.svghodo = d3.select("#" + this.hodoBoxId).append("svg")
	    .attr("width", skewt.getHodographWidth())
	    .attr("height", skewt.getHodographWidth() + skewt._margin[0])
	    .append("g")
	    .attr("transform", "translate(" + hw/2+"," + (hw/2+skewt._margin[0])+ ")");

	this.svgtext = d3.select("#" + this.textBoxId).append("svg").attr("width", 300).attr("height", 400)
	    .append("g").attr("transform", "translate(0,50)");

	skewt.skewtgroup = skewt.svg.append("g").attr("class", "skewt"); // put skewt lines in this group
	skewt.barbgroup = skewt.svg.append("g").attr("class", "windbarb"); // put barbs in this group
	skewt.hodogroup = skewt.svghodo.append("g").attr("class", "hodo"); // put hodo stuff in this group

    },
    createTimeline: function(hours) {
	this.timelineId = this.divid + "_timeline";
	var html = "<div class='skewt-times' id='" + this.timelineId + "'>";
	for (var i = 0; i <= hours; i++) {
	    var label = i + "";
	    if (i <= 9) label = "0" + label;
	    html += "<div class='skewt-rollover " + (i == 0 ? "skewt-selected" : "") + "' id='" + i + "'>" + label + "</div>";
	}
	html += "</div>";
	return html;
    },
    drawBackground: function() {
	var skewt = this;
	var svghodo = d3.select("#" + this.hodoBoxId + " svg g").append("g").attr("class", "hodobg");
	var svg = d3.select("#" + this.mainBoxId + " svg g").append("g").attr("class", "skewtbg");

	var dryline = d3.line()
	    .curve(d3.curveLinear)
	    .x(function(d, i) {
		return skewt.x((273.15 + d) / Math.pow((1000 / pp[i]), 0.286) - 273.15) + (skewt.y(skewt.basep) - skewt.y(pp[i])) / skewt.tan;
	    })
	    .y(function(d, i) {
		return skewt.y(pp[i])
	    });
        
	var moistline = d3.line()
	    .curve(d3.curveLinear)
	    .x(function(d,i) { 
		return skewt.x(d) + (skewt.y(skewt.basep)-skewt.y(p_levels[i]))/skewt.tan;
	    })
	    .y(function(d,i) { 
		return skewt.y(p_levels[i])
	    });


	// Add clipping path
	svg.append("clipPath")
	    .attr("id", skewt.clipperId)
	    .append("rect")
	    .attr("x", 0)
	    .attr("y", 0)
	    .attr("width", skewt.getSkewtWidth())
	    .attr("height", skewt.getSkewtHeight());

	// Skewed temperature lines
	svg.selectAll("gline")
	    .data(d3.range(-100, 45, 10))
	    .enter().append("line")
	    .attr("x1", function(d) {
		return skewt.x(d) - 0.5 + (skewt.y(skewt.basep) - skewt.y(100)) / skewt.tan;
	    })
        //.attr("x1", function(d) { return skewt.x(d)-0.5; })
	    .attr("x2", function(d) {
		return skewt.x(d) - 0.5;
	    })
	    .attr("y1", 0)
	    .attr("y2", this.getSkewtHeight())
	    .attr("class", function(d) {
		if (d == 0) {
		    return "skewt-grid  skewt-grid-temperature-zero";
		} else {
		    return "skewt-grid  skewt-grid-temperature"
		}
	    })
	    .attr("clip-path", "url(#"+ skewt.clipperId+")");
	//.attr("transform", "translate(0," + this.getSkewtHeight() + ") skewX(-30)");

	// Logarithmic pressure lines
	svg.selectAll("gline2")
	    .data(this.plines)
	    .enter().append("line")
	    .attr("x1", 0)
	    .attr("x2", this.getSkewtWidth())
	    .attr("y1", function(d) {
		return skewt.y(d);
	    })
	    .attr("y2", function(d) {
		return skewt.y(d);
	    })
	    .attr("class", "skewt-line-pressure");

	// create array to plot dry adiabats
	var pp = d3.range(this.topp, this.basep + 1, 10);
	var dryad = d3.range(-30, 240, 20);
	var all = [];
	for (i = 0; i < dryad.length; i++) {
	    var z = [];
	    for (j = 0; j < pp.length; j++) {
		z.push(dryad[i]);
	    }
	    all.push(z);
	}

	// Draw dry adiabats
	svg.selectAll(".dryline")
	    .data(all)
	    .enter().append("path")
	    .attr("class", "skewt-grid skewt-grid-adiabat")
	    .attr("clip-path", "url(#" + skewt.clipperId+")")
	    .attr("d", dryline);

	// Draw moist adiabats (6,10,14,18,22,26,30C) at these t/p coordinates
	var p_levels = [1000,988,975,962,950,938,925,900,875,850,825,800,750,700,650,600,550,500,450,400,350,300,275,250,225,200];
	var moist_temps = [ 
	    [ 6.0,5.4,4.8,4.2,3.6,2.9,2.3,0.9,-0.5,-2.0,-3.5,-5.1,-8.7,-12.6,-16.9,-21.8,-27.3,-33.5,-40.5,-48.3,-57.1,-67.0,-72.3,-77.9,-84.0,-90.6 ],
	    [ 10.0,9.5,8.9,8.3,7.7,7.1,6.5,5.3,4.0,2.6,1.2,-0.3,-3.5,-7.2,-11.2,-15.8,-21.0,-26.9,-33.7,-41.5,-50.3,-60.5,-65.9,-71.7,-78.0,-84.8 ],
	    [ 14.0,13.5,13.0,12.4,11.9,11.4,10.8,9.7,8.5,7.2,5.9,4.5,1.6,-1.7,-5.4,-9.6,-14.3,-19.9,-26.3,-33.8,-42.6,-52.9,-58.4,-64.4,-70.8,-77.9 ],
	    [ 18.0,17.5,17.0,16.6,16.1,15.6,15.0,14.0,12.9,11.7,10.5,9.3,6.6,3.6,0.3,-3.4,-7.7,-12.6,-18.5,-25.4,-33.8,-44.0,-49.6,-55.7,-62.4,-69.7 ],
	    [ 22.0,21.6,21.1,20.7,20.2,19.7,19.3,18.3,17.3,16.2,15.1,14.0,11.5,8.9,5.9,2.6,-1.2,-5.6,-10.7,-16.8,-24.4,-33.9,-39.4,-45.5,-52.2,-59.8 ],
	    [ 26.0,25.6,25.2,24.8,24.3,23.9,23.5,22.5,21.6,20.6,19.6,18.6,16.4,13.9,11.2,8.3,4.9,1.1,-3.3,-8.6,-15.1,-23.4,-28.3,-33.9,-40.4,-47.9 ],
	    [ 30.0,29.6,29.2,28.8,28.4,28.0,27.6,26.8,25.9,25.0,24.1,23.1,21.0,18.8,16.4,13.7,10.7,7.4,3.5,-1.0,-6.4,-13.3,-17.4,-22.2,-27.8,-34.6 ] 
	];
	svg.selectAll(".moistline")
	    .data(moist_temps)
	    .enter().append("path")
	    .attr("class", "skewt-grid skewt-grid-moist")
	    .attr("clip-path", "url(#" + skewt.clipperId+")")
	    .attr("d", moistline);

	svg.selectAll(".moistlabels")
	    .data([6,10,14,18,22,26,30]).enter().append("text")
	    .attr("x", function (d,i) { 
		return skewt.x(moist_temps[i][moist_temps[i].length-2]) + (skewt.y(skewt.basep)-skewt.y(225))/skewt.tan; 
	    })
	    .attr("y", skewt.y(225))
	    .attr("dy", "0.75em")
	    .attr("class", "skewt-moistlabels")
	    .attr("text-anchor", "middle")
	    .text(function(d) { return d; });

	// Line along right edge of plot
	svg.append("line")
	    .attr("x1", skewt.getSkewtWidth() - 0.5)
	    .attr("x2", skewt.getSkewtWidth() - 0.5)
	    .attr("y1", 0)
	    .attr("y2", skewt.getSkewtHeight())
	    .attr("class", "skewt-grid");

	// draw hodograph background
	svghodo.selectAll(".circles")
	    .data(d3.range(this.windSpeedStep, this.windSpeedMax, this.windSpeedStep))
	    .enter().append("circle")
	    .attr("cx", 0)
	    .attr("cy", 0)
	    .attr("r", function(d) {
		return skewt.hodoRange(d);
	    })
	    .attr("class", "skewt-grid");
	svghodo.selectAll("hodolabels")
	    .data(d3.range(this.windSpeedStep, this.windSpeedMax, this.windSpeedStep*2)).enter().append("text")
	    .attr('x', 0)
	    .attr('y', function(d, i) {
		return skewt.hodoRange(d);
	    })
	    .attr('dy', '0.4em')
	    .attr('class', 'hodolabels')
	    .attr('text-anchor', 'middle')
	    .text(function(d) {
		return Utils.trimDecimals(d,1) + ' kts';
	    });

	// Add axes
	svg.append("g").attr("class", "x axis").attr("transform", "translate(0," + (skewt.getSkewtHeight() - 0.5) + ")").call(skewt.xAxis);
	svg.append("g").attr("class", "y axis").attr("transform", "translate(-0.5,0)").call(skewt.yAxis);
	svg.append("g").attr("class", "y axis ticks").attr("transform", "translate(-0.5,0)").call(skewt.yAxis2);
	//svg.append("g").attr("class", "y axis height").attr("transform", "translate(0,0)").call(skewt.yAxis2);
    },
    makeBarbTemplates: function() {
	let skewt = this;
	let speeds = d3.range(5, 105, 5);
	let barbdef = skewt.svg.append('defs');
	speeds.forEach((d,idx) =>{
	    let thisbarb = barbdef.append('g').attr('id', 'barb' + d);
	    let flags = Math.floor(d / 50);
	    let pennants = Math.floor((d - flags * 50) / 10);
	    let halfpennants = Math.floor((d - flags * 50 - pennants * 10) / 5);
	    let px = skewt.barbsize;
	    // Draw wind barb stems
	    thisbarb.append("line").attr("x1", 0).attr("x2", 0).attr("y1", 0).attr("y2", skewt.barbsize);

	    // Draw wind barb flags and pennants for each stem
	    for (i = 0; i < flags; i++) {
		thisbarb.append("polyline")
		    .attr("points", "0," + px + " -10," + (px) + " 0," + (px - 4))
		    .attr("class", "flag");
		px -= 7;
	    }
	    // Draw pennants on each barb
	    for (i = 0; i < pennants; i++) {
		thisbarb.append("line")
		    .attr("x1", 0)
		    .attr("x2", -10)
		    .attr("y1", px)
		    .attr("y2", px + 4)
		px -= 3;
	    }
	    // Draw half-pennants on each barb
	    for (i = 0; i < halfpennants; i++) {
		thisbarb.append("line")
		    .attr("x1", 0)
		    .attr("x2", -5)
		    .attr("y1", px)
		    .attr("y2", px + 2)
		px -= 3;
	    }
	})
    },
    drawTextLabels: function() {
	var skewt = this;
	spacing = 20;
	var headers = ['Ens Min', 'Ens Mean', 'Ens Max'];
	var labels = ['SBCAPE', 'MLCAPE', 'MUCAPE', '0-1km Shear', '0-1km SRH', '0-3km Shear', '0-3km SRH',
		      '0-6km Shear', '0-6km SRH', 'STP (fixed)',
		      'Bunkers Dir', 'Bunkers Spd'
		     ];

	skewt.svgtext.selectAll("labels")
	    .data(labels).enter().append("text")
	    .attr('x', 70)
	    .attr('y', function(d, i) {
		return spacing * i;
	    })
	    .attr('class', 'index header')
	    .attr('text-anchor', 'end')
	    .text(function(d) {
		return d;
	    });

	skewt.svgtext.selectAll("headers")
	    .data(headers).enter().append("text")
	    .attr('x', function(d, i) {
		return 110 + 80 * i;
	    })
	    .attr('y', -20)
	    .attr('class', 'index header')
	    .attr('text-anchor', 'middle')
	    .text(function(d) {
		return d;
	    });

	//skewt.svgtext.selectAll("barline")
	//  .data(labels).enter().append("line")
	//	.attr('x1', 65)
	//	.attr('x2', 275)
	//	.attr('y1', function(d,i) { return spacing*(i+1)-4.5; })
	//	.attr('y2', function(d,i) { return spacing*(i+1)-4.5; })
	//	.attr('class', 'barline');

	//skewt.svgtext.selectAll("keys")
	//  .data([0,0,0,0,0,0,0]).enter().append("text")
	//    .attr('x', 0)
	//    .attr('y', function(d,i) { return spacing*(i+1)+10; })
	//	.attr('class', 'key')
	//	.attr('text-anchor', 'start')
	//	.text(function(d) { return d; });

	//skewt.svgtext.selectAll("keys2")
	//  .data([5000,5000,5000,100,100,100,10]).enter().append("text")
	//    .attr('x', 300)
	//    .attr('y', function(d,i) { return spacing*(i+1)+10; })
	//	.attr('class', 'key')
	//	.attr('text-anchor', 'end')
	//	.text(function(d) { return d; });

	//skewt.svgtext.selectAll("rect")
	//  .data([20,50,100,50,100,150,75]).enter().append("rect")
	// 	.attr('x', function(d,i) { return d; })
	// 	.attr('y', function(d,i) { return spacing*(i+1)+4; })
	// 	.attr('height', 6)
	// 	.attr('width', function(d,i) { return d/2; })
	// 	.attr('class', 'rectline');
    },
    drawToolTips: function() {
	var skewt = this;
	// Draw T/Td tooltips
	this.focus = skewt.skewtgroup.append("g").attr("class", "focus tmpc").style("display", "none");
	this.focus.append("circle").attr("r", 4);
	this.focus.append("text").attr("x", 9).attr("dy", ".35em");

	this.focus2 = skewt.skewtgroup.append("g").attr("class", "focus dwpc").style("display", "none");
	this.focus2.append("circle").attr("r", 4);
	this.focus2.append("text").attr("x", -9).attr("text-anchor", "end").attr("dy", ".35em");

	this.focus3 = skewt.skewtgroup.append("g").attr("class", "focus").style("display", "none");
	this.focus3.append("text").attr("x", 0).attr("text-anchor", "start").attr("dy", ".35em");

	this.focus4 = skewt.hodogroup.append("g").attr("class", "focus hodo").style("display", "none");
	this.focus4.append("circle").attr("r", 5);

	function mousedown() {
	    if(skewt.options.mouseDownListener)
		skewt.options.mouseDownListener(skewt.d.record);
	}

	function mousemove() {
	    var y0 = skewt.y.invert(d3.mouse(this)[1]); // get y value of mouse pointer in pressure space
	    var i = skewt.bisectTemp(skewt.mouseoverdata, y0, 1, skewt.mouseoverdata.length - 1);
	    var d0 = skewt.mouseoverdata[i - 1];
	    var d1 = skewt.mouseoverdata[i];
	    skewt.d = y0 - d0.pressure > d1.pressure - y0 ? d1 : d0;
	    skewt.highlightData(skewt.d);
	}

	skewt.svg.append("rect")
	    .attr("class", "overlay")
	    .attr("width", skewt.getSkewtWidth())
	    .attr("height", skewt.getSkewtHeight())
	    .on("mouseover", function() {
		skewt.focus.style("display", null);
		skewt.focus2.style("display", null);
		skewt.focus3.style("display", null);
		skewt.focus4.style("display", null);
	    })
	    .on("mouseout", function() {
		skewt.focus.style("display", "none");
		skewt.focus2.style("display", "none");
		skewt.focus3.style("display", "none");
		skewt.focus4.style("display", "none");
	    })
	    .on("mousemove", mousemove)
	    .on("mousedown", mousedown);	    

    },
    highlightRecord:function(record) {
	if(!this.mouseoverdata) return;
	this.mouseoverdata.every(d=>{
	    if(d.record && d.record.equals(record)) {
		this.highlightData(d);
		return false;
	    }
	    return true;
	});

    },
    highlightData:function(d) {
	let skewt = this;
	skewt.d = d;
	//Turn them on
	skewt.focus.style("display","block");
	skewt.focus2.style("display","block");
	skewt.focus3.style("display","block");
	skewt.focus4.style("display","block");	    

	skewt.focus.attr("transform", "translate(" + (skewt.x(skewt.d.temperature) + (skewt.y(skewt.basep) - skewt.y(skewt.d.pressure)) / skewt.tan) + "," + skewt.y(skewt.d.pressure) + ")");
	skewt.focus2.attr("transform", "translate(" + (skewt.x(skewt.d.dewpoint) + (skewt.y(skewt.basep) - skewt.y(skewt.d.pressure)) / skewt.tan) + "," + skewt.y(skewt.d.pressure) + ")");
	skewt.focus3.attr("transform", "translate(0," + skewt.y(skewt.d.pressure) + ")");
	skewt.focus.select("text").text(Math.round(skewt.d.temperature) + "°C");
	skewt.focus2.select("text").text(Math.round(skewt.d.dewpoint) + "°C");
	skewt.focus3.select("text").text("--" + (Math.round(skewt.d.heightagl / 100) / 10) + "km" +" " +
					 Utils.trimDecimals(skewt.d.pressure,2)+"mb"
					);

	let uspd = skewt.d.windspeed * Math.sin((180+skewt.d.winddir)*skewt.deg2rad);
	let vspd = skewt.d.windspeed * Math.cos((180+skewt.d.winddir)*skewt.deg2rad);

	if (!(isNaN(uspd) || isNaN(vspd))) {
	    if (skewt.d.pressure > 200) {
		skewt.focus4.attr("transform", " translate(" + skewt.hodoRange(uspd) + "," + -skewt.hodoRange(vspd) + ")");
	    } else {
		skewt.focus4.attr("transform", " translate(1000,1000)");
	    }
	}

    },
    parseData: function(json) {
	let skewt = this;
	let wsRange = null;
	if(json.wind_speed) {
	    json.wind_speed.forEach(w=>{
		if(isNaN(w)) return;
		if(wsRange==null) {
		    wsRange = {min:w,max:w};
		} else {
		    wsRange.min = Math.min(wsRange.min,w);
		    wsRange.max = Math.max(wsRange.max,w);		    
		}
	    });
	}
	this.windspeedRange = wsRange;
	skewt.interpobjects = [];
	this.numberOfMembers = 1;
	skewt.alldata = json.temperature.map(function(c, k) {
	    let obj = {
		pressure: json.pressure[k],
		height: json.height[k],
		temperature: json.temperature[k],
		dewpoint: json.dewpoint[k], 
		winddir: json.wind_direction[k],
		windspeed: json.wind_speed[k],
		windspeedround: Math.round((json.wind_speed[k]) / 5) * 5,
		heightagl: json.height[k] - +json.height[0],
	    };
	    if(json.records) {
		obj.record = json.records[k];
	    }
	    return obj;
	});

	let data = skewt.alldata;
	// interpolate to given heights for each sounding
	let requestedLevels = [0, 1, 3, 6, 9]; // levels in km agl
	var test = requestedLevels.map(d=> {
	    if (d == 0) {
		return data[0];
	    }
	    d = 1000 * d + data[0].height; // want height AGL
	    for (i = 0; i < data.length; i++) {
		if (data[i].height > d) {
		    break;
		} // since heights increase monotonically
	    }
	    //in case we ran off the end of the array
	    if(!data[i]) return null;
	    var interp = d3.interpolateObject(data[i - 1], data[i]); // interp btw two levels
	    var half = interp(1 - (d - data[i].height) / (data[i - 1].height - data[i].height));
	    return half
	});
	skewt.interpobjects.push(test);
	return skewt.alldata;
    },
    drawFirstHour: function() {
	var skewt = this;
	// draw initial set of lines
	skewt.tlines = skewt.skewtgroup.selectAll("tlines")
	    .data(skewt.tlinetest[0]).enter().append("path")
	    .attr("class", function(d, i) {
		return (i < 10) ? "skewt-line-temperature member" : "skewt-line-temperature mean"
	    })
	    .attr("clip-path", "url(#" + skewt.clipperId+")")
	    .attr("d", skewt.line);

	skewt.tdlines = skewt.skewtgroup.selectAll("tdlines")
	    .data(skewt.tlinetest[0]).enter().append("path")
	    .attr("class", function(d, i) {
		return (i < 10) ? "skewt-line-dewpoint member" : "skewt-line-dewpoint mean"
	    })
	    .attr("clip-path", "url(#" + skewt.clipperId +")")
	    .attr("d", skewt.line2);

	skewt.holines = skewt.hodogroup.selectAll("hodolines")
	    .data(skewt.hodobarbstest[0]).enter().append("path")
	    .attr("class", function(d, i) {
		return (i > 0) ? "hodoline member" : "hodoline mean"
	    })
	    .attr("d", skewt.hodoline);


	var tmp = skewt.hodobarbstest[0][0];
	//            tmp = skewt.flattened;
	/*
	  skewt.hododots = skewt.hodogroup.selectAll('hododots')
          .data(tmp).enter().append("circle")
          .attr("r", function(d, i) {
          return (i < 50) ? 2 : 4
          })
          .attr("cx", function(d, i) {
          return skewt.hodoRange(d.windspeed * Math.sin((180 + d.winddir) * skewt.deg2rad));
          })
          .attr("cy", function(d, i) {
          return -skewt.hodoRange(d.windspeed * Math.cos((180 + d.winddir) * skewt.deg2rad));
          })
          .attr("class", function(d, i) {
          return "hododot hgt" + parseInt(d.heightagl / 1000)
          });
        */

	var barbs = skewt.hodobarbstest[0][0];
	//            barbs = skewt.barbstest[0][skewt.numberOfMembers-1]

	this.displayWindBarbs();
    },
    displayWindBarbs:function() {
	let skewt=this;
	let barbs = this.barbs;
	//check for first time
	if(skewt.options.barbSkip<0) {
	    skewt.options.barbSkip=1;
	    while(barbs.length/skewt.options.barbSkip>200) {
		skewt.options.barbSkip++;
	    }
	}
	let barbSkip = skewt.options.barbSkip;
	skewt.allbarbs = skewt.barbgroup.selectAll("barbs")
	    .data(barbs).enter().append("use")
	    .attr("xlink:href", function(d,idx) {
		if(barbSkip>1 && idx%barbSkip!=0) return "#foo";
		return "#barb" + d.windspeedround;
	    })
	    .attr("transform", function(d, i) {
		return "translate(" + skewt.getSkewtWidth() + "," + skewt.y(d.pressure) + ") rotate(" + (d.winddir + 180) + ")";
	    })
	
	skewt.allbarbs.style('cursor', 'pointer')
	skewt.allbarbs.on("click", function(d,e) {
	    skewt.allbarbs.remove();
	    if(d3.event.shiftKey) {
		skewt.options.barbSkip--;
		skewt.options.barbSkip = Math.max(1,skewt.options.barbSkip);
	    } else {
		skewt.options.barbSkip++;
	    }
	    skewt.displayWindBarbs();
	});

	skewt.allbarbs.append('title').text(function(d) {return "Wind speed:" + d.windspeedround+"\n"+"Wind direction:" + d.winddir+"\n"+
							 "Click to show less.\nShift-click to show more"})

    },

    drawFirstHourText: function() {
	if(!skewt.indices) return;
	units = [' J/kg', ' J/kg', ' J/kg', ' kts', '', ' kts', '', ' kts', '', '', '°', ' kts', ''];
	skewt.mins = skewt.svgtext.selectAll("mins")
	    .data(skewt.indices[0].min.slice(0, 12)).enter().append("text") // only plot first 12 parameters
	    .attr('x', 110)
	    .attr('y', function(d, i) {
		return spacing * i;
	    })
	    .attr('class', 'index')
	    .attr('text-anchor', 'middle')
	    .text(function(d, i) {
		return d + units[i]
	    });

	skewt.means = skewt.svgtext.selectAll("means")
	    .data(skewt.indices[0].mean.slice(0, 12)).enter().append("text")
	    .attr('x', 190)
	    .attr('y', function(d, i) {
		return spacing * i;
	    })
	    .attr('class', 'index')
	    .attr('text-anchor', 'middle')
	    .text(function(d, i) {
		return d + units[i]
	    });

	skewt.maxs = skewt.svgtext.selectAll("maxs")
	    .data(skewt.indices[0].max.slice(0, 12)).enter().append("text")
	    .attr('x', 270)
	    .attr('y', function(d, i) {
		return spacing * i;
	    })
	    .attr('class', 'index')
	    .attr('text-anchor', 'middle')
	    .text(function(d, i) {
		return d + units[i]
	    });

	skewt.lcl = skewt.skewtgroup.selectAll("lcl")
	    .data([skewt.indices[0]]).enter().append("rect")
	    .attr('x', skewt.getSkewtWidth() - 70)
	    .attr('y', function(d, i) {
		return skewt.y(d.min[12])
	    })
	    .attr('width', 20)
        //.attr('height', 3)
	    .attr('height', function(d, i) {
		return skewt.y(d.max[12]) - skewt.y(d.min[12]);
	    })
	    .attr('class', 'rectline');

	skewt.lclmean = skewt.skewtgroup.selectAll("lclmean")
	    .data([skewt.indices[0]]).enter().append("rect")
	    .attr('x', skewt.getSkewtWidth() - 70)
	    .attr('y', function(d, i) {
		return skewt.y(d.mean[12])
	    })
	    .attr('width', 20)
	    .attr('height', 2)
	    .attr('class', 'rectline2');

	skewt.lcltext = skewt.skewtgroup.selectAll("lcltext")
	    .data([skewt.indices[0]]).enter().append("text")
	    .attr('x', skewt.getSkewtWidth() - 70)
	    .attr('y', function(d, i) {
		return skewt.y(d.mean[12])
	    })
	    .attr('dy', '0.4em')
	    .attr('text-anchor', 'end')
	    .attr('class', 'lcltext')
	    .text(function(d, i) {
		return d.mean[13] + " m";
	    });
    },

    updateData: function(i) {
	var skewt = this;
	// update data for lines, barbs, dots, stats
	skewt.tlines.data(skewt.tlinetest[i]).attr("d", skewt.line);
	skewt.tdlines.data(skewt.tlinetest[i]).attr("d", skewt.line2);
	var displayBarbdata = skewt.barbstest[i][skewt.numberOfMembers-1];
	skewt.allbarbs.data(skewt.barbstest[i][skewt.numberOfMembers-1])
	    .attr("xlink:href", function(d) {
		return "#barb" + d.windspeedround;
	    })
	    .attr("transform", function(d, i) {
		return "translate(" + skewt.getSkewtWidth() + "," + skewt.y(d.pressure) + ") rotate(" + (d.winddir + 180) + ")";
	    });
	skewt.holines.data(skewt.hodobarbstest[i]).attr("d", skewt.hodoline);

	/*
	  skewt.hododots.data(skewt.flattened.slice(i * 55, i * 55 + 55))
          .attr("cx", function(d) {
          return skewt.hodoRange(d.windspeed * Math.sin((180 + d.winddir) * skewt.deg2rad));
          })
          .attr("cy", function(d) {
          return -skewt.hodoRange(d.windspeed * Math.cos((180 + d.winddir) * skewt.deg2rad));
          });
	*/

	skewt.means.data(skewt.indices[i].mean).text(function(d, i) {
	    return d + units[i]
	});
	skewt.mins.data(skewt.indices[i].min).text(function(d, i) {
	    return d + units[i]
	});
	skewt.maxs.data(skewt.indices[i].max).text(function(d, i) {
	    return d + units[i]
	});
	skewt.lcl.data([skewt.indices[i]])
	    .attr('y', function(d, i) {
		return skewt.y(d.min[12])
	    })
	    .attr('height', function(d, i) {
		return skewt.y(d.max[12]) - skewt.y(d.min[12]);
	    });
	skewt.lclmean.data([skewt.indices[i]]).attr('y', function(d, i) {
	    return skewt.y(d.mean[12])
	});
	skewt.lcltext.data([skewt.indices[i]])
	    .attr('y', function(d, i) {
		return skewt.y(d.mean[12])
	    })
	    .text(function(d, i) {
		return d.mean[13] + " m";
	    });
	skewt.mouseoverdata = skewt.tlinetest[i][skewt.numberOfMembers-1].slice(0).reverse();
    },
    loadData: function() {
	let skewt = this;
	let json = this.jsonData;
        this.hodobarbstest = [];
        this.tlinetest = [];
        this.interpdots = [];
        this.barbstest = [];

        var temp = [], temp2 = [],  temp3 = [];
        parsedCSV = this.alldata.filter(d=>{
	    return (d.temperature > -1000 && d.dewpoint > -1000);
        });
        this.barbs = parsedCSV.filter(d=> {
	    return (d.winddir >= 0 && d.windspeed >= 0 && d.pressure >= this.topp);
        });
        this.hodobarbs = this.barbs.filter(d=> {
	    return (d.pressure >= 200);
        });
        if (skewt.windStride > 1) {
	    let newbarbs = this.barbs.filter(function(values,index,d) {
                return index % skewt.windStride == 0;
	    });
	    this.barbs = newbarbs;
        }
        this.interpdot = this.interpobjects[0]
        temp.push(this.hodobarbs);
        temp2.push(parsedCSV);
        temp3.push(this.barbs);
        this.interpdots.push(this.interpdot);
        this.hodobarbstest.push(temp);
        this.tlinetest.push(temp2);
        this.barbstest.push(temp3);
        // need this for dots for some reason
        this.mouseoverdata = this.tlinetest[0][this.numberOfMembers-1].slice(0).reverse();
        this.flattened = this.interpdots.reduce(function(a, b) {
            return a.concat(b);
        });
        this.flattened = this.interpdots;
        this.flattened=parsedCSV;
        this.drawFirstHour();
    }
}

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
        textPlace:"below"
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
        barbsize: 25
    });

    //methods
    $.extend(this, {
        initUI: function() {
            var skewt = this;
            this.mainBoxId = this.divid + "_mainbox";
            this.hodoBoxId = this.divid + "_hodobox";
            this.textBoxId = this.divid + "_textbox";
            var hodoStyle = "vertical-align:top;";
            var textStyle = "vertical-align:top;width:100%;";
            if (this.options.showHodograph) {
                hodoStyle += "display:inline-block;";
            } else {
                hodoStyle += "display:none;";
            }
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
            //jeffmc            skewt.drawTextLabels();
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
            var skewt = this;
            this.x = d3.scale.linear().range([0, skewt.getSkewtWidth()]).domain([-45, 50]);
            this.y = d3.scale.log().range([0, skewt.getSkewtHeight()]).domain([skewt.topp, skewt.basep]);
            var hw = this.getHodographWidth();
            this.r = d3.scale.linear().range([0, hw]).domain([0, 150]);

            this.y2 = d3.scale.linear();
            this.xAxis = d3.svg.axis().scale(this.x).tickSize(0, 0).ticks(10).orient("bottom");
            this.yAxis = d3.svg.axis().scale(this.y).tickSize(0, 0).tickValues(this.plines)
                .tickFormat(d3.format(".0d")).orient("left");
            this.yAxis2 = d3.svg.axis().scale(this.y).tickSize(5, 0).tickValues(this.pticks).orient("right"); // just for ticks
            //this.yAxis2 = d3.svg.axis().scale(this.y2).orient("right").tickSize(3,0).tickFormat(d3.format(".0d"));

            this.line = d3.svg.line()
                .interpolate("linear")
                .x(function(d, i) {
                    return skewt.x(d.temperature) + (skewt.y(skewt.basep) - skewt.y(d.pressure)) / skewt.tan;
                })
                //.x(function(d,i) { return skewt.x(d.temperature); })
                .y(function(d, i) {
                    return skewt.y(d.pressure);
                });

            this.line2 = d3.svg.line()
                .interpolate("linear")
                .x(function(d, i) {
                    return skewt.x(d.dewpoint) + (skewt.y(skewt.basep) - skewt.y(d.pressure)) / skewt.tan;
                })
                .y(function(d, i) {
                    return skewt.y(d.pressure);
                });

            this.hodoline = d3.svg.line.radial()
                .radius(function(d) {
                    return skewt.r(d.windspeed);
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

            var dryline = d3.svg.line()
                .interpolate("linear")
                .x(function(d, i) {
                    return skewt.x((273.15 + d) / Math.pow((1000 / pp[i]), 0.286) - 273.15) + (skewt.y(skewt.basep) - skewt.y(pp[i])) / skewt.tan;
                })
                .y(function(d, i) {
                    return skewt.y(pp[i])
                });

            // Add clipping path
            svg.append("clipPath")
                .attr("id", "clipper")
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
                        return "temperaturezero";
                    } else {
                        return "gridline"
                    }
                })
                .attr("clip-path", "url(#clipper)");
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
                .attr("class", "gridline");

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
                .attr("class", "gridline")
                .attr("clip-path", "url(#clipper)")
                .attr("d", dryline);

            // Line along right edge of plot
            svg.append("line")
                .attr("x1", skewt.getSkewtWidth() - 0.5)
                .attr("x2", skewt.getSkewtWidth() - 0.5)
                .attr("y1", 0)
                .attr("y2", skewt.getSkewtHeight())
                .attr("class", "gridline");

            // draw hodograph background
            svghodo.selectAll(".circles")
                .data(d3.range(10, 80, 10))
                .enter().append("circle")
                .attr("cx", 0)
                .attr("cy", 0)
                .attr("r", function(d) {
                    return skewt.r(d);
                })
                .attr("class", "gridline");
            svghodo.selectAll("hodolabels")
                .data(d3.range(10, 80, 20)).enter().append("text")
                .attr('x', 0)
                .attr('y', function(d, i) {
                    return skewt.r(d);
                })
                .attr('dy', '0.4em')
                .attr('class', 'hodolabels')
                .attr('text-anchor', 'middle')
                .text(function(d) {
                    return d + 'kts';
                });

            // Add axes
            svg.append("g").attr("class", "x axis").attr("transform", "translate(0," + (skewt.getSkewtHeight() - 0.5) + ")").call(skewt.xAxis);
            svg.append("g").attr("class", "y axis").attr("transform", "translate(-0.5,0)").call(skewt.yAxis);
            svg.append("g").attr("class", "y axis ticks").attr("transform", "translate(-0.5,0)").call(skewt.yAxis2);
            //svg.append("g").attr("class", "y axis height").attr("transform", "translate(0,0)").call(skewt.yAxis2);
        },
        makeBarbTemplates: function() {
            var skewt = this;
            var speeds = d3.range(5, 105, 5);
            barbdef = skewt.svg.append('defs');
            speeds.forEach(function(d) {
                var thisbarb = barbdef.append('g').attr('id', 'barb' + d);
                var flags = Math.floor(d / 50);
                var pennants = Math.floor((d - flags * 50) / 10);
                var halfpennants = Math.floor((d - flags * 50 - pennants * 10) / 5);
                var px = skewt.barbsize;
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

            skewt.svg.append("rect")
                .attr("class", "overlay")
                .attr("width", skewt.getSkewtWidth())
                .attr("height", skewt.getSkewtHeight())
                .on("mouseover", function() {
                    skewt.focus.style("display", null);
                    skewt.focus2.style("display", null);
                    skewt.focus3.style("display", null);
                })
                .on("mouseout", function() {
                    skewt.focus.style("display", "none");
                    skewt.focus2.style("display", "none");
                    skewt.focus3.style("display", "none");
                })
                .on("mousemove", mousemove);

            function mousemove() {
                var y0 = skewt.y.invert(d3.mouse(this)[1]); // get y value of mouse pointer in pressure space
                var i = skewt.bisectTemp(skewt.mouseoverdata, y0, 1, skewt.mouseoverdata.length - 1);
                var d0 = skewt.mouseoverdata[i - 1];
                var d1 = skewt.mouseoverdata[i];
                skewt.d = y0 - d0.pressure > d1.pressure - y0 ? d1 : d0;
                skewt.focus.attr("transform", "translate(" + (skewt.x(skewt.d.temperature) + (skewt.y(skewt.basep) - skewt.y(skewt.d.pressure)) / skewt.tan) + "," + skewt.y(skewt.d.pressure) + ")");
                skewt.focus2.attr("transform", "translate(" + (skewt.x(skewt.d.dewpoint) + (skewt.y(skewt.basep) - skewt.y(skewt.d.pressure)) / skewt.tan) + "," + skewt.y(skewt.d.pressure) + ")");
                skewt.focus3.attr("transform", "translate(0," + skewt.y(skewt.d.pressure) + ")");
                skewt.focus.select("text").text(Math.round(skewt.d.temperature) + "°C");
                skewt.focus2.select("text").text(Math.round(skewt.d.dewpoint) + "°C");
                skewt.focus3.select("text").text("--" + (Math.round(skewt.d.heightagl / 100) / 10) + "km");
            }
        },
        parseDataNew: function(json) {
            var skewt = this;
            requestedLevels = [0, 1, 3, 6, 9]; // levels in km agl
            skewt.interpobjects = [];
            this.numberOfMembers = 1;
            skewt.alldata = json.temperature.map(function(c, k) {
                    var obj = {
                        pressure: json.pressure[k],
                        height: json.height[k],
                        temperature: json.temperature[k],
                        dewpoint: json.dewpoint[k], 
                        winddir: json.wind_direction[k],
                        windspeed: json.wind_speed[k],
                        windspeedround: Math.round((json.wind_speed[k]) / 5) * 5,
                        heightagl: json.height[k] - +json.height[0],
                    };
                    return obj;
                });

            var obj = skewt.alldata;
            // interpolate to given heights for each sounding
            var test = requestedLevels.map(function(d) {
                    if (d == 0) {
                        return obj[0];
                    }
                    d = 1000 * d + obj[0].height; // want height AGL
                    for (i = 0; i <= obj.length; i++) {
                        if (obj[i].height > d) {
                            var closeindex = i;
                            break;
                        } // since heights increase monotonically
                    }
                    var interp = d3.interpolateObject(obj[i - 1], obj[i]); // interp btw two levels
                    var half = interp(1 - (d - obj[i].height) / (obj[i - 1].height - obj[i].height));
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
                    return (i < 10) ? "temperature member" : "temperature mean"
                })
                .attr("clip-path", "url(#clipper)")
                .attr("d", skewt.line);

            skewt.tdlines = skewt.skewtgroup.selectAll("tdlines")
                .data(skewt.tlinetest[0]).enter().append("path")
                .attr("class", function(d, i) {
                    return (i < 10) ? "dewpoint member" : "dewpoint mean"
                })
                .attr("clip-path", "url(#clipper)")
                .attr("d", skewt.line2);

            skewt.holines = skewt.hodogroup.selectAll("hodolines")
                .data(skewt.hodobarbstest[0]).enter().append("path")
                .attr("class", function(d, i) {
                    return (i < 10) ? "hodoline member" : "hodoline mean"
                })
                .attr("d", skewt.hodoline);


            var tmp = skewt.hodobarbstest[0][0];
            //            tmp = skewt.flattened;
            skewt.hododots = skewt.hodogroup.selectAll('hododots')
                .data(tmp).enter().append("circle")
                .attr("r", function(d, i) {
                    return (i < 50) ? 2 : 4
                })
                .attr("cx", function(d, i) {
                    return skewt.r(d.windspeed * Math.sin((180 + d.winddir) * skewt.deg2rad));
                })
                .attr("cy", function(d, i) {
                    return -skewt.r(d.windspeed * Math.cos((180 + d.winddir) * skewt.deg2rad));
                })
                .attr("class", function(d, i) {
                    return "hododot hgt" + parseInt(d.heightagl / 1000)
                });

            var barbs = skewt.hodobarbstest[0][0];
            //            barbs = skewt.barbstest[0][skewt.numberOfMembers-1]
            barbs = this.barbs;
            skewt.allbarbs = skewt.barbgroup.selectAll("barbs")
                .data(barbs).enter().append("use")
                .attr("xlink:href", function(d) {
                    return "#barb" + d.windspeedround;
                })
                .attr("transform", function(d, i) {
                    return "translate(" + skewt.getSkewtWidth() + "," + skewt.y(d.pressure) + ") rotate(" + (d.winddir + 180) + ")";
                });
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
            skewt.allbarbs.data(skewt.barbstest[i][skewt.numberOfMembers-1])
                .attr("xlink:href", function(d) {
                    return "#barb" + d.windspeedround;
                })
                .attr("transform", function(d, i) {
                    return "translate(" + skewt.getSkewtWidth() + "," + skewt.y(d.pressure) + ") rotate(" + (d.winddir + 180) + ")";
                });
            skewt.holines.data(skewt.hodobarbstest[i]).attr("d", skewt.hodoline);

            skewt.hododots.data(skewt.flattened.slice(i * 55, i * 55 + 55))
                .attr("cx", function(d) {
                    return skewt.r(d.windspeed * Math.sin((180 + d.winddir) * skewt.deg2rad));
                })
                .attr("cy", function(d) {
                    return -skewt.r(d.windspeed * Math.cos((180 + d.winddir) * skewt.deg2rad));
                });
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
            loadData: function(json) {
                this.parseDataNew(json);
                this.hodobarbstest = [];
                this.tlinetest = [];
                this.interpdots = [];
                this.barbstest = [];
                /*
                for (var hr = 0; hr <= this.numberOfTimes; hr++) {
                */
                    var temp = [],
                        temp2 = [],
                        temp3 = [];
                    /*                    for (var mem = 0; mem < this.numberOfMembers; mem++) {
                        parsedCSV = this.alldata[mem][hr].filter(d=>{
                                return (d.temperature > -1000 && d.dewpoint > -1000);
                            });
*/
                        parsedCSV = this.alldata.filter(d=>{
                                return (d.temperature > -1000 && d.dewpoint > -1000);
                            });
                        this.barbs = parsedCSV.filter(d=> {
                                return (d.winddir >= 0 && d.windspeed >= 0 && d.pressure >= this.topp);
                            });
                        this.hodobarbs = this.barbs.filter(d=> {
                                return (d.pressure >= 200);
                            });
                        //parsedCSVreversed = parsedCSV.reverse(); // bisector needs ascending array
                        //                        this.interpdot = this.interpobjects[hr + 49 * mem];
                        this.interpdot = this.interpobjects[0]
                        
                        temp.push(this.hodobarbs);
                        temp2.push(parsedCSV);
                        temp3.push(this.barbs);
                        this.interpdots.push(this.interpdot);
                        //                    }
                    this.hodobarbstest.push(temp);
                    this.tlinetest.push(temp2);
                    this.barbstest.push(temp3);
                    //                }
                // need this for dots for some reason
                this.mouseoverdata = this.tlinetest[0][this.numberOfMembers-1].slice(0).reverse();
                this.flattened = this.interpdots.reduce(function(a, b) {
                        return a.concat(b);
                    });
                this.flattened = this.interpdots;
                this.flattened=parsedCSV;
                this.drawFirstHour();
            }
        });
    this.initUI();
    let skewt = this;

    if(this.jsonData) {
        this.loadData(this.jsonData);
    } else {
        d3.json('/repository/lib/skewt/skewt.json', (err, json) => {
                this.loadData(json);
            });
    }

    /*
    d3.json('/repository/skewt/conv_OUN.js', function(err, json) {
        if(true) return;
        var keys = {
            'sbcape': [],
            'mlcape': [],
            'mucape': [],
            'shr01': [],
            'srh01': [],
            'shr03': [],
            'srh03': [],
            'shr06': [],
            'srh06': [],
            'stp': [],
            'bunkers_dir': [],
            'bunkers_spd': [],
            'mllclp': [],
            'mllclz': []
        };
        skewt.indices = [];
        for (var f = 0; f < json['sbcape'].length; f += 10) {
            stats = {
                'mean': [],
                'min': [],
                'max': []
            }
            for (var key in keys) {
                var scaled = (key == 'stp') ? 10 : 1;
                var temp = json[key].slice(f, f + 10);
                stats['min'].push(Math.round(d3.min(temp)) / scaled);
                stats['mean'].push(Math.round(d3.mean(temp)) / scaled);
                stats['max'].push(Math.round(d3.max(temp)) / scaled);
            }
            skewt.indices.push(stats);
        }
        skewt.drawFirstHourText();
    });
    */
    //    this.initUI();

}
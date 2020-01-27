/**
   Copyright 2008-2019 Geode Systems LLC
*/


//Note: I put all of the chart definitions together at the top so one can see everything that is available here
var DISPLAY_D3_GLIDER_CROSS_SECTION = "GliderCrossSection";
var DISPLAY_D3_PROFILE = "profile";
var DISPLAY_D3_LINECHART = "D3LineChart";
var DISPLAY_SKEWT = "skewt";
var DISPLAY_VENN = "venn";
var DISPLAY_CHERNOFF = "chernoff";
var DISPLAY_D3BUBBLE = "d3bubble";

//Note: Added requiresData and category
addGlobalDisplayType({
    type: DISPLAY_D3_LINECHART,
    forUser: false,
    label: "D3 LineChart",
    requiresData: true,
    category: "Charts"
});
addGlobalDisplayType({
    type: DISPLAY_D3_PROFILE,
    forUser: false,
    label: "Profile",
    requiresData: true,
    category: "Charts"
});
addGlobalDisplayType({
    type: DISPLAY_D3_GLIDER_CROSS_SECTION,
    forUser: false,
    label: "Glider cross section",
    requiresData: true,
    category: "Charts"
});


addGlobalDisplayType({
    type: DISPLAY_VENN,
    forUser: true,
    label: "Venn Diagram",
    requiresData: true,
    category: "Misc"
});

addGlobalDisplayType({
    type: DISPLAY_CHERNOFF,
    forUser: true,
    label: "Chernoff Faces",
    requiresData: true,
    category: "Misc"
});

addGlobalDisplayType({
    type: DISPLAY_D3BUBBLE,
    forUser: true,
    label: "D3 Bubble Chart",
    requiresData: true,
    category: "Misc"
});


addGlobalDisplayType({
    type: DISPLAY_SKEWT,
    forUser: false,
    label: "SkewT",
    requiresData: true,
    category: "Charts"
});

//Note: define meaningful things as variables not as string literals
var FIELD_TIME = "time";
var FIELD_DEPTH = "depth";
var FIELD_VALUE = "value";
var FIELD_SELECTEDFIELD = "selectedfield";

var TYPE_LATITUDE = "latitude";
var TYPE_LONGITUDE = "longitude";
var TYPE_TIME = "time";
var TYPE_VALUE = "value";
var TYPE_ELEVATION = "elevation";


var FUNC_MOVINGAVERAGE = "movingAverage";

var D3Util = {
    foo: "bar",
    getAxis: function(axisType, range) {
        var axis;
        if (axisType == FIELD_TIME) {
            axis = d3.time.scale().range(range);
        } else {
            axis = d3.scale.linear().range(range);
        }
        return axis;
    },
    getDataValue: function(axis, record, index) {
        var data;
        if (axis.fieldIdx >= 0) {
            data = record.getData()[axis.fieldIdx];
        } else {
            switch (axis.type) {
            case TYPE_TIME:
                data = new Date(record.getDate());
                break;
            case TYPE_ELEVATION:
                //console.log(record.getElevation());
                data = record.getElevation();
                break;
            case TYPE_LATITUDE:
                data = record.getLatitude();
            case TYPE_LONGITUDE:
                data = record.getLongitude();
            default:
                data = record.getData()[index];
            }
        }


        if (axis.reverse == true) {
            return -1 * data;
        } else {
            return data;
        }
    },
    // This will be the default but we can add more colorscales
    getColorFromColorBar: function(value, range) {
        var colors = ["#00008B", "#0000CD", "#0000FF", "#00FFFF", "#7CFC00", "#FFFF00", "#FFA500", "#FF4500", "#FF0000", "#8B0000"];
        var colorScale = d3.scale.linear()
            .domain([0, colors.length - 1])
            .range(range);

        var colorScaler = d3.scale.linear()
            .range(colors)
            .domain(d3.range(0, colors.length).map(colorScale));

        color = colorScaler(value);
        return color;
    },
    // This is for the path lines the previous function for generic ones. 
    addColorBar: function(svg, colors, colorSpacing, displayWidth) {
        //Note: this originally had this.displayWidth which was undefined
        var colorBar = svg.append("g")
            .attr({
                "id": "colorBarG",
                "transform": "translate(" + (displayWidth - 40) + ",0)"
            });

        colorBar.append("g")
            .append("defs")
            .append("linearGradient")
            .attr({
                id: "colorBarGradient",
                x1: "0%",
                y1: "100%",
                x2: "0%",
                y2: "0%"
            })
            .selectAll("stop")
            .data(colors)
            .enter()
            .append("stop")
            .attr({
                "offset": function(d, i) {
                    return colorSpacing * (i) + "%"
                },
                "stop-color": function(d, i) {
                    return colors[i]
                },
                "stop-opacity": 1
            });

        return colorBar;
    }
}


function RamaddaSkewtDisplay(displayManager, id, properties) {
    let SUPER  = new RamaddaDisplay(displayManager, id, DISPLAY_SKEWT, properties);
    var ID_SKEWT = "skewt";
    var ID_DATE_LABEL = "skewtdate";
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);

    RamaddaUtil.defineMembers(this, {
        needsData: function() {
            return true;
        },
        getWikiEditorTags: function() {
            return Utils.mergeLists(
                SUPER.getWikiEditorTags(),
                ['label:Skewt Attributes',
                 'skewtWidth="500"',
                 'skewtHeight="550"',
                 'hodographWidth=150',
                 'showHodograph=false',
                 'windStride=1',
                 'showText=false',
                ])

        },
        initDisplay:  function() {
            SUPER.initDisplay.call(this);
        },
        handleEventMapClick: function(source, args) {
            if (!this.dataCollection) {
                return;
            }
            var pointData = this.dataCollection.getList();
            for (var i = 0; i < pointData.length; i++) {
                pointData[i].handleEventMapClick(this, source, args.lon, args.lat);
            }
        },
        handleEventPointDataLoaded: function(source, pointData) {
            //TODO: this results in a double  call to updateUI when first created
            this.updateUI();
        },
        updateUI: async function() {
//          console.log("skewt.updateui");
            if(!this.loadedResources) {
                var time = new Date();
                await Utils.importCSS(ramaddaBaseUrl +"/htdocs_v_" + time.getTime()+"/lib/skewt/sounding.css");
                //            await Utils.importCSS(ramaddaBaseHtdocs+"/lib/skewt/sounding.css");
                //            await Utils.importJS(ramaddaBaseHtdocs +"/lib/skewt/d3skewt.js");
                await Utils.importJS(ramaddaBaseUrl +"/htdocs_v_" + time.getTime()+"/lib/skewt/d3skewt.js");
                this.loadedResources = true;
            }

            if(!window["D3Skewt"]) {
                setTimeout(()=>this.updateUI(),100);
                return;
            }
            SUPER.updateUI.call(this);

//          console.log("skewt.updateui-1");
            let records =  this.filterData();
            if (!records || records.length==0) {
                this.setContents(this.getLoadingMessage());
                return;
            }
//          console.log("skewt.updateui-2");

            let skewtId = this.getDomId(ID_SKEWT);
            let html = HtmlUtils.div(["id", skewtId], "");
            this.setContents(html);
            var date = records[0].getDate();
            if(this.jq(ID_DATE_LABEL).length==0) {
                this.jq(ID_TOP_LEFT).append(HtmlUtils.div(["id",this.getDomId(ID_DATE_LABEL)]));
            }
            if(date!=null) {
                this.jq(ID_DATE_LABEL).html("Date: " + this.formatDate(date));
            } else {
                this.jq(ID_DATE_LABEL).html("");
            }
            var options = {};
            if (this.propertyDefined("showHodograph"))
                options.showHodograph = this.getProperty("showHodograph", true);
            if (this.propertyDefined("showText"))
                options.showText = this.getProperty("showText", true);
            if (this.propertyDefined("skewtWidth"))
                options.skewtWidth = parseInt(this.getProperty("skewtWidth"));
            if (this.propertyDefined("skewtHeight"))
                options.skewtHeight = parseInt(this.getProperty("skewtHeight"));
            if (this.propertyDefined("hodographWidth")){
                options.hodographWidth = parseInt(this.getProperty("hodographWidth"));
            }
            if (this.propertyDefined("windStride")){
                options.windStride = parseInt(this.getProperty("windStride"));
            }
            options.showText = this.getProperty("showText",true);
            //            options.hodographWidth = 200;
            var fields = this.getData().getRecordFields();
            var names = [
                {id:"pressure",aliases:["vertCoord"]},
                {id:"height",aliases:["Geopotential_height_isobaric"]},
                {id:"temperature",aliases:["Temperature_isobaric"]},
                {id:"dewpoint",aliases:[]},
                {id:"rh",aliases:["Relative_humidity_isobaric","relative_humidity"]},
                {id:"wind_direction",aliases:[]},
                {id:"wind_speed",aliases:[]},
                {id:"uwind",aliases:["u-component_of_wind_isobaric","u"]},
                {id:"vwind",aliases:["v-component_of_wind_isobaric","v"]},
            ];
            //TODO: check for units
            var data ={};
            var dataFields ={};
            for(var i=0;i<names.length;i++) {
                var obj = names[i];
                var id = obj.id;
                var field = this.getFieldById(fields,id);
                if(field == null) {
                    for(var j=0;j<obj.aliases.length;j++) {
                        field = this.getFieldById(fields,obj.aliases[j]);
                        if(field) break;
                    }
                }
                if(field) {
                    data[id] = this.getColumnValues(records, field).values;
                    dataFields[id]=field;
                }
            }

            if(!data.pressure) {
                this.displayError("No pressure defined in data");
                return;
            }

            if(!data.temperature) {
                this.displayError("No temperature defined in data");
                return;
            }

            if(!data.height) {
                var pressures = [
                    1013.25, 954.61, 898.76, 845.59, 795.01, 746.91, 701.21,
                    657.80, 616.6, 577.52, 540.48, 505.39, 472.17, 440.75,
                    411.05, 382.99, 356.51, 331.54, 303.00, 285.85, 264.99,
                    226.99, 193.99, 165.79, 141.70, 121.11, 103.52, 88.497,
                    75.652, 64.674, 55.293, 25.492, 11.970, 5.746, 2.871,
                    1.491, 0.798, 0.220, 0.052, 0.010,];
                var alts = [
                    0, 500, 1000, 1500, 2000, 2500, 3000, 3500, 4000, 4500, 5000,
                    5500, 6000, 6500, 7000, 7500, 8000, 8500, 9000, 9500, 10000,
                    11000, 12000, 13000, 14000, 15000, 16000, 17000, 18000, 19000,
                    20000, 25000, 30000, 35000, 40000, 45000, 50000, 60000, 70000,
                    80000,
                ];
                
                data.height = [];
                for(var i=0;i<data.pressure.length;i++) {
                    var pressure = data.pressure[i];
                    var alt = alts[alts.length-1];
                    for(var j=0;j<pressures.length;j++) {
                        if(pressure>=pressures[j]) {
                            if(j==0) alt = 0;
                            else {
                                var p1 = pressures[j-1];
                                var p2 = pressures[j];
                                var a1 = alts[j-1];
                                var a2 = alts[j];
                                var percent = 1-(pressure-p2)/(p1-p2);
                                alt = (a2-a1)*percent+a1;
                            }
                            break;
                        }
                    }
                    data.height.push(alt);
                }
            }


            if(!data.dewpoint) {
                if(!data.rh) {
                    this.displayError("No dewpoint or rh");
                    return;
                }
                data.dewpoint = [];
                for(var i=0;i<data.rh.length;i++) {
                    var rh=data.rh[i];
                    var t=data.temperature[i];
                    var dp = t-(100-rh)/5;
                    data.dewpoint.push(dp);
                }
            }


            if(!data.wind_speed) {
                if(!data.uwind || !data.vwind) {
                    this.displayError("No wind speed defined in data");
                    return;
                }
                data.wind_speed = [];
                data.wind_direction = [];
                for(var i=0;i<data.uwind.length;i++) {
                    var u = data.uwind[i];
                    var v = data.vwind[i];
                    var ws = Math.sqrt(u*u+v*v);
                    var wdir = 180+(180/Math.PI)*Math.atan2(v,u);
                    data.wind_speed.push(ws);
                    data.wind_direction.push(wdir);
                }
            }

            var alldata = data;
            data = {};
            //if any missing then don't include
            for(a  in alldata) data[a] = [];
            alldata[names[0].id].map((v,idx)=>{
                var ok = true;
                for(id in alldata) {
                    if(isNaN(alldata[id][idx])) {
                        ok = false;
                        break;
                    }
                }
                if(ok) {
                    for(id in alldata) {
                        data[id].push(alldata[id][idx]);
                    }
                }
            });


            if(data.height.length>1) {
                if(data.height[0]>data.height[1]) {
                    for(name in data)
                        data[name] = Utils.reverseArray(data[name]);
                }
            }
            if(data.temperature.length==0) {
                this.displayError("No data is available");
                return;
            }

            /*
            if(options.windStride > 1) {
                
                var new_wind_speed = [];
                var new_wind_direction = [];
                for (var i = 0; i<data.wind_speed.length; i++) {
		    var pres = data.pressure[i];
                    if (i%options.windStride == 0) {
                        new_wind_speed.push(data.wind_speed[i]);
                        new_wind_direction.push(data.wind_direction[i]);
                    } else {
                        new_wind_speed.push(0);
                        new_wind_direction.push(0);
		    }
                }
                data.wind_speed = new_wind_speed;
                data.wind_direction = new_wind_direction;
            }
            */


            options.myid = this.getId();
            try {
                this.skewt = new D3Skewt(skewtId, options,data);
            } catch(e) {
                this.displayError("An error occurred: " +e);
                console.log("error:" + e.stack);
                return;
            }
            await this.getDisplayEntry((e)=>{
                var q= e.getAttribute("variables");
                if(!q) return;
                q = q.value;
                q = q.replace(/\r\n/g,"\n");
                q = q.replace(/^ *\n/,"");
                q = q.replace(/^ *([^:]+):([^\n].*)$/gm,"<div title='$1' class=display-skewt-index-label>$1</div>: <div title='$2'  class=display-skewt-index>$2</div>");
                q = q.replace(/[[\r\n]/g,"\n");
                q = HtmlUtils.div(["class", "display-skewt-text"],q);
                $("#" + this.skewt.textBoxId).html(q);
            });
        }
    });
}


function RamaddaD3Display(displayManager, id, properties) {
    // To get it to the console
    testProperties = properties;

    var ID_SVG = "svg";
    var SUPER;
    RamaddaUtil.inherit(this, SUPER = new RamaddaDisplay(displayManager, id, "d3", properties));
    addRamaddaDisplay(this);

    RamaddaUtil.defineMembers(this, {
        initDisplay: function() {
            this.createUI();
            this.setDisplayTitle(properties.graph.title);

            //Note: use innerHeight/innerWidth wiki attributes
            var width = this.getProperty("innerWidth", 600);
            var height = this.getProperty("innerHeight", 300);
            var margin = {
                top: 20,
                right: 50,
                bottom: 30,
                left: 50
            };
            var divStyle =
                "height:" + height + "px;" +
                "width:" + width + "px;";
            var html = HtmlUtils.div([ATTR_ID, this.getDomId(ID_SVG), ATTR_STYLE, divStyle], "");
            this.setContents(html);

            // To create dynamic size of the div
            this.displayHeight = parseInt((d3.select("#" + this.getDomId(ID_SVG)).style("height")).split("px")[0]) - margin.top - margin.bottom; //this.getProperty("height",300);//d3.select("#"+this.getDomId(ID_SVG)).style("height");//
            this.displayWidth = parseInt((d3.select("#" + this.getDomId(ID_SVG)).style("width")).split("px")[0]) - margin.left - margin.right; //this.getProperty("width",600);//d3.select("#"+this.getDomId(ID_SVG)).style("width");//

            //                console.log("WxH:" + this.displayHeight +" " + this.displayWidth);

            // To solve the problem with the classess within the class
            var myThis = this;
            var zoom = d3.behavior.zoom()
                .on("zoom", function() {
                    myThis.zoomBehaviour()
                });
            this.zoom = zoom;
            this.svg = d3.select("#" + this.getDomId(ID_SVG)).append("svg")
                .attr("width", this.displayWidth + margin.left + margin.right)
                .attr("height", this.displayHeight + margin.top + margin.bottom)
                .attr(ATTR_CLASS, "D3graph")
                .call(zoom)
                .on("click", function() {
                    myThis.click(d3.event)
                })
                .on("dblclick", function() {
                    myThis.dbclick(d3.event)
                })
                .append("g")
                .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

            // Define the Axis
            // 100 pixels for the legend... lets see if we keep it
            this.x = D3Util.getAxis(properties.graph.axis.x.type, [0, this.displayWidth - 100]);
            this.y = D3Util.getAxis(properties.graph.axis.y.type, [this.displayHeight, 0]);

            this.xAxis = d3.svg.axis()
                .scale(this.x)
                .orient("bottom");

            this.yAxis = d3.svg.axis()
                .scale(this.y)
                .orient("left");

            // Add Axis to the plot
            this.svg.append("g")
                .attr(ATTR_CLASS, "x axis")
                .attr("transform", "translate(0," + this.displayHeight + ")")
                .call(this.xAxis);

            this.svg.append("g")
                .attr(ATTR_CLASS, "y axis")
                .call(this.yAxis);


            // Color Bar
            var colors = ["#00008B", "#0000CD", "#0000FF", "#00FFFF", "#7CFC00", "#FFFF00", "#FFA500", "#FF4500", "#FF0000", "#8B0000"];

            var colorSpacing = 100 / (colors.length - 1);

            var colorBar = D3Util.addColorBar(this.svg, colors, colorSpacing, this.displayWidth);
            this.color = d3.scale.category10();
            this.updateUI();
        },
        needsData: function() {
            return true;
        },
        initDialog: function() {
            this.addFieldsCheckboxes();
        },
        getDialogContents: function() {
            var height = this.getProperty(PROP_HEIGHT, "400");
            var html = HtmlUtils.div([ATTR_ID, this.getDomId(ID_FIELDS), ATTR_CLASS, "display-fields", ]);
            html += SUPER.getDialogContents.apply(this);
            return html;
        },
        fieldSelectionChanged: function() {
            this.updateUI();
        },
        // onlyZoom is not updating the axis
        updateUI: function() {
            //Note: Not sure why onlyZoom was a function param. The pointData gets passes in 
            //when the json is loaded
            //            updateUI: function(onlyZoom) {
            var onlyZoom = false;

            //Note: if we write to the SVG dom element then we lose the svg object that got created in initDisplay
            //Not sure how to show a message to the user
            if (!this.hasData()) {
                //this.writeHtml(ID_SVG, HtmlUtils.div([ATTR_CLASS,"display-message"], this.getLoadingMessage()));
                return;
            }
            test = this;
            var selectedFields = this.getSelectedFields();
            if (selectedFields.length == 0) {
                //this.writeHtml(ID_SVG, "No fields selected");
                return;
            }
            this.addFieldsCheckboxes();
            pointData = this.getData();
            if (pointData == null) {
                //this.writeHtml(ID_SVG, "No data");
                console.log("no data");
                return;
            }

            var fields = pointData.getNumericFields();
            var records = pointData.getRecords();
            var ranges = RecordUtil.getRanges(fields, records);
            var elevationRange = RecordUtil.getElevationRange(fields, records);
            var offset = (elevationRange[1] - elevationRange[0]) * 0.05;
            // To be used inside a function we can use this.x inside them so we extract as variables. 
            var x = this.x;
            var y = this.y;
            var color = this.color;
            var axis = properties.graph.axis;

            if (onlyZoom) {
                this.zoom.x(this.x);
                this.zoom.y(this.y);
            } else {
                // Update axis for the zoom and other changes
                this.x.domain(d3.extent(records, function(d) {
                    return D3Util.getDataValue(axis.x, d, selectedFields[0].getIndex());
                }));
                // the y domain depends on the first selected element I have to think about it.
                this.y.domain(d3.extent(records, function(d) {
                    return D3Util.getDataValue(axis.y, d, selectedFields[0].getIndex());
                }));

                this.zoom.x(this.x);
                this.zoom.y(this.y);
            }

            this.svg.selectAll(".y.axis").call(this.yAxis);
            this.svg.selectAll(".x.axis").call(this.xAxis);

            // Remove previous lines
            this.svg.selectAll(".line").remove();
            this.svg.selectAll(".legendElement").remove();

            var myThis = this;
            for (var fieldIdx = 0; fieldIdx < selectedFields.length; fieldIdx++) {
                var dataIndex = selectedFields[fieldIdx].getIndex();
                var range = ranges[dataIndex];
                // Plot line for the values
                var line = d3.svg.line()
                    .x(function(d) {
                        return x(D3Util.getDataValue(axis.x, d, selectedFields[fieldIdx].getIndex()));
                    })
                    .y(function(d) {
                        return y(D3Util.getDataValue(axis.y, d, selectedFields[fieldIdx].getIndex()));
                    });

                displayLine = this.svg.append("path")
                    .datum(records)
                    .attr(ATTR_CLASS, "line")
                    .attr("d", line)
                    .on("mousemove", function() {
                        myThis.mouseover(d3.event)
                    })
                    .attr("fill", "none")
                    .attr("stroke", function(d) {
                        return color(fieldIdx);
                    })
                    .attr("stroke-width", "0.5px");

                if (properties.graph.axis.z == FIELD_SELECTEDFIELD) {
                    displayLine.attr("stroke", "url(#colorBarGradient)");
                }

                if (properties.graph.derived != null) {
                    var funcs = properties.graph.derived.split(",");
                    for (funcIdx = 0; funcIdx < funcs.length; funcIdx++) {
                        var func = funcs[funcIdx];
                        if (func == FUNC_MOVINGAVERAGE) {
                            // Plot moving average Line
                            var movingAverageLine = d3.svg.line()
                                .x(function(d) {
                                    return x(D3Util.getDataValue(axis.x, d, selectedFields[fieldIdx].getIndex()));
                                })
                                .y(function(d, i) {
                                    if (i == 0) {
                                        return _movingSum = D3Util.getDataValue(axis.y, d, selectedFields[fieldIdx].getIndex());
                                    } else {
                                        _movingSum += D3Util.getDataValue(axis.y, d, selectedFields[fieldIdx].getIndex());
                                    }
                                    return y(_movingSum / i);
                                })
                                .interpolate("basis");
                            this.svg.append("path")
                                .attr(ATTR_CLASS, "line")
                                .attr("d", movingAverageLine(records))
                                .attr("fill", "none")
                                .attr("stroke", function(d) {
                                    return color(fieldIdx);
                                })
                                .attr("stroke-width", "1.5px")
                                .attr("viewBox", "50 50 100 100 ")
                                .style("stroke-dasharray", ("3, 3"));
                        } else {
                            console.log("Error: Unknown derived function:" + func);
                        }

                    }
                }

                // Legend element Maybe create a function or see how we implement the legend
                this.svg.append("svg:rect")
                    .attr(ATTR_CLASS, "legendElement")
                    .attr("x", this.displayWidth - 100)
                    .attr("y", (50 + 50 * fieldIdx))
                    .attr("stroke", function(d) {
                        return color(fieldIdx);
                    })
                    .attr("height", 2)
                    .attr("width", 40);

                this.svg.append("svg:text")
                    .attr(ATTR_CLASS, "legendElement")
                    .attr("x", this.displayWidth - 100 + 40 + 10) // position+color rect+padding
                    .attr("y", (55 + 55 * fieldIdx))
                    .attr("stroke", function(d) {
                        return color(fieldIdx);
                    })
                    .attr("style", "font-size:50%")
                    .text(selectedFields[fieldIdx].getLabel());
            }
        },

        zoomBehaviour: function() {
            // Call redraw with only zoom don't update extent of the data.
            this.updateUI(true);
        },
        //this gets called when an event source has selected a record
        handleEventRecordSelection: function(source, args) {},
        mouseover: function() {
            // TO DO
            testing = d3.event;
            console.log("mouseover");
        },
        click: function(event) {
            // TO DO
            console.log("click:" + event);
        },
        dbclick: function(event) {
            // Unzoom
            this.zoom();
            this.updateUI();
        },
        getSVG: function() {
            return this.svg;
        }
    });
}


function RamaddaD3LineChartDisplay(displayManager, id, properties) {
    var dfltProperties = {};
    //Note: use json structures to define the props
    dfltProperties.graph = {
        title: "Line chart",
        //Note: changed this to "derived" from "extraLine".
        //This is a comma separated list of functions (for now just one)
        derived: FUNC_MOVINGAVERAGE,
        axis: {
            y: {
                type: TYPE_VALUE,
                fieldname: FIELD_SELECTEDFIELD
            },
            x: {
                type: TYPE_TIME,
                fieldname: FIELD_TIME,
            }
        }
    };

    properties = $.extend(dfltProperties, properties);
    return new RamaddaD3Display(displayManager, id, properties);
}


function RamaddaProfileDisplay(displayManager, id, properties) {
    var dfltProperties = {};
    //Note: use json structures to define the props
    dfltProperties.graph = {
        title: "Profile chart",
        derived: null,
        axis: {
            y: {
                type: TYPE_ELEVATION,
                fieldname: FIELD_DEPTH,
                fieldIdx: 3,
                reverse: true
            },
            x: {
                type: TYPE_VALUE,
                fieldname: FIELD_VALUE,
            },
        }
    };
    //Note: now set the properties
    properties = $.extend(dfltProperties, properties);
    return new RamaddaD3Display(displayManager, id, properties);
}




function RamaddaGliderCrossSectionDisplay(displayManager, id, properties) {
    var dfltProperties = {};
    //Note: use json structures to define the props
    dfltProperties.graph = {
        title: "Glider cross section",
        derived: null,
        axis: {
            y: {
                type: TYPE_ELEVATION,
                fieldname: FIELD_DEPTH,
                reverse: true
            },
            x: {
                type: TYPE_TIME,
                fieldname: FIELD_TIME,
            },
            z: FIELD_SELECTEDFIELD,
        }
    };
    properties = $.extend(dfltProperties, properties);

    return new RamaddaD3Display(displayManager, id, properties);
}






function RamaddaVennDisplay(displayManager, id, properties) {
    var ID_VENN = "venn";
    let SUPER = new RamaddaFieldsDisplay(displayManager, id, DISPLAY_VENN, properties);
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);
    $.extend(this, {
        getContentsStyle: function() {
            return "";
        },
        checkLayout: function() {
            this.updateUIInner();
        },
        updateUI: function() {
            var includes = "<script src='" + ramaddaBaseUrl + "/lib/venn.js'></script>";
            this.writeHtml(ID_DISPLAY_TOP, includes);
            let _this = this;
            var func = function() {
                _this.updateUIInner();
            };
            setTimeout(func, 10);
        },
        updateUIInner: function() {
            let records = this.filterData();
            if (!records) {
                return;
            }
            var allFields = this.getData().getRecordFields();
            var fields = this.getSelectedFields(allFields);
            if (fields.length == 0)
                fields = allFields;
            var strings = this.getFieldsOfType(fields, "string");
            if (strings.length == 0) {
                this.displayError("No string fields specified");
                return;
            }
            /*
              var sets = [{sets : [0], label : 'SE', size : 28,}, 
              {sets : [1], label : 'Treat', size: 35},
              {sets : [2], label : 'Anti-CCP', size : 108}, 
              {sets : [3], label : 'DAS28', size:106},
              {sets : [0,1], size:1},
              {sets : [0,2], size:1},
              {sets : [0,3], size:14},
            */
            var setInfos = {};
            var setCnt = 0;
            for (var rowIdx = 0; rowIdx < records.length; rowIdx++) {
                var row = this.getDataValues(records[rowIdx]);
                var keys = [];
                var key = "";
                for (var fieldIdx = 0; fieldIdx < strings.length; fieldIdx++) {
                    var field = strings[fieldIdx];
                    var value = row[field.getIndex()];
                    var setKey = field.getId() + "--" + value;
                    keys.push(setKey);
                    key += "--" + setKey;
                    if (!Utils.isDefined(setInfos[setKey])) {
                        setInfos[setKey] = {
                            count: 0,
                            setIds: [setCnt],
                            label: value,
                        };
                        setCnt++;
                    }
                    setInfos[setKey].count++;
                }
                var ids = [];
                if (!Utils.isDefined(setInfos[key])) {
                    for (var i = 0; i < keys.length; i++) {
                        ids.push(setInfos[keys[i]].setIds[0]);
                    }
                    setInfos[key] = {
                        count: 0,
                        setIds: ids,
                        label: null,
                    };
                }
                setInfos[key].count++;
            }

            var sets = [];
            for (var a in setInfos) {
                var setInfo = setInfos[a];
                var obj = {
                    sets: setInfo.setIds,
                    size: setInfo.count
                };
                if (setInfo.label)
                    obj.label = setInfo.label;
                sets.push(obj);
            }
            this.writeHtml(ID_DISPLAY_CONTENTS, HtmlUtils.div(["id", this.getDomId(ID_VENN), "style", "height:300px;"], ""));
            var chart = venn.VennDiagram()
                .width(600)
                .height(400);
            var id = "#" + this.getDomId(ID_VENN);
            var strokeColors = this.getColorTable(true, "strokeColors", "nice");
            var fillColors = this.getColorTable(true, "fillColors", "nice");
            var textColors = this.getColorTable(true, "textColors");
            if (!textColors)
                textColors = strokeColors;
            d3.select(id).datum(sets).call(chart);
            d3.selectAll(id + " .venn-circle path")
                .style("fill-opacity", parseFloat(this.getProperty("fillOpacity", 0.5)))
                .style("stroke-width", parseInt(this.getProperty("strokeWidth", 1)))
                .style("stroke-opacity", parseFloat(this.getProperty("strokeOpacity", 0.5)))
                .style("stroke", function(d, i) {
                    return i < strokeColors.length ? strokeColors[i] : strokeColors[i % strokeColors.length];
                })
                .style("fill", function(d, i) {
                    return i < fillColors.length ? fillColors[i] : fillColors[i % fillColors.length];
                })
            d3.selectAll(id + " .venn-circle text")
                .style("fill", function(d, i) {
                    return i < textColors.length ? textColors[i] : textColors[i % textColors.length];
                })
                .style("font-size", this.getProperty("fontSize", "16px"))
                .style("font-weight", this.getProperty("fontWeight", "100"));

        }
    });
}



function RamaddaChernoffDisplay(displayManager, id, properties) {
    var ID_VENN = "venn";
    let SUPER = new RamaddaFieldsDisplay(displayManager, id, DISPLAY_VENN, properties);
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);
    $.extend(this, {
        getContentsStyle: function() {
            return "";
        },
        checkLayout: function() {
            this.updateUIInner();
        },
        timeout: 100,
        written: false,
        updateUI: function() {
            if (!this.written) {
                this.written = true;
                var includes = "<script src='" + ramaddaBaseUrl + "/lib/chernoff.js'></script>";
                this.writeHtml(ID_DISPLAY_TOP, includes);
            }
            this.updateUIInner();
        },
        updateUIInner: function() {
            let _this = this;
            if (!Utils.isDefined(d3.chernoff)) {
                this.timeout = this.timeout * 2;
                var func = function() {
                    _this.updateUIInner();
                };
                setTimeout(func, this.timeout);
                return;
            }
            let records = this.filterData();
            if (!records) {
                return;
            }
            var allFields = this.getData().getRecordFields();
            var fields = this.getSelectedFields(allFields);
            var numericFields = this.getFieldsOfType(fields, "numeric");
            if (numericFields.length == 0) {
                this.displayError("No numeric fields specified");
                return;
            }
            if (fields.length == 0)
                fields = allFields;
            var string = this.getFieldOfType(fields, "string");
            var legend = "";
            var colorField = this.getFieldById(allFields, this.getProperty("colorBy"));
            var colorscale;
            if (colorField) {
                var colors = this.getColorTable(true, null, "blue_white_red");
                var colorValues = this.getColumnValues(records, colorField);
                colorscale = [];
                var min = parseFloat(this.getProperty("colorByMin", colorValues.min));
                var max = parseFloat(this.getProperty("colorByMax", colorValues.max));
                var range = max - min;
                //                console.log("range:" +  max +" " +min +" #colors:" + colors.length);
                for (var i = 0; i < colorValues.values.length; i++) {
                    var value = colorValues.values[i];
                    var percent = (value - min) / range;
                    var idx = Math.round(percent * (colors.length - 1));
                    //                    console.log(idx+" " +colors[idx] +" " + value + " " + percent);
                    colorscale.push(colors[idx]);
                }
                this.displayColorTable(colors, ID_DISPLAY_BOTTOM, min, max);
                legend += "<b>Colored by</b>: " + colorField.getLabel() + "&nbsp;&nbsp;";
            }
            var attrs = [{
                label: "Face width",
                name: "face",
                key: "f",
                min: 0,
                max: 1
            }, {
                label: "Hair",
                name: "hair",
                key: "h",
                min: -1,
                max: 1
            }, {
                label: "Mouth",
                name: "mouth",
                key: "m",
                min: -1,
                max: 1
            }, {
                label: "Nose height",
                name: "noseHeight",
                key: "nh",
                min: 0,
                max: 1
            }, {
                label: "Nose width",
                name: "noseWidth",
                key: "nw",
                min: 0,
                max: 1
            }, {
                label: "Eyes height",
                name: "eyesHeight",
                key: "eh",
                min: 0,
                max: 1
            }, {
                label: "Eyes width",
                name: "eyesWidth",
                key: "ew",
                min: 0,
                max: 1
            }, {
                label: "Brow",
                name: "brow",
                key: "b",
                min: -1,
                max: 1
            }];
            var html = "";
            var showHelp = this.getProperty("showHelp", false);
            if (showHelp) {
                html += "Settings:<br><table class=ramadda-table><thead><tr><th>Attribute&nbsp;</th><th>&nbsp;Default range&nbsp;</th><th>&nbsp;Set field&nbsp;</th><th>&nbsp;Set min&nbsp;</th><th>&nbsp;Set max&nbsp;</th></tr></thead><tbody>";
            }

            for (a in attrs) {
                var attr = attrs[a];
                if (showHelp) {
                    html += "<tr><td>" + attr.label + "</td><td align=center>" + attr.min + " - " + attr.max + "</td><td>" + attr.name + "Field=&lt;field_id&gt;" + "</td><td>" + attr.name + "Min=&lt;min_value&gt;" + "</td><td>" + attr.name + "Max=&lt;max_value&gt;" + "</td></tr>";
                }
                attr.field = this.getFieldById(allFields, this.getProperty(attr.name + "Field"));
                if (attr.field) {
                    legend += "<b>" + attr.label + "</b>: " + attr.field.getLabel() + "&nbsp;&nbsp;";
                    if (Utils.isDefined(this.getProperty(attr.name + "Min"))) {
                        attr.min = parseFloat(this.getProperty(attr.name + "Min"));
                    }
                    if (Utils.isDefined(this.getProperty(attr.name + "Max"))) {
                        attr.max = parseFloat(this.getProperty(attr.name + "Max"));
                    }
                    attr.column = this.getColumnValues(records, attr.field);
                }
            }
            if (showHelp) {
                html += "</tbody></table>";
            }

            var sortField = this.getFieldById(allFields, this.getProperty("sortBy"));

            var rows = [];
            for (var rowIdx = 0; rowIdx < records.length; rowIdx++) {
                var blob = {
                    values: this.getDataValues(records[rowIdx])
                };
                if (colorscale) blob.color = colorscale[rowIdx]
                rows.push(blob);
            }

            if (sortField) {
                rows.sort(function(a, b) {
                    var v1 = a.values[sortField.getIndex()];
                    var v2 = b.values[sortField.getIndex()];
                    if (v1 < v2) return 1;
                    if (v1 > v2) return -1;
                    return 0;
                });
            }

            var data = [];
            for (var rowIdx = 0; rowIdx < rows.length; rowIdx++) {
                var blob = rows[rowIdx];
                var row = blob.values;
                var color = blob.color;
                var faceData = {
                    f: 0.5, //0-1
                    h: 0, //-1-1
                    m: 0, //-1-1
                    nh: 0.5, //0-1
                    nw: 0.5, //0-1
                    eh: 0.5, //0-1
                    ew: 0.5, //0-1
                    b: 0 //-1-1
                };
                data.push({
                    faceData: faceData,
                    color: color
                });
                var tt = "";
                for (a in attrs) {
                    var attr = attrs[a];
                    var field = attr.field;
                    if (!field) {
                        faceData[attr.key] = attr.min + (attr.max - attr.min) / 2;
                    } else {
                        var value = row[field.getIndex()];
                        var min = attr.column.min;
                        var max = attr.column.max;
                        tt += field.getLabel() + ": " + value + " range: " + min + " - " + max + " (" + attr.label + ")\n";
                        if (max != min) {
                            var percent = (value - min) / (max - min);
                            var adjValue = attr.min + (attr.max - attr.min) * percent;
                            //                            console.log(" %:" + percent + " v:" + value +" min:" + min +" max:" + max +" adj:" + adjValue);
                            faceData[attr.key] = adjValue;
                        }
                    }
                }
                var label = (string ? row[string.getIndex()] : "Row: " + rowIdx);
                var labelValue = (string ? row[string.getIndex()] : "");
                label = HtmlUtils.div(["class", "display-chernoff-label"], label);
                var div = HtmlUtils.div(["id", this.getDomId("chernoff") + "_" + rowIdx, "class", "display-chernoff-face"], "");
                html += HtmlUtils.div(["title", tt, "class", "display-chernoff-wrapper ramadda-div-link", "value", labelValue], div + label);
            }
            legend = HtmlUtils.div(["class", "display-chernoff-legend"], legend);
            var height = this.getProperty("height", "400px");
            if (!height.endsWith("px")) height += "px";
            this.writeHtml(ID_DISPLAY_CONTENTS, legend + HtmlUtils.div(["style", "height:" + height + ";", "class", "display-chernoff-container", "id", this.getDomId("chernoff")], html));
            for (var rowIdx = 0; rowIdx < records.length; rowIdx++) {
                var div = "#" + this.getDomId("chernoff") + "_" + rowIdx;
                this.makeFace(div, data[rowIdx].faceData, data[rowIdx].color);
            }

            if (string) {
                $("#" + this.getDomId(ID_DISPLAY_CONTENTS)).find(".ramadda-div-link").click(function() {
                    var value = $(this).attr("value");
                    _this.propagateEvent("handleEventFieldValueSelect", {
                        field: string,
                        value: value
                    });
                });
            }
        },
        makeFace: function(div, faceData, color) {
            function chernoffFace() {
                var width = 400,
                    height = 200;
                var chernoff = d3.chernoff()
                    .face(function(d) {
                        return d.f;
                    })
                    .hair(function(d) {
                        return d.h;
                    })
                    .mouth(function(d) {
                        return d.m;
                    })
                    .nosew(function(d) {
                        return d.nw;
                    })
                    .noseh(function(d) {
                        return d.nh;
                    })
                    .eyew(function(d) {
                        return d.ew;
                    })
                    .eyeh(function(d) {
                        return d.eh;
                    })
                    .brow(function(d) {
                        return d.b;
                    });

                function data() {
                    return [faceData];
                }

                function drawFace(selection) {
                    var svg = selection.append("svg")
                        .attr("width", width)
                        .attr("height", height);
                    var face = svg.selectAll("g.chernoff")
                        .data(data())
                        .enter().append("g")
                        .attr("class", "chernoff")
                        .call(chernoff);
                    if (color)
                        face.attr("style", "fill:" + color);
                }

                function draw(selection) {
                    selection.call(drawFace);
                }
                return draw;
            }
            d3.select(div)
                .call(chernoffFace());
        }





    });
}



let bubbleTestData = [
    {
	cat: 'library', 
	name: 'D3', value: 30,
    icon: 'https://naustud.io/tech-stack/img/d3.svg',
    desc: `
		D3.js (or just D3 for Data-Driven Documents) is a JavaScript library for
		producing dynamic, interactive data visualizations in web browsers.
		It makes use of the widely implemented SVG, HTML5, and CSS standards.<br>
		This infographic you are viewing is made with D3.
	`
}, {
    cat: 'library', name: 'Raphaël', value: 10,
    icon: 'https://naustud.io/tech-stack/img/raphael.svg',
    desc: `
		Raphaël is a cross-browser JavaScript library that draws Vector graphics for web sites.
		It will use SVG for most browsers, but will use VML for older versions of Internet Explorer.
	`
}, {
    cat: 'library', name: 'Relay', value: 70,
    icon: 'https://naustud.io/tech-stack/img/relay.svg',
    desc: `
		A JavaScript framework for building data-driven React applications.
		It uses GraphQL as the query language to exchange data between app and server efficiently.
		Queries live next to the views that rely on them, so you can easily reason
		about your app. Relay aggregates queries into efficient network requests to fetch only what you need.
	`
}, {
    cat: 'library', name: 'Three.js', value: 40,
    icon: 'https://naustud.io/tech-stack/img/threejs.png',
    desc: `
		Three.js allows the creation of GPU-accelerated 3D animations using
		the JavaScript language as part of a website without relying on
		proprietary browser plugins. This is possible thanks to the advent of WebGL.
	`
}, {
    cat: 'library sub', name: 'Lodash', value: 30,
    icon: 'https://naustud.io/tech-stack/img/lodash.svg',
    desc: `
		Lodash is a JavaScript library which provides <strong>utility functions</strong> for
		common programming tasks using the functional programming paradigm.`
}, {
    cat: 'library sub', name: 'Moment JS', value: 30,
    icon: 'https://naustud.io/tech-stack/img/momentjs.png',
    desc: `
		Handy and resourceful JavaScript library to parse, validate, manipulate, and display dates and times.
	`
}, {
    cat: 'library sub', name: 'Numeral.js', value: 20,
    icon: 'Numeral.js',
    desc: `
		A javascript library for formatting and manipulating numbers.
	`
}, {
    cat: 'library sub', name: 'Redux', value: 80,
    icon: 'https://naustud.io/tech-stack/img/redux.svg',
    desc: `
		Redux is an open-source JavaScript library designed for managing
		application state. It is primarily used together with React for building user interfaces.
		Redux is inspired by Facebook’s Flux and influenced by functional programming language Elm.
	`
}, {
    cat: 'framework', name: 'Angular 2.0', value: 30,
    icon: 'https://naustud.io/tech-stack/img/angular2.svg',
    desc: `
		Angular (commonly referred to as 'Angular 2+' or 'Angular 2') is a TypeScript-based
		open-source front-end web application platform led by the Angular Team at Google and
		by a community of individuals and corporations to address all of the parts of the
		developer's workflow while building complex web applications.
	`
}, /*{
     cat: 'framework', name: 'Trails.JS', value: 10,
     icon: '',
     },*/
		      {
			  cat: 'framework', name: 'Bootstrap CSS', value: 50,
			  icon: 'https://naustud.io/tech-stack/img/bootstrap.svg',
			  desc: `
		Bootstrap is a free and open-source front-end web framework for designing websites
		and web applications. It contains HTML-and CSS-based design templates for typography,
		forms, buttons, navigation and other interface components, as well as optional JavaScript extensions.
	`
		      }, {
			  cat: 'framework', name: 'Ember JS', value: 10,
			  icon: 'https://naustud.io/tech-stack/img/ember.png',
			  desc: `
		Ember.js is an open-source JavaScript web framework, based on the Model–view–viewmodel
		(MVVM) pattern. It allows developers to create scalable single-page web applications by
		incorporating common idioms and best practices into the framework.
	`
		      }, {
			  cat: 'framework', name: 'ExpressJS', value: 30,
			  icon: 'https://naustud.io/tech-stack/img/expressjs.png',
			  desc: `
		Express.js, or simply Express, is a JavaScript framework designed for building web applications and APIs.
		It is the de facto server framework for Node.js.
	`
		      }, /*{
			   cat: 'framework', name: 'Foundation', value: 10,
			   icon: '',
			   },*/										{
			       cat: 'framework', name: 'Hexo', value: 50,
			       icon: 'https://naustud.io/tech-stack/img/hexo.png',
			       desc: `
		A fast, simple & powerful blog-aware <strong>static website</strong> generator, powered by Node.js.
	`
			   }, {
			       cat: 'framework', name: 'ReactJS', value: 100,
			       icon: 'https://naustud.io/tech-stack/img/react.png',
			       desc: `React (sometimes written React.js or ReactJS) is an open-source JavaScript framework maintained by Facebook for building user interfaces.
		React processes only user interface in applications and can be used in combination with other JavaScript libraries
		or frameworks such as Redux, Flux, Backbone...
	`
			   }, /*{
				cat: 'framework', name: 'SenchaTouch', value: 10,
				icon: '',
				},*/ 										{
				    cat: 'tooling', name: 'Atom', value: 10,
				    icon: 'https://naustud.io/tech-stack/img/atom.png',
				    desc: `
		Atom is a free and open-source text and source code editor for macOS, Linux, and Windows with support
		for plug-ins written in Node.js, and embedded Git Control, developed by GitHub.
		Atom is a desktop application built using web technologies.
	`
				}, {
				    cat: 'tooling', name: 'Google Chrome & Devtool', value: 70,
				    icon: 'https://naustud.io/tech-stack/img/chrome-devtools.svg',
				    desc: `
		<strong>Web development tools (devtool)</strong> allow web developers to test and debug their code.
		At Nau, we use the one come with Google Chrome to debug our apps. It is one the the most powerful
		and sophisticated devtool available.
	`
				}, {
				    cat: 'tooling', name: 'Jenkins CI', value: 30,
				    icon: 'https://naustud.io/tech-stack/img/jenkins.png',
				    desc: `
		Jenkins is an open source automation server. Jenkins helps to automate the non-human part of
		the whole software development process, with now common things like continuous integration,
		but by further empowering teams to implement the technical part of a Continuous Delivery.
	`
				}, {
				    cat: 'tooling', name: 'Sublime Text 3', value: 100,
				    icon: 'https://naustud.io/tech-stack/img/sublimetext.png',
				    desc: `
		Sublime Text 3 is a powerful and cross-platform source code editor. It is well-known for
		introducing the concept of multi-cursor and lots of text editing command. Besides, its
		plugin ecosystem is very rich which allows enhancing productivity to the fullest.
	`
				}, {
				    cat: 'tooling', name: 'Visual Studio Code', value: 50,
				    icon: 'https://naustud.io/tech-stack/img/vscode.png',
				    desc: `
		Visual Studio Code is a cross-platform source code editor developed by Microsoft.
		It includes support for debugging, embedded Git control, syntax highlighting,
		intelligent code completion, snippets, and code refactoring. Its extensions eco system is
		growing quickly and it is becoming the best Front End editors out there.
	`
				}, {
				    cat: 'tooling', name: 'Performance Tooling', value: 30,
				    icon: 'Performance;Tooling',
				    desc: `
		At Nau, web performance is our top priority when development web sites and applications.
		We're practicing code optimization and Front End delivery optimization from day 1.
		To measure the resuslts, we use several tools to audit and benchmark our applications,
		including (but not limit to): Chrome devtool timeline & audit, Google PageSpeed Insights, Web Page Test, Website Grader...
	`
				}, {
				    cat: 'tooling', name: 'Yeoman generator for Nau Workflow', value: 20,
				    icon: 'https://naustud.io/tech-stack/img/yeoman.png',
				    desc: `
		Yeoman is an open source, command-line interface set of tools mainly used to generate
		structure and scaffolding for new projects, especially in JavaScript and Node.js.
		At Nau, we have developed a Yeoman generator that help quickly set up new projects aligned with
		Nau's conventions and standards.
	`
				}, {
				    cat: 'tooling', name: 'live-server', value: 30,
				    icon: 'live-server',
				    desc: `
		A Node.js-based developer web server for quickly test apps and web pages with some
		magic of 'auto-reload' on the browser.
	`
				}, {
				    cat: 'tooling', name: 'PostCSS', value: 30,
				    icon: 'https://naustud.io/tech-stack/img/postcss.svg',
				    desc: `
		PostCSS is a software development tool that uses JavaScript-based plugins to automate routine CSS operations.<br>
		We use PostCSS mainly for auto-vendor-prefixing, but very soon we'll use it for NextCSS compilation.
	`
				}, {
				    cat: 'backend', name: 'Elastic Search', value: 10,
				    icon: 'Elastic;Search',
				    desc: `
		A specialized database software for high performance search queries.
	`
				}, {
				    cat: 'backend', name: 'Keystone CMS', value: 50,
				    icon: 'https://naustud.io/tech-stack/img/keystonejs.png',
				    desc: `
		The de-facto CMS system for website built with Node.js. It can be compared with
		Wordpress of PHP language.
	`
				}, {
				    cat: 'backend', name: 'KoaJS', value: 10,
				    icon: 'https://naustud.io/tech-stack/img/koajs.png',
				    desc: `
		The advanced and improved version of ExpressJS, with leaner middlewares architecture
		thanks to the avent of ES6 generators.
	`
				}, {
				    cat: 'backend', name: 'Loopback', value: 30,
				    icon: 'https://naustud.io/tech-stack/img/loopback.svg',
				    desc: `
		Powerful API-focused web framework built for Node.js. It feature easy to use configurations
		and auto API documentation page.
	`
				}, {
				    cat: 'backend', name: 'Restify', value: 20,
				    icon: 'https://naustud.io/tech-stack/img/restify.png',
				    desc: `
		High performance API development framework, built for Node.js. It has some convenient wrapper
		to automatically generate admin backoffice site and API documentation page.
	`
				}, {
				    cat: 'backend', name: 'MongoDB', value: 70,
				    icon: 'https://naustud.io/tech-stack/img/mongodb.png',
				    desc: `
		The de-facto Database solution for JavaScript and Node.js applications. It is a light weight,
		high performance NoSQL database and can be used for small and large websites.
	`
				}, {
				    cat: 'backend', name: 'NodeJS', value: 100,
				    icon: 'https://naustud.io/tech-stack/img/nodejs.svg',
				    desc: `
		Node.js is a cross-platform JavaScript runtime environment.
		Node.js allows creation of high performance and high concurrency websites with smaller footprint compared to
		other server-side solution. Node.js ecosystem is growing very fast and is trusted by a lot of big companies who
		are adopting it to enhance current products as well as for new ones.
	`
				}, {
				    cat: 'platform', name: 'Docker Platform', value: 10,
				    icon: 'https://naustud.io/tech-stack/img/docker.svg',
				    desc: `
		Docker is an open-source project that automates the deployment of applications inside software containers.
		At Nau, we're still learning this technology to later facilitate easy web app deployments.
	`
				}, {
				    cat: 'platform', name: 'MeteorJS', value: 80,
				    icon: 'https://naustud.io/tech-stack/img/meteor.svg',
				    desc: `
		MeteorJS is a free and open-source JavaScript web framework written using Node.js.
		Meteor allows for rapid prototyping and produces cross-platform (Android, iOS, Web) code.
		It integrates with MongoDB and uses the Distributed Data Protocol and a publish–subscribe
		pattern to automatically propagate data changes to clients without requiring the developer
		to write any synchronization code.
	`
				}, {
				    cat: 'platform', name: 'Phonegap', value: 50,
				    icon: 'https://naustud.io/tech-stack/img/phonegap.png',
				    desc: `
		A platform, library and tool for building hybrid mobile app.
	`
				}, {
				    cat: 'platform', name: 'Reaction Commerce', value: 20,
				    icon: 'https://naustud.io/tech-stack/img/reactioncommerce.png',
				    desc: `
		Reaction Commerce is the first open source, real-time platform to
		combine the flexibility developers and designers want with the stability
		and support businesses need. ReactionCommerce is based on MeteorJS platform.
	`
				}, {
				    cat: 'platform', name: 'ReactNative', value: 10,
				    icon: 'https://naustud.io/tech-stack/img/reactnative.png',
				    desc: `
		React Native lets you build mobile apps using only JavaScript.
		It uses the same design as React, letting us compose a rich
		mobile UI from declarative components.
	`
				}, {
				    cat: 'platform', name: 'SquareSpace', value: 30,
				    icon: 'https://naustud.io/tech-stack/img/squarespace.svg',
				    desc: `
		Squarespace is a SaaS-based content management system-integrated ecommerce-aware website builder and blogging platform.
		At Nau, we have built a website for Squarespace using their low-level API which allowed fully customization
		of the interface and other Front End functionalities.
	`
				}, {
				    cat: 'language', name: 'HTML5 & CSS3', value: 100,
				    icon: 'https://naustud.io/tech-stack/img/html5-css3.png',
				    desc: `
		The languages of the Web Front End. At Nau, they are in our blood and with them we can build
		world-class websites with any kind of visual effects or designs requested.
	`
				}, {
				    cat: 'language', name: 'JavaScript', value: 100,
				    icon: 'https://naustud.io/tech-stack/img/javascript.png',
				    desc: `
		JavaScript is the heart of modern Web front end development and essential element of any Single Page
		Applications. In Nau, we invest a good deal in training developers to have good control of this universal language
		and now caplable of developing full stack websites with only JavaScript.
	`
				}, {
				    cat: 'language', name: 'CSS Next', value: 10,
				    icon: 'https://naustud.io/tech-stack/img/cssnext.png',
				    desc: `
		The CSS language specs of the future but with the help of PostCSS (like Babel for ES6),
		we can use CSS Next today.
	`
				}, {
				    cat: 'language', name: 'GraphQL', value: 50,
				    icon: 'https://naustud.io/tech-stack/img/graphql.svg',
				    desc: `
		GraphQL is a data query language developed by Facebook publicly released in 2015.
		It provides an alternative to REST and ad-hoc webservice architectures. In combination
		with RelayJS, this combo help us reduce the time to develop web apps for weeks.
	`
				}, {
				    cat: 'language', name: 'LESS CSS', value: 20,
				    icon: 'https://naustud.io/tech-stack/img/less.svg',
				    desc: `
		A preprocessor language to be compiled to CSS. This language is not as popular nowadays and we
		only use them when requested.
	`
				}, {
				    cat: 'language', name: 'SASS (SCSS flavor)', value: 70,
				    icon: 'https://naustud.io/tech-stack/img/sass.png',
				    desc: `
		This is our main CSS preprocessor language helping us lay structured foundation to CSS as well
		as assisting on writing more convenient BEM anotations.
	`
				}, {
				    cat: 'language', name: 'TypeScript 2', value: 30,
				    icon: 'https://naustud.io/tech-stack/img/typescript.png',
				    desc: `
		The strict-typing flavor of ECMAScript, always requires a compiler to compile to vanilla JavaScript
		but the type checking and other syntactical sugar are exceptional. Right now, we only use it for
		Angular 2 projects when needed.
	`
				}, {
				    cat: 'workflow', name: 'code.naustud.io', value: 100,
				    icon: 'https://naustud.io/tech-stack/img/naustudio.svg',
				    desc: `
		A set of guidelines, presets, configs and stadard documentation for Nau developers.
		Please visit the document site at: <a href='http://code.naustud.io' target='_blank'>code.naustud.io</a>
	`
				}, {
				    cat: 'workflow', name: 'Mobile First', value: 100,
				    icon: 'Mobile First',
				    desc: `
		This is one of our most important principle for web and mobile development.
		More details will be discussed in blog later.
	`
				}, {
				    cat: 'workflow', name: 'BabelJS', value: 50,
				    icon: 'https://naustud.io/tech-stack/img/babel.png',
				    desc: `
		The de-facto tool to work with ECMAScript 6 and ReactJS nowadays.
	`
				}, /*{
				     cat: 'workflow', name: 'Browserify', value: 10,
				     icon: '',
				     },*/ 										{
					 cat: 'workflow', name: 'CSS BEM Notation', value: 70,
					 icon: 'CSS BEM Notation',
					 desc: `
		Our naming standard for CSS, which enhance collaboration, documentation and reusability of
		CSS rules.
	`
				     }, {
					 cat: 'workflow', name: 'Front End Code Guide', value: 30,
					 icon: 'Front End;Code Guide',
					 desc: `
		Based on an existing best practice document for HTML and CSS. We're adopting it as our standards
		and guideline.
	`
				     }, {
					 cat: 'workflow', name: 'ESLint', value: 20,
					 icon: 'https://naustud.io/tech-stack/img/eslint.svg',
					 desc: `
		The tool to check and validate JavaScript code when we develop and prevent potential issues with code.
	`
				     }, {
					 cat: 'workflow', name: 'Gitflow Workflow', value: 70,
					 icon: 'https://naustud.io/tech-stack/img/gitflow.png',
					 desc: `
		Our code version control tool is Git, and Gitflow is one of its workflow standard which
		ensure good collaboration and avoid conflict-resolving efforts. For more info, visit: code.naustud.io
	`
				     }, {
					 cat: 'workflow', name: 'GulpJS', value: 50,
					 icon: 'https://naustud.io/tech-stack/img/gulp.png',
					 desc: `
		GulpJS is a task automation tools written for Node.js. It is among the most popular
		Front End and Node project automation tools nowadays
	`
				     }, {
					 cat: 'workflow', name: 'Nau Code Styles', value: 50,
					 icon: 'Nau Code Styles',
					 desc: `
		Based on AirBnB's well-defined JavaScript code styles. Our derivation has some different standards such as
		TAB indentation. This code style has an accompanied ESLint config.
	`
				     }, {
					 cat: 'workflow', name: 'Stylelint', value: 50,
					 icon: 'https://naustud.io/tech-stack/img/stylelint.svg',
					 desc: `
		Our on-stop tool to validate both CSS and SCSS with a set of conventions and guidelines from our best practice.
	`
				     }, {
					 cat: 'workflow', name: 'SystemJS', value: 20,
					 icon: 'SystemJS',
					 desc: `
		A module loader library that come along Angular 2. Its use is scarce, however.
	`
				     }, {
					 cat: 'workflow', name: 'Webpack', value: 30,
					 icon: 'https://naustud.io/tech-stack/img/webpack.svg',
					 desc: `
		A module bundler library that is becoming de-facto tool to use in ReactJS or SPA apps nowadays.
	`
				     }, {
					 cat: 'legacy', name: 'AngularJS 1', value: 10,
					 icon: 'https://naustud.io/tech-stack/img/angular1.png',
					 desc: `
		Angular 1. Deprecated
	`
				     }, {
					 cat: 'legacy', name: 'Backbone', value: 30,
					 icon: 'https://naustud.io/tech-stack/img/backbone.png',
					 desc: `
		A Model-View library. Deprecated
	`
				     }, {
					 cat: 'legacy', name: 'Grunt & Automation Stack', value: 30,
					 icon: 'https://naustud.io/tech-stack/img/grunt.svg',
					 desc: `
		Grunt task automation tool. Deprecated
	`
				     }, {
					 cat: 'legacy', name: 'jQuery', value: 50,
					 icon: 'https://naustud.io/tech-stack/img/jquery.png',
					 desc: `
		Deprecated, because <a href='http://youmightnotneedjquery.com/' target='_blank'>youmightnotneedjquery.com</a>
	`
				     }, {
					 cat: 'legacy', name: 'RequireJS & AMD', value: 30,
					 icon: 'https://naustud.io/tech-stack/img/requirejs.svg',
					 desc: `
		AMD module loader. Deprecated and replaced by ES module and Webpack.
	`
				     }, {
					 cat: 'legacy tooling', name: 'Browser Sync', value: 40,
					 icon: 'Browser Sync',
					 desc: `
		Web development server popular among gulp/grunt web apps. No deprecated and replaced by live-server
		or webpackDevServer.
	`
				     }, {
					 cat: 'legacy tooling', name: 'Git Pre-commit', value: 30,
					 icon: 'Git;Pre-commit',
					 desc: `
		Pre-commit hook for git, now deprecated due to slow commit time. Code validation should be done
		in the code editor.
	`
				     }, {
					 cat: 'legacy tooling', name: 'http-server', value: 20,
					 icon: 'http-server',
					 desc: `
		A quick test web server based on Node.js, deprecated and replaced by live-server.
	`
				     }, {
					 cat: 'legacy tooling', name: 'LiveReload', value: 20,
					 icon: 'Live;Reload',
					 desc: `
		A propritery auto-reload solution for web developers, now deprecated in favor of live-server and
		hot module reload in Webpack.
	`
				     }];



function RamaddaD3bubbleDisplay(displayManager, id, properties) {
    const ID_BUBBLES = "bubbles";
    var SUPER;
    RamaddaUtil.inherit(this, SUPER = new RamaddaDisplay(displayManager, id,
							 DISPLAY_D3BUBBLE, properties));
    addRamaddaDisplay(this);
    if(!window["BubbleChart"]) {
	Utils.importJS(ramaddaBaseUrl +"/lib/d3/d3-legend.min.js");
	Utils.importJS(ramaddaBaseUrl +"/lib/d3/bubblechart.js");
    }
    RamaddaUtil.defineMembers(this, {
        needsData: function() {
            return true;
        },
	callbackWaiting:false,
	getWikiEditorTags: function() {
	    return Utils.mergeLists(SUPER.getWikiEditorTags(),
				    [
					"label:Bubble Chart",
					'labelField=""',
					'categoryField=""',
					'valueField=""',
					'sizeLabel1=""',
					'sizeLabel2=""',
					'showSizeLegend=false'
				    ])},

        updateUI: function() {
            if(!window["BubbleChart"]) {
		if(!this.callbackWaiting) {
		    this.callbackWaiting = true;
                    setTimeout(()=>{
			this.callbackWaiting = false;
			this.updateUI()
		    },100);
		}
                return;
            }
	    
	    var records = this.filterData();
	    if (!records) {
                return;
	    }  
	    let data =[];

	    let html = HtmlUtil.tag("svg", ["id", this.getDomId(ID_BUBBLES),
					    "width","100%","height","700", "font-family","sans-serif","font-size","10", "text-anchor","middle"])
	    this.jq(ID_DISPLAY_CONTENTS).html(html);
            let categoryField = this.getFieldById(null, this.getProperty("categoryField"));
	    let valueField = this.getFieldById(null, this.getProperty("valueField"));
	    let values;
	    let min = 0;
	    let max = 0;
	    if(valueField) {
		values =  this.getColumnValues(records, valueField).values;
		values.map((v,idx)=>{
		    min  = idx==0?v:Math.min(v,min);
		    max  = idx==0?v:Math.max(v,max);
		});
	    }

	    let labelField = this.getFieldById(null, this.getProperty("labelField","name"));
	    let imageField = this.getFieldById(null, this.getProperty("imageField"));	    
	    let template = this.getProperty("template","${default}");
	    if(!labelField) {
                this.setContents(this.getMessage("No label field found"));
		return
	    }

	    records.map(r=>{
		let desc =  this.getRecordHtml(r,null, template);
		let label = r.getValue(labelField.getIndex());
		let obj = {
		    name: label,
		    icon:label,
		    desc:desc,
		    cat:"",
		    value:10,

		}
		if(categoryField)
		    obj.cat = r.getValue(categoryField.getIndex());
		if(valueField)
		    obj.value = r.getValue(valueField.getIndex());
		if(imageField)
		    obj.icon = r.getValue(imageField.getIndex());
		data.push(obj);
	    });
//	    new BubbleChart("#"+this.getDomId(ID_BUBBLES),bubbleTestData);
	    let colors =  this.getColorTable(true);
	    new BubbleChart("#"+this.getDomId(ID_BUBBLES),data,
			    {label1:this.getProperty("sizeLabel1"),
			     label2: this.getProperty("sizeLabel2"), 
			     colors:colors,
			     showSizeLegend: this.getProperty("showSizeLegend",valueField!=null)
});
	}
    })
}

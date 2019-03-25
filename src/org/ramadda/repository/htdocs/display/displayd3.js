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
                this.updateUI();
        },
        updateUI: async function() {
         if(!this.loadedResources) {
            await Utils.importCSS(ramaddaBaseHtdocs+"/lib/skewt/sounding.css");
            await Utils.importJS(ramaddaBaseHtdocs +"/lib/skewt/d3skewt.js");
            this.loadedResources = true;
         }
         if(!window["D3Skewt"]) {
             setTimeout(()=>this.updateUI(),100);
             return;
         }
         SUPER.updateUI.call(this);
         var skewtId = this.getDomId(ID_SKEWT);
         var html = HtmlUtils.div(["id", skewtId], "");
         this.setContents(html);
         var pointData = this.getData();
         if (pointData == null) return;
         var records =  pointData.getRecords();
         if (!records || records.length==0) {
             console.log("no data yet");
             return;
         }
         var date = records[0].getDate();
         if(this.jq(ID_DATE_LABEL).size()==0) {
             this.jq(ID_TOP_LEFT).append(HtmlUtils.div(["id",this.getDomId(ID_DATE_LABEL)]));
         }
         if(date!=null) {
             this.jq(ID_TOP_LEFT).html("Date: " + this.formatDate(date));
         } else {
             this.jq(ID_TOP_LEFT).html("");
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
            //            options.hodographWidth = 200;
            var fields = this.getData().getRecordFields();
            var names = ["pressure","height","temperature","dewpoint","wind_direction","wind_speed"];
            var pressure = this.getFieldById(fields,"pressure");
            var height = this.getFieldById(fields,"height");
            var height = this.getFieldById(fields,"height");
            var data ={};

            for(var i=0;i<names.length;i++) {
                var field = this.getFieldById(fields,names[i]);
                if(field == null) {
                    this.displayError("No " + names[i] + " defined in data");
                    return;
                }
                var values = this.getColumnValues(records, field).values;
                data[names[i]] = values;
            }
            this.skewt = new D3Skewt(skewtId, options,data);
            //            console.log(this.jq(ID_SKEWT).html());
            await this.getDisplayEntry((e)=>{
                    var q= e.getAttribute("variables");
                    if(!q) return;
                    q = q.value;
                    q = q.replace(/\r\n/g,"\n");
                    q = q.replace(/^ *\n/,"");
                    q = q.replace(/^ *([^:]+):([^\n].*)$/gm,"<div title='$1' class=display-skewt-index-label>$1</div>: <div title='$2'  class=display-skewt-index>$2</div>");
                    q = q.replace(/[[\r\n]/g,"\n");
                                  //                    console.log(q);
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
                console.log("not there");
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
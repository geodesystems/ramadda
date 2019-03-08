/**
Copyright 2008-2019 Geode Systems LLC
*/

var CATEGORY_PLOTLY = "More Charts";
var DISPLAY_PLOTLY_RADAR = "radar";
var DISPLAY_PLOTLY_WINDROSE = "windrose";
var DISPLAY_PLOTLY_DENSITY = "density";
var DISPLAY_PLOTLY_DOTPLOT = "dotplot";
var DISPLAY_PLOTLY_SPLOM = "splom";
var DISPLAY_PLOTLY_3DSCATTER = "3dscatter";
var DISPLAY_PLOTLY_3DMESH = "3dmesh";
var DISPLAY_PLOTLY_TREEMAP = "ptreemap";
var DISPLAY_PLOTLY_TERNARY = "ternary";

addGlobalDisplayType({
    type: DISPLAY_PLOTLY_RADAR,
    label: "Radar",
    requiresData: true,
    forUser: true,
    category: CATEGORY_PLOTLY
});
addGlobalDisplayType({
    type: DISPLAY_PLOTLY_WINDROSE,
    label: "Wind Rose",
    requiresData: true,
    forUser: true,
    category: CATEGORY_PLOTLY
});
addGlobalDisplayType({
    type: DISPLAY_PLOTLY_DENSITY,
    label: "Density",
    requiresData: true,
    forUser: true,
    category: CATEGORY_PLOTLY
});
addGlobalDisplayType({
    type: DISPLAY_PLOTLY_DOTPLOT,
    label: "Dot Plot",
    requiresData: true,
    forUser: true,
    category: CATEGORY_PLOTLY
});
addGlobalDisplayType({
    type: DISPLAY_PLOTLY_SPLOM,
    label: "Splom",
    requiresData: true,
    forUser: true,
    category: CATEGORY_PLOTLY
});
addGlobalDisplayType({
    type: DISPLAY_PLOTLY_3DSCATTER,
    label: "3D Scatter",
    requiresData: true,
    forUser: true,
    category: CATEGORY_PLOTLY
});
addGlobalDisplayType({
    type: DISPLAY_PLOTLY_3DMESH,
    label: "3D Mesh",
    requiresData: true,
    forUser: true,
    category: CATEGORY_PLOTLY
});
//Ternary doesn't work
//addGlobalDisplayType({type: DISPLAY_PLOTLY_TERNARY, label:"Ternary",requiresData:true,forUser:true,category:CATEGORY_PLOTLY});
//Treempap doesn't work
//addGlobalDisplayType({type: DISPLAY_PLOTLY_PTREEMAP, label:"Tree Map",requiresData:true,forUser:true,category:CATEGORY_PLOTLY});


function RamaddaPlotlyDisplay(displayManager, id, type, properties) {
    let SUPER = new RamaddaFieldsDisplay(displayManager, id, type, properties);
    RamaddaUtil.inherit(this, SUPER);
    RamaddaUtil.defineMembers(this, {
        needsData: function() {
            return true;
        },
        setDimensions: function(layout, widthDelta) {
            //                var width  = parseInt(this.getProperty("width","400").replace("px","").replace("%",""));
            var height = parseInt(this.getProperty("height", "400").replace("px", "").replace("%", ""));
            //                layout.width = width-widthDelta;
            layout.height = height;
        },
        pointDataLoaded: function(pointData, url, reload) {
            SUPER.pointDataLoaded.call(this, pointData, url, reload);
            if (this.dataCollection)
                this.displayManager.propagateEventRecordSelection(this,
                    this.dataCollection.getList()[0], {
                        index: 0
                    });

        },
        displayData: function() {
            this.updateUI();
        },
        pageHasLoaded: function() {
            SUPER.pageHasLoaded.call(this);
            this.updateUI();
        },

        fieldSelectionChanged: function() {
            SUPER.fieldSelectionChanged.call(this);
            this.updateUI();
        },
        makeAxis: function(title, tickangle) {
            return {
                title: title,
                titlefont: {
                    size: 20
                },
                tickangle: tickangle,
                tickfont: {
                    size: 15
                },
                tickcolor: 'rgba(0,0,0,0)',
                ticklen: 5,
                showline: true,
                showgrid: true
            };
        },
        getDisplayStyle: function() {
            return "";
        },
        makePlot: function(data, layout) {
            this.clearHtml();
            //For some reason plotly won't display repeated times in the DISPLAY div
            this.jq(ID_DISPLAY_CONTENTS).html(HtmlUtils.div(["id", this.getDomId("tmp"), "style", this.getDisplayStyle()], ""));
            //               Plotly.plot(this.getDomId(ID_DISPLAY_CONTENTS), data, layout)
            var plot = Plotly.plot(this.getDomId("tmp"), data, layout);
            var myPlot = document.getElementById(this.getDomId("tmp"));
            this.addEvents(plot, myPlot);
        },
        addEvents: function(plot, myPlot) {}
    });
}



function RamaddaRadialDisplay(displayManager, id, type, properties) {
    var SUPER;
    $.extend(this, {
        width: "400px",
        height: "400px",
    });
    RamaddaUtil.inherit(this, SUPER = new RamaddaPlotlyDisplay(displayManager, id, type, properties));
    RamaddaUtil.defineMembers(this, {
        getPlotType: function() {
            return 'barpolar';
        },
        updateUI: function() {
            var records = this.filterData();
            if (!records) {
                return;
            }
            var fields = this.getSelectedFields(this.getData().getRecordFields());
            var stringField = this.getFieldOfType(fields, "string");
            if (!stringField) {
                this.displayError("No string field specified");
                return;
            }
            var numericFields = this.getFieldsOfType(fields, "numeric");
            if (numericFields.length == 0) {
                this.displayError("No numeric fields specified");
                return;
            }
            var theta = this.getColumnValues(records, stringField).values;
            var values = [];
            var min = Number.MAX_VALUE;
            var max = Number.MIN_VALUE;
            var plotData = [];
            for (var i = 0; i < numericFields.length; i++) {
                var field = numericFields[i];
                var column = this.getColumnValues(records, field);
                min = Math.min(min, column.min);
                max = Math.max(max, column.max);
                plotData.push({
                    type: this.getPlotType(),
                    r: column.values,
                    theta: theta,
                    fill: 'toself',
                    name: field.getLabel()
                });
            }

            layout = {
                polar: {
                    radialaxis: {
                        visible: true,
                        range: [min, max]
                    }
                },
            }
            this.setDimensions(layout, 2);
            this.makePlot(plotData, layout);
        },
    });
}

function RamaddaRadarDisplay(displayManager, id, properties) {
    var SUPER;
    RamaddaUtil.inherit(this, SUPER = new RamaddaRadialDisplay(displayManager, id, DISPLAY_PLOTLY_RADAR, properties));

    RamaddaUtil.defineMembers(this, {
        getPlotType: function() {
            return 'scatterpolar';
        },
    });
    addRamaddaDisplay(this);
}

function RamaddaWindroseDisplay(displayManager, id, properties) {
    var SUPER;
    RamaddaUtil.inherit(this, SUPER = new RamaddaRadialDisplay(displayManager, id, DISPLAY_PLOTLY_WINDROSE, properties));
    RamaddaUtil.defineMembers(this, {
        getPlotType: function() {
            return 'barpolar';
        },
    });
    addRamaddaDisplay(this);
}




function RamaddaDensityDisplay(displayManager, id, properties) {
    var SUPER;
    $.extend(this, {
        width: "600px",
        height: "400px",
    });
    RamaddaUtil.inherit(this, SUPER = new RamaddaPlotlyDisplay(displayManager, id, DISPLAY_PLOTLY_DENSITY, properties));
    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
        updateUI: function() {
            var records = this.filterData();
            if (!records) return;
            var fields = this.getSelectedFields(this.getData().getRecordFields());
            var numericFields = this.getFieldsOfType(fields, "numeric");
            if (numericFields.length < 2) {
                this.displayError("No numeric fields specified");
                return;
            }

            var x = this.getColumnValues(records, numericFields[0]);
            var y = this.getColumnValues(records, numericFields[1]);
            var markers = {
                x: x.values,
                y: y.values,
                mode: 'markers',
                name: "",
                marker: {
                    color: this.getProperty("pointColor", 'rgb(102,0,0)'),
                    size: parseInt(this.getProperty("markerSize", "4")),
                    opacity: 0.4
                },
                type: 'scatter'
            };
            var density = {
                x: x.values,
                y: y.values,
                name: 'density',
                ncontours: 20,
                colorscale: 'Hot',
                reversescale: true,
                type: 'histogram2dcontour'
            };
            var plotData = [];
            if (this.getProperty("showDensity", true))
                plotData.push(density);
            if (this.getProperty("showPoints", true))
                plotData.push(markers);
            var layout = {
                showlegend: true,
                autosize: true,
                margin: {
                    t: 50
                },
                hovermode: 'closest',
                bargap: 0,
                xaxis: {
                    domain: [x.min, x.max],
                    showline: this.getProperty("showLines", true),
                    showgrid: this.getProperty("showLines", true),
                    title: fields[0].getLabel()
                },
                yaxis: {
                    domain: [y.min, y.max],
                    showline: this.getProperty("showLines", true),
                    showgrid: this.getProperty("showLines", true),
                    title: fields[1].getLabel()
                },
            };
            this.setDimensions(layout, 2);
            this.makePlot(plotData, layout);
        },
    });
}


function RamaddaPlotly3DDisplay(displayManager, id, type, properties) {
    var SUPER;
    $.extend(this, {
        width: "400px",
        height: "400px",
    });
    RamaddaUtil.inherit(this, SUPER = new RamaddaPlotlyDisplay(displayManager, id, type, properties));

    RamaddaUtil.defineMembers(this, {
        addEvents: function(plot, myPlot) {
            myPlot.on('plotly_click', function() {
                //                        alert('You clicked this Plotly chart!');
            });
        },

        getDisplayStyle: function() {
            return "border: 1px #ccc solid;";
        },
        get3DType: function() {
            //                'mesh3d'
            return 'scatter3d';
        },
        updateUI: function() {
            var records = this.filterData();
            if (!records) return;
            var fields = this.getSelectedFields(this.getData().getRecordFields());
            var stringField = this.getFieldOfType(fields, "string");
            var fields = this.getFieldsOfType(fields, "numeric");
            if (fields.length == 0) {
                this.displayError("No numeric fields specified");
                return;
            }
            if (fields.length < 3) {
                this.displayError("Don't have 3 numeric fields specified");
                return;
            }
            var x = this.getColumnValues(records, fields[0]);
            var y = this.getColumnValues(records, fields[1]);
            var z = this.getColumnValues(records, fields[2]);

            var trace1 = {
                x: x.values,
                y: y.values,
                z: z.values,
                mode: 'markers',
                marker: {
                    size: 12,
                    line: {
                        color: 'rgba(217, 217, 217, 0.14)',
                        width: 0.5
                    },
                    opacity: 0.8
                },
                type: this.get3DType()
            };


            var plotData = [trace1];


            var layout = {
                scene: {
                    xaxis: {
                        backgroundcolor: "rgb(200, 200, 230)",
                        gridcolor: "rgb(255, 255, 255)",
                        showbackground: true,
                        zerolinecolor: "rgb(255, 255, 255)",
                        title: fields[0].getLabel(),
                    },
                    yaxis: {
                        backgroundcolor: "rgb(230, 200,230)",
                        gridcolor: "rgb(255, 255, 255)",
                        showbackground: true,
                        zerolinecolor: "rgb(255, 255, 255)",
                        title: fields[1].getLabel(),
                    },
                    zaxis: {
                        backgroundcolor: "rgb(230, 230,200)",
                        gridcolor: "rgb(255, 255, 255)",
                        showbackground: true,
                        zerolinecolor: "rgb(255, 255, 255)",
                        title: fields[2].getLabel(),
                    }
                },
                margin: {
                    l: 0,
                    r: 0,
                    b: 50,
                    t: 50,
                    pad: 4
                },
            };
            this.setDimensions(layout, 2);
            this.makePlot(plotData, layout);
        },
    });
}


function Ramadda3dmeshDisplay(displayManager, id, properties) {
    var SUPER;
    $.extend(this, {
        width: "100%",
        height: "100%",
    });
    RamaddaUtil.inherit(this, SUPER = new RamaddaPlotly3DDisplay(displayManager, id, DISPLAY_PLOTLY_3DMESH, properties));
    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
        get3DType: function() {
            return 'mesh3d';
        },
    });
}



function Ramadda3dscatterDisplay(displayManager, id, properties) {
    var SUPER;
    $.extend(this, {
        width: "100%",
        height: "100%",
    });
    RamaddaUtil.inherit(this, SUPER = new RamaddaPlotly3DDisplay(displayManager, id, DISPLAY_PLOTLY_3DSCATTER, properties));
    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
        get3DType: function() {
            return 'scatter3d';
        },
    });
}




function RamaddaTernaryDisplay(displayManager, id, properties) {
    var SUPER;
    $.extend(this, {
        width: "400px",
        height: "400px",
    });
    RamaddaUtil.inherit(this, SUPER = new RamaddaPlotlyDisplay(displayManager, id, DISPLAY_PLOTLY_TERNARY, properties));
    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
        updateUI: function() {
            var records = this.filterData();
            if (!records) return;
            var fields = this.getSelectedFields(this.getData().getRecordFields());
            var stringField = this.getFieldOfType(fields, "string");
            var fields = this.getFieldsOfType(fields, "numeric");
            if (fields.length == 0) {
                this.displayError("No numeric fields specified");
                return;
            }
            if (fields.length < 3) {
                this.displayError("Don't have 3 numeric fields specified");
                return;
            }

            var rawData = [];
            var a = this.getColumnValues(records, fields[0]);
            var b = this.getColumnValues(records, fields[1]);
            var c = this.getColumnValues(records, fields[2]);
            for (var i = 0; i < a.length; i++) {
                rawData.push({
                    a: 100 * a[i] / a.max,
                    b: 100 * b[i] / b.max,
                    c: 100 * c[i] / c.max,

                    label: stringField ? stringField.getLabel() : "Point " + (i + 1)
                });
            }
            var xrawData = [{
                a: 75,
                b: 25,
                c: 0,
                label: 'point 1'
            }, {
                a: 70,
                b: 10,
                c: 20,
                label: 'point 2'
            }, {
                a: 75,
                b: 20,
                c: 5,
                label: 'point 3'
            }, {
                a: 5,
                b: 60,
                c: 35,
                label: 'point 4'
            }, {
                a: 10,
                b: 80,
                c: 10,
                label: 'point 5'
            }, {
                a: 10,
                b: 90,
                c: 0,
                label: 'point 6'
            }, {
                a: 20,
                b: 70,
                c: 10,
                label: 'point 7'
            }, {
                a: 10,
                b: 20,
                c: 70,
                label: 'point 8'
            }, {
                a: 15,
                b: 5,
                c: 80,
                label: 'point 9'
            }, {
                a: 10,
                b: 10,
                c: 80,
                label: 'point 10'
            }, {
                a: 20,
                b: 10,
                c: 70,
                label: 'point 11'
            }, ];


            var plotData = [{
                type: 'scatterternary',
                mode: 'markers',
                a: rawData.map(function(d) {
                    return d.a;
                }),
                b: rawData.map(function(d) {
                    return d.b;
                }),
                c: rawData.map(function(d) {
                    return d.c;
                }),
                text: rawData.map(function(d) {
                    return d.label;
                }),
                marker: {
                    symbol: 100,
                    color: '#DB7365',
                    size: 14,
                    line: {
                        width: 2
                    }
                },
            }];
            var layout = {
                ternary: {
                    sum: 100,
                    aaxis: this.makeAxis(fields[0].getLabel(), 0),
                    baxis: this.makeAxis(fields[1].getLabel(), 45),
                    caxis: this.makeAxis(fields[2].getLabel(), -45),
                    bgcolor: '#fff1e0'
                },
                annotations: [{
                    showarrow: false,
                    text: this.getProperty("chartTitle", ""),
                    x: 1.0,
                    y: 1.3,
                    font: {
                        size: 15
                    }
                }],
                paper_bgcolor: '#fff1e0',
            };


            this.setDimensions(layout, 2);
            this.makePlot(plotData, layout);
        },
    });
}


function RamaddaDotplotDisplay(displayManager, id, properties) {

    $.extend(this, {
        width: "600px",
        height: "400px",
    });
    let SUPER = new RamaddaPlotlyDisplay(displayManager, id, DISPLAY_PLOTLY_DOTPLOT, properties);
    RamaddaUtil.inherit(this, SUPER);


    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
        getDisplayStyle: function() {
            return "border: 1px #ccc solid;";
        },

        updateUI: function() {
            var records = this.filterData();
            if (!records) return;
            var fields = this.getSelectedFields(this.getData().getRecordFields());
            var stringField = this.getFieldOfType(fields, "string");
            if(!stringField) {
                stringField = fields[0];
            }

            var fields = this.getFieldsOfType(fields, "numeric");
            if (fields.length == 0) {
                this.displayError("No numeric fields specified");
                return;
            }

            var labels = null;
            var labelName = "";
            if (stringField) {
                labels = this.getColumnValues(records, stringField).values;
                labelName = stringField.getLabel();
            }
            var colors = this.getColorTable(true);
            if (!colors)
                colors = ['rgba(156, 165, 196, 0.95)', 'rgba(204,204,204,0.95)', 'rgba(255,255,255,0.85)', 'rgba(150,150,150,0.95)']
            var plotData = [];
            for (i in fields) {
                var color = i >= colors.length ? colors[0] : colors[i];
                var field = fields[i];
                var values = this.getColumnValues(records, field).values;
                if (!labels) {
                    labels = [];
                    for (var j = 0; j < values.length; j++) {
                        labels.push("Point " + (j + 1));
                    }
                }

                plotData.push({
                    type: 'scatter',
                    x: values,
                    y: labels,
                    mode: 'markers',
                    name: field.getLabel(),
                    marker: {
                        color: color,
                        line: {
                            color: 'rgba(156, 165, 196, 1.0)',
                            width: 1,
                        },
                        symbol: 'circle',
                        size: 16
                    }
                });
            }



            var layout = {
                title: '',
                yaxis: {
                    title: this.getProperty("yAxisTitle", labelName),
                    showline: this.getProperty("yAxisShowLine", true),
                    showgrid: this.getProperty("yAxisShowGrid", true),
                },
                xaxis: {
                    title: this.getProperty("xAxisTitle", fields[i].getLabel()),
                    showgrid: this.getProperty("xAxisShowGrid", false),
                    showline: this.getProperty("xAxisShowLine", true),
                    linecolor: 'rgb(102, 102, 102)',
                    titlefont: {
                        font: {
                            color: 'rgb(204, 204, 204)'
                        }
                    },
                    tickfont: {
                        font: {
                            color: 'rgb(102, 102, 102)'
                        }
                    },
                    autotick: true,
                    ticks: 'outside',
                    tickcolor: 'rgb(102, 102, 102)'
                },
                margin: {
                    l: this.getProperty("marginLeft", 140),
                    r: this.getProperty("marginRight", 40),
                    b: this.getProperty("marginBottom", 50),
                    t: this.getProperty("marginTop", 20),
                },
                legend: {
                    font: {
                        size: 10,
                    },
                    yanchor: 'middle',
                    xanchor: 'right'
                },
                paper_bgcolor: this.getProperty("paperBackground", 'rgb(254, 247, 234)'),
                plot_bgcolor: this.getProperty("plotBackground", 'rgb(254, 247, 234)'),
                hovermode: 'closest'
            };
            this.setDimensions(layout, 2);
            this.makePlot(plotData, layout);
        },
    });
}



function RamaddaSplomDisplay(displayManager, id, properties) {
    var SUPER;
    $.extend(this, {
        width: "600px",
        height: "600px",
    });
    RamaddaUtil.inherit(this, SUPER = new RamaddaPlotlyDisplay(displayManager, id, DISPLAY_PLOTLY_SPLOM, properties));

    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
        setDimensions: function(layout, widthDelta) {
            var width = parseInt(this.getProperty("width", "400").replace("px", "").replace("%", ""));
            var height = parseInt(this.getProperty("height", "400").replace("px", "").replace("%", ""));
            layout.width = width - widthDelta;
            layout.height = height;
        },
        makeAxis: function() {
            return {
                showline: false,
                zeroline: false,
                gridcolor: this.getProperty("gridColor", "white"),
                ticklen: 2,
                tickfont: {
                    size: this.getProperty("tickFontSize", 12)
                },
                titlefont: {
                    size: this.getProperty("titleFontSize", 12)
                }
            }
        },
        updateUI: function() {
            var records = this.filterData();
            if (!records) return;
            var fields = this.getSelectedFields(this.getData().getRecordFields());
            if (fields.length == 0) {
                fields = this.getData().getRecordFields();
            }
            var labels;
            if (this.getProperty("labels"))
                labels = this.getProperty("labels").split(",");

            var dataObj = {
                type: 'splom',
                dimensions: [],
                marker: {
                    size: parseInt(this.getProperty("markerSize", 5)),
                    line: {
                        color: this.getProperty("lineColor", 'white'),
                        width: 0.5
                    }
                }
            };


            var colorByField = this.getFieldById(fields, this.getProperty("colorBy"));
            var colorBy = this.getProperty("colorBy");
            if (colorBy) {
                var colorByField = this.getFieldById(fields, colorBy);
                if (colorByField) {
                    var obj = this.getColumnValues(records, colorByField);
                    var colors = this.getColorTable();
                    if (!colors) colors = Utils.getColorTable("blue_white_red");
                    var colorscale = [];
                    var min = parseFloat(this.getProperty("colorByMin", obj.min));
                    var max = parseFloat(this.getProperty("colorByMax", obj.max));
                    if (Utils.isDefined(colors.min)) {
                        var clippedColors = [];
                        for (var i = 0; i < colors.colors.length; i++) {
                            var percent = i / colors.colors.length;
                            var value = colors.min + (colors.max - colors.min) * percent;
                            if (value >= min && value <= max)
                                clippedColors.push(colors.colors[i]);
                        }
                        colors = clippedColors;
                    }
                    if (colors.colors) colors = colors.colors;
                    var range = max - min;
                    var colorValues = [];
                    for (var i = 0; i < obj.values.length; i++) {
                        var value = obj.values[i];
                        var percent = (value - min) / range;
                        colorValues.push(percent);
                    }
                    for (var i = 0; i < colors.length; i++) {
                        var value = i / colors.length;
                        var next = (i + 1) / colors.length;
                        colorscale.push([value, colors[i]]);
                        colorscale.push([next, colors[i]]);
                    }
                    dataObj.marker.color = colorValues;
                    dataObj.marker.colorscale = colorscale;
                }

                this.displayColorTable(colors, ID_DISPLAY_BOTTOM, min, max);
            }

            var stringField = this.getFieldOfType(fields, "string");
            if (stringField) {
                var l = this.getColumnValues(records, stringField).values;
                dataObj.text = [];
                for (var i = 0; i < l.length; i++)
                    dataObj.text.push(stringField.getLabel() + ": " + l[i]);
            }

            var plotData = [dataObj];
            var layout = {
                autosize: false,
                hovermode: 'closest',
                dragmode: 'select',
                plot_bgcolor: this.getProperty("bgColor", 'rgba(240,240,240, 0.95)'),
                margin: {
                    l: this.getProperty("marginLeft", 140),
                    r: this.getProperty("marginRight", 40),
                    b: this.getProperty("marginBottom", 50),
                    t: this.getProperty("marginTop", 20),
                },
            }

            var cnt = 0;
            for (var i = 0; i < fields.length; i++) {
                var field = fields[i];
                if (!field.isFieldNumeric()) continue;
                var values = this.getColumnValues(records, field).values;
                var label;
                if (labels && i < labels.length)
                    label = labels[i];
                else
                    label = field.getUnitLabel();
                dataObj.dimensions.push({
                    label: label,
                    values: values
                });
                var key = "axis" + (cnt == 0 ? "" : "" + (cnt + 1));
                layout["x" + key] = this.makeAxis();
                layout["y" + key] = this.makeAxis();
                cnt++;
            }
            this.setDimensions(layout, 2);
            this.makePlot(plotData, layout);
        },
    });
}



function RamaddaPTreemapDisplay(displayManager, id, properties) {
    var SUPER;
    $.extend(this, {
        width: "400px",
        height: "400px",
    });
    RamaddaUtil.inherit(this, SUPER = new RamaddaPlotlyDisplay(displayManager, id, DISPLAY_PLOTLY_TREEMAP, properties));
    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
        updateUI: function() {
            var records = this.filterData();
            if (!records) return;
            var selectedFields = this.getSelectedFields(this.getData().getRecordFields());
            var field = this.getFieldOfType(selectedFields, "numeric");
            if (!field) {
                this.displayError("No numeric field specified");
                return;
            }
            var values = this.getColumnValues(records, field).data;
            // declaring arrays
            var shapes = [];
            var annotations = [];
            var counter = 0;

            // For Hover Text
            var x_trace = [];
            var y_trace = [];
            var text = [];

            //colors
            var colors = this.getColorTable();
            if (colors.colors) colors = colors.colors;

            // Generate Rectangles using Treemap-Squared
            var rectangles = Treemap.generate(values, 100, 100);

            for (var i in rectangles) {
                var shape = {
                    type: 'rect',
                    x0: rectangles[i][0],
                    y0: rectangles[i][1],
                    x1: rectangles[i][2],
                    y1: rectangles[i][3],
                    line: {
                        width: 2
                    },
                    fillcolor: colors[counter]
                };
                shapes.push(shape);
                var annotation = {
                    x: (rectangles[i][0] + rectangles[i][2]) / 2,
                    y: (rectangles[i][1] + rectangles[i][3]) / 2,
                    text: String(values[counter]),
                    showarrow: false
                };
                annotations.push(annotation);

                // For Hover Text
                x_trace.push((rectangles[i][0] + rectangles[i][2]) / 2);
                y_trace.push((rectangles[i][1] + rectangles[i][3]) / 2);
                text.push(String(values[counter]));

                // Incrementing Counter
                counter++;
            }

            // Generating Trace for Hover Text
            var trace0 = {
                x: x_trace,
                y: y_trace,
                text: text,
                mode: 'text',
                type: 'scatter'
            };

            var layout = {
                height: 700,
                width: 700,
                shapes: shapes,
                hovermode: 'closest',
                annotations: annotations,
                xaxis: {
                    showgrid: false,
                    zeroline: false
                },
                yaxis: {
                    showgrid: false,
                    zeroline: false
                }
            };

            var data = {
                data: [trace0]
            };
            this.setDimensions(layout, 2);
            this.makePlot([trace0], layout);
        },
    });





}
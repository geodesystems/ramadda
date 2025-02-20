/**
   Copyright (c) 2008-2025 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/


const DISPLAY_PLOTLY_RADAR = "radar";
const DISPLAY_PLOTLY_WINDROSE = "windrose";
const DISPLAY_PLOTLY_DENSITY = "density";
const DISPLAY_PLOTLY_DOTPLOT = "dotplot";
const DISPLAY_PLOTLY_SPLOM = "splom";
const DISPLAY_PLOTLY_PROFILE = "profile";
const DISPLAY_PLOTLY_3DSCATTER = "3dscatter";
const DISPLAY_PLOTLY_3DMESH = "3dmesh";
const DISPLAY_PLOTLY_TREEMAP = "ptreemap";
const DISPLAY_PLOTLY_TERNARY = "ternary";
const DISPLAY_PLOTLY_SUNBURST= "sunburst";
const DISPLAY_PLOTLY_TEXTCOUNT = "textcount";
const DISPLAY_PLOTLY_COMBOCHART = "combochart";
const DISPLAY_PLOTLY_PARCOORDS = "parcoords";

addGlobalDisplayType({
    type: DISPLAY_PLOTLY_RADAR,
    label: "Radar",
    category: CATEGORY_RADIAL_ETC,
    tooltip: makeDisplayTooltip('Radar Plot','radar.png')
});
addGlobalDisplayType({
    type: DISPLAY_PLOTLY_WINDROSE,
    label: "Wind Rose",
    category: CATEGORY_RADIAL_ETC,
    tooltip: makeDisplayTooltip('Wind Rose Plot','windrose.png')
});
addGlobalDisplayType({
    type: DISPLAY_PLOTLY_SUNBURST,
    label: "Sunburst",
    category: CATEGORY_RADIAL_ETC,
    tooltip: makeDisplayTooltip('Sunburst Plot','sunburst.png')                            
});
addGlobalDisplayType({
    type: DISPLAY_PLOTLY_DENSITY,
    label: "Density",
    category: CATEGORY_RADIAL_ETC,
    tooltip: makeDisplayTooltip('Density Plot','density.png',null)                                    
});
addGlobalDisplayType({
    type: DISPLAY_PLOTLY_COMBOCHART,
    label: "Combo Chart",
    category: CATEGORY_CHARTS,
    tooltip: makeDisplayTooltip('Combo Chart','combochart.png','Display line and bar chart')
});

addGlobalDisplayType({
    type: DISPLAY_PLOTLY_PARCOORDS,
    label: "Parallel Coords",
    category: CATEGORY_RADIAL_ETC,
    tooltip: makeDisplayTooltip('Parallel Coordinates','parallel.png',null)
});

addGlobalDisplayType({
    type: DISPLAY_PLOTLY_DOTPLOT,
    label: "Dot Plot",
    category: CATEGORY_CHARTS,
    tooltip: makeDisplayTooltip('Dot Plot', 'dotplot.png')
});
addGlobalDisplayType({
    type: DISPLAY_PLOTLY_SPLOM,
    label: "Splom",
    category: CATEGORY_RADIAL_ETC,
    tooltip: makeDisplayTooltip('Splom','splom.png')    
});
addGlobalDisplayType({
    type: DISPLAY_PLOTLY_3DSCATTER,
    label: "3D Scatter",
    category: CATEGORY_RADIAL_ETC,
    tooltip: makeDisplayTooltip('3D Scatter','3dscatter.png')    
});
addGlobalDisplayType({
    type: DISPLAY_PLOTLY_PROFILE,
    label: "Profile",
    category: CATEGORY_CHARTS,
    tooltip: makeDisplayTooltip('Profile','profile.png')                    
});
addGlobalDisplayType({
    type: DISPLAY_PLOTLY_3DMESH,
    label: "3D Mesh",
    requiresData: false,
    forUser: false,
    category: CATEGORY_RADIAL_ETC,
    tooltip: makeDisplayTooltip('3D Mesh','3dmesh.png')
});

addGlobalDisplayType({
    type: DISPLAY_PLOTLY_TEXTCOUNT,
    label: "Text Count",
    category: CATEGORY_TEXT,
    tooltip: makeDisplayTooltip('Text Count','textcount.png',
				'Given a text field show the number of <br>times certain word patterns occur')
});

//Ternary doesn't work
//addGlobalDisplayType({type: DISPLAY_PLOTLY_TERNARY, label:"Ternary",requiresData:true,forUser:true,category:CATEGORY_RADIAL_ETC});
//Treempap doesn't work
//addGlobalDisplayType({type: DISPLAY_PLOTLY_PTREEMAP, label:"Tree Map",requiresData:true,forUser:true,category:CATEGORY_RADIAL_ETC});


var ID_PLOT= "plot";
var ID_PLOTY = "plotly";


function RamaddaPlotlyDisplay(displayManager, id, type, properties) {
    let SUPER = new RamaddaFieldsDisplay(displayManager, id, type, properties);
    //Dynamically load plotly
    let myProps = [
	{label:'Plotly Properties'},
	{p:'plotTitle'},
	{p:'font'},
	{p:'fontSize',d:12},
	{p:'fontColor',d:'#000'}];

    defineDisplay(this, SUPER, myProps, {
//    RamaddaUtil.inherit(this, SUPER);
//    RamaddaUtil.defineMembers(this, {
	getRequirement:function() {
	    return "Plotly";
	},
        needsData: function() {
            return true;
        },
	showFieldsInDialog: function() {
	    return true;
	},
	updateUI:function(args) {
	    if(!window.Plotly) {
		let url = RamaddaUtil.getCdnUrl("/lib/plotly/plotly-2.24.1.min.js");
		let callback = this.loadingJS?null:   ()=>{
//		    Utils.loadScript('/repository/lib/d3/d3.js');
		    this.updateUI(args);
		};
		Utils.loadScript(url,callback); 
		return;
	    }
	    this.updateUIInner(args);
	},
	updateUIInner:function(args) {
	},
        setDimensions: function(layout, widthDelta,ext) {
	    ext = ext??{};
            //                var width  = parseInt(this.getProperty("width","400").replace("px","").replace("%",""));
            var height = parseInt(this.getProperty("height", "400").replace("px", "").replace("%", ""));
            //                layout.width = width-widthDelta;
            layout.height = height;
	    if(!layout.margin) layout.margin={};
	    [["l","marginLeft"],["r","marginRight"],["t","marginTop"],["b","marginBottom"]].map(t=>{
		let key = t[1];
		if(Utils.isDefined(this.getProperty(key)))
		    layout.margin[t[0]]  = this.getProperty(key);
		else if(Utils.isDefined(ext[key])) 
		    layout.margin[t[0]]  = ext[key];
	    });
        },
        pointDataLoaded: function(pointData, url, reload) {
            SUPER.pointDataLoaded.call(this, pointData, url, reload);
	    /*
              if (this.dataCollection)
              this.displayManager.propagateEventRecordSelection(this,
              this.dataCollection.getList()[0], {
              index: 0
              });
	    */

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
	    let html = 
		HtmlUtils.div([ATTR_ID,this.getDomId(ID_HEADER)],"") +
		HtmlUtils.div([ATTR_ID, this.getDomId(ID_PLOT), ATTR_STYLE, "width:100%;"+this.getDisplayStyle()], "") +
		HtmlUtils.div([ATTR_ID,this.getDomId(ID_FOOTER)],"");
	    this.setContents(html);
	    //do the plot creation a bit later so the width of the ID_PLOT div gets set OK
	    setTimeout(()=>{
		let plot = Plotly.newPlot(this.getDomId(ID_PLOT), data, layout,{displayModeBar: false});
		let myPlot = document.getElementById(this.getDomId(ID_PLOT));
		if(myPlot) {
		    this.initPlot(plot, myPlot);
		}
	    },1);
        },
	makeLayout:function(layout) {
            let myLayout = {
                title: this.getPlotTitle(''),
		font: {
		    family: this.getFont(),
		    size: this.getFontSize(),
		    color: this.getFontColor()
		}
	    };
	    if(layout) return $.extend(layout,myLayout);


	    return myLayout;
	},
        handleClickEvent: function(data) {
	    if(data.points && data.points.length>0) {
		let record = data.points[0].record;
		if(!record) {
		    let index = data.points[0].pointIndex;
		    record = this.indexToRecord[index];
		}
		if(record) {
		    this.propagateEventRecordSelection({record: record});
		}
	    }
	},
        initPlot: function(plot, myPlot) {
	    let _this = this;
            myPlot.on('plotly_click', function(data) {
		_this.handleClickEvent(data);
            });

	}
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
        updateUIInner: function() {
            var records = this.filterData();
            if (!records) {
                return;
            }
            var fields = this.getSelectedFields(this.getData().getRecordFields());
	    var numericFields = this.getFieldsByType(fields, "numeric");
            if (numericFields.length == 0) {
                this.displayError("No numeric fields specified");
                return;
            }
	    var theta;
	    var thetaType;
	    if(this.getProperty("useDates")) {
		var tmp = this.getDateValues(records);
		theta=[];
		var dateFormat = this.getProperty("dateFormat", "yyyyMMdd");
		thetaType = "category";
		tmp.map(d=>{
		    theta.push(Utils.formatDateWithFormat(d,dateFormat));
		});
	    } else {
		var thetaField = this.getFieldById(null, this.getProperty("thetaField"));
		if (thetaField) {
		    theta = this.getColumnValues(records, thetaField).values;
		}
	    }
            var values = [];
            var min = Number.MAX_VALUE;
            var max = Number.MIN_VALUE;
            var plotData = [];
            for (var i = 0; i < numericFields.length; i++) {
                var field = numericFields[i];
                var column = this.getColumnValues(records, field);
		if(!theta) {
		    theta = [];
		    var cnt = 0;
		    for(var cnt=0;cnt<column.values.length;cnt++)
			theta.push(cnt*360/column.values.length);
		}
                min = Math.min(min, column.min);
                max = Math.max(max, column.max);
		var values = column.values;
                plotData.push({
                    type: this.getPlotType(),
                    r: values,
                    theta: theta,
                    fill: 'toself',
                    name: field.getLabel(),
                });
            }

            layout = {
                polar: {
                    angularaxis: {
			type:"category"
		    },
                    radialaxis: {
                        visible: true,
                        range: [min, max]
                    }
                },
            }
	    if(thetaType) {
		layout.polar.angularaxis  ={
		    type:thetaType
		};
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
        updateUIInner: function() {
            var records = this.filterData();
            if (!records) return;
            var fields = this.getSelectedFields(this.getData().getRecordFields());
            var numericFields = this.getFieldsByType(fields, "numeric");
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
    let SUPER = new RamaddaPlotlyDisplay(displayManager, id, type, properties);
    let myProps = [
	{label:'3D Plot'},
	{p:'markerSize',d:6},
	{p:'axisLineColor',d:'rgb(255,255,255)'},
	{p:'xaxisBackground',d:'rgb(200, 200, 230)'},
	{p:'yaxisBackground',d:'#ccc'},	
	{p:'zaxisBackground',d:'rgb(230, 230,200)'},
	{p:'chartBackground', d:'rgb(255,255,255,0)'}
    ];
    defineDisplay(this, SUPER, myProps, {
        initPlot: function(plot, myPlot) {
	    SUPER.initPlot.call(this, plot, myPlot);
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
        updateUIInner: function() {
            let records = this.filterData();
            if (!records) return;
            let allFields = this.getSelectedFields(this.getData().getRecordFields());
            let fields = this.getFieldsByType(allFields, "numeric");
            if (fields.length == 0) {
                this.displayError("No numeric fields specified");
                return;
            }
            if (fields.length < 3) {
                this.displayError("Don't have 3 numeric fields specified");
                return;
            }
	    this.records = records;
	    this.xField = fields[0];	    
	    this.yField = fields[1];
	    this.zField = fields[2];	    
            let x = this.getColumnValues(records, this.xField);
            let y = this.getColumnValues(records, this.yField);
            let z = this.getColumnValues(records, this.zField);
	    let marker =  {
                size: +this.getMarkerSize(),
		color:'#fff',
                opacity: 0.8,
                line: {
		    color: 'blue',
                    width: 2
                },
            };

            let colorBy = this.getColorByInfo(records);
	    if(colorBy.isEnabled()) {
		let dataColumn = this.getColumnValues(records, colorBy.getField());
		//Map the values into a standard 0...1 range
		marker.line.color = Utils.normalize(dataColumn.values);		    
		let scale = [];
		let colors = colorBy.getColors();
		colors.forEach((c,idx)=>{
		    scale.push([idx/colors.length,c]);
		});
		scale.push([1,scale[scale.length-1][1]]);
		marker.line.colorscale=scale;
		marker.colorscale=scale;
	    }

            this.trace1 = {
		name:colorBy.isEnabled()?colorBy.getField().getLabel():"",
                x: x.values,
                y: y.values,
                z: z.values,
                mode: 'markers',
		marker:marker,
                type: this.get3DType()
            };

	    let gridColor = this.getAxisLineColor();
            let layout = {
                scene: {
                    xaxis: {
                        backgroundcolor: this.getXaxisBackground(),
                        gridcolor: gridColor,
                        showbackground: true,
                        zerolinecolor: gridColor,
                        title: this.xField.getLabel(),
                    },
                    yaxis: {
                        backgroundcolor: this.getYaxisBackground(),
                        gridcolor: gridColor,
                        showbackground: true,
                        zerolinecolor: gridColor,
                        title: this.yField.getLabel(),
                    },
                    zaxis: {
                        backgroundcolor: this.getZaxisBackground(),
                        gridcolor: gridColor,
                        showbackground: true,
                        zerolinecolor: gridColor,
                        title: this.zField.getLabel(),
                    }
                },
                margin: {
                    l: 0,
                    r: 0,
                    b:0,
                    t:0,
                    pad: 4
                },
		legend: {
		    yanchor:"top", y:1,   xanchor: 'center', x: 0.5, orientation: 'h' 
		},
		paper_bgcolor: this.getChartBackground()
            };
            this.setDimensions(layout, 2);
	    layout.showLegend=false;
            let trace2 = {
		name:'Selected',
		showLegend:false,
                mode: 'markers',
                type: this.get3DType(),
                x: [],
                y: [],
                z: [],
		marker:{
                    size: this.getMarkerSize()+6,
		    color:'red',
                    opacity: 0.8,
                    line: {
			color: 'red',
			width: 2
                    },
		},
            };


            let plotData = [this.trace1,trace2];
	    this.makePlot(plotData, layout);
	    if(colorBy.isEnabled()) {
		colorBy.displayColorTable();
	    }

        },
        initPlot: function(plot, myPlot) {
	    SUPER.initPlot.call(this, plot, myPlot);
	    myPlot.on('plotly_click', (data)=>{
		if(data.points && data.points.length) {
		    let record = this.records[data.points[0].pointNumber];
		    if(record) {
			this.propagateEventRecordSelection({record: record});
		    }
		}
	    });
	},
        handleEventRecordSelection: function(source, args) {
	    SUPER.handleEventRecordSelection.call(this, source, args);
	    let record = args.record;
	    if(!record) return;
            let x = this.xField.getValue(record);
            let y = this.yField.getValue(record);
            let z = this.zField.getValue(record);	    	    
	    let update = {'x': [[x]], 'y': [[y]],'z':[[z]]};
	    Plotly.update(this.domId(ID_PLOT), update, {}, [1]);
	}
    });
}


function Ramadda3dmeshDisplay(displayManager, id, properties) {
    let SUPER;
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



function RamaddaSunburstDisplay(displayManager, id, properties) {
    $.extend(this, {
        width: "500",
        height: "500",
    });
    let SUPER = new RamaddaPlotlyDisplay(displayManager, id, DISPLAY_PLOTLY_SUNBURST, properties);
    let myProps = [
	{label:'Sunburst Display'},
	{p:'parentField',ex:''},
	{p:'labelField',ex:''},
	{p:'idField',ex:''},
	{p:'valueField',ex:''},
	{p:'nodeFields',ex:''},
	{p:'treeRoot',ex:'some label'},
	{p:'doTopColors',ex:'true'},
    ];

    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
        getDisplayStyle: function() {
            return "";
        },
        updateUIInner: function() {
            var records = this.filterData();
            if (!records) return;
            var parentField = this.getFieldById(null, this.getProperty("parentField"));
	    var valueField = this.getFieldById(null, this.getProperty("valueField"));
	    var labelField = this.getFieldById(null, this.getProperty("labelField"));
	    let roots=null;
	    try {
		roots = this.makeTree();
	    } catch(error) {
                this.setContents(this.getMessage(error.toString()));
		return;
	    }

	    let ids = [];
	    let labels = [];
	    let parentNodes= [];

	    let parents = [];
	    let values=[];
	    //descend and calculate values
	    let calcValue = function(node) {
		if(node.children.length==0) {
		    if(node.record) {
			var value = node.record.getValue(valueField.getIndex());
			node.value = value;
			return value;
		    }
		    return 0;
		}
		let sum = 0;
		node.children.map(child=>{
		    sum += calcValue(child);
		});
		node.value = sum;
		if(node.record){
		    node.record.setValue(valueField.getIndex(),sum);
		}
		return sum;
	    }
	    if(valueField) {
		roots.map(calcValue);
	    }
	    this.myRecords = [];
	    let recordList =  this.myRecords;
	    let makeList = function(node) {
		recordList.push(node.record);
		if(valueField)
		    values.push(node.value);
		parentNodes.push(node.parent);
		ids.push(node.id);
		labels.push(node.label);
		parents.push(node.parent==null?"":node.parent.id);
		node.children.map(makeList);
	    }
	    roots.map(makeList);
            var colors = this.getColorTable(true);
	    let doTopColors= this.getProperty("doTopColors",true);
	    if(!colors) {
		var colorMap = Utils.parseMap(this.getProperty("colorMap"));
		if(colorMap) {
		    colors = [];
		    let dfltIdx =0;
		    let dflt = ["#1f77b4", "#ff7f0e", "#2ca02c", "#d62728", "#9467bd", "#8c564b", "#e377c2", "#7f7f7f", "#bcbd22", "#17becf"];
		    ids.map((id,idx)=>{
			if(doTopColors && parentNodes[idx]!=null)  return;
			let color = colorMap[id];
			if(!color) {
			    color = colorMap[labels[idx]];
			}
			if(!color) {
			    if(dfltIdx>=dflt.length) dfltIdx = 0;
			    color = dflt[dfltIdx];
			    dfltIdx++;
			}
			colors.push(color);
		    });
		}
	    }

	    var data = [{
		type: "sunburst",
		ids:ids,
		labels: labels,
		parents: parents,
		outsidetextfont: {size: 20, color: "#377eb8"},
		leaf: {opacity: 0.4},
		marker: {
		    line: {width: 1}
		},
		branchvalues: 'total'
	    }];
	    if(valueField) {
		data[0].values = values;
	    }
	    var layout = {
		margin: {l: 0, r: 0, b: 0, t: 0},
		width: +this.getProperty("width"),
		height: +this.getProperty("height"),
	    };
	    if(colors) {
		if(!doTopColors) {
		    data[0].marker.colors = colors;
		} else {
		    layout.sunburstcolorway= colors;
		    layout.extendsunburstcolors= true;
		    layout.extendsunburstcolorway= true;
		}
	    }

	    this.makePlot(data, layout);
        },
        initPlot: function(plot, myPlot) {
	    SUPER.initPlot.call(this, plot, myPlot);
	    myPlot.on('plotly_sunburstclick', d=>{this.handleSunburstClickEvent(d)});
	},
        handleSunburstClickEvent: function(data) {
	    if(!data.points || data.points.length<=0) {
		return;
	    }
	    var pointNumber = data.points[0].pointNumber;
	    var record = this.myRecords[pointNumber];
	    //	    console.log(pointNumber +" " + record);
	    if(record) {
		this.propagateEventRecordSelection({record: record});
	    }
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
        updateUIInner: function() {
            var records = this.filterData();
            if (!records) return;
            var fields = this.getSelectedFields(this.getData().getRecordFields());
            var stringField = this.getFieldByType(fields, "string");
            var fields = this.getFieldsByType(fields, "numeric");
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
    let myProps = [
	{label:'Dotplot Display'},
	{p:'fields',ex:''},
	{p:'labelField',ex:''},
	{p:'lineColor',d:'rgba(156, 165, 196, 1.0)'},
	{p:'symbol',d:'circle',ex:'circle|circle-open|circle-dot|circle-open-dot|square|square-open|square-dot|square-open-dot|diamond|diamond-open|diamond-dot|diamond-open-dot|cross|cross-open|cross-dot|cross-open-dot|x|x-open|x-dot|x-open-dot|triangle-up|triangle-up-open|triangle-up-dot|triangle-up-open-dot|triangle-down|triangle-down-open|triangle-down-dot|triangle-down-open-dot|triangle-left|triangle-left-open|triangle-left-dot|triangle-left-open-dot|triangle-right|triangle-right-open|triangle-right-dot|triangle-right-open-dot|triangle-ne|triangle-ne-open|triangle-ne-dot|triangle-ne-open-dot|triangle-se|triangle-se-open|triangle-se-dot|triangle-se-open-dot|triangle-sw|triangle-sw-open|triangle-sw-dot|triangle-sw-open-dot|triangle-nw|triangle-nw-open|triangle-nw-dot|triangle-nw-open-dot|pentagon|pentagon-open|pentagon-dot|pentagon-open-dot|hexagon|hexagon-open|hexagon-dot|hexagon-open-dot|hexagon2|hexagon2-open|hexagon2-dot|hexagon2-open-dot|octagon|octagon-open|octagon-dot|octagon-open-dot|star|star-open|star-dot|star-open-dot|hexagram|hexagram-open|hexagram-dot|hexagram-open-dot|star-triangle-up|star-triangle-up-open|star-triangle-up-dot|star-triangle-up-open-dot|star-triangle-down|star-triangle-down-open|star-triangle-down-dot|star-triangle-down-open-dot|star-square|star-square-open|star-square-dot|star-square-open-dot|star-diamond|star-diamond-open|star-diamond-dot|star-diamond-open-dot|diamond-tall|diamond-tall-open|diamond-tall-dot|diamond-tall-open-dot|diamond-wide|diamond-wide-open|diamond-wide-dot|diamond-wide-open-dot|hourglass|hourglass-open|bowtie|bowtie-open|circle-cross|circle-cross-open|circle-x|circle-x-open|square-cross|square-cross-open|square-x|square-x-open|diamond-cross|diamond-cross-open|diamond-x|diamond-x-open|cross-thin|cross-thin-open|x-thin|x-thin-open|asterisk|asterisk-open|hash|hash-open|hash-dot|hash-open-dot|y-up|y-up-open|y-down|y-down-open|y-left|y-left-open|y-right|y-right-open|line-ew|line-ew-open|line-ns|line-ns-open|line-ne|line-ne-open|line-nw|line-nw-open|arrow-up|arrow-up-open|arrow-down|arrow-down-open|arrow-left|arrow-left-open|arrow-right|arrow-right-open|arrow-bar-up|arrow-bar-up-open|arrow-bar-down|arrow-bar-down-open|arrow-bar-left|arrow-bar-left-open|arrow-bar-right|arrow-bar-right-open|arrow|arrow-open|arrow-wide|arrow-wide-open'},
	{p:'dotSize',d:16},
	{p:'&lt;field&gt;.dotSize',ex:16},	
        {p:"yAxisTitle"},
	{p:"yAxisType",ex:'log'},
	{p:"yAxisShowLine",d:true},
	{p:"yAxisShowGrid",d:false},
	{p:"xAxisTitle"},
	{p:"xAxisType",ex:'log'},
	{p:"xAxisShowGrid",d:false},
	{p:"xAxisShowLine",d:true},
	{p:"marginLeft",d:140},
	{p:"marginRight",d:40},
	{p:"marginBottom",d:50},
	{p:"marginTop",d:20},
	{p:"chartFill"},
	{p:"chartAreaFill"},
	
    ];

    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
        getDisplayStyle: function() {
            return "";
        },

        updateUIInner: function() {
//https://codepen.io/etpinard/pen/LLZGZV
	    /*
	    let sym = Plotly.PlotSchema.get().traces.scatter.attributes.marker.symbol.values.filter(s => typeof s === 'string' && !s.match(/^[0-9]+$/))
	    let list='';
	    sym.forEach(s=>{list+=s+','});
	    console.log(list);
	    */



            let records = this.filterData();
            if (!records) return;
            var pointData = this.getData();
            if (pointData == null) return;
            let allFields = pointData.getRecordFields();
            let stringField = this.getFieldById(allFields,this.getLabelField());
            if (!stringField) {
		stringField = this.getFieldByType(allFields, "string");
	    }

            if (!stringField) {
                stringField = allFields[0];
            }

	    let fields   = this.getFieldsByIds(null, this.getPropertyFields("",true));
            if (fields.length == 0) {
		fields = this.getFieldsByType(allFields, "numeric");
	    }
            if (fields.length == 0) {
		fields = this.getFieldsByType(allFields, "date");
	    }
            if (fields.length == 0) {
                this.displayError("No numeric fields specified");
                return;
            }

            let labels = null;
            let labelName = "";
            if (stringField) {
                labels = this.getColumnValues(records, stringField).values;
                labelName = stringField.getLabel();
            }
            var colors = this.getColorTable(true);
            if (!colors)
                colors = ['rgba(156, 165, 196, 0.95)', 'rgba(204,204,204,0.95)', 'rgba(255,255,255,0.85)', 'rgba(150,150,150,0.95)']
            var plotData = [];
            var colorBy = this.getColorByInfo(records);
	    var  didColorBy = false;
            for (i in fields) {
                let field = fields[i];
                let size = this.getDotSize(this.getProperty(field.getId()+'.dotSize'));
                let symbol = this.getSymbol(this.getProperty(field.getId()+'.symbol'));
                let lineColor = this.getLineColor(this.getProperty(field.getId()+'.lineColor'));
                let color = this.getProperty(field.getId()+'.color');
		if(!color)color= i >= colors.length ? colors[0] : colors[i];
                let values = this.getColumnValues(records, field).values;
                if (colorBy.index >= 0) {
		    color = [];
		    records.map(record=>{
			var value = record.getData()[colorBy.index];
			didColorBy = true;
			color.push(colorBy.getColor(value, record));
                    })
		}
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
                            color: lineColor,
                            width: 1,
                        },
                        symbol: symbol,
                        size: size
                    }
                });
            }
	    

            let layout = this.makeLayout({
                yaxis: {
                    title: this.getYAxisTitle(labelName),
		    type:this.getYAxisType(),
                    showline: this.getYAxisShowLine(),
                    showgrid: this.getYAxisShowGrid(),
                    tickfont: {
                        font: {
                            color: 'rgb(102, 102, 102)',
                        }
                    },
                },
                xaxis: {
                    title: this.getXAxisTitle(fields[i].getLabel()),
		    type:this.getXAxisType(),
                    showgrid: this.getXAxisShowGrid(false),
                    showline: this.getXAxisShowLine(true),
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
                    l: this.getMarginLeft(),
                    r: this.getMarginRight(40),
                    b: this.getMarginBottom(),
                    t: this.getMarginTop(),
                },
                legend: {
                    font: {
                        size: 10,
                    },
                    yanchor: 'middle',
                    xanchor: 'right'
                },
                paper_bgcolor: this.getChartFill(this.getProperty("chart.fill",'rgb(254, 247, 234)')),
                plot_bgcolor: this.getChartAreaFill(this.getProperty("chartArea.fill", 'rgb(254, 247, 234)')),
                hovermode: 'closest'
            });
            this.setDimensions(layout, 2);
            this.makePlot(plotData, layout);
	    if(didColorBy) {
		colorBy.displayColorTable();
	    }

        },
    });
}


function RamaddaProfileDisplay(displayManager, id, properties) {
//    if(!properties.width) properties.width="400px";
    let SUPER = new RamaddaPlotlyDisplay(displayManager, id, DISPLAY_PLOTLY_PROFILE, properties);
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);
    this.defineProperties([
	{label:'Profile Properties'},
	{p:'indexField',d:null,ex:''},
	{p:'fields',d:null,ex:''},
	{p:'profileMode',d:'lines',ex:'lines|markers|lines+markers'},
	{p:'yAxisTitle',d:'Pressure- Digiquartz',ex:''},
	{p:'yAxisShowLine',d:'true',ex:'false'},
	{p:'yAxisShowGrid',d:'true',ex:'false'},
	{p:'xAxisTitle',d:'',ex:''},
	{p:'xAxisShowGrid',d:'true',ex:'false'},
	{p:'xAxisShowLine',d:'true',ex:'false'},
	{p:'yAxisReverse',d:false,ex:'true'},
	{p:'setRangeFromAllValues',ex:true},
	{p:'marginLeft',d:'60',ex:'60'},
	{p:'marginRight',d:'100',ex:'100'},
	{p:'marginBottom',d:'50',ex:'50'},
	{p:'marginTop',d:'100',ex:'100'},
	{p:'showLegend',d:'true',ex:'false'},
	{p:'legendYAnchor',d:null,ex:'top|middle|bottom'},
	{p:'legendXAnchor',d:null,ex:'right|center|left'},
	{p:'chart.fill',d:'rgb(254, 247, 234)',ex:'color'},
	{p:'chartArea.fill',d:'rgb(254, 247, 234)',ex:'color'},
	{p:'xAxis2Title',d:'Conductivity',ex:''},
	{p:'lineColor',d:'blue'},
	{p:'lineWidth',d:1},
	{p:'markerLineColor',d:'rgba(0,0,0,0.2)'},
	{p:'markerLineWidth',d:1},
	{p:'markerSize',d:16},
	{p:'symbol',d:'circle',ex:'circle|square|diamond|cross|x|triangle-up|triangle-down|pentagon|hexagon|hexagram|star|hash'},
	{p:'yAxisReverse',ex:true},
        {p:'yAxisTitle'},
	{p:'yAxisShowLine', ex:true},
	{p:'yAxisShowGrid', ex:true},
        {p:'xAxisTitle'},
        {p:'xAxisShowGrid', ex:true},
        {p:'xAxisShowLine', ex:true},
    ]);

    RamaddaUtil.defineMembers(this, {
        getDisplayStyle: function() {
            return "";
        },
	showFieldsInDialog: function() {
	    return true;
	},
        updateUIInner: function() {
            let records = this.filterData();
            if (!records) return;
//	    this.writePropertyDef = "";
	    let indexField = this.getFieldById(null,this.getIndexField());
	    if(indexField==null) {
                this.setContents(this.getMessage("No indexField specified"));
		return;
	    }
            let fields = this.getSelectedFields(this.getData().getRecordFields());


            if (fields.length == 0) {
		let tmp = this.getFieldsByType(null, "numeric");
		if(tmp.length>0) fields.push(tmp[0]);
	    }

            if (fields.length == 0) {
                this.setContents(this.getMessage("No fields found"));
		return;
	    }
            let index = this.getColumnValues(records, indexField).values;
            let data = [];
	    let range;
	    if(this.getSetRangeFromAllValues()) {
		let allRecords = this.getData().getRecords();
		let min,max;
		allRecords.forEach((r,idx)=>{
		    let v = fields[0].getValue(r);
		    if(idx==0 || v<min) min=v;
		    if(idx==0 || v>max) max=v;		    
		});
		range=[min,max]
	    }

  
	    let minX=NaN;
	    let maxX= NaN;
            fields.forEach((field,idx)=>{
		let values= this.getColumnValues(records, field);
		minX = Utils.min(values.min,minX)
		maxX = Utils.max(values.max,maxX);	
		let x = values.values;
		if(fields.length==1) {
		    let oldIndex = this.indexToRecord;
		    this.indexToRecord={};
		    let nindex=[];
		    let nx=[];
		    x.forEach((v,idx)=>{
			if(!isNaN(v)) {
			    this.indexToRecord[nindex.length] = oldIndex[idx];
			    nindex.push(index[idx]);
			    nx.push(v);
			}
		    });
		    index=nindex;
		    x =nx;
		}

		let colors=null;
		let colorTable = this.getProperty(field.getId()+'.colorTable',  this.getProperty('colorTable'));
		if(colorTable) {
                    let ct = Utils.ColorTables[colorTable];
		    if(ct) {
			let colorByInfo = new  ColorByInfo(this, fields, records, null,null,ct.colors, null, field);
			colors =[];
			records.forEach(r=>{
			    let c = colorByInfo.getColorFromRecord(r);
			    colors.push(c);
			});
		    }			
		}

		let trace =   {
		    y: index,
		    x: x,
		    type: 'scatter',
		    mode: this.getProfileMode(),
                    name: field.getUnitLabel(),
		    line: {
                        color: this.getProperty("lineColor"+(idx+1)),
                        width: this.getLineWidth(),
		    },
                    marker: {
			color:colors,
                        line: {
                            color: this.getMarkerLineColor(),
                            width: this.getMarkerLineWidth(),
                        },
                        symbol: this.getProperty('symbol'+ (idx+1),this.getSymbol()),
                        size: this.getMarkerSize()
                    }
		};
		if(idx>0)
		    trace.xaxis="x2";
		data.push(trace);
	    });


	    let labelName = indexField.getLabel();
            let layout = {
                yaxis: {
		    autorange: this.getYAxisReverse()?"reversed":null,
                    title: this.getYAxisTitle(labelName),
                    showline: this.getYAxisShowLine(true),
                    showgrid: this.getYAxisShowGrid(true),
                },
                xaxis: {
		    range: range,
                    title: this.getXAxisTitle(fields[0].getUnitLabel()),
                    showgrid: this.getXAxisShowGrid(true),
                    showline: this.getXAxisShowLine(true),
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
                    l: +this.getMarginLeft(60),
                    r: +this.getMarginRight(100),
                    b: +this.getMarginBottom(50),
                    t: +this.getMarginTop(100),
                },
                legend: {
                    font: {
                        size: 10,
                    },
                    yanchor: this.getLegendYAnchor(),
                    xanchor: this.getLegendXAnchor(),
                },
                showlegend: this.getShowLegend(true),
		paper_bgcolor: this.getProperty("chart.fill", 'transparent'),		
//                plot_bgcolor: this.getProperty("chartArea.fill", 'rgb(254, 247, 234)'),
                plot_bgcolor: this.getProperty("chartArea.fill", '#fff'),				
                hovermode: 'closest'
            };
	    if(fields.length>1) {
                layout.xaxis2 =  {
		    overlaying: 'x', 
		    side: 'top',
                    title: this.getProperty("xAxis2Title", fields[1].getLabel()),
                    showgrid: this.getProperty("xAxisShowGrid", true),
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
                };
	    }

	    if(this.annotations) {
		let shapes= layout.shapes = [];
		this.annotations.forEach(v=>{
		    shapes.push({
			type: 'line',
			x0: minX,
			x1: maxX,      
			y0: v,      
			y1: v,     
			line: {
			    color: 'red',
			    width: 2,
			    dash: 'solid', 
			}
		    });
		});
	    }

            this.setDimensions(layout, 2);
            this.makePlot(data, layout);
	    if(this.writePropertyDef)
		console.log(this.writePropertyDef);
	    this.writePropertyDef=null;
        },
	addAnnotation: function (v) {
	    this.annotations = [v];
	    this.updateUI(true);
	},
	clearAnnotations: function () {
	    this.annotations=null;
	    this.updateUI(true);
	}	
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
        updateUIInner: function() {
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

            var stringField = this.getFieldByType(fields, "string");
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
        updateUIInner: function() {
            var records = this.filterData();
            if (!records) return;
            var selectedFields = this.getSelectedFields(this.getData().getRecordFields());
            var field = this.getFieldByType(selectedFields, "numeric");
            if (!field) {
                this.displayError("No numeric field specified");
                return;
            }
            var values = this.getColumnValues(records, field).values;
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
function TextcountDisplay(displayManager, id, properties) {
    let SUPER  =  new RamaddaPlotlyDisplay(displayManager, id, DISPLAY_PLOTLY_TEXTCOUNT, properties);
    let myProps = [
	{label:'Text Count Display'},
	{p:'patterns',ex:'foo,bar'},
	{p:'labels',ex:'Foo,Bar'},
	{p:'textField',ex:''},
    ];
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
        getDialogContents: function(tabTitles, tabContents) {
	    let html = HtmlUtils.div([ATTR_ID,this.getDomId("dialog_set_pattern")],"Change patterns") + "<br>" +
		HtmlUtils.textarea("",Utils.join(this.patternList||[],"\n"),[ATTR_ID, this.getDomId("dialog_patterns"),"rows","10"]);

	    
            tabTitles.push("Patterns");
            tabContents.push(html);
            SUPER.getDisplayDialogContents.call(this, tabTitles, tabContents);
        },
        initDialog: function() {
            SUPER.initDialog.call(this);
	    let _this = this;
            this.jq("dialog_set_pattern").button().click(function() {
		_this.patterns = _this.jq("dialog_patterns").val().trim().replace(/\n/g,",");
		_this.updateUI();
            });

        },
        updateUIInner: function() {
            let records = this.filterData();
            if (!records) return;
	    let patterns = this.getProperty("patterns");
	    if(patterns == null) {
		this.setContents(this.getMessage("No patterns specified"));
		return;
	    }
	    this.patternList = patterns.split(",");
	    let labels = this.getProperty("labels");
	    if(labels) {
		labels = labels.split(",");
	    }

	    this.textField = this.getFieldById(null, this.getProperty("textField"));
	    if(!this.textField) {
		this.textField = this.getFieldByType(null, "string");
	    }
	    if(!this.textField) {
		this.setContents(this.getMessage("No text field in data"));
		return;
	    }

	    let count = [];
	    let matchers = [];
	    this.patternList.map(p=>{
		count.push(0);
		matchers.push(new TextMatcher(p));
	    });

            for (var rowIdx = 0; rowIdx < records.length; rowIdx++) {
                var record = records[rowIdx];
                var row = record.getData();
		var value = record.getValue(this.textField.getIndex());
		matchers.map((m,index)=>{
		    if(m.matches(value)) {
			count[index]++;
		    }
		});
	    }

	    let data = [{
		type: 'bar',
		x: count,
		y: labels?labels:this.patternList,
		orientation: 'h',
	    }];
	    let layout = {
		margin: {
		    l: 100,
		    r: 50,
		    b: 50,
		    t: 10,
		    padding:4
		},
	    };
	    if(Utils.isDefined(this.properties.height)) {
		layout.height = +this.properties.height;
	    }

            this.makePlot(data, layout);
        },
        handleClickEvent: function(data) {
	    if(!data.points || data.points.length<=0) {
		return;
	    }
	    let pointNumber = data.points[0].pointNumber;
	    let pattern = this.patternList[pointNumber];
	    let args = {
		fieldId: this.textField.getId(),
		value: pattern
	    };

	    this.propagateEvent(DisplayEvent.filterChanged, args);
	},

    });
}



function CombochartDisplay(displayManager, id, properties) {
    let SUPER  =  new RamaddaPlotlyDisplay(displayManager, id, DISPLAY_PLOTLY_COMBOCHART, properties);
    let myProps = [
	{label:'Combo Chart'},
	{p:'fields',ex:''},
 	{p:'&lt;field&gt;.axisSide',ex:'right|left'},
	{p:'&lt;field&gt;.axisTitle',ex:''},
	{p:'&lt;field&gt;.chartType',ex:'scatter|bar'},
	{p:'chartType',ex:'scatter|bar'},
	{p:'&lt;field&gt;.chartColor',ex:''},
	{p:'chartType',ex:''},
	{p:'xAxisTitle',ex:''},
	{p:'xAxisShowGrid',ex:''},
	{p:'xAxisShowLine',ex:''},
	{p:'legendBackground',ex:''},
	{p:'legendBorder',ex:''},
	{p:'chartBackground',ex:'#ccc'},
	{p:'plotBackground',ex:''},
	{p:'marginLeft',ex:''},
	{p:'marginRight',ex:''},
	{p:'marginBottom',ex:''},
	{p:'marginTop',ex:''},
	{p:'chartPad',ex:''},
    ];	
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
	updateUIInner: function() {
            let records = this.filterData();
            if (!records) return;
	    var layout = {
                xaxis: {
                    title: this.getProperty("xAxisTitle", "Time"),
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
                    tickcolor: 'rgb(102, 102, 102)',
		  
		},
                legend: {
                    font: {
                        size: 10,
                    },
		    bgcolor: this.getProperty("legendBackground",'rgba(255,255,255,0)'),
		    bordercolor: this.getProperty("legendBorder",'rgba(255,255,255,0)'),
		    x: 0,
		    y: 1.0,

                },
		margin: {
                    l: this.getProperty("marginLeft", 50),
                    r: this.getProperty("marginRight", 50),
                    b: this.getProperty("marginBottom", 50),
                    t: this.getProperty("marginTop", 0),
		    pad: this.getProperty("chartPad", 4),
		},
		paper_bgcolor: this.getProperty("chartBackground", 'rgb(255,255,255,0)'),
		plot_bgcolor: this.getProperty("plottBackground", 'rgb(255,255,255,0)'),
	    };
	    let fields   = this.getFieldsByIds(null, this.getPropertyFields("",true));
	    var data = [];
	    var domain = [];
	    records.map((r,idx)=>{
		domain.push(r.getTime());
	    });
	    let right =  true;
	    fields.map((field,idx)=>{
		let values = this.getColumnValues(records, field).values;
		var trace = {
		    x: domain,
		    y: values,
		    name: field.getLabel(),
		    type: this.getProperty(field.getId()+".chartType",this.getProperty("chartType",'scatter')),
		    marker: {color: this.getProperty(field.getId()+".chartColor",this.getProperty("chartColor"))},
		};
		if(idx>0) {
		    trace.yaxis = "y" + (idx+1);
		}
		data.push(trace);
		var yAxisId = idx>0?"yaxis"+(idx+1):"yaxis";
		var axis = {
		    title: this.getProperty(field.getId()+".axisTitle" ,field.getLabel()), 
		    titlefont: {color: 'rgb(0,0,0)'},
		    tickfont: {color: 'rgb(0,0,0,)'},
		    side: this.getProperty(field.getId()+".axisSide",
					   this.getProperty("axisSide", right?'right':'left'))
		};
		if(idx>0)
		    axis.overlaying='y';
		layout[yAxisId] = axis;
		right = !right;
	    });
            this.setDimensions(layout);
            this.makePlot(data, layout);
	}
    });
}





function RamaddaParcoordsDisplay(displayManager, id, properties) {
    let SUPER = new RamaddaPlotlyDisplay(displayManager, id, DISPLAY_PLOTLY_PARCOORDS, properties);
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
        updateUIInner: function() {
            var records = this.filterData();
            if (!records) return;
	    let fields   = this.getFieldsByIds(null, this.getPropertyFields(""));
            if (fields.length == 0) {
                this.displayError("No fields specified");
                return;
            }
	    let dimensions =[];
	    let maxLabelLength = this.getProperty("maxLabelLength",200/fields.length);
	    fields.map(f=>{
		let col = this.getColumnValues(records, f)
		let values = col.values;
		let ticktext = null;
		let tickvals = null;
		if(f.isString()) {
		    let tmpValues = [];
		    let seen = {};
		    ticktext =[];
		    tickvals =[];
		    let cnt = 1;
		    values.map(v=>{
			if(!seen[v]) {
			    seen[v] = cnt++;
			    ticktext.push(v);
			    tickvals.push(seen[v]);
			}
			tmpValues.push(seen[v]);
		    });
		    values = tmpValues;
		}

		let label = this.getProperty(f.getId()+".label",f.getLabel());
		if(label.length>maxLabelLength)
		    label = label.substring(0,maxLabelLength-1)+"...";
		let dim  = {
		    label:label,
		    values:values
		};
		if(this.getProperty(f.getId()+".constraintrange")) {
		    dim.constraintrange = this.getProperty(f.getId()+".constraintrange").split(",");
		}
		if(this.getProperty(f.getId()+".tickvals")) {
		    dim.tickvals = this.getProperty(f.getId()+".tickvals").split(",");
		}
		if(this.getProperty(f.getId()+".ticktext")) {
		    dim.ticktext = this.getProperty(f.getId()+".ticktext").split(",");
		} else {
		    dim.ticktext = ticktext;
		    dim.tickvals = tickvals;		    
		}
		dimensions.push(dim);
	    });

	    let color = this.getProperty("color", 'blue');
	    let colorByField = this.getFieldById(null, this.getProperty("colorBy"));
	    let line = {};
	    let ct = null;
	    let ctMin=0,ctMax=0;
            if (colorByField) {
		let colorValues =   this.getColumnValues(records, colorByField);
		ctMin = colorValues.min;
		ctMax = colorValues.max;
		line.color  = colorValues.values;
		ct = this.getColorTable(true,null,null);
		if(ct) {
		    let colors = [];
		    let step   = 1/(ct.length-1);
		    ct.map((c,idx)=>{
			let v = idx*step;
			colors.push([v,c]);
		    });
		    line.colorscale = colors;
		}
	    }

	    var trace = {
		type: 'parcoords',
		line: line,
		dimensions:dimensions,
	    };

	    var data = [trace]	    
	    let layout  = {
		margin: {
		    l:175,
		    t:50,
		    b:25
		}
	    };
	    this.setDimensions(layout, 2);
	    
            this.makePlot(data, layout);
	    if(ct)
		this.displayColorTable(ct, ID_COLORTABLE,ctMin,ctMax);

        },
    });
}

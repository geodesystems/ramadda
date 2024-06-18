/**
   Copyright 2008-2024 Geode Systems LLC
*/


const DISPLAY_LINECHART = "linechart";
const DISPLAY_AREACHART = "areachart";
const DISPLAY_BARCHART = "barchart";
const DISPLAY_BARTABLE = "bartable";
const DISPLAY_BARSTACK = "barstack";
const DISPLAY_PIECHART = "piechart";
const DISPLAY_TIMERANGECHART = "timerangechart";
const DISPLAY_SANKEY = "sankey";
const DISPLAY_CALENDAR = "calendar";
const DISPLAY_SCATTERPLOT = "scatterplot";
const DISPLAY_HISTOGRAM = "histogram";
const DISPLAY_BUBBLE = "bubble";
const DISPLAY_GAUGE = "gauge";
const DISPLAY_TABLE = "table";
const DISPLAY_WORDTREE = "wordtree";
const DISPLAY_TREEMAP = "treemap";
const DISPLAY_ORGCHART = "orgchart";
const ID_CHART = "chart";
const ID_CHARTS = "charts";
const ID_CHARTS_INNER = "chartsinner";


var ramaddaChartInfo = {
    debug:false,
    version:"51",
    loading:false,
    chartsHaveLoaded:false,
    pending:[],
    packages:{}
};
function haveGoogleChartsLoaded(callback) {
    //    console.log("haveGoogleChartsLoaded");
    if(ramaddaChartInfo.chartsHaveLoaded) {
	if(callback) callback();
	return true;
    }
    if(callback) {
	ramaddaChartInfo.pending.push(callback);
    }
    if(!window["google"]) {
	if(ramaddaChartInfo.debug)
	    console.log("\tno google");
	if(ramaddaChartInfo.loading) {
	    return false;
	}
	ramaddaChartInfo.loading = true;
	Utils.loadScript('https://www.gstatic.com/charts/loader.js',()=>{
	    if(ramaddaChartInfo.debug)
		console.log("loader.js loaded calling google.charts.load version:" + ramaddaChartInfo.version);
	    google.charts.load(ramaddaChartInfo.version, {
		packages: ['corechart']
	    });
	    //	    console.log("calling setOnLoadCallback");
	    google.charts.setOnLoadCallback(() =>{
		if(ramaddaChartInfo.debug)
		    console.log("core chart loaded");
		ramaddaChartInfo.chartsHaveLoaded = true;
		ramaddaChartInfo.pending.forEach(callback=>{
		    if(ramaddaChartInfo.debug)
			console.log("\tcalling callback");
		    callback();
		});
		ramaddaChartInfo.pendingDisplays = [];
	    });
	});
    }
}


function ramaddaLoadGoogleChart(what, callback) {
    let package = ramaddaChartInfo.packages[what];
    if(!package)
	package = ramaddaChartInfo.packages[what] ={
	    loading:false,
	    loaded:false,
	    pending:[]
	};
    if(!package.loaded) {
	if(callback)
	    package.pending.push(callback);
	if (!package.loading) {
	    package.loading = true;
	    if(ramaddaChartInfo.debug)
		console.log("calling google.charts.load:" + what);
	    google.charts.load(ramaddaChartInfo.version, {
		packages: [what],
		callback:()=>{
		    if(ramaddaChartInfo.debug)
			console.log("google.charts.load callback:" + what +" pending:" + package.pending.length);
		    package.loaded = true;
		    package.pending.forEach(callback=>callback());
		}
	    });
	}
	return false;
    }
    return true;
}



addGlobalDisplayType({
    type: DISPLAY_TABLE,
    label: "Table",
    category: CATEGORY_TABLE,
    desc:"Basic tabular display",
    preview: "table.png"}, true);
addGlobalDisplayType({
    type: DISPLAY_LINECHART,
    label: "Line Chart",
    category: CATEGORY_CHARTS,
    preview:"linechart.png",
    desc:"Show time series or other data",
    helpurl:true
});
addGlobalDisplayType({
    type: DISPLAY_BARCHART,
    label: "Bar Chart",
    category: CATEGORY_CHARTS,
    preview:'barchart.png'
});
addGlobalDisplayType({
    type: DISPLAY_BARSTACK,
    label: "Stacked Bar Chart",
    category: CATEGORY_CHARTS,
    preview:'barstack.png',
    helpurl:true    
});
addGlobalDisplayType({
    type: DISPLAY_AREACHART,
    label: "Area Chart",
    category: CATEGORY_CHARTS,
    preview:'areachart.png',
});

addGlobalDisplayType({
    type: DISPLAY_BARTABLE,
    label: "Bar Table",
    category: CATEGORY_CHARTS,
    preview:'bartable.png'
});
addGlobalDisplayType({
    type: DISPLAY_SCATTERPLOT,
    label: "Scatter Plot",
    category: CATEGORY_CHARTS,
    preview: 'scatterplot.png'
});
addGlobalDisplayType({
    type: DISPLAY_HISTOGRAM,
    label: "Histogram",
    category: CATEGORY_CHARTS,
    preview: 'histogram.png'
});
addGlobalDisplayType({
    type: DISPLAY_BUBBLE,
    label: "Bubble Chart",
    category: CATEGORY_CHARTS,
    preview: 'bubblechart.png'
});
addGlobalDisplayType({
    type: DISPLAY_PIECHART,
    label: "Pie Chart",
    category: CATEGORY_CHARTS,
    preview: 'piechart.png'                    
});

addGlobalDisplayType({
    type: DISPLAY_GAUGE,
    label: "Gauge",
    requiresData: true,
    forUser: true,
    category: CATEGORY_MISC,
    preview: "gauge.png"
});
addGlobalDisplayType({
    type: DISPLAY_TIMERANGECHART,
    label: "Time Range",
    requiresData: true,
    forUser: true,
    category: CATEGORY_MISC,
    preview: 'timerange.png',
    desc:'Show data with start/end times'    
});
addGlobalDisplayType({
    type: DISPLAY_SANKEY,
    label: "Sankey Chart",
    requiresData: true,
    forUser: true,
    category: CATEGORY_RADIAL_ETC,
    preview: "sankey.png"                                    
});

addGlobalDisplayType({
    type: DISPLAY_CALENDAR,
    label: "Calendar Chart",
    requiresData: true,
    forUser: true,
    category: CATEGORY_MISC,
    preview: "calendar.png"
});
addGlobalDisplayType({
    type: DISPLAY_WORDTREE,
    label: "Word Tree",
    requiresData: true,
    forUser: true,
    category: CATEGORY_TEXT,
    preview: "wordtree.png",
    desc:"Specify a number of fields. Each field value is a level in the tree"    
});
addGlobalDisplayType({
    type: DISPLAY_TREEMAP,
    label: "Tree Map",
    requiresData: true,
    forUser: true,
    category: CATEGORY_RADIAL_ETC,
    preview: "treemap.png"    
});

addGlobalDisplayType({
    type: DISPLAY_ORGCHART,
    label: "Org Chart",
    requiresData: true,
    forUser: true,
    category: CATEGORY_RADIAL_ETC,
    preview: "orgchart.png"                                
});




let PROP_CHART_MIN = "chartMin";
let PROP_CHART_MAX = "chartMax";
let DFLT_WIDTH = "600px";
let DFLT_HEIGHT = "200px";

/*
  Create a chart
  id - the id of this chart. Has to correspond to a div tag id 
  pointData - A PointData object (see below)
*/
function RamaddaGoogleChart(displayManager, id, chartType, properties) {
    const ID_TRENDS_CBX = "trends_cbx";
    const ID_PERCENT_CBX = "percent_cbx";
    const ID_COLORS = "colors";
    const ID_HIGHLIGHTFIELDSHOLDER = "highlightfieldsholder";
    const ID_HIGHLIGHTFIELDS = "highlightfields";	    
    let _this = this;
    if(!Utils.isDefined(properties['sortOnDate']))
	properties['sortOnDate'] = true;
    //Init the defaults first
    $.extend(this, {
	debugChartOptions:false,
	useTestData:false,	
        indexField: -1,
        curveType: 'none',
        fontSize: 0,
        showPercent: false,
        percentFields: null,
    });
    const SUPER = new RamaddaFieldsDisplay(displayManager, id, chartType, properties);
    let myProps = [
	{label:'Chart Style'},
	{p:'chartWidth',ex:''},
	{p:'chartHeight',ex:''},
	{p:'chartLeft',d:0},
	{p:'chartRight',d:0},
	{p:'chartTop',d:10,ex:'0'},
	{p:'chartBottom',ex:'0',d:0},

	{p:'textFontSize',ex:12},
	{p:'textBold',ex:true},
	{p:'textItalic',ex:true},
	{p:'textColor',ex:'green'},
	{p:'textFontName',ex:'Times'},
	
	{p:'hAxisTitle'},
	{p:'vAxisTitle'},	
	{p:'hAxisHideTicks'},
	{p:'vAxisHideTicks'},	
	{p:'lineDashStyle',d:null,ex:'2,2,20,2,20'},
	{p:'highlight.lineDashStyle',d:'2,2,20,2,20',ex:'2,2,20,2,20'},
	{p:'nohighlight.lineDashStyle',d:'2,2,20,2,20',ex:'2,2,20,2,20'},	
	{p:'some_field.lineDashStyle',d:'2,2,20,2,20',ex:'2,2,20,2,20'},

	{p:'labelInLegend',ex:'label'},
	{p:'highlight.labelInLegend',d:null,ex:'label',canCache:true},
	{p:'nohighlight.labelInLegend',d:null,ex:'label',canCache:true},	
	{p:'some_field.labelInLegend',d:null,ex:'label',canCache:true},

	{p:'highlightFields',d:null,ex:'fields'},
	{p:'highlightShowFields',d:null,ex:'true'},
	{p:'highlightShowFieldsSize',d:"4",ex:'4'},
	{p:"acceptHighlightFieldsEvent",d:true,ex:'true'},
	{p:'highlightDim',d:'true',ex:'true',tt:'Dim the non highlight lines'},


	{p:'seriesType',d:null,ex:'line|area|bars',canCache:true},
	{p:'highlight.seriesType',d:null,ex:'line|area|bars',canCache:true},
	{p:'nohighlight.seriesType',d:null,ex:'line|area|bars',canCache:true},	
	{p:'some_field.seriesType',d:null,ex:'line|area|bars'},	

	{p:'pointSize',d:null,ex:'0'},
	{p:'highlight.pointSize',d:'0',ex:'4'},
	{p:'nohighlight.pointSize',d:'0',ex:'4'},	
	{p:'some_field.pointSize',d:'4',ex:'4'},

	{p:'lineWidth',d:null,ex:null},
	{p:'highlight.lineWidth',d:null,ex:'2'},
	{p:'nohighlight.lineWidth',d:null,ex:'2'},	
	{p:'some_field.lineWidth',d:'2',ex:'2'},

	{p:'highlight.color',d:null,ex:null},
	{p:'nohighlight.color',d:null,ex:null},
	{p:'some_field.color',d:null,ex:null},

	{p:'pointShape',d:null,ex:null},
	{p:'highlight.pointShape',d:null,ex:null},
	{p:'nohighlight.pointShape',d:null,ex:null},	
	{p:'some_field.pointShape',d:null,ex:null},

	{p:'dragToZoom',d:true},
	{p:'dragToPan',d:false},	

	{p:'skipMissing',d:false,ex:'true',tt:'skip rows  that have any missing values'},
	{p:'replaceNanWithZero'},
	{p:'maxColumns',d:-1},
	{p:'interpolateNulls',d:true,ex:'true'},
	{p:'animateChart',ex:true},
	{p:'animationDuration',d:500},
	{p:'animationEasing',d:'linear'},
	{label:'Trendlines'},
	{p:'showTrendLines',d:null,ex:"true"},
	{p:"trendlineType",ex:"exponential"},
	{p:"trendlineVisibleInLegend",ex:"true"},
	{p:"trendlineColor",d:"red"},
	{p:"trendlineLineWidth",ex:"2"},
	{p:"trendlineOpacity",ex:"0.3"},		    		    		    

	{p:'Annotations'},
	{p:'annotations',ex:'date,label,desc;date,label,desc;',tt:'e.g. 2008-09-29,A,Start of housing crash;2008-11-04,B,Obama elected;'},
 	{p:'annotationFields',ex:'',tt:'Set of fields to add an annotation to the line chart'},
 	{p:'annotationStride',ex:10,tt:'Only show every N annotations'},
 	{p:'annotationLabelField',ex:'field',tt:'Field to use for annotation label'},		
 	{p:'annotationLabelTemplate',ex:'"${field}',tt:'Template to use for label'},		
	


    ];
    this.debugTimes = false;


    this.isGoogleChart = true;
    defineDisplay(this, SUPER, myProps, {

	showFieldsInDialog: function() {
	    return true;
	},
	checkFinished: function() {
	    return true;
	},
	useDisplayMessage:function() {
	    return true;
	},
	//Override so we don't include the expandable class
	getContentsClass: function() {
	    return "display-contents-inner display-" + this.type;
	},

        useChartableFields: function() {
            return true;
        },
        defaultSelectedToAll: function() {
            return false;
        },
	getRequiredPackages: function() {
	    return [];
	},
	chartCallbackPending: false,
        updateUI: function(args) {
	    let callback = this.chartCallbackPending?null:()=>{
		//		console.log(this.type +".updateUI: charts loaded");
		this.updateUI();
	    };
	    this.chartCallbackPending=true;
	    if(!haveGoogleChartsLoaded(callback)) {
		return;
	    }

	    let required = this.getRequiredPackages();
	    if(required.length>0) {
		if(!this.packageLoading) this.packageLoading = {};
		let gotPackages  = required.length;
		//		console.log(this.type+" loading packages:" + required);
		required.forEach(pkg=> {
		    let callback = this.packageLoading[pkg]?null:()=>{
			//			console.log(this.type+" package loaded callback");
			this.updateUI();
		    };
		    if(ramaddaLoadGoogleChart(pkg,callback)) {
			gotPackages--;
		    }
		});
		if(gotPackages>0) return;
	    }
	    args = args || {};
            SUPER.updateUI.call(this, args);
	    //	    console.log(this.type +".updateUI: ready");
	    this.updateUIInner(args);
	},
	updateUIInner: function(args) {

	    let debug = false;
	    if(debug)
		console.log(this.type+".updateUI")
            if (!this.getDisplayReady()) {
		if(debug)
		    console.log("\tdisplay not ready");
                return;
            }
	    if(debug)
		console.log("\tcalling displayData");
	    if(args.dataFilterChanged) {
		//this.setDisplayMessage(this.getLoadingMessage());
		setTimeout(()=>{
		    this.displayData(args.reload, debug);
		},1);
		return;
	    }
            this.displayData(args.reload, debug);
        },
        getWikiAttributes: function(attrs) {
            this.defineWikiAttributes(["vAxisMinValue", "vAxisMaxValue",'vAxisExplicit']);
            SUPER.getWikiAttributes.call(this, attrs);
            if (this.colorList.join(",") != "blue,red,green") {
                attrs.push("colors");
                attrs.push(this.getColorList().join(", "));
            }
        },

        initDialog: function() {
            SUPER.initDialog.call(this);
            let _this = this;
            let updateFunc = function(e) {
                if (e && e.which != 13 && e.which!=0) {
                    return;
                }
                _this.vAxisMinValue = Utils.toFloat(_this.jq("vaxismin").val());
		//		console.log("vaxis:" + _this.vAxisMinValue + " " + this.getVAxisMinValue());
                _this.vAxisMaxValue = Utils.toFloat(_this.jq("vaxismax").val());
                _this.minDate = _this.jq("mindate").val();
                _this.maxDate = _this.jq("maxdate").val();
                _this.displayData();

            };
	    ["vaxismin","vaxismax","mindate","maxdate"].map(f=>{
		this.jq(f).blur(updateFunc);
		this.jq(f).keypress(updateFunc);
	    });



            this.jq(ID_COLORS).keypress(function(e) {
                if (e.which != 13) {
                    return;
                }
                let v = _this.jq(ID_COLORS).val();
                _this.colorList = v.split(",");
                _this.displayData();
                let pointData = _this.dataCollection.getList();
                _this.getDisplayManager().handleEventPointDataLoaded(_this, _this.lastPointData);
            });

            this.jq(ID_TRENDS_CBX).click(function() {
                _this.setProperty('showTrendLines', _this.jq(ID_TRENDS_CBX).is(':checked'));
                _this.displayData();

            });
            this.jq(ID_PERCENT_CBX).click(function() {
                _this.showPercent = _this.jq(ID_PERCENT_CBX).is(':checked');
                _this.displayData();

            });

        },
        setColor: function() {
            let v = prompt("Enter comma separated list of colors to use", this.colorList.join(","));
            if (v != null) {
                this.colorList = v.split(",");
                this.displayData();
                let pointData = this.dataCollection.getList();
                this.getDisplayManager().handleEventPointDataLoaded(this, this.lastPointData);
            }
        },
        getVAxisMinValue: function() {
            return parseFloat(this.getProperty("vAxisMinValue", NaN));
        },
        getVAxisMaxValue: function() {
            return parseFloat(this.getProperty("vAxisMaxValue", NaN));
        },
	getHAxisMinValue: function() {
            return parseFloat(this.getProperty("hAxisMinValue", NaN));
        },
        getHAxisMaxValue: function() {
            return parseFloat(this.getProperty("hAxisMaxValue", NaN));
        },
        getMenuItems: function(menuItems) {
            SUPER.getMenuItems.call(this, menuItems);
            let get = this.getGet();
            //                menuItems.push(HU.onClick(get+".setColor();", "Set color"));

            let min = "0";
            if (!isNaN(this.getVAxisMinValue())) {
                min = "" + this.getVAxisMinValue();
            }
            let max = "";
            if (!isNaN(this.getVAxisMaxValue())) {
                max = "" + this.getVAxisMaxValue();
            }

            let tmp = HU.formTable();
            tmp += HU.formEntry("Axis Range:", HU.input("", min, ["size", "7", ATTR_ID, this.domId("vaxismin")]) + " - " +
				HU.input("", max, ["size", "7", ATTR_ID, this.domId("vaxismax")]));
            tmp += HU.formEntry("Date Range:", HU.input("", this.minDate, ["size", "10", ATTR_ID, this.domId("mindate")]) + " - " +
				HU.input("", this.maxDate, ["size", "10", ATTR_ID, this.domId("maxdate")]));


            tmp += HU.formEntry("Colors:",
				HU.input("", this.getColorList().join(","), ["size", "35", ATTR_ID, this.domId(ID_COLORS)]));
            tmp += "</table>";
            menuItems.push(tmp);

        },
        getChartType: function() {
            return this.getType();
        },
        askMinZAxis: function() {
            this.setMinZAxis(prompt("Minimum axis value", "0"));
        },
        setMinZAxis: function(v) {
            if (v != null) {
                this.vAxisMinValue = parseFloat(v);
                this.displayData();
            }
        },
        askMaxZAxis: function() {
            this.setMaxZAxis(prompt("Maximum axis value", "100"));
        },
        setMaxZAxis: function(v) {
            if (v != null) {
                this.vAxisMaxValue = parseFloat(v);
                this.displayData();
            }
        },
        askMinDate: function() {
            let ex = this.minDate;
            if (ex == null || ex == "") {
                ex = "1800-01-01";
            }
            let v = prompt("Minimum date", ex);
            if (v == null) return;
            this.setMinDate(v);
        },
        setMinDate: function(v) {
            this.minDate = v;
            this.displayData();
        },
        askMaxDate: function() {
            let ex = this.maxDate;
            if (ex == null || ex == "") {
                ex = "2100-01-01";
            }
            let v = prompt("Maximum date", ex);
            if (v == null) return;
            this.setMaxDate(v);
        },
        setMaxDate: function(v) {
            this.maxDate = v;
            this.displayData();
        },
        trendLineEnabled: function() {
            return false;
        },
        getDialogContents: function(tabTitles, tabContents) {
            let height = "600";
            let html = HU.div([ATTR_ID, this.domId(ID_FIELDS), STYLE, HU.css('overflow-y','auto','max-height', height + "px")], "  ");
            if (this.trendLineEnabled()) {
                html += HU.div([ATTR_CLASS, "display-dialog-subheader"], "Other");

                html += HU.checkbox(this.domId(ID_TRENDS_CBX),
				    [],
				    this.getShowTrendLines(),"Show trend line");
                html += " ";
                html += HU.checkbox(this.domId(ID_PERCENT_CBX),
				    [],
				    this.showPercent,"Show percent of displayed total");
                html += "<br>";
            }

            tabTitles.push("Fields");
            tabContents.push(html);
            SUPER.getDialogContents.call(this, tabTitles, tabContents);
        },
        okToHandleEventRecordSelection: function() {
            return true;
        },
        handleEventRecordHighlight: function(source, args) {
	    this.handleEventRecordSelection(source, args);
	},
        handleEventRecordSelection: function(source, args) {
	    SUPER.handleEventRecordSelection.call(this, source, args);
            //TODO: don't do this in index space, do it in time or space space
            if (source == this) {
                return;
            }
            if (!this.okToHandleEventRecordSelection()) {
                return;
	    }
	    let index = this.findMatchingIndex(args.record).index
	    if(index<0 || !Utils.isDefined(index)) {
		return;
	    }
	    this.setChartSelection(index);
        },
        getFieldsToSelect: function(pointData) {
            //STRING return pointData.getChartableFields();
	    return pointData.getRecordFields();
        },
        canDoGroupBy: function() {
            return false;
        },
        clearCache: function() {
        },
        googleChartCallbackPending: false,
        includeIndexInData: function() {
            return false;
        },
        getGroupBy: function() {
            return this.getProperty("groupByField");
        },
        getIncludeIndexIfDate: function() {
            return false;
        },
	makeIndexValue: function(indexField, value, offset) {
	    if(indexField.isString()) {
		return  {v:offset,f:value};
	    } 
	    if(value && value.getTime) {
		return  {v:value,f:this.formatDate(value)}
	    }
	    return value;
	},
	getFieldsToDisplay: function(fields) {
	    return fields;
	},

        displayData: function(reload, debug) {
	    if(this.dataLoadFailed) {
		return;
	    } 

	    if(debug)
		console.log(this.type +" displayData " + this.getId() +" " + this.type);

	    let isExpanded = this.jq(ID_CHART).attr("isexpanded");
	    let originalHeight = this.jq(ID_CHART).attr("original-height");
	    if(isExpanded==="true") {
		this.setProperty("expandedHeight",this.jq(ID_CHART).css("height"));
		this.setProperty("isExpanded",true);
		this.setProperty("originalHeight",originalHeight);
	    } else {
		this.setProperty("expandedHeight",null);
		this.setProperty("isExpanded",false);
		this.setProperty("originalHeight",null);
	    }
            if (!this.getDisplayReady()) {
		if(debug)
		    console.log("\tdisplay not ready");
                return;
            }
            if (this.inError) {
		if(debug)
		    console.log("\tin error");
                return;
            }
            if (!haveGoogleChartsLoaded()) {
		if(debug)
		    console.log("\tgoogle charts have not loaded callback pending:" +this.googleChartCallbackPending);
                if (!this.googleChartCallbackPending) {
                    this.googleChartCallbackPending = true;
		    this.setDisplayMessage(this.getLoadingMessage());
                    setTimeout(()=> {
                        this.googleChartCallbackPending = false;
                        this.displayData();
                    }, 100);
                }
                return;
            }
            if (this.inError) {
                return;
            }
            if (!this.hasData()) {
		this.clearChart();
		this.setDisplayMessage(this.getLoadingMessage());
                return;
            }

	    if(!this.getAcceptEventDataSelection()) {
		this.setDisplayMessage("Creating display...");
	    }

            //            let selectedFields = this.getSelectedFields(this.getFieldsToSelect(pointData));
	    let records =this.filterData();
            let selectedFields = this.getSelectedFields();
	    if(debug)
		console.log("\tpointData #records:" +records.length);


	    if(debug)
		console.log("\tselectedFields:" + selectedFields);

            if (selectedFields.length == 0 && this.lastSelectedFields != null) {
                selectedFields = this.lastSelectedFields;
		if(debug)
		    console.log("\tusing last selectedFields:" + selectedFields);
            }


            if (selectedFields == null || selectedFields.length == 0) {
                if (this.getChartType() == DISPLAY_TABLE || this.getChartType() == DISPLAY_TREEMAP) {
                    selectedFields = this.dataCollection.getList()[0].getNonGeoFields();
		    if(debug)
			console.log("\tfields from data collection:" + selectedFields);
                } else {
                    selectedFields = this.getSelectedFields();
		    if(debug)
			console.log("\tgetSelectedFields again:" + selectedFields);
		    
                    selectedFields = this.getSelectedFields();
		    if(selectedFields.length==0) {
			this.getFields().every(f=>{
			    if(f.isNumeric() && !f.isFieldGeo()) {
				selectedFields = [f];
				return false;
			    }
			    return true;
			});

		    }
                }
            }



            if (selectedFields.length == 0) {
		if(!this.getAcceptEventDataSelection()) {
		    //                    this.setContents("No fields selected");
		    this.setDisplayMessage("No fields selected");
		}
                return;
            }

	    /*
	      for(a in this) {
	      let o = this[a];
	      if(o==null || o.isDisplayThing) continue;
	      let t = typeof o
	      if(t == 'function' || t=='string' || t=='boolean' || t=='number' || t=='undefined') continue;
	      if(Array.isArray(o)) {
	      if(o.length==0) continue;
	      console.log(a +  ' array:' + o.length);
	      } else {
	      console.log(a +  ' ' +t +' ' + Object.keys(o).length);
	      }
	      }*/

            //Check for the skip
            let tmpFields = [];
            for (let i = 0; i < selectedFields.length; i++) {
                if (!this.shouldSkipField(selectedFields[i])) {
                    tmpFields.push(selectedFields[i]);
                }
            }
            selectedFields = tmpFields;
	    if(debug)
		console.log("\tsetting lastSelectedFields:" + selectedFields);
            this.lastSelectedFields = selectedFields;
	    //Do this here because the title, if displayed, may hold a {field} macro
	    //that doesn't get set before we've loaded the data
	    if(this.lastSelectedFields && this.lastSelectedFields.length>0) {
		this.jq(ID_TITLE_FIELD).html(this.lastSelectedFields[0].getLabel(this));
	    }

	    let maxColumns = this.getMaxColumns(-1);
	    if(maxColumns>0) {
		let tmp = [];
		selectedFields.every((f,idx)=>{
		    if(idx>=maxColumns) return false;
		    tmp.push(f)
		    return true;
		});
		selectedFields=tmp;
	    }




            let props = {
                includeIndex: this.includeIndexInData()
            };
            props.groupByIndex = -1;

            let groupBy = this.getGroupBy();
            if (groupBy) {
		this.getFields().every(field=>{
                    if (field.getId() == groupBy) {
                        props.groupByIndex = field.getIndex();
                        props.groupByField = field;
			return false;
                    }
		    return true;
                });
            }

            let fieldsToSelect = selectedFields;
            if (this.raw) {
                fieldsToSelect = this.dataCollection.getList()[0].getRecordFields();
                props.raw = true;
            }

            props.includeIndexIfDate = this.getIncludeIndexIfDate();

            let dataHasIndex = props.includeIndex;

	    let t1= new Date();
	    fieldsToSelect = this.getFieldsToDisplay(fieldsToSelect);
            let dataList = this.getStandardData(fieldsToSelect, props);
	    let t2= new Date();
	    if(this.debugTimes)
		Utils.displayTimes("chart.getStandardData",[t1,t2],true);

	    if(debug)
		console.log(this.type +" fields:" + fieldsToSelect.length +" dataList:" + dataList.length);
            if (dataList.length == 0 && !this.userHasSelectedAField) {
                let pointData = this.dataCollection.getList()[0];
                let chartableFields = this.getFieldsToSelect(pointData);
                for (let i = 0; i < chartableFields.length; i++) {
                    let field = chartableFields[i];
                    dataList = this.getStandardData([field], props);
                    if (dataList.length > 0) {
                        this.setSelectedFields([field]);
                        break;
                    }
                }
            }

            if (dataList.length == 0) {
		this.setContents(this.getMessage(this.getNoDataMessage()));
		//                this.setDisplayMessage(this.getNoDataMessage());
                return;
            }

            if (this.showPercent) {
                let newList = [];
                let isNumber = [];
                let isOk = [];
                let headerRow = null;
                let fields = null;
                if (this.percentFields != null) {
                    fields = this.percentFields.split(",");
                }
                for (let i = 0; i < dataList.length; i++) {
                    let row = this.getDataValues(dataList[i]);
                    if (i == 0) {
                        headerRow = row;
                        continue;
                    }
                    if (i == 1) {
                        let seenIndex = false;
                        for (let j = 0; j < row.length; j++) {
                            let valueIsNumber = (typeof row[j] == "number");
                            let valueIsDate = (typeof row[j] == "object");
                            if (valueIsNumber) {
                                if (dataHasIndex && !seenIndex) {
				    valueIsNumber = false;
                                    seenIndex = true;
                                }
                            }
                            if (valueIsDate) {
                                seenIndex = true;
                            }
                            if (valueIsNumber && fields != null) {
                                valueIsNumber = fields.indexOf(fieldsToSelect[j].getId()) >= 0 ||
                                    fields.indexOf("#" + (j + 1)) >= 0;
                            }
                            isNumber.push(valueIsNumber);
                        }
                        let newHeader = [];
                        for (let j = 0; j < headerRow.length; j++) {
                            let v = headerRow[j];
                            if (!isNumber[j]) {
                                newHeader.push(v);
                            } else {
                                newHeader.push("% " + v);
                            }
                        }
                        newList.push(newHeader);
                    }

                    let total = 0;
                    let cnt = 0;
                    for (let j = 0; j < row.length; j++) {
                        if (isNumber[j]) {
                            total += parseFloat(row[j]);
                            cnt++;
                        }
                    }
                    let newRow = [];
                    for (let j = 0; j < row.length; j++) {
                        if (isNumber[j]) {
                            if (total != 0) {
                                let v = parseFloat(((row[j] / total) * 100).toFixed(1));
                                newRow.push(v);
                            } else {
                                newRow.push(NaN);
                            }
                        } else {
                            newRow.push(row[j]);
                        }
                    }
                    newList.push(newRow);
                }
                dataList = newList;
            }
            try {
		this.clearDisplayMessage();
		let tt1 =new Date();
                this.makeGoogleChart(dataList, props, selectedFields);
		let tt2 =new Date();
		if(this.debugTimes)
		    Utils.displayTimes("chart.makeGoogleChart",[tt1,tt2],true);
            } catch (e) {
		this.handleError("Error making chart:" + e,e);
		this.setIsFinished();
                return;
            }

	    this.setIsFinished();
            let container = this.jq(ID_CHART);
	    if(this.jq(ID_CHART).is(':visible')) {
		this.lastWidth = container.width();
	    } else {
		this.lastWidth = -1;
	    }

	    if(reload) {
		let pointData = this.getData();
		if(pointData) {
		    let dataList = pointData.getRecords();
		    if(dataList.length>0) {
			let record = dataList[0];
			this.propagateEventRecordSelection({record: record});
		    }
		}
	    }
        },
        printDataList: function(dataList) {
            console.log("data list:" + dataList.length);
            for (let i = 0; i < dataList.length; i++) {
                let row = dataList[i];
                let s = "";
                for (let j = 0; j < row.length; j++) {
                    if (j > 0) s += ", ";
                    s += row[j];
                }
                console.log("row: " + i + "  " + s);
            }
        },
	mapCharts: function(func) {
            if (this.charts != null) {
		this.charts.map(chart=>{
		    func(chart);
		});
	    }
	},
        clearChart: function() {
	    if(this.chart) {
		this.chart.clearChart();
		this.chart = null;
	    }
	    this.mapCharts(chart=>{
		if(chart.clearChart) {
		    chart.clearChart();
		}
	    });
	    this.charts = [];
        },
        setChartSelection: function(index) {
	    if(!Array.isArray(index)) {
		index=[index];
	    }
	    let selection=index.map(i=>{
		return {
                    row: i,
		    column:null
		}
	    });
	    this.mapCharts(chart=>{
                if (chart.setSelection) {
		    chart.setSelection(selection);
		}});
	    
        },
        tableHeaderMouseover: function(i, tooltip) {},
	doAddTooltip: function() {
	    return true;
	},
	getAddStyle: function() {
	    return true;
	},
	getAnnotationTemplate: function() {
	    return this.getProperty("annotationTemplate");
	},
	getFormatNumbers: function() {
	    return false;
	},
        getDataTableValueGetter: function() {
	    return (v)=>{return v;}
	},
	getHighlightFields:function() {
	    let p = this.getPropertyFromUrl("highlightFields");
	    return  Utils.split(this.getPropertyFromUrl("highlightFields"),",",true,true)||[];
	},
        handleEventPropertyChanged: function(source, prop) {
	    if(prop.property == "dateRange") {
		let index1=this.findClosestDate(prop.minDate).index;
		let index2=this.findClosestDate(prop.maxDate).index;		
		if(index1>=0) {
		    if(this.getAcceptDateRange()) {
			this.setChartSelection([index1,index2]);
		    }
		}
	    } else    if(prop.property == "highlightFields") {
		if(this.getProperty("acceptHighlightFieldsEvent",true)) {
		    this.setProperty("highlightFields",prop.value);
		    this.forceUpdateUI();
		    let v = Utils.split(prop.value,",",true,true);
		    this.jq(ID_HIGHLIGHTFIELDS).val(v);
		}
		return;
	    }
            SUPER.handleEventPropertyChanged.call(this, source,prop);
	},
	makeSeriesInfo: function(dataTable) {
	    let seriesNames =[];
	    //Assume first one is the index
	    for(let i=1;i<dataTable.getNumberOfColumns();i++) {
		//Styles, etc
		if(dataTable.getColumnRole(i)!="") continue;
		seriesNames.push(dataTable.getColumnLabel(i));
	    }
	    let colors = this.getColorList();
	    let highlightMap  ={};
	    let highlightFields = this.getHighlightFields();
	    let highlightDim = this.getProperty("highlightDim",false);
	    highlightFields.forEach(f=>{highlightMap[f] = true});
	    let seriesInfo = {};
            let useMultipleAxes = this.getProperty("useMultipleAxes", true);
	    let extraMap = {};
	    this.getExtraLines().forEach(info=>{
		let id = Utils.makeId(info.label);
		extraMap[id] = info;
	    });
	    seriesNames.forEach((name,idx)=>{
		name = name.replace(/\(.*\)/g,"");
		//check for the formatted label
		if(name.indexOf('<')>=0) return;
		let id = Utils.makeId(name);
		let highlight = highlightMap[id];
		let s = {
		};
		["labelInLegend", "seriesType","lineDashStyle","pointSize", "lineWidth","color","pointShape"].forEach(a=>{
		    let dflt = this.getProperty((highlight?"highlight.":"nohighlight.") + a,this.getProperty(a));
		    let value = this.getProperty(id+"." + a,dflt);
		    if(extraMap[id] && extraMap[id][a]) {
			value =extraMap[id][a];
		    }
		    
		    if(value && a=="lineDashStyle") {
			let tmp = [];
			let delim = ",";
			if(value.indexOf(";")>=0) delim=";";
			Utils.split(value,delim,true,true).forEach(tok=>{
			    tmp.push(parseFloat(tok));
			});
			value=tmp;
		    }
		    if(a=="seriesType") a = "type";
		    s[a] = value;
		});
		if(!s.color) s.color = colors[idx%colors.length];
		if(highlightFields.length>0) {
		    if(!highlight && highlightDim) {
			s.color = Utils.pSBC(0.75,s.color);
		    }
		}
		if (useMultipleAxes) {
		    if(idx%2==0) {
			s.targetAxisIndex=0;
		    }  else  {
			s.targetAxisIndex=1;
		    }
		}
		seriesInfo[idx] = s;
	    });


	    if(this.getProperty("highlightShowFields",false)) {
		if(this.jq(ID_HIGHLIGHTFIELDSHOLDER).length==0) {
		    this.jq(ID_HEADER2).append(HU.span([ID,this.domId(ID_HIGHLIGHTFIELDSHOLDER)]));
		}
		
		if(this.jq(ID_HIGHLIGHTFIELDS).length==0) {
		    let seriesValues = [];
		    seriesNames.forEach(n=>{
			seriesValues.push([Utils.makeId(n),n]);
		    });
		    seriesValues.sort((a,b)=>{
			return a[1].localeCompare(b[1]);
			
		    });
		    let highlightWidget = SPACE + HU.vbox(["Highlight",
							   HU.select("",[ID,this.domId(ID_HIGHLIGHTFIELDS),"multiple","true","size",this.getProperty("highlightShowFieldsSize","3")],seriesValues,highlightFields)]);
		    let select =  HU.span([CLASS,"display-filter",STYLE,""],highlightWidget);
		    this.jq(ID_HIGHLIGHTFIELDSHOLDER).html(select);
		    this.jq(ID_HIGHLIGHTFIELDS).change(()=>{
			let v = Utils.makeArray(this.jq(ID_HIGHLIGHTFIELDS).val());
			v = Utils.join(v,",");
			this.setProperty("highlightFields",v);
			this.addToDocumentUrl("highlightFields",v);
			this.forceUpdateUI();
			let props = {
			    property: "highlightFields",
			    value:v
			};
			this.propagateEvent(DisplayEvent.propertyChanged, props);
		    });
		}
	    }
	    //	    console.log(JSON.stringify(seriesInfo,null,2));
	    return seriesInfo;
	},
	
	makeTrendlinesInfo: function(dataTable) {
	    let seriesNames =[];
	    //Assume first one is the index
	    for(let i=1;i<dataTable.getNumberOfColumns();i++) {
		//Styles, etc
		if(dataTable.getColumnRole(i)!="") continue;
		seriesNames.push(dataTable.getColumnLabel(i));
	    }
	    let trendlinesInfo = {};
	    seriesNames.forEach((name,idx)=> {
		let id = Utils.makeId(name);
		if(this.getProperty("showTrendline." + id, this.getShowTrendLines())) {
		    let s = {
		    };
		    trendlinesInfo[idx] = s;
		    s.type = this.getProperty("trendlineType." + id,this.getProperty("trendlineType"));
		    s.visibleInLegend = this.getProperty("trendlineVisibleInLegend." + id,this.getProperty("trendlineVisibleInLegend"));
		    s.color = this.getProperty("trendlineColor." + id,
					       this.getProperty("trendlineColor","red"));
		    s.lineWidth = this.getProperty("trendlineLineWidth." + id,this.getProperty("trendlineLineWidth",2));
		    s.opacity = this.getProperty("trendlineOpacity." + id,this.getProperty("trendlineOpacity"));
		}
	    });
	    return trendlinesInfo;
	},
	getExtraLines:function() {
	    if(this.extraLines) return this.extraLines;
	    this.extraLines = [];
	    let idx=0;
	    while(this.getProperty('fixedLine'+idx)) {
		let info = {
		    color: 'blue',
		    lineWidth: 2,
		    label: '',
		    value: 0,
		};
		Utils.split(this.getProperty('fixedLine'+idx),",",true,true).forEach(tok=>{
		    let tuple = Utils.split(tok,':');
		    if(tuple.length!=2) return;
		    let key = tuple[0];
		    let value = tuple[1];
		    if(key=='value') {
			info.value=+value;
		    } else {
			info[key] = value;
		    }
		});
		if(!isNaN(info.value)) {
		    this.extraLines.push(info);
		}
		idx++;
	    }
	    return this.extraLines;
	},
        makeDataTable: function(dataList, props, selectedFields, chartOptions) {
	    let extraLines = this.getExtraLines();
	    let indexIsString = this.getProperty("indexIsString", this.getProperty("forceStrings",false));
	    let useStringInLegend = this.getProperty("useStringInLegend");

	    this.getPropertyCount=0;
	    this.getPropertyCounts={};
	    let dateType = this.getProperty("dateType","date");
	    let debug =    false || displayDebug.makeDataTable;
	    //	    debug=true
	    let debugRows = 1;
	    debugRows = 2;
	    if(debug) this.logMsg(this.type+" makeDataTable #records:" + dataList.length);
	    //    if(debug) console.log(selectedFields.map(f=>{return f.getId()+'-'+f.getLabel()}));
	    let replaceNanWithZero = this.getReplaceNanWithZero();

	    let maxWidth = this.getProperty("maxFieldLength",this.getProperty("maxFieldWidth",-1));
	    let tt = this.getProperty("tooltip");
	    let addTooltip = (tt || this.getProperty("addTooltip",false)) && this.doAddTooltip();
	    
    	    let addStyle= this.getAddStyle();
	    let annotationTemplate = this.getAnnotationTemplate();
	    let formatNumbers = this.getFormatNumbers();


            if (dataList.length == 1) {
		return google.visualization.arrayToDataTable(this.makeDataArray(dataList));
            }

	    let groupField = this.getFieldById(null,  this.getProperty("groupBy"));
	    if(groupField) {
		dataList = this.filterData();
		let groupedData =[];
		let groupValues = [];
		let seen = {};
		let cnt =0;
		let dateToValue =  {};
		let dates = [];
		if(debug)console.log("\trecord[0]:" + dataList[0]);

		dataList.map((record,idx)=>{
		    if(cnt++==0) return;
		    let values = this.getDataValues(record);
		    let value = values[groupField.getIndex()];
		    if(!seen[value]) {
			seen[value]  = true;
			seen[Utils.makeId(value)]  = true;			
			groupValues.push(value);
		    }
		    let newValues =dateToValue[record.getDate()];
		    if(!newValues) {
			dates.push(record.getDate());
			newValues = {};
			dateToValue[record.getDate()] = newValues;
		    }
		    newValues[value] = values[selectedFields[0].getIndex()];
		});

		let data = [];
		let tmp = [];
		let highlightFields = this.getHighlightFields();
		let tmpMap ={};
		highlightFields.forEach(f=>{
		    if(seen[f]) {
			tmp.push(Utils.makeLabel(f));
		    }
		});

		groupValues = Utils.mergeLists(tmp,groupValues);
		groupValues = groupValues.filter(v=>{
		    if(tmpMap[v]) return false;
		    tmpMap[v] = true;
		    return true;
		});
		dates.map(date=>{
		    let tuple=[this.getDateValue(date)];
		    data.push(tuple);
		    let valueMap = dateToValue[date];
		    groupValues.map(group=>{
			let value = valueMap[group];
			tuple.push(value);
		    });
		});

		let header = Utils.mergeLists(["Date"],groupValues);
		if(debug)console.log("\theader:" + header);
		let dataTable = new google.visualization.DataTable();
		if(data.length>0) {
		    //TODO: figure out type of columns with null values
		    let tuple = data[0];
		    tuple.forEach((t,idx)=>{
			let name = header[idx];
			let type = t==null?"number":(typeof t);
			if(type =="number") {
			    if(debug)console.log("\tadd column:" + name+ " type: number");
			    dataTable.addColumn("number", name);
			} else if(type =="string") {
			    if(debug)console.log("\tadd column:" + name+ " type: string");
			    dataTable.addColumn("string", name);
			} else if(t.getTime || (t.v && t.v.getTime)) {
			    if(debug)console.log("\tadd column:" + name+ " type: date");
			    dataTable.addColumn("date", name);
			} else {
			    console.log("\tUnknown type:" + t);
			    console.log(JSON.stringify(t,null,2));
			}
		    });
		}
		dataTable.addRows(data);
		if(chartOptions) {
		    chartOptions.series = this.makeSeriesInfo(dataTable);
		    chartOptions.trendlines = this.makeTrendlinesInfo(dataTable);
		}
		return dataTable;
	    }

            let justData = [];
            let tooltipFields = this.getFieldsByIds(null,this.getProperty("tooltipFields", ""));
            let dataTable = new google.visualization.DataTable();
            let header = this.getDataValues(dataList[0]);
            let sample = this.getDataValues(dataList[1]);
	    let fixedValueS = this.getProperty("fixedValue");
	    let fixedValueN  = fixedValueS?parseFloat(fixedValueS):NaN;
	    let fIdx = 0;
	    let maxHeaderLength = this.getProperty("maxHeaderLength",-1);
	    let maxHeaderWidth = this.getProperty("maxHeaderWidth",-1);
	    let headerStyle= this.getProperty("chartHeaderStyle");
            for (let colIdx = 0; colIdx < header.length; colIdx++) {
		let field=null;
		if(colIdx>0 || !props.includeIndex) {
		    field = selectedFields[fIdx++];
		} else {
		    //todo?
		}
                let value = sample[colIdx];
		let headerLabel = header[colIdx];
		if(maxHeaderLength>0 && headerLabel.length>maxHeaderLength) {
		    let orig = headerLabel;
		    headerLabel = headerLabel.substring(0,maxHeaderLength-1)+"...";
		    headerLabel = HU.span([TITLE,orig], headerLabel);
		}
		if(maxHeaderWidth>0 || headerStyle) {
		    let orig = headerLabel;
		    let style = "";
		    if(maxHeaderWidth>0)
			headerLabel = headerLabel.replace(/ /g,"&nbsp;");
		    if(maxHeaderWidth>0)
			style+="max-width:" + maxHeaderWidth +"px;overflow-x:auto;";
		    if(headerStyle)
			style+=headerStyle;
		    headerLabel = HU.div([TITLE,orig,STYLE,style], headerLabel);
		} 
                if (colIdx == 0 && props.includeIndex) {
                    //This might be a number or a date
                    if ((typeof value) == "object") {
                        //assume its a date
 			if(typeof value.v == "number") {
			    if(indexIsString)  {
				if(debug)console.log("\tadd column:" + headerLabel+ " type: string");
				if(useStringInLegend)
				    dataTable.addColumn('string', headerLabel);
				else
				    dataTable.addColumn('number', headerLabel);

			    }   else {
				if(debug)console.log("\tadd column:" + headerLabel+ " type: number");
				dataTable.addColumn('number', headerLabel);
			    }
			} else {
			    if(debug)console.log("\tadd column:" + headerLabel+ " type: " + dateType);
			    dataTable.addColumn(dateType, headerLabel);
			}
                    } else {
			if(debug)console.log("\tadd column:" + headerLabel+ " type: " + (typeof value) +" sample:" + value);
                        dataTable.addColumn((typeof value), headerLabel);
                    }
                } else {
		    if(colIdx>0 && fixedValueS) {
			if(debug)console.log("\tadd column: fixedValue type: number");
			dataTable.addColumn('number', this.getProperty("fixedValueLabel","Count"));
		    } else {
			if(field.isString()) {
			    if(debug)console.log("\tadd column: " + headerLabel +" type: string");
			    dataTable.addColumn('string', headerLabel);
			} else if(field.isFieldDate()) {
			    if(debug)console.log("\tadd column: " + headerLabel +" type: " + dateType);
			    dataTable.addColumn(dateType, headerLabel);
			} else {
			    if(debug)console.log("\tadd column: " + headerLabel +" type: number");
			    dataTable.addColumn('number', headerLabel);
			}
		    }
		    this.extraLines.forEach(info=>{
			dataTable.addColumn('number', info.label);
		    });
		    if(annotationTemplate) {
			if(debug)console.log("\tadd column: annotation");
			dataTable.addColumn({
			    type: 'string',
			    role: 'annotation',
			    id: 'annotation',
			    'p': {
				'html': true
			    }
			});
		    }

		    if(addStyle) {
			if(debug)
			    console.log("\tadd style column");
			dataTable.addColumn({ type: 'string', role: 'style' });
		    }
		    if(colIdx>0 && fixedValueS) {
			break;
		    }
                }


		if(addTooltip) {
		    if(debug)
			console.log("\tadd tooltip column");
		    dataTable.addColumn({
			type: 'string',
			role: 'tooltip',
			'p': {
			    'html': true
			}
		    });
		}
	    }

	    if(debug) {
		console.log("columns:");
		for(let i=0;i<dataTable.getNumberOfColumns();i++)
		    console.log("\tcol[" + i +"]=" + dataTable.getColumnLabel(i) +" role:" +
				dataTable.getColumnRole(i)+
				" type:" + dataTable.getColumnType(i));
	    }

	    let annotationStride = this.getAnnotationStride(0);
	    let annotationLabelTemplate = this.getAnnotationLabelTemplate();

	    let annotationCnt = 0;	    
	    if(Utils.stringDefined(this.getProperty("annotations")) ||
	       Utils.stringDefined(this.getProperty("annotationFields"))) {
		let clonedList = Utils.cloneList(dataList);
		clonedList.shift();
		this.annotations  = new Annotations(this,clonedList);

		if(this.annotations.hasFields()) {
                    dataTable.addColumn({
			type: 'string',
			role: 'annotation',
			id: 'annotation',
			'p': {
                            'html': true
			}
                    });
		    dataTable.addColumn({
			type: 'string',
			role: 'annotationText',
			'p': {
                            'html': true
			}
                    });
		}
	    }

	    if(this.annotations?.isEnabled()) {
		if(this.annotations.getShowLegend()) {
		    //Pad the left to align with  the chart axis
		    this.jq(ID_LEGEND).html("<table width=100%><tr valign=top><td width=10%></td><td width=90%>" +
					    HU.div([CLASS, "display-chart-legend"],this.annotations.getLegend())
					    +"</td></tr></table>");
		}
		dataTable.addColumn({
                    type: 'string',
                    role: 'annotation',
		    id: 'annotation',
                    'p': {
                        'html': true
                    }
                });
		dataTable.addColumn({
                    type: 'string',
                    role: 'annotationText',
                    'p': {
                        'html': true
                    }
                });
	    }


	    let times = [new Date()];
	    let records = [];
            for (let i = 1; i < dataList.length; i++) {
		records.push(dataList[i].record);
	    }
	    let colors =  this.getColorTable(true);
            let colorBy = this.getColorByInfo(records);
	    let valueGetter = this.getDataTableValueGetter(records);
	    let didColorBy = false;
	    let tuples = [];
            for (let rowIdx = 1; rowIdx < dataList.length; rowIdx++) {
		let record =dataList[rowIdx];
                let row = this.getDataValues(record);
		//		let index = row[0];
		//		if(index.v) index  = index.v;
		let theRecord = record.record;
		let color = "";
                if (colorBy.index >= 0) {
                    let value = theRecord.getData()[colorBy.index];
		    hasColorByValue  = true;
		    colorByValue = value;
                    didColorBy = true;
		    color =  colorBy.getColorFromRecord(theRecord);
                }

                row = row.slice(0);
                let newRow = [];
		if(debug && rowIdx<debugRows)
		    console.log("row[" + rowIdx+"]:");

		let fIdx=0;
		let rowOk = true;
		let skipMissing = this.getSkipMissing();
                for (let colIdx = 0; colIdx < row.length; colIdx++) {
		    let field = selectedFields[fIdx++];
                    let value = row[colIdx];
		    //		    if(rowIdx==1)			console.log("\tcol:" + colIdx +" value:", value,' type:'+(typeof value));
		    if(indexIsString) {
			if(Utils.isDefined(value.f)) {
			    let s = (value.f).toString().replace(/\n/g, " ");
			    if(s.trim().length==0) s='<blank>';
			    if(maxWidth>0 && s.length>maxWidth)
				s = s.substring(0,maxWidth) +"...";
			    value.f = s;
			}
		    }
		    if(colIdx>0 && fixedValueS) {
			newRow.push(valueGetter(fixedValueN, colIdx, field, theRecord));
			if(debug && rowIdx<debugRows)
			    console.log("\t fixed:" + fixedValueN);
		    } else {
			let type = (typeof value);
			if(type == "number") {
			    if(skipMissing && isNaN(value)) {
				rowOk = false;
				break;
			    }
			    if(formatNumbers) {
				value = {v:value,f:String(this.formatNumber(value))};
				if(replaceNanWithZero && isNaN(value.v)) value.v=0
			    }
			}  else if(type=="boolean") {
			    value = String(value);
			}
			if(debug && rowIdx<debugRows) {
			    let v = value.f?("f:" + value.f +" v:" +value.v):value;
			    //			    console.log("\t value[" + colIdx +"]=",v," " + (typeof value));
			}
			if(maxWidth>0 && type == "string" && value.length > maxWidth)
			    value = value.substring(0,maxWidth) +"...";
			let o = valueGetter(value, colIdx, field, theRecord);
			if(colIdx==0 && useStringInLegend) {
			    if(Utils.isDefined(o.f)) o=o.f;
			}
			newRow.push(o);
		    }
                    if (colIdx == 0 && props.includeIndex) {
			/*note to self - an inline comment breaks the minifier 
			  if the index so don't add a tooltip */
                    } else {
			extraLines.forEach(info=>{
			    newRow.push(info.value);
			});

			if(annotationTemplate) {
			    let v = annotationTemplate.replace("${value}",value.f||value);
			    if(debug && rowIdx<debugRows)
				console.log("\t annotation" + v);
			    newRow.push(v);
			}
			if(addStyle) {
			    newRow.push(color);
			    if(debug && rowIdx<debugRows)
				console.log("\t color:" + color);
			}
                    }
		    if(colIdx>0 && fixedValueS) {
			break;
		    }
		    if(addTooltip) {
			let tooltip = "";
			if(tt) {
			    tooltip  = this.getRecordHtml(theRecord,null,tt);
			} else {
			    let label = "";
			    if (theRecord) {
				for (let j = 0; j < tooltipFields.length; j++) {
				    label += "<b>" + tooltipFields[j].getLabel(this) + "</b>: " +
					theRecord.getValue(tooltipFields[j].getIndex()) + "<br>";
				}
			    }
			    tooltip += label;
			    for (let j = 0; j < row.length; j++) {
				if (j > 0)
				    tooltip += "<br>";
				label = header[j].replace(/ /g, "&nbsp;");
				value = row[j];
				if (!Utils.isDefined(value)) value = "NA";
				if (value && value.f) {
				    value = value.f;
				}
				
				if (Utils.isNumber(value)) {
				    value = this.formatNumber(value);
				}
				value = "" + value;
				value = value.replace(/ /g, SPACE);
				tooltip += HU.b(label) + ":" + SPACE + value;
			    }
			}
			tooltip = HU.div([STYLE,HU.css('padding','8px')],tooltip);
			newRow.push(tooltip);
			if(debug && rowIdx<debugRows)
			    console.log("\t added tooltip");
		    }



		}

		if(!rowOk) continue;



		if(this.annotations?.hasFields()) {
                    if (theRecord) {
			let desc = "";
			this.annotations.getFields().forEach(f=>{
			    let d = HU.b(f.getLabel())+': '+f.getValue(theRecord);
			    if(d!="")
				desc+= (d+"<br>");
			});
			desc = desc.trim();
			desc = desc.replace(/ /g,"&nbsp;");
			annotationCnt++;
			let label = null; 
			if(annotationLabelTemplate) {
			    label = this.applyRecordTemplate(theRecord, null, null,annotationLabelTemplate);
			} else {
			    if(desc.trim().length>0) {
				label =""+(this.annotations.labelField?theRecord.getValue(this.annotations.labelField.getIndex()):("#"+annotationCnt))
				if(label.trim().length==0) label = "#"+annotationCnt;
			    }
			}
			//			debug =true;
			if(debug && rowIdx<debugRows) {
			    console.log("\t label:" + label);
			    console.log("\t desc:" + desc);
			}

			debug =false;
			if(annotationStride<=0 || (annotationCnt%annotationStride)==0) {
			    newRow.push(label);
			    newRow.push(desc);
			} else {
			    newRow.push(null);
			    newRow.push(null);

			}
		    } else {
			if(i<2)
			    console.log("No records for annotation");
		    }
		}
		if(this.annotations &&  this.annotations.isEnabled()) {
		    let annotations = this.annotations.getAnnotationsFor(rowIdx);
		    if(annotations) {
			if(debug && rowIdx<debugRows) {
			    console.log("\t annotation:" + annotations);
			}
			let label = "";
			let desc = "";
			annotations.forEach(a=>{
			    if(label!="") label+="/";
			    label+=a.label;
			    if(desc!="") desc+="<br>";
			    else {
				if(a.record && a.record.getDate()) {
				    desc+=HU.b(this.formatDate(a.record.getDate()))+"<br>";
				}
			    }
			    desc+=a.description;			    
			});
			newRow.push(label);
			newRow.push(desc);
		    } else {
			if(debug && rowIdx<debugRows) {
			    console.log("\t annotation:" + "null");
			    console.log("\t desc:" + "null");
			}
			newRow.push(null);
			newRow.push(null);
		    }
		    debug =false;
		}
		//		if(justData.length<4)   console.log(newRow);
                justData.push(newRow);
	    }

	    if(debug)
		console.log("#rows:" + justData.length);

	    times.push(new Date());
            dataTable.addRows(justData);
            if (didColorBy) {
		colorBy.displayColorTable();
            }
	    if(chartOptions) {
		//Only make the series if it isn't stacked
		if(!chartOptions.isStacked) {
		    chartOptions.series = this.makeSeriesInfo(dataTable);
		}
		chartOptions.trendlines = this.makeTrendlinesInfo(dataTable);
	    }

	    if(this.debugTimes)
		Utils.displayTimes("makeDataTable",times,true);
            return dataTable;
        },
        makeChartOptions: function(dataList, props, selectedFields) {
            let chartOptions = {
		interpolateNulls: this.getInterpolateNulls(),
                tooltip: {
                    isHtml: true,
		    //		    ignoreBounds: true, 
		    //changed this to focus from both as when both then the tooltip
		    //that is shown on a click stays around when the mouse over tooltip
		    //is shown
		    trigger: 'focus' 
                },
            };


            $.extend(chartOptions, {
		width:"100%",
                lineWidth: this.getProperty("lineWidth",1),
                colors: this.getColorList(),
                curveType: this.curveType,
		pointShape:this.getProperty("pointShape"),
		pointSize: this.getProperty("pointSize"),
                vAxis: {}
            });

            chartOptions.backgroundColor = {};
            chartOptions.chartArea = {};
            chartOptions.chartArea.backgroundColor = {};
	    

            chartOptions.legend = {
                textStyle: {}
            };
            chartOptions.hAxis = {
                gridlines: {},
                minorGridlines: {},		
                textStyle: {},
            };
	    chartOptions.xannotations = {
		textStyle: {
		    fontSize: this.getProperty('annotationsFontSize',12),
		    color: this.getProperty('annotationsTextColor')
		},
		stem: {
		    color:this.getProperty('annotationsStemColor'),
		    length:this.getProperty('annotationsStemLength'),
		},
		style: this.getProperty('annotationsStyle')
	    };


	    if(this.getDragToZoom()) {
		chartOptions.explorer =  { 
		    actions: ['dragToZoom', 'rightClickToReset'],
		    axis: 'horizontal',
		    keepInBounds: true,
		    maxZoomIn: 4.0
		};
	    } else if(this.getDragToPan()) {
		chartOptions.explorer= {
		    axis: 'horizontal',
		    keepInBounds: true,
		    maxZoomIn: 4.0
		}
	    }

            chartOptions.vAxis = {
                gridlines: {},
                minorGridlines: {},		
                textStyle: {},
            };
	    if(this.getProperty("vAxisReverse"))
		chartOptions.vAxis.direction=-1;


	    chartOptions.hAxis.minValue = this.getProperty("hAxisMinValue");
	    chartOptions.hAxis.maxValue = this.getProperty("hAxisMaxValue");
	    chartOptions.vAxis.minValue = this.getProperty("vAxisMinValue");
	    chartOptions.vAxis.maxValue = this.getProperty("vAxisMaxValue");

	    if(this.getProperty('vAxisExplicit')) {
		chartOptions.vAxis.viewWindowMode='explicit';
		chartOptions.vAxis.viewWindow  ={
		    max:chartOptions.vAxis.maxValue,
                    min:chartOptions.vAxis.minValue
		}
	    }




	    chartOptions.vAxis.logScale = this.getProperty("vAxisLogScale",this.getProperty("logScale"));
	    chartOptions.hAxis.logScale = this.getProperty("hAxisLogScale");

            chartOptions.hAxis.titleTextStyle = {};
            chartOptions.vAxis.titleTextStyle = {};
	    if(this.getProperty("hAxisDateFormat")) {
		chartOptions.hAxis.format = this.getProperty("hAxisDateFormat");
	    }


	    //	    this.getPropertyShow = true;
	    let lineColor = this.getProperty("lineColor");
	    let backgroundColor = this.getProperty("chartBackground");
            this.setPropertyOn(chartOptions.backgroundColor, "chart.fill", "fill", backgroundColor);
            this.setPropertyOn(chartOptions.backgroundColor, "chart.stroke", "stroke", this.getProperty("chartArea.fill", ""));
            this.setPropertyOn(chartOptions.backgroundColor, "chart.strokeWidth", "strokeWidth", null);
            this.setPropertyOn(chartOptions.chartArea.backgroundColor, "chartArea.fill", "fill", backgroundColor);
            this.setPropertyOn(chartOptions.chartArea.backgroundColor, "chartArea.stroke", "stroke", null);
            this.setPropertyOn(chartOptions.chartArea.backgroundColor, "chartArea.strokeWidth", "strokeWidth", null);

	    let minorGridLinesColor = this.getProperty("minorGridLines.color",this.getProperty("gridlines.color")||lineColor||"transparent");
            this.setPropertyOn(chartOptions.hAxis.gridlines, "hAxis.gridlines.color", "color", this.getProperty("gridlines.color")|| lineColor);
	    this.setPropertyOn(chartOptions.hAxis.minorGridlines, "hAxis.minorGridlines.color", "color", minorGridLinesColor);

	    this.setPropertyOn(chartOptions.hAxis, "hAxis.baselineColor", "baselineColor", this.getProperty("baselineColor")|| lineColor);	    
            this.setPropertyOn(chartOptions.vAxis.gridlines, "vAxis.gridlines.color", "color", this.getProperty("gridlines.color")|| lineColor);
	    this.setPropertyOn(chartOptions.vAxis.minorGridlines, "vAxis.minorGridlines.color", "color",  minorGridLinesColor);
	    this.setPropertyOn(chartOptions.vAxis, "vAxis.baselineColor", "baselineColor", this.getProperty("baselineColor")|| lineColor);

            let textColor = this.getTextColor();
	    let textBold = this.getTextBold();
	    let textItalic = this.getTextItalic();
            let textFontName = this.getTextFontName();
            let fontSize = this.getTextFontSize();	    
            this.setPropertyOn(chartOptions.hAxis.textStyle, "hAxis.text.color", "color", this.getProperty("axis.text.color", textColor));
            this.setPropertyOn(chartOptions.vAxis.textStyle, "vAxis.text.color", "color", this.getProperty("axis.text.color", textColor));
            this.setPropertyOn(chartOptions.hAxis.textStyle, "hAxis.text.bold", "bold", textBold);
            this.setPropertyOn(chartOptions.vAxis.textStyle, "vAxis.text.bold", "bold", textBold);
            this.setPropertyOn(chartOptions.hAxis.textStyle, "hAxis.text.italic", "italic", textItalic);
            this.setPropertyOn(chartOptions.vAxis.textStyle, "vAxis.text.italic", "italic", textItalic);
            this.setPropertyOn(chartOptions.hAxis.textStyle, "hAxis.text.fontName", "fontName", textFontName);
            this.setPropertyOn(chartOptions.vAxis.textStyle, "vAxis.text.fontName", "fontName", textFontName);
            this.setPropertyOn(chartOptions.hAxis.textStyle, "hAxis.text.fontSize", "fontSize",fontSize);
            this.setPropertyOn(chartOptions.vAxis.textStyle, "vAxis.text.fontSize", "fontSize", fontSize);

	    chartOptions.vAxis.title  =
		Utils.decodeText(this.getProperty("vAxis.text", this.getProperty("vAxisText",this.getVAxisTitle())));

	    chartOptions.hAxis.title  =
		Utils.decodeText(this.getProperty("hAxis.text", this.getProperty("hAxisText",this.getHAxisTitle())));
	    chartOptions.hAxis.slantedText = this.getProperty("hAxis.slantedText",this.getProperty("slantedText",false));
            this.setPropertyOn(chartOptions.hAxis.titleTextStyle, "hAxis.text.color", "color", textColor);
            this.setPropertyOn(chartOptions.vAxis.titleTextStyle, "vAxis.text.color", "color", textColor);
            this.setPropertyOn(chartOptions.legend.textStyle, "legend.text.color", "color", textColor);

	    chartOptions.hAxis.viewWindow = {};
	    if(Utils.isDefined(this.getProperty('hAxisViewMin'))) {
		chartOptions.hAxis.viewWindow.min = +this.getProperty('hAxisViewMin');
	    }
	    if(Utils.isDefined(this.getProperty('hAxisViewMax'))) {
		chartOptions.hAxis.viewWindow.max = +this.getProperty('hAxisViewMax');
	    }	    





	    let prop;
	    prop = this.getProperty("hAxis.ticks");
	    if(prop || prop=="")  {
		let fmt = this.getProperty("hAxis.tickTemplate","${value}");
		let ticks = Utils.split(this.getProperty("hAxis.ticks"),",",true,true);
		ticks=ticks.map(t=> {
		    let toks = t.split(':');
		    let label;
		    if(toks.length>1) {
			t = toks[0];
			label=toks[1];
		    } else {
			label = fmt.replace("${value}",t);
		    }
		    return {
			v:t,
			f:label
		    }
		});
		chartOptions.hAxis.ticks  = ticks;
	    }
	    prop = this.getProperty("vAxis.ticks");
	    if(prop || prop=="")  {
		let fmt = this.getProperty("vAxis.tickTemplate","${value}");
		let ticks = Utils.split(this.getProperty("vAxis.ticks"),",",true,true);
		ticks=ticks.map(t=> {
		    return {
			v:t,
			f:fmt.replace("${value}",t)
		    }
		});
		chartOptions.vAxis.ticks  = ticks;
	    }

	    if(this.getHAxisHideTicks()) 
		chartOptions.hAxis.ticks  = [];
	    if(this.getVAxisHideTicks()) 
		chartOptions.vAxis.ticks  = [];	    

            if (fontSize > 0) {
                chartOptions.fontSize = fontSize;
            }

	    let defaultRanges=[];
	    let numeric = [];
            dataList.forEach((v,idx)=>{
		if(idx==0) return;
		let tuple = this.getDataValues(v);
		if(idx==1) {
		    tuple.forEach((tv,idx)=>{
			numeric.push((typeof tv)=="number");
		    });
		    numeric.forEach(v=>defaultRanges.push([Number.MAX_VALUE,Number.MIN_VALUE]));
		}
		
		
		let cnt = 0;
		tuple.forEach((tv,idx)=>{
		    if(numeric[idx]) {
			defaultRanges[cnt][0] = Math.min(defaultRanges[cnt][0],tv);
			defaultRanges[cnt][1] = Math.max(defaultRanges[cnt][1],tv);
			cnt++;
		    }
		});
	    });

            let range = [NaN, NaN];
	    //	    console.log("range:" +this.getVAxisMinValue());
            if (!isNaN(this.getVAxisMinValue())) {
                range[0] = this.getVAxisMinValue();
            } else if (defaultRanges.length>0) {
		if(this.getProperty("vAxisUseDefault")) {
                    range[0] = defaultRanges[0][0];
		}
            }

	    if (!isNaN(this.getVAxisMaxValue())) {
                range[1] = this.getVAxisMaxValue();
            } else if (defaultRanges.length>0) {
		//                range[1] = defaultRanges[0][1];
            }



            if (!isNaN(range[0])) {
                chartOptions.vAxis.minValue = range[0];
            }
            if (!isNaN(range[1])) {
                chartOptions.vAxis.maxValue = range[1];
		//		chartOptions.vAxis.maxValue = null;
            }

	    //	    console.log(chartOptions.vAxis.maxValue);

            this.chartDimensions = {
                width: "90%",
                left: "10%",
                right: 10,
            }

            useMultipleAxes = this.getProperty("useMultipleAxes", true);

            if ((selectedFields.length > 1 && useMultipleAxes) || this.getProperty("padRight", false) === true) {
                this.chartDimensions.width = "80%";
            }


            if (this.getShowTrendLines()) {
                chartOptions.trendlines = {
                    0: {
                        type: 'linear',
                        color: 'green',
                    }
                };
            }
	    
	    let expandedHeight  = this.getProperty("expandedHeight");
	    if(expandedHeight) {
		chartOptions.height = expandedHeight;
	    }
	    if(this.getPropertyShow) {
		this.getPropertyShow = false;
		Utils.makeDownloadFile("props.txt",this.getPropertyOutput);
	    }
            this.setContents(HU.div([ID,this.domId(ID_CHARTS)]));
	    


            return chartOptions;
        },
        getChartHeight: function() {
            return this.getProperty('chartHeight', this.getProperty("height"));
        },
        getChartWidth: function() {
            return this.getProperty('chartWidth', this.getProperty('width'));
        },
        getChartDiv: function(chartId) {
            let divAttrs = [ATTR_ID, chartId];
            let style = "";
            let width = this.getChartWidth();
            if (false && width) {
		if(width.endsWith("%")) {
                    style += HU.css("width", width);
		} else {
                    if (width > 0)
			style += HU.css("width", width + "px");
                    else if (width < 0)
			style += HU.css("width" , (-width) + "%");
                    else
			style += HU.css("width", width);
		}
            } else {
		//                style += HU.css("width","100%");
            }
	    let expandedHeight  = this.getProperty("expandedHeight");
            let height =  this.getChartHeight();
	    if(expandedHeight) {
                style += HU.css("height", expandedHeight);
	    } else {
		if (height) {
                    if (height > 0)
			style += HU.css("height", height + "px");
                    else if (height < 0)
			style += HU.css("height", (-height) + "%");
                    else
			style += HU.css("height", height);
		} else {
                    style += HU.css("height", "100%");
		}
	    }
	    //	    style += HU.css("text-align","center");
            divAttrs.push(STYLE);
            divAttrs.push(style);
	    divAttrs.push(CLASS);
	    divAttrs.push("ramadda-expandable-target");
	    let isExpanded = this.getProperty("isExpanded");
	    let originalHeight = this.getProperty("originalHeight");
	    if(isExpanded) {
		divAttrs.push("isexpanded","true")
		divAttrs.push("original-height",originalHeight)
	    }
	    if(this.getProperty("expandableHeight")) {
		divAttrs.push("expandable-height");
		divAttrs.push(this.getProperty("expandableHeight"));
	    }
            return HU.div(divAttrs, "");
        },
        doMakeGoogleChart: function(dataList, props, chartDiv, selectedFields, chartOptions) {
            throw new Error("doMakeGoogleChart undefined");
        },
	makeGoogleChart: function(dataList, props, selectedFields) {
	    //	    try {
	    this.doMakeGoogleChartInner(dataList,props,selectedFields);
	    //	    } catch(err) {
	    //		this.handleError("Error creating chart: " + err, err);
	    //	    }
	},
	setAxisRanges: function(chartOptions, selectedFields, records) {
	    if(this.getProperty("hAxisFixedRange")) {
		let x = this.getColumnValues(records, selectedFields[0]);
		chartOptions.hAxis.minValue = x.min;
		chartOptions.hAxis.maxValue = x.max;
	    }

	    let vaxes = {};
	    let getProp = (f,prop,dflt)=>{
		let v= Utils.getProperty(this.getProperty(f.getId()+'.' + prop,this.getProperty(prop,dflt)));
		return v;
	    }
	    selectedFields.forEach((f,idx)=>{
		let min = this.getProperty(f.getId()+'.vAxisMinValue');
		let max = this.getProperty(f.getId()+'.vAxisMaxValue');		
		let viewWindow = {};
		if(Utils.isDefined(min)) viewWindow.min= parseFloat(min);
		if(Utils.isDefined(max)) viewWindow.max= parseFloat(max);		
		vaxes[idx] = { viewWindow: viewWindow}
		vaxes[idx].title=getProp(f,'vAxisTitle')
		let ts = vaxes[idx].textStyle={};
		ts.color= getProp(f,'vAxis.text.color');
		ts.fontSize=getProp(f,'vAxis.text.fontSize');
		ts.fontName = getProp(f,'vAxis.text.fontName');
		ts.bold=getProp(f,'vAxis.text.bold');
		ts.italic=getProp(f,'vAxis.text.italic');		
	    })

	    chartOptions.vAxes = vaxes;

	    if(this.getProperty("vAxisFixedRange") || this.getProperty("vAxisSelectedFields") || this.getProperty("vAxisAllFields")) {
		let min = Number.MAX_VALUE;
		let max = Number.MIN_VALUE;		
		let fields = this.getProperty("vAxisAllFields")?this.getFields():selectedFields;
		fields.forEach(f=>{
		    if(f.isFieldNumeric()) {
			let y = this.getColumnValues(records, f);
			if(!isNaN(y.min))
			    min  = Math.min(min, y.min);
			if(!isNaN(y.max))
			    max  = Math.max(max, y.max);
		    }
		});
		if(min!=Number.MAX_VALUE) {
		    chartOptions.vAxis.minValue = min;
		    chartOptions.vAxis.maxValue = max;
		}
	    }

	},
	doMultiChartsByField:function() {
	    return this.getProperty('doMultiChartsByField');
	},
	makeMultiChart:function (label, dataList, fields,props) {
	    let multiStyle="width:200px;" + this.getProperty("multiStyle","");
	    let multiLabelTemplate=this.getProperty("multiLabelTemplate","${value}");
	    let labelPosition = this.getProperty("multiChartsLabelPosition","bottom");
	    let tmpChartOptions = this.chartOptions;
	    this.chartOptions = $.extend({},this.chartOptions);
	    label = multiLabelTemplate.replace("${value}",label);
	    let header = HU.div([CLASS,"display-multi-header"], label);
	    let top =labelPosition=="top"?header:"";
	    let bottom = labelPosition=="bottom"?header:"";
	    let innerId = this.domId(ID_CHART)+"_" + this.chartCount;
	    let div = HU.div([CLASS,"display-multi-div", STYLE,HU.css('display','inline-block')+ multiStyle], top + this.getChartDiv(innerId) + bottom);
	    this.jq(ID_CHARTS_INNER).append(div);
	    let chart = this.makeGoogleChartInner(dataList, innerId, props, fields);
	    if(chart) {
		this.charts.push(chart);
		chart.chartOptions = this.chartOptions;
	    }
	    this.chartOptions =tmpChartOptions;
	    return chart;
	},


	doMakeGoogleChartInner: function(dataList, props, selectedFields) {
            if (typeof google == 'undefined') {
                this.setDisplayMessage("No google");
                return;
            }
            this.chartOptions = this.makeChartOptions(dataList, props, selectedFields);
	    this.chartOptions.bar = {groupWidth:"95%"}
            if (!Utils.isDefined(this.chartOptions.height)) {
                this.chartOptions.height = "100%";
            }
	    this.charts = [];
	    this.chartCount  = -1;

	    //	    if(this.getChartHorizontal()) {
	    //		this.chartOptions.bars='horizontal'
	    //	    }
            let records = this.getPointData().getRecords();
	    this.setAxisRanges(this.chartOptions, selectedFields, records);
	    //	    console.log(JSON.stringify(this.chartOptions, null,2));
	    //Clear out any existing charts
	    this.clearChart();
	    if(this.getProperty("doMultiCharts",this.getProperty("multipleCharts",false))) {
		this.jq(ID_CHARTS).html(HU.div([ID,this.domId(ID_CHARTS_INNER),STYLE,HU.css('text-align','center')]));
		if(this.doMultiChartsByField()) {
		    this.multiChartData=[];
		    selectedFields.forEach((field,idx)=>{
			this.chartCount  =idx;
			let dataList = this.getStandardData([field], props);
			let chart = this.makeMultiChart(field.getLabel(), dataList, [field], props);
			this.multiChartData.push({props:props,
						  fields:[field],
						  dataList:dataList,
						  field:field,
						  chartOptions:chart.chartOptions,
						  chart:chart});
		    });
		    return;
		}

		let multiField=this.getFieldById(null,this.getProperty("multiField"));
		let map = {};
		let groups = [];
		let tmp = [];
		dataList.forEach((v,idx)=>{if(idx>0) tmp.push(v)});
		if(!multiField) {
		    tmp.sort(function(a,b) {
			let v1 = a.record?a.record.getDate():a.date;
			let v2 = b.record?b.record.getDate():b.date;
			return v1.getTime()-v2.getTime();
		    });
		}
		dataList = Utils.mergeLists([dataList[0]], tmp);
		dataList.forEach((v,idx)=>{
		    if(idx==0) return;
                    let record = v.record;
		    let groupValue = record?multiField?record.getValue(multiField.getIndex()):record.getDate():v.date;
		    let list=null;
		    list = map[groupValue];
		    if(!list) {
			list = [];
			map[groupValue] = list;
			groups.push(groupValue);
		    }
		    list.push(v);
		})
		if(multiField) groups.sort();
		groups.forEach((groupValue,idx)=>{
		    this.chartCount  =idx;
		    let tmpDataList = [];
		    let list = map[groupValue];
		    tmpDataList.push(dataList[0]);
		    tmpDataList = Utils.mergeLists(tmpDataList,list);
		    let label = groupValue;
		    if(groupValue.getTime) label = this.formatDate(groupValue);
		    this.makeMultiChart(label, tmpDataList,  selectedFields,props);

		});
	    } else {
		this.jq(ID_CHARTS).append(this.getChartDiv(this.domId(ID_CHART)));
		let chart = this.makeGoogleChartInner(dataList, this.domId(ID_CHART), props, selectedFields);
		if(chart) this.charts.push(chart);
	    }
	},

	drawChart:function(chart,dataTable,chartOptions) {
	    chart.draw(dataTable, 	    chartOptions);
	},

	makeGoogleChartInner: function(dataList, chartId, props, selectedFields) {
	    let chartDiv = document.getElementById(chartId);
	    if(!chartDiv) {
		this.logMsg("makeGoogleChart: no chart div found:" + chartId);
		return;
	    }
	    let dataTable = this.makeDataTable(dataList, props, selectedFields, this.chartOptions);
            let chart = this.doMakeGoogleChart(dataList, props, chartDiv, selectedFields, this.chartOptions);
            if (chart == null) return null;
            if (!dataTable) {
                this.setDisplayMessage(this.getNoDataMessage());
                return null;
            }
	    if(this.getProperty("vAxisSharedRange")) {
		let indexIsString = this.getProperty("indexIsString", this.getProperty("forceStrings",false));
		let min = NaN;
		let max = NaN;		
		for(let i=0;i<dataTable.getNumberOfColumns();i++) {
		    //if(i==0 && indexIsString) continue;
		    //should we always skip the first column since it is the index
		    if(i==0) continue;
		    if(dataTable.getColumnType(i)!='number') continue;
		    let minmax = dataTable.getColumnRange(i);
		    minmax={min:NaN,max:NaN};
		    let values = dataTable.getDistinctValues(i);
		    values.forEach(v=>{
			if(isNaN(v))return;
			min = Utils.min(min,v);
			max = Utils.max(max,v);			
		    });
		}
		this.getExtraLines().forEach(info=>{
		    min = Utils.min(min,info.value);
		    max = Utils.max(max,info.value);			
		});

		if(!isNaN(min) && !isNaN(max)) {
		    let diff = max-min;
		    //pad out 10%
		    min-=diff*0.1;
		    max+=diff*.1;
		    if(Utils.isDefined(this.getProperty('vAxisMinValue')))min=+this.getProperty('vAxisMinValue');
		    if(Utils.isDefined(this.getProperty('vAxisMaxValue')))max=+this.getProperty('vAxisMaxValue');
		    
                    chartOptions.vAxis.minValue = min;
                    chartOptions.vAxis.maxValue = max;
		}
            }


	    if(this.getAnimateChart(this.getProperty("animation",false,true))) {
		this.chartOptions.animation = {
		    startup: true,
		    duration:parseFloat(this.getAnimationDuration()),
		    easing:this.getAnimationEasing()
		};
		HU.callWhenScrolled(this.domId(ID_CHART),()=>{
		    if(!this.animationCalled) {
			this.animationCalled = true;
			this.mapCharts(chart=>{
			    this.drawChart(chart,dataTable, this.chartOptions);
			});
		    }
		});
	    } else {
		try {
		    if(this.debugChartOptions)
			console.log(JSON.stringify(this.chartOptions, null,2));
		    this.chart = chart;
		    this.dataTable = dataTable;
		    let testData = google.visualization.arrayToDataTable([
			['Genre', 'Fantasy & Sci Fi', 'Western'],
			['2010', 10, 24],
			['2020', 16, 22],
			['2030', 28, 19]
		    ]);

		    let t1 = new Date();
		    this.drawChart(chart, this.useTestData?testData:dataTable, this.chartOptions);
		} catch(err) {
		    this.handleError("Error creating chart:" + err,err);
		    return null;
		}
	    }
	    this.addEvents(chart);
	    return chart;
	},
	addEvents: function(chart) {
            let _this = this;
	    if(this.getProperty("propagateHighlightEvent")) {
		google.visualization.events.addListener(chart, 'onmouseover', function(event) {
                    pointData = _this.dataCollection.getList()[0];
                    let fields = pointData.getRecordFields();
                    let records = pointData.getRecords();
	            let record = records[event.row];
		    if(!record) return;
		    _this.getDisplayManager().notifyEvent(DisplayEvent.recordHighlight, _this, {highlight:true,record: record});
		});
	    }
            google.visualization.events.addListener(chart, 'select', function(event) {
                _this.mapCharts(chart=>{
		    //		    chart.setSelection([]);
		    if (chart.getSelection) {
			let selected = chart.getSelection();
			if (selected && selected.length > 0) {
                            let index = selected[0].row;
			    let record = _this.indexToRecord[index];
			    if(record) {
				let records = _this.getBinnedRecords(record);
				if(records) {
				    if(records.length==1)  {
					_this.propagateEventRecordSelection({record: records[0]});
				    } else {
					_this.propagateEventRecordSelection({record: records[0],
									     records:records});
				    }
				} else {
				    _this.propagateEventRecordSelection({record: record});
				}
			    }
			}
		    }});
            });
	}


    });
}



function RamaddaAxisChart(displayManager, id, chartType, properties) {
    let SUPER = new RamaddaGoogleChart(displayManager, id, chartType, properties);
    let myProps = [
	{label:'Chart Properties'},
	{p:'indexField',ex:'field',tt:'alternate field to use as index'},
 	{p:'indexIsString',ex:'true',tt:'if index is a string set to true'},
 	{p:'useStringInLegend',ex:'true',tt:'if index is a string then add the string value to the data'},	
	{p:'vAxisMinValue',ex:''},
	{p:'vAxisMaxValue',ex:''},
	{p:'vAxisExplicit',ex:true,tt:'use the min/max values explicitly'},	
	{p:'&lt;field&gt;.vAxisMinValue',ex:'',tt:'Min value for the field'},
	{p:'&lt;field&gt;.vAxisMaxValue',ex:'',tt:'Max value for the field'},	
	{p:'vAxisSharedRange',ex:'true',tt:'use the same max value across all time series'},
	{p:'vAxisReverse',ex:'true',tt:'Reverse the v axis'},
	{p:"hAxisFixedRange"},
	{p:"vAxisSelectedFields",ex:'true',tt:'Use selected fields to find min/max for the range'},
	{p:"vAxisAllFields",ex:'true',tt:'Use all field values to find min/max for the range'},
	{p:'vAxisLogScale',ex:'true'},
	{p:'hAxisLogScale',ex:'true'},
	{p:'tooltipFields',ex:''},
 	{p:'dateType',ex:'datetime'},
 	{p:'addTooltip',ex:'false',tt:'Set this to false for multi-series charts if you only want the hovered series to show in the tt'},
	{inlineLabel:'Multiples Charts'},
	{p:'doMultiCharts',ex:'true'},
	{p:'multiField',ex:'field'},
	{p:'multiStyle',ex:''},
	{p:'multiLabelTemplate',ex:'${value}'},
	{p:'multiChartsLabelPosition',ex:'bottom|top|none'},
	{inlineLabel:'Chart Layout'},
	{p:'lineColor',ex:''},
	{p:'chartBackground',ex:''},
	{p:'chart.fill',ex:''},
	{p:'chartArea.fill',ex:''},
	{p:'chart.stroke',ex:''},
	{p:'chart.strokeWidth',ex:''},
	{p:'chartArea.fill',ex:''},
	{p:'chartArea.stroke',ex:''},
	{p:'chartArea.strokeWidth',ex:''},
	{p:'gridlines.color',ex:'transparent'},
	{p:'minorGridLines.color',ex:'transparent'},
	{p:'gridlines.color',ex:''},
	{p:'hAxis.gridlines.color',ex:''},
	{p:'hAxis.minorGridlines.color',ex:'transparent'},
	{p:'baselineColor',ex:''},
	{p:'hAxis.baselineColor',ex:''},
	{p:'gridlines.color',ex:''},
	{p:'vAxis.gridlines.color',ex:''},
	{p:'vAxis.minorGridlines.color',ex:'transparent'},
	{p:'baselineColor',ex:''},
	{p:'vAxis.baselineColor',ex:''},
	{p:'textColor',ex:'#000'},
	{p:'textBold',ex:'true'},

	{p:'axis.text.color',ex:'#000'},
	{p:'hAxis.text.color',ex:'#000'},
	{p:'vAxis.text.color',ex:'#000'},
	{p:'&lt;field&gt;.vAxis.text.color',ex:'#000'},

	{p:'hAxis.text.fontSize',ex:'16'},
	{p:'vAxis.text.fontSize',ex:'16'},
	{p:'&lt;field&gt;.vAxis.text.fontSize',ex:'16'},

	{p:'hAxis.text.fontName',ex:'Times'},
	{p:'vAxis.text.fontName',ex:'Times'},
	{p:'&lt;field&gt;.vAxis.text.fontName',ex:'Times'},		


	{p:'hAxis.text.bold',ex:'true'},
	{p:'vAxis.text.bold',ex:'true'},
	{p:'&lt;field&gt;.vAxis.text.bold',ex:'true'},	

	{p:'hAxis.text.italic',ex:'true'},
	{p:'vAxis.text.italic',ex:'true'},
	{p:'&lt;field&gt;.vAxis.text.italic',ex:'true'},	


	{p:'vAxisText',ex:''},
	{p:'vAxis.text',ex:''},

	{p:'slantedText',ex:'true'},
	{p:'hAxis.slantedText',ex:''},
	{p:'hAxis.text.color',ex:'#000'},
	{p:'vAxis.text.color',ex:'#000'},

	{p:'&lt;field&gt;.vAxis.text.color',ex:'#000'},



	{p:'legend.position',ex:'top|bottom|none'},
	{p:'legend.text.color',ex:'#000'},
	{p:'hAxis.ticks',tt:'Comma separated list of tick marks',ex:''},
	{p:'hAxis.tickTemplate',ex:'${value}'},
	{p:'vAxis.ticks',tt:'Comma separated list of tick marks',ex:''},
	{p:'vAxis.tickTemplate',ex:'${value}'},	

	{p:'useMultipleAxes',ex:'true'},
    ];

    defineDisplay(this, SUPER, myProps, {

	setChartArea: function(chartOptions) {
            if (!chartOptions.chartArea) {
                chartOptions.chartArea = {};
            }
	    $.extend(chartOptions.chartArea, {
                left: this.getChartLeft(this.chartDimensions.left),
                right: this.getChartRight(this.chartDimensions.right),
                top: this.getChartTop(),
		bottom: this.getChartBottom(50),
                height: this.getChartHeight('70%'),
                width: this.getChartWidth(this.chartDimensions.width),
            });
	    ["left","top","right","bottom"].forEach(a=>{
		let v =chartOptions.chartArea[a];
		if(v) v = String(v).replace("px","");
		chartOptions.chartArea[a] = v;
	    });						    
	},

        makeChartOptions: function(dataList, props, selectedFields) {
            chartOptions = SUPER.makeChartOptions.call(this, dataList, props, selectedFields);

	    let dataFields = dataList[0].fields;


	    let expandedHeight  = this.getProperty("expandedHeight");
            chartOptions.height = expandedHeight || this.getProperty("chartHeight", this.getProperty("height", "150"));

            if (!chartOptions.legend)
                chartOptions.legend = {};


	    this.setPropertyOn(chartOptions.legend, "legend.position", "position", this.getProperty("legendPosition", 'bottom'));
	    this.setChartArea(chartOptions);
	    
            let useMultipleAxes = this.getProperty("useMultipleAxes", true);
            if (useMultipleAxes) {
		//TODO: 
                chartOptions.series = [
		    {
			targetAxisIndex: 0
                    }, {
			targetAxisIndex: 1
                    }];
            }

	    if(chartOptions.legend.position=="left") {
                chartOptions.series = [
		    {
			targetAxisIndex: 1
		    }]
	    }



	    if (!chartOptions.hAxis) {
		chartOptions.hAxis = {};
	    }
	    if (!chartOptions.vAxis) {
		chartOptions.vAxis = {};
	    }
	    chartOptions.hAxis.textPosition = this.getProperty("hAxisTextPosition","top");
	    chartOptions.vAxis.textPosition = this.getProperty("vAxisTextPosition");


            if (this.getProperty("hAxisTitle")) {
                chartOptions.hAxis.title = this.getProperty("hAxisTitle");
            }
            if (this.getProperty("vAxisTitle")) {
                chartOptions.vAxis.title = this.getProperty("vAxisTitle");
		if(chartOptions.vAxis.title && dataFields) {

		    let label = dataFields.reduce((acc,v)=>{
			return acc+" " + v.getLabel(this);
		    },"");
		    chartOptions.vAxis.title = chartOptions.vAxis.title.replace("${fields}",label);
		}

            }
	    //	    console.log(JSON.stringify(chartOptions,null,2));

            if (Utils.isDefined(this.chartHeight)) {
                chartOptions.height = this.chartHeight;
            }

            return chartOptions;

        }
    });

}



function RamaddaSeriesChart(displayManager, id, chartType, properties) {
    const SUPER = new RamaddaAxisChart(displayManager, id, chartType, properties);
    defineDisplay(this, SUPER, [], {
        includeIndexInData: function() {
            return this.getProperty("includeIndex", true);
        },
        trendLineEnabled: function() {
            return true;
        },
    });
}


function BlankchartDisplay(displayManager, id, properties) {
    const SUPER = new RamaddaSeriesChart(displayManager, id, "blankchart", properties);
    defineDisplay(addRamaddaDisplay(this), SUPER, [], {
        doMakeGoogleChart: function(dataList, props, chartDiv, selectedFields, chartOptions) {
            return null;
        },
    });
}


function LinechartDisplay(displayManager, id, properties) {
    const SUPER =  new RamaddaSeriesChart(displayManager, id, DISPLAY_LINECHART, properties);
    defineDisplay(addRamaddaDisplay(this), SUPER, [], {
        doMakeGoogleChart: function(dataList, props, chartDiv, selectedFields, chartOptions) {
            return new google.visualization.LineChart(chartDiv);
        },
    });
}




function AreachartDisplay(displayManager, id, properties) {
    const SUPER = new RamaddaSeriesChart(displayManager, id, DISPLAY_AREACHART, properties);
    let myProps = [
	{label:'Area Chart Properties'},
	{p:'isStacked',ex:'true'}
    ];
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
        doMakeGoogleChart: function(dataList, props, chartDiv,  selectedFields, chartOptions) {
            if (this.isStacked)
                chartOptions.isStacked = true;
            return new google.visualization.AreaChart(chartDiv);
        }
    });
}


function RamaddaBaseBarchart(displayManager, id, type, properties) {
    const SUPER  = new RamaddaSeriesChart(displayManager, id, type, properties);
    let myProps = [
    ];
    defineDisplay(this, SUPER, myProps, {
        canDoGroupBy: function() {
            return true;
        },
        makeChartOptions: function(dataList, props, selectedFields) {
            chartOptions = SUPER.makeChartOptions.call(this, dataList, props, selectedFields);
            let chartType = this.getChartType();
            if (chartType == DISPLAY_BARSTACK) {
		chartOptions.series = null;
                chartOptions.isStacked = true;
            }
            if (this.getProperty("barWidth")) {
		let w = this.getProperty("barWidth");
		if(w=="flex") {
		    if(dataList.length<100) {
			w = "10";
		    } else {
			w = null;
		    }
		}
		if(w) {
                    chartOptions.bar = {
			groupWidth: w
                    }
		}
	    }
	    chartOptions.orientation = this.getProperty("orientation","horizontal");
	    return chartOptions;
	},

        doMakeGoogleChart: function(dataList, props, chartDiv,  selectedFields, chartOptions) {
            return new google.visualization.BarChart(chartDiv);
	    //            return new google.charts.Bar(chartDiv);	    
        }
    });
}


function BarchartDisplay(displayManager, id, properties) {
    const SUPER =  new RamaddaBaseBarchart(displayManager, id, DISPLAY_BARCHART, properties);
    defineDisplay(addRamaddaDisplay(this), SUPER, [], {});
}

function BarstackDisplay(displayManager, id, properties) {
    properties = $.extend({
        "isStacked": true
    }, properties);
    const SUPER =  new RamaddaBaseBarchart(displayManager, id, DISPLAY_BARSTACK, properties);
    defineDisplay(addRamaddaDisplay(this), SUPER, [], {});
}


function HistogramDisplay(displayManager, id, properties) {
    const SUPER =  new RamaddaGoogleChart(displayManager, id, DISPLAY_HISTOGRAM, properties);
    let myProps = [
	{label:'Histogram Properties'},
	{p:'legendPosition',ex:'none|top|right|left|bottom'},
	{p:'textPosition',ex:'out|in|none'},
	{p:'isStacked',ex:'false|true|percent|relative'},
	{p:'logScale',ex:'true|false'},
	{p:'scaleType',ex:'log|mirrorLog'},
	{p:'minValue',ex:''},
	{p:'maxValue',ex:''}
    ];
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
        okToHandleEventRecordSelection: function() {
            return false;
        },
        makeDataTable: function(dataList, props, selectedFields) {
            return google.visualization.arrayToDataTable(this.makeDataArray(dataList));
        },
        doMakeGoogleChart: function(dataList, props, chartDiv,  selectedFields, chartOptions) {
            //            chartOptions = {};
            if (this.legendPosition) {
                if (!chartOptions.legend)
                    chartOptions.legend = {};
                chartOptions.legend.position = this.legendPosition;
            }
            let isStacked = this.getProperty("isStacked", null);
            if (isStacked)
                chartOptions.isStacked = isStacked == "true" ? true : isStacked == "false" ? false : isStacked;
            chartOptions.vAxis = {};
            chartOptions.vAxis.viewWindow = {};
            if (Utils.isDefined(this.logScale)) {
                chartOptions.vAxis.logScale = ("" + this.logScale) == true;
            }
            if (this.textPosition) {
                chartOptions.vAxis.textPosition = this.textPosition;
            }


            if (Utils.isDefined(this.minValue)) {
                chartOptions.vAxis.viewWindow.min = parseFloat(this.minValue);
            }
            if (Utils.isDefined(this.maxValue)) {
                chartOptions.vAxis.viewWindow.max = parseFloat(this.maxValue);
            }
            if (!isNaN(this.getVAxisMaxValue())) {
                chartOptions.vAxis.maxValue = this.getVAxisMaxValue();
            }
            if (!isNaN(this.getVAxisMinValue())) {
                chartOptions.vAxis.minValue = parseFloat(this.getVAxisMinValue());
            }
            if (!isNaN(this.getHAxisMaxValue())) {
                chartOptions.hAxis.maxValue = this.getHAxisMaxValue();
            }
            if (!isNaN(this.getHAxisMinValue())) {
                chartOptions.hAxis.minValue = parseFloat(this.getHAxisMinValue());
            }	    
            return new google.visualization.Histogram(chartDiv);
        },

    });
}


function RamaddaTextChart(displayManager, id, chartType, properties) {
    const SUPER = new RamaddaGoogleChart(displayManager, id, chartType, properties);
    defineDisplay(this, SUPER, [], {
        getFieldsToSelect: function(pointData) {
            return pointData.getNonGeoFields();
        },
    });
}




function PiechartDisplay(displayManager, id, properties) {
    let ID_PIE_LEGEND = "pielegend";
    const SUPER = new RamaddaTextChart(displayManager, id, DISPLAY_PIECHART, properties);
    let myProps = [
	{label:'Pie Chart Properties'},
	{p:'groupBy',ex:''},
	{p:'groupByCount',ex:'true'},
	{p:'groupByCountLabel',ex:''},
	{p:'showTopLegend'},
	{p:'binCount',ex:'true'},
	{p:'pieHole',ex:'0.5'},
	{p:'is3D',ex:'true'},
	{p:'bins',ex:''},
	{p:'binMin',ex:''},
	{p:'binMax',ex:'max'},
	{p:'sumFields',ex:'true'},
	{p:'sliceVisibilityThreshold',ex:'0.01'},
	{p:'pieSliceTextColor',ex:'black'},
	{p:'pieSliceBorderColor',d:'white',ex:'black'}
    ];

    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {

	uniqueValues:[],
	uniqueValuesMap:{},
        canDoGroupBy: function() {
            return true;
        },
	makeGoogleChart: function(dataList, props, selectedFields) {
	    this.uniqueValues = [];
	    this.uniqueValuesMap = {};
	    SUPER.makeGoogleChart.call(this, dataList, props, selectedFields);
	    if(!this.getShowTopLegend()) return;
	    let legend = "";
	    let colors = this.getColorList();
	    let colorCnt = 0;
	    this.uniqueValues.map((v,idx)=>{
		if(colorCnt>=colors.length) colorCnt = 0;
		let color  = colors[colorCnt];
		legend += HU.div([STYLE,HU.css('display','inline-block','width','8px','height','8px','background', color)]) +SPACE + v +SPACE2;
		colorCnt++;
	    });
	    if(this.jq(ID_PIE_LEGEND).length==0) {
		this.jq(ID_HEADER2).append(HU.div([ID,this.domId(ID_PIE_LEGEND)]));
	    }
	    this.jq(ID_PIE_LEGEND).html(legend);

	},
        setChartSelection: function(index) {
	    //noop
	},
	//Override these methods since the pie chart can't use the explorer
	getDragToZoom:function(){return false;},
	getDragToPan:function(){return false;},	
        getGroupBy: function() {
            if (!this.groupBy && this.groupBy != "") {
                let stringField = this.getFieldByType(this.getFields(), "string");
                if (stringField) {
                    this.groupBy = stringField.getId();
                }
            }
            return this.groupBy;
        },
        getChartDiv: function(chartId) {
            let divAttrs = [ATTR_ID, chartId];
            divAttrs.push(STYLE);
            let style = "";
	    let width = this.getProperty("chartWidth") || this.getChartWidth();
	    let height = this.getProperty("chartHeight") || this.getChartHeight();
            if (width) {
                if (width > 0)
                    style += "width:" + width + "px;";
                else if (width < 0)
                    style += "width:" + (-width) + "%;";
                else
                    style += "width:" + width + ";";
            } else {
                style += "width:" + "100%;";
            }
            if (height) {
                if (height > 0)
                    style += "height:" + height + "px;";
                else if (height < 0)
                    style += "height:" + (-height) + "%;";
                else
                    style += "height:" + height + ";";
            } else {
                style += "height:" + "100%;";
            }
	    //	    style += "border:1px solid green;"
            divAttrs.push(style);
            return HU.div(divAttrs, "");
        },
        doMakeGoogleChart: function(dataList, props, chartDiv,  selectedFields, chartOptions) {
            chartOptions.tooltip = {
                textStyle: {
                    color: '#000000',
		    fontSize:12,
                },
                showColorCode: true,
		//		isHtml: true,
		//		ignoreBounds: true,
            };
	    this.chartOptions.legend = {'position':this.getProperty("legendPosition", 'right'),'alignment':'center'};
            if (this.getBins()) {
                chartOptions.title = "Bins: " + this.getDataValues(dataList[0])[1];
	    } else if(this.getSumFields()) {
                chartOptions.title = this.getProperty("chartTitle","Categories/Values");
            } else {
                chartOptions.title = this.getDataValues(dataList[0])[0] + " - " + this.getDataValues(dataList[0])[1];
            }

            if (this.getIs3D()) {
                chartOptions.is3D = true;
            }
            if (this.getPieHole()) {
                chartOptions.pieHole = this.pieHole;
            }
            if (this.getSliceVisibilityThreshold()) {
                chartOptions.sliceVisibilityThreshold = this.sliceVisibilityThreshold;
            }

	    chartOptions.pieSliceBorderColor = this.getPieSliceBorderColor();
	    chartOptions.pieSliceTextStyle  = {
		color: this.getPieSliceTextColor()
            };

	    chartOptions.chartArea = {};
	    $.extend(chartOptions.chartArea, {
                left: this.getChartLeft(),
                right: this.getChartRight(),
                top: this.getChartTop(),
		bottom: this.getChartBottom(),
                width: '100%',
                height: '100%'
            });

            return new google.visualization.PieChart(chartDiv);
        },
	getColorList:function() {
	    if (this.getProperty("colors") && this.getProperty("colors")!="default") {
		return SUPER.getColorList.call(this);
	    }
	    if (this.getProperty("colorTable")) {
		let ct =this.getColorTable();
		return ct.colors;
	    }	    
	    return Utils.mergeLists(Utils.getColorTable("schemeset1",true),
				    Utils.getColorTable("schemecategory",true));
	},

        makeDataTable: function(dataList, props, selectedFields) {
            let dataTable = new google.visualization.DataTable();
            let list = [];
            let header = this.getDataValues(dataList[0]);
            dataTable.addColumn("string", header[0]);
            dataTable.addColumn("number", header[1]);


            if (this.getBins()) {
                let bins = parseInt(this.getProperty("bins", null));
                let min = Number.MAX_VALUE;
                let max = Number.MIN_VALUE;
                let haveMin = false;
                let haveMax = false;
                if (this.getBinMin()) {
                    min = parseFloat(this.getBinMin());
                    haveMin = true;
                }
                if (this.getBinMax()) {
                    max = parseFloat(this.getBinMax());
                    haveMax = true;
                }

                let goodValues = [];
                for (let i = 1; i < dataList.length; i++) {
                    let tuple = this.getDataValues(dataList[i]);
                    let value = tuple[1];
                    if (!Utils.isRealNumber(value)) {
                        continue;
                    }
                    if (!haveMin)
                        min = Math.min(value, min);
                    if (!haveMax)
                        max = Math.max(value, max);
                    goodValues.push(value);
                }

                let binList = [];
                let step = (max - min) / bins;
                for (let binIdx = 0; binIdx < bins; binIdx++) {
                    binList.push({
                        min: min + binIdx * step,
                        max: min + (binIdx + 1) * step,
                        values: []
                    });
                }

                for (let rowIdx = 0; rowIdx < goodValues.length; rowIdx++) {
                    let value = goodValues[rowIdx];
                    let ok = false;

                    for (let binIdx = 0; binIdx < binList.length; binIdx++) {
                        if (value < binList[binIdx].min || (value >= binList[binIdx].min && value <= binList[binIdx].max)) {
                            binList[binIdx].values.push(value);
                            ok = true;
                            break;
                        }
                    }
                    if (!ok) {
                        binList[binList.length - 1].values.push(value);
                    }
                }
                for (let binIdx = 0; binIdx < bins; binIdx++) {
                    let bin = binList[binIdx];
                    list.push(["Bin:" + this.formatNumber(bin.min) + "-" + this.formatNumber(bin.max),
                               bin.values.length
			      ]);
                }
            } else if(this.getSumFields()) {
		dataTable = new google.visualization.DataTable();
		dataTable.addColumn("string", "Category");
		dataTable.addColumn("number", "Value");
		let records=  this.filterData();
		let sumFields =  this.getFieldsByIds(null, this.getSumFields());
		let sums = [];
		sumFields.map(f=>{sums.push(0)});
		if(this.chartCount>=0) {
		    records = [records[this.chartCount]];
		}
                records.map(record=>{
		    sumFields.map((f,idx)=>{
			let v = record.getValue(f.getIndex());
			if(!isNaN(v))  sums[idx]+=v;
		    });
		});
		sumFields.map((f,idx)=>{
                    list.push([f.getLabel(this),sums[idx]>0?sums[idx]:0]);
		});

            } else {
                for (let i = 1; i < dataList.length; i++) {
                    let tuple = this.getDataValues(dataList[i]);
                    let s = "" + (tuple.length == 1 ? "#" + i : tuple[0]);
                    let v = tuple.length == 1 ? tuple[0] : tuple[1];
                    list.push([s, v]);
                }
            }
	    list.map(tuple=>{
		let s = tuple[0];
		if(!this.uniqueValuesMap[s]) {
		    this.uniqueValuesMap[s] = true;
		    this.uniqueValues.push(s);
		}
	    });
            dataTable.addRows(list);
            return dataTable;
        }
    });


}


//TODO: this is broken because we don't load the sankey package because it loads an old version of d3


function SankeyDisplay(displayManager, id, properties) {
    this.tries = 0;
    const SUPER = new RamaddaTextChart(displayManager, id, DISPLAY_SANKEY, properties);
    defineDisplay(addRamaddaDisplay(this), SUPER, [], {
	getRequiredPackages: function() {
	    return ['sankey'];
	},
        doMakeGoogleChart: function(dataList, props, chartDiv,  selectedFields, chartOptions) {
            chartOptions.height = parseInt(this.getProperty("chartHeight", this.getProperty("height", "400")));
            chartOptions.sankey = {
                node: {
                    colors: this.colors,
                    width: 5,
                },
                link: {
                    colorMode: 'source',
                    colors: this.colors,
                    color: {
                        //                                stroke:'black',
                        //strokeWidth:1,
                    }
                }
            }
	    return new google.visualization.Sankey(chartDiv);
        },
        defaultSelectedToAll: function() {
            return true;
        },
        makeDataTable: function(dataList, props, selectedFields) {
            if (!this.getProperty("doCategories", false)) {
                let values = this.makeDataArray(dataList);
                return google.visualization.arrayToDataTable(values);
            }
            let strings = [];
            for (let i = 0; i < selectedFields.length; i++) {
                let field = selectedFields[i];
                if (field.isFieldString()) {
                    strings.push(field);
                }
            }
            let values = [];
            values.push(["characteristic 1", "characteristic 2", "value"]);
            for (let i = 1; i < strings.length; i++) {
                let field1 = strings[i - 1];
                let field2 = strings[i];
                let cnts = {};
                for (let r = 1; r < dataList.length; r++) {
                    let row = this.getDataValues(dataList[r]);
                    let value1 = row[i - 1];
                    let value2 = row[i];
                    let key = value1 + "-" + value2;
                    if (!cnts[key]) {
                        cnts[key] = {
                            v1: value1,
                            v2: value2,
                            cnt: 0
                        }
                    }
                    cnts[key].cnt++;
                }
                for (a in cnts) {
                    values.push([cnts[a].v1, cnts[a].v2, cnts[a].cnt]);
                }
            }
            return google.visualization.arrayToDataTable(values);
        }
    });
}

function WordtreeDisplay(displayManager, id, properties) {
    const SUPER = new RamaddaTextChart(displayManager, id, DISPLAY_WORDTREE, properties);
    let myProps = [
	{label:"Word Tree"},
	{p:"treeRoot",d:"root", tt:"Word to use for root"},
	{p:"wordColors",ex:"red,green,blue",tt:"Colors to use for tree levels"},
	{p:"fixedSize",d:"false",tt:""},
	{p:"buckets",ex:"100,110,115,120,130",tt:"For numeric fields the buckets to put the records in"},	
	{p:"&lt;field&gt;.buckets",ex:"100,110,115,120,130",tt:"Specify buckets for a particular field"},
	{p:"bucketLabels",ex:"young,middle,old,really_old",tt:"For numeric fields the labels used for the buckets"},	
	{p:"&lt;field&gt;.bucketLabels",ex:"young,middle,old,really_old",tt:"Specify bucket labels for a particular field"},			
    ];
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
        handleEventRecordSelection: function(source, args) {},
	getRequiredPackages: function() {
	    return ['wordtree'];
	},
        doMakeGoogleChart: function(dataList, props, chartDiv, selectedFields, chartOptions) {
            if (this.getProperty("chartHeight"))
                chartOptions.height = parseInt(this.getProperty("chartHeight"));
            if (this.getWordColors()) {
                let tmp = this.getWordColors().split(",");
                let colors = [];
                for (let i = 0; i < 3 && i < tmp.length; i++) {
                    colors.push(tmp[i]);
                }
                if (colors.length == 3)
                    chartOptions.colors = colors;
            }

            if (this.getProperty("chartWidth")) {
                chartOptions.width = parseInt(this.getProperty("chartWidth"));
            }

            chartOptions.wordtree = {
                format: 'implicit',
                wordSeparator: "_SEP_",
                word: this.getTreeRoot(),
                //                    type: this.getProperty("treeType","double")

            }
            if (this.getProperty("maxFontSize")) {
                chartOptions.maxFontSize = parseInt(this.getProperty("maxFontSize"));
            }

            return new google.visualization.WordTree(chartDiv); 
        },


        makeDataTable: function(dataList, props, selectedFields) {
            //null ->get all data
            let root = this.getTreeRoot();
            let records = this.filterData(null, selectedFields, {skipFirst:true});
            let fields = this.getSelectedFields(this.getData().getRecordFields());
            let valueField = this.getFieldById(null, this.getProperty("colorBy"));
            let values = [];
            let typeTuple = ["phrases"];
            values.push(typeTuple);
            let fixedSize = this.getFixedSize();
            if (valueField)
                fixedSize = 1;
            if (fixedSize) typeTuple.push("size");
            if (valueField)
                typeTuple.push("value");
            let fieldInfo = {};

            let header = "";
            for (let i = 0; i < fields.length; i++) {
                let field = fields[i];
                if (header != "")
                    header += " -&gt;";
                header += field.getLabel(this);
                if (!field.isFieldNumeric()) continue;
                let column = this.getColumnValues(records, field);
                let buckets = [];
                let argBuckets = this.getProperty("buckets." + field.getId(), this.getProperty("buckets", null));
                let min, max;
                if (argBuckets) {
                    let argBucketLabels = this.getProperty("bucketLabels." + field.getId(), this.getProperty("bucketLabels", null));
                    let bucketLabels;
                    if (argBucketLabels)
                        bucketLabels = argBucketLabels.split(",");
                    let bucketList = argBuckets.split(",");
                    let prevValue = 0;
                    for (let bucketIdx = 0; bucketIdx < bucketList.length; bucketIdx++) {
                        let v = parseFloat(bucketList[bucketIdx]);
                        if (bucketIdx == 0) {
                            min = v;
                            max = v;
                        }
                        min = Math.min(min, v);
                        max = Math.max(max, v);
                        if (bucketIdx > 0) {
                            let label;
                            if (bucketLabels && i <= bucketLabels.length)
                                label = bucketLabels[bucketIdx - 1];
                            else
                                label =this.formatNumber(prevValue) + "-" + this.formatNumber(v);
                            buckets.push({
                                min: prevValue,
                                max: v,
                                label: label
                            });
                        }
                        prevValue = v;
                    }
                } else {
                    let numBuckets = parseInt(this.getProperty("numBuckets." + field.getId(), this.getProperty("numBuckets", 10)));
                    min = column.min;
                    max = column.max;
                    let step = (column.max - column.min) / numBuckets;
                    for (let bucketIdx = 0; bucketIdx < numBuckets; bucketIdx++) {
                        let r1 = column.min + (bucketIdx * step);
                        let r2 = column.min + ((bucketIdx + 1) * step);
			let label = this.formatNumber(r1) + "-" + this.formatNumber(r2);
                        buckets.push({
                            min: r1,
                            max: r2,
                            label: label
                        });
                    }
                }
                fieldInfo[field.getId()] = {
                    min: min,
                    max: max,
                    buckets: buckets
                };
            }

            let sep = "_SEP_";
            for (let rowIdx = 0; rowIdx < records.length; rowIdx++) {
                let row = this.getDataValues(records[rowIdx]);
                let string = root;
                for (let fieldIdx = 0; fieldIdx < fields.length; fieldIdx++) {
                    let field = fields[fieldIdx];
                    string += sep;
                    let value = row[field.getIndex()];
                    if (field.isFieldNumeric()) {
                        let info = fieldInfo[field.getId()];
                        for (let bucketIdx = 0; bucketIdx < info.buckets.length; bucketIdx++) {
                            let bucket = info.buckets[bucketIdx];
                            if (value >= bucket.min && value <= bucket.max) {
                                value = bucket.label;
                                break;
                            }
                        }
                    }
                    string += value;
                }
                let data = [string.trim()];
                if (fixedSize) data.push(parseInt(fixedSize));
                if (valueField)
                    data.push(row[valueField.getIndex()]);
                values.push(data);
            }
            if (this.getProperty("header")) {
                header = this.getProperty("header", "");
            } else {
                header = "<b>Fields: </b>" + header;
                if (this.getProperty("headerPrefix"))
                    header = this.getProperty("headerPrefix") + " " + header;
            }
            this.writeHtml(ID_DISPLAY_TOP, header);
            return google.visualization.arrayToDataTable(values);
        },
    });
}



function TableDisplay(displayManager, id, properties) {
    const SUPER = new RamaddaTextChart(displayManager, id, DISPLAY_TABLE, properties);
    let myProps = [
	{label:'Table'},
	{p:'imageField',ex:''},
	{p:'tableWidth',ex:'100%'},
	{p:'frozenColumns',ex:'1'},
	{p:'colorCells',ex:'field1,field2'},
	{p:'foregroundColor'},
	{p:'showRowNumber',ex:true},
	{p:'field.colorTable',ex:''},
	{p:'field.colorByMap',ex:'value1:color1,value2:color2'},
	{p:'maxHeaderLength',ex:'60'},
	{p:'maxHeaderWidth',ex:'60'},
	{p:'headerStyle'}];

    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
	getRequiredPackages: function() {
	    return ['table'];
	},
        canDoGroupBy: function() {
            return true;
        },
        defaultSelectedToAll: function() {
            return true;
        },
        getDataTableValueGetter: function(records) {
	    let unhighlightColor = this.getProperty("unhighlightColor","#fff");
	    let highlightColor = this.getProperty("highlightColor","#FFFFCC");
	    let colorCells = this.getProperty("colorCells");
	    let colorByMap = {};
	    let linkField = this.getFieldById(null,this.getProperty("linkField"));
	    let iconField = this.getFieldById(null,this.getProperty("iconField"));
	    let foreground = this.getProperty("foregroundColor");
	    let cbs = [];
	    if(colorCells) {
		colorCells.split(",").forEach(c=>{
		    let f = this.getFieldById(null,c);
		    if(f) {
			colorByMap[c] = new ColorByInfo(this, null, records, null,c+".colorByMap",null, c, f);
			cbs.push(colorByMap[c]);
		    }
		});
	    }

	    //Show the bars
	    let dom = this.jq(ID_COLORTABLE);
	    cbs.forEach((cb,idx)=>{
		let id = this.domId(ID_COLORTABLE+idx);
		dom.append(HU.div([ID,id]));
		cb.displayColorTable(null,true,ID_COLORTABLE+idx);
	    });


	    return  (v,idx, field, record)=>{
		if(v===null) {
		    return {
			v:0,
			f:""
		    }
		}
		let f = v;
		if(v && v.f) {
		    f = v.f;
		    v = v.v;
		}
		if(v && v.getTime) {
		    f = this.formatDate(v);
		}
		if(iconField && record && idx==0) {
		    let icon = record.getValue(iconField.getIndex());
		    f = HU.image(icon) +"&nbsp;" +f;
		}
		if(linkField && record&& idx==0) {
		    let url = record.getValue(linkField.getIndex());
		    if(f) f = f.trim();
		    if (Utils.isDefined(f) && f!="") {
			f = HU.href(url,f);
		    }
		}

		if(!this.getFilterHighlight() || !record) {
		    f = HU.div([STYLE,HU.css('padding','4px')],f)
		} else {
		    let c = record.isHighlight(this) ? highlightColor: unhighlightColor;
		    f = HU.div([STYLE,HU.css('padding','4px','background', c)],f)
		}


		if(field) {
		    let colorBy = colorByMap[field.getId()];
		    if(colorBy && record) {
			let color =  colorBy.getColorFromRecord(record);
			let fg = foreground || Utils.getForegroundColor(color);
			f = HU.div([STYLE,HU.css('height','100%','background', color,'color',fg+" !important")],f)
		    }
		    if(field.getType()=="url") {
			return {
			    v:v,
			    f:HU.href(v,v,['target','_link'])
			};
		    }
		    if(field.getType()=="image") {
			return {
			    v:v,
			    f:HU.href(v,HU.image(v,[WIDTH,this.getProperty("imageWidth",100)]))
			};
		    }		    
		}

		return {
		    v:v,
		    f:f
		};
	    }
	},
        doMakeGoogleChart: function(dataList, props, chartDiv, selectedFields, chartOptions) {
	    let expandedHeight  = this.getProperty("expandedHeight");
	    if(!expandedHeight) {
		chartOptions.height = null;
		if (this.chartHeight) {
                    chartOptions.height = this.chartHeight;
		}
		if (chartOptions.height == null) {
                    let height = this.getProperty("height", null);
                    if (height) {
			chartOptions.height = height;
                    }
		}
		if (chartOptions.height == null) {
                    chartOptions.height = "300px";
		}
	    }


	    if(this.debugChartOptions)
		console.log(JSON.stringify(chartOptions,null,2));
            chartOptions.allowHtml = true;
	    if(this.getProperty("tableWidth"))
		chartOptions.width=this.getProperty("tableWidth");
            chartOptions.frozenColumns =this.getProperty("frozenColumns",0);
	    chartOptions.showRowNumber=this.getProperty("showRowNumber",false);

            if (dataList.length && this.getDataValues(dataList[0]).length > 4) {
                chartOptions.cssClassNames = {
                    headerCell: 'display-table-header-max'
                };
            } else {
                chartOptions.cssClassNames = {
                    headerCell: 'display-table-header'
                };
            }
	    if(!chartOptions.cssClassNames)
		chartOptions.cssClassNames = {};

	    if(this.getProperty("fixCellHeight",true)) {
		chartOptions.cssClassNames.headerCell= 'display-table-cell';
		chartOptions.cssClassNames.tableCell= 'display-table-cell';
	    }
            return new google.visualization.Table(chartDiv); 
        },
	doAddTooltip: function() {
	    return false;
	},
	getAddStyle: function() {
	    return false;
	},
	getFormatNumbers: function() {
	    return true;
	},
        setChartSelection: function(index) {
	    SUPER.setChartSelection.call(this,index);
	    var container = this.jq(ID_CHART).find('.google-visualization-table-table:eq(0)').parent();
	    var row = this.jq(ID_CHART).find('.google-visualization-table-tr-sel');
	    //	    var header = this.jq(ID_CHART).find('.google-visualization-table-table:eq(1)').parent();

	    //The header selector doesn't work so for now just offset by 60
	    //$(container).prop('scrollTop', $(row).prop('offsetTop') - $(header).height());
	    $(container).prop('scrollTop', $(row).prop('offsetTop')-60);
	},
        xxxxmakeDataTable: function(dataList, props, selectedFields) {
            let rows = this.makeDataArray(dataList);
            let data = [];
            for (let rowIdx = 0; rowIdx < rows.length; rowIdx++) {
                let row = rows[rowIdx];
                for (let colIdx = 0; colIdx < row.length; colIdx++) {
		    let t = (typeof row[colIdx]);
                    if (t == "string") {
                        row[colIdx] = row[colIdx].replace(/\n/g, "<br>");
			if(row[colIdx].startsWith("http:") || row[colIdx].startsWith("https:")) {
			    row[colIdx] = "<a href='" +row[colIdx] +"'>" + row[colIdx]+"</a>";
			}
		    } else if(t == "number") {
			//This doesn't stick
			if(isNaN(row[colIdx])) 
			    row[colIdx] = "--";
		    }
                }
                data.push(row);
            }
            return google.visualization.arrayToDataTable(data);
        }
    });

}



function BubbleDisplay(displayManager, id, properties) {
    const SUPER = new RamaddaTextChart(displayManager, id, DISPLAY_BUBBLE, properties);
    let myProps = [
	{label:'Bubble Chart Attibutes'},
	{p:'xField'},
	{p:'yField'},	
	{p:'labelField'},
	{p:'colorBy'},
	{p:'sizeField'},	
	{p:'legendPosition',ex:'none|top|right|left|bottom'},
	{p:'hAxisFormat',ex:'none|decimal|scientific|percent|short|long'},
	{p:'vAxisFormat',ex:'none|decimal|scientific|percent|short|long'},
	{p:'hAxisTitle',ex:''},
	{p:'vAxisTitle',ex:''}
    ];
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
        getChartDiv: function(chartId) {
            let divAttrs = [ATTR_ID, chartId];
            divAttrs.push(STYLE);
            let style = "";
	    let width = this.getProperty("chartWidth") || this.getChartWidth();
	    let height = this.getProperty("chartHeight") || this.getChartHeight();
            if (width) {
                if (width > 0)
                    style += "width:" + width + "px;";
                else if (width < 0)
                    style += "width:" + (-width) + "%;";
                else
                    style += "width:" + width + ";";
            } else {
                style += "width:" + "100%;";
            }
            if (height) {
                if (height > 0)
                    style += "height:" + height + "px;";
                else if (height < 0)
                    style += "height:" + (-height) + "%;";
                else
                    style += "height:" + height + ";";
            } else {
                style += "height:" + "100%;";
            }
	    //	    style += "border:1px solid green;"
	    style += "padding:5px;"
            divAttrs.push(style);
            return HU.div(divAttrs, "");
        },

	getFieldsToDisplay: function(fields) {
	    if(fields.length>=4) return fields;
	    let labelField=this.getFieldById(null, this.getProperty("labelField"));
	    let colorField=this.getFieldById(null, this.getColorBy(this.getProperty("colorField")));
	    let sizeField=this.getFieldById(null, this.getProperty("sizeField"));
	    let xField=this.getFieldById(null, this.getProperty("xField"));
	    let yField=this.getFieldById(null, this.getProperty("yField"));	    	    	    	    
	    if(!labelField) throw new Error("Need to specify labelField");
	    if(!xField) throw new Error("Need to specify xField");
	    if(!yField) throw new Error("Need to specify yField");	    
	    let f = [labelField, xField, yField];
	    if(colorField) f.push(colorField);
	    if(sizeField) f.push(sizeField);
	    return f;
	},

        makeDataTable: function(dataList, props, selectedFields, chartOptions) {
	    let debug =displayDebug.makeDataTable;
	    if(debug) {
		this.logMsg(this.type+" makeDataTable #records:" + dataList.length);
                let fields = this.getSelectedFields();
		console.log("\tfields:" + fields);
	    }
	    let tmp =[];
	    let a = this.makeDataArray(dataList);
	    while(a[0].length<5)
		a[0].push("");
	    tmp.push(a[0]);
	    //Remove nans
	    this.didUnhighlight = false;
	    let minColorValue = Number.MAX_SAFE_INTEGER;
	    for(let i=1;i<a.length;i++) {
		let tuple = a[i];
		while(tuple.length<5) {
		    tuple.push(1);
		}
		minColorValue = Math.min(minColorValue, tuple[3]);
	    }


	    for(let i=1;i<a.length;i++) {
		let tuple = a[i];
		while(tuple.length<5)
		    tuple.push(1);
		if(debug && i<5)
		    console.log("\tdata:" + tuple);
		let ok = true;
		for(j=1;j<tuple.length && ok;j++) {
		    if(isNaN(tuple[j])) ok = false;
		}
		//If highlighting and have color then set to NaN
		if(this.getFilterHighlight()) {
		    let unhighlightColor = this.getProperty("unhighlightColor","#eee");
		    if(dataList[i].record && !dataList[i].record.isHighlight(this)) {
			this.didUnhighlight = true;
			tuple[3] =minColorValue-0.111;
		    }
		}
		if(ok) 
		    tmp.push(tuple);
	    }
            return google.visualization.arrayToDataTable(tmp);
        },
        doMakeGoogleChart: function(dataList, props, chartDiv, selectedFields, chartOptions) {
            let ct = this.getColorTable(true);
            if (ct) {
                chartOptions.colors = ct;
            } else if (!this.colors) {
                chartOptions.colors = this.getColorList();
            }
            if (chartOptions.colors) {
                chartOptions.colors = Utils.getColorTable("rainbow", true);
            }
	    $.extend(chartOptions.chartArea, {
                left: this.getChartLeft(this.chartDimensions.left),
                right: this.getChartRight(this.chartDimensions.right),
                top: this.getChartTop(10),
		bottom: this.getChartBottom(40),
                height: this.getChartHeight(200)
            });
            chartOptions.height = "100px";
            chartOptions.sizeAxis = {
	    }

            chartOptions.colorAxis = {
                legend: {
                    position: this.getProperty("legendPosition", "in")
                }
            }
	    let colorTable = this.getColorTable(true);
	    if(colorTable) {
		chartOptions.colorAxis.colors = colorTable;
		if(this.didUnhighlight) {
		    chartOptions.colorAxis.colors = [...chartOptions.colorAxis.colors];
		    chartOptions.colorAxis.colors.unshift(this.getProperty("unhighlightColor","#eee"));
		}
	    }

            chartOptions.bubble = {
                textStyle: {
                    auraColor: "none"
                },
                stroke: "#666"
            };


            header = this.getDataValues(dataList[0]);
	    chartOptions.hAxis = chartOptions.hAxis||{};
            chartOptions.vAxis = chartOptions.vAxis||{};

	    chartOptions.hAxis.minValue = this.getProperty("hAxisMinValue");
	    chartOptions.hAxis.maxValue = this.getProperty("hAxisMaxValue");
	    chartOptions.vAxis.minValue = this.getProperty("vAxisMinValue");
	    chartOptions.vAxis.maxValue = this.getProperty("vAxisMaxValue");


            let records = this.getPointData().getRecords();
	    if(this.getProperty("hAxisFixedRange")) {
		let x = this.getColumnValues(records, selectedFields[1]);
		chartOptions.hAxis.minValue = x.min;
		chartOptions.hAxis.maxValue = x.max;
	    }
	    if(this.getProperty("vAxisFixedRange")) {
		let y = this.getColumnValues(records, selectedFields[2]);
		chartOptions.vAxis.minValue = y.min;
		chartOptions.vAxis.maxValue = y.max;
	    }



	    chartOptions.vAxis.viewWindowMode = this.getProperty("viewWindowMode","pretty");
	    chartOptions.hAxis.viewWindowMode = this.getProperty("viewWindowMode","pretty");

            chartOptions.hAxis.format = this.getProperty("hAxisFormat", null);
            chartOptions.vAxis.format = this.getProperty("vAxisFormat", null);

            chartOptions.hAxis.title = this.getProperty("hAxisTitle", header.length > 1 ? header[1] : null);
            chartOptions.vAxis.title = this.getProperty("vAxisTitle", header.length > 2 ? header[2] : null);

	    //	    console.log(JSON.stringify(chartOptions,null,2));

            return new google.visualization.BubbleChart(chartDiv); 
        }

    });
}


function BartableDisplay(displayManager, id, properties) {
    const SUPER = new RamaddaSeriesChart(displayManager, id, DISPLAY_BARTABLE, properties);
    defineDisplay(addRamaddaDisplay(this), SUPER, [], {
	getRequiredPackages: function() {
	    return ['bar'];
	},
        doMakeGoogleChart: function(dataList, props, chartDiv, selectedFields, chartOptions) {
            let height = "";
            if (Utils.isDefined(this.getChartHeight())) {
                height = this.getChartHeight();
            } else {
                if (dataList.length > 1) {
                    let numBars = dataList.length;
                    if (this.getProperty('isStacked')) {
                        height = numBars * 22;
                    } else {
                        height = numBars * 22 + numBars * 14 * (this.getDataValues(dataList[0]).length - 2);
                    }
                }
            }
            $.extend(chartOptions, {
                title: '',
                bars: 'horizontal',
                colors: this.getColorList(),
                width: (Utils.isDefined(this.getChartWidth()) ? this.getChartWidth() : "100%"),
                chartArea: {
                    left: this.getChartLeft('30%'),
                    top: 0,
                    width: '70%',
                    height: '80%'
                },
                height: height,
                bars: 'horizontal',
                tooltip: {
                    showColorCode: true,
		    isHtml:true
                },
                legend: {
                    position: 'none'
                },
            });

            if (Utils.isDefined(this.getProperty('isStacked'))) {
                chartOptions.isStacked = this.getProperty('isStacked');
            }

            if (this.getProperty('hAxisTitle'))
                chartOptions.hAxis = {
                    title: this.getProperty('hAxisTitle')
                };
            if (this.getProperty('vAxisTitle'))
                chartOptions.vAxis = {
                    title: this.getProperty('vAxisTitle')
                };
            return new google.visualization.BarChart(chartDiv);
	    //            return new google.charts.Bar(chartDiv); 
        },
        getDefaultSelectedFields: function(fields, dfltList) {
            let f = [];
            for (i = 0; i < fields.length; i++) {
                let field = fields[i];
                if (!field.isNumeric()) {
                    f.push(field);
                    break;
                }
            }
            for (i = 0; i < fields.length; i++) {
                let field = fields[i];
                if (field.isNumeric()) {
                    f.push(field);
                    break;
                }
            }
            return f;
        }
    });


}


function TreemapDisplay(displayManager, id, properties) {
    const SUPER = new RamaddaTextChart(displayManager, id, DISPLAY_TREEMAP, properties);
    defineDisplay(addRamaddaDisplay(this), SUPER, [], {
        handleEventRecordSelection: function(source, args) {},
	getRequiredPackages: function() {
	    return ['treemap'];
	},
        getFieldsToSelect: function(pointData) {
            return pointData.getRecordFields();
        },
        tooltips: {},
        makeChartOptions: function(dataList, props, selectedFields) {
            let _this = this;
            let tooltip = function(row, size, value) {
                if (_this.tooltips[row]) {
                    return _this.tooltips[row];
                }
                return "<div class='display-treemap-tooltip-outer'><div class='display-treemap-tooltip''><i>left-click: go down<br>right-click: go up</i></div></div>";
            };
            let chartOptions = SUPER.makeChartOptions.call(this, dataList, props, selectedFields);
            $.extend(chartOptions, {
                highlightOnMouseOver: true,
                generateTooltip: tooltip,
                maxDepth: parseInt(this.getProperty("maxDepth", 2)),
                maxPostDepth: parseInt(this.getProperty("maxPostDepth", 3)),
            });

            return chartOptions;
        },
        defaultSelectedToAll: function() {
            return true;
        },

        doMakeGoogleChart: function(dataList, props, chartDiv, selectedFields, chartOptions) {
            let dataTable = this.makeDataTable(dataList, props, selectedFields, chartOptions);
            if (!dataTable) return null;
            return new google.visualization.TreeMap(chartDiv);
        },

        addTuple: function(data, colorField, seen, value, parent, n1, n2) {
            let ovalue = value;
            let cnt = 0;
            if (Utils.isDefined(seen[value]) && parent) {
                value = parent + ":" + value;
            }
            while (true) {
                if (!Utils.isDefined(seen[value])) {
                    seen[value] = true;
                    break;
                }
                value = ovalue + " " + (++cnt);
            }
            let tuple = [value, parent, n1];
            if (colorField) tuple.push(n2);
            data.push(tuple);
            return value;
        },

        valueClicked: function(field, value) {
            field = this.getFieldById(this.getFields(), field);
            this.propagateEvent("fieldValueSelected", {
                field: field,
                value: value
            });
        },
        makeDataTable: function(dataList, props, selectedFields) {
            let records = this.filterData(null,null,{skipFirst:true});
            if (!records) {
                return null;
            }
            let allFields = this.getFields();
            let fields = this.getSelectedFields(allFields);
            if (fields.length == 0)
                fields = allFields;
            let strings = this.getFieldsByType(fields, "string");
            if (strings.length < 2) {
                this.displayError("No string fields specified");
                return null;
            }
            let addPrefix = this.getProperty("addPrefix", true);
            let sizeField = this.getFieldById(allFields, this.getProperty("sizeBy"));
            let colorField = this.getFieldById(allFields, this.getProperty("colorBy"));
            let values = this.getFieldsByType(fields, "numeric");
            if (!sizeField && values.length > 0)
                sizeField = values[0];
            if (!colorField && values.length > 1)
                colorField = values[1];

            let tooltipFields = [];
            let toks = this.getProperty("tooltipFields", "").split(",");
            for (let i = 0; i < toks.length; i++) {
                let tooltipField = this.getFieldById(null, toks[i]);
                if (tooltipField)
                    tooltipFields.push(tooltipField);
            }
            if (tooltipFields.length == 0) tooltipFields = allFields;

            this.tooltips = {};

            let columns = [];
            for (let fieldIndex = 0; fieldIndex < strings.length; fieldIndex++) {
                let field = strings[fieldIndex];
                columns.push(this.getColumnValues(records, field).values);
            }

            let data = [];
            let leafs = [];
            let tmptt = [];
            let seen = {};
            this.addTuple(data, colorField, {}, "Node", "Parent", "Value", "Color");
            let root = strings[0].getLabel(this);
            this.addTuple(data, colorField, seen, root, null, 0, 0);
            let keys = {};
            let call = this.getGet();
            for (let rowIdx = 0; rowIdx < records.length; rowIdx++) {
                //               if(rowIdx>20) break;
                let row = this.getDataValues(records[rowIdx]);
                let key = "";
                let parentKey = "";
                for (let fieldIndex = 0; fieldIndex < strings.length - 1; fieldIndex++) {
                    let values = columns[fieldIndex];
                    if (key != "")
                        key += ":";
                    key += values[rowIdx];
                    if (!Utils.isDefined(keys[key])) {
                        let parent = Utils.isDefined(keys[parentKey]) ? keys[parentKey] : root;
                        let value = values[rowIdx];
                        if (addPrefix && fieldIndex > 0)
                            value = parent + ":" + value;
                        keys[key] = this.addTuple(data, colorField, seen, value, parent, 0, 0);
                    }
                    parentKey = key;
                }
                let parent = Utils.isDefined(keys[parentKey]) ? keys[parentKey] : root;
                let value = row[strings[strings.length - 1].getIndex()];
                let size = sizeField ? row[sizeField.getIndex()] : 1;
                let color = colorField ? row[colorField.getIndex()] : 0;
                value = this.addTuple(leafs, colorField, seen, value, parent, size, color);
                let tt = "<div class='display-treemap-tooltip-outer'><div class='display-treemap-tooltip''>";
                for (let f = 0; f < tooltipFields.length; f++) {
                    let v = row[tooltipFields[f].getIndex()];
                    let field = tooltipFields[f];
                    v = HU.onClick(call + ".valueClicked('" + field.getId() + "','" + v + "')", v, []);
                    tt += HU.b(field.getLabel(this)) + ": " + v + "<br>";
                }
                tt += "</div></div>";
                tmptt.push(tt);
            }
            for (let i = 0; i < leafs.length; i++) {
                data.push(leafs[i]);
                this.tooltips[data.length - 2] = tmptt[i];
            }
            return google.visualization.arrayToDataTable(data);
        },
    });
}



function TimerangechartDisplay(displayManager, id, properties) {
    const SUPER = new RamaddaTextChart(displayManager, id, DISPLAY_TIMERANGECHART, properties);
    let myProps = [
	{label:'Time Range'},
	{p:'startDateField',ex:''},
	{p:'endDateField',ex:''},
	{p:'labelFields',ex:''},
	{p:'showLabel',ex:'false'},		
	{p:"fontSize",d:10},
	{p:"font",d:'Helvetica'},
	{p:'alternatingRowStyle',d:'true',ex:'false'}
    ];

    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
	getRequiredPackages: function() {
	    return ['timeline'];
	},
        doMakeGoogleChart: function(dataList, props, chartDiv, selectedFields, chartOptions) {
	    if(this.dataColors && this.dataColors.length)
		chartOptions.colors = this.dataColors;

	    chartOptions.alternatingRowStyle = this.getAlternatingRowStyle();
	    chartOptions.timeline =  {
		rowLabelStyle: {fontName: this.getFont(), fontSize: this.getFontSize()},
                barLabelStyle: {fontName: this.getFont(), fontSize:  this.getFontSize() }
	    };

	    chartOptions.tooltip = null;
            return new google.visualization.Timeline(chartDiv);
        },
        makeDataTable: function(dataList, props, selectedFields) {
	    let tt = this.getProperty("tooltip");
	    let addTooltip = (tt || this.getProperty("addTooltip",false)) && this.doAddTooltip();
	    let records = this.filterData(null,null,{skipFirst:true});
            let strings = [];
            let stringField = this.getFieldByType(selectedFields, "string");
            if (!stringField)
                stringField = this.getFieldByType(null, "string");
            let showLabel = this.getProperty("showLabel", true);
            let labelFields = [];
            let labelFieldsTemplate = this.getProperty("labelFieldsTemplate");
            let toks = this.getProperty("labelFields", "").split(",");
            for (let i = 0; i < toks.length; i++) {
                let field = this.getFieldById(null, toks[i]);
                if (field)
                    labelFields.push(field);
            }

	    let startDateField = this.getFieldById(null,this.getPropertyStartDateField());
	    let endDateField = this.getFieldById(null,this.getPropertyEndDateField());
            let dateFields = [];
	    if(startDateField==null || endDateField==null) {
                dateFields = this.getFieldsByType(null, "date");
	    } else {
		dateFields = [startDateField, endDateField];
	    }
            let values = [];
            let dataTable = new google.visualization.DataTable();
            if (dateFields.length < 2) {
                throw new Error("Need to have at least 2 date fields");
            }

            if (stringField) {
                dataTable.addColumn({
                    type: 'string',
                    id: stringField.getLabel(this)
                });
            } else {
                dataTable.addColumn({
                    type: 'string',
                    id: "Index"
                });
            }

	    labelFields = [];

            if (labelFields.length > 0) {
                dataTable.addColumn({
                    type: 'string',
                    id: 'Label'
                });
            } else if(addTooltip) {
                dataTable.addColumn({
                    type: 'string',
                    id: 'dummy'
                });
	    }
	    if(addTooltip)  {
		dataTable.addColumn({
                    type: 'string',
                    role: 'tooltip',
                    'p': {
			'html': true
                    }});
	    }

            dataTable.addColumn({
                type: 'date',
                id: dateFields[0].getLabel(this)
            });
            dataTable.addColumn({
                type: 'date',
                id: dateFields[1].getLabel(this)
            });


            let colorBy = this.getColorByInfo(records);
	    if(colorBy.isEnabled()) {
		this.dataColors = [];
	    }

            for (let r = 0; r < records.length; r++) {
		let record = records[r];
                let row = this.getDataValues(records[r]);
                let tuple = [];
		if(this.dataColors) {
		    let c = colorBy.getColorFromRecord(record, "blue");
		    this.dataColors.push(c);
		}
                values.push(tuple);
                if (stringField && showLabel)
                    tuple.push(row[stringField.getIndex()]);
                else
                    tuple.push("#" + (r + 1));
                if (labelFields.length > 0) {
                    let label = "";
                    if (labelFieldsTemplate)
                        label = labelFieldsTemplate;
                    for (let l = 0; l < labelFields.length; l++) {
                        let f = labelFields[l];
                        let value = row[f.getIndex()];
                        if (labelFieldsTemplate) {
                            label = label.replace("{" + f.getId() + "}", value);
                        } else {
                            label += value + " ";
                        }

                    }
                    tuple.push(label);
                } else 	if(addTooltip)  {
		    tuple.push(null);
		}
		if(addTooltip)  {
		    let text =   this.getRecordHtml(record, null, tt);
		    tuple.push(text);
		}
                tuple.push(row[dateFields[0].getIndex()]);
                tuple.push(row[dateFields[1].getIndex()]);
            }
            dataTable.addRows(values);
	    if(colorBy.isEnabled()) {
		colorBy.displayColorTable(null,true);
	    }
            return dataTable;
        }
    });
}



function CalendarDisplay(displayManager, id, properties) {
    const SUPER =  new RamaddaGoogleChart(displayManager, id, DISPLAY_CALENDAR, properties);
    let myProps = [
	{label:'Calendar'},
	{p:'cellSize',d:15,ex:"15"},
	{p:'missingValue',ex:""},	
	{p:'strokeColor',d: '#76a7fa', ex:'#76a7fa'},
	{p:'strokeWidth',ex:'1'},
	{p:'strokeOpacity',d:0.5,ex:'0.5'},	    		
	{p:'noDataBackground',ex:'green'},
	{p:'noDataColor',ex:'red'},
	{p:'colorAxis',ex:'red,blue'},	

    ];
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
	getRequiredPackages: function() {
	    return ['calendar'];
	},
        doMakeGoogleChart: function(dataList, props, chartDiv, selectedFields, chartOptions) {
	    let opts = {
		calendar: {
                    cellSize: parseInt(this.getPropertyCellSize()),
		    cellColor: {
			stroke: this.getPropertyStrokeColor(),
			strokeOpacity: this.getPropertyStrokeOpacity(),
			strokeWidth: this.getPropertyStrokeWidth(1),
		    },
		},
		height: this.getProperty("height", 800),
		noDataPattern: {
		    backgroundColor: this.getPropertyNoDataBackground(),
		    color: this.getPropertyNoDataColor()
		},
	    };
	    let colors = this.getPropertyColorAxis();
	    if(colors) {
		opts.colorAxis =  {
		    colors:colors.split(",")
		}
	    }
	    $.extend(chartOptions, opts);
            //If a calendar is show in tabs then it never returns from the draw
            if (this.jq(ID_CHART).width() == 0) {
                return;
            }

            let cal =  new google.visualization.Calendar(chartDiv);
	    return cal;

        },
        defaultSelectedToAll: function() {
            return true;
        },
        getContentsStyle: function() {
            let height = this.getProperty("height", 800);
            if (height > 0) {
                return " height:" + height + "px; " + " max-height:" + height + "px; overflow-y: auto;";
            }
            return "";
        },
        canDoMultiFields: function() {
            return false;
        },
        getIncludeIndexIfDate: function() {
            return true;
        },
        makeDataTable: function(dataList, props, selectedFields) {
            let dataTable = new google.visualization.DataTable();
            let header = this.getDataValues(dataList[0]);
            if (header.length < 2) return null;
            dataTable.addColumn({
                type: 'date',
                id: 'Date'
            });
            dataTable.addColumn({
                type: 'number',
                id: header[1]
            });
            dataTable.addColumn({
                type: 'string',
                role: 'tooltip',
                'p': {
                    'html': true
                }
            });
            let haveMissing = false;
            let missing = this.getPropertyMissingValue();
            if (missing) {
                haveMissing = true;
                missing = parseFloat(missing);
            }
            let list = [];
            let cnt = 0;
            let options = {
                weekday: 'long',
                year: 'numeric',
                month: 'long',
                day: 'numeric'
            };
	    this.dateToRecords = {};
            for (let i = 1; i < dataList.length; i++) {
		let records = this.getBinnedRecords(dataList[i].record);
		let obj = dataList[i];
		if(!records && obj.record) records  = [obj.record];
                let value = this.getDataValues(obj)[1];
                if (value == NaN) continue;
                if (haveMissing && value == missing) {
                    continue;
                }
                cnt++;

		let dttm = this.getDataValues(dataList[i])[0];
		this.dateToRecords[dttm.v.getTime()] = records;
                let tooltip = "<center><b>" + dttm.f + "</b></center>" +
                    "<b>" + header[1].replace(/ /g, "&nbsp;") + "</b>:&nbsp;" + this.formatNumber(value);
                tooltip = HU.div([STYLE, HU.css('padding','5px')], tooltip);
                list.push([this.getDataValues(dataList[i])[0], value, tooltip]);
            }
            dataTable.addRows(list);
            return dataTable;
	}

    });


}









function GaugeDisplay(displayManager, id, properties) {
    const SUPER =  new RamaddaGoogleChart(displayManager, id, DISPLAY_GAUGE, properties);
    properties.multiChartsLabelPosition = 'none';
    let myProps = [
	{label:'Gauge'},
	{p:'minorTicks',d:0},
	{p:'majorTicks',ex:'10,20,30,40'},
	{p:'fontSize',ex:'14pt'},
	{p:'greenFrom',ex:0},
	{p:'greenTo',ex:10},
	{p:'yellowFrom',ex:0},
	{p:'yellowTo',ex:10},		
	{p:'redFrom',ex:0},
	{p:'redColor',ex:'#ff0000'},
	{p:'yellowColor',ex:'#ff0000'},
	{p:'greenColor',ex:'#ff0000'},	
	{p:'redTo',ex:10},
	{p:'gaugeMin',tt:'min gauge value'},
	{p:'gaugeMax',tt:'max gauge value'},
	{p:'field_based_values',tt:'all of the above can have fieldid.propert, e.g., temp.redFrom=...'},		
    ];


    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
	getRequiredPackages: function() {
	    return ['gauge'];
	},
        getChartHeight: function() {
            return this.getProperty("chartHeight", this.getChartWidth());
        },
        getChartWidth: function() {
            return this.getProperty("chartWidth", "150");
        },

	//check the font size
	drawChart(chart,dataTable,chartOptions) {
	    SUPER.drawChart.call(this,chart,dataTable,chartOptions);
	    let items = $(chart.container).find('svg g text');
	    if(this.getFontSize()) {
		items.css('font-size',this.getFontSize());
	    }
	},
	doMultiChartsByField:function() {
	    //If we are doing multi charts then do them by field (columns)
	    return true;
	},
        doMakeGoogleChart: function(dataList, props, chartDiv, selectedFields, chartOptions) {
            this.dataList = dataList;
            let field = selectedFields[0];
	    let c = (props)=>{
		props.forEach(prop=>{
		    let v = this.getFieldProperty(field,prop);
		    if(Utils.isDefined(v))
			chartOptions[prop] = v;
		});
	    };
	    c(['redFrom','redTo','yellowFrom','yellowTo','greenFrom','greenTo','greenColor','redColor','yellowColor','min','max']);
	    $.extend(chartOptions,{
		minorTicks: this.getFieldProperty(field,'minorTicks',0)
	    });
	    if(Utils.stringDefined(this.getFieldProperty(field,'majorTicks'))) {
		chartOptions.majorTicks =  Utils.split(this.getFieldProperty(field,'majorTicks'));
	    }

            let min = Number.MAX_VALUE;
            let max = Number.MIN_VALUE;
            let setMinMax = true;
            for (let row = 1; row < dataList.length; row++) {
                let tuple = this.getDataValues(dataList[row]);
                //                        if(tuple.length>2) setMinMax = false;
                for (let col = 0; col < tuple.length; col++) {
                    if (!Utils.isNumber(tuple[col])) {
                        continue;
                    }
                    let value = tuple[col];
                    min = Utils.min(min, value);
                    max = Utils.max(max, value);
                }
            }
            min = Utils.formatNumber(min, true);
            max = Utils.formatNumber(max, true);
	    let gaugeMin  = this.getFieldProperty(field,'gaugeMin');
	    let gaugeMax  = this.getFieldProperty(field,'gaugeMax');
            if (Utils.isDefined(gaugeMin)) {
                setMinMax = true;
                min = parseFloat(gaugeMin);
            }
            if (Utils.isDefined(gaugeMax)) {
                setMinMax = true;
                max = parseFloat(gaugeMax);
            }
            if (setMinMax) {
                chartOptions.min = min;
                chartOptions.max = max;
            }
            return new google.visualization.Gauge(chartDiv);
        },

        makeDataTable: function(dataList, props, selectedFields) {
	    if(dataList==null) return;
            dataList = this.makeDataArray(dataList);
            if (!Utils.isDefined(this.index)) this.index = dataList.length - 1;
            let index = this.index + 1;
            let list = [];
            list.push(["Label", "Value"]);
            let header = this.getDataValues(dataList[0]);
            if (index >= dataList.length) index = dataList.length - 1;
            let row = this.getDataValues(dataList[index]);
            for (let i = 0; i < row.length; i++) {
                if (!Utils.isNumber(row[i])) continue;
                let h = header[i];
                if (h.length > 20) {
                    let index = h.indexOf("(");
                    if (index > 0) {
                        h = h.substring(0, index);
                    }
                }
                if (h.length > 20) {
                    h = h.substring(0, 19) + "...";
                }
                if (this.getProperty("gaugeLabel"))
                    h = this.getProperty("gaugeLabel");
                else if (this["gaugeLabel" + (i + 1)]) h = this["gaugeLabel" + (i + 1)];
                let value = row[i];
                list.push([h, Utils.formatNumber(value, true)]);
            }

            return google.visualization.arrayToDataTable(list);
        },
        setChartSelection: function(index) {
            this.index = index;
	    if(this.multiChartData) {
		this.multiChartData.forEach(info=>{
		    //{dataList:dataList,field:field,chartOptions:chart.chartOptions,chart:chart});
		    let dataTable = this.makeDataTable(info.dataList,info.props, info.fields);
                    this.drawChart(info.chart,dataTable, info.chartOptions);
		});
		return
	    }
            let dataTable = this.makeDataTable(this.dataList);
            this.mapCharts((chart,idx)=>{
                this.drawChart(chart,dataTable, this.chartOptions);
	    });
        },
    });
}




function ScatterplotDisplay(displayManager, id, properties) {
    const SUPER = new RamaddaGoogleChart(displayManager, id, DISPLAY_SCATTERPLOT, properties);
    defineDisplay(addRamaddaDisplay(this), SUPER, [], {
        trendLineEnabled: function() {
            return true;
        },
	setAxisRanges: function(chartOptions, selectedFields, records) {
	    if(this.getProperty("hAxisFixedRange")) {
		let x = this.getColumnValues(records, selectedFields[0]);
		if(chartOptions.hAxis) chartOptions.hAxis = {};
		chartOptions.hAxis.minValue = x.min;
		chartOptions.hAxis.maxValue = x.max;
	    }

	    if(this.getProperty("vAxisFixedRange")) {
		if(chartOptions.vAxis) chartOptions.vAxis = {};
		let x = this.getColumnValues(records, selectedFields[1]);
		chartOptions.vAxis.minValue = x.min;
		chartOptions.vAxis.maxValue = x.max;
	    }
	},
        makeChartOptions: function(dataList, props, selectedFields) {
            let chartOptions = SUPER.makeChartOptions.call(this, dataList, props, selectedFields);
            chartOptions.curveType = null;
            chartOptions.lineWidth = 0;
            $.extend(chartOptions, {
                title: '',
                tooltip: {
                    isHtml: true
                },
                legend: 'none',
            });

            if (!chartOptions.chartArea) chartOptions.chartArea = {};
            $.extend(chartOptions.chartArea, {
                left: "10%",
                top: 10,
                height: "80%",
                width: "90%"
            });
            if (this.getShowTitle()) {
                chartOptions.title = this.getTitle(true);
            }

	    if (!chartOptions.hAxis) chartOptions.hAxis = {};
	    if (!chartOptions.vAxis) chartOptions.vAxis = {};
	    if(this.getProperty("hAxisLogScale", false)) 
		chartOptions.hAxis.logScale = true;
	    if(this.getProperty("vAxisLogScale", false)) 
		chartOptions.vAxis.logScale = true;


	    chartOptions.vAxis.viewWindowMode = this.getProperty("viewWindowMode","pretty");
	    chartOptions.hAxis.viewWindowMode = this.getProperty("viewWindowMode","pretty");

	    /*
	      chartOptions.trendlines =  {
	      0: {
	      type: 'linear',
	      color: 'green',
	      lineWidth: 3,
	      opacity: 0.3,
	      showR2: true,
	      visibleInLegend: true
	      }
	      };		

	    */


            if (dataList.length > 0 && this.getDataValues(dataList[0]).length > 1) {
                if (!chartOptions.vAxis) chartOptions.vAxis = {};
                if (!chartOptions.hAxis) chartOptions.hAxis = {};
		if (this.getProperty("hAxisTitle")) {
                    chartOptions.hAxis.title = this.getProperty("hAxisTitle");
		}
		if (this.getProperty("vAxisTitle")) {
                    chartOptions.vAxis.title = this.getProperty("vAxisTitle");
		}

		if(!chartOptions.hAxis.title) {
                    $.extend(chartOptions.hAxis, {
			title: this.getDataValues(dataList[0])[0]
                    });
		}

		if(!chartOptions.vAxis.title) {
                    $.extend(chartOptions.vAxis, {
			title: this.getDataValues(dataList[0])[1]
                    });
		}
                //We only have the one vAxis range for now
                if (!isNaN(this.getVAxisMinValue())) {
		    //                    chartOptions.hAxis.minValue = this.getVAxisMinValue();
                    chartOptions.vAxis.minValue = this.getVAxisMinValue();
                }
                if (!isNaN(this.getVAxisMaxValue())) {
		    //                    chartOptions.hAxis.maxValue = this.getVAxisMaxValue();
                    chartOptions.vAxis.maxValue = this.getVAxisMaxValue();
                }
            }
	    //	    console.log(JSON.stringify(chartOptions,null,2));

            return chartOptions;
        },
        doMakeGoogleChart: function(dataList, props, chartDiv, selectedFields, chartOptions) {
	    if(!chartDiv) return
            let height = this.getProperty("height",400);
            if (Utils.isDefined(this.getProperty("chartHeight"))) {
                height = this.getProperty("chartHeight");
            }
            let width = "100%";
	    if (Utils.isDefined(this.getProperty("chartWidth"))) {
                width = this.getProperty("chartWidth");
            }
	    if((typeof height)=="number") height = height+"px";
	    if((typeof width)=="number") width = width+"px";

            $("#" + chartDiv.id).css("width", width);
            $("#" + chartDiv.id).css("height", height);
            return new google.visualization.ScatterChart(chartDiv);
        },

        getDefaultSelectedFields: function(fields, dfltList) {
            let f = [];
            for (i = 0; i < fields.length; i++) {
                let field = fields[i];
                if (field.isNumeric()) {
                    f.push(field);
                    if (f.length >= 2)
                        break;
                }
            }
            return f;
        }
    });


}



function OrgchartDisplay(displayManager, id, properties) {
    const ID_ORGCHART = "orgchart";
    const SUPER = new RamaddaGoogleChart(displayManager, id, DISPLAY_ORGCHART, properties);
    let myProps = [
	{label:'Orgchart'},
	{p:'labelField',ex:''},
	{p:'parentField',ex:''},
	{p:'idField',ex:''},
	{p:'treeRoot',ex:'some label'},
	{p:'treeTemplate',ex:''},
	{p:'treeNodeSize',ex:'small|medium|large'}
    ];
    defineDisplay(addRamaddaDisplay(this), SUPER,myProps, {
        handleEventRecordSelection: function(source, args) {},
        needsData: function() {
            return true;
        },
	getRequiredPackages: function() {
	    return ['orgchart'];
	},

	updateUIInner: function() {
            this.displayHtml(HU.div([ID,this.domId(ID_ORGCHART)],""));
	    if(this.jq(ID_ORGCHART).length==0) {
		setTimeout(()=>this.updateUI(),1000);
		return;
	    }
	    let roots=null;
	    try {
		roots = this.makeTree();
	    } catch(error) {
		this.handleError("An error has occurred:" + error, error);
		return;
	    }
	    if(roots==null) return;

	    let data = new google.visualization.DataTable();
            data.addColumn('string', 'Name');
            data.addColumn('string', 'Parent');
            data.addColumn('string', 'ToolTip');
	    let rows = [];
	    let cnt=0;
	    let func = function(node) {
		cnt++;
		let value = node.label;
		if(node.display) {
		    value = {'v':node.label,f:node.display};
		}
		let row = [value, node.parent?node.parent.label:"",node.tooltip||""];
		rows.push(row);
		if(node.children.length>0) {
		    node.children.map(func);
		}
		if(node.record) {
		    //		    _this.countToRecord[cnt] = node.record;
		}
	    }
	    roots.map(func);
            data.addRows(rows);
            let chart = new google.visualization.OrgChart(document.getElementById(this.domId(ID_ORGCHART)));
            // Draw the chart, setting the allowHtml option to true for the tooltips.
            this.drawChart(chart, data, {'allowHtml':true,'allowCollapse':true,
					 'size':this.getProperty("treeNodeSize","medium")});
	}
    });
}

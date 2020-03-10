/**
   Copyright 2008-2019 Geode Systems LLC
*/


var CATEGORY_CHARTS = "Charts";
var CATEGORY_OTHER = "Other Charts";

var DISPLAY_LINECHART = "linechart";
var DISPLAY_AREACHART = "areachart";
var DISPLAY_BARCHART = "barchart";
var DISPLAY_BARTABLE = "bartable";
var DISPLAY_BARSTACK = "barstack";
var DISPLAY_PIECHART = "piechart";
var DISPLAY_TIMERANGECHART = "timerangechart";
var DISPLAY_SANKEY = "sankey";
var DISPLAY_CALENDAR = "calendar";
var DISPLAY_SCATTERPLOT = "scatterplot";
var DISPLAY_HISTOGRAM = "histogram";
var DISPLAY_BUBBLE = "bubble";
var DISPLAY_GAUGE = "gauge";
var DISPLAY_TABLE = "table";
var DISPLAY_WORDTREE = "wordtree";
var DISPLAY_TREEMAP = "treemap";
var ID_CHART = "chart";
var ID_CHARTS = "charts";
var ID_CHARTS_INNER = "chartsinner";

var googleChartsLoaded = false;
function googleChartsHaveLoaded() {
    googleChartsLoaded = true;
}
if(window["google"]) {
    google.charts.setOnLoadCallback(googleChartsHaveLoaded);
}

function haveGoogleChartsLoaded() {
    if (!googleChartsLoaded) {
        if (Utils.isDefined(google.visualization)) {
            if (Utils.isDefined(google.visualization.Gauge)) {
                googleChartsLoaded = true;
            }
        }
    }
    return googleChartsLoaded;
}

function waitOnGoogleCharts(object, callback) {
    if (haveGoogleChartsLoaded()) {
	return true;
    }
    if (!object.googleChartCallbackPending) {
        object.googleChartCallbackPending = true;
        var func = function() {
            object.googleChartCallbackPending = false;
            callback();
        }
        setTimeout(func, 100);
    }
    return false;
}





addGlobalDisplayType({
    type: DISPLAY_LINECHART,
    label: "Line Chart",
    requiresData: true,
    forUser: true,
    category: CATEGORY_CHARTS
});
addGlobalDisplayType({
    type: DISPLAY_BARCHART,
    label: "Bar Chart",
    requiresData: true,
    forUser: true,
    category: CATEGORY_CHARTS
});
addGlobalDisplayType({
    type: DISPLAY_BARSTACK,
    label: "Stacked Bar Chart",
    requiresData: true,
    forUser: true,
    category: CATEGORY_CHARTS
});
addGlobalDisplayType({
    type: DISPLAY_AREACHART,
    label: "Area Chart",
    requiresData: true,
    forUser: true,
    category: CATEGORY_CHARTS
});

addGlobalDisplayType({
    type: DISPLAY_BARTABLE,
    label: "Bar Table",
    requiresData: true,
    forUser: true,
    category: CATEGORY_CHARTS
});
addGlobalDisplayType({
    type: DISPLAY_SCATTERPLOT,
    label: "Scatter Plot",
    requiresData: true,
    forUser: true,
    category: CATEGORY_CHARTS
});
addGlobalDisplayType({
    type: DISPLAY_HISTOGRAM,
    label: "Histogram",
    requiresData: true,
    forUser: true,
    category: CATEGORY_CHARTS
});
addGlobalDisplayType({
    type: DISPLAY_BUBBLE,
    label: "Bubble Chart",
    requiresData: true,
    forUser: true,
    category: CATEGORY_CHARTS
});
addGlobalDisplayType({
    type: DISPLAY_PIECHART,
    label: "Pie Chart",
    requiresData: true,
    forUser: true,
    category: CATEGORY_CHARTS
});

addGlobalDisplayType({
    type: DISPLAY_GAUGE,
    label: "Gauge",
    requiresData: true,
    forUser: true,
    category: CATEGORY_MISC
});
addGlobalDisplayType({
    type: DISPLAY_TIMERANGECHART,
    label: "Timerange",
    requiresData: true,
    forUser: true,
    category: CATEGORY_MISC
});
addGlobalDisplayType({
    type: DISPLAY_SANKEY,
    label: "Sankey Chart",
    requiresData: true,
    forUser: true,
    category: CATEGORY_MISC
});

addGlobalDisplayType({
    type: DISPLAY_CALENDAR,
    label: "Calendar Chart",
    requiresData: true,
    forUser: true,
    category: CATEGORY_MISC
});
addGlobalDisplayType({
    type: DISPLAY_TABLE,
    label: "Table",
    requiresData: true,
    forUser: true,
    category: CATEGORY_MISC
});
addGlobalDisplayType({
    type: DISPLAY_WORDTREE,
    label: "Word Tree",
    requiresData: true,
    forUser: true,
    category: "Text"
});
addGlobalDisplayType({
    type: DISPLAY_TREEMAP,
    label: "Tree Map",
    requiresData: true,
    forUser: true,
    category: CATEGORY_MISC
});




var PROP_CHART_MIN = "chartMin";
var PROP_CHART_MAX = "chartMax";
var DFLT_WIDTH = "600px";
var DFLT_HEIGHT = "200px";

/*
  Create a chart
  id - the id of this chart. Has to correspond to a div tag id 
  pointData - A PointData object (see below)
*/
function RamaddaGoogleChart(displayManager, id, chartType, properties) {
    var ID_TRENDS_CBX = "trends_cbx";
    var ID_PERCENT_CBX = "percent_cbx";
    var ID_COLORS = "colors";

    var _this = this;


    //Init the defaults first
    $.extend(this, {
        indexField: -1,
        curveType: 'none',
        fontSize: 0,
        showPercent: false,
        percentFields: null,
    });
    let SUPER = new RamaddaFieldsDisplay(displayManager, id, chartType, properties);
    RamaddaUtil.inherit(this, SUPER);



    RamaddaUtil.defineMembers(this, {
        clearCachedData: function() {
            SUPER.clearCachedData();
            this.computedData = null;
        },
        updateUI: function(reload) {
	    let debug = false;
            SUPER.updateUI.call(this, reload);
	    if(debug)
		console.log(this.type+" updateUI")
            if (!this.getDisplayReady()) {
		if(debug)
		    console.log("\tdisplay not ready");
                return;
            }
	    //	    var t1= new Date();
	    if(debug)
		console.log("\tcalling displayData");
            this.displayData(reload);
	    //	    var t2= new Date();
	    //	    Utils.displayTimes("chart.displayData",[t1,t2]);
        },
        getWikiAttributes: function(attrs) {
            this.defineWikiAttributes(["vAxisMinValue", "vAxisMaxValue"]);
            SUPER.getWikiAttributes.call(this, attrs);
            if (this.colorList.join(",") != "blue,red,green") {
                attrs.push("colors");
                attrs.push(this.getColorList().join(", "));
            }
        },

        initDialog: function() {
            SUPER.initDialog.call(this);
            var _this = this;
            var updateFunc = function(e) {
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
                var v = _this.jq(ID_COLORS).val();
                _this.colorList = v.split(",");
                _this.displayData();
                var pointData = _this.dataCollection.getList();
                _this.getDisplayManager().handleEventPointDataLoaded(_this, _this.lastPointData);
            });

            this.jq(ID_TRENDS_CBX).click(function() {
                _this.showTrendLines = _this.jq(ID_TRENDS_CBX).is(':checked');
                _this.displayData();

            });
            this.jq(ID_PERCENT_CBX).click(function() {
                _this.showPercent = _this.jq(ID_PERCENT_CBX).is(':checked');
                _this.displayData();

            });

        },
        setColor: function() {
            var v = prompt("Enter comma separated list of colors to use", this.colorList.join(","));
            if (v != null) {
                this.colorList = v.split(",");
                this.displayData();
                var pointData = this.dataCollection.getList();
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
            var get = this.getGet();
            //                menuItems.push(HU.onClick(get+".setColor();", "Set color"));

            var min = "0";
            if (!isNaN(this.getVAxisMinValue())) {
                min = "" + this.getVAxisMinValue();
            }
            var max = "";
            if (!isNaN(this.getVAxisMaxValue())) {
                max = "" + this.getVAxisMaxValue();
            }

            var tmp = HU.formTable();
            tmp += HU.formEntry("Axis Range:", HU.input("", min, ["size", "7", ATTR_ID, this.getDomId("vaxismin")]) + " - " +
				       HU.input("", max, ["size", "7", ATTR_ID, this.getDomId("vaxismax")]));
            tmp += HU.formEntry("Date Range:", HU.input("", this.minDate, ["size", "10", ATTR_ID, this.getDomId("mindate")]) + " - " +
				       HU.input("", this.maxDate, ["size", "10", ATTR_ID, this.getDomId("maxdate")]));


            tmp += HU.formEntry("Colors:",
				       HU.input("", this.getColorList().join(","), ["size", "35", ATTR_ID, this.getDomId(ID_COLORS)]));
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
            var ex = this.minDate;
            if (ex == null || ex == "") {
                ex = "1800-01-01";
            }
            var v = prompt("Minimum date", ex);
            if (v == null) return;
            this.setMinDate(v);
        },
        setMinDate: function(v) {
            this.minDate = v;
            this.displayData();
        },
        askMaxDate: function() {
            var ex = this.maxDate;
            if (ex == null || ex == "") {
                ex = "2100-01-01";
            }
            var v = prompt("Maximum date", ex);
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
            var height = "600";
            var html = HU.div([ATTR_ID, this.getDomId(ID_FIELDS), STYLE, HU.css('overflow-y','auto','max-height', height + "px")], " FIELDS ");

            if (this.trendLineEnabled()) {
                html += HU.div([ATTR_CLASS, "display-dialog-subheader"], "Other");

                html += HU.checkbox(this.getDomId(ID_TRENDS_CBX),
					   [],
					   this.getProperty("showTrendLines", false)) + "  " + "Show trend line";
                html += " ";
                html += HU.checkbox(this.getDomId(ID_PERCENT_CBX),
					   [],
					   this.showPercent) + "  " + "Show percent of displayed total" + "<br>";
                html += "<br>";
            }

            tabTitles.push("Fields");
            tabContents.push(html);
            SUPER.RamaddaDisplay.getDialogContents.call(this, tabTitles, tabContents);
        },
        okToHandleEventRecordSelection: function() {
            return true;
        },
        handleEventRecordHighlight: function(source, args) {
	    this.handleEventRecordSelection(source, args);
	},
        handleEventRecordSelection: function(source, args) {
            //TODO: don't do this in index space, do it in time or space space
            if (source == this) {
                return;
            }
            if (!this.okToHandleEventRecordSelection()) {
                return;
	    }
	    var index = this.findMatchingIndex(args.record);
	    //	    console.log(this.type +" index:" + index);
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
            this.computedData = null;
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
		value = {v:offset,f:value};
	    } 
	    return value;
	},
        displayData: function(reload) {
	    let debug = false;
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
                    this.setContents(this.getLoadingMessage());
                    setTimeout(()=> {
                        this.googleChartCallbackPending = false;
                        this.displayData();
                    }, 10000);
                }
                return;
            }
            if (this.inError) {
                return;
            }
            if (!this.hasData()) {
                this.clearChart();
                this.setContents(this.getLoadingMessage());
                return;
            }


            this.setContents(HU.div([ATTR_CLASS, "display-message"],
					   "Building display..."));

            this.allFields = this.dataCollection.getList()[0].getRecordFields();
            var pointData = this.dataCollection.getList()[0];

	    if(debug)
		console.log("\tpointData #records:" + pointData.getRecords().length);


            //            var selectedFields = this.getSelectedFields(this.getFieldsToSelect(pointData));
            var selectedFields = this.getSelectedFields();
	    

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
			this.allFields.every(f=>{
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
                this.setContents("No fields selected");
                return;
            }

            //Check for the skip
            var tmpFields = [];
            for (var i = 0; i < selectedFields.length; i++) {
                if (!this.shouldSkipField(selectedFields[i])) {
                    tmpFields.push(selectedFields[i]);
                }
            }
            selectedFields = tmpFields;
	    if(debug)
		console.log("\tsetting lastSelectedFields:" + selectedFields);
            this.lastSelectedFields = selectedFields;


            var props = {
                includeIndex: this.includeIndexInData()
            };
            props.groupByIndex = -1;

            var groupBy = this.getGroupBy();
            if (groupBy) {
                for (var i = 0; i < this.allFields.length; i++) {
                    var field = this.allFields[i];
                    if (field.getId() == groupBy) {
                        props.groupByIndex = field.getIndex();
                        props.groupByField = field;
                        break;
                    }
                }
            }

            var fieldsToSelect = selectedFields;
            if (this.raw) {
                fieldsToSelect = this.dataCollection.getList()[0].getRecordFields();
                props.raw = true;
            }

            props.includeIndexIfDate = this.getIncludeIndexIfDate();

            var dataHasIndex = props.includeIndex;
            let dataList = this.getStandardData(fieldsToSelect, props);
	    if(debug)
		console.log(this.type +" fields:" + fieldsToSelect.length +" dataList:" + dataList.length);
            this.computedData = dataList;
            if (dataList.length == 0 && !this.userHasSelectedAField) {
                var pointData = this.dataCollection.getList()[0];
                var chartableFields = this.getFieldsToSelect(pointData);
                for (var i = 0; i < chartableFields.length; i++) {
                    var field = chartableFields[i];
                    dataList = this.getStandardData([field], props);
                    if (dataList.length > 0) {
                        this.setSelectedFields([field]);
                        break;
                    }
                }
            }

            if (dataList.length == 0) {
                this.setContents(HU.div([ATTR_CLASS, "display-message"],
					       "No data available"));
                return;
            }

            if (this.showPercent) {
                var newList = [];
                var isNumber = [];
                var isOk = [];
                var headerRow = null;
                var fields = null;
                if (this.percentFields != null) {
                    fields = this.percentFields.split(",");
                }
                for (var i = 0; i < dataList.length; i++) {
                    var row = this.getDataValues(dataList[i]);
                    if (i == 0) {
                        headerRow = row;
                        continue;
                    }
                    if (i == 1) {
                        var seenIndex = false;
                        for (var j = 0; j < row.length; j++) {
                            var valueIsNumber = (typeof row[j] == "number");
                            var valueIsDate = (typeof row[j] == "object");
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
                        var newHeader = [];
                        for (var j = 0; j < headerRow.length; j++) {
                            var v = headerRow[j];
                            if (!isNumber[j]) {
                                newHeader.push(v);
                            } else {
                                newHeader.push("% " + v);
                            }
                        }
                        newList.push(newHeader);
                    }

                    var total = 0;
                    var cnt = 0;
                    for (var j = 0; j < row.length; j++) {
                        if (isNumber[j]) {
                            total += parseFloat(row[j]);
                            cnt++;
                        }
                    }
                    var newRow = [];
                    for (var j = 0; j < row.length; j++) {
                        if (isNumber[j]) {
                            if (total != 0) {
                                var v = parseFloat(((row[j] / total) * 100).toFixed(1));
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
                this.makeGoogleChart(dataList, props, selectedFields);
            } catch (e) {
		console.log("Error making chart:\n" + e +"\n" + e.stack);
                return;
            }
            var container = this.jq(ID_CHART);
	    if(this.jq(ID_CHART).is(':visible')) {
		this.lastWidth = container.width();
	    } else {
		this.lastWidth = -1;
	    }

	    if(reload) {
		var pointData = this.getData();
		if(pointData) {
		    let dataList = pointData.getRecords();
		    if(dataList.length>0) {
			let record = dataList[0];
			this.getDisplayManager().notifyEvent("handleEventRecordSelection", this, {record: record});
		    }
		}
	    }
        },
        printDataList: function(dataList) {
            console.log("data list:" + dataList.length);
            for (var i = 0; i < dataList.length; i++) {
                var row = dataList[i];
                var s = "";
                for (var j = 0; j < row.length; j++) {
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
	    this.mapCharts(chart=>{
		if(chart.clearChart) {
		    chart.clearChart();
		}
	    });
        },
        setChartSelection: function(index) {
	    this.mapCharts(chart=>{
                if (chart.setSelection) {
		    chart.setSelection([{
                        row: index,
			column:null
		    }]);
		}});
        },
        tableHeaderMouseover: function(i, tooltip) {},
	getAddToolTip: function() {
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



        makeDataTable: function(dataList, props, selectedFields) {
	    let debug =displayDebug.makeDataTable;
	    let debugRows = 3;
	    if(debug) console.log(this.type+" makeDataTable #records" + dataList.length);

	    let maxWidth = this.getProperty("maxFieldLength",this.getProperty("maxFieldWidth",-1));
	    let addTooltip = this.getAddToolTip();
    	    let addStyle= this.getAddStyle();
	    let annotationTemplate = this.getAnnotationTemplate();
	    let formatNumbers = this.getFormatNumbers();
	    let valueGetter = this.getDataTableValueGetter();
            if (dataList.length == 1) {
                return google.visualization.arrayToDataTable(this.makeDataArray(dataList));
            }
	    var groupField = this.getFieldById(null,  this.getProperty("groupBy"));

	    if(groupField) {
		dataList = this.filterData();
		var groupedData =[];
		var groupValues = [];
		var seen = {};
		var cnt =0;
		var dateToValue =  {};
		var dates = [];
		dataList.map(record=>{
		    if(cnt++==0) return;
		    var values = this.getDataValues(record);
		    var value = values[groupField.getIndex()];
		    if(!seen[value]) {
			seen[value]  = true;
			groupValues.push(value);
		    }
		    var newValues =dateToValue[record.getDate()];
		    if(!newValues) {
			dates.push(record.getDate());
			newValues = {};
			dateToValue[record.getDate()] = newValues;
		    }
		    newValues[value] = values[selectedFields[0].getIndex()];
		});



		var data = [];
		var dataTable = new google.visualization.DataTable();
		

		dataTable.addColumn("date", "Date");
		groupValues.map(group=>{
		    dataTable.addColumn("number",group);
		});
		dates.map(date=>{
		    var tuple=[date];
		    data.push(tuple);
		    var valueMap = dateToValue[date];
		    groupValues.map(group=>{
			var value = valueMap[group];
			tuple.push(value);
		    });
		});
		dataTable.addRows(data);
		return dataTable;
	    }

            var justData = [];
            var tooltipFields = this.getFieldsByIds(null,this.getProperty("tooltipFields", ""));
//	    addTooltip=false;
            var dataTable = new google.visualization.DataTable();
            var header = this.getDataValues(dataList[0]);
            var sample = this.getDataValues(dataList[1]);
	    var fixedValueS = this.getProperty("fixedValue");
	    var fixedValueN;
	    if(fixedValueS) fixedValueN = parseFloat(fixedValueS);
	    let fIdx = 0;
	    let forceStrings = this.getProperty("forceStrings",false);
	    let maxHeaderLength = this.getProperty("maxHeaderLength",-1);
	    let maxHeaderWidth = this.getProperty("maxHeaderWidth",-1);
	    let headerStyle= this.getProperty("headerStyle");
            for (var j = 0; j < header.length; j++) {
		let field=null;
		if(j>0 || !props.includeIndex) {
		    field = selectedFields[fIdx++];
		} else {
		    //todo?
		}
                var value = sample[j];
		let headerLabel = header[j];
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

                if (j == 0 && props.includeIndex) {
                    //This might be a number or a date
                    if ((typeof value) == "object") {
                        //assume its a date
 			if(typeof value.v == "number") {
			    if(forceStrings) 
				dataTable.addColumn('string', headerLabel);
			    else {
				dataTable.addColumn('number', headerLabel);
			    }
			} else {
			    dataTable.addColumn('date', headerLabel);
			}
                    } else {
                        dataTable.addColumn((typeof value), headerLabel);
                    }
                } else {
		    if(j>0 && fixedValueS) {
			dataTable.addColumn('number', this.getProperty("fixedValueLabel","Count"));
		    } else {
			if(field.isString()) {
			    dataTable.addColumn('string', headerLabel);
			} else if(field.isFieldDate()) {
			    dataTable.addColumn('date', headerLabel);
			} else {
			    dataTable.addColumn('number', headerLabel);
			}
		    }
		    if(annotationTemplate) {
			dataTable.addColumn({
			    type: 'string',
			    role: 'annotation',
			    'p': {
				'html': true
			    }
			});
		    }

		    if(addStyle) {
			if(debug)
			    console.log("add style column");
			dataTable.addColumn({ type: 'string', role: 'style' });
		    }
		    if(addTooltip) {
			if(debug)
			    console.log("add tooltip column");
			dataTable.addColumn({
                            type: 'string',
                            role: 'tooltip',
                            'p': {
				'html': true
                            }
			});
		    }
		    if(j>0 && fixedValueS) {
			break;
		    }
                }
            }



	    if(debug) {
		for(var i=0;i<dataTable.getNumberOfColumns();i++)
		    console.log("col:" + i +" " + dataTable.getColumnLabel(i) +" " + dataTable.getColumnType(i));
	    }

	    var annotationLabelField = this.getFieldById(null,this.getProperty("annotationLabelField"));
	    var annotationFields = this.getFieldsByIds(null,this.getProperty("annotationFields"));
	    var annotations = this.getProperty("annotations");
	    if(annotations) annotationFields = [];
	    if(annotationFields.length>0) {
                dataTable.addColumn({
                    type: 'string',
                    role: 'annotation',
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




	    var indexToAnnotation = null;
	    if(annotations) {
		indexToAnnotation = {};
		var annotationsList=[];
		var annotationsMap = {};
		let legend = "";
		var labelCnt = 0;
		var toks = annotations.split(";");
		for(var i=0;i<toks.length;i++) {
		    var toks2 = toks[i].split(",");
		    //index,label,description,url
		    if(toks2.length<2) continue;
		    var index = toks2[0].trim();
		    var label = toks2[1].trim();
		    if(label == "") {
			labelCnt++;
			label  =""+labelCnt;
		    }
		    var desc = toks2.length<2?"":toks2[2];
		    var url = toks2.length<3?null:toks2[3];
		    let isDate = false;
		    let dateLabel ="";
		    if(index.match(/^[0-9]+$/)) {
			index = parseFloat(index);
		    } else {
			index = Utils.parseDate(index,false);
			isDate = true;
			dateLabel = Utils.formatDateYYYYMMDD(index)+": ";
		    }
		    var annotation = {label: label,description: desc   };
		    annotationsList.push(annotation);
		    annotation.index = isDate?index.getTime():index;
		    var legendLabel = desc;
		    if(url!=null) {
			legendLabel = HU.href(url, legendLabel,["target","_annotation"]);
		    }
		    legend+= HU.b(label)+":" + legendLabel+" ";
		}
		if(this.getProperty("showAnnotationsLegend")) {
		    //Pad the left to align with  the chart axis
		    this.jq(ID_LEGEND).html("<table width=100%><tr valign=top><td width=10%></td><td width=90%>" +
					    HU.div([CLASS, "display-chart-legend"],legend)
					    +"</td></tr></table>");
		}
		for(var aidx=0;aidx<annotationsList.length;aidx++) {
		    var annotation = annotationsList[aidx];

		    var minIndex = null;
		    var minDistance = null;
		    for (var rowIdx = 1; rowIdx < dataList.length; rowIdx++) {
			var row = this.getDataValues(dataList[rowIdx]);
			var index = row[0];
			if(index.v) index=  index.v;
			if(index.getTime) index = index.getTime();
			var distance = Math.abs(annotation.index-index);
			if(minIndex == null) {
			    minIndex = rowIdx;
			    minDistance = distance;
			} else {
			    if(distance<minDistance) {
				minIndex = rowIdx;
				minDistance = distance;
			    }
			}
		    }
		    if(minIndex!=null) {
			indexToAnnotation[minIndex] = annotation;
		    }
		}
	    }

	    if(indexToAnnotation) {
		dataTable.addColumn({
                    type: 'string',
                    role: 'annotation',
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

	    var annotationCnt=0;

	    var records = [];
            for (var i = 1; i < dataList.length; i++) {
		records.push(dataList[i].record);
	    }
	    var colors =  this.getColorTable(true);
            var colorBy = this.getColorByInfo(records);

	    var didColorBy = false;
	    var tuples = [];
            for (var rowIdx = 1; rowIdx < dataList.length; rowIdx++) {
		var record =dataList[rowIdx];
                var row = this.getDataValues(record);
		//		var index = row[0];
		//		if(index.v) index  = index.v;
		var theRecord = record.record;
		var color = "";
                if (colorBy.index >= 0) {
                    var value = theRecord.getData()[colorBy.index];
		    hasColorByValue  = true;
		    colorByValue = value;
                    didColorBy = true;
		    color =  colorBy.getColorFromRecord(theRecord);
                }

                row = row.slice(0);
                var label = "";
                if (theRecord) {
                    for (var j = 0; j < tooltipFields.length; j++) {
                        label += "<b>" + tooltipFields[j].getLabel() + "</b>: " +
                            theRecord.getValue(tooltipFields[j].getIndex()) + "<br>";
                    }
		}
		let tooltip = "";
                tooltip += label;
                for (let j = 0; j < row.length; j++) {
		    if (j > 0)
                        tooltip += "<br>";
		    label = header[j].replace(/ /g, "&nbsp;");
		    value = row[j];
		    if (!value) value = "NA";
		    if (value && (typeof value) == "object") {
                        if (value.f) {
			    value = value.f;
                        }
		    }
		    if (Utils.isNumber(value)) {
                        value = this.formatNumber(value);
		    }
		    value = "" + value;
		    value = value.replace(/ /g, SPACE);
		    tooltip += HU.b(label) + ":" + SPACE + value;
                }

		var tt = this.getProperty("tooltip");
		if(tt) {
		    tt  = this.getRecordHtml(theRecord,null,tt);
		    tt = tt.replace("${default}",tooltip);
		    tooltip = tt;
		}
		tooltip = HU.div([STYLE,HU.css('padding','8px')],tooltip);
                let newRow = [];
		if(debug && rowIdx<debugRows)
		    console.log("row:");

                for (var j = 0; j < row.length; j++) {
                    var value = row[j];
		    if(forceStrings) {
			if(value.f) value = (value.f).toString().replace(/\n/g, " ");
		    }
		    if(j>0 && fixedValueS) {
			newRow.push(valueGetter(fixedValueN, theRecord));
			if(debug && rowIdx<debugRows)
			    console.log("\t fixed:" + fixedValueN);
		    } else {
			let type = (typeof value);
			if(type == "number") {
                            if(formatNumbers) {
				value = {v:value,f:String(this.formatNumber(value))};
			    }
			}  else if(type=="boolean") {
			    value = String(value);
			}
			if(debug && rowIdx<debugRows)
			    console.log("\t value:" + value +" " + (typeof value));
			if(maxWidth>0 && type == "string" && value.length > maxWidth)
			    value = value.substring(0,maxWidth) +"...";
			newRow.push(valueGetter(value, theRecord));
		    }
                    if (j == 0 && props.includeIndex) {
			/*note to self - an inline comment breaks the minifier 
			  is the index so don't add a tooltip */
                    } else {
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
			    //			    if(debug && rowIdx<debugRows)
			    //				console.log("\t style:" + color);
			}
			if(addTooltip) {
                            newRow.push(tooltip);
			    if(debug && rowIdx<debugRows)
			    	console.log("\t tooltip:");
			}
                    }
		    if(j>0 && fixedValueS) {
			break;
		    }
                }
		if(annotationFields.length>0) {
                    if (theRecord) {
			var desc = "";
			annotationFields.map(f=>{
			    var d = ""+theRecord.getValue(f.getIndex());
			    if(d!="")
				desc+= (d+"<br>");
			});
			desc = desc.trim();
			desc = desc.replace(/ /g,"&nbsp;");
			annotationCnt++;
			var label = null; 
			if(desc.trim().length>0) {
			    label =""+( annotationLabelField?theRecord.getValue(annotationLabelField.getIndex()):(annotationCnt))
			    if(label.trim().length==0) label = ""+annotationCnt;
			}
			if(debug && rowIdx<debugRows) {
			    console.log("\t label:" + label);
			    console.log("\t desc:" + desc);
			}
			newRow.push(label);
			newRow.push(desc);
		    } else {
			if(i<2)
			    console.log("No records for annotation");
		    }
		}
		if(indexToAnnotation) {
		    var annotation = indexToAnnotation[rowIdx];
		    if(annotation) {
			if(debug && rowIdx<debugRows) {
			    console.log("\t annotation:" + annotation.label);
			    console.log("\t desc:" + annotation.description);
			}
			newRow.push(annotation.label);
			newRow.push(annotation.description);
		    } else {
			if(debug && rowIdx<debugRows) {
			    console.log("\t annotation:" + "null");
			    console.log("\t desc:" + "null");
			}
			newRow.push(null);
			newRow.push(null);
		    }
		}
                justData.push(newRow);
//		if(debug && rowIdx>debugRows) break;
	    }
            dataTable.addRows(justData);
            if (didColorBy) {
		colorBy.displayColorTable();
            }
            return dataTable;
        },
        makeChartOptions: function(dataList, props, selectedFields) {
            var chartOptions = {
                tooltip: {
                    isHtml: true
                },
            };

            $.extend(chartOptions, {
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
            chartOptions.vAxis = {
                gridlines: {},
                minorGridlines: {},		
                textStyle: {}
            };
	    chartOptions.hAxis.minValue = this.getProperty("hAxisMinValue");
	    chartOptions.hAxis.maxValue = this.getProperty("hAxisMaxValue");
	    chartOptions.vAxis.minValue = this.getProperty("vAxisMinValue");
	    chartOptions.vAxis.maxValue = this.getProperty("vAxisMaxValue");

            chartOptions.hAxis.titleTextStyle = {};
            chartOptions.vAxis.titleTextStyle = {};
	    if(this.getProperty("hAxisDateFormat")) {
		chartOptions.hAxis.format = this.getProperty("hAxisDateFormat");
	    }

	    //	    this.getPropertyShow = true;
	    var lineColor = this.getProperty("lineColor");
	    var backgroundColor = this.getProperty("chartBackground");
            this.setPropertyOn(chartOptions.backgroundColor, "chart.fill", "fill", backgroundColor);
            this.setPropertyOn(chartOptions.backgroundColor, "chart.stroke", "stroke", this.getProperty("chartArea.fill", ""));
            this.setPropertyOn(chartOptions.backgroundColor, "chart.strokeWidth", "strokeWidth", null);

            this.setPropertyOn(chartOptions.chartArea.backgroundColor, "chartArea.fill", "fill", backgroundColor);
            this.setPropertyOn(chartOptions.chartArea.backgroundColor, "chartArea.stroke", "stroke", null);
            this.setPropertyOn(chartOptions.chartArea.backgroundColor, "chartArea.strokeWidth", "strokeWidth", null);

	    let minorGridLinesColor = this.getProperty("minorGridLines.color",this.getProperty("gridlines.color", lineColor||"transparent"));
            this.setPropertyOn(chartOptions.hAxis.gridlines, "hAxis.gridlines.color", "color", this.getProperty("gridlines.color", lineColor));
	    this.setPropertyOn(chartOptions.hAxis.minorGridlines, "hAxis.minorGridlines.color", "color", minorGridLinesColor);

	    this.setPropertyOn(chartOptions.hAxis, "hAxis.baselineColor", "baselineColor", this.getProperty("baselineColor", lineColor));	    

            this.setPropertyOn(chartOptions.vAxis.gridlines, "vAxis.gridlines.color", "color", this.getProperty("gridlines.color", lineColor));
	    this.setPropertyOn(chartOptions.vAxis.minorGridlines, "vAxis.minorGridlines.color", "color",  minorGridLinesColor);
	    this.setPropertyOn(chartOptions.vAxis, "vAxis.baselineColor", "baselineColor", this.getProperty("baselineColor", lineColor));


            var textColor = this.getProperty("textColor", "#000");
	    var textBold = this.getProperty("textBold", "false");
            this.setPropertyOn(chartOptions.hAxis.textStyle, "hAxis.text.color", "color", this.getProperty("axis.text.color", textColor));
            this.setPropertyOn(chartOptions.vAxis.textStyle, "vAxis.text.color", "color", this.getProperty("axis.text.color", textColor));

            this.setPropertyOn(chartOptions.hAxis.textStyle, "hAxis.text.bold", "bold", textBold);
            this.setPropertyOn(chartOptions.vAxis.textStyle, "vAxis.text.bold", "bold", textBold);

	    chartOptions.vAxis.text  = this.getProperty("vAxis.text", this.getProperty("vAxisText"));
	    chartOptions.hAxis.slantedText = this.getProperty("hAxis.slantedText",this.getProperty("slantedText",false));
            this.setPropertyOn(chartOptions.hAxis.titleTextStyle, "hAxis.text.color", "color", textColor);
            this.setPropertyOn(chartOptions.vAxis.titleTextStyle, "vAxis.text.color", "color", textColor);
            this.setPropertyOn(chartOptions.legend.textStyle, "legend.text.color", "color", textColor);

	    if(this.getProperty("hAxis.ticks") || this.getProperty("hAxis.ticks")=="")  {
		chartOptions.hAxis.ticks  = this.getProperty("hAxis.ticks").split(",").filter(v=>v!="");
	    }
	    if(this.getProperty("vAxis.ticks") || this.getProperty("vAxis.ticks")=="")  {
		chartOptions.vAxis.ticks  = this.getProperty("vAxis.ticks").split(",").filter(v=>v!="");
	    }


            if (this.fontSize > 0) {
                chartOptions.fontSize = this.fontSize;
            }

            var defaultRange = this.getDisplayManager().getRange(selectedFields[0]);
            var range = [NaN, NaN];
	    //	    console.log("range:" +this.getVAxisMinValue());
            if (!isNaN(this.getVAxisMinValue())) {
                range[0] = this.getVAxisMinValue();
            } else if (defaultRange != null) {
                range[0] = defaultRange[0];
            }

            if (!isNaN(this.getVAxisMaxValue())) {
                range[1] = this.getVAxisMaxValue();
            } else if (defaultRange != null) {
                range[1] = defaultRange[1];
            }
            //console.log("range:" + range[0]+" " + range[1]);

            if (!isNaN(range[0])) {
                chartOptions.vAxis.minValue = range[0];
            }
            if (!isNaN(range[1])) {
                chartOptions.vAxis.maxValue = range[1];
            }
            this.chartDimensions = {
                width: "90%",
                left: "10%",
                right: 10,
            }

            useMultipleAxes = this.getProperty("useMultipleAxes", true);

            if ((selectedFields.length > 1 && useMultipleAxes) || this.getProperty("padRight", false) === true) {
                this.chartDimensions.width = "80%";
            }

            if (this.getProperty("showTrendLines", false)) {
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
            this.setContents(HU.div([ID,this.getDomId(ID_CHARTS)]));
            return chartOptions;
        },
        getChartHeight: function() {
            return this.getProperty("height");
        },
        getChartWidth: function() {
            return this.getProperty("width");
        },
        getChartDiv: function(chartId) {
            var divAttrs = [ATTR_ID, chartId];
            divAttrs.push(STYLE);
            var style = "";
            var width = this.getChartWidth();
            if (width) {
		if(width.endsWith("%")) {
                    style += "width:" + width + ";"
		} else {
                    if (width > 0)
			style += "width:" + width + "px;";
                    else if (width < 0)
			style += "width:" + (-width) + "%;";
                    else
			style += "width:" + width + ";";
		}
            } else {
                style += "width:" + "100%;";
            }
	    let expandedHeight  = this.getProperty("expandedHeight");
            var height =  this.getChartHeight();
	    if(expandedHeight) {
                style += "height:" + expandedHeight+";";
	    } else {
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
	    }
	    style += "text-align:center;"
            divAttrs.push(style);
	    divAttrs.push(CLASS);
	    divAttrs.push("ramadda-expandable-target");

	    let isExpanded = this.getProperty("isExpanded");
	    let originalHeight = this.getProperty("originalHeight");
	    if(isExpanded) {
		divAttrs.push("isexpanded","true")
		divAttrs.push("original-height",originalHeight)
	    }
            return HU.div(divAttrs, "");
        },
        doMakeGoogleChart: function(dataList, props, chartDiv, selectedFields, chartOptions) {
            throw new Error("doMakeGoogleChart undefined");
        },
	makeGoogleChart: function(dataList, props, selectedFields) {
	    try {
		this.doMakeGoogleChartInner(dataList,props,selectedFields);
	    } catch(err) {
		this.setErrorMessage(err);
		console.log("Error:" + err);
		console.log(err.stack);
	    }
	},
	doMakeGoogleChartInner: function(dataList, props, selectedFields) {
            if (typeof google == 'undefined') {
                this.setContents("No google");
                return;
            }
            this.chartOptions = this.makeChartOptions(dataList, props, selectedFields);
	    this.chartOptions.bar = {groupWidth:"95%"}
            if (!Utils.isDefined(this.chartOptions.height)) {
                this.chartOptions.height = "100%";
            }

	    this.charts = [];
	    this.chartCount  = -1;


            let records = this.getPointData().getRecords();
	    if(this.getProperty("hAxisFixedRange")) {
		let x = this.getColumnValues(records, selectedFields[0]);
		this.chartOptions.hAxis.minValue = x.min;
		this.chartOptions.hAxis.maxValue = x.max;
	    }
	    if(this.getProperty("vAxisFixedRange")) {
		
		let y = this.getColumnValues(records, selectedFields[1]||selectedFields[0]);
		this.chartOptions.vAxis.minValue = y.min;
		this.chartOptions.vAxis.maxValue = y.max;
	    }

//	    console.log(JSON.stringify(chartOptions, null,2));

	    
	    if(this.getProperty("doMultiCharts",this.getProperty("multipleCharts",false))) {
		let multiField=this.getFieldById(null,this.getProperty("multiField"));
		let labelPosition = this.getProperty("multiChartsLabelPosition","bottom");
		let map = {};
		let groups = [];
		let tmp = [];
		dataList.map((v,idx)=>{if(idx>0) tmp.push(v)});
		if(!multiField) {
		    tmp.sort(function(a,b) {
			var v1 = a.record?a.record.getDate():a.date;
			var v2 = b.record?b.record.getDate():b.date;
			return v1.getTime()-v2.getTime();
		    });
		}
		dataList = Utils.mergeLists([dataList[0]], tmp);
		dataList.map((v,idx)=>{
		    if(idx==0) return;
                    var record = v.record;
		    var groupValue = record?multiField?record.getValue(multiField.getIndex()):record.getDate():v.date;
		    let list=null;
		    list = map[groupValue];
		    if(!list) {
			list = [];
			map[groupValue] = list;
			groups.push(groupValue);
		    }
		    list.push(v);
		})
		this.jq(ID_CHARTS).html(HU.div([ID,this.getDomId(ID_CHARTS_INNER),STYLE,HU.css('text-align','center')]));
		let multiStyle="width:200px;" + this.getProperty("multiStyle","");
		let multiLabelTemplate=this.getProperty("multiLabelTemplate","${value}");
		if(multiField) groups.sort();
		groups.forEach((groupValue,idx)=>{
		    this.chartCount  =idx;
		    let tmpDataList = [];
		    let list = map[groupValue];
		    tmpDataList.push(dataList[0]);
		    tmpDataList = Utils.mergeLists(tmpDataList,list);
		    var innerId = this.getDomId(ID_CHART)+"_" + this.chartCount;
		    var label = groupValue;
		    if(groupValue.getTime) label = this.formatDate(groupValue);
		    label = multiLabelTemplate.replace("${value}",label);
		    var header = HU.div([CLASS,"display-multi-header"], label);
		    var top =labelPosition=="top"?header:"";
		    var bottom = labelPosition=="bottom"?header:"";
		    var div = HU.div([CLASS,"display-multi-div", STYLE,HU.css('display','inline-block')+ multiStyle], top + this.getChartDiv(innerId) + bottom);
		    this.jq(ID_CHARTS_INNER).append(div);
		    let chart = this.makeGoogleChartInner(tmpDataList, innerId, props, selectedFields);
		    if(chart) this.charts.push(chart);
		});
	    } else {
		this.jq(ID_CHARTS).append(this.getChartDiv(this.getDomId(ID_CHART)));
		let chart = this.makeGoogleChartInner(dataList, this.getDomId(ID_CHART), props, selectedFields);
		if(chart) this.charts.push(chart);
	    }

	},
	makeGoogleChartInner: function(dataList, chartId, props, selectedFields) {
	    let chartDiv = document.getElementById(chartId);
//	    console.log(JSON.stringify(this.chartOptions, null, 2));
	    var dataTable = this.makeDataTable(dataList, props, selectedFields, this.chartOptions);
            let chart = this.doMakeGoogleChart(dataList, props, chartDiv, selectedFields, this.chartOptions);
            if (chart == null) return null;
            if (!dataTable) {
                this.setContents(this.getMessage("No data available"));
                return null;
            }
	    if(this.getProperty("animation",false,true)) {
		this.chartOptions.animation = {
		    startup: true,
		    duration:parseFloat(this.getProperty("animationDuration",1000,true)),
		    easing:this.getProperty("animationEasing","linear",true)
		};
		HU.callWhenScrolled(this.getDomId(ID_CHART),()=>{
		    if(!this.animationCalled) {
			this.animationCalled = true;
			this.mapCharts(chart=>{
			    chart.draw(dataTable, this.chartOptions);
			});
		    }
		});
	    } else {
		chart.draw(dataTable, this.chartOptions);
	    }
	    this.addEvents(chart);
	    return chart;
	},
	addEvents: function(chart) {
            let theDisplay = this;
	    google.visualization.events.addListener(chart, 'onmouseover', function(event) {
                mapVar = theDisplay.getProperty("mapVar", null);
                if (!Utils.stringDefined(mapVar)) {
                    return;
                }
                row = event.row;
                pointData = theDisplay.dataCollection.getList()[0];
                var fields = pointData.getRecordFields();
                var records = pointData.getRecords();
                var record = records[row];
                map = ramaddaMapMap[mapVar];
                if (map) {
                    if (theDisplay.mouseOverPoint)
                        map.removePoint(theDisplay.mouseOverPoint);
                } else {}
                if (record && map) {
                    latField = null;
                    lonField = null;
                    for (i = 0; i < fields.length; i++) {
                        if (fields[i].isFieldLatitude()) latField = fields[i];
                        else if (fields[i].isFieldLongitude()) lonField = fields[i];
                    }
                    if (latField && lonField) {
                        lat = record.getValue(latField.getIndex());
                        lon = record.getValue(lonField.getIndex());
                        theDisplay.mouseOverPoint = map.addPoint(chartId, new OpenLayers.LonLat(lon, lat));
                    }
                }
            });
            google.visualization.events.addListener(chart, 'select', function(event) {
                theDisplay.mapCharts(chart=>{
		    if (chart.getSelection) {
			var selected = chart.getSelection();
			if (selected && selected.length > 0) {
                            var index = selected[0].row;
			    var record = theDisplay.indexToRecord[index];
			    if(record) {
				theDisplay.getDisplayManager().notifyEvent("handleEventRecordSelection", theDisplay, {record: record});
			    }
			}
		    }});
            });
	}


    });
}



function RamaddaAxisChart(displayManager, id, chartType, properties) {
    let SUPER = new RamaddaGoogleChart(displayManager, id, chartType, properties);
    RamaddaUtil.inherit(this, SUPER);
    $.extend(this, {
	getWikiEditorTags: function() {
	    var t = SUPER.getWikiEditorTags();
	    var myTags = [
		"label:Chart Attributes",
		'indexField=field',
		"vAxisMinValue=\"\"",
		"vAxisMaxValue=\"\"", 
		'tooltipFields=""',
		'annotations="date,label,desc;date,label,desc; e.g. 2008-09-29,A,Start of housing crash;2008-11-04,B,Obama elected;"',
		'annotationFields=""',	
		'annotationLabelField=""',
		'indexField="alternate field to use as index"',
		'forceStrings="if index is a string set to true"',
		'inlinelabel:Multiples',
		'doMultiCharts=true',
		'multiField=field',
		'multiStyle=""',
		'multiLabelTemplate="${value}"',
		'multiChartsLabelPosition=bottom|top|none',
		'inlinelabel:Chart Layout',
		"chartHeight=\"\"",
		"chartWidth=\"\"",
		"chartLeft=\"0\"",
		"chartRight=\"0\"",
		"chartTop=\"0\"",
		"chartBottom=\"0\"",
		"inlinelabel:Misc Options",
		'lineColor=""',
		'chartBackground=""',
		'chart.fill=""',
		'chartArea.fill=""',
		'chart.stroke=""',
		'chart.strokeWidth=""',
		'chartArea.fill=""',
		'chartArea.stroke=""',
		'chartArea.strokeWidth=""',
		'gridlines.color="transparent"',
		'minorGridLines.color="transparent"',
		'gridlines.color=""',
		'hAxis.gridlines.color=""',
		'hAxis.minorGridlines.color="transparent"',
		'baselineColor=""',
		'hAxis.baselineColor=""',
		'gridlines.color=""',
		'vAxis.gridlines.color=""',
		'vAxis.minorGridlines.color="transparent"',
		'baselineColor=""',
		'vAxis.baselineColor=""',
		'textColor="#000"',
		'textBold="true"',
		'axis.text.color="#000"',
		'hAxis.text.color="#000"',
		'axis.text.color="#000"',
		'vAxis.text.color="#000"',
		'hAxis.text.bold="false"',
		'vAxis.text.bold="false"',
		'vAxisText=""',
		'vAxis.text=""',
		'slantedText="true"',
		'hAxis.slantedText=""',
		'hAxis.text.color="#000"',
		'vAxis.text.color="#000"',
		'legend.position="top|bottom|none"',
		'legend.text.color="#000"',
		'hAxis.ticks=""',
		'hAxis.ticks=""',
		'vAxis.ticks=""',
		'vAxis.ticks=""',
		'useMultipleAxes="true"',
		'showTrendLines="true"',



	    ]
	    myTags.map(tag=>t.push(tag));
	    return t;
	},

	setChartArea: function(chartOptions) {
            if (!chartOptions.chartArea) {
                chartOptions.chartArea = {};
            }
	    $.extend(chartOptions.chartArea, {
                left: this.getProperty("chartLeft", this.chartDimensions.left),
                right: this.getProperty("chartRight", this.chartDimensions.right),
                top: this.getProperty("chartTop", "10"),
		bottom: this.getProperty("chartBottom"),
                height: this.getProperty("chartHeight", "70%"),
                width: this.getProperty("chartWidth", this.chartDimensions.width),
            });
	},

        makeChartOptions: function(dataList, props, selectedFields) {
            chartOptions = SUPER.makeChartOptions.call(this, dataList, props, selectedFields);

            var useMultipleAxes = this.getProperty("useMultipleAxes", true);
	    let expandedHeight  = this.getProperty("expandedHeight");
            chartOptions.height = expandedHeight || this.getProperty("chartHeight", this.getProperty("height", "150"));

            if (!chartOptions.legend)
                chartOptions.legend = {};

	    this.setPropertyOn(chartOptions.legend, "legend.position", "position", this.getProperty("legendPosition", 'bottom'));
	    this.setChartArea(chartOptions);
	    
            if (useMultipleAxes) {
                $.extend(chartOptions, {
                    series: [{
                        targetAxisIndex: 0
                    }, {
                        targetAxisIndex: 1
                    }]
                });
            }

	    if (!chartOptions.hAxis) {
		chartOptions.hAxis = {};
	    }
	    if (!chartOptions.vAxis) {
		chartOptions.vAxis = {};
	    }
	    chartOptions.hAxis.textPosition = this.getProperty("hAxisTextPosition");
	    chartOptions.vAxis.textPosition = this.getProperty("vAxisTextPosition");

	    //	    console.log(JSON.stringify(chartOptions,null, 2));

            if (this.getProperty("hAxisTitle")) {
                chartOptions.hAxis.title = this.getProperty("hAxisTitle");
            }
            if (this.getProperty("vAxisTitle")) {
                chartOptions.vAxis.title = this.getProperty("vAxisTitle");
            }
            if (Utils.isDefined(this.chartHeight)) {
                chartOptions.height = this.chartHeight;
            }

            return chartOptions;

        }
    });

}



function RamaddaSeriesChart(displayManager, id, chartType, properties) {
    let SUPER = new RamaddaAxisChart(displayManager, id, chartType, properties);
    RamaddaUtil.inherit(this, SUPER);
    $.extend(this, {
        includeIndexInData: function() {
            return this.getProperty("includeIndex", true);
        },
        trendLineEnabled: function() {
            return true;
        },
    });
}


function LinechartDisplay(displayManager, id, properties) {
    RamaddaUtil.inherit(this, new RamaddaSeriesChart(displayManager, id, DISPLAY_LINECHART, properties));
    addRamaddaDisplay(this);
    $.extend(this, {
        doMakeGoogleChart: function(dataList, props, chartDiv, selectedFields, chartOptions) {
            return new google.visualization.LineChart(chartDiv);
        },
    });
}




function AreachartDisplay(displayManager, id, properties) {
    let SUPER = new RamaddaSeriesChart(displayManager, id, DISPLAY_AREACHART, properties);
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);
    $.extend(this, {
	getWikiEditorTags: function() {
	    return Utils.mergeLists(SUPER.getWikiEditorTags(),
				    [
					"isStacked=true"])},

        doMakeGoogleChart: function(dataList, props, chartDiv,  selectedFields, chartOptions) {
            if (this.isStacked)
                chartOptions.isStacked = true;
            return new google.visualization.AreaChart(chartDiv);
        }
    });
}


function RamaddaBaseBarchart(displayManager, id, type, properties) {
    let SUPER  = new RamaddaSeriesChart(displayManager, id, type, properties);
    RamaddaUtil.inherit(this, SUPER);
    $.extend(this, {
	getWikiEditorTags: function() {
	    return Utils.mergeLists(SUPER.getWikiEditorTags(),
				    [
					//"inlinelabel:Bar Chart",
				    ])},

        canDoGroupBy: function() {
            return true;
        },
        makeChartOptions: function(dataList, props, selectedFields) {
            chartOptions = SUPER.makeChartOptions.call(this, dataList, props, selectedFields);
            var chartType = this.getChartType();
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
        }
    });
}


function BarchartDisplay(displayManager, id, properties) {
    RamaddaUtil.inherit(this, new RamaddaBaseBarchart(displayManager, id, DISPLAY_BARCHART, properties));
    addRamaddaDisplay(this);
}

function BarstackDisplay(displayManager, id, properties) {
    properties = $.extend({
        "isStacked": true
    }, properties);
    RamaddaUtil.inherit(this, new RamaddaBaseBarchart(displayManager, id, DISPLAY_BARSTACK, properties));
    addRamaddaDisplay(this);
}


function HistogramDisplay(displayManager, id, properties) {
    let SUPER =  new RamaddaGoogleChart(displayManager, id, DISPLAY_HISTOGRAM, properties);
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);
    RamaddaUtil.inherit(this, {
	getWikiEditorTags: function() {
	    return Utils.mergeLists(SUPER.getWikiEditorTags(),
				    ["label:Histogram Attributes",
				     'legendPosition="none|top|right|left|bottom"',
				     'textPosition="out|in|none"',
				     'isStacked="false|true|percent|relative"',
				     'logScale="true|false"',
				     'scaleType="log|mirrorLog"',
				     'minValue=""',
				     'maxValue=""'])},

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
            var isStacked = this.getProperty("isStacked", null);
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
    let SUPER = new RamaddaGoogleChart(displayManager, id, chartType, properties);
    RamaddaUtil.inherit(this, SUPER);
    $.extend(this, {
        getFieldsToSelect: function(pointData) {
            return pointData.getNonGeoFields();
        },
    });
}




function PiechartDisplay(displayManager, id, properties) {
    let ID_PIE_LEGEND = "pielegend";
    let SUPER = new RamaddaTextChart(displayManager, id, DISPLAY_PIECHART, properties);
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);
    $.extend(this, {
	uniqueValues:[],
	uniqueValuesMap:{},
        canDoGroupBy: function() {
            return true;
        },
	makeGoogleChart: function(dataList, props, selectedFields) {
	    this.uniqueValues = [];
	    this.uniqueValuesMap = {};
	    SUPER.makeGoogleChart.call(this, dataList, props, selectedFields);
	    if(!this.getProperty("showTopLegend")) return;
	    let legend = "";
	    let colors = this.getColorList();
	    let colorCnt = 0;
	    this.uniqueValues.map((v,idx)=>{
		if(colorCnt>=colors.length) colorCnt = 0;
		var color  = colors[colorCnt];
		legend += HU.div([STYLE,HU.css('display','inline-block','width','8px','height','8px','background', color)]) +SPACE + v +SPACE2;
		colorCnt++;
	    });
	    if(this.jq(ID_PIE_LEGEND).length==0) {
		this.jq(ID_HEADER2).append(HU.div([ID,this.getDomId(ID_PIE_LEGEND)]));
	    }
	    this.jq(ID_PIE_LEGEND).html(legend);

	},
	getWikiEditorTags: function() {
	    return Utils.mergeLists(SUPER.getWikiEditorTags(),
				    [
					"label:Pie Chart Attributes",
					'groupBy=""',
					'groupByCount=true',
					'groupByCountLabel=""',
					'binCount=true',
					"pieHole=\"0.5\"",
					"is3D=\"true\"",
					"bins=\"\"",
					"binMin=\"\"",
					"binMax=\"max\"",
					"sliceVisibilityThreshold=\"0.01\"",
					'pieSliceTextColor=black',
					'pieSliceBorderColor=black'
				    ]);
	},
        setChartSelection: function(index) {
	    //noop
	},
        getGroupBy: function() {
            if (!this.groupBy && this.groupBy != "") {
                var stringField = this.getFieldOfType(this.allFields, "string");
                if (stringField) {
                    this.groupBy = stringField.getId();
                }
            }
            return this.groupBy;
        },
        getChartDiv: function(chartId) {
            var divAttrs = [ATTR_ID, chartId];
            divAttrs.push(STYLE);
            var style = "";
	    var width = this.getProperty("chartWidth") || this.getChartWidth();
	    var height = this.getProperty("chartHeight") || this.getChartHeight();
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
            if (this.getProperty("bins", null)) {
                chartOptions.title = "Bins: " + this.getDataValues(dataList[0])[1];
	    } else if(this.getProperty("sumFields")) {
                chartOptions.title = this.getProperty("chartTitle","Categories/Values");
            } else {
                chartOptions.title = this.getDataValues(dataList[0])[0] + " - " + this.getDataValues(dataList[0])[1];
            }

            if (this.is3D) {
                chartOptions.is3D = true;
            }
            if (this.pieHole) {
                chartOptions.pieHole = this.pieHole;
            }
            if (this.sliceVisibilityThreshold) {
                chartOptions.sliceVisibilityThreshold = this.sliceVisibilityThreshold;
            }

	    chartOptions.pieSliceBorderColor = this.getProperty("pieSliceBorderColor","transparent");
	    chartOptions.pieSliceTextStyle  = {
		color: this.getProperty("pieSliceTextColor","white")
            };

	    chartOptions.chartArea = {};
	    $.extend(chartOptions.chartArea, {
                left: this.getProperty("chartLeft", 0),
                right: this.getProperty("chartRight", 0),
                top: this.getProperty("chartTop", 0),
		bottom: this.getProperty("chartBottom",0),
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
            var dataTable = new google.visualization.DataTable();
            var list = [];
            var header = this.getDataValues(dataList[0]);
            dataTable.addColumn("string", header[0]);
            dataTable.addColumn("number", header[1]);
            if (this.getProperty("bins", null)) {
                var bins = parseInt(this.getProperty("bins", null));
                var min = Number.MAX_VALUE;
                var max = Number.MIN_VALUE;
                var haveMin = false;
                var haveMax = false;
                if (this.getProperty("binMin")) {
                    min = parseFloat(this.getProperty("binMin"));
                    haveMin = true;
                }
                if (this.getProperty("binMax")) {
                    max = parseFloat(this.getProperty("binMax"));
                    haveMax = true;
                }

                var goodValues = [];
                for (var i = 1; i < dataList.length; i++) {
                    var tuple = this.getDataValues(dataList[i]);
                    var value = tuple[1];
                    if (!Utils.isRealNumber(value)) {
                        continue;
                    }
                    if (!haveMin)
                        min = Math.min(value, min);
                    if (!haveMax)
                        max = Math.max(value, max);
                    goodValues.push(value);
                }

                var binList = [];
                var step = (max - min) / bins;
                for (var binIdx = 0; binIdx < bins; binIdx++) {
                    binList.push({
                        min: min + binIdx * step,
                        max: min + (binIdx + 1) * step,
                        values: []
                    });
                }

                for (var rowIdx = 0; rowIdx < goodValues.length; rowIdx++) {
                    var value = goodValues[rowIdx];
                    var ok = false;

                    for (var binIdx = 0; binIdx < binList.length; binIdx++) {
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
                for (var binIdx = 0; binIdx < bins; binIdx++) {
                    var bin = binList[binIdx];
                    list.push(["Bin:" + this.formatNumber(bin.min) + "-" + this.formatNumber(bin.max),
                               bin.values.length
			      ]);
                }
            } else if(this.getProperty("sumFields")) {
		dataTable = new google.visualization.DataTable();
		dataTable.addColumn("string", "Category");
		dataTable.addColumn("number", "Value");
		var records=  this.filterData();
		let sumFields =  this.getFieldsByIds(null, this.getProperty("sumFields"));
		let sums = [];
		sumFields.map(f=>{sums.push(0)});
		if(this.chartCount>=0) {
		    records = [records[this.chartCount]];
		}
                records.map(record=>{
		    sumFields.map((f,idx)=>{
			var v = record.getValue(f.getIndex());
			if(!isNaN(v))  sums[idx]+=v;
		    });
		});
		sumFields.map((f,idx)=>{
                    list.push([f.getLabel(),sums[idx]>0?sums[idx]:0]);
		});

            } else {
                for (var i = 1; i < dataList.length; i++) {
                    var tuple = this.getDataValues(dataList[i]);
                    var s = "" + (tuple.length == 1 ? "#" + i : tuple[0]);
                    var v = tuple.length == 1 ? tuple[0] : tuple[1];
                    list.push([s, v]);
                }
            }
	    list.map(tuple=>{
		var s = tuple[0];
		if(!this.uniqueValuesMap[s]) {
		    this.uniqueValuesMap[s] = true;
		    this.uniqueValues.push(s);
		}
	    });
	    //	    list =[];
	    //	    for(i=0;i<20;i++)list.push([""+i,5]);


            dataTable.addRows(list);
            return dataTable;
        }
    });


}


//TODO: this is broken because we don't load the sankey package because it loads an old version of d3
function SankeyDisplay(displayManager, id, properties) {
    RamaddaUtil.inherit(this, new RamaddaTextChart(displayManager, id, DISPLAY_SANKEY, properties));
    addRamaddaDisplay(this);
    $.extend(this, {
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
	    //		dataList = this.filterData(dataList, selectedFields,false,true);
            if (!this.getProperty("doCategories", false)) {
                var values = this.makeDataArray(dataList);
                return google.visualization.arrayToDataTable(values);
            }
            var strings = [];
            for (var i = 0; i < selectedFields.length; i++) {
                var field = selectedFields[i];
                if (field.isFieldString()) {
                    strings.push(field);
                }
            }
            var values = [];
            values.push(["characteristic 1", "characteristic 2", "value"]);
            for (var i = 1; i < strings.length; i++) {
                var field1 = strings[i - 1];
                var field2 = strings[i];
                var cnts = {};
                for (var r = 1; r < dataList.length; r++) {
                    var row = this.getDataValues(dataList[r]);
                    var value1 = row[i - 1];
                    var value2 = row[i];
                    var key = value1 + "-" + value2;
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
    RamaddaUtil.inherit(this, new RamaddaTextChart(displayManager, id, DISPLAY_WORDTREE, properties));
    addRamaddaDisplay(this);
    $.extend(this, {
        handleEventRecordSelection: function(source, args) {},
        doMakeGoogleChart: function(dataList, props, chartDiv, selectedFields, chartOptions) {
            if (this.getProperty("chartHeight"))
                chartOptions.height = parseInt(this.getProperty("chartHeight"));
            if (this.getProperty("wordColors")) {
                var tmp = this.getProperty("wordColors").split(",");
                var colors = [];
                for (var i = 0; i < 3 && i < tmp.length; i++) {
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
                word: this.getProperty("treeRoot", "root"),
                //                    type: this.getProperty("treeType","double")

            }
            if (this.getProperty("maxFontSize")) {
                chartOptions.maxFontSize = parseInt(this.getProperty("maxFontSize"));
            }

            return new google.visualization.WordTree(chartDiv); 
        },


        makeDataTable: function(dataList, props, selectedFields) {
            //null ->get all data
            var root = this.getProperty("treeRoot", "root");
            var records = this.filterData(null, selectedFields, false,true);
            var fields = this.getSelectedFields(this.getData().getRecordFields());
            var valueField = this.getFieldById(null, this.getProperty("colorBy"));
            var values = [];
            var typeTuple = ["phrases"];
            values.push(typeTuple);
            var fixedSize = this.getProperty("fixedSize");
            if (valueField)
                fixedSize = 1;
            if (fixedSize) typeTuple.push("size");
            if (valueField)
                typeTuple.push("value");
            var fieldInfo = {};

            var header = "";
            for (var i = 0; i < fields.length; i++) {
                var field = fields[i];
                if (header != "")
                    header += " -&gt;";
                header += field.getLabel();
                if (!field.isFieldNumeric()) continue;
                var column = this.getColumnValues(records, field);
                var buckets = [];
                var argBuckets = this.getProperty("buckets." + field.getId(), this.getProperty("buckets", null));
                var min, max;
                if (argBuckets) {
                    var argBucketLabels = this.getProperty("bucketLabels." + field.getId(), this.getProperty("bucketLabels", null));
                    var bucketLabels;
                    if (argBucketLabels)
                        bucketLabels = argBucketLabels.split(",");
                    var bucketList = argBuckets.split(",");
                    var prevValue = 0;
                    for (var bucketIdx = 0; bucketIdx < bucketList.length; bucketIdx++) {
                        var v = parseFloat(bucketList[bucketIdx]);
                        if (bucketIdx == 0) {
                            min = v;
                            max = v;
                        }
                        min = Math.min(min, v);
                        max = Math.max(max, v);
                        if (bucketIdx > 0) {
                            var label;
                            if (bucketLabels && i <= bucketLabels.length)
                                label = bucketLabels[bucketIdx - 1];
                            else
                                label = Utils.formatNumber(prevValue, true) + "-" + Utils.formatNumber(v, true);
                            buckets.push({
                                min: prevValue,
                                max: v,
                                label: label
                            });
                        }
                        prevValue = v;
                    }
                } else {
                    var numBuckets = parseInt(this.getProperty("numBuckets." + field.getId(), this.getProperty("numBuckets", 10)));
                    min = column.min;
                    max = column.max;
                    var step = (column.max - column.min) / numBuckets;
                    for (var bucketIdx = 0; bucketIdx < numBuckets; bucketIdx++) {
                        var r1 = column.min + (bucketIdx * step);
                        var r2 = column.min + ((bucketIdx + 1) * step);
                        var label = Utils.formatNumber(r1, true) + "-" + Utils.formatNumber(r2, true);
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

            var sep = "_SEP_";
            for (var rowIdx = 0; rowIdx < records.length; rowIdx++) {
                var row = this.getDataValues(records[rowIdx]);
                var string = root;
                for (var fieldIdx = 0; fieldIdx < fields.length; fieldIdx++) {
                    var field = fields[fieldIdx];
                    string += sep;
                    var value = row[field.getIndex()];
                    if (field.isFieldNumeric()) {
                        var info = fieldInfo[field.getId()];
                        for (var bucketIdx = 0; bucketIdx < info.buckets.length; bucketIdx++) {
                            var bucket = info.buckets[bucketIdx];
                            if (value >= bucket.min && value <= bucket.max) {
                                value = bucket.label;
                                break;
                            }
                        }
                    }
                    string += value;
                }
                var data = [string.trim()];
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
    let SUPER = new RamaddaTextChart(displayManager, id, DISPLAY_TABLE, properties);
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);
    $.extend(this, {
	getWikiEditorTags: function() {
	    return Utils.mergeLists(SUPER.getWikiEditorTags(),
				    [
					"label:Table Attributes",
					'tableWidth=100%',
					'frozenColumns=1',
					'showRowNumber=true',
					'maxHeaderLength=60',
					'maxHeaderWidth=60',
					'headerStyle=""']); 
	},

        canDoGroupBy: function() {
            return true;
        },
        defaultSelectedToAll: function() {
            return true;
        },
        getDataTableValueGetter: function() {
	    let unhighlightColor = this.getProperty("unhighlightColor","#fff");
	    let highlightColor = this.getProperty("highlightColor","#FFFFCC");
	    return  (v,record)=>{
		let f = v;
		if(v.f) {
		    f = v.f;
		    v = v.v;
		}
		if(!this.highlightFilter || !record) {
		    f = HU.div([STYLE,HU.css('padding','4px')],f)
		} else {
		    let c = record.isHighlight(this) ? highlightColor: unhighlightColor;
		    f = HU.div([STYLE,HU.css('padding','4px','background', c)],f)
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
                    var height = this.getProperty("height", null);
                    if (height) {
			chartOptions.height = height;
                    }
		}
		if (chartOptions.height == null) {
                    chartOptions.height = "300px";
		}
	    }


//	    console.log(JSON.stringify(chartOptions,null,2));
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
            return new google.visualization.Table(chartDiv); 
        },
	getAddToolTip: function() {
	    return false;
	},
	getAddStyle: function() {
	    return false;
	},
	getFormatNumbers: function() {
	    return true;
	},
	formatNumber: function(n) {
	    if(isNaN(n))
                return this.getProperty("nanValue", "--");
	    return SUPER.formatNumber.call(this, n);
	},
        xxxxmakeDataTable: function(dataList, props, selectedFields) {
            var rows = this.makeDataArray(dataList);
            var data = [];
            for (var rowIdx = 0; rowIdx < rows.length; rowIdx++) {
                var row = rows[rowIdx];
                for (var colIdx = 0; colIdx < row.length; colIdx++) {
		    var t = (typeof row[colIdx]);
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
    let SUPER = new RamaddaTextChart(displayManager, id, DISPLAY_BUBBLE, properties);
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);
    $.extend(this, {
	getWikiEditorTags: function() {
	    return Utils.mergeLists(SUPER.getWikiEditorTags(),
				    [
					'label:Bubble Chart Attibutes',
					'legendPosition="none|top|right|left|bottom"',
					'hAxisFormat="none|decimal|scientific|percent|short|long"',
					'vAxisFormat="none|decimal|scientific|percent|short|long"',
					'hAxisTitle=""',
					'vAxisTitle=""'])},

        getChartDiv: function(chartId) {
            var divAttrs = [ATTR_ID, chartId];
            divAttrs.push(STYLE);
            var style = "";
	    var width = this.getProperty("chartWidth") || this.getChartWidth();
	    var height = this.getProperty("chartHeight") || this.getChartHeight();
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

        makeDataTable: function(dataList, props, selectedFields, chartOptions) {
	    let debug =displayDebug.makeDataTable;
	    if(debug) {
		console.log(this.type+" makeDataTable #records:" + dataList.length);
                var fields = this.getSelectedFields();
		console.log("\t fields:" + fields);
	    }
	    var tmp =[];
	    var a = this.makeDataArray(dataList);
	    while(a[0].length<5)
		a[0].push("");
	    tmp.push(a[0]);
	    //Remove nans
	    this.didUnhighlight = false;
	    let minColorValue = Number.MAX_SAFE_INTEGER;
	    for(var i=1;i<a.length;i++) {
		var tuple = a[i];
		while(tuple.length<5) {
		    tuple.push(1);
		}
		minColorValue = Math.min(minColorValue, tuple[3]);
	    }


	    for(var i=1;i<a.length;i++) {
		var tuple = a[i];
		while(tuple.length<5)
		    tuple.push(1);
		if(debug && i<5)
		    console.log("\tdata:" + tuple);
		var ok = true;
		for(j=1;j<tuple.length && ok;j++) {
		    if(isNaN(tuple[j])) ok = false;
		}
		//If highlighting and have color then set to NaN
		if(this.highlightFilter) {
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
            var ct = this.getColorTable(true);
            if (ct) {
                chartOptions.colors = ct;
            } else if (!this.colors) {
                chartOptions.colors = this.getColorList();
            }
            if (chartOptions.colors) {
                chartOptions.colors = Utils.getColorTable("rainbow", true);
            }
	    $.extend(chartOptions.chartArea, {
                left: this.getProperty("chartLeft", this.chartDimensions.left),
                right: this.getProperty("chartRight", this.chartDimensions.right),
                top: this.getProperty("chartTop", "10"),
		bottom: this.getProperty("chartBottom",40),
//                width: this.getProperty("chartWidth", '98%'),
                height: this.getProperty("chartHeight", '200')
            });
            chartOptions.height = "100px";
            chartOptions.sizeAxis = {
	    }

            chartOptions.colorAxis = {
                legend: {
                    position: this.getProperty("legendPosition", "in")
                }
            }
	    var colorTable = this.getColorTable(true);
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
    let SUPER = new RamaddaSeriesChart(displayManager, id, DISPLAY_BARTABLE, properties);
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);
    $.extend(this, {
        doMakeGoogleChart: function(dataList, props, chartDiv, selectedFields, chartOptions) {
            var height = "";
            if (Utils.isDefined(this.chartHeight)) {
                height = this.chartHeight;
            } else {
                if (dataList.length > 1) {
                    var numBars = dataList.length;
                    if (this.isStacked) {
                        height = numBars * 22;
                    } else {
                        height = numBars * 22 + numBars * 14 * (this.getDataValues(dataList[0]).length - 2);
                    }
                }
            }

            $.extend(chartOptions, {
                title: "the title",
                bars: 'horizontal',
                colors: this.getColorList(),
                width: (Utils.isDefined(this.chartWidth) ? this.chartWidth : "100%"),
                chartArea: {
                    left: '30%',
                    top: 0,
                    width: '70%',
                    height: '80%'
                },
                height: height,
                bars: 'horizontal',
                tooltip: {
                    showColorCode: true,
                },
                legend: {
                    position: 'none'
                },
            });

            if (Utils.isDefined(this.isStacked)) {
                chartOptions.isStacked = this.isStacked;
            }

            if (this.hAxis)
                chartOptions.hAxis = {
                    title: this.hAxis
                };
            if (this.vAxis)
                chartOptions.vAxis = {
                    title: this.vAxis
                };
            return new google.charts.Bar(chartDiv); 
        },
        getDefaultSelectedFields: function(fields, dfltList) {
            var f = [];
            for (i = 0; i < fields.length; i++) {
                var field = fields[i];
                if (!field.isNumeric()) {
                    f.push(field);
                    break;
                }
            }
            for (i = 0; i < fields.length; i++) {
                var field = fields[i];
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
    let SUPER = new RamaddaTextChart(displayManager, id, DISPLAY_TREEMAP, properties);
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);
    $.extend(this, {
        handleEventRecordSelection: function(source, args) {},
        getFieldsToSelect: function(pointData) {
            return pointData.getRecordFields();
        },
        tooltips: {},
        makeChartOptions: function(dataList, props, selectedFields) {
            let _this = this;
            var tooltip = function(row, size, value) {
                if (_this.tooltips[row]) {
                    return _this.tooltips[row];
                }
                return "<div class='display-treemap-tooltip-outer'><div class='display-treemap-tooltip''><i>left-click: go down<br>right-click: go up</i></div></div>";
            };
            var chartOptions = SUPER.makeChartOptions.call(this, dataList, props, selectedFields);
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
            var dataTable = this.makeDataTable(dataList, props, selectedFields);
            if (!dataTable) return null;
            return new google.visualization.TreeMap(chartDiv);
        },

        addTuple: function(data, colorField, seen, value, parent, n1, n2) {
            var ovalue = value;
            var cnt = 0;
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
            var tuple = [value, parent, n1];
            if (colorField) tuple.push(n2);
            data.push(tuple);
            return value;
        },

        valueClicked: function(field, value) {
            var allFields = this.getData().getRecordFields();
            field = this.getFieldById(allFields, field);
            this.propagateEvent("handleEventFieldValueSelect", {
                field: field,
                value: value
            });
        },
        makeDataTable: function(dataList, props, selectedFields) {
            var records = this.filterData(null,null,false,true);
            if (!records) {
                return null;
            }
            var allFields = this.getData().getRecordFields();
            var fields = this.getSelectedFields(allFields);
            if (fields.length == 0)
                fields = allFields;
            var strings = this.getFieldsOfType(fields, "string");
            if (strings.length < 2) {
                this.displayError("No string fields specified");
                return null;
            }
            var addPrefix = this.getProperty("addPrefix", true);
            var sizeField = this.getFieldById(allFields, this.getProperty("sizeBy"));
            var colorField = this.getFieldById(allFields, this.getProperty("colorBy"));
            var values = this.getFieldsOfType(fields, "numeric");
            if (!sizeField && values.length > 0)
                sizeField = values[0];
            if (!colorField && values.length > 1)
                colorField = values[1];

            var tooltipFields = [];
            var toks = this.getProperty("tooltipFields", "").split(",");
            for (var i = 0; i < toks.length; i++) {
                var tooltipField = this.getFieldById(null, toks[i]);
                if (tooltipField)
                    tooltipFields.push(tooltipField);
            }
            if (tooltipFields.length == 0) tooltipFields = allFields;

            this.tooltips = {};

            var columns = [];
            for (var fieldIndex = 0; fieldIndex < strings.length; fieldIndex++) {
                var field = strings[fieldIndex];
                columns.push(this.getColumnValues(records, field).values);
            }

            var data = [];
            var leafs = [];
            var tmptt = [];
            var seen = {};
            this.addTuple(data, colorField, {}, "Node", "Parent", "Value", "Color");
            var root = strings[0].getLabel();
            this.addTuple(data, colorField, seen, root, null, 0, 0);
            var keys = {};
            var call = this.getGet();
            for (var rowIdx = 0; rowIdx < records.length; rowIdx++) {
                //               if(rowIdx>20) break;
                var row = this.getDataValues(records[rowIdx]);
                var key = "";
                var parentKey = "";
                for (var fieldIndex = 0; fieldIndex < strings.length - 1; fieldIndex++) {
                    var values = columns[fieldIndex];
                    if (key != "")
                        key += ":";
                    key += values[rowIdx];
                    if (!Utils.isDefined(keys[key])) {
                        var parent = Utils.isDefined(keys[parentKey]) ? keys[parentKey] : root;
                        var value = values[rowIdx];
                        if (addPrefix && fieldIndex > 0)
                            value = parent + ":" + value;
                        keys[key] = this.addTuple(data, colorField, seen, value, parent, 0, 0);
                    }
                    parentKey = key;
                }
                var parent = Utils.isDefined(keys[parentKey]) ? keys[parentKey] : root;
                var value = row[strings[strings.length - 1].getIndex()];
                var size = sizeField ? row[sizeField.getIndex()] : 1;
                var color = colorField ? row[colorField.getIndex()] : 0;
                value = this.addTuple(leafs, colorField, seen, value, parent, size, color);
                var tt = "<div class='display-treemap-tooltip-outer'><div class='display-treemap-tooltip''>";
                for (var f = 0; f < tooltipFields.length; f++) {
                    var v = row[tooltipFields[f].getIndex()];
                    var field = tooltipFields[f];
                    v = HU.onClick(call + ".valueClicked('" + field.getId() + "','" + v + "')", v, []);
                    tt += HU.b(field.getLabel()) + ": " + v + "<br>";
                }
                tt += "</div></div>";
                tmptt.push(tt);
            }
            for (var i = 0; i < leafs.length; i++) {
                data.push(leafs[i]);
                this.tooltips[data.length - 2] = tmptt[i];
            }
            return google.visualization.arrayToDataTable(data);
        },
    });
}



function TimerangechartDisplay(displayManager, id, properties) {
    RamaddaUtil.inherit(this, new RamaddaTextChart(displayManager, id, DISPLAY_TIMERANGECHART, properties));
    addRamaddaDisplay(this);
    $.extend(this, {
        doMakeGoogleChart: function(dataList, props, chartDiv, selectedFields, chartOptions) {
            return new google.visualization.Timeline(chartDiv);
        },
        makeDataTable: function(dataList, props, selectedFields) {
	    var records = this.filterData(null,null,false,true);
            var strings = [];
            var stringField = this.getFieldOfType(selectedFields, "string");
            if (!stringField)
                stringField = this.getFieldOfType(null, "string");
            var showLabel = this.getProperty("showLabel", true);
            var labelFields = [];
            var labelFieldsTemplate = this.getProperty("labelFieldsTemplate");
            var toks = this.getProperty("labelFields", "").split(",");
            for (var i = 0; i < toks.length; i++) {
                var field = this.getFieldById(null, toks[i]);
                if (field)
                    labelFields.push(field);
            }
            var dateFields = this.getFieldsOfType(selectedFields, "date");
            if (dateFields.length == 0)
                dateFields = this.getFieldsOfType(null, "date");
            var values = [];
            var dataTable = new google.visualization.DataTable();
            if (dateFields.length < 2) {
                throw new Error("Need to have at least 2 date fields");
            }
            if (stringField) {
                dataTable.addColumn({
                    type: 'string',
                    id: stringField.getLabel()
                });
            } else {
                dataTable.addColumn({
                    type: 'string',
                    id: "Index"
                });
            }
            if (labelFields.length > 0) {
                dataTable.addColumn({
                    type: 'string',
                    id: 'Label'
                });
            }
            dataTable.addColumn({
                type: 'date',
                id: dateFields[0].getLabel()
            });
            dataTable.addColumn({
                type: 'date',
                id: dateFields[1].getLabel()
            });
            for (var r = 0; r < records.length; r++) {
                var row = this.getDataValues(records[r]);
                var tuple = [];
                values.push(tuple);
                if (stringField && showLabel)
                    tuple.push(row[stringField.getIndex()]);
                else
                    tuple.push("#" + (r + 1));
                if (labelFields.length > 0) {
                    var label = "";
                    if (labelFieldsTemplate)
                        label = labelFieldsTemplate;
                    for (var l = 0; l < labelFields.length; l++) {
                        var f = labelFields[l];
                        var value = row[f.getIndex()];
                        if (labelFieldsTemplate) {
                            label = label.replace("{" + f.getId() + "}", value);
                        } else {
                            label += value + " ";
                        }

                    }
                    tuple.push(label);
                }
                tuple.push(row[dateFields[0].getIndex()]);
                tuple.push(row[dateFields[1].getIndex()]);
            }
            dataTable.addRows(values);
            return dataTable;
        }
    });
}



function CalendarDisplay(displayManager, id, properties) {
    RamaddaUtil.inherit(this, new RamaddaGoogleChart(displayManager, id, DISPLAY_CALENDAR, properties));
    addRamaddaDisplay(this);
    RamaddaUtil.inherit(this, {
        doMakeGoogleChart: function(dataList, props, chartDiv, selectedFields, chartOptions) {
            chartOptions.calendar = {
                cellSize: parseInt(this.getProperty("cellSize", 15))
            };
            //If a calendar is show in tabs then it never returns from the draw
            if (this.jq(ID_CHART).width() == 0) {
                return;
            }
            return new google.visualization.Calendar(chartDiv);
        },

        defaultSelectedToAll: function() {
            return true;
        },
        xgetContentsStyle: function() {
            var height = this.getProperty("height", 200);
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
            var dataTable = new google.visualization.DataTable();
            var header = this.getDataValues(dataList[0]);
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
            var haveMissing = false;
            var missing = this.getProperty("missingValue", null);
            if (missing) {
                haveMissing = true;
                missing = parseFloat(missing);
            }
            var list = [];
            var cnt = 0;
            var options = {
                weekday: 'long',
                year: 'numeric',
                month: 'long',
                day: 'numeric'
            };
            for (var i = 1; i < dataList.length; i++) {
                var value = this.getDataValues(dataList[i])[1];
                if (value == NaN) continue;
                if (haveMissing && value == missing) {
                    continue;
                }
                cnt++;
                var dttm = this.formatDate(this.getDataValues(dataList[i])[0], {
                    options: options
                });
                dttm = dttm.replace(/ /g, SPACE);
                var tooltip = "<center><b>" + dttm + "</b></center>" +
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
    RamaddaUtil.inherit(this, new RamaddaGoogleChart(displayManager, id, DISPLAY_GAUGE, properties));
    addRamaddaDisplay(this);
    RamaddaUtil.inherit(this, {
        getChartHeight: function() {
            return this.getProperty("height", this.getChartWidth());
        },
        getChartWidth: function() {
            return this.getProperty("width", "150");
        },
        doMakeGoogleChart: function(dataList, props, chartDiv, selectedFields, chartOptions) {
            this.dataList = dataList;
            this.chartOptions = chartOptions;
            var min = Number.MAX_VALUE;
            var max = Number.MIN_VALUE;
            var setMinMax = true;
            for (var row = 1; row < dataList.length; row++) {
                var tuple = this.getDataValues(dataList[row]);
                //                        if(tuple.length>2) setMinMax = false;
                for (var col = 0; col < tuple.length; col++) {
                    if (!Utils.isNumber(tuple[col])) {
                        continue;
                    }
                    var value = tuple[col];
                    min = Math.min(min, value);
                    max = Math.max(max, value);
                }
            }
            min = Utils.formatNumber(min, true);
            max = Utils.formatNumber(max, true);
            if (Utils.isDefined(this.gaugeMin)) {
                setMinMax = true;
                min = parseFloat(this.gaugeMin);
            }
            if (Utils.isDefined(this.gaugeMax)) {
                setMinMax = true;
                max = parseFloat(this.gaugeMax);
            }
            if (setMinMax) {
                chartOptions.min = min;
                chartOptions.max = max;
            }
            return new google.visualization.Gauge(chartDiv);
        },

        makeDataTable: function(dataList, props, selectedFields) {
            dataList = this.makeDataArray(dataList);
            if (!Utils.isDefined(this.index)) this.index = dataList.length - 1;
            var index = this.index + 1;
            var list = [];
            list.push(["Label", "Value"]);
            var header = this.getDataValues(dataList[0]);
            if (index >= dataList.length) index = dataList.length - 1;
            var row = this.getDataValues(dataList[index]);
            for (var i = 0; i < row.length; i++) {
                if (!Utils.isNumber(row[i])) continue;
                var h = header[i];
                if (h.length > 20) {
                    var index = h.indexOf("(");
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
                var value = row[i];
                list.push([h, Utils.formatNumber(value, true)]);
            }
            return google.visualization.arrayToDataTable(list);
        },
        setChartSelection: function(index) {
            this.index = index;
            var dataTable = this.makeDataTable(this.dataList);
            this.mapCharts(chart=>{
                chart.draw(dataTable, this.chartOptions);
	    });
        },
    });
}




function ScatterplotDisplay(displayManager, id, properties) {
    let SUPER = new RamaddaGoogleChart(displayManager, id, DISPLAY_SCATTERPLOT, properties);
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);
    $.extend(this, {
        trendLineEnabled: function() {
            return true;
        },
        makeChartOptions: function(dataList, props, selectedFields) {
            var chartOptions = SUPER.makeChartOptions.call(this, dataList, props, selectedFields);
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
	    //	    console.log(JSON.stringify(chartOptions,null,2));

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
            var height = this.getProperty("height",400);
            if (Utils.isDefined(this.getProperty("chartHeight"))) {
                height = this.getProperty("chartHeight");
            }
            var width = "100%";
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
            var f = [];
            for (i = 0; i < fields.length; i++) {
                var field = fields[i];
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




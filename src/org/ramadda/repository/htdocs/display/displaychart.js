/**
   Copyright 2008-2019 Geode Systems LLC
*/

var CATEGORY_CHARTS = "Charts";
var CATEGORY_OTHER = "Other Charts";
var CATEGORY_MISC = "Misc";
var DISPLAY_LINECHART = "linechart";
var DISPLAY_AREACHART = "areachart";
var DISPLAY_BARCHART = "barchart";
var DISPLAY_BARTABLE = "bartable";
var DISPLAY_BARSTACK = "barstack";
var DISPLAY_PIECHART = "piechart";
var DISPLAY_TIMELINECHART = "timelinechart";
var DISPLAY_SANKEY = "sankey";
var DISPLAY_CALENDAR = "calendar";
var DISPLAY_SCATTERPLOT = "scatterplot";
var DISPLAY_HISTOGRAM = "histogram";
var DISPLAY_BUBBLE = "bubble";
var DISPLAY_GAUGE = "gauge";
var DISPLAY_STATS = "stats";
var DISPLAY_RECORDS = "records";
var DISPLAY_TABLE = "table";
var DISPLAY_CROSSTAB = "crosstab";
var DISPLAY_CORRELATION = "correlation";
var DISPLAY_RANKING = "ranking";
var DISPLAY_TSNE = "tsne";
var DISPLAY_HEATMAP = "heatmap";
var DISPLAY_WORDTREE = "wordtree";
var DISPLAY_TREEMAP = "treemap";
var ID_CHART = "chart";


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

function displayGetFunctionValue(v) {
    if(isNaN(v))return 0;
    return v;
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
    type: DISPLAY_TIMELINECHART,
    label: "Timeline",
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
    type: DISPLAY_STATS,
    label: "Stats Table",
    requiresData: false,
    forUser: true,
    category: CATEGORY_MISC
});
addGlobalDisplayType({
    type: DISPLAY_RECORDS,
    label: "Records",
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
    type: DISPLAY_RANKING,
    label: "Ranking",
    requiresData: true,
    forUser: true,
    category: CATEGORY_MISC
});
addGlobalDisplayType({
    type: DISPLAY_CORRELATION,
    label: "Correlation",
    requiresData: true,
    forUser: true,
    category: CATEGORY_MISC
});
addGlobalDisplayType({
    type: DISPLAY_CROSSTAB,
    label: "Crosstab",
    requiresData: true,
    forUser: true,
    category: CATEGORY_MISC
});
addGlobalDisplayType({
    type: DISPLAY_TSNE,
    label: "TSNE",
    requiresData: true,
    forUser: true,
    category: CATEGORY_MISC
});
addGlobalDisplayType({
    type: DISPLAY_HEATMAP,
    label: "Heatmap",
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
 */
function RamaddaFieldsDisplay(displayManager, id, type, properties) {
    let _this = this;
    this.TYPE = "RamaddaFieldsDisplay";
    let SUPER = new RamaddaDisplay(displayManager, id, type, properties);
    RamaddaUtil.inherit(this, this.RamaddaDisplay = SUPER);
    RamaddaUtil.defineMembers(this, {
        needsData: function() {
            return true;
        },
	getWikiEditorTags: function() {
	    var t = SUPER.getWikiEditorTags();
	    var myTags = [
		"fields=\"\"",
	    ];
	    myTags.map(tag=>t.push(tag));
	    return t;
	},

        handleEventMapClick: function(source, args) {
            if (!this.dataCollection) return;
            var pointData = this.dataCollection.getList();
            for (var i = 0; i < pointData.length; i++) {
                pointData[i].handleEventMapClick(this, source, args.lon, args.lat);
            }
        },
        initDisplay: function() {
            SUPER.initDisplay.call(this);
            if (this.needsData()) {
                this.setContents(this.getLoadingMessage());
            }
            this.updateUI();
        },
        updateUI: function() {
            this.addFieldsCheckboxes();
        },
        //This keeps checking the width of the chart element if its zero
        //we do this for displaying in tabs
        checkLayout: function() {
            var _this = this;
            var d = _this.jq(ID_DISPLAY_CONTENTS);
            //       console.log("checklayout:  widths:" + this.lastWidth +" " + d.width());
            if (this.lastWidth != d.width()) {
                _this.displayData();
            }
            if (true) return;

            if (d.width() == 0) {
                var cb = function() {
                    _this.checkWidth(cnt + 1);
                };
                setTimeout(cb, 5000);
            } else {
                //                    console.log("checkWidth:"+ _this.getTitle() +" calling displayData");
                _this.displayData();
            }
        },

        getWikiAttributes: function(attrs) {
            SUPER.getWikiAttributes.call(this, attrs);
            if (this.lastSelectedFields) {
                attrs.push("fields");
                var v = "";
                for (var i = 0; i < this.lastSelectedFields.length; i++) {
                    v += this.lastSelectedFields[i].getId();
                    v += ",";
                }
                attrs.push(v);
            }
        },
        initDialog: function() {
            SUPER.initDialog.call(this);
            this.addFieldsCheckboxes();
        },
        getDialogContents: function(tabTitles, tabContents) {
            var height = "600";
            var html = HtmlUtils.div([ATTR_ID, this.getDomId(ID_FIELDS), "style", "overflow-y: auto;    max-height:" + height + "px;"], " FIELDS ");
            tabTitles.push("Fields");
            tabContents.push(html);
            SUPER.getDialogContents.call(this, tabTitles, tabContents);
        },
        handleEventFieldsSelected: function(source, fields) {
            this.userHasSelectedAField = true;
            this.overrideFields = null;
            this.removeProperty(PROP_FIELDS);
            this.setSelectedFields(fields);
            this.fieldSelectionChanged();
        },
        getFieldsToSelect: function(pointData) {
            return pointData.getRecordFields();
        },
        canDoMultiFields: function() {
            return true;
        }
    })
}







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
        colorList: ['blue', 'red', 'green', 'orange', 'fuchsia', 'teal', 'navy', 'silver'],
        curveType: 'none',
        fontSize: 0,
        showPercent: false,
        percentFields: null,
    });
    if (properties.colors) {
        this.colorList = ("" + properties.colors).split(",");
    }
    let SUPER = new RamaddaFieldsDisplay(displayManager, id, chartType, properties);
    RamaddaUtil.inherit(this, SUPER);

    RamaddaUtil.defineMembers(this, {
        clearCachedData: function() {
            SUPER.clearCachedData();
            this.computedData = null;
        },
        updateUI: function() {
            SUPER.updateUI.call(this);
            if (!this.getDisplayReady()) {
                return;
            }
//	    var t1= new Date();
            this.displayData();
//	    var t2= new Date();
//	    Utils.displayTimes("chart.displayData",[t1,t2]);
        },
        getWikiAttributes: function(attrs) {
            this.defineWikiAttributes(["vAxisMinValue", "vAxisMaxValue"]);
            SUPER.getWikiAttributes.call(this, attrs);
            if (this.colorList.join(",") != "blue,red,green") {
                attrs.push("colors");
                attrs.push(this.colorList.join(", "));
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
        getMenuItems: function(menuItems) {
            SUPER.getMenuItems.call(this, menuItems);
            var get = this.getGet();
            //                menuItems.push(HtmlUtils.onClick(get+".setColor();", "Set color"));

            var min = "0";
            if (!isNaN(this.getVAxisMinValue())) {
                min = "" + this.getVAxisMinValue();
            }
            var max = "";
            if (!isNaN(this.getVAxisMaxValue())) {
                max = "" + this.getVAxisMaxValue();
            }

            var tmp = HtmlUtils.formTable();
            tmp += HtmlUtils.formEntry("Axis Range:", HtmlUtils.input("", min, ["size", "7", ATTR_ID, this.getDomId("vaxismin")]) + " - " +
				       HtmlUtils.input("", max, ["size", "7", ATTR_ID, this.getDomId("vaxismax")]));
            tmp += HtmlUtils.formEntry("Date Range:", HtmlUtils.input("", this.minDate, ["size", "10", ATTR_ID, this.getDomId("mindate")]) + " - " +
				       HtmlUtils.input("", this.maxDate, ["size", "10", ATTR_ID, this.getDomId("maxdate")]));


            tmp += HtmlUtils.formEntry("Colors:",
				       HtmlUtils.input("", this.colorList.join(","), ["size", "35", ATTR_ID, this.getDomId(ID_COLORS)]));
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
            var html = HtmlUtils.div([ATTR_ID, this.getDomId(ID_FIELDS), "style", "overflow-y: auto;    max-height:" + height + "px;"], " FIELDS ");

            if (this.trendLineEnabled()) {
                html += HtmlUtils.div([ATTR_CLASS, "display-dialog-subheader"], "Other");

                html += HtmlUtils.checkbox(this.getDomId(ID_TRENDS_CBX),
					   [],
					   this.getProperty("showTrendLines", false)) + "  " + "Show trend line";
                html += " ";
                html += HtmlUtils.checkbox(this.getDomId(ID_PERCENT_CBX),
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
	    if(!args.record) return;
	    var index = this.recordToIndex[args.record.getId()];
	    if(Utils.isDefined(index)) {
		this.setChartSelection(index);
	    }
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
            return this.groupBy;
        },
        getIncludeIndexIfDate: function() {
            return false;
        },
        displayData: function() {
            var _this = this;

            if (!this.getDisplayReady()) {
                return;
            }
            if (this.inError) {
                return;
            }
            if (!haveGoogleChartsLoaded()) {
                if (!this.googleChartCallbackPending) {
                    this.googleChartCallbackPending = true;
                    var func = function() {
                        _this.googleChartCallbackPending = false;
                        _this.displayData();
                    }
                    this.setContents(this.getLoadingMessage());
                    setTimeout(func, 500);
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

            this.setContents(HtmlUtils.div([ATTR_CLASS, "display-message"],
					   "Building display..."));


            this.allFields = this.dataCollection.getList()[0].getRecordFields();
            var pointData = this.dataCollection.getList()[0];
            //            var selectedFields = this.getSelectedFields(this.getFieldsToSelect(pointData));
            var selectedFields = this.getSelectedFields();
            if (selectedFields.length == 0 && this.lastSelectedFields != null) {
                selectedFields = this.lastSelectedFields;
            }


            if (selectedFields == null || selectedFields.length == 0) {
                if (this.getChartType() == DISPLAY_TABLE || this.getChartType() == DISPLAY_TREEMAP) {
                    selectedFields = this.dataCollection.getList()[0].getNonGeoFields();
                } else {
                    selectedFields = this.getSelectedFields();
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
                        this.groupByField = field;
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
	    //            var dataList = this.computedData;
            var dataList = null;
            if (this["function"] && dataList == null) {
                var pointData = this.dataCollection.getList()[0];
                var allFields = pointData.getRecordFields();
                var records = pointData.getRecords();
                var indexField = this.indexField;
                var chartableFields = this.getFieldsToSelect(pointData);
                this.hasDate = this.getHasDate(records);
                var date_formatter = this.getDateFormatter();
                var setVars = "";
                for (var i = 0; i < chartableFields.length; i++) {
                    var field = chartableFields[i];
                    setVars += "\tvar " + field.getId() + "=displayGetFunctionValue(args." + field.getId() + ");\n";
                }
                var code = "function displayChartEval(args) {\n" + setVars + "\treturn  " + this["function"] + "\n}";
                eval(code);
                var newList = [];
                var fieldNames = null;
                var rowCnt = -1;
                var indexField = this.getFieldById(null,this.getProperty("indexField"));
//		console.log("index:" + indexField);
		
                for (var rowIdx = 0; rowIdx < records.length; rowIdx++) {
                    var record = records[rowIdx];
                    var row = record.getData();
                    var date = record.getDate();
                    if (!this.dateInRange(date)) continue;
                    rowCnt++;
                    var values = [];
                    var indexName = null;
                    if (indexField) {
                        values.push(record.getValue(indexField.getIndex()) + offset);
                        indexName = indexField.getLabel();
                    } else {
                        if (this.hasDate) {
                            values.push(this.getDateValue(date, date_formatter));
                            indexName = "Date";
                        } else {
                            values.push(rowIdx);
                            indexName = "Index";
                        }
                    }
                    if (fieldNames == null) {
                        fieldNames = [indexName, this.functionName ? this.functionName : "value"];
                        newList.push(fieldNames);
                    }
                    var args = {};
                    for (var j = 0; j < chartableFields.length; j++) {
                        var field = chartableFields[j];
                        var value = row[field.getIndex()];
                        args[field.getId()] = value;
                    }
                    var value = displayChartEval(args);
                    values.push(value);
                    newList.push(values);
                }
                dataList = newList;
            }


            if (dataList == null) {
                dataList = this.getStandardData(fieldsToSelect, props);
            }


            this.computedData = dataList;

            if (this.rotateTable && dataList.length) {
                var header = dataList[0];
                var flipped = [];
                for (var rowIdx = 1; rowIdx < dataList.length; rowIdx++) {
                    var row = dataList[rowIdx];
                }
            }

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
                this.setContents(HtmlUtils.div([ATTR_CLASS, "display-message"],
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
                this.makeChart(dataList, props, selectedFields);
            } catch (e) {
                console.log(e.stack);
                this.displayError("" + e);
                return;
            }

            var d = _this.jq(ID_CHART);
            this.lastWidth = d.width();
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
        clearChart: function() {
            if (this.chart != null && this.chart.clearChart) {
                this.chart.clearChart();
            }
        },

        setChartSelection: function(index) {
            if (this.chart != null) {
                if (this.chart.setSelection) {
                    this.chart.setSelection([{
                        row: index,
			column:null
                    }]);
                }
            }
        },
        tableHeaderMouseover: function(i, tooltip) {},
        makeDataTable: function(dataList, props, selectedFields) {
	    //            dataList = this.filterData(dataList, selectedFields,false,true);
            if (dataList.length == 1) {
                return google.visualization.arrayToDataTable(this.makeDataArray(dataList));
            }
	    var groupField = this.getFieldById(null,  this.getProperty("group"));

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
            var dataTable = new google.visualization.DataTable();
            var header = this.getDataValues(dataList[0]);
            var sample = this.getDataValues(dataList[1]);
	    var fixedValueS = this.getProperty("fixedValue");
	    var fixedValueN;
	    if(fixedValueS) fixedValueN = parseFloat(fixedValueS);
	    var fIdx = 0;


            for (var j = 0; j < header.length; j++) {
		var field;
		if(j>0 || !props.includeIndex) {
		    field = selectedFields[fIdx++];
		}
                var value = sample[j];
                if (j == 0 && props.includeIndex) {
                    //This might be a number or a date
                    if ((typeof value) == "object") {
                        //assume its a date
			dataTable.addColumn('date', header[j]);
                    } else {
                        dataTable.addColumn((typeof value), header[j]);
                    }
                } else {
		    if(j>0 && fixedValueS) {
			dataTable.addColumn('number', this.getProperty("fixedValueLabel","Count"));
		    } else {
			if(field.isString()) {
			    dataTable.addColumn('string', header[j]);
			} else if(field.isFieldDate()) {

			    dataTable.addColumn('date', header[j]);
			} else {
			    dataTable.addColumn('number', header[j]);
			}
		    }
		    dataTable.addColumn({ type: 'string', role: 'style' });
                    dataTable.addColumn({
                        type: 'string',
                        role: 'tooltip',
                        'p': {
                            'html': true
                        }
                    });
		    if(j>0 && fixedValueS) {
			break;
		    }
                }
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
	    var annotationsMap = {};
	    if(annotations) {
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
		var toks = annotations.split(",");
		for(var i=0;i<toks.length;i++) {
		    var toks2 = toks[i].split(";");
		    //index,label,description
		    if(toks2.length!=3) continue;
		    var index = toks2[0].trim();
		    var label = toks2[1];
		    var desc = toks2[2];
		    if(index.match(/^[0-9]+$/)) {
			index = parseFloat(index);
		    } else {
			index = Utils.parseDate(index,false);
		    }
//		    console.log("annotation:" + index +" l:" + label);
		    annotationsMap[index] = {
			label: label,
			description: desc
		    }
		}
	    }


	    var annotationCnt=0;

	    var records = [];
            for (var i = 1; i < dataList.length; i++) {
		records.push(dataList[i].record);
	    }
	    var colors =  this.getColorTable(true);
            var colorBy = this.getColorByInfo(records);

	    var didColorBy = false;
            for (var i = 1; i < dataList.length; i++) {
		var record =dataList[i];
		var theRecord = dataList[i].record;
                var row = this.getDataValues(record);
		var color = "";
                if (colorBy.index >= 0) {
                    var value = theRecord.getData()[colorBy.index];
		    hasColorByValue  = true;
		    colorByValue = value;
                    didColorBy = true;
		    color =  colorBy.getColor(value, theRecord);
                }

                row = row.slice(0);
                var label = "";
                if (dataList[i].record) {
                    for (var j = 0; j < tooltipFields.length; j++) {
                        label += "<b>" + tooltipFields[j].getLabel() + "</b>: " +
                            theRecord.getValue(tooltipFields[j].getIndex()) + "<br>";
                    }
		}
		var tooltip = "";
                tooltip += label;
                for (var j = 0; j < row.length; j++) {
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
		    value = value.replace(/ /g, "&nbsp;");
		    tooltip += "<b>" + label + "</b>:&nbsp;" + value;
                }

		var tt = this.getProperty("tooltip");
		if(tt) {
		    tt  = this.getRecordHtml(theRecord,null,tt);
		    tt = tt.replace("${default}",tooltip);
		    tooltip = tt;
		}
		tooltip = "<div style='padding:8px;'>" + tooltip + "</div>";
                newRow = [];
                for (var j = 0; j < row.length; j++) {
                    var value = row[j];
		    if(j>0 && fixedValueS) {
			newRow.push(fixedValueN);
		    } else {
			newRow.push(value);
		    }
                    if (j == 0 && props.includeIndex) {
                        //is the index so don't add a tooltip
                    } else {
                        newRow.push(color);
                        newRow.push(tooltip);
                    }
		    if(j>0 && fixedValueS) {
			break;
		    }
                }
                //                    newRow.push("annotation");
	    
		if(annotationFields.length>0) {
                    if (dataList[i].record) {
			var desc = "";
			annotationFields.map(f=>{
			    var d = ""+dataList[i].record.getValue(f.getIndex());
			    if(d!="")
				desc+= (d+"<br>");
			});
			desc = desc.trim();
			desc = desc.replace(/ /g,"&nbsp;");
			annotationCnt++;
			var label = null; 
			if(desc.trim().length>0) {
			    label =""+( annotationLabelField?dataList[i].record.getValue(annotationLabelField.getIndex()):(annotationCnt))
			    if(label.trim().length==0) label = ""+annotationCnt;
			}


			newRow.push(label);
			newRow.push(desc);
		    } else {
			if(i<2)
			    console.log("No records for annotation");
		    }

		}

		if(annotations) {
		    var index = newRow[0];
		    if(index.v) index=  index.v;
		    var annotation = annotationsMap[index];
		    if(!annotation) {
			annotation = annotationsMap[i];
		    }
		    if(annotation) {
			newRow.push(annotation.label);
			newRow.push(annotation.description);
//			console.log("index:" + index +" label:" + annotation.label +" " + annotation.description);
		    } else {
			newRow.push(null);
			newRow.push(null);
		    }
		}
                justData.push(newRow);
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
                colors: this.colorList,
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
                textStyle: {}
            };
            chartOptions.vAxis = {
                gridlines: {},
                textStyle: {}
            };
            chartOptions.hAxis.titleTextStyle = {};
            chartOptions.vAxis.titleTextStyle = {};
            this.setPropertyOn(chartOptions.backgroundColor, "chart.fill", "fill", null);
            this.setPropertyOn(chartOptions.backgroundColor, "chart.stroke", "stroke", this.getProperty("chartArea.fill", ""));
            this.setPropertyOn(chartOptions.backgroundColor, "chart.strokeWidth", "strokeWidth", null);

            this.setPropertyOn(chartOptions.chartArea.backgroundColor, "chartArea.fill", "fill", null);
            this.setPropertyOn(chartOptions.chartArea.backgroundColor, "chartArea.stroke", "stroke", null);
            this.setPropertyOn(chartOptions.chartArea.backgroundColor, "chartArea.strokeWidth", "strokeWidth", null);
            this.setPropertyOn(chartOptions.hAxis.gridlines, "hAxis.gridlines.color", "color", this.getProperty("gridlines.color", null));
            this.setPropertyOn(chartOptions.vAxis.gridlines, "vAxis.gridlines.color", "color", this.getProperty("gridlines.color", null));
            var textColor = this.getProperty("textColor", "#000");
            this.setPropertyOn(chartOptions.hAxis.textStyle, "hAxis.text.color", "color", this.getProperty("axis.text.color", textColor));
            this.setPropertyOn(chartOptions.vAxis.textStyle, "vAxis.text.color", "color", this.getProperty("axis.text.color", textColor));

            this.setPropertyOn(chartOptions.hAxis.titleTextStyle, "hAxis.text.color", "color", textColor);
            this.setPropertyOn(chartOptions.vAxis.titleTextStyle, "vAxis.text.color", "color", textColor);

            this.setPropertyOn(chartOptions.legend.textStyle, "legend.text.color", "color", textColor);


            if (this.lineWidth) {
                chartOptions.lineWidth = this.lineWidth;
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
            this.setContents(this.getChartDiv());
            return chartOptions;
        },
        getChartHeight: function() {
            return this.getProperty("height");
        },
        getChartWidth: function() {
            return this.getProperty("width");
        },
        getChartDiv: function() {

            var chartId = this.getDomId(ID_CHART);
            var divAttrs = [ATTR_ID, chartId];
            divAttrs.push("style");
            var style = "";
            var width = this.getChartWidth();
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
            var height = this.getChartHeight();
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
            divAttrs.push(style);
            return HtmlUtils.div(divAttrs, "");
        },
        makeGoogleChart: function(dataList, props, selectedFields) {
            if (typeof google == 'undefined') {
                this.setContents("No google");
                return;
            }
            this.chartOptions = this.makeChartOptions(dataList, props, selectedFields);
	    this.chartOptions.bar = {groupWidth:"95%"}

//	    console.log(JSON.stringify(this.chartOptions,null,2));
	    


            this.chart = this.doMakeGoogleChart(dataList, props, selectedFields, this.chartOptions);
            if (this.chart != null) {
               var dataTable = this.makeDataTable(dataList, props, selectedFields);
                if (!dataTable) {
                    this.setContents(this.getMessage("No data available"));
                    return;
                }
                if (!Utils.isDefined(this.chartOptions.height)) {
                    this.chartOptions.height = "100%";
                }
                //                console.log("draw:" +" " +JSON.stringify(this.chartOptions,null,3));
		if(this.getProperty("animation",false,true)) {
		    this.chartOptions.animation = {
			startup: true,
			duration:parseFloat(this.getProperty("animationDuration",1000,true)),
			easing:this.getProperty("animationEasing","linear",true)
		    };
		    HtmlUtils.callWhenScrolled(this.getDomId(ID_CHART),()=>{
			if(!this.animationCalled) {
			    this.animationCalled = true;
			    this.chart.draw(dataTable, this.chartOptions);
			}
		    });
		} else {
		    this.chart.draw(dataTable, this.chartOptions);
		}


                var theDisplay = this;
                google.visualization.events.addListener(this.chart, 'onmouseover', function(event) {
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
                //always propagate the event when loaded
		/*
                theDisplay.displayManager.propagateEventRecordSelection(theDisplay,
									theDisplay.dataCollection.getList()[0], {
									    index: 0
									});
		*/
                google.visualization.events.addListener(this.chart, 'select', function(event) {
                    if (theDisplay.chart.getSelection) {
                        var selected = theDisplay.chart.getSelection();
                        if (selected && selected.length > 0) {
                            var index = selected[0].row;
			    var record = theDisplay.indexToRecord[index];
//			    console.log(index +" " + record.getData()[0]);
			    if(record) {
				theDisplay.getDisplayManager().notifyEvent("handleEventRecordSelection", theDisplay, {xxx:"XXX",record: record});
			    }
                        }
                    }
                });
            }
        },
        getChartId: function() {
            return this.getDomId(ID_CHART);
        },
        doMakeGoogleChart: function(dataList, props, selectedFields, chartOptions) {
            throw new Error("doMakeGoogleChart undefined");
        }
    });
    this.makeChart = this.makeGoogleChart;
}



function RamaddaAxisChart(displayManager, id, chartType, properties) {
    let SUPER = new RamaddaGoogleChart(displayManager, id, chartType, properties);
    RamaddaUtil.inherit(this, SUPER);
    $.extend(this, {
	getWikiEditorTags: function() {
	    var t = SUPER.getWikiEditorTags();
	    var myTags = [
		"label:Chart Attributes",
		"vAxisMinValue=\"\"",
		"vAxisMaxValue=\"\"", 
		"chartHeight=\"\"",
		"chartHeight=\"\"",
		"chartWidth=\"\"",
		"chartLeft=\"\"",
		"chartRight=\"\"",
		'tooltipFields=""',
		'annotations=""',
		'annotationFields=""',	
		'annotationLabelField=""',
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
            chartOptions.height = this.getProperty("chartHeight", this.getProperty("height", "150"));
            if (!chartOptions.legend)
                chartOptions.legend = {};

            $.extend(chartOptions.legend, {
                position: this.getProperty("legendPosition", 'bottom')
            });


            /*
              chartOptions.chartArea={};
              chartOptions.chartArea.backgroundColor =  {
              'fill': '#ccc',
              'opacity': 1
              }
            */
            //            chartOptions.chartArea.backgroundColor =  "green";
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

            if (this.hAxis) {
                chartOptions.hAxis.title = this.hAxis;
            }
            if (this.vAxis) {
                chartOptions.vAxis.title = this.vAxis;
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
            return true;
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
        doMakeGoogleChart: function(dataList, props, selectedFields, chartOptions) {
            return new google.visualization.LineChart(document.getElementById(this.getChartId()));
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

        doMakeGoogleChart: function(dataList, props, selectedFields, chartOptions) {
            if (this.isStacked)
                chartOptions.isStacked = true;
            return new google.visualization.AreaChart(document.getElementById(this.getChartId()));
        }
    });
}


function RamaddaBaseBarchart(displayManager, id, type, properties) {
    let SUPER  = new RamaddaSeriesChart(displayManager, id, type, properties);
    RamaddaUtil.inherit(this, SUPER);
    $.extend(this, {
	getWikiEditorTags: function() {
	    return Utils.mergeLists(SUPER.getWikiEditorTags(),
				    ["barWidth=\"10\""])},
        doMakeGoogleChart: function(dataList, props, selectedFields, chartOptions) {
            var chartType = this.getChartType();
            if (chartType == DISPLAY_BARSTACK) {
                chartOptions.isStacked = true;
            }
            if (this.getProperty("barWidth")) {
                chartOptions.bar = {
                    groupWidth: this.getProperty("barWidth")
                }
            }
            chartOptions.orientation = "horizontal";
            return new google.visualization.BarChart(document.getElementById(this.getChartId()));
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
        doMakeGoogleChart: function(dataList, props, selectedFields, chartOptions) {
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
            return new google.visualization.Histogram(document.getElementById(this.getChartId()));
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
    let SUPER = new RamaddaTextChart(displayManager, id, DISPLAY_PIECHART, properties);
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);
    $.extend(this, {
        canDoGroupBy: function() {
            return true;
        },
	getWikiEditorTags: function() {
	    return Utils.mergeLists(SUPER.getWikiEditorTags(),
				    [
					"label:Pie Chart Attributes",
					"pieHole=\"0.5\"",
					"is3D=\"true\"",
					"bins=\"\"",
					"binMin=\"\"",
					"binMax=\"max\"",
					"groupBy=\"field\""  ,
					"sliceVisibilityThreshold=\"0.01\"",
				    ]);
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
        getChartDiv: function() {
            var chartId = this.getDomId(ID_CHART);
            var divAttrs = [ATTR_ID, chartId];
            divAttrs.push("style");
            var style = "";
            var width = this.getChartWidth();
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
            var height = this.getChartHeight();
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
            divAttrs.push(style);
            return HtmlUtils.div(divAttrs, "");
        },
        doMakeGoogleChart: function(dataList, props, selectedFields, chartOptions) {
            chartOptions.tooltip = {
                textStyle: {
                    color: '#000000'
                },
                showColorCode: true
            };
            if (this.getProperty("bins", null)) {
                chartOptions.title = "Bins: " + this.getDataValues(dataList[0])[1];
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
            return new google.visualization.PieChart(document.getElementById(this.getChartId()));
        },
        makeDataTable: function(dataList, props, selectedFields) {
            var dataTable = new google.visualization.DataTable();
            var list = [];
            var groupBy = this.groupByField;
            var header = this.getDataValues(dataList[0]);
            dataTable.addColumn("string", header[0]);
            dataTable.addColumn("number", header[1]);
            //                    dataTable.addColumn({type:'string',role:'tooltip'});
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
            } else {
                for (var i = 1; i < dataList.length; i++) {
                    var tuple = this.getDataValues(dataList[i]);
                    var s = "" + (tuple.length == 1 ? "#" + i : tuple[0]);
                    var v = tuple.length == 1 ? tuple[0] : tuple[1];
                    list.push([s, v]);
                }
            }
            dataTable.addRows(list);
            return dataTable;
        }
    });


}


function SankeyDisplay(displayManager, id, properties) {
    RamaddaUtil.inherit(this, new RamaddaTextChart(displayManager, id, DISPLAY_SANKEY, properties));
    addRamaddaDisplay(this);
    $.extend(this, {
        doMakeGoogleChart: function(dataList, props, selectedFields, chartOptions) {
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
            return new google.visualization.Sankey(document.getElementById(this.getChartId()));
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
        doMakeGoogleChart: function(dataList, props, selectedFields, chartOptions) {
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

            return new google.visualization.WordTree(document.getElementById(this.getChartId()));
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
        canDoGroupBy: function() {
            return true;
        },
        defaultSelectedToAll: function() {
            return true;
        },
        doMakeGoogleChart: function(dataList, props, selectedFields, chartOptions) {
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
            chartOptions.allowHtml = true;
            if (dataList.length && this.getDataValues(dataList[0]).length > 4) {
                chartOptions.cssClassNames = {
                    headerCell: 'display-table-header-max'
                };
            } else {
                chartOptions.cssClassNames = {
                    headerCell: 'display-table-header'
                };
            }
            return new google.visualization.Table(document.getElementById(this.getChartId()));
        },
        makeDataTable: function(dataList, props, selectedFields) {
	    //		dataList = this.filterData(dataList, selectedFields,false,true);
            var rows = this.makeDataArray(dataList);

            var data = [];
            for (var rowIdx = 0; rowIdx < rows.length; rowIdx++) {
		
                var row = rows[rowIdx];
                for (var colIdx = 0; colIdx < row.length; colIdx++) {
                    if ((typeof row[colIdx]) == "string") {
                        row[colIdx] = row[colIdx].replace(/\n/g, "<br>");
			if(row[colIdx].startsWith("http:") || row[colIdx].startsWith("https:")) {
			    row[colIdx] = "<a href='" +row[colIdx] +"'>" + row[colIdx]+"</a>";
			}
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

        makeDataTable: function(dataList, props, selectedFields) {
	    var tmp =[];
	    var a = this.makeDataArray(dataList);
	    tmp.push(a[0]);
	    //Remove nans
	    for(var i=1;i<a.length;i++) {
		var tuple = a[i];
		var ok = true;
		for(j=1;j<tuple.length && ok;j++) {
		    if(isNaN(tuple[j])) ok = false;
		}
		if(ok)
		    tmp.push(tuple);
	    }
            return google.visualization.arrayToDataTable(tmp);
        },
        doMakeGoogleChart: function(dataList, props, selectedFields, chartOptions) {
            var ct = this.getColorTable(true);
            if (ct) {
                chartOptions.colors = ct;
            } else if (!this.colors) {
                chartOptions.colors = this.colorList;
            }
            if (chartOptions.colors) {
                chartOptions.colors = Utils.getColorTable("rainbow", true);
            }

	    $.extend(chartOptions.chartArea, {
                left: this.getProperty("chartLeft", this.chartDimensions.left),
                right: this.getProperty("chartRight", this.chartDimensions.right),
                top: this.getProperty("chartTop", "10"),
		bottom: this.getProperty("chartBottom"),
                width: '98%',
                height: '90%'
            });
            chartOptions.sizeAxis = {

	    }

            chartOptions.colorAxis = {
                legend: {
                    position: this.getProperty("legendPosition", "in")
                }
            }
	    var colorTable = this.getColorTable(true);
	    if(colorTable)
		chartOptions.colorAxis.colors = colorTable;

            chartOptions.bubble = {
                textStyle: {
                    auraColor: "none"
                },
                stroke: "#666"
            };
            chartOptions.hAxis = {};
            chartOptions.vAxis = {};
            header = this.getDataValues(dataList[0]);
            chartOptions.hAxis.format = this.getProperty("hAxisFormat", null);
            chartOptions.vAxis.format = this.getProperty("vAxisFormat", null);
            chartOptions.hAxis.title = this.getProperty("hAxisTitle", header.length > 1 ? header[1] : null);
            chartOptions.vAxis.title = this.getProperty("vAxisTitle", header.length > 2 ? header[2] : null);


            return new google.visualization.BubbleChart(document.getElementById(this.getChartId()));
        }

    });
}


function BartableDisplay(displayManager, id, properties) {
    let SUPER = new RamaddaSeriesChart(displayManager, id, DISPLAY_BARTABLE, properties);
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);
    $.extend(this, {
        xgetIncludeIndexIfDate: function() {
            return true;
        },
        doMakeGoogleChart: function(dataList, props, selectedFields, chartOptions) {
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
                colors: this.colorList,
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
                    showColorCode: true
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
            return new google.charts.Bar(document.getElementById(this.getChartId()));
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

        doMakeGoogleChart: function(dataList, props, selectedFields, chartOptions) {
            var dataTable = this.makeDataTable(dataList, props, selectedFields);
            if (!dataTable) return null;
            return new google.visualization.TreeMap(document.getElementById(this.getChartId()));
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
                    v = HtmlUtils.onClick(call + ".valueClicked('" + field.getId() + "','" + v + "')", v, []);
                    tt += "<b>" + field.getLabel() + "</b>" + ": " + v + "<br>";
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



function TimelinechartDisplay(displayManager, id, properties) {
    RamaddaUtil.inherit(this, new RamaddaTextChart(displayManager, id, DISPLAY_TIMELINECHART, properties));
    addRamaddaDisplay(this);
    $.extend(this, {
        doMakeGoogleChart: function(dataList, props, selectedFields, chartOptions) {
            return new google.visualization.Timeline(document.getElementById(this.getChartId()));
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
        doMakeGoogleChart: function(dataList, props, selectedFields, chartOptions) {
            chartOptions.calendar = {
                cellSize: parseInt(this.getProperty("cellSize", 15))
            };
            //If a calendar is show in tabs then it never returns from the draw
            if (this.jq(ID_CHART).width() == 0) {
                return;
            }
            return new google.visualization.Calendar(document.getElementById(this.getChartId()));
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
                dttm = dttm.replace(/ /g, "&nbsp;");
                var tooltip = "<center><b>" + dttm + "</b></center>" +
                    "<b>" + header[1].replace(/ /g, "&nbsp;") + "</b>:&nbsp;" + this.formatNumber(value);
                tooltip = HtmlUtils.tag("div", ["style", "padding:5px;"], tooltip);
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
        doMakeGoogleChart: function(dataList, props, selectedFields, chartOptions) {
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
            return new google.visualization.Gauge(document.getElementById(this.getChartId()));
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
            if (this.chart) {
                this.index = index;
                var dataTable = this.makeDataTable(this.dataList);
                this.chart.draw(dataTable, this.chartOptions);
            }
        },
    });
}




function ScatterplotDisplay(displayManager, id, properties) {
    let SUPER = new RamaddaGoogleChart(displayManager, id, DISPLAY_SCATTERPLOT, properties);
    RamaddaUtil.inherit(this, SUPER);
    $.extend(this, {
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
                $.extend(chartOptions.hAxis, {
                    title: this.getDataValues(dataList[0])[0]
                });
                if (!chartOptions.vAxis) chartOptions.vAxis = {};
                $.extend(chartOptions.vAxis, {
                    title: this.getDataValues(dataList[0])[1]
                });
                //We only have the one vAxis range for now
                if (!isNaN(this.getVAxisMinValue())) {
                    chartOptions.hAxis.minValue = this.getVAxisMinValue();
                    chartOptions.vAxis.minValue = this.getVAxisMinValue();
                }
                if (!isNaN(this.getVAxisMaxValue())) {
                    chartOptions.hAxis.maxValue = this.getVAxisMaxValue();
                    chartOptions.vAxis.maxValue = this.getVAxisMaxValue();
                }
            }
            return chartOptions;
        },
        doMakeGoogleChart: function(dataList, props, selectedFields, chartOptions) {
            var height = 400;
            if (Utils.isDefined(this.chartHeight)) {
                height = this.chartHeight;
            }
            var width = "100%";
            if (Utils.isDefined(this.chartWidth)) {
                width = this.chartWidth;
            }

            var chartId = this.getChartId();
            $("#" + chartId).css("width", width);
            $("#" + chartId).css("height", height);
            return new google.visualization.ScatterChart(document.getElementById(this.getChartId()));
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

    addRamaddaDisplay(this);
}




function RamaddaStatsDisplay(displayManager, id, properties, type) {
    var dflt = Utils.isDefined(properties["showDefault"]) ? properties["showDefault"] : true;
    $.extend(this, {
        showMin: dflt,
        showMax: dflt,
        showAverage: dflt,
        showStd: dflt,
        showPercentile: dflt,
        showCount: dflt,
        showTotal: dflt,
        showPercentile: dflt,
        showMissing: dflt,
        showUnique: dflt,
        showType: dflt,
        showText: dflt,
    });
    let SUPER = new RamaddaFieldsDisplay(displayManager, id, type || DISPLAY_STATS, properties);
    RamaddaUtil.inherit(this, SUPER);

    if (!type)
        addRamaddaDisplay(this);


    RamaddaUtil.defineMembers(this, {
        "map-display": false,
	getWikiEditorTags: function() {
	    return Utils.mergeLists(SUPER.getWikiEditorTags(),
				    [
					'label:Summary Statistics',
					'showMin="true"',
					'showMax="true"',
                                        'showAverage="true"',
                                        'showStd="true"',
                                        'showPercentile="true"',
                                        'showCount="true"',
                                        'showTotal="true"',
                                        'showPercentile="true"',
                                        'showMissing="true"',
                                        'showUnique="true"',
                                        'showType="true"',
                                        'showText="true"'
				    ])},



        needsData: function() {
            return true;
            //                return this.getProperty("loadData", false) || this.getCreatedInteractively();
        },
        handleEventPointDataLoaded: function(source, pointData) {
            if (!this.needsData()) {
                if (this.dataCollection == null) {
                    this.dataCollection = source.dataCollection;
                    this.updateUI();
                }
            }
        },
        getDefaultSelectedFields: function(fields, dfltList) {
            if (dfltList != null && dfltList.length > 0) {
                return dfltList;
            }
            var tuples = this.getStandardData(null, {
                includeIndex: false
            });
            var justOne = (tuples.length == 2);

            //get the numeric fields
            var l = [];
            for (i = 0; i < fields.length; i++) {
                var field = fields[i];
                if (!justOne && (!this.showText && !field.isNumeric())) continue;
                var lbl = field.getLabel().toLowerCase();
                if (lbl.indexOf("latitude") >= 0 || lbl.indexOf("longitude") >= 0) {
                    continue;
                }
                l.push(field);
            }
            return l;
        },

        getFieldsToSelect: function(pointData) {
            return pointData.getRecordFields();
        },
        defaultSelectedToAll: function() {
            return true;
        },
        fieldSelectionChanged: function() {
            SUPER.fieldSelectionChanged.call(this);
            this.updateUI();
        },
        updateUI: function() {
            SUPER.updateUI.call(this);
            if (!this.hasData()) {
                this.setContents(this.getLoadingMessage());
                return;
            }
            var dataList = this.getStandardData(null, {
                includeIndex: false
            });
            var allFields = this.dataCollection.getList()[0].getRecordFields();
            this.allFields = allFields;
            var fields = this.getSelectedFields([]);
            var fieldMap = {};
            var stats = [];
            var justOne = (dataList.length == 2);
            for (var rowIdx = 1; rowIdx < dataList.length; rowIdx++) {
                var tuple = this.getDataValues(dataList[rowIdx]);
                if (rowIdx == 1) {
                    for (var col = 0; col < tuple.length; col++) {
                        stats.push({
                            isNumber: false,
                            count: 0,
                            min: Number.MAX_SAFE_INTEGER,
                            uniqueMap: {},
                            unique: 0,
                            std: 0,
                            max: Number.MIN_SAFE_INTEGER,
                            total: 0,
                            numMissing: 0,
                            numNotMissing: 0,
                            type: null,
                            values: []
                        });
                    }
                }
                for (var fieldIdx = 0; fieldIdx < fields.length; fieldIdx++) {
                    var field = fields[fieldIdx];
                    var col = field.getIndex()
                    stats[col].type = field.getType();
                    var v = tuple[col];
                    if (v) {
                        if (!Utils.isDefined(stats[col].uniqueMap[v])) {
                            stats[col].uniqueMap[v] = 1;
                            stats[col].unique++;
                        } else {
                            stats[col].uniqueMap[v]++;
                        }
                    }
                    stats[col].isNumber = field.isNumeric();
                    stats[col].count++;
                    if (v == null) {
                        stats[col].numMissing++;
                    } else {
                        stats[col].numNotMissing++;
                    }
                    if (v && (typeof v == 'number')) {
                        var label = field.getLabel().toLowerCase();
                        if (label.indexOf("latitude") >= 0 || label.indexOf("longitude") >= 0) {
                            continue;
                        }
                        stats[col].total += v;
                        stats[col].max = Math.max(stats[col].max, v);
                        stats[col].min = Math.min(stats[col].min, v);
                        stats[col].values.push(v);
                    }
                }
            }


            if (this.showUnique) {
                for (var fieldIdx = 0; fieldIdx < fields.length; fieldIdx++) {
                    var field = fields[fieldIdx];
                    var col = field.getIndex();
                    stats[col].uniqueMax = 0;
                    stats[col].uniqueValue = "";
                    for (var v in stats[col].uniqueMap) {
                        var count = stats[col].uniqueMap[v];
                        if (count > stats[col].uniqueMax) {
                            stats[col].uniqueMax = count;
                            stats[col].uniqueValue = v;
                        }
                    }
                }
            }

            if (this.showStd) {
                for (var fieldIdx = 0; fieldIdx < fields.length; fieldIdx++) {
                    var field = fields[fieldIdx];
                    var col = field.getIndex();
                    var values = stats[col].values;
                    if (values.length > 0) {
                        var average = stats[col].total / values.length;
                        var stdTotal = 0;
                        for (var i = 0; i < values.length; i++) {
                            var diff = values[i] - average;
                            stdTotal += diff * diff;
                        }
                        var mean = stdTotal / values.length;
                        stats[col].std = Math.sqrt(mean);
                    }
                }
            }
            var border = (justOne ? "0" : "1");
            var html = HtmlUtils.openTag("table", ["border", border, "bordercolor", "#ccc", "class", "display-stats", "cellspacing", "1", "cellpadding", "5"]);
            var dummy = ["&nbsp;"];
            if (!justOne) {
                header = [""];
                if (this.showCount) {
                    header.push("Count");
                    dummy.push("&nbsp;");
                }
                if (this.showMin) {
                    header.push("Min");
                    dummy.push("&nbsp;");
                }
                if (this.showPercentile) {
                    header.push("25%");
                    dummy.push("&nbsp;");
                    header.push("50%");
                    dummy.push("&nbsp;");
                    header.push("75%");
                    dummy.push("&nbsp;");
                }
                if (this.showMax) {
                    header.push("Max");
                    dummy.push("&nbsp;");
                }
                if (this.showTotal) {
                    header.push("Total");
                    dummy.push("&nbsp;");
                }
                if (this.showAverage) {
                    header.push("Average");
                    dummy.push("&nbsp;");
                }
                if (this.showStd) {
                    header.push("Std");
                    dummy.push("&nbsp;");
                }
                if (this.showUnique) {
                    header.push("# Unique");
                    dummy.push("&nbsp;");
                    header.push("Top");
                    dummy.push("&nbsp;");
                    header.push("Freq.");
                    dummy.push("&nbsp;");
                }
                if (this.showMissing) {
                    header.push("Not&nbsp;Missing");
                    dummy.push("&nbsp;");
                    header.push("Missing");
                    dummy.push("&nbsp;");
                }
                html += HtmlUtils.tr(["valign", "bottom"], HtmlUtils.tds(["class", "display-stats-header", "align", "center"], header));
            }
            var cats = [];
            var catMap = {};
            for (var fieldIdx = 0; fieldIdx < fields.length; fieldIdx++) {
                var field = fields[fieldIdx];
                var col = field.getIndex();
                var field = allFields[col];
                var right = "";
                var total = "&nbsp;";
                var label = field.getLabel().toLowerCase();
                var avg = stats[col].numNotMissing == 0 ? "NA" : this.formatNumber(stats[col].total / stats[col].numNotMissing);
                //Some guess work about when to show a total
                if (label.indexOf("%") < 0 && label.indexOf("percent") < 0 && label.indexOf("median") < 0) {
                    total = this.formatNumber(stats[col].total);
                }
                if (justOne) {
                    right = HtmlUtils.tds(["xalign", "right"], [this.formatNumber(stats[col].min)]);
                    continue;
                }
                var values = [];
                if (!stats[col].isNumber && this.showText) {
                    if (this.showCount)
                        values.push(stats[col].count);
                    if (this.showMin)
                        values.push("-");
                    if (this.showPercentile) {
                        values.push("-");
                        values.push("-");
                        values.push("-");
                    }
                    if (this.showMax)
                        values.push("-");
                    values.push("-");
                    if (this.showAverage) {
                        values.push("-");
                    }
                    if (this.showStd) {
                        values.push("-");
                    }
                    if (this.showUnique) {
                        values.push(stats[col].unique);
                        values.push(stats[col].uniqueValue);
                        values.push(stats[col].uniqueMax);
                    }
                    if (this.showMissing) {
                        values.push(stats[col].numNotMissing);
                        values.push(stats[col].numMissing);
                    }
                } else {
                    if (this.showCount) {
                        values.push(stats[col].count);
                    }
                    if (this.showMin) {
                        values.push(this.formatNumber(stats[col].min));
                    }
                    if (this.showPercentile) {
                        var range = stats[col].max - stats[col].min;
                        values.push(this.formatNumber(stats[col].min + range * 0.25));
                        values.push(this.formatNumber(stats[col].min + range * 0.50));
                        values.push(this.formatNumber(stats[col].min + range * 0.75));
                    }
                    if (this.showMax) {
                        values.push(this.formatNumber(stats[col].max));
                    }
                    if (this.showTotal) {
                        values.push(total);
                    }
                    if (this.showAverage) {
                        values.push(avg);
                    }
                    if (this.showStd) {
                        values.push(this.formatNumber(stats[col].std));
                    }
                    if (this.showUnique) {
                        values.push(stats[col].unique);
                        if (Utils.isNumber(stats[col].uniqueValue)) {
                            values.push(this.formatNumber(stats[col].uniqueValue));
                        } else {
                            values.push(stats[col].uniqueValue);
                        }
                        values.push(stats[col].uniqueMax);
                    }
                    if (this.showMissing) {
                        values.push(stats[col].numNotMissing);
                        values.push(stats[col].numMissing);
                    }

                }
                right = HtmlUtils.tds(["align", "right"], values);
                var align = (justOne ? "right" : "left");
                var label = field.getLabel();
                var toks = label.split("!!");
                var tooltip = "";
                tooltip += field.getId();
                if (field.description && field.description != "") {
                    tooltip += "\n" + field.description + "\n";
                }
                label = toks[toks.length - 1];
                if (field.unit && field.unit != "")
                    label = label + " [" + field.unit + "]";
                if (justOne) {
                    label += ":";
                }
                label = label.replace(/ /g, "&nbsp;")
                var row = HtmlUtils.tr([], HtmlUtils.td(["align", align], field.getTypeLabel() +"&nbsp;<b>" + HtmlUtils.span(["title", tooltip], label) + "</b>") + right);
                if (justOne) {
                    html += row;
                } else {
                    html += row;
                }
            }
            html += "</table>";
            this.setContents(html);
            this.initTooltip();
            //always propagate the event when loaded
            this.displayManager.propagateEventRecordSelection(this,
							      this.dataCollection.getList()[0], {
								  index: 0
							      });
        },
        handleEventRecordSelection: function(source, args) {
            //                this.lastHtml = args.html;
            //                this.setContents(args.html);
        }
    });
}



function RamaddaRecordsDisplay(displayManager, id, properties, type) {
    let SUPER = new RamaddaFieldsDisplay(displayManager, id, DISPLAY_RECORDS, properties);
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
	getWikiEditorTags: function() {
	    return Utils.mergeLists(SUPER.getWikiEditorTags(),
				    [
					"maxHeight=\"\"",
				    ]);
	},
        needsData: function() {
            return true;
        },
        handleEventPointDataLoaded: function(source, pointData) {
            if (!this.needsData()) {
                if (this.dataCollection == null) {
                    this.dataCollection = source.dataCollection;
                    this.updateUI();
                }
            }
        },
        getFieldsToSelect: function(pointData) {
            return pointData.getRecordFields();
        },
        defaultSelectedToAll: function() {
            return true;
        },
        fieldSelectionChanged: function() {
            SUPER.fieldSelectionChanged.call(this);
            this.updateUI();
        },
        updateUI: function() {
            SUPER.updateUI.call(this);
            var records = this.filterData();
            if (!records) {
                this.setContents(this.getLoadingMessage());
                return;
            }
            var fields = this.getSelectedFields(this.getData().getRecordFields());
            var html = "";
            for (var rowIdx = 0; rowIdx < records.length; rowIdx++) {
                var tuple = this.getDataValues(records[rowIdx]);
                for (var fieldIdx = 0; fieldIdx < fields.length; fieldIdx++) {
                    var field = fields[fieldIdx];
                    var v = tuple[field.getIndex()];
                    html += HtmlUtil.b(field.getLabel()) + ": " + v + "</br>";
                }
                html += "<p>";
            }
            var height = this.getProperty("maxHeight", "400px");
            if (!height.endsWith("px")) {
                height = height + "px";
            }
            this.setContents(HtmlUtil.div(["style", "max-height:" + height + ";overflow-y:auto;"], html));
        },
        handleEventRecordSelection: function(source, args) {
            //                this.lastHtml = args.html;
            //                this.setContents(args.html);
        }
    });
}





function RamaddaCrosstabDisplay(displayManager, id, properties) {
    var ID_TABLE = "crosstab";
    let SUPER = new RamaddaFieldsDisplay(displayManager, id, DISPLAY_CROSSTAB, properties);
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
        needsData: function() {
            return true;
        },
        handleEventPointDataLoaded: function(source, pointData) {
            if (!this.needsData()) {
                if (this.dataCollection == null) {
                    this.dataCollection = source.dataCollection;
                    this.updateUI();
                }
            }
        },
        fieldSelectionChanged: function() {
            this.updateUI();
        },
        updateUI: function(pointData) {
            SUPER.updateUI.call(this, pointData);
            if (!this.hasData()) {
                this.setContents(this.getLoadingMessage());
                return;
            }
            var allFields = this.dataCollection.getList()[0].getRecordFields();
	    var enums = [];
	    allFields.map(field=>{
		var label = field.getLabel();
		if(label.length>30) label = label.substring(0,29);
		enums.push([field.getId(),label]);
	    });
	    var select = HtmlUtils.span(["class","display-filterby"],
					"Display: " + HtmlUtils.select("",["style","", "id",this.getDomId("crosstabselect")],enums,
								       this.getProperty("column", "", true)));


            this.setContents(select+HtmlUtils.div(["id",this.getDomId(ID_TABLE)]));
	    let _this = this;
	    this.jq("crosstabselect").change(function() {
		_this.setProperty("column", $(this).val());
		_this.makeTable();
	    });
	    this.makeTable();
	},
	makeTable: function() {
            var dataList = this.getStandardData(null, {
                includeIndex: false
            });
            var allFields = this.dataCollection.getList()[0].getRecordFields();
	    var col =  this.getFieldById(null, this.getProperty("column", "", true));
	    var rows =  this.getFieldsByIds(null, this.getProperty("rows", null, true));
	    if(!col) col  = allFields[0];
	    if(rows.length==0) rows  = allFields;

            var html = HtmlUtils.openTag("table", ["border", "1px", "bordercolor", "#ccc", "class", "display-crosstab", "cellspacing", "1", "cellpadding", "2"]);
	    var total = dataList.length-1;
	    var cnt =0;
	    rows.map((row)=>{
		if(row.getId()==col.getId()) return;
		cnt++;
		var colValues = [];
		var rowValues = [];
		var count ={};
		var rowcount ={};
		var colcount ={};
		for (var rowIdx = 1; rowIdx < dataList.length; rowIdx++) {
		    var tuple = this.getDataValues(dataList[rowIdx]);
		    var colValue = (""+tuple[col.getIndex()]).trim();
		    var rowValue = (""+tuple[row.getIndex()]).trim();
		    var key = colValue+"--" + rowValue;
		    if(colValues.indexOf(colValue)<0) colValues.push(colValue);
		    if(rowValues.indexOf(rowValue)<0) rowValues.push(rowValue);
		    if (!(rowValue in rowcount)) {
			rowcount[rowValue] = 0;
		    }
		    rowcount[rowValue]++;
		    if (!(key in count)) {
			count[key] = 0;
		    }
		    count[key]++;
		}
		colValues.sort();
		rowValues.sort();
		if(cnt==1)
		    html+="<tr><td></td><td align=center class=display-crosstab-header colspan=" + colValues.length+">" + col.getLabel()+"</td><td>&nbsp;</td></tr>";
		html+="<tr valign=bottom class=display-crosstab-header-row><td class=display-crosstab-header>" + row.getLabel() +"</td>";
		for(var j=0;j<colValues.length;j++) {
		    var colValue = colValues[j];
		    html+="<td>" + (colValue==""?"&lt;blank&gt;":colValue) +"</td>";
		}
		html+="<td><b>Total</b></td>";
		html+="</tr>";
		for(var i=0;i<rowValues.length;i++) {
		    var rowValue = rowValues[i];
		    html+="<tr>";
		    html+="<td>" + (rowValue==""?"&lt;blank&gt;":rowValue) +"</td>";
		    for(var j=0;j<colValues.length;j++) {
			var colValue = colValues[j];
			var key = colValue+"--" + rowValue;
			if(Utils.isDefined(count[key])) {
			    var perc = Math.round(count[key]/total*100) +"%";
			    html+="<td align=right>" + count[key] +"&nbsp;(" + perc+")</td>";
			} else {
			    html+="<td>&nbsp;</td>";
			}
		    }
		    var perc = Math.round(rowcount[rowValue]/total*100) +"%";
		    html+="<td align=right>" + rowcount[rowValue] +"&nbsp;(" + perc+")</td>";
		    html+="</tr>";
		}
	    });
            html += "</table>";
	    this.jq(ID_TABLE).html(html);
        },
    });
}




function RamaddaCorrelationDisplay(displayManager, id, properties) {
    var ID_BOTTOM = "bottom";
    $.extend(this, {
        colorTable: "red_white_blue",
        colorByMin: "-1",
        colorByMax: "1",
    });

    let SUPER = new RamaddaFieldsDisplay(displayManager, id, DISPLAY_CORRELATION, properties);
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);

    RamaddaUtil.defineMembers(this, {
        "map-display": false,
        needsData: function() {
            return true;
        },
        getMenuItems: function(menuItems) {
            SUPER.getMenuItems.call(this, menuItems);
            var get = this.getGet();
            var tmp = HtmlUtils.formTable();
            var colorTable = this.getColorTableName();
            var ct = "<select id=" + this.getDomId("colortable") + ">";
            for (table in Utils.ColorTables) {
                if (table == colorTable)
                    ct += "<option selected>" + table + "</option>";
                else
                    ct += "<option>" + table + "</option>";
            }
            ct += "</select>";

            tmp += HtmlUtils.formEntry("Color Bar:", ct);

            tmp += HtmlUtils.formEntry("Color By Range:", HtmlUtils.input("", this.colorByMin, ["size", "7", ATTR_ID, this.getDomId("colorbymin")]) + " - " +
				       HtmlUtils.input("", this.colorByMax, ["size", "7", ATTR_ID, this.getDomId("colorbymax")]));
            tmp += "</table>";
            menuItems.push(tmp);
        },
        initDialog: function() {
            SUPER.initDialog.call(this);
            var _this = this;
            var updateFunc = function() {
                _this.colorByMin = _this.jq("colorbymin").val();
                _this.colorByMax = _this.jq("colorbymax").val();
                _this.updateUI();

            };
            var func2 = function() {
                _this.colorTable = _this.jq("colortable").val();
                _this.updateUI();

            };
            this.jq("colorbymin").blur(updateFunc);
            this.jq("colorbymax").blur(updateFunc);
            this.jq("colortable").change(func2);
        },

        handleEventPointDataLoaded: function(source, pointData) {
            if (!this.needsData()) {
                if (this.dataCollection == null) {
                    this.dataCollection = source.dataCollection;
                    this.updateUI();
                }
            }
        },
        fieldSelectionChanged: function() {
            this.updateUI();
        },
        updateUI: function(pointData) {
            SUPER.updateUI.call(this, pointData);
            if (!this.hasData()) {
                this.setContents(this.getLoadingMessage());
                return;
            }
            var dataList = this.getStandardData(null, {
                includeIndex: false
            });
            var allFields = this.dataCollection.getList()[0].getRecordFields();
            var fields = this.getSelectedFields([]);
            if (fields.length == 0) fields = allFields;
            var fieldCnt = 0;
            for (var fieldIdx = 0; fieldIdx < fields.length; fieldIdx++) {
                var field1 = fields[fieldIdx];
                if (!field1.isFieldNumeric() || field1.isFieldGeo()) continue;
                fieldCnt++;
            }

            var html = HtmlUtils.openTag("table", ["border", "0", "class", "display-correlation", "width", "100%"]);
            var col1Width = 10 + "%";
            var width = 90 / fieldCnt + "%";
            html += "\n<tr valign=bottom><td class=display-heading width=" + col1Width + ">&nbsp;</td>";
            var short = this.getProperty("short", fieldCnt > 8);
            var showValue = this.getProperty("showValue", !short);
            var useId = this.getProperty("useId", true);
            var useIdTop = this.getProperty("useIdTop", useId);
            var useIdSide = this.getProperty("useIdSide", useId);
            for (var fieldIdx = 0; fieldIdx < fields.length; fieldIdx++) {
                var field1 = fields[fieldIdx];
                if (!field1.isFieldNumeric() || field1.isFieldGeo()) continue;
                var label = useIdTop ? field1.getId() : field1.getLabel();
                if (short) label = "";
                html += "<td align=center width=" + width + ">" + HtmlUtils.tag("div", ["class", "display-correlation-heading-top"], label) + "</td>";
            }
            html += "</tr>\n";

            var colors = null;
            colorByMin = parseFloat(this.colorByMin);
            colorByMax = parseFloat(this.colorByMax);
            colors = this.getColorTable(true);
            for (var fieldIdx1 = 0; fieldIdx1 < fields.length; fieldIdx1++) {
                var field1 = fields[fieldIdx1];
                if (!field1.isFieldNumeric() || field1.isFieldGeo()) continue;
                var label = useIdSide ? field1.getId() : field1.getLabel();
                html += "<tr valign=center><td>" + HtmlUtils.tag("div", ["class", "display-correlation-heading-side"], label.replace(/ /g, "&nbsp;")) + "</td>";
                var rowName = field1.getLabel();
                for (var fieldIdx2 = 0; fieldIdx2 < fields.length; fieldIdx2++) {
                    var field2 = fields[fieldIdx2];
                    if (!field2.isFieldNumeric() || field2.isFieldGeo()) continue;
                    var colName = field2.getLabel();
                    var t1 = 0;
                    var t2 = 0;
                    var cnt = 0;

                    for (var rowIdx = 1; rowIdx < dataList.length; rowIdx++) {
                        var tuple = this.getDataValues(dataList[rowIdx]);
                        var v1 = tuple[field1.getIndex()];
                        var v2 = tuple[field2.getIndex()];
                        t1 += v1;
                        t2 += v2;
                        cnt++;
                    }
                    var avg1 = t1 / cnt;
                    var avg2 = t2 / cnt;
                    var sum1 = 0;
                    var sum2 = 0;
                    var sum3 = 0;
                    for (var rowIdx = 1; rowIdx < dataList.length; rowIdx++) {
                        var tuple = this.getDataValues(dataList[rowIdx]);
                        var v1 = tuple[field1.getIndex()];
                        var v2 = tuple[field2.getIndex()];
                        sum1 += (v1 - avg1) * (v2 - avg2);
                        sum2 += (v1 - avg1) * (v1 - avg1);
                        sum3 += (v2 - avg2) * (v2 - avg2);
                    }
                    r = sum1 / Math.sqrt(sum2 * sum3);

                    var style = "";
                    if (colors != null) {
                        var percent = (r - colorByMin) / (colorByMax - colorByMin);
                        var index = parseInt(percent * colors.length);
                        if (index >= colors.length) index = colors.length - 1;
                        else if (index < 0) index = 0;
                        style = "background-color:" + colors[index];
                    }
                    var value = r.toFixed(3);
                    var label = value;
                    if (!showValue || short) label = "&nbsp;";
                    html += "<td class=display-correlation-cell align=right style=\"" + style + "\">" + HtmlUtils.tag("div", ["class", "display-correlation-element", "title", "&rho;(" + rowName + "," + colName + ") = " + value], label) + "</td>";
                }
                html += "</tr>";
            }
            html += "<tr><td></td><td colspan = " + (fieldCnt + 1) + ">" + HtmlUtils.div(["id", this.getDomId(ID_BOTTOM)], "") + "</td></tr>";
            html += "</table>";
            this.setContents(html);
            this.displayColorTable(colors, ID_BOTTOM, colorByMin, colorByMax);
            this.initTooltip();
            this.displayManager.propagateEventRecordSelection(this,
							      this.dataCollection.getList()[0], {
								  index: 0
							      });

        },
    });
}



function RamaddaRankingDisplay(displayManager, id, properties) {
    var ID_TABLE = "table";
    $.extend(this, {
	height: "500px;",
        sortAscending:false,
    });
    if(properties.sortAscending) this.sortAscending = "true" == properties.sortAscending;
    let SUPER = new RamaddaFieldsDisplay(displayManager, id, DISPLAY_RANKING, properties);
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);

    RamaddaUtil.defineMembers(this, {
	getWikiEditorTags: function() {
	    return Utils.mergeLists(SUPER.getWikiEditorTags(),
				    [
					"label:Chart Attributes",
					"sortField=\"\"",
					'nameFields=""',
				    ]);

	},


        needsData: function() {
            return true;
        },
        handleEventPointDataLoaded: function(source, pointData) {
            if (!this.needsData()) {
                if (this.dataCollection == null) {
                    this.dataCollection = source.dataCollection;
                    this.updateUI();
                }
            }
        },
        fieldSelectionChanged: function() {
            this.updateUI();
        },
        updateUI: function(pointData) {
            SUPER.updateUI.call(this, pointData);
            if (!this.hasData()) {
                this.setContents(this.getLoadingMessage());
                return;
            }
            var dataList = this.getStandardData(null, {
                includeIndex: false
            });
            var allFields = this.dataCollection.getList()[0].getRecordFields();
            var fields = this.getSelectedFields([]);
            if (fields.length == 0) fields = allFields;
            var numericFields = this.getFieldsOfType(fields, "numeric");
            var sortField = this.getFieldById(numericFields, this.getProperty("sortField","",true));
            if (numericFields.length == 0) {
                this.setContents("No fields specified");
                return;
            }
            if (!sortField) {
                sortField = numericFields[0];
            }
            if (!sortField) {
                this.setContents("No fields specified");
                return;
            }

            var stringFields = this.getFieldsByIds(allFields, this.getProperty("nameFields","",true));
            if(stringFields.length==0) {
		var tmp = this.getFieldById(allFields, this.getProperty("nameField","",true));
		if(tmp) stringFields.push(tmp);
	    }
            if(stringFields.length==0) {
                var stringField = this.getFieldOfType(allFields, "string");
		if(stringField) stringFields.push(stringField);
	    }
            var menu = "<select class='ramadda-pulldown' id='" + this.getDomId("sortfields") + "'>";
            for (var i = 0; i < numericFields.length; i++) {
                var field = numericFields[i];
                var extra = "";
                if (field.getId() == sortField.getId()) extra = " selected ";
                menu += "<option value='" + field.getId() + "'  " + extra + " >" + field.getLabel() + "</option>\n";
            }
            menu += "</select>" ;
	    var top ="";
	    top += HtmlUtils.span(["id",this.getDomId("sort")], HtmlUtils.getIconImage(this.sortAscending?"fa-sort-up":"fa-sort-down", ["style","cursor:pointer;","title","Change sort order"]));
            if (this.getProperty("showRankingMenu", true)) {
                top+= " " + HtmlUtils.div(["style","display:inline-block;", "class","display-filterby"],menu);
            }
	    this.jq(ID_TOP_LEFT).html(top);
	    this.jq("sort").click(()=>{
		this.sortAscending= !this.sortAscending;
		if(this.sortAscending) 
		    this.jq("sort").html(HtmlUtils.getIconImage("fa-sort-up", ["style","cursor:pointer;"]));
		else
		    this.jq("sort").html(HtmlUtils.getIconImage("fa-sort-down", ["style","cursor:pointer;"]));
		this.updateUI();
	    });
            var html = "";
            html += HtmlUtils.openTag("div", ["style", "max-height:100%;overflow-y:auto;"]);
            html += HtmlUtils.openTag("table", ["id", this.getDomId(ID_TABLE)]);
            var tmp = [];
            for (var rowIdx = 1; rowIdx < dataList.length; rowIdx++) {
                var obj = dataList[rowIdx];
                obj.originalRow = rowIdx;
                tmp.push(obj);
            }

	    var includeNaN = this.getProperty("includeNaN",false);
	    if(!includeNaN) {
		var tmp2 = [];
		tmp.map(r=>{
		    var t = this.getDataValues(r);
		    var v = t[sortField.getIndex()];
		    if(!isNaN(v)) tmp2.push(r);
		});
		tmp = tmp2;
	    }
            var cnt = 0;
            tmp.sort((a, b) => {
                var t1 = this.getDataValues(a);
                var t2 = this.getDataValues(b);
                var v1 = t1[sortField.getIndex()];
                var v2 = t2[sortField.getIndex()];
		
                if (v1 < v2) return this.sortAscending?-1:1;
                if (v1 > v2) return this.sortAscending?1:-1;
                return 0;
            });


            for (var rowIdx = 0; rowIdx < tmp.length; rowIdx++) {
                var obj = tmp[rowIdx];
                var tuple = this.getDataValues(obj);
                var label = "";
                stringFields.map(f=>{
		    label += tuple[f.getIndex()]+" ";
		});

                label = label.trim();
		value = tuple[sortField.getIndex()];
                if (isNaN(value) || value === null) {
		    if(!includeNaN) continue;
		    value = "NA";
		}
		html += "<tr valign=top class='display-ranking-row' what='" + obj.originalRow + "'><td> #" + (rowIdx + 1) + "</td><td>&nbsp;" + label + "</td><td align=right>&nbsp;" +
                    value + "</td></tr>";
            }
            html += HtmlUtils.closeTag("table");
            html += HtmlUtils.closeTag("div");
            this.setContents(html);
            let _this = this;
            this.jq(ID_TABLE).find(".display-ranking-row").click(function(e) {
                _this.getDisplayManager().propagateEventRecordSelection(_this, _this.getPointData(), {
                    index: parseInt($(this).attr("what")) - 1
                });
            });
	    HtmlUtils.initSelect(this.jq("sortfields"));
            this.jq("sortfields").change(function() {
                _this.setProperty("sortField", $(this).val());
                _this.updateUI();
            });
        },
    });
}



function RamaddaTsneDisplay(displayManager, id, properties) {
    var ID_BOTTOM = "bottom";
    var ID_CANVAS = "tsnecanvas";
    var ID_DETAILS = "tsnedetails";
    var ID_RUN = "tsnerun";
    var ID_RESET = "tsnereset";
    var ID_STEP = "tsnestep";
    var ID_SEARCH = "tsnesearch";
    $.extend(this, {
        colorTable: "red_white_blue",
        colorByMin: "-1",
        colorByMax: "1",
        height: "500px;"
    });

    let SUPER = new RamaddaFieldsDisplay(displayManager, id, DISPLAY_TSNE, properties);
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);

    RamaddaUtil.defineMembers(this, {
        nameToIndex: {},
        needsData: function() {
            return true;
        },
        handleEventPointDataLoaded: function(source, pointData) {
            if (!this.needsData()) {
                if (this.dataCollection == null) {
                    this.dataCollection = source.dataCollection;
                    this.updateUI();
                }
            }
        },
        updateUI: async function(pointData) {
            SUPER.updateUI.call(this, pointData);
            if (!this.hasData()) {
                this.setContents(this.getLoadingMessage());
                return;
            }
            await Utils.importJS(ramaddaBaseUrl + "/lib/tsne.js");
            //Height is the height of the overall display including the menu bar
            var height = this.getProperty("height");
            if (height.endsWith("px")) height = height.replace("px", "");
            height = parseInt(height);
            //            height-=30;
            var details = HtmlUtils.div(["style", "height:" + height + "px;max-height:" + height + "px", "class", "display-tnse-details", "id", this.getDomId(ID_DETAILS)], "");
            var canvas = HtmlUtils.div(["class", "display-tnse-canvas-outer", "style", "height:" + height + "px"], HtmlUtils.div(["class", "display-tnse-canvas", "id", this.getDomId(ID_CANVAS)], ""));
            var buttons = HtmlUtils.div(["id", this.getDomId(ID_RUN), "class", "ramadda-button", "what", "run"], "Stop") + "&nbsp;" +
                HtmlUtils.div(["id", this.getDomId(ID_STEP), "class", "ramadda-button", "what", "step"], "Step") + "&nbsp;" +
                HtmlUtils.div(["id", this.getDomId(ID_RESET), "class", "ramadda-button", "what", "reset"], "Reset") + "&nbsp;" +
                HtmlUtils.input("", "", ["id", this.getDomId(ID_SEARCH), "placeholder", "search"]);

            buttons = HtmlUtils.div(["class", "display-tnse-toolbar"], buttons);
            this.jq(ID_TOP_LEFT).append(buttons);
            this.setContents("<table  width=100%><tr valign=top><td width=80%>" + canvas + "</td><td width=20%>" + details + "</td></tr></table>");
            this.search = this.jq(ID_SEARCH);
            this.search.keyup(e => {
                var v = this.search.val().trim();
                this.canvas.find(".display-tnse-mark").removeClass("display-tnse-highlight");
                if (v == "") return;
                v = v.toLowerCase();
                for (name in this.nameToIndex) {
                    if (name.toLowerCase().startsWith(v)) {
                        this.jq("element-" + this.nameToIndex[name]).addClass("display-tnse-highlight");
                    }
                }
            });
            this.details = this.jq(ID_DETAILS);
            this.reset = this.jq(ID_RESET);
            this.step = this.jq(ID_STEP);
            this.step.button().click(() => {
                this.running = false;
                this.run.html(this.running ? "Stop" : "Run");
                this.takeStep();
            });
            this.reset.button().click(() => {
                this.start();
            });
            this.run = this.jq(ID_RUN);
            this.run.button().click(() => {
                this.running = !this.running;
                if (this.running) this.takeStep();
                this.run.html(this.running ? "Stop" : "Run");
            });
            this.canvas = this.jq(ID_CANVAS);
            this.running = true;
            this.start();
        },
        start: function() {
            this.canvas.html("");
            this.haveStepped = false;
            this.dataList = this.getStandardData(null, {
                includeIndex: false
            });
            var allFields = this.dataCollection.getList()[0].getRecordFields();
            if (!this.fields) {
                this.fields = this.getSelectedFields([]);
                if (this.fields.length == 0) this.fields = allFields;
                var strings = this.getFieldsOfType(this.fields, "string");
                if (strings.length > 0)
                    this.textField = strings[0];
            }
            var data = [];
            for (var rowIdx = 1; rowIdx < this.dataList.length; rowIdx++) {
                var tuple = this.getDataValues(this.dataList[rowIdx]);
                var nums = [];
                for (var i = 0; i < this.fields.length; i++) {
                    if (this.fields[i].isNumeric()){
                        var v = tuple[this.fields[i].getIndex()];
                        if(isNaN(v)) v = 0;
                        nums.push(v);
                    }
                }
                data.push(nums);
            }

            var opt = {}
            opt.epsilon = 10; // epsilon is learning rate
            opt.perplexity = 30; // how many neighbors each point influences
            opt.dim = 2; // dimensionality of the embedding (2 = default)
            this.tsne = new tsnejs.tSNE(opt);
            this.tsne.initDataRaw(data);
            this.takeStep();
        },
        takeStep: function() {
            var numSteps = 10;
            for (var step = 0; step < numSteps; step++) {
                this.tsne.step();
            }

            var pts = this.tsne.getSolution();
            var minx, miny, maxx, maxy;
            for (var i = 0; i < pts.length; i++) {
                if (i == 0) {
                    maxx = minx = pts[i][0];
                    maxy = miny = pts[i][1];
                } else {
                    maxx = Math.max(maxx, pts[i][0]);
                    minx = Math.min(minx, pts[i][0]);
                    maxy = Math.max(maxy, pts[i][1]);
                    miny = Math.min(miny, pts[i][1]);
                }
            }
            var sleep = 250;
            for (var i = 0; i < pts.length; i++) {
                var x = pts[i][0];
                var y = pts[i][1];
                var px = 100 * (x - minx) / (maxx - minx);
                var py = 100 * (y - miny) / (maxy - miny);
                if (!this.haveStepped) {
                    var title = "";
                    if (this.textField) {
                        var tuple = this.getDataValues(this.dataList[i]);
                        title = tuple[this.textField.getIndex()];
                    }
                    if (title.length > 10) {
                        title.length = 10;
                    }
                    this.nameToIndex[title] = i;
                    this.canvas.append(HtmlUtils.div(["title", title, "index", i, "id", this.getDomId("element-" + i), "class", "display-tnse-mark", "style", "left:" + px + "%;" + "top:" + py + "%;"], title));
                } else {
                    this.jq("element-" + i).animate({
                        left: px + "%",
                        top: py + "%"
                    }, sleep, "linear");
                }

            }
            let _this = this;
            if (!this.haveStepped) {
                this.canvas.find(".display-tnse-mark").click(function(e) {
                    var index = parseInt($(this).attr("index"));
                    if (index < 0 || index >= _this.dataList.length) return;
                    var tuple = _this.getDataValues(_this.dataList[index]);
                    var details = "<table class=formtable width=100% >";
                    for (var i = 0; i < _this.fields.length; i++) {
                        var field = _this.fields[i];
                        details += "<tr><td align=right class=formlabel>" + field.getLabel() + ":</td><td>" + tuple[field.getIndex()] + "</td></tr>";
                    }
                    details += "</table>";
                    _this.details.html(details);
                });
            }
            if (!this.haveStepped) {
                //                this.haveStepped = true;
                //                this.takeStep();
                //                return;
            }
            this.haveStepped = true;
            if (this.running)
                setTimeout(() => this.takeStep(), sleep);
        },
    });
}


function RamaddaHeatmapDisplay(displayManager, id, properties) {
    $.extend(this, {
        colorTable: "red_white_blue",
    });
    let SUPER = new RamaddaFieldsDisplay(displayManager, id, DISPLAY_HEATMAP, properties);
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
        "map-display": false,
        needsData: function() {
            return true;
        },
        getMenuItems: function(menuItems) {
            SUPER.getMenuItems.call(this, menuItems);
            var get = this.getGet();
            var tmp = HtmlUtils.formTable();
            var colorTable = this.getColorTableName();
            var ct = "<select id=" + this.getDomId("colortable") + ">";
            for (table in Utils.ColorTable) {
                if (table == colorTable)
                    ct += "<option selected>" + table + "</option>";
                else
                    ct += "<option>" + table + "</option>";
            }
            ct += "</select>";

            tmp += HtmlUtils.formEntry("Color Table:", ct);

            tmp += HtmlUtils.formEntry("Color By Range:", HtmlUtils.input("", this.colorByMin, ["size", "7", ATTR_ID, this.getDomId("colorbymin")]) + " - " +
				       HtmlUtils.input("", this.colorByMax, ["size", "7", ATTR_ID, this.getDomId("colorbymax")]));
            tmp += "</table>";
            menuItems.push(tmp);
        },
        initDialog: function() {
            SUPER.initDialog.call(this);
            var _this = this;
            var updateFunc = function() {
                _this.colorByMin = _this.jq("colorbymin").val();
                _this.colorByMax = _this.jq("colorbymax").val();
                _this.updateUI();

            };
            var func2 = function() {
                _this.colorTable = _this.jq("colortable").val();
                _this.updateUI();

            };
            this.jq("colorbymin").blur(updateFunc);
            this.jq("colorbymax").blur(updateFunc);
            this.jq("colortable").change(func2);
        },

        handleEventPointDataLoaded: function(source, pointData) {
            if (!this.needsData()) {
                if (this.dataCollection == null) {
                    this.dataCollection = source.dataCollection;
                    this.updateUI();
                }
            }
        },
        fieldSelectionChanged: function() {
            this.updateUI();
        },
        getContentsStyle: function() {
            var height = this.getProperty("height", -1);
            if (height > 0) {
                return " height:" + height + "px; " + " max-height:" + height + "px; overflow-y: auto;";
            }
            return "";
        },
        updateUI: function(pointData) {
            var _this = this;
            if (!haveGoogleChartsLoaded()) {
                var func = function() {
                    _this.updateUI();
                }
                this.setContents(this.getLoadingMessage());
                setTimeout(func, 1000);
                return;
            }

            SUPER.updateUI.call(this, pointData);
            if (!this.hasData()) {
                this.setContents(this.getLoadingMessage());
                return;
            }
            var dataList = this.getStandardData(null, {
                includeIndex: true
            });
            var header = this.getDataValues(dataList[0]);
            var showIndex = this.getProperty("showIndex", true);
            var showValue = this.getProperty("showValue", true);
            var textColor = this.getProperty("textColor", "black");

            var cellHeight = this.getProperty("cellHeight", null);
            var extraTdStyle = "";
            if (this.getProperty("showBorder", "false") == "true") {
                extraTdStyle = "border-bottom:1px #666 solid;";
            }
            var extraCellStyle = "";
            if (cellHeight)
                extraCellStyle += "height:" + cellHeight + "px; max-height:" + cellHeight + "px; min-height:" + cellHeight + "px;";
            var allFields = this.dataCollection.getList()[0].getRecordFields();
            var fields = this.getSelectedFields([]);

            if (fields.length == 0) fields = allFields;
            var html = "";
            var colors = null;
            var colorByMin = null;
            var colorByMax = null;
            if (Utils.stringDefined(this.getProperty("colorByMins"))) {
                colorByMin = [];
                var c = this.getProperty("colorByMins").split(",");
                for (var i = 0; i < c.length; i++) {
                    colorByMin.push(parseFloat(c[i]));
                }
            }
            if (Utils.stringDefined(this.getProperty("colorByMaxes"))) {
                colorByMax = [];
                var c = this.getProperty("colorByMaxes").split(",");
                for (var i = 0; i < c.length; i++) {
                    colorByMax.push(parseFloat(c[i]));
                }
            }

            if (Utils.stringDefined(this.getProperty("colorTables"))) {
                var c = this.getProperty("colorTables").split(",");
                colors = [];
                for (var i = 0; i < c.length; i++) {
                    var name = c[i];
                    if (name == "none") {
                        colors.push(null);
                        continue;
                    }
                    var ct = Utils.getColorTable(name, true);
                    //                        console.log("ct:" + name +" " +(ct!=null));
                    colors.push(ct);
                }
            } else {
                colors = [this.getColorTable(true)];
            }
            var mins = null;
            var maxs = null;
            for (var rowIdx = 1; rowIdx < dataList.length; rowIdx++) {
                var row = this.getDataValues(dataList[rowIdx]);
                if (mins == null) {
                    mins = [];
                    maxs = [];
                    for (var colIdx = 1; colIdx < row.length; colIdx++) {
                        mins.push(Number.MAX_VALUE);
                        maxs.push(Number.MIN_VALUE);
                    }
                }

                for (var colIdx = 0; colIdx < fields.length; colIdx++) {
                    var field = fields[colIdx];
                    //Add one to the field index to account for the main time index
                    var index = field.getIndex() + 1;
                    if (!field || !field.isFieldNumeric() || field.isFieldGeo()) continue;

                    var value = row[index];
                    if (value == Number.POSITIVE_INFINITY || isNaN(value) || !Utils.isNumber(value) || !Utils.isDefined(value) || value == null) {
                        continue;
                    }
                    mins[colIdx] = Math.min(mins[colIdx], value);
                    maxs[colIdx] = Math.max(maxs[colIdx], value);
                }
            }

            html += HtmlUtils.openTag("table", ["border", "0", "class", "display-heatmap"]);
            html += "<tr valign=bottom>";
            if (showIndex) {
                html += "<td align=center>" + HtmlUtils.tag("div", ["class", "display-heatmap-heading-top"], header[0]) + "</td>";
            }
            for (var fieldIdx = 0; fieldIdx < fields.length; fieldIdx++) {
                var field = fields[fieldIdx];
                if ((!field.isFieldNumeric() || field.isFieldGeo())) continue;
                html += "<td align=center>" + HtmlUtils.tag("div", ["class", "display-heatmap-heading-top"], field.getLabel()) + "</td>";
            }
            html += "</tr>\n";




            for (var rowIdx = 1; rowIdx < dataList.length; rowIdx++) {
                var row = this.getDataValues(dataList[rowIdx]);
                var index = row[0];
                //check if its a date
                if (index.f) {
                    index = index.f;
                }
                var rowLabel = index;
                html += "<tr valign='center'>\n";
                if (showIndex) {
                    html += HtmlUtils.td(["class", "display-heatmap-heading-side", "style", extraCellStyle], rowLabel);
                }
                var colCnt = 0;
                for (var colIdx = 0; colIdx < fields.length; colIdx++) {
                    var field = fields[colIdx];
                    //Add one to the field index to account for the main time index
                    var index = field.getIndex() + 1;
                    if (!field || !field.isFieldNumeric() || field.isFieldGeo()) continue;
                    var style = "";
                    var value = row[index];
                    var min = mins[colIdx];
                    var max = maxs[colIdx];
                    if (colorByMin && colCnt < colorByMin.length)
                        min = colorByMin[colCnt];
                    if (colorByMax && colCnt < colorByMax.length)
                        max = colorByMax[colCnt];


                    var ok = min != max && !(value == Number.POSITIVE_INFINITY || isNaN(value) || !Utils.isNumber(value) || !Utils.isDefined(value) || value == null);
                    var title = header[0] + ": " + rowLabel + " - " + field.getLabel() + ": " + value;
                    if (ok && colors != null) {
                        var ct = colors[Math.min(colCnt, colors.length - 1)];
                        if (ct) {
                            var percent = (value - min) / (max - min);
                            var ctIndex = parseInt(percent * ct.length);
                            if (ctIndex >= ct.length) ctIndex = ct.length - 1;
                            else if (ctIndex < 0) ctIndex = 0;
                            style = "background-color:" + ct[ctIndex] + ";";
                        }
                    }
                    var number;
                    if (!ok) {
                        number = "-";
                    } else {
                        number = Utils.formatNumber(value)
                    }
                    if (!showValue) number = "";
                    html += HtmlUtils.td(["valign", "center", "align", "right", "style", style + extraCellStyle + extraTdStyle, "class", "display-heatmap-cell"], HtmlUtils.div(["title", title, "style", extraCellStyle + "color:" + textColor], number));
                    colCnt++;
                }
                html += "</tr>";
            }
            html += "</table>";
            this.setContents(html);
            this.initTooltip();

        },
    });
}

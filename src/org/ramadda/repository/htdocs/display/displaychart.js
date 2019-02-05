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
var DISPLAY_TABLE = "table";
var DISPLAY_TEXT = "text";
var DISPLAY_CROSSTAB = "crosstab";
var DISPLAY_CORRELATION = "correlation";
var DISPLAY_HEATMAP = "heatmap";
var DISPLAY_WORDTREE = "wordtree";

var googleChartsLoaded = false;

function googleChartsHaveLoaded() {
    googleChartsLoaded = true;
}
google.charts.setOnLoadCallback(googleChartsHaveLoaded);

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
    type: DISPLAY_TABLE,
    label: "Table",
    requiresData: true,
    forUser: true,
    category: CATEGORY_MISC
});
addGlobalDisplayType({
    type: DISPLAY_TEXT,
    label: "Text Readout",
    requiresData: false,
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
    category: CATEGORY_MISC
});




var PROP_CHART_MIN = "chartMin";
var PROP_CHART_MAX = "chartMax";
var PROP_CHART_TYPE = "chartType";
var DFLT_WIDTH = "600px";
var DFLT_HEIGHT = "200px";



/*
 */
function RamaddaFieldsDisplay(displayManager, id, type, properties) {
    var _this = this;
    this.TYPE = "RamaddaFieldsDisplay";
    var SUPER;
    RamaddaUtil.inherit(this, this.RamaddaDisplay = SUPER = new RamaddaDisplay(displayManager, id, type, properties));



    RamaddaUtil.defineMembers(this, {
        needsData: function() {
            return true;
        },
        handleEventMapClick: function(source, args) {
            if (!this.dataCollection) return;
            var pointData = this.dataCollection.getList();
            for (var i = 0; i < pointData.length; i++) {
                pointData[i].handleEventMapClick(this, source, args.lon, args.lat);
            }
        },
        initDisplay: function() {
            this.createUI();
            if (this.needsData()) {
                this.setContents(this.getLoadingMessage());
            }
            this.updateUI();
        },
        updateUI: function() {
            this.addFieldsCheckboxes();
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
function RamaddaMultiChart(displayManager, id, properties) {
    var ID_CHART = "chart";
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
        vAxisMinValue: NaN,
        vAxisMaxValue: NaN,
        showPercent: false,
        percentFields: null
    });
    if (properties.colors) {
        this.colorList = ("" + properties.colors).split(",");
    }
    var SUPER;
    RamaddaUtil.inherit(this, SUPER = new RamaddaFieldsDisplay(displayManager, id, properties.chartType, properties));


    RamaddaUtil.defineMembers(this, {
        getType: function() {
            return this.getProperty(PROP_CHART_TYPE, DISPLAY_LINECHART);
        },
        initDisplay: function() {
            this.createUI();
            this.updateUI();
        },
        clearCachedData: function() {
            SUPER.clearCachedData();
            this.computedData = null;
        },
        updateUI: function() {
            SUPER.updateUI.call(this);
            if (!this.getDisplayReady()) {
                return;
            }
            this.displayData();
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
            var updateFunc = function() {
                _this.vAxisMinValue = Utils.toFloat(_this.jq("vaxismin").val());
                _this.vAxisMaxValue = Utils.toFloat(_this.jq("vaxismax").val());
                _this.minDate = _this.jq("mindate").val();
                _this.maxDate = _this.jq("maxdate").val();
                _this.displayData();

            };
            this.jq("vaxismin").blur(updateFunc);
            this.jq("vaxismax").blur(updateFunc);
            this.jq("mindate").blur(updateFunc);
            this.jq("maxdate").blur(updateFunc);



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
        getMenuItems: function(menuItems) {
            SUPER.getMenuItems.call(this, menuItems);
            var get = this.getGet();
            //                menuItems.push(HtmlUtils.onClick(get+".setColor();", "Set color"));

            var min = "0";
            if (!isNaN(this.vAxisMinValue)) {
                min = "" + this.vAxisMinValue;
            }
            var max = "";
            if (!isNaN(this.vAxisMaxValue)) {
                max = "" + this.vAxisMaxValue;
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
            return this.getProperty(PROP_CHART_TYPE, DISPLAY_LINECHART);
        },
        defaultSelectedToAll: function() {
            if (this.chartType == DISPLAY_TABLE || this.chartType == DISPLAY_SANKEY|| this.chartType == DISPLAY_WORDTREE) {
                return true;
            }
            return SUPER.defaultSelectedToAll.call(this);
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
            var chartType = this.getProperty(PROP_CHART_TYPE, DISPLAY_LINECHART);
            if (chartType == DISPLAY_LINECHART || chartType == DISPLAY_AREACHART || chartType == DISPLAY_BARCHART) {
                return true;
            }
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
        handleEventRecordSelection: function(source, args) {
            //TODO: don't do this in index space, do it in time or space space
            if (source == this) {
                return;
            }
            if (!this.okToHandleEventRecordSelection())
                return;
            var data = this.dataCollection.getList()[0];
            if (data != args.data) {
                return;
            }
            this.setChartSelection(args.index);
        },
        getFieldsToSelect: function(pointData) {
            var chartType = this.getProperty(PROP_CHART_TYPE, DISPLAY_LINECHART);
            if (this.chartType == DISPLAY_TABLE || this.chartType == DISPLAY_BARTABLE || this.chartType == DISPLAY_BUBBLE || this.chartType == DISPLAY_SANKEY || this.chartType == DISPLAY_WORDTREE || this.chartType == DISPLAY_TIMELINECHART || this.chartType == DISPLAY_PIECHART) {
                //                    return pointData.getRecordFields();
                return pointData.getNonGeoFields();
            }
            return pointData.getChartableFields();
        },
        canDoGroupBy: function() {
            return this.chartType == DISPLAY_PIECHART || this.chartType == DISPLAY_TABLE;
        },
        clearCache: function() {
            this.computedData = null;
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
                var func = function() {
                    _this.displayData();
                }
                this.setContents(this.getLoadingMessage());
                setTimeout(func, 500);
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
            var chartType = this.getProperty(PROP_CHART_TYPE, DISPLAY_LINECHART);
            var selectedFields = this.getSelectedFields([]);
            if (selectedFields.length == 0 && this.lastSelectedFields != null) {
                selectedFields = this.lastSelectedFields;
            }

            if (selectedFields == null || selectedFields.length == 0) {
                if (this.chartType == DISPLAY_TABLE) {
                    //                        selectedFields = this.allFields;
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
                includeIndex: true,
            };
            if (this.chartType == DISPLAY_TABLE || this.chartType == DISPLAY_BARTABLE || chartType == DISPLAY_PIECHART || chartType == DISPLAY_SCATTERPLOT || chartType == DISPLAY_HISTOGRAM || chartType == DISPLAY_BUBBLE || chartType == DISPLAY_GAUGE || chartType == DISPLAY_SANKEY || chartType == DISPLAY_WORDTREE ||chartType == DISPLAY_TIMELINECHART) {
                props.includeIndex = false;
            }
            props.groupByIndex = -1;

            if (chartType == DISPLAY_PIECHART) {
                if (!this.groupBy && this.groupBy != "") {
                    var stringField = this.getFieldOfType(this.allField, "string");
                    if (stringField) {
                        this.groupBy = stringField.getId();
                    }
                }

            }


            if (this.groupBy) {
                for (var i = 0; i < this.allFields.length; i++) {
                    var field = this.allFields[i];
                    if (field.getId() == this.groupBy) {
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



            if (chartType == DISPLAY_BARTABLE) {
                props.includeIndexIfDate = true;
            }


            var dataHasIndex = props.includeIndex;
            var dataList = this.computedData;
            if (this["function"] && this.computedData == null) {
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
                    setVars += "\tvar " + field.getId() + "=args." + field.getId() + ";\n";
                }
                var code = "function displayChartEval(args) {\n" + setVars + "\treturn  " + this["function"] + "\n}";
                eval(code);
                var newList = [];
                var fieldNames = null;
                var rowCnt = -1;
                var indexField = this.indexField;
                for (var rowIdx = 0; rowIdx < records.length; rowIdx++) {
                    var record = records[rowIdx];
                    var row = record.getData();
                    var date = record.getDate();
                    if (!this.dateInRange(date)) continue;
                    rowCnt++;
                    var values = [];
                    var indexName = null;
                    if (indexField >= 0) {
                        var field = allFields[indexField];
                        values.push(record.getValue(indexField) + offset);
                        indexName = field.getLabel();
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


            //                console.log("fields:" + selectedFields +" data.length = " + dataList.length +" " + dataList);

            if (dataList.length == 0 && !this.userHasSelectedAField) {
                var pointData = this.dataCollection.getList()[0];
                var chartableFields = this.getFieldsToSelect(pointData);
                for (var i = 0; i < chartableFields.length; i++) {
                    var field = chartableFields[i];
                    dataList = this.getStandardData([field], props);
                    if (dataList.length > 0) {
                        this.setSelectedFields([field]);
                        //                            console.log("Defaulting to field:" + field.getId());
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
                            //xb                                console.log("fields:" + fields.length +" j:" + j +" id:" +fieldsToSelect[j].getId() +" is:" + valueIsNumber);

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
                        //                            console.log("header:"  + newHeader)
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
                    //                        console.log("row "  + j +":"  + newRow)
                    newList.push(newRow);
                }
                dataList = newList;
            }

            try {
                this.makeChart(chartType, dataList, props, selectedFields);
            } catch (e) {
                console.log(e.stack);
                this.displayError("" + e);
                return;
            }

            var d = _this.jq(ID_CHART);
            this.lastWidth = d.width();
            if (d.width() == 0) {
                //                    _this.checkWidth(0);
            }
        },
        //This keeps checking the width of the chart element if its zero
        //we do this for displaying in tabs
        checkLayout: function() {
            var _this = this;
            var d = _this.jq(ID_CHART);
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
            if (this.chart != null) {
                this.chart.clearChart();
            }
        },
        setChartSelection: function(index) {
            if (this.chart != null) {
                if (this.chart.setSelection) {
                    this.chart.setSelection([{
                        row: index,
                        column: null
                    }]);
                }
                //                    var container = $('#table_div').find('.google-visualization-table-table:eq(0)').parent();
                //                    var header = $('#table_div').find('.google-visualization-table-table:eq(1)').parent();
                //                    var row = $('.google-visualization-table-tr-sel');
                //                    $(container).prop('scrollTop', $(row).prop('offsetTop') - $(header).height());
            }
        },

        tableHeaderMouseover: function(i, tooltip) {
            //alert("new:" + tooltip);
        },
        makeDataTable: function(chartType, dataList, props, selectedFields) {
            dataList = this.filterData(dataList, selectedFields);
            if (this.chartType == DISPLAY_SANKEY) {
                if (!this.getProperty("doCategories", false)) {
                    return google.visualization.arrayToDataTable(this.makeDataArray(dataList));
                }
                var strings = [];
                for (var i = 0; i < selectedFields.length; i++) {
                    var field = selectedFields[i];
                    if (field.getType() == "string") {
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

            if (dataList.length == 1) {
                return google.visualization.arrayToDataTable(this.makeDataArray(dataList));
            }


            if (this.chartType == DISPLAY_TIMELINECHART) {
                var records = this.filterData();
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
                if (stringField)
                    dataTable.addColumn({
                        type: 'string',
                        id: stringField.getLabel()
                    });
                else
                    dataTable.addColumn({
                        type: 'string',
                        id: "Index"
                    });
                if (labelFields.length > 0)
                    dataTable.addColumn({
                        type: 'string',
                        id: "Label"
                    });
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



            if (this.chartType == DISPLAY_TABLE) {
                return google.visualization.arrayToDataTable(this.makeDataArray(dataList));
            }
            if (this.chartType == DISPLAY_GAUGE) {
                return this.makeGaugeDataTable(this.makeDataArray(dataList));
            }
            if (this.chartType == DISPLAY_HISTOGRAM) {
                return google.visualization.arrayToDataTable(this.makeDataArray(dataList));
            }
            if (this.chartType == DISPLAY_BUBBLE) {
                return google.visualization.arrayToDataTable(this.makeDataArray(dataList));
            }

            if (this.chartType == DISPLAY_CALENDAR) {
                var dataTable = new google.visualization.DataTable();
                var header = this.getDataValues(dataList[0]);
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

            if (this.chartType == DISPLAY_PIECHART) {
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

            var dataTable = new google.visualization.DataTable();
            var header = this.getDataValues(dataList[0]);
            var sample = this.getDataValues(dataList[1]);
            for (var j = 0; j < header.length; j++) {
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
                    //Assume all remaining fields are numbers
                    dataTable.addColumn('number', header[j]);
                    dataTable.addColumn({
                        type: 'string',
                        role: 'tooltip',
                        'p': {
                            'html': true
                        }
                    });
                }
            }
            //                dataTable.addColumn({type:'string',  role: 'annotation' });


            var justData = [];
            var begin = props.includeIndex ? 1 : 0;
            var tooltipFields = [];
            var toks = this.getProperty("tooltipFields", "").split(",");
            for (var i = 0; i < toks.length; i++) {
                var tooltipField = this.getFieldById(null, toks[i]);
                if (tooltipField)
                    tooltipFields.push(tooltipField);
            }


            for (var i = 1; i < dataList.length; i++) {
                var row = this.getDataValues(dataList[i]);
                row = row.slice(0);
                var label = "";
                if (dataList[i].record) {
                    for (var j = 0; j < tooltipFields.length; j++) {
                        label += "<b>" + tooltipFields[j].getLabel() + "</b>: " +
                            dataList[i].record.getValue(tooltipFields[j].getIndex()) + "<br>";
                    }
                }

                var tooltip = "<div style='padding:8px;'>";
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
                tooltip += "</div>";

                newRow = [];
                for (var j = 0; j < row.length; j++) {
                    var value = row[j];
                    newRow.push(value);
                    if (j == 0 && props.includeIndex) {
                        //is the index so don't add a tooltip
                    } else {
                        newRow.push(tooltip);
                    }
                }
                //                    newRow.push("annotation");
                justData.push(newRow);
            }
            dataTable.addRows(justData);
            return dataTable;
        },

        makeGoogleChart: function(chartType, dataList, props, selectedFields) {
            //                for(var i=0;i<selectedFields.length;i++) 
            //                    console.log(selectedFields[i].getId());
            //                console.log("makeGoogleChart:" + chartType);
            if (typeof google == 'undefined') {
                this.setContents("No google");
                return;
            }

            if (chartType == DISPLAY_TABLE && dataList.length > 0) {
                var header = this.getDataValues(dataList[0]);
                /*
                var get = this.getGet();
                for(var i=0;i<header.length;i++) {
                    var s = header[i];
                    var tt = "tooltip";
                    s = HtmlUtils.tag("div",["onmouseover", get +".tableHeaderMouseover(" + i+",'" + tt +"');"],s);
                    header[i] = s;
                }
                */
            }

            var dataTable = this.makeDataTable(chartType, dataList, props, selectedFields);
            var chartOptions = {
                tooltip: {
                    isHtml: true
                },
            };
            $.extend(chartOptions, {
                lineWidth: 1,
                colors: this.colorList,
                curveType: this.curveType,
                vAxis: {}
            });


            if (this.lineWidth) {
                chartOptions.lineWidth = this.lineWidth;
            }
            if (this.fontSize > 0) {
                chartOptions.fontSize = this.fontSize;
            }



            var defaultRange = this.getDisplayManager().getRange(selectedFields[0]);

            var range = [NaN, NaN];
            if (!isNaN(this.vAxisMinValue)) {
                range[0] = parseFloat(this.vAxisMinValue);
            } else if (defaultRange != null) {
                range[0] = defaultRange[0];
            }
            if (!isNaN(this.vAxisMaxValue)) {
                range[1] = parseFloat(this.vAxisMaxValue);
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
            var width = "90%";
            var left = "10%";
            useMultipleAxes = this.getProperty("useMultipleAxes", true);

            if ((selectedFields.length > 1 && useMultipleAxes) || this.getProperty("padRight", false) === true) {
                width = "80%";
            }
            var chartId = this.getDomId(ID_CHART);
            var divAttrs = [ATTR_ID, chartId];
            if (chartType == DISPLAY_PIECHART) {
                divAttrs.push("style");
                var style = "";
                if (this.getProperty("width")) {
                    var width = this.getProperty("width");
                    if (width > 0)
                        style += "width:" + width + "px;";
                    else if (width < 0)
                        style += "width:" + (-width) + "%;";
                    else
                        style += "width:" + width;
                } else {
                    style += "width:" + "100%;";
                }
                if (this.getProperty("height")) {
                    var height = this.getProperty("height");
                    if (height > 0)
                        style += "height:" + height + "px;";
                    else if (height < 0)
                        style += "height:" + (-height) + "%;";
                    else
                        style += "height:" + height;
                } else {
                    style += "height:" + "100%;";
                }
                divAttrs.push(style);
            } else {
                //                    divAttrs.push("style");
                //                    divAttrs.push("width:600px;");
            }
            divAttrs.push("style");
            divAttrs.push("height:100%;");
            this.setContents(HtmlUtils.div(divAttrs, ""));


            if (chartType == DISPLAY_SANKEY) {
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
            }

            if (chartType == DISPLAY_LINECHART || chartType == DISPLAY_AREACHART ||
                chartType == DISPLAY_BARCHART ||
                chartType == DISPLAY_HISTOGRAM ||
                chartType == DISPLAY_BARSTACK) {
                chartOptions.height = this.getProperty("chartHeight", this.getProperty("height", "150"));
                $.extend(chartOptions, {
                    //series: [{targetAxisIndex:0},{targetAxisIndex:1},],
                    legend: {
                        position: 'bottom'
                    },
                    chartArea: {
                        left: this.getProperty("chartLeft", left),
                        top: this.getProperty("chartTop", "10"),
                        height: this.getProperty("chartHeight", "70%"),
                        width: this.getProperty("chartWidth", width),
                    },
                });
                //                    console.log(JSON.stringify(chartOptions));
                if (useMultipleAxes) {
                    $.extend(chartOptions, {
                        series: [{
                            targetAxisIndex: 0
                        }, {
                            targetAxisIndex: 1
                        }]
                    });
                }

                if (this.getProperty("showTrendLines", false)) {
                    chartOptions.trendlines = {
                        0: {
                            type: 'linear',
                            color: 'green',
                        }
                    };
                }

                if (this.hAxis) {
                    if (chartOptions.hAxis) {
                        chartOptions.hAxis.title = this.hAxis;
                    } else {
                        chartOptions.hAxis = {
                            title: this.hAxis
                        }
                    }
                }
                if (this.vAxis) {
                    if (chartOptions.vAxis) {
                        chartOptions.vAxis.title = this.vAxis;
                    } else {
                        chartOptions.vAxis = {
                            title: this.vAxis
                        }
                    }
                }

                if (Utils.isDefined(this.chartHeight)) {
                    chartOptions.height = this.chartHeight;
                }
            }

            if (chartType == DISPLAY_BARTABLE) {
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
                //                    console.log(chartOptions);
                //                    this.chart = new google.visualization.BarChart(document.getElementById(chartId));
                this.chart = new google.charts.Bar(document.getElementById(chartId));


            } else if (chartType == DISPLAY_BARCHART || chartType == DISPLAY_BARSTACK) {
                if (chartType == DISPLAY_BARSTACK) {
                    chartOptions.isStacked = true;
                }
                if (this.getProperty("barWidth")) {
                    chartOptions.bar = {
                        groupWidth: this.getProperty("barWidth")
                    }
                }
                chartOptions.orientation = "horizontal";
                this.chart = new google.visualization.BarChart(document.getElementById(chartId));
            } else if (chartType == DISPLAY_SANKEY) {
                this.chart = new google.visualization.Sankey(document.getElementById(chartId));
            } else if (chartType == DISPLAY_WORDTREE) {
                if(this.getProperty("chartHeight")) 
                    chartOptions.height= parseInt(this.getProperty("chartHeight"));
                if(this.getProperty("wordColors")) {
                    var tmp = this.getProperty("wordColors").split(",");
                    var colors = [];
                    for(var i=0;i<3 && i<tmp.length;i++) {
                        colors.push(tmp[i]);
                    }
                    if(colors.length==3)
                        chartOptions.colors= colors;
                }

                if(this.getProperty("chartWidth"))  {
                    chartOptions.width= parseInt(this.getProperty("chartWidth"));
                }

                chartOptions.wordtree  = {
                    format: 'implicit',
                    wordSeparator:"_SEP_",
                    word: this.getProperty("treeRoot","root"),
                    //                    type: this.getProperty("treeType","double")

                }
                if(this.getProperty("maxFontSize")) {
                    chartOptions.maxFontSize = parseInt(this.getProperty("maxFontSize"));
                    console.log(chartOptions.wordtree.maxFontSize+ " " + this.getProperty("maxFontSize"));
                }

                this.chart = new google.visualization.WordTree(document.getElementById(chartId));
            } else if (chartType == DISPLAY_SCATTERPLOT) {
                var height = 400;
                if (Utils.isDefined(this.chartHeight)) {
                    height = this.chartHeight;
                }
                var width = "100%";
                if (Utils.isDefined(this.chartWidth)) {
                    width = this.chartWidth;
                }
                //                    $("#" + chartId).css("border","1px red solid");
                //                    $("#" + chartId).css("width",width);
                $("#" + chartId).css("width", width);
                $("#" + chartId).css("height", height);
                chartOptions = {
                    title: '',
                    tooltip: {
                        isHtml: true
                    },
                    legend: 'none',
                    chartArea: {
                        left: "10%",
                        top: 10,
                        height: "80%",
                        width: "90%"
                    }
                };

                if (this.getShowTitle()) {
                    chartOptions.title = this.getTitle(true);
                }

                if (dataList.length > 0 && this.getDataValues(dataList[0]).length > 1) {
                    chartOptions.hAxis = {
                        title: this.getDataValues(dataList[0])[0]
                    };
                    chartOptions.vAxis = {
                        title: this.getDataValues(dataList[0])[1]
                    };
                    //We only have the one vAxis range for now
                    if (!isNaN(this.vAxisMinValue)) {
                        chartOptions.hAxis.minValue = this.vAxisMinValue;
                        chartOptions.vAxis.minValue = this.vAxisMinValue;
                    }
                    if (!isNaN(this.vAxisMaxValue)) {
                        chartOptions.hAxis.maxValue = this.vAxisMaxValue;
                        chartOptions.vAxis.maxValue = this.vAxisMaxValue;
                    }
                }


                this.chart = new google.visualization.ScatterChart(document.getElementById(chartId));
            } else if (chartType == DISPLAY_HISTOGRAM) {
                if (this.legendPosition) {
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
                if (!isNaN(this.vAxisMaxValue)) {
                    chartOptions.vAxis.maxValue = parseFloat(this.vAxisMaxValue);
                }
                //                    console.log(JSON.stringify(chartOptions));
                if (!isNaN(this.vAxisMinValue)) {
                    chartOptions.vAxis.minValue = parseFloat(this.vAxisMinValue);
                }
                this.chart = new google.visualization.Histogram(document.getElementById(chartId));

            } else if (chartType == DISPLAY_GAUGE) {
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
                this.chart = new google.visualization.Gauge(document.getElementById(chartId));
            } else if (chartType == DISPLAY_BUBBLE) {
                var ct = this.getColorTable(true);
                if (ct) {
                    chartOptions.colors = ct;
                } else if (!this.colors) {
                    chartOptions.colors = this.colorList;
                }
                if (chartOptions.colors) {
                    chartOptions.colors = Utils.getColorTable("rainbow", true);
                }

                chartOptions.chartArea = {
                    left: 100,
                    top: 10,
                    width: '98%',
                    height: '90%'
                }
                chartOptions.colorAxis = {
                    legend: {
                        position: "in"
                    }
                }

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
                this.chart = new google.visualization.BubbleChart(document.getElementById(chartId));
            } else if (chartType == DISPLAY_CALENDAR) {
                chartOptions.calendar = {
                    cellSize: parseInt(this.getProperty("cellSize", 15))
                };
                //If a calendar is show in tabs then it never returns from the draw
                if (this.jq(ID_CHART).width() == 0) {
                    return;
                }
                this.chart = new google.visualization.Calendar(document.getElementById(chartId));
            } else if (chartType == DISPLAY_TIMELINECHART) {
                this.chart = new google.visualization.Timeline(document.getElementById(chartId));
            } else if (chartType == DISPLAY_PIECHART) {
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
                this.chart = new google.visualization.PieChart(document.getElementById(chartId));
            } else if (chartType == DISPLAY_TABLE) {

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
                this.chart = new google.visualization.Table(document.getElementById(chartId));
            } else if (chartType == DISPLAY_AREACHART) {
                if (this.isStacked)
                    chartOptions.isStacked = true;
                this.chart = new google.visualization.AreaChart(document.getElementById(chartId));
            } else {
                //                    this.chart =  new Dygraph.GVizChart(
                //                    document.getElementById(chartId));
                this.chart = new google.visualization.LineChart(document.getElementById(chartId));
            }
            if (this.chart != null) {
                if (!Utils.isDefined(chartOptions.height)) {
                    chartOptions.height = "100%";
                }
                this.chart.draw(dataTable, chartOptions);
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
                theDisplay.displayManager.propagateEventRecordSelection(theDisplay,
                    theDisplay.dataCollection.getList()[0], {
                        index: 0
                    });
                google.visualization.events.addListener(this.chart, 'select', function(event) {
                    if (theDisplay.chart.getSelection) {
                        var selected = theDisplay.chart.getSelection();
                        if (selected && selected.length > 0) {
                            var index = selected[0].row;
                            theDisplay.displayManager.propagateEventRecordSelection(theDisplay,
                                theDisplay.dataCollection.getList()[0], {
                                    index: index
                                });
                        }
                    }
                });
            }
        }
    });



    this.makeChart = this.makeGoogleChart;
}



function LinechartDisplay(displayManager, id, properties) {
    properties = $.extend({
        "chartType": DISPLAY_LINECHART
    }, properties);
    RamaddaUtil.inherit(this, new RamaddaMultiChart(displayManager, id, properties));
    addRamaddaDisplay(this);
}

function AreachartDisplay(displayManager, id, properties) {
    properties = $.extend({
        "chartType": DISPLAY_AREACHART
    }, properties);
    RamaddaUtil.inherit(this, new RamaddaMultiChart(displayManager, id, properties));
    addRamaddaDisplay(this);
}

function BarchartDisplay(displayManager, id, properties) {
    properties = $.extend({
        "chartType": DISPLAY_BARCHART
    }, properties);
    RamaddaUtil.inherit(this, new RamaddaMultiChart(displayManager, id, properties));
    addRamaddaDisplay(this);
}

function BarstackDisplay(displayManager, id, properties) {
    properties = $.extend({
        "chartType": DISPLAY_BARSTACK,
        "isStacked": true
    }, properties);
    RamaddaUtil.inherit(this, new RamaddaMultiChart(displayManager, id, properties));
    addRamaddaDisplay(this);
}

function PiechartDisplay(displayManager, id, properties) {
    properties = $.extend({
        "chartType": DISPLAY_PIECHART
    }, properties);
    RamaddaUtil.inherit(this, new RamaddaMultiChart(displayManager, id, properties));
    addRamaddaDisplay(this);
}

function TimelinechartDisplay(displayManager, id, properties) {
    properties = $.extend({
        "chartType": DISPLAY_TIMELINECHART
    }, properties);
    RamaddaUtil.inherit(this, new RamaddaMultiChart(displayManager, id, properties));
    addRamaddaDisplay(this);
}

function SankeyDisplay(displayManager, id, properties) {
    properties = $.extend({
        "chartType": DISPLAY_SANKEY
    }, properties);
    RamaddaUtil.inherit(this, new RamaddaMultiChart(displayManager, id, properties));
    addRamaddaDisplay(this);
}

function WordtreeDisplay(displayManager, id, properties) {
    properties = $.extend({
        "chartType": DISPLAY_WORDTREE
    }, properties);
    RamaddaUtil.inherit(this, new RamaddaMultiChart(displayManager, id, properties));
    addRamaddaDisplay(this);
    $.extend(this, {
            handleEventRecordSelection: function(source, args) {
          },
          makeDataTable: function(chartType, dataList, props, selectedFields) {
                //null ->get all data
            var root = this.getProperty("treeRoot","root");
            var records = this.filterData(null, selectedFields);
            var fields = this.getSelectedFields(this.getData().getRecordFields());
            var valueField = this.getFieldById(null, this.getProperty("colorBy"));
            var values = [];
            var typeTuple=["phrases"];
            values.push(typeTuple);
            var fixedSize = this.getProperty("fixedSize");
            if(valueField)
                fixedSize=1;
            if(fixedSize) typeTuple.push("size");
            if(valueField)
                typeTuple.push("value");
            var fieldInfo = {};

            var header="";
            for (var i = 0; i < fields.length; i++) {
                var field = fields[i];
                if(header!="")
                    header+=" -&gt;";
                header+=field.getLabel();
                if(!field.isFieldNumeric()) continue;
                var column = this.getColumnValues(records, field);
                var buckets = [];
                var argBuckets = this.getProperty("buckets." + field.getId(),this.getProperty("buckets",null));
                var min,max;
                if(argBuckets) {
                    var argBucketLabels = this.getProperty("bucketLabels." + field.getId(),this.getProperty("bucketLabels",null));
                    var bucketLabels;
                    if(argBucketLabels)
                        bucketLabels = argBucketLabels.split(",");
                    var bucketList  = argBuckets.split(",");
                    var prevValue  = 0;
                    for(var bucketIdx=0;bucketIdx<bucketList.length;bucketIdx++)  {
                        var v = parseFloat(bucketList[bucketIdx]);
                        if(bucketIdx==0) {
                            min = v;
                            max = v;
                        }
                        min = Math.min(min,v);
                        max = Math.max(max,v);
                        if(bucketIdx>0)  {
                            var label;
                            if(bucketLabels && i<=bucketLabels.length)
                                label = bucketLabels[bucketIdx-1];
                            else
                                label = Utils.formatNumber(prevValue, true) +"-" + Utils.formatNumber(v, true);
                            buckets.push({min:prevValue,max:v,label:label});
                        }
                        prevValue = v;
                    }
                } else {
                    var numBuckets = parseInt(this.getProperty("numBuckets." + field.getId(),this.getProperty("numBuckets",10)));
                    min = column.min;
                    max = column.max;
                    var step = (column.max-column.min)/numBuckets;
                    for(var bucketIdx=0;bucketIdx<numBuckets;bucketIdx++) {
                        var r1 = column.min+(bucketIdx*step);
                        var r2 = column.min+((bucketIdx+1)*step);
                        var label = Utils.formatNumber(r1, true) +"-" + Utils.formatNumber(r2, true);
                        buckets.push({min:r1,max:r2,label:label});
                    }
                }
                fieldInfo[field.getId()] = {min:min,max:max,buckets:buckets};
            }

            var sep = "_SEP_";
            for (var rowIdx = 0; rowIdx < records.length; rowIdx++) {
                var row = this.getDataValues(records[rowIdx]);
                var string = root;
                for (var fieldIdx = 0; fieldIdx < fields.length; fieldIdx++) {
                    var field = fields[fieldIdx];
                    string +=sep;
                    var value = row[field.getIndex()];
                    if(field.isFieldNumeric()) {
                        var info = fieldInfo[field.getId()];
                        for(var bucketIdx=0;bucketIdx<info.buckets.length;bucketIdx++) {
                            var bucket  = info.buckets[bucketIdx]; 
                            if(value>=bucket.min && value<=bucket.max) {
                                value=bucket.label;
                                break;
                            }
                        }
                    } 
                    string+=value;
                }
                var data = [string.trim()];
                if(fixedSize) data.push(parseInt(fixedSize));
                if(valueField)
                    data.push(row[valueField.getIndex()]);
                values.push(data);
            }
            if(this.getProperty("header")) {
                header = this.getProperty("header","");
            } else {
                header = "<b>Fields: </b>" +header;
                if(this.getProperty("headerPrefix")) 
                    header = this.getProperty("headerPrefix") + " " + header;
            }
            this.writeHtml(ID_DISPLAY_TOP,header);
            return google.visualization.arrayToDataTable(values);
            },
                });
}

function CalendarDisplay(displayManager, id, properties) {
    properties = $.extend({
        "chartType": DISPLAY_CALENDAR
    }, properties);
    RamaddaUtil.inherit(this, new RamaddaMultiChart(displayManager, id, properties));
    addRamaddaDisplay(this);
    RamaddaUtil.inherit(this, {
        xgetContentsStyle: function() {
            var height = this.getProperty("height", 200);
            if (height > 0) {
                return " height:" + height + "px; " + " max-height:" + height + "px; overflow-y: auto;";
            }
            return "";
        },
        canDoMultiFields: function() {
            return false;
        }
    });
}


function TableDisplay(displayManager, id, properties) {
    properties = $.extend({
        "chartType": DISPLAY_TABLE
    }, properties);
    RamaddaUtil.inherit(this, new RamaddaMultiChart(displayManager, id, properties));
    addRamaddaDisplay(this);
}

function HistogramDisplay(displayManager, id, properties) {
    properties = $.extend({
        "chartType": DISPLAY_HISTOGRAM
    }, properties);
    RamaddaUtil.inherit(this, new RamaddaMultiChart(displayManager, id, properties));
    addRamaddaDisplay(this);
    RamaddaUtil.inherit(this, {
        okToHandleEventRecordSelection: function() {
            return false;
        },
    });
}

function GaugeDisplay(displayManager, id, properties) {
    properties = $.extend({
        "chartType": DISPLAY_GAUGE
    }, properties);
    RamaddaUtil.inherit(this, new RamaddaMultiChart(displayManager, id, properties));
    addRamaddaDisplay(this);
    RamaddaUtil.inherit(this, {
        makeGaugeDataTable: function(dataList) {
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
                var dataTable = this.makeGaugeDataTable(this.dataList);
                this.chart.draw(dataTable, this.chartOptions);
            }
        },
    });
}

function BubbleDisplay(displayManager, id, properties) {
    properties = $.extend({
        "chartType": DISPLAY_BUBBLE
    }, properties);
    RamaddaUtil.inherit(this, new RamaddaMultiChart(displayManager, id, properties));
    addRamaddaDisplay(this);
}


function BartableDisplay(displayManager, id, properties) {
    properties = $.extend({
        "chartType": DISPLAY_BARTABLE
    }, properties);
    RamaddaUtil.inherit(this, new RamaddaMultiChart(displayManager, id, properties));
    $.extend(this, {
        getDefaultSelectedFields: function(fields, dfltList) {
            var f = [];
            for (i = 0; i < fields.length; i++) {
                var field = fields[i];
                if (!field.isNumeric) {
                    f.push(field);
                    break;
                }
            }
            for (i = 0; i < fields.length; i++) {
                var field = fields[i];
                if (field.isNumeric) {
                    f.push(field);
                    break;
                }
            }
            return f;
        }
    });

    addRamaddaDisplay(this);
}



function ScatterplotDisplay(displayManager, id, properties) {
    properties = $.extend({
        "chartType": DISPLAY_SCATTERPLOT
    }, properties);
    RamaddaUtil.inherit(this, new RamaddaMultiChart(displayManager, id, properties));
    $.extend(this, {
        getDefaultSelectedFields: function(fields, dfltList) {
            var f = [];
            for (i = 0; i < fields.length; i++) {
                var field = fields[i];
                if (field.isNumeric) {
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


function RamaddaTextDisplay(displayManager, id, properties) {
    var SUPER;
    $.extend(this, SUPER = new RamaddaDisplay(displayManager, id, DISPLAY_TEXT, properties));
    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
        lastHtml: "<p>&nbsp;<p>&nbsp;<p>",
        initDisplay: function() {
            this.createUI();
            this.setContents(this.lastHtml);
        },
        handleEventRecordSelection: function(source, args) {
            this.lastHtml = args.html;
            this.setContents(args.html);
        }
    });
}


function RamaddaStatsDisplay(displayManager, id, properties, type) {
    var SUPER;
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
    RamaddaUtil.inherit(this, SUPER = new RamaddaFieldsDisplay(displayManager, id, type || DISPLAY_STATS, properties));

    if (!type)
        addRamaddaDisplay(this);


    RamaddaUtil.defineMembers(this, {
        "map-display": false,
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
                if (!justOne && (!this.showText && !field.isNumeric)) continue;
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
                    stats[col].isNumber = field.isNumeric;
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
                if (this.showType) {
                    header.push("Type");
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
                if (this.showType) {
                    values.push(stats[col].type);
                }
                right = HtmlUtils.tds(["align", "right"], values);
                var align = (justOne ? "right" : "left");
                var label = field.getLabel();
                var toks = label.split("!!");
                var title = field.getId();
                label = toks[toks.length - 1];
                if (justOne) {
                    label += ":";
                }
                label = label.replace(/ /g, "&nbsp;")
                var row = HtmlUtils.tr([], HtmlUtils.td(["align", align], "<b>" + HtmlUtils.tag("div", ["title", title], label) + "</b>") + right);
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





function RamaddaCrosstabDisplay(displayManager, id, properties) {
    var SUPER;
    RamaddaUtil.inherit(this, SUPER = new RamaddaFieldsDisplay(displayManager, id, DISPLAY_CROSSTAB, properties));
    addRamaddaDisplay(this);
    this.columns = this.getProperty("columns", "").split(",");
    this.rows = this.getProperty("rows", "").split(",");
    RamaddaUtil.defineMembers(this, {
        "map-display": false,
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
            var fieldMap = {};
            cols = [];
            rows = [];

            for (var j = 0; j < this.columns.length; j++) {
                var name = this.columns[j];
                for (var i = 0; i < allFields.length; i++) {
                    field = allFields[i];
                    if (name == field.getLabel() || name == ("#" + (i + 1))) {
                        cols.push(allFields[i]);
                        break;
                    }
                }
            }
            for (var j = 0; j < this.rows.length; j++) {
                var name = this.rows[j];
                for (var i = 0; i < allFields.length; i++) {
                    if (name == allFields[i].getLabel() || name == ("#" + (i + 1))) {
                        rows.push(allFields[i]);
                        break;
                    }
                }
            }
            var html = HtmlUtils.openTag("table", ["border", "1px", "bordercolor", "#ccc", "class", "display-stats", "cellspacing", "1", "cellpadding", "5"]);
            var uniques = {};
            var seen = {};
            for (var j = 0; j < cols.length; j++) {
                var col = cols[j];
                var key = col.getLabel();
                for (var rowIdx = 1; rowIdx < dataList.length; rowIdx++) {
                    var tuple = this.getDataValues(dataList[rowIdx]);
                    var colValue = tuple[col.getIndex()];
                    if (!(key in uniques)) {
                        uniques[key] = [];
                    }
                    var list = uniques[key];
                    if (list.indexOf(colValue) < 0) {
                        console.log(colValue);
                        list.push(colValue);
                    }
                }
            }

            for (key in uniques) {
                uniques[key].sort();
                console.log(uniques[key]);
            }


            for (var j = 0; j < cols.length; j++) {
                var col = cols[j];
                for (var rowIdx = 1; rowIdx < dataList.length; rowIdx++) {
                    var tuple = this.getDataValues(dataList[rowIdx]);
                    //                        var colValue = tuple;
                    //html += HtmlUtils.tr([],HtmlUtils.tds(["class","display-stats-header","align","center"],["","Min","Max","Total","Average"]));
                    for (var i = 0; i < rows.length; i++) {
                        var row = rows[j];


                    }
                }
            }
            html += "</table>";
            this.setContents(html);
            this.initTooltip();
        },
    });
}




function RamaddaCorrelationDisplay(displayManager, id, properties) {
    var SUPER;
    var ID_BOTTOM = "bottom";
    $.extend(this, {
        colorTable: "red_white_blue",
        colorByMin: "-1",
        colorByMax: "1",
    });

    RamaddaUtil.inherit(this, SUPER = new RamaddaFieldsDisplay(displayManager, id, DISPLAY_CORRELATION, properties));
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
            var html = HtmlUtils.openTag("table", ["border", "0", "class", "display-correlation"]);
            html += "<tr valign=bottom><td class=display-heading>&nbsp;</td>";
            for (var fieldIdx = 0; fieldIdx < fields.length; fieldIdx++) {
                var field1 = fields[fieldIdx];
                if (!field1.isFieldNumeric() || field1.isFieldGeo()) continue;
                html += "<td align=center class=top-heading>" + HtmlUtils.tag("div", ["class", "top-heading"], field1.getLabel()) + "</td>";
            }
            html += "</tr>";

            var colors = null;
            colorByMin = parseFloat(this.colorByMin);
            colorByMax = parseFloat(this.colorByMax);
            colors = this.getColorTable(true);
            var colCnt = 0;
            for (var fieldIdx1 = 0; fieldIdx1 < fields.length; fieldIdx1++) {
                var field1 = fields[fieldIdx1];
                if (!field1.isFieldNumeric() || field1.isFieldGeo()) continue;
                colCnt++;
                html += "<tr><td>" + HtmlUtils.tag("div", ["class", "side-heading"], field1.getLabel().replace(/ /g, "&nbsp;")) + "</td>";
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
                    html += "<td align=right style=\"" + style + "\">" + HtmlUtils.tag("div", ["class", "display-correlation-element", "title", "&rho;(" + rowName + "," + colName + ")"], r.toFixed(3)) + "</td>";
                }
                html += "</tr>";
            }
            html += "<tr><td></td><td colspan = " + colCnt + ">" + HtmlUtils.div(["id", this.getDomId(ID_BOTTOM)], "") + "</td></tr>";
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


function RamaddaHeatmapDisplay(displayManager, id, properties) {
    var SUPER;
    $.extend(this, {
        colorTable: "red_white_blue",
    });

    RamaddaUtil.inherit(this, SUPER = new RamaddaFieldsDisplay(displayManager, id, DISPLAY_HEATMAP, properties));
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
                html += "<td align=center class=top-heading>" + HtmlUtils.tag("div", ["class", "top-heading"], header[0]) + "</td>";
            }
            for (var fieldIdx = 0; fieldIdx < fields.length; fieldIdx++) {
                var field = fields[fieldIdx];
                if ((!field.isFieldNumeric() || field.isFieldGeo())) continue;
                html += "<td align=center class=top-heading>" + HtmlUtils.tag("div", ["class", "top-heading"], field.getLabel()) + "</td>";
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
                    //                        html+="<td>" + HtmlUtils.tag("div",[HtmlUtils.attr("class","side-heading")+ extraCellStyle], rowLabel) +"</td>";
                    html += HtmlUtils.td(["class", "side-heading", "style", extraCellStyle], rowLabel);
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
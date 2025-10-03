/**
   Copyright (c) 2008-2025 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/


function RamaddaXlsDisplay(displayManager, id, properties) {

    var COORD_X = "xaxis";
    var COORD_Y = "yaxis";
    var COORD_GROUP = "group";


    var ID_SEARCH = "search";
    var ID_SEARCH_PREFIX = "table";
    var ID_SEARCH_EXTRA = "searchextra";
    var ID_SEARCH_HEADER = "searchheader";
    var ID_RESULTS = "results";
    var ID_DOWNLOADURL = "downloadurl";
    var ID_CONTENTS = "tablecontents";
    var ID_SEARCH_DIV = "searchdiv";
    var ID_SEARCH_FORM = "searchform";
    var ID_SEARCH_TEXT = "searchtext";
    var ID_TABLE_HOLDER = "tableholder";
    var ID_TABLE = "table";
    var ID_CHARTTOOLBAR = "charttoolbar";
    var ID_CHART = "chart";

    RamaddaUtil.inherit(this, new RamaddaDisplay(displayManager, id, "xls", properties));
    addRamaddaDisplay(this);


    this.url = properties.url;
    this.tableProps = {
        fixedRowsTop: 0,
        fixedColumnsLeft: 0,
        rowHeaders: true,
        colHeaders: true,
        headers: null,
        skipRows: 0,
        skipColumns: 0,
    };
    if (properties != null) {
        $.extend(this.tableProps, properties);
    }


    RamaddaUtil.defineMembers(this, {
        initDisplay: function() {
            this.createUI();
            this.setDisplayTitle("Table Data");
            var body =
                HU.div([ATTR_ID, this.getDomId(ID_SEARCH_HEADER)]) +
                HU.div([ATTR_ID, this.getDomId(ID_TABLE_HOLDER)]) +
                HU.div([ATTR_ID, this.getDomId(ID_CHARTTOOLBAR)]) +
                HU.div([ATTR_ID, this.getDomId(ID_CHART)]);
            this.setContents(body);
            this.loadTableData(this.url);
        },
    });


    RamaddaUtil.defineMembers(this, {
        currentSheet: 0,
        currentData: null,
        columnLabels: null,
        startRow: 0,
        groupIndex: -1,
        xAxisIndex: -1,
        yAxisIndex: -1,
        header: null,
        cellSelected: function(row, col) {
            this.startRow = row;
            if (this.jq("params-xaxis-select").attr("checked")) {
                this.xAxisIndex = col;
            } else if (this.jq("params-group-select").attr("checked")) {
                this.groupIndex = col;
            } else {
                this.yAxisIndex = col;
            }
            var label = "";
            var p1 = "";
            var p2 = "";

            this.setAxisLabel(COORD_X, this.getHeading(this.xAxisIndex, true));
            this.setAxisLabel(COORD_GROUP, this.getHeading(this.groupIndex, true));
            this.setAxisLabel(COORD_Y, this.getHeading(this.yAxisIndex, true));
        },
        getAxisLabelId: function(root) {
            return "params-" + root + "-label"
        },
        setAxisLabel: function(fieldId, lbl) {
            fieldId = this.getAxisLabelId(fieldId);
            var id = HU.getUniqueId();
            if (lbl.length > 25) {
                lbl = lbl.substring(0, 25) + "...";
            }
            if (lbl.trim() != "") {
                lbl = HU.span([ATTR_ID, id,
			       ATTR_CLASS, "ramadda-tag-box"],
			      SPACE2 + lbl + SPACE2);
            }
            this.jq(fieldId).html(lbl);
        },
        loadSheet: function(sheetIdx) {

            var all = $("[id^=" + this.getDomId("sheet_") + "]");
            var sel = $("#" + this.getDomId("sheet_") + sheetIdx);

            all.css(CSS_FONT_WEIGHT, 'normal');
            sel.css(CSS_FONT_WEIGHT, FONT_BOLD);
            all.css(CSS_BORDER, HU.border(1,COLOR_LIGHT_GRAY));
            sel.css(CSS_BORDER, HU.border(1,'#666'));

            this.currentSheet = sheetIdx;
            var sheet = this.sheets[sheetIdx];
	    let rows;
            if (sheet) {
                rows = sheet.rows.slice(0);
                if (rows.length > 0) {
                    this.header = rows[0];
                }
            }

	    if(!rows) {
		this.displayHtml(this.getMessage("No data"));
		return;
	    }
            var html = "";
            var _this = this;
            var args = {
                contextMenu: true,
                stretchH: 'all',
                colHeaders: true,
                rowHeaders: true,
                minSpareRows: 1,
                afterSelection: function() {
                    if (arguments.length > 2) {
                        for (var i = 0; i < arguments.length; i++) {
                            //                                console.log("a[" + i +"]=" + arguments[i]);
                        }
                        var row = arguments[0];
                        var col = arguments[1];
                        _this.cellSelected(row, col);
                    }
                },
            };
            $.extend(args, this.tableProps);
            if (this.tableProps.useFirstRowAsHeader) {
                var headers = rows[0];
                args.colHeaders = headers;
                rows = rows.splice(1);
            }
            for (var i = 0; i < this.tableProps.skipRows; i++) {
                rows = rows.splice(1);
            }

            if (rows.length == 0) {
                this.displayMessage("No data found");
                this.jq(ID_RESULTS).html("");
                return;
            }

            this.jq(ID_RESULTS).html("Found: " + rows.length);
            args.data = rows;
            this.currentData = rows;

            if (this.tableProps.headers != null) {
                args.colHeaders = this.tableProps.headers;
            }

            if (this.getProperty("showTable", true)) {
                this.jq(ID_TABLE).handsontable(args);
            }

        },
        getDataForSheet: function(sheetIdx, args) {
            var sheet = this.sheets[sheetIdx];
            var rows = sheet.rows.slice(0);
            if (rows.length > 0) {
                this.header = rows[0];
            }

            if (this.tableProps.useFirstRowAsHeader) {
                var headers = rows[0];
                if (args) {
                    args.colHeaders = headers;
                }
                rows = rows.splice(1);
            }
            for (var i = 0; i < this.tableProps.skipRows; i++) {
                rows = rows.splice(1);
            }
            return rows;
        },

        makeChart: function(chartType, props) {
            if (typeof google == 'undefined') {
                this.jq(ID_CHART).html("No google chart available");
                return;
            }

            if (props == null) props = {};
            var xAxisIndex = Utils.getDefined(props.xAxisIndex, this.xAxisIndex);
            var groupIndex = Utils.getDefined(props.groupIndex, this.groupIndex);
            var yAxisIndex = Utils.getDefined(props.yAxisIndex, this.yAxisIndex);

            //                console.log("y:" + yAxisIndex +" props:" + props.yAxisIndex);

            if (yAxisIndex < 0) {
                alert("You must select a y-axis field.\n\nSelect the desired axis with the radio button.\n\nClick the column in the table to chart.");
                return;
            }

            var sheetIdx = this.currentSheet;
            if (!(typeof props.sheet == "undefined")) {
                sheetIdx = props.sheet;
            }

            var rows = this.getDataForSheet(sheetIdx);
            if (rows == null) {
                this.jq(ID_CHART).html("There is no data");
                return;
            }


            //remove the first header row
            var rows = rows.slice(1);

            for (var i = 0; i < this.startRow - 1; i++) {
                rows = rows.slice(1);
            }

            var subset = [];
            console.log("x:" + xAxisIndex + " " + " y:" + yAxisIndex + " group:" + groupIndex);
            for (var rowIdx = 0; rowIdx < rows.length; rowIdx++) {
                var row = [];
                var idx = 0;
                if (xAxisIndex >= 0) {
                    row.push(rows[rowIdx][xAxisIndex]);
                } else {
                    row.push(rowIdx);
                }
                if (yAxisIndex >= 0) {
                    row.push(rows[rowIdx][yAxisIndex]);
                }
                subset.push(row);
                if (rowIdx < 2)
                    console.log("row:" + row);
            }
            rows = subset;

            for (var rowIdx = 0; rowIdx < rows.length; rowIdx++) {
                var cols = rows[rowIdx];


                for (var colIdx = 0; colIdx < cols.length; colIdx++) {
                    var value = cols[colIdx] + "";
                    cols[colIdx] = parseFloat(value.trim());
                }
            }


            var lbl1 = this.getHeading(xAxisIndex, true);
            var lbl2 = this.getHeading(yAxisIndex, true);
            var lbl3 = this.getHeading(groupIndex, true);
            this.columnLabels = [lbl1, lbl2];


            var labels = this.columnLabels != null ? this.columnLabels : ["Field 1", "Field 2"];
            rows.splice(0, 0, labels);
            /*
              for(var rowIdx=0;rowIdx<rows.length;rowIdx++) {
              var cols = rows[rowIdx];
              var s = "";
              for(var colIdx=0;colIdx<cols.length;colIdx++) {
              if(colIdx>0)
              s += ", ";
              s += "'" +cols[colIdx]+"'" + " (" + (typeof cols[colIdx]) +")";
              }
              console.log(s);
              if(rowIdx>5) break;
              }
            */

            var dataTable = google.visualization.arrayToDataTable(rows);
            var chartOptions = {};
            var width = "95%";
            $.extend(chartOptions, {
                legend: {
                    position: 'top'
                },
            });

            if (this.header != null) {
                if (xAxisIndex >= 0) {
                    chartOptions.hAxis = {
                        title: this.header[xAxisIndex]
                    };
                }
                if (yAxisIndex >= 0) {
                    chartOptions.vAxis = {
                        title: this.header[yAxisIndex]
                    };
                }
            }

            var chartDivId = HU.getUniqueId();
            var divAttrs = [ATTR_ID, chartDivId];
            if (chartType == "scatterplot") {
                divAttrs.push(ATTR_STYLE);
                divAttrs.push(HU.css(CSS_WIDTH,HU.px(450),CSS_HEIGHT,HU.px(450)));
            }
            this.jq(ID_CHART).append(HU.div(divAttrs));

            if (chartType == "barchart") {
                chartOptions.chartArea = {
                    left: 75,
                    top: 10,
                    height: "60%",
                    width: width
                };
                chartOptions.orientation = "horizontal";
                this.chart = new google.visualization.BarChart(document.getElementById(chartDivId));
            } else if (chartType == "table") {
                this.chart = new google.visualization.Table(document.getElementById(chartDivId));
            } else if (chartType == "motion") {
                this.chart = new google.visualization.MotionChart(document.getElementById(chartDivId));
            } else if (chartType == "scatterplot") {
                chartOptions.chartArea = {
                    left: 50,
                    top: 30,
                    height: 400,
                    width: 400
                };
                chartOptions.legend = 'none';
                chartOptions.axisTitlesPosition = "in";
                this.chart = new google.visualization.ScatterChart(document.getElementById(chartDivId));
            } else {
                $.extend(chartOptions, {
                    lineWidth: 1
                });
                chartOptions.chartArea = {
                    left: 75,
                    top: 10,
                    height: "60%",
                    width: width
                };
                this.chart = new google.visualization.LineChart(document.getElementById(chartDivId));
            }
            if (this.chart != null) {
                this.chart.draw(dataTable, chartOptions);
            }
        },

        addNewChartListener: function(makeChartId, chartType) {
            var _this = this;
            $("#" + makeChartId + "-" + chartType).button().click(function(event) {
                console.log("make chart:" + chartType);
                _this.makeChart(chartType);
            });
        },

        makeSheetButton: function(id, index) {
            var _this = this;
            $("#" + id).button().click(function(event) {
                _this.loadSheet(index);
            });
        },
        clear: function() {
            this.jq(ID_CHART).html("");
            this.startRow = 0;
            this.groupIndex = -1;
            this.xAxisIndex = -1;
            this.yAxisIndex = -1;
            this.setAxisLabel(COORD_GROUP, "");
            this.setAxisLabel(COORD_X, "");
            this.setAxisLabel(COORD_Y, "");
        },
        getHeading: function(index, doField) {
            if (index < 0) return "";
            if (this.header != null && index >= 0 && index < this.header.length) {
                var v = this.header[index];
                v = v.trim();
                if (v.length > 0) return v;
            }
            if (doField)
                return "Field " + (index + 1);
            return "";
        },
        showTableData: function(data) {
            var _this = this;

            this.data = data;
            this.sheets = this.data.sheets;
            this.columns = data.columns;



            var buttons = "";
            for (var sheetIdx = 0; sheetIdx < this.sheets.length; sheetIdx++) {
                var id = this.getDomId("sheet_" + sheetIdx);
                buttons += HU.div([ATTR_ID, id,
				   ATTR_CLASS, "ramadda-xls-button-sheet"],
				  this.sheets[sheetIdx].name);

                buttons += "\n";
            }

            var weight = "12";

            var tableHtml = "<table width=100% style=\"max-width:1000px;\" > ";
            if (this.sheets.length > 1) {
                weight = "10";
            }

            tableHtml += "<tr valign=top>";

            if (this.sheets.length > 1) {
                //                    tableHtml += HU.openTag([ATTR_CLASS,"col-md-2"]);
                tableHtml += HU.td(["width", "140"], HU.div([ATTR_CLASS, "ramadda-xls-buttons"], buttons));
                weight = "10";
            }


            var makeChartId = HU.getUniqueId();

            var tableWidth = this.getProperty("tableWidth", "");
            var tableHeight = this.getProperty("tableHeight", "500px");

            var style = "";
            if (tableWidth != "") {
                style += HU.css(CSS_WIDTH,tableWidth);
            }
            style += HU.css(CSS_HEIGHT,tableHeight);
            style += HU.css(CSS_OVERFLOW,OVERFLOW_AUTO);
            tableHtml += HU.td([], HU.div([ATTR_ID, this.getDomId(ID_TABLE),
					   ATTR_CLASS, "ramadda-xls-table",
					   ATTR_STYLE, style]));


            tableHtml += HU.close(TAG_TR,TAG_TABLE);

            var chartToolbar = "";
            var chartTypes = ["barchart", "linechart", "scatterplot"];
            for (var i = 0; i < chartTypes.length; i++) {
                chartToolbar += HU.div([ATTR_ID, makeChartId + "-" + chartTypes[i],
					ATTR_CLASS, "ramadda-xls-button"], "Make " + chartTypes[i]);
                chartToolbar += SPACE;
            }

            chartToolbar += SPACE;
            chartToolbar += HU.div([ATTR_ID, this.getDomId("removechart"),
				    ATTR_CLASS, "ramadda-xls-button"], "Clear Charts");


            chartToolbar += HU.p();
            chartToolbar += HU.open(TAG_FORM);
            chartToolbar += "Fields: ";
            chartToolbar += "<input type=radio checked name=\"param\" id=\"" + this.getDomId("params-yaxis-select") + "\"> y-axis:&nbsp;" +
                HU.div([ATTR_ID, this.getDomId("params-yaxis-label"),
			ATTR_STYLE, HU.css(CSS_BORDER_BOTTOM,HU.border(1,COLOR_LIGHT_GRAY,'dotted'),
					   CSS_MIN_WIDTH,HU.em(10),
					   CSS_DISPLAY,DISPLAY_INLINE_BLOCK)], "");

            chartToolbar += SPACE3;
            chartToolbar += "<input type=radio  name=\"param\" id=\"" + this.getDomId("params-xaxis-select") + "\"> x-axis:&nbsp;" +
                HU.div([ATTR_ID, this.getDomId("params-xaxis-label"),
			ATTR_STYLE, HU.css(CSS_BORDER_BOTTOM,HU.border(1,COLOR_LIGHT_GRAY,'dotted'),
					   CSS_MIN_WIDTH,HU.em(10),
					   CSS_DISPLAY,DISPLAY_INLINE_BLOCK)], "");

            chartToolbar += SPACE3;
            chartToolbar += "<input type=radio  name=\"param\" id=\"" + this.getDomId("params-group-select") + "\"> group:&nbsp;" +
                HU.div([ATTR_ID, this.getDomId("params-group-label"),
			ATTR_STYLE, HU.css(CSS_BORDER_BOTTOM,HU.border(1,COLOR_LIGHT_GRAY,'dotted'),
					   CSS_MIN_WIDTH,HU.em(10),
					   CSS_DISPLAY,DISPLAY_INLINE_BLOCK)], "");

            chartToolbar += HU.close(TAG_FORM);

            if (this.getProperty("showSearch", true)) {
                var results = HU.div([ATTR_STYLE, HU.css(CSS_DISPLAY,DISPLAY_INLINE_BLOCK),
				      ATTR_ID, this.getDomId(ID_RESULTS)], "");
                var download = HU.div([ATTR_STYLE, HU.css(CSS_DISPLAY,DISPLAY_INLINE_BLOCK),
				       ATTR_ID, this.getDomId(ID_DOWNLOADURL)]);
                var searchDiv = HU.div([ATTR_ID, this.getDomId(ID_SEARCH_DIV), ATTR_CLASS, "ramadda-xls-search-form"]);


                var search = "";
                search += HU.openTag(TAG_FORM, ["action", "#", ATTR_ID, this.getDomId(ID_SEARCH_FORM)]);
                search += HU.image(icon_tree_closed, [ATTR_ID, this.getDomId(ID_SEARCH + "_open")]);
                search += "\n";
                search += HU.input(ID_SEARCH_TEXT, this.jq(ID_SEARCH_TEXT).val(), ["size", "60", ATTR_ID, this.getDomId(ID_SEARCH_TEXT), ATTR_PLACEHOLDER, "Search"]);
                search += "<input type=submit name='' style='display:none;'>";

                search += HU.openTag(TAG_DIV, [ATTR_ID, this.getDomId(ID_SEARCH_EXTRA), ATTR_CLASS, "ramadda-xls-search-extra"], "");
                if (this.columns) {
                    var extra = HU.openTag(TAG_TABLE, [ATTR_CLASS, CLASS_FORMTABLE]);
                    for (var i = 0; i < this.columns.length; i++) {
                        var col = this.columns[i];
                        var id = ID_SEARCH_PREFIX + "_" + col.name;
                        var widget = HU.input(id, this.jq(id).val(), [ATTR_ID, this.getDomId(id), ATTR_PLACEHOLDER, "Search"]);
                        extra += HU.formEntry(col.name.replace("_", " ") + ":", widget);
                    }
                    extra += HU.closeTag(TAG_TABLE);
                    search += extra;
                }


                if (this.searchFields) {
                    var extra = HU.openTag(TAG_TABLE, [ATTR_CLASS, CLASS_FORMTABLE]);
                    for (var i = 0; i < this.searchFields.length; i++) {
                        var col = this.searchFields[i];
                        var id = ID_SEARCH_PREFIX + "_" + col.name;
                        var widget = HU.input(id, this.jq(id).val(), [ATTR_ID, this.getDomId(id), ATTR_PLACEHOLDER, "Search"]);
                        extra += HU.formEntry(col.label + ":", widget);
                    }
                    extra += HU.closeTag(TAG_TABLE);
                    search += extra;
                }
                search += HU.closeTag(TAG_DIV);
                search += HU.closeTag(TAG_FORM);

                this.jq(ID_SEARCH_HEADER).html(HU.leftRight(searchDiv, results + " " + download));

                this.jq(ID_SEARCH_DIV).html(search);

                if (!this.extraOpen) {
                    this.jq(ID_SEARCH_EXTRA).hide();
                }


                this.jq(ID_SEARCH + "_open").button().click(function(event) {
                    _this.jq(ID_SEARCH_EXTRA).toggle();
                    _this.extraOpen = !_this.extraOpen;
                    if (_this.extraOpen) {
                        _this.jq(ID_SEARCH + "_open").attr(ATTR_SRC, icon_tree_open);
                    } else {
                        _this.jq(ID_SEARCH + "_open").attr(ATTR_SRC, icon_tree_closed);
                    }
                });

            }


            if (this.getProperty("showTable", true)) {
                this.jq(ID_TABLE_HOLDER).html(tableHtml);
                chartToolbar += HU.br();
                if (this.getProperty("showChart", true)) {
                    this.jq(ID_CHARTTOOLBAR).html(chartToolbar);
                }
            }

            if (this.getProperty("showSearch", true)) {
                this.jq(ID_SEARCH_FORM).submit(function(event) {
                    event.preventDefault();
                    _this.loadTableData(_this.url, "Searching...");
                });
                this.jq(ID_SEARCH_TEXT).focus();
                this.jq(ID_SEARCH_TEXT).select();
            }


            for (var i = 0; i < chartTypes.length; i++) {
                this.addNewChartListener(makeChartId, chartTypes[i]);
            }
            this.jq("removechart").button().click(function(event) {
                _this.clear();
            });

            for (var sheetIdx = 0; sheetIdx < this.sheets.length; sheetIdx++) {
                var id = this.getDomId("sheet_" + sheetIdx);
                this.makeSheetButton(id, sheetIdx);
            }
            var sheetIdx = 0;
            var rx = /sheet=([^&]+)/g;
            var arr = rx.exec(window.location.search);
            if (arr) {
                sheetIdx = arr[1];
            }
            this.loadSheet(sheetIdx);


            if (this.defaultCharts) {
                for (var i = 0; i < this.defaultCharts.length; i++) {
                    var dflt = this.defaultCharts[i];
                    this.makeChart(dflt.type, dflt);
                }
            }
            this.setAxisLabel("params-yaxis-label", this.getHeading(this.yAxisIndex, true));

            this.displayDownloadUrl();

        },
        displayMessage: function(msg, icon) {
            if (!icon) {
                icon = icon_information;
            }
            var html = HU.hbox([HU.image(icon, [ATTR_ALIGN, "left"]),
				HU.inset(msg, 10, 10, 5, 10)]);
            html = HU.div([ATTR_CLASS, "note"], html);
            this.jq(ID_TABLE_HOLDER).html(html);
        },
        displayDownloadUrl: function() {
            var url = this.lastUrl;
            if (url == null) {
                this.jq(ID_DOWNLOADURL).html("");
                return
            }
            url = url.replace("xls_json", "media_tabular_extractsheet");
            url = HU.url(url,["execute","true"]);
            let img = HU.span([ATTR_TITLE,'Download'],HU.getIconImage("fa-download"));
            this.jq(ID_DOWNLOADURL).html(HU.href(url, img));
        },
        loadTableData: function(url, message) {
            this.url = url;
            if (!message) message = this.getLoadingMessage();
            this.displayMessage(message, icon_progress);
            var _this = this;

            var text = this.jq(ID_SEARCH_TEXT).val();
            if (text && text != "") {
                url = HU.url(url,["table.text",text]);
            }
            if (this.columns) {
                for (var i = 0; i < this.columns.length; i++) {
                    var col = this.columns[i];
                    var id = ID_SEARCH_PREFIX + "_" + col.name;
                    var text = this.jq(id).val();
                    if (text) {
                        url = url + "&table." + col.name + "=" + encodeURIComponent(text);
                    }
                }
            }

            if (this.searchFields) {
                for (var i = 0; i < this.searchFields.length; i++) {
                    var col = this.searchFields[i];
                    var id = ID_SEARCH_PREFIX + "_" + col.name;
                    var text = this.jq(id).val();
                    if (text) {
                        url = url + "&table." + col.name + "=" + encodeURIComponent(text);
                    }
                }
            }

            this.lastUrl = url;

	    let  load = async function() {
		if(!this.loadedJS) {
		    await Utils.importJS(ramaddaBaseHtdocs + "/lib/jquery.handsontable.full.min.js");
		    await Utils.importCSS(ramaddaBaseHtdocs + "/lib/jquery.handsontable.full.min.css");
		    this.loadedJS = true;
		}
		var jqxhr = $.getJSON(url, function(data) {
                    if (GuiUtils.isJsonError(data)) {
                        _this.displayMessage("Error: " + data.error);
                        return;
                    }
		    try {
			_this.showTableData(data);

		    } catch(exc) {
                        _this.displayMessage("Error: " + exc);
			this.handleError("Error:" + exc, exc);
		    }
                })
                    .fail(function(jqxhr, textStatus, error) {
			var err = textStatus + ", " + error;
			_this.displayMessage("An error occurred: " + error);
			console.log("JSON error:" + err);
                    });
	    }
	    load();
	}
    });

}

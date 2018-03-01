/**
Copyright 2008-2018 Geode Systems LLC
*/

var CHARTS_CATEGORY = "Charts";
var DISPLAY_LINECHART = "linechart";
var DISPLAY_BARCHART = "barchart";
var DISPLAY_BARTABLE = "bartable";
var DISPLAY_BARSTACK = "barstack";
var DISPLAY_SCATTERPLOT = "scatterplot";
var DISPLAY_STATS = "stats";
var DISPLAY_PIECHART = "piechart";
var DISPLAY_TABLE = "table";
var DISPLAY_TEXT = "text";
var DISPLAY_CROSSTAB = "crosstab";
var DISPLAY_CORRELATION = "correlation";

var googleChartsLoaded = false;
function googleChartsHaveLoaded () {
    googleChartsLoaded= true;
}
google.charts.setOnLoadCallback(googleChartsHaveLoaded);

function haveGoogleChartsLoaded () {
    if(!googleChartsLoaded) {
        if (typeof google.visualization !== "undefined") { 
            if (typeof google.visualization.BarChart !== "undefined") { 
                googleChartsLoaded = true;
            }
        }
    }
    return googleChartsLoaded;
}


addGlobalDisplayType({type: DISPLAY_LINECHART, label:"Line Chart",requiresData:true,forUser:true,category:CHARTS_CATEGORY});
addGlobalDisplayType({type:DISPLAY_BARCHART,label: "Bar Chart",requiresData:true,forUser:true,category:CHARTS_CATEGORY});
addGlobalDisplayType({type:DISPLAY_BARSTACK,label: "Stacked Bar Chart",requiresData:true,forUser:true,category:CHARTS_CATEGORY});
addGlobalDisplayType({type:DISPLAY_BARTABLE,label: "Bar Table",requiresData:true,forUser:true,category:CHARTS_CATEGORY});

addGlobalDisplayType({type:DISPLAY_SCATTERPLOT,label: "Scatter Plot",requiresData:true,forUser:true,category:CHARTS_CATEGORY});
addGlobalDisplayType({type:DISPLAY_STATS , label: "Stats Table",requiresData:false,forUser:true,category:CHARTS_CATEGORY});


addGlobalDisplayType({type:DISPLAY_PIECHART,label: "Pie Chart",requiresData:true,forUser:true,category:CHARTS_CATEGORY});
addGlobalDisplayType({type:DISPLAY_TABLE , label: "Table",requiresData:true,forUser:true,category:CHARTS_CATEGORY});
addGlobalDisplayType({type:DISPLAY_TEXT , label: "Text Readout",requiresData:false,forUser:true,category:CHARTS_CATEGORY});

addGlobalDisplayType({type:DISPLAY_CORRELATION , label: "Correlation",requiresData:true,forUser:true,category:CHARTS_CATEGORY});




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
            initDisplay:function() {
                this.initUI();
                this.updateUI();
            },
            updateUI: function() {
                this.addFieldsCheckboxes();
            },
            getWikiAttributes: function(attrs) {
                SUPER.getWikiAttributes.call(this, attrs);
                if(this.lastSelectedFields) {
                    attrs.push("fields");
                    var v = "";
                    for(var i=0;i<this.lastSelectedFields.length;i++) {
                        v +=this.lastSelectedFields[i].getId();
                        v+= ",";
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
                var html  =  HtmlUtil.div([ATTR_ID,  this.getDomId(ID_FIELDS),"style","overflow-y: auto;    max-height:" + height +"px;"]," FIELDS ");
                tabTitles.push("Fields"); 
                tabContents.push(html);
                SUPER.getDialogContents.call(this,tabTitles, tabContents);
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
        })}
        


        



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
    //A hack  so charts are displayed OK in a tabs or accordian
    //When the doc is done wait 5 seconds then display (or re-display) the data
    $(document).ready(function(){
            var cb = function() {
                _this.displayData();
            };
            //            setTimeout(cb,8000);
        });


    //Init the defaults first
    $.extend(this, {
            indexField: -1,
            colors: ['blue', 'red', 'green', 'orange','fuchsia','teal','navy','silver'],
            curveType: 'none',
            fontSize: 0,
            vAxisMinValue:NaN,
            vAxisMaxValue:NaN,
            showTrendlines: false,
            showPercent: false,
                percentFields: null
           });

    if(properties.colors) {
        this.colors = (""+properties.colors).split(",");
    }
    var SUPER;
    RamaddaUtil.inherit(this, SUPER = new RamaddaFieldsDisplay(displayManager, id, properties.chartType, properties));

    RamaddaUtil.defineMembers(this, {
            getType: function () {
                return this.getProperty(PROP_CHART_TYPE,DISPLAY_LINECHART);
            },
            initDisplay:function() {
                this.initUI();
                this.updateUI();
            },
            updateUI: function() {
                SUPER.updateUI.call(this);
                this.displayData();
            },
            getWikiAttributes: function(attrs) {
                this.defineWikiAttributes(["vAxisMinValue","vAxisMaxValue"]);
                SUPER.getWikiAttributes.call(this, attrs);
                if(this.colors.join(",") != "blue,red,green") {
                    attrs.push("colors");
                    attrs.push(this.colors.join(", "));
                }
            },
               
           initDialog: function() {
                SUPER.initDialog.call(this);
                var _this  = this;
                var updateFunc  = function() {
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
                        if(e.which != 13) {
                            return;
                        }
                        var v = _this.jq(ID_COLORS).val();
                        _this.colors = v.split(",");
                        _this.displayData();
                        var pointData =   _this.dataCollection.getList();
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
                var v = prompt("Enter comma separated list of colors to use", this.colors.join(","));
                if(v!=null) {
                    this.colors = v.split(",");
                    this.displayData();
                    var pointData =   this.dataCollection.getList();
                    this.getDisplayManager().handleEventPointDataLoaded(this, this.lastPointData);
                }
            },
                            getMenuItems: function(menuItems) {
                SUPER.getMenuItems.call(this,menuItems);
                var get = this.getGet();
                //                menuItems.push(HtmlUtil.onClick(get+".setColor();", "Set color"));

                var min = "0";
                if(!isNaN(this.vAxisMinValue)) {
                    min = "" +this.vAxisMinValue;
                }
                var max = "";
                if(!isNaN(this.vAxisMaxValue)) {
                    max = "" +this.vAxisMaxValue;
                }
               var tmp = HtmlUtil.formTable();
                tmp += HtmlUtil.formEntry("Axis Range:", HtmlUtil.input("", min, ["size","7",ATTR_ID,  this.getDomId("vaxismin")]) + " - " +
                                          HtmlUtil.input("", max, ["size","7",ATTR_ID,  this.getDomId("vaxismax")]));
                tmp += HtmlUtil.formEntry("Date Range:", HtmlUtil.input("", this.minDate, ["size","10",ATTR_ID,  this.getDomId("mindate")]) + " - " +
                                          HtmlUtil.input("", this.maxDate, ["size","10",ATTR_ID,  this.getDomId("maxdate")]));


                tmp += HtmlUtil.formEntry("Colors:",
                                          HtmlUtil.input("", this.colors.join(","), ["size","35",ATTR_ID,  this.getDomId(ID_COLORS)]));
                tmp += "</table>";
                menuItems.push(tmp);

            },
getChartType: function() {
                return this.getProperty(PROP_CHART_TYPE,DISPLAY_LINECHART);
            },
            defaultSelectedToAll: function() {
                if(this.chartType == DISPLAY_TABLE) {
                    return true;
                }
                return SUPER.defaultSelectedToAll.call(this);
            },
            askMinZAxis: function() {
                this.setMinZAxis(prompt("Minimum axis value", "0"));
            },
            setMinZAxis: function(v) {
                if(v!=null) {
                    this.vAxisMinValue = parseFloat(v);
                    this.displayData();
                }
            },
            askMaxZAxis: function() {
                this.setMaxZAxis(prompt("Maximum axis value", "100"));
            },
            setMaxZAxis: function(v) {
                if(v!=null) {
                    this.vAxisMaxValue = parseFloat(v);
                    this.displayData();
                }
            },
            askMinDate: function() {
                var ex = this.minDate;
                if(ex == null || ex == "") {
                    ex = "1800-01-01";
                }
                var v = prompt("Minimum date", ex);
                if(v == null) return;
                this.setMinDate(v);
            },
               setMinDate: function(v) {
                this.minDate =v;
                this.displayData();
            },
            askMaxDate: function() {
                var ex = this.maxDate;
                if(ex == null || ex == "") {
                    ex = "2100-01-01";
                } 
                var v = prompt("Maximum date", ex);
                if(v == null) return;
                this.setMaxDate(v);
            },
            setMaxDate: function(v) {
                this.maxDate =v;
                this.displayData();
            },
           getDialogContents: function(tabTitles, tabContents) {
                var height = "600";
                var html  =  HtmlUtil.div([ATTR_ID,  this.getDomId(ID_FIELDS),"style","overflow-y: auto;    max-height:" + height +"px;"]," FIELDS ");

                html += HtmlUtil.div([ATTR_CLASS,"display-dialog-subheader"],  "Other");

                html += HtmlUtil.checkbox(this.getDomId(ID_TRENDS_CBX),
                                          [],
                                          this.showTrendLines) +"  "  + "Show trend line";
                html += " ";
                html += HtmlUtil.checkbox(this.getDomId(ID_PERCENT_CBX),
                                          [],
                                          this.showPercent) +"  "  + "Show percent of displayed total" + "<br>";
                html +=  "<br>";

                tabTitles.push("Fields"); 
                tabContents.push(html);
                SUPER.RamaddaDisplay.getDialogContents.call(this,tabTitles, tabContents);
            },
            handleEventMapClick: function (source,args) {
                var pointData =   this.dataCollection.getList();
                for(var i=0;i<pointData.length;i++) {
                    pointData[i].handleEventMapClick(this, source, args.lon,args.lat);
                }
            },
            handleEventRecordSelection: function(source, args) {
                //TODO: don't do this in index space, do it in time or space space
                if(source==this) {
                    return;
                }
                this.setChartSelection(args.index);
            },
            getFieldsToSelect: function(pointData) {
                var chartType = this.getProperty(PROP_CHART_TYPE,DISPLAY_LINECHART);
                if(this.chartType == DISPLAY_TABLE || this.chartType == DISPLAY_BARTABLE) {
                    //                    return pointData.getRecordFields();
                    return pointData.getNonGeoFields();
                } 
                return  pointData.getChartableFields();
            },
            canDoGroupBy: function() {
                return this.chartType == DISPLAY_PIECHART || this.chartType == DISPLAY_TABLE;
            },
                xcnt:0,
            displayData: function() {
                if(this.inError) {
                    return;
                }
                if(!haveGoogleChartsLoaded ()) {
                    //console.log("charts not loaded");
                    var _this =this;
                    var func = function() {
                        _this.displayData();
                    }
                    //                    this.xcnt++;
                    //                    this.setContents(this.getLoadingMessage() + " waiting on charts " + this.xcnt);
                    this.setContents(this.getLoadingMessage());
                    setTimeout(func,1000);
                    return;
                }


                if(this.inError) {
                    return;
                }
                if(!this.hasData()) {
                    this.clearChart();
                    this.setContents(this.getLoadingMessage());
                    return;
                }
                this.setContents(HtmlUtil.div([ATTR_CLASS,"display-message"],
                                              "Building display..."));

                this.allFields =  this.dataCollection.getList()[0].getRecordFields();


                var chartType = this.getProperty(PROP_CHART_TYPE,DISPLAY_LINECHART);
                var selectedFields = this.getSelectedFields([]);
                //                console.log(chartType +"  selectedFields 1:" + selectedFields +" "+ (selectedFields!=null?selectedFields.length:"null"));

                if(selectedFields.length==0 && this.lastSelectedFields!=null) { 
                    selectedFields = this.lastSelectedFields;
                }

                if(selectedFields == null || selectedFields.length == 0) {
                    if(this.chartType == DISPLAY_TABLE) {
                        //                        selectedFields = this.allFields;
                        selectedFields = this.dataCollection.getList()[0].getNonGeoFields();
                    } else {
                        selectedFields = this.getSelectedFields();
                    }
                }
                        
                if(selectedFields.length==0) {
                    this.setContents("No fields selected");
                    return;
                }

                //Check for the skip
                var tmpFields = [];
                for(var i=0;i<selectedFields.length;i++) {
                    if(!this.shouldSkipField(selectedFields[i])) {
                        tmpFields.push(selectedFields[i]);
                    }
                }
                selectedFields = tmpFields;

                this.lastSelectedFields= selectedFields;

                var props = {
                    includeIndex: true,
                };
                if(this.chartType == DISPLAY_TABLE || this.chartType == DISPLAY_BARTABLE || chartType == DISPLAY_PIECHART || chartType == DISPLAY_SCATTERPLOT) {
                    props.includeIndex = false;
                }
                props.groupByIndex = -1;

                if(chartType == DISPLAY_PIECHART) {
                    if(!this.groupBy && this.groupBy!="") {
                        for(var i=0;i<this.allFields.length;i++) {
                            var field = this.allFields[i];
                            if(field.getType() == "string") {
                                this.groupBy = field.getId();
                                break;
                            }
                        }
                    }

                }


                if(this.groupBy) {
                    for(var i=0;i<this.allFields.length;i++) {
                        var field = this.allFields[i];
                        if(field.getId() == this.groupBy) {
                            props.groupByIndex = field.getIndex();
                            props.groupByField = field;
                            break;
                        }
                    }
                }

                var fieldsToSelect =selectedFields;
                if(this.raw) {
                    fieldsToSelect  = this.dataCollection.getList()[0].getRecordFields();
                    props.raw = true;
                }
                


                if(chartType == DISPLAY_BARTABLE) {
                    props.includeIndexIfDate =  true;
                }


                var dataHasIndex = props.includeIndex;
                var dataList = this.getStandardData(fieldsToSelect, props);

                

                if(this.rotateTable && dataList.length) {
                    var header = dataList[0];
                    var flipped = [];
                    for(var rowIdx=1;rowIdx<dataList.length;rowIdx++) {
                        var row = dataList[rowIdx];
                    }
                }


                //                console.log("fields:" + selectedFields +" data.length = " + dataList.length +" " + dataList);

                if(dataList.length==0 && !this.userHasSelectedAField) {
                    var pointData =   this.dataCollection.getList()[0];
                    var chartableFields =this.getFieldsToSelect(pointData);
                    for(var i=0;i<chartableFields.length;i++) {
                        var field = chartableFields[i];
                        dataList = this.getStandardData([field], props);
                        if(dataList.length>0) {
                            this.setSelectedFields([field]);
                            //                            console.log("Defaulting to field:" + field.getId());
                            break;
                        }
                    }
                }


                if(dataList.length==0) {
                    this.setContents(HtmlUtil.div([ATTR_CLASS,"display-message"],
                                                  "No data available"));
                    return;
                }


                if(this.showPercent) {
                    var newList = [];
                    var isNumber = [];
                    var isOk = [];
                    var headerRow = null;
                    var fields = null;
                    if(this.percentFields!=null) {
                        fields = this.percentFields.split(",");
                    }
                    for(var i=0;i<dataList.length;i++)  {
                        var row = dataList[i];
                        if(i == 0) {
                            headerRow = row;
                            continue;
                        }
                        if(i == 1) {
                            var seenIndex = false;
                            for(var j=0;j<row.length;j++)  {
                                var valueIsNumber = (typeof row[j] == "number");
                                var valueIsDate = (typeof row[j] == "object");
                                if(valueIsNumber) {
                                    if(dataHasIndex && !seenIndex) {
                                        valueIsNumber = false;
                                        seenIndex = true;
                                    }
                                } 
                                if(valueIsDate) {
                                    seenIndex = true;
                                }

                                if(valueIsNumber && fields!=null) {
                                    valueIsNumber =  fields.indexOf(fieldsToSelect[j].getId())>=0 ||
                                        fields.indexOf("#"+(j+1))>=0;
                                } 
                                //xb                                console.log("fields:" + fields.length +" j:" + j +" id:" +fieldsToSelect[j].getId() +" is:" + valueIsNumber);

                                isNumber.push(valueIsNumber);
                            }
                            var newHeader = [];
                            for(var j=0;j<headerRow.length;j++)  {
                                var v = headerRow[j];
                                if(!isNumber[j]) {
                                    newHeader.push(v);
                                } else {
                                    newHeader.push("% "  + v);
                                }
                            }
                            //                            console.log("header:"  + newHeader)
                            newList.push(newHeader);
                        }

                        var total  = 0;
                        var cnt = 0;
                        for(var j=0;j<row.length;j++)  {
                            if(isNumber[j]) {
                                total +=  parseFloat(row[j]);
                                cnt++;
                            }
                        }
                        var newRow = [];
                        for(var j=0;j<row.length;j++)  {
                            if(isNumber[j]) {
                                if(total!=0)  {
                                    var v = parseFloat(((row[j]/total)*100).toFixed(1));
                                    newRow.push(v);
                                }  else {
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

                this.makeChart(chartType, dataList, selectedFields);

                var d = this.jq(ID_CHART);
                if(d.width() == 0) {
                    if(!this.calledBackDisplay) this.calledBackDisplay = 0;
                    this.calledBackDisplay++;
                    if(this.calledBackDisplay<5) {
                        var _this = this;
                        var cb = function() {
                            _this.displayData();
                        };
                        setTimeout(cb,3000);
                    }
                }

            },
            printDataList:function(dataList) {
                console.log("data list:" + dataList.length);
                for(var i=0;i<dataList.length;i++)  {
                    var row = dataList[i];
                    var s = "";
                    for(var j=0;j<row.length;j++)  {
                        if(j>0) s+= ", ";
                        s +=  row[j];
                    }
                    console.log("row: " +i +"  " + s);
                }
            },
            clearChart: function() {
                if(this.chart !=null) {
                    this.chart.clearChart();
                }
            },
            setChartSelection: function(index) {
                if(this.chart!=null) {
                    this.chart.setSelection([{row:index, column:null}]); 
                    //                    var container = $('#table_div').find('.google-visualization-table-table:eq(0)').parent();
                    //                    var header = $('#table_div').find('.google-visualization-table-table:eq(1)').parent();
                    //                    var row = $('.google-visualization-table-tr-sel');
                    //                    $(container).prop('scrollTop', $(row).prop('offsetTop') - $(header).height());
                }
            },

            tableHeaderMouseover: function(i,tooltip) {
                //alert("new:" + tooltip);
            },
            makeGoogleChart: function(chartType, dataList, selectedFields) {
                if(typeof google == 'undefined') {
                    this.setContents("No google");
                    return;
                }

                if(chartType == DISPLAY_TABLE && dataList.length>0) {
                    var header = dataList[0];
                    /*
                    var get = this.getGet();
                    for(var i=0;i<header.length;i++) {
                        var s = header[i];
                        var tt = "tooltip";
                        s = HtmlUtil.tag("div",["onmouseover", get +".tableHeaderMouseover(" + i+",'" + tt +"');"],s);
                        header[i] = s;
                    }
                    */
                }

                var dataTable = google.visualization.arrayToDataTable(dataList);
                for(var i=1;i<dataList.length;i++) {
                    var row = dataList[i];
                    var s = "";
                    for(var j=0;j<row.length;j++) {
                        s  = s +" -  "  + row[j];
                    }
                }

                var   chartOptions = {};
                $.extend(chartOptions, {
                        lineWidth: 1,
                        colors: this.colors,
                        curveType:this.curveType,
                        vAxis: {}});


                if(this.lineWidth) {
                    chartOptions.lineWidth = this.lineWidth;
                }
                if(this.fontSize>0) {
                    chartOptions.fontSize = this.fontSize;
                }



                var defaultRange = this.getDisplayManager().getRange(selectedFields[0]);

                var range = [NaN,NaN];
                if(!isNaN(this.vAxisMinValue)) {
                    range[0] = parseFloat(this.vAxisMinValue);
                } else if(defaultRange!=null) {
                    range[0] = defaultRange[0];
                }
                if(!isNaN(this.vAxisMaxValue)) {
                    range[1] = parseFloat(this.vAxisMaxValue);
                } else if(defaultRange!=null) {
                    range[1] = defaultRange[1];
                }
                //console.log("range:" + range[0]+" " + range[1]);
                

                if(!isNaN(range[0])) {
                    chartOptions.vAxis.minValue =range[0];
                }
                if(!isNaN(range[1])) {
                    chartOptions.vAxis.maxValue =range[1];
                }
                var width = "90%";
                var left = "10%";
                useMultipleAxes = this.getProperty("useMultipleAxes",true);
                if(selectedFields.length>1 && useMultipleAxes) {
                    width = "80%";
                }
                var chartId = this.getDomId(ID_CHART);
                var divAttrs = [ATTR_ID, chartId];
                if(chartType == DISPLAY_PIECHART) {
                    divAttrs.push("style");
                    var style = "";
                    if(this.chartWidth) 
                        style += "width:" + this.chartWidth +";" ;                    
                    else 
                        style += "width:" + "600px;";
                    if(this.chartHeight) 
                        style += "height:" + this.chartHeight +";" ;                    
                    else 
                        style += "height:" + "400px;";
                    divAttrs.push(style);
                } else {
                    //                    divAttrs.push("style");
                    //                    divAttrs.push("width:600px;");
                }
                divAttrs.push("style");
                divAttrs.push("height:100%;");
                this.setContents(HtmlUtil.div(divAttrs,""));


                if(chartType == DISPLAY_LINECHART || chartType == DISPLAY_BARCHART ||
                   chartType == DISPLAY_BARSTACK ) {
                    chartOptions.height = this.getProperty("chartHeight",this.getProperty("height","150"));
                    $.extend(chartOptions, {
                            //series: [{targetAxisIndex:0},{targetAxisIndex:1},],
                            legend: { position: 'bottom' },
                                chartArea:{left:left,top:10,height:"70%",width:width,
                                },
                                //                                explorer: {
                                //                                actions: ['dragToPan', 'rightClickToReset'],
                                //                                axis: 'horizontal',
                                //                                keepInBounds: true
                                //                            }
                        });
                    if (useMultipleAxes) {
                        $.extend(chartOptions, {
                            series: [{targetAxisIndex:0},{targetAxisIndex:1}]
                            });
                    }

                    if(this.showTrendLines) {
                        chartOptions.trendlines = {
                            0: {
                                type: 'linear',
                                color: 'green',
                            }
                        };
                    }

                    if(this.hAxis) {
                       if (chartOptions.hAxis) {
                           chartOptions.hAxis.title = this.hAxis;
                       } else {
                           chartOptions.hAxis = {title: this.hAxis}
                       }
                    }
                    if(this.vAxis) {
                       if (chartOptions.vAxis) {
                           chartOptions.vAxis.title = this.vAxis;
                       } else {
                           chartOptions.vAxis = {title: this.vAxis}
                       }
                    }

                    if(Utils.isDefined(this.chartHeight)) {
                        chartOptions.height=this.chartHeight;
                    }
                }

                if(chartType == DISPLAY_BARTABLE) {
                    var height  = "";
                    if(Utils.isDefined(this.chartHeight)) {
                        height = this.chartHeight;
                    } else {
                        if(dataList.length>1)  {
                            var numBars  = dataList.length;
                            if(this.isStacked) {
                                height = numBars*22;
                            } else {
                                height = numBars*22+numBars*14*(dataList[0].length-2);
                            }
                        }
                    }
                    
                    var   chartOptions =  {
                        title: "the title",
                        bars: 'horizontal',
                        colors: this.colors,
                        width: (Utils.isDefined(this.chartWidth)?this.chartWidth:"100%"),
                        chartArea: {left:'30%',top:0,width:'70%',height:'80%'},
                        height: height,
                        bars: 'horizontal',
                        tooltip: {showColorCode: true},
                        legend: { position: 'none' },
                    };

                    if(Utils.isDefined(this.isStacked)) {
                        chartOptions.isStacked = this.isStacked;                        
                    }

                    if(this.hAxis)
                        chartOptions.hAxis= {title: this.hAxis};
                    if(this.vAxis)
                        chartOptions.vAxis= {title: this.vAxis};
                    //                    console.log(chartOptions);
                    //                    this.chart = new google.visualization.BarChart(document.getElementById(chartId));
                    this.chart = new google.charts.Bar(document.getElementById(chartId));


                } else if(chartType == DISPLAY_BARCHART || chartType == DISPLAY_BARSTACK) {
                    if(chartType == DISPLAY_BARSTACK) {
                        chartOptions.isStacked = true;
                    }
                    chartOptions.orientation =  "horizontal";
                    this.chart = new google.visualization.BarChart(document.getElementById(chartId));

                } else  if(chartType == DISPLAY_SCATTERPLOT) {
                    var height  = 400;
                    if(Utils.isDefined(this.chartHeight)) {
                        height = this.chartHeight;
                    }
                    var width  = "100%";
                    if(Utils.isDefined(this.chartWidth)) {
                        width = this.chartWidth;
                    }
                    //                    $("#" + chartId).css("border","1px red solid");
                    //                    $("#" + chartId).css("width",width);
                    $("#" + chartId).css("width",width);
                    $("#" + chartId).css("height",height);
                    chartOptions = {
                        title: '',
                        legend: 'none',
                        chartArea: {left:"10%", top:10, height:"80%",width:"90%"}
                    };

                    if(this.getShowTitle()) {
                        chartOptions.title =  this.getTitle(true);
                    }

                    if(dataList.length>0 && dataList[0].length>1) { 
                        chartOptions.hAxis =  {title: dataList[0][0]};
                        chartOptions.vAxis =  {title: dataList[0][1]};
                        //We only have the one vAxis range for now
                        if(!isNaN(this.vAxisMinValue)) {
                            chartOptions.hAxis.minValue = this.vAxisMinValue;
                            chartOptions.vAxis.minValue = this.vAxisMinValue;
                        }
                        if(!isNaN(this.vAxisMaxValue)) {
                            chartOptions.hAxis.maxValue = this.vAxisMaxValue;
                            chartOptions.vAxis.maxValue = this.vAxisMaxValue;
                        }
                    }


                    this.chart = new google.visualization.ScatterChart(document.getElementById(chartId));
                } else  if(chartType == DISPLAY_PIECHART) {
                    if(this.is3D) {
                        chartOptions.is3D = true;
                    }
                    if(this.pieHole) {
                        chartOptions.pieHole = this.pieHole;
                    }
                    if(this.sliceVisibilityThreshold) {
                        chartOptions.sliceVisibilityThreshold = this.sliceVisibilityThreshold;
                    }
                    this.chart = new google.visualization.PieChart(document.getElementById(chartId));
                } else  if(chartType == DISPLAY_TABLE) {

                    chartOptions.height = null;
                    if(this.chartHeight)  {
                        chartOptions.height = this.chartHeight;
                    }
                    if(chartOptions.height == null) {
                        var height = this.getProperty("height",null);
                        if(height) {
                            chartOptions.height = height;
                        }
                    }
                    if(chartOptions.height == null) {
                        chartOptions.height = "300px";
                    }
                    chartOptions.allowHtml = true;
                    if(dataList.length && dataList[0].length>4) {
                        chartOptions.cssClassNames = {headerCell: 'display-table-header-max' };
                    } else {
                        chartOptions.cssClassNames = {headerCell: 'display-table-header' };
                    }
                    this.chart = new google.visualization.Table(document.getElementById(chartId));
                } else {
                    //                    this.chart =  new Dygraph.GVizChart(
                    //                    document.getElementById(chartId));
                    this.chart = new google.visualization.LineChart(document.getElementById(chartId));
                }
                if(this.chart!=null) {
                    if(!Utils.isDefined(chartOptions.height)) {
                        chartOptions.height = "100%";
                    }
                    this.chart.draw(dataTable, chartOptions); 
                   var theDisplay = this;

                    google.visualization.events.addListener(this.chart, 'onmouseover', function(event) {
                            mapVar  = theDisplay.getProperty("mapVar",null);
                            if(!Utils.stringDefined(mapVar)) {
                                return;
                            }
                            row = event.row;
                            pointData = theDisplay.dataCollection.getList()[0];
                            var fields =  pointData.getRecordFields();
                            var records = pointData.getRecords();
                            var record = records[row];
                            map = ramaddaMapMap[mapVar];
                            if(map) {
                                if(theDisplay.mouseOverPoint)
                                    map.removePoint(theDisplay.mouseOverPoint);
                            } else {
                            }
                            if(record && map) {
                                latField = null;
                                lonField = null;
                                for(i=0;i<fields.length;i++) {
                                    if(fields[i].isFieldLatitude()) latField = fields[i];
                                    else if(fields[i].isFieldLongitude()) lonField = fields[i];
                                }
                                if(latField && lonField) {
                                    lat = record.getValue(latField.getIndex());
                                    lon = record.getValue(lonField.getIndex());
                                    theDisplay.mouseOverPoint  = map.addPoint(chartId, new OpenLayers.LonLat(lon,lat));
                                }
                            }
                        });
                    google.visualization.events.addListener(this.chart, 'select', function(event) {
                            if(theDisplay.chart.getSelection) {
                                var selected = theDisplay.chart.getSelection();
                                if(selected && selected.length>0) {
                                    var index = selected[0].row;
                                    theDisplay.displayManager.handleEventRecordSelection(theDisplay, 
                                                                                         theDisplay.dataCollection.getList()[0], index);
                                }
                            }
                        });
                }
            }
        });


    this.makeChart = this.makeGoogleChart;
}



function LinechartDisplay(displayManager, id, properties) {
    properties = $.extend({"chartType": DISPLAY_LINECHART}, properties);
    RamaddaUtil.inherit(this, new RamaddaMultiChart(displayManager, id, properties));
    addRamaddaDisplay(this);
}

function BarchartDisplay(displayManager, id, properties) {
    properties = $.extend({"chartType": DISPLAY_BARCHART}, properties);
    RamaddaUtil.inherit(this, new RamaddaMultiChart(displayManager, id, properties));
    addRamaddaDisplay(this);
}

function BarstackDisplay(displayManager, id, properties) {
    properties = $.extend({"chartType": DISPLAY_BARSTACK,"isStacked":true}, properties);
    RamaddaUtil.inherit(this, new RamaddaMultiChart(displayManager, id, properties));
    addRamaddaDisplay(this);
}

function PiechartDisplay(displayManager, id, properties) {
    properties = $.extend({"chartType": DISPLAY_PIECHART}, properties);
    RamaddaUtil.inherit(this, new RamaddaMultiChart(displayManager, id, properties));
    addRamaddaDisplay(this);
}


function TableDisplay(displayManager, id, properties) {
    properties = $.extend({"chartType": DISPLAY_TABLE}, properties);
    RamaddaUtil.inherit(this, new RamaddaMultiChart(displayManager, id, properties));
    addRamaddaDisplay(this);
}


function BartableDisplay(displayManager, id, properties) {
    properties = $.extend({"chartType": DISPLAY_BARTABLE}, properties);
    RamaddaUtil.inherit(this, new RamaddaMultiChart(displayManager, id, properties));
    $.extend(this, {
            getDefaultSelectedFields: function(fields, dfltList) {
                var f = [];
                for(i=0;i<fields.length;i++) { 
                    var field = fields[i];
                    if(!field.isNumeric) {
                        f.push(field);
                        break;
                    }
                }
                for(i=0;i<fields.length;i++) { 
                    var field = fields[i];
                    if(field.isNumeric) {
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
    properties = $.extend({"chartType": DISPLAY_SCATTERPLOT}, properties);
    RamaddaUtil.inherit(this, new RamaddaMultiChart(displayManager, id, properties));
    $.extend(this, {
            getDefaultSelectedFields: function(fields, dfltList) {
                var f = [];
                for(i=0;i<fields.length;i++) { 
                    var field = fields[i];
                    if(field.isNumeric) {
                        f.push(field);
                        if(f.length>=2) 
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
            lastHtml:"<p>&nbsp;<p>&nbsp;<p>",
            initDisplay: function() {
                this.initUI();
                this.setContents(this.lastHtml);
            },
            handleEventRecordSelection: function(source,  args) {
                this.lastHtml = args.html;
                this.setContents(args.html);
            }
        });
}


function RamaddaStatsDisplay(displayManager, id, properties) {
    var SUPER;
    $.extend(this, {
            showMin:true,
                showMax: true,
                showAverage:true});
    RamaddaUtil.inherit(this, SUPER = new RamaddaFieldsDisplay(displayManager, id, DISPLAY_STATS, properties));
    addRamaddaDisplay(this);


    RamaddaUtil.defineMembers(this, {
            "map-display": false,
            needsData: function() {
                return true;
                //                return this.getProperty("loadData", false) || this.getCreatedInteractively();
            },
            handleEventPointDataLoaded : function(source, pointData) {
                if(!this.needsData()) {
                    if(this.dataCollection == null) {
                        this.dataCollection =  source.dataCollection;
                        this.updateUI();
                    }
                }
            },
            getDefaultSelectedFields: function(fields, dfltList) {
                if(dfltList!=null && dfltList.length>0) {
                    return dfltList;
                }
                var tuples = this.getStandardData(null,{includeIndex: false});
                var justOne = (tuples.length == 2);

                //get the numeric fields
                var l = [];
                for(i=0;i<fields.length;i++) { 
                    var field = fields[i];
                    if(!justOne && !field.isNumeric) continue;
                    var lbl  =field.getLabel().toLowerCase();
                    if(lbl.indexOf("latitude")>=0 || lbl.indexOf("longitude")>=0) {
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
                this.updateUI();
            },
            updateUI: function(pointData) {
                SUPER.updateUI.call(this,pointData);
                //                console.log("stats.updateUI:" + this.hasData());
                if(!this.hasData()) {
                    this.setContents(this.getLoadingMessage());
                    return;
                }
                var tuples = this.getStandardData(null,{includeIndex: false});
                var allFields =  this.dataCollection.getList()[0].getRecordFields();
                this.allFields =  allFields;
                var fields = this.getSelectedFields([]);
                var fieldMap = {};
                var stats = new Array();
                var justOne = (tuples.length == 2);
                for(var rowIdx=1;rowIdx<tuples.length;rowIdx++) {
                    var tuple = tuples[rowIdx];
                    if(rowIdx == 1) {
                        for(var col=0;col<tuple.length;col++) {
                            stats.push({isNumber:false,min:Number.MAX_SAFE_INTEGER,max:Number.MIN_SAFE_INTEGER,total:0});
                        }
                    }
                    for(var fieldIdx=0;fieldIdx<fields.length;fieldIdx++) {
                        var field = fields[fieldIdx];
                        var col = field.getIndex()
                        var v  = tuple[col];
                        if(typeof v == 'number') {
                            var label = field.getLabel().toLowerCase();
                            if(label.indexOf("latitude")>=0 || label.indexOf("longitude")>=0) {
                                continue;
                            }

                            if(!stats[col].isNumber) {
                                stats[col].max = v;
                                stats[col].min = v;
                            }
                            stats[col].isNumber = true;
                            stats[col].total+=v;
                            stats[col].max=Math.max(stats[col].max, v);
                            stats[col].min=Math.min(stats[col].min, v);
                        }
                    }
                }

                var border = (justOne?"0":"1");
                var html = HtmlUtil.openTag("table",["border", border ,"bordercolor","#ccc","class","display-stats","cellspacing","1","cellpadding","5"]);
                var dummy = ["&nbsp;"];
                if(!justOne) {
                    header = [""];
                    if(this.showMin) {
                        header.push("Min");
                        dummy.push("&nbsp;");
                    }
                    if(this.showMax) {
                        header.push("Max");
                        dummy.push("&nbsp;");
                    }
                    header.push("Total");
                    dummy.push("&nbsp;");
                    if(this.showAverage) {
                        header.push("Average");
                        dummy.push("&nbsp;");
                    }
                    html += HtmlUtil.tr([],HtmlUtil.tds(["class","display-stats-header","align","center"],header));
                }
                var cats = [];
                var catMap = {};
                for(var fieldIdx=0;fieldIdx<fields.length;fieldIdx++) {
                    var field = fields[fieldIdx];
                    var col = field.getIndex()
                    var field = allFields[col];
                    var right = "";
                    if(stats[col].isNumber) {
                        var total = "&nbsp;";
                        var avg  = this.formatNumber(stats[col].total/tuples.length);
                        var label = field.getLabel().toLowerCase();
                        //Some guess work about when to show a total
                        if(label.indexOf("%")<0 && label.indexOf("percent")<0 && label.indexOf("median")<0) {
                            total  =  this.formatNumber(stats[col].total);
                        } 
                        var values = [];
                        if(justOne) {
                            right = HtmlUtil.tds(["xalign","right"],[this.formatNumber(stats[col].min)]);
                        } else {
                            if(this.showMin)
                                values.push(this.formatNumber(stats[col].min));
                            if(this.showMax)
                                values.push(this.formatNumber(stats[col].max));
                            values.push(total);
                            if(this.showAverage)
                                values.push(avg);
                            right = HtmlUtil.tds(["align","right"],values);
                        }
                    } else {
                        right = HtmlUtil.tds([],dummy);
                    }
                    var align = (justOne?"right":"left");

                    var label = field.getLabel();
                   var toks =  label.split("!!");
                    var title = label;
                    label = toks[toks.length-1];
                    if(justOne) {
                        label +=":";
                    }
                    var row =  HtmlUtil.tr([],HtmlUtil.td(["align",align],"<b>" +HtmlUtil.tag("div", ["title",title], label)+"</b>") + right);
                    if(justOne) {
                        html += row;
                    } else {
                        html += row;
                    }
                }
                html += "</table>";
                this.setContents(html);
                $( document ).tooltip();
            },
            handleEventRecordSelection: function(source,  args) {
                //                this.lastHtml = args.html;
                //                this.setContents(args.html);
            }
        });
}



function RamaddaCrosstabDisplay(displayManager, id, properties) {
    var SUPER;
    RamaddaUtil.inherit(this, SUPER = new RamaddaFieldsDisplay(displayManager, id, DISPLAY_CROSSTAB, properties));
    addRamaddaDisplay(this);
    this.columns=this.getProperty("columns","").split(",");
    this.rows=this.getProperty("rows","").split(",");
    RamaddaUtil.defineMembers(this, {
            "map-display": false,
            needsData: function() {
                return true;
            },
            handleEventPointDataLoaded : function(source, pointData) {
                if(!this.needsData()) {
                    if(this.dataCollection == null) {
                        this.dataCollection =  source.dataCollection;
                        this.updateUI();
                    }
                }
            },
            fieldSelectionChanged: function() {
                this.updateUI();
            },
            updateUI: function(pointData) {
                SUPER.updateUI.call(this,pointData);
                if(!this.hasData()) {
                    this.setContents(this.getLoadingMessage());

                    return;
                }
                var tuples = this.getStandardData(null,{includeIndex: false});
                var allFields =  this.dataCollection.getList()[0].getRecordFields();
                var fieldMap = {};
                cols = [];
                rows = [];
                
                for(var j=0;j<this.columns.length;j++) {
                    var name = this.columns[j];
                    for(var i=0;i<allFields.length;i++) {
                        field = allFields[i];
                        if(name == field.getLabel()|| name == ("#" +(i+1))) {
                            cols.push(allFields[i]);
                            break;
                        }
                    }
                }
                for(var j=0;j<this.rows.length;j++) {
                    var name = this.rows[j];
                    for(var i=0;i<allFields.length;i++) {
                        if(name == allFields[i].getLabel()|| name == ("#" +(i+1))) {
                            rows.push(allFields[i]);
                            break;
                        }
                    }
                }
                var html = HtmlUtil.openTag("table",["border", "1px" ,"bordercolor","#ccc","class","display-stats","cellspacing","1","cellpadding","5"]);
                var uniques = {};
                var seen = {};
                for(var j=0;j<cols.length;j++) {
                    var col = cols[j];
                    var key = col.getLabel();
                    for(var rowIdx=1;rowIdx<tuples.length;rowIdx++) {
                        var tuple = tuples[rowIdx];
                        var colValue = tuple[col.getIndex()];
                        if (!(key in uniques)) {
                            uniques[key] = [];
                        }
                        var list = uniques[key];
                        if (list.indexOf(colValue)<0) {
                            console.log(colValue);
                            list.push(colValue);
                        }
                    }
                }

                for(key in uniques) {
                    uniques[key].sort();
                    console.log(uniques[key]);
                }


                for(var j=0;j<cols.length;j++) {
                    var col = cols[j];
                    for(var rowIdx=1;rowIdx<tuples.length;rowIdx++) {
                        var tuple = tuples[rowIdx];
                        //                        var colValue = tuple;
                        //html += HtmlUtil.tr([],HtmlUtil.tds(["class","display-stats-header","align","center"],["","Min","Max","Total","Average"]));
                        for(var i=0;i<rows.length;i++) {
                            var row = rows[j];


                        }
                    }
                }
                html += "</table>";
                this.setContents(html);
                $( document ).tooltip();
            },
        });
}




function RamaddaCorrelationDisplay(displayManager, id, properties) {
    var SUPER;
    $.extend(this, {
            colorBar:"red_white_blue",
            colorByMin:"-1",
            colorByMax:"1",
                });

    RamaddaUtil.inherit(this, SUPER = new RamaddaFieldsDisplay(displayManager, id, DISPLAY_CORRELATION, properties));
    addRamaddaDisplay(this);

    RamaddaUtil.defineMembers(this, {
            "map-display": false,
            needsData: function() {
                return true;
            },
            getMenuItems: function(menuItems) {
                SUPER.getMenuItems.call(this,menuItems);
                var get = this.getGet();
                var tmp = HtmlUtil.formTable();
                var ct = "<select id=" + this.getDomId("colorbar")+">";
                for(table in Utils.ColorTables) {
                    if(table == this.colorBar)
                        ct+="<option selected>"+ table+"</option>";
                    else
                        ct+="<option>"+ table+"</option>";
                }
                ct+= "</select>";

                tmp += HtmlUtil.formEntry("Color Bar:",ct);
                                          
                tmp += HtmlUtil.formEntry("Color By Range:", HtmlUtil.input("", this.colorByMin, ["size","7",ATTR_ID,  this.getDomId("colorbymin")]) + " - " +
                                          HtmlUtil.input("", this.colorByMax, ["size","7",ATTR_ID,  this.getDomId("colorbymax")]));
                tmp += "</table>";
                menuItems.push(tmp);
            },
           initDialog: function() {
                SUPER.initDialog.call(this);
                var _this  = this;
                var updateFunc  = function() {
                    _this.colorByMin = _this.jq("colorbymin").val();
                    _this.colorByMax = _this.jq("colorbymax").val();
                    _this.updateUI();
                    
                };
                var func2  = function() {
                    _this.colorBar = _this.jq("colorbar").val();
                    _this.updateUI();
                    
                };
                this.jq("colorbymin").blur(updateFunc);
                this.jq("colorbymax").blur(updateFunc);
                this.jq("colorbar").change(func2);
        },

            handleEventPointDataLoaded : function(source, pointData) {
                if(!this.needsData()) {
                    if(this.dataCollection == null) {
                        this.dataCollection =  source.dataCollection;
                        this.updateUI();
                    }
                }
            },
            fieldSelectionChanged: function() {
                this.updateUI();
            },
            updateUI: function(pointData) {
                SUPER.updateUI.call(this,pointData);
                if(!this.hasData()) {
                    this.setContents(this.getLoadingMessage());
                    return;
                }
                var tuples = this.getStandardData(null,{includeIndex: false});
                var allFields =  this.dataCollection.getList()[0].getRecordFields();
                var fields = this.getSelectedFields([]);
                if(fields.length==0) fields = allFields;
                var html = HtmlUtil.openTag("table",["border", "1px" ,"bordercolor","#ccc","class","display-correlation","cellspacing","1","cellpadding","5"]);

                html+="<tr><td class=display-heading>&nbsp;</td>";
                for(var fieldIdx=0;fieldIdx<fields.length;fieldIdx++) {
                    var field1 = fields[fieldIdx];
                    if(!field1.isFieldNumeric() || field1.isFieldGeo()) continue;
                    html+= "<td align=center class=display-heading><b>" + field1.getLabel() +"</b></td>";
                }
                html+="</tr>";

                var colors = null;
                colorByMin = parseFloat(this.colorByMin);
                colorByMax = parseFloat(this.colorByMax);
                if(this.colorBar && this.colorBar !="none")
                    colors = Utils.ColorTables[this.colorBar];
                for(var fieldIdx1=0;fieldIdx1<fields.length;fieldIdx1++) {
                    var field1 = fields[fieldIdx1];

                    if(!field1.isFieldNumeric() || field1.isFieldGeo())  continue;
                    html+="<tr><td class=display-heading><b>" + field1.getLabel() +"</b></td>";
                    for(var fieldIdx2=0;fieldIdx2<fields.length;fieldIdx2++) {
                        var field2 = fields[fieldIdx2];
                        if(!field2.isFieldNumeric() || field2.isFieldGeo()) continue;
                        var t1=0;
                        var t2=0;
                        var cnt=0;

                        for(var rowIdx=1;rowIdx<tuples.length;rowIdx++) {
                            var tuple = tuples[rowIdx];
                            var v1  = tuple[field1.getIndex()];
                            var v2  = tuple[field2.getIndex()];
                            t1+=v1;
                            t2+=v2;
                            cnt++;
                        }
                        var avg1 = t1/cnt;
                        var avg2 = t2/cnt;
                        var sum1 = 0;
                        var sum2 = 0;
                        var sum3 = 0;
                        for(var rowIdx=1;rowIdx<tuples.length;rowIdx++) {
                            var tuple = tuples[rowIdx];
                            var v1  = tuple[field1.getIndex()];
                            var v2  = tuple[field2.getIndex()];
                            sum1+= (v1-avg1)*(v2-avg2);
                            sum2+= (v1-avg1)*(v1-avg1);
                            sum3+= (v2-avg2)*(v2-avg2);
                        }                        
                        r = sum1/Math.sqrt(sum2*sum3);

                        var style="";
                        if(colors!=null) {
                            var percent = (r- colorByMin)/(colorByMax-colorByMin);
                            var index = parseInt(percent*colors.length);
                            if(index>=colors.length) index = colors.length-1;
                            else if(index<0) index = 0;
                            style = "background-color:" + colors[index];
                        }
                        html+="<td align=right style=\"" + style +"\">" + r.toFixed(3) +"</td>";
                    }
                    html+="</tr>";
                }
                html += "</table>";
                this.setContents(html);

                $( document ).tooltip();
            },
        });
}



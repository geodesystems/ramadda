/**
Copyright 2008-2018 Geode Systems LLC
*/

var CHARTS_CATEGORY = "Charts";
var DISPLAY_LINECHART = "linechart";
var DISPLAY_AREACHART = "areachart";
var DISPLAY_BARCHART = "barchart";
var DISPLAY_BARTABLE = "bartable";
var DISPLAY_BARSTACK = "barstack";
var DISPLAY_PIECHART = "piechart";
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
addGlobalDisplayType({type: DISPLAY_AREACHART, label:"Area Chart",requiresData:true,forUser:true,category:CHARTS_CATEGORY});
addGlobalDisplayType({type:DISPLAY_BARTABLE,label: "Bar Table",requiresData:true,forUser:true,category:CHARTS_CATEGORY});
addGlobalDisplayType({type:DISPLAY_SCATTERPLOT,label: "Scatter Plot",requiresData:true,forUser:true,category:CHARTS_CATEGORY});
addGlobalDisplayType({type:DISPLAY_HISTOGRAM,label: "Histogram",requiresData:true,forUser:true,category:CHARTS_CATEGORY});
addGlobalDisplayType({type:DISPLAY_BUBBLE,label: "Bubble Chart",requiresData:true,forUser:true,category:CHARTS_CATEGORY});
addGlobalDisplayType({type:DISPLAY_GAUGE,label: "Gauge",requiresData:true,forUser:true,category:CHARTS_CATEGORY});
addGlobalDisplayType({type:DISPLAY_PIECHART,label: "Pie Chart",requiresData:true,forUser:true,category:CHARTS_CATEGORY});

addGlobalDisplayType({type:DISPLAY_CALENDAR,label: "Calendar Chart",requiresData:true,forUser:true,category:"Misc"});
addGlobalDisplayType({type:DISPLAY_STATS , label: "Stats Table",requiresData:false,forUser:true,category:"Misc"});
addGlobalDisplayType({type:DISPLAY_TABLE , label: "Table",requiresData:true,forUser:true,category:"Misc"});
addGlobalDisplayType({type:DISPLAY_TEXT , label: "Text Readout",requiresData:false,forUser:true,category:"Misc"});
addGlobalDisplayType({type:DISPLAY_CORRELATION , label: "Correlation",requiresData:true,forUser:true,category:"Misc"});




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

    _this.redisplayPending = false;
    _this.redisplayPendingCnt = 0;

    //Another hack to redraw the chart after the window is resized
    $(window).resize(function() {
        var theDisplay = _this.getThis();
         if(!theDisplay.getDisplayReady())  {
             return;
         }
	//This handles multiple resize events but keeps only having one timeout pending at a time
	if(theDisplay.redisplayPending) {
	    theDisplay.redisplayPendingCnt++;
	    return;
	}
	var timeoutFunc = function(myCnt){
            if(myCnt == theDisplay.redisplayPendingCnt) {
		//Ready to redisplay
		theDisplay.redisplayPending = false;
		theDisplay.redisplayPendingCnt=0;
		theDisplay.displayData();
            } else {
		//Had a resize event during the previous timeout
		setTimeout(timeoutFunc.bind(null,theDisplay.redisplayPendingCnt),1000);
	    }
	}
	theDisplay.redisplayPending = true;
        setTimeout(timeoutFunc.bind(null,theDisplay.redisplayPendingCnt),1000);
    });

    //Init the defaults first
    $.extend(this, {
            indexField: -1,
                colorList: ['blue', 'red', 'green', 'orange','fuchsia','teal','navy','silver'],
                curveType: 'none',
                fontSize: 0,
                vAxisMinValue:NaN,
                vAxisMaxValue:NaN,
                showTrendlines: false,
                showPercent: false,
                percentFields: null
                });
    if(properties.colors)  {
        this.colorList = (""+properties.colors).split(",");
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
            clearCachedData: function() {
                SUPER.clearCachedData();
                this.computedData = null;
            },
            updateUI: function() {
                SUPER.updateUI.call(this);
                if(!this.getDisplayReady()) {
                    return;
                }
                this.displayData();
            },
            getWikiAttributes: function(attrs) {
                this.defineWikiAttributes(["vAxisMinValue","vAxisMaxValue"]);
                SUPER.getWikiAttributes.call(this, attrs);
                if(this.colorList.join(",") != "blue,red,green") {
                    attrs.push("colors");
                    attrs.push(this.colorList.join(", "));
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
                        _this.colorList = v.split(",");
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
                var v = prompt("Enter comma separated list of colors to use", this.colorList.join(","));
                if(v!=null) {
                    this.colorList = v.split(",");
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
                                          HtmlUtil.input("", this.colorList.join(","), ["size","35",ATTR_ID,  this.getDomId(ID_COLORS)]));
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
                var data =   this.dataCollection.getList()[0];
                if(data != args.data) {
                    return;
                }
                //                console.log("chart index="+ args.index);
                this.setChartSelection(args.index);
            },
            getFieldsToSelect: function(pointData) {
                var chartType = this.getProperty(PROP_CHART_TYPE,DISPLAY_LINECHART);
                if(this.chartType == DISPLAY_TABLE || this.chartType == DISPLAY_BARTABLE || this.chartType == DISPLAY_BUBBLE) {
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
                var _this =this;
                if(!this.getDisplayReady()) {
                    return;
                }
                if(this.inError) {
                    return;
                }
                if(!haveGoogleChartsLoaded ()) {
                    //console.log("charts not loaded");
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
                if(this.chartType == DISPLAY_TABLE || this.chartType == DISPLAY_BARTABLE || chartType == DISPLAY_PIECHART || chartType == DISPLAY_SCATTERPLOT || chartType == DISPLAY_HISTOGRAM|| chartType == DISPLAY_BUBBLE|| chartType == DISPLAY_GAUGE)  {
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
                            this.groupByField  = field;
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
                var dataList = this.computedData;
                if(this["function"] && this.computedData==null) {
                    var pointData =   this.dataCollection.getList()[0];
                    var allFields = pointData.getRecordFields();
                    var records = pointData.getRecords();
                    var indexField = this.indexField;
                    var chartableFields =this.getFieldsToSelect(pointData);
                    this.hasDate = this.getHasDate(records);
                    var date_formatter = this.getDateFormatter();
                    var setVars = "";
                    for(var i=0;i<chartableFields.length;i++) {
                        var field = chartableFields[i];
                        setVars+="\tvar " + field.getId() +"=args." + field.getId()+";\n";
                    }
                    var code = "function displayChartEval(args) {\n" + setVars +"\treturn  " + this["function"]+"\n}";
                    eval(code);
                    var newList = [];
                    var fieldNames = null;
                    var rowCnt = -1;
                    var indexField = this.indexField;
                    for(var rowIdx=0;rowIdx<records.length;rowIdx++)  {
                        var record = records[rowIdx];
                        var row = record.getData();
                        var date = record.getDate();
                        if(!this.dateInRange(date)) continue;
                        rowCnt++;
                        var values = [];
                        var indexName = null;
                        if(indexField>=0) {
                            var field = allFields[indexField];
                            values.push(record.getValue(indexField)+offset);
                            indexName =  field.getLabel();
                        } else {
                            if(this.hasDate) {
                                values.push(this.getDateValue(date, date_formatter));
                                indexName = "Date";
                            } else {
                                values.push(rowIdx);
                                indexName = "Index";
                            }
                        }
                        if(fieldNames==null) {
                            fieldNames = [indexName, this.functionName?this.functionName:"value"];
                            newList.push(fieldNames);
                        }
                        var args = {};
                        for(var j=0;j<chartableFields.length;j++) {
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

                if(dataList == null) {
                    dataList = this.getStandardData(fieldsToSelect, props);
                }
                this.computedData= dataList;

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

                this.makeChart(chartType, dataList, props, selectedFields);

                var d = _this.jq(ID_CHART);
                this.lastWidth = d.width();
                if(d.width() == 0) {
                    //                    _this.checkWidth(0);
                }
            },
           //This keeps checking the width of the chart element if its zero
           //we do this for displaying in tabs
            checkLayout: function() {
                var _this = this;
                var d = _this.jq(ID_CHART);
                //       console.log("checklayout:  widths:" + this.lastWidth +" " + d.width());
                if(this.lastWidth!=d.width()) {
                    _this.displayData();
                }
                if(true)return;

                if(d.width() ==0) {
                    var cb = function() {
                        _this.checkWidth(cnt+1);
                    };
                    setTimeout(cb,5000);
                } else {
                    //                    console.log("checkWidth:"+ _this.getTitle() +" calling displayData");
                    _this.displayData();
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
                    if(this.chart.setSelection) {
                        this.chart.setSelection([{row:index, column:null}]); 
                    }
                    //                    var container = $('#table_div').find('.google-visualization-table-table:eq(0)').parent();
                    //                    var header = $('#table_div').find('.google-visualization-table-table:eq(1)').parent();
                    //                    var row = $('.google-visualization-table-tr-sel');
                    //                    $(container).prop('scrollTop', $(row).prop('offsetTop') - $(header).height());
                }
            },

            tableHeaderMouseover: function(i,tooltip) {
                //alert("new:" + tooltip);
            },
            makeDataTable:function(chartType,dataList,props,selectedFields) {
                if(dataList.length==1 || this.chartType == DISPLAY_TABLE) {
                    return  google.visualization.arrayToDataTable(dataList);
                }
                if(this.chartType == DISPLAY_GAUGE) {
                    return this.makeGaugeDataTable(dataList);
                }
                if(this.chartType == DISPLAY_HISTOGRAM) {
                    return  google.visualization.arrayToDataTable(dataList);
                }
                if(this.chartType == DISPLAY_BUBBLE) {
                    return  google.visualization.arrayToDataTable(dataList);
                }

                if(this.chartType == DISPLAY_CALENDAR) {
                    var dataTable = new google.visualization.DataTable();
                    var header = dataList[0];
                    dataTable.addColumn({ type: 'date', id: 'Date' });
                    dataTable.addColumn({ type: 'number', id: header[1]});
                    var list = [];
                    for(var i=1;i<dataList.length;i++) {
                        list.push([dataList[i][0],dataList[i][1]]);
                    }
                    dataTable.addRows(list);
                }

                if(this.chartType == DISPLAY_PIECHART) {
                    var dataTable = new google.visualization.DataTable();
                    var list = [];
                    var groupBy = this.groupByField;
                    var data = selectedFields[0];
                    var header = dataList[0];
                    dataTable.addColumn("string",header[0]);
                    dataTable.addColumn("number",header[1]);
                    //                    dataTable.addColumn({type:'string',role:'tooltip'});
                    for(var i=1;i<dataList.length;i++) {
                        //                        list.push([dataList[i][0],dataList[i][1],"\n" +header[0]+": " + dataList[i][0] +"\n" +header[1] +": " +dataList[i][1]]);
                        list.push([dataList[i][0],dataList[i][1]]);
                    }
                    dataTable.addRows(list);
                    return dataTable;
                }
                var dataTable = new google.visualization.DataTable();
                var header = dataList[0];
                var sample = dataList[1];
                for(var j=0;j<header.length;j++) {
                    var value = sample[j];
                    if(j==0 && props.includeIndex) {
                        //This might be a number or a date
                        if((typeof value) == "object") {
                            //assume its a date
                            dataTable.addColumn('date', header[j]);
                        } else {
                            dataTable.addColumn((typeof value), header[j]);
                        }
                   } else {
                        //Assume all remaining fields are numbers
                        dataTable.addColumn('number', header[j]);
                        dataTable.addColumn({type: 'string', role: 'tooltip','p': {'html': true}});
                    }
                }
                var justData= [];
                var begin = props.includeIndex?1:0;
                for(var i=1;i<dataList.length;i++) {
                    var row = dataList[i];
                    row  = row.slice(0);
                    var tooltip = "<div style='padding:8px;'>";
                    for(var j=0;j<row.length;j++) {
                        if(j>0)
                            tooltip+="<br>";
                        label = header[j].replace(/ /g,"&nbsp;");
                        value = row[j];
                        if(!value) value ="NA";
                        if(value && (typeof value) =="object") {
                            if(value.f) {
                                value = value.f;
                            }  
                        }
                        if(Utils.isNumber(value)) {
                            value  = this.formatNumber(value);
                        }
                        value = ""+value;
                        value = value.replace(/ /g,"&nbsp;");
                        tooltip+="<b>" +label+"</b>:&nbsp;" +value;
                    }
                    tooltip+="</div>";
                    newRow = [];
                    for(var j=0;j<row.length;j++) {
                        var value = row[j];
                        newRow.push(value);
                        if(j==0 && props.includeIndex) {
                            //is the index so don't add a tooltip
                        } else {
                            newRow.push(tooltip);
                        }
                    }
                    justData.push(newRow);
                }
                dataTable.addRows(justData);
                return dataTable;
            },

            makeGoogleChart: function(chartType, dataList, props, selectedFields) {
                //                for(var i=0;i<selectedFields.length;i++) 
                //                    console.log(selectedFields[i].getId());
                //                console.log("makeGoogleChart:" + chartType);
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

                var dataTable = this.makeDataTable(chartType, dataList, props,selectedFields);

                /*
                for(var i=1;i<dataList.length;i++) {
                    var row = dataList[i];
                    var s = "";
                    //                    row.push("index:" + i);
                    for(var j=0;j<row.length;j++) {
                        s  = s +" -  "  + row[j];
                    }
                }
                */
                var   chartOptions = {
                    tooltip: {isHtml: true},
                };
                $.extend(chartOptions, {
                        lineWidth: 1,
                        colors: this.colorList,
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
                        style += "width:" + "100%;";
                    if(this.chartHeight) 
                        style += "height:" + this.chartHeight +";" ;                    
                    else 
                        style += "height:" + "100%;";
                    divAttrs.push(style);
                } else {
                    //                    divAttrs.push("style");
                    //                    divAttrs.push("width:600px;");
                }
                divAttrs.push("style");
                divAttrs.push("height:100%;");
                this.setContents(HtmlUtil.div(divAttrs,""));


                if(chartType == DISPLAY_LINECHART || chartType == DISPLAY_AREACHART || chartType == DISPLAY_BARCHART ||
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
                    
                    $.extend(chartOptions, {
                        title: "the title",
                        bars: 'horizontal',
                        colors: this.colorList,
                        width: (Utils.isDefined(this.chartWidth)?this.chartWidth:"100%"),
                        chartArea: {left:'30%',top:0,width:'70%',height:'80%'},
                        height: height,
                        bars: 'horizontal',
                        tooltip: {showColorCode: true},
                        legend: { position: 'none' },
                                });

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
                        tooltip: {isHtml: true},
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
                } else  if(chartType == DISPLAY_HISTOGRAM) {
                    if(this.legendPosition) {
                        chartOptions.legend={};
                        chartOptions.legend.position=this.legendPosition;
                    }
                    chartOptions.vAxis={};
                    chartOptions.vAxis.viewWindow={};
                    if(Utils.isDefined(this.logScale)) {
                        chartOptions.vAxis.logScale = (""+this.logScale) == true;
                    }
                    if(this.textPosition) {
                        chartOptions.vAxis.textPosition = this.textPosition;
                    }
                    
                    if(Utils.isDefined(this.minValue)) {
                        chartOptions.vAxis.viewWindow.min = parseFloat(this.minValue);
                    }
                    if(Utils.isDefined(this.maxValue)) {
                        chartOptions.vAxis.viewWindow.max = parseFloat(this.maxValue);
                    }
                    if(!isNaN(this.vAxisMaxValue)) {
                        chartOptions.vAxis.maxValue = parseFloat(this.vAxisMaxValue);
                    }
                    //                    console.log(JSON.stringify(chartOptions));
                    if(!isNaN(this.vAxisMinValue)) {
                        chartOptions.vAxis.minValue = parseFloat(this.vAxisMinValue);
                    }
                    this.chart = new google.visualization.Histogram(document.getElementById(chartId));

                } else  if(chartType == DISPLAY_GAUGE) {
                    this.dataList = dataList;
                    this.chartOptions = chartOptions;
                    var min =Number.MAX_VALUE;
                    var max =Number.MIN_VALUE;
                    for(var row=1;row<dataList.length;row++) {
                        var tuple = dataList[row];
                        for(var col=0;col<tuple.length;col++) {
                            if(!Utils.isNumber(tuple[col])) {
                                continue;
                            }
                            min = Math.min(min, tuple[col]);
                            max = Math.max(max, tuple[col]);
                        }
                    }
                    if(Utils.isDefined(this.gaugeMin)) 
                        min  = parseFloat(this.gaugeMin);
                    if(Utils.isDefined(this.gaugeMax)) 
                        max  = parseFloat(this.gaugeMax);
                    chartOptions.min = min;
                    chartOptions.max = max;
                    this.chart = new google.visualization.Gauge(document.getElementById(chartId));
                } else  if(chartType == DISPLAY_BUBBLE) {
                    if(this.colorTable)
                        chartOptions.colors=Utils.getColorTable(properties.colorTable);
                    else if(!this.colors)
                        chartOptions.colors=Utils.getColorTable("rainbow");
                    else
                        chartOptions.colors=this.colorList;
                    chartOptions.chartArea = {left:100,top:10,width:'98%',height:'90%'}
                    chartOptions.colorAxis  = {
                        legend: {
                            position:"in"
                        }
                    }

                    chartOptions.bubble  = {
                        textStyle: {auraColor:"none"},
                        stroke:"#666"
                    };
                    chartOptions.hAxis = {};
                    chartOptions.vAxis = {};
                    header = dataList[0];
                    chartOptions.hAxis.format =  this.getProperty("hAxisFormat", null);
                    chartOptions.vAxis.format =  this.getProperty("vAxisFormat", null);
                    chartOptions.hAxis.title =  this.getProperty("hAxisTitle", header.length>1?header[1]:null);
                    chartOptions.vAxis.title =  this.getProperty("vAxisTitle", header.length>2?header[2]:null);
                    this.chart = new google.visualization.BubbleChart(document.getElementById(chartId));
                } else  if(chartType == DISPLAY_CALENDAR) {
                    chartOptions.calendar = {
                        cellSize: parseInt(this.getProperty("cellSize",15))
                    };
                    this.chart = new google.visualization.Calendar(document.getElementById(chartId));
                } else  if(chartType == DISPLAY_PIECHART) {
                    chartOptions.tooltip = {textStyle: {color: '#000000'}, showColorCode: true};
                    chartOptions.title=dataList[0][0] +" - " +dataList[0][1];
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
                } else  if(chartType == DISPLAY_AREACHART) {
                    if(this.isStacked)
                        chartOptions.isStacked = true;
                    this.chart = new google.visualization.AreaChart(document.getElementById(chartId));
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
                                    theDisplay.displayManager.propagateEventRecordSelection(theDisplay, 
                                                                                            theDisplay.dataCollection.getList()[0], {index:index});
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

function AreachartDisplay(displayManager, id, properties) {
    properties = $.extend({"chartType": DISPLAY_AREACHART}, properties);
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

function CalendarDisplay(displayManager, id, properties) {
    properties = $.extend({"chartType": DISPLAY_CALENDAR}, properties);
    RamaddaUtil.inherit(this, new RamaddaMultiChart(displayManager, id, properties));
    addRamaddaDisplay(this);
    RamaddaUtil.inherit(this,{
            xgetDimensionsStyle: function() {
                var height = this.getProperty("height",200);
                if(height>0) {
                    return " height:" + height +"px; " + " max-height:" + height +"px; overflow-y: auto;";
                }
                return "";
            }
        });
}


function TableDisplay(displayManager, id, properties) {
    properties = $.extend({"chartType": DISPLAY_TABLE}, properties);
    RamaddaUtil.inherit(this, new RamaddaMultiChart(displayManager, id, properties));
    addRamaddaDisplay(this);
}

function HistogramDisplay(displayManager, id, properties) {
    properties = $.extend({"chartType": DISPLAY_HISTOGRAM}, properties);
    RamaddaUtil.inherit(this, new RamaddaMultiChart(displayManager, id, properties));
    addRamaddaDisplay(this);
}

function GaugeDisplay(displayManager, id, properties) {
    properties = $.extend({"chartType": DISPLAY_GAUGE}, properties);
    RamaddaUtil.inherit(this, new RamaddaMultiChart(displayManager, id, properties));
    addRamaddaDisplay(this);
    RamaddaUtil.inherit(this,{
            makeGaugeDataTable: function(dataList) {
                if(!Utils.isDefined(this.index)) this.index = dataList.length-1;
                var index = this.index+1;
                var list = [];
                list.push(["Label","Value"]);
                var header = dataList[0];
                if(index>=dataList.length) index = dataList.length-1;
                var row = dataList[index];
                for(var i=0;i<row.length;i++) {
                    if(!Utils.isNumber(row[i])) continue;
                    var h  = header[i];
                    if(h.length>20) {
                        var index = h.indexOf("(");
                        if(index>0) {
                            h = h.substring(0,index);
                        } 
                    }
                    if(h.length>20) {
                        h = h.substring(0,19)+"...";
                    }
                    if(this.gaugeLabel) h = this.gaugeLabel;
                    else if(this["gaugeLabel" + (i+1)]) h = this["gaugeLabel" + (i+1)];
                    list.push([h,row[i]]);
                }
                return  google.visualization.arrayToDataTable(list);
        },
        setChartSelection: function(index) {
                if(this.chart) {
                    this.index  = index;
                    var dataTable = this.makeGaugeDataTable(this.dataList);
                    this.chart.draw(dataTable, this.chartOptions);
                }
            },
                });
}

function BubbleDisplay(displayManager, id, properties) {
    properties = $.extend({"chartType": DISPLAY_BUBBLE}, properties);
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


function RamaddaStatsDisplay(displayManager, id, properties,type) {
    var SUPER;
    var dflt = Utils.isDefined(properties["showDefault"])?properties["showDefault"]:true;
    $.extend(this, {
            showMin:dflt,
                showMax: dflt,
                showAverage:dflt,
                showStd:dflt,
                showPercentile:dflt,
                showCount:dflt,
                showTotal: dflt,
                showPercentile:dflt,
                showMissing:dflt,
                showUnique:dflt,
                showType:dflt,
                showText: dflt,
                });
    RamaddaUtil.inherit(this, SUPER = new RamaddaFieldsDisplay(displayManager, id, type||DISPLAY_STATS, properties));

    if(!type)
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
                    if(!justOne && (!this.showText && !field.isNumeric)) continue;
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
            updateUI: function() {
                SUPER.updateUI.call(this);
                if(!this.hasData()) {
                    this.setContents(this.getLoadingMessage());
                    return;
                }
                var tuples = this.getStandardData(null,{includeIndex: false});
                var allFields =  this.dataCollection.getList()[0].getRecordFields();
                this.allFields =  allFields;
                var fields = this.getSelectedFields([]);
                var fieldMap = {};
                var stats = [];
                var justOne = (tuples.length == 2);
                for(var rowIdx=1;rowIdx<tuples.length;rowIdx++) {
                    var tuple = tuples[rowIdx];
                    if(rowIdx == 1) {
                        for(var col=0;col<tuple.length;col++) {
                            stats.push({isNumber:false,count:0,min:Number.MAX_SAFE_INTEGER,uniqueMap:{},unique:0,std:0,max:Number.MIN_SAFE_INTEGER,total:0,numMissing:0,numNotMissing:0,type:null,values:[]});
                        }
                    }
                    for(var fieldIdx=0;fieldIdx<fields.length;fieldIdx++) {
                        var field = fields[fieldIdx];
                        var col = field.getIndex()
                        stats[col].type = field.getType();
                        var v  = tuple[col];
                        if(v) {
                            if(!Utils.isDefined(stats[col].uniqueMap[v])) {
                                stats[col].uniqueMap[v] = 1;
                                stats[col].unique++;
                            } else {
                                stats[col].uniqueMap[v]++;
                            }
                        }
                        stats[col].isNumber = field.isNumeric;
                        stats[col].count++;
                        if(v==null) {
                            stats[col].numMissing++;
                        } else {
                            stats[col].numNotMissing++;
                        }
                        if(v && (typeof v == 'number')) {
                            var label = field.getLabel().toLowerCase();
                            if(label.indexOf("latitude")>=0 || label.indexOf("longitude")>=0) {
                                continue;
                            }
                            stats[col].total+=v;
                            stats[col].max=Math.max(stats[col].max, v);
                            stats[col].min=Math.min(stats[col].min, v);
                            stats[col].values.push(v);
                        }
                    }
                }


                if(this.showUnique) {
                    for(var fieldIdx=0;fieldIdx<fields.length;fieldIdx++) {
                        var field = fields[fieldIdx];
                        var col = field.getIndex();
                        stats[col].uniqueMax = 0;
                        stats[col].uniqueValue = "";
                        for(var v in stats[col].uniqueMap) {
                            var count = stats[col].uniqueMap[v];
                            if(count>stats[col].uniqueMax) {
                                stats[col].uniqueMax =  count;
                                stats[col].uniqueValue  =v ;
                            }
                        }
                    }
                }

                if(this.showStd) {
                    for(var fieldIdx=0;fieldIdx<fields.length;fieldIdx++) {
                        var field = fields[fieldIdx];
                        var col = field.getIndex();
                        var values = stats[col].values;
                        if(values.length>0) {
                            var average = stats[col].total/values.length;
                            var stdTotal = 0;
                            for(var i=0;i<values.length;i++) {
                                var diff = values[i]-average;
                                stdTotal += diff*diff;
                            }
                            var mean = stdTotal/values.length;
                            stats[col].std = Math.sqrt(mean);
                        }
                    }
                }
                var border = (justOne?"0":"1");
                var html = HtmlUtil.openTag("table",["border", border ,"bordercolor","#ccc","class","display-stats","cellspacing","1","cellpadding","5"]);
                var dummy = ["&nbsp;"];
                if(!justOne) {
                    header = [""];
                    if(this.showCount) {
                        header.push("Count");
                        dummy.push("&nbsp;");
                    }
                    if(this.showMin) {
                        header.push("Min");
                        dummy.push("&nbsp;");
                    }
                    if(this.showPercentile) {
                        header.push("25%");
                        dummy.push("&nbsp;");
                        header.push("50%");
                        dummy.push("&nbsp;");
                        header.push("75%");
                        dummy.push("&nbsp;");
                    }
                    if(this.showMax) {
                        header.push("Max");
                        dummy.push("&nbsp;");
                    }
                    if(this.showTotal) {
                        header.push("Total");
                        dummy.push("&nbsp;");
                    }
                    if(this.showAverage) {
                        header.push("Average");
                        dummy.push("&nbsp;");
                    }
                    if(this.showStd) {
                        header.push("Std");
                        dummy.push("&nbsp;");
                    }
                    if(this.showUnique) {
                        header.push("# Unique");
                        dummy.push("&nbsp;");
                        header.push("Top");
                        dummy.push("&nbsp;");
                        header.push("Freq.");
                        dummy.push("&nbsp;");
                    }
                    if(this.showMissing) {
                        header.push("Not&nbsp;Missing");
                        dummy.push("&nbsp;");
                        header.push("Missing");
                        dummy.push("&nbsp;");
                    }
                    if(this.showType) {
                        header.push("Type");
                        dummy.push("&nbsp;");                        
                    }
                    html += HtmlUtil.tr(["valign","bottom"],HtmlUtil.tds(["class","display-stats-header","align","center"],header));
                }
                var cats = [];
                var catMap = {};
                for(var fieldIdx=0;fieldIdx<fields.length;fieldIdx++) {
                    var field = fields[fieldIdx];
                    var col = field.getIndex();
                    var field = allFields[col];
                    var right = "";
                    var total = "&nbsp;";
                    var label = field.getLabel().toLowerCase();
                    var avg  = stats[col].numNotMissing==0?"NA":this.formatNumber(stats[col].total/stats[col].numNotMissing);
                    //Some guess work about when to show a total
                    if(label.indexOf("%")<0 && label.indexOf("percent")<0 && label.indexOf("median")<0) {
                        total  =  this.formatNumber(stats[col].total);
                    } 
                    if(justOne) {
                        right = HtmlUtil.tds(["xalign","right"],[this.formatNumber(stats[col].min)]);
                        continue;
                    } 
                    var values = [];
                    if(!stats[col].isNumber && this.showText) {
                        if(this.showCount)
                            values.push(stats[col].count);
                        if(this.showMin)
                            values.push("-");
                        if(this.showPercentile) {
                            values.push("-");
                            values.push("-");
                            values.push("-");
                        }
                        if(this.showMax)
                            values.push("-");
                        values.push("-");
                        if(this.showAverage) {
                            values.push("-");
                        }
                        if(this.showStd) {
                            values.push("-");
                        }
                        if(this.showUnique) {
                            values.push(stats[col].unique);
                            values.push(stats[col].uniqueValue);
                            values.push(stats[col].uniqueMax);
                        }
                        if(this.showMissing) {
                            values.push(stats[col].numNotMissing);
                            values.push(stats[col].numMissing);
                        }
                    } else {
                        if(this.showCount) {
                            values.push(stats[col].count); 
                        }
                        if(this.showMin) {
                            values.push(this.formatNumber(stats[col].min));
                        }
                        if(this.showPercentile) {
                            var range = stats[col].max-stats[col].min;
                            values.push(this.formatNumber(stats[col].min+range*0.25));
                            values.push(this.formatNumber(stats[col].min+range*0.50));
                            values.push(this.formatNumber(stats[col].min+range*0.75));
                        }
                        if(this.showMax) {
                            values.push(this.formatNumber(stats[col].max));
                        }
                        if(this.showTotal) {
                            values.push(total);
                        }
                        if(this.showAverage) {
                            values.push(avg);
                        }
                        if(this.showStd) { 
                           values.push(this.formatNumber(stats[col].std));
                        }
                        if(this.showUnique) {
                            values.push(stats[col].unique);
                            if(Utils.isNumber(stats[col].uniqueValue)) {
                                values.push(this.formatNumber(stats[col].uniqueValue));
                            } else  {
                                values.push(stats[col].uniqueValue);
                            }
                            values.push(stats[col].uniqueMax);
                        }
                        if(this.showMissing) {
                            values.push(stats[col].numNotMissing);
                            values.push(stats[col].numMissing);
                        }

                    } 
                    if(this.showType) {
                        values.push(stats[col].type);
                    }
                    right = HtmlUtil.tds(["align","right"],values);
                    var align = (justOne?"right":"left");
                    var label = field.getLabel();
                    var toks =  label.split("!!");
                    var title = field.getId();
                    label = toks[toks.length-1];
                    if(justOne) {
                        label +=":";
                    }
                    label = label.replace(/ /g,"&nbsp;")
                    var row =  HtmlUtil.tr([],HtmlUtil.td(["align",align],"<b>" +HtmlUtil.tag("div", ["title",title], label)+"</b>") + right);
                    if(justOne) {
                        html += row;
                    } else {
                        html += row;
                    }
                }
                html += "</table>";
                this.setContents(html);
                this.initTooltip();
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
                this.initTooltip();
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
                else if(this.colorTable && this.colorTable !="none")
                    colors = Utils.ColorTables[this.colorTable];
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
                this.initTooltip();

            },
        });
}



/**
Copyright 2008-2014 Geode Systems LLC
*/



/*
This package supports charting and mapping of georeferenced time series data
*/

var pointDataCache = {};


function DataCollection() {
    RamaddaUtil.defineMembers(this,{ 
            data: [],
            hasData: function() {
                for(var i=0;i<this.data.length;i++) {
                    if(this.data[i].hasData()) return true;
                }
                return false;
            },
            getList: function() {
                return this.data;
            },
            addData: function(data) {
                this.data.push(data);
            },
            handleEventMapClick: function (myDisplay, source, lon, lat) {
                for(var i=0;i<this.data.length;i++ ) {
                    this.data[i].handleEventMapClick(myDisplay, source, lon, lat);
                }

            },


});
    
}

function BasePointData(name, properties) {
    if(properties == null) properties = {};

    RamaddaUtil.defineMembers(this, {
            recordFields : null,
                records : null,
                entryId: null,
                entry: null});

    $.extend(this, properties);

    RamaddaUtil.defineMembers(this, {
            name : name,
            properties : properties,
            initWith : function(thatPointData) {
                this.recordFields = thatPointData.recordFields;
                this.records = thatPointData.records;
            },
            handleEventMapClick: function (myDisplay, source, lon, lat) {
            },
            hasData : function() {
                return this.records!=null;
            },
            clear: function() {
                this.records = null;
                this.recordFields = null;
            },
            getProperties : function() {
                return this.properties;
            },
            getProperty : function(key, dflt) {
                var value = this.properties[key];
                if(value == null) return dflt;
                return value;
            },


            getRecordFields : function() {
                return this.recordFields;
            },
            addRecordField : function(field) {
                this.recordFields.push(field);
            },
            getRecords : function() {
                return this.records;
            },
            getNumericFields : function() {
                var recordFields = this.getRecordFields();
                var numericFields = [];
                for(var i=0;i<recordFields.length;i++) {
                    var field = recordFields[i];
                    if(field.isNumeric) numericFields.push(field);
                }
                return numericFields;
            },
            getChartableFields : function(display) {
                var recordFields = this.getRecordFields();
                var numericFields = [];
                var skip = /(TIME|HOUR|MINUTE|SECOND|YEAR|MONTH|DAY|LATITUDE|LONGITUDE|^ELEVATION$)/g;
                var skip = /(xxxnoskip)/g;
                for(var i=0;i<recordFields.length;i++) {
                    var field = recordFields[i];
                    if(!field.isNumeric || !field.isChartable()) {
                        continue;
                    }
                    var ID = field.getId().toUpperCase() ;
                    if(ID.match(skip)) {
                        continue;
                    }
                    numericFields.push(field);
                }

                return RecordUtil.sort(numericFields);
            },
            getNonGeoFields : function(display) {
                var recordFields = this.getRecordFields();
                var numericFields = [];
                //                var skip = /(TIME|HOUR|MINUTE|SECOND|YEAR|MONTH|DAY|LATITUDE|LONGITUDE|ELEVATION)/g;
                for(var i=0;i<recordFields.length;i++) {
                    var field = recordFields[i];
                    if(field.isFieldGeo()) {
                        continue;
                    }
                    //                    var ID = field.getId().toUpperCase() ;
                    //                    if(ID.match(skip)) {
                    //                        continue;
                    //                    }
                    numericFields.push(field);
                }
                return numericFields;
                //                return RecordUtil.sort(numericFields);
            },

            loadData: function(display) {
            },
            getName : function() {
                return this.name;
            },
            getTitle : function() {
                if(this.records !=null && this.records.length>0)
                    return this.name +" - " + this.records.length +" points";
                return this.name;
            }
        });
}




/*
This encapsulates some instance of point data. 
name - the name of this data
recordFields - array of RecordField objects that define the metadata
data - array of Record objects holding the data
*/
function PointData(name, recordFields, records, url, properties) {
    RamaddaUtil.inherit(this, new  BasePointData(name, properties));
    RamaddaUtil.defineMembers(this, {
            recordFields : recordFields,
            records : records,
            url : url,
            loadingCnt: 0,
            equals : function(that) {
                return this.url == that.url;
            },
            getIsLoading: function() {
                return this.loadingCnt>0;
            },
            handleEventMapClick: function (myDisplay, source, lon, lat) {
                this.lon = lon;
                this.lat = lat;
                if(myDisplay.getDisplayManager().hasGeoMacro(this.url)) {
                    this.loadData(myDisplay, true);
                }
            },
            startLoading: function() {
                this.loadingCnt++;
            },
            stopLoading: function() {
                this.loadingCnt--;
            },
            loadData: function(display, reload) {
                if(this.url==null) {
                    console.log("No URL");
                    return;
                }
                var props = {
                    lat:this.lat,                    
                    lon:this.lon,
                };
                var jsonUrl = display.displayManager.getJsonUrl(this.url, display, props);
                this.loadPointJson(jsonUrl, display, reload);
            },
            loadPointJson: function(url, display, reload) {
                var pointData = this;
                this.startLoading();
                var _this = this;
                var obj = pointDataCache[url];
                if(obj == null) {
                    obj =     {
                        pointData: null,
                        pending: []
                    };
                    //                    console.log("created new obj in cache: " +url);
                    pointDataCache[url] = obj;
                } 
                if(obj.pointData!=null) {
                    //                    console.log("from cache " +url);
                    display.pointDataLoaded(obj.pointData, url, reload);
                    return;
                }
                obj.pending.push(display);
                if(obj.pending.length>1) {
                    //                    console.log("Waiting on callback:" + obj.pending.length +" " + url);
                    return;
                }
                console.log("load data:" + url);
                //                console.log("loading point url:" + url);
                var jqxhr = $.getJSON( url, function(data) {
                        if(GuiUtils.isJsonError(data)) {
                            display.pointDataLoadFailed(data);
                            return;
                        }
                        var newData =    makePointData(data, _this.derived, display);
                        obj.pointData  = pointData.initWith(newData);
                        var tmp = obj.pending;
                        obj.pending = [];
                        for(var i=0;i<tmp.length;i++) {
                            //                            console.log("Calling: " + tmp[i]);
                            tmp[i].pointDataLoaded(pointData, url, reload);
                        }
                        pointData.stopLoading();
                    })
                    .fail(function(jqxhr, textStatus, error) {
                            var err = textStatus + ": " + error;
                            console.log("JSON error:" +err);
                            display.pointDataLoadFailed(err);
                            pointData.stopLoading();
                        });
            }

        });
}


function DerivedPointData(displayManager, name, pointDataList, operation) {
    RamaddaUtil.inherit(this, new  BasePointData(name));
    RamaddaUtil.defineMembers(this, {
            displayManager: displayManager,
            operation: operation,
            pointDataList: pointDataList,
            loadDataCalls: 0,
            display: null,
            pointDataLoaded: function(pointData) {
                this.loadDataCalls--;
                if(this.loadDataCalls<=0) {
                    this.initData();
                }
            },
            equals : function(that) {
                if(that.pointDataList == null) return false;
                if(this.pointDataList.length!=that.pointDataList.length) return false;
                for(var i in this.pointDataList) {
                    if(!this.pointDataList[i].equals(that.pointDataList[i])) {
                        return false;
                    }
                }
                return true;
            },
            initData: function() {
                var pointData1 = this.pointDataList[0];
                if(this.pointDataList.length == 1) {
                    this.records = pointData1.getRecords();
                    this.recordFields = pointData1.getRecordFields();
                } else if(this.pointDataList.length > 1) {
                    var results = this.combineData(pointData1, this.pointDataList[1]);
                    this.records = results.records;
                    this.recordFields = results.recordFields;
                }
                this.display.pointDataLoaded(this);
            },
            combineData: function(pointData1, pointData2) {
                var records1 = pointData1.getRecords();
                var records2 = pointData2.getRecords();
                var newRecords = [];
                var newRecordFields;

                //TODO:  we really need visad here to sample

                if(records1.length!=records2.length) {
                    console.log("bad records:" + records1.length +" " + records2.length);
                }

                if(this.operation == "average") {
                    for(var recordIdx=0;recordIdx<records1.length;recordIdx++) {
                        var record1 = records1[recordIdx];
                        var record2 = records2[recordIdx];
                        if(record1.getDate()!=record2.getDate()) {
                            console.log("Bad record date:" + record1.getDate() + " " + record2.getDate());
                            break;
                        }
                        var newRecord = $.extend(true, {}, record1);
                        var data1 = newRecord.getData();
                        var data2 = record2.getData();
                        for(var colIdx=0;colIdx<data1.length;colIdx++) {
                            data1[colIdx]= (data1[colIdx]+data2[colIdx])/2;
                        }
                        newRecords.push(newRecord);
                    }
                    newRecordFields = pointData1.getRecordFields();
                } else  if(this.operation == "other func") {
                }
                if(newRecordFields==null) {
                    //for now just use the first operand
                    newRecords = records1;
                    newRecordFields = pointData1.getRecordFields();
                }
                return {records: newRecords,
                        recordFields: newRecordFields};
            },
            loadData: function(display) {
                this.display = display;
                this.loadDataCalls=0;
                for(var i in this.pointDataList) {
                    var pointData = this.pointDataList[i];
                    if(!pointData.hasData()) {
                        this.loadDataCalls++;
                        pointData.loadData(this);
                    }
                    if(this.loadDataCalls==0) {
                        this.initData();
                    }
                }
                //TODO: notify display
            }
        });
}





/*
This class defines the metadata for a record column. 
index - the index i the data array
id - string id
label - string label to show to user
type - for now not used but once we have string or other column types we'll need it
missing - the missing value forthis field. Probably not needed and isn't used
as I think RAMADDA passes in NaN
unit - the unit of the value
 */
function RecordField(props) {
    $.extend(this, {
            isDate:props.type == "date",
            isLatitude:false,
            isLongitude:false,
            isElevation:false,
        });
    $.extend(this, props);
    $.extend(this, {
             isNumeric: props.type == "double" || props.type == "integer",
             properties: props
             });
 

   RamaddaUtil.defineMembers(this, {
             getIndex: function() {
                 return this.index;
             },
               isFieldGeo: function() {
               return this.isFieldLatitude() || this.isFieldLongitude() || this.isFieldElevation();
           },
             isFieldLatitude: function() {
                   return this.isLatitude || this.id.toLowerCase() == "latitude";
             },
             isFieldLongitude: function() {
               return this.isLongitude || this.id.toLowerCase() == "longitude";
             },
             isFieldElevation: function() {
               return this.isElevation || this.id.toLowerCase() == "elevation" || this.id.toLowerCase() == "altitude"; 
             },
             isFieldNumeric: function() {
               return this.isNumeric;
             },
             isFieldDate: function() {
               return this.isDate;
             },
             isChartable: function() {
               return this.chartable;
             },
             getSortOrder: function() {
               return this.sortorder;
             },
             getId: function() {
                 return this.id;
             },
             getLabel: function() { 
                if(this.label == null || this.label.length==0) return this.id;
                 return this.label;
             },
               setLabel: function(l) {
               this.label = l;
           },
             getType: function() {
                 return this.type;
             },
             getMissing: function() {
                 return this.missing;
             },
             setUnit: function(u) {
               this.unit = u;
              },
             getUnit: function() {
                 return this.unit;
             }
        });

}


/*
The main data record. This holds a lat/lon/elevation, time and an array of data
The data array corresponds to the RecordField fields
 */
function PointRecord(lat, lon, elevation, time, data) {
    RamaddaUtil.defineMembers(this, {
            latitude : lat,
            longitude : lon,
            elevation : elevation,
            recordTime : time,
            data : data,
            getData : function() {
                return this.data;
            }, 
            allZeros: function() {
                var tuple = this.getData();
                var allZeros = false;
                var nums  = 0;
                var nonZero = 0;
                for(var j=0;j<tuple.length;j++) {
                    if(typeof tuple[j] == "number") {
                        nums++;
                        if(!isNaN(tuple[j]) && tuple[j] != 0) {
                            nonZero ++;
                            break;
                        }
                    }
                }
                if(nums>0 && nonZero == 0) {
                    return true;
                }
                return false;
            },
            getValue : function(index) {
                return this.data[index];
            }, 
            push: function(v) {
                this.data.push(v);
            },
            hasLocation : function() {
                return ! isNaN(this.latitude);
            }, 
            hasElevation : function() {
                return ! isNaN(this.elevation);
            }, 
            getLatitude : function() {
                return this.latitude;
            }, 
            getLongitude : function() {
                return this.longitude;
            }, 
            getTime : function() {
            	return this.time;
            },
            getElevation : function() {
                return this.elevation;
            }, 
            getDate : function() {
                return this.recordTime;
            }
        });
}



function makePointData(json, derived,source) {

    var fields = [];
    var latitudeIdx = -1;
    var longitudeIdx = -1;
    var elevationIdx = -1;
    var dateIdx = -1;

    var lastField = null;
    for(var i=0;i<json.fields.length;i++) {
        var field  = json.fields[i];
        var recordField = new RecordField(field);
        lastField = recordField;
        fields.push(recordField);
        if(recordField.isFieldLatitude()) {
            latitudeIdx = recordField.getIndex();
            //            console.log("Latitude idx:" + latitudeIdx);
        } else if(recordField.isFieldLongitude()) {
            longitudeIdx = recordField.getIndex();
            //            console.log("Longitude idx:" + longitudeIdx);
        } else if(recordField.isFieldElevation()) {
            elevationIdx = recordField.getIndex();
            //            console.log("Elevation idx:" + elevationIdx);
        } else if(recordField.isFieldDate()) {
            dateIdx = recordField.getIndex();
            //            console.log("Date idx:" + dateIdx);
        }

    }


    if(!derived) {
        derived = [
                   //               {'name':'temp_f','label':'Temp F', 'columns':'temperature','function':'v1*9/5+32', 'isRow':true,'decimals':2,},
               //               {'name':'Avg. Temperature','function':'return A.average(5, c1);','columns':'temperature','isColumn',true},
               //               {'name':'max_temp_f','function':'return A.max(c1);','columns':'temp_f'},
               //               {'name':'min_temp_f','function':'return A.min(c1);','columns':'temp_f'},
               //               {'name':'mavg_temp_f_10','function':'return A.movingAverage(10, c1);','columns':'temp_f'},
               //               {'name':'mavg_temp_f_20','function':'return A.movingAverage(20, c1);','columns':'temp_f'},
               ]
        }


    if(derived) {
        var index = lastField.getIndex()+1;
        for(var dIdx=0;dIdx<derived.length;dIdx++) {
            var d = derived[dIdx];
            //            if(!d.isRow) continue;
            var label = d.label;
            if(!label) label = d.name;
            var recordField = new RecordField({
                        type:"double",
                            index: (index+dIdx),
                            chartable: true,
                            id: d.name,
                            label:label,
                });
            recordField.derived= d;
            fields.push(recordField);
        }
    }

    var pointRecords =[];
    var rows = [];

    for(var i=0;i<json.data.length;i++) {
        var tuple = json.data[i];
        var values = tuple.values;
        //lat,lon,alt,time,data values
        var date  = null;
        if ((typeof tuple.date === 'undefined')) {
            if(dateIdx>=0) {
                date = new Date(values[dateIdx]);
            }
        } else {
            if(tuple.date!=null && tuple.date!=0) {
                date = new Date(tuple.date);
            }
        }
        if ((typeof tuple.latitude === 'undefined')) {
            if(latitudeIdx>=0) 
                tuple.latitude = values[latitudeIdx];
            else
                tuple.latitude = NaN;
        }
        if ((typeof tuple.longitude === 'undefined')) {
            if(longitudeIdx>=0) 
                tuple.longitude = values[longitudeIdx];
            else 
                tuple.longitude = NaN;
        }

        if ((typeof tuple.elevation === 'undefined')) {
            if(elevationIdx>=0) 
                tuple.elevation = values[elevationIdx];
            else
                tuple.elevation = NaN;
        }



        if(derived) {
            for(var dIdx=0;dIdx<derived.length;dIdx++) {
                var d = derived[dIdx];
                if(!d.isRow) {
                    continue;
                }
                if(!d.compiledFunction) {
                    var funcParams = [];
                    var params = (d.columns.indexOf(";")>=0?d.columns.split(";"):d.columns.split(","));
                    d.fieldsToUse =  [];
                    for(var i=0;i<params.length;i++) {
                        var param = params[i].trim();
                        funcParams.push("v" +(i+1));
                        var theField = null;
                        for(var fIdx=0;fIdx<fields.length;fIdx++) {
                            var f = fields[fIdx];
                            if(f.getId() == param) {
                                theField = f;
                                break;
                            }
                        }
                        d.fieldsToUse.push(theField);
                      
                    }
                    var code =  "";
                    for(var i=0;i<funcParams.length;i++) {
                        code += "var v" +  (i+1) +"=args[" + i +"];\n";
                    }
                    var tmp = d["function"];
                    if(tmp.indexOf("return")<0) tmp = "return " + tmp;
                    code += tmp +"\n";
                    d.compiledFunction = new Function("args",code);
                    //                    console.log("Func:" + d.compiledFunction);
                }
                //TODO: compile the function once and call it
                var args  = [];

                var anyNotNan  = false;
                for(var fIdx=0;fIdx<d.fieldsToUse.length;fIdx++) {
                    var f = d.fieldsToUse[fIdx];
                    var v = NaN;
                    if(f!=null)  {
                        v = values[f.getIndex()];
                        if(v == null) v = NaN;
                    } 
                    if(!isNaN(v)) {
                        anyNotNan = true;
                    } else {
                    }
                    args.push(v);
                } 
                //                console.log("anyNot:" + anyNotNan);
                //                console.log(args);
                try {
                    var result  = NaN;
                    if(anyNotNan) {
                        result = d.compiledFunction(args);
                        if(d.decimals>=0) {
                            result = result.toFixed(d.decimals);
                        }
                        result = parseFloat(result);
                    } else {
                        //                        console.log("NAN");
                    }
                    //                    console.log("in:" + result +" out: " + result);
                    values.push(result);
                } catch(e) {
                    console.log("Error evaluating function:" + d["function"] + "\n"+e);
                    values.push(NaN);            
                }
            }
        }
        rows.push(values);
        var record = new PointRecord(tuple.latitude, tuple.longitude,tuple.elevation, date, values);
        pointRecords.push(record);
    }



    for(var dIdx=0;dIdx<derived.length;dIdx++) {
        var d = derived[dIdx];
        if(!d.isColumn) continue;
        var f = d["function"];
        var funcParams = [];
        //TODO: allow for no columns and choose all
        var params = d.columns.split(",");
        for(var i=0;i<params.length;i++) {
            var index = -1;
            for(var fIdx=0;fIdx<fields.length;fIdx++) {
                var f = fields[fIdx];
                //                console.log("F:" + f.getId() +" " + f.getLabel() );
                if(f.getId() == params[i] ||f.getLabel() == params[i]) {
                    index = f.getIndex();
                    console.log("index:" + index +" f:" + f.getId());
                    break;
                }
            }
            if(index<0) {
                console.log("Could not find column index for field: " + params[i]);
                continue;
            }
            funcParams.push("c" +(i+1));
        }
        var columnData = RecordUtil.slice(rows, index);
        //        var newData = A.movingAverage(columnData,{step:100});
        var daFunk =  new Function(funcParams, d["function"]);
        console.log("daFunk - "  + daFunk);
        var newData = daFunk(columnData);
        console.log("got new:"+ newData + " " +(typeof newData));
        if((typeof newData) ==  "number") {
            for(var rowIdx=0;rowIdx<pointRecords.length;rowIdx++) {
                var record  = pointRecords[rowIdx];
                record.push(newData);
            }
        } else if(Utils.isDefined(newData.length)) {
            console.log("newData.length:" + newData.length +" records.length:" + pointRecords.length);
            for(var rowIdx=0;rowIdx<newData.length;rowIdx++) {
                var record  = pointRecords[rowIdx];
                if(!record) {
                    console.log("bad record: " + rowIdx);
                    record.push(NaN);
                } else {
                    //                    console.log("    date:" + record.getDate() +" v: " + newData[rowIdx]);
                    var v  = newData[rowIdx];
                    if(d.decimals>=0) {
                        v = parseFloat(v.toFixed(d.decimals));
                    }
                    record.push(v);
                }
            }
        }
    }

    if(source!=null) {
        for(var i=0;i<fields.length;i++) {
            var field  = fields[i];
            var prefix = "field." + field.getId()+".";
            if(Utils.isDefined(source[prefix+"unit"])) {
                field.setUnit(source[prefix+"unit"]);
            }
            if(Utils.isDefined(source[prefix+"label"])) {
                field.setLabel(source[prefix+"label"]);
            }
            if(Utils.isDefined(source[prefix+"scale"])  || Utils.isDefined(source[prefix+"offset1"]) || Utils.isDefined(source[prefix+"offset2"])) {
                var offset1 = Utils.isDefined(source[prefix+"offset1"])?parseFloat(source[prefix+"offset1"]):0;
                var offset2 = Utils.isDefined(source[prefix+"offset2"])?parseFloat(source[prefix+"offset2"]):0;
                var scale = Utils.isDefined(source[prefix+"scale"])?parseFloat(source[prefix+"scale"]):1;
                var index = field.getIndex();
                for(var rowIdx=0;rowIdx<pointRecords.length;rowIdx++) {
                    var record  = pointRecords[rowIdx];
                    var values=  record.getData();
                    var value =  values[index];
                    values[index]= (value+offset1)*scale+offset2;
                }
            } 

        } 
    }



    var name = json.name;
    if ((typeof name === 'undefined')) {
        name =  "Point Data";
    }
    return new  PointData(name,  fields, pointRecords);
}






function makeTestPointData() {
    var json = {
        fields:
        [{index:0,
               id:"field1",
               label:"Field 1",
               type:"double",
               missing:"-999.0",
               unit:"m"},

        {index:1,
               id:"field2",
               label:"Field 2",
               type:"double",
               missing:"-999.0",
               unit:"c"},
            ],
        data: [
               [-64.77,-64.06,45, null,[8.0,1000]],
               [-65.77,-64.06,45, null,[9.0,500]],
               [-65.77,-64.06,45, null,[10.0,250]],
               ]
    };

    return makePointData(json);

}









/*
function InteractiveDataWidget (theChart) {
    this.jsTextArea =  id + "_js_textarea";
    this.jsSubmit =  id + "_js_submit";
    this.jsOutputId =  id + "_js_output";
        var jsInput = "<textarea rows=10 cols=80 id=\"" + this.jsTextArea +"\"/><br><input value=\"Try it out\" type=submit id=\"" + this.jsSubmit +"\">";

        var jsOutput = "<div id=\"" + this.jsOutputId +"\"/>";
        $("#" + this.jsSubmit).button().click(function(event){
                var js = "var chart = ramaddaGlobalChart;\n";
                js += "var data = chart.pointData.getData();\n";
                js += "var fields= chart.pointData.getRecordFields();\n";
                js += "var output= \"#" + theChart.jsOutputId  +"\";\n";
                js += $("#" + theChart.jsTextArea).val();
                eval(js);
            });
        html += "<table width=100%>";
        html += "<tr valign=top><td width=50%>";
        html += jsInput;
        html += "</td><td width=50%>";
        html += jsOutput;
        html += "</td></tr></table>";
*/



function RecordFilter(properties) {
    if(properties == null) properties = {};
    RamaddaUtil.defineMembers(this, {
            properties: properties,
            recordOk:function(display, record, values) {
                return true;
            }
        });
}


function MonthFilter(param) {
    RamaddaUtil.inherit(this,new RecordFilter());
    RamaddaUtil.defineMembers(this,{
            months: param.split(","),
            recordOk: function(display, record, values) {
                for(i in this.months) {
                    var month = this.months[i];
                    var date = record.getDate();
                    if(date == null) return false;
                    if(date.getMonth == null) {
                        //console.log("bad date:" + date);
                        return false;
                    }
                    if(date.getMonth()==month) return true;
                }
                return false;
            }
        });
}


var A = {
    add : function(v1,v2) {
        if(isNaN(v1) || isNaN(v2))  return NaN;
        return v1+v2;
    },

    average : function(values) {
        var sum = 0;
        if(values.length==0) return 0;
        for(var i=0;i<values.length;i++) {
            sum += values[i];
        }
        return sum/values.length;
    },
    percentIncrease : function(values) {
        var percents = [];
        var sum = 0;
        if(values.length==0) return 0;
        var lastValue;
        for(var i=0;i<values.length;i++) {
            var v = values[i];
            var incr = NaN;
            if(i>0 && lastValue!=0) {
                incr = (v-lastValue)/lastValue;
            }
            lastValue = v;
            percents.push(incr*100);
        }
        return percents;
    },
    movingAverage : function(values,props) {
        if(!props) {
            props = {};
        }
        if(!props.step) props.step = 5;
        var newValues = [];
        console.log("STEP:" + props.step);
        for(var i=props.step;i<values.length;i++) {
            var total = 0;
            var cnt = 0;
            for(var j=i-props.step;j<i;j++) {
                if(values[j] == values[j]) {
                    total += values[j];
                    cnt++;
                }
            }
            var value = (cnt>0?total/cnt:NaN);
            if(newValues.length==0) {
                for(var extraIdx=0;extraIdx<props.step;extraIdx++) {
                    newValues.push(value);
                }
            }
            newValues.push(value);
        }

        console.log("MovingAverage: values:" + values.length +" new:" + newValues.length);
        return newValues;
    },
   expMovingAverage : function(values,props) {
        if(!props) {
            props = {};
        }
        if(!props.step) props.step = 5;
        var sma = A.movingAverage(values, props);
        var mult   = (2.0 / (props.step + 1) );
        var newValues = [];
        console.log("STEP:" + props.step);
        for(var i=props.step;i<values.length;i++) {
            var total = 0;
            var cnt = 0;
            for(var j=i-props.step;j<i;j++) {
                if(values[j] == values[j]) {
                    total += values[j];
                    cnt++;
                }
            }
            var value = (cnt>0?total/cnt:NaN);
            if(newValues.length==0) {
                for(var extraIdx=0;extraIdx<props.step;extraIdx++) {
                    newValues.push(value);
                }
            }
            newValues.push(value);
        }

        console.log("MovingAverage: values:" + values.length +" new:" + newValues.length);
        return newValues;
    },

    max : function(values) {
        var max = NaN;
        for(var i=0;i<values.length;i++) {
            if(i==0 || values[i]>max) {
                max = values[i];
            }
        }
        return max;
    },
    min : function(values) {
        var min = NaN;
        for(var i=0;i<values.length;i++) {
            if(i==0 || values[i]<min) {
                min = values[i];
            }
        }
        return min;
    },

}

var RecordUtil = {
    getRanges: function(fields,records) {
        var maxValues = [];
        var minValues = [];
        for(var i=0;i<fields.length;i++) {
            maxValues.push(NaN);
            minValues.push(NaN);
        }

        for(var row=0;row<records.length;row++) {
            for(var col=0;col<fields.length;col++) {
                var value  = records[row].getValue(col);
                if(isNaN(value)) continue;    
                maxValues[col] = (isNaN(maxValues[col])?value:Math.max(value, maxValues[col]));
                minValues[col] = (isNaN(minValues[col])?value:Math.min(value, minValues[col]));
            }
        }

        var ranges = [];
        for(var col=0;col<fields.length;col++) {
            ranges.push([minValues[col],maxValues[col]]);
        }
        return ranges;
    },



    getElevationRange: function(fields,records) {
        var maxValue =NaN;
        var minValue = NaN;

        for(var row=0;row<records.length;row++) {
            if(records[row].hasElevation()) { 
                var value = records[row].getElevation();
                maxValue = (isNaN(maxValue)?value:Math.max(value, maxValue));
                minValue = (isNaN(minValue)?value:Math.min(value, minValue));
            }
        }
        return [minValue,maxValue];
    },


    slice: function(records,index) {
        var values = [];
        for(var rowIdx=0;rowIdx<records.length;rowIdx++) {
            var row = records[rowIdx];
            if(row.getValue) {
                values.push(row.getValue(index));
            } else {
                values.push(row[index]);
            }
        }
        return values;
    },


    sort : function(fields) {
        fields = fields.slice(0);
        fields.sort(function(a,b){
                var s1 = a.getSortOrder();
                var s2 = b.getSortOrder();
                return s1<s2;
            });
        return fields;
    },
    getPoints: function (records, bounds) {
        var points =[];
        var north=NaN,west=NaN,south=NaN,east=NaN;
        if(records!=null) {
            for(j=0;j<records.length;j++) { 
                var record = records[j];
                if(!isNaN(record.getLatitude()) && !isNaN(record.getLongitude())) { 
                    if(j == 0) {
                        north  =  record.getLatitude();
                        south  = record.getLatitude();
                        west  =  record.getLongitude();
                        east  = record.getLongitude();
                    } else {
                        north  = Math.max(north, record.getLatitude());
                        south  = Math.min(south, record.getLatitude());
                        west  = Math.min(west, record.getLongitude());
                        east  = Math.max(east, record.getLongitude());
                    }
                    if(record.getLongitude()<-180 || record.getLatitude()>90) {
                        console.log("Bad index=" + j +" " + record.getLatitude() +" " + record.getLongitude());
                    }
                    points.push(new OpenLayers.Geometry.Point(record.getLongitude(),record.getLatitude()));
                }
            }
        }
        bounds.north = north;
        bounds.west = west;
        bounds.south = south;
        bounds.east = east;
        return points;
    },
    findClosest: function(records, lon, lat, indexObj) {
        if(records == null) return null;
        var closestRecord = null;
        var minDistance = 1000000000;
        var index = -1;
        for(j=0;j<records.length;j++) { 
            var record = records[j];
            if(isNaN(record.getLatitude())) { 
                continue;
            }
            var distance = Math.sqrt((lon-record.getLongitude())*(lon-record.getLongitude()) + (lat-record.getLatitude())*(lat-record.getLatitude()));
            if(distance<minDistance) {
                minDistance = distance;
                closestRecord = record;
                index = j;
            }
        }
        if(indexObj!=null) {
            indexObj.index = index;
        }
        return closestRecord;
    },
    clonePoints: function(points) {
        var result = [];
        for(var i=0;i<points.length;i++) {
            var point = points[i];
            result.push(new OpenLayers.Geometry.Point(point.x,point.y));
        }
        return result;
    }
};

 
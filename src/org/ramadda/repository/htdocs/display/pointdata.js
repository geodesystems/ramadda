/**
   Copyright 2008-2020 Geode Systems LLC
*/

var FILTER_ALL = "-all-";



let pointDataCache = {};

function DataCollection() {
    RamaddaUtil.defineMembers(this, {
        data: [],
        hasData: function() {
            for (var i = 0; i < this.data.length; i++) {
                if (this.data[i].hasData()) return true;
            }
            return false;
        },
        getList: function() {
            return this.data;
        },
	setData: function(data) {
	    this.data = [data];
	},
        addData: function(data) {
            this.data.push(data);
        },
        handleEventMapClick: function(myDisplay, source, lon, lat) {
            var anyHandled = false;
            for (var i = 0; i < this.data.length; i++) {
                if (this.data[i].handleEventMapClick(myDisplay, source, lon, lat)) {
                    anyHandled = true;
                }
            }
            return anyHandled;

        },


    });

}

function BasePointData(name, properties) {
    if (properties == null) properties = {};

    RamaddaUtil.defineMembers(this, {
        recordFields: null,
        records: null,
        entryId: null,
        entry: null
    });

    $.extend(this, properties);

    RamaddaUtil.defineMembers(this, {
        name: name,
        properties: properties,
        initWith: function(thatPointData) {
            this.recordFields = thatPointData.recordFields;
            this.records = thatPointData.records;
            this.setGroupField();
	    return this;
        },
        handleEventMapClick: function(myDisplay, source, lon, lat) {
            return false;
        },
        hasData: function() {
            return this.records != null;
        },
        clear: function() {
            this.records = null;
            this.recordFields = null;
        },
        getProperties: function() {
            return this.properties;
        },
        getProperty: function(key, dflt) {
            var value = this.properties[key];
            if (value == null) return dflt;
            return value;
        },

        getRecordFields: function() {
            return this.recordFields;
        },
        addRecordField: function(field) {
            this.recordFields.push(field);
        },
        getRecords: function() {
            return this.records;
        },
        getNumericFields: function() {
            var recordFields = this.getRecordFields();
            var numericFields = [];
            for (var i = 0; i < recordFields.length; i++) {
                var field = recordFields[i];
                if (field.isNumeric()) numericFields.push(field);
            }
            return numericFields;
        },
        getChartableFields: function(display) {
            var recordFields = this.getRecordFields();
            var numericFields = [];
            var skip = /(TIME|HOUR|MINUTE|SECOND|YEAR|MONTH|DAY|LATITUDE|LONGITUDE|^ELEVATION$)/g;
            var skip = /(xxxnoskip)/g;
            for (var i = 0; i < recordFields.length; i++) {
                var field = recordFields[i];
                if (!field.isNumeric() || !field.isChartable()) {
                    continue;
                }
                var ID = field.getId().toUpperCase();
                if (ID.match(skip)) {
                    continue;
                }
                numericFields.push(field);
            }

            return RecordUtil.sort(numericFields);
        },
        getNonGeoFields: function(display) {
            var recordFields = this.getRecordFields();
            var numericFields = [];
            //                var skip = /(TIME|HOUR|MINUTE|SECOND|YEAR|MONTH|DAY|LATITUDE|LONGITUDE|ELEVATION)/g;
            var hadDate = false;
            for (var i = 0; i < recordFields.length; i++) {
                var field = recordFields[i];
                if (field.isFieldGeo()) {
                    continue;
                }
		//		console.log("F:" + field.getId());
                if (field.isFieldDate()) {
                    if (hadDate && field.getId() == "recordDate") {
                        continue;
                    }
                    hadDate = true;
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

        loadData: function(display) {},
        getName: function() {
            return this.name;
        },
        getTitle: function() {
            if (this.records != null && this.records.length > 0)
                return this.name + " - " + this.records.length + " points";
            return this.name;
        }
    });
}





function convertToPointData(array) {
    var fields = [];
    var records = [];
    var header = array[0];
    var samples = array[1];
    for(var i=0;i<header.length;i++) {
        let label = String(header[i]);
	let id = label.toLowerCase().replace(/[ ., ]+/g,"_");
        let sample =samples[i];
        let tof= typeof sample;
        let type;
        if(tof=="string")
            type = "string";
        else if(tof=="number")
            type = "double";
        else if(sample.getTime)
            type = "date";
        else 
            console.log("Unknown type:" + tof);
        fields.push(new RecordField({
            id:id,
	    index:i,
            label:label,
            type:type,
            chartable:true
        }));
    }
    for(var i=1;i<array.length;i++) {
        records.push(new  PointRecord(fields,NaN, NaN, NaN, null, array[i]));
    }
    return new  PointData("pointdata", fields, records,null,null);
}


/*
  This encapsulates some instance of point data. 
  name - the name of this data
  recordFields - array of RecordField objects that define the metadata
  data - array of Record objects holding the data
*/
function PointData(name, recordFields, records, url, properties) {
    RamaddaUtil.inherit(this, new BasePointData(name, properties));
    RamaddaUtil.defineMembers(this, {
        recordFields: recordFields,
        records: records,
        url: url,
        loadingCnt: 0,
        equals: function(that) {
	    if(this.jsonUrl) {
		return this.jsonUrl == that.jsonUrl;
	    }
            return this.url == that.url;
        },
        getIsLoading: function() {
            return this.loadingCnt > 0;
        },
        handleEventMapClick: function(myDisplay, source, lon, lat) {
            this.lon = lon;
            this.lat = lat;
	    ///repository/grid/json?entryid=3715ca8e-3c42-4105-96b1-da63e3813b3a&location.latitude=0&location.longitude=179.5
	    //	    initiallatitude=40&location.latitude=0&location.longitude=179.5
            if (myDisplay.getDisplayManager().hasGeoMacro(this.url)) {
		console.log("url:" + this.url);
                this.loadData(myDisplay, true);
                return true;
            }
            return false;
        },
        startLoading: function() {
            this.loadingCnt++;
        },
        stopLoading: function() {
            this.loadingCnt--;
        },
        setGroupField: function() {
            if(this.recordFields) {
                for(var i=0;i<this.recordFields.length;i++) {
                    var field = this.recordFields[i];
                    if(field.isFieldGroup()) {
                        this.groupField = field;
                        break;
                    }
                }
            }
        },
        extractGroup: function(group, records) {
            if(!this.groupField) return records;
            var groupData = this.getDataGroups(records);
            if(groupData.length==0) return records;
            if(!group) group = groupData[0];
            return records;
        },
        getDataGroups: function(records) {
            if(!this.groupData) {
                if(!records) return [];
                this.groupData= [];
                var groupField = this.getGroupField();
                if(!groupField) return this.groupData;
                var seen = {};
                for(var i=0;i<records.length;i++) {
                    var record = records[i];
                    var data;
                    if(record.tuple) 
                        data = record.tuple;
                    else if(record.record)
                        data = record.record.getData();
                    console.log("data:" + data);
                    var value = groupField.getValue(data);
                    if(!seen[value]) {
                        seen[value] = true;
                        this.groupData.push(value);
                    }
                }
            }
            console.log(this.groupData);
            return this.groupData;
        },
        getGroupField: function() {
            return this.groupField;
        },
        isGroup: function() {
            return this.getGroupField()!=null;
        },
        loadData: function(display, reload) {
            if (this.url == null) {
                console.log("No URL");
                return;
            }
            var props = {
                lat: this.lat,
                lon: this.lon,
            };
            var jsonUrl = display.displayManager.getJsonUrl(this.url, display, props);
	    this.jsonUrl = jsonUrl;
            this.loadPointJson(jsonUrl, display, reload);
        },
        loadPointJson: function(url, display, reload) {
	    let debug = displayDebug.loadPointJson;
	    let doCache = true;
            let pointData = this;
            this.startLoading();
            var _this = this;
	    if(debug)
		console.log("loadPointJson: "+ display.getId());
            var cacheObject = pointDataCache[url];
	    if(!doCache) cacheObject = null;
            if (cacheObject == null) {
                cacheObject = {
                    pointData: null,
                    pending: [],
		    size:0,
		    url:url,
		    toString:function() {
			return "cache:" + (this.pointData==null?" no data ":" data:" +this.pointData.pdcnt +" " + this.pointData.getRecords().length) +" url:" + this.url;
		    }

                };
		if(debug)
                    console.log("\tcreated new obj in cache: " +url);
                pointDataCache[url] = cacheObject;
            }
            if (cacheObject.pointData != null) {
		if(debug)
                    console.log("\tdata was in cache:" +cacheObject.pointData.getRecords().length+" url:" + url);
                display.pointDataLoaded(cacheObject.pointData, url, reload);
                return;
            }

            cacheObject.pending.push(display);
            if (cacheObject.pending.length > 1) {
		if(debug)
		    console.log("\tWaiting on callback:" + cacheObject.pending.length +" " + url +" d:" + display);
                return;
            }
            var fail = function(jqxhr, textStatus, error) {
                var err = textStatus;
		if(err) {
		    if(error)
			err += ": " + error;
		} else {
		    err = error;
		}
		console.log("Point data load error:" + (err?err:""));
		cacheObject.pending.map(display=>{
                    display.pointDataLoadFailed(err);
		});
		delete pointDataCache[url];
                pointData.stopLoading();
            }

            var success=function(data) {
                if (GuiUtils.isJsonError(data)) {
		    console.log("is error");
		    if(debug)
			console.log("\tloadPointData failed");
                    display.pointDataLoadFailed(data);
                    return;
                }
		if(data.errorcode == "nodata" || !data.fields) {
		    if(debug)
			console.log("\tno data:" + url);
		    let dummy = new PointData("", [],[]);
                    var tmp = cacheObject.pending;
                    cacheObject.pending = [];
                    for (var i = 0; i < tmp.length; i++) {
			tmp[i].handleNoData(dummy);
		    }
		    return;
		}
		if(debug)
		    console.log("\tmaking point data");
		let t1 = new Date();
                var newData = makePointData(data, _this.derived, display);
		let t2 = new Date();
		if(debug)
		    Utils.displayTimes("makePointData",[t1,t2],true);

		if(debug)
		    console.log("\tdone making point data #records:" + newData.getRecords().length);
                pointData = cacheObject.pointData = newData;
		//                cacheObject.pointData = pointData.initWith(newData);
		if(data.properties) {
		    display.applyRequestProperties(data.properties);
		}

		if(debug)
                    console.log("\tpending:" + cacheObject.pending.length);
                var tmp = cacheObject.pending;
                cacheObject.pending = [];
                for (var i = 0; i < tmp.length; i++) {
		    if(debug)
			console.log("\tcalling pointDataLoaded:" + tmp[i].getId() +" #:" + pointData.getRecords().length);
                    tmp[i].pointDataLoaded(pointData, url, reload);
                }

		if(cacheObject.pointData.records && cacheObject.pointData.records.length) {
		    cacheObject.size = cacheObject.pointData.records.length*cacheObject.pointData.records[0].getData().length;
		}

		let size = 0;
		Object.keys(pointDataCache).map(key=>{
		    size+=pointDataCache[key].size;
		});
		if(debug)
		    console.log("\tcache size:" + size);
		//Size is just the number of rows*columns
		if(size>1000000) {
		    Object.keys(pointDataCache).map(key=>{
			if(pointDataCache[key].pending.length==0) {
			    if(debug)
				console.log("\tDeleting from cache:" + key);
			    delete pointDataCache[key];
			}
		    });
		}
                pointData.stopLoading();
	    }
	    let fullUrl = url;
	    if(!fullUrl.startsWith("http")) {
		var base = window.location.protocol + "//" + window.location.host;
		fullUrl = base+fullUrl;
	    }
            console.log("load data:" + fullUrl);
            Utils.doFetch(url, success,fail,"text");
            //var jqxhr = $.getJSON(url, success).fail(fail);
        }

    });
    this.setGroupField();
}


function DerivedPointData(displayManager, name, pointDataList, operation) {
    RamaddaUtil.inherit(this, new BasePointData(name));
    RamaddaUtil.defineMembers(this, {
        displayManager: displayManager,
        operation: operation,
        pointDataList: pointDataList,
        loadDataCalls: 0,
        display: null,
        pointDataLoaded: function(pointData) {
            this.loadDataCalls--;
            if (this.loadDataCalls <= 0) {
                this.initData();
            }
        },
        equals: function(that) {
            if (that.pointDataList == null) return false;
            if (this.pointDataList.length != that.pointDataList.length) return false;
            for (var i in this.pointDataList) {
                if (!this.pointDataList[i].equals(that.pointDataList[i])) {
                    return false;
                }
            }
            return true;
        },
        initData: function() {
            var pointData1 = this.pointDataList[0];
            if (this.pointDataList.length == 1) {
                this.records = pointData1.getRecords();
                this.recordFields = pointData1.getRecordFields();
		console.log("initData:" + this.recordFields.length);
            } else if (this.pointDataList.length > 1) {
                var results = this.combineData(pointData1, this.pointDataList[1]);
                this.records = results.records;
                this.recordFields = results.recordFields;
		console.log("initData 2:" + this.recordFields.length);
            }
            this.setGroupField();
            this.display.pointDataLoaded(this);
        },

        combineData: function(pointData1, pointData2) {
            var records1 = pointData1.getRecords();
            var records2 = pointData2.getRecords();
            var newRecords = [];
            var newRecordFields;

            //TODO:  we really need visad here to sample

            if (records1.length != records2.length) {
                console.log("bad records:" + records1.length + " " + records2.length);
            }

            if (this.operation == "average") {
                for (var recordIdx = 0; recordIdx < records1.length; recordIdx++) {
                    var record1 = records1[recordIdx];
                    var record2 = records2[recordIdx];
                    if (record1.getDate() != record2.getDate()) {
                        console.log("Bad record date:" + record1.getDate() + " " + record2.getDate());
                        break;
                    }
                    var newRecord = $.extend(true, {}, record1);
                    var data1 = newRecord.getData();
                    var data2 = record2.getData();
                    for (var colIdx = 0; colIdx < data1.length; colIdx++) {
                        data1[colIdx] = (data1[colIdx] + data2[colIdx]) / 2;
                    }
                    newRecords.push(newRecord);
                }
                newRecordFields = pointData1.getRecordFields();
            } else if (this.operation == "other func") {}
            if (newRecordFields == null) {
                //for now just use the first operand
                newRecords = records1;
                newRecordFields = pointData1.getRecordFields();
            }
            return {
                records: newRecords,
                recordFields: newRecordFields
            };
        },
        loadData: function(display) {
            this.display = display;
            this.loadDataCalls = 0;
            for (var i in this.pointDataList) {
                var pointData = this.pointDataList[i];
                if (!pointData.hasData()) {
                    this.loadDataCalls++;
                    pointData.loadData(this);
                }
                if (this.loadDataCalls == 0) {
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
        isDate: props.type == "date",
        isLatitude: false,
        isLongitude: false,
        isElevation: false,
    });
    $.extend(this, props);
    $.extend(this, {
        isGroup:props.group,
        properties: props
    });

    RamaddaUtil.defineMembers(this, {
	clone: function() {
	    var newField = {};
	    $.extend(newField,this);
	    return newField;
	},
	toString: function() {
	    return this.getId();
	    return "Field:" + this.getId() +" label:" + this.getLabel() +" type:" + this.getType()+" " + this.isNumeric();
	},
        getIndex: function() {
            return this.index;
        },
        getValue: function(row) {
            return row[this.index];
        },
        getEnumeratedValues: function(row) {
	    return this.enumeratedValues;
	},
        isFieldGroup: function() {
            return this.isGroup;
        },
	isRecordDate: function() {
	    return this.getId()=="recordDate";
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
            return this.isNumeric();
        },
        isFieldString: function() {
            return this.type == "string" || this.type == "enumeration";
        },
        isFieldEnumeration: function() {
            return this.type == "enumeration";
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
        getTypeLabel: function() {
            var type = "fa-font";
            if(this.isFieldGeo()) {
                type="fa-globe";
            } else if(this.isFieldNumeric()) {
                type="fa-hashtag";
            } else if(this.isFieldEnumeration()) {
                type="fa-list";
            }
            var tt = this.getType();
            return  HtmlUtils.span(["title",tt,"class","fa " +type,"style","color:rgb(169, 169, 169);font-size:12pt;"]);
        },
        getUnitLabel: function() {
            var label = this.getLabel();
            if (this.unit && this.unit != "")
                label = label + " [" + this.unit + "]";
            return label;
        },

        getUnitSuffix: function() {
            if (this.unit && this.unit != "")
                return "&nbsp;[" + this.unit + "]";
            return "";
        },

        getLabel: function() {
            if (this.label == null || this.label.length == 0) return this.id;
            return this.label;
        },
        setLabel: function(l) {
            this.label = l;
        },
        isNumeric: function() {
	    return this.type == "double" || this.type == "integer";
	},
	isString: function() {
	    return this.type == "string" || this.type=="enumeration" || this.type =="url" || this.type == "image";
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
function PointRecord(fields,lat, lon, elevation, time, data) {
    this.isPointRecord = true;
    $.extend(this, {
	fields:fields,
        latitude: lat,
        longitude: lon,
        elevation: elevation,
        recordTime: time,
        data: data,
	id: HtmlUtils.getUniqueId(),
    });
}


PointRecord.prototype =  {
    clone: function() {
	    var newRecord = {};
	    $.extend(newRecord,this);
	    newRecord.data = [];
	    this.data.map((v,idx)=>{newRecord.data[idx] = v;});
	    return newRecord;
	},
	isHighlight: function(display) {
	    if(!this.highlightForDisplay) this.highlightForDisplay={};
	    return this.highlightForDisplay[display];
	},
    getDisplayProperty: function(display,prop,dflt) {
	if(!this.displayProperties) this.displayProperties={};
	let props = this.displayProperties[display];
	if(!props) {
	    props = this.displayProperties[display] = {};
	}
	return props[prop] || dflt;
    },
    setDisplayProperty: function(display,prop,value) {
	if(!this.displayProperties) this.displayProperties={};
	let props = this.displayProperties[display];
	if(!props) {
	    props = this.displayProperties[display] = {};
	}
	props[prop] =value;
    },

	
	setHighlight:function(display, value) {
	    if(!this.highlightForDisplay) this.highlightForDisplay={};
	    if(!value || this.highlightForDisplay[display] == null) {
		this.highlightForDisplay[display] = value;
	    }
	},
	clearHighlight:function(display) {
	    if(!this.highlightForDisplay) this.highlightForDisplay={};
	    delete this.highlightForDisplay[display];
	},
	toString: function() {
	    return "data:"  + this.data;
	},
	getId: function() {
	    return this.id;
	},
        getData: function() {
            return this.data;
        },
        allZeros: function() {
            var tuple = this.getData();
            var allZeros = false;
            var nums = 0;
            var nonZero = 0;
            for (var j = 0; j < tuple.length; j++) {
                if (typeof tuple[j] == "number") {
                    nums++;
                    if (!isNaN(tuple[j]) && tuple[j] != 0) {
                        nonZero++;
                        break;
                    }
                }
            }
            if (nums > 0 && nonZero == 0) {
                return true;
            }
            return false;
        },
        getValue: function(index) {
            return this.data[index];
        },
	setValue: function(index,value) {
            this.data[index] = value;
        },
        push: function(v) {
            this.data.push(v);
        },
        hasDate: function() {
	    return this.getDate()!=null;
	},
        hasLocation: function() {
            return this.latitude !=null && !isNaN(this.latitude);
        },
        hasElevation: function() {
            return this.elevation !=null && !isNaN(this.elevation);
        },
        getLatitude: function() {
            return this.latitude;
        },
        getLongitude: function() {
            return this.longitude;
        },
        getTime: function() {
            return this.recordTime;
        },
        getElevation: function() {
            return this.elevation;
        },
        getDate: function() {
            return this.recordTime;
        }
};


function makePointData(json, derived, source) {
    let debug  =false;
    if(debug) console.log("makePointData");
    var fields = [];
    var latitudeIdx = -1;
    var longitudeIdx = -1;
    var elevationIdx = -1;
    var dateIdx = -1;
    var dateIndexes = [];

    var offsetFields = [];
    var lastField = null;
    for (var i = 0; i < json.fields.length; i++) {
        var field = json.fields[i];
        var recordField = new RecordField(field);
        if (recordField.isFieldNumeric()) {
            if (source.getProperty) {
                var offset1 = source.getProperty(recordField.getId() + ".offset1", source.getProperty("offset1"));
                var offset2 = source.getProperty(recordField.getId() + ".offset2", source.getProperty("offset2"));
                var scale = source.getProperty(recordField.getId() + ".scale", source.getProperty("scale"));

                if (offset1 || offset2 || scale) {
                    var unit = source.getProperty(recordField.getId() + ".unit", source.getProperty("unit"));
                    if (unit) {
                        recordField.unit = unit;
                    }
                    var o = {
                        offset1: 0,
                        offset2: 0,
                        scale: 1
                    };
                    if (offset1) o.offset1 = parseFloat(offset1);
                    if (offset2) o.offset2 = parseFloat(offset2);
                    if (scale) o.scale = parseFloat(scale);
                    recordField.offset = o;
                    offsetFields.push(recordField);
                }
            }
        }


        lastField = recordField;
        fields.push(recordField);
        //        console.log("field:" + recordField.getId());
        if (recordField.isFieldLatitude()) {
            latitudeIdx = recordField.getIndex();
        } else if (recordField.isFieldLongitude()) {
            longitudeIdx = recordField.getIndex();
            //            console.log("Longitude idx:" + longitudeIdx);
        } else if (recordField.isFieldElevation()) {
            elevationIdx = recordField.getIndex();
            //            console.log("Elevation idx:" + elevationIdx);
        } else if (recordField.isFieldDate()) {
	    dateIdx = recordField.getIndex();
	    dateIndexes.push(dateIdx);
        }

    }

    if (derived) {
        var index = lastField.getIndex() + 1;
        for (var dIdx = 0; dIdx < derived.length; dIdx++) {
            var d = derived[dIdx];
            var label = d.label || d.name;
            var recordField = new RecordField({
                type: "double",
                index: (index + dIdx),
                chartable: true,
                id: d.name,
                label: label,
            });
            recordField.derived = d;
            fields.push(recordField);
        }
    }

    let pointRecords = [];
    let isArray = false;
    let hasGeo = false;
    json.data.forEach((tuple,i)=>{
	if(debug && i>0 && (i%10000)==0) console.log("\tprocessed:" + i);
	if(i==0) {
	    isArray = Array.isArray(tuple);
	}
	let values;
	if(isArray)
	    values = tuple;
	else
            values = tuple.values;
        //lat,lon,alt,time,data values
        let date = null;
        if (isArray || (typeof tuple.date === 'undefined')) {
            if (dateIdx >= 0) {
                date = new Date(values[dateIdx]);
            }
        } else {
            if (tuple.date != null && tuple.date != 0) {
                date = new Date(tuple.date);
            }
        }
        if (isArray || (typeof tuple.latitude === 'undefined')) {
            if (latitudeIdx >= 0)
                tuple.latitude = values[latitudeIdx];
            else
                tuple.latitude = NaN;
        }
        if (isArray || (typeof tuple.longitude === 'undefined')) {
            if (longitudeIdx >= 0)
                tuple.longitude = values[longitudeIdx];
            else
                tuple.longitude = NaN;
        }
        for (var j = 0; j < dateIndexes.length; j++) {
            values[dateIndexes[j]] = new Date(values[dateIndexes[j]]);
        }
        for (var col = 0; col < values.length; col++) {
            if(values[col]==null) {
                values[col] = NaN;
            } 
        }

        if (derived) {
            for (var dIdx = 0; dIdx < derived.length; dIdx++) {
                var d = derived[dIdx];
                if (!d.isRow) {
                    continue;
                }
                if (!d.compiledFunction) {
                    var funcParams = [];
                    var params = (d.columns.indexOf(";") >= 0 ? d.columns.split(";") : d.columns.split(","));
                    d.fieldsToUse = [];
                    for (var i = 0; i < params.length; i++) {
                        var param = params[i].trim();
                        funcParams.push("v" + (i + 1));
                        var theField = null;
                        for (var fIdx = 0; fIdx < fields.length; fIdx++) {
                            var f = fields[fIdx];
                            if (f.getId() == param) {
                                theField = f;
                                break;
                            }
                        }
                        d.fieldsToUse.push(theField);

                    }
                    var code = "";
                    for (var i = 0; i < funcParams.length; i++) {
                        code += "var v" + (i + 1) + "=args[" + i + "];\n";
                    }
                    var tmp = d["function"];
                    if (tmp.indexOf("return") < 0) tmp = "return " + tmp;
                    code += tmp + "\n";
                    d.compiledFunction = new Function("args", code);
                    //                    console.log("Func:" + d.compiledFunction);
                }
                //TODO: compile the function once and call it
                var args = [];
                var anyNotNan = false;
                for (var fIdx = 0; fIdx < d.fieldsToUse.length; fIdx++) {
                    var f = d.fieldsToUse[fIdx];
                    var v = NaN;
                    if (f != null) {
                        v = values[f.getIndex()];
                        if (v == null) v = NaN;
                    }
                    if (!isNaN(v)) {
                        anyNotNan = true;
                    } else {}
                    args.push(v);
                }
                //                console.log("anyNot:" + anyNotNan);
                //                console.log(args);
                try {
                    var result = NaN;
                    if (anyNotNan) {
                        result = d.compiledFunction(args);
                        if (d.decimals >= 0) {
                            result = result.toFixed(d.decimals);
                        }
                        result = parseFloat(result);
                    } else {
                        //                        console.log("NAN");
                    }
                    //                    console.log("in:" + result +" out: " + result);
                    values.push(result);
                } catch (e) {
                    console.log("Error evaluating function:" + d["function"] + "\n" + e);
                    values.push(NaN);
                }
            }
	}

        for (var fieldIdx = 0; fieldIdx < offsetFields.length; fieldIdx++) {
            var field = offsetFields[fieldIdx];
            var offset = field.offset;
            var value = values[field.getIndex()];
            value = (value + offset.offset1) * offset.scale + offset.offset2;
            values[field.getIndex()] = value;
        }
        var record = new PointRecord(fields, tuple.latitude, tuple.longitude, tuple.elevation, date, values);
        pointRecords.push(record);
    });


    if (source != null) {
        for (var i = 0; i < fields.length; i++) {
            var field = fields[i];
            var prefix = "field." + field.getId() + ".";
            if (Utils.isDefined(source[prefix + "unit"])) {
                field.setUnit(source[prefix + "unit"]);
            }
            if (Utils.isDefined(source[prefix + "label"])) {
                field.setLabel(source[prefix + "label"]);
            }
            if (Utils.isDefined(source[prefix + "scale"]) || Utils.isDefined(source[prefix + "offset1"]) || Utils.isDefined(source[prefix + "offset2"])) {
                var offset1 = Utils.isDefined(source[prefix + "offset1"]) ? parseFloat(source[prefix + "offset1"]) : 0;
                var offset2 = Utils.isDefined(source[prefix + "offset2"]) ? parseFloat(source[prefix + "offset2"]) : 0;
                var scale = Utils.isDefined(source[prefix + "scale"]) ? parseFloat(source[prefix + "scale"]) : 1;
                var index = field.getIndex();
                for (var rowIdx = 0; rowIdx < pointRecords.length; rowIdx++) {
                    var record = pointRecords[rowIdx];
                    var values = record.getData();
                    var value = values[index];
                    values[index] = (value + offset1) * scale + offset2;
                }
            }

        }
    }

    var name = json.name;
    if ((typeof name === 'undefined')) {
        name = "Point Data";
    }

    pointRecords.sort(function(a, b) {
        if (a.getDate() && b.getDate()) {
            if (a.getDate().getTime() < b.getDate().getTime()) return -1;
            if (a.getDate().getTime() > b.getDate().getTime()) return 1;
            return 0;
        }
    });

    let pd =  new PointData(name, fields, pointRecords);
    return pd;
}



function BaseFilter(display,properties) {
    this.display = display;
    if (properties == null) properties = {};
    RamaddaUtil.defineMembers(this, {
        properties: properties,
	isEnabled: function() {
	    return true;
	},
	prepareToFilter: function() {
	},
        isRecordOk: function(display, record, values) {
            return true;
        },
	getWidget: function() {return ""}
    });
}



function BoundsFilter(display, properties) {
    RamaddaUtil.inherit(this, new BaseFilter(display, properties));
    $.extend(this, {
	isRecordOk: function(record) {
	    if(this.display.filterBounds && record.hasLocation()) {
		var b = this.display.filterBounds;
		var lat = record.getLatitude();
		var lon = record.getLongitude();
		if(lat>b.top || lat<b.bottom || lon <b.left || lon>b.right)
		    return false;
	    }
            return true;
	},
    });
}


function RecordFilter(display,filterFieldId, properties) {
    let filterField = display.getFieldById(null, filterFieldId);
    RamaddaUtil.inherit(this, new BaseFilter(display, properties));
    this.id = filterFieldId;
    $.extend(this, {
	field: filterField,
	values:[],
	hideFilterWidget: display.getProperty("hideFilterWidget",false, true),
	displayType:display.getProperty(filterField +".filterDisplay","menu"),
	label:   filterField==null?"":display.getProperty(filterField.getId()+".filterLabel",filterField.getLabel()+":"),
	suffix:  filterField==null?"":display.getProperty(filterField.getId()+".filterSuffix",""),
	dateIds: [],
	prefix:display.getProperty(this.id +".filterPrefix"),
	suffix:display.getProperty(this.id +".filterSuffix"),
	startsWith:display.getProperty(this.id +".filterStartsWith",false)
    });

    RamaddaUtil.defineMembers(this, {
	
	toString:function() {
	    return "filter:" + filterFieldId;
	},
	getField: function() {
	    return this.field;
	},
	getFilterId: function(id) {
	    return  this.display.getDomId("filterby_" + id);
	},
	isEnabled: function() {
	    return this.field!=null;
	},
	recordOk: function(display, record, values) {
            return true;
        },
	getProperty: function(key, dflt) {
	    return this.display.getProperty(key, dflt);
	},
	prepareToFilter: function() {
	    if(!this.field) return;
	    //	    if (prefix) pattern = prefix + value;
	    //	    if (suffix) pattern = value + suffix;
	    var value=null;
	    var _values =[];
	    var values=null;
	    var matchers =[];
	    if(this.field.isNumeric()) {
		var minField = $("#" + this.display.getDomId("filterby_" + this.field.getId()+"_min"));
		var maxField = $("#" + this.display.getDomId("filterby_" + this.field.getId()+"_max"));
		var minValue = parseFloat(minField.val().trim());
		var maxValue = parseFloat(maxField.val().trim());
		var dfltMinValue = parseFloat(minField.attr("data-min"));
		var dfltMaxValue = parseFloat(maxField.attr("data-max"));
		if(minValue!= dfltMinValue || maxValue!= dfltMaxValue) {
		    value = [minValue,maxValue];
		}
 	    } else if(this.field.getType()=="date"){
		var date1 = $("#" + this.display.getDomId("filterby_" + this.field.getId()+"_date1")).val();
		var date2 = $("#" + this.display.getDomId("filterby_" + this.field.getId()+"_date2")).val();
		if(date1!=null && date1.trim()!="") 
		    date1 =  Utils.parseDate(date1);
		else
		    date1=null;
		if(date2!=null && date2.trim()!="") 
		    date2 =  Utils.parseDate(date2);
		else
		    date2=null;
		if(date1!=null || date2!=null)
		    value = [date1,date2]; 
	    }  else {
		values = this.getFieldValues();
		if(!values && !Array.isArray(values)) values = [values];
		if(values.length==0) return;
		var tmp = [];
		values.map(v=>{
		    tmp.push(v.replace(/_comma_/g,","));
		});
		values = tmp;
		values.map(v=>{
		    _values.push((""+v).toLowerCase());
		    try {
			matchers.push(new TextMatcher(v));
		    } catch(skipIt){}
		});
	    }
	    var anyValues = value!=null;
	    if(!anyValues && values) {
		values.forEach(v=>{if(v.length>0 && v!= FILTER_ALL)anyValues = true});
	    }
	    if(anyValues) {
		this.mySearch =  {
		    value:value,
		    values:values,
		    matchers:matchers,
		    _values:_values,
		    anyValues:anyValues,
		};
	    } else {
		this.mySearch = null;
	    }
	},
	isRecordOk:function(record) {
	    
	    let ok = true;
	    if(!this.isEnabled() || !this.mySearch) return ok;
	    var rowValue = record.getValue(this.field.getIndex());
	    let filterField = this.field;
	    if(filterField.getType() == "enumeration") {
		ok = this.mySearch.values.includes(""+rowValue);
	    } else if(filterField.isNumeric()) {
		if(isNaN(this.mySearch.value[0]) && isNaN(this.mySearch.value[0])) return ok;
		if(!isNaN(this.mySearch.value[0]) && rowValue<this.mySearch.value[0]) ok = false;
		else if(!isNaN(this.mySearch.value[1]) && rowValue>this.mySearch.value[1]) ok = false;
	    } else if(filterField.getType()=="date"){
		if(this.mySearch.value &&  Array.isArray(this.mySearch.value)) {
		    if(rowValue == null) {
			ok = false;
		    }  else  {
			var date1 = this.mySearch.value[0];
			var date2 = this.mySearch.value[1];
			var dttm = rowValue.getTime();
			if(isNaN(dttm)) ok = false;
			else if(date1 && dttm<date1.getTime())
			    ok = false;
			else if(date2 && dttm>date2.getTime())
			    ok = false;
		    }
		}
	    } else {
		var startsWith = this.startsWith;
		ok = false;
		roWValue  = String(rowValue).toLowerCase();

		for(var j=0;j<this.mySearch._values.length;j++) {
		    var fv = this.mySearch._values[j];
		    if(startsWith) {
			if(rowValue.toString().startsWith(fv)) {
			    ok = true;
			    break;
			}
		    } else  if(rowValue.toString().indexOf(fv)>=0) {
			ok = true;
			break;
		    }
		}
		
		if(!ok && !startsWith) {
		    for(ri=0;ri<this.mySearch.matchers.length;ri++) {
			var matcher = this.mySearch.matchers[ri];
			if(matcher.matches(rowValue.toString())) {
			    ok = true;
			    break;
			}
		    }
		}
	    }
	    return ok;
	},

	getFieldValues: function() {
	    var element =$("#" + this.display.getDomId("filterby_" + this.field.getId()));
	    var value=null;
	    if(element.attr("isCheckbox")) {
		if(element.is(':checked')) {
		    value = element.attr("onValue");
		} else {
		    value = element.attr("offValue");
		}
	    } else if(element.attr("isButton")) {
		value = element.attr("data-value");
	    } else {
		value = element.val();
	    }
	    if(!value) value = FILTER_ALL;
	    if(!Array.isArray(value)) {
		if(!this.field.isFieldEnumeration()) {
		    value = value.split(",");
		} else {
		    value = [value];
		}
	    }
	    var tmp = [];
	    value.forEach(v=>tmp.push(v.trim()));
	    value = tmp;
	    //	    console.log(this.type +".getFilterFieldValues:" + Array.isArray(value) +" " + value.length +" " +value);
	    return value;
	},
	initDateWidget: function(inputFunc) {
	    if(!this.hideFilterWidget) {
		for(var i=0;i<this.dateIds.length;i++) {
		    let id = this.dateIds[i];
		    HtmlUtils.datePickerInit(id);
		    $("#" + id).change(function(){
			inputFunc($(this));
		    });
		}
	    }
	},
	getWidget: function(fieldMap, bottom,records) {
	    let debug = false;
	    if(debug) console.log(this.id +".getWidget");
	    if(!this.isEnabled()) {
		if(debug) console.log("\tnot enabled");
		return "";
	    }
	    let widgetStyle = "";
	    if(this.hideFilterWidget)
		widgetStyle = "display:none;";
	    let filterField = this.field;
	    fieldMap[filterField.getId()] = {
		field: filterField,
		values:[],
	    };
            let widget;
	    let widgetId = this.getFilterId(filterField.getId());
            if(filterField.getType() == "enumeration") {
		if(debug) console.log("\tis enumeration");
		let dfltValue = this.getProperty(filterField.getId() +".filterValue",FILTER_ALL);
		let filterValues = this.getProperty(filterField.getId()+".filterValues");
                let enums = null;
		if (filterValues) {
		    let toks;
		    if ((typeof filterValues) == "string") {
			filterValues = Utils.getMacro(filterValues);
			toks = filterValues.split(",");
		    } else {
			toks = filterValues;
		    }
		    enums=[];
		    toks.map(tok=>{
			let tmp = tok.split(":");
			if(tmp.length>1) {
			    tok = [tmp[0],tmp[1]];
			} else if(tok == FILTER_ALL) {
			    tok = [tmp[0],"All"];
			}
			enums.push({value:tok});
		    })
		}
		let includeAll = this.getProperty(filterField.getId() +".includeAll",true);
		if(enums == null) {
		    let allName = this.getProperty(filterField.getId() +".allName","All");
		    enums = [];
		    if(includeAll) {
			enums.push({value:[FILTER_ALL,allName]});
		    }
		    let seen = {};
		    let dflt = filterField.getEnumeratedValues();
		    if(dflt) {
			for(let v in dflt) {
			    seen[v] = true;
			    enums.push({value:[v,dflt[v]]});
			}
		    }
		    let enumValues = [];
		    let imageField=this.display.getFieldOfType(null, "image");
		    let valuesAreNumbers = true;
		    records.map((record,idx)=>{
			let value = this.display.getDataValues(record)[filterField.getIndex()];
			if(!seen[value]) {
			    seen[value]  = true;
			    let obj = {};
			    if(imageField)
				obj.image = this.display.getDataValues(record)[imageField.getIndex()];
			    if((+value+"") != value) valuesAreNumbers = false;
			    let label = value;
			    if(label.length>30) {
				label=  label.substring(0,29)+"...";
			    }
			    if(typeof value == "string")
				value = value.replace(/\'/g,"&apos;");
			    let tuple = [value, label];
			    obj.value = tuple;
			    enumValues.push(obj);
			}
		    });
		    if(this.getProperty(filterField.getId() +".filterSort",true)) {
			enumValues.sort((a,b)  =>{
			    a= a.value;
			    b = b.value;
			    if(valuesAreNumbers) {
				return +a - +b;
			    }
			    return (""+a[1]).localeCompare(""+b[1]);
			});
		    }
		    for(let j=0;j<enumValues.length;j++) {
			let v = enumValues[j];
			enums.push(v);
		    }
		}
		let attrs= ["style",widgetStyle, "id",widgetId,"fieldId",filterField.getId()];
		if(this.getProperty(filterField.getId() +".filterMultiple",false)) {
		    attrs.push("multiple");
		    attrs.push("");
		    attrs.push("size");
		    attrs.push(this.getProperty(filterField.getId() +".filterMultipleSize","3"));
		    dfltValue = dfltValue.split(",");
		}
		if(this.displayType!="menu") {
		    if(debug) console.log("\tnot menu");
		    if(!includeAll && dfltValue == FILTER_ALL) dfltValue = enums[0].value;
		    let buttons = "";
		    let colorMap = Utils.parseMap(this.getProperty(filterField.getId() +".filterColorByMap"));
		    let useImage = this.displayType == "image";
		    let imageAttrs = [];
		    let imageMap = Utils.getNameValue(this.getProperty(filterField.getId() +".filterImages"));
		    if(useImage) {
			let w = this.getProperty(filterField.getId() +".filterImageWidth");
			let h = this.getProperty(filterField.getId() +".filterImageHeight");
			if(h) {
			    imageAttrs.push("height");
			    imageAttrs.push(h);
			}
			if(w) {
			    imageAttrs.push("width");
			    imageAttrs.push(w);
			}
			if(!h && !w) {
			    imageAttrs.push("width");
			    imageAttrs.push("50");
			}
			
			imageAttrs.push("style");
			imageAttrs.push(this.getProperty(filterField.getId() +".filterImageStyle","border-radius:50%;"));
		    }
		    for(let j=0;j<enums.length;j++) {
			var extra = "";
			var v = enums[j].value;
			var color = colorMap?colorMap[v]:null;
			var label;
			if(Array.isArray(v)) {
			    label = v[1];
			    v = v[0];
			} else {
			    label = v;
			}
			var style = this.getProperty(filterField.getId() +".filterItemStyle","");
			if(color) {
			    style += " background-color:" + color +"; ";
			}
			
			var clazz = " display-filter-item display-filter-item-" + this.displayType +" ";
			if(v == dfltValue) {
			    clazz+=  " display-filter-item-" + this.displayType +"-selected ";
			} else {
			}
			if(v == FILTER_ALL) {
			    extra = " display-filter-item-all ";
			}
			if(useImage) {
			    var image=null;
			    if(imageMap) image = imageMap[v];
			    if(!image || image=="") image = enums[j].image;
			    if(image) {
				buttons+=HtmlUtils.div(["fieldId",filterField.getId(),"class",clazz,"style",style, "data-value",v,"title",label],
						       HtmlUtils.image(image,imageAttrs));
			    } else {
				buttons+=HtmlUtils.div(["fieldId",filterField.getId(),"class",clazz,"style",style,"data-value",v,"title",label],label);
			    }
			} else {
			    buttons+=HtmlUtils.div(["fieldId",filterField.getId(),"class",clazz, "style",style,"data-value",v],label);
			}
			buttons+="\n";
		    }

		    if(useImage && this.getProperty(filterField.getId() +".filterShowButtonsLabel")) {
			buttons+=HtmlUtils.div(["class","display-filter-item-label","id",this.display.getDomId("filterby_" + filterField.getId() +"_label")],"&nbsp;");
		    }
		    bottom[0]+= HtmlUtils.div(["data-value",dfltValue,"class","display-filter-items","id",widgetId,"isButton","true", "fieldId",
					       filterField.getId()], buttons);
		    if(debug) console.log("\treturn 1");
		    return "";
		} else if(this.getProperty(filterField.getId() +".filterCheckbox")) {
		    if(debug) console.log("\tis checkbox");
		    attrs.push("isCheckbox");
		    attrs.push(true);
		    var tmp = [];
		    enums.map(e=>tmp.push(e.value));
		    var checked = tmp.includes(dfltValue);
		    if(tmp.length>0) {
			attrs.push("onValue");
			attrs.push(tmp[0]);
		    }
		    if(tmp.length>1) {
			attrs.push("offValue");
			attrs.push(tmp[1]);
		    }
		    widget = HtmlUtils.checkbox("",attrs,checked);
		    //			    console.log(widget);
		} else {
		    if(debug) console.log("\tis select");
		    var tmp = [];
		    enums.map(e=>tmp.push(e.value));
                    widget = HtmlUtils.select("",attrs,tmp,dfltValue);
		}
	    } else if(filterField.isNumeric()) {
		if(debug) console.log("\tis numeric");
		var min=0;
		var max=0;
		var cnt=0;
		records.map(record=>{
		    var value = this.display.getDataValues(record)[filterField.getIndex()];
		    if(isNaN(value))return;
		    if(cnt==0) {min=value;max=value;}
		    else {
			min = Math.min(min, value);
			max = Math.max(max, value);
		    }
		    cnt++;
		});
		var dfltValueMin = this.getProperty(filterField.getId() +".filterValueMin",min);
		var dfltValueMax = this.getProperty(filterField.getId() +".filterValueMax",max);

                widget = HtmlUtils.input("",dfltValueMin,["data-min",min,"class","display-filter-range display-filter-input","style",widgetStyle, "id",widgetId+"_min","size",3,"fieldId",filterField.getId()]);
		widget += "-";
                widget += HtmlUtils.input("",dfltValueMax,["data-max",max,"class","display-filter-range display-filter-input","style",widgetStyle, "id",widgetId+"_max","size",3,"fieldId",filterField.getId()]);
	    } else if(filterField.getType() == "date") {
                widget =HtmlUtils.datePicker("","",["class","display-filter-input","style",widgetStyle, "id",widgetId+"_date1","fieldId",filterField.getId()]) +"-" +
		    HtmlUtils.datePicker("","",["class","display-filter-input","style",widgetStyle, "id",widgetId+"_date2","fieldId",filterField.getId()]);
		this.dateIds.push(widgetId+"_date1");
		this.dateIds.push(widgetId+"_date2");
            } else {
		var dfltValue = this.getProperty(filterField.getId() +".filterValue","");
		var attrs =["style",widgetStyle, "id",widgetId,"fieldId",filterField.getId(),"class","display-filter-input"];
		var placeholder = this.getProperty(filterField.getId() +".filterPlaceholder");
		if(placeholder) {
		    attrs.push("placeholder");
		    attrs.push(placeholder);
		}
                widget =HtmlUtils.input("",dfltValue,attrs);
		var values=fieldMap[filterField.getId()].values;
		var seen = {};
		records.map(record=>{
		    var value = this.display.getDataValues(record)[filterField.getIndex()];
		    if(!seen[value]) {
			seen[value] = true;
			values.push(value);
		    }	
		});
            }
	    var label =   this.getProperty(filterField.getId()+".filterLabel",filterField.getLabel()+":");
	    var suffix =   this.getProperty(filterField.getId()+".filterSuffix","");
	    if(!this.hideFilterWidget) {
		var tt = label;
		if(label.length>50) label = label.substring(0,49)+"...";
		if(!this.getProperty(filterField.getId() +".showFilterLabel",true)) {
		    label = "";
		}
		else
		    label = label+" ";
		widget = HtmlUtils.div(["style","display:inline-block;"],
				       this.display.makeFilterLabel(label,tt) + widget+suffix);
	    }
            return widget +(this.hideFilterWidget?"":"&nbsp;&nbsp;");
	}
	
    });


    


}




function MonthFilter(param) {
    RamaddaUtil.inherit(this, new BaseFilter());
    RamaddaUtil.defineMembers(this, {
        months: param.split(","),

        recordOk: function(display, record, values) {
            for (i in this.months) {
                var month = this.months[i];
                var date = record.getDate();
                if (date == null) return false;
                if (date.getMonth == null) {
                    //console.log("bad date:" + date);
                    return false;
                }
                if (date.getMonth() == month) return true;
            }
            return false;
        }
    });
}



var A = {
    add: function(v1, v2) {
        if (isNaN(v1) || isNaN(v2)) return NaN;
        return v1 + v2;
    },
    average: function(values) {
        var sum = 0;
        if (values.length == 0) return 0;
        for (var i = 0; i < values.length; i++) {
            sum += values[i];
        }
        return sum / values.length;
    },
    percentIncrease: function(values) {
        var percents = [];
        var sum = 0;
        if (values.length == 0) return 0;
        var lastValue;
        for (var i = 0; i < values.length; i++) {
            var v = values[i];
            var incr = NaN;
            if (i > 0 && lastValue != 0) {
                incr = (v - lastValue) / lastValue;
            }
            lastValue = v;
            percents.push(incr * 100);
        }
        return percents;
    },
    movingAverage: function(values, props) {
        if (!props) {
            props = {};
        }
        if (!props.step) props.step = 5;
        var newValues = [];
        console.log("STEP:" + props.step);
        for (var i = props.step; i < values.length; i++) {
            var total = 0;
            var cnt = 0;
            for (var j = i - props.step; j < i; j++) {
                if (values[j] == values[j]) {
                    total += values[j];
                    cnt++;
                }
            }
            var value = (cnt > 0 ? total / cnt : NaN);
            if (newValues.length == 0) {
                for (var extraIdx = 0; extraIdx < props.step; extraIdx++) {
                    newValues.push(value);
                }
            }
            newValues.push(value);
        }

        console.log("MovingAverage: values:" + values.length + " new:" + newValues.length);
        return newValues;
    },
    expMovingAverage: function(values, props) {
        if (!props) {
            props = {};
        }
        if (!props.step) props.step = 5;
        var sma = A.movingAverage(values, props);
        var mult = (2.0 / (props.step + 1));
        var newValues = [];
        console.log("STEP:" + props.step);
        for (var i = props.step; i < values.length; i++) {
            var total = 0;
            var cnt = 0;
            for (var j = i - props.step; j < i; j++) {
                if (values[j] == values[j]) {
                    total += values[j];
                    cnt++;
                }
            }
            var value = (cnt > 0 ? total / cnt : NaN);
            if (newValues.length == 0) {
                for (var extraIdx = 0; extraIdx < props.step; extraIdx++) {
                    newValues.push(value);
                }
            }
            newValues.push(value);
        }

        console.log("MovingAverage: values:" + values.length + " new:" + newValues.length);
        return newValues;
    },

    max: function(values) {
        var max = NaN;
        for (var i = 0; i < values.length; i++) {
            if (i == 0 || values[i] > max) {
                max = values[i];
            }
        }
        return max;
    },
    min: function(values) {
        var min = NaN;
        for (var i = 0; i < values.length; i++) {
            if (i == 0 || values[i] < min) {
                min = values[i];
            }
        }
        return min;
    },

}

var RecordUtil = {
    groupBy:function(records, display, dateBin, field) {
	let debug = displayDebug.groupBy;
	if(debug) console.log("groupBy");
	let groups ={
	    max:0,
	    values:[],
	    labels:[],
	    map:{},
	}


	records.forEach((r,idx)=>{
	    let key;
	    let label = null;
	    let date = r.getDate();
	    //	    if(debug && idx>0 && (idx%10000)==0) console.log("\trecord:" + idx);
	    if(field) {
		if(field=="latlon") {
		    key = label = r.getLatitude() +"/" + r.getLongitude(); 
		} else {
		    key = label = r.getValue(field.getIndex());
		}
	    } else {
		if(!date) {
		    return;
		}
		key = date;
		if(dateBin===true) {
		    //do the label later
		} else {
		    if(dateBin=="day") {
			key = new Date(label=date.getFullYear()+"-" + date.getUTCMonth() +"-" +date.getUTCDay())
		    } else if(dateBin=="month") {
			label=date.getFullYear()+"-" + date.getUTCMonth();
			key = new Date(label +"-01");
		    } else if(dateBin=="year") {
			label = date.getFullYear();
			key = new Date(date.getFullYear()+"-01-01");
		    } else if(dateBin=="decade") {
			let year = date.getFullYear();
			year = year-year%10;
			label = year+"s";
			key = new Date(year+"-01-01");
		    } else if(dateBin) {
			label = String(key);
		    }
		}
	    }
	    let array = groups.map[key];
	    if(!array) {
		if(debug) console.log("\tadding group:"  + key);
		array = groups.map[key] = [];
		groups.values.push(key);
		if(label==null)
		    label = display.formatDate(date,null,true);
		groups.labels.push(label);
	    }
	    array.push(r);
	    groups.max = Math.max(groups.max, array.length);
	});
	return groups;
    },
    expandBounds: function(bounds, perc) {
	return {
	    east: Math.min(180,bounds.east +(bounds.east-bounds.west)*perc),
	    west: Math.max(-180, bounds.west -(bounds.east-bounds.west)*perc),
	    north: Math.min(90,bounds.north +(bounds.north-bounds.south)*perc),
	    south: Math.max(-90,bounds.south -(bounds.north-bounds.south)*perc),
	}
    },
    convertBounds: function(bounds) {
	if(!bounds) return null;
	if(Utils.isDefined(bounds.top)) 
	    return  {north:bounds.top,west:bounds.left,south:bounds.bottom,east:bounds.right};
	return bounds;
    },
    subset:function(records,bounds) {
	bounds = RecordUtil.convertBounds(bounds);
	//	console.log("subset:" + JSON.stringify(bounds));
	let cnt = 0;
	records =  records.filter((record,idx)=>{
	    let lat = record.getLatitude?record.getLatitude():record.r?record.r.getLatitude():record.y;
	    let lon = record.getLongitude?record.getLongitude():record.r?record.r.getLongitude():record.x;
	    let ok =   lat<= bounds.north &&
		lat>= bounds.south &&
		lon>= bounds.west &&
		lon<= bounds.east;
	    return ok;
	});
	return records;
    },
    gridPoints: function(rows,cols,points,args) {
	let debug = displayDebug.gridPoints;
	let values = [];
	for(var rowIdx=0;rowIdx<rows;rowIdx++)  {
	    let row = [];
	    values.push(row);
	    for(var colIdx=0;colIdx<cols;colIdx++)  {
		row.push({v:NaN,count:0,total:0,min:NaN,max:NaN,t:""});
	    }
	}

	points.map((p,idx)=>{
	    let cell = values[p.y][p.x];
	    cell.min = cell.count==0?p.v:Math.min(cell.min,p.v);
	    cell.max = cell.count==0?p.v:Math.max(cell.max,p.v);
	    cell.count++;
	    cell.total += p.value;
	});

	let minValue = NaN;
	let maxValue = NaN;
	let maxCount=0;
	let minCount=0;

	for(var rowIdx=0;rowIdx<rows;rowIdx++)  {
	    for(var colIdx=0;colIdx<cols;colIdx++)  {
		let cell = values[rowIdx][colIdx];
		if(cell.count==0) continue;
		let v;
		if(args.operator=="count")
		    v = cell.count;
		else if(args.operator=="min")
		    v =  cell.min;
		else if(args.operator=="max")
		    v =  cell.max;
		else if(args.operator=="total")
		    v =  cell.total;
		else
		    v =  cell.total/cell.count;
		cell.v = v;
		if(!isNaN(v)) {
		    minValue = isNaN(minValue)?v:Math.min(minValue,v);
		    maxValue = isNaN(maxValue)?v:Math.max(maxValue,v);
		}
		maxCount = Math.max(maxCount, cell.count);
		minCount = minCount==0?cell.count:Math.min(minCount, cell.count);
	    }
	}	
	if(debug)
	    console.log("operator:" + args.operator +" values:" + minValue +" - " + maxValue +" counts:" + minCount +" - " + maxCount);
	values.minValue = minValue;
	values.maxValue = maxValue;
	values.minCount = minCount;
	values.maxCount = maxCount;
	return values;
    },
    draw3DRect:function(canvas,ctx,x,y,width, height, depth) {
	// Dimetric projection functions
	var dimetricTx = function(x,y,z) { return x + z/2; };
	var dimetricTy = function(x,y,z) { return y + z/4; };
	
	// Isometric projection functions
	var isometricTx = function(x,y,z) { return (x -z) * Math.cos(Math.PI/6); };
	var isometricTy = function(x,y,z) { return y + (x+z) * Math.sin(Math.PI/6); };
	
	var drawPoly = (function(ctx,tx,ty) {
	    return function() {
		var args = Array.prototype.slice.call(arguments, 0);
		// Begin the path
		ctx.beginPath();
		// Move to the first point
		var p = args.pop();
		if(p) {
		    ctx.moveTo(tx.apply(undefined, p), ty.apply(undefined, p));
		}
		// Draw to the next point
		while((p = args.pop()) !== undefined) {
		    ctx.lineTo(tx.apply(undefined, p), ty.apply(undefined, p));
		}
		ctx.closePath();
		ctx.stroke();
		ctx.fill();
	    };
	})(ctx, dimetricTx, dimetricTy);
	
	// Set some context
	ctx.save();
	ctx.scale(1,-1);
	ctx.translate(0,-canvas.height);
	ctx.save();
	
	// Move our graph
	ctx.translate(x,y);  
	// Draw the "container"
	//back
	let  baseColor = ctx.fillStyle;
	//		drawPoly([0,0,depth],[0,height,depth],[width,height,depth],[width,0,depth]);
	//left
	//		drawPoly([0,0,0],[0,0,depth],[0,height,depth],[0,height,0]);
	//right
	ctx.fillStyle =    Utils.pSBC(-0.5,baseColor);
	drawPoly([width,0,0],[width,0,depth],[width,height,depth],[width,height,0]);
	ctx.fillStyle =    baseColor;
	//front
	drawPoly([0,0,0],[0,height,0],[width,height,0],[width,0,0]);
	//top		
	ctx.fillStyle =    Utils.pSBC(0.5,baseColor);
	drawPoly([0,height,0],[0,height,depth],[width,height,depth],[width,height,0]);
	ctx.fillStyle =    baseColor;
	ctx.restore();
	ctx.restore();
    },


    xcnt:0,
    drawArrow:function(context, fromx, fromy, tox, toy,headlen) {
	let dx = tox - fromx;
	let dy = toy - fromy;
	let angle = Math.atan2(dy, dx);
	context.moveTo(fromx, fromy);
	context.lineTo(tox, toy);
	context.lineTo(tox - headlen * Math.cos(angle - Math.PI / 6), toy - headlen * Math.sin(angle - Math.PI / 6));
	context.moveTo(tox, toy);
	context.lineTo(tox - headlen * Math.cos(angle + Math.PI / 6), toy - headlen * Math.sin(angle + Math.PI / 6));
    },
    


    drawGridCell: function(opts, canvas, ctx, x,y,v,colIdx,rowIdx, cell,grid,alphaByCount) {
	let c =  opts.color|| "#ccc";
	let perc = 1.0;
	if(opts.colorBy && v) {
	    perc = opts.colorBy.getValuePercent(v);
	    if(opts.colorBy.index>=0) {
		c=  opts.colorBy.getColor(v);
	    }
	}
	if(alphaByCount && cell && grid) {
	    if(grid.maxCount!=grid.minCount) {
		let countPerc = (cell.count-grid.minCount)/(grid.maxCount-grid.minCount);
		c = Utils.addAlphaToColor(c,countPerc);
	    }
	}
	ctx.fillStyle =c
	ctx.strokeStyle =c
	if(opts.shape == "circle") {
	    ctx.beginPath();
	    ctx.arc(x,y, opts.cellSize, 0, 2 * Math.PI);
	    if(opts.stroke)
		ctx.stroke();
	    else
		ctx.fill();
	} else if(opts.shape == "vector") {
	    let length = opts.cellSizeH;
	    if(opts.lengthBy && opts.lengthBy.index>=0) {
		length = opts.lengthBy.scaleToValue(v);
	    }
	    let x2=x+length;
	    let y2=y;
	    let arrowLength = opts.display.getProperty("arrowLength",-1);
	    /*
	      if(opts.angleBy && opts.angleBy.index>=0) {
	      let perc = opts.angleBy.getValuePercent(v);
	      let degrees = (360*perc);
	      let rads = degrees * (Math.PI/360);
	      x2 = length*Math.cos(rads)-0* Math.sin(rads);
	      y2 = 0*Math.cos(rads)-length* Math.sin(rads);
	      x2+=x;
	      y2+=y;
	      }
	    */
	    if(opts.colorBy && opts.colorBy.index>=0) {
                let perc = opts.colorBy.getValuePercent(v);
                let degrees = (180*perc)+90;
		degrees = degrees*(Math.PI / 360)
                x2 = length*Math.cos(degrees)-0* Math.sin(degrees);
		y2 = 0*Math.cos(degrees)-length* Math.sin(degrees);
                x2+=x;
                y2+=y;
            }
	    //Draw the circle if no arrow
	    if(arrowLength<=0) {
		ctx.save();
		ctx.fillStyle="#000";
		ctx.beginPath();
		ctx.arc(x,y, 1, 0, 2 * Math.PI);
		ctx.fill();
		ctx.restore();
	    }
	    ctx.beginPath();
	    ctx.moveTo(x,y);
	    ctx.lineTo(x2,y2);
	    ctx.lineWidth=opts.display.getProperty("lineWidth",1);
	    ctx.stroke();
	    if(arrowLength>0) {
		ctx.beginPath();
		this.drawArrow(ctx, x,y,x2,y2,arrowLength);
		ctx.stroke();
	    }



	} else if(opts.cell3D) {
	    let height = perc*(opts.cellSizeH||20);
	    ctx.strokeStyle = "#000";
	    ctx.strokeStyle = "rgba(0,0,0,0)"
	    RecordUtil.draw3DRect(canvas,ctx,x, canvas.height-y,+opts.cellSize,height,+opts.cellSize);
	} else if(opts.shape=="tile"){
	    let crx = x+opts.cellSizeX/2;
	    let cry = y+opts.cellSizeY/2;
 	    if((rowIdx%2)==0)  {
		crx = crx+opts.cellSizeX/2;
		cry = cry-opts.cellSizeY/2;
	    }
	    let sizex = opts.cellSizeX/2;
	    let sizey = opts.cellSizeY/2;
	    ctx.beginPath();
	    let quarter = Math.PI/2;
	    ctx.moveTo(crx + sizex * Math.cos(quarter), cry + sizey * Math.sin(quarter));
	    for (let side=0; side < 7; side++) {
		ctx.lineTo(crx + sizex * Math.cos(quarter+side * 2 * Math.PI / 6), cry + sizey * Math.sin(quarter+side * 2 * Math.PI / 6));
	    }
	    ctx.strokeStyle = "#000";
	    //	    ctx.fill();
	    ctx.stroke();

	} else {
	    let crx = x-opts.cellSizeX/2;
	    let cry = y+opts.cellSizeY/2;
	    crx=x;
	    cry=y
	    if(opts.stroke)
		ctx.strokeRect(x-opts.cellSizeX/2, y/*+opts.cellSizeY/2*/, opts.cellSizeX, opts.cellSizeY);
	    //	    ctx.strokeRect(x, y/*+opts.cellSizeY/2*/, opts.cellSizeX, opts.cellSizeY);
	    else
		ctx.fillRect(crx, cry, opts.cellSizeX, opts.cellSizeY);
	}
	if(opts.cellShowText && v!=null) {
	    ctx.textAlign = "center";
	    ctx.fillStyle = opts.cellTextColor||"#000";
	    ctx.fillText(Utils.formatNumber(v), x,y+10);
	}
    },
    //This gets the value at row/col if its defined. else 0
    //    sum+=this.getGridValue(src,rowIdx,colIdx,t[0],t[1],t[2],cnt); 
    getGridValue:function(src,row,col,mult,total,goodones) {
	if(row>=0 && row<src.length && col>=0 && col<src[row].length) {
	    if(isNaN(src[row][col])) return 0;
	    total[0]+=mult;
	    goodones[0]++;
	    return src[row][col]*mult;
	}
	return 0;
    },
    applyKernel: function(src, kernel) {
	let result = this.cloneGrid(src,null,0);
	for(var rowIdx=0;rowIdx<src.length;rowIdx++)  {
	    let row = result[rowIdx];
	    for(var colIdx=0;colIdx<row.length;colIdx++)  {
		//		if(isNaN(row[colIdx])) continue;
		if(isNaN(row[colIdx])) row[colIdx] = 0;
		let total =[0];
		let goodones =[0];
		let sum = 0;
		kernel.every(t=>{
		    sum+=this.getGridValue(src,rowIdx+t[0],colIdx+t[1],t[2],total,goodones); 
		    return true;
		});
		if(goodones[0]>0)
		    row[colIdx] = sum/total[0];
		else
		    row[colIdx] = NaN;
	    }
	}
	return result;
    },
    blurGrid: function(type, src) {
	let kernels = {
	    average5: [
		[0,1,0],
		[1,1,1],
		[0,1,0],
	    ],
	    average9: [
		[1,1,1],
		[1,1,1],
		[1,1,1],
	    ],
	    average25:[
		[1,1,1,1,1],
		[1,1,1,1,1],
		[1,1,1,1,1],
		[1,1,1,1,1],
		[1,1,1,1,1],
	    ],
	    average49:[
		[1,1,1,1,1,1,1],
		[1,1,1,1,1,1,1],
		[1,1,1,1,1,1,1],
		[1,1,1,1,1,1,1],
		[1,1,1,1,1,1,1],
		[1,1,1,1,1,1,1],
		[1,1,1,1,1,1,1],
	    ],
	    gauss9:[
		[0.077847,0.123317,0.077847],
		[0.123317,0.195346,0.123317],
		[0.077847,0.123317,0.077847]
	    ],
	    gauss25:[
		[0.003765,0.015019,0.023792,0.015019,0.003765],
		[0.015019,0.059912,0.094907,0.059912,0.015019],
		[0.023792,0.094907,0.150342,0.094907,0.023792],
		[0.015019,0.059912,0.094907,0.059912,0.015019],
		[0.003765,0.015019,0.023792,0.015019,0.003765],
	    ],
	    gauss49: [
		[0.00000067 ,0.00002292 ,0.00019117 ,0.00038771 ,0.00019117 ,0.00002292 ,0.00000067],
		[0.00002292 ,0.00078633 ,0.00655965 ,0.01330373 ,0.00655965 ,0.00078633 ,0.00002292],
		[0.00019117 ,0.00655965 ,0.05472157 ,0.11098164 ,0.05472157 ,0.00655965 ,0.00019117],
		[0.00038771 ,0.01330373 ,0.11098164 ,0.22508352 ,0.11098164 ,0.01330373 ,0.00038771],
		[0.00019117 ,0.00655965 ,0.05472157 ,0.11098164 ,0.05472157 ,0.00655965 ,0.00019117],
		[0.00002292 ,0.00078633 ,0.00655965 ,0.01330373 ,0.00655965 ,0.00078633 ,0.00002292],
		[0.00000067 ,0.00002292 ,0.00019117 ,0.00038771 ,0.00019117 ,0.00002292 ,0.00000067]
	    ]
	}
	let a = kernels[type];
	if(!a) {
	    if(type.startsWith("average"))
		a=kernels.average5;
	    else if(type.startsWith("gauss"))
		a=kernels.gauss9;
	}
	if(!a) return src;
	return this.applyKernel(src, this.makeKernel(a));
    },
    makeKernel: function(kernel) {
	let a = [];
	let mid = (kernel.length-1)/2;
	for(let rowIdx=0;rowIdx<kernel.length;rowIdx++) {
	    let row = kernel[rowIdx];
	    let rowOffset = rowIdx-mid;
	    for(let colIdx=0;colIdx<row.length;colIdx++) {
		let colOffset = colIdx-mid;
		a.push([rowOffset,colOffset,kernel[rowIdx][colIdx]]);
	    }
	}
	return a;
    },
    printGrid: function(grid) {
	console.log("grid:");
	for(var rowIdx=0;rowIdx<grid.length;rowIdx++)  {
	    let row = grid[rowIdx];
	    let h = "";
	    for(var colIdx=0;colIdx<row.length;colIdx++)  {
		if(Utils.isDefined(row[colIdx].v))
		    h+=row[colIdx].v+",";
		else
		    h+=row[colIdx]+",";
	    }
	    console.log(h);
	}
    },
    applyFilter(opts, grid) {
	if(!opts.filter || opts.filter=="" || opts.filter=="none") {
	    return;
	}

	let copy = this.cloneGrid(grid,v=>v.v);
	let filtered = copy;
	let filterPasses = opts.display.getProperty("hm.filterPasses",1);
	for(var i=0;i<filterPasses;i++) {
	    filtered = this.blurGrid(opts.filter,filtered);
	}
	let filterThreshold = opts.display.getProperty("hm.filterThreshold",-999);
	for(var rowIdx=0;rowIdx<grid.length;rowIdx++)  {
	    let row = grid[rowIdx];
	    for(var colIdx=0;colIdx<row.length;colIdx++)  {
		let cell = row[colIdx];
		let filterValue = filtered[rowIdx][colIdx];
		if(filterThreshold!=-999) {
		    if(filterValue<filterThreshold)
			filterValue = cell.v;
		}
		cell.v = filterValue;
	    }
	}
    },
    getMinMaxGrid: function(src,valueGetter) {
	let min = NaN;
	let max = NaN;
	for(var rowIdx=0;rowIdx<src.length;rowIdx++)  {
	    let row = src[rowIdx];
	    for(var colIdx=0;colIdx<row.length;colIdx++)  {
		let v = row[colIdx]
		if(valueGetter) v = valueGetter(v,rowIdx,colIdx);
		if(isNaN(v)) continue;
		min = isNaN(min)?v:Math.min(min,v);
		max = isNaN(max)?v:Math.max(max,v);
	    }
	}
	return {min:min,max:max};
    },



    cloneGrid: function(src,valueGetter,dflt) {
	let dest = [];
	let hasDflt = Utils.isDefined(dflt);
	for(var rowIdx=0;rowIdx<src.length;rowIdx++)  {
	    let row = src[rowIdx];
	    let nrow=[];
	    dest.push(nrow);
	    for(var colIdx=0;colIdx<row.length;colIdx++)  {
		let v = row[colIdx]
		if(valueGetter) v = valueGetter(v,rowIdx,colIdx);
		if(hasDflt)
		    nrow.push(dflt);
		else
		    nrow.push(v);
	    }
	}
	return dest;
    },
    gridData: function(gridId,records,args) {
	if(!args) args = {};
	if(isNaN(args.cellSize) || args.cellSize == null)
	    args.cellSize = args.cellSizeX;

	if(isNaN(args.cellSizeX) || args.cellSizeX == null)
	    args.cellSizeX= args.cellSize;
	if(isNaN(args.cellSizeY) || args.cellSizeY == null)
	    args.cellSizeY= args.cellSizeX;
	let opts = {
	    shape:"circle",
	    color:"blue",
	    w:800,
	    h:400,
	    cellSize:2,
	    cellSizeX:2,
	    cellSizeY:2,
	    operator:"average"
	}
	$.extend(opts,args);
	//	console.log(JSON.stringify(opts,null,2));
	let id = HtmlUtils.getUniqueId();
	$(document.body).append('<canvas style="display:none;" id="' + id +'" width="' + opts.w+'" height="' + opts.h +'"></canvas>');
	let canvas = document.getElementById(id);
	var ctx = canvas.getContext("2d");
	//	ctx.strokeStyle= "#000";
	//	ctx.strokeRect(0,0,canvas.width,canvas.height);
	let cnt = 0;
	let earthWidth = args.bounds.east-args.bounds.west;
	let earthHeight= args.bounds.north-args.bounds.south;
	ctx.font = opts.cellFont || "8pt Arial;"
	var gradient = ctx.createLinearGradient(0, 0, 0, canvas.height);
	gradient.addColorStop(0,'white');
	gradient.addColorStop(1,'red');

	let scaleX = (lat,lon)=>{
	    return Math.floor(opts.w*(lon-args.bounds.west)/earthWidth);
	};
	let scaleY;
	if(opts.display && opts.display.map) {
	    //Get the global bounds so we can map down to the image
	    var n1 = opts.display.map.transformLLPoint(createLonLat(opts.bounds.east,85));
	    var s1 = opts.display.map.transformLLPoint(createLonLat(opts.bounds.east,-85));
	    var n2 = opts.display.map.transformLLPoint(createLonLat(opts.bounds.east,opts.bounds.north));
	    var s2 = opts.display.map.transformLLPoint(createLonLat(opts.bounds.east,opts.bounds.south));
	    //	    console.log("n1:" + n1 +" s2:" + s1 +" n2:" + n2 +" s2:" + s2 +" bounds:" + JSON.stringify(opts.bounds));
	    scaleY = (lat,lon)=> {
		var pt = opts.display.map.transformLLPoint(createLonLat(lon,lat));
		var dy = n2.lat-pt.lat;
		var perc = dy/(n2.lat-s2.lat)
		return Math.floor(perc*opts.h);
	    };
	} else {
	    scaleY= (lat,lon)=> {
		return Math.floor(opts.h*(args.bounds.north-lat)/earthHeight);		
	    }
	}

	ctx.lineStyle = "#000";
	if(opts.doHeatmap) {
	    let cols = Math.floor(opts.w/opts.cellSizeX);
	    let rows = Math.floor(opts.h/opts.cellSizeY);
	    let points = [];
	    records.map((record,idx)=>{
		let lat = record.getLatitude();
		let lon = record.getLongitude();
		let x = scaleX(lat,lon);
		let y = scaleY(lat,lon);
		record[gridId+"_coordinates"] = {x:x,y:y};
		let v = 0;
		if(opts.colorBy && opts.colorBy.index>=0) {
		    v = record.getValue(opts.colorBy.index);
		}
		x =Math.floor(x/opts.cellSizeX);
		y =Math.floor(y/opts.cellSizeY);
		if(x<0) x=0;
		if(y<0) y=0;
		if(x>=cols) x=cols-1;
		if(y>=rows) y=rows-1;
		points.push({x:x,y:y,value:v,r:record});
	    });

	    let grid = RecordUtil.gridPoints(rows,cols,points,args);
	    opts.cellSizeX = +opts.cellSizeX;
	    opts.cellSizeY = +opts.cellSizeY;
	    this.applyFilter(opts,grid);
	    //get the new min/max from the filtered grid
	    let mm = this.getMinMaxGrid(grid,v=>v.v);
	    if(opts.colorBy) {
		if(!Utils.isDefined(opts.display.getProperty("colorByMin")))  {
		    opts.colorBy.setRange(mm.min,mm.max);
		}  else {
		    //		    console.log("color by range is already set:" +opts.display.getProperty("colorByMin"));
		}
		opts.colorBy.index=0;
	    }

	    let countThreshold = opts.display.getProperty("hm.countThreshold",opts.operator=="count"?1:0);
	    for(var rowIdx=0;rowIdx<rows;rowIdx++)  {
		let row = grid[rowIdx];
		for(var colIdx=0;colIdx<cols;colIdx++)  {
		    let cell = row[colIdx];
		    let v = cell.v;
		    if(isNaN(v)) continue;
		    let x = colIdx*opts.cellSizeX;
		    let y = rowIdx*opts.cellSizeY;
		    if(cell.count>=countThreshold)
			RecordUtil.drawGridCell(opts, canvas, ctx, x,y,cell.v,colIdx,rowIdx,cell, grid);
		}
	    }

	    opts.shape = "rect";
	    for(var rowIdx=0;rowIdx<rows;rowIdx++)  {
		let row = grid[rowIdx];
		for(var colIdx=0;colIdx<cols;colIdx++)  {
		    let cell = row[colIdx];
		    let v = cell.v;
		    if(isNaN(v)) continue;
		    let x = colIdx*opts.cellSizeX;
		    let y = rowIdx*opts.cellSizeY;
		    if(cell.count>=countThreshold)
			RecordUtil.drawGridCell(opts, canvas, ctx, x,y,cell.v,colIdx,rowIdx,cell, grid);
		}
	    }
	    


	} else {
	    records.sort((a,b)=>{return b.getLatitude()-a.getLatitude()});
	    records.map((record,idx)=>{
		let lat = record.getLatitude();
		let lon = record.getLongitude();
		let x = scaleX(lat,lon);
		let y = scaleY(lat,lon);
		record[gridId+"_coordinates"] = {x:x,y:y};
		let v = opts.colorBy? record.getData()[opts.colorBy.index]:null;
		if(false && opts.forMercator) {
		    var [tx,ty]  =RecordUtil.convertGeoToPixel(lat, lon,opts.bounds,opts.w,opts.h);
		    x = tx;
		    y = ty;
		}
		RecordUtil.drawGridCell(opts, canvas, ctx, x,y,v);
	    });
	}

	let alpha = opts.display.getProperty("colorTableAlpha",-1);
	//add in the color table alpha
	if(alpha>0) {
	    var image = ctx.getImageData(0, 0, opts.w, opts.h);
	    var imageData = image.data,
		length = imageData.length;
	    for(var i=3; i < length; i+=4){  
		if(imageData[i]) {
		    imageData[i] = alpha*255;
		}
	    }
	    image.data = imageData;
	    ctx.putImageData(image, 0, 0);
	}

	let img =  canvas.toDataURL("image/png");
	canvas.parentNode.removeChild(canvas);
	return img;
    },
    convertGeoToPixel:function(lat, lon,bounds,mapWidth,mapHeight) {
	var mapLonLeft = bounds.west;
	var mapLonRight = bounds.east;
	var mapLonDelta = mapLonRight - mapLonLeft;
	var mapLatBottom = bounds.south;
	var mapLatBottomDegree = mapLatBottom * Math.PI / 180;
	var x = (lon - mapLonLeft) * (mapWidth / mapLonDelta);
	var lat = lat * Math.PI / 180;
	var worldMapWidth = ((mapWidth / mapLonDelta) * 360) / (2 * Math.PI);
	var mapOffsetY = (worldMapWidth / 2 * Math.log((1 + Math.sin(mapLatBottomDegree)) / (1 - Math.sin(mapLatBottomDegree))));
	var y = mapHeight - ((worldMapWidth / 2 * Math.log((1 + Math.sin(lat)) / (1 - Math.sin(lat)))) - mapOffsetY);
	return [x, y];
    },


    getRanges: function(fields, records) {
        var maxValues = [];
        var minValues = [];
        for (var i = 0; i < fields.length; i++) {
            maxValues.push(NaN);
            minValues.push(NaN);
        }

        for (var row = 0; row < records.length; row++) {
            for (var col = 0; col < fields.length; col++) {
                var value = records[row].getValue(col);
                if (isNaN(value)) continue;
                maxValues[col] = (isNaN(maxValues[col]) ? value : Math.max(value, maxValues[col]));
                minValues[col] = (isNaN(minValues[col]) ? value : Math.min(value, minValues[col]));
            }
        }

        var ranges = [];
        for (var col = 0; col < fields.length; col++) {
            ranges.push([minValues[col], maxValues[col]]);
        }
        return ranges;
    },



    getElevationRange: function(fields, records) {
        var maxValue = NaN;
        var minValue = NaN;

        for (var row = 0; row < records.length; row++) {
            if (records[row].hasElevation()) {
                var value = records[row].getElevation();
                maxValue = (isNaN(maxValue) ? value : Math.max(value, maxValue));
                minValue = (isNaN(minValue) ? value : Math.min(value, minValue));
            }
        }
        return [minValue, maxValue];
    },


    slice: function(records, index) {
        var values = [];
        for (var rowIdx = 0; rowIdx < records.length; rowIdx++) {
            var row = records[rowIdx];
            if (row.getValue) {
                values.push(row.getValue(index));
            } else {
                values.push(row[index]);
            }
        }
        return values;
    },


    sort: function(fields) {
        fields = fields.slice(0);
        fields.sort(function(a, b) {
            var s1 = a.getSortOrder();
            var s2 = b.getSortOrder();
            return s1 < s2;
        });
        return fields;
    },
    getPoints: function(records, bounds) {
	let points = [];
	this.getBounds(records, bounds,points);
	return points;
    },
    getBounds: function(records, bounds,points) {
	bounds = bounds||{};
        if (records == null) {
	    return bounds;
	}
        var north = NaN,
            west = NaN,
            south = NaN,
            east = NaN;
	let errorCnt = 0;
        for (j = 0; j < records.length; j++) {
            var record = records[j];
            if (!isNaN(record.getLatitude()) && !isNaN(record.getLongitude())) {
                if (j == 0) {
                    north = record.getLatitude();
                    south = record.getLatitude();
                    west = record.getLongitude();
                    east = record.getLongitude();
                } else {
                    north = Math.max(north, record.getLatitude());
                    south = Math.min(south, record.getLatitude());
                    west = Math.min(west, record.getLongitude());
                    east = Math.max(east, record.getLongitude());
                }
                if (record.getLongitude() < -180 || record.getLatitude() > 90) {
		    //		    if(errorCnt++<50)
		    //			console.log("bad location: index=" + j + " " + record.getLatitude() + " " + record.getLongitude());
                }
		if(points)
                    points.push(new OpenLayers.Geometry.Point(record.getLongitude(), record.getLatitude()));
            }
        }
        bounds.north = north;
        bounds.west = west;
        bounds.south = south;
        bounds.east = east;
        return bounds;
    },

    findClosest: function(records, lon, lat, indexObj) {
        if (records == null) return null;
        var closestRecord = null;
        var minDistance = 1000000000;
        var index = -1;
        for (j = 0; j < records.length; j++) {
            var record = records[j];
            if (isNaN(record.getLatitude())) {
                continue;
            }
            var distance = Math.sqrt((lon - record.getLongitude()) * (lon - record.getLongitude()) + (lat - record.getLatitude()) * (lat - record.getLatitude()));
            if (distance < minDistance) {
                minDistance = distance;
                closestRecord = record;
                index = j;
            }
        }
        if (indexObj != null) {
            indexObj.index = index;
        }
        return closestRecord;
    },
    clonePoints: function(points) {
        var result = [];
        for (var i = 0; i < points.length; i++) {
            var point = points[i];
            result.push(new OpenLayers.Geometry.Point(point.x, point.y));
        }
        return result;
    }
};


function CsvUtil() {
    let eg = "convertData=\"derived(field=field_id, function=population*10);\nrotateData(includeFields=true,includeDate=true,flipColumns=true);\naddPercentIncrease(replaceValues=false);\"\n";
    $.extend(this, {
	process: function(display, pointData, cmds) {
	    this.display = display;
	    let commands = DataUtils.parseCommands(cmds);

	    commands.map(cmd=>{
		if(this[cmd.command]) {
		    let orig = pointData;
    		    pointData = this[cmd.command](pointData, cmd.args);
		    if(!pointData) pointData=orig;
		    else pointData.entryId = orig.entryId;
		} else {
		    console.log("unknown command:" + cmd.command);
		}
	    });
	    return pointData;
	},
	help: function(pointData, args) {
	    console.log(eg);
	    return null;
	},
	furlData: function(pointData, args) {
	    /** TODO
		let records = pointData.getRecords(); 
		let header = this.display.getDataValues(records[0]);
		var newRecords  =[];
		var newFields = [];
		var lastRecord = dataList[dataList.length-1];
		var fields  = pointData.getRecordFields();
		var newData  =[];
		newData.push(["label","value"]);
		//		dataList.map(r=>{
		fields.map((f,idx)=>{
		let row = [f.getLabel(),lastRecord.getValue(f.getIndex())];
		newData.push();
		});
		pointData = convertToPointData(newData);
		pointData.entryId = originalPointData.entryId;
	    **/
	},
	derived: function(pointData, args) {
	    let records = pointData.getRecords(); 
	    let fields =  pointData.getRecordFields();
	    let newFields =  fields.slice();
	    let newRecords  =[];
	    let id = args["field"] || ("field_" + fields.length);
            newFields.push(new RecordField({
		id:id,
		index:fields.length,
		label:Utils.makeLabel(id),
		type:"double",
		chartable:true,
		unit: args.unit
            }));
	    let func = args["function"];
	    if(!func) {
		console.log("No func specified in derived");
		return null;
	    }
	    func = func.replace(/_nl_/g, "\n").replace(/_semi_/g,";");
	    if(func.indexOf("return")<0) {
		func = "return " + func;
	    }
            let setVars = "";
            fields.map((field,idx)=>{
		if(field.isFieldNumeric() && field.getId()!="") {
		    let varName = field.getId().replace(/^([0-9]+)/g,"v$1");
		    setVars += "\tvar " + varName + "=displayGetFunctionValue(args[\"" + field.getId() + "\"]);\n";
		}
            });
            let code = "function displayDerivedEval(args) {\n" + setVars + func + "\n}";
	    //	    console.log(code);
            eval(code);
	    records.map((record, rowIdx)=>{
		let newRecord = record.clone();
		newRecord.data= record.data.slice();
		newRecords.push(newRecord);
		let funcArgs = {};
		fields.map((field,idx)=>{
		    if(field.isFieldNumeric() && field.getId()!="") {
			funcArgs[field.getId()] = record.getValue(field.getIndex());
		    }
		});
		try {
		    let value = displayDerivedEval(funcArgs);
		    newRecord.data.push(value);
		} catch(exc) {
		    console.log("Error processing derived:" + exc);
		    newRecord.data.push(NaN);
		}
	    });
	    return   new  PointData("pointdata", newFields, newRecords,null,null);
	},
	rotateData: function(pointData, args) {
	    let records = pointData.getRecords(); 
            let header = this.display.getDataValues(records[0]);
	    let rotated = [];
	    for(var colIdx=0;colIdx<header.length;colIdx++) {
		rotated.push([]);
	    }
	    let includeFields =args["includeFields"] == "true";
	    let includeDate = args["includeDate"] == "true";
	    let flipColumns =args["flipColumns"]=="true";
	    let fields = pointData.getRecordFields();
	    if (!flipColumns && includeFields) {
                fields.map((f,colIdx)=>{
		    if(f.isRecordDate()) return;
		    rotated[colIdx].push(colIdx==0?args["fieldName"]||"Field":f.getLabel());
		});
	    }
            for (var rowIdx = 0; rowIdx < records.length; rowIdx++) {
                let row = this.display.getDataValues(records[rowIdx]);
		for(var colIdx=0;colIdx<row.length;colIdx++) {
		    let field = fields[colIdx];
		    if(field.isRecordDate()) {
			continue;
		    }
		    var value = row[colIdx];
		    if(value.f) value = value.f;
		    if(value.getTime) {
			value = this.display.formatDate(value);
		    }
		    if(!includeFields && rowIdx==0 && colIdx==0) value="";

		    if(flipColumns)
			rotated[colIdx].unshift(value);
		    else
			rotated[colIdx].push(value);
		}
            }
	    if (flipColumns && includeFields) {
                fields.map((f,colIdx)=>{
		    if(f.isRecordDate()) return;
		    rotated[colIdx].unshift(colIdx==0?args["fieldName"]||"Field":f.getLabel());
		});
	    }
	    return  convertToPointData(rotated);
	},
	addPercentIncrease: function(pointData, args) {
	    let records = pointData.getRecords(); 
            let header = this.display.getDataValues(records[0]);
            let fields  = pointData.getRecordFields();
	    let newRecords  =[];
	    let firstRecord= records[0];
	    let replaceValues = args["replaceValues"]=="true";
	    let newFields = [];
	    fields.map((f,fieldIdx)=>{
		f = f.clone();
		let newField = f.clone();
		if(!replaceValues) {
		    f.index = newFields.length;
		    newFields.push(f);
		}
		if(f.isNumeric()) {
		    newField.unit = "%";
		    newField.index = newFields.length;
		    newFields.push(newField);
		    newField.id = newField.id +"_percent";
		    newField.label = newField.label+" % increase";
		}
	    });
	    /*
	      newFields.map((f,fieldIdx)=>{
	      if(fieldIdx>3) return;
	      console.log("F:" + f.getLabel() +" " + f.index);
	      });*/
	    records.map((record, rowIdx)=>{
		let data = [];
		let newRecord = record.clone();
		newRecords.push(newRecord);
		fields.map((f,fieldIdx)=>{
		    let value = record.data[f.getIndex()];
		    if(!f.isNumeric()) {
			data.push(value);
			return;
		    }
		    if(!replaceValues) {
			data.push(value);
		    }
		    if(rowIdx==0) {
			data.push(0);
		    } else {
			let basev = firstRecord.data[f.getIndex()];
			let perc = basev==0?0:(value-basev)/basev;
			data.push(perc);
		    }
		}); 
		newRecord.data=data;
	    });
	    return   new  PointData("pointdata", newFields, newRecords,null,null);
	},
	doAverage: function(pointData, args) {
	    let records = pointData.getRecords(); 
            let header = this.display.getDataValues(records[0]);
            let fields  = pointData.getRecordFields();
	    var newRecords  =[];
	    var newFields = [];
	    var firstRow = records[0];
	    fields.map(f=>{
		var newField = f.clone();
		newFields.push(newField);
		newField.label = newField.label+" (avg)";
	    });
	    var sums=[];
	    fields.map(f=>{sums.push(0)});
	    var newRecord;
	    for (var rowIdx=0; rowIdx <records.length; rowIdx++) {
		var record = records[rowIdx];
		if(newRecord==null) {
		    newRecord = record.clone();
		    newRecords.push(newRecord);
		}
		fields.map((f,idx)=>{
		    if(!f.isNumeric()) return;
		    var v = record.data[f.getIndex()];
		    sums[idx]+=v;
		});
		fields.map((f,idx)=>{
		    if(!f.isNumeric()) return;
		    newRecord.data[idx] = sums[idx]/records.length;
		});
	    }
	    return   new  PointData("pointdata", newFields, newRecords,null,null);
	},
	mergeRows: function(pointData, args) {
	    let records = pointData.getRecords(); 
            let fields  = pointData.getRecordFields();
	    let op = args["operator"] || "count";

	    let keyFields =  this.display.getFieldsByIds(fields, (args["keyFields"]||"").replace(/_comma_/g,","));
	    if(keyFields.length==0) throw new Error("No key fields processing mergeRows:" + args["keyFields"]);
	    let altFields =  this.display.getFieldsByIds(fields, (args["altFields"]||"").replace(/_comma_/g,","));

	    let newFields = [];
	    let seen = {};
	    keyFields.forEach((f,idx)=>{
		seen[f.getId()] = true;
		var newField = f.clone();
		newField.index = newFields.length;
		newFields.push(newField);
	    });
	    let tmp =  this.display.getFieldsByIds(fields, (args["valueFields"]||"").replace(/_comma_/g,","));
	    if(args["valueFields"]==null) tmp=fields;
	    let valueFields = [];
	    tmp.forEach(f=>{
		if(!seen[f.getId()])
		    valueFields.push(f);
	    });

	    valueFields.forEach((f,idx)=>{
		var newField = f.clone();
		newField.index = newFields.length;
		if(newField.isNumeric()) {
		    newField.id = newField.id +"_" + op;
		    newField.label = Utils.makeLabel(newField.id);
		}
		newFields.push(newField);
	    });
	    //	    console.log("key fields:" + keyFields);
	    //	    console.log("value fields:" + valueFields);
	    if(op == "count") {
		newFields.push(new RecordField({
		    id:"count",
		    index:newFields.length,
		    label:"Count",
		    type:"double",
		    chartable:true,
		}));
	    }
	    let keys = [];
	    let collection ={
	    };

	    for (var rowIdx=0; rowIdx <records.length; rowIdx++) {
		var record = records[rowIdx];
		let key = "";
		keyFields.forEach(f=>{
		    let v = record.getValue(f.getIndex());
		    if(v=="" && altFields.length>0) {
			altFields.forEach(f2=>{
			    v+=record.getValue(f1.getIndex()) +"-";
			});
		    }
		    key +=v+"-";
		});
		let obj = collection[key];
		if(!obj) {
		    keys.push(key);
		    obj = collection[key]= {
			count:0,
			sums: [],
			mins: [],
			maxs:[],
			records:[]
		    };
		    valueFields.forEach((f,idx)=>{
			obj.sums.push(0);
			obj.mins.push(NaN);
			obj.maxs.push(NaN);
		    });
		}
		valueFields.forEach((f,idx)=>{
		    let v = record.getValue(f.getIndex());
		    if(f.isNumeric() && !isNaN(v)) {
			obj.sums[idx]+=v;
			obj.mins[idx] = isNaN(obj.mins[idx])?v:Math.min(obj.mins[idx],v);
			obj.maxs[idx] = isNaN(obj.maxs[idx])?v:Math.max(obj.maxs[idx],v);
		    }
		});
		obj.count++;
		obj.records.push(record);
	    }
	    let newRecords = [];
	    let cnt = 0;
	    keys.forEach((key,idx)=>{
		let obj = collection[key];
		let data =[];
		let seen = {};
		let date = obj.records[0].getDate();
		let bounds = RecordUtil.getBounds(obj.records);
		let lat =  bounds.south+(bounds.north-bounds.south)/2;
		let lon =  bounds.west+(bounds.east-bounds.west)/2;
		if(key.indexOf("US")>=0) {
		    cnt++;
		    if(cnt==1) {
//			console.log(obj.records.length +" " +bounds.west +" " + bounds.east +" " + lat  +" " +lon);
			obj.records.forEach(r=>{
//			    console.log(r.getValue(0) + " " + r.getLatitude() +" " + r.getLongitude())
			});
		    }
		}
		keyFields.forEach(f=>{
		    let v = obj.records[0].getValue(f.getIndex());
		    data.push(v);
		    seen[f.getId()]=true;
		});
		valueFields.forEach((f,idx)=>{
		    if(seen[f.getId()]) return;
		    if(!f.isNumeric()) {
			data.push(obj.records[0].getValue(f.getIndex()));
		    } else {
			if(op == "sum") 
			    data.push(obj.sums[idx]);
			else if(op == "average") 
			    data.push(obj.sums[idx]/obj.count);
			else if(op == "min") 
			    data.push(obj.mins[idx]);
			else if(op == "max") 
			    data.push(obj.maxs[idx]);
		    }
		});
		if(op == "count") {
		    data.push(obj.count);
		}
		let newRecord = new  PointRecord(newFields,lat,lon, NaN, date, data);
		newRecords.push(newRecord);
	    });

	    return   new  PointData("pointdata", newFields, newRecords,null,null);
	},
	unfurl: function(pointData, args) {
	    let records = pointData.getRecords(); 
            let fields  = pointData.getRecordFields();
	    let headerField =  this.display.getFieldById(fields, args.headerField||"");
	    let uniqueField =  this.display.getFieldById(fields, args.uniqueField||"");
	    let valueFields =  this.display.getFieldsByIds(fields, args.valueFields||"");
	    let includeFields =  this.display.getFieldsByIds(fields, args.includeFields||"");

	    
	    if(!headerField) throw new Error("No headerField");
	    if(!uniqueField) throw new Error("No uniqueField");
	    if(valueFields.length==0) throw new Error("No value fields");
	    /*
	      newFields.push(new RecordField({
	      id:"count",
	      index:newFields.length,
	      label:"Count",
	      type:"double",
	      chartable:true,
	      }));
	    */
	    let newColumns = [];
	    let newColumnMap = {};
	    let uniqueToRecords = {};
	    let rowMap = {};
	    let uniques = [];
	    let indexMap={};
	    records.forEach(record=>{
		let unfurlValue = record.getValue(headerField.getIndex());
		let uniqueValue = record.getValue(uniqueField.getIndex());
		if(!newColumnMap[unfurlValue]) {
                    newColumnMap[unfurlValue] = true;
                    if (valueFields.length > 1) {
                        valueFields.forEach(f=>{
                            let label = unfurlValue + " - "
                                + f.getLabel();
                            newColumns.push(label);
                        });
                    } else {
			newColumns.push(unfurlValue);
		    }
		}
                let rowGroup    = rowMap[uniqueValue];
                if (rowGroup == null) {
                    rowMap[uniqueValue] =  rowGroup = [];
                    uniques.push(uniqueValue);
                }
                rowGroup.push(record);
	    });

	    newColumns.sort();
            newColumns.forEach((v,idx) => {
                indexMap[v] = idx;
            });

	    let newRecords = [];
	    let newFields = [];
	    newColumns = Utils.mergeLists([uniqueField.getId()], newColumns);
	    newColumns.forEach((c,idx)=>{
		let type = (idx==0?"string":"double");
		newFields.push(new RecordField({
		    id:c,
		    index:newFields.length,
		    label:c,
		    type:type,
		    chartable:true,
		}));
	    });
	    uniques.forEach(u=>{
		let array = [];
		newColumns.forEach(c=>{
		    array.push("");
		});
                array[0] = u;
                let  includeCnt = 0;
                let rowValues  = null;
                let firstRow   = null;
                cnt = 0;
                rowMap[u].forEach(row=>{
                    if (firstRow == null) {
                        firstRow = row;
                    }
                    let colname = row.getValue(headerField.getIndex());
                    if (valueFields.length > 1) {
                        valueFields.forEach(f=>{
                            let label = colname + " - "  + f.getId();
                            let idx = indexMap[label];
                            if (idx == null) {
				return;
                            }
                            let value =  row.getValue(valueIndex);
                            array[1 + includeFields.length + idx] = value;
                        });
		    } else {
                        let idx = indexMap[colname];
                        if (idx == null) {
			    return;
                        }
                        let    valueIndex = valueFields[0].getIndex();
                        let value = row.getValue(valueIndex);
                        array[1 + includeFields.length + idx] = value;
                    }
                    cnt++;
		});

                includeFields.forEach(f=>{
                    array[1 + includeCnt] = firstRow.getValue(f.getIndex());
                    includeCnt++;
                });
		let newRecord = new  PointRecord(newFields,NaN, NaN, NaN, null, array);
		newRecords.push(newRecord);
                cnt++;
            });
 	    return   new  PointData("pointdata", newFields, newRecords,null,null);
	},


    });
}




var DataUtils = {
    parseCommands: function(commands) {
	let result = [];
	if(!commands) return result;
	commands.split(";").map(command=>{
	    command = command.trim();
	    let idx=command.indexOf("(");
	    let args = {};
	    if(idx>=0) {
		let rest = command.substring(idx+1).trim();
		command = command.substring(0,idx).trim();
		if(rest.endsWith(")")) rest = rest.substring(0,rest.length-1);
		let toks = [];
		let inQuote = false;
		let escape = false;
		let tok = "";
		for(let i=0;i<rest.length;i++) {
		    let c = rest[i];
		    if(c=="\\") {
			escape=true;
			continue;
		    }
		    if(escape) {
			tok += c;
			escape=false;
			continue;
		    }
		    if(c=="\'") {
			if(!inQuote) {
			    inQuote = true;
			} else {
			    inQuote = false;
			}
			tok += c;
			continue;
		    }
		    if(c == ",") {
			if(inQuote) {
			    tok += c;			    
			    continue;
			}
			toks.push(tok);
			tok = "";
			continue;
		    }
		    tok += c;			    
		}
		toks.push(tok);
		toks.forEach(arg=>{
		    arg =arg.trim();
		    let value = "";
		    let atoks = arg.match(/(.*)=(.*)/);
		    if(atoks) {
			arg=atoks[1];
			value= atoks[2];
		    }
		    arg = arg.trim();
		    value = value.trim();
		    //		    console.log("arg:" + arg +" v:" + value);
		    //Strip off quotes
		    value = value.replace(/^'/g,"").replace(/'$/g,"");
		    if(arg!="") {
			args[arg] = value;
		    }
		});
	    }
	    if(command!="") {
		result.push({command:command,args:args});
	    }
	});
	return result;
    },
    getDataFilters: function(display, prop) {
	let filters = [];
	if(!prop) return filters;
	let baseId = display.getId();
	let cnt = 0;
	DataUtils.parseCommands(prop).map(cmd=>{
	    let filterId = baseId+"_" + (cnt++);
	    let [type,fieldId,value,enabled,label,expr]  = [cmd.command,cmd.args.field,cmd.args.value,cmd.args.enabled,cmd.args.label,cmd.args.expr];
	    if(!Utils.isDefined(enabled))
		enabled = true;
	    else {
		enabled = enabled.trim()=="true" || enabled.trim()=="";
	    }
	    if(label) {
		var cbx =  display.jq("datafilterenabled_" + filterId);
		if(cbx.length) {
		    enabled = cbx.is(':checked');
		} 
	    }
	    if(type=="match" || type=="notmatch")
		value = new RegExp(value);
	    else
		value = +value;
	    let fields = null;
	    if(cmd.args.fields) {
		fields = display.getFieldsByIds(null, cmd.args.fields.replace(/:/g,","));
	    }
	    let allFields = display.getData().getRecordFields();
	    let field = display.getFieldById(null,fieldId);
	    filters.push({
		id:filterId,
		props:cmd.args,
		type:type.trim(),
		field:field,
		fields:fields?fields:field?[field]:null,
		allFields:allFields,
		value:value,
		label:label,
		enabled: enabled,
		expr:expr,
		isRecordOk: function(r) {
		    if(!this.enabled) {
			return true;
		    }
		    let value = this.field?r.getValue(this.field.getIndex()):NaN;
		    if(this.type == "match") {
			return String(value).match(this.value);
		    } else if(this.type == "nomissing") {
			let fieldsToUse =null;
			if(this.fields) {
			    fieldsToUse = this.fields;
			} else if(this.field) {
			    fieldsToUse = [this.field];
			} else {
			    fieldsToUse = r.fields;
			}
			let ok = false;
			fieldsToUse.some(f=>{
			    if(field && !(field.isFieldLatitude() || f.isFieldLongitude()))
				if(f.isFieldLatitude() || f.isFieldLongitude()) return true;
			    if(f.isNumeric()) {
				let v  = r.getValue(f.getIndex());
				//				console.log("V:" + v);
				ok  =!isNaN(v);
			    }
			    return ok;
			});
			//			if(!ok) 
			//			    console.log("****** V:" +value + " v:" + this.value);
			return ok;
		    } else if(this.type == "notmatch") {
			return  !String(value).match(this.value);
		    } else if(this.type == "lessthan") {
			return  value<this.value;
		    } else if(this.type == "greaterthan") {
			return  value>this.value;
		    }  else if(this.type == "equals") {
			return  value==this.value;
		    }  else if(this.type == "notequals") {
			return value!=this.value;
		    }  else if(this.type == "bounds") {
			let lat =  r.getLatitude();
			let lon =  r.getLongitude();
			if(this.props.north && lat>+this.props.north) return false;
			if(this.props.south && lat<+this.props.south) return false;
			if(this.props.west && lon<+this.props.west)   return false;
			if(this.props.east && lon>+this.props.east) {return false;}
			return true;
		    }  else if(this.type == "eval") {
			//return true;
			//assume its inline code
			let code = "function dataFilterCall(){\n";
			this.allFields.every(f=>{
			    let value = r.getValue(f.getIndex());
			    if(typeof value == "string") value = "'" + value +"'";
			    else if(typeof value != "number") value = "'" + value +"'";
			    code+=f.getId() +"=" + value+";\n"
			    return true;
			});
			let expr = this.expr;
			if(expr == null) throw "No expr given in data filter";
			if(expr.indexOf("return")<0)  expr  = " return " + expr;
			code+=expr+"\n}\n";
			code +="var dataFilterValue = dataFilterCall();\n";
			eval(code);
			return dataFilterValue;
		    } else {
			console.log("Unknown filter:" + this.type);
			return true;
		    }

		}
	    });
	});
	return filters;
    },
}


function RequestMacro(display, macro) {
    this.display = display;
    let values = null;
    let enums = this.getProperty("request." +macro+".values");
    if(enums) {
	values =[]	
	if(this.getProperty("request." + macro+".includeAll",this.getProperty("request.includeAll",false))) {
	    values.push(["","All"]);
	}
	if(this.getProperty("request." + macro+".includeNone",false)) {
	    values.push(["","None"]);
	}
	enums.split(",").every(tok=>{
	    [id,label] = tok.split(":");
	    values.push([id,label||id]);
	    return true;
	});
    }
    let macroType = this.getProperty("request." +macro+".type",values!=null?"enumeration":macro=="bounds"?"bounds":"string");
    let dflt =this.getProperty("request." +macro+".default",null);
    if(dflt == null) {
	if(values && values.length>0  && macroType=="enumeration") {
	    dflt = values[0][0];
	} else {
	    dflt = "";
	}
    }
    $.extend(this,{
	name: macro,
	values:values,
	urlarg: this.getProperty("request." +macro+".urlarg",macro),
	type:macroType,
	triggerReload:this.getProperty("request." +macro+".triggerReload",true),
	dflt:dflt,
	dflt_from:this.getProperty("request." +macro+"_from.default",""),		    
	dflt_to:this.getProperty("request." +macro+"_to.default",""),
	dflt_min:this.getProperty("request." +macro+"_min.default",""),		    
	dflt_max:this.getProperty("request." +macro+"_max.default",""),
	label:this.getProperty("request." +macro+".label",Utils.makeLabel(macro)),
	multiple:this.getProperty("request." +macro+".multiple",false),
	rows:this.getProperty("request." +macro+".rows",3),
    });
}


RequestMacro.prototype = {
    getProperty: function(prop, dflt)   {
	return this.display.getProperty(prop, dflt);
    },
    getWidget: function(dateIds) {
	let debug = false;
	let visible = this.display.getProperty("request." +this.name +".visible",
					       this.display.getProperty("macros.visible",true));
	let style = visible?"":"display:none;";
	let widget;
	let label = this.label;
	if(debug)console.log("getWidget:" + label +" type:" + this.type);
	if(this.type=="bounds") {
	    widget = HU.checkbox("",[ID,this.display.getDomId(this.getId())], false) +HU.span([CLASS,"display-request-reload",TITLE,"Reload with current bounds"], " In bounds");
	    label = null;
	} else if(this.type=="enumeration") {
 	    if(this.values && this.values.length>0) {
		let attrs = [STYLE, style, ID,this.display.getDomId(this.getId()),CLASS,"display-filter-input"];
		if(this.multiple) {
		    attrs.push("multiple");
		    attrs.push(null);
		    attrs.push("size");
		    attrs.push(Math.min(this.rows,this.values.length));
		}
		if(debug)
		    console.log("\tselect: dflt:" + this.dflt +" values:" + this.values);
		
		widget = HU.select("",attrs,this.values,this.dflt,20);
	    }
	} else if(this.type=="numeric") {
	    let minId = this.display.getDomId(this.getId()+"_min");
	    let maxId = this.display.getDomId(this.getId()+"_max");			    
	    widget = HU.input("","",[STYLE, style, ID,minId,"size",4,CLASS,"display-filter-input"],this.dflt_min) +
		" - " +
		HU.input("","",[STYLE, style, ID,maxId,"size",4,CLASS,"display-filter-input"],this.dflt_max)
	    label = label+" range";
	} else if(this.type=="date") {
	    let fromId = this.display.getDomId(this.getId()+"_from");
	    let toId = this.display.getDomId(this.getId()+"_to");
	    dateIds.push(fromId);
	    dateIds.push(toId);
	    widget = HU.datePicker("",this.dflt_from,[CLASS,"display-filter-input",STYLE, style, "name","",ID,fromId]) +
		" - " +
		HU.datePicker("",this.dflt_to,[CLASS,"display-filter-input",STYLE, style, "name","",ID,toId])
	    label = label+" range";
	} else {
	    let size = "10";
	    if(this.type=="number")
		size = "4";
	    widget = HU.input("",this.dflt,[STYLE, style, ID,this.display.getDomId(this.getId()),"size",size,CLASS,"display-filter-input"]);
	}
	if(!widget) return "";
	return (visible?this.display.makeFilterWidget(label,widget):widget);
    },
    getId: function() {
	return "macro_" + this.name;
    },
    getValue: function() {
	let widget = this.display.jq(this.getId());
	let value = this.dflt;
	if(widget.length!=0) value =  widget.val();
	this.display.setProperty("request." + this.name+".default",value);
	return value;
    },
    apply: function(url) {
	if(this.type == "bounds") {
	    if(this.display.getBounds && this.display.jq(this.getId()).is(':checked')) {
		let bounds = this.display.getBounds();
		if(bounds) {
		    bounds = RecordUtil.convertBounds(bounds);
		    ["north","south","east","west"].map(b=>{
			url+="&" + b+"=" +bounds[b];
		    });
		    
		}
	    }
	} else if(this.type=="numeric") {
	    let min = this.display.jq(this.getId()+"_min").val()||"";
	    let max = this.display.jq(this.getId()+"_max").val()||"";
	    this.dflt_min = min;
	    this.dflt_max = max;
	    if(min!="")
		url = url +"&" + HU.urlArg(this.urlarg+"_from",min);
	    if(max!="")
		url = url +"&" + HU.urlArg(this.urlarg+"_to",max);
	    this.display.setProperty("request." +this.name+"_min.default",min);
	    this.display.setProperty("request." +this.name+"_max.default",max);

	} else if(this.type=="date") {
	    let from = this.display.jq(this.getId()+"_from").val()||"";
	    let to = this.display.jq(this.getId()+"_to").val()||"";
	    this.dflt_from = from;
	    this.dflt_to = to;
	    this.display.setProperty("request." +this.name+"_from.default",from);
	    this.display.setProperty("request." +this.name+"_to.default",to);
	    if(from!="")
		url = url +"&" + HU.urlArg(this.urlarg+"_fromdate",from);
	    if(to!="")
		url = url +"&" + HU.urlArg(this.urlarg+"_todate",to);
	    //			    this.display.setProperty(this.name+".default",value);
	} else if(this.type=="enumeration") {
	    let value = this.getValue();
	    if(!Array.isArray(value)) {value=[value];}
	    if(value.length>0 && value[0]!="") {
		let regexp = new RegExp(this.urlarg+"=[^$&]*",'g');
		url = url.replace(regexp,"");
		value.map(v=>{
		    if(v!="")
			url = url +"&" + HU.urlArg(this.urlarg,v);
		});
	    }
	} else {
	    let value = this.getValue();
	    this.dflt  = value;
	    if(value!="") {
		let regexp = new RegExp(this.urlarg+"=[^$&]*",'g');
		url = url.replace(regexp,"");
		url = url +"&" + HU.urlArg(this.urlarg,value);
	    }
	}
	return  url;
    }

}





function ColorByInfo(display, fields, records, prop,colorByMapProp, defaultColorTable, propPrefix, theField, props,lastColorBy) {
    this.properties = props || {};
    if(!prop) prop = "colorBy";
    if(Utils.isDefined(this.properties.minValue)) this.properties.hasMinValue = true;
    if(Utils.isDefined(this.properties.maxValue)) this.properties.hasMaxValue = true;    

    if ( !propPrefix ) {
	propPrefix = ["colorBy",""];
    } else if( !Array.isArray(propPrefix) ) {
	propPrefix = [propPrefix];
    }
    $.extend(this, {
	display:display,
	fieldProp: prop,
	fieldValue:display.getProperty(prop),
	propPrefix: propPrefix,
	colorHistory:{}
    });

    let colorByAttr = this.getProperty(prop||"colorBy", null);
    if(theField==null) {
	if(prop.getId) {
	    theField = prop;
	} else {
	    theField = display.getFieldById(null, colorByAttr);
	}
    }

    if(theField) {
	this.field = theField;
	propPrefix = [theField.getId()+".",""];
	colorByAttr =theField.getId();
	this.propPrefix.unshift(theField.getId()+".colorBy");
	this.propPrefix.push("colorBy");
    }

    $.extend(this, {
	display:display,
        id: colorByAttr,
	fields:fields,
        field: theField,
	colorThresholdField:display.getFieldById(null, display.getProperty("colorThresholdField")),
	aboveColor: display.getProperty("colorThresholdAbove","red"),
	belowColor:display.getProperty("colorThresholdBelow","blue"),
	nullColor:display.getProperty("nullColor"),	
	excludeZero:this.getProperty(PROP_EXCLUDE_ZERO, false),
	overrideRange: this.getProperty("overrideColorRange",false),
	inverse: this.getProperty("Inverse",false),
	origRange:null,
	origMinValue:0,
	origMaxValue:0,
        minValue: 0,
        maxValue: 0,
	toMinValue: 0,
        toMaxValue: 100,
        isString: this.properties.isString,
        stringMap: null,
	colorByMap: {},
	colorByValues:[],
	colorByMinPerc: this.getProperty("MinPercentile", -1),
	colorByMaxPerc: this.getProperty("MaxPercentile", -1),
	colorByOffset: 0,
        pctFields:null,
	compareFields: display.getFieldsByIds(null, this.getProperty("CompareFields", "")),
    });
    //Reuse the last color map if there is one so the string->color stays the same
    if(lastColorBy && !lastColorBy.colorOverflow) {
//	this.lastColorByMap= lastColorBy.colorByMap;
    }
    
    if(this.fieldValue == "year") {
	let seen= {};
	this.dates = [];
	records.forEach(r=>{
	    let date = r.getDate();
	    if(!date) return;
	    let year = r.getDate().getUTCFullYear();
	    if(!seen[year]) {
		seen[year] = true;
		this.dates.push(year);
	    }
	});
	this.dates.sort();
	this.setRange(this.dates[0],this.dates[this.dates.length-1]);
    }


    this.convertAlpha = this.getProperty("convertColorAlpha",false);
    this.alphaMin = this.getProperty("alphaMin");
    this.alphaMax = this.getProperty("alphaMax");    
    this.hasAlphaMin = Utils.isDefined(this.alphaMin);
    this.hasAlphaMax = Utils.isDefined(this.alphaMax);    
    if(this.convertAlpha) {
	if(!Utils.isDefined(this.getProperty("alphaSourceMin"))) {
	    var min = 0, max=0;
	    records.forEach((record,idx)=>{
		var tuple = record.getData();
		if(this.compareFields.length>0) {
		    this.compareFields.map((f,idx2)=>{
			var v = tuple[f.getIndex()];
			if(isNaN(v)) return;
			min = idx==0 && idx2==0?v:Math.min(min,v);
			max = idx==0 && idx2==0?v:Math.max(max,v);
		    });
		} else if (this.index=0) {
		    var v = tuple[this.index];
		    if(isNaN(v)) return;
		    min = idx==0?v:Math.min(min,v);
		    max = idx==0?v:Math.max(max,v);
		}
	    });
	    this.alphaSourceMin = min;
	    this.alphaSourceMax = max;
	} else {
	    this.alphaSourceMin = +this.getProperty("alphaSourceMin",40);
	    this.alphaSourceMax = +this.getProperty("alphaSourceMax",80);
	}
	this.alphaTargetMin = +this.getProperty("alphaTargetMin",0); 
	this.alphaTargetMax = +this.getProperty("alphaTargetMax",1); 
    }

    this.convertIntensity = this.getProperty("convertColorIntensity",false);
    if(this.convertIntensity) {
	if(!Utils.isDefined(this.getProperty("intensitySourceMin"))) {
	    var min = 0, max=0;
	    records.forEach((record,idx)=>{
		var tuple = record.getData();
		if(this.compareFields.length>0) {
		    this.compareFields.map((f,idx2)=>{
			var v = tuple[f.getIndex()];
			if(isNaN(v)) return;
			min = idx==0 && idx2==0?v:Math.min(min,v);
			max = idx==0 && idx2==0?v:Math.max(max,v);
		    });
		} else if (this.index=0) {
		    var v = tuple[this.index];
		    if(isNaN(v)) return;
		    min = idx==0?v:Math.min(min,v);
		    max = idx==0?v:Math.max(max,v);
		}
	    });
	    this.intensitySourceMin = min;
	    this.intensitySourceMax = max;
	} else {
	    this.intensitySourceMin = +this.getProperty("intensitySourceMin",80);
	    this.intensitySourceMax = +this.getProperty("intensitySourceMax",40);
	}
	this.intensityTargetMin = +this.getProperty("intensityTargetMin",1); 
	this.intensityTargetMax = +this.getProperty("intensityTargetMax",0); 
    }

    if (this.display.percentFields != null) {
        this.pctFields = this.display.percentFields.split(",");
    }


    let colors = null;
    if(colorByAttr) {
	let c = this.display.getProperty(colorByAttr +".colors");
	if(c) colors = c.split(",");
    }



    if(!colors){
	colors = defaultColorTable || this.display.getColorTable(true,[this.properties.colorTableProperty,
								       colorByAttr +".colorTable",
								       "colorTable"]);
    }

    
//    if(!colors && this.hasField()) {
//	colors = this.display.getColorTable(true,"colorTable");
//    }

    if(!colors) {
	var c = this.getProperty(colorByAttr +".colors");
	if(c)
	    colors = c.split(",");
    }


    if(!colors) {
	colors = this.display.getColorTable(true);
    }
    this.colors = colors;


    if(this.hasField() && !colors) {
//	this.index = -1;
//	return;
    }
    let scolorScale = this.display.getColorScale();
    if(scolorScale) {
	//"9,14.99,palegreen,darkgreen;15,19.99,#ffc966,#ffa500;20, 24.99,red,darkred;25, 27.99,mediumpurple,purple";
	this.colorScale = [];
	scolorScale.split(";").forEach(tok=>{
	    let toks=tok.split(',');
	    this.colorScale.push({
		min:+toks[0],
		max:+toks[1],		
		color1:toks[2],
		color2:toks[3]
	    });
	});
	this.colorScaleInterval = (d) => {
	    for(let i=0;i<this.colorScale.length;i++) {
		let scale = this.colorScale[i];
		if (d <= scale.max) {
		    return d3.scaleLinear().domain([scale.min, scale.max]).range([scale.color1, scale.color2])(d);
		}
	    }
	    if(!isNaN(d))
		console.log('color scale miss',d);
	    return null;
	};
    }



    if (!this.colors && this.display.colors && this.display.colors.length > 0) {
        this.colors = source.colors;
        if (this.colors.length == 1 && Utils.ColorTables[this.colors[0]]) {
            this.colors = Utils.ColorTables[this.colors[0]].colors;
        }
    }

    if (this.colors == null) {
        this.colors = Utils.ColorTables.inversegrayscale.colors;
    }


    if(!this.field) {
	for (var i = 0; i < fields.length; i++) {
            var field = fields[i];
            if (field.getId() == this.id || ("#" + (i + 1)) == this.id) {
		this.field = field;
            }
	}
    }

    if(!this.field) {
	if(this.id == "hour")
	    this.timeField="hour";
	else if(this.id == "day")
	    this.timeField="day";	
    }


    if(this.field && this.field.isString()) this.isString = true;
    this.index = this.field != null ? this.field.getIndex() : -1;
    this.stringMap = this.display.getColorByMap(colorByMapProp);
    let uniqueValues = [];
    let seenValue = {};
    if(this.index>=0 || this.timeField) {
	let min = NaN;
	let max = NaN;
	records.forEach((record,idx)=>{
            let tuple = record.getData();
	    let v;
            if(this.timeField) {
		if(this.timeField=="hour")
		    v = record.getTime().getHours();
		else
		    v = record.getTime().getTime();
	    } else {
		v = tuple[this.index];		
	    }
            if (this.isString) {
		if(!seenValue[v]) {
		    seenValue[v] = true;
		    uniqueValues.push(v);
		}
		return;
	    }
            if (this.excludeZero && v === 0) {
		return;
            }
	    min = Utils.min(min,v);
	    max = Utils.max(max,v);
	});
	this.minValue =min;
	this.maxValue =max;	
	this.origRange = [min,max];
    }

    if(uniqueValues.length>0) {
	uniqueValues.sort((a,b)=>{
	    return a.toString().localeCompare(b.toString());
	});
	uniqueValues.forEach(v=>{
	    if (!Utils.isDefined(this.colorByMap[v])) {
		let index = this.colorByValues.length;
                let color;
		if(this.lastColorByMap && this.lastColorByMap[v]) {
		    color = this.lastColorByMap[v];
		    //			console.log("\tlast v:" + v +" c:" + color);
		} 	else {
		    if(index>=this.colors.length) {
			this.colorOverflow = true;
			index = index%this.colors.length;
			//			    console.log("\tmod index:" + index +" l:" + this.colors.length);
		    }
		    color = this.colors[index];
		    //			console.log("\tindex:" + index +" v:" + v +" c:" + color);
		}
                this.colorByValues.push({value:v,color:color});
		this.colorByMap[v] = color;
                this.setRange(1,  this.colorByValues.length, true);
	    }
	});
    }

    if (this.display.showPercent) {
        this.setRange(0, 100,true);
    }

    var steps = this.getProperty("Steps");

    if(steps) {
	this.steps = steps.split(",");
    }



    this.colorByLog = this.getProperty("Log", false);
    this.colorByLog10 = this.getProperty("Log10", false);
    this.colorByLog2 = this.getProperty("Log2", false);
    if(this.colorByLog) {
	this.colorByFunc = Math.log;
    }   else if(this.colorByLog10) {
	this.colorByFunc = Math.log10;
    }   else if(this.colorByLog2) {
	this.colorByFunc = Math.log2;
    }

    this.setRange(this.getProperty("Min", this.minValue),
		  this.getProperty("Max", this.maxValue), true);

    this.range = this.maxValue - this.minValue;
    this.toMinValue = this.getProperty("ToMin", this.toMinValue);
    this.toMaxValue = this.getProperty("ToMax", this.toMaxValue);
    this.enabled = this.timeField!=null || (this.getProperty("doColorBy",true) && this.index>=0);
    this.initDisplayCalled = false;
}



ColorByInfo.prototype = {
    initDisplay: function() {
	this.filterHighlight = this.display.getFilterHighlight();
	this.initDisplayCalled = true;
    },
    getProperty: function(prop, dflt, debug) {
	if(this.properties[prop]) return this.properties[prop];
	if(this.debug) console.log("getProperty:" + prop);
	for(let i=0;i<this.propPrefix.length;i++) {
	    this.display.debugGetProperty = debug;
	    if(this.debug) console.log("\t" + this.propPrefix[i]+prop);
	    let v = this.display.getProperty(this.propPrefix[i]+prop);
	    this.display.debugGetProperty = false;
	    if(Utils.isDefined(v)) return v;
	}
	return dflt;
    },
    isEnabled: function() {
	return this.enabled ||this.getDoCount();
    },
    getField: function() {
	return this.field;
    },
    getColors: function() {
	return this.colors;
    },    
    displayColorTable: function(width,force, domId) {
	if(!this.getProperty('showColorTable',true)) return;
	domId = domId??ID_COLORTABLE;
	if(this.compareFields.length>0) {
	    let legend = "";
	    this.compareFields.forEach((f,idx)=>{
		legend += HtmlUtils.div([STYLE,HU.css('display','inline-block','width','15px','height','15px','background', this.colors[idx])]) +" " +
		    f.getLabel() +" ";
	    });
	    let dom = this.display.jq(domId);
	    dom.html(HtmlUtils.div([STYLE,HU.css('text-align','center','margin-top','5px')], legend));
	}
	if(!force && this.index<0) return;
	if(this.colorScale) {
	    let html = '<table width=100%><tr>';
	    let steps = 4;
	    let w = Math.round(parseFloat(100/(this.colorScale.length*steps)))+'%'
	    this.colorScale.forEach(s=>{
		for(let step=0;step<steps;step++) {
		    let value = s.min+(s.max-s.min)/steps*step;
		    let c =this.colorScaleInterval(value);
		    let contents='&nbsp';
		    if(step==0)
			contents =s.min;
		    else if(step==steps-1)
			contents =s.max;		    
		    let fg = Utils.getForegroundColor(c);
		    html+=HU.tag('td',[ATTR_CLASS,'display-colorscale-item',ATTR_TITLE,value,ATTR_WIDTH,w,ATTR_STYLE,HU.css('color',fg,'background',c)],contents);		    
		}
	    });
	    html += '</tr></table>';
	    this.display.displayColorTableHtml(html,domId);
	    return;
	}
	if(this.stringMap) {
	    let colors = [];
	    this.colorByValues= [];
	    for (var i in this.stringMap) {
		let color = this.stringMap[i];
		this.colorByValues.push({value:i,color:color});
		colors.push(color);
	    }
	    this.display.displayColorTable(colors, domId, this.origMinValue, this.origMaxValue, {
		field: this.field,
		colorByInfo:this,
		width:width,
		stringValues: this.colorByValues});
	} else {
	    let colors = this.colors;
	    if(this.getProperty("clipColorTable",true) && this.colorByValues.length) {
		var tmp = [];
		for(var i=0;i<this.colorByValues.length && i<colors.length;i++) 
		    tmp.push(this.colors[i]);
		colors = tmp;
	    }
	    let cbs = this.colorByValues.map(v=>{return v;});
	    cbs.sort((a,b)=>{
		return a.value.toString().localeCompare(b.value.toString());
	    });
	    let getValue = v=>{
		if(this.doingDates) return new Date(v);
		return v;
	    }
	    this.display.displayColorTable(colors, domId, getValue(this.origMinValue),
					   getValue(this.origMaxValue), {
		label:this.getDoCount()?'Count':null,
		field: this.field,
		colorByInfo:this,
		width:width,
		stringValues: cbs
	    });
	}
    },
    resetRange: function() {
	if(this.origRange) {
	    this.setRange(this.origRange[0],this.origRange[1]);
	}
    },
    setRange: function(minValue,maxValue, force) {
	if(this.display.getColorByAllRecords() && !force) {
	    return;
	}
	if(this.display.getProperty("useDataForColorRange") && this.origRange) {
	    minValue = this.origRange[0];
	    maxValue = this.origRange[1];	    
	}	    

	if(displayDebug.colorTable)
	    console.log(" setRange: min:" + minValue + " max:" + maxValue);
	if(!force && this.overrideRange) return;
	this.origMinValue = minValue;
	this.origMaxValue = maxValue;


	if (this.colorByFunc) {
	    if (minValue < 0) {
		this.colorByOffset =  -minValue;
	    } else if(minValue == 0) {
		this.colorByOffset =  1;
	    }
//	    if(minValue>0)
		minValue = this.colorByFunc(minValue + this.colorByOffset);
//	    if(maxValue>0)
		maxValue = this.colorByFunc(maxValue + this.colorByOffset);
	}
	this.minValue = minValue;
	this.maxValue = maxValue;
	this.range = this.maxValue -this.minValue;
	if(!this.origRange) {
	    this.origRange = [minValue, maxValue];
	}
//	console.log("min/max:" + this.minValue +" " + this.maxValue);
    },
    getValuePercent: function(v) {
	let perc =   (v - this.minValue) / this.range;
	if(this.inverse) perc = 1-perc;
	return perc;
    },
    scaleToValue: function(v) {
	let perc = this.getValuePercent(v);
	return this.toMinValue + (perc*(this.toMaxValue-this.toMinValue));
    },
    getDoCount:function() {
	return this.doCount;
    },
    getDoTotal:function() {
	return this.doTotalCount;
    },
    getDoEnum:function() {
	return this.doEnum;
    },
    setDoEnum:function(v) {
	this.isString=true;
	this.doEnum=v;
    },            
    setDoTotal:function(v) {
	this.doTotalCount=v;
    },
    setDoCount:function(min,max) {
	this.doCount = true;
        this.minValue = min;
	this.maxValue = max;
	this.range = this.maxValue-this.minValue;
	this.origMinValue=min;
	this.origMaxValue=max;
    },
    isValueOk:function(v) {
	if(this.properties.hasMinValue && v<this.properties.minValue) return false;
	if(this.properties.hasMaxValue && v>this.properties.maxValue) return false;		    
	if(isNaN(v)) return false;
	return true;
    },
    doAverage:function(records) {
	let total = 0;
	let cnt = 0;
	records.forEach(r=>{
	    let v = r.getData()[this.index];
	    if(!this.isValueOk(v)) return;
	    total+= v;
	    cnt++;
	});
	return  total/cnt;
    },
    processEnum:function(records) {
	let counts={};
	records.forEach(r=>{
	    let v = r.getData()[this.index];
	    if(!counts[v]) {
		counts[v] = 0;
	    }
	    counts[v]++;
	});
	let maxCount=0;
	let maxValue = null;
	
	Object.keys(counts).forEach(v=>{
	    if(counts[v]>maxCount) {
		maxValue=v;
		maxCount=counts[v];
	    }
	});
	return  maxValue;
    },

    doTotal:function(records) {
	let total = 0;
	let cnt = 0;
	records.forEach(r=>{
	    let v = r.getData()[this.index];
	    if(!this.isValueOk(v)) return;
	    total+= v;
	    cnt++;
	});
	return  total;
    },    
    getColorFromRecord: function(record, dflt, checkHistory,debug) {
	this.lastValue = NaN;
	if(!this.initDisplayCalled)   this.initDisplay();
	if(this.filterHighlight && !record.isHighlight(this.display)) {
	    return this.display.getUnhighlightColor();
	}

	let records = record;
	if(!Array.isArray(records)) records=[records];
	else record = records[0];
	if(this.colorThresholdField && this.display.selectedRecord) {
	    let v=this.display.selectedRecord.getValue(this.colorThresholdField.getIndex());
	    let v2=records[0].getValue(this.colorThresholdField.getIndex());
	    if(v2>v) return this.aboveColor;
	    else return this.belowColor;
	}

	if (this.index >= 0 || this.getDoCount()) {
	    let value;
	    if(this.getDoEnum()) {
		value = this.processEnum(records);
	    } else if(records.length>1) {
		value =  this.getDoTotal()?this.doTotal(records):this.doAverage(records);
	    } else {
		value= records[0].getData()[this.index];
	    }
	    //check if it is a date
	    if(value?.getTime) {
		value = value.getTime();
		this.doingDates = true;
	    }
	    value = this.getDoCount()?records.length:value;
	    this.lastValue = value;
	    return  this.getColor(value, record,checkHistory);
	} else if(this.timeField) {
	    let value;
	    if(this.timeField=="hour") {
		value = records[0].getTime().getHours();
	    }  else {
		value = records[0].getTime().getTime();
	    }
	    this.lastValue = value;
//	    console.log(value);
	    return  this.getColor(value, records[0],checkHistory);
	} 
	if(this.fieldValue == "year") {
	    if(records[0].getDate()) {
		let value = records[0].getDate().getUTCFullYear();
		this.lastValue = value;
		return this.getColor(value, records[0]);
	    }
	}
	return dflt;
    },
    hasField: function() {
	return this.index>=0;
    },
    getColor: function(value, pointRecord, checkHistory) {
	if(this.colorScaleInterval)
	    return this.colorScaleInterval(value);
	let c = this.getColorInner(value, pointRecord);
	if(c==null) c=this.nullColor;
	return c;
    },

    getColorInner: function(value, pointRecord,debug) {
//	if(debug) console.log(value);
	if(!this.initDisplayCalled)   this.initDisplay();

	if(this.filterHighlight && pointRecord && !pointRecord.isHighlight(this.display)) {
	    return this.display.getUnhighlightColor();
	}

	let percent = 0.5;
        if (this.showPercent) {
            let total = 0;
            let data = pointRecord.getData();
            for (let j = 0; j < data.length; j++) {
                let ok = this.fields[j].isNumeric() && !this.fields[j].isFieldGeo();
                if (ok && this.pctFields != null) {
                    ok = this.pctFields.indexOf(this.fields[j].getId()) >= 0 ||
                        this.pctFields.indexOf("#" + (j + 1)) >= 0;
                }
                if (ok) {
                    total += data[j];
                }
            }
            if (total != 0) {
                percent =  value / total * 100;
                percent = (percent - this.minValue) / (this.maxValue - this.minValue);
            }
        } else {
            let v = value;
	    if(this.stringMap) {
		let color = this.stringMap[value];
		if(!Utils.isDefined(color)) {
		    return this.stringMap["default"];
		}
		return color;
	    }
            if (this.isString) {
                color = this.colorByMap[v];
		if(color) return color;
            }
	    let tmp = v;
            v += this.colorByOffset;
            if (this.colorByFunc && v>0) {
                v = this.colorByFunc(v);
            }
            percent = this.range?(v - this.minValue) / this.range:0.5;
//	    console.log(this.display.getName(),v,percent,this.range,this.minValue);
//	    if(tmp>3 && tmp<6)
//		console.log("ov:" + tmp  +" v:" + v + " perc:" + percent);
        }


	let index=0;
	if(this.steps) {
	    for(;index<this.steps.length;index++) {
		if(value<=this.steps[index]) {
		    break;
		}
	    }
	} else {
	    index = parseInt(percent * this.colors.length);
	}
//	console.log("v:" + value +" index:" + index +" colors:" + this.colors);
        if (index >= this.colors.length) index = this.colors.length - 1;
        else if (index < 0) index = 0;
	if(this.stringMap) {
	    let color = this.stringMap[value];
	    if(!Utils.isDefined(color)) {
		return this.stringMap["default"];
	    }
	    return color;
	} else {
	    return this.colors[index];
	}
	return null;
    },
    convertColor: function(color, colorByValue) {
	color = this.convertColorIntensity(color, colorByValue);
	color = this.convertColorAlpha(color, colorByValue);
	return color;
    },
    convertColorIntensity: function(color, colorByValue) {
	if(!this.convertIntensity) return color;
	percent = (colorByValue-this.intensitySourceMin)/(this.intensitySourceMax-this.intensitySourceMin);
	intensity=this.intensityTargetMin+percent*(this.intensityTargetMax-this.intensityTargetMin);
	let result =  Utils.pSBC(intensity,color);
	//		    console.log(color +" " + result +" intensity:" + intensity +" min:" + this.intensityTargetM
	return result || color;
    },
    xcnt:0,
    convertColorAlpha: function(color, colorByValue) {
	if(this.hasAlphaMin) {
	    if(colorByValue<=this.alphaMin) {
		let result =  Utils.addAlphaToColor(color, 0.0);
		return result || color;
	    }

	}

	if(this.hasAlphaMax) {
	    if(colorByValue>=this.alphaMax) {
		let result =  Utils.addAlphaToColor(color, 0.0);
		return result || color;
	    }
	}	


	if(!this.convertAlpha) {
	    return color;
	}
	percent = (colorByValue-this.alphaSourceMin)/(this.alphaSourceMax-this.alphaSourceMin);
	alpha=this.alphaTargetMin+percent*(this.alphaTargetMax-this.alphaTargetMin);
	let result =  Utils.addAlphaToColor(color, alpha);
	return result || color;
    }
}


function SizeBy(display,records,fieldProperty) {
    this.display = display;
    if(!records) records = display.filterData();
    let pointData = this.display.getPointData();
    let fields = pointData?pointData.getRecordFields():[];
    $.extend(this, {
        id: this.display.getProperty(fieldProperty|| "sizeBy"),
        minValue: 0,
        maxValue: 0,
	threshold:parseFloat(this.display.getProperty('sizeByThreshold',NaN)),
        field: null,
        index: -1,
        isString: false,
        stringMap: {},
    });


    let sizeByMap = this.display.getProperty("sizeByMap");
    if (sizeByMap) {
        let toks = sizeByMap.split(",");
        for (let i = 0; i < toks.length; i++) {
            let toks2 = toks[i].split(":");
            if (toks2.length > 1) {
                this.stringMap[toks2[0]] = toks2[1];
            }
        }
    }

    for (let i = 0; i < fields.length; i++) {
        let field = fields[i];
        if (field.getId() == this.id || ("#" + (i + 1)) == this.id) {
            this.field = field;
	    if (field.isString()) this.isString = true;
        }
    }

    this.index = this.field != null ? this.field.getIndex() : -1;
    if (!this.isString && this.field) {
	let col = this.display.getColumnValues(records, this.field);
	this.minValue = col.min;
	this.maxValue =  col.max;
	if(Utils.isDefined(this.display.getProperty("sizeByMin"))) {
	    this.minValue = +this.display.getProperty("sizeByMin",0)
	}
	if(Utils.isDefined(this.display.getProperty("sizeByMax"))) {
	    this.maxValue = +this.display.getProperty("sizeByMax",0)
	}
    }

    if(this.display.getProperty("sizeBySteps")) {
	this.steps = [];
	this.display.getProperty("sizeBySteps").split(",").forEach(tuple=>{
	    let [value,size] = tuple.split(":");
	    this.steps.push({value:+value,size:+size});
	});
    }
    this.radiusMin = parseFloat(this.display.getProperty("sizeByRadiusMin", -1));
    this.radiusMax = parseFloat(this.display.getProperty("sizeByRadiusMax", -1));
    this.offset = 0;
    this.sizeByLog = this.display.getProperty("sizeByLog", false);
    this.origMinValue =   this.minValue;
    this.origMaxValue =   this.maxValue; 

    this.maxValue = Math.max(this.minValue,this.maxValue);
    if (this.sizeByLog) {
	this.func = Math.log;
        if (this.minValue < 1) {
            this.offset = 1 - this.minValue;
        }
        this.minValue = this.func(this.minValue + this.offset);
        this.maxValue = this.func(this.maxValue + this.offset);
    }
}

SizeBy.prototype = {
    getMaxSize:function() {
	return this.getSizeFromValue(this.origMaxValue);
    },
    getSize: function(values, dflt, func) {
        if (this.index <= 0) {
	    return dflt;
	}
        let value = values[this.index];
	let size = this.getSizeFromValue(value,func,false);
	return size;
    },

    getSizeFromValue: function(value,func, debug) {	
	if(this.steps) {
	    if(value<=this.steps[0].value) return this.steps[0].size;
	    for(let i=1;i<this.steps.length;i++) {
		if(value>this.steps[i-1].value && value<=this.steps[i].value ) return this.steps[i].size;
	    }
	    return this.steps[this.steps.length-1].size;
	}
        if (this.isString) {
	    let size;
            if (Utils.isDefined(this.stringMap[value])) {
                let v = parseInt(this.stringMap[value]);
                size = v;
            } else if (Utils.isDefined(this.stringMap["*"])) {
                let v = parseInt(this.stringMap["*"]);
                size = v;
            } 
	    if(isNaN(size)) size =  this.radiusMin;
	    if(func) func(NaN,size);
	    return size;
        } else {
            let denom = this.maxValue - this.minValue;
            let v = value + this.offset;
            if (this.func) v = this.func(v);
            let percent = (denom == 0 ? NaN : (v - this.minValue) / denom);
	    let size;
            if (this.radiusMax >= 0 && this.radiusMin >= 0) {
                size =  Math.round(this.radiusMin + percent * (this.radiusMax - this.radiusMin));
            } else {
                size = 6 + parseInt(15 * percent);
            }
	    if(debug) console.log("min:" + this.minValue +" max:" + this.maxValue+ " value:" + value +" percent:" + percent +" v:" + v +" size:" + size);
	    if(isNaN(size)) size =  this.radiusMin;
	    if(func) func(percent, size);
	    return size;
        }
    },
    getLegend: function(cnt,bg,vert) {
	let html = "";
	if(this.index<0) return "";
	if(this.steps) {
	    this.steps.forEach(step=>{
		let dim = step.size*2+"px";
		let v = step.value;
		html += HU.div([CLASS,"display-size-legend-item"], HU.center(HU.div([STYLE,HU.css("height", dim,"width",dim,
												  "background-color",bg||"#bbb",
												  "border-radius","50%"
												 )])) + v);

		if(vert) html+="<br>";
	    });
	} else {
	    for(let i=0;i<=cnt;i++) {
		let v = this.origMinValue+ i/cnt*(this.origMaxValue-this.origMinValue);
		let size  =this.getSizeFromValue(v,null,false);
		if(isNaN(size) || size==0) continue;
		v = this.display.formatNumber(v);
		let dim = size*2+"px";
		html += HU.div([CLASS,"display-size-legend-item"], HU.center(HU.div([STYLE,HU.css("height", dim,"width",dim,
												  "background-color",bg||"#bbb",
												  "border-radius","50%"
												 )])) + v);

		if(vert) html+="<br>";
	    }
	}
	return HU.div([CLASS,"display-size-legend"], html);
    }

}


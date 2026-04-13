/**
   Copyright (c) 2008-2026 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

var debugColorBy = false;


function ValueMapper(display,fieldProperty,propPrefix,theField,props) {
    this.properties = props || {};
    this.display = display;
    if(Utils.isDefined(this.properties.minValue)) this.properties.hasMinValue = true;
    if(Utils.isDefined(this.properties.maxValue)) this.properties.hasMaxValue = true;    

 
    if ( !propPrefix ) {
	propPrefix = [fieldProperty,""];
    } else if( !Array.isArray(propPrefix) ) {
	propPrefix = [propPrefix];
    }

    this.propPrefix=propPrefix;
    let valueAttr = this.getProperty(fieldProperty, null);
    if(theField==null) {
	if(fieldProperty.getId) {
	    theField = fieldProperty;
	} else {
	    theField = display.getFieldById(null, valueAttr);
	}
    }

    if(theField) {
	this.field = theField;
	valueAttr =theField.getId();
	propPrefix.unshift(theField.getId()+'.'+ fieldProperty);
	propPrefix.push(fieldProperty);
    }
    
    $.extend(this, {
        id: valueAttr,
	valueAttr:valueAttr,
	fieldValue:display.getProperty(fieldProperty),
	propPrefix: propPrefix,
	origRange:null,
	origMinValue:0,
	origMaxValue:0,
	valueOffset: 0,
        minValue: 0,
        maxValue: 0,
	toMinValue: 0,
        toMaxValue: 100
    });
}

ValueMapper.prototype = {
    processRecords:function(records) {
	this.uniqueValues = [];
	let seenValue = {};
	let min = NaN;
	let max = NaN;
	let values = [];
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
	    if(!isNaN(v)) {
		values.push(v);
	    }
	    if(!seenValue[v]) {
		seenValue[v] = true;
		if(this.isString || !isNaN(v)) {
		    this.uniqueValues.push(v);
		}
	    }

            if (this.isString) {
		return;
	    }
            if (this.excludeZero && v === 0) {
		return;
            }
	    min = Utils.min(min,v);
	    max = Utils.max(max,v);
	});

	if(this.isString) {
	    return;
	}
	this.minValue =min;
	this.maxValue =max;	
	this.origRange = [min,max];
	this.allValues = values;

	this.stats = new Stats(values);
	let clipMin=this.getProperty('ClipMin',this.stats.p5Value);
	if(String(clipMin).startsWith('p')) {
	    clipMin = parseFloat(clipMin.substring(1).trim())/100;
	    clipMin = Stats.quantile(this.stats.sorted, clipMin);
	}
	let clipMax=this.getProperty('ClipMax',this.stats.p5Value);
	if(String(clipMax).startsWith('p')) {
	    clipMax = parseFloat(clipMax.substring(1).trim())/100;
	    clipMax = Stats.quantile(this.stats.sorted, clipMax);
	}

	this.mapper = new Mapper(this.stats,
				 {method: this.getProperty('Method',this.getProperty('Function', MAPPER_METHOD.LINEAR)),
				  clipMin:clipMin,
				  clipMax:clipMax,
				  // boosts low values
				  gamma: this.getProperty('Gamma',0.5),
				  center: this.getProperty('Center',0),
				  clamp: this.getProperty('Clamp',true),
				  epsilon: 1e-9
				 });
	//	console.dir(this.stats.summary());
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
    getDoCount:function() {
	return this.doCount;
    },

    getValuePercent:function(value) {
	if(this.getDoCount()) {
	    if(this.range>0) {
		return value/this.range;
	    }
	    return 0.5;
	}
	if(this.isString) {
	    let index = this.uniqueValues.indexOf(value);
	    if(index<0) return NaN;
	    return   index / (this.uniqueValues.length - 1);
	}

	if(!this.mapper) {
	    console.log('No mapper defined',this.field);
	    return 0.5;

	}

	let percent = this.mapper.map(value);
	if(this.inverse) percent = 1-percent;
	return percent;
    }
    
}

function ColorByInfo(display, fields, records,
		     fieldProperty,colorByMapProp, defaultColorTable,
		     propPrefix, theField, props,lastColorBy) {

    let SUPER = new ValueMapper(display,fieldProperty??'colorBy', propPrefix,theField,props);
    $.extend(this, SUPER);
    $.extend(this, {
	colorHistory:{},
	fields:fields,
	colorThresholdField:display.getFieldById(null, display.getProperty('colorThresholdField')),
	literal:display.getProperty('colorByLiteral'),
	aboveColor: display.getProperty('colorThresholdAbove','red'),
	belowColor:display.getProperty('colorThresholdBelow','blue'),
	nullColor:display.getProperty('nullColor',null),	
	excludeZero:this.getProperty(PROP_EXCLUDE_ZERO, false),
	overrideRange: this.getProperty('overrideColorRange',false),
	inverse: this.getProperty('Inverse',false),
        isString: this.properties.isString,
        stringMap: null,
	colorByMap: {},
	colorByValues:[],
        pctFields:null,
	compareFields: display.getFieldsByIds(null, this.getProperty('CompareFields', '')),
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
    let colorTabelSteps = null;
    let typeProperty;
    if(this.field) {
	typeProperty= 'type_' + this.field.getType();
    }
    if(this.valueAttr) {
	let c = this.display.getProperty(this.valueAttr +".colors");
	if(c) colors = c.split(",");
    }
    if(typeProperty) {
	let c = this.display.getProperty(typeProperty +".colors");
	if(c) colors = c.split(",");
    }    

    if(defaultColorTable) {
	this.id = defaultColorTable.id;
    }

    if(!colors) {
	if(defaultColorTable) {
	    if(Array.isArray(defaultColorTable)) {
		colors = defaultColorTable;
	    } else {
		if(!defaultColorTable.steps) {
		    colors = defaultColorTable.colors;
		}
	    }
	} else {
	    colors =  this.display.getColorTable(true,[this.properties.colorTableProperty,
						       this.valueAttr +'.colorTable',
						       (typeProperty?typeProperty:'skip')+'.colorTable',
						       'colorTable']);
	}
    }
    if(!colors) {
	let colorTableObject  = defaultColorTable??
	    this.display.getColorTable(false,[this.properties.colorTableProperty,
					      this.valueAttr +".colorTable",
					      "colorTable"]);

	if(colorTableObject)
	    this.colorTableSteps = colorTableObject.steps;
    }

    
    //    if(!colors && this.hasField()) {
    //	colors = this.display.getColorTable(true,"colorTable");
    //    }

    if(!colors) {
	var c = this.getProperty(this.valueAttr +".colors");
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


    if(this.field) {
	if(this.field.isString()) this.isString = true;
//	else if(this.field.isBoolean()) this.isBoolean= true;
	else if(this.field.isBoolean()) this.isString=true;
    }
    this.index = this.field != null ? this.field.getIndex() : -1;
    this.stringMap = this.display.getColorByMap(colorByMapProp);
    if(this.index>=0 || this.timeField) {
	this.processRecords(records);
    } else {
//	console.log('not processing records');
    }


    if(this.isString && this.uniqueValues.length>0) {
	this.uniqueValues.sort((a,b)=>{
	    return a.toString().localeCompare(b.toString());
	});
	this.uniqueValues.forEach(v=>{
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

    var steps = this.getProperty("Steps");
    if(steps) {
	this.steps = steps.split(",");
    }

    this.setRange(this.getProperty("Min", this.minValue),
		  this.getProperty("Max", this.maxValue), true);

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
    getId:function() {
	return this.id;
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
		legend += HU.div([ATTR_STYLE,HU.css(CSS_DISPLAY,DISPLAY_INLINE_BLOCK,
						    CSS_WIDTH,HU.px(15),
						    CSS_HEIGHT,HU.px(15),
						    CSS_BACKGROUND, this.colors[idx])]) +" " +
		    f.getLabel() +" ";
	    });
	    let dom = this.display.jq(domId);
	    dom.html(HU.div([ATTR_STYLE,HU.css(CSS_TEXT_ALIGN,ALIGN_CENTER,
					       CSS_MARGIN_TOP,HU.px(5))], legend));
	}
	if(!force && this.index<0) return;
	if(this.colorScale) {
	    let html = HU.open(TAG_TABLE,[ATTR_WIDTH,HU.perc(100)]) + HU.open(TAG_TR);
	    let steps = 4;
	    let w = HU.perc(Math.round(parseFloat(100/(this.colorScale.length*steps))));
	    this.colorScale.forEach(s=>{
		for(let step=0;step<steps;step++) {
		    let value = s.min+(s.max-s.min)/steps*step;
		    let c =this.colorScaleInterval(value);
		    let contents=SPACE;
		    if(step==0)
			contents =s.min;
		    else if(step==steps-1)
			contents =s.max;		    
		    let fg = Utils.getForegroundColor(c);
		    html+=HU.tag(TAG_TD,[ATTR_CLASS,'display-colorscale-item',
					 ATTR_TITLE,value,
					 ATTR_WIDTH,w,
					 ATTR_STYLE,
					 HU.css(CSS_COLOR,fg,CSS_BACKGROUND,c)],contents);		    
		}
	    });
	    html += HU.close(TAG_TR, TAG_TABLE);
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
	    let cbs = null;
	    if(this.colorTableSteps) {
		colors = [];
		cbs=[];
		this.colorTableSteps.forEach(step=>{
		    colors.push(step.color);
		    cbs.push({value:step.label??(step.min+ ' - '+ step.max),color:step.color});
		});
	    }
	    if(this.getProperty("clipColorTable",true) && this.colorByValues.length) {
		var tmp = [];
		for(var i=0;i<this.colorByValues.length && i<colors.length;i++) 
		    tmp.push(this.colors[i]);
		colors = tmp;
	    }
	    if(cbs==null) {
		cbs = this.colorByValues.map(v=>{return v;});
		cbs.sort((a,b)=>{
		    return a.value.toString().localeCompare(b.value.toString());
		});
	    }
	    let getValue = v=>{
		if(this.doingDates) return new Date(v);
		return v;
	    }
	    let _this = this;
	    let inverseValueFunc = (minValue,maxValue,percent)=>{
		if(!_this.mapper) return NaN;
		return  _this.mapper.invert(percent);
	    }

	    this.display.displayColorTable(colors, domId,
					   getValue(this.origMinValue),
					   getValue(this.origMaxValue), {
					       label:this.getDoCount()?'Count':null,
					       field: this.field,
					       colorByInfo:this,
					       width:width,
					       stringValues: cbs,
					       getValueFunction:inverseValueFunc
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
	if(this.getProperty('useDataForColorRange') && this.origRange) {
	    minValue = this.origRange[0];
	    maxValue = this.origRange[1];	    
	}	    

	if(displayDebug.colorTable)
	    console.log(" setRange: min:" + minValue + " max:" + maxValue);
	if(!force && this.overrideRange) return;
	this.origMinValue = minValue;
	this.origMaxValue = maxValue;
	this.valueOffset = 0;
	this.minValue = minValue;
	this.maxValue = maxValue;
	if(this.stats) {
	    this.stats.min  = minValue;
	    this.stats.max  = maxValue;	    
	}
	if(!this.origRange) {
	    this.origRange = [minValue, maxValue];
	}
    },
    scaleToValue: function(v) {
	let perc = this.getValuePercent(v);
	return this.toMinValue + (perc*(this.toMaxValue-this.toMinValue));
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
	    record.setDisplayProperty(this.display.getId(),'colorByValue',value);
	    this.lastValue = value;
	    if(isNaN(value)) {
		if(this.nullColor) return this.nullColor;
	    }
	    let color =   this.getColor(value, record,checkHistory);
	    if(debugColorBy)	console.log(value,color)
	    return color;
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
	if(this.literal) {
	    value = String(value);
	    if(value.indexOf('(')) {
		value = value.replace(/\(.*\)/,'');
	    }
	    return value;
	}
	if(this.colorScaleInterval)
	    return this.colorScaleInterval(value);
	let c = this.getColorInner(value, pointRecord);
	if(c==null) c=this.nullColor;
	return c;
    },



    getColorInner: function(value, pointRecord,debug) {
	if(this.colorTableSteps) {
	    for(let i=0;i<this.colorTableSteps.length;i++) {
		let step = this.colorTableSteps[i];
		if(value>=step.min && value<=step.max) return step.color;
	    }
	    if(value<=this.colorTableSteps[0].min) return this.colorTableSteps[0].color;
	    if(value>=this.colorTableSteps[this.colorTableSteps.length-1].max)
		return this.colorTableSteps[this.colorTableSteps.length-1].color;	    

	}


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
	    if(this.isBoolean) {
		if(v===true || v=='yes') v=true;
		else v=false;
		if(this.colors.length>=2) {
		    return this.colors[v?1:0];
		}
		if(v) return 'green';
		return 'red';		
	    }
            if (this.isString) {
                color = this.colorByMap[v];
		if(color) return color;
            }
	    let tmp = v;
            v += this.valueOffset;
	    if(isNaN(v)) {
		return this.nullColor;
	    }	    
	    percent = this.getValuePercent(v);
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
	if(isNaN(index)) {
	    if(debugColorBy)
		console.log("v:" + value +" index:" + index +" null color:" + this.nullColor);
	    return this.nullColor;
	}
	//	    console.log("v:" + value +" index:" + index +" colors:" + this.colors);
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


function SizeBy(display,records,fieldProperty,args) {
    let SUPER = new ValueMapper(display,fieldProperty??'sizeBy');//, propPrefix,theField,props);
    args= args??{};
    $.extend(this, SUPER);
    if(!records) records = display.filterData();
    let pointData = this.display.getPointData();
    let fields = pointData?pointData.getRecordFields():[];
    $.extend(this, {
	threshold:parseFloat(this.getProperty('sizeByThreshold',NaN)),
	sizeToMinMax:new Map(),
        field: null,
        index: -1,
        isString: false,
        stringMap: {},
    });


    let sizeByMap = this.getProperty("sizeByMap");
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
    if (this.field) {
	this.processRecords(records);
	if(Utils.isDefined(this.getProperty("sizeByMin"))) {
	    this.minValue = +this.getProperty("sizeByMin",0)
	}
	if(Utils.isDefined(this.getProperty("sizeByMax"))) {
	    this.maxValue = +this.getProperty("sizeByMax",0)
	}
    }

    if(this.getProperty("sizeBySteps")) {
	this.steps = [];
	this.getProperty("sizeBySteps").split(",").forEach(tuple=>{
	    let [value,size] = tuple.split(":");
	    this.steps.push({value:+value,size:+size});
	});
    }
    this.radiusMin = parseFloat(this.getProperty("sizeByRadiusMin", this.getProperty("radiusMin",Utils.isDefined(args.radiusMin)?args.radiusMin:4)));
    this.radiusMax = parseFloat(this.getProperty("sizeByRadiusMax", this.getProperty("radiusMax",Utils.isDefined(args.radiusMax)?args.radiusMax:20)));
//    console.log('sizeby radius:',this.radiusMin,this.radiusMax);
    this.origMinValue =   this.minValue;
    this.origMaxValue =   this.maxValue; 
    this.maxValue = Math.max(this.minValue,this.maxValue);
}

SizeBy.prototype = {
    isEnabled:function() {
	return this.index>=0;
    },
    getMaxValueSize:function() {
	return this.getSizeFromValue(this.origMaxValue);
    },
    getField:function() {
        return this.field;
    },
    getSize: function(values, dflt, func) {
        if (this.index <= 0) {
	    return dflt;
	}
        let value = values[this.index];
	let size = this.getSizeFromValue(value,func,false);
	if(!this.sizeToMinMax[size]) {
	    this.sizeToMinMax[size]= {min:Number.MAX_VALUE,max:-Number.MAX_VALUE};
	}
	let minMax = this.sizeToMinMax[size];
	minMax.min = Math.min(minMax.min,value);
	minMax.max = Math.max(minMax.max,value);	
	minMax.value = value;
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
	let percent=NaN;
        if (this.isString) {
	    let size = NaN;
            if (Utils.isDefined(this.stringMap[value])) {
                size = parseInt(this.stringMap[value]);
            } else if (Utils.isDefined(this.stringMap["*"])) {
                size = parseInt(this.stringMap["*"]);
            } 
	    if(isNaN(size)) {
		percent  = this.getValuePercent(value);
		size =  this.radiusMin;
	    }
	    if(isNaN(percent)) {
		if(func) func(NaN,size);
		return size;
	    }
        } 
	if(isNaN(percent)) {
	    percent  = this.getValuePercent(value);
	}
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
    },
    showSettings: function(anchor) {
	let _this = this;
	let html ='';
	let colorByFunctions=MAPPER_METHOD_ALL.split('|');
	html+=HU.formTable();
	html +=HU.formEntry('Function:',
			    HU.select("",[ATTR_ID,'sizebyfunction'],colorByFunctions,
				      _this.display.getSizeByMethod(MAPPER_METHOD.LINEAR)));
	html +=HU.formEntry('Radius Range:',
			    HU.input("", _this.radiusMin,
				     [ATTR_SIZE, 7,
				      ATTR_ID, 'radiusmin']) + ' - ' +
			    HU.input("", _this.radiusMax,
				     [ATTR_SIZE, 7,
				      ATTR_ID, 'radiusmax']));

	html+=HU.formTableClose();
	html+=HU.makeApplyOkCancelButtons();
	html = HU.div([ATTR_STYLE,HU.css(CSS_PADDING,HU.px(8))], html);
	let display = _this.display;
	if(display.sizeByDialog) display.sizeByDialog.remove();

	let dialog =  display.sizeByDialog =
	    HU.makeDialog({
		content:html,
		title:'Size by Settings',
		anchor:anchor,
		draggable:true,
		header:true});
	let apply = ()=>{
	    let apply2 = (suffix,value) =>{
		_this.display.setProperty(suffix,value);
		if(_this.fieldValue) {
		    _this.display.setProperty(_this.fieldValue+'.'+suffix,value);
		}
	    }
	    apply2('sizeByMethod',dialog.find('#sizebyfunction').val());
	    apply2('sizeByRadiusMin',dialog.find('#radiusmin').val());
	    apply2('sizeByRadiusMax',dialog.find('#radiusmax').val());
	    _this.display.forceUpdateUI();
	}
	dialog.find(HU.dotClass(CLASS_BUTTON_APPLY)).button().click(()=>{
	    apply();
	});
	dialog.find(HU.dotClass(CLASS_BUTTON_OK)).button().click(()=>{
	    apply();
	    dialog.remove();
	});
	dialog.find(HU.dotClass(CLASS_BUTTON_CANCEL)).button().click(()=>{
	    dialog.remove();
	});

    },
    initLegend: function(selector) {
	let _this = this;
	selector.find('.display-size-legend-item').click(function() {
	    _this.showSettings($(this));
	});
    },
    getLegend: function(cnt,bg,vert) {
	let html = "";
	if(this.index<0) return "";
	if(this.steps) {
	    this.steps.forEach(step=>{
		let dim = HU.px(step.size*2);
		let v = step.value;
		html += HU.div([ATTR_CLASS,"display-size-legend-item",ATTR_TITLE,'Click for settings dialog'],
			       HU.center(HU.div([ATTR_STYLE,HU.css(CSS_HEIGHT, dim,CSS_WIDTH,dim,
								   CSS_BACKGROUND_COLOR,bg??"#bbb",
								   CSS_BORDER_RADIUS,HU.perc(50)
								  )])) + v);

		if(vert) html+=HU.br();
	    });
	} else {	
	    let sorted = Object.keys(this.sizeToMinMax).sort((a,b)=> a-b);
	    const step = (sorted.length - 1) / (cnt - 1);
//	    console.log(this.sizeToMinMax);
	    let seen = {};
	    for (let i = 0; i < cnt; i++) {
		let index = Math.round(i * step);
		if(seen[index]) continue;
		seen[index] = true;
		let size = sorted[index];
		let minMax = this.sizeToMinMax[size];
		if(!minMax) continue;
		let min = minMax.min;
		let max = this.sizeToMinMax[size].max;		
		if(isNaN(size) || size==0) continue;
		let label;
		if(this.isString) {
		    label = minMax.value;
		    if(!Utils.stringDefined(label)) label='&lt;blank&gt;';
		} else {
		    label =  this.display.formatNumber(min) +' - '+
			this.display.formatNumber(max);
		    if(isNaN(min)) {
			label = '--';
		    } else if(min==max) {
			label = min;
		    }
		}
		let dim = HU.px(size*2);
		html += HU.div([ATTR_CLASS,'display-size-legend-item',
			       ATTR_TITLE,'Click for settings dialog'],
			       HU.center(HU.div([ATTR_STYLE,HU.css(CSS_HEIGHT, dim,
								   CSS_WIDTH,dim,
								   CSS_BACKGROUND_COLOR,bg??"#bbb",
								   CSS_BORDER_RADIUS,HU.perc(50)
								  )])) + label);

		if(vert) html+=HU.br();
	    }
	}
	return HU.div([ATTR_CLASS,"display-size-legend"], html);
    }

}


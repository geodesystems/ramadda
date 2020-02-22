/**
Copyright 2008-2019 Geode Systems LLC
*/




function AreaWidget(display) {
    var ID_CONTAINS = "contains";
    var ID_NORTH = "north";
    var ID_SOUTH = "south";
    var ID_EAST = "east";
    var ID_WEST = "west";

    var ID_AREA_LINK = "arealink";

    RamaddaUtil.inherit(this, {
        display: display,
        getHtml: function() {
            var callback = this.display.getGet();
            //hack, hack
            var cbx = HtmlUtils.checkbox(this.display.getDomId(ID_CONTAINS), ["title", "Search mode: checked - contains, unchecked - overlaps"], false);
            var link = HtmlUtils.onClick(callback + ".areaWidget.areaLinkClick();", HtmlUtils.image(root + (this.linkArea ? "/icons/link.png" : "/icons/link_break.png"), [ATTR_TITLE, "Set bounds from map", ATTR_CLASS, "display-area-link", "border", "0", ATTR_ID, this.display.getDomId(ID_AREA_LINK)]));

            var mylocation = HtmlUtils.onClick(callback + ".areaWidget.useMyLocation();", HtmlUtils.image(root + "/icons/compass.png"), [ATTR_TITLE, "Set my location", ATTR_CLASS, "display-area-link", "border", "0"]);


            var erase = HtmlUtils.onClick(callback + ".areaWidget.areaClear();", HtmlUtils.image(root + "/icons/eraser.png", [ATTR_TITLE, "Clear form", ATTR_CLASS, "display-area-link", "border", "0"]));

            var areaForm = HtmlUtils.openTag(TAG_TABLE, [ATTR_CLASS, "display-area", "border", "0", "cellpadding", "0", "cellspacing", "0"]);
            areaForm += HtmlUtils.tr([],
                HtmlUtils.td(["align", "center"],
                    HtmlUtils.leftCenterRight(mylocation,
                        HtmlUtils.input(ID_NORTH, "", ["placeholder", " N", ATTR_CLASS, "input display-area-input", "size", "5", ATTR_ID,
                            this.display.getDomId(ID_NORTH), ATTR_TITLE, "North"
                        ]), link, "20%", "60%", "20%")));

            areaForm += HtmlUtils.tr([], HtmlUtils.td([],
                HtmlUtils.input(ID_WEST, "", ["placeholder", " W", ATTR_CLASS, "input  display-area-input", "size", "5", ATTR_ID,
                    this.display.getDomId(ID_WEST), ATTR_TITLE, "West"
                ]) +
                HtmlUtils.input(ID_EAST, "", ["placeholder", " E", ATTR_CLASS, "input  display-area-input", "size", "5", ATTR_ID,
                    this.display.getDomId(ID_EAST), ATTR_TITLE, "East"
                ])));
            areaForm += HtmlUtils.tr([],
                HtmlUtils.td(["align", "center"],
                    HtmlUtils.leftCenterRight(erase, HtmlUtils.input(ID_SOUTH, "", ["placeholder", " S", ATTR_CLASS, "input  display-area-input", "size", "5", ATTR_ID,
                        this.display.getDomId(ID_SOUTH), ATTR_TITLE, "South"
                    ]), cbx)));

            areaForm += HtmlUtils.closeTag(TAG_TABLE);
            return areaForm;
        },
        areaClear: function() {
            $("#" + this.display.getDomId(ID_NORTH)).val("");
            $("#" + this.display.getDomId(ID_WEST)).val("");
            $("#" + this.display.getDomId(ID_SOUTH)).val("");
            $("#" + this.display.getDomId(ID_EAST)).val("");
            this.display.areaClear();
        },
        useMyLocation: function() {
            if (navigator.geolocation) {
                var _this = this;
                navigator.geolocation.getCurrentPosition(function(position) {
                    _this.setUseMyLocation(position);
                });
            } else {}
        },
        setUseMyLocation: function(position) {
            var lat = position.coords.latitude;
            var lon = position.coords.longitude;
            var offset = 5.0;
            if (this.display.myLocationOffset)
                offset = parseFloat(this.display.myLocationOffset);

            $("#" + this.display.getDomId(ID_NORTH)).val(lat + offset);
            $("#" + this.display.getDomId(ID_WEST)).val(lon - offset);
            $("#" + this.display.getDomId(ID_SOUTH)).val(lat - offset);
            $("#" + this.display.getDomId(ID_EAST)).val(lon + offset);
            if (this.display.submitSearchForm)
                this.display.submitSearchForm();
        },
        areaLinkClick: function() {
            this.linkArea = !this.linkArea;
            var image = root + (this.linkArea ? "/icons/link.png" : "/icons/link_break.png");
            $("#" + this.display.getDomId(ID_AREA_LINK)).attr("src", image);
            if (this.linkArea && this.lastBounds) {
                var b = this.lastBounds;
                $("#" + this.display.getDomId(ID_NORTH)).val(formatLocationValue(b.top));
                $("#" + this.display.getDomId(ID_WEST)).val(formatLocationValue(b.left));
                $("#" + this.display.getDomId(ID_SOUTH)).val(formatLocationValue(b.bottom));
                $("#" + this.display.getDomId(ID_EAST)).val(formatLocationValue(b.right));
            }
        },
        linkArea: false,
        lastBounds: null,
        handleEventMapBoundsChanged: function(source, args) {
            bounds = args.bounds;
            this.lastBounds = bounds;
            if (!args.force && !this.linkArea) return;
            $("#" + this.display.getDomId(ID_NORTH)).val(formatLocationValue(bounds.top));
            $("#" + this.display.getDomId(ID_WEST)).val(formatLocationValue(bounds.left));
            $("#" + this.display.getDomId(ID_SOUTH)).val(formatLocationValue(bounds.bottom));
            $("#" + this.display.getDomId(ID_EAST)).val(formatLocationValue(bounds.right));
        },
        setSearchSettings: function(settings) {
            var cbx = $("#" + this.display.getDomId(ID_CONTAINS));
            if (cbx.is(':checked')) {
                settings.setAreaContains(true);
            } else {
                settings.setAreaContains(false);
            }
            settings.setBounds(this.display.getFieldValue(this.display.getDomId(ID_NORTH), null),
                this.display.getFieldValue(this.display.getDomId(ID_WEST), null),
                this.display.getFieldValue(this.display.getDomId(ID_SOUTH), null),
                this.display.getFieldValue(this.display.getDomId(ID_EAST), null));
        },
    });
}




function DateRangeWidget(display) {
    var ID_DATE_START = "date_start";
    var ID_DATE_END = "date_end";

    RamaddaUtil.inherit(this, {
        display: display,
        initHtml: function() {
            this.display.jq(ID_DATE_START).datepicker();
            this.display.jq(ID_DATE_END).datepicker();
        },
        setSearchSettings: function(settings) {
            var start = this.display.jq(ID_DATE_START).val();
            var end = this.display.jq(ID_DATE_START).val();
            settings.setDateRange(start, end);
        },
        getHtml: function() {
            var html = HtmlUtils.input(ID_DATE_START, "", ["class", "display-date-input", "placeholder", " start date", ATTR_ID,
                    display.getDomId(ID_DATE_START), "size", "10"
                ]) + " - " +
                HtmlUtils.input(ID_DATE_END, "", ["class", "display-date-input", "placeholder", " end date", ATTR_ID,
                    display.getDomId(ID_DATE_END), "size", "10"
                ]);
            return html;
        }
    });
}



function DisplayAnimation(display, enabled) {
    let ID_RUN = "animrun";
    let ID_NEXT = "animnext";
    let ID_PREV= "animprev";
    let ID_BEGIN= "animbegin";
    let ID_END= "animend";
    let ID_SLIDER = "slider";
    let ID_TICKS = "ticks";
    let ID_SHOWALL = "showall";
    let ID_ANIMATION_LABEL = "animationlabel";
    let MODE_FRAME = "frame";
    let MODE_SLIDING = "sliding";
    this.display = display;
    $.extend(this,{
	enabled: enabled,
        running: false,
        inAnimation: false,
        begin: null,
        end: null,
        dateMin: null,
        dateMax: null,
        dateRange: 0,
        dateFormat: display.getProperty("animationDateFormat", "yyyyMMdd"),
        mode: display.getProperty("animationMode", "cumulative"),
        startAtEnd: display.getProperty("animationStartAtEnd", false),
        speed: parseInt(display.getProperty("animationSpeed", 500)),
	getEnabled: function() {
	    return this.enabled;
	},
        toggleAnimation: function() {
	    this.running = !this.running;
	    if(this.btnRun)
		this.btnRun.html(HtmlUtils.getIconImage(this.running ? "fa-stop" : "fa-play"));
	    if (this.running)
		this.startAnimation();
	},
        getDomId: function(id) {
	    return this.display.getDomId(id);
	},
	jq: function(id) {
	    return this.display.jq(id);
	},
	init: function(dateMin, dateMax, records) {
	    let debug = false;
	    if(debug)
		console.log("animation.init:" +dateMin +" " + dateMax +" " +(records?"#records:" + records.length: "no records") );
	    let _this = this;
	    this.records = records;
	    this.dateMin = dateMin;
	    this.dateMax = dateMax;
	    this.begin = this.dateMin;
	    this.end = this.dateMax;
	    if(!this.dateMin) return;
	    this.dates=[];
	    var seen = {};
	    records.map(r=>{
		if(!seen[r.getDate()]) {
		    seen[r.getDate()] = true;
		    this.dates.push(r.getDate());
		}
	    });
	    this.dates.sort(function(a,b) {
		return a.getTime() - b.getTime();
	    });
	    
	    this.frameIndex = 0;
	    if(this.startAtEnd) {
		this.begin = this.dateMax;
		this.end = this.dateMax;
		if (this.mode == MODE_FRAME) {
		    this.frameIndex = this.dates.length-1;
		}		    
	    }
	    if (this.mode == MODE_FRAME) {
		this.end = this.begin;
	    }

	    
            this.dateRange = this.dateMax.getTime() - this.dateMin.getTime();
	    this.steps= parseFloat(this.display.getProperty("animationSteps", 60));
	    this.windowUnit = this.display.getProperty("animationWindow", "");

	    if (this.windowUnit != "") {
		if (this.windowUnit == "decade") {
		    this.window = 1000 * 60 * 60 * 24 * 365 * 10;// + 1000 * 60 * 60 * 24 * 365;
		} else 	if (this.windowUnit == "century") {
		    this.window = 1000 * 60 * 60 * 24 * 365 * 100;// + 1000 * 60 * 60 * 24 * 365;
		} else 	if (this.windowUnit == "halfdecade") {
		    this.window = 1000 * 60 * 60 * 24 * 365 * 5;// + 1000 * 60 * 60 * 24 * 365;
		} else if (this.windowUnit == "year") {
		    this.window = 1000 * 60 * 60 * 24 * 366;
		} else if (this.windowUnit == "month") {
		    this.window = 1000 * 60 * 60 * 24 * 32;
		} else if (this.windowUnit == "week") {
		    this.window = 1000 * 60 * 60 * 24*7;
		} else if (this.windowUnit == "day") {
		    this.window = 1000 * 60 * 60 * 24;
		} else if (this.windowUnit == "hour") {
		    this.window = 1000 * 60 * 60;
		} else if (this.windowUnit == "minute") {
		    this.window = 1000 * 61;
		} else {
		    this.window = 1001;
		}
	    } else if(this.steps>0){
		this.window = this.dateRange / this.steps;
	    }
	    var sliderValues = this.mode != MODE_FRAME?[this.begin.getTime(),this.end.getTime()]:[this.begin.getTime()];
	    this.jq(ID_SLIDER).slider({
		range: _this.mode != MODE_FRAME,
		min: _this.dateMin.getTime(),
		max: _this.dateMax.getTime(),
		values: sliderValues,
		slide: function( event, ui ) {
		    _this.stopAnimation();
		    _this.setSliderValues(ui.values);
		    _this.updateLabels();
		},
		stop: function(event,ui) {
		    _this.stopAnimation();
		    _this.setSliderValues(ui.values);
		    _this.applyAnimation(true);
		}
	    });

	    if(this.records && this.display.getProperty("animationShowTicks",true)) {
		let tickStyle = this.display.getProperty("animationTickStyle","");
		var ticks = "";
		var min = this.dateMin.getTime();
		var max = this.dateMax.getTime();
		var p = 0;
		for(var i=0;i<this.records.length;i++) {
		    var record = this.records[i];
		    var date = record.getDate().getTime();
		    var perc = (date-min)/(max-min)*100;
		    var tt = this.formatAnimationDate(record.getDate());
		    ticks+=HtmlUtils.div(["id",this.display.getId()+"-"+record.getId(), "class","display-animation-tick","style","left:" + perc+"%;"+tickStyle,"title",tt,"recordIndex",i],"");
		}
		this.jq(ID_TICKS).html(ticks);
		this.display.makeTooltips(this.jq(ID_TICKS).find(".display-animation-tick"), this.records,(open,record) =>{
		    this.display.handleEventRecordHighlight(this, {highlight: open,record:record, skipAnimation:true});
		});
	    }

	    this.updateLabels();
	},
	setSliderValues: function(v) {
	    let debug = true;
	    if(debug)
		console.log("animtion.setSliderValues");

	    if(this.mode != MODE_FRAME) {
		this.begin = new Date(v[0]);
		this.end = new Date(v[1]);
	    } else {
		var sliderDate = new Date(v[0]);
		var closest = this.dates[0];
		var dist = 0;
		this.dates.map(d=>{
		    if(Math.abs(d.getTime()-sliderDate.getTime()) < Math.abs(closest.getTime()-sliderDate.getTime())) {
			closest = d;
		    }
		});
		this.begin = this.end = closest;
	    }
	},
        handleEventRecordHighlight: function(source, args) {
	    var element = $("#" + this.display.getId()+"-"+args.record.getId());
	    //	    console.log(args.highlight +" " + element.length);
	    if(args.highlight) {
		element.addClass("display-animation-tick-highlight");
	    } else {
		element.removeClass("display-animation-tick-highlight");
	    }
	},
	makeControls:function() {
            var buttons =  "";
	    if(this.display.getProperty("animationShowButtons",true)) {
		var short = display.getProperty("animationWidgetShort",false);
		if(!short)
		    buttons +=   HtmlUtils.span(["id", this.getDomId(ID_BEGIN),"title","Go to beginning"], HtmlUtils.getIconImage("fa-fast-backward")); 
		buttons += HtmlUtils.span(["id", this.getDomId(ID_PREV), "title","Previous"], HtmlUtils.getIconImage("fa-step-backward")); 
		if(!short)
		    buttons +=HtmlUtils.span(["id", this.getDomId(ID_RUN),  "title","Run/Stop"], HtmlUtils.getIconImage("fa-play")); 
		buttons +=HtmlUtils.span(["id", this.getDomId(ID_NEXT), "title","Next"], HtmlUtils.getIconImage("fa-step-forward"));
		if(!short)
		    buttons +=HtmlUtils.span(["id", this.getDomId(ID_END), "title","Go to end"], HtmlUtils.getIconImage("fa-fast-forward"));
		if(!short)
		    buttons += HtmlUtils.span(["id", this.getDomId(ID_SHOWALL), "title","Show all"], HtmlUtils.getIconImage("fa-sync"));
	    }
	    buttons+=HtmlUtils.span(["id", this.getDomId(ID_ANIMATION_LABEL), "class", "display-animation-label"]);
            buttons = HtmlUtils.div([ "class","display-animation-buttons"], buttons);
	    if(display.getProperty("animationShowSlider",true)) {
		let style= display.getProperty("animationSliderStyle","");
		buttons +=   HtmlUtils.div(["class","display-animation-slider","style",style,"id",this.getDomId(ID_SLIDER)],
					  HtmlUtils.div(["class","display-animation-ticks","id",this.getDomId(ID_TICKS)]));
	    }
	    

            this.jq(ID_TOP_LEFT).append(HtmlUtils.div(["style",this.display.getProperty("animationStyle")], buttons));
            this.btnRun = this.jq(ID_RUN);
            this.btnPrev = this.jq(ID_PREV);
            this.btnNext = this.jq(ID_NEXT);
            this.btnBegin = this.jq(ID_BEGIN);
            this.btnEnd = this.jq(ID_END);
            this.btnShowAll = this.jq(ID_SHOWALL);
            this.label = this.jq(ID_ANIMATION_LABEL);
            this.btnRun.button().click(() => {
                this.toggleAnimation();
            });
            this.btnBegin.button().click(() => {
		this.begin = this.dateMin;
		if (this.mode == MODE_SLIDING) {
		    this.end = new Date(this.dateMin.getTime()+this.window);
		} else if (this.mode == MODE_FRAME) {
		    this.frameIndex = 0;
		    this.begin = this.end = this.deltaFrame(0);
		} else {
		    this.end = new Date(this.dateMin.getTime()+this.window);
		    //			this.end =this.begin;
		}
		this.stopAnimation();
		this.applyAnimation();
            });
            this.btnEnd.button().click(() => {
		this.end = this.dateMax;
		if (this.mode == MODE_SLIDING) {
		    this.begin = new Date(this.dateMax.getTime()-this.window);
		} else if (this.mode == MODE_FRAME) {
		    this.frameIndex = this.dates.length+1;
		    this.begin = this.end = this.deltaFrame(0);
		} else {
		    this.end =this.dateMax;
		}
		this.stopAnimation();
		this.applyAnimation();
            });
            this.btnPrev.button().click(() => {
		if (this.mode == MODE_SLIDING) {
		    this.begin = new Date(this.begin.getTime()-this.window);
		    if(this.begin.getTime()<this.dateMin.getTime())
			this.begin = this.dateMin;
		    this.end = new Date(this.begin.getTime()+this.window);
		} else if (this.mode == MODE_FRAME) {
		    this.begin = this.end = this.deltaFrame(-1);
		} else {
		    this.end = new Date(this.end.getTime()-this.window);
		    if(this.end.getTime()<=this.begin.getTime()) {
			this.end = new Date(this.begin.getTime()+this.window);
		    }
		}
		this.stopAnimation();
		this.applyAnimation();
            });
            this.btnNext.button().click(() => {
		this.stopAnimation();
		this.doNext();
            });
            this.btnShowAll.button().click(() => {
		this.begin = this.dateMin;
		this.end = this.dateMax;
		this.inAnimation = false;
		this.running = false;
		if(this.btnRun)
		    this.btnRun.html(HtmlUtils.getIconImage("fa-play"));
		this.applyAnimation();
            });
        },
	atEnd: function() {
	    return this.end.getTime()>=this.dateMax.getTime();
	},
	doNext: function() {
	    let debug = true;
	    let wasAtEnd = this.atEnd();
	    if(debug) console.log("animation.doNext atEnd=" + wasAtEnd);
	    if (this.mode == MODE_SLIDING) {
		this.begin = this.end;
		this.end = new Date(this.end.getTime()+this.window);
		if(this.atEnd()) {
		    this.end = this.dateMax;
		    this.begin = new Date(this.end.getTime()-this.window);
		    this.inAnimation = false;
		    this.stopAnimation();
		}
	    } else if (this.mode == MODE_FRAME) {
		this.begin = this.end = this.deltaFrame(1);
		if(this.running) {
		    if(wasAtEnd) {
			if(this.display.getProperty("animationLoop")) {
			    setTimeout(()=>{
				this.begin = this.end = this.dateMin;
				this.frameIndex=0;
				this.updateUI();
			    },this.display.getProperty("animationDwell",1000));
			    return;
			} else {
			    this.stopAnimation();
			}
		    }
		}
	    } else {
		this.end = new Date(this.end.getTime()+this.window);
		if(this.atEnd()) {
		    this.end = this.dateMax;
		    this.inAnimation = false;
		    this.stopAnimation();
		}
	    }
	    this.applyAnimation();
	},

	deltaFrame: function(delta) {
	    this.frameIndex+=delta;
	    if(!this.dates) return;
	    if(this.frameIndex>= this.dates.length)
		this.frameIndex = this.dates.length-1;
	    else if(this.frameIndex<0)
		this.frameIndex = 0;
	    return this.dates[this.frameIndex];
	},
	startAnimation: function() {
            if (!this.dateMax) return;
	    if (!this.inAnimation) {
                this.inAnimation = true;
                this.label.html("");
		if (this.mode == MODE_FRAME) {
		    this.frameIndex =0;
		    this.begin = this.end = this.deltaFrame(0);
		    this.display.animationStart();
		    this.doNext();
		    return;
		}

                var date = this.dateMin;
                this.begin = date;
                var unit = this.windowUnit;
                if (unit != "") {
                    var tmp = 0;
                    if (unit == "decade") {
                        this.begin = new Date(date.getUTCFullYear(), 0);
                    } else if (unit == "year") {
                        this.begin = new Date(date.getUTCFullYear(), 0);
                    } else if (unit == "month") {
                        this.begin = new Date(date.getUTCFullYear(), date.getMonth());
                    } else if (unit == "day") {
                        this.begin = new Date(date.getUTCFullYear(), date.getMonth(), date.getDay());
                    } else if (unit == "hour") {
                        this.begin = new Date(date.getUTCFullYear(), date.getMonth(), date.getDay(), date.getHours());
                    } else if (unit == "minute") {
                        this.begin = new Date(date.getUTCFullYear(), date.getMonth(), date.getDay(), date.getHours(), date.getMinutes());
                    } else {
                        this.begin = new Date(date.getUTCFullYear(), date.getMonth(), date.getDay(), date.getHours(), date.getSeconds());
                    }
                } 
                this.end = this.begin;
		this.display.animationStart();
            }
	    this.doNext();
        },
	stopAnimation:function() {
	    if(this.btnRun)
		this.btnRun.html(HtmlUtils.getIconImage("fa-play"));
            this.running = false;
	},
	setDateRange: function(begin,end) {
	    this.begin = begin;
	    this.end = end;
	    this.stopAnimation();
	    this.updateUI();
	},
	applyAnimation: function(skipSlider) {
	    this.display.animationApply(this);
	    this.updateUI();
	},
	updateUI: function(skipSlider) {
	    if(!skipSlider) {
		this.jq(ID_SLIDER).slider('values',0,this.begin.getTime());
		this.jq(ID_SLIDER).slider('values',1,this.end.getTime());
	    }
	    this.updateLabels();
            var windowEnd = this.end.getTime();
            if (windowEnd <= this.dateMax.getTime()) {
                if (this.running) {
                    setTimeout(() => {
			if(!this.running) return;
			this.doNext();
		    }, this.speed);
                }
            } else {
                this.running = false;
                this.inAnimation = false;
		if(this.btnRun)
                    this.btnRun.html(HtmlUtils.getIconImage("fa-play"));
            }
	},

	updateLabels: function() {
	    if(this.label) {
		if (this.mode == MODE_FRAME && this.begin == this.end) {
		    this.label.html(this.formatAnimationDate(this.begin));
		} else {
		    this.label.html(this.formatAnimationDate(this.begin) + " - " + this.formatAnimationDate(this.end));
		}
	    }


	},
        formatAnimationDate: function(d) {
            if (this.dateFormat == "yyyy") {
                return Utils.formatDateYYYY(d);
            } else if (this.dateFormat == "yyyyMMdd") {
                return Utils.formatDateYYYYMMDD(d);
	    } else if (this.dateFormat == "monthdayyear") {
                return Utils.formatDateMonthDayYear(d);
	    } else if (this.dateFormat == "mdy") {
                return Utils.formatDateMDY(d);
	    } else if (this.dateFormat == "hhmm") {
                return Utils.formatDateHHMM(d);
            } else {
                return Utils.formatDate(d);
            }
        },

    });
}



function ColorByInfo(display, fields, records, prop,colorByMapProp, defaultColorTable) {
    var colorByAttr = display.getProperty(prop||"colorBy", null);
    var excludeZero = display.getProperty(PROP_EXCLUDE_ZERO, false);
    $.extend(this, {
	display:display,
        id: colorByAttr,
	fields:fields,
	overrideRange: display.getProperty("overrideColorRange",false),
	origRange:null,
	origMinValue:0,
	origMaxValue:0,
        minValue: 0,
        maxValue: 0,
        field: null,
        index: -1,
        isString: false,
        stringMap: null,
	colorByMap: {},
	colorByValues:[],
	colorByMinPerc: display.getDisplayProp(display, "colorByMinPercentile", -1),
	colorByMaxPerc: display.getDisplayProp(display, "colorByMaxPercentile", -1),
	colorByOffset: 0,
        pctFields:null,
	compareFields: display.getFieldsByIds(null, display.getProperty("colorByCompareFields", "", true)),
	displayColorTable: function(width,force) {
	    if(!this.display.getProperty("showColorTable",true)) return;
	    if(this.compareFields.length>0) {
		var legend = "";
		this.compareFields.map((f,idx)=>{
		    legend += HtmlUtils.div(["style","display:inline-block;width: 15px;height: 15px; background:" + this.colors[idx]+";"]) +" " +
			f.getLabel() +" ";
		});
		this.display.jq(ID_COLORTABLE).html(HtmlUtils.div(["style","text-align:center; margin-top:5px;"], legend));
	    }
	    if(!force && this.index<0) return;
	    if(this.stringMap) {
		var colors = [];
		this.colorByValues= [];
		for (var i in this.stringMap) {
		    this.colorByValues.push(i);
		    colors.push(this.stringMap[i]);
		}
		this.display.displayColorTable(colors, ID_COLORTABLE, this.origMinValue, this.origMaxValue, {
		    colorByInfo:this,
		    width:width,
		    stringValues: this.colorByValues});
	    } else {
		var colors = this.colors;
		if(this.display.getProperty("clipColorTable",true) && this.colorByValues.length) {
		    var tmp = [];
		    for(var i=0;i<this.colorByValues.length && i<colors.length;i++) 
			tmp.push(this.colors[i]);
		    colors = tmp;
		}
		this.display.displayColorTable(colors, ID_COLORTABLE, this.origMinValue, this.origMaxValue, {
		    colorByInfo:this,
		    width:width,
		    stringValues: this.colorByValues
		});
	    }
	},
	resetRange: function() {
	    if(this.origRange)
		this.setRange(this.origRange[0],this.origRange[1]);
	},
	setRange: function(minValue,maxValue, force) {
	    if(!force && this.overrideRange) return;
	    if (this.colorByLog) {
		if (minValue < 1) {
		    this.colorByOffset = 1 - minValue;
		}
		minValue = this.colorByFunc(minValue + this.colorByOffset);
		maxValue = this.colorByFunc(maxValue + this.colorByOffset);
	    }
	    this.minValue = minValue;
	    this.maxValue = maxValue;
	    this.origMinValue = minValue;
	    this.origMaxValue = maxValue;
	    this.range = maxValue - minValue;
	    if(!this.origRange) {
		this.origRange = [minValue, maxValue];
	    }
	},
	getValuePercent: function(v) {
            return  (v - this.minValue) / this.range;
	},
	getColor: function(value, pointRecord) {
	    var percent = 0;
            if (this.showPercent) {
                var total = 0;
                var data = pointRecord.getData();
                for (var j = 0; j < data.length; j++) {
                    var ok = this.fields[j].isNumeric() && !this.fields[j].isFieldGeo();
                            if (ok && this.pctFields != null) {
                                ok = this.pctFields.indexOf(this.fields[j].getId()) >= 0 ||
                                    this.pctFields.indexOf("#" + (j + 1)) >= 0;
                            }
                    if (ok) {
                        total += data[j];
                    }
                }
                if (total != 0) {
                    percent = percent = value / total * 100;
                    percent = (percent - this.minValue) / (this.maxValue - this.minValue);
                }
            } else {
                var v = value;
		if(this.stringMap) {
		    var color = this.stringMap[value];
		    if(!Utils.isDefined(color)) {
			return this.stringMap["default"];
		    }
		    return color;
		}
                if (this.isString) {
                    color = this.colorByMap[v];
		    if(color) return color;
                }
                v += this.colorByOffset;
                if (this.colorByLog) {
                    v = this.colorByFunc(v);
                }
                percent = (v - this.minValue) / this.range;
            }

	    var index=0;
	    if(this.steps) {
		for(;index<this.steps.length;index++) {
		    if(v<=this.steps[index]) {
			break;
		    }
		}
		this.xcnt++;
	    } else {
		index = parseInt(percent * this.colors.length);
	    }
            if (index >= this.colors.length) index = this.colors.length - 1;
            else if (index < 0) index = 0;
	    if(this.stringMap) {
		var color = this.stringMap[value];
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
	    var result =  Utils.pSBC(intensity,color);
	    //		    console.log(color +" " + result +" intensity:" + intensity +" min:" + this.intensityTargetM
	    return result || color;
	},
	convertColorAlpha: function(color, colorByValue) {
	    if(!this.convertAlpha) return color;
	    percent = (colorByValue-this.alphaSourceMin)/(this.alphaSourceMax-this.alphaSourceMin);
	    alpha=this.alphaTargetMin+percent*(this.alphaTargetMax-this.alphaTargetMin);
	    var result =  Utils.addAlphaToColor(color, alpha);
	    return result || color;
	}
    });

    this.convertAlpha = this.display.getProperty("convertColorAlpha",false);
    if(this.convertAlpha) {
	if(!Utils.isDefined(this.display.getProperty("alphaSourceMin"))) {
	    var min = 0, max=0;
	    records.map((record,idx)=>{
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
	    this.alphaSourceMin = +this.display.getProperty("alphaSourceMin",40);
	    this.alphaSourceMax = +this.display.getProperty("alphaSourceMax",80);
	}
	this.alphaTargetMin = +this.display.getProperty("alphaTargetMin",0); 
	this.alphaTargetMax = +this.display.getProperty("alphaTargetMax",1); 
    }



    this.convertIntensity = this.display.getProperty("convertColorIntensity",false);
    if(this.convertIntensity) {
	if(!Utils.isDefined(this.display.getProperty("intensitySourceMin"))) {
	    var min = 0, max=0;
	    records.map((record,idx)=>{
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
	    this.intensitySourceMin = +this.display.getProperty("intensitySourceMin",80);
	    this.intensitySourceMax = +this.display.getProperty("intensitySourceMax",40);
	}
	this.intensityTargetMin = +this.display.getProperty("intensityTargetMin",1); 
	this.intensityTargetMax = +this.display.getProperty("intensityTargetMax",0); 
    }

    if (this.display.percentFields != null) {
        this.pctFields = this.display.percentFields.split(",");
    }
    var colors = defaultColorTable || this.display.getColorTable(true,colorByAttr +".colorTable");
    if(!colors) {
	var c = this.display.getProperty(colorByAttr +".colors");
	if(c)
	    colors = c.split(",");
    }
    if(!colors)
	colors = this.display.getColorTable(true);
    this.colors = colors;

    if (!this.colors && this.display.colors && this.display.colors.length > 0) {
        this.colors = source.colors;
        if (this.colors.length == 1 && Utils.ColorTables[this.colors[0]]) {
            this.colors = Utils.ColorTables[this.colors[0]].colors;
        }
    }

    if (this.colors == null) {
        this.colors = Utils.ColorTables.grayscale.colors;
    }

    for (var i = 0; i < fields.length; i++) {
        var field = fields[i];
        if (field.getId() == this.id || ("#" + (i + 1)) == this.id) {
            this.field = field;
	    if (field.isString()) this.isString = true;
        }
    }
    this.index = this.field != null ? this.field.getIndex() : -1;
    this.stringMap = this.display.getColorByMap(colorByMapProp);
    if(this.index>=0) {
	var cnt = 0;
	records.map(record=>{
            var tuple = record.getData();
            var v = tuple[this.index];
            if (this.isString) {
		if (!Utils.isDefined(this.colorByMap[v])) {
		    var index = this.colorByValues.length;
                    this.colorByValues.push(v);
                    var color  = index>=this.colors.length?this.colors[this.colors.length-1]:this.colors[index];
		    this.colorByMap[v] = color;
                    this.setRange(1,  this.colorByValues.length, true);
		}
		return;
	    }
            if (excludeZero && v === 0) {
		return;
            }
	    if (!isNaN(v) && !(v === null)) {
		if (cnt == 0 || v > this.maxValue) this.maxValue = v;
		if (cnt == 0 || v < this.minValue) this.minValue = v;
		cnt++;
	    }
	});
    }

    if (this.display.showPercent) {
        this.setRange(0, 100,true);
    }
    var steps = this.display.getProperty("colorBySteps");
    if(steps) {
	this.steps = steps.split(",");
    }

    this.colorByLog = this.display.getProperty("colorByLog", false);
    this.colorByFunc = Math.log;
    this.setRange(this.display.getDisplayProp(this.display, "colorByMin", this.minValue),
		     this.display.getDisplayProp(this.display, "colorByMax", this.maxValue), true);

    this.range = this.maxValue - this.minValue;
}

/**
   Copyright 2008-2019 Geode Systems LLC
*/

function AreaWidget(display) {
    const ID_CONTAINS = "contains";
    const ID_NORTH = "north";
    const ID_SOUTH = "south";
    const ID_EAST = "east";
    const ID_WEST = "west";
    const ID_AREA_LINK = "arealink";

    RamaddaUtil.inherit(this, {
        display: display,
        getHtml: function() {
            var callback = this.display.getGet();
            //hack, hack
            var cbx = HtmlUtils.checkbox(this.display.getDomId(ID_CONTAINS), [TITLE, "Search mode: checked - contains, unchecked - overlaps"], false);
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
    const ID_DATE_START = "date_start";
    const ID_DATE_END = "date_end";

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
            var html = HtmlUtils.input(ID_DATE_START, "", [CLASS, "display-date-input", "placeholder", " start date", ATTR_ID,
							   display.getDomId(ID_DATE_START), "size", "10"
							  ]) + " - " +
                HtmlUtils.input(ID_DATE_END, "", [CLASS, "display-date-input", "placeholder", " end date", ATTR_ID,
						  display.getDomId(ID_DATE_END), "size", "10"
						 ]);
            return html;
        }
    });
}



function DisplayAnimation(display, enabled) {
    const ID_RUN = "animrun";
    const ID_NEXT = "animnext";
    const ID_PREV= "animprev";
    const ID_BEGIN= "animbegin";
    const ID_END= "animend";
    const ID_SLIDER = "slider";
    const ID_TICKS = "ticks";
    const ID_SHOWALL = "showall";
    const ID_ANIMATION_LABEL = "animationlabel";
    const MODE_FRAME = "frame";
    const MODE_SLIDING = "sliding";
    $.extend(this,{
	display:display,
	enabled: enabled,
        running: false,
        inAnimation: false,
        begin: null,
        end: null,
        dateMin: null,
        dateMax: null,
        dateRange: 0,
        dateFormat: display.getProperty("animationDateFormat", display.getProperty("dateFormat", "yyyymmdd")),
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
	    records.every(r=>{
		if(!seen[r.getDate()]) {
		    seen[r.getDate()] = true;
		    this.dates.push(r.getDate());
		}
		return true;
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
		if(debug)console.log("animation.init making ticks: #records=" + records.length +" date:" + this.dateMin + " " + this.dateMax);
		let tickStyle = this.display.getProperty("animationTickStyle","");
		var ticks = "";
		var min = this.dateMin.getTime();
		var max = this.dateMax.getTime();
		var p = 0;
		let seenDate={};
		for(var i=0;i<this.records.length;i++) {
		    var record = this.records[i];
		    var date = record.getDate().getTime();
		    if(seenDate[date]) continue;
		    seenDate[date] = true;
		    if(debug)console.log("\ttick:" + record.getDate());
		    var perc = (date-min)/(max-min)*100;
		    var tt = this.formatAnimationDate(record.getDate());
		    ticks+=HtmlUtils.div([ID,this.display.getId()+"-"+record.getId(), CLASS,"display-animation-tick",STYLE,HU.css('left', perc+'%')+tickStyle,TITLE,tt,"recordIndex",i],"");
		}
		this.jq(ID_TICKS).html(ticks);
		if(debug)console.log("animation.init done making ticks");
		this.display.makeTooltips(this.jq(ID_TICKS).find(".display-animation-tick"), this.records,(open,record) =>{
		    this.display.handleEventRecordHighlight(this, {highlight: open,record:record, skipAnimation:true});
		});
	    }
	    if(debug)console.log("animation.init-3");

	    this.updateLabels();
	    if(debug)console.log("animation.init-done");
	},
	getIndex: function() {
	    return this.frameIndex;
	},
	getBeginTime: function() {
	    return this.begin;
	},
	setSliderValues: function(v) {
	    let debug = false;
	    if(debug)
		console.log("animation.setSliderValues");

	    if(this.mode != MODE_FRAME) {
		this.begin = new Date(v[0]);
		this.end = new Date(v[1]);
	    } else {
		let sliderDate = new Date(v[0]);
		let closest = this.dates[0];
		let dist = 0;
		let closestIdx=0;
		this.dates.forEach((d,idx)=>{
		    if(Math.abs(d.getTime()-sliderDate.getTime()) < Math.abs(closest.getTime()-sliderDate.getTime())) {
			closest = d;
			closestIdx = idx;
		    }
		});
		this.begin = this.end = closest;
		this.frameIndex = closestIdx;
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
            let buttons =  "";
	    let showButtons  = this.display.getProperty("animationShowButtons",true);
	    let showSlider = display.getProperty("animationShowSlider",true);
	    if(showButtons) {
		var short = display.getProperty("animationWidgetShort",false);
		if(!short)
		    buttons +=   HtmlUtils.span([ID, this.getDomId(ID_BEGIN),TITLE,"Go to beginning"], HtmlUtils.getIconImage("fa-fast-backward")); 
		buttons += HtmlUtils.span([ID, this.getDomId(ID_PREV), TITLE,"Previous"], HtmlUtils.getIconImage("fa-step-backward")); 
		if(!short)
		    buttons +=HtmlUtils.span([ID, this.getDomId(ID_RUN),  TITLE,"Run/Stop"], HtmlUtils.getIconImage("fa-play")); 
		buttons +=HtmlUtils.span([ID, this.getDomId(ID_NEXT), TITLE,"Next"], HtmlUtils.getIconImage("fa-step-forward"));
		if(!short)
		    buttons +=HtmlUtils.span([ID, this.getDomId(ID_END), TITLE,"Go to end"], HtmlUtils.getIconImage("fa-fast-forward"));
		if(!short)
		    buttons += HtmlUtils.span([ID, this.getDomId(ID_SHOWALL), TITLE,"Show all"], HtmlUtils.getIconImage("fa-sync"));
	    }
	    if(showButtons) {
		buttons+=HtmlUtils.span([ID, this.getDomId(ID_ANIMATION_LABEL), CLASS, "display-animation-label"]);
	    } else {
		buttons+=HtmlUtils.div([ID, this.getDomId(ID_ANIMATION_LABEL), CLASS, "xxdisplay-animation-label",STYLE,HU.css("text-align","center","font-size","14pt")]);
	    }
            buttons = HtmlUtils.div([ CLASS,"display-animation-buttons"], buttons);
	    if(showSlider) {
		let style= display.getProperty("animationSliderStyle","");
		buttons +=   HtmlUtils.div([CLASS,"display-animation-slider",STYLE,style,ID,this.getDomId(ID_SLIDER)],
					   HtmlUtils.div([CLASS,"display-animation-ticks",ID,this.getDomId(ID_TICKS)]));
	    }

	    if(this.display.getProperty("animationShow",true)) {
		this.jq(ID_TOP_LEFT).append(HtmlUtils.div([STYLE,this.display.getProperty("animationStyle")], buttons));
	    }
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
	    let debug = false;
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
			if(this.display.getProperty("animationLoop",true)) {
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
        formatAnimationDate: function(date) {
	    return Utils.formatDateWithFormat(date,this.dateFormat,true);
        },

    });
}


function ColorByInfo(display, fields, records, prop,colorByMapProp, defaultColorTable, propPrefix, theField, props) {
    this.properties = props || {};
    if(!prop) prop = "colorBy";
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
	belowColor:display.getProperty("colorThresholdAbove","blue"),
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
        isString: false,
        stringMap: null,
	colorByMap: {},
	colorByValues:[],
	colorByMinPerc: this.getProperty("MinPercentile", -1),
	colorByMaxPerc: this.getProperty("MaxPercentile", -1),
	colorByOffset: 0,
        pctFields:null,
	compareFields: display.getFieldsByIds(null, this.getProperty("CompareFields", "")),
    });
    if(this.fieldValue == "year") {
	let seen= {};
	this.dates = [];
	records.forEach(r=>{
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
    if(this.convertAlpha) {
	if(!Utils.isDefined(this.getProperty("alphaSourceMin"))) {
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
	    this.intensitySourceMin = +this.getProperty("intensitySourceMin",80);
	    this.intensitySourceMax = +this.getProperty("intensitySourceMax",40);
	}
	this.intensityTargetMin = +this.getProperty("intensityTargetMin",1); 
	this.intensityTargetMax = +this.getProperty("intensityTargetMax",0); 
    }

    if (this.display.percentFields != null) {
        this.pctFields = this.display.percentFields.split(",");
    }

    let colors = defaultColorTable || this.display.getColorTable(true,[colorByAttr +".colorTable","colorTable"]);
    if(!colors && colorByAttr) {
	let c = this.display.getProperty(colorByAttr +".colors");
	if(c) colors = c.split(",");
    }
    
//    if(!colors && this.hasField()) {
//	colors = this.display.getColorTable(true,"colorTable");
//    }

    if(!colors) {
	var c = this.getProperty(colorByAttr +".colors");
	if(c)
	    colors = c.split(",");
    }


    if(!colors)
	colors = this.display.getColorTable(true);
    this.colors = colors;

    if(this.hasField() && !colors) {
//	this.index = -1;
//	return;
    }



    if (!this.colors && this.display.colors && this.display.colors.length > 0) {
        this.colors = source.colors;
        if (this.colors.length == 1 && Utils.ColorTables[this.colors[0]]) {
            this.colors = Utils.ColorTables[this.colors[0]].colors;
        }
    }

    if (this.colors == null) {
        this.colors = Utils.ColorTables.grayscale.colors;
    }


    if(!this.field) {
	for (var i = 0; i < fields.length; i++) {
            var field = fields[i];
            if (field.getId() == this.id || ("#" + (i + 1)) == this.id) {
		this.field = field;
            }
	}
    }

    if(this.field && this.field.isString()) this.isString = true;
    this.index = this.field != null ? this.field.getIndex() : -1;
    this.stringMap = this.display.getColorByMap(colorByMapProp);
    if(this.index>=0) {
	var cnt = 0;
	records.forEach(record=>{
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
            if (this.excludeZero && v === 0) {
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
    this.enabled = this.getProperty("doColorBy",true) && this.index>=0;
}


ColorByInfo.prototype = {
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
	return this.enabled;
    },
    displayColorTable: function(width,force, domId) {
	if(!this.getProperty("showColorTable",true)) return;
	if(this.compareFields.length>0) {
	    var legend = "";
	    this.compareFields.map((f,idx)=>{
		legend += HtmlUtils.div([STYLE,HU.css('display','inline-block','width','15px','height','15px','background', this.colors[idx])]) +" " +
		    f.getLabel() +" ";
	    });
	    let dom = this.display.jq(domId || ID_COLORTABLE);
	    dom.html(HtmlUtils.div([STYLE,HU.css('text-align','center','margin-top','5px')], legend));
	}
	if(!force && this.index<0) return;
	if(this.stringMap) {
	    var colors = [];
	    this.colorByValues= [];
	    for (var i in this.stringMap) {
		this.colorByValues.push(i);
		colors.push(this.stringMap[i]);
	    }
	    this.display.displayColorTable(colors, domId || ID_COLORTABLE, this.origMinValue, this.origMaxValue, {
		colorByInfo:this,
		width:width,
		stringValues: this.colorByValues});
	} else {
	    var colors = this.colors;
	    if(this.getProperty("clipColorTable",true) && this.colorByValues.length) {
		var tmp = [];
		for(var i=0;i<this.colorByValues.length && i<colors.length;i++) 
		    tmp.push(this.colors[i]);
		colors = tmp;
	    }
	    this.display.displayColorTable(colors, domId || ID_COLORTABLE, this.origMinValue, this.origMaxValue, {
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
//	console.log(this.propPrefix +" min:" + minValue + " max:" + maxValue);
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
	this.range = maxValue - minValue;
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

    getColorFromRecord: function(record, dflt) {
	if(this.display.getFilterHighlight() && !record.isHighlight(this.display)) {
	    return this.display.getProperty("unhighlightColor","#eee");
	}

	if(this.colorThresholdField && this.display.selectedRecord) {
	    let v=this.display.selectedRecord.getValue(this.colorThresholdField.getIndex());
	    let v2=record.getValue(this.colorThresholdField.getIndex());
	    if(v2>v) return this.aboveColor;
	    else return this.belowColor;
	}

	if (this.index >= 0) {
	    let value = record.getData()[this.index];
	    return  this.getColor(value, record);
	}
	if(this.fieldValue == "year") {
	    let value = record.getDate().getUTCFullYear();
	    return this.getColor(value, record);
	}
	return dflt;
    },
    hasField: function() {
	return this.index>=0;
    },
    getColor: function(value, pointRecord) {
	if(this.display.getFilterHighlight() && pointRecord && !pointRecord.isHighlight(this.display)) {
	    return this.display.getUnhighlightColor();
	}

	var percent = 0.5;
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
                percent =  value / total * 100;
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
	    let tmp = v;
            v += this.colorByOffset;
            if (this.colorByFunc && v>0) {
                v = this.colorByFunc(v);
            }
            percent = this.range?(v - this.minValue) / this.range:0.5;
//	    if(tmp>3 && tmp<6)
//		console.log("ov:" + tmp  +" v:" + v + " perc:" + percent);
        }



	var index=0;
	if(this.steps) {
	    for(;index<this.steps.length;index++) {
		if(v<=this.steps[index]) {
		    break;
		}
	    }
	} else {
	    index = parseInt(percent * this.colors.length);
	}
//	console.log("v:" + v +" index:" + index +" colors:" + this.colors);
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
}

function drawSparkLine(display, dom,w,h,data, records,min,max,colorBy,attrs, margin) {
    if(!attrs) attrs = {};
    if(!margin)
	margin = { top: 0, right: 0, bottom: 0, left: 0 };
    const INNER_WIDTH  = w - margin.left - margin.right;
    const INNER_HEIGHT = h - margin.top - margin.bottom;
    const BAR_WIDTH  = w / data.length;
    const x    = d3.scaleLinear().domain([0, data.length]).range([0, INNER_WIDTH]);
    const y    = d3.scaleLinear().domain([min, max]).range([INNER_HEIGHT, 0]);
    const recty    = d3.scaleLinear().domain([min, max]).range([0,INNER_HEIGHT]);

    var tt = d3.select("body").append("div")	
	.attr(CLASS, "sparkline-tooltip")				
	.style("opacity", 0);

    const svg = d3.select(dom).append('svg')
	  .attr('width', w)
	  .attr('height', h)
	  .append('g')
	  .attr('transform', 'translate(' + margin.left + ',' + margin.top + ')');
    const line = d3.line()
	  .x((d, i) => x(i))
	  .y(d => y(d));

    let lineColor = attrs.lineColor||display.getProperty("sparklineLineColor","#000");
    let barColor = attrs.barColor ||display.getProperty("sparklineBarColor","MediumSeaGreen");	    
    let circleColor = attrs.circleColor ||display.getProperty("sparklineCircleColor","#000");
    let circleRadius = attrs.circleRadius ||display.getProperty("sparklineCircleRadius",1);
    let lineWidth = attrs.lineWidth ||display.getProperty("sparklineLineWidth",1);
    let defaultShowEndPoints = true;
    let getColor = (d,i,dflt)=>{
	return colorBy?colorBy.getColorFromRecord(records[i], dflt):dflt;
    };
    let showBars = attrs.showBars|| display.getProperty("sparklineShowBars",false);

    if(!showBars && (attrs.showLines|| display.getProperty("sparklineShowLines",true))) {
	svg.selectAll('line').data(data).enter().append("line")
	    .attr('x1', (d,i)=>{return x(i)})
	    .attr('y1', (d,i)=>{return y(d)})
	    .attr('x2', (d,i)=>{return x(i+1)})
	    .attr('y2', (d,i)=>{return y(i<data.length-1?data[i+1]:data[i])})
	    .attr("stroke-width", lineWidth)
            .attr("stroke", (d,i)=>{
		if(isNaN(d)) return "rgba(0,0,0,0)";
		return getColor(d,i,lineColor)
	    })
	    .style("cursor", "pointer");
    }

    if(showBars) {
	defaultShowEndPoints = false;
	svg.selectAll('.bar').data(data)
	    .enter()
	    .append('rect')
	    .attr('class', 'bar')
	    .attr('x', (d, i) => x(i))
	    .attr('y', d => y(d))
	    .attr('width', BAR_WIDTH)
	    .attr('height', d => h-y(d))
	    .attr('fill', (d,i)=>getColor(d,i,barColor))
	    .style("cursor", "pointer")
    }

    if(attrs.showCircles || display.getProperty("sparklineShowCircles",false)) {
	svg.selectAll('circle').data(data).enter().append("circle")
	    .attr('r', (d,i)=>{return isNaN(d)?0:circleRadius})
	    .attr('cx', (d,i)=>{return x(i)})
	    .attr('cy', (d,i)=>{return y(d)})
	    .attr('fill', (d,i)=>getColor(d,i,circleColor))
	    .style("cursor", "pointer");
    }

    if(attrs.showEndpoints || display.getProperty("sparklineShowEndPoints",defaultShowEndPoints)) {
	let fidx=0;
	while(isNaN(data[fidx]) && fidx<data.length) fidx++;
	let lidx=data.length-1;
	while(isNaN(data[lidx]) && fidx>=0) lidx--;	
	svg.append('circle')
	    .attr('r', attrs.endPointRadius|| display.getProperty("sparklineEndPointRadius",2))
	    .attr('cx', x(fidx))
	    .attr('cy', y(data[fidx]))
	    .attr('fill', attrs.endPoint1Color || display.getProperty("sparklineEndPoint1Color") || getColor(data[0],0,display.getProperty("sparklineEndPoint1Color",'steelblue')));
	svg.append('circle')
	    .attr('r', attrs.endPointRadius|| display.getProperty("sparklineEndPointRadius",2))
	    .attr('cx', x(lidx))
	    .attr('cy', y(data[lidx]))
	    .attr('fill', attrs.endPoint2Color || display.getProperty("sparklineEndPoint2Color")|| getColor(data[data.length-1],data.length-1,display.getProperty("sparklineEndPoint2Color",'tomato')));
    }
    let _display = display;
    let doTooltip = display.getProperty("sparklineDoTooltip", true)  || attrs.doTooltip;
    svg.on("click", function() {
	var coords = d3.mouse(this);
	if(records) {
	    let record = records[Math.round(x.invert(coords[0]))]
	    if(record)
		_display.propagateEventRecordSelection({select:true,record: record});
	}
    });


    if(doTooltip) {
	svg.on("mouseover", function() {
	    if(!records) return;
	    var coords = d3.mouse(this);
	    let record = records[Math.round(x.invert(coords[0]))]
	    if(!record) return;
	    let html = _display.getRecordHtml(record);
	    let ele = $(dom);
	    let offset = ele.offset().top + ele.height();
	    let left = ele.offset().left;
	    tt.transition().duration(200).style("opacity", .9);		
	    tt.html(html)
		.style("left", left + "px")		
		.style("top", offset + "px");	
	})
	    .on("mouseout", function(d) {		
		tt.transition()		
		    .duration(500)		
		    .style("opacity", 0);
	    });
    }
}



function drawPieChart(display, dom,width,height,array,min,max,colorBy,attrs) {
    if(!attrs) attrs = {};
    let margin = Utils.isDefined(attrs.margin)?attrs.margin:4;
    let colors = attrs.pieColors||Utils.ColorTables.cats.colors;

    var radius = Math.min(width, height) / 2 - margin
    var svg = d3.select(dom)
	.append("svg")
	.attr("width", width)
	.attr("height", height)
	.append("g")
	.attr("transform", "translate(" + width / 2 + "," + height / 2 + ")");
    var data = {};
    array.forEach(tuple=>{
	data[tuple[0]] = tuple[1];
    })

    // set the color scale
    var color = d3.scaleOrdinal()
	.domain(data)
	.range(colors)

    // Compute the position of each group on the pie:
    var pie = d3.pie()
	.value(function(d) {return d.value; })
    var data_ready = pie(d3.entries(data))

    // Build the pie chart: Basically, each part of the pie is a path that we build using the arc function.
    svg
	.selectAll('whatever')
	.data(data_ready)
	.enter()
	.append('path')
	.attr('d', d3.arc()
	      .innerRadius(0)
	      .outerRadius(radius)
	     )
	.attr('fill', function(d){ return(color(d.data.key)) })
	.attr("stroke", "black")
	.style("stroke-width", "1px")
	.style("opacity", 0.7)
}





function SizeBy(display,records) {
    this.display = display;
    if(!records) records = display.filterData();
    let pointData = this.display.getPointData();
    let fields = pointData.getRecordFields();
    $.extend(this, {
        id: this.display.getProperty("sizeBy"),
        minValue: 0,
        maxValue: 0,
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
	let size = this.getSizeFromValue(value,func);
//	console.log("G: " + value +  " s:" + size);
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
	    if(debug) console.log("value:" + value +" v:" + v +" size:" + size);
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




function Annotations(display,records) {
    this.display = display;
    if(!records) records = display.filterData();
    let pointData = this.display.getPointData();
    let fields = pointData.getRecordFields();
    this.labelField = this.display.getFieldById(null,this.display.getProperty("annotationLabelField"));
    this.fields = this.display.getFieldsByIds(null,this.display.getProperty("annotationFields"));
    let prop = this.display.getProperty("annotations");
    if(prop) this.fields = [];
    this.indexToAnnotation = null;
    if(prop) {
	this.indexToAnnotation = {};
	this.recordToAnnotation = {};
	this.annotations=[];
	this.legend = "";
	let labelCnt = 0;
	let toks = prop.split(";");
	this.hasRange = false;
	for(let i=0;i<toks.length;i++) {
	    let toks2 = toks[i].split(",");
	    //index,label,description,url
	    if(toks2.length<2) continue;
	    let index = toks2[0].trim();
	    let label = toks2[1];
	    if(label.trim() == "") {
		labelCnt++;
		label  =""+labelCnt;
	    }
	    let desc = toks2.length<2?"":toks2[2];
	    let url = toks2.length<3?null:toks2[3];
	    let isDate = false;
	    let dateLabel ="";
	    let annotation = {label: label,description: desc,toString:function() {return this.description;}   };
	    this.annotations.push(annotation);
	    if(index.match(/^[0-9]+$/)) {
		index = parseFloat(index);
	    } else {
		let index2 = null;
		if(index.indexOf(":")>=0) {
		    index2 = index.split(":")[1];
		    index = index.split(":")[0];
		}
		
		if(index=="today") {
		    index = new Date();
		} else {
		    index = Utils.parseDate(index,false);
		}
		if(index2) {
		    this.hasRange = true;
 		    if(index2=="today") {
			index2 = Utils.formatDateYYYYMMDD(new Date());
		    } else {
			index2 = Utils.parseDate(index2,false);

		    }
		    annotation.index2 = index2.getTime();
		}
		isDate = true;
		dateLabel = Utils.formatDateYYYYMMDD(index)+": ";
	    }
	    annotation.index = isDate?index.getTime():index;
	    let legendLabel = desc;
	    if(url!=null) {
		legendLabel = HU.href(url, legendLabel,["target","_annotation"]);
	    }
	    this.legend+= HU.b(label)+":" + legendLabel+" ";
	}
	for(let aidx=0;aidx<this.annotations.length;aidx++) {
	    let annotation = this.annotations[aidx];
	    let minIndex = null;
	    let minRecord = null;
	    let minDistance = null;
	    for (let rowIdx = 0; rowIdx < records.length; rowIdx++) {
		let ele = records[rowIdx];
		let record = ele.record?ele.record:ele;
		let row = this.display.getDataValues(records[rowIdx]);
		let index = row[0];
		if(index.v) index=  index.v;
		if(record) index = record.getTime();
		let distance =  Number.MAX_VALUE;
		if(annotation.index2) {
		    //range
 		    if(index>=annotation.index && index<=annotation.index2) {
			distance = 0;
		    } else {
			distance = Math.min(Math.abs(annotation.index-index),Math.abs(annotation.index2-index));
		    }
		    if(distance==0) {
			this.indexToAnnotation[rowIdx] = annotation;
			this.recordToAnnotation[record] = annotation;
		    }
		} else {
		    distance = Math.abs(annotation.index-index);
		}
		if(minIndex == null) {
		    minIndex = rowIdx;
		    minDistance = distance;
		    minRecord = record;
		} else {
		    if(distance<minDistance) {
			minIndex = rowIdx;
			minDistance = distance;
			minRecord = record;
		    }
		}
	    }
	    if(minIndex!=null) {
		this.indexToAnnotation[minIndex] = annotation;
		this.recordToAnnotation[minRecord] = annotation;

	    }
	}
    }
}

Annotations.prototype = {
    isEnabled: function() {
	return this.annotations!=null;
    },
    getAnnotations: function() {
	return this.annotations;
    },
    getAnnotation: function(rowIdx) {
	if(this.recordToAnnotation[rowIdx])
	    return this.recordToAnnotation[rowIdx];
	return this.indexToAnnotation?this.indexToAnnotation[rowIdx]:null;
    },
    getAnnotationFromDate: function(date) {
	let distance =  Number.MAX_VALUE;
	let minAnnotation = null;
	let minDistance = null;
	let time = date.getTime();
	for(let aidx=0;aidx<this.annotations.length;aidx++) {
	    let annotation = this.annotations[aidx];
	    if(annotation.index2) {
 		if(time>=annotation.index && time<=annotation.index2) {
		    return annotation;
		}
	    } else {
		distance = Math.abs(annotation.index-time);
		if(minAnnotation == null) {
		    minAnnotation = annotation;
		    minDistance = distance;
		} else {
		    if(distance<minDistance) {
			minAnnotation = annotation;
			minDistance = distance;
		    }
		}
	    }
	}
	return minAnnotation;
    },
    getLegend: function() {
	return this.legend;
    },
    getShowLegend: function() {
	return 	this.display.getProperty("showAnnotationsLegend");
    },
    hasFields: function() {
	return this.fields && this.fields.length>0;
    },
    getFields: function() {
	return this.fields;
    }
    

}



var Gfx = {
    gridData: function(gridId,fields, records,args) {
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
	    scale:1,
	    cellSize:2,
	    cellSizeX:2,
	    cellSizeY:2,
	    operator:"average"
	}
	$.extend(opts,args);
	//	console.log(JSON.stringify(opts,null,2));
	let id = HtmlUtils.getUniqueId();
	opts.scale=+opts.scale;
	let scale = opts.scale;
//	scale=1;
	opts.w*=opts.scale;
	opts.h*=opts.scale;
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
	    records.forEach((record,idx)=>{
		let lat = record.getLatitude();
		let lon = record.getLongitude();
		let x = scaleX(lat,lon);
		let y = scaleY(lat,lon);
//		console.log("x:" + x +" " + y +" lat:" + lat +" " + lon);
		record[gridId+"_coordinates"] = {x:x,y:y};
		let colorValue = 0;
		if(opts.colorBy && opts.colorBy.index>=0) {
		    colorValue = record.getValue(opts.colorBy.index);
		}
		let lengthValue = 0;
		if(opts.lengthBy && opts.lengthBy.index>=0) {
		    lengthValue = record.getValue(opts.lengthBy.index);
		}		
		x =Math.floor(x/opts.cellSizeX);
		y =Math.floor(y/opts.cellSizeY);
		if(x<0) x=0;
		if(y<0) y=0;
		if(x>=cols) x=cols-1;
		if(y>=rows) y=rows-1;
		points.push({x:x,y:y,colorValue:colorValue,r:record});
//		console.log(x+" " + y +" " + colorValue);
	    });


	    let grid = Gfx.gridPoints(rows,cols,points,args);
	    opts.cellSizeX = +opts.cellSizeX;
	    opts.cellSizeY = +opts.cellSizeY;
	    this.applyFilter(opts,grid);
	    //get the new min/max from the filtered grid
	    let mm = this.getMinMaxGrid(grid,v=>v.v);
	    if(opts.colorBy) {
		if(!Utils.isDefined(opts.display.getProperty("colorByMin")))  {
		    opts.colorBy.setRange(mm.min,mm.max);
		}  
		opts.colorBy.index=0;
	    }

	    let countThreshold = opts.display.getProperty("hm.countThreshold",opts.operator=="count"?1:0);
	    let glyph = new Glyph(opts.display,
				  scale,
				  fields,
				  records,
				  {type:"rect",
				   canvasWidth:canvas.width,
				   canvasHeight: canvas.height,
				   colorByInfo:opts.colorBy,
				   width: opts.cellSizeX,
				   height: opts.cellSizeY,
				   stroke:false,
				   pos:"c",
				   dx:opts.cellSizeX/2,
				   dy:opts.cellSizeY/2,				   
				  },
				  "");
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
			glyph.draw(opts, canvas, ctx, x,y,{
			    colorValue:cell.v,
			    col:colIdx,row:rowIdx,cell:cell, grid:grid});
		}
	    }
	} else {
	    records.sort((a,b)=>{return b.getLatitude()-a.getLatitude()});
	    let glyphs=[];
	    let cnt = 1;
	    while(cnt<11) {
		let attr = opts.display.getProperty("glyph" + (cnt++));
		if(!attr)
		    continue;
		glyphs.push(new Glyph(opts.display,scale, fields,records,{
		    canvasWidth:canvas.width,
		    canvasHeight: canvas.height
		},attr));
	    }


	    glyphs.forEach(glyph=>{
		records.map((record,idx)=>{
		    let lat = record.getLatitude();
		    let lon = record.getLongitude();
		    let x = scaleX(lat,lon);
		    let y = scaleY(lat,lon);
		    record[gridId+"_coordinates"] = {x:x,y:y};
		    let colorValue = opts.colorBy? record.getData()[opts.colorBy.index]:null;
		    let lengthValue = opts.lengthBy? record.getData()[opts.lengthBy.index]:null;
		    glyph.draw(opts, canvas, ctx, x,y,{colorValue:colorValue, lengthValue:lengthValue,record:record});
		});
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

	points.forEach((p,idx)=>{
	    let cell = values[p.y][p.x];
	    cell.min = cell.count==0?p.colorValue:Math.min(cell.min,p.colorValue);
	    cell.max = cell.count==0?p.colorValue:Math.max(cell.max,p.colorValue);
	    cell.count++;
	    cell.total += p.colorValue;
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
}


function Glyph(display, scale, fields, records, args, attrs) {
    args = args||{};
    $.extend(this,{
	display: display,
	type:"label",
	dx:0,
	dy:0,
	label:"",
	baseHeight:0,
	baseWidth:0,
	width:8,
	fill:true,
	stroke:true,
    });
    $.extend(this,args);
    attrs.split(",").forEach(attr=>{
	let toks = attr.split(":");
	let name = toks[0];
	let value="";
	for(let i=1;i<toks.length;i++) {
	    if(i>1) value+=":";
	    value+=toks[i];
	}
	value = value.replace(/_nl_/g,"\n").replace(/_colon_/g,":").replace(/_comma_/g,",");
	if(value=="true") value=true;
	else if(value=="false") value=false;
	this[name] = value;
//	console.log("attr:" + name+"=" + value);
    });
    if(this.type=="image") {
	this.imageField=display.getFieldById(fields,this.imageField);
	this.myImage= new Image();
    }
    this.scale = scale;
    if(this.height==null) {
	if(this.type == "3dbar")
	    this.height=20;
	else
	    this.height=8;
    }
    if(this.pos==null) {
	if(this.type == "3dbar")
	    this.color = "blue";
	else if(this.type == "rect") {
	    this.pos = "c";
	}
	else
	    this.pos = "nw";
    }	
    
    this.width = (+this.width);
    this.height = (+this.height);
    if(this.dx=="canvasWidth") this.dx=this.canvasWidth;
    else if(this.dx=="-canvasWidth") this.dx=-this.canvasWidth;
    else if(this.dx=="canvasWidth2") this.dx=this.canvasWidth/2;
    else if(this.dx=="-canvasWidth2") this.dx=-this.canvasWidth/2;    
    else if(this.dx=="width") this.dx=this.width;
    else if(this.dx=="-width") this.dx=-this.width;
    else if(this.dx=="width2") this.dx=this.width/2;
    else if(this.dx=="-width2") this.dx=-this.width/2;
    if(this.dy=="canvasHeight") this.dy=this.canvasHeight;
    else if(this.dy=="-canvasHeight") this.dy=-this.canvasHeight;
    else if(this.dy=="canvasHeight2") this.dy=this.canvasHeight/2;
    else if(this.dy=="-canvasHeight2") this.dy=-this.canvasHeight/2;    
    else if(this.dy=="height") this.dy=this.height;
    else if(this.dy=="-height") this.dy=-this.height;
    else if(this.dy=="height2") this.dy=this.height/2;
    else if(this.dy=="-height2") this.dy=-this.height/2;



    this.baseWidth = +this.baseWidth;
    this.width = (+this.width)*scale;
    this.height = (+this.height)*scale;
    this.dx = (+this.dx)*scale;
    this.dy = (+this.dy)*scale;
    if(this.sizeBy) {
	this.sizeByField=display.getFieldById(fields,this.sizeBy);
	if(this.sizeByField) {
	    let props = {
		Min:this.sizeByMin,
		Max:this.sizeByMax,
	    };
	    this.sizeByInfo =  new ColorByInfo(display, fields, records, this.sizeBy,this.sizeBy, null, this.sizeBy,this.sizeByField,props);
	}
    }
    if(!this.colorByInfo && this.colorBy) {
	this.colorByField=display.getFieldById(fields,this.colorBy);
	let ct = this.colorTable?display.getColorTableInner(true, this.colorTable):null;
	if(this.colorByField) {
	    let props = {
		Min:this.colorByMin,
		Max:this.colorByMax,
	    };	    
	    this.colorByInfo =  new ColorByInfo(display, fields, records, this.colorBy,this.colorBy+".colorByMap", ct, this.colorBy,this.colorByField, props);
	}
    }


}



Glyph.prototype = {
    draw: function(opts, canvas, ctx, x,y,args) {
	let color =   null;
	if(this.colorByInfo) {
	    if(this.colorByField) {
		let v = args.record.getValue(this.colorByField.getIndex());
		color=  this.colorByInfo.getColor(v);
	    } else if(args.colorValue) {
		color=  this.colorByInfo.getColor(args.colorValue);
	    }

	}
	let lengthPercent = 1.0;
	if(this.sizeByInfo) {
	    let v = args.record.getValue(this.sizeByField.getIndex());
	    lengthPercent = this.sizeByInfo.getValuePercent(v);
	}

	if(args.alphaByCount && args.cell && args.grid) {
	    if(args.grid.maxCount!=args.grid.minCount) {
		let countPerc = (args.cell.count-args.grid.minCount)/(args.grid.maxCount-args.grid.minCount);
		color = Utils.addAlphaToColor(c,countPerc);
	    }
	}
	ctx.fillStyle =color || this.fillStyle || this.color || "blue";
	ctx.strokeStyle =this.strokeStyle || this.color || "#000";
	ctx.lineWidth=this.lineWidth||1;

	if(this.type=="label" || this.label) {
	    if(!this.label) {
		console.log("No label specified");
		return;
	    }
	    ctx.font = this.font || "12pt arial"
	    ctx.fillStyle = ctx.strokeStyle =    this.color|| "#000";
	    let text = String(this.label);
	    if(args.record) {
		args.record.fields.forEach(f=>{
		    text = text.replace("\${" + f.getId()+"}",args.record.getValue(f.getIndex()));
		});
	    }
	    text = text.split("\n");
	    let h = 0;
	    let hgap = 3;
	    let maxw = 0;
	    text.forEach((t,idx)=>{
		let dim = ctx.measureText(t);
		if(idx>0) h+=hgap;
		maxw=Math.max(maxw,dim.width);
		h +=dim.actualBoundingBoxAscent+dim.actualBoundingBoxDescent;
	    });
	    let pt = Utils.translatePoint(x, y, maxw,  h, this.pos,{dx:this.dx,dy:this.dy});
	    text.forEach(t=>{
		let dim = ctx.measureText(t);
//		console.log(pt.x +" " + pt.y +" " + t);
		ctx.fillText(t,pt.x,pt.y);
		pt.y += dim.actualBoundingBoxAscent + dim.actualBoundingBoxDescent + hgap;
	    });
	} else 	if(this.type == "circle") {
	    /*
	    ctx.beginPath();
	    ctx.moveTo(0,y);
	    ctx.lineTo(100,y);
	    ctx.stroke();
	    ctx.moveTo(x,0);
	    ctx.lineTo(x,100);
	    ctx.stroke();
	    */
	    ctx.beginPath();
	    let w = this.width*lengthPercent+ this.baseWidth;
//	    this.dx=0; 
//	    this.dy=-50;
//	    this.pos="n"; 
	    let pt = Utils.translatePoint(x, y, w,  w, this.pos,{dx:this.dx,dy:this.dy});
	    let cx = pt.x+w/2;
	    let cy = pt.y+w/2;
	    ctx.arc(cx,cy, w/2, 0, 2 * Math.PI);
//	    console.log(pt.x +" " + pt.y +" " + cx +" " + cy  +" " + this.width);
	    if(this.fill)  {
		ctx.fill();
	    }
	    if(this.stroke) 
		ctx.stroke();
	} else if(this.type=="rect") {
	    let pt = Utils.translatePoint(x, y, this.width,  this.height, this.pos,{dx:this.dx,dy:this.dy});
	    if(this.fill)  
		ctx.fillRect(pt.x,pt.y, this.width, this.height);
	    if(this.stroke) 
		ctx.strokeRect(pt.x,pt.y, this.width, this.height);
	} else if(this.type=="image") {
	    if(this.imageField) {
		let img = args.record.getValue(this.imageField.getIndex());
		let pt = Utils.translatePoint(x, y, this.width,  this.height, this.pos,{dx:this.dx,dy:this.dy});
		let i = new Image();
		i.src = img;
		setTimeout(()=>{
		    ctx.drawImage(i,pt.x,pt.y,40,40);
		},1000);
//		ctx.drawImage(this.myImage,pt.x,pt.y);
//		ctx.drawImage(this.myImage,0,0);
	    }
	} else 	if(this.type == "gauge") {
	    let pt = Utils.translatePoint(x, y, this.width,  this.height, this.pos,{dx:this.dx,dy:this.dy});
	    ctx.fillStyle =  this.fillColor || "#F7F7F7";
	    ctx.beginPath();
	    let cx= pt.x+this.width/2;
	    let cy = pt.y+this.height;
	    ctx.arc(cx,cy, this.width/2,  1 * Math.PI,0);
	    ctx.fill();
	    ctx.strokeStyle =   "#000";
	    ctx.stroke();
	    ctx.beginPath();
//	    ctx.moveTo(cx-this.width/2,cy);
//	    ctx.lineTo(cx+this.width/2,cy);
//	    ctx.stroke();

	    ctx.beginPath();
	    let length = this.width/2*0.75;
            let degrees = (180*lengthPercent);
	    let ex = cx-this.width*0.4;
	    let ey = cy;
	    let ep = this.rotate(cx,cy,ex,ey,degrees);
	    ctx.strokeStyle =  this.color || "#000";
	    ctx.lineWidth=this.lineWidth||2;
	    ctx.moveTo(cx,cy);
	    ctx.lineTo(ep.x,ep.y);
	    ctx.stroke();
	    ctx.lineWidth=1;
	    this.showLabel = true;
	    if(this.showLabel && this.sizeByInfo) {
		ctx.fillStyle="#000";
		let label = String(this.sizeByInfo.minValue);
		ctx.font = this.font || "9pt arial"
		let dim = ctx.measureText(label);
		ctx.fillText(label,cx-this.width/2-dim.width-2,cy);
		ctx.fillText(this.sizeByInfo.maxValue,cx+this.width/2+2,cy);
	    }
	} else if(this.type=="3dbar") {
	    let pt = Utils.translatePoint(x, y, this.width,  this.height, this.pos,{dx:this.dx,dy:this.dy});
	    let height = lengthPercent*(this.height) + parseFloat(this.baseHeight);
	    ctx.fillStyle =   color || this.color;
	    ctx.strokeStyle = this.strokeStyle||"#000";
	    this.draw3DRect(canvas,ctx,pt.x, 
			    canvas.height-pt.y-this.height,
			    +this.width,height,+this.width);
	    
	} else if(this.type=="axis") {
	    let pt = Utils.translatePoint(x, y, this.width,  this.height, this.pos,{dx:this.dx,dy:this.dy});
	    let height = lengthPercent*(this.height) + parseFloat(this.baseHeight);
	    ctx.strokeStyle = this.strokeStyle||"#000";
	    ctx.beginPath();
	    ctx.moveTo(pt.x,pt.y);
	    ctx.lineTo(pt.x,pt.y+this.height);
	    ctx.lineTo(pt.x+this.width,pt.y+this.height);
	    ctx.stroke();
//	    this.draw3DRect(canvas,ctx,x+this.dx, canvas.height-y-this.dy,+this.width,height,+this.width);	    
	} else if(this.type == "vector") {
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
	} else if(this.type=="tile"){
	    let crx = x+opts.cellSizeX/2;
	    let cry = y+opts.cellSizeY/2;
 	    if((args.row%2)==0)  {
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
	    console.log("Unknwon cell shape:" + this.type);
	}
    },
    rotate:function(cx, cy, x, y, angle,anticlock_wise = false) {
	if(angle == 0){
            return {x:parseFloat(x), y:parseFloat(y)};
	}
	if(anticlock_wise){
            var radians = (Math.PI / 180) * angle;
	}else{
            var radians = (Math.PI / -180) * angle;
	}
	var cos = Math.cos(radians);
	var sin = Math.sin(radians);
	var nx = (cos * (x - cx)) + (sin * (y - cy)) + cx;
	var ny = (cos * (y - cy)) - (sin * (x - cx)) + cy;
	return {x:nx, y:ny};
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

}

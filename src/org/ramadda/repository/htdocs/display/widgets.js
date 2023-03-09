/**
   Copyright 2008-2023 Geode Systems LLC
*/

function AreaWidget(display) {
    const ID_CONTAINS = "mapcontains";
    const ID_NORTH = "north";
    const ID_SOUTH = "south";
    const ID_EAST = "east";
    const ID_WEST = "west";
    const ID_SETTINGS = "mapsettings";
    const ID_AREA_LINK = "arealink";
    const ID_MAP_SHOW = "showmap";
    const ID_MAP_POPUP_WRAPPER = "mappopupwrapper";    
    const ID_MAP_POPUP = "mappopup";    
    const ID_CLEAR = "mapclear";    
    const ID_SET_LOCATION="mapsetlocation";



    $.extend(this, {
	areaContains: HU.getUrlArgument("map_contains")=="true",
        display: display,
        initHtml: function() {
	    this.display.jq(ID_SETTINGS).click(()=>{
		this.showSettings();
	    });
	    this.display.jq(ID_MAP_SHOW).click(()=>{
		this.showMap();
	    });

	    let params = {};
	    this.map =  new RepositoryMap(this.display.domId(ID_MAP_POPUP), params);
	    this.map.setSelection(this.display.getId(),true,1);
	},
        showSettings: function() {
	    let _this = this;
	    let html = "";
	    html+= HU.div([CLASS,"ramadda-clickable",TITLE, "Use my location",ID,this.display.domId(ID_SET_LOCATION)],
			  HU.getIconImage("fas fa-compass") + SPACE + "Use my location");
            html += HU.div([CLASS,"ramadda-clickable",TITLE, "Clear form",ID,this.display.domId(ID_CLEAR)],
			  HU.getIconImage("fas fa-eraser") + SPACE + "Clear form");
	    html+= HU.div([TITLE, "Search mode: checked - contains, unchecked - overlaps"],
			  HtmlUtils.checkbox("",[ID, this.display.getDomId(ID_CONTAINS)], this.areaContains) +HU.tag("label",[CLASS,"ramadda-clickable","for",this.display.getDomId(ID_CONTAINS)], SPACE + "Contains"));
	    html = HU.div([STYLE,"margin:5px;"], html);
	    this.settingsDialog = HU.makeDialog({content:html,anchor:this.display.jq(ID_SETTINGS),draggable:false,header:true});
	    this.display.jq(ID_CONTAINS).change(function(e) {
		_this.areaContains = $(this).is(':checked');
	    });
	    this.display.jq(ID_SET_LOCATION).click(()=>{
		this.settingsDialog.remove();
		this.useMyLocation();
	    });
	    this.display.jq(ID_CLEAR).click(()=>{
		this.settingsDialog.remove();
		this.areaClear();
	    });	    
	},
        getHtml: function() {
	    let bounds =  HU.getUrlArgument("map_bounds");
	    let n="",w="",s="",e="";
	    if(bounds) {
		[n,w,s,e]  = bounds.split(",");
	    }
            let callback = this.display.getGet();
            let settings = HU.div([TITLE,"Settings",CLASS,"ramadda-clickable",ID,this.display.domId(ID_SETTINGS)],HU.getIconImage("fas fa-cog"));
	    let showMap = HU.div([CLASS,"ramadda-clickable",ID,this.display.domId(ID_MAP_SHOW),TITLE,"Show map selector"], HtmlUtils.getIconImage("fas fa-globe"));

	    let input = (id,place,title,v)=>{
		return HtmlUtils.input(id, v, ["placeholder", place, ATTR_CLASS, "input display-area-input", "size", "5", ATTR_ID,
						this.display.getDomId(id), ATTR_TITLE, title]);
	    };
            let areaForm = HtmlUtils.openTag(TAG_TABLE, [ATTR_CLASS, "display-area"]);
            areaForm += HtmlUtils.tr([],
				     HtmlUtils.td(["align", "center"],
						  HtmlUtils.leftCenterRight("",
									    input(ID_NORTH, " N","North",n),showMap, "20%", "60%", "20%")));

            areaForm += HtmlUtils.tr([], HtmlUtils.td([],
						      input(ID_WEST, " W", "West",w) +
						      input(ID_EAST, " E", "East",e)));

            areaForm += HtmlUtils.tr([],
				     HtmlUtils.td(["align", "center"],
						  HtmlUtils.leftCenterRight("", input(ID_SOUTH,  " S", "South",s), settings, "20%", "60%", "20%")));


            areaForm += HtmlUtils.closeTag(TAG_TABLE);
            areaForm += HU.div([ID,this.display.domId(ID_MAP_POPUP_WRAPPER),STYLE,HU.css("display","none")],SPACE+"Shift-drag: select region. Cmd-drag: move region" +
				HU.div([ID,this.display.domId(ID_MAP_POPUP),STYLE,HU.css("width","400px","height","300px")]));
            return areaForm;
        },
	showMap: function() {
	    let anchor = this.display.jq(ID_MAP_SHOW);
	    this.dialog = HU.makeDialog({contentId:this.display.domId(ID_MAP_POPUP_WRAPPER),anchor:anchor,draggable:true,header:true});
	    this.map.selectionPopupInit();
	    this.map.getMap().updateSize();
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
                let _this = this;
                navigator.geolocation.getCurrentPosition(function(position) {
                    _this.setUseMyLocation(position);
                });
            } else {}
        },
        setUseMyLocation: function(position) {
            let lat = position.coords.latitude;
            let lon = position.coords.longitude;
            let offset = 5.0;
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
            let image = root + (this.linkArea ? "/icons/link.png" : "/icons/link_break.png");
            $("#" + this.display.getDomId(ID_AREA_LINK)).attr("src", image);
            if (this.linkArea && this.lastBounds) {
                let b = this.lastBounds;
                $("#" + this.display.getDomId(ID_NORTH)).val(MapUtils.formatLocationValue(b.top));
                $("#" + this.display.getDomId(ID_WEST)).val(MapUtils.formatLocationValue(b.left));
                $("#" + this.display.getDomId(ID_SOUTH)).val(MapUtils.formatLocationValue(b.bottom));
                $("#" + this.display.getDomId(ID_EAST)).val(MapUtils.formatLocationValue(b.right));
            }
        },
        linkArea: false,
        lastBounds: null,
        handleEventMapBoundsChanged: function(source, args) {
            bounds = args.bounds;
            this.lastBounds = bounds;
            if (!args.force && !this.linkArea) return;
            $("#" + this.display.getDomId(ID_NORTH)).val(MapUtils.formatLocationValue(bounds.top));
            $("#" + this.display.getDomId(ID_WEST)).val(MapUtils.formatLocationValue(bounds.left));
            $("#" + this.display.getDomId(ID_SOUTH)).val(MapUtils.formatLocationValue(bounds.bottom));
            $("#" + this.display.getDomId(ID_EAST)).val(MapUtils.formatLocationValue(bounds.right));
        },
        setSearchSettings: function(settings) {
	    let n = this.display.getFieldValue(this.display.getDomId(ID_NORTH), null);
	    let w = this.display.getFieldValue(this.display.getDomId(ID_WEST), null);	    
	    let s = this.display.getFieldValue(this.display.getDomId(ID_SOUTH), null);
	    let e = this.display.getFieldValue(this.display.getDomId(ID_EAST), null);
            settings.setAreaContains(this.areaContains);
	    HU.addToDocumentUrl("map_contains",this.areaContains);
            settings.setBounds(n,w,s,e);
	    HU.addToDocumentUrl("map_bounds",[n||"",w||"",s||"",e||""].join(","));
        },
    });
}



function DateRangeWidget(display, what) {
    const ID_DATE_START = "date_start";
    const ID_DATE_END = "date_end";
    let startLabel, endLabel;
    this.what = what||"date";
    if(what == "createdate") {
	startLabel = "Create start";
	endLabel = "Create end";	
    } else {
	startLabel = "Start date";
	endLabel = "End date";	
    }

    this.baseId = this.what;
    RamaddaUtil.inherit(this, {
        display: display,
        initHtml: function() {
            $("#" + this.baseId +ID_DATE_START).datepicker({
		dateFormat: "yy-mm-dd"
	    });
            $("#" + this.baseId +ID_DATE_END).datepicker({
		dateFormat: "yy-mm-dd"
	    });	    
        },
        setSearchSettings: function(settings) {
            let start = $("#"+ this.baseId +ID_DATE_START).val();
            let end =  $("#"+ this.baseId +ID_DATE_END).val();
	    HU.addToDocumentUrl(this.baseId+ID_DATE_START,Utils.stringDefined(start)?start:null);
	    HU.addToDocumentUrl(this.baseId+ID_DATE_END,Utils.stringDefined(end)?end:null);		    	    
	    if(this.what=="createdate")
		settings.setCreateDateRange(start, end);
	    else
		settings.setDateRange(start, end);
        },
        getHtml: function() {
	    let start = HU.getUrlArgument(this.baseId+ID_DATE_START);
	    let end = HU.getUrlArgument(this.baseId+ID_DATE_END);	    
            let html = HtmlUtils.input(this.baseId +ID_DATE_START, start||"", [CLASS, "display-date-input", "placeholder", " " +startLabel, TITLE, startLabel, ATTR_ID,
									this.baseId +ID_DATE_START, 
							  ]) + " - " +
                HtmlUtils.input(this.baseId +ID_DATE_END, end||"", [CLASS, "display-date-input", "placeholder",  " " +endLabel, TITLE,endLabel,ATTR_ID,
							       this.baseId +ID_DATE_END, 
						 ]);
            return html;
        }
    });
}



function DisplayAnimation(display, enabled,attrs) {
    let dflt = {
    };
    attrs = attrs||{};
    $.extend(dflt,attrs);
    const ID_RUN = "animrun";
    const ID_ANIM_NEXT = "animnext";
    const ID_ANIM_PREV= "animprev";
    const ID_BEGIN= "animbegin";
    const ID_END= "animend";
    const ID_SLIDER = "slider";
    const ID_TICKS = "ticks";
    const ID_TOOLTIP = "tooltip";    
    const ID_SHOWALL = "showall";
    const ID_WINDOW = "window";
    const ID_STEP = "step";        
    const ID_SETTINGS = "settings";
    const ID_FASTER = "faster";
    const ID_SLOWER = "slower";
    const ID_RESET = "reset";    
    const ID_ANIMATION_LABEL = "animationlabel";
    const MODE_FRAME = "frame";
    const MODE_SLIDING = "sliding";
    $.extend(this,{
	display:display,
	enabled: enabled,
	targetDiv:attrs.targetDiv,
	baseDomId:attrs.baseDomId,
	labelSize:display.getProperty("animationLabelSize","12pt"),
	labelStyle:display.getProperty("animationLabelStyle",""),
        running: false,
        inAnimation: false,
        begin: null,
        end: null,
        dateMin: null,
        dateMax: null,
        dateRange: 0,
        dateFormat: display.getProperty("animationDateFormat", display.getProperty("dateFormat", "yyyymmdd")),
        mode: display.getProperty("animationMode", "cumulative"),
        startAtBeginning: display.getProperty("animationStartAtBeginning", true),	
        startAtEnd: display.getProperty("animationStartAtEnd", false),
        speed: parseInt(display.getProperty("animationSpeed", 500)),
        dwell: parseInt(display.getProperty("animationDwell", 1000)),	
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
	    return this.domId(id);
	},
        domId: function(id) {
	    return this.display.getDomId(id+(this.baseDomId?this.baseDomId:""));
	},
	jq: function(id) {
	    return this.display.jq(id+(this.baseDomId?this.baseDomId:""));
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
	    let seen = {};
	    this.dateToRecordMap = {};
	    records.every(r=>{
		let date = r.getDate();
		if(!r) return;
		if(!seen[date]) {
		    seen[date] = true;
		    this.dates.push(date);
		    this.dateToRecordMap[date] = r;
		}
		return true;
	    });
	    this.dates.sort(function(a,b) {
		return a.getTime() - b.getTime();
	    });
	    
            this.dateRange = this.dateMax.getTime() - this.dateMin.getTime();
	    this.steps= parseFloat(this.display.getProperty("animationSteps", 60));
	    this.setWindow();
	    this.frameIndex = 0;
	    if(!this.display.getProperty("animationStartShowAll",false)) { 
		this.resetRange();
	    }
	    let sliderValues = this.mode != MODE_FRAME?[this.begin.getTime(),this.end.getTime()]:[this.begin.getTime()];
	    let tooltipFunc = {
		    mouseleave: function(e) {
			if(_this.tooltip)
			    _this.tooltip.hide();
		    },
		    mousemove: function(e) {
			if(!_this.tooltip) return;
			if(e.offsetX>=0) {
			    let parentWidth = _this.tooltip.parent().width();
			    let parentLeft = _this.tooltip.parent().offset().left; 
			    let percent = (e.pageX-parentLeft)/parentWidth;
			    let dttm = new Date(_this.dateMin.getTime() + percent*_this.dateRange);
			    dttm = _this.formatAnimationDate(dttm,_this.tooltipDateFormat);
			    if(!_this.makeSlider) {
				dttm+="<br>+/-:zoom";
			    }
			    _this.tooltip.html(dttm);
			    _this.tooltip.show();
			    _this.tooltip.position({
				of: e.target,
				my: "left top",
				at: "left+" + e.offsetX +" bottom",
				collision: "fit fit"
			    });
			}
		    }};

	    if(this.makeSlider) {
		let slider = this.jq(ID_SLIDER).slider({
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
			_this.dateRangeChanged(true);
		    }
		});
		this.jq(ID_SLIDER).on(tooltipFunc);
	    } else {
		this.jq(ID_TICKS).on(tooltipFunc);
	    }

	    this.updateTicks();
	    if(debug)console.log("animation.init-3");
	    this.updateLabels();
	    if(debug)console.log("animation.init-done");
	},
	resetRange: function() {
	    if(this.startAtEnd) {
		this.begin = this.dateMax;
		this.end = this.dateMax;
		if (this.mode == MODE_FRAME) {
		    this.frameIndex = this.dates.length-1;
		}		    
	    } else   if(this.startAtBeginning) {
		this.begin = this.dateMin;
		this.end = new Date(this.begin.getTime()+this.window);
	    }
	    if (this.mode == MODE_FRAME) {
		this.end = this.begin;
	    }
	},
	setWindow: function() {
	    let window = this.display.getProperty("animationWindow");
	    let step = this.display.getProperty("animationStep", window);
	    if (window) {
		this.window = this.getMillis(window);
	    } else if(this.steps>0){
		this.window = this.dateRange / this.steps;
	    }
	    if (step) {
		this.step = this.getMillis(step);
	    } else {
		this.step = this.window;
	    }
	},
	timeMap: {
	    century: 1000 * 60 * 60 * 24 * 365 * 100,
	    centuries: 1000 * 60 * 60 * 24 * 365 * 100,	    
	    decade: 1000 * 60 * 60 * 24 * 365 * 10,
	    halfdecade: 1000 * 60 * 60 * 24 * 365 * 5,
	    year: 1000 * 60 * 60 * 24 * 365 * 1,
	    years: 1000 * 60 * 60 * 24 * 365 * 1,	    
	    month: 1000 * 60 * 60 * 24 * 31,
	    months: 1000 * 60 * 60 * 24 * 31,	    
	    week: 1000 * 60 * 60 * 24 * 7,
	    weeks: 1000 * 60 * 60 * 24 * 7,	    
	    day: 1000 * 60 * 60 * 24 * 1,
	    days: 1000 * 60 * 60 * 24 * 1,		    	    
	    hour: 1000 * 60 * 60,
	    hours: 1000 * 60 * 60,
	    minute: 1000 * 60,
	    minutes: 1000 * 60,	    
	    second: 1000,
	    seconds: 1000		    
	},
	getMillis:function(window) {
	    window =(""+window).trim();
	    let cnt = 1;
	    let unit = "day";
	    let toks = window.match("^([0-9]+)(.*)");
	    if(toks) {
		cnt = +toks[1];
		unit  = toks[2].trim();
	    } else {
		toks = window.match("(^[0-9]+)$");
		if(toks) {
		    unit = "minute";
		    cnt = +toks[1];
		} else {
		    unit = window;
		}
	    }
	    let scale = 1;
	    unit = unit.toLowerCase().trim();
	    if(this.timeMap[unit]) {
		scale = this.timeMap[unit];
	    } else {
		if(unit.endsWith("s"))
		    unit = unit.substring(0, unit.length-1);
		if(this.timeMap[unit]) {
		    scale = this.timeMap[unit];
		} else {
		    console.log("Unknown unit:" + unit);
		}
	    }
	    return  cnt*scale;
	},
	getIndex: function() {
	    return this.frameIndex;
	},
	getBeginTime: function() {
	    return this.begin;
	},
	handleEventAnimationChanged(args) {
	    this.begin = args.begin;
	    this.end = args.end;
	    this.stopAnimation();
	    this.applyAnimation();
	},
	setSliderValues: function(v) {
	    let debug = false;
	    if(debug)
		console.log(this.display.type+" animation.setSliderValues");
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
	    let element = $("#" + this.display.getId()+"-"+args.record.getId());
	    if(this.ticks)
		this.ticks.removeClass("display-animation-tick-highlight");
	    if(args.highlight) {
		element.addClass("display-animation-tick-highlight");
	    } else {
		element.removeClass("display-animation-tick-highlight");
	    }
	},
	makeControls:function() {
	    this.tickHeight = this.display.getProperty("animationHeight","15px");
	    this.makeSlider = this.display.getProperty("animationMakeSlider",true);
            let buttons =  "";
	    let showButtons  = this.display.getProperty("animationShowButtons",true);
	    let showSlider = display.getProperty("animationShowSlider",true);
	    let showLabel = display.getProperty("animationShowLabel",true);	    
	    if(showButtons) {
		let short = display.getProperty("animationWidgetShort",false);
		buttons +=   HtmlUtils.span([ID, this.getDomId(ID_SETTINGS),TITLE,"Settings"], HtmlUtils.getIconImage("fas fa-cog")); 
		if(!short)
		    buttons +=   HtmlUtils.span([ID, this.getDomId(ID_BEGIN),TITLE,"Go to beginning"], HtmlUtils.getIconImage("fa-fast-backward")); 
		buttons += HtmlUtils.span([ID, this.getDomId(ID_ANIM_PREV), TITLE,"Previous"], HtmlUtils.getIconImage("fa-step-backward")); 
		if(!short)
		    buttons +=HtmlUtils.span([ID, this.getDomId(ID_RUN),  TITLE,"Run/Stop"], HtmlUtils.getIconImage("fa-play")); 
		buttons +=HtmlUtils.span([ID, this.getDomId(ID_ANIM_NEXT), TITLE,"Next"], HtmlUtils.getIconImage("fa-step-forward"));
		if(!short)
		    buttons +=HtmlUtils.span([ID, this.getDomId(ID_END), TITLE,"Go to end"], HtmlUtils.getIconImage("fa-fast-forward"));
	    }

	    if(showLabel) {
		if(showButtons) {
		    buttons+=HtmlUtils.span([ID, this.getDomId(ID_ANIMATION_LABEL), CLASS, "display-animation-label",STYLE,this.labelStyle+HU.css("font-size",this.labelSize)]);
		} else {
		    buttons+=HtmlUtils.div([ID, this.getDomId(ID_ANIMATION_LABEL), CLASS, "display-animation-label",STYLE,this.labelStyle+HU.css("text-align","center","font-size",this.labelSize)]);
		}
	    }
            buttons = HtmlUtils.div([ CLASS,"display-animation-buttons"], buttons);
	    if(showSlider) {
		let style= HU.css("height",this.tickHeight) +display.getProperty("animationSliderStyle","");
		let tooltip  = HU.div([ID,this.getDomId(ID_TOOLTIP),CLASS,"display-animation-tooltip"],"");
		let tickContainerStyle = HU.css("height",this.tickHeight);
		if(!this.makeSlider) {
		    tickContainerStyle += HU.css("background","efefef","border","1px solid #aaa");
		}
		if(!this.makeSlider) {
		    style+=HU.css("cursor","move");
		}
		buttons +=   HtmlUtils.div([CLASS,"display-animation-slider",STYLE,style,ID,this.getDomId(ID_SLIDER)],
					   tooltip + HtmlUtils.div([STYLE, tickContainerStyle,CLASS,"display-animation-ticks","tabindex","0",ID,this.getDomId(ID_TICKS)]));
	    }
	    this.html = HtmlUtils.div([STYLE,this.display.getProperty("animationStyle")], buttons);
	    if(this.display.getProperty("animationShow",true)) {
		if(this.targetDiv) this.targetDiv.append(this.html);
		else this.jq(ID_TOP_LEFT).append(this.html);
	    }
	    if(!this.makeSlider) {
		let _this = this;
		this.jq(ID_TICKS).mouseenter(function(event) {
		    $(this).focus();
		});
		this.lastKeyTime = 0;
		let ticks = this.jq(ID_TICKS);
		ticks.mousedown(function(e) {
		    _this.mouseIsDown = true;
		    let parentOffset = $(this).parent().offset(); 
		    _this.mouseX = e.pageX - parentOffset.left;
		});

		ticks.mousemove(function(e) {
		    if(!_this.mouseIsDown) return;
		    var parentOffset = $(this).parent().offset(); 
		    var relX = e.pageX - parentOffset.left;
		    let range = _this.dateMax.getTime() - _this.dateMin.getTime();
		    let width = $(this).width();
		    let dx = (_this.mouseX-relX);
		    var parentOffset = $(this).parent().offset(); 
		    _this.mouseX = e.pageX - parentOffset.left;
		    if(dx==0) return;
		    let dt = range*dx/width 
		    if(!_this.originaDateMin) {
			_this.originaDateMin = _this.dateMin;
			_this.originaDateMax = _this.dateMax;		
		    }
		    _this.dateMin = new Date(_this.dateMin.getTime()+dt);
		    _this.dateMax = new Date(_this.dateMax.getTime()+dt);			
		    let t1 = new Date();
		    _this.updateTicks();
		    let t2 = new Date();
//		    Utils.displayTimes("update ticks",[t1,t2],true);
		    _this.updateLabels();

		});
		ticks.mouseup(function(e) {
		    _this.mouseIsDown = false;
		});
		ticks.keypress(function(event) {
		    let now = new Date();
		    let diff = now.getTime()-_this.lastKeyTime;
		    _this.lastKeyTime = now.getTime();
		    if(event.which==43)
			_this.zoom(true);
		    else if(event.which==45)
			_this.zoom(false);		    
		    else if(event.which==61)
			_this.zoomReset();

		});

		this.jq(ID_TICKS).bind('xwheel', function(e){		    
		    $(this).focus();
		    if(e.originalEvent.deltaY<0) {
			let range = _this.dateMax.getTime() - _this.dateMin.getTime();
			let newRange = range*0.9;
			let diff = range-newRange;
			_this.dateMin = new Date(_this.dateMin.getTime()+diff);
			_this.dateMax = new Date(_this.dateMax.getTime()-diff);			
			_this.updateTicks();
			_this.updateLabels();
		    } else if(e.originalEvent.deltaY>0) {
			//zoom out 
		    } else {
		    }
		    e.stopPropagation();
		    e.stopImmediatePropagation();
		    e.preventDefault();
		});

	    }

	    if(this.display.getProperty("animationTooltipShow",false)) {
		this.tooltip = this.jq(ID_TOOLTIP);
		this.tooltipDateFormat = this.display.getProperty("animationTooltipDateFormat");
	    }


	    let _this  =this;
            this.jq(ID_SETTINGS).button().click(function(){
		let window = _this.display.getProperty("animationWindow");
		let step = _this.display.getProperty("animationStep", window);		
		let clazz = "ramadda-hoverable ramadda-clickable";
		let html = HU.div([ID,_this.domId(ID_FASTER),TITLE, "Faster", CLASS,clazz], "Faster") +	
	    HU.div([ID,_this.domId(ID_SLOWER),TITLE, "Slower", CLASS,clazz], "Slower")		+
		    HU.div([ID,_this.domId(ID_RESET),TITLE, "Reset", CLASS,clazz], "Reset") +
		    HU.div([ID,_this.domId(ID_SHOWALL),TITLE, "Show all", CLASS,clazz], "Show all");
		if(window) {
		    html+=HU.div([TITLE, "Window, e.g., 1 week, 2 months, 3 days, 2 weeks, etc"], "Window:<br>" +SPACE2 + HU.input("",window,[ID,_this.domId(ID_WINDOW),"size","10"]));
		    html+=HU.div([TITLE, "Step, e.g., 1 week, 2 months, 3 days, 2 weeks, etc"], "Step:<br>" +SPACE2+ HU.input("",step,[ID,_this.domId(ID_STEP),"size","10"]));
		}
		html=HU.div([STYLE,HU.css("margin","4px")], html);
		_this.dialog = HU.makeDialog({content:html,anchor:$(this),draggable:false,header:false});

		let key = (e)=>{
		    if(Utils.isReturnKey(e)) {
			_this.dialog.hide();
			_this.display.setProperty("animationWindow",_this.jq(ID_WINDOW).val());
			_this.display.setProperty("animationStep",_this.jq(ID_STEP).val());			
			_this.setWindow();
			_this.resetRange();
			_this.dateRangeChanged();
		    }
		};
		_this.jq(ID_WINDOW).keyup(key);
		_this.jq(ID_STEP).keyup(key);
		_this.jq(ID_FASTER).click(()=>{
		    _this.dialog.hide();
		    _this.speed = _this.speed*0.75;
		});
		_this.jq(ID_SLOWER).click(()=>{
		    _this.dialog.hide();
		    _this.speed = _this.speed*1.5;
		});

		_this.jq(ID_RESET).click(()=>{
		    _this.dialog.hide();
		    _this.speed =  parseInt(_this.display.getProperty("animationSpeed", 500));
		    _this.resetRange();
		    _this.inAnimation = false;
		    _this.stopAnimation();
		    _this.dateRangeChanged();
		});		
		_this.jq(ID_SHOWALL).click(()=>{
		    _this.dialog.hide();
		    _this.begin = _this.dateMin;
		    _this.end = _this.dateMax;
		    _this.inAnimation = false;
		    _this.stopAnimation();
		    _this.dateRangeChanged();
		});		

	    });
            this.btnRun = this.jq(ID_RUN);
            this.btnPrev = this.jq(ID_ANIM_PREV);
            this.btnNext = this.jq(ID_ANIM_NEXT);
            this.btnBegin = this.jq(ID_BEGIN);
            this.btnEnd = this.jq(ID_END);
            this.label = this.jq(ID_ANIMATION_LABEL);
            this.btnRun.button().click(() => {
                this.toggleAnimation();
            });
            this.btnBegin.button().click(() => {
		let diff = this.getDiff();
		let fullRange = this.fullRange();
		this.begin = this.dateMin;
		if (this.mode == MODE_SLIDING) {
		    this.end = new Date(this.begin.getTime()+(fullRange?this.window:diff));
		} else if (this.mode == MODE_FRAME) {
		    this.frameIndex = 0;
		    this.begin = this.end = this.deltaFrame(0);
		} else {
		    this.end = new Date(this.dateMin.getTime()+this.window);
		}
		this.stopAnimation();
		this.dateRangeChanged();
            });
            this.btnEnd.button().click(() => {
		let diff = this.getDiff();
		let fullRange = this.fullRange();
		this.end = this.dateMax;
		if (this.mode == MODE_SLIDING) {
		    this.begin = new Date(this.end.getTime()-(fullRange?this.window:diff));
		} else if (this.mode == MODE_FRAME) {
		    this.frameIndex = this.dates.length+1;
		    this.begin = this.end = this.deltaFrame(0);
		} else {
		    this.end =this.dateMax;
		}
		this.stopAnimation();
		this.dateRangeChanged();
            });
            this.btnPrev.button().click(() => {
		this.stopAnimation();
		this.doPrev();
            });
            this.btnNext.button().click(() => {
		this.stopAnimation();
		this.doNext();
            });

        },
	fullRange: function() {
	    return this.atBegin() && this.atEnd();
	},
	atEnd: function() {
	    return this.end.getTime()>=this.dateMax.getTime();
	},
	atBegin: function() {
	    return this.begin.getTime()<=this.dateMin.getTime();
	},	
	getDiff: function() {
	    return  this.end.getTime()-this.begin.getTime();
	},
	doPrev: function()  {
	    let diff = this.getDiff()||this.window;
	    diff = this.window||this.getDiff();
	    if (this.mode == MODE_SLIDING) {
		this.begin = new Date(this.begin.getTime()-diff);
		if(this.begin.getTime()<this.dateMin.getTime())
		    this.begin = this.dateMin;
		this.end = new Date(this.begin.getTime()+diff);
	    } else if (this.mode == MODE_FRAME) {
		this.begin = this.end = this.deltaFrame(-1);
	    } else {
		this.end = new Date(this.end.getTime()-this.window);
		if(this.end.getTime()<=this.begin.getTime()) {
		    this.end = new Date(this.begin.getTime()+this.window);
		}
	    }
	    this.dateRangeChanged();
	},
	doNext: function() {
	    let debug = false;
	    let wasAtEnd = this.atEnd();
	    if(debug) console.log("animation.doNext:" + this.mode +" atEnd=" + wasAtEnd);
	    if (this.mode == MODE_SLIDING) {
		let window = this.window||this.getDiff();
		this.begin = new Date(this.begin.getTime()+this.step);
		this.end = new Date(this.end.getTime()+this.step);
		//this.end.getTime()+this.window);		
		if(this.atEnd()) {
		    this.end = this.dateMax;
		    this.begin = new Date(this.end.getTime()-window);
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
			    },this.dwell);
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
	    this.dateRangeChanged();
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
                if(this.fullRange()) {
		    this.end = new Date(this.begin.getTime()+this.window);
		}
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
	dateRangeChanged: function(skipSlider) {
	    this.applyAnimation(skipSlider);
	    this.display.getDisplayManager().notifyEvent(DisplayEvent.animationChanged, this.display, {
		begin:this.begin,
		end: this.end
	    });
	    let record = this.dateToRecordMap[this.begin];
	    if(record && this.display.getProperty("animationPropagateRecordSelection",false)) {
		this.display.getDisplayManager().notifyEvent(DisplayEvent.recordSelection, this, {record:record});
	    }
	},
	applyAnimation: function(skipSlider) {
	    this.display.animationApply(this);
	    this.updateUI();
	},
	setRecordListHighlight: function(recordList) {
	    this.recordListHighlight = recordList;
	    this.updateTicks();
	},
	zoomReset: function() {
	    if(this.originaDateMin) {
		this.dateMax = this.originaDateMax;		
		this.dateMin = this.originaDateMin;
		this.updateTicks();
		this.updateLabels();
	    }
	},
	zoom: function(zoomin) {
	    let range = this.dateMax.getTime() - this.dateMin.getTime();
	    let newRange = range*(zoomin?0.9:1.1);
	    let diff = range-newRange;
	    if(!this.originaDateMin) {
		this.originaDateMin = this.dateMin;
		this.originaDateMax = this.dateMax;		
	    }
	    this.dateMin = new Date(this.dateMin.getTime()+diff);
	    this.dateMax = new Date(this.dateMax.getTime()-diff);			
	    this.updateTicks();
	    this.updateLabels();
	},
	updateTicks: function() {
	    let debug = false;
	    this.tickCount = 0;
	    if(!this.records || !this.display.getProperty("animationShowTicks",true)) return;
	    this.highlightRecords = {};
	    if(this.recordListHighlight) {
		this.recordListHighlight.forEach(r=>{
		    this.highlightRecords[r.getId()] = true;
		});
	    }
	    if(debug)console.log("animation.init making ticks: #records=" + records.length +" date:" + this.dateMin + " " + this.dateMax);
	    let tickStyle = this.display.getProperty("animationTickStyle","");
	    let ticks = "";
	    let min = this.dateMin.getTime();
	    let max = this.dateMax.getTime();
	    let p = 0;
	    let seenDate={};
	    let t1 = new Date();
	    for(let i=0;i<this.records.length;i++) {
		let record = this.records[i];
		let date = record.getDate().getTime();
		if(seenDate[date]) continue;
		seenDate[date] = true;
		if(debug)console.log("\ttick:" + record.getDate());
		if(date<min) continue;
		if(date>max) continue;
		this.tickCount++;
		let perc = (date-min)/(max-min)*100;
		let tt = this.formatAnimationDate(record.getDate());
		let clazz = "display-animation-tick";
		if(this.highlightRecords[record.getId()]) {
		    clazz+=" display-animation-tick-highlight-base ";
		}
		ticks+=HtmlUtils.div([ID,this.display.getId()+"-"+record.getId(), CLASS,clazz,STYLE,HU.css("height",this.tickHeight,'left', perc+'%')+tickStyle,TITLE,tt,RECORD_ID,record.getId()],"");
	    }
	    let t2 = new Date();
	    this.jq(ID_TICKS).html(ticks);
	    let t3 = new Date();
	    if(debug)console.log("animation.init done making ticks");
	    let propagateHighlight = display.getProperty("animationHighlightRecord",false);
	    let propagateSelect = display.getProperty("animationSelectRecord",true);
	    this.ticks = this.jq(ID_TICKS).find(".display-animation-tick");
	    let _this = this;
	    this.display.makeTooltips(this.ticks, this.records,(open,record) =>{
		if(_this.display.animationLastRecordSelectTime) {
		    let now = new Date();
		    //If we recently selected a recordwith a click then don't do the highlight record from the mouse overs
		    //for a couple more seconds
		    if(now.getTime()-_this.display.animationLastRecordSelectTime.getTime()<1500) {
			return false;
		    }
		}
		if(record && propagateHighlight) {
		    if(propagateSelect) {
			_this.display.propagateEventRecordSelection({select:false,record: null});
		    }
		    this.display.handleEventRecordHighlight(this, {highlight: open,record:record, skipAnimation:true});
		}
		return true;
	    },null,propagateHighlight);
	    if(propagateSelect) {
		this.display.makeRecordSelect(this.ticks,this.display.makeIdToRecords(this.records),record=>{
		    _this.display.animationLastRecordSelectTime = new Date();
		});
	    }

	    let t4 = new Date();
//	    Utils.displayTimes("",[t1,t2,t3,t4],true);
	},
	updateUI: function(skipSlider) {
	    if(!skipSlider) {
		if(this.makeSlider) {
		    this.jq(ID_SLIDER).slider('values',0,this.begin.getTime());
		    this.jq(ID_SLIDER).slider('values',1,this.end.getTime());
		}
	    }
	    this.updateLabels();
            let windowEnd = this.end.getTime();
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
	makeLabel: function(label) {
	    return HU.span([STYLE,HU.css("font-size",this.labelSize)+this.labelStyle],label);
	},

	updateLabels: function() {
	    if(this.label) {
		if(!this.makeSlider) {
		    this.label.html(HU.leftCenterRight(this.makeLabel(this.formatAnimationDate(this.dateMin)),this.makeLabel("# " +this.tickCount), this.makeLabel(this.formatAnimationDate(this.dateMax))));
		} else {
		    if (this.mode == MODE_FRAME && this.begin == this.end) {
			this.label.html(this.makeLabel(this.formatAnimationDate(this.begin)));
		    } else {
			this.label.html(this.makeLabel(this.formatAnimationDate(this.begin) + " - " + this.formatAnimationDate(this.end)));
		    }
		}
	    }
	},
        formatAnimationDate: function(date,format,debug) {
	    let timeZoneOffset =this.display.getTimeZoneOffset();
	    let timeZone =this.display.getTimeZone();	    
	    if(timeZoneOffset) {
		if(debug) console.log("date before:" + date.toUTCString());
		date = Utils.createDate(date, -timeZoneOffset);
		if(debug) console.log("date after:" + date.toUTCString());
	    }
	    let fmt =  Utils.formatDateWithFormat(date,format||this.dateFormat,true);
	    if(timeZone) return fmt +" " + timeZone;
	    return fmt;
        },

    });
}



function ColorByInfo(display, fields, records, prop,colorByMapProp, defaultColorTable, propPrefix, theField, props,lastColorBy) {
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


    let colors = defaultColorTable || this.display.getColorTable(true,[this.properties.colorTableProperty,
								       colorByAttr +".colorTable",
								       "colorTable"]);

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
	if(!this.getProperty("showColorTable",true)) return;
	if(this.compareFields.length>0) {
	    var legend = "";
	    this.compareFields.forEach((f,idx)=>{
		legend += HtmlUtils.div([STYLE,HU.css('display','inline-block','width','15px','height','15px','background', this.colors[idx])]) +" " +
		    f.getLabel() +" ";
	    });
	    let dom = this.display.jq(domId || ID_COLORTABLE);
	    dom.html(HtmlUtils.div([STYLE,HU.css('text-align','center','margin-top','5px')], legend));
	}
	if(!force && this.index<0) return;
	if(this.stringMap) {
	    let colors = [];
	    this.colorByValues= [];
	    for (var i in this.stringMap) {
		let color = this.stringMap[i];
		this.colorByValues.push({value:i,color:color});
		colors.push(color);
	    }
	    this.display.displayColorTable(colors, domId || ID_COLORTABLE, this.origMinValue, this.origMaxValue, {
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
	    this.display.displayColorTable(colors, domId || ID_COLORTABLE, this.origMinValue, this.origMaxValue, {
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
	if(this.display.getProperty("useDataForColorRange") && this.origRange) {
	    minValue = this.origRange[0];
	    maxValue = this.origRange[1];	    
//	    this.range = this.maxValue -this.minValue;
//	    console.log(this.minValue,this.maxValue);
//	    return;
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
    setDoCount:function(min,max) {
	this.doCount = true;
        this.minValue = min;
	this.maxValue = max;
	this.range = this.maxValue-this.minValue;
	this.origMinValue=min;
	this.origMaxValue=max;
    },
    getColorFromRecord: function(record, dflt, checkHistory,debug) {
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
	    if(records.length>1) {
		let total = 0;
		records.forEach(r=>{
		    total+= r.getData()[this.index];
		});
		value = total/records.length;
	    } else {
		value= records[0].getData()[this.index];
	    }
	    value = this.getDoCount()?records.length:value;
	    return  this.getColor(value, record,checkHistory);
	} else if(this.timeField) {
	    let value;
	    if(this.timeField=="hour") {
		value = records[0].getTime().getHours();
	    }  else {
		value = records[0].getTime().getTime();
	    }
//	    console.log(value);
	    return  this.getColor(value, records[0],checkHistory);
	} 
	if(this.fieldValue == "year") {
	    if(records[0].getDate()) {
		let value = records[0].getDate().getUTCFullYear();
		return this.getColor(value, records[0]);
	    }
	}
	return dflt;
    },
    hasField: function() {
	return this.index>=0;
    },
    getColor: function(value, pointRecord, checkHistory) {
//	if(checkHistory) {
//	    if(this.colorHistory[value]) return this.colorHistory[value];
//	}
	let c = this.getColorInner(value, pointRecord);
	if(c==null) c=this.nullColor;
//	if(checkHistory) {
//	    this.colorHistory[value] = c;
//	}
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
    convertColorAlpha: function(color, colorByValue) {
	if(!this.convertAlpha) return color;
	percent = (colorByValue-this.alphaSourceMin)/(this.alphaSourceMax-this.alphaSourceMin);
	alpha=this.alphaTargetMin+percent*(this.alphaTargetMax-this.alphaTargetMin);
	let result =  Utils.addAlphaToColor(color, alpha);
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

    let tt = d3.select("body").append("div")	
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

    let lineColor = attrs.lineColor||display.getSparklineLineColor();
    let barColor = attrs.barColor ||display.getSparklineBarColor();
    let circleColor = attrs.circleColor ||display.getSparklineCircleColor();
    let circleRadius = attrs.circleRadius ||display.getSparklineCircleRadius();
    let lineWidth = attrs.lineWidth ||display.getSparklineLineWidth();
    let defaultShowEndPoints = true;
    let getColor = (d,i,dflt)=>{
	return colorBy?colorBy.getColorFromRecord(records[i], dflt):dflt;
    };
    let showBars = attrs.showBars|| display.getSparklineShowBars();
    svg.append('line')
	.attr('x1',0)
	.attr('y1', 0)
	.attr('x2', 0)
	.attr('y2', h)    
	.attr("stroke-width", 1)
    	.attr("stroke", '#ccc');

    svg.append('line')
	.attr('x1',0)
	.attr('y1', h)
	.attr('x2', w)
	.attr('y2', h)    
	.attr("stroke-width", 1)
    	.attr("stroke", '#ccc');
    


    let getNum = n=>{
	if(isNaN(n)) return 0;
	return n;
    };

    if(showBars) {
	defaultShowEndPoints = false;
	svg.selectAll('.bar').data(data)
	    .enter()
	    .append('rect')
	    .attr('class', 'bar')
	    .attr('x', (d, i) => getNum(x(i)))
	    .attr('y', d => getNum(y(d)))
	    .attr('width', BAR_WIDTH)
	    .attr('height', d => getNum(h-y(d)))
	    .attr('fill', (d,i)=>getColor(d,i,barColor))
	    .style("cursor", "pointer")
    }


    if(attrs.showLines|| display.getSparklineShowLines()) {
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


    if(attrs.showCircles || display.getSparklineShowCircles()) {
	svg.selectAll('circle').data(data).enter().append("circle")
	    .attr('r', (d,i)=>{return isNaN(d)?0:circleRadius})
	    .attr('cx', (d,i)=>{return getNum(x(i))})
	    .attr('cy', (d,i)=>{return getNum(y(d))})
	    .attr('fill', (d,i)=>getColor(d,i,circleColor))
	    .style("cursor", "pointer");
    }



    if(attrs.showEndpoints || display.getSparklineShowEndPoints(defaultShowEndPoints)) {
	let fidx=0;
	while(isNaN(data[fidx]) && fidx<data.length) fidx++;
	let lidx=data.length-1;
	while(isNaN(data[lidx]) && lidx>=0) lidx--;	
	svg.append('circle')
	    .attr('r', attrs.endPointRadius|| display.getSparklineEndPointRadius())
	    .attr('cx', x(fidx))
	    .attr('cy', y(data[fidx]))
	    .attr('fill', attrs.endPoint1Color || display.getSparklineEndPoint1Color() || getColor(data[0],0,display.getSparklineEndPoint1Color()));
	svg.append('circle')
	    .attr('r', attrs.endPointRadius|| display.getSparklineEndPointRadius())
	    .attr('cx', x(lidx))
	    .attr('cy', y(data[lidx]))
	    .attr('fill', attrs.endPoint2Color || display.getSparklineEndPoint2Color()|| getColor(data[data.length-1],data.length-1,display.getSparklineEndPoint2Color()));
    }
    let _display = display;
    let doTooltip = display.getSparklineDoTooltip()  || attrs.doTooltip;
    svg.on("click", function() {
	let coords = d3.mouse(this);
	if(records) {
	    let record = records[Math.round(x.invert(coords[0]))]
	    if(record)
		_display.propagateEventRecordSelection({select:true,record: record});
	}
    });



    if(doTooltip) {
	svg.on("mouseover", function() {
	    if(!records) return;
	    let coords = d3.mouse(this);
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



function drawDots(display, dom,w,h,data, range, colorBy,attrs, margin) {
    attrs = attrs ||  {};
    margin = margin || { top: 0, right: 0, bottom: 0, left: 0 };
    const INNER_WIDTH  = w - margin.left - margin.right;
    const INNER_HEIGHT = h - margin.top - margin.bottom;
    const x    = d3.scaleLinear().domain([range.minx, range.maxx]).range([0, INNER_WIDTH]);
    const y    = d3.scaleLinear().domain([range.miny, range.maxy]).range([INNER_HEIGHT, 0]);
    let tt = d3.select("body").append("div").attr(CLASS, "sparkline-tooltip").style("opacity", 0);
    const svg = d3.select(dom).append('svg')
	  .attr('width', w)
	  .attr('height', h)
	  .append('g')
//	  .attr('transform', 'translate(' + margin.left + ',' + margin.top + ')');

    let circleColor = attrs.circleColor ||display.getProperty("sparklineCircleColor","#000");
    let circleRadius = attrs.circleRadius ||display.getProperty("sparklineCircleRadius",1);
    let getColor = (d,i,dflt)=>{
	return "#000"
//	return colorBy?colorBy.getColorFromRecord(records[i], dflt):dflt;
    };
    console.log(JSON.stringify(range));

    let getNum = n=>{
	if(isNaN(n)) return 0;
	return n;
    };

    
    let recordMap = {};
    
    svg.selectAll('circle').data(data).enter().append("circle")
	.attr('r', (d,i)=>{return circleRadius})
	.attr('cx', (d,i)=>{return getNum(x(d.x))})
	.attr('cy', (d,i)=>{return getNum(y(d.y))})
	.attr('fill', (d,i)=>{return getColor(d,i,circleColor)})
	.attr(RECORD_ID, (d,i)=>{
	    recordMap[d.record.getId()] =d.record;
	    return d.record.getId()})
	.style("cursor", "pointer");

    let _display = display;
    let doTooltip = display.getProperty("sparklineDoTooltip", true)  || attrs.doTooltip;
    svg.on("click", function() {
	let coords = d3.mouse(this);
	if(records) {
	    let record = records[Math.round(x.invert(coords[0]))]
	    if(record)
		_display.propagateEventRecordSelection({select:true,record: record});
	}
    });


    if(doTooltip) {
	svg.on("mouseover", function() {
	    d3.select(this).attr("r", 10).style("fill", "red");
	    let ele = $(dom);
	    ele.attr('r', 20);
	    console.log("mouse over:" + d3.select(this).attr(RECORD_ID));
	    if(true) return
	    let record = recordMap[ele.attr(RECORD_ID)];
	    console.log(ele.attr(RECORD_ID) +" " + record);
	    let coords = d3.mouse(this);
	    if(!record) return;
	    let html = _display.getRecordHtml(record);
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

    let radius = Math.min(width, height) / 2 - margin
    let svg = d3.select(dom)
	.append("svg")
	.attr("width", width)
	.attr("height", height)
	.append("g")
	.attr("transform", "translate(" + width / 2 + "," + height / 2 + ")");
    let data = {};
    array.forEach(tuple=>{
	data[tuple[0]] = tuple[1];
    })

    // set the color scale
    let color = d3.scaleOrdinal()
	.domain(data)
	.range(colors)

    // Compute the position of each group on the pie:
    let pie = d3.pie()
	.value(function(d) {return d.value; })
    let data_ready = pie(d3.entries(data))

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


function SizeBy(display,records,fieldProperty) {
    this.display = display;
    if(!records) records = display.filterData();
    let pointData = this.display.getPointData();
    let fields = pointData?pointData.getRecordFields():[];
    $.extend(this, {
        id: this.display.getProperty(fieldProperty|| "sizeBy"),
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


function Annotations(display,records) {
    this.display = display;
    if(!records) records = this.display.filterData();
    let pointData = this.display.getPointData();
    let fields = pointData.getRecordFields();
    this.labelField = this.display.getFieldById(null,this.display.getProperty("annotationLabelField"));
    this.fields = this.display.getFieldsByIds(null,this.display.getProperty("annotationFields"));
    let prop = this.display.getProperty("annotations");
    if(prop) this.fields = [];
    this.map = {}
    let add = (record,index,annotation)=>{
	annotation.record = record;
	if(!this.map[index])
	    this.map[index] = [];
	this.map[index].push(annotation);
	if(!this.map[record.getId()])
	    this.map[record.getId()] = [];
	this.map[record.getId()].push(annotation);	
    }
    if(prop) {
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
	    let annotation = {label: label,description: desc,toString:function() {return this.label+" " + this.description;}   };
	    this.annotations.push(annotation);
	    if(index.match(/^[0-9]+$/)) {
		index = parseFloat(index);
	    } else {
		let index2 = null;
		if(index.indexOf(":")>=0) {
		    index2 = index.split(":")[1];
		    index = index.split(":")[0];
		}
		let desc2=null;
		
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
		    desc  = desc||(this.display.formatDate(index)+"-"+ this.display.formatDate(index2));
		    annotation.index2 = index2.getTime();
		} else {
		    desc  = desc||this.display.formatDate(index)
		}
		isDate = true;
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
			add(record,rowIdx,annotation);
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
		add(minRecord,minIndex,annotation);

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

    getAnnotationsFor: function(rowIdx) {
	return this.map[rowIdx];
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



let Gfx = {
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
	let ctx = canvas.getContext("2d");
	//	ctx.strokeStyle= "#000";
//	ctx.fillStyle= "rgba(255,0,0,0.25)";	
//	ctx.fillRect(0,0,canvas.width,canvas.height);

	let cnt = 0;
	let earthWidth = args.bounds.east-args.bounds.west;
	let earthHeight= args.bounds.north-args.bounds.south;
	ctx.font = opts.cellFont || "8pt Arial;"
	let gradient = ctx.createLinearGradient(0, 0, 0, canvas.height);
	gradient.addColorStop(0,'white');
	gradient.addColorStop(1,'red');

	let scaleX = (lat,lon)=>{
	    return Math.floor(opts.w*(lon-args.bounds.west)/earthWidth);
	};
	let scaleY;
	if(opts.display && opts.display.map) {
	    //Get the global bounds so we can map down to the image
	    let n1 = opts.display.map.transformLLPoint(MapUtils.createLonLat(opts.bounds.east,85));
	    let s1 = opts.display.map.transformLLPoint(MapUtils.createLonLat(opts.bounds.east,-85));
	    let n2 = opts.display.map.transformLLPoint(MapUtils.createLonLat(opts.bounds.east,opts.bounds.north));
	    let s2 = opts.display.map.transformLLPoint(MapUtils.createLonLat(opts.bounds.east,opts.bounds.south));
//	    console.log("n1:" + n1 +" s2:" + s1 +" n2:" + n2 +" s2:" + s2 +" bounds:" + JSON.stringify(opts.bounds));
	    scaleY = (lat,lon)=> {
		let pt = opts.display.map.transformLLPoint(MapUtils.createLonLat(lon,lat));
		let dy = n2.lat-pt.lat;
		let perc = dy/(n2.lat-s2.lat)
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

	    let countThreshold = opts.display.getProperty("hmCountThreshold",opts.operator=="count"?1:0);
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
	    for(let rowIdx=0;rowIdx<rows;rowIdx++)  {
		let row = grid[rowIdx];
		for(let colIdx=0;colIdx<cols;colIdx++)  {
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
		records.forEach((record,idx)=>{
		    let lat = record.getLatitude();
		    let lon = record.getLongitude();
		    let x = scaleX(lat,lon);
		    let y = scaleY(lat,lon);
		    record[gridId+"_coordinates"] = {x:x,y:y};
		    let colorValue = opts.colorBy? record.getData()[opts.colorBy.index]:null;
		    let lengthValue = opts.lengthBy? record.getData()[opts.lengthBy.index]:null;
		    glyph.draw(opts, canvas, ctx, x,y,{colorValue:colorValue, lengthValue:lengthValue,record:record},idx<10);
		});
	    });
	}

	let alpha = opts.display.getProperty("colorTableAlpha",-1);
	//add in the color table alpha
	if(alpha>0) {
	    let image = ctx.getImageData(0, 0, opts.w, opts.h);
	    let imageData = image.data,
		length = imageData.length;
	    for(let i=3; i < length; i+=4){  
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
	for(let rowIdx=0;rowIdx<rows;rowIdx++)  {
	    let row = [];
	    values.push(row);
	    for(let colIdx=0;colIdx<cols;colIdx++)  {
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

	for(let rowIdx=0;rowIdx<rows;rowIdx++)  {
	    for(let colIdx=0;colIdx<cols;colIdx++)  {
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
	for(let rowIdx=0;rowIdx<src.length;rowIdx++)  {
	    let row = result[rowIdx];
	    for(let colIdx=0;colIdx<row.length;colIdx++)  {
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
	for(let rowIdx=0;rowIdx<grid.length;rowIdx++)  {
	    let row = grid[rowIdx];
	    let h = "";
	    for(let colIdx=0;colIdx<row.length;colIdx++)  {
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
	let filterPasses = opts.display.getProperty("hmFilterPasses",1);
	for(let i=0;i<filterPasses;i++) {
	    filtered = this.blurGrid(opts.filter,filtered);
	}
	let filterThreshold = opts.display.getProperty("hmFilterThreshold",-999);
	for(let rowIdx=0;rowIdx<grid.length;rowIdx++)  {
	    let row = grid[rowIdx];
	    for(let colIdx=0;colIdx<row.length;colIdx++)  {
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
	for(let rowIdx=0;rowIdx<src.length;rowIdx++)  {
	    let row = src[rowIdx];
	    for(let colIdx=0;colIdx<row.length;colIdx++)  {
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
	for(let rowIdx=0;rowIdx<src.length;rowIdx++)  {
	    let row = src[rowIdx];
	    let nrow=[];
	    dest.push(nrow);
	    for(let colIdx=0;colIdx<row.length;colIdx++)  {
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
	let mapLonLeft = bounds.west;
	let mapLonRight = bounds.east;
	let mapLonDelta = mapLonRight - mapLonLeft;
	let mapLatBottom = bounds.south;
	let mapLatBottomDegree = mapLatBottom * Math.PI / 180;
	let x = (lon - mapLonLeft) * (mapWidth / mapLonDelta);
	lat = lat * Math.PI / 180;
	let worldMapWidth = ((mapWidth / mapLonDelta) * 360) / (2 * Math.PI);
	let mapOffsetY = (worldMapWidth / 2 * Math.log((1 + Math.sin(mapLatBottomDegree)) / (1 - Math.sin(mapLatBottomDegree))));
	let y = mapHeight - ((worldMapWidth / 2 * Math.log((1 + Math.sin(lat)) / (1 - Math.sin(lat)))) - mapOffsetY);
	return [x, y];
    },
}


function Glyph(display, scale, fields, records, args, attrs) {
    args = args||{};
    $.extend(this,{
	display: display,
	type:"label",
	records:records,
	dx:0,
	dy:0,
	label:"",
	baseHeight:0,
	baseWidth:0,
	width:8,
	fill:true,
	stroke:true,
	toString: function() {
	    return this.type;
	}
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
	//Check for the ${...} macros
	if(name=='colorBy')
	    value = value.replace('\${','').replace('}','');

	if(value=="true") value=true;
	else if(value=="false") value=false;
	this[name] = value;
//	console.log(name+"="+value);
    });

    if(this.labelBy) {
	this.labelField=display.getFieldById(fields,this.labelBy);
	if(!this.labelField) {
	    console.log("Could not find label field: " + this.labelBy);
	}
    }

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
    
    let cvrt = s=>{
	if(!isNaN(+s)) return +s;
	s  = String(s);
	s = s.replace(/canvasWidth2/g,""+(this.canvasWidth/2)).replace(/canvasWidth/g,this.canvasWidth);
	s = s.replace(/canvasHeight2/g,""+(this.canvasHeight/2)).replace(/canvasHeight/g,this.canvasHeight);	
	s = s.replace(/width2/g,""+(this.width/2)).replace(/width/g,this.width);	
	s = s.replace(/height2/g,""+(this.height/2)).replace(/height/g,this.width);	
	try {
	    s = eval(s);
	} catch(err) {
	    console.error('error evaling glyph value:' + s,err);
	}
	return s;
    };
    this.width = cvrt(this.width);
    this.height = cvrt(this.height);

    this.dx = cvrt(this.dx);
    this.dy = cvrt(this.dy);    


    this.baseWidth = +this.baseWidth;
    this.width = (+this.width)*scale;
    this.height = (+this.height)*scale;
    this.dx = (+this.dx)*scale;
    this.dy = (+this.dy)*scale;
    if(this.sizeBy) {
	this.sizeByField=display.getFieldById(fields,this.sizeBy);
	if(!this.sizeByField) {
	    console.log("Could not find sizeBy field:" + this.sizeBy);
	} else  {
	    let props = {
		Min:this.sizeByMin,
		Max:this.sizeByMax,
	    };
	    this.sizeByInfo =  new ColorByInfo(display, fields, records, this.sizeBy,this.sizeBy, null, this.sizeBy,this.sizeByField,props);
	}
    }


    this.dontShow =false;
    if(!this.colorByInfo && this.colorBy) {
	this.colorByField=display.getFieldById(fields,this.colorBy);
	let ct = this.colorTable?display.getColorTableInner(true, this.colorTable):null;
	if(!this.colorByField) {
	    console.log("Could not find colorBy field:" + this.colorBy);
//	    console.log("Fields:" + fields);
	    this.dontShow =true;
	} else {
	    let props = {
		Min:this.colorByMin,
		Max:this.colorByMax,
	    };	    
	    this.colorByInfo =  new ColorByInfo(display, fields, records, this.colorBy,this.colorBy+".colorByMap", ct, this.colorBy,this.colorByField, props);
	}
    }


    if(this.requiredField) {
	if(!display.getFieldById(fields,this.requiredField)) {
	    this.dontShow = true;
	}
    }
}


Glyph.prototype = {
    draw: function(opts, canvas, ctx, x,y,args,debug) {
	if(this.dontShow)return;
	debug = this.debug??debug;
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
	ctx.fillStyle =color || this.fillStyle || this.color || 'blue';
	ctx.strokeStyle =this.strokeStyle || this.color || '#000';
	ctx.lineWidth=this.lineWidth??this.strokeWidth??1;
	if(this.type=='label' || this.label) {
	    let label = this.labelField?args.record.getValue(this.labelField.getIndex()):this.label;
	    if(label===null) {
		console.log('No label value');
		return;
	    }
	    let text = String(label);
	    if(args.record) {
		text = this.display.applyRecordTemplate(args.record, null,null,text,{
		    entryname:this.entryname
		});
	    }

	    if(!isNaN(parseFloat(text))) {
		if(this.valueScale) {
		    text = text* +this.valueScale;
		}
		if(Utils.isDefined(this.decimals))
		    text = number_format(text,+this.decimals);
	    }
	    if(this.template) {
		text = this.template.replace('${value}',text);
	    }
	    //Normalize the font
	    if(this.font && this.font.match(/\d+(px|pt)$/)) {
		this.font = this.font +' sans-serif';
	    }

	    ctx.font = this.font ??  this.display.getProperty('glyphFont','12pt sans-serif');
	    ctx.fillStyle = ctx.strokeStyle =    color || this.color|| this.display.getProperty('glyphColor','#000');

	    if(debug) console.log('glyph label: font=' + ctx.font +' fill:' + ctx.fillStyle +' stroke:' + ctx.strokeStyle);
	    text = text.replace(/\${.*}/g,'');
	    text = text.replace(/_nl_/g,'\n').split('\n');
	    //remove any macros that did not get set

	    let h = 0;
	    let hgap = 3;
	    let maxw = 0;
	    let pady = +(this.pady??2);
	    text.forEach((t,idx)=>{
		let dim = ctx.measureText(t);
		if(idx>0) h+=hgap;
		maxw=Math.max(maxw,dim.width);
		h +=dim.actualBoundingBoxAscent+dim.actualBoundingBoxDescent;
	    });
	    let pt = Utils.translatePoint(x, y, maxw,  h, this.pos,{dx:this.dx,dy:this.dy});
	    if(debug) console.log('position:',{point:pt,x:x,y:y,text_width:maxw,text_height:h,pos:this.pos,dx:this.dx,dy:this.dy});
	    let bg = this.bg;
	    text.forEach(t=>{
		let dim = ctx.measureText(t);
		if(bg) {
		    ctx.fillStyle = bg;
		    let pad = +(this.bgpad??6);
		    if(debug) console.log('drawing background:' + bg +' padding:' + pad);
		    let rh = dim.actualBoundingBoxAscent+dim.actualBoundingBoxDescent;
		    let rw = dim.width;
		    ctx.fillRect(pt.x-pad,pt.y-rh-pad,rw+2*pad,rh+2*pad);
		}
		ctx.fillStyle = ctx.strokeStyle =    color || this.color|| this.display.getProperty('glyphColor','#000');
		dim = ctx.measureText(t);
		let offset =dim.actualBoundingBoxAscent+dim.actualBoundingBoxDescent;
		if(debug) console.log('draw text:' + t +' x:' + pt.x +' y:'+ (pt.y+offset));
		ctx.fillText(t,pt.x,pt.y+offset);
		pt.y += pady+dim.actualBoundingBoxAscent + dim.actualBoundingBoxDescent + hgap;
	    });
	} else 	if(this.type == 'circle') {
	    ctx.beginPath();
	    let w = this.width*lengthPercent+ this.baseWidth;
	    let pt = Utils.translatePoint(x, y, w,  w, this.pos,{dx:this.dx,dy:this.dy});
	    let cx = pt.x+w/2;
	    let cy = pt.y+w/2;
	    if(debug) console.log('draw circle',{cx:cx,cy:cy,w:w});
	    ctx.arc(cx,cy, w/2, 0, 2 * Math.PI);
	    if(this.fill)  {
		ctx.fill();
	    }
	    if(this.stroke) 
		ctx.stroke();
	} else if(this.type=='rect') {
	    let pt = Utils.translatePoint(x, y, this.width,  this.height, this.pos,{dx:this.dx,dy:this.dy});
	    if(this.fill)  
		ctx.fillRect(pt.x,pt.y, this.width, this.height);
	    if(this.stroke) 
		ctx.strokeRect(pt.x,pt.y, this.width, this.height);
	} else if(this.type=='image') {
	    let src = this.url;
	    if(!src && this.imageField) {
		src =  args.record.getValue(this.imageField.getIndex());
	    }
	    if(src) {
		src= src.replace('\${root}',ramaddaBaseUrl);
		this.width = +(this.width??50);
		this.height = +(this.height??50);		
		let pt = Utils.translatePoint(x, y, this.width,  this.height, this.pos,{dx:this.dx,dy:this.dy});
		if(this.debug) console.log('image glyph:' + src,{pos:this.pos,pt:pt,x:x,y:y,dx:this.dx,dy:this.dy,width:this.width,height:this.height});
		let i = new Image();
		i.src = src;
		if(!i.complete) {
		    let loaded = false;
		    i.onload=()=>{
			ctx.drawImage(i,pt.x,pt.y,this.width,this.width);
			loaded=true;
		    }
		    return () =>{
			return loaded;
		    }
		} else {
		    ctx.drawImage(i,pt.x,pt.y,this.width,this.width);
		}
	    } else {
		console.log('No url defined for glyph image');
	    }
	} else 	if(this.type == 'gauge') {
	    let pt = Utils.translatePoint(x, y, this.width,  this.height, this.pos,{dx:this.dx,dy:this.dy});
	    ctx.fillStyle =  this.fillColor || '#F7F7F7';
	    ctx.beginPath();
	    let cx= pt.x+this.width/2;
	    let cy = pt.y+this.height;
	    ctx.arc(cx,cy, this.width/2,  1 * Math.PI,0);
	    ctx.fill();
	    ctx.strokeStyle =   '#000';
	    ctx.stroke();
	    ctx.beginPath();
	    ctx.beginPath();
	    let length = this.width/2*0.75;
            let degrees = (180*lengthPercent);
	    let ex = cx-this.width*0.4;
	    let ey = cy;
	    let ep = Utils.rotate(cx,cy,ex,ey,degrees);
	    ctx.strokeStyle =  this.color || '#000';
	    ctx.lineWidth=this.lineWidth||2;
	    ctx.moveTo(cx,cy);
	    ctx.lineTo(ep.x,ep.y);
	    ctx.stroke();
	    ctx.lineWidth=1;
	    this.showLabel = true;
	    if(this.showLabel && this.sizeByInfo) {
		ctx.fillStyle='#000';
		let label = String(this.sizeByInfo.minValue);
		ctx.font = this.font || '9pt arial'
		let dim = ctx.measureText(label);
		ctx.fillText(label,cx-this.width/2-dim.width-2,cy);
		ctx.fillText(this.sizeByInfo.maxValue,cx+this.width/2+2,cy);
	    }
	} else if(this.type=='3dbar') {
	    let pt = Utils.translatePoint(x, y, this.width,  this.height, this.pos,{dx:this.dx,dy:this.dy});
	    let height = lengthPercent*(this.height) + parseFloat(this.baseHeight);
	    ctx.fillStyle =   color || this.color;
	    ctx.strokeStyle = this.strokeStyle||'#000';
	    this.draw3DRect(canvas,ctx,pt.x, 
			    canvas.height-pt.y-this.height,
			    +this.width,height,+this.width);
	    
	} else if(this.type=='axis') {
	    let pt = Utils.translatePoint(x, y, this.width,  this.height, this.pos,{dx:this.dx,dy:this.dy});
	    let height = lengthPercent*(this.height) + parseFloat(this.baseHeight);
	    ctx.strokeStyle = this.strokeStyle||'#000';
	    ctx.beginPath();
	    ctx.moveTo(pt.x,pt.y);
	    ctx.lineTo(pt.x,pt.y+this.height);
	    ctx.lineTo(pt.x+this.width,pt.y+this.height);
	    ctx.stroke();
	} else if(this.type == 'vector') {
	    if(!this.sizeByInfo) {
		console.log('make Vector: no sizeByInfo');
		return;
	    }
	    ctx.strokeStyle =   color || this.color;
	    let v = args.record.getValue(this.sizeByField.getIndex());
	    lengthPercent = this.sizeByInfo.getValuePercent(v);
	    let length = opts.cellSizeH;
	    if(opts.lengthBy && opts.lengthBy.index>=0) {
		length = opts.lengthBy.scaleToValue(v);
	    }
	    let x2=x+length;
	    let y2=y;
	    let arrowLength = opts.display.getArrowLength();
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
		ctx.fillStyle='#000';
		ctx.beginPath();
		ctx.arc(x,y, 1, 0, 2 * Math.PI);
		ctx.fill();
		ctx.restore();
	    }
	    ctx.beginPath();
	    ctx.moveTo(x,y);
	    ctx.lineTo(x2,y2);
	    ctx.lineWidth=opts.display.getLineWidth();
	    ctx.stroke();
	    if(arrowLength>0) {
		ctx.beginPath();
		this.drawArrow(ctx, x,y,x2,y2,arrowLength);
		ctx.stroke();
	    }
	} else if(this.type=='tile'){
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
	    ctx.strokeStyle = '#000';
	    //	    ctx.fill();
	    ctx.stroke();
	} else {
	    console.log('Unknown cell shape:' + this.type);
	}
    },
    draw3DRect:function(canvas,ctx,x,y,width, height, depth) {
	// Dimetric projection functions
	let dimetricTx = function(x,y,z) { return x + z/2; };
	let dimetricTy = function(x,y,z) { return y + z/4; };
	
	// Isometric projection functions
	let isometricTx = function(x,y,z) { return (x -z) * Math.cos(Math.PI/6); };
	let isometricTy = function(x,y,z) { return y + (x+z) * Math.sin(Math.PI/6); };
	
	let drawPoly = (function(ctx,tx,ty) {
	    return function() {
		let args = Array.prototype.slice.call(arguments, 0);
		// Begin the path
		ctx.beginPath();
		// Move to the first point
		let p = args.pop();
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

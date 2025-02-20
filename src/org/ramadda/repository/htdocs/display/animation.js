/**
   Copyright (c) 2008-2025 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/



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
	labelTemplate:display.getProperty("animationLabelTemplate"),
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
        useIndex: display.getProperty("animationUseIndex",false),
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
	setBeginEnd: function(date) {
	    this.setBegin(date);
	    this.setEnd(date);	    
	},
	setBegin: function(date) {
	    this.begin = this.makeDate(date);
	},
	setEnd: function(date) {
	    this.end = this.makeDate(date);
	},
	makeIndex:function(idx) {
	    return {
		wrapper:true,
		isIndex:true,
		index:idx,
		getTime:function() {return this.value;},
		value:idx};
	},

	makeDate: function(date) {
	    if(!date) return null;
	    if(this.useIndex) {
		return this.makeIndex(Utils.isDefined(date.index)?date.index:date.getTime());
	    }

	    if(date.wrapper) date = date.date;
	    return {
		wrapper:true,
		date:date,
		value:date.getTime(),
		getTime:function() {return this.value;}
	    }
	},
	init: function(dateMin, dateMax, records) {
	    let debug = false;
	    if(debug)
		console.log("animation.init:" +dateMin +" " + dateMax +" " +(records?"#records:" + records.length: "no records") );
	    let _this = this;
	    this.records = records;
	    if(this.useIndex) {
		this.dateMin = this.makeIndex(0);
		this.dateMax = this.makeIndex(records.length-1);		
	    } else {
		this.dateMin = this.makeDate(dateMin);
		this.dateMax = this.makeDate(dateMax);
	    }
	    let beginDate = this.dateMin;
	    let endDate = this.dateMax;	    
	    this.setBegin(beginDate);
	    this.setEnd(endDate);
	    if(!this.dateMin) return;
	    this.dates=[];
	    let seen = {};
	    this.dateToRecordMap = {};
	    records.every((r,idx)=>{
		if(this.useIndex) {
		    this.dates.push(this.makeIndex(idx));
		    this.dateToRecordMap[idx] = r;
		    return true;
		}
		let date = r.getDate();
		if(!date) return;
		if(!seen[date]) {
		    seen[date] = true;
		    this.dates.push(this.makeDate(date));
		    this.dateToRecordMap[date] = r;
		}
		return true;
	    });
	    this.dates.sort(function(a,b) {
		return a.value - b.value;
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
		let slider = this.slider = this.jq(ID_SLIDER).slider({
		    range: _this.mode != MODE_FRAME,
		    min: _this.dateMin.getTime(),
		    max: _this.dateMax.getTime(),
		    values: sliderValues,
		    create: function() {
			_this.sliderHandleLeft = $(".ui-slider-handle:eq(0)");
			_this.sliderHandleRight = $(".ui-slider-handle:eq(1)");
			if(_this.sliderHandleLeft.length &&_this.sliderHandleRight.length) {
			    _this.sliderHandleLeft.attr(ATTR_TITLE,'Shift-drag to move both');
			    _this.sliderHandleRight.attr(ATTR_TITLE,'Shift-drag to move both');
			}
		    },
		    slide: function( event, ui ) {
			_this.stopAnimation();
			_this.checkSliderValues(event,ui);
			_this.setSliderValues(ui.values);
			_this.updateLabels();
		    },
		    stop: function(event,ui) {
			_this.checkSliderValues(event,ui);
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
	checkSliderValues:function(event,ui) {
	    if(event.shiftKey && this.lastSliderValues) {
		let left= this.sliderHandleLeft[0]==ui.handle;
		if(left) {
		    let delta=ui.values[0]-this.lastSliderValues[0];
		    ui.values[1]+=delta;
		} else {
		    let delta=ui.values[1]-this.lastSliderValues[1];
		    ui.values[0]+=delta;
		}
		this.slider.slider("values", ui.values);
	    }
	    this.lastSliderValues=ui.values;
	},
	resetRange: function() {
	    if(this.display.getProperty("animationInitRange")) {
		let toks = Utils.split(this.display.getProperty("animationInitRange"),",");
		let beginIdx = 0;
		let idx=0;
		if(toks[0].trim()=='begin')  idx=0;
		else  idx=+toks[0];
		if(idx<0) idx=this.records.length+idx;
		let record = this.records[idx];
		if(record)	this.setBegin(record.getTime());
		if(toks.length>1) {
		    if(toks[1].trim()=='end')  idx=this.records.length-1;
		    else idx=+toks[1];
		    record = this.records[idx];
		    if(record)   this.setEnd(record.getTime());
		}
		return
	    }



	    if(this.startAtEnd) {
		this.setBegin(this.dateMax);
		this.setEnd(this.dateMax);
		if (this.mode == MODE_FRAME) {
		    this.frameIndex = this.dates.length-1;
		}		    
	    } else   if(this.startAtBeginning) {
		this.setBegin(this.dateMin);
		this.setEnd(new Date(this.begin.getTime()+this.window));
	    }
	    if (this.mode == MODE_FRAME) {
		this.setEnd(this.begin);
	    }
	},
	setWindow: function() {
	    let window = this.display.getProperty("animationWindow");
	    let step = this.display.getProperty("animationStep", window);
	    if (window) {
		this.window = DataUtils.timeToMillis(window);
	    } else if(this.steps>0){
		if(this.useIndex) {
		    this.window = 1;
		} else {
		    this.window = this.dateRange / this.steps;
		}
	    }
	    if (step) {
		this.step = DataUtils.timeToMillis(step);
	    } else {
		this.step = this.window;
	    }
	},
	getIndex: function() {
	    return this.frameIndex;
	},
	getBeginTime: function() {
	    return this.begin;
	},
	handleEventAnimationChanged(args) {
	    this.setBegin(args.begin);
	    this.setEnd(args.end);
	    this.stopAnimation();
	    this.applyAnimation();
	},
	setSliderValues: function(v) {
	    let debug = false;
	    if(debug)
		console.log(this.display.type+" animation.setSliderValues");
	    if(this.mode != MODE_FRAME) {
		this.setBegin(new Date(v[0]));
		this.setEnd(new Date(v[1]));
	    } else {
		let sliderDate = new Date(v[0]);
		let closest = this.dates[0];
		let dist = 0;
		let closestIdx=0;
		this.dates.forEach((d,idx)=>{
		    if(Math.abs(d.value-sliderDate.getTime()) < Math.abs(closest.getTime()-sliderDate.getTime())) {
			closest = d;
			closestIdx = idx;
		    }
		});
		this.setBegin(closest);
		this.setEnd(closest);
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
		    _this.dateMin = _this.makeDate(new Date(_this.dateMin.getTime()+dt));
		    _this.dateMax = _this.makeDate(new Date(_this.dateMax.getTime()+dt));			
		    _this.updateTicks();
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
			_this.dateMin = _this.makeDate(new Date(_this.dateMin.getTime()+diff));
			_this.dateMax = _this.makeDate(new Date(_this.dateMax.getTime()-diff));			
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
		    _this.setBegin(_this.dateMin);
		    _this.setEnd(_this.dateMax);
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
		this.setBegin(this.dateMin);
		if (this.mode == MODE_SLIDING) {
		    this.end = this.makeDate(new Date(this.begin.getTime()+(fullRange?this.window:diff)));
		} else if (this.mode == MODE_FRAME) {
		    this.frameIndex = 0;
		    let date = this.deltaFrame(0);
		    this.setBeginEnd(date);
		} else {
		    this.setEnd(new Date(this.dateMin.getTime()+this.window));
		}
		this.stopAnimation();
		this.dateRangeChanged();
            });
            this.btnEnd.button().click(() => {
		let diff = this.getDiff();
		let fullRange = this.fullRange();
		this.end = this.dateMax;
		if (this.mode == MODE_SLIDING) {
		    this.setBegin(new Date(this.end.getTime()-(fullRange?this.window:diff)));
		} else if (this.mode == MODE_FRAME) {
		    this.frameIndex = this.dates.length+1;
		    this.setBeginEnd(this.deltaFrame(0));
		} else {
		    this.setEnd(this.dateMax);
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
		this.setBegin(new Date(this.begin.getTime()-diff));
		if(this.begin.getTime()<this.dateMin.getTime())
		    this.setBegin(this.dateMin);
		this.setEnd(new Date(this.begin.getTime()+diff));
	    } else if (this.mode == MODE_FRAME) {
		this.setBeginEnd(this.deltaFrame(-1));
	    } else {
		this.setEnd(new Date(this.end.getTime()-this.window));
		if(this.end.getTime()<=this.begin.getTime()) {
		    this.setEnd(new Date(this.begin.getTime()+this.window));
		}
	    }
	    this.dateRangeChanged();
	},
	doNext: function() {
	    let debug = false;
	    let wasAtEnd = this.atEnd();
//	    debug=true;
	    if(debug) console.log("animation.doNext:" + this.mode +" atEnd=" + wasAtEnd);

	    if (this.mode == MODE_SLIDING) {
		let window = this.window||this.getDiff();
		this.setBegin(new Date(this.begin.getTime()+this.step));
		this.setEnd(new Date(this.end.getTime()+this.step));
		//this.end.getTime()+this.window);		
		if(this.atEnd()) {
		    this.end = this.dateMax;
		    this.setBegin(new Date(this.end.getTime()-window));
		    this.inAnimation = false;
		    this.stopAnimation();
		}
	    } else if (this.mode == MODE_FRAME) {
		this.setBeginEnd(this.deltaFrame(1));
		if(this.running) {
		    if(wasAtEnd) {
			if(this.display.getProperty("animationLoop",true)) {
			    setTimeout(()=>{
				this.setBegin(this.dateMin);
				this.setEnd(this.dateMin);
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
		this.end = this.makeDate(new Date(this.end.getTime()+this.window));
		if(this.atEnd()) {
		    this.end = this.dateMax;
		    this.inAnimation = false;
		    this.stopAnimation();
		}
	    }
	    this.dateRangeChanged();
	},
	//This gets called when another display propagates its animation times
	setTimes:function(times) {
	    if(!this.getEnabled()) return;
	    this.setBeginEnd(times[0],times[times.length-1]);
	    this.dates.every((date,idx)=>{
		if(date.getTime()==times[0].getTime()) {
		    this.frameIndex=idx;
		    return false;
		}
		return true;
	    });
	    
	    this.updateUI();
	    this.dateRangeChanged();
	},
	deltaFrame: function(delta) {
	    this.frameIndex+=delta;
	    if(!this.dates) return;
	    if(this.frameIndex>= this.dates.length) {
		this.frameIndex = this.dates.length-1;
	    }   else if(this.frameIndex<0) {
		this.frameIndex = 0;
	    }
	    return this.dates[this.frameIndex];
	},
	startAnimation: function() {
            if (!this.dateMax) return;
	    if (!this.inAnimation) {
                this.inAnimation = true;
                this.label.html("");
		if (this.mode == MODE_FRAME) {
		    this.frameIndex =0;
		    this.setBeginEnd(this.deltaFrame(0));
		    this.display.animationStart();
		    this.doNext();
		    return;
		}
                if(this.fullRange()) {
		    this.end = this.makeDate(new Date(this.begin.value+this.window));
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
	    this.setBegin(begin);
	    this.setEnd(end);
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
	    //Buffer the apply calls in case the user is clicking really fast
	    if(this.applyTimeout) {
		clearTimeout(this.applyTimeout);
	    }
	    this.applyTimeout = setTimeout(()=>{
		this.applyTimeout = null;
		this.display.animationApply(this);
		this.updateUI();
	    },20);
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
	    this.dateMin = this.makeDate(new Date(this.dateMin.getTime()+diff));
	    this.dateMax = this.makeDate(new Date(this.dateMax.getTime()-diff));			
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
		let date;
		if(this.useIndex) {
		    date = i;
		}  else {
		    let dttm = record.getDate();
		    if(!dttm) continue;
		    date = record.getDate().getTime();
		    if(seenDate[date]) continue;
		    seenDate[date] = true;
		    if(debug)console.log("\ttick:" + record.getDate());
		}
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

	applyLabelTemplate: function(records) {
	    if(!this.labelTemplate) return;
	    if(records &&records.length ) {
		let record = records[0];
		let label = this.display.applyRecordTemplate(record, null,null,this.labelTemplate);
		this.label.html(this.makeLabel(label));
	    }
	},

	updateLabels: function() {
	    if(!this.label) return;
	    if(!this.makeSlider) {
		this.label.html(HU.leftCenterRight(this.makeLabel(this.formatAnimationDate(this.dateMin)),this.makeLabel("# " +this.tickCount), this.makeLabel(this.formatAnimationDate(this.dateMax))));
	    } else {
		if(this.labelTemplate) {
		    //If there is a labelTemplate then the display will call applyLabelTemplate when
		    //it has filtered its records
		    return;
		}
		if (this.mode == MODE_FRAME && this.begin.getTime() == this.end.getTime()) {
		    this.label.html(this.makeLabel(this.formatAnimationDate(this.begin)));
		} else {
		    this.label.html(this.makeLabel(this.formatAnimationDate(this.begin) + " - " + this.formatAnimationDate(this.end)));
		}
	    }
	},
        formatAnimationDate: function(date,format,debug) {
	    if(Utils.isDefined(date.index)) return "#" +(date.index+1);
	    if(date.date) date = date.date;
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

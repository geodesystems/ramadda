/**
   Copyright (c) 2008-2026 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

function AreaWidget(display,arg) {
    this.arg=arg;
    this.createTime  = new Date();
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
    const ARG_MAP_CONTAINS="map_contains";
    let mapContains = this.arg?null:Utils.stringDefined(HU.getUrlArgument(ARG_MAP_CONTAINS))?HU.getUrlArgument(ARG_MAP_CONTAINS)=='true':true;
    $.extend(this, {
	areaContains: mapContains,
        display: display,
	domId:function(id) {
	    if(this.arg!=null)
		id =  this.arg+'_'+ id;
	    return this.display.domId(id);
	},
	jq:function(id) {
	    return jqid(this.domId(id));
	},
        initHtml: function() {
	    this.jq(ID_SETTINGS).click(()=>{
		this.showSettings();
	    });
	    this.jq(ID_MAP_SHOW).click(()=>{
		this.showMap();
	    });

	    let params = {};
	    this.map =  new RepositoryMap(this.domId(ID_MAP_POPUP), params);
	    this.map.setSelection(this.arg?this.domId(''):this.display.getId(),true,1);
	},
        showSettings: function() {
	    let _this = this;
	    let html = "";
	    html+= HU.div([ATTR_CLASS,CLASS_CLICKABLE,
			   ATTR_TITLE, "Use my location",
			   ATTR_ID,this.domId(ID_SET_LOCATION)],
			  HU.getIconImage("fas fa-compass") + SPACE + "Use my location");
            html += HU.div([ATTR_CLASS,CLASS_CLICKABLE,
			    ATTR_TITLE, "Clear form",
			    ATTR_ID,this.domId(ID_CLEAR)],
			   HU.getIconImage(ICON_ERASER) + SPACE + "Clear form");
	    html+= HU.div([ATTR_TITLE, "Search mode: checked - contains, unchecked - overlaps"],
			  HU.checkbox("",[ATTR_ID, this.domId(ID_CONTAINS)], this.areaContains) +
			  HU.tag(TAG_LABEL,[ATTR_CLASS,CLASS_CLICKABLE,
					    ATTR_FOR,this.domId(ID_CONTAINS)], SPACE + "Contains"));
	    html = HU.div([ATTR_STYLE,HU.css(CSS_MARGIN,HU.px(5))], html);
	    this.settingsDialog = HU.makeDialog(
		{content:html,anchor:this.jq(ID_SETTINGS),draggable:false,header:true});
	    this.jq(ID_CONTAINS).change(function(e) {
		_this.areaContains = HU.isChecked($(this));
	    });
	    this.jq(ID_SET_LOCATION).click(()=>{
		this.settingsDialog.remove();
		this.useMyLocation();
	    });
	    this.jq(ID_CLEAR).click(()=>{
		this.settingsDialog.remove();
		this.areaClear();
	    });	    
	},
        getHtml: function() {
	    let bounds =  this.arg?null:HU.getUrlArgument(ARG_MAPBOUNDS);
	    let n="",w="",s="",e="";
	    if(bounds) {
		[n,w,s,e]  = bounds.split(",");
	    }
            let callback = this.display.getGet();
            let settings = HU.div([ATTR_TITLE,"Settings",
				   ATTR_CLASS,CLASS_CLICKABLE,
				   ATTR_ID,this.domId(ID_SETTINGS)],HU.getIconImage("fas fa-cog"));
	    let showMap = HU.div([ATTR_CLASS,CLASS_CLICKABLE,
				  ATTR_ID,this.domId(ID_MAP_SHOW),
				  ATTR_TITLE,"Show map selector"], HU.getIconImage("fas fa-globe"));

	    let input = (id,place,title,v)=>{
		return HU.input(id, v, [ATTR_PLACEHOLDER, place,
					ATTR_CLASS, HU.classes('input','display-area-input'),
					ATTR_SIZE, 5,
					ATTR_ID,this.domId(id),
					ATTR_TITLE, title]);
	    };
            let areaForm = HU.openTag(TAG_TABLE,
				      [ATTR_CLASS, "display-area"]);
            areaForm += HU.tr([],
			      HU.td([ATTR_ALIGN, ALIGN_CENTER],
				    HU.leftCenterRight("",
						       input(ID_NORTH, " N","North",n),
						       showMap,
						       "20%", "60%", "20%")));

            areaForm += HU.tr([], HU.td([],
					input(ID_WEST, " W", "West",w) +
					input(ID_EAST, " E", "East",e)));

            areaForm += HU.tr([],
			      HU.td([ATTR_ALIGN, ALIGN_CENTER],
				    HU.leftCenterRight("", input(ID_SOUTH,  " S", "South",s),
						       settings,
						       "20%", "60%", "20%")));


            areaForm += HU.closeTag(TAG_TABLE);
            areaForm += HU.div([ATTR_ID,this.domId(ID_MAP_POPUP_WRAPPER),
				ATTR_STYLE,HU.css(CSS_DISPLAY,DISPLAY_NONE)],
			       SPACE+"Shift-drag: select region. Cmd-drag: move region" +
			       HU.div([ATTR_ID,this.domId(ID_MAP_POPUP),
				       ATTR_STYLE,HU.css(CSS_WIDTH,HU.px(400),
							 CSS_HEIGHT,HU.px(300))]));
            return areaForm;
        },
	showMap: function() {
	    let anchor = this.jq(ID_MAP_SHOW);
	    this.dialog = HU.makeDialog({
		contentId:this.domId(ID_MAP_POPUP_WRAPPER),
		anchor:anchor,
		draggable:true,
		header:true});
	    this.map.selectionPopupInit();
	    this.map.getMap().updateSize();
	},
        areaClear: function() {
            jqid(this.domId(ID_NORTH)).val("");
            jqid(this.domId(ID_WEST)).val("");
            jqid(this.domId(ID_SOUTH)).val("");
            jqid(this.domId(ID_EAST)).val("");
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

            jqid(this.domId(ID_NORTH)).val(lat + offset);
            jqid(this.domId(ID_WEST)).val(lon - offset);
            jqid(this.domId(ID_SOUTH)).val(lat - offset);
            jqid(this.domId(ID_EAST)).val(lon + offset);
            if (this.display.submitSearchForm)
                this.display.submitSearchForm();
        },
        areaLinkClick: function() {
            this.linkArea = !this.linkArea;
            let image = root + (this.linkArea ? "/icons/link.png" : "/icons/link_break.png");
            jqid(this.domId(ID_AREA_LINK)).attr(ATTR_SRC, image);
            if (this.linkArea && this.lastBounds) {
                let b = this.lastBounds;
                jqid(this.domId(ID_NORTH)).val(MapUtils.formatLocationValue(b.top));
                jqid(this.domId(ID_WEST)).val(MapUtils.formatLocationValue(b.left));
                jqid(this.domId(ID_SOUTH)).val(MapUtils.formatLocationValue(b.bottom));
                jqid(this.domId(ID_EAST)).val(MapUtils.formatLocationValue(b.right));
            }
        },
        linkArea: false,
        lastBounds: null,
        handleEventMapBoundsChanged: function(source, args) {
            bounds = args.bounds;
            this.lastBounds = bounds;
            if (!args.force && !this.linkArea) return;
            jqid(this.domId(ID_NORTH)).val(MapUtils.formatLocationValue(bounds.top));
            jqid(this.domId(ID_WEST)).val(MapUtils.formatLocationValue(bounds.left));
            jqid(this.domId(ID_SOUTH)).val(MapUtils.formatLocationValue(bounds.bottom));
            jqid(this.domId(ID_EAST)).val(MapUtils.formatLocationValue(bounds.right));
        },
	getContains: function() {
	    return HU.isChecked(this.jq(ID_CONTAINS));
	},
        getValues: function(settings) {
	    return {
		north:this.jq(ID_NORTH).val(),
		west:this.jq(ID_WEST).val(),		
		south:this.jq(ID_SOUTH).val(),
		east:this.jq(ID_EAST).val(),
	    }
	},
        setSearchSettings: function(settings) {
	    let n = this.display.getFieldValue(this.domId(ID_NORTH), null);
	    let w = this.display.getFieldValue(this.domId(ID_WEST), null);	    
	    let s = this.display.getFieldValue(this.domId(ID_SOUTH), null);
	    let e = this.display.getFieldValue(this.domId(ID_EAST), null);
            settings.setAreaContains(this.areaContains);
            settings.setBounds(n,w,s,e);
	    let now = new Date();
	    let okToAddToUrl = now.getTime()-this.createTime.getTime()>5000;
	    if(okToAddToUrl) 
		HU.addToDocumentUrl(ARG_MAP_CONTAINS,this.areaContains);
	    if(Utils.stringDefined(n,w,s,e)) {
		if(okToAddToUrl) 
		    HU.addToDocumentUrl(ARG_MAPBOUNDS,[n||"",w||"",s||"",e||""].join(","));
	    } else {
		HU.removeFromDocumentUrl(ARG_MAPBOUNDS);
	    }
        },
    });
}


function DateRangeWidget(display, what,startLabel,endLabel) {
    const ID_DATE_START = "date_start";
    const ID_DATE_END = "date_end";
    this.what = what??'date';
    if(this.what == 'createdate') {
	startLabel = 'Create start';
	endLabel = 'Create end';
    } else    if(this.what == 'changedate') {
	startLabel = 'Change start';
	endLabel = 'Change end';	
    } else if(this.what=='date' || startLabel==null) {
	startLabel = display.getProperty("date.start.label","Start date");
	endLabel = display.getProperty("date.end.label","End date");	
    }


    this.baseId = this.what;
    RamaddaUtil.inherit(this, {
        display: display,
        initHtml: function() {
	    let args= HU.makeClearDatePickerArgs({dateFormat: "yy-mm-dd",changeMonth:true,changeYear:true});
            jqid(this.baseId +ID_DATE_START).datepicker(args);
            jqid(this.baseId +ID_DATE_END).datepicker(args);
        },
	getStartEnd: function() {
            let start = jqid(this.baseId +ID_DATE_START).val();
            let end =  jqid(this.baseId +ID_DATE_END).val();
	    return {start:start,end:end};
	},
        setSearchSettings: function(settings) {
            let start = jqid(this.baseId +ID_DATE_START).val();
            let end =  jqid(this.baseId +ID_DATE_END).val();
	    HU.addToDocumentUrl(this.baseId+ID_DATE_START,Utils.stringDefined(start)?start:null);
	    HU.addToDocumentUrl(this.baseId+ID_DATE_END,Utils.stringDefined(end)?end:null);		    	    
	    if(this.what=="createdate") {
		settings.setCreateDateRange(start, end);
	    } else    if(this.what=="changedate") {
		settings.setChangeDateRange(start, end);		
	    } else    if(this.what=="date") {
		settings.setDateRange(start, end);
	    } else {
	    }
        },
        getHtml: function() {
	    let start = HU.getSanitizedUrlArgument(this.baseId+ID_DATE_START);
	    let end = HU.getSanitizedUrlArgument(this.baseId+ID_DATE_END);	    
            let html = HU.input(this.baseId +ID_DATE_START, start||"",
				[ATTR_CLASS, "display-date-input",
				 ATTR_PLACEHOLDER, " " +startLabel,
				 ATTR_TITLE, startLabel,
				 ATTR_ID, this.baseId +ID_DATE_START, 
				]) + " - " +
                HU.input(this.baseId +ID_DATE_END, end||"",
			 [ATTR_CLASS, "display-date-input",
			  ATTR_PLACEHOLDER,  " " +endLabel,
			  ATTR_TITLE,endLabel,
			  ATTR_ID, this.baseId +ID_DATE_END, 
			 ]);
            return html;
        }
    });
}



function drawSparkline(display, dom,w,h,data, records,min,max,colorBy,params) {
    if(w<0 || h<0) {
	return;
    }

    let opts = {
	theMargin:{ top: 0, right: 0, bottom: 0, left: 0 },
	flipYAxis:false,
	drawAxis:true,
	drawAxisLabels:false,	
	axisWidth:1,
	axisColor:COLOR_LIGHT_GRAY,
    }
    if(params) {
	if(params.margin) $.extend(opts.theMargin,params.margin);
	$.extend(opts,params);
    }
    const INNER_WIDTH  = w - opts.theMargin.left - opts.theMargin.right;
    const INNER_HEIGHT = h - opts.theMargin.top - opts.theMargin.bottom;
    const BAR_WIDTH  = w / data.length;
    const x    = d3.scaleLinear().domain([0, data.length]).range([0, INNER_WIDTH]);
    let y = !opts.flipYAxis?
	d3.scaleLinear().domain([min, max]).range([INNER_HEIGHT, 0]):
	d3.scaleLinear().domain([max, min]).range([INNER_HEIGHT, 0]);    
    const recty    = d3.scaleLinear().domain([min, max]).range([0,INNER_HEIGHT]);

    let tt = d3.select(TAG_BODY).append(TAG_DIV)	
	.attr(ATTR_CLASS, "sparkline-tooltip")				
	.style(CSS_OPACITY, 0);

    const svg = d3.select(dom).append(TAG_SVG)
	  .attr(ATTR_WIDTH, w)
	  .attr(ATTR_HEIGHT, h)
	  .append(TAG_G)
	  .attr(ATTR_TRANSFORM, HU.translate(opts.theMargin.left, opts.theMargin.top));
    const line = d3.line()
	  .x((d, i) => x(i))
	  .y(d => y(d));

    let lineColor = opts.lineColor||display.getSparklineLineColor();
    let barColor = opts.barColor ||display.getSparklineBarColor();
    let circleColor = opts.circleColor ||display.getSparklineCircleColor();
    let circleRadius = opts.circleRadius ||display.getSparklineCircleRadius();
    let lineWidth = opts.lineWidth ||display.getSparklineLineWidth();
    let defaultShowEndPoints = false;
    let getColor = (d,i,dflt)=>{
	return colorBy?colorBy.getColorFromRecord(records[i], dflt):dflt;
    };
    let showBars = opts.showBars|| display.getSparklineShowBars();
    if(opts.drawAxisLabels) {
	let minLabel= opts.flipYAxis?Utils.formatNumber(max):Utils.formatNumber(min);	
	let maxLabel = opts.flipYAxis?Utils.formatNumber(min):Utils.formatNumber(max);	
	svg.append('text')
	    .attr(ATTR_X, 5) 
	    .attr(ATTR_Y, h-5) 
	    .attr("text-anchor", "left") 
	    .attr('font-size','8pt')
	    .text(minLabel);
	svg.append('text')
	    .attr(ATTR_X, 5) 
	    .attr(ATTR_Y, 0+10) 
	    .attr("text-anchor", "left") 
	    .attr('font-size','8pt')
	    .text(maxLabel);	
    }

    if(opts.drawAxis) {
	svg.append('line')
	    .attr('x1',0)
	    .attr('y1', 0)
	    .attr('x2', 0)
	    .attr('y2', h)    
	    .attr(ATTR_STROKE_WIDTH, opts.axisWidth)
    	    .attr(ATTR_STROKE, opts.axisColor);

	svg.append('line')
	    .attr('x1',0)
	    .attr('y1', h)
	    .attr('x2', w)
	    .attr('y2', h)    
	    .attr(ATTR_STROKE_WIDTH, opts.axisWidth)
    	    .attr(ATTR_STROKE, opts.axisColor);
    }
    


    let getNum = n=>{
	if(isNaN(n)) return 0;
	return n;
    };

    if(showBars) {
	defaultShowEndPoints = false;
	svg.selectAll('.bar').data(data)
	    .enter()
	    .append(TAG_RECT)
	    .attr(ATTR_CLASS, 'bar')
	    .attr('x', (d, i) => getNum(x(i)))
	    .attr('y', d => getNum(y(d)))
	    .attr(ATTR_WIDTH, BAR_WIDTH)
	    .attr(ATTR_HEIGHT, d => getNum(h-y(d)))
	    .attr(ATTR_FILL, (d,i)=>getColor(d,i,barColor))
	    .style(CSS_CURSOR, CURSOR_POINTER)
    }


    if(opts.showLines|| display.getSparklineShowLines()) {
	svg.selectAll('line').data(data).enter().append("line")
	    .attr('x1', (d,i)=>{return x(i)})
	    .attr('y1', (d,i)=>{return y(d)})
	    .attr('x2', (d,i)=>{return x(i+1)})
	    .attr('y2', (d,i)=>{return y(i<data.length-1?data[i+1]:data[i])})
	    .attr(ATTR_STROKE_WIDTH, lineWidth)
            .attr(ATTR_STROKE, (d,i)=>{
		if(isNaN(d)) return HU.rgb(0,0,0,0);
		return getColor(d,i,lineColor)
	    })
	    .style(CSS_CURSOR, CURSOR_POINTER);
    }


    if(opts.showCircles || display.getSparklineShowCircles()) {
	svg.selectAll('circle').data(data).enter().append("circle")
	    .attr('r', (d,i)=>{return isNaN(d)?0:circleRadius})
	    .attr('cx', (d,i)=>{return getNum(x(i))})
	    .attr('cy', (d,i)=>{return getNum(y(d))})
	    .attr(ATTR_FILL, (d,i)=>getColor(d,i,circleColor))
	    .style(CSS_CURSOR, CURSOR_POINTER);
    }



    if(opts.showEndpoints || display.getSparklineShowEndPoints(defaultShowEndPoints)) {
	let fidx=0;
	while(isNaN(data[fidx]) && fidx<data.length) fidx++;
	let lidx=data.length-1;
	while(isNaN(data[lidx]) && lidx>=0) lidx--;	
	svg.append('circle')
	    .attr('r', opts.endPointRadius|| display.getSparklineEndPointRadius())
	    .attr('cx', x(fidx))
	    .attr('cy', y(data[fidx]))
	    .attr(ATTR_FILL, opts.endPoint1Color || display.getSparklineEndPoint1Color() || getColor(data[0],0,display.getSparklineEndPoint1Color()));
	svg.append('circle')
	    .attr('r', opts.endPointRadius|| display.getSparklineEndPointRadius())
	    .attr('cx', x(lidx))
	    .attr('cy', y(data[lidx]))
	    .attr(ATTR_FILL, opts.endPoint2Color || display.getSparklineEndPoint2Color()|| getColor(data[data.length-1],data.length-1,display.getSparklineEndPoint2Color()));
    }
    let _display = display;
    let doTooltip = display.getSparklineDoTooltip()  || opts.doTooltip;
    svg.on("click", function(event) {
	let coords = d3.pointer(event);
	if(records) {
	    let record = records[Math.round(x.invert(coords[0]))]
	    if(record)
		_display.propagateEventRecordSelection({select:true,record: record});
	}
    });



    if(doTooltip) {
	svg.on("mouseover", function(event) {
	    if(!records) return;
	    let coords = d3.pointer(event);
	    let record = records[Math.round(x.invert(coords[0]))]
	    if(!record) return;
	    let html = _display.getRecordHtml(record);
	    let ele = $(dom);
	    let offset = ele.offset().top + ele.height();
	    let left = ele.offset().left;
	    tt.transition().duration(200).style(CSS_OPACITY, .9);		
	    tt.html(html)
		.style(CSS_LEFT, HU.px(left))		
		.style(CSS_TOP, HU.px(offset));	
	})
	    .on("mouseout", function(d) {		
		tt.transition()		
		    .duration(500)		
		    .style(CSS_OPACITY, 0);
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
    let tt = d3.select(TAG_BODY).append(TAG_DIV).attr(ATTR_CLASS, "sparkline-tooltip").style(CSS_OPACITY, 0);
    const svg = d3.select(dom).append(TAG_SVG)
	  .attr(ATTR_WIDTH, w)
	  .attr(ATTR_HEIGHT, h)
	  .append(TAG_G);
    //	  .attr('transform', HU.translate(margin.left, margin.top));

    let circleColor = attrs.circleColor ||display.getProperty("sparklineCircleColor",COLOR_BLACK);
    let circleRadius = attrs.circleRadius ||display.getProperty("sparklineCircleRadius",1);
    let getColor = (d,i,dflt)=>{
	return COLOR_BLACK;
	//	return colorBy?colorBy.getColorFromRecord(records[i], dflt):dflt;
    };
    console.log(JSON.stringify(range));

    let getNum = n=>{
	if(isNaN(n)) return 0;
	return n;
    };

    
    let recordMap = {};
    svg.selectAll('circle').data(data).enter().append('circle')
	.attr('r', (d,i)=>{return circleRadius})
	.attr('cx', (d,i)=>{return getNum(x(d.x))})
	.attr('cy', (d,i)=>{return getNum(y(d.y))})
	.attr(ATTR_FILL, (d,i)=>{return getColor(d,i,circleColor)})
	.attr(ATTR_RECORD_ID, (d,i)=>{
	    recordMap[d.record.getId()] =d.record;
	    return d.record.getId()})
	.style(CSS_CURSOR, CURSOR_POINTER);

    let _display = display;
    let doTooltip = display.getProperty("sparklineDoTooltip", true)  || attrs.doTooltip;
    svg.on("click", function(event) {
	let coords = d3.pointer(event);
	if(records) {
	    let record = records[Math.round(x.invert(coords[0]))]
	    if(record)
		_display.propagateEventRecordSelection({select:true,record: record});
	}
    });


    if(doTooltip) {
	svg.on("mouseover", function(event) {
	    d3.select(this).attr("r", 10).style(CSS_FILL, "red");
	    let ele = $(dom);
	    ele.attr('r', 20);
	    if(true) return
	    let record = recordMap[ele.attr(ATTR_RECORD_ID)];
	    let coords = d3.pointer(event);
	    if(!record) return;
	    let html = _display.getRecordHtml(record);
	    let offset = ele.offset().top + ele.height();
	    let left = ele.offset().left;
	    tt.transition().duration(200).style(CSS_OPACITY, .9);		
	    tt.html(html)
		.style(CSS_LEFT,HU.px(left))		
		.style(CSS_TOP, HU.px(offset));	
	}).on("mouseout", function(d) {		
	    tt.transition()		
		.duration(500)		
		.style(CSS_OPACITY, 0);
	});
    }
}




function drawPieChart(display, dom,width,height,array,min,max,colorBy,attrs) {
    if(!attrs) attrs = {};
    let margin = Utils.isDefined(attrs.margin)?attrs.margin:4;
    let colors = attrs.pieColors||Utils.ColorTables.cats.colors;
    let colorMap = attrs.colorMap;
    if(!colorMap) {
	colorMap  = {};
	array.forEach((tuple,idx)=>{
	    let key = tuple[0];
	    colorMap[key] = colors[idx%colors.length];
	})
    }

    let radius = Math.min(width, height) / 2 - margin
    let svg = d3.select(dom)
	.append(TAG_SVG)
	.attr(ATTR_WIDTH, width)
	.attr(ATTR_HEIGHT, height)
	.append(TAG_G)
	.attr(ATTR_TRANSFORM, HU.translate(width / 2, height / 2));
    let data = {};
    array.forEach(tuple=>{
	data[tuple[0]] = tuple[1];
    })


    // Compute the position of each group on the pie:
    let pie = d3.pie().value(function(d) {return d.value; })
    let data_ready = pie(Utils.makeKeyValueList(data));

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
	.attr(ATTR_FILL, function(d){
	    return colorMap[d.data.key];
	})
	.attr(ATTR_STROKE, COLOR_BLACK)
	.style(CSS_STROKE_WIDTH, HU.px(1))
	.style(CSS_OPACITY, 0.7)
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
    if(!prop) return;
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
	if(index.match(/^#[0-9]+$/)) {
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
	    legendLabel = HU.href(url, legendLabel,[ATTR_TARGET,"_annotation"]);
	}
	this.legend+= HU.b(label)+": " + legendLabel+" ";
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





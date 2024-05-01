/**
   Copyright 2008-2024 Geode Systems LLC
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



    let mapContains = this.arg?null:Utils.stringDefined(HU.getUrlArgument("map_contains"))?HU.getUrlArgument("map_contains")=='true':true;
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
	    html+= HU.div([CLASS,"ramadda-clickable",TITLE, "Use my location",ID,this.domId(ID_SET_LOCATION)],
			  HU.getIconImage("fas fa-compass") + SPACE + "Use my location");
            html += HU.div([CLASS,"ramadda-clickable",TITLE, "Clear form",ID,this.domId(ID_CLEAR)],
			  HU.getIconImage("fas fa-eraser") + SPACE + "Clear form");
	    html+= HU.div([TITLE, "Search mode: checked - contains, unchecked - overlaps"],
			  HtmlUtils.checkbox("",[ID, this.domId(ID_CONTAINS)], this.areaContains) +HU.tag("label",[CLASS,"ramadda-clickable","for",this.domId(ID_CONTAINS)], SPACE + "Contains"));
	    html = HU.div([STYLE,"margin:5px;"], html);
	    this.settingsDialog = HU.makeDialog({content:html,anchor:this.jq(ID_SETTINGS),draggable:false,header:true});
	    this.jq(ID_CONTAINS).change(function(e) {
		_this.areaContains = $(this).is(':checked');
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
	    let bounds =  this.arg?null:HU.getUrlArgument("map_bounds");
	    let n="",w="",s="",e="";
	    if(bounds) {
		[n,w,s,e]  = bounds.split(",");
	    }
            let callback = this.display.getGet();
            let settings = HU.div([TITLE,"Settings",CLASS,"ramadda-clickable",ID,this.domId(ID_SETTINGS)],HU.getIconImage("fas fa-cog"));
	    let showMap = HU.div([CLASS,"ramadda-clickable",ID,this.domId(ID_MAP_SHOW),TITLE,"Show map selector"], HtmlUtils.getIconImage("fas fa-globe"));

	    let input = (id,place,title,v)=>{
		return HtmlUtils.input(id, v, ["placeholder", place, ATTR_CLASS, "input display-area-input", "size", "5", ATTR_ID,
						this.domId(id), ATTR_TITLE, title]);
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
            areaForm += HU.div([ID,this.domId(ID_MAP_POPUP_WRAPPER),STYLE,HU.css("display","none")],SPACE+"Shift-drag: select region. Cmd-drag: move region" +
				HU.div([ID,this.domId(ID_MAP_POPUP),STYLE,HU.css("width","400px","height","300px")]));
            return areaForm;
        },
	showMap: function() {
	    let anchor = this.jq(ID_MAP_SHOW);
	    this.dialog = HU.makeDialog({contentId:this.domId(ID_MAP_POPUP_WRAPPER),anchor:anchor,draggable:true,header:true});
	    this.map.selectionPopupInit();
	    this.map.getMap().updateSize();
	},
        areaClear: function() {
            $("#" + this.domId(ID_NORTH)).val("");
            $("#" + this.domId(ID_WEST)).val("");
            $("#" + this.domId(ID_SOUTH)).val("");
            $("#" + this.domId(ID_EAST)).val("");
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

            $("#" + this.domId(ID_NORTH)).val(lat + offset);
            $("#" + this.domId(ID_WEST)).val(lon - offset);
            $("#" + this.domId(ID_SOUTH)).val(lat - offset);
            $("#" + this.domId(ID_EAST)).val(lon + offset);
            if (this.display.submitSearchForm)
                this.display.submitSearchForm();
        },
        areaLinkClick: function() {
            this.linkArea = !this.linkArea;
            let image = root + (this.linkArea ? "/icons/link.png" : "/icons/link_break.png");
            $("#" + this.domId(ID_AREA_LINK)).attr("src", image);
            if (this.linkArea && this.lastBounds) {
                let b = this.lastBounds;
                $("#" + this.domId(ID_NORTH)).val(MapUtils.formatLocationValue(b.top));
                $("#" + this.domId(ID_WEST)).val(MapUtils.formatLocationValue(b.left));
                $("#" + this.domId(ID_SOUTH)).val(MapUtils.formatLocationValue(b.bottom));
                $("#" + this.domId(ID_EAST)).val(MapUtils.formatLocationValue(b.right));
            }
        },
        linkArea: false,
        lastBounds: null,
        handleEventMapBoundsChanged: function(source, args) {
            bounds = args.bounds;
            this.lastBounds = bounds;
            if (!args.force && !this.linkArea) return;
            $("#" + this.domId(ID_NORTH)).val(MapUtils.formatLocationValue(bounds.top));
            $("#" + this.domId(ID_WEST)).val(MapUtils.formatLocationValue(bounds.left));
            $("#" + this.domId(ID_SOUTH)).val(MapUtils.formatLocationValue(bounds.bottom));
            $("#" + this.domId(ID_EAST)).val(MapUtils.formatLocationValue(bounds.right));
        },
	getContains: function() {
	    return this.jq(ID_CONTAINS).is(':checked');
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
		HU.addToDocumentUrl("map_contains",this.areaContains);
	    if(Utils.stringDefined(n,w,s,e)) {
		if(okToAddToUrl) 
		    HU.addToDocumentUrl("map_bounds",[n||"",w||"",s||"",e||""].join(","));
	    } else {
		HU.removeFromDocumentUrl("map_bounds");
	    }
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
	    let args= HtmlUtils.makeClearDatePickerArgs({dateFormat: "yy-mm-dd",changeMonth:true,changeYear:true});
            $("#" + this.baseId +ID_DATE_START).datepicker(args);
            $("#" + this.baseId +ID_DATE_END).datepicker(args);
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



function drawSparkline(display, dom,w,h,data, records,min,max,colorBy,params) {
    if(w<0 || h<0) {
	return;
    }

    let opts = {
	theMargin:{ top: 0, right: 0, bottom: 0, left: 0 }
    }
    if(params) {
	if(params.margin) $.extend(opts.theMargin,params.margin);
	$.extend(opts,params);
    }
    const INNER_WIDTH  = w - opts.theMargin.left - opts.theMargin.right;
    const INNER_HEIGHT = h - opts.theMargin.top - opts.theMargin.bottom;
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
	  .attr('transform', 'translate(' + opts.theMargin.left + ',' + opts.theMargin.top + ')');
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


    if(opts.showLines|| display.getSparklineShowLines()) {
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


    if(opts.showCircles || display.getSparklineShowCircles()) {
	svg.selectAll('circle').data(data).enter().append("circle")
	    .attr('r', (d,i)=>{return isNaN(d)?0:circleRadius})
	    .attr('cx', (d,i)=>{return getNum(x(i))})
	    .attr('cy', (d,i)=>{return getNum(y(d))})
	    .attr('fill', (d,i)=>getColor(d,i,circleColor))
	    .style("cursor", "pointer");
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
	    .attr('fill', opts.endPoint1Color || display.getSparklineEndPoint1Color() || getColor(data[0],0,display.getSparklineEndPoint1Color()));
	svg.append('circle')
	    .attr('r', opts.endPointRadius|| display.getSparklineEndPointRadius())
	    .attr('cx', x(lidx))
	    .attr('cy', y(data[lidx]))
	    .attr('fill', opts.endPoint2Color || display.getSparklineEndPoint2Color()|| getColor(data[data.length-1],data.length-1,display.getSparklineEndPoint2Color()));
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
	    d3.select(this).attr("r", 10).style("fill", "red");
	    let ele = $(dom);
	    ele.attr('r', 20);
	    if(true) return
	    let record = recordMap[ele.attr(RECORD_ID)];
	    console.log(ele.attr(RECORD_ID) +" " + record);
	    let coords = d3.pointer(event);
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
	.append("svg")
	.attr("width", width)
	.attr("height", height)
	.append("g")
	.attr("transform", "translate(" + width / 2 + "," + height / 2 + ")");
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
	.attr('fill', function(d){
	    return colorMap[d.data.key];
	})
	.attr("stroke", "black")
	.style("stroke-width", "1px")
	.style("opacity", 0.7)
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
	    shape:"rect",
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
//	opts.cellSizeX=2;	opts.cellSizeY=2;	opts.cellSize=2;
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
				  {type:opts.shape,
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



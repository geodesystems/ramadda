



const DISPLAY_MAPGRID = "mapgrid";
const DISPLAY_MAPCHART = "mapchart";
const DISPLAY_MAPARRAY = "maparray";
const DISPLAY_MAPSHRINK = "mapshrink";
const DISPLAY_MAPIMAGES = "mapimages";

addGlobalDisplayType({
    type: DISPLAY_MAPGRID,
    label: "Map Grid",
    category:CATEGORY_MAPS,
    preview: "mapgrid.png",
    desc:"Can display US States or World countries",    
});

addGlobalDisplayType({
    type: DISPLAY_MAPCHART,
    label: "Map Chart",
    requiresData: true,
    category:CATEGORY_MAPS,
    preview:"mapchart.png",
    desc:"Plot numeric data as heights. Can display US States, European countries or world countries",        
});


addGlobalDisplayType({
    type: DISPLAY_MAPSHRINK,
    label: "Map Shrink",
    requiresData: true,
    category:CATEGORY_MAPS,
    tooltip: makeDisplayTooltip("Show values as relative size of map regions","mapshrink.png","Can display US States, European countries or world countries"),            
});


addGlobalDisplayType({
    type: DISPLAY_MAPARRAY,
    label: "Map Array",
    requiresData: true,
    category:CATEGORY_MAPS,
    tooltip: makeDisplayTooltip("Colored map regions displayed separately","maparray.png","Can display US States, European countries or world countries"),                
});
addGlobalDisplayType({
    type: DISPLAY_MAPIMAGES,
    label: "Map Images",
    requiresData: true,
    category:CATEGORY_MAPS,
    tooltip: makeDisplayTooltip("Display images in map regions","mapimage.png","Can display US States, European countries or world countries"),                    
});



let ramaddaGridState = {
    grids:{
    },
    loading:{
    }

};

function RamaddaMapgridDisplay(displayManager, id, properties) {
    const SUPER =  new RamaddaFieldsDisplay(displayManager, id, DISPLAY_MAPGRID, properties);
    let myProps = [
	{label:'Grid Map Attributes'},
	{p:'localeField',ex:''},
	{p:'grid',ex:'countries|us'},
	{p:'cellSize',ex:'30',tt:'use 0 for flexible width'},
	{p:'cellHeight',ex:'30'},
	{p:'showCellLabel',ex:'false'},
    ];
    myProps.push(...RamaddaDisplayUtils.sparklineProps);

    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
        needsData: function() {
            return true;
        },
        displayData: function(reload) {
	    this.updateUI();
	},
	handleEventFieldsSelected: function(source, fields) {
	    if(this.getProperty("selectedFieldIsColorBy") && fields.length>0) {
		this.colorByFieldChanged(fields[0]);
	    }
        },
	colorByFieldChanged:function(field) {
	    this.haveCalledUpdateUI = false;
	    this.setProperty("colorBy", field);
	    this.vectorMapApplied  = false;
	    this.updateUI({colorByFieldChanged:true});
	},
	updateUI: function() {
	    let what = this.getProperty("grid","us");
	    let grid = ramaddaGridState.grids[what];
	    if(!grid) {
		if(ramaddaGridState.loading[what]) return;
		ramaddaGridState.loading[what] = true;
		let url = RamaddaUtil.getCdnUrl("/resources/grid_"+ what+".json");
		$.getJSON(url, ( data ) =>{
		    ramaddaGridState.grids[what] = data;
		    this.updateUI();
		}).fail(function(err) {
		    console.log( "error loading " + url);
		    console.dir(err)
		})
		return;
	    }


	    let records = this.filterData();
	    if(!records) return;
            let fields = this.getData().getNonGeoFields();
	    let localeField = this.getFieldById(fields,this.getProperty("localeField","state"));
	    if(localeField==null) {
		localeField = this.getFieldById(fields,"state");
	    }
	    let minx = Number.MAX_VALUE;
	    let miny = Number.MAX_VALUE;
	    let maxx = Number.MIN_VALUE;
	    let maxy = Number.MIN_VALUE;
	    let map = {};


	    grid.forEach(o=>{
		minx = Math.min(minx,o.x);
		maxx = Math.max(maxx,o.x);
		miny = Math.min(miny,o.y);
		maxy = Math.max(maxy,o.y);
		map[this.domId("cell_" +o.x+ "_"+o.y)] = o;
	    });

            let colorBy = this.getColorByInfo(records);
	    let sparkLinesColorBy = this.getColorByInfo(records,"sparklineColorBy");
	    let strokeColorBy = this.getColorByInfo(records,"strokeColorBy","strokeColorByMap");
	    let sparkLineField = this.getFieldById(fields,this.getProperty("sparklineField"));
	    let table =HU.open(TABLE,[WIDTH,"100%"]);
	    let width = this.getProperty("cellWidth", this.getProperty("cellSize",0));
	    let height = this.getProperty("cellHeight",width);
	    if(height==0) height=30;
	    let showLabel  = this.getProperty("showCellLabel",true);
	    let cellStyle  = this.getProperty("cellStyle","");
	    let cellMap = {};
	    for(let y=1;y<=maxy;y++) {
		table+=HU.open(TR);
		for(let x=1;x<=maxx;x++) {
		    let id = this.domId("cell_" +x+ "_"+y);
		    let o = map[id];
		    let extra = " id='" + id +"' ";
		    let style = HU.css('position','relative','margin','1px','vertical-align','center','text-align','center',HEIGHT, height+"px");
		    if(width>0) style+=HU.css(WIDTH,width+'px');
		    let c = "";
		    if(o) {
			style+="background:#ccc;" + cellStyle;
			if(!sparkLineField) {
			    extra += " title='" + o.name +"' ";
			}
			extra += HU.attr(CLASS,'display-mapgrid-cell');
			c = HU.div([STYLE,HU.css('padding-left','3px')], (showLabel?o.codes[0]:""));
			o.codes.forEach(c=>cellMap[c] = id);
			cellMap[o.name] = id;
		    }
		    let td = HU.td([],"<div " + extra +" style='" + style +"'>" + c+"</div>");
		    table+=td;
		}
		table+=HU.close(TR);
	    }
	    table +=HU.tr([],HU.td(["colspan", maxx],"<br>" +   HU.div([ID,this.domId(ID_COLORTABLE)])));
	    table+=HU.close(TABLE);
	    this.setContents(HU.center(table));

	    let states = [];
	    let stateData = this.stateData = {
	    }
	    let minData = 0;
	    let maxData = 0;
	    let seen = {};
	    let contents = this.getContents();
	    for(let i=0;i<records.length;i++) {
		let record = records[i]; 
		let tuple = record.getData();
		let state = tuple[localeField.getIndex()];
		let cellId = cellMap[state];
		if(!cellId) {
		    cellId = cellMap[state.toUpperCase()];
		}
		if(!cellId) {
		    //		    console.log("Could not find cell:" + state);
		    continue;
		}
		$("#"+cellId).attr(RECORD_INDEX,i);

		if(!stateData[state]) {
		    states.push(state);
		    stateData[state] = {
			cellId: cellId,
			data:[],
			records:[]
		    }
		}
		if(sparkLineField) {
		    let value = record.getValue(sparkLineField.getIndex());
		    if(!isNaN(value)) {
			minData = i==0?value:Math.min(minData, value);
			maxData = i==0?value:Math.max(maxData, value);
			stateData[state].data.push(value);
			stateData[state].records.push(record);
		    }
		}

		let colorByEnabled = colorBy.isEnabled();

		//TODO: sort the state data on time
                if (colorByEnabled) {
                    let value = record.getData()[colorBy.index];
		    let color = colorBy.getColorFromRecord(record);
		    let cell = contents.find("#" + cellId);
		    cell.css("background",color);
		    let foreground = Utils.getForegroundColor(color);
		    if(foreground) {
			cell.css('color', foreground);
		    }
		    cell.attr(RECORD_INDEX,i);
                }
		if (strokeColorBy.isEnabled()) {
                    let value = record.getData()[strokeColorBy.index];
		    let color = strokeColorBy.getColor(value, record);
		    let cell = contents.find("#" + cellId);
		    cell.css("border-color",color);
		    cell.css("border-width","2px");
                }
	    }

	    if(sparkLineField) {
		let vOffset = 0;
		states.forEach((state,idx)=>{
		    let s = stateData[state];
		    let innerId = s.cellId+"_inner";
		    let cellWidth = width;
		    if(cellWidth==0) {
			cellWidth = $("#" + s.cellId).width();
		    }
		    let style = HU.css(WIDTH,cellWidth+'px',HEIGHT, (height-vOffset) +'px','position','absolute','left','0px','top', vOffset+'px');
		    let innerDiv = HU.div([ID, innerId, STYLE,style]);
		    $("#" + s.cellId).append(innerDiv);
		    drawSparkline(this, "#"+innerId,cellWidth,height-vOffset,s.data,s.records,minData,maxData,sparkLinesColorBy);
		});
	    }

	    this.makePopups(contents.find(".display-mapgrid-cell"), records);
	    let _this = this;
	    contents.find(".display-mapgrid-cell").click(function() {
		let record = records[$(this).attr(RECORD_INDEX)];
		if(record) {
		    _this.propagateEventRecordSelection({record: record});
		}
	    });	
	    if(!sparkLineField) {
		this.makeTooltips(contents.find(".display-mapgrid-cell"), records, null, "${default}");
	    }
            if (colorBy.index >= 0) {
		colorBy.displayColorTable();
	    }
	    if (sparkLinesColorBy.index >= 0) {
		sparkLinesColorBy.displayColorTable();
	    }
	},

        handleEventRecordSelection: function(source, args) {
	    let contents = this.getContents();
	    if(this.selectedCell) {
		this.selectedCell.css("border",this.selectedBorder);
	    }
	    let index = this.recordToIndex[args.record.getId()];
	    if(!Utils.isDefined(index)) return;
	    this.selectedCell = contents.find(HU.attrSelect(RECORD_INDEX, index));
	    this.selectedBorder = this.selectedCell.css("border");
	    this.selectedCell.css("border","1px solid red");
	},
    })}




const ID_BASEMAP = "basemap";
function RamaddaOtherMapDisplay(displayManager, id, type, properties) {
    const SUPER = new RamaddaFieldsDisplay(displayManager, id, type, properties);
    let myProps = [
	{label:'Base map properties'},
	{p:'regionField',ex:''},
	{p:'mapFile',ex:'usmap.json|countries.json',d:"usmap.json"},
	{p:'mapEntry',tt:'entry id of geojson map file'},
	{p:'mapFeature',tt:'feature name to match data with',canCache:true},		
	{p:'valueField',tt:'Field to get the height of each polygon from',ex:''},
	{p:'skipRegions',ex:'Alaska,Hawaii'},
	{p:'pruneMissing',ex:'true'},				
	{p:'mapBackground',ex:'transparent'},
	{p:'transforms',ex:"Alaska,0.4,30,-40;Hawaii,2,50,5;Region,scale,offsetX,offsetY"},
	{p:'prunes',ex:'Alaska,100;Region,maxCount'},
	{p:'mapWidth',ex:'600'},
	{p:'mapHeight',ex:'400'},
	{p:'maxLon'},
	{p:'minLon'},
	{p:'maxLat'},
	{p:'minLat'},			
	{p:"strokeColor"},
	{p:"strokeWidth"},
	{p:"missingFill"},			
    ];

    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
        checkLayout: function() {
            this.updateUI();
        },
        makeMap: function() {
	},
	makePoly:function(polygon) {
	    let poly = [];
	    polygon.forEach(point=>{
		let lon = point[0];
		let lat = point[1];
		if(isNaN(lon) || isNaN(lat)) return;
		poly.push({x:lon,y:lat});
	    });
	    return poly;
	},
	findValues:function(region, valueMap) {
	    if(valueMap[region]) return valueMap[region];
	    let values = null;
	    if(!this.aliasMap[region]) {
		return null;
	    }
	    this.aliasMap[region].forEach(alias=>{
		if(valueMap[alias]) values = valueMap[alias];
	    });
	    return values;
	},
	makeValueMap: function(records,needsValue) {
	    let regionField=this.getFieldById(null,this.getPropertyRegionField());
	    let valueField=this.getFieldById(null,this.getPropertyValueField());	    
	    if(!regionField) {
                this.displayError("No region field specified");
		return null
	    }
	    if(!valueField && needsValue) {
                this.displayError("No value field specified");
		return null
	    }	    
	    //If the colorBy wasn't set then use the value field
	    if(valueField) {
		if(this.getProperty("colorBy")==null) this.setProperty("colorBy",valueField.getId());
		if(this.getProperty("sizeBy")==null) this.setProperty("sizeBy",valueField.getId());	    
	    }
	    let valueMap = {};
	    this.valueRange = {
		min: null,
		max:null
	    };
	    this.idToRecord = {};
	    records.forEach(record=>{
		let region = record.getValue(regionField.getIndex());
		this.idToRecord[record.getId()] = record;
		let values  = valueMap[region] = {
		    record:record
		}
		if(valueField) { 
		    let value = record.getValue(valueField.getIndex());
		    values.value = value;
		    this.valueRange.min = this.valueRange.min===null?value:Math.min(value,this.valueRange.min);
		    this.valueRange.max = this.valueRange.max===null?value:Math.max(value,this.valueRange.max);
		}
	    });
	    if(valueField) {
		records.forEach(record=>{
		    let region = record.getValue(regionField.getIndex());
		    let values  = valueMap[region];
		    let value = values.value
		    let percent = (value-this.valueRange.min)/(this.valueRange.max-this.valueRange.min);
		    values.percent = percent;
		});
	    }
	    return valueMap;
	},
	writeMap:function(skipHeight)  {
	    let width = this.getMapWidth(this.getProperty("width",800));
	    let css = HU.css(BACKGROUND,this.getMapBackground("transparent"),WIDTH,HU.getDimension(width));
	    let height;
	    if(!skipHeight) {
		height = this.getMapHeight(this.getProperty("height"));
		let mw = this.mapRange.maxLon-this.mapRange.minLon;
		let mh = this.mapRange.maxLat-this.mapRange.minLat;
		if(!height)
		    height = mh/mw*width;
		if(isNaN(height)) height=400; 
		css+=HU.css(HEIGHT,HU.getDimension(height));
	    }
	    
	    this.mapRange.maxLon= this.getPropertyMaxLon(this.mapRange.maxLon);
	    this.mapRange.minLon= this.getPropertyMinLon(this.mapRange.minLon);
	    this.mapRange.maxLat= this.getPropertyMaxLat(this.mapRange.maxLat);
	    this.mapRange.minLat= this.getPropertyMinLat(this.mapRange.minLat);	    	    
	    this.setContents(HU.div([ID,this.domId(ID_BASEMAP),STYLE,css]));
	    if(isNaN(width)) {
		width = this.getContents().width();
	    }
	    return [width,height];

	},
	makeSvg: function(width,height) {
	    const svg = d3.select("#" + this.domId(ID_BASEMAP)).append('svg')
		  .attr('width', width)
		  .attr('height', height)
		  .append('g')
	    let padx = 0;
	    let pady = padx;
	    let scaleX  = d3.scaleLinear().domain([this.mapRange.minLon, this.mapRange.maxLon]).range([padx, width-padx]);
	    let scaleY  = d3.scaleLinear().domain([this.mapRange.maxLat, this.mapRange.minLat]).range([pady, height-pady]);
	    return [svg,scaleX,scaleY];
	},

	clearTooltip: function() {
	    if(this.tooltipDiv)
		this.tooltipDiv.style("opacity", 0);
	},
	makeTooltipDiv: function() {
	    if(!this.tooltipDiv) {
		this.tooltipDiv = d3.select("body").append("div")
		    .attr(ATTR_CLASS, "ramadda-shadow-box  display-tooltip")
		    .style("opacity", 0)
		    .style("position", "absolute")
		    .style("background", "#fff")
	    }
	    this.clearTooltip();
	    return this.tooltipDiv;
	},
	addEvents:function(polys, idToRecord, tooltipDiv) {
	    let _this = this;
	    idToRecord  = idToRecord|| this.idToRecord;
	    tooltipDiv = tooltipDiv || this.makeTooltipDiv();
	    polys.on('click', function (event, d) {
		let poly = d3.select(this);
		let record = idToRecord[poly.attr(RECORD_ID)];
		if(record)
		    _this.propagateEventRecordSelection({record: record});
	    });
	    polys.on('mouseover', function (event, i) {
		let poly = d3.select(this);
		let record = idToRecord[poly.attr(RECORD_ID)];
		poly.attr("lastStroke",poly.attr("stroke"))
		    .attr("lastFill",poly.attr("fill"));
		poly.attr("stroke",_this.getProperty('highlightStrokeColor','blue')).attr("stroke-width",_this.getProperty('highlightStrokeWidth',1))
		    .attr("fill",_this.getProperty('highlightFillColor','blue'));
		let tooltip = _this.getProperty("tooltip");
		if(!tooltip) return;
		let regionName = poly.attr("regionName");
		let tt = null;
		if(!record) {
		    tt = regionName;
		    console.log("no record found for region:" +regionName);
		} else {
		    _this.propagateEventRecordSelection({highlight:true,record: record});
		    tt =  _this.getRecordHtml(record,null,tooltip);
		}
		if(tt) {
		    _this.tooltipDiv.html(tt)
			.style("left", (event.pageX + 10) + "px")
			.style("top", (event.pageY + 20) + "px");
		    _this.tooltipDiv.style("opacity", 1);
		    //For now don't transition as it seems to screw up
		    //subsequent mouse overs
		    /*
		    _this.tooltipDiv.transition()
			.delay(500)
			.duration(500)
			.style("opacity", 1);
		    */
		}
	    });
	    polys.on('mouseout', function (d, i) {
//		_this.tooltipDiv.transition();
		let poly = d3.select(this);
		poly.attr("stroke",poly.attr("lastStroke"))
		    .attr("fill",poly.attr("lastFill"))
		    .attr("stroke-width",1);
		_this.tooltipDiv.style("opacity", 0);
	    });
	},
        updateUI: function() {
	    this.clearTooltip();
	    if(!this.mapJson) {
		if(!this.gettingFile) {
		    this.gettingFile = true;
		    let mapFile = this.getPropertyMapFile();
		    let mapEntry = this.getMapEntry();
		    if(mapEntry!=null) {
			mapFile = RamaddaUtil.getUrl("/entry/get?entryid=" + mapEntry);
		    } else {
			if(!mapFile.startsWith("/") && !mapFile.startsWith("http")) {
			    mapFile =RamaddaUtil.getCdnUrl("/resources/" + mapFile);
			}
		    }
		    $.getJSON(mapFile, (data) =>{
			this.mapJson = data;
			this.regionNames=[];
			this.makeRegions();
			this.updateUI();
		    });
		}
		return;
	    }
	    if(!this.regions) {
		if(!this.makeRegions()) return;
	    }
	    this.makeMap();
	},
	makeRegions:function() {
	    if(!this.mapJson) return false;
	    let debug = this.getProperty("debug");
	    let pruneMissing = this.getPropertyPruneMissing(false);
	    let allRegions = {};
	    if(this.getData()==null) {
		return false;
	    }
	    let allRecords = this.getData().getRecords()
	    if(!allRecords) return;
	    let regionField=this.getFieldById(null,this.getPropertyRegionField());
	    if(regionField==null) {
		this.displayError("No region field");
		return false;
	    }
	    allRecords.forEach(record=>{
		let v = record.getValue(regionField.getIndex());
//		console.log("data region:" + v);
		allRegions[v] = true;
	    });
	    this.regions = {};
	    this.mapRange  = {
		minLon:null,
		maxLon:null,
		minLat:null,
		maxLat:null
	    };
	    let transforms = {}
	    let prunes = {}	    
	    this.getPropertyTransforms("").split(";").map(t=>t.split(",")).forEach(tuple=>{
		let region = tuple[0];
		transforms[region] = {
		    scale:tuple[1]!=null?+tuple[1]:1,
		    dx:tuple[2]!=null?+tuple[2]:0,
		    dy:tuple[3]!=null?+tuple[3]:0}
	    });

	    this.getPropertyPrunes("").split(";").map(t=>t.split(",")).forEach(tuple=>{
		let region = tuple[0];
		prunes[region] =  +tuple[1];
	    });

	    let tfunc=(region,polygon)=>{
		let prune = prunes[region];
		if(prune>0) {
		    if(polygon.length<prune) return null;
		}

		let transform = transforms[region];
		if(!transform) 
		    return polygon;
		let bounds = Utils.getBounds(polygon);
		let centerx = bounds.minx + (bounds.maxx-bounds.minx)/2;
		let centery = bounds.miny + (bounds.maxy-bounds.miny)/2;		
		polygon.map(pair=>{
		    pair[0]= (pair[0]-centerx)*transform.scale+centerx;
		    pair[1]= (pair[1]-centery)*transform.scale+centery;		    		    
		    pair[0] += transform.dx;
		    pair[1] += transform.dy;
		    return pair;
		});
		return polygon;		
	    };
	    
	    this.skipRegions = this.getPropertySkipRegions("").split(",").map(r=>r.replace(/_comma_/g,","));
	    let features = this.mapJson.geojson;
	    if(!features)
		features = this.mapJson.features;

	    this.aliasMap = {};
	    let mapFeature = this.getMapFeature();
	    features.forEach(blob=>{
		let region;
		if(mapFeature) {
		    region = blob.properties[mapFeature];
		} else  {
		    region = blob.properties.name || blob.properties.name_long || blob.properties.NAME || blob.properties.ADMIN;
		}
		let aliases = [region];
		//Some hacks
		if(region=="United States of America") aliases.push("United States");
		if(region=="United Republic of Tanzania") aliases.push("Tanzania");
		if(region=="Democratic Republic of the Congo") aliases.push("Democratic Republic of Congo");
		if(region=="Czech Rep.") aliases.push("Czech Republic");
		if(region=="Bosnia and Herz.") aliases.push("Bosnia and Herzegovina");
		this.aliasMap[region] = aliases;
		if(blob.properties.ISO_A3)
		    aliases.push(blob.properties.ISO_A3);
		if(blob.properties.STUSPS)
		    aliases.push(blob.properties.STUSPS);
		if(blob.properties.STATEFP)
		    aliases.push(blob.properties.STATEFP);		
		if(!blob.geometry) {
		    if(debug)
			console.log(region +" no geometry");
		    return;
		}
		if(debug)
		    console.log("region:" + region);
		let ok = true;
		aliases.forEach(alias=>{
		    if(this.skipRegions.includes(alias)) ok = false;});
		if(!ok) {
		    return;
		}
		ok = false;
		aliases.forEach(alias=>{
		    if(allRegions[alias]) {
			ok =true;
		    }});
		if(!ok) this.handleWarning("Missing data for map region:" + region);
		if(pruneMissing && !ok) {
		    if(debug)console.log("missing data:" + region);
//		    return;
		}
		this.regionNames.push(region);
		let coords = blob.geometry.coordinates;
		let info = {
		    name:region,
		    aliases: aliases,
		    polygons:[],
		    bounds:null
		};
		aliases.forEach(alias=>{
		    this.regions[alias] = info;
		});
		if(blob.geometry.type  == "MultiPolygon") {
		    coords.forEach(group=>{
			group.forEach(polygon=>{
			    polygon  = tfunc(region,polygon);
			    if(polygon)info.polygons.push(polygon);
			});
		    });
		} else {
		    coords.forEach(polygon=>{
			info.polygons.push(tfunc(region,polygon));
		    });
		}
		info.polygons.forEach(polygon=>{
		    polygon.forEach(point=>{
			let lon = point[0];
			let lat = point[1];
			if(isNaN(lon) || isNaN(lat)) return;
			this.mapRange.minLon= this.mapRange.minLon===null?lon:Math.min(this.mapRange.minLon,lon);
			this.mapRange.maxLon= this.mapRange.maxLon===null?lon:Math.max(this.mapRange.maxLon,lon);
			this.mapRange.minLat= this.mapRange.minLat===null?lat:Math.min(this.mapRange.minLat,lat);
			this.mapRange.maxLat= this.mapRange.maxLat===null?lat:Math.max(this.mapRange.maxLat,lat);						
		    });
		});
		let bounds = null;
		info.polygons.forEach(polygon=>{
		    bounds = Utils.mergeBounds(bounds, Utils.getBounds(polygon));
		});
		info.bounds = bounds;
	    });
	    return true;
	},
	
    });
}


function RamaddaMapchartDisplay(displayManager, id, properties) {
    const SUPER = new RamaddaOtherMapDisplay(displayManager, id, DISPLAY_MAPCHART, properties);
    let myProps = [
	{label:'Map chart Properties'},
	{p:'fixedFillColor',tt:'Use this color for all polygons',ex:'red'},
	{p:'fixedStrokeColor',tt:'Use this color for all polygons',ex:'red'},	
	{p:'missingLineColor'},
	{p:'missingFillColor'},		
	{p:'maxLayers',ex:'10'},
	{p:'translateX',ex:'0'},
	{p:'translateY',ex:'0'},	
	{p:'skewX',ex:'-10'},
	{p:'skewY',ex:'0'},	
	{p:'rotate',ex:'10'},
	{p:'scale',ex:'0'},
	{p:'fillColor',ex:'red'},
	{p:'blur',ex:'4'},			
    ];

    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
        makeMap: function() {
            let records = this.filterData();
            if (!records) {
                return;
            }
	    let valueMap = this.makeValueMap(records,true);
	    if(!valueMap) return;
	    let allRecords = this.getData().getRecords()
	    let pruneMissing = this.getPropertyPruneMissing(false);
	    let maxLayers = +this.getPropertyMaxLayers(20);
	    Object.keys(valueMap).forEach(region=>{
		let values = valueMap[region];
		values.layers = Math.round(values.percent*(maxLayers-1))+1;
	    });
	    this.colorBy = this.getColorByInfo(allRecords);

	    let [width,height] = this.writeMap();
	    let [svg, scaleX, scaleY] = this.makeSvg(width,height);
	    SU.transform(svg,SU.translate(width/2, height/2), SU.scale(0.9), SU.rotate(this.getPropertyRotate(0)), SU.translate(-width/2,-height/2), SU.translate(this.getPropertyTranslateX(30),this.getPropertyTranslateY(0)), SU.skewX(this.getPropertySkewX(-10)), SU.scale(this.getPropertyScale(1)));
	    let defs = svg.append("defs");
	    SU.makeBlur(svg,"blur", this.getPropertyBlur(3));
	    pruneMissing=false;
	    let fixedFillColor = this.getFixedFillColor();
	    let fixedStrokeColor = this.getFixedStrokeColor();	    	    
//	    console.dir(this.colorBy);
	    for(let layer=0;layer<maxLayers;layer++) {
		this.regionNames.forEach(region=>{
		    let values= this.findValues(region, valueMap);
		    let maxLayer = 1;
		    let value = NaN;
		    let missing = values==null;
		    let record = null;
		    if(!missing) {
			maxLayer = values.layers;
			value = values.value;
			record = values.record;
		    } else {
			if(pruneMissing) return;
			maxLayer = 1;
			if(layer>0) return;
		    }
		    let recordId = record?record.getId():"";
		    if(!Utils.isDefined(maxLayer)) maxLayer = 1;
		    if(layer>maxLayer) return;
		    this.regions[region].polygons.forEach(polygon=>{
			let uid = HtmlUtils.getUniqueId('selector_');
			let poly = this.makePoly(polygon);
			let fillColor = "transparent";
			if(missing) {
			    fillColor = this.getMissingFillColor("#ccc");
			    lineColor=this.getMissingLineColor("#000"); 
			} else {
			    if(layer==maxLayer-1) {
				fillColor = this.colorBy.getColorFromRecord(record);
				lineColor  = Utils.pSBC(0.1,fillColor);
			    } else {
				lineColor  = Utils.pSBC(-0.3,this.colorBy.getColor(value));
			    }
			    if(fixedFillColor)
				fillColor=fixedFillColor;
			    if(fixedStrokeColor)
				lineColor=fixedStrokeColor;			    
			}
			if(missing) {
			    svg.selectAll(uid)
				.data([poly])
				.enter().append("polygon")
				.attr("points",function(d) { 
				    return d.map(d=>{return [-layer+scaleX(d.x),-layer+scaleY(d.y)].join(",");}).join(" ");
				})
				.attr("regionName",region)
				.attr("fill",fillColor)
		    		.attr("stroke-width",1)
			    	.attr("stroke",lineColor);
			    return;
			}

			

			if(layer==0) {
			    svg.selectAll(uid)
				.data([poly])
				.enter().append("polygon")
				.attr("points",function(d) { 
				    return d.map(d=>{return [-layer+scaleX(d.x),-layer+scaleY(d.y)].join(",");}).join(" ");
				})
		    		.attr("stroke-width",3)
				.attr("stroke","black")
				.style("filter","url(#blur)");
			}
			let polys = 
			    svg.selectAll(uid)
			    .data([poly])
			    .enter().append("polygon")
			    .attr("points",function(d) { 
				return d.map(d=>{return [-layer+scaleX(d.x),-layer+scaleY(d.y)].join(",");}).join(" ");
			    })
			    .attr("fill",fillColor)
			    .attr("opacity",1)
			    .attr("stroke",lineColor)
			    .attr("stroke-width",1)
			    .style("cursor", "pointer")
			    .attr(RECORD_ID,recordId);
			this.addEvents(polys);
		    });
		});
	    }
	    if(!fixedFillColor)
		this.colorBy.displayColorTable();
	}
    });
}



function RamaddaMaparrayDisplay(displayManager, id, properties) {
    const ID_MAPBLOCK = "mapblock";
    const ID_MAPLABEL = "maplabel";        
    const SUPER = new RamaddaOtherMapDisplay(displayManager, id, DISPLAY_MAPARRAY, properties);
    let myProps = [
	{label:'Map array properties'},
	{p:'blockWidth',ex:''},
	{p:'sortByValue',ex:'true'},
	{p:'fillColor',ex:'red'},
	{p:'showValue',ex:'true'},	
    ];
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
        makeMap: function() {
            let records = this.filterData();
            if (!records) {
                return;
            }
	    let valueMap = this.makeValueMap(records,true);
	    if(!valueMap) return;
	    let allRecords = this.getData().getRecords()
	    this.colorBy = this.getColorByInfo(allRecords);
	    let [width,height] = this.writeMap(true);
	    let blockWidth= this.getPropertyBlockWidth(75);
	    let blockHeight= blockWidth;
	    let pruneMissing = this.getPropertyPruneMissing(true);
	    let sortedRegions = this.regionNames;
	    if(this.getSortByValue(true)) {
		sortedRegions.sort((a,b)=>{
		    if(!valueMap[a]) return 1;
		    if(!valueMap[b]) return -1;		    
		    return valueMap[a].value-valueMap[b].value;
		});
	    } else {
		sortedRegions.sort();
	    }

	    let html = "";
	    sortedRegions.forEach((region,idx)=>{
		html+= HU.div([CLASS,"display-maparray-block"],
			      HU.div([CLASS,"display-maparray-header"],region) +
			      HU.div([ID,this.domId(ID_MAPBLOCK+"_"+idx),CLASS,"display-maparray-map",STYLE,HU.css(WIDTH,blockWidth+"px",HEIGHT,blockHeight+"px")]) +
			      HU.div([ID,this.domId(ID_MAPLABEL+"_"+idx),"display-maparray-label"]));			      


		    
	    });
	    this.jq(ID_BASEMAP).html(html+"<p>");

	    let showValue = this.getPropertyShowValue(true);

	    sortedRegions.forEach((region,idx)=>{
		let info = this.regions[region];
		let svg = d3.select("#" + this.domId(ID_MAPBLOCK+"_"+idx)).append('svg')
		    .attr('width', blockWidth)
		    .attr('height', blockHeight)
		    .append('g')
		let padx=5;
		let pady=5;
		let mapWidth = info.bounds.getWidth();
		let mapHeight = info.bounds.getHeight();
		let scaleX;
		let scaleY;
		if(mapWidth>mapHeight) {
		    scaleX= d3.scaleLinear().domain([info.bounds.minx, info.bounds.maxx]).range([0, blockWidth-padx]);
		    scaleY= d3.scaleLinear().domain([info.bounds.maxy, info.bounds.miny]).range([0, (mapHeight/mapWidth)*blockHeight-pady]);
		} else {
		    scaleX= d3.scaleLinear().domain([info.bounds.minx, info.bounds.maxx]).range([0, (mapWidth/mapHeight)*blockWidth-padx]);
		    scaleY= d3.scaleLinear().domain([info.bounds.maxy, info.bounds.miny]).range([0, blockHeight-pady]);
		}
		let values = valueMap[region];
		let value = NaN;
		let missing = values==null;
		let record = null;
		if(!missing) {
		    value = values.value;
		    record = values.record;
		    if(showValue) {
			this.jq(ID_MAPLABEL+"_"+idx).html(value);
		    }
		} else {
		    if(pruneMissing) return;
		}

		let recordId = record?record.getId():"";
		info.polygons.forEach(polygon=>{
		    let uid = HtmlUtils.getUniqueId();
		    let poly = this.makePoly(polygon);
		    let fillColor = "transparent";
		    if(missing) {
			fillColor = "#ccc";
			lineColor="#000" 
		    } else {
			fillColor = this.colorBy.getColor(value);
			lineColor = "#ccc";
		    }
		    if(missing) {
			svg.selectAll(uid)
			    .data([poly])
			    .enter().append("polygon")
			    .attr("points",function(d) { 
				return d.map(d=>{return [-layer+scaleX(d.x),-layer+scaleY(d.y)].join(",");}).join(" ");
			    })
			    .attr("fill","#ccc")
		    	    .attr("stroke-width",1)
			    .attr("stroke","black");
			return;
		    }
		    let polys = 
			svg.selectAll(uid)
			.data([poly])
			.enter().append("polygon")
			.attr("points",function(d) { 
			    return d.map(d=>{return [+scaleX(d.x),+scaleY(d.y)].join(",");}).join(" ");
			})
			.attr("fill",fillColor)
			.attr("opacity",1)
			.attr("stroke",lineColor)
			.attr("stroke-width",1)
			.style("cursor", "pointer")
			.attr(RECORD_ID,recordId);
		    this.addEvents(polys);
		});
	    });
	    this.colorBy.displayColorTable();
	}
    });
}




function RamaddaMapshrinkDisplay(displayManager, id, properties) {
    const SUPER = new RamaddaOtherMapDisplay(displayManager, id, DISPLAY_MAPSHRINK, properties);
    let myProps = [
	{label:'Map shrink Properties'},
   ];

    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
        makeMap: function() {
            let records = this.filterData();
            if (!records) {
                return;
            }
	    let allRecords = this.getData().getRecords()
	    let pruneMissing = this.getPropertyPruneMissing(false);
	    let valueMap = this.makeValueMap(records,true);
	    if(!valueMap) return;
	    let sizeBy = new SizeBy(this, allRecords);
	    this.colorBy = this.getColorByInfo(allRecords);
	    let [width,height] = this.writeMap();
	    let [svg, scaleX, scaleY] = this.makeSvg(width,height);

	    for(let layer=0;layer<2;layer++) {
		this.regionNames.forEach(region=>{
		    let values= this.findValues(region, valueMap);
		    let value = NaN;
		    let missing = values==null;
		    let record = null;
		    if(!missing) {
			value = values.value;
			record = values.record;
		    } else {
			if(pruneMissing) return;
			if(layer>0) return;
		    }
		    let recordId = record?record.getId():"";
		    this.regions[region].polygons.forEach(polygon=>{
			let uid = HtmlUtils.getUniqueId();
			let poly = this.makePoly(polygon);
			let fillColor = "red";
			let transform  = "";
			lineColor="#000" 
			if(layer==0) {
			    fillColor = "#fff";
			} else {
			    lineColor="transparent" 
			    fillColor = this.colorBy.getColor(value);
			    let bounds = Utils.getBounds(polygon);
			    let center = bounds.getCenter();
			    let p=0;
			    let sizeByFunc = function(p, size) {
				percent = p;
				return percent;
			    }
			    sizeBy.getSizeFromValue(value,sizeByFunc);
			    transform = SU.translate(scaleX(center.x),scaleY(center.y)) + SU.scale(percent) + SU.translate(-scaleX(center.x),-scaleY(center.y))
			}
			if(missing) {
			    svg.selectAll("base"+uid)
				.data([poly])
				.enter().append("polygon")
				.attr("points",function(d) { 
				    return  d.map(d=>{return [-layer+scaleX(d.x),-layer+scaleY(d.y)].join(",");}).join(" ");
				})
				.attr("fill","#ccc")
		    		.attr("stroke-width",1)
			    	.attr("stroke","black");
			    return;
			}
			if(layer==0) {
			    svg.selectAll("base"+uid)
				.data([poly])
				.enter().append("polygon")
				.attr("points",function(d) { 
				    return  d.map(d=>{return [-layer+scaleX(d.x),-layer+scaleY(d.y)].join(","); }).join(" ");
				})
		    		.attr("stroke-width",1)
				.attr("stroke","black")
				.attr('transform',transform);
			}
			let polys = 
			    svg.selectAll(uid)
			    .data([poly])
			    .enter().append("polygon")
			    .attr("points",function(d) { 
				return  d.map(d=>{return [-layer+scaleX(d.x),-layer+scaleY(d.y)].join(",");}).join(" ");
			    })
			    .attr("fill",fillColor)
			    .attr("opacity",1)
			    .attr("stroke",lineColor)
			    .attr("stroke-width",1)
			    .attr('transform',transform)
			    .style("cursor", "pointer")
			    .attr(RECORD_ID,recordId);
			if(layer==1)
			    this.addEvents(polys);
		    });
		});
	    }
	    this.colorBy.displayColorTable();
	}
    });
}


function RamaddaMapimagesDisplay(displayManager, id, properties) {
    const SUPER = new RamaddaOtherMapDisplay(displayManager, id, DISPLAY_MAPIMAGES, properties);
    let myProps = [
	{label:'Map Images Properties'},
	{p:'imageField',ex:''},
    ];
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
        getHeightForStyle: function(dflt) {
	    return null;
	},
	addMacroAttributes:function(macros,row,attrs) {
	    SUPER.addMacroAttributes.call(this,macros,row,attrs);
	    if(!this.imageField) return;
	    let f = this.imageField;
	    let value = row[f.getIndex()];
	    let imageAttrs = [];
	    let tokenAttrs  = macros.getAttributes("imageField_image");
	    let width = tokenAttrs?tokenAttrs["width"]:null;
	    if(width) {
		imageAttrs.push("width");
		imageAttrs.push(width);
	    } else if(this.getProperty("imageWidth")) {
		imageAttrs.push("width");
		imageAttrs.push(this.getProperty("imageWidth")); 
	    } else  {
		imageAttrs.push("width");
		imageAttrs.push("100%");
	    }
	    imageAttrs.push(ATTR_STYLE);
	    imageAttrs.push("vertical-align:top");
	    let img =  HU.image(value, imageAttrs);
	    attrs["imageField" +"_image"] =  img;
	    attrs["imageField" +"_url"] =  value;
	},
        makeMap: function() {
            let records = this.filterData();
            if (!records) {
                return;
            }
	    this.imageField = this.getFieldById(null,this.getPropertyImageField());	    
	    if(this.imageField == null) {
		this.imageField =  this.getFieldByType(null, "image");
	    }
	    let strokeWidth = this.getStrokeWidth(1);
	    let strokeColor = this.getStrokeColor("#000");
	    if(this.imageField==null) {
                this.displayError("No image fields");
		return
	    }
	    let valueMap = this.makeValueMap(records);
	    if(!valueMap) return;
	    Object.keys(valueMap).forEach(region=>{
		let values = valueMap[region];
		values.image = values.record.getValue(this.imageField.getIndex());
	    });
	    let [width, height] = this.writeMap();
	    let [svg, scaleX, scaleY] = this.makeSvg(width,height);
	    let defs = svg.append("defs");
	    this.regionNames.forEach((region,idx)=>{
		let values= this.findValues(region, valueMap);
		let recordId = values!=null?values.record.getId():"";
		let regionClean = Utils.cleanId(region);
		this.regions[region].polygons.forEach(polygon=>{
		    let uid = HtmlUtils.getUniqueId();
		    if(values!=null) {
			defs.append("svg:pattern")
			    .attr(ATTR_ID, "bgimage"+ uid)
			    .attr("x", "1")
			    .attr("y", "1")
			    .attr("width", "100%")
		            .attr("height", "100%")
			    .attr("patternContentUnits","objectBoundingBox")
			    .append("svg:image")
			    .attr("xlink:href", values.image)
			    .attr("preserveAspectRatio","none")
			    .attr("width", 1)
			    .attr("height", 1)
			    .attr("x", "0")
			    .attr("y", "0");
		    }
		    let polys = svg.selectAll(regionClean+"base"+uid)
			.data([this.makePoly(polygon)])
			.enter().append("polygon")
			.attr("regionName",region)
			.attr("points",function(d) { 
			    return d.map(d=>{return [scaleX(d.x),scaleY(d.y)].join(",");}).join(" ");
			})
			.attr(RECORD_ID,recordId)
		    	.attr("stroke-width",strokeWidth)
			.attr("stroke",strokeColor);
		    if(values!=null)
			polys.style("fill", "url(#bgimage"+ uid+")")
		    else
			polys.style("fill",this.getPropertyMissingFill("#fff"));
		    this.addEvents(polys);
		});
	    });
	}
    });
}


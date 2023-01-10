/**
   Copyright 2008-2023 Geode Systems LLC
*/

var DISPLAY_IMDV = 'imdv';
var DISPLAY_EDITABLEMAP = 'editablemap';
addGlobalDisplayType({
    type: DISPLAY_IMDV,
    label: 'Integrated Map Data',
    category:CATEGORY_MAPS,
    tooltip: makeDisplayTooltip('Integrated Map Data',[],'Create interactive maps with points, routes, data, etc'),        
});

var MAP_RESOURCES;
var MAP_RESOURCES_MAP; 
var GLYPH_FIXED = 'fixed';
var GLYPH_GROUP = 'group';
var GLYPH_MARKER = 'marker';
var GLYPH_POINT = 'point';
var GLYPH_LABEL = 'label';
var GLYPH_BOX = 'box';
var GLYPH_CIRCLE = 'circle';
var GLYPH_TRIANGLE = 'triangle';
var GLYPH_HEXAGON = 'hexagon';
var GLYPH_LINE = 'line';
var GLYPH_ROUTE = 'route';
var GLYPH_POLYLINE = 'polyline';
var GLYPH_FREEHAND = 'freehand';
var GLYPH_POLYGON = 'polygon';
var GLYPH_FREEHAND_CLOSED = 'freehand_closed';
var GLYPH_IMAGE = 'image';
var GLYPH_ENTRY = 'entry';
var GLYPH_MULTIENTRY = 'multientry';
var GLYPH_MAP = 'map';
var GLYPH_MAPSERVER = 'mapserver'

var GLYPH_DATA = 'data';
var GLYPH_TYPES_SHAPES = [GLYPH_POINT,GLYPH_BOX,GLYPH_CIRCLE,GLYPH_TRIANGLE,GLYPH_HEXAGON,GLYPH_LINE,GLYPH_POLYLINE,GLYPH_FREEHAND,GLYPH_POLYGON,GLYPH_FREEHAND_CLOSED];
var GLYPH_TYPES_LINES_OPEN = [GLYPH_LINE,GLYPH_POLYLINE,GLYPH_FREEHAND,GLYPH_ROUTE];
var GLYPH_TYPES_LINES = [GLYPH_LINE,GLYPH_POLYLINE,GLYPH_FREEHAND,GLYPH_POLYGON,GLYPH_FREEHAND_CLOSED,GLYPH_ROUTE];
var GLYPH_TYPES_CLOSED = [GLYPH_POLYGON,GLYPH_FREEHAND_CLOSED,GLYPH_BOX,GLYPH_TRIANGLE,GLYPH_HEXAGON];
var MAP_TYPES = ['geo_geojson','geo_gpx','geo_shapefile','geo_kml'];
var LEGEND_IMAGE_ATTRS = ['style','color:#ccc;font-size:9pt;'];
var BUTTON_IMAGE_ATTRS = ['style','color:#ccc;'];

var CLASS_IMDV_STYLEGROUP= 'imdv-stylegroup';
var CLASS_IMDV_STYLEGROUP_SELECTED = 'imdv-stylegroup-selected';

let ImdvUtils = {
    findGlyph:function(list, id) {
	if(!list) return null;
	let glyph;
	for(let i=0;i<list.length;i++) {
	    let mapGlyph = list[i].findGlyph(id);
	    if(mapGlyph) return mapGlyph;
	}
	return null;
    },
    //We use this to call redraw so we don't have to keep track of when to redraw when
    //we're updating the map
    scheduleRedraw:function(layer,feature) {
	if(layer.redrawPending) {
	    return;
	}
	layer.redrawPending = true;
	setTimeout(()=>{
	    layer.redraw();
	    layer.redrawPending = false;
	},1)
    }
}





function RamaddaImdvDisplay(displayManager, id, properties) {
    this.mapProperties = {};
    Utils.importJS(ramaddaBaseHtdocs+'/wiki.js');
    if(!MAP_RESOURCES) {
        $.getJSON(ramaddaBaseUrl+'/mapresources.json', data=>{
	    MAP_RESOURCES_MAP={};
	    MAP_RESOURCES = data;
	    MAP_RESOURCES.forEach((r,idx)=>{MAP_RESOURCES_MAP[idx] = r;});
	}).fail(err=>{
	    console.error('Failed loading mapresources.json:' + err);
	});
    }
    ImageHandler = OpenLayers.Class(OpenLayers.Handler.RegularPolygon, {
	CLASS_NAME:'IMDV',
	initialize: function(control, callbacks, options) {
	    OpenLayers.Handler.RegularPolygon.prototype.initialize.apply(this,arguments);
	    this.display = options.display;
	},
	finalize: function(cancel) {
	    if(cancel || !this.image) return
	    let image = this.image;
	    this.theImage = image;
	    this.image =null;
	    this.display.featureChanged();	    
	    OpenLayers.Handler.RegularPolygon.prototype.finalize.apply(this,arguments);
	    //call deactivate in a bit. If we do this now then there is an error in OL
	    setTimeout(()=>{
		let box = this.display.getNewFeature();
                box.style.strokeColor = 'transparent';
		let mapGlyph =this.display.handleNewFeature(box,this.style);
		mapGlyph.setImage(image);
		box.mapGlyph = mapGlyph;
                this.display.myLayer.redraw(box);
		this.display.clearCommands();
	    },250);
	},
	move: function(evt) {
	    if(!this.checkingImageSize) {
		this.checkingImageSize = true;
		const img = new Image();
		img.onload = ()=> {
		    this.imageBounds={width:img.width,height:img.height};
		}
		img.src = this.style.imageUrl;		
	    }
	    OpenLayers.Handler.RegularPolygon.prototype.move.apply(this,arguments);
	    let mapBounds = this.feature.geometry.getBounds();
	    let b = this.display.map.transformProjBounds(mapBounds);
	    if(this.imageBounds && !evt.shiftKey) {
		let aspect1 = this.imageBounds.width/this.imageBounds.height;
		let aspect2 = this.imageBounds.height/this.imageBounds.width;
		let rh = b.top-b.bottom;
		let rw = b.right-b.left;
		//		b.right = aspect1*(rh) + b.left
		b.bottom = b.top-aspect2*rw;
	    }
	    this.lastBounds = b;
	    if(isNaN(b.bottom)) b.bottom = b.top;
	    if(isNaN(b.top)) b.top= b.bottom;	    
	    if(!this.image) {
		this.image=  this.display.getMap().addImageLayer('IMDV Image','IMDV Image','',this.style.imageUrl,true,  b.top,b.left,b.bottom,b.right);
	    } else {
		b = this.display.map.transformLLBounds(b);
		this.image.extent = b;
		this.image.moveTo(b,true,true);
	    }
	    this.image.setOpacity(this.style.imageOpacity);
	}
	
    });

    MyPoint = OpenLayers.Class(OpenLayers.Handler.Point, {
	finalize: function(cancel) {
	    OpenLayers.Handler.Point.prototype.finalize.apply(this,arguments);
	    if(!cancel)this.display.handleNewFeature(this.display.getNewFeature());
	},
    });



    MyEntryPoint = OpenLayers.Class(OpenLayers.Handler.Point, {
	finalize: function(cancel) {
	    OpenLayers.Handler.Point.prototype.finalize.apply(this,arguments);
	    if(!cancel)this.display.handleNewFeature(this.display.getNewFeature());
	    this.display.clearCommands();	    	    
	},
    });
    

    MyPolygon = OpenLayers.Class(OpenLayers.Handler.Polygon, {
	finalize: function(cancel) {
	    OpenLayers.Handler.Path.prototype.finalize.apply(this,arguments);
	    if(cancel) return;
	    let line =  this.display.getNewFeature();
	    if(!line || !line.geometry) {
		return;
	    }
	    this.display.handleNewFeature(line);
	},
	move: function(evt) {
	    OpenLayers.Handler.Path.prototype.move.apply(this,arguments);
	    this.display.showDistances(this.line.geometry,this.glyphType);
	}
    });

    MyPath = OpenLayers.Class(OpenLayers.Handler.Path, {
	finalize: function(cancel) {
	    OpenLayers.Handler.Path.prototype.finalize.apply(this,arguments);
	    if(cancel) return;
	    let line =  this.display.getNewFeature();
	    if(!line || !line.geometry) {
		return;
	    }
	    this.display.handleNewFeature(line);
	},
	move: function(evt) {
	    OpenLayers.Handler.Path.prototype.move.apply(this,arguments);
	    this.display.showDistances(this.line.geometry,this.glyphType);
	}
    });

    MyRoute = OpenLayers.Class(OpenLayers.Handler.Path, {
	finalize: function(cancel) {
	    if(this.makingRoute) return;
	    OpenLayers.Handler.Path.prototype.finalize.apply(this,arguments);
	    if(cancel) return;
	    if(this.finishedWithRoute) return;
	    //A hack to get the line that was just drawn
	    let line =  this.display.getNewFeature();
	    if(!line || !line.geometry) {
		return;
	    }
	    let pts = this.display.getLatLonPoints(line.geometry);
	    if(pts==null) return;
	    let xys = [];
	    pts.forEach(pt=>{
		xys.push(Utils.trimDecimals(pt.y,6));
		xys.push(Utils.trimDecimals(pt.x,6));
	    });
	    let args = {
		mode:this.display.routeType??'car',
		points:Utils.join(xys,',')
	    };
	    if(this.display.routeProvider)
		args.provider = this.display.routeProvider;

	    let reset=  ()=>{
		this.makingRoute = false;
		this.finishedWithRoute = false;
		this.display.clearMessage2();
		this.display.getMap().clearAllProgress();
		this.display.setCommandCursor();
	    };

	    let url = ramaddaBaseUrl+'/map/getroute?entryid='+this.display.getProperty('entryId');
	    this.finishedWithRoute = true;
	    this.display.showProgress('Creating route...');
	    this.makingRoute = true;
	    $.post(url, args,data=>{
		reset();
		this.display.myLayer.removeFeatures([line]);
		if(data.error) {
		    this.display.handleError(data.error);
		    return;
		}
		if(!data.routes || data.routes.length==0) {
		    alert('No routes found');
		    this.display.clearMessage2();
		    return;
		}
		let points = [];
		let routeData = data.routes[0];
		if(routeData.overview_polyline) {
		    let d = googleDecode(routeData.overview_polyline.points);
		    d.forEach(pair=>{
			points.push(MapUtils.createPoint(pair[1],pair[0]));
		    });
		} else {
		    routeData.sections.forEach(section=>{
			let decoded = hereDecode(section.polyline);
			decoded.polyline.forEach(pair=>{
			    points.push(MapUtils.createPoint(pair[1],pair[0]));
			});
		    });
		}
		let  route = this.display.getMap().createPolygon('', '', points, {
		    strokeWidth:4
		},null,true);
		route.style = $.extend({},this.style);
		route.type=GLYPH_ROUTE;
		this.display.addFeatures([route]);
		this.display.handleNewFeature(route,null,{type:GLYPH_ROUTE,routeProvider:this.display.routeProvider,routeType:this.display.routeType});
		this.display.showDistances(route.geometry,GLYPH_ROUTE,true);
	    }).fail(err=>{
		reset();
		this.display.myLayer.removeFeatures([line]);
		this.display.clearCommands();
		this.display.handleError(err);
	    });
	},
	move: function(evt) {
	    if(this.makingRoute) return;
	    OpenLayers.Handler.Path.prototype.move.apply(this,arguments);
	}
	
    });


    MyRegularPolygon = OpenLayers.Class(OpenLayers.Handler.RegularPolygon, {
	dragend: function() {
	    OpenLayers.Handler.RegularPolygon.prototype.dragend.apply(this,arguments);
	    this.display.handleNewFeature(this.display.getNewFeature(),this.style);
	},
	move: function(evt) {
	    OpenLayers.Handler.RegularPolygon.prototype.move.apply(this,arguments);
	    if(!this.feature || !this.feature.geometry) return;
	    this.display.showDistances(this.feature.geometry,this.glyphType);
	}
    });    
    





    const LABEL_NONE = '<none>';
    const LIST_ROW_CLASS  = 'imdv-feature-row';
    const LIST_SELECTED_CLASS  = 'imdv-feature-selected';
    const ID_MAP_MENUBAR = 'mapmenubar';
    const ID_TOPWIKI = 'topwiki';
    const ID_BOTTOMWIKI = 'bottomwiki';    
    const ID_EDIT_NAME  ='editname';
    const ID_MESSAGE  ='message';
    const ID_MESSAGE2  ='message2';    
    const ID_MESSAGE3  ='message3';
    const ID_ADDRESS  ='address';
    const ID_ADDRESS_INPUT  ='address_input';
    const ID_ADDRESS_WAIT  ='address_wait';
    const ID_ADDRESS_CLEAR  ='address_clear';
    const ID_ADDRESS_ADD  ='address_add';                    
    const ID_LIST_DELETE  ='listdelete';
    const ID_LIST_OK  ='listok';
    const ID_LIST_CANCEL = 'listcancel';
    const ID_DELETE  ='delete';
    const ID_SELECT  ='select';
    const ID_OK  ='ok';
    const ID_APPLY  ='apply';
    const ID_CANCEL = 'cancel';
    const ID_MENU_NEW = 'new_file';
    const ID_MENU_FILE = 'menu_file';
    const ID_MENU_EDIT = 'menu_edit';    
    const ID_TOBACK = 'toback';
    const ID_TOFRONT = 'tofront';    

    const ID_CUT = 'cut';
    const ID_COPY= 'copy';
    const ID_PASTE= 'paste';        
    const ID_COMMANDS = 'commands';
    const ID_CLEAR = 'clear';
    const ID_CMD_LIST = 'cmdlist';
    const ID_LIST = 'list';        
    const ID_PROPERTIES = 'properties';
    const ID_NAVIGATE = 'navigate';

    const ID_SAVE = 'save';
    const ID_SAVEAS = 'saveas';    
    const ID_REFRESH = 'refresh';
    const ID_IMPORT = 'import';
    const ID_DOWNLOAD = 'download';    
    const ID_SELECTOR = 'selector';
    const ID_SELECT_ALL = 'selectall';    
    const ID_EDIT = 'edit';
    const ID_MOVER = 'mover';
    const ID_RESIZE = 'resize';
    const ID_RESHAPE = 'reshape';    
    const ID_ROTATE  = 'rotate';
    const ID_LEGEND = 'legend';
    const ID_LEGEND_LEFT = 'legend_left';
    const ID_LEGEND_RIGHT = 'legend_right';    
    const ID_LEGEND_MAP_WRAPPER = 'legend_map_wrapper';
    const ID_LEGEND_MAP = 'legend_map';            
    const ID_MAP_PROPERTIES = 'mapproperties';




    if(!Utils.isDefined(properties.showOpacitySlider)&&!Utils.isDefined(getGlobalDisplayProperty('showOpacitySlider'))) 
	properties.showOpacitySlider=true; 
    const SUPER = new RamaddaBaseMapDisplay(displayManager,  id, DISPLAY_IMDV,  properties);
    RamaddaUtil.inherit(this,SUPER);
    addRamaddaDisplay(this);
    this.defineSizeByProperties();
    //do this here as this might be used by displaymap for annotation
    this.map = this.getProperty('theMap');
    const myProps = [
	{label:'Editable Map Properties'},
	{p:'displayOnly',d:false},
	{p:'strokeColor',d:'blue'},
	{p:'strokeWidth',d:2},
	{p:'pointRadius',d:10},
	{p:'externalGraphic',d:'/map/marker-blue.png'},
	{p:'fontSize',d:'12px'},
	{p:'fontWeight',d:'normal'},
	{p:'fontStyle',d:'normal'},	
	{p:'fontFamily',d:"'Open Sans', Helvetica Neue, Arial, Helvetica, sans-serif"},
	{p:'imageOpacity',d:1},
	{p:'userCanEdit',tt:'Set to false to not show menubar, etc for all users'},
	{p:'showLegendShapes',d:true},	
	{p:'showMapLegend',d:false},

    ];
    
    displayDefineMembers(this, myProps, {
	commands: [],
        myLayer: [],
	glyphs:[],
	markers:{},
	levels: [['','None'],[4,'4 - Most zoomed out'],5,6,7,8,
		 9,10,11,12,13,14,15,16,17,18,19,[20,'20 - Most zoomed in']],
	DOT_STYLE:{
	    zIndex:1000,
	    fillColor:'#000',
	    fillOpacity:1,
	    strokeWidth:0,
	    pointRadius:4
	},

	getGlyphs: function() {
	    return this.glyphs;
	},
	selected:{},
	getMap: function() {
	    return this.map;
	},

	getUsedMarkers:function() {
	    return Object.keys(this.markers);
	},


	makeLabel:function(l) {
	    let _l = l.toLowerCase();
	    let v = this.getMapProperty(_l+'.label');
	    if(v) return v;
	    return MapUtils.makeLabel(l);
	},

	setLastDroppedTime:function(date) {
	    this.lastDroppedTime = date;
	},
	showFeatureProperty:function(p) {
	    let _p = p.toLowerCase();
	    return this.getMapProperty(_p+'.showFeature',true);
	},

	findGlyph:function(id) {
	    return ImdvUtils.findGlyph(this.glyphs,id);
	},
	
	addGlyph:function(glyph,dontNotify) {
	    if(Array.isArray(glyph))
		this.glyphs.push(...glyph);
	    else
		this.glyphs.push(glyph);
	    if(!dontNotify) this.handleGlyphsChanged();
	},

	handleEvent:function(event,lonlat) {
	    return;
	},
	handleNewFeature:function(feature,style,mapOptions) {
	    style = style || feature?.style;
	    mapOptions = mapOptions??feature?.mapOptions ?? style?.mapOptions;
	    if(feature && feature.style && feature.style.mapOptions)
		delete feature.style.mapOptions;
	    let mapGlyph = new MapGlyph(this,mapOptions.type, mapOptions, feature,style);
	    this.addGlyph(mapGlyph);
	    mapGlyph.applyEntryGlyphs();
	    this.clearMessage2(1000);
	    return mapGlyph;
	},
	handleGlyphsChanged: function (){
	    this.makeLegend();
	    this.addFeatureList();
	    this.featureChanged();	    
	},
	getLayer: function() {
	    return this.myLayer;
	},
	redraw: function(feature) {
	    ImdvUtils.scheduleRedraw(this.myLayer,feature);
	},
	getNewFeature: function() {
	    return this.myLayer.features[this.myLayer.features.length-1];
	},
	clearAddresses:function() {
	    if(this.addresses) {
		this.addresses.forEach(mapGlyph=>{
		    this.removeFeatures(mapGlyph.getFeatures());
		});
		this.addresses = null;
	    }
	},
	    
	gotoAddress:function(widget,address) {
            let url = ramaddaBaseUrl + '/geocode?query=' + encodeURIComponent(address);
	    let add = loc=> {
		if(this.addresses == null)this.addresses=[];
		let pt = MapUtils.createLonLat(loc.longitude, loc.latitude);
		let label = '';
		let toks = Utils.split(loc.name,',',true,true);
		let offset = -10;
		for(let i=0;i<toks.length;i++) {
		    label +=toks[i]+'\n';
		    offset-=6;
		}
		offset = -12;
		label = label.trim();
		let style = {
		    label:label,
		    fontSize: '9pt',
		    labelYOffset:offset,
		    labelAlign: 'ct',
		    pointRadius:12		    
		};

		let addIt = this.jq(ID_ADDRESS_ADD).is(':checked');
		let points = [loc.latitude,loc.longitude];
		style.externalGraphic=ramaddaBaseUrl +(addIt?'/icons/map/marker-blue.png':'/icons/map/marker.png');
		let mapGlyph = this.createMapMarker(GLYPH_MARKER,{type:GLYPH_MARKER,name:loc.name}, style,points,addIt)
		if(!addIt)
		    this.addresses.push(mapGlyph);
		mapGlyph.panMapTo();
	    };
	    
	    let clear = ()=>{
		widget.css('background','#fff');
		this.jq(ID_ADDRESS_WAIT).html('');
	    }

	    widget.css('background','#eee');
	    this.jq(ID_ADDRESS_WAIT).html(HU.getIconImage(icon_wait));
            let jqxhr = $.getJSON(url, (data)=> {
		clear();
                if (data.result.length == 0) {
                    wait.html('Nothing found');
                    return;
                } else if(data.result.length==1) {
		    add(data.result[0]);
		} else {
		    let html = '';
		    data.result.forEach((loc,idx)=>{
			html+=HU.div(['index',idx,'class','ramadda-clickable ramadda-menu-item'],loc.name);
		    });
		    html = HU.div(['style','max-height:200px;overflow-y:auto;'], html);
		    let dialog = HU.makeDialog({content:html,header:false,anchor:widget,my:'left top',at:'left bottom'});
		    let _this = this;
		    dialog.find('.ramadda-menu-item').click(function() {
			let loc = data.result[$(this).attr('index')];
			dialog.remove();
			add(loc);
		    });
		}
	    }).fail((data)=>{
		clear();
		window.alert('Failed to find address');
	    });
	},

	showDistances:function(geometry, glyphType,fadeOut) {
	    let msg = this.getDistances(geometry,glyphType);
	    if(Utils.stringDefined(msg)) {
		this.showMessage2(msg,fadeOut);
	    }
	},

	getLatLonPoints:function(geometry) {
	    let components = geometry.components;
	    if(components==null) return null;
	    if(components.length) {
		if(components[0].components) components = components[0].components;
	    }
	    let pts = components.map(pt=>{
		return  this.getMap().transformProjPoint(pt)
	    });
	    return pts;
	},

	getDistances:function(geometry,glyphType,justDistance) {
	    if(!geometry) return null;
	    let pts = this.getLatLonPoints(geometry);
	    if(pts==null) return null;
	    //            let garea = MapUtils.squareMetersToSquareFeet(geometry.getGeodesicArea(this.map.getMap().getProjectionObject()));
            let garea = MapUtils.squareMetersToSquareFeet(geometry.getArea());
	    let area = -1;
	    let acres;

	    if(!glyphType) return '';
	    let distancePrefix = 'Total distance: ';

	    if(glyphType == GLYPH_CIRCLE || glyphType == GLYPH_BOX || glyphType == GLYPH_POLYGON || glyphType == GLYPH_TRIANGLE || glyphType == GLYPH_FREEHAND_CLOSED || glyphType==GLYPH_IMAGE) {
		area = MapUtils.calculateArea(pts);
		acres = area/43560;
	    }

	    if(glyphType == GLYPH_CIRCLE || glyphType == GLYPH_BOX ||  glyphType == GLYPH_TRIANGLE  || glyphType==GLYPH_IMAGE) {
		distancePrefix = 'Perimeter: ';
	    }

	    let msg = 'Distance: ';
	    if(glyphType == GLYPH_BOX || glyphType == GLYPH_IMAGE) {
		msg = '';
		let w = MapUtils.distance(pts[0].y,pts[0].x,pts[1].y,pts[1].x);
		let h = MapUtils.distance(pts[1].y,pts[1].x,pts[2].y,pts[2].x);		
		let unit = 'ft';
		if(w>5280 || h>5280) {
		    unit = 'm';
		    w = w/5280;
		    h = h/5280;		    
		}

		msg= 'W: ' + Utils.formatNumberComma(w) + ' ' + unit +
		    ' H: ' + Utils.formatNumberComma(h) + ' ' + unit;
	    } else {
		let segments = '';
		let total = 0;
		for(let i=0;i<pts.length-1;i++) {
		    let pt1 = pts[i];
		    let pt2 = pts[i+1];		
		    let d = MapUtils.distance(pt1.y,pt1.x,pt2.y,pt2.x);
		    total+=d;
		    let unit = 'feet';
		    if(d>5280) {
			unit = 'miles';
			d = d/5280;
		    }
		    d = Utils.formatNumberComma(d);
		    segments+= d +' ' + unit +' ';
		}
		let unit = 'feet';
		if(total>5280) {
		    unit = 'miles';
		    total = total/5280;
		}
		msg = distancePrefix + Utils.formatNumberComma(total) +' ' + unit;
		if(pts.length>2 && pts.length<6)  {
		    msg+='<br>Segments:' + segments;
		}
		if(pts.length<=1) msg='';
	    }
	    if(!justDistance&&area>0) {
		unit='ft';
		if(area>MapUtils.squareFeetInASquareMile) {
		    unit = 'miles';
		    area = area/MapUtils.squareFeetInASquareMile;
		    msg+=   '<br>' +
			'Area: ' + Utils.formatNumber(area) +' sq ' + unit;
		} else {
		    msg+=   '<br>' +
			'Area: ' + Utils.formatNumber(acres) +' acres';
		}
	    }

	    return msg;
	},


	setCommandCursor: function(cursor) {
	    this.getMap().setCursor(cursor??'pointer');
	},


	wrapDialog:function(html) {
	    return HU.div(['style','margin:5px;'],html);
	},

	setCommand:function(command, args) {
	    this.clearCommands();
	    if(command!=ID_SELECTOR && command!=ID_MOVER) {
		this.unselectAll();
	    }
	    this.command = command;
	    let glyphType = this.getGlyphType(command);
	    this.commands.forEach(cmd=>{
		cmd.deactivate();
	    });
	    if(!command) return;
	    this.jq('new_' + command).addClass('imdv-command-active');
	    this.commands.every(cmd=>{
		if(cmd.name != command) {
		    return true;
		}
		if(!glyphType) {
		    this.showCommandMessage(cmd.message);
		    cmd.activate();
		    return false;
		}
		this.initGlyphCommand(glyphType, cmd,args);
		return false;
	    });
	},
	initGlyphCommand:function(glyphType, cmd,args) {
	    args = args ??{};
	    this.setCommandCursor();
	    let styleMap = MapUtils.createStyleMap({'default':{}});
	    let tmpStyle = {};
	    $.extend(tmpStyle,glyphType.getStyle());
	    tmpStyle.mapOptions = {
		type:glyphType.type
	    }
	    if(glyphType.isFixed() || glyphType.isGroup()) {
		let text = prompt(glyphType.isFixed()?'Text:':'Name:');
		if(!text) return
		let mapOptions = tmpStyle.mapOptions;
		delete tmpStyle.mapOptions;
		tmpStyle.text = text;
		if(glyphType.isGroup()) {
		    tmpStyle.externalGraphic = glyphType.getIcon();
		    mapOptions.name = text;
		}
		this.clearCommands();
		let mapGlyph = new MapGlyph(this,mapOptions.type, mapOptions, null,tmpStyle);
		this.addGlyph(mapGlyph);
		this.clearMessage2(1000);
		this.clearCommands();
		if(glyphType.isFixed()) {
		    mapGlyph.addFixed();
		}
		return;
	    }

	    if(glyphType.isMapServer()) {
		let html = '';
		let form = (label,name,size)=>{
		    if(name=='predefined') {
			let ids =Utils.mergeLists([''],RAMADDA_MAP_LAYERS.map(l=>{return [l.id,l.name]}));
			html+=HU.formEntry(label+':',HU.select('',['id',this.domId(name)],ids,this.cache[name]));
		    } else {
			html+=HU.formEntry(label+':',HU.input('',this.cache[name]??'',['id',this.domId(name),'size',size??'60']));
		    }
		}
		let args = [
		    ['Name','servername'],
		    ['Server URL','serverurl'],
		    ['WMS Layer','wmslayer','20'],
		    ['Legend URL','maplegend'],
		    ['Or Predefined','predefined']
		];
		html+= 'Enter either a TMS server URL or a WMS server URL with a layer name<br>';
		this.cache = this.cache??{};
		html +=  HU.formTable();
		args.forEach(a=>{
		    form(a[0],a[1],a[2]);
		});
		html+='</table>';
		html+=HU.div(['style',HU.css('text-align','center','padding-bottom','8px','margin-bottom','8px','border-bottom','1px solid #ccc')], HU.div([CLASS,'ramadda-button-ok display-button'], 'OK') + SPACE2 +
			     HU.div([CLASS,'ramadda-button-cancel display-button'], 'Cancel'));
		html=HU.div(['style','margin:10px;'], html);
		let dialog = HU.makeDialog({content:html,title:'Map Server',header:true,my:'left top',at:'left bottom',draggable:true,anchor:this.jq(ID_MENU_NEW)});
		let cancel = ()=>{
		    dialog.remove();
		}
		let ok = ()=>{
		    args.forEach(a=>{
			this.cache[a[1]] =  this.jq(a[1]).val().trim();
		    });
		    let predefined = this.jq('predefined').val().trim();
		    let url = this.jq('serverurl').val().trim();
		    if(!Utils.stringDefined(url) && !Utils.stringDefined(predefined)) {
			alert('Please enter a map server');
			return;
		    }
		    let mapOptions = tmpStyle.mapOptions;
		    mapOptions.name = this.jq('servername').val().trim();
		    delete tmpStyle.mapOptions;
		    this.clearCommands();
		    let mapGlyph = new MapGlyph(this,mapOptions.type, mapOptions, null,tmpStyle);
		    mapGlyph.setMapServerUrl(url,this.jq('wmslayer').val().trim(),this.jq('maplegend').val().trim(),predefined);
		    mapGlyph.checkMapServer();
		    this.addGlyph(mapGlyph);
		    this.clearCommands();
		    this.clearMessage2(1000);
		    dialog.remove();
		}
		dialog.find('.ramadda-button-ok').button().click(ok);
		dialog.find('.ramadda-button-cancel').button().click(()=>{
		    dialog.remove();
		});
		return;
	    }


	    if(glyphType.isImage() || glyphType.isEntry()||glyphType.isMultiEntry() || glyphType.isMap() || glyphType.isData()) {
		let callback = (entryId,imageUrlOrEntryAttrs,resourceId) =>{
		    let attrs = {};
		    let imageUrl;
		    if(typeof imageUrlOrEntryAttrs == 'string') {
			imageUrl = imageUrlOrEntryAttrs;
		    } else {
			attrs = imageUrlOrEntryAttrs;
		    }
		    attrs.entryId = entryId;
		    if(glyphType.isImage()) {
			tmpStyle.strokeColor='#ccc';
			tmpStyle.fillColor = 'transparent';
		    } else {
			$.extend(tmpStyle.mapOptions,attrs);
		    }
		    tmpStyle.mapOptions.entryId = entryId;
		    tmpStyle.mapOptions.entryType = attrs.entryType;

		    if(glyphType.isMap()) {
			if(resourceId) {
			    let resource  =MAP_RESOURCES_MAP[resourceId];
			    attrs.name = resource.name;
			    attrs.entryType = resource.type;
			    attrs.resourceUrl = resource.url;
			    if(resource.style) $.extend(tmpStyle,resource.style);
			}

			let mapOptions = tmpStyle.mapOptions;
			delete tmpStyle.mapOptions;
			mapOptions.name = attrs.entryName;
			$.extend(mapOptions,attrs);
			let mapGlyph = this.handleNewFeature(null,tmpStyle,mapOptions);
			mapGlyph.checkMapLayer();
			this.clearCommands();
			return;
		    } 

		    //Hacky, gotta clean up all of this
		    if(attrs.entryName && !glyphType.isImage()) {
			attrs.name  = attrs.entryName;				
			tmpStyle.label = attrs.entryName;				
			delete attrs.entryName;
		    }

		    if(glyphType.isMultiEntry()) {
			let mapOptions = tmpStyle.mapOptions;
			$.extend(mapOptions,attrs);
			delete tmpStyle.mapOptions;
			this.clearCommands();
			let mapGlyph = this.handleNewFeature(null,tmpStyle,mapOptions);
			mapGlyph.addEntries(true);
			this.clearCommands();
			return
		    }

		    if(glyphType.isData()) {
			let mapOptions = tmpStyle.mapOptions;
			$.extend(mapOptions,attrs);
			delete tmpStyle.mapOptions;
			this.clearCommands();
			this.createData(mapOptions);
			this.clearCommands();
			return;
		    }

		    if(glyphType.isEntry() && (Utils.isDefined(attrs.latitude) || Utils.isDefined(attrs.north))) {
			if(confirm('Do you want to use this entry\'s location?')) {
			    let style = $.extend({},tmpStyle);
			    style.externalGraphic =attrs.icon;
			    let glyphAttrs = tmpStyle.mapOptions;
			    delete tmpStyle.mapOptions;
			    $.extend(glyphAttrs,attrs);
			    glyphAttrs.useentrylocation = true;
			    let points = Utils.isDefined(attrs.latitude)?[attrs.latitude,attrs.longitude]:[attrs.north,attrs.west];
			    let mapGlyph = this.createMapMarker(GLYPH_ENTRY,glyphAttrs, style,points,true);
			    mapGlyph.applyEntryGlyphs();
			    this.clearCommands();
			    mapGlyph.panMapTo();
			    return
			}
		    }

		    if(glyphType.isImage()) {
			let url = imageUrl??ramaddaBaseUrl +'/entry/get?entryid=' + entryId;
			this.lastImageUrl = url;
			tmpStyle.imageUrl = url;
		    } else if(attrs.icon) {
			tmpStyle.externalGraphic = attrs.icon;
		    }
		    tmpStyle.mapOptions.entryId = entryId;
		    tmpStyle.mapOptions.name = attrs.name;			    
		    cmd.handler.style = tmpStyle;
		    cmd.handler.layerOptions.styleMap=styleMap;
		    this.showCommandMessage(glyphType.isImage()?'Click and drag to create image':'New Entry Marker');
		    cmd.activate();
		    if(this.selector) this.selector.cancel(true);
		};

		if(args.url) {
		    callback(null, args.url);
		    return;
		}
		
		//Do this a bit later because the dialog doesn't get popped up
		let initCallback = ()=>{
		    this.jq('mapresource').change(()=>{
			callback('',{},this.jq('mapresource').val());
			if(this.selector) this.selector.cancel();
		    });
		    this.jq('imageurl').keypress(function(e){
			if(e.keyCode == 13) {
			    callback('',$(this).val());
			}
		    });
		};
		let extra = null;
		if(glyphType.isImage()) {
		    extra = HU.b('Enter Image URL: ') + HU.input('',this.lastImageUrl??'',['id',this.getDomId('imageurl'),'size','40']);
		} else if(glyphType.isMap() && MAP_RESOURCES) {
		    let ids = MAP_RESOURCES.map((r,idx)=>{
			return [idx,r.name];
		    });
		    ids = Utils.mergeLists([['','Select Resource']],ids);
		    extra = HU.b('Load Map: ') + HU.select('',['id',this.domId('mapresource')],ids);
		}			    
		if(extra!=null) {
		    extra = this.wrapDialog(extra + '<br>Or select entry:');
		}
		let props = {title:glyphType.isImage()?'Select Image':
			     (glyphType.isEntry()||glyphType.isMultiEntry()?'Select Entry':glyphType.isData()?'Select Data':'Select Map'),
			     extra:extra,
			     initCallback:initCallback,
			     callback:callback,
			     'eventSourceId':this.domId(ID_MENU_NEW)};
		let entryType = glyphType.isImage()?'type_image':glyphType.isMap()?Utils.join(MAP_TYPES,','):'';
		props.typeLabel  = glyphType.isImage()?'Images':glyphType.isMap()?'Maps':'';
		this.selector = selectCreate(null, HU.getUniqueId(''),'',false,'entryid',this.getProperty('entryId'),entryType,null,props);
		return
	    } 
	    if(glyphType.getType() == GLYPH_MARKER) {
		let input =  HU.textarea('',this.lastText??'',[ID,this.domId('labeltext'),'rows',3,'cols', 40]);
		let html =  HU.formTable();
		html += HU.formEntryTop('Label:',input);
		let icon = this.lastIcon || tmpStyle.externalGraphic;
		let icons = HU.image(icon,['id',this.domId('externalGraphic_image')]) +
		    HU.hidden('',icon,['id',this.domId('externalGraphic')]);
		if(!Utils.isDefined(this.lastIncludeIcon)) this.lastIncludeIcon = true;
		html += HU.formEntry('',HU.checkbox(this.domId('includeicon'),[],this.lastIncludeIcon,'Include Icon')+' ' + icons);
		html+='</table>';
		html+=HU.div(['style',HU.css('text-align','center','padding-bottom','8px','margin-bottom','8px','border-bottom','1px solid #ccc')], HU.div([CLASS,'ramadda-button-ok display-button'], 'OK') + SPACE2 +
			     HU.div([CLASS,'ramadda-button-cancel display-button'], 'Cancel'));
		
		html+=HU.b('Select Icon');
		html+=HU.div(['id',this.domId('recenticons')]);
		html+=HU.div(['id',this.domId('icons')]);
		html=HU.div(['style',HU.css('xmin-width','250px','margin','5px')],html);
		let dialog = HU.makeDialog({content:html,title:'Marker',header:true,my:'left top',at:'left bottom',draggable:true,anchor:this.jq(ID_MENU_NEW)});

		this.addIconSelection(this.jq('icons'));
		let cancel = ()=>{
		    dialog.remove();
		}
		let ok = ()=>{
		    let doIcon = this.lastIncludeIcon  = this.jq('includeicon').is(':checked');
		    if(!doIcon) {
			tmpStyle.externalGraphic=icon_blank;
		    } else {
			this.lastIcon = this.jq('externalGraphic').val();
			tmpStyle.externalGraphic = this.lastIcon;
			tmpStyle.labelAlign='cb';
			tmpStyle.labelYOffset='12';
		    }
		    let text = this.jq('labeltext').val();
		    dialog.remove();
		    //			    if(!Utils.stringDefined(text)) return;
		    this.lastText = text;
		    tmpStyle.label = text;
		    cmd.handler.style = tmpStyle;
		    cmd.handler.layerOptions.styleMap=styleMap;
		    this.showCommandMessage('New Marker');
		    cmd.activate();
		}
		dialog.find('.ramadda-button-ok').button().click(ok);
		dialog.find('.ramadda-button-cancel').button().click(()=>{
		    dialog.remove();
		});
		return;
	    }
	    cmd.handler.style = tmpStyle;
	    cmd.handler.layerOptions.styleMap=styleMap;

	    let message = glyphType?'New ' + glyphType.getName():cmd.message??'';

	    if(glyphType.isRoute()) {
		let html =  HU.formTable();
		if(this.getProperty('hereRoutingEnabled') || this.getProperty('googleRoutingEnabled')) {
		    let providers = [];
		    if(this.getProperty('googleRoutingEnabled')) providers.push('google');
		    if(this.getProperty('hereRoutingEnabled')) providers.push('here');			
		    html+=HU.formEntry('Provider:', HU.select('',['id',this.domId('routeprovider')],providers,this.routeProvider));
		}
		html+=HU.formEntry('Route Type:' , HU.select('',['id',this.domId('routetype')],['car','bicycle','pedestrian'],this.routeType));
		html += HU.close(TAG_TABLE);
		let buttons  =HU.div([CLASS,'ramadda-button-ok display-button'], 'OK') + SPACE2 +
		    HU.div([CLASS,'ramadda-button-cancel display-button'], 'Cancel');	    
		html+=HU.div(['style',HU.css('text-align','right','margin-top','5px')], buttons);
		html=HU.div(['style',HU.css('xmin-width','250px','margin','5px')],html);
		let dialog = HU.makeDialog({content:html,title:'Select Route Type',header:true,my:'left top',at:'left bottom',anchor:this.jq(ID_MENU_NEW)});
		let ok = ()=>{
		    cmd.handler.finishedWithRoute = false;
		    this.routeProvider = this.jq('routeprovider').val();
		    this.routeType = this.jq('routetype').val();
		    dialog.remove();
		    this.showCommandMessage(message+': ' + Utils.makeLabel(this.routeType)+' - Draw one or more line segments');
		    cmd.activate();
		};
		dialog.find('.ramadda-button-ok').button().click(ok);
		dialog.find('.ramadda-button-cancel').button().click(()=>{
		    dialog.remove();
		});
		return
	    }

	    cmd.activate();	    


	},
	
	createMapMarker:function(glyphType, glyphAttrs,style,points,andAdd) {
	    let feature = this.makeFeature(this.getMap(),'OpenLayers.Geometry.Point', style, points);
	    feature.style = style;
	    this.addFeatures([feature]);
	    let mapGlyph = new MapGlyph(this,glyphType, glyphAttrs, feature,style);
	    mapGlyph.checkImage(feature);
	    if(andAdd) {
		this.addGlyph(mapGlyph);
	    }
	    return mapGlyph;
	},

	clearCommands:function() {
	    this.getMap().closePopup();
	    this.clearMessage2();
	    this.getMap().clearAllProgress();
	    //	    this.unselectAll();
	    HtmlUtils.hidePopupObject();
	    this.showCommandMessage('');
	    let buttons = this.jq(ID_COMMANDS).find('.ramadda-clickable');
	    buttons.removeClass('imdv-command-active');
	    buttons.each(function() {
		$(this).attr('selected',false);
	    });
	    this.commands.forEach(cmd=>{
		cmd.deactivate();
	    });
	    this.command= null;
	},
	getMapOptions: function(feature) {
	    if(feature.mapOptions) return feature.mapOptions;
	    let mapOptions = feature.style?feature.style.mapOptions:{};
	    if(!mapOptions) {
		mapOptions = {};
		if(feature.style) {
		    mapOptions.type = feature.style.type;
		}
	    }  else if(feature.style) {
		delete feature.style.mapOptions;
	    }
	    feature.mapOptions = mapOptions;
	    return mapOptions;
	},
	initGlyphButtons:function(dom) {
	    let _this = this;
	    dom.find('[buttoncommand]').click(function(event) {
		event.preventDefault();
		let command  = $(this).attr('buttoncommand');
		let id  = $(this).attr('glyphid');
		let mapGlyph   = _this.findGlyph(id);
		if(!mapGlyph) {
		    console.error('No map glyph from id:' + id);
		    return;
		}
		if(command=='toback') _this.changeOrder(false,mapGlyph);
		else if(command=='tofront') _this.changeOrder(true,mapGlyph);		
		else if(command=='edit') {
		    _this.editFeatureProperties(mapGlyph);
		} else if(command==ID_SELECT) {
		    if(mapGlyph.isSelected()) 
			_this.unselectGlyph(mapGlyph);
		    else
			_this.selectGlyph(mapGlyph);
		} else if(command==ID_DELETE) {
		    HU.makeOkCancelDialog($(this),'Are you sure you want to delete this glyph?',
					  ()=>{_this.removeMapGlyphs([mapGlyph]);});
		}
	    });
	},
	
	makeGlyphButtons:function(mapGlyph,includeEdit) {
	    if(!this.canEdit()) return '';
	    let buttons = [];
	    let icon = i=>{
		return HU.getIconImage(i,[],BUTTON_IMAGE_ATTRS);
	    };
	    if(includeEdit) {
		buttons.push(HU.span([CLASS,'ramadda-clickable',TITLE,'Edit','glyphid',mapGlyph.getId(),'buttoncommand','edit'],
				     icon('fas fa-cog')));
	    }
	    buttons.push(
		HU.span([CLASS,'ramadda-clickable',TITLE,'Select','glyphid',mapGlyph.getId(),'buttoncommand',ID_SELECT],
			icon('fas fa-hand-pointer')),
		HU.span([CLASS,'ramadda-clickable',TITLE,'Delete','glyphid',mapGlyph.getId(),'buttoncommand',ID_DELETE],
			icon('fa-solid fa-delete-left'))
	    );
	    return Utils.wrap(buttons,HU.open('span',['style',HU.css('margin-right','8px')]),'</span>');
	},
	makeListItem:function(mapGlyph,idx) {
	    let style  = mapGlyph.getStyle()||{};
	    let line = "";
	    let type = mapGlyph.getType();
	    let select = HU.span(['title','Click to select','style',HU.css('padding-left','0px','padding-right','5px'), 'glyphid',mapGlyph.getId(),'class','ramadda-clickable imdv-feature '], HU.getIconImage('fas fa-arrow-pointer'));
	    let visible = HU.checkbox('',['style','margin-right:5px;','title','Visible','glyphid',mapGlyph.getId(),'class','ramadda-clickable imdv-feature-visible '],mapGlyph.getVisible());
	    let title =  mapGlyph.getLabel();
	    title+='<br>' +
		select + visible +
		this.makeGlyphButtons(mapGlyph,true);
 	    line += HU.td(['nowrap','',STYLE,HU.css('padding','5px')], title);
	    let col = mapGlyph.getDecoration();
	    let msg = this.getDistances(mapGlyph.getGeometry(),mapGlyph.getType());
	    if(msg) {
		col+='' + msg.replace(/<br>/g,' ');
	    }
	    line+= HU.td(['glyphid',mapGlyph.getId(),
			  STYLE,HU.css('padding','5px')],col);
	    return line;
	},	    

	createData:function(mapOptions) {
	    let displayAttrs = {};
	    let callback = text=>{
		let ff = text.match(/"filterFields":\"([^\"]+)"/);
		let regexp = /createDisplay *\( *\"map\" *(.*?)}\);/;
		regexp = /createDisplay\s*\(\s*\"map\"\s*,\s*({[\s\S]+?\})\);/;				
		let match = text.match(regexp);
		let attrs = {};
		let inner;
		let pointDataUrl = null;
		let msg = "";
		if(!match) {
		    msg = "There doesn't seem to be any map displays defined for this entry<br>";
		}
		if(match) {
		    inner = match[1];
		    inner = inner.replace(/\n/g," ");
		    let dr=/\"data\" *: *new *PointData[^\)]+?\)/g;
		    dr=/\"data\"\s*:\s*new\s*PointData\s*\((.*?)\)/g;
		    match  = inner.match(dr);
		    if(match) {
			let re = new RegExp("\"(" + ramaddaBaseUrl+"[^\"]+)\"");
			match = match[0].match(re);
			if(match) 
			    pointDataUrl = match[1];
		    }
		    inner = inner.replace(dr,"");
		    inner = inner.replace(/,\s*\}/,"}");
		    try {
			attrs = JSON.parse(inner);
			if(ff && !attrs.filterFields) attrs.filterFields=ff[1];
		    } catch(err) {
			console.error(err);
			console.error(inner);
		    }
		}
		let skip = ['hereRoutingEnabled','googleRoutingEnabled','canEdit','title','bounds','zoomLevel','mapCenter','width','height','user','entryIcon','entryId','thisEntryType','fileUrl','popupWidth','popupHeight','divid'];
		let userInput = "";
		for(key in attrs) {
		    if(skip.includes(key)) continue;
		    let value  = attrs[key];
		    userInput+=key+"=" + value+"\n";
		}
		let widget =  msg+HU.textarea("",userInput,[ID,this.domId('displayattrs'),"rows",10,"cols", 60]);
		let andZoom = HU.checkbox(this.domId('andzoom'),[],true,"Zoom to display");
		let buttons  =HU.center(HU.div([CLASS,'ramadda-button-ok display-button'], "OK") + SPACE2 +
					HU.div([CLASS,'ramadda-button-cancel display-button'], "Cancel"));
		
		widget = HU.div(['style',HU.css('margin','4px')], widget+"<br>"+andZoom + buttons);
		
		let dialog =  HU.makeDialog({content:widget,anchor:this.jq(ID_MENU_FILE),title:"Map Display Attributes",header:true,draggable:true,remove:false});

		
		dialog.find('.ramadda-button-ok').button().click(()=>{
		    let displayAttrs = this.parseDisplayAttrs(this.jq('displayattrs').val());
		    displayAttrs.pointDataUrl = pointDataUrl;
		    let mapGlyph = this.handleNewFeature(null,null,mapOptions);
		    mapGlyph.addData(displayAttrs,this.jq("andzoom").is(":checked"));
		    dialog.remove();
		});
		dialog.find('.ramadda-button-cancel').button().click(()=>{
		    dialog.remove();
		});		
	    };
	    let error = text=>{
		alert("An error occurred:" + text);
	    };	    
	    let url = ramaddaBaseUrl+"/getwiki";
	    $.post(url,{
		entryid:mapOptions.entryId},
		   callback).fail(error);
	},

	parseDisplayAttrs:function(val) {
	    let displayAttrs = {};
	    let lines =  Utils.split(String(val),"\n",true,true,[]);
	    lines.forEach(line=>{
		let toks  =Utils.splitFirst(line,"=");
		if(toks.length==1) {
		    displayAttrs[toks[0]] = "";
		} else 	if(toks.length>=2) {
		    let v = toks[1];
		    if(v=="true") v = true;
		    else if(v=="false") v = false;			    
		    displayAttrs[toks[0]] = v;
		}
	    });
	    return displayAttrs;
	},
	
	addFeatureList:function() {
	    let features="<table width=100%>";
	    this.glyphListMap = {};
            this.glyphs.forEach((mapGlyph,idx)=>{
		this.glyphListMap[idx]  = mapGlyph;
		let clazz = "";
		if(mapGlyph.isSelected()) {
		    clazz+= " " + LIST_SELECTED_CLASS;
		}
		features+=HU.openTag("tr",['class',LIST_ROW_CLASS+" " + clazz,'valign','top','style','border-bottom:1px solid #ccc',"glyphid",mapGlyph.getId()]);
		let tds=this.makeListItem(mapGlyph,idx);
		features+=tds;
		features+="</tr>";
	    });
	    features+="</table>";
	    let _this  = this;
	    this.jq(ID_LIST).html(features);
	    this.initGlyphButtons(this.jq(ID_LIST));
	    this.jq(ID_LIST).find('.imdv-feature-visible').change(function(){
		let id  = $(this).attr("glyphid");
		let mapGlyph   = _this.findGlyph(id);
		if(!mapGlyph) {
		    console.error("No map glyph from id:" + id);
		    return;
		}
		mapGlyph.setVisible($(this).is(':checked'),true);
	    });
	    this.jq(ID_LIST).find(".imdv-feature").click(function(event) {
		let clazz  = LIST_SELECTED_CLASS;
		let id  = $(this).attr("glyphid");
		let mapGlyph   = _this.findGlyph(id);
		if(!mapGlyph) {
		    console.error("No map glyph from id:" + id);
		    return;
		}
		if (event.shiftKey) {
		    _this.editFeatureProperties(mapGlyph);
		    return;
		}
		let row = $(this).closest('.' + LIST_ROW_CLASS)
		if(row.hasClass(clazz)) {
		    row.removeClass(clazz);
		    _this.unselectGlyph(mapGlyph);
		} else {
		    row.addClass(clazz);
		    _this.selectGlyph(mapGlyph);
		}
	    });
	},
	
	listFeatures:function() {
	    this.clearCommands();
	    let close = () =>{
		if(this.listDialog) {
		    this.listDialog.hide();
		    this.listDialog.remove();
		}
		this.listDialog = null;
	    };
	    close();

	    let html ='';
	    html+=HU.div([ID,this.domId(ID_LIST), STYLE,HU.css('margin-bottom','10px','border','1px solid #ccc', 'max-height','300px','max-width','600px','overflow-x','auto','overflow-y','auto')], '');

	    html+='<center>';
	    html +=HU.div([ID,this.domId(ID_LIST_DELETE), CLASS,'display-button'], 'Delete Selected');
	    html += SPACE2;	    
	    html +=HU.div([ID,this.domId(ID_LIST_CANCEL), CLASS,'display-button'], 'Close');	    
	    html+='</center>';
	    html  = HU.div([CLASS,'wiki-editor-popup'], html);
	    let dialog = this.listDialog  = HU.makeDialog({content:html,anchor:this.jq(ID_MENU_FILE),title:'Features',header:true,draggable:true,remove:false});
	    this.addFeatureList();
	    let _this  = this;
	    this.jq(ID_LIST_DELETE).button().click(()=>{
		let cut  = [];
		this.jq(ID_LIST).find('.imdv-feature-selected').each(function() {
		    let id  = $(this).attr('glyphid');
		    let mapGlyph   = _this.findGlyph(id);
		    if(!mapGlyph) {
			console.error('No map glyph from id:' + id);
			return;
		    }
		    cut.push(mapGlyph);
		});
		this.removeImages(cut);
		this.setClipboard(cut);
		this.removeMapGlyphs(cut);
	    });
	    this.jq(ID_LIST_CANCEL).button().click(close);
	},
	addFeatures:function(features) {
	    let layer = this.myLayer;
	    layer.addFeatures(features);
	    features.forEach(feature=>{
		feature.layer = layer;
	    });
	    //	    this.featureChanged();
	},
	
	removeMapGlyph:function(mapGlyph) {
	    Utils.removeItem(this.glyphs, mapGlyph);
	},

	removeMapGlyphs: function(mapGlyphs) {
	    mapGlyphs.forEach(mapGlyph=>{
		Utils.removeItem(this.glyphs, mapGlyph);
		mapGlyph.doRemove();
	    });
	    this.handleGlyphsChanged();
	},
	removeFeatures: function(features) {
	    if(features)
		this.myLayer.removeFeatures(features);

	},
	addControl:function(name,msg,control) {
	    control.name = name;
	    control.message=msg;
	    this.map.getMap().addControl(control);
	    this.commands.push(control);
	    return control;
	},

	pasteCount:0,
	doPaste: function(evt) {
	    if(!this.clipboard) return;
	    let newOnes = this.clipboard.map(mapGlyph=>{
		return  mapGlyph.clone();
	    });
	    for(let i=0;i<newOnes.length;i++) {
		newOnes[i].type = this.clipboard[i].type;
	    }
	    let h = this.map.getMap().getExtent().getHeight();
	    this.pasteCount++;
	    let delta = (this.pasteCount*0.05)*h;
	    newOnes.forEach(mapGlyph=>{
		mapGlyph.move(delta,-delta);
		//		this.checkImage(feature);
		if(mapGlyph.isMap()) {
		    mapGlyph.checkMapLayer();
		} else if(mapGlyph.isMapServer()) {
		    mapGlyph.checkMapServer();
		}
	    });
	    this.addGlyph(newOnes);
	},
	initSideHelp:function(dialog) {
	    dialog.find('.imdv-property').click(function(){
		let value = $(this).attr('value');
		if(!value) return;
		value = value.replace(/\\n/g,'\n');
		let target = $(this).attr('target');		
		var textComp = GuiUtils.getDomObject(target);
		if(textComp) {
		    insertAtCursor('', textComp.obj, value);
		}
	    });
	},
	makeSideHelp:function(lines,target,props){
	    props = props??{};
	    if(!props.prefix) props.prefix='';
	    if(!props.suffix) props.suffix='';	    
	    let help = '';
	    lines.forEach((line)=>{
		let attrs = ['class','ramadda-clickable imdv-property','target',target];
		if(line.title) {
		    attrs.push('title',line.title);
		}
		if(line.line) {
		    line = line.line;
		}
		attrs.push('value',props.prefix + line + props.suffix);
		help+=HU.div(attrs,line);
	    });
	    help = HU.div(['class','imdv-side-help'], help);
	    return help;
	},
	getFeaturePropertyApply:function() {
	    return (mapGlyph, props)=>{
		mapGlyph.applyPropertiesComponent();
		mapGlyph.applyPropertiesDialog();
		this.featureChanged();	    
		let style = {};
		if(mapGlyph.isData()) {
		    let displayAttrs = this.parseDisplayAttrs(this.jq('displayattrs').val());
		} else if(props) {
		    props.forEach(prop=>{
			let id = 'glyphedit_' + prop;
			if(prop=='externalGraphic') id ='externalGraphic';
			if(prop=='labelSelect') return;
			let v = this.jq(id).val();
			if(prop=='label') {
			    v = v.replace(/\\n/g,'\n');
			}
			if(prop=='showLabels') {
			    v = this.jq(id).is(':checked');
			}
			style[prop] = v;
		    });
		}

		if(style.externalGraphic && !style.externalGraphic.startsWith('data:')) mapGlyph.attrs.icon=style.externalGraphic;

		if(mapGlyph.isData()) {
		    let displayAttrs = this.parseDisplayAttrs(this.jq('displayattrs').val());
		    mapGlyph.applyDisplayAttrs(displayAttrs);
		} else if(mapGlyph.isMultiEntry()) {
		    mapGlyph.addEntries();
		}

		if(Utils.stringDefined(style.popupText)) {
		    style.cursor = 'pointer';
		} else {
		    style.cursor = 'pointer';
		}
		if(mapGlyph.getImage()) {
		    mapGlyph.getImage().setOpacity(style.imageOpacity);
		    mapGlyph.checkImage();
		}

		mapGlyph.applyStyle(style);
		mapGlyph.makeLegend();
		mapGlyph.initLegend();
		this.showMapLegend();
		//TODO:
		this.redraw(mapGlyph);
	    };
	},
	doEdit: function(mapGlyph) {
	    if(!mapGlyph) {
		if(this.getSelected().length==0) return;
		mapGlyph = this.getSelected()[0];
	    }
	    if(!mapGlyph) return;
	    let style = mapGlyph.style;
	    let html =HU.div([STYLE,HU.css('margin','8px')], 'Feature: ' + mapGlyph.type); 
	    this.redraw(mapGlyph);
	    if(mapGlyph.image && Utils.isDefined(mapGlyph.image.opacity)) {
		mapGlyph.style.imageOpacity=mapGlyph.image.opacity;
	    }
	    this.editFeatureProperties(mapGlyph);
	},
	
	makeMenu: function(html) {
	    return  HU.div([CLASS,'wiki-editor-popup'], html);
	},
	menuItem: function(id,label,cmd) {
	    if(cmd) {
		//		HU.image(icon_command,['width','12px']);
		let prefix = '';
		if(cmd!='Esc') prefix = 'Ctrl-';
		label = HU.leftRightTable(label,HU.div(['style','margin-left:8px;'], HU.span(['style','color:#ccc'], prefix+cmd)));
	    }
	    return  HU.div([ID,id,CLASS,'ramadda-clickable'],label);
	},
	editFeatureProperties:function(mapGlyph) {
	    this.doProperties(mapGlyph.getStyle(), this.getFeaturePropertyApply(), mapGlyph);
	},


	makeColorBar:function(domId) {
	    let colors = Utils.split("transparent,red,orange,yellow,#fffeec,green,blue,indigo,violet,white,black,IndianRed,LightCoral,Salmon,DarkSalmon,LightSalmon,Crimson,Red,FireBrick,DarkRed,Pink,LightPink,HotPink,DeepPink,MediumVioletRed,PaleVioletRed,LightSalmon,Coral,Tomato,OrangeRed,DarkOrange,Orange,Gold,Yellow,LightYellow,LemonChiffon,LightGoldenrodYellow,PapayaWhip,Moccasin,PeachPuff,PaleGoldenrod,Khaki,DarkKhaki,Lavender,Thistle,Plum,Violet,Orchid,Fuchsia,Magenta,MediumOrchid,MediumPurple,RebeccaPurple,BlueViolet,DarkViolet,DarkOrchid,DarkMagenta,Purple,Indigo,SlateBlue,DarkSlateBlue,MediumSlateBlue,GreenYellow,Chartreuse,LawnGreen,Lime,LimeGreen,PaleGreen,LightGreen,MediumSpringGreen,SpringGreen,MediumSeaGreen,SeaGreen,ForestGreen,Green,DarkGreen,YellowGreen,OliveDrab,Olive,DarkOliveGreen,MediumAquamarine,DarkSeaGreen,LightSeaGreen,DarkCyan,Teal,Aqua,Cyan,LightCyan,PaleTurquoise,Aquamarine,Turquoise,MediumTurquoise,DarkTurquoise,CadetBlue,SteelBlue,LightSteelBlue,PowderBlue,LightBlue,SkyBlue,LightSkyBlue,DeepSkyBlue,DodgerBlue,CornflowerBlue,MediumSlateBlue,RoyalBlue,Blue,MediumBlue,DarkBlue,Navy,MidnightBlue,Cornsilk,BlanchedAlmond,Bisque,NavajoWhite,Wheat,BurlyWood,Tan,RosyBrown,SandyBrown,Goldenrod,DarkGoldenrod,Peru,Chocolate,SaddleBrown,Sienna,Brown,Maroon,White,Snow,HoneyDew,MintCream,Azure,AliceBlue,GhostWhite,WhiteSmoke,SeaShell,Beige,OldLace,FloralWhite,Ivory,AntiqueWhite,Linen,LavenderBlush,MistyRose,Gainsboro,LightGray,Silver,DarkGray,Gray,DimGray,LightSlateGray,SlateGray,DarkSlateGray",",");
	    let bar = "";
	    let cnt = 0;
	    colors.forEach(color=>{
		bar += HU.div(['title',color,'color',color,'widget-id',domId,'class','ramadda-clickable ramadda-color-select ramadda-dot', 'style',HU.css('background',color)]) +HU.space(1);
		cnt++;
		if(cnt>=10) {
		    cnt = 0;
		    bar+="<br>";
		}
	    });
	    bar = HU.div(['style','max-height:150px;overflow-y:auto;border:1px solid #ccc;'],bar);
	    return bar;
	},
	    

	makeStyleForm:function(style,mapGlyph) {
	    let html = "";
	    let props;
	    let values = {};
	    html +=HU.formTable();
	    if(style) {
		props = [];
		let isImage = style.imageUrl;
		for(a in style) {
		    if(isImage) {
			if(a!="imageUrl" && a!="imageOpacity" && a!="popupText") continue;
		    }
		    props.push(a);
		    values[a] = style[a];
		}
//		if(mapGlyph && mapGlyph.getType()==GLYPH_MARKER) {props = ["pointRadius","externalGraphic"];} 
	    } else {
		props = ['strokeColor','strokeWidth','strokeDashstyle','strokeOpacity',
			 'fillColor','fillOpacity',
			 'pointRadius','externalGraphic','imageOpacity','fontSize','fontWeight','fontStyle','fontFamily','labelAlign','labelXOffset','labelYOffset'];
	    }

	    
	    if(props.includes("entryId") && !props.includes("wikiText")) props.push("wikiText");
	    if(!props.includes("wikiText") && !props.includes("text") && !props.includes("popupText")) {
		props.push("popupText");
	    }
	    let notProps = ['mapOptions','labelSelect','cursor','display']

	    
	    let strip=null;
	    let headers = {
		strokeColor: {label:'Stroke',strip:'stroke'},
		fillColor: {label:'Fill',strip:'fill'},
		fontColor: {label:'Font',strip:'font'},
		labelAlign: {label:'Label',strip:'label'},
	    };
	    props.forEach(prop=>{
		let id = "glyphedit_" + prop;
		let domId = this.domId(id);
		if(notProps.includes(prop)) return;
		let header = headers[prop];
		if(header) {
		    strip=header.strip;
		    html+=HU.tr(['class','formgroupheader'],HU.td(['colspan',2],header.label));
		}  else if(strip && !prop.startsWith(strip)) {
		    html+=HU.tr(['class','formgroupheader'],HU.td(['colspan',2],''));
		    strip = null;
		}
		let label = prop;
		if(strip) label = label.replace(strip,'');
		label =  Utils.makeLabel(label);		
		if(prop=="pointRadius") label = "Size";
		let widget;
		if(prop=="externalGraphic") {
		    label="Marker"
		    let options = "";
		    let graphic = values[prop];
		    if(!Utils.isDefined(graphic))
			graphic = this.getExternalGraphic();
		    widget = HU.hidden("",graphic,['id',this.domId('externalGraphic')]) +
			'<table><tr valign=top><td width=1%>' +
			HU.image(graphic,['width','24px','id',this.domId('externalGraphic_image')]) +
			'</td><td>' +
			HU.div(['style','margin-left:5px;display:inline-block;','id',this.domId("externalGraphic_icons")],'Loading...') +
			"</td></tr></table>";
		    


		} else {
		    let v = values[prop];
		    if(!Utils.isDefined(v) && prop!="wikiText") {
			let propFunc = "get" + prop[0].toUpperCase()+prop.substring(1);
			v = this[propFunc]?this[propFunc]():this.getProperty(prop);
		    }
		    let size = "20";
		    if(prop=="label") {
			size="80";
			widget =  HU.textarea("",v,[ID,domId,"rows",5,"cols", 60]);
			if(mapGlyph.isEntry()) {
			    widget+="<br>" +HU.checkbox(this.domId("useentrylabel"),[],mapGlyph.getUseEntryLabel(),"Use label from entry");
			}
		    } else if(prop=="popupText") {
			return;
			//skip this
		    } else if(prop=="wikiText"|| prop=="text") {
			size="80"
			let icons = this.getUsedMarkers();
			widget =  HU.textarea("",v||"",[ID,domId,"rows",5,"cols", 60]);
			if(icons.length>0) {
			    let hdr = HU.b("Add icon: ");
			    icons.forEach(icon=>{
				hdr+=HU.span(['class','ramadda-clickable ramadda-icons-recent','icon',icon,'textarea',domId],
					      HU.getIconImage(icon,['width','16px']));
				hdr+=' ';
			    });
			    widget = HU.div([],hdr)+widget;
			}
		    } else if(prop=="strokeDashstyle") {
			widget = HU.select("",['id',domId],['solid','dot','dash','dashdot','longdash','longdashdot'],v);
		    } else if(prop=="strokeLinecap") {
			widget = HU.select("",['id',domId],['butt','round','square'],v);
		    } else if(prop=="fontWeight") {
			widget = HU.select("",['id',domId],["normal","bold","lighter","bolder","100","200","300","400","500","600","700","800","900"],v);
 		    } else if(prop=="fontStyle") {
			widget = HU.select("",['id',domId],["normal","italic"],v);			
		    } else {
			if(props == "pointRadius") label="Size";
			if(prop=="strokeWidth" || prop=="pointRadius" || prop=="fontSize" || prop=="imageOpacity") size="4";
			else if(prop=="fontFamily") size="60";
			else if(prop.toLowerCase().indexOf('url')>=0) size="80";
			if(prop.indexOf("Color")>=0) {
			    widget =  HU.input("",v,['class','ramadda-imdv-color',ID,domId,"size",8]);
			    widget =  HU.div(['id',domId+'_display','class','ramadda-dot', 'style',HU.css('background',v)]) +
				HU.space(2)+widget;
//			    widget  = HU.table(['cellpadding','0','cellspacing','0'],HU.tr(['valign','top'],HU.tds([],[widget,bar])));
			} else if(prop=="labelAlign") {
			    html +=HU.formTableClose();
			    html +=HU.formTable();			    
			    //lcr tmb
			    let items = [["lt","Left Top"],["ct","Center Top"],["rt","Right Top"],
					 ["lm","Left Middle"],["cm","Center Middle"],["rm","Right Middle"],
					 ["lb","Left Bottom"],["cb","Center Bottom"],["rb","Right Bottom"]];
			    widget =  HU.select("",['id',domId],items,v);			
			} else if(prop=="showLabels") {
			    widget = HU.checkbox(domId,[],v);
			} else if(prop.indexOf("Width")>=0 || prop.indexOf("Offset")>=0 || prop=="rotation") {
			    let isRotation = prop=="rotation";
			    if(!Utils.isDefined(v)) {
				v=isRotation?0:1;
			    } else if(v==="") {
				v=isRotation?0:1;
			    }
			    let min  = prop.indexOf("Offset")>=0?0:0;
			    let max = isRotation?360:50;
			    widget =  HU.input("",v,[ID,domId,"size",4])+HU.space(4) +
				    
				HU.div(['slider-min',min,'slider-max',max,'slider-step',1,'slider-value',v,'slider-id',domId,ID,domId+'_slider','class','ramadda-slider',STYLE,HU.css("display","inline-block","width","200px")],"");

			} else if(prop.toLowerCase().indexOf("opacity")>=0) {
			    if(!v || v=="") v= 1;
			    widget =  HU.input("",v,[ID,domId,"size",4])+HU.space(4) +
				HU.div(['slider-min',0,'slider-max',1,'slider-value',v,'slider-id',domId,ID,domId+'_slider','class','ramadda-slider',STYLE,HU.css("display","inline-block","width","200px")],"");
			} else {
			    widget =  HU.input("",v,[ID,this.domId(id),"size",size]);
			}
		    }
		}
		html+=HU.formEntry(label+":",widget);
		html+='\n';
	    });
	    html+="</table>";
	    html = HU.div([STYLE,HU.css("max-height","350px","overflow-y","scroll","margin-bottom","5px")], html);
	    return {props:props,html:html};
	},
	
	getLevelRangeWidget:function(level,showMarkerToo) {
	    if(!level) level={};
	    let visibleCbx =
		HU.checkbox(this.domId('showmarkerwhennotvisible'),['id',this.domId('showmarkerwhennotvisible')],
			    showMarkerToo,'Show marker instead');
	    return  HU.b("Visible between levels:") + HU.space(3) +visibleCbx +'<br>'+
		HU.select("",[ID,this.domId("minlevel")],this.levels,Utils.isDefined(level.min)?level.min:"",null,true) +
		" &lt;= level &lt;= " +
		HU.select("",[ID,this.domId("maxlevel")],this.levels,Utils.isDefined(level.max)?level.max:"")+" " +
		HU.span(['class','imdv-currentlevellabel'], "(current level: " + this.getCurrentLevel()+")") +'<br>';
	},
	doProperties: function(style, apply,mapGlyph) {
	    let _this = this;
	    style = style ?? mapGlyph?mapGlyph.getStyle():style;
	    let props;
	    let buttons = "";
	    buttons+="<center>";
	    buttons +=HU.div([CLASS,"display-button","command",ID_APPLY], "Apply");
	    buttons += SPACE2;
	    buttons +=HU.div([CLASS,"display-button","command",ID_OK], "Ok");
	    buttons += SPACE2;
	    if(mapGlyph) {
		buttons +=HU.div([CLASS,"display-button","command",ID_DELETE], "Delete");
		buttons += SPACE2;
	    }
	    buttons +=HU.div([CLASS,"display-button","command",ID_CANCEL], "Cancel");	   
	    buttons+="</center>";
	    let content =[];
	    if(mapGlyph) {
		mapGlyph.addToPropertiesDialog(content,style);
	    }
	    let blocks;
	    if(mapGlyph&&mapGlyph.isData()) {
		let menuAttrs  = mapGlyph?.displayInfo?.display?mapGlyph.displayInfo.display.getWikiEditorTags():null;
		let menuBar = "";
		if(menuAttrs) {
		    blocks = getWikiEditorMenuBlocks(menuAttrs,true);
		    let ctItems =  Utils.getColorTablePopup(null, true);
		    blocks.push({title:"Color table",items:ctItems});
		    //		    menuBar =getWikiEditorMenuBar(blocks,this.domId("displayattrsmenubar"));
		    let cnt = 0
		    let items = [];
		    let seen = {};
		    blocks.forEach((block,idx)=>{
			if(typeof block=="string") {
			    return;
			}
			if(block.items.length==0) return
			if(seen[block.title]) return;
			seen[block.title] = true;
			items.push(HU.div(['class','ramadda-clickable','blockidx',idx], block.title));
		    });
		    menuBar = HU.div([],HU.b("Add:"))+HU.div(['id',this.domId('displayattrsmenubar'),'style',HU.css('margin-left','5px','max-height','200px','overflow-y','auto')], Utils.join(items,""));
		}
		let displayAttrs = mapGlyph.getDisplayAttrs();
		let attrs = "";
		for(a in displayAttrs) {
		    if(Utils.isDefined(displayAttrs[a])) {
			attrs+=a+"="+ displayAttrs[a]+"\n";
		    }
		}
		let textarea = HU.textarea("",attrs,[ID,this.domId('displayattrs'),"rows",10,"cols", 60]);
		content.push({header:"Display Properties", contents: HU.hbox([textarea, menuBar])});
	    } else {
		let r =  this.makeStyleForm(style,mapGlyph);
		content.push({header:"Style",contents:r.html});
		props = r.props;
	    }
	    if(mapGlyph) {
		mapGlyph.getPropertiesComponent(content);
	    }
	    let html = buttons;
	    let accord= HU.makeAccordionHtml(content);
	    html+=accord.contents;
	    html+=buttons;
	    html  = HU.div(['style',HU.css('min-width','700px','min-height','400px'),CLASS,"wiki-editor-popup"], html);
	    this.map.ignoreKeyEvents = true;
	    let dialog = HU.makeDialog({content:html,anchor:this.jq(ID_MENU_FILE),title:"Map Properties" + (mapGlyph?": "+mapGlyph.getName():""),header:true,draggable:true,resizable:true});
	    HU.makeAccordion('#'+accord.id);
	    this.initSideHelp(dialog);


	    dialog.find('.ramadda-icons-recent').click(function() {
		let textarea = $(this).attr('textarea');
		let icon = '<img src=' + $(this).attr('icon')+'>';
		HtmlUtils.insertIntoTextarea(textarea,icon);
	    });


	    this.jq('displayattrsmenubar').find('.ramadda-clickable').click(function() {
		let block = blocks[$(this).attr('blockidx')];
		let sub = Utils.join(block.items,"");
		sub = HU.div(['style','max-height:200px;overflow-y:auto;'], sub);
		let dialog = HU.makeDialog({content:sub,anchor:$(this)});
		let insert = line=>{
		    if(!line) return
		    line = line.trim()+"\n";
		    let v = _this.jq('displayattrs').val().trim();
		    if(v!="") v  +="\n";
		    v =v+line;
		    _this.jq('displayattrs').val(v);
		};
		dialog.find('[colortable]').click(function() {
		    insert("colorTable="+$(this).attr('colortable'));
		    dialog.remove();
		});
		dialog.find('.ramadda-menu-item').click(function() {
		    insert($(this).attr('data-attribute'));
		    dialog.remove();
		});
	    });


	    if(mapGlyph) {
		mapGlyph.initPropertiesComponent(dialog);
		this.initGlyphButtons(dialog);
	    }
	    let icons =dialog.find("#" + this.domId("externalGraphic_icons"));
	    if(icons.length>0) {
		this.addIconSelection(icons);
	    }

	    dialog.find('.ramadda-slider').each(function() {
		let min = $(this).attr('slider-min');
		let max = $(this).attr('slider-max');
		let step = $(this).attr('slider-step')??0.01;
		$(this).slider({		
		    min: +min,
		    max: +max,
		    step:+step,
		    value:$(this).attr('slider-value'),
		    slide: function( event, ui ) {
			let id = $(this).attr('slider-id');
			$("#"+ id).val(ui.value);
		    }});
	    });

	    dialog.find('.ramadda-imdv-color').focus(function() {
		let id = $(this).attr('id');
		let bar = _this.makeColorBar(id);
		let dialog = HU.makeDialog({content:bar,header:false,anchor:$(this),my:"left top",at:"left bottom"});
		dialog.find('.ramadda-color-select').click(function(){
		    let c = $(this).attr('color');
		    let id = $(this).attr('widget-id');
		    $("#"+ id).val(c);
		    $("#"+ id+'_display').css('background',c);
		    dialog.remove();
		});

	    });

	    dialog.find('.ramadda-imdv-color').change(function() {
		let c = $(this).val();
		let id = $(this).attr('id');
		$("#"+ id+'_display').css('background',c);
	    });

	    if(apply==null) {
		apply = () =>{
		    let style = {};
		    props.forEach(prop=>{
			let value = this.jq('glyphedit_'+prop).val();
			this.setProperty(prop, value);
			if(prop == "externalGraphic") {
			    value = this.jq('externalGraphic_image').val();
			    if(value && !value.startsWith(ramaddaBaseUrl))
				value = ramaddaBaseUrl+  value;
			}
			style[prop] = value;
		    });
		    this.glyphTypes.forEach(g=>{
			g.applyStyle(style,true);
		    });
		}
	    }
	    let close = ()=>{
		this.map.ignoreKeyEvents = false;
		dialog.hide();
		dialog.remove();
	    }

	    let applying =false;
	    let myApply = (andClose)=>{
		if(applying) return;
		applying = true;
		apply(mapGlyph,props);
		_this.handleGlyphsChanged();
		if(andClose)  close();
		applying = false;
	    };
	    dialog.find('.display-button').button().click(function() {
		let command = $(this).attr("command");
		switch(command) {
		case ID_OK: 
		    myApply(true);
		    break;
		case ID_APPLY:
		    myApply();
		    break;
		case ID_CANCEL:
		    close();
		    break;
		case ID_DELETE:
		    _this.removeMapGlyphs([mapGlyph]);
		    close();
		    break;
		}
	    });
	},
	addIconSelection:function(icons, callback) {
	    let _this = this;
	    let used = this.getUsedMarkers();
	    if(used.length>0) {
		let html = HU.b("Recent: ");
		used.forEach(icon=>{
		    html+=HU.span(['class','ramadda-clickable ramadda-icons-recent','icon',icon],
				  HU.getIconImage(icon,['width','24px']));
		});
		this.jq('recenticons').html(html);
		this.jq('recenticons').find('.ramadda-icons-recent').click(function(){
		    let icon = $(this).attr('icon');
		    _this.jq("externalGraphic_image").attr('src',icon);
		    _this.jq("externalGraphic").val(icon);			
		    if(callback) callback(icon);
		});
	    }



	    HU.getEmojis(emojis=>{
		let html = "";
		emojis.forEach(cat=>{
		    if(html!="") html+="</div>";
		    html+=HU.open('div',['class','ramadda-imdv-image-category']);
		    html+=HU.div(['class','ramadda-imdv-image-category-label'],HU.b(cat.name));
		    cat.images.forEach(image=>{
			html+=HU.image(image.image,['class','ramadda-clickable ramadda-imdv-image','width','24px','loading','lazy','title',image.name]);
		    });
		});
		html+="</div>";
		html = HU.div(['style',HU.css('width','400px','max-height','200px','overflow-y','auto')], html);
		html = HU.input("","",['id',this.domId('externalGraphic_search'),'placeholder','Search','size','30']) +"<br>"+
		    html;
		icons.html(html);
		let _this = this;
		let images = icons.find('.ramadda-imdv-image');
		images.click(function() {
		    let src = $(this).attr('src');
		    _this.jq("externalGraphic_image").attr('src',src);
		    _this.jq("externalGraphic").val(src);			
		    if(callback) callback(src);
		});
		let cats = icons.find('.ramadda-imdv-image-category');
		let search = (value) =>{
		    images.each(function() {
			let textOk = true;		
			if(value) {
			    textOk = false;
			    let html = $(this).attr('title').toLowerCase();
			    if(html.indexOf(value)>=0) {
				textOk=true;
			    }
			}
			$(this).attr('imagevisible',textOk);
			if(!textOk) {
			    $(this).fadeOut();
			} else {
			    $(this).show();
			}
		    });
		    cats.each(function() {
			let anyVisible = false;
			$(this).find('.ramadda-imdv-image').each(function() {
			    if($(this).attr('imagevisible')=='true') {
				anyVisible = true;
			    }
			});

			let label = $(this).find('.ramadda-imdv-image-category-label');
			if(!anyVisible) {
			    label.fadeOut();
			} else {
			    label.show();
			}
			
		    });

		};
		jqid(this.domId('externalGraphic_search')).keyup(function() {
		    search($(this).val());
		});
	    });
	},	    

	doSaveAs: function() {
	    let name = prompt("New entry name:");
	    if(!name) return;
	    let args = {
		name:name,
		type:"geo_editable_json",
		group:this.getProperty("parentEntryId"),
		authtoken:this.getProperty("authToken"),
		response:"json"
	    }
	    let url = ramaddaBaseUrl +"/entry/change";
            $.post(url, args, (result) => {
		if(result.entries && result.entries.length) {
		    this.setProperty("entryId",result.entries[0]);
		    this.doSave();
		    this.showMessage("Saved");
		    return
		}
		this.clearFeatureChanged();
		if(result.error) {
		    this.showMessage(result.error);
		} else {
		    this.showMessage(result.message);
		}
	    }).fail(function(jqxhr, textStatus, error) {
		console.log("fail:" + result);
		this.showMessage("failed to save map:" + textStatus +" " + error);
	    });
	},
	doSave: function() {
	    let _this = this;
	    if(this.getProperty("thisEntryType")!="geo_editable_json" && this.getProperty("thisEntryType")!="geo_imdv") {
		this.showMessage("Entry is not the correct type");
		return;
	    }
	    let json = this.makeJson();
	    let url = ramaddaBaseUrl +"/entry/setfile"; 
            let args = {
                entryid: this.getProperty("entryId"),
		"file": json,
            };
	    var formdata = new FormData();
	    formdata.append("entryid",this.getProperty("entryId"));
	    formdata.append("file",json);
	    $.ajax({
		url: url,
		data: formdata,
		processData: false,
		contentType: false,
		type: 'POST',
		enctype: 'multipart/form-data',
		success: function (result) {
		    if(result.error) {
			_this.showMessage(result.error);
		    } else {
			_this.clearFeatureChanged();
			_this.showMessage(result.message);
		    }
		}
	    }).fail(function(jqxhr, textStatus, error) {
		_this.showMessage("failed to save map:" + textStatus +" " + error);
	    });
	},
	doImport: function() {
	    let callback = (entryId) =>{
		let url = ramaddaBaseUrl+"/entry/get?entryid=" + entryId;
		this.showProgress("Importing map...");
		let finish = ()=>{
		    this.display.clearMessage2();
		    this.getMap().clearAllProgress();
		}
		$.ajax({
                    url: url,
                    dataType: 'text',
                    success: (data) => {
			finish();
			if(data=="") data="[]";
			let json = JSON.parse(data);
			this.loadAnnotationJson(json,this.map,this.myLayer);
			this.featureChanged(true);
		    }
		}).fail(err=>{
		    finish();
		    this.handleError(err);
		});		    
	    };
	    let props = {title:'Select IMDV entry to import',
			 callback:callback,
			 'eventSourceId':this.domId(ID_MENU_FILE),
			 typeLabel:'IMDV Entries'};
	    this.selector = selectCreate(null, HU.getUniqueId(""),"",false,'entryid',this.getProperty('entryId'),'geo_imdv',null,props);

	},
	

	doDownload: function() {
	    let json = this.makeJson();
	    Utils.makeDownloadFile("map.json",json);
	},
	makeJson: function() {
	    let _this = this;
	    let list =[];
            this.getGlyphs().forEach(mapGlyph=>{
		list.push(mapGlyph.makeJson());
	    });
	    let latlon = this.getMap().getBounds();
	    let tbounds =  _this.getMap().transformLLBounds(latlon);
	    let json  = {
		mapProperties:this.mapProperties||{},
		glyphs:list,
		zoomLevel:this.getCurrentLevel(),
		bounds:{
		    north:latlon.top,
		    west:latlon.left,
		    south:latlon.bottom,
		    east:latlon.right,
		}
	    };
            let baseLayer = this.getMap().getMap().baseLayer?.ramaddaId;
	    if(baseLayer) {
		json.baseLayer = baseLayer;
	    }
	    return  JSON.stringify(json);
	},
	loadAnnotationJson: function(mapJson,map,layer) {
	    let glyphs = mapJson.glyphs||[];
	    glyphs.forEach(jsonObject=>{
		let mapGlyph = this.makeGlyphFromJson(jsonObject);
		if(mapGlyph) this.addGlyph(mapGlyph,true);
	    });
	    this.clearFeatureChanged();
	    this.checkMapProperties();
	    this.makeLegend();
	    this.showMapLegend();
	    this.checkVisible();
	},

	makeGlyphFromJson:function(jsonObject) {
	    let mapOptions = jsonObject.mapOptions;
	    if(!mapOptions) {
		mapOptions = {
		    type:jsonObject.type
		}
	    }
	    mapOptions.id = jsonObject.id;
	    let type = jsonObject.type||mapOptions.type;
	    let glyphType = this.getGlyphType(type);
	    if(!glyphType) {
		console.log("no type:" + type);
		console.dir(this.glyphTypeMap);
		return null;
	    }
	    let style = $.extend({},glyphType.getStyle());
	    if(jsonObject.style) $.extend(style,jsonObject.style);
	    style = $.extend({},style);
	    if(Utils.stringDefined(style.externalGraphic)) {
		this.markers[style.externalGraphic] = true;
	    }
	    if(Utils.stringDefined(style.popupText)) {
		style.cursor = 'pointer';
	    } else {
		style.cursor = 'pointer';
	    }
	    if(glyphType.isData()) {
		let mapGlyph = new MapGlyph(this,mapOptions.type, mapOptions);
		mapGlyph.addData(mapOptions.displayAttrs,false);
		return mapGlyph;
	    }

	    if(glyphType.isMultiEntry()) {
		let mapGlyph = new MapGlyph(this,mapOptions.type, mapOptions, null,style);
		mapGlyph.addEntries();
		return mapGlyph;
	    }  
	    if(glyphType.isFixed()) {
		let mapGlyph = new MapGlyph(this,mapOptions.type, mapOptions, null,style);
		mapGlyph.addFixed();
		return mapGlyph;
	    }
	    if(glyphType.isGroup()) {
		let mapGlyph = new MapGlyph(this,mapOptions.type, mapOptions, null,style);
		mapGlyph.loadJson(jsonObject);
		return mapGlyph;
	    }
	    if(glyphType.isMap()) {
		let mapGlyph = new MapGlyph(this,mapOptions.type, mapOptions, null,style);
		mapGlyph.checkMapLayer(false);
		return mapGlyph;
	    }
	    if(glyphType.isMapServer()) {
		let mapGlyph = new MapGlyph(this,mapOptions.type, mapOptions, null,style);
		mapGlyph.checkMapServer(false);
		return mapGlyph;
	    }  		
	    let points=jsonObject.points;
	    if(!points || points.length==0) {
		console.log("Unknown glyph:" + mapOptions.type);
		return null;
	    }
	    
	    let feature = this.makeFeature(this.getMap(),jsonObject.geometryType, style, points);
	    if(feature) {
		feature.style = style;
		this.addFeatures([feature]);
		let mapGlyph = new MapGlyph(this,mapOptions.type, mapOptions, feature,style);
		mapGlyph.checkImage(feature);
		//If its an entry then fetch the entry info from the repository and use the updated lat/lon and name
		if(glyphType.isEntry()) {
		    let callback = (entry)=>{
			//the mapglyphs are defined by the type
			mapGlyph.putTransientProperty("mapglyphs", entry.mapglyphs);
			if(mapGlyph.getUseEntryName()) 
			    mapGlyph.setName(entry.getName());
			if(mapGlyph.getUseEntryLabel())
			    mapGlyph.style.label= entry.getName();
			if(mapGlyph.getUseEntryLocation() && entry.hasLocation()) {
			    let feature = this.makeFeature(this.getMap(),"OpenLayers.Geometry.Point", mapGlyph.style,
							   [entry.getLatitude(), entry.getLongitude()]);
			    feature.style = mapGlyph.style;
			    mapGlyph.addFeature(feature,true);
			    this.addFeatures([feature]);
			}

			mapGlyph.applyEntryGlyphs();
			mapGlyph.applyMapStyle();
			this.redraw(mapGlyph);
			this.makeLegend();
		    };

		    getRamadda().getEntry(mapOptions.entryId, callback);
		}
		return mapGlyph;
	    } 
	    console.log("Couldn't make feature:" + mapOptions.type);
	    return null;
	},


	makeFeature:function(map,geometryType, style, points) {
	    if(points.length>2) {
		let latLons = [];
		for(let i=0;i<points.length;i+=2) {
		    latLons.push(MapUtils.createPoint(points[i+1],points[i]));
		}
		if(geometryType=="OpenLayers.Geometry.Polygon") {
		    map.transformPoints(latLons);
		    let linearRing = MapUtils.createLinearRing(latLons);
		    let geom = MapUtils.createPolygon(linearRing);
		    return MapUtils.createVector(geom,null,style);
		} else {
		    return  map.createPolygon("","",latLons,style,null,geometryType=="OpenLayers.Geometry.LineString");
		}
	    } 
	    let point =  MapUtils.createLonLat(points[1], points[0]);
	    return  map.createPoint("",point,style);
	},
	doMapProperties:function() {
	    if(!this.mapProperties)this.mapProperties={};
	    let accords = [];

	    let buttons = HU.buttons([HU.div([CLASS,'ramadda-button-apply display-button'], 'Apply'),
				      HU.div([CLASS,'ramadda-button-ok display-button'], 'OK'),
				      HU.div([CLASS,'ramadda-button-cancel display-button'], 'Cancel')]);
	    let cbxs = [HU.checkbox(this.domId('usercanedit'),[],
				    Utils.isDefined(this.mapProperties.userCanEdit)?this.mapProperties.userCanEdit:false,'User Can Edit'),
			HU.checkbox(this.domId('showopacityslider'),[],
				    Utils.isDefined(this.mapProperties.showOpacitySlider)?this.mapProperties.showOpacitySlider:this.getShowOpacitySlider(),'Show Opacity Slider'),
			HU.checkbox(this.domId('showgraticules'),[],
				    Utils.isDefined(this.mapProperties.showGraticules)?this.mapProperties.showGraticules:false,'Show Lat/Lon Lines'),
			HU.checkbox(this.domId('showmouseposition'), [],
				    Utils.isDefined(this.mapProperties.showMousePosition)?this.mapProperties.showMousePosition:false,'Show Mouse Position'),
			HU.checkbox(this.domId('showaddress'), [],
				    Utils.isDefined(this.mapProperties.showAddress)?this.mapProperties.showAddress:false,'Show Address')			
];
	    let basic = '';
	    basic+=Utils.join(cbxs,'<br>');

	    let right = HU.formTable();
	    right+=HU.formEntry('Legend position:',
				HU.select('',['id',this.domId('legendposition')],[{label:'Left',value:'left'},
										  {label:'Right',value:'right'},
										  {label:'In Map',value:'map'},
										  {label:'None',value:'none'}],
					  this.getMapProperty('legendPosition','left')));
	    right+=HU.formEntry('Legend width:',
				HU.input('',this.getMapProperty('legendWidth',''),['id',this.domId('legendwidth'),'size','4']));
				
	    right+=HU.formTableClose();
	    right+=HU.checkbox(this.domId('showbasemapselect'), [],
			       this.getMapProperty('showBaseMapSelect'),'Show Base Map Select');


	    
	    basic=HU.table([],HU.tr(['valign','top'],HU.td([],basic) + HU.td(['width','50%'], right)));
	    basic+='<p>';
	    basic+=this.getLevelRangeWidget(this.mapProperties.visibleLevelRange,this.mapProperties.showMarkerWhenNotVisible);



	    accords.push({header:'Basic', contents:basic});
	    accords.push({header:'Header/Footer',
		      contents:
			  HU.b('Top Wiki Text:') +'<br>' +
			  HU.textarea('',this.mapProperties.topWikiText??'',['id',this.domId('topwikitext_input'),'rows','4','cols','80']) +"<br>" +
			  HU.b('Bottom Wiki Text:') +'<br>' +
			  HU.textarea('',this.mapProperties.bottomWikiText??'',['id',this.domId('bottomwikitext_input'),'rows','4','cols','80']) +'<br>'
			 });

	    let props = this.mapProperties.otherProperties;
	    if(!props) props="";
	    

	    let lines = ['legendLabel=Some label','filter.show=false','filter.toggle.show=false','showButtons=false'];
	    let help = 'Add property:' + this.makeSideHelp(lines,this.domId('otherproperties_input'),{suffix:'\n'});
	    accords.push({header:'Other Properties',
			  contents:
			  HU.hbox([
			      HU.textarea('',props,['id',this.domId('otherproperties_input'),'rows','8','cols','40']),HU.space(2),help])
			 });
	    

	    let accord = HU.makeAccordionHtml(accords);
	    let html = buttons + accord.contents;
	    html  = HU.div(['style','min-width:700px;min-height:300px;margin:10px;'],html);
	    let anchor = this.jq(ID_MENU_FILE);
	    let dialog = HU.makeDialog({content:html,title:'Properties',header:true,
					my:'left top',at:'left bottom',draggable:true,anchor:anchor});

	    this.initSideHelp(dialog);
	    HU.makeAccordion('#'+accord.id);
	    let close=()=>{
		dialog.hide();
		dialog.remove();
	    }
	    let apply = ()=>{
		this.mapProperties.userCanEdit = this.jq('usercanedit').is(':checked');
		this.mapProperties.showOpacitySlider = this.jq('showopacityslider').is(':checked');
		this.mapProperties.showGraticules = this.jq('showgraticules').is(':checked');
		this.mapProperties.showMousePosition = this.jq('showmouseposition').is(':checked');
		this.mapProperties.showAddress = this.jq('showaddress').is(':checked');								
		this.mapProperties.legendPosition=this.jq('legendposition').val();
		this.mapProperties.legendWidth=this.jq('legendwidth').val();
		this.mapProperties.showBaseMapSelect=this.jq('showbasemapselect').is(':checked');
		this.mapProperties.topWikiText = this.jq('topwikitext_input').val();
		this.mapProperties.bottomWikiText = this.jq('bottomwikitext_input').val();
		this.mapProperties.otherProperties = this.jq('otherproperties_input').val();		
		this.parsedMapProperties = null;
				
		let min = this.jq("minlevel").val().trim();
		let max = this.jq("maxlevel").val().trim();
		if(min=="") min = null;
		if(max=="") max = null;	
		this.mapProperties.visibleLevelRange = {min:min,max:max};

		this.mapProperties.showMarkerWhenNotVisible = this.jq('showmarkerwhennotvisible').is(':checked');
		this.checkMapProperties();
		this.makeLegend();
	    };

	    dialog.find('.ramadda-button-apply').button().click(()=>{
		apply();
	    });
	    dialog.find('.ramadda-button-ok').button().click(()=>{
		apply();
		close();
	    });
	    dialog.find('.ramadda-button-cancel').button().click(()=>{
		close();
	    });

	},

	checkMapProperties: function() {
	    this.mapProperties=this.mapProperties??{};
	    this.checkOpacitySlider();
	    this.checkTopWiki();
	    if(!this.getMap()) return;
	    this.getMap().applyHighlightStyle(this.getOtherProperties());
	    this.getMap().setGraticulesVisible(this.mapProperties.showGraticules);
	    if(this.mapProperties.showMousePosition)
		this.getMap().initMousePositionReadout();
	    else
		this.getMap().destroyMousePositionReadout();		
	},
	

	checkOpacitySlider:function() {
	    let visible;
	    if(Utils.isDefined(this?.mapProperties.showOpacitySlider))
		visible = this.mapProperties.showOpacitySlider;
	    else
		visible = this.getShowOpacitySlider(true);
	    this.getMap().showOpacitySlider(visible);

	},
	canEdit:function() {
	    return this.getProperty("canEdit");
	},
	showFileMenu: function(button) {
	    let _this = this;
	    let html ="";
	    let div = '<div class=ramadda-menu-divider></div>';
	    if(this.canEdit()) {
		html +=this.menuItem(this.domId(ID_SAVE),"Save",'S');
	    }
	    html+= this.menuItem(this.domId(ID_DOWNLOAD),"Download")
	    html+=div;
	    html+= this.menuItem(this.domId(ID_IMPORT),"Import")
	    html+=div;
	    html+= this.menuItem(this.domId(ID_CMD_LIST),"List Features...",'L');
	    html+= this.menuItem(this.domId(ID_CLEAR),"Clear Commands","Esc");
	    html+= this.menuItem(this.domId(ID_REFRESH),"Refresh");	    
	    html+=div;
	    html+= this.menuItem(this.domId(ID_PROPERTIES),"Set Default Style...");
	    html+= this.menuItem(this.domId(ID_MAP_PROPERTIES),"Properties...");
	    html+=div;
	    html+= HU.href(ramaddaBaseUrl+'/userguide/imdv.html','Help',['target','_help']);
	    html  = this.makeMenu(html);
//	    console.log('creating file menu');
	    this.dialog = HU.makeDialog({content:html,anchor:button});
	    let clear = () =>{
		this.clearCommands();
		HU.hidePopupObject(null,true);
	    };

	    this.jq(ID_NAVIGATE).click(function() {
		clear();
		_this.setCommand(null);
	    });
	    this.jq(ID_MAP_PROPERTIES).click(function(){
		clear();
		_this.doMapProperties();
	    });
	    this.jq(ID_SAVE).click(function(){
		clear();
		_this.doSave();
	    });
	    this.jq(ID_SAVEAS).click(function(){
		clear();
		_this.doSaveAs();
	    });	    
	    this.jq(ID_DOWNLOAD).click(function(){
		clear();
		_this.doDownload();
	    });	    
	    this.jq(ID_IMPORT).click(function(){
		clear();
		_this.doImport();
	    });
	    this.jq(ID_PROPERTIES).click(function(){
		clear();
		_this.doProperties();
	    });
	    this.jq(ID_REFRESH).click(function(){
		clear();
		ImdvUtils.scheduleRedraw(this.myLayer);
	    });
	    this.jq(ID_CLEAR).click(function(){
		clear();
		_this.unselectAll();
	    });	    
	    this.jq(ID_CMD_LIST).click(function(){
		clear();
		_this.listFeatures();
	    });	    
	},

	handleEditEvent:function() {
	    if(this.getSelected().length==1) {
		this.doEdit(this.getSelected()[0]);
		return;
	    }
	    this.setCommand(ID_EDIT);
	},
	getDecoration:function(style) {
	    let color = style.strokeColor??"black";
	    let line = "solid";
	    if(style.strokeDashstyle) {
		if(['dot','dashdot'].includes(style.strokeDashstyle)) {
		    line = "dotted";
		} else  if(style.strokeDashstyle.indexOf("dash")>=0) {
		    line = "dashed";
		}
	    }
	    let cssStyle = "";
	    if(color) cssStyle= HU.css('border-bottom' , "3px " + line+ " " +color);
	    let dim = 'width:30px;height:6px;';
	    return  HU.div(['style','margin-bottom:5px;margin-right:5px;display:inline-block;'+dim + cssStyle]);
	},

	showMapLegend: function() {
	    if(!this.getShowMapLegend(true)) return;
	    let _this = this;
	    let html = "";
	    let map = {};
            this.getGlyphs().forEach((mapGlyph,idx)=>{
		if(mapGlyph.getType()!=GLYPH_ROUTE && mapGlyph.getType()!=GLYPH_FREEHAND) return;
		let feature = mapGlyph.getFeature();
		if(!feature) return;
		let msg = this.getDistances(mapGlyph.getGeometry(),mapGlyph.getType(),true);
		let id = HU.getUniqueId('feature_');
		feature.legendId = id;
		map[id] = mapGlyph;
		let item = this.getDecoration(feature.style) + msg;
		html+=HU.div(['class','ramadda-clickable','id',id],item);
	    });
	    if(html!="") {
		html = HU.div(['style','padding:5px;border : var(--basic-border);  background-color : var(--color-mellow-yellow);'], html);
	    }
	    this.jq(ID_MESSAGE3).html(html);
	    this.jq(ID_MESSAGE3).find('.ramadda-clickable').click(function() {
		let id = $(this).attr('id');
		_this.selectGlyph(map[id]);
	    });
	},
	featureChanged:function(skipRedoLegend) {
	    this.featureHasBeenChanged = true;
	},
	clearFeatureChanged:function() {
	    this.featureHasBeenChanged = false;
	},
	showNewMenu: function(button) {
	    let _this = this;
	    let html ="<table><tr valign=top>";
	    let tmp = Utils.splitList(this.glyphTypes,this.glyphTypes.length/4);
	    tmp.forEach(glyphTypes=>{
		html+="<td>&nbsp;</td>";
		html+="<td>";
		glyphTypes.forEach(glyphType=>{
		    let icon = glyphType.options.icon||ramaddaBaseUrl+"/map/marker-blue.png";
		    let label = HU.image(icon,['width','16']) +SPACE1 + glyphType.getName();
		    if(glyphType.getTooltip())
			label = HU.span(['title',glyphType.getTooltip()],label);
		    html+= this.menuItem(this.domId("menunew_" + glyphType.type),label+SPACE2);
		});
		html+="</td>";
	    });
	    html+="</tr></table>";
	    html  = this.makeMenu(html);
	    this.dialog = HU.makeDialog({content:html,anchor:button});
	    this.glyphTypes.forEach(g=>{
		this.jq("menunew_" + g.type).click(function(){
		    HtmlUtils.hidePopupObject(null,true);
		    _this.setCommand(g.type);
		});
	    });

	},


	showEditMenu: function(button) {
	    let html = [[ID_CUT,'Cut','X'],[ID_COPY,'Copy','C'],[ID_PASTE,'Paste','V'],null,
			[ID_SELECTOR,'Select'],
			[ID_SELECT_ALL,'Select All','A'],			
			null,
			[ID_MOVER,'Move','M'],
			[ID_RESHAPE,'Reshape'],
			[ID_RESIZE,'Resize'],
			[ID_ROTATE,'Rotate'],			
			null,
			[ID_TOFRONT,'To Front','F'],
			[ID_TOBACK,'To Back','B'],
			null,
			[ID_EDIT,'Edit Properties','P']].reduce((prev,tuple)=>{
			    prev = prev || '';
			    if(!tuple) return prev+ HU.div([CLASS,'ramadda-menu-divider']);						
			    return prev + 	this.menuItem(this.domId(tuple[0]),tuple[1],tuple[2]);
			},'');
	    
//	    console.log('creating edit menu');
	    this.dialog = HU.makeDialog({content:this.makeMenu(html),anchor:button});

	    let clear = () =>{
		HU.hidePopupObject(null,true);
	    };

	    this.jq(ID_CUT).click(()=>{
		clear();
		this.doCut();
	    });
	    this.jq(ID_SELECT_ALL).click(()=>{
		clear();
		this.selectAll();
	    });
	    
	    this.jq(ID_COPY).click(()=>{
		clear();
		this.doCopy();
	    });	    
	    this.jq(ID_PASTE).click(()=>{
		clear();
		this.doPaste();
	    });
	    this.jq(ID_EDIT).click(()=>{
		clear();
		this.handleEditEvent();
	    });	    
	    [ID_SELECTOR,ID_MOVER,ID_RESHAPE,ID_RESIZE,ID_ROTATE].forEach(command=>{
		this.jq(command).click(()=>{
		    clear();
		    this.setCommand(command);
		});
	    });
	},
	checkSelected:function(mapGlyph) {
	    if(mapGlyph.isSelected()) {
		this.unselectGlyph(mapGlyph);
		this.selectGlyph(mapGlyph);
	    }
	},
	unselectGlyph:function(mapGlyph) {
	    if(!mapGlyph) return;
	    mapGlyph.unselect();
	},
	selectGlyph:function(mapGlyph,maxPoints,dontRedraw) {
	    return mapGlyph.select(maxPoints,dontRedraw);
  	},

	toggleSelectGlyph:function(mapGlyph) {
	    if(mapGlyph.isSelected()) {
		this.unselectGlyph(mapGlyph);
	    } else {
		this.selectGlyph(mapGlyph);
	    }
	},
	selectFeatures:function(mapGlyph,features,maxPoints,debug) {
	    let pointCount = 0;
	    if(!features || features.length==0) return pointCount;
	    let vertices = [];
	    features.forEach(feature=>{
		if(!feature.geometry) return;
		let geom  = feature.geometry.getVertices();
		vertices.push(...geom);
	    });
	    let step = 0;
	    if(vertices.length>maxPoints) {
		step = Math.round(vertices.length/maxPoints);
	    }
	    if(debug) console.log(vertices.length,step);
	    vertices.forEach((pt,idx)=>{
		if(step>0) {
		    if(idx>0 && idx!=vertices.length-1) {
			if(idx%step!=0) return
		    }
		}
		//Make a copy since this if there is a shared point it screws up the redraw of the original feature
                pt = MapUtils.createPoint(pt.x,pt.y);
		let dot = MapUtils.createVector(pt,null,this.DOT_STYLE);
		mapGlyph.selectDots.push(dot);
		pointCount++;
	    });
	    return pointCount;
	},

	getSelected: function() {
	    let selected=[];
	    this.getGlyphs().forEach(mapGlyph=>{
		mapGlyph.getSelected(selected);
	    });
	    return selected;
	},
	selectAll:function() {
	    this.getGlyphs().forEach(mapGlyph=>{
		this.selectGlyph(mapGlyph,20,true);
	    });
	},
	unselectAll:function() {
	    this.getGlyphs().forEach(mapGlyph=>{
		this.unselectGlyph(mapGlyph);
	    });
	},	
	setClipboard:function(mapGlyphs) {
	    if(mapGlyphs)
		this.clipboard = mapGlyphs.map(mapGlyph=>{return mapGlyph;});
	    else
		this.clipboard=null;
	    this.pasteCount=0;
	},
	removeImages: function(mapGlyphs) {
	    if(!mapGlyphs) return;
	    mapGlyphs.forEach(mapGlyph=>{
		mapGlyph.removeImage();
	    });
	},
	clearBounds:function(geom) {
	    if(geom.clearBounds) geom.clearBounds();
	    if(geom.components) {
		geom.components.forEach(g=>{
		    this.clearBounds(g);
		});
	    }
	},
	changeOrder: function(toFront, mapGlyph) {
	    let selected = mapGlyph?[mapGlyph]:this.getSelected();
	    if(!selected || selected.length==0) {
		return;
	    }
	    selected.forEach(mapGlyph=>{
		if(toFront)
		    Utils.toFront(this.getGlyphs(),mapGlyph);
		else
		    Utils.toBack(this.getGlyphs(),mapGlyph);
		mapGlyph.changeOrder(toFront);
	    });
	    this.featureChanged();
	    this.redraw();
	},

	moveGlyphBefore:function(glyph1,glyph2,list) {
	    Utils.moveBefore(list??this.glyphs,glyph1,glyph2);
	    glyph2.changeOrder(false);
	    this.featureChanged();
	    this.redraw();
	},

	doCut: function() {
	    let selected = this.getSelected();
	    if(selected.length>0) {
		this.removeImages(selected);
		let tmp = selected.map(feature=>{return feature;});
		this.setClipboard(tmp);
		this.removeMapGlyphs(tmp);
	    }
	},
	doCopy: function() {
	    if(this.getSelected().length==0) return;
	    this.setClipboard(this.getSelected().map(mapGlyph=>{return mapGlyph;}));
	},
	addGlyphType: function(glyph) {
	    this.glyphTypes.push(glyph);
	    this.glyphTypeMap[glyph.getType()]  = glyph;
	},
	getGlyphType:function(type) {
	    return this.glyphTypeMap[type];
	},
	createMapGlyph: function(attrs,style,andZoom) {				
	    style = style??{};
	    let layer = this.addMapLayer(attrs,style,andZoom);
	    if(!layer) return;
	    layer.style = style;
	    return layer;
	},
	createMapLayer:function(mapGlyph,opts,style,andZoom) {
	    let url;
	    if(opts.resourceUrl) {
		//For now proxy the request through our ramadda
		url =   ramaddaBaseUrl+'/proxy?url=' + encodeURIComponent(opts.resourceUrl);
	    } else {
		url = ramaddaBaseUrl +"/entry/get?entryid="+opts.entryId;
	    }
	    mapGlyph.setDownloadUrl(url);
	    let selectCallback = (feature,layer,event)=>{
		//Don't handle the feature selected if we have a drawing command
		if(Utils.stringDefined(this.command)) return;
		mapGlyph.featureSelected(feature,layer,event);
	    }
	    let unselectCallback = (feature,layer,event)=>{
		mapGlyph.featureUnselected(feature,layer,event);
	    }	    
	    let errorCallback = (url,err)=>{
		this.handleError(err,url);
	    };

	    let loadCallback = (map,layer)=>{
		if(layer.mapGlyph) {
		    layer.mapGlyph.handleMapLoaded(map,layer);
		}
	    }
	    switch(opts.entryType) {
	    case 'latlonimage': 
		let w = 2048;
		let h = 1024;
		return this.getMap().addImageLayer(opts.entryId, opts.name,"",url,true,
						   opts.north, opts.west,opts.south,opts.east, w,h);
	    case 'geo_gpx': 
		return this.getMap().addGpxLayer(opts.name,url,true, selectCallback, unselectCallback,style,loadCallback,andZoom,errorCallback);
		break;
	    case 'geo_geojson': 
		return this.getMap().addGeoJsonLayer(opts.name,url,true, selectCallback, unselectCallback,style,loadCallback,andZoom,errorCallback);
		break;		
	    case 'geo_shapefile_fips': 
	    case 'geo_shapefile': 
		url = ramaddaBaseUrl+'/entry/show?entryid=' + opts.entryId+'&output=shapefile.kml&formap=true';
		//fall thru to kml
	    case 'geo_kml': 
		let loadCallback2 = (map,layer)=>{
		    if(layer.features) {layer.features.forEach(f=>{f.style = style;});}
		    loadCallback(map,layer);
		};
		let layer =  this.getMap().addKMLLayer(opts.name,url,true, selectCallback, unselectCallback,style,loadCallback2,andZoom,errorCallback);
		return layer;
	    default:
		this.handleError('Unknown map type:' + opts.entryType);
		return null;
	    }
	},
	loadMap: function(entryId) {
	    let _this = this;
	    //Pass in true=skipParent
	    let url = this.getProperty("fileUrl",null,false,true);
	    if(!url && entryId)
		url = ramaddaBaseUrl+"/entry/get?entryid=" + entryId;
	    if(!url) return;
	    this.showProgress("Loading map...");
	    let finish = ()=>{
		this.showCommandMessage('');
		this.getMap().clearAllProgress();
	    }
            $.ajax({
                url: url,
                dataType: 'text',
                success: (data) => {
		    finish();
		    if(data=="") data="[]";
		    try {
			let json = JSON.parse(data);
			let bounds = null;
			let zoomLevel = -1;
			if(json.baseLayer) {
			    let base = _this.map.baseLayers[json.baseLayer];
			    if(base) {
				_this.map.getMap().setBaseLayer(base);
			    }


			}
			if(!_this.getProperty("embedded") && !_this.getProperty("zoomLevel")) {
			    if(json.bounds) {
				zoomLevel = json.zoomLevel;
				bounds =  MapUtils.createBounds(json.bounds.west,
								json.bounds.south,
								json.bounds.east,
								json.bounds.north);

				bounds =  _this.getMap().transformLLBounds(bounds);
			    } else {
				_this.getGlyphs().forEach(mapGlyph=>{
				    let feature = mapGlyph.getFeature();
				    if(!feature) return;
				    if(!bounds)
					bounds = MapUtils.createBounds();
				    bounds.extend(feature.geometry.getBounds());
				    if(feature.mapLayer) {
					let dataBounds = feature.mapLayer.getDataExtent();
					if(dataBounds) {
					    bounds.extend(dataBounds);
					}
				    }
				});
			    }				     
			}
			this.mapProperties = json.mapProperties||this.mapProperties||{};

			//Check the map legend
			if(this.mapLegendToggleId) {
			    if(Utils.isDefined(this.mapProperties.mapLegendOpen)) {
				if(!this.mapProperties.mapLegendOpen) {
				    $("#" + this.mapLegendToggleId).css('display','none');
				}
			    }

			}

			this.getMap().applyHighlightStyle(this.getOtherProperties());
			if(zoomLevel>=0 && Utils.isDefined(zoomLevel)) {
			    _this.getMap().setZoom(zoomLevel);
			}
			if(bounds) {
			    _this.map.getMap().setCenter(bounds.getCenterLonLat());
			}
			try {
			    this.loadAnnotationJson(json,_this.map,_this.myLayer);
			} catch(err) {
			    this.handleError(err);
			}
			this.clearFeatureChanged();
			this.checkMapProperties();
			this.makeLegend();
			this.showMapLegend();
			this.checkVisible();
		    } catch(err) {
			this.showMessage("Failed to load map:" + err);
			console.log("error:" + err);
			console.log(err.stack);
			console.log("map json:" + data);
		    }

                }
            }).fail(err=>{
		finish();
		this.handleError(err);
	    });
	},
	handleError:function(err,url) {
	    if(err.stack) console.error(err.stack);
	    let message;
	    let responseText = err.responseText??err?.priv?.responseText;
	    if(responseText) {
		let match = responseText.match(/<div\s+class\s*=\s*"ramadda-message-inner">(.*?)<\/div>/);
		if(match) message = match[1];
		if(!message) {
		    if(responseText.startsWith("{")) {
			match = responseText.match(/error:'(.*)'/);
			if(match)   message = match[1];
		    }
		}
	    }
	    if(message ==null && (typeof err) =="object") {
		if(err.error) message= err.error;
		else {
		    if(err.responseText) {
			message= Utils.stripTags(responseText);
		    }
		}
	    }
	    err = message??err;
	    if(url) err="Error loading URL:" + url+"<br>"+err;
	    this.showMessage(err);
	},
	doMakeMapGlyphs:function() {
	    let externalGraphic = this.getExternalGraphic();
	    if(!externalGraphic.startsWith(ramaddaBaseUrl)) externalGraphic = ramaddaBaseUrl+externalGraphic;
	    new GlyphType(this,GLYPH_GROUP,"Group",
			  {externalGraphic: externalGraphic,
			   pointRadius:this.getPointRadius(10),
			   fontColor: this.getProperty("labelFontColor","#000"),
			   fontSize: this.getFontSize(),
			   fontFamily: this.getFontFamily(),
			   fontWeight: this.getFontWeight(),
			   fontStyle: this.getFontStyle(),
			   labelAlign: this.getProperty("labelAlign","cb"),
			   labelXOffset: this.getProperty("labelXOffset","0"),
			   labelYOffset: this.getProperty("labelYOffset","14"),
			   labelOutlineColor:this.getProperty("labelOutlineColor","#fff"),
			   labelOutlineWidth: this.getProperty("labelOutlineWidth","0"),

			   strokeWidth:0,
			   fillColor:'transparent',
			   labelSelect:true,
			  },
			  MyEntryPoint,
			  {isGroup:true, tooltip:'Add group',			  
			   icon:ramaddaBaseUrl+"/icons/chart_organisation.png"});
	    new GlyphType(this,GLYPH_MARKER,"Marker",
			  {label : "label",
			   externalGraphic: externalGraphic,
			   pointRadius:this.getPointRadius(10),
			   rotation:0,
			   fontColor: this.getProperty("labelFontColor","#000"),
			   fontSize: this.getFontSize(),
			   fontFamily: this.getFontFamily(),
			   fontWeight: this.getFontWeight(),
			   fontStyle: this.getFontStyle(),
			   labelAlign: this.getProperty("labelAlign","cb"),
			   labelXOffset: this.getProperty("labelXOffset","0"),
			   labelYOffset: this.getProperty("labelYOffset","14"),
			   labelOutlineColor:this.getProperty("labelOutlineColor","#fff"),
			   labelOutlineWidth: this.getProperty("labelOutlineWidth","0"),

			   strokeWidth:0,
			   fillColor:'transparent',
			   labelSelect:true,
			  }, MyPoint,
			  {icon:ramaddaBaseUrl+"/icons/map/marker-blue.png"});

	    new GlyphType(this,GLYPH_POINT,"Point",
			  {strokeWidth:this.getProperty("strokeWidth",2), 
			   strokeDashstyle:'solid',
			   fillColor:"transparent",
			   fillOpacity:1,
			   strokeColor:this.getStrokeColor(),
			   strokeOpacity:1,
			   pointRadius:this.getPointRadius(4)},
			  MyPoint,
			  {icon:ramaddaBaseUrl+"/icons/dot.png"});
	    new GlyphType(this,GLYPH_FIXED,"Fixed Text", {
			      text:"",
			      right:"50%",
			      bottom:'5px',
			      left:'',
			      top:'',
			      borderWidth:1,
			      borderColor:"#ccc",
			      fillColor:"#fffeec",
			      fontColor:"#000",
			      fontSize:"12pt"			      
			      
			  },
			  MyEntryPoint,
			  {isFixed:true, tooltip:'Add fixed text',
			   icon:ramaddaBaseUrl+"/icons/sticky-note-text.png"});			    

	    new GlyphType(this,GLYPH_LINE, "Line",
			  {strokeColor:this.getStrokeColor(),
			   strokeWidth:this.getStrokeWidth(),
			   strokeOpacity:1,
			   strokeDashstyle:'solid',
			   strokeLinecap: 'butt',
			  },
			  MyPath,
			  {maxVertices:2,
			   icon:ramaddaBaseUrl+"/icons/line.png"});		
	    new GlyphType(this,GLYPH_POLYLINE, "Polyline",
			  {strokeColor:this.getStrokeColor(),
			   strokeWidth:this.getStrokeWidth(),
			   strokeOpacity:1,
			   strokeDashstyle:'solid',			      
			   strokeLinecap: 'butt'
},
			  MyPath,
			  {icon:ramaddaBaseUrl+"/icons/polyline.png"});
	    new GlyphType(this,GLYPH_POLYGON, "Polygon",
			  {strokeColor:this.getStrokeColor(),
			   strokeWidth:this.getStrokeWidth(),
			   strokeOpacity:1,
			   strokeDashstyle:'solid',			      
			   strokeLinecap: 'butt',
			   fillColor:"transparent",
			   fillOpacity:1.0},
			  MyPolygon,
			  {icon:ramaddaBaseUrl+"/icons/polygon.png"});

	    new GlyphType(this,GLYPH_FREEHAND,"Freehand",
			  {strokeColor:this.getStrokeColor(),
			   strokeWidth:this.getStrokeWidth(),
			   strokeOpacity:1,
			   strokeDashstyle:'solid',
			   strokeLinecap: 'butt'},
			  MyPath,
			  {freehand:true,icon:ramaddaBaseUrl+"/icons/freehand.png"});
	    new GlyphType(this,GLYPH_FREEHAND_CLOSED,"Closed",
			  {strokeColor:this.getStrokeColor(),
			   strokeWidth:this.getStrokeWidth(),
			   strokeOpacity:1,
			   strokeDashstyle:'solid',
			   strokeLinecap: 'butt',
			   fillColor:"transparent",
			   fillOpacity:1.0},
			  MyPolygon,
			  {freehand:true,icon:ramaddaBaseUrl+"/icons/freehandclosed.png"});

	    new GlyphType(this,GLYPH_BOX, "Box",
			  {strokeColor:this.getStrokeColor(),
			   strokeWidth:this.getStrokeWidth(),
			   strokeDashstyle:'solid',
			   strokeOpacity:1,
			   fillColor:"transparent",
			   fillOpacity:1.0},
			  MyRegularPolygon,
			  {snapAngle:90,sides:4,irregular:true,
			   icon:ramaddaBaseUrl+"/icons/rectangle.png"});
	    new GlyphType(this,GLYPH_CIRCLE, "Circle",
			  {strokeColor:this.getStrokeColor(),
			   strokeWidth:this.getStrokeWidth(),
			   strokeDashstyle:'solid',
			   strokeOpacity:1,
			   fillColor:"transparent",
			   fillOpacity:1.0},
			  MyRegularPolygon,
			  {snapAngle:45,sides:40,icon:ramaddaBaseUrl+"/icons/ellipse.png"});

	    new GlyphType(this,GLYPH_TRIANGLE, "Triangle",
			  {strokeColor:this.getStrokeColor(),
			   strokeWidth:this.getStrokeWidth(),
			   strokeDashstyle:'solid',
			   strokeOpacity:1,
			   fillColor:"transparent",
			   fillOpacity:1.0},
			  MyRegularPolygon,
			  {snapAngle:10,sides:3,
			   icon:ramaddaBaseUrl+"/icons/triangle.png"});				
	    new GlyphType(this,GLYPH_HEXAGON, "Hexagon",
			  {strokeColor:this.getStrokeColor(),
			   strokeWidth:this.getStrokeWidth(),
			   strokeDashstyle:'solid',
			   strokeOpacity:1,
			   fillColor:"transparent",
			   fillOpacity:1.0},
			  MyRegularPolygon,
			  {snapAngle:90,sides:6,
			   icon:ramaddaBaseUrl+"/icons/hexagon.png"});		


	    new GlyphType(this,GLYPH_MAP,"Map File",
			  {strokeColor:this.getStrokeColor(),
			   strokeWidth:this.getStrokeWidth(),
			   strokeDashstyle:'solid',			      
			   strokeOpacity:1,
			   fillColor:"transparent",
			   fillOpacity:1.0,
			   pointRadius:3,
			   externalGraphic:'',
			   fontColor: this.getProperty("labelFontColor","#000"),
			   fontSize: this.getFontSize(),
			   fontFamily: this.getFontFamily(),
			   fontWeight: this.getFontWeight(),
			   fontStyle: this.getFontStyle(),
			   labelAlign: 'cm',
			   labelXOffset: '0',
			   labelYOffset: '0',
			   labelOutlineColor:'#fff',
			   labelOutlineWidth: '0'},
			  MyEntryPoint,
			  {isMap:true,
			   tooltip:"Select a gpx, geojson or  shapefile map",
			   icon:ramaddaBaseUrl+"/icons/mapfile.png"});	


	    new GlyphType(this,GLYPH_MAPSERVER,"Map Server",
			  {
			      opacity:1.0,
			      legendUrl:""
			  },
			  MyEntryPoint,
			  {isMapServer:true,
			   tooltip:"Provide a Web Map Service URL",
			   icon:ramaddaBaseUrl+"/icons/drive-globe.png"});	


		new GlyphType(this,GLYPH_ROUTE, "Route", {
		    strokeColor:this.getStrokeColor(),
		    strokeWidth:this.getStrokeWidth(),
		    strokeDashstyle:'solid',
		    strokeOpacity:1,
		},MyRoute,{icon:ramaddaBaseUrl+"/icons/route.png"});

	    new GlyphType(this,GLYPH_IMAGE, "Image",
			  {strokeColor:"#ccc",
			   strokeWidth:1,
			   imageOpacity:this.getImageOpacity(1),
			   fillColor:"transparent"},
			  ImageHandler,
			  {tooltip:"Select an image entry to display",
			   snapAngle:90,sides:4,irregular:true,isImage:true,
			   icon:ramaddaBaseUrl+"/icons/imageicon.png"}
			 );
	    new GlyphType(this,GLYPH_ENTRY,"Entry Marker",
			  {externalGraphic: ramaddaBaseUrl +"/icons/entry.png",
			   pointRadius:12,
			   label:"label",
			   fontColor: this.getProperty("labelFontColor","#000"),
			   fontSize: this.getFontSize(),
			   fontFamily: this.getFontFamily(),
			   fontWeight: this.getFontWeight(),
			   fontStyle: this.getFontStyle(),
			   labelAlign: this.getProperty("labelAlign","cb"),
			   labelXOffset: this.getProperty("labelXOffset","0"),
			   labelYOffset: this.getProperty("labelYOffset","14"),
			   labelOutlineColor:this.getProperty("labelOutlineColor","#fff"),
			   labelOutlineWidth: this.getProperty("labelOutlineWidth","0")
			  },
			  MyEntryPoint,
			  {tooltip:"Add an entry as a marker",
			   isEntry:true,
			   icon:ramaddaBaseUrl+"/icons/entry.png"});
	    new GlyphType(this,GLYPH_MULTIENTRY,"Multi Entry",
			  {showLabels:true,
			   pointRadius:12,
			   fontColor: this.getProperty("labelFontColor","#000"),
			   fontSize: this.getFontSize(),
			   fontFamily: this.getFontFamily(),
			   fontWeight: this.getFontWeight(),
			   fontStyle: this.getFontStyle(),
			   labelAlign: this.getProperty("labelAlign","cb"),
			   labelXOffset: this.getProperty("labelXOffset","0"),
			   labelYOffset: this.getProperty("labelYOffset","14")
			   },
			  MyEntryPoint,
			  {tooltip:"Display children entries of selected entry",
			   isMultiEntry:true,
			   icon:ramaddaBaseUrl+"/icons/sitemap.png"});

	    new GlyphType(this,GLYPH_DATA,"Data",
			  {},
			  MyEntryPoint,
			  {isData:true, tooltip:'Select a map data entry to display',
			   icon:ramaddaBaseUrl+"/icons/chart.png"});


	},
	clearMessage2:function(time) {
	    this.jq(ID_MESSAGE2).hide(time);
	},
	showMessage2:function(msg,fadeOut) {
	    if(Utils.stringDefined(msg)) {
		this.jq(ID_MESSAGE2).html(msg);
		this.jq(ID_MESSAGE2).show();
	    }
	    if(fadeOut) {
		setTimeout(()=>{
		    this.clearMessage2(1000);
		},2000);
	    }
	},
	showCommandMessage:function(msg)  {
	    this.showMessage(msg,-1);
	},
	showMessage:function(msg,clearTime)  {
	    this.jq(ID_MESSAGE).html(msg);
	    this.jq(ID_MESSAGE).show();
	    if(this.messageErase) clearTimeout(this.messageErase);
	    this.messageErase = null;
	    if(clearTime>0 || !Utils.isDefined(clearTime)) {
		this.messageErase = setTimeout(()=>{
		    this.jq(ID_MESSAGE).hide();
		    this.jq(ID_MESSAGE).html('');
		},clearTime??3000);
	    }
	},
	showProgress:function(msg) {
	    this.showMessage(msg);
	},
	//Override base class method
	setErrorMessage: function(msg) {
	    this.showMessage(msg);
	},
	getCurrentLevel: function() {
	    return this.getMap().getZoom();
	},
	checkVisible:function() {
	    let features =[];
	    this.getGlyphs().forEach(mapGlyph=>{
		mapGlyph.checkVisible();
//		if(mapGlyph.getFilterable()) {
//		    features.push(...mapGlyph.getAllFeatures());
//		}
	    });
//	    MapUtils.gridFilter(this.getMap(), features);
	},
	initMap: function(map) {
	    SUPER.initMap.call(this)
	},
	
	getOtherProperties: function() {
	    if(!this.parsedMapProperties) {
		this.parsedMapProperties = Utils.parseMap(this?.mapProperties.otherProperties,"\n","=")??{};
	    }
	    return this.parsedMapProperties;
	},

	getMapProperty: function(name,dflt) {
	    if(this.mapProperties[name]) return this.mapProperties[name];
	    let value = this.getOtherProperties()[name];
	    
	    if(Utils.isDefined(value)) {
		if(value==="true") return true;
		if(value==="false") return false;		
		return value;
	    }
	    return  this.getProperty(name,dflt);
	},
	makeLegend: function() {
	    let _this = this;
	    let legendPosition = this.getMapProperty('legendPosition','left');
	    let legendContainer;
	    let leftLegend = this.jq(ID_LEGEND_LEFT);
	    let mapLegend = this.jq(ID_LEGEND_MAP_WRAPPER);	    
	    let rightLegend = this.jq(ID_LEGEND_RIGHT);
	    if(legendPosition=='left') {
		legendContainer=leftLegend;
		mapLegend.hide();
		rightLegend.hide();
	    } else   if(legendPosition=='right') {
		legendContainer=rightLegend;
		mapLegend.hide();
		leftLegend.hide();
	    } else   if(legendPosition=='map') {
		legendContainer = this.jq(ID_LEGEND_MAP);
		mapLegend.show();
		leftLegend.hide();
		rightLegend.hide();
	    } else {
		mapLegend.hide();
		leftLegend.hide();
		rightLegend.hide();
	    }

	    //Remove the old one
	    this.jq(ID_LEGEND).remove();
	    if(!legendContainer) return;

	    legendContainer.show();
	    legendContainer.html(HU.div(['id',this.domId(ID_LEGEND)],''));
	    let showShapes = this.getMapProperty('showShapes',true);
	    let legendWidth=parseInt(this.getMapProperty("legendWidth",250));
	    let legendLabel= this.getMapProperty("legendLabel","");
	    let html = '';
	    if(this.getMapProperty('showBaseMapSelect')) {
		html+=HU.div(['style','margin-bottom:4px;','class','imdv-legend-offset'], HU.b('Base Map: ') +this.getBaseLayersSelect());
	    }


	    let idToGlyph={};
	    let glyphs = this.getGlyphs();
	    if(this.getMapProperty('showAddress',false)) {
		this.jq(ID_ADDRESS).show();
	    } else {
		this.jq(ID_ADDRESS).hide();
	    }
	    glyphs.forEach((mapGlyph,idx)=>{
		html+=mapGlyph.makeLegend({idToGlyph:idToGlyph});
	    });

	    if(Utils.stringDefined(legendLabel)) {
		legendLabel=legendLabel.replace(/\\n/,'<br>');
		html = HU.div([],legendLabel)+html;
	    }
	    if(html!="") {
		let height= this.getProperty('height');
		let legendHeight= this.getProperty('legendHeight',height);		
		let css  = HU.css('max-width',legendWidth+'px','width',legendWidth+'px');
		if(height) css+=HU.css('height',legendHeight);
		let attrs = ['class','imdv-legend','style',css]
		html  = HU.div(attrs,html);
	    }
	    this.jq(ID_LEGEND).html(html);


	    HU.initToggleBlock(this.jq(ID_LEGEND),(id,visible,element)=>{
		let mapGlyph = idToGlyph[element.attr('map-glyph-id')];
		if(mapGlyph) mapGlyph.setLegendVisible(visible);
	    });


	    this.jq(ID_LEGEND).find('.imdv-legend-item-edit').click(function(event) {
		event.stopPropagation();
		let id = $(this).attr('glyphid');
		let mapGlyph = _this.findGlyph(id);
		if(!mapGlyph) return;
		_this.editFeatureProperties(mapGlyph);
	    });

	    this.jq(ID_LEGEND).find('.imdv-legend-item-view').click(function(event) {
		event.stopPropagation();
		let id = $(this).attr('glyphid');
		let mapGlyph = _this.findGlyph(id);
		if(!mapGlyph) return;
		mapGlyph.panMapTo(event.shiftKey);
	    });
	    


	    this.initBaseLayersSelect();
	    this.getGlyphs().forEach((mapGlyph,idx)=>{
		mapGlyph.initLegend();
	    });

	    let items = this.jq(ID_LEGEND).find('.imdv-legend-label');
	    this.initGlyphButtons(this.jq(ID_LEGEND));
	    items.tooltip({
		content: function () {
		    let title =  $(this).prop('title');
		    title=HU.div(['style','font-size:10pt;'],title);
		    return title;
		},
		show: {
		    delay: 1000,
		}
	    }	    );
	    items.click(function(event) {
		//If we have recently been dragging and dropping the glyphs then don't
		//handle the click
		if(_this.lastDroppedTime) {
		    let now = new Date();
		    if(now.getTime()-_this.lastDroppedTime.getTime()<2000) {
			return
		    }
		}
		let id = $(this).attr('glyphid');
		let mapGlyph = _this.findGlyph(id);
		if(!mapGlyph) return;
		if(event.shiftKey) {
		    _this.toggleSelectGlyph(mapGlyph);
		    return
		}
		if(event.altKey || event.metaKey) {
		    mapGlyph.panMapTo();
		    return;
		}
		mapGlyph.setVisible(!mapGlyph.getVisible(),true);
	    });
	},

	checkTopWiki:function() {
	    /*
	    if(this.canEdit())
		this.jq(ID_MAP_MENUBAR).show();
	    else
		this.jq(ID_MAP_MENUBAR).hide();	    
*/
	    let  wiki=(dom,text) =>{
		if(!Utils.stringDefined(text)) {
		    this.jq(dom).html('');
		} else {
		    this.wikify(text,null,wiki=>{
			this.jq(dom).html(wiki);
		    });
		    
		}
	    };

	    wiki("topwikitext",this.mapProperties?.topWikiText);
	    wiki("bottomwikitext",this.mapProperties?.bottomWikiText);	    


	},
	canEdit: function() {
	    //Is it set in the wiki tag?
	    let userCanEdit = this.getUserCanEdit(null);
	    if(Utils.isDefined(userCanEdit))
		return userCanEdit;
	    //Is logged in user
	    if(this.getProperty("canEdit")) return true;
	    return this.mapProperties?.userCanEdit;
	},

        initDisplay: function(embedded) {
	    let _this = this;
	    SUPER.initDisplay.call(this)
   
	    this.myLayer = this.map.createFeatureLayer('Annotation Features',false,null,{rendererOptions: {zIndexing: true}});
	    //For now don't have a separate selection layer?
//	    this.selectionLayer = this.map.createFeatureLayer('Selection',false,null,{rendererOptions: {zIndexing: true}});	    
	    this.selectionLayer = this.myLayer;
	    this.selectionLayer.setZIndex(1001)
	    this.myLayer.setZIndex(1000)	    
	    this.selectionLayer.canSelect = false;
	    if(this.getProperty('layerIndex')) {
		this.myLayer.ramaddaLayerIndex = +this.getProperty('layerIndex');
	    }
	    this.myLayer.ramaddaLayerIndex = 1001;
	    this.icon = '/icons/map/marker-blue.png';
	    this.glyphTypes=[];
	    this.glyphTypeMap = {};
	    this.doMakeMapGlyphs();
	    if(embedded) {
		return;
	    }

	    setTimeout(()=>{
		this.getMap().getMap().events.register('zoomend', '', () =>{
		    this.checkVisible();
		    $('.imdv-currentlevellabel').html('(current level: ' + this.getCurrentLevel()+')');

		},true);
	    },500);
	    this.getMap().featureClickHandler = e=>{
		let debug = false;
		let feature = e.feature;
		if(debug)
		    console.log('featureClick:' + feature);
		if(!feature) return;
		let mapGlyph = feature.mapGlyph || (feature.layer?feature.layer.mapGlyph:null);
		if(!mapGlyph) {
 		    if(debug)console.log('\tno mapGlyph');
		    return true;
		}
		if(mapGlyph.isMap()) {
 		    if(debug)console.log('\tis map');
		    return true;
		}
		if(this.command==ID_EDIT) {
 		    if(debug)console.log('\tdoing edit');
		    this.doEdit(feature.layer.mapGlyph);
		    return false;
		}

		if(this.command!=null) {
 		    if(debug)console.log('\tdoing command:' + this.command);
		    return false;
		}
		let showPopup = (html,props)=>{
		    this.getMap().lastClickTime  = new Date().getTime();
		    let id = HU.getUniqueId('div');
		    let div = HU.div(['id',id]);
		    let location = e.feature.geometry.getBounds().getCenterLonLat();
		    if(this.getMap().currentPopup) {
			this.getMap().onPopupClose();
		    }
		    if(e.event && e.event.xy)
			location = this.getMap().getMap().getLonLatFromViewPortPx(e.event.xy);
		    let popup =this.getMap().makePopup(location,div,props);
		    this.getMap().currentPopup = popup;
		    this.getMap().getMap().addPopup(popup);
		    jqid(id).html(html);
		    jqid(id).find('a').each(function() {
			$(this).click(function(){
			    let url = $(this).attr('href');
			    if(url)
				window.location=url;
			});

		    });
		}


		let doPopup = (html,props)=>{
 		    if(debug)console.log('\tdoPopup:'+ html)
		    let js =[];
		    //Parse out any script tags 
		    let regexp = /<script *src=("|')?([^ "']+)("|')?.*?<\/script>/g;
		    let array = [...html.matchAll(regexp)];
		    array.forEach(tuple=>{
			html = html.replace(tuple[0],'');
			let url = tuple[2];
			url = url.replace(/'/g,'');
			js.push(url);
		    });


		    //Run through any script tags and load them
		    //once done show the popup
		    let cb = ()=>{
			if(js.length==0 && js[0]==null) {
 			    if(debug)console.log('\tshowPopup:'+ html)
			    showPopup(html,props);

			    return;
			}
			let url = js[0];
			js.splice(0,1);
			Utils.loadScript(url,cb);
		    };
		    cb();
		};
		let text= mapGlyph.getPopupText()??'';
		if(mapGlyph.isEntry() || mapGlyph.isMultiEntry() || text.startsWith('<wiki>')) {
 		    if(debug)console.log('\twikifying')
		    let wiki = (text.startsWith('<wiki>')?text:mapGlyph.getWikiText())??'';
		    let width = '400';
		    let height='300';
		    let widthRegexp = /popupWidth *= *(\d+)/;
		    let widthMatch = wiki.match(widthRegexp);
		    if(widthMatch) {
			width=widthMatch[1];
			wiki = wiki.replace(widthRegexp,'');
		    }
		    let heightRegexp = /popupHeight *= *(\d+)/;		    
		    let heightMatch = wiki.match(heightRegexp);
		    if(heightMatch) {
			height=heightMatch[1];
			wiki = wiki.replace(heightRegexp,'');
		    }		    

		    if(!Utils.stringDefined(wiki))
			wiki = '{{mappopup}}';
		    let wikiCallback = html=>{
			html = mapGlyph.convertPopupText(html);
			html = HU.div(['style','max-height:300px;overflow-y:auto;'],html);
			doPopup(html,{width:this.getProperty('popupWidth',width),
				      height:this.getProperty('popupHeight',height)});
		    };
		    this.wikify(wiki,feature.entryId ?? mapGlyph.getEntryId(),wikiCallback);
		    return false;
		}

		if(!Utils.stringDefined(text)) {
 		    if(debug)console.log('\tno text')
		    return false;
		}
		text = mapGlyph.convertPopupText(text).replace(/\n/g,'<br>');
		doPopup(text);
		return false;
	    };

	    /*
	      Don't do this for now since if we have set the Show legend property=false that isn't set yet
	    legend = HU.toggleBlock('',legend,true,{orientation:'horizontal',
						    imgopen:'fa-solid fa-angles-down',
						    imgclosed:'fa-solid fa-angles-right',						    
						   });
	    */
	    let toggleResult = {};
	    let toggleListener = (id,vis)=>{
		this.mapProperties.mapLegendOpen = vis;
	    };
	    let innerDiv = HU.div(['id',this.domId(ID_LEGEND_MAP_WRAPPER),'class','imdv-legend-map-wrapper','style','display:none;background:#fff;z-index: 500;position:absolute;left:50px;top:15px;'],
				  HU.toggleBlock('Legend' + HU.space(2),HU.div(['id',this.domId(ID_LEGEND_MAP)]),
						 Utils.isDefined(this.mapProperties.mapLegendOpen)?this.mapProperties.mapLegendOpen:true,
						 {animated:300,listener:toggleListener},toggleResult));
	    this.mapLegendToggleId = toggleResult.id;
	    let inner = $(innerDiv);
	    this.jq(ID_MAP_CONTAINER).append(inner);
	    inner.draggable();
	    let legendLeft = HU.div(['id',this.domId(ID_LEGEND_LEFT),'style','display:none']);
	    this.jq(ID_LEFT).html(legendLeft);
	    let legendRight = HU.div(['id',this.domId(ID_LEGEND_RIGHT),'style','display:none']);
	    this.jq(ID_RIGHT).html(legendRight);	    

	    this.jq(ID_HEADER0).append(HU.div([ID,this.domId('topwikitext')]));
	    this.jq(ID_BOTTOM).append(HU.div([ID,this.domId('bottomwikitext')]));	    
	    let menuBar=  '';
	    [[ID_MENU_FILE,'File'],[ID_MENU_EDIT,'Edit'],[ID_MENU_NEW,'New']].forEach(t=>{
		menuBar+=   HU.div([ID,this.domId(t[0]),CLASS,'ramadda-menubar-button'],t[1])});
	    menuBar = HU.div([CLASS,'ramadda-menubar'], menuBar);
	    let message2 = HU.div([ID,this.domId(ID_MESSAGE2),CLASS,'ramadda-imdv-message2'],'');
	    this.jq(ID_MAP_CONTAINER).append(message2);
	    let message3 = HU.div([ID,this.domId(ID_MESSAGE3),CLASS,'ramadda-imdv-message3'],'');
	    if(this.getShowMapLegend()) {
		this.jq(ID_MAP_CONTAINER).append(message3);
	    }

	    let address =
		HU.span(['style',HU.css('position','relative')], 
			HU.div(['style','display:inline-block;','id',this.domId(ID_ADDRESS_CLEAR),'title','Clear','class','ramadda-clickable'],HU.getIconImage('fa-solid fa-xmark',[],['style','color:#ccc;'])) +
		' ' +
			HU.div(['id',this.domId(ID_ADDRESS_WAIT),'style',HU.css('position','absolute','right','0px',
										'display','inline-block','margin-right','2px','width','20px')])+
			HU.input('','',['id',this.domId(ID_ADDRESS_INPUT),'placeholder','Search for address','size','30']));

	    if(this.canEdit()) {
		address = address +' ' +HU.checkbox(this.domId(ID_ADDRESS_ADD),['id',this.domId(ID_ADDRESS_ADD),'title','Add marker to map'],false);
	    }

	    address = HU.div(['style',HU.css('white-space','nowrap','display','none','position','relative'),'id',this.domId(ID_ADDRESS)], address);	    
		
	    let message = HU.div([ID,this.domId(ID_MESSAGE),STYLE,HU.css('display','inline-block','white-space','nowrap','margin-left','10px')],'');
	    let mapHeader = HU.div([STYLE,HU.css('margin-left','10px'), ID,this.domId(ID_MAP)+'_header']);
	    if(this.canEdit()) {
		menuBar= HU.table(['id',this.domId(ID_MAP_MENUBAR),'width','100%'],HU.tr(['valign','bottom'],HU.td([],menuBar) +
											 HU.td(['width','50%'], message) +
											 HU.td(['align','right','style','padding-right:10px;','width','50%'],mapHeader+address)));
	    } else {
		menuBar= HU.table(['id',this.domId(ID_MAP_MENUBAR),'width','100%'],HU.tr(['valign','bottom'],HU.td([],'') +
											 HU.td(['width','50%'], message) +
											 HU.td(['align','right','style','padding-right:10px;','width','50%'],mapHeader+address)));
	    }
	    


	    this.jq(ID_TOP_LEFT).append(menuBar);
            this.jq(ID_ADDRESS_INPUT).keypress(function(event) {
                if (event.which == 13) {
		    let address = $(this).val();
		    if(!Utils.stringDefined(address)) return;
		    _this.gotoAddress($(this),address);
		}
	    });

	    this.jq(ID_ADDRESS_CLEAR).click(()=>{
		this.clearAddresses();
	    });

	    this.jq(ID_MENU_NEW).click(function() {
		_this.showNewMenu($(this));
	    });
	    this.jq(ID_MENU_FILE).click(function() {
		_this.showFileMenu($(this));
	    });
	    this.jq(ID_MENU_EDIT).click(function() {
		_this.showEditMenu($(this));
	    });


	    this.makeControls();	    

	    $(window).bind('beforeunload', ()=>{
		if(this.canEdit() && this.featureHasBeenChanged) {
		    return 'Changes have been made. Are you sure you want to leave?';
		}
	    });


	    let cmds = '';
	    this.jq(ID_COMMANDS).html(cmds);
	    this.jq(ID_MAP).mouseover(function(){
		$(this).focus();
	    });

	    if(this.getProperty('thisEntryType')=='geo_editable_json' || this.getProperty('thisEntryType')=='geo_imdv') {
		this.loadMap();
	    }
	},

	makeControls:function() {
	    let _this = this;
	    if(!this.canEdit()) return;
	    Utils.initDragAndDrop(this.jq(ID_MAP),
				  event=>{},
				  event=>{},
				  (event,item,result) =>{
				      let entryId = this.getProperty('entryId') || this.entryId;
				      Ramadda.handleDropEvent(event, item, result, entryId,(data,entryid, name,isImage)=>{
					  if(MAP_TYPES.includes(data.type)) {
					      let glyphType = this.getGlyphType(GLYPH_MAP);
					      let style = $.extend({},glyphType.getStyle());
					      let attrs = {
						  entryId:data.entryid,
						  type:glyphType.type,
						  name:data.name,
						  entryType:data.type,
					      }
					      let mapGlyph = this.handleNewFeature(null,style,attrs);
					      mapGlyph.checkMapLayer();
					      return;
					  } else {
					      this.setCommand('image',{url:data.geturl});
					  }
				      });
				  },
				  (file)=>{
				      if(file.type.match('image.*')) return true;
				      if(file.name.match('.*\.(geojson|gpx|zip|kml|kmz)')) return true;
				      return false;

				  }
				 );

	    this.jq(ID_MAP).css('caret-color','transparent');


	    //		this.jq(ID_LEFT).html(HU.div([ID,this.domId(ID_COMMANDS),CLASS,'imdv-commands']));
	    let keyboardControl = new OpenLayers.Control();
	    let control = new OpenLayers.Control();
	    let callbacks = {
		keydown: function(event) {
		    if(event.key=='MediaTrackPrevious') return;
//		    console.log('key down:' + event.key);
		    HtmlUtils.hidePopupObject();
		    if(event.key=='Escape') {
			_this.clearCommands();
			_this.unselectAll();
			return;
		    }
		    if(event.key=='Backspace') {
			_this.doCut();
			return;
		    }			
		    if(!event.ctrlKey) return;
		    switch(event.key) {
		    case 'a': 
			_this.selectAll();
			break;
		    case 'p':
			_this.handleEditEvent();
			break;
		    case 'x': 
			_this.doCut();
			break;
		    case 'v': 
			if(!_this.clipboard ||  _this.clipboard.length==0) {
			    return;
			}
			_this.doPaste(event);
			break;
		    case 'c': 
			_this.doCopy();
			break;
		    case 'f': 
			_this.changeOrder(true);
			break;
		    case 'b': 
			_this.changeOrder(false);
			break;			    			    
		    case 'm':
			_this.setCommand(ID_MOVER);
			break;
		    case 'l':
			_this.listFeatures();
			break;
		    case 's': 
			_this.doSave();
			break;			    
		    case 'e': 
			_this.doEdit();
			break;
		    default:
			return;
		    }
		    event.preventDefault();
		}};
	    let options = {};
	    let handler = new OpenLayers.Handler.Keyboard(control, callbacks, options);
	    handler.activate();
	    this.getMap().getMap().addControl(keyboardControl);
	    this.addControl(ID_SELECTOR,"Click-drag to select",
			    this.featureSelector = new OpenLayers.Control.SelectFeature(this.myLayer, {
				select: function(feature) {
				    if(this.isShiftKey() && feature.mapGlyph.isSelected()) {
					_this.unselectGlyph(feature.mapGlyph);
					return;
				    }
				    _this.selectGlyph(feature.mapGlyph);
				},
				selectBox: function(position) {
				    this.checkEvent();
				    OpenLayers.Control.SelectFeature.prototype.selectBox.apply(this,arguments);
				},
				isShiftKey:function() {
				    let event = this?.handlers?.feature?.evt;
				    if(!event) return false;
				    return event.shiftKey || event.metaKey;
				},
				checkEvent: function() {
				    if(!this.isShiftKey()) {
					_this.unselectAll();
				    }
				},
				selectStyle: {
				    pointRadius:this.getPointRadius(),
				    strokeWidth:2,
				    fillOpacity: 0.5,
				    fillColor: "blue",
				    cursor: "pointer"
				},
				clickout: true,
				toggle: true,
				multiple: true, 
				hover: false,
				toggleKey: "ctrlKey", // ctrl key removes from selection
				multipleKey: "shiftKey", // shift key adds to selection
				box: true
			    }));

	    this.addControl(ID_EDIT,"Click to edit properties",new OpenLayers.Control.SelectFeature(this.myLayer, {
		onSelect: function(feature) {
		    _this.doEdit(feature.mapGlyph);
		},
		clickout: true,
		toggle: true,
		multiple: false, 
		hover: false,
		toggleKey: "ctrlKey", // ctrl key removes from selection
		multipleKey: "shiftKey", // shift key adds to selection
		box: false
	    }));



	    let imageChecker = feature=>{
		if(feature.mapGlyph) {
		    feature.mapGlyph.checkImage(feature);
		}
		_this.featureChanged();
	    };
	    let mover =  this.addControl(ID_MOVER,"Click drag to move",new OpenLayers.Control.DragFeature(this.myLayer,{
		moveFeature: function(pixel) {
		    let mapGlyph = this.feature.mapGlyph;
		    if(!mapGlyph) {
			console.log('no map glyph');
			return;
		    }
		    let selected = _this.getSelected();
		    if(selected.length==0) {
			selected = [mapGlyph];
		    } else if(!selected.includes(mapGlyph)) {
			selected.push(mapGlyph);
		    }
		    let res = this.map.getResolution();
		    let dx = res * (pixel.x - this.lastPixel.x);
		    let dy = res * (this.lastPixel.y - pixel.y);
		    selected.forEach(mapGlyph=>{
			mapGlyph.move(dx,dy);
		    });
		    this.lastPixel = pixel;
		},
		onDrag: function(feature, pixel) {
		}
	    }));

	    let MyMover =  OpenLayers.Class(OpenLayers.Control.ModifyFeature, {
		dragComplete: function() {
		    OpenLayers.Control.ModifyFeature.prototype.dragComplete.apply(this, arguments);
		    this.theDisplay.featureChanged();	    
		    this.theDisplay.clearMessage2(1000);
		},
		dragVertex: function(vertex, pixel) {
		    this.theDisplay.checkSelected(this.feature.mapGlyph);
		    this.theDisplay.showDistances(this.feature.geometry,this.feature.type);
		    if(!this.feature.image && this.feature.type!=GLYPH_BOX && !this.feature?.mapGlyph.isImage()) {
			OpenLayers.Control.ModifyFeature.prototype.dragVertex.apply(this, arguments);
			return;
		    }
		    let v  = this.feature.geometry.getVertices();
		    let p  = vertex.geometry.getVertices()[0];
		    let index = -1;
		    v.every((v,idx)=>{
			if(v.x==p.x && v.y == p.y) {
			    index = idx;
			    return false;
			}
			return true
		    });
		    
		    let pos = this.map.getLonLatFromViewPortPx(pixel);
		    let geom = vertex.geometry;
		    geom.move(pos.lon - geom.x, pos.lat - geom.y);
		    p  = vertex.geometry.getVertices()[0];
		    if(index==0) {
			//nw
			v[3].x = p.x;
			v[1].y = p.y;			    
		    } else 	if(index==1) {
			//ne
			v[2].x = p.x;
			v[0].y = p.y;
		    } else 	if(index==2) {
			//se
			v[1].x = p.x;
			v[3].y = p.y;			    
		    } else 	if(index==3) {
			//sw
			v[0].x = p.x;
			v[2].y = p.y;
		    }
		    this.feature.geometry.clearBounds();
		    this.layer.drawFeature(this.feature, this.standalone ? undefined :
					   'select');
		    this.layer.drawFeature(vertex);
		    if(this.feature.mapGlyph) {
			imageChecker(this.feature);
		    }
		}
	    });

	    let resizer = new MyMover(this.myLayer,{
		theDisplay:this,
		onDrag: function(feature, pixel) {
		    imageChecker(feature);},
		mode:OpenLayers.Control.ModifyFeature.RESIZE|OpenLayers.Control.ModifyFeature.DRAG});

	    let reshaper = new MyMover(this.myLayer, {
		theDisplay:this,
		onDrag: function(feature, pixel) {
		    console.log('on drag');
		    imageChecker(feature);},
		createVertices:false,
		mode:OpenLayers.Control.ModifyFeature.RESHAPE});
	    let rotator = new MyMover(this.myLayer, {
		theDisplay:this,
		onDrag: function(feature, pixel) {
		    imageChecker(feature);},
		createVertices:false,
		mode:OpenLayers.Control.ModifyFeature.ROTATE});		
	    this.addControl(ID_RESIZE,"Click to resize",resizer);
	    this.addControl(ID_RESHAPE,"Click to reshape",reshaper);
	    this.addControl(ID_ROTATE,"Click to rotate",rotator);		

	    this.glyphTypes.forEach(g=>{
		this.glyphTypeMap[g.type]  = g;
		g.createDrawer();
	    });

	    
	}
    });
}



var MapObject = function(display, glyphType,feature) {
    this.id = HU.getUniqueId("");
    feature.objectId = this.id;
    this.display = display;
    this.feature = feature;
}

MapObject.prototype = {
    getId:function() {
	return this.id;
    }
}

var GlyphType = function(display,type,name,style,handler,options) {
    this.display = display;
    this.name = name;
    this.type = type;
    this.glyphStyle = style;
    this.handler = handler;
    this.options = options || {};
    this.options.glyphType = type;
    this.options.display = display;
    this.options.mapGlyph = this;
    display.addGlyphType(this);
};

GlyphType.prototype = {
    getName: function() {
	return this.name;
    },
    getTooltip: function() {
	return this.options.tooltip;
    },
    getIcon:function() {
	return this.options.icon;
    },
    getStyle:function() {
	return this.glyphStyle;
    },
    getType:  function() {
	return this.type;
    },
    isLabel:  function() {
	return this.getStyle().label!=null;
    },
    isImage:  function() {
	return this.options.isImage;
    },
    isEntry:  function() {
	return this.options.isEntry;
    },
    isMultiEntry:  function() {
	return this.options.isMultiEntry;
    },    
    isData:  function() {
	return this.options.isData;
    },
    isFixed:  function() {
	return this.options.isFixed;
    },
    isGroup:  function() {
	return this.options.isGroup;
    },	        
    isMap:  function() {
	return this.options.isMap;
    },
    isMapServer:  function() {
	return this.options.isMapServer;
    },			    
    isRoute: function() {
	return this.type == GLYPH_ROUTE;
    },
    isIcon:  function() {
	return this.getStyle().externalGraphic!=null;
    },	
    applyStyle: function(style,forAll) {
	for(a in style) {
	    if(forAll && this.type ==GLYPH_LABEL) {
		//		    if(a=="pointRadius") continue;
		if(a=="strokeColor") continue;
		if(a=="fillColor") continue;		    
	    }
	    if(Utils.isDefined(this.getStyle()[a])) this.getStyle()[a] = style[a];
	}
    },
    newFeature: function(feature) {
	let glyph = new MapObject(this.display,this.type, feature);
    },
    createDrawer:function() {
	let _this = this;
	let layer = this.display.myLayer;
	let Drawer = OpenLayers.Class(OpenLayers.Control.DrawFeature, {
	    initialize: function(layer, options) {
		let defaultStyle = $.extend({}, MapUtils.getVectorStyle("default"));
		defaultStyle={};
		$.extend(defaultStyle, _this.getStyle());		    
		let styleMap = MapUtils.createStyleMap({"default":defaultStyle});
		options = {
		    handlerOptions:{
			style: defaultStyle,
			layerOptions:{
			    styleMap:styleMap
			}
		    }
		};
		$.extend(options.handlerOptions, _this.options);
		OpenLayers.Control.DrawFeature.prototype.initialize.apply(
		    this, [layer, _this.handler||OpenLayers.Handler.Point, options]
		);
	    },
	    drawFeature: function(geometry) {
		OpenLayers.Control.DrawFeature.prototype.drawFeature.apply(this, arguments);
		let feature =this.layer.features[this.layer.features.length-1];
		if(this.handler.theImage) {
		    feature.image = this.handler.theImage;
		}
		feature.type = _this.type;
		
		let newStyle;
		if(this.handler.style) {
		    newStyle=this.handler.style;
		}
		if(newStyle) {
		    for(a in _this.getStyle()) {
			if(!Utils.isDefined(newStyle[a])) {
			    newStyle[a] = _this.getStyle()[a];
			}
		    }
		    if(feature.style && feature.style.label)
			newStyle.label = feature.style.label;
		    let tmp = {};
		    $.extend(tmp, newStyle);
		    feature.style=tmp;
		}
		ImdvUtils.scheduleRedraw(this.layer);
		_this.newFeature(feature);
	    }
	});
	this.drawer = new Drawer(layer);
	this.display.addControl(this.type,"",this.drawer);
	return this.drawer;
    },
};



function MapGlyph(display,type,attrs,feature,style) {
    this.transientProperties = {};

    let glyphType = display.getGlyphType(type);
    if(attrs.routeProvider)
	this.name = "Route: " + attrs.routeProvider +" - " + attrs.routeType;
    else 
	this.name = attrs.name || glyphType.getName() || type;
    let mapGlyphs = attrs.mapglyphs;
    if(attrs.mapglyphs) delete attrs.mapglyphs;
    if(mapGlyphs){
	mapGlyphs = mapGlyphs.replace(/\\n/g,"\n");
	this.putTransientProperty("mapglyphs", mapGlyphs);
    }


    this.display = display;
    this.type = type;
    this.features = [];
    this.attrs = attrs;
    this.style = style??{};
    this.id = attrs.id ?? HU.getUniqueId("glyph_");
    if(feature) this.addFeature(feature);

    if(this.isEntry()) {
	if(!Utils.isDefined(this.attrs.useentryname))
	    this.attrs.useentryname = true;
	if(!Utils.isDefined(this.attrs.useentrylabel))
	    this.attrs.useentrylabel = true;	
    }
}


var ID_MAPFILTERS = 'mapfilters';
var ID_MAPLEGEND = 'maplegend';


MapGlyph.prototype = {
    CLASS_NAME:'MapGlyph',
    animationInfo:{},
    domId:function(id) {
	return this.getId() +'_'+this.display.domId(id);
    },
    jq:function(id) {
	return jqid(this.domId(id));
    },
    getFixedId: function() {
	return this.domId('_fixed');
    },
    getElevations:async function(points,callback,update) {
	let elevations = points.map(()=>{return 0;});
	let ok = true;
	let count=0;
	for(let i=0;i<points.length;i++) {
	    if(!ok) break;
	    let point = points[i];
	    let url = "https://nationalmap.gov/epqs/pqs.php?x="
		+ point.x + "&y=" + point.y
                + "&units=feet&output=json";
//	    console.log('url:'+ url);
            await  $.getJSON(url, (data)=> {
		let elevation = data?.USGS_Elevation_Point_Query_Service?.Elevation_Query?.Elevation;
		elevations[i]= elevation;
		count++;
		if(update)
		    ok = update(count,points.length);
//		console.log("elevation #" + elevations.length+"/" + points.length+": " + elevation);
	    }).fail((data)=>{
		console.log('Failed to find elevation');
		console.dir(data);
		elevations.push(NaN);
	    });
	}
	callback(elevations,ok);
    },
    getIcon: function() {
	return this.attrs.icon??this.display.getGlyphType(this.getType()).getIcon();
    },
    putTransientProperty(name,value) {
	this.transientProperties[name] = value;
    },
    getTransientProperty(name) {
	return this.transientProperties[name];
    },    
    clone: function() {
	let style = $.extend({},this.style);
	let attrs = $.extend({},this.attrs);
	let cloned =  new MapGlyph(this.display, this.type,attrs,null,style);
	let features = this.features.map(f=>{
	    f = f.clone();
	    f.layer=this.display.myLayer;
	    f.mapGlyph = cloned;
	    return f;
	});
	cloned.features=features;
	this.display.addFeatures(features);
	return cloned;
    },
    makeJson:function() {
	let attrs=this.getAttributes();
	let obj = {
	    mapOptions:attrs,
	    id:this.getId()
	};
	let style = this.getStyle();
	if(this.getMapLayer()) {
	    style = this.getMapLayer().style||style;
	}
	if(style) {
	    style = $.extend({},style);
	    if(this.getImage() && Utils.isDefined(this.getImage().opacity)) {
		style.imageOpacity=this.getImage().opacity;
		style.strokeColor="transparent";
	    }
	    obj.style = style;
	}
	let geom = this.getGeometry();
	if(geom) {
	    obj.geometryType=geom.CLASS_NAME;
	    let points = obj.points=[];
	    let vertices  = geom.getVertices();
	    let  p =d=>{
		return Utils.trimDecimals(d,6);
	    };
	    if(this.getImage()) {
		let b = this.getMap().transformProjBounds(geom.getBounds());
		points.push(p(b.top),p(b.left),
			    p(b.top),p(b.right),
			    p(b.bottom),p(b.right),
			    p(b.bottom),p(b.left));				    
	    } else {
		vertices.forEach(vertex=>{
		    let pt = vertex.clone().transform(this.getMap().sourceProjection, this.getMap().displayProjection);
		    points.push(p(pt.y),p(pt.x));
		});
	    }
	}
	if(this.children) {
	    let childrenJson=[];
	    this.children.forEach(child=>{
		childrenJson.push(child.makeJson());
	    });
	    obj.children = childrenJson;
	}
	return obj;
    },	


    isMultiEntry:  function() {
	return this.type == GLYPH_MULTIENTRY;
    },
    getEntryGlyphs:function(checkTransient) {
	if(Utils.stringDefined(this.attrs.entryglyphs))
	    return this.attrs.entryglyphs;
	if(checkTransient)
	    return this.transientProperties.mapglyphs;
	return null;
    },
    addToPropertiesDialog:function(content,style) {
	let html='';
	let layout = (lbl,widget)=>{
	    html+=HU.b(lbl)+'<br>'+widget+'<br>';
	}
	let nameWidget = HU.input('',this.getName(),['id',this.domId('mapglyphname'),'size','40']);
	if(this.isEntry()) {
	    nameWidget+='<br>' +HU.checkbox(this.domId('useentryname'),[],this.getUseEntryName(),'Use name from entry');
	    nameWidget+=HU.space(3) +HU.checkbox(this.domId('useentrylocation'),[],this.getUseEntryLocation(),'Use location from entry');
	}
	html+=HU.b('Name: ') +nameWidget+'<br>';
	if(this.isEntry()) {
	    layout('Glyphs:</b> <a target=_help href=https://ramadda.org/repository/userguide/imdv.html#glyphs>Help</a><b>',
		   HU.textarea('',this.getEntryGlyphs()??'',[ID,this.domId('entryglyphs'),'rows',5,'cols', 90]));
	    /*
	      glyph1='type:gauge,color:red,pos:sw,width:50,height:50,dx:20,dy:-30,sizeBy:atmos_temp,sizeByMin:0,sizeByMax:100'
	      glyph2='type:label,pos:sw,dx:25,dy:0,label:${atmos_temp}'
	    */
	}

	let level = this.getVisibleLevelRange()??{};
	html+= HU.checkbox(this.domId('visible'),[],this.getVisible(),'Visible')+'<br>';


	html+=this.display.getLevelRangeWidget(level,this.getShowMarkerWhenNotVisible());
	
	/*
	let elevButtons = [];
	if(this.isOpenLine()|| this.mapLayer) {
	    elevButtons.push(HU.span(['style','margin-top:4px;','id',this.domId('makeelevations'),'class','ramadda-clickable'],'Add elevations'));
	    elevButtons.push(HU.span(['style','margin-top:4px;','id',this.domId('clearelevations'),'class','ramadda-clickable'],'Clear elevations'));
	    elevButtons.push(HU.span(['id',this.domId('elevationslabel')],''));	    
	}

	if(elevButtons.length) {
	    html+= Utils.wrap(elevButtons,HU.open('span',['style',HU.css('margin-right','8px')]),'</span>');
	}
*/

	let domId = this.display.domId('glyphedit_popupText');
	let featureInfo = this.getFeatureInfo();
	let lines = ['${default}'];
	lines = Utils.mergeLists(['default'],featureInfo.map(info=>{return info.id;}));

	let propsHelp =this.display.makeSideHelp(lines,domId,{prefix:'${',suffix:'}'});
	html+=HU.leftRightTable(HU.b('Popup Text:'),
				this.getHelp('#popuptext'));
	let help = 'Add macro:'+ HU.div(['class','imdv-side-help'],propsHelp);
	html+= HU.hbox([HU.textarea('',style.popupText??'',[ID,domId,'rows',4,'cols', 40]),HU.space(2),help]);

	html+=HU.b('Legend Text:') +'<br>' +
	    HU.textarea('',this.attrs.legendText??'',
			[ID,this.domId('legendtext'),'rows',4,'cols', 40]);
	
	if(this.isMultiEntry()) {
	    html+='<br>';
	    html+= HU.checkbox(this.domId('showmultidata'),[],this.getShowMultiData(),'Show entry data');
	}

	content.push({header:'Properties',contents:html});

	html=  this.getHelp('#miscproperties')+'<br>';
	let miscLines =['filter.show=false',
			'filter.zoomonchange.show=false','filter.toggle.show=false'];
	this.getFeatureInfo().forEach(info=>{
	    miscLines.push({line:info.id+'.show=true',title:info.property});
	    miscLines.push({line:info.id+'.label=',title:info.property});	    
	});

	let miscHelp =this.display.makeSideHelp(miscLines,this.domId('miscproperties'),{suffix:'\n'});
	let ex = 'Add property:' + miscHelp;

	html += HU.hbox([HU.textarea('',this.attrs.properties??'',[ID,this.domId('miscproperties'),'rows',6,'cols', 40]),
			 HU.space(2),ex]);
	content.push({header:'Settings',contents:html});


    },
    addElevations:async function(update,done) {
	let pts;
	if(this.mapLayer) {
	    pts = [];
	    let features= this.mapLayer.features;
	    features.forEach((feature,idx)=>{
		let pt = feature.geometry.getCentroid();
		pts.push(this.display.getMap().transformProjPoint(pt))
	    });
	    console.dir(pts);
	} else {
	    pts = this.display.getLatLonPoints(this.getGeometry());
	}
	let callback = (points)=>{
	    this.attrs.elevations = points;
	    this.features[0].elevations = points;
	    done();
	};
	await this.getElevations(pts,callback,update);
    },


    applyPropertiesDialog: function() {

	if(this.isMultiEntry()) {
	    this.setShowMultiData(this.jq("showmultidata").is(':checked'));
	}
	//Make sure we do this after we set the above style properties
	this.setName(this.jq("mapglyphname").val());
	this.attrs.legendText = this.jq('legendtext').val();
	if(this.isEntry()) {
	    this.setUseEntryName(this.jq("useentryname").is(":checked"));
	    this.setUseEntryLabel(this.jq("useentrylabel").is(":checked"));
	    this.setUseEntryLocation(this.jq("useentrylocation").is(":checked"));
	    let glyphs = this.jq("entryglyphs").val();
	    this.setEntryGlyphs(glyphs);
	    this.applyEntryGlyphs();
	}
	

	this.setVisible(this.jq("visible").is(":checked"),true);
	this.parsedProperties = null;
	this.attrs.properties = this.jq('miscproperties').val();
	this.setVisibleLevelRange(this.display.jq("minlevel").val().trim(),
				      this.display.jq("maxlevel").val().trim());
	this.setShowMarkerWhenNotVisible(this.display.jq('showmarkerwhennotvisible').is(':checked'));
    },
    featureSelected:function(feature,layer,event) {
//	console.log('imdv.featureSelected');
	if(this.selectedStyleGroup) {
	    let indices = this.selectedStyleGroup.indices;
//	    console.log('\thave a selectedStyleGroup');
	    if(indices.includes(feature.featureIndex)) {
		this.selectedStyleGroup.indices = Utils.removeItem(indices,feature.featureIndex);
		feature.style =  feature.originalStyle = null;
//		console.log("removing selected:" + feature.featureIndex,indices);
	    } else {
		this.getStyleGroups().forEach((group,idx)=>{
		    group.indices = Utils.removeItem(group.indices,feature.featureIndex);
		});
		feature.style = feature.originalStyle = $.extend(feature.style??{},this.selectedStyleGroup.style);
		indices.push(feature.featureIndex);
//		console.log("adding selected:" + feature.featureIndex,indices);
	    }
	    ImdvUtils.scheduleRedraw(layer,feature);
	    this.display.featureChanged(true);	    
	    return
	}
//	console.log('\tcalling onFeatureSelect');
	this.display.getMap().onFeatureSelect(feature.layer,event)
    },
    featureUnselected:function(feature,layer,event) {
//	this.display.getMap().onFeatureSelect(feature.layer,event)
    },
    applyEntryGlyphs:function(args) {
	if(!Utils.stringDefined(this.getEntryGlyphs(true))) {
	    return;
	}

	let opts = {
	    entryId:this.attrs.entryId
	};

	if(args) {
	    $.extend(opts,args);
	}

	let glyphs = [];
	this.getEntryGlyphs(true).trim().split("\n").forEach(line=>{
	    line = line.trim();
	    if(line.startsWith("#") || line == "") return;
	    glyphs.push(line);
	});
	if(glyphs.length==0) {
	    console.log("\tno glyphs-2");
	    return;
	}
	let url = ramaddaBaseUrl + "/entry/data?record.last=1&max=1&entryid=" + opts.entryId;
	let pointData = new PointData("",  null,null,url,
				      {entryId:opts.entryId});
	let callback = (data)=>{
	    this.makeGlyphs(pointData,data,glyphs);
	    this.display.clearFeatureChanged();
	}
	let fauxDisplay  = {
	    display:this.display,
	    type: "map glyph proxy",
	    getId() {
		return "ID";
	    },
	    pointDataLoaded:function(data,url) {
		callback(data);

	    },
            pointDataLoadFailed:function(err){
		this.display.pointDataLoadFailed(err);
	    },
	    applyRequestProperties:function(props) {
	    },
	    handleLog:function(err) {
		this.display.handleLog(err);
	    },
	    displayError:function(err) {
		console.log("Error:" + err);
	    }
	    
	}
	pointData.loadData(fauxDisplay,null);
    },
    makeGlyphs: function(pointData,data,glyphLines) {
	let glyphs = [];
	let lines=[];
	let canvasWidth=100;
	let canvasHeight=100;
	let widthRegexp = /width *= *([0-9]+)/;
	let heightRegexp = /height *= *([0-9]+)/;
	let fillRegexp = /fill *= *(.+)/;
	let borderRegexp = /border *= *(.+)/;
	let fontRegexp = /font *= *(.+)/;				
	let sizeRegexp = /size *= *(.+)/;
	let fontSizeRegexp = /fontSize *= *(.+)/;					
	let fill;
	let border;	
	let font;
	let size;
	let fontSize;		

	glyphLines.forEach(line=>{
	    line = line.trim();
	    let skip = true;
	    line.split(";").forEach(line2=>{
		line2 = line2.trim();
		let match;
		if(match  = line2.match(widthRegexp)) {
		    canvasWidth=parseFloat(match[1]);
		    return;
		}
		
		if(match  = line2.match(heightRegexp)) {
		    canvasHeight=parseFloat(match[1]);
		    return;
		}
		if(match  = line2.match(fillRegexp)) {
		    fill=match[1];
		    return;
		}
		if(match  = line2.match(sizeRegexp)) {
		    size=match[1];
		    return;
		}		
		if(match  = line2.match(fontSizeRegexp)) {
		    fontSize=match[1];
		    return;
		}		
		if(match  = line2.match(borderRegexp)) {
		    border=match[1];
		    return;
		}	    	    	    
		if(match  = line2.match(fontRegexp)) {
		    font = match[1];
		    return;
		}
		skip=false;
	    })
	    if(!skip)
		lines.push(line);
	});


	lines.forEach(line=>{
	    glyphs.push(new Glyph(this.display,1.0, data.getRecordFields(),data.getRecords(),{
		canvasWidth:canvasWidth,
		canvasHeight: canvasHeight,
		entryname: this.getName(),
		font:font
	    },line));
	});
	let cid = HU.getUniqueId("canvas_");
	let c = HU.tag("canvas",[CLASS,"", STYLE,"xdisplay:none;", 	
				 WIDTH,canvasWidth,HEIGHT,canvasHeight,ID,cid]);

	$(document.body).append(c);
	let canvas = document.getElementById(cid);
	let ctx = canvas.getContext("2d");
	if(fill) {
	    ctx.fillStyle=fill;
	    ctx.fillRect(0,0,canvasWidth,canvasHeight);
	}
	if(border) {
	    ctx.strokeStyle = border;
	    ctx.strokeRect(0,0,canvasWidth,canvasHeight);
	}
	ctx.strokeStyle ="#000";
	ctx.fillStyle="#000";
	let pending = [];
	let records = data.getRecords();
	glyphs.forEach(glyph=>{
	    //if its an image glyph then the image might not be loaded so the call returns a
	    //isReady function that we keep checking until it is ready
	    let isReady =  glyph.draw({}, canvas, ctx, 0,canvasHeight,{record:records[records.length-1]});
	    if(isReady) pending.push(isReady);
	});

	let finish = ()=>{
	    let img = canvas.toDataURL();
	    if($('#testimg').length) 
		$("#testimg").html(HU.tag("img",["src",img]));
	    canvas.remove();
	    if(fontSize) {
		this.style.fontSize=fontSize;
	    }
	    
	    if(size) {
		this.style.pointRadius=size;
	    }
	    this.style.externalGraphic=img;
	    this.applyStyle(this.style);		
	    this.display.redraw();
	};


	let check = () =>{
	    let allGood = true;
	    pending.every(p=>{
		if(!p()) {
		    allGood=false;
		    return false;
		}
		return true;
	    });
	    if(allGood) {
		finish();
	    }  else {
		setTimeout(check,100);
	    }
	};
	check();
    },

    setDownloadUrl:function(url) {
	this.downloadUrl =url;
    },
    setEntryGlyphs:function(v) {
	this.attrs.entryglyphs = v;
	return this;
    },
    getUseEntryLocation: function() {
	return this.attrs.useentrylocation;
    },
    setUseEntryLocation: function(v) {
	this.attrs.useentrylocation = v;
	return this;
    },
    getUseEntryName: function() {
	return this.attrs.useentryname;
    },
    setUseEntryName: function(v) {
	this.attrs.useentryname=v;
	return this;
    },
    getUseEntryLabel: function() {
	return this.attrs.useentrylabel;
    },    
    setUseEntryLabel: function(v) {
	this.attrs.useentrylabel =v;
	return this;
    },
    showMultiEntries:function() {
	let _this = this;
	if(!this.entries) return;
	let html = '';
	let map = {};
	this.entries.forEach(entry=>{
	    map[entry.getId()] = entry;
	    let link = entry.getLink(null,true,['target','_entry']);
	    link = HU.div(['style','white-space:nowrap;max-width:180px;overflow-x:hidden;','title',entry.getName()], link);
	    let add = '';
	    if(MAP_TYPES.includes(entry.getType().getId())) {
		add = HU.span(['class','ramadda-clickable','title','add map','entryid',entry.getId(),'command',GLYPH_MAP],HU.getIconImage('fas fa-plus'));
	    } else if(entry.isPoint) {
		add = HU.span(['class','ramadda-clickable','title','add data','entryid',entry.getId(),'command',GLYPH_DATA],HU.getIconImage('fas fa-plus'));
	    } else if(entry.isGroup) {
		add = HU.span(['class','ramadda-clickable','title','add multi entry','entryid',entry.getId(),'command',GLYPH_MULTIENTRY],HU.getIconImage('fas fa-plus'));
	    } else {
	    }		
	    if(add!='') {
		link = HU.leftRightTable(link,add);
	    }

	    html+=HU.div(['style',HU.css('white-space','nowrap')],link);
	});
	if(html!='') {
	    html = HU.div(['class','ramadda-cleanscroll', 'style','max-height:200px;overflow-y:auto;'], HU.div(['style','margin-right:10px;'],html));
	    this.jq('multientry').html(HU.b('Entries')+html);
	}
	this.jq('multientry').find('[command]').click(function(){
	    let command = $(this).attr('command');
	    let entry = map[$(this).attr('entryid')];
	    let glyphType = _this.display.getGlyphType(command);
	    let style = $.extend({},glyphType.getStyle());
	    let mapOptions = {
		type:command,
		entryType: entry.getType().getId(),
		entryId:entry.getId(),
		name:entry.getName(),
		icon:entry.getIconUrl()
	    }
	    if(command===GLYPH_MAP) {
		let mapGlyph = _this.display.handleNewFeature(null,style,mapOptions);
		mapGlyph.checkMapLayer();
	    } else if(command==GLYPH_DATA) {
		_this.display.createData(mapOptions);
	    } else if(command==GLYPH_MULTIENTRY) {
		let mapGlyph = _this.display.handleNewFeature(null,style,mapOptions);
		mapGlyph.addEntries(true);
	    }
	});
    },
    getMap: function() {
	return this.display.getMap();
    },
    changeOrder:function(toFront) {
	if(this.mapServerLayer) {
	    if(toFront)
		this.display.getMap().toFrontLayer(this.mapServerLayer);
	    else
		this.display.getMap().toBackLayer(this.mapServerLayer);		    
	    return;		
	}
	if(this.getImage()) {
	    if(toFront)
		this.display.getMap().toFrontLayer(this.getImage());
	    else
		this.display.getMap().toBackLayer(this.getImage());		    
	    return;		
	}
	if(this.features.length) {
	    if(toFront)
		Utils.toFront(this.display.getLayer().features, this.features,true);
	    else
		Utils.toBack(this.display.getLayer().features, this.features,true);
	}
    },	
    getId:function() {
	return this.id;
    },
    getFilterable: function() {
	return false;
	return this.attrs.filterable??true;
    },
    getAllFeatures: function() {
	let features=[];
	if(this.features) features.push(...this.features);
	if(this.mapLayer) features.push(...this.mapLayer.features);
	return features;
    },
    getFeatures: function() {
	return this.features;
    },
    clearFeatures: function() {
	this.display.removeFeatures(this.features);
	this.features = [];
    },
    addFeature: function(feature,andClear) {
	if(andClear) this.clearFeatures();
	this.features.push(feature);
	feature.mapGlyph = this;
    },
    getStyle: function() {
	return this.style;
    },
    panMapTo: function(andZoomIn) {
	if(this.isMultiEntry() && this.entries) {
	    let bounds = null;
	    this.entries.forEach(entry=>{
		let b = null;
		if(entry.hasBounds()) {
		    b =   MapUtils.createBounds(entry.getWest(),entry.getSouth(),entry.getEast(), entry.getNorth());
		} else if(entry.hasLocation()) {
		    b =   MapUtils.createBounds(entry.getLongitude(),entry.getLatitude(),
						entry.getLongitude(),entry.getLatitude());
		} 
		if(b) {
		    if(!bounds) bounds = b;
		    else bounds.extend(b);
		}
	    });
	    if(bounds) {
		this.display.getMap().zoomToExtent(this.display.getMap().transformLLBounds(bounds));
	    }
	}

	if(this.children) {
	    let features = [];
	    this.children.forEach(child=>{
		child.collectFeatures(features);
	    });
	    if(features.length) {
		this.display.getMap().centerOnFeatures(features);
	    }
	} else 	if(this.features.length) {
	    this.display.getMap().centerOnFeatures(this.features);
	} else if(this.mapLayer) {
	    this.display.getMap().zoomToLayer(this.mapLayer);
	} else	if(this.displayInfo?.display) {
	    if(this.displayInfo.display.myFeatureLayer && (
		!Utils.isDefined(this.displayInfo.display.layerVisible) ||
		    this.displayInfo.display.layerVisible)) {
		
		this.display.getMap().zoomToLayer(this.displayInfo.display.myFeatureLayer);
	    } else  if(this.displayInfo.display.pointBounds) {
		this.display.getMap().zoomToExtent(this.display.getMap().transformLLBounds(this.displayInfo.display.pointBounds));
	    }
	}
	if(andZoomIn) {
	    this.display.getMap().setZoom(16);
	}

    },
    collectFeatures: function(features) {
	if(this.children) {
	    this.children.forEach(child=>{
		child.collectFeatures(features);
	    });
	} else 	if(this.features.length) {
	    features.push(...this.features);
	}
    },
    getGeometry: function() {
	if(this.features.length>0) return this.features[0].geometry;
	else return null;
    },
    setName: function(name) {
	this.attrs.name = name;
    },
    getName: function() {
	return this.attrs.name||this.name;
    },
    setName: function(name) {
	this.attrs.name = name;
    },    
    getFeature: function() {
	if(this.features.length>0) return this.features[0];
	return null;
    },
    getFeatures: function() {
	return this.features;
    },    
    getType: function() {
	return this.type;
    },
    getWikiText:function() {
	return this.style.wikiText || this.getPopupText();
    },

    getPopupText: function() {
	return this.style.popupText;
    },
    getEntryId: function() {
	return this.attrs.entryId;
    },
    getLabel:function(forLegend,addDecorator) {
	let name = this.getName();
	let label;
	let theLabel;
	if(Utils.stringDefined(name)) {
	    if(!forLegend)
		theLabel= this.getType()+': '+name;
	    else
		theLabel = name;
	} else if(this.isFixed()) {
	    theLabel = this.style.text;
	    if(theLabel && theLabel.length>15) theLabel = theLabel.substring(0,14)+'...'
	} else {
	    theLabel =  this.getType();
	}
	label = theLabel;
	let url = null;
	let glyphType = this.display.getGlyphType(this.getType());
	let right = '';
	if(addDecorator) {
	    right+=this.getDecoration(true);
	}

	if(glyphType) {
	    let icon = Utils.getStringDefined([this.style.externalGraphic,this.attrs.icon,glyphType.getIcon()]);
	    if(icon.startsWith('data:')) icon = this.attrs.icon;
	    if(icon && icon.endsWith('blank.gif')) icon = glyphType.getIcon();
	    icon = HU.image(icon,['width','18px']);
	    if(url && forLegend)
		icon = HU.href(url,icon,['target','_entry']);
	    if(forLegend && !this.isMapServer() && !this.isFixed()) {
		right+=SPACE+
		    HU.span([CLASS,'ramadda-clickable imdv-legend-item-view',
			     'glyphid',this.getId(),
			     TITLE,'Click:Move to; Shift-click:Zoom in',],
			    HU.getIconImage('fas fa-binoculars',[],LEGEND_IMAGE_ATTRS));
	    }
	    label = HU.span(['style','margin-right:5px;'], icon)  + label;
	}
	if(forLegend) {
	    label = HU.div(['title',theLabel+'<br>Click to toggle visibility<br>Shift-click to select','style',HU.css('xmax-width','150px','overflow-x','hidden','white-space','nowrap')], label);	    
	}
	if(right!='') {
	    right= HU.span(['style',HU.css('white-space','nowrap')], right);
	    //	    label=HU.leftRightTable(label,right);
	}
	if(forLegend) {
	    let clazz = 'imdv-legend-label';
	    label = HU.div(['class','ramadda-clickable ' + clazz,'glyphid',this.getId()],label);
	    return [label,right];
	}
	return label;
    },

    removeChildGlyph: function(child) {
	if(this.children) this.children = Utils.removeItem(this.children, child);
    },

    loadJson:function(jsonObject) {
	if(jsonObject.children) {
	    this.children = [];
	    jsonObject.children.forEach(childJson=>{
		let child = this.display.makeGlyphFromJson(childJson);
		if(child) {
		    this.addChildGlyph(child);
		}
	    });
	}
    },
    getChildren: function(child) {
	if(!this.children) this.children = [];
	return this.children;
    },
    findGlyph:function(id) {
	if(id == this.getId()) return this;
	return ImdvUtils.findGlyph(this.children,id);
    },
    addChildGlyph: function(child) {
	this.getChildren().push(child);
	child.setParentGlyph(this);
    },
    getParentGlyph: function() {
	return this.parentGlyph;
    },
    setParentGlyph: function(parent) {
	if(this.parentGlyph) this.parentGlyph.removeChildGlyph(this);
	this.parentGlyph = parent;
	if(parent) this.display.removeMapGlyph(this);
    },    
    getLegendVisible:function() {
	return this.attrs.legendVisible;
    },
    setLegendVisible:function(visible) {
	this.attrs.legendVisible = visible;
    },
    getFiltersVisible:function() {
	return this.attrs.filtersVisible;
    },
    setFiltersVisible:function(visible) {
	this.attrs.filtersVisible = visible;
    },    
    convertText:function(text) {
	text =text.replace(/\n *\*/g,'\n &bull;');
	text =text.replace(/^ *\*/g,'&bull;');
	text = text.replace(/\"/g,"\\").replace(/\n/g,'<br>');
	return text;
    },
	

    makeLegend:function(opts) {
	opts = opts??{};
	let html = '';
	if(!this.display.getShowLegendShapes() && this.isShape()) {
	    return "";
	}
	let label =  this.getLabel(true,true);
	let body = HU.div(['class','imdv-legend-inner'],this.getLegendBody());
	if(this.isGroup()) {
	    let child="";
	    this.getChildren().forEach(mapGlyph=>{
		let childHtml = mapGlyph.makeLegend(opts);
		if(childHtml) child+=childHtml;
	    });
	    body+=HU.div(['class','imdv-legend-offset'],child);
	}

	let block = HU.toggleBlockNew("",body,this.getLegendVisible(),
				      {separate:true,headerStyle:'display:inline-block;',
				       extraAttributes:['map-glyph-id',this.getId()]});		
	if(opts.idToGlyph)
	    opts.idToGlyph[this.getId()] = this;
	let clazz = "";
	if(!this.getVisible()) clazz+=' imdv-legend-label-invisible ';
	html+=HU.open('div',['id',this.getId(),'class','imdv-legend-item '+clazz]);
	html+=HU.div(['style','display: flex;'],HU.div(['style','margin-right:4px;'],block.header)+
		     HU.div(['style','width:80%;'], label[0])+
		     HU.div([],label[1]));

	html+=HU.div(['class','imdv-legend-body'],block.body);
	html+=HU.close('div');
	return html;
    },

    getLegendBody:function() {
	let body = '';
	let buttons = this.display.makeGlyphButtons(this,true);
	if(this.isMap() && this.getProperty('showFeaturesTable',true))  {
	    this.showFeatureTableId = HU.getUniqueId('btn');
	    if(buttons!=null) buttons = HU.space(1)+buttons;
	    buttons =  HU.span(['id',this.showFeatureTableId,'title','Show features table','class','ramadda-clickable'],
			      HU.getIconImage('fas fa-table',[],BUTTON_IMAGE_ATTRS)) +buttons;
	}

	/** For now don't add this as we can also get it through the entry link below
	if(this.downloadUrl) {
	    if(buttons!=null) buttons = HU.space(1)+buttons;
	    buttons = HU.href(this.downloadUrl,HU.getIconImage('fas fa-download',[],BUTTON_IMAGE_ATTRS),['target','_download','title','Download','class','ramadda-clickable']) +buttons;
	}
	*/

	if(this.attrs.entryId) {
	    if(buttons!=null) buttons = HU.space(1)+buttons;
	    url = RamaddaUtils.getEntryUrl(this.attrs.entryId);
	    buttons = HU.href(url,HU.getIconImage('fas fa-home',[],BUTTON_IMAGE_ATTRS),['target','_entry','title','View entry','class','ramadda-clickable']) +buttons;
	}	

	
	if(!this.display.canEdit() && !this.getProperty('showButtons',true))
	    buttons = '';

	if(buttons!='')
	    body+=HU.div(['class','imdv-legend-offset'],buttons);
//	    body+=HU.center(buttons);
	if(Utils.stringDefined(this.attrs.legendText)) {
	    let text = this.attrs.legendText.replace(/\n/g,'<br>');
	    body += HU.div(['class','imdv-legend-offset imdv-legend-text'],text);
	}

	if(this.isMap()) {
	    let boxStyle = 'display:inline-block;width:14px;height:14px;margin-right:4px;';
	    let legend = '';
	    let styleLegend='';
	    this.getStyleGroups().forEach((group,idx)=>{
		styleLegend+=HU.openTag('table',['width','100%']);
		styleLegend+= HU.openTag('tr',['title',this.display.canEdit()?'Select style':'','class',CLASS_IMDV_STYLEGROUP +(this.display.canEdit()?' ramadda-clickable':''),'index',idx]);
		let style = boxStyle;
		if(group.style.fillColor)
		    style+=HU.css('background',group.style.fillColor);
		if(group.style.strokeColor)
		    style+=HU.css('border','1px solid ' +group.style.strokeColor);		
		styleLegend+=HU.tag('td',['width','18px'],
				    HU.div(['style', style]));
		styleLegend +=HU.tag('td',[], group.label);
		styleLegend+='</tr>';
		styleLegend+='</table>'
	    });

	    if(styleLegend!='') {
		styleLegend=HU.div(['id','glyphstyle_' + this.getId()], styleLegend);
		legend+=styleLegend;
	    }

	    if(this.attrs.mapStyleRules) {
		let rulesLegend = '';
		let lastProperty='';
		this.attrs.mapStyleRules.forEach(rule=>{
		    if(rule.type=='use') return;
		    if(!Utils.stringDefined(rule.property)) return;
		    let propOp = rule.property+rule.type;
		    if(lastProperty!=propOp) {
			if(rulesLegend!='') rulesLegend+='</table>';
			let type = rule.type;
			if(type=='==') type='=';
			type = type.replace(/</g,'&lt;').replace(/>/g,'&gt;');
			rulesLegend+= HU.b(this.makeLabel(rule.property,true))+' ' +type+'<br><table width=100%>';
		    }
		    lastProperty  = propOp;
		    let label = rule.value;
		    label   = HU.span(['style','font-size:9pt;'],label);
		    let item = '<tr><td width=16px>';
		    let style = boxStyle;
		    rule.style.split('\n').forEach(line=>{
			line  = line.trim();
			if(line=='') return;
			let toks = line.split(':');
			if(toks[0]=='fillColor') style+=HU.css('background',toks[1]);
			else if(toks[0]=='strokeColor') style+=HU.css('border','1px solid ' +toks[1]);
		    });
		    item+=HU.div(['style',style],'')+'</td>';
		    item += '</td><td>'+ label+'</td></tr>';
		    rulesLegend+=HU.div([],item);
		});
		if(rulesLegend!='') {
		    rulesLegend+= '</table>';
		    legend+=rulesLegend;
		}
	    }
	    if(legend!='') {
		body+=HU.toggleBlock('Legend',legend,true);
	    }
	}

	if(this.isMapServer() || Utils.stringDefined(this.style.imageUrl)) {
	    let v = this.isImage()?this.style.imageOpacity:this.style.opacity;
	    body += 
		HU.center(
		    HU.div(['title','Set image opacity','slider-min',0,'slider-max',1,'slider-value',v,
			ID,this.domId('image_opacity_slider'),'class','ramadda-slider',STYLE,HU.css('display','inline-block','width','80%')],''));
	}

	let item = i=>{
	    if(Utils.stringDefined(i)) {
		body+=HU.div(['class','imdv-legend-body-item'], i);
	    }
	};

	body+=HU.div(['id',this.domId('legendcolortable_fill')]);
	body+=HU.div(['id',this.domId('legendcolortable_stroke')]);	
	body+=HU.div(['id',this.domId(ID_MAPLEGEND)]);

	//Put the placeholder here for map filters
	body+=HU.div(['id',this.domId(ID_MAPFILTERS)]);

	if(this.type==GLYPH_LABEL && this.style.label) {
	    item(this.style.label.replace(/\"/g,"\\"));
	}
	item(this.display.getDistances(this.getGeometry(),this.getType()));
	if(this.isMultiEntry()) {
	    item(HU.div(['id',this.domId('multientry')]));
	}

	if(Utils.stringDefined(this.style.imageUrl)) {
	    item(HU.center(HU.href(this.style.imageUrl,HU.image(this.style.imageUrl,['style',HU.css('margin-bottom','4px','border','1px solid #ccc','width','150px')]),['target','_image'])));
	}
	if(Utils.stringDefined(this.style.legendUrl)) {
	    item(HU.center(HU.href(this.style.legendUrl,HU.image(this.style.legendUrl,['style',HU.css('margin-bottom','4px','border','1px solid #ccc','width','150px')]),['target','_image'])));
	}
	return body;
    },
    initLegend:function() {

	let _this = this;
	if(this.display.canEdit()) {
	    let label = jqid(this.getId()).find('.imdv-legend-label');
	    //Set the last dropped time so we don't also handle this as a setVisibility click
	    let notify = ()=>{_this.display.setLastDroppedTime(new Date());};
	    jqid(this.getId()).draggable({
		start: notify,
		drag: notify,
		stop: notify,
		containment:this.display.domId(ID_LEGEND),
		revert: true
	    });
	    //Only drop on the legend label
	    label.droppable( {
		hoverClass: 'imdv-legend-item-droppable',
		accept:'.imdv-legend-item',
		drop: function(event,ui){
		    notify();
		    let draggedGlyph = _this.display.findGlyph(ui.draggable.attr('id'));
		    if(!draggedGlyph) {
			console.log('Could not find dragged glyph');
			return;
		    }
		    _this.handleDroppedGlyph(draggedGlyph);
		}
	    });

	    let items = this.jq(ID_LEGEND).find('.imdv-legend-label');
	    let rows = jqid('glyphstyle_'+this.getId()).find('.' + CLASS_IMDV_STYLEGROUP);

	    rows.click(function() {
		console.log('click');
		if($(this).hasClass(CLASS_IMDV_STYLEGROUP_SELECTED)) {
		    $(this).removeClass(CLASS_IMDV_STYLEGROUP_SELECTED);
		    _this.selectedStyleGroup = null;
		    return;
		}
		rows.removeClass(CLASS_IMDV_STYLEGROUP_SELECTED);
		$(this).addClass(CLASS_IMDV_STYLEGROUP_SELECTED);
		_this.selectedStyleGroup = _this.getStyleGroups()[$(this).attr('index')];

	    });
	}


	if(this.isMultiEntry() && this.entries) {
	    this.showMultiEntries();
	}
	if(this.showFeatureTableId) {
	    $('#'+ this.showFeatureTableId).click(function() {
		_this.showFeaturesTable($(this));
	    });
	}

	this.jq('image_opacity_slider').slider({		
	    min: 0,
	    max: 1,
	    step:0.01,
	    value:this.jq('image_opacity_slider').attr('slider-value'),
	    stop: function( event, ui ) {
		if(_this.isMapServer())
		    _this.style.opacity = ui.value;
		else if(_this.image) {
		    _this.style.imageOpacity = ui.value;
		    _this.image.setOpacity(_this.style.imageOpacity);
		}
		_this.applyStyle(_this.style);
	    }});
	
//	this.applyMapStyle();
	this.makeFeatureFilters();
	if(this.isGroup()) {
	    this.getChildren().forEach(mapGlyph=>{mapGlyph.initLegend();});
	}
    },
    handleDroppedGlyph:function(draggedGlyph) {
	let debug = false;
	if(this.display.handleDropTimeout) {
	    if(debug)	    console.log('clearing pending drop');
	    clearTimeout(this.display.handleDropTimeout);
	}
	this.display.handleDropTimeout = setTimeout(()=>{
	    if(debug)	    console.log(this.name +' handleDrop');
	    this.display.handleDropTimeout = null;
	    draggedGlyph.display.removeMapGlyph(draggedGlyph);
	    draggedGlyph.setParentGlyph(null);
	    if(this.isGroup()) {
	    if(debug)		console.log('landed on group');
		this.addChildGlyph(draggedGlyph);
		draggedGlyph.changeOrder(false);
	    } else {
		if(this.getParentGlyph()) {
		    if(debug)		    console.log('landed on glyph in a group');
		    this.getParentGlyph().addChildGlyph(draggedGlyph);
		    this.display.moveGlyphBefore(this, draggedGlyph,this.getParentGlyph().getChildren());
		} else {
		    this.display.moveGlyphBefore(this, draggedGlyph);
		}
	    }
	    this.display.handleGlyphsChanged();
	    this.display.redraw();
	},1);
    },

    convertPopupText:function(text) {
	if(this.getImage()) {
	    text = text.replace(/\${image}/g,HU.image(this.style.imageUrl,['width','200px']));
	}
	return text;
    },
    
    isGroup:function() {
	return this.getType() ==GLYPH_GROUP;
    },
    isEntry:function() {
	return this.getType() ==GLYPH_ENTRY;
    },
    isImage:function() {
	return this.getType() ==GLYPH_IMAGE;
    },    
    isMap:function() {
	return this.getType()==GLYPH_MAP;
    },
    isMapServer:function() {
	return this.getType()==GLYPH_MAPSERVER;
    },    
    isMultiEntry:function() {
	return this.getType()==GLYPH_MULTIENTRY;
    },
    getShowMultiData:function() {
	return this.attrs.showmultidata;
    },
    setShowMultiData:function(v) {
	this.attrs.showmultidata = v;
    },
    setMapServerUrl:function(url,wmsLayer,legendUrl,predefined) {
	this.style.legendUrl = legendUrl;

	if(Utils.stringDefined(predefined)) {
	    if(!Utils.stringDefined(this.attrs.name)) {
		let mapLayer = RAMADDA_MAP_LAYERS_MAP[predefined];
		if(mapLayer) this.attrs.name = mapLayer.name;
	    }
	} else {
	    if(!Utils.stringDefined(wmsLayer)) {
		if(url.match('&request=GetMap')) {
		    try {
			let _url = new URL(url);
			url = _url.protocol+'//' + _url.host +_url.pathname;
			wmsLayer = _url.searchParams.get('layers');
		    } catch(err) {
			console.log(err);
		    }
		}
	    }
	    if(!Utils.stringDefined(this.attrs.name)) {
		let _url = new URL(url);
		this.attrs.name = _url.hostname;
	    }
	}


	this.attrs.mapServerUrl = url;
	this.attrs.wmsLayer = wmsLayer;
	this.attrs.predefinedLayer = predefined;
    },
    getAttributes: function() {
	return this.attrs;
    },
    setMapLayer:function(mapLayer) {
	this.mapLayer = mapLayer;
	if(mapLayer) {
	    mapLayer.mapGlyph = this;
	    mapLayer.textGetter = (feature)=>{
		let text  = this.getPopupText();
		if(!Utils.isDefined(text) || !feature.attributes) return null;
		return this.applyMacros(text, feature.attributes);
	    };
	}
    },
    getMapLayer: function() {
	return this.mapLayer;
    },
    setMapServerLayer:function(mapLayer) {
	this.mapServerLayer = mapLayer;
	if(mapLayer)
	    mapLayer.mapGlyph = this;
    },
    getMapServerLayer: function() {
	return this.mapServerLayer;
    },    
    checkMapLayer:function(andZoom) {
	//Only create the map if we're visible
	if(!this.isMap() || !this.isVisible()) return;
	if(this.mapLayer==null) {
	    if(!Utils.isDefined(andZoom)) andZoom = true;
	    this.setMapLayer(this.display.createMapLayer(this,this.attrs,this.style,andZoom));
	    this.applyMapStyle();
	}
    },
    checkMapServer:function(andZoom) {
	if(!this.isMapServer()) return;
	if(this.mapServerLayer==null) {
	    let url=this.attrs.mapServerUrl;
	    let wmsLayer=this.attrs.wmsLayer;
	    if(Utils.stringDefined(wmsLayer)) {
		this.mapServerLayer = MapUtils.createLayerWMS(this.getName(), url, {
		    layers: wmsLayer,
		    format: "image/png",
		    isBaseLayer: false,
		    srs: "epse:4326",
		    transparent: true
		}, {
		    opacity:1.0
		});
	    } else if(Utils.stringDefined(url)) {
		//Convert malformed TMS url
		url = url.replace(/\/{/g,"/${");
		this.mapServerLayer =  this.display.getMap().createXYZLayer(this.getName(), url);
		console.log(url)
	    } else if(Utils.stringDefined(this.attrs.predefinedLayer)) {
		let mapLayer = RAMADDA_MAP_LAYERS_MAP[this.attrs.predefinedLayer];
		if(mapLayer) {
		    this.mapServerLayer = this.display.getMap().makeMapLayer(this.attrs.predefinedLayer);
		} else {
		    console.error("Unknown map layer:" +this.attrs.predefinedLayer);
		}
	    } else {
		console.error("No map server url defined");
		return;
	    }

	    if(!Utils.isDefined(andZoom)) andZoom = true;
	    if(Utils.isDefined(this.style.opacity)) {
		this.mapServerLayer.opacity = +this.style.opacity;
	    }

	    this.mapServerLayer.setVisibility(this.isVisible());
	    this.mapServerLayer.isBaseLayer = false;
	    this.mapServerLayer.visibility = this.isVisible();
	    this.mapServerLayer.canTakeOpacity = true;
	    this.display.getMap().addLayer(this.mapServerLayer,true);
	}
    },

    getImage:function() {
	return this.image;
    },
    setImage:function(image) {
	this.image = image;
	this.image.mapGlyph = this;
    },     
    hasMapFeatures: function() {
	if(!this.isMap() || !this?.mapLayer?.features || this.mapLayer.features.length==0) return false;
	return true;
    },
    canDoMapStyle: function() {
	if(!this.hasMapFeatures() || !this.mapLayer.features[0].attributes ||
	   Object.keys(this.mapLayer.features[0].attributes).length==0) {
	    return false;
	}
	return true;
    },

    applyPropertiesComponent: function() {
	if(this.isMap()) {
	    this.setMapPointsRange(jqid('mappoints_range').val());
	    this.setMapLabelsTemplate(jqid('mappoints_template').val());	    

	    let styleGroups = this.getStyleGroups();
	    let groups = [];
	    for(let i=0;i<20;i++) {
		let prefix = 'mapstylegroups_' + i;
		let group = styleGroups[i];
		let label = jqid(prefix+'_label').val();
		if(!Utils.stringDefined(label)) continue;
		if(!group) group = {
		    style:{},
		    indices:[],
		}
		group.label = label
		group.style = {};

		let value;
		if(Utils.stringDefined(value=jqid(prefix+'_fillcolor').val())) group.style.fillColor = value;
		if(Utils.stringDefined(value=jqid(prefix+'_fillopacity').val())) group.style.fillOpacity = value;
		if(Utils.stringDefined(value=jqid(prefix+'_strokecolor').val())) group.style.strokeColor = value;
		if(Utils.stringDefined(value=jqid(prefix+'_strokewidth').val())) group.style.strokeWidth = value;		
		groups.push(group);
	    }
	    this.attrs.styleGroups = groups;
	}	    

	if(!this.canDoMapStyle()) return;
	this.attrs.fillColors = this.jq('fillcolors').is(':checked');
	let getColorBy=(prefix)=>{
	    return {
		property:this.jq(prefix +'colorby_property').val(),
		min:this.jq(prefix +'colorby_min').val(),
		max:this.jq(prefix +'colorby_max').val(),
		colorTable:this.jq(prefix +'colorby_colortable').val()};		
	};

	this.attrs.fillColorBy =  getColorBy('fill');
	this.attrs.strokeColorBy =  getColorBy('stroke');	

	let rules = [];
	for(let i=0;i<20;i++) {
	    if(!Utils.stringDefined(jqid('mapproperty_' + i).val()) &&
	       !Utils.stringDefined(jqid('mapstyle_' + i).val())) {
		continue;
	    }
	    let rule = {
		property:jqid('mapproperty_' + i).val(),
		type:jqid('maptype_' + i).val(),
		value:jqid('mapvalue_' + i).val(),
		style:jqid('mapstyle_' + i).val(),
	    }
	    rules.push(rule);
	}
	this.attrs.mapStyleRules =rules;
    },
    getColorTableDisplay:function(id,min,max,showRange,isEnum,strings) {
	if(isEnum) showRange=false;
	let ct = Utils.ColorTables[id];
	if(!ct) {
	    return "----";
	}
        let display = Utils.getColorTableDisplay(ct,  min??0, max??1, {
	    tooltips:strings,
	    showRange: false,
            height: "20px",
	    showRange:showRange
        });
	let attrs = [TITLE,id,'style','margin-right:4px;',"colortable",id]
//	if(ct.colors.length>20)   attrs.push(STYLE,HU.css('width','400px'));
        return  HtmlUtils.div(attrs,display);
    },
    initPropertiesComponent: function(dialog) {
	let _this = this;
	let clearElevations = this.jq('clearelevations');
	clearElevations.button().click(function(){
	    _this.attrs.elevations = null;
	    $(this).attr('disabled','disabled');
	    $(this).addClass('ramadda-button-disabled');
	});
	if(!this.attrs.elevations) {
	    this.jq('clearelevations').prop('disabled',true);
	}

	this.jq('makeelevations').button().click(function(){
	    $(this).html('Fetching elevations');
	    let callback =(count,total)=>{
		$(this).html('Processed ' + count + ' of ' + total);
		return true;
	    }
	    let done = ()=>{
		clearElevations.removeClass('ramadda-button-disabled');
		clearElevations.attr('disabled',null);
		$(this).html('Done');
		setTimeout(()=>{
		    $(this).html('Add elevations');
		},3000);

	    }
	    _this.addElevations(callback,done)
	});


	let decorate = (prefix) =>{
	    let div = this.getColorTableDisplay(this.jq(prefix+'colorby_colortable').val(),NaN,NaN,false);
	    this.jq(prefix+'colorby_colortable_label').html(div);
	};

	decorate('fill');
	decorate('stroke');	
	dialog.find(".ramadda-colortable-select").click(function() {
	    let prefix = $(this).attr('prefix');
	    let ct = $(this).attr("colortable");
	    _this.jq(prefix+'colorby_colortable').val(ct);
	    decorate(prefix);
	});
	Utils.displayAllColorTables(this.display.domId('fillcolorby'));
	Utils.displayAllColorTables(this.display.domId('strokecolorby'));


	let initColor  = prefix=>{
	    dialog.find('#'+this.domId(prefix+'colorby_property')).change(function() {
		let prop =  $(this).val();
		_this.featureInfo.every(info=>{
		    if(info.property!=prop) return true;
		    if(info.isString() || info.isEnumeration()) {
			_this.jq(prefix+'colorby_min').val('');
			_this.jq(prefix+'colorby_max').val('');		    
		    }  else {
			_this.jq(prefix+'colorby_min').val(info.min);
			_this.jq(prefix+'colorby_max').val(info.max);
		    }
		    return true;
		});
	    });
	};
	initColor('fill');
	initColor('stroke');

	dialog.find('[mapproperty_index]').change(function() {
	    let info = _this.featureInfoMap[$(this).val()];
	    if(!info) return;
	    let index  = $(this).attr('mapproperty_index');	    
	    let tt = "";
	    let value = jqid('mapvalue_' + index).val();
	    let wrapper = jqid('mapvaluewrapper_' + index);
	    if(info.isNumeric()) {
		wrapper.html(HU.input("",value,['id','mapvalue_' + index,'size','15']));
		tt=info.min +" - " + info.max;
	    }  else  if(info.samples.length) {
		tt = Utils.join(info.getSamplesLabels(), ", ");
		if(info.isEnumeration()) {
		    wrapper.html(HU.select("",['id','mapvalue_' + index],info.samples,value));
		} else {
		    wrapper.html(HU.input("",value,['id','mapvalue_' + index,'size','15']));
		}
	    }
	    jqid('mapvalue_' + index).attr('title',tt);
	});


    },

    getFeaturesTable:function(id) {
	let columns  =this.getFeatureInfo().filter(info=>{
	    return info.showTable();
	});
	let table;
	this.featureTableMap = {};

	let featureInfo = this.getFeatureInfo();
	let rowCnt=0;
	let stats;
	this.mapLayer.features.forEach((feature,rowIdx)=>{
	    if(Utils.isDefined(feature.isVisible) && !feature.isVisible) {
		return
	    }
	    let attrs = feature.attributes;
	    let first = rowCnt++==0;
	    if(first) {
		table = HU.openTag('table',['id',id,'table-ordering','true','table-searching','true','table-height','400px','class','stripe rowborder ramadda-table'])
		table+='<thead><tr>';
		stats = [];
		columns.forEach((column,idx)=>{
		    table+=HU.tag('th',[],column.getLabel(true));
		    stats.push({total:0,count:0,min:0,max:0});
		});
		table+=HU.close('tr','thead','tbody');
	    }
	    this.featureTableMap[rowIdx] =feature;
	    table+=HU.openTag('tr',['title','Click to zoom to','featureidx', rowIdx,'class','imdv-feature-table-row ramadda-clickable']);
	    columns.forEach((column,idx)=>{
		let stat =  stats[idx];
		let v= this.getFeatureValue(feature,column.property)??'';
//		let v = attrs[column.property]??'';
		if(v && Utils.isDefined(v.value)) v = v.value;
//		console.dir(column.property,v,attrs[column.property]);
		let nv = +v;
		let sv = String(v);
		let isNumber = column.isNumeric();
		if(isNumber && sv!='' && !isNaN(nv)) {
		    stat.count++;
		    stat.min = first?nv:Math.min(nv, stat.min);
		    stat.max = first?nv:Math.max(nv, stat.max);		    
		    stat.total+=nv;
		}
		//Check for html. Maybe just convert to entities?
		if(sv.indexOf('<')>=0) {
		    sv = Utils.stripTags(sv);
		}
		table+=HU.tag('td',['style',isNumber?'text-align:right':'','align',isNumber?'right':'left'],sv);
	    });
	});
	if(stats) {
	    table+='<tfoot><tr>';
	    let fmt = (label,amt) =>{
		return HU.tr(HU.td(['style','text-align:right','align','right'],HU.b(label)) +
			     HU.td(['style','text-align:right','align','right'],Utils.formatNumberComma(amt)));
	    };
	    columns.forEach((column,idx)=>{
		let stat =  stats[idx];
		table+='<td align=right>';
		if(stat.count!=0) {
		    let inner = '<table>';
		    inner +=
			fmt('Total:', stat.total) +
			fmt('Min:', stat.min) +
			fmt('Max:', stat.max) +
			fmt('Avg:', stat.total/stat.count);
		    inner+='</table>';
		    table+=inner;
		}
		table+='</td>';
	    });
	    table+='</tr></tfoot>';
	    table+=HU.close('tbody');
	}

	return table;
    },
    downloadFeaturesTable:function(id) {
	let columns;
	let csv='';
	this.mapLayer.features.forEach((feature,rowIdx)=>{
	    if(Utils.isDefined(feature.isVisible) && !feature.isVisible) {
		return;
	    }
	    let attrs = feature.attributes;
	    if(columns==null) {
		columns  =this.getFeatureInfo().filter(info=>{
		    return info.showTable();
		});
		let rows = columns.map((column,idx)=>{ return column.getLabel();});
		csv+=Utils.join(rows,',');
		csv+='\n';
	    }
	    let rows = columns.map((column,idx)=>{
		let v = attrs[column.property]??'';
		if(Utils.isDefined(v.value)) v = v.value;
		return  String(v).replace(/\n/g,'_nl_');
	    });
	    csv+=Utils.join(rows,',');
	    csv+='\n';
	});
	Utils.makeDownloadFile('features.csv',csv);
    },

    updateFeaturesTable:function() {
	if(!this.featuresTableDialog) return;
	let tableId = HU.getUniqueId("table");
	let table =this.getFeaturesTable(tableId);
	let html='';
	if(!table) {
	    html += HU.div(['style','width:200px;margin:10px;'],'No data');
	} else {
	    html +=  
		HU.div(['style',HU.css('margin','5px','max-width','1000px','overflow-x','scroll')],table);
	    html = HU.div(['style','margin:5px;'], html);
	}
	this.jq('featurestable').html(html);
	HU.formatTable('#'+tableId,{scrollX:true});
	let _this = this;
	this.featuresTableDialog.find('.imdv-feature-table-row').click(function() {
	    let feature = _this.featureTableMap[$(this).attr('featureidx')];
	    _this.display.getMap().centerOnFeatures([feature]);
	});
    },
    showFeaturesTable:function(anchor) {
	if(this.featuresTableDialog)
	    this.featuresTableDialog.remove();
	let html =  HU.div(['id',this.domId('downloadfeatures'),'class','ramadda-clickable'],'Download Table');
	html+=HU.div(['id',this.domId('featurestable')]);
	html = HU.div(['style','margin:10px;'], html);
	this.featuresTableDialog =
	    HU.makeDialog({content:html,title:this.name,header:true,draggable:true,
			   my:'left top',
			   at:'left bottom',
			   anchor:anchor});
	
	this.updateFeaturesTable();
	this.jq('downloadfeatures').button().click(()=>{
	    this.downloadFeaturesTable();
	});
    },
    getHelp:function(url,label) {
	if(url.startsWith('#')) url = '/userguide/imdv.html' + url;
	return HU.href(ramaddaBaseUrl+url,HU.getIconImage(icon_help) +' ' +(label??'Help'),['target','_help']);
    },
    getPropertiesComponent: function(content) {
	if(!this.canDoMapStyle()) return;
	let attrs = this.mapLayer.features[0].attributes;
	let featureInfo = this.featureInfo = this.getFeatureInfo();
	let keys  = Object.keys(featureInfo);
	let numeric = featureInfo.filter(info=>{return info.isNumeric();});
	let enums = featureInfo.filter(info=>{return info.isEnumeration();});
	let colorBy = '';
	colorBy+=HU.leftRightTable(HU.checkbox(this.domId('fillcolors'),['id',this.domId('fillcolors')],
					       this.attrs.fillColors,'Fill Colors'),
				   this.getHelp('#mapstylerules'));

	numeric = featureInfo;
	if(numeric.length) {
	    let numericProperties=Utils.mergeLists([['','Select']],numeric.map(info=>{return info.property;}));
	    let mapComp = (obj,prefix) =>{
		let comp = '';
		comp+=HU.div(['class','formgroupheader'], 'Map value to ' + prefix +' color')+ HU.formTable();
		comp += HU.formEntry('Property:', HU.select('',['id',this.domId(prefix+'colorby_property')],numericProperties,obj.property) +HU.space(2)+ HU.b('Range: ') + HU.input('',obj.min??'', ['id',this.domId(prefix+'colorby_min'),'size','6','title','min value']) +' -- '+    HU.input('',obj.max??'', ['id',this.domId(prefix+'colorby_max'),'size','6','title','max value']));
		comp += HU.hidden('',obj.colorTable||'blues',['id',this.domId(prefix+'colorby_colortable')]);
		comp+=HU.formEntry('Color table:', HU.div(['id',this.domId(prefix+'colorby_colortable_label')])+
				   Utils.getColorTablePopup(null,null,'Select',true,'prefix',prefix));
		comp+=HU.close('table');
		return comp;
	    };
	    colorBy+=mapComp(this.attrs.fillColorBy ??{},'fill');
	    colorBy+=mapComp(this.attrs.strokeColorBy ??{},'stroke');	    
	}

	let properties=Utils.mergeLists([['','Select']],featureInfo.map(info=>{return info.property;}));
	let ex = '';
	let helpLines = [];
	featureInfo.forEach(info=>{
	    helpLines.push(info.id);
	    let seen ={};
	    let list =[];
	    let label = HU.b(this.makeLabel(info.property,true));
	    let line = ''
	    if(info.isNumeric()) {
		line =  info.min +' - ' + info.max;
	    } else if(info.isEnumeration()) {
		line =  Utils.join(info.getSamplesValues(),', ');
	    } else {
		line =  Utils.join(info.getSamplesValues(),', ');
	    }
	    ex+=label+': ' + line +'<br>';
	});
	let c = OpenLayers.Filter.Comparison;
	let operators = [c.EQUAL_TO,c.NOT_EQUAL_TO,c.LESS_THAN,c.GREATER_THAN,c.LESS_THAN_OR_EQUAL_TO,c.GREATER_THAN_OR_EQUAL_TO,[c.BETWEEN,'between'],[c.LIKE,'like'],[c.IS_NULL,'is null'],['use','Use']];
	let rulesTable = HU.formTable();
	let sample = 'Samples&#013;';
	for(a in attrs) {
	    let v = attrs[a]?String(attrs[a]):'';
	    v = v.replace(/"/g,'').replace(/\n/g,' ');
	    sample+=a+'=' + v+'&#013;';
	}
	rulesTable+=HU.tr([],HU.tds(['style','font-weight:bold;'],['Property','Operator','Value','Style']));
	let rules = this.getMapStyleRules();
	let styleTitle = 'e.g.:&#013;fillColor:red&#013;fillOpacity:0.5&#013;strokeColor:blue&#013;strokeWidth:1&#013;';
	for(let index=0;index<20;index++) {
	    let rule = index<rules.length?rules[index]:{};
	    let value = rule.value??'';
	    let info = this.featureInfoMap[properties,rule.property];
	    let title = sample;
	    let valueInput;
	    if(info) {
		if(info.isNumeric())
		    title=info.min +' - ' + info.max;
		else if(info.samples.length)
		    title = Utils.join(info.getSamplesLabels(), ', ');
	    }
	    if(info?.isEnumeration()) {
		valueInput = HU.select('',['id','mapvalue_' + index],info.samples,value); 
	    } else {
		valueInput = HU.input('',value,['id','mapvalue_' + index,'size','15']);
	    }
	    let propSelect =HU.select('',['id','mapproperty_' + index,'mapproperty_index',index],properties,rule.property);
	    let opSelect =HU.select('',['id','maptype_' + index],operators,rule.type);	    
	    valueInput =HU.span(['id','mapvaluewrapper_' + index],valueInput);
	    let s = Utils.stringDefined(rule.style)?rule.style:'';
	    let styleInput = HU.textarea('',s,['id','mapstyle_' + index,'rows','3','cols','30','title',styleTitle]);
	    rulesTable+=HU.tr(['valign','top'],HU.tds([],[propSelect,opSelect,valueInput,styleInput]));
	}
	rulesTable += '</table>';
	let table = HU.b('Style Rules')+HU.div(['class','imdv-properties-section'], rulesTable);
	content.push({header:'Map Style Rules', contents:colorBy+table});


	let mapPointsRange = HU.leftRightTable(HU.b('Visiblity limit: ') + HU.select('',[ID,'mappoints_range'],this.display.levels,this.getMapPointsRange()??'',null,true) + ' '+
					       HU.span(['class','imdv-currentlevellabel'], '(current level: ' + this.display.getCurrentLevel()+')'),
					       this.getHelp('#map_labels'));
	let mapPoints = HU.textarea('',this.getMapLabelsTemplate()??'',['id','mappoints_template','rows','6','cols','40','title','Map points template, e.g., ${code}']);

	let propsHelp =this.display.makeSideHelp(helpLines,'mappoints_template',{prefix:'${',suffix:'}'});
	mapPoints = HU.hbox([mapPoints,HU.space(2),'Add property:' + propsHelp]);
	content.push({header:'Map Labels',
		      contents:mapPointsRange+  HU.b('Label Template:')+'<br>' +mapPoints});

	let styleGroups =this.getStyleGroups();
	let styleGroupsUI = HU.leftRightTable('',
					      this.getHelp('#adding_a_map'));
	styleGroupsUI+=HU.openTag('table',['width','100%']);
	styleGroupsUI+=HU.tr([],HU.tds([],
				       ['Group','Fill','Opacity','Stroke','Width','Features']));
	for(let i=0;i<20;i++) {
	    let group = styleGroups[i];
	    let prefix = 'mapstylegroups_' + i;
	    styleGroupsUI+=HU.tr([],HU.tds([],[
		HU.input('',group?.label??'',['id',prefix+'_label','size','10']),
		HU.input('',group?.style.fillColor??'',['class','ramadda-imdv-color','id',prefix+'_fillcolor','size','6']),
		HU.input('',group?.style.fillOpacity??'',['title','0-1','id',prefix+'_fillopacity','size','2']),		
		HU.input('',group?.style.strokeColor??'',['class','ramadda-imdv-color','id',prefix+'_strokecolor','size','6']),
		HU.input('',group?.style.strokeWidth??'',['id',prefix+'_strokewidth','size','6']),				
		Utils.join(group?.indices??[],',')]));
	}
	styleGroupsUI += HU.closeTag('table');
	styleGroupsUI = HU.div(['style',HU.css('max-height','150px','overflow-y','auto')], styleGroupsUI);
	content.push({header:'Map Style Groups',contents:styleGroupsUI});
	content.push({header:'Sample Values',contents:ex});
    },
    getStyleGroups: function() {
	if(!this.attrs.styleGroups) {
	    this.attrs.styleGroups = [];
	}
	return this.attrs.styleGroups??[];
    },
    getMapStyleRules: function() {
	return this.attrs.mapStyleRules??[];
    },

    
    numre : /^[\d,\.]+$/,
    cleanupFeatureValue:function(v) {
	if(v===null) return null;
	if(Utils.isDefined(v.value)) {
	    v = v.value;
	}
	let sv = String(v);
//	if(sv.indexOf('[object')>=0) {console.log('X',v,(typeof v));} else console.log(sv);
	if(sv.trim()=="") return "";
	if(sv.match(this.numre)) {
	    sv = sv.replace(/,/g,'').replace(/^0+/,"");
	}
	return sv;
    },
    getFeatureValue:function(feature,property) {
	if(!feature.attributes) return null;
	let value = feature.attributes[property];
	if(!Utils.isDefined(value)) return null;
	return  this.cleanupFeatureValue(value);
    },
    getFeatureInfo:function() {
	if(this.featureInfo) return this.featureInfo;
	if(!this.mapLayer?.features) return [];
	let features= this.mapLayer.features;
	if(!this.mapLayer || this.mapLayer.features.length==0) return [];
	let attrs = this.mapLayer.features[0].attributes;
	let keys =   Object.keys(attrs);
	let _this = this;
	let first = [];		
	let middle=[];
	let last = [];
	keys.forEach(key=>{
	    let info = {
		property:key,
		id:Utils.makeId(key),
		min:NaN,
		max:NaN,
		type:'',
		getId:function() {
		    return this.id;
		},
		getProperty:function(prop,dflt) {
		    let v =    _this.getProperty(this.id+'.' + prop,dflt);
		    return v;
		},
		show: function() {
		    return  this.getProperty('show',_this.getProperty('feature.show',true));
		},
		showFilter: function() {
		    return   this.getProperty('filter.show',_this.getProperty('filter.show',this.show()));
		},
		showTable: function() {
		    return this.getProperty('table.show',_this.getProperty('table.show',this.show()));
		},
		showPopup: function() {
		    return this.getProperty('popup.show',_this.getProperty('popup.show',this.show()));
		},				

		getType:function() {
		    return this.getProperty('type',this.type);
		},
		getLabel:function(addSpan) {
		    let label  =this.getProperty('label');
		    if(!Utils.stringDefined(label)) label  =_this.display.makeLabel(this.property);
		    if(addSpan) label = HU.span(['title',this.property],label);
		    return label;
		},

		isNumeric:function(){return this.isInt() || this.getType()=='numeric';},
		isInt:function() {return this.getType()=='int';},
		isString:function() {return this.getType()=='string';},
		isEnumeration:function() {return this.getType()=='enumeration';},
		seen:{},
		samples:[],
		getSamplesLabels:function() {
		    return this.samples.map(sample=>{return sample.label;});
		},
		getSamplesValues:function() {
		    return this.samples.map(sample=>{
			let cnt = this.seen[sample.value];
			return sample.value +' ('+ cnt+')';
		    });
		}		

	    };
	    let _c = info.property.toLowerCase();
	    if(_c.indexOf('objectid')>=0) {
		last.push(info);
	    } else if(_c.indexOf('name')>=0) {
		first.push(info);
	    } else {
		middle.push(info);
	    }
	});


	this.featureInfo =  Utils.mergeLists(first,middle,last);
	features.forEach((f,fidx)=>{
	    this.featureInfo.forEach(info=>{
		let value= this.getFeatureValue(f,info.property);
		if(!Utils.isDefined(value)) return;
		if(isNaN(value) || info.samples.length>0) {
		    if(info.samples.length<30) {
			info.type='enumeration';
			if(!info.seen[value]) {
			    info.seen[value] = 0;
			    info.samples.push(value);
			}
			info.seen[value]++;
		    } else {
			info.type='string';
		    }
		} else if(!isNaN(value)) {
		    if(info.type != 'numeric')
			info.type='int';
		    if(Math.round(value)!=value) {
			info.type = 'numeric';
		    }
		    info.min = isNaN(info.min)?value:Math.min(info.min,value);
		    info.max = isNaN(info.max)?value:Math.max(info.max,value);			
		}
	    });
	});

	this.featureInfoMap = {};
	this.featureInfo.forEach(info=>{
	    if(info.samples.length) {
		let items = info.samples.map(item=>{
		    return {value:item,label:Utils.makeLabel(item)};
		});
		info.samples =  items.sort((a,b)=>{
		    return a.label.localeCompare(b.label);
		});
	    }
	    this.featureInfoMap[info.property] = info;
	});
	return this.featureInfo;
    },
    makeLabel:function(l,makeSpan) {
	let id = Utils.makeId(l);
	let label = l;
	if(id=='shapestlength') {
	    label =  'Shape Length';
	} else 	if(this.getProperty(id+'.feature.label')) {
	    label =  this.getProperty(id+'.feature.label');
	} else {
	    label =  this.display.makeLabel(l);
	}
	if(makeSpan) label = HU.span(['title','aka:' +id], label);
	return label;
    },

   getProperty:function(key,dflt) {
	if(this.attrs.properties) {
	    if(!this.parsedProperties) {
		this.parsedProperties = Utils.parseMap(this.attrs.properties,"\n","=")??{};
	    }
	    let v = this.parsedProperties[key];
	    if(v) {
		v = String(v);
		if(v==='true') return true;
		if(v==='false') return false;		
		return v;
	    }
	}
	return this.display.getMapProperty(key,dflt);
    },
    makeFeatureFilters:function() {
	let _this = this;
	let first = "";
	let sliders = "";
	let strings = "";
	let enums = "";
	let filters = this.attrs.featureFilters = this.attrs.featureFilters ??{};
	this.filterInfo = {};
	this.getFeatureInfo().forEach(info=>{
	    if(!info.showFilter()) {
		return;
	    }
	    this.filterInfo[info.id] = info;
	    let filter = filters[info.property] = filters[info.property]??{};
	    let id = info.getId();
	    let label = HU.span(['title',info.property],HU.b(info.getLabel()));
	    if(info.isString())  {
		filter.type="string";
		let attrs =['filter-property',info.property,'class','imdv-filter-string','id',this.domId('string_'+ id),'size',20];
		attrs.push('placeholder',this.getProperty(info.property.toLowerCase()+'.filterPlaceholder',''));
		let string=label+":<br>" +
		    HU.input("",filter.stringValue??"",attrs) +"<br>";
		if(info.getProperty('filter.first')) first+=string; else strings+=string;
		return
	    } 
	    if(info.samples.length)  {
		filter.type="enum";
		if(info.samples.length>1) {
		    let sorted = info.samples.sort((a,b)=>{
			return a.value.localeCompare(b.value);
		    });
		    let options = sorted.map(sample=>{
			let label = sample.label +' (' + info.seen[sample.value]+')';
			if(sample.value=='')
			    label = '&lt;blank&gt;' +' (' + info.seen[sample.value]+')';
			return {value:sample.value,label:label}
		    });
		    let line=label+":<br>" +
			HU.select("",['style','width:90%;','filter-property',info.property,'class','imdv-filter-enum','id',this.domId('enum_'+ id),'multiple',null,'size',Math.min(info.samples.length,5)],options,filter.enumValues,50)+"<br>";
		    if(info.getProperty('filter.first')) first+=line; else enums+=line;
		}
		return;
	    }


	    if((info.isNumeric() || info.isInt()) && (info.min<info.max)) {
		let min = info.getProperty('filter.min',info.min);
		let max = info.getProperty('filter.max',info.max);		
		filter.minValue = min;
		filter.maxValue = max;
		filter.type="range";


		let line =
		    HU.leftRightTable(HU.div(['id',this.domId('slider_min_'+ id),'style','max-width:50px;overflow-x:auto;'],Utils.formatNumber(filter.min??min)),
				      HU.div(['id',this.domId('slider_max_'+ id),'style','max-width:50px;overflow-x:auto;'],Utils.formatNumber(filter.max??max)));
		let slider =  HU.div(['slider-min',min,'slider-max',max,'slider-isint',info.isInt(),
			    'slider-value-min',filter.min??info.min,'slider-value-max',filter.max??info.max,
				      'filter-property',info.property,'feature-id',info.id,'class','imdv-filter-slider',
				      'style',HU.css("display","inline-block","width","100%")],"");
		if(info.getProperty('filter.animate',false)) {
		    line+=HU.table(['width','100%'],
				   HU.tr([],
					 HU.td(['width','18px'],HU.span(['feature-id',info.id,'class','imdv-filter-play ramadda-clickable','title','Play'],HU.getIconImage('fas fa-play'))) +
					 HU.td([],slider)));
		} else {
		    line+=slider;
		}


		line =  HU.b(label)+":<br>" +line;
		if(info.getProperty('filter.first')) first+=line; else sliders+=line;
	    }
	});


	if(sliders!='')
	    sliders = HU.div(['style',HU.css('margin-left','10px','margin-right','20px')],sliders);
	if(first!='')
	    first = HU.div(['style',HU.css('margin-left','10px','margin-right','20px')],first);	    

	let widgets = first+enums+sliders+strings;


	if(widgets!="") {
	    let update = () =>{
		this.display.featureHasBeenChanged = true;
		this.applyMapStyle(true);
		if($("#"+this.zoomonchangeid).is(':checked')) {
		    this.panMapTo();
		}
		this.updateFeaturesTable();
	    };

	    let clearAll = HU.span(['style','margin-right:5px;','class','ramadda-clickable','title','Clear Filters','id',this.domId('filters_clearall')],HU.getIconImage('fas fa-eraser',null,LEGEND_IMAGE_ATTRS));

	    this.zoomonchangeid = HU.getUniqueId("andzoom");

	    widgets = HU.div(['style','padding-bottom:5px;max-height:200px;overflow-y:auto;'], widgets);
	    let filtersHeader ='';
	    if(this.getProperty('filter.zoomonchange.show',true)) {
		filtersHeader = HU.checkbox(this.zoomonchangeid,['id',this.zoomonchangeid],this.getZoomOnChange(),"Zoom on change");
	    }
	    filtersHeader = HU.leftRightTable(filtersHeader, clearAll);

	    if(this.getProperty('filter.toggle.show',true)) {
		let toggle = HU.toggleBlockNew('Filters',filtersHeader + widgets,this.getFiltersVisible(),{separate:true,headerStyle:'display:inline-block;',callback:null});
		this.jq(ID_MAPFILTERS).html(HU.div(['style','margin-right:5px;'],toggle.header+toggle.body));
		HU.initToggleBlock(this.jq(ID_MAPFILTERS),(id,visible)=>{this.setFiltersVisible(visible);});
	    } else  {
		this.jq(ID_MAPFILTERS).html(HU.div(['style','margin-right:5px;'],filtersHeader  + 
						   widgets));
		this.setFiltersVisible(true);		    
	    }


	    jqid(this.zoomonchangeid).change(function() {
		_this.setZoomOnChange($(this).is(':checked'));
	    });

	    this.jq('filters_clearall').click(()=>{
		this.display.featureChanged();
		this.attrs.featureFilters = {};
		this.applyMapStyle();
		this.updateFeaturesTable();
	    });
	    this.jq(ID_MAPFILTERS).find('.imdv-filter-string').keypress(function(event) {
		let keycode = (event.keyCode ? event.keyCode : event.which);
                if (keycode == 13) {
		    let key = $(this).attr('filter-property');
		    let filter = filters[key]??{};
		    filter.type='string';
		    filter.stringValue = ($(this).val()??"").trim();
		    filter.property = key;
		    update();
		}
	    });
	    this.jq(ID_MAPFILTERS).find('.imdv-filter-enum').change(function(event) {
		let key = $(this).attr('filter-property');
		let filter = filters[key]??{};
		filter.property = key;
		filter.type='enum';
		filter.enumValues=$(this).val();
		update();
	    });

	    let sliderMap = {};
	    let sliders = this.jq(ID_MAPFILTERS).find('.imdv-filter-slider');
	    sliders.each(function() {
		let theFeatureId = $(this).attr('feature-id');
		let onSlide = function( event, ui, force) {
		    let id = theFeatureId;
		    let filter = filters[id]??{};
		    filter.min = +ui.values[0];
		    filter.max = +ui.values[1];			    
		    filter.property=id;
		    _this.jq('slider_min_'+ id).html(Utils.formatNumber(filter.min));
		    _this.jq('slider_max_'+ id).html(Utils.formatNumber(filter.max));			    
		    if(force || _this.getProperty(id+'.filter.live')) {
			update();
			return
		    }
		    if(!_this.sliderThrottle) 
			_this.sliderThrottle=Utils.throttle(()=>{
			    update();
			},1000);
		    _this.sliderThrottle();
		};
		let min = +$(this).attr('slider-min');
		let max = +$(this).attr('slider-max');
		let isInt = $(this).attr('slider-isint')=="true";
		let step = 1;
		let range = max-min;
		if(!isInt) {
		    step = range/100;
		} else {
		    if(range>10)
			step = Math.max(1,Math.floor(range/100));		    
		}
		sliderMap[$(this).attr('feature-id')] = {
		    slider:   $(this),
		    slide:onSlide,
		    min:min,
		    max:max,
		    step:step
		}

		$(this).slider({		
		    min: parseFloat(min),
		    max: parseFloat(max),
		    step:step,
		    values:[+$(this).attr('slider-value-min'),+$(this).attr('slider-value-max')],
		    slide: onSlide});
	    });

	    this.jq(ID_MAPFILTERS).find('.imdv-filter-play').click(function() {
		let playing = $(this).attr('playing');
		let info = _this.filterInfo[$(this).attr('feature-id')];
		if(!info) return;
		let animation = _this.animationInfo[info.id];
		if(!animation) {
		    _this.animationInfo[info.id] = animation  = {};
		}
		let sliderInfo = sliderMap[info.id];
		if(!sliderInfo) return;
		let slider = sliderInfo.slider;
		if(Utils.isDefined(playing) && playing==='true') {
		    $(this).html(HU.getIconImage('fas fa-play'));
		    $(this).attr('playing',false);
		    if(animation.timeout) {
			clearTimeout(animation.timeout);
			animation.timeout = null;
		    }
		} else {
		    $(this).html(HU.getIconImage('fas fa-stop'));
		    $(this).attr('playing',true);
		    let step = (_values) =>{
			let values = _values?? slider.slider('values');
			if(values[1]>=sliderInfo.max) {
			    $(this).html(HU.getIconImage('fas fa-play'));
			    $(this).attr('playing',false);			    
			    return;
			}
			values = [values[0],values[1]];
			let stepSize = parseFloat(info.getProperty('filter.animate.step',sliderInfo.step*2));
			values[1]=Math.min(sliderInfo.max,values[1]+stepSize);
			slider.slider('values',values);
			sliderInfo.slide({},{values:values},true);
			animation.timeout = setTimeout(step,info.getProperty('filter.animate.sleep',1000));
		    };
		    let values = slider.slider('values');
		    //If at the end then set to the start
		    if(values[1]>=sliderInfo.max) {
			values[1] = values[0];
			step(values);
		    } else {
			step();
		    }
		}

	    });

	}
    },
    handleMapLoaded: function(map,layer) {
	this.mapLoaded = true;
	this.makeFeatureFilters();
	this.applyMapStyle();
    },
    applyMacros:function(template, attributes, macros) {
	if(!macros) macros =  Utils.tokenizeMacros(template);
	if(attributes) {
	    let attrs={};
            for (let attr in attributes) {
		let value;
		if (typeof attributes[attr] == 'object' || typeof attributes[attr] == 'Object') {
                    let o = attributes[attr];
		    if(o)
			value = "" + o["value"];
		    else
			value = "";
		} else {
                    value = "" + attributes[attr];
		}
		let _attr = attr.toLowerCase();
		attrs[attr] = attrs[_attr] = value;			
	    }
	    template = macros.apply(attrs);
	}
	if(template.indexOf('${default}')>=0) {
	    let columns = [];
	    let labelMap = {};
	    this.getFeatureInfo().forEach(info=>{
		if(info.showPopup()) {
		    columns.push(info.property);
		    labelMap[info.property] = info.getLabel();
		}
	    });
	    let labelGetter = attr=>{
		return labelMap[attr];
	    }
	    template = template.replace('${default}',MapUtils.makeDefaultFeatureText(attributes,columns,
										     labelGetter));
	}
	return template;
    },

    applyMapStyle:function(skipLegendUI) {
	let _this = this;
	//If its a map then set the style on the map features
	if(!this.mapLayer || !this.mapLoaded) return;
	let features= this.mapLayer.features;
	if(!skipLegendUI && this.canDoMapStyle()) {
	    this.makeFeatureFilters();
	}

	let style = this.style;
	let rules = this.getMapStyleRules();
	let useRules = [];
	if(rules) {
	    rules = rules.filter(rule=>{
		if(rule.type=='use') {
		    useRules.push(rule);
		    return false;
		}
		return Utils.stringDefined(rule.property);
	    });
	}


	let featureFilters = this.attrs.featureFilters ??{};
	let rangeFilters = [];
	let stringFilters =[];
	let enumFilters =[];	
	for(a in featureFilters) {
	    let filter= featureFilters[a];
	    if(filter.type=="string") {
		if(Utils.stringDefined(filter.stringValue)) stringFilters.push(filter);
	    } else if(filter.type=="enum") {
		if(filter.enumValues && filter.enumValues.length>0) enumFilters.push(filter);
	    } else {
		if(Utils.isDefined(filter.min) || Utils.isDefined(filter.max)) {
		    if(filter.min!=filter.minValue || filter.max!=filter.maxValue) {
			rangeFilters.push(filter);
		    }
		}
	    }
	}

	features.forEach((feature,idx)=>{
	    feature.featureIndex = idx;
	});

	if(this.mapLabels) {
	    this.display.removeFeatures(this.mapLabels);
	    this.mapLabels = null;
	}

	//Add the map labels at the end after we call checkVisible
	let needToAddMapLabels = false;
	if(Utils.stringDefined(this.getMapLabelsTemplate())) {
	    needToAddMapLabels = true;
	    this.mapLabels = [];
	    let markerStyle = 	$.extend({},this.style);
	    markerStyle.pointRadius=0;
	    markerStyle.externalGraphic = null;
	    let template = this.getMapLabelsTemplate().replace(/\\n/g,'\n');
	    let macros = Utils.tokenizeMacros(template);
	    features.forEach((feature,idx)=>{
		let pt = feature.geometry.getCentroid(); 
		let style = $.extend({},markerStyle);
		style.label = this.applyMacros(template, feature.attributes,macros);
		let marker = MapUtils.createVector(pt,null,style);
		marker.point = pt;
		feature.mapPoint  = marker;
		this.mapLabels.push(marker);
	    });
	}

	//Apply the base style here
	this.mapLayer.style = style;
	features.forEach((f,idx)=>{
	    f.style = $.extend({},style);
	    f.originalStyle = $.extend({},style);			    
	});


	//Check for any rule based styles
	let attrs = features.length>0?features[0].attributes:{};
	let keys  = Object.keys(attrs);
	if(rules && rules.length>0) {
	    this.mapLayer.style = null;
	    this.mapLayer.styleMap = this.display.getMap().getVectorLayerStyleMap(this.mapLayer, style,rules);
	    features.forEach((f,idx)=>{f.style = null;});
	} 

	let applyColors = (obj,attr,strings)=>{
	    if(!obj || !Utils.stringDefined(obj?.property))  return;
	    let prop =obj.property;
	    let min =Number.MAX_VALUE;
	    let max =Number.MIN_VALUE;
	    let ct =Utils.getColorTable(obj.colorTable,true);
	    let anyNumber =  false;
	    features.forEach((f,idx)=>{
		let value = this.getFeatureValue(f,prop);
		if(isNaN(+value)) {
		    if(!strings.includes(value)) {
			strings.push(value);
		    }
		} else {
		    anyNumber =  true;
		    min = Math.min(min,value);
		    max = Math.max(max,value);		    
		}
	    });
		

	    if(!anyNumber) {
		obj.min =min = 0;
		obj.max = max= strings.length-1;
		obj.isEnumeration = true;
	    } else {
		obj.isEnumeration = false;
		obj.min = min;
		obj.max = max;		
	    }
	    strings = strings.sort((a,b)=>{
		return a.localeCompare(b);
	    });

	    let range = max-min;
	    features.forEach((f,idx)=>{
		let value = this.getFeatureValue(f,prop);
		if(!Utils.isDefined(value)) {
		    return;
		}
		let index;
		if(obj.isEnumeration) {
		    index = (strings.indexOf(value)%ct.length);
		} else {
		    value = +value;
		    let percent = (value-min)/range;
		    index = Math.max(0,Math.min(ct.length-1,Math.round(percent*ct.length)));
		}
		if(!f.style)
		    f.style = $.extend({},style);
		f.style[attr]=ct[index];
	    });
	};


	let fillStrings = [];
	let strokeStrings = [];		
	
	applyColors(this.attrs.fillColorBy,'fillColor',fillStrings);
	applyColors(this.attrs.strokeColorBy,'strokeColor',strokeStrings);	
	let addColor= (obj,prefix, strings) => {
	    if(obj && Utils.stringDefined(obj.property)) {
		let div = this.getColorTableDisplay(obj.colorTable,obj.min,obj.max,true,obj.isEnumeration, strings);
		div = HU.center(this.makeLabel(obj.property,true))+HU.center(div);

		this.jq('legendcolortable_'+prefix.toLowerCase()).html(div);
		if(obj.isEnumeration) {
		    if(!this.extraFilter) this.extraFilter = {};
		    let slices =jqid(this.domId('legendcolortable_'+ prefix.toLowerCase())).find('.display-colortable-slice'); 
		    slices.css('cursor','pointer');
		    slices.click(function() {
			let ct = Utils.ColorTables[obj.colorTable];
			let html = Utils.getColorTableDisplay(ct, 0,0, {
			    tooltips:strings,
			    showColorTableDots:true,
			    horizontal:false,
			    showRange: false,
			});
			html = HU.div(['style','max-height:200px;overflow-y:auto;margin:2px;'], html);
			let dialog = HU.makeDialog({content:html,title:HU.div(['style','margin-left:20px;margin-right:20px;'], _this.makeLabel(obj.property,true)+' Legend'),header:true,my:"left top",at:"left bottom",draggable:true,anchor:$(this)});
			let dots = dialog.find('.display-colortable-dot-item');
			dots.css({cursor:'pointer',title:'Click to show legend'});
			dots.addClass('ramadda-clickable');
			let select = jqid(_this.domId('enum_'+ Utils.makeId(obj.property)));

			select.find('option').each(function() {
			    if($(this).prop('selected')) {
				let value = $(this).prop('title');
				let dot = dialog.find('.display-colortable-dot-item[label="' +value+'"]');
				dot.addClass('display-colortable-dot-item-selected');
			    }
			});

			dots.click(function(event) {
			    let meta = event.metaKey || event.ctrlKey;
			    let label = $(this).attr('label');
			    let selected = $(this).hasClass('display-colortable-dot-item-selected');
			    let option = select.find('option[value="' +label+'"]');
			    if(!meta) {
				select.find('option').prop('selected',null);
				dots.removeClass('display-colortable-dot-item-selected');
			    }				
			    if(!selected) {
				$(this).addClass('display-colortable-dot-item-selected');
				option.prop('selected','selected');
			    } else {
				$(this).removeClass('display-colortable-dot-item-selected');
				option.prop('selected',null);
			    }
			    select.trigger('change');
			});
		    });
		}
	    }	
	};

	addColor(this.attrs.fillColorBy,'Fill',fillStrings);
	addColor(this.attrs.strokeColorBy,'Stroke',strokeStrings);	

	
	if(useRules.length>0) {
	    useRules.forEach(rule=>{
		let styles = [];
		let styleMap = {};
		Utils.split(rule.style,'\n',true,true).forEach(s=>{
		    let toks = [];
		    let index = s.indexOf(':');
		    if(index>=0) {
			toks.push(s.substring(0,index).trim());
			toks.push(s.substring(index+1).trim());			
		    }
		    if(toks.length!=2) return;
		    styleMap[toks[0].trim()] = toks[1].trim();
		    styles.push(toks[0].trim());
		});
		features.forEach((f,idx)=>{
		    if(!f.style) {
			f.style = $.extend({},style);
		    }
		    let value=this.getFeatureValue(f,rule.property);
		    if(!value) return;
		    styles.forEach(style=>{
			let v = styleMap[style];
			v = v.replace("${value}",value);
			if(v.startsWith('js:')) {
			    v = eval(v.substring(3));
			}
			f.style[style] = v;
		    });
		});
	    });
	}	    


	let redrawFeatures = false;
	features.forEach((f,idx)=>{
	    let visible = true;
	    rangeFilters.every(filter=>{
		let value=this.getFeatureValue(f,filter.property);
		if(Utils.isDefined(value)) {
		    visible = value>=filter.min && value<=filter.max;
		}
		return visible;
	    });
	    if(visible) {
		stringFilters.every(filter=>{
		    let value=this.getFeatureValue(f,filter.property)??'';
		    if(Utils.isDefined(value)) {
			value= String(value).toLowerCase();
			visible = value.indexOf(filter.stringValue)>=0;
		    }
		    return visible;
		});
	    }
	    if(visible) {
		enumFilters.every(filter=>{
		    let value=this.getFeatureValue(f,filter.property)??'';
		    visible =filter.enumValues.includes(value);
		    return visible;
		});
	    }		

	    if(f.mapPoint) {
		f.mapPoint.filtered =false;
	    }

	    f.isVisible  = visible;
	    
	    if(!visible) {
		if(!f.style) f.style={};
		f.style.display='none';
		if(f.mapPoint) {
		    redrawFeatures = true;
		    f.mapPoint.style.display='none';
		    f.mapPoint.filtered =true;
		}
	    }
	});

	if(this.attrs.fillColors) {
	    //	let ct = Utils.getColorTable('googlecharts',true);
	    let ct = Utils.getColorTable('d3_schemeCategory20',true);	
	    let cidx=0;
	    features.forEach((f,idx)=>{
		f.style = f.style??{};
		cidx++;
		if(cidx>=ct.length) cidx=0;
		f.style.fillColor=ct[cidx]
	    });
	}


	let indexToGroup = {
	};
	this.getStyleGroups().forEach(group=>{
	    group.indices.forEach(index=>{
		indexToGroup[index] = group;
	    });
	});

	features.forEach((f,idx)=>{
	    let group = indexToGroup[idx];
	    if(group) {
		$.extend(f.style,group.style)
	    }
	});
	this.checkVisible();

	if(needToAddMapLabels) {
	    this.display.addFeatures(this.mapLabels);
	}	    

	ImdvUtils.scheduleRedraw(this.mapLayer);
	if(redrawFeatures) {
	    this.display.redraw();
	}

    },

    applyStyle:function(style) {
	this.style = style;
	this.applyMapStyle();
	if(this.getMapServerLayer()) {
	    if(Utils.isDefined(style.opacity)) {
		this.getMapServerLayer().opacity = +style.opacity;
		this.getMapServerLayer().setVisibility(false);
		this.getMapServerLayer().setVisibility(true);
		ImdvUtils.scheduleRedraw(this.getMapServerLayer());
	    }
	}

	this.features.forEach(feature=>{
	    if(feature.style) {
		$.extend(feature.style,style);
	    }
	});	    
	if(this.isFixed()) {
	    this.addFixed();
	}
	this.display.featureChanged(true);
    },
    move:function(dx,dy) {
	if(this.getUseEntryLocation()) {
	    this.setUseEntryLocation(false);
	}
	this.features.forEach(feature=>{
	    feature.geometry.move(dx,dy);
	    feature.layer.drawFeature(feature);
	});
	if(this.image) {
	    this.image.extent.left+=dx;
	    this.image.extent.right+=dx;	    
	    this.image.extent.top+=dy;
	    this.image.extent.bottom+=dy;
	    this.image.moveTo(this.image.extent,true,true);
	}

	this.display.checkSelected(this);
    },
    removeImage:function() {
	if(this.image) {
	    this.display.getMap().removeLayer(this.image);
	    this.image=null;
	}
    },
    getMap:function() {
	return this.display.getMap();
    },
    checkImage:function(feature) {
	if(this.image && Utils.isDefined(this.image.opacity)) {
	    this.style.imageOpacity=this.image.opacity;
	}
	if(!this.style.imageUrl) return;
	let geometry = this.getGeometry() || feature?.geometry;
	if(!geometry) {
	    console.log("no image geometry");
	    return;
	}
	this.display.clearBounds(geometry);
	let b = geometry.getBounds();
	if(this.image) {
	    this.image.extent = b;
	    this.image.moveTo(b,true,true);
	} else {
	    b = this.getMap().transformProjBounds(b);
	    this.image=  this.getMap().addImageLayer(this.getName(),this.getName(),"",this.style.imageUrl,true,  b.top,b.left,b.bottom,b.right);
	    if(Utils.isDefined(this.style.imageOpacity))
		this.image.setOpacity(this.style.imageOpacity);
	}
    },
    getDisplayAttrs: function() {
	return this.attrs.displayAttrs;
    },
    applyDisplayAttrs: function(attrs) {
	if(this.displayInfo && this.displayInfo.display) {
	    this.displayInfo.display.deleteDisplay();
	    this.displayInfo.display = null;
	    jqid(this.displayInfo.divId).remove();
	    jqid(this.displayInfo.bottomDivId).remove();			
	}
	this.addData(attrs,false);
    },
    isVisible: function() {
	return this.attrs.visible??true;
    },
    setShowMarkerWhenNotVisible:function(v) {
	this.attrs.showMarkerWhenNotVisible = v;
	return this;
    },
    getZoomOnChange:function() {
	return this.attrs.zoomOnChange;
    },
    setZoomOnChange:function(v) {
	this.attrs.zoomOnChange = v;
	return this;
    },
    getMapPointsRange:function() {
	return this.attrs.mapPointsRange;
    },
    setMapPointsRange:function(v) {
	this.attrs.mapPointsRange  = v;
	return this;
    },    
    getMapLabelsTemplate:function() {
	return this.attrs.mapLabelsTemplate;
    },
    setMapLabelsTemplate:function(v) {
	this.attrs.mapLabelsTemplate = v;
	return this;
    },	   

    getShowMarkerWhenNotVisible:function() {
	return this.attrs.showMarkerWhenNotVisible;
    },
    getVisibleLevelRange:function() {
	return this.attrs.visibleLevelRange;
    },
    setVisible:function(visible,callCheck) {
	this.attrs.visible = visible;
	if(this.children) {
	    this.children.forEach(child=>{
		child.setVisible(visible, callCheck);
	    });
	}

	if(callCheck)
	    this.checkVisible();
	this.checkMapLayer();

	let legend = jqid(this.getId());
	if(this.getVisible()) 
	    legend.removeClass('imdv-legend-label-invisible');
	else
	    legend.addClass('imdv-legend-label-invisible');			    
    },
    getVisible:function() {
	if(!Utils.isDefined(this.attrs.visible)) this.attrs.visible = true;
	return this.attrs.visible;
    },
    setVisibleLevelRange:function(min,max) {
	let range = this.getVisibleLevelRange();
	let oldMin = range?.min;
	let oldMax = range?.max;	
	if(min===oldMin && max===oldMax) return;
	if(min=="") min = null;
	if(max=="") max = null;	
	this.attrs.visibleLevelRange = {min:min,max:max};
	this.checkVisible();
    },    

    checkVisible: function() {
	let showMarker = this.getShowMarkerWhenNotVisible();
	let range = this.getVisibleLevelRange()??{};
	let displayRange = this.display?.mapProperties.visibleLevelRange;
	if(!range || (displayRange && !Utils.stringDefined(range.min) && !Utils.stringDefined(range.max))) {
	    range = displayRange;
	    showMarker  = this.display.mapProperties.showMarkerWhenNotVisible;
	}
	let visible=true;
	let level = this.display.getCurrentLevel();
	let min = Utils.stringDefined(range.min)?+range.min:-1;
	let max = Utils.stringDefined(range.max)?+range.max:10000;
	visible =  this.getVisible() && (level>=min && level<=max);

	if(this.getVisible() && showMarker && !visible && !this.showMarkerMarker) {
	    let featuresToUse = this.features;
	    if(!featuresToUse || featuresToUse.length==0) {
		featuresToUse = this.mapLayer?.features
	    }		

	    let bounds = this.display.getMap().getFeaturesBounds(featuresToUse,true);
	    if(bounds) {
		let center = MapUtils.getCenter(bounds);
		this.showMarkerMarker = this.display.getMap().createMarker("", center, this.getIcon(), "",
									   null,null,16,null,null,{});
		this.display.addFeatures([this.showMarkerMarker]);
		this.showMarkerMarker.mapGlyph = this;
	    }
	}

	let setVis = (feature,vis)=>{
	    if(!Utils.isDefined(vis))  vis=visible;
	    if(feature.filtered)vis =false;
	    if(!feature.style) feature.style = {};
	    if(vis) {
		feature.style.display = 'inline';
	    }  else {
		feature.style.display = 'none';
	    }
	    $.extend(feature.style,{display:feature.style.display});
	};

	if(this.features) {
	    this.features.forEach(f=>{setVis(f);});
	}

	if(this.showMarkerMarker) {
	    if(!this.getVisible() || visible) {
		this.display.removeFeatures([this.showMarkerMarker]);
		this.showMarkerMarker = null;
	    }
	}

	if(this.isFixed()) {
	    if(visible)
		jqid(this.getFixedId()).show();
	    else
		jqid(this.getFixedId()).hide();
	}
	if(this.getMapLayer()) {
	    this.getMapLayer().setVisibility(visible);
	}
	if(this.getMapServerLayer()) {
	    this.getMapServerLayer().setVisibility(visible);
	}	

	if(this.selectDots && this.selectDots.length>0) {
	    this.selectDots.forEach(dot=>{
		dot.style.display = visible?'inline':'none';
	    });
	    ImdvUtils.scheduleRedraw(this.display.selectionLayer);
	}
	if(this.image) {
	    this.image.setVisibility(visible);
	}	
	if(this.displayInfo && this.displayInfo.display) {
	    this.displayInfo.display.setVisible(visible);
	}

	if(this.mapLabels && this.mapLoaded) {
	    this.mapLabels.forEach((point,idx)=>{setVis(point,true);});
	    if(!visible) {
		this.mapLabels.forEach((point,idx)=>{setVis(point,false);});
	    } else if(Utils.stringDefined(this.getMapPointsRange())) {
		if(level<parseInt(this.getMapPointsRange())) {
		    visible=false;
		    this.mapLabels.forEach((point,idx)=>{setVis(point,false);});
		}
	    } else {
		MapUtils.gridFilter(this.getMap(), this.mapLabels);
	    }
	}

	if(this.children) {
	    this.children.forEach(child=>{child.checkVisible();});
	}


	ImdvUtils.scheduleRedraw(this.display.myLayer);
    },    
    isShape:function() {
	if(this.getType()==GLYPH_LABEL) {
	    if(!Utils.stringDefined(this.style.externalGraphic)) return true;
	    if(this.style.externalGraphic && this.style.externalGraphic.endsWith("blank.gif")) return true;
	    if(this.style.pointRadius==0) return true;
	}
	return GLYPH_TYPES_SHAPES.includes(this.getType());
    },
    isData:function() {
	return this.type == GLYPH_DATA;
    },
    isFixed:function() {
	return this.type == GLYPH_FIXED;
    },    
    addFixed: function() {
	let style = this.style;
	let line = "solid";
	if(style.strokeDashstyle) {
	    if(['dot','dashdot'].includes(style.strokeDashstyle)) {
		line = "dotted";
	    } else  if(style.strokeDashstyle.indexOf("dash")>=0) {
		line = "dashed";
	    }
	}
	let css = HU.css('padding','5px');
	let color = Utils.stringDefined(style.borderColor)?style.borderColor:"#ccc";
	css+= HU.css('border' , style.borderWidth+"px" + " " + line+ " " +color);
	if(Utils.stringDefined(style.fillColor)) {
	    css+=HU.css("background",style.fillColor);
	}
	css+=HU.css('color',style.fontColor);
	if(Utils.stringDefined(style.fontSize)) {
	    css+=HU.css('font-size',style.fontSize);
	}

	['right','left','bottom','top'].forEach(d=>{
	    if(Utils.stringDefined(style[d])) css+=HU.css(d,HU.getDimension(style[d]));
	});
	let id = this.getFixedId();
	jqid(id).remove();
	let text = this.style.text??"";
	let html = HU.div(['id',id,CLASS,"ramadda-imdv-fixed",'style',css],"");
	this.display.jq(ID_MAP_CONTAINER).append(html);
	let toggleLabel = null;
	if(text.startsWith("toggle:")) {
	    text = text.trim();
	    let regexp = /toggle:(.*)\n/;
	    let match = text.match(regexp);
	    if(match) {
		toggleLabel=match[1];
		text = text.replace(regexp,"").trim();
	    }
	} else {
	    text = this.convertText(text);
	}

	if(text.startsWith("<wiki>")) {
	    this.display.wikify(text,null,wiki=>{
		if(toggleLabel)
		    wiki = HU.toggleBlock(toggleLabel+SPACE2, wiki,false);
		wiki = HU.div(['style','max-height:300px;overflow-y:auto;'],wiki);
		jqid(id).html(wiki)});
	} else {
	    text = text.replace(/\n/g,"<br>");
	    if(toggleLabel)
		text = HU.toggleBlock(toggleLabel+SPACE2, text,false);
	    jqid(id).html(text);
	}
    },

    addData:function(displayAttrs,andZoom) {
	displayAttrs = displayAttrs??{};
	displayAttrs.doInitCenter = andZoom??false;
	this.attrs.displayAttrs = displayAttrs;
	let entryId = this.getEntryId();
	let pointDataUrl = displayAttrs.pointDataUrl ||ramaddaBaseUrl+"/entry/data?max=50000&entryid=" + entryId;
	let pointData = new PointData(this.attrs.name,  null,null,
				      pointDataUrl,
				      {entryId:entryId});
	
	let divId   = HU.getUniqueId("display_");
	let bottomDivId   = HU.getUniqueId("displaybottom_");	    
	this.display.jq(ID_HEADER1).append(HU.div([ID,divId]));
	this.display.jq(ID_BOTTOM).append(HU.div([ID,bottomDivId]));	    
	let attrs = {"externalMap":this.display.getMap(),
		     "isContained":true,
		     "showRecordSelection":true,
		     "showInnerContents":false,
		     "entryIcon":this.attrs.icon,
		     "title":this.attrs.name,
		     "max":"5000",
		     "thisEntryType":this.attrs.entryType,
		     "entryId":entryId,
		     "divid":divId,
		     "bottomDiv":bottomDivId,			 
		     "data":pointData,
		     "fileUrl":ramaddaBaseUrl+"/entry/get?entryid=" + entryId+"&fileinline=true"};
	$.extend(attrs,displayAttrs);
	attrs = $.extend({},attrs);
	attrs.name=this.getName();
let display = this.display.getDisplayManager().createDisplay("map",attrs);
	//Not sure why we do this since we can't integrate charts with map record selection
	//	display.setProperty("showRecordSelection",false);

	display.errorMessageHandler = (display,msg) =>{
	    this.display.setErrorMessage(msg,5000);
	};
	this.displayInfo =   {
	    display:display,
	    divId:divId,
	    bottomDivId: bottomDivId
	};
    },
    getDecoration:function(small) {
	let type = this.getType();
	let style = this.style??{};
	let css= ['display','inline-block'];
	let dim = small?'10px':'25px';
	css.push('width',small?'10px':'50px');
	let line = "solid";
	if(style.strokeWidth>0) {
	    if(style.strokeDashstyle) {
		if(['dot','dashdot'].includes(style.strokeDashstyle)) {
		    line = "dotted";
		} else  if(style.strokeDashstyle.indexOf("dash")>=0) {
		    line = "dashed";
		}
	    }
	    css.push('border',(small?Math.min(+style.strokeWidth,1):style.strokeWidth)+"px " + line +" " + style.strokeColor);
	}

	if(style.imageUrl) {
	    if(!small) 
		return HU.toggleBlock(style.imageUrl, HU.image(style.imageUrl,["width","200px"]));
	} else if(type==GLYPH_LABEL) {
	    if(!small)
		return style.label.replace(/\n/g,"<br>");
	} else if(type==GLYPH_MARKER) {
	    if(!small)
		return HU.image(style.externalGraphic,['width','16px']);
	} else if(type==GLYPH_BOX) {
	    if(Utils.stringDefined(style.fillColor)) {
		css.push('background',style.fillColor);
	    }
	    css.push('height',dim);
	    return HU.div(['style',HU.css(css)]);
	} else if(type==GLYPH_HEXAGON) {
	    css=[];
	    if(Utils.stringDefined(style.fillColor)) {
		css.push('background',style.fillColor);
	    }
	    if(Utils.stringDefined(style.strokeColor)) {
		css.push('color',style.strokeColor);
	    }		
	    css.push('font-size',small?'16px':'32px','vertical-align','center');
	    return HU.span(['style',HU.css(css)],"&#x2B22;");
	} else if(type==GLYPH_CIRCLE || type==GLYPH_POINT) {
	    if(Utils.stringDefined(style.fillColor)) {
		css.push('background',style.fillColor);
	    }
	    if(type==GLYPH_POINT) {
		css.push('margin-top','10px','height','10px','width','10px');
	    } else {
		css.push('height',dim);
		css.push('width',dim);
	    }		    
	    return HU.div(['class','ramadda-dot', 'style',HU.css(css)]);
	} else if(type==GLYPH_LINE ||
		  type==GLYPH_POLYLINE ||
		  type==GLYPH_POLYGON ||
		  type==GLYPH_FREEHAND_CLOSED ||
		  type==GLYPH_MAP
		  || type==GLYPH_ROUTE
		  ||  type==GLYPH_FREEHAND) {
	    if(this.isClosed()) {
		if(type==GLYPH_FREEHAND_CLOSED)
		    css.push('border-radius','10px');
		css.push('height','10px');
		//		css.push('margin-top','10px','margin-bottom','10px');
		css.push('background',style.fillColor);
	    } else {
		css.push('margin-bottom','4px','border-bottom', style.borderWidth+"px" + " " + line+ " " +style.strokeColor);
	    }
	    return HU.div(['style',HU.css(css)]);
	}
	return HU.div(['style',HU.css('display','inline-block','border','1px solid transparent','width',small?'10px':'50px')]);
    },
    isClosed: function() {
	return GLYPH_TYPES_CLOSED.includes(this.type);
    },
    isOpenLine:function() {
	return GLYPH_TYPES_LINES_OPEN.includes(this.type);
    },
    addEntries: function(andZoom) {
	let entryId = this.getEntryId();
        let entry =  new Entry({
            id: entryId,
        });
	if(this.features)
	    this.display.removeFeatures(this.features);
	this.features =[];
        let callback = (entries)=>{
	    this.entries = entries;
	    entries.forEach((e,idx)=>{
		if(!e.hasLocation()) return;
		let  pt = MapUtils.createPoint(e.getLongitude(),e.getLatitude());
		pt = this.display.getMap().transformLLPoint(pt);
		let style = $.extend({},this.style);
		style.externalGraphic = e.getIconUrl();
		style.strokeWidth=1;
		style.strokeColor="transparent";
		/*
		let bgstyle = $.extend({},style);
		bgstyle = $.extend(bgstyle,{externalGraphic:ramaddaBaseUrl+"/images/white.png"});
		bgstyle.label = null;
		let bgpt = MapUtils.createPoint(pt.x,pt.y);
		let bg = MapUtils.createVector(bgpt,null,bgstyle);
		bg.noSelect = true;
		//		bg.mapGlyph=this;
		//		bg.entryId = e.getId();
		this.features.push(bg);
		*/
		if(style.showLabels) {
		    let label  =e.getName();
		    let toks = Utils.split(label," ",true,true);
		    if(toks.length>1) {
			label = "";
			Utils.splitList(toks,3).forEach(l=>{
			    label += Utils.join(l," ");
			    label+="\n";
			})
			label = label.trim();
		    }
		    style.label=label;
		} else {
		    style.label=null;
		}
		let marker = MapUtils.createVector(pt,null,style);


		let attrs = {name:e.getName(),
			     entryglyphs:e.mapglyphs,
			     entryId:e.getId()
			    };
		let mapGlyph = new MapGlyph(this.display,GLYPH_MARKER, attrs, marker,style);
		marker.mapGlyph = this;
		marker.entryId = e.getId();
		if(this.getShowMultiData()) {
		    mapGlyph.applyEntryGlyphs();
		}
		this.features.push(marker);
	    });
	    this.display.addFeatures(this.features);
	    this.checkVisible();
	    if(andZoom)
		this.panMapTo();
	    this.showMultiEntries();
	};
	entry.getChildrenEntries(callback);
    },
    isSelected:function() {
	return this.selected;
    },
    getSelected:function(selected) {
	if(this.isSelected()) {
	    selected.push(this);
	}
	if(this.children) {
	    this.children.forEach(child=>{child.getSelected(selected);});
	}
    },

    select:function(maxPoints,dontRedraw) {
	if(!Utils.isDefined(maxPoints)) maxPoints = 20;
	if(this.isSelected()) {
	    console.log('already selected');
	    return;
	}
	this.selected = true;
	this.selectDots = [];
	let pointCount = 0;
	let mapLayer = this.getMapLayer();
	if(mapLayer && mapLayer.features) {
	    let style={
		strokeColor:'#000',
		strokeWidth:2,
		fillColor:'transparent'
	    };
	    mapLayer.features.forEach(f=>{
		f.originalStyle = f.style;
		f.style = style;
	    });
	    ImdvUtils.scheduleRedraw(this.mapLayer);
	}	    

	let image = this.getImage();
	if(image) {
	    let ext = image.extent;
	    [[ext.left,ext.top],[ext.right,ext.top],[ext.left,ext.bottom],[ext.right,ext.bottom]].forEach(tuple=>{
                let pt = MapUtils.createPoint(tuple[0],tuple[1]);
		let dot = MapUtils.createVector(pt,null,this.display.DOT_STYLE);	
		this.selectDots.push(dot);
	    });
	} else {
	    pointCount+=this.display.selectFeatures(this,this.getFeatures(),maxPoints);
	}
	this.display.selectionLayer.addFeatures(this.selectDots,{silent:true});
	if(this.children) {
	    this.children.forEach(child=>{
		pointCount+=child.select(maxPoints, dontRedraw);
	    });
	}
	return pointCount;
    },
    unselect:function() {
	if(!this.isSelected()) return;
	this.selected = false;
	if(this.selectDots) {
	    this.display.selectionLayer.removeFeatures(this.selectDots);
	    this.selectDots= null;
	}

	if(this.mapLayer && this.mapLayer.features) {
	    this.applyMapStyle(true);
	}

	if(this.children) {
	    this.children.forEach(child=>{
		child.unselect();
	    });
	}	

    },
    
    doRemove:function() {
	if(this.isFixed()) {
	    jqid(this.getFixedId()).remove();
	}
	if(this.mapLabels) {
	    this.display.removeFeatures(this.mapLabels);
	}
	if(this.features) {
	    this.display.removeFeatures(this.features);
	}
	if(this.showMarkerMarker) {
	    this.display.removeFeatures([this.showMarkerMarker]);
	    this.showMarkerMarker = null;
	}
	if(this.selectDots) {
	    this.display.selectionLayer.removeFeatures(this.selectDots);
	    this.selectDots = null;
	}

	if(this.getImage()) {
	    this.display.getMap().removeLayer(this.getImage());
	    this.image =null;
	}

	if(this.getMapLayer()) {
	    this.display.getMap().removeLayer(this.getMapLayer());
	    this.setMapLayer(null);
	}
	if(this.getMapServerLayer()) {
	    this.display.getMap().removeLayer(this.getMapServerLayer());
	    this.setMapServerLayer(null);
	}
	if(this.displayInfo) {
	    jqid(this.displayInfo.divId).remove();
	    jqid(this.displayInfo.bottomDivId).remove();			
	    if(this.displayInfo.display) {
		this.displayInfo.display.deleteDisplay();
	    }
	}

	this.setParentGlyph(null);

	if(this.children) {
	    let tmp = [...this.children];
	    tmp.forEach(child=>{
		child.doRemove();
	    });
	}
    }
}


function RamaddaEditablemapDisplay(displayManager, id, properties) {
    const SUPER = new RamaddaImdvDisplay(displayManager,  id,  properties);
    RamaddaUtil.inherit(this,SUPER);
}


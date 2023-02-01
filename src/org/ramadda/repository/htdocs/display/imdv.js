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
var GLYPH_RINGS = 'rings';
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
var IMDV_PROPERTY_HINTS= ['filter.live=true','filter.show=false',
			  'filter.zoomonchange.show=false',
			  'filter.toggle.show=false','showButtons=false'];


let ImdvUtils = {
    applyFeatureStyle:function(feature,style) {
	if(!feature.style) {
	    feature.style=style;
	    return;
	}
	let geom = feature?.geometry?.CLASS_NAME;
	if(geom=='OpenLayers.Geometry.Point' && Utils.stringDefined(feature.style.externalGraphic)) {
	    for(a in style) {
		//TODO:
	    }
	} else {
	    feature.style= style;
	}
    },
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
	if(!layer) return;
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
	    if(!cancel) {
		this.display.handleNewFeature(this.display.getNewFeature());
	    }
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
	properties.showOpacitySlider=false; 
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
	{p:'externalGraphic',d:'/map/blue-dot.png'},
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
	    mapOptions = $.extend({},mapOptions);
	    if(feature && feature.style && feature.style.mapOptions)
		delete feature.style.mapOptions;
	    let mapGlyph = new MapGlyph(this,mapOptions.type, mapOptions, feature,style);
	    this.addGlyph(mapGlyph);
	    mapGlyph.glyphCreated();
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

	makeRangeRings:function(center,radii,style,angle,ringStyle) {
	    style = style??{};
	    let rings = [];
	    let labelStyle = {labelAlign:style.labelAlign??'lt',
			      fontSize:style.fontSize??'10pt',
			      fontColor:style.fontColor??'#000'};
	    for(a in style) {
		if(a.indexOf('label')>=0|| a.indexOf('font')>=0 || a.indexOf('textBackground')>=0) {
		    labelStyle[a] = style[a];
		}
	    }
	    for(let idx=radii.length-1;idx>=0;idx--) {
		let km =radii[idx];
		if(km==null) continue;
		let skm = String(km).trim();
		if(skm.endsWith('mi')) {
		    km = 1.60934*parseFloat(skm.replace('mi',''));
		} else if (skm.endsWith('ft')) {
		    km = 0.0003048*parseFloat(skm.replace('ft',''));
		} else if (skm.endsWith('m')) {
		    km = parseFloat(skm.replace('m',''))/1000;
		} else if (skm.endsWith('km')) {
		    km = skm.replace('km','');
		}
		let p1 = MapUtils.createLonLat(center.lon??center.x, center.lat??center.y);
		let p2 = Utils.reverseBearing(p1,Utils.isDefined(angle)?angle:90+45,km);
		if(p2==null) {
		    console.error("Could not create range rings with center:",center,p2,km);
		    return null;
		}
		p1 = this.getMap().transformLLPoint(p1);
		p2 = this.getMap().transformLLPoint(p2);
		let dist = Utils.distance(p1.lon,p1.lat,p2.lon,p2.lat);
		let ring = OpenLayers.Geometry.Polygon.createRegularPolygon({x:p1.lon,y:p1.lat},
									    dist, 100,0);
		let _style = $.extend({},style??{strokeWidth:2,strokeColor:'blue',fillColor:'transparent'});
		if(ringStyle['*']) {
		    $.extend(_style,ringStyle['*']);
		}
		if(idx==2*(parseInt(idx/2))) {
		    if(ringStyle['even']) {
			$.extend(_style,ringStyle['even']);
		    }
		} else {
		    if(ringStyle['odd']) {
			$.extend(_style,ringStyle['odd']);
		    }
		}
		if(ringStyle && ringStyle[idx+1]) {
		    $.extend(_style,ringStyle[idx+1]);
		}
		if(idx==radii.length-1 && ringStyle['N']) {
		    $.extend(_style,ringStyle['N']);
		}

		rings.push(MapUtils.createVector(ring,null,_style));


		p2 = MapUtils.createPoint(p2.lon,p2.lat);
		let s = $.extend({},labelStyle);
		s.label=skm;
		let label = MapUtils.createVector(p2,null,s);
		rings.push(label);
	    }
	    return rings;
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
		if(total>3500) {
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
		if(this.mapServerDialog) {
		    this.mapServerDialog.show();
		    return;
		}
		let wmtHtml = '';
		let form = (label,name,size)=>{
		    wmtHtml+=HU.formEntry(label+':',HU.input('',this.cache[name]??'',['id',this.domId(name),'size',size??'60']));
		}
		let args = [
		    ['Name','servername'],
		    ['Server URL','serverurl'],
		    ['WMS Layer','wmslayer','20'],
		    ['Legend URL','maplegend']
		];

		this.cache = this.cache??{};
		wmtHtml +=  HU.formTable();
		args.forEach(a=>{
		    form(a[0],a[1],a[2]);
		});
		wmtHtml+='</table>';
		let contents = [];
		let ids =Utils.mergeLists([{value:'',label:'Select'}],RAMADDA_MAP_LAYERS.map(l=>{return [l.id,l.name]}));
		let predefined =  HU.select('',['id',this.domId('predefined')],ids,this.cache['predefined']);	
		let html = 'Pre-defined layer: ' + predefined;
		html+='<p>Or enter either a TMS/WMS server URL with a layer name:';
		html+=wmtHtml;
		let buttons = HU.buttons([
		    HU.div([CLASS,'ramadda-button-ok display-button'], 'OK'),
		    HU.div([CLASS,'ramadda-button-cancel display-button'], 'Cancel')]);
		html+=buttons;
		contents.push({label:'WMS/WMTS',contents:html});

		let datacube = HU.div(['id',this.domId('datacube_contents')],'Loading...');
		contents.push({label:'Data Cubes',contents:datacube});

		let stac = HU.div(['id',this.domId('stac_contents')]);
		contents.push({label:'STAC',contents: stac});

		let tabs = HU.makeTabs(contents)
		html=HU.div(['style','min-width:600px;min-height:400px;margin:10px;'], tabs.contents);

		let dialog = this.mapServerDialog = HU.makeDialog({remove:false,content:html,title:'Map Server',header:true,my:'left top',at:'left bottom',draggable:true,anchor:this.jq(ID_MENU_NEW)});
		//We don't want to remove the dialog, just show it
		dialog.remove= () =>{
		    dialog.hide();
		}
		tabs.init();
		this.initDatacube(dialog);
		this.initStac(dialog);
		let cancel = ()=>{
		    dialog.hide();
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
		    this.clearMessage2(1000);

		    dialog.remove();
		}
		dialog.find('.ramadda-button-ok').button().click(ok);
		dialog.find('.ramadda-button-cancel').button().click(()=>{
		    dialog.hide();
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
		let dialog =  HU.makeDialog({content:html,title:'Marker',header:true,my:'left top',at:'left bottom',draggable:true,anchor:this.jq(ID_MENU_NEW)});

		let closeDialog = () =>{
		    dialog.remove();
		}

		this.addIconSelection(this.jq('icons'));
		let cancel = ()=>{
		    closeDialog();
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
		    closeDialog();
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
		    closeDialog();
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
	
	initStac:function(dialog) {
	    let load;
	    let stac= HU.div(['id',this.domId('stac_top')]) +
		HU.div(['id',this.domId('stac_output')]);
	    this.jq('stac_contents').html(stac);
	    let catalogUrl = ramaddaBaseUrl+'/resources/stac_catalogs.json';
	    let stacLinks = [{value:'',label:'Select'}, {value:catalogUrl,label:'Stac Catalogs'},
			     {value:ramaddaBaseUrl+'/resources/pc_catalog.json',label:'Planet Earth Catalogs'}];
	    let seenStacUrls = {};
	    seenStacUrls[catalogUrl]=true;
	    let makeTop=(current)=>{ 
		let input = this.jq('stac_input').val()??'';
		let plus = HU.span(['id',this.domId('stac_add'),'class','ramadda-clickable','title','Add a STAC catalog URL'],HU.getIconImage('fas fa-plus'));
		let back  = HU.span(['id',this.domId('stac_back'),'class','ramadda-clickable','title','Go back'],HU.getIconImage('fas fa-rotate-left'));

		let top = back +' ' + plus+' '+HU.select("",['style','max-width:500px;overflow:none;','id',this.domId('stac_url')],stacLinks,current,100);
		top = HU.div(['style','border-bottom:1px solid #ddd;padding-bottom:6px;margin-bottom:6px;'], top);
		this.jq('stac_top').html(top);
		this.jq('stac_url').change(()=>{
		    let url = this.jq('stac_url').val();
		    if(Utils.stringDefined(url)) {
			load(url);
		    }
		});
		this.jq('stac_input').keypress((event)=>{
                    if (event.which == 13) {
			let url = this.jq('stac_input').val();
			if(!Utils.stringDefined(url)) {
			    return;
			}
			load(url);
		    }
		});
		this.jq('stac_back').click(()=>{
		    if(!this.currentStacUrl) return;
		    let index = stacLinks.findIndex(item=>{return item.value==this.currentStacUrl});
		    if(index>0) {
			load(stacLinks[index-1].value);
		    }
		});


		this.jq('stac_add').click(function() {
		    let link = HU.href('https://stacindex.org/catalogs',HU.getIconImage('fas fa-binoculars'),['target','_stacindex','title','Look for catatalogs on stacindex.org']);
		    let input = HU.input('','',['id',_this.domId('stac_add_url'),'style','width:400px;'])+' ' +link;
		    let html = HU.b('STAC  Catalog URL: ') + input;
		    html+= HU.buttons([
			HU.div([CLASS,'stac-add-ok display-button'], 'OK'),
			HU.div([CLASS,'stac-add-cancel display-button'], 'Cancel')]);
		    html=HU.div(['style','margin:5px;margin-top:10px;'],html);
		    let dialog =  HU.makeDialog({content:html,anchor:$(this),remove:false,xmodal:true,sticky:true});

		    _this.jq('stac_add_url').keypress((event)=>{
			if (event.which == 13) {
			    let url = _this.jq('stac_add_url').val();
			    if(Utils.stringDefined(url))
				load(url);
			    dialog.remove();
			}
		    });
		    dialog.find('.display-button').button().click(function() {
			if($(this).hasClass('stac-add-ok')) {
			    let url = _this.jq('stac_add_url').val();
			    if(Utils.stringDefined(url))
				load(url);
			}
			dialog.remove();
		    });
		});



		this.jq('stac_go').button().click(()=>{
		    let url = this.jq('stac_input').val();
		    if(!Utils.stringDefined(url)) {
			url = this.jq('stac_url').val();
		    }
		    if(!Utils.stringDefined(url)) {
			return;
		    }
		    load(url);
		});
	    };
	    makeTop();

	    let _this = this;
	    let showStac=(data,url)=>{
		//is it the stac_catalogs.json
		if(Array.isArray(data)) {
		    data = {
			title:'Stac Catalogs',
			links:data
		    }
		}
		    
		console.dir(data);
		let html = '';
		let title = (data.title??url)+HU.space(1) + HU.href(url,HU.getIconImage('fas fa-link',[],['style','font-size:9pt;']),['target','_stac']);
		html+=HU.center(HU.b(title));
		if(data.description) {
		    let desc = Utils.stripTags(data.description);
//		    console.log('BEFORE:'+desc);
		    desc = desc.replace(/[\n\s*\n]\n+/g,'\n').trim();
//		    console.log("AFTER:" +desc);
		    desc = desc.replace(/[\n\n]+/g,'\n').replace(/\n/g,'<br>');
		    html+=HU.div(['class','boxquote','style','max-width:600px;overflow-z:auto;max-height:100px;overflow-y:auto;'],desc);
		}
		let linksHtml1 ='';
		let linksHtml2 ='';		
		if(data.links) {
		    let cnt = 0;
		    data.links.forEach(link=>{
			let url = link.href??link.url??link.link;
			if(!Utils.stringDefined(url) ||link.rel=='self') return;
			let label = link.title;
			if(!label)
			    label  = url.replace(/.*\/([^\/]+$)/,"$1");
			let href = HU.href(url,HU.getIconImage('fas fa-link',[],['title',link.summary??'','style','font-size:9pt;'])+' '+label+(link.rel?' ('+link.rel+')':''),['target','_stactarget','class','ramadda-clickable']);

			let isJson;
			if(Utils.isDefined(link.variables)) {
			    isJson = true;
			} else if(Utils.stringDefined(link.type)) {
			    isJson  = link.type == 'application/json' || link.type == 'application/geo+json';
			} else {
			    //Some guess work
			    if(link.rel)
				isJson = ['next','search','parent','root','child','items','search'].includes(link.rel);
			    if(!isJson) isJson = url.endsWith('json');
			}

			if(!isJson) {
			    linksHtml2+=HU.tr(HU.td(['width','10%','nowrap','true'],'')+HU.td(href));
			} else {
			    linksHtml1+=HU.tr(HU.td(['width','10%','nowrap','true'],
					     HU.div(['title',url,'class','imdv-stac-item'],HU.span(['link',url,'class','imdv-stac-link'], 'Load'))) +
					     HU.td(href));
			}

		    });
		}

		let assetsHtml = '';
		if(data.assets) {
		    let table='';
		    let images = '';
		    let other = '';
		    Object.keys(data.assets).forEach((key,idx)=>{
			console.log(key)
			//limit the number
			if(idx>200) return;
			let asset = data.assets[key];
			if(!Utils.stringDefined(asset.href)) return;
			if(table=='') {
			    table+=HU.b('Assets: ')+'note: the IMDV does not handle all projections. Try the thumbnail<br>';
			    table +='<table width=100%>';
			}

			let label = asset.name??asset.title??key;
			let link = HU.href(asset.href,HU.getIconImage('fas fa-link',[],['style','font-size:9pt;'])+' ' +label +' ('+ asset.type+')',['title',asset.href,'target','_stactarget','class','ramadda-clickable']);
			let type= asset.type??asset.media_type;
			let isImage = type&& type.indexOf('image')>=0;

			if(asset.href.endsWith('ovr')) isImage= false;
			if(isImage) {
			    images+=HU.tr(HU.td(['width','10%','nowrap','true'], HU.div(['class','imdv-stac-item'],HU.span(['asset-id',key,'class','imdv-stac-asset'], 'Add Image')))+
					  HU.td(link));
			} else {
			    other+=HU.tr(HU.td('')+ HU.td(link));
			}
		    });
		    table+=images;
		    table+=other;
		    table+'</table>';
		    assetsHtml=table;
		}
		let linksHtml = linksHtml1+linksHtml2;
		if(linksHtml!='') {
		    html+=HU.b('Links:<br>');
		    linksHtml=HU.table(linksHtml);
		    if(assetsHtml!='') 
			html+=HU.div(['style','max-height:5em;overflow-y:auto;'], linksHtml);
		    else
			html+= linksHtml;		    
		}

		html+=assetsHtml;
		html=HU.div(['style','max-height:300px;overflow-y:auto;'], html);
		this.jq('stac_output').html(html);
		this.jq('stac_output').find('.imdv-stac-link').button().click(function() {
		    load($(this).attr('link'));
		});
		this.jq('stac_output').find('.imdv-stac-asset').button().click(function() {
		    let bbox=data.bbox;
		    if(!data.bbox) {
			console.dir(data);
			bbox = data?.extent?.spatial?.bbox;
			if(bbox) {
			    bbox=bbox[0];
			    if(bbox) bbox = [bbox[0],bbox[3],bbox[2],bbox[1]];
			}
		    }
		    if(!bbox) {
			alert('No bbox found');
			return
		    }
		    let asset =  data.assets[$(this).attr('asset-id')];
		    let url = asset.href;
		    if(asset.type&& asset.type.indexOf('image/tiff')>=0) {
			url =   ramaddaBaseUrl+'/tifftopng?url=' + encodeURIComponent(url);
		    }
		    let attrs = {
			type:GLYPH_MAP,
			entryType:'stacimage',
			icon:'/repository/icons/mapfile.png',
			name:asset.name??asset.title,
			bbox:bbox,
			resourceUrl:url
		    }
		    let mapGlyph = _this.handleNewFeature(null,{rotation:0,transform:''},attrs);
		    mapGlyph.checkMapLayer(true);
		});
	    };
	    load = (url) =>{
		if(!Utils.stringDefined(url)) return;
		this.currentStacUrl = url
		this.jq('stac_url').val(url);
		let _this = this;
		this.jq('stac_output').html('Loading...');
		$.getJSON(url, data=>{
		    if(!seenStacUrls[url]) {
			seenStacUrls[url]=true;
			stacLinks.push({value:url,label:data.title??url});
			makeTop(url);
		    }
		    showStac(data,url);
		}).fail(err=>{
//		    console.dir(err);
		    JSON.parse(err.responseText)
		    this.jq('stac_output').html('Load failed. URL: ' + url);
		});
	    }
	},
	initDatacube:function(dialog) {
	    let _this = this;
	    let datacube= HU.div(['id',this.domId('datacube_top')]) +
		HU.div(['id',this.domId('datacube_output')]);
	    this.jq('datacube_contents').html(datacube);

	    let load;
	    let datacubeLinks = [{value:'',label:'Select'}];
	    MapUtils.getMapProperty('datacubeservers','').split(',').forEach(c=>{
		datacubeLinks.push(c);
	    });

	    let makeTop=(current)=>{ 
		let input = this.jq('datacube_input').val()??'';
		let plus = HU.span(['id',this.domId('datacube_add'),'class','ramadda-clickable','title','Add a Data Cube server URL'],HU.getIconImage('fas fa-plus'));
		let top =plus +' ' +HU.select("",['style','max-width:500px;overflow:none;','id',this.domId('datacube_url')],datacubeLinks,current,100);
		top = HU.div(['style','border-bottom:1px solid #ddd;padding-bottom:6px;margin-bottom:6px;'], top);
		this.jq('datacube_top').html(top);

		this.jq('datacube_add').click(function() {
		    let input = HU.input('','',['id',_this.domId('datacube_add_url'),'style','width:400px;']);
		    let html = HU.b('DATACUBE  Catalog URL: ') + input;
		    html+= HU.buttons([
			HU.div([CLASS,'datacube-add-ok display-button'], 'OK'),
			HU.div([CLASS,'datacube-add-cancel display-button'], 'Cancel')]);
		    html=HU.div(['style','margin:5px;margin-top:10px;'],html);
		    let dialog =  HU.makeDialog({content:html,anchor:$(this),remove:false,xmodal:true,sticky:true});

		    let add = ()=>{
			let url = _this.jq('datacube_add_url').val();
			if(Utils.stringDefined(url)) {
			    load(url);
			    if(!datacubeLinks.includes(url)) {
				datacubeLinks.push(url);
				makeTop(url);
			    }
			}
			dialog.remove();
		    }
		    _this.jq('datacube_add_url').keypress((event)=>{
			if (event.which == 13) {
			    add();
			}
		    });
		    dialog.find('.display-button').button().click(function() {
			if($(this).hasClass('datacube-add-ok')) {
			    add();
			}
			dialog.remove();
		    });
		});


		this.jq('datacube_url').change(()=>{
		    let url = this.jq('datacube_url').val();
		    if(Utils.stringDefined(url)) {
			load(url);
		    }
		});
	    };
	    makeTop();
	    let showDatacube=(data,baseUrl)=>{
		let html='';
		html += HU.formTable();
		let selects = [];
		let variableMap = {}
		let idToMaps = {}	    
		let selectStyle='max-width:300px;overflow-x:hidden;'
		data.datasets.forEach((dataset,idx)=>{
		    let variables  = {};
		    variableMap[''+idx] = variables;
		    if(!dataset.variables) return;
		    let items = [{label:'Select Variable',value:''}];
		    dataset.variables.forEach(v=>{
			variables[v.id] =v;
			items.push({label:v.title,value:v.id});
		    });
		    let selectId = HU.getUniqueId('select_');
		    selects.push(selectId);	
		    html+=HU.formEntry(dataset.title+':',
				       HU.select("",['style',selectStyle,'id',selectId,'dataset',idx],items));
		    if(dataset.placeGroups) {
			let maps=[{value:'',label:'Select'}];
			dataset.placeGroups.forEach((group,idx)=>{
			    let url = baseUrl+'/places/' + group.id
			    let uid  = Utils.getUniqueId('map');
			    idToMaps[uid] = {label:group.title,value:group.id,url:url};
			    maps.push({label:group.title,value:uid});
			});
			if(maps.length>1) {
			    let selectId = HU.getUniqueId('mapselect_');
			    selects.push(selectId);	
			    html+=HU.formEntry("Maps:",
					       HU.select("",['ismap','true','style',selectStyle,'id',selectId],maps));
			}
		    }
		});
		html+='</table>';
		html = HU.div(['style','auto;max-height:400px;overflow-y:auto;'], html);
		html+= HU.buttons([
		    HU.div([CLASS,'ramadda-button-ok-datacube display-button'], 'OK'),
		    HU.div([CLASS,'ramadda-button-cancel-datacube display-button'], 'Cancel')]);
		
		let datacubeDialog=this.jq('datacube_output');
		datacubeDialog.html(html);
		datacubeDialog.find('.ramadda-button-ok-datacube').button().click(()=>{
		    let variable;
		    let mapInfo
		    selects.every(sid=>{
			let select=jqid(sid);
			let id = select.val();
			if(Utils.stringDefined(id)) {
			    if(select.attr('ismap')) {
				mapInfo  = idToMaps[id];
			    } else {
				variable=variableMap[select.attr('dataset')][id];
			    }
			    return false;
			}
			return true;
		    });
		    dialog.hide();
		    if(mapInfo) {
			let glyphType = this.getGlyphType(GLYPH_MAP);
			let attrs = {
			    type:GLYPH_MAP,
			    entryType:'geo_geojson',
			    icon:'/repository/icons/mapfile.png',
			    name:mapInfo.label,
			    resourceUrl:mapInfo.url,
			}
			let mapGlyph = this.handleNewFeature(null,glyphType.getStyle(),attrs);
			mapGlyph.checkMapLayer();
			return
		    }		    
		    if(variable) {
			let url = variable.tileUrl;
			url = url.replace('http:','https:');
			url=HU.url(url,['crs','EPSG:3857'])+'&cbar={colorbar}&vmin={vmin}&vmax={vmax}&time={time}';
			delete variable.htmlRepr;
			let mapOptions = {name:variable.title,
					  variable:variable};
			this.clearCommands();
			mapOptions.icon = ramaddaBaseUrl+'/icons/xcube.png';
			mapOptions.type=GLYPH_MAPSERVER;
			let mapGlyph = new MapGlyph(this,GLYPH_MAPSERVER, mapOptions, null,{});
			mapGlyph.setMapServerUrl(url,'','','');
			mapGlyph.checkMapServer();
			this.addGlyph(mapGlyph);
			this.clearMessage2(1000);
		    }
		});
		datacubeDialog.find('.ramadda-button-cancel-datacube').button().click(()=>{
		    dialog.hide();
		});
	    };

	    load = (url) => {
		if(!Utils.stringDefined(url)) return;
		let baseUrl = url
		//https://api.earthsystemdatalab.net/api/datasets?details=1
		if(url.indexOf('/api/datasets')<0) {
		    url = url +'/datasets?details=1';
		}
		this.jq('datacube_output').html('Loading...');
		console.log(url);
		$.getJSON(url, data=>{
		    showDatacube(data,baseUrl);
		}).fail(err=>{
		    console.error('Failed loading datacube datasets:' + err);
		});
	    };
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
		if(line=='<hr>') {
		    help+='<thin_hr>';
		    return
		}
		if(line.info) {
		    help+=HU.div(['class','ramadda-clickable imdv-property-popup','target',target,
				  'info-id',line.info], line.title);
		    return;
		}
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
		    if(mapGlyph.getImage().imageHook) {
			mapGlyph.getImage().imageHook();
		    }
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
	

	getFillPatternSelect:function(domId,v) {
	    let patterns = [
		'',
		'circles-1',
		'circles-2',
		'circles-3',
		'circles-4',
		'circles-5',
		'circles-6',
		'circles-7',
		'circles-8',
		'circles-9',
		'diagonal-stripe-1',
		'diagonal-stripe-2',
		'diagonal-stripe-3',
		'diagonal-stripe-4',
		'diagonal-stripe-5',
		'diagonal-stripe-6',
		'dots-1',
		'dots-2',
		'dots-3',
		'dots-4',
		'dots-5',
		'dots-6',
		'dots-7',
		'dots-8',
		'dots-9',
		'horizontal-stripe-1',
		'horizontal-stripe-2',
		'horizontal-stripe-3',
		'horizontal-stripe-4',
		'horizontal-stripe-5',
		'horizontal-stripe-6',
		'horizontal-stripe-7',
		'horizontal-stripe-8',
		'horizontal-stripe-9',
		'vertical-stripe-1',
		'vertical-stripe-2',
		'vertical-stripe-3',
		'vertical-stripe-4',
		'vertical-stripe-5',
		'vertical-stripe-6',
		'vertical-stripe-7',
		'vertical-stripe-8',
		'vertical-stripe-9',
		'crosshatch',
		'houndstooth',
		'lightstripe',
		'smalldot',
		'verticalstripe',
		'whitecarbon'];
	    let opts = patterns.map(p=>{
		if(p=='') return {value:'',label:'None'};
		return {value:p,label:p};
	    });
	    return  HU.select("",['id',domId],opts,v);
	},


	makeStyleForm:function(style,mapGlyph) {
	    let html = "";
	    let props;
	    let values = {};
	    html +=HU.formTable();
	    if(mapGlyph) {
		html+=(mapGlyph.addToStyleDialog(style)??'');
	    }

	    if(style) {
		props = [];
		let isImage = style.imageUrl;
		for(a in style) {
		    if(isImage) {
			if(a!='transform' && a!='rotation' && a!="imageUrl" && a!="imageOpacity" && a!="popupText") continue;
		    }
		    props.push(a);
		    values[a] = style[a];
		}
		//		if(mapGlyph && mapGlyph.getType()==GLYPH_MARKER) {props = ["pointRadius","externalGraphic"];} 
	    } else {
		props = ['strokeColor','strokeWidth','strokeDashstyle','strokeOpacity',
			 'fillColor','fillOpacity','fillPattern',
			 'pointRadius','externalGraphic','graphicName',
			 'imageOpacity','fontSize','fontWeight','fontStyle','fontFamily','labelAlign','labelXOffset','labelYOffset'];
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
		textBackgroundStrokeColor: {label:'Text Background',strip:'textBackground'},		
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
			widget =  HU.textarea("",v,[ID,domId,"rows",3,"cols", 60]);
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
		    } else if(prop=='textBackgroundShape') {
			widget = HU.select('',['id',domId],['rectangle','circle','ellipse'],v);
		    } else {
			if(props == "pointRadius") label="Size";
			if(prop=="textBackgroundFillOpacity" || prop=="textBackgroundPadding" || prop=="strokeWidth" || prop=="pointRadius" || prop=="fontSize" || prop=="imageOpacity") size="4";
			else if(prop=="fontFamily") size="60";
			else if(prop.toLowerCase().indexOf('url')>=0) size="60";
			if(prop.indexOf("Color")>=0) {
			    widget =  HU.input("",v,['class','ramadda-imdv-color',ID,domId,"size",8]);
			    widget =  HU.div(['id',domId+'_display','class','ramadda-dot', 'style',HU.css('background',Utils.stringDefined(v)?v:'transparent')]) +
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
			} else if(prop=='graphicName') {
			    label = 'Graphic';
			    let g = ['','circle', 'square', 'star', 'x', 'cross', 'triangle',
				     'lightning','rectangle','church','_x','arrow','plane','arrow'];
			    let opts = g.map(p=>{
				if(p=='') return {value:'',label:'None'};
				return {value:p,label:p};
			    });
			    widget =  HU.select("",['id',domId],opts,v);			
			} else if(prop=='fillPattern') {
			    widget = this.getFillPatternSelect(domId,v);
			} else if(prop.indexOf('Radius')>=0 ||
				  prop.indexOf("Width")>=0 ||
				  prop.indexOf("Padding")>=0 ||
				  prop.indexOf("Offset")>=0 ||
				  prop=="rotation") {
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
	    let accord;
	    if(mapGlyph) {
		accord= HU.makeTabs(content);
		html+=accord.contents;
	    } else {
		html+=HU.center(HU.b('Default Style')) + content[0].contents;
	    }
	    html+=buttons;
	    html  = HU.div(['style',HU.css('min-width','700px','min-height','400px'),CLASS,"wiki-editor-popup"], html);
	    this.map.ignoreKeyEvents = true;
	    let dialog = HU.makeDialog({content:html,anchor:this.jq(ID_MENU_FILE),title:"Map Properties" + (mapGlyph?": "+mapGlyph.getName():""),header:true,draggable:true,resizable:true});
	    if(accord) 
		accord.init();
	    if(mapGlyph)
		mapGlyph.initSideHelp(dialog);
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
			if(!Utils.stringDefined(value)) {
			    this.setProperty(prop, null);
			    return;
			}
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
	    //for backwards compatabity
	    if(type=='label') {
		mapOptions.type = type =GLYPH_MARKER;

	    }
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
	    

	    let lines = ['legendLabel=Some label',...IMDV_PROPERTY_HINTS];
	    let help = 'Add property:' + this.makeSideHelp(lines,this.domId('otherproperties_input'),{suffix:'\n'});
	    accords.push({header:'Other Properties',
			  contents:
			  HU.hbox([
			      HU.textarea('',props,['id',this.domId('otherproperties_input'),'rows','8','cols','40']),HU.space(2),help])
			 });
	    

//	    let accord = HU.makeAccordionHtml(accords);
	    let accord = HU.makeTabs(accords);	    
	    let html = buttons + accord.contents;
	    html  = HU.div(['style','min-width:700px;min-height:300px;margin:10px;'],html);
	    let anchor = this.jq(ID_MENU_FILE);
	    let dialog = HU.makeDialog({content:html,title:'Properties',header:true,
					my:'left top',at:'left bottom',draggable:true,anchor:anchor});

	    this.initSideHelp(dialog);
	    accord.init();
//	    HU.makeAccordion('#'+accord.id);
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
	    //	    html+= this.menuItem(this.domId(ID_REFRESH),"Refresh");	    
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
	    let tmp = Utils.splitList(this.glyphTypes,this.glyphTypes.length/3);
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
		if(opts.resourceUrl.indexOf('ramadda.org')>=0)
		    url =   ramaddaBaseUrl+'/proxy?url=' + encodeURIComponent(opts.resourceUrl);
		else
		    url =   opts.resourceUrl;
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
		this.makeLegend()
	    }
	    switch(opts.entryType) {
	    case 'stacimage': 
		//TODO: handle the 3d bbox with 6 values
		let west,south,east,north;
		if(opts.bbox.length==4)
		    [west,south,east,north] = opts.bbox;
		else
		    [west, south, lowest, east, north, highest] = opts.bbox;		    
		console.log("BBOX:" + west,south,east,north);
		let iw = 2048;
		let ih = 1024;
		//sanity check for bad bbox
		if(north<-90) {
		    [south,west,north,east] = opts.bbox;		    
		}
//		console.log("north:" +north+" west:" +west +" south:" +south +" east:" + east);
		let ilayer =  this.getMap().addImageLayer('', opts.name,"",url,true,
							  north,west,south,east, iw,ih,{},loadCallback);
		ilayer.mapGlyph = mapGlyph;
		mapGlyph.initImageLayer(ilayer);
		mapGlyph.handleMapLoaded(this.getMap(),ilayer);
		if(andZoom)
		    this.getMap().zoomToLayer(ilayer);
		return ilayer;
		
	    case 'latlonimage': 
		let w = 2048;
		let h = 1024;
		return this.getMap().addImageLayer(opts.entryId, opts.name,"",url,true,
						   opts.north, opts.west,opts.south,opts.east, w,h);
	    case 'geo_gpx': 
		return this.getMap().addGpxLayer(opts.name,url,true, selectCallback, unselectCallback,style,loadCallback,andZoom,errorCallback);
		break;
	    case 'geo_shapefile_fips': 
	    case 'geo_shapefile': 
		url = ramaddaBaseUrl+'/entry/show?entryid=' + opts.entryId+'&output=geojson.geojson&formap=true';
		//fall thru to geojson

	    case 'geo_geojson': 
		return this.getMap().addGeoJsonLayer(opts.name,url,true, selectCallback, unselectCallback,style,loadCallback,andZoom,errorCallback);
		break;		

		/*
		  case 'geo_shapefile_fips': 
		  case 'geo_shapefile': 
		  url = ramaddaBaseUrl+'/entry/show?entryid=' + opts.entryId+'&output=shapefile.kml&formap=true';
		*/

	    case 'geo_kml': 
		let loadCallback2 = (map,layer)=>{
		    if(layer.features) {
			layer.features.forEach(f=>{
			    ImdvUtils.applyFeatureStyle(f,style);
			});
		    }
		    loadCallback(map,layer);
		};
		url =  ramaddaBaseUrl+"/entry/show?entryid=" + opts.entryId +"&output=kml.doc&converthref=true";

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
			   textBackgroundStrokeColor:'',
			   textBackgroundStrokeWidth:1,			   			   
			   textBackgroundFillColor:'',
			   textBackgroundFillOpacity:1.0,
			   textBackgroundPadding:2,
			   textBackgroundShape:'rectangle',
			   textBackgroundRadius:0
			  }, MyPoint,
			  {icon:ramaddaBaseUrl+"/map/blue-dot.png"});

	    new GlyphType(this,GLYPH_POINT,"Point",
			  {graphicName:'circle',
			   pointRadius:6,
			   strokeColor:'blue',
			   strokeWidth:1,
			   strokeOpacity:1,
			   strokeDashstyle:'solid',
			   fillColor:"blue",
			   fillOpacity:1,
			   rotation:0,
			   fontColor: this.getProperty("labelFontColor","#000"),
			   fontSize: this.getFontSize(),
			   fontFamily: this.getFontFamily(),
			   fontWeight: this.getFontWeight(),
			   fontStyle: this.getFontStyle(),
			   label:'',
			   labelAlign: this.getProperty("labelAlign","cb"),
			   labelXOffset: this.getProperty("labelXOffset","0"),
			   labelYOffset: this.getProperty("labelYOffset","14"),
			   labelOutlineColor:this.getProperty("labelOutlineColor","#fff"),
			   labelOutlineWidth: this.getProperty("labelOutlineWidth","0"),
			  },
			  MyPoint,
			  {icon:ramaddaBaseUrl+"/icons/dots/blue.png"});
	    new GlyphType(this,GLYPH_RINGS,"Range Rings",
			  {strokeColor:'blue',
			   strokeWidth:2,
			   fillColor:"transparent",
			   pointRadius:6,
			   rotation:0,
			   fontColor: this.getProperty("labelFontColor","#000"),
			   fontSize: this.getFontSize(),
			   fontFamily: this.getFontFamily(),
			   fontWeight: this.getFontWeight(),
			   fontStyle: this.getFontStyle(),
			   labelAlign:'lt',
			   textBackgroundStrokeColor:'',
			   textBackgroundStrokeWidth:1,			   			   
			   textBackgroundFillColor:'',
			   textBackgroundFillOpacity:1.0,
			   textBackgroundPadding:2,
			   textBackgroundShape:'rectangle',
			   textBackgroundRadius:0
			  },
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
			   fillOpacity:1.0,
			   fillPattern:''},
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
			   fillOpacity:1.0,fillPattern:''},
			  MyPolygon,
			  {freehand:true,icon:ramaddaBaseUrl+"/icons/freehandclosed.png"});

	    new GlyphType(this,GLYPH_BOX, "Box",
			  {strokeColor:this.getStrokeColor(),
			   strokeWidth:this.getStrokeWidth(),
			   strokeDashstyle:'solid',
			   strokeOpacity:1,
			   fillColor:"transparent",
			   fillOpacity:1.0,fillPattern:''},
			  MyRegularPolygon,
			  {snapAngle:90,sides:4,irregular:true,
			   icon:ramaddaBaseUrl+"/icons/rectangle.png"});
	    new GlyphType(this,GLYPH_CIRCLE, "Circle",
			  {strokeColor:this.getStrokeColor(),
			   strokeWidth:this.getStrokeWidth(),
			   strokeDashstyle:'solid',
			   strokeOpacity:1,
			   fillColor:"transparent",
			   fillOpacity:1.0,fillPattern:''},
			  MyRegularPolygon,
			  {snapAngle:45,sides:40,icon:ramaddaBaseUrl+"/icons/ellipse.png"});

	    new GlyphType(this,GLYPH_TRIANGLE, "Triangle",
			  {strokeColor:this.getStrokeColor(),
			   strokeWidth:this.getStrokeWidth(),
			   strokeDashstyle:'solid',
			   strokeOpacity:1,
			   fillColor:"transparent",
			   fillOpacity:1.0,fillPattern:''},
			  MyRegularPolygon,
			  {snapAngle:10,sides:3,
			   icon:ramaddaBaseUrl+"/icons/triangle.png"});				
	    new GlyphType(this,GLYPH_HEXAGON, "Hexagon",
			  {strokeColor:this.getStrokeColor(),
			   strokeWidth:this.getStrokeWidth(),
			   strokeDashstyle:'solid',
			   strokeOpacity:1,
			   fillColor:"transparent",
			   fillOpacity:1.0,fillPattern:''},
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
			   fillPattern:'',
			   pointRadius:6,
			   externalGraphic:'',
			   graphicName:'',
			   fontColor: this.getProperty("labelFontColor","#000"),
			   fontSize: this.getFontSize(),
			   fontFamily: this.getFontFamily(),
			   fontWeight: this.getFontWeight(),
			   fontStyle: this.getFontStyle(),
			   labelAlign: 'cm',
			   labelXOffset: '0',
			   labelYOffset: '10',
			   labelOutlineColor:'#fff',
			   labelOutlineWidth: '0',
			   textBackgroundStrokeColor:'',
			   textBackgroundStrokeWidth:1,			   			   
			   textBackgroundFillColor:'',
			   textBackgroundFillOpacity:1.0,
			   textBackgroundPadding:2,
			   textBackgroundShape:'rectangle',
			   textBackgroundRadius:0
			  },
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
			   fillColor:"transparent",
			   rotation:0,
			   transform:''},
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
	    });
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
	    if(Utils.isDefined(this.mapProperties[name])) {
		return this.mapProperties[name];
	    }
	    let value = this.getOtherProperties()[name];
	    if(Utils.isDefined(value)) {
		return Utils.getProperty(value);
	    }
	    return  this.getProperty(name,dflt);
	},
	makeLegendDroppable:function(droppedOnGlyph,label,notify) {
	    notify = notify?? (()=>{this.setLastDroppedTime(new Date());});
	    label.droppable( {
		hoverClass: 'imdv-legend-item-droppable',
		accept:'.imdv-legend-item',
		drop: (event,ui)=>{
		    notify();
		    let draggedGlyph = this.findGlyph(ui.draggable.attr('glyphid'));
		    if(!draggedGlyph) {
			console.log('Could not find dragged glyph');
			return;
		    }
		    this.handleDroppedGlyph(draggedGlyph,droppedOnGlyph);
		}
	    });
	},
	handleDroppedGlyph:function(draggedGlyph,droppedOnGlyph) {
	    let debug = false;
	    if(this.handleDropTimeout) {
		if(debug)	    console.log('clearing pending drop');
		clearTimeout(this.handleDropTimeout);
	    }
	    this.handleDropTimeout = setTimeout(()=>{
		if(debug)	    console.log(this.name +' handleDrop');
		this.handleDropTimeout = null;
		this.removeMapGlyph(draggedGlyph);
		draggedGlyph.setParentGlyph(null);
		if(droppedOnGlyph) {
		    if(droppedOnGlyph.isGroup()) {
			if(debug)		console.log('landed on group');
			droppedOnGlyph.addChildGlyph(draggedGlyph);
			draggedGlyph.changeOrder(false);
		    } else {
			if(droppedOnGlyph.getParentGlyph()) {
			    if(debug) console.log('landed on glyph in a group');
			    droppedOnGlyph.getParentGlyph().addChildGlyph(draggedGlyph);
			    this.moveGlyphBefore(droppedOnGlyph, 
						 draggedGlyph,
						 droppedOnGlyph.getParentGlyph().getChildren());
			} else {
			    this.moveGlyphBefore(droppedOnGlyph, draggedGlyph);
			}
		    }
		} else {
		    draggedGlyph.setParentGlyph(null);
		    Utils.removeItem(this.getGlyphs(),draggedGlyph);
		    this.getGlyphs().push(draggedGlyph);
		    draggedGlyph.changeOrder(false);
		    this.featureChanged();
		    this.redraw();
		}
		this.handleGlyphsChanged();
		this.redraw();
	    },1);
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
	    let baseIndex = 100;
	    glyphs.forEach((mapGlyph,idx)=>{
		baseIndex = mapGlyph.setLayerLevel(baseIndex);
	    });
	    this.getMap().checkLayerOrder();

	    glyphs.forEach((mapGlyph,idx)=>{
		html+=mapGlyph.makeLegend({idToGlyph:idToGlyph});
	    });
	    if(glyphs.length)
		html+=HU.div(['id',this.domId('dropend'),'class','imdv-legend-item','style','width:100%;height:1em;'],'');

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

	    this.makeLegendDroppable(null,this.jq('dropend'),null);

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
	    //Always on top
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
			HU.div(['style','display:inline-block;','id',this.domId(ID_ADDRESS_CLEAR),'title','Clear','class','ramadda-clickable'],HU.getIconImage('fa-solid fa-eraser',[],['style','color:#ccc;'])) +
			' ' +
			HU.div(['id',this.domId(ID_ADDRESS_WAIT),'style',HU.css('position','absolute','right','0px',
										'display','inline-block','margin-right','2px','width','20px')])+
			HU.input('','',['id',this.domId(ID_ADDRESS_INPUT),'placeholder','Search for address','size','20']));

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
				      if(file.name.match('.*\.(json|geojson|gpx|zip|kml|kmz)')) return true;
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
    isRings: function() {
	return this.type == GLYPH_RINGS;
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

    if(this.isRings()) {
	this.checkRings();
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
    initSideHelp:function(dialog) {
	let _this = this;
	dialog.find('.imdv-property-popup').click(function() {
	    let id  = $(this).attr('info-id');
	    let target  = $(this).attr('target');	    
	    let info = _this.getFeatureInfo(id);
	    if(!info) return;
	    let html = HU.b(info.getLabel());
	    let items =   ['show=true','label=','filter.first=true','type=enum']
	    if(info.isNumeric()) {
		items.push('format.decimals=0',
			   'filter.min=0',
			   'filter.max=100',
			   'filter.animate=true',
			   'filter.animate.step=1',
			   'filter.animate.sleep=100',
			   'filter.live=true');

	    }
	    items.push('colortable.select=true');

	    items.forEach(item=>{
		let label = item.replace('=.*','');
		html+=HU.div(['style','margin-left:10px;', 'class','ramadda-menu-item ramadda-clickable','item',item],item);
	    });

	    html = HU.div(['style','margin-left:10px;margin-right:10px;'],html);
	    let dialog =  HU.makeDialog({content:html, anchor:$(this)});
	    dialog.find('.ramadda-clickable').click(function() {
		dialog.remove();
		let item = $(this).attr('item');
		let line = info.id+'.' + item+'\n';
		let textComp = GuiUtils.getDomObject(target);
		if(textComp) {
		    insertAtCursor('', textComp.obj, line);
		}
	    });
	});
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
    getRadii:function() {
	if(!this.attrs.radii) {
	    let level = this.display.getCurrentLevel();
//	    console.log("level:" + level);
	    let r = (size,unit) =>{
		let s =[];
		for(let i=1;i<=5;i++) {
		    s.push((size*i)+unit);
		}
		this.attrs.radii = s;
		return s;
	    }
	    if(level>=19) return r(25,'ft');
	    if(level>=18) return r(50,'ft');
	    if(level>=17) return r(75,'ft');	    
	    if(level>=16) return r(150,'ft');
	    if(level>=15) return r(250,'ft');	    
	    if(level>=14) return r(500,'ft');
	    if(level>=13) return r(1500,'ft');
	    if(level>=12) return r(0.5,'mi');
	    if(level>=11) return r(1,'mi');
	    if(level>=10) return r(2,'mi');
	    if(level>=9) return r(2.5,'mi');	    	    	    
	    if(level>=8) return r(5,'mi');
	    if(level>=7) return r(10,'mi');
	    if(level>=6) return r(25,'mi');
	    if(level>=5) return r(50,'mi');
	    if(level>=4) return r(100,'mi');	    	    	    	    
	    if(level>=3) return r(250,'mi');	    	    	    	    
	    return	 r(500,'mi');
	}
	return this.attrs.radii;
    },
    addToStyleDialog:function(style) {
	if(this.isRings()) {
	    return HU.formEntry('Rings Radii:',HU.input('',Utils.join(this.getRadii(),','),
							['id',this.domId('radii'),'size','40'])+' e.g., 1km, 2mi (miles), 100ft') +
		HU.formEntry('Ring label angle:',
			     HU.input('',Utils.isDefined(this.attrs.rangeRingAngle)?this.attrs.rangeRingAngle:90+45,[
				 'id',this.domId('rangeringangle'),'size',4])) +
		HU.formEntryTop('Ring Styles',
				HU.hbox([HU.textarea('',this.attrs.rangeRingStyle??'',['id',this.domId('rangeringstyle'),'rows',5,'cols', 40]),
					 'Format:<br>ring #,style:value,style:value  e.g.:<br>1,fillColor:red,strokeColor:blue<br>2,strokeDashstyle:dot|dash|dashdot|longdash<br>N,strokeColor:black<br>*,strokeWidth:5<br>even,...<br>odd,...']));
	}
	if(this.isMapServer() && this.getDatacubeVariable()) {
	    return HU.formEntry('Color Table:',HU.div(['id',this.domId('colortableproperties')]));
	}	    
	return '';
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
	let featureInfo = this.getFeatureInfoList();
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
	let miscLines =[...IMDV_PROPERTY_HINTS];
	miscLines.push('<hr>');
	this.getFeatureInfoList().forEach(info=>{
	    //	    miscLines.push({line:info.id+'.show=true',title:info.property});
	    miscLines.push({info:info.id,title:info.getLabel()});	    
	});

	let miscHelp =this.display.makeSideHelp(miscLines,this.domId('miscproperties'),{suffix:'\n'});
	let ex = 'Add property:' + miscHelp;

	html += HU.hbox([HU.textarea('',this.attrs.properties??'',[ID,this.domId('miscproperties'),'rows',6,'cols', 40]),
			 HU.space(2),ex]);
	content.push({header:'Flags',contents:html});
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

	if(this.isMapServer()  && this.getDatacubeVariable()) {
	    if(this.currentColorbar!=this.getDatacubeVariable().colorBarName) {
		this.getDatacubeVariable().colorBarName = this.currentColorbar;
		this.mapServerLayer.url = this.getMapServerUrl();
		this.mapServerLayer.redraw();
	    }
	}
	if(this.isRings()) {
	    this.attrs.radii=Utils.split(this.jq('radii').val()??'',',',true,true);
	    this.attrs.rangeRingAngle=this.jq('rangeringangle').val();
	    this.attrs.rangeRingStyle = this.jq('rangeringstyle').val();
	    if(this.features.length>0) this.features[0].style.strokeColor='transparent';
	}
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
    glyphCreated:function() {
	this.applyEntryGlyphs();
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
    addFeatures: function(features,andClear,addToDisplay) {
	if(andClear) this.clearFeatures();
	this.features.push(...features);
	features.forEach(feature=>{
	    feature.mapGlyph = this;   
	});

	if(addToDisplay) {
	    this.display.addFeatures(features);
	}
    },

    addFeature: function(feature,andClear) {
	this.addFeatures([feature],andClear);
    },
    getStyle: function() {
	return this.style;
    },
    panMapTo: function(andZoomIn) {
	let bounds = this.getBounds();
	if(bounds) {
	    this.display.getMap().zoomToExtent(bounds);
	}
	if(andZoomIn) {
	    this.display.getMap().setZoom(16);
	}
    },
    getBounds: function() {
	let bounds = null;
	if(this.isMultiEntry() && this.entries) {
	    this.entries.forEach(entry=>{
		let b = null;
		if(entry.hasBounds()) {
		    b =   MapUtils.createBounds(entry.getWest(),entry.getSouth(),entry.getEast(), entry.getNorth());
		} else if(entry.hasLocation()) {
		    b =   MapUtils.createBounds(entry.getLongitude(),entry.getLatitude(),
						entry.getLongitude(),entry.getLatitude());
		} 
		if(b) {
		    bounds = MapUtils.extendBounds(bounds,b);
		}
	    });
	    if(bounds) {
		return this.display.getMap().transformLLBounds(bounds);
	    }
	    return null;
	}

	if(this.children) {
	    this.children.forEach(child=>{
		bounds =  MapUtils.extendBounds(bounds,child.getBounds());
	    });
	} else 	if(this.features && this.features.length) {
	    bounds = this.display.getMap().getFeaturesBounds(this.features);
	    if(this.rings) {
		bounds = MapUtils.extendBounds(bounds,
					       this.display.getMap().getFeaturesBounds(this.rings));
	    }
	} if(this.isMapServer()) {
	    if(this.getDatacubeVariable() && Utils.isDefined(this.getDatacubeAttr('geospatial_lat_min'))) {
		let attrs = this.getDatacubeAttrs();
		bounds= MapUtils.createBounds(attrs.geospatial_lon_min, attrs.geospatial_lat_min, attrs.geospatial_lon_max, attrs.geospatial_lat_max);
		bounds= this.display.getMap().transformLLBounds(bounds);
	    }
	} else if(this.getMapLayer()) {
	    if(this.getMapLayer().getVisibility()) {
		bounds =  this.display.getMap().getFeaturesBounds(this.getMapLayer().features);
		if(!bounds) {
		    if(this.getMapLayer().maxExtent) {
			let e = this.getMapLayer().maxExtent;
			bounds = MapUtils.createBounds(e.left,e.bottom,e.right,e.top);
		    }
		}
	    }
	    if(this.imageLayers) {
		this.imageLayers.forEach(obj=>{
		    if(!obj.layer || !obj.layer.getVisibility()) return;
		    bounds = MapUtils.extendBounds(bounds,
						   this.getMap().getLayerVisbileExtent(obj.layer)||obj.layer.extent);
		});
	    }
	} else	if(this.displayInfo?.display) {
	    if(this.displayInfo.display.myFeatureLayer && (
		!Utils.isDefined(this.displayInfo.display.layerVisible) ||
		    this.displayInfo.display.layerVisible)) {
		bounds =  this.display.getMap().getFeaturesBounds(this.displayInfo.display.myFeatureLayer.features);
	    } else  if(this.displayInfo.display.pointBounds) {
		bounds= this.display.getMap().transformLLBounds(this.displayInfo.display.pointBounds);
	    }
	}
	return bounds;
    },


    collectFeatures: function(features) {
	if(this.children) {
	    this.children.forEach(child=>{
		child.collectFeatures(features);
	    });
	} else 	if(this.features.length) {
	    features.push(...this.features);
	} else if(this.getMapLayer()) {
	    let f = this.getMapLayer().features;
	    if(f)
		features.push(...f);
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
    hasBounds:function() {
	if(this.isMapServer()) {
	    if(this.getDatacubeVariable() && Utils.isDefined(this.getDatacubeAttr('geospatial_lat_min'))) {
		return true;
	    }
	    return false;
	}
	return  !this.isFixed();
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
	    //For now don't add the decoration (the graphic indicator)
	    //	    right+=this.getDecoration(true);
	}

	if(glyphType) {
	    let icon = Utils.getStringDefined([this.style.externalGraphic,this.attrs.icon,glyphType.getIcon()]);
	    if(icon.startsWith('data:')) icon = this.attrs.icon;
	    if(icon && icon.endsWith('blank.gif')) icon = glyphType.getIcon();
	    icon = HU.image(icon,['width','18px']);
	    if(url && forLegend)
		icon = HU.href(url,icon,['target','_entry']);
	    let showZoomTo = forLegend && this.hasBounds();
	    if(showZoomTo) {
		right+=SPACE+
		    HU.span([CLASS,'ramadda-clickable imdv-legend-item-view',
			     'glyphid',this.getId(),
			     TITLE,'Click:Move to; Shift-click:Zoom in',],
			    HU.getIconImage('fas fa-magnifying-glass',[],LEGEND_IMAGE_ATTRS));
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
    

    getLegendDiv:function() {
	return this.jq('legend_');
    },
    setLayerLevel:function(level) {
	if(this.getMapLayer()) {
	    this.getMapLayer().ramaddaLayerIndex=level++;
	}
	if(this.imageLayers) {
	    this.imageLayers.forEach(obj=>{
		if(obj.layer) {
		    obj.layer.ramaddaLayerIndex=level++;
		}
	    });
	}
	if(this.mapServerLayer) {
	    this.mapServerLayer.ramaddaLayerIndex=level++;
	}

	if(this.image) {
	    this.image.ramaddaLayerIndex=level++;
	}

	if(this.isGroup()) {
	    this.getChildren().forEach(mapGlyph=>{
		level = mapGlyph.setLayerLevel(level);
	    });
	}
	return level;
    },
    makeLegend:function(opts) {
	opts = opts??{};
	let html = '';
	if(!this.display.getShowLegendShapes() && this.isShape()) {
	    return "";
	}
	let label =  this.getLabel(true,true);
	let body = HU.div(['class','imdv-legend-inner'],this.getLegendBody());

	if(this.imageLayers) {
	    let cbx='';
	    if(this.getMapLayer() && this.getMapLayer().features.length) {
		cbx+=HU.div([],HU.checkbox(Utils.getUniqueId(''),['imageid','main','class','imdv-imagelayer-checkbox'],
					   this.isImageLayerVisible({id:'main'}),
					   'Main Layer'));
	    }
	    this.imageLayerMap = {};
	    this.imageLayers.forEach(obj=>{
		this.imageLayerMap[obj.id] = obj;
		cbx+=HU.div([],HU.checkbox(Utils.getUniqueId(''),['imageid',obj.id,'class','imdv-imagelayer-checkbox'],
					   this.isImageLayerVisible(obj),
					   obj.name));
	    });
	    body+=HU.div(['class','imdv-legend-offset'],cbx);
	}
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
	html+=HU.open('div',['id',this.domId('legend_'),'glyphid',this.getId(),'class','imdv-legend-item '+clazz]);
	html+=HU.div(['style','display: flex;'],HU.div(['style','margin-right:4px;'],block.header)+
		     HU.div(['style','width:80%;'], label[0])+
		     HU.div([],label[1]));

	html+=HU.div(['class','imdv-legend-body'],block.body);
	html+=HU.close('div');
	return html;
    },

    getLegendStyle:function(style) {
	style = $.extend($.extend({},this.style),style??{});
	let s = '';
	let lineColor;
	let lineStyle;
	let lineWidth;
	if(Utils.stringDefined(style.fillPattern)) {
	    let svg = window.olGetSvgPattern(style.fillPattern,
					     style.strokeColor,style.fillColor);
	    s+='background-image: url(\''+ svg.url+'\');background-repeat: repeat;';
	}  else if(style.fillColor) {
	    s+=HU.css('background',style.fillColor);
	} 
	if(Utils.stringDefined(style.strokeColor)) 
	    lineColor = style.strokeColor;
	if(Utils.stringDefined(style.strokeWidth)) 
	    lineWidth = style.strokeWidth;
	if(Utils.stringDefined(style.strokeDashstyle)) {
	    if(['dot','dashdot'].includes(style.strokeDashstyle)) {
		lineStyle = "dotted";
	    } else  if(style.strokeDashstyle.indexOf("dash")>=0) {
		lineStyle = "dashed";
	    }
	}
	if(lineColor || lineColor||lineWidth) {
	    s+=HU.css('border',HU.getDimension(lineWidth??'1px')+ ' ' + (lineStyle??'solid') + ' ' +
		      (lineColor??'black'));
	}
	return s;
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
	    if(!this.mapLoaded) {
		if(this.isVisible()) 
		    body += HU.div(['class','imdv-legend-inner'],'Loading...');
		return body;
	    }

	    let boxStyle = 'display:inline-block;width:14px;height:14px;margin-right:4px;';
	    let legend = '';
	    let styleLegend='';
	    this.getStyleGroups().forEach((group,idx)=>{
		styleLegend+=HU.openTag('table',['width','100%']);
		styleLegend+= HU.openTag('tr',['title',this.display.canEdit()?'Select style':'','class',CLASS_IMDV_STYLEGROUP +(this.display.canEdit()?' ramadda-clickable':''),'index',idx]);
		let style = boxStyle + this.getLegendStyle(group.style);
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
		    let lineWidth;
		    let lineStyle;
		    let lineColor;
		    let svg;
		    let havePattern = rule.style.indexOf('fillPattern')>=0;
		    let fillColor,strokeColor;
		    let styleObj = {};
		    rule.style.split('\n').forEach(line=>{
			line  = line.trim();
			if(line=='') return;
			let toks = line.split(':');
			styleObj[toks[0]] = toks[1];
		    });

		    let style = boxStyle +this.getLegendStyle(styleObj);
		    let div=HU.div(['class','circles-1','style',style],'');
		    item+=div+'</td>';
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


	let showAnimation = false;
	if(this.isMapServer() && this.getDatacubeVariable() && this.getDatacubeVariable().dims && this.getDatacubeVariable().shape && this.getDatacubeAttr('time_coverage_start')) {
	    let v = this.getDatacubeVariable();
	    body+='Time: ' + HU.span(['id',this.domId('time_current')],this.getCurrentTimeStep()??'')+'<br>';
	    let idx=v.dims.indexOf('time');
	    let numTimes = v.shape[idx];
	    let start =new Date(v.attrs.time_coverage_start);
	    let end =new Date(v.attrs.time_coverage_end);
	    let value = end.getTime();
	    if(this.attrs.currentTimeStep) {
		value = new Date(this.attrs.currentTimeStep).getTime();
	    }

	    let slider = 
		HU.div(['title','Set time','slider-min',start.getTime(),'slider-max',end.getTime(),'slider-value',value,
			ID,this.domId('time_slider'),'class','ramadda-slider',STYLE,HU.css('display','inline-block','width','90%')],'');

	    let anim = HU.join([
		['Settings','fa-cog','settings'],
		['Go to start','fa-backward','start'],
		['Step backward','fa-step-backward','stepbackward'],
		['Play','fa-play','play'],
		['Step forward','fa-step-forward','stepforward'],
		['Go to end','fa-forward','end']
	    ].map(t=>{
		return HU.span(['class','imdv-time-anim ramadda-clickable','title',t[0],'action',t[2]],HU.getIconImage(t[1]));
	    }),HU.space(2));

	    if(this.getProperty('showAnimation',true)) {
		showAnimation  = true;
		body+=HU.center(anim);
		body+=slider;
		let fstart = this.formatDate(start);
		let fend = this.formatDate(end);
		body+=HU.leftRightTable(HU.span(['id',this.domId('time_min')],fstart),
					HU.span(['id',this.domId('time_max')],fend));
	    }
	}



	if(this.isMapServer() || Utils.stringDefined(this.style.imageUrl) || this.imageLayers || this.image) {
	    let v = (this.imageLayers||this.isImage())?this.style.imageOpacity:this.style.opacity;
	    if(!Utils.isDefined(v)) v = 1;
	    if(showAnimation)
		body+='Opacity:';
	    body += 
		HU.center(
		    HU.div(['title','Set image opacity','slider-min',0,'slider-max',1,'slider-value',v,
			    ID,this.domId('image_opacity_slider'),'class','ramadda-slider',STYLE,HU.css('display','inline-block','width','90%')],''));
	}

	if(this.display.canEdit() && (this.image || Utils.stringDefined(this.style.imageUrl))) {
	    body+='Rotation:';
	    body += HU.center(
		HU.div(['title','Set image rotation','slider-min',0,'slider-max',360,'slider-value',this.style.rotation??0,
			ID,this.domId('image_rotation_slider'),'class','ramadda-slider',STYLE,HU.css('display','inline-block','width','90%')],''));
	}



	let item = i=>{
	    if(Utils.stringDefined(i)) {
		body+=HU.div(['class','imdv-legend-body-item'], i);
	    }
	};


	body+=HU.div(['id',this.domId('legendcolortableprops')]);
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

	if(this.imageLayers) {
	    this.getLegendDiv().find('.imdv-imagelayer-checkbox').change(function() {
		let visible = $(this).is(':checked');
		let id = $(this).attr('imageid');
		let obj = _this.imageLayerMap[id]??{id:id};
		_this.setImageLayerVisible(obj,visible);
	    });
	}


	if(this.display.canEdit()) {
	    let label = this.getLegendDiv().find('.imdv-legend-label');
	    //Set the last dropped time so we don't also handle this as a setVisibility click
	    let notify = ()=>{_this.display.setLastDroppedTime(new Date());};
	    this.getLegendDiv().draggable({
		start: notify,
		drag: notify,
		stop: notify,
		containment:this.display.domId(ID_LEGEND),
		revert: true
	    });
	    this.display.makeLegendDroppable(this,label,notify);
	    //Only drop on the legend label

	    let items = this.jq(ID_LEGEND).find('.imdv-legend-label');
	    let rows = jqid('glyphstyle_'+this.getId()).find('.' + CLASS_IMDV_STYLEGROUP);

	    rows.click(function() {
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

	let setRotation = (event,ui) =>{
	    this.style.rotation = ui.value; 
	    if(this.image && this.image.imageHook) {
		this.image.imageHook();
	    }
	}
	let setOpacity = (event,ui) =>{
	    if(this.isMapServer())
		this.style.opacity = ui.value;
	    else if(this.image || this.imageLayers) {
		this.style.imageOpacity = ui.value;
		if(this.image) {
		    this.image.setOpacity(this.style.imageOpacity);
		}
		if(this.imageLayers)
		    this.imageLayers.forEach(obj=>{
			if(obj.layer)
			    obj.layer.setOpacity(this.style.imageOpacity);
		    });
	    }
	    this.applyStyle(this.style);
	}

	let getSliderTime=(value)=>{
	    return new Date(parseFloat(value));
	}
	let getStep = ()=>{
	    let min =  +this.jq('time_slider').attr('slider-min');
	    let max= +this.jq('time_slider').attr('slider-max');
	    let temp = this.getDatacubeAttr('temporal_resolution');
	    let msPerDay = 1000*60*60*24;
	    if(temp) {
		let match = temp.match(/(\d+)D/);
		if(match) {
		    return +match[1]*msPerDay;
		}
	    }
	    return msPerDay;
	}	    
	let timeSliderStop=v=>{
	    let current = getSliderTime(v);
	    let s = current.format('isoDate');
	    this.jq('time_current').html(s);
	    this.attrs.currentTimeStep = current.getTime();
	    if(this.mapServerLayer) {
		this.mapServerLayer.url = this.getMapServerUrl();
		this.getMapServerLayer().setVisibility(false);
		this.getMapServerLayer().setVisibility(true);
		ImdvUtils.scheduleRedraw(this.getMapServerLayer());
	    }
	}


	this.getLegendDiv().find('.imdv-time-anim').click(function() {
	    let action = $(this).attr('action');
	    let slider=	_this.jq('time_slider');
	    let current = +slider.slider('value');
	    let min = +slider.attr('slider-min');
	    let max = +slider.attr('slider-max');	    
	    let step = +slider.slider('option','step');
	    let change = (v)=>{
		v = Math.min(max,Math.max(min,v));
		slider.slider('value',v);
		timeSliderStop(v);
	    }

	    switch(action) {
	    case 'play':
		if(_this.timeAnimationTimeout)
		    clearTimeout(_this.timeAnimationTimeout);
		_this.timeAnimationTimeout=null;
		if(_this.timeAnimationRunning) {
		    $(this).html(HU.getIconImage('fas fa-play'));
		} else {
		    $(this).html(HU.getIconImage('fas fa-stop'));
		    let stepTime = () =>{
			let current = +slider.slider('value');
			current = current+(_this.attrs.timeAnimationStep??1)*step;
			change(current);
			//			console.log("current time: " +new Date(current) +' step:' + _this.attrs.timeAnimationStep);
			if(current>=max) {
			    $(this).html(HU.getIconImage('fas fa-play'));
			    _this.timeAnimationRunning = false;
			    return
			}
			_this.timeAnimationTimeout=setTimeout(stepTime,_this.attrs.timeAnimationPause??4000);
		    }
		    stepTime();
		}
		_this.timeAnimationRunning = !_this.timeAnimationRunning;
		break;
	    case 'start': 
		change(min);
		break;
	    case 'end': 
		change(max);
		break;		
	    case 'stepforward': 
		change(current+step);
		break;
	    case 'stepbackward': 
		change(current-step);
		break;		
	    case 'settings':
		let html = HU.formTable();

		html+=HU.formEntry('Time Pause:', HU.input("",_this.attrs.timeAnimationPause??4000,
							   ['id',_this.domId('timeanimation_pause'),'size','4']) +' (ms)');
		html+=HU.formEntry('Time Step:', HU.input("",_this.attrs.timeAnimationStep??1,
							  ['id',_this.domId('timeanimation_step'),'size','4']) +' Time steps to skip');		
		html+='</table>';

		let buttons = HU.buttons([
		    HU.div([CLASS,'ramadda-button-ok ramadda-button-apply display-button'], 'Apply'),
		    HU.div([CLASS,'ramadda-button-ok display-button'], 'OK'),
		    HU.div([CLASS,'ramadda-button-cancel display-button'], 'Cancel')]);
		html+=buttons;
		html = HU.div(['style','margin:6px;'], html);
		let dialog = HU.makeDialog({content:html,title:'Time Animation Settings',draggable:true,header:true,my:"left top",at:"left bottom",anchor:$(this)});
		
		dialog.find('.display-button').button().click(function() {
		    if($(this).hasClass('ramadda-button-ok')) {
			_this.attrs.timeAnimationPause = parseFloat(_this.jq('timeanimation_pause').val());
			_this.attrs.timeAnimationStep = parseFloat(_this.jq('timeanimation_step').val());
			if($(this).hasClass('ramadda-button-apply')) return;
		    }
		    dialog.remove();
		});

		break
	    }
	});

	this.jq('time_slider').slider({
	    min: +this.jq('time_slider').attr('slider-min'),
	    max: +this.jq('time_slider').attr('slider-max'),
	    step:getStep(),
	    value:+this.jq('time_slider').attr('slider-value'),
	    slide: ( event, ui )=> {
		let current = getSliderTime(ui.value);
		this.jq('time_current').html(current.format('isoDate'));
	    },
	    stop: ( event, ui )=> {
		timeSliderStop(ui.value);
	    }});

	this.jq('image_rotation_slider').slider({
	    min: 0,
	    max: 360,
	    step:1,
	    value:this.jq('image_rotation_slider').attr('slider-value'),
	    slide:function(event,ui) {
		setRotation(event,ui);
	    },
	    stop: function( event, ui ) {
		setRotation(event,ui);
	    }});


	this.jq('image_opacity_slider').slider({		
	    min: 0,
	    max: 1,
	    step:0.01,
	    value:this.jq('image_opacity_slider').attr('slider-value'),
	    slide:function(event,ui) {
		setOpacity(event,ui);
	    },
	    stop: function( event, ui ) {
		setOpacity(event,ui);
	    }});
	
	this.makeFeatureFilters();
	if(this.isMap() && this.mapLoaded) {
	    let addColor= (obj,prefix, strings) => {
		if(obj && Utils.stringDefined(obj.property)) {
		    let div = this.getColorTableDisplay(obj.colorTable,obj.min,obj.max,true,obj.isEnumeration, strings);
		    let html = HU.b(HU.center(this.makeLabel(obj.property,true)));
		    if(obj.isEnumeration) {
			html+=HU.div(['style','max-height:150px;overflow-y:auto;'],div);
		    } else {
			html+=HU.center(div);
		    }

		    this.jq('legendcolortable_'+prefix.toLowerCase()).html(html);
		    this.initColorTableDots(obj,this.jq('legendcolortable_'+prefix.toLowerCase()));

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
			    _this.initColorTableDots(obj, dialog);
			});
		    }
		}	
	    };

	    let ctProps = [];
	    this.getFeatureInfoList().forEach(info=>{
		if(!info.isColorTableSelect()) return;
		ctProps.push(info);
	    });

	    if(ctProps.length>1) {
		let menu = HU.select('',['id',this.domId('colortableproperty')],
				     ctProps.map(info=>{return {
					 value:info.id,label:info.getLabel()}}),this.attrs.fillColorBy.property);
		this.jq('legendcolortableprops').html(HU.b('Color by: ') + menu);
		this.jq('colortableproperty').change(()=>{
		    let val = this.jq('colortableproperty').val();
		    let info = this.getFeatureInfo(val);
		    if(!info) return;
		    $.extend(this.attrs.fillColorBy,{
			property:info.id,
			min:info.min,
			max:info.max});
		    this.applyMapStyle();
		    this.display.makeLegend();
		});
	    }
	    if(this.getProperty('showColorTableLegend',true)) {
		addColor(this.attrs.fillColorBy,'Fill',this.fillStrings);
		addColor(this.attrs.strokeColorBy,'Stroke',this.strokeStrings);
	    }
	}	



	if(this.isGroup()) {
	    this.getChildren().forEach(mapGlyph=>{mapGlyph.initLegend();});
	}
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
    isRings:function() {
	return this.getType()==GLYPH_RINGS;
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
    setMapServerUrl:function(url,wmsLayer,legendUrl,predefined,mapOptions) {
	this.style.legendUrl = legendUrl;
	this.attrs.mapServerUrl = url;
	this.attrs.wmsLayer = wmsLayer;
	this.attrs.predefinedLayer = predefined;
	this.mapServerOptions = mapOptions;

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
		if(text=='none') return null;
		if(!Utils.stringDefined(text)) text = '${default}';
		if(!feature.attributes) return null;
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
    getMapServerUrl:function() {
	let url=this.attrs.mapServerUrl;
	//Convert malformed TMS url
	url = url.replace(/\/{/g,"/${");
	if(this.getDatacubeVariable()) {
	    let variable = this.getDatacubeVariable();
	    url = url.replace(/\{colorbar\}/,variable.colorBarName);
	    url = url.replace(/\{vmin\}/,variable.colorBarMin);
	    url = url.replace(/\{vmax\}/,variable.colorBarMax);	    	    
	    if(variable.attrs.time_coverage_start) {
		let time = this.getCurrentTimeStep();
		url = url.replace(/\{time\}/,encodeURIComponent(time));
	    }
	}
	return url;
    },

    getDatacubeVariable:function() {
	return  this.attrs.variable;
    },
    getDatacubeAttrs:function() {
	return this.getDatacubeVariable()?.attrs;
    },
    getDatacubeAttr:function(attr) {
	let attrs =  this.getDatacubeAttrs();
	return attrs?attrs[attr]:null;
    },    
    getCurrentTimeStep:function() {
	if(Utils.isDefined(this.attrs.currentTimeStep)) {
	    return  this.formatDate(new Date(this.attrs.currentTimeStep));
	}
	if(!this.getDatacubeAttr('time_coverage_end')) return null;		
	return this.formatDate(new Date(this.getDatacubeAttr('time_coverage_end')));
    },

    formatDate: function(date) {
	return date.format('isoDate');
    },
    createMapServer:function() {
	this.mapServerLayer =  this.display.getMap().createXYZLayer(this.getName(), this.getMapServerUrl());
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
		this.createMapServer();
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
		if(Utils.stringDefined(value=jqid(prefix+'_fillpattern').val())) group.style.fillPattern = value;
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
	    showColorTableDots:isEnum&& strings.length<=15,
	    horizontal:!isEnum || strings.length>15,
	    showRange: false,
            height: "20px",
	    showRange:showRange
        });
	let attrs = [TITLE,id,'style','margin-right:4px;',"colortable",id]
	//	if(ct.colors.length>20)   attrs.push(STYLE,HU.css('width','400px'));
        return  HtmlUtils.div(attrs,display);
    },
    initColorTables: function(currentColorbar) {
	if(!currentColorbar)
	    this.currentColorbar=this.getDatacubeVariable()?.colorBarName;
	currentColorbar = currentColorbar??this.getDatacubeVariable()?.colorBarName;
	let items = [];
	let image;
	let html = '';
	this.display.colorbars.forEach(coll=>{
	    let cat=coll[0];
	    html+=HU.b(cat)+'<br>';
	    coll[2].forEach(c=>{
		let name = c[0];
		let url = 'data:image/png;base64,' + c[1];
		html+=HU.image(url,['class','ramadda-clickable','colorbar',name, 'height','20px','width','256px','title',name]);
		html+='<br>';
		if(name==currentColorbar) {
		    image = c[1];
		}
	    });
	});
	let url = image?('data:image/png;base64,' + image):null;
	this.jq('colortableproperties').html(HU.div(['id',this.domId('colortable'),'class','ramadda-clickable','title','Click to select color bar'],
						    url? HU.image(url,['height','20px','width','256px']):'No image'));

	let _this = this;
	html = HU.div(['style','margin:8px;max-height:200px;overflow-y:auto;'], html);
	this.jq('colortable').click(function() {
	    let colorSelect = HU.makeDialog({content:html,
					     my:'left top',
					     at:'left bottom',
					     anchor:$(this)});
	    colorSelect.find('img').click(function() {
		_this.currentColorbar = $(this).attr('colorbar');
		colorSelect.remove();
		_this.initColorTables(_this.currentColorbar);
	    });
	});
    },
    initPropertiesComponent: function(dialog) {
	let _this = this;
	if(this.isMapServer() && this.getDatacubeVariable()) {
	    if(!this.display.colorbars) {
		let dataCubeServers=  MapUtils.getMapProperty('datacubeservers','').split(',');
		let url = dataCubeServers[0]+'/colorbars';
		$.getJSON(url, (data)=> {
		    this.display.colorbars = data;
		    this.initColorTables();
		});
	    } else {
		this.initColorTables();
	    }
	}	    	


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
	let columns  =this.getFeatureInfoList().filter(info=>{
	    return info.showTable();
	});
	let table;
	this.featureTableMap = {};

	let featureInfo = this.getFeatureInfoList();
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
		if(v && Utils.isDefined(v.value)) v = v.value;
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
		sv=column.format(sv);
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
		columns  =this.getFeatureInfoList().filter(info=>{
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
	let featureInfo = this.featureInfo = this.getFeatureInfoList();
	let keys  = Object.keys(featureInfo);
	let numeric = featureInfo.filter(info=>{return info.isNumeric();});
	let enums = featureInfo.filter(info=>{return info.isEnumeration();});
	let colorBy = '';
	colorBy+=HU.leftRightTable(HU.checkbox(this.domId('fillcolors'),['id',this.domId('fillcolors')],
					       this.attrs.fillColors,'Fill Colors'),
				   this.getHelp('#mapstylerules'));

	numeric = featureInfo;
	if(numeric.length) {
	    let numericProperties=Utils.mergeLists([['','Select']],numeric.map(info=>{return {value:info.property,label:info.getLabel()};}));
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

	let properties=Utils.mergeLists([['','Select']],featureInfo.map(info=>{
	    return {value:info.property,label:info.getLabel()};}));
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
	let styleTitle = 'e.g.:&#013;fillColor:red&#013;fillOpacity:0.5&#013;strokeColor:blue&#013;strokeWidth:1&#013;strokeDashstyle:solid|dot|dash|dashdot|longdash|longdashdot';
	for(let index=0;index<20;index++) {
	    let rule = index<rules.length?rules[index]:{};
	    let value = rule.value??'';
	    let info = this.featureInfoMap[rule.property];
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
	content.push({header:'Style Rules', contents:colorBy+table});


	let mapPointsRange = HU.leftRightTable(HU.b('Visiblity limit: ') + HU.select('',[ID,'mappoints_range'],this.display.levels,this.getMapPointsRange()??'',null,true) + ' '+
					       HU.span(['class','imdv-currentlevellabel'], '(current level: ' + this.display.getCurrentLevel()+')'),
					       this.getHelp('#map_labels'));
	let mapPoints = HU.textarea('',this.getMapLabelsTemplate()??'',['id','mappoints_template','rows','6','cols','40','title','Map points template, e.g., ${code}']);

	let propsHelp =this.display.makeSideHelp(helpLines,'mappoints_template',{prefix:'${',suffix:'}'});
	mapPoints = HU.hbox([mapPoints,HU.space(2),'Add property:' + propsHelp]);


	let styleGroups =this.getStyleGroups();
	let styleGroupsUI = HU.leftRightTable('',
					      this.getHelp('#adding_a_map'));
	styleGroupsUI+=HU.openTag('table',['width','100%']);
	styleGroupsUI+=HU.tr([],HU.tds([],
				       ['Group','Fill','Opacity','Stroke','Width','Pattern','Features']));
	for(let i=0;i<20;i++) {
	    let group = styleGroups[i];
	    let prefix = 'mapstylegroups_' + i;
	    styleGroupsUI+=HU.tr([],HU.tds([],[
		HU.input('',group?.label??'',['id',prefix+'_label','size','10']),
		HU.input('',group?.style.fillColor??'',['class','ramadda-imdv-color','id',prefix+'_fillcolor','size','6']),
		HU.input('',group?.style.fillOpacity??'',['title','0-1','id',prefix+'_fillopacity','size','2']),		
		HU.input('',group?.style.strokeColor??'',['class','ramadda-imdv-color','id',prefix+'_strokecolor','size','6']),
		HU.input('',group?.style.strokeWidth??'',['id',prefix+'_strokewidth','size','6']),
		this.display.getFillPatternSelect(prefix+'_fillpattern',group?.style.fillPattern??''),
		Utils.join(group?.indices??[],',')]));
	}
	styleGroupsUI += HU.closeTag('table');
	styleGroupsUI = HU.div(['style',HU.css('max-height','150px','overflow-y','auto')], styleGroupsUI);

	content.push({header:'Style Groups',contents:styleGroupsUI});
	content.push({header:'Labels',
		      contents:mapPointsRange+  HU.b('Label Template:')+'<br>' +mapPoints});

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
    getFeatureInfoList:function() {
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
		getProperty:function(prop,dflt,checkGlyph) {
		    if(checkGlyph) dflt=_this.getProperty(prop,dflt);
		    let v =   _this.getProperty(this.id+'.' + prop,dflt);
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
		isColorTableSelect: function() {
		    return this.getProperty('colortable.select',_this.getProperty('colortable.select',false));
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

		format:function(value) {
		    if(this.isNumeric()) {
			let decimals = this.getProperty('format.decimals',null,true);
			if(Utils.isDefined(decimals)&&!isNaN(value)) {
			    value = Utils.trimDecimals(value,decimals);
			}
		    }
		    return value;
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
	    this.featureInfoMap[info.id] = info;	    
	});
	return this.featureInfo;
    },
    getFeatureInfo:function(property) {
	this.getFeatureInfoList();
	if(this.featureInfoMap) return this.featureInfoMap[property];
	return null;
    },
    makeLabel:function(l,makeSpan) {
	let info = this.getFeatureInfo(l);
	if(info) return info.getLabel(makeSpan);
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
	let debug = false;
	if(debug)
	    console.log("KEY:" + key);
	if(this.attrs.properties) {
	    if(!this.parsedProperties) {
		this.parsedProperties = Utils.parseMap(this.attrs.properties,"\n","=")??{};
	    }

	    let v = this.parsedProperties[key];
	    if(debug) console.log("V:" + v);
	    if(debug) console.log("PROPS:",this.parsedProperties);	    
	    if(v) {
		return Utils.getProperty(v);
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
	this.getFeatureInfoList().forEach(info=>{
	    if(!info.showFilter()) {
		return;
	    }
	    this.filterInfo[info.property] = info;
	    this.filterInfo[info.getId()] = info;	    
	    if(!filters[info.property]) filters[info.property]= {};
	    let filter = filters[info.property];
	    if(!Utils.isDefined(filter.min) || isNaN(filter.min)) filter.min = info.min;
	    if(!Utils.isDefined(filter.max) || isNaN(filter.max)) filter.max = info.max;	    
	    filter.property =  info.property;
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
		if(isNaN(filter.min)||filter.min<min) filter.min = min;
		if(isNaN(filter.max) || filter.max>max) filter.max = max;
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
		filtersHeader = HU.checkbox(this.zoomonchangeid,
					    ['title','Zoom on change','id',this.zoomonchangeid],
					    this.getZoomOnChange(),
					    HU.span(['title','Zoom on change','style','margin-left:12px;'], HU.getIconImage('fas fa-binoculars',[],LEGEND_IMAGE_ATTRS)));
	    }
	    filtersHeader+=HU.span(['id',this.domId('filters_count')],Utils.isDefined(this.visibleFeatures)?'#'+this.visibleFeatures:'');
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
		if($("#"+this.zoomonchangeid).is(':checked')) {
		    this.panMapTo();
		}
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
		    let featureInfo = _this.getFeatureInfo(theFeatureId);
		    let id = featureInfo.property;
		    let filter = filters[id]??{};
		    if(ui.animValues) {
			filter.min = ui.animValues[0];
			filter.max = ui.animValues[1];			
		    } else {
			if(ui.handleIndex==0)
			    filter.min = ui.value;
			else
			    filter.max = ui.value;
		    }

		    filter.property=id;
		    _this.jq('slider_min_'+ featureInfo.getId()).html(Utils.formatNumber(filter.min));
		    _this.jq('slider_max_'+ featureInfo.getId()).html(Utils.formatNumber(filter.max));			    
		    if(force ||_this.getProperty('filter.live') ||  _this.getProperty(featureInfo.getId()+'.filter.live')) {
			update();
			return
		    }
		    if(!_this.sliderThrottle) 
			_this.sliderThrottle=Utils.throttle(()=>{
			    update();
			},500);
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
		//Round the step
		//		if(step>10) step=parseInt(step);
		let args = {
		    range:true,
		    min: parseFloat(min),
		    max: parseFloat(max),
		    step:step,
		    values:[parseFloat($(this).attr('slider-value-min')),
			    parseFloat($(this).attr('slider-value-max'))],
		    slide: onSlide};
		$(this).slider(args);		
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
			sliderInfo.slide({},{animValues:values},true);
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
	//Check if there are any KML ground overlays
	//TODO: limit the number we add?
	if(layer?.protocol?.format?.groundOverlays) {
	    let format = layer.protocol.format;
	    let text=(node,tag)=>{
		let child = format.getElementsByTagNameNS(node,'*',tag);
		if(!child.length) return null;
		return child[0].innerHTML;
	    };
	    this.imageLayers=[];
	    let cnt=0;
	    format.groundOverlays.forEach(go=>{
		let icons = format.getElementsByTagNameNS(go,'*','Icon');
		let ll = format.getElementsByTagNameNS(go,'*','LatLonBox');
		if(!icons.length || !ll.length) return;
		ll =ll[0];
		let name = text(go,'name');
		let url = text(icons[0],'href');
		if(!url) return;
		url = url.replace(/&amp;/g,'&');
		let north = text(ll,'north');
		let south = text(ll,'south');
		let east = text(ll,'east');
		let west = text(ll,'west');			    
		let obj = {
		    id:'groundoverlay_'+(cnt++),
		    url:url,
		    name:name,
		    north:north,west:west,south:south,east:east
		}
		this.imageLayers.push(obj);
		//We might have a bunch of overlays so don't add them if there are lots
		this.isImageLayerVisible(obj,cnt<=3);
	    });
	}

	this.mapLoaded = true;
	this.makeFeatureFilters();
	this.applyMapStyle();
    },
    applyMacros:function(template, attributes, macros) {
	if(!macros) macros =  Utils.tokenizeMacros(template);
	let infos = this.getFeatureInfoList();
	if(attributes) {
	    let attrs={};
	    infos.forEach(info=>{
		let attr=info.property;
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
		value = info.format(value);
		let _attr = attr.toLowerCase();
		attrs[attr] = attrs[_attr] = value;			
	    });
	    template = macros.apply(attrs);
	}
	if(template.indexOf('${default}')>=0) {
	    let columns = [];
	    let labelMap = {};
	    let infoMap = {};
	    infos.forEach(info=>{
		infoMap[info.property] = info;
		if(info.showPopup()) {
		    columns.push(info.property);
		    labelMap[info.property] = info.getLabel();
		}
	    });
	    let formatter = (attr,value)=>{
		let info = infoMap[attr];
		if(info) {
		    value = info.format(value);
		}
		return value;
	    };
	    let labelGetter = attr=>{
		return labelMap[attr];
	    }
	    template = template.replace('${default}',MapUtils.makeDefaultFeatureText(attributes,columns,
										     formatter,labelGetter));
	}
	return template;
    },

    initColorTableDots:function(obj, dialog) {
	let _this  = this;
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
    },
    

    applyMapStyle:function(skipLegendUI) {
	let _this = this;
	//If its a map then set the style on the map features
	if(!this.mapLayer || !this.mapLoaded) return;

	let features= this.mapLayer.features;
	if(!features) return
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
	    if(!filter.property) {
		continue;
	    }
	    let info =this.getFeatureInfo(filter.property);
	    if(!info) {
		continue;
	    }
	    if(info && !info.showFilter()) continue;
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

	if(features) {
	    features.forEach((feature,idx)=>{
		feature.featureIndex = idx;
	    });
	}

	if(this.mapLabels) {
	    this.display.removeFeatures(this.mapLabels);
	    this.mapLabels = null;
	}


	//Apply the base style here
	
	this.mapLayer.style = style;
	if(features) {
	    features.forEach((f,idx)=>{
		ImdvUtils.applyFeatureStyle(f, $.extend({},style));
		f.originalStyle = $.extend({},style);			    
	    });
	}


	//Check for any rule based styles
	let attrs = features.length>0?features[0].attributes:{};
	let keys  = Object.keys(attrs);
	if(rules && rules.length>0) {
	    this.mapLayer.style = null;
	    this.mapLayer.styleMap = this.display.getMap().getVectorLayerStyleMap(this.mapLayer, style,rules);
	    features.forEach((f,idx)=>{
		f.style = null;
	    });
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


	this.fillStrings = [];
	this.strokeStrings = [];		
	
	applyColors(this.attrs.fillColorBy,'fillColor',this.fillStrings);
	applyColors(this.attrs.strokeColorBy,'strokeColor',this.strokeStrings);	

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
			    v = v.substring(3);
			    try {
				v = eval(v);
			    } catch(err) {
				console.error('Error evaluating style rule:' + v);
			    }
			}
			f.style[style] = v;
		    });
		});
	    });
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
		let pt = feature.geometry.getCentroid(true); 
		let labelStyle = $.extend({},markerStyle);
		labelStyle.label = this.applyMacros(template, feature.attributes,macros);
		let mapLabel = MapUtils.createVector(pt,null,labelStyle);
		mapLabel.point = pt;
		feature.mapLabel  = mapLabel;
		this.mapLabels.push(mapLabel);
	    });
	}

	this.visibleFeatures = 0;
	let redrawFeatures = false;
	let max =-1;
	features.forEach((f,idx)=>{
	    let visible = true;
	    rangeFilters.every(filter=>{
		let value=this.getFeatureValue(f,filter.property);
		if(Utils.isDefined(value)) {
		    max = Math.max(max,value);
		    visible = value>=filter.min && value<=filter.max;
		    //		    if(value>1000) console.log(filter.property,value,visible,filter.min,filter.max);
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

	    if(visible) this.visibleFeatures++;
	    f.isVisible  = visible;
	    MapUtils.setFeatureVisible(f,visible);
	    if(f.mapLabel) {
		redrawFeatures = true;
		f.mapLabel.isFiltered=!visible;
		MapUtils.setFeatureVisible(f.mapLabel,visible);
	    }
	});


	this.jq('filters_count').html('#' + this.visibleFeatures);


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
		f.style = $.extend({},f.style);
		$.extend(f.style,group.style)
	    }
	});

	this.mapLayer.features.forEach(f=>{
	    if(f.style && f.style.fillPattern && !Utils.stringDefined(f.style.fillColor)) {
		f.style.fillColor='transparent'
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

    checkRings:function(points) {
	if(!this.features[0]){
	    console.log("range rings has no features");
	    return
	}

	let style = $.extend({},this.features[0].style);
	style.strokeColor='transparent';
	this.features[0].style = style;

	let pt = this.features[0].geometry.getCentroid();
	let center = this.display.getMap().transformProjPoint(pt)

	if(this.rings) this.display.removeFeatures(this.rings);
	let ringStyle = {};
	if(this.attrs.rangeRingStyle) {
	    //0,fillColor:red,strokeColor:blue
	    Utils.split(this.attrs.rangeRingStyle,'\n',true,true).forEach(line=>{
		let toks = line.split(',');
		ringStyle[toks[0]] = {};
		for(let i=1;i<toks.length;i++) {
		    let toks2 = toks[i].split(':');
		    ringStyle[toks[0]][toks2[0]] = toks2[1];
		}
	    });
	}
	this.rings = this.display.makeRangeRings(center,this.getRadii(),this.style,this.attrs.rangeRingAngle,ringStyle);
	if(this.rings) {
	    this.rings.forEach(ring=>{
		ring.mapGlyph=this;
		MapUtils.setFeatureVisible(ring,this.isVisible());
	    });
	    this.display.addFeatures(this.rings);
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
	if(this.isRings()){
	    this.checkRings();
	}

	this.display.featureChanged(true);
    },
    move:function(dx,dy) {
	if(this.getUseEntryLocation()) {
	    this.setUseEntryLocation(false);
	}
	if(this.rings){
	    this.rings.forEach(feature=>{
		feature.geometry.move(dx,dy);
		feature.layer.drawFeature(feature);
	    });
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
	if(this.image) {
	    if(Utils.isDefined(this.image.opacity)) {
		this.style.imageOpacity=this.image.opacity;
	    }
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
	    this.initImageLayer(this.image);
	    this.image.imageHook();
	    if(Utils.isDefined(this.style.imageOpacity)) {
		this.image.setOpacity(this.style.imageOpacity);
	    }
	}
    },
    initImageLayer:function(image) {
	this.image = image;
	image.imageHook = (image)=> {
	    let transform='';
	    if(Utils.stringDefined(this.style.transform)) 
		transform = this.style.transform;
	    if(this.style.rotation>0)
		transform += ' rotate(' + this.style.rotation +'deg)';

	    if(!Utils.stringDefined(transform))  transform=null;
	    var childNodes = this.image.div.childNodes;
	    for(var i = 0, len = childNodes.length; i < len; ++i) {
                var element = childNodes[i].firstChild || childNodes[i];
                var lastChild = childNodes[i].lastChild;
                if (lastChild && lastChild.nodeName.toLowerCase() === "iframe") {
		    element = lastChild.parentNode;
                }
		element.style.transform=transform;
		//                    OpenLayers.Util.modifyDOMElement(element, null, null, null, null, null, null, null);
	    }
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


	if(this.rings) {
	    this.rings.forEach(feature=>{
		MapUtils.setFeatureVisible(feature,visible);
	    });
	}

	if(callCheck)
	    this.checkVisible();
	this.checkMapLayer();
	let legend = this.getLegendDiv();
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
	    MapUtils.setFeatureVisible(feature, vis);
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
	if(this.getMapLayer() && !this.imageLayers) {
	    this.getMapLayer().setVisibility(visible);
	}
	if(this.imageLayers) {
	    this.imageLayers.forEach(obj=>{
		let imageVisible = visible && this.isImageLayerVisible(obj);
		if(obj.layer)
		    obj.layer.setVisibility(imageVisible);
	    })
	}
	if(this.getMapServerLayer()) {
	    this.getMapServerLayer().setVisibility(visible);
	}	

	if(this.selectDots && this.selectDots.length>0) {
	    this.selectDots.forEach(dot=>{
		MapUtils.setFeatureVisible(dot,visible);
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
	    if(!visible) {
		this.mapLabels.forEach(mapLabel=>{setVis(mapLabel,false);});
	    } else if(Utils.stringDefined(this.getMapPointsRange())) {
		if(level<parseInt(this.getMapPointsRange())) {
		    visible=false;
		    this.mapLabels.forEach(mapLabel=>{setVis(mapLabel,false);});
		}
	    } else {
		//If the label wasn't filtered then turn them all on
		this.mapLabels.forEach(mapLabel=>{
		    if(!mapLabel.isFiltered)
			setVis(mapLabel,true);
		});
		let args ={};
		if(this.getProperty('mapLabelGridWidth'))
		    args.cellWidth = +this.getProperty('mapLabelGridWidth');
		if(this.getProperty('mapLabelGridHeight'))
		    args.cellHeight = +this.getProperty('mapLabelGridHeight');		
		MapUtils.gridFilter(this.getMap(), this.mapLabels,args);
	    }
	}

	if(this.children) {
	    this.children.forEach(child=>{child.checkVisible();});
	}
	ImdvUtils.scheduleRedraw(this.display.myLayer);
    },    
    setImageLayerVisible:function(obj,visible) {
	this.isImageLayerVisible(obj,true,visible);
	if(obj.layer) obj.layer.setVisibility(visible);
    },
    isImageLayerVisible:function(obj,create,visible) {
	if(!this.attrs.imageLayerState) {
	    this.attrs.imageLayerState = {};
	}
	let state=this.attrs.imageLayerState[obj.id];
	if(!state) {
	    state = this.attrs.imageLayerState[obj.id] = {
		visible:true
	    }
	}
	if(Utils.isDefined(visible)) state.visible=visible;
	if(obj.id=='main') {
	    if(this.getMapLayer()) this.getMapLayer().setVisibility(visible);
	    return
	}
	if(state.visible && create)  {
	    obj.layer =this.getMap().addImageLayer(obj.name,obj.name,'',obj.url,true,
						   obj.north,obj.west,obj.south,obj.east);
	    
	    if(Utils.isDefined(this.style.imageOpacity))
		obj.layer.setOpacity(this.style.imageOpacity);
	    this.display.makeLegend();
	}
	return state.visible;
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
	if(this.rings) {
	    this.display.removeFeatures(this.rings);
	}
	if(this.showMarkerMarker) {
	    this.display.removeFeatures([this.showMarkerMarker]);
	    this.showMarkerMarker = null;
	}
	if(this.selectDots) {
	    this.display.selectionLayer.removeFeatures(this.selectDots);
	    this.selectDots = null;
	}

	if(this.getMapLayer()) {
	    this.display.getMap().removeLayer(this.getMapLayer());
	    this.image =null;
	    this.setMapLayer(null);
	}

	if(this.getImage()) {
	    this.display.getMap().removeLayer(this.getImage());
	    this.image =null;
	}


	if(this.imageLayers) {
	    this.imageLayers.forEach(obj=>{
		if(obj.layer)
		    this.display.getMap().removeLayer(obj.layer);
	    })
	    this.imageLayers = [];
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



/*
  this is a hook that OL calls to handle fillPatterns
  list of patterns from https://iros.github.io/patternfills/sample_svg.html
  and is generated with /bin/svgs.tcl
*/
window.olGetPatternId = function(ol,p,stroke,fill) {
    if(!ol.defs) {
        ol.defs = ol.createDefs();
    }
    if(!ol.idToSvgId) {
	ol.idToSvgId={};
    }
    stroke = stroke||'#000';
    fill = fill||'transparent';
    let id = p+'_'+stroke +'_'+fill;
    if(ol.idToSvgId[id]) return ol.idToSvgId[id];
    p = window.olGetSvgPattern(p,stroke,fill);
    if(!window.olPatternBaseId) window.olPatternBaseId = 1;
    let svgId = 'pattern_'+(window.olPatternBaseId++);
    let patternNode = ol.nodeFactory(null, "pattern");
    patternNode.setAttributeNS(null, "id",svgId);
    patternNode.setAttributeNS(null, "width",p.width);
    patternNode.setAttributeNS(null, "height",p.height);
    patternNode.setAttributeNS(null, "patternUnits","userSpaceOnUse");
    let imageNode = ol.nodeFactory(null, "image");
    patternNode.appendChild(imageNode);
    imageNode.setAttributeNS(null, "x",0);	    
    imageNode.setAttributeNS(null, "y",0);
    imageNode.setAttributeNS(null, "width",p.width);
    imageNode.setAttributeNS(null, "height",p.height);
    imageNode.setAttributeNS(null, "href",p.url);
    ol.defs.appendChild(patternNode);
    ol.idToSvgId[id] = svgId;
    return svgId;
};


var IMDV_PATTERNS = {
    "diagonal-stripe-1":{width:10,height:10,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'><rect width='10' height='10' fill='<%= background %>'/><path d='M-1,1 l2,-2 M0,10 l10,-10 M9,11 l2,-2' stroke='<%= foreground %>' stroke-width='1'/></svg> "},
    "diagonal-stripe-3":{width:10,height:10,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'><rect width='10' height='10' fill='<%= background %>'/><path d='M-1,1 l2,-2 M0,10 l10,-10 M9,11 l2,-2' stroke='<%= foreground %>' stroke-width='3'/></svg>"},
    "diagonal-stripe-2":{width:10,height:10,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'><rect width='10' height='10' fill='<%= background %>'/><path d='M-1,1 l2,-2 M0,10 l10,-10 M9,11 l2,-2' stroke='<%= foreground %>' stroke-width='2'/></svg>"},
    "diagonal-stripe-6":{width:10,height:10,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'><rect width='10' height='10' fill='<%= foreground %>'/><path d='M-1,1 l2,-2 M0,10 l10,-10 M9,11 l2,-2' stroke='<%= background %>' stroke-width='1'/></svg>"},
    "diagonal-stripe-5":{width:10,height:10,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'><rect width='10' height='10' fill='<%= foreground %>'/><path d='M-1,1 l2,-2 M0,10 l10,-10 M9,11 l2,-2' stroke='<%= background %>' stroke-width='2'/></svg>"},
    "diagonal-stripe-4":{width:10,height:10,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'><rect width='10' height='10' fill='<%= foreground %>'/><path d='M-1,1 l2,-2 M0,10 l10,-10 M9,11 l2,-2' stroke='<%= background %>' stroke-width='3'/></svg>"},
    "subtle-patch":{width:5,height:5,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='5' height='5'><rect width='5' height='5' fill='<%= background %>' /><rect x='2' y='2' width='1' height='1' fill='<%= foreground %>' /></svg>"},
    "whitecarbon":{width:6,height:6,svg:"<svg xmlns='http://www.w3.org/2000/svg' xmlns:xlink='http://www.w3.org/1999/xlink' width='6' height='6'><rect width='6' height='6' fill='#eeeeee'/><g id='c'><rect width='3' height='3' fill='#e6e6e6'/><rect y='1' width='3' height='2' fill='#d8d8d8'/></g><use xlink:href='#c' x='3' y='3'/></svg>"},
    "crosshatch":{width:8,height:8,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='8' height='8'><rect width='8' height='8' fill='#fff'/><path d='M0 0L8 8ZM8 0L0 8Z' stroke-width='0.5' stroke='#aaa'/></svg> "},
    "houndstooth":{width:10,height:10,svg:"<svg width='10' height='10' xmlns='http://www.w3.org/2000/svg'><path d='M0 0L4 4' stroke='#aaa' fill='#aaa' stroke-width='1'/><path d='M2.5 0L5 2.5L5 5L9 9L5 5L10 5L10 0' stroke='#aaa' fill='#aaa' stroke-width='1'/><path d='M5 10L5 7.5L7.5 10' stroke='#aaa' fill='#aaa' stroke-width='1'/></svg> "},
    "verticalstripe":{width:6,height:49,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='6' height='49'><rect width='3' height='50' fill='#fff'/><rect x='3' width='1' height='50' fill='#ccc'/></svg> "},
    "smalldot":{width:5,height:5,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='5' height='5'><rect width='5' height='5' fill='#fff'/><rect width='1' height='1' fill='#ccc'/></svg>"},
    "lightstripe":{width:5,height:5,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='5' height='5'><rect width='5' height='5' fill='white'/><path d='M0 5L5 0ZM6 4L4 6ZM-1 1L1 -1Z' stroke='#888' stroke-width='1'/></svg>"},
    "vertical-stripe-8":{width:10,height:10,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'><rect width='10' height='10' fill='<%= background %>' /><rect x='0' y='0' width='8' height='10' fill='<%= foreground %>' /></svg>"},
    "vertical-stripe-9":{width:10,height:10,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'><rect width='10' height='10' fill='<%= background %>' /><rect x='0' y='0' width='9' height='10' fill='<%= foreground %>' /></svg>"},
    "vertical-stripe-7":{width:10,height:10,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'><rect width='10' height='10' fill='<%= background %>' /><rect x='0' y='0' width='7' height='10' fill='<%= foreground %>' /></svg>"},
    "vertical-stripe-6":{width:10,height:10,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'><rect width='10' height='10' fill='<%= background %>' /><rect x='0' y='0' width='6' height='10' fill='<%= foreground %>' /></svg>"},
    "vertical-stripe-4":{width:10,height:10,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'><rect width='10' height='10' fill='<%= background %>' /><rect x='0' y='0' width='4' height='10' fill='<%= foreground %>' /></svg>"},
    "vertical-stripe-5":{width:10,height:10,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'><rect width='10' height='10' fill='<%= background %>' /><rect x='0' y='0' width='5' height='10' fill='<%= foreground %>' /></svg>"},
    "vertical-stripe-1":{width:10,height:10,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'><rect width='10' height='10' fill='<%= background %>' /><rect x='0' y='0' width='1' height='10' fill='<%= foreground %>' /></svg>"},
    "vertical-stripe-2":{width:10,height:10,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'><rect width='10' height='10' fill='<%= background %>' /><rect x='0' y='0' width='2' height='10' fill='<%= foreground %>' /></svg>"},
    "vertical-stripe-3":{width:10,height:10,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'><rect width='10' height='10' fill='<%= background %>' /><rect x='0' y='0' width='3' height='10' fill='<%= foreground %>' /></svg>"},
    "circles-6":{width:10,height:10,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'><rect width='10' height='10' fill='<%= background %>' /><circle cx='3.5' cy='3.5' r='3.5' fill='<%= foreground %>'/></svg> "},
    "circles-7":{width:10,height:10,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'><rect width='10' height='10' fill='<%= background %>' /><circle cx='4' cy='4' r='4' fill='<%= foreground %>'/></svg>"},
    "circles-5":{width:10,height:10,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'><rect width='10' height='10' fill='<%= background %>' /><circle cx='3' cy='3' r='3' fill='<%= foreground %>'/></svg> "},
    "circles-4":{width:10,height:10,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'><rect width='10' height='10' fill='<%= background %>' /><circle cx='2.5' cy='2.5' r='2.5' fill='<%= foreground %>'/></svg>"},
    "circles-1":{width:10,height:10,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'><rect width='10' height='10' fill='<%= background %>' /><circle cx='1' cy='1' r='1' fill='<%= foreground %>'/></svg>"},
    "circles-3":{width:10,height:10,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'><rect width='10' height='10' fill='<%= background %>' /><circle cx='2' cy='2' r='2' fill='<%= foreground %>'/></svg>"},
    "circles-2":{width:10,height:10,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'><rect width='10' height='10' fill='<%= background %>' /><circle cx='1.5' cy='1.5' r='1.5' fill='<%= foreground %>'/></svg> "},
    "circles-9":{width:10,height:10,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'><rect width='10' height='10' fill='<%= background %>' /><circle cx='5' cy='5' r='5' fill='<%= foreground %>'/></svg>"},
    "circles-8":{width:10,height:10,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'><rect width='10' height='10' fill='<%= background %>' /><circle cx='4.5' cy='4.5' r='4.5' fill='<%= foreground %>'/></svg>"},
    "horizontal-stripe-6":{width:10,height:10,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'><rect width='10' height='10' fill='<%= background %>' /><rect x='0' y='0' width='10' height='6' fill='<%= foreground %>' /></svg>"},
    "horizontal-stripe-7":{width:10,height:10,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'><rect width='10' height='10' fill='<%= background %>' /><rect x='0' y='0' width='10' height='7' fill='<%= foreground %>' /></svg>"},
    "horizontal-stripe-5":{width:10,height:10,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'><rect width='10' height='10' fill='<%= background %>' /><rect x='0' y='0' width='10' height='5' fill='<%= foreground %>' /></svg>"},
    "horizontal-stripe-4":{width:10,height:10,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'><rect width='10' height='10' fill='<%= background %>' /><rect x='0' y='0' width='10' height='4' fill='<%= foreground %>' /></svg>"},
    "horizontal-stripe-1":{width:10,height:10,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'><rect width='10' height='10' fill='<%= background %>' /><rect x='0' y='0' width='10' height='1' fill='<%= foreground %>' /></svg>"},
    "horizontal-stripe-3":{width:10,height:10,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'><rect width='10' height='10' fill='<%= background %>' /><rect x='0' y='0' width='10' height='3' fill='<%= foreground %>' /></svg>"},
    "horizontal-stripe-2":{width:10,height:10,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'><rect width='10' height='10' fill='<%= background %>' /><rect x='0' y='0' width='10' height='2' fill='<%= foreground %>' /></svg>"},
    "horizontal-stripe-9":{width:10,height:10,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'><rect width='10' height='10' fill='<%= background %>' /><rect x='0' y='0' width='10' height='9' fill='<%= foreground %>' /></svg>"},
    "horizontal-stripe-8":{width:10,height:10,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'><rect width='10' height='10' fill='<%= background %>' /><rect x='0' y='0' width='10' height='8' fill='<%= foreground %>' /></svg>"},
    "dots-8":{width:10,height:10,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'><rect width='10' height='10' fill='<%= background %>' /><rect x='0' y='0' width='8' height='8' fill='<%= foreground %>' /></svg>"},
    "dots-9":{width:10,height:10,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'><rect width='10' height='10' fill='<%= background %>' /><rect x='0' y='0' width='9' height='9' fill='<%= foreground %>' /></svg>"},
    "dots-4":{width:10,height:10,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'><rect width='10' height='10' fill='<%= background %>' /><rect x='0' y='0' width='4' height='4' fill='<%= foreground %>' /></svg>"},
    "dots-5":{width:10,height:10,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'><rect width='10' height='10' fill='<%= background %>' /><rect x='0' y='0' width='5' height='5' fill='<%= foreground %>' /></svg>"},
    "dots-7":{width:10,height:10,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'><rect width='10' height='10' fill='<%= background %>' /><rect x='0' y='0' width='7' height='7' fill='<%= foreground %>' /></svg>"},
    "dots-6":{width:10,height:10,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'><rect width='10' height='10' fill='<%= background %>' /><rect x='0' y='0' width='6' height='6' fill='<%= foreground %>' /></svg>"},
    "dots-2":{width:10,height:10,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'><rect width='10' height='10' fill='<%= background %>' /><rect x='0' y='0' width='2' height='2' fill='<%= foreground %>' /></svg>"},
    "dots-3":{width:10,height:10,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'><rect width='10' height='10' fill='<%= background %>' /><rect x='0' y='0' width='3' height='3' fill='<%= foreground %>' /></svg> "},
    "dots-1":{width:10,height:10,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'><rect width='10' height='10' fill='<%= background %>' /><rect x='0' y='0' width='1' height='1' fill='<%= foreground %>' /></svg>"},
}



window.olGetSvgPattern = function(p,stroke,fill) {
    stroke = stroke||'#000';
    fill = fill||'transparent';

    p=IMDV_PATTERNS[p] ??IMDV_PATTERNS['diagonal-stripe-1'];
    let svg = p.svg.replace(/<%= *background *%>/g,fill).replace(/<%= *foreground *%>/g,stroke);
    let prefix = 'data:image/svg+xml;base64,';
    let url  = prefix+btoa(svg);
    return {
	width:p.width,
	height:p.height,
	url:url
    }
};



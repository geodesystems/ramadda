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
var GLYPH_TYPES_LINES_STRAIGHT = [GLYPH_LINE,GLYPH_POLYLINE];
var GLYPH_TYPES_CLOSED = [GLYPH_POLYGON,GLYPH_FREEHAND_CLOSED,GLYPH_BOX,GLYPH_TRIANGLE,GLYPH_HEXAGON];
var MAP_TYPES = ['geo_geojson','geo_gpx','geo_shapefile','geo_kml'];
var LEGEND_IMAGE_ATTRS = ['style','color:#ccc;font-size:9pt;'];
var BUTTON_IMAGE_ATTRS = ['style','color:#ccc;'];
var CLASS_IMDV_STYLEGROUP= 'imdv-stylegroup';
var CLASS_IMDV_STYLEGROUP_SELECTED = 'imdv-stylegroup-selected';
var IMDV_PROPERTY_HINTS= ['filter.live=true','filter.show=false',
			  'filter.zoomonchange.show=false',
			  'filter.toggle.show=false','showButtons=false','showLegendInMap=true'];


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
        $.getJSON(Ramadda.getUrl('/mapresources.json'), data=>{
	    MAP_RESOURCES_MAP={};
	    MAP_RESOURCES = data;
	    MAP_RESOURCES.forEach((r,idx)=>{MAP_RESOURCES_MAP[idx] = r;});
	}).fail(err=>{
	    console.error('Failed loading mapresources.json:' + err);
	});
    }
    ImageHandler = OpenLayers.Class(OpenLayers.Handler.RegularPolygon, {
	CLASS_NAME:'IMDV Image Handler',
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

	    let url = Ramadda.getUrl('/map/getroute?entryid='+this.display.getProperty('entryId'));
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
    //Set these so the glyphs can access them
    this.ID_LEGEND_MAP = ID_LEGEND_MAP;

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
	    style = Utils.clone({},style?? (feature?.style) ?? {});
	    mapOptions = Utils.clone({},mapOptions??feature?.mapOptions ?? style?.mapOptions);
	    delete style.mapOptions;
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
            let url = Ramadda.getUrl('/geocode?query=' + encodeURIComponent(address));
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
		style.externalGraphic=Ramadda.getUrl(addIt?'/icons/map/marker-blue.png':'/icons/map/marker.png');
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
	    if(angle=='') angle = NaN;
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
		if(skm.endsWith(UNIT_MILES)) {
		    km = 1.60934*parseFloat(skm.replace(UNIT_MILES,''));
		} else if (skm.endsWith(UNIT_FT)) {
		    km = 0.0003048*parseFloat(skm.replace(UNIT_FT,''));
		} else if (skm.endsWith(UNIT_KM)) {
		    km = skm.replace(UNIT_KM,'');
		} else if (skm.endsWith(UNIT_M)) {
		    km = parseFloat(skm.replace(UNIT_M,''))/1000;
		} else {
		    //console.log('unknown unit:' + skm);
		}
		let p1 = MapUtils.createLonLat(center.lon??center.x, center.lat??center.y);
		let p2 = Utils.reverseBearing(p1,Utils.isDefined(angle)&& !isNaN(angle)?angle:90+45,km);
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
		if(!isNaN(angle)) 
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
	    let distancePrefix = 'Distance: ';

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
		let unit = UNIT_FT;
		if(w>5280 || h>5280) {
		    unit = UNIT_MILES;
		    w = w/5280;
		    h = h/5280;		    
		}
		msg= 'W: ' + Utils.formatNumberComma(w,1) + ' ' + unit +
		    ' H: ' + Utils.formatNumberComma(h,1) + ' ' + unit;
	    } else {
		let segments = '';
		let total = 0;
		for(let i=0;i<pts.length-1;i++) {
		    let pt1 = pts[i];
		    let pt2 = pts[i+1];		
		    let d = MapUtils.distance(pt1.y,pt1.x,pt2.y,pt2.x);
		    total+=d;
		    let unit = UNIT_FT;
		    if(d>5280) {
			unit = UNIT_MILES;
			d = d/5280;
		    }
		    d = Utils.formatNumberComma(d);
		    segments+= d +' ' + unit +' ';
		}
		let unit = UNIT_FT;
		if(total>3500) {
		    unit = UNIT_MILES;
		    total = total/5280;
		}
		msg = distancePrefix + Utils.formatNumberComma(total) +' ' + unit;
		if(pts.length>2 && pts.length<6)  {
//		    msg+='<br>Segments:' + segments;
		}
		if(pts.length<=1) msg='';
	    }
	    if(!justDistance&&area>0) {
		unit=UNIT_FT;
		if(area>MapUtils.squareFeetInASquareMile) {
		    unit = UNIT_MILES;
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
	    let tmpStyle = Utils.clone({},glyphType.getStyle());
	    let tmpMapOptions =  tmpStyle.mapOptions = {
		type:glyphType.type
	    }
	    if(glyphType.isFixed() || glyphType.isGroup()) {
		let text = prompt(glyphType.isFixed()?'Text:':'Name:');
		if(!text) return
		let style = Utils.clone({},tmpStyle);
		let mapOptions = Utils.clone({},tmpMapOptions);
		style.text = text;
		if(glyphType.isGroup()) {
		    style.externalGraphic = glyphType.getIcon();
		    mapOptions.name = text;
		}
		this.clearCommands();
		let mapGlyph = new MapGlyph(this,mapOptions.type, mapOptions, null,style);
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
		    let style = Utils.clone({},tmpStyle);
		    let mapOptions = Utils.clone({},tmpMapOptions);
		    mapOptions.name = this.jq('servername').val().trim();
		    this.clearCommands();
		    let mapGlyph = new MapGlyph(this,mapOptions.type, mapOptions, null,style);
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
		    let mapOptions = Utils.clone({},tmpMapOptions);
		    attrs.entryId = entryId;
		    let style = Utils.clone({},tmpStyle);
		    if(glyphType.isImage()) {
			style.strokeColor='#ccc';
			style.fillColor = 'transparent';
		    } else {
			$.extend(mapOptions,attrs);
		    }
		    mapOptions.entryId = entryId;
		    mapOptions.entryType = attrs.entryType;
		    attrs.name = attrs.name??attrs.entryName;
		    delete attrs.entryName;
		    if(glyphType.isMap()) {
			if(resourceId) {
			    let resource  =MAP_RESOURCES_MAP[resourceId];
			    attrs.name = resource.name;
			    attrs.entryType = resource.type;
			    attrs.resourceUrl = resource.url;
			    if(resource.style) $.extend(style,resource.style);
			}

			$.extend(mapOptions,attrs);
			let mapGlyph = this.handleNewFeature(null,style,mapOptions);
			mapGlyph.checkMapLayer();
			this.clearCommands();
			return;
		    } 

		    //Hacky, gotta clean up all of this
		    if(attrs.name && !glyphType.isImage()) {
			style.label = attrs.name;				
		    }

		    if(glyphType.isMultiEntry()) {
			this.clearCommands();
			mapOptions.name = mapOptions.entryName?? attrs.entryName;
			delete mapOptions['entryName']
			let mapGlyph = this.handleNewFeature(null,style,mapOptions);
			mapGlyph.addEntries(true);
			this.clearCommands();
			return
		    }

		    if(glyphType.isData()) {
			this.clearCommands();
			mapOptions.name = mapOptions.entryName?? attrs.entryName;
			delete mapOptions['entryName']
			this.createData(mapOptions);
			this.clearCommands();
			return;
		    }

		    if(glyphType.isImage() && Utils.isDefined(attrs.north) &&
		       Utils.isDefined(attrs.west) &&
		       Utils.isDefined(attrs.south) &&
		       Utils.isDefined(attrs.east)) {
			style.strokeColor='transparent';
			let feature = this.makeFeature(this.getMap(),'OpenLayers.Geometry.Polygon', style,
						       [attrs.north,attrs.west,attrs.north, attrs.east,
							attrs.south,attrs.east,
							attrs.south, attrs.west]);
			attrs.type = GLYPH_IMAGE;
			style.imageUrl = Ramadda.getUrl('/entry/get?entryid=' + entryId);
			let mapGlyph = new  MapGlyph(this, GLYPH_IMAGE,attrs,feature,style);
			mapGlyph.checkImage(feature);
			this.addGlyph(mapGlyph);
			mapGlyph.panMapTo();
			return;
		    }

		    if(glyphType.isEntry() && (Utils.isDefined(attrs.latitude) || Utils.isDefined(attrs.north))) {
			if(confirm('Do you want to use this entry\'s location?')) {
			    style = $.extend({},style);
			    style.externalGraphic =attrs.icon;
			    mapOptions.useentrylocation = true;
			    let points = Utils.isDefined(attrs.latitude)?[attrs.latitude,attrs.longitude]:[attrs.north,attrs.west];
			    let mapGlyph = this.createMapMarker(GLYPH_ENTRY,mapOptions, style,points,true);
			    mapGlyph.applyEntryGlyphs();
			    this.clearCommands();
			    mapGlyph.panMapTo();
			    return
			}
		    }

		    if(glyphType.isImage()) {
			let url = imageUrl??Ramadda.getUrl('/entry/get?entryid=' + entryId);
			this.lastImageUrl = url;
			style.imageUrl = url;
		    } else if(attrs.icon) {
			style.externalGraphic = attrs.icon;
		    }
		    mapOptions.entryId = entryId;
		    mapOptions.name = attrs.name;			    
		    cmd.handler.style = style;
		    style.mapOptions = Utils.clone({},mapOptions);
		    cmd.handler.layerOptions.styleMap=styleMap;
		    this.showCommandMessage(glyphType.isImage()?'Click and drag to create image':'New Entry Marker');
		    cmd.activate();
		    if(this.selector) this.selector.cancel(true);
		};

		if(args.url) {
		    if(args.entryId)
			callback(args.entryId,args);
		    else
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
		let entryType = glyphType.isImage()?'type_image,latlonimage':glyphType.isMap()?Utils.join(MAP_TYPES,','):'';
		props.typeLabel  = glyphType.isImage()?'Images':glyphType.isMap()?'Maps':'';
		this.selector = selectCreate(null, HU.getUniqueId(''),'',false,'entryid',this.getProperty('entryId'),entryType,null,props);
		return
	    } 
	    if(glyphType.getType() == GLYPH_MARKER) {
		let input =  HU.textarea('',this.lastText??'',[ID,this.domId('labeltext'),'rows',3,'cols', 40]);
		let html =  HU.formTable();
		html += HU.formEntryTop('Label:',input);
		let prop = 'externalGraphic';
		let icon = this.lastIcon || tmpStyle.externalGraphic;
		let icons = HU.image(icon,['id',this.domId(prop+'_image')]) +
		    HU.hidden('',icon,['id',this.domId(prop)]);
		if(!Utils.isDefined(this.lastIncludeIcon)) this.lastIncludeIcon = true;
		html += HU.formEntry('',HU.checkbox(this.domId('includeicon'),[],this.lastIncludeIcon,'Include Icon')+' ' + icons);
		html+='</table>';
		html+=HU.div(['style',HU.css('text-align','center','padding-bottom','8px','margin-bottom','8px','border-bottom','1px solid #ccc')], HU.div([CLASS,'ramadda-button-ok display-button'], 'OK') + SPACE2 +
			     HU.div([CLASS,'ramadda-button-cancel display-button'], 'Cancel'));
		
		html+=HU.b('Select Icon');
		html+=HU.div(['id',this.domId('recenticons')]);
		html+=HU.div(['id',this.domId('icons'),'icon-property',prop]);
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
		    let style = Utils.clone({},tmpStyle);
		    style.mapOptions = Utils.clone({},tmpMapOptions);
		    let doIcon = this.lastIncludeIcon  = this.jq('includeicon').is(':checked');
		    if(!doIcon) {
			style.externalGraphic=icon_blank;
		    } else {
			this.lastIcon = this.jq('externalGraphic').val();
			style.externalGraphic = this.lastIcon;
			style.labelAlign='cb';
			style.labelYOffset='12';
		    }
		    let text = this.jq('labeltext').val();
		    closeDialog();
		    //			    if(!Utils.stringDefined(text)) return;
		    this.lastText = text;
		    style.label = text;
		    cmd.handler.style = style;
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
	    cmd.handler.style = Utils.clone({},tmpStyle);
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
	    let catalogUrl = Ramadda.getUrl('/resources/stac_catalogs.json');
	    let stacLinks = [{value:'',label:'Select'}, {value:catalogUrl,label:'Stac Catalogs'},
			     {value:Ramadda.getUrl('/resources/pc_catalog.json'),label:'Planet Earth Catalogs'}];
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
			url =   Ramadda.getUrl('/tifftopng?url=' + encodeURIComponent(url));
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
			mapOptions.icon = Ramadda.getUrl('/icons/xcube.png');
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
	    if(!dom) return;
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
			let re = new RegExp("\"(" + Ramadda.getBaseUrl()+"[^\"]+)\"");
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
	    let url = Ramadda.getUrl("/getwiki");
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
		this.featureChanged();	    
		let style = {};
		if(mapGlyph.isData()) {
		    let displayAttrs = this.parseDisplayAttrs(this.jq('displayattrs').val());
		} else if(props) {
		    props.forEach(prop=>{
			let id = 'glyphedit_' + prop;
			if(prop.toLowerCase().indexOf('externalgraphic')>=0) 
			    id =prop;
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
		if(Utils.stringDefined(style.popupText)) {
		    style.cursor = 'pointer';
		} else {
		    style.cursor = 'pointer';
		}

		if(mapGlyph.isData()) {
		    let displayAttrs = this.parseDisplayAttrs(this.jq('displayattrs').val());
		    mapGlyph.applyDisplayAttrs(displayAttrs);
		} else if(mapGlyph.isMultiEntry()) {
		    mapGlyph.addEntries();
		}

		mapGlyph.applyPropertiesComponent(style);
		mapGlyph.applyPropertiesDialog();


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
	    let patterns = ['',...Object.keys(IMDV_PATTERNS).sort()];
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

	    let isGroup = mapGlyph?mapGlyph.isGroup():false;
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
		dotSize: {label:'Line Dots',strip:'dot'},
		labelAlign: {label:'Label',strip:'label'},
		textBackgroundStrokeColor: {label:'Text Background',strip:'textBackground'},		
	    };
	    props.forEach(prop=>{
		let shared = true;
		let id = "glyphedit_" + prop;
		let domId = this.domId(id);
		if(notProps.includes(prop)) return;
		let header = headers[prop];
		if(header) {
		    strip=header.strip;
		    html+=HU.tr([],HU.td(['colspan',2],HU.div(['class','imdv-form-header'],header.label)));
		}  else if(strip && !prop.startsWith(strip)) {
		    html+=HU.tr([],HU.td(['colspan',2],''));
		    strip = null;
		}
		let label = prop;
		if(strip) label = label.replace(strip,'');
		label =  Utils.makeLabel(label);		
		if(prop=="pointRadius") label = "Size";
		let widget;
		let extra ='';
		if(prop=="externalGraphic" || prop.indexOf('ExternalGraphic')>=0) {
//		    shared = false;
		    label="Marker"
		    let options = "";
		    let graphic = values[prop];
		    if(!Utils.isDefined(graphic))
			graphic = this.getExternalGraphic();
		    domId = this.domId(prop);
		    let div = HU.div(['class','imdv-icons',
				'style','margin-left:5px;display:inline-block;',
				'icon-property',prop,
				      'id',this.domId(prop+"_icons")],'Loading...');
		    widget = HU.hidden("",graphic,['id',domId]) +
			'<table><tr valign=top><td width=1%>' +
			HU.image(graphic,['width','24px','id',this.domId(prop+'_image')]) +
			'</td><td>' +
			div +
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
			if(prop == "pointRadius") {
			    label="Size";
			} 
			if(prop=="textBackgroundFillOpacity" || prop=="textBackgroundPadding" || prop=="strokeWidth" || prop=="pointRadius" || prop=="fontSize" || prop=="imageOpacity" || prop=='dotSize') size="4";
			else if(prop=="fontFamily") size="60";
			else if(prop.toLowerCase().indexOf('url')>=0) size="60";
			else if(prop=='transform') {
			    size="60";
			    extra = HU.space(2) +HU.href('https://developer.mozilla.org/en-US/docs/Web/CSS/transform','Help',['target','_help']);
			}
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
			    let min  = isRotation?-360:(prop.indexOf("Offset")>=0?0:0);
			    let max = isRotation?360:50;
			    let size = 4;
			    if(prop.indexOf("Offset")>=0) size=8;
			    widget =  HU.input("",v,[ID,domId,"size",size])+HU.space(4) +
				
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
		let suffix='';
		if(isGroup && shared) {
		    suffix=HU.span(['title','Apply this style to children glyphs',
				    'class','ramadda-clickable imdv-style-suffix','widget-id',domId,'property',prop],HU.getIconImage('fas fa-folder-tree'));
		}
		html+=HU.formEntry(label+":",widget+suffix+extra);
		html+='\n';
	    });
	    html+="</table>";
	    html = HU.div(['class','imdv-form',STYLE,HU.css("max-height","350px","overflow-y","scroll","margin-bottom","5px")], html);
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
		Object.keys(displayAttrs).sort().forEach(key=>{
		    if(Utils.isDefined(displayAttrs[key])) {
			attrs+=key+"="+ displayAttrs[key]+"\n";
		    }
		});
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

	    dialog.find('.imdv-style-suffix').click(function() {
		let prop = $(this).attr('property');
		let id = $(this).attr('widget-id');
		let value = jqid(id).val();
		if(!value) {
		    value = jqid(id+'_image').val();
		    console.log(prop,id,value);
		}

		if(prop && value) {
		    mapGlyph.applyStyleToChildren(prop,value);
		}
	    });
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
	    let icons =dialog.find('.imdv-icons');
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
			    if(value && Ramadda.isRamaddaUrl(value))
				value = Ramadda.getUrl(value);
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
	    let prop = icons.attr('icon-property');
	    if(used.length>0) {
		let html = HU.b("Recent: ");
		used.forEach(icon=>{
		    html+=HU.span(['class','ramadda-clickable ramadda-icons-recent','icon',icon],
				  HU.getIconImage(icon,['width','24px']));
		});
		this.jq('recenticons').html(html);
		this.jq('recenticons').find('.ramadda-icons-recent').click(function(){
		    let icon = $(this).attr('icon');
		    _this.jq(prop+"_image").attr('src',icon);
		    _this.jq(prop).val(icon);			
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
		html = HU.input("","",['id',this.domId(prop+'_search'),'placeholder','Search','size','30']) +"<br>"+
		    html;
		icons.html(html);
		let _this = this;
		let images = icons.find('.ramadda-imdv-image');
		images.click(function() {
		    let src = $(this).attr('src');
		    _this.jq(prop+"_image").attr('src',src);
		    _this.jq(prop).val(src);			
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
		jqid(this.domId(prop+'_search')).keyup(function() {
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
	    let url = Ramadda.getUrl("/entry/change");
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
	    let url = Ramadda.getUrl("/entry/setfile"); 
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
		let url = Ramadda.getUrl("/entry/get?entryid=" + entryId);
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
			this.loadAnnotationJson(json,this.map);
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

	loadAnnotationJson: function(mapJson,map) {
//	    this.voroni();


	    let glyphs = mapJson.glyphs||[];
	    glyphs.forEach(jsonObject=>{
		let mapGlyph = this.makeGlyphFromJson(jsonObject);
		if(mapGlyph) this.addGlyph(mapGlyph,true);
	    });
	    if(this.getMapProperty('mapLegendPosition')) {
		this.createMapLegendWrapper();
	    }
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
		let mapGlyph = new MapGlyph(this,mapOptions.type, mapOptions,null,style);
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

	    try {
		let mapGlyph = new MapGlyph(this,mapOptions.type, mapOptions, null,style,true,jsonObject);
		return mapGlyph;
	    } catch(err) {
		this.handleError("Error creating glyph",err);
		console.log(err);
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
	    let cbxs = [
		HU.checkbox(this.domId('usercanedit'),[],
			    this.getMapProperty('userCanEdit',false),'User Can Edit'),
		HU.checkbox(this.domId('showopacityslider'),[],
			    this.getMapProperty('showOpacitySlider',this.getShowOpacitySlider()),'Show Opacity Slider'),
		HU.checkbox(this.domId('showgraticules'),[],
			    this.getMapProperty('showGraticules',false),'Show Graticules'),
		HU.checkbox(this.domId('showmouseposition'), [],
			    this.getMapProperty('showMousePosition',false),'Show Mouse Position'),
		HU.checkbox(this.domId('showaddress'), [],
			    this.getMapProperty('showAddress',false),'Show Address')			
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
	    basic+=this.getLevelRangeWidget(this.getMapProperty('visibleLevelRange'),
					    this.getMapProperty('showMarkerWhenNotVisible'));

	    accords.push({header:'Basic', contents:basic});
	    accords.push({header:'Header/Footer',
			  contents:
			  HU.b('Top Wiki Text:') +'<br>' +
			  HU.textarea('',this.getMapProperty('topWikiText',''),
				      ['id',this.domId('topwikitext_input'),'rows','4','cols','80']) +"<br>" +
			  HU.b('Bottom Wiki Text:') +'<br>' +
			  HU.textarea('',this.getMapProperty('bottomWikiText',''),
				      ['id',this.domId('bottomwikitext_input'),'rows','4','cols','80']) +'<br>'
			 });

	    let props = this.getMapProperty('otherProperties','');
	    let lines = ['legendLabel=Some label',...IMDV_PROPERTY_HINTS,
			'graticuleStyle=strokeColor:#000,strokeWidth:1,strokeDashstyle:dot'];
	    let help = 'Add property:' + this.makeSideHelp(lines,this.domId('otherproperties_input'),{suffix:'\n'});
	    accords.push({header:'Other Properties',
			  contents:
			  HU.hbox([
			      HU.textarea('',props,['id',this.domId('otherproperties_input'),'rows','8','cols','60']),HU.space(2),help])
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
		this.setMapProperty('userCanEdit', this.jq('usercanedit').is(':checked'),
				    'showOpacitySlider', this.jq('showopacityslider').is(':checked'),
				    'showGraticules',this.jq('showgraticules').is(':checked'),
				    'showMousePosition', this.jq('showmouseposition').is(':checked'),
				    'showAddress', this.jq('showaddress').is(':checked'),
				    'legendPosition',this.jq('legendposition').val(),
				    'legendWidth',this.jq('legendwidth').val(),
				    'showBaseMapSelect',this.jq('showbasemapselect').is(':checked'),
				    'topWikiText', this.jq('topwikitext_input').val(),
				    'bottomWikiText', this.jq('bottomwikitext_input').val(),
				    'otherProperties', this.jq('otherproperties_input').val());		
		this.parsedMapProperties = null;
		let min = this.jq("minlevel").val().trim();
		let max = this.jq("maxlevel").val().trim();
		if(min=="") min = null;
		if(max=="") max = null;	
		this.setMapProperty('visibleLevelRange', {min:min,max:max},
				    'showMarkerWhenNotVisible', this.jq('showmarkerwhennotvisible').is(':checked'));
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

	    let gratStyle = this.getMapProperty('graticuleStyle');
	    if(Utils.stringDefined(gratStyle)) {
		try {
		    let tmp = {};
		    gratStyle.split(",").forEach(tok=>{
			let toks =tok.split(":");
			if(toks.length==2) {
			    let prop = toks[0].trim();
			    let v =toks[1].trim();
			    if(prop=='strokeWidth') v = parseInt(v);
			    tmp[prop] = v;
			}
		    });
		    gratStyle=tmp;
		} catch(err) {
		    console.log("Error parsing graticule style:" + gratStyle +"\n\terror:" + err);
		    gratStyle;
		}
	    }
	    this.getMap().setGraticulesVisible(this.getMapProperty('showGraticules'),gratStyle);
	    if(this.getMapProperty('showMousePosition'))
		this.getMap().initMousePositionReadout();
	    else
		this.getMap().destroyMousePositionReadout();		
	},
	

	checkOpacitySlider:function() {
	    let visible;
	    if(Utils.isDefined(this.getMapProperty('showOpacitySlider')))
		visible = this.getMapProperty('showOpacitySlider');
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
	    html+= HU.href(Ramadda.getUrl('/userguide/imdv.html'),'Help',['target','_help']);
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
		    let icon = glyphType.options.icon||Ramadda.getUrl("/map/marker-blue.png");
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
			[ID_MOVER,'Move',UNIT_M],
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
		    url =   Ramadda.getUrl('/proxy?url=' + encodeURIComponent(opts.resourceUrl));
		else
		    url =   opts.resourceUrl;
	    } else {
		url = Ramadda.getUrl("/entry/get?entryid="+opts.entryId);
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
		url = Ramadda.getUrl('/entry/show?entryid=' + opts.entryId+'&output=geojson.geojson&formap=true');
		//fall thru to geojson

	    case 'geo_geojson': 
		return this.getMap().addGeoJsonLayer(opts.name,url,true, selectCallback, unselectCallback,style,loadCallback,andZoom,errorCallback);
		break;		

		/*
		  case 'geo_shapefile_fips': 
		  case 'geo_shapefile': 
		  url = Ramadda.getUrl('/entry/show?entryid=' + opts.entryId+'&output=shapefile.kml&formap=true'0;
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
		url =  Ramadda.getUrl("/entry/show?entryid=" + opts.entryId +"&output=kml.doc&converthref=true");

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
		url = Ramadda.getUrl("/entry/get?entryid=" + entryId);
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
			this.parsedMapProperties= null;

			//Check the map legend
			if(this.mapLegendToggleId) {
			    if(Utils.isDefined(this.getMapProperty('mapLegendOpen'))) {
				if(!this.getMapProperty('mapLegendOpen')) {
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
			    this.loadAnnotationJson(json,_this.map);
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
	    if(!Ramadda.isRamaddaUrl(externalGraphic)) externalGraphic = Ramadda.getUrl(externalGraphic);
	    let textBackgroundStyle={
		textBackgroundStrokeColor:'',
		textBackgroundStrokeWidth:1,			   			   
		textBackgroundFillColor:'',
		textBackgroundFillOpacity:1.0,
		textBackgroundPadding:2,
		textBackgroundShape:'rectangle',
		textBackgroundRadius:0};


	    let textStyle = {
		fontColor: this.getProperty("labelFontColor","#000"),
		fontSize: this.getFontSize(),
		fontFamily: this.getFontFamily(),
		fontWeight: this.getFontWeight(),
		fontStyle: this.getFontStyle(),
		labelAlign: this.getProperty("labelAlign","cb"),
		labelXOffset: this.getProperty("labelXOffset","0"),
		labelYOffset: this.getProperty("labelYOffset","14"),
		labelOutlineColor:this.getProperty("labelOutlineColor","#fff"),
		labelOutlineWidth: this.getProperty("labelOutlineWidth","0")};

	    let lineStyle = {strokeColor:this.getStrokeColor(),
			     strokeWidth:this.getStrokeWidth(),
			     strokeOpacity:1,
			     strokeDashstyle:'solid',
			     strokeLinecap: 'butt'};
	    
	    let dotStyle = {dotSize:3,
			    dotStrokeColor:'blue',
			    dotStrokeWidth:1,			   
			    dotFillColor:'blue',
			    dotExternalGraphic:''};
	    
	    new GlyphType(this,GLYPH_GROUP,"Group",
			  Utils.clone(
			      {externalGraphic: externalGraphic,
			       pointRadius:this.getPointRadius(10)},
			      lineStyle,
			      textStyle,
			      {fillColor:'transparent',
				  labelSelect:true},
			      textBackgroundStyle), 
			  MyEntryPoint,
			  {isGroup:true, tooltip:'Add group',			  
			   icon:Ramadda.getUrl("/icons/chart_organisation.png")});
	    new GlyphType(this,GLYPH_MARKER,"Marker",
			  Utils.clone({label : "label",
				       externalGraphic: externalGraphic,
				       pointRadius:this.getPointRadius(10),
				       rotation:0,
				       label:''},
				      textStyle,
				      {fillColor:'transparent',
				       labelSelect:true},textBackgroundStyle), 
			  MyPoint,
			  {icon:Ramadda.getUrl("/map/blue-dot.png")});

	    new GlyphType(this,GLYPH_POINT,"Point",
			  Utils.clone(
			      {graphicName:'circle',
			       pointRadius:6},
			      lineStyle,
			      {
			       fillColor:"blue",
			       fillOpacity:1,
			       rotation:0,
			       label:''},
			      textStyle),
			  MyPoint,
			  {icon:Ramadda.getUrl("/icons/dots/blue.png")});
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
			   icon:Ramadda.getUrl("/icons/sticky-note-text.png")});			    

	    new GlyphType(this,GLYPH_LINE, "Line",
			  Utils.clone(lineStyle, dotStyle),
			  MyPath,
			  {maxVertices:2,
			   icon:Ramadda.getUrl("/icons/line.png")});		
	    new GlyphType(this,GLYPH_POLYLINE, "Polyline",
			  Utils.clone(lineStyle,dotStyle),
			  MyPath,
			  {icon:Ramadda.getUrl("/icons/polyline.png")});
	    new GlyphType(this,GLYPH_POLYGON, "Polygon",
			  Utils.clone(lineStyle,
				      {fillColor:"transparent",
				       fillOpacity:1.0,
				       fillPattern:''}),
			  MyPolygon,
			  {icon:Ramadda.getUrl("/icons/polygon.png")});

	    new GlyphType(this,GLYPH_FREEHAND,"Freehand",
			  Utils.clone(lineStyle),			  
			  MyPath,
			  {freehand:true,icon:Ramadda.getUrl("/icons/freehand.png")});
	    new GlyphType(this,GLYPH_FREEHAND_CLOSED,"Closed",
			  Utils.clone(lineStyle,
				      {fillColor:"transparent",
				       fillOpacity:1.0,fillPattern:''}),
			  MyPolygon,
			  {freehand:true,icon:Ramadda.getUrl("/icons/freehandclosed.png")});

	    new GlyphType(this,GLYPH_BOX, "Box",
			  Utils.clone(lineStyle,{
			      fillColor:"transparent",
			      fillOpacity:1.0,fillPattern:''}),
			  MyRegularPolygon,
			  {snapAngle:90,sides:4,irregular:true,
			   icon:Ramadda.getUrl("/icons/rectangle.png")});
	    new GlyphType(this,GLYPH_CIRCLE, "Circle",
			  Utils.clone(lineStyle,{
			      fillColor:"transparent",
			      fillOpacity:1.0,fillPattern:''}),
			  MyRegularPolygon,
			  {snapAngle:45,sides:40,icon:Ramadda.getUrl("/icons/ellipse.png")});

	    new GlyphType(this,GLYPH_TRIANGLE, "Triangle",
			  Utils.clone(lineStyle,{
			      fillColor:"transparent",
			      fillOpacity:1.0,fillPattern:''}),
			  MyRegularPolygon,
			  {snapAngle:10,sides:3,
			   icon:Ramadda.getUrl("/icons/triangle.png")});				
	    new GlyphType(this,GLYPH_HEXAGON, "Hexagon",
			  Utils.clone(lineStyle,{
			      fillColor:"transparent",
			      fillOpacity:1.0,fillPattern:''}),
			  MyRegularPolygon,
			  {snapAngle:90,sides:6,
			   icon:Ramadda.getUrl("/icons/hexagon.png")});		
	    new GlyphType(this,GLYPH_RINGS,"Range Rings",
			  Utils.clone(lineStyle,
				      {fillColor:"transparent",
				      pointRadius:6,
				      rotation:0},
				      textStyle,
				      textBackgroundStyle),
			  MyPoint,
			  {icon:Ramadda.getUrl("/icons/dot.png")});



	    new GlyphType(this,GLYPH_MAP,"Map File",
			  Utils.clone(lineStyle,
				      {fillColor:"transparent",
				       fillOpacity:1.0,
				       fillPattern:'',
				       pointRadius:6,
				       externalGraphic:'',
				       graphicName:''},
				      textStyle,
				      textBackgroundStyle),
			  MyEntryPoint,
			  {isMap:true,
			   tooltip:"Select a gpx, geojson or  shapefile map",
			   icon:Ramadda.getUrl("/icons/mapfile.png")});	


	    new GlyphType(this,GLYPH_MAPSERVER,"Map Server",
			  {
			      opacity:1.0,
			      legendUrl:""
			  },
			  MyEntryPoint,
			  {isMapServer:true,
			   tooltip:"Provide a Web Map Service URL",
			   icon:Ramadda.getUrl("/icons/drive-globe.png")});	

	    new GlyphType(this,GLYPH_ROUTE, "Route",
			  Utils.clone(lineStyle),						    
			  MyRoute,{icon:Ramadda.getUrl("/icons/route.png")});

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
			   icon:Ramadda.getUrl("/icons/imageicon.png")}
			 );
	    new GlyphType(this,GLYPH_ENTRY,"Entry Marker",
			  Utils.clone(
			      {externalGraphic: Ramadda.getUrl("/icons/entry.png"),
			       pointRadius:12,
			       label:"label"},
			      textStyle),
			  MyEntryPoint,
			  {tooltip:"Add an entry as a marker",
			   isEntry:true,
			   icon:Ramadda.getUrl("/icons/entry.png")});
	    new GlyphType(this,GLYPH_MULTIENTRY,"Multi Entry",
			  Utils.clone(
			      {showLabels:true,
			       pointRadius:12},
			      textStyle),
			  MyEntryPoint,
			  {tooltip:"Display children entries of selected entry",
			   isMultiEntry:true,
			   icon:Ramadda.getUrl("/icons/sitemap.png")});

	    new GlyphType(this,GLYPH_DATA,"Data", {},
			  MyEntryPoint,
			  {isData:true, tooltip:'Select a map data entry to display',
			   icon:Ramadda.getUrl("/icons/chart.png")});

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
	    if(msg!='')
		msg = HU.div(['class','imdv-message-inner'],msg);
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
		this.parsedMapProperties = Utils.parseMap(this.mapProperties.otherProperties,"\n","=")??{};
	    }
	    return this.parsedMapProperties;
	},

	setMapProperty:function() {
	    for(let i=0;i<arguments.length;i+=2) {
		this.mapProperties[arguments[i]]=arguments[i+1];
	    }
	},
	getMapProperty: function(name,dflt) {
	    let debug=false;
	    let value = this.getOtherProperties()[name];
	    if(Utils.isDefined(value)) {
		if(debug) console.log('p1:'+ value);
		return Utils.getProperty(value);
	    }
	    if(Utils.isDefined(value=this.mapProperties[name])) {
		if(debug) console.log('p2:'+ value);
		return value;
	    }
	    value=  this.getProperty(name,dflt);
	    if(debug) console.log('p3:'+ value);
	    return value;
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

	getShowLegendInMap() {
	    return  this.getMapProperty('legendPosition','left')=='map';
	},
	makeLegend: function() {
	    let _this = this;
	    let legendPosition = this.getMapProperty('legendPosition','left');
	    let leftLegend = this.jq(ID_LEGEND_LEFT);
	    let mapLegend = this.jq(ID_LEGEND_MAP_WRAPPER);	    
	    let rightLegend = this.jq(ID_LEGEND_RIGHT);
	    this.jq(ID_LEGEND).remove();
	    //Remove the old one
	    this.jq(ID_LEGEND).remove();
	    let showShapes = this.getMapProperty('showShapes',true);
	    let legendWidth=this.getMapProperty("legendWidth",'200px');
	    if(!Utils.stringDefined(legendWidth)) legendWidth='200px';
	    let legendLabel= this.getMapProperty("legendLabel","");
	    let idToGlyph={};
	    let glyphs = this.getGlyphs();
	    let html = '';
	    if(this.getMapProperty('showBaseMapSelect')) {
		html+=HU.div(['style','margin-bottom:4px;','class','imdv-legend-offset'], HU.b('Base Map: ') +this.getBaseLayersSelect());
	    }

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

	    this.inMapLegend='';
	    glyphs.forEach((mapGlyph,idx)=>{
		html+=mapGlyph.makeLegend({idToGlyph:idToGlyph});
	    });
	    if(glyphs.length)
		html+=HU.div(['id',this.domId('dropend'),'class','imdv-legend-item','style','width:100%;height:1em;'],'');

	    if(Utils.stringDefined(legendLabel)) {
		legendLabel=legendLabel.replace(/\\n/,'<br>');
		html = HU.div([],legendLabel)+html;
	    }
	    let inMap  =(legendPosition=='map');
	    if(html!="") {
		let height= this.getProperty('height');
		let legendHeight= this.getProperty('legendHeight',height);		
		let css  = HU.css('max-width',HU.getDimension(legendWidth),'width',HU.getDimension(legendWidth));
		if(height && !inMap) css+=HU.css('height',legendHeight);
		let attrs = ['class','imdv-legend','style',css]
		html  = HU.div(attrs,html);
	    }

	    if(inMap) {
		this.inMapLegend = html;
	    }
	    this.jq(ID_LEGEND_MAP_WRAPPER).html('');
	    if(this.inMapLegend!='' || inMap) {
		let inMapLegend=HU.div(['id',this.domId(ID_LEGEND_MAP)],this.inMapLegend);
		let toggleResult = {};
		let toggleListener = (id,vis)=>{
		    this.setMapProperty('mapLegendOpen',vis);
		};

		inMapLegend=HU.toggleBlock('Legend' + HU.space(2),inMapLegend,
					   this.getMapProperty('mapLegendOpen',true),
					   {animated:300,listener:toggleListener},toggleResult);
		if(inMap) {
		    inMapLegend = HU.div(['id',this.domId(ID_LEGEND)], inMapLegend);
		}
		this.jq(ID_LEGEND_MAP_WRAPPER).html(inMapLegend);
		this.mapLegendToggleId = toggleResult.id;
	    } 


	    let legendContainer;
	    if(legendPosition=='left') {
		legendContainer=leftLegend;
	    } else   if(legendPosition=='right') {
		legendContainer=rightLegend;
	    } else   if(legendPosition=='map') {
		legendContainer = this.jq(ID_LEGEND_MAP_WRAPPER);
	    } else {
	    }


	    if(legendContainer && !inMap) {
		legendContainer.show();
		legendContainer.html(HU.div(['id',this.domId(ID_LEGEND)],''));
		this.jq(ID_LEGEND).html(html);
	    }

	    this.makeLegendDroppable(null,this.jq('dropend'),null);

	    HU.initToggleBlock(this.jq(ID_LEGEND),(id,visible,element)=>{
		let mapGlyph = idToGlyph[element.attr('map-glyph-id')];
		if(mapGlyph) mapGlyph.setLegendVisible(visible);
	    });


	    if(!legendContainer) {
		return;
	    }
	    legendContainer.find('.imdv-legend-item-edit').click(function(event) {
		event.stopPropagation();
		let id = $(this).attr('glyphid');
		let mapGlyph = _this.findGlyph(id);
		if(!mapGlyph) return;
		_this.editFeatureProperties(mapGlyph);
	    });

	    legendContainer.find('.imdv-legend-item-view').click(function(event) {
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
	    this.initGlyphButtons(legendContainer);

	    let items = legendContainer.find('.imdv-legend-label');
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

	addToMapLegend:function(glyph,contents) {
	    this.inMapLegend +=contents;
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

	    wiki("topwikitext",this.getMapProperty('topWikiText'));
	    wiki("bottomwikitext",this.getMapProperty('bottomWikiText'));	    


	},
	canEdit: function() {
	    //Is it set in the wiki tag?
	    let userCanEdit = this.getUserCanEdit(null);
	    if(Utils.isDefined(userCanEdit))
		return userCanEdit;
	    //Is logged in user
	    if(this.getProperty("canEdit")) return true;
	    return this.getMapProperty('userCanEdit');
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

	    this.createMapLegendWrapper();

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
	    address=HU.span(['style','margin-right:5px'], address);

	    address = HU.div(['style',HU.css('white-space','nowrap','display','none','position','relative'),'id',this.domId(ID_ADDRESS)], address);	    
	    
	    let message = HU.div([ID,this.domId(ID_MESSAGE),'class','imdv-message']);
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


	createMapLegendWrapper:function() {
	    let _this = this;
	    this.jq(ID_LEGEND_MAP_WRAPPER).remove();
	    let legendPosition = this.getMapProperty('mapLegendPosition',{left:'50px',top:'20px'});
	    let legendStyle = '';
	    //	    ['left','top','right','bottom'].forEach(pos=>{
	    ['left','top'].forEach(pos=>{
		if(legendPosition[pos]) {
		    legendStyle+=HU.css(pos,legendPosition[pos]);
		}
	    });

	    //gotta have this here or else the draggable sets it to relative
	    legendStyle+=HU.css('position','absolute');
	    let innerDiv = HU.div(['id',this.domId(ID_LEGEND_MAP_WRAPPER),'class','imdv-legend-map-wrapper','style',legendStyle]);
	    let inner = $(innerDiv);
	    this.jq(ID_MAP_CONTAINER).append(inner);
	    let haveCleared = false;
	    inner.draggable({
		containment:this.jq(ID_MAP_CONTAINER),
		//A bit tricky - we clear all the style when we start dragging
		//so if right or bottom were set then those get nuked
		//because the drag drags left/top
		start:function() {
		    //		    inner.attr('style','position:absolute;');
		},
		stop:function() {
		    let top = inner.position().top;
		    let left = inner.position().left;		    
		    let bottom = top+inner.height();
		    let right = left+inner.width();		    
		    let pw = inner.parent().width();
		    let ph = inner.parent().height();		    
		    let pos = _this.mapProperties.mapLegendPosition = {};
		    pos.left = inner.css('left');		    
		    pos.top = inner.css('top');
		    return
		    let set = (which,v) =>{
			v =  Math.max(0,(parseInt(v)))+'px';
			pos[which] =v;
			//			inner.css(pos,v);
		    }
		    if(top<ph-bottom) set('top',top);
		    else set('bottom',(ph-bottom));
		    if(left<pw-right) set('left',left);
		    else set('right',pw-right);
		}
	    });
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

					  name = name??data.name;
					  if(MAP_TYPES.includes(data.type)) {
					      let glyphType = this.getGlyphType(GLYPH_MAP);
					      let style = $.extend({},glyphType.getStyle());
					      let attrs = {
						  entryId:data.entryid,
						  type:glyphType.type,
						  name:name,
						  entryType:data.type,
					      }
					      let mapGlyph = this.handleNewFeature(null,style,attrs);
					      mapGlyph.checkMapLayer();
					      return;
					  } else {
					      this.setCommand(GLYPH_IMAGE,{url:data.geturl,
									   entryId:data.entryid,
									   name:name});
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
	    let mover =  this.addControl(ID_MOVER,"Click &amp; drag to move",new OpenLayers.Control.DragFeature(this.myLayer,{
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
		    if(Utils.isDefined(this.feature.isDraggable) && !this.feature.isDraggable) return
		    this.theDisplay.checkSelected(this.feature.mapGlyph);
		    this.theDisplay.showDistances(this.feature.geometry,this.feature.type);
		    if(!this.feature.image && this.feature.type!=GLYPH_BOX && !this.feature?.mapGlyph.isImage()) {
			OpenLayers.Control.ModifyFeature.prototype.dragVertex.apply(this, arguments);
			if(this.feature.mapGlyph) {
			    this.feature.mapGlyph.vertexDragged(this.feature,vertex,pixel);
			}
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
    if(!type) {
	console.log("no type");
	console.trace();
	return
    }
    if(!style)
	console.trace();
    if(style.mapOptions) {
	delete style.mapOptions;
    }
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
    "diagonal-stripe-2":{width:10,height:10,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'><rect width='10' height='10' fill='<%= background %>'/><path d='M-1,1 l2,-2 M0,10 l10,-10 M9,11 l2,-2' stroke='<%= foreground %>' stroke-width='2'/></svg>"},
    "diagonal-stripe-3":{width:10,height:10,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'><rect width='10' height='10' fill='<%= background %>'/><path d='M-1,1 l2,-2 M0,10 l10,-10 M9,11 l2,-2' stroke='<%= foreground %>' stroke-width='3'/></svg>"},
    "diagonal-stripe-4":{width:10,height:10,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'><rect width='10' height='10' fill='<%= background %>'/><path d='M-1,1 l2,-2 M0,10 l10,-10 M9,11 l2,-2' stroke='<%= foreground %>' stroke-width='4'/></svg>"},
    "diagonal-stripe-5":{width:10,height:10,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'><rect width='10' height='10' fill='<%= background %>'/><path d='M-1,1 l2,-2 M0,10 l10,-10 M9,11 l2,-2' stroke='<%= foreground %>' stroke-width='5'/></svg>"},
    "diagonal-stripe-6":{width:10,height:10,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'><rect width='10' height='10' fill='<%= background %>'/><path d='M-1,1 l2,-2 M0,10 l10,-10 M9,11 l2,-2' stroke='<%= foreground %>' stroke-width='6a'/></svg>"},
    "subtle-patch":{width:5,height:5,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='5' height='5'><rect width='5' height='5' fill='<%= background %>' /><rect x='2' y='2' width='1' height='1' fill='<%= foreground %>' /></svg>"},
    "whitecarbon":{width:6,height:6,svg:"<svg xmlns='http://www.w3.org/2000/svg' xmlns:xlink='http://www.w3.org/1999/xlink' width='6' height='6'><rect width='6' height='6' fill='<%= background %>'/><g id='c'><rect width='3' height='3' fill='<%= foreground %>'/><rect y='1' width='3' height='2' fill='<%= foreground %>'/></g><use xlink:href='#c' x='3' y='3'/></svg>"},
    "crosshatch":{width:8,height:8,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='8' height='8'><rect width='8' height='8' fill='<%= background %>'/><path d='M0 0L8 8ZM8 0L0 8Z' stroke-width='0.5' stroke='<%= foreground %>'/></svg> "},
    "houndstooth":{width:10,height:10,svg:"<svg width='10' height='10' xmlns='http://www.w3.org/2000/svg'><path d='M0 0L4 4' stroke='#aaa' fill='#aaa' stroke-width='1'/><path d='M2.5 0L5 2.5L5 5L9 9L5 5L10 5L10 0' stroke='<%= foreground %>' fill='<%= foreground %>' stroke-width='1'/><path d='M5 10L5 7.5L7.5 10' stroke='<%= foreground %>' fill='<%= foreground %>' stroke-width='1'/></svg> "},
    "verticalstripe":{width:6,height:49,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='6' height='49'><rect width='3' height='50' fill='<%= foreground %>'/><rect x='3' width='1' height='50' fill='#ccc'/></svg> "},
    "smalldot":{width:5,height:5,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='5' height='5'><rect width='5' height='5' fill='<%= background %>'/><rect width='1' height='1' fill='<%= foreground %>'/></svg>"},
    "lightstripe":{width:5,height:5,svg:"<svg xmlns='http://www.w3.org/2000/svg' width='5' height='5'><rect width='5' height='5' fill='<%= background %>'/><path d='M0 5L5 0ZM6 4L4 6ZM-1 1L1 -1Z' stroke='<%= foreground %>' stroke-width='1'/></svg>"},
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



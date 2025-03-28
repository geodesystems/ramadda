/**
   Copyright (c) 2008-2025 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

var DISPLAY_IMDV = 'imdv';
var DISPLAY_EDITABLEMAP = 'editablemap';
addGlobalDisplayType({
    type: DISPLAY_IMDV,
    label: 'Integrated Map Data',
    category:CATEGORY_MAPS,
    tooltip: makeDisplayTooltip('Integrated Map Data','imdv.png','Create interactive maps with points, routes, data, etc'),        
});


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
var GLYPH_ISOLINE= 'isoline';
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
var GLYPH_OSM_LOCATIONS = 'osmlocations';
var GLYPH_TYPES_SHAPES = [GLYPH_POINT,GLYPH_BOX,GLYPH_CIRCLE,GLYPH_TRIANGLE,GLYPH_HEXAGON,GLYPH_LINE,GLYPH_POLYLINE,GLYPH_FREEHAND,GLYPH_POLYGON,GLYPH_FREEHAND_CLOSED];
var GLYPH_TYPES_LINES_OPEN = [GLYPH_LINE,GLYPH_POLYLINE,GLYPH_FREEHAND,GLYPH_ROUTE];
var GLYPH_TYPES_LINES = [GLYPH_LINE,GLYPH_POLYLINE,GLYPH_FREEHAND,GLYPH_POLYGON,GLYPH_FREEHAND_CLOSED,GLYPH_ROUTE];
var GLYPH_TYPES_LINES_STRAIGHT = [GLYPH_LINE,GLYPH_POLYLINE];
var GLYPH_TYPES_CLOSED = [GLYPH_POLYGON,GLYPH_FREEHAND_CLOSED,GLYPH_BOX,GLYPH_TRIANGLE,GLYPH_HEXAGON];
var MAP_TYPES = ['geo_geojson','geo_gpx','geo_shapefile','geo_kml'];

var LEGEND_IMAGE_ATTRS = [ATTR_STYLE,'color:#ccc;font-size:9pt;'];
var BUTTON_IMAGE_ATTRS = [ATTR_STYLE,'color:#ccc;'];
var CLASS_IMDV_STYLEGROUP= 'imdv-stylegroup';
var CLASS_IMDV_STYLEGROUP_SELECTED = 'imdv-stylegroup-selected';
var PROP_DONT_SHOW_IN_LEGEND='dontShowInLegend';
var PROP_SHOW_LAYER_SELECT_IN_LEGEND = "showLayerSelectInLegend";
var PROP_LAYERS_STEP_SHOW= "showLayersStep";
var PROP_LAYERS_SHOW_SEQUENCE= "showLayersInSequence";
var PROP_LAYERS_ANIMATION_SHOW = "showLayersAnimation";
var PROP_LAYERS_ANIMATION_PLAY = "layersAnimationPlay";
var PROP_SHOW_CONTROL_IN_HEADER= "showControlInHeader";

var PROP_MOVE_TO_LATEST_LOCATION = "moveToLatestLocation";
var PROP_LAYERS_ANIMATION_DELAY = "layersAnimationDelay";
var PROP_LAYERS_ANIMATION_ON = "layersAnimatioOn";

var ATTR_BUTTON_COMMAND='buttoncommand';

var IMDV_PROPERTY_HINTS= ['filter.live=true','filter.show=false',
			  'filter.zoomonchange.show=false',
			  'filter.toggle.show=false',
			  'filter.sortOnCount=true',
			  'filter.showRawValues=true',
			  'legendTooltip=',
			  'showLabelInMap=true',
			  PROP_MOVE_TO_LATEST_LOCATION+'=true',
			  'showLabelInMapWhenVisible=true',
			  'showViewInLegend=true',
			  PROP_SHOW_LAYER_SELECT_IN_LEGEND +'=true',			  
			  'inMapLabel=',			  			  
			  'showLegendInMap=true',			  
			  PROP_DONT_SHOW_IN_LEGEND +'=true',
			  'mapLegendHeight=300px',
			  'showLegendBox=true',
			  'showButtons=false',
			  'showMeasures=false',
			  'showTextSearch=true'];


var IMDV_GROUP_PROPERTY_HINTS= [PROP_LAYERS_STEP_SHOW+'=true',
				PROP_LAYERS_SHOW_SEQUENCE+'=true',
				PROP_LAYERS_ANIMATION_SHOW+'=true',
				PROP_LAYERS_ANIMATION_DELAY+'=1000',
				PROP_LAYERS_ANIMATION_PLAY+'=true'];				



var CLASS_LEGEND_LABEL = 'imdv-legend-label';
var CLASS_LEGEND_LABEL_INVISIBLE = 'imdv-legend-label-invisible';
var CLASS_LEGEND_LABEL_HIGHLIGHT = 'imdv-legend-label-highlight';
var CLASS_LEGEND_ITEM = 'imdv-legend-item';
var CLASS_LEGEND_OFFSET = 'imdv-legend-offset';
var CLASS_LEGEND_INNER = 'imdv-legend-inner';
var CLASS_LEGEND_ITEM_VIEW = 'imdv-legend-item-view';
var CLASS_LEGEND_ITEM_DROPPABLE= 'imdv-legend-item-droppable';



var CLASS_FILTER_SLIDER = 'imdv-filter-slider';
var CLASS_FILTER_SLIDER_LABEL = 'imdv-filter-slider-label';
var CLASS_FILTER_PLAY = 'imdv-filter-play';
var CLASS_FILTER_STRING = 'imdv-filter-string';
var CLASS_FILTER_STRINGS = 'imdv-filter-strings';
 

var ID_GLYPH_ID='glyphid';
var ID_GLYPH_LEGEND = 'glyphlegend';

var ID_LEVEL_RANGE_SLIDER = 'level_range_slider';
var ID_LEVEL_RANGE_CLEAR = 'level_range_clear';
var ID_LEVEL_RANGE_CHANGED = 'level_range_changed';
var ID_LEVEL_RANGE_MIN = 'level_range_min';
var ID_LEVEL_RANGE_MAX = 'level_range_max';

var ID_LEVEL_RANGE_SAMPLE_MIN = 'level_range_sample_min';
var ID_LEVEL_RANGE_SAMPLE_MAX = 'level_range_sample_max';
var ID_OSM_LABEL = 'osmlabel';
var ID_OSM_TEXT = 'osmtext';

let ImdvUtils = {
    getImdv: function(id) {
	return Utils.displaysMap[id];
    },


    applyFeatureStyle:function(feature,style) {
	if(!feature.style) {
	    feature.style=style;
	    return;
	}
	let geom = feature?.geometry?.CLASS_NAME;
	//If it is a point and has a graphic then we want to try to use the original graphic
	//if there isn't one in the new style
	if(geom=='OpenLayers.Geometry.Point' && Utils.stringDefined(feature.style.externalGraphic)) {
	    style = $.extend({},style);
	    if(!feature.originalGraphic)
		feature.originalGraphic = feature.style.externalGraphic;

	    if(!Utils.stringDefined(style.externalGraphic)) {
		style.externalGraphic = feature.originalGraphic;
	    }
	    feature.style= style;
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
//	    console.log('redrawing layer:',layer.name);
	    layer.redraw();
	    layer.redrawPending = false;
	},1)
    }
}


function RamaddaImdvDisplay(displayManager, id, properties) {
    this.mapProperties = {};
    Utils.importJS(ramaddaBaseHtdocs+'/wiki.js');
    MapUtils.initMapResources();
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
//		mapGlyph.checkImage(null,true);
		mapGlyph.setRotation(mapGlyph.style.rotation);
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
	    this.display.createRoute(this.display.routeProvider,this.display.routeType,pts,{line:line});
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
    const ID_GLYPH_LABELS  ='glyphlabels';
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
    const ID_MENU_VIEW = 'menu_view';
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
    const ID_MAP_LABEL = 'map_label';
    const ID_MAP_PROPERTIES = 'mapproperties';
    const ID_MAP_REGIONS = 'showregions';
    const ID_MAP_CHOOSE = 'chooselatlon';    
    const ID_MAP_VIEWLAYERS = 'viewlayers';
    const ID_MAP_RESETMAPVIEW = 'resetmapview';
    const ID_MAP_MYLOCATION = 'mylocation';    
    const ID_DROP_BEGINNING = 'dropbeginning';
    const ID_DROP_END = 'dropend';
    //Set these so the glyphs can access them
    this.ID_LEGEND_MAP = ID_LEGEND_MAP;

//    if(!Utils.isDefined(properties.showOpacitySlider)&&!Utils.isDefined(getGlobalDisplayProperty('showOpacitySlider'))) 
//	properties.showOpacitySlider=false; 
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
	{p:'userCanChange',tt:'Set to false to not show menubar, etc for all users'},
	{p:'showLegendShapes',d:true,canCache:true},	
	{p:'showMapLegend',d:false,canCache:true},

    ];
    
    displayDefineMembers(this, myProps, {
	commands: [],
        myLayer: [],
	glyphs:[],
	markers:{},
	minLevel:2,maxLevel:20,
	levels: [['','None'],[2,'2 - Most zoomed out'],3,4,5,6,7,8,
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

	isRouteEnabled:function() {
	    return this.isHereEnabled() || this.getProperty('googleRoutingEnabled');
	},
	isIsolineEnabled:function() {
	    return this.getProperty('isolineEnabled');
	},
	isHereEnabled:function() {
	    return this.getProperty('hereRoutingEnabled');
	},
	handleNewRoute:function(cmd,pts) {
	    let html = this.createRouteForm();
	    let buttons  =HU.div([ATTR_CLASS,'ramadda-button-ok display-button'], 'OK') + SPACE2 +
		HU.div([ATTR_CLASS,'ramadda-button-cancel display-button'], 'Cancel');	    
	    html+=HU.div([ATTR_STYLE,HU.css('text-align','right','margin-top','5px')], buttons);
	    html=HU.div([ATTR_STYLE,HU.css('margin','5px')],html);
	    let dialog = HU.makeDialog({content:html,title:'Select Route Type',header:true,my:'left top',at:'left bottom',anchor:this.jq(ID_MENU_NEW)});
	    let message = 'New Route';
	    let ok = ()=>{
		this.routeProvider = this.jq('routeprovider').val();
		this.routeType = this.jq('routetype').val();
		dialog.remove();
		if(pts) {
		    this.createRoute(this.routeProvider,this.routeType,pts,{
			doSequence:true});
		    return
		}
		if(cmd) {
		    cmd.handler.finishedWithRoute = false;
		    this.showCommandMessage(message+': ' + Utils.makeLabel(this.routeType)+' - Draw one or more line segments');
		    cmd.activate();
		}
	    };
	    dialog.find('.ramadda-button-ok').button().click(ok);
	    dialog.find('.ramadda-button-cancel').button().click(()=>{
		dialog.remove();
	    });
	},
	createRouteForm:function(addSequence) {
	    let html='';
	    if(this.isRouteEnabled()) {
		html+=HU.formTable();
		let providers = [];
		if(this.getProperty('googleRoutingEnabled')) providers.push('google');
		if(this.getProperty('hereRoutingEnabled')) providers.push('here');			
		html+=HU.formEntry('Provider:', HU.select('',[ATTR_ID,this.domId('routeprovider')],providers,this.routeProvider));
		html+=HU.formEntry('Route Type:' , HU.select('',[ATTR_ID,this.domId('routetype')],['car','bicycle','pedestrian'],this.routeType));
		if(addSequence) {
		    if(this.getProperty('hereRoutingEnabled')) {
			html += HU.formEntry('',HU.checkbox(this.domId('routedosequence'),[],false,
							    'Calculate best route from locations'));
		    }
		}
		html += HU.close(TAG_TABLE);
	    } else {
		html="Routing is not enabled";
	    }

	    return html;
	},

	createRoute:function(provider,mode,pts,args) {
	    args = args??{};

	    let xys = [];
	    pts.forEach(pt=>{
		xys.push(Utils.trimDecimals(pt.y,6));
		xys.push(Utils.trimDecimals(pt.x,6));
	    });

	    let routeArgs = {
		mode:mode??'car',
		points:Utils.join(xys,','),
		provider:provider
	    };	    
	    if(args.doSequence) {
		routeArgs.dosequence=true;
	    }	    	    
	    let url = Ramadda.getUrl('/map/getroute?entryid='+this.getProperty('entryId'));


	    let reset=  ()=>{
		this.makingRoute = false;
		this.finishedWithRoute = false;
		this.clearMessage2();
		this.getMap().clearAllProgress();
		this.setCommandCursor();
	    };



	    let handleRouteData = data=>{
		reset();
		if(data.errors && data.errors.length) {
		    alert("An error occurred:" + data.errors[0]);
		    return;
		}
		if(data.error_description) {
		    alert("An error occurred:" + data.error_description);
		    return;
		}
		if(args.line)
		    this.myLayer.removeFeatures([args.line]);
		if(data.error) {
		    this.handleError(data.error);
		    return;
		}

//		console.dir(data);

		let instructions=[];
		let points = [];
		if(data.results && data.results.length>0) {
		    //for the route sequence from here
		    let result = data.results[0];
		    if(result.waypoints) {
			result.waypoints.forEach(pt=>{
			    points.push(MapUtils.createPoint(pt.lng,pt.lat));
			});		    
		    }
		} else if(data.routes && data.routes.length>0) {
		    let routeData = data.routes[0];
//		    console.dir(data);
		    if(routeData.legs) {
			routeData.legs.forEach(leg=>{
			    instructions.push(...leg.steps.map(step=>{
				return {instr:step.html_instructions,
					lat:step?.start_location.lat,
					lon:step?.start_location.lng}
			    }));
			});
		    }
		    if(routeData.overview_polyline) {
			let d = googleDecode(routeData.overview_polyline.points);
			d.forEach(pair=>{
			    points.push(MapUtils.createPoint(pair[1],pair[0]));
			});
		    } else {
			//from Here
			routeData.sections.forEach(section=>{
			    let decoded = hereDecode(section.polyline);
			    decoded.polyline.forEach(pair=>{
				points.push(MapUtils.createPoint(pair[1],pair[0]));
			    });
			    if(section.actions) {
				instructions.push(...section.actions.map(action=>{
				    return {instr:action.instruction,loc:action.start_location}
				}));
			    }
			});
		    }
		} else {

		}
//		console.log(instructions);
		if(points.length==0) {
		    alert('No routes found');
		    this.clearMessage2();
		    return;
		}

		if(routeArgs.dosequence) {
		    routeArgs.dosequence=false;
		    let xys = [];
		    points.forEach(pt=>{
			xys.push(Utils.trimDecimals(pt.y,6));
			xys.push(Utils.trimDecimals(pt.x,6));
		    });
		    $.post(url, routeArgs,data=>{
			handleRouteData(data);
		    }).fail(fail);
		    return;
		}


		let  route = this.getMap().createPolygon('', '', points, {
		    strokeWidth:4
		},null,true);
		let glyphType = this.getGlyphType(GLYPH_ROUTE);
		route.style = Utils.clone(glyphType.getStyle());
		this.addFeatures([route]);
		let name = null;
		if(provider)
		    name ="Route: " + provider  +" - " +mode;

		this.handleNewFeature(route,null,{name:name,type:GLYPH_ROUTE,routeProvider:this.routeProvider,routeType:this.routeType,instructions:instructions});
		this.showDistances(route.geometry,GLYPH_ROUTE,true);
	    };

	    let fail = err=>{
		reset();
		if(args.line)
		    this.myLayer.removeFeatures([args.line]);
		this.clearCommands();
		this.handleError(err);
	    }

	    this.finishedWithRoute = true;
	    this.showProgress('Creating route...');
	    this.makingRoute = true;
	    $.post(url, routeArgs,data=>{
		handleRouteData(data);
	    }).fail(fail);
	},	    
	addIsolineForCurrentMarker() {
	    if(!this.currentLocationMarker) return;
	    this.addIsolineAt(this.currentLocationMarker.location);
	},
	addIsolineForMarker:function(glyph) {
	    let center = this.getMap().transformProjPoint(glyph.getCentroid());
	    this.addIsolineAt(center);
	},

	addIsolineAt:function(center,lon) {
	    if(Utils.isDefined(lon) && (typeof center=='number')) {
		center = {y:center,x:lon};
	    }
	    this.getMap().closePopup();
	    let html = HU.formTable();
	    html+=HU.formEntry('Mode:' , HU.select('',[ATTR_ID,this.domId('isolinemode')],['car','bicycle','pedestrian'],this.isolineMode));
	    html+=HU.formEntry('Range:' , HU.input('',this.isolineValue??'10',[ATTR_ID,this.domId('isolinevalue'),'size','5']) +
			       HU.space(2)+
			       HU.select('',[ATTR_ID,this.domId('isolinetype')],[{label:'Time (minutes)',value:'time'},{label:'Distance (miles)',value:'distance'}],this.isolineType));	    
	    html+=HU.formTableClose();
	    let buttons  =HU.div([ATTR_CLASS,'ramadda-button-ok display-button'], 'OK') + SPACE2 +
		HU.div([ATTR_CLASS,'ramadda-button-cancel display-button'], 'Cancel');	    
	    html+=HU.div([ATTR_STYLE,HU.css('text-align','right','margin-top','5px')], buttons);
	    html=HU.div([ATTR_STYLE,HU.css('margin','5px')],html);
	    let dialog = HU.makeDialog({content:html,title:'Select Isoline Type',draggable:true,header:true,my:'left top',at:'left bottom',anchor:this.jq(ID_MENU_NEW)});
	    let ok = ()=>{
		this.isolineMode=this.jq('isolinemode').val();
		this.isolineValue=this.jq('isolinevalue').val();		
		this.isolineType=this.jq('isolinetype').val();		
		dialog.remove();
		this.createIsoline(this.isolineMode,this.isolineValue,this.isolineType,Utils.getDefined(center.y,center.lat),Utils.getDefined(center.x,center.lon),{});
	    };
	    dialog.find('.ramadda-button-ok').button().click(ok);
	    dialog.find('.ramadda-button-cancel').button().click(()=>{
		dialog.remove();
	    });
	},
	createIsoline:function(mode,value,type,latitude,longitude,args) {
	    args = args??{};
	    let originalValue = value;
	    let unit;
	    if(type=='time') {
		//Convert to seconds
		value = 60*parseFloat(value);
		unit = "minutes";
	    } else {
		//convert to meters
		value = parseInt(1609.34*parseFloat(value));
		unit = "miles";
	    }

	    let url = Ramadda.getUrl('/map/getisoline?entryid='+this.getProperty('entryId'));
	    let routeArgs = {
		mode:mode??'car',
		latitude:latitude,
		longitude:longitude,
		'rangetype':type,
		'rangevalue':value
	    };	    
	    let reset=  ()=>{
		this.clearMessage2();
		this.getMap().clearAllProgress();
		this.setCommandCursor();
	    };

	    let handleIsolineData = data=>{
		reset();

		if(data.status==400) {
		    alert("An error occurred:" + data.cause);
		    return;
		}

		if(data.errors && data.errors.length) {
		    alert("An error occurred:" + data.errors[0]);
		    return;
		}
		if(data.error_description) {
		    alert("An error occurred:" + data.error_description);
		    return;
		}
		if(data.error) {
		    this.handleError(data.error);
		    return;
		}
		if(!data.isolines || !data.isolines[0]) {
		    alert('No isolines found');
		    return;
		}

		let latLons = hereDecode(data.isolines[0].polygons[0].outer).polyline;
		if(!latLons || latLons.length==0) {
		    alert('No isolines found');
		    this.clearMessage2();
		    return;
		}

		let points=[];
		latLons.forEach(pair=>{
		    points.push(MapUtils.createPoint(pair[1],pair[0]));
		});

		let  isoLine = this.getMap().createPolygon('', '', points, {
		    strokeWidth:4
		},null,false);
		let glyphType = this.getGlyphType(GLYPH_ISOLINE);
		isoLine.style = Utils.clone(glyphType.getStyle());
		this.addFeatures([isoLine]);
		let name = "Isoline: " + mode;
		let icon ='/icons/' + mode+'.png';
		this.handleNewFeature(isoLine,null,{name:name,icon:Ramadda.getUrl(icon),type:GLYPH_ISOLINE,legendText:"Mode: " + mode+"\nRange: " + originalValue+" " + unit});
		this.showDistances(isoLine.geometry,GLYPH_ISOLINE,true);
	    };

	    let fail = err=>{
		reset();
		if(args.line)
		    this.myLayer.removeFeatures([args.line]);
		this.clearCommands();
		this.handleError(err);
	    }

	    this.finishedWithRoute = true;
	    this.showProgress('Creating isoline...');
	    this.makingRoute = true;
	    $.post(url, routeArgs,data=>{
		handleIsolineData(data);
	    }).fail(fail);
	},	    


	handleEvent:function(event,lonlat) {
	    return;
	},
        handleKeyUp:function(event) {
	    if(event.key!='Shift') return false;
	    this.getSelected().forEach(glyph=>{
		if(glyph.isImage() && glyph.getImage()) {
                    glyph.getImage().setOpacity(1);
		}
	    });
	    return true;
	},
        handleKeyDown:function(event) {
	    if(event.key!='Shift') return false;
	    this.getSelected().forEach(glyph=>{
		if(glyph.isImage() && glyph.getImage()) {
                    glyph.getImage().setOpacity(0.3);
		}
	    });
	    return true;
	},	

	handleNewFeature:function(feature,style,mapOptions) {
	    style = Utils.clone({},style?? (feature?.style) ?? {});
	    mapOptions = Utils.clone({},mapOptions??feature?.mapOptions ?? style?.mapOptions);
	    delete style.mapOptions;
	    if(feature?.style?.mapOptions)
		delete feature.style.mapOptions;
	    let mapGlyph = new MapGlyph(this,mapOptions.type, mapOptions, feature,style);
	    this.addGlyph(mapGlyph);
	    mapGlyph.glyphCreated();
	    this.clearMessage2(1000);
	    /*
	      not now...
	    if(mapGlyph.isMap()) {
	    setTimeout(()=>{mapGlyph.panMapTo();},100);
	    }
	    */
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
			html+=HU.div(['index',idx,ATTR_CLASS,HU.classes(CLASS_CLICKABLE,'ramadda-menu-item')],loc.name);
		    });
		    html = HU.div([ATTR_STYLE,'max-height:200px;overflow-y:auto;'], html);
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

	makeRangeRings:function(center,radii,style,angle,ringStyle,labels) {
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
		} else if(skm.endsWith(UNIT_NM)) {
		    km = 1.852*parseFloat(skm.replace(UNIT_NM,''));
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
		if(!isNaN(angle)) {
		    if(labels && idx<labels.length) {
			s.label = labels[idx].replace("${d}",skm);
		    } else {
			s.label=skm;
		    }
		}
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
	    if(components==null) {
		if(geometry.x && geometry.y) {
		    let pt = MapUtils.createPoint(geometry.x,geometry.y);
		    pt = this.getMap().transformProjPoint(pt);
		    return [pt];
		}
		return null;
	    }
	    if(components.length) {
		if(components[0].components) components = components[0].components;
	    }
	    let pts = components.map(pt=>{
		return  this.getMap().transformProjPoint(pt)
	    });
	    return pts;
	},

	getDistances:function(geometry,glyphType,justDistance,forceAcres,asObject) {
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
	    let feet=0;

	    let msg = 'Distance: ';
	    if(glyphType == GLYPH_BOX || glyphType == GLYPH_IMAGE) {
		msg = '';
		let w = MapUtils.distance(pts[0].y,pts[0].x,pts[1].y,pts[1].x);
		let h = MapUtils.distance(pts[1].y,pts[1].x,pts[2].y,pts[2].x);		
		feet = w*2+h*2;
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
		    if(!isNaN(d)) {
			feet+=d;
		    }
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
	    if(asObject) {
		return {
		    feet:feet,
		    sqfeet:area,
		    sqmiles:area/MapUtils.squareFeetInASquareMile,
		    acres:acres

		}
	    }
	    if(!justDistance&&area>0) {
		unit=UNIT_FT;
		if(area>MapUtils.squareFeetInASquareMile) {
		    unit = UNIT_MILES;
		    area = area/MapUtils.squareFeetInASquareMile;
		    msg+=   '<br>' +
			'Area: ' + Utils.formatNumber(area) +' sq ' + unit;
		    if(forceAcres) {
			msg+=   SPACE + Utils.formatNumber(acres) +' acres';
		    }
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
	    return HU.div([ATTR_STYLE,'margin:5px;'],html);
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
	    if(!command) {
		return;
	    }
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
	    if(glyphType.isOSM()) {
		this.initOSMSearch();
		return;
	    }

	    args = args ??{};
	    this.setCommandCursor();
	    let styleMap = MapUtils.createStyleMap({'default':{}});
	    let tmpStyle = Utils.clone({},glyphType.getStyle());
	    let tmpMapOptions =  tmpStyle.mapOptions = {
		type:glyphType.type
	    }
	    if(glyphType.isFixed() || glyphType.isGroup()) {
		let text = args.text;
		if(!text) text = prompt(glyphType.isFixed()?'Text:':'Name:');
		if(!text) return;
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
		return mapGlyph;
	    }

	    if(glyphType.isMapServer()) {
		if(this.mapServerDialog) {
		    this.mapServerDialog.show();
		    return;
		}
		let wmtHtml = '';
		wmtHtml+='<br>';
		wmtHtml+='<br>Or enter either a TMS/WMS server URL with a layer name:';
		let form = (label,name,size)=>{
		    wmtHtml+=HU.formEntry(label+':',HU.input('',this.cache[name]??'',[ATTR_ID,this.domId(name),'size',size??'60']));
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
		wmtHtml+='<br>Or enter Tile JSON URL:';
		wmtHtml +=  HU.formTable();
		form("Tile JSON",'tilejson',60);
		wmtHtml +=  '</table>';

		let contents = [];
		let ids =Utils.mergeLists([{value:'',label:'Select'}],RAMADDA_MAP_LAYERS.map(l=>{return [l.id,l.name]}));
		let predefined =  HU.select('',[ATTR_ID,this.domId('predefined')],ids,this.cache['predefined']);	
		let html = 'Pre-defined layer: ' + predefined;
		html+=wmtHtml;


		let buttons = HU.buttons([
		    HU.div([ATTR_CLASS,'ramadda-button-ok display-button'], 'OK'),
		    HU.div([ATTR_CLASS,'ramadda-button-cancel display-button'], 'Cancel')]);
		html+=buttons;
		contents.push({label:'WMS/WMTS',contents:html});

		let datacube = HU.div([ATTR_ID,this.domId('datacube_contents')],'Loading...');
//		contents.push({label:'Data Cubes',contents:datacube});

		let stac = HU.div([ATTR_ID,this.domId('stac_contents')]);
//		contents.push({label:'STAC',contents: stac});

//		let tabs = HU.makeTabs(contents)
		//For now just show the WMS
		let tabs = HU.div([],HU.b(contents[0].label)) +
		    contents[0].contents;
//		html=HU.div([ATTR_STYLE,'min-width:600px;min-height:400px;margin:10px;'], tabs.contents);
		html=HU.div([ATTR_STYLE,'min-width:600px;min-height:400px;margin:10px;'], tabs);

		let dialog = this.mapServerDialog = HU.makeDialog({remove:false,content:html,title:'Map Server',header:true,my:'left top',at:'left bottom',draggable:true,anchor:this.jq(ID_MENU_NEW)});
		//We don't want to remove the dialog, just show it
		dialog.remove= () =>{
		    dialog.hide();
		}
//		tabs.init();
		this.initDatacube(dialog);
		this.initStac(dialog);
		let cancel = ()=>{
		    dialog.hide();
		}
		let loadLayer= (args) =>{
		    args = args??{};
		    let style = Utils.clone({},tmpStyle);
		    let mapOptions = Utils.clone({},tmpMapOptions);
		    mapOptions.bounds=args.bounds;
		    mapOptions.name = args.name;
		    mapOptions.legendText = args.legendText;
		    this.clearCommands();
		    let mapGlyph = new MapGlyph(this,mapOptions.type, mapOptions, null,style);
		    mapGlyph.setMapServerUrl(args.url,args.layerName,args.legendUrl,this.jq('predefined').val());
		    mapGlyph.checkMapServer();
		    this.addGlyph(mapGlyph);
		    this.clearMessage2(1000);
		    dialog.remove();
		    if(mapGlyph.hasBounds()) {
			mapGlyph.panMapTo();
		    }
		}
		let ok = ()=>{
		    args.forEach(a=>{
			this.cache[a[1]] =  this.jq(a[1]).val().trim();
		    });
		    let predefined = this.jq('predefined').val().trim();
		    let tileJson = this.jq('tilejson').val();
		    if(Utils.stringDefined(tileJson)) {
			$.getJSON(tileJson, (data)=> {
			    let args = {
				name:data.name,
				url:data.tiles[0],
			    };
			    if(data.attribution && data.attribution_link) {
				args.legendText = HU.href(data.attribution_link,"Courtesy: "+ data.attribution,['target',
												  '_other']);
			    }
			    if(data.bounds) {
				args.bounds=data.bounds;
			    }
			    loadLayer(args);
			}).fail((data)=>{
			    window.alert('Failed to find address');
			});
			return;
		    }

		    let url = this.jq('serverurl').val();
		    if(!Utils.stringDefined(url) && !Utils.stringDefined(predefined)) {
			alert('Please enter a map server');
			return;
		    }
		    loadLayer({
			name:this.jq('servername').val(),
			url:url,
			layerName:this.jq('wmslayer').val(),
			legendUrl:this.jq('maplegend').val()
		    });

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

		    if(glyphType.isMultiEntry()) {
			style.externalGraphic = attrs.icon??glyphType.getIcon();
		    }

		    if(glyphType.isImage()) {
			style.strokeColor='#ccc';
			style.fillColor = 'transparent';
		    } else {
			$.extend(mapOptions,attrs);
		    }
		    mapOptions.entryId = entryId;
		    mapOptions.entryType = attrs.entryType;
		    mapOptions.thumbnailUrl = attrs.thumbnailUrl;
		    attrs.name = attrs.name??Utils.makeLabel(attrs.entryName);
		    delete attrs.entryName;
		    if(glyphType.isMap()) {
			if(resourceId) {
			    let resource  =MapUtils.MAP_RESOURCES_MAP[resourceId];
			    attrs.name = Utils.makeLabel(resource.name);
			    attrs.entryType = resource.type;
			    attrs.resourceUrl = resource.url;
			    if(resource.style) $.extend(style,resource.style);
			}

			$.extend(mapOptions,attrs);
			let mapGlyph = this.handleNewFeature(null,style,mapOptions);
			mapGlyph.checkMapLayer(true);
//			this.clearCommands();
			return;
		    } 

		    //Hacky, gotta clean up all of this
		    if(attrs.name && !glyphType.isImage()) {
			style.label = attrs.name;				
		    }

		    if(glyphType.isMultiEntry()) {
			this.clearCommands();
			mapOptions.name = mapOptions.entryName?? attrs.entryName;
			delete mapOptions['icon'];
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
			if(!attrs.isImage) {
			    style.imageUrl = attrs.thumbnailUrl;
			    if(!style.imageUrl) {
				alert("Selected entry does not have an image");
				return;
			    }
			} else {
			    style.imageUrl = Ramadda.getUrl('/entry/get?entryid=' + entryId);
			}
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
			    mapOptions.name = attrs.name;
			    let points = Utils.isDefined(attrs.latitude)?[attrs.latitude,attrs.longitude]:[attrs.north,attrs.west];
			    let mapGlyph = this.createMapMarker(GLYPH_ENTRY,mapOptions, style,points,true);
			    mapGlyph.applyDataIcon();
			    this.clearCommands();
			    mapGlyph.panMapTo();
			    return
			}
		    }

		    if(glyphType.isImage()) {
			let url = imageUrl ?? attrs.url;
			if(!url) {
			    if(!attrs.isImage) {
				url = attrs.thumbnailUrl;
				if(!url) {
				    alert("Selected entry does not have an image X");
				    return;
				}
			    } else {
				url = Ramadda.getUrl('/entry/get?entryid=' + entryId);
			    }
			}
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
		    extra = HU.b('Enter Image URL: ') + HU.input('',this.lastImageUrl??'',[ATTR_ID,this.getDomId('imageurl'),'size','40']);
		} else if(glyphType.isMap() && MapUtils.MAP_RESOURCES) {
		    let ids = MapUtils.MAP_RESOURCES.map((r,idx)=>{
			return [idx,r.name];
		    });
		    ids = Utils.mergeLists([['','Select Resource']],ids);
		    extra = HU.b('Load Map: ') + HU.select('',[ATTR_ID,this.domId('mapresource')],ids);
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
		let entryType = glyphType.isImage()?'type_image,type_document_pdf,geo_gdal,latlonimage':glyphType.isMap()?Utils.join(MAP_TYPES,','):'';
		props.typeLabel  = glyphType.isImage()?'Images':glyphType.isMap()?'Maps':'';
		this.selector = RamaddaUtils.selectCreate(null, HU.getUniqueId(''),'',false,'entryid',this.getProperty('entryId'),entryType,null,props);
		return
	    } 
	    if(glyphType.getType() == GLYPH_MARKER) {
		let input =  HU.textarea('',this.lastText??'',[ATTR_ID,this.domId('labeltext'),'rows',3,'cols', 40]);
		let html =  HU.formTable();
		html += HU.formEntryTop('Label:',input);
		let prop = 'externalGraphic';
		let icon = this.lastIcon || tmpStyle.externalGraphic;
		let targetIconId = this.domId(prop+'_image');
		let icons = HU.image(icon,[ATTR_ID,targetIconId]) +
		    HU.hidden('',icon,[ATTR_ID,this.domId(prop)]);
		if(!Utils.isDefined(this.lastIncludeIcon)) this.lastIncludeIcon = true;
		html += HU.formEntry('',HU.checkbox(this.domId('includeicon'),[],this.lastIncludeIcon,'Include Icon')+' ' + icons);
		html+='</table>';
		html+=HU.div([ATTR_STYLE,HU.css('text-align','center','padding-bottom','8px','margin-bottom','8px','border-bottom','1px solid #ccc')],
			     HU.div([ATTR_CLASS,'ramadda-button-ok display-button'], 'OK') + SPACE2 +
			     HU.div([ATTR_CLASS,'ramadda-button-cancel display-button'], 'Cancel'));
		
		html+=HU.b('Select Icon');
		html+=HU.div([ATTR_ID,this.domId('recenticons')]);
		html+=HU.div([ATTR_ID,this.domId('icons'),'icon-property',prop]);
		html=HU.div([ATTR_STYLE,HU.css('margin','5px')],html);
		let dialog =  HU.makeDialog({content:html,title:'Marker',header:true,my:'left top',at:'left bottom',draggable:true,anchor:this.jq(ID_MENU_NEW)});

		let closeDialog = () =>{
		    dialog.remove();
		}

		this.initIconSelection(this.jq('icons'));
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
		this.handleNewRoute(cmd);
		return
	    }

	    this.showCommandMessage('New ' + glyphType.getName() +(glyphType.getNewHelp()?
								   ' - '+ glyphType.getNewHelp():''));
	    cmd.activate();	    

	},
	
	setOSMLabel:function(l,hide) {
	    this.jq(ID_OSM_LABEL).html(l);
	    this.jq(ID_OSM_LABEL).show();
	    if(hide) {
		setTimeout(() =>{
		    this.jq(ID_OSM_LABEL).fadeOut(500)
		},1500);
	    }
	},

	initOSMSearch:function() {
	    if(!this.osm) this.osm = {
		markers:[],
		ids:{}

	    }
	    if(this.osm.dialog) return;

	    let makeList = l=> {
		return l.map(a=>{return [a,Utils.makeLabel(a)];});
	    };


	    let places = makeList(['city', 'town', 'village', 'hamlet',  'island', 'suburb', 'locality']);
	    let tourism = makeList(['hotel','motel','guest_house','alpine_hut','apartment','artwork','attraction','camp_site','caravan_site','chalet','gallery','heritage','hostel','information','museum','picnic_site','theme_park','viewpoint','wilderness_hut','zoo']);
	    let shops = makeList(['supermarket', 'convenience', 'bakery', 'butcher',    'clothes', 'electronics', 'hardware', 'books',    'car', 'bicycle', 'sports', 'shoes' ]);
	    let amenities =makeList(['bar','bbq','bicycle_parking','bicycle_rental','biergarten','bus_station','cafe','car_rental','car_sharing','car_wash','charging_station','cinema','clinic','college','community_centre','courthouse','dentist','doctors','drinking_water','fast_food','ferry_terminal','fire_station','food_court','fountain','fuel','gym','hospital','ice_cream','kindergarten','library','marketplace','monastery','nightclub','nursing_home','park','parking','pharmacy','place_of_worship','police','post_box','post_office','prison','public_bath','pub','recycling','restaurant','school','shelter','taxi','theatre','toilets','townhall','university','veterinary','waste_basket','waste_disposal','water_point']);

	    let html = ''
	    html += HU.formTable();
	    let bbox = this.getMap().getBounds();

	    let fmt = v=>{
		return Utils.trimDecimals(v,1);
	    }
//	    html+=HU.formEntry('BBOX:','N: '+ fmt(bbox.top) +' W: '+fmt(bbox.left) +' S: ' + fmt(bbox.bottom) +
//			       ' E: '+  fmt(bbox.right));
	    html+=HU.formEntry('Text:',
			       HU.input('',this.osm.text??'',[ATTR_ID,this.domId(ID_OSM_TEXT)]));
	    let widgets = [];
	    let widget = (id,label,list) =>{
		widgets.push(HU.b(label+=':')+'<br>'+
			     HU.select('',[ATTR_STYLE,'min-width:200px;',ATTR_ID,this.domId('osm' + id),'multiple','true','size','3'],
				       list,null));
	    }
	    widget('tourism','Tourism',tourism);
	    widget('place','Place',places);
	    widget('shop','Shop',shops);
	    widget('amenity','Amenity',amenities);	    	    	    	    

	    html += HU.formTableClose();
	    html += HU.formTable();	    
	    for(let i=0;i<widgets.length;i+=2) {
		html+=HU.tr([],HU.tds([],[widgets[i],widgets[i+1]]));
	    }
	    html += HU.formTableClose();
	    html += HU.formTable();	    
	    html+=HU.formEntry('Limit:',
			       HU.input('',this.osm.limit??'100',[ATTR_ID,this.domId('osmlimit'),ATTR_SIZE,10]));	    
	    html += HU.formTableClose();	    
	    let buttons =Utils.join([HU.div([ATTR_CLASS,'ramadda-button-ok ramadda-button'], 'Search'),
				     HU.div([ATTR_CLASS,'ramadda-button-clear ramadda-button'], 'Clear Markers'),
				     HU.div([ATTR_CLASS,'ramadda-button-add ramadda-button',ATTR_TITLE,'Add markers as glyphs'], 'Add'),				     
				    HU.div([ATTR_CLASS,'ramadda-button-cancel ramadda-button'], 'Close')],
				    SPACE1);

	    html+=HU.vspace();
	    html+=HU.center(buttons);
	    html+=HU.div([ATTR_STYLE,HU.css('min-width','150px')],
			 "&nbsp;" +
			 HU.span([ATTR_ID,this.domId(ID_OSM_LABEL)],'&nbsp;'));
	    html = HU.div([ATTR_CLASS, 'ramadda-dialog'],html);
	    this.osm.dialog = HU.makeDialog({content:html,anchor:this.jq(ID_MENU_NEW),
					     callback:()=>{this.osm.dialog=null;},
					     draggable:true,title:'Open Street Map Query',header:true});

	    let _this = this;
	    this.jq(ID_OSM_TEXT).keydown(e=>{
		if(Utils.isReturnKey(e)) {
		    this.doOSMSearch();
		}
	    });
	    this.osm.dialog.find('.ramadda-button-cancel').button().click(function() {
		_this.osm.dialog.remove();
		_this.osm.dialog=null;
	    });
	    this.osm.dialog.find('.ramadda-button-add').button().click(()=> {
		this.addOSMMarkers();
	    });
	    this.osm.dialog.find('.ramadda-button-clear').button().click(() =>{
		this.clearOSMMarkers();
	    });
	    this.osm.dialog.find('.ramadda-button-ok').button().click(() =>{
		this.doOSMSearch();
	    });
	},

	doOSMSearch:function() {
	    let bbox = this.getMap().getBounds();
	    let bboxq ='(' + Utils.join([bbox.bottom,bbox.left,bbox.top,bbox.right],',') +')';
	    let query = '[out:json];(\n';
	    let clauses = [];
	    let add = key=>{
		let l=this.osm[key] = this.jq('osm' + key).val();
		if(typeof l=='string') {
		    l = [l];
		}
		l.forEach(v=>{
		    if(v!='---') {
			clauses.push('["' + key +'"="' + v+'"]');
		    }
		});	    
	    };
	    add('tourism');
	    add('place');
	    add('shop');
	    add('amenity');

	    let text = this.osm.text = this.jq(ID_OSM_TEXT).val();
	    if(Utils.stringDefined(text)) {
		clauses.push('["name"~"' + text+'", i]');
	    }

	    if(clauses.length==0) {
		alert('Please select a search criteria');
		return;
	    }
	    clauses.forEach(c=>{
		query+='node' +c+bboxq+';\n';
	    });

	    query +=');\nout body;';
	    console.log('osm query:',query);
	    this.setOSMLabel(HU.image(icon_progress) +'&nbsp;&nbsp;Searching...');
	    let url = "https://overpass-api.de/api/interpreter";
	    let callback = data => {
		if(!data.elements?.length) {
		    this.setOSMLabel('No locations found',true);
		    return;
		}
		let limit = this.osm.limit = (this.jq('osmlimit').val()??'500');
		this.handleOSMResults(data,limit);
		this.setOSMLabel('Found ' + data.elements.length +' locations',true);
	    };
	    let  error = error => {
		console.error("Error fetching data:", error)
		let msg = 'An error has occurred.';
		if(error.statusText) msg+=' Error:' + error.statusText;
		this.setOSMLabel(msg);
	    };
	    $.post(url,{data:query},callback).fail(error);
	},

	clearOSMMarkers: function() {
	    this.osm.markers.forEach(m=>{
		this.getMap().removeMarker(m.marker);
	    });
	    this.osm.markers=[];
	    this.osm.ids={};
	},
	addOSMMarkers:function() {
	    let group =    this.initGlyphCommand(this.groupGlyphType,null,{text:'OSM Group'});
	    this.osm.markers.forEach(m=>{
		let points = [m.latitude,m.longitude];
		let style = $.extend({},this.markerGlyphType.glyphStyle);
		style.label = m.name??'';
		style.popupText = m.text;
		let mapGlyph = this.createMapMarker(GLYPH_MARKER,{type:GLYPH_MARKER,name:m.name,legendText:m.text}, style,points,true);
		group.addChildGlyph(mapGlyph);
	    });
	    this.clearOSMMarkers();
	    this.makeLegend();

	},
	handleOSMResults: function(data,limit) {
	    limit = +limit;
//	    console.dir(data);
	    let cnt = 0;
	    let bounds = new RamaddaBounds();
	    data.elements.every((e,idx)=>{
		if(idx>=limit) return false;
		if(this.osm.ids[e.id]) return true;
		this.osm.ids[e.id] = true;
		let lonlat = MapUtils.createLonLat(e.lon,e.lat);
		let name =null;

		let popup = '<table>';
		if(e.tags) {
		    Object.keys(e.tags).forEach(k=>{
			let v = e.tags[k];
			if(k=='name') {
			    name = v;
			    return;
			}
			v  = String(v);
			if(v.startsWith('http')) {
			    v = HU.href(v,v);
			}
			let label  =k;
			if(label.startsWith('addr:')) label = label.substring('addr:'.length);
			label = Utils.makeLabel(label);
			popup+=HU.formEntry(label+':',v);
		    });
		}
		popup += '</table>';

		if(name) popup  =HU.center(HU.b(name)) + popup;
		let marker = this.getMap().addMarker('location', lonlat, null, '', popup, 20, 20);
		cnt++;
		bounds.expand(e.lat,e.lon);
		this.osm.markers.push({ latitude:e.lat,longitude:e.lon,
					marker:marker,name:name??'',
					text:popup,data:e});
		return true;
	    });
	    if(cnt>0) {
		this.getMap().setViewToBounds(bounds);
	    }
	},
	initStac:function(dialog) {
	    let load;
	    let stac= HU.div([ATTR_ID,this.domId('stac_top')]) +
		HU.div([ATTR_ID,this.domId('stac_output')]);
	    this.jq('stac_contents').html(stac);
	    let catalogUrl = Ramadda.getUrl('/resources/stac_catalogs.json');
	    let stacLinks = [{value:'',label:'Select'}, {value:catalogUrl,label:'Stac Catalogs'},
			     {value:Ramadda.getUrl('/resources/pc_catalog.json'),label:'Planet Earth Catalogs'}];
	    let seenStacUrls = {};
	    seenStacUrls[catalogUrl]=true;
	    let makeTop=(current)=>{ 
		let input = this.jq('stac_input').val()??'';
		let plus = HU.span([ATTR_ID,this.domId('stac_add'),ATTR_CLASS,CLASS_CLICKABLE,ATTR_TITLE,'Add a STAC catalog URL'],HU.getIconImage('fas fa-plus'));
		let back  = HU.span([ATTR_ID,this.domId('stac_back'),ATTR_CLASS,CLASS_CLICKABLE,ATTR_TITLE,'Go back'],HU.getIconImage('fas fa-rotate-left'));

		let top = back +' ' + plus+' '+HU.select("",[ATTR_STYLE,'max-width:500px;overflow:none;',ATTR_ID,this.domId('stac_url')],stacLinks,current,100);
		top = HU.div([ATTR_STYLE,'border-bottom:1px solid #ddd;padding-bottom:6px;margin-bottom:6px;'], top);
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
		    let link = HU.href('https://stacindex.org/catalogs',HU.getIconImage('fas fa-binoculars'),['target','_stacindex',ATTR_TITLE,'Look for catatalogs on stacindex.org']);
		    let input = HU.input('','',[ATTR_ID,_this.domId('stac_add_url'),ATTR_STYLE,'width:400px;'])+' ' +link;
		    let html = HU.b('STAC  Catalog URL: ') + input;
		    html+= HU.buttons([
			HU.div([ATTR_CLASS,'stac-add-ok display-button'], 'OK'),
			HU.div([ATTR_CLASS,'stac-add-cancel display-button'], 'Cancel')]);
		    html=HU.div([ATTR_STYLE,'margin:5px;margin-top:10px;'],html);	
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
	    let showStac=(data,baseUrl)=>{
		//is it the stac_catalogs.json
		if(Array.isArray(data)) {
		    data = {
			title:'Stac Catalogs',
			links:data
		    }
		}
		
		let html = '';
		let title = (data.title??baseUrl)+HU.space(1) + HU.href(baseUrl,HU.getIconImage('fas fa-link',[],[ATTR_STYLE,'font-size:9pt;']),['target','_stac']);
		html+=HU.center(HU.b(title));
		if(data.description) {
		    let desc = Utils.stripTags(data.description);
		    //		    console.log('BEFORE:'+desc);
		    desc = desc.replace(/[\n\s*\n]\n+/g,'\n').trim();
		    //		    console.log("AFTER:" +desc);
		    desc = desc.replace(/[\n\n]+/g,'\n').replace(/\n/g,'<br>');
		    html+=HU.div([ATTR_CLASS,'boxquote',ATTR_STYLE,'max-width:600px;overflow-z:auto;max-height:100px;overflow-y:auto;'],desc);
		}
		let linksHtml1 ='';
		let linksHtml2 ='';		
		if(data.links) {
		    let cnt = 0;
		    data.links.forEach(link=>{
			let url = link.href??link.url??link.link;
			if(!Utils.stringDefined(url) ||link.rel=='self') return;
			if(baseUrl.startsWith("http:")  || baseUrl.startsWith("https:")) 
			   url = new URL(url,baseUrl).href;
			let label = link.title;
			if(!label)
			    label  = url.replace(/.*\/([^\/]+$)/,"$1");
			let href = HU.href(url,HU.getIconImage('fas fa-link',[],[ATTR_TITLE,link.summary??'',ATTR_STYLE,'font-size:9pt;'])+' '+label+(link.rel?' ('+link.rel+')':''),['target','_stactarget',ATTR_CLASS,CLASS_CLICKABLE]);

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
			    linksHtml2+=HU.tr(HU.td([ATTR_WIDTH,'10%','nowrap','true'],'')+HU.td(href));
			} else {
			    linksHtml1+=HU.tr(HU.td([ATTR_WIDTH,'10%','nowrap','true'],
						    HU.div([ATTR_TITLE,url,ATTR_CLASS,'imdv-stac-item'],HU.span(['link',url,ATTR_CLASS,'imdv-stac-link'], 'Load'))) +
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
			//limit the number
			if(idx>200) return;
			let asset = data.assets[key];
			if(!Utils.stringDefined(asset.href)) return;
			if(table=='') {
			    table+=HU.b('Assets: ')+'note: the IMDV does not handle all projections. Try the thumbnail<br>';
			    table +='<table width=100%>';
			}

			let label = asset.name??asset.title??key;
			let assetUrl = new URL(asset.href,baseUrl).href;
			let link = HU.href(assetUrl,HU.getIconImage('fas fa-link',[],[ATTR_STYLE,'font-size:9pt;'])+' ' +label +' ('+ asset.type+')',[ATTR_TITLE,assetUrl,'target','_stactarget',ATTR_CLASS,CLASS_CLICKABLE]);
			let type= asset.type??asset.media_type;
			let isImage = type&& type.indexOf('image')>=0;

			if(asset.href.endsWith('ovr')) isImage= false;
			if(isImage) {
			    images+=HU.tr(HU.td([ATTR_WIDTH,'10%','nowrap','true'], HU.div([ATTR_CLASS,'imdv-stac-item'],HU.span(['asset-id',key,ATTR_CLASS,'imdv-stac-asset'], 'Add Image')))+
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
			html+=HU.div([ATTR_STYLE,'max-height:5em;overflow-y:auto;'], linksHtml);
		    else
			html+= linksHtml;		    
		}

		html+=assetsHtml;
		html=HU.div([ATTR_STYLE,'max-height:300px;overflow-y:auto;'], html);
		this.jq('stac_output').html(html);
		this.jq('stac_output').find('.imdv-stac-link').button().click(function() {
		    load($(this).attr('link'));
		});
		this.jq('stac_output').find('.imdv-stac-asset').button().click(function() {
		    let bbox=data.bbox;
		    if(!data.bbox) {
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
//		    console.log(bbox);
		    let asset =  data.assets[$(this).attr('asset-id')];
		    let url = new URL(asset.href,baseUrl).href;
		    if(asset.type&& asset.type.indexOf('image/tiff')>=0) {
			url =   Ramadda.getUrl('/tifftopng?url=' + encodeURIComponent(url));
		    }
		    console.log(url);
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
		    //JSON.parse(err.responseText)
		    console.dir(err);
		    this.jq('stac_output').html('Load failed. URL: ' + url);
		});
	    }
	},
	initDatacube:function(dialog) {
	    let _this = this;
	    let datacube= HU.div([ATTR_ID,this.domId('datacube_top')]) +
		HU.div([ATTR_ID,this.domId('datacube_output')]);
	    this.jq('datacube_contents').html(datacube);

	    let load;
	    let datacubeLinks = [{value:'',label:'Select'}];
	    MapUtils.getMapProperty('datacubeservers','').split(',').forEach(c=>{
		datacubeLinks.push(c);
	    });

	    let makeTop=(current)=>{ 
		let input = this.jq('datacube_input').val()??'';
		let plus = HU.span([ATTR_ID,this.domId('datacube_add'),ATTR_CLASS,CLASS_CLICKABLE,ATTR_TITLE,'Add a Data Cube server URL'],HU.getIconImage('fas fa-plus'));
		let top =plus +' ' +HU.select("",[ATTR_STYLE,'max-width:500px;overflow:none;',ATTR_ID,this.domId('datacube_url')],datacubeLinks,current,100);
		top = HU.div([ATTR_STYLE,'border-bottom:1px solid #ddd;padding-bottom:6px;margin-bottom:6px;'], top);
		this.jq('datacube_top').html(top);

		this.jq('datacube_add').click(function() {
		    let input = HU.input('','',[ATTR_ID,_this.domId('datacube_add_url'),ATTR_STYLE,'width:400px;']);
		    let html = HU.b('DATACUBE  Catalog URL: ') + input;
		    html+= HU.buttons([
			HU.div([ATTR_CLASS,'datacube-add-ok display-button'], 'OK'),
			HU.div([ATTR_CLASS,'datacube-add-cancel display-button'], 'Cancel')]);
		    html=HU.div([ATTR_STYLE,'margin:5px;margin-top:10px;'],html);
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
				       HU.select("",[ATTR_STYLE,selectStyle,ATTR_ID,selectId,'dataset',idx],items));
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
					       HU.select("",['ismap','true',ATTR_STYLE,selectStyle,ATTR_ID,selectId],maps));
			}
		    }
		});
		html+='</table>';
		html = HU.div([ATTR_STYLE,'auto;max-height:400px;overflow-y:auto;'], html);
		html+= HU.buttons([
		    HU.div([ATTR_CLASS,'ramadda-button-ok-datacube display-button'], 'OK'),
		    HU.div([ATTR_CLASS,'ramadda-button-cancel-datacube display-button'], 'Cancel')]);
		
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
	    let buttons = this.jq(ID_COMMANDS).find('.' + CLASS_CLICKABLE);
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
	    //init any buttons
	    dom.find('.ramadda-button').button();

	    dom.find('[' + ATTR_BUTTON_COMMAND +']').click(function(event) {
		event.preventDefault();
		let command  = $(this).attr(ATTR_BUTTON_COMMAND);
		let id  = $(this).attr(ID_GLYPH_ID);
		let mapGlyph   = _this.findGlyph(id);
		if(!mapGlyph) {
		    console.error('No map glyph from id:' + id);
		    return;
		}
		if(command=='toback') _this.changeOrder(false,mapGlyph);
		else if(command==PROP_LAYERS_STEP_SHOW) mapGlyph.toggleLayersVisibility(event);
		else if(command==PROP_LAYERS_ANIMATION_PLAY) mapGlyph.toggleLayersAnimation(event);
		else if(command=='tofront') _this.changeOrder(true,mapGlyph);		
		else if(command=='popup')   _this.handleMapGlyphClick(mapGlyph);
		else if(command=='edit')  _this.editFeatureProperties(mapGlyph,$(this));
		else if(command=="addisoline")  _this.addIsolineForMarker(mapGlyph);
		else if(command==ID_SELECT) {
		    if(mapGlyph.isSelected()) 
			_this.unselectGlyph(mapGlyph);
		    else
			_this.selectGlyph(mapGlyph);
		} else if(command==ID_DELETE) {
		    if(_this.dontAskDelete) {
			_this.removeMapGlyphs([mapGlyph]);
		    } else {
			let dontAsk = HU.checkbox(_this.domId('dontask'),[ATTR_ID,_this.domId('dontask'),ATTR_CLASS,''],false,'Don\'t ask again');
			HU.makeOkCancelDialog($(this),'Are you sure you want to delete this glyph?',
					      ()=>{
						  _this.dontAskDelete  = _this.jq('dontask').is(':checked');
						  _this.removeMapGlyphs([mapGlyph]);},null,dontAsk);
		    }
		}
	    });
	},
	
	makeGlyphButtons:function(mapGlyph,includeEdit,debug) {
	    let buttons = [];
	    let icon = i=>{
		return HU.getIconImage(i,[],BUTTON_IMAGE_ATTRS);
	    };
	    let showPopup =(mapGlyph.isEntry() ||  Utils.stringDefined(mapGlyph.getPopupText()));
	    if(showPopup) {
		buttons.push(HU.span([ATTR_CLASS,CLASS_CLICKABLE,TITLE,'Map Popup',ID_GLYPH_ID,mapGlyph.getId(),
				      ATTR_BUTTON_COMMAND,'popup'],
				     icon('fas fa-arrow-up-from-bracket')));
	    }
	    if(true||(this.canChange() && includeEdit)) {
		buttons.push(HU.span([ATTR_CLASS,CLASS_CLICKABLE,TITLE,'Settings',ID_GLYPH_ID,mapGlyph.getId(),
				      ATTR_BUTTON_COMMAND,'edit'],
				     icon('fas fa-cog')));
		buttons.push(
		    HU.span([ATTR_CLASS,CLASS_CLICKABLE,TITLE,'Select',ID_GLYPH_ID,mapGlyph.getId(),
			     ATTR_BUTTON_COMMAND,ID_SELECT],
			    icon('fas fa-hand-pointer')));
		buttons.push(HU.span([ATTR_CLASS,CLASS_CLICKABLE,TITLE,'Delete',ID_GLYPH_ID,mapGlyph.getId(),
				      ATTR_BUTTON_COMMAND,ID_DELETE],icon('fas fa-eraser')));

		if(mapGlyph.isMarker() && this.isIsolineEnabled()) {
		    buttons.push(HU.span([ATTR_CLASS,CLASS_CLICKABLE,TITLE,'Add Isoline',
					  ID_GLYPH_ID,mapGlyph.getId(),ATTR_BUTTON_COMMAND,"addisoline"],icon('fa-regular fa-circle-dot')));
		}
	    }
	    if(buttons.length==0) return '';
	    let attrs = [ATTR_STYLE,HU.css('margin-right','8px')];
	    let bar =  Utils.wrap(buttons,HU.open('span',attrs),'</span>');
	    return bar;
	},
	makeListItem:function(mapGlyph,idx) {
	    let style  = mapGlyph.getStyle()||{};
	    let line = "";
	    let type = mapGlyph.getType();
	    let select = HU.span([ATTR_TITLE,'Click to select',ATTR_STYLE,HU.css('padding-left','0px','padding-right','5px'), ID_GLYPH_ID,mapGlyph.getId(),
				  ATTR_CLASS,HU.classes(CLASS_CLICKABLE,'imdv-feature')], HU.getIconImage('fas fa-arrow-pointer'));
	    let visible = HU.checkbox('',[ATTR_STYLE,'margin-right:5px;',ATTR_TITLE,'Visible',ID_GLYPH_ID,mapGlyph.getId(),
					  ATTR_CLASS,HU.classes(CLASS_CLICKABLE,'imdv-feature-visible')],mapGlyph.getVisible());
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
	    line+= HU.td([ID_GLYPH_ID,mapGlyph.getId(),
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
		let skip = ['hereRoutingEnabled','googleRoutingEnabled','canEdit',ATTR_TITLE,'bounds','zoomLevel','mapCenter',ATTR_WIDTH,'height','user','entryIcon','entryId','thisEntryType','fileUrl','popupWidth','popupHeight','divid'];
		let userInput = "";
		for(key in attrs) {
		    if(skip.includes(key)) continue;
		    let value  = attrs[key];
		    userInput+=key+"=" + value+"\n";
		}
		let widget =  msg+HU.textarea("",userInput,[ATTR_ID,this.domId('displayattrs'),"rows",10,"cols", 60]);
		let andZoom = HU.checkbox(this.domId('andzoom'),[],true,"Zoom to display");
		let buttons  =HU.center(HU.div([ATTR_CLASS,'ramadda-button-ok display-button'], "OK") + SPACE2 +
					HU.div([ATTR_CLASS,'ramadda-button-cancel display-button'], "Cancel"));
		
		widget = HU.div([ATTR_STYLE,HU.css('margin','4px')], widget+"<br>"+andZoom + buttons);
		
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
		features+=HU.openTag("tr",[ATTR_CLASS,LIST_ROW_CLASS+" " + clazz,ATTR_VALIGN,'top',ATTR_STYLE,'border-bottom:1px solid #ccc',"glyphid",mapGlyph.getId()]);
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
	    html +=HU.div([ID,this.domId(ID_LIST_DELETE), ATTR_CLASS,'display-button'], 'Delete Selected');
	    html += SPACE2;	    
	    html +=HU.div([ID,this.domId(ID_LIST_CANCEL), ATTR_CLASS,'display-button'], 'Close');	    
	    html+='</center>';
	    html  = HU.div([ATTR_CLASS,'wiki-editor-popup'], html);
	    let dialog = this.listDialog  = HU.makeDialog({content:html,anchor:this.jq(ID_MENU_FILE),title:'Features',header:true,draggable:true,remove:false});
	    this.addFeatureList();
	    let _this  = this;
	    this.jq(ID_LIST_DELETE).button().click(()=>{
		let cut  = [];
		this.jq(ID_LIST).find('.imdv-feature-selected').each(function() {
		    let id  = $(this).attr(ID_GLYPH_ID);
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
	    let clipboard = this.clipboard;
	    this.unselectAll();
	    let newOnes = clipboard.map(mapGlyph=>{
		return  mapGlyph.clone();
	    });
	    for(let i=0;i<newOnes.length;i++) {
		newOnes[i].type = clipboard[i].type;
	    }
	    let h = this.map.getMap().getExtent().getHeight();
	    this.pasteCount++;
	    let delta = (this.pasteCount*0.05)*h;
	    newOnes.forEach(mapGlyph=>{
		mapGlyph.move(delta,-delta);
		mapGlyph.checkImage();
		if(mapGlyph.isMap()) {
		    mapGlyph.checkMapLayer();
		} else if(mapGlyph.isMapServer()) {
		    mapGlyph.checkMapServer();
		}
	    });
	    this.addGlyph(newOnes);
	    newOnes.forEach(mapGlyph=>{
		this.selectGlyph(mapGlyph);});
	},

	initSideHelp:function(dialog) {
	    dialog.find('.imdv-property').click(function(){
		let value = $(this).attr('value');
		if(!value) return;
		value = value.replace(/\\n/g,'\n');
		let target = $(this).attr('target');		
		let textComp = GuiUtils.getDomObject(target);
		if(textComp) {
		    WikiUtil.insertAtCursor('', textComp.obj, value);
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
		    help+=HU.div([ATTR_CLASS,HU.classes(CLASS_CLICKABLE,'imdv-property-popup'),'target',target,
				  'info-id',line.info], line.title);
		    return;
		}
		let attrs = [ATTR_CLASS,HU.classes(CLASS_CLICKABLE,'imdv-property'),'target',target];
		if(line.title) {
		    attrs.push(ATTR_TITLE,line.title);
		}
		let skip = line.skip;
		if(line.line) {
		    line = line.line;
		}
		if(skip) {
		    help+=HU.div([],line);
		    return;
		}
		attrs.push('value',props.prefix + line + props.suffix);
		help+=HU.div(attrs,line);
	    });
	    help = HU.div([ATTR_CLASS,'imdv-side-help',ATTR_STYLE,props.style??''], help);
	    return help;
	},
	getFeaturePropertyApply:function() {
	    return (mapGlyph, props)=>{
		this.featureChanged();	    
		let style = {};
		if(mapGlyph.isData()) {
		    let displayAttrs = this.parseDisplayAttrs(this.jq('displayattrs').val());
		}
		if(props) {
		    props.forEach(prop=>{
			//Remove the extra _cleared props added from a past bug
			if(prop.endsWith('_cleared')) return;
			let id = 'glyphedit_' + prop;
			if(prop.toLowerCase().indexOf('externalgraphic')>=0 || prop=='childIcon')  {
			    if(Utils.isTrue(this.jq(prop).attr('clearpressed'))) {
				style[prop+'_cleared'] = true;
			    } else {
				style[prop+'_cleared'] = false;
			    }
			    id =prop;
			}
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
		if(style.externalGraphic && !style.externalGraphic.startsWith('data:')) {
		    mapGlyph.attrs.icon=style.externalGraphic;
		}
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
		mapGlyph.applyPropertiesDialog(style);


//		mapGlyph.applyStyle(style);

		mapGlyph.makeLegend();
		mapGlyph.initLegend();
		this.showMapLegend();
		mapGlyph.redraw();
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
	    return  HU.div([ATTR_CLASS,'wiki-editor-popup'], html);
	},
	menuItem: function(id,label,cmd) {
	    if(cmd) {
		//		HU.image(icon_command,[ATTR_WIDTH,'12px']);
		let prefix = '';
		if(cmd!='Esc') prefix = 'Ctrl-';
		label = HU.leftRightTable(label,HU.div([ATTR_STYLE,'margin-left:8px;'], HU.span([ATTR_STYLE,'color:#ccc'], prefix+cmd)));
	    }
	    return  HU.div([ID,id,ATTR_CLASS,CLASS_CLICKABLE],label);
	},
	editFeatureProperties:function(mapGlyph,anchor) {
	    this.doProperties(mapGlyph.getStyle(), this.getFeaturePropertyApply(), mapGlyph,anchor);
	},


	makeColorBar:function(domId) {
	    let colors = Utils.split('transparent,#ff0000,#ffa500,#ffff00,#fffeec,#008000,#0000ff,#4b0082,#ee82ee,#ffffff,#000000,#cd5c5c,#f08080,#fa8072,#e9967a,#ffa07a,#dc143c,#ff0000,#b22222,#8b0000,#ffc0cb,#ffb6c1,#ff69b4,#ff1493,#c71585,#db7093,#ff7f50,#ff6347,#ff4500,#ff8c00,#ffa500,#ffd700,#ffff00,#ffffe0,#fffacd,#fafad2,#ffefd5,#ffe4b5,#ffdab9,#eee8aa,#f0e68c,#bdb76b,#e6e6fa,#d8bfd8,#dda0dd,#ee82ee,#da70d6,#ff00ff,#ff00ff,#ba55d3,#9370db,#663399,#8a2be2,#9400d3,#9932cc,#8b008b,#800080,#4b0082,#6a5acd,#483d8b,#7b68ee,#adff2f,#7fff00,#7cfc00,#00ff00,#32cd32,#98fb98,#90ee90,#00fa9a,#00ff7f,#3cb371,#2e8b57,#228b22,#006400,#9acd32,#6b8e23,#808000,#556b2f,#66cdaa,#8fbc8f,#20b2aa,#008b8b,#008080,#00ffff,#00ffff,#e0ffff,#afeeee,#7fffd4,#40e0d0,#48d1cc,#00ced1,#5f9ea0,#4682b4,#b0c4de,#b0e0e6,#add8e6,#87ceeb,#87cefa,#00bfff,#1e90ff,#6495ed,#7b68ee,#4169e1,#0000ff,#0000cd,#00008b,#000080,#191970,#fff8dc,#ffebcd,#ffe4c4,#ffdead,#f5deb3,#deb887,#d2b48c,#bc8f8f,#f4a460,#daa520,#b8860b,#cd853f,#d2691e,#8b4513,#a0522d,#a52a2a,#800000,#fffafa,#f0fff0,#f5fffa,#f0ffff,#f0f8ff,#f8f8ff,#f5f5f5,#fff5ee,#f5f5dc,#fdf5e6,#fffaf0,#fffff0,#faebd7,#faf0e6,#fff0f5,#ffe4e1,#dcdcdc,#d3d3d3,#c0c0c0,#a9a9a9,#808080,#696969,#778899,#708090,#2f4f4f',',');

	    let bar = "";
	    let cnt = 0;
	    colors.forEach(color=>{
		bar += HU.div([ATTR_TITLE,color,'color',color,'widget-id',domId,
			       ATTR_CLASS,HU.classes(CLASS_CLICKABLE,'ramadda-color-select','ramadda-dot'),
			       ATTR_STYLE,HU.css('background',color)]) +HU.space(1);
		cnt++;
		if(cnt>=10) {
		    cnt = 0;
		    bar+="<br>";
		}
	    });
	    bar = HU.div([ATTR_STYLE,'max-height:150px;overflow-y:auto;border:1px solid #ccc;'],bar);
	    return bar;
	},
	

	getFillPatternSelect:function(domId,v) {
	    let patterns = ['',...Object.keys(IMDV_PATTERNS).sort()];
	    let opts = patterns.map(p=>{
		if(p=='') return {value:'',label:'None'};
		return {value:p,label:p};
	    });
	    return  HU.select("",[ATTR_ID,domId],opts,v);
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
		if(mapGlyph) {
		    style = mapGlyph.getStyleForProperties(style);
		}
		props = [];
		let isImage = style.imageUrl;
		for(a in style) {
		    if(isImage) {
//			if(a!='transform' && a!='rotation' && a!="imageUrl" && a!="imageOpacity" && a!="popupText") continue;
		    }
		    props.push(a);
		    values[a] = style[a];
		}
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
	    if(mapGlyph && mapGlyph.isGroup() &&  !props.includes("popupText")) {
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
		transform:{label:'Image Transforms'}
	    };

	    props.forEach(prop=>{
		let shared = true;
		let id = "glyphedit_" + prop;
		let domId = this.domId(id);
		if(notProps.includes(prop)) return;
		let header = headers[prop];
		if(header) {
		    strip=header.strip;
		    html+=HU.tr([],HU.td(['colspan',2],HU.div([ATTR_CLASS,'imdv-form-header'],header.label)));
		}  else if(strip && !prop.startsWith(strip)) {
		    html+=HU.tr([],HU.td(['colspan',2],''));
		    strip = null;
		}
		let label = prop;
		if(strip) label = label.replace(strip,'');
		label =  Utils.makeLabel(label);		
		if(prop.endsWith('_cleared')) return;
		if(prop=="pointRadius") label = "Size";
		let widget;
		let extra ='';
		if(prop=='externalGraphic' || prop.indexOf('ExternalGraphic')>=0 || prop=='childIcon') {
//		    shared = false;
		    label="Marker"
		    if(prop=='childIcon') label = 'Child Entry Icon';
		    let options = "";
		    let graphic = values[prop];
		    if(!Utils.isDefined(graphic))
			graphic = this.getExternalGraphic();
		    domId = this.domId(prop);
		    let div = HU.div([ATTR_CLASS,'imdv-icons',
				ATTR_STYLE,'margin-left:5px;display:inline-block;',
				'icon-property',prop,
				      ATTR_ID,this.domId(prop+"_icons")],'Loading...');
		    widget = HU.hidden("",graphic,[ATTR_ID,domId]) +
			'<table><tr valign=top><td width=1%>' +
			HU.image(graphic,[ATTR_WIDTH,'24px',ATTR_ID,this.domId(prop+'_image')]) +
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
		    let rows = 1;
		    if(prop=="label") {
			size="80";
			if(mapGlyph.attrs.labelTemplate) {
			    v= mapGlyph.attrs.labelTemplate;
			}
			widget =  HU.textarea("",v,[ATTR_ID,domId,"rows",3,"cols", 60]);
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
				hdr+=HU.span([ATTR_CLASS,
					      HU.classes(CLASS_CLICKABLE,'ramadda-icons-recent'),
					      'icon',icon,'textarea',domId],
					     HU.getIconImage(icon,[ATTR_WIDTH,'16px']));
				hdr+=' ';
			    });
			    widget = HU.div([],hdr)+widget;
			}
		    } else if(prop=="strokeDashstyle") {
			widget = HU.select("",[ATTR_ID,domId],['solid','dot','dash','dashdot','longdash','longdashdot'],v);
		    } else if(prop=="strokeLinecap") {
			widget = HU.select("",[ATTR_ID,domId],['butt','round','square'],v);
		    } else if(prop=="fontWeight") {
			widget = HU.select("",[ATTR_ID,domId],["normal","bold","lighter","bolder","100","200","300","400","500","600","700","800","900"],v);
 		    } else if(prop=="fontStyle") {
			widget = HU.select("",[ATTR_ID,domId],["normal","italic"],v);			
		    } else if(prop=='textBackgroundShape') {
			widget = HU.select('',[ATTR_ID,domId],['rectangle','circle','ellipse'],v);
		    } else {
			if(prop == "pointRadius") {
			    label="Size";
			} 
			if(prop=="textBackgroundFillOpacity" || prop=="textBackgroundPadding" || prop=="strokeWidth" || prop=="pointRadius" || prop=="fontSize" || prop=="imageOpacity" || prop=='dotSize') size="4";
			else if(prop=="fontFamily") size="60";
			else if(prop.toLowerCase().indexOf('url')>=0) size="60";
			else if(prop=='imagecss') {
			    label = 'Image CSS';
			    size='60';
			    rows=5;
			    extra = '<pre>property=value...</pre>'
			} else if(prop=='imagefilter') {
			    label = 'Image Filter'
			    size='60';
			    extra='<br>e.g.:<pre>contrast(200%) grayscale(80%) brightness(0.4)\ninvert(75%) saturate(30%) sepia(60%)</pre>';
			    extra += HU.href('https://developer.mozilla.org/en-US/docs/Web/CSS/filter','Help',['target','_help']);
			} else if(prop=='clippath') {
			    label='Image Clip Path';
			    size='60';
			    extra = '<br>' + HU.span([ATTR_ID,'clippathdraw', ATTR_CLASS,CLASS_CLICKABLE,ATTR_TITLE,'Draw polygon'],HU.getIconImage('fas fa-draw-polygon')+' clip the image') +', e.g.,<pre>polygon(x1 y1,x2 y2,x3 y3,x4 y4)\n'+
				'e.g., to clip 5% from left, 18% from top and 10% from right do:\n'+
				'polygon(5% 18%,95% 18%,95% 90%,5% 90%)'+
				'</pre>';
			    extra += HU.href('https://developer.mozilla.org/en-US/docs/Web/CSS/clip-path','Help',['target','_help']);
			} else if(prop=='transform') {
			    label='Image Transform';
			    size='60';
			    extra = '<br>Apply CSS transform. e.g., to shrink the lower part of an image do:<pre>perspective(10px) rotateX(-0.05deg)</pre>';
			    extra += HU.href('https://developer.mozilla.org/en-US/docs/Web/CSS/transform','Help',['target','_help']);
			}
			if(prop.indexOf("Color")>=0) {
			    widget =  HU.input("",v,[ATTR_CLASS,'ramadda-imdv-color',ID,domId,"size",8]);
			    widget+=SPACE1;
			    widget +=  HU.input("",v,[ATTR_STYLE,HU.css('border-radius','var(--default-radius)'),
						      'type','color','baseid',domId,ATTR_CLASS,'ramadda-imdv-color-hidden',ID,domId+'colorinput',"size",8]);

/*
			    widget +=  HU.span([ATTR_TITLE,'Show color chooser',
						ATTR_CLASS,'ramadda-clickable ramadda-imdv-color-select','baseid',domId,
						ID,domId+'_select'],HU.getIconImage('fas fa-palette'));
						*/
			    
/*			    widget =  HU.div([ATTR_ID,domId+'_display',ATTR_CLASS,'ramadda-dot', ATTR_STYLE,HU.css('background',Utils.stringDefined(v)?v:'transparent')]) +
				HU.space(2)+widget;
			    //			    widget  = HU.table(['cellpadding','0','cellspacing','0'],HU.tr([ATTR_VALIGN,'top'],HU.tds([],[widget,bar])));
*/
			} else if(prop=="labelAlign") {
			    html +=HU.formTableClose();
			    html +=HU.formTable();			    
			    //lcr tmb
			    let items = [["lt","Left Top"],["ct","Center Top"],["rt","Right Top"],
					 ["lm","Left Middle"],["cm","Center Middle"],["rm","Right Middle"],
					 ["lb","Left Bottom"],["cb","Center Bottom"],["rb","Right Bottom"]];
			    widget =  HU.select("",[ATTR_ID,domId],items,v);			
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
			    widget =  HU.select("",[ATTR_ID,domId],opts,v);			
			} else if(prop=='fillPattern') {
			    widget = this.getFillPatternSelect(domId,v);
			} else if(prop.indexOf('Radius')>=0 ||
				  prop.indexOf("Width")>=0 ||
				  prop.indexOf("Padding")>=0 ||
				  prop.indexOf("Offset")>=0 ||
				  prop=="rotation") {
			    let isRotation = prop=="rotation";
			    let isOffset = prop.indexOf('Offset')>=0;
			    if(!Utils.isDefined(v)) {
				v=isRotation?0:1;
			    } else if(v==="") {
				v=isRotation?0:1;
			    }
			    let min  = isRotation?-360:(isOffset?-50:0);
			    let max = isRotation?360:50;
			    let size = 4;
			    if(prop.indexOf("Offset")>=0) size=8;
			    widget =  HU.input("",v,[ID,domId,"size",size])+HU.space(4) +
				
			    HU.div(['slider-min',min,'slider-max',max,'slider-step',1,'slider-value',v,'slider-id',domId,ID,domId+'_slider',ATTR_CLASS,'ramadda-slider',STYLE,HU.css("display","inline-block","width","200px")],"");

			} else if(prop.toLowerCase().indexOf("opacity")>=0) {
			    if(!v || v=="") v= 1;
			    widget =  HU.input("",v,[ID,domId,"size",4])+HU.space(4) +
				HU.div(['slider-min',0,'slider-max',1,'slider-value',v,'slider-id',domId,ID,domId+'_slider',ATTR_CLASS,'ramadda-slider',STYLE,HU.css("display","inline-block","width","200px")],"");
			} else  if (rows>1) {
			    widget =
				HU.textarea("",v,[ATTR_ID,this.domId(id),"rows",rows,"cols",size]);

			} else {
			    widget =  HU.input("",v,[ID,this.domId(id),"size",size]);
			}
		    }
		}
		let suffix='';
		if(isGroup && shared) {
		    suffix=HU.span([ATTR_TITLE,'Apply this style to children glyphs',
				    ATTR_CLASS,HU.classes(CLASS_CLICKABLE,'imdv-style-suffix'),
				    'widget-id',domId,'property',prop],HU.getIconImage('fas fa-folder-tree'));
		}
		if(label=='') label='Label';

		html+=HU.formEntry(label+":",widget+suffix+extra);
		html+='\n';
	    });
	    html+="</table>";
	    html = HU.div([ATTR_CLASS,'imdv-form',STYLE,HU.css("max-height","350px","overflow-y","scroll","margin-bottom","5px")], html);
	    return {props:props,html:html};
	},
	
	setLevelRangeTick:function() {
	    let current = this.getCurrentLevel();
	    let perc = 100*(current-this.minLevel)/(this.maxLevel-this.minLevel);
	    this.jq('level_range_tick').css('left',perc+'%');
	    this.jq('level_range_tick').attr(ATTR_TITLE,'Current level:' + current);
	},
	getLevelRangeWidget:function(level,showMarkerToo) {
	    if(!level) level={};
	    let visibleCbx =
		HU.checkbox(this.domId('showmarkerwhennotvisible'),[ATTR_ID,this.domId('showmarkerwhennotvisible')],
			    showMarkerToo,'Show marker instead');
	    let min = level.min??this.minLevel;
	    let max = level.max??this.maxLevel;	    
	    let current = this.getCurrentLevel();
	    let perc = 100*(current-this.minLevel)/(this.maxLevel-this.minLevel);
	    let width = '400px';
	    let widget =   HU.b("Visible between levels:") + HU.space(3) +visibleCbx +'<br>';
	    widget+=HU.hidden('',level.min??this.minLevel,
			      [ATTR_ID,this.domId(ID_LEVEL_RANGE_MIN)]) +
		HU.hidden('',level.max??this.maxLevel,[ATTR_ID,this.domId(ID_LEVEL_RANGE_MAX)])
	    widget+=HU.hidden('','',[ATTR_ID,this.domId(ID_LEVEL_RANGE_CHANGED)]);
	    let slider =
		HU.div([ATTR_ID,this.domId(ID_LEVEL_RANGE_SLIDER),ATTR_STYLE,HU.css('margin-bottom','110px','margin-top','10px','position','relative',ATTR_WIDTH,width)]);

	    let clear = HU.span([ATTR_TITLE,'Clear range values',
				 ATTR_STYLE,HU.css('margin-left','10px'),
				 ATTR_CLASS,'ramadda-clickable',
				 ATTR_ID,this.domId(ID_LEVEL_RANGE_CLEAR)],
				HU.getIconImage('fas fa-delete-left'));
	    slider= HU.hbox([slider,clear]);

	    let tick = HU.image(icon_downdart,
				[ATTR_ID,
				 this.domId('level_range_tick'),
				 ATTR_STYLE,HU.css('position','absolute','top','0px')]);
	    let sample1 = HU.image(RamaddaUtil.getCdnUrl('/map/zoom/zoom' + min+'.png'),
				   [ATTR_ID,this.domId(ID_LEVEL_RANGE_SAMPLE_MIN),
				    ATTR_STYLE,'position:absolute;left:0px;bottom:0px;',
				    ATTR_WIDTH,'120px']);
	    let sample2 = HU.image(RamaddaUtil.getCdnUrl('/map/zoom/zoom' + max+'.png'),
				   [ATTR_ID,this.domId(ID_LEVEL_RANGE_SAMPLE_MAX),
				    ATTR_STYLE,'position:absolute;right:0px;bottom:0px;',
				   ATTR_WIDTH,'120px']);	    
	    let container = HU.div([ATTR_STYLE,HU.css('display','inline-block','position','relative')],
				   slider +tick+sample1+sample2);


	    return widget+container;
	},
	doProperties: function(style, apply,mapGlyph,_anchor) {
	    let _this = this;
	    style = style ?? mapGlyph?mapGlyph.getStyle():style;
	    let props;
	    let buttons = "";
	    buttons+="<center>";
	    buttons +=HU.div([ATTR_CLASS,"display-button","command",ID_APPLY], "Apply");
	    buttons += SPACE2;
	    buttons +=HU.div([ATTR_CLASS,"display-button","command",ID_OK], "Ok");
	    buttons += SPACE2;
	    if(mapGlyph) {
		buttons +=HU.div([ATTR_CLASS,"display-button","command",ID_DELETE], "Delete");
		buttons += SPACE2;
	    }
	    buttons +=HU.div([ATTR_CLASS,"display-button","command",ID_CANCEL], "Cancel");	   
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
			items.push(HU.div([ATTR_CLASS,CLASS_CLICKABLE,'blockidx',idx], block.title));
		    });
		    menuBar = HU.div([],HU.b("Add:"))+HU.div([ATTR_ID,this.domId('displayattrsmenubar'),ATTR_STYLE,HU.css('margin-left','5px','max-height','200px','overflow-y','auto')], Utils.join(items,""));
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
	    }// else {
		let r =  this.makeStyleForm(style,mapGlyph);
		let div =
		    HU.checkbox(this.domId('styledialogactive'),[ATTR_ID,
								 this.domId('styledialogactive')], false,
				'Active') +r.html;
		content.push({header:"Style",contents:HU.div([ATTR_ID,this.domId('styledialog')],div)});
		props = r.props;
//	    }
	    if(mapGlyph) {
		mapGlyph.getPropertiesComponent(content);
	    }
	    let html = buttons;
	    let accord;
	    if(mapGlyph) {
		accord= HU.makeTabs(content,{contentsStyle:'min-height:400px;min-width:750px;'});
		html+=accord.contents;
	    } else {
		html+=HU.center(HU.b('Default Style')) + content[0].contents;
	    }
	    html+=buttons;
	    html  = HU.div([ATTR_STYLE,HU.css('min-width','700px','min-height','400px'),ATTR_CLASS,"wiki-editor-popup"], html);
	    this.map.ignoreKeyEvents = true;
	    let anchor = this.jq(ID_MENU_FILE);
	    if(anchor.length==0) {
		anchor = _anchor;
	    }
	    let dialog = HU.makeDialog({content:html,anchor:anchor,title:"Map Properties" + (mapGlyph?": "+mapGlyph.getName():""),header:true,draggable:true,resizable:true});
	    if(accord) 
		accord.init();
	    if(mapGlyph)
		mapGlyph.initSideHelp(dialog);
	    this.initSideHelp(dialog);


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
			if(prop == 'externalGraphic' || prop=='childIcon') {
			    value = this.jq(prop+'_image').val();
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
		//Do this in another thread because these dialogs are big and the remove call takes a little while
		setTimeout(()=>{dialog.remove();},0);
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

	    let ifApply = () =>{
		if(this.jq('styledialogactive').is(':checked')) {
		    myApply();
		}
	    };

	    this.jq('styledialog').find('select').change(function() {
		ifApply();
	    });
	    this.jq('styledialog').find('input').change(function(event) {
		if($(this).attr(ATTR_ID)!=_this.domId('styledialogactive')) {
		    ifApply();
		}
	    });
	    this.jq('styledialog').find('input').keypress(function(event) {
		if(event.keyCode == 13) {
		    ifApply();
		}
	    });

	    dialog.find('.imdv-style-suffix').click(function() {
		let prop = $(this).attr('property');
		let id = $(this).attr('widget-id');
		let value = jqid(id).val();
		if(!value) {
		    value = jqid(id+'_image').val();
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

	    let setLevelRange = (min,max)=>{		    
		this.jq(ID_LEVEL_RANGE_CHANGED).val('changed');
		this.jq(ID_LEVEL_RANGE_MIN).val(min);
		this.jq(ID_LEVEL_RANGE_MAX).val(max);		    
		this.jq(ID_LEVEL_RANGE_SAMPLE_MIN).attr('src',
							RamaddaUtil.getCdnUrl('/map/zoom/zoom' + min+'.png'));
		
		this.jq(ID_LEVEL_RANGE_SAMPLE_MAX).attr('src',
							RamaddaUtil.getCdnUrl('/map/zoom/zoom' + max+'.png'));
	    }
	    

	    this.jq(ID_LEVEL_RANGE_CLEAR).click(()=>{
		setLevelRange(this.minLevel,this.maxLevel);
		this.jq(ID_LEVEL_RANGE_CHANGED).val('cleared');
		this.jq(ID_LEVEL_RANGE_SLIDER).slider('values',[this.minLevel,this.maxLevel]);
	    });
	    this.jq(ID_LEVEL_RANGE_SLIDER).slider({
		range:true,
		min:this.minLevel,
		max:this.maxLevel,
		values:[
		    parseInt(this.jq(ID_LEVEL_RANGE_MIN).val()??this.minLevel),
		    parseInt(this.jq(ID_LEVEL_RANGE_MAX).val()??this.maxLevel)],		
		slide:(event,ui)=>{
		    let min = ui.values[0];
		    let max = ui.values[1];		    
		    setLevelRange(min,max);
		}

	    });

	    
	    this.jq('displayattrsmenubar').find('.' + CLASS_CLICKABLE).click(function() {
		let block = blocks[$(this).attr('blockidx')];
		let sub = Utils.join(block.items,"");
		sub = HU.div([ATTR_STYLE,'max-height:200px;overflow-y:auto;'], sub);
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
		dialog.find('#clippathdraw').button().click(function(){
		    let input = _this.jq('glyphedit_clippath');
		    let html = HU.div([ATTR_ID,_this.domId('clippath_container'),ATTR_STYLE,HU.css('position','relative','display','inline-block','background','#ccc;')],
				      HU.image(mapGlyph.style.imageUrl,[ATTR_TITLE,'Click to select point\nshift-click:use previous X\nmeta-click: use previous Y', ATTR_WIDTH,'600px',ATTR_CLASS,'theimage',ATTR_STYLE,HU.css('cursor','pointer','border','1px solid #ccc')]));
		    let buttons = HU.buttons([HU.div([ATTR_CLASS,'ramadda-button-ok display-button'], 'OK'),
					      HU.div([ATTR_CLASS,'ramadda-button-cancel display-button'], 'Cancel')]);

		    let tryIt = true;
		    html = buttons+
			HU.div([ATTR_CLASS,'ramadda-button-clear display-button'], 'Clear')+HU.space(1)+
			HU.span([ATTR_CLASS,CLASS_CLICKABLE,ATTR_ID,'pathtry',ATTR_STYLE,'width:4em'],
				'Try') +HU.space(1) +
HU.input('','',[ATTR_CLASS,'pathoutput','size','60',ATTR_STYLE,'margin-bottom:0.5em;']) +
			'<br>'+html;
		    html = HU.div([ATTR_STYLE,HU.css('margin','10px','text-align','center')],html);
		    let path='';
		    let dialog = HU.makeDialog({content:html,title:'Edit Clip Path',draggable:true,header:true,anchor:$(this),my:"left top",at:"left bottom"});
		    let output = dialog.find('.pathoutput');
		    let image = dialog.find('.theimage');
		    let canvas;
		    let ctx;
		    image.on('load', function() {
			let container = _this.jq('clippath_container');
			container.append(
			    HU.tag(TAG_CANVAS,[ATTR_ID,_this.domId('clippath_canvas'),
					     ATTR_WIDTH,image.width(),
					     ATTR_HEIGHT,image.height(),					     
					     ATTR_STYLE,HU.css('position','absolute','pointer-events','none','left','0px','top','0px',
							       'background','transparent')]));
			
			canvas = document.getElementById(_this.domId('clippath_canvas'));
			ctx = canvas.getContext("2d");
			ctx.strokeStyle= "blue";
			ctx.lineWidth=1;
		    });


		    let clear = () =>{
			if(!ctx) return;
			ctx.clearRect(0, 0, canvas.width, canvas.height);
		    };

		    let tryButton = dialog.find('#pathtry').button();
		    tryButton.click(function(){
			if(tryIt) {
			    image.css('clip-path',output.val());
			    $(this).html('Reset');
			} else {
			    image.css('clip-path','');
			    $(this).html('Try');
			}
			tryIt = !tryIt;
		    });
		    dialog.find('.ramadda-button-clear').button().click(()=>{
			path='';
			output.val('');
			image.css('clip-path','');
			tryButton.html('Try');
			tryIt=true;
			clear();
		    });
		    dialog.find('.ramadda-button-ok').button().click(()=>{
			let val = output.val();
			dialog.remove();
			input.val(val);
		    });
		    dialog.find('.ramadda-button-cancel').button().click(()=>{
			dialog.remove();
		    });		    

		    let w = image.width();
		    let h = image.height();		    
		    let lastX=0;
		    let lastY=0;		    
		    
		    image.mousedown(function(e){
			let offset = $(this).offset();
			let x=e.pageX - offset.left;
			let y  =e.pageY - offset.top;
			if(e.originalEvent.shiftKey) x = lastX;
			if(e.originalEvent.metaKey) y = lastY;			
			if(ctx && path!='') {
			    ctx.beginPath();
			    ctx.moveTo(lastX,lastY);
			    ctx.lineTo(x,y);			    
			    ctx.stroke();
			}


			lastX = x; lastY = y;
			if(path!='') path+=', ';
			let xp = parseInt(100*(x/w));
			let yp = parseInt(100*(y/h));			
			path+=xp+'% ' + yp +'%'; 
			output.val('polygon(' + path +')');
		    });
		});
	    }



	    let icons =dialog.find('.imdv-icons');
	    if(icons.length>0) {
		this.initIconSelection(icons);
	    }

	    this.setLevelRangeTick();
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
			ifApply();
		    }});
	    });

	    dialog.find('.ramadda-imdv-color-hidden').on('input',function() {
		let id = $(this).attr('baseid');
		let val = $(this).val();
		$("#"+ id).val(val);
		$("#"+ id+'_display').css('background',val);
		ifApply();
	    });
	    dialog.find('.ramadda-imdv-color-select').click(function() {
		let base = $(this).attr('baseid');
		jqid(base+'colorinput').click();
	    });
	    dialog.find('.ramadda-imdv-color').focus(function() {
		let id = $(this).attr(ATTR_ID);
		let bar = _this.makeColorBar(id);
		let dialog = HU.makeDialog({content:bar,header:false,anchor:$(this),my:"left top",at:"left bottom"});
		dialog.find('.ramadda-color-select').click(function(){
		    let c = $(this).attr('color');
		    let id = $(this).attr('widget-id');
		    jqid(id).val(c);
		    jqid(id+'_display').css('background',c);
		    jqid(id+'colorinput').val(c);
		    dialog.remove();
		    ifApply();
		});

	    });

	    dialog.find('.ramadda-imdv-color').change(function() {
		let c = $(this).val();
		let id = $(this).attr(ATTR_ID);
		$("#"+ id+'_display').css('background',c);
		ifApply();
	    });


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
		default:
		    alert('unknown command:' +command);
		}
	    });
	},
	initIconSelection:function(icons, callback) {
	    let _this = this;
	    icons.each(function() {
		_this.addIconSelection($(this),callback);
	    });
	},

	addIconSelection:function(icons, callback) {
	    let _this = this;
	    let used = this.getUsedMarkers();
	    let prop = icons.attr('icon-property');
	    let apply = (icon,clear)=>{
		this.jq(prop+'_image').attr(ATTR_SRC,icon);
		if(clear) {
		    this.jq(prop).attr('clearpressed',true);
		} else {
		    this.jq(prop).attr('clearpressed',false);
		}
		this.jq(prop).val(icon);			
		if(callback) callback(icon);
		if(Utils.stringDefined(icon)) {
		    Utils.copyToClipboard(icon);
		    console.log('icon:' + icon +' copied to clipboard');
		}
	    }

	    if(used.length>0) {
		let html = HU.b("Recent: ");
		used.forEach(icon=>{
		    html+=HU.span([ATTR_CLASS,HU.classes(CLASS_CLICKABLE,'ramadda-icons-recent'),
				   'icon',icon],
				  HU.getIconImage(icon,[ATTR_WIDTH,'24px']));
		});
		this.jq('recenticons').html(html);
		this.jq('recenticons').find('.ramadda-icons-recent').click(function(){
		    apply($(this).attr('icon'));
		});
	    }


	    //Note: the emojis have to come from this RAMADDA instead of the CDN
	    //because if we use the CDN icon in the data icon canvas we get an security error
	    HU.getEmojis(emojis=>{
		let prefix = HU.getUniqueId('icons_');
		let html = "";
		emojis.forEach(cat=>{
		    if(html!="") html+="</div>";
		    html+=HU.open('div',[ATTR_CLASS,'ramadda-imdv-image-category']);
		    html+=HU.div([ATTR_CLASS,'ramadda-imdv-image-category-label'],HU.b(cat.name));
		    cat.images.forEach(image=>{
			html+=HU.image(image.image,[ATTR_CLASS,HU.classes(CLASS_CLICKABLE,'ramadda-imdv-image'),
						    ATTR_WIDTH,'24px',ATTR_STYLE,'margin-right:4px;margin-bottom:2px;','loading','lazy',ATTR_TITLE,image.name]);
		    });
		});
		html+="</div>";
		html = HU.div([ATTR_STYLE,HU.css(ATTR_WIDTH,'400px','max-height','200px','overflow-y','auto')], html);
		html = HU.input("","",[ATTR_ID,prefix+'_search','placeholder','Search','size','30']) +' ' +
		    HU.span([ATTR_CLASS,HU.classes(CLASS_CLICKABLE,'ramadda-imdv-image-delete')],'Clear')+
		    HU.space(2) +
		    HU.span([ATTR_CLASS,HU.classes(CLASS_CLICKABLE,'ramadda-imdv-image-add')],'Set URL')+		    
		    '<br>'+
		    html;
		icons.html(html);
		icons.find('.ramadda-imdv-image-delete').button().click(()=>{
		    apply('',true);
		});
		icons.find('.ramadda-imdv-image-add').button().click(()=>{
		    let url = prompt("Image URL:");
		    if(!url) return;
		    apply(url);
		});		
		let images = icons.find('.ramadda-imdv-image');
		images.click(function() {
		    apply($(this).attr(ATTR_SRC));
		});
		let cats = icons.find('.ramadda-imdv-image-category');
		let search = (value) =>{
		    images.each(function() {
			let textOk = true;		
			if(value) {
			    textOk = false;
			    let html = $(this).attr(ATTR_TITLE).toLowerCase();
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
		jqid(prefix+'_search').keyup(function() {
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
//	    console.log(json);
	    let url = Ramadda.getUrl("/entry/setfile"); 
	    let formdata = new FormData();
	    formdata.append("entryid",this.getProperty("entryId"));
	    formdata.append("authtoken",this.getProperty("authToken"));	    
	    formdata.append("file",json);
	    let bounds = this.getFullBounds(true);
	    if(bounds) {
		formdata.append("bounds",bounds.top+"," + bounds.left+","+bounds.bottom+","+bounds.right);
	    }
	    $.ajax({
		url: url,
		cache:false,
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
	loadIMDVUrl:function(url, finish,parentGlyph) {
	    $.ajax({
                url: url,
		cache:false,
                dataType: 'text',
                success: (data) => {
		    if(data=="") data="[]";
		    let json = JSON.parse(data);
		    this.loadIMDVJson(json,this.map,parentGlyph);
		    if(finish) finish();
		}
	    }).fail(err=>{
		if(finish)
		    finish();
		this.handleError(err);
	    });		    
	},
	doImport: function() {
	    let callback = (entryId) =>{
		let url = Ramadda.getUrl("/entry/get?entryid=" + entryId);
		this.showProgress("Importing map...");
		let finish = ()=>{
		    this.display.clearMessage2();
		    this.getMap().clearAllProgress();
		    this.featureChanged(true);
		}
		this.loadIMDVUrl(url,finish);
	    };
	    let props = {title:'Select IMDV entry to import',
			 callback:callback,
			 'eventSourceId':this.domId(ID_MENU_FILE),
			 typeLabel:'IMDV Entries'};
	    this.selector = RamaddaUtils.selectCreate(null, HU.getUniqueId(""),"",false,'entryid',this.getProperty('entryId'),'geo_imdv',null,props);

	},
	

	doDownload: function() {
	    let json = this.makeJson();
	    Utils.makeDownloadFile("imdvmap.json",json);
	},
	makeJson: function() {
	    let _this = this;
	    let list =[];
            this.getGlyphs().forEach(mapGlyph=>{
		let json = mapGlyph.makeJson();
		list.push(json);
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

	makeBox: function(type) {
	    type = type??GLYPH_BOX;
	    let html = '';
	    let style = HU.css('margin','1px');
	    let r = this.dialogRectangle??{
	    };
	    html+=HU.center(HU.input('',r.n,[ATTR_STYLE, style,ATTR_ID,this.domId('_north'),'size','6','placeholder','North']));
	    html+=HU.input('',r.w,[ATTR_STYLE, style,ATTR_ID,this.domId('_west'),'size','6','placeholder','West']);
	    html+=HU.input('',r.e,[ATTR_STYLE, style,ATTR_ID,this.domId('_east'),'size','6','placeholder','East']);
	    html+=HU.center(HU.input('',r.s,[ATTR_STYLE, style,ATTR_ID,this.domId('_south'),'size','6','placeholder','South']));
	    
	    let buttons = HU.buttons([HU.div([ATTR_CLASS,'ramadda-button-ok display-button'], 'OK'),
				      HU.div([ATTR_CLASS,'ramadda-button-cancel display-button'], 'Cancel')]);
	    html+=buttons;
	    html = HU.div([ATTR_CLASS, 'ramadda-dialog'],html);
	    let dialog = HU.makeDialog({content:html,anchor:this.domId(ID_MENU_NEW),draggable:true,
					title:'Make box',header:true});
	    dialog.find('.ramadda-button-ok').button().click(()=>{
		let r = this.dialogRectangle = {
		    n:this.jq('_north').val(),
		    w:this.jq('_west').val(),		    		    
		    s:this.jq('_south').val(),		    
		    e:this.jq('_east').val(),		    
		};

		this.createBox(r.n,r.w,r.s,r.e,type);
		dialog.remove();
	    });
	    dialog.find('.ramadda-button-cancel').button().click(()=>{
		dialog.remove();
	    });	    
	},

	createBox: function(n,w,s,e,type,style) {
	    let props = {
		geometryType:'OpenLayers.Geometry.Polygon',
		points:[n,w,n,e,s,e,s,w]
	    }
	    let _style = Utils.clone(this.boxStyle);
	    if(style) {
		_style = Utils.clone(_style, style);
	    }
	    let attrs = {
		type:type,
	    }
	    let glyph = new MapGlyph(this,type??GLYPH_BOX,attrs,null,_style,true,props);
	    this.addGlyph(glyph);
	    glyph.panMapTo();
	},
	loadIMDVJson: function(mapJson,map,parentGlyph) {
	    let glyphs = mapJson.glyphs||[];
//	    this.createBox(40,-107,30,-100,{fillColor:'red'});
	    glyphs.forEach((jsonObject,idx)=>{
		let mapGlyph = this.makeGlyphFromJson(jsonObject);
		if(mapGlyph) {
		    if(parentGlyph) parentGlyph.addChildGlyph(mapGlyph);
		    else this.addGlyph(mapGlyph,true);
		}
	    });
	    if(this.getMapProperty('mapLegendPosition')) {
		this.createMapLegendWrapper();
	    }
	    this.editState = null;
	    this.clearFeatureChanged();
	    this.checkMapProperties();
	    this.makeMenuBar();
	    this.makeControls();
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
	    if(Utils.isDefined(mapOptions.showmultidata))  {
		mapOptions.showdataicons = mapOptions.showmultidata;
		delete mapOptions.showmultidata;
	    }

	    mapOptions.id = jsonObject.id;
	    let type = jsonObject.type||mapOptions.type;
	    //for backwards compatabity
	    if(type=='label') {
		mapOptions.type = type =GLYPH_MARKER;

	    }
	    let glyphType = this.getGlyphType(type);
	    if(!glyphType) {
		console.log("no type:" + type,jsonObject);
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
		console.dir("No points defined for glyph:", glyphType.type);
		console.dir(jsonObject);
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


	doMapProperties:function() {
	    if(!this.mapProperties)this.mapProperties={};
	    let accords = [];

	    let buttons = HU.buttons([HU.div([ATTR_CLASS,'ramadda-button-apply display-button'], 'Apply'),
				      HU.div([ATTR_CLASS,'ramadda-button-ok display-button'], 'OK'),
				      HU.div([ATTR_CLASS,'ramadda-button-cancel display-button'], 'Cancel')]);
	    let cbxs = [
		HU.checkbox(this.domId('showopacityslider'),[],
			    this.getMapProperty('showOpacitySlider',this.getShowOpacitySlider()),'Show Opacity Slider'),
		HU.checkbox(this.domId('showgraticules'),[],
			    this.getMapProperty('showGraticules',false),'Show Graticules'),
		HU.checkbox(this.domId('showoverviewmap'),[],
			    this.getMapProperty('showOverviewMap',false),'Show Overview Map'),		
		HU.checkbox(this.domId('showmouseposition'), [],
			    this.getMapProperty('showMousePosition',false),'Show Mouse Position'),
		HU.checkbox(this.domId('showaddress'), [],
			    this.getMapProperty('showAddress',false),'Show Address'),
		HU.checkbox(this.domId('usercanchange'),[ATTR_TITLE,'Non logged in users can add glyphs and change styles but can\'t save'],
			    this.getMapProperty('userCanChange',false),'Anon. users can change'),
		
	    ];
	    let basic = '';
	    basic+=Utils.join(cbxs,'<br>');

	    let right = HU.formTable();
	    right+=HU.formEntry('Legend position:',
				HU.select('',[ATTR_ID,this.domId('legendposition')],[{label:'Left',value:'left'},
										  {label:'Right',value:'right'},
										  {label:'In Map',value:'map'},
										  {label:'None',value:'none'}],
					  this.getMapProperty('legendPosition','left')));
	    right+=HU.formEntry('Legend width:',
				HU.input('',this.getMapProperty('legendWidth',''),[ATTR_ID,this.domId('legendwidth'),'size','4']));
	    
	    right+=HU.formTableClose();
	    right+=HU.checkbox(this.domId('showbasemapselect'), [],
			       this.getMapProperty('showBaseMapSelect'),'Show Base Map Select');


	    
	    basic=HU.table([],HU.tr([ATTR_VALIGN,'top'],HU.td([],basic) + HU.td([ATTR_WIDTH,'50%'], right)));
	    basic+='<p>';
	    basic+=this.getLevelRangeWidget(this.getMapProperty('visibleLevelRange'),
					    this.getMapProperty('showMarkerWhenNotVisible'));

	    accords.push({header:'Basic', contents:basic});
	    accords.push({header:'Header/Footer',
			  contents:
			  HU.b('Top Wiki Text:') +'<br>' +
			  HU.textarea('',this.getMapProperty('topWikiText',''),
				      [ATTR_ID,this.domId('topwikitext_input'),'rows','4','cols','80']) +"<br>" +
			  HU.b('Bottom Wiki Text:') +'<br>' +
			  HU.textarea('',this.getMapProperty('bottomWikiText',''),
				      [ATTR_ID,this.domId('bottomwikitext_input'),'rows','4','cols','80']) +'<br>'
			 });

	    let props = this.getMapProperty('otherProperties','');
	    let lines = ['legendLabel=Some label',
			 'showViewInLegend=true',
			 ...IMDV_PROPERTY_HINTS,
			 'dragPanEnabled=false',
			 'zoomPanEnabled=false',			 
			 'addCurrentLocationMarker=true',
			 'centerOnCurrentLocation=true',
			 'currentLocationUpdateTime=seconds',
			 'showAddress=true',
			 'graticuleStyle=strokeColor:#000,strokeWidth:1,strokeDashstyle:dot'];
	    let help = HU.b('Add property: ') + HU.span([ATTR_ID,this.domId('propsearch')]) +
		this.makeSideHelp(lines,this.domId('otherproperties_input'),{suffix:'\n'});
	    accords.push({header:'Other Properties',
			  contents:
			  HU.hbox([
			      HU.textarea('',props,[ATTR_ID,this.domId('otherproperties_input'),'rows','8','cols','40']),HU.space(2),help])
			 });
	    

	    //	    let accord = HU.makeAccordionHtml(accords);
	    let accord = HU.makeTabs(accords);	    
	    let html = buttons + accord.contents;
	    html  = HU.div([ATTR_STYLE,'min-width:700px;min-height:300px;margin:10px;'],html);
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
		this.setMapProperty('userCanChange', this.jq('usercanchange').is(':checked'),
				    'showOpacitySlider', this.jq('showopacityslider').is(':checked'),
				    'showGraticules',this.jq('showgraticules').is(':checked'),
				    'showOverviewMap',this.jq('showoverviewmap').is(':checked'),				    
				    'showMousePosition', this.jq('showmouseposition').is(':checked'),
				    'showAddress', this.jq('showaddress').is(':checked'),
				    'legendPosition',this.jq('legendposition').val(),
				    'legendWidth',this.jq('legendwidth').val(),
				    'showBaseMapSelect',this.jq('showbasemapselect').is(':checked'),
				    'topWikiText', this.jq('topwikitext_input').val(),
				    'bottomWikiText', this.jq('bottomwikitext_input').val(),
				    'otherProperties', this.jq('otherproperties_input').val());		

		this.propertyCache = {}
		this.parsedMapProperties = null;
		let min = this.jq("minlevel").val()?.trim();
		let max = this.jq("maxlevel").val()?.trim();
		if(min=="") min = null;
		if(max=="") max = null;	
		this.setMapProperty('visibleLevelRange', {min:min,max:max},
				    'showMarkerWhenNotVisible', this.jq('showmarkerwhennotvisible').is(':checked'));
		this.checkMapProperties();
		this.makeLegend();
		this.featureChanged(true);

	    };

	    HU.initPageSearch('.imdv-property',
			      this.domId('otherproperties_input'),null,true,
			      {target:'#'+this.domId('propsearch')});


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
	    this.getMap().setShowOverviewMap(this.getMapProperty('showOverviewMap'));
	    if(this.getMapProperty('showMousePosition'))
		this.getMap().initMousePositionReadout();
	    else
		this.getMap().destroyMousePositionReadout();		

	    this.map.setDragPanEnabled(this.getMapProperty('dragPanEnabled',true));
	    this.map.setZoomPanEnabled(this.getMapProperty('zoomPanEnabled',true));	    

	    this.checkCurrentLocation();
	},
	
	xcnt:0,
	geoOptions: {
	    enableHighAccuracy: true, 
	    maximumAge        : 30000, 
	    timeout           : 27000
	},

	checkCurrentLocation:function() {
	    if(this.currentLocationMarker) {
		this.getMap().removeMarker(this.currentLocationMarker);
		this.currentLocationMarker=null;
	    }
	    if(!this.getMapProperty('addCurrentLocationMarker',false)) {
		return;
	    }
            if (!navigator.geolocation) {
		console.log('no navigator.geolocation available');
		return;
	    }


            navigator.geolocation.getCurrentPosition(position=> {
		let lat = position.coords.latitude;
		let lon = position.coords.longitude;
		let lonlat = MapUtils.createLonLat(lon,lat);
		if(this.currentLocationMarker) {
		    this.getMap().removeMarker(this.currentLocationMarker);
		    this.currentLocationMarker=null;
		}

		let popup = '<center><h2>Current Location</h2></center>';

		if(this.isIsolineEnabled()) {
		    popup+=HU.onClick('ImdvUtils.getImdv(\'' + this.getId() +'\').addIsolineForCurrentMarker()',	    HU.getIconImage('fa-regular fa-circle-dot')+' ' +'Add Isoline');
		}
		this.currentLocationMarker =
		    this.getMap().addMarker('location', lonlat, null, '', popup, 20, 20);
		if(this.getMapProperty('centerOnCurrentLocation')) {
		    this.getMap().setCenter(lonlat);
		}
            },error=>{
		console.error(error);
	    },this.geoOptions);

	    //Add a timeout callback
	    if(this.checkCurrentLocationTimeout) clearTimeout(this.checkCurrentLocationTimeout);
	    if(Utils.isDefined(this.getMapProperty('currentLocationUpdateTime'))) {
		this.checkCurrentLocationTimeout = setTimeout(()=>{
		    this.checkCurrentLocation();
		},1000*parseFloat(this.getMapProperty('currentLocationUpdateTime',30)));
	    }		

	},
	checkOpacitySlider:function() {
	    let visible;
	    if(Utils.isDefined(this.getMapProperty('showOpacitySlider')))
		visible = this.getMapProperty('showOpacitySlider');
	    else
		visible = this.getShowOpacitySlider(false);
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
	    html+= this.menuItem(this.domId(ID_CMD_LIST),"List Features...");
	    html+= this.menuItem(this.domId(ID_CLEAR),"Clear Commands","Esc");
	    //	    html+= this.menuItem(this.domId(ID_REFRESH),"Refresh");	    
	    html+=div;
	    html+= this.menuItem(this.domId(ID_PROPERTIES),"Set Default Style...");
	    html+= this.menuItem(this.domId(ID_MAP_PROPERTIES),"Properties...");
	    html+=div;
	    html+= HU.href(Ramadda.getUrl('/userguide/imdv/index.html'),'Help',['target','_help']);
	    html  = this.makeMenu(html);
	    //	    console.log('creating file menu');
	    this.dialog = HU.makeDialog({content:html,anchor:button});
	    let clear = () =>{
		this.clearCommands();
		HU.hidePopupObject(null,true);
	    };

	    this.jq(ID_NAVIGATE).click(()=> {
		clear();
		this.setCommand(null);
	    });
	    this.jq(ID_MAP_PROPERTIES).click(()=>{
		clear();
		this.doMapProperties();
	    });
	    this.jq(ID_SAVE).click(()=>{
		clear();
		this.doSave();
	    });
	    this.jq(ID_SAVEAS).click(()=>{
		clear();
		this.doSaveAs();
	    });	    
	    this.jq(ID_DOWNLOAD).click(()=>{
		clear();
		this.doDownload();
	    });	    
	    this.jq(ID_IMPORT).click(()=>{
		clear();
		this.doImport();
	    });
	    this.jq(ID_PROPERTIES).click(()=>{
		clear();
		this.doProperties();
	    });
	    this.jq(ID_REFRESH).click(()=>{
		clear();
		ImdvUtils.scheduleRedraw(this.myLayer);
	    });
	    this.jq(ID_CLEAR).click(()=>{
		clear();
		this.unselectAll();
	    });	    
	    this.jq(ID_CMD_LIST).click(()=>{
		clear();
		this.listFeatures();
	    });	    
	},

	getZoomImage:function(level) {
	    return RamaddaUtil.getCdnUrl('/map/zoom/zoom' + level+'.png')
	},
	makeViewMenu:function(addLabel,suffix,makeHtml) {
	    let html = '';
	    if(!makeHtml) {
		makeHtml = (id,label)=>{
		    return this.menuItem(id,label);
		}
	    }
	    let lbl = (l,i) =>{
		if(!addLabel) 
		    return (HU.getIconImage(i));
		return (i?HU.getIconImage(i):HU.div([ATTR_STYLE,'display:inline-block;width:20px;']))+HU.space(1)+
		    HU.span([],l);
	    }

	    let make = (id,label,icon)=>{
		return HU.span([ATTR_TITLE,label], makeHtml(id,lbl(label,icon)));
	    }
	    suffix = suffix??'';
	    if(this.initialLocation) {
		html+= make(this.domId(ID_MAP_RESETMAPVIEW+suffix),"Initial View","fas fa-house");
	    }

	    html+= make(this.domId(ID_MAP_VIEWLAYERS+suffix),'Set View to All','fas fa-eye');

            if (navigator.geolocation) {
		html+= make(this.domId(ID_MAP_MYLOCATION+suffix),"Your Location","fas fa-street-view");
	    }

	    html+= make(this.domId(ID_MAP_REGIONS+suffix),"Regions","fas fa-map");
	    html+= make(this.domId(ID_MAP_CHOOSE+suffix),"Set Location/Zoom","fas fa-compass");	    
	    return html;
	},
	initViewMenu:function(suffix,anchor) {
	    let _this = this;
	    suffix = suffix??'';
	    let clear = () =>{
		this.clearCommands();
		HU.hidePopupObject(null,true);
	    };

	    this.jq(ID_MAP_CHOOSE+suffix).click(function(){
		clear();
		let html = HU.formTable();
		html+=HU.formEntry('Latitude:',HU.input('','',[ATTR_ID,_this.domId('choose_latitude')]));
		html+=HU.formEntry('Longitude:',HU.input('','',[ATTR_ID,_this.domId('choose_longitude')]));		

		let opts = [2,3,4,5,6,7,8,9,10,11,13,14,15,16,17,18].map(v=>{
		    return {label:v,value:v,
			    image:_this.getZoomImage(v)
			   };
		});
		let zoomPopup  = '';
		opts.forEach(level=>{
		    zoomPopup+=
			HU.div([ATTR_STYLE,HU.css('margin-bottom','2px')],
			       HU.image(level.image,
					['data-level',level.value,
					 ATTR_TITLE,'Level:' + level.value,
					 ATTR_WIDTH,'100px',ATTR_CLASS,'ramadda-clickable']));
		});
		zoomPopup=HU.div([ATTR_STYLE,'text-align:center;max-height:300px;overflow-y:auto;'], zoomPopup);
		html+= HU.hidden('',_this.getCurrentLevel(),
				 [ATTR_ID,_this.domId('choose_zoom_value')]);
		let zoomButton = HU.div([ATTR_ID,_this.domId('choose_zoom_button')],
					HU.image(_this.getZoomImage(_this.getCurrentLevel()),
						 [ATTR_TITLE,'Click to select level',
						  ATTR_ID,_this.domId('choose_zoom_image'),
					  
						  ATTR_WIDTH,'100px',ATTR_CLASS,'ramadda-clickable']));
		html+=HU.formEntry('Zoom Level:',zoomButton);
		html+=HU.formTableClose();
		let buttons =HU.div([ATTR_CLASS,'ramadda-button-apply display-button'], 'Apply') + SPACE2 +
		    HU.div([ATTR_CLASS,'ramadda-button-ok display-button'], 'OK') + SPACE2 +
		    HU.div([ATTR_CLASS,'ramadda-button-cancel display-button'], 'Cancel');	    
		html+=HU.center(buttons);
		html = HU.div([ATTR_CLASS, 'ramadda-dialog'],html);
		let dialog = HU.makeDialog({content:html,anchor:anchor??$(this),draggable:true,
					    title:'Set Location/Zoom',header:true});
		let zoomDialog;
		_this.jq('choose_zoom_button').click(function(){
		    if(zoomDialog) zoomDialog.remove();
		    zoomDialog =HU.makeDialog({content:zoomPopup,anchor:$(this),header:true,title:'&nbsp;&nbsp;Select Zoom Level'});
		    zoomDialog.find('.ramadda-clickable').click(function() {
			let zoom  = $(this).attr('data-level');
			_this.jq('choose_zoom_value').val(zoom);
			_this.jq('choose_zoom_image').attr('src',$(this).attr('src'));
			zoomDialog.remove();
			zoomDialog=null;
		    });
		});
		dialog.find('.display-button').button().click(function(){
		    if($(this).hasClass('ramadda-button-ok') || $(this).hasClass('ramadda-button-apply')) {
			let lat = _this.jq('choose_latitude').val().trim();
			let lon = _this.jq('choose_longitude').val().trim();			
			if(lat!='' && lon!='') {
			    let lonlat = MapUtils.createLonLat(lon,lat);
			    _this.getMap().setCenter(lonlat);
			}
			_this.getMap().setZoom(parseInt(_this.jq('choose_zoom_value').val()));
		    }
		    if($(this).hasClass('ramadda-button-apply'))  return;
		    if(zoomDialog) zoomDialog.remove();
		    dialog.remove();
		});
	    });
	    this.jq(ID_MAP_VIEWLAYERS+suffix).click(()=>{
		clear();
		this.zoomToLayers();
	    });

	    this.jq(ID_MAP_RESETMAPVIEW+suffix).click(()=>{
		clear();
		if(this.initialLocation?.zoomLevel>=0 && Utils.isDefined(this.initialLocation?.zoomLevel)) {
		    this.getMap().setZoom(this.initialLocation?.zoomLevel);
		}
		if(this.initialLocation?.bounds) {
		    this.map.getMap().setCenter(this.initialLocation?.bounds.getCenterLonLat());
		}
	    });
	    this.jq(ID_MAP_MYLOCATION+suffix).click(()=>{
		clear();
		navigator.geolocation.getCurrentPosition(position=> {
		    let lat = position.coords.latitude;
		    let lon = position.coords.longitude;
		    let lonlat = MapUtils.createLonLat(lon,lat);
		    this.getMap().setCenter(lonlat);
		    this.getMap().setZoom(8);
		},error=>{
		    console.error(error);
		},this.geoOptions);
	    });
	    this.jq(ID_MAP_REGIONS+suffix).click(function(){
		clear();
		_this.initRegionsSelector(anchor??$(this));
	    });
	},


	showViewMenu: function(button) {
	    let html ="";
	    let div = '<div class=ramadda-menu-divider></div>';
	    html+=this.makeViewMenu(true);
	    html  = this.makeMenu(html);
	    this.dialog = HU.makeDialog({content:html,anchor:button});
	    this.initViewMenu(null,this.jq(ID_MENU_VIEW));
	},

	getFullBounds:function(toLatLon) {
	    let bounds=null;
            this.getGlyphs().forEach((mapGlyph,idx)=>{
		bounds =  MapUtils.extendBounds(bounds,mapGlyph.getBounds());
	    });
	    if(bounds && toLatLon) {
		bounds = this.getMap().transformProjBounds(bounds);
	    }
	    return bounds;
	},

	zoomToLayers:function() {
	    let bounds=this.getFullBounds();
	    if(bounds)
		this.getMap().zoomToExtent(bounds);
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
	    return  HU.div([ATTR_STYLE,'margin-bottom:5px;margin-right:5px;display:inline-block;'+dim + cssStyle]);
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
		html+=HU.div([ATTR_CLASS,CLASS_CLICKABLE,ATTR_ID,id],item);
	    });
	    if(html!="") {
		html = HU.div([ATTR_STYLE,'padding:5px;border : var(--basic-border);  background-color : var(--color-mellow-yellow);'], html);
	    }
	    this.jq(ID_MESSAGE3).html(html);
	    this.jq(ID_MESSAGE3).find('.' + CLASS_CLICKABLE).click(function() {
		let id = $(this).attr(ATTR_ID);
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
	    let html ='<table><tr valign=top><td>';
	    let tmp = Utils.splitList(this.glyphTypes,this.glyphTypes.length/3);
	    this.glyphTypes.forEach(glyphType=>{
		if(glyphType.type=='map' || glyphType.type=='freehand') {
		    html+='<td>&nbsp;</td><td>';
		}
		if(!glyphType.handler) return;
		let icon = glyphType.options.icon||Ramadda.getUrl('/map/marker-blue.png');
		let label = HU.image(icon,[ATTR_WIDTH,'16']) +SPACE1 + glyphType.getName();
		if(glyphType.getTooltip())
		    label = HU.span([ATTR_TITLE,glyphType.getTooltip()],label);
		html+= this.menuItem(this.domId('menunew_' + glyphType.type),label+SPACE2);
	    });
	    html+='</td></tr></table>';
	    html  = this.makeMenu(html);
	    this.dialog = HU.makeDialog({content:html,anchor:button});
	    this.glyphTypes.forEach(g=>{
		this.jq('menunew_' + g.type).click(function(){
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
			    if(!tuple) return prev+ HU.div([ATTR_CLASS,'ramadda-menu-divider']);						
			    return prev + 	this.menuItem(this.domId(tuple[0]),tuple[1],tuple[2]);
			},'');
	    
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
	unselectAll:function(except) {
	    this.getGlyphs().forEach(mapGlyph=>{
		if(except == mapGlyph) return;
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
	    this.clearCommands();
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
	    url = url.replace(/\${root}/,ramaddaBaseUrl);
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
		this.showMessage('Unknown map type:' + opts.entryType,4000);
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
		cache:false,
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
				this.initialLocation = {
				    zoomLevel:zoomLevel,
				    bounds:bounds
				}
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

			//If there was a location in the URL then don't set the location here
			if(Utils.stringDefined(HU.getUrlArgument(ARG_ZOOMLEVEL))) {
			    zoomLevel=null;
			}
			if(Utils.stringDefined(HU.getUrlArgument(ARG_MAPCENTER))) {
			    bounds = null;
			}

			if(zoomLevel>=0 && Utils.isDefined(zoomLevel)) {
			    _this.getMap().setZoom(zoomLevel);
			}
			if(bounds) {
			    _this.map.getMap().setCenter(bounds.getCenterLonLat());
			}
			try {
			    this.loadIMDVJson(json,_this.map);
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
	    
	    let boxStyle = this.boxStyle =  Utils.clone(lineStyle,{
		fillColor:"transparent",
		fillOpacity:1.0,fillPattern:''});

	    let dotStyle = {dotSize:3,
			    dotStrokeColor:'blue',
			    dotStrokeWidth:1,			   
			    dotFillColor:'blue',
			    dotExternalGraphic:''};
	    

	    this.groupGlyphType =
		new GlyphType(this,GLYPH_GROUP,"Group",
			  Utils.clone(
			      {externalGraphic: externalGraphic,
			       label:'',
			       pointRadius:this.getPointRadius(10)},
			      {fillColor:'transparent',
			       fillOpacity:1,
			       pointRadius:6,
			       labelSelect:true},
			      lineStyle,
			      textStyle,
			      textBackgroundStyle,
			      {transform:'', clippath:'', imagefilter:'',imagecss:''}), 
			  MyEntryPoint,
			  {isGroup:true, tooltip:'Add group',			  
			   icon:Ramadda.getUrl("/icons/chart_organisation.png")});
	    this.markerGlyphType =
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
			      textStyle,textBackgroundStyle),
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
			   newHelp:'Click and drag to create a new line',
			   icon:Ramadda.getUrl("/icons/line.png")});		
	    new GlyphType(this,GLYPH_POLYLINE, "Polyline",
			  Utils.clone(lineStyle,dotStyle),
			  MyPath,
			  {
			      newHelp:'Click and drag to create a new polyline',
			      icon:Ramadda.getUrl("/icons/polyline.png")});
	    new GlyphType(this,GLYPH_POLYGON, "Polygon",
			  Utils.clone(lineStyle,
				      {fillColor:"transparent",
				       fillOpacity:1.0,
				       fillPattern:''}),
			  MyPolygon,
			  {
			      newHelp:'Click and drag to create a new polygon',
			      icon:Ramadda.getUrl("/icons/polygon.png")});

	    new GlyphType(this,GLYPH_FREEHAND,"Freehand",
			  Utils.clone(lineStyle),			  
			  MyPath,
			  {freehand:true,
			   newHelp:'Click and drag to create a freehand line',			   
			   icon:Ramadda.getUrl("/icons/freehand.png")});
	    new GlyphType(this,GLYPH_FREEHAND_CLOSED,"Closed",
			  Utils.clone(lineStyle,
				      {fillColor:"transparent",
				       fillOpacity:1.0,fillPattern:''}),
			  MyPolygon,
			  {freehand:true,
			   newHelp:'Click and drag to create a new closed freehand line',
			   icon:Ramadda.getUrl("/icons/freehandclosed.png")});

	    new GlyphType(this,GLYPH_BOX, "Box",
			  Utils.clone(this.boxStyle),
			  MyRegularPolygon,
			  {snapAngle:90,sides:4,irregular:true,
			   tooltip:'Create box; ctrl-b: to manually enter bounds',
			   newHelp:'Click and drag to create a new box',
			   icon:Ramadda.getUrl("/icons/rectangle.png")});
	    new GlyphType(this,GLYPH_CIRCLE, "Circle",
			  Utils.clone(this.boxStyle),
			  MyRegularPolygon,
			  {snapAngle:45,sides:40,
			   newHelp:'Click and drag to create a new circle',			   
			   icon:Ramadda.getUrl("/icons/ellipse.png")});

	    new GlyphType(this,GLYPH_TRIANGLE, "Triangle",
			  Utils.clone(lineStyle,{
			      fillColor:"transparent",
			      fillOpacity:1.0,fillPattern:''}),
			  MyRegularPolygon,
			  {snapAngle:10,sides:3,
			   newHelp:'Click and drag to create a new triangle',			   
			   icon:Ramadda.getUrl("/icons/triangle_blue.png")});				
	    new GlyphType(this,GLYPH_HEXAGON, "Hexagon",
			  Utils.clone(lineStyle,{
			      fillColor:"transparent",
			      fillOpacity:1.0,fillPattern:''}),
			  MyRegularPolygon,
			  {snapAngle:90,sides:6,
			   newHelp:'Click and drag to create a new hexagon',			   
			   icon:Ramadda.getUrl("/icons/hexagon_blue.png")});		
	    new GlyphType(this,GLYPH_RINGS,"Range Rings",
			  Utils.clone(lineStyle,
				      {fillColor:"transparent",
				      pointRadius:6,
				      rotation:0},
				      textStyle,
				      textBackgroundStyle),
			  MyPoint,
			  {
			      newHelp:'Click to create a new range ring',
			      icon:Ramadda.getUrl("/icons/rangerings.png")});



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

	    new GlyphType(this,GLYPH_ISOLINE, "Isoline",
			  Utils.clone({},lineStyle,{fillColor:'transparent',fillOpacity:1}),						   
			  null,{icon:Ramadda.getUrl("/icons/route.png")});

	    new GlyphType(this,GLYPH_IMAGE, "Image",
			  Utils.clone({},
				      {imageOpacity:this.getImageOpacity(1)},
				      lineStyle,
				      {rotation:0,
				       transform:'', clippath:'', imagefilter:'',imagecss:''}),
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
			      {externalGraphic: externalGraphic},
			      {childIcon:''},
			      {showLabels:true, pointRadius:12},
			      textStyle,textBackgroundStyle),
			  MyEntryPoint,
			  {tooltip:"Display children entries of selected entry",
			   isMultiEntry:true,
			   icon:Ramadda.getUrl("/icons/folder.png")});

	    new GlyphType(this,GLYPH_DATA,"Data",
 			  {externalGraphic: externalGraphic},
			  MyEntryPoint,
			  {isData:true,
			   tooltip:'Select a map data entry to display',
			   icon:Ramadda.getUrl("/icons/chart.png")});
	    new GlyphType(this,GLYPH_OSM_LOCATIONS,"Query OSM",
 			  {externalGraphic: externalGraphic},
			  MyEntryPoint,
			  {isOsm:true,
			   tooltip:'Query Open Stree Map for locations',
			   icon:Ramadda.getUrl("/icons/osm.png")});	    

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
		msg = HU.div([ATTR_CLASS,'imdv-message-inner'],msg);
	    else if(this.messageIsTimed) return;
	    this.jq(ID_MESSAGE).html(msg);
	    this.jq(ID_MESSAGE).show();
	    if(this.messageErase) clearTimeout(this.messageErase);
	    this.messageErase = null;
	    if(clearTime>0 || !Utils.isDefined(clearTime)) {
		this.messageIsTimed = true;
		this.messageErase = setTimeout(()=>{
		    this.messageIsTimed = false;
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
	    //8 second time
	    this.showMessage(msg,8000);
	},
	getCurrentLevel: function() {
	    return this.getMap().getZoom();
	},
	checkVisible:function() {
	    this.getGlyphs().forEach(mapGlyph=>{
		mapGlyph.checkVisible();
	    });
	},
	initMap: function(map) {
	    SUPER.initMap.call(this)
	},
	initMapParams:function(params) {
	    SUPER.initMapParams.call(this,params);
	},
	
	getOtherProperties: function() {
	    if(!this.parsedMapProperties) {
		this.parsedMapProperties = Utils.parseMap(this.mapProperties.otherProperties,"\n","=")??{};
	    }
	    return this.parsedMapProperties;
	},

	setMapProperty:function() {
//	    console.log("setMapProperty");
	    for(let i=0;i<arguments.length;i+=2) {
//		console.log("\t" +arguments[i]+'='+arguments[i+1]);
		this.mapProperties[arguments[i]]=arguments[i+1];
	    }
	},
	propertyCache:{
	},
	getMapProperty: function(name,dflt,debug) {
	    if(debug)
		console.log("getProperty:" + name);

	    let value=  this.propertyCache[name];
	    if(Utils.isDefined(value)) {
		if(debug)
		    console.log('\tfrom cache:'+ value);
		return value;
	    }
	

	    //The wiki tag property overrides the map properties
	    value = this.propertyCache[name] = this.getProperty(name,null);
	    if(Utils.isDefined(value)) {
		if(debug)
		    console.log('\tfrom display property:'+ value);
		return Utils.getProperty(value);
	    }

	    value = this.getOtherProperties()[name];
	    if(debug) console.log('\tfrom other properties:', value);
	    if(!Utils.isDefined(value)) {
		value = this.mapProperties[name];
		if(debug) console.log('\tfrom map properties:', value);
	    }

	    if(!Utils.isDefined(value)) {
		value = dflt;
		if(debug) console.log('\tusing dflt:', value);
	    }


	    if(Utils.isDefined(value)) {
		value = Utils.getProperty(value);
	    }
		    
	    return value;
	},
	makeLegendDroppable:function(droppedOnGlyph,label,notify) {
	    notify = notify?? (()=>{this.setLastDroppedTime(new Date());});
	    label.droppable( {
		hoverClass: CLASS_LEGEND_ITEM_DROPPABLE,
		accept:'.' + CLASS_LEGEND_ITEM,
		tolerance:'pointer',
		drop: (event,ui)=>{
		    notify();
		    let draggedGlyph = this.findGlyph(ui.draggable.attr(ID_GLYPH_ID));
		    if(!draggedGlyph) {
			console.log('Could not find dragged glyph');
			return;
		    }
		    this.handleDroppedGlyph(draggedGlyph,droppedOnGlyph,label);
		}
	    });
	},
	handleDroppedGlyph:function(draggedGlyph,targetGlyph,target) {
	    let debug = false;
	    if(this.handleDropTimeout) {
		if(debug)	    console.log('clearing pending drop');
		clearTimeout(this.handleDropTimeout);
	    }
	    this.handleDropTimeout = setTimeout(()=>{
		this.handleDropTimeout = null;
		this.removeMapGlyph(draggedGlyph);
		draggedGlyph.setParentGlyph(null);
		if(targetGlyph) {
		    if(debug)	    console.log('handleDrop: target:' + targetGlyph.getName());
		    if(targetGlyph.isGroup()) {
			if(debug)		console.log('landed on group',targetGlyph.getName());
			targetGlyph.addChildGlyph(draggedGlyph);
			draggedGlyph.changeOrder(false);
		    } else {
			if(targetGlyph.getParentGlyph() && targetGlyph.getParentGlyph().isGroup()) {
			    if(debug) console.log('landed on glyph in a group');
			    targetGlyph.getParentGlyph().addChildGlyph(draggedGlyph);
			    this.moveGlyphBefore(targetGlyph, 
						 draggedGlyph,
						 targetGlyph.getParentGlyph().getChildren());
			} else {
			    if(debug) console.log('moving before:' + targetGlyph.getName());
			    this.moveGlyphBefore(targetGlyph, draggedGlyph);
			}
		    }
		} else if(target) {
		    draggedGlyph.setParentGlyph(null);
		    Utils.removeItem(this.getGlyphs(),draggedGlyph);
		    if(target.attr(ATTR_ID)==this.domId(ID_DROP_BEGINNING)) {
			if(debug) console.log('dropped on beginning');
			this.getGlyphs().unshift(draggedGlyph);
		    } else  {
			if(debug) console.log('dropped on end');
			this.getGlyphs().push(draggedGlyph);
		    }
		    draggedGlyph.changeOrder(false);
		    this.featureChanged();
		    this.redraw();
		}
		draggedGlyph.glyphHasBeenDropped();
		this.handleGlyphsChanged();
		this.redraw();
	    },1);
	},

	getShowLegendInMap() {
	    return  this.getMapProperty('legendPosition','left')=='map';
	},
	checkGlyphLayers:function() {
	    let baseIndex = 100;
	    this.getGlyphs().forEach((mapGlyph,idx)=>{
		baseIndex = mapGlyph.setLayerLevel(baseIndex);
	    });
	    this.getMap().checkLayerOrder();
	},
	makeLegend: function() {
	    let _this = this;
	    let legendDiv = this.getMapProperty("legendDivId");
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
	    let showViewInLegend= this.getMapProperty("showViewInLegend",false);
	    let idToGlyph={};
	    let glyphs = this.getGlyphs();
	    let html = '';
	    if(this.getMapProperty('showBaseMapSelect')) {
		html+=HU.div([ATTR_STYLE,'margin-bottom:4px;',ATTR_CLASS,CLASS_LEGEND_OFFSET], HU.b('Base Map: ') +this.getBaseLayersSelect());
	    }

	    if(this.getMapProperty('showAddress',false)) {
		this.jq(ID_ADDRESS).show();
	    } else {
		this.jq(ID_ADDRESS).hide();
	    }
	    this.checkGlyphLayers();



	    this.inMapLegend='';
	    if(glyphs.length)
		html+=HU.div([ATTR_ID,this.domId(ID_DROP_BEGINNING),ATTR_STYLE,'width:100%;height:1px;'],'');
	    glyphs.forEach((mapGlyph,idx)=>{
		html+=mapGlyph.makeLegend({idToGlyph:idToGlyph});
	    });
	    if(glyphs.length)
		html+=HU.div([ATTR_ID,this.domId(ID_DROP_END),ATTR_CLASS,CLASS_LEGEND_ITEM,ATTR_STYLE,'width:100%;height:1em;'],'');

	    let top = '';
	    if(Utils.stringDefined(legendLabel)) {
		legendLabel=legendLabel.replace(/\\n/,'<br>');
		top = HU.div([],legendLabel);
	    }
	    if(this.getMapProperty(PROP_SHOW_LAYER_SELECT_IN_LEGEND,false)) {
		top+=HU.div([ATTR_STYLE,'text-align:center;margin-bottom:2px;'],
			    this.getBaseLayersSelect());
	    }
	    if(showViewInLegend) {
		top +=HU.center(this.makeViewMenu(false,'_legend',(id,lbl)=>{return HU.span([ATTR_STYLE,'margin-right:6px;',ATTR_ID,id,ATTR_CLASS,CLASS_CLICKABLE], lbl);}));
	    }
	    html=top+html;

	    let inMap  =(legendPosition=='map');
	    if(html!="") {
		let height= this.getProperty('height');
		let legendHeight= this.getProperty('legendHeight',height);		
		let css  = HU.css('max-width',HU.getDimension(legendWidth),ATTR_WIDTH,HU.getDimension(legendWidth));
		if(height && !inMap && !legendDiv) css+=HU.css('height',legendHeight);
		if(!legendDiv) {
		    let attrs = [ATTR_CLASS,'imdv-legend',ATTR_STYLE,css]
		    html  = HU.div(attrs,html);
		}
	    }

	    if(inMap) {
		this.inMapLegend = html;
	    }
	    this.jq(ID_LEGEND_MAP_WRAPPER).html('');
	    if(this.inMapLegend!='' || inMap) {
		let inMapLegend=HU.div([ATTR_ID,this.domId(ID_LEGEND_MAP)],this.inMapLegend);
		let toggleResult = {};
		let toggleListener = (id,vis)=>{
		    this.setMapProperty('mapLegendOpen',vis);
		};

		inMapLegend=HU.toggleBlock('Legend' + HU.space(2),inMapLegend,
					   this.getMapProperty('mapLegendOpen',true),
					   {animated:300,listener:toggleListener},toggleResult);
		if(inMap) {
		    inMapLegend = HU.div([ATTR_ID,this.domId(ID_LEGEND)], inMapLegend);
		}
		this.jq(ID_LEGEND_MAP_WRAPPER).html(inMapLegend);
		this.mapLegendToggleId = toggleResult.id;
	    } 


	    let legendContainer;
	    if(legendDiv) {
		legendContainer = $('#'+legendDiv);
	    } else if(legendPosition=='left') {
		legendContainer=leftLegend;
	    } else   if(legendPosition=='right') {
		legendContainer=rightLegend;
	    } else   if(legendPosition=='map') {
		legendContainer = this.jq(ID_LEGEND_MAP_WRAPPER);
	    } else {
	    }

	    if(legendContainer && !inMap) {
		legendContainer.show();
		legendContainer.html(HU.div([ATTR_ID,this.domId(ID_LEGEND)],''));
		this.jq(ID_LEGEND).html(html);
	    }

	    this.initViewMenu('_legend');

	    this.makeLegendDroppable(null,this.jq(ID_DROP_BEGINNING),null);
	    this.makeLegendDroppable(null,this.jq(ID_DROP_END),null);


	    HU.initToggleBlock(this.jq(ID_LEGEND),(id,visible,element)=>{
		let mapGlyph = idToGlyph[element.attr('map-glyph-id')];
		if(mapGlyph) mapGlyph.setLegendVisible(visible);
	    });


	    this.getContainer().find('.imdv-legend-item-edit').click(function(event) {
		event.stopPropagation();
		let id = $(this).attr(ID_GLYPH_ID);
		let mapGlyph = _this.findGlyph(id);
		if(!mapGlyph) return;
		_this.editFeatureProperties(mapGlyph);
	    });

	    this.getContainer().find('.' + CLASS_LEGEND_ITEM_VIEW).click(function(event) {
		event.stopPropagation();
		let id = $(this).attr(ID_GLYPH_ID);
		let mapGlyph = _this.findGlyph(id);
		if(!mapGlyph) return;
		mapGlyph.panMapTo(event.shiftKey);
	    });

	    this.initBaseLayersSelect();
	    this.getGlyphs().forEach((mapGlyph,idx)=>{
		mapGlyph.initLegend();
	    });
	    this.initGlyphButtons(this.getContainer());
	    let items = this.getContainer().find('.' + CLASS_LEGEND_LABEL);
	    items.tooltip({
		content: function () {
		    let title =  $(this).prop(ATTR_TITLE);
		    title=HU.div([ATTR_STYLE,'font-size:10pt;'],title);
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
		let id = $(this).attr(ID_GLYPH_ID);
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
	editState:null,
	canChange: function() {
	    //Check if the user can edit the IMDV entry 
	    if(this.canEdit()) return true;
	    if(!this.editState) {
		let canchange = this.getUserCanChange(null);
		if(!Utils.isDefined(canchange)) {
		    canchange = this.getMapProperty('userCanChange');
		}
		this.editState = {
		    canchange:canchange
		}
	    }
	    return this.editState.canchange;
	},

        initDisplay: function(embedded) {
	    let _this = this;
	    SUPER.initDisplay.call(this)
	    this.myLayer = this.map.createFeatureLayer('IMDV Features',true,null,{rendererOptions: {zIndexing: true}});
	    //For now don't have a separate selection layer?
	    //	    this.selectionLayer = this.map.createFeatureLayer('Selection',false,null,{rendererOptions: {zIndexing: true}});	    
	    this.selectionLayer = this.myLayer;
	    this.selectionLayer.setZIndex(1001)
	    this.myLayer.setZIndex(1000)	    
//	    this.selectionLayer.canSelect = false;
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
		    Utils.bufferedCall(this.getId()+'_checkvisible', ()=>{this.checkVisible();});
		    this.setLevelRangeTick();
		    $('.imdv-currentlevellabel').html('(current level: ' + this.getCurrentLevel()+')');
		},true);
		this.getMap().getMap().events.register('moveend', '', () =>{
		    Utils.bufferedCall(this.getId()+'_checkvisible', ()=>{this.checkVisible();});
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
		return this.handleMapGlyphClick(mapGlyph,e.event? e.event.xy:null,e);
	    };

	    /*
	      Don't do this for now since if we have set the Show legend property=false that isn't set yet
	      legend = HU.toggleBlock('',legend,true,{orientation:'horizontal',
	      imgopen:'fa-solid fa-angles-down',
	      imgclosed:'fa-solid fa-angles-right',						    
	      });
	    */

	    this.createMapLegendWrapper();

	    let legendLeft = HU.div([ATTR_ID,this.domId(ID_LEGEND_LEFT),ATTR_STYLE,'display:none']);
	    this.jq(ID_LEFT).html(legendLeft);
	    let legendRight = HU.div([ATTR_ID,this.domId(ID_LEGEND_RIGHT),ATTR_STYLE,'display:none']);
	    this.jq(ID_RIGHT).html(legendRight);	    

	    this.jq(ID_HEADER0).append(HU.div([ID,this.domId('topwikitext')]));
	    this.jq(ID_BOTTOM).append(HU.div([ID,this.domId('bottomwikitext')]));	    
	    let message2 = HU.div([ID,this.domId(ID_MESSAGE2),ATTR_CLASS,'ramadda-imdv-message2'],'');
	    this.jq(ID_MAP_CONTAINER).append(message2);
	    let message3 = HU.div([ID,this.domId(ID_MESSAGE3),ATTR_CLASS,'ramadda-imdv-message3'],'');
	    if(this.getShowMapLegend()) {
		this.jq(ID_MAP_CONTAINER).append(message3);
	    }

	    let labels = HU.div([ID,this.domId(ID_GLYPH_LABELS),ATTR_CLASS,'imdv-inmap-labels'],'');
	    this.jq(ID_MAP_CONTAINER).append(labels);



	    this.makeMenuBar();
	    this.makeControls();

	    $(window).bind('beforeunload', ()=>{
		if(this.canEdit() && this.featureHasBeenChanged) {
		    return 'Changes have been made. Are you sure you want to leave?';
		}
	    });


	    let cmds = '';
	    this.jq(ID_COMMANDS).html(cmds);
	    this.jq(ID_MAP).keydown(function(event){
		if(!event.ctrlKey) return;
		_this.getSelected().forEach(glyph=>{
		    glyph.handleKeyDown(event);
		});
		event.preventDefault();
	    });
	    this.jq(ID_MAP).mouseover(function(){
		$(this).focus();
	    });

	    if(this.getProperty('thisEntryType')=='geo_editable_json' || this.getProperty('thisEntryType')=='geo_imdv') {
		this.loadMap();
	    }
	},

	handleMapGlyphClick:function(mapGlyph,xy,event) {	
	    if(mapGlyph==null) return false;
	    let debug = false;
	    if(mapGlyph.isMap()) {
		if(event && event.event && event.feature && event.event.altKey) {
//		    return false;
		}
 		if(debug)console.log('\tis map');
		return true;
	    }
	    if(this.command==ID_EDIT) {
 		if(debug)console.log('\tdoing edit');
		this.doEdit(mapGlyph);
		return false;
	    }

	    if(this.command!=null) {
 		if(debug)console.log('\tdoing command:' + this.command);
		return false;
	    }
	    let showPopup = (html,props)=>{
		this.getMap().lastClickTime  = new Date().getTime();
		let id = HU.getUniqueId('div');
		let div = HU.div([ATTR_ID,id]);
//		let location = e.feature.geometry.getBounds().getCenterLonLat();
		let location = mapGlyph.getCentroid();
		if(this.getMap().currentPopup) {
		    this.getMap().onPopupClose();
		}
		if(location!=null) 
		    location = MapUtils.createLonLat(location.x, location.y);
		else
		    location = this.getMap().getMap().getLonLatFromViewPortPx({x:50,y:10});
		if(xy)    location = this.getMap().getMap().getLonLatFromViewPortPx(xy);
		let popup =this.getMap().makePopup(location,div,props);
		this.getMap().currentPopup = popup;
		this.getMap().getMap().addPopup(popup);
		if(this.isIsolineEnabled()) {
		    let latlon =   this.getMap().transformProjPoint(location);
		    html+='<p>'+
			HU.onClick('ImdvUtils.getImdv(\'' + this.getId() +'\').addIsolineAt('+ latlon.lat+',' + latlon.lon+')',
				   HU.getIconImage('fa-regular fa-circle-dot')+' ' +'Add Isoline');
		}
		jqid(id).html(html);
		//For some reason the links don't work in the popup
		//so we do this and handle the clicks here
		jqid(id).find('a').each(function() {
		    $(this).click(function(){
			let url = $(this).attr('href');
			if(url) {
			    window.open(url,'_blank');
			}
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
		    html = HU.div([ATTR_STYLE,'max-height:300px;overflow-y:auto;'],html);
		    doPopup(html,{width:this.getProperty('popupWidth',width),
				  height:this.getProperty('popupHeight',height)});
		};
		this.wikify(wiki, mapGlyph.getEntryId(),wikiCallback);
		return false;
	    }

	    if(!Utils.stringDefined(text)) {
 		if(debug)console.log('\tno text')
		return false;
	    }
	    text = mapGlyph.convertPopupText(text).replace(/\n/g,'<br>');
	    doPopup(text);
	    return false;
	},
	getLabels:function() {
	    return this.jq(ID_GLYPH_LABELS);
	},

	appendHeader:function(html) {
	    this.jq(ID_HEADER0).append(html);
	},
	makeMenuBar:function() {
	    if(!this.getMapProperty('showMenuBar',true)) return;
	    let _this = this;
	    let menuBar=  '';
	    [[ID_MENU_FILE,'File'],[ID_MENU_EDIT,'Edit'],[ID_MENU_NEW,'New'],[ID_MENU_VIEW,'View']].forEach(t=>{
		menuBar+=   HU.div([ID,this.domId(t[0]),ATTR_CLASS,'ramadda-menubar-button'],t[1])});
	    menuBar = HU.div([ATTR_CLASS,'ramadda-menubar'], menuBar);

	    let address =
		HU.span([ATTR_STYLE,HU.css('position','relative')], 
			HU.div([ATTR_STYLE,'display:inline-block;',ATTR_ID,this.domId(ID_ADDRESS_CLEAR),ATTR_TITLE,'Clear',ATTR_CLASS,CLASS_CLICKABLE],HU.getIconImage('fa-solid fa-eraser',[],[ATTR_STYLE,'color:#ccc;'])) +
			' ' +
			HU.div([ATTR_ID,this.domId(ID_ADDRESS_WAIT),ATTR_STYLE,HU.css('position','absolute','right','0px',
										'display','inline-block','margin-right','2px',ATTR_WIDTH,'20px')])+
			HU.input('','',[ATTR_ID,this.domId(ID_ADDRESS_INPUT),'placeholder','Search for address','size','20']));

	    if(this.canChange()) {
		address = address +' ' +HU.checkbox(this.domId(ID_ADDRESS_ADD),[ATTR_ID,this.domId(ID_ADDRESS_ADD),ATTR_TITLE,'Add marker to map'],false);
	    }
	    address=HU.span([ATTR_STYLE,'margin-right:5px'], address);

	    address = HU.div([ATTR_STYLE,HU.css('white-space','nowrap','display','none','position','relative'),
			      ATTR_ID,this.domId(ID_ADDRESS)], address);	    
	    
	    let message = HU.div([ID,this.domId(ID_MESSAGE),ATTR_CLASS,'imdv-message']);
	    let mapHeader = HU.div([STYLE,HU.css('margin-left','10px'),
				    ATTR_ID,this.domId(ID_MAP)+'_header']);
	    if(this.canChange()) {
		menuBar=  HU.table([ATTR_ID,this.domId(ID_MAP_MENUBAR),ATTR_WIDTH,'100%'],HU.tr([ATTR_VALIGN,'bottom'],HU.td([],menuBar) +
											 HU.td([ATTR_WIDTH,'50%'], message) +
											 HU.td(['align','right',ATTR_STYLE,'padding-right:10px;',ATTR_WIDTH,'50%'],mapHeader+address)));
	    } else {
		menuBar= HU.table([ATTR_ID,this.domId(ID_MAP_MENUBAR),ATTR_WIDTH,'100%'],HU.tr([ATTR_VALIGN,'bottom'],HU.td([],'') +
											 HU.td([ATTR_WIDTH,'50%'], message) +
											 HU.td(['align','right',ATTR_STYLE,'padding-right:10px;',ATTR_WIDTH,'50%'],mapHeader+address)));
	    }
	    


	    this.jq(ID_TOP_LEFT).html(menuBar);
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
	    this.jq(ID_MENU_VIEW).click(function() {
		_this.showViewMenu($(this));
	    });	    
	    this.jq(ID_MENU_EDIT).click(function() {
		_this.showEditMenu($(this));
	    });
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
	    if(legendStyle=='') legendStyle='left:50px;top:20px;'

	    //gotta have this here or else the draggable sets it to relative
	    legendStyle+=HU.css('position','absolute');
	    let innerDiv = HU.div([ATTR_ID,this.domId(ID_LEGEND_MAP_WRAPPER),ATTR_CLASS,'imdv-legend-map-wrapper',ATTR_STYLE,legendStyle]);
	    let inner = $(innerDiv);

	    this.jq(ID_MAP_CONTAINER).append(inner);
	    let haveCleared = false;
	    inner.draggable({
		containment:this.jq(ID_MAP_CONTAINER),
		//A bit tricky - we clear all the style when we start dragging
		//so if right or bottom were set then those get nuked
		//because the drag drags left/top
		start:function() {
		    //		    inner.attr(ATTR_STYLE,'position:absolute;');
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
		    /* TODO?
		    let set = (which,v) =>{
			v =  Math.max(0,(parseInt(v)))+'px';
			pos[which] =v;
		    }
		    if(top<ph-bottom) set('top',top);
		    else set('bottom',(ph-bottom));
		    if(left<pw-right) set('left',left);
		    else set('right',pw-right);
		    */
		}
	    });
	},


	makeControls:function() {
	    let _this = this;
//	    if(!this.canChange()) return;
	    if(this.haveMadeControls) return;
	    this.haveMadeControls = true;
	    
	    Utils.initDragAndDrop(this.jq(ID_MAP),
				  event=>{},
				  event=>{},
				  (event,item,result) =>{
				      let entryId = this.getProperty('entryId') || this.entryId;
				      Ramadda.handleDropEvent(event, item, result, entryId,this.getProperty("authToken"),(data,entryid, name,isImage)=>{

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
				      let _name = file.name.toLowerCase();
				      if(_name.match('.*\.(json|geojson|gpx|shz|zip|kml|kmz)')) return true;
				      return false;
				  }
				 );

	    this.jq(ID_MAP).css('caret-color','transparent');


	    //		this.jq(ID_LEFT).html(HU.div([ID,this.domId(ID_COMMANDS),ATTR_CLASS,'imdv-commands']));
	    let keyboardControl = new OpenLayers.Control();
	    let control = new OpenLayers.Control();
	    let callbacks = {
		keydown: function(event) {
		    if(event.key=='MediaTrackPrevious') return;
		    HtmlUtils.hidePopupObject();
		    if(event.key=='Escape') {
			_this.clearCommands();
			_this.unselectAll();
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
		    case 'b':
			_this.makeBox(GLYPH_BOX);
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
		    case 'm':
			_this.setCommand(ID_MOVER);
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
	    this.addControl(ID_SELECTOR,'Click-drag to select',
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

	    this.addControl(ID_EDIT,'Click to edit properties',
			    new OpenLayers.Control.SelectFeature(this.myLayer, {
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
	    let mover =  this.addControl(ID_MOVER,'Click &amp; drag to move',
					 new OpenLayers.Control.DragFeature(this.myLayer,{
		moveFeature: function(pixel) {
		    let mapGlyph = this.feature.mapGlyph;
		    if(!mapGlyph) {
			console.log('no map glyph');
			return;
		    }
		    //If it isn't selected then clear any existing selection and select this one
		    if(!mapGlyph.isSelected()) {
			_this.unselectAll();
			mapGlyph.select();
		    }
		    let selected = _this.getSelected();
		    let res = this.map.getResolution();
		    let dx = res * (pixel.x - this.lastPixel.x);
		    let dy = res * (this.lastPixel.y - pixel.y);
		    selected.forEach(mapGlyph=>{
			mapGlyph.move(dx,dy);
		    });
		    this.lastPixel = pixel;
		    _this.featureChanged();	    
		},
		onDrag: function(feature, pixel) {
		}
	    }));

	    let MyMover =  OpenLayers.Class(OpenLayers.Control.ModifyFeature, {
		dragStart: function(feature) {
		    OpenLayers.Control.ModifyFeature.prototype.dragStart.apply(this, arguments);
		    if(feature && feature.mapGlyph) {
			_this.unselectAll(feature.mapGlyph);
			if(!feature.mapGlyph.isSelected()) {
			    _this.selectGlyph(feature.mapGlyph);
			}
		    }

		},
		dragComplete: function() {
		    OpenLayers.Control.ModifyFeature.prototype.dragComplete.apply(this, arguments);
		    this.theDisplay.featureChanged();	    
		    this.theDisplay.clearMessage2(1000);
		},
		dragVertex: function(vertex, pixel) {
		    if(Utils.isDefined(this.feature.isDraggable) && !this.feature.isDraggable) return
		    let mapGlyph = this.feature.mapGlyph;
		    if(!mapGlyph) return;

		    if(mapGlyph) {
			if(!mapGlyph.isSelected()) mapGlyph.select();
		    }
		    this.theDisplay.showDistances(this.feature.geometry,this.feature.type);
		    if(!this.feature.image &&
		       this.feature.type!=GLYPH_BOX &&
		       !this.feature?.mapGlyph.isImage()) {
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
			_this.checkSelected(this.feature.mapGlyph);
			if(this.mode==OpenLayers.Control.ModifyFeature.ROTATE) {
			    if(this.feature.mapGlyph.isImage()) {
				this.feature.style={strokeColor:'transparent',fillColor:'transparent'};
				let rotation = Utils.getRotation(v);
				this.feature.mapGlyph.style.rotation = rotation.angle;
			    }
			} 
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
		    imageChecker(feature);},
		createVertices:false,
		mode:OpenLayers.Control.ModifyFeature.RESHAPE});
	    let rotator = new MyMover(this.myLayer, {
		theDisplay:this,
		createVertices:false,
		mode:OpenLayers.Control.ModifyFeature.ROTATE});		

	    this.addControl(ID_RESIZE,'Click to resize',resizer);
	    this.addControl(ID_RESHAPE,'Click to reshape',reshaper);
	    this.addControl(ID_ROTATE,'Click to rotate',rotator);		

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
    getNewHelp: function() {
	return this.options.newHelp;
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
    isOSM:  function() {
	return this.options.isOsm;
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
    patternNode.setAttributeNS(null, ATTR_ID,svgId);
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
    "sparse-rect-1":{
	width:30,height:30,
	svg:"<svg xmlns='http://www.w3.org/2000/svg' width='30' height='30'><rect width='2' height='2' fill='<%= foreground %>' />' /></svg>"
    },
    "sparse-rect-2":{
	width:40,height:40,
	svg:"<svg xmlns='http://www.w3.org/2000/svg' width='30' height='30'><rect width='2' height='2' fill='<%= foreground %>' />' /></svg>"
    },    
    "sparse-rect-3":{
	width:50,height:50,
	svg:"<svg xmlns='http://www.w3.org/2000/svg' width='30' height='30'><rect width='2' height='2' fill='<%= foreground %>' />' /></svg>"
    },
    "sparse-rect-4":{
	width:60,height:60,
	svg:"<svg xmlns='http://www.w3.org/2000/svg' width='30' height='30'><rect width='2' height='2' fill='<%= foreground %>' />' /></svg>"
    },    


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


window.olDrawTextHook= function(renderer,style,label,featureId) {
    //check if we draw a background
    //returns true if the label.bbox.width==0
    let needRedo = olCheckLabelBackground(renderer,style,label,featureId);
    if (!label.parentNode) {
        renderer.textRoot.appendChild(label);
	if(needRedo) {
	    let bbox = label.getBBox();
	    renderer.textRoot.removeChild(label);
	    olCheckLabelBackground(renderer,style,label,featureId,bbox);
	    renderer.textRoot.appendChild(label);
	}
    }
}

function olCheckLabelBackground(renderer,   style,label,featureId,bbox) {
    if(style.textBackgroundFillColor !="" || style.textBackgroundStrokeColor !="") {
	bbox = bbox??label.getBBox();
	if(bbox.width==0 || bbox.height==0) {
	    return true;
	}
	let shape = 'rect';
	if(style.textBackgroundShape=='circle') shape='circle'
	else if(style.textBackgroundShape=='ellipse') shape='ellipse'	    
	let bg = renderer.nodeFactory(featureId + '_textbackground', shape);
	let pad=!isNaN(style.textBackgroundPadding)?style.textBackgroundPadding:0;
	let bgStyle = "";
	bgStyle+="fill:" +((style.textBackgroundFillColor=='' || !style.textBackgroundFillColor)?"transparent":style.textBackgroundFillColor)+";";
	if(style.textBackgroundStrokeColor!="") bgStyle+="stroke:" +style.textBackgroundStrokeColor+";";	    
	if(style.textBackgroundStrokeWidth>=0)
	    bgStyle+="stroke-width:" + style.textBackgroundStrokeWidth+";";
	if(!isNaN(style.textBackgroundFillOpacity))
	    bgStyle+="fill-opacity:" + style.textBackgroundFillOpacity+";";

	if(shape=='circle') {
	    bg.setAttribute("cx", bbox.x+bbox.width/2);
	    bg.setAttribute("cy", bbox.y+bbox.height/2);
	    bg.setAttribute("r", (bbox.width/2)+(+pad));
	} else   if(shape=='ellipse') {
	    bg.setAttribute("cx", bbox.x+bbox.width/2);
	    bg.setAttribute("cy", bbox.y+bbox.height/2);
	    bg.setAttribute("rx", (bbox.width/2)+(+pad));
	    bg.setAttribute("ry", (bbox.height/2)+(+pad));	    	    				
	} else {
	    bg.setAttribute("x", bbox.x-pad);
	    bg.setAttribute("y", bbox.y-pad);
	    bg.setAttribute("width", bbox.width+pad*2);
	    bg.setAttribute("height", bbox.height+pad*2);
	    if(style.textBackgroundRadius) {
		bg.setAttribute("rx", style.textBackgroundRadius);
	    }

	}
	bg.setAttribute(ATTR_STYLE, bgStyle);
	renderer.vectorRoot.appendChild(bg);
    }
    return false;
}




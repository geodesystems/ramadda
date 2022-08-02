/**
   Copyright 2008-2021 Geode Systems LLC
*/
var xcnt=0;
const DISPLAY_IMDV = "imdv";
const DISPLAY_EDITABLEMAP = "editablemap";
addGlobalDisplayType({
    type: DISPLAY_IMDV,
    label: "Integrated Map Data",
    category:CATEGORY_MAPS,
    tooltip: makeDisplayTooltip("Integrated Map Data"),        
});


var GLYPH_FIXED = "fixed";
var GLYPH_MARKER = "marker";
var GLYPH_POINT = "point";
var GLYPH_LABEL = "label";
var GLYPH_BOX = "box";
var GLYPH_CIRCLE = "circle";
var GLYPH_TRIANGLE = "triangle";
var GLYPH_HEXAGON = "hexagon";
var GLYPH_LINE = "line";
var GLYPH_ROUTE = "route";
var GLYPH_POLYLINE = "polyline";
var GLYPH_FREEHAND = "freehand";
var GLYPH_POLYGON = "polygon";
var GLYPH_FREEHAND_CLOSED = "freehand_closed";
var GLYPH_IMAGE = "image";
var GLYPH_ENTRY = "entry";
var GLYPH_MULTIENTRY = "multientry";
var GLYPH_MAP = "map";
var GLYPH_DATA = "data";
var GLYPH_TYPES_SHAPES = [GLYPH_POINT,GLYPH_BOX,GLYPH_CIRCLE,GLYPH_TRIANGLE,GLYPH_HEXAGON,GLYPH_LINE,GLYPH_POLYLINE,GLYPH_FREEHAND,GLYPH_POLYGON,GLYPH_FREEHAND_CLOSED];
var GLYPH_TYPES_LINES = [GLYPH_LINE,GLYPH_POLYLINE,GLYPH_FREEHAND,GLYPH_POLYGON,GLYPH_FREEHAND_CLOSED,GLYPH_ROUTE];
var GLYPH_TYPES_CLOSED = [GLYPH_POLYGON,GLYPH_FREEHAND_CLOSED,GLYPH_BOX,GLYPH_TRIANGLE,GLYPH_HEXAGON];
var MAP_TYPES = ['type_map','geo_geojson','geo_gpx','geo_shapefile'];
var LEGEND_IMAGE_ATTRS = ['style','color:#ccc;font-size:10pt;'];

function RamaddaImdvDisplay(displayManager, id, properties) {
    OpenLayers.Handler.ImageHandler = OpenLayers.Class(OpenLayers.Handler.RegularPolygon, {
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
                box.style.strokeColor = "transparent";
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
		this.image=  this.display.map.addImageLayer("","","",this.style.imageUrl,true,  b.top,b.left,b.bottom,b.right);
	    } else {
		b = this.display.map.transformLLBounds(b);
		this.image.extent = b;
		this.image.moveTo(b,true,true);
	    }
	    this.image.setOpacity(this.style.imageOpacity);
	}
	
    });

    OpenLayers.Handler.MyPoint = OpenLayers.Class(OpenLayers.Handler.Point, {
	finalize: function(cancel) {
	    OpenLayers.Handler.Point.prototype.finalize.apply(this,arguments);
	    if(!cancel)this.display.handleNewFeature(this.display.getNewFeature());
	},
    });



    OpenLayers.Handler.MyEntryPoint = OpenLayers.Class(OpenLayers.Handler.Point, {
	finalize: function(cancel) {
	    OpenLayers.Handler.Point.prototype.finalize.apply(this,arguments);
	    if(!cancel)this.display.handleNewFeature(this.display.getNewFeature());
	    this.display.clearCommands();	    	    
	},
    });
    

    OpenLayers.Handler.MyPolygon = OpenLayers.Class(OpenLayers.Handler.Polygon, {
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




    OpenLayers.Handler.MyPath = OpenLayers.Class(OpenLayers.Handler.Path, {
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

    OpenLayers.Handler.MyRoute = OpenLayers.Class(OpenLayers.Handler.Path, {
	finalize: function(cancel) {
	    if(this.makingRoute) return;
	    OpenLayers.Handler.Path.prototype.finalize.apply(this,arguments);
	    if(cancel) return;
	    if(this.finishedWithRoute) return;
	    //A hack to get the line that was just drawn
	    let line =  this.display.getNewFeature();
	    if(!line || !line.geometry) {
//		this.display.handleNewFeature(line);
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
		mode:this.display.routeType??"car",
		points:Utils.join(xys,",")
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

	    let url = ramaddaBaseUrl+"/map/getroute?entryid="+this.display.getProperty("entryId");
	    this.finishedWithRoute = true;
	    this.display.showProgress("Creating route...");
	    this.makingRoute = true;
	    $.post(url, args,data=>{
		reset();
		this.display.myLayer.removeFeatures([line]);
		if(data.error) {
		    this.display.handleError(data.error);
		    return;
		}
		if(!data.routes || data.routes.length==0) {
		    alert("No routes found");
		    this.display.clearMessage2();
		    return;
		}
		let points = [];
		let routeData = data.routes[0];
		if(routeData.overview_polyline) {
		    let d = googleDecode(routeData.overview_polyline.points);
		    d.forEach(pair=>{
			points.push(new OpenLayers.Geometry.Point(pair[1],pair[0]));
		    });
		} else {
		    routeData.sections.forEach(section=>{
			let decoded = hereDecode(section.polyline);
			decoded.polyline.forEach(pair=>{
			    points.push(new OpenLayers.Geometry.Point(pair[1],pair[0]));
			});
		    });
		}
		let  route = this.display.getMap().createPolygon("", "", points, {
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


    OpenLayers.Handler.MyRegularPolygon = OpenLayers.Class(OpenLayers.Handler.RegularPolygon, {
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
    



    const LIST_ROW_CLASS  = "imdv-feature-row";
    const LIST_SELECTED_CLASS  = "imdv-feature-selected";
    const ID_TOPWIKI = "topwiki";
    const ID_EDIT_NAME  ="editname";
    const ID_MESSAGE  ="message";
    const ID_MESSAGE2  ="message2";    
    const ID_MESSAGE3  ="message3";    
    const ID_LIST_DELETE  ="listdelete";
    const ID_LIST_OK  ="listok";
    const ID_LIST_CANCEL = "listcancel";
    const ID_DELETE  ="delete";
    const ID_OK  ="ok";
    const ID_APPLY  ="apply";
    const ID_CANCEL = "cancel";
    const ID_MENU_NEW = "new_file";
    const ID_MENU_FILE = "menu_file";
    const ID_MENU_EDIT = "menu_edit";    
    const ID_TOBACK = "toback";
    const ID_TOFRONT = "tofront";    

    const ID_CUT = "cut";
    const ID_COPY= "copy";
    const ID_PASTE= "paste";        
    const ID_COMMANDS = "commands";
    const ID_CLEAR = "clear";
    const ID_CMD_LIST = "cmdlist";
    const ID_LIST = "list";        
    const ID_PROPERTIES = "properties";
    const ID_NAVIGATE = "navigate";
    const ID_SAVE = "save";
    const ID_SAVEAS = "saveas";    
    const ID_DOWNLOAD = "download";    
    const ID_SELECTOR = "selector";
    const ID_SELECT_ALL = "selectall";    
    const ID_EDIT = "edit";
    const ID_MOVER = "mover";
    const ID_RESIZE = "resize";
    const ID_RESHAPE = "reshape";    
    const ID_ROTATE  = "rotate";
    const ID_LEGEND = "legend";
    const ID_MAP_PROPERTIES = "mapproperties";

    if(!Utils.isDefined(properties.showOpacitySlider)&&!Utils.isDefined(getGlobalDisplayProperty('showOpacitySlider'))) 
	properties.showOpacitySlider=true; 
    const SUPER = new RamaddaBaseMapDisplay(displayManager,  id, DISPLAY_IMDV,  properties);
    RamaddaUtil.inherit(this,SUPER);
    addRamaddaDisplay(this);
    this.defineSizeByProperties();
    //do this here as this might be used by displaymap for annotation
    this.map = this.getProperty("theMap");
    const myProps = [
	{label:'Editable Map Properties'},
	{p:"displayOnly",d:false},
	{p:"strokeColor",d:"blue"},
	{p:"strokeWidth",d:2},
	{p:"pointRadius",d:10},
	{p:"externalGraphic",d:"/map/marker-blue.png"},
	{p:"fontSize",d:"12px"},
	{p:"fontWeight",d:"normal"},
	{p:"fontStyle",d:"normal"},	
	{p:"fontFamily",d:"'Open Sans', Helvetica Neue, Arial, Helvetica, sans-serif"},
	{p:"imageOpacity",d:1},
	{p:'showLegend',d:true},
	{p:'showMenuBar',d:true},
	{p:'showLegendShapes',d:true},	
	{p:'showMapLegend',d:false},

    ];
    
    displayDefineMembers(this, myProps, {
	commands: [],
        myLayer: [],
	glyphs:[],
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

	findGlyph:function(id) {
	    let glyph;
	    this.glyphs.every(mapGlyph=>{
		if(mapGlyph.getId()==id) {
		    glyph=mapGlyph;
		    return false;
		}
		return true;
	    });
	    return glyph;
	},
		    

	addGlyph:function(glyph) {
	    if(Array.isArray(glyph))
		this.glyphs.push(...glyph);
	    else
		this.glyphs.push(glyph);
	},

	handleEvent:function(event,lonlat) {
	    return;
	},
	handleNewFeature:function(feature,style,mapOptions) {
	    style = style || feature?.style;
	    mapOptions = mapOptions??feature?.mapOptions ?? style?.mapOptions;
//	    console.log("new:" +mapOptions.type);
	    if(feature && feature.style && feature.style.mapOptions)
		delete feature.style.mapOptions;
	    let mapGlyph = new MapGlyph(this,mapOptions.type, mapOptions, feature,style);
	    this.addGlyph(mapGlyph);
	    this.featureChanged();	    
	    this.clearMessage2(1000);
	    return mapGlyph;
	},

	getLayer: function() {
	    return this.myLayer;
	},
	redraw: function(feature) {
	    this.myLayer.redraw(feature);
	},
	getNewFeature: function() {
	    return this.myLayer.features[this.myLayer.features.length-1];
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


	    let distancePrefix = "Total distance: ";

	    if(glyphType == GLYPH_CIRCLE || glyphType == GLYPH_BOX || glyphType == GLYPH_POLYGON || glyphType == GLYPH_TRIANGLE || glyphType == GLYPH_FREEHAND_CLOSED || glyphType==GLYPH_IMAGE) {
		area = MapUtils.calculateArea(pts);
		acres = area/43560;
	    }

	    if(glyphType == GLYPH_CIRCLE || glyphType == GLYPH_BOX ||  glyphType == GLYPH_TRIANGLE  || glyphType==GLYPH_IMAGE) {
		distancePrefix = "Perimeter: ";
	    }

	    let msg = "Distance: ";
	    if(glyphType == GLYPH_BOX || glyphType == GLYPH_IMAGE) {
		msg = "";
		let w = MapUtils.distance(pts[0].y,pts[0].x,pts[1].y,pts[1].x);
		let h = MapUtils.distance(pts[1].y,pts[1].x,pts[2].y,pts[2].x);		
		let unit = "ft";
		if(w>5280 || h>5280) {
		    unit = "m";
		    w = w/5280;
		    h = h/5280;		    
		}

		msg= "w: " + Utils.formatNumberComma(w) + " " + unit +
		    " h: " + Utils.formatNumberComma(h) + " " + unit;
	    } else {
		let segments = "";
		let total = 0;
		for(let i=0;i<pts.length-1;i++) {
		    let pt1 = pts[i];
		    let pt2 = pts[i+1];		
		    let d = MapUtils.distance(pt1.y,pt1.x,pt2.y,pt2.x);
		    total+=d;
		    let unit = "feet";
		    if(d>5280) {
			unit = "miles";
			d = d/5280;
		    }
		    d = Utils.formatNumberComma(d);
		    segments+= d +" " + unit +" ";
		}
		let unit = "feet";
		if(total>5280) {
		    unit = "miles";
		    total = total/5280;
		}
		msg = distancePrefix + Utils.formatNumberComma(total) +" " + unit;
		if(pts.length>2 && pts.length<6)  {
		    msg+="<br>Segments:" + segments;
		}
		if(pts.length<=1) msg="";
	    }
	    if(!justDistance&&area>0) {
		unit="ft";
		if(area>MapUtils.squareFeetInASquareMile) {
		    unit = "miles";
		    area = area/MapUtils.squareFeetInASquareMile;
		    msg+=   "<br>" +
			"Area: " + Utils.formatNumber(area) +" sq " + unit;
		} else {
		    msg+=   "<br>" +
			"Area: " + Utils.formatNumber(acres) +" acres";
		}
	    }

	    return msg;
	},


	setCommandCursor: function(cursor) {
	    this.getMap().setCursor(cursor??'pointer');
	},


	setCommand:function(command, args) {
	    args = args ||{};
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
	    this.jq("new_" + command).addClass("imdv-command-active");
	    this.commands.every(cmd=>{
		if(cmd.name != command) {
		    return true;
		}
		if(glyphType) {
		    this.setCommandCursor();
		    let styleMap = new OpenLayers.StyleMap({"default":{}});
		    let tmpStyle = {};
		    $.extend(tmpStyle,glyphType.getStyle());
		    tmpStyle.mapOptions = {
			type:glyphType.type
		    }
		    if(glyphType.isFixed()) {
			let text = prompt("Text:");
			if(!text) return
			let mapOptions = tmpStyle.mapOptions;
			delete tmpStyle.mapOptions;
			tmpStyle.text = text;
			this.clearCommands();
			let mapGlyph = new MapGlyph(this,mapOptions.type, mapOptions, null,tmpStyle);
			mapGlyph.addFixed();
			this.addGlyph(mapGlyph);
			this.featureChanged();	    
			this.clearMessage2(1000);
			return;
		    }


		    if(glyphType.isImage() || glyphType.isEntry()||glyphType.isMultiEntry() || glyphType.isMap() || glyphType.isData()) {
			let callback = (entryId,imageUrlOrEntryAttrs) =>{
			    let attrs = {};
			    let imageUrl;
			    if(typeof imageUrlOrEntryAttrs == "string") {
				imageUrl = imageUrlOrEntryAttrs;
			    } else {
				attrs = imageUrlOrEntryAttrs;
			    }
			    attrs.entryId = entryId;
			    if(glyphType.isImage()) {
				tmpStyle.strokeColor="#ccc";
				tmpStyle.fillColor = "transparent";
			    }
			    $.extend(tmpStyle.mapOptions,attrs);
			    tmpStyle.mapOptions.entryId = entryId;
			    tmpStyle.mapOptions.entryType = attrs.entryType;
			    if(attrs.entryName) {
				attrs.name  = attrs.entryName;
				delete attrs.entryName;
			    }
			    if(glyphType.isMap()) {
				let mapOptions = tmpStyle.mapOptions;
				delete tmpStyle.mapOptions;
				$.extend(mapOptions,attrs);
				let mapGlyph = this.handleNewFeature(null,tmpStyle,mapOptions);
				mapGlyph.checkMapLayer();
				return;
			    }
			    if(glyphType.isMultiEntry()) {
				let mapOptions = tmpStyle.mapOptions;
				$.extend(mapOptions,attrs);
				delete tmpStyle.mapOptions;
				this.clearCommands();
				let mapGlyph = this.handleNewFeature(null,tmpStyle,mapOptions);
				mapGlyph.addEntries(true);
				return
			    }

			    if(glyphType.isData()) {
				let mapOptions = tmpStyle.mapOptions;
				$.extend(mapOptions,attrs);
				delete tmpStyle.mapOptions;
				this.clearCommands();
				this.createMapData(mapOptions);
				return;
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
			    this.showCommandMessage(glyphType.isImage()?"Click and drag to create image":"New Entry Marker");
			    cmd.activate();
			    selectCancel(true);
			};

			if(args.url) {
			    callback(null, args.url);
			    return;
			}
	
			//Do this a bit later because the dialog doesn't get popped up
			let initCallback = ()=>{
			    this.jq('imageurl').keypress(function(e){
				if(e.keyCode == 13) {
				    callback("",$(this).val());
				}
			    });
			};
			let extra = HU.div(['style','margin:5px;'],
					   HU.b("Enter Image URL: ") + HU.input("",this.lastImageUrl??"",['id',this.getDomId('imageurl'),'size','40']) +
					   "<br>Or select entry:");
			let props = {title:glyphType.isImage()?'Select Image':
				     (glyphType.isEntry()||glyphType.isMultiEntry()?'Select Entry':glyphType.isData()?'Select Data':'Select Map'),
				     extra:glyphType.isImage()?extra:null,
				     initCallback:initCallback,
				     callback:callback,
				     'eventSourceId':this.domId(ID_MENU_NEW)};
			let entryType = glyphType.isImage()?'type_image':glyphType.isMap()?Utils.join(MAP_TYPES,','):'';
			selectCreate(null, HU.getUniqueId(""),"",false,'entryid',this.getProperty('entryId'),entryType,null,props);
			return
		    } else if(glyphType.getType() == GLYPH_MARKER) {
			let html =  "";
			let icon = this.lastIcon || tmpStyle.externalGraphic;
			let icons = HU.image(icon,['id',this.domId("externalGraphic_image")]) +
			    HU.hidden("",icon,['id',this.domId("externalGraphic")]);
			html += icons;
			html+=HU.div(['style',HU.css('text-align','center','padding-bottom','8px','margin-bottom','8px','border-bottom','1px solid #ccc')], HU.div([ID,this.domId(ID_OK), CLASS,"display-button"], "OK") + SPACE2 +
				     HU.div([ID,this.domId(ID_CANCEL), CLASS,"display-button"], "Cancel"));
			
			html+=HU.b("Select Icon");
			html+=HU.div(['id',this.domId('emojis')]);

			html=HU.div(['style',HU.css('xmin-width','250px','margin','5px')],html);
			let dialog = HU.makeDialog({content:html,title:'Select Marker',header:true,my:"left top",at:"left bottom",draggable:true,anchor:this.jq(ID_MENU_NEW)});

			this.addEmojis(this.jq('emojis'));
			let cancel = ()=>{
			    dialog.remove();
			}
			let ok = ()=>{
			    this.lastIcon = this.jq('externalGraphic').val();
			    tmpStyle.externalGraphic = this.lastIcon;
			    dialog.remove();
			    cmd.handler.style = tmpStyle;
			    cmd.handler.layerOptions.styleMap=styleMap;
			    this.showCommandMessage("New Marker");
			    cmd.activate();
			}
			this.jq(ID_OK).button().click(ok);
			this.jq(ID_CANCEL).button().click(()=>{
			    dialog.remove();
			});


			return
		    } else if(glyphType.isLabel()) {
			let input =  HU.textarea("",this.lastText??"",[ID,this.domId('labeltext'),"rows",3,"cols", 40]);
			let html =  HU.formTable();
			html += HU.formEntryTop("Text:",input);
			let icon = this.lastIcon || tmpStyle.externalGraphic;
			let icons = HU.image(icon,['id',this.domId('externalGraphic_image')]) +
			    HU.hidden('',icon,['id',this.domId('externalGraphic')]);
			html += HU.formEntry("",HU.checkbox(this.domId('includeicon'),[],this.lastIncludeIcon,"Include Icon")+" " + icons);
			html+="</table>";
			html+=HU.div(['style',HU.css('text-align','center','padding-bottom','8px','margin-bottom','8px','border-bottom','1px solid #ccc')], HU.div([ID,this.domId(ID_OK), CLASS,"display-button"], "OK") + SPACE2 +
				     HU.div([ID,this.domId(ID_CANCEL), CLASS,"display-button"], "Cancel"));
			
			html+=HU.b("Select Icon");
			html+=HU.div(['id',this.domId('emojis')]);

			html=HU.div(['style',HU.css('xmin-width','250px','margin','5px')],html);
			let dialog = HU.makeDialog({content:html,title:'Text',header:true,my:"left top",at:"left bottom",draggable:true,anchor:this.jq(ID_MENU_NEW)});

			this.addEmojis(this.jq('emojis'));
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
				tmpStyle.labelYOffset="10";
			    }
			    let text = this.jq("labeltext").val();
			    dialog.remove();
			    if(!Utils.stringDefined(text)) return;
			    this.lastText = text;
			    tmpStyle.label = text;
			    cmd.handler.style = tmpStyle;
			    cmd.handler.layerOptions.styleMap=styleMap;
			    this.showCommandMessage("New Label");
			    cmd.activate();
			}
			this.jq(ID_OK).button().click(ok);
			this.jq(ID_CANCEL).button().click(()=>{
			    dialog.remove();
			});
			return;
		    }
		    cmd.handler.style = tmpStyle;
		    cmd.handler.layerOptions.styleMap=styleMap;
		}
		let message = glyphType?"New " + glyphType.getName():cmd.message;
		message = message||"";
		if(glyphType && glyphType.isRoute()) {
		    let html =  HU.formTable();
		    if(this.getProperty("hereRoutingEnabled") || this.getProperty("googleRoutingEnabled")) {
			let providers = [];
			if(this.getProperty("googleRoutingEnabled")) providers.push("google");
			if(this.getProperty("hereRoutingEnabled")) providers.push("here");			
			html+=HU.formEntry("Provider:", HU.select("",['id',this.domId("routeprovider")],providers,this.routeProvider));
		    }
		    html+=HU.formEntry("Route Type:" , HU.select("",['id',this.domId("routetype")],["car","bicycle","pedestrian"],this.routeType));
		    html += HU.close(TAG_TABLE);
		    let buttons  =HU.div([ID,this.domId(ID_OK), CLASS,"display-button"], "OK") + SPACE2 +
			HU.div([ID,this.domId(ID_CANCEL), CLASS,"display-button"], "Cancel");	    
		    html+=HU.div(['style',HU.css('text-align','right','margin-top','5px')], buttons);
		    html=HU.div(['style',HU.css('xmin-width','250px','margin','5px')],html);
		    let dialog = HU.makeDialog({content:html,title:'Select Route Type',header:true,my:"left top",at:"left bottom",anchor:this.jq(ID_MENU_NEW)});
		    let ok = ()=>{
			cmd.handler.finishedWithRoute = false;
			this.routeProvider = this.jq('routeprovider').val();
			this.routeType = this.jq('routetype').val();
			dialog.remove();
			this.showCommandMessage(message+": " + Utils.makeLabel(this.routeType)+" - Draw one or more line segments");
			cmd.activate();
		    };
		    this.jq(ID_OK).button().click(ok);
		    this.jq(ID_CANCEL).button().click(()=>{
			dialog.remove();
		    });
		} else {
		    this.showCommandMessage(message);
		    cmd.activate();
		}
		return false;
	    });
	},
	clearCommands:function() {
	    this.getMap().closePopup();
	    this.clearMessage2();
	    this.getMap().clearAllProgress();
//	    this.unselectAll();
	    HtmlUtils.hidePopupObject();
	    this.showCommandMessage("");
	    let buttons = this.jq(ID_COMMANDS).find(".ramadda-clickable");
	    buttons.removeClass("imdv-command-active");
	    buttons.each(function() {
		$(this).attr("selected",false);
	    });
	    this.commands.forEach(cmd=>{
		cmd.deactivate();
	    });
	    this.command= null;
	    this.redraw();
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
	    dom.find("[buttoncommand]").click(function(event) {
		event.preventDefault();
		let command  = $(this).attr("buttoncommand");
		let id  = $(this).attr("glyphid");
		let mapGlyph   = _this.findGlyph(id);
		if(!mapGlyph) {
		    console.error("No map glyph from id:" + id);
		    return;
		}
		if(command=="toback") _this.changeOrder(false,mapGlyph);
		else if(command=="tofront") _this.changeOrder(true,mapGlyph);		
		else if(command=="edit") {
		    _this.editFeatureProperties(mapGlyph);
		}
	    });
	},
	    
	makeGlyphButtons:function(mapGlyph,includeEdit) {
	    let buttons = "";
	    if(includeEdit) {
		buttons+=HU.span([CLASS,"ramadda-clickable",TITLE,"Edit","glyphid",mapGlyph.getId(),"buttoncommand","edit"],
				 HU.getIconImage("fas fa-cog")) +"&nbsp;";
	    }
	    buttons+=
		HU.span([CLASS,"ramadda-clickable",TITLE,"To back","glyphid",mapGlyph.getId(),"buttoncommand","toback"],
			HU.image(Utils.getIcon("shape_move_back.png"))) + "&nbsp;" +
		HU.span([CLASS,"ramadda-clickable",TITLE,"To front","glyphid",mapGlyph.getId(),"buttoncommand","tofront"],
			HU.image(Utils.getIcon("shape_move_front.png"))) + "&nbsp;";
	    return buttons;
	},
	makeListItem:function(mapGlyph,idx) {
	    let style  = mapGlyph.getStyle()||{};
	    let line = "";
	    let type = mapGlyph.getType();
	    let select = HU.span(['title','Click to select','style',HU.css('padding-left','0px','padding-right','5px'), 'glyphid',mapGlyph.getId(),'class','ramadda-clickable imdv-feature '], HU.getIconImage("fas fa-arrow-pointer"));
	    let visible = HU.checkbox("",['style','margin-right:5px;','title','Visible','glyphid',mapGlyph.getId(),'class','ramadda-clickable imdv-feature-visible '],mapGlyph.getVisible());
	    let title =  mapGlyph.getLabel();
	    title+="<br>" +
		select + visible +
		this.makeGlyphButtons(mapGlyph,true);
 	    line += HU.td(["nowrap","",STYLE,HU.css("padding","5px")], title);
	    let col = mapGlyph.getDecoration();
	    let msg = this.getDistances(mapGlyph.getGeometry(),mapGlyph.getType());
	    if(msg) {
		col+="" + msg.replace(/<br>/g," ");
	    }
	    line+= HU.td(["glyphid",mapGlyph.getId(),
			  STYLE,HU.css("padding","5px")],col);
	    return line;
	},	    

	createMapData:function(mapOptions) {
	    let displayAttrs = {};
	    let callback = text=>{
//		console.log(text);
		let ff = text.match(/"filterFields":\"([^\"]+)"/);
		let regexp = /createDisplay *\( *\"map\" *(.*?)}\);/;
		regexp = /createDisplay\s*\(\s*\"map\"\s*,\s*({[\s\S]+?\})\);/;				
		let match = text.match(regexp);
		let attrs = {};
		let inner;
		let pointDataUrl = null;
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
		let widget =  HU.textarea("",userInput,[ID,this.domId('displayattrs'),"rows",10,"cols", 60]);
		let andZoom = HU.checkbox(this.domId('andzoom'),[],true,"Zoom to display");
		let buttons  =HU.center(HU.div([ID,this.domId(ID_OK), CLASS,"display-button"], "OK") + SPACE2 +
					HU.div([ID,this.domId(ID_CANCEL), CLASS,"display-button"], "Cancel"));
		
		widget = HU.div(['style',HU.css('margin','4px')], widget+"<br>"+andZoom + buttons);
		
		let dialog =  HU.makeDialog({content:widget,anchor:this.jq(ID_MENU_FILE),title:"Map Display Attributes",header:true,draggable:true,remove:false});

		
		this.jq(ID_OK).button().click(()=>{
		    let displayAttrs = this.parseDisplayAttrs(this.jq('displayattrs').val());
		    displayAttrs.pointDataUrl = pointDataUrl;
		    let mapGlyph = this.handleNewFeature(null,null,mapOptions);
		    mapGlyph.addMapData(displayAttrs,this.jq("andzoom").is(":checked"));
		    dialog.remove();
		});
		this.jq(ID_CANCEL).button().click(()=>{
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
		if(this.isFeatureSelected(mapGlyph)) {
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

	    let html ="";
	    html+=HU.div([ID,this.domId(ID_LIST), STYLE,HU.css("margin-bottom","10px","border","1px solid #ccc", "max-height","300px","max-width","600px","overflow-x","auto","overflow-y","auto")], "");

	    html+="<center>";
	    html +=HU.div([ID,this.domId(ID_LIST_DELETE), CLASS,"display-button"], "Delete Selected");
	    html += SPACE2;	    
	    html +=HU.div([ID,this.domId(ID_LIST_CANCEL), CLASS,"display-button"], "Close");	    
	    html+="</center>";
	    html  = HU.div([CLASS,"wiki-editor-popup"], html);
	    let dialog = this.listDialog  = HU.makeDialog({content:html,anchor:this.jq(ID_MENU_FILE),title:"Features",header:true,draggable:true,remove:false});
	    this.addFeatureList();
	    let _this  = this;
	    this.jq(ID_LIST_DELETE).button().click(()=>{
		let cut  = [];
		this.jq(ID_LIST).find(".imdv-feature-selected").each(function() {
		    let id  = $(this).attr("glyphid");
		    let mapGlyph   = _this.findGlyph(id);
		    if(!mapGlyph) {
			console.error("No map glyph from id:" + id);
			return;
		    }
		    cut.push(mapGlyph);
		});
		this.removeImages(cut);
		this.setClipboard(cut);
		this.removeMapGlyphs(cut);
		this.addFeatureList();
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
	    
	removeMapGlyphs: function(mapGlyphs) {
	    mapGlyphs.forEach(mapGlyph=>{
		Utils.removeItem(this.glyphs, mapGlyph);
		mapGlyph.doRemove();
	    });
	    this.featureChanged();	    
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
		}
	    });
	    this.addGlyph(newOnes);
	    this.makeLegend();
	},
	getFeaturePropertyApply:function() {
	    return (mapGlyph, props)=>{
		this.featureChanged();	    
		let style = {};
		mapGlyph.setName(this.jq("mapglyphname").val());
		mapGlyph.applyPropertiesComponent();
		if(mapGlyph.isData()) {
		    let displayAttrs = this.parseDisplayAttrs(this.jq('displayattrs').val());
		} else if(props) {
		    props.forEach(prop=>{
			let id = "glyphedit_" + prop;
			if(prop=="externalGraphic") id ="externalGraphic";
			if(prop=="labelSelect") return;
			let v = this.jq(id).val();
			if(prop=="label") {
			    v = v.replace(/\\n/g,"\n");
			}
			if(prop=="showLabels") {
			    v = this.jq(id).is(":checked");
			}
			style[prop] = v;
		    });
		}


		if(mapGlyph.isData()) {
		    let displayAttrs = this.parseDisplayAttrs(this.jq('displayattrs').val());
		    mapGlyph.applyDisplayAttrs(displayAttrs);
		} else if(mapGlyph.isMultiEntry()) {
		    mapGlyph.addEntries();
		}

		if(Utils.stringDefined(style.popupText)) {
		    style.cursor = 'pointer';
		} else {
		    style.cursor = 'auto';
		}
		if(mapGlyph.getImage()) {
		    mapGlyph.getImage().setOpacity(style.imageOpacity);
		    mapGlyph.checkImage();
		}
		mapGlyph.applyStyle(style);
		mapGlyph.setVisible(this.jq("visible").is(":checked"),true);
		mapGlyph.setVisibleLevelRange(this.jq("minlevel").val().trim(),
					      this.jq("maxlevel").val().trim());
		this.redraw();
		this.makeLegend();
		this.showMapLegend();
	    };
	},
	doEdit: function(mapGlyph) {
	    if(!mapGlyph) {
		if(this.getSelected().length==0) return;
		mapGlyph = this.getSelected()[0];
	    }
	    if(!mapGlyph) return;
	    let style = mapGlyph.style;
	    let html =HU.div([STYLE,HU.css("margin","8px")], "Feature: " + mapGlyph.type); 
	    this.redraw(mapGlyph);
	    if(mapGlyph.image && Utils.isDefined(mapGlyph.image.opacity)) {
		mapGlyph.style.imageOpacity=mapGlyph.image.opacity;
	    }
	    this.editFeatureProperties(mapGlyph);
	},
	
	makeMenu: function(html) {
	    return  HU.div([CLASS,"wiki-editor-popup"], html);
	},
	menuItem: function(id,label,cmd) {
	    if(cmd) {
//		HU.image(icon_command,['width','12px']);
		let prefix = '';
		if(cmd!="Esc") prefix = 'Ctrl-';
		label = HU.leftRightTable(label,HU.div(['style','margin-left:8px;'], HU.span(['style','color:#ccc'], prefix+cmd)));
	    }
	    return  HU.div([ID,id,CLASS,"ramadda-clickable"],label);
	},
	editFeatureProperties:function(mapGlyph) {
	    this.doProperties(mapGlyph.getStyle(), this.getFeaturePropertyApply(), mapGlyph);
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
		if(mapGlyph && mapGlyph.getType()==GLYPH_MARKER) {
		    props = ["pointRadius","externalGraphic"];
		} 
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
	    props.forEach(prop=>{
		let id = "glyphedit_" + prop;
		let domId = this.domId(id);
		if(notProps.includes(prop)) return;
		let label =  Utils.makeLabel(prop);		
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
			size="80"
			widget =  HU.textarea("",v,[ID,domId,"rows",5,"cols", 60]);
		    } else if(prop=="popupText") {
			//skip this
		    } else if(prop=="wikiText"|| prop=="text") {
			size="80"
			widget =  HU.textarea("",v||"",[ID,domId,"rows",5,"cols", 60]);
		    } else if(prop=="strokeDashstyle") {
			widget = HU.select("",['id',domId],['solid','dot','dash','dashdot','longdash','longdashdot'],v);
		    } else if(prop=="fontWeight") {
			widget = HU.select("",['id',domId],["normal","bold","lighter","bolder","100","200","300","400","500","600","700","800","900"],v);
 		    } else if(prop=="fontStyle") {
			widget = HU.select("",['id',domId],["normal","italic"],v);			
		    } else {
			if(props == "pointRadius") label="Size";
			if(prop=="strokeWidth" || prop=="pointRadius" || prop=="fontSize" || prop=="imageOpacity") size="4";
			else if(prop=="fontFamily") size="60";
			else if(prop=="imageUrl") size="80";		    
			if(prop.indexOf("Color")>=0) {
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
			    bar = HU.div(['style','max-height:66px;overflow-y:auto;border:1px solid #ccc;'],bar);
			    widget =  HU.input("",v,['class','ramadda-imdv-color',ID,domId,"size",8]);
			    widget =  HU.div(['id',domId+'_display','class','ramadda-dot', 'style',HU.css('background',v)]) +
				HU.space(2)+widget;
			    widget  = HU.table(['cellpadding','0','cellspacing','0'],HU.tr(['valign','top'],HU.tds([],[widget,bar])));
			} else if(prop=="labelAlign") {
			    //lcr tmb
			    let items = [["lt","Left Top"],["ct","Center Top"],["rt","Right Top"],
					 ["lm","Left Middle"],["cm","Center Middle"],["rm","Right Middle"],
					 ["lb","Left Bottom"],["cb","Center Bottom"],["rb","Right Bottom"]];
			    widget =  HU.select("",['id',domId],items,v);			
			} else if(prop=="showLabels") {
			    widget = HU.checkbox(domId,[],v);
			} else if(prop.indexOf("Width")>=0 || prop.indexOf("Offset")>=0) {
			    if(!Utils.isDefined(v)) v=1;
			    else if(v==="") v=1;
			    let min  = prop.indexOf("Offset")>=0?0:0;
			    widget =  HU.input("",v,[ID,domId,"size",4])+HU.space(4) +
				HU.div(['slider-min',min,'slider-max',50,'slider-step',1,'slider-value',v,'slider-id',domId,ID,domId+'_slider','class','ramadda-slider',STYLE,HU.css("display","inline-block","width","200px")],"");

			} else if(prop.indexOf("Opacity")>=0) {
			    if(!v || v=="") v= 1;
			    widget =  HU.input("",v,[ID,domId,"size",4])+HU.space(4) +
				HU.div(['slider-min',0,'slider-max',1,'slider-value',v,'slider-id',domId,ID,domId+'_slider','class','ramadda-slider',STYLE,HU.css("display","inline-block","width","200px")],"");
			} else {
			    widget =  HU.input("",v,[ID,this.domId(id),"size",size]);
			}
		    }
		}
		html+=HU.formEntry(label+":",widget);
	    });
	    html+="</table>";
	    html = HU.div([STYLE,HU.css("max-height","350px","overflow-y","scroll","margin-bottom","5px")], html);
	    return {props:props,html:html};
	},
	    
	doProperties: function(style, apply,mapGlyph) {

	    let _this = this;
	    style = style || mapGlyph?mapGlyph.getStyle():style;
	    let html="";
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
		html =  HU.formTable();
		html+=HU.formEntry("Name:",HU.input("",mapGlyph.getName(),['id',this.domId('mapglyphname'),'size','40']));
		let level = mapGlyph.getVisibleLevelRange()??{};
		html+=HU.formEntry("Visible:",HU.checkbox(this.domId("visible"),[],mapGlyph.getVisible()));
		let levels = [["","None"],[4,"4 - Most zoomed out"],5,6,7,8,
			      9,10,11,12,13,14,15,16,17,18,19,[20,"20 - Most zoomed in"]];
		html+=HU.formEntry("Visible between levels:",
				   HU.select("",[ID,this.domId("minlevel")],levels,Utils.isDefined(level.min)?level.min:"",null,true) +
				   " &lt;= level &lt;= " +
				   HU.select("",[ID,this.domId("maxlevel")],levels,Utils.isDefined(level.max)?level.max:""));
		html+=HU.formEntry("Current level:" , this.getCurrentLevel());
		html+="</table>";
		let domId = this.domId("glyphedit_" + 'popupText');
		html+=HU.b("Popup Text:") +"<br>" + HU.textarea("",style.popupText??"",[ID,domId,"rows",5,"cols", 60]);
		content.push(["Properties",html]);
	    }
	    if(mapGlyph&&mapGlyph.isData()) {
		let displayAttrs = mapGlyph.getDisplayAttrs();
		let attrs = "";
		for(a in displayAttrs) {
		    attrs+=a+"="+ displayAttrs[a]+"\n";
		}
		content.push(["Display Properties", HU.textarea("",attrs,[ID,this.domId('displayattrs'),"rows",10,"cols", 60])]);
	    } else {
		let r =  this.makeStyleForm(style,mapGlyph);
		content.push(["Style",r.html]);
		props = r.props;
	    }
	    if(mapGlyph) {
		mapGlyph.getPropertiesComponent(content);
	    }
	    html = buttons;
	    content.forEach((tuple,idx)=>{
		html+=HU.toggleBlock(HU.b(tuple[0]),tuple[1],idx==0);
	    });
	    html+=buttons;
	    html  = HU.div(['style','min-width:600px',CLASS,"wiki-editor-popup"], html);
	    this.map.ignoreKeyEvents = true;
	    let dialog = HU.makeDialog({content:html,anchor:this.jq(ID_MENU_FILE),title:"Map Properties",header:true,draggable:true});
	    if(mapGlyph) {
		mapGlyph.initPropertiesComponent(dialog);
		this.initGlyphButtons(dialog);
	    }
	    let icons =dialog.find("#" + this.domId("externalGraphic_icons"));
	    if(icons.length>0) {
		this.addEmojis(icons);
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

	    dialog.find('.ramadda-imdv-color').change(function() {
		let c = $(this).val();
		let id = $(this).attr('id');
		$("#"+ id+'_display').css('background',c);
	    });
	    dialog.find('.ramadda-color-select').click(function(){
		let c = $(this).attr('color');
		let id = $(this).attr('widget-id');
		$("#"+ id).val(c);
		$("#"+ id+'_display').css('background',c);
	    });

	    if(apply==null) {
		apply = () =>{
		    let style = {};
		    props.forEach(prop=>{
			let value = this.jq(prop).val();
			this.setProperty(prop, value);
			if(prop == "externalGraphic") {
			    if(!value.startsWith(ramaddaBaseUrl))
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


	    dialog.find('.display-button').button().click(function() {
		let command = $(this).attr("command");
		switch(command) {
		case ID_OK: 
		    apply(mapGlyph,props);
		    _this.addFeatureList();
		    close();
		    break;
		case ID_APPLY:
		    apply(mapGlyph,props);
		    _this.addFeatureList();
		    break;
		case ID_CANCEL:
		    close();
		    break;
		case ID_DELETE:
		    _this.removeMapGlyphs([mapGlyph]);
		    _this.addFeatureList();
		    close();
		    break;
		}
	    });
	},
	addEmojis:function(icons) {
	    let _this = this;
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
		this.featureHasBeenChanged = false;
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
			_this.featureHasBeenChanged = false;
			_this.showMessage(result.message);
		    }
		}
	    }).fail(function(jqxhr, textStatus, error) {
		_this.showMessage("failed to save map:" + textStatus +" " + error);
	    });
	},
	doDownload: function() {
	    let json = this.makeJson();
	    Utils.makeDownloadFile("map.json",json);
	},
	makeJson: function() {
	    let _this = this;
	    let list =[];
            this.getGlyphs().forEach(mapGlyph=>{
		let attrs=mapGlyph.getAttributes();
		let obj = {
		    mapOptions:attrs,
		    id:mapGlyph.getId()
		};
		let style = mapGlyph.getStyle();
		if(mapGlyph.getMapLayer()) {
		    style = mapGlyph.getMapLayer().style||style;
		}
		if(style) {
		    if(mapGlyph.getImage() && Utils.isDefined(mapGlyph.getImage().opacity)) {
			style.imageOpacity=mapGlyph.getImage().opacity;
			style.strokeColor="transparent";
		    }
		    obj.style = style;
		}
		list.push(obj);
		let geom = mapGlyph.getGeometry();
		if(geom) {
		    obj.geometryType=geom.CLASS_NAME;
		    let points = obj.points=[];
		    let vertices  = geom.getVertices();
		    let  p =d=>{
			return Utils.trimDecimals(d,6);
		    };
		    if(mapGlyph.getImage()) {
			let b = this.map.transformProjBounds(geom.getBounds());
			points.push(p(b.top),p(b.left),
				    p(b.top),p(b.right),
				    p(b.bottom),p(b.right),
				    p(b.bottom),p(b.left));				    
		    } else {
			vertices.forEach(vertex=>{
			    let pt = vertex.clone().transform(this.map.sourceProjection, this.map.displayProjection);
			    points.push(p(pt.y),p(pt.x));
			});
		    }
		}
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
	    if(mapJson.list || Array.isArray(mapJson)) {
		this.loadOldJson(mapJson,map);
		return
	    }
	    let glyphs = mapJson.glyphs||[];
	    glyphs.forEach(jsonObject=>{
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
		    return;
		}
		let style = $.extend({},glyphType.getStyle());
		if(jsonObject.style) $.extend(style,jsonObject.style);
		style = $.extend({},style);
		if(Utils.stringDefined(style.popupText)) {
		    style.cursor = 'pointer';
		} else {
		    style.cursor = 'auto';
		}
		if(glyphType.isData()) {
		    let mapGlyph = new MapGlyph(this,mapOptions.type, mapOptions);
		    mapGlyph.addMapData(mapOptions.displayAttrs,false);
		    this.addGlyph(mapGlyph,true);
		    return
		}

		if(glyphType.isMultiEntry()) {
		    let mapGlyph = new MapGlyph(this,mapOptions.type, mapOptions, null,style);
		    this.addGlyph(mapGlyph);
		    mapGlyph.addEntries();
		    return
		}  
		if(glyphType.isFixed()) {
		    let mapGlyph = new MapGlyph(this,mapOptions.type, mapOptions, null,style);
		    this.addGlyph(mapGlyph);
		    mapGlyph.addFixed();
		    return

		}

		if(glyphType.isMap()) {
		    let mapGlyph = new MapGlyph(this,mapOptions.type, mapOptions, null,style);
		    mapGlyph.checkMapLayer(false);
		    this.addGlyph(mapGlyph);
		    return
		}  
		let points=jsonObject.points;
		if(!points || points.length==0) {
		    console.log("Unknown glyph:" + mapOptions.type);
		    return;
		}

		let feature = this.makeFeature(map,jsonObject.geometryType, style, points);
		if(feature) {
		    feature.style = style;
		    this.addFeatures([feature]);
		    let mapGlyph = new MapGlyph(this,mapOptions.type, mapOptions, feature,style);
		    mapGlyph.checkImage(feature);
		    this.addGlyph(mapGlyph);
		} else {
		    console.log("Couldn't make feature:" + mapOptions.type);
		}
	    });
	},

	loadOldJson: function(mapJson,map) {
	    let list = mapJson.list||mapJson;
	    list.forEach(mapGlyph=>{
		if(!mapGlyph.points) {
		    console.log("No points defined:" + JSON.stringify(mapGlyph));
		    return;
		}
		let mapOptions = mapGlyph.mapOptions;
		if(!mapOptions) {
		    mapOptions = {
			type:mapGlyph.type
		    }
		}

		let type = mapGlyph.type||mapOptions.type;
		let glyph = this.getGlyphType(type);
		if(!glyph) {
		    return;
		}

		let style = $.extend({},glyph?glyph.getStyle():{});
		if(mapGlyph.style) $.extend(style,mapGlyph.style);
		style = $.extend({},style);
		if(style.label) {
		    style.pointRadius=0
		}
		if(Utils.stringDefined(style.popupText)) {
		    style.cursor = 'pointer';
		} else {
		    style.cursor = 'auto';
		}
		if(!style.fillColor) style.fillColor = "transparent";
		let points=mapGlyph.points;
		if(points && points.length>0) {
		    let oldWay = Utils.isDefined(points[0].latitude);
		    if(oldWay) {
			let tmp = [];
			for(let i=0;i<points.length;i++) {
			    tmp.push(points[i].latitude,points[i].longitude);
			}
			points = tmp;
		    }
		}

		if(mapOptions.type==GLYPH_MAP) {
		    let mapGlyph = new MapGlyph(this,mapOptions.type, mapOptions, null,style);
		    mapGlyph.checkMapLayer();
		    this.addGlyph(mapGlyph);
		    return
		}  
		let feature = this.makeFeature(map,mapGlyph.geometryType, style, points);
		if(feature) {
		    feature.style = style;
		    this.addFeatures([feature]);
		    let mapGlyph = new MapGlyph(this,mapOptions.type, mapOptions, feature,style);
		    mapGlyph.checkImage(feature);
		    this.addGlyph(mapGlyph);
		}
	    });
	},

	makeFeature:function(map,geometryType, style, points) {
	    if(points.length>2) {
		let latLons = [];
		for(let i=0;i<points.length;i+=2) {
		    latLons.push(new OpenLayers.Geometry.Point(points[i+1],points[i]));
		}
		if(geometryType=="OpenLayers.Geometry.Polygon") {
		    map.transformPoints(latLons);
		    let linearRing = new OpenLayers.Geometry.LinearRing(latLons);
		    let geom = new OpenLayers.Geometry.Polygon(linearRing);
		    return new OpenLayers.Feature.Vector(geom,null,style);
		} else {
		    return  map.createPolygon("","",latLons,style,null,geometryType=="OpenLayers.Geometry.LineString");
		}
	    } 
	    let point =  MapUtils.createLonLat(points[1], points[0]);
	    return  map.createPoint("",point,style);
	},
	doMapProperties:function() {
	    if(!this.mapProperties)this.mapProperties={};
	    let html = HU.formTable();
	    html+=HU.formEntry("",HU.checkbox(this.domId("showlegend"),[],
					      Utils.isDefined(this.mapProperties.showLegend)?this.mapProperties.showLegend:this.getShowLegend(),'Show Legend'));
	    html+="</table>"
	    html+=HU.div(['style',HU.css('text-align','center','padding-bottom','8px','margin-bottom','8px')], HU.div([ID,this.domId(ID_OK), CLASS,"display-button"], "OK") + SPACE2 +
			 HU.div([ID,this.domId(ID_CANCEL), CLASS,"display-button"], "Cancel"));
	    html+=HU.b("Top Wiki Text:") +"<br>" +
		HU.textarea("",this.mapProperties.topWikiText??"",['id',this.domId('topwikitext_input'),'rows','6','cols','80']);


	    html  = HU.div(['style','margin:5px;'],html);
	    let anchor = this.jq(ID_MENU_FILE);
	    let dialog = HU.makeDialog({content:html,title:'Properties',header:true,
					my:"left top",at:"left bottom",draggable:true,anchor:anchor});

	    let close=()=>{
		dialog.hide();
		dialog.remove();
	    }
	    this.jq(ID_OK).button().click(()=>{
		this.mapProperties.showLegend = this.jq("showlegend").is(':checked');
		this.mapProperties.topWikiText = this.jq("topwikitext_input").val();
		this.checkTopWiki();
		this.makeLegend();
		close();
	    });
	    this.jq(ID_CANCEL).button().click(()=>{
		close();
	    });

	},

	showFileMenu: function(button) {
	    let _this = this;
	    let html ="";
	    if(this.getProperty("canEdit")) {	    
		html +=this.menuItem(this.domId(ID_SAVE),"Save",'S');
	    }
	    html+= this.menuItem(this.domId(ID_DOWNLOAD),"Download")
	    html+= this.menuItem(this.domId(ID_PROPERTIES),"Set Default Style...");
	    html+= this.menuItem(this.domId(ID_CMD_LIST),"List Features...",'L');
	    html+= this.menuItem(this.domId(ID_CLEAR),"Clear Commands","Esc");
	    html+= this.menuItem(this.domId(ID_MAP_PROPERTIES),"Properties...");
	    html+= HU.href(ramaddaBaseUrl+'/userguide/imdv.html','Help',['target','_help']);
	    html  = this.makeMenu(html);
	    this.dialog = HU.makeDialog({content:html,anchor:button});

	    this.jq(ID_NAVIGATE).click(function() {
		HtmlUtils.hidePopupObject();
		_this.setCommand(null);
	    });
	    this.jq(ID_MAP_PROPERTIES).click(function(){
		_this.doMapProperties();
	    });
	    this.jq(ID_SAVE).click(function(){
		_this.clearCommands();
		_this.doSave();
	    });
	    this.jq(ID_SAVEAS).click(function(){
		_this.clearCommands();
		_this.doSaveAs();
	    });	    
	    this.jq(ID_DOWNLOAD).click(function(){
		_this.clearCommands();
		_this.doDownload();
	    });	    
	    this.jq(ID_PROPERTIES).click(function(){
		_this.clearCommands();
		_this.doProperties();
	    });
	    this.jq(ID_CLEAR).click(function(){
		_this.clearCommands();
		_this.unselectAll();
	    });	    
	    this.jq(ID_CMD_LIST).click(function(){
		_this.clearCommands();
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
	    let _this = this;
	    if(!this.getShowMapLegend()) return;
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
	featureChanged:function() {
	    this.featureHasBeenChanged = true;
	    this.showMapLegend();
	    this.makeLegend();
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
		    HtmlUtils.hidePopupObject();
		    _this.setCommand(g.type);
		});
	    });

	},


	showEditMenu: function(button) {
	    let html = [[ID_CUT,"Cut",'X'],[ID_COPY,"Copy",'C'],[ID_PASTE,"Paste",'V'],null,
			[ID_SELECTOR,"Select"],
			[ID_SELECT_ALL,"Select All",'A'],			
			null,
			[ID_MOVER,"Move",'M'],
			[ID_RESHAPE,"Reshape"],
			[ID_RESIZE,"Resize"],
			[ID_ROTATE,"Rotate"],			
			null,
			[ID_TOFRONT,"To Front","F"],
			[ID_TOBACK,"To Back","B"],
			null,
			[ID_EDIT,"Edit Properties",'P']].reduce((prev,tuple)=>{
		 prev = prev || "";
		 if(!tuple) return prev+ HU.div([CLASS,"ramadda-menu-divider"]);						
		 return prev + 	this.menuItem(this.domId(tuple[0]),tuple[1],tuple[2]);
	     },"");
	    
	    this.dialog = HU.makeDialog({content:this.makeMenu(html),anchor:button});
	    this.jq(ID_CUT).click(()=>{
		HtmlUtils.hidePopupObject();
		this.doCut();
	    });
	    this.jq(ID_SELECT_ALL).click(()=>{
		HtmlUtils.hidePopupObject();
		this.selectAll();
	    });
	    
	    this.jq(ID_COPY).click(()=>{
		HtmlUtils.hidePopupObject();
		this.doCopy();
	    });	    
	    this.jq(ID_PASTE).click(()=>{
		HtmlUtils.hidePopupObject();
		this.doPaste();
	    });
	    this.jq(ID_TOFRONT).click(()=>{
		HtmlUtils.hidePopupObject();
		this.changeOrder(true);
	    });
	    this.jq(ID_TOBACK).click(()=>{
		HtmlUtils.hidePopupObject();
		this.changeOrder(false);
	    });
	    this.jq(ID_EDIT).click(()=>{
		HtmlUtils.hidePopupObject();
		this.handleEditEvent();
	    });	    
	    [ID_SELECTOR,ID_MOVER,ID_RESHAPE,ID_RESIZE,ID_ROTATE].forEach(command=>{
		this.jq(command).click(()=>{
		    HtmlUtils.hidePopupObject();
		    this.setCommand(command);
		});
	    });
	},
	
	checkSelected:function(mapGlyph) {
	    if(this.isFeatureSelected(mapGlyph)) {
		this.unselectGlyph(mapGlyph);
		this.selectGlyph(mapGlyph);
	    }
	},
	unselectGlyph:function(mapGlyph) {
	    if(!mapGlyph) return;
	    mapGlyph.unselect();
	},
	isFeatureSelected:function(mapGlyph) {
	    return mapGlyph.selectDots!=null;
	},
	toggleSelectGlyph:function(mapGlyph) {
	    if(this.isFeatureSelected(mapGlyph)) {
		this.unselectGlyph(mapGlyph);
	    } else {
		this.selectGlyph(mapGlyph);
	    }
	},
	selectGlyph:function(mapGlyph,maxPoints,dontRedraw) {
	    if(!Utils.isDefined(maxPoints)) maxPoints = 20;
	    if(this.isFeatureSelected(mapGlyph)) {
		return;
	    }
	    mapGlyph.selectDots = [];
	    let pointCount = 0;
	    let mapLayer = mapGlyph.getMapLayer();
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
		mapLayer.redraw();
	    }	    


	    let image = mapGlyph.getImage();
	    if(image) {
		let ext = image.extent;
		[[ext.left,ext.top],[ext.right,ext.top],[ext.left,ext.bottom],[ext.right,ext.bottom]].forEach(tuple=>{
                    let pt = new OpenLayers.Geometry.Point(tuple[0],tuple[1]);
		    let dot = new OpenLayers.Feature.Vector(pt,null,this.DOT_STYLE);	
		    mapGlyph.selectDots.push(dot);
		});

	    }


	    pointCount+=this.selectFeatures(mapGlyph,mapGlyph.getFeatures(),maxPoints);
	    this.selectionLayer.addFeatures(mapGlyph.selectDots,{silent:true});
	    if(!dontRedraw) {
		this.selectionLayer.redraw();
	    }
	    return pointCount;
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
                pt = new OpenLayers.Geometry.Point(pt.x,pt.y);
		let dot = new OpenLayers.Feature.Vector(pt,null,this.DOT_STYLE);
		mapGlyph.selectDots.push(dot);
		pointCount++;
	    });
	    return pointCount;
	},

	getSelected: function() {
	    let selected=[];
	    this.getGlyphs().forEach(mapGlyph=>{
		if(mapGlyph.selectDots) {
		    return selected.push(mapGlyph);
		}
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
	createMapLayer:function(opts,style,andZoom) {
	    let url = ramaddaBaseUrl +"/entry/get?entryid="+opts.entryId;
	    let selectCallback = null;
	    let unSelectCallback = null;	    
	    let errorCallback = (url,err)=>{
		this.handleError(err,url);
	    };

	    let loadCallback = (map,layer)=>{
		if(layer.mapGlyph)layer.mapGlyph.applyMapStyle();
	    }
	    switch(opts.entryType) {
	    case 'latlonimage': 
		let w = 2048;
		let h = 1024;
		return this.getMap().addImageLayer(opts.entryId, opts.name,"",url,true,
						   opts.north, opts.west,opts.south,opts.east, w,h);
	    case 'geo_gpx': 
		return this.getMap().addGpxLayer(opts.name,url,true, selectCallback, unSelectCallback,style,loadCallback,andZoom,errorCallback);
		break;
	    case 'geo_geojson': 
		return this.getMap().addGeoJsonLayer(opts.name,url,true, selectCallback, unSelectCallback,style,loadCallback,andZoom,errorCallback);
		break;		
	    case 'geo_shapefile_fips': 
	    case 'geo_shapefile': 
		url = ramaddaBaseUrl+'/entry/show?entryid=' + opts.entryId+'&output=shapefile.kml&formap=true';
		//fall thru to kml
	    case 'geo_kml': 
		loadCallback = (map,layer)=>{
		    if(layer.features) {layer.features.forEach(f=>{f.style = style;});}
		    layer.redraw();
		};
		let layer =  this.getMap().addKMLLayer(opts.name,url,true, selectCallback, unSelectCallback,style,loadCallback,andZoom,errorCallback);

		return layer;
	    default:
		this.handleError('Unknown map type:' + opts.entryType);
		return null;
	    }
	},

	showProgress:function(msg) {
	    this.clearMessage2();
	    this.getMap().setProgress(HU.div([ATTR_CLASS, "display-map-message"], msg));
	    this.getMap().showLoadingImage();
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
					bounds = new OpenLayers.Bounds();
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
			if(zoomLevel>=0 && Utils.isDefined(zoomLevel)) {
			    _this.getMap().getMap().zoomTo(zoomLevel);
			}
			if(bounds) {
			    _this.map.getMap().setCenter(bounds.getCenterLonLat());
			}
			try {
			    this.loadAnnotationJson(json,_this.map,_this.myLayer);
			} catch(err) {
			    this.handleError(err);
			}
			this.featureHasBeenChanged = false;
			this.checkTopWiki();
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
	    new GlyphType(this,GLYPH_MARKER,"Marker",
			  {strokeWidth:0, 
			   fillColor:"transparent",
			   externalGraphic: externalGraphic,
			   pointRadius:this.getPointRadius(10)},
			  OpenLayers.Handler.MyPoint,
			  {icon:ramaddaBaseUrl+"/map/marker-blue.png"});
	    new GlyphType(this,GLYPH_POINT,"Point",
			  {strokeWidth:this.getProperty("strokeWidth",2), 
			   strokeDashstyle:'solid',
			   fillColor:"transparent",
			   fillOpacity:1,
			   strokeColor:this.getStrokeColor(),
			   strokeOpacity:1,
			   pointRadius:this.getPointRadius(4)},
			  OpenLayers.Handler.MyPoint,
			  {icon:ramaddaBaseUrl+"/icons/dot.png"});
	    new GlyphType(this,GLYPH_LABEL,"Label",
			  {label : "label",
			   externalGraphic: externalGraphic,
			   pointRadius:this.getPointRadius(10),
			   fontColor: this.getProperty("labelFontColor","#000"),
			   fontSize: this.getFontSize(),
			   fontFamily: this.getFontFamily(),
			   fontWeight: this.getFontWeight(),
			   fontStyle: this.getFontStyle(),
			   labelAlign: this.getProperty("labelAlign","lb"),
			   labelXOffset: this.getProperty("labelXOffset","0"),
			   labelYOffset: this.getProperty("labelYOffset","0"),
			   labelOutlineColor:this.getProperty("labelOutlineColor","#fff"),
			   labelOutlineWidth: this.getProperty("labelOutlineWidth","0"),
			   //			      pointRadius:0,
			   strokeWidth:0,
			   fillColor:'transparent',
			   labelSelect:true,
			  }, OpenLayers.Handler.MyPoint,
			  {icon:ramaddaBaseUrl+"/icons/text.png"});
	    new GlyphType(this,GLYPH_FIXED,"Fixed Text",
			  {
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
			  OpenLayers.Handler.MyEntryPoint,
			  {isFixed:true, tooltip:'Add fixed text',
			   icon:ramaddaBaseUrl+"/icons/sticky-note-text.png"});			    

	    new GlyphType(this,GLYPH_LINE, "Line",
			  {strokeColor:this.getStrokeColor(),
			   strokeWidth:this.getStrokeWidth(),
			   strokeDashstyle:'solid',
			   strokeOpacity:1,
			  },
			  OpenLayers.Handler.MyPath,
			  {maxVertices:2,
			   icon:ramaddaBaseUrl+"/icons/line.png"});		
	    new GlyphType(this,GLYPH_POLYLINE, "Polyline",
			  {strokeColor:this.getStrokeColor(),
			   strokeWidth:this.getStrokeWidth(),
			   strokeDashstyle:'solid',			      
			   strokeOpacity:1},
			  OpenLayers.Handler.MyPath,
			  {icon:ramaddaBaseUrl+"/icons/polyline.png"});
	    new GlyphType(this,GLYPH_POLYGON, "Polygon",
			  {strokeColor:this.getStrokeColor(),
			   strokeWidth:this.getStrokeWidth(),
			   strokeDashstyle:'solid',			      
			   strokeOpacity:1,
			   fillColor:"transparent",
			   fillOpacity:1.0},
			  OpenLayers.Handler.MyPolygon,
			  {icon:ramaddaBaseUrl+"/icons/polygon.png"});

	    new GlyphType(this,GLYPH_FREEHAND,"Freehand",
			  {strokeColor:this.getStrokeColor(),
			   strokeWidth:this.getStrokeWidth(),
			   strokeDashstyle:'solid',
			   strokeOpacity:1},
			  OpenLayers.Handler.MyPath,
			  {freehand:true,icon:ramaddaBaseUrl+"/icons/freehand.png"});
	    new GlyphType(this,GLYPH_FREEHAND_CLOSED,"Closed",
			  {strokeColor:this.getStrokeColor(),
			   strokeWidth:this.getStrokeWidth(),
			   strokeDashstyle:'solid',
			   strokeOpacity:1,
			   fillColor:"transparent",
			   fillOpacity:1.0},
			  OpenLayers.Handler.MyPolygon,
			  {freehand:true,icon:ramaddaBaseUrl+"/icons/freehandclosed.png"});

	    new GlyphType(this,GLYPH_BOX, "Box",
			  {strokeColor:this.getStrokeColor(),
			   strokeWidth:this.getStrokeWidth(),
			   strokeDashstyle:'solid',
			   strokeOpacity:1,
			   fillColor:"transparent",
			   fillOpacity:1.0},
			  OpenLayers.Handler.MyRegularPolygon,
			  {snapAngle:90,sides:4,irregular:true,
			   icon:ramaddaBaseUrl+"/icons/rectangle.png"});
	    new GlyphType(this,GLYPH_CIRCLE, "Circle",
			  {strokeColor:this.getStrokeColor(),
			   strokeWidth:this.getStrokeWidth(),
			   strokeDashstyle:'solid',
			   strokeOpacity:1,
			   fillColor:"transparent",
			   fillOpacity:1.0},
			  OpenLayers.Handler.MyRegularPolygon,
			  {snapAngle:45,sides:40,icon:ramaddaBaseUrl+"/icons/ellipse.png"});

	    new GlyphType(this,GLYPH_TRIANGLE, "Triangle",
			  {strokeColor:this.getStrokeColor(),
			   strokeWidth:this.getStrokeWidth(),
			   strokeDashstyle:'solid',
			   strokeOpacity:1,
			   fillColor:"transparent",
			   fillOpacity:1.0},
			  OpenLayers.Handler.MyRegularPolygon,
			  {snapAngle:10,sides:3,
			   icon:ramaddaBaseUrl+"/icons/triangle.png"});				
	    new GlyphType(this,GLYPH_HEXAGON, "Hexagon",
			  {strokeColor:this.getStrokeColor(),
			   strokeWidth:this.getStrokeWidth(),
			   strokeDashstyle:'solid',
			   strokeOpacity:1,
			   fillColor:"transparent",
			   fillOpacity:1.0},
			  OpenLayers.Handler.MyRegularPolygon,
			  {snapAngle:90,sides:6,
			   icon:ramaddaBaseUrl+"/icons/hexagon.png"});		


	    new GlyphType(this,GLYPH_MAP,"Map",
			  {strokeColor:this.getStrokeColor(),
			   strokeWidth:this.getStrokeWidth(),
			   strokeDashstyle:'solid',			      
			   strokeOpacity:1,
			   fillColor:"transparent",
			   fillOpacity:1.0},
			  OpenLayers.Handler.MyEntryPoint,
			  {isMap:true,
			   tooltip:"Select a gpx, geojson or  shapefile map",
			   icon:ramaddaBaseUrl+"/icons/map.png"});	

	    if(this.getProperty("hereRoutingEnabled")||this.getProperty("googleRoutingEnabled")) {
		new GlyphType(this,GLYPH_ROUTE, "Route", {
		    strokeColor:this.getStrokeColor(),
		    strokeWidth:this.getStrokeWidth(),
		    strokeDashstyle:'solid',
		    strokeOpacity:1,
		},OpenLayers.Handler.MyRoute,{icon:ramaddaBaseUrl+"/icons/route.png"});
	    }
	    new GlyphType(this,GLYPH_IMAGE, "Image",
			  {strokeColor:"#ccc",
			   strokeWidth:1,
			   imageOpacity:this.getImageOpacity(1),
			   fillColor:"transparent"},
			  OpenLayers.Handler.ImageHandler,
			  {tooltip:"Select an image entry to display",
			   snapAngle:90,sides:4,irregular:true,isImage:true,
			   icon:ramaddaBaseUrl+"/icons/imageicon.png"}
			 );
	    new GlyphType(this,GLYPH_ENTRY,"Entry Marker",
			  {externalGraphic: ramaddaBaseUrl +"/icons/video.png",
			   pointRadius:12},
			  OpenLayers.Handler.MyEntryPoint,
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
			   labelAlign: "cb",
			   labelXOffset: 0,
			   labelYOffset: 15},
			  OpenLayers.Handler.MyEntryPoint,
			  {tooltip:"Display children entries of selected entry",
			   isMultiEntry:true,
			   icon:ramaddaBaseUrl+"/icons/sitemap.png"});

	    new GlyphType(this,GLYPH_DATA,"Data",
			  {},
			  OpenLayers.Handler.MyEntryPoint,
			  {isData:true, tooltip:'Select a map data entry to display',
			   icon:ramaddaBaseUrl+"/icons/chart.png"});


	},
	showCommandMessage:function(msg)  {
	    this.jq(ID_MESSAGE).html(msg);
	    this.jq(ID_MESSAGE).show();
	},
	showMessage:function(msg)  {
	    this.setMessage(msg)
	    if(this.messageErase) clearTimeout(this.messageErase);
	    this.messageErase = setTimeout(()=>{
//		this.jq(ID_MESSAGE).hide();
		this.setMessage("");
	    },3000);
	},
	getCurrentLevel: function() {
	    return this.getMap().getMap().getZoom();
	},
	checkVisible:function() {
	    this.getGlyphs().forEach(mapGlyph=>{
		mapGlyph.checkVisible();
	    });
	},
	initMap: function(map) {
	    SUPER.initMap.call(this)
	},
	makeLegend: function() {
	    let _this = this;
	    if(Utils.isDefined(this?.mapProperties?.showLegend) &&
	       !this.mapProperties.showLegend) {
		this.jq(ID_LEGEND).hide();
		return;
	    }
	    if(!this.getShowLegend()) {
		this.jq(ID_LEGEND).hide();
		return;
	    }
	    this.jq(ID_LEGEND).show();
	    let showShapes = this.getShowLegendShapes();
	    let html = "";
	    let idToGlyph={};
	    this.getGlyphs().forEach((mapGlyph,idx)=>{
		if(!showShapes && mapGlyph.isShape()) {
		    return;
		}
		let label =  mapGlyph.getLabel(true,true,idx==0?'imdv-legend-item-first':'');
		let body = HU.div(['style','margin-left:10px;'],mapGlyph.getLegendBody());
		let block = HU.toggleBlockNew("",body,mapGlyph.getLegendVisible(),
					      {separate:true,headerStyle:'display:inline-block;',
					       extraAttributes:['map-glyph-id',mapGlyph.getId()]});		
		idToGlyph[mapGlyph.getId()] = mapGlyph;
		html+=HU.table(['width','100%','cellpadding','0','cellspacing','0'],
			       HU.tr([],
				     HU.td(['width','5%','style','padding-right:1px;'],block.header) +
				     HU.td([],label)));				     
		html+=block.body;
	    });
	    if(html!="") {
		html  = HU.div(['class','imdv-legend'],html);
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
		mapGlyph.zoomTo();
	    });
	    

	    this.getGlyphs().forEach((mapGlyph,idx)=>{
		mapGlyph.initLegendBody();
	    });


	    let items = this.jq(ID_LEGEND).find('.imdv-legend-item');
	    this.initGlyphButtons(this.jq(ID_LEGEND));
	    items.each(function() {
		$(this).attr('title','Click to toggle visibility');
	    });

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
		let id = $(this).attr('glyphid');
		let mapGlyph = _this.findGlyph(id);
		if(!mapGlyph) return;
		if(event.shiftKey) {
		    _this.toggleSelectGlyph(mapGlyph);
		    return
		}
		if(event.altKey || event.metaKey) {
		    mapGlyph.zoomTo();
		    return;
		}
		mapGlyph.setVisible(!mapGlyph.getVisible(),true);
		if(mapGlyph.getVisible()) 
		    $(this).removeClass('imdv-legend-item-invisible');
		else
		    $(this).addClass('imdv-legend-item-invisible');			    
	    });
	},
	wikify:function(wiki,entryId,wikiCallback,wikiError) {
	    wikiError = wikiError || (error=>{this.handleError(error);});
	    let url = ramaddaBaseUrl + "/wikify";
	    $.post(url,{
		doImports:"false",
		entryid:entryId??this.getProperty("entryId"),
		text:wiki},
		   wikiCallback).fail(wikiError);
	},

	checkTopWiki:function() {
	    if(!Utils.isDefined(this.mapProperties?.topWikiText)) return;
	    if(!Utils.stringDefined(this.mapProperties.topWikiText)) {
		this.jq("topwikitext").html('');
	    } else {
		this.wikify(this.mapProperties.topWikiText,null,wiki=>{
		    this.jq("topwikitext").html(wiki);
		});
	    }
	},
	canEdit: function() {
	    return this.getProperty("canEdit") || this.getShowMenuBar(false);
	},

        initDisplay: function(embedded) {
	    let _this = this;
	    SUPER.initDisplay.call(this)
	    let legend = HU.div(['id',this.domId(ID_LEGEND)]);
	    this.jq(ID_LEFT).html(legend);
	    
	    this.myLayer = this.map.createFeatureLayer("Annotation Features",false,null,{rendererOptions: {zIndexing: true}});
	    this.selectionLayer = this.map.createFeatureLayer("Selection",false,null,{rendererOptions: {zIndexing: true}});	    
	    this.selectionLayer.setZIndex(1000)
	    this.myLayer.setZIndex(1001)	    
	    this.selectionLayer.canSelect = false;
	    if(this.getProperty("layerIndex")) {
		this.myLayer.ramaddaLayerIndex = +this.getProperty("layerIndex");
	    }
	    this.myLayer.ramaddaLayerIndex = 1001;
	    this.icon = "/icons/map/marker-blue.png";
	    this.glyphTypes=[];
	    this.glyphTypeMap = {};
	    this.doMakeMapGlyphs();
	    if(embedded) {
		return;
	    }

	    setTimeout(()=>{
		this.getMap().getMap().events.register("zoomend", "", () =>{
		    this.checkVisible();
		},true);
	    },500);
	    this.getMap().featureClickHandler = e=>{
		let debug = false;
		let feature = e.feature;
		if(debug)
		    console.log("featureClick:" + feature);
		if(!feature) return;
		let mapGlyph = feature.mapGlyph || (feature.layer?feature.layer.mapGlyph:null);
		if(!mapGlyph) {
 		    if(debug)console.log("\tno mapGlyph");
		    return true;
		}
		if(mapGlyph.isMap()) {
 		    if(debug)console.log("\tis map");
		    return true;
		}
		if(this.command==ID_EDIT) {
 		    if(debug)console.log("\tdoing edit");
		    this.doEdit(feature.layer.mapGlyph);
		    return false;
		}

		if(this.command!=null) {
 		    if(debug)console.log("\tdoing command:" + this.command);
		    return false;
		}
		let showPopup = (html,props)=>{
		    this.getMap().lastClickTime  = new Date().getTime();
		    let id = HU.getUniqueId("div");
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
		}

		let doPopup = (html,props)=>{
 		    if(debug)console.log("\tdoPopup:"+ html)
		    let js =[];
		    //Parse out any script tags 
		    let regexp = /<script *src=("|')?([^ "']+)("|')?.*?<\/script>/g;
		    let array = [...html.matchAll(regexp)];
		    array.forEach(tuple=>{
			html = html.replace(tuple[0],"");
			let url = tuple[2];
			url = url.replace(/'/g,"");
			js.push(url);
		    });


		    //Run through any script tags and load them
		    //once done show the popup
		    let cb = ()=>{
			if(js.length==0 && js[0]==null) {
 			    if(debug)console.log("\tshowPopup:"+ html)
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
		if(mapGlyph.isEntry() || mapGlyph.isMultiEntry() || text.startsWith("<wiki>")) {
 		    if(debug)console.log("\twikifying")
		    let wiki = text.startsWith("<wiki>")?text:mapGlyph.getWikiText();
		    if(!Utils.stringDefined(wiki))
			wiki = "+section title={{name}}\n{{simple}}\n-section";

		    let wikiCallback = html=>{
			html = mapGlyph.convertPopupText(html);
			html = HU.div(['style','max-height:300px;overflow-y:auto;'],html);
			doPopup(html,{width:"600",height:"400"});
		    };
		    this.wikify(wiki,feature.entryId ?? mapGlyph.getEntryId(),wikiCallback);
		    return false;
		}

		if(!Utils.stringDefined(text)) {
 		    if(debug)console.log("\tno text")
		    return false;
		}
		text = mapGlyph.convertPopupText(text).replace(/\n/g,"<br>");
		doPopup(text);
		return false;
	    };

	    let control;
	    this.getMap().xxxfeatureOverHandler = e=>{
		e.feature.style.old_strokeColor=e.feature.style.strokeColor;
		e.feature.style.strokeColor="green";		
		e.feature.layer.redraw(e.feature);
	    };
	    this.getMap().xxxfeatureOutHandler = e=>{
		e.feature.style.strokeColor=e.feature.style.old_strokeColor;
		e.feature.layer.redraw(e.feature);
	    };	    

	    if(this.canEdit()) {
//		this.jq(ID_LEFT).html(HU.div([ID,this.domId(ID_COMMANDS),CLASS,"imdv-commands"]));
		var keyboardControl = new OpenLayers.Control();
		control = new OpenLayers.Control();
		let callbacks = {
		    keydown: function(event) {
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
		this.map.getMap().addControl(keyboardControl);
		this.addControl(ID_SELECTOR,"Click-drag to select",this.featureSelector = new OpenLayers.Control.SelectFeature(this.myLayer, {
		    select: function(feature) {
			if(this.isShiftKey() && _this.isFeatureSelected(feature.mapGlyph)) {
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
			if(!this.feature.image && this.feature.type!=GLYPH_BOX) {
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
			var pos = this.map.getLonLatFromViewPortPx(pixel);
			var geom = vertex.geometry;
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

		let menuBar=  "";
		[[ID_MENU_FILE,"File"],[ID_MENU_EDIT,"Edit"],[ID_MENU_NEW,"New"]].forEach(t=>{
		    menuBar+=   HU.div([ID,this.domId(t[0]),CLASS,"ramadda-menubar-button"],t[1])});
	    	menuBar = HU.div([CLASS,"ramadda-menubar"], menuBar);
		let message2 = HU.div([ID,this.domId(ID_MESSAGE2),CLASS,"ramadda-imdv-message2"],"");
		this.jq(ID_MAP_CONTAINER).append(message2);
		let message3 = HU.div([ID,this.domId(ID_MESSAGE3),CLASS,"ramadda-imdv-message3"],"");		
		if(this.getShowMapLegend()) {
		    this.jq(ID_MAP_CONTAINER).append(message3);
		}

		let message = HU.div([ID,this.domId(ID_MESSAGE),STYLE,HU.css("display","inline-block","white-space","nowrap","margin-left","10px")],"");
		let mapHeader = HU.div([STYLE,HU.css("margin-left","10px","xdisplay","inline-block"), ID,this.domId(ID_MAP)+"_header"]);
		menuBar= HU.table(['width','100%'],HU.tr(["valign","bottom"],HU.td(['xwidth','50%'],menuBar) +
							 HU.td(['width','50%'], message) +
						 HU.td(['align','right','style','padding-right:10px;','width','50%'],mapHeader)));
		let showMenuBar = this.getShowMenuBar(null);
		if(!Utils.isDefined(showMenuBar))
		    showMenuBar = this.canEdit();

		this.jq(ID_HEADER0).append(HU.div([ID,this.domId("topwikitext")]));

		if(showMenuBar) {
		    this.jq(ID_TOP_LEFT).append(menuBar);
		}
		this.jq(ID_MENU_NEW).click(function() {
		    _this.showNewMenu($(this));
		});
		this.jq(ID_MENU_FILE).click(function() {
		    _this.showFileMenu($(this));
		});
		this.jq(ID_MENU_EDIT).click(function() {
		    _this.showEditMenu($(this));
		});

		$(window).bind('beforeunload', ()=>{
		    if(this.getProperty("canEdit") && this.featureHasBeenChanged) {
			return 'Changes have been made. Are you sure you want to leave?';
		    }
		});
	    } else {
		let menuBar=HU.div([STYLE,HU.css("display","inline-block"), ID,this.domId(ID_MAP)+"_header"]);
		this.jq(ID_TOP_LEFT).append(HU.center(menuBar));
	    }

	    let cmds = "";
	    this.glyphTypes.forEach(g=>{
		this.glyphTypeMap[g.type]  = g;
		g.createDrawer();
	    });

	    this.jq(ID_COMMANDS).html(cmds);
	    this.jq(ID_MAP).mouseover(function(){
		$(this).focus();
	    });
	    
	    Utils.initDragAndDrop(this.jq(ID_MAP),
				  event=>{},
				  event=>{},
				  (event,item,result) =>{
				      let entryId = this.getProperty("entryId") || this.entryId;
				      Ramadda.handleDropEvent(event, item, result, entryId,(data,entryid, name,isImage)=>{
					  this.setCommand('image',{url:data.geturl});
				      });
				  },
				  "image.*");



	    if(this.getProperty("thisEntryType")=="geo_editable_json" || this.getProperty("thisEntryType")=="geo_imdv") {
		this.loadMap();
	    }
        },
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
    isMap:  function() {
	return this.options.isMap;
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
		let defaultStyle = OpenLayers.Util.extend({}, OpenLayers.Feature.Vector.style["default"]);
		defaultStyle={};
		$.extend(defaultStyle, _this.getStyle());		    
		let styleMap = new OpenLayers.StyleMap({"default":defaultStyle});
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
			    newStyle[a] = _this.getStyle()[s];
			}
		    }
		    if(feature.style && feature.style.label)
			newStyle.label = feature.style.label;
		    let tmp = {};
		    $.extend(tmp, newStyle);
		    feature.style=tmp;
		}
		this.layer.redraw();
		_this.newFeature(feature);
	    }
	});
	this.drawer = new Drawer(layer);
	this.display.addControl(this.type,"",this.drawer);
	return this.drawer;
    },
};



function MapGlyph(display,type,attrs,feature,style) {
    let glyphType = display.getGlyphType(type);
    if(attrs.routeProvider)
	this.name = "Route: " + attrs.routeProvider +" - " + attrs.routeType;
    else 
	this.name = attrs.name || glyphType.getName() || type;
    this.display = display;
    this.type = type;
    this.features = [];
    this.attrs = attrs;
    this.style = style??{};
    this.id = attrs.id ?? HU.getUniqueId("glyph_");
    if(feature) this.addFeature(feature);
}

MapGlyph.prototype = {
    domId:function(id) {
	return this.getId() +'_'+this.display.domId(id);
    },
    jq:function(id) {
	return jqid(this.domId(id));
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
    isMultiEntry:  function() {
	return this.type == GLYPH_MULTIENTRY;
    },
    showMultiEntries:function() {
	let _this = this;
	if(!this.entries) return;
	let html = "";
	let map = {};
	this.entries.forEach(entry=>{
	    map[entry.getId()] = entry;
	    let link = entry.getLink(null,true,['target','_entry']);
	    link = HU.div(['style','white-space:nowrap;max-width:200px;overflow-x:hidden;','title',entry.getName()], link);
	    if(MAP_TYPES.includes(entry.getType().getId())) {
		link = HU.leftRightTable(link,HU.span(['class','ramadda-clickable','title','add map','entryid',entry.getId(),'command',GLYPH_MAP],HU.getIconImage('fas fa-plus')));
	    } else if(entry.isPoint) {
		link = HU.leftRightTable(link,HU.span(['class','ramadda-clickable','title','add data','entryid',entry.getId(),'command',GLYPH_DATA],HU.getIconImage('fas fa-plus')));
	    } else if(entry.isGroup) {
		link = HU.leftRightTable(link,HU.span(['class','ramadda-clickable','title','add multi entry','entryid',entry.getId(),'command',GLYPH_MULTIENTRY],HU.getIconImage('fas fa-plus')));
	    } else {
	    }		
	    html+=HU.div([],link);
	});
	html = HU.div(['style','auto;max-height:200px;overflow-y:auto;'], HU.div(['style','margin-right:10px;'],html));
	this.jq('multientry').html(HU.b("Entries")+html);
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
		_this.display.createMapData(mapOptions);
	    } else if(command==GLYPH_MULTIENTRY) {
		let mapGlyph = _this.display.handleNewFeature(null,style,mapOptions);
		mapGlyph.addEntries(true);
	    }
	});
    },
    changeOrder:function(toFront) {
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
    addFeature: function(feature) {
	this.features.push(feature);
	feature.mapGlyph = this;
    },
    getStyle: function() {
	return this.style;
    },
    zoomTo: function() {
	if(this.features.length) {
	    this.display.getMap().centerOnFeatures(this.features);
	    return;
	}
	if(this.mapLayer) {
	    this.display.getMap().zoomToLayer(this.mapLayer);
	    return
	}
	if(this.displayInfo?.display) {
	    if(this.displayInfo.display.myFeatureLayer && (
		!Utils.isDefined(this.displayInfo.display.layerVisible) ||
		    this.displayInfo.display.layerVisible)) {
		    
		this.display.getMap().zoomToLayer(this.displayInfo.display.myFeatureLayer);
		return
	    }
	    if(this.displayInfo.display.pointBounds) {
		this.display.getMap().zoomToExtent(this.display.getMap().transformLLBounds(this.displayInfo.display.pointBounds));
		return;
	    }
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
    getLabel:function(forLegend,addDecorator,extraClass) {
	let name = this.getName();
	let label;
	if(Utils.stringDefined(name)) {
	    if(!forLegend)
		label= this.getType()+": "+name;
	    else
		label = name;
	} else if(this.isFixed()) {
	    label = this.style.text;
	    if(label && label.length>15) label = label.substring(0,14)+"..."
	} else {
	    label =  this.getType();
	}
	let url = null;
	if(this.attrs.entryId) {
	    url = RamaddaUtils.getEntryUrl(this.attrs.entryId);
	    if(!forLegend)
		label = HU.href(url, label,['target','_entry','title','View Entry']);
	}
	let glyphType = this.display.getGlyphType(this.getType());

//	label = HU.span(,label);
	let right = "";
	if(addDecorator) {
	    right+=this.getDecoration(true);
	    if(GLYPH_TYPES_LINES.includes(this.getType())) {
//		right+=this.display.getDecoration(this.style);
	    }
	}

	if(glyphType) {
	    let icon = this.attrs.icon??this.style.externalGraphic??glyphType.getIcon();
	    if(icon && icon.endsWith("blank.gif")) icon = glyphType.getIcon();
	    icon = HU.image(icon,['width','18px']);
	    if(url && forLegend)
		icon = HU.href(url,icon,['target','_entry']);
	    if(forLegend) {
		right+=SPACE+
		    HU.span([CLASS,"ramadda-clickable imdv-legend-item-view",
			     'glyphid',this.getId(),
			     TITLE,"Zoom to",],

			    HU.getIconImage("fas fa-binoculars",[],LEGEND_IMAGE_ATTRS));
	    }

	    label = icon +" " + label;
	}
	if(forLegend) {
	    label = HU.div(['style',HU.css('margin-right','5px','max-width','200px','overflow-x','hidden','white-space','nowrap')], label);
	}
	if(right!="") {
	    right= HU.span(['style',HU.css('white-space','nowrap')], right);
	    label=HU.leftRightTable(label,right);
	}
	if(forLegend) {
	    let clazz = 'ramadda-clickable imdv-legend-item';
	    if(extraClass) clazz+=' ' + extraClass;
	    if(!this.getVisible()) 
		clazz+=' imdv-legend-item-invisible ';
	    label = HU.div(['class','ramadda-clickable ' + clazz,'glyphid',this.getId()],label);
	}
	return label;
    },
    initLegendBody:function() {
	if(this.isMultiEntry() && this.entries) {
	    this.showMultiEntries();
	}
	this.makeFeatureFilters();
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
    getLegendBody:function() {
	let body = '';
	body+=HU.center(this.display.makeGlyphButtons(this,true));
	let item = i=>{
	    if(Utils.stringDefined(i)) {
		body+=HU.div(['class','imdv-legend-body-item'], i);
	    }
	};

	let colorMap = this.attrs.colorBy ??{};
	if(Utils.stringDefined(colorMap.property)) {
	    let div = this.getColorTableDisplay(colorMap.colorTable,colorMap.min,colorMap.max,true);
	    body+=HU.center(colorMap.property)+HU.center(div);
	}	

	//Put the placeholder here for map sliders
	body+=HU.div(['id',this.domId('mapfilters')]);

	if(this.type==GLYPH_LABEL && this.style.label) {
	    item(this.style.label.replace(/\"/g,"\\"));
	}
	if(this.isFixed()) {
	    item(this.style.text.replace(/\"/g,"\\"));
	}
	item(this.display.getDistances(this.getGeometry(),this.getType()));
	if(this.getPopupText())  {
	    let text = this.getPopupText();
	    if(text.startsWith("<wiki>")) {
	    } else {
		item(text.replace(/\n/g,"<br>"));
	    }
	}
	if(this.isMultiEntry()) {
	    item(HU.div(['id',this.domId('multientry')]));
	}

	if(Utils.stringDefined(this.style.imageUrl)) {
	    item("<img style='border:1px solid #ccc;' width='150px' src='" +this.style.imageUrl+"'>");
	}
	return body;
    },
    convertPopupText:function(text) {
	if(this.getImage()) {
	    text = text.replace(/\${image}/g,HU.image(this.style.imageUrl,['width','200px']));
	}
	return text;
    },
		    
	
    isEntry:function() {
	return this.getType() ==GLYPH_ENTRY;
    },
    isMap:function() {
	return this.getType()==GLYPH_MAP;
    },
    isMultiEntry:function() {
	return this.getType()==GLYPH_MULTIENTRY;
    },
    getAttributes: function() {
	return this.attrs;
    },
    setMapLayer:function(mapLayer) {
	this.mapLayer = mapLayer;
	mapLayer.mapGlyph = this;
    },
    getMapLayer: function() {
	return this.mapLayer;
    },
    checkMapLayer:function(andZoom) {
	//Only create the map if we're visible
	if(!this.isMap() || !this.isVisible()) return;
	if(this.mapLayer==null) {
	    if(!Utils.isDefined(andZoom)) andZoom = true;
	    this.setMapLayer(this.display.createMapLayer(this.attrs,this.style,andZoom));
	    this.applyMapStyle();
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
	if(!this.canDoMapStyle()) return;
	let colorBy = {
	    property:this.jq('colorby_property').val(),
	    min:this.jq('colorby_min').val(),
	    max:this.jq('colorby_max').val(),
	    colorTable:this.jq('colorby_colortable').val()};		
	this.attrs.colorBy = colorBy;
	let rules = [];
	for(let i=0;i<20;i++) {
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
    getColorTableDisplay:function(id,min,max,showRange) {
	let ct = Utils.ColorTables[id];
	if(!ct) {
	    return "----";
	}
        let display = Utils.getColorTableDisplay(ct,  min??0, max??1, {
	    showRange: false,
            height: "20px",
	    showRange:showRange
        });
        return  HtmlUtils.div([STYLE,HU.css('width','200px'),TITLE,a,"X"+CLASS, "ramadda-colortable-select","colortable",a],display);
    },
    initPropertiesComponent: function(dialog) {
	let _this = this;
	let colorMap = this.attrs.colorBy ??{};
	let decorate = () =>{
	    let div = this.getColorTableDisplay(this.jq('colorby_colortable').val());
	    this.jq('colorby_colortable_label').html(div);
	};
	decorate();
	dialog.find(".ramadda-colortable-select").click(function() {
	    let ct = $(this).attr("colortable");
	    _this.jq('colorby_colortable').val(ct);
	    decorate();
	});
	Utils.displayAllColorTables(this.display.domId('colorby'));
	dialog.find('#'+this.domId('colorby_property')).change(function() {
	    let prop =  $(this).val();
	    _this.featureInfo.every(info=>{
		if(info.property==prop) {
		    _this.jq('colorby_min').val(info.min);
		    _this.jq('colorby_max').val(info.max);		    
		    return false;
		}
		return true;
	    });
	});

	dialog.find('[mapproperty_index]').change(function() {
	    let info = _this.featureInfoMap[$(this).val()];
	    if(!info) return;
	    let index  = $(this).attr('mapproperty_index');	    
	    let tt = "";
	    let value = jqid('mapvalue_' + index).val();
	    let wrapper = jqid('mapvaluewrapper_' + index);
	    if(info.isNumeric) {
		wrapper.html(HU.input("",value,['id','mapvalue_' + index,'size','15']));
		tt=info.min +" - " + info.max;
	    }  else  if(info.samples.length) {
		tt = Utils.join(info.samples, ", ");
		if(info.isEnum) {
		    wrapper.html(HU.select("",['id','mapvalue_' + index],info.samples,value));
		} else {
		    wrapper.html(HU.input("",value,['id','mapvalue_' + index,'size','15']));
		}
	    }
	    jqid('mapvalue_' + index).attr('title',tt);
	});


    },

    getPropertiesComponent: function(content) {
	if(!this.canDoMapStyle()) return;
	let attrs = this.mapLayer.features[0].attributes;
	let featureInfo = this.featureInfo = this.getFeatureInfo();
	let keys  = Object.keys(featureInfo);
	let colorMap = this.attrs.colorBy ??{};
	let numeric = featureInfo.filter(info=>{return info.isNumeric;});
	let enums = featureInfo.filter(info=>{return info.isEnum;});
	let colorBy = "";
	if(numeric.length) {
	    let numericProperties=Utils.mergeLists([['','Select']],numeric.map(info=>{return info.property;}));
	    colorBy=HU.b("Color map")+"<br>";
	    colorBy += HU.formTable();
	    colorBy += HU.formEntry("Property:", HU.select('',['id',this.domId('colorby_property')],numericProperties,colorMap.property));
	    colorBy += HU.formEntry("Range:",HU.input("",colorMap.min??"", ['id',this.domId('colorby_min'),'size','6','title','min value']) +" -- "+
				    HU.input("",colorMap.max??"", ['id',this.domId('colorby_max'),'size','6','title','max value']));
	    colorBy += HU.hidden('',colorMap.colorTable||'blues',['id',this.domId('colorby_colortable')]);
	    colorBy+=HU.formEntry("Color table:", HU.hbox([HU.div(['style',HU.css(),'id',this.domId('colorby_colortable_label')]),
							   Utils.getColorTablePopup(null,null,"Select")]));
	    colorBy+="</table>";
	}
	let properties=Utils.mergeLists([['','Select']],featureInfo.map(info=>{return info.property;}));
	let ex = "";
	featureInfo.forEach(info=>{
	    let seen ={};
	    let list =[];
	    if(info.isNumeric) {
		ex+=HU.b(info.property)+": " +  info.min +" - " + info.max+"<br>";
	    } else if(info.isEnum) {
		ex+=HU.b(info.property)+": " +  Utils.join(info.samples,", ") +"<br>";
	    } else {
		ex+=HU.b(info.property)+": " +  Utils.join(info.samples,", ") +"<br>";
	    }
	});
	let c = OpenLayers.Filter.Comparison;
	let operators = [c.EQUAL_TO,c.NOT_EQUAL_TO,c.LESS_THAN,c.GREATER_THAN,c.LESS_THAN_OR_EQUAL_TO,c.GREATER_THAN_OR_EQUAL_TO,[c.BETWEEN,'between'],[c.LIKE,'like'],[c.IS_NULL,'is null']];
	let table = HU.b("Style Rules")+"<br>";
	table+=HU.formTable();
	let sample = "Samples&#013;";
	for(a in attrs) {
	    let v = attrs[a]?String(attrs[a]):'';
	    v = v.replace(/"/g,"").replace(/\n/g," ");
	    sample+=a+"=" + v+"&#013;";
	}
	table+=HU.tr([],HU.tds(['style','font-weight:bold;'],['Property','Operator','Value','Style']));
	let rules = this.getMapStyleRules();
	let styleTitle = 'e.g.:&#013;fillColor:red&#013;fillOpacity:0.5&#013;strokeColor:blue&#013;strokeWidth:1&#013;';
	for(let index=0;index<20;index++) {
	    let rule = index<rules.length?rules[index]:{};
	    let value = rule.value??"";
	    let info = this.featureInfoMap[properties,rule.property];
	    let title = sample;
	    let valueInput;
	    if(info) {
		if(info.isNumeric)
		    title=info.min +" - " + info.max;
		else if(info.samples.length)
		    title = Utils.join(info.samples, ", ");
	    }
	    if(info?.isEnum) {
		valueInput = HU.select("",['id','mapvalue_' + index],info.samples,value); 
	    } else {
		valueInput = HU.input("",value,['id','mapvalue_' + index,'size','15']);
	    }
	    let propSelect =HU.select('',['id','mapproperty_' + index,'mapproperty_index',index],properties,rule.property);
	    let opSelect =HU.select('',['id','maptype_' + index],operators,rule.type);	    
	    valueInput =HU.span(['id','mapvaluewrapper_' + index],valueInput);
	    let s = Utils.stringDefined(rule.style)?rule.style:'';
	    let styleInput = HU.textarea("",s,['id','mapstyle_' + index,'rows','3','cols','10','title',styleTitle]);
	    table+=HU.tr(['valign','top'],HU.tds([],[propSelect,opSelect,valueInput,styleInput]));
	}
	table += "</table>";
	table = HU.div(['style','max-height:200px;overflow-y:auto;'], table);
	content.push(["Map Style Rules",colorBy+table]);
	content.push(["Sample Values",ex]);
    },
    getMapStyleRules: function() {
	return this.attrs.mapStyleRules??[];
    },
    getFeatureKeys:function() {
	if(this.mapLayer.features.length==0) return [];
	let attrs = this.mapLayer.features[0].attributes;
	return  Object.keys(attrs).filter(key=>{
	    if(key=="OBJECTID" || key=="objectid" || key=="ORIGINAL_OID") return false;
	    return true;
	});
    },
    getFeatureInfo:function() {
	let keyInfo = [];
	if(!this.mapLayer?.features) return keyInfo; 
	let features= this.mapLayer.features;
	let keys  = this.getFeatureKeys();
	keys.forEach(key=>{
	    keyInfo.push({
		property:key,
		min:Number.MAX_VALUE,
		max:Number.MIN_VALUE,
		isNumeric:false,
		isInt:true,
		isString:false,
		isEnum:false,
		seen:{},
		samples:[]
	    });		
	});
	features.forEach(f=>{
	    keyInfo.forEach(info=>{
		let v = f.attributes[info.property];
		if(!Utils.isDefined(v)) return;
		let sv = String(v);
		if(sv.trim()=="") return;
		if(isNaN(v) || info.samples.length>0) {
		    if(info.samples.length<30) {
			info.isEnum = true;
			if(!info.seen[v]) {
			    info.seen[v] = true;
			    info.samples.push(v);
			}
		    } else {
			info.isEnum = false;
			info.isString = true;
		    }
		}
		if(!isNaN(v)) {
		    info.isNumeric=true;
		    if(Math.round(v)!=v) {
			info.isInt=false;
		    }
		    info.min = Math.min(info.min,v);
		    info.max = Math.max(info.max,v);			
		}
	    });
	});
	this.featureInfo = keyInfo;
	this.featureInfoMap = {};
	keyInfo.forEach(info=>{
	    this.featureInfoMap[info.property] = info;
	});
	return keyInfo;
    },
    makeFeatureFilters:function() {
	let _this = this;
	let featureInfo = this.getFeatureInfo();
	let sliders = "";
	let strings = "";
	let enums = "";
	let filters = this.attrs.featureFilters = this.attrs.featureFilters ??{};
	featureInfo.forEach(info=>{
	    let filter = filters[info.property] = filters[info.property]??{};
	    let id = Utils.makeId(info.property);
	    if(info.isString)  {
		filter.type="string";
		strings+=HU.b(info.property)+":<br>" +
		    HU.input("",filter.stringValue??"",['filter-property',info.property,'class','imdv-filter-string','id',this.domId('string_'+ id),'size',20]) +"<br>";
		return
	    } 
	    if(info.samples.length)  {
		filter.type="enum";
		if(info.samples.length>1) 
		    enums+=HU.b(info.property)+":<br>" +
		    HU.select("",['filter-property',info.property,'class','imdv-filter-enum','id',this.domId('enum_'+ id),'multiple','multiple','rows',Math.min(info.samples.length,5)],info.samples,filter.enumValues,50)+"<br>";
		return;
	    }


	    if(info.isNumeric) {
		filter.minValue = info.min;
		filter.maxValue = info.max;		
		filter.type="range";
		sliders+=HU.b(info.property)+":<br>" +
		    HU.leftRightTable(HU.div(['id',this.domId('slider_min_'+ id),'style','max-width:50px;overflow-x:auto;'],Utils.formatNumber(filter.min??info.min)),
				      HU.div(['id',this.domId('slider_max_'+ id),'style','max-width:50px;overflow-x:auto;'],Utils.formatNumber(filter.max??info.max))) +
		    HU.div(['slider-min',info.min,'slider-max',info.max,'slider-isint',info.isInt,
			    'slider-value-min',filter.min??info.min,'slider-value-max',filter.max??info.max,
			    'filter-property',info.property,'class','imdv-filter-slider',
			    STYLE,HU.css("display","inline-block","width","100%")],"")+"<br>";			    
	    }
	});

	if(sliders!="")
	    sliders = HU.div(['style',HU.css('margin-left','10px','margin-right','20px')],sliders);
	let widgets = enums+sliders+strings;
	if(widgets!="") {
	    let update = () =>{
		this.display.featureHasBeenChanged = true;
		this.applyMapStyle(true);
	    };
	    let clearAll = HU.div(['class','ramadda-clickable','title','Clear Filters','id',this.domId('filters_clearall')],HU.getIconImage('fas fa-trash-can',null,LEGEND_IMAGE_ATTRS));
	    
	    let toggle = HU.toggleBlockNew("Filters",widgets,this.getFiltersVisible(),{separate:true,headerStyle:'display:inline-block;',callback:null});
	    this.jq('mapfilters').html(HU.div(['style','margin-right:10px;'],HU.leftRightTable(toggle.header,clearAll))+toggle.body);
	    HU.initToggleBlock(this.jq('mapfilters'),(id,visible)=>{this.setFiltersVisible(visible);});
	    this.jq('filters_clearall').click(()=>{
		this.display.featureChanged();
		this.attrs.featureFilters = {};
		this.applyMapStyle();
	    });
	    this.jq('mapfilters').find('.imdv-filter-string').keypress(function(event) {
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
	    this.jq('mapfilters').find('.imdv-filter-enum').change(function(event) {
		let key = $(this).attr('filter-property');
		let filter = filters[key]??{};
		    filter.property = key;
		filter.type='enum';
		filter.enumValues=$(this).val();
		update();
	    });

	    this.jq('mapfilters').find('.imdv-filter-slider').each(function() {
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
		$(this).slider({		
		    min: +min,
		    max: +max,
		    step:+step,
		    values:[$(this).attr('slider-value-min'),$(this).attr('slider-value-max')],
		    slide: function( event, ui ) {
			let key = $(this).attr('filter-property');
			let id = Utils.makeId(key);
			let filter = filters[key]??{};
			filter.min = +ui.values[0];
			filter.max = +ui.values[1];			    
			filter.property=key;
			_this.jq('slider_min_'+ id).html(Utils.formatNumber(filter.min));
			_this.jq('slider_max_'+ id).html(Utils.formatNumber(filter.max));			    

			if(!_this.sliderThrottle) 
			    _this.sliderThrottle=Utils.throttle(()=>{
				update();
			    },1000);
			_this.sliderThrottle();
		    }});
	    });
	}
    },
    applyMapStyle:function(skipLegendUI) {
	let _this = this;
	//If its a map then set the style on the map features
	if(!this.mapLayer) return;
	let features= this.mapLayer.features;
	if(!skipLegendUI && this.canDoMapStyle()) {
	    this.makeFeatureFilters();
	}

	let style = this.style;
	let rules = this.getMapStyleRules();
	if(rules) {
	    rules = rules.filter(rule=>{
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


	
	let attrs = features.length>0?features[0].attributes:{};
	let keys  = Object.keys(attrs);
	if(rules && rules.length>0) {
	    this.mapLayer.style = null;
	    this.mapLayer.styleMap = this.display.getMap().getVectorLayerStyleMap(this.mapLayer, style,rules);
	    features.forEach((f,idx)=>{f.style = null;});
	} else if(Utils.stringDefined(this.attrs.colorBy?.property)) {
	    let prop =this.attrs.colorBy.property;
	    let min =+this.attrs.colorBy.min;
	    let max =+this.attrs.colorBy.max;
	    let range = max-min;
	    let ct =Utils.getColorTable(this.attrs.colorBy.colorTable,true);
	    features.forEach((f,idx)=>{
		let value = f.attributes[prop];
		if(!Utils.isDefined(value)) {
		    return;
		}
		value = +value;
		let percent = (value-min)/range;
		let index = Math.max(0,Math.min(ct.length-1,Math.round(percent*ct.length)));
		f.style = $.extend({},style);
		f.style.fillColor=ct[index];
	    });
	} else {
	    this.mapLayer.style = style;
	    features.forEach((f,idx)=>{
		f.style = $.extend({},style);
		f.originalStyle = $.extend({},style);			    
	    });
	}

	features.forEach((f,idx)=>{
	    let visible = true;
	    rangeFilters.every(filter=>{
		let v = f.attributes[filter.property];
		if(Utils.isDefined(v)) {
		    visible = v>=filter.min && v<=filter.max;
		}
		return visible;
	    });
	    if(visible) {
		stringFilters.every(filter=>{
		    let v = f.attributes[filter.property];
		    if(Utils.isDefined(v)) {
			v= String(v).toLowerCase();
			visible = v.indexOf(filter.stringValue)>=0;
		    }
		    return visible;
		});
	    }
	    if(visible) {
		enumFilters.every(filter=>{
		    let v = f.attributes[filter.property];
		    visible =filter.enumValues.includes(v);
		    return visible;
		});
	    }		


	    if(!visible) {
		if(!f.style) f.style={};
		f.style.display='none';
	    }
	});
	this.mapLayer.redraw();
    },

   applyStyle:function(style) {
       this.style = style;
       this.applyMapStyle();

	this.features.forEach(feature=>{
	    if(feature.style) {
		$.extend(feature.style,style);
	    }
	});	    
	if(this.isFixed()) {
	    this.addFixed();
	}
	this.display.featureChanged();

    },
    move:function(dx,dy) {
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
	    this.image=  this.getMap().addImageLayer("","","",this.style.imageUrl,true,  b.top,b.left,b.bottom,b.right);
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
	this.addMapData(attrs,false);
    },
    isVisible: function() {
	return this.attrs.visible??true;
    },
    getVisibleLevelRange:function() {
	return this.attrs.visibleLevelRange;
    },
    setVisible:function(visible,callCheck) {
	this.attrs.visible = visible;
	if(callCheck)
	    this.checkVisible();
	this.checkMapLayer();
    },
    getVisible:function() {
	if(!Utils.isDefined(this.attrs.visible)) this.attrs.visible = true;
	return this.attrs.visible;
    },
    setVisibleLevelRange:function(min,max) {
	let range = this.getVisibleLevelRange;
	let oldMin = range?.min;
	let oldMax = range?.max;	
	if(min===oldMin && max===oldMax) return;
	if(min=="") min = null;
	if(max=="") max = null;	
	this.attrs.visibleLevelRange = {min:min,max:max};
	this.checkVisible();
    },    
    checkVisible: function() {
	let range = this.getVisibleLevelRange()??{};
	let visible=true;
	let level = this.display.getCurrentLevel();
	let min = Utils.isDefined(range.min)?+range.min:-1;
	let max = Utils.isDefined(range.max)?+range.max:10000;
	visible =  this.getVisible() && (level>=min && level<=max);
//	console.log("current level:" +level,"min:" + min,max,visible);
	this.features.forEach(feature=>{
	    if(!feature.style) feature.style = {};
	    if(visible) {
		feature.style.display = 'inline';
	    }  else {
		feature.style.display = 'none';
	    }
	    
	    $.extend(feature.style,{display:feature.style.display});
	});
	if(this.isFixed()) {
	    if(visible)
		jqid(this.getId()).show();
	    else
		jqid(this.getId()).hide();
	    

	}
	if(this.getMapLayer()) {
	    this.getMapLayer().setVisibility(visible);
	}
	if(this.selectDots) {
	    this.selectDots.forEach(dot=>{
		dot.style.display = visible?'inline':'none';
	    });
	    this.display.selectionLayer.redraw();
	}
	if(this.image) {
	    this.image.setVisibility(visible);
	}	
	if(this.displayInfo && this.displayInfo.display) {
	    this.displayInfo.display.setVisible(visible);
	}
	this.display.myLayer.redraw();
    },    
    isShape:function() {
	if(this.getType()==GLYPH_LABEL) {
	    if(!Utils.stringDefined(this.style.externalGraphic)) return true;
	    if(this.style.externalGraphicb && this.style.externalGraphic.endsWith("blank.gif")) return true;
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
	if(this.fixedComponent) this.fixedComponent.remove();
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
	    if(Utils.stringDefined(style[d])) css+=HU.css(d,style[d]);
	});
	jqid(this.getId()).remove();
	let text = this.style.text??"";
	let html = HU.div(['id',this.getId(),CLASS,"ramadda-imdv-fixed",'style',css],"");
	this.display.jq(ID_MAP_CONTAINER).append(html);
	let toggleLabel = null;
	if(text.startsWith("toggle:")) {
	    let match = text.match(/toggle:(.*)\n/);
	    if(match) {
		toggleLabel=match[1];
		text = text.replace(/toggle:(.*)\n/,"").trim();
	    }
	}
	if(text.startsWith("<wiki>")) {
	    this.display.wikify(text,null,wiki=>{
		if(toggleLabel)
		    wiki = HU.toggleBlock(toggleLabel+SPACE2, wiki,false);
		wiki = HU.div(['style','max-height:300px;overflow-y:auto;'],wiki);
		jqid(this.getId()).html(wiki)});
	} else {
	    text = text.replace(/\n/g,"<br>");
	    if(toggleLabel)
		text = HU.toggleBlock(toggleLabel+SPACE2, text,false);
	    jqid(this.getId()).html(text);
	}
    },

    addMapData:function(displayAttrs,andZoom) {
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
	if(!Utils.isDefined(displayAttrs))  displayAttrs={};
	$.extend(attrs,displayAttrs);
	let display = this.display.getDisplayManager().createDisplay("map",attrs);
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
	let dim = small?'12px':'25px';
	css.push('width',small?'20px':'50px');
	let line = "solid";
	if(style.strokeWidth>0) {
	    if(style.strokeDashstyle) {
		if(['dot','dashdot'].includes(style.strokeDashstyle)) {
		    line = "dotted";
		} else  if(style.strokeDashstyle.indexOf("dash")>=0) {
		    line = "dashed";
		}
	    }
	    css.push('border',(small?Math.min(+style.strokeWidth,2):style.strokeWidth)+"px " + line +" " + style.strokeColor);
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
	return "";
    },
    isClosed: function() {
	return GLYPH_TYPES_CLOSED.includes(this.type);
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
	    entries.forEach(e=>{
		if(!e.hasLocation()) return;
		let  pt = new OpenLayers.Geometry.Point(e.getLongitude(),e.getLatitude());
		pt = this.display.getMap().transformLLPoint(pt);
		let style = $.extend({},this.style);
		style.externalGraphic = e.getIconUrl();
		style.strokeWidth=1;
		style.strokeColor="red";
		let bgstyle = $.extend({},style);
		bgstyle = $.extend(bgstyle,{externalGraphic:ramaddaBaseUrl+"/images/white.png"});
		let bgpt = new OpenLayers.Geometry.Point(pt.x,pt.y);
		let bg = new OpenLayers.Feature.Vector(bgpt,null,bgstyle);
		if(this.style.showLabels) {
		    let label  =e.getName();
		    let toks = Utils.split(label," ",true,true);
		    if(toks.length>1) {
			label = Utils.join(toks,"\n");
		    }
		    style.label=label;
		} else {
		    style.label=null;
		}

		let marker = new OpenLayers.Feature.Vector(pt,null,style);
		bg.noSelect = true;
//		bg.mapGlyph=this;
//		bg.entryId = e.getId();

		marker.mapGlyph = this;
		marker.entryId = e.getId();
		this.features.push(bg);
		this.features.push(marker);

	    });
	    this.display.addFeatures(this.features);
	    if(andZoom)
		this.zoomTo();
	    this.showMultiEntries();
	};
	entry.getChildrenEntries(callback);
    },
    unselect:function() {
	if(this.selectDots) {
	    this.display.selectionLayer.removeFeatures(this.selectDots);
	    this.selectDots= null;
	}
	if(this.mapLayer) {
	    this.mapLayer.features.forEach(f=>{
		f.style = f.originalStyle;
	    });
	    this.mapLayer.redraw();
	}
    },
	
    doRemove:function() {
	if(this.isFixed()) {
	    jqid(this.getId()).remove();
	}
	if(this.features) {
	    this.display.removeFeatures(this.features);
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
	}
	if(this.displayInfo) {
	    jqid(this.displayInfo.divId).remove();
	    jqid(this.displayInfo.bottomDivId).remove();			
	    if(this.displayInfo.display) {
		this.displayInfo.display.deleteDisplay();
	    }
	}
    }
    

}



function RamaddaEditablemapDisplay(displayManager, id, properties) {
    const SUPER = new RamaddaImdvDisplay(displayManager,  id,  properties);
    RamaddaUtil.inherit(this,SUPER);
}

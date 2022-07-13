/**
   Copyright 2008-2021 Geode Systems LLC
*/


const DISPLAY_EDITABLEMAP = "editablemap";
addGlobalDisplayType({
    type: DISPLAY_EDITABLEMAP,
    label: "Editable Map",
    category:CATEGORY_MAPS,
    tooltip: makeDisplayTooltip("Editable map"),        
});


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
var GLYPH_IMAGE = "image";
var GLYPH_ENTRY = "entry";
var GLYPH_MAP = "map";


function RamaddaEditablemapDisplay(displayManager, id, properties) {
//    import { decode} from ramaddaBaseUrl+'/lib/here.js';


    let _this = this;
    OpenLayers.Handler.ImageHandler = OpenLayers.Class(OpenLayers.Handler.RegularPolygon, {
	initialize: function(control, callbacks, options) {
	    OpenLayers.Handler.RegularPolygon.prototype.initialize.apply(this,arguments);
	    this.display = options.display;
	},
	finalize: function() {
	    this.theImage = this.image;
	    this.image =null;
	    this.display.featureChanged();	    
	    if(this.imageBounds) {
		let aspect = this.imageBounds.width/this.imageBounds.height;
		let ring = this.feature.geometry.components[0];
		let pts = ring.components;
		let nw = pts[0], ne=pts[1],se=pts[2],sw=pts[3];
		let w = ne.x-nw.x;
		let h = nw.y-sw.y;		
		se.x = ne.x = nw.x+aspect*(nw.y-sw.y);
		w = ne.x-nw.x;
		h = nw.y-sw.y;		
	    }

	    OpenLayers.Handler.RegularPolygon.prototype.finalize.apply(this,arguments);

	    //call deactivate in a bit. If we do this now then there is an error in OL
	    setTimeout(()=>{
		this.display.clearCommands();
		//A bit of a hack but we want to change the style of the newly created image
		let image = this.display.myLayer.features[this.display.myLayer.features.length-1];
		image.style.strokeColor = "transparent";
		this.display.myLayer.redraw(image);
	    },500);
	},
	move: function(evt) {
	    if(!this.checkingImageSize) {
		this.checkingImageSize = true;
		const img = new Image();
		let _this = this;
		img.onload = function() {
		    _this.imageBounds={width:this.width,height:this.height};
		}
		img.src = this.style.imageUrl;		
	    }
	    OpenLayers.Handler.RegularPolygon.prototype.move.apply(this,arguments);
	    let mapBounds = this.feature.geometry.getBounds();
	    if(this.image) {
		this.display.getMap().removeLayer(this.image);
	    }
	    let ring = this.feature.geometry.components[0];
	    let pts = ring.components;
	    let nw = pts[0], ne=pts[1],se=pts[2],sw=pts[3];
	    let w = ne.x-nw.x;
	    let h = nw.y-sw.y;		

	    let b = this.display.map.transformProjBounds(mapBounds);
	    if(this.imageBounds) {
		let aspect = this.imageBounds.width/this.imageBounds.height;
		if(!evt.shiftKey)
		    b.right = aspect*(b.top-b.bottom) + b.left 
	    }
	    this.lastBounds = b;
	    this.image=  this.display.map.addImageLayer("","","",this.style.imageUrl,true,  b.top,b.left,b.bottom,b.right);
	    this.image.setOpacity(this.style.imageOpacity);
	}
	
    });


    OpenLayers.Handler.MyPoint = OpenLayers.Class(OpenLayers.Handler.Point, {
	finalize: function() {
	    OpenLayers.Handler.Point.prototype.finalize.apply(this,arguments);
	    this.display.featureChanged();	    
	},
    });


    OpenLayers.Handler.MyEntryPoint = OpenLayers.Class(OpenLayers.Handler.Point, {
	finalize: function() {
	    OpenLayers.Handler.Point.prototype.finalize.apply(this,arguments);
	    this.display.featureChanged();
	    this.display.clearCommands();	    	    
	},
    });
    

    OpenLayers.Handler.MyPath = OpenLayers.Class(OpenLayers.Handler.Path, {
	finalize: function() {
	    OpenLayers.Handler.Path.prototype.finalize.apply(this,arguments);
	    if(this.finishedWithRoute) return;
	    //A hack to get the line that was just drawn
	    let line =  this.display.myLayer.features[this.display.myLayer.features.length-1];
	    if(this.glyphType!=	GLYPH_ROUTE || !line || !line.geometry) {
		this.display.featureChanged();	    
		this.display.jq(ID_MESSAGE2).hide(1000);
		return;
	    }
	    line.type = GLYPH_POLYLINE;
	    this.display.jq(ID_MESSAGE2).html("Creating route...");
	    let pts = this.display.getLatLonPoints(line.geometry);
	    let xys = [];
	    pts.forEach(pt=>{
		xys.push(Utils.trimDecimals(pt.y,6));
		xys.push(Utils.trimDecimals(pt.x,6));
	    });
	    let args = {
		mode:this.display.routeType??"car",
		points:Utils.join(xys,",")
	    };
	    let url = ramaddaBaseUrl+"/map/getroute";
	    this.finishedWithRoute = true;

	    $.post(url, args,data=>{
		this.display.removeFeatures([line]);
		if(data.error) {
		    alert("Error:" + data.error);
		    this.finishedWithRoute = false;
		    this.display.jq(ID_MESSAGE2).hide();
		    return;
		}
		if(!data.routes || data.routes.length==0) {
		    alert("No routes found");
		    this.finishedWithRoute = false;
		    this.display.jq(ID_MESSAGE2).hide();
		    return;
		}
		let points = [];
		data.routes[0].sections.forEach(section=>{
		    let decoded = hereDecode(section.polyline);
		    decoded.polyline.forEach(pair=>{
			points.push(new OpenLayers.Geometry.Point(pair[1],pair[0]));
		    });
		});
		let  route = this.display.getMap().createPolygon("", "", points, {
		    strokeWidth:4
		},null,true);
		route.style = $.extend({},this.style);
		route.type=GLYPH_ROUTE;
		this.display.addFeatures([route]);
		this.display.clearCommands();
		this.display.showDistances(route.geometry,GLYPH_ROUTE,true);
	    }).fail(err=>{
		this.display.removeFeatures([line]);
		this.display.clearCommands();
		alert("Error:" + err);
	    });
	},
	move: function(evt) {
	    OpenLayers.Handler.Path.prototype.move.apply(this,arguments);
	    this.display.showDistances(this.line.geometry,this.glyphType);
	}
	
    });

    OpenLayers.Handler.MyRegularPolygon = OpenLayers.Class(OpenLayers.Handler.RegularPolygon, {
	finalize: function() {
	    OpenLayers.Handler.RegularPolygon.prototype.finalize.apply(this,arguments);
	    this.display.featureChanged();	    
	    this.display.jq(ID_MESSAGE2).hide(1000);
	},
	move: function(evt) {
	    OpenLayers.Handler.RegularPolygon.prototype.move.apply(this,arguments);
	    if(!this.feature || !this.feature.geometry) return;
	    this.display.showDistances(this.feature.geometry,this.glyphType);
	}
    });    
    



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
    const ID_DELETE_ALL = "deleteall";
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

    if(!Utils.isDefined(properties.showOpacitySlider)) properties.showOpacitySlider=true; 

    const SUPER = new RamaddaBaseMapDisplay(displayManager,  id, DISPLAY_EDITABLEMAP,  properties);
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
    ];
    
    displayDefineMembers(this, myProps, {
	commands: [],
        myLayer: [],
	glyphs:{},
	selected:{},
	getMap: function() {
	    return this.map;
	},
	handleEvent:function(event,lonlat) {
	    return;
	},
	showDistances:function(geometry, glyphType,fadeOut) {
	    let msg = this.getDistances(geometry,glyphType);
	    this.jq(ID_MESSAGE2).html(msg);
	    this.jq(ID_MESSAGE2).show();
	    if(fadeOut) {
		setTimeout(()=>{
		    this.jq(ID_MESSAGE2).hide(1000);
		},2000);
	    }
	},

	getLatLonPoints:function(geometry) {
	    let components = geometry.components;
	    if(components==null) return null;
	    if(components.length) {
		if(components[0].components) components = components[0].components;
	    }
	    let pts = components.map(pt=>{
		return  this.map.transformProjPoint(pt)
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

	    if(glyphType == GLYPH_CIRCLE || glyphType == GLYPH_BOX || glyphType == GLYPH_POLYLINE || glyphType == GLYPH_TRIANGLE || glyphType == GLYPH_FREEHAND || glyphType==GLYPH_IMAGE) {
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
		}
		msg+=   "<br>" +
		    "Area: " + Utils.formatNumber(area) +" sq " + unit + "<br>" +
		    Utils.formatNumber(acres) +" acres";
	    }

	    return msg;
	},


	setCommand:function(command, args) {
	    args = args ||{};
	    this.clearCommands();
	    this.command = command;
	    let glyph = this.glyphMap[command];
	    this.commands.forEach(cmd=>{
		cmd.deactivate();
	    });
	    if(!command) return;
	    this.jq("new_" + command).addClass("ramadda-display-editablemap-command-active");
	    this.commands.every(cmd=>{
		if(cmd.name != command) {
		    return true;
		}
		if(glyph) {
		    let styleMap = new OpenLayers.StyleMap({"default":{}});
		    let tmpStyle = {};
		    $.extend(tmpStyle,glyph.getStyle());
		    tmpStyle.mapOptions = {
			type:glyph.type
		    }
		    if(glyph.isImage() || glyph.isEntry() || glyph.isMap()) {
			//imageUrl may be a url or it may be the entry attributes
			let callback = (entryId,imageUrl) =>{
			    let attrs = imageUrl||{};
			    if(typeof imageUrl != "string") {
				imageUrl = null;
			    }

			    attrs.entryId = entryId;
			    tmpStyle.entryId = entryId;
			    if(attrs.entryName) {
				attrs.name  = attrs.entryName;
				delete attrs.entryName;
			    }
			    if(glyph.isMap()) {
				let dummy = this.createMapGlyph(attrs,tmpStyle,true);
				if(dummy)
				    this.addFeatures([dummy]);
				return;
			    }
			    if(glyph.isImage()) {
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
			    this.showCommandMessage(glyph.isImage()?"New Image":"New Entry");
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
			let props = {title:glyph.isImage()?'Select Image Entry':
				     (glyph.isEntry()?'Select Entry':'Select Map Entry'),
				     extra:glyph.isImage()?extra:null,
				     initCallback:initCallback,
				     callback:callback,
				     'eventSourceId':this.domId(ID_MENU_NEW)};
			let entryType = glyph.isImage()?'type_image':glyph.isMap()?'type_map,geo_gpx,geo_shapefile':'';
			selectCreate(null, HU.getUniqueId(""),"",false,'entryid',this.getProperty('entryId'),entryType,null,props);
			return
		    } else if(glyph.isLabel()) {
			let text = prompt("Label text:",this.lastText);
			if(!text) return;
			text = text.replace(/\\n/g,"\n");
			this.lastText = text;
			tmpStyle.label = text;
		    }
		    cmd.handler.style = tmpStyle;
		    cmd.handler.layerOptions.styleMap=styleMap;
		}
		let message = glyph?"New " + glyph.label:cmd.message;
		message = message||"";
		if(glyph && glyph.isRoute()) {
		    let html = "Route Type:&nbsp;" +  HU.select("",['id',this.domId("routetype")],["car","bicycle","pedestrian"],this.routeType);
		    html+="<br>"
		    let buttons  =HU.div([ID,this.domId(ID_OK), CLASS,"display-button"], "OK") + SPACE2 +
			HU.div([ID,this.domId(ID_CANCEL), CLASS,"display-button"], "Cancel");	    
		    html+=HU.div(['style',HU.css('text-align','right','margin-top','5px')], buttons);
		    html=HU.div(['style',HU.css('xmin-width','250px','margin','5px')],html);
		    let dialog = HU.makeDialog({content:html,title:'Select Route Type',header:true,my:"left top",at:"left bottom",anchor:this.jq(ID_MENU_NEW)});
		    let ok = ()=>{
			cmd.handler.finishedWithRoute = false;
			this.routeType = this.jq('routetype').val();
			dialog.remove();
			this.showCommandMessage(message+": " + Utils.makeLabel(this.routeType)+" - Draw one or more line segments");
			cmd.activate();
		    };
		    this.jq("routetype").change(ok);
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
	    HtmlUtils.hidePopupObject();
	    this.showCommandMessage("");
	    let buttons = this.jq(ID_COMMANDS).find(".ramadda-clickable");
	    buttons.removeClass("ramadda-display-editablemap-command-active");
	    buttons.each(function() {
		$(this).attr("selected",false);
	    });
	    this.commands.forEach(cmd=>{
		cmd.deactivate();
	    });
	    this.command= null;
	    this.clearSelected();
	    this.myLayer.redraw();
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
	makeListItem:function(feature,idx) {
	    let style  = feature.style||{};
	    let line = "";
	    let mapOptions=this.getMapOptions(feature);
	    let title = mapOptions.name || mapOptions.type;
	    title+="<br>" +
		HU.span([CLASS,"ramadda-clickable",TITLE,"Edit","feature-idx",idx,"command","edit"],
			HU.getIconImage("fas fa-cog")) +"&nbsp;" +
		HU.span([CLASS,"ramadda-clickable",TITLE,"To back","feature-idx",idx,"command","toback"],
			HU.image(Utils.getIcon("shape_move_back.png"))) + "&nbsp;" +
		HU.span([CLASS,"ramadda-clickable",TITLE,"To front","feature-idx",idx,"command","tofront"],
			HU.image(Utils.getIcon("shape_move_front.png"))) + "&nbsp;";
 	    line += HU.td(["nowrap","",TITLE,'Shift click to edit',STYLE,HU.css("padding","5px")], title);
	    let col = "";
	    let css= [];
	    css.push('width','50px');

	    if(style.strokeWidth>0) {
		let line = "solid";
		if(style.strokeDashstyle) {
		    if(['dot','dashdot'].includes(style.strokeDashstyle)) {
			line = "dotted";
		    } else  if(style.strokeDashstyle.indexOf("dash")>=0) {
			line = "dashed";
		    }
		}
		css.push('border',style.strokeWidth+"px " + line +" " + style.strokeColor);
	    }

	    if(feature.style.imageUrl) {
		col += HU.toggleBlock(feature.style.imageUrl, HU.image(feature.style.imageUrl,"width=200px"));
	    } else if(mapOptions.type==GLYPH_LABEL) {
		col +=style.label.replace(/\n/g,"<br>");
	    } else if(mapOptions.type==GLYPH_MARKER) {
		col +=HU.image(style.externalGraphic,['width','16px']);
	    } else if(mapOptions.type==GLYPH_BOX) {
		if(Utils.stringDefined(style.fillColor)) {
		    css.push('background',style.fillColor);
		}
		css.push('height','25px');
		col +=HU.div(['style',HU.css(css)]);
	    } else if(mapOptions.type==GLYPH_HEXAGON) {
		css=[];
		if(Utils.stringDefined(style.fillColor)) {
		    css.push('background',style.fillColor);
		}
		if(Utils.stringDefined(style.strokeColor)) {
		    css.push('color',style.strokeColor);
		}		
		css.push('font-size','32px','vertical-align','center');
		col += HU.span(['style',HU.css(css)],"&#x2B22;")+"<br>";
	    } else if(mapOptions.type==GLYPH_CIRCLE || mapOptions.type==GLYPH_POINT) {
		if(Utils.stringDefined(style.fillColor)) {
		    css.push('background',style.fillColor);
		}
		
		if(mapOptions.type==GLYPH_POINT) {
		    css.push('margin-top','10px','height','10px','width','10px');
		} else {
		    css.push('height','25px');
		    css.push('width','25px');
		}		    
		css.push('display','block');
		col +=HU.div(['class','ramadda-dot', 'style',HU.css(css)]);
	    } else if(mapOptions.type==GLYPH_LINE || mapOptions.type==GLYPH_POLYLINE
		      ||  mapOptions.type==GLYPH_FREEHAND) {
		css.push('height',style.strokeWidth+'px');
		css.push('margin-top','10px','margin-bottom','10px');
		col +=HU.div(['style',HU.css(css)]);
	    } else {
		let s = "";
		for(a in style) {
		    s += " " + a+":" + style[a];
		}
		//not now
		//		col+=HU.div([],s);
	    }
	    let msg = this.getDistances(feature.geometry,feature.type);
	    if(msg) {
		col+="" + msg.replace(/<br>/g," ");
	    }
	    line+= HU.td([CLASS,"ramadda-clickable ramadda-display-editablemap-feature","feature-idx",idx,
			  TITLE,'Shift click to edit',STYLE,HU.css("padding","5px")],col);
	    return line;
	},	    
	addFeatureList:function() {
	    let features="<table width=100%>";
	    this.featureListMap = {};
            this.getFeatures().forEach((feature,idx)=>{
		this.featureListMap[idx]  = feature;
		features+=HU.openTag("tr",['valign','top','style','border-bottom:1px solid #ccc',"feature-idx",idx]);
		let tds=this.makeListItem(feature,idx);
		features+=tds;
		features+="</tr>";
	    });
	    features+="</table>";
	    let _this  = this;
	    this.jq(ID_LIST).html(features);

	    this.jq(ID_LIST).find("[command]").click(function(event) {
		event.preventDefault();
		let command  = $(this).attr("command");
		let idx  = $(this).attr("feature-idx");
		let feature   = _this.featureListMap[idx];
		if(command=="toback") _this.toFront(false,[feature]);
		else if(command=="tofront") _this.toFront(true,[feature]);		
		else if(command=="edit") {
		    _this.editFeatureProperties(feature);
		}
		
	    });

	    this.jq(ID_LIST).find(".ramadda-display-editablemap-feature").click(function(event) {
		let clazz  = "ramadda-display-editablemap-feature-selected";
		let idx  = $(this).attr("feature-idx");
		let feature   = _this.featureListMap[idx];
		if (event.shiftKey) {
		    if(feature) {
			feature.listIdx = idx;
			_this.editFeatureProperties(feature);
			return;
		    }
		}

		if($(this).hasClass(clazz)) {
		    $(this).removeClass(clazz);
		    if(feature) {
			_this.featureSelector.clickoutFeature(feature);
		    }
		} else {
		    $(this).addClass(clazz);
		    if(feature) {
			_this.selectFeature(feature);
		    }
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
	    this.jq(ID_LIST_DELETE).button().click(()=>{
		let _this  = this;
		let cut  = [];
		this.jq(ID_LIST).find(".ramadda-display-editablemap-feature-selected").each(function() {
		    let feature   = _this.featureListMap[$(this).attr("feature-idx")];
		    if(!feature) return;
		    cut.push(feature);
		});
		this.removeImages(cut);
		this.setClipboard(cut);
		this.removeFeatures(cut);
		this.addFeatureList();
	    });

	    this.jq(ID_LIST_OK).button().click(()=>{
		let nuke = [];
		this.myLayer.features.forEach((feature,idx)=>{
		    if(this.jq("cbx" + idx).is(':checked')) {
			nuke.push(feature);
		    }
		});

		this.removeFeatures(nuke);
		close();
		this.listDialog  = null;
	    });
	    this.jq(ID_LIST_CANCEL).button().click(()=>{
		close()});
	},
	getFeatures: function() {
            return this.myLayer.features;
	},
	addFeatures:function(features) {
	    let layer = this.myLayer;
	    layer.addFeatures(features);
	    features.forEach(feature=>{
		feature.layer = layer;
	    });
	    this.featureChanged();
	},
	    
	removeFeatures: function(features) {
	    features.forEach(feature=>{
		if(feature.mapLayer) {
		    this.getMap().removeLayer(feature.mapLayer);
		}
	    });
	    this.myLayer.removeFeatures(features);
	    this.featureChanged();	    
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
	    let newOnes = this.clipboard.map(feature=>{
		feature= feature.clone();
		if(feature.style) {
		    feature.style =$.extend({},feature.style);
		}
		return feature;
	    });
	    for(let i=0;i<newOnes.length;i++) {
		newOnes[i].type = this.clipboard[i].type;
	    }
	    let h = this.map.getMap().getExtent().getHeight();
	    this.pasteCount++;
	    let delta = (this.pasteCount*0.05)*h;
	    newOnes.forEach(feature=>{
		feature.geometry.move(delta,-delta);
		this.checkImage(feature);
	    });
	    this.addFeatures(newOnes);
	},
	getFeaturePropertyApply:function() {
	    return (feature, props)=>{
		this.featureChanged();	    
		let style = {};
		props.forEach(prop=>{
		    if(prop=="labelSelect") return;
		    let v = this.jq(prop).val();
		    if(prop=="label") {
			v = v.replace(/\\n/g,"\n");
		    }
		    style[prop] = v;
		    feature.style[prop] = v;
		});
		if(feature.mapLayer) {
		    feature.mapLayer.styleMap = this.getMap().getVectorLayerStyleMap(feature.mapLayer, style);
		    feature.mapLayer.style = style;
		    if(feature.mapLayer.features) {
			feature.mapLayer.features.forEach(f=>{
			    f.style = $.extend(f.style,style);
			});
		    }
		    feature.mapLayer.redraw();

		}

		if(Utils.stringDefined(feature.style.popupText)) {
		    feature.style.cursor = 'pointer';
		} else {
		    feature.style.cursor = 'auto';
		}
		if(feature.style.imageUrl) {
		    if(feature.image) feature.image.setOpacity(feature.style.imageOpacity);
		    this.checkImage(feature);
		}
		this.myLayer.redraw();
		this.showLegend();
	    };
	},
	clearSelected:function() {
	    this.myLayer.selectedFeatures  = [];
	    this.myLayer.redraw();
	},
	getSelected: function() {
	    return this.myLayer.selectedFeatures ?? [];
	},
	doEdit: function(feature) {
	    this.clearSelected();
	    if(!feature) {
		if(this.getSelected().length==0) return;
		feature = this.getSelected()[0];
	    }
	    if(!feature) return;
	    let style = feature.style;
	    let html =HU.div([STYLE,HU.css("margin","8px")], "Feature: " + feature.type); 
	    this.myLayer.redraw(feature);
	    if(feature.image && Utils.isDefined(feature.image.opacity)) {
		feature.style.imageOpacity=feature.image.opacity;
	    }
	    this.editFeatureProperties(feature);
	},
	
	makeMenu: function(html) {
	    return  HU.div([CLASS,"wiki-editor-popup"], html);
	},
	menuItem: function(id,label,cmd) {
	    if(cmd) {
//		HU.image(icon_command,['width','12px']);
		let prefix = '';
		if(cmd!="Esc") prefix = 'ctrl-';
		label = HU.leftRightTable(label,HU.div(['style','margin-left:8px;'], HU.span(['style','color:#ccc'], prefix+cmd)));
	    }
	    return  HU.div([ID,id,CLASS,"ramadda-clickable"],label);
	},
	editFeatureProperties:function(feature) {
	    this.doProperties(feature.style, this.getFeaturePropertyApply(), feature);
	},


	doProperties: function(style, apply,feature) {
	    let html = "";
	    html +=HU.formTable();
	    let props;
	    let values = {};
	    if(style) {
		props = [];
		let isImage = style.imageUrl;
		for(a in style) {
		    if(isImage) {
			if(a!="imageUrl" && a!="imageOpacity") continue;
		    }
		    props.push(a);
		    values[a] = style[a];
		}
		if(feature.type==GLYPH_MARKER) {
		    props = ["pointRadius","externalGraphic"];
		} 
	    } else {
		props = ['strokeColor','strokeWidth','strokeDashstyle','strokeOpacity',
			 'fillColor','fillOpacity',
			 'pointRadius','externalGraphic','imageOpacity','fontSize','fontWeight','fontStyle','fontFamily','labelAlign'];
	    }
	    
	    if(props.includes("entryId") && !props.includes("wikiText")) props.push("wikiText");
	    if(!props.includes("wikiText") && !props.includes("popupText")) props.push("popupText");


	    props.forEach(prop=>{
		if(prop=="labelSelect") return;
		if(prop=="cursor") return;		
		let label =  Utils.makeLabel(prop);		
		if(prop=="pointRadius") label = "Size";
		let widget;
		if(prop=="externalGraphic") {
		    label="Marker"
		    let options = "";
		    let graphic = values[prop];
		    if(!Utils.isDefined(graphic))
			graphic = this.getExternalGraphic();
		    widget = HU.hidden("",graphic,['id',this.domId("externalGraphic")]) +
			"<table><tr valign=top><td width=1%>" +
			HU.image(graphic,['width','24px','id',this.domId("externalGraphic_image")]) +
			"</td><td>" +
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
			widget =  HU.textarea("",v,[ID,this.domId(prop),"rows",5,"cols", 60]);
		    } else if(prop=="popupText" || prop=="wikiText") {
			size="80"
			widget =  HU.textarea("",v||"",[ID,this.domId(prop),"rows",5,"cols", 60]);
		    } else if(prop=="strokeDashstyle") {
			widget = HU.select("",['id',this.domId(prop)],['solid','dot','dash','dashdot','longdash','longdashdot'],v);
		    } else if(prop=="fontWeight") {
			widget = HU.select("",['id',this.domId(prop)],["normal","bold","lighter","bolder","100","200","300","400","500","600","700","800","900"],v);
 		    } else if(prop=="fontStyle") {
			widget = HU.select("",['id',this.domId(prop)],["normal","italic"],v);			
		    } else {
			if(props == "pointRadius") label="Size";
			if(prop=="strokeWidth" || prop=="pointRadius" || prop=="fontSize" || prop=="imageOpacity") size="4";
			else if(prop=="fontFamily") size="60";
			else if(prop=="imageUrl") size="80";		    
			if(prop.indexOf("Color")>=0) {
			    let id = this.domId(prop);
			    let colors = Utils.split("transparent,red,orange,yellow,green,blue,indigo,violet,white,black,IndianRed,LightCoral,Salmon,DarkSalmon,LightSalmon,Crimson,Red,FireBrick,DarkRed,Pink,LightPink,HotPink,DeepPink,MediumVioletRed,PaleVioletRed,LightSalmon,Coral,Tomato,OrangeRed,DarkOrange,Orange,Gold,Yellow,LightYellow,LemonChiffon,LightGoldenrodYellow,PapayaWhip,Moccasin,PeachPuff,PaleGoldenrod,Khaki,DarkKhaki,Lavender,Thistle,Plum,Violet,Orchid,Fuchsia,Magenta,MediumOrchid,MediumPurple,RebeccaPurple,BlueViolet,DarkViolet,DarkOrchid,DarkMagenta,Purple,Indigo,SlateBlue,DarkSlateBlue,MediumSlateBlue,GreenYellow,Chartreuse,LawnGreen,Lime,LimeGreen,PaleGreen,LightGreen,MediumSpringGreen,SpringGreen,MediumSeaGreen,SeaGreen,ForestGreen,Green,DarkGreen,YellowGreen,OliveDrab,Olive,DarkOliveGreen,MediumAquamarine,DarkSeaGreen,LightSeaGreen,DarkCyan,Teal,Aqua,Cyan,LightCyan,PaleTurquoise,Aquamarine,Turquoise,MediumTurquoise,DarkTurquoise,CadetBlue,SteelBlue,LightSteelBlue,PowderBlue,LightBlue,SkyBlue,LightSkyBlue,DeepSkyBlue,DodgerBlue,CornflowerBlue,MediumSlateBlue,RoyalBlue,Blue,MediumBlue,DarkBlue,Navy,MidnightBlue,Cornsilk,BlanchedAlmond,Bisque,NavajoWhite,Wheat,BurlyWood,Tan,RosyBrown,SandyBrown,Goldenrod,DarkGoldenrod,Peru,Chocolate,SaddleBrown,Sienna,Brown,Maroon,White,Snow,HoneyDew,MintCream,Azure,AliceBlue,GhostWhite,WhiteSmoke,SeaShell,Beige,OldLace,FloralWhite,Ivory,AntiqueWhite,Linen,LavenderBlush,MistyRose,Gainsboro,LightGray,Silver,DarkGray,Gray,DimGray,LightSlateGray,SlateGray,DarkSlateGray",",");

			    let bar = "";
			    let cnt = 0;
			    colors.forEach(color=>{
				bar += HU.div(['title',color,'color',color,'widget-id',id,'class','ramadda-clickable ramadda-color-select ramadda-dot', 'style',HU.css('background',color)]) +HU.space(1);
				cnt++;
				if(cnt>=10) {
				    cnt = 0;
				    bar+="<br>";
				}
			    });
			    bar = HU.div(['style','max-height:66px;overflow-y:auto;border:1px solid #ccc;'],bar);
			    widget =  HU.input("",v,['class','ramadda-editablemap-color',ID,id,"size",8]);
			    widget =  HU.div(['id',id+'_display','class','ramadda-dot', 'style',HU.css('background',v)]) +
				HU.space(2)+widget;
			    widget  = HU.table([],HU.tr(['valign','top'],HU.tds([],[widget,bar])));
			} else if(prop=="labelAlign") {
			    //lcr tmb
			    let items = [["lt","Left Top"],["ct","Center Top"],["rt","Right Top"],
					 ["lm","Left Middle"],["cm","Center Middle"],["rm","Right Middle"],
					 ["lb","Left Bottom"],["cb","Center Bottom"],["rb","Right Bottom"]];
			    widget =  HU.select("",['id',this.domId(prop)],items,v);			
			} else if(prop.indexOf("Width")>=0 || prop.indexOf("Offset")>=0) {
			    let id = this.domId(prop);
			    if(!v || v=="") v= 1;
			    let min  = prop.indexOf("Offset")>=0?0:0;
			    widget =  HU.input("",v,[ID,this.domId(prop),"size",4])+HU.space(4) +
				HU.div(['slider-min',min,'slider-max',50,'slider-step',1,'slider-value',v,'slider-id',id,ID,id+'_slider','class','ramadda-slider',STYLE,HU.css("display","inline-block","width","200px")],"");

			} else if(prop.indexOf("Opacity")>=0) {
			    let id = this.domId(prop);
			    if(!v || v=="") v= 1;
			    widget =  HU.input("",v,[ID,this.domId(prop),"size",4])+HU.space(4) +
				HU.div(['slider-min',0,'slider-max',1,'slider-value',v,'slider-id',id,ID,id+'_slider','class','ramadda-slider',STYLE,HU.css("display","inline-block","width","200px")],"");
			} else {
			    widget =  HU.input("",v,[ID,this.domId(prop),"size",size]);
			}
		    }
		}
		html+=HU.formEntry(label+":",widget);
	    });

	    html+="</table>";
	    html = HU.div([STYLE,HU.css("max-height","350px","overflow-y","scroll","margin-bottom","5px")], html);
	    html+="<center>";
	    html +=HU.div([ID,this.domId(ID_APPLY), CLASS,"display-button"], "Apply");
	    html += SPACE2;
	    html +=HU.div([ID,this.domId(ID_OK), CLASS,"display-button"], "Ok");
	    html += SPACE2;
	    if(feature) {
		html +=HU.div([ID,this.domId(ID_DELETE), CLASS,"display-button"], "Delete");
		html += SPACE2;
	    }
	    html +=HU.div([ID,this.domId(ID_CANCEL), CLASS,"display-button"], "Cancel");	    
	    html  = HU.div([CLASS,"wiki-editor-popup"], html);
	    html+="</center>";
	    this.map.ignoreKeyEvents = true;
	    let dialog = HU.makeDialog({content:html,anchor:this.jq(ID_MENU_FILE),title:"Map Properties",header:true,draggable:true});
	    let icons =dialog.find("#" + this.domId("externalGraphic_icons"));
	    if(icons.length>0) {
		HU.getEmojis(emojis=>{
		    let html = "";
		    emojis.forEach(cat=>{
			if(html!="") html+="</div>";
			html+=HU.open('div',['class','ramadda-editablemap-image-category']);
			html+=HU.div(['class','ramadda-editablemap-image-category-label'],HU.b(cat.name));
			cat.images.forEach(image=>{
			    html+=HU.image(image.image,['class','ramadda-clickable ramadda-editablemap-image','width','24px','loading','lazy','title',image.name]);
			});
		    });
		    html+="</div>";
		    html = HU.div(['style',HU.css('width','400px','max-height','200px','overflow-y','auto')], html);
		    html = HU.input("","",['id',this.domId('externalGraphic_search'),'placeholder','Search','size','30']) +"<br>"+
			html;
		    icons.html(html);
		    let _this = this;
		    let images = icons.find('.ramadda-editablemap-image');
		    images.click(function() {
			let src = $(this).attr('src');
			jqid(_this.domId("externalGraphic_image")).attr('src',src);
			jqid(_this.domId("externalGraphic")).val(src);			
		    });
		    let cats = icons.find('.ramadda-editablemap-image-category');
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
			    $(this).find('.ramadda-editablemap-image').each(function() {
				if($(this).attr('imagevisible')=='true') {
				    anyVisible = true;
				}
			    });

			    let label = $(this).find('.ramadda-editablemap-image-category-label');
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

	    dialog.find('.ramadda-editablemap-color').change(function() {
		let c = $(this).val();
		let id = $(this).attr('id');
		$("#"+ id+'_display').css('background',c);
		asdsa
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
		    this.glyphs.forEach(g=>{
			g.applyStyle(style);
		    });
		}
	    }
	    let close = ()=>{
		this.map.ignoreKeyEvents = false;
		dialog.hide();
		dialog.remove();
	    }
	    if(feature) {
		this.jq(ID_DELETE).button().click(()=>{
		    this.removeFeatures([feature]);
		    this.addFeatureList();
		    close();
		});
	    }
	    this.jq(ID_OK).button().click(()=>{
		apply(feature,props);
		this.addFeatureList();
		close();
	    });
	    this.jq(ID_APPLY).button().click(()=>{
		apply(feature,props);
		this.addFeatureList();
	    });
	    this.jq(ID_CANCEL).button().click(()=>{
		close();
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
	    if(this.getProperty("thisEntryType")!="geo_editable_json") {
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
	    let list =[];
            this.myLayer.features.forEach(feature=>{
		let geom = feature.geometry;
		let mapOptions=this.getMapOptions(feature);
		let obj = {
		    mapOptions:mapOptions,
		    geometryType:geom.CLASS_NAME,
		    points:[]
		};
		let style = feature.style;
		if(mapOptions.type ==GLYPH_MAP && feature.mapLayer) {
		    style = feature.mapLayer.style||style;
		}

		if(style) {
		    if(feature.image && Utils.isDefined(feature.image.opacity)) {
			style.imageOpacity=feature.image.opacity;
		    }
		    obj.style = style;
		}
		list.push(obj);
		let vertices  = geom.getVertices();
		if(feature.image) {
		    let mapBounds = feature.geometry.getBounds();
		    let b = this.map.transformProjBounds(mapBounds);
		    obj.points.push({latitude:b.top,longitude:b.left});
		    obj.points.push({latitude:b.top,longitude:b.right});
		    obj.points.push({latitude:b.bottom,longitude:b.right});
		    obj.points.push({latitude:b.bottom,longitude:b.left});
		    obj.points.push({latitude:b.top,longitude:b.left});		    		    		    		    
		} else {
		    vertices.forEach(vertex=>{
			let pt = vertex.clone().transform(this.map.sourceProjection, this.map.displayProjection);
			let lat = Utils.trimDecimals(pt.y,6);
			let lon = Utils.trimDecimals(pt.x,6);			
			//new way
			obj.points.push(lat);
			obj.points.push(lon);			
//			obj.points.push({latitude:lat,longitude:lon});
		    });
		}

	    });
	    let latlon = this.getMap().getBounds();
	    let json  = {
		list:list,
		zoomLevel:this.getMap().getMap().getZoom(),
		bounds:{
		    north:latlon.top,
		    west:latlon.left,
		    south:latlon.bottom,
		    east:latlon.right,
		}
	    };
	    return  JSON.stringify(json);
	},


	showFileMenu: function(button) {
	    let html ="";
	    if(!Utils.isAnonymous()) {	    
		html +=this.menuItem(this.domId(ID_SAVE),"Save",'S');
	    }
	    html+= this.menuItem(this.domId(ID_DOWNLOAD),"Download")
	    html+= this.menuItem(this.domId(ID_PROPERTIES),"Set Default Properties");
	    html+= this.menuItem(this.domId(ID_CMD_LIST),"List Features",'L');
	    html+= this.menuItem(this.domId(ID_CLEAR),"Clear Commands","Esc");
	    html  = this.makeMenu(html);
	    this.dialog = HU.makeDialog({content:html,anchor:button});
	    let _this = this;

	    this.jq(ID_NAVIGATE).click(function() {
		HtmlUtils.hidePopupObject();
		_this.setCommand(null);
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
	    });	    
	    this.jq(ID_CMD_LIST).click(function(){
		_this.clearCommands();
		_this.listFeatures();
	    });	    
	},

	handleEditEvent:function() {
	    if(this.getSelected().length==1) {
		console.log("selected:"); 
		this.doEdit(this.getSelected()[0]);
		return;
	    }
	    this.setCommand(ID_EDIT);
	},
	showLegend: function() {
	    if(!this.getShowLegend()) return;
	    let html = "";
	    let map = {};
            this.getFeatures().forEach((feature,idx)=>{
		if(feature.type!=GLYPH_ROUTE && feature.type!=GLYPH_FREEHAND) return;
		let msg = this.getDistances(feature.geometry,feature.type,true);
		let color = feature.style.strokeColor??"black";
		let line = "solid";
		if(feature.style.strokeDashstyle) {
		    if(['dot','dashdot'].includes(feature.style.strokeDashstyle)) {
			line = "dotted";
		    } else  if(feature.style.strokeDashstyle.indexOf("dash")>=0) {
			line = "dashed";
		    }
		}
		let id = HU.getUniqueId('feature_');
		feature.legendId = id;
		map[id] = feature;
		let style = "";
		if(color) style= HU.css('border-bottom' , "3px " + line+ " " +color);
		let item = HU.div(['style','margin-bottom:5px;margin-right:5px;display:inline-block;width:30px;height:6px;' + style]) + msg;
		html+=HU.div(['class','ramadda-clickable','id',id],item);
	    });
	    if(html!="") {
		html = HU.div(['style','padding:5px;border : var(--basic-border);  background-color : var(--color-mellow-yellow);'], html);
	    }
	    this.jq(ID_MESSAGE3).html(html);
	    let _this = this;
	    this.jq(ID_MESSAGE3).find('.ramadda-clickable').click(function() {
		let id = $(this).attr('id');
		_this.clearSelected();
		_this.selectFeature(map[id]);
	    });
	},
	featureChanged:function() {
	    this.featureHasBeenChanged = true;
	    this.showLegend();
	},
	showNewMenu: function(button) {
	    let html ="<table><tr valign=top>";
	    let tmp = Utils.splitList(this.glyphs,this.glyphs.length/2);
	    tmp.forEach(glyphs=>{
		html+="<td>&nbsp;</td>";
		html+="<td>";
		glyphs.forEach(g=>{
		    let icon = g.options.icon||ramaddaBaseUrl+"/map/marker-blue.png";
		    let label = HU.image(icon,['width','16']) +SPACE1 + g.label;
		    html+= this.menuItem(this.domId("menunew_" + g.type),label+SPACE2);
		});
		html+="</td>";
	    });
	    html+="</tr></table>";
	    html  = this.makeMenu(html);
	    this.dialog = HU.makeDialog({content:html,anchor:button});
	    let _this = this;
	    this.glyphs.forEach(g=>{
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
	     null,
	     [ID_TOFRONT,"To Front"],
	     [ID_TOBACK,"To Back"],
	     null,
			[ID_EDIT,"Edit Properties",'P']].reduce((prev,tuple)=>{
		 prev = prev || "";
		 if(!tuple) return prev+ HU.div([CLASS,"ramadda-menu-divider"]);						
		 return prev + 	this.menuItem(this.domId(tuple[0]),tuple[1],tuple[2]);
	     },"");
	    
	    this.dialog = HU.makeDialog({content:this.makeMenu(html),anchor:button});
	    let _this = this;
	    this.jq(ID_DELETE_ALL).click(function(){
		HtmlUtils.hidePopupObject();
		_this.doDeleteAll();
	    });
	    this.jq(ID_CUT).click(function(){
		HtmlUtils.hidePopupObject();
		_this.doCut();
	    });
	    this.jq(ID_SELECT_ALL).click(function(){
		HtmlUtils.hidePopupObject();
		_this.selectAll();
	    });
	    
	    this.jq(ID_COPY).click(function(){
		HtmlUtils.hidePopupObject();
		_this.doCopy();
	    });	    
	    this.jq(ID_PASTE).click(function(){
		HtmlUtils.hidePopupObject();
		_this.doPaste();
	    });
	    this.jq(ID_TOFRONT).click(function(){
		HtmlUtils.hidePopupObject();
		_this.toFront(true);
	    });
	    this.jq(ID_TOBACK).click(function(){
		HtmlUtils.hidePopupObject();
		_this.toFront(false);
	    });
	    this.jq(ID_EDIT).click(function(){
		HtmlUtils.hidePopupObject();
		_this.handleEditEvent();
	    });	    
	    [ID_SELECTOR,ID_MOVER,ID_RESHAPE,ID_RESIZE].forEach(command=>{
		this.jq(command).click(function(){
		    HtmlUtils.hidePopupObject();
		    if(ID_SELECTOR==command) {
			_this.clearSelected();
		    }
		    _this.setCommand(command);
		});
	    });
	},
	
	selectFeature:function(feature) {
	    this.featureSelector.clickFeature(feature);
	},
	selectAll:function() {
	    this.myLayer.features.forEach(feature=>{
		this.selectFeature(feature);
	    });
	},
	setClipboard:function(features) {
	    if(features)
		this.clipboard = features.map(feature=>{return feature;});
	    else
		this.clipboard=null;
	    this.pasteCount=0;
	},
	removeImages: function(features) {
	    if(!features) return;
	    features.forEach(feature=>{
		if(feature.image) {
		    this.getMap().removeLayer(feature.image);
		    feature.image = null;
		}
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
	checkImage:function(feature) {
	    if(!feature.style || !feature.style.imageUrl) return;
	    if(feature.image && Utils.isDefined(feature.image.opacity)) {
		feature.style.imageOpacity=feature.image.opacity;
	    }
	    this.removeImages([feature]);
	    this.clearBounds(feature.geometry);
	    let mapBounds = feature.geometry.getBounds();
	    let b = this.map.transformProjBounds(mapBounds);
	    feature.image=  this.map.addImageLayer("","","",feature.style.imageUrl,true,  b.top,b.left,b.bottom,b.right);
	    if(Utils.isDefined(feature.style.imageOpacity))
		feature.image.setOpacity(feature.style.imageOpacity);
	},
	toFront: function(toFront, selected) {
	    selected = selected ?? this.getSelected();
	    if(!selected) return;
	    this.featureChanged();	    
	    let features = this.myLayer.features;
	    selected.forEach(feature=>{
		if(feature.image) {
		    if(toFront)
			this.map.toFront(feature.image);
		    else
			this.map.toBack(feature.image);		    
		}
		if(toFront)
		    Utils.toFront(features, feature);
		else
		    Utils.toBack(features, feature);
	    });
	    this.myLayer.redraw();
	},


	doCut: function() {
//	    this.clearCommands();
	    if(this.getSelected().length>0) {
		this.removeImages(this.getSelected());
		let features = this.getSelected().map(feature=>{return feature;});
		this.setClipboard(features);
		this.removeFeatures(features);
	    }
	},
	doDeleteAll: function() {
//	    this.clearCommands();
	    if(!window.confirm("Are you sure you want to delete all map features?")) return
	    this.removeImages(this.myLayer.features);
	    this.setClipboard(this.myLayer.features.map(feature=>{return feature;}));
	    this.removeFeatures(this.myLayer.features);
	},
	doCopy: function() {
//	    this.clearCommands();
	    if(getSelected().length==0) return;
	    this.setClipboard(this.getSelected().map(feature=>{return feature;}));
	},
	addGlyph: function(glyph) {
	    this.glyphs[glyph.getId()]= glyph;
	},
	loadAnnotationJson: function(mapJson,map,layer, glyphMap) {
	    let list = mapJson.list||mapJson;
	    list.forEach(mapGlyph=>{
		if(!mapGlyph.points) {
		    console.log("No points defined:" + JSON.stringify(mapGlyph));
		    return;
		}
		let glyph = glyphMap?glyphMap[mapGlyph.type]:null;
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
		let mapOptions = mapGlyph.mapOptions;
		if(!mapOptions) {
		    mapOptions = {
			type:mapGlyph.type
		    }
		}

		if(!style.fillColor) style.fillColor = "transparent";
		let feature;
		let points=mapGlyph.points;
		if(points.length>0) {
		    let oldWay = Utils.isDefined(points[0].latitude);
		    if(!oldWay) {
			let tmp = [];
			for(let i=0;i<points.length;i+=2) {
			    tmp.push({latitude:points[i],longitude:points[i+1]});			    
			}
			points = tmp;
		    }
		}

		if(points.length>1) {
		    let latLons = [];
		    points.forEach(pt=>{
			latLons.push(new OpenLayers.Geometry.Point(pt.longitude,pt.latitude));
		    });
		    if(mapGlyph.geometryType=="OpenLayers.Geometry.Polygon") {
			map.transformPoints(latLons);
			let linearRing = new OpenLayers.Geometry.LinearRing(latLons);
			let geom = new OpenLayers.Geometry.Polygon(linearRing);
			feature = new OpenLayers.Feature.Vector(geom,null,style);
		    } else {
			feature = map.createPolygon("","",latLons,style,	null,mapGlyph.geometryType=="OpenLayers.Geometry.LineString");
		    }
		} else if(mapOptions.type==GLYPH_MAP) {
		    feature = this.createMapGlyph(mapOptions,style)

		} else {
		    let point =  MapUtils.createLonLat(points[0].longitude, points[0].latitude);
		    feature = map.createPoint("",point,style);
		}
		if(feature) {
		    feature.mapOptions = mapOptions;
		    feature.style = style;
		    this.checkImage(feature);
		    this.addFeatures([feature]);
		}
	    });
	},
	createMapGlyph: function(attrs,style,andZoom) {				
	    style = style??{};
	    let  dummy = this.getMap().createPolygon("", "", [], {strokeWidth:0},null,true);
	    dummy.style = {};
	    dummy.mapOptions = {type:GLYPH_MAP};
	    $.extend(dummy.mapOptions,  attrs);
	    let layer = this.addMapLayer(dummy.mapOptions,style,andZoom);
	    if(!layer) return;
	    layer.style = style;
	    dummy.mapLayer = layer;
	    return dummy;
	},
	addMapLayer(opts,style,andZoom) {
	    let url = ramaddaBaseUrl +"/entry/get?entryid="+opts.entryId;
	    let selectCallback = null;
	    let unSelectCallback = null;	    
//	    console.log(opts.entryType);
	    switch(opts.entryType) {
	    case 'latlonimage': 
		let w = 2048;
		let h = 1024;
		return this.getMap().addImageLayer(opts.entryId, opts.name,"",url,true,
						   opts.north, opts.west,opts.south,opts.east, w,h);
	    case 'geo_gpx': 
		return this.getMap().addGpxLayer(opts.name,url,true, selectCallback, unSelectCallback,style,null,andZoom);
		break;
	    case 'geo_geojson': 
		return this.getMap().addGeoJsonLayer(opts.name,url,true, selectCallback, unSelectCallback,style,null,andZoom);
		break;		
	    case 'geo_shapefile_fips': 
	    case 'geo_shapefile': 
		url = ramaddaBaseUrl+'/entry/show?entryid=' + opts.entryId+'&output=shapefile.kml&formap=true';
		//fall thru to kml
	    case 'geo_kml': 
		let loadCallback = (map,layer)=>{
		    if(layer.features) {layer.features.forEach(f=>{f.style = style;});}
		    layer.redraw();
		};
		let layer =  this.getMap().addKMLLayer(opts.name,url,true, selectCallback, unSelectCallback,style,loadCallback,andZoom);

		return layer;
	    default:
		console.error('Unknown map type:' + opts.entryType);
		return null;
	    }
	},


	loadMap: function(entryId) {
	    //Pass in true=skipParent
	    let url = this.getProperty("fileUrl",null,false,true);
	    if(!url && entryId)
		url = ramaddaBaseUrl+"/entry/get?entryid=" + entryId;
	    if(!url) return;
	    let _this = this;
            $.ajax({
                url: url,
                dataType: 'text',
                success: (data) => {
		    if(data=="") data="[]";
		    try {
			let json = JSON.parse(data);
			this.loadAnnotationJson(json,_this.map,_this.myLayer,_this.glyphMap);
			this.featureHasBeenChanged = false;
			this.showLegend();
			let bounds = null;
			let zoomLevel = -1;
			if(!_this.getProperty("embedded") && !_this.getProperty("zoomLevel")) {
			    if(json.bounds) {
				zoomLevel = json.zoomLevel;
				bounds =  _this.getMap().transformLLBounds(MapUtils.createBounds(json.bounds.west,
												 json.bounds.south,
												 json.bounds.east,
												 json.bounds.north));
			    } else if(_this.myLayer.features.length>0) {
				bounds = new OpenLayers.Bounds();
				_this.myLayer.features.forEach(feature=>{
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
			if(bounds) {
			    _this.map.zoomToExtent(bounds);
			}
			if(zoomLevel>=0 && Utils.isDefined(zoomLevel)) {
			    _this.getMap().getMap().zoomTo(zoomLevel);
			}
		    } catch(err) {
			this.showMessage("Failed to load map:" + err);
			console.log("error:" + err);
			console.log(err.stack);
			console.log("map json:" + data);
		    }

                }
            }).fail(err=>{
		this.showMessage("Failed to load map:" + err);
		console.log("error:" + JSON.stringify(err));
	    });



	},
	doMakeMapGlyphs:function() {
	    let externalGraphic = this.getExternalGraphic();
	    if(!externalGraphic.startsWith(ramaddaBaseUrl)) externalGraphic = ramaddaBaseUrl+externalGraphic;
	    return [
		new GlyphType(this,GLYPH_MARKER,"Marker",
			     {strokeWidth:0, 
			      fillColor:"transparent",
			      externalGraphic: externalGraphic,
			      pointRadius:this.getPointRadius(10)},
			      OpenLayers.Handler.MyPoint,
			      {icon:ramaddaBaseUrl+"/map/marker-blue.png"}),
		new GlyphType(this,GLYPH_POINT,"Point",
			     {strokeWidth:this.getProperty("strokeWidth",2), 
			      strokeDashstyle:'solid',
			      fillColor:"transparent",
			      fillOpacity:1,
			      strokeColor:this.getStrokeColor(),
			      strokeOpacity:1,
			      pointRadius:this.getPointRadius(4)},
			      OpenLayers.Handler.MyPoint,
			      {icon:ramaddaBaseUrl+"/icons/dot.png"}),
		new GlyphType(this,GLYPH_LABEL,"Label",
			     {label : "label",
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
			      labelSelect:true,
			     }, OpenLayers.Handler.Point,
			      {icon:ramaddaBaseUrl+"/icons/text.png"}),
		new GlyphType(this,GLYPH_LINE, "Line",
			     {strokeColor:this.getStrokeColor(),
			      strokeWidth:this.getStrokeWidth(),
			      strokeDashstyle:'solid',
			      strokeOpacity:1,
			     },
			      OpenLayers.Handler.MyPath,
			      {maxVertices:2,
			       icon:ramaddaBaseUrl+"/icons/line.png"}),		
		new GlyphType(this,GLYPH_POLYLINE, "Polyline",
			     {strokeColor:this.getStrokeColor(),
			      strokeWidth:this.getStrokeWidth(),
			      strokeDashstyle:'solid',			      
			      strokeOpacity:1,
			      fillColor:"transparent",
			      fillOpacity:1.0},
			      OpenLayers.Handler.MyPath,
			      {icon:ramaddaBaseUrl+"/icons/polyline.png"			      }),
		new GlyphType(this,GLYPH_FREEHAND,"Freehand",
			     {strokeColor:this.getStrokeColor(),
			      strokeWidth:this.getStrokeWidth(),
			      strokeDashstyle:'solid',
			      strokeOpacity:1,     
			     },
			     OpenLayers.Handler.MyPath,
			      {freehand:true,icon:ramaddaBaseUrl+"/icons/freehand.png"}),
		ramaddaState.routingEnabled?
		    new GlyphType(this,GLYPH_ROUTE, "Route",
				  {
				      strokeColor:this.getStrokeColor(),
				      strokeWidth:this.getStrokeWidth(),
				      strokeDashstyle:'solid',
				      strokeOpacity:1,
				  },
				  OpenLayers.Handler.MyPath,{icon:ramaddaBaseUrl+"/icons/route.png"}):null,		
		new GlyphType(this,GLYPH_IMAGE, "Image",
			     {strokeColor:"blue",
			      strokeWidth:1,
			      imageOpacity:this.getImageOpacity(1),
			      fillColor:"transparent"},
			     OpenLayers.Handler.ImageHandler,
			      {snapAngle:90,sides:4,irregular:true,isImage:true,
			       icon:ramaddaBaseUrl+"/icons/imageicon.png"}
			    ),
		new GlyphType(this,GLYPH_ENTRY,"Entry Marker",
			     {strokeWidth:0, 
			      fillColor:"transparent",
			      externalGraphic: ramaddaBaseUrl +"/icons/video.png",
			      pointRadius:12},
			      OpenLayers.Handler.MyEntryPoint,
			      {isEntry:true,
			       icon:ramaddaBaseUrl+"/icons/entry.png"}),
		new GlyphType(this,GLYPH_MAP,"Map",
			      {strokeColor:this.getStrokeColor(),
			      strokeWidth:this.getStrokeWidth(),
			      strokeDashstyle:'solid',			      
			      strokeOpacity:1,
			      fillColor:"transparent",
			      fillOpacity:1.0},
			      OpenLayers.Handler.MyEntryPoint,
			      {isMap:true,
			       icon:ramaddaBaseUrl+"/icons/map.png"}),		
		new GlyphType(this,GLYPH_BOX, "Box",
			      {strokeColor:this.getStrokeColor(),
			       strokeWidth:this.getStrokeWidth(),
			       strokeDashstyle:'solid',
			       strokeOpacity:1,
			       fillColor:"transparent",
			       fillOpacity:1.0},
			     OpenLayers.Handler.MyRegularPolygon,
			      {snapAngle:90,sides:4,irregular:true,
			       icon:ramaddaBaseUrl+"/icons/rectangle.png"}
			    ),
		new GlyphType(this,GLYPH_CIRCLE, "Circle",
			     {strokeColor:this.getStrokeColor(),
			      strokeWidth:this.getStrokeWidth(),
			      strokeDashstyle:'solid',
			      strokeOpacity:1,
			      fillColor:"transparent",
			      fillOpacity:1.0},
			     OpenLayers.Handler.MyRegularPolygon,
			      {snapAngle:45,sides:40,icon:ramaddaBaseUrl+"/icons/ellipse.png"}
			    ),

		new GlyphType(this,GLYPH_TRIANGLE, "Triangle",
			     {strokeColor:this.getStrokeColor(),
			      strokeWidth:this.getStrokeWidth(),
			      strokeDashstyle:'solid',
			      strokeOpacity:1,
			      fillColor:"transparent",
			      fillOpacity:1.0},
			     OpenLayers.Handler.MyRegularPolygon,
			      {snapAngle:10,sides:3,
			       icon:ramaddaBaseUrl+"/icons/triangle.png"}
			    ),				
		new GlyphType(this,GLYPH_HEXAGON, "Hexagon",
			     {strokeColor:this.getStrokeColor(),
			      strokeWidth:this.getStrokeWidth(),
			      strokeDashstyle:'solid',
			      strokeOpacity:1,
			      fillColor:"transparent",
			      fillOpacity:1.0},
			     OpenLayers.Handler.MyRegularPolygon,
			      {snapAngle:90,sides:6,
			       icon:ramaddaBaseUrl+"/icons/hexagon.png"}
			    ),		

	    ];
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
	initMap: function(map) {
	    SUPER.initMap.call(this)
	},
        initDisplay: function(embedded) {
	    if(!embedded) {
		SUPER.initDisplay.call(this)
	    }
	    let _this = this;
	    this.myLayer = this.map.createFeatureLayer("Annotation Features",false,null,{rendererOptions: {zIndexing: true}});
	    if(this.getProperty("layerIndex")) {
		this.myLayer.ramaddaLayerIndex = +this.getProperty("layerIndex");
	    }
	    this.icon = "/icons/map/marker-blue.png";
	    this.glyphs = this.doMakeMapGlyphs().map(glyph=>{
		if(glyph) return glyph;
		return null;
	    });
	    this.glyphMap = {};
	    this.glyphs.forEach(g=>{
		this.glyphMap[g.type]  = g;
	    });
	    if(embedded) {
		return;
	    }

	    this.map.featureClickHandler = e=>{
		if(this.command!=null) return;
		if(!e.feature || !e.feature.style)return;
		let style = e.feature.style;
		let showPopup = (html,props)=>{
		    let id = HU.getUniqueId("div");
		    let div = HU.div(['id',id]);
		    let location = e.feature.geometry.getBounds().getCenterLonLat();
		    if(this.getMap().currentPopup) {
			this.getMap().onPopupClose();
		    }
		    let popup =this.getMap().makePopup(location,div,props);
		    this.getMap().currentPopup = popup;
		    this.getMap().getMap().addPopup(popup);
		    jqid(id).html(html);
		}

		let doPopup = (html,props)=>{
		    let js =[];
		    //Parse out any script tags 
		    let regexp = /<script *src=("|')?([^ "']+)("|')?.*?<\/script>/g;
		    let array = [...html.matchAll(regexp)];
		    array.forEach(tuple=>{
			html = html.replace(tuple[0],"");
			let url = tuple[2];
			url = url.replace(/'/g,"");
			js.push(url);
//			console.log("JS:" + url);
		    });


		    //Run through any script tags and load them
		    //once done show the popup
		    let cb = ()=>{
//			console.log("cb");
			if(js[0]==null) {
//			    console.log("\tshowPopup");
			    showPopup(html,props);
			    return;
			}
			let url = js[0];
			js.splice(0,1);
//			console.log("\tloading:"+url);
			Utils.loadScript(url,cb);
		    };
		    cb();
		};
		let text= style.popupText;
		if(style.entryId) {
		    let wiki = style.wikiText;
		    if(!Utils.stringDefined(wiki))
			wiki = "+section title={{name}}\n{{simple}}\n-section";

		    let url = ramaddaBaseUrl + "/wikify";
		    let wikiCallback = html=>{
			doPopup(html,{width:"600",height:"400"});
		    };
		    let wikiError = error=>{
			console.error(error.responseText);
			alert("Error:" + error.responseText);
		    };		    
		    $.post(url,{
			doImports:"false",
			entryid:style.entryId,
			text:wiki},
			   wikiCallback).fail(wikiError);
		    return;
		}


		if(!Utils.stringDefined(text)) return;
		text = text.replace(/\n/g,"<br>");
		doPopup(text);
	    };

	    let control;
//	    if(!this.getDisplayOnly() || !Utils.isAnonymous()) {
	    if(!Utils.isAnonymous()) {
//		this.jq(ID_LEFT).html(HU.div([ID,this.domId(ID_COMMANDS),CLASS,"ramadda-display-editablemap-commands"]));
		var keyboardControl = new OpenLayers.Control();
		control = new OpenLayers.Control();
		let callbacks = {
		    keydown: function(event) {
			HtmlUtils.hidePopupObject();
			if(event.key=='Escape') {
			    _this.clearCommands();
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
		    highlight: function(feature) {
			let tmp = {};
//			_this.selectFeature(feature);
			$.extend(tmp,feature.style);
			tmp.strokeColor = "#000";
			if(tmp.pointRadius) tmp.pointRadius*=1.5;
			if(tmp.strokeWidth) tmp.strokeWidth*=2;
			//fontSize is, eg, 14px
			if(tmp.fontSize) { 
			    let match=String(tmp.fontSize).match(/([0-9\.]+)(.*)/);
			    if(match) {
				let num = 1.5*match[1];
				tmp.fontSize=num+match[2];
			    }
			    tmp.fontWeight="bold";
			}
			this.selectStyle = tmp;
			OpenLayers.Control.SelectFeature.prototype.highlight.apply(this,arguments);
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
			_this.doEdit(feature);
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
		    if(feature.image) {
			_this.checkImage(feature);
		    }
		    _this.featureChanged();
		};
		let mover =  this.addControl(ID_MOVER,"Click drag to move",new OpenLayers.Control.DragFeature(this.myLayer,{
		    onDrag: function(feature, pixel) {
			imageChecker(feature);
		    }
		}));

		let MyMover =  OpenLayers.Class(OpenLayers.Control.ModifyFeature, {
		    dragComplete: function() {
			OpenLayers.Control.ModifyFeature.prototype.dragComplete.apply(this, arguments);
			this.theDisplay.featureChanged();	    
			this.theDisplay.jq(ID_MESSAGE2).hide(1000);
		    },
		    dragVertex: function(vertex, pixel) {
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
			if(this.feature.image) {
			    imageChecker(this.feature);
			}
		    }
		});



		let resizer = new MyMover(this.myLayer,{
		    theDisplay:this,
		    onDrag: function(feature, pixel) {imageChecker(feature);},
		    mode:OpenLayers.Control.ModifyFeature.RESIZE|OpenLayers.Control.ModifyFeature.DRAG});
		let reshaper = new MyMover(this.myLayer, {
		    theDisplay:this,
		    onDrag: function(feature, pixel) {imageChecker(feature);},
		    createVertices:false,
		    mode:OpenLayers.Control.ModifyFeature.RESHAPE});
		this.addControl(ID_RESIZE,"Click to resize",resizer);
		this.addControl(ID_RESHAPE,"Click to reshape",reshaper);

		let menuBar=  "";
		[[ID_MENU_FILE,"File"],[ID_MENU_EDIT,"Edit"],[ID_MENU_NEW,"New"]].forEach(t=>{
		    menuBar+=   HU.div([ID,this.domId(t[0]),CLASS,"ramadda-menubar-button"],t[1])});
	    	menuBar = HU.div([CLASS,"ramadda-menubar"], menuBar);
		let message2 = HU.div([ID,this.domId(ID_MESSAGE2),CLASS,"ramadda-editablemap-message2"],"");
		this.jq(ID_MAP_CONTAINER).append(message2);
		let message3 = HU.div([ID,this.domId(ID_MESSAGE3),CLASS,"ramadda-editablemap-message3"],"");		
		if(this.getShowLegend()) {
		    this.jq(ID_MAP_CONTAINER).append(message3);
		}

		let message = HU.div([ID,this.domId(ID_MESSAGE),STYLE,HU.css("display","inline-block","white-space","nowrap","margin-left","10px")],"");
		let mapHeader = HU.div([STYLE,HU.css("margin-left","10px","xdisplay","inline-block"), ID,this.domId(ID_MAP)+"_header"]);
		menuBar= HU.table(['width','100%'],HU.tr(["valign","bottom"],HU.td(['xwidth','50%'],menuBar) +
							 HU.td(['width','50%'], message) +
							 HU.td(['align','right','style','padding-right:10px;','width','50%'],mapHeader)));
		this.jq(ID_TOP_LEFT).append(menuBar);
		this.jq(ID_MENU_NEW).click(function() {
		    _this.showNewMenu($(this));
		});
		this.jq(ID_MENU_FILE).click(function() {
		    _this.showFileMenu($(this));
		});
		this.jq(ID_MENU_EDIT).click(function() {
		    _this.showEditMenu($(this));
		});

		$(window).bind('beforeunload', function(){
		    if(!Utils.isAnonymous() && _this.featureHasBeenChanged) {
			return 'Changes have been made. Are you sure you want to leave?';
		    }
		});
	    } else {
		let menuBar=HU.div([STYLE,HU.css("display","inline-block"), ID,this.domId(ID_MAP)+"_header"]);
		this.jq(ID_TOP_LEFT).append(HU.center(menuBar));
	    }

	    let cmds = "";
	    this.glyphs.forEach(g=>{
		this.glyphMap[g.type]  = g;
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

	    if(this.getProperty("thisEntryType")=="geo_editable_json") {
		this.loadMap();
	    }
        },
    });
}



var GlyphType = function(display,type,label,style,handler,options) {
    const MapObject = function(display, glyphType,feature) {
	this.id = HU.getUniqueId("");
	feature.objectId = this.id;
	this.display = display;
	this.feature = feature;
	this.display.addGlyph(this);
    }


    MapObject.prototype = {
	getId:function() {
	    return this.id;
	}
    }


    this.display = display;
    this.label = label;
    this.type = type;
    this.glyphStyle = style;
    this.handler = handler;
    this.options = options || {};
    this.options.glyphType = type;
    this.options.display = display;
    this.options.mapGlyph = this;
    $.extend(this,{
	getStyle:function() {
	    return this.glyphStyle;
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
	isMap:  function() {
	    return this.options.isMap;
	},			
	isRoute: function() {
	    return this.type == GLYPH_ROUTE;
	},
	isIcon:  function() {
	    return this.getStyle().externalGraphic!=null;
	},	
	applyStyle: function(style) {
	    this.display.featureChanged();
	    for(a in style) {
		if(this.getStyle()[a]) this.getStyle()[a] = style[a];
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
		    if(_this.isLabel()) {
			_this.display.setCommand(null);
		    }
		}
	    });
	    this.drawer = new Drawer(layer);
	    this.display.addControl(this.type,"",this.drawer);
	    return this.drawer;
	},
    });	

}

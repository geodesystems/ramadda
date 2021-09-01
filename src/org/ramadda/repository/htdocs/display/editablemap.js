/**
   Copyright 2008-2019 Geode Systems LLC
*/

const DISPLAY_EDITABLEMAP = "editablemap";


addGlobalDisplayType({
    type: DISPLAY_EDITABLEMAP,
    label: "Editable Map",
    category:CATEGORY_MAPS,
    tooltip: makeDisplayTooltip("Editable map"),        
});


OpenLayers.Control.EditListener = OpenLayers.Class(OpenLayers.Control, {
    display:null,
    setDisplay:function(display) {
	this.display = display;
    },
    defaultHandlerOptions: {
        'single': true,
        'double': false,
        'pixelTolerance': 0,
        'stopSingle': false,
        'stopDouble': false
    },

    initialize: function(options) {
        this.handlerOptions = OpenLayers.Util.extend({},
						     this.defaultHandlerOptions);
        OpenLayers.Control.prototype.initialize.apply(this, arguments);
        this.handler = new OpenLayers.Handler.Click(this, {
            'click': this.trigger
        }, this.handlerOptions);
    },
    trigger: function(e) {
        var xy = this.display.map.getMap().getLonLatFromViewPortPx(e.xy);
        var lonlat = this.display.map.transformProjPoint(xy)
	this.display.handleEvent(e,lonlat);
    }

});



function RamaddaEditablemapDisplay(displayManager, id, properties) {
    const ID_OK  ="ok";
    const ID_APPLY  ="apply";
    const ID_CANCEL = "cancel";

    const ID_MENU_FILE = "menu_file";
    const ID_MENU_EDIT = "menu_edit";    

    const ID_DELETE_ALL = "deleteall";
    const ID_CUT = "cut";
    const ID_COPY= "copy";
    const ID_PASTE= "paste";        
    const ID_COMMANDS = "commands";
    const ID_PROPERTIES = "properties";
    const ID_SAVE = "save";
    const ID_SAVEAS = "saveas";    
    const ID_DOWNLOAD = "download";    
    const ID_SELECTOR = "selector";
    const ID_MOVER = "mover";
    const ID_MODIFIER = "modifier";    
    const SUPER = new RamaddaBaseMapDisplay(displayManager, id, DISPLAY_EDITABLEMAP, properties);
    RamaddaUtil.inherit(this,SUPER);
    addRamaddaDisplay(this);
    this.defineSizeByProperties();
    let myProps = [
	{label:'Editable Map Properties'},
	{p:"displayOnly",d:false},
	{p:"strokeColor",d:"blue"},
	{p:"strokeWidth",d:2},
	{p:"pointRadius",d:10},
	{p:"externalGraphic",d:"/map/marker-blue.png"},
	{p:"fontSize",d:"14px"},
	{p:"fontWeight",d:"normal"},
	{p:"fontFamily",d:"'Open Sans', Helvetica Neue, Arial, Helvetica, sans-serif"},
    ];
    
    displayDefineMembers(this, myProps, {
	commands: [],
        features: [],
	selected:{},
	getMap: function() {
	    return this.map;
	},
	handleEvent:function(event,lonlat) {
	    return;
	},
	setCommand:function(command) {
	    let glyph = this.glyphMap[command];
	    this.clearCommands();
	    this.commands.forEach(cmd=>{
		cmd.deactivate();
	    });
	    if(!command) return;
	    this.jq("new_" + command).addClass("ramadda-display-editablemap-command-active");
	    this.commands.forEach(cmd=>{
		if(cmd.name != command) {
		    return;
		}
		if(glyph) {
		    let styleMap = new OpenLayers.StyleMap({"default":{}});
		    let tmpStyle = {};
		    $.extend(tmpStyle,glyph.style);
		    if(glyph.isLabel()) {
			let text = prompt("Label text:",this.lastText);
			if(!text) return;
			this.lastText = text;
			tmpStyle.label = text;
		    }
		    cmd.handler.style = tmpStyle;
		    cmd.handler.layerOptions.styleMap=styleMap;
		}
		cmd.activate();
	    });
	},
	clearCommands:function() {
	    let buttons = this.jq(ID_COMMANDS).find(".ramadda-clickable");
	    buttons.removeClass("ramadda-display-editablemap-command-active");
	    buttons.each(function() {
		$(this).attr("selected",false);
	    });
	    this.commands.forEach(cmd=>{
		cmd.deactivate();
	    });
	    this.command= null;
	    this.features.redraw();
	},
	addFeatures:function(features) {
	    let layer = this.features;
	    layer.addFeatures(features);
	    features.forEach(feature=>{
		feature.layer = layer;
	    });
	},
	    
	addControl:function(name,control) {
	    control.name = name;
	    this.map.getMap().addControl(control);
	    this.commands.push(control);
	},

	doPaste: function() {
	    if(!this.clipboard) return;
	    let newOnes = this.clipboard.map(feature=>{return feature.clone();});
	    this.features.addFeatures(newOnes);
	},
	doEdit: function() {
	    this.clearCommands();
	    if(!this.features.selectedFeatures) return;
	    if(this.features.selectedFeatures.length==0) return;
	    let feature = this.features.selectedFeatures[0];
	    let html =HU.div([STYLE,HU.css("margin","8px")], "Feature: " + feature.type); 
	    this.features.redraw(feature);
            let popup = this.map.makePopup(feature.geometry.getBounds().getCenterLonLat(),html);
	    this.map.currentPopup = popup;
            this.map.getMap().addPopup(popup);
	},

	doProperties: function() {
	    let html = "";
	    html +=HU.formTable();
	    let props = ["strokeColor","strokeWidth","pointRadius","externalGraphic","fontSize","fontWeight","fontFamily"];
	    html+=HU.formEntry("Line Color: ", HU.input("",this.getStrokeColor(),[ID,this.domId("strokeColor")]));
	    html+=HU.formEntry("Line Width: ", HU.input("",this.getStrokeWidth(),[ID,this.domId("strokeWidth"),"size",4]));
	    html+=HU.formEntry("Point Radius: ", HU.input("",this.getPointRadius(),[ID,this.domId("pointRadius"),"size",4]));
	    html+=HU.formEntry("Font Size: ", HU.input("",this.getFontSize(),[ID,this.domId("fontSize"),"size",4]));
	    html+=HU.formEntry("Font Weight: ", HU.input("",this.getFontWeight(),[ID,this.domId("fontWeight"),"size",6]));
	    html+=HU.formEntry("Font Family: ", HU.input("",this.getFontFamily(),[ID,this.domId("fontFamily"),"size","60"]));	    

	    let icons = ["/map/marker-blue.png","/map/marker-gold.png","/map/marker-green.png","/map/marker.png","/map/POI.png","/map/arts.png","/map/bar.png","/map/binocular.png","/map/blue-dot.png","/map/blue-pushpin.png","/map/building.png","/map/burn.png","/map/bus.png","/map/cabs.png","/map/calendar.png","/map/camera.png","/map/campfire.png","/map/campground.png","/map/car.png","/map/caution.png","/map/coffeehouse.png","/map/convienancestore.png","/map/cycling.png","/map/dollar.png","/map/drinking_water.png","/map/earthquake.png","/map/electronics.png","/map/envelope.png","/map/euro.png","/map/fallingrocks.png","/map/ferry.png","/map/film.png","/map/firedept.png","/map/fishing.png","/map/flag.png","/map/gas.png","/map/glass.png","/map/globe.png","/map/golfer.png","/map/green-dot.png","/map/grn-pushpin.png","/map/grocerystore.png","/map/hammer.png","/map/helicopter.png","/map/hiker.png","/map/home.png","/map/homegardenbusiness.png","/map/horsebackriding.png","/map/hospitals.png","/map/hotsprings.png","/map/info.png","/map/info_circle.png","/map/lodging.png","/map/ltblu-pushpin.png","/map/ltblue-dot.png","/map/man.png","/map/marina.png","/map/mechanic.png","/map/motorcycling.png","/map/mountain.png","/map/movies.png","/map/orange-dot.png","/map/paper-plane.png","/map/parkinglot.png","/map/partly_cloudy.png","/map/pharmacy-us.png","/map/phone.png","/map/picnic.png","/map/pink-dot.png","/map/pink-pushpin.png","/map/plane.png","/map/police.png","/map/postoffice-us.png","/map/purple-dot.png","/map/purple-pushpin.png","/map/question.png","/map/rail.png","/map/rainy.png","/map/rangerstation.png","/map/realestate.png","/map/recycle.png","/map/red-dot.png","/map/red-pushpin.png","/map/restaurant.png","/map/sailing.png","/map/salon.png","/map/shopping-basket.png","/map/shopping.png","/map/ski.png","/map/smiley.png","/map/snack_bar.png","/map/snowflake_simple.png","/map/sportvenue.png","/map/star.png","/map/sticky-note.png","/map/subway.png","/map/sunny.png","/map/swimming.png","/map/toilets.png","/map/trail.png","/map/tram.png","/map/tree.png","/map/truck.png","/map/volcano.png","/map/water.png","/map/waterfalls.png","/map/wheel_chair_accessible.png","/map/woman.png","/map/yellow-dot.png","/map/yen.png","/map/ylw-pushpin.png"];


	    let options = "";
	    let graphic = this.getExternalGraphic();
	    icons.forEach(icon=>{
		let extra ="";
		let url =  ramaddaBaseUrl + icon;
		let label = icon.replace("/map/","");
		let attrs = ["value",icon, "data-class", "ramadda-select-icon","data-style", "", "img-src",url];
		if(icon == graphic)
		    attrs.push("selected","true");
		options+=HU.tag("option",attrs, " "+label);
	    });

            var select = HU.openTag("select", [ID,this.domId("externalGraphic")]);
	    select+=options;
            select+=HU.closeTag("select");

	    html+=HU.formEntry("Icon: ", select);
	    html+="</table>";
	    html+="<br>";
	    html+="<center>";
	    html +=HU.div([ID,this.domId(ID_APPLY), CLASS,"display-button"], "Apply");
	    html += SPACE2;
	    html +=HU.div([ID,this.domId(ID_OK), CLASS,"display-button"], "Ok");
	    html += SPACE2;
	    html +=HU.div([ID,this.domId(ID_CANCEL), CLASS,"display-button"], "Cancel");	    
	    html  = HU.div([CLASS,"wiki-editor-popup"], html);
	    html+="</center>";
	    let dialog = HU.makeDialog({content:html,anchor:this.jq(ID_MENU_FILE),title:"Map Properties",header:true,draggable:true,remove:false});

	    this.jq("externalGraphic").iconselectmenu().iconselectmenu("menuWidget").addClass("ui-menu-icons ramadda-select-icon");
	    let apply = () =>{
		let style = {};
		props.forEach(prop=>{
		    let value = this.jq(prop).val();
		    this.setProperty(prop, value);
		    if(prop == "externalGraphic") {
			value = ramaddaBaseUrl+  value;
		    }
		    style[prop] = value;
		});
		this.glyphs.forEach(g=>{
		    g.applyStyle(style);
		});
	    }
	    let close = ()=>{
		dialog.hide();
		dialog.remove();
	    }
	    this.jq(ID_OK).button().click(()=>{
		apply();
		close();
	    });
	    this.jq(ID_APPLY).button().click(()=>{
		apply();
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
		    alert("Saved");
		    return
		}
		console.log(result);
		if(result.error) {
		    alert(result.error);
		} else {
		    alert(result.message);
		}
	    }).fail(function(jqxhr, textStatus, error) {
		console.log("fail:" + result);
		alert("failed to save map:" + textStatus +" " + error);
	    });
	},
	doSave: function() {
	    if(this.getProperty("entryType")!="geo_editable_json") {
		alert("Entry is not the correct type");
		return;
	    }
	    let json = this.makeJson();
//	    console.log(json);
	    let url = ramaddaBaseUrl +"/entry/setfile"; 
            var args = {
                entryid: this.getProperty("entryId"),
		"file": json,
            };
            $.post(url, args, (result) => {
		if(result.error) {
		    alert(result.error);
		} else {
		    alert(result.message);
		}
	    }).fail(function(jqxhr, textStatus, error) {
		alert("failed to save map:" + textStatus +" " + error);
	    });
	},
	doDownload: function() {
	    let json = this.makeJson();
	    console.log(JSON.stringify(json,null,2));
	    Utils.makeDownloadFile("map.json",json);
	},

	makeJson: function() {
	    let list =[];
            this.features.features.forEach(feature=>{
		let geom = feature.geometry;
		let obj = {
		    type:feature.type,
		    points:[]
		};
		if(feature.style) obj.style = feature.style;
		list.push(obj);
		let vertices  = geom.getVertices();
//		console.log(feature.type +" vertices:" + vertices);
		vertices.forEach(vertex=>{
		    let pt = vertex.clone().transform(this.map.sourceProjection, this.map.displayProjection);
		    obj.points.push({latitude:pt.y,longitude:pt.x});
		});

	    });
	    return  JSON.stringify(list,null,2);
	},


	showFileMenu: function(button) {
	    let html ="";
	    if(!Utils.isAnonymous()) {	    
		html +=HU.div([ID,this.domId(ID_SAVE),CLASS,"ramadda-clickable"],"Save");
//		html +=HU.div([ID,this.domId(ID_SAVEAS),CLASS,"ramadda-clickable"],"Save As...");		
	    }
	    html+=
		HU.div([ID,this.domId(ID_DOWNLOAD),CLASS,"ramadda-clickable"],"Download")
	    html+=
		HU.div([ID,this.domId(ID_PROPERTIES),CLASS,"ramadda-clickable"],"Properties")	    
	    html  = HU.div([CLASS,"wiki-editor-popup"], html);
	    this.dialog = HU.makeDialog({content:html,anchor:button});
	    let _this = this;
	    

	    this.jq(ID_SAVE).click(function(){
		HtmlUtils.hidePopupObject();
		_this.doSave();
	    });
	    this.jq(ID_SAVEAS).click(function(){
		HtmlUtils.hidePopupObject();
		_this.doSaveAs();
	    });	    
	    this.jq(ID_DOWNLOAD).click(function(){
		HtmlUtils.hidePopupObject();
		_this.doDownload();
	    });	    
	    this.jq(ID_PROPERTIES).click(function(){
		HtmlUtils.hidePopupObject();
		_this.doProperties();
	    });
	},

	showEditMenu: function(button) {
	    let html = 
		HU.div([ID,this.domId(ID_CUT),CLASS,"ramadda-clickable"],"Cut") +
		HU.div([ID,this.domId(ID_COPY),CLASS,"ramadda-clickable"],"Copy") +
		HU.div([ID,this.domId(ID_PASTE),CLASS,"ramadda-clickable"],"Paste") +
		HU.div([CLASS,"ramadda-menu-divider"]) +						
		HU.div([ID,this.domId(ID_SELECTOR),CLASS,"ramadda-clickable"],"Select") +
		HU.div([ID,this.domId(ID_MOVER),CLASS,"ramadda-clickable"],"Move") +
		HU.div([ID,this.domId(ID_MODIFIER),CLASS,"ramadda-clickable"],"Modify") +
		HU.div([CLASS,"ramadda-menu-divider"]) +
		HU.div([ID,this.domId(ID_DELETE_ALL),CLASS,"ramadda-clickable"],"Delete All");		
	    
	    html  = HU.div([CLASS,"wiki-editor-popup display-menu"], html);
	    this.dialog = HU.makeDialog({content:html,anchor:button});
	    let buttons = [ID_SELECTOR,ID_MOVER,ID_MODIFIER];
	    let _this = this;
	    this.jq(ID_DELETE_ALL).click(function(){
		HtmlUtils.hidePopupObject();
		_this.doDeleteAll();
	    });
	    this.jq(ID_CUT).click(function(){
		HtmlUtils.hidePopupObject();
		_this.doCut();
	    });
	    this.jq(ID_COPY).click(function(){
		HtmlUtils.hidePopupObject();
		_this.doCopy();
	    });	    
	    this.jq(ID_PASTE).click(function(){
		HtmlUtils.hidePopupObject();
		_this.doPaste();
	    });
	    buttons.forEach(command=>{
		this.jq(command).click(function(){
		    HtmlUtils.hidePopupObject();
		    if(ID_SELECTOR==command)
			_this.features.selectedFeatures  = [];
		    _this.setCommand(command);
		});
	    });
	},
	
	doCut: function() {
	    this.clearCommands();
	    if(!this.features.selectedFeatures) return;
	    this.clipboard = this.features.selectedFeatures.map(feature=>{return feature;});
	    this.features.removeFeatures(this.features.selectedFeatures);
	},
	doDeleteAll: function() {
	    this.clearCommands();
	    if(!window.confirm("Are you sure you want to delete all map features?")) return
	    this.clipboard = this.features.features.map(feature=>{return feature;});
	    this.features.removeFeatures(this.features.features);
	},
	doCopy: function() {
	    this.clearCommands();
	    if(!this.features.selectedFeatures) return;
	    this.clipboard = this.features.selectedFeatures.map(feature=>{return feature;});
	},
	loadJson: function(map) {
//	    console.log(JSON.stringify(map,null,2));
	    map.forEach(mapGlyph=>{
		if(!mapGlyph.points || mapGlyph.points.length==0) {
		    console.log("No points defined:" + JSON.stringify(mapGlyph));
		    return;
		}
		let glyph = this.glyphMap[mapGlyph.type];
		let style = mapGlyph.style||(glyph?glyph.style:{});
		if(mapGlyph.type=="line" || mapGlyph.type=="freehand") {
		    let points = [];
		    mapGlyph.points.forEach(pt=>{
			points.push(new OpenLayers.Geometry.Point(pt.longitude,pt.latitude));
		    });
		    let feature = this.map.createPolygon("","",points,style);
		    feature.type=mapGlyph.type;
		    feature.style = style;
		    this.features.addFeatures([feature]);
		} else {
		    if(style["label"]) {
			style.pointRadius=0
		    }
		    let point =  MapUtils.createLonLat(mapGlyph.points[0].longitude, mapGlyph.points[0].latitude);
		    let feature = this.map.createPoint("",point,style);
		    feature.type=mapGlyph.type;
		    this.features.addFeatures([feature]);
		}
	    });
	    if(this.features.length>0) {
		let bounds = new OpenLayers.Bounds();
		this.features.features.forEach(feature=>{
		    bounds.extend(feature.geometry.getBounds());
		});
		this.map.zoomToExtent(bounds);
	    }
	},
	loadMap: function(entryId) {
	    let url = this.getProperty("fileUrl");
	    if(!url) return;
	    let _this = this;
            $.ajax({
                url: url,
                dataType: 'text',
                success: (data) => {
		    if(data=="") data="[]";
		    try {
			_this.loadJson(JSON.parse(data));
		    } catch(err) {
			alert("failed to load map:" + err);
			console.log("map json:" + data);
		    }
                }
            }).fail(err=>{
		alert("failed to load map:" + err);
	    });



	},
	doMakeMapGlyphs:function() {
	    return [
		new MapGlyph(this,"marker","Marker",
			     {strokeWidth:0, 
			      fillColor:"transparent",
			      externalGraphic: ramaddaBaseUrl+this.getExternalGraphic(),
			      pointRadius:this.getPointRadius()},
			     OpenLayers.Handler.Point),
		new MapGlyph(this,"circle","Circle",
			     {strokeWidth:this.getProperty("strokeWidth",2), 
			      fillColor:"transparent",
			      strokeColor:this.getStrokeColor(),
			      pointRadius:this.getPointRadius()},
			     OpenLayers.Handler.Point),
		new MapGlyph(this,"label","Label",
			     {label : "label",
			      fontColor: this.getProperty("labelFontColor","#000"),
			      fontSize: this.getFontSize(),
			      fontFamily: this.getFontFamily(),
			      fontWeight: this.getFontWeight(),
			      labelAlign: this.getProperty("labelAlign","lb"),
			      labelXOffset: this.getProperty("labelXOffset","0"),
			      labelYOffset: this.getProperty("labelYOffset","0"),
			      labelOutlineColor:this.getProperty("labelOutlineColor","#fff"),
			      labelOutlineWidth: this.getProperty("labelOutlineWidth","0"),
			      labelSelect:true,
			     }, OpenLayers.Handler.Point),
		new MapGlyph(this,"line", "Line",
			     {strokeColor:this.getStrokeColor(),
			      strokeWidth:this.getStrokeWidth()},
			     OpenLayers.Handler.Path),
		new MapGlyph(this,"freehand","Freehand",
			     {strokeColor:this.getStrokeColor(),
			      strokeWidth:this.getStrokeWidth()},
			     OpenLayers.Handler.Path,
			     {freehand:true})		
	    ];
	},
        initDisplay: function() {
            SUPER.initDisplay.call(this);
	    this.features = this.map.createFeatureLayer("Features",false);
	    this.map.featureClickHandler = e=>{
	    };
	    this.icon = "/icons/map/marker-blue.png";
            this.eventHandler = new OpenLayers.Control.EditListener();
	    this.eventHandler.setDisplay(this);
            this.map.getMap().addControl(this.eventHandler);
            this.eventHandler.activate();	    
	    let _this = this;

	    if(!this.getDisplayOnly()) {
		this.jq(ID_LEFT).html(HU.div([ID,this.domId(ID_COMMANDS),CLASS,"ramadda-display-editablemap-commands"]));
		var keyboardControl = new OpenLayers.Control();
		var control = new OpenLayers.Control();
		var callbacks = { keydown: function(evt) {
                    if(!evt.metaKey) return;
		    console.log(evt.keyCode);
		    switch(evt.keyCode) {
		    case 88: // x
			_this.doCut();
			return;
		    case 86: // v
			_this.doPaste();
			return;
		    case 67: // c
			_this.doCopy();
			return;
		    case 69: // e
			_this.doEdit();
			return;		    		    

		    }
		}};
		var options = {};
		var handler = new OpenLayers.Handler.Keyboard(control, callbacks, options);
		handler.activate();
		this.map.getMap().addControl(keyboardControl);

		this.features.events.on({
                    'featureselected': function(feature) {
			console.log("selected");
                    },
                    'featureunselected': function(feature) {
			console.log("unselected");
                    }
		});

		this.addControl(ID_SELECTOR,new OpenLayers.Control.SelectFeature(this.features,
										 {
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
									     }
									    ));

	    this.addControl(ID_MOVER,new OpenLayers.Control.DragFeature(this.features));
	    let Modder = OpenLayers.Class(OpenLayers.Control.ModifyFeature, {
		dragStart:function(feature) {
		    this.theFeature = feature;
		    return OpenLayers.Control.ModifyFeature.prototype.dragStart.apply(this,arguments);
		},
		dragVertex:function(vertex,pixel) {
		    var pos = this.map.getLonLatFromViewPortPx(pixel);
//		    console.log("dragVertex:" + pos);
		    OpenLayers.Control.ModifyFeature.prototype.dragVertex.apply(this,arguments);
		}
	    });
	    this.addControl(ID_MODIFIER,new Modder(this.features,{}));

	    let menuBar=  HU.div([ID,this.domId(ID_MENU_FILE),CLASS,"ramadda-menubar-button"],"File")+
		HU.div([ID,this.domId(ID_MENU_EDIT),CLASS,"ramadda-menubar-button"],"Edit")	;

	    menuBar = HU.div([CLASS,"ramadda-menubar"], menuBar);
	    this.jq(ID_TOP_LEFT).append(menuBar);

	    this.jq(ID_MENU_FILE).click(function() {
		_this.showFileMenu($(this));
	    });
	    this.jq(ID_MENU_EDIT).click(function() {
		_this.showEditMenu($(this));
	    });

	    }

	    let cmds = "";
	    this.glyphs = this.doMakeMapGlyphs();
	    this.jq(ID_COMMANDS).html(cmds);
	    this.glyphMap = {};
	    this.defaultStyle = {
	    };
	    this.glyphs.forEach(g=>{
		this.glyphMap[g.type]  = g;
		g.createDrawer();
		this.jq(ID_COMMANDS).append(HU.div([ID,this.domId("new_" + g.type),CLASS,"ramadda-clickable ramadda-display-editablemap-command"],"New " + g.label));
		this.jq("new_" + g.type).button().click(function(){
		    if($(this).hasClass("ramadda-display-editablemap-command-active")) {
			_this.setCommand(null);
		    } else {
			_this.setCommand(g.type);
		    }
		});
	    });

	    if(this.getProperty("entryType")=="geo_editable_json") {
		this.loadMap(this.getProperty("entryId"));
	    }
        },
    });
}


var MapGlyph = function(map,type,label,style,handler,options) {
    this.map = map;
    this.label = label;
    this.type = type;
    this.style = style;
    this.handler = handler;
    this.options = options;
    this.options = options || {};
    $.extend(this,{
	isLabel:  function() {
	    return this.style.label!=null;
	},
	isIcon:  function() {
	    return this.style.externalGraphic!=null;
	},	
	applyStyle: function(style) {
	    for(a in style) {
		if(this.style[a]) this.style[a] = style[a];
	    }
	},
	createDrawer:function() {
	    let _this = this;
	    let layer = this.map.features;
	    let Drawer = OpenLayers.Class(OpenLayers.Control.DrawFeature, {
		initialize: function(layer, options) {
		    let defaultStyle = OpenLayers.Util.extend({}, OpenLayers.Feature.Vector.style["default"]);
		    defaultStyle={};
		    $.extend(defaultStyle, _this.style);		    
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
		    feature.type = _this.type;
		    if(this.handler.style) {
			feature.style=this.handler.style;
//			console.log("s1:" + JSON.stringify(feature.style));
		    } else   {
			feature.style=style;
//			console.log("s2:" + JSON.stringify(feature.style));
		    }
		    this.layer.redraw();
		    if(_this.isLabel()) {
			_this.map.setCommand(null);
		    }

		}
	    });
	    this.drawer = new Drawer(layer);
	    this.map.addControl(this.type,this.drawer);
	    return this.drawer;
	},
    });	

}

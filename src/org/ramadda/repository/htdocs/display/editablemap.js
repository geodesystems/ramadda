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



function RamaddaEditablemapDisplay(displayManager, id, properties) {



OpenLayers.Handler.ImageHandler = OpenLayers.Class(OpenLayers.Handler.RegularPolygon, {
    initialize: function(control, callbacks, options) {
	OpenLayers.Handler.RegularPolygon.prototype.initialize.apply(this,arguments);
	this.display = options.display;
    },
    finalize: function() {
	this.theImage = this.image;
	this.image =null;
	OpenLayers.Handler.RegularPolygon.prototype.finalize.apply(this,arguments);
    },
    move: function(evt) {
	OpenLayers.Handler.RegularPolygon.prototype.move.apply(this,arguments);
	let mapBounds = this.feature.geometry.getBounds();
	if(this.image) {
	    this.display.map.removeLayer(this.image);
	}
	let b = this.display.map.transformProjBounds(mapBounds);
	this.image=  this.display.map.addImageLayer("","","",this.style.imageUrl,true,  b.top,b.left,b.bottom,b.right);
	this.image.setOpacity(this.style.imageOpacity);
    }
    
});





    const ID_MESSAGE  ="message";
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
    const ID_PROPERTIES = "properties";
    const ID_NAVIGATE = "navigate";
    const ID_SAVE = "save";
    const ID_SAVEAS = "saveas";    
    const ID_DOWNLOAD = "download";    
    const ID_SELECTOR = "selector";
    const ID_EDIT = "edit";
    const ID_MOVER = "mover";
    const ID_RESIZE = "resize";
    const ID_RESHAPE = "reshape";    




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
	{p:"imageOpacity",d:1},
    ];
    
    displayDefineMembers(this, myProps, {
	commands: [],
        myLayer: [],
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
	    this.commands.every(cmd=>{
		if(cmd.name != command) {
		    return true;
		}
		if(glyph) {
		    let styleMap = new OpenLayers.StyleMap({"default":{}});
		    let tmpStyle = {};
		    $.extend(tmpStyle,glyph.style);
		    if(glyph.isImage()) {
			let url = prompt("Image URL:",this.lastImageUrl);
			if(!url) return;
			this.lastImageUrl = url;
			tmpStyle.imageUrl = this.lastImageUrl;
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
		if(cmd.message)
		    this.showMessage(cmd.message);
		cmd.activate();
		return false;
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
	    this.myLayer.redraw();
	},
	addFeatures:function(features) {
	    let layer = this.myLayer;
	    layer.addFeatures(features);
	    features.forEach(feature=>{
		feature.layer = layer;
	    });
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
	    let newOnes = this.clipboard.map(feature=>{return feature.clone();});
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
	    this.myLayer.addFeatures(newOnes);
	},
	doEdit: function(feature) {
//	    this.clearCommands();
	    if(!feature) {
		if(!this.myLayer.selectedFeatures) return;
		if(this.myLayer.selectedFeatures.length==0) return;
		feature = this.myLayer.selectedFeatures[0];
	    }
	    if(!feature) return;
	    let style = feature.style;
	    let html =HU.div([STYLE,HU.css("margin","8px")], "Feature: " + feature.type); 
	    this.myLayer.redraw(feature);
	    let apply = props=>{
		props.forEach(prop=>{
		    if(prop=="labelSelect") return;
		    let v = this.jq(prop).val();
		    if(prop=="label") {
			v = v.replace(/\\n/g,"\n");
		    }
		    feature.style[prop] = v;
		});
		if(feature.style.imageUrl) {
		    if(feature.image) feature.image.setOpacity(feature.style.imageOpacity);
		    this.checkImage(feature);
		}
		this.myLayer.redraw();
	    };
	    if(feature.image && Utils.isDefined(feature.image.opacity)) {
		feature.style.imageOpacity=feature.image.opacity;
	    }

	    this.doProperties(feature.style,apply, feature);
	},

	
	makeMenu: function(html) {
	    return  HU.div([CLASS,"wiki-editor-popup"], html);
	},
	menuItem: function(id,label) {
	    return  HU.div([ID,id,CLASS,"ramadda-clickable"],label);
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
	    } else {
		props = ["strokeColor","strokeWidth","pointRadius","externalGraphic","fontSize","fontWeight","fontFamily"];
	    }
	    props.forEach(prop=>{
		if(prop=="labelSelect") return;
		let label = Utils.makeLabel(prop);
		let widget;
		if(prop=="externalGraphic") {
		    let icons = ["/map/marker-blue.png","/map/marker-gold.png","/map/marker-green.png","/map/marker.png","/map/POI.png","/map/arts.png","/map/bar.png","/map/binocular.png","/map/blue-dot.png","/map/blue-pushpin.png","/map/building.png","/map/burn.png","/map/bus.png","/map/cabs.png","/map/calendar.png","/map/camera.png","/map/campfire.png","/map/campground.png","/map/car.png","/map/caution.png","/map/coffeehouse.png","/map/convienancestore.png","/map/cycling.png","/map/dollar.png","/map/drinking_water.png","/map/earthquake.png","/map/electronics.png","/map/envelope.png","/map/euro.png","/map/fallingrocks.png","/map/ferry.png","/map/film.png","/map/firedept.png","/map/fishing.png","/map/flag.png","/map/gas.png","/map/glass.png","/map/globe.png","/map/golfer.png","/map/green-dot.png","/map/grn-pushpin.png","/map/grocerystore.png","/map/hammer.png","/map/helicopter.png","/map/hiker.png","/map/home.png","/map/homegardenbusiness.png","/map/horsebackriding.png","/map/hospitals.png","/map/hotsprings.png","/map/info.png","/map/info_circle.png","/map/lodging.png","/map/ltblu-pushpin.png","/map/ltblue-dot.png","/map/man.png","/map/marina.png","/map/mechanic.png","/map/motorcycling.png","/map/mountain.png","/map/movies.png","/map/orange-dot.png","/map/paper-plane.png","/map/parkinglot.png","/map/partly_cloudy.png","/map/pharmacy-us.png","/map/phone.png","/map/picnic.png","/map/pink-dot.png","/map/pink-pushpin.png","/map/plane.png","/map/police.png","/map/postoffice-us.png","/map/purple-dot.png","/map/purple-pushpin.png","/map/question.png","/map/rail.png","/map/rainy.png","/map/rangerstation.png","/map/realestate.png","/map/recycle.png","/map/red-dot.png","/map/red-pushpin.png","/map/restaurant.png","/map/sailing.png","/map/salon.png","/map/shopping-basket.png","/map/shopping.png","/map/ski.png","/map/smiley.png","/map/snack_bar.png","/map/snowflake_simple.png","/map/sportvenue.png","/map/star.png","/map/sticky-note.png","/map/subway.png","/map/sunny.png","/map/swimming.png","/map/toilets.png","/map/trail.png","/map/tram.png","/map/tree.png","/map/truck.png","/map/volcano.png","/map/water.png","/map/waterfalls.png","/map/wheel_chair_accessible.png","/map/woman.png","/map/yellow-dot.png","/map/yen.png","/map/ylw-pushpin.png"];
		    let options = "";
		    let graphic = values[prop];
		    if(graphic===null)
			graphic = this.getExternalGraphic();
		    icons.forEach(icon=>{
			let extra ="";
			let url =  ramaddaBaseUrl + icon;
			let lbl = icon.replace("/map/","");
			let attrs = ["value",icon, "data-class", "ramadda-select-icon","data-style", "", "img-src",url];
			if(icon == graphic)
			    attrs.push("selected","true");
			options+=HU.tag("option",attrs, " "+lbl);
		    });

		    var select = HU.openTag("select", [ID,this.domId("externalGraphic")]);
		    select+=options;
		    select+=HU.closeTag("select");
		    widget = select;

		} else {
		    let v = values[prop];
		    if(!Utils.isDefined(v)) {
			let propFunc = "get" + prop[0].toUpperCase()+prop.substring(1);
			v = propFunc?this[propFunc]():this.getProperty(prop);
//			console.log("V:" + v +" " + "get" + prop[0].toUpperCase()+prop.substring(1));
		    } else {
//			console.log("value:" + v);
		    }
		    let size = "20";
		    if(prop=="label") {
//			v = v.replace(/\n/g,"\\n");
			size="80"
			widget =  HU.textarea("",v,[ID,this.domId(prop),"rows",5,"cols", 60]);
		    } else {
			if(prop=="strokeWidth" || prop=="pointRadius" || prop=="fontSize" || prop=="fontWeight" || prop=="imageOpacity") size="4";
			else if(prop=="fontFamily") size="60";
			else if(prop=="imageUrl") size="80";		    
			widget =  HU.input("",v,[ID,this.domId(prop),"size",size]);
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
	    let dialog = HU.makeDialog({content:html,anchor:this.jq(ID_MENU_FILE),title:"Map Properties",header:true,draggable:true,remove:false});

	    this.jq("externalGraphic").iconselectmenu().iconselectmenu("menuWidget").addClass("ui-menu-icons ramadda-select-icon");
	    if(apply==null)
		apply = () =>{
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
		this.map.ignoreKeyEvents = false;
		dialog.hide();
		dialog.remove();
	    }
	    if(feature) {
		this.jq(ID_DELETE).button().click(()=>{
		    this.myLayer.removeFeatures([feature]);
		    close();
		});
	    }
	    this.jq(ID_OK).button().click(()=>{
		apply(props);
		close();
	    });
	    this.jq(ID_APPLY).button().click(()=>{
		apply(props);
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
		console.log(result);
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
	    if(this.getProperty("entryType")!="geo_editable_json") {
		this.showMessage("Entry is not the correct type");
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
		    this.showMessage(result.error);
		} else {
		    this.showMessage(result.message);
		}
	    }).fail(function(jqxhr, textStatus, error) {
		this.showMessage("failed to save map:" + textStatus +" " + error);
	    });
	},
	doDownload: function() {
	    let json = this.makeJson();
	    console.log(JSON.stringify(json,null,2));
	    Utils.makeDownloadFile("map.json",json);
	},
	makeJson: function() {
	    let list =[];
            this.myLayer.features.forEach(feature=>{
		if(!feature.type) return;
		let geom = feature.geometry;
		let obj = {
		    type:feature.type,
		    points:[]
		};
		if(feature.style) {
		    if(feature.image && Utils.isDefined(feature.image.opacity)) {
			feature.style.imageOpacity=feature.image.opacity;
		    }
		    obj.style = feature.style;
		}
		list.push(obj);
		let vertices  = geom.getVertices();
		obj.geometryType=geom.CLASS_NAME;
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
			obj.points.push({latitude:pt.y,longitude:pt.x});
		    });
		}

	    });
	    return  JSON.stringify(list,null,2);
	},


	showFileMenu: function(button) {
	    let html ="";
	    if(!Utils.isAnonymous()) {	    
		html +=this.menuItem(this.domId(ID_SAVE),"Save");
		//html +=this.menuItem(this.domId(ID_SAVEAS),"Save As...");		
	    }
	    html+= this.menuItem(this.domId(ID_DOWNLOAD),"Download")
	    html+= this.menuItem(this.domId(ID_PROPERTIES),"Set Default Properties");
//	    html+= this.menuItem(this.domId(ID_NAVIGATE),"Navigate");	    

	    html  = this.makeMenu(html);

	    this.dialog = HU.makeDialog({content:html,anchor:button});
	    let _this = this;

	    this.jq(ID_NAVIGATE).click(function() {
		HtmlUtils.hidePopupObject();
		_this.setCommand(null);
	    });
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
	showNewMenu: function(button) {
	    let html ="";
	    this.glyphs.forEach(g=>{
		html+= this.menuItem(this.domId("menunew_" + g.type),"New " + g.label);
	    });
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
	    let html = 
		this.menuItem(this.domId(ID_CUT),"Cut") +
		this.menuItem(this.domId(ID_COPY),"Copy") +
		this.menuItem(this.domId(ID_PASTE),"Paste") +
		HU.div([CLASS,"ramadda-menu-divider"]) +						
		this.menuItem(this.domId(ID_SELECTOR),"Select") +
		HU.div([CLASS,"ramadda-menu-divider"]) +						
		this.menuItem(this.domId(ID_MOVER),"Move") +
		this.menuItem(this.domId(ID_RESHAPE),"Reshape") +
		this.menuItem(this.domId(ID_RESIZE),"Resize") +
		HU.div([CLASS,"ramadda-menu-divider"]) +						
		this.menuItem(this.domId(ID_TOFRONT),"To Front") +
		this.menuItem(this.domId(ID_TOBACK),"To Back") +		
		HU.div([CLASS,"ramadda-menu-divider"]) +		
		this.menuItem(this.domId(ID_EDIT),"Edit Properties") +
		HU.div([CLASS,"ramadda-menu-divider"]) +
		this.menuItem(this.domId(ID_DELETE_ALL),"Delete All");		
	    
	    html  = this.makeMenu(html);
	    this.dialog = HU.makeDialog({content:html,anchor:button});
	    let buttons = [ID_EDIT,ID_SELECTOR,ID_MOVER,ID_RESHAPE,ID_RESIZE];
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
	    this.jq(ID_TOFRONT).click(function(){
		HtmlUtils.hidePopupObject();
		_this.toFront(true);
	    });
	    this.jq(ID_TOBACK).click(function(){
		HtmlUtils.hidePopupObject();
		_this.toFront(false);
	    });
	    buttons.forEach(command=>{
		this.jq(command).click(function(){
		    HtmlUtils.hidePopupObject();
		    if(ID_SELECTOR==command) {
			_this.myLayer.selectedFeatures  = [];
		    }
		    _this.setCommand(command);
		});
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
		    this.map.removeLayer(feature.image);
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
	toFront: function(toFront) {
	    if(!this.myLayer.selectedFeatures) return;
//	    this.clearCommands();
	    let selected = this.myLayer.selectedFeatures;
	    let features = this.myLayer.features;
	    selected.forEach(s=>{
		const index = features.indexOf(s);
		console.log("index:" + index);
		if (index > -1) {
		    features.splice(index, 1);
		    if(toFront)
			features.push(s);
		    else
			features.unshift(s);			
		}
	    });
	    this.myLayer.redraw();
	},

	doCut: function() {
	    this.clearCommands();
	    if(this.myLayer.selectedFeatures) {
		this.removeImages(this.myLayer.selectedFeatures);
		let features = this.myLayer.selectedFeatures.map(feature=>{return feature;});
		this.setClipboard(features);
		this.myLayer.removeFeatures(features);
	    }
	},
	doDeleteAll: function() {
	    this.clearCommands();
	    if(!window.confirm("Are you sure you want to delete all map features?")) return
	    this.removeImages(this.myLayer.features);
	    this.setClipboard(this.myLayer.features.map(feature=>{return feature;}));
	    this.myLayer.removeFeatures(this.myLayer.features);
	},
	doCopy: function() {
	    this.clearCommands();
	    if(!this.myLayer.selectedFeatures) return;
	    this.setClipboard(this.myLayer.selectedFeatures.map(feature=>{return feature;}));
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
		if(style.label) {
		    style.pointRadius=0
		}
		let feature;
		if(mapGlyph.points.length>1) {
		    let points = [];
		    mapGlyph.points.forEach(pt=>{
			points.push(new OpenLayers.Geometry.Point(pt.longitude,pt.latitude));
		    });
		    if(mapGlyph.geometryType=="OpenLayers.Geometry.Polygon") {
			this.map.transformPoints(points);
			let linearRing = new OpenLayers.Geometry.LinearRing(points);
			let geom = new OpenLayers.Geometry.Polygon(linearRing);
			feature = new OpenLayers.Feature.Vector(geom,null,style);
		    } else {
			feature = this.map.createPolygon("","",points,style);
		    }
		} else {
		    let point =  MapUtils.createLonLat(mapGlyph.points[0].longitude, mapGlyph.points[0].latitude);
		    feature = this.map.createPoint("",point,style);
		}
		feature.type=mapGlyph.type;
		feature.style = style;
		this.checkImage(feature);

		this.myLayer.addFeatures([feature]);

	    });
	    if(this.myLayer.length>0) {
		let bounds = new OpenLayers.Bounds();
		this.myLayer.features.forEach(feature=>{
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
			this.showMessage("failed to load map:" + err);
			console.log("error:" + err);
			console.log("map json:" + data);
		    }
                }
            }).fail(err=>{
		this.showMessage("failed to load map:" + err);
	    });



	},
	doMakeMapGlyphs:function() {
	    return [
		new MapGlyph(this,"marker","Marker",
			     {strokeWidth:0, 
			      fillColor:"transparent",
			      externalGraphic: ramaddaBaseUrl+this.getExternalGraphic(),
			      pointRadius:this.getPointRadius(10)},
			     OpenLayers.Handler.Point),
		new MapGlyph(this,"point","Point",
			     {strokeWidth:this.getProperty("strokeWidth",2), 
			      fillColor:"transparent",
			      strokeColor:this.getStrokeColor(),
			      pointRadius:this.getPointRadius(4)},
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
		new MapGlyph(this,"box", "Box",
			     {strokeColor:this.getStrokeColor(),
			      strokeWidth:this.getStrokeWidth(),
			      fillColor:"transparent",
			      fillOpacity:1.0},
			     OpenLayers.Handler.RegularPolygon,
			     {snapAngle:90,sides:4,irregular:true}
			    ),
		new MapGlyph(this,"circle", "Circle",
			     {strokeColor:this.getStrokeColor(),
			      strokeWidth:this.getStrokeWidth(),
			      fillColor:"transparent",
			      fillOpacity:1.0},
			     OpenLayers.Handler.RegularPolygon,
			     {snapAngle:45,sides:40}
			    ),
		new MapGlyph(this,"triangle", "Triangle",
			     {strokeColor:this.getStrokeColor(),
			      strokeWidth:this.getStrokeWidth(),
			      fillColor:"transparent",
			      fillOpacity:1.0},
			     OpenLayers.Handler.RegularPolygon,
			     {snapAngle:10,sides:3}
			    ),				
		new MapGlyph(this,"hexagon", "Hexagon",
			     {strokeColor:this.getStrokeColor(),
			      strokeWidth:this.getStrokeWidth(),
			      fillColor:"transparent",
			      fillOpacity:1.0},
			     OpenLayers.Handler.RegularPolygon,
			     {snapAngle:90,sides:6}
			    ),		
		new MapGlyph(this,"line", "Line",
			     {strokeColor:this.getStrokeColor(),
			      strokeWidth:this.getStrokeWidth()},
			     OpenLayers.Handler.Path,{maxVertices:2}),		

		new MapGlyph(this,"polyline", "Polyline",
			     {strokeColor:this.getStrokeColor(),
			      strokeWidth:this.getStrokeWidth()},
			     OpenLayers.Handler.Path),
		new MapGlyph(this,"freehand","Freehand",
			     {strokeColor:this.getStrokeColor(),
			      strokeWidth:this.getStrokeWidth()},
			     OpenLayers.Handler.Path,
			     {freehand:true}),
		new MapGlyph(this,"image", "Image",
			     {strokeColor:"transparent",
			      strokeWidth:1,
			      imageOpacity:this.getImageOpacity(1),
			      fillColor:"transparent"},
			     OpenLayers.Handler.ImageHandler,
			     {snapAngle:90,sides:4,irregular:true,isImage:true}
			    ),
		
	    ];
	},
	showMessage:function(msg)  {
	    this.jq(ID_MESSAGE).html(msg);
	    this.jq(ID_MESSAGE).show();
	    if(this.messageErase) clearTimeout(this.messageErase);
	    this.messageErase = setTimeout(()=>{
		this.jq(ID_MESSAGE).hide();
	    },3000);
	},
        initDisplay: function() {
            SUPER.initDisplay.call(this)

	    this.myLayer = this.map.createFeatureLayer("Features",false,null,{rendererOptions: {zIndexing: true}});
	    this.icon = "/icons/map/marker-blue.png";
	    let _this = this;
	    let control;


	    if(!this.getDisplayOnly() || !Utils.isAnonymous()) {
//		this.jq(ID_LEFT).html(HU.div([ID,this.domId(ID_COMMANDS),CLASS,"ramadda-display-editablemap-commands"]));
		var keyboardControl = new OpenLayers.Control();
		control = new OpenLayers.Control();
		var callbacks = { keydown: function(evt) {
                    if(!evt.metaKey) return;
//		    console.log(evt.keyCode);
		    switch(evt.keyCode) {
		    case 88: // x
			_this.doCut();
			return;
		    case 86: // v
			_this.doPaste(evt);
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
		this.addControl(ID_SELECTOR,"Click-drag to select",new OpenLayers.Control.SelectFeature(this.myLayer, {
		    highlight: function(feature) {
			let tmp = {};
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
//			console.log(JSON.stringify(tmp));
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
		};
		let MyMover =  OpenLayers.Class(OpenLayers.Control.ModifyFeature, {
		    dragVertex: function(vertex, pixel) {
			if(!this.feature.image) {
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
			imageChecker(this.feature);
		    }
		});
		let mover =  this.addControl(ID_MOVER,"Click drag to move",new OpenLayers.Control.DragFeature(this.myLayer,{
		    onDrag: function(feature, pixel) {imageChecker(feature);}
		}));
		let resizer = new MyMover(this.myLayer,{
		    onDrag: function(feature, pixel) {imageChecker(feature);},
		    mode:OpenLayers.Control.ModifyFeature.RESIZE|OpenLayers.Control.ModifyFeature.DRAG});
		let reshaper = new MyMover(this.myLayer, {
		    onDrag: function(feature, pixel) {imageChecker(feature);},
		    createVertices:false,
		    mode:OpenLayers.Control.ModifyFeature.RESHAPE});
		this.addControl(ID_RESIZE,"Click to resize",resizer);
		this.addControl(ID_RESHAPE,"Click to reshape",reshaper);

		let menuBar=  HU.div([ID,this.domId(ID_MENU_FILE),CLASS,"ramadda-menubar-button"],"File")+
		    HU.div([ID,this.domId(ID_MENU_EDIT),CLASS,"ramadda-menubar-button"],"Edit") +
		    HU.div([ID,this.domId(ID_MENU_NEW),CLASS,"ramadda-menubar-button"],"New");		    
	    	menuBar = HU.div([CLASS,"ramadda-menubar"], menuBar);
		let message = HU.span([ID,this.domId(ID_MESSAGE),STYLE,HU.css("margin-left","10px")],"");
		menuBar+=message;
		let mapHeader = HU.div([STYLE,HU.css("margin-left","20px","display","inline-block"), ID,this.domId(ID_MAP)+"_header"]);
		menuBar= HU.table(['width','100%'],HU.tr(["valign","bottom"],HU.td(['width','50%'],menuBar) +
							 HU.td(['width','50%'],mapHeader)));
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

	    } else {
		let menuBar=HU.div([STYLE,HU.css("display","inline-block"), ID,this.domId(ID_MAP)+"_header"]);
		this.jq(ID_TOP_LEFT).append(HU.center(menuBar));
	    }

	    let cmds = "";
	    this.glyphs = this.doMakeMapGlyphs();
	    this.glyphMap = {};
	    this.glyphs.forEach(g=>{
		this.glyphMap[g.type]  = g;
		g.createDrawer();
	    });

	    this.jq(ID_COMMANDS).html(cmds);
	    this.defaultStyle = {
	    };
	    /**** Don't do the legend based buttons for now
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
	    */


	    if(this.getProperty("entryType")=="geo_editable_json") {
		this.loadMap(this.getProperty("entryId"));
		//Do it in a bit so the layer gets its bounds set
		setTimeout(()=>{
		    this.map.zoomToLayer(this.myLayer);		    
		},1000);
	    }
        },
    });
}


var MapGlyph = function(display,type,label,style,handler,options) {
    this.display = display;
    this.label = label;
    this.type = type;
    this.style = style;
    this.handler = handler;
    this.options = options || {};
    this.options.display = display;
    this.options.mapGlyph = this;
    $.extend(this,{
	isLabel:  function() {
	    return this.style.label!=null;
	},
	isImage:  function() {
	    return this.options.isImage;
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
	    let layer = this.display.myLayer;
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
		    if(this.handler.theImage) {
			feature.image = this.handler.theImage;
		    }
		    feature.type = _this.type;
		    let newStyle;
		    if(this.handler.style) {
			newStyle=this.handler.style;
		    }
		    if(newStyle) {
			if(feature.style && feature.style.label)
			    newStyle.label = feature.style.label;
			else if(this.handler.style && this.handler.style.label)
			    newStyle.label = this.handler.style.label;
//			console.log(JSON.stringify(newStyle));
			let tmp = {};
			$.extend(tmp, newStyle);
			feature.style=tmp;
		    }
		    this.layer.redraw();
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

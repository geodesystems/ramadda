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
    const ID_COMMANDS = "commands";
    const ID_SELECTOR = "selector";
    const ID_MOVER = "mover";
    const ID_MODIFIER = "modifier";    
    const SUPER = new RamaddaBaseMapDisplay(displayManager, id, DISPLAY_EDITABLEMAP, properties);
    RamaddaUtil.inherit(this,SUPER);
    addRamaddaDisplay(this);
    this.defineSizeByProperties();
    let myProps = [
	{label:'Editable Map Properties'},
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
	    this.clearCommands();
	    this.jq(command).addClass("ramadda-display-editablemap-command-active");
	    this.commands.forEach(cmd=>{
		if(cmd.name == command)
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

	createDrawer:function(type,name,style,handler) {
	    let layer = this.features;
	    let Drawer = OpenLayers.Class(OpenLayers.Control.DrawFeature, {
		initialize: function(layer, options) {
		    var defaultStyle = OpenLayers.Util.extend({}, OpenLayers.Feature.Vector.style["default"]);
		    if(style)
			$.extend(defaultStyle, style);
		    let styleMap = new OpenLayers.StyleMap({"default":defaultStyle});
		    options = {handlerOptions:{
			layerOptions:{
			    styleMap:styleMap
			}
		    }};
		    OpenLayers.Control.DrawFeature.prototype.initialize.apply(
			this, [layer, handler||OpenLayers.Handler.Point, options]
		    );
		},
		drawFeature: function(geometry) {
		    OpenLayers.Control.DrawFeature.prototype.drawFeature.apply(this, arguments);
		    let feature =this.layer.features[this.layer.features.length-1];
		    feature.type = type;
		    feature.foo="BAR";
		    feature.style=style;
		    console.log("TYPE:" + feature.type);
		    this.layer.redraw();

		}
	    });
	    let drawer = new Drawer(layer);
	    this.addControl(name,drawer);
	    return drawer;
	},
	    
	addControl:function(name,control) {
	    control.name = name;
	    this.map.getMap().addControl(control);
	    this.commands.push(control);
	},

	doPaste: function() {
	    if(!this.clipboard) return;
	    let newOnes = this.clipboard.map(feature=>{return feature.clone();});
	    console.log(newOnes);
	    this.features.addFeatures(newOnes);
	},
	doEdit: function() {
	    this.clearCommands();
	    if(!this.features.selectedFeatures) return;
	    if(this.features.selectedFeatures.length==0) return;
	    let feature = this.features.selectedFeatures[0];
	    let html =HU.div([STYLE,HU.css("margin","8px")], "Feature: " + feature.type); 
	    if(feature.style.externalGraphic) {
//		html += HU.image(feature.style.externalGraphic);
	    }
//	    feature.style.strokeColor="green";
	    this.features.redraw(feature);
            let popup = this.map.makePopup(feature.geometry.getBounds().getCenterLonLat(),html);
	    this.map.currentPopup = popup;
            this.map.getMap().addPopup(popup);
	},

	doCut: function() {
	    this.clearCommands();
	    if(!this.features.selectedFeatures) return;
	    this.clipboard = this.features.selectedFeatures.map(feature=>{return feature;});
	    this.features.removeFeatures(this.features.selectedFeatures);
	},
	doCopy: function() {
	    this.clearCommands();
	    if(!this.features.selectedFeatures) return;
	    this.clipboard = this.features.selectedFeatures.map(feature=>{return feature;});
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

	    this.jq(ID_LEFT).html(HU.div([ID,this.domId(ID_COMMANDS),CLASS,"ramadda-display-editablemap-commands"]));
	    let _this = this;
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
										     pointRadius:10,
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
		    console.log("start");
		    this.theFeature = feature;
		    return OpenLayers.Control.ModifyFeature.prototype.dragStart.apply(this,arguments);
		},
		dragVertex:function(vertex,pixel) {
		    var pos = this.map.getLonLatFromViewPortPx(pixel);
		    console.log("dragVertex:" + pos);
		    OpenLayers.Control.ModifyFeature.prototype.dragVertex.apply(this,arguments);
		}
	    });
	this.addControl(ID_MODIFIER,new Modder(this.features,{}));


	    let buttons = [];
	    buttons.push(ID_SELECTOR,ID_MOVER,ID_MODIFIER);
	    let cmds = 
		HU.div([ID,this.domId(ID_SELECTOR),CLASS,"ramadda-clickable ramadda-display-editablemap-command"],"Select") +
		HU.div([ID,this.domId(ID_MOVER),CLASS,"ramadda-clickable ramadda-display-editablemap-command"],"Move") +
		HU.div([ID,this.domId(ID_MODIFIER),CLASS,"ramadda-clickable ramadda-display-editablemap-command"],"Modify");

	    let glyphs = [
		{label:"Circle",
		 type:"circle",
		 style:{strokeWidth:2, 
			fillColor:"transparent",
			strokeColor:"blue",
			pointRadius:10},
		 handler:OpenLayers.Handler.Point},
		{label:"Marker",
		 type:"marker",
		 style:{strokeWidth:0, 
			fillColor:"transparent",
			externalGraphic: "/repository/icons/map/marker-blue.png",
			pointRadius:10},
		 handler:OpenLayers.Handler.Point},
		{label:"Line",
		 type:"line",
		 style: {strokeColor:"red",strokeWidth:4},
		 handler:OpenLayers.Handler.Path}
	    ];
	    this.jq(ID_COMMANDS).html(cmds);
	    glyphs.forEach(g=>{
		let command = "new_" + g.type;
		buttons.push(command);
		this.createDrawer(g.type,command,g.style, g.handler);
		this.jq(ID_COMMANDS).append(HU.div([ID,this.domId(command),CLASS,"ramadda-clickable ramadda-display-editablemap-command"],"New " + g.label));
	    });
	    buttons.forEach(command=>{
		this.jq(command).button().click(function(){
		    if(ID_SELECTOR==command)
			_this.features.selectedFeatures  = [];
		    if($(this).hasClass("ramadda-display-editablemap-command-active")) {
			_this.setCommand(null);
		    } else {
			_this.setCommand(command);
		    }
		});
	    });
        },
    });
}


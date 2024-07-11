/**
   Copyright (c) 2008-2023 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/


function RepositoryMap(mapId, params) {
    ramaddaMapAdd(this);

    let _this = this;
    if (!params) params = {};
    this.params = params;
    this.mapId = mapId || "map";

    [ARG_MAPCENTER,ARG_ZOOMLEVEL].forEach(prop=>{
	if(Utils.stringDefined(HU.getUrlArgument(prop))) {
	    params[prop]  = HU.getUrlArgument(prop);
	    if(debugBounds)console.log("url arg:"+ prop+"=" + params[prop]);
	}
    });
    if(params.mapCenter) {
	[lat,lon] =  	params.mapCenter.replace("%2C",",").split(",")
	params.initialLocation = {lon:lon,lat:lat};
    }



    let showDflt = true;
    if(params.simple)
	showDflt = false;

    let dflt = {
        pointRadius: 3,
        strokeColor: "blue",
        strokeWidth: 1,
        fillOpacity: 0.8,
        fillColor: "#e6e6e6",

	layerStrokeColor:"blue",
	layerStrokeWith:1,
	layerFillColor:"#ccc",
	layerFillOpacity:0.3,	

	highlightStrokeColor:"blue",
	highlightFillColor:"match",	
	highlightStrokeWidth:2,
	highlightFillOpacity:0.5,

	selectStrokeColor:null,
	selectStrokeOpacity:null,
	selectFillColor:null,
	selectStrokeWidth:null,
	selectFillOpacity:0.4,	

	maxZoom:18,
	singlePointZoom:7,
        scrollToZoom: true,
        selectOnHover: false,
        highlightOnHover: true,
        showLocationSearch: false,
	imageOpacity:1.0,
	iconSize:null,
	iconWidth:null,
	iconHeight:null,
	changeSizeOnSelect:true,
        tickSelectColor: "red",
        tickHoverColor: "blue",
        tickColor: "#888",
	showDates:true,

        defaultMapLayer: map_default_layer,

	displayDivSticky:true,
	showLayerToggle:false,
	showLatLonLines:false,
        showScaleLine: showDflt,
        showLayerSwitcher: showDflt,
        showLatLonPosition: false,
        showZoomPanControl: false,
        showZoomOnlyControl: true,
        enableDragPan: true,
        showSearch: false,
	showBookmarks:false,
        showBounds: true,
	showOpacitySlider:false,

	doPopup:true,
	doPopupSlider:false,
	doFeatureSelect:true,
	popupWidth:400, 
	popupHeight:250,	
	popupSliderRight:false,
	doSelect:true,
	addMarkerOnClick:false,
	linked:false,
	linkGroup:null,
	linkMouse:false,
	addToUrl:true
    };


    if(!Utils.isDefined(params.initialZoom)) {
	this.hadInitialZoom = Utils.isDefined(params[ARG_ZOOMLEVEL]);
	params.initialZoom= Utils.isDefined(params[ARG_ZOOMLEVEL])?params[ARG_ZOOMLEVEL]:MapUtils.defaults.defaultZoomLevel;
	if(debugBounds) console.log("setting initial zoom:",params.initialZoom);
    } else {
	if(debugBounds) console.log("initial zoom already set:",params.initialZoom);
    }


    $.extend(dflt, params);
    params = this.params = dflt;

    this.getProperty = (key,dflt)=>{
	if(Utils.isDefined(this.params[key])) return this.params[key];
	return dflt;
    };

    $.extend(this, {
        name: "map",
        map: null,

        sourceProjection: MapUtils.defaults.sourceProjection,
        displayProjection: MapUtils.defaults.displayProjection,
        projectionUnits: MapUtils.defaults.units,
        mapDivId: this.mapId,
//        defaultLocation: MapUtils.defaults.location,
        defaultLocation: null,
        latlonReadout: null,

	haveAddedDefaultLayer: false,
        defaultCanSelect: true,
        layer: null,
        markers: null,
        vectors: null,
	allLayers: [],
	externalLayers:[],
        loadedLayers: [],
	nonSelectLayers: [],
	shareSelected:false,
        boxes: null,
        kmlLayer: null,
        kmlLayerName: null,
        geojsonlLayer: null,
        geojsonLayerName: null,
        vectorLayers: [],
        features: {},
        lines: null,
        selectorBox: null,
        selectorMarker: null,
        listeners: [],
        fixedText: false,
        initialLayers: [],
        imageLayers: {},
        imageLayersList: [],	
    });

    this.seenMarkers = {};    
    this.startDate = null;
    this.endDate = null;
    this.startFeature = null;
    this.endFeature = null;
    this.basePointStyle = null;
    this.defaultStyle = {
        pointRadius: this.params.pointRadius,
        fillOpacity: this.params.fillOpacity,
        fillColor: this.params.fillColor,
        fill: this.params.fill,
        strokeColor: this.params.strokeColor,
        strokeWidth: this.params.strokeWidth,
    };

    this.highlightStyle = {
	strokeColor:this.params.highlightStrokeColor,
	strokeWidth:this.params.highlightStrokeWidth,
	fillColor:this.params.highlightFillColor,
	fillOpacity:this.params.highlightFillOpacity,
    }



    if (Utils.isDefined(params.onSelect)) {
        this.onSelect = params.onSelect;
    } else {
        this.onSelect = null;
    }

    if (params.initialLocation) {
	if(!Array.isArray(params.initialLocation)) {
            this.defaultLocation = MapUtils.createLonLat(params.initialLocation.lon, params.initialLocation.lat);
	} else {
            this.defaultLocation = MapUtils.createLonLat(params.initialLocation[1], params.initialLocation[0]);
	}
	
	if(debugBounds)
	    console.log("setting default location:" + this.defaultLocation);
    } else if (Utils.isDefined(params.initialBounds)) {
        if ((typeof params.initialBounds) == "string") {
            params.initialBounds = params.initialBounds.split(",");
        }
        this.defaultBounds = MapUtils.createBounds(params.initialBounds[1], params.initialBounds[2], params.initialBounds[3], params.initialBounds[0]);
	this.defaultLocation = null;
	if(debugBounds)
	    console.log("setting default bounds-1:" + this.defaultBounds);
    }
    

    if(this.params.initialZoom)
	this.params.initialZoom=+this.params.initialZoom;
    var options = {
        projection: this.sourceProjection,
        displayProjection: this.displayProjection,
        units: this.projectionUnits,
        controls: [],
        maxResolution: 156543.0339,
        maxExtent: MapUtils.defaults.maxExtent,
        div: this.mapDivId,
	zoom:this.params.initialZoom,
        eventListeners: {
            featureover: function(e) {
		if(_this.featureOverHandler) {
		    _this.featureOverHandler(e);
		    return;
		}
                _this.handleFeatureover(e.feature);
            },
            featureout: function(e) {
		if(_this.featureOutHandler) {
		    _this.featureOutHandler(e);
		    return;
		}
                _this.handleFeatureout(e.feature);
            },
            nofeatureclick: function(e) {
		if(debugSelect)    console.log('nofeatureclick');
                _this.handleNofeatureclick(e,e.layer);
            },
	    
            featureclick: function(e) {

		if(_this.featureClickHandler && !_this.featureClickHandler(e))  {
		    if(debugSelect)    console.log('featureclick-1');
		    return;
		}
		if(!_this.params.doFeatureSelect) return;
                if(e.feature && e.feature.noSelect) {
		    if(debugSelect)    console.log('featureclick-2');
                    return;
                }
		if(debugSelect)    console.log('featureclick');
		let time =  new Date().getTime();
		//We get multiple click events if we have multiple features on the same point
		if(Utils.isDefined(_this.lastClickTime)) {
		    if(time-_this.lastClickTime <500) {
			return;
		    }
		}
		_this.lastClickTime  = time;
		_this.handleFeatureclick(e.layer, e.feature,false,e);
            }
        }
    };


//    this.addLayer(wmts);


    this.mapOptions = options;
    this.finishMapInit();
    jQuery(document).ready(function($) {
        if (_this.getMap()) {
            _this.getMap().updateSize();
        }
    });
}



OpenLayers.Control.Click = OpenLayers.Class(OpenLayers.Control, {
    listeners: null,
    addListener: function(listener) {
        if (this.listeners == null) {
            this.listeners = [];
        }
        this.listeners.push(listener);
    },
    defaultHandlerOptions: {
        'single': true,
        'double': false,
        'pixelTolerance': 0,
        'stopSingle': false,
        'stopDouble': false
    },

    initialize: function(options) {
        this.handlerOptions = $.extend({}, this.defaultHandlerOptions);
        OpenLayers.Control.prototype.initialize.apply(this, arguments);
        this.handler = new OpenLayers.Handler.Click(this, {
            'click': this.handleMapClick
        }, this.handlerOptions);
    },
    setLatLonZoomFld: function(lonFld, latFld, zoomFld, listener,onAlt) {
        this.lonFldId = lonFld;
        this.latFldId = latFld;
        this.zoomFldId = zoomFld;
        this.clickListener = listener;
	this.onAlt = onAlt;
    },

    setTheMap: function(map) {
        this.theMap = map;
    },

    handleMapClick: function(e) {
	if(this.onAlt) {
	    if(!e.altKey) return;
	    let map = this.theMap;
	    if(!map) return;
	    if(map.fldSouth?.id)
		jqid(map.fldSouth.id).val('');
	    if(map.fldEast?.id)
		jqid(map.fldEast.id).val('');	    
            if (this.theMap.selectorBox && this.theMap.boxes) {
		this.theMap.boxes.removeMarker(this.theMap.selectorBox);
		this.theMap.selectorBox = null;
	    }
	}
        let xy = this.theMap.getMap().getLonLatFromViewPortPx(e.xy);
        let lonlat = this.theMap.transformProjPoint(xy)
        if (this.listeners != null) {
            for (var i = 0; i < this.listeners.length; i++) {
                this.listeners[i](lonlat);
            }
        }
        if (!this.lonFldId) {
            this.lonFldId = "lonfld";
            this.latFldId = "latfld";
            this.zoomFldId = "zoomfld";
        }
	let map  =this.theMap;
	if(map.polygonSelectorId) {
	    map.addPolygonSelectionPoint(lonlat);
	    return
	}
	

	let lonFld = GuiUtils.getDomObject(this.lonFldId);
        let latFld = GuiUtils.getDomObject(this.latFldId);
        let zoomFld = GuiUtils.getDomObject(this.zoomFldId);
        if (latFld && lonFld) {
            latFld.obj.value = MapUtils.formatLocationValue(lonlat.lat);
            lonFld.obj.value = MapUtils.formatLocationValue(lonlat.lon);
        }
        if (zoomFld) {
            zoomFld.obj.value = this.theMap.getMap().getZoom();
        }


	if(this.theMap.getProperty('addMarkerOnClick') || this.theMap.selectorMarker) {
	    this.theMap.addSelectionMarker(lonlat);
	}


	if(Utils.isDefined(this.clickListener)) {
            this.clickListener.handleClick(this, e, lonlat.lon, lonlat.lat);
        }


    }

});







var markerMap = {};



function ramaddaFindFeature(layer, point) {
    for (var j = 0; j < layer.features.length; j++) {
        var feature = layer.features[j];
        var geometry = feature.geometry;
        if (!geometry) {
            continue;
        }
        bounds = geometry.getBounds();
        if (!bounds.contains(point.x, point.y)) {
            continue;
        }
        if (geometry.components) {
            for (var sub = 0; sub < geometry.components.length; sub++) {
                comp = geometry.components[sub];
                bounds = comp.getBounds();
                if (!bounds.contains(point.x, point.y)) {
                    continue;
                }
                if (comp.containsPoint && comp.containsPoint(point)) {
                    return {
                        feature: feature,
                        index: j
                    };
                }
            }
        } else {
            if (!geometry.containsPoint) {
                console.log("unknown geometry:" + geometry.CLASS_NAME);
                continue;
            }
            if (geometry.containsPoint(point)) {
                return {
                    feature: feature,
                    index: j
                };
            }
        }
    }
    return null;
}


RepositoryMap.prototype = {
    firstCustomRegion: true,
    CUSTOM_MAP : "CUSTOM",

    drawFeature:function(layer,feature,style) {
//	if(style=='default' && feature.originalStyle) style=feature.originalStyle;
//	console.log('draw',style);
	layer.drawFeature(feature,style);
    },
	
    setFeatureStyle:function(feature,style) { 
	if(style && style.display=='none') {
	} else {
	}
	if(feature) feature.style = style;
    },

    validBounds: function(b) {
	if(!b) return false
	return !isNaN(b.bottom) && !isNaN(b.left) && !isNaN(b.right) && !isNaN(b.top);
    },
    centerOnMarkers: function(dfltBounds, force, justMarkerLayer) {
	if(debugBounds) {
	    console.log("centerOnMarkers: force=" + force +" dflt:" + dfltBounds +" justMarkers:" + justMarkerLayer);
	}
        this.centerOnMarkersCalled = true;
        this.centerOnMarkersForce = force;
        let now = Date.now();
        let bounds = null;
        if (dfltBounds) {
            if (dfltBounds.left < -180 || dfltBounds.left > 180 ||
                dfltBounds.right < -180 || dfltBounds.right > 180 ||
                dfltBounds.bottom < -90 || dfltBounds.bottom > 90 ||
                dfltBounds.top < -90 || dfltBounds.top > 90) {
                dfltBounds = MapUtils.createBounds(-180, -90, 180, 90);
            }
        }
        this.dfltBounds = dfltBounds;

        if (!force) {
	    let didMarkers = false;

	    if(!justMarkerLayer) {
		this.externalLayers.forEach(layer=>{
                    let dataBounds = layer.getDataExtent();
		    if(debugBounds)
			console.log("centerOnMarkers using external layer");
		    bounds = MapUtils.extendBounds(bounds,this.transformProjBounds(dataBounds));
		    didMarkers = true;
		});
	    }		

            if (this.markers) {
                // markers are in projection coordinates
                let dataBounds = this.markers.getDataExtent();
		if(debugBounds)
		    console.log("centerOnMarkers using markers.getDataExtent");
		bounds = MapUtils.extendBounds(bounds,this.transformProjBounds(dataBounds));
		didMarkers = true;
            }
	    if(bounds && isNaN(bounds.left)) bounds = null;
            if (this.circles) {
                let dataBounds = this.circles.getDataExtent();
                let b = this.transformProjBounds(dataBounds);
		bounds = MapUtils.extendBounds(bounds,b);
		didMarkers = true;
		if(debugBounds)
		    console.log("centerOnMarkers using circles.getDataExtent:" + b);
	    }

	    if(bounds && isNaN(bounds.left)) bounds = null;

            if ( !bounds || !didMarkers || !justMarkerLayer) {
                if (this.lines) {
                    let dataBounds = this.lines.getDataExtent();
		    bounds = MapUtils.extendBounds(bounds,this.transformProjBounds(dataBounds));
		    if(debugBounds)
			console.log("centerOnMarkers using lines.getDataExtent");
                }
                for (let layer in this.getMap().layers) {
                    layer = this.getMap().layers[layer];
                    if (!layer.getDataExtent) {
			continue;
		    }
                    if (layer.isBaseLayer || !layer.getVisibility()) {
			continue;
		    }
                    let dataBounds = layer.getDataExtent();
                    if (dataBounds) {
                        let latLonBounds = this.transformProjBounds(dataBounds);
			if(!this.validBounds(latLonBounds)) continue;
			bounds = MapUtils.extendBounds(bounds,latLonBounds);
			if(debugBounds)
			    console.log("centerOnMarkers using layer.getDataExtent: " + latLonBounds +" layer=" + layer.name +" " + layer.ramaddaId);
                    }
                }
            }
	}


        if (!bounds) {
	    if(debugBounds)
		console.log("centerOnMarkers using dfltBounds: " + dfltBounds);
            bounds = dfltBounds;
        }
        if (!bounds) {
	    if(debugBounds)
		console.log("centerOnMarkers: no bounds");
            return;
        }

        if (!this.getMap()) {
            this.defaultBounds = bounds;
	    if(debugBounds)
		console.log("centerOnMarkers: no map");
            return;
        }
        if (bounds.getHeight() > 160) {
            bounds.top = 80;
            bounds.bottom = -80;
	    if(debugBounds)
		console.log("centerOnMarkers resetting height");
        }
	if(debugBounds)
	    console.log("calling setViewToBounds: ",bounds);
        this.setViewToBounds(bounds);
    },
    initRegionSelector:function(selectId,div,forSelection) {
	this.regionSelector = $('#' + selectId);
	this.mapWidgetDiv = $('#' + div);
	//Hide it to start
	this.mapWidgetDiv.hide();

	//When page is loaded then initialize
	$( document ).ready(()=>{
	    if(this.regionSelector.val() != this.CUSTOM_MAP) {
		this.mapWidgetDiv.hide();
	    } else {
		this.initMap(forSelection);
	    }
	    this.regionSelector.change(()=>{
		this.mapRegionSelected();
	    });
	    this.mapRegionSelected();
	});
    },
    mapRegionSelected: function() {
        let value = this.regionSelector.val();
        if (value === null) {
            console.log("Error: No map region value");
            return;
        }
        if (value == "") {
            this.toggleMapWidget(false);
            return;
        }
        let toks = value.split(",");

        if (toks.length == 1) {
            if (toks[0] != this.CUSTOM_MAP) {
                return;
            } else {
                if (!this.firstCustomRegionm) {
                    this.setMapRegion( "", "", "", "", "");
                }
                this.firstCustomRegion = false;
                this.toggleMapWidget(true);
                return;
            }
        }
        if (toks.length != 5) {
            return;
        }
        this.toggleMapWidget(false);
        this.setMapRegion(toks[0], toks[1], toks[2], toks[3], toks[4]);
    },


    setMapRegion: function(regionid, north, west, south, east) {
	let baseId = this.mapId;
        $("#" + baseId + "_regionid").val(regionid);
        $("#" + baseId + "_north").val(north);
        $("#" + baseId + "_west").val(west);
        $("#" + baseId + "_south").val(south);
        $("#" + baseId + "_east").val(east);

    },

    toggleMapWidget: function(onOrOff) {
        if (onOrOff) {
            // check if the map has been initialized
            if (!this.inited) {
                this.initMap(true);
            }
            this.mapWidgetDiv.show();
        } else {
            this.mapWidgetDiv.hide();
        }
    },


    setViewToBounds: function(bounds) {
	let singlePoint = bounds.left==bounds.right;
        let projBounds = this.transformLLBounds(bounds);
        if (projBounds.getWidth() == 0) {
	    if(debugBounds) console.log("setViewToBounds single point max zoom:" + this.params.singlePointZoom);
	    //Set the center then zoom then set the center again
	    this.getMap().setCenter(projBounds.getCenterLonLat());
            this.zoomTo(this.params.singlePointZoom);
	    this.getMap().setCenter(projBounds.getCenterLonLat());
        } else {
//	    if(debugBounds)console.log(bounds.getCenterLonLat());
	    this.getMap().setCenter(projBounds.getCenterLonLat());
            this.zoomToExtent(projBounds);
        }
	if(this.getMap().getZoom()>this.params.maxZoom) {
	    if(debugBounds)  console.log("setViewToBounds- setting to max zoom",this.params.maxZoom);
            this.zoomTo(this.params.maxZoom);
	}
	//xxxxx
//	this.defaultBounds=null;
//	this.defaultLocation=null;

    },
    setCenter:function(to) {
	if(debugBounds)
	    console.log("setCenter");
//	this.getMap().panTo(this.transformLLPoint(to));
        this.getMap().setCenter(this.transformLLPoint(to));
    },
    getZoom: function() {
	return this.getMap().getZoom();
    },
    setZoom: function(zoom) {
	this.zoomTo(zoom);
    },
    zoomTo:function(zoom,onlyIfWeNeedToZoomIn) {
	zoom=+zoom;
	if(zoom<0) {
	    zoom = this.params.singlePointZoom;
	}
	if(onlyIfWeNeedToZoomIn) {
	    if(zoom<this.getZoom()) return;
	}
	if(debugBounds) {
	    console.log("zoomTo:",zoom);
	}
	this.getMap().zoomTo(parseInt(zoom));

    },
    zoomToLayer:  function(layer,scale)  {
        let dataBounds = this.getLayerVisibleExtent(layer);
	if(!dataBounds)
	    dataBounds = layer.extent;
	if(debugBounds)  console.log("zoomToLayer:",dataBounds);
        if (dataBounds) {
	    if(scale)
		dataBounds = dataBounds.scale(parseFloat(scale),dataBounds.getCenterPixel());
            this.zoomToExtent(dataBounds, false);
	}
    },
    zoomToBounds:function(bounds) {
        this.getMap().setCenter(bounds.getCenterLonLat());
        this.zoomToExtent(bounds);
    },
    zoomToMarkers: function() {
	this.zoomToLayer(this.markers);
    },
    zoomToExtent: function(bounds,flag) {
	if(debugBounds) {
	    console.log("zoomToExtent:", bounds);
	}
	let ok = num=>{
	    return !isNaN(num) && Utils.isDefined(num);
	}
	if(!ok(bounds.left) ||
	   !ok(bounds.right) ||
	   !ok(bounds.top) ||
	   !ok(bounds.bottom)) {
	    if(debugBounds)
		console.error("zoomToExtent: bounds are bad:", bounds);
	    return
	}
	if(bounds.left == bounds.right || bounds.top == bounds.bottom) {
	    bounds = this.transformProjBounds(bounds);
	    var center = bounds.getCenterLonLat();
	    this.setCenter(center);
	    return;
	}
	this.getMap().zoomToExtent(bounds,flag);
    },
    centerToMarkers: function() {
        if (!this.markers)
            return;
        bounds = this.markers.getDataExtent();
	if(debugBounds)
	    console.log("centerToMarkers");
        this.getMap().setCenter(bounds.getCenterLonLat());
    },
    setInitialCenterAndZoom: function(lon, lat, zoomLevel) {
        this.defaultLocation = MapUtils.createLonLat(lon, lat);
	if(debugBounds)
	    console.log("setInitialCenterAndZoom:" + this.defaultBounds);

	this.setInitialZoom(zoomLevel);
        this.params.initialZoom = zoomLevel;
    },
    setInitialZoom: function(zoomLevel) {
        this.params.initialZoom = zoomLevel;
        if(this.params.initialZoom) this.params.initialZoom=+this.params.initialZoom;
	if(debugBounds) console.log("setInitialZoom:",this.params.initialZoom);
    },
    setInitialCenter: function(lon,lat) {
        this.defaultLocation = MapUtils.createLonLat(lon, lat);
    },
    finishMapInit: function() {
        let _this = this;
        if (this.params.showSearch) {
            this.searchDiv = this.mapDivId + "_search";
            let cbx = HtmlUtils.checkbox(this.searchDiv + "_download", [], false);
            let input = "<input placeholder=\"Search - ? for help\" id=\"" + this.searchDiv + "_input" + "\" size=40>";
            let search = "<table width=100%><tr><td>" + input + " <span  id=\"" + this.searchDiv + "_message\"></span></td><td align=right>"+/* + cbx + " Download*/"</td></tr></table>"
            $("#" + this.searchDiv).html(search);
            this.searchMsg = $("#" + this.searchDiv + "_message");
            let searchInput = $("#" + this.searchDiv + "_input");
            searchInput.keypress(function(event) {
                if (event.which == 13) {
                    _this.searchFor(searchInput.val());
                }
            });
        }

	let getId = id=>{
	    return this.mapDivId+"_" + id;
	};
	let html = HU.open("table",[STYLE,HU.css("height","100%"), WIDTH,"100%","border","0"  ]);
	let theMap = HtmlUtils.div([CLASS, "ramadda-map-inner", "style","width:100%;height:100%;position:relative;","id",getId("themap")]);
	html+=HU.tr([STYLE,HU.css("height","100%")],HU.td([WIDTH,"100%"],theMap));
	html+="</table>";
	//	$("#" + this.mapDivId).html(html);
	$("#" + this.mapDivId).html(theMap);

	$("#" + getId("themap")).append(HtmlUtils.div(["id",getId("progress"), CLASS,"ramadda-map-progess", "style","z-index:3000;position:absolute;top:10px;left:40%;"],""));
	$("#" + getId("themap")).append(HtmlUtils.div(["id",getId("label"), "style","z-index:1000;position:absolute;bottom:10px;left:10px;"],""));
	$("#" + getId("themap")).append(HtmlUtils.div(["id",getId("toolbar"), "style","z-index:1000;position:absolute;top:10px;left:50%;    transform: translateX(-50%);"],""));

        this.map = new OpenLayers.Map(this.mapDivId+"_themap", this.mapOptions);

        //register the location listeners later since the map triggers a number of
        //events at the start
        let callback = function() {
            _this.getMap().events.register("changebaselayer", "", function() {
                _this.baseLayerChanged();
            });
            _this.getMap().events.register("zoomend", "ramaddamap", function() {
		_this.zoomChanged();
                _this.locationChanged();
		_this.setNoPointerEvents();
		if(debugBounds)  console.log("zoomend",_this.getMap().getZoom());
            });
            _this.getMap().events.register("moveend", "ramaddamap", function() {
                _this.locationChanged();
            });
	    _this.getMap().events.register("move", "ramaddamap", function() {
                _this.locationChanged();
            });
        };
	//Do this later
        setTimeout(callback, 2000);

        if (this.mapHidden) {
            //A hack when we are hidden
            this.getMap().size = MapUtils.createSize(1, 1);
        }

	try {
	    if(window["initExtraMap"]) {
		initExtraMap(this);
	    } else {
		//console.log("No initExtraMap");
	    }
	} catch(err) {
	    console.log("Error calling initExtraMap:" + err);
	}

        this.addBaseLayers();

        if (this.kmlLayer) {
            var url = ramaddaBaseUrl + "/entry/show?output=shapefile.kml&entryid=" + this.kmlLayer;
            this.addKMLLayer(this.kmlLayerName, url, false, null, null, null, null);
        }
        if (this.geojsonLayer) {
            var url = getRamadda().getEntryDownloadUrl(this.geojsonLayer);
            this.addGeoJsonLayer(this.geojsonLayerName, url, false, null, null, null, null);
        }
	//Not sure why we do this as addDisplay should only be for the displays under display/
	//	Utils.addDisplay(this);
	//Do this later for when this map is being shown for a display_map
	let makeSlider = () =>{
	    let slider = "Image Opacity:&nbsp;" + 
		HU.div([ID,this.mapDivId +"_opacity_slider_div",STYLE,HU.css("display",
									     "inline-block","width","150px")],"");

	    slider = HU.span(['id',this.mapDivId+'_opacity_slider','style',
			      (this.params.showOpacitySlider)?'display:inline':'display:none'], slider);	    

	    $("#" + this.mapDivId+"_header").append(slider);
	    $("#"+ this.mapDivId +"_opacity_slider_div").slider({
		min: 0,
		max: 1,
		step:0.05,
		value:_this.params.imageOpacity,
		slide: function( event, ui ) {
		    _this.params.imageOpacity = ui.value;
		    if(!_this.imageLayersList) return;
		    _this.allLayers.forEach(layer=>{
			if(Utils.isDefined(layer.canTakeOpacity))
			    layer.setOpacity(_this.params.imageOpacity);
		    });
		    _this.imageLayersList.forEach(image=>{
			image.setOpacity(_this.params.imageOpacity);
		    });
		},
	    });
	};
	setTimeout(makeSlider,200);
	
	
	if(this.getProperty('addMarkerOnClick')) {
	    this.setSelection();
	}



	setTimeout(()=>{
	    let mapDiv = document.querySelector("#"+this.mapDivId+"_themap");
	    if(!mapDiv) return;
	    let observer = new ResizeObserver(Utils.throttle(()=>{
		if(this.getMap()) {
		    this.getMap().updateSize();
		    let baseLayers = this.getMap().layers.filter(layer=>{
			return layer.isBaseLayer;
		    });	    
		    //Sometimes the base layer doesn't redraw very well so force a redraw
		    baseLayers.forEach(layer=>{
			if(layer.visibility) {
			    layer.redraw();
			}
		    });	    
		}
	    },1000));
	    observer.observe(mapDiv);
	},1000);
    },
    showOpacitySlider:function(visible) {
	this.params.showOpacitySlider = visible;
	if(visible)
	    $('#'+this.mapDivId+'_opacity_slider').css('display','inline');
	else
	    $('#'+this.mapDivId+'_opacity_slider').css('display','none');	
    },
    getBounds: function() {
	return  this.transformProjBounds(this.getMap().getExtent());
    },
    zoomChanged: function() {
    },
    receiveShareState:function(source, state) {
	if(state.center) {
	    if(!this.params.linked) return;
	    this.getMap().setCenter(state.center, state.zoom);
	} else if(state.marker) {
	    if(this.params.addMarkerOnClick) {
		this.addSelectionMarker(state.marker,true);
	    }
	} else if(state.mouse) {
	    if(!this.params.linkMouse) return;
	    if(this.sharedMouse) {
		this.getMarkersLayer().removeFeatures([this.sharedMouse],{silent:true});
	    }
	    this.sharedMouse = this.addPoint("mouse", state.mouse, {},"");
	}
    },
    locationChanged: function() {
	ramaddaMapShareState(this,{center:this.map.getCenter(),
				   zoom: this.map.getZoom()});
	//buffer calls
	if(this.pendingLocationChangeTimeout) {
	    clearTimeout(this.pendingLocationChangeTimeout);
	}
	this.pendingLocationChangeTimeout = setTimeout(()=>{
	    this.locationChangedInner();
	    this.pendingResizeTimeout = null;
	},500);
	
    },
    locationChangedInner: function(toConsole) {
	let latlon = this.getBounds();
	let bits = 100000;
	let r = (v=>{
	    return Math.round(v*bits)/bits;
	});
	latlon.top = r(latlon.top);
	latlon.left = r(latlon.left);
	latlon.bottom = r(latlon.bottom);
	latlon.right = r(latlon.right);
	let center =   this.transformProjPoint(this.getMap().getCenter())

	if(toConsole) {
	    console.log(latlon.top, latlon.left,latlon.bottom,latlon.right);
	    console.log(this.getMap().getZoom());
	    console.log(r(center.lat),r(center.lon));
	    return
	} 
	if(this.params.addToUrl) {
	    HU.addToDocumentUrl("map_bounds",latlon.top + "," + latlon.left + "," + latlon.bottom + "," + latlon.right);
//	    if(debugBounds)console.log("locationChanged: setting url args:",this.getMap().getZoom());
	    HU.addToDocumentUrl(ARG_ZOOMLEVEL , this.getMap().getZoom());
            HU.addToDocumentUrl(ARG_MAPCENTER, r(center.lat)+","+ r(center.lon));
	}
    },
    baseLayerChanged: function() {
        let baseLayer = this.getMap().baseLayer;
        if (!baseLayer) return;
        baseLayer = baseLayer.ramaddaId;
	HU.addToDocumentUrl("defaultMapLayer",baseLayer);
	ramaddaMapShareState(this,"baseLayer");
    },
    appendToolbar: function(html) {
	$("#" + this.mapDivId+"_toolbar").append(html);
    },

    setMapDiv: function(divid) {
        this.mapHidden = false;
        this.mapDivId = divid;
        this.getMap().render(this.mapDivId);
        this.getMap().updateSize();
        this.centerOnMarkers(this.dfltBounds);
    },
    addPopup:function(popup) {
        this.currentPopup = popup;
	this.getMap().addPopup(popup);
    },
    makePopup: function(projPoint, text, props) {
	if(debugPopup)
	    console.log("makePopup:" + text);
	props = props||{};
	let size  =  MapUtils.createSize(props.width|| this.params.popupWidth||200, props.height || this.params.popupHeight||200);
	return  new OpenLayers.Popup("popup",
				     projPoint,
				     size,
				     text,
				     true,
				     props.callback?props.callback:()=>{this.onPopupClose()});
    },

    highlightMarkers:function(selector,  background1, background2, id) {
	let _this =  this;
	if(!background1) background1= '#ffffcc';
	let links = 	    $(selector).find('[data-mapid]');
	links.mouseenter(function(event) {
//	    _this.closePopup();  HtmlUtils.hidePopupObject();
	    if (background1)
                $(this).css('background', background1);
	    let id = $(this).data('mapid');
	    if (!id)  return;
            let box = _this.findBox(id);
	    if(box) {
		box.setBorder('red');
		_this.boxes.drawMarker(box);
	    }
	    let marker = _this.circleMarker(id, MapUtils.circleHiliteAttrs);
	    if(marker) {
		if(event.shiftKey)
		    _this.centerOnFeatures([marker]);
                return;
	    }
	    if (id == null)
                return;
	    if (!Utils.isDefined($(this).data('latitude'))) {
                console.log("no lat");
                return;
	    }
	    attrs = {
                pointRadius: 12,
                stroke: true,
                strokeColor: "blue",
                strokeWidth: 2,
                fill: false,
	    };
	    point = _this.addPoint(id, MapUtils.createLonLat($(this).data('longitude'), $(this).data('latitude')),
				   attrs);
	    markerMap[id] = point;
        });
	links.mouseleave(function() {
	    if (background2)
                $(this).css('background', background2);
	    else 
                $(this).css('background', 'transparent');
	    let id = $(this).data('mapid');
	    if (!id)  return;
	    if (markerMap[id]) {
                _this.removePoint(markerMap[id]);
                markerMap[id] = null;
	    }


            let box = _this.findBox(id);
	    if(box) {
		box.setBorder(box.defaultBorderColor);
		_this.boxes.drawMarker(box);
	    }

	    _this.uncircleMarker($(this).data('mapid'));
        });
    },



    applyHighlightStyle:function(opts) {
	['pointRadius','highlightStrokeColor','highlightStrokeWidth',
	 'highlightFillColor', 'highlightFillOpacity'].forEach(a=>{
	     if(Utils.isDefined(opts[a])) {
		 let  v= opts[a];
		 this.params[a] = v;
		 let _a = a.replace('highlight','');
		 _a = _a.substring(0,1).toLowerCase()+_a.substring(1);
		 this.highlightStyle[_a] = v;
	     }
	 });
    },

    getLayerHighlightStyle:function(layer) {
	let highlightStyle = $.extend({},this.highlightStyle);
	if(layer.highlightStyle) {
	    $.extend(highlightStyle, layer.highlightStyle);
	}
	return highlightStyle;
    },

    checkMatchStyle(fs,highlight) {
	['pointRadius','fillColor','fillOpacity','strokeColor','strokeOpacity','strokeWidth'].forEach(a=>{
	    if(highlight[a]=="match") {
		if(Utils.stringDefined(fs[a]))
		    highlight[a] = fs[a];
		else {
		    highlight[a]='transparent';
		}
	    }
	});
    },
    handleFeatureover: function(feature, skipText,extraStyle) {
        if (this.selectedFeature)  return;
	if(this.doMouseOver || feature.highlightText || feature.highlightTextGetter) {
	    let location = feature.location;
	    if (location) {
		if(this.highlightFeature != feature) {
		    this.closeHighlightPopup();
		    let text = feature.highlightTextGetter?feature.highlightTextGetter(feature):feature.highlightText;
		    if (!Utils.stringDefined(text))  {text = feature.text;}
		    if (Utils.stringDefined(text)) {
			let projPoint = this.transformLLPoint(location);
			text =HtmlUtils.div(["style","padding:2px;"],text);
			this.highlightPopup = new OpenLayers.Popup("popup",
								   projPoint,
								   feature.highlightSize,
								   text,
								   false);
			this.highlightPopup.backgroundColor=this.highlightBackgroundColor||"#ffffcc";
			this.highlightPopup.autoSize=true;
			this.highlightPopup.keepInMap=true;
			this.highlightPopup.padding=0;

			this.addPopup(this.highlightPopup);
		    }
		}
	    }
	}

        let layer = feature.layer;
        if (!(layer.isMapLayer === true)) {
            if (!skipText && feature.text) {
                this.showFeatureText(feature);
            }
            return;
        }
	if (layer.canSelect === false || layer.noHighlight) return;
        if (!feature.isSelected) {
	    this.highlightFeature(feature,extraStyle);
            if (this.params.displayDiv) {
                this.displayedFeature = feature;
                if (!skipText) {
		    if(this.pendingDisplayTextTimeout) {
			clearTimeout(this.pendingDisplayTextTimeout);
		    }
                    let callback = () =>{
			if (this.displayedFeature == feature) {
			    let text = this.getFeatureText(layer, feature);
                            this.showText(text);
                            this.dateFeatureOver(feature);
			}
                    }
                    this.pendingDisplayTextTimeout = setTimeout(callback, 500);
                }
            }
        }
    },


    unhighlightFeature:function(feature) {
	if(feature.originalStyle) {
	    this.setFeatureStyle(feature, feature.originalStyle);
	    this.drawFeature(feature.layer,feature);
	}
    },
    highlightFeature:function(feature,highlightStyle) {
	let fs = feature.style;
	if(!feature.originalStyle && feature.style) {
            feature.originalStyle = $.extend({},feature.style);
	}
	this.setFeatureStyle(feature,null);
	let layer = feature.layer;
	let highlight = $.extend({},highlightStyle??this.getLayerHighlightStyle(layer));


	if(highlight.fillColor!="transparent" && highlight.fillColor!="match" && feature.originalStyle) {
	    highlight.fillColor  = Utils.brighterColor(feature.originalStyle.fillColor||highlight.fillColor,0.4)??highlight.fillColor;
	}

	if(!Utils.isDefined(highlight.fillOpacity)) {
	    highlight.fillOpacity = 0.3;
	}
	fs = fs ??{};
	this.checkMatchStyle(fs,highlight);

	if(fs.strokeWidth) highlight.strokeWidth = (+fs.strokeWidth)+1;
	if(Utils.stringDefined(fs.externalGraphic) && !Utils.stringDefined(highlight.externalGraphic)) {
	    highlight.externalGraphic = fs.externalGraphic;
	    if(fs.graphicWidth) 
		highlight.graphicWidth = fs.graphicWidth*1.3;
	    if(fs.graphicHeight) 
		highlight.graphicHeight = fs.graphicHeight*1.3;	    
	}


	if(fs.pointRadius && !highlight.pointRadius) {
	    highlight.pointRadius = fs.pointRadius*1.2;
	}
	if(fs.externalGraphic && !highlight.externalGraphic) {
	    highlight.externalGraphic = fs.externalGraphic;
	    highlight.fillOpacity=1;
	}	    
	this.drawFeature(feature.layer,feature, highlight);
    },

    closeHighlightPopup: function() {
        if(this.highlightPopup) {
            this.getMap().removePopup(this.highlightPopup);
	    try {
		this.highlightPopup.destroy();
	    }catch(err) {}
	    this.highlightPopup  = null;
	    this.highlightFeature = null;
	}
    },
    handleFeatureout: function(feature, skipText) {
        if(this.displayedFeature==feature) {
	    this.displayedFeature=null;
	    if(this.pendingDisplayTextTimeout)
		clearTimeout(this.pendingDisplayTextTimeout);
	    this.pendingDisplayTextTimeout=null;
	}

	this.closeHighlightPopup();
        let layer = feature.layer;
        if (layer && !(layer.isMapLayer === true)) {
            if (!skipText) {
                if (feature.text && !this.fixedText) {
                    this.hideFeatureText(feature);
                }
            }
            return;
        }
        if (layer == null || layer.canSelect === false) {
	    return;
	}

	//Only reset to the original style if there is something there
	if(feature.originalStyle && feature.originalStyle.fillColor) {
	    this.setFeatureStyle(feature, feature.originalStyle);
	}


        if (!feature.isSelected) {
	    this.drawFeature(layer,feature, feature.style || "default");
        }
        this.dateFeatureOut(feature);
        if (!skipText && this.displayedFeature == feature && !this.fixedText) {
	    if(!this.params.displayDivSticky) 
                this.showText("");
        }
    },
    getLayerCanSelect: function(layer) {
        if (!layer) return false;
        return layer.canSelect;
    },
    handleNofeatureclick: function(event,layer) {
	//We get multiple click events if we have multiple features on the same point
	if(Utils.isDefined(this.lastClickTime)) {
	    let time =  new Date().getTime();
	    if(time-this.lastClickTime <2000) {
		return;
	    }
	}

	this.closePopup();
        HtmlUtils.hidePopupObject();

        if (layer.canSelect === false) return;
        if (layer && layer.selectedFeature) {
            this.unselectFeature(layer.selectedFeature);
            this.clearDateFeature();
        }
    },
    handleFeatureclick: function(layer, feature, center,event,extraStyle) {
        if (!layer)
            layer = feature.layer;

	if(layer && (layer.canSelect===false)) {
	    if(debugSelect)    console.log('handleFeatureclick- layer no select:' + layer.canSelect);
	    return
	}
	if(debugPopup) console.log("handleFeatureClick");
        this.dateFeatureSelect(feature);
        if (layer.canSelect === false) {
	    if(debugSelect || debugPopup) console.log("\tlayer no select");
	    return;
	}
        if (layer.selectedFeature) {
            this.unselectFeature(layer.selectedFeature);
        }


	if(!this.params.doSelect) {
	    if(debugSelect || debugPopup) console.log("\tparams no select");
	    return;
	}
        this.selectedFeature = feature;
        layer.selectedFeature = feature;
        layer.selectedFeature.isSelected = true;
	let fs = feature.style??{};
	let style = {};
	if(layer.style) $.extend(style, layer.style);
	$.extend({},feature.style);
	let fstyle = feature.originalStyle??feature.style??{};
	let highlightStyle = this.getLayerHighlightStyle(layer);
	$.extend(style, {
	    strokeColor:fstyle.highlightStrokeColor??this.params.selectStrokeColor ?? highlightStyle.strokeColor,
	    strokeWidth:fstyle.highlightStrokeWidth?? this.params.selectStrokeWidth ?? highlightStyle.strokeWidth,
	    strokeOpacity: this.params.selectStrokeOpacity ?? 0.75,
	    fillColor:fstyle.highlightFillColor??this.params.selectFillColor ??highlightStyle.fillColor,
	    fillOpacity: fstyle.highlightFillOpacity??this.params.selectFillOpacity ??highlightStyle.fillOpacity,
	    pointRadius: fstyle.highlightPointRadius??this.params.selectPointRadius ??highlightStyle.pointRadius??fs.pointRadius,	    
	    fill: true,
	});

	if(this.params.changeSizeOnSelect && Utils.isDefined(style.pointRadius)) {
	    style.pointRadius = Math.round(style.pointRadius*1.5);
	}

	if(extraStyle) {
	    $.extend(style,extraStyle);
	}

	if(style.fillColor!="transparent") {
	    style.fillColor  = this.params.selectFillColor || highlightStyle.fillColor;
	} 

	if(style.fillColor=="match") {
	    style.fillColor = style.strokeColor;
	}

	this.checkMatchStyle(fs,style);

	//If it is a graphic then just clone the style
	if(feature.style &&  feature.style.externalGraphic) {
	    style = $.extend({},feature.style);
	    //Dim it a bit
	    style.fillOpacity=0.6;
	    if(this.params.changeSizeOnSelect) {
		if(Utils.isDefined(style.graphicHeight))  {
		    style.graphicHeight*=1.5;
		    style.graphicYOffset = -style.graphicHeight/ 2;
		}
		if(Utils.isDefined(style.graphicWidth)) {
		    style.graphicWidth*=1.5;
		    style.graphicXOffset = -style.graphicWidth/ 2;
		}
	    }
	}


	this.drawFeature(layer,layer.selectedFeature, style);
        if (layer.selectCallback) {
            layer.feature = layer.selectedFeature;
            if (feature.originalStyle) {
		//Don't do this now as it mucks up the display
		//                feature.style = feature.originalStyle;
            }
	    if(debugPopup) console.log("\thave a selectCallback");
	    layer.selectCallback(layer.feature,layer,event);
        } else {
	    if(debugPopup) console.log("\tcalling showMarkerPopup");
            this.showMarkerPopup(feature, true);
        }
	

        if (center) {
            let geometry = feature.geometry;
            if (geometry) {
                let bounds = geometry.getBounds();
                if (bounds) {
                    this.zoomToExtent(bounds);
                    this.getMap().setCenter(bounds.getCenterLonLat());
                }
            }
        }
    },


    unselectFeature: function(feature) {
        if (!feature) return;
	feature.renderIntent = null;
        feature.isSelected = false;
        let layer = feature.layer;
        if (!layer) return;
	this.drawFeature(layer,layer.selectedFeature, layer.selectedFeature.style || "default");
        layer.selectedFeature.isSelected = false;
        layer.selectedFeature = null;
        this.selectedFeature = null;
        this.onPopupClose();
        if (true) return;

        if (!feature) return;
        this.clearDateFeature(feature);
        layer = feature.layer;
        layer.selectedFeature = null;
        this.onPopupClose();
        feature.isSelected = false;
        layer.selectedFeature = null;
        this.selectedFeature = null;
        this.checkFeatureVisible(feature, true);
    },
    handleEntrySelect: function(id, name, type) {
        if (type != "geo_kml" && type != "geo_json" && type != "geo_shapefile") return null;
        var layer;
        if (type == "geo_kml") {
            var url = ramaddaBaseUrl + "/entry/get?entryid=" + id;
            layer = this.addKMLLayer(name, url, true, null, null, null, null, true);
        } else if (type == "geo_json") {
            var url = ramaddaBaseUrl + "/entry/get?entryid=" + id;
            layer = this.addGeoJsonLayer(name, url, true, null, null, null, null, true);
        } else {
            var url = ramaddaBaseUrl + "/entry/show?output=shapefile.kml&entryid=" + id;
            layer = this.addKMLLayer(name, url, true, null, null, null, null, true);
        }
        return layer;
    },
    findLayer: function(id) {
        for(var i=0;i<this.getMap().layers.length;i++) {
	    if(this.getMap().layers[i].ramaddaId == id) return this.getMap().layers[i];
	}
	return null;
    },
    addLayer: function(layer,nonSelectable) {
	this.allLayers.push(layer);
	layer.initialVisibility  = layer.getVisibility();
	if (this.map != null) {
            this.getMap().addLayer(layer);
	    if(nonSelectable)
		this.nonSelectLayers.push(layer);
	    else
		this.loadedLayers.push(layer);
	    this.checkLayerOrder();
        } else {
            this.initialLayers.push(layer);
        }
    },
    toFrontLayer: function(layer) {
	Utils.toFront(this.nonSelectLayers, layer);
	this.checkLayerOrder();
    },
    toBackLayer: function(layer) {
	Utils.toBack(this.nonSelectLayers, layer);
	this.checkLayerOrder();
    },    
    redraw: function() {
	this.getMap().layers.forEach(layer=>{
	    layer.redraw();
	});
    },
    checkLayerOrder: function() {
	//Offset a bunch from the base
	let base = this.numberOfBaseLayers+100;
	let debug = false;
//	debug = true;
	let max = 0;
	if(debug)   console.log("***** layer order");
	let changed = false;
	let setIndex=layer=>{
	    let index;
	    if(layer.ramaddaLayerIndex) {
		index=layer.ramaddaLayerIndex;
 	    } else {
		index=base++;
	    }
	    if(!changed)
		changed = layer.theIndex!=index;
	    if(debug) console.log('\t' + layer.name +' ramadda:' + layer.ramaddaLayerIndex +' old:' + layer.theIndex +' ' + index+' ' +changed);
	    layer.theIndex = index;
	    //	    this.getMap().setLayerIndex(layer, index);		
	    base=Math.max(base,index);
	};
	this.nonSelectLayers.forEach(setIndex);
	this.loadedLayers.forEach(setIndex);
//	console.log("external")
//	this.externalLayers.forEach(setIndex);
	if (this.boxes) {
	    if(debug)console.log("\tboxes");
	    setIndex(this.boxes);
	}
	if (this.lines) {
	    if(debug)console.log("\tlines");
	    setIndex(this.lines);
	}
        if (this.circles) {
	    if(debug)console.log("\tcircles");
	    setIndex(this.circles);
	}
	if (this.markers) {
	    if(debug)console.log("\tmarkers");
	    setIndex(this.markers);
	}
	if (this.labelLayer) {
	    if(debug)console.log("\tlabel");
	    setIndex(this.labelLayer);
	}

	if(changed) {
	    let nonBaseLayers = this.getMap().layers.filter(layer=>{
		return !layer.isBaseLayer;
	    });
	    let baseLayers = this.getMap().layers.filter(layer=>{
		return layer.isBaseLayer;
	    });	    

	    nonBaseLayers.sort((l1,l2)=>{
		if(!Utils.isDefined(l1.theIndex)) {
		    if(Utils.isDefined(l2.theIndex))
			return -1;
		    else
			return 1;
		}
		if(!Utils.isDefined(l2.theIndex)) {
		    if(Utils.isDefined(l1.theIndex))
			return 1;
		    else
			return -1;
		}
		return l1.theIndex-l2.theIndex;
	    });
	    baseLayers.push(...nonBaseLayers);
	    this.getMap().layers = baseLayers;
	    if(debug)
		console.log("reset z");
	    this.getMap().resetLayersZIndex();
	}
    },

    addImageLayer: function(layerId, name, desc, url, visible, north, west, south, east, width, height, args,loadCallback) {
	let _this = this;
        let theArgs = {
            forSelect: false,
            addBox: true,
            isBaseLayer: false,
	    alwaysInRange:true
        };
        if (args)
            $.extend(theArgs, args);

        //Things go blooeey with lat up to 90
        if (north > 88) north = 88;
        if (south < -88) south = -88;
        let latLonBounds = MapUtils.createBounds(west, south, east, north);
        let imageBounds = this.transformLLBounds(latLonBounds);
        let image = MapUtils.createLayerImage(
            name, url,
            imageBounds,
            MapUtils.createSize(width, height), {
                numZoomLevels: 3,
                isBaseLayer: theArgs.isBaseLayer,
		alwaysInRange:theArgs.alwaysInRange,
		displayOutsideMaxExtent:true,
                resolutions: this.getMap().layers[0] ? this.getMap().layers[0].resolutions : null,
                maxResolution: this.getMap().layers[0] ? this.getMap().layers[0].resolutions[0] : null
            }
        );
        image.events.on({
            "loadend": function(e) {
		if(loadCallback) loadCallback(_this,image);
	    }});

	image.id = layerId;
        image.latLonBounds = latLonBounds;
        //        image.setOpacity(0.5);
        if (theArgs.forSelect) {
            this.selectImage = image;
        }
        let lonlat = MapUtils.createLonLat(west, north);
        image.lonlat = this.transformLLPoint(lonlat);
        image.setVisibility(visible);
        image.id = layerId;
        image.text = this.getPopupText(desc);
        image.ramaddaId = layerId;
        image.ramaddaName = name;
        this.addLayer(image,true);
        image.north = north;
        image.west = west;
        image.south = south;
        image.east = east;
	image.opacity=this.params.imageOpacity;

        if (!this.imageLayers) this.imageLayers = {}
        if (!theArgs.isBaseLayer) {
            this.imageLayers[layerId] = image;
	    this.imageLayersList.push(image);
        }

	if(!this.noPointerEventsLayers) this.noPointerEventsLayers = [];
	this.noPointerEventsLayers.push(image);
	this.setNoPointerEvents();
        return image;
    },
    setNoPointerEvents: function() {
	if(!this.noPointerEventsLayers) return;
	//Do this later
	setTimeout(()=>{
	    this.noPointerEventsLayers.forEach(image=>{
		if(image.div) {
		    var childNodes = image.div.childNodes;
		    for(var i = 0, len = childNodes.length; i < len; ++i) {
			var element = childNodes[i].firstChild || childNodes[i];
			var lastChild = childNodes[i].lastChild;
			if (lastChild && lastChild.nodeName.toLowerCase() === "iframe") {
			    element = lastChild.parentNode;
			}
			element.style["pointer-events"]="none";
		    }
		}
	    });
	},1000);
    },
    addWMSLayer: function(name, url, layer, isBaseLayer, nonSelectable,args) {
	if(!args) args = {};
	let attrs = {
            wrapDateLine: MapUtils.defaults.wrapDateline,
        };
	if(args.opacity) attrs.opacity=args.opacity;
        var layer = MapUtils.createLayerWMS(name, url, {
            layers: layer,
            format: "image/png",
            isBaseLayer: false,
            srs: "epse:4326",
            transparent: true
        }, attrs);
        if (isBaseLayer) {
            layer.isBaseLayer = true;
            layer.visibility = false;
        } else {
            layer.isBaseLayer = false;
            layer.visibility = args.visible;
        }

        //If we have this here we get this error: 
        //http://lists.osgeo.org/pipermail/openlayers-users//2012-August/026025.html
        //        layer.reproject = true;
        this.addLayer(layer,nonSelectable);
	return layer;
    },
    addMapLayer: function(name, url, layer, isBaseLayer, isDefault) {
        var layer;
        if (/\/tile\//.exec(url)) {
            layer = MapUtils.createLayerXYZ(
                name, url, {
                    sphericalMercator: MapUtils.defaults.doSphericalMercator,
                    numZoomLevels: MapUtils.defaults.zoomLevels,
                    wrapDateLine: MapUtils.defaults.wrapDateline
                });
        } else {
            layer = MapUtils.createLayerWMS(name, url, {
                layers: layer,
                format: "image/png"
            }, {
                wrapDateLine: MapUtils.defaults.wrapDateline
            });
        }
        if (isBaseLayer)
            layer.isBaseLayer = true;
        else
            layer.isBaseLayer = false;
        layer.visibility = false;
        layer.reproject = true;
        this.addLayer(layer);
        if (isDefault) {
            this.haveAddedDefaultLayer = true;
            this.getMap().setLayerIndex(layer, 0);
            this.getMap().setBaseLayer(layer);
        }
    },

    getMapLayer:function(id) {
	let layer = RAMADDA_MAP_LAYERS_MAP[id];
	return layer?.layer;
    },
    getVectorLayerStyleMap: function(layer, args,ruleString) {
	//	ruleString = 'OWN_TYPE_NAME:~:.*Conservation.*:fillColor:red:strokeColor:black;OWN_TYPE_NAME:~:Joint:fillColor:blue';
        let props = {
            pointRadius: this.params.pointRadius,
            fillOpacity: this.params.fillOpacity,
            fillColor: this.params.fillColor,
            fill: this.params.fill,
            strokeColor: this.params.strokeColor,
            strokeWidth: this.params.strokeWidth,
            select_fillOpacity: this.params.fillOpacity,
            select_fillColor: "#666",
            select_strokeColor: "#666",
            select_strokeWidth: 1
        };
	if (args) $.extend(props, args);

        let temporaryStyle = $.extend({}, MapUtils.getVectorStyle("temporary"));
        $.extend(temporaryStyle, props);
/*        $.extend(temporaryStyle, {
            pointRadius: props.pointRadius,
            fillOpacity: props.fillOpacity,
            strokeWidth: props.strokeWidth,
            strokeColor: props.strokeColor,
        });*/
        let selectStyle = $.extend({}, MapUtils.getVectorStyle("select"));
        $.extend(selectStyle, {
            pointRadius: 3,
            fillOpacity: props.select_fillOpacity,
            fillColor: props.select_fillColor,
            strokeColor: props.select_strokeColor,
            strokeWidth: props.select_strokeWidth,
	    externalGraphic:props.externalGraphic	    
        });

        let defaultStyle = $.extend({}, MapUtils.getVectorStyle("default"));
        $.extend(defaultStyle, {
            pointRadius: props.pointRadius,
            fillOpacity: props.fillOpacity,
            fillColor: props.fillColor,
	    fillPattern: props.fillPattern,
            fill: props.fill,
            strokeColor: props.strokeColor,
            strokeWidth: props.strokeWidth,
	    externalGraphic:props.externalGraphic
        });

        let map = MapUtils.createStyleMap({
            "temporary": temporaryStyle,
            "default": defaultStyle,
            "select": selectStyle
        });

	ruleString =ruleString||this.getProperty("rules");
	let rules = [];
	if(Utils.isDefined(ruleString)) {
	    if(!Array.isArray(ruleString)) {
		let tmp = [];
		Utils.split(ruleString,";",true,true).forEach(line=>{
		    let toks = Utils.split(line,":");
		    //rules="OWN_TYPE_NAME:~:.*Conversation.*:fillColor:red"
		    if(toks.length<3) {
			console.log("bad rule:" + line);
			return;
		    }
		    let rule = {
			property: toks[0],
			type:toks[1],
			value: toks[2],
			style: {}
		    }
		    for(let i=3;i<toks.length;i+=2) {
			rule.style[toks[i]] = toks[i+1];
		    }
		    tmp.push(rule);
		});
		ruleString = tmp;
	    }
	    rules = ruleString;
	}

	if(rules.length>0) {
	    let mapRules =[];
	    rules.forEach(rule=>{
		let tmp = $.extend({},defaultStyle);
		let style = rule.style;
		if(typeof style == "string") {
		    let tmpStyle = {};
		    Utils.split(style,"\n",true,true).forEach(line=>{
			let toks = Utils.split(line,':',true,true);
			if(toks.length==2)
			    tmpStyle[toks[0]] = toks[1];
		    });
		    style = tmpStyle;
		}		    
		tmp = $.extend(tmp, style);
		rule.value = String(rule.value??"");
		let numeric =rule.value.match(/^\d*\.?\d*$/); 
		let string = rule.type=="~=" || rule.type=="NULL";
		let props = {
		    property: rule.property,
		    type: rule.type,
		    
		};
		if(rule.type==OpenLayers.Filter.Comparison.BETWEEN) {
		    let toks = Utils.split(String(rule.value),":",true,true);
		    if(toks.length==2) {
			props.lowerBoundary= +toks[0];
			props.upperBoundary= +toks[1];			
		    }
		} else {
		    props.value= numeric?+rule.value:rule.value;
		}		    
		let olRule = new OpenLayers.Rule({
		    filter: new OpenLayers.Filter.Comparison(props),
		    symbolizer: tmp
		});
		mapRules.push(olRule);
	    });
	    mapRules.push(new OpenLayers.Rule({elseFilter: true,symbolizer:args}));
	    let finalRule = new OpenLayers.Rule({
		filter: new OpenLayers.Filter.Comparison(),
		symbolizer: args
	    });
//	    mapRules.push(finalRule);

	    map.styles.default.addRules(mapRules);
	}
        return map;
    },

    getFeatureName: function(feature,dontCheckLabel) {
        let p = feature.attributes;
	let featureLabelProperty = this.params.featureLabelProperty;
	if(!dontCheckLabel && Utils.stringDefined(featureLabelProperty)) {
	    let value = p[featureLabelProperty];
	    if(value) {
		return value;
	    }
	}
        for (let attr in p) {
            let name = ("" + attr).toLowerCase();
            if (!(name.includes("label"))) continue;
            let value = this.getAttrValue(p, attr);
            if (value) return value;
        }
        for (let attr in p) {
            let name = ("" + attr).toLowerCase();
            if (!(name.includes("name"))) continue;
            let value = this.getAttrValue(p, attr);
            if (value) return value;
        }
    },
    getAttrValue: function(p, attr) {
        let value = "";
        if ((typeof p[attr] == 'object') || (typeof p[attr] == 'Object')) {
            let o = p[attr];
            if (o) {
                value = "" + o["value"];
            }
        } else {
            value = "" + p[attr];
        }
        return value;
    },

    searchFor: function(searchFor) {
        let _this = this;
        if (searchFor) searchFor = searchFor.trim();
        if (searchFor == "") searchFor = null;
        this.searchText = searchFor;
        let bounds = null;
        let toks = null;
        let doHelp = false;
        let download = $("#" + this.searchDiv + "_download").is(':checked');
        let attrs = [];
        let doOr = true;
        if (searchFor) {
            searchFor = searchFor.trim();
            if (searchFor == "?") {
                doHelp = true;
            }
            searchFor = searchFor.toLowerCase();
            toks = [];
            let tmp = Utils.split(searchFor,'|',true,true)
            if (tmp.length == 1) {
                tmp = Utils.split(searchFor,'&',true,true);
                doOr = tmp.length == 1;
            }
            for (let i = 0; i < tmp.length; i++) {
                let not = false;
                let equals = false;
                let tok = tmp[i];
                let tmp2 = tok.split(":");
                let field = "";
                let value = tok;
                if (tmp2.length > 1) {
                    field = tmp2[0];
                    value = tmp2[1];
                }
                if (value.startsWith("!")) {
                    not = true;
                    value = value.substring(1);
                }
                if (value.startsWith("=")) {
                    value = value.substring(1);
                    equals = true;
                }
                toks.push({
                    field: field,
                    value: value,
                    not: not,
                    equals: equals
                });
            }
        } else {
            this.searchMsg.html("");
	}

        for (let a in this.loadedLayers) {
            let layer = this.loadedLayers[a];
            if (layer.selectedFeature) {
                this.handleNofeatureclick(null, layer);
            }
            for (let f in layer.features) {
                let feature = layer.features[f];
                let p = feature.attributes;
                if (!searchFor) {
                    feature.featureVisibleSearch = true;
                    attrs.push(p);
                    continue;
                }
                if (doHelp) {
                    let space = "&nbsp;&nbsp;&nbsp;"
                    let help = "Enter, e.g.:<br>";
                    help += space + "<i>&lt;term&gt;</i> (any match)<br>";
                    help += space + "<i>=&lt;term&gt;</i> (exact match)<br>";
                    help += space + "<i>!&lt;term&gt;</i> (not match)<br>";
                    help += space + "<i>&lt;term 1&gt;|&lt;term 2&gt;</i> (match any)<br>";
                    help += space + "<i>&lt;term 1&gt;&amp;&lt;term 2&gt;</i> (match all)<br>";
                    help += "Or by field:<br>";
                    for (let attr in p) {
                        help += space + attr.toLowerCase() + ":" + "<i>&lt;term&gt;</i><br>";
                    }
                    this.showText(help);
                    return
                }
                let matches = false;
                let checkAll = false;
                let allMatched = true;
                let someMatched = false;
		//                if (tok.not) checkAll = true;
                for (let v in toks) {
                    let tok = toks[v];
                    for (let attr in p) {
                        if (tok.field != "" && tok.field != attr.toLowerCase()) {
                            continue;
                        }
                        let value = this.getAttrValue(p, attr);
                        value = value.toLowerCase().trim();
                        if (tok.equals) {
                            matches = (value == tok.value);
                        } else {
                            matches = value.includes(tok.value);
                        }
                        if (tok.not) {
                            matches = !matches;
                            if (!matches) {
                                break;
                            }
                        } else {
                            if (matches) break;
                        }
                    }
                    if (matches) someMatched = true;
                    if (!matches) allMatched = false;
                }

                if ((doOr && someMatched) || (!doOr && allMatched)) {
                    feature.featureVisibleSearch = true;
                    if (this.getFeatureVisible(feature)) {
                        attrs.push(p);
                    }
                } else {
                    feature.featureVisibleSearch = false;
                }
            }

            this.centerOnMarkers();
            if (download) {
                let csv = "";
                for (let i in attrs) {
                    let p = attrs[i];
                    for (let attr in p) {
                        if (csv != "") csv += ",";
                        csv += attr.toLowerCase();
                    }
                    csv += "\n";
                    break;
                }
                for (let i in attrs) {
                    let p = attrs[i];
                    let line = "";
                    for (let attr in p) {
                        let value = this.getAttrValue(p, attr);
                        if (value.includes(",")) {
                            value = "\"" + value + "\"";
                        }
                        if (line != "") line += ",";
                        line += value;
                    }
                    line += "\n";
                    csv += line;
                }
                Utils.makeDownloadFile("download.csv", csv);
            }
            this.setFeatureVisibility(layer);
	}
    },
    checkFeatureVisible: function(feature, redraw,debug) {
        let layer = feature.layer;
        let visible = this.getFeatureVisible(feature);
	if(feature.ramaddaId) {
            let cbx = this.getVisibilityCheckbox(feature.ramaddaId);
	    cbx.prop('checked',visible);
	}
        if (feature.originalStyle) {
	    this.setFeatureStyle(feature, feature.originalStyle);
        }
        let style = feature.style;
        if (!style) {
	    style = {};
            let defaultStyle = layer?layer.styleMap.styles["default"].defaultStyle:{};
            $.extend(style, defaultStyle);
	    this.setFeatureStyle(feature, style);
        }

        if (!visible) {
            style.display = 'none';
        } else {
            style.display = 'inline';
        }
        if (redraw) {
            if (!feature.isSelected)
                feature.renderIntent = null;
	    if(layer)
		this.drawFeature(layer,feature, feature.style || "default");
        }
        return visible;
    },

    getFeatureVisible: function(feature) {
        let visible = true;
        if (Utils.isDefined(feature.featureVisibleSearch) && !feature.featureVisibleSearch) {
            visible = false;
        }
        if (Utils.isDefined(feature.featureVisibleDate) && !feature.featureVisibleDate) {
            visible = false;
        }
        if (Utils.isDefined(feature.featureVisible) && !feature.featureVisible) {
            visible = false;
        }
        return visible;
    },
    setFeatureVisibility: function(layer) {
	if(!layer.features) return;
        let _this = this;
        let didSearch = this.searchText || (this.startDate && this.endDate);
        let bounds = null;
        let html = "";
        let didOn = false;
        let didOff = false;
        let cnt = 0;
        let onFeature = null;
        for (let i = 0; i < layer.features.length; i++) {
            let feature = layer.features[i];
            let visible = this.checkFeatureVisible(feature, false);
            if (!visible) {
                this.clearDateFeature(feature);
                didOff = true;
            } else {
                this.dateFeatureSelect(feature);
                didOn = true;
                cnt++;
                if (!onFeature) onFeature = feature;
                html += HtmlUtils.div([ATTR_TITLE,'',ATTR_CLASS, 'ramadda-map-feature', 'feature-index', '' + i], this.getFeatureName(feature));
                let geometry = feature.geometry;
                if (geometry) {
                    let fbounds = geometry.getBounds();
                    if (bounds) bounds.extend(fbounds);
                    else bounds = fbounds;
                }
            }
        }
        if (cnt == 1) {
            this.showText(this.getFeatureText(layer, onFeature));
        } else {
            if (didSearch || (didOn && didOff)) {
                let id = this.mapDivId + "_features";
                this.showText(HU.div(["id", id, "class", "ramadda-map-features"], html),true);
		/****
                $("#" + id + " .ramadda-map-feature").tooltip({
		    content: function() {
			let index = parseInt($(this).attr("feature-index"));
			feature =  layer.features[index];
			if(feature) {
			    let p = feature.attributes;
			    if(p) {
				return MapUtils.makeDefaultFeatureText(p);
			    }				
			}
			return null;
		    }
		    });
		    **/

                $("#" + id + " .ramadda-map-feature").click(function(e) {
                    let index = parseInt($(this).attr("feature-index"));
		    _this.dontShowText = true;
                    _this.handleFeatureclick(layer, layer.features[index], true,null,{strokeColor:'red'});
		    _this.dontShowText = false;
                });
                $("#" + id + " .ramadda-map-feature").mouseover(function() {
                    let index = parseInt($(this).attr("feature-index"));
                    _this.handleFeatureover(layer.features[index], true,{strokeColor:'red'});
                });
                $("#" + id + " .ramadda-map-feature").mouseout(function() {
                    let index = parseInt($(this).attr("feature-index"));
                    _this.handleFeatureout(layer.features[index], true);
                });

            } else {
                this.clearDateFeature();
		if(!this.params.displayDivSticky) 
		    this.showText("");
            }
        }
        if (this.searchMsg) {
            if (didSearch)
                this.searchMsg.html(cnt + " matched");
            else {
                this.searchMsg.html("");
		this.showText('');
	    }
        }
        layer.redraw();
        if (bounds) {
            this.zoomToExtent(bounds);
            this.getMap().setCenter(bounds.getCenterLonLat());
        } else {
            this.centerOnMarkers(this.dfltBounds, this.centerOnMarkersForce);
        }
    },
    getFeatureText: function(layer, feature) {
	if(feature.textGetter) {
	    let text =  feature.textGetter(feature);
	    if(text) {
		if(debugPopup) console.log("getFeatureText-feature has textGetter:");
		return text;
	    }
	}
	if(layer.textGetter) {
	    let text= layer.textGetter(feature);
	    if(text) {
		if(debugPopup) console.log("getFeatureText-layer has textGetter");
		return text;
	    }
	}
	if(this.textGetter) {
	    let text= this.textGetter(layer,feature);
	    if(text) {
		if(debugPopup) console.log("getFeatureText-map has textGetter:" + this.textGetter);
		return text;
	    }
	}

        let style = feature.style || feature.originalStyle || layer.style;
        let p = feature.attributes;
        let out = feature.popupText ?? feature.text;
        if (!out) {
            if (style && (style["balloonStyle"] || style["popupText"])) {
                out = style["balloonStyle"] || style["popupText"];
                for (let attr in p) {
                    //$[styleid/attr]
                    let label = MapUtils.makeLabel(attr);
                    let value = "";
                    if (typeof p[attr] == 'object' || typeof p[attr] == 'Object') {
                        let o = p[attr];
			value = "" + (o?o["value"]:'');
                    } else {
                        value = "" + p[attr];
                    }
		    let _attr = attr.toLowerCase();
                    out = out.replace("${" + style.id + "/" + attr + "}", value).replace("${" + attr+"}",value).replace("${" + _attr+"}",value);
                }
            } else {
		if(debugPopup) console.log("getFeatureText-using feature attributes");
		if(layer.textGetter) {
		    out= layer.textGetter(feature);
		} else {
		    out = MapUtils.makeDefaultFeatureText(p);
		}
	    }		
	}
	if(out && out.indexOf('${default}')>=0) {
	    out = out.replace('${default}',MapUtils.makeDefaultFeatureText(p));
	}
        return out;
    },
    onFeatureSelect: function(layer,event) {
	if(debugPopup) console.log("\tonFeatureSelect");
	//	console.trace();
	let _this = this;
        if (this.onSelect) {
            func = window[this.onSelect];
            func(this, layer);
	    if(debugPopup) console.log("\thas onSelect");
            return;
        }
        let feature = layer.feature;
	if(this.featureSelectHandler && this.featureSelectHandler(feature)) {
	    if(debugPopup) console.log("\thas featureSelectHandler");
	    return;
	}

	if(!this.getDoPopup()) {
	    return;
	}
        let out = this.getFeatureText(layer, feature);
	if(!out) {
	    if(debugPopup) console.log("\tno feature text");
	    return;
	}	    
	if(debugPopup) console.log("\thas feature text");
        if (this.currentPopup) {
            this.getMap().removePopup(this.currentPopup);
            this.currentPopup.destroy();
	    this.currentPopup = null;
        }

	let location = null;
	if(event && event.event && event.event.xy) {
	    location = this.getMap().getLonLatFromPixel(event.event.xy)
	}
	if(location==null) {
	    location = feature.geometry.getBounds().getCenterLonLat();
	}
	if(this.showPopupSlider(location, out)) {
	    return
	}

        let popup = this.makePopup(location,out);
        feature.popup = popup;
        popup.feature = feature;
        this.addPopup(popup);
    },

    onFeatureUnselect:  function(layer) {
        this.onPopupClose();
    },

    removeKMLLayer:  function(layer) {
	this.removeLayer(layer);
    },
    removeLayer:  function(layer,skipMapRemoval) {
	this.externalLayers = Utils.removeItem(this.externalLayers, layer);
	this.allLayers = Utils.removeItem(this.allLayers, layer);
	if(this.nonSelectLayers)
	    this.nonSelectLayers = Utils.removeItem(this.nonSelectLayers, layer);
	if(this.loadedLayers) {
	    this.loadedLayers = Utils.removeItem(this.loadedLayers, layer);
	}
	if(this.imageLayersList) {
	    this.imageLayersList = Utils.removeItem(this.imageLayersList, layer);
	}
	if(!skipMapRemoval)
            this.getMap().removeLayer(layer);
    },

    setDefaultCanSelect:  function(canSelect) {
        this.defaultCanSelect = canSelect;
    },

    getCanSelect:  function(canSelect) {
        if (((typeof canSelect) == "undefined") || (canSelect == null)) {
            return this.defaultCanSelect;
        }
        return canSelect;
    },

    addSelectCallback:  function(layer, canSelect, selectCallback, unselectCallback) {
        let _this = this;
        if (this.getCanSelect(canSelect)) {
            if (selectCallback == null || !Utils.isDefined(selectCallback))
                selectCallback = function(feature,layer,event) {
                    return _this.onFeatureSelect(layer,event)
                };
            if (unselectCallback == null || !Utils.isDefined(unselectCallback))
                unselectCallback = function(feature,layer,event) {
                    return _this.onFeatureUnselect(layer,event)
                };
            layer.selectCallback = selectCallback;
            layer.unselectCallback = selectCallback;
            /**
               layer.events.on({ 
               "featureselected": selectCallback,
               "featureunselected": unselectCallback,
               });

               if (this.highlightOnHover) {
               this.getMap().addControl(highlight);
               highlight.activate();   
               }
               this.getMap().addControl(select);
               select.activate();   
            */
	}     
    },

    initDates:  function(layer) {
        let _this = this;
        let features = layer.features;
        let didDate = false;
        let didYear = false;
        this.hasDates = false;
        this.dates = [];
        this.dateFeatures = [];
        this.minDate = null;
        this.maxDate = null;
        for (let i = 0; i < features.length; i++) {
            let feature = features[i];
            let p = feature.attributes;
            for (let attr in p) {
                let name = ("" + attr).toLowerCase();
                let isYear = name.includes("year") || name.includes("_yr");
                let isDate = name.includes("date");
                if (!(isDate || isYear)) continue;
                let value = this.getAttrValue(p, attr);
                if (!value) continue;
                if (value == "0") continue;
                try {
                    let date = Utils.parseDate(value);
		    if(!date) continue;
		    if(date && isNaN(date.getTime())) continue;
                    if (isYear) didYear = true;
                    else didDate = true;
                    if (this.startDate != null && this.endDate != null) {
                        if (date.getTime() < this.startDate.getTime() || date.getTime() > this.endDate.getTime()) {
                            continue;
                        }
                    }
		    this.dates.push(date);
		    
                    this.dateFeatures.push(feature);
                    this.minDate = this.minDate == null ? date : this.minDate.getTime() > date.getTime() ? date : this.minDate;
                    this.maxDate = this.maxDate == null ? date : this.maxDate.getTime() < date.getTime() ? date : this.maxDate;
                    feature.featureDate = date;
                    this.hasDates = true;
                    break;
                } catch (e) {
                    console.log("error:" + e);
                }
            }
        }
        if (this.hasDates && this.params.showDates) {
            let options = null;
            if (didYear)
                options = {
                    year: 'numeric'
                };
            $("#" + this.mapDivId + "_footer").html(HtmlUtils.div(["class", "ramadda-map-animation", "id", this.mapDivId + "_animation"], ""));
            this.animation = $("#" + this.mapDivId + "_animation");
            let ticksDiv = HtmlUtils.div(["class", "ramadda-map-animation-ticks", "id", this.mapDivId + "_animation_ticks"], "");
            let infoDiv = HtmlUtils.div(["class", "ramadda-map-animation-info", "id", this.mapDivId + "_animation_info"], "");
            this.animation.html(ticksDiv + infoDiv);
            let startLabel = Utils.formatDate(this.minDate, options);
            let endLabel = Utils.formatDate(this.maxDate, options);
            this.animationTicks = $("#" + this.mapDivId + "_animation_ticks");
            this.animationInfo = $("#" + this.mapDivId + "_animation_info");
            let center = "";
            if (this.startDate && this.endDate) {
                center = HtmlUtils.div(["id", this.mapDivId + "_ticks_reset", "class", "ramadda-map-animation-tick-reset"], "Reset");
            }
            let info = "<table width=100%><tr valign=top><td width=40%>" + startLabel + "</td><td align=center width=20%>" + center + "</td><td align=right width=40%>" + endLabel + "</td></tr></table>";
            this.animationInfo.html(info);
            if (this.startDate && this.endDate) {
                let reset = $("#" + this.mapDivId + "_ticks_reset");
                reset.click(function() {
                    _this.startDate = null;
                    _this.endDate = null;
                    _this.startFeature = null;
                    _this.endFeature = null;
                    _this.setFeatureDateRange(layer, "Resetting range...");
                });
            }
            let width = this.animationTicks.width();
            let percentPad = width > 0 ? 5 / width : 0;
            //            console.log("w:" + width + " " + percentPad);
            let html = "";
            let start = this.minDate.getTime();
            let end = this.maxDate.getTime();
            if (this.startDate != null && this.endDate != null) {
                start = this.startDate.getTime();
                end = this.endDate.getTime();
            }
            let range = end - start;
            if (range > 0) {
                for (let i = 0; i < this.dates.length; i++) {
                    let date = this.dates[i];
                    let time = date.getTime();
                    if (time < start || time > end) continue;
                    let feature = this.dateFeatures[i];
                    feature.dateIndex = i;
                    let percent = 100 * (time - start) / range;
                    percent = percent - percent * percentPad;
                    if(!options) options = {};
                    let fdate = date.toLocaleDateString("en-US", options);
                    let name = this.getFeatureName(feature);
		    if(name && name.length>100) name = name.substring(0,99)+'...';
                    let tooltip = "";
                    tooltip += name != null ? name + "<br>" : "";
                    tooltip += fdate;
//		    tooltip=HU.div([ATTR_STYLE,HU.css('max-height','300px','overflow-y','auto')], tooltip);
                    tooltip += "<br>shift-click: set visible range<br>cmd/ctrl-click:zoom";
                    tooltip += "";
                    html += HtmlUtils.div(["id", this.mapDivId + "_tick" + i, "feature-index", "" + i, "style", "left:" + percent + "%", "class", "ramadda-map-animation-tick", "title", tooltip], "");
                }
            }
            this.animationTicks.html(html);
            let tick = $("#" + this.mapDivId + "_animation .ramadda-map-animation-tick");
            tick.tooltip({
                content: function() {
                    return $(this).prop('title');
                },
                position: {
                    my: "left top",
                    at: "left bottom+2"
                },
                classes: {
                    "ui-tooltip": "ramadda-popup"
                }
            });
            tick.mouseover(function() {
                let index = parseInt($(this).attr("feature-index"));
                let feature = _this.dateFeatures[index];
                _this.handleFeatureover(feature, true);
            });
            tick.mouseout(function() {
                let index = parseInt($(this).attr("feature-index"));
                let feature = _this.dateFeatures[index];
                _this.handleFeatureout(feature, true);
            });
            tick.click(function(evt) {
                let index = parseInt($(this).attr("feature-index"));
                let feature = _this.dateFeatures[index];
                if (evt.shiftKey) {
                    if (_this.startDate == null) {
                        _this.startDate = feature.featureDate;
                        _this.startFeature = feature;
                        _this.dateFeatureSelect(feature);
                    } else if (_this.endDate == null) {
                        _this.endDate = feature.featureDate;
                        _this.endFeature = feature;
                        _this.dateFeatureSelect(feature);
                    } else {
                        _this.dateFeatureSelect(null);
                        _this.startDate = feature.featureDate;
                        _this.startFeature = feature;
                        _this.endDate = null;
                        _this.endFeature = null;
                        _this.dateFeatureSelect(feature);
                    }
                    if (_this.startDate != null && _this.endDate != null) {
                        if (_this.startDate.getTime() == _this.endDate.getTime()) {
                            _this.startDate = null;
                            _this.startFeature = null;
                            _this.endDate = null;
                            _this.endFeature = null;
                            _this.dateFeatureSelect(null);
                            return;
                        } else if (_this.startDate.getTime() > _this.endDate.getTime()) {
                            let tmp = _this.startDate;
                            _this.startDate = _this.endDate;
                            _this.endDate = tmp;
                            tmp = _this.startFeature;
                            _this.startFeature = _this.endFeature;
                            _this.endFeature = tmp;
                        }
                        _this.setFeatureDateRange(feature.layer);
                    }
                } else {
                    let center = evt.metaKey || evt.ctrlKey;
                    if (_this.startDate != null || _this.endDate != null) {
                        _this.startDate = null;
                        _this.endDate = null;
                        //                        _this.setFeatureDateRange(feature.layer, feature.featureDate,"Resetting range...");
                        //                            center = true;
                    }
                    _this.clearDateFeature();
		    _this.handleFeatureclick(feature.layer, feature, center,null,{strokeColor:'red'});
                }
            });
        }
    },

    setFeatureDateRange:  function(layer, msg) {
        this.dateFeatureSelect(null);
        if (!msg) msg = "Setting range...";
        this.animationTicks.html(msg);
        setTimeout(() => this.setFeatureDateRangeInner(layer), 100);
    },

    setFeatureDateRangeInner:  function(layer) {
        let features = layer.features;
        if (layer.selectedFeature) {
            this.unselectFeature(layer.selectedFeature);
        }
        for (let i = 0; i < features.length; i++) {
            let feature = features[i];
            if (this.startDate && this.endDate && feature.featureDate) {
                feature.featureVisibleDate = this.startDate.getTime() <= feature.featureDate.getTime() &&
                    feature.featureDate.getTime() <= this.endDate.getTime();
            } else {
                feature.featureVisibleDate = true;
            }
        }
        this.setFeatureVisibility(layer);
        this.initDates(layer);
    },
    dateFeatureSelect:  function(feature) {
        let tick = this.getFeatureTick(feature);
        tick.css("background-color", this.params.tickSelectColor);
        tick.css("zIndex", "100");
    },
    dateFeatureOver:  function(feature) {
	this.closeTicks();
        let tick = this.getFeatureTick(feature);
        tick.css("background-color", this.params.tickHoverColor);
        tick.css("zIndex", "100");
        //In case some aren't closed
        //        this.getFeatureTick(null).tooltip("close");
        if (this.tickOpen) this.tickOpen.tooltip("close");
        this.tickOpen = tick;
        tick.tooltip("open");
    },
    dateFeatureOut:  function(feature) {
	this.closeTicks();
        let tick = this.getFeatureTick(feature);
        if (feature && (feature == this.startFeature || feature == this.endFeature)) {
            tick.css("background-color", this.params.tickSelectColor);
            tick.css("zIndex", "0");
        } else {
            tick.css("background-color", "");
            tick.css("zIndex", "0");
        }
        this.tickOpen = null;
    },
    closeTicks:function() {
	let all =this.getAllTicks();
	all.tooltip("close");
        all.css("background-color", "");
    },

    getAllTicks:function() {
	return jqid(this.mapDivId + "_animation_ticks").find(".ramadda-map-animation-tick");
    },
    getFeatureTick:  function(feature) {
        if (!feature)
            return $("#" + this.mapDivId + "_animation_ticks" + " .ramadda-map-animation-tick");
        return $("#" + this.mapDivId + "_tick" + feature.dateIndex);
    },
    clearDateFeature:  function(feature) {
        let element = feature != null ? $("#" + this.mapDivId + "_tick" + feature.dateIndex) : $("#" + this.mapDivId + "_animation_ticks .ramadda-map-animation-tick");
        element.css("background-color", "");
        element.css("zIndex", "0");
    },


    initMapVectorLayer:  function(layer, url, canSelect, selectCallback, unselectCallback, loadCallback, zoomToExtent,errorCallback) {
        let _this = this;
	if(layer.visibility) {
            this.showLoadingImage(true);
	}
	layer.isMapLayer = true;
        layer.canSelect = canSelect;
        this.loadedLayers.push(layer);
        layer.events.on({
            "loadend": function(e) {
                _this.hideLoadingImage(true);
                if (e.response && Utils.isDefined(e.response.code) && e.response.code == OpenLayers.Protocol.Response.FAILURE) {
		    if(errorCallback) {
			errorCallback(url,e.response);
		    } else {
			console.log("An error occurred loading the map:" + url+"\n" + JSON.stringify(e.response, null, 2));
		    }
                    return;
                }

                if (zoomToExtent) {
                    let dataBounds = layer.getDataExtent();
                    if (dataBounds) {
                        _this.zoomToExtent(dataBounds, true);
                    }
                } else {
                    if (_this.centerOnMarkersCalled) {
                        if (!_this.dfltBounds)
                            _this.centerOnMarkers(_this.dfltBounds, _this.centerOnMarkersForce);
                    }
                }
                if (loadCallback) {
                    loadCallback(_this, layer);
                }
                if (layer.features.length == 1 && _this.params.displayDiv) {
                    _this.fixedText = true;
                    _this.showText(_this.getFeatureText(layer, layer.features[0]));
                }
                _this.initDates(layer);
		return

            }
        });

        this.addLayer(layer);
        this.addSelectCallback(layer, canSelect, selectCallback, unselectCallback);
    },

    getLayerProperty:function(layer, prop, idx,name, dflt) {
	let id = Utils.makeId(name);
	let v = this.getProperty(prop  +"_"+id, this.getProperty(prop+idx,this.getProperty(prop, dflt)));
	return v;
    },
    addMapFileLayer:  function(layer, url, name, canSelect, selectCallback, unselectCallback, args, loadCallback, zoomToExtent,errorCallback) {

	let idx  = this.loadedLayers.length+1;
	let opts =  {
            strokeColor: this.getLayerProperty(layer, "layerStrokeColor", idx,name, "blue"),
            strokeWidth: this.getLayerProperty(layer, "layerStrokeWidth", idx,name, 1),
	    fillColor:this.getLayerProperty(layer, "layerFillColor", idx,name, "#ccc"),
	    fillOpacity:this.getLayerProperty(layer, "layerFillOpacity", idx,name, 0.4)
        }
	let highlightStyle = {};
	$.extend(highlightStyle, this.highlightStyle);
	for(a in highlightStyle) {
	    let prop = String(a);
	    prop = "highlight" + prop.substring(0,1).toUpperCase() + prop.substring(1);
	    highlightStyle[a] = this.getLayerProperty(layer, prop,  idx,name,  highlightStyle[a]);
	}

	layer.highlightStyle = highlightStyle;
	if(this.getLayerProperty(layer, "noHighlight", idx,name, false)) {
	    layer.noHighlight = true;
	} 

	args = args||{};
	$.extend(opts, args);

        layer.styleMap = this.getVectorLayerStyleMap(layer, opts);
	this.checkLayerToggle(name,layer,idx,opts);
        this.initMapVectorLayer(layer, url, canSelect, selectCallback, unselectCallback, loadCallback, zoomToExtent,errorCallback);
        return layer;
    },

    addGpxLayer: function(name, url,
			  canSelect, selectCallback, unselectCallback, args, loadCallback, zoomToExtent,errorCallback) {
	let layer = MapUtils.createLayerGPX(this,name,url);
	this.addMapFileLayer(layer, url, name, canSelect, selectCallback, unselectCallback, args, loadCallback, zoomToExtent,errorCallback);
	return layer;
    },

    printLayers:function(label) {
	console.log(label);
	this.getMap().layers.forEach(l=>{
	    if(l.name.indexOf("Cb")>=0)
		console.log("\t" + l.name);
	});
    },

    addGeoJsonLayer:  function(name, url, canSelect, selectCallback, unselectCallback, args, loadCallback, zoomToExtent,errorCallback) {
	if(this.hadInitialZoom) zoomToExtent=false;
	let layer = MapUtils.createLayerGeoJson(this,name,url);
	if(args) {
	    if(Utils.isDefined(args.zoomToExtent))
		zoomToExtent=args.zoomToExtent;
	}
	this.addMapFileLayer(layer, url, name, canSelect, selectCallback, unselectCallback, args, loadCallback, zoomToExtent,errorCallback);
	return layer;
    },


    addKMLLayer:  function(name, url, canSelect, selectCallback, unselectCallback, args, loadCallback, zoomToExtent,errorCallback) {
	if(url.match(".kmz")) {
	    let div = $("<div  class=ramadda-map-message>Note: KMZ files are not supported</div>")[0];
            this.getMap().viewPortDiv.appendChild(div);
	    return;
	}

        let layer = MapUtils.createLayerKML(this,name, url);
	this.addMapFileLayer(layer, url, name, canSelect, selectCallback, unselectCallback, args, loadCallback, zoomToExtent,errorCallback);
        return layer;
    },

    checkLayerToggle:function(name,layer,idx,opts) {
	let visible = this.getLayerProperty(layer, "layerVisible", idx,name, true);
	if(!visible) {
	    layer.setVisibility(false);
	}

	if(this.params["showLayerToggle"]) {
	    let color = Utils.addAlphaToColor(opts.fillColor,0.4);
            let cbx = HU.span([], HtmlUtils.checkbox(this.mapDivId + "_layertoggle"+idx, ["title", "Toggle Layer"], visible,name)) +" ";
	    cbx = HU.span([STYLE,HU.css('margin-right','5px','padding','5px','background',color)], cbx);
	    $("#" + this.mapDivId+"_header").append(" " +cbx);
	    $("#" + this.mapDivId + "_layertoggle"+idx).change(function() {
		if($(this).is(':checked')) {
		    layer.setVisibility(true);
		} else {
		    layer.setVisibility(false);
		}
	    });
	    
	}
    },

    createXYZLayer:  function(name, url, attribution,notBaseLayer,visible) {
	visible=  Utils.getBoolean(visible);
        let options = {
            sphericalMercator: MapUtils.defaults.doSphericalMercator,
            numZoomLevels: MapUtils.defaults.zoomLevels,
            wrapDateLine: MapUtils.defaults.wrapDateline,
	    isBaseLayer: !notBaseLayer,
	    visibility: visible
        };
        if (attribution)
            options.attribution = attribution;
        return MapUtils.createLayerXYZ(name, url, options);
    },

    addBaseLayers:  function() {
	this.mapLayers = Utils.mergeLists([], RAMADDA_MAP_LAYERS);
        let dflt = this.params.defaultMapLayer || "osm";
        if (!this.haveAddedDefaultLayer && dflt) {
	    let index = -1;
	    let dfltLayer = this.mapLayers.find((layer,idx)=>{
		if(layer.id==dflt) {
		    index= idx;
		    return true;
		}
	    });
            if (index >= 0) {
                this.mapLayers.splice(index, 1);
                this.mapLayers.splice(0, 0, dfltLayer);
            }
        }

        this.firstLayer = null;
        this.hybridLayer = null;
        this.defaultOLMapLayer = null;

	this.baseLayers = {};
	this.numberOfBaseLayers = 0;

	let l = "";
        for (let i = 0; i < this.mapLayers.length; i++) {
            let mapLayer = this.mapLayers[i];
	    if(!mapLayer.isForMap()) continue;
            let newLayer = this.makeMapLayer(mapLayer);
            if (this.firstLayer == null) {
                this.firstLayer = newLayer;
            }
            if (newLayer != null) {
		if(l!="") l+=",";
		l+=mapLayer.id+":" + newLayer.name;
                newLayer.ramaddaId = mapLayer.id;
		if(!newLayer.isBaseLayer) {
		    this.addLayer(newLayer);
		} else {
		    this.baseLayers[mapLayer.id] = newLayer;
                    if (mapLayer.id == this.params.defaultMapLayer) {
			this.defaultOLMapLayer = newLayer;
                    }
		    this.addBaseLayer(newLayer);
		}
            }
	}
	//	console.log(l);

	this.graticule= this.createGraticule({
	    strokeColor: "#888",
	    strokeWidth: 2,
	    strokeOpacity: 0.5
	});
        this.getMap().addControl(this.graticule);
    },
    createGraticule:function(style) {
	return MapUtils.createGraticule({
            layerName: "Lat/Lon Lines",
	    lineSymbolizer: style,
	    xautoActivate:false,
            numPoints: 2,
            labelled: true,
            visible: this.params.showLatLonLines===true,
	    //Pass this because the default Canvas renderer can't render dash style
	    renderers: ['VML', 'SVG']		
        });
    },


    makeMapLayer:function(mapLayer) {
	let layer;
	if(typeof mapLayer =="string") 
	    layer = RAMADDA_MAP_LAYERS_MAP[mapLayer];
	else
	    layer = mapLayer;
	if(layer) {
	    let l= layer.createMapLayer(this);

	    if(layer.opts.alias) {
		this.baseLayers[layer.opts.alias] = l;
	    }
	    if(layer.opts.refresh) {
		let redrawFunc = () =>{
		    if(l.getVisibility()) {
			l.redraw(true);
		    }
		    setTimeout(redrawFunc,1000*layer.opts.refresh);
		};
		setTimeout(redrawFunc,1000*layer.opts.refresh);
	    }
	    return l;
	}
	if (/\/tile\//.exec(mapLayer)) {
            let layerURL = mapLayer;
            return MapUtils.createLayerXYZ(
                "ESRI China Map", layerURL, {
                    sphericalMercator: MapUtils.defaults.doSphericalMercator,
                    numZoomLevels: MapUtils.defaults.zoomLevels,
                    wrapDateLine: MapUtils.defaults.wrapDateline
                });
        } else {
            let match = /wms:(.*),(.*),(.*)/.exec(mapLayer);
            if (!match) {
                console.log("no match for map layer:" + mapLayer);
		return null;
            }
            return this.addWMSLayer(match[1], match[2], match[3], true);
        }
	return null;
    },
    setShowOverviewMap:function(v,args) {
	if(!v) {
	    if(this.overviewMap) {
		this.overviewMap.destroy();
	    }
	    this.overviewMap=null;
	    return;
	}

	if(!this.overviewMap) {
	    let opts = 	{
		maximized:true,
		autoPan:true};
	    if(args)
		$.extend(opts,args)
	    this.getMap().addControl(this.overviewMap = new OpenLayers.Control.OverviewMap(opts));
	}
    },
    setGraticulesVisible:function(visible,style) {
	this.params.showLatLonLines=visible;
	if(this.graticule) {
	    if(style) {
		this.graticule.gratLayer.visibility = visible;
		this.graticule.deactivate();
		this.graticule.gratLayer.redraw();
		this.getMap().removeControl(this.graticule);
		this.graticule = this.createGraticule(style);
		this.getMap().addControl(this.graticule);
	    }
	    this.graticule.gratLayer.visibility = visible;
	    if(!visible)
		this.graticule.deactivate();
	    else
		this.graticule.activate();	    
	    this.graticule.gratLayer.redraw();
	}
    },
    addBaseLayer: function(layer) {
        this.addLayer(layer);
	this.numberOfBaseLayers++;

    },
    getBaseLayer: function(id) {
	if(this.baseLayers) return this.baseLayers[id];
    },
    makeSimpleWms:  function(mapLayer) {
	let url = "/repository/wms?version=1.1.1&request=GetMap&layers=" + mapLayer +"&FORMAT=image%2Fpng&SRS=EPSG%3A4326&BBOX=-180.0,-80.0,180.0,80.0&width=400&height=400"
	return this.addWMSLayer(Utils.makeLabel(mapLayer) +" background", url, mapLayer, true);
    },


    initSearch:  function(inputId) {
	let _this = this;
        $("#" + inputId).keyup(function(e) {
            if (e.keyCode == 13) {
                _this.searchMarkers($("#" + inputId).val());
            }
        });
    },


    getVisibilityCheckbox:function(ramaddaId) { 
	let cbx =  $('#' + "visible_" + this.mapId + "_" + ramaddaId);
	//Check for the _ id problem
	if(cbx.length==0) {
	    cbx =  $('#' + "visible_" + this.mapId + "_" + ramaddaId.replace(/-/g,'_'));
	}
	return cbx;
    },

    searchMarkers:  function(text) {
        text = text.trim();
        text = text.toLowerCase();
        let all = text == "";
        let cbxall = $(':input[id*=\"' + "visibleall_" + this.mapId + '\"]');
        cbxall.prop('checked', all);
        let bounds = MapUtils.createBounds();
        let cnt = 0;
        if (this.markers) {
            let list = this.getMarkers();
            for (let idx = 0; idx < list.length; idx++) {
                marker = list[idx];
                let visible = true;
                let cbx = this.getVisibilityCheckbox(marker.ramaddaId);
                let block = $('#' + "block_" + this.mapId + "_" + marker.ramaddaId);
                let name = marker.name;
                if (all) visible = true;
                else if (!Utils.isDefined(name)) {
                    visible = false;
                } else {
                    visible = name.toLowerCase().includes(text);
                }
                if (visible) {
                    marker.style.display = 'inline';
                    if (marker.location)
                        bounds.extend(marker.location);
                    cnt++;
                } else {
                    marker.style.display = 'none';
                }
                if (visible)
                    block.show();
                else
                    block.hide();
                cbx.prop('checked', visible);
            }
            this.markers.redraw();
        }
        if (this.boxes && this.boxes.markers) {
            for (let marker in this.boxes.markers) {
                marker = this.boxes.markers[marker];
                name = marker.name;
                let visible = true;
                if (all) visible = true;
                else if (!Utils.isDefined(name)) {
                    visible = false;
                } else {
                    visible = name.toLowerCase().includes(text);
                }
                marker.display(visible);
                if (visible) {
                    let b = this.transformProjBounds(marker.bounds);
                    bounds.extend(b);
                    cnt++;
                }
            }
            this.boxes.redraw();
        }

        if (this.lines) {
            let features = this.lines.features;
            for (let i = 0; i < features.length; i++) {
                let line = features[i];
                name = line.name;
                let visible = true;
                if (all) visible = true;
                else if (!Utils.isDefined(name)) {
                    visible = false;
                } else {
                    visible = name.toLowerCase().includes(text);
                }
                if (visible) {
                    line.style.display = 'inline';
                } else {
                    line.style.display = 'none';
                }
            }
            this.lines.redraw();
        }
        if (cnt > 0) {
            this.centerOnMarkers(bounds, true);
        }
    },

    setLatLonReadout:  function(llr) {
        this.latlonReadout = llr;
    },

    getMap:  function() {
	if(getMapDebug)
	    console.trace();
        return this.map;
    },
    setDragPanEnabled:function(enabled) {
	if(this.dragPanControl) {
            if (!enabled) 
		this.dragPanControl.deactivate();
	    else
		this.dragPanControl.activate();	    
        }
    },
    setZoomPanEnabled:function(enabled) {
	if(!enabled) {
	    if(this.panZoomControl) 
		this.panZoomControl.destroy();
	    this.panZoomControl= null;
	} else {
	    if(!this.panZoomControl) 
		this.getMap().addControl(this.panZoomControl = new OpenLayers.Control.PanZoom());
	}
    },

    initMap:  function(doRegion) {
	let _this = this;
        this.startTime = Date.now();
        if (this.inited)
            return;
        this.inited = true;
        this.dragPanControl = new OpenLayers.Control.Navigation({
            zoomWheelEnabled: this.params.scrollToZoom,
            dragPanOptions: {
                enableKinetic: true
            }
        });
        this.getMap().addControl(this.dragPanControl);
	this.setDragPanEnabled(this.params.enableDragPan);
	if(this.params.scrollToZoom) {
	    //	$("#"+this.mapDivId+"_themap").attr('tabindex','1');
	    const el = document.querySelector("#"+this.mapDivId+"_themap");
	    this.checkedBaseLayerDiv = false;
	    //A big hack. We want to not pass the wheel event up to the main
	    //view because then not only do we zoom in the map we also scroll
	    //the page. Howwever, when the layer switcher is popped up
	    //we want to catch the wheel event so we can scroll
	    if(el) {
		el.onwheel = (event)=>{
		    if(!this.baseLayerDiv) {
			let div = $('#'+this.mapDivId).find('.layersDiv');		    
			if(div.length>0) {
			    this.baseLayerDiv = div;
			}
		    }
		    if(this.baseLayerDiv && this.baseLayerDiv.is(':visible')) {
			return;
		    }
		    
		    if(this.currentPopup && this.currentPopup.div) {
			return;
		    }
		    event.preventDefault();
		}
	    }
	}

        if (this.params.showZoomPanControl && !this.params.showZoomOnlyControl) {
	    this.setZoomPanEnabled(true);

        }
        if (this.params.showZoomOnlyControl && !this.params.showZoomPanControl) {
            this.getMap().addControl(new OpenLayers.Control.Zoom());
        }
        if (this.params.showScaleLine) {
            this.getMap().addControl(new OpenLayers.Control.ScaleLine());
        }
	if(this.params.showOverviewMap) {
	    this.setShowOverviewMap(true);
	}


        let keyboardControl = new OpenLayers.Control();
        let control = new OpenLayers.Control();
        let callbacks = {
            keyup: function(evt) {
		_this.handleKeyUp(evt);
	    },
            keydown: function(evt) {
		_this.handleKeyDown(evt);
            }
	};
        let handler = new OpenLayers.Handler.Keyboard(control, callbacks, {});
        handler.activate();
        this.getMap().addControl(keyboardControl);
        this.getMap().addControl(new OpenLayers.Control.KeyboardDefaults());

	
	if(this.params.linkMouse) {
	    this.getMap().events.register("mousemove", this.getMap(), event=>{
                if (event.shiftKey) {
		    if(this.sharedMouse) {
			this.getMarkersLayer().removeFeatures([this.sharedMouse],{silent:true});
		    }
		    let location = this.getMap().getLonLatFromPixel(event.xy)
		    location = this.transformProjPoint(location);
		    ramaddaMapShareState(this,{mouse:location});
		}
	    });
	}


        if (this.params.showLayerSwitcher) {
            this.getMap().addControl(new OpenLayers.Control.LayerSwitcher());
        }


        if (this.params.showLatLonPosition) {
	    this.initMousePositionReadout();
        }
        if (this.params.showLocationSearch) {
            this.initLocationSearch();
        }

        if (this.defaultBounds) {
            this.hadDefaultBounds = true;
	}

        if (false && this.defaultLocation && !this.defaultBounds) {
            let center = this.defaultLocation;
            let offset = 10.0;
            this.defaultBounds = MapUtils.createBounds(center.lon - offset, center.lat - offset, center.lon + offset, center.lat + offset);
	    if(debugBounds)
		console.log("setting default bounds-2:" + center.lon +" " + center.lat +" bounds:" + this.defaultBounds);
            this.defaultLocation = null;
        }

	this.applyDefaultLocation();

        for (let i = 0; i < this.initialLayers.length; i++) {
            this.addLayer(this.initialLayers[i]);
        }
        this.initialLayers = [];


	
        if (doRegion) {
            this.addRegionSelectorControl();
        }

        let cbx = $(':input[id*=\"' + "visible_" + this.mapId + '\"]');
	let toggle = ()=>{
            this.checkImageLayerVisibility();
            this.checkMarkerVisibility();
            this.checkLinesVisibility();
            this.checkBoxesVisibility();
	    this.centerOnMarkerLayer();
	};
        cbx.change(event =>{toggle();});
        let cbxall = $(':input[id*=\"' + "visibleall_" + this.mapId + '\"]');
        cbxall.change(event=> {
            cbx.prop("checked", cbxall.is(':checked'));
	    toggle();
        });

	for(let markerIdx=1;true;markerIdx++) {
	    let marker = this.getProperty("marker" + markerIdx);
	    if(!marker) break;
	    this.addMarkerEmbed(marker);
	}	
    },

    finishInit:function() {
    },


    destroyMousePositionReadout:function() {
	if(this.mousePositionReadout) {
	    this.mousePositionReadout.destroy();
	}
	this.mousePositionReadout  =null;
    },

    finishMarkers:function() {
	if(!this.markers) return;
    },

    initMousePositionReadout:function() {
	if(this.mousePositionReadout) return;
        if (!this.latlonReadout)
            this.latlonReadout = this.mapId + "_latlonreadout";
        let latLonReadout = GuiUtils.getDomObject(this.latlonReadout);
        if (latLonReadout) {
	    this.getMap().addControl(this.mousePositionReadout = new OpenLayers.Control.MousePosition({
                numDigits: 5,
                element: latLonReadout.obj,
                prefix: ""
            }));
        } else {
            this.getMap().addControl(this.mousePositionReadout = new OpenLayers.Control.MousePosition({
                numDigits: 5,
                prefix: ""
            }));
        }
    },

    handleKeyUp:function(evt) {
	if(this.keyUpListener) {
	    this.keyUpListener(evt);
	}
    },
    handleKeyDown:function(evt) {
	if(this.keyDownListener) {
	    if(this.keyDownListener(evt)) return;
	}


	if(evt.ctrlKey && evt.shiftKey) {
	    if(evt.key=='B') {
		this.locationChangedInner(true);
	    }
	    return;
	}
        if (evt.keyCode == 79 || evt.key=='Shift') {
            if (!this.imageLayersList) return;
	    if(this.ignoreKeyEvents) return;
	    this.imageLayersList.forEach(image=>{
		if(!image.getVisibility()) return;
		
                if (!Utils.isDefined(image.opacity)) {
                    image.opacity = 1.0;
                }
                opacity = image.opacity;
                if (evt.shiftKey) {
                    opacity += .1;
                } else {
                    opacity -= 0.1;
                }
                if (opacity < 0) opacity = 0;
                else if (opacity > 1) opacity = 1;
//		if(evt.key=='Shift') opacity = 0.1;
                image.setOpacity(opacity);
            });
        }
        if (evt.keyCode == 84) {
            if (this.selectImage) {
                this.selectImage.setVisibility(!this.selectImage.getVisibility());
            }
        }
    },
    addKeyUpListener:function(listener) {
	this.keyUpListener = listener;
    },
    addKeyDownListener:function(listener) {
	this.keyDownListener = listener;
    },
    applyDefaultLocation:  function() {	
	if(debugBounds)
	    console.log("apply default location:" + this.defaultLocation);
	if(this.defaultLocation) {
            let projPoint = this.transformLLPoint(this.defaultLocation);
            this.getMap().setCenter(projPoint);
	    if(!(this.params.initialZoom>=0)) {
		if(debugBounds)  console.log("zoomTo:",4);
		this.zoomTo(4);
	    }
	    this.defaultLocation = null;
	} else  if (this.defaultBounds) {
            let llPoint = this.defaultBounds.getCenterLonLat();
            let projPoint = this.transformLLPoint(llPoint);
	    if(debugBounds)
		console.log("applying default bounds: center:" +  llPoint +" b:" + this.defaultBounds +" zoom:" + this.params.initialZoom);
            this.getMap().setCenter(projPoint);
	    let extent = this.transformLLBounds(this.defaultBounds);
            this.zoomToExtent(extent);
	    if(this.params.initialZoom<0) {
		let width = this.defaultBounds.right-this.defaultBounds.left;
		let zoom = -1;
		//a hack for zoomed in boxes
		zoom = this.map.getZoomForExtent(extent)+2;
		if(debugBounds)
		    console.log("overriding initialZoom:",zoom);
		this.params.initialZoom = zoom;
	    }
            this.defaultBounds = null;
            this.getMap().setCenter(projPoint);
	    return;
        } else {
	    let layers =this.allLayers.filter(layer=>{
		if(!layer.ramaddaId) return false;
		return !layer.isBaseLayer && this.isLayerVisible(layer.ramaddaId) && layer.initialVisibility;
	    });
	    let bounds = null;
	    if(layers.length) {
		layers.forEach(layer=>{
		    let b =this.getLayerVisibleExtent(layer); 
		    if(b) bounds = MapUtils.extendBounds(bounds,b);
		});
	    }
	    if(bounds) {
		this.zoomToExtent(bounds, false);
	    } else {
		this.getMap().zoomToMaxExtent();
	    }
        }

	if(this.params.initialZoom>=0) {
	    let zoom = this.params.initialZoom;
	    if(debugBounds)
		console.log("initial zoom:" + zoom);
	    this.params.initialZoom=-1;
	    this.zoomTo(zoom);
	    //In case we are in tabs then set the zoom level later
	    if(this.initialZoomTimeout) {
		setTimeout(()=>{
		    if(debugBounds)console.log("initial zoom time out:" + zoom);
		    this.zoomTo(zoom);
		},this.initialZoomTimeout??500);
	    }
	}
    },


    removeSearchMarkers:  function() {
        if (!this.searchMarkerList) return;
        for (let i = 0; i < this.searchMarkerList.length; i++) {
            this.removeMarker(this.searchMarkerList[i]);
        }
    },


    addAllLocationResults:  function() {
        this.removeSearchMarkers();
        this.searchMarkerList = [];
        if (!this.locationSearchResults) return;
        let east, west, north, south;
        for (let i = 0; i < this.locationSearchResults.length; i++) {
            let result = this.locationSearchResults[i];
            let lonlat = MapUtils.createLonLat(result.longitude, result.latitude);
            let icon = result.icon;
            if (!icon)
                icon = ramaddaCdn + "/icons/green-dot.png";
            this.searchMarkerList.push(this.addMarker("search", lonlat, icon, "", result.name, 20, 20));
            east = i == 0 ? result.longitude : Math.max(east, result.longitude);
            west = i == 0 ? result.longitude : Math.min(west, result.longitude);
            north = i == 0 ? result.latitude : Math.max(north, result.latitude);
            south = i == 0 ? result.latitude : Math.min(south, result.latitude);
        }
        let bounds = this.transformLLBounds(MapUtils.createBounds(west, south, east, north));
        this.zoomToExtent(bounds);
    },

    initLocationSearch:  function() {
        if (this.selectRegion) return;
        let _this = this;
        let input = HtmlUtils.span(["style", "padding-right:4px;", "id", this.mapDivId + "_loc_search_wait"], "") +
            HtmlUtils.checkbox(this.mapDivId + "_loc_bounds", ["title", "Search in map bounds"], false) + HtmlUtils.span(["title", "Search in map bounds"], " In view ") +
            HtmlUtils.input("", "", ["class", "ramadda-map-loc-input", "title", "^string - matches beginning", "size", "30", "placeholder", "Search location", "id", this.mapDivId + "_loc_search"])
        $("#" + this.mapDivId + "_footer2").html(input);
        let searchInput = $("#" + this.mapDivId + "_loc_search");
        let bounds = $("#" + this.mapDivId + "_loc_bounds");
        let searchPopup = $("#" + this.mapDivId + "_loc_popup");
        let wait = $("#" + this.mapDivId + "_loc_search_wait");
        searchInput.blur(function(e) {
            setTimeout(function() {
                wait.html("");
                searchPopup.hide();
            }, 250);
        });
        searchInput.on('input', function(e) {
            searchPopup.hide();
            if (searchInput.val() == "") {
                _this.removeSearchMarkers();
            }
        });

        searchInput.keypress(function(e) {
            let keyCode = e.keyCode || e.which;
            if (keyCode == 27) {
                searchPopup.hide();
                return;
            }
            if (keyCode != 13) {
                return;
            }
            wait.html(HtmlUtils.image(icon_wait));
            let url = ramaddaBaseUrl + "/geocode?query=" + encodeURIComponent(searchInput.val());
            if (bounds.is(':checked')) {
                let b = _this.transformProjBounds(_this.getMap().getExtent());
                url += "&bounds=" + b.top + "," + b.left + "," + b.bottom + "," + b.right;
            }
            let jqxhr = $.getJSON(url, function(data) {
                wait.html("");
                let result = HtmlUtils.openTag("div", ["style", "max-height:400px;overflow-y:auto;"]);
                if (data.result.length == 0) {
                    wait.html("Nothing found");
                    return;
                } else {
                    _this.locationSearchResults = data.result;
                    for (let i = 0; i < data.result.length; i++) {
                        let n = data.result[i].name.replace("\"", "'");
                        let icon = data.result[i].icon;
                        if (!icon)
                            icon = ramaddaCdn + "/icons/green-dot.png";
                        result += HtmlUtils.div(["class", "ramadda-map-loc", "name", n, "icon", icon, "latitude", data.result[i].latitude, "longitude", data.result[i].longitude], "<img width='16' src=" + icon + "> " + data.result[i].name);
                    }
                    result += HtmlUtils.div(["class", "ramadda-map-loc", "name", "all"], "Show all");
                }
                let my = "left bottom";
                let at = "left top";
                result += HtmlUtils.closeTag("div");
                searchPopup.html(result);
                HtmlUtils.setPopupObject(searchPopup);
                searchPopup.show();
                searchPopup.position({
                    of: searchInput,
                    my: my,
                    at: at,
                    collision: "fit fit"
                });
                searchPopup.find(".ramadda-map-loc").click(function() {
                    searchPopup.hide();
                    let name = $(this).attr("name");
                    if (name == "all") {
                        _this.addAllLocationResults();
                        return;
                    }
                    let lat = parseFloat($(this).attr("latitude"));
                    let lon = parseFloat($(this).attr("longitude"));
                    let icon = $(this).attr("icon");
                    let offset = 0.05;
                    let bounds = _this.transformLLBounds(MapUtils.createBounds(lon - offset, lat - offset, lon + offset, lat + offset));
                    let lonlat = MapUtils.createLonLat(lon, lat);
                    _this.removeSearchMarkers();
                    _this.searchMarkerList = [];
                    _this.searchMarkerList.push(_this.addMarker("search", lonlat, icon, "", name, 20, 20));
                    //Only zoom  if its a zoom in
                    let b = _this.transformProjBounds(_this.getMap().getExtent());
                    if (Math.abs(b.top - b.bottom) > offset) {
                        _this.zoomToExtent(bounds);
                    } else {
                        _this.setCenter(MapUtils.createLonLat(lon, lat));
                    }
                });
            }).fail(function(jqxhr, textStatus, error) {
                wait.html("Error:" + error);
            });
        });
    },

    addVectorLayer:  function(layer, canSelect) {
        this.addLayer(layer);
        this.vectorLayers.push(layer);
        if (this.getCanSelect(canSelect)) {
            let _this = this;
            if (!this.getMap().featureSelect) {
                this.getMap().featureSelect = new OpenLayers.Control.SelectFeature([layer], {
                    multiple: false,
                    hover: this.selectOnHover,
                    onSelect: function(feature) {
                        _this.showMarkerPopup(feature, true);
                    }
                });
            } else {
		this.getMap().featureSelect.layers.push(layer);
            }
        }
        this.checkLayerOrder();
    },

    isLayerVisible:  function(id, parentId) {
	if(!id) return true;
        //        let cbx =   $(':input[id*=\"' + "visible_" + this.mapId +"_" + id+'\"]');
        let cbx = this.getVisibilityCheckbox(id);
        if (cbx.length == 0 && parentId != null) cbx = $('#' + "visible_" + this.mapId + "_" + parentId);
        if (cbx.length == 0) return true;
        return cbx.is(':checked');
    },

    initForDrawing:  function() {
	let _this = this;
        if (!_this.drawingLayer) {
            this.drawingLayer = MapUtils.createLayerVector("Drawing");
            this.addLayer(_this.drawingLayer);
        }
        this.drawControl = new OpenLayers.Control.DrawFeature(
            _this.drawingLayer, OpenLayers.Handler.Point);
        this.getMap().addControl(_this.drawControl);
    },

    drawingFeatureAdded:  function(feature) {
        // alert(feature);
    },

    addClickHandler:  function(lonfld, latfld, zoomfld, object,onAlt) {
	this.handleClickSelection = true;
        this.lonFldId = lonfld;
        this.latFldId = latfld;
        if (this.clickHandler) {
            return;
	}
	this.clickListener = object;
        if (!this.map) {
            return;
	}

        this.clickHandler = new OpenLayers.Control.Click();
        this.clickHandler.setLatLonZoomFld(latfld, lonfld, zoomfld, object,onAlt);
        this.clickHandler.setTheMap(this);
        this.getMap().addControl(this.clickHandler);
        this.clickHandler.activate();
	return this.clickHandler;
    },

    setSelection:  function(argBase, doRegion, absolute,polygonId) {
	if(polygonId) {
	    this.polygonSelectorId =polygonId.replace(/\./g,'_');
	}
        this.selectRegion = doRegion;
        this.argBase = argBase??'';
        if (!GuiUtils) {
            return;
        }
	let getField=suffix=>{
            return  GuiUtils.getDomObject(this.argBase + '_'+suffix) ??
		GuiUtils.getDomObject(this.argBase + '.' + suffix) ??
		GuiUtils.getDomObject(this.argBase + suffix) ??
		GuiUtils.getDomObject(this.mapId + '_' + suffix);
	}
        this.fldNorth = getField('north');
        this.fldSouth = getField('south');
        this.fldEast = getField('east');
        this.fldWest = getField('west');
        this.fldLat = getField('latitude');
        this.fldLon = getField('longitude');
        if (this.fldLon || this.params.addMarkerOnClick) {
            this.addClickHandler(this?.fldLat?.id, this?.fldLon?.id);
	    if(this.fldLon)
		this.setSelectionMarker(this.fldLon.obj.value, this.fldLat.obj.value);
        } else if(this.fldWest && this.fldNorth) {
            this.addClickHandler(this?.fldNorth?.id, this?.fldWest?.id,null,null,true);
	}
    },

    selectionPopupInit:  function() {
	this.selectionDialog = window.ramaddaGlobalDialog;
        if (!this.inited) {
            this.initMap(this.selectRegion);
            if (this.argBase && !this.fldNorth) {
                this.setSelection(this.argBase);
            }
            if (this.fldNorth) {
                this.setSelectionBox(this.fldNorth.obj.value,
				     this.fldWest.obj.value, this.fldSouth.obj.value,
				     this.fldEast.obj.value, true);
            }


            if (this.fldLon) {
                this.addClickHandler(this.fldLon.id, this.fldLat.id);
                this.setSelectionMarker(this.fldLon.obj.value, this.fldLat.obj.value);
	    }
        }
	//The updateSize messes with map position
	let center = this.map.getCenter();
//	let zoom =  this.map.getZoom();
	this.getMap().updateSize();
        this.getMap().setCenter(center);
//	this.setZoom(zoom);
    },

    setSelectionBoxFromFields:  function(zoom) {
        if (this.fldNorth) {
            // alert("north = " + this.fldNorth.obj.value);
            this.setSelectionBox(this.fldNorth.obj.value,
				 this.fldWest.obj.value,
				 this.fldSouth.obj.value,
				 this.fldEast.obj.value, true);
            if (this.selectorBox) {
                let boxBounds = this.selectorBox.bounds
                this.getMap().setCenter(boxBounds.getCenterLonLat());
                if (zoom) {
                    this.zoomToExtent(boxBounds);
                }
            }
        }
    },

    toggleSelectorBox:  function(toggle) {
        if (this.selectorControl) {
            if (toggle) {
                this.selectorControl.activate();
                this.selectorControl.box.activate();
            } else {
                this.selectorControl.deactivate();
                this.selectorControl.box.deactivate();
            }
        }
    },

    resetExtent:  function() {
	if(debugBounds)
	    console.log("resetExtent");
        this.getMap().zoomToMaxExtent();
    },

    // Assume that north, south, east, and west are in degrees or
    // some variant thereof
    setSelectionBox:  function(north, west, south, east, centerView) {
        if (north == "" || west == "" || south == "" || east == "")
            return;
        let bounds = MapUtils.createBounds(west, Math.max(south,
							  -MapUtils.defaults.maxLatValue), east, Math.min(north, MapUtils.defaults.maxLatValue));
        if (!this.selectorBox) {
            let args = {
                "color": "red",
                "selectable": false
            };
            this.selectorBox = this.createBox("", "Selector box", north, west, south, east, "", args);
        } else {
            this.selectorBox.bounds = this.transformLLBounds(bounds);
            // this.selectorBox.bounds = bounds;
        }
        if (centerView) {
	    if(debugBounds)
		console.log("calling setViewToBounds-1");
            this.setViewToBounds(bounds)
        }

        if (this.boxes) {
            this.boxes.redraw();
        }

        if (this.selectImage) {
            let imageBounds = MapUtils.createBounds(west, south, east, north);
            imageBounds = this.transformLLBounds(imageBounds);
            this.selectImage.extent = imageBounds;
            this.selectImage.redraw();
        }

    },

    clearSelectionMarker:  function() {
        if (this.selectorMarker != null) {
            this.removeMarker(this.selectorMarker);
            this.selectorMarker = null;
        }
    },

    clearSelectedFeatures:  function() {
        if (this.getMap().controls != null) {
            let myControls = this.getMap().controls;
            for (i = 0; i < myControls.length; i++) {
                if (myControls[i].displayClass == "olControlSelectFeature") {
                    myControls[i].unselectAll();
                }
            }
        }
    },

    setSelectionMarker:  function(lon, lat, andCenter, zoom) {
        if (!lon || !lat || lon == "" || lat == "")
            return;
        if (this.lonFldId != null) {
            $("#" + this.lonFldId).val(MapUtils.formatLocationValue(lon));
            $("#" + this.latFldId).val(MapUtils.formatLocationValue(lat));
        }


        let lonlat = MapUtils.createLonLat(lon, lat);
        if (this.selectorMarker == null) {
            this.selectorMarker = this.addMarker(MapUtils.POSITIONMARKERID, lonlat, "", "", "", 20, 10);
        } else {
            this.selectorMarker.lonlat = this.transformLLPoint(lonlat);
        }
        if (this.markers) {
            this.markers.redraw();
        }
        if (andCenter) {
	    if(debugBounds)
		console.log("setSelectionMarker-1");
            this.getMap().setCenter(this.selectorMarker.lonlat);
        }
        if (zoom) {
	    if(debugBounds)
		console.log("setSelectionMarker-2");

            if (zoom.zoomOut) {
                let level = this.getMap().getZoom();
                level--;
                if (this.getMap().isValidZoomLevel(level)) {
                    this.zoomTo(level);
                }
                return;
            }
            if (zoom.zoomIn) {
                let level = this.getMap().getZoom();
                level++;
                if (this.getMap().isValidZoomLevel(level)) {
                    this.zoomTo(level);
                }
                return;
            }

            let offset = zoom.offset;
            if (offset) {
                let bounds = this.transformLLBounds(MapUtils.createBounds(lon - offset, lat - offset, lon + offset, lat + offset));
                this.zoomToExtent(bounds);
            }
        }
    },

    transformLLBounds:  function(bounds) {
        if (!bounds)
            return;
	if(Utils.isDefined(bounds.north))
            bounds= MapUtils.createBounds(bounds.west,bounds.south, bounds.east,bounds.north);
        let llbounds = bounds.clone();
        return llbounds.transform(this.displayProjection, this.sourceProjection);
    },

    ensureLonLat:function(points) {
	if(points.length>0 && !points[0].transform) {
	    points = points.map(point=>{
                point = MapUtils.createPoint(point.x,point.y);
		return point;
	    });
	}
	return points;
    },
    transformPoints: function(points) {
	points = this.ensureLonLat(points);
        points.forEach(point=>{
            point.transform(this.displayProjection, this.sourceProjection);
        });
	return points;
    },
    transformLLPoint:  function(point) {
        if (!point)
            return null;
        let llpoint = point.clone();
        return llpoint.transform(this.displayProjection, this.sourceProjection);
    },

    transformProjBounds:  function(bounds) {
        if (!bounds)
            return;
        let projbounds = bounds.clone();
        return projbounds.transform(this.sourceProjection, this.displayProjection);
    },

    transformProjPoint:  function(point) {
        if (!point)
            return;
        let projpoint = point.clone();
        return projpoint.transform(this.sourceProjection, this.displayProjection);
    },

    normalizeBounds:  function(bounds) {
        if (!this.map) {
            return bounds;
        }
        let newBounds = bounds;
        let newLeft = bounds.left;
        let newRight = bounds.right;
        let extentBounds = this.getMap().restrictedExtent;
        if (!extentBounds) {
            extentBounds = this.maxExtent;
        }
        if (!extentBounds) {
            return bounds;
        }
        /*
         * if (extentBounds.left < 0) { // map is -180 to 180 if (bounds.right >
         * 180) { //bounds is 0 to 360 newLeft = bounds.left-360; newRight =
         * bounds.right-360; } } else { // map is 0 to 360
         */
        if (extentBounds.left >= 0) { // map is 0 to 360+
            if (bounds.left < 0) { // left edge is -180 to 180
                newLeft = bounds.left + 360;
            }
            if (bounds.right < 0) { // right edge is -180 to 180
                newRight = bounds.right + 360;
            }
        }
        // just account for crossing the dateline
        if (newLeft > newRight) {
            newRight = bounds.right + 360;
        }
        newLeft = Math.max(newLeft, extentBounds.left);
        newRight = Math.min(newRight, extentBounds.right);
        newBounds = MapUtils.createBounds(newLeft, bounds.bottom, newRight,
					  bounds.top);
        return newBounds;
    },

    findSelectionFields:  function() {
        if (this.argBase && !(this.fldNorth || this.fldLon)) {
            this.setSelection(this.argBase);
        }
    },

    removePolygonSelectionLines:  function() {
	if(this.selectionLines) {
	    this.selectionLines.forEach(line=>{
		this.removePolygon(line);
	    });
	    this.selectionLines = [];
	}
	if(this.polygonSelectorId)
	    jqid(this.polygonSelectorId).val('');
    },
    addPolygonSelectionPoint:function(lonlat) {
	if(!this.polygonSelectionPoints) this.polygonSelectionPoints=[];
	this.polygonSelectionPoints.push(lonlat);
	this.applyPolygonSelectionPoints();
    },
    applyPolygonSelectionPoints:function() {
	this.removePolygonSelectionLines();
	if(!this.selectionLines) this.selectionLines = [];
	for(let i=0;i<this.polygonSelectionPoints.length-1;i++) {
	    let p1 = this.polygonSelectionPoints[i];
	    let p2 = this.polygonSelectionPoints[i+1];	    
	    let line = this.addLine('s','', p1.lat, p1.lon,
				    p2.lat, p2.lon,{strokeColor:'red',strokeWidth:2});
	    this.selectionLines.push(line);
	}
	let text='';
	this.polygonSelectionPoints.forEach(p=>{
	    if(text!='') text+=',';
	    text+=Utils.trimDecimals(p.lat,5)+','+Utils.trimDecimals(p.lon,5);
	});
	jqid(this.polygonSelectorId).val(text);
    },

    setFieldValues:function(lat,lon) {
        if (this.fldNorth) {
            this.fldNorth.obj.value = lat;
            this.fldSouth.obj.value = lat;
            this.fldWest.obj.value = lon;
            this.fldEast.obj.value = lon;
        } else if (this.fldLat) {
            this.fldLon.obj.value =lon;
            this.fldLat.obj.value = lat;
        }
        let lonlat = MapUtils.createLonLat(lon,lat);
	this.addSelectionMarker(lonlat);
    },


    setLocale:  function() {
	let lat='',lon='';
	if(!window.navigator || !navigator.geolocation) return;
	let options= {
	    enableHighAccuracy: true, 
	    maximumAge        : 30000, 
	    timeout           : 27000
	};

	navigator.geolocation.getCurrentPosition(position=> {
	    let lat = position.coords.latitude;
	    let lon = position.coords.longitude;
	    this.setFieldValues(lat,lon);
	    if(this.selectionDialog) this.selectionDialog.hide()
	},error=>{
		console.error(error);
	},options);	
    },
    selectionClear:  function() {
	this.removePolygonSelectionLines();
	this.polygonSelectionPoints=[];
        this.findSelectionFields();
	HU.addToDocumentUrl("map_bounds","");
        if (this.fldNorth) {
            this.fldNorth.obj.value = "";
            this.fldSouth.obj.value = "";
            this.fldWest.obj.value = "";
            this.fldEast.obj.value = "";
        } else if (this.fldLat) {
            this.fldLon.obj.value = "";
            this.fldLat.obj.value = "";
        }
        if (this.selectorBox && this.boxes) {
            this.boxes.removeMarker(this.selectorBox);
            this.selectorBox = null;
        }
        if (this.selectorMarker && this.markers) {
            this.removeMarker(this.selectorMarker);
            this.selectorMarker = null;
        }
    },

    clearMarkers:  function() {
        if (!this.markers) return;
        let markers = this.getMarkers();
        this.markers.removeFeatures(markers);
        this.markers.redraw();
    },
    getMarkers:  function() {
        if (this.markers == null) return [];
        return this.markers.features;
    },
    clearRegionSelector:  function(listener) {
        if (!this.selectorControl)
            return;
        if (this.selectorControl.box) {
            //Nothing here really works to hide the box
            this.selectorControl.box.removeBox();
        }
    },
    addRegionSelectorControl:  function(listener, forZoom) {
        let _this = this;
        if (_this.selectorControl)
            return;
        _this.selectorListener = listener;
        _this.selectorControl = new OpenLayers.Control();
        OpenLayers.Util.extend(_this.selectorControl, {
            draw: function() {
                this.box = new OpenLayers.Handler.Box(_this.selectorControl, {
                    "done": this.notice
                }, {
                    keyMask: OpenLayers.Handler.MOD_SHIFT,
                });
		if(!forZoom)
                    this.box.activate();
            },

            notice: function(bounds) {
		if(!Utils.isDefined(bounds.left)) return;
                let ll = _this.getMap().getLonLatFromPixel(MapUtils.createPixel(
                    bounds.left, bounds.bottom));
                let ur = _this.getMap().getLonLatFromPixel(MapUtils.createPixel(
                    bounds.right, bounds.top));
                ll = _this.transformProjPoint(ll);
                ur = _this.transformProjPoint(ur);
                bounds = MapUtils.createBounds(ll.lon, ll.lat, ur.lon,
					       ur.lat);
                bounds = _this.normalizeBounds(bounds);
                _this.setSelectionBox(bounds.top, bounds.left, bounds.bottom, bounds.right, false);
                _this.findSelectionFields();
                if (listener) {
                    listener(bounds);
                }
                if (_this.fldNorth) {
                    _this.fldNorth.obj.value = MapUtils.formatLocationValue(bounds.top);
                    _this.fldSouth.obj.value = MapUtils.formatLocationValue(bounds.bottom);
                    _this.fldWest.obj.value = MapUtils.formatLocationValue(bounds.left);
                    _this.fldEast.obj.value = MapUtils.formatLocationValue(bounds.right);
		    HU.addToDocumentUrl("map_bounds",bounds.top+"," + bounds.left+"," + bounds.bottom +","+bounds.right);
                }
            }
        });
        _this.map.addControl(_this.selectorControl);
        _this.panControl = new OpenLayers.Control();
        OpenLayers.Util.extend(_this.panControl, {
            draw: function() {
                this.box = new OpenLayers.Handler.Drag(_this.panControl, {
                    "down": this.down,
                    "move": this.move
                }, {
                    keyMask: OpenLayers.Handler.MOD_META,
                    boxDivClassName: "map-drag-box"
                });
                this.box.activate();
            },

            down: function(pt) {
                this.firstPoint = _this.getMap().getLonLatFromPixel(MapUtils.createPixel(
                    pt.x, pt.y));
                this.firstPoint = _this.transformProjPoint(this.firstPoint);
                _this.findSelectionFields();
                if (_this.fldNorth) {
                    n = this.origNorth = parseFloat(_this.fldNorth.obj.value);
                    s = this.origSouth = parseFloat(_this.fldSouth.obj.value);
                    w = this.origWest = parseFloat(_this.fldWest.obj.value);
                    e = this.origEast = parseFloat(_this.fldEast.obj.value);
                    pt = this.firstPoint;
                    this.doWest = false;
                    this.doSouth = false;
                    this.doEast = false;
                    this.doNorth = false;
                    if (pt.lon <= e && pt.lon >= w && pt.lat <= n && pt.lat >= s) {
                        this.doSouth = this.doWest = this.doEast = this.doNorth = true;
                        this.type = "center";
                    } else if (pt.lon > e) {
                        if (pt.lat > n) {
                            this.type = "ne";
                            this.doEast = this.doNorth = true;
                        } else if (pt.lat < s) {
                            this.type = "se";
                            this.doSouth = this.doEast = true;
                        } else {
                            this.type = "e";
                            this.doEast = true;
                        }
                    } else if (pt.lon < w) {
                        if (pt.lat > n) {
                            this.type = "nw";
                            this.doWest = this.doNorth = true;
                        } else if (pt.lat < s) {
                            this.type = "sw";
                            this.doSouth = this.doWest = true;
                        } else {
                            this.type = "w";
                            this.doWest = true;
                        }
                    } else if (pt.lat > n) {
                        this.type = "n";
                        this.doNorth = true;
                    } else {
                        this.type = "s";
                        this.doSouth = true;
                    }
                }
            },
            move: function(pt) {
                let ll = _this.getMap().getLonLatFromPixel(MapUtils.createPixel(pt.x, pt.y));
                ll = _this.transformProjPoint(ll);
                dx = ll.lon - this.firstPoint.lon;
                dy = ll.lat - this.firstPoint.lat;
                newWest = this.origWest;
                newEast = this.origEast;
                newSouth = this.origSouth;
                newNorth = this.origNorth;


                if (this.doWest)
                    newWest += dx;
                if (this.doSouth)
                    newSouth += dy;
                if (this.doEast)
                    newEast += dx;
                if (this.doNorth)
                    newNorth += dy;
                let bounds = MapUtils.createBounds(newWest, newSouth, newEast, newNorth);
                bounds = _this.normalizeBounds(bounds);
                _this.setSelectionBox(bounds.top, bounds.left, bounds.bottom, bounds.right, false);
                _this.findSelectionFields();
                if (!_this.fldNorth) return;
                _this.fldNorth.obj.value = MapUtils.formatLocationValue(bounds.top);
                _this.fldSouth.obj.value = MapUtils.formatLocationValue(bounds.bottom);
                _this.fldWest.obj.value = MapUtils.formatLocationValue(bounds.left);
                _this.fldEast.obj.value = MapUtils.formatLocationValue(bounds.right);

            }
        });
        _this.map.addControl(_this.panControl);
    },


    closePopup:  function(evt) {
        if (this.currentPopup) {
	    this.getMap().removePopup(this.currentPopup);
	    try {
		this.currentPopup.destroy();
	    } catch(err) {
		console.error('Error destroying map popup:' + err);
	    }
            this.currentPopup = null;
            this.hiliteBox('');
            if (this.selectedFeature) {
                this.unselectFeature(this.selectedFeature);
            }
        }
    },


    onPopupClose:  function(evt) {
	if(this.params.displayDiv) {
	    this.showText("");
	}
        if (this.currentPopup) {
            this.getMap().removePopup(this.currentPopup);
	    try {
		this.currentPopup.destroy();
	    } catch(exc) {}
            this.currentPopup = null;
            this.hiliteBox('');
            if (this.selectedFeature) {
                this.unselectFeature(this.selectedFeature);
            }
        }
    },

    findObject:  function(id, array) {
        for (i = 0; i < array.length; i++) {
            let aid = array[i].ramaddaId;
            if (!aid)
                aid =array[i].id;
            if (aid == id) {
                return array[i];
            }
        }
        return null;
    },

    findMarker:  function(id) {
        if (!this.markers) {
            return null;
        }
        return this.findObject(id, this.getMarkers());
    },

    findFeature:  function(id) {
	let feature= this.features[id];
	if(!feature && this.pointsMap) {
	    feature = this.pointsMap[id];
	}
	return feature;
    },

    findBox:  function(id) {
        if (!this.boxes) {
            return null;
        }
        return this.findObject(id, this.boxes.markers);
    },

    hiliteBox:  function(id) {
        if (this.currentBox) {
            this.currentBox.setBorder(this.currentBox.defaultBorderColor||"blue",1);
        }
        this.currentBox = this.findBox(id);
        if (this.currentBox) {
            this.currentBox.setBorder("red");
        }
    },
    checkMarkerVisibility:  function() {
        if (!this.markers) return;
        let list = this.getMarkers();
        for (let idx = 0; idx < list.length; idx++) {
            marker = list[idx];
            let visible = this.isLayerVisible(marker.ramaddaId, marker.parentId);
            //            console.log("   visible:" + visible +" " + marker.ramaddaId + " " + marker.parentId);
            if (visible) {
                marker.style.display = 'inline';
            } else {
                marker.style.display = 'none';
            }
            //            marker.display(visible);
        }
        this.markers.redraw();
    },

    checkLinesVisibility:  function() {
        if (!this.lines) return;
        let features = this.lines.features;
        for (let i = 0; i < features.length; i++) {
            let line = features[i];
            let visible = this.isLayerVisible(line.ramaddaId);
            if (visible) {
                line.style.display = 'inline';
            } else {
                line.style.display = 'none';
            }

        }
        this.lines.redraw();

    },


    checkBoxesVisibility:  function() {
        if (!this.boxes) return;
        for (let marker in this.boxes.markers) {
            marker = this.boxes.markers[marker];
            let visible = this.isLayerVisible(marker.ramaddaId);
            marker.display(visible);
        }
        this.boxes.redraw();
    },



    checkImageLayerVisibility:  function() {
        if (!this.imageLayersList) return;
	this.imageLayersList.forEach(image=>{
            let visible = this.isLayerVisible(image.id);
            image.setVisibility(visible);
            if (!visible) {
                if (image.box) {
                    this.removeBox(image.box);
                    image.box = null;
                }
            } else {
                if (image.box) {
                    this.removeBox(image.box);
                    image.box = this.createBox(i, "", image.north, image.west, image.south, image.east, image.text, {});
                }
            }
        });
    },

    hiliteMarker:  function(id,event) {
        let mymarker = this.findMarker(id);
        if (!mymarker) {
            mymarker = this.findFeature(id);
        }


        if (!mymarker && this.imageLayers) {
            mymarker = this.imageLayers[id];
	    //Handle the id mismatch
	    if(!mymarker)
		mymarker=this.imageLayers[id.replace(/_/g,'-')];
        }


	//Check for polygon
	if(!mymarker && this.polygonMap) {
	    mymarker = this.polygonMap[id];
	    //If it is a polygon then treat it as a click and return
	    if(mymarker) {
		this.handleFeatureclick(mymarker.layer,mymarker,false,{});
		return;
	    }
	}


	
        if (!mymarker) {
	    console.log('cannot find marker with id:' +id);
            return;
        }
        let latLonBounds = mymarker.latLonBounds;
        if (latLonBounds) {
            let projBounds = this.transformLLBounds(latLonBounds);
            this.zoomToExtent(projBounds);
        } else {
	    if(debugBounds) console.log("hiliteMarker");
            this.setCenter(mymarker.lonlat);
        }

        this.showMarkerPopup(mymarker);
	if(event && event.shiftKey) {
	    this.setZoom(12);
	}


    },


    clearAllProgress: function() {
	this.clearProgress();
	this.hideLoadingImage();	
    },
    clearProgress: function(msg) {
	$("#" + this.mapDivId+"_progress").hide();
    },
    setProgress: function(msg) {
	$("#" + this.mapDivId+"_progress").html(msg);
	$("#" + this.mapDivId+"_progress").show();
    },
    setLabel: function(msg) {
	$("#" + this.mapDivId+"_label").html(msg);
    },	
    setCursor:  function(cursor) {
	jqid(this.mapDivId+"_themap").css('cursor',cursor??'default');
    },
    loadingImageCnt:0,
    hideLoadingImage:  function(checkCnt) {
	if(checkCnt) {
	    this.loadingImageCnt = Math.max(0,this.loadingImageCnt-1);
	    if(this.loadingImageCnt>0) return;
	}
	this.setCursor();
	jqid(this.mapDivId+"_themap").css('cursor','default');
        if (this.loadingImage) {
            this.loadingImage.style.visibility = "hidden";
        }
    },


    showLoadingImage:  function(checkCnt) {
	if(checkCnt) {
	    this.loadingImageCnt++;
	    if(this.loadingImageCnt>1) return;
	}
	this.setCursor('progress');
        if (this.loadingImage) {
            this.loadingImage.style.visibility = "visible";
            return;
        }
        let sz = MapUtils.createSize(120,120);
        let width = this.getMap().viewPortDiv.offsetWidth;
        let height = this.getMap().viewPortDiv.offsetHeight;
        let position = MapUtils.createPixel(width / 2 - sz.w / 2, height / 2 - sz.h / 2);
        this.loadingImage = MapUtils.createImage("loadingimage",
						 position,
						 sz,
						 ramaddaCdn + '/icons/mapprogress.gif');
        this.loadingImage.style.zIndex = 1010;
        this.getMap().viewPortDiv.appendChild(this.loadingImage);
    },


    centerOnMarkerLayer:  function() {
        this.centerOnMarkers(null, false, true);
    },

    centerOnMarkersInit:  function() {
	if(!this.hadDefaultBounds) {
            this.centerOnMarkers(null);
	}
    },

    getLayerVisibleExtent: function(layer) {
        let maxExtent = null;
        let features = layer.features;
	if(!features || features.length==0) {
	    maxExtent =  layer.getDataExtent();
	    if(maxExtent) return maxExtent;
	    return layer.maxExtent;
	}
        let geometry = null;
	let visible = 0;
	let notVisible=0;
        for(let i=0, len=features.length; i<len; i++) {
	    let feature = features[i];
	    if(!feature.getVisibility()) {
		notVisible++;
		continue;
	    }
	    visible++;
            geometry = feature.geometry;
            if (geometry) {
//		console.dir(feature.fid,this.transformProjBounds(geometry.getBounds()));
                if (maxExtent === null) {
                    maxExtent = MapUtils.createBounds();
                }
                maxExtent.extend(geometry.getBounds());
            }
        }
//	console.log('not visible:'+ notVisible +' visible:' + visible,   this.transformProjBounds(maxExtent));
        return maxExtent;
    },


    animateViewToBounds:  function(bounds, ob,steps) {
	if(!Utils.isDefined(steps)) steps = 1;
	if(!ob)
	    ob = this.transformProjBounds(this.getMap().getExtent());
	let numSteps = 10;
	if(steps>numSteps) {
	    this.setViewToBounds(bounds);
	    return;
	}
	let p = steps/numSteps; 
	steps++;
	newBounds = MapUtils.createBounds(
	    ob.left+(bounds.left- ob.left)*p,
	    ob.bottom+(bounds.bottom- ob.bottom)*p,
	    ob.right+(bounds.right- ob.right)*p,
	    ob.top+(bounds.top-ob.top)*p);
	this.setViewToBounds(newBounds);
	setTimeout(()=>this.animateViewToBounds(bounds, ob, steps),125);
    },


    getPopupText:  function(text, marker) {
        if (text == null) return null;
        if (text.indexOf("base64:") == 0) {
            text = window.atob(text.substring(7)).trim();
//	    console.log(text);
            if (text.indexOf("{") == 0) {
                props = JSON.parse(text);
                text = props.text;
                if (!text) text = "";
                if (marker) {
                    marker.inputProps = props;
                }
            }
        }
        return text;
    },

    clearSeenMarkers:  function() {
	this.seenMarkers = {};
    },

    addEntryMarker:  function(id, location, iconUrl, markerName, text, type, props) {
        marker = this.addMarker(id, location, iconUrl, markerName, text);
        marker.entryType = type;
        marker.entryId = id;
	if(props && props.fillColor) {
	    let pointStyle = {
		pointRadius:props.radius||12,
		fillColor:props.fillColor,
		strokeWidth:props.strokeWidth,
		strokeColor:props.strokeColor
	    };
	    this.addPoint(id, location, pointStyle,"");
	}
    },


    createMarker:  function(id, location, iconUrl, markerName, text, parentId, size, xoffset, yoffset, canSelect,attrs) {
	if(iconUrl=="dot") {
	    return this.createPoint(id, location,{}, text);
	}

	if(Utils.isDefined(location.x)) {
	    location = MapUtils.createLonLat(location.x,location.y);
	}


	attrs  = attrs?? {};
        if (Array.isArray(location)) {
            location = MapUtils.createLonLat(location[0], location[1]);
        }
	//TODO: sometime clean up the size, etc
	let width = size || this.params.iconWidth || this.params.iconSize || 20;
	let height = size || this.params.iconHeight || this.params.iconSize || 20;	
	size=width;
        if (xoffset == null) xoffset = 0;
        if (yoffset == null) yoffset = 0;
        if (!this.markers) {
            this.markers = MapUtils.createLayerVector("Markers");
            this.addVectorLayer(this.markers, canSelect);
        }
        if (!Utils.stringDefined(iconUrl)) {
            iconUrl = ramaddaCdn + '/icons/marker.png';
        }

        let sz = MapUtils.createSize(width,height);
        let calculateOffset = function(width) {
            let offset = MapUtils.createPixel(-(size.w / 2)-xoffset, -(size.h / 2) - yoffset);
	    return offset;
        };

        let icon = MapUtils.createIcon(iconUrl, sz, null, calculateOffset);
        let projPoint = this.transformLLPoint(location);
        let marker = MapUtils.createMarker(projPoint, icon);
	let pt = MapUtils.createPoint(location.lon, location.lat).transform(this.displayProjection, this.sourceProjection);
	let style = {
            externalGraphic: iconUrl,
            graphicWidth: size,
            graphicXOffset: xoffset + (-size / 2),
            graphicYOffset: -size / 2,
	    labelYOffset:attrs.labelYOffset??-size,
	    label:attrs.label
        };
	if(attrs.fontSize) style.fontSize = attrs.fontSize;
	if(Utils.isDefined(attrs.rotation)) {
	    style.rotation = attrs.rotation;
	}
        let feature = MapUtils.createVector(pt, {description: ''}, style);
        feature.ramaddaId = id;
        feature.parentId = parentId;
        feature.text = this.getPopupText(text, feature);
        feature.name = markerName;
        feature.location = location;

        marker.ramaddaId = id;
        marker.text = this.getPopupText(text, marker);
        marker.name = markerName;
        marker.location = location;
        let locationKey = location + "";
        feature.locationKey = locationKey;
        let seenMarkers = this.seenMarkers[locationKey];
        if (seenMarkers == null) {
            seenMarkers = [];
            this.seenMarkers[locationKey] = seenMarkers;
        } else {}
        seenMarkers.push(feature);
        //        console.log("loc:" + locationKey +" " + seenMarkers);


        let _this = this;
        let clickFunc = function(evt) {
            _this.showMarkerPopup(marker, true);
            if (marker.ramaddaClickHandler != null) {
                marker.ramaddaClickHandler.call(null, marker);
            }
            MapUtils.stopEvent(evt);
        };

        marker.events.register('click', marker, clickFunc);
        marker.events.register('touchstart', marker, clickFunc);


        let visible = this.isLayerVisible(marker.ramaddaId, marker.parentId);
        if (!visible) marker.display(false);
        feature.what = "marker";
        return feature;
    },

    addMarkerEmbed:function(marker) {
	let props = {};
	if(marker.startsWith("base64:")) {
	    marker = window.atob(marker.substring(7));
	}
	if (marker.indexOf("{") == 0) {
	    props = JSON.parse(marker);
	} else {
	    let [lat,lon,text] = marker.split(",");
	    props.lat = lat;props.lon = lon; props.text = text;
	}
	let point = MapUtils.createLonLat(parseFloat(props.lon), parseFloat(props.lat));
	if(props.size==null || props.size=="") props.size=16;
	let type = props.type || "icon"
	let attrs = props;
	attrs.pointRadius=props.size||"16";
	attrs.labelYOffset = -8-attrs.pointRadius;
	/*
	  if(!props.description) props.description = "";
	  if(!Utils.isAnonymous()) {
	  props.description +="<br>" + "edit";
	  }
	*/

	if(type == "icon") {
	    let icon = props.icon || "/markers/marker-red.png";
	    //addMarker:  function(id, location, iconUrl, markerName, text, parentId, size, yoffset, canSelect) {
	    this.addMarker("", point,icon,props.description,props.description,"",parseFloat(props.size||"16"),null,true,attrs);
	} else {
	    //addPoint:  function(id, point, attrs, text, notReally, textGetter)
	    attrs.fillColor=attrs.fillColor||"blue";
	    attrs.strokeWidth=attrs.strokeWidth||"1";		    
	    attrs.graphicName=type;
	    if(type=="none") {
		attrs.graphicName = "circle";
		attrs.pointRadius=0;
		attrs.fillColor="transparent";
	    }
	    this.addPoint("", point, attrs, props.description);
	    if(attrs.graphicName!="circle") 
		this.addPoint("", point, {pointRadius:attrs.pointRadius, strokeColor:"transparent", fillColor:"transparent"},props.description);
	}
	this.setDoPopup(true);
    },

    setDoPopup:function(doPopup) {
	this.params.doPopup=  doPopup;
    },

    getDoPopup:function(doPopup) {
	return this.params.doPopup;
    },

    createPolygonFromString:function(id,s,polygonProps,latlon,text,justPoints) {
//	s = "35.6895;139.6917;37.7749;-122.4194";
	let delimiter;
	[";",","].forEach(d=>{
	    if(s.indexOf(d)>=0) delimiter = d;
	});

	polygonProps = polygonProps??{
	    fill: true,
	    fillColor: "#0000ff",
	    fillOpacity: 0.05,
	    strokeWidth:1,
	    strokeColor:"blue"};

	let toks  = s.split(delimiter);
	let points = [];
	let p = [];
	let segments = [p];
	

	let handlePoints = (lat1,lon1,lat2,lon2) =>{
	    if(!latlon) {
		let tmp =lat1;lat1=lon1;lon1=tmp;tmp =lat2;lat2=lon2;lon2=tmp;
	    }
	    if(isNaN(lat1) || isNaN(lat2) || isNaN(lon1) || isNaN(lon2)) {
		return;
	    }

	    if(justPoints) {
		points.push(lon1,lat1,lon2,lat2);
		return;
	    } 
	    if(MapUtils.crossIDL(lon1,lon2)) {
		let inter = MapUtils.intercept(lat1,lon1,lat2,lon2);
		p.push(MapUtils.createPoint(lon1,lat1));
		p.push(MapUtils.createPoint(lon1<0?-180:180,inter));		
		p=[];
		segments.push(p);
		p.push(MapUtils.createPoint(lon2<0?-180:180,inter));		
		p.push(MapUtils.createPoint(lon2,lat2));
	    } else {
		p.push(MapUtils.createPoint(lon1,lat1));
		p.push(MapUtils.createPoint(lon2,lat2));
	    }
	}

	//check for spaces
	if(toks.length>0 && toks[0].trim().indexOf(' ')>0) {
	    latlon = false;
	    let tmp = [];
	    toks.forEach(tok=>{
		let subtoks = Utils.split(tok.trim(),' ',true,true);
		if(subtoks.length==2) {
		    tmp.push(parseFloat(subtoks[0]),parseFloat(subtoks[1]));
		}
	    });
	    for(let pIdx=2;pIdx<tmp.length;pIdx+=2) {
		let lat1 = tmp[pIdx-2];
		let lon1 = tmp[pIdx-1];
		let lat2 = tmp[pIdx];
		let lon2 = tmp[pIdx+1];
		handlePoints(lat1,lon1,lat2,lon2);
	    }
	} else {
	    for(let pIdx=2;pIdx<toks.length;pIdx+=2) {
		let lat1 = parseFloat(toks[pIdx-2]);
		let lon1 = parseFloat(toks[pIdx-1]);
		let lat2 = parseFloat(toks[pIdx]);
		let lon2 = parseFloat(toks[pIdx+1]);
		handlePoints(lat1,lon1,lat2,lon2);
	    }
	}
	if(justPoints) return points;
	let polys = [];
	segments.forEach(p=>{
	    if(p.length>0)
		polys.push(this.createPolygon(id, "",p,polygonProps,text,false));
	});
	return polys;
    },

    addPolygonString:function(id,s,polygonProps,latlon,text) {
	let polys = this.createPolygonFromString(id, s,polygonProps,latlon,text);
        this.getLinesLayer().addFeatures(polys);
	return polys;
    },

    addMarkers:  function(markers) {
        if (!this.markers) {
            this.markers = MapUtils.createLayerVector("Markers");
            this.addVectorLayer(this.markers, true);
        }
        this.markers.addFeatures(markers);
    },

    initBoxes:  function(theBoxes) {
        if (!this.getMap()) {
            // alert('whoa, no map');
        }
        this.addLayer(theBoxes);
        // Added this because I was getting an unknown method error
        theBoxes.getFeatureFromEvent = function(evt) {
            return null;
        };
    },

    removeBox:  function(box) {
        if (this.boxes && box) {
            this.boxes.removeMarker(box);
            this.boxes.redraw();
        }
    },

    addBox:  function(box) {
        if (!this.boxes) {
            this.boxes = MapUtils.createLayerBoxes("Boxes", {
                wrapDateLine: MapUtils.defaults.wrapDateline,
            });
            if (!this.getMap()) {
                this.initialBoxes = this.boxes;
            } else {
                this.initBoxes(this.boxes);
            }
        }
        this.boxes.addMarker(box);
        this.boxes.redraw();
    },


    createBox:  function(id, name, north, west, south, east, text, params) {
        if (text.indexOf("base64:") == 0) {
            text = window.atob(text.substring(7));
        }

        let args = {
            "color": "blue",
            "selectable": true,
            "zoomToExtent": false,
            "sticky": false
        };
        for (let i in params) {
            args[i] = params[i];
        }
	if(!args.color) args.color = this.params.boxColor || "blue";
        let bounds = MapUtils.createBounds(west, Math.max(south, -MapUtils.defaults.maxLatValue),
					   east, Math.min(north, MapUtils.defaults.maxLatValue));
        let projBounds = this.transformLLBounds(bounds);
        let box = MapUtils.createBox(projBounds);
        box.sticky = args.sticky;
        let _this = this;

        if (args["selectable"]) {
            box.events.register("click", box, function(e) {
                _this.showMarkerPopup(box, true);
		MapUtils.stopEvent(e);
            });
        }

        let lonlat = MapUtils.createLonLat(west, north);
        box.lonlat = this.transformLLPoint(lonlat);
        box.text = this.getPopupText(text);
        box.name = name;
        box.setBorder(args.color??'blue', 1);
	box.defaultBorderColor = args.color;
        box.ramaddaId = id;
        if (args.zoomToExtent) {
            this.centerOnMarkers(bounds);
        }
        this.addBox(box);
        return box;
    },


    circleMarker:  function(id, attrs) {
        let marker = this.findMarker(id);
        if (!marker) {
            marker = this.findFeature(id);
        }
        if (!marker) {
            return null;
        }
	let   myattrs = {
            pointRadius: 16,
            stroke: true,
            strokeColor: "red",
            strokeWidth: 2,
            fill: false,
        };

        if (attrs)
            $.extend(myattrs, attrs);
        this.showFeatureText(marker);
        return this.addPoint(id + "_circle", marker.location, myattrs);
    },



    addFeatureHighlightHandler:  function( callback) {
	this.featureHighlightHandler = callback;
    },

    addFeatureSelectHandler:  function( callback) {
	this.featureSelectHandler= callback;
    },

    showFeatureText:  function(feature) {
	if(this.featureHighlightHandler) {
	    this.featureHighlightHandler(feature,true);
	}
        let _this = this;
        if (feature.text && this.params.displayDiv) {
            this.textFeature = feature;
            let callback = function() {
                if (_this.textFeature == feature) {
                    _this.showText(_this.textFeature.text);
                }
            }
            setTimeout(callback, 1000);
        }
    },

    showText:  function(text,force) {
        if (!force && this.searchMsg && Utils.stringDefined(this.searchMsg.html())) {
	    return;
	}

	if(this.dontShowText) return;
	if(text.startsWith('url:')) {
	    let url = text.substring('url:'.length);
            $.get(url,
		  (data) =>{
		      this.showText(data);
		  })
		.done(function() {})
		.fail(function() {
                    console.log("Failed to load marker text url: " + url);
		});
	    return;
	}

        $("#" + this.params.displayDiv).html(text);
    },

    hideFeatureText:  function(feature) {
	if(this.featureHighlightHandler)
	    this.featureHighlightHandler(feature,false);
        if (!feature || this.textFeature == feature) {
	    if(!this.params.displayDivSticky) 
                this.showText("");
        }
    },
    
    
    createPoint:  function(id, point, attrs, text,  textGetter,skipDefaultStyle) {
        //Check if we have a LonLat instead of a Point
        let location = point;
        if (typeof point.x === 'undefined') {
            point = MapUtils.createPoint(point.lon, point.lat);
        } else {
            location = MapUtils.createLonLat(point.x, point.y);
        }

	if(!this.basePointStyle) {
	    this.basePointStyle = $.extend({}, MapUtils.getVectorStyle('default'));
            $.extend(this.basePointStyle, {
		pointRadius: 5,
		stroke: true,
		strokeColor: "red",
		strokeWidth: 0,
		strokeOpacity: 0.75,
		fill: true,
		fillColor: "blue",
		fillOpacity: 0.75,
            });
	}

	let cstyle= Object.assign({},this.basePointStyle);
        if (attrs) {
	    cstyle = Object.assign(cstyle, attrs);
            if (cstyle.pointRadius <= 0) cstyle.pointRadius = 1;
        }
        if (cstyle.fillColor == "" || cstyle.fillColor == "none") {
            cstyle.fillOpacity = 0.0;
        }
        if (cstyle.strokeColor == "" || cstyle.strokeColor == "none") {
            cstyle.strokeOpacity = 0.0;
        }


	//["star", "cross", "x", "square", "triangle", "circle", "lightning", "rectangle", "church"];
        let center = MapUtils.createPoint(point.x, point.y);
        center.transform(this.displayProjection, this.sourceProjection);


        let feature = MapUtils.createVector(center, null, cstyle);
        feature.center = center;
        feature.ramaddaId = id;
	if(text)
            feature.text = this.getPopupText(text, feature);
        feature.textGetter = textGetter;
        feature.location = location;
	feature.lonlat = location;
        this.features[id] = feature;
        return feature;
    },

    addPoint:  function(id, point, attrs, text, textGetter) {
	let feature = this.createPoint(id,point,attrs,text,textGetter);
	this.getMarkersLayer().addFeatures([feature],{silent:true});
	if(!this.pointsMap) this.pointsMap={};
	this.pointsMap[id]=feature;
        return feature;
    },

    addPoints:function(points) {
	this.getMarkersLayer().addFeatures(points,{silent:true});
    },
    getMarkersLayer:  function() {
        if (this.circles == null) {
            this.circles = MapUtils.createLayerVector("Shapes");
	    this.circles.layerName = "circles";
            this.circles.setZIndex(1);
            this.addVectorLayer(this.circles);
	    this.circles.canSelect = true;
        }
	return this.circles;
    },

    setPointsVisibility: function(visible) {
	if(this.circles) 
	    this.circles.setVisibility(visible);
    },


    addRectangle:  function(id, north, west, south, east, attrs, info) {
        let points = [MapUtils.createPoint(west, north),
		      MapUtils.createPoint(west, south),
		      MapUtils.createPoint(east, south),
		      MapUtils.createPoint(east, north),
		      MapUtils.createPoint(west, north)
		     ];
        return this.addPolygon(id, "", points, attrs, info);
    },


    createLine:  function(id, name, lat1, lon1, lat2, lon2, attrs, info) {
        let points = [MapUtils.createPoint(lon1, lat1),
		      MapUtils.createPoint(lon2, lat2)
		     ];
        return this.createPolygon(id, name, points, attrs, info);
    },


    addLine:  function(id, name, lat1, lon1, lat2, lon2, attrs, info,justCreate) {
        let points = [MapUtils.createPoint(lon1, lat1),
		      MapUtils.createPoint(lon2, lat2)
		     ];
        return this.addPolygon(id, name, points, attrs, info,justCreate);
    },

    
    addSelectionMarker:function(lonlat,dontPropagate) {
	if(this.selectorMarker)
	    this.removeMarker(this.selectorMarker);
	this.selectorMarker = this.addMarker(MapUtils.POSITIONMARKERID, lonlat, this.params.markerIcon??"", "", "", 40, 20);
	if(this.getProperty('addMarkerOnClick')) {
	    if(!dontPropagate)
		ramaddaMapShareState(this,{marker:lonlat});
	}
    },


    addMarker:  function(id, location, iconUrl, markerName, text, parentId, size, yoffset, canSelect, attrs,polygon, justCreate) {
        let marker = this.createMarker(id, location, iconUrl, markerName, text, parentId, size, 0, yoffset, canSelect,attrs);
	marker.lonlat = location;
	if(!justCreate) {
	    this.addMarkers([marker]);
	}
	if(polygon) {
	    this.addPolygonString(id, polygon,null,true,text);
	}
        return marker;
    },


    addLines:  function(id, name, attrs, values, info,andZoom) {
	attrs = attrs || {};
	if (!attrs.strokeWidth)
	    attrs.strokeWidth = this.params.strokeWidth;

	if (!attrs.strokeColor)
	    attrs.strokeColor = this.params.strokeColor;
	

        let points = [];
        for (let i = 0; i < values.length; i += 2) {
            points.push(MapUtils.createPoint(values[i + 1], values[i]));
        }
        let lines = this.addPolygon(id, name, points, attrs, info,false,true);
	if(andZoom && this.getLinesLayer()) {
            let dataBounds = this.getLinesLayer().getDataExtent();
            if (dataBounds) {
                this.zoomToExtent(dataBounds, true);
            }
	}
	return lines;
    },

    removePolygon:  function(line) {
        if (this.lines) {
            //            this.lines.removeAllFeatures();
            this.lines.removeFeatures([line]);
        }
    },


    createPolygon:  function(id, name, points, attrs, marker,makeLineString) {
        let _this = this;
        let location;
        if (points.length > 1) {
            location = MapUtils.createLonLat(points[0].x + (points[1].x - points[0].x) / 2,
					     points[0].y + (points[1].y - points[0].y) / 2);
        } else if(points.length>0) {
            location = MapUtils.createLonLat(points[0].x, points[0].y);
        }

	points = this.transformPoints(points);
        let base_style = $.extend({}, MapUtils.getVectorStyle('default'));
        let style = $.extend({}, base_style);
        if (attrs) {
            for (let key in attrs) {
                style[key] = attrs[key];
            }
        }
	//for now create a polygon not a linestring
	let geom;
	if(makeLineString) {
	    geom = MapUtils.createLineString(points);
	} else {
	    geom = MapUtils.createPolygon(MapUtils.createLinearRing(points));
	}


        let line = MapUtils.createVector(geom,null,style);
	if(!marker) marker = name;
        line.text = marker;
        line.ramaddaId = id;
        line.location = location;
        line.name = name;
	if(!this.polygonMap)this.polygonMap={};
	this.polygonMap[id] = line;
        let visible = this.isLayerVisible(line.ramaddaId);
        if (visible) {
            line.style.display = 'inline';
	} else {
            line.style.display = 'none';
        }
        return line;
    },
    addPolygon:  function(id, name, points, attrs, marker,justCreate,makeLineString) {
	let polygon  =this.createPolygon(id,name,points,attrs,marker,makeLineString);
	if(!justCreate) {
            this.getLinesLayer().addFeatures([polygon]);
	}
        return polygon;
    },

    getLinesLayer: function() {
        if (!this.lines) {
            let base_style = $.extend({}, MapUtils.getVectorStyle('default'));
            this.lines = MapUtils.createLayerVector("Lines", {
                style: base_style
            });
            this.addVectorLayer(this.lines);
	    this.lines.isMapLayer = true;
        }	
	return this.lines;
    },
    //Get the bounding box of the features
    getFeaturesBounds: function(features,convertToLatLon) {
	if(!features) return null;
	let bounds = MapUtils.createBounds();
	features.forEach((feature,idx)=>{
	    if(!feature.geometry) return;
	    if(MapUtils.isFeatureVisible(feature)) {
		bounds = MapUtils.extendBounds(bounds, feature.geometry.getBounds());
	    }
	});
	if(convertToLatLon) {
	    bounds = this.transformProjBounds(bounds);
	}
	return bounds;
    },
    centerOnFeatures: function(features) {
	let bounds = this.getFeaturesBounds(features);
	if(bounds.left == bounds.right || bounds.top == bounds.bottom) {
	    bounds = this.transformProjBounds(bounds);
	    let center = bounds.getCenterLonLat();
	    this.setCenter(center);
	    return;
	}
	this.zoomToExtent(bounds);
    },
    getHighlightLinesLayer: function() {
        if (!this.highlightlines) {
            let base_style = $.extend({},  MapUtils.getVectorStyle('default'));
            this.highlightlines = MapUtils.createLayerVector("Highlight Lines", {
                style: base_style
            });
            this.addVectorLayer(this.highlightlines);
        }	
	return this.highlightlines;
    },

    handleMarkerLayer:  function() {
        if (this.currentPopup) {
            this.getMap().removePopup(this.currentPopup);
            this.currentPopup.destroy();
            this.currentPopup = null;
        }
        let marker = this.currentEntryMarker;
        if (marker.entryLayer) {
            let idx = this.loadedLayers.indexOf(marker.entryLayer);
            if (idx >= 0)
                this.loadedLayers = this.loadedLayers.slice(idx);
            this.getMap().removeLayer(marker.entryLayer);
            marker.entryLayer = null;
            return;
        }
        let layer = this.handleEntrySelect(marker.entryId, marker.name, marker.entryType);
        if (layer) {
            marker.entryLayer = layer;
        }
    },

    showPopupSlider:function(location, markerText) {
	if(this.params.doPopupSlider || this.params.popupSliderRight) {
	    //Set location then do the popup later to allow map to repaint
	    this.doingPopup = true;
	    //For some reason if we do this here then the browser hangs
	    //	    if(location)this.setCenter(location);
	    setTimeout(()=> {
		let slider = $("#" +this.mapDivId+"_slider");
		if(this.params.popupSliderRight) {
		    slider.css("right","0px");
		} else {
		    slider.css("left","0px");
		}
		slider.hide();
		let width = HU.getDimension(this.params.popupWidth);
		let contents = HU.div([STYLE,HU.css('width',width,"padding","5px")], HU.div([ID,this.mapDivId+"_sliderclose",CLASS,"ramadda-clickable"], HU.getIconImage(icon_close)) + markerText);
		slider.html(contents);
		slider.slideDown(800);
		$("#" +this.mapDivId+"_sliderclose").click(()=>{
		    slider.slideUp();
		});
		//Wait a bit so the above setCenter can happen
		setTimeout(()=>{
		    this.doingPopup = false;
		},10);

	    },500);
	    return true;
	}
	return false;
    },


    showMarkerPopup:  function(marker, fromClick, simplePopup) {
	if(debugPopup) console.log("showMarkerPopup");

        if (this.entryClickHandler && window[this.entryClickHandler]) {
            if (!window[this.entryClickHandler](this, marker)) {
		if(debugPopup) console.log("\twindowClickHandler");
                return;
            }
        }

        if (this.currentPopup) {
            this.getMap().removePopup(this.currentPopup);
	    try {
		this.currentPopup.destroy();
	    } catch(err){}
        }


	//For now do this but we had the check for the clickListener here for some reason
	//Sometime I need to clean up all of the click listening that goes no
//	if(!Utils.isDefined(this.clickListener)) {
	    //	    console.log("showMarkerPopup:" + this.clickListener);
	    if(this.featureSelectHandler && this.featureSelectHandler(marker)) {
		if(debugPopup) console.log("\tfeatureSelectHandler returned true");
		return;
	    }
//	}

        let id = marker.ramaddaId;
        if (!id)
            id = marker.id;
	if(this.shareSelected &&  window["ramaddaDisplaySetSelectedEntry"]) {
	    ramaddaDisplaySetSelectedEntry(id);
	}


	if(!this.getDoPopup()) {
	    if(debugPopup) console.log("\tparams.doPopup=false");
	    return;
	}

	let inputProps = marker.inputProps ?? {};

        this.hiliteBox(id);
        let _this = this;
        if (marker.inputProps) {
            marker.text = this.getPopupText(marker.inputProps.text);
        }
        let markerText;

	if(marker.textGetter) {
	    markerText =marker.textGetter(marker);
	    if(debugPopup) console.log("\thas textGetter:" + markerText);
	} else if(this.textGetter) {
	    markerText = this.textGetter(marker.layer, marker);
	}
	if(!markerText) {
	    markerText =marker.text;
	}

	if(!markerText) {
	    if(debugPopup) console.log("\tno marker text");
	    return;
	}
	if(markerText.startsWith('url:')) {
	    let url = markerText.substring('url:'.length);
            $.get(url,
		  (data) =>{
		      this.showMarkerPopupInner(marker,fromClick, simplePopup,data,inputProps);
		  })
		.done(function() {})
		.fail(function() {
                    console.log("Failed to load marker text url: " + url);
		});
	} else {
	    this.showMarkerPopupInner(marker,fromClick, simplePopup,markerText,inputProps);
	}

    },

    showMarkerPopupInner:  function(marker, fromClick, simplePopup,markerText,inputProps) {

	let html = markerText;
	if(this.params.displayDiv) {
	    this.showText(markerText);
	    if(debugPopup) console.log("\thad displayDiv");
	    return;
	}

        if (fromClick && marker.locationKey != null) {
            markers = this.seenMarkers[marker.locationKey];
            if (markers && markers.length > 1) {
	    if(debugPopup)
		console.log('showMarkerPopup: seenMarkers:', markers.length);
                markerText = "";
                for (let i = 0; i < markers.length; i++) {
                    otherMarker = markers[i];
                    if (i > 0)
                        markerText += '<hr>';
                    if (otherMarker.inputProps) {
			otherMarker.text = otherMarker.textGetter?otherMarker.textGetter(marker):this.getPopupText(otherMarker.inputProps.text);
                    }
		    let text = otherMarker.text?otherMarker.text:otherMarker.textGetter?otherMarker.textGetter(marker):"NA";
                    markerText += text;
                    if (i > 10) break;
                }
            }
        }

	
        // set marker text as the location
        let location = marker.location;

        let projPoint = null;
	if(!location) {
	    if(marker.geometry) {
		let b = marker.geometry.getBounds();
		if(b) {
		    projPoint = b.getCenterLonLat();
		}
	    }
	} else {
            if (typeof location.lon === 'undefined') {
		location = MapUtils.createLonLat(location.x, location.y);
            }
            if (!markerText || markerText == "") {
		//            marker.location = this.transformProjPoint(marker.lonlat);
		if (location.lat == location.lat) {
                    markerText = "Lon: " + location.lat + "<br>" + "Lat: " + location.lon;
		}
            }
	    
            if (marker.entryType) {
		let type = marker.entryType;
		if (type == "geo_kml" || type == "geo_json" || type == "geo_shapefile") {
                    this.currentEntryMarker = marker;
                    let call = "ramaddaMapMap['" + this.mapId + "'].handleMarkerLayer();";
                    let label = marker.entryLayer ? "Remove Layer" : "Load Layer";
                    markerText = "<center>" + HtmlUtils.onClick(call, label) + "</center>" + markerText;
		}
            }
	    projPoint = this.transformLLPoint(location);
	}	

	if(projPoint==null) {
	    if(debugPopup) console.log("\tno projPoint");
	    return;
	}


	if(this.showPopupSlider(marker.location, markerText)) {
	    return
	}

	let props = {};
        if (inputProps.chartType) {
	    props.width=400;
	}


	let uid = HU.getUniqueId("div");
	let div = HU.div(['style','width:100%;','id',uid]);
	props.width = props.width??inputProps.minSizeX??this.params.popupWidth;
	props.height = props.height??inputProps.minSizeY??this.params.popupHeight;	
        let popup = this.makePopup( projPoint,div,props);
	if(debugPopup) console.log('showMarkerPopup:', markerText);
        marker.popupText = popup;
        popup.marker = marker;
        this.addPopup(popup);
	jqid(uid).html(markerText);
	if(this.popupListener) {
	    this.popupListener(uid,markerText);
	}

	if(debugPopup) console.log("\tmade popup");

        if (inputProps.chartType) {
            this.popupChart(marker.inputProps);
        }

	if(this.popupHandler) {
	    this.popupHandler(marker,popup);
	}
    },


    popupChart:  function(props) {
        let displayManager = getOrCreateDisplayManager(props.divId, {}, true);
        let pointDataProps = {
            entryId: props.entryId
        };
        let title = props.title;
        if (!title) title = "Chart";
        let fields = (props.fields == null ? null : props.fields.split(","));
        let chartProps = {
            "title": title,
            "layoutHere": true,
            "divid": props.divId,
            "entryId": props.entryId,
            "fields": fields,
            "vAxisMinValue": props.vAxisMinValue,
            "vAxisMaxValue": props.vAxisMaxValue,
            "data": new PointData(title, null, null, getRamadda().getRoot() + "/entry/show?entryid=" + props.entryId + "&output=points.product&product=points.json&numpoints=10000", pointDataProps)
        };
	if(props.chartArgs) {
	    let toks = props.chartArgs.split(",");
	    for(let i=0;i<toks.length;i+=2) {
		chartProps[toks[i]] = toks[i+1];
	    }
	}
        displayManager.createDisplay(props.chartType, chartProps);
    },

    uncircleMarker:  function(id) {
        feature = this.features[id + "_circle"];
        if (feature && this.circles) {
            this.circles.removeFeatures([feature]);
        }
        this.hideFeatureText(feature);
    },

    removePoint:  function(point) {
	this.removePoints([point]);
    },
    removePointsLayer:  function() {
        if (this.circles) {
	    this.removeLayer(this.circles);
	    this.circles = null;
	}
    },
    removeMarkersLayer:  function() {
        if (this.markers) {
	    this.removeLayer(this.markers);
	    this.markers= null;
	}
    },    
    removePoints:  function(points) {
        if (this.circles) {
	    this.circles.removeFeatures(points);
	}
    },
    removeMarker:  function(marker) {
	this.removeMarkers([marker]);
    },
    removeMarkers:  function(markers) {
        if (this.markers) {
            this.markers.removeFeatures(markers);
        }
	//remove the marker from the seenMarkers map
	markers.forEach(marker=>{
	    if(!marker) return;
	    if(!marker.locationKey) return;
            let seenMarkers = this.seenMarkers[marker.locationKey];
            if (!seenMarkers) return;
            this.seenMarkers[marker.locationKey] = Utils.removeElement(this.seenMarkers[marker.locationKey], marker);
	});
    },    
    createFeatureLayer: function(name, canSelect,style,opts) {
        let base_style = $.extend({},MapUtils.getVectorStyle('default'));
	if(style)
	    $.extend(base_style,style);
	opts = opts||{};
	opts.style = base_style;
        let layer =  MapUtils.createLayerVector(name||"Markers", opts);
	layer.canSelect=canSelect;
	this.externalLayers.push(layer);
        this.addVectorLayer(layer,canSelect);
	return layer;
    },

}




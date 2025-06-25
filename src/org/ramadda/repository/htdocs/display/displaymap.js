/**
   Copyright (c) 2008-2025 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/


const DISPLAY_MAP = "map";

let displayMapMarkers = ["marker.png", "marker-blue.png", "marker-gold.png", "marker-green.png"];
let displayMapCurrentMarker = -1;
let displayMapUrlToVectorListeners = {};
let displayMapMarkerIcons = {};

var ID_REGION_SELECTOR = "regionselector";


var debugit = false;
var debugMapTime = false;

addGlobalDisplayType({
    type: DISPLAY_MAP,
    label: "Map",
    category:CATEGORY_MAPS,
    preview: "map1.png",
    desc:"Lots of ways to show georeferenced data - dots, heatmaps, plots, etc",        
});




function MapFeature(source, points) {
    RamaddaUtil.defineMembers(this, {
        source: source,
        points: points
    });
}

var ID_MAP = "map";
var ID_MAP_CONTAINER = "mapcontainer";
var xcnt = 1;
var ycnt = 1;

function RamaddaBaseMapDisplay(displayManager, id, type,  properties) {
    $.extend(this, {
        theMap: null
    });

    //Default to a tooltip
    //    if(!properties.tooltip) properties.tooltip='${default}';
    this.myycnt = ++ycnt;
    this.myName = "map " + (this.myycnt);
    const SUPER = new RamaddaDisplay(displayManager, id, type,   properties);
    RamaddaUtil.inherit(this,SUPER);
    this.defineSizeByProperties();
    let myProps = [
	{label:'Base Map Properties'},
	{p:'bounds',ex:'north,west,south,east',tt:'initial bounds'},
	{p:'maxBounds',ex:'n,w,s,e or conus',tt:'max bounds when applying filters'},
	{p:'gridBounds',ex:'north,west,south,east'},	
	{p:'mapCenter',ex:'lat,lon',tt:"initial position"},
	{p:'zoomLevel',ex:4,tt:"initial zoom"},
	{p:'centerOnConus',ex:true},
	{p:'centerOnNA',ex:true},
	{p:'initBoundsUseAllRecords',ex:true},
	{p:'initBoundsPadding',ex:'A percent, e.g.0.05'},
	{p:'zoomTimeout',ex:500,
	 tt:"initial zoom timeout delay. set this if the map is in tabs, etc, and not going to the initial zoom"},
	{p:'popupWidth',d:400},
	{p:'popupHeight',d:200},	
	{p:'linked',ex:true,tt:"Link location with other maps"},
	{p:'linkGroup',ex:'some_name',tt:"Map groups to link with"},
	{p:'initialLocation', ex:'lat,lon',tt:"initial location"},
	{p:'defaultMapLayer',ex:'osm|google.roads|esri.street|google.hybrid|google.roads|google.terrain|google.satellite|opentopo|esri.topo|usfs|usgs.topo|naip|usgs.imagery|esri.shaded|esri.lightgray|esri.darkgray|esri.terrain|shadedrelief|esri.aeronautical|historic|osm.toner|osm.toner.lite'},
	{p:'geojsonLayer',ex:'entry ID',tt:'Display the geojson layer file held by give entry'},
	{p:'geojsonLayerName',d:'Map'},
	{p:'justShowMapLayer',ex:true,tt:'If true then just show map layer, don\'t use it for data display'},
	{p:'extraLayer1',label:'XYZ Layer',
	 ex:'xyz:name:Some Name:url:https\\\\://server/tiles/${z}/${x}/${y}.png:baseLayer:false:visible:true'},
	{p:'extraLayer1',label:'WMS Layer',
	 ex:'wms:name:Some Name:url:https\://server/tiles/${z}/${x}/${y}.png:baseLayer:false:visible:true'},
	{p:'extraLayer1',label:'GeoJSON Layer',
	 ex:'geojson:name:Some Name:url:resources/usmap.json:fillColor:transparent'},
	{p:'extraLayer1',label:'KML Layer',
	 ex:'geojson:name:Some Name:url:resources/usmap.json:fillColor:transparent'},		

	{p:'linkFields',tt:'Comma separated list of fields in the data to match with the map field, e.g., geoid'},	
	{p:'linkFeature',ex:'geoid',tt:'The field in the map to match with the data field, e.g., geoid'},
	{p:'debugFeatureLinking',tt:'Debug feature linking',ex:true},
	{p:'pruneFeatures',ex:true,tt:'Hide any features in the map that don\'t have a corresponding record'},
	{p:'showHighlight',ex:'true',tt:'Show popup when point is moused over'},
	{p:'highlightTemplate',ex:'Some text ${field1}',tt:'Template for mousing over points'},

	{p:'highlightWidth',d:200,tt:'Width of highlight popup'},
	{p:'highlightHeight',ex:200,tt:'Height of highlight popup'},	

	{p:'polygonField',tt:'Field that contains a polygon'},		

	{p:'annotationLayerTop',ex:'true',tt:'If showing the extra annotation layer put it on top'},

	{p:'showBoundsFilter'},
	{p:'showBounds',ex:'true',d:false},
	{p:'boundsStrokeColor',d:'blue'},
	{p:'boundsFillColor',d:'transparent'},
	{p:'boundsFillOpacity',d:'0.0'},			

	{p:'showOpacitySlider',ex:'false'},
	{p:'showLocationSearch',ex:'true'},
	{p:'showLatLonPosition',ex:'false',d:true},
	{p:'singlePointZoom',ex:'12'},
	{p:'showOverviewMap',ex:true},
	{p:'overviewMapWidth',d:180},
	{p:'overviewMapHeight',d:90},
	{p:'overviewMapHeight',d:90},
	{p:'overviewMapResolution',d:1},					
	{p:'overviewMapMinRatio',d:24},
	{p:'overviewMapMaxRatio',d:64},
	
	{p:'overviewMapLayer',ex:'osm|google.roads|esri.street|google.hybrid|google.roads|google.terrain|google.satellite|opentopo|esri.topo|usfs|usgs.topo|naip|usgs.imagery|esri.shaded|esri.lightgray|esri.darkgray|esri.terrain|shadedrelief|esri.aeronautical|historic|osm.toner|osm.toner.lite'},
	{p:'showGraticules',ex:true},	
	{p:'showLayerSwitcher',d:true,ex:'false'},
	{p:'showScaleLine',ex:'true',d:false},
	{p:'showZoomPanControl',ex:'true',d:true},
	{p:'showZoomOnlyControl',ex:'false',d:false},
	{p:'enableDragPan',ex:'false',d:true},
	{p:'showLayers',d:true,ex:'false',tt:'Connect points with map vectors'},
	{p:'showBaseLayersSelect',ex:true,d:false},
	{p:'baseLayerSelectLabel',d:null},
	{p:'locations',ex:'countries.json,usstates.json,uscities.json,usairports.json'},
	{p:'highlightColor',d:'blue',ex:'#ccc',tt:''},
	{p:'highlightFillColor',ex:'#ccc',
	 tt:'Use "match" to match the features opacity'},		
	{p:'highlightFillOpacity',ex:'0.5',
	 tt:'Use "match" to match the features opacity'},		
	{p:'highlightStrokeWidth',ex:'2',
	 tt:'Use "match" to match the features opacity'},			 
	{p:"highlightStrokeColor",
	 tt:'Use "match" to match the features opacity'},

	{p:'selectFillColor',ex:'#ccc',
	 tt:'Use "match" to match the features opacity'},		
	{p:'selectFillOpacity',ex:'0.5',
	 tt:'Use "match" to match the features opacity'},		
	{p:'selectStrokeWidth',ex:'2',
	 tt:'Use "match" to match the features opacity'},			 
	{p:"selectStrokeColor",
	 tt:'Use "match" to match the features opacity'},
	{p:"selectStrokeOpacity",
	 tt:'Use "match" to match the features opacity'},			 		
	
        {p:"vectorLayerStrokeColor",d:'#000'},
	{p:"vectorLayerFillColor",d:'#ccc'},
	{p:"vectorLayerFillOpacity",d:0.25},
        {p:"vectorLayerStrokeWidth",d:0.3},
    ];

    this.debugZoom = properties['debugZoom'];
    
    displayDefineMembers(this, myProps, {
        mapBoundsSet: false,
	extraLayers:[],
        initDisplay: function() {
	    //	    if(displayDebug.initMap) console.log(this.getLogLabel()+".initDisplay");
            SUPER.initDisplay.call(this);
	    if(!HU.documentReady) {
		$( document ).ready(()=> {
		    if(this.map) {
			setTimeout(()=>{
			    this.callingUpdateSize = true;
			    this.map.getMap().updateSize();
			    this.callingUpdateSize = false;
			},50);
		    }
		});
	    }
            let _this = this;
            let html = "";
            let extraStyle="";
            let height = this.getProperty("height", this.getProperty("mapHeight", '70vh'));
            if (height < 0) {
		height = (-height)+"%";
	    }
	    height = HU.getDimension(height);
            extraStyle += HU.css(ATTR_HEIGHT, height);

	    let map =HU.div(["tabindex","1",ATTR_CLASS, "display-map-map ramadda-expandable-target", ATTR_STYLE,
			     extraStyle, ATTR_ID, this.domId(ID_MAP)]);

	    let mapContainer = HU.div([ATTR_CLASS,"ramadda-map-container",ATTR_ID,this.domId(ID_MAP_CONTAINER)],
				      map+
				      HU.div([ATTR_CLASS,"ramadda-map-slider",ATTR_STYLE,this.getProperty("popupSliderStyle", "max-height:400px;overflow-y:auto;xxxmax-width:300px;overflow-x:auto;"),ATTR_ID,this.domId(ID_MAP)+"_slider"]));

            this.setContents(mapContainer);
	    
            if (!this.map) {
                this.createMap();
            } else {
                this.map.setMapDiv(this.domId(ID_MAP));
            }
            if (!this.haveCalledUpdateUI) {
		//		if(displayDebug.initMap)  console.log(this.getLogLabel()+" have not calledUpdateUI. setting up callback");
                let callback = function() {
		    _this.updateUICallback = null;
                    _this.updateUI();
                }
                this.updateUICallback = setTimeout(callback, 1);
            }
        },
	initRegionsSelector:function(button) {
	    //Fetch the regions
	    if(!MapUtils.regions) {
		let jqxhr = $.getJSON(RamaddaUtil.getUrl("/regions.json"), data=> {
		    if (GuiUtils.isJsonError(data)) {
			console.log("Error fetching regions");
			MapUtils.regions=[];
			return;
		    }
		    MapUtils.regions=data;
		    this.initRegionsSelector(button);
		});
		return;
	    }		    
	    let _this = this;
	    if(_this.regionsDialog) _this.regionsDialog.remove();		
	    let id = _this.domId(ID_REGION_SELECTOR);
	    let html = _this.makeRegionsMenu();
	    html = HU.div([ATTR_CLASS, "ramadda-popup-inner",ATTR_ID,id,
			   ATTR_STYLE,HU.css('margin-top','0.5em','min-width','800px')],html);
	    _this.regionsDialog = HU.makeDialog({content:html,title:'Regions',
						 draggable:true,header:true,
						 my:'left top',at:'left bottom',anchor:button});
	    _this.regionsDialog.find(".ramadda-menu-item").click(function() {
		let region = MapUtils.regions[+$(this).attr("idx")];
		_this.map.setViewToBounds(new RamaddaBounds(region.north, region.west, region.south, region.east));
	    });
	    this.jq('regionsearch').focus();
	    let regionItems = 	  this.regionsDialog.find(".ramadda-region-item")
	    this.jq('regionsearch').keyup(function(event) {
		let text = $(this).val().trim().toLowerCase();
		regionItems.each(function() {
		    if(text=='') {
			$(this).show();
		    } else {
			let corpus = $(this).html();
			if(!corpus) return;
			corpus =  corpus.toLowerCase();
			if(corpus.indexOf(text)>=0) {
			    $(this).show();
			} else {
			    $(this).hide();
			}
		    }
		});

	    });

	},

	makeRegionsMenu:function() {
	    let groups = {};
	    MapUtils.regions.forEach((r,idx)=>{
		//skip world as its a dup
		if(r.name == "World") return
		let group = r.group;
		if(group.toLowerCase()=="model regions") group="Regions";
		let name = r.name.replace(/ /g,"&nbsp;");
		let item = HU.div([ATTR_CLASS,"ramadda-menu-item ramadda-region-item", "idx",idx],name);
		if(!groups[group]) groups[group] = "";
		groups[group] +=item;});
	    let html ='';
	    html+= HU.center(HU.input('','',['placeholder','Find Region',ATTR_ID,this.domId('regionsearch'),'width','10']));
	    html += "<table width=100%><tr valign=top>";
	    let keys = Object.keys(groups);
	    let width = (100/keys.length)+'%';
	    keys.forEach(group=>{
		html+= HU.td(['width',width], HU.div([ATTR_STYLE,HU.css("font-weight","bold","border-bottom","1px solid #ccc","margin-right","5px")], Utils.camelCase(group))+HU.div([ATTR_STYLE,HU.css("max-height","200px","overflow-y","auto", "margin-right","10px")], groups[group]));
	    });
	    html+=HU.close(TAG_TR,TAG_TABLE);
	    return html;
	},

	getTurfPoints:function(f) {
	    let p=[];
	    let coords = f.geometry.coordinates;
	    coords.forEach(pair=>{
		p.push(pair[1],pair[0]);
	    });
	    return p;
	},
	makeTurfBounds:function(bounds,padding) {
	    if(Utils.isDefined(padding)) {
		let w = bounds.east-bounds.west;
		let h = bounds.north - bounds.south;
		return [bounds.west-padding*w, bounds.south-padding*h, bounds.east+padding*w, bounds.north+padding*h];
	    }
	    return [bounds.west, bounds.south, bounds.east, bounds.north];
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

	createGeoJsonLayer:function(name,geojson,layer,style) {
	    if(layer) {
		layer.removeFeatures(layer.features);
	    } else {
		layer = MapUtils.createLayerVector(name,
						   {projection: this.getMap().getMap().displayProjection},
						   style);
		this.getMap().addLayer(layer);
		layer.ramaddaLayerIndex = 100;
		if(Utils.isDefined(this.layerVisible) && !this.layerVisible)
		    layer.setVisibility(false);
	    }
            let format= new OpenLayers.Format.GeoJSON({});
	    let features = format.read(geojson);
	    let strategy = new OpenLayers.Strategy.Fixed();
	    strategy.layer= layer;
	    strategy.merge({features:features});
	    this.extraLayers.push(layer);
	    return layer;
	},

	getBaseLayersSelect:function() {
	    if(this.map.baseLayers) {
		let items = [];
		let on = false;
		for(a in this.map.baseLayers) {
		    let layer = this.map.baseLayers[a];
		    if(!layer.isBaseLayer) continue;
		    if(layer.getVisibility()) on = a;
		    items.push([a,layer.name]);
		}
		let prefix = this.getBaseLayerSelectLabel();
		if(prefix) prefix=HU.b(prefix)+':'+SPACE;
		return  HU.span([ATTR_TITLE,"Choose base layer", ATTR_CLASS,"display-filter"],

				(prefix??'') +
				HU.select("",[ATTR_ID,this.domId("baselayers")],items,on));
	    }
	    return '';
	},
	initBaseLayersSelect:function() {
	    let _this = this;
	    this.jq("baselayers").change(function() {
		let on = $(this).val();
		for(let id in _this.map.baseLayers) {
		    if(id==on) {
			_this.map.getMap().setBaseLayer(_this.map.baseLayers[id]);
			break;
		    }
		}
	    });
	},
	checkLevelRange:function(layers, redraw) {
	    if(!layers) layers=[this.getMap().getMarkersLayer()];
	    if(this.debugZoom) console.log("features:");
	    let level = this.getMap().getMap().getZoom();
	    layers.forEach(layer=>{
		if(!layer) return;
		layer.features.forEach(feature=>{
		    if(!feature.levelRange) {
			if(this.debugZoom) console.log("\tno level range");
			return;
		    }
		    if(!feature.style) feature.style = {};
		    let visible = level>=feature.levelRange.min &&
			level<=feature.levelRange.max;
		    let changed= false;
		    //		console.log("\tlevel:",level,feature.levelRange,visible); 
		    if(visible) {
			changed = feature.style.display != 'inline';
			feature.style.display = 'inline';
		    }  else {
			changed = feature.style.display != 'none';
			feature.style.display = 'none';
		    }			
		    if(redraw &&changed) {
			layer.drawFeature(feature);
		    }
		});
	    });
	},

	getMap:function() {
	    return this.map;
	},
	setErrorMessage: function(msg) {
	    if(this.errorMessageHandler) {
		this.errorMessageHandler(this,msg);
		return
	    }
	    if(this.map)
		this.map.setProgress(HU.div([ATTR_CLASS, "display-map-message"], msg));
	    else
		SUPER.setErrorMessage.call(this,msg);
	},
	setMessage: function(msg) {
	    if(this.map) {
		if(msg!="")
		    msg = HU.div([ATTR_CLASS, "display-map-message"], msg);
		this.map.setProgress(msg);
	    }
	},
	setMapLabel: function(msg) {
	    if(this.map)
		this.map.setLabel(HU.div([ATTR_CLASS, "display-map-message"], msg));
	},	
	startProgress: function(msg) {
	    this.setMessage(msg??this.getProperty("loadingMessage","Creating map..."));
	},
	clearProgress: function() {
	    if(this.errorMessage) {
		this.errorMessage = null;
		return;
	    }
	    if(this.map) {
		this.map.hideLoadingImage();
		this.map.setProgress("");
	    }
	},

        checkLayout: function() {
            if (!this.map) {
                return;
            }
            let d = this.jq(ID_MAP);
            if (d.width() > 0 && this.lastWidth != d.width() && this.map) {
                this.lastWidth = d.width();
                this.map.getMap().updateSize();
            }
	    /* not sure why we have this here but it ends up screwing up the map zooming way out
	       if(!this.setMapLocationAndZoom && this.mapParams) {
	       this.setMapLocationAndZoom = true;
	       if(this.mapParams.initialZoom>=0) {
	       this.map.zoomTo(this.mapParams.initialZoom);
	       }
	       if(this.mapParams.initialLocation) {
	       let loc = MapUtils.createLonLat(this.mapParams.initialLocation.lon, this.mapParams.initialLocation.lat);
	       this.map.setCenter(loc);
	       }

	       }**/
        },

        initMapParams: function(params) {
	    params.maxBounds = this.getMaxBounds();
	    if(this.getProperty('canMove',false)) {
		params.canMove=true;
	    }
	    if(!this.getProperty('addMapLocationToUrl',true)) {
		params.addToUrl=false;
	    }
	    if(this.getSinglePointZoom()) {
		params.singlePointZoom = this.getSinglePointZoom();
	    }
	    if(this.getShowOpacitySlider()) {
		params.showOpacitySlider=true;
	    }
	    ['highlightStrokeColor','highlightFillColor',"highlightStrokeWidth","highlightFillOpacity","changeSizeOnSelect"].forEach(p=>{
		let v = this.getProperty(p);
		if(Utils.isDefined(v)) {
		    params[p] = v;
		}
	    });
	},
	initMap:function(map) {
	},
        doDisplayMap: function() {
	    return true;
	},
        createMap: function() {
            this.map = this.getProperty("externalMap", null);
	    if(this.map) {
		return this.map;
	    }

            let _this = this;
            let params = {
                defaultMapLayer: this.getDefaultMapLayer(map_default_layer),
		showLayerSwitcher: this.getShowLayerSwitcher(),
		showScaleLine: this.getShowScaleLine(),
		showLatLonPosition: this.getShowLatLonPosition(),
		showZoomPanControl: this.getShowZoomPanControl(),
		showZoomOnlyControl: this.getShowZoomOnlyControl(),
		enableDragPan: this.getEnableDragPan(),
		highlightColor: this.getHighlightColor(),
		highlightFillColor: this.getHighlightFillColor("match"),
		highlightFillOpacity: this.getHighlightFillOpacity(),				
		highlightStrokeWidth: this.getHighlightStrokeWidth(1),

		selectFillColor: this.getSelectFillColor("match"),
		selectFillOpacity: this.getSelectFillOpacity(),				
		selectStrokeWidth: this.getSelectStrokeWidth(),
		selectStrokeColor: this.getSelectStrokeColor(),		
		selectStrokeOpacity: this.getSelectStrokeOpacity(),				

		showLatLonLines:this.getProperty("showLatLonLines"),
		popupWidth: this.getPopupWidth(),
		popupHeight: this.getPopupHeight(),

            };
	    this.mapParams = params;
            let displayDiv = this.getProperty("displayDiv", null);
            if (displayDiv) {
                params.displayDiv = displayDiv;
		params.displayDivSticky = this.getProperty("displayDivSticky", false);
            }
            if (!this.getShowLocationSearch(true)) {
                params.showLocationSearch = false;
            }


	    params.addMarkerOnClick = this.getProperty('addMarkerOnClick');
	    params.linked = this.getLinked(false);
	    params.linkGroup = this.getLinkGroup(null);

	    this.hadInitialPosition = false;
            if (this.getProperty("latitude")) {
		this.hadInitialPosition = true;
                params.initialLocation = {lon:+this.getProperty("longitude", -105),
					  lat:+this.getProperty("latitude", 40)};
	    }
	    if(this.getCenterOnConus()) {
		if(!this.getZoomLevel()) 
		    this.setProperty('zoomLevel',3);
		this.setProperty('mapCenter','39.8333,-98.5855');
	    }
	    if(this.getCenterOnNA()) {
		if(!this.getZoomLevel()) 
		    this.setProperty('zoomLevel',3);
		this.setProperty('mapCenter','46.17983,-92.43896');
	    }	    

	    this.hadUrlArgumentMapCenter = Utils.stringDefined(HU.getUrlArgument(ARG_MAPCENTER));
	    this.hadUrlArgumentZoom = Utils.stringDefined(HU.getUrlArgument(ARG_ZOOMLEVEL));
	    if(!this.hadUrlArgumentMapCenter && this.getMapCenter()) {
		this.hadInitialPosition = true;
		[lat,lon] =  this.getMapCenter().replace("%2C",",").split(",");
                params.initialLocation = {lon:lon,lat:lat};
	    }

	    if(!this.hadUrlArgumentZoom && this.getZoomLevel()) {
		this.hadInitialPosition = true;
                params.initialZoom = +this.getZoomLevel();
		params.initialZoomTimeout = this.getZoomTimeout();
		if(debugBounds) console.log("DisplayMap - set initialZoom", params.initialZoom);
	    }

            this.map = this.getProperty("theMap", null);
            if (this.map) {
                this.map.setMapDiv(this.domId(ID_MAP));
            } else {
		if(this.getInitialLocation()) {
		    let toks = this.getInitialLocation().split(",");
		    params.initialLocation = {
			lat:+toks[0],
			lon:+toks[1]
		    }
		}
		this.initMapParams(params);
                this.map = new RepositoryMap(this.domId(ID_MAP), params);
		this.map.popupListener = (id,text) =>{
		    let fields = jqid(id).find('[field-id]');
		    fields.addClass('ramadda-clickable');
		    fields.click(function() {
			let field = $(this).attr('field-id');
			let value = $(this).attr('field-value');			
			//A hack because for some reason clicking on an href isn't doing anything
			if(value && value.toLowerCase().startsWith('http')) {
			    window.open(value,'_link');
			    return;
			}
			let args = {
			    id:field,
			    fieldId: field,
			    value: value
			};
			_this.propagateEvent(DisplayEvent.filterChanged, args,true);
		    });
		};
		if(this.getShowOverviewMap()) {
		    let opts = {size:{}};
		    let layer = this.getOverviewMapLayer();
		    if(layer) {
			layer = this.getMap().getMapLayer(layer);
			if(layer) {
			    opts.layers=[layer.clone()]
			}
		    }
		    opts.size.w=this.getOverviewMapWidth();
		    opts.size.h=this.getOverviewMapHeight();		    
		    opts.resolutionFactor = this.getOverviewMapResolution();
		    opts.minRatio = this.getOverviewMapMinRatio();
		    opts.maxRatio = this.getOverviewMapMaxRatio();		    
		    this.map.setShowOverviewMap(true,opts);
		}
		if(this.getShowGraticules()) {
		    this.map.setGraticulesVisible(true);
		}		
		this.map.myid = this.getLogLabel();
		//Set this so there is no popup on the off feature
		this.map.addKeyUpListener(event=>{
		    this.handleKeyUp(event);
		});
		this.map.addKeyDownListener(event=>{
		    this.handleKeyDown(event);
		});		
		this.map.textGetter = (layer,feature) =>{
		    return null;
		};
                this.lastWidth = this.jq(ID_MAP).width();
            }
	    this.initMap(this.map);
            if (this.doDisplayMap()) {
                this.map.setDefaultCanSelect(false);
            }
            this.map.initMap(false);
	    let hasLoc = Utils.isDefined(this.getZoomLevel())   ||
		Utils.isDefined(this.getMapCenter()) ||
		this.hadInitialPosition;
	    
            if (this.getPropertyBounds() ||this.getPropertyGridBounds() ) {
		this.hadInitialPosition = true;
                let toks = this.getPropertyBounds(this.getGridBounds("")).split(",");
                if (toks.length == 4) {
                    if (this.getShowBounds()) {
                        let attrs = {};
                        if (this.getBoundsStrokeColor(this.getProperty("boundsColor"))) {
                            attrs.strokeColor = this.getBoundsStrokeColor(this.getProperty("boundsColor"));
                        }
                        attrs.fillColor = this.getBoundsFillColor();
                        attrs.fillOpacity = this.getBoundsFillOpacity();			
                        let feature = this.map.addRectangle("bounds", parseFloat(toks[0]), parseFloat(toks[1]), parseFloat(toks[2]), parseFloat(toks[3]), attrs, "");
			feature.noSelect = true;
                    }
		    if(!hasLoc && this.setInitMapBounds)
			this.setInitMapBounds(parseFloat(toks[0]), parseFloat(toks[1]), parseFloat(toks[2]), parseFloat(toks[3]));
                }
            }


	    
	    if(this.getProperty("annotationLayer")) {
		let opts = {theMap:this.map,
			    embedded:true,
			    displayOnly:true,
			   };
		if(this.getPropertyAnnotationLayerTop()) {
		    opts.layerIndex = 100;
		}

		this.editableMap = new  RamaddaEditablemapDisplay(this.getDisplayManager(),HU.getUniqueId(""),opts);
		this.editableMap.initDisplay(true);
		this.editableMap.loadMap(this.getProperty("annotationLayer"));
	    }
	    
	    let extras = [];
	    if(this.getProperty('extraLayers')) {
		extras =Utils.mergeLists(extras,    this.getProperty('extraLayers').split(","));
	    }
	    if(Utils.stringDefined(this.getProperty('extraLayer'))) {
		extras.push(this.getProperty('extraLayer'));    
	    }
	    for(let i=1;true;i++) {
		if(!Utils.stringDefined(this.getProperty('extraLayer'+i))) break;
		extras.push(this.getProperty('extraLayer'+i));    
	    }


	    if(extras.length) {
		setTimeout(()=>{
		    this.addExtraLayers(extras);
		},1000);
	    }

            if (this.getShowLayers()) {
		//do this later so the map displays its initial location OK
		setTimeout(()=>{
		    let match = true;
		    let getId=id=>{
			match = true;
			if(id.startsWith('true:')) {
			    id =id.substring('true:'.length);
			    match=true;
			} else if(id.startsWith('false:')) {
			    id =id.substring('false:'.length);
			    match = false;
			}
			return id;
		    }
                    if (this.getProperty("shapefileLayer")) {
			let ids = Utils.split(this.getProperty('shapefileLayer',''),',',true,true);
			let labels = Utils.split(this.getProperty('shapefileLayerName',''),',',true,true);
			ids.forEach((id,idx)=>{
			    id = getId(id);
			    let url = RamaddaUtil.getUrl('/entry/show?output=shapefile.kml&entryid=' + id);
			    let label = labels[idx]??'Map';
			    this.addBaseMapLayer(url, label,true,match);
			});
                    }
                    if (this.getProperty('kmlLayer')) {
			let ids = Utils.split(this.getProperty('kmlLayer',''),',',true,true);
			let labels = Utils.split(this.getProperty('kmlLayerName',''),',',true,true);
			ids.forEach((id,idx)=>{
			    id = getId(id);
			    let url = this.getRamadda().getEntryDownloadUrl(id);
			    let label = labels[idx]??'Map';
			    this.addBaseMapLayer(url, label, true,match);
			});
                    }
		    let idx=0;
		    let geojsonUrl;
		    while((geojsonUrl=this.getProperty('geojsonLayer' + idx))) {
			this.addBaseMapLayer(geojsonUrl, '', false,match);
			idx++;
		    }
                    if (this.getProperty('geojsonLayer')) {
			let ids = Utils.split(this.getProperty('geojsonLayer',''),',',true,true);
			let labels = Utils.split(this.getProperty('geojsonLayerName',''),',',true,true);
			ids.forEach((id,idx)=>{
			    id = getId(id);
			    let url = this.getRamadda().getEntryDownloadUrl(id);
			    let label = labels[idx]??'Map';
			    this.addBaseMapLayer(url, label, false,match);
			});
                    }
		    let mapLayers = this.getProperty('mapLayers');
		    //Check to make sure it is an array
		    if(mapLayers && mapLayers.forEach) {
			this.displayingMapLayer = false;

			let process=(layer)=>{
			    let url
			    if(layer.type=='shapefile')
				url = RamaddaUtil.getUrl('/entry/show?output=shapefile.kml&entryid=' + layer.id);
			    else 
				url =  this.getRamadda().getEntryDownloadUrl(layer.id);
			    if(layer.match)
				this.displayingMapLayer = true;
			    this.addBaseMapLayer(url, layer.name, layer.type=='kml'||layer.type=='shapefile',layer.match,layer.style);
			};
			mapLayers.forEach(layer=>{if(layer.match) process(layer);});
			mapLayers.forEach(layer=>{if(!layer.match) process(layer);});			
		    }

		},500);
            }
        },

	addExtraLayers:function(extras) {
	    //extraLayers="baselayer:nexrad,geojson:US States:resources/usmap.json:fillColor:transparent"
	    extras.forEach(tuple=>{
		if(tuple.trim().length==0) return;
		tuple = tuple.replace(/https:/g,'https_semicolon_');
		tuple = tuple.replace(/\\:/g,'_semicolon_');		
		let args= {};
		let list = tuple.split(":");
		let type = list[0];
		for(let i=1;i<list.length;i+=2) {
		    let v = list[i+1]
		    if(!v) {
			console.log("Poorly formed extraLayers:" +tuple);
			continue;
		    }
		    args[list[i].trim()] =  Utils.getProperty(v.replace(/_semicolon_/g,':').replace(/_comma_/g,','));
		}

		let getUrl = url =>{
		    if(!url) return null;
		    if(url.startsWith("resources")) {
			url = RamaddaUtil.getUrl("/" + url);
		    } else if(url.startsWith("/resources")) {
			url = RamaddaUtil.getUrl(url);			
		    } else    if(!url.startsWith("/") && !url.startsWith("http")) {
			url = RamaddaUtil.getUrl("/entry/get?entryid=" + url);
		    }
		    return url;
		};

		let name = args['name'] ?? type;
		let isBaseLayer = Utils.isDefined(args.baseLayer)?args.baseLayer:false;
		let visible = Utils.isDefined(args.visible)?args.visible:true;
		if(type=="baselayer") {
		    if(!args.layer && list.length==2) {
			args.layer = list[1];
		    }
		    if(!args.layer) {
			this.logMsg("Could not find base layer:",tuple);
			return;
		    }
		    let layer = this.map.getBaseLayer(args.layer);
		    if(!layer) {
			this.logMsg("Could not find base layer:",tuple);
		    } else {
			layer.setVisibility(true);
		    }
		} else 	if(type=="geojson" || type=="kml") {
		    let url = getUrl(args.url);
		    if(!args.fillColor) args.fillColor='transparent';
		    //(name, url, canSelect, selectCallback, unselectCallback, args, loadCallback, zoomToExtent)
		    if(type=="kml") {
			this.map.addKmlLayer(name, url, false, null, null, args, null);
		    } else {
			this.map.addGeoJsonLayer(name, url, false, null, null, args, null);
		    }
		} else if(type=="wms") {
		    let url = args.url;
		    if(!url) {
			console.log("no url in wms:",args);
			return;
		    }
		    let layer=args.layer;
		    if(!layer) {
			console.log("no layer in wms:",args);
			return;
		    }
		    let opacity = args.opacity??1;
		    layer =  this.map.addWMSLayer(name,url,layer, isBaseLayer,true,{visible:visible,opacity:opacity});
		    if(isBaseLayer && (visible || this.getDefaultMapLayer()==name)) {
			this.map.getMap().setBaseLayer(layer);
		    }
		} else if(type=="xyz") {
		    let url = args.url;
		    if(!url) {
			console.log("no url in xyz:",args);
			return;
		    }
		    let layer = this.map.createXYZLayer(name,url,args.attribution,!isBaseLayer,visible);
                    this.map.addLayer(layer);
		    if(isBaseLayer && (visible || this.getDefaultMapLayer()==name)) {
			this.map.getMap().setBaseLayer(layer);
		    }
		} else {
		    console.log("Unknown map type:" + type)
		}
	    });
	},


        handleKeyUp:function(event) {
	},
        handleKeyDown:function(event) {
	},	
        addBaseMapLayer: function(url, label,isKml,matchData,style) {
	    if(!style) style={};
            let _this = this;
            mapLoadInfo = displayMapUrlToVectorListeners[url];
            if (mapLoadInfo == null) {
                mapLoadInfo = {
                    otherMaps: [],
                    layer: null
                };
                let selectFunc = function(layer) {
                    _this.mapFeatureSelected(layer);
                }
		//Don't do this for now as its handled elsewhere?
		selectFunc = null;
                let hasBounds = this.getProperty("bounds") != null ||
		    Utils.isDefined(this.getProperty("zoomLevel"))   ||
		    Utils.isDefined(this.getProperty("mapCenter"));
		let attrs =   {
                    strokeColor: matchData?this.getVectorLayerStrokeColor():
			this.getProperty('extraVectorLayerStrokeColor','blue'),
		    fillColor:this.getVectorLayerFillColor(),
		    fillOpacity:matchData?this.getVectorLayerFillOpacity():0,
                    strokeWidth: matchData?this.getVectorLayerStrokeWidth():
			this.getProperty('extraVectorLayerStrokeWidth',1),			
		}

		$.extend(attrs,style);
		//For some reason the attrs don't get applied to kml layers so we pass the attrs to baseMapLoaded
		let callback = (map, layer) =>{_this.baseMapLoaded(layer, url,isKml?attrs:null,matchData);}
		let layer;
		if (isKml)
                    layer = this.map.addKMLLayer(label??'Map', url, matchData &&this.doDisplayMap(), selectFunc,
						 null, attrs, callback, !hasBounds && matchData);
                else 
                    layer = this.map.addGeoJsonLayer(label??'Map', url, matchData&&this.doDisplayMap(), selectFunc,
						     null,   attrs,  callback, !hasBounds && matchData);
            } else if (mapLoadInfo.layer) {
                this.cloneLayer(mapLoadInfo.layer);
            } else {
                this.map.showLoadingImage();
                mapLoadInfo.otherMaps.push(this);
            }
        },
        baseMapLoaded: function(layer, url,attrs,matchData) {
	    if(attrs &&layer.features) {
		layer.features.forEach(f=>{
		    if(f.style) {
			$.extend(f.style,attrs);
		    } else {
			f.style = $.extend({},attrs);
		    }
		});
                layer.redraw();
	    }
	    if(this.getJustShowMapLayer()) return;
	    if(!matchData)return;
            this.vectorLayer = layer;
            this.applyVectorMap();
            mapLoadInfo = displayMapUrlToVectorListeners[url];
            if (mapLoadInfo) {
                mapLoadInfo.layer = layer;
                for (let i = 0; i < mapLoadInfo.otherMaps.length; i++) {
                    mapLoadInfo.otherMaps[i].cloneLayer(layer);
                }
                mapLoadInfo.otherMaps = [];
            }
        },
        applyVectorMap: function(force, textGetter, args) {
	},
        getBounds: function() {
	    if(this.map)
		return this.map.getBounds();
	    return null;
	},
    });
}



function RamaddaMapDisplay(displayManager, id, properties) {
    const ID_MAP_SLIDER = "map_slider";    
    const ID_LATFIELD = "latfield";
    const ID_LONFIELD = "lonfield";
    const ID_SIZEBY_LEGEND = "sizebylegend";
    const ID_COLORTABLE_SIDE = "colortableside";
    const ID_SHAPES = "shapes";
    const ID_HEATMAP_ANIM_LIST = "heatmapanimlist";
    const ID_HEATMAP_ANIM_PLAY = "heatmapanimplay";
    const ID_HEATMAP_ANIM_STEP_FORWARD = "heatmapanimstepforward";
    const ID_HEATMAP_ANIM_STEP_BACK = "heatmapanimstepback";    
    const ID_HEATMAP_TOGGLE = "heatmaptoggle";    
    const ID_HTMLLAYER = "htmllayer";
    const ID_TRACK_VIEW = "trackview";

    $.extend(this, {
        showBoxes: true,
        showPercent: false,
        percentFields: null,
        kmlLayer: null,
        kmlLayerName: "",
        geojsonLayer: null,
        geojsonLayerName: "",
        theMap: null,
	layerVisible:true
    });

    


    const SUPER = new RamaddaBaseMapDisplay(displayManager, id, DISPLAY_MAP, properties);
    RamaddaUtil.inherit(this,SUPER);
    addRamaddaDisplay(this);
    this.defineSizeByProperties();
    let shapes = 'square|triangle|downtriangle|circle|plane|star|cross|diamond|x|thinx||lightning|church';
    let myProps = [
	{label:'Map Properties'},
	{p:'strokeWidth',d:1},
	{p:'strokeColor',d:'#000'},
	{p:'strokeOpacity',d:1},
	{p:'fillColor',d:'blue'},
	{p:'fillOpacity',d:0.5},
	{p:'radius',d:5,tt:'Size of the map points'},
	{p:'scaleRadius',ex:'true',tt:'Scale the radius based on # points shown'},
	{p:'scaleRadiusMin',d:1},
	{p:'scaleRadiusMax',d:10},	
	{p:'scaleRadiusMaxPoints',d:5000},
	{p:'radiusList',tt:'comma separated list of count:size',
	 ex:'100:15,200:12,300:10'},
	{p:'maxRadius',ex:'16',d:1000},
	{p:'shape',d:'circle',ex:shapes,tt:'Use shape'},
	{p:'shapeBy',tt:'field to shape by'},
	{p:'shapeByMap',ex:'value1:' + shapes+':label1,value2:...'},
	{p:'defaultShape',ex:shapes},
	{p:'markerIcon',ex:'/icons/...'},
	{p:'iconSize',ex:16},
	{p:'hideMissingColor',ex:true,tt:'hide points when no color by value'},
	{p:'justOneMarker',ex:'true',tt:'This is for data that is all at one point and you want to support selecting points for other displays'},	
	{p:'showPoints',ex:'true',tt:'Also show the map points when showing heatmap or glyphs or vectors'},
	{p:'applyPointsToVectors',d:true,tt:'If false then just show any attached map vectors without coloring them from the points'},
	{p:'bounds',ex:'north,west,south,east',tt:'initial bounds'},
	{p:'gridBounds',ex:'north,west,south,east'},	
	{p:'mapCenter',ex:'lat,lon',tt:'initial position'},
	{p:'zoomLevel',ex:4,tt:'initial zoom'},
	{p:'zoomTimeout',ex:500,tt:'initial zoom timeout delay. set this if the map is in tabs, etc, and not going to the initial zoom'},


	{p:'fixedPosition',ex:true,tt:'Keep the initial position'},
	{p:'linked',ex:true,tt:'Link location with other maps'},
	{p:'linkGroup',ex:'some_name',tt:'Map groups to link with'},
	{p:'initialLocation', ex:'lat,lon',tt:'initial location'},
	{p:'defaultMapLayer',ex:'osm|google.roads|esri.street|google.hybrid|google.terrain|google.satellite|opentopo|esri.topo|usfs|usgs.topo|naip|usgs.imagery|esri.shaded|esri.lightgray|esri.darkgray|esri.terrain|shadedrelief|esri.aeronautical|historic|osm.toner|osm.toner.lite'},
	{p:'extraLayers',tt:'comma separated list of layers to display',
	 ex:'baselayer:goes-visible,baselayer:nexrad,geojson:US States:/resources/usmap.json:fillColor:transparent'},

	{p:'skipZero',ex:'true',tt:'Skip locations that are at 0,0'},

	{p:'doPopup', ex:'false',tt:'Do not show popups'},
	{p:'doPopupSlider', ex:'true',tt:'Do the inline popup that slides down'},
	{p:'popupSliderRight', ex:'true',tt:'Position the inline slider to the right'},	
	{p:'popupSliderStyle', ex:'max-width:300px;overflow-x:auto;',tt:''},	
	{p:'showRegionSelector',ex:true},
	{p:'regionSelectorLabel'},	
	{p:'centerOnFilterChange',d:true,ex:false,tt:'Center map when the data filters change'},
	{p:'centerOnHighlight',ex:true,tt:'Center map when a record is highlighted'},
	{p:'centerOnMarkersAfterUpdate',ex:true,tt:'Always center on the markers'},	
	{p:'zoomLevelOnHighlight',ex:16,tt:'Set the zoom level'},
	{p:'doInitCenter',tt:'Center the maps on initialization'},
	{p:'boundsAnimation',ex:true,tt:'Animate when map is centered'},
	{p:'iconField',ex:'""',tt:'Field id for the image icon url'},
	{p:'rotateField',ex:'""',tt:'Field id for degrees rotation'},
	{p:'rotateScale',d:'1.0',tt:'Scale value to multiply the rotate field value by to get degrees rotation'},		
	{p:'filterBadLocations',ex:'false',d:true,tt:'Do not show records with bad lat/lon'},
	{p:'hideNaN',tt:'If doing color by do not show the points with missing values'},


	{label:'Map GUI'},
	{p:'showTableOfContents',ex:'true',tt:'Show left table of contents'},
	{p:'tocTitle'},
	{p:'tocWidth'},
	{p:'tocZoom',ex:3,tt:'zoom level when clicking on a table of contents item'},
	{p:'tocFields',ex:'',tt:'fields to show in TOC'},
	{p:'tocTemplate',ex:'',tt:'template to show in TOC'},	

	{p:'showMarkersToggle',ex:'true',tt:'Show the toggle checkbox for the marker layer'},
	{p:'showMarkersToggleLabel',ex:'label',tt:'Label to use for checkbox'},
	{p:'showClipToBounds',ex:'true',tt:'Show the clip bounds checkbox'},
	{p:'clipToBounds',ex:'true',tt:'Clip to bounds'},	
	{p:'showMarkers',ex:'false',tt: 'Hide the markers'},
	{p:'acceptEntryMarkers',ex:'true',d:false,tt:'If other maps can add entry markers and boxes'},



	{label:'Map Highlight'},
	{p:'showRecordSelection',ex:'false',d:'true'},
	{p:'highlight',ex:'true',tt:'Show mouse over highlights'},
	{p:'displayDiv',tt:'Div id to show highlights in'},
	{p:'showRecordHighlight',d:true},
	{p:'recordHighlightFeature',ex:'true',tt:'If there is a vector map that is being shown then highlight the map feature instead of drawing a point'},
	{p:'recordHighlightIcon',ex:'/icons/plane.png'},
	{p:'recordHighlightIconSize',d:30},
	{p:'recordHighlightShape',ex:shapes},
	{p:'recordHighlightRadius',ex:'20',tt:'Radius to use to show other displays highlighted record'},
	{p:'recordHighlightStrokeWidth',ex:'2',tt:'Stroke to use to show other displays highlighted record'},
	{p:'recordHighlightStrokeColor',d:'red',tt:'Color to use to show other displays highlighted record'},
	{p:'recordHighlightFillColor',ex:'rgba(0,0,0,0)',tt:'Fill color to use to show other displays highlighted record'},
	{p:'recordHighlightFillOpacity',ex:'0.5',tt:'Fill opacity to use to show other displays highlighted record'},
	{p:'recordHighlightVerticalLine',tt:'Draw a vertical line at the location of the selected record'},
	{p:'highlightColor',ex:'#ccc',tt:''},
	{p:'highlightFillColor',ex:'#ccc',tt:''},	
	{p:'highlightStrokeWidth',ex:'2',tt:''},	
	{p:'unhighlightColor',ex:'#ccc',tt:'Fill color when records are unhighlighted with the filters'},
	{p:'unhighlightStrokeWidth',ex:'1',tt:'Stroke width for when records are unhighlighted with the filters'},
	{p:'unhighlightStrokeColor',ex:'#aaa',tt:'Stroke color for when records are unhighlighted with the filters'},
	{p:'unhighlightRadius',d:-1,ex:'1',tt:'Radius for when records are highlighted with the filters'},

	{label:'Map Collisions'},
	{p:'handleCollisions',ex:true,tt:'Handle point collisions'},
	{p:'showCollisionToggle',ex:true,tt:'Show the toggle checkbox'},
	{p:'collisionFixed',d:false,ex:true,
	 tt:'If true, don\'t show the grouped markers on a click'},
	{p:'collisionPointSize',d:16,tt:'Size of each point. Higher # is more spread out'},
	{p:'collisionDotColor',d:'#fff',tt:'Color of dot drawn at center'},
	{p:'collisionDotOpacity',d:'0.9',tt:'Opacity of dot drawn at center'},	
	{p:'collisionRingColor',d:'#000',tt:'Color of ring'},
	{p:'collisionRingWidth',d:0.25,tt:'Width of ring'},	
	{p:'collisionDotColorOn',d:'blue',tt:'color to use when the collision marker is selected'},
	{p:'collisionDotRadius',d:12,tt:'Radius of dot drawn at center'},
	{p:'collisionScaleDots',ex:true,d:false,tt:'Scale the group dots'},
	{p:'collisionLineColor',ex:'red',tt:'Color of line drawn at center'},
	{p:'collisionLabelTemplate',d:'${count}'},
	{p:'collisionLabelColor',d:'#000'},
	{p:'collisionLabelFontSize',d:'10'},	


	{p:'collisionIcon',ex:'/icons/...',tt:'Use an icon for collisions',canCache:true},
	{p:'collisionIconSize',d:16,ex:'16',canCache:true},
	{p:'collisionTooltip',ex:'${default}',tt:'Tooltip to use for collision dot',canCache:true},


	{label:'Map Lines'},
	{p:'showSegments',ex:'true',tt:'If data has 2 lat/lon locations draw a line'},
	{p:'segmentWidth',d:'1',tt:'Segment line width'},	
	{p:'useGreatCircle',d:false,ex:'true',tt:'use great circle routes for segments'},
	{p:'sizeSegments',d:false,ex:'true',tt:'Size the segments based on record value'},	
	{p:'isPath',ex:'true',tt:'Make a path from the points'},
	{p:'isPathThreshold',ex:'1000',tt:'Make path from the points if # records<threshold'},
	{p:'groupByField',tt:'Field id to group the paths'},	
	{p:'pathWidth',d:'1'},
	{p:'pathColor',ex:'red'},	
	{p:'pathWindowTime',tt:'Show leading dots',ex:'1 day'},
	{p:'pathWindowSize',tt:'Number of records to show as leading dots'},
	{p:'pathWindowStrokeColor'},

	{p:'isTrajectory',ex:'true',tt:'Make a path from the points'},	
	{p:'showPathEndPoint',ex:true},
	{p:'pathEndPointShape',ex:'arrow'},
	{p:'latField1',tt:'Field id for segments'},
	{p:'lonField1',tt:'Field id for segments'},
	{p:'latField2',tt:'Field id for segments'},
	{p:'lonField2',tt:'Field id for segments'},
	{p:'trackUrlField',ex:'field id',tt:'The data can contain a URL that points to data'},

	{label:'Map Labels'},
	{p:'labelTemplate',ex:'${field}',tt:'Display labels in the map'},
	{p:'declutterLabels',d:true},
	{p:'labelRecordTemplate',ex:'${field}',tt:'Apply the template to the records. Use ${recordtemplate} for the labelTemplate'},
	{p:'labelKeyField',ex:'field',tt:'Make a key, e.g., A, B, C, ... based on the value of the key field'},	
	{p:'labelLimit',ex:'1000',tt:'Max number of records to display labels'},
	{p:'labelFontColor',ex:'#000'},
	{p:'labelFontSize',ex:'12px'},
	{p:'labelFontFamily',ex:'\'Open Sans\', Helvetica Neue, Arial, Helvetica, sans-serif'},
	{p:'labelFontWeight',ex:'plain'},
	{p:'labelBackground',ex:'green'},
	{p:'labelStrokeColor',ex:'#000'},
	{p:'labelStrokeWidth',d:1},
	{p:'labelAlign',ex:'l|c|r t|m|b'},
	{p:'labelXOffset',ex:'0'},
	{p:'labelYOffset',ex:'0'},
	{p:'labelOutlineColor',ex:'#fff'},
	{p:'labelOutlineWidth',ex:'0'},
	{p:'labelDeclutterPadding',d:1},
	{p:'labelDeclutterGranularity',d:1},
	{p:'labelDeclutterPixelsPerLine'},
	{p:'labelDeclutterPixelsPerCharacter'},
	{label:'Map Glyphs'},
	{p:'doGridPoints',ex:'true',tt:'Display a image showing shapes or bars',canCache:true},
	{p:'gridWidth',ex:'800',tt:'Width of the canvas'},
	{label:'label glyph',p:'glyph1',ex:'type:label,pos:sw,dx:10,dy:-10,label:field_colon_ ${field}_nl_field2_colon_ ${field2}'},
	{label:'rect glyph', p:'glyph1',ex:'type:rect,pos:sw,dx:10,dy:0,colorBy:field,width:150,height:100'},
	{label:'circle glyph',p:'glyph1',ex:'type:circle,pos:n,dx:10,dy:-10,fill:true,colorBy:field,width:20,baseWidth:5,sizeBy:field'},
	{label:'3dbar glyph', p:'glyph1',ex:'type:3dbar,pos:sw,dx:0,dy:0,height:30,width:6,baseHeight:5,sizeBy:field'},
	{label:'gauge glyph',p:'glyph1',ex:'type:gauge,color:#000,pos:sw,width:50,height:50,dx:10,dy:-10,sizeBy:field,sizeByMin:0'},

	{label:'Hex map, Voronoi, etc'},
	{p:'mapType',ex:'hex or triangle or square or voronoi',tt:'Create a hex or triangle or square or voronoi map'},
	{p:'doHexmap',ex:'true',tt:'Create a hexmap'},
	{p:'doTrianglemap',ex:'true',tt:'Create a triangle map'},		
	{p:'doSquaremap',ex:'true',tt:'Create a square map'},		
	{p:'hexmapUseCount',ex:'true',tt:'Use the record count for the color'},
	{p:'hexmapUseEnum',ex:'true',tt:'Use the value which appears the most'},
	{p:'hexmapLabelTemplate',ex:'${count} ${colorbyvalue}',tt:'For the label'},
	{p:'hexmapShowTotal',ex:'true',tt:'Total the values'},	
	{p:'hexmapMinValue',ex:'1',tt:'If doing averages this is the lower cut off to add to a total'},
	{p:'hexmapMaxValue',ex:'100',tt:'If doing averages this is the upper cut off to add to a total'},
	{p:'hexmapUseFullBounds',d:true,tt:'When filtering is the original fill bounds used'},
	{p:'hexmapPadding',tt:'Percent to bad out the bounding box',d:0.05},
	{p:'hexmapCellSide',tt:'How many on units per side',d:25},
	{p:'hexmapUnits',tt:'Side units',ex:'miles|kilometers|degrees|radians',d:'miles'},
	{p:'hexmapStrokeColor',d:'blue'},
	{p:'hexmapStrokeWidth',d:1},
	{p:'hexmapStrokeOpacity',d:1.0},
	{p:'hexmapStrokeDashstyle',d:'solid'},
	{p:'hexmapFillColor',d:'transparent'},
	{p:'hexmapFillOpacity',d:1.0},	
	{p:'hexmapColorBy',tt:'Field id to color the hexmap by'},
	{p:'hexmapColorTable',tt:'Color table for hexmap'},	
	{p:'hexmapEmptyStrokeColor'},
	{p:'hexmapEmptyStrokeWidth'},
	{p:'hexmapEmptyFillColor'},
	{p:'doVoronoi',ex:'true',tt:'Create  voronoi polygons'},
	{p:'voronoiColorBy',tt:'Field id to color the hexmap by'},
	{p:'voronoiStrokeColor',d:'blue'},
	{p:'voronoiStrokeWidth',d:1},
	{p:'voronoiStrokeOpacity',d:1},	
	{p:'voronoiStrokeDashstyle',d:'solid'},
	{p:'voronoiFillColor',d:'transparent'},
	{p:'voronoiFillOpacity',d:1.0},	
	{p:'voronoiPadding',ex:0.1,tt:'% to pad the bounds, 0-1.0'},
	{label:'Heatmap'},
	{p:'doHeatmap',ex:'true',tt:'Grid the data into an image',canCache:true},
	{p:'hmShowPoints',ex:'true',tt:'Also show the map points'},
	{p:'hmShowReload',ex:'true',tt:''},
	{p:'hmShowGroups',ex:'true',tt:''},
	{p:'hmBounds',ex:'north,west,south,east',tt:''},
	{p:'htmlLayerField'},
	{p:'htmlLayerShape',ex:'barchart|piechart'},	
	{p:'htmlLayerWidth',ex:'30'},
	{p:'htmlLayerHeight',ex:'15'},
	{p:'htmlLayerStyle',ex:'css style'},
	{p:'htmlLayerScale',d:'2:0.75,3:1,4:2,5:3,6:4,7:6',tt:'zoomlevel:scale,...'},
	{p:'htmlLayerPopupLabelField'},
	{p:'htmlLayerMin',tt:'min value for sparkline'},
	{p:'htmlLayerMax',tt:'max value for sparkline'},	
	{p:'htmlLayerFlipYAxis',ex:true},
	{p:'htmlLayerDrawAxisLabels',ex:true},
	{p:'htmlLayerPopupDrawAxisLabels',ex:true},	
	{p:'htmlLayerScaleWithAll',ex:'false',tt:'Scale data values with all or with just the local data'},
	{p:'cellShape',ex:'rect|3dbar|circle|vector'},
	{p:'cellColor',ex:'color'},
	{p:'cellFilled',ex:true},
	{p:'cellSize',ex:'8'},
	{p:'cellSizeH',ex:'20',tt:'Base value to scale by to get height'},
	{p:'cellSizeHBase',ex:'0',tt:'Extra height value'},
	{p:'arrowLength',d:-1,canCache:true},
	{p:'lineWidth',d:1,canCache:true},	
	{p:'angleBy',ex:'field',tt:'field for angle of vectors'},
	{p:'hmOperator',ex:'count|average|min|max'},
	{p:'hmAnimationSleep',ex:'1000'},
	{p:'hmReloadOnZoom',ex:'true'},
	{p:'reloadOnZoom',ex:'true'},	
	{p:'hmGroupByDate',ex:'true|day|month|year|decade',tt:'Group heatmap images by date'}, 
	{p:'hmGroupBy',ex:'field id',tt:'Field to group heatmap images'}, 
	{p:'hmLabelPrefix'},
	{p:'hmShowToggle',ex:true,tt:'Show the toggle checkbox to turn off/on the heatmap'},
	{p:'hmToggleLabel'},
	{p:'boundsScale',ex:'0.1',tt:'Scale up the map bounds'},
	{p:'hmFilter',ex:'average5|average9|average25|gauss9|gauss25',tt:'Apply filter to image'},
	{p:'hmFilterPasses',ex:'1'},
	{p:'hmFilterThreshold',ex:'1'},
	{p:'hmCountThreshold',ex:'1'},
    ];
    
    myProps.push({label:'Canvas'});
    myProps.push(...RamaddaDisplayUtils.getCanvasProps());
    myProps.push(...RamaddaDisplayUtils.sparklineProps);


    displayDefineMembers(this, myProps, {
        mapBoundsSet: false,
        features: [],
        myMarkers: {},
        mapEntryInfos: {},
	tracks:{},
	checkFinished: function() {
	    return true;
	},
        initDisplay: function() {
            SUPER.initDisplay.call(this);
            let _this = this;
	    let legendSide = this.getProperty('sizeByLegendSide');
	    if(legendSide) {
		let legend = HU.div([ATTR_ID,this.domId(ID_SIZEBY_LEGEND)]);
		if(legendSide=='top') {
		    this.jq(ID_HEADER0).append(legend);
		} else if(legendSide=='left') {
		    this.jq(ID_LEFT).append(legend);
		} else if(legendSide=='right') {
		    this.jq(ID_RIGHT).append(legend);
		} else if(legendSide=='bottom') {
		    this.jq(ID_BOTTOM).append(legend);
		} else {
		    console.log('Unknown legend side:' + legendSide);
		}
	    }
	    this.startProgress();
        },

        checkLayout: function() {
            if (!this.map) {
                return;
            }
            let d = this.jq(ID_MAP);
            if (d.width() ==0) return;
	    //Wait a bit so the dom settles down
	    setTimeout(()=>{
		this.checkLayoutInner();
	    },1000);
	},

        checkLayoutInner: function() {
            let d = this.jq(ID_MAP);
            this.map.getMap().updateSize();
            if (d.width() > 0 && this.lastWidth != d.width()) {
                this.lastWidth = d.width();
            }

	    if(!this.setMapLocationAndZoom && this.mapParams) {
		this.setMapLocationAndZoom = true;
		if(this.mapParams.initialZoom>=0) {
		    this.map.zoomTo(this.mapParams.initialZoom);
		}
		if(this.mapParams.initialLocation) {
		    let loc = MapUtils.createLonLat(this.mapParams.initialLocation.lon, this.mapParams.initialLocation.lat);
		    this.map.setCenter(loc);
		}
	    }
	    //And for some reason we need a little more delay for the final redraw
	    setTimeout(()=>{
		this.map.redraw();
	    },500);
        },

	handlePopup: function(feature, popup) {
	    if(!this.trackUrlField) return;
	    let func = ()=>{
		if(feature.record) {
		    if(this.tracks[feature.record.getId()]) {
			this.removeTrack(feature.record);
		    } else {
			let url = feature.record.getValue(this.trackUrlField.getIndex());
			$.getJSON(url, data=>{this.loadTrack(feature.record, data)}).fail(err=>{console.log("url failed:" + url +"\n" + err)});
		    }
		}
	    };
	    this.jq(ID_TRACK_VIEW).click(func);
	    this.jq(ID_TRACK_VIEW+"_1").click(func);	    
	},
	macroHook: function(record, token,value) {
	    if(!this.trackUrlField) {
		return null;
	    }
	    if(token.tag!=this.trackUrlField.getId()) {
		return null;
	    }
	    if(String(value).trim().length==0) return "";
	    this.currentPopupRecord = record;
	    let haveTrack = this.tracks[record.getId()]!=null;
	    let label =haveTrack?"Remove track":(token.attrs["label"] ||  "View track");
	    return SPACE + HU.span([ATTR_CLASS,"ramadda-clickable",ATTR_ID,this.domId(ID_TRACK_VIEW)],label);
	}, 
	getRecordUrlHtml: function(attrs, field, record) {
	    this.currentPopupRecord = record;
	    if(!this.trackUrlField || this.trackUrlField.getId()!=field.getId()) {
		return SUPER.getRecordUrlHtml.call(this, attrs, field, record);
	    }
	    let value = record.getValue(field.getIndex());
	    let haveTrack = this.tracks[record.getId()]!=null;
	    let label = haveTrack?"Remove track":(attrs[field.getId()+".label"] || "View track");
	    return  HU.span([ATTR_CLASS,"ramadda-clickable",ATTR_ID,this.domId(ID_TRACK_VIEW+"_1")],label);
	},
	removeTrack:function(record) {
	    if(this.tracks[record.getId()]) {
		this.map.removePolygon(this.tracks[record.getId()]);
		this.tracks[record.getId()] = null;
	    }
	    this.jq(ID_TRACK_VIEW).html("View track");
	    this.jq(ID_TRACK_VIEW+"_1").html("View track");
	    let item = this.jq(ID_LEFT).find(HU.attrSelect(RECORD_ID,record.getId()));
	    item.removeClass("display-map-toc-item-on");
	},
	loadTrack: function(record, data) {
            let newData = makePointData(data, null,this,"");
	    let points = RecordUtil.getPoints(newData.getRecords(),{});
	    let feature = this.markers?this.markers[record.getId()]:null;
	    let item = this.jq(ID_LEFT).find(HU.attrSelect(RECORD_ID,record.getId()));
	    record.trackData = newData;
	    item.addClass("display-map-toc-item-on");
	    try {
		record.setLocation(points[0].y, points[0].x);
		let loc =  MapUtils.createLonLat(points[0].x, points[0].y);
		loc = this.map.transformLLPoint(loc);
		if(feature)
		    feature.move(loc);
	    } catch(err) {
		console.log(err);
	    }

	    let bounds = {};
	    let attrs = {
		strokeColor:this.getStrokeColor("blue"),
		strokeWidth:this.getStrokeWidth(1),
		fillColor:this.getProperty("fillColor") ||'transparent',
	    };
            let polygon = this.map.addPolygon("", "", points, attrs);
	    polygon.record = record;
	    this.tracks[record.getId()]=polygon;
	    if(polygon.geometry) {
		this.map.zoomToExtent(polygon.geometry.getBounds());
	    }
	    this.map.closePopup();
	    setTimeout(()=>{
		this.getDisplayManager().notifyEvent(DisplayEvent.dataSelection, this, {data:newData});
	    },100);
	},
	applyToFeatureLayers:function(func) {
	    if(this.myFeatureLayer) func(this.myFeatureLayer);
	    if(this.myFeatureLayerNoSelect) func(this.myFeatureLayerNoSelect);
	},
	addFeatures:function(features,noSelect) {
	    let madeNewOne = false;
	    let layer = noSelect?this.myFeatureLayerNoSelect:this.myFeatureLayer;
	    if(!layer) {
		if(debugit && ycnt!=this.myycnt) {
		    console.trace("addFeatures",this.myName);
		}
		if(noSelect) 
		    layer = this.myFeatureLayerNoSelect = this.map.createFeatureLayer("Map Features NS",false);		
		else
		    layer = this.myFeatureLayer = this.map.createFeatureLayer("Map Features",true);
		if(Utils.isDefined(this.layerVisible)) {
		    layer.setVisibility(this.layerVisible);
		}
		if(this.getProperty("showMarkersToggle") && !this.getProperty("markersVisibility", true)) {
		    this.applyToFeatureLayers(layer=>{layer.setVisibility(false);});
		}
		this.myFeatures= [];
	    }
	    layer.addFeatures(features);
	    features.forEach(feature=>{
		feature.layer = layer;
		this.myFeatures.push(feature);
	    });
	},

	removeFeature: function(feature) {
	    if(feature) {
		this.applyToFeatureLayers(layer=>{
		    layer.removeFeatures([feature]);
		});
	    }
	},

	removeExtraLayers: function() {
	    try {
		this.extraLayers.every(layer=>{
		    layer.destroy();
		    this.map.removeLayer(layer,true);
		    return true;
		});
	    } catch(exc) {
		console.log(exc);
	    }
	    this.extraLayers = [];
	    this.heatmapLayers = [];
	    this.voronoiLayer=null;
	    this.hexmapLayer=null;
	},

	getMyMapLayers:function() {
	    return [this.heatmapLayers,this.voronoiLayer,this.hexmapLayer];
	},

	toString: function() {
	    return "displaymap";
	},
	deleteDisplay: function() {
	    if(debugit)
		console.log("delete:" + this.myName);
	    this.getDisplayManager().removeDisplay(this);
	    this.removeFeatures();
	    this.displayDeleted = true;
            this.map.getMap().events.unregister("updatesize", this,this.updatesizeFunc);
            this.map.getMap().events.unregister("moveend", this,this.moveendFunc);
            this.map.getMap().events.unregister("zoomend", this,this.zoomendFunc);	    	    
            let pointData = this.getPointData();
	    if(pointData) {
		pointData.removeFromCache(this);
	    }
	},

	removeFeatures: function() {
	    this.removeFeatureLayer();
	    this.removeExtraLayers();
	},


	setVisible: function(visible) {
	    this.layerVisible = visible;
	    let show = true;
	    if(this.jq("showMarkersToggle").length>0) {
		show = this.jq("showMarkersToggle").is(":checked");
	    }
	    if(show) {
		if(this.myFeatureLayer) {
		    this.myFeatureLayer.setVisibility(visible);
		    //		    console.log("setvisible:" + visible+" "+ this.myFeatureLayer.getVisibility());
		}
		if(this.myFeatureLayerNoSelect) {
		    this.myFeatureLayerNoSelect.setVisibility(visible);
		}
	    }
	    //TODO: have my own labelLayer
	    if(this.map.labelLayer)
		this.map.labelLayer.setVisibility(visible);
	    this.extraLayers.forEach(layer=>{
		layer.setVisibility(visible);
	    });

	},

	removeFeatureLayer: function() {
	    if(debugMapTime) {
		console.log('start removeFeatureLayer');
		console.time('removeFeatureLayer');
	    }
	    if(this.myFeatureLayer) {
		this.myFeatureLayer.destroy();
		this.map.removeLayer(this.myFeatureLayer,true);
		this.myFeatureLayer = null;
	    }
	    if(this.myFeatureLayerNoSelect) {
		this.myFeatureLayerNoSelect.destroy();
		this.map.removeLayer(this.myFeatureLayerNoSelect,true);
		this.myFeatureLayerNoSelect = null;
	    }	    
	    if(this.labelFeatures) {
		this.map.labelLayer.removeFeatures(this.labelFeatures,{silent:true});
		this.labelFeatures = null;
		this.jq("legendid").html("");
	    }
	    if(debugMapTime)
		console.timeEnd('removeFeatureLayer');
	    this.myFeatures= null;
	},

        initMapParams: function(params) {
	    SUPER.initMapParams.call(this,params);
	    if(this.getDoPopupSlider() || this.getPopupSliderRight()) {
		params.doPopupSlider = true;
		if(this.getPopupSliderRight()) {
		    params.popupSliderRight = true;
		}
	    }
	},
	initMap: function(map) {
	    if(!this.getShowMarkers(this.getProperty("markersVisibility", true))) {
		map.getMarkersLayer().setVisibility(false);
	    }
	    let boundsAnimation = this.getProperty("boundsAnimation");
	    if(boundsAnimation) {
		this.didAnimationBounds = false;
                let animationBounds = boundsAnimation.split(",");
                if (animationBounds.length == 4) {
		    let pause = parseFloat(this.getProperty("animationPause","1000"));
		    HU.callWhenScrolled(this.domId(ID_MAP),()=>{
			if(this.didAnimationBounds) {
			    return;
			}
			this.didAnimationBounds = true;
			let a = animationBounds;
			let b = MapUtils.createBounds(parseFloat(a[1]),parseFloat(a[2]),parseFloat(a[3]),parseFloat(a[0]));
			this.map.animateViewToBounds(b);
		    },pause);
		}
            }
	},
        createMap: function() {
	    SUPER.createMap.call(this);
            let _this = this;
	    if(!this.getShowMarkers(this.getProperty("markersVisibility", true))) {
		this.map.getMarkersLayer().setVisibility(false);
	    }

            if (this.doDisplayMap()) {
                this.map.setDefaultCanSelect(false);
            }
            this.map.initMap(false);
            this.map.addRegionSelectorControl(function(bounds) {
		_this.propagateEvent(DisplayEvent.mapBoundsChanged, {"bounds": bounds,    "force": true});
            },true);
	    this.map.popupHandler = (feature,popup) =>{
		this.handlePopup(feature, popup);
	    };
	    this.map.addFeatureSelectHandler(feature=>{
		if(debugPopup) console.log("\tdisplaymap: featureSelectHandler");
		let didSomething= false;
		let record = feature.record;
		if(feature.collisionInfo)  {
		    if(debugPopup) console.log("has collisioninfo");
		    return feature.collisionInfo.dotSelected(feature);
		}
		if(record) {
		    this.propagateEventRecordSelection({record:record});
		    this.propagateFilterFields(record);
		    //		    didSomething= true;
		}

		if(record && !this.getMap().getDoPopup() && this.getShowRecordSelection()) {
		    if(debugPopup) console.log("highlighting point");
		    this.highlightPoint(record.getLatitude(),record.getLongitude(),true,false);
		    //		    didSomething= true;
		}

		if(record && this.getProperty("shareSelected")) {
		    let idField = this.getFieldById(null,ATTR_ID);
		    if(idField) {
			ramaddaDisplaySetSelectedEntry(record.getValue(idField.getIndex()),this.getDisplayManager().getDisplays(),this);
		    }
		    if(debugPopup) console.log("\tdisplaymap: share selected");
		    //		    didSomething= true;
		}
		if(didSomething)
		    this.lastFeatureSelectTime = new Date();
		return false;
	    });

            this.map.addFeatureHighlightHandler((feature, highlight)=>{
		let record = feature.record;
		if(record) {
		    if(this.lastHighlightedRecord) {
			let args = {highlight:false,record: this.lastHighlightedRecord};
			this.getDisplayManager().notifyEvent(DisplayEvent.recordHighlight, this, args);
			if (this.getAnimationEnabled()) {
			    this.getAnimation().handleEventRecordHighlight(this, args);
			}
			this.lastHighlightedRecord = null;
		    }
		    if(highlight) {
			this.lastHighlightedRecord = record;
		    }
		    let args = {highlight:highlight,record: record};
		    this.getDisplayManager().notifyEvent(DisplayEvent.recordHighlight, this, args);
		    if (this.getAnimationEnabled()) {
			this.getAnimation().handleEventRecordHighlight(this, args);
		    }
		}

	    });

	    this.map.highlightBackgroundColor=this.getProperty("highlighBackgroundColor","#fffeec");
	    if(this.getProperty("addEntryMarkers")) {
		this.map.setDoPopup(true);
	    } else {
		this.map.setDoPopup(this.getProperty("doPopup",true));
	    }
	    this.createTime = new Date();

            this.map.addClickHandler(this.domId(ID_LONFIELD), this
				     .domId(ID_LATFIELD), null, this);

            this.map.getMap().events.register("updatesize", this, this.updatesizeFunc=()=>{
		if(!this.callingUpdateSize) {
		    _this.updateHtmlLayers();
		}
            });

	    //register the events in a bit
	    setTimeout(()=>{
		this.registerEvents();
	    },1000);



	    
	    let boundsAnimation = this.getProperty("boundsAnimation");
	    if(boundsAnimation) {
		this.didAnimationBounds = false;
                let animationBounds = boundsAnimation.split(",");
                if (animationBounds.length == 4) {
		    let pause = parseFloat(this.getProperty("animationPause","1000"));
		    HU.callWhenScrolled(this.domId(ID_MAP),()=>{
			if(_this.didAnimationBounds) {
			    return;
			}
			_this.didAnimationBounds = true;
			let a = animationBounds;
			let b = MapUtils.createBounds(parseFloat(a[1]),parseFloat(a[2]),parseFloat(a[3]),parseFloat(a[0]));
			_this.map.animateViewToBounds(b);
		    },pause);
		}
            }


            let currentFeatures = this.features;
            this.features = [];
            for (let i = 0; i < currentFeatures.length; i++) {
                this.addFeature(currentFeatures[i]);
            }
	    if(this.getProperty("displayEntries",true)) {
		let entries = this.getDisplayManager().collectEntries();
		for (let i = 0; i < entries.length; i++) {
                    let pair = entries[i];
                    this.handleEventEntriesChanged(pair.source, pair.entries);
		}
	    }


	    if(this.getProperty("addEntryMarkers")) {
		this.getDisplayEntry(entry=>{
		    if(!entry) {
			console.log("failed to get entry");
			return;
		    }
		    entry.getChildrenEntries(entries=>{
			entries.forEach(entry=>{
			    if(!entry.hasLocation()) return;
			    let lonlat = MapUtils.createLonLat(entry.getWest(), entry.getNorth());
			    let html = "<b>" + entry.getName()+"</b>";
			    if(entry.isImage()) {
				html+="<br>" + HU.image(entry.getImageUrl(),["width","200px"]);
			    }
			    this.map.addMarker(entry.getId(), lonlat, entry.getIconUrl(),  "", html, null, 16);
			});
		    });
		});

	    }

            if (this.layerEntries) {
                let selectCallback = function(feature,layer,event) {
                    _this.handleLayerSelect(layer);
                }
                let unselectCallback = function(feature,layer,event) {
                    _this.handleLayerUnselect(layer);
                }
                let toks = this.layerEntries.split(",");
                for (let i = 0; i < toks.length; i++) {
                    let tok = toks[i];
                    let url = RamaddaUtil.getUrl("/entry/show?output=shapefile.kml&entryid=" + tok);
                    this.map.addKMLLayer("layer", url, true, selectCallback, unselectCallback);
                    //TODO: Center on the kml
                }
            }

	    for(let markerIdx=1;true;markerIdx++) {
		let marker = this.getProperty("marker" + markerIdx);
		if(!marker) break;
		this.map.addMarkerEmbed(marker);
	    }
        },
	registerEvents:function() {
            this.getMap().getMap().events.register("zoomend", this, this.zoomendFunc = ()=>{
		if(this.callingUpdateUI) return;
		if(this.lastUpdateTime) {
		    let diff = (new Date().getTime())-this.lastUpdateTime.getTime()
		    if(diff<2000) {
			return
		    }
		}
		if(this.debugZoom) {
		    console.log("level:" + this.getMap().getMap().getZoom());
		}
		//		console.log(this.getLogLabel()+" zoomend:"+this.callingUpdateUI+" " + this.lastUpdateTime);
		if(this.pointLevelRange || this.glyphLevelRange) {
		    this.checkLevelRange([this.myFeatureLayer],true);
		}		

                this.mapBoundsChanged();
		this.checkHeatmapReload();
		this.updateHtmlLayers();
		if(!this.haveAddPoints) {
		    return;
		}

		if(this.getHandleCollisions()) {
		    if(this.lastZoom == this.map.getZoom()) {
			return;
		    }
		    //Wait a bit
		    if(this.lastCollisionTimeout) {
			clearTimeout(this.lastCollisionTimeout);
		    }

		    //		    console.log(this.getLogLabel()+" setting up collision timeout");
		    this.lastTimeout = setTimeout(()=>{
			this.haveCalledUpdateUI = false;
			//			console.log(this.getLogLabel()+" calling updateUI from handleCollisions");
			this.updateUI();
			this.lastCollisionTimeout = null;
		    },1000);
		}
		//		console.log(this.getLogLabel()+" finished zoomend");
            });
            this.map.getMap().events.register("moveend", this, this.moveendFunc = ()=> {
		if(this.map.doingPopup) return;
                this.mapBoundsChanged();
		this.checkHeatmapReload();
            });
	},
        getBounds: function() {
	    if(this.map)
		return this.map.getBounds();
	    return null;
	},
        mapFeatureSelected: function(layer) {
            if (!this.getPointData()) {
                return;
            }
            this.map.onFeatureSelect(layer);
            if (!Utils.isDefined(layer.feature.record)) {
                return;
            }
            this.propagateEventRecordSelection({
                record: layer.feature.record
            });
        },
	showVectorLayer:true,
	toggleVectorLayer: function() {
	    this.showVectorLayer = !this.showVectorLayer;
            if(this.vectorLayer != null) {
		this.vectorLayer.setVisibility(this.showVectorLayer);
	    }
	    this.haveCalledUpdateUI = false;
	    this.updateUI();
	},
        doDisplayMap: function() {
            if (!this.getShowLayers()) return false;
            if (!this.getProperty("displayAsMap", true)) return false;
            if(this.getProperty("kmlLayer") ||
	       this.getProperty("shapefileLayer") ||
	       this.getProperty("geojsonLayer") ||
	       this.displayingMapLayer) {
		if(this.getShowLayers()) {
		    return this.showVectorLayer;
		}
	    }
        },
        cloneLayer: function(layer) {
            let _this = this;
            this.map.hideLoadingImage();
            layer = layer.clone();
            let features = layer.features;
            let clonedFeatures = [];
            for (let j = 0; j < features.length; j++) {
                feature = features[j];
                feature = feature.clone();
                if (feature.style) {
                    oldStyle = feature.style;
                    feature.style = {};
                    for (let a in oldStyle) {
                        feature.style[a] = oldStyle[a];
                    }
                }
                feature.layer = layer;
                clonedFeatures.push(feature);
            }
            layer.removeAllFeatures();
            this.map.getMap().addLayer(layer);
            layer.addFeatures(clonedFeatures);
            this.vectorLayer = layer;
            this.applyVectorMap();
            this.map.addSelectCallback(layer, this.doDisplayMap(), function(feature,layer,event) {
                _this.mapFeatureSelected(layer);
            });
        },
        handleEventPointDataLoaded: function(source, pointData) {
        },
        handleEventFieldsSelected: function(source, fields) {
	    if(this.getProperty("selectedFieldIsColorBy") && fields.length>0) {
		this.colorByFieldChanged(fields[0]);
	    }
        },
        handleLayerSelect: function(layer) {
	    if(debugPopup) this.logMsg("handleLayerSelect");
            let args = this.layerSelectArgs;
            if (!this.layerSelectPath) {
                if (!args) {
                    this.map.onFeatureSelect(layer);
                    return;
                }
                //If args was defined then default to search
                this.layerSelectPath = "/search/do";
            }
            let url = RamaddaUtil.getUrl(this.layerSelectPath);
            if (args) {
                let toks = args.split(",");
                for (let i = 0; i < toks.length; i++) {
                    let tok = toks[i];
                    let toktoks = tok.split(":");
                    let urlArg = toktoks[0];
                    let layerField = toktoks[1];
                    let attrs = layer.feature.attributes;
                    let fieldValue = null;
                    for (let attr in attrs) {
                        let attrName = "" + attr;
                        if (attrName == layerField) {
                            let attrValue = null;
                            if (typeof attrs[attr] == 'object' || typeof attrs[attr] == 'Object') {
                                let o = attrs[attr];
                                attrValue = o["value"];
                            } else {
                                attrValue = attrs[attr];
                            }
                            url = HU.appendArg(url, urlArg, attrValue);
                            url = url.replace("${" + urlArg + "}", attrValue);
                        }
                    }
                }
            }
            url = HU.appendArg(url, "output", "json");
            let entryList = new EntryList(this.getRamadda(), url, null, false);
            entryList.doSearch(this);
            this.getEntryList().showMessage("Searching", HU.div([ATTR_STYLE, HU.css("margin","20px")], this.getWaitImage()));
        },
        getEntryList: function() {
            if (!this.entryListDisplay) {
                let props = {
                    showMenu: true,
                    showTitle: true,
                    showDetails: true,
                    layoutHere: false,
                    showForm: false,
                    doSearch: false,
                };
                let id = this.getUniqueId("display");
                this.entryListDisplay = new RamaddaEntrylistDisplay(this.getDisplayManager(), id, props);
                this.getDisplayManager().addDisplay(this.entryListDisplay);
            }
            return this.entryListDisplay;
        },
        entryListChanged: function(entryList) {
            let entries = entryList.getEntries();
            this.getEntryList().entryListChanged(entryList);
        },
        handleLayerUnselect: function(layer) {
            this.map.onFeatureUnselect(layer);
        },
        addMapLayer: function(source, props) {
            let _this = this;
            let entry = props.entry;
            if (!this.addedLayers) this.addedLayers = {};
            if (this.addedLayers[entry.getId()]) {
                let layer = this.addedLayers[entry.getId()];
                if (layer) {
                    this.map.removeKMLLayer(layer);
                    this.addedLayers[entry.getId()] = null;
                }
                return;
            }

            let type = entry.getType().getId();
            if (type == "geo_shapefile" || type == "geo_geojson") {
                let bounds = MapUtils.createBounds(entry.getWest(), entry.getSouth(), entry.getEast(), entry.getNorth());
                if (bounds.left < -180 || bounds.right > 180 || bounds.bottom < -90 || bounds.top > 90) {
                    bounds = null;
                }

                let selectCallback = function(feature,layer,event) {
		    if(debugPopup) this.logMsg("selectCallback");
                    _this.handleLayerSelect(layer);
                }
                let unselectCallback = function(feature,layer,event) {
                    _this.handleLayerUnselect(layer);
                }
                let layer;
                if (type == "geo_geojson") {
                    let url = entry.getRamadda().getEntryDownloadUrl(entry);
                    layer = this.map.addGeoJsonLayer(this.getProperty('geojsonLayerName','Map'), url, this.doDisplayMap(), selectCallback, unselectCallback, null, null, true);
                } else {
                    let url = RamaddaUtil.getUrl("/entry/show?output=shapefile.kml&entryid=" + entry.getId());
                    layer = this.map.addKMLLayer(entry.getName(), url, true, selectCallback, unselectCallback, null, null, true);
                }
                this.addedLayers[entry.getId()] = layer;
                return;
            }

            let baseUrl = entry.getAttributeValue("base_url");
            if (!Utils.stringDefined(baseUrl)) {
                console.log("No base url:" + entry.getId());
                return;
            }
            let layer = entry.getAttributeValue("layer_name");
            if (layer == null) {
                layer = entry.getName();
            }
            this.map.addWMSLayer(entry.getName(), baseUrl, layer, false);
        },
        mapBoundsChanged: function() {
            let bounds = this.map.getMap().calculateBounds().transform(this.map.sourceProjection,
								       this.map.displayProjection);
	    this.propagateEvent(DisplayEvent.mapBoundsChanged, {"bounds": bounds,    "force": false});
	    if(this.clipToView || this.getClipToBounds()) {
		if(this.lastUpdateTime) {
		    let now = new Date();
		    if(now.getTime()-this.lastUpdateTime.getTime()>1000) {
			this.haveCalledUpdateUI = false;
			this.clipBounds = true;
			this.updatingFromClip = true;
			this.updateUI();
		    }
		}
	    }

        },
        addFeature: function(feature) {
            this.features.push(feature);
            feature.line = this.map.addPolygon("lines_" +
					       feature.source.getId(), RecordUtil
					       .clonePoints(feature.points), null);
        },
        getContentsDiv: function() {
	    let style="";
	    if(!this.getProperty("showInnerContents",true)) {
		style+="display:none;";
	    }		

            let html =  HU.div([ATTR_STYLE,style,ATTR_CLASS, "display-inner-contents", ID,
				this.domId(ID_DISPLAY_CONTENTS)], "");
	    return html;
        },
	addHighlightMarker:function(marker) {
	    if(!this.highlightMarkers) this.highlightMarkers=[];
	    this.highlightMarkers.push(marker);
	},
	removeHighlight: function() {
	    if(this.highlightMarkers) {
		this.highlightMarkers.forEach(marker=>{
		    this.removeFeature(marker);
		});
		this.highlightMarkers = null;
	    }
	},
	highlightPoint: function(lat,lon,highlight,andCenter,dontRemove,record) {
	    if(!this.getMap()) return;
	    if(!dontRemove) {
		this.removeHighlight();
	    }
	    if(!this.getShowRecordHighlight()) return;
	    if(highlight) {
		let point = MapUtils.createLonLat(lon,lat);
                let attrs = {
                    pointRadius: parseFloat(this.getProperty("recordHighlightRadius", +this.getPropertyRadius(6)+8)),
                    stroke: true,
                    strokeColor: this.getRecordHighlightStrokeColor(),
                    strokeWidth: parseFloat(this.getProperty("recordHighlightStrokeWidth", 2)),
		    fillColor: this.getProperty("recordHighlightFillColor", "#ccc"),
		    fillOpacity: parseFloat(this.getProperty("recordHighlightFillOpacity", 0.5)),
                };
		if(this.getProperty("recordHighlightUseMarker",false)) {
		    let size = +this.getProperty("recordHighlightRadius", +this.getRadius(24));
		    this.addHighlightMarker(this.getMap().createMarker("pt-" + featureCnt, point, null, "pt-" + featureCnt,null,null,size));
		} else 	if(this.getProperty("recordHighlightVerticalLine",false)) {
		    let points = [];
                    points.push(MapUtils.createPoint(lon,0));
		    points.push(MapUtils.createPoint(lon,80));
                    this.addHighlightMarker(this.getMap().createPolygon(id, "highlight", points, attrs, null));
		} else {
		    attrs.graphicName = this.getRecordHighlightShape();
		    let markerIcon = this.getRecordHighlightIcon();
		    let marker;
		    if(markerIcon) {
			let size = this.getRecordHighlightIconSize();
			marker = this.map.createMarker("highlight", point, markerIcon, "highlight",null,null, size,null,null,attrs);
		    } else {
			marker = this.getMap().createPoint("highlight", point, attrs);
		    }
		    this.addHighlightMarker(marker);
		    
		}
		if(this.highlightMarkers) {
		    this.highlightMarkers.forEach(marker=>{
			if(record) {
			    marker.record=record;
			    marker.textGetter=this.getTextGetter();
			}
		    });
		    this.addFeatures(this.highlightMarkers);
		}
		if(andCenter && this.getCenterOnHighlight()) {
		    this.getMap().setCenter(point);
		    if(this.getZoomLevelOnHighlight()) {
			this.getMap().setZoom(this.getZoomLevelOnHighlight());
		    }
		}
	    }
	},


        handleEventEntryMouseover: function(source, args) {
            if (!this.map) {
                return;
            }
            id = args.entry.getId() + "_mouseover";
            attrs = {
                lineColor: "red",
                fillColor: "red",
                fillOpacity: 0.5,
                lineOpacity: 0.5,
                doCircle: true,
                lineWidth: 1,
                fill: true,
                circle: {
                    lineColor: "black"
                },
                polygon: {
                    lineWidth: 4,
                }
            }
            this.addOrRemoveEntryMarker(id, args.entry, true, attrs);
        },
        handleEventEntryMouseout: function(source, args) {
            if (!this.map) {
                return;
            }
            id = args.entry.getId() + "_mouseover";
            this.addOrRemoveEntryMarker(id, args.entry, false);
        },
        handleEventAreaClear: function() {
            if (!this.map) {
                return;
            }
            this.map.clearRegionSelector();
        },
	propagateFilterFields: function(record) {
	    let fields = this.getFieldsByIds(null, this.getProperty("filterFieldsToPropagate"));
	    fields.map(field=>{
		let args = {
		    fieldId:field.getId(),
		    value:record.getValue(field.getIndex())
		};
		this.propagateEvent(DisplayEvent.filterChanged, args);
	    });
	},	    
	doneLocation:false,
        handleClick: function(theMap, event, lon, lat) {
	    if(event.shiftKey && event.metaKey) {
		if(!this.doneLocation) {
		    if(Utils.isAnonymous()) {
			console.log("latitude,longitude");
		    } else {
			console.log("latitude,longitude,address,city,state,zip,country");
		    }
		    this.doneLocation = true;
		}
		let loc = lat+"," + lon;
		if(Utils.isAnonymous()) {
                    let point= this.map.addPoint("", MapUtils.createLonLat(lon, lat));
		    Utils.copyToClipboard(loc+"\n");
		    console.log(loc);
		} else  {
		    let url = RamaddaUtil.getUrl("/map/getaddress?latitude=" + lat +"&longitude=" + lon);
		    $.getJSON(url, data=>{
			if(data.length==0) {
			    console.log(loc+",,,,,");
			} else {
			    let comp = data[0];
			    let point= this.map.addPoint("", MapUtils.createLonLat(lon, lat),null,comp.address);
			    console.log(loc+"," + comp.address+"," + comp.city +"," + comp.state +"," + comp.zip +"," + comp.country); 
			}
		    }).fail(err=>{
			console.log(loc+",,,,,");
		    });
		}
		return;
	    }

	    let debug = false;
	    if(debug)   console.log("click");
	    if(this.lastFeatureSelectTime) {
		let diff = new Date().getTime()-this.lastFeatureSelectTime.getTime();
		this.lastFeatureSelectTime = null;
		if(diff<1000) {
		    if(debug)   console.log("\tclick: lastFeatureSelectTime:" + diff)
		    return;
		}
	    }

	    if(event.shiftKey) {
		if(Utils.isAnonymous()) return;
		let text = prompt("Marker text", "");
		if(!text) return;
		let url = RamaddaUtil.getUrl("/metadata/addform?entryid=" + this.getProperty("entryId")+"&metadata_type=map_marker&metadata_attr1=" +
					     encodeURIComponent(text) +"&metadata_attr2=" + lat +"," + lon); 
		window.location = url;
		if(debug) console.log("\tclick:shift");
		return
	    }

            if (!this.map) {
		if(debug)    console.log("\tclick:no map")
                return;
            }

            if (this.doDisplayMap()) {
		if(debug)    console.log("\tclick: no display map")
                return;
            }

            let justOneMarker = this.getJustOneMarker(false);
            if(justOneMarker) {
		if(debug)    console.log("\tclick: just one")
                let pointData = this.getPointData();
                if(pointData) {
                    if(pointData.handleEventMapClick(this, this, lon, lat)) {
			this.startProgress('Reloading data...');
			this.getMap().showLoadingImage();
		    }
		    this.getDisplayManager().notifyEvent("mapClick", this, {lat:lat,lon:lon});
                }
            }


	    if(!this.records) {
		if(debug)    console.log("\tclick: no records")
		return;
	    }
	    let indexObj = [];
            let closest = RecordUtil.findClosest(this.records, lon, lat, indexObj);
            if (!closest) {
		if(debug)    console.log("\tclick: no closest")
		return;
	    }
	    if(debug)    console.log("\tclick: handling")
	    this.propagateEventRecordSelection({record: closest});

	    //If we are highlighting a record then change the marker
	    if(this.highlightMarkers) {
		this.highlightPoint(closest.getLatitude(),closest.getLongitude(),true,false);
	    }
	    
	    this.propagateFilterFields(closest);
        },

        getPosition: function() {
            let lat = $("#" + this.domId(ID_LATFIELD)).val();
            let lon = $("#" + this.domId(ID_LONFIELD)).val();
            if (lat == null)
                return null;
            return [lat, lon];
        },

        haveInitBounds: false,
        setInitMapBounds: function(north, west, south, east) {
            if (!this.map) return;
            if (this.haveInitBounds) return;
	    this.lastUpdateTime = new Date();
            this.haveInitBounds = true;
	    if(this.getProperty("doInitCenter",true)) {
		this.map.centerOnMarkers(MapUtils.createBounds(west, south, east,
							       north),true);
	    }
        },

        sourceToEntries: {},
        handleEventEntriesChanged: function(source, entries) {
            if (!this.map) {
                return;
            }
            //debug
            if (source == this.lastSource) {
                this.map.clearSelectionMarker();
            }
            if ((typeof source.forMap) != "undefined" && !source.forMap) {
                return;
            }
            let oldEntries = this.sourceToEntries[source.getId()];
            if (oldEntries != null) {
                for (let i = 0; i < oldEntries.length; i++) {
                    let id = source.getId() + "_" + oldEntries[i].getId();
                    this.addOrRemoveEntryMarker(id, oldEntries[i], false);
                }
            }

            this.sourceToEntries[source.getId()] = entries;
            let markers = MapUtils.createLayerMarkers("Markers",{});
            let lines =  MapUtils.createLayerVector("Lines", {});
            let north = -90,
                west = 180,
                south = 90,
                east = -180;
            let didOne = false;
            for (let i = 0; i < entries.length; i++) {
                let entry = entries[i];
                let id = source.getId() + "_" + entry.getId();
                let mapEntryInfo = this.addOrRemoveEntryMarker(id, entries[i], true);
                if (entry.hasBounds()) {
                    if (entry.getNorth() > 90 ||
                        entry.getSouth() < -90 ||
                        entry.getEast() > 180 ||
                        entry.getWest() < -180) {
                        console.log("bad bounds on entry:" + entry.getName() + " " +
				    entry.getNorth() + " " +
				    entry.getSouth() + " " +
				    entry.getEast() + " " +
				    entry.getWest());
                        continue;
                    }

                    north = Math.max(north, entry.getNorth());
                    south = Math.min(south, entry.getSouth());
                    east = Math.max(east, entry.getEast());
                    west = Math.min(west, entry.getWest());
                    didOne = true;
                }
            }
            let bounds = (didOne ? MapUtils.createBounds(west, south, east, north) : null);
            //debug                    this.map.centerOnMarkers(bounds, true);
        },
        handleEventEntrySelection: function(source, args) {
            if (!this.map) {
                return;
            }
            let _this = this;
            let entry = args.entry;
            if (entry == null) {
                this.map.clearSelectionMarker();
                return;
            }
            let selected = args.selected;

            if (!entry.hasLocation()) {
                return;
            }
        },
        addOrRemoveEntryMarker: function(id, entry, add, args) {
	    if(!this.getAcceptEntryMarkers()) {
		return
	    }
            if (!args) {
                args = {};
            }
            let dflt = {
                lineColor: entry.lineColor,
                fillColor: entry.lineColor,
                lineWidth: entry.lineWidth,
                doCircle: false,
                doRectangle: this.showBoxes,
                fill: false,
                fillOpacity: 0.75,
                pointRadius: 12,
                polygon: {},
                circle: {}
            }
            dfltPolygon = {}
            dfltCircle = {}
            $.extend(dflt, args);
            if (!dflt.lineColor) dflt.lineColor = "blue";

            $.extend(dfltPolygon, dflt);
            if (args.polygon)
                $.extend(dfltPolygon, args.polygon);
            $.extend(dfltCircle, dflt);
            if (args.circle)
                $.extend(dfltCircle, args.circle);

            let mapEntryInfo = this.mapEntryInfos[id];
            if (!add) {
                if (mapEntryInfo != null) {
                    mapEntryInfo.removeFromMap(this.map);
                    this.mapEntryInfos[id] = null;
                }
            } else {
                if (mapEntryInfo == null) {
                    mapEntryInfo = new MapEntryInfo(entry);
                    this.mapEntryInfos[id] = mapEntryInfo;
                    if (entry.hasBounds() && dflt.doRectangle) {
                        let attrs = {};
                        mapEntryInfo.rectangle = this.map.addRectangle(id,
								       entry.getNorth(), entry.getWest(), entry
								       .getSouth(), entry.getEast(), attrs);
                    }
                    let latitude = entry.getLatitude();
                    let longitude = entry.getLongitude();
                    if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
                        return;
                    }
                    let point = MapUtils.createLonLat(longitude, latitude);

                    if (dflt.doCircle) {
                        attrs = {
                            pointRadius: dfltCircle.pointRadius,
                            stroke: true,
                            strokeColor: dfltCircle.lineColor,
                            strokeWidth: dfltCircle.lineWidth,
                            fillColor: dfltCircle.fillColor,
                            fillOpacity: dfltCircle.fillOpacity,
                            fill: dfltCircle.fill,
                        };
                        mapEntryInfo.circle = this.map.addPoint(id, point, attrs);
                    } else {
                        mapEntryInfo.marker = this.map.addMarker(id, point, entry.getIconUrl(), "", this.getEntryHtml(entry));
                    }
                    if (entry.polygon) {
                        let points = []
                        for (let i = 0; i < entry.polygon.length; i += 2) {
                            points.push(MapUtils.createPoint(entry.polygon[i + 1], entry.polygon[i]));
                        }
                        let attrs = {
                            strokeColor: dfltPolygon.lineColor,
                            strokeWidth: Utils.isDefined(dfltPolygon.lineWidth) ? dfltPolygon.lineWidth : 2
                        };
                        mapEntryInfo.polygon = this.map.addPolygon(id, entry.getName(), points, attrs, mapEntryInfo.marker);
                    }
                    let _this = this;
                    if (mapEntryInfo.marker) {
                        mapEntryInfo.marker.entry = entry;
                        mapEntryInfo.marker.ramaddaClickHandler = function(marker) {
                            _this.handleMapClick(marker);
                        };
                        if (this.handledMarkers == null) {
                            this.map.centerToMarkers();
                            this.handledMarkers = true;
                        }
                    }
                }
                return mapEntryInfo;
            }
        },
        handleMapClick: function(marker) {
            if (this.selectedMarker != null) {
                this.getDisplayManager().handleEventEntrySelection(this, {
                    entry: this.selectedMarker.entry,
                    selected: false
                });
            }
            this.getDisplayManager().handleEventEntrySelection(this, {
                entry: marker.entry,
                selected: true
            });
            this.selectedMarker = marker;
        },
	fakeLocations: function(){
	    return  this.getTheLinkFields() && this.getLinkFeature();
	},
	getTheLinkFields:function() {
	    let tmpLinkField=this.getFieldById(null,this.getProperty("linkField"));
	    let linkFields=this.getFieldsByIds(null,this.getProperty("linkFields"));	    
	    if(linkFields.length==0 && tmpLinkField!=null) linkFields=[tmpLinkField];
	    if(linkFields.length==0) linkFields=null;
	    return linkFields
	},

        applyVectorMap: function(force, textGetter, args) {
	    let debug = false;
            if (!force && this.vectorMapApplied) {
                return;
            }
	    let points = this.myPoints || this.myFeatures;
            if (!this.doDisplayMap() || !this.vectorLayer || !points) {
                return;
            }

	    if(!this.getApplyPointsToVectors()) {
		return;
	    }
	    
	    if(!args) args = {};
	    if(debug) this.logMsg("applyVectorMap");
	    if(!textGetter) textGetter  = this.textGetter;

	    let linkFields=this.getTheLinkFields();
	    let linkFeature=this.getLinkFeature();
            let features = this.vectorLayer.features.slice();
            let allFeatures = features.slice();
	    this.recordToFeature = {};
	    let debugFeatureLinking = this.getDebugFeatureLinking();
	    debug = debugFeatureLinking;
	    //	    debug=true
	    points.forEach(point=>{
		let record = point.record;
		if(!record) return;
		let feature = record.getDisplayProperty(this.getId(),"feature");
		if(feature)  this.recordToFeature[record.getId()] = feature;
	    });

	    if(linkFeature && linkFields) {
		linkFeature = linkFeature.toLowerCase();
		let recordMap = {};
		points.forEach(p=>{
		    let record = p.record;
		    if(record) {
			let tuple = record.getData();
			let value='';
			linkFields.forEach(linkField=>{
			    value+= tuple[linkField.getIndex()];
			});
			value  = value.toString().trim();
			record.linkValue = value;
			if(recordMap[value]) {
			    let other = recordMap[value];
			    //Get the highest value
			    let v1 = record.getDisplayProperty(this.getId(),'colorByValue');
			    let v2 = other.getDisplayProperty(this.getId(),'colorByValue');			    
			    if(Utils.isDefined(v1) && Utils.isDefined(v2)) {
				if(v1>v2) {
				    recordMap[value] = record;
				}
			    }

			} else {
			    recordMap[value] = record;
			}
		    }
		});
		if(debug) {
		    let values = Object.keys(recordMap);
		    console.log('map data/feature linking: data fields:' + linkFields.map(f=>{return f.getId()}));
		    if(points.length>0) {
			console.dir('all fields:' +points[0].record.fields.map(f=>{
			    return f.getId()
			}));
		    }
		    window.displayMapSamples = values;
		    console.log('to view all record values do: console.log(displayMapSamples)\nvalues 0-10:'+ values.slice(0,10));
		    console.log('looking for map feature:' + linkFeature);
		    console.log('#map features:' + features.length+
				' #records:' + points.length +
				' #unique record values:'+ values.length);



		}

		let errorCnt = 0;
		let foundCnt = 0;		
		window.displayMapMissingFeatures=[];
		features.forEach((feature,idx)=>{
		    let attrs = feature.attributes;
		    let ok = false;
		    for (let attr in attrs) {
			let _attr = String(attr).toLowerCase();
			if(linkFeature!=_attr) continue;
			ok  = true;
			let value = this.map.getAttrValue(attrs, attr);
			if(!Utils.isDefined(value)) {
			    console.log("\tno map feature attribute value");
			    continue;
			}
			value = value.toString().trim();
			feature.linkValue = value;
			record = recordMap[value];
			if(record) {
			    let v =record.getDisplayProperty(this.getId(),'colorByValue');
			    foundCnt++;
			    if(debugFeatureLinking&& foundCnt<3)
				console.log("%cfound record:" + value+": " + record.getId(),'color: green;');
			    this.recordToFeature[record.getId()] = feature;
			} else {
			    if(debugFeatureLinking) {
				window.displayMapMissingFeatures.push(attrs);
				errorCnt++;
				if(errorCnt<3) 
				    console.log("%ccould not find record with map value:" + value +":",'color: red;');
				if(errorCnt<2) { 
				    console.log('feature:',attrs);
				}
			    }
			}
		    }
		    if(!ok && idx==0) console.log("No map feature found:" + linkFeature,'attrs:',attrs);
		});
		if(debugFeatureLinking) {
		    console.log('features matched: '+ foundCnt + ' not matched:' + errorCnt);
		    if(errorCnt>0)
			console.log('to view all missing map features do: console.log(displayMapMissingFeatures)');
		    
		}
	    }



	    let j=0;
	    features.forEach((feature,idx)=>{
		feature.wasPruned = feature.pruned;
		feature.pruned = false;
		feature.newStyle=null;
		if(feature.style) {
		    feature.style.display ="inline-block";
		}
		feature.featureIndex = j++;
		feature.featureMatched = false;
		feature.pointCount = 0;
		feature.circles = [];
		if(feature.style)feature.style.balloonStyle = null;
	    });

            this.vectorMapApplied = true;

	    let maxExtent = null;
	    let doCount = this.getProperty("colorByCount",false);
	    let matchedFeatures = [];
	    let seen = {};
	    let maxCnt = -1;
	    let minCnt = -1;


	    points.forEach((point,idx)=>{
                if (point.style && point.style.display == "none") {
		    return;
		}
		let record = point.record;
                let center = point.center;
		let tmp = {index:-1,maxExtent: maxExtent};
		let matchedFeature = this.recordToFeature[record.getId()];
		if(matchedFeature) {
		    if (matchedFeature.geometry) {
			if (maxExtent === null) {
			    maxExtent = MapUtils.createBounds();
			}
			maxExtent.extend(matchedFeature.geometry.getBounds());
		    } else {
			//console.log("no geometry:" + matchedFeature.CLASS_NAME);
		    }
		}  else {
		    if(center) {
			matchedFeature = this.findContainingFeature(features, center,tmp,false);
		    }
		}
		if(!matchedFeature) {
		    return;
		}
		matchedFeature.featureMatched = true;
		record.setDisplayProperty(this.getId(),"feature",matchedFeature);
		if(!point.colorByColor && point.hasColorByValue && isNaN(point.colorByValue)) {
		    return;
		}
		maxExtent = tmp.maxExtent;
		if(!seen[matchedFeature.featureIndex]) {
		    seen[matchedFeature.featureIndex] = true;
		    matchedFeatures.push(matchedFeature); 
		}
		matchedFeature.circles.push(point);
		matchedFeature.record = record;
		matchedFeature.textGetter=textGetter;
		if(doCount) {
		    matchedFeature.pointCount++;
		    maxCnt = maxCnt==-1?matchedFeature.pointCount:Math.max(maxCnt, matchedFeature.pointCount);
		    minCnt = minCnt==-1?matchedFeature.pointCount:Math.min(minCnt, matchedFeature.pointCount);
		} else {
		    if(tmp.index>=0)
			features.splice(tmp.index, 1);
		}
	    });


	    if(!doCount) {
		for(let i=0;i<matchedFeatures.length;i++) {
		    let matchedFeature = matchedFeatures[i];
		    style = matchedFeature.style;
		    if (!style) style = {
			"stylename": "from display",
		    };
		    delete style.display;
		    let newStyle = {};
		    $.extend(newStyle,style);
		    let theCircle;
		    let max=0;
		    matchedFeature.circles.forEach((c,idx)=>{
			if(idx==0 || c.colorByValue>max) {
			    max = c.colorByValue;
			    theCircle=c;
			}
		    });
		    if(!theCircle) theCircle=matchedFeature.circles[0]
		    $.extend(newStyle, theCircle.style);
		    matchedFeature.newStyle=newStyle;
		    matchedFeature.popupText = theCircle.text;
		    matchedFeature.dataIndex = i;
		}
	    }


	    let strokeWidth = this.getVectorLayerStrokeWidth();
	    let strokeColor = this.getVectorLayerStrokeColor();
	    let fillOpacity  =this.getVectorLayerFillOpacity();

	    let prune = this.getPruneFeatures();
	    if(doCount) {
		let colors = this.getColorTable(true);
		if (colors == null) {
		    colors = Utils.ColorTables.grayscale.colors;
		}
		let range = maxCnt-minCnt;
		let labelSuffix = this.getProperty("doCountLabel","points");
		for (let j = 0; j < features.length; j++) {
		    let feature = features[j];
		    let percent = range==0?0:(feature.pointCount - minCnt) /range;
                    let index = parseInt(percent * colors.length);
                    if (index >= colors.length) index = colors.length - 1;
                    else if (index < 0) index = 0;
		    let color= colors[index];
		    let style = feature.style;
		    if (!style) style = {
			"stylename": "from display",
		    };
		    let newStyle = {};
		    $.extend(newStyle,style);
		    $.extend(newStyle,{
			fillColor: color,
			"fillOpacity": fillOpacity,
			"strokeWidth": strokeWidth,
			"strokeColor": strokeColor,
		    });

		    if(feature.pointCount==0) {
			//TODO: what to do with no count features?
			if(prune === true) {
			    newStyle.display = "none";
			}
		    }
		    feature.newStyle = newStyle;
		    feature.dataIndex = j;
		    feature.popupText = HU.div([],feature.pointCount +SPACE + labelSuffix);
		}
		this.displayColorTable(colors, ID_COLORTABLE, minCnt,maxCnt,{});
	    } else {
		if(prune) {
		    for (let i = 0; i < features.length; i++) {
			let feature = features[i];
			if(feature.featureMatched) {
			    continue;
			}
			let style = feature.style;
			if (!style) style = {
			    "stylename": "from display"
			};
			let newStyle = {};
			$.extend(newStyle,style);
			newStyle.display= "none";
			feature.pruned = true;
			feature.newStyle = newStyle;
		    }
		}
	    }



	    let redrawCnt = 0;
	    allFeatures.forEach((feature,idx)=>{
		if(!feature.newStyle) feature.newStyle={};
		if(feature.wasPruned && !feature.pruned ||
		   !feature.style ||
		   (feature.newStyle.display && 
		    feature.newStyle.display=="none" &&
		    feature.newStyle.display!=feature.style.display) ||
		   feature.style.fillColor!=feature.newStyle.fillColor) {
		    feature.style = feature.newStyle;
		    feature.style.strokeWidth=strokeWidth;
		    feature.style.strokeColor=strokeColor;		    
		    feature.style.fillOpacity=fillOpacity;

		    if(!feature.style.fillColor) {
			feature.style.fillColor = "rgba(230,230,230,0.5)";
			feature.style.strokeColor = "rgba(200,200,200,0.5)";
		    }
		    redrawCnt++;
		    this.vectorLayer.drawFeature(feature);
		    
		}
		feature.newStyle = null;
	    });

	    
	    /** TODO?
		if (!args.dontSetBounds && maxExtent && !this.hadInitialPosition && this.getCenterOnFilterChange(true)) {
		this.map.zoomToExtent(maxExtent, true);
		}
		if(!this.getProperty("fixedPosition",false))  {
		this.hadInitialPosition    = false;
		}
	    */


	    if(!this.hadInitialPosition && !this.applyMapVectorZoom) {
		this.applyMapVectorZoom = true;
		if(!this.hadUrlArgumentMapCenter && !this.hadUrlArgumentZoom &&
		   this.getProperty("doInitCenter",true)) {
		    this.map.zoomToLayer(this.vectorLayer);
		}
	    }




        },
	findContainingFeature: function(features, center, info,debug) {
	    //	    debug=true;
	    let matchedFeature = null;
            for (let j = 0; j < features.length; j++) {
                let feature = features[j];
                let geometry = feature.geometry;
                if (!geometry) {
		    if(debug) console.log("\tno geometry")
                    continue;
                }
                bounds = geometry.getBounds();
                if (!bounds.contains(center.x, center.y)) {
		    //		    if(debug) console.log("\tnot in bounds:" + bounds)
                    continue;
                }
		if(debug) console.log("\tfindContainingFeature:" + center.x+" " + center.y);
                if (geometry.components) {
		    if(debug) console.log("\thas components:" +geometry.components.length);
                    geometry.components.every(comp=> {
                        bounds = comp.getBounds();
                        if (!bounds.contains(center.x, center.y)) {
			    if(debug) console.log("\t\tnot contain:" + bounds + " " + comp.CLASS_NAME);
			    return true;
                        }
			if(!comp.containsPoint) {
			    if(debug) console.log("\t\tunknown geometry:" + comp.CLASS_NAME);
			    return true;
			}
			if(debug) console.log("\t\tcontains:" + comp.containsPoint(center));
                        if (comp.containsPoint(center)) {
                            matchedFeature = feature;
			    if (feature.geometry) {
				if (info.maxExtent === null) {
				    info.maxExtent = MapUtils.createBounds();
				}
				info.maxExtent.extend(feature.geometry.getBounds());
			    }
                            info.index = j;
			    return false;
                        }
			return true;
                    });
		}
		if(matchedFeature) return matchedFeature;
                if (!geometry.containsPoint) {
                    if(debug && !geometry.components) 
			console.log("unknown geometry:" + geometry.CLASS_NAME);
                    continue;
                }
                if (geometry.containsPoint(center)) {
		    if (info.maxExtent === null) {
			info.maxExtent = MapUtils.createBounds();
		    }
		    info.maxExtent.extend(geometry.getBounds());
                    matchedFeature = feature;
                    info.index = j;
                    break;
                }
	    }
	    return matchedFeature;
	},
        needsData: function() {
            return true;
        },
	animationStart:function(animation) {
	    if(this.myFeatures) {
                for (let i = 0; i < this.myFeatures.length; i++) {
                    let point = this.myFeatures[i];
                    point.style.display = 'none';
                }
	    }
            if (this.map.circles)
                this.map.circles.redraw();

	},
        handleDateRangeChanged: function(source, prop) {
	    this.getAnimation().setDateRange(prop.minDate, prop.maxDate);
	    this.applyDateRange();
	},
       	animationApply: function(animation) {
	    SUPER.animationApply.call(this,animation,true);
	    this.applyDateRange();
	},
       	applyDateRange: function() {
	    let animation = this.getAnimation();
            let windowStart = animation.begin.getTime();
            let windowEnd = animation.end.getTime();
            let atLoc = {};
	    if(this.myFeatures) {
		for (let i = 0; i < this.myFeatures.length; i++) {
                    let point = this.myFeatures[i];
                    if (point.date < windowStart || point.date > windowEnd) {
			point.style.display = 'none';
			continue;
                    }
                    if (atLoc[point.location]) {
			let other = atLoc[point.location];
			if (other.date < point.date) {
                            atLoc[point.location] = point;
                            other.style.display = 'none';
                            point.style.display = 'inline';
			} else {
                            point.style.display = 'none';
			}
			continue;
                    }
                    atLoc[point.location] = point;
                    point.style.display = 'inline';
		}
	    }

	    this.applyToFeatureLayers(layer=>{layer.redraw();});
            this.applyVectorMap(true, this.textGetter);
	},
        showAllPoints: function() {
	    if(this.myFeatures) {
		for (let i = 0; i < this.myFeatures.length; i++) {
		    let line = this.myFeatures[i];
		    line.style.display = 'inline';
		}
		if (this.map.lines)
		    this.map.lines.redraw();
	    }
	    this.applyToFeatureLayers(layer=>{layer.redraw();});
            this.applyVectorMap(true);
        },

	colorByFieldChanged:function(field) {
	    this.haveCalledUpdateUI = false;
	    this.setProperty("colorBy", field);
	    this.vectorMapApplied  = false;
	    this.updateUI({fieldChanged:true});
	},
	sizeByFieldChanged:function(field) {
	    this.haveCalledUpdateUI = false;
	    this.setProperty("sizeBy", field);
	    this.vectorMapApplied  = false;
	    this.updateUI({fieldChanged:true});
	},
	dataFilterChanged: function(args) {
	    if(!args) args = {};
	    this.vectorMapApplied  = false;
	    this.updateUI({source:args.source, dataFilterChanged:true, dontSetBounds:true,  reload:true,callback: (records)=>{
		if(args.source=="animation") return;
		if(!this.getCenterOnFilterChange()) return;
		if(this.getShowPoints() && records && records.length) {
		    //If we have our own features then just zoom to that layer and return
		    if(this.myFeatureLayer?.features?.length) {
			this.map.zoomToLayer(this.myFeatureLayer);
			return
		    }
		    this.map.centerOnMarkers(null, false, true);
		    return;
		}
		if (this.vectorLayer && this.showVectorLayer) {
		    this.map.zoomToLayer(this.vectorLayer,1.2);
		} else if(this.lastImageLayer) {
		    this.map.zoomToLayer(this.lastImageLayer);
		} else {
		    //true -> Just markers
		    if(records && records.length)
			this.map.centerOnMarkers(null, false, true);
		}
	    }});
	},
	requiresGeoLocation: function() {
	    if(this.fakeLocations()) return false;
	    if(this.shapesField && this.shapesTypeField) return false;
	    if((this.getLinkFields()||this.getProperty("linkField")) && this.getLinkFeature()) return false;
	    return true;
	},
	addFilters: function(filters) {
	    SUPER.addFilters.call(this, filters);
	    if(this.getShowBoundsFilter()) {
		filters.push(new BoundsFilter(this));
	    }
	},
	getHeader2:function() {
	    let html = SUPER.getHeader2.call(this);
	    if(this.getProperty("showClipToBounds")) {
		this.clipToView=false;
		html =  HU.div([ATTR_STYLE,HU.css("display","inline-block","cursor","pointer","padding","1px","border","1px solid rgba(0,0,0,0)"), ATTR_TITLE,"Clip to view", ID,this.domId("clip")],HU.getIconImage("fa-map"))+SPACE2+ html;
	    }


	    if(this.getProperty("showMarkersToggle")) {
		let dflt = this.getProperty("markersVisibility", true);
		html += HU.checkbox(this.domId("showMarkersToggle"),[ATTR_ID,this.domId("showMarkersToggle")],dflt,
				    this.getProperty("showMarkersToggleLabel","Show Markers")) +SPACE2;
	    }

	    if(this.getShowBaseLayersSelect()) {
		html+=this.getBaseLayersSelect();
	    }

	    if(this.getProperty("showVectorLayerToggle",false)) {
		html += HU.checkbox("",[ATTR_ID,this.domId("showVectorLayerToggle")],!this.showVectorLayer) +" " +
		    this.getProperty("showVectorLayerToggleLabel","Show Points") +SPACE4;
	    }
	    html += HU.div([ATTR_CLASS,CLASS_HEADER_SPAN,ATTR_ID,this.domId("locations")]);
	    return html;
	},
	locationMenuCnt:0,
	addLocationMenu:function(url,data) {
	    let html = "";
	    let idx = this.locationMenuCnt++;
	    let label = data.label || data.name;
	    if(!label) {
		label = Utils.makeLabel(url.replace(/^.*[\\\/]/, '').replace(/\.[^\.]+$/,"").replace("_"," "));
	    }
	    html += HU.div([ATTR_CLASS,"ramadda-menu-button ramadda-clickable ramadda-map-button bold",
			    ATTR_ID,this.domId("location_" + idx)],"View " + (label)) +SPACE;
	    this.map.appendToolbar(html);
	    //	    this.jq("locations").append(html);
	    let _this = this;
	    this.jq("location_" + idx).click(function() {
		let inner = "";
		let locations = [];
		if(data.features) {
		    data.features.forEach(feature=>{
			let name = feature.properties.NAME || feature.properties.name;
			locations.push({name:name,geometry:feature.geometry});
		    });
		} else {
		    locations = data.locations;
		}
		locations.sort((a,b)=>{
		    return a.name.localeCompare(b.name);
		});
		locations.forEach((loc,idx)=>{
		    if(Utils.isDefined(loc.latitude)) {
			inner+=HU.div([ATTR_CLASS,"ramadda-clickable ramadda-hoverable display-map-location",
				       "latitude",loc.latitude,
				       "longitude",loc.longitude], loc.name);
		    } else if(Utils.isDefined(loc.north)) {
			inner+=HU.div([ATTR_CLASS,"ramadda-clickable ramadda-hoverable display-map-location", "north",loc.north,"west",loc.west,"south",loc.south,"east",loc.east], loc.name);

		    } else if(Utils.isDefined(loc.geometry)) {
			inner+=HU.div([ATTR_CLASS,"ramadda-clickable ramadda-hoverable display-map-location", "index",idx], loc.name);
		    }
		});
		inner = HU.div([ATTR_ID,_this.domId("locationmenu"),ATTR_STYLE,HU.css("max-height","200px","overflow-y","auto","padding","5px")],inner);
		let dialog = HU.makeDialog({content:inner,my:"left top",at:"left bottom",anchor:$(this),draggable:false,header:false});
		_this.jq("locationmenu").find(".ramadda-clickable").click(function() {
		    if(_this.locationFeatures) {
			_this.locationFeatures.forEach(feature=>{
			    _this.map.getHighlightLinesLayer().removeFeatures([feature]);
			});
		    }
		    _this.locationFeatures = [];
		    if($(this).attr("longitude")) {
			let point = MapUtils.createLonLat(+$(this).attr("longitude"),+$(this).attr("latitude"));
			_this.map.zoomTo(9);
			_this.map.setCenter(point);
		    } else if($(this).attr("north")) {
			_this.map.setViewToBounds(new RamaddaBounds(+$(this).attr("north"),+$(this).attr("west"),+$(this).attr("south"),+$(this).attr("east")));
		    } else {
			let geometry = locations[$(this).attr("index")].geometry;
			let type = geometry.type;
			let shapes = geometry.coordinates;
			let style = {
			    strokeColor: "blue",
			    fillColor:'rgba(0,0,255,0.10)',
			    strokeWidth: 2};
			if(type=="MultiPolygon") {
			    for(let i=0;i<shapes.length;i++) {
				let c2 = shapes[i];
				for(let j=0;j<c2.length;j++) {
				    _this.locationFeatures.push(_this.createFeature(c2[j],null,null,style));
				}
			    }
			} else if(type=="Polygon") {
			    for(let i=0;i<shapes.length;i++) {
				_this.locationFeatures.push(_this.createFeature(shapes[i],null,null,style));
			    }
			} else {
			    console.log("Unknown geometry:" + type);
			}
			_this.map.centerOnFeatures(_this.locationFeatures);
		    }
		    dialog.remove();
		});
	    });
	},
	initHeader2:function() {
            SUPER.initHeader2.call(this);

	    let _this = this;
	    this.initBaseLayersSelect();


	    if(this.getProperty('showTogglePath',false)) {
		let cbx = HU.checkbox('',[ATTR_ID,this.domId('togglepath')],this.getIsPath(),
				      'Show track');
		this.jq(ID_HEADER2_PREPREFIX).append(HU.span([ATTR_STYLE,'padding-right:10px;'],cbx));
		this.jq('togglepath').click(function() {
		    let on = $(this).is(':checked');
		    _this.setProperty('isPath',on);
		    _this.setProperty('showPoints',!on);			
		    _this.haveCalledUpdateUI = false;
		    _this.updateUI({fieldChanged:true});
		});

	    }

	    this.getProperty("locations","").split(",").forEach(url=>{
		url  =url.trim();
		if(url.length==0) return;
		let show = url.startsWith("show:");
		if(show) url = url.substring("show:".length);
		if(!url.startsWith("/") && !url.startsWith("http")) {
		    url = RamaddaUtil.getCdnUrl("/resources/" +url);			
		}
		if(url.endsWith('geojson')) {
		    this.map.addGeoJsonLayer('location layer', url, false, null, null, {}, null);
		    return;
		}
		let success = (data) =>{
		    data=JSON.parse(data);
		    this.addLocationMenu(url, data);
		    if(show) {
			let attrs = {
			    pointRadius:this.getProperty('locationRadius',4),
			    fillColor:this.getProperty('locationFillColor','blue'),
			    strokeWidth:this.getProperty('locationStrokeWidth',0),
			    strokeColor:this.getProperty('locationStrokeColor','blue'),			    
			}

			data.locations.forEach(loc=>{
			    let point = MapUtils.createLonLat(loc.longitude, loc.latitude);
			    this.map.addPoint('', point,attrs,loc.name);
			});
		    }
		};
		let fail = err=>{console.log("Error loading location json:" + url+"\n" + err);}
		Utils.doFetch(url, success,fail,null);	    
	    });


	    this.jq("showMarkersToggle").change(function() {
		let visible = $(this).is(':checked');
		_this.applyToFeatureLayers(layer=>{layer.setVisibility(visible);})
	    });
	    this.jq("showVectorLayerToggle").change(function() {
		_this.toggleVectorLayer();
	    });
	    
	    this.jq("clip").click(function(e){
		_this.clipToView = !_this.clipToView;
		if(!_this.clipToView) {
		    $(this).css("border","1px solid rgba(0,0,0,0)");
		} else {
		    $(this).css("border","1px solid #aaa");
		}
		_this.haveCalledUpdateUI = false;
		_this.updateUI();
	    });
	},

	handleNoData: function(pointData,reload) {
	    this.jq(ID_PAGE_COUNT).html("");
            this.addPoints([],[],[]);
	    this.setMessage(this.getNoDataMessage());

	},
	createFeature:function(polygon,record, textGetter, style){
	    if(!style) {
		if(this.baseStyle) {
		    this.baseStyle = $.extend({}, MapUtils.getVectorStyle('default'));
		    $.extend(this.baseStyle,{
			strokeColor: this.getProperty("vectorLayerStrokeColor","#000"),
			fillColor:this.getProperty("vectorLayerFillColor","#ccc"),
			fillOpacity:this.getProperty("vectorLayerFillOpacity",0.10),
			strokeWidth: this.getProperty("vectorLayerStrokeWidth",1),
			cursor:'pointer'
		    });
		}
		style = this.baseStyle;
	    }


	    let sitePoints = [];
	    polygon.forEach(pair=>{
		let point = MapUtils.createPoint(pair[0],pair[1]);
		let projPoint = this.map.transformLLPoint(point);
		sitePoints.push(projPoint);
	    });
	    let linearRing = MapUtils.createLinearRing(sitePoints);
	    let geometry = MapUtils.createPolygon([linearRing]);
	    let polygonFeature = MapUtils.createVector(geometry, null, style);
	    this.map.getHighlightLinesLayer().addFeatures([polygonFeature]);
	    polygonFeature.record = record;
	    polygonFeature.textGetter = textGetter;
	    return polygonFeature;
	},
	loadShapes: function(records) {
	    if(this.coordinateFeatures) {
		this.map.getHighlightLinesLayer().removeFeatures(this.coordinateFeatures);
	    }
	    let textGetter = (f)=>{
		if(f.record) {
                    return  this.getRecordHtml(f.record, null, this.getTooltip('${default}'));
		}
		return "NONE";
	    };
	    this.coordinateFeatures = [];
	    records.forEach((r,idx)=>{
		let type = r.getValue(this.shapesTypeField.getIndex());		
		let shapesString= r.getValue(this.shapesField.getIndex());
		let shapes = JSON.parse(shapesString);
		if(type=="MultiPolygon") {
		    for(let i=0;i<shapes.length;i++) {
			let c2 = shapes[i];
			for(let j=0;j<c2.length;j++) {
			    this.coordinateFeatures.push(this.createFeature(c2[j],r,textGetter));
			}
		    }
		} else if(type=="Polygon") {
		    for(let i=0;i<shapes.length;i++) {
			this.coordinateFeatures.push(this.createFeature(shapes[i],r,textGetter));
		    }
		} else {
		    console.log("Unknown geometry:" + type);
		}
	    });

	},	    
	toggleTrack:function(record,item) {
	    let marker = this.markers?this.markers[record.getId()]:null;
	    if(this.tracks[record.getId()]) {
		if(item)item.removeClass("display-map-toc-item-on");
		this.map.removePolygon(this.tracks[record.getId()]);
		this.tracks[record.getId()] = null;
		if(item) {
		    item.attr(ATTR_TITLE,"Click to view; Double-click to view track");
		}
	    } else {
		if(item) {
		    item.addClass("display-map-toc-item-on");
		    item.attr(ATTR_TITLE,"Click to view; Double-click to remove track");
		}
		let url = record.getValue(this.trackUrlField.getIndex());
		if(url!="")
		    $.getJSON(url, data=>{this.loadTrack(record, data)}).fail(err=>{console.log("url failed:" + url +"\n" + err)});
	    }
	    this.map.setCenter(MapUtils.createLonLat(record.getLongitude(),record.getLatitude()));
	},
	makeToc:function(records) {
	    let urlField  =  this.getFieldById(null,this.getProperty("urlField"));
	    let fields = this.getFieldsByIds(null,this.getTocFields(this.getProperty("labelField","name")));
	    if(!fields || fields.length==0) fields = this.getFieldsByType(null,"string");
	    let template = this.getTocTemplate();
	    let html='';
	    let title = this.getTocTitle(this.getProperty('tableOfContentsTitle',''));
	    if(template) {
		let clazz = "ramadda-clickable  display-map-toc-item ramadda-noselect";
		records.forEach((record,idx)=>{
		    let label = this.applyRecordTemplate(record,this.getDataValues(record),null, template);
		    html+=HU.div([ATTR_CLASS,clazz,RECORD_INDEX,idx], label);
		});
	    } else   if(fields && fields.length>0) {
		let iconField = this.getFieldById(null, this.getProperty("iconField"));
		let doTable=this.getProperty('tocTable',true);
		if(doTable) {
		    html+='<table  width=100%><tr>'
		    if(urlField) html+='<td></td>';
		    fields.forEach(f=>{
			html+='<td style=\'font-weight:bold;\'>' + f.getLabel() +'</td>';
		    });
		    html+='</tr>';
		}
		let clazz = "ramadda-clickable  display-map-toc-item ramadda-noselect";
		records.forEach((record,idx)=>{
		    let title = "View record";
		    if(this.trackUrlField) title = "Click to view; Double-click to view track";
		    let values=[];
		    values.push(...fields.map(f=>{
			return f.getValue(record);
		    }));

		    let value;
		    if(doTable) {
			let width = Math.floor(100/values.length)+'%';
			value = Utils.wrap(values,HU.open('td',[ATTR_CLASS,clazz,RECORD_ID,record.getId(),RECORD_INDEX,idx,'x'+ATTR_WIDTH,width]),'</td>');
			if(urlField) {
			    let url = urlField.getValue(record);
			    if(Utils.stringDefined(url)) {
				value = HU.td([ATTR_STYLE,HU.css('width','10px')],
					      HU.href(url,HU.getIconImage('fas fa-link',null,[ATTR_STYLE,'font-size:8pt;']),['target','_link']))+value;
			    }
			}
			html+=HU.tag('tr',[], value);
		    } else {
			value = Utils.join(values,' ');
			if(!iconField) {
			    clazz+=" ramadda-nav-list-link ";
			} else {
			    value = HU.getIconImage(iconField.getValue(record,icon_blank16),["width",16]) + SPACE + value;
			}
			if(urlField) {

			    let url = urlField.getValue(record);
			    if(Utils.stringDefined(url)) {
				value=HU.href(url,HU.getIconImage('fas fa-link',null,[ATTR_STYLE,'font-size:8pt;']),['target','_link']) + HU.space(1) +value;
			    }
			}
			html+= HU.span([ATTR_TITLE, title, ATTR_CLASS,clazz,RECORD_ID,record.getId(),RECORD_INDEX,idx], value);
		    }
		});
		if(doTable) {
		    html+='</table>';
		}
	    }

	    if(html) {
		let height = this.getProperty('height', this.getProperty('mapHeight', 300));
		height='calc(' +HU.getDimension(height)+' - 1em)';
		let style = HU.css('height',height,'max-height',height,'overflow-y','auto');
		if(this.getTocWidth()) {
		    style+=HU.css("min-width",this.getTocWidth());
		}
		html = HU.div([ATTR_CLASS, "display-map-toc",ATTR_STYLE,style,ATTR_ID, this.domId("toc")],html);
		if(title) html = HU.center(HU.b(title)) + html;
		this.jq(ID_LEFT).html(html);
		let _this = this;
		let items = this.jq(ID_LEFT).find(".display-map-toc-item");
		if(this.getProperty('showTableOfContentsTooltip')) 
		    this.makeTooltips(items,records);
		items.click(function() {
		    let idx = $(this).attr(RECORD_INDEX);
		    let record = records[idx];
		    if(!record) return;
		    _this.highlightPoint(record.getLatitude(), record.getLongitude(),true, false,false,record);
		    _this.map.setCenter(MapUtils.createLonLat(record.getLongitude(),record.getLatitude()));
		    if(_this.getProperty("tocZoom")) {
		    	_this.map.setZoom(_this.getProperty("tocZoom"));
		    }

		    if(record.trackData) {
			setTimeout(()=>{
			    _this.getDisplayManager().notifyEvent("dataSelection", _this, {data:record.trackData});
			},100);
		    }
		});

		items.dblclick(function() {
		    _this.removeHighlight();
		    let idx = $(this).attr(RECORD_INDEX);
		    let record = records[idx];
		    if(!record) return;
		    if(_this.trackUrlField) {
			let url = record.getValue(_this.trackUrlField.getIndex());
			if(url && url.length>0) {
			    _this.toggleTrack(record,$(this));
			    return;
			}
		    } 
		    _this.map.setCenter(MapUtils.createLonLat(record.getLongitude(),record.getLatitude()));
		});
	    }
	},	    

        updateUI: function(args) {
	    if(!args) args={};
	    let debug = false;
	    this.lastUpdateTime = null;
            SUPER.updateUI.call(this,args);
	    //	    console.log("map.updateUI: " + !this.getDisplayReady() +" " + !this.hasData() +" " +!this.getProperty("showData", true));
            if (this.haveCalledUpdateUI || !this.getDisplayReady() ||!this.hasData() || !this.getProperty("showData", true)) {
		if(debug) console.log("map.updateUI have called:" + this.haveCalledUI +" ready:" + this.getDisplayReady() +" has data:" + this.hasData() +" showData:" +this.getProperty("showData", true));
                return;
            }


	    if(this.updateUICallback) {
		clearTimeout(this.updateUICallback);
		this.updateUICallback = null;
	    }
            let pointData = this.getPointData();
	    this.lastZoom = this.map?this.map.getZoom():null;

	    //Set the shapes Fields here before filter data so we can accept non georeferenced data
	    this.shapesField = this.getFieldById(null,this.getProperty("shapesField"));
	    this.shapesTypeField = this.getFieldById(null,this.getProperty("shapesTypeField"));
	    this.trackUrlField  =  this.getFieldById(null,this.getProperty("trackUrlField"));
            let records = this.records =  this.filterData();
	    if(this.shapesTypeField && this.shapesField) {
		this.setProperty("tooltipNotFields",this.shapesTypeField.getId()+"," + this.shapesField);
		this.loadShapes(records);
	    }

	    if(debug) console.log("displaymap.updateUI reload=" +args.reload);
            if (records == null) {
		if(debug) console.log("\tno data");
                return;
            }



	    if(this.getSkipZero()) {
		records = records.filter(record=>{
		    return !(record.getLatitude()==0 && record.getLongitude()==0);
		});
	    }


	    if(this.getShowTableOfContents(false)) {
		this.makeToc(records);
	    }

	    if(!this.updatingFromClip) {
		//stop the flash
		if(args.source!="animation") {
		    this.setMessage(args.dataFilterChanged|| args.fieldChanged|| args.reload?"Reloading map...":"Creating map...");
		}
	    }
	    this.updatingFromClip = false;

	    //	    setTimeout(()=>{
	    try {
		if(displayDebug.initMap) this.logMsg("calling updateUIInner",true);
		this.callingUpdateUI = true;
		this.updateUIInner(args, pointData, records,debug);
		this.callingUpdateUI = false;
		if(this.getCenterOnMarkersAfterUpdate()) {
		    this.map.centerOnMarkers();
		}
		if(displayDebug.initMap) this.logMsg("done calling updateUIInner",true);
		if(args.callback)args.callback(records);
		this.clearProgress();
		if(!this.layerVisible) {
		    setTimeout(()=>{this.setVisible(false);},50);
		}
	    } catch(exc) {
		this.callingUpdateUI = false;
		console.log(exc)
		console.log(exc.stack);
		this.setMessage("Error:" + exc);
	    }

	    this.notifyExternalDisplay();

	    this.setIsFinished();
	    //	    });

	},
	notifyExternalDisplay:function() {
	    let externalDisplay = this.getProperty("externalDisplay");
	    if(externalDisplay) {
		externalDisplay.externalDisplayReady(this);
	    }
	},
	filterDataPhase2:function(records) {
	    records = SUPER.filterDataPhase2.call(this,records);
	    if(this.clipBounds || this.clipToView) {
		let bounds = RecordUtil.getBounds(records);
		this.clipBounds = false;
		let clipRecords = false;
		if(!this.lastPointBounds || (this.lastPointBounds && this.lastPointBounds!=bounds)) {
		    clipRecords = true;
		}
		this.lastPointBounds = bounds;
		if(this.clipToView || clipRecords) {
		    let viewbounds = this.map.getMap().calculateBounds().transform(this.map.sourceProjection, this.map.displayProjection);
		    let tmpRecords =records.filter(r=>{
			return viewbounds.containsLonLat(MapUtils.createLonLat(r.getLongitude(),r.getLatitude()));
		    });
		    //		    console.log("clipped records:" + tmpRecords.length);
		    records = tmpRecords;
		}
	    }
	    return records;
	},


	updateUIInner: function(args, pointData, records, debug) {
	    let _this = this;
	    let t1= new Date();
	    debug = debug || displayDebug.displayMapUpdateUI;
	    if(debug) console.log("displaymap.updateUIInner:" + records.length);
	    this.haveCalledUpdateUI = true;
	    let header='';
	    if(this.getShowCollisionToggle()) {
		header+=HU.span([ATTR_TITLE,'Toggle showing collisions',
				 ATTR_CLASS,'display-header-item'],
				HU.checkbox('',[ATTR_ID,this.domId('collisiontoggle')],
					    !this.getHandleCollisions(),
					    'Show all points'));
	    }


	    if(this.getShowRegionSelector()) {
		let label = this.getRegionSelectorLabel() ?? HU.getIconImage("fa-globe-americas");
		
		header+= HU.span([ATTR_CLASS,"display-header-span ramadda-menu-button ramadda-clickable",  ATTR_TITLE,"Select region", ID,this.domId("selectregion")],label)+SPACE2;

	    }


	    if(Utils.stringDefined(header)) {
		this.writeHeader(ID_HEADER2_PREFIX,header);
	    }

	    this.jq('collisiontoggle').change(function(){
		let on = $(this).is(':checked');
		_this.setProperty('handleCollisions',!on);
		_this.haveCalledUpdateUI = false;
		_this.updateUI();
		
	    });
	    this.jq("selectregion").click(function() {
		_this.initRegionsSelector($(this));
	    });

	    if(!this.getProperty("makeDisplay",true)) {
		return;
	    }
	    if(!this.fullBounds) {
		this.fullBounds = {};
		RecordUtil.getPoints(this.getRecords(), this.fullBounds);
	    }
            let pointBounds = {};
	    let haveLinkedFeature  = this.getTheLinkFields() && this.getLinkFeature();
	    if(!haveLinkedFeature && this.getFilterBadLocations(true)) {
		records = records.filter(r=>{
		    return r.getLatitude()>=-90 && r.getLatitude()<=90 &&
			r.getLongitude()>=-180 &&
			r.getLongitude()<=180;
		});
	    }


            let points = RecordUtil.getPoints(records, pointBounds);
            let fields = pointData.getRecordFields();
            let showSegments = this.getShowSegments(false);
	    let okToSetMapBounds = !showSegments && !this.hadInitialPosition && !args.dontSetBounds && 
		(!args.dataFilterChanged || this.getCenterOnFilterChange());
	    let haveRecords = records.length>0;
	    if(this.getInitBoundsUseAllRecords()) {
		pointBounds = {};
		let allRecords = this.getData().getRecords();
		RecordUtil.getPoints(allRecords, pointBounds);
		haveRecords=allRecords.length>0;
	    }
	    if(haveRecords) {
		if (!isNaN(pointBounds.north)) {
		    let padding = this.getInitBoundsPadding();
		    if(padding) {
			let w = pointBounds.east-pointBounds.west;
			pointBounds.east-=w*padding;
			pointBounds.west-=w*padding;			
			let h = pointBounds.north-pointBounds.south;
			pointBounds.north+=h*padding;
			pointBounds.south-=h*padding;			
		    }
		    this.pointBounds = pointBounds;
		    this.initBounds = pointBounds;
		    if(okToSetMapBounds) {

			if(pointBounds.insideDateLine) {
			    this.setInitMapBounds(pointBounds.north, -178, pointBounds.south, -170);
			} else {
			    this.setInitMapBounds(pointBounds.north, pointBounds.west, pointBounds.south, pointBounds.east);
			}
		    }
		}
	    }
	    if (this.map == null) {
		return;
	    }
	    if(this.highlightMarkers) {
		this.highlightMarkers.forEach(marker=>{
		    this.map.removePoint(marker);
		    this.map.removeMarker(marker);
		});
		this.highlightMarkers = null;
	    }

	    this.map.clearSeenMarkers();
	    let t2= new Date();
	    //	    debug = true;
	    if(debug) console.log("displaymap calling addPoints");
	    //Add in to handle when there is no geolocation on the record
	    //	    if(points.length==0 &&  haveLinkedFeature) {
	    if(points.length==0) {
		this.records.forEach(record=>{
		    points.push({x:-105,y:40});
		});
	    }
            this.addPoints(records,fields,points,pointBounds,debug);
	    let t3= new Date();
            this.addLabels(records,fields);
            this.applyVectorMap(true, this.textGetter,args);
	    let t4= new Date();
	    if(debug) Utils.displayTimes("time pts=" + points.length,[t1,t2,t3,t4], true);
	    this.lastUpdateTime = new Date();
	},
	xcnt:0,
	heatmapCnt:0,
	animationApply: function(animation, skipUpdateUI) {
 	    if(!this.heatmapLayers || !this.getHeatmapVisible()) {
		SUPER.animationApply.call(this, animation, skipUpdateUI);
		return;
	    }
	    let onDate=null;
	    //	    console.log("displaymap.animationApply:" + animation.begin + " " +animation.end);
	    let onLayer = null;
	    let offLayers = [];
	    this.heatmapLayers.every(layer=>{
		if(!layer.date) return true;
		if(layer.date.getTime()>= animation.begin.getTime() && layer.date.getTime()<= animation.end.getTime()) {
		    onDate = layer.date;
		    onLayer = layer;
		    layer.setVisibility(true);
		} else {
		    if(layer.getVisibility()) 
			offLayers.push(layer);
		}
		return true;
	    })
	    offLayers.forEach(layer=>{
		layer.setVisibility(false);
	    });
 	    if(!onDate) {
		SUPER.animationApply.call(this, animation, skipUpdateUI);
	    }
	    if(onLayer!=null)
		this.setMapLabel(onLayer.heatmapLabel);
	},
        setDateRange: function(min, max) {
	    //Not sure why we do this
	    if(this.getDoGridPoints(false)|| this.getDoHeatmap(false)) {
		SUPER.setDateRange.call(this, min,max);
	    } else {
		SUPER.setDateRange.call(this, min,max);
	    }
	},
	showColorTable: function(colorBy) {
	    colorBy.displayColorTable(null,true);
	    this.callingUpdateSize = true;
	    //for now don't do this as it takes a long time
	    //	    this.map.getMap().updateSize();
	    this.callingUpdateSize = false;
	},

	applyHeatmapAnimation: function(index) {
 	    if(!this.heatmapLayers || !this.getHeatmapVisible())
		return

	    this.jq(ID_HEATMAP_ANIM_LIST)[0].selectedIndex = index;
	    let offLayers = [];
	    this.heatmapLayers.forEach((layer,idx)=>{
		if(index==idx)
		    layer.setVisibility(true);
		else
		    offLayers.push(layer);
	    });
	    offLayers.forEach(layer=>{
		layer.setVisibility(false);
	    });
	    this.setMapLabel(this.heatmapLayers[index].heatmapLabel);
	},
	stepHeatmapAnimation: function(delta){
	    console.log('step');
 	    if(!this.heatmapLayers || !this.getHeatmapVisible())
		return

	    console.log('step2');

	    let index = this.jq(ID_HEATMAP_ANIM_LIST)[0].selectedIndex;
	    index+=delta;
	    if(index<0) {
		index = 0;
	    }
	    if(index>=this.heatmapLayers.length) {
		index =0;
	    }
	    this.applyHeatmapAnimation(index);
	    if(this.heatmapPlayingAnimation) {
		setTimeout(()=>{
		    this.stepHeatmapAnimation(1);
		},this.getHmAnimationSleep(1000));
	    }
	},
	checkHeatmapReload:function() {
	    //	    return
	    if(!this.getHmReloadOnZoom(this.getReloadOnZoom(false))) return;
	    let now = new Date ();
	    //Don't do this the first couple of seconds after we've been created
	    if(now.getTime()-this.createTime.getTime()<3000) return;
	    let diff = 0;
	    if(this.checkHeatmapReloadTime) {
		diff = now.getTime()-this.checkHeatmapReloadTime.getTime();
	    }
	    this.checkHeatmapReloadTime = now;
	    if(diff<1000) {
		if(!this.checkHeatmapReloadPending) {
		    this.checkHeatmapReloadPending = true;
		    setTimeout(()=>{
			this.checkHeatmapReloadPending = false;
			this.checkHeatmapReload();
		    },1100)
		}
		return;
	    }
	    this.checkHeatmapReloadTime = null;
	    this.reloadHeatmap = true;
	    this.haveCalledUpdateUI = false;
	    this.updateUI();
	},
	createHeatmap(records, fields, bounds) {
	    let debug = displayDebug.displayMapCreateMap;
	    //	    debug = true;
	    if(debug) console.log("createHeatmap");
	    let colorBy = this.getColorByInfo(records, null,null,null,["hmColorBy","colorBy",""]);
	    let angleBy = this.getColorByInfo(records, "angleBy",null,null,["hmAngleBy","angleBy",""]);
	    let lengthBy = this.getColorByInfo(records, "lengthBy",null,null,["hmLengthBy","lengthBy",""]);
	    if(!angleBy.isEnabled()) angleBy = colorBy;
	    if(!lengthBy.isEnabled()) lengthBy=null;
	    records = records || this.filterData();
	    if(this.getHmBounds()) {
		let toks = this.getHmBounds().split(",");
		bounds = new RamaddaBounds(+toks[0],+toks[1], +toks[2],+toks[3]);
	    }
	    let mapBounds = this.map.getBounds();
	    bounds = bounds ||  RecordUtil.getBounds(records);
	    bounds = RecordUtil.convertBounds(mapBounds);
	    if(this.pointBounds) bounds = this.pointBounds;
	    if(debug) {
		console.dir(bounds.north,bounds.west,bounds.south,bounds.east);
	    }


	    this.removeExtraLayers();
	    this.heatmapLayers = [];
	    this.extraLayers = [];	    
	    if(records.length==0) {
		this.errorMessage = this.getNoDataMessage();
		this.setMessage(this.errorMessage);
		return
	    }
	    if(this.reloadHeatmap) {
		this.reloadHeatmap = false;
		bounds = new RamaddaBounds(this.map.getBounds());
		records = RecordUtil.subset(records, bounds);
		//		bounds =  RecordUtil.getBounds(records);
	    }
	    bounds = RecordUtil.expandBounds(bounds,this.getProperty("boundsScale",0.05));

	    let dfltArgs = this.getDefaultGridByArgs();
	    let ratio = (bounds.east-bounds.west)/(bounds.north-bounds.south);
	    let w = Math.round(this.getProperty("gridWidth",800));
	    let h = Math.round(w/ratio);
	    let groupByField = this.getFieldById(null,this.getHmGroupBy());
	    let groupByDate = this.getHmGroupByDate();
	    if(debug) console.log('calling groupBy group by date:' + groupByDate +' group by field:' + groupByField);
	    let t1 = new Date();
	    let groups = (groupByField || groupByDate)?RecordUtil.groupBy(records, this, groupByDate, groupByField):null;
	    let t2 = new Date();
	    //	    Utils.displayTimes("make groups",[t1,t2],true);
	    if(debug) console.log("\tdone calling groupBy");
	    if(groups == null || groups.max == 0) {
		doTimes = false;
		groups= {
		    max:records.length,
		    values:["none"],
		    map:{none:records}
		}
	    }
	    //	    if(debug)	console.dir('groups:',groups);

	    //	    if(debug) console.log("\tdone calling groupBy count="+ groups.values.length);
	    let recordCnt = groups.max;
 	    if(dfltArgs.cellSize==0) {
		let sqrt = Math.sqrt(recordCnt);
		let size = Math.round(w/sqrt);
		dfltArgs.cellSizeX = dfltArgs.cellSizeY = dfltArgs.cellSize = size;
	    } else if(String(dfltArgs.cellSize).endsWith("%")) {
		dfltArgs.cellSize =dfltArgs.cellSizeX =  dfltArgs.cellSizeY = Math.floor(parseFloat(dfltArgs.cellSize.substring(0,dfltArgs.cellSize.length-1))/100*w);
	    }
	    let args =$.extend({colorBy:colorBy,angleBy:angleBy,lengthBy:lengthBy,w:w,h:h,bounds:bounds,forMercator:true},
			       dfltArgs);
	    if(debug) {
		console.log("#records:" + records.length+" dim:" + w +" " +h + " #records:" + records.length +" cell:" + dfltArgs.cellSizeX + " #records:" + records.length +" bounds:" + bounds);
	    }
	    let labels = [];
	    let labelPrefix = this.getHmLabelPrefix("${field}-");
	    groups.values.forEach((value,idx)=>{
		let recordsAtTime = groups.map[value];
		if(debug && idx<5)
		    console.log("group:" + value +" #:" + groups.map[value].length);

		let img = Gfx.gridData(this.getId(),fields, recordsAtTime,args);
		let label = value=="none"?"Heatmap": labelPrefix +" " +groups.labels[idx];
		label = label.replace("${field}",colorBy.field?colorBy.field.getLabel():"");
		labels.push(label);
		let layer = this.map.addImageLayer("heatmap"+(this.heatmapCnt++), label, "", img, idx==0, bounds.north, bounds.west, bounds.south, bounds.east,w,h, { 
		    isBaseLayer: false,
		});
		//For now don't set the layer index since it places this layer too high
		//		this.map.getMap().setLayerIndex(layer, 1000);
		layer.heatmapLabel = label;
		if(groupByDate) {
		    if(value.getTime)
			layer.date = value;
		}
		if(!this.getHeatmapVisible()) layer.setVisibility(false);
		this.extraLayers.push(layer);
		this.heatmapLayers.push(layer);
	    });
	    if(this.getHmShowGroups(true) && this.heatmapLayers.length>1 && !this.getAnimationEnabled()) {
		this.heatmapPlayingAnimation = false;
		let controls =  [];
		controls.push(HU.div([ATTR_ID,this.domId(ID_HEATMAP_ANIM_STEP_BACK),ATTR_STYLE,HU.css("display","inline-block"),ATTR_TITLE,"Step back"],
 				     HU.getIconImage("fa-step-backward",[ATTR_CLASS,"display-anim-button"])));

		if(!groupByField) 
		    controls.push(HU.div([ATTR_ID,this.domId(ID_HEATMAP_ANIM_PLAY),ATTR_STYLE,HU.css("display","inline-block"),ATTR_TITLE,"Play/Stop Animation"],
					 HU.getIconImage("fa-play",[ATTR_CLASS,"display-anim-button"])));
		controls.push(HU.div([ATTR_ID,this.domId(ID_HEATMAP_ANIM_STEP_FORWARD),ATTR_STYLE,HU.css("display","inline-block"),ATTR_TITLE,"Step forward"],
 				     HU.getIconImage("fa-step-forward",[ATTR_CLASS,"display-anim-button"])));
		

		controls.push(HU.div([ATTR_STYLE,HU.css("display","inline-block","margin-left","5px","margin-right","5px")], HU.select("",[ATTR_ID,this.domId(ID_HEATMAP_ANIM_LIST)],labels)));
		this.writeHeader(ID_HEADER2_PREPREFIX, Utils.join(controls,"&nbsp;&nbsp;"));
		let _this = this;
		this.jq(ID_HEATMAP_ANIM_LIST).change(function() {
		    let index = $(this)[0].selectedIndex;
		    _this.applyHeatmapAnimation(index);
		});
		this.jq(ID_HEATMAP_ANIM_PLAY).click(function() {
		    _this.heatmapPlayingAnimation = !_this.heatmapPlayingAnimation;
		    let icon = _this.heatmapPlayingAnimation?"fa-stop":"fa-play";
		    $(this).html(HU.getIconImage(icon,[ATTR_CLASS, "display-anim-button"]));
		    if(_this.heatmapPlayingAnimation) {
			_this.stepHeatmapAnimation(1);
		    }
		});
		this.jq(ID_HEATMAP_ANIM_STEP_FORWARD).click(function() {
		    _this.heatmapPlayingAnimation = false;
		    let icon = _this.heatmapPlayingAnimation?"fa-stop":"fa-play";
		    _this.jq(ID_HEATMAP_ANIM_PLAY).html(HU.getIconImage(icon,[ATTR_CLASS,"display-anim-button"]));
		    _this.stepHeatmapAnimation(1);
		});
		this.jq(ID_HEATMAP_ANIM_STEP_BACK).click(function() {
		    _this.heatmapPlayingAnimation = false;
		    let icon = _this.heatmapPlayingAnimation?"fa-stop":"fa-play";
		    _this.jq(ID_HEATMAP_ANIM_PLAY).html(HU.getIconImage(icon,[ATTR_CLASS,"display-anim-button"]));
		    _this.stepHeatmapAnimation(-1);
		});		

	    }
	    if(groups.values[0]!="none") {
		this.setMapLabel(labels[0]);
	    }
	    this.showColorTable(colorBy);


	    if(this.getHmShowToggle(this.getProperty("hm.showToggle")) || this.getHmShowReload()) {
		let cbx = this.jq(ID_HEATMAP_TOGGLE);
		let reload =  HU.getIconImage("fa-sync",[ATTR_CLASS,"display-anim-button",ATTR_TITLE,"Reload heatmap", ID,this.domId("heatmapreload")])+SPACE2;
		this.heatmapVisible= cbx.length==0 ||cbx.is(':checked');

		let toggle = reload +
		    HU.checkbox("",[ATTR_ID,this.domId(ID_HEATMAP_TOGGLE)],this.heatmapVisible,
				this.getHmToggleLabel(this.getProperty('hm.toggleLabel','Heatmap')));
		this.writeHeader(ID_HEADER2_PREFIX,HU.span([ATTR_STYLE,HU.css('margin-right:8px;')],toggle));


		let _this = this;
		this.jq('heatmapreload').click(()=> {
		    this.reloadHeatmap = true;
		    this.removeExtraLayers();
		    this.haveCalledUpdateUI = false;
		    this.updateUI();
		});
		this.jq(ID_HEATMAP_TOGGLE).change(()=>{
		    if(_this.heatmapLayers)  {
			let visible = _this.getHeatmapVisible();
			_this.heatmapVisible  = visible;
			_this.heatmapLayers.forEach(layer=>layer.setVisibility(visible));
			_this.map.setPointsVisibility(!visible);
		    }
		});
	    }
	},

	getHeatmapVisible:function() {
	    let toggle = this.jq(ID_HEATMAP_TOGGLE);
	    return toggle.length==0 || toggle.is(':checked');
	},
	updateHtmlLayers: function() {
	    if(this.htmlLayerInfo) {
		this.createHtmlLayer(this.htmlLayerInfo.records, this.htmlLayerInfo.fields);
	    }
	},
	updateHtmlLayer:function() {
	    if(!this.htmlLayer) return;
	    if(!this.htmlLayerId) {
		this.htmlLayerId =this.getUniqueId(ID_HTMLLAYER);
		this.htmlPopupLayerId =this.getUniqueId('popup');
		this.jq(ID_MAP).append(HU.div([ATTR_ID,this.htmlPopupLayerId,ATTR_STYLE,HU.css('position','absolute','width','100%','left','0px','top','0px','bottom','0px','xbackground','red','z-index','0','pointer-events', 'none')]));
		let vp  = this.map.getMap().getViewport();
		vp = $(vp).children()[0];
		$(vp).css('display','relative');
		$(vp).append(HU.div([ATTR_STYLE,'z-index:10',ATTR_CLASS,'display-map-htmllayer', ATTR_ID,this.htmlLayerId]));
	    }
	    if(this.htmlPopupLayerId) {
		//		jqid(this.htmlPopupLayerId).html(HU.div([ATTR_STYLE,'position:absolute;top:50px;left:100px'],'xxxx'));
		jqid(this.htmlPopupLayerId).html(this.htmlPopup);
	    }
	    $('#'+ this.htmlLayerId).html(HU.div([ATTR_STYLE,'position:relative;'],this.htmlLayer));
	    


	},
        createHtmlLayer: function(records, fields) {
	    let _this = this;
	    _this.htmlLayerMouseOver = null;
	    let htmlLayerField = this.getFieldById(fields,this.getHtmlLayerField());
	    this.htmlLayerInfo = {
		records:records,
		fields:fields,
	    };
	    this.htmlLayer = '';
	    this.htmlPopup = '';
	    let fillColor = this.getFillColor("#619FCA");
	    let strokeColor = this.getStrokeColor("#888");
	    let popupLabelField = this.getFieldById(fields, this.getHtmlLayerPopupLabelField());
	    let flipYAxis = this.getHtmlLayerFlipYAxis();
	    let drawAxisLabels = this.getHtmlLayerDrawAxisLabels();	    	    
	    let drawPopupAxisLabels = this.getHtmlLayerPopupDrawAxisLabels();	    	    
	    let scaleAll = this.getHtmlLayerScaleWithAll(true);	    

	    let w = this.getHtmlLayerWidth(30);
	    let h = this.getHtmlLayerHeight(15);
	    let shape = this.getHtmlLayerShape("barchart");
	    if(shape=="barchart")
		this.setProperty("colorBy",htmlLayerField.getId());
	    if(this.getHtmlLayerScale()) {
		let zooms = [];		
		this.getHtmlLayerScale().split(",").forEach(t=>{
		    zooms.push(t.split(":"));
		});
		//3:0.5,4:1,5:2
		let zoom = this.map.map.getZoom();
		let scale = 1.0;
		if(zooms.length==1 && zooms[0].length==1) {
		    scale=zooms[0][0];
		} else {
		    zooms.every(t=>{
			scale=t[1];
			if(t[0] >= zoom) {
			    return false;
			}
			return true;
		    });
		}
		w*=scale;h*=scale;
	    }
	    let style = this.getHtmlLayerStyle("");
	    let infos = [];
	    let allData = this.getColumnValues(records, htmlLayerField);
	    let groups = RecordUtil.groupBy(records, this, false,"latlon");
	    let container = $($(this.map.getMap().getViewport()).children()[0]);
	    let cleft = +container.css("left").replace("px","");
	    let ctop = +container.css("top").replace("px","");
	    let hoverW = w*3;
	    let hoverH = h*3;
	    let layerRecords = [];

	    groups.values.forEach((value,idx)=>{
		let recordsAtTime = groups.map[value];
		let data = [];
		layerRecords.push(recordsAtTime[0]);
		recordsAtTime.forEach((r,idx)=>{
		    data.push(r.getValue(htmlLayerField.getIndex()));
		});
		let record = recordsAtTime[0];
		let px = this.map.getMap().getPixelFromLonLat(this.map.transformLLPoint(MapUtils.createLonLat(record.getLongitude(),record.getLatitude())));
		let id = this.getId() +"_sl"+ idx;
		let hid = id +"_hover";
		let html = 
		    HU.div([ATTR_ID,id,  ATTR_CLASS,'display-map-html-item',
			    ATTR_STYLE,style +HU.css('line-height','0px','z-index','1000','position','absolute','left', (px.x-w/2-cleft) +'px','top', (px.y-h/2-ctop)+'px')]);
		let label = '';
		if(record && popupLabelField) {
		    label = popupLabelField.getValue(record);
		}
		if(Utils.stringDefined(label)) {
		    label  = HU.div([ATTR_STYLE,'white-space:nowrap;position:absolute;font-size:8pt;top:25px;left:10px;'],label);
		}
		this.htmlPopup +=
		    HU.div([ATTR_ID,hid, RECORD_INDEX, idx,
			    ATTR_TITLE,"", ATTR_CLASS,'display-map-html-hitem',
			    ATTR_STYLE,style +HU.css('display','none','line-height','0px','z-index','2001','position','absolute','xleft', (px.x-hoverW/2-cleft) +'px','left','0px','top','0px','xtop', (px.y-hoverH/2-ctop)+'px')],label);
		this.htmlLayer += html;
		infos.push({
		    id:id,
		    hoverId: hid,
		    data:data,
		    min:Utils.getMin(data),
		    max:Utils.getMax(data),		    		    
		    records: recordsAtTime
		});
	    });
	    this.updateHtmlLayer();
            let colorBy = this.getColorByInfo(records);
	    infos.forEach((info,idx)=>{
		if(shape == "pie" || shape == "piechart") {
		    [0,1].forEach((cid,idx)=>{
			let id = HU.getUniqueId("pie");
			let cw = idx==0?w:hoverW;
			let ch = idx==0?h:hoverH;
			let pie = HU.tag(TAG_CANVAS,[ATTR_STYLE,HU.css('cursor','pointer'),
						     ATTR_ID,id ,ATTR_WIDTH,cw,ATTR_HEIGHT, ch]);
			if(idx==0)
			    $("#" + info.id).html(pie);
			else
			    $("#" + info.hoverId).html(pie);
			let canvas = document.getElementById(id);
			let color = colorBy&& colorBy.isEnabled()?colorBy.getColor(info.data[0]):fillColor;
			let ctx = canvas.getContext("2d");
			if(idx==1) {
			    ctx.fillStyle= '#fff';
			    ctx.beginPath();
			    ctx.moveTo(cw/2,ch/2);
			    ctx.arc(cw/2,ch/2, cw/2-2, 0-Math.PI/2, 2*Math.PI);
			    ctx.closePath();
			    ctx.fill();
			}
			ctx.beginPath();
			ctx.moveTo(cw/2,ch/2);
			ctx.arc(cw/2,ch/2, cw/2-2, 0-Math.PI/2, info.data[0]*2 * Math.PI-Math.PI/2);
			ctx.lineTo(cw/2,ch/2);
			ctx.closePath();
			ctx.strokeStyle= strokeColor;
			ctx.fillStyle= color;
			ctx.fill();
			ctx.stroke();
			ctx.beginPath();
			ctx.arc(cw/2,ch/2, cw/2-2, 0, 2 * Math.PI);
			ctx.closePath();
			ctx.stroke();
		    });
		} else {
		    let props1 = {
			flipYAxis:flipYAxis,
			drawAxisLabels:drawAxisLabels,
			//drawAxis:false
		    };
		    let props2 = {
			flipYAxis:flipYAxis,
			drawAxisLabels:drawAxisLabels||drawPopupAxisLabels,
		    }

		    let min = allData.min;
		    let max = allData.max;		    
		    if(!scaleAll) {
			min = info.min;
			max = info.max;
		    }
		    min = this.getHtmlLayerMin(min);
		    max = this.getHtmlLayerMax(max);		    
		    drawSparkline(this,"#"+ info.id,w,h,info.data,info.records,min,max,colorBy,props1);
		    $('#' + info.hoverId).css('background','#fff').css('border','1px solid #ccc');
		    drawSparkline(this,"#"+ info.hoverId,hoverW,hoverH,info.data,info.records,min,max,colorBy,props2);
		}
	    });
	    let items = this.find(".display-map-html-item");			
	    let hitems = this.find(".display-map-html-hitem");
	    this.makeTooltips(hitems, layerRecords);
	    
	    items.mouseenter(function() {
		//		$(this).css('display','none');
		hitems.hide();
		let popup = 	$('#'+$(this).attr(ID)+'_hover');
		popup.show();
		//		$('#'+$(this).attr(ID)+'_hover').fadeIn(1000);
	    });
	    items.mouseleave(function() {
		hitems.hide();
	    });
	    hitems.mouseleave(function() {
		$('#'+ $(this).attr(ID).replace('_hover','')).css('display','block');
		$(this).css('display','none');
	    });
	    if(colorBy.hasField()) {
		this.showColorTable(colorBy);
	    }
	},


        makeVoronoi: function(records, fields, points,bounds) {
	    if(!MapUtils.loadTurf(()=>{this.makeVoronoi(records, fields, points,bounds);})) {
		return;
	    }

	    let pad;
	    //pad the bounds
	    if(pad = this.getVoronoiPadding(0)) {
		let  w = bounds.east-bounds.west;
		let  h = bounds.north-bounds.south;		
		let b = $.extend({},bounds);
		b.west = Math.max(-180, b.west-w*pad);
		b.east = Math.min(180,b.east+w*pad);
		b.north=  Math.min(90,b.north+h*pad);
		b.south = Math.max(-90,b.south-h*pad);				
		bounds = b;
	    }

	    let options = {
		bbox: this.makeTurfBounds(bounds)
	    };
	    let geojsonPoints = [];
	    let dups = {};
	    let vpoints = [];
	    points.forEach((p,idx)=>{
		let record = records[idx];
		let key =p.x+'_'+p.y;
		if(dups[key]) return;
		dups[key] = true;
		geojsonPoints.push(turf.point([p.x,p.y]));
		vpoints.push({point:p,record:record});
	    });
	    let collection = turf.featureCollection(geojsonPoints);
	    //	    let points = turf.randomPoint(1000, options);
	    let  voronoiPolygons= turf.voronoi(collection, options);
	    this.voronoiLayer =this.createGeoJsonLayer('Voronoi',voronoiPolygons,this.voronoiLayer);
            let colorBy = this.getColorByInfo(records,'voronoiColorBy',null,null,null,this.lastColorBy);
	    this.lastColorBy = colorBy;
	    let textGetter = this.getTextGetter(fields);
	    let style = {
		strokeColor:this.getVoronoiStrokeColor(),
		strokeWidth:this.getVoronoiStrokeWidth(),
		strokeDashstyle:this.getVoronoiStrokeDashstyle(),
		fillColor:this.getVoronoiFillColor(),
		fillOpacity:this.getVoronoiFillOpacity()		
	    }
	    this.voronoiLayer.features.forEach((f,idx)=>{
		let record = vpoints[idx].record;
		let s =Utils.clone({},style);
		if(colorBy.isEnabled()) {
		    s.fillColor= colorBy.getColorFromRecord(record);
		}
		f.style=s;
		f.record  =record;
		f.textGetter  = textGetter;
	    });
	    this.voronoiLayer.redraw();
            if (colorBy.isEnabled()) {
		colorBy.displayColorTable();
	    }
	    this.notifyExternalDisplay();

	},

        makeHexmap: function(records, fields, points,bounds) {
	    if(!MapUtils.loadTurf(()=>{this.makeHexmap(records, fields, points,bounds);})) {
		return;
	    }
	    let bbox =  this.makeTurfBounds(this.getHexmapUseFullBounds()?this.fullBounds:bounds,this.getHexmapPadding());
	    let cellSide = this.getHexmapCellSide();
	    let options = {units: this.getHexmapUnits()};
	    let hexgrid;
	    let mapType = this.getProperty('mapType');
	    if(this.getDoTrianglemap() || mapType=='triangle') {
		hexgrid = turf.triangleGrid(bbox, cellSide, options);
	    } else if(this.getDoSquaremap() || mapType=='square') {
		hexgrid = turf.squareGrid(bbox, cellSide, options);
	    } else {
		hexgrid = turf.hexGrid(bbox, cellSide, options);
	    }
	    let vpoints = [];
	    let yes= 0, no=0;


	    points.forEach((p,idx)=>{
		let record = records[idx];
		let pts = turf.points([[p.x,p.y]]);
		let isIn = false;
		hexgrid.features.every((feature,fidx)=>{
		    if(!feature.searchWithin) {
			let coords = feature.geometry.coordinates[0];
			feature.searchWithin = turf.polygon([coords]);
			feature.bbox = turf.bbox(feature.searchWithin);
			feature.records=[];
		    }
		    if(p.x>=feature.bbox[0] && p.x<= feature.bbox[2] &&
		       p.y>=feature.bbox[1] && p.y<=feature.bbox[3]) {
			let ptsWithin = turf.pointsWithinPolygon(pts,feature.searchWithin);
			if(ptsWithin.features.length>0) {
			    isIn = true;
			    feature.records.push(record);
			    return false;
			}
		    }
		    return true;
		});
		if(isIn) yes++;
		else no++;
	    });
	    let maxCount = -1,minCount=-1;
	    hexgrid.features.forEach((feature,fidx)=>{
		if(feature.records && feature.records.length>0) {
		    maxCount=maxCount==-1?feature.records.length:Math.max(maxCount, feature.records.length);
		    minCount=minCount==-1?feature.records.length:Math.min(minCount, feature.records.length);		    
		}
	    });
	    this.hexmapLayer =this.createGeoJsonLayer('Hexmap',hexgrid,this.hexmapLayer);
            let colorBy = this.getColorByInfo(records,'hexmapColorBy',null,null,null,
					      this.lastColorBy,
					      {isString:this.getHexmapUseEnum(),
					       colorTableProperty:'hexmapColorTable',
					       minValue:this.getHexmapMinValue(),
					       maxValue:this.getHexmapMaxValue()});
	    if(this.getHexmapUseEnum()) {
		colorBy.setDoEnum(true);
	    } else  if(this.getHexmapUseCount(this.getProperty('hexmapShowCount'))) {
		colorBy.setDoCount(minCount,maxCount);
	    } else {
		let doTotal = this.getHexmapShowTotal();
		colorBy.setDoTotal(doTotal);
		//recalculate the color ranges based on the average value
		let minValue = NaN;
		let maxValue = NaN;		
		this.hexmapLayer.features.forEach((f,idx)=>{
		    let records=hexgrid.features[idx].records;
		    if(!records || records.length==0) return;
		    let value = doTotal?
			colorBy.doTotal(records):
			colorBy.doAverage(records);
		    minValue = Utils.min(minValue,value);
		    maxValue = Utils.max(maxValue,value);		    
		});
		colorBy.setRange(minValue,maxValue, true);
	    }
	    this.lastColorBy = colorBy;
	    let textGetter = this.getTextGetter(fields,true);
	    let style = {
		strokeColor:this.getHexmapStrokeColor(),
		strokeDashstyle:this.getHexmapStrokeDashstyle(),
		strokeWidth:this.getHexmapStrokeWidth(),
		fillColor:this.getHexmapFillColor('transparent'),
		fillOpacity:this.getHexmapFillOpacity()				
	    }
	    let emptyStyle = {
		strokeColor:this.getHexmapEmptyStrokeColor(style.strokeColor),
		strokeWidth:this.getHexmapEmptyStrokeWidth(style.strokeWidth),
		fillColor:this.getHexmapEmptyFillColor(style.fillColor),
		fillOpacity:this.getHexmapFillOpacity()						
	    }
	    let baseStyle = {
		pointRadius:0,
		fontSize:this.getProperty('hexmapLabelFontSize',6),
		fontColor:this.getProperty('hexmapLabelFontColor','#000'),
		fontWeight: this.getProperty('hexmapLabelFontWeight','bold')
	    }

	    let labelTemplate =	 this.getHexmapLabelTemplate();
	    this.hexmapLayer.features.forEach((f,idx)=>{
		let records=hexgrid.features[idx].records;
		let s = (records && records.length>0)?style:emptyStyle;
		s = Utils.clone({},s);
		if(records && records.length>0) {
		    if(colorBy.isEnabled()) {
			s.fillColor= colorBy.getColorFromRecord(records,null,null,null);
			f.colorByValue=colorBy.lastValue;
			if(isNaN(colorBy.lastValue)) {
			    s.display='none';
			}
		    }
		    if(labelTemplate) {
			let l = labelTemplate.replace('${count}',records.length);
			l= l.replace('${colorbyvalue}',f.colorByValue)
			s =Utils.clone({},baseStyle,s);
			s.label = l;
		    }
		} 
		
		f.records  =records;
		f.style=s;
		f.textGetter  = textGetter;
	    });
	    this.hexmapLayer.redraw();
            if (colorBy.isEnabled()) {
		//		colorBy.displayColorTable();
		this.displayColorTable(colorBy, ID_COLORTABLE,colorBy.minValue,colorBy.maxValue);
	    }

	    this.notifyExternalDisplay();	    



	},
	


        addPoints: function(records, fields, points,bounds,debug) {
	    let mapType = this.getProperty('mapType');
	    if(mapType=='voronoi' || this.getProperty('doVoronoi',false)) {
		this.makeVoronoi(records,fields,points,bounds);
		if(!this.getShowPoints())
		    return;
	    }
	    if(mapType=='hex' ||
	       mapType=='triangle' ||
	       mapType=='square' ||
	       this.getDoHexmap() ||
	       this.getDoTrianglemap() ||
	       this.getDoSquaremap()) {
		this.makeHexmap(records,fields,points,bounds);
		if(!this.getShowPoints())
		    return;
	    }	    


	    if(this.getDoGridPoints()|| this.getDoHeatmap(false)) {
		let showMarkers = this.showMarkers;
		if(this.getHmShowPoints() || this.getShowPoints()) {
		    let show = true;
		    if(this.jq("showMarkersToggle").length>0) {
			show = this.jq("showMarkersToggle").is(":checked");
		    }
		    if(show) {
			this.createPoints(records, fields, points, bounds,debug);
			if(this.getHmShowToggle(false) && this.map.circles) {
			    this.map.setPointsVisibility(false);
			}
		    }
		}

		this.createHeatmap(records, fields, bounds);
		return;
	    }
	    if(this.getHtmlLayerField()) {
		this.createHtmlLayer(records, fields);
		if(!this.getShowPoints(false)) 
		    return;
	    }
	    if(debugMapTime)
		console.time('createPoints');
	    this.createPoints(records, fields, points, bounds,debug);
	    if(debugMapTime) {
		console.timeEnd('createPoints');
		console.log('#points:' + records.length);
	    }
	},
        createPoints: function(records, fields, points,bounds, debug) {
	    debug = debug ||displayDebug.displayMapAddPoints;
	    let _this = this;
	    let debugTimes  = false;
	    let features = [];
	    let featuresToAdd = [];
	    let pointsToAdd = [];
	    let linesToAdd = [];	    	    
	    //getColorByInfo: function(records, prop,colorByMapProp, defaultColorTable,propPrefix) {
	    let hideMissingColor = this.getHideMissingColor();
            let colorBy = this.getColorByInfo(records,null,null,null,null,this.lastColorBy);
	    let hideNaN = this.getHideNaN();

	    this.lastColorBy = colorBy;
	    let cidx=0
	    let polygonField = this.getFieldById(fields, this.getPolygonField());
	    let polygonColorTable = this.getColorTable(true, "polygonColorTable",null);
	    let latlon = this.getProperty("latlon",true);
            let source = this;
            let radius = +this.getPropertyRadius(8);
	    let highlightRecords = this.getFilterHighlight();
	    let unhighlightFillColor = this.getUnhighlightColor();
	    let unhighlightStrokeWidth = this.getProperty("unhighlightStrokeWidth",0);
	    let unhighlightStrokeColor = this.getProperty("unhighlightStrokeColor","#aaa");
	    let unhighlightRadius = this.getUnhighlightRadius();
	    let strokeOpacity = this.getStrokeOpacity();
	    this.markers = {};

	    //change the order of the records if we are highlighting
	    if(highlightRecords) {
		let tmpRecords = [];
		let tmpPoints = [];		
		records.forEach((record,idx)=>{
		    if(!record.isHighlight(this)) {
			tmpRecords.push(record);
			tmpPoints.push(points[idx]);
		    }
		});
		records.forEach((record,idx)=>{
		    if(record.isHighlight(this)) {
			tmpRecords.push(record);
			tmpPoints.push(points[idx]);
		    }
		});

		records = tmpRecords;
		points = tmpPoints;
	    }



	    let radiusList = this.getRadiusList();
	    let numLocs = 0;
	    if(this.getScaleRadius()|| radiusList) {
		let seen ={};
		points.every(p=>{
		    let key = p.x+"_"+p.y;
		    if(!seen[key]) {
			numLocs++;
			seen[key] = true;
		    }
		    return true;
		});
	    }
	    if(this.getScaleRadius()) {
		let minRadius = this.getScaleRadiusMin();
		let maxRadius=this.getScaleRadiusMax();
		let maxLocs = this.getScaleRadiusMaxPoints();
		let perc = Math.min(1.0,numLocs/maxLocs);
		radius = Math.max(1,Math.round(maxRadius-(maxRadius-minRadius)*perc));
	    }
	    if(radiusList) {
		Utils.split(radiusList,',',true,true).every(tuple=>{
		    let pair = Utils.split(tuple,':',true,true);
		    if(pair.length!=2) return true;
		    let cnt =+pair[0];
		    if(numLocs<=cnt) {
			radius = +pair[1];
			return false;
		    }
		    return true;
		});
	    }


	    radius = Math.min(radius, this.getMaxRadius());
            let strokeWidth = +this.getPropertyStrokeWidth();
            let strokeColor = this.getPropertyStrokeColor();
            let isTrajectory = this.getDisplayProp(source, "isTrajectory", false);
            if (isTrajectory) {
		let tpoints = points.map(p=>{
		    return $.extend({},p);
		});
                let attrs = {
                    strokeWidth: this.getProperty('pathStrokeWidth',
						  this.getProperty("strokeWidth",2)),
                    strokeColor: this.getPathColor(this.getStrokeColor("blue")),
		    fillColor:this.getProperty("fillColor","transparent")
                }
		if(tpoints.length==1) {
		    featuresToAdd.push(this.map.createPoint(ID,  tpoints[0], attrs, null));
		} else {
		    if(this.getShowPathEndPoint()) {
			featuresToAdd.push(this.map.createMarker("startpoint", tpoints[0],RamaddaUtil.getCdnUrl("/icons/map/marker-green.png")));
			featuresToAdd.push(this.map.createMarker("endpoint", tpoints[tpoints.length-1],RamaddaUtil.getCdnUrl("/icons/map/marker-blue.png")));
		    }
		    let poly = this.map.createPolygon(ID, "", tpoints, attrs, null,true);
		    poly.noSelect = true;
		    featuresToAdd.push(poly);
		}
		if(!this.getProperty("showPoints")) {
		    this.addFeatures(featuresToAdd);
                    return;
		}
            }

            let latField1 = this.getFieldById(fields, this.getProperty("latField1"));
            let latField2 = this.getFieldById(fields, this.getProperty("latField2"));
            let lonField1 = this.getFieldById(fields, this.getProperty("lonField1"));
            let lonField2 = this.getFieldById(fields, this.getProperty("lonField2"));
	    let showSegments = this.getShowSegments(false);
	    let greatCircle = this.getUseGreatCircle();
            if (greatCircle && (
		showSegments && latField1 && latField2 && lonField1 && lonField2) ||
		polygonField) {
		if(!MapUtils.loadTurf(()=>{
		    this.createPoints(records, fields, points,bounds, debug);		
		})) {
		    return;
		}
	    }


            let sizeSegments = this.getSizeSegments();
            let sizeEndPoints = this.getProperty("sizeEndPoints", true);
            let showEndPoints = this.getProperty("showEndPoints", false);
            let endPointSize = parseInt(this.getProperty("endPointSize", "4"));
            let dfltEndPointSize = endPointSize;
            let segmentWidth = parseInt(this.getSegmentWidth(1));
            let dfltSegmentWidth = segmentWidth;
	    let haveLayer = this.getShowLayers() && (this.getProperty("geojsonLayer") || this.getProperty("kmlLayer") || this.getProperty("shapefileLayer"));
	    //	    if(haveLayer && Utils.isDefined(haveLayer.match) && 
	    if(!haveLayer) {
		if(this.getProperty('mapLayers')) {
		    this.getProperty('mapLayers').forEach(layer=>{
			if(layer.match) {
			    haveLayer = true;
			}
		    });
		}
	    }

            let showPoints = this.getProperty("showPoints", !haveLayer);
            let lineColor = this.getProperty("lineColor", "green");
	    let lineCap = this.getProperty('lineCap', 'round');
            let iconField = this.getFieldById(fields, this.getProperty("iconField"));

	    let makeLevelRange = v=>{
		if(!Utils.isDefined(v)) return null;
		let toks  = v.split(":");
		let r = {
		    min:Utils.stringDefined(toks[0])?+toks[0]:-1,
		    max:Utils.stringDefined(toks[1])?+toks[1]:100
		}
		if(isNaN(r.min)) r.min = -1;
		if(isNaN(r.max)) r.max = 100;
		return r;
	    };

	    this.glyphLevelRange =makeLevelRange(this.getProperty("glyphLevelRange"));
	    this.pointLevelRange =makeLevelRange(this.getProperty("pointLevelRange"));

	    let glyphs=RamaddaDisplayUtils.getGlyphs(this,fields,records);
	    let canvasBackground = this.getProperty('canvasBackground');
	    let canvasBorder = this.getProperty('canvasBorder');
	    let canvasWidth =this.getCanvasWidth();
	    let canvasHeight =this.getCanvasHeight();
	    let glyphSize =this.getProperty('glyphSize','32');
            let rotateField = this.getFieldById(fields, this.getProperty('rotateField'));	   
	    let rotateScale = this.getRotateScale();
	    let markerIcon = this.getMarkerIcon(this.getProperty('pointIcon'));
	    let usingIcon = markerIcon || iconField;
	    let showPoint = !usingIcon;
	    if(glyphs.length>0) showPoint=this.getProperty('showPoint',true);

            let iconSize = parseFloat(this.getProperty('iconSize',this.getProperty('radius',32)));
	    let iconMap = this.getIconMap();
	    let dfltShape = this.getProperty('defaultShape',null);
	    let dfltShapes = ['circle','triangle','star',  'square', 'cross','x', 'lightning','rectangle','church'];
	    let dfltShapeIdx=0;

	    let shapeBy = {
		id: this.getDisplayProp(source, 'shapeBy', null),
		field:null,
		map: {},
		labels:{},
		patterns:[]
	    }


	    if(this.getDisplayProp(source, 'shapeByMap', null)) {
		this.getDisplayProp(source, 'shapeByMap', null).split(',').forEach((pair)=>{
		    let tuple = pair.split(':');
		    shapeBy.map[tuple[0]] = tuple[1];
		    if(tuple[0].match('(\\*|\\.||\\+)')) {
			shapeBy.patterns.push({pattern:tuple[0],shape:tuple[1]});
			shapeBy.labels[tuple[0]] = tuple[2] ??tuple[0];
		    }

		})
	    }

	    let sizeBy = new SizeBy(this, this.getProperty("sizeByAllRecords",true)?this.getData().getRecords():records);

            for (let i = 0; i < fields.length; i++) {
                let field = fields[i];
                if (field.getId() == shapeBy.id || ("#" + (i + 1)) == shapeBy.id) {
                    shapeBy.field = field;
		    if (field.isString()) shapeBy.isString = true;
                }
            }
            shapeBy.index = shapeBy.field != null ? shapeBy.field.getIndex() : -1;


            if (this.getProperty("showColorByMenu", false) && colorBy.field && !this.madeColorByMenu) {
                this.madeColorByMenu = true;
                let menu = HU.open(SELECT,[ATTR_CLASS,'ramadda-pulldown',
					   ATTR_ID,this.domId("colorByMenu")]);
                for (let i = 0; i < fields.length; i++) {
                    let field = fields[i];
                    if (!field.isNumeric() || field.isFieldGeo()) continue;
                    let extra = "";
                    if (colorBy.field.getId() == field.getId()) extra = "selected ";
                    menu += "<option value='" + field.getId() + "' " + extra + ">" + field.getLabel() + "</option>\n";
                }
                menu += HU.close(SELECT);
                this.writeHtml(ID_TOP_RIGHT, "Color by: " + menu);
                this.jq("colorByMenu").change(() => {
                    let value = this.jq("colorByMenu").val();
                    this.vectorMapApplied = false;
		    this.haveCalledUpdateUI = false;
                    this.setProperty("colorBy", value);
                    this.updateUI();
                });
            }



	    let dateMin = null;
	    let dateMax = null;

	    let dates = [];
            let justOneMarker = this.getPropertyJustOneMarker();

            for (let i = 0; i < records.length; i++) {
                let pointRecord = records[i];
		dates.push(pointRecord.getDate());
                if (dateMin == null) {
                    dateMin = pointRecord.getDate();
                    dateMax = pointRecord.getDate();
                } else {
                    let date = pointRecord.getDate();
                    if (date) {
                        if (date.getTime() < dateMin.getTime())
                            dateMin = date;
                        if (date.getTime() > dateMax.getTime())
                            dateMax = date;
                    }
                }
	    }


            if (dateMax) {
		if (this.getAnimationEnabled()) {
		    //TODO: figure out when to call this. We want to update the animation if it was from a filter change
		    //but not from an animation change. Hummmm.
		    //this.getAnimation().init(dateMin, dateMax,records);
		}
            }

	    this.removeFeatureLayer();
            let didColorBy = false;
            let seen = {};
	    let xnct =0;
	    let lastPoint;
	    let pathAttrs ={
		strokeColor: this.getProperty("pathColor",lineColor),
		strokeWidth: this.getPathWidth()
	    };

	    let fillColor = this.getFillColor();
	    let fillOpacity =  this.getFillOpacity();
	    let isPath = this.getIsPath();
	    if(this.getIsPathThreshold()>records.length) isPath=true;
	    if(isPath)
		showPoints = this.getProperty("showPoints", !isPath);

	    let groupByField = this.getFieldById(null,this.getGroupByField());
	    let groups;
	    if(groupByField)
		groups =  RecordUtil.groupBy(records, this, false, groupByField);
	    


	    let tooltip = this.getTooltip('${default}');
	    let haveTooltip = Utils.stringDefined(tooltip);
	    let highlight = this.getShowHighlight(this.getProperty("highlight"));
	    let highlightTemplate = this.getHighlightTemplate();
	    if(highlightTemplate)
		highlight=true;
	    
	    let highlightWidth = this.getHighlightWidth(300);
	    let highlightHeight = this.getHighlightHeight(-1);
	    let highlightSize = null;
	    if(highlightHeight>0) {
	    	highlightSize = MapUtils.createSize(highlightWidth,highlightHeight);
	    }
	    let addedPoints = [];
	    let textGetter = this.getTextGetter(fields);

	    let highlightGetter = f=>{
		if(f.record) {
                    return   HU.div([],this.getRecordHtml(f.record, fields, highlightTemplate|| tooltip));
                    return   HU.div([ATTR_STYLE,HU.css('background','#fff')],this.getRecordHtml(f.record, fields, highlightTemplate|| tooltip));
		}
		return null;
	    };	    
	    this.haveAddPoints = true;
	    this.recordToInfo = {};
	    let dummyUpLocation = this.fakeLocations();
	    let recordInfos = records.map((record,idx)=>{
		let recordInfo =  {
		    record:record,
		    features:[],
		    visible:true
		}
		if(!record.point) {
		    recordInfo.x = record.getLongitude();
		    recordInfo.y = record.getLatitude();
		    if(dummyUpLocation && isNaN(recordInfo.x)) {
			recordInfo.x = -105;
			recordInfo.y=40;
		    }
		} else {
		    recordInfo.x = record.point.x;
		    recordInfo.y = record.point.y;
		}


		this.recordToInfo[record.getId()]  =recordInfo;
		return recordInfo;
	    });




	    if(this.getHandleCollisions()) {
		let collisionTooltip = this.getCollisionTooltip('${default}');
		let collisionTextGetter = collisionTooltip==null?null:(records)=>{
		    let html = "#" + records.length+" records<hr class=ramadda-thin-hr>";
		    records.forEach(record=>{
			html+=HU.div([ATTR_STYLE,HU.css("border-bottom","1px solid #ccc")], this.getRecordHtml(record, null,collisionTooltip));
		    });
		    return html;
		};


		let collisionArgs = {
		    textGetter: collisionTextGetter,
		    fixed:this.getCollisionFixed(),
		    visible: false,
		    icon:this.getCollisionIcon(),
		    iconSize:this.getCollisionIconSize(),	
		    dotColor:this.getCollisionDotColor(),
		    dotOpacity:this.getCollisionDotOpacity(),
		    ringColor:this.getCollisionRingColor(),
		    ringWidth:this.getCollisionRingWidth(),		    		    
		    dotColorOn:this.getCollisionDotColorOn(),
		    dotRadius:this.getCollisionDotRadius(),
		    scaleDots:this.getPropertyCollisionScaleDots(),
		    labelTemplate:this.getCollisionLabelTemplate(),
		    labelColor:this.getCollisionLabelColor(),
		    labelFontSize:this.getCollisionLabelFontSize(),
		}

		let CH = new CollisionHandler(this.map, {
		    pointSize:this.getCollisionPointSize(),
		    collisionArgs:collisionArgs,
		    lineWidth: this.getProperty("collisionLineWidth","2"),			
		    lineColor: this.getProperty("collisionLineColor","#000"),
		    addCollisionLines:function(info,lines) {
			_this.addFeatures(lines,true);
		    },
		    setCollisionVisible: function(info,visible) {
			info.records.forEach(record=>{
			    let recordInfo = _this.recordToInfo[record.getId()];
			    if(!recordInfo) {
				return;
			    }
			    recordInfo.features.forEach(f=>{
				f.featureVisible = info.visible;
				_this.map.checkFeatureVisible(f,true);
			    });
			});
		    }
		});


		//First get the rounded point for each RecordInfo
		CH.initPoints(recordInfos);

		recordInfos.forEach((recordInfo,idx)=>{
		    let collisionPoint = recordInfo.collisionPoint;
		    if(collisionPoint ==null) return;
		    let cntAtPoint = CH.countAtPoint[collisionPoint];
		    if(cntAtPoint==1) {
			return;
		    } 
		    let record = recordInfo.record;
		    let info = CH.getCollisionInfo(collisionPoint);
		    info.addRecord(record);
		    recordInfo.collisionInfo = info;
		    recordInfo.visible = info.visible;		    
		    if(!collisionArgs.fixed) {
			let anglePer = 360/cntAtPoint;
			let lineOffset = CH.offset;
			let delta = cntAtPoint/8;
			if(delta>1)
			    lineOffset*=delta;
			let ep = Utils.rotate(collisionPoint.x,collisionPoint.y,collisionPoint.x,collisionPoint.y-lineOffset,
					      info.records.length*anglePer-180,true);
			let line = this.getMap().createLine("line-" + idx, "", collisionPoint.y,collisionPoint.x, ep.y,ep.x, {strokeColor:CH.lineColor,strokeWidth:CH.lineWidth});

			if(!info.visible) {
			    line.featureVisible = false;
			    this.map.checkFeatureVisible(line,true);
			}
			info.addLine(line);
			//set the rotated location of the point to use later
			recordInfo.x=ep.x;
			recordInfo.y=ep.y;
		    } else {
			recordInfo.dontShow = true;
		    }
		});
		CH.getCollisionInfos().forEach((info,idx)=>{
		    let dots = info.createDots(idx);
		    featuresToAdd.push(...dots);
		});
	    }


	    let featureCnt=0;
	    let sizeByFunc = function(percent, size) {
                if (sizeEndPoints &&!isNaN(percent)) {
		    endPointSize = dfltEndPointSize + parseInt(10 * percent);
                }
                if (sizeSegments) {
		    if(isNaN(percent)) {
			segmentWidth = dfltSegmentWidth + size;
		    } else {
			segmentWidth = dfltSegmentWidth + parseInt(10 * percent);
			segmentWidth=size;
			if(segmentWidth==0 || isNaN(segmentWidth)) segmentWidth=1;
		    }
                }
	    };


	    if(isPath && groups) {
		let pathWindowStrokeColor=this.getProperty('pathWindowStrokeColor');
		let pathWindowSize = this.getProperty('pathWindowSize',0);
		let pathWindowTime = this.getProperty('pathWindowTime');
		let pathWindowTimeMS;
		if(pathWindowTime)
		    pathWindowTimeMS = DataUtils.timeToMillis(pathWindowTime);
		let dots = [];
		groups.values.forEach(value=>{
		    let firstRecord = null;
		    let lastRecord = null;
		    let secondRecord = null;		    
		    let groupRecords=groups.map[value];
		    let length=groupRecords.length;
		    let windowStartIndex=-1;
		    if(pathWindowTime) {
			let last = groupRecords[groupRecords.length-1];
			if(last && last.recordTime) {
			    let endMS = last.recordTime.getTime();
			    let startMS = endMS-pathWindowTimeMS;
			    //			    console.log("START:" + new Date(startMS) +" end:" + new Date(endMS));
			    for(let idx=groupRecords.length-1;idx>=0;idx--) {
				let record = groupRecords[idx];
				if(!record || !record.recordTime) continue;
				if(record.recordTime.getTime()<startMS) {
				    break;
				}
				windowStartIndex=idx;
			    }
			    //			    console.dir('start index',windowStartIndex);
			}
		    } else if(pathWindowSize>0) {
			windowStartIndex = Math.max(0,length-pathWindowSize);
		    }
		    groups.map[value].forEach((record,idx)=>{
			if(!firstRecord) firstRecord=record;
			featureCnt++;
			if(lastRecord) {
			    let attrs = $.extend({},pathAttrs);
			    let color=colorBy.getColorFromRecord(record, pathAttrs.strokeColor,true);
			    attrs.strokeColor = color;
			    let inWindow  =false;
			    if(windowStartIndex>=0) {
				inWindow = idx>=windowStartIndex;
			    }				
			    if(pathWindowStrokeColor) {
				attrs.strokeColor=pathWindowStrokeColor;
			    }

			    if(!inWindow) {
				if(attrs.strokeWidth>0) {
				    let line = this.map.createLine("line-" + featureCnt, "", lastRecord.getLatitude(), lastRecord.getLongitude(), record.getLatitude(),record.getLongitude(),attrs);
				    featuresToAdd.push(line);
				    line.record=record;
				    line.textGetter=textGetter;
				}
			    } else {
				let percent = (idx-windowStartIndex)/(length-windowStartIndex);
				let dotPoint = {x:record.getLongitude(),y:record.getLatitude()}; 
				let dotAttrs =   {fillColor:color,
						  strokeWidth:0,
						  fillOpacity:Math.max(0.2,percent),
						  pointRadius:Math.max(2,Math.floor(percent*radius))};
				let dot = this.map.createPoint("endpoint",dotPoint,dotAttrs, null);
				dot.record=record;
				dot.textGetter = textGetter;
				dots.push(dot);
			    }
			}
			secondRecord = lastRecord;
			lastRecord = record;
			if(secondRecord) {
			    /*
			      let angleDeg = Utils.getBearing({x:lastRecord.getLongitude(),
			      
			      y:lastRecord.getLatitude()},
			      {x:secondRecord.getLongitude(),
			      y:secondRecord.getLatitude()});							  
			      //			    let endPoint = this.map.createPoint("endpoint", {x:lastRecord.getLongitude(),y:lastRecord.getLatitude()}, {fillColor:"red",strokeColor:"#000",pointRadius:6,graphicName:"arrow",rotation:angleDeg}, null);
			      //featuresToAdd.push(endPoint);
			      //                            featuresToAdd.push(endPoint);
			      */
			}

		    });
		    featuresToAdd.push(...dots);


		    if(lastRecord) {
			let color=  colorBy.getColorFromRecord(lastRecord, pathAttrs.strokeColor);
			if(secondRecord && this.getShowPathEndPoint(false)) {
			    let shape = this.getProperty("pathEndPointShape",null);
			    let angleDeg = Utils.getBearing({lon:secondRecord.getLongitude(),
							     lat:secondRecord.getLatitude()},
							    {lon:lastRecord.getLongitude(),
							     lat:lastRecord.getLatitude()});							  
			    let endPoint = this.map.createPoint("endpoint", {x:lastRecord.getLongitude(),y:lastRecord.getLatitude()}, {fillColor:color,strokeColor:"#000",pointRadius:6,graphicName:shape,rotation:angleDeg}, null);
			    featuresToAdd.push(endPoint);
			}
			if(this.getProperty("showPathStartPoint",false)) {
			    let endPoint = this.map.createPoint("startpoint", {x:firstRecord.getLongitude(),y:firstRecord.getLatitude()}, {fillColor:color,pointRadius:2}, null);
			    featuresToAdd.push(endPoint);
			}			
		    }
		});

	    }


	    let colorByEnabled = colorBy.isEnabled();
	    let graphicName = this.getPropertyShape();

	    let didMarker = false;
	    let times=[new Date()];



	    //main loop
	    recordInfos.forEach((recordInfo,idx)=>{
		if(recordInfo.dontShow) return;

		featureCnt++;
		let record = recordInfo.record;
		let point  = recordInfo;

		if(!point) {
                    point = MapUtils.createPoint(record.getLongitude(), record.getLatitude());
		} else {
		    if(!Utils.isDefined(point.x) || !Utils.isDefined(point.y)) return;
		}


		if(justOneMarker) {
		    debug = false;
		    if(didMarker) {
			if(debug)
			    console.log("didMarker");
			return;
		    }
                    if(this.justOneMarker)
			this.map.removeMarker(this.justOneMarker);
                    if(!isNaN(point.x) && !isNaN(point.y)) {
			didMarker = true;
                        this.justOneMarker= this.map.createMarker(id, [point.x,point.y], null, "", "");
			featuresToAdd.push(this.justOneMarker);
			if(debug) console.log("\tadding justOneMarker had initial position:" + this.hadInitialPosition);
			if(!this.hadInitialPosition) {
			    let loc = MapUtils.createLonLat(point.x,point.y);
			    if(debug) console.log("\tsetting center:" + loc);
			    this.map.setCenter(loc);
			}
                        return;
                    } else {
			return;
                    }
                }

                let values = record.getData();
                let props = {
		    strokeOpacity:strokeOpacity,
                    pointRadius: radius,
                    strokeWidth: strokeWidth,
                    strokeColor: strokeColor,
		    fillColor: fillColor,
		    fillOpacity: fillOpacity
                };

		if(shapeBy.field) {
		    let gv = values[shapeBy.index];
		    if(gv)  {
			let _gv = String(gv).toLowerCase();
			let shape = null;
			shapeBy.patterns.every(pattern=>{
			    if(_gv.match(pattern.pattern)) {
				shape =  pattern.shape;
				return false;
			    }
			    return true;
			});

			if(!shape) shape = shapeBy.map[_gv];


			if(!shape) {
			    if(dfltShape) {
				//				shape = shapeBy.map[_gv] =  dfltShape;
				shape =   dfltShape;
				shapeBy.labels[_gv] = gv;
			    } else {
				if(dfltShapeIdx>=dfltShapes.length)
				    dfltShapeIdx = 0;
				shape = shapeBy.map[_gv] = dfltShapes[dfltShapeIdx++];
				shapeBy.labels[_gv] = gv;
			    }
			}
			if(!shape)
			    shape = shapeBy.map[_gv];
			if(Utils.isDefined(shape)) {
			    props.graphicName = shape;
			}
			
		    }
		}


                segmentWidth = dfltSegmentWidth;
                props.pointRadius = sizeBy.getSize(values, props.pointRadius,sizeByFunc);
		if(props.pointRadius<0) return;
		if(isNaN(props.pointRadius) || props.pointRadius == 0) props.pointRadius= radius;
		let hasColorByValue = false;
		let colorByValue;
		let colorByColor;
		let theColor =  null;

		if(colorBy.compareFields.length>0) {
		    let maxColor = null;
		    let maxValue = 0;
		    colorBy.compareFields.forEach((f,idx)=>{
			let value = record.getData()[f.getIndex()];
			if(idx==0 || value>maxValue) {
			    maxColor = colorBy.colors[idx];
			    maxValue = value;
			}
		    });
		    colorByValue = maxValue;
		    theColor = maxColor;
		} else {
		    if(colorByEnabled) {
			let value = record.getData()[colorBy.index];
			if(hideNaN && isNaN(value)) return;
			colorByValue = value;
			theColor =  colorBy.getColorFromRecord(record, theColor,false);
			//if(idx<5) console.log("%cpt:" + value + " " + theColor,"background:" + theColor);
		    }
                }


		if(theColor) {
                    didColorBy = true;
		    hasColorByValue  = true;
		    colorByColor = props.fillColor = colorBy.convertColor(theColor, colorByValue);
		}
		

		if(highlightRecords && !record.isHighlight(this)) {
		    props.fillColor =  unhighlightFillColor;
		    props.strokeColor =  unhighlightStrokeColor;
		    props.strokeWidth=unhighlightStrokeWidth;
		    if(unhighlightRadius>0)
			props.pointRadius = unhighlightRadius;
		}


		if(polygonField) {
		    let s = values[polygonField.getIndex()];
		    let polygonProps ={};
		    $.extend(polygonProps,props);
		    polygonProps.fillColor = "transparent";
		    polygonProps.fillColor = props.fillColor;
		    if(polygonProps.strokeWidth==0)
			polygonProps.strokeWidth=1;
		    if(polygonColorTable) {
			if(cidx>=polygonColorTable.length) cidx=0;
			polygonProps.strokeColor=polygonColorTable[cidx++];
		    } else if(colorByColor) {
			polygonProps.strokeColor=colorByColor;
		    }
		    let polys = this.map.createPolygonFromString('',s, polygonProps,latlon,null);
		    polys.forEach(poly=>{
			poly.textGetter = textGetter;
			poly.record = record;
			let recordDate = record.getDate();
			if (recordDate) {
			    poly.date = recordDate.getTime();
			}
			featuresToAdd.push(poly);
		    });
		    polys.forEach(poly=>{
			featuresToAdd.push(poly);
		    });
		}


                if (showSegments && latField1 && latField2 && lonField1 && lonField2) {
                    let lat1 = values[latField1.getIndex()];
                    let lat2 = values[latField2.getIndex()];
                    let lon1 = values[lonField1.getIndex()];
                    let lon2 = values[lonField2.getIndex()];
                    let attrs = {};
                    if (props.fillColor)
                        attrs.strokeColor = props.fillColor;
                    else
                        attrs.strokeColor = lineColor;
		    attrs.strokeLinecap = lineCap;
		    attrs.strokeColor =   colorBy.getColorFromRecord(record, attrs.strokeColor);
                    attrs.strokeWidth = segmentWidth;
		    let line;
		    if(greatCircle) {
			let start = turf.point([lon1,lat1]);
			let end = turf.point([lon2,lat2]);
			let points = this.getTurfPoints(turf.greatCircle(start, end));
			let type = 'OpenLayers.Geometry.LineString';
			line =this.makeFeature(this.getMap(),type,attrs,points);		    
		    } else {
			line= this.map.createLine("line-" + featureCnt, "", lat1, lon1, lat2, lon2, attrs);
		    }
		    featuresToAdd.push(line);
		    line.record = record;
		    line.textGetter = textGetter;
		    if(highlight) {
			line.highlightTextGetter = highlightGetter;
			line.highlightSize = highlightSize;
		    }	
		    line.record = record;
                    featuresToAdd.push(line);
                    if (showEndPoints) {
                        let pointProps = {};
                        $.extend(pointProps, props);
                        pointProps.fillColor = attrs.strokeColor;
                        pointProps.strokeColor = attrs.strokeColor;
                        pointProps.pointRadius = dfltEndPointSize;
                        pointProps.pointRadius = endPointSize;
                        let p1 = MapUtils.createLonLat(lon1, lat1);
                        let p2 = MapUtils.createLonLat(lon2, lat2);
                        if (!Utils.isDefined(seen[p1])) {
                            seen[p1] = true;
			    let pt1 =this.map.createPoint("endpt-" + featureCnt, p1, pointProps);
			    featuresToAdd.push(pt1);
			    pt1.record = record;
			    pt1.textGetter = textGetter;
                            featuresToAdd.push(pt1);
                        }
                        if (!Utils.isDefined(seen[p2])) {
                            seen[p2] = true;
                            let pt2 = this.map.createPoint("endpt2-" + featureCnt, p2, pointProps);
			    featuresToAdd.push(pt2);
			    pt2.record = record;
			    pt2.textGetter = textGetter;
                            featuresToAdd.push(pt2);
                        }

                    }
		}
		let mapPoint=null;
		let recordFeatures =recordInfo.features;
		if(usingIcon) {
		    if(iconField) {
			let tuple = record.getData();
			let icon = tuple[iconField.getIndex()];
			if(iconMap) {
			    icon = iconMap[icon];
			    if(!icon) icon = this.getMarkerIcon(null,true);
			}
			let size = iconSize;
			if(sizeBy.index>=0) {
			    size = props.pointRadius;
			}
			mapPoint = this.map.createMarker("pt-" + featureCnt, point, icon, "pt-" + featureCnt,null,null,size);
			mapPoint.isMarker = true;
			recordFeatures.push(mapPoint);
			this.markers[record.getId()] = mapPoint;
			pointsToAdd.push(mapPoint);
		    } else  {
			let attrs = {
			}
			if(rotateField) {
			    attrs.rotation = rotateScale*record.getValue(rotateField.getIndex());
			}
			mapPoint = this.map.createMarker("pt-" + featureCnt, point, markerIcon, "pt-" + featureCnt,null,null,
							 iconSize,null,null,attrs);
			mapPoint.levelRange = this.pointLevelRange;
			mapPoint.textGetter= textGetter;
			pointsToAdd.push(mapPoint);
			mapPoint.isMarker = true;
			recordFeatures.push(mapPoint);
			this.markers[record.getId()] = mapPoint;
		    }
		}

		if(glyphs.length>0) {
		    let cid = HU.getUniqueId("canvas_");
		    let c = HU.tag(TAG_CANVAS,[ATTR_CLASS,"", ATTR_WIDTH,canvasWidth,ATTR_HEIGHT,canvasHeight,ATTR_ID,cid]);
		    $(document.body).append(c);
		    let canvas = document.getElementById(cid);
		    let ctx = canvas.getContext("2d");

		    if(canvasBackground) {
			ctx.fillStyle=canvasBackground;
			ctx.fillRect(0,0,canvasWidth,canvasHeight);
		    }
		    if(canvasBorder) {
			ctx.strokeStyle = canvasBorder;
			ctx.strokeRect(0,0,canvasWidth,canvasHeight);
		    }
		    ctx.strokeStyle ="#000";
		    ctx.fillStyle="#000";
		    let pending = [];
		    glyphs.forEach(glyph=>{
			let isReady =  glyph.draw({}, canvas, ctx, 0,canvasHeight,{record:record});
			if(isReady) pending.push(isReady);
		    });
		    let img = canvas.toDataURL();
		    let size = glyphSize;
		    if(sizeBy.index>=0) {
			size = props.pointRadius;
		    }
		    mapPoint = this.map.createMarker("pt-" + featureCnt, point, img, "pt-" + featureCnt,null,null,size);
		    mapPoint.levelRange=this.glyphLevelRange;
		    mapPoint.isMarker = true;
		    recordFeatures.push(mapPoint);
		    this.markers[record.getId()] = mapPoint;
		    pointsToAdd.push(mapPoint);
		}

		if(showPoint || colorByEnabled)  {
		    if(!props.graphicName)
			props.graphicName = graphicName;
		    if(rotateField) {
			props.rotation = rotateScale*record.getValue(rotateField.getIndex());
		    }
		    props.fillColor =   colorBy.getColorFromRecord(record, props.fillColor);
		    if(props.fillColor==null) {
			if(hideMissingColor)
			    return
		    }
		    if(radius>0) {
			if(haveTooltip) {
			    props.cursor = 'pointer';
			}
			let propsToUse = props;
			mapPoint = this.map.createPoint("pt-" + featureCnt, point, propsToUse, null);
			mapPoint.levelRange = this.pointLevelRange;
			pointsToAdd.push(mapPoint);
			this.markers[record.getId()] = mapPoint;
			recordFeatures.push(mapPoint);
		    }
		}


		if(isPath && !groups && lastPoint) {
		    pathAttrs.strokeColor = colorBy.getColorFromRecord(record, pathAttrs.strokeColor);
		    let line = this.map.createLine("line-" + featureCnt, "", lastPoint.y, lastPoint.x, point.y,point.x,pathAttrs);
		    linesToAdd.push(line);
		}
		lastPoint = point;
		if(features) {
		    recordFeatures.forEach(f=>{features.push(f);});
		}
                let date = record.getDate();

		recordFeatures.forEach(mapPoint=>{
		    if(highlight) {
			mapPoint.highlightTextGetter = highlightGetter;
			mapPoint.highlightSize = highlightSize;
		    }
		    mapPoint.record = record;
		    mapPoint.textGetter = textGetter;
		    mapPoint.hasColorByValue = hasColorByValue;
		    mapPoint.colorByValue= colorByValue;
		    mapPoint.colorByColor = colorByColor;
		    if (date) {
			mapPoint.date = date.getTime();
		    }
		    if(!recordInfo.visible) {
			mapPoint.featureVisible = false;
			this.map.checkFeatureVisible(mapPoint,true,records.length<20);
		    }
		});

		if(recordInfo.collisionInfo) {
		    recordInfo.collisionInfo.addFeatures(recordFeatures);
		}
	    });
	    
	    times.push(new Date());
	    if(showPoints) {
		this.addFeatures(pointsToAdd);
	    }

	    this.addFeatures(linesToAdd);
	    this.myPoints = pointsToAdd;
	    this.addFeatures(featuresToAdd);
	    times.push(new Date());
	    if(debugTimes)
		Utils.displayTimes("map: #records:" + records.length+" times:",times, true,["create markers","add features"]);


	    if(records.length>0 && this.getProperty("selectFirstRecord")&& !this.haveSelectedFirstRecord) {
		this.haveSelectedFirstRecord = true;
		let record = records[0];
		this.propagateEventRecordSelection({record:record});
		let displayDiv = this.getProperty("displayDiv", null);
		if(displayDiv && this.textGetter) {
		    $("#" + displayDiv).html(this.textGetter({record:record}));
		}
		let marker =  this.markers[record.getId()];
		if(marker) {
		    this.map.handleFeatureclick(null,marker);
		}
	    }

	    if (showSegments) {
		this.map.centerOnMarkers();
	    }


	    let legendSide = this.getSizeByLegendSide();
	    if(legendSide) {
		let legend = sizeBy.getLegend(5,fillColor,legendSide=="left" || legendSide=="right");
		if(legend !="") {
		    let label = this.getSizeByLegendLabel();
		    if(label) legend=HU.div([ATTR_STYLE,'text-align:center;font-weight:bold'],label)+legend;
		    let style = this.getSizeByLegendStyle();
		    if(style) legend = HU.div([ATTR_STYLE,style],legend);
		    this.jq(ID_SIZEBY_LEGEND).html(legend);
		    this.callingUpdateSize = true;
		    this.map.getMap().updateSize();
		    this.callingUpdateSize = false;
		}
	    }
	    times = [new Date()];
	    if (didColorBy) {
		this.showColorTable(colorBy);
	    }
	    this.jq(ID_BOTTOM).append(HU.div([ATTR_ID,this.domId(ID_SHAPES)]));
	    times.push(new Date());
	    //	    Utils.displayTimes("final map points:",times, true);

	    if(iconField&& iconMap) {
		let html = "";
		for(a in iconMap) {
		    html+=HU.image(iconMap[a],["width","32"]) +" " + a+" ";
		}
		this.jq(ID_SHAPES).html(HU.center(html));
	    }

	    if(shapeBy.field) {
		let shapes = shapeBy.field.getLabel()+": ";
		for(v in shapeBy.map) {
		    let label = shapeBy.labels[v]??v;
		    let shape = shapeBy.map[v];
		    if(shape=="circle") shape=HU.getIconImage("fa-circle");
		    else if(shape=="square") shape=HU.getIconImage("fa-square");		    
		    else if(shape=="rectangle") shape=HU.getIconImage("fa-square");		    
		    else if(shape=="star") shape=HU.getIconImage("fa-star");
		    else if(shape=="diamond") shape=HU.getIconImage("fa-diamond");		    		    
		    else if(shape=="triangle") shape=HU.getIconImage("/icons/triangle.png",["width","16px"]);		    
		    else if(shape=="lightning") shape=HU.getIconImage("/icons/lightning.png",["width","16px"]);		    
		    else if(shape=="cross") shape=HU.getIconImage("/icons/cross.png",["width","16px"]);		    
		    else if(shape=="church") shape=HU.getIconImage("fa-cross");
		    shapes+=shape+" " + label +SPACE2;
		}
		this.jq(ID_SHAPES).html(HU.center(shapes));
	    }

	    if (this.getProperty("animationTakeStep", false)) {
		this.getAnimation().doNext();
	    }

	    if(this.pointLevelRange || this.glyphLevelRange) {
		this.checkLevelRange([this.myFeatureLayer],true);
	    }		


        },

	getTextGetter:function(fields,showCount) {
	    let tooltip = this.getTooltip('${default}');
	    return  this.textGetter = f=>{
		if(!Utils.stringDefined(tooltip)) {
		    if(debugPopup) console.log("No tooltip");
		    return null;
		}

		let records;
		if(f.record) records = [f.record];
		else records = f.records;
		if(!records || records.length==0) return null;
		if(this.properties.myTextGetter) {
		    let popup= this.properties.myTextGetter(this,records);
		    if(popup) return popup;
		}

		let text ='';
		let tooltipTemplate=this.getProperty('tooltipTemplate');
		let tooltipHeader=this.getProperty('tooltipHeader');		
		if(tooltipHeader) {
		    text+=tooltipHeader.replace("${count}",records.length);
		} else if(showCount && records.length>0) {
		    text+=HU.b('Count:  ') + records.length+'<br>';
		}

		records.forEach((record,idx)=>{
		    let v = this.getRecordHtml(record, fields, tooltip);
		    if(tooltipTemplate)
			v = tooltipTemplate.replace('${value}',v);
		    else   if(idx>0) text+='<thin_hr/>';
		    text +=   v;
		});
		if(debugPopup) console.log("textGetter: getRecordHtml:"  + text);
		if(text=="") return null;
		
		let  tabs = [];
		tabs.push({label:'Properties', contents:text});
		let mapDiv;
		let theRecord;
		if(records.length>0 && this.getShowMapInTooltip()) {
		    theRecord = records[0];
		    let lat = theRecord.getLatitude();
		    let lon = theRecord.getLongitude();		    
		    mapDiv= HU.getUniqueId('');
		    let div  = HU.div([ATTR_ID,mapDiv,ATTR_STYLE,HU.css('height','400px')]);
		    tabs.push({label:'Overview Map', contents:div});
		}
		if(tabs.length==1) {
		    return text;
		}

		let t =  HU.makeTabs(tabs);
		let tabInit = t.init;	
		let popupInit = () =>{
		    tabInit();
		    if(!mapDiv) return;
		    var params = {
			initialZoom:14,
			mapCenter:theRecord.getLongitude()+','+ theRecord.getLatitude(),
			defaultMapLayer:this.getTooltipMapLayer("osm"),
		    };
		    let theMap = new RepositoryMap(mapDiv, params);
		    theMap.initMap(false);
		}
		t.init = popupInit;
		return t;
	    };
	},


        addLabels:function(records, fields) {
	    let limit = this.getLabelLimit(1000);
	    if(records.length>limit) return;
            let labelTemplate = this.getLabelTemplate();
            let labelRecordTemplate = this.getProperty('labelRecordTemplate');
	    //	    if(!labelRecordTemplate) return;
	    let labelKeyField;
	    if(this.getLabelKeyField()) {
		labelKeyField = this.getFieldById(fields,this.getLabelKeyField());
	    }
            if(!labelTemplate && !labelKeyField) return;
	    let doUniqueLabelPosition=this.getProperty('doUniqueLabelPosition',true);
	    let labelPositions = {};
	    let pixelsPerCell = 10;
            let width = this.map.getMap().viewPortDiv.offsetWidth;
            let height = this.map.getMap().viewPortDiv.offsetHeight;
	    let bounds = this.map.getBounds();
	    let numCellsX = Math.round(width/pixelsPerCell);
	    let numCellsY = Math.round(height/pixelsPerCell);	    
	    let cellWidth = (bounds.right-bounds.left)/numCellsX;
	    let cellHeight = (bounds.top-bounds.bottom)/numCellsY;	    
	    let grid = {};
	    if(labelKeyField) labelTemplate= "${_key}";
	    labelTemplate = labelTemplate.replace(/_nl_/g,"\n");
	    let labelStyle = {
                fontColor: this.getProperty("labelFontColor","#000"),
		textBackgroundFillColor:this.getLabelBackground(),
		textBackgroundStrokeColor:this.getLabelStrokeColor(),
		textBackgroundStrokeWidth:this.getLabelStrokeWidth(),				
                fontSize: this.getProperty("labelFontSize","10pt"),
                fontFamily: this.getProperty("labelFontFamily","'Open Sans', Helvetica Neue, Arial, Helvetica, sans-serif"),
                fontWeight: this.getProperty("labelFontWeight","plain"),
                labelAlign: this.getProperty("labelAlign","cc"),
                labelXOffset: this.getProperty("labelXOffset","0"),
                labelYOffset: this.getProperty("labelYOffset","0"),
                labelOutlineColor:this.getProperty("labelOutlineColor","#fff"),
                labelOutlineWidth: this.getProperty("labelOutlineWidth","0"),
		labelSelect:true,
	    };

	    if(!this.map.labelLayer) {
		this.map.labelLayer = MapUtils.createLayerVector("Labels", {
		    styleMap: MapUtils.createStyleMap({'default':labelStyle}),
                });
		this.map.addVectorLayer(this.map.labelLayer, true);
                this.map.labelLayer.setZIndex(100);
	    }


	    let keyMap={};
	    let keyLegend="";
	    let features =  [];
            let seen = {};
	    let colorBy = this.getProperty("colorBy");
	    let sizeBy = this.getProperty("sizeBy");
	    let keyIndex = 0;
	    let alpha = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".split("");
	    let  keys = [];
	    for(let j=0;j<3;j++) {
		alpha.forEach(c=>{
		    let cc = "";
		    for(let i=0;i<=j;i++) {
			cc+=c;
		    }
		    keys.push(cc);
		});
	    }
	    let labelFeatures = [];
            for (let i = 0; i < records.length; i++) {
		let record = records[i];
		let lat = record.getLatitude();
		let lon = record.getLongitude();		
		let point = {x:record.getLongitude(),y:record.getLatitude()};
                let center = MapUtils.createPoint(point.x, point.y);
                center.transform(this.map.displayProjection, this.map.sourceProjection);
		let text = this.applyRecordTemplate(record,this.getDataValues(record),null, labelTemplate);
		let style = $.extend({label:text},labelStyle);
		let labelFeature = MapUtils.createVector(center,null,style);
                labelFeature.noSelect = true;
                labelFeature.attributes = {};
                labelFeature.attributes[RECORD_INDEX] = (i+1);
                labelFeature.attributes["recordIndex"] = (i+1)+"";
		if(labelRecordTemplate) {
		    labelFeature.attributes['recordtemplate'] = text;
		}
		if(labelKeyField) {
		    let v = labelKeyField.getValue(record);
		    if(!keyMap[v]) {
			if(keyIndex>=keys.length) keyIndex=0;
			keyMap[v] = keys[keyIndex++];
			keyLegend+=keyMap[v]+": " + v+"<br>";
		    }
		    labelFeature.attributes["_key"] = keyMap[v];
		    style.label = style.label.replace('${_key}',keyMap[v]);
                }
                features.push(labelFeature);
	    }

	    if(!this.haveAddedZoomListener) {
		this.haveAddedZoomListener = true;
		if(this.getDeclutterLabels()) {
		    this.getMap().getMap().events.register('zoomend', '', () =>{
			Utils.bufferedCall(this.getId()+'_checklabels', 
					   ()=>{this.declutterLabels();});
		    },true);
		    this.getMap().getMap().events.register('moveend', '', () =>{
			Utils.bufferedCall(this.getId()+'_checklabels', 
					   ()=>{this.declutterLabels();});
		    },true);
		}
	    }

	    if(keyLegend.length>0) {
		if(!this.legendId) {
		    this.legendId = this.domId("legendid");
		    this.jq(ID_RIGHT).append(HU.div([ATTR_ID,this.legendId,
						     ATTR_CLASS,"display-map-legend",ATTR_STYLE, HU.css("max-height",this.getHeight("400px"))]));
		}
		this.jq("legendid").html(keyLegend);
	    }
	    if(this.labelFeatures)
		this.map.labelLayer.removeFeatures(this.labelFeatures);
            this.map.labelLayer.addFeatures(features);
	    this.labelFeatures = features;
	    if(this.getDeclutterLabels()) {
		this.declutterLabels();
	    }
	    $("#" + this.map.labelLayer.id).css("z-index",900);
        },


	declutterLabels:function() {
	    if(!this.labelFeatures) return;
	    MapUtils.declutter(this.getMap(), this.labelFeatures,this.getDeclutterArgs());
	    this.map.labelLayer.redraw();
	},
	getDeclutterArgs:function() {
	    let get = v=>{
		if(!v) return null;
		return +v;
	    }
	    let args ={
		fontSize: this.getProperty("labelFontSize","10pt"),
		padding: +this.getProperty('labelDeclutterPadding',1),
		granularity: +this.getProperty('labelDeclutterGranularity',1),		
		pixelsPerLine:get(this.getProperty('labelDeclutterPixelsPerLine')),
		pixelsPerCharacter:get(this.getProperty('labelDeclutterPixelsPerCharacter'))
	    };
	    return args;
	},
	
        handleEventRemoveDisplay: function(source, display) {
            if (!this.map) {
                return;
            }
            let mapEntryInfo = this.mapEntryInfos[display];
            if (mapEntryInfo != null) {
                mapEntryInfo.removeFromMap(this.map);
            }
            let feature = this.findFeature(display, true);
            if (feature != null) {
                if (feature.line != null) {
                    this.map.removePolygon(feature.line);
                }
            }
        },
        findFeature: function(source, andDelete) {
            for (let i in this.features) {
                let feature = this.features[i];
                if (feature.source == source) {
                    if (andDelete) {
                        this.features.splice(i, 1);
                    }
                    return feature;
                }
            }
            return null;
        },

        getMarkerIcon: function(dflt,autoVersion) {
            if (this.getProperty("markerIcon")) {
                let icon = this.getProperty("markerIcon");
		if(icon.startsWith('cdn:')) {
		    icon=markerIcon.replace('cdn:','');
                    icon =  RamaddaUtil.getCdnUrl(icon);
		}
                return icon;
            }
	    if(dflt) return dflt;
	    if(!autoVersion) return null;
            displayMapCurrentMarker++;
            if (displayMapCurrentMarker >= displayMapMarkers.length) displayMapCurrentMarker = 0;
            return RamaddaUtil.getCdnUrl("/lib/openlayers/v2/img/" + displayMapMarkers[displayMapCurrentMarker]);
        },
	highlightMarkers:null,
        handleEventRecordHighlight: function(source, args) {
	    SUPER.handleEventRecordHighlight.call(this,source,args);
	    if(displayDebug.handleEventRecordSelect)
		this.logMsg("handleEvent");
	    
	    if(this.getRecordHighlightFeature() ||
	       isNaN(args.record.getLatitude()) ||
	       isNaN(args.record.getLongitude())) {
		if(this.recordToFeature) {
		    let feature = this.recordToFeature[args.record.getId()];
		    if(this.highlightedFeature) {
			this.getMap().unhighlightFeature(this.highlightedFeature);
		    }
		    this.highlightedFeature = feature;
		    if(feature) {
			let style = {};
			if(feature.style) style = $.extend(style,feature.style);
			style = $.extend(style,{
			    strokeColor: this.getRecordHighlightStrokeColor(),
			    strokeWidth: this.getRecordHighlightStrokeWidth(3)}
					);
			this.getMap().highlightFeature(feature,style);
			
		    }
		}
	    } else {
		let records = args.records??[args.record];
		records.forEach((record,idx)=>{
		    this.highlightPoint(record.getLatitude(),record.getLongitude(),args.highlight,idx==0,idx!=0);
		});
	    }
	},
        handleEventRecordSelection: function(source, args) {
	    SUPER.handleEventRecordSelection.call(this, source, args);
            if (!this.map) {
                return;
            }
	    args.highlight = true;
            if (!this.getShowRecordSelection(true)) {
		return;
	    }
	    this.handleEventRecordHighlight(source,args);
	    //For now return
	    if(true) return;

            let record = args.record;
            if (record.hasLocation()) {
                let latitude = record.getLatitude();
                let longitude = record.getLongitude();
                if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) return;
                let point = MapUtils.createLonLat(longitude, latitude);



                let marker = this.myMarkers[source];
                if (marker != null) {
                    this.map.removeMarker(marker);
                }
                let icon = displayMapMarkerIcons[source];
                if (icon == null) {
                    icon = this.getMarkerIcon(null,true);
                    displayMapMarkerIcons[source] = icon;
                }
                this.myMarkers[source] = this.map.addMarker(source.getId(), point, icon, "", args.html, null, 24);
            }
        }
    });
}

function MapEntryInfo(entry) {
    RamaddaUtil.defineMembers(this, {
        entry: entry,
        marker: null,
        rectangle: null,
        removeFromMap: function(map) {
            if (this.marker != null) {
                map.removeMarker(this.marker);
            }
            if (this.rectangle != null) {
                map.removePolygon(this.rectangle);
            }
            if (this.polygon != null) {
                map.removePolygon(this.polygon);
            }
            if (this.circle != null) {
                map.removePoint(this.circle);
            }
        }

    });
}






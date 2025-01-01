/**
   Copyright (c) 2008-2025 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/


const DISPLAY_THREE_GLOBE = "three_globe";
addGlobalDisplayType({
    type: DISPLAY_THREE_GLOBE,
    forUser: true,
    label: "3D Globe",
    requiresData: true,
    category: CATEGORY_MAPS,
    tooltip: makeDisplayTooltip('3D Globe','3dglobe.png','Create interactive 3D globes'),        
});

const DISPLAY_THREE_GRID = "three_grid";
addGlobalDisplayType({
    type: DISPLAY_THREE_GRID,
    forUser: false,
    label: "3D Grid",
    requiresData: true,
    category: CATEGORY_CHARTS,
    tooltip: makeDisplayTooltip('3D Grid',null,'In development'),        
});

var ramaddaLoadedThree=false;
var ramaddaLoadedThreeGlobe=false;

function RamaddaThree_Base(displayManager, id, type,properties) {
    const SUPER = new RamaddaDisplay(displayManager, id, type, properties);
    let myProps = [];
    defineDisplay(this, SUPER, myProps, {
	parseInt:function(v,dflt) {
	    if(typeof v == "number") return v;
	    if(!v) return  dflt;
	    if(v.match("rgb")) v = Utils.rgbToHex(v);
	    if(v.startsWith("#")) v = v.substring(1);
	    if(!v.startsWith("0x")) v = '0x' + v;
	    return parseInt(Number(v), 10);
	},
	getScene() {
	    return this.scene;
	},
	getControls() {
	    return this.controls;
	},
    });

}


function RamaddaThree_globeDisplay(displayManager, id, properties) {
    const ID_CONTAINER = "container";
    const ID_GLOBE = "globe";
    const ID_POPUP = "popup";
    const ID_POSITION_BUTTON = "positionbutton";
    const ID_ROTATE_BUTTON = "rotatebutton";            
    if(!properties.width && properties.globeWidth) {
	properties.width = properties.globeWidth;
    }

    let positions = {
"Base":{
position: {x:6.048899205465489e-21,y:1.832130202344028e-20,z:250},
up: {x:1.546799663044268e-22,y:1,z:-7.328520809376114e-23}
},
"North America":{
position: {x:-185.0051316412852,y:166.36750886777244,z:-24.391663730099083},
up: {x:0.6540380289233074,y:0.7458336330441189,z:0.12635841302550785}
},
"South America":{
position: {x:-220.53665870332205,y:-71.90185405284046,z:93.24004264123046},
up: {x:-0.2051500901346411,y:0.9474654144116881,z:0.24540319682399764}
},
"Europe":{
position: {x:76.01040670390248,y:193.37167151814756,z:139.03170403539133},
up: {x:-0.286288586763548,y:0.6309644429890481,z:-0.7210566668247681}
},
"Asia":{
position: {x:215.9418144948879,y:118.22041784669743,z:-43.50937320632211},
up: {x:-0.4872587496088534,y:0.8718613869731202,z:-0.04936226124191964}
},
"Africa":{
position: {x:82.50264441171257,y:6.133419606337622,z:235.91459223415376},
up: {x:-0.005217856566188513,y:0.9996943480371796,z:-0.024165770738191875}
},
"Australia":{
position: {x:166.64046097372514,y:-104.10022198889281,z:-154.57716696953614},
up: {x:0.32825699830271454,y:0.9086541300046129,z:-0.25806009976525474}
},
"Pacific":{
position: {x:-50.02109926067879,y:70.1629622786457,z:-293.1352563989274},
up: {x:0.578136747554258,y:0.8103545388010297,z:0.09530699120186392}
},
"North Atlantic":{
position: {x:-137.7219663374266,y:134.93931772927482,z:159.1352899859414},
up: {x:0.3485760134063413,y:0.8418048847668705,z:-0.4121399020482765}
},
	"South Pole":{
	    position: {x:0,y:-249.99925472592855,z:-0.6104395794518828},
	    up: {x:0.9999999999999998,y:0,z:0}
	}, 
	"North Pole":{
	    position: {x:12.435991378990524,y:249.68975620440986,z:0.6097253513751638},
	    up: {x:-0.9987620026286266,y:0.049743817204226284,z:0.00012147100820090829}
	},
    }


    let myProps = [
        {label:'3D Globe Attributes'},
	{p:"globeWidth",d:800},
	{p:"globeHeight",d:400},
	{p:"globeStyle",d:'',tt:'css for globe'},	
	{p:"baseImage",d:"earth-blue-marble.jpg",ex:"earth-blue-marble.jpg|earth-day.jpg|earth-dark.jpg|world-boundaries.png|caida.jpg|white.png|lightblue.png|black.png"},
	{p:"globeBackgroundImage",ex:"night-sky.png|white.png|lightblue.png|black.png"},
	{p:'backgroundColor',d:'#CAE1FF',ex:'#ffffff'},
	{p:"initialPosition",ex:"North America|South America|Europe|Asia|Africa|Australia|South Pole|North Pole"},
	{p:"initialZoom",ex:"1",tt:'lower number more zoomed'},
	{p:'linked',ex:true,tt:"Link location with other globes"},
	{p:'linkGroup',ex:'some_name',tt:"Globe groups to link with"},
	{p:'mapColor',d:'blue'},	
	{p:'showGraticules',ex:true},
	{p:'showAtmosphere',d:true,ex:'false'},
	{p:'atmosphereColor',d:'#fff',ex:'red'},	    	    
	{p:'atmosphereAltitude',d:0.25,ex:0.5},
	{p:'ambientLight',d:'ffffff',ex:'ffffff'},
	{p:'ambientIntensity',d:1,ex:'1'},
	{p:'directionalIntensity',d:1},		
	{p:'directionalLight1',ex:'ffffff'},
	{p:'directionalIntensity1',ex:'0.5'},
	{p:'directionalPosition1',ex:'0,1,0'},
	{p:'directionalLight2',ex:'ffffff'},
	{p:'directionalIntensity2',ex:'0.5'},		
	{p:'directionalPosition2',ex:'0,1,0'},

	{p:'initialAltitude',d:250,ex:500},
	{p:'color',d:'blue',ex:'red'},
	{p:'showPoints',d:true,ex:'false'},
	{p:'showSpheres',ex:true},			
	{p:'pointRadius',d:1,ex:'1',canCache:true},
	{p:'pointResolution',d:12},
	{p:'heightField',tt:'field to map height to'},
	{p:'heightMin',d:0,tt:'min height range that heightField value percent is mapped to'},
	{p:'heightMax',d:0.5},
	{p:'radiusField',tt:'field to map radius to'},
	{p:'radiusMin',d:1},
	{p:'radiusMax',d:5},
	{p:'labelFields',tt:'fields for the label'},
	{p:'labelColor',d:'red',ex:'red'},
	{p:'labelSize',d:0.5},
	{p:'labelDotRadius',d:0.1},
	{p:'labelResolution',d:3},
	{p:'labelIncludeDot',d:true},		
	{p:'labelAltitude',d:0.01},		
	{p:'imageField',tt:'Field id image overlay'},
	{p:'latField1',tt:'Field id for segments'},
	{p:'lonField1',tt:'Field id for segments'},
	{p:'latField2',tt:'Field id for segments'},
	{p:'lonField2',tt:'Field id for segments'},
	{p:'lineWidth',d:0.1},
	{p:'lineAltitude',d:0.05},	
	{p:'showEndPoints',d:true},	
	{p:'polygonField',tt:'field that holds polygons'},

	{p:'geojson',tt:'base layer map or used for chloropleth display',ex:'us_states.geojson|us_counties.geojson|countries.geojson|entryid|url'},
	{p:'polygonNameField',tt:'Field to match with the name field in the geojson map, e.g., state'},
	{p:'polygonAltitude',d:0.01},

	{p:'autoRotate',ex:'true'},

	{p:'selectedDiv',ex:'div id to show selected record'},
	{p:'doPopup',d:true,ex:'',tt:''},
	{p:'centerOnFilterChange',d:true,ex:false,tt:'Center map when the data filters change'},	
    ];


    const SUPER = new RamaddaThree_Base(displayManager, id, DISPLAY_THREE_GLOBE, properties);
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
        needsData: function() {
            return true;
        },
        initDisplay:  function() {
            SUPER.initDisplay.call(this);
        },
	dataFilterChanged: function(args) {
	    SUPER.dataFilterChanged.call(this,args);
	    if(!this.getCenterOnFilterChange()) return;
	    if(!this.filteredRecords) return;
	    if(this.filteredRecords.length==0) return;
	    this.viewRecords(this.filteredRecords);
	},
	viewRecords:function(records) {
	    let bounds = RecordUtil.getBounds(records);
	    let lat = bounds.south+(bounds.north-bounds.south)/2;
	    let lng = bounds.west+(bounds.east-bounds.west)/2;
	    if(!isNaN(lat) && !isNaN(lng)) {
		this.globe.pointOfView({lat: lat,
					lng: lng,
					alt:10000});
	    }

	},

        updateUI: async function() {
	    
	    if(!window["THREE"]) {
		if(!ramaddaLoadedThree) {
                    ramaddaLoadedThree = true;
//		    Utils.importJS(ramaddaBaseHtdocs+"/lib/three/three.min.js");
		    Utils.importJS("//unpkg.com/three@0.160");		    
		}
		setTimeout(()=>{this.updateUI()},100);
		return
	    }	    

	    if(!window["Globe"]) {
		if(!ramaddaLoadedThreeGlobe) {
                    ramaddaLoadedThreeGlobe = true;
		    Utils.importJS("//unpkg.com/globe.gl");
		}
		setTimeout(()=>{this.updateUI()},100);
		return
	    }	    

            SUPER.updateUI.call(this);
	    this.jq(ID_POPUP).hide();
	    let records =this.filterData();
	    if(!records) return;
	    this.filteredRecords = records;

	    if(!this.globe) {
		this.createGlobe();
	    }


	    if(this.imageField && records.length>0) {
		let url = this.imageField.getValue(records[0]);
		this.globe.globeImageUrl(url);
	    }


            let colorBy = this.getColorByInfo(records);
	    let dfltColor = this.getColor();
	    let pointData = [];

	    let heightField = this.getFieldById(null, this.getHeightField());
	    let heights;
	    if(heightField) {
		heights = this.getColumnValues(records, heightField);
	    }
	    let heightMin = this.getHeightMin();
	    let heightMax = this.getHeightMax();	    
	    let getHeight=record=>{
		if(!heights) return 0;
		let v = heightField.getValue(record);
		let percent = (v-heights.min)/(heights.max-heights.min);
		let height = heightMin+percent*(heightMax-heightMin);
		return height;
	    }


	    let radiusField = this.getFieldById(null, this.getProperty("radiusField"));
	    let radiuss;
	    if(radiusField) {
		radiuss = this.getColumnValues(records, radiusField);
	    }
	    let radiusMin = this.getRadiusMin();
	    let radiusMax = this.getRadiusMax();	    
	    let getRadius=record=>{
		if(!radiuss || !record) return this.getPointRadius();
		let v = radiusField.getValue(record);
		let percent = (v-radiuss.min)/(radiuss.max-radiuss.min);
		let radius = radiusMin+percent*(radiusMax-radiusMin);
		return radius;
	    }


	    let labels = this.getProperty("labelFields");
	    if(labels=="{colorby}") labels=this.getProperty('colorBy');
	    let labelFields = this.getFieldsByIds(null, labels);
	    if(labelFields && labelFields.length) {
		let labelSize = +this.getLabelSize(1.0);
		let labelData = [];
		let labelColor = this.getLabelColor();
		records.forEach((record,idx)=>{
		    let label = "";
		    labelFields.forEach((f,idx) =>{
			if(idx>0) label+=" ";
			let s = String(f.getValue(record));
			if(s=='NaN') s  = '--';
			label+=s;
		    });
		    let d = {
			record:record,
			label: label,
			lat:record.getLatitude(),
			lng:record.getLongitude(),
			color:labelColor
		    };
		    d.labelSize=labelSize;
		    labelData.push(d);
		});
		this.globe.labelsData(labelData)
		    .labelLat(d => d.lat)
		    .labelLng(d => d.lng)
		    .labelAltitude(this.getLabelAltitude())
		    .labelText(d => d.label)
		    .labelColor(d => d.color)
		    .labelIncludeDot(this.getLabelIncludeDot())
		    .labelDotRadius(this.getLabelDotRadius())
		    .labelSize('labelSize')		
		    .labelResolution(this.getLabelResolution());
	    }


	    let polygonField = this.getFieldById(null, this.getProperty("polygonField"));
	    let latField1 = this.getFieldById(null, this.getProperty("latField1"));
	    let lonField1 = this.getFieldById(null, this.getProperty("lonField1"));
	    let latField2 = this.getFieldById(null, this.getProperty("latField2"));
	    let lonField2 = this.getFieldById(null, this.getProperty("lonField2"));	    	    
	    if(latField1 && lonField1  && latField2 && lonField2) { 
		let arcsData = [];
		let showEndPoints = this.getShowEndPoints();
		records.forEach((record,idx)=>{
		    let color=colorBy.getColorFromRecord(record, dfltColor);
		    arcsData.push({
			startLat:latField1.getValue(record),
			startLng:lonField1.getValue(record),
			endLat:latField2.getValue(record),
			endLng:lonField2.getValue(record),			
			color:color,
			record:record
		    });

		    if(showEndPoints) {
			pointData.push({
			    height:0,
			    lat:latField1.getValue(record),
			    lng:lonField1.getValue(record),
			    color:color,
			    radius:this.getPointRadius(),
			    record:record,
			});
			pointData.push({
			    height:0,
			    lat:latField2.getValue(record),
			    lng:lonField2.getValue(record),
			    color:color,
			    radius:this.getPointRadius(),
			    record:record,
			});
		    }
		});

		this.globe.arcsData(arcsData)
		    .arcStroke(+this.getLineWidth())
		    .arcAltitude(this.getLineAltitude())
		    .arcColor('color')
//		    .arcDashLength(() => Math.random())
		    .arcDashGap(0);
	    } else if(polygonField) {
		let polygonColorTable = this.getColorTable(true, "polygonColorTable",null);
		let delimiter;
		let first = !this.didit;
		if(!this.didit) {
		    this.didit = true;
		}
		let latlon = this.getProperty("latlon",true);
		let pathData = [];
		let pData = [];
		let cidx=0;
		records.forEach((record,idx)=>{
		    let color = dfltColor;
		    if(polygonColorTable) {
			if(cidx>=polygonColorTable.length) cidx=0;
			color=polygonColorTable[cidx++];
		    }

//		    if(idx>=5) return;
		    let poly = [];
		    let polyObj = {
			points:poly,
			record:record,
			color:color,
		    };
		    pathData.push(polyObj);
		    let s = polygonField.getValue(record);
		    if(!delimiter) {
			[";",","].forEach(d=>{
			    if(s.indexOf(d)>=0) delimiter = d;
			});
		    }
		    let toks  = s.split(delimiter);
		    let p = [];
		    for(let pIdx=0;pIdx<toks.length;pIdx+=2) {
			let lat = parseFloat(toks[pIdx]);
			let lon = parseFloat(toks[pIdx+1]);
			if(!latlon) {
			    let tmp =lat;
			    lat=lon;
			    lon=tmp;
			}
			poly.push([lat,lon,1]);
			if(pIdx==0 || pIdx==toks.length-2) {
			    let pt = {
				lat:lat,
				lng:lon,
				color:color,
				height:getHeight(record),
				radius:this.getPointRadius(),
				record:record,
			    };
			    pointData.push(pt);
			}
		    }
		});
		this.globe.pathsData(pathData)
		    .pathPoints((d) => {return d.points;})		
		    .pathColor((d) => {return [d.color,d.color]})		
		    .pathDashLength(0.01)
		    .pathDashGap(0.0)
		    .pathPointAlt((d) => {return 0.0;}) ;
	    } else {
		records.forEach((record,idx)=>{
		    let pt = {
			lat:record.getLatitude(),
			lng:record.getLongitude(),		    
			color:colorBy.getColorFromRecord(record, dfltColor),
			height:getHeight(record),
			radius:getRadius(record),
			record:record,
		    };
		    pointData.push(pt);
		});

	    }

	    let haveLatLong=false;
	    pointData.every(pt=>{
		if(Utils.isDefined(pt.lat)) {
		    haveLatLong = true;
		    return false;
		}
		return true;
	    });


	    if(haveLatLong && pointData.length>0) {
		if(this.getShowSpheres()) {
		    this.globe.customLayerData(pointData)
			.customThreeObject(d => new THREE.Mesh(
			    new THREE.SphereBufferGeometry(d.radius),
			    new THREE.MeshLambertMaterial({ color: d.color })
			))
			.customThreeObjectUpdate((obj, d) => {
			    Object.assign(obj.position, this.globe.getCoords(d.lat, d.lng, d.height+0.01));
			});
		} else if(this.getShowPoints()) {
		  this.globe.pointsData(pointData)
			.pointAltitude('height')
			.pointColor('color')
			.pointRadius('radius')
			.pointResolution(this.getPointResolution());
		} else {
//		    console.log("Not showing spheres or points");
		}
	    }

	    if(this.getProperty("geojson")) {
		let nameField = this.getFieldById(null, this.getPolygonNameField());
		if(nameField) {
		    this.addGeojsonLayer(records);
		}
	    }


	    if(colorBy.isEnabled()) {
		colorBy.displayColorTable();
	    }
	    if(!this.getInitialPosition()) {
		this.viewRecords(records);
	    }
	    this.callHook("updateUI");

        },
	getScene() {
	    return this.globe.scene();
	},
	getControls() {
	    return this.globe.controls();
	},
	setPosition:function(pos) {
	    if((typeof pos=="string") && pos.trim().startsWith("{")) {
		pos = this.parsePosition(pos);
	    }
	    let scope = this.getControls();
	    if(pos.target)
		scope.target.copy(pos.target);
	    if(pos.position)
		scope.object.position.copy(pos.position);
	    if(pos.up)
		scope.object.up.copy(pos.up);
	    if(pos.zoom)
		scope.object.zoom = pos.zoom;
	    scope.object.updateProjectionMatrix();
//	    _eye.subVectors( scope.object.position, scope.target );
	    scope.object.lookAt( scope.target );
	},
	getDataObjects: function(recordMap) {
	    let dataObjects = [];
	    let f= (object,idx)=>{
		if(object.__data && object.__data.record) {
		    if(recordMap) {
			let id = object.__data.record.getId();
			if(!recordMap[id]) recordMap[id]=[];
			recordMap[id].push(object);
		    }
		    dataObjects.push(object);
		}
		if(object.children && object.children.length) {
		    object.children.forEach(f);
		}
	    }
	    this.getScene().children.forEach(f);
	    return dataObjects;
	},

	listScene: function() {
	    this.getScene().children.forEach(obj=>{
		console.log(obj.type);
		console.dir(obj);
	    });
	},
	addLights:function() {
	    if(this.addedLight) return;
	    this.addedLight = true;

	    //turn off any directionallight
	    this.getScene().children.filter(obj=>{
		return obj.type=="DirectionalLight" || obj.type=="AmbientLight";
	    }).forEach(obj=>{
//		console.log("removing " + obj.type);
		this.getScene().remove(obj);
	    });

	    let ambientLight = this.getAmbientLight();
	    if(ambientLight && ambientLight!="none") {
//		console.log("adding ambient light:" + ambientLight);
		this.getScene().add(new THREE.AmbientLight(this.parseInt(ambientLight), this.getAmbientIntensity()));
	    }
	    
	    for(let i=1;i<10;i++) {
		let light = this.getProperty("directionalLight"+ i);
		if(!Utils.isDefined(light) || light=="none") {
		    continue;
		}
//		console.log("adding directional light:" + light);
		let dl = new THREE.DirectionalLight(this.parseInt(light),
						    this.getProperty("directionalIntensity"+ i,
								     this.getDirectionalIntensity()));
		let pos = this.getProperty("directionalPosition" + i,"0,1,0").split(",");
		dl.position.set(+pos[0],+pos[1],+pos[2]);
		this.getScene().add(dl);
	    }
	},
	createGlobe:function() {
	    this.imageField = this.getFieldById(null, this.getImageField());
	    let _this = this;
	    let popup = HU.div([CLASS,"display-three-globe-popup",ID,this.domId(ID_POPUP),STYLE,HU.css("display","none","position","absolute","left","60%","top","0px")],"");
	    let pos = HU.div([TITLE,"Select Position", CLASS,"ramadda-clickable", ID,this.domId(ID_POSITION_BUTTON),STYLE,HU.css("position","absolute","left","10px","top","10px","z-index","1000")],HU.getIconImage("fa-globe"));
	    let rotate = HU.div([TITLE,"Toggle rotate", CLASS,"ramadda-clickable", ID,this.domId(ID_ROTATE_BUTTON),STYLE,HU.css("position","absolute","left","10px","top","30px","z-index","1000")],HU.getIconImage("fa-rotate"));	    
	    let w  = parseInt(this.getGlobeWidth());
	    let h = parseInt(this.getGlobeHeight());

	    let globe = HU.div([STYLE,HU.css("position","relative")],
			       pos +
			       rotate+
			       popup +
			       HU.div(['style',HU.css('width',(w+2)+'px')+this.getGlobeStyle(''),ID, this.domId(ID_GLOBE)]));
	    let html = HU.center(globe);
	    html  = globe;
	    this.setContents(html);
	    this.jq(ID_POPUP).click(()=>{
		this.jq(ID_POPUP).hide();
	    });
		

	    this.jq(ID_ROTATE_BUTTON).click(()=>{
		let scope = this.getControls();
		scope.autoRotate = !scope.autoRotate;
	    });

	    this.jq(ID_POSITION_BUTTON).click(()=>{
		let html = "";
		for(a in positions) {
		    html+=HU.div([CLASS,"ramadda-clickable","place",a],a);
		}
		html=HU.div([STYLE,HU.css("margin","5px")],html);
		let dialog = HU.makeDialog({content:html,anchor:this.jq(ID_POSITION_BUTTON)});
		dialog.find(".ramadda-clickable").click(function() {
		    _this.setPosition(positions[$(this).attr("place")]);
		    dialog.remove();
		});
	    });
	    //Initial example code from https://github.com/vasturiano/three-globe

	    let domGlobe = document.getElementById(this.domId(ID_GLOBE));
	    this.globe = Globe()(domGlobe);	    	    
	    if(Utils.isDefined(this.getInitialZoom())) {
		this.globe.pointOfView({ lat: 0, lng: 0, altitude:  this.getInitialZoom()});
	    }
	    this.globe.onGlobeReady(()=>this.addLights());
	    this.globe.width(w);
	    this.globe.height(h);
	    this.globe.showGraticules(this.getShowGraticules())
		.showAtmosphere(this.getShowAtmosphere())
		.atmosphereColor(this.getAtmosphereColor())	    	    
		.atmosphereAltitude(this.getAtmosphereAltitude());

	    let baseImage = this.getImageUrl(this.getBaseImage());
	    if(baseImage) {
		this.globe.globeImageUrl(baseImage);
	    }
	    let bgImage = this.getImageUrl(this.getGlobeBackgroundImage());
	    if(bgImage) {
		this.globe.backgroundImageUrl(bgImage);
	    }

	    let bg = this.getBackgroundColor();
	    if(bg) {
		this.globe.backgroundColor(bg);
	    }

	    try {
		let canvas = this.jq(ID_GLOBE).find('canvas');
		canvas.attr('tabindex','1');
		canvas.mouseover(()=>{
		    this.mouseOver = true;
		});
		canvas.mouseout(()=>{
		    this.mouseOver = false;
		});		

		domGlobe.addEventListener('keydown', (e) => {
		    if(e.code=="KeyD") {
			//debug
			this.listScene();
			return
		    }

		    if(e.code=="KeyP") {
			let name = prompt("Name:");
			if(!name) return;
			let attrs = ["x","y","z"];
			let  pos =  "{position: {";
			attrs.forEach((a,idx)=>pos+=(idx>0?",":"") + a+":" + this.getControls().object.position[a]);
			    pos+="},\nup: {";
			attrs.forEach((a,idx)=>pos+=(idx>0?",":"") + a+":" + this.getControls().object.up[a]);
			pos+="}}";
			let state = '"'+name+'":' + pos;
			console.log(state);
			Utils.copyToClipboard(pos.replace(/\n/g,""));
		    }

		    if(e.code=="KeyR") {
			let pos = positions[this.getInitialPosition() || "North America"] || this.getInitialPosition();
			if(pos) {
			    this.setPosition(pos);
			}
		    }
		});
	    } catch(err) {
		console.error("Error creating trackball control:" + err);
		console.log("ctor:");console.log(THREE.TrackballControls);
		console.error(err.stack);
	    }

	    let handleMouseEvent=this.handleMouseEvent = object=>{
		this.jq(ID_POPUP).hide();
		let record = object.record;
		if(!record) return;
		this.propagateEventRecordSelection({record: record})
		this.showRecord(record);
	    };


	    if(this.getProperty("geojson")) {
		let nameField = this.getFieldById(null, this.getPolygonNameField());
		if(!nameField) {
		    this.addGeojsonLayer();
		}
	    }

	    this.globe.onPointClick(handleMouseEvent);
	    this.globe.onArcClick(handleMouseEvent);
	    this.globe.onPathClick(handleMouseEvent);
	    this.globe.onLabelClick(handleMouseEvent);
	    this.globe.onGlobeClick(()=>{this.jq(ID_POPUP).hide();});
	    let linked  = this.getLinked();
	    let linkGroup  = this.getLinkGroup();
	    this.globe.onZoom(()=>{
		//Only propagate zoom if its from the user
		if(!this.mouseOver) return;
		if(this.zooming) return;
		let globeDisplays = 
		    Utils.displaysList.filter(d=>{
			if(!d.getId) {
			    console.dir(d);
			    return false;
			}
			if(d.getId() == this.getId()) return false;
			if(d.type!=DISPLAY_THREE_GLOBE) return false;
			if(!d.getLinked()) return false;
			if(!d.globe) return false;
			if(d.getLinkGroup() && this.getLinkGroup() && d.getLinkGroup()!=this.getLinkGroup()) return false;
			if(d.zooming) return false;
			return true;
		    });
		this.zooming = true;
		globeDisplays.forEach(d=>{
		    let pos =  this.getControls().object.position;
		    d.zooming = true;
		    d.getControls().object.position.set(pos.x,pos.y,pos.z);		    
		    d.zooming = false;
		});
		this.zooming = false;
	    });


	    if(this.getInitialPosition()) {
		let posArg = this.getInitialPosition();
		let pos = positions[posArg];
		if(!pos && posArg.startsWith("{")) {
		    pos = this.parsePosition(posArg);
		}

		if(!pos) {
		    console.error("Unknown initial position:" + this.getInitialPosition());
		    return;
		}
		this.setPosition(pos);
	    }

	    if(this.getAutoRotate()) {
		this.getControls().autoRotate = true;
//		console.dir(this.getControls())
	    }

	    this.callHook("createGlobe");
	},

	parsePosition: function(pos) {
	    if(pos && (typeof pos=="string") && pos.startsWith("{")) {
		//A hack to wrap keys with quotes
		pos = pos.replace("position","\"position\"").replace("up","\"up\"").replace(/x/g,"\"x\"").replace(/y/g,"\"y\"").replace(/z/g,"\"z\"");
		try {
		    return JSON.parse(pos);
		} catch(err) {
		    console.err("Error parsing position:" + err+"\n" + pos);
		}
	    }
	    return null;
	},
	showRecord: function(record) {
	    if(this.getDoPopup()) {
		let html = this.getRecordHtml(record,null,this.getProperty("tooltip"));
		this.jq(ID_POPUP).html(html);
		this.jq(ID_POPUP).show(1000);
		return;
	    }
	    if(this.getSelectedDiv()) {
		let html = this.getRecordHtml(record,null,this.getProperty("tooltip"));
		$("#" + this.getSelectedDiv()).html(html);
	    }
	},

	addGeojsonLayer:function(records) {
	    let nameField = this.getFieldById(null, this.getPolygonNameField());
            let colorBy = null;
	    if(records) colorBy = this.getColorByInfo(records);
	    let url = this.getMapUrl(this.getProperty("geojson"));
	    let strokeColor= this.getMapColor();
	    $.getJSON(url, json=>{
		//		    console.log(json.features.length);
		//		    json.features.forEach(f=>{console.log(f.properties.NAME);});
		let aliases = {
		    "united states of america":"united states" ,
		    "czechia":"czech republic",
		    'swaziland':'eswatini',
		    'south korea':'korea (rep.)',
		    'north korea':'korea (dem. people s rep.)',
		    'eq. guinea':'guinea',
		    'gambia':'gambia the',
		    'congo':'congo (rep.)',
		    'democratic republic of the congo':'congo (dem. rep.)',
		    'ivory coast':'cote d\'ivoire'
		}
		let nameMap = {};
		if(nameField) {
		    records.forEach(record=>{
			let name = nameField.getValue(record);
//			console.log("name:" +name);
			nameMap[name] = record;
			nameMap[name.toLowerCase()] = record;
			nameMap[name.toUpperCase()] = record;			    			    
		    });
		}
		let logCnt = 0;
		json.features.forEach(f=>{
		    if(!f.properties) {
			if(logCnt++<50)
			    console.error("No properties in feature");
			return;
		    }
		    let seen = {};
		    let names = [f.properties.SOVEREIGNT, f.properties.name, f.properties.NAME, f.properties.ADMIN].filter(name=>{
			if(seen[name])return false;
			seen[name] = true;
			return name});
		    if(names.length==0) {
			if(logCnt++<50)
			    console.log("Could not find name in feature:" +JSON.stringify(f.properties));
			return;
		    }
		    let record = null;
		    names.every(name=>{
			record=nameMap[name];
			if(record) return false;
			return true;
		    });

		    if(!record) {
			names.every(name=>{
			    let alias = aliases[name.toLowerCase()];
			    if(!alias) return true;
 			    record = nameMap[alias];
			    if(record) return false;
			    return true;
			});
		    }

		    if(!record) {
//			this.handleLog("Could not find record for feature:" +names);
			return;
		    }
		    f.record=record;
		    if(colorBy && colorBy.isEnabled()) {
			f.color = colorBy.getColorFromRecord(record, null);
		    }
		});

		let alt = this.getPolygonAltitude();
		this.globe.polygonsData(json.features)
		    .polygonStrokeColor(()=>strokeColor)
		    .polygonCapColor((f)=>f.color || "transparent")
		    .polygonSideColor((f)=>f.color||"transparent")		    
		    .polygonAltitude(alt);


		if(nameField) {
		    let tooltip = this.getProperty("tooltip");
		    if(Utils.stringDefined(tooltip)) {
			this.globe.polygonLabel(f=>{
			    if(!f.record) return null;
			    let html =  this.getRecordHtml(f.record,null,this.getProperty("tooltip"));
			    html = HU.div([CLASS,"display-three-globe-popup",'style',this.getProperty('popupStyle','')], html);
			    return html;
			});
		    }

		    this.globe.onPolygonHover(
			hoverD => {
			    this.globe
				.polygonAltitude(d => d === hoverD ? 0.08 : alt)
			})
			    .polygonsTransitionDuration(300);
		    //				.polygonCapColor(d => d === hoverD ? 'steelblue' : d.color)
		}

	    }).fail(err=>{
		console.error("failed to load json:" + url);
	    });
	    if(nameField) {
		this.globe.onPolygonClick(this.handleMouseEvent);
	    }
	},
	getImageUrl:function(image) {
	    if(!image) return null;
	    if(!image.startsWith("http") && !image.startsWith("/")) image = ramaddaBaseHtdocs+"/images/maps/" + image;
	    return image;
	},
	getMapUrl:function(url) {
	    if(!url) return null;
	    if(!url.startsWith("http") && !url.startsWith("/")) {
		//entry id e.g., 41d9b105-d61b-4fc1-8198-8e75c49b1a24
		if(url.trim().match(/.*[0-9a-z]+-[0-9a-z]+-[0-9a-z]+-[0-9a-z]+-[0-9a-z]+.*/)) {

		    url = ramaddaBaseUrl +'/entry/get?entryid=' + url;
		} else {
		    url = ramaddaBaseHtdocs+"/resources/" + url;
		}
	    }
	    return url;
	},	
        handleEventRecordSelection: function(source, args) {
	    SUPER.handleEventRecordSelection.call(this, source, args);
	    let record = args.record;
	    this.globe.pointOfView({lat: record.getLatitude(),lng:record.getLongitude(),alt:10000});
	    let coords = this.globe.getCoords(args.record.getLatitude(),args.record.getLongitude());
	    if(this.selectedObjects) {
		this.selectedObjects.forEach(object=>{
		    if(!object.material) return;
		    object.material.color.setHex(object.__oldcolor);
		});
	    }
	    let map = {};
	    this.getDataObjects(map);
	    let objects = map[args.record.getId()];
	    this.selectedObjects = objects;
	    if(!objects) return;
	    objects.forEach(object=>{
		if(!object.material) return
		object.__oldcolor =  object.material.color.getHex();
		object.material.color.setRGB(1,0,0);
	    });

	}
    });
}


function RamaddaThree_gridDisplay(displayManager, id, properties) {
    const ID_CONTAINER = "container";
    const ID_GRID = "grid";
    const ID_POPUP = "popup";
    const ID_POSITION_BUTTON = "positionbutton";        
    const CAMERA_ANGLE = 45;
    let myProps = [
        {label:'3D Grid Attributes'},
	{p:"gridWidth",d:400},
	{p:"gridHeight",d:400},
	{p:'backgroundColor',d:'#CAE1FF',ex:'#ffffff'},
	{p:'canvasBorder',d:'1px solid #ccc'},
	{p:'shape',d:"box",ex:'box|cylinder'},
	{p:'heightField',tt:'field to scale height by'},
	{p:'heightScale',d:10,tt:'scale factor'},	
	{p:'doPopup',d:true,ex:'',tt:''},
	{p:'doImages',ex:true},
    ];
    const SUPER = new RamaddaThree_Base(displayManager, id, DISPLAY_THREE_GRID, properties);
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
        needsData: function() {
            return true;
        },
        initDisplay:  function() {
            SUPER.initDisplay.call(this);
        },
	dataFilterChanged: function(args) {
	    SUPER.dataFilterChanged.call(this,args);
	},
        updateUI: async function() {
            if(!ramaddaLoadedThree) {
                ramaddaLoadedThree = true;
		await Utils.importJS(ramaddaBaseHtdocs+"/lib/three/three.min.js");
		await Utils.importJS(ramaddaBaseHtdocs+"/lib/three/controls/OrbitControls.js");
            }
	    if(!window["THREE"]) {
		setTimeout(()=>{this.updateUI()},100);
		return
	    }	    
	    if(!THREE.OrbitControls) {
		setTimeout(()=>{this.updateUI()},100);
		return
	    }

            SUPER.updateUI.call(this);
	    this.jq(ID_POPUP).hide();
	    if(!this.shapes) {
		this.createScene();
	    }

	    this.shapes.forEach(shape=>{
		this.scene.remove(shape);
	    });

	    let records =this.filterData();
	    if(!records) return;
	    this.filteredRecords = records;

//	    records = [...records,...records,...records,...records,...records]
//	    records = [...records,...records,...records,...records,...records]	    



	    let sqrt = Math.ceil(Math.sqrt(records.length));
	    let cubeWidth = 1;
	    let cubeSpace = cubeWidth/2;	    
	    let rectWidth = sqrt*(cubeWidth+cubeSpace);
	    let topRadius = cubeWidth;
	    let bottomRadius = cubeWidth;	    

	    if(!this.initCamera) {
		this.initCamera = true;
		let h = (rectWidth/2)/Math.tan(Utils.toRadians(CAMERA_ANGLE/2));
		this.camera.position.set(0,0,h*2);
//		this.addChecker(rectWidth);
	    }
	    let initX = ((cubeWidth+cubeSpace)*-sqrt/2);
	    let initY = -initX;
	    let x = initX;
	    let y= initY;
	    let colCnt = 0;

            let colorBy = this.getColorByInfo(records);
	    let colorBys = [];
	    if(this.colorByFields) {
		let fields = this.getFields();
		this.colorByFields.forEach(field=>{
//		    function ColorByInfo(display, fields, records, prop,colorByMapProp, defaultColorTable, propPrefix, theField, props,lastColorBy) {
		    let cb = new ColorByInfo(this,fields,records,null,null,null,null,field);
		    colorBys.push(cb);
		})
	    }
	    let bounds = RecordUtil.getBounds(records);

	    let heightBy;
	    let heightScale = this.getHeightScale();
	    if(this.getProperty("heightField")) {
		heightBy = new SizeBy(this, this.getProperty("sizeByAllRecords",true)?this.getData().getRecords():records,"heightField");
	    }

	    if(colorBys.length==0) colorBys=[colorBy];
	    colorBys=[colorBy];	    
	    let doImages = this.getDoImages();
	    let imageFields = this.getFieldsByType(null,"image");
	    const loader = new THREE.TextureLoader();
	    let shape  = this.getShape();
	    shapeWidth = 0.5;
	    let doGeo = false;
	    records.forEach((record,idx)=>{	
		if(x>-initX) {
		    y-=(cubeWidth+cubeSpace);
		    x  = initX;
		}

		if(doGeo) {
		    let percentX = (record.getLongitude()-bounds.west)/bounds.getWidth();
		    x = initX+percentX*rectWidth;
		    let percentY = (record.getLatitude()-bounds.south)/bounds.getHeight();
		    y = (initY+percentY*rectWidth/this.aspectRatio)-rectWidth/this.aspectRatio
		}
		
		if(doImages) {
		    let geometry = new THREE.BoxGeometry(cubeWidth,cubeWidth,cubeWidth);
		    const materials = [];
		    imageFields.forEach(f=>{
			let image = f.getValue(record);
//			image = "https://ramadda.org/repository/metadata/view/Screenshot_2021-10-19_at_13-51-39_Point_Data_Collection.png?element=1&entryid=90e2c8e8-7e24-4f6b-9f0c-134fbd690999&metadata_id=b34d307a-7e7c-4a62-8c1e-1e1cd5637b2b";
//			image = 'https://localhost:8430/repository/images/logo.png';
			if(Utils.stringDefined(image)) {
			    materials.push(new THREE.MeshBasicMaterial({map: loader.load(image)}));
			}
		    });
		    let cube = new THREE.Mesh(geometry, materials);
		    cube.position.x = x;
		    cube.position.y = y;		
		    this.scene.add(cube);
		    this.shapes.push(cube); 
		} else {
		    let materials = [];
/*
		    for(let i=0;i<colorBys.length;i++) {
			let color = colorBys[i].getColorFromRecord(record, null);
			let material = new THREE.MeshLambertMaterial( { color: this.parseInt(color,0xff0000) } );
			materials.push(this.parseInt(color,0xff0000));
		    }
*/
		    let color = colorBy.getColorFromRecord(record, null);
		    let material = new THREE.MeshLambertMaterial( { color: this.parseInt(color,0xff0000) } );
		    let height = cubeWidth;
		    if(heightBy) {
			let percent;
			let func = perc =>{percent = perc;}
			let h = heightBy.getSize(record.getData(),0,func);
			if(!isNaN(percent))
			    height = height+percent*height*heightScale;
		    }

		    let geometry;
		    if(shape=="box") {
			geometry = new THREE.BoxGeometry(shapeWidth,shapeWidth,height);
		    } else {
			geometry = new THREE.CylinderGeometry(topRadius,bottomRadius,height,32);
		    }
		    let cube = new THREE.Mesh(geometry, material);
		    if(shape=="box") {
		    } else {
			cube.rotation.x = Utils.toRadians(90);
		    }
//		    cube.position.set(x,y,0);
		    cube.position.x=x;
		    cube.position.y = y;		
		    cube.position.z = height/2;
		    this.scene.add(cube);
		    cube.__record = record;
		    this.shapes.push(cube);
		}
		x+=cubeWidth+cubeSpace;
	    });
	    /*
	    var geometry = new THREE.CylinderBufferGeometry( 0, 10, 30, 4, 1 );
	    var material = new THREE.MeshPhongMaterial( { color: 0xffffff, flatShading: true } );
	    for ( var i = 0; i < 500; i ++ ) {
		var mesh = new THREE.Mesh( geometry, material );
		mesh.position.x = ( Math.random() - 0.5 ) * 1000;
		mesh.position.y = ( Math.random() - 0.5 ) * 1000;
		mesh.position.z = ( Math.random() - 0.5 ) * 1000;
		mesh.updateMatrix();
		mesh.matrixAutoUpdate = false;
		this.scene.add( mesh );
	    }
	    */

	    if(colorBy.isEnabled()) {
		colorBy.displayColorTable();
	    }
	    this.callHook("updateUI");
        },
	addChecker:function(width) {
	    const planeSize = width*1.1;
	    const loader = new THREE.TextureLoader();
	    const texture = loader.load('https://threejsfundamentals.org/threejs/resources/images/checker.png');
	    texture.wrapS = THREE.RepeatWrapping;
	    texture.wrapT = THREE.RepeatWrapping;
	    texture.magFilter = THREE.NearestFilter;
	    const repeats = planeSize / 2;
	    texture.repeat.set(repeats, repeats);
	    const planeGeo = new THREE.PlaneGeometry(planeSize, planeSize);
	    const planeMat = new THREE.MeshPhongMaterial({
		map: texture,
		side: THREE.DoubleSide,
	    });
	    const mesh = new THREE.Mesh(planeGeo, planeMat);
//	    mesh.rotation.x = Math.PI * -.5;
	    this.scene.add(mesh);
	},

	createScene: function() {
	    let popup = HU.div([CLASS,"display-three-globe-popup",ID,this.domId(ID_POPUP),STYLE,HU.css("display","none","position","absolute","left","60%","top","0px")],"");
	    let grid = HU.div([STYLE,HU.css("position","relative")],
			      popup +
			      HU.div([STYLE,HU.css("min-width","200px","min-height","200px"), ID, this.domId(ID_GRID)]));
	    let html = HU.center(grid);
	    this.setContents(html);
	    this.scene = new THREE.Scene();
	    //fov,aspect ratio, near plane, far plane
	    let w = this.getGridWidth();
	    let h  = this.getGridHeight();
	    this.aspectRatio = w/h;
	    const AMOUNT = 6;
	    const WIDTH = ( w / AMOUNT ) * window.devicePixelRatio;
	    const HEIGHT = ( h / AMOUNT ) * window.devicePixelRatio;

	    this.camera = new THREE.PerspectiveCamera(CAMERA_ANGLE,w/h,0.1,1000);
	    this.camera = new THREE.OrthographicCamera( -100,100,100,-100, 0.1, 1000 );
	    this.camera.position.set(0,0,100);

	    var axes = new THREE.AxisHelper(1000);            this.scene.add(axes);

	    this.renderer = new THREE.WebGLRenderer({antialias:true,alpha: true});
	    this.renderer.setClearColor(this.getBackgroundColor());
	    this.renderer.setSize(w,h);
	    this.controls = new THREE.OrbitControls( this.camera, this.renderer.domElement );
	    this.controls.minDistance = 0;
	    this.controls.maxDistance = 1000;
	    this.controls.target.set( 0, 0, 5 );
	    this.controls.update();
	    this.jq(ID_GRID).append(this.renderer.domElement);
	    
	    let addLight=(v,x,y,z,i) =>{
		i=0.01;
		if(!Utils.isDefined(i)) i=1;
//		var light = new THREE.PointLight(this.parseInt(v),i);
		let light = new THREE.DirectionalLight(this.parseInt(v));
		light.position.set(x,y,z);
		this.getScene().add(light);
	    }
	    addLight(0xffffff,-1,1,1);
//	    addLight(0xffffff,1,1,1);
//	    addLight(0xffffff,-1,-1,1);
//	    addLight(0xffffff,1,-1,1);	    
	    var light = new THREE.AmbientLight( 0xffffff,0.5);
	    this.scene.add( light );
	    light = new THREE.HemisphereLight( 0xffffff,0xffffbb,1.0);
//	    this.scene.add( light );	    


	    let _this = this;
	    let canvas = this.jq(ID_GRID).find('canvas');
	    canvas.attr('tabindex','1');
	    canvas.css("border",this.getCanvasBorder());
	    this.renderer.domElement.addEventListener('keydown', (e) => {
		    if(e.code=="KeyP") {
			let name = prompt("Name:");
			if(!name) return;
			let attrs = ["x","y","z"];
			let  pos =  "{position: {";
			attrs.forEach((a,idx)=>pos+=(idx>0?",":"") + a+":" + this.getControls().object.position[a]);
			    pos+="},\nup: {";
			attrs.forEach((a,idx)=>pos+=(idx>0?",":"") + a+":" + this.getControls().object.up[a]);
			pos+="}}";
			let state = '"'+name+'":' + pos;
			Utils.copyToClipboard(pos.replace(/\n/g,""));
		    }

		    if(e.code=="KeyR") {
			_this.controls.reset();
//			let pos = positions[this.getInitialPosition() || "North America"];
//			if(pos) {			    this.setPosition(pos);			}
		    }
		});



	    let cnt = 0;
	    let handleMouseEvent=event=>{
		this.jq(ID_POPUP).hide();
		event.preventDefault();
		if(!event.shiftKey || event.which != 1)  return;
		let r = new THREE.Raycaster();
		let mouse = {
		    x: ( event.offsetX / w) * 2 - 1,
		    y : - ( event.offsetY / h) * 2 + 1
		};
		r.setFromCamera( mouse, this.camera ); 
		let intersects = r.intersectObjects(this.shapes,true);
		if(intersects.length==0) {
		    console.log("nothing found");
		    return;
		}
		let minDistance = NaN;
		let minObject = null;
		intersects.forEach(o=>{
		    if(minObject==null || minDistance>o.distance) {
			minObject = o.object;
			minDistance = o.distance;
		    }
		});
//		console.dir(minObject);
		let getRecord = o=>{
		    if(o.__record) return o.__record;
		    if(o.parent) return getRecord(o.parent);
		    return null;
		};
		let record = getRecord(minObject);
		if(!record) {
		    console.log("Could not find record");
		    return;
		}
		this.propagateEventRecordSelection({record: record})
		if(this.getDoPopup()) {
		    let html = this.getRecordHtml(record);
		    this.jq(ID_POPUP).html(html);
		    this.jq(ID_POPUP).show(1000);
		    return;
		}
		if(this.getSelectedDiv()) {
		    let html = this.getRecordHtml(record);
		    $("#" + this.getSelectedDiv()).html(html);
		}
	    };
	    this.renderer.domElement.addEventListener( 'mouseup', handleMouseEvent, false );
	    _this.renderer.render( _this.scene, _this.camera );
	    this.shapes = [];
	    if(!this.animating) {
		this.animating = true;
		function animate() {
		    requestAnimationFrame( animate );
		    _this.controls.update();
		    _this.shapes.forEach((shape,idx)=>{
//			shape.rotation.x+=0.01
//			shape.rotation.y+=0.01
//			if(idx==0) console.log(shape.rotation.x);
		    });
		    _this.renderer.render( _this.scene, _this.camera );
		}
		animate();
	    }

	}	    
    });
}

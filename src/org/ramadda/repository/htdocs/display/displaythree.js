/**
   Copyright 2008-2021 Geode Systems LLC
*/


const DISPLAY_THREE_GLOBE = "three_globe";
addGlobalDisplayType({
    type: DISPLAY_THREE_GLOBE,
    forUser: true,
    label: "3D Globe",
    requiresData: true,
    category: CATEGORY_MAPS
});

var ramaddaLoadedThree=false;

function RamaddaThree_globeDisplay(displayManager, id, properties) {
    const ID_CONTAINER = "container";
    const ID_GLOBE = "globe";
    const ID_POPUP = "popup";
    const ID_POSITION_BUTTON = "positionbutton";        
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
	{p:"baseImage",d:"earth-blue-marble.jpg",ex:"earth-blue-marble.jpg|earth-day.jpg|earth-dark.jpg|caida.jpg|white.png|lightblue.png|black.png"},
	{p:"initialPosition",ex:"North America|South America|Europe|Asia|Africa|Australia|South Pole|North Pole"},
	{p:'showGraticules',ex:true},
	{p:'showAtmosphere',d:true,ex:'false'},
	{p:'atmosphereColor',d:'#fff',ex:'red'},	    	    
	{p:'atmosphereAltitude',d:0.25,ex:0.5},
	{p:'backgroundColor',d:'CAE1FF',ex:'ffffff'},
	{p:'ambientLight',d:'ffffff',ex:'0000ff'},
	{p:'initialAltitude',d:250,ex:500},
	{p:'color',d:'blue',ex:'red'},
	{p:'radius',d:1,ex:'1'},
	{p:'heightField',tt:'field to map height to'},
	{p:'heightMin',d:0,tt:'min height range that heightField value percent is mapped to'},
	{p:'heightMax',d:0.5},
	{p:'radiusField',tt:'field to map radius to'},
	{p:'radiusMin',d:1},
	{p:'radiusMax',d:5},
	{p:'showSpheres',ex:true},			
	{p:'selectedDiv',ex:'div id to show selected record'},
	{p:'doPopup',d:true,ex:'',tt:''},		
    ];
    const SUPER = new RamaddaDisplay(displayManager, id, DISPLAY_THREE_GLOBE, properties);
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
        needsData: function() {
            return true;
        },
        initDisplay:  function() {
            SUPER.initDisplay.call(this);
        },
        updateUI: async function() {
            if(!ramaddaLoadedThree) {
                ramaddaLoadedThree = true;
		await Utils.importJS("//unpkg.com/three");
//		await Utils.importJS("//unpkg.com/three/examples/js/controls/TrackballControls.js");
		await Utils.importJS(ramaddaBaseUrl+"/htdocs_v_" + new Date().getTime()+"/lib/three/TrackballControls.js");
		await Utils.importJS("//unpkg.com/three-globe");
            }
	    if(!window["THREE"]) {
		setTimeout(()=>{this.updateUI()},100);
		return
	    }
	    if(!THREE.TrackballControls) {
		setTimeout(()=>{this.updateUI()},100);
		return
	    }
	    if(!window["ThreeGlobe"]) {
		setTimeout(()=>{this.updateUI()},100);
		return
	    }	    
            SUPER.updateUI.call(this);
	    let records =this.filterData();
	    if(!records) return;


	    if(!this.globe) {
		this.createGlobe();
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
		if(!radiuss || !record) return this.getRadius();
		let v = radiusField.getValue(record);
		let percent = (v-radiuss.min)/(radiuss.max-radiuss.min);
		let radius = radiusMin+percent*(radiusMax-radiusMin);
		return radius;
	    }



	    let polygonField = this.getFieldById(null, this.getProperty("polygonField"));
	    if(polygonField) {
		let polygonColorTable = this.getColorTable(true, "polygonColorTable",null);
		let map = new RepositoryMap();
		let delimiter;
		let first = !this.didit;
		if(!this.didit) {
		    this.didit = true;
//		    for(a in this.globe) console.log(a);
		}
		let latlon = this.getProperty("latlon",true);
		let pathData = [[
		    [ 17.254821446627215, -62.92306995333236, 0 ],
		    [ 17.091324519936318, -63.58157397058453, 0 ],
		    [ 17.456562781891563, -63.65027719398423, 0 ],
		    [ 16.803238429608967, -63.37079745818032, 0 ],
		    [ 16.99651000078848, -62.93961166380253, 0 ],
		    [ 16.607371880158045, -62.14284965800188, 0 ],
		    [ 15.87187644791576, -62.74449210722255, 0 ]]];
		pathData = [];
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
				radius:this.getRadius(),
				record:record,
			    };
			    pointData.push(pt);
			}
		    }
		});
		if(first) {
//		    for(a in this.globe) console.log(a);
//		    console.log(pathData);
		}
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

	    if(pointData.length>0) {
		if(this.getShowSpheres(true)) {
		    this.globe.customLayerData(pointData)
			.customThreeObject(d => new THREE.Mesh(
			    new THREE.SphereBufferGeometry(d.radius),
			    new THREE.MeshLambertMaterial({ color: d.color })
			))
			.customThreeObjectUpdate((obj, d) => {
			    Object.assign(obj.position, this.globe.getCoords(d.lat, d.lng, d.height+0.01));
			});
		} else {
		  this.globe.pointsData(pointData)
			.pointAltitude('height')
			.pointColor('color')
			.pointRadius('radius');
		}
	    }




	    if(colorBy.isEnabled()) {
		colorBy.displayColorTable();
	    }
        },
	setPosition:function(pos) {
	    let scope = this.controls;
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
//	    scope.dispatchEvent( _changeEvent );
	    //lastPosition.copy( scope.object.position );
	    //lastZoom = scope.object.zoom;
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
		if(object.children)
		    object.children.forEach(f);
	    }
	    this.scene.children.forEach(f);
	    return dataObjects;
	},

	createGlobe:function() {
	    let _this = this;
	    let popup = HU.div([CLASS,"display-three-globe-popup",ID,this.domId(ID_POPUP),STYLE,HU.css("display","none","position","absolute","left","60%","top","0px")],"");
	    let pos = HU.div([TITLE,"Select Position", CLASS,"ramadda-clickable", ID,this.domId(ID_POSITION_BUTTON),STYLE,HU.css("position","absolute","left","10px","top","10px")],HU.getIconImage("fa-globe"));
	    let globe = HU.div([STYLE,HU.css("position","relative")],
			       pos +
			       popup +
			       HU.div([ID, this.domId(ID_GLOBE)]));
	    let html = HU.center(globe);
	    html  = globe;
	    this.setContents(html);
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
	    let image  = this.getBaseImage();



	    if(!image.startsWith("http") && !image.startsWith("/")) image = ramaddaBaseUrl+"/images/maps/" + image;
	    //Initial example code from https://github.com/vasturiano/three-globe
	    this.globe = new ThreeGlobe()
		  .globeImageUrl(image)	    
//		  .bumpImageUrl('//unpkg.com/three-globe/example/img/earth-topology.png')
		  .showGraticules(this.getShowGraticules())
		  .showAtmosphere(this.getShowAtmosphere())
		  .atmosphereColor(this.getAtmosphereColor())	    	    
		  .atmosphereAltitude(this.getAtmosphereAltitude())	    	    

	    // Setup renderer
	    let w  = this.getGlobeWidth();
	    let h = this.getGlobeHeight();
	    const renderer = new THREE.WebGLRenderer();
	    renderer.setSize(w,h);
	    document.getElementById(this.domId(ID_GLOBE)).appendChild(renderer.domElement);

	    // Setup scene
	    this.scene = new THREE.Scene();
	    this.scene.add(this.globe);
	    let light = this.getAmbientLight();
	    if(!light.startsWith("0x")) light = '0x' + light;
	    this.scene.add(new THREE.AmbientLight(parseInt(Number(light), 10)));
	    this.scene.add(new THREE.DirectionalLight(0xffffff, 0.6));

	    let bg = this.getBackgroundColor();
	    if(!bg.startsWith("0x")) bg = '0x' + bg;	    
	    this.scene.background = new THREE.Color(parseInt(Number(bg), 10));

	    // Setup camera

	    this.camera = new THREE.PerspectiveCamera();
	    this.camera.aspect = w/h;
	    this.camera.updateProjectionMatrix();
	    this.camera.position.z = +this.getInitialAltitude();


	    let controls;
	    // Add camera controls
	    try {
		this.controls = controls = new THREE.TrackballControls(this.camera, renderer.domElement);
		controls.minDistance = 101;
		controls.rotateSpeed = 5;
		controls.zoomSpeed = 0.8;
		let canvas = this.jq(ID_GLOBE).find('canvas');
		canvas.attr('tabindex','1');
		renderer.domElement.addEventListener('keydown', (e) => {
		    if(e.code=="KeyL") {
			let name = prompt("Name:");
			if(!name) return;
			let attrs = ["x","y","z"];
			let state = '"'+name+'":'+ "{\nposition: {";
			attrs.forEach((a,idx)=>state+=(idx>0?",":"") + a+":" + this.controls.object.position[a]);
			    state+="},\nup: {";
			    attrs.forEach((a,idx)=>state+=(idx>0?",":"") + a+":" + this.controls.object.up[a]);
			    state+="}\n},";
			    console.log(state);
		    }

		    if(e.code=="KeyR") {
			//console.log(controls.scope);
			if(this.getInitialPosition()) {
			    let pos = positions[this.getInitialPosition()];
			    if(pos) {
				this.setPosition(pos);
				return;
			    }
			}
			controls.reset();
		    }
		});
	    } catch(err) {
		console.error("Error creating trackball control:" + err);
		console.log("ctor:");console.log(THREE.TrackballControls);
		console.error(err.stack);
	    }

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
		let intersects = r.intersectObjects(this.getDataObjects(),true);
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
		let getData = o=>{
		    if(o.__data) return o.__data;
		    if(o.parent) return getData(o.parent);
		    return null;
		};
		let data = getData(minObject);
		if(!data) {
		    console.log("Could not find data");
		    return;
		}
		let record = data.record;
		this.propagateEventRecordSelection({record: record})
//		let v3 = new THREE.Vector3(0,1,0);
//		this.controls.target  =v3;
//		this.controls.update();
		

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
	    renderer.domElement.addEventListener( 'mouseup', handleMouseEvent, false );

	    if(this.getInitialPosition()) {
		let pos = positions[this.getInitialPosition()];
		if(!pos) {
		    console.error("Unknown initial position:" + this.getInitialPosition());
		    return;
		}
		this.setPosition(pos);
	    }



	    let globeImage = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAyAAAABzCAYAAABggmasAAAGnUlEQVR4nO3YoZKUVxSF0XmRuMjISFzkqLxBJBKJiyQKiaIqBomkMAgEjopCIZGRcZGdJ8i9+9Y+0E3NWlXbd89M93++ubvjpry93L2f3F8PbJe/h/d6eC+H92x4b4b3YXifhvdxeNPv9zK76c/bH8Obfr+Xf4c3/Pd367+Paz8PAfhGBIgAESCLCRABcjIBIkAA2BMgAkSALCZABMjJBIgAAWBPgAgQAbKYABEgJxMgAgSAPQEiQATIYgJEgJxMgAgQAPYEiAARIIsJEAFyMgEiQADYEyACRIAsJkAEyMkEiAABYE+ACBABspgAESAnEyACBIA9ASJABMhiAkSAnEyACBAA9gSIABEgiwkQAXIyASJAANgTIAJEgCwmQATIyQSIAAFgT4AIEAGymAARICcTIAIEgD0BIkAEyGICRICcTIAIEAD2BIgAESCLCRABcjIBIkAA2BMgAkSALCZABMjJBIgAAWBPgAgQAbKYABEgJxMgAgSAPQEiQATIYgJEgJxMgAgQAPYEiAARIIsJEAFyMgEiQADYEyACRIAsJkAEyMkEiAABYE+ACBABspgAESAnEyACBIC9Py937yd37SDYBsP0gfDP8D4P78XwHg/vyfDuh/fb8F4NbzpAvgxv+vN268Ew/Q+J6WAdDpprP78A+E4JkHICRIAIEAEiQAAgJ0DKCRABIkAEiAABgJwAKSdABIgAESACBAByAqScABEgAkSACBAAyAmQcgJEgAgQASJAACAnQMoJEAEiQASIAAGAnAApJ0AEiAARIAIEAHICpJwAESACRIAIEADICZByAkSACBABIkAAICdAygkQASJABIgAAYCcACknQASIABEgAgQAcgKknAARIAJEgAgQAMgJkHICRIAIEAEiQAAgJ0DKCRABIkAEiAABgJwAKSdABIgAESACBAByAqScABEgAkSACBAAyAmQcgJEgAgQASJAACAnQMoJEAEiQASIAAGAnAApJ0AEiAARIAIEAK5n/CCffqBPH/jTr286GJ4O7+fh/TK8H4f36/CeD+/N8KYP3unP23QgDR/kl3fDez27az8fAOCrECDlBIgAESACRIAAQE6AlBMgAkSACBABAgA5AVJOgAgQASJABAgA5ARIOQEiQASIABEgAJATIOUEiAARIAJEgABAToCUEyACRIAIEAECADkBUk6ACBABIkAECADkBEg5ASJABIgAESAAkBMg5QSIABEgAkSAAEBOgJQTIAJEgAgQAQIAOQFSToAIEAEiQAQIAOQESDkBIkAEiAARIACQEyDlBIgAESACRIAAQE6AlBMgAkSACBABAgA5AVJOgAgQASJABAgA5ARIOQEiQASIABEgAJATIOUEiAARIAJEgABAToCUEyACRIAIEAECADkBUk6ACBABIkAECADkxg/y6QPh1gPk2fCmD+jpAPlpeD8Mb/r1/T686QD58MD26sYnQABgT4CUEyACRIAIEAECADkBUk6ACBABIkAECADkBEg5ASJABIgAESAAkBMg5QSIABEgAkSAAEBOgJQTIAJEgAgQAQIAOQFSToAIEAEiQAQIAOQESDkBIkAEiAARIACQEyDlBIgAESACRIAAQE6AlBMgAkSACBABAgA5AVJOgAgQASJABAgA5ARIOQEiQASIABEgAJATIOUEiAARIAJEgABAToCUEyACRIAIEAECADkBUk6ACBABIkAECADkBEg5ASJABIgAESAAkBMg5QSIABEgAkSAAEBOgJQTIAJEgAgQAQIAOQFSToAIEAEiQAQIAOQESDkBIkAEiAARIABwPeMBcu2D4hsfHJf74U0f5NNBM71Hw3s+vJfDe/HANv3zm95wYF77+xwAvgsCpJwAESACRIAIEADICZByAkSACBABIkAAICdAygkQASJABIgAAYCcACknQASIABEgAgQAcgKknAARIAJEgAgQAMgJkHICRIAIEAEiQAAgJ0DKCRABIkAEiAABgJwAKSdABIgAESACBAByAqScABEgAkSACBAAyAmQcgJEgAgQASJAACAnQMoJEAEiQASIAAGAnAApJ0AEiAARIAIEAHICpJwAESACRIAIEADICZByAkSACBABIkAAICdAygkQASJABIgAAYCcACknQASIABEgAgQAcgKknAARIAJEgAgQAMgJkHICRIAIEAEiQAAgJ0DKCRABIkAEiAABAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAID/8x8ldCfh8DwIowAAAABJRU5ErkJggg==";
//	    globeImage='//unpkg.com/three-globe/example/img/earth-water.png';

/*
	    const globeMaterial = this.globe.globeMaterial();
//	    globeMaterial.bumpScale = 10;
	    new THREE.TextureLoader().load(globeImage, texture => {
		console.log("loaded");
		globeMaterial.specularMap = texture;
//		globeMaterial.specular = new THREE.Color('grey');
		globeMaterial.shininess = 15;
	    });
*/




	    // Kick-off renderer
	    (function animate() { // IIFE
		// Frame cycle
		if(controls)
		    controls.update();
		renderer.render(_this.scene, _this.camera);
		setTimeout(()=>{
		    requestAnimationFrame(animate);
		},10);
	    })();
	},
        handleEventRecordSelection: function(source, args) {
	    SUPER.handleEventRecordSelection.call(this, source, args);
	    let coords = this.globe.getCoords(args.record.getLatitude(),args.record.getLongitude());
//	    this.controls.target  = new THREE.Vector3(coords.x,coords.y,coords.z);
//	    this.controls.update();

	    if(this.selectedObjects) {
		this.selectedObjects.forEach(object=>{
		    object.material.color.setHex(object.__oldcolor);
		});
	    }
	    let map = {};
	    this.getDataObjects(map);
	    let objects = map[args.record.getId()];
	    this.selectedObjects = objects;
	    if(!objects) return;
	    objects.forEach(object=>{
		object.__oldcolor =  object.material.color.getHex();
		object.material.color.setRGB(1,0,0);
	    });

	}
    });
}

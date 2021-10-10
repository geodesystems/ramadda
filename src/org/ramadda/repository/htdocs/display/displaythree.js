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
    let myProps = [
        {label:'3D Globe Attributes'},
	{p:"globeWidth",d:800},
	{p:"globeHeight",d:400},
	{p:"baseImage",d:"earth-blue-marble.jpg",ex:"earth-blue-marble.jpg|earth-day.jpg|earth-dark.jpg|caida.jpg|white.png|lightblue.png|black.png"},
	{p:'showGraticules'},
	{p:'showAtmosphere',d:true,ex:'false'},
	{p:'atmosphereColor',d:'#fff',ex:'red'},	    	    
	{p:'atmosphereAltitude',d:0.25,ex:0.5},
	{p:'backgroundColor',d:'CAE1FF',ex:'ffffff'},
	{p:'ambientLight',d:'ffffff',ex:'0000ff'},
	{p:'initialAltitude',d:250,ex:500},
	{p:'color',d:'blue',ex:'red'},
	{p:'radius',d:1,ex:'1'},
	{p:'heightMin',d:0},
	{p:'heightMax',d:0.5},	
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
		await Utils.importJS("//unpkg.com/three/examples/js/controls/TrackballControls.js");
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
	    let heightField = this.getFieldById(null, this.getProperty("heightField"));
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
			radius:this.getRadius(),
			record:record,
		    };
		    pointData.push(pt);
		});

	    }

	    if(pointData.length>0) {
		this.globe.pointsData(pointData)
		    .pointAltitude('height')
		    .pointColor('color')
		    .pointRadius('radius');
	    }
		

	    if(colorBy.isEnabled()) {
		colorBy.displayColorTable();
	    }
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
	    let popup = HU.div([CLASS,"display-three-globe-popup",ID,this.domId(ID_POPUP),STYLE,HU.css("display","none","position","absolute","left","60%","top","0px")],"");
	    let globe = HU.div([STYLE,HU.css("position","relative")],
			       popup +
			       HU.div([ID, this.domId(ID_GLOBE)]));
	    let html = HU.center(globe);
	    this.setContents(html);
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
	    let _this = this;
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
		    if(e.code=="KeyR") {
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

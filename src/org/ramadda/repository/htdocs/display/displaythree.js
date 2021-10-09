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
    const ID_GLOBE = "globe";
    let myProps = [
        {label:'3D Globe Attributes'},
	{p:"globeWidth",d:800},
	{p:"globeHeight",d:400},
	{p:"baseImage",d:"earth-blue-marble.jpg",ex:"earth-blue-marble.jpg|earth-day.jpg|earth-dark.jpg|caida.jpg"},
	{p:'showGraticules'},
	{p:'showAtmosphere',d:true,ex:'false'},
	{p:'atmosphereColor',d:'#fff',ex:'red'},	    	    
	{p:'atmosphereAltitude',d:0.25,ex:0.5},
	{p:'backgroundColor',d:'CAE1FF',ex:'ffffff'},
	{p:'ambientLight',d:'ffffff',ex:'0000ff'},
	{p:'initialAltitude',d:250,ex:500},
	{p:'color',d:'blue',ex:'red'},
	{p:'radius',d:1,ex:'1'},
	{p:'selectedDiv',ex:'div id to show selected record'},	
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
	    let gData = [];
	    records.forEach((record,idx)=>{
		let pt = {
		    lat:record.getLatitude(),
		    lng:record.getLongitude(),		    
		    color:colorBy.getColorFromRecord(record, dfltColor),
		    height:0,
		    radius:this.getRadius(),
		    record:record,
		};
		gData.push(pt);
	    });


	    let pts = this.globe.pointsData(gData)
		.pointAltitude('height')
		.pointColor('color')
		.pointRadius('radius');

	    if(colorBy.isEnabled()) {
		colorBy.displayColorTable();
	    }

        },
	createGlobe:function() {
	    let html = HU.center(HU.div([ID, this.domId(ID_GLOBE)]));
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
	    const scene = new THREE.Scene();
	    scene.add(this.globe);
	    let light = this.getAmbientLight();
	    if(!light.startsWith("0x")) light = '0x' + light;
	    scene.add(new THREE.AmbientLight(parseInt(Number(light), 10)));
	    scene.add(new THREE.DirectionalLight(0xffffff, 0.6));

	    let bg = this.getBackgroundColor();
	    if(!bg.startsWith("0x")) bg = '0x' + bg;	    
	    scene.background = new THREE.Color(parseInt(Number(bg), 10));


	    // Setup camera
	    const camera = new THREE.PerspectiveCamera();
	    camera.aspect = w/h;
	    camera.updateProjectionMatrix();
	    camera.position.z = +this.getInitialAltitude();


	    let controls;
	    // Add camera controls
	    try {
		controls = new THREE.TrackballControls(camera, renderer.domElement);
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

	    let mouse=event=>{
		event.preventDefault();
		if(event.which != 3)  return;
		let r = new THREE.Raycaster();
		let mouse = {
		    x: ( event.offsetX / w) * 2 - 1,
		    y : - ( event.offsetY / h) * 2 + 1
		};
		r.setFromCamera( mouse, camera ); 
		let dataObjects = [];
		let f= (object,idx)=>{
		    if(object.__data && object.__data.record) dataObjects.push(object);
		    if(object.children)
			object.children.forEach(f);
		}
		scene.children.forEach(f);
		let intersects = r.intersectObjects(dataObjects,true);
		if(intersects.length==0) {
		    console.log("nothing found");
		    return;
		}
		if(intersects.length>0) {
		    let record = intersects[0].object.__data.record;
		    console.log("record:" + record);
		    this.propagateEventRecordSelection({record: record})
		    if(this.getSelectedDiv()) {
			let html = this.getRecordHtml(record);
			$("#" + this.getSelectedDiv()).html(html);
		    }
		}
	    };
	    renderer.domElement.addEventListener( 'mouseup', mouse, false );




	    // Kick-off renderer
	    (function animate() { // IIFE
		// Frame cycle
		if(controls)
		    controls.update();
		renderer.render(scene, camera);
		setTimeout(()=>{
		    requestAnimationFrame(animate);
		},10);
	    })();
	}
	    
    });
}

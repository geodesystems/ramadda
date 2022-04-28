
function Model3D(url,props) {

    console.log(url);
    if(!url) {
//	url = "/models/fbx/source/datsun240k.fbx";
//	url = "/models/mari/source/MARI.fbx";
    }


    this.opts = {
	background:'f4f4f4',
	width:600,
	height:400,
	showAxis:true,
	cameraAngle:75
    }
    $.extend(this.opts, props||{});
    this.divId = this.opts.id||'model';
    $("#"+ this.divId).css('background',"#"+this.opts.background);
    let loading = "Loading..." +"<br>" +
	HU.image(ramaddaBaseUrl  + "/icons/mapprogress.gif",['style',HU.css('width','10%')]);
    jqid(this.divId).html(HU.div(['style',HU.css('padding-top','30px','text-align','center')],loading));
    $.extend(this, {
	init:function() {
	    //Initial code from https://redstapler.co/add-3d-model-to-website-threejs/
	    let _this = this;
	    let scene = this.scene = new THREE.Scene();
            scene.background = new THREE.Color(Utils.hexStringToInt(this.opts.background));	    
	    
            this.renderer = new THREE.WebGLRenderer({antialias:true});
            this.renderer.setSize(this.opts.width,this.opts.height);

            let camera = this.camera = new THREE.PerspectiveCamera(this.opts.cameraAngle,this.opts.width/this.opts.height,1,100);
	    camera.position.set(0.1102351103028313,3.2204652449816744,17.05354206986939);

            let controls = new THREE.OrbitControls(camera,this.renderer.domElement );
            controls.addEventListener('change', (event)=>{
		this.renderer.render(scene,camera);
	    });
	    controls.target.set(0,1,0);
	    controls.update();


	    jqid(this.divId).mouseenter(function() {
		$(this).focus();
	    });
	    jqid(this.divId).keypress((event) =>{
		if(event.keyCode==118) {
		    this.visible = !this.visible;
		    this.object.visible = this.visible;
		    return
		}
		if(event.keyCode==100) {
//		    console.dir(camera);
//		    console.dir(controls);		    
//		    console.log("position=\"" +camera.position.x+","+camera.position.y+","+camera.position.z+"\"");
//		    console.log("rotation=\"" +camera.rotation.x+","+camera.rotation.y+","+camera.rotation.z+"\"");		    
		    console.log("camera.position.set(" +camera.position.x+","+camera.position.y+","+camera.position.z+");");
		    console.log("camera.rotation.set(" +camera.rotation.x+","+camera.rotation.y+","+camera.rotation.z+");");		    
		}
	    });
	    if(this.opts.showAxis) {
		scene.add(new THREE.AxesHelper(10));
	    }
	    const ambientLight = new THREE.AmbientLight(0x404040,this.opts.ambientLight||30)
	    scene.add(ambientLight)
	    let addLight=(v,x,y,z,i) =>{
		if(!Utils.isDefined(i)) i=1;
		let light = new THREE.PointLight(this.parseInt(v),i);
//		let light = new THREE.DirectionalLight(this.parseInt(v));
		light.position.set(x,y,z);
		this.scene.add(light);
	    }
//	    addLight(0xffffff,0,0,0);
//	    addLight(0xffffff,1,1,1);	    


	    let update = (xhr)=>{
//		console.dir(xhr);
		//.loaded / xhr.total) * 100				    
		console.log("loading...");
	    };
	    let error =  (error) => {
		jqid(this.divId).html(error);
		console.log(error)
	    };
	    if(url.match(/\.gltf/gi)) {
		const loader = new THREE.GLTFLoader();
		loader.load(url, function(gltf){
		    console.log("loaded gltf model");
//		    _this.animate();
//		    jqid(_this.divId).html(_this.renderer.domElement);
//		    return
		    let obj = gltf.scene.children[0];
		    _this.scene.add(gltf.scene);
		    _this.initObject(obj);
		    _this.animate();
		},update,error);
	    } else  if(url.match(/\.fbx/gi)) {
		const fbxLoader = new THREE.FBXLoader()
		fbxLoader.load(
		    url,
		    (object) => {
			console.log("loaded fbx model");
			_this.scene.add(object);
			_this.initObject(object);
			_this.animate();
		    },
		    update,error);
	    } else {
		alert("Unknown model:" + url);
	    }

	},
	initObject: function(object) {
	    this.object = object;
	    this.visible = true;
	    jqid(this.divId).html(this.renderer.domElement);
	    let tmp  = [object.rotation.x,  object.rotation.y,  object.rotation.z];
	    object.rotation.x=0;
	    object.rotation.y=0;
	    object.rotation.z=0;




	    bbox = new THREE.Box3().setFromObject(object);
	    var referenceBox = new THREE.Box3(new THREE.Vector3(-10,-10,-10),new THREE.Vector3(10,10,10));
	    var referenceSize = referenceBox.getSize(new THREE.Vector3());
	    var objectSize = bbox.getSize(new THREE.Vector3());
	    var ratio = referenceSize.divide( objectSize );
//	    console.log(referenceSize);
	    console.log("object size:");
	    console.log(objectSize);
	    console.log('ratio:');
	    console.log(ratio);


	    var scale = Math.min(ratio.x, Math.min(ratio.y, ratio.z));
	    console.log("scale:" + scale);
	    object.scale.setScalar(scale);
	    this.addHelper(object);
	    return;

	    var bbox = new THREE.Box3().setFromObject(object);
	    var center  = bbox.center(new  THREE.Vector3());
	    var delta = center.multiply(new THREE.Vector3(-1,-1,-1));
	    delta = center;
	    console.log('delta:');
	    console.log(delta);
	    object.translateX(delta.x);
	    object.translateY(delta.y);
	    object.translateZ(delta.z);

	    this.addHelper(object,0x00ff00);
//	    object.rotation.x=tmp[0];
//	    object.rotation.y=tmp[1];
//	    object.rotation.z=tmp[2];
	    



//	    console.dir(object.scale);
	    return

	    var center = new THREE.Vector3();
	    box.getCenter(center);
	    
	    var scaleTemp = new THREE.Vector3().copy(scaleV3).divide(size);
	    var scale = Math.min(scaleTemp.x, Math.min(scaleTemp.y, scaleTemp.z));
	    object.scale.setScalar(scale);
	    object.position.sub(center.multiplyScalar(scale));
	},
	addHelper:function(object, color) {
	    if(!Utils.isDefined(color)) color = 0xff0000;
	    let helper = new THREE.BoxHelper(object, color);
	    helper.update();
	    this.scene.add(helper);	    
	},
	parseInt:function(v,dflt) {
	    if(typeof v == "number") return v;
	    if(!v) return  dflt;
	    if(v.match("rgb")) v = Utils.rgbToHex(v);
	    if(v.startsWith("#")) v = v.substring(1);
	    if(!v.startsWith("0x")) v = '0x' + v;
	    return parseInt(Number(v), 10);
	},

	animate:function() {
            this.renderer.render(this.scene,this.camera);
            requestAnimationFrame(()=>{this.animate();});
	}
    });
    setTimeout(()=>{this.init();},1);
}

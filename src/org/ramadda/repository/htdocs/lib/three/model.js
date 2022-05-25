function Model3D(models,props) {
    this.models = models;
    this.opts = {
	background:'#f4f4f4',
	width:600,
	height:400,
	boxSize:10,
	showAxes:false,
	axesSize:10,
	axesColor:"green",
	axesColorX:null,
	axesColorY:null,
	axesColorZ:null,		
	cameraAngle:75,
	showBbox:false,
	bboxColor:"#ff0000",
	cameraPosition:[0.11023511030283133,3.220465244981674,17.053542069869387], 
	cameraRotation:[-0.12947709924571024,0.006409864567801695,0.0008345938147183319],
	ambientLight:"#f0f0f0,1",
	lights:"#f0f0f0,0,10,0,1;"
    }
    $.extend(this.opts, props||{});

    this.objects = [];
    this.visible = true;
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
            scene.background = new THREE.Color(this.opts.background);	    
	    
	    this.addedRenderer = false;
            this.renderer = new THREE.WebGLRenderer({antialias:true});
            this.renderer.setSize(this.opts.width,this.opts.height);

            let camera = this.camera = new THREE.PerspectiveCamera(this.opts.cameraAngle,this.opts.width/this.opts.height,1,100);
	    if(typeof this.opts.cameraPosition  == "string")
		this.opts.cameraPosition = this.opts.cameraPosition.split(",");
	    camera.position.set(+this.opts.cameraPosition[0],
				+this.opts.cameraPosition[1],
				+this.opts.cameraPosition[2]);
	    if(typeof this.opts.cameraRotation  == "string")
		this.opts.cameraRotation = this.opts.cameraRotation.split(",");
	    camera.rotation.set(+this.opts.cameraRotation[0],
				+this.opts.cameraRotation[1],
				+this.opts.cameraRotation[2]);	    

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
		    this.objects.forEach(object=>{
			object.visible = this.visible;
		    });
		    return
		}
		if(event.keyCode==100) {
		    let str = "cameraPosition=\"" +camera.position.x+","+camera.position.y+","+camera.position.z+"\"" +"\n" +
			"cameraRotation=\"" +camera.rotation.x+","+camera.rotation.y+","+camera.rotation.z+"\"";
		    Utils.copyToClipboard(str);
		    console.log("copied to clipboard:");
		    console.log(str);
		}
	    });
	    if(this.opts.showAxes) {
		let axes = new THREE.AxesHelper(+this.opts.axesSize);
		axes.setColors(new THREE.Color(this.opts.axesColorX || this.opts.axesColor ),
			       new THREE.Color(this.opts.axesColorY || this.opts.axesColor ),
			       new THREE.Color(this.opts.axesColorZ || this.opts.axesColor ));			       
		scene.add(axes);
	    }

	    let ambientLight = this.getProperty("ambientLight",null);
	    if(Utils.stringDefined(ambientLight) && ambientLight!="none") {
		let al = Utils.split(ambientLight,",");
		if(al.length==0) al = [];
		else if(al.length==1) al.push("30");
		if(al[0]=="") al[0]="#404040";
		if(al[1]=="") al[0]="5";
		scene.add(new THREE.AmbientLight(new THREE.Color(al[0]).getHex(),+al[1]));
	    }
	    let addLight=(v,x,y,z,i) =>{
		if(!Utils.isDefined(i)) i=1;
		let c = new THREE.Color(v);
		let light = new THREE.PointLight(c.getHex(),i,0,2);
//		let light = new THREE.DirectionalLight(c.getHex());
		light.position.set(+x,+y,+z);
		this.scene.add(light);
	    }
	    let lights =this.getProperty("lights","");
	    if(Utils.stringDefined(lights) && lights!="none") {
		Utils.split(lights,";",true,true).forEach(s=>{
		    let tuple = s.split(",");
		    if(tuple.length<4)  {
			console.log("Bad light:" + s);
			return;
		    }
		    addLight(tuple[0],tuple[1],tuple[2],tuple[3],tuple[4]);
		});
	    }

	    let update = (xhr)=>{
//		console.log("loading...");
	    };
	    let error =  (error) => {
		jqid(this.divId).html(error);
		console.log(error)
	    };
	    this.models.forEach((model,idx)=>{
		let url = model.url;
		if(url.match(/\.gltf/gi)) {
		    const loader = new THREE.GLTFLoader();
		    loader.load(url, function(gltf){
			console.log("loaded gltf model:" + model.name +" " +url);
			let obj = gltf.scene.children[0];
			_this.initObject(idx,model,gltf.scene);
			_this.scene.add(gltf.scene);
			_this.animate();
		    },update,error);
		} else  if(url.match(/\.fbx/gi)) {
		    const fbxLoader = new THREE.FBXLoader()
		    fbxLoader.load(
			url,
			(object) => {
			    console.log("loaded fbx model:" + model.name +" " +url);
			    _this.scene.add(object);
			    _this.initObject(idx,model,object);
			    _this.animate();
			},
			update,error);
		} else {
		    alert("Unknown model:" + url);
		}
	    });
	},
	initObject: function(idx,model,object) {
	    this.objects.push(object);
	    if(!this.addedRenderer) {
		jqid(this.divId).html(this.renderer.domElement);
		this.addedRenderer = true;
	    }
	    let debug = msg =>{
		console.log(msg,object.position,object.rotation,object.scale);
	    };

	    let getBbox = () =>{
		return new THREE.Box3().setFromObject(object);
	    };
	    let getCenter = () =>{
		let center = new THREE.Vector3();
		getBbox().getCenter(center);
		return center;
	    };	    
	    doScale = () =>{
		var referenceBox = new THREE.Box3(new THREE.Vector3(-this.opts.boxSize,-this.opts.boxSize,-this.opts.boxSize),new THREE.Vector3(this.opts.boxSize,this.opts.boxSize,this.opts.boxSize));
		var referenceSize = referenceBox.getSize(new THREE.Vector3());
		var objectSize = getBbox().getSize(new THREE.Vector3());
		var ratio = referenceSize.divide( objectSize );
		var scale = Math.min(ratio.x, Math.min(ratio.y, ratio.z));
		object.scale.multiplyScalar(scale);
		object.updateWorldMatrix(true,true);
	    };
	    object.updateWorldMatrix(true,true);
	    doScale();
	    object.position.sub(getCenter());
	    object.updateWorldMatrix(true,true);
	    if(offset = this.getModelOpt(idx,model,"offset")) {
		offset = offset.split(",");
		let base = offset[0];
		object.position.add(new THREE.Vector3(parseFloat(offset[0]),parseFloat(offset[1]||0),parseFloat(offset[2]||0)));
	    }

	    object.updateWorldMatrix(true,true);
	    if(this.getModelOpt(idx,model,"showBbox",false)) {
		this.addHelper(object,this.getModelOpt(idx,model,"bboxColor"));
	    }
	},
	getProperty:function(what,dflt) {
	    let v = null;
	    this.models.every(model=>{
		if(Utils.isDefined(model[what])) {
		    v = model[what];
		    return false;
		}
		return true;
	    });
	    if(v) return v;
	    if(Utils.isDefined(this.opts[what])) return this.opts[what];
	    return dflt;
	},

	getModelOpt:function(idx,model,what,dflt) {
	    if(Utils.isDefined(model[what])) {
		return model[what];
	    }
	    return this.opts[what+"_" + model.id] || this.opts[what+"_" + idx] || this.opts[what] || dflt;
	},
	addHelper:function(object, color) {
	    if(typeof color == "string") color = new THREE.Color(color).getHex();
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

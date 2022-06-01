function RamaddaModel3D(display,model,idx) {
    this.display = display;
    this.index = idx;
    $.extend(this,model);
}

RamaddaModel3D.prototype = {
    setObject:function(object) {
	this.object = object;
	if(!this.visible)
	    this.object.visible   =false;
    },

    setVisible:function(v){
	if(this.backgroundSphere)
	    this.backgroundSphere.visible = v;
	if(this.object)
	    this.object.visible = v;
	this.visible = v;
	if(this.helper)
	    this.helper.visible = v;
    }
}


function Ramadda3DDisplayManager(models,props) {
    props = props||{};
    this.properties = props;
    this.opts = {
	showToc:false,
	width:640,
	height:480,
    }
    $.extend(this.opts, props||{});
    this.models = models.map((model,idx)=>{
	return new RamaddaModel3D(this,model,idx);
    });

    if(this.models.length>0) {
	this.models[0].visible=true;
    }
    this.divId = this.opts.id||'model';
    this.separate = true;
    let html = "";
    let width = this.opts.width;
    let height = this.opts.height;	
    this.models.forEach((model,idx)=>{
	html+= HU.div(['id',this.getSubDivId(idx),
		       'style',HU.css('display','none','width',HU.getDimension(width),'height',HU.getDimension(height))]);
    });
    jqid(this.divId).css('width',this.opts.width).css('height',this.opts.height).css('max-width',this.opts.width).css('max-height',this.opts.height).css('overflow-y','hide');
    jqid(this.divId).html(html);
    this.displays = [];
    this.models.forEach((model,idx)=>{
	let displayProps = $.extend({},props);
	displayProps.divId = this.getSubDivId(idx);
	this.displays.push(new Ramadda3DDisplay([model],displayProps));
    });
    setTimeout(()=>{this.init();},1);
}

Ramadda3DDisplayManager.prototype = {
    init:function() {
	if(this.opts.showToc) {
	    this.showToc();
	}
	this.displays.forEach(display=>{display.init();});
	this.doLayout();
	this.models.forEach((model,idx)=>{
	    if(model.visible) {
		this.displays[0].loadModel(model);
	    }
	});

    },
    getSubDivId:function(idx) {
	return  this.divId +'_display' + idx;
    },
    isSeparate: function() {
	return this.separate && this.models.length>1;
    },
    showToc:function() {
	let _this = this;
	let html = "";
	let toc = this.getTocDiv();
	toc.addClass('ramadda-model-toc');
	this.models.forEach((model,idx)=>{
	    let clazz = model.visible?'ramadda-model-toc-item-on':'ramadda-model-toc-item-off';
	    let prefix ="";
	    if(model.entryid) {
		prefix = HU.div(['style',HU.css('margin-right','4px')],
				HU.href(ramaddaBaseUrl + '/entry/show?entryid=' + model.entryid,HU.getIconImage(ramaddaBaseUrl+'/media/3dmodel.png'),
					['title','View entry:' + model.name,'target','_entry']));
	    }
	    let label = model.name;
	    if(model.thumbnail) {
		label +="<br>" + HU.image(model.thumbnail,['width','100px']);
	    }

	    let extra = "";
	    if(model.description) {
		extra +=HU.div(['model-index',idx,'class','ramadda-model-toc-item-description',
				'style',HU.css('display',model.visible?'block':'none')], model.description);
	    }

	    html+=HU.div(['model-index',idx,'class','ramadda-model-toc-item ' + clazz,'id',this.divId+'_model_' + idx,'title','Toggle '+ model.name],
			 '<table width=100%><tr valign=top><td>' +prefix+'</td><td width=99%>'+
			 HU.div(['style','display:inline-block;width:100%;','class','ramadda-model-toc-item-name  ramadda-clickable', 'model-index',idx],label)+
			 '</td></tr></table>'+extra);
	});
	toc.css('height',this.opts.height+"px");
	toc.html(html);
	toc.find('.ramadda-model-toc-item-name').click(function() {
	    let item = $(this).closest('.ramadda-model-toc-item');
	    let model =_this.models[$(this).attr('model-index')];
	    let extra = item.find('.ramadda-model-toc-item-description');
	    if(model.visible) {
		model.setVisible(false);
		extra.hide();
		item.removeClass('ramadda-model-toc-item-on');
		item.addClass('ramadda-model-toc-item-off');			
	    } else {
		model.setVisible(true);
		extra.show();
		item.removeClass('ramadda-model-toc-item-off');
		item.addClass('ramadda-model-toc-item-on');			
		if(!model.object) {
		    model.display.loadModel(model);
		}
	    }
	    if(_this.isSeparate()) {
		_this.doLayout();
	    }
	    if(model.object) {
		model.display.layoutModels();
	    }
	    model.display.addLights();
	});
    },
    getTocDiv:function() {
	return jqid(this.divId+"_toc");
    },
    doLayout: function() {
	let cntVisible=0;
	this.models.forEach(model=>{if(model.visible) cntVisible++;});
	let baseWidth = this.opts.width;
	let baseHeight = this.opts.height;	    
	let width = baseWidth;
	let height = baseHeight;
	let cols = 1;
	if(cntVisible>1) {
	    let ratio = width/height;
	    cols = Math.ceil(Math.sqrt(cntVisible));
	    let rows = Math.ceil(cntVisible/cols);
	    width = width/cols-1;
	    height = Math.floor(baseHeight/rows);
	    if(cntVisible==2) height=baseHeight;
	}
	let cnt=0;
	let visibleModels = this.models.filter((model,idx)=>{
	    let subDiv = jqid(this.getSubDivId(model.index));
	    if(!model.visible) {
		subDiv.hide();
		return false;
	    }
	    return true;
	});
	visibleModels.forEach((model,idx)=>{
	    let subDiv = jqid(this.getSubDivId(model.index));
	    cnt++;
	    subDiv.show();
	    subDiv.css('width',width).css('height',height+2).css('display','inline-block');
	    if(visibleModels.length==1) {
		subDiv.css('border','0px solid #eee');
	    } else {
		subDiv.css('border','1px solid #eee');
	    }
	    model.display.resize(width,height);
	});

    }
}


function Ramadda3DDisplay(models,props) {
    this.lights = [];
    this.models = models;
    this.models.forEach(model=>{model.display =this;});
    this.properties = props||{};
    this.opts = {
	showToc:false,
	rotating:false,
	background:'#f4f4f4',
	boxSize:10,
	showCheckerboard:false,
	showAxes:false,
	axesSize:10,
	axesColor:"#000000",
	axesColorX:null,
	axesColorY:null,
	axesColorZ:null,		
	cameraAngle:75,
	showBbox:false,
	bboxColor:"#ff0000",
	cameraPosition:"0.11023,3.22046,17.05354;-0.12947,0.00640,0.00083",
	ambientLight:"#f0f0f0,1",
	lights:""
    }
    $.extend(this.opts, props||{});
}

Ramadda3DDisplay.prototype = {
    init:function() {
	this.addBackground(this.opts.backgroundImage,this.opts.fixeBackgroundImage,this);
	let _this = this;
	this.loadingCount = 0;
	this.visible = true;
	this.divId = this.opts.divId;
	this.threeId = this.domId("_three");
	jqid(this.divId).css('background',"#"+this.opts.background).css('position','relative');
	let loading = "Loading..." +"<br>" +
	    HU.image(ramaddaBaseUrl  + "/icons/mapprogress.gif",['style',HU.css('width','60px')]);
	let icon=i=>{
	    return HU.getIconImage(i,[],['style','color:#ccc']);
	};
	let buttons = [];
	buttons.push(HU.span(['id',this.domId('_toolbarholder')]));
	buttons.push(HU.span(['title','Show grid','class','ramadda-clickable','id',this.domId('_grid')],icon('fas fa-table-cells')));
	buttons.push(HU.span(['title','Set ambient light','class','ramadda-clickable','id',this.domId('_light')],icon('fas fa-lightbulb')));
	buttons.push(HU.span(['title','Reset view','class','ramadda-clickable','id',this.domId('_home')],icon('fas fa-house')));
	buttons.push(HU.span(['title','Auto-rotate','class','ramadda-clickable','id',this.domId('_play')],icon('fas fa-play')));
	if(this.models.length==1 && this.models[0].entryid) {
	    buttons.push(HU.href(ramaddaBaseUrl + '/entry/show?entryid=' + this.models[0].entryid,
				 HU.getIconImage(ramaddaBaseUrl+'/media/3dmodel.png'),
				 ['target','_entry','title','View entry','class','ramadda-clickable']));


	}
	let toolbar = HU.div(['class','ramadda-model-toolbar'], Utils.wrap(buttons,"<span style='margin-right:8px;'>","</span>"));
	let html = HU.div(['id',this.threeId]) +
	    HU.div(['style',HU.css('display','none','width','100px','text-align','center','position','absolute','left','10px','top','10px'),'id',this.domId('_loading')],loading) +
	    HU.div(['style',HU.css('position','absolute','right','0px','top','0px'),'id',this.domId('_toolbar')],toolbar)		
	;


	jqid(this.divId).html(html);

	jqid(this.domId('_grid')).click(()=> {
	    this.toggleGrid();
	});

	jqid(this.domId('_light')).click(()=> {
	    let light = prompt("Set Light (format: color,intensity e.g, #ff0000,5):",this.getProperty("ambientLight",null));
	    if(light===null) return;
	    light=light.trim();
	    if(light=="")
		this.properties['ambientLight'] = null;
	    else
		this.properties['ambientLight'] = light;
	    this.addLights();
	    if(this.canEdit()) {
		let args = {"edit_media_3dmodel_ambient_light":light}
		this.doSave(args);
	    }
	});

	jqid(this.domId('_home')).click(()=> {
	    this.setCameraPosition();
	    this.models.forEach(model=>{
		if(model.object && model.originalRotation) {
		    model.object.rotation.x =  model.originalRotation.x;
		    model.object.rotation.y =  model.originalRotation.y;
		    model.object.rotation.z =  model.originalRotation.z;
		}});
	});
	jqid(this.domId('_play')).click(function() {
	    _this.opts.rotating = !_this.opts.rotating;
	    if(_this.opts.rotating) {
		$(this).html(icon('fas fa-stop'));
	    } else {
		$(this).html(icon('fas fa-play'));
	    }
	});
	let scene = this.scene = new THREE.Scene();
	scene.background = new THREE.Color(this.opts.background);
	this.addedRenderer = false;
        this.renderer = new THREE.WebGLRenderer({antialias:true});
	this.renderer.outputEncoding = THREE.sRGBEncoding;
        this.renderer.setSize(this.opts.width,this.opts.height);

        let camera = this.camera = new THREE.PerspectiveCamera(this.opts.cameraAngle,this.opts.width/this.opts.height,1,1000);
        let controls = this.controls = new THREE.OrbitControls(camera,this.renderer.domElement );
        controls.addEventListener('change', (event)=>{
	    this.renderer.render(scene,camera);
	});
	setTimeout(()=>{
	    this.setCameraPosition();
	},1);


	jqid(this.divId).mouseenter(function() {
	    $(this).focus();
	});
	jqid(this.divId).keypress((event) =>{
	    if(event.key=="d") {
		let str = this.getCameraPosition();
		Utils.copyToClipboard(str);
		console.log("copied to clipboard:");
		console.log(str);
	    }
	});
	if(this.opts.showAxes) {
	    let size = this.opts.axesSize;
	    if(size==0) size = this.opts.width;
	    let axes = new THREE.AxesHelper(size);
	    axes.setColors(new THREE.Color(this.opts.axesColorX || this.opts.axesColor ),
			   new THREE.Color(this.opts.axesColorY || this.opts.axesColor ),
			   new THREE.Color(this.opts.axesColorZ || this.opts.axesColor ));			       
	    scene.add(axes);
	}

	if(this.models.length==1 && this.models[0].entryid) {
	    let model = this.models[0];
	    getGlobalRamadda().getEntry(model.entryid,entry=>{
		this.entry = entry;
		if(this.canEdit()) {
		    let buttons = [
			HU.span(['title','Set default camera position','class','ramadda-clickable','id',this.domId('_setcamera')],icon('fas fa-camera'))];
		    this.jq('_toolbarholder').html(Utils.join(buttons,HU.space(3)));
		    this.jq('_setcamera').click(()=>{
			let pos = this.getCameraPosition();
			let args = {'edit_media_3dmodel_camera_position':pos}
			this.doSave(args);
		    });
		}


		if(Utils.stringDefined(model.annotations)|| entry.canEdit()) {
		    let annotations = Utils.split(model.annotations,"\n",true,true);
		    this.initAnnotations(annotations);
		}
	    });
	}
	this.addLights();
	if(this.getProperty("showGrid")) {
	    this.toggleGrid();
	}

	if(this.getProperty("showCheckerboard")) {
	    this.addChecker(this.opts.width);
	}
	this.animate();
    },
    resize:function(width,height) {
	if(!this.camera || !this.renderer) return;
	this.camera.aspect = width / height;
	this.camera.updateProjectionMatrix();
	this.renderer.setSize(width, height);
    },
    getBbox:function (object){
	return new THREE.Box3().setFromObject(object);
    },
    getCenter:function (object){
	let center = new THREE.Vector3();
	this.getBbox(object).getCenter(center);
	return center;
    },	    
    doScale:function (object,size) {
	let referenceBox = new THREE.Box3(new THREE.Vector3(-size,-size,-size),new THREE.Vector3(size,size,size));
	let referenceSize = referenceBox.getSize(new THREE.Vector3());
	let objectSize = this.getBbox(object).getSize(new THREE.Vector3());
	let ratio = referenceSize.divide( objectSize );
	let scale = Math.min(ratio.x, Math.min(ratio.y, ratio.z));
	object.scale.multiplyScalar(scale);
	object.updateWorldMatrix(true,true);
    },


    singleModel:function() {
	return this.models.length==1;
    },
    incrLoading:function(d) {
	this.loadingCount +=d;
	if(this.loadingCount<=0) {
	    this.loadingCount=0;
	}
	this.checkLoading();
    },
    getCameraPosition:function() {
	let camera = this.camera;
	let get=(o)=>{
	    let decimals = 5;
	    return Utils.trimDecimals(o.x,decimals)+","+Utils.trimDecimals(o.y,decimals)+","+Utils.trimDecimals(o.z,decimals);
	};	    
	return  get(camera.position)+";" + get(camera.rotation) +";"+get(this.controls.target);
    },
    checkLoading:function() {
	if(this.loadingCount>0) this.jq('_loading').show();
	else  this.jq('_loading').hide();
    },
    jq:function(suffix) {
	return jqid(this.domId(suffix));
    },
    domId:function(suffix) {
	return this.divId+suffix;
    },
    doSave:function(args) {
	this.entry.doSave(this.opts.authtoken,args);
    },

    canEdit: function() {
	if(!this.entry) return false;
	return this.entry.canEdit();
    },
    initAnnotations:function(annotations) {
	let html = "";
	let _this = this;
	let canEdit = this.canEdit();
	annotations = annotations.map(line=>{
	    let toks = Utils.split(line,";");
	    if(toks.length!=4) return null;
	    return line;
	});
	annotations.forEach((line,idx)=>{
	    //pos;rot;target;comment
	    let toks = Utils.split(line,";");
	    let pos = toks[0]+";"+toks[1]+";"+toks[2];
	    let btns = "";
	    if(canEdit) {
		btns =
		    HU.span(['annotation-index',idx,'title','Edit annotation','class','ramadda-clickable ramadda-model-annotation-edit'],
			    HU.getIconImage("fas fa-pen")) +" " +
		    HU.span(['annotation-index',idx,'title','Delete annotation','class','ramadda-clickable ramadda-model-annotation-delete'],
			    HU.getIconImage("fas fa-trash-can"));
	    }
	    let label = HU.span(['3dposition',pos,'class','ramadda-clickable ramadda-model-annotation'], toks[3]);
	    html+=HU.div([],btns +label);
	});
	if(canEdit || html!="") {
	    this.getAnnotationsDiv().css('height',this.opts.height+"px");
	    let topLabel = HU.b("Annotations");
	    if(canEdit) {
		topLabel+=" " + HU.span(['class','ramadda-clickable ramadda-model-annotation-add','title','Add annotation'],HU.getIconImage('fas fa-plus'));
	    }
	    this.getAnnotationsDiv().html(HU.div(['style',HU.css('height',this.opts.height+"px"),'class','ramadda-model-annotations'], topLabel + html));
	    this.getAnnotationsDiv().find('.ramadda-model-annotation').click(function() {
		_this.setCameraPosition($(this).attr('3dposition'));
	    });
	    this.getAnnotationsDiv().find('.ramadda-model-annotation-add').click(function() {
		let comment = prompt("Annotation:");
		if(!Utils.stringDefined(comment)) return;
		let line =  _this.getCameraPosition()+";" + comment;
		annotations.push(line);
		_this.saveAnnotations(annotations);
	    });
	    this.getAnnotationsDiv().find('.ramadda-model-annotation-edit').click(function() {
		let idx = $(this).attr('annotation-index');
		let line = annotations[idx];
		let toks = Utils.split(line,";");
		let pos = toks[0]+";"+toks[1]+";"+toks[2];
		let comment = prompt("Annotation:",toks[3]);
		if(Utils.stringDefined(comment)) {
		    let line =  _this.getCameraPosition()+";" + comment;
		    annotations[idx] = line;
		    _this.saveAnnotations(annotations);
		}
	    });		
	    this.getAnnotationsDiv().find('.ramadda-model-annotation-delete').click(function() {
		if(!window.confirm("Are you sure you want to delete the annotation?")) return
		let idx = $(this).attr('annotation-index');
		annotations.splice(idx,1);
		_this.saveAnnotations(annotations);
	    });		
	}
    },
    saveAnnotations:function(annotations) {
	this.initAnnotations(annotations);
	let args = {
	    "edit_media_3dmodel_annotations":Utils.join(annotations,"\n"),
	}
	this.entry.doSave(this.opts.authtoken,args);
    },
    grids:null,
    toggleGrid:function() {
	if(this.grids==null) {
	    let grids = this.grids = [];
	    let gridSize  = this.getProperty("gridSize",50);
	    let divisions=this.getProperty("gridDivisions",10);
	    let gc = this.getProperty("gridColor",0x888888);
	    var gridXZ = new THREE.GridHelper(gridSize,divisions, gc,gc);
	    gridXZ.position.y = -gridSize/2;
	    grids.push(gridXZ);

	    var gridXY = new THREE.GridHelper(gridSize,divisions, gc,gc);
	    gridXY.position.z = -gridSize/2;
	    gridXY.rotation.x = Math.PI / 2;
	    grids.push(gridXY);

	    var gridYZ = new THREE.GridHelper(gridSize,divisions, gc,gc);
	    gridYZ.position.x = -gridSize/2;
	    gridYZ.rotation.z = Math.PI / 2;
	    grids.push(gridYZ);
	    grids.forEach(grid=>{
		this.scene.add(grid);
	    });
	    this.gridVisible = true;
	    return
	}
	this.gridVisible = !this.gridVisible;
	this.grids.forEach(grid=>{grid.visible = this.gridVisible;});
    },

    addChecker:function(width) {
	const planeSize = width*1.1;
	const loader = new THREE.TextureLoader();
	const texture = loader.load(ramaddaBaseUrl+'/images/checker.png');
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
	mesh.rotation.x = Math.PI * -.5;
	mesh.position.y = -10;
	this.scene.add(mesh);
    },


    addLights: function() {
	this.lights.forEach(light=>{
	    this.scene.remove(light);
	});
	this.lights = [];
	let ambientLight = this.getProperty("ambientLight",null);
	if(Utils.stringDefined(ambientLight) && ambientLight!="none") {
	    let al = Utils.split(ambientLight,",");
	    if(al.length==0) al = [];
	    else if(al.length==1) al.push("30");
	    if(al[0]=="") al[0]="#404040";
	    if(al[1]=="") al[0]="5";
	    let light;
	    if(al.length==2) {
		light = new THREE.AmbientLight(new THREE.Color(al[0]).getHex(),+al[1]);
	    } else {
		light = new THREE.HemisphereLight(new THREE.Color(al[0]).getHex(),new THREE.Color(al[1]).getHex(),+al[2]);
	    }
	    this.lights.push(light);
	    this.scene.add(light);
	}
	let addLight=(v,x,y,z,i) =>{
	    if(!Utils.isDefined(i)) i=1;
	    let c = new THREE.Color(v);
	    let light = new THREE.PointLight(c.getHex(),i,0,2);
	    //		let light = new THREE.DirectionalLight(c.getHex());
	    light.position.set(+x,+y,+z);
	    this.lights.push(light);
	    this.scene.add(light);
	}
	let lights =this.getProperty("lights","");
	if(Utils.stringDefined(lights) && lights!="none") {
	    let color = null;
	    Utils.split(lights,"\n",true,true).forEach(s=>{
		let tuple = s.split(",");
		if(tuple.length==4)  {
		    color = tuple[0];
		} else if(tuple.length==3 && color) {
		    tuple = [color,...tuple];
		}
		if(tuple.length<4)  {
		    console.log("Bad light:" + s);
		    return;
		}
		addLight(tuple[0],tuple[1],tuple[2],tuple[3],tuple[4]);
	    });
	}
    },	    
    addBackground:function(backgroundImage, fixedBackgroundImage,object) {
	if(!this.singleModel()) return;
	if(backgroundImage) {
	    var backgroundSphere = new THREE.Mesh(
		new THREE.SphereGeometry(100,10,10),
		new THREE.MeshBasicMaterial({
		    map: (new THREE.TextureLoader).load(backgroundImage),
		    side: THREE.DoubleSide
		})
	    );
	    this.scene.add(backgroundSphere);
	    object.backgroundSphere = backgroundSphere;
	}
	if(fixedBackgroundImage) {
	    new THREE.TextureLoader().load(fixedBackgroundImage, texture=> {
		this.scene.background = texture;  
		object.background = texture;
	    });
	}
    },


    getAnnotationsDiv:function() {
	return jqid(this.divId+"_annotations");
    },	
    loadModel:function(model) {
	let _this = this;
	let update = (xhr)=>{
	};
	let error =  (error) => {
	    this.incrLoading(-1);
	    jqid(this.divId).html(error);
	    console.log(error)
	};
	let url = model.url;
	if(url.match(/\.gltf/gi)) {
	    const loader = new THREE.GLTFLoader();
	    this.incrLoading(1);
	    loader.load(url, (gltf)=>{
		this.incrLoading(-1);
		console.log('loaded gltf model:' + model.name +' ' +url);
		let obj = gltf.scene.children[0];
		this.initObject(model,gltf.scene);
		this.scene.add(gltf.scene);
	    },update,error);
	} else  if(url.match(/\.fbx/gi)) {
	    const fbxLoader = new THREE.FBXLoader()
	    this.loadingModelCount++;
	    this.incrLoading(1);
	    fbxLoader.load(
		url,
		(object) => {
		    this.incrLoading(-1);
		    console.log("loaded fbx model:" + model.name +" " +url);
		    this.scene.add(object);
		    this.initObject(model,object);
		},
		update,error);
	} else  if(url.match(/\.3ds/gi)) {
	    let loader = new THREE.TDSLoader( );
	    this.incrLoading(1);
	    loader.load(url, ( object )=> {
		this.incrLoading(-1);
		console.log("loaded 3ds model:" + model.name +" " +url);
		this.scene.add(object);
		this.initObject(model,object);
	    } );
	} else  if(url.match(/\.dae/gi)) {
	    const loader = new THREE.ColladaLoader();
	    this.incrLoading(1);
	    loader.load(url,collada =>{
		this.incrLoading(-1);
		console.log("loaded dae model:" + model.name +" " +url);
		let player = collada.scene;
		this.scene.add( player );
		this.initObject(model,player);
	    });
	} else  if(url.match(/\.obj/gi)) {
	    const loader = new THREE.OBJLoader();
	    this.incrLoading(1);
	    loader.load(url,object =>{
		this.incrLoading(-1);
		console.log("loaded obj model:" + model.name +" " +url);
		this.scene.add( object );
		this.initObject(model,object);
	    });		
	} else {
	    alert("Unknown model:" + url);
	}
    },

    setCameraPosition: function(a) {
	this.controls.enabled = false;
	let camera = this.camera;
	a = a || this.getProperty("cameraPosition","");
	//Format is x,y,z;targetx,targety,targetz
	if(Utils.stringDefined(a)) {
	    let p = a.split(";");
	    a = p[0].split(",");
	    if(a.length==3) {
		camera.position.set(+a[0],+a[1],+a[2]);
	    }
	    if(p.length>1) {
		a = p[1].split(",");
		if(a.length==3) {
		    this.camera.rotation.set(+a[0],+a[1],+a[2]);
		}
	    }
	    if(p.length>2) {
		a = p[2].split(",");
		if(a.length==3) {
		    this.controls.target.set(+a[0],+a[1],+a[2]);
		}
	    }

	}

	this.camera.lookAt( this.controls.target );
	this.controls.enabled = true;
	this.controls.update();
    },
    initObject: function(model,object) {
	if(model.texture) {
	    const textureLoader = new THREE.TextureLoader( );
	    const texture = textureLoader.load(model.texture);
	    object.traverse( function ( child ) {
		if ( child.isMesh ) child.material.map = texture;
	    } );		
	}

	if(model.normal) {
	    const normal = new THREE.TextureLoader().load(model.normal);
	    object.traverse( function ( child ) {
		if (child.isMesh ) {
		    child.material.specular.setScalar( 0.1 );
		    child.material.normalMap = normal;
		}
	    } );
	}

	this.addBackground(model.backgroundImage, model.fixedBackgroundImage,model);

	model.setObject(object);
	if(!this.addedRenderer) {
	    jqid(this.threeId).html(this.renderer.domElement);
	    this.addedRenderer = true;
	}

	object.updateWorldMatrix(true,true);
	this.doScale(object,this.opts.boxSize);
	object.position.sub(this.getCenter(object));
	object.updateWorldMatrix(true,true);
	if(offset = this.getModelProperty(model,"offset")) {
	    offset = offset.split(",");
	    let base = offset[0];
	    object.position.add(new THREE.Vector3(parseFloat(offset[0]),parseFloat(offset[1]||0),parseFloat(offset[2]||0)));
	}
	this.layoutModels();
	object.updateWorldMatrix(true,true);
	this.addHelper(model,this.getModelProperty(model,"bboxColor"));
	//Set the camera position if this is the first model loaded
	if(!this.loadedModels) {
	    this.loadedModels =true;
	    this.setCameraPosition();
	}
    },
    layoutModels:function() {
	let visible = [];
	this.models.forEach(model=>{if(model.visible && model.object) visible.push(model);});
	if(visible.length==0) return;
	let clear = model=>{
	    if(model.layoutOffset) {
		model.object.position.sub(model.layoutOffset);
		model.layoutOffset=null;
	    }
	    if(model.helper) {
		this.scene.remove(model.helper);
		model.helper = null;
	    }
	};
	if(visible.length==1) {
	    clear(visible[0]);
	    this.addHelper(visible[0],this.getModelProperty(visible[0],"bboxColor"));
	    return;
	}
	let w = 1.2*this.opts.boxSize;
	let cols = Math.ceil(Math.sqrt(visible.length));
	let rows = parseInt(visible.length/cols);

	let dx = -w*Math.ceil(cols/2);
	let cnt = visible.length;
	let col=0;

	visible.forEach(model=>{
	    clear(model);
	    let offsetX=dx;
	    dx+=w;
	    model.layoutOffset = new THREE.Vector3(offsetX,0,0);
	    model.object.position.add(model.layoutOffset);		    
	    this.addHelper(model,this.getModelProperty(model,"bboxColor"));
	});
    },

    getProperty:function(what,dflt) {
	let debug = false;
	if(debug)
	    console.log("getProperty:" + what);
	if(Utils.isDefined(this.properties[what])) {
	    return this.properties[what];
	}
	let v = null;
	this.models.every(model=>{
	    if(!model.visible) return true;
	    if(Utils.isDefined(model[what])) {
		if(debug)
		    console.log("\tfrom:" +model.name +" " + model[what]);
		v = model[what];
		return false;
	    }
	    return true;
	});
	if(v) return v;
	if(Utils.isDefined(this.opts[what])) {
	    if(debug)
		console.log("\tfrom opts:" + this.opts[what]);
	    return this.opts[what];
	}
	return dflt;
    },

    getModelProperty:function(model,what,dflt) {
	let idx = model.index;
	if(Utils.isDefined(model[what])) {
	    return model[what];
	}
	return this.opts[what+"_" + model.id] || this.opts[what+"_" + idx] || this.opts[what] || dflt;
    },
    addHelper:function(model, color) {
	if(!this.getModelProperty(model,"showBbox",false)) {
	    return;
	}
	if(typeof color == "string") color = new THREE.Color(color).getHex();
	if(!Utils.isDefined(color)) color = 0xff0000;
	if(model.helper) {
	    this.scene.remove(model.helper);
	}
	model.helper = new THREE.BoxHelper(model.object, color);
	model.helper.update();
	this.scene.add(model.helper);	    
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
	if(this.opts.rotating) {
	    this.models.forEach(model=>{
		if(model.object) {
		    if(!Utils.isDefined(model.originalRotation)) {
			model.originalRotation = {x:model.object.rotation.x,y:model.object.rotation.y,z:model.object.rotation.z};
		    }
		    model.object.rotation.y += 0.005;
		}
	    });
	}
    }
}

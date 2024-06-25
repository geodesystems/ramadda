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
	let contents = '';
	html+= HU.div(['id',this.getSubDivId(idx),
		       'class','ramadda-model-3ddisplay','tabindex','0','style',HU.css('position','relative','display','none','width',HU.getDimension(width),'height',HU.getDimension(height))],contents);
    });
    jqid(this.divId).css('width',this.opts.width).css('height',this.opts.height).css('max-width',this.opts.width).css('max-height',this.opts.height).css('overflow-y','hide');
    jqid(this.divId).html(html);
    this.displays = [];
    props.manager = this;
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
    annotationsEnabled: function() {
	return this.models.length==1 && this.models[0].entryid;
    },
    getSubDivId:function(idx) {
	return  this.divId +'_display' + idx;
    },
    hasMultiples: function() {
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
				HU.href(ramaddaBaseUrl + '/entry/show?entryid=' + model.entryid,
					HU.getIconImage(ramaddaBaseUrl+'/media/3dmodel.png',
							["width",ramaddaGlobals.iconWidth]),
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
	    if(_this.hasMultiples()) {
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
	    height = Math.floor(baseHeight/rows)-1;
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
	showPlanes:false,
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
	this.addBackground(this.opts.backgroundImage,this.opts.fixedBackgroundImage,this);
	let _this = this;
	this.loadingCount = 0;
	this.visible = true;
	this.divId = this.opts.divId;
	this.threeId = this.domId("_three");
	jqid(this.divId).css('background',"#"+this.opts.background).css('position','relative');
	let menuButton = HU.div(['style',HU.css('position','absolute','right','0px','top','0px'),'class','ramadda-clickable ramadda-model-toolbar','id',this.domId('_menu')], HU.getIconImage('fas fa-bars',[],['style',HU.css('color','#000')]));
	let background=HU.div(['id',this.domId('_background'),'style',HU.css('position','relative','width','100%')]);
	let extra = HU.div(['id',this.domId('_background')], background)   +   menuButton;

	if(this.models[0]) {
	    if(this.models[0].watermark1) {
		extra+=HU.image(this.models[0].watermark1,[ATTR_CLASS,'ramadda-model-watermark']);
	    }
	    if(this.models[0].watermark2) {
		extra+=HU.image(this.models[0].watermark2,[ATTR_CLASS,'ramadda-model-watermark-right']);
	    }	    
	}
	

	let html = HU.div(['id',this.threeId]) +  extra;
	

	jqid(this.divId).html(html);


	jqid(this.domId('_menu')).click(function(){
	    _this.showMenu($(this));
	    
	});

	let scene = this.scene = new THREE.Scene();
	scene.background = new THREE.Color(this.opts.background);


	this.addedRenderer = false;
        this.renderer = new THREE.WebGLRenderer({antialias:true});
	this.renderer.outputEncoding = THREE.sRGBEncoding;
        this.renderer.setSize(640,480);
	if(this.castShadows()) {
	    this.renderer.shadowMap.enabled = true;
	}

        let camera = this.camera = new THREE.PerspectiveCamera(this.opts.cameraAngle,this.opts.width/this.opts.height,1,1000);

//	const helper = new THREE.CameraHelper(camera)
//	scene.add(helper)

        let controls = this.controls = new THREE.OrbitControls(camera,this.renderer.domElement );
        controls.addEventListener('change', (event)=>{
	    this.renderer.render(scene,camera);
	    if(this.getManager().shareCameraPosition) {
		this.shareCamera();
	    }
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
		if(Utils.stringDefined(model.annotations)|| entry.canEdit()) {
		    let annotations = Utils.split(model.annotations,"\n",true,true);
		    this.initAnnotations(annotations??[]);
		}
	    });
	}
	this.addLights();
	let debug = false;
//	let debug = this.models[0].name.indexOf("Burden")>=0;
	if(this.getProperty("showGrid",false,debug)) {
	    this.toggleGrid();
	}
	if(this.getProperty("showPlanes",false,debug)) {
	    this.addPlanes();
	}

	this.animate();
    },
    shareCamera: function() {
	this.sharingCamera = true;
	let pos = this.getCameraPosition();
	this.getManager().models.forEach(model=>{
	    let otherDisplay = model.display;
	    if(otherDisplay.sharingCamera || !otherDisplay.showingModels()) return;
	    otherDisplay.setCameraPosition(pos);
	});
	this.sharingCamera = false;
    },
    showingModels:function() {
	let showing = false;
	this.models.every(model=>{
	    if(model.object) {
		showing = true;
		return false;
	    }
	    return true;
	});
	return showing;
    },
    showMenu:function(button) {
	let _this =  this;
	let icon=(i,lbl)=>{
	    return HU.getIconImage(i,[],['style','color:#ccc']) +" " + (lbl||"");
	};
	let buttons = [];
	if(this.models.length==1 && this.models[0].entryid) {
	    buttons.push(HU.href(ramaddaBaseUrl + '/entry/show?entryid=' + this.models[0].entryid,
				 HU.getIconImage(ramaddaBaseUrl+'/media/3dmodel.png',
						["width",ramaddaGlobals.iconWidth]) +" " + "View entry",
				 ['target','_entry','title','View entry','class','ramadda-clickable']));


	}
	
	buttons.push(HU.span(['id',this.domId('_play')],icon(_this.opts.rotating?'fas fa-stop':'fas fa-play','Auto-rotate')));
	buttons.push(HU.checkbox(this.domId('_sharing'), [],this.getManager().shareCameraPosition,"Share position"));
	buttons.push(HU.span(['id',this.domId('_grid')],icon('fas fa-table-cells','Show grid')));
	buttons.push(HU.span(['id',this.domId('_light')],icon('fas fa-lightbulb','Set ambient light')));
	buttons.push(HU.span(['id',this.domId('_home')],icon('fas fa-house','Reset view')));
	if(this.canEdit()) {
	    buttons.push(HU.span(['id',this.domId('_setcamera')],icon('fas fa-camera','Set default camera position')));
	}

	let toolbar = HU.div(['class',''], Utils.wrap(buttons,"<div class='ramadda-clickable' style='margin:4px;'>","</div>"));
	let dialog = HU.makeDialog({content:toolbar,anchor:button});
	jqid(this.domId('_grid')).click(()=> {
	    this.toggleGrid();
	});

	this.jq('_sharing').change(function() {
	    _this.getManager().shareCameraPosition = $(this).is(':checked');
	});
	this.jq('_light').click(()=> {
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

	this.jq('_setcamera').click(()=>{
	    let pos = this.getCameraPosition();
	    let args = {'edit_media_3dmodel_camera_position':pos}
	    this.doSave(args);
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
		$(this).html(icon('fas fa-stop','Auto-rotate'));
	    } else {
		$(this).html(icon('fas fa-play','Auto-rotate'));
	    }
	});


    },
    resize:function(width,height) {
	if(!this.camera || !this.renderer) return;
	this.opts.width=width;
	this.opts.height=height;
	this.jq('_loadingbg').css('max-height',HU.getDimension(this.opts.height));
	if(this.checkerBoard) this.scene.remove(this.checkerBoard);
	if(this.getProperty("showCheckerboard")) {
	    this.addChecker(width);
	}
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
	if(this.loadingCount>0) {
	    this.jq('_background').show();
	} else {
	    this.jq('_background').hide();
	    this.jq('_loadingbg').hide();
	}
    },
    jq:function(suffix) {
	return jqid(this.domId(suffix));
    },
    domId:function(suffix) {
	return this.divId+suffix;
    },
    doSave:function(args) {
	if(!this.opts.authtoken) {
	    alert('No auth token');
	    return;
	}
	this.entry.doSave(this.opts.authtoken,args);
    },

    canEdit: function() {
	if(!this.entry) return false;
	return this.entry.canEdit();
    },
    getManager:function() {
	return this.opts.manager;
    },
    initAnnotations:function(annotations) {
	if(!this.getManager().annotationsEnabled()) return;
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
	    let div = this.getAnnotationsDiv();
	    let topLabel = HU.b("Annotations");
	    if(canEdit) {
		topLabel+=" " + HU.span(['class','ramadda-clickable ramadda-model-annotation-add','title','Add annotation'],HU.getIconImage('fas fa-plus'));
	    }
	    div.html(HU.div(['style',HU.css('height',this.getManager().opts.height+'px'),'class','ramadda-model-annotations'], topLabel + html));
	    div.find('.ramadda-model-annotation').click(function() {
		_this.setCameraPosition($(this).attr('3dposition'));
	    });
	    div.find('.ramadda-model-annotation-add').click(function() {
		let comment = prompt("Annotation:");
		if(!Utils.stringDefined(comment)) return;
		let line =  _this.getCameraPosition()+";" + comment;
		annotations.push(line);
		_this.saveAnnotations(annotations);
	    });
	    div.find('.ramadda-model-annotation-edit').click(function() {
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
	    div.find('.ramadda-model-annotation-delete').click(function() {
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
	if(!this.opts.authtoken) {
	    alert('No auth token');
	    return;
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
	    let gridXZ = new THREE.GridHelper(gridSize,divisions, gc,gc);
	    gridXZ.position.y = -gridSize/2;
	    grids.push(gridXZ);
	    let gridXY = new THREE.GridHelper(gridSize,divisions, gc,gc);
	    gridXY.position.z = -gridSize/2;
	    gridXY.rotation.x = Math.PI / 2;
	    grids.push(gridXY);
	    let gridYZ = new THREE.GridHelper(gridSize,divisions, gc,gc);
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

    addPlanes:function() {
	let meshSize = +this.getProperty("planeSize",50);
	let mm = (c) =>{
	    if(c && typeof c == "string") {
		if(c=="none") return null;
		let hc =  new THREE.Color(c);
		c = hc.getHex();
	    }
	    let mesh = new THREE.Mesh(new THREE.PlaneGeometry(meshSize,meshSize), new THREE.MeshBasicMaterial( {color: c,
														   side: THREE.DoubleSide }));

	    this.scene.add(mesh);
	    mesh.receiveShadow = this.castShadows();
	    return mesh;
	};

	let c =['#fffeec','#E0FFFF','#ffcccb'];
	let mesh;
	mesh  = mm(this.getProperty("planeColorX",this.getProperty("planeColor",c[0])));
	if(mesh) {
	    mesh.rotation.x = Utils.toRadians(-90);
	    mesh.position.y = -meshSize/2;
	}
	mesh  = mm(this.getProperty("planeColorY",this.getProperty("planeColor",c[1])));
	if(mesh) {
	    mesh.rotation.y = Utils.toRadians(-90);
	    mesh.position.x = -meshSize/2;
	}

	mesh  = mm(this.getProperty("planeColorZ",this.getProperty("planeColor",c[2])));
	if(mesh) {
	    mesh.rotation.z = Utils.toRadians(-90);		
	    mesh.position.z = -meshSize/2;
	}

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
	mesh.receiveShadow=this.castShadows();
	this.checkerBoard = mesh;
	this.scene.add(mesh);
    },


    addLights: function() {
//	return
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
//	    let light = new THREE.PointLight(c.getHex(),i,0,2);
	    let light = new THREE.DirectionalLight(c.getHex());
	    light.position.set(+x,+y,+z);
	    this.lights.push(light);
	    this.scene.add(light);
	    if(this.castShadows()) {
//		const cameraHelper = new THREE.CameraHelper(light.shadow.camera);
//		this.scene.add(cameraHelper);
		light.castShadow =true;
//		light.shadow.camera.top = 2;
//		light.shadow.camera.bottom = - 2;
//		light.shadow.camera.left = - 2;
//		light.shadow.camera.right = 2;
//		light.shadow.camera.near = 0.1;
//		light.shadow.camera.far = 400;
	    }
	    if(this.addLightHelper()) {
		const sphereSize = 10;
		const pointLightHelper = new THREE.DirectionalLightHelper( light, sphereSize );
		this.scene.add( pointLightHelper );
		this.lights.push(pointLightHelper);
	    }
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
    castShadows:function() {
	return this.getProperty("castShadows");
    },
    addLightHelper:function() {
	return this.getProperty("addLightHelper");
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
	return jqid(this.getManager().divId+"_annotations");
    },	
    loadModel:function(model) {
	let loading = "";
	if(this.models.length>0 && this.models[0].thumbnail) {
	    let thumbnail = this.models[0].thumbnail
	    loading+=HU.div(['id',this.domId('_loadingbg'),'style',HU.css('max-height',HU.getDimension(this.opts.height),'overflow-y','hidden','margin','1px','position','absolute','left','0px','right','0px')],
			    HU.image(thumbnail,['width','100%','style','filter: blur(3px);-webkit-filter: blur(3px);']));
	}

	loading += HU.div(['style',HU.css('width','100px','text-align','center','position','absolute','left','10px','top','10px')],
			     "Loading..." +"<br>" +
			     HU.image(ramaddaBaseUrl  + "/icons/mapprogress.gif",['style',HU.css('width','60px')]));
	
	this.jq('_background').html(loading);



	let _this = this;
	let update = (xhr)=>{
	};
	let error =  (error) => {
	    this.incrLoading(-1);
	    jqid(this.divId).html(error);
	    console.log(error)
	};
	let url = model.url;
	if(url.match(/\.gltf/gi)|| url.match(/\.glb/gi)) {
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
	} else  if(url.match(/\.stl/gi)) {
	    const loader = new THREE.STLLoader();
	    this.incrLoading(1);
	    loader.load(url,geometry =>{
		this.incrLoading(-1);
		console.log("loaded stl model:" + model.name +" " +url);
		const material = new THREE.MeshPhysicalMaterial({
		    color: 0xb2ffc8,
		    //    envMap: envTexture,
		    metalness: 0.25,
		    roughness: 0.1,
		    opacity: 1.0,
		    transparent: true,
		    transmission: 0.99,
		    clearcoat: 1.0,
		    clearcoatRoughness: 0.25
		})
		console.dir(geometry)
		const mesh = new THREE.Mesh(geometry, material)
		this.scene.add( mesh);
//		this.initObject(model,player);
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
	object.castShadow=true;
	object.traverse( function ( child ) {
	    if (child.isMesh ) {
		child.castShadow=true;
	    }
	} );		

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
	this.addBbox(model,this.getModelProperty(model,"bboxColor"));
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
	    if(model.bbox) {
		this.scene.remove(model.bbox);
		model.bbox = null;
	    }
	};
	if(visible.length==1) {
	    clear(visible[0]);
	    this.addBbox(visible[0],this.getModelProperty(visible[0],"bboxColor"));
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
	    this.addBbox(model,this.getModelProperty(model,"bboxColor"));
	});
    },

    getProperty:function(what,dflt,debug) {
	if(debug)
	    console.log("getProperty:" + what);
	if(Utils.isDefined(this.properties[what])) {
	    if(debug)
		console.log("\tfrom this.properties[what]");
	    return this.properties[what];
	}
	let v = null;
	this.models.every(model=>{
	    if(!model.visible) {
		if(this.models.length>1)
		    return true;
	    }
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
	if(debug)
	    console.log("\tfrom dflt:" + dflt);
	return dflt;
    },

    getModelProperty:function(model,what,dflt) {
	let idx = model.index;
	if(Utils.isDefined(model[what])) {
	    return model[what];
	}
	return this.opts[what+"_" + model.id] || this.opts[what+"_" + idx] || this.opts[what] || dflt;
    },
    addBbox:function(model, color) {
	if(!this.getModelProperty(model,"showBbox",false)) {
	    return;
	}
	if(typeof color == "string") color = new THREE.Color(color).getHex();
	if(!Utils.isDefined(color)) color = 0xff0000;
	if(model.bbox) {
	    this.scene.remove(model.bbox);
	}
	model.bbox = new THREE.BoxHelper(model.object, color);
	model.bbox.update();
	this.scene.add(model.bbox);	    
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

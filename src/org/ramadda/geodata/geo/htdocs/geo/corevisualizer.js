
var DISPLAY_CORE = "core";
addGlobalDisplayType({
    type: DISPLAY_CORE,
    label: "Core Visualizer",
    requiresData: false,
    forUser: true,
    category: CATEGORY_CONTROLS,
});


var ID_CV_CONTENTS = 'cv_contents';
var ID_CV_CANVAS = 'cv_canvas';
var ID_CV_MENUBAR = 'cv_menubar';
var ID_CV_DISPLAYS = 'cv_displays';
var ID_CV_DISPLAYSBAR = 'cv_displays_bar';
var ID_CV_DISPLAYS_ADD='cv_displays_add';




var ID_CV_MEASURE = 'measure';
var ID_CV_SAMPLE = 'sample';
var ID_CV_DOROTATION = 'doRotation';
var ID_CV_COLUMN_WIDTH = 'columnwidth';
var ID_CV_IMAGEWIDTHSCALE = 'imageWidthScale';
var ID_CV_SCALE_LABEL = 'scalelabel';

//These sync up with the opts args and are used for the wiki props and url args
var ID_CV_SHOWLABELS = 'showLabels';
var ID_CV_SHOWALLDEPTHS= 'showAllDepths';
var ID_CV_ASFEET = 'asFeet';
var ID_CV_SHOWHIGHLIGHT = 'showHighlight';
var ID_CV_SHOWBOXES = 'showBoxes';
var ID_CV_SHOWPIECES='showPieces';

var ID_CV_GOTO = 'goto';
var ID_CV_RELOAD = 'reload';
var ID_CV_COLLECTIONS= 'collections';
var CV_LINE_COLOR='#aaa';
var CV_HIGHLIGHT_COLOR = 'red';
var CV_SAMPLE_COLOR = 'red';
var CV_BOX_COLOR = 'red';
var CV_STROKE_WIDTH = 0.4;
var CV_AXIS_WIDTH=100;
var CV_ANNOTATIONS_WIDTH=200;
var CV_LEGEND_WIDTH=150;
var CV_OFFSET_X=170;
var CV_FONT_SIZE = 15;
var CV_FONT_SIZE_SMALL = 10;
var CV_TICK_WIDTH = 8;


function RamaddaCoreDisplay(displayManager, id, args) {
    const SUPER  = new RamaddaDisplay(displayManager, id, DISPLAY_CORE, args);
    let myProps =[];
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
	clearRecordSelection: function(source, args) {
	    let displays = this.getDisplayManager().getDisplays();
	    displays.forEach((display,idx)=>{
		if(display==this) return;
		if(display.clearAnnotations) display.clearAnnotations();
	    });
	    if(!this.recordSelect) return;
	    if(this.recordSelect.dialog)
		this.recordSelect.dialog.remove();
	    if(this.recordSelect.line) 
		this.destroy(this.recordSelect.line);
	    this.recordSelect.dialog= null;
	    this.recordSelect.line=null;
	},
	findDepthField:function(record) {
	    let depthField;
	    record.fields.every(f=>{
		let id = f.getId();
		if(id=='depth' || id=='section_depth') {
		    depthField=f;
		    return false;
		}
		return true;
	    });
	    return depthField;
	},
	handleEventRecordSelection: function(source, args) {
	    if(!this.recordSelect) {
		this.recordSelect={
		}
	    }
	    this.clearRecordSelection();
	    if(!args.record) return;
	    let depthField = this.findDepthField(args.record);
	    if(depthField) {
		let depth =depthField.getValue(args.record);
		let y = this.worldToCanvas(value);
		this.goToWorld(depth);
		this.sampleAtDepth(depth);
		return;
		let html = this.applyRecordTemplate(args.record,null,null, '${default}');
		let left = false;
		html = HU.div([ATTR_STYLE,HU.css('padding','5px')],html);
		this.recordSelect.dialog =
		    HU.makeDialog({anchor:this.mainDiv,
				   callback:()=>{
				       this.clearRecordSelection();
				   },
				   title:'Record',
				   decorate:true,
				   at:(left?'left top':'right top'),
				   my:(left?'left top':'right top'),
				   header:true,
				   content:html,
				   draggable:true});


	    }
	},

	updateUI:  function() {
	    if(this.haveDrawn) return;
	    this.haveDrawn = true;
	    let menuItemsLeft = [];
	    let menuItemsRight = [];    
	    let canvas =HU.div([ATTR_CLASS,'cv-canvas',ATTR_ID,this.domId(ID_CV_CANVAS)]);
	    let menuBarContainer =HU.div([ATTR_CLASS,"cv-menubar",ATTR_ID,this.domId(ID_CV_MENUBAR)]);	
	    let displaysBarContainer =HU.div([ATTR_CLASS,"cv-displaysbar",ATTR_ID,this.domId(ID_CV_DISPLAYSBAR)]);	
	    let displays =HU.div([ATTR_CLASS,'cv-displays',ATTR_ID,this.domId(ID_CV_DISPLAYS)]);
	    let main = HU.div([ATTR_CLASS,'cv-main'],canvas);	
	    let row1 = HU.tr([],HU.td([],displaysBarContainer)+
			     HU.td([],menuBarContainer));

	    let row2 = HU.tr([ATTR_VALIGN,'top'],HU.td([],displays)+
			     HU.td([],main));


	    let html=HU.table(['cellpadding','0px','cellspacing','0px',ATTR_WIDTH,'100%'],row1+row2);

	    this.jq(ID_DISPLAY_CONTENTS).html(html);

	    let displaysBar = HU.div([ATTR_TITLE,'Add data display',ATTR_ACTION,ID_CV_DISPLAYS_ADD,
				      ATTR_ID,this.domId(ID_CV_DISPLAYS_ADD),ATTR_CLASS,CLASS_CLICKABLE],
				     HU.getIconImage('fas fa-chart-line'));
	    this.jq(ID_CV_DISPLAYSBAR).html(displaysBar);
	    menuItemsLeft.push(HU.span([ATTR_ID,this.domId('add'),
					ATTR_TITLE,'Add image collection',ATTR_ACTION,'add',
					ATTR_CLASS,CLASS_CLICKABLE],
				       HU.getIconImage('fas fa-plus')));

	    menuItemsLeft.push(HU.space(1));

	    menuItemsLeft.push(HU.span([ATTR_TITLE,'Reset zoom (\'=\')',ATTR_ACTION,'home',ATTR_ID,'home',ATTR_CLASS,CLASS_CLICKABLE],HU.getIconImage('fas fa-house')));


	    menuItemsLeft.push(HU.span([ATTR_TITLE,'Zoom out (\'-\')',ATTR_ACTION,'zoomout',
					ATTR_CLASS,CLASS_CLICKABLE],
				       HU.getIconImage('fas fa-magnifying-glass-minus')));

	    menuItemsLeft.push(HU.span([ATTR_TITLE,'Zoom in (\'+\')',ATTR_ACTION,'zoomin',
					ATTR_CLASS,CLASS_CLICKABLE],
				       HU.getIconImage('fas fa-magnifying-glass-plus')));
	    menuItemsLeft.push(HU.span([ATTR_TITLE,'Pan down',ATTR_ACTION,'down',
					ATTR_CLASS,CLASS_CLICKABLE],
				       HU.getIconImage('fas fa-arrow-down')));
	    menuItemsLeft.push(HU.span([ATTR_TITLE,'Pan up',ATTR_ACTION,'up',
					ATTR_CLASS,CLASS_CLICKABLE],
				       HU.getIconImage('fas fa-arrow-up')));
	    menuItemsRight.push(HU.span([ATTR_TITLE,'Show Gallery',
					 ATTR_ACTION,'gallery',
					 ATTR_CLASS,CLASS_CLICKABLE],
					HU.getIconImage('fas fa-images')));




	    menuItemsLeft.push(HU.space(1));
	    menuItemsLeft.push(HU.input('','',[ATTR_SIZE,'10',ATTR_ID,this.domId(ID_CV_GOTO),ATTR_PLACEHOLDER,"Go to depth"]));
	    menuItemsLeft.push(HU.space(1));
	    menuItemsLeft.push(HU.div([ATTR_STYLE,HU.css('display','inline-block','padding-left','5px','padding-right','5px'),
				       ATTR_ID,this.domId(ID_CV_MEASURE),ATTR_CLASS,CLASS_CLICKABLE,
				       ATTR_TITLE,'Measure'],
				      HU.getIconImage('fas fa-ruler-vertical')));
	    menuItemsLeft.push(HU.div([ATTR_STYLE,HU.css('display','inline-block','padding-left','5px','padding-right','5px'),
				       ATTR_ID,this.domId(ID_CV_SAMPLE),ATTR_CLASS,CLASS_CLICKABLE,
				       ATTR_TITLE,'Sample @ depth'],
				      HU.getIconImage('fas fa-eye-dropper')));	
	    menuItemsLeft.push(HU.space(1));

	    menuItemsLeft.push(HU.span([ATTR_ID,this.domId(ID_CV_COLLECTIONS),
					ATTR_STYLE,HU.css('margin-right','50px',
							  'vertical-align','top','display','inline-block','max-width','600px','overflow-x','auto')]));


	    menuItemsRight.push(HU.span([ATTR_ID,this.domId('settings'),
					 ATTR_TITLE,'Settings',ATTR_ACTION,'settings',
					 ATTR_CLASS,CLASS_CLICKABLE],
					HU.getIconImage('fas fa-cog')));
	    
	    let menuBar=Utils.join(menuItemsLeft,HU.space(1));
	    menuBar +=HU.div([ATTR_STYLE,HU.css('position','absolute','right','10px','top','4px')],
			     Utils.join(menuItemsRight,HU.space(2)));
	    this.mainDiv =jqid(this.opts.mainId);
	    if(!this.opts.showMenuBar)menuBar='';
	    this.jq(ID_CV_MENUBAR).html(menuBar);

	    addHandler({
		selectClick:function(type,id,entryId,value) {
		    _this.loadCollection(entryId);
		}
	    },this.domId('add'));

	    addHandler({
		selectClick:(type,id,entryId,value) =>{
		    this.addDisplayEntry(entryId);
		}
	    },this.domId(ID_CV_DISPLAYS_ADD));	

	    $(document).on('keydown', (event) =>{
		if(event.key=='=') {
		    this.resetZoomAndPan();
		} else    if(event.key=='+') {
		    this.zoom(1.1);
		} else  if(event.key=='-') {
		    this.zoom(0.9);
		} else if (event.key === 'Escape' || event.keyCode === 27) {
		    this.clearRecordSelection();
		    this.toggleSampling(false);
		    this.toggleMeasure(false);
		}
	    });

	    this.jq(ID_DISPLAY_CONTENTS).find('.ramadda-clickable').click(function(event){
		let action = $(this).attr(ATTR_ACTION);
		if(action=='settings') {
		    _this.showSettings($(this));
		} else	if(action=='home') {
		    _this.resetZoomAndPan();
		} else if(action=='zoomin') {
		    _this.zoom(1.1);
		} else	if(action=='zoomout') {
		    _this.zoom(0.9);
		} else	if(action==ID_CV_DISPLAYS_ADD) {
		    let id = $(this).attr('id');
		    let localeId = _this.opts.mainEntry;
		    RamaddaUtils.selectInitialClick(event,id,id,true,null,localeId,'',null);	    
		} else	if(action=='add') {
		    let id = $(this).attr('id');
		    //selectInitialClick:function(event, selectorId, elementId, allEntries, selecttype, localeId, entryType,baseUrl,props) {
		    let localeId = _this.opts.mainEntry;
		    RamaddaUtils.selectInitialClick(event,id,id,true,null,localeId,'isgroup',null);	    
		} else	if(action=='gallery') {
		    _this.showGallery($(this));
		} else	if(action=='down') {
		    let pos = _this.stage.position();
		    _this.stage.position({x:pos.x,y:pos.y-50});
		    _this.positionChanged();
		} else	if(action=='up') {
		    let pos = _this.stage.position();
		    _this.stage.position({x:pos.x,y:pos.y+50});
		    _this.positionChanged();
		}
		
	    })

	    this.jq(ID_CV_MEASURE).click(function() {
		_this.toggleSampling(false);
		_this.toggleMeasure();
	    });
	    this.jq(ID_CV_SAMPLE).click(function() {
		_this.toggleMeasure(false);
		_this.toggleSampling();
	    });	

	    HU.onReturn(this.jq(ID_CV_GOTO),obj=>{
		let depth = obj.val().trim();
		_this.goToWorld(depth);
	    });


	    let container = this.container = this.jq(ID_CV_CANVAS).get(0);

	    this.stage = new Konva.Stage({
		container: container,  
		width: container.offsetWidth,
		height: this.getScreenHeight(),
		draggable: true 
	    });
	    this.stage.on('dragmove',()=>{
		this.updateCollectionLabels();
	    });
	    this.stage.offsetY(this.opts.offsetY);

	    this.stage.scale({ x: this.opts.initScale, y: this.opts.initScale });
	    this.layer = new Konva.Layer();
	    this.legendLayer = new Konva.Layer();    
	    this.drawLayer = new Konva.Layer();
	    this.stage.add(this.legendLayer);
	    this.stage.add(this.layer);
	    this.stage.add(this.drawLayer);
	    this.annotationLayer = new Konva.Layer();
	    this.stage.add(this.annotationLayer);

	    this.addEventListeners();
	    this.collections = [];
	    
	    this.loadingMessage= this.makeText(this.annotationLayer,"Loading...",50,50, {fontSize:45});

	    if(this.opts.collectionIds) {
		Utils.split(this.opts.collectionIds,",",true,true).forEach(entryId=>{
		    this.loadCollection(entryId);
		});
	    }


	    this.drawLegend();
	    this.layer.draw();

	    this.displayEntries = [];
	    if(this.opts.displayEntries) {
		Utils.split(this.opts.displayEntries,'\n',true,true).forEach((line,idx)=>{
		    let toks = Utils.split(line,',',true,true);
		    let entryId = toks[0];
		    toks.shift();
		    if(entryId.startsWith('#')) return;
		    this.displayEntries.push({
			display:null,
			entryId:entryId,
			props:this.parseDisplayProps(toks)
		    });
		});
	    }
	    this.loadDisplays();

	},

    });


    //function RamaddaCoreVisualizer(collection,container,args) {
    if(!window['cv-base-id']) window['cv-base-id']=1;
    this.id = window['cv-base-id']++;
    let _this = this;
    this.entries = [];
    if(!args) args={};
    
    this.opts = {
	screenHeight:args.height??800,
	maxColumnWidth:300,
	offsetY:0,
	//This is the scale applied to the world coordinates on the image
	scaleY:1,
	//This is the initial canvas scale
	initScale:1.0,
	imageWidthScale:1.0,
	legendWidth:0,
	hadLegendWidth:Utils.isDefined(args.legendWidth),

	showAnnotations:true,	
	axisWidth:CV_AXIS_WIDTH,
	measureStyle:{
	    outline:'#aaa',
	    fontSize: 12,
	    background:'#efefef',
	    fill: 'black'},
	hadAxisWidth:Utils.isDefined(args.axisWidth),	
	top:0,
	range: {
	},
	boxLabelStyle: {background:'rgb(224, 255, 255)',fill:'black',fontSize:CV_FONT_SIZE_SMALL},

	showLegend:true,
	showLabels:true,
	showPieces:false,
	showAllDepths:false,
	asFeet:false,
	showBoxes:true,
	showHighlight:false,
	showMenuBar:true,

	bgPaddingX:4,
	bgPaddingY:2	
    }

    //check for numbers as strings
    for (const [key, value] of Object.entries(args)) {
	if (typeof value === 'string') {
	    if(value=="true")
		args[key] = true;
	    else if(value=="false")
		args[key] = false;		
	    else if(!isNaN(value)) 
		args[key] = +value;

	}
    }



    $.extend(this.opts,args);

    //define setters/getters & check for url override
    [ID_CV_SHOWLABELS, ID_CV_SHOWALLDEPTHS, ID_CV_ASFEET, ID_CV_SHOWHIGHLIGHT, ID_CV_SHOWBOXES,
     ID_CV_SHOWPIECES,ID_CV_DOROTATION,ID_CV_IMAGEWIDTHSCALE].forEach(prop=>{
	let getName =  'get' + prop.substring(0, 1).toUpperCase() + prop.substring(1);
	let setName =  'set' + prop.substring(0, 1).toUpperCase() + prop.substring(1);	
	let getFunc = (dflt)=>{
	    return  this.getCoreProperty(prop,dflt);
	};
	let setFunc = (v)=>{
	    this.setCoreProperty(prop,v);
	};
	if(!this[getName])
	    this[getName] = getFunc;
	if(!this[setName])
	    this[setName] = setFunc;	

	let value = HU.getUrlArgument(prop);
	if(Utils.isDefined(value)) {
	    this.setCoreProperty(prop,value);
	}
    });

    this.opts.top =+this.opts.top;

}


RamaddaCoreDisplay.prototype = {
    needsData: function() {
        return false;
    },


    getHasBoxes:function() {
	return this.getProperty('hasBoxes');
    },
    setHasBoxes:function(value) {
	this.setProperty('hasBoxes',value);
    },    
    getCoreProperty:function(key,dflt,debug) {
	let v = this.opts[key];
	if(debug) console.log('getProperty:',key,v);
	if(!Utils.isDefined(v)) return dflt;
	return v;
    },
    setCoreProperty:function(key,v) {
	this.opts[key] = v;
	return this.propertyChanged(key,v);
    },    
    propertyChanged:function(prop,value) {
	HU.addToDocumentUrl(prop,this.getCoreProperty(prop));
	return value;
    },
    getXOffset:function(column) {
	let max = +this.opts.maxColumnWidth*this.getImageWidthScale();
	return this.getAxisWidth()+100+column*(max+100);
    },
    getAxisWidth:function() {
	return this.opts.axisWidth;
    },
    goToWorld:function(world) {
	let scale = this.stage.scale().y;
	let y = this.worldToCanvas(world);
	y = y*scale;
	//Offset a bit
	y-=20;
	this.stage.position({ x: 0, y: -y });
	this.positionChanged();
    },

    worldToCanvas:function(w,debug) {
	w = w*this.opts.scaleY;
	let range = this.opts.range;
	let r = range.max-range.min;
	let h = this.getCanvasHeight();
	let c =   h*(w/r);
	if(debug) {
	    //	    console.log('worldToCanvas',w,c);    console.log('canvasToWorld',this.canvasToWorld(c));
	}
	return c;
    },
    canvasToWorld:function(c) {
	let scale = this.stage.scale().y;
	//	console.log('canvas',c,this.opts.offsetY,this.opts.scaleY,scale);
	let range = this.opts.range;
	let r = range.max - range.min;
	let h=this.getCanvasHeight();
	let world =  (r*c)/h;
	world = world/this.opts.scaleY;
	return world;
    },
    calculateDistance:function(x1, y1, x2, y2) {
	return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    },
    clearMeasure:function() {
	if(!this.measureState) return;
	if(this.measureState.line) {
	    this.measureState.line.destroy();
	    this.measureState.line=null;
	}
	if(this.measureState.text1) {
	    this.destroy(this.measureState.text1);
	    this.measureState.text1=null;
	}	
	if(this.measureState.text2) {
	    this.destroy(this.measureState.text2);
	    this.measureState.text2=null;
	}	
	this.drawLayer.draw();
    },
    isSampling: function() {
	return this.sampling;
    },

    toggleSampling:function(s) {
	if(Utils.isDefined(s)) {
	    this.sampling = s;
	}  else {
	    this.sampling = !this.sampling;
	}

    	if(this.isSampling()) {
	    this.stage.container().style.cursor = 'crosshair';
	    this.jq(ID_CV_SAMPLE).css('background','#ccc');
	} else {
	    if(this.sampleDialog) this.sampleDialog.remove();
	    this.clearRecordSelection();
	    this.sampleDialog=null;
	    if(this.stage.container().style.cursor == 'crosshair')
		this.stage.container().style.cursor = 'default';
	    this.jq(ID_CV_SAMPLE).css('background','transparent');
	    return;
	}


	if(this.samplingInit) return;
	this.samplingInit=true;
	this.stage.on('mousedown', (e) => {
	    if(!this.isSampling()) return;
	    const pos = this.stage.getRelativePointerPosition();
	    let depth = this.canvasToWorld(pos.y);
	    this.sampleAtDepth(depth);
	});
    },

    formatDepth:function(n) {
	n = parseFloat(n);
	if(this.getCoreProperty(ID_CV_ASFEET)){
	    n = n*3.28084;
	}
	return n.toFixed(3);
	let f =  Utils.formatNumber(n);
	return f;
    },
    sampleAtDepth:function(depth) {
	this.clearRecordSelection();
	let y = this.worldToCanvas(depth);	    
	let displays = this.getDisplayManager().getDisplays();
	let html =
	    HU.div([],HU.b('Selected depth: ') + this.formatDepth(depth))  + HU.div([ATTR_CLASS,'ramadda-thin-hr']);
	if(!this.recordSelect) {
	    this.recordSelect={
	    }
	}
	this.recordSelect.line = new Konva.Line({
	    points: [this.getAxisWidth(), y, 2000,y],
	    stroke: CV_SAMPLE_COLOR,
	    strokeWidth: 2,
	    lineCap: 'round',
	    lineJoin: 'round',
	});

	this.drawLayer.add(this.recordSelect.line);
	this.drawLayer.draw();

	let colors  = ['red','green','blue'];

	let maxHeight='100px;';
	if(displays.length<=2)
	    maxHeight='400px';
	
	displays.forEach((display,idx)=>{
	    if(display==this) return;
	    if(display.addAnnotation) display.addAnnotation(depth);
	    let records = display.getRecords();
	    if(records==null || records.length==0) return;
	    let closest = null;
	    let min = 0;
	    let depthField = this.findDepthField(records[0]);
	    if(depthField==null) return;
	    records.forEach(record=>{
		let v = depthField.getValue(record);
		let diff  = Math.abs(v-depth);
		if(closest==null || diff<min) {
		    min =diff;
		    closest=record;
		}
	    });
	    if(!closest) return;
	    if(html!='')
		html+=HU.div([ATTR_CLASS,'ramadda-thin-hr']);
	    html += HU.div([],HU.b(display.getTitle()));
	    let recordHtml = this.applyRecordTemplate(closest,null,null, '${default}');
	    html+=HU.div([ATTR_STYLE,HU.css('max-height',maxHeight,'overflow-y','auto')],
			 recordHtml);
	});
	if(Utils.stringDefined(html)) {
	    html=HU.div([ATTR_STYLE,HU.css('padding','5px')], html);
	    if(this.sampleDialog) this.sampleDialog.remove();
	    this.sampleDialog =
		HU.makeDialog({anchor:this.mainDiv,
			       callback:()=>{
				   this.clearRecordSelection();
			       },
			       title:'Sample',
			       decorate:true,
			       at:'right top',
			       my:'right top',
			       header:true,
			       content:html,
			       draggable:true});

	}
    },
    isMeasuring: function() {
	return this.measuring;
    },


    toggleMeasure:function(s) {
	if(Utils.isDefined(s)) {
	    this.measuring = s;
	}  else {
	    this.measuring = !this.measuring;
	}

    	if(this.isMeasuring()) {
	    this.jq(ID_CV_MEASURE).css('background','#ccc');
	    this.stage.container().style.cursor = 'row-resize';
	    this.stage.setAttrs({draggable: false});
	} else {
	    if(this.stage.container().style.cursor == 'row-resize')
		this.stage.container().style.cursor = 'default';
	    this.jq(ID_CV_MEASURE).css('background','transparent');
	    this.clearMeasure();
	    this.stage.setAttrs({draggable: true});
	    return;
	}

	if(this.measureState) return;
	this.measureState= {
	    start:null,
	    line:null
	}

	this.stage.on('mousedown', (e) => {
	    if(!this.isMeasuring()) return;
	    this.clearMeasure();
	    const pos = this.stage.getRelativePointerPosition();
	    this.measureState.start = pos;
	    this.measureState.line = new Konva.Line({
		points: [pos.x, pos.y, pos.x, pos.y],
		stroke: CV_SAMPLE_COLOR,
		strokeWidth: 2,
		lineCap: 'round',
		lineJoin: 'round',
	    });
	    this.scaleLine(this.measureState.line);
	    this.drawLayer.add(this.measureState.line);
	    let world  =this.canvasToWorld(pos.y);	    
	    this.measureState.text1 = this.makeText(this.drawLayer,
						    this.formatDepth(world),
						    this.measureState.start.x+5,
						    pos.y,
						    $.extend({debug:true},this.opts.measureStyle));
	    this.drawLayer.draw();
	});

	this.stage.on('mousemove', (e) => {
	    if(!this.isMeasuring()) return;
	    if (!this.measureState.line || !this.measureState.start) return;
	    const pos = this.stage.getRelativePointerPosition();
	    if(this.measureState.text2)this.destroy(this.measureState.text2);
	    this.measureState.text2 = this.makeText(this.drawLayer,
						    this.formatDepth(this.canvasToWorld(pos.y)),
						    this.measureState.start.x+5,
						    pos.y,
						    this.opts.measureStyle);
	    this.drawLayer.add(this.measureState.text2);
	    this.measureState.line.points([this.measureState.start.x, this.measureState.start.y, this.measureState.start.x, pos.y]);
	    this.drawLayer.draw();
	});

	this.stage.on('mouseup', (e) => {
	    if(!this.isMeasuring()) return;
	    if (!this.measureState.start) return;
	    const pos = this.stage.getRelativePointerPosition();
	    const distance = this.calculateDistance(this.measureState.start.x, this.measureState.start.y, this.measureState.start.x, pos.y);
	    // Display the measurement as text on the canvas
	    this.drawLayer.draw();
	    this.measureState.start=null;
	});
    },

    deleteDisplay:function(e) {
	jqid(e.props.divid).remove();
	const indexToRemove = this.displayEntries.findIndex(item => item.props.divid === e.props.divid);
	if (indexToRemove !== -1) {
	    let display = this.displayEntries[indexToRemove].display;
	    this.displayEntries.splice(indexToRemove, 1);
	    this.getDisplayManager().removeDisplay(display);
	}
    },
    loadDisplays:function() {
	this.displayEntries.forEach((e,idx)=>{
	    let props = e.props;
	    let entryId = e.entryId;
	    if(e.display)  return;
	    this.getEntry(entryId, entry => {
		$.extend(props, {
		    "entry":entryId,
		    "entryId":entryId,
		    "data":new PointData("Display",  null,null,
					 "/repository/entry/data?entryid=" + entryId+"&max=10000",
					 {entryId:entryId}),
		});
		if(!props.title) {
		    props.title = entry.getName();
		    if(props.title && props.title.length>20) {
			props.title = props.title.substring(0,19)+'...';
		    }
		}		    
		//Add the hook for when the display pops up its dialog to add the Delete button
		props.dialogHook = (what,v)=>{
		    if(what=='contents') {
			//The v is the dialog html
			return HU.div([ATTR_STYLE,HU.css('margin','5px')], HU.div([ATTR_CLASS,'cv-display-delete'],'Delete display')) + v;
		    }  else if(what=='init') {
			//the v is the dialog
			v.find('.cv-display-delete').button().click(()=>{
			    v.remove();
			    this.deleteDisplay(e);
			});
		    }
		};
		let divId = HU.getUniqueId('display_');
		let div = HU.div([ATTR_ID,divId,ATTR_STYLE,'display:inline-block;min-width:150px;']);
		props.divid = divId;
		this.jq(ID_CV_DISPLAYS).append(div);
		e.display = this.getDisplayManager().createDisplay(props.displayType,props);
	    });
	});
    },

    addDisplayEntry:function(entryId) {
	let toks=[];
	let html = 'Enter display properties. e.g.:<pre>indexField:&lt;field id&gt;\nfields:&lt;field id&gt;\ntitle:Some title\netc.</pre>';
	html+=HU.textarea('',this.lastDisplayProps??'',[ATTR_ID,this.domId('displayprops'),ATTR_STYLE,HU.css('width','400px'),'rows','5']);
	let buttons = [HU.div([ATTR_ID,this.domId('cancel')],'Cancel'),	
		       ' '+   HU.div([ATTR_ID,this.domId('add_display')],'Create Display')];
	html+=HU.buttons(buttons);
	html=HU.div([ATTR_STYLE,HU.css('padding','5px')], html);
	
	let anchor =  this.jq(ID_CV_DISPLAYS_ADD);
	let dialog = HU.makeDialog({content:html,title:'Display Properties',
				    draggable:true,header:true,my:'left top',at:'left bottom',anchor:anchor});

	this.jq('cancel').button().click(()=>{
	    dialog.remove();
	});
	this.jq('add_display').button().click(()=>{
	    this.lastDisplayProps = this.jq('displayprops').val();
	    let props = Utils.split(this.lastDisplayProps,'\n',true,true);
	    dialog.remove();
	    this.processDisplayEntry(entryId,props);
	});


    },

    processDisplayEntry:function(entryId,props) {
	props = this.parseDisplayProps(props);
	this.displayEntries.push({
	    display:null,
	    entryId:entryId,
	    props:props
	});
	this.loadDisplays();
    },


    parseDisplayProps:function(toks) {
	let props = {
	    'displayType':'profile',
	    "profileMode":"lines",
	    "indexField":"depth|section_depth",
	    //Use second numeric field
	    "fields":"@2",
	    "showLegend":false,
	    "showTitle":true,
	    "width":"100%",
	    "height":"500px",
	    "yAxisReverse":true,
	    "marginTop":"0",
	    "marginRight":"0",
	    "showMenu":true,
	    "max":"10000",

	};
	for(let i=0;i<toks.length;i++) {
	    let pair = toks[i];
	    let tuple = Utils.split(pair,':',true);
	    if(tuple.length<1) continue;
	    let key  =tuple[0];
	    let value = tuple[1];
	    if(key=='fields') value = value.split(',');
	    props[key] = value; 
	}
	return props;
    },



    showSettings:function(anchor) {
	let _this = this;
	let html = '';
	html+=HU.div([],
		     HU.checkbox(this.domId(ID_CV_SHOWLABELS),
				 [ATTR_ID,this.domId(ID_CV_SHOWLABELS)],
				 this.getShowLabels(),'Show Labels'));

	html+=HU.div([],
		     HU.checkbox(this.domId(ID_CV_SHOWBOXES),
				 [ATTR_ID,this.domId(ID_CV_SHOWBOXES)],this.getCoreProperty(ID_CV_SHOWBOXES),'Show Boxes'));    

	html+=HU.div([],HU.space(2) +
		     HU.checkbox(this.domId(ID_CV_SHOWALLDEPTHS),
				 [ATTR_ID,this.domId(ID_CV_SHOWALLDEPTHS)],this.getShowAllDepths(),'Show All Depths'));
	

	html+=HU.div([],
		     HU.checkbox(this.domId(ID_CV_SHOWHIGHLIGHT),
				 [ATTR_ID,this.domId(ID_CV_SHOWHIGHLIGHT)],
				 this.getCoreProperty(ID_CV_SHOWHIGHLIGHT),
				 'Show Highlight'));    
	if(this.getHasBoxes()) {
	    html+=HU.div([],
			 HU.checkbox(this.domId(ID_CV_SHOWPIECES),
				     [ATTR_ID,this.domId(ID_CV_SHOWPIECES)],
				     this.getCoreProperty(ID_CV_SHOWPIECES),'Show Pieces'));

	}

	html+=HU.div([],
		     HU.checkbox(this.domId(ID_CV_DOROTATION),
				 [ATTR_ID,this.domId(ID_CV_DOROTATION)],this.getCoreProperty(ID_CV_DOROTATION),
				 'Do Rotation'));


	html+=HU.div([],
		     HU.checkbox(this.domId(ID_CV_ASFEET),
				 [ATTR_ID,this.domId(ID_CV_ASFEET)],this.getCoreProperty(ID_CV_ASFEET),
				 'Display Feet'));
	
	html+= HU.div([ATTR_STYLE,HU.css('margin-bottom','0.5em')],
		      'Image Width Scale: ' +
		      HU.span([ATTR_STYLE,HU.css('display','inline-block','min-width','6em'),
			       ATTR_ID,this.domId(ID_CV_SCALE_LABEL)],
			      this.getImageWidthScale()) +
		      HU.div([ATTR_STYLE,HU.css('margin-top','0.5em')],HU.div([ATTR_TITLE,'Shift-key: apply while dragging',
									       ATTR_ID,this.domId(ID_CV_IMAGEWIDTHSCALE)])));

	html+= HU.div([],
		      'Column width: ' + HU.input('',this.opts.maxColumnWidth,
						  [ATTR_ID,this.domId(ID_CV_COLUMN_WIDTH),ATTR_STYLE,'width:40px']));


	html=HU.div([ATTR_STYLE,HU.css('padding','10px')], html);
	if(this.settingsDialog) this.settingsDialog.remove();
	let dialog =  this.settingsDialog = HU.makeDialog({anchor:anchor,
							   decorate:true,
							   at:'right bottom',
							   my:'right top',
							   header:true,
							   content:html,
							   draggable:true});

	
	HU.onReturn(this.jq(ID_CV_COLUMN_WIDTH),obj=>{
	    _this.opts.maxColumnWidth=+obj.val();
	    _this.drawCollections();
	});


	this.jq(ID_CV_IMAGEWIDTHSCALE).slider({
	    //range: true,
	    min: 0.1,
	    max: 10,
	    step:0.1,
	    value:this.getImageWidthScale(),
	    slide: function( event, ui ) {
		_this.setImageWidthScale(+ui.value);
		_this.jq(ID_CV_SCALE_LABEL).html(_this.getImageWidthScale());
		if(event?.originalEvent?.originalEvent?.shiftKey) {
		    _this.drawCollections();
		    _this.toggleBoxes();
		}
	    },
	    stop: function(event,ui) {
		_this.drawCollections();
		_this.toggleBoxes();

	    }
	});

	this.jq(ID_CV_SHOWLABELS).change(function(){
	    _this.setCoreProperty(ID_CV_SHOWLABELS, $(this).is(':checked'));
	    _this.toggleLabels();
	});
	this.jq(ID_CV_DOROTATION).change(function(){
	    _this.setCoreProperty(ID_CV_DOROTATION, $(this).is(':checked'));
	    _this.drawCollections({forceNewImages:true});
	});	
	this.jq(ID_CV_ASFEET).change(function(){
	    _this.setCoreProperty(ID_CV_ASFEET, $(this).is(':checked'));
	    _this.drawCollections({forceNewImages:true,resetZoom:true});
	});
	this.jq(ID_CV_SHOWALLDEPTHS).change(function(){
	    _this.setCoreProperty(ID_CV_SHOWALLDEPTHS, $(this).is(':checked'));
	    _this.toggleBoxes();
	});		
	this.jq(ID_CV_SHOWPIECES).change(function(){
	    _this.setCoreProperty(ID_CV_SHOWPIECES, $(this).is(':checked'));
	    _this.drawCollections({forceNewImages:true,resetZoom:true});
	});
	this.jq(ID_CV_SHOWBOXES).change(function(){
	    _this.setCoreProperty(ID_CV_SHOWBOXES, $(this).is(':checked'));
	    _this.toggleBoxes();
	});
	this.jq(ID_CV_SHOWHIGHLIGHT).change(function(){
	    _this.setCoreProperty(ID_CV_SHOWHIGHLIGHT, $(this).is(':checked'));
	    _this.toggleHighlight();
	});
    },


    addAnnotations:function(collection) {
	if(this.opts.showLegend && collection.legends && collection.legends.length>0) {
	    if(!collection.legendObjects) {
		collection.legendObjects=[];
	    }
	    collection.legends.forEach(l=>{
		let setPosition = image=> {
		    let y1 = this.worldToCanvas(l.top);
		    let y2 = this.worldToCanvas(l.bottom);	    
		    let aspectRatio = image.width()/ image.height()
		    let newHeight= (y2-y1)
		    let newWidth = newHeight * aspectRatio;
		    image.setAttrs({
			x: this.opts.legendWidth-newWidth,
			y: y1,
			width:newWidth,
			height:newHeight});
		}

		if(l.image) {
		    setPosition(l.image);
		    return;
		}

		if(Utils.stringDefined(l.url)) {
		    if(l.pending) {return;}
		    l.pending=true;
		    Konva.Image.fromURL(l.url,  (image) =>{
			l.image = image;
			setPosition(image);
			this.annotationLayer.add(image);
			collection.legendObjects.push(image);
		    });
		}
	    });
	}

	if(!this.opts.showAnnotations || !collection.annotations) return;
	if(collection.annotationObjects) {
	    collection.annotationObjects.forEach(obj=>{
		this.destroy(obj);
	    });
	}
	collection.annotationObjects=[];
	

	Utils.split(collection.annotations,",",true,true).forEach(a=>{
	    let tuple = a.split(';');
	    let depth = tuple[0];
	    let label = tuple[1]??'';
	    let style = Utils.convertText(tuple[2]??'');
	    let desc = Utils.convertText(tuple[3]);
	    let x= this.opts.axisWidth-75;
	    let y = this.worldToCanvas(+depth);
	    let styleObj = {doOffsetWidth:true};
	    let styles = style.split(';');
	    let lineColor = null;
	    let lineWidth=1;
	    for(let i=0;i<styles.length;i++) {
		let pair = styles[i].split(':');
		if(pair.length!=2) continue;
		if(pair[0]=='lineColor') {
		    lineColor  = pair[1];
		} else 	if(pair[0]=='lineWidth') {
		    lineWidth  = +pair[1];		    
		} else {
		    styleObj[pair[0]] = pair[1];
		}
	    }
	    if(styleObj.fontColor && !styleObj.fill) styleObj.fill = styleObj.fontColor;
	    styleObj.flag=true;
	    let pad =5;
	    let l = this.makeText(this.annotationLayer,label,x+pad,y-5, styleObj);
	    if(Utils.stringDefined(desc)) {
		this.addClickHandler(l,(e,obj)=>{
		    let y = l.getAbsolutePosition().y;
		    let world = this.canvasToWorld(y);
		    desc  =desc.replace(/\n/g,'<br>');
		    this.showPopup(label,desc,l,true);
		});
	    }
	    let tick = new Konva.Line({
		points: [x+pad, y, this.opts.axisWidth, y],
		stroke: lineColor??CV_LINE_COLOR,
		strokeWidth: lineWidth,
	    });
	    this.annotationLayer.add(tick);
	    if(lineColor) {
		let line = new Konva.Line({
		    points: [this.opts.axisWidth, y,this.opts.axisWidth+10000,y],
		    stroke: lineColor,
		    strokeWidth: lineWidth,
		});
		this.annotationLayer.add(line);
		
	    }


	    collection.annotationObjects.push(l);
	    collection.annotationObjects.push(tick);
	    if(!collection.visible) {
		this.toggleAll([l,tick],false);
	    }
	});
    },

    showGallery: function(anchor) {
	let contents=[];
	this.collections.forEach(c=>{
	    let html = '';
	    let sorted = c.data.sort((e1,e2)=>{
		return e1.topDepth-e2.topDepth;
	    });
	    sorted.forEach(entry=>{
		let label = entry.label;
		label+=' ' +this.formatDepth(entry.topDepth) +' - ' +this.formatDepth(entry.bottomDepth);
		let url = RamaddaUtil.getEntryUrl(entry.entryId);
		label = HU.href(url,label,[ATTR_TITLE,'View entry',ATTR_TARGET,'_entry']);
		html+=HU.div([],HU.b(label));
		html+=HU.div([],HU.href(url,HU.image(entry.url,[ATTR_WIDTH,'400px']),[ATTR_TITLE,'View entry',ATTR_TARGET,'_entry']));
	    });
	    html = HU.div([ATTR_STYLE,HU.css('max-height','600px','overflow-y','auto')],html);
	    contents.push({label:c.name,contents:html});
	});
	if(this.collections.length==0) {
	    contents.push({label:'',contents:'No collections are available'});
	}
	let gallery;
	let tabs;
	if(contents.length>1) {
	    tabs = HU.makeTabs(contents);
	    gallery = tabs.contents;
	} else {
	    gallery=HU.b(contents[0].label)  +
		HU.div([ATTR_CLASS,'cv-gallery-collection'],contents[0].contents);
	}
	gallery = HU.div([ATTR_STYLE,HU.css('margin','5px')], gallery);
	let dialog =  HU.makeDialog({anchor:anchor,
				     decorate:true,
				     at:'right top',
				     my:'right top',
				     header:true,
				     content:gallery,
				     draggable:true});
	if(tabs) {
	    tabs.init();
	}
    },
    getViewportTopY:function() {
	const scale = this.stage.scaleY();     // assume uniform scaling unless using scaleX/scaleY separately
	const pos = this.stage.position();     // current stage position (pan offset)
	return -pos.y / scale;
    },

    getScreenHeight:function() {
	return this.opts.screenHeight;
    },

    getCanvasHeight:function() {
	return this.stage.height();
    },
    
    clear:function(data) {
	this.layer.removeChildren();
	this.layer.destroyChildren() 
	this.layer.clear();
    },
    destroy:function(obj) {
	obj.destroy();
	if(obj.backgroundRect) obj.backgroundRect.destroy();
    },
    removeCollection:function(collection) {
	this.collections.splice(collection.collectionIndex, 1);
	if(collection.annotationObjects) {
	    collection.annotationObjects.forEach(obj=>{
		this.destroy(obj);
	    });
	}
	if(collection.legendObjects) {
	    collection.legendObjects.forEach(obj=>{
		this.destroy(obj);
	    });
	}	
	this.drawCollections();
    },

    loadCollection:function(entryId) {
	let _this = this;
	let url = RamaddaUtils.getUrl('/core/entries');
	url +='?entryid='+ entryId;
	$.getJSON(url, (data)=> {
	    if(data.error) {
		console.log('Error loading collection:' + entryId +' error:' + data.error);
		return;
	    }

	    _this.addCollection(data);
	}).fail((data)=>{
	    window.alert('Failed to load entries');
	});
    },

    drawCollections:function(args) {
	let opts = {
	    foreNewImages:false,
	    resetZoom:false
	}
	if(args) $.extend(opts,args);
	if(this.loadingMessage) {
	    this.loadingMessage.destroy();
	    this.loadingMessage=null;
	}
	let _this=this;

	this.clear();
	this.checkRange();
	let html = '';
	let displayIndex=0;
	this.collections.forEach((collection,idx)=>{
	    collection.collectionIndex = idx;
	    let style = HU.css('display','inline-block');
	    if(!collection.visible) style+=HU.css('background','#aaa');
	    html+=HU.span(['collection-index',idx,
			   ATTR_CLASS,'ramadda-button',
			   ATTR_STYLE,style],collection.name);
	    this.toggleAll(collection.annotationObjects,collection.visible,false)
	    this.toggleAll(collection.legendObjects,collection.visible)	    
	    if(collection.visible) {
		collection.displayIndex=displayIndex++;
		this.addEntries(collection,opts.forceNewImages);
	    }
	});
	this.jq(ID_CV_COLLECTIONS).html(html);
	this.jq(ID_CV_COLLECTIONS).find('.ramadda-button').button().click(function(event) {
	    let collection = _this.collections[+$(this).attr('collection-index')];
	    _this.showCollectionMenu(collection, $(this));
	});
	this.drawLegend();
	this.updateCollectionLabels();
	if(opts.resetZoom) {
	    this.resetZoomAndPan();
	}



    },
    showCollectionMenu:function(collection, target,args) {
	let _this = this;
	let html = '';
	html+=HU.div([ATTR_CLASS,CLASS_CLICKABLE,
		      ATTR_ACTION,'view'],'View Entry');
	html+=HU.div([ATTR_CLASS,CLASS_CLICKABLE,
		      ATTR_ACTION,'goto'],'Scroll To');	    
	html+=HU.div([ATTR_CLASS,CLASS_CLICKABLE,
		      ATTR_ACTION,'toggle'],collection.visible?'Hide':'Show');
	html+=HU.div([ATTR_CLASS,CLASS_CLICKABLE,
		      ATTR_ACTION,'delete'],'Delete');
	html=HU.div([ATTR_STYLE,HU.css('min-width','200px','padding','5px')], html);
	let opts = {anchor:target,
		    decorate:true,
		    at:'left bottom',
		    my:'left top',
		    content:html,
		    draggable:false}
	if(args) $.extend(opts,args);
	let dialog =  HU.makeDialog(opts);
	
	dialog.find('.ramadda-clickable').click(function() {
	    let action = $(this).attr(ATTR_ACTION);
	    if(action=='delete') {
		_this.removeCollection(collection);
	    } else if(action=='toggle') {
		collection.visible=!collection.visible;
		_this.drawCollections();
	    } else if(action=='goto') {
		let y = _this.worldToCanvas(collection.range.min)-50;
		let x =  collection.xPosition-200;
		//not sure what x to use
		let scale = _this.stage.scaleY();
		let pos = _this.stage.position();
		x = x*scale;
		y = y*scale;
		x=0;
		_this.stage.position({x:-x,y:-y});
		_this.positionChanged();
	    } else if(action=='view') {
		let url = RamaddaUtils.getEntryUrl(collection.entryId);
		window.open(url, '_entry');
	    }
	    dialog.remove();
	});
    },

    addCollection:function(collection) {
	collection.visible= true;
	if(!Utils.isDefined(window.cv_collection_id))
	    window.cv_collection_id = 0;
	window.cv_collection_id++;
	collection.collectionId=window.cv_collection_id;
	this.collections.push(collection);
	this.addAnnotations(collection);
	this.drawCollections();
	this.toggleLabels();
	this.toggleBoxes();
    },
    addEntries:function(collection,forceNewImages) {
	let column = collection.displayIndex;
	collection.xPosition =  this.getXOffset(column);

	let x = this.getXOffset(column);
	let l = this.makeText(this.layer,collection.name,x,this.worldToCanvas(collection.range.min)-20,
			      {fontStyle:'bold'});
	collection.nameText = l;
	l.collectionId = collection.collectionId;
	this.addClickHandler(l,(event)=>{
	    let canvas = jqid(this.domId(ID_CV_MENUBAR));
	    let dx = event.evt.layerX;
	    let dy = event.evt.layerY+15;	    
	    let args = 	    {at:'left+'+  dx +' bottom+' + dy};
	    this.showCollectionMenu(collection, canvas,args);
	});
	this.checkRange();
	let range =  this.opts.range;
	//	console.log(collection.name,'range',range.min,range.max,collection.data.length);
	collection.data.forEach((entry,idx)=>{
	    entry.collection = collection;
	    entry.displayIndex = column;
	    this.addCoreEntry(entry,forceNewImages);
	});
    },

    toggleAll:function(l,visible,debug) {
	if(!l) return;
	if(debug)
	    console.log('toggle',l.length);
	l.forEach(obj=>{
	    this.toggle(obj,visible);
	});
    },
    toggle:function(obj,visible) {
	if(!obj) return;
	if(obj.isDepthLabel && visible) {
	    visible =this.getShowAllDepths();
	}
	if(visible) {
	    obj.show();
	    if(obj.backgroundRect)
		obj.backgroundRect.show();
	} else {
	    obj.hide();
	    if(obj.backgroundRect)
		obj.backgroundRect.hide();
	}		
    },

    applyToggle:function(entry) {
	this.toggleBoxes(entry);
	this.toggleLabels(entry);	
    },

    toggleBoxes: function(entry) {
	let show = this.opts.showBoxes;
	let entries = entry?[entry]:this.entries;
	entries.forEach(e=>{
	    this.toggleAll(e.boxShapes,show);
	});
    },

    toggleLabels: function(entry) {
	let show = this.getShowLabels();
	let entries = entry?[entry]:this.entries;
	entries.forEach(e=>{
	    this.toggleAll(e.labels,show);
	});
    },
    toggleHighlight: function() {
	let show = this.getShowHighlight();
	this.entries.forEach(e=>{
	    if(!e.highlight) return;
	    if(show) {
		e.highlight.stroke(CV_HIGHLIGHT_COLOR);
	    } else {
		e.highlight.stroke('transparent');
	    }		
	});
    },
    showPopup:function(label,text,obj,left) {
	let _this = this;
	if(!text) return;
	if(obj &&  obj.dialog) obj.dialog.remove();
	text = Utils.convertText(text);
	let div = HU.div([ATTR_STYLE,HU.css('margin','10px')],
			 text);
	let dialog =  HU.makeDialog({anchor:this.mainDiv,
				     title:label,
				     decorate:true,
				     at:(left?'left top':'right top'),
				     my:(left?'left top':'right top'),
				     header:true,
				     content:div,
				     draggable:true});

	
	dialog.find('.wiki-image img').css('cursor','pointer');
	dialog.find('.wiki-image img').click(function() {
	    let url  =$(this).attr('src');
	    let div = HU.image(url,[ATTR_WIDTH,'800px']);
	    let idialog =  HU.makeDialog({anchor:dialog,
					  decorate:true,
					  at:'right top',
					  my:'right top',
					  header:true,
					  content:div,
					  draggable:true});

	    dialog.remove();
	    
	});

	if(obj) obj.dialog=dialog;
    },
    positionChanged:function() {
	this.updateCollectionLabels();
    },
    scaleLine:function(obj) {
	let scale = this.stage.scaleX();
	let _scale = 1/scale;
	//	    if(!obj.isScalable) return;
	const currentWidth = obj.strokeWidth();
	if(!Utils.isDefined(obj.originalStrokeWidth)) obj.originalStrokeWidth  = currentWidth;
	const scaledWidth = obj.originalStrokeWidth * _scale;
	obj.strokeWidth(scaledWidth);
    },

    scaleChanged:function() {
	const textNodes = this.stage.find('Text');
	let stageScale = this.stage.scaleX();
	let inverseScale = 1/stageScale;
	let newScale = {
	    x: inverseScale,
	    y:inverseScale
	};

	let scaleLines = obj=>{
	    this.scaleLine(obj);
	}
	this.stage.find('Rect').forEach(scaleLines);
	this.stage.find('Line').forEach(scaleLines);	


	textNodes.forEach(text=>{
	    if(!text.scaleInfo) {
		text.scaleInfo = {
		    x:text.x(),
		    y:text.y(),
		    width:text.width(),
		    height:text.height()
		};
	    }
	    let info  = text.scaleInfo;
	    text.scale(newScale);
	    if(text.backgroundRect) {
		let rect= text.backgroundRect;
		let doOffsetWidth = rect.doOffsetWidth;
		let dim = this.getBackgroundRectDimensions(text,doOffsetWidth);
		if(rect.debug) {
		    const box = text.getClientRect();
		    console.log(box);
		}

		rect.x(dim.x);
		rect.y(dim.y);
		rect.width(dim.width);		
		rect.height(dim.height);
		rect.scale(newScale);
	    }
	});

	if(this.legendText) {
	    let skip=1;
	    if (stageScale < 0.005) skip= 100;
	    else if (stageScale < 0.007) skip= 15;
	    else if (stageScale < 0.01) skip= 13;
	    else if (stageScale < 0.013) skip= 12;
	    else if (stageScale < 0.018) skip= 9;
	    else if (stageScale < 0.025) skip= 8;
	    else if (stageScale < 0.04) skip= 7;
	    else if (stageScale < 0.07) skip= 6;
	    else if (stageScale < 0.1) skip= 5;
	    else if (stageScale < 0.4) skip= 4;
	    else if (stageScale < 0.6) skip= 3;
	    else if (stageScale < 0.8) skip= 2;	    
	    //	    console.log(stageScale,skip);
	    this.legendText.forEach((text,idx)=>{
		if(stageScale>0.9) text.setVisible(true);
		else text.visible(idx % skip === 0);
	    });
	}
	this.updateCollectionLabels();
    },
    updateCollectionLabels:function() {
	//Keep the collection labels at the top of the viewport
	let top = this.getViewportTopY();
	let scale = this.stage.scaleX();
	let margin=10;
	if(scale>1)  margin = margin/scale;
	this.collections.forEach(collection=>{
	    if(collection.nameText) {
		let text =collection.nameText; 
		let pos = text.position();
		text.position({ x: pos.x, y: top+margin });
	    }		
	});
    },

    zoom:function(scaleBy) {
	let mousePos = this.stage.getPointerPosition();
	let oldScale = this.stage.scaleX();
	let newScale = oldScale * scaleBy;
	let stagePos = this.stage.position();
	if(!mousePos) {
	    mousePos = {x:0,y:0};
	}

	let mousePointTo = {
	    x: (mousePos.x - stagePos.x) / oldScale,
	    y: (mousePos.y - stagePos.y) / oldScale
	};
	let newPos = {
	    x: mousePos.x - mousePointTo.x * newScale,
	    y: mousePos.y - mousePointTo.y * newScale
	};
	this.stage.scale({ x: newScale, y: newScale });
	this.stage.position(newPos);
	this.stage.batchDraw();
	this.scaleChanged();
    },
    addEventListeners:function() {
	window.addEventListener('resize', () => {
	    const containerWidth = this.container.offsetWidth;
	    this.stage.width(containerWidth);
	    this.stage.batchDraw();
	});

	this.stage.on('dblclick',  (e)=> {
	    this.zoom(1.2);
	});
	this.stage.on('wheel', (e) => {
	    e.evt.preventDefault(); 
	    let scale = this.stage.scaleX();
	    let oscale = scale;
	    const pointer = this.stage.getPointerPosition();
	    const zoomSpeed = 0.03;
	    const direction = e.evt.deltaY > 0 ? -1 : 1;
	    const scaleBy = 1 + direction  * zoomSpeed;
	    scale = scale*scaleBy;
	    scale = Math.max(0.005, Math.min(100, scale));
	    const newPos = {
		x: pointer.x - (pointer.x - this.stage.x()) * (scale / this.stage.scaleX()),
		y: pointer.y - (pointer.y - this.stage.y()) * (scale / this.stage.scaleY())
	    };
	    this.stage.scale({ x: scale, y: scale });
	    this.stage.position(newPos);
	    this.stage.batchDraw();
	    this.scaleChanged();
	});


	document.addEventListener('keydown', (e) => {
	    const scrollSpeed = 20;
	    const stagePos = this.stage.position();
	    switch (e.key) {
            case 'ArrowUp':

		this.stage.position({
		    x: stagePos.x,
		    y: stagePos.y + scrollSpeed
		});
		break;
            case 'ArrowDown':
		this.stage.position({
		    x: stagePos.x,
		    y: stagePos.y - scrollSpeed
		});
		break;
            case 'ArrowLeft':
		this.stage.position({
		    x: stagePos.x + scrollSpeed,
		    y: stagePos.y
		});
		break;
            case 'ArrowRight':
		this.stage.position({
		    x: stagePos.x - scrollSpeed,
		    y: stagePos.y
		});
		break;
	    default:
		return;
	    }
	    e.preventDefault();
	    this.stage.batchDraw();
	});
    },
    resetZoomAndPan:function() {
	this.stage.scale({ x: 1, y: 1 });
	this.stage.position({ x: 0, y: 0 });
	this.drawLegend();
	this.stage.batchDraw();
	this.scaleChanged();
	this.positionChanged();
    },
    getBackgroundRectDimensions:function(text,doOffsetWidth){
	let textX  = text.x();
	if(doOffsetWidth) {
	    textX-=text.width();
	}
	return {
	    x: textX-this.opts.bgPaddingX,
	    y: text.y()-this.opts.bgPaddingY,
	    width: text.width()+this.opts.bgPaddingX*2,
	    height: text.height()+this.opts.bgPaddingY*2
	};
    },

    makeText:function(layer,t,x,y,args) {
	let opts = {
	    doOffsetWidth:false,
	    fill:'#000',
	    fontSize:CV_FONT_SIZE,
	    background:null,
	    fontStyle:'',
	    strokeWidth:1,
	    flag:null
	}
	if(args) $.extend(opts,args);
	let text=  new Konva.Text({
	    x: x,
	    y: y,
	    text: t,
	    fontSize: opts.fontSize,
	    fontFamily: 'helvetica',
	    fontStyle:opts.fontStyle,
	    fill: opts.fill,
	});

	text.flag=args.flag;
	let scale = this.stage.scaleX();
	text.scale({
	    x: 1 / scale,
	    y: 1 / scale,
	});

	let textX  = text.x();
	if(opts.doOffsetWidth) {
	    text.offsetX(text.width());
	    textX-=text.width();
	}
	if(opts.background || opts.outline)  {
	    let dim = this.getBackgroundRectDimensions(text,opts.doOffsetWidth);
	    let bgAttrs = {
		fill: opts.background,
		stroke:opts.outline,
		strokeWidth: opts.strokeWidth,
		cornerRadius: 0 
	    };
	    $.extend(bgAttrs,dim);
	    let bg = new Konva.Rect(bgAttrs);
	    bg.debug = opts.debug;
	    bg.doOffsetWidth=opts.doOffsetWidth;
	    bg.scale({
		x: 1 / scale,
		y: 1 / scale,
	    });
	    layer.add(bg);
	    text.backgroundRect = bg;
	}

	layer.add(text);
	return text;
    },
    checkRange:function() {
	let min =null;
	let max =null;    
	let haveLegend = false;
	let maxLegendWidth=-1;
	let haveAnnotation = false;	
	this.collections.forEach(collection=>{
	    if(this.opts.showLegend && collection.legends && collection.legends.length>0) {
		haveLegend = true;
		collection.legends.forEach(l=>{
		    if(Utils.stringDefined(l.width)) {
			let w = +l.width;
			if(w>maxLegendWidth) maxLegendWidth=w;
		    }
		});
	    }
	    if(this.opts.showAnnotations && collection.annotations && collection.annotations.length>0) {
		haveAnnotation = true;
	    }	    

	    if(!collection.visible) return;
	    let cmin =null;
	    let cmax =null;    
	    let check = (top,bottom) =>{
		if(isNaN(top) || isNaN(bottom)) return;
		if(min === null || top<min) min =+top;
		if(max === null || bottom>max) max =+bottom;
		if(cmin === null || top<cmin) cmin =+top;
		if(cmax === null || bottom>cmax) cmax =+bottom;
	    };
	    this.setHasBoxes(false);
	    collection.data.forEach(entry=>{
		check(entry.topDepth,entry.bottomDepth);
		if(entry.boxes) {
		    entry.boxes.forEach(box=>{
			if(box.marker) return;
			if(Utils.isNumber(box.top) && Utils.isNumber(box.bottom)) {
			    entry.hasBoxes=true;
			    this.setHasBoxes(true);
			    check(box.top,box.bottom);
			}
		    });
		}

	    });
	    collection.range = {min:cmin,max:cmax};
	    //	    console.log(cmin,cmax);
	});

	if(Utils.isDefined(min)) {
	    this.opts.range={
		min:min,
		max:max,
	    };
	    let y1 = this.worldToCanvas(min);
	    let y2 = this.worldToCanvas(max);	    
	    y1-=(y2-y1-10)*0.05;
	    if(y1<-10) y1=-10;
	    let distance = y2-y1;
	    let canvasHeight = this.stage.height();
	    let scaleFactor = canvasHeight / distance; 
	    //	    this.stage.scale({ x: scaleFactor, y: scaleFactor });
	    this.stage.position({ x: 0, y: -y1 });
	}


	if(!this.opts.hadLegendWidth && haveLegend) {
	    if(maxLegendWidth>0) this.opts.legendWidth=maxLegendWidth;
	    else this.opts.legendWidth=CV_LEGEND_WIDTH;
	}


	if(!this.opts.hadAxisWidth) {
	    if(haveAnnotation) {
		this.opts.axisWidth=CV_ANNOTATIONS_WIDTH+(haveLegend?this.opts.legendWidth:0);
	    } else if(haveLegend) {
		this.opts.axisWidth=CV_AXIS_WIDTH+this.opts.legendWidth;
	    }
	}





	//	console.log('legend:',haveLegend,this.opts.legendWidth,'axis:',haveAnnotation,this.opts.axisWidth);
    },



    drawLegend:function() {
	this.legendLayer.clear();
	this.legendLayer.destroyChildren() 
	this.legendLayer.draw();
	this.checkRange();
	let axisWidth = this.getAxisWidth();
	let tickWidth = CV_TICK_WIDTH;
	let h = this.getCanvasHeight()-this.opts.top;
	let step = h/100;
	let cnt = 10;
	while(cnt-->0) {
	    let y = this.worldToCanvas(step+step)-this.worldToCanvas(step);
	    if(y>40) break;
	    step+=10;
	}
	let y1 = 0;
	let y2 = 0;	
	let range = this.opts.range;
	let bottom = range.max+(0.5*(range.max-range.min));
	step = (range.max-range.min)/6;
	this.legendText = [];
	for(let i=0;i<=bottom;i+=step) {
	    let y = this.worldToCanvas(i);
	    if(i==0) y1=y;
	    y2=y;
	    let l1 = this.makeText(this.legendLayer,this.formatDepth(i),
				   axisWidth-tickWidth,y,{doOffsetWidth:true});
	    this.legendText.push(l1);
	    let tick1 = new Konva.Line({
		points: [axisWidth-tickWidth, y, axisWidth, y],
		stroke: CV_LINE_COLOR,
		strokeWidth: CV_STROKE_WIDTH,
	    });
	    this.legendLayer.add(tick1);
	    let tick2 = new Konva.Line({
		points: [axisWidth, y, axisWidth+3000, y],
		stroke: CV_LINE_COLOR,
		strokeWidth: CV_STROKE_WIDTH,
		dash: [5, 10],
	    });
	    this.legendLayer.add(tick2);


	}

	let line = new Konva.Line({
	    points: [axisWidth, y1, axisWidth, y2],
	    stroke: CV_LINE_COLOR,
	    strokeWidth: 1,
	});
	this.legendLayer.add(line);
	this.legendLayer.draw();
	this.collections.forEach(collection=>{
	    this.addAnnotations(collection);
	});

    },

    canShowPopup:function(obj,f) {
    	if(this.isSampling() || this.isMeasuring()) return false;
	return true;
    },
    addClickHandler:function(obj,f) {
	obj.on('click', (e) =>{
	    if(!this.canShowPopup())  return;
	    f(e,obj);
	});
	obj.on('mouseover', function (e) {
	    document.body.style.cursor = 'pointer'; 
	});
	obj.on('mouseout', function (e) {
	    document.body.style.cursor = 'default'; 
	});
    },


    editEntry:function(entry,y1,y2) {
	let _this = this;
	let html = '';
	y1 = this.formatDepth(y1);
	y2 = this.formatDepth(y2);	
	html+=HU.formTable();
	html+=HU.formEntry('Name:',HU.input('',entry.label,  [ATTR_ID,this.domId('editname')]));
	html+=HU.formEntry('Top:',HU.input('',y1,  [ATTR_ID,this.domId('edittop')]));
	html+=HU.formEntry('Bottom:',HU.input('',y2, [ATTR_ID,this.domId('editbottom')]));		
	html+=HU.formTableClose();
	let buttonList =[
	    HU.div([ATTR_ACTION,'cancel','class','ramadda-button ' + CLASS_CLICKABLE],"Cancel"),
	    HU.div([ATTR_ACTION,'apply','class','ramadda-button ' + CLASS_CLICKABLE],
		   "Change Entry")];
	html+=HU.buttons(buttonList);


	if(this.editDialog) {
	    this.editDialog.remove();
	}
	let dialog = this.editDialog =  HU.makeDialog({anchor:this.mainDiv,
						       decorate:true,
						       at:'left top',
						       my:'left top',
						       header:true,
						       content:html,
						       draggable:true});
	dialog.find('.ramadda-button').button().click(function(){
	    let apply = $(this).attr(ATTR_ACTION)=='apply';
	    if(apply) {
		let name = _this.jq('editname').val();
		let top = _this.jq('edittop').val();
		let bottom = _this.jq('editbottom').val();		
		entry.label = name;
		entry.topDepth = top;
		entry.bottomDepth = bottom;
		let what = 'name';
		let value = name;
		let url = HU.url(ramaddaBaseUrl + '/entry/changefield',
				 ['entryid',entry.entryId,
				  'what1','name','value1',name,
				  'what2','top_depth','value2',top,
				  'what3','bottom_depth','value3',bottom]);			     				     
		$.getJSON(url, function(data) {
		    if(data.error) {
			alert('An error has occurred: '+data.error);
			return;
		    }
		}).fail(data=>{
		    console.dir(data);
		    alert('An error occurred:' + data);
		});

	    }
	    _this.removeEntry(entry);
	    _this.addCoreEntry(entry,false);
	    _this.layer.draw();
	    dialog.remove();
	});
    },

    removeEntry:function(entry) {
	if(!entry.group) return;
	entry.group.remove();
	this.layer.draw();
    },

    addCoreEntry:function(entry,forceNewImages) {
	if(!forceNewImages &&entry.image) {
	    this.addEntryImage(entry);
	} else {
 	    Konva.Image.fromURL(entry.url,  (image) =>{
		this.entries.push(entry);
		entry.image = image;
		this.addEntryImage(entry);
		this.applyToggle(entry);
	    });
	}
	
    },
    getCenter:function(shape) {
	const angleRad = Utils.toRadians(shape.rotation || 0);
	return {
	    x:
	    shape.x +
		shape.width / 2 * Math.cos(angleRad) +
		shape.height / 2 * Math.sin(-angleRad),
	    y:
	    shape.y +
		shape.height / 2 * Math.cos(angleRad) +
		shape.width / 2 * Math.sin(angleRad)
	};
    },
    rotateAroundPoint:function(shape, deltaDeg, point) {
	const angleRad = Utils.toRadians(deltaDeg);
	const x = Math.round(
	    point.x +
		(shape.x - point.x) * Math.cos(angleRad) -
		(shape.y - point.y) * Math.sin(angleRad)
	);
	const y = Math.round(
	    point.y +
		(shape.x - point.x) * Math.sin(angleRad) +
		(shape.y - point.y) * Math.cos(angleRad)
	);
	return {
	    ...shape,
	    rotation: Math.round(((shape.rotation??0) + deltaDeg)),
	    x,
	    y
	};
    },
    rotateAroundCenter:function(shape, deltaDeg,centerShape) {
	const center = this.getCenter(centerShape??shape);
	return this.rotateAroundPoint(shape, deltaDeg, center);
    },

    definePopup:function (obj,label,text){
	this.addClickHandler(obj,(e,obj)=>{
	    let y = obj.getAbsolutePosition().y;
	    this.showPopup(label,text,obj);
	});
    },

    isValidBox:function(box) {
	return Utils.isNumber(box.top) && Utils.isNumber(box.bottom);
    },

    makeTicks:function(entry,group,top,bottom,x,y1,y2) {
	let tickWidth=CV_TICK_WIDTH;
	let l1 = this.makeText(group,this.formatDepth(top),
			       x-tickWidth,y1,{doOffsetWidth:true,fontSize:CV_FONT_SIZE_SMALL});
	let tick1 = new Konva.Line({
	    points: [x-tickWidth, y1, x, y1],
	    stroke: CV_LINE_COLOR,
	    strokeWidth: CV_STROKE_WIDTH,
	});
	group.add(tick1);
	let l2 = this.makeText(group,this.formatDepth(bottom),
			       x-tickWidth,y2, {doOffsetWidth:true,fontSize:CV_FONT_SIZE_SMALL});
	let tick2 = new Konva.Line({
	    points: [x-tickWidth, y2, x, y2],
	    stroke: CV_LINE_COLOR,
	    strokeWidth: CV_STROKE_WIDTH,
	});
	group.add(tick2);
	entry.labels.push(l1);
	entry.labels.push(l2);
	return {l1:l1,l2:l2,tick1:tick1,tick2:tick2};
    },
    addEntryImage:function(entry,debug) {
	if(isNaN(entry.topDepth)|| isNaN(entry.bottomDepth)) return;
	entry.labels = [];
	entry.boxShapes = [];
	let addBoxShape =(shape)=> {
	    shape.isScalable = true;
	    entry.boxShapes.push(shape);
	};

	let showPieces= this.getShowPieces();
	let maxWidth=+this.opts.maxColumnWidth;
	let column = entry.displayIndex;
	if(!Utils.isDefined(column)) column = 0;
	let doRotation=entry.doRotation||this.opts.doRotation;
	let canvasY1 = this.worldToCanvas(entry.topDepth,true);
	let canvasY2 = this.worldToCanvas(entry.bottomDepth);
	let canvasHeight = (canvasY2-canvasY1);
	let image = entry.image.clone();
	let imageX = this.getXOffset(column);
	let imageY = canvasY1;
	let aspectRatio = image.width()/ image.height()
	let newHeight= (canvasY2-canvasY1)
	let newWidth = newHeight * aspectRatio;

	let group = new Konva.Group({
	    xclip: {
		x: imageX-100,
		y: imageY-100,
		width: 100+maxWidth,
		height: 10000,
            },
	    //	    draggable: this.opts.canEdit,
	    dragBoundFunc: function(pos) {
		return {
		    x: this.getAbsolutePosition().x,  
		    y: pos.y                          
		};
	    }		
	});
	this.layer.add(group);
	entry.group = group;
	
	let imageWidth = image.width()*this.getImageWidthScale();
	let imageAttrs = {
	    x: imageX,
	    y: imageY,
	    width:imageWidth,
	    height:image.height(),
        };

	let origImageCenter = {
	    x:imageX+image.width()/2,
	    y:imageY+image.height()/2
	}
	if(doRotation) {
	    imageAttrs =  this.rotateAroundCenter(imageAttrs, 90);
	}
	image.setAttrs(imageAttrs);
	let imageRect = image.getClientRect();
	let imageScale = canvasHeight/imageRect.height;
	image.scale({x:imageScale,y:imageScale});
	imageRect = image.getClientRect({ relativeTo: this.layer});

	let dx = imageRect.x-imageX;
	let dy = imageRect.y-canvasY1;
	image.position({x:image.x()-dx,y:image.y()-dy});
	let imageWidthScale = imageScale * this.getImageWidthScale();
	let scaleX =v=>{
	    return (+v)*imageWidthScale;
	}

	let scale =v=>{
	    return (+v)*imageScale;
	}
	let rotOffset = {
	    x:imageRect.x-imageX,
	    y:imageRect.y-imageY,
	}
	if(!showPieces || !entry.hasBoxes) {
	    group.add(image);
	    group.ticks = this.makeTicks(entry,group,entry.topDepth,entry.bottomDepth,imageX,imageY,imageY+newHeight);
	    let ir = image.getClientRect({relativeTo: this.layer});
	    let rect = new Konva.Rect({
		x: ir.x,
		y: ir.y,
		width:ir.width,
		height:ir.height,
		stroke: CV_HIGHLIGHT_COLOR,
		strokeWidth: 2,
	    });
	    group.add(rect);
	    if(!this.getShowHighlight()) {
		rect.stroke('transparent');
	    }
	    if(entry.boxes) {
		entry.boxes.forEach(box=>{
		    //		    if(!this.opts.showBoxes) return;
		    if(showPieces && this.isValidBox(box)) {
			return;
		    }
		    if(box.polygon) {
			let convertedPolygon= [];
			for(let i=0;i<box.polygon.length;i+=2) {
			    let x =box.polygon[i];
			    let y =box.polygon[i+1];			    
			    x = imageX+scaleX(x);
			    y = imageY+scale(y);
			    if(doRotation) {
				let boxAttrs =  {x:x,y:y};
				boxAttrs = this.rotateAroundPoint(boxAttrs, 90,origImageCenter);
				x=boxAttrs.x - rotOffset.x;
				y=boxAttrs.y - rotOffset.y;
			    }
			    convertedPolygon.push(x,y);
			}
			const polygon = new Konva.Line({
			    points: convertedPolygon,
			    stroke: CV_BOX_COLOR,
			    strokeWidth: 2,
			    closed: true,
			});
			addBoxShape(polygon);
			group.add(polygon);
		    } else {
			let boxAttrs = {
			    x: imageX+scaleX(box.x),
			    y: imageY+scale(box.y),
			    width: scaleX(box.width),
			    height:scale(box.height),
			    stroke: CV_BOX_COLOR,
			    strokeWidth: 1,
			}
			if(box.stroke) boxAttrs.stroke = box.stroke;
			if(box.fill) boxAttrs.fill = box.fill;
			if(Utils.stringDefined(box.strokeWidth)) boxAttrs.strokeWidth = box.strokeWidth;
			if(doRotation) {
			    boxAttrs =  this.rotateAroundPoint(boxAttrs, 90,origImageCenter);
			    boxAttrs.x=boxAttrs.x - rotOffset.x;
			    boxAttrs.y=boxAttrs.y - rotOffset.y;
			}
			//			boxAttrs.x*=imageScale;
			let mark;
			let labelOffset=0;
			if(box.marker) {
			    let b = boxAttrs;
			    let h = 16;
			    let w=16;
			    labelOffset=w;
			    boxAttrs.points = [
				b.x+w, b.y+h/2,                    
				b.x, b.y,      
				b.x+w, b.y - h/2
			    ];

			    let markerAttrs = {
				points:boxAttrs.points,
				closed:true,
				stroke:boxAttrs.stroke,
				fill:boxAttrs.fill,
				strokeWidth:0				
			    }

			    mark = new Konva.Line(markerAttrs);
			} else {
			    mark = new Konva.Rect(boxAttrs);
			}
			addBoxShape(mark);
			group.add(mark);
			if(Utils.stringDefined(box.label)) {
			    let l = this.makeText(group,box.label,boxAttrs.x+labelOffset+2,boxAttrs.y, this.opts.boxLabelStyle);
			    entry.boxShapes.push(l);
			} else	if(box.marker) {
			    let label = this.formatDepth(box.top);
			    let l = this.makeText(group,label,boxAttrs.x+labelOffset+2,boxAttrs.y, this.opts.boxLabelStyle);
			    entry.boxShapes.push(l);
			} else {
			    let l = this.makeText(group,this.formatDepth(box.top),boxAttrs.x+labelOffset+2,boxAttrs.y, this.opts.boxLabelStyle);
			    l.isDepthLabel = true;
			    entry.boxShapes.push(l);
			    this.toggle(l,this.getShowAllDepths());
			    l = this.makeText(group,this.formatDepth(box.bottom),
					      boxAttrs.x+labelOffset+2,
					      boxAttrs.y+boxAttrs.height,
					      this.opts.boxLabelStyle);
			    l.isDepthLabel = true;
			    entry.boxShapes.push(l);
			    this.toggle(l,this.getShowAllDepths())
			}
		    }
		});
	    }

	    this.definePopup(rect,entry.label,entry.text);
	    let imageLabel = this.makeText(group,entry.label,imageX+4,imageY+4,
					   {strokeWidth:0.4,outline:'#000',fontSize:CV_FONT_SIZE_SMALL,background:'#fff'});
	    this.definePopup(imageLabel,entry.label,entry.text);
	    this.toggleLabels();
	    this.toggle(imageLabel,this.getShowLabels());
	    entry.highlight = rect;
	    entry.labels.push(imageLabel);
	}
	

	if(showPieces && entry.boxes) {
	    entry.boxes.forEach(box=>{
		if(!this.isValidBox(box)) return;
		if(box.marker) return;
		let boxGroup = new Konva.Group({
		    xclip: {
			x: imageX+scale(box.x),
			y: imageY+scale(box.y),
			width: scale(box.width),
			height:scale(box.height),
		    }});
		group.add(boxGroup);
		let x = imageX;
		let y = this.worldToCanvas(box.top);
		let width = scale(box.width);
		let height=this.worldToCanvas(box.bottom)-y;
		//		console.log(box.top,box.bottom);
		let boxImage = entry.image.clone({
		    crop:{
			x: box.x,
			y: box.y,
			width: box.width,
			height:box.height,
		    }
		});
		width = width*this.getImageWidthScale();
		let imageRect = new Konva.Rect({
		    stroke:CV_BOX_COLOR,
		    strokeWidth:1,
		    x: x,
		    y: y,
		    width: width,
		    height:height,
		});
		addBoxShape(imageRect);
		//		entry.boxShapes.push(imageRect);
		boxGroup.add(imageRect);


		const croppedImageDataURL = boxImage.toDataURL();
		let newImage = new Image();
		newImage.src = croppedImageDataURL;
		newImage.onload = () => {
		    let boxImage= new Konva.Image({
			image:newImage,
			x: x,
			y: y,
			width: width,
			height:height,
		    });
		    if(doRotation) {
			boxImage.rotate(90);
			boxImage.width(height);
		    } else {
			boxImage.width(width);
		    }
		    let bir = boxImage.getClientRect({relativeTo: this.layer});
		    let dx = imageX-bir.x;
		    let dy = bir.y-canvasY1;
		    boxImage.x(boxImage.x()+dx);
		    bir = boxImage.getClientRect({relativeTo: this.layer});
		    this.makeTicks(entry,boxGroup,box.top,box.bottom,x,y,y+height);
		    boxGroup.add(boxImage);
		    boxImage.moveToBottom();
		    this.toggleLabels();
		};

		/*
		  console.log('a',boxImage.getClientRect());

		  if(doRtoation) {
		  boxImage.rotate(90);
		  }
		  let bir = boxImage.getClientRect({relativeTo: this.layer});
		  let dx = imageX-bir.x;
		  let dy = bir.y-canvasY1;
		  boxImage.x(boxImage.x()+dx);
		  bir = boxImage.getClientRect({relativeTo: this.layer});
		  console.dir(bir)
		  this.makeTicks(entry,boxGroup,box.top,box.bottom,x,y,y+height);
		  boxGroup.add(boxImage);
		  group.add(boxGroup);
		  let br = new Konva.Rect({
		  x:bir.x,
		  y:bir.y,
		  //		    rotation:boxImage.attrs.rotation,
		  width: bir.width,
		  height:bir.height,
		  stroke: "red",
		  strokeWidth: 1,
		  });
		  boxGroup.add(br);
		  if(Utils.stringDefined(box.label)) {
		  this.definePopup(br,entry.label + ' - '+box.label,entry.text);
		  }
		*/
	    });
	}

	let getPos = (obj)=>{
	    let y1 = obj.getAbsolutePosition().y;
	    let scale = this.stage.scaleY();
	    let y2 = y1+scale*obj.getHeight();
	    
	    let top = this.canvasToWorld(y1);
	    let bottom = this.canvasToWorld(y2);		
	    return {top:top,bottom:bottom};
	}
	group.on('dragmove', (e)=> {
	    let pos  = getPos(e.target);
	    group.ticks.l1.text(this.formatDepth(pos.top));
	    group.ticks.l2.text(this.formatDepth(pos.bottom));	    
	});

	group.on('dragend', (e)=> {
	    let pos  = getPos(e.target);
	    console.log(pos);
	    this.editEntry(entry,pos.top,pos.bottom);
	});
    }
}



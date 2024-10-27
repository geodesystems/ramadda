

var ID_CV_SHOWLABELS = 'showlabels';
var ID_CV_DOROTATION = 'dorotation';
var ID_CV_COLUMN_WIDTH = 'columnwidth';
var ID_CV_SCALE = 'scale';
var ID_CV_SHOWHIGHLIGHT = 'showhighlight';
var ID_CV_SHOWPIECES='showpieces';
var ID_CV_GOTO = 'goto';
var ID_CV_RELOAD = 'reload';
var ID_CV_COLLECTIONS= 'collections';
var CV_LINE_COLOR='#aaa';
var CV_HIGHLIGHT_COLOR = 'red';

var CV_AXIS_WIDTH=100;
var CV_ANNOTATIONS_WIDTH=200;
var CV_LEGEND_WIDTH=150;

var CV_OFFSET_X=170;
var CV_FONT_SIZE = 15;
var CV_FONT_SIZE_SMALL = 10;
var CV_TICK_WIDTH = 8;


function RamaddaCoreVisualizer(collection,container,args) {
    if(!window['cv-base-id']) window['cv-base-id']=1;
    this.id = window['cv-base-id']++;
    let _this = this;
    this.entries = [];
    if(!args) args={};
    
    this.opts = {
	screenHeight:args.height??800,
	maxColumnWidth:300,
	offsetY:-20,
	//This is the scale applied to the world coordinates on the image
	scaleY:0.5,
	//This is the initial canvas scale
	initScale:1.0,
	legendWidth:0,
	hadLegendWidth:Utils.isDefined(args.legendWidth),

	showPieces:false,
	showAnnotations:true,	
	axisWidth:CV_AXIS_WIDTH,
	hadAxisWidth:Utils.isDefined(args.axisWidth),	
	top:0,

	range: {
	},


	showLegend:true,
	showLabels:true,
	showHighlight:false,
	showMenuBar:true,

    }

    Utils.split('showPieces,hasBoxes',',').forEach(prop=>{
	let getName =  'get' + prop.substring(0, 1).toUpperCase() + prop.substring(1);
	let setName =  'set' + prop.substring(0, 1).toUpperCase() + prop.substring(1);	
	let getFunc = (dflt)=>{
	    let value =  this.getProperty(prop,dflt);
	    return value;
	};
	let setFunc = (v)=>{
	    this.setProperty(prop,v);
	};
	this[getName] = getFunc;
	this[setName] = setFunc;	
    });
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
    this.opts.top =+this.opts.top;
    let menuItemsLeft = [];
    let menuItemsRight = [];    
    menuItemsLeft.push(HU.span([ATTR_ID,this.domId('add'),
				ATTR_TITLE,'Add collection','action','add',
				ATTR_CLASS,'ramadda-clickable'],
			       HU.getIconImage('fas fa-plus')));

    menuItemsLeft.push(HU.space(1));

    menuItemsLeft.push(HU.span([ATTR_TITLE,'Reset zoom','action','home',ATTR_ID,'home',ATTR_CLASS,'ramadda-clickable'],HU.getIconImage('fas fa-house')));


    menuItemsLeft.push(HU.span([ATTR_TITLE,'Zoom out','action','zoomout',
				ATTR_CLASS,'ramadda-clickable'],
			       HU.getIconImage('fas fa-magnifying-glass-minus')));

    menuItemsLeft.push(HU.span([ATTR_ID,this.domId('zoomin'),ATTR_TITLE,'Zoom in','action','zoomin',
			    ATTR_CLASS,'ramadda-clickable'],
			       HU.getIconImage('fas fa-magnifying-glass-plus')));
    menuItemsLeft.push(HU.span([ATTR_ID,this.domId('down'),ATTR_TITLE,'Pan down','action','down',
			    ATTR_CLASS,'ramadda-clickable'],
			       HU.getIconImage('fas fa-arrow-down')));
    menuItemsLeft.push(HU.span([ATTR_ID,this.domId('up'),ATTR_TITLE,'Pan up','action','up',
			    ATTR_CLASS,'ramadda-clickable'],
			       HU.getIconImage('fas fa-arrow-up')));

    menuItemsRight.push(HU.span([ATTR_ID,this.domId('gallery'),ATTR_TITLE,'Show Gallery','action','gallery',
			    ATTR_CLASS,'ramadda-clickable'],
				 HU.getIconImage('fas fa-images')));

  



    menuItemsLeft.push(HU.space(1));
    menuItemsLeft.push(HU.input('','',[ATTR_SIZE,'10',ATTR_ID,this.domId(ID_CV_GOTO),ATTR_PLACEHOLDER,"Go to depth"]));
    menuItemsLeft.push(HU.space(1));
    menuItemsLeft.push(HU.span([ATTR_ID,this.domId(ID_CV_COLLECTIONS),
				ATTR_STYLE,HU.css('margin-right','50px',
						  'vertical-align','top','display','inline-block','max-width','600px','overflow-x','auto')]));


    menuItemsRight.push(HU.span([ATTR_ID,this.domId('settings'),
				 ATTR_TITLE,'Settings','action','settings',
			    ATTR_CLASS,'ramadda-clickable'],
				 HU.getIconImage('fas fa-cog')));


	
    let menuBar=Utils.join(menuItemsLeft,HU.space(1));
    menuBar +=HU.div([ATTR_STYLE,HU.css('position','absolute','right','10px','top','4px')],
		     Utils.join(menuItemsRight,HU.space(2)));
    this.mainDiv =jqid(this.opts.mainId);
    if(!this.opts.showMenuBar)menuBar='';
    let top = HU.div([ATTR_CLASS,"cv-menubar"],menuBar);
    jqid(this.opts.topId).html(top);
    addHandler({
	selectClick:function(type,id,entryId,value) {
	    _this.loadCollection(entryId);
	}
    },this.domId('add'));


    jqid(this.opts.topId).find('.ramadda-clickable').click(function(event){
	let action = $(this).attr('action');
	if(action=='settings') {
	    _this.showSettings($(this));
	} else	if(action=='home') {
	    _this.resetZoomAndPan();
	} else if(action=='zoomin') {
	    _this.zoom(1.1);
	} else	if(action=='zoomout') {
	    _this.zoom(0.9);
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
	} else	if(action=='up') {
	    let pos = _this.stage.position();
	    _this.stage.position({x:pos.x,y:pos.y+50});
	}
	
    })

    HU.onReturn(this.jq(ID_CV_GOTO),obj=>{
	let depth = obj.val().trim();
	let y = _this.worldToCanvas(depth);
	this.stage.position({ x: 0, y: -y });
    });


    this.stage = new Konva.Stage({
	container: container,  
	width: container.offsetWidth,
	height: this.getScreenHeight(),
	draggable: true 
    });
    this.stage.offsetY(this.opts.offsetY);

    this.stage.scale({ x: this.opts.initScale, y: this.opts.initScale });
    this.annotationLayer = new Konva.Layer();
    this.stage.add(this.annotationLayer);
    this.layer = new Konva.Layer();
    this.legendLayer = new Konva.Layer();    
    this.stage.add(this.legendLayer);
    this.stage.add(this.layer);
    this.addEventListeners();
    this.collections = [];
    
    this.loadingMessage= this.makeText(this.annotationLayer,"Loading...",50,50, {fontSize:45});

//    this.addCollection(collection);
    if(this.opts.collectionIds) {
	Utils.split(this.opts.collectionIds,",",true,true).forEach(entryId=>{
	    this.loadCollection(entryId);
	});
    }


    this.drawLegend();
    this.layer.draw();
}


RamaddaCoreVisualizer.prototype = {
    getProperty:function(key,dflt) {
	let v = this.opts[key];
	if(v===null) return dflt;
	return v;
    },
    setProperty:function(key,v) {
	this.opts[key] = v;
    },    
    domId:function(id) {
	return 'id_'+this.id+'_'+ id;
    },
    jq:function(id) {
	return jqid(this.domId(id));
    },
    
    getXOffset:function(column) {
	return this.opts.axisWidth+100+column*(+this.opts.maxColumnWidth+100);
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
	let pos = this.stage.position().y;
	c=c+this.opts.offsetY;
	let scale = this.stage.scale().y;
	c = (c-pos)/scale;
	let range = this.opts.range;
	let r = range.max - range.min;
	let h=this.getCanvasHeight();
	let w =  (r*c)/h;
	w = w/this.opts.scaleY;
	return w;
    },


    showSettings:function(anchor) {
	let _this = this;
	let html = '';
	html+=HU.div([],
		     HU.checkbox(this.domId(ID_CV_SHOWLABELS),
				 [ATTR_ID,this.domId(ID_CV_SHOWLABELS)],this.opts.showLabels,'Show Labels'));

	html+=HU.div([],
		     HU.checkbox(this.domId(ID_CV_SHOWHIGHLIGHT),
				 [ATTR_ID,this.domId(ID_CV_SHOWHIGHLIGHT)],this.opts.showHighlight,'Show Highlight'));    
	if(this.getHasBoxes()) {
	    html+=HU.div([],
		     HU.checkbox(this.domId(ID_CV_SHOWPIECES),
				 [ATTR_ID,this.domId(ID_CV_SHOWPIECES)],
				 this.getShowPieces(),'Show Pieces'));

	}

	html+=HU.div([],
		     HU.checkbox(this.domId(ID_CV_DOROTATION),
				 [ATTR_ID,this.domId(ID_CV_DOROTATION)],this.opts.doRotation,'Do Rotation'));



	html+= HU.div([],
		      'Column width: ' + HU.input('',this.opts.maxColumnWidth,
						  [ATTR_ID,this.domId(ID_CV_COLUMN_WIDTH),ATTR_STYLE,'width:40px']));
	html+= HU.div([],
		      'Scale: ' + HU.input('',this.opts.scaleY,
					       [ATTR_ID,this.domId(ID_CV_SCALE),ATTR_STYLE,'width:40px']));	

	html=HU.div([ATTR_STYLE,HU.css('padding','5px')], html);
	let dialog =  HU.makeDialog({anchor:anchor,
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


	HU.onReturn(this.jq(ID_CV_SCALE),obj=>{
	    _this.opts.scaleY=+obj.val();
	    _this.drawCollections();
	});

	this.jq(ID_CV_SHOWLABELS).change(function(){
	    _this.opts.showLabels = $(this).is(':checked');
	    _this.toggleLabels();
	});
	this.jq(ID_CV_DOROTATION).change(function(){
	    _this.opts.doRotation = $(this).is(':checked');
	    _this.drawCollections(true);
	});	

	this.jq(ID_CV_SHOWPIECES).change(function(){
	    _this.setShowPieces($(this).is(':checked'));
	    _this.drawCollections(true);
	});

	this.jq(ID_CV_SHOWHIGHLIGHT).change(function(){
	    _this.opts.showHighlight = $(this).is(':checked');
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
		if(l.pending) {return;}
		l.pending=true;
		Konva.Image.fromURL(l.url,  (image) =>{
		    l.image = image;
		    setPosition(image);
		    this.annotationLayer.add(image);
		    collection.legendObjects.push(image);
		});
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
	    let x= this.opts.axisWidth-50;
	    let y = this.worldToCanvas(+depth);
	    let styleObj = {doOffsetWidth:true};
	    let styles = style.split(';');
	    for(let i=0;i<styles.length;i++) {
		let pair = styles[i].split(':');
		if(pair.length!=2) continue;
		styleObj[pair[0]] = pair[1];
	    }
	    if(styleObj.fontColor && !styleObj.fill) styleObj.fill = styleObj.fontColor;
	    let l = this.makeText(this.annotationLayer,label,x,y-5, styleObj);
	    if(Utils.stringDefined(desc)) {
		this.addClickHandler(l,(e,obj)=>{
		    let y = l.getAbsolutePosition().y;
		    let world = this.canvasToWorld(y);
		    desc  =desc.replace(/\n/g,'<br>');
		    this.showPopup(label,desc,l,true);
		});
	    }
	    let tick = new Konva.Line({
		points: [x+3, y, this.opts.axisWidth, y],
		stroke: CV_LINE_COLOR,
		strokeWidth: 0.5,
	    });
	    this.annotationLayer.add(tick);
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
		label+=' ' +entry.topDepth +' - ' +entry.bottomDepth;
		label = HU.href(RamaddaUtil.getEntryUrl(entry.entryId),label,
				['target','_entry']);

		html+=HU.b(label);
		html+='<br>';
		html+=HU.image(entry.url,[ATTR_WIDTH,'400px']);
		html+='<br>';
	    });
	    html = HU.div([ATTR_STYLE,HU.css('padding','10px','max-height','600px','overflow-y','auto')],html);
	    contents.push({label:c.name,contents:html});
	});
	if(this.collections.length==0)
	    contents.push({label:'',contents:'No collections are available'});
	let gallery;
	let tabs;
	if(contents.length>1) {
	    tabs = HU.makeTabs(contents);
	    gallery = tabs.contents;
	} else {
	    gallery=HU.b(contents[0].label) +'<br>' + contents[0].contents;
	}
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
	    _this.addCollection(data);
	}).fail((data)=>{
	    window.alert('Failed to load entries');
	});
    },

    drawCollections:function(forceNewImages) {
	if(this.loadingMessage) {
	    this.loadingMessage.destroy();
	    this.loadingMessage=null;
	}
	let _this=this;
	this.clear();
	this.checkRange();
	this.stage.scale({ x: 1, y: 1 });
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
		this.addEntries(collection,forceNewImages);
	    }
	});
	this.jq(ID_CV_COLLECTIONS).html(html);
	this.jq(ID_CV_COLLECTIONS).find('.ramadda-button').button().click(function() {
	    let collection = _this.collections[+$(this).attr('collection-index')];
	    let html = '';
	    html+=HU.div([ATTR_CLASS,'ramadda-clickable',
			  'action','view'],'View Entry');
	    html+=HU.div([ATTR_CLASS,'ramadda-clickable',
			  'action','goto'],'Scroll To');	    

	    html+=HU.div([ATTR_CLASS,'ramadda-clickable',
			  'action','toggle'],collection.visible?'Hide':'Show');

	    html+=HU.div([ATTR_CLASS,'ramadda-clickable',
			  'action','delete'],'Delete');
	    html=HU.div([ATTR_STYLE,HU.css('min-width','200px','padding','5px')], html);
	    let dialog =  HU.makeDialog({anchor:$(this),
				     decorate:true,
				     at:'left bottom',
				     my:'left top',
				     content:html,
				     draggable:false});

	    dialog.find('.ramadda-clickable').click(function() {
		let action = $(this).attr('action');
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
		} else if(action=='view') {
		    let url = RamaddaUtils.getEntryUrl(collection.entryId);
		    window.open(url, '_entry');
		}
		dialog.remove();
	    });
	});
	this.drawLegend();
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
    },
    addEntries:function(collection,forceNewImages) {
	let column = collection.displayIndex;
	collection.xPosition =  this.getXOffset(column);

	let x = this.getXOffset(column);
	let l = this.makeText(this.layer,collection.name,x,this.worldToCanvas(collection.range.min)-20,
			      {fontStyle:'bold'});
	l.collectionId = collection.collectionId;
	this.addClickHandler(l,(e)=>{
	    if(!confirm('Do you want to remove this collection?')) return;
	    this.removeCollection(collection);
	});
	this.checkRange();
	let range =  this.opts.range;
//	console.log(collection.name,'range',range.min,range.max);
	collection.data.forEach(entry=>{
	    entry.displayIndex = column;
	    this.addEntry(entry,forceNewImages);
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


    toggleLabels: function() {
	let show = this.getShowLabels();
	this.entries.forEach(e=>{
	    this.toggleAll(e.labels,show);
	});
    },
    getShowLabels: function() {
	return this.opts.showLabels;
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
    getShowHighlight: function() {
	return this.opts.showHighlight;
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
    },
    addEventListeners:function() {
	window.addEventListener('resize', () => {
	    const containerWidth = container.offsetWidth;
	    this.stage.width(containerWidth);
	    this.stage.batchDraw();
	});

	this.stage.on('dblclick',  (e)=> {
	    this.zoom(1.2);
	});
	this.stage.on('wheel', (e) => {
	    e.evt.preventDefault(); 
	    let scale = this.stage.scaleX();
	    const pointer = this.stage.getPointerPosition();
	    const zoomSpeed = 0.1;
	    const direction = e.evt.deltaY > 0 ? -1 : 1;
	    scale += direction * zoomSpeed;
	    scale = Math.max(0.005, Math.min(100, scale));
	    const newPos = {
		x: pointer.x - (pointer.x - this.stage.x()) * (scale / this.stage.scaleX()),
		y: pointer.y - (pointer.y - this.stage.y()) * (scale / this.stage.scaleY())
	    };
	    this.stage.scale({ x: scale, y: scale });
	    this.stage.position(newPos);
	    this.stage.batchDraw();
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
    },
    makeText:function(layer,t,x,y,args) {
	let opts = {
	    doOffsetWidth:false,
	    fill:'#000',
	    fontSize:CV_FONT_SIZE,
	    background:null,
	    fontStyle:'',
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

	let textX  = text.x();
	if(opts.doOffsetWidth) {
	    text.offsetX(text.width());
	    textX-=text.width();
	}
	if(opts.background || opts.outline)  {
	    let pad = 2;
	    let bg = new Konva.Rect({
		x: textX-pad,
		y: text.y()-pad,
		width: text.width()+pad*2,
		height: text.height()+pad*2,
		fill: opts.background,
		stroke:opts.outline,
		strokeWidth: 1,
		cornerRadius: 0 
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
	let haveAnnotation = false;	
	this.collections.forEach(collection=>{
	    if(this.opts.showLegend && collection.legends && collection.legends.length>0) {
		haveLegend = true;
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
			if(Utils.isNumber(box.top) && Utils.isNumber(box.bottom)) {
			    entry.hasBoxes=true;
			    this.setHasBoxes(true);
			    check(box.top,box.bottom);
			}
		    });
		}

	    });
	    collection.range = {min:cmin,max:cmax};
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
	    this.opts.legendWidth=CV_LEGEND_WIDTH;
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
	let axisWidth = this.opts.axisWidth;
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
	let bottom = range.max+(0.1*(range.max-range.min));
	for(let i=0;i<=bottom;i+=step) {
	    let y = this.worldToCanvas(i);
	    if(i==0) y1=y;
	    y2=y;
	    let l1 = this.makeText(this.legendLayer,Utils.formatNumber(i),
				   axisWidth-tickWidth,y,{doOffsetWidth:true});
	    let tick1 = new Konva.Line({
		points: [axisWidth-tickWidth, y, axisWidth, y],
		stroke: CV_LINE_COLOR,
		strokeWidth: 1,
	    });
	    this.legendLayer.add(tick1);
	    let tick2 = new Konva.Line({
		points: [axisWidth, y, axisWidth+3000, y],
		stroke: CV_LINE_COLOR,
		strokeWidth: 0.4,
		dash: [5, 5],
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

    addClickHandler:function(obj,f) {
	obj.on('click', (e) =>{
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
	y1 = Utils.formatNumber(y1);
	y2 = Utils.formatNumber(y2);	
	html+=HU.formTable();
	html+=HU.formEntry('Name:',HU.input('',entry.label,  [ATTR_ID,this.domId('editname')]));
	html+=HU.formEntry('Top:',HU.input('',y1,  [ATTR_ID,this.domId('edittop')]));
	html+=HU.formEntry('Bottom:',HU.input('',y2, [ATTR_ID,this.domId('editbottom')]));		
	html+=HU.formTableClose();
	let buttonList =[
	    HU.div(['action','cancel','class','ramadda-button ' + CLASS_CLICKABLE],"Cancel"),
	    HU.div(['action','apply','class','ramadda-button ' + CLASS_CLICKABLE],
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
	    let apply = $(this).attr('action')=='apply';
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
	    _this.addEntry(entry,false);
	    _this.layer.draw();
	    dialog.remove();
	});
    },

    removeEntry:function(entry) {
	if(!entry.group) return;
	entry.group.remove();
	this.layer.draw();
    },

    addEntry:function(entry,forceNewImages) {
	if(!forceNewImages &&entry.image) {
	    this.addEntryImage(entry);
	} else {
 	    Konva.Image.fromURL(entry.url,  (image) =>{
		this.entries.push(entry);
		entry.image = image;
		this.addEntryImage(entry);
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

    makeTicks:function(group,top,bottom,x,y1,y2) {
	let tickWidth=CV_TICK_WIDTH;
	let l1 = this.makeText(group,Utils.formatNumber(top),
			       x-tickWidth,y1,{doOffsetWidth:true,fontSize:CV_FONT_SIZE_SMALL});
	let tick1 = new Konva.Line({
	    points: [x-tickWidth, y1, x, y1],
	    stroke: CV_LINE_COLOR,
	    strokeWidth: 1,
	});
	group.add(tick1);
	let l2 = this.makeText(group,Utils.formatNumber(bottom),
			       x-tickWidth,y2, {doOffsetWidth:true,fontSize:CV_FONT_SIZE_SMALL});
	let tick2 = new Konva.Line({
	    points: [x-tickWidth, y2, x, y2],
	    stroke: CV_LINE_COLOR,
	    strokeWidth: 1,
	});
	group.add(tick2);
	return {l1:l1,l2:l2,tick1:tick1,tick2:tick2};
    },
    addEntryImage:function(entry,debug) {
	
	if(isNaN(entry.topDepth)|| isNaN(entry.bottomDepth)) return;
	entry.labels = [];

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

	let imageAttrs = {
	    x: imageX,
	    y: imageY,
	    width:image.width(),
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
	let scale =v=>{
	    return (+v)*imageScale;
	}
	let rotOffset = {
	    x:imageRect.x-imageX,
	    y:imageRect.y-imageY,
	}
	if(!showPieces || !entry.hasBoxes) {
	    group.add(image);
	    group.ticks = this.makeTicks(group,entry.topDepth,entry.bottomDepth,imageX,imageY,imageY+newHeight);
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
		    if(showPieces && this.isValidBox(box)) {
			return;
		    }
		    let boxAttrs = {
			x: imageX+scale(box.x),
			y: imageY+scale(box.y),
			width: scale(box.width),
			height:scale(box.height),
			stroke: 'red',
			strokeWidth: 1,
		    }
		    if(doRotation) {
			boxAttrs =  this.rotateAroundPoint(boxAttrs, 90,origImageCenter);
			boxAttrs.x=boxAttrs.x - rotOffset.x;
			boxAttrs.y=boxAttrs.y - rotOffset.y;
		    }
		    let boxRect = new Konva.Rect(boxAttrs);
		    group.add(boxRect);
		});
	    }

	    this.definePopup(rect,entry.label,entry.text);
	    let imageLabel = this.makeText(group,entry.label,imageX+4,imageY+4,
					   {outline:'#000',fontSize:CV_FONT_SIZE_SMALL,background:'#fff'});
	    this.toggle(imageLabel,this.getShowLabels());
	    entry.image = image;
	    entry.highlight = rect;
	    entry.labels.push(imageLabel);
	}
	

	if(showPieces && entry.boxes) {
	    entry.boxes.forEach(box=>{
		if(!this.isValidBox(box)) return;
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
		let width = scale(box.width)*8;
		let height=this.worldToCanvas(box.bottom)-y;
		let boxImage = entry.image.clone({
/*		    x: x,
		    y: y,
		    width: width,
		    height:height,
		    */
		    crop:{
			x: box.x,
			y: box.y,
			width: box.width,
			height:box.height,
		    }
		});
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
		    this.makeTicks(boxGroup,box.top,box.bottom,x,y,y+height);
		    boxGroup.add(boxImage);
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
		this.makeTicks(boxGroup,box.top,box.bottom,x,y,y+height);
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
	    console.log(y1,obj.getHeight());
	    let scale = this.stage.scaleY();
	    let y2 = y1+scale*obj.getHeight();
	
	    let top = this.canvasToWorld(y1);
	    let bottom = this.canvasToWorld(y2);		
	    return {top:top,bottom:bottom};
	}
	group.on('dragmove', (e)=> {
	    let pos  = getPos(e.target);
//	    console.log(pos.top,pos.bottom);
	    group.ticks.l1.text(Utils.formatNumber(pos.top));
	    group.ticks.l2.text(Utils.formatNumber(pos.bottom));	    
	});

	group.on('dragend', (e)=> {
	    let pos  = getPos(e.target);
	    console.log(pos);
	    this.editEntry(entry,pos.top,pos.bottom);
	});
    }
}



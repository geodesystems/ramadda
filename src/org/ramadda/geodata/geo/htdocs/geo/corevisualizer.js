
var ID_CV_SHOWLABELS = 'showlabels';
var ID_CV_SHOWHIGHLIGHT = 'showhighlight';
var ID_CV_GOTO = 'goto';
var CV_LINE_COLOR='#aaa';
var CV_HIGHLIGHT_COLOR = 'red';
var CV_LEGEND_X=80;
var CV_OFFSET_X=170;
var CV_COLUMN_WIDTH=300;
var CV_FONT_SIZE = 15;
var CV_TICK_WIDTH = 8;


function RamaddaCoreVisualizer(collection,container,args) {
    if(!window['cv-base-id']) window['cv-base-id']=1;
    this.id = window['cv-base-id']++;
    let _this = this;
    this.entries = [];
    if(!args) args={};
    
    this.opts = {
	screenHeight:args.height??1000,
	canvasHeight:args.height??1000,
	scale:1.0,
	offset:30,
	top:0,
	bottom:1000,
	scaleY:100,
	showLabels:true,
	showHighlight:false,
	showMenuBar:true,
	initScale:1.0
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

    this.opts.top =+this.opts.top;
    this.opts.top =+this.opts.top;    

    let menuItemsLeft = [];
    let menuItemsCenter = [];    
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

    menuItemsCenter.push(HU.span([ATTR_ID,this.domId('gallery'),ATTR_TITLE,'Show Gallery','action','gallery',
			    ATTR_CLASS,'ramadda-clickable'],
				 HU.getIconImage('fas fa-images')));

  



    menuItemsLeft.push(HU.space(1));
    menuItemsLeft.push(HU.input('','',[ATTR_SIZE,'15',ATTR_ID,this.domId(ID_CV_GOTO),ATTR_PLACEHOLDER,"Go to depth"]));



    menuItemsRight.push(HU.checkbox(this.domId(ID_CV_SHOWLABELS),
			       [ATTR_ID,this.domId(ID_CV_SHOWLABELS)],this.opts.showLabels,'Show Labels'));
    menuItemsRight.push(HU.checkbox(this.domId(ID_CV_SHOWHIGHLIGHT),
			       [ATTR_ID,this.domId(ID_CV_SHOWHIGHLIGHT)],this.opts.showHighlight,'Show Highlight'));    
	
    let menuBar=HU.leftCenterRight(
	Utils.join(menuItemsLeft,HU.space(1)),
	Utils.join(menuItemsCenter,HU.space(1)),
	Utils.join(menuItemsRight,HU.space(2)));	
    this.mainDiv =jqid(this.opts.mainId);
    if(!this.opts.showMenuBar)menuBar='';
    let top = HU.div([ATTR_CLASS,"cv-menubar"],menuBar);
    jqid(this.opts.topId).html(top);
    addHandler({
	selectClick:function(type,id,entryId,value) {
	    _this.loadEntries(entryId);
	}
    },this.domId('add'));


    jqid(this.opts.topId).find('.ramadda-clickable').click(function(event){
	let action = $(this).attr('action');
	if(action=='home') {
	    _this.resetZoomAndPan();
	} else if(action=='zoomin') {
	    let scale = _this.stage.scaleX();
	    scale = 1.2*scale;
	    _this.stage.scale({ x: scale, y: scale });
	} else	if(action=='zoomout') {
	    let scale = _this.stage.scaleX();
	    scale = 0.8*scale;
	    _this.stage.scale({ x: scale, y: scale });
	} else	if(action=='add') {
	    let id = $(this).attr('id');
	    //selectInitialClick:function(event, selectorId, elementId, allEntries, selecttype, localeId, entryType,baseUrl,props) {
	    let localeId = _this.opts.mainEntry;
	    RamaddaUtils.selectInitialClick(event,id,id,true,null,localeId,'isgroup',null);	    
	} else	if(action=='gallery') {
	    _this.showGallery($(this));
	} else	if(action=='down') {
	    let pos = _this.stage.position();
	    _this.stage.position({x:pos.px,y:pos.y-50});

	} else	if(action=='up') {
	    let pos = _this.stage.position();
	    _this.stage.position({x:pos.px,y:pos.y+50});
	}
	
    })

    this.jq(ID_CV_GOTO).keydown(function(event) {
	if (event.key === "Enter" || event.keyCode === 13) {
	    let depth = $(this).val().trim();
	    let y = _this.worldToScreen(depth);
	    _this.stage.position({ x: 0, y: -y });

	}
    });
    this.jq(ID_CV_SHOWLABELS).change(()=>{
	this.toggleLabels();
    });

    this.jq(ID_CV_SHOWHIGHLIGHT).change(()=>{
	this.toggleHighlight();
    });
    


    this.stage = new Konva.Stage({
	container: container,  
	width: container.offsetWidth,
	height: this.getScreenHeight(),
	draggable: true 
    });

    this.stage.scale({ x: this.opts.initScale, y: this.opts.initScale });

    this.legendLayer = new Konva.Layer();
    this.stage.add(this.legendLayer);
    this.layer = new Konva.Layer();
    this.stage.add(this.layer);
    this.drawLegend();
    this.layer.draw();
    this.addEventListeners();
    this.collections = [];
    this.collectionIndex = 0;

    this.addCollection(collection);

    if(this.opts.otherEntries) {
	Utils.split(this.opts.otherEntries,",",true,true).forEach(entryId=>{
	    this.loadEntries(entryId);
	});
    }


}


RamaddaCoreVisualizer.prototype = {
    domId:function(id) {
	return 'id_'+this.id+'_'+ id;
    },
    jq:function(id) {
	return jqid(this.domId(id));
    },
    
    getXOffset:function(column) {
	return CV_OFFSET_X+column*CV_COLUMN_WIDTH;
    },
    worldToScreen:function(y) {
	let off = y-(+this.opts.top);

	let perc = off/(this.opts.bottom-this.opts.top);
	let sy =  (this.getCanvasHeight()*perc);
	return this.opts.offset+sy*this.getScale();
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
    getScale:function() {
	return this.opts.scale;
    },
    getScreenHeight:function() {
	return this.opts.screenHeight;
    },

    getCanvasHeight:function() {
	return this.opts.canvasHeight;
    },
    
    clear:function(data) {
	this.layer.removeChildren();
    },
    removeCollection:function(idx) {
	this.collections.splice(idx, 1);
	this.clear();
	this.collectionIndex=0;
	this.collections.forEach(collection=>{
	    this.addEntries(collection);
	});
    },

    loadEntries:function(entryId) {
	let _this = this;
	let url = RamaddaUtils.getUrl('/core/entries');
	url +='?entryid='+ entryId;
	$.getJSON(url, (data)=> {
	    _this.addCollection(data);
	}).fail((data)=>{
	    window.alert('Failed to load entries');
	});
    },

    addCollection:function(collection) {
	if(!Utils.isDefined(window.cv_collection_id))
	    window.cv_collection_id = 0;
	window.cv_collection_id++;
	collection.collectionId=window.cv_collection_id;
	this.collections.push(collection);
	this.addEntries(collection);
    },
    addEntries:function(collection) {
	this.collectionIndex++;
	let column = this.collectionIndex-1;
	let x = this.getXOffset(column);
	let l = this.makeText(collection.name,x,10,{fontStyle:'bold'});
	l.collectionId = collection.collectionId;
	this.layer.add(l);
	this.addClickHandler(l,()=>{
	    if(!confirm('Do you want to remove this collection?')) return;
	    let index = this.collections.findIndex(c=>{
		return collection.collectionId==c.collectionId;
	    });
	    this.removeCollection(index);

	});
	collection.data.forEach(entry=>{
	    this.addEntry(entry.label,entry.url,entry.topDepth,entry.bottomDepth,entry.text,column);
	});
    },

    toggleLabels: function() {
	let show = this.showLabels();
	this.entries.forEach(e=>{
	    if(!e.ilabel) return;
	    if(show) {
		e.ilabel.show();
		e.ilabel.backgroundRect.show();
	    } else {
		e.ilabel.hide();
		e.ilabel.backgroundRect.hide();
	    }		
	});
    },
    showLabels: function() {
	return this.jq(ID_CV_SHOWLABELS).is(':checked');
    },
    toggleHighlight: function() {
	let show = this.showHighlight();
	this.entries.forEach(e=>{
	    if(!e.highlight) return;
	    if(show) {
		e.highlight.stroke(CV_HIGHLIGHT_COLOR);
	    } else {
		e.highlight.stroke('transparent');
	    }		
	});
    },
    showHighlight: function() {
	return this.jq(ID_CV_SHOWHIGHLIGHT).is(':checked');
    },



    showPopup:function(label,text,obj) {
	let _this = this;
	if(!text) return;
	if(obj &&  obj.dialog) obj.dialog.remove();
	text = Utils.convertText(text);
	let div = HU.div([ATTR_STYLE,HU.css('margin','10px')],
			 text);

	let dialog =  HU.makeDialog({anchor:this.mainDiv,
				     decorate:true,
				     at:'right top',
				     my:'right top',
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
	//Not now
	if(false && !Utils.isAnonymous()) {
	    dialog.find('.ramadda-column-value').each(function() {
		let id = $(this).attr('column-id');
		if(id=='top_depth' || id=='bottom_depth') {
		    let v = $(this).html();
		    let input = HU.input('',v,[ATTR_ID,_this.domId('input'+id)]);
		    $(input).appendTo($(this)).keypress(function(event) {
			if (event.keyCode === 13) {
			    console.log($(this).val());
			}});
		}
	    });
	}

	if(obj) obj.dialog=dialog;
    },
    addEventListeners:function() {
	window.addEventListener('resize', () => {
	    const containerWidth = container.offsetWidth;
	    this.stage.width(containerWidth);
	    this.stage.batchDraw();
	});

	this.stage.on('wheel', (e) => {
	    e.evt.preventDefault(); 
	    let scale = this.stage.scaleX();
	    const pointer = this.stage.getPointerPosition();
	    const zoomSpeed = 0.1;
	    const direction = e.evt.deltaY > 0 ? -1 : 1;
	    scale += direction * zoomSpeed;
	    scale = Math.max(0.05, Math.min(5, scale));
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
	this.stage.batchDraw();
    },
    makeText:function(t,x,y,args) {
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



	if(opts.doOffsetWidth) {
	    text.offsetX(text.width());
	}
	if(opts.background)  {
	    let pad = 2;
	    let bg = new Konva.Rect({
		x: text.x()-pad,
		y: text.y()-pad,
		width: text.width()+pad*2,
		height: text.height()+pad*2,
		fill: opts.background,
		cornerRadius: 0 
	    });
	    this.layer.add(bg);
	    text.backgroundRect = bg;
	}
	return text;
    },
    drawLegend:function() {
	let legendX = CV_LEGEND_X;
	let tickWidth = CV_TICK_WIDTH;
	let h = this.opts.bottom-this.opts.top;
	let step = h/100;
	let cnt = 10;
	while(cnt-->0) {
	    let y = this.worldToScreen(step+step)-this.worldToScreen(step);
	    if(y>40) break;
	    step+=10;
	}
	let y1 = 0;
	let y2 = 0;	
	for(let i=this.opts.top;i<=this.opts.bottom;i+=step) {
	    let y = this.worldToScreen(i);
	    if(cnt==0) y1=y;
	    y2=y;
	    let l1 = this.makeText(i,legendX-tickWidth,y,{doOffsetWidth:true});
	    this.legendLayer.add(l1);
	    let tick1 = new Konva.Line({
		points: [legendX-tickWidth, y, legendX, y],
		stroke: CV_LINE_COLOR,
		strokeWidth: 1,
	    });
	    this.legendLayer.add(tick1);
	    let tick2 = new Konva.Line({
		points: [legendX, y, legendX+3000, y],
		stroke: CV_LINE_COLOR,
		strokeWidth: 0.4,
		dash: [5, 5],
	    });
	    this.legendLayer.add(tick2);


	}

	let line = new Konva.Line({
	    points: [legendX, y1, legendX, y2],
	    stroke: CV_LINE_COLOR,
	    strokeWidth: 1,
	});
	this.legendLayer.add(line);


    },

    addClickHandler:function(obj,f) {
	obj.on('click', () =>{
	    f();
	});
	
	obj.on('mouseover', function () {
	    document.body.style.cursor = 'pointer'; 
	});
	obj.on('mouseout', function () {
	    document.body.style.cursor = 'default'; 
	});
    },


    addEntry:function(label,url,y1,y2,text,column) {
	if(!Utils.isDefined(column)) column = 0;
	let scy1 = this.worldToScreen(y1);
	let scy2 = this.worldToScreen(y2);


	Konva.Image.fromURL(url,  (image) =>{
	    let imageX = this.getXOffset(column);
	    let imageY = scy1;
	    let scaleX = 0.5;
	    let aspectRatio = image.width()/ image.height()
	    let newHeight= (scy2-scy1)
	    let newWidth = newHeight * aspectRatio;


	    image.setAttrs({
		x: imageX,
		y: imageY,
		width:newWidth,
		height:newHeight,
            });
	    let tickWidth=CV_TICK_WIDTH;
	    this.layer.add(image);
	    let l1 = this.makeText(y1,imageX-tickWidth,imageY,{doOffsetWidth:true,fontSize:10});
	    this.layer.add(l1);
	    let tick1 = new Konva.Line({
		points: [imageX-tickWidth, imageY, imageX, imageY],
		stroke: CV_LINE_COLOR,
		strokeWidth: 1,
	    });
	    this.layer.add(tick1);


	    let y = imageY+newHeight;
	    let l2 = this.makeText(y2,imageX-tickWidth,y,{doOffsetWidth:true,fontSize:10});
	    this.layer.add(l2);

	    let tick2 = new Konva.Line({
		points: [imageX-tickWidth, y, imageX, y],
		stroke: CV_LINE_COLOR,
		strokeWidth: 1,
	    });
	    this.layer.add(tick2);
	    
	    let rect = new Konva.Rect({
		x: imageX,
		y: imageY,
		width: newWidth,
		height:newHeight,
		fill: 'transparent',
		stroke: CV_HIGHLIGHT_COLOR,
		strokeWidth: 2,
	    });
	    this.layer.add(rect);
	    if(!this.showHighlight()) {
		rect.stroke('transparent');
	    }
		
	    this.addClickHandler(rect,()=>{this.showPopup(label,text,rect);});


	    let ilabel = this.makeText(label,imageX+10,imageY+10,{background:'#fff'});
	    this.layer.add(ilabel);
	    if(!this.showLabels()) {
		ilabel.hide();
	    }

	    this.entries.push({
		label:label,
		url:url,
		y1:y1,
		y2:y2,
		text:text,
		image:image,
		highlight:rect,
		ilabel:ilabel
	    });
	});
    }
}




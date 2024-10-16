
var ID_CV_SHOWLABELS = 'showlabels';
var ID_CV_SHOWHIGHLIGHT = 'showhighlight';
var CV_LINE_COLOR='#aaa';
var CV_HIGHLIGHT_COLOR = 'red';

function RamaddaCoreVisualizer(container,args) {
    if(!window['cv-base-id']) window['cv-base-id']=1;
    this.id = window['cv-base-id']++;
    let _this = this;
    this.entries = [];
    this.opts = {
	screenHeight:1000,
	top:0,
	bottom:1000,
	scaleY:100,
    }
    if(args) $.extend(this.opts,args);

    let menuItems = [];
    menuItems.push(HU.span([ATTR_TITLE,'Reset zoom','action','home',ATTR_ID,'home',ATTR_CLASS,'ramadda-clickable'],"<i class='fas fa-house'></i>"));
    menuItems.push(HU.checkbox(this.domId(ID_CV_SHOWLABELS),
			       [ATTR_ID,this.domId(ID_CV_SHOWLABELS)],true,'Show Labels'));
    menuItems.push(HU.checkbox(this.domId(ID_CV_SHOWHIGHLIGHT),
			       [ATTR_ID,this.domId(ID_CV_SHOWHIGHLIGHT)],false,'Show Highlight'));    
	
    let menuBar=Utils.join(menuItems,HU.space(3));
    this.mainDiv =jqid(this.opts.mainId);
    let top = HU.div([ATTR_CLASS,"cv-menubar"],menuBar);
    jqid(this.opts.topId).html(top);
    jqid(this.opts.topId).find('.ramadda-clickable').click(function(){
	let action = $(this).attr('action');
	if(action=='home') {
	    _this.resetZoomAndPan();
	}
    })

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

    this.layer = new Konva.Layer();
    this.stage.add(this.layer);
    this.drawLegend();
    this.layer.draw();
    this.addEventListeners();
}


RamaddaCoreVisualizer.prototype = {
    domId:function(id) {
	return this.id+'_'+ id;
    },
    jq:function(id) {
	return jqid(this.domId(id));
    },
    
    getScreenHeight:function() {
	return this.opts.screenHeight;
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


    worldToScreen:function(y) {
	let off = y-this.opts.top;
	let perc = off/(this.opts.bottom-this.opts.top);
	let sy =  (this.opts.screenHeight*perc);
	return sy;
    },
    showPopup:function(text,obj) {
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
	    fontSize:15,
	    background:null
	}
	if(args) $.extend(opts,args);
	let text=  new Konva.Text({
	    x: x,
	    y: y,
	    text: t,
	    fontSize: opts.fontSize,
	    fontFamily: 'helvetica',
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
	let legendX = 50;
	let tickWidth = 20;
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
	    this.layer.add(l1);
	    let tick1 = new Konva.Line({
		points: [legendX-tickWidth, y, legendX, y],
		stroke: CV_LINE_COLOR,
		strokeWidth: 1,
	    });
	    this.layer.add(tick1);
	}

	let line = new Konva.Line({
	    points: [legendX, y1, legendX, y2],
	    stroke: CV_LINE_COLOR,
	    strokeWidth: 1,
	});
	this.layer.add(line);


    },

    addEntry:function(label,url,y1,y2,text) {
	let scy1 = this.worldToScreen(y1);
	let scy2 = this.worldToScreen(y2);


	Konva.Image.fromURL(url,  (image) =>{
	    let imageX = 100;
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
	    let tickWidth=20;
	    this.layer.add(image);
	    let l1 = this.makeText(y1,imageX-tickWidth,imageY,{doOffsetWidth:true});
	    this.layer.add(l1);
	    let tick1 = new Konva.Line({
		points: [imageX-tickWidth, imageY, imageX, imageY],
		stroke: CV_LINE_COLOR,
		strokeWidth: 1,
	    });
	    this.layer.add(tick1);


	    let y = imageY+newHeight;
	    let l2 = this.makeText(y2,imageX-tickWidth,y,{doOffsetWidth:true});
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

	    rect.on('click', () =>{
		this.showPopup(text,rect);
	    });

	    rect.on('mouseover', function () {
		document.body.style.cursor = 'pointer'; 
	    });

	    rect.on('mouseout', function () {
		document.body.style.cursor = 'default'; 
	    });

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




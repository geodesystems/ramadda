

function RamaddaCoreVisualizer(container,args) {
    let _this = this;

    this.opts = {
	scaleY:100,
    }
    if(args) $.extend(this.opts,args);

    let menuBar='';
    menuBar+=HU.span([ATTR_TITLE,'Reset zoom','action','home',ATTR_ID,'home',ATTR_CLASS,'ramadda-clickable'],"<i class='fas fa-house'></i>");
    let top = HU.div([],menuBar);
    jqid(this.opts.topId).html(top);
    jqid(this.opts.topId).find('.ramadda-clickable').click(function(){
	let action = $(this).attr('action');
	if(action=='home') {
	    _this.resetZoomAndPan();
	}
    })

    this.stage = new Konva.Stage({
	container: container,  
	width: container.offsetWidth,
	height: 5000,
	draggable: true 
    });

    this.layer = new Konva.Layer();
    this.stage.add(this.layer);
    this.drawLegend();
    this.layer.draw();
    this.addEventListeners();
}


RamaddaCoreVisualizer.prototype = {
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
	    scale = Math.max(0.5, Math.min(5, scale));
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
    makeText:function(t,x,y) {
	return  new Konva.Text({
	    x: x,
	    y: y,
	    text: t,
	    fontSize: 15,
	    fontFamily: 'helvetica',
	    fill: '#000',
	});
    },
    drawLegend:function() {

    },

    addImage:function(url,y1,y2) {
	Konva.Image.fromURL(url,  (image) =>{
	    let imageX = 100;
	    let imageY = y1;
	    let scaleX = 0.5;
	    let aspectRatio = image.width()/ image.height()
	    let newHeight= (y2-y1)
	    let newWidth = newHeight * aspectRatio;

	    image.setAttrs({
		x: imageX,
		y: imageY,
		width:newWidth,
		height:newHeight,
            });
	    let tickWidth=20;
	    this.layer.add(image);
	    let l1 = this.makeText(y1,imageX-tickWidth,imageY);
	    l1.offsetX(l1.width());
	    this.layer.add(l1);
	    let tick1 = new Konva.Line({
		points: [imageX-tickWidth, imageY, imageX, imageY],
		stroke: '#000',
		strokeWidth: 1,
	    });
	    this.layer.add(tick1);


	    let y = imageY+newHeight;
	    let l2 = this.makeText(y2,imageX-tickWidth,y);
	    l2.offsetX(l2.width());
	    this.layer.add(l2);

	    let tick2 = new Konva.Line({
		points: [imageX-tickWidth, y, imageX, y],
		stroke: '#000',
		strokeWidth: 1,
	    });
	    this.layer.add(tick2);
	    
	    let rect1 = new Konva.Rect({
		x: imageX,
		y: imageY,
		width: newWidth,
		height:newHeight,
		fill: 'transparent',
		stroke: 'black',
		strokeWidth: 1,
	    });
	    this.layer.add(rect1);


	});
    }
}




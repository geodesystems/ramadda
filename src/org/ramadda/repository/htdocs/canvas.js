/**
   Copyright (c) 2008-2025 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

function RamaddaCanvas(id,canEdit) {
    this.divId = id;
    this.div = jqid(id);
    this.canEdit = canEdit;

    $(document).ready(()=>{
	setTimeout(()=>{
	    this.init();
	},1000);

    });
}


RamaddaCanvas.prototype = {
    init:function() {
	let id = this.id =  this.divId+"_canvas";
	let div = jqid(this.divId);
	let canvasHtml = HU.tag(TAG_CANVAS,[ATTR_CLASS,'ramadda-abs-canvas',
					    ATTR_ID,id,
					    ATTR_WIDTH,div.width(),
					    ATTR_HEIGHT,div.height(),
					    ATTR_TABINDEX,1,
					    ATTR_STYLE,HU.css(CSS_POSITION,'absolute',
							      CSS_BACKGROUND,'transparent',
							      CSS_LEFT,HU.px(0),
							      CSS_TOP,HU.px(0))]);
	div.prepend(canvasHtml);
	let canEdit = this.canEdit;
	let _this = this;
	this.ctx = document.getElementById(this.id).getContext("2d");
	let canvas =  this.canvas =  jqid(this.id);
	this.ctx.strokeStyle ="red";
	this.ctx.lineWidth=2;
	this.glyphs = [];
	for(let i=0;i<50;i++) {
	    let line = div.attr("line"+i);
	    if(!line) continue;
	    let glyph = this.makeGlyph();
	    line.split(";").forEach(p=>{
		let tuple = p.split(":");
		let attr =tuple[0];
		let value = tuple[1];
		if(attr=='points') value = value.split(",");
		glyph[attr]  = value;
	    });
	    this.glyphs.push(glyph);
	}
	this.redraw();
	if(!canEdit) return;
	this.initDrag();
	let visible = true;
	let getId=suffix=>{
	    return id+"_"+ suffix;
	}
	let  header =[];

	header.push(HU.span([ATTR_TITLE,'Print lines',
			     ATTR_ID,getId('_print'),
			     ATTR_CLASS,HU.classes(CLASS_HOVERABLE,CLASS_CLICKABLE)],HU.getIconImage('fas fa-print')));
	header.push(HU.span([ATTR_TITLE,'List lines',
			     ATTR_ID,getId('_listmenu'),
			     ATTR_CLASS,HU.classes(CLASS_HOVERABLE,CLASS_CLICKABLE)],HU.getIconImage('fas fa-list')));    
	header.push(HU.checkbox(getId('_toggle'),[ATTR_TITLE,'Toggle component visibility',ATTR_ID,getId('_toggle')],true,'Visible'));
	header.push(HU.span([ATTR_ID,getId('_polyline'),'active','false',
			     ATTR_TITLE,'Draw line',
			     ATTR_STYLE,HU.css(CSS_PADDING,HU.px(2)),
			     ATTR_CLASS,HU.classes(CLASS_HOVERABLE,CLASS_CLICKABLE)],HU.getIconImage('fas fa-draw-polygon')));
	header.push("Color: " + HU.input('','blue',[ATTR_ID,getId('_color'),
						    ATTR_STYLE,HU.css(CSS_WIDTH,HU.px(80))]));
	header.push("Width: " + HU.select('',[ATTR_ID,getId('_width'),
					      ATTR_STYLE,HU.css(CSS_WIDTH,HU.px(40))],
					  [1,2,3,4,5,6,7,8,9,10,15,20]));
	header.push(HU.checkbox(getId('_arrow'),[ATTR_TITLE,'Arrow endpoint',
						 ATTR_ID,getId('_arrow')],false,'Arrow'));
	let left = Utils.wrap(header,HU.open(TAG_SPAN,[ATTR_STYLE,HU.css(CSS_MARGIN_RIGHT,HU.px(8)]),HU.close(TAG_SPANE));
			      let right = HU.div([ATTR_STYLE,HU.css(CSS_MAX_WIDTH,HU.px(200),
								    CSS_OVERFLOW_X,OVERFLOW_HIDDEN,
								    CSS_WHITE_SPACE,WHITE_SPACE_NOWRAP),
						  ATTR_ID,getId('_message')]);
	jqid(this.divId+"_header").html(HU.leftRightTable(left,right));
	jqid(getId('_toggle')).change(function() {
	    _this.toggleVisibility(HU.isChecked($(this)));
	});
	jqid(getId('_listmenu')).click(function() {
	    let menu = "";
	    _this.glyphs.forEach((g,idx)=>{
		let line = HU.span([ATTR_TITLE,'Delete',
				    ATTR_STYLE,HU.css(CSS_MARGIN_RIGHT,HU.px(10)),
				    ATTR_CLASS,CLASS_CLICKABLE,
				    ATTR_ACTION,'delete','idx',idx],HU.getIconImage('fas fa-trash'));
		line += HU.span([ATTR_TITLE,'Apply style',
				 ATTR_STYLE,HU.css(CSS_MARGIN_RIGHT,HU.px(10)),
				 ATTR_CLASS,CLASS_CLICKABLE,'action','apply','idx',idx],HU.getIconImage('fas fa-gears'));
		line= HU.div([ATTR_STYLE,HU.css(CSS_PADDING,HU.px(4)),
			      ATTR_CLASS,CLASS_HOVERABLE,'idx',idx],line+g.color);
		menu +=line;
	    });
	    menu = HU.div([ATTR_STYLE,HU.css(CSS_PADDING,HU.px(4))],menu);
            _this.dialog = HU.makeDialog({content:menu,my:"left top",at:"left bottom",anchor:$(this)});
	    _this.dialog.find(HU.dotClass(CLASS_HOVERABLE)).mouseenter(function(){
		let glyph = _this.glyphs[+$(this).attr('idx')];
		glyph.highlight = true;
		_this.redraw();
	    });
	    _this.dialog.find(HU.dotClass(CLASS_HOVERABLE)).mouseleave(function(){
		let glyph = _this.glyphs[+$(this).attr('idx')];
		glyph.highlight = false;
		_this.redraw();
	    });
	    _this.dialog.find(HU.dotClass(CLASS_CLICKABLE)).click(function(){
		let action = $(this).attr('action');
		let glyph = _this.glyphs[+$(this).attr('idx')];
		glyph.highlight = false;
		if(action=='delete') {
		    Utils.removeItem(_this.glyphs,glyph);
		} else {
		    glyph.width = jqid(getId('_width')).val();
		    glyph.color = jqid(getId('_color')).val();
		    glyph.arrow = HU.isChecked(jqid(getId('_arrow')));
		}
		_this.redraw();
		_this.dialog.remove();
		_this.dialog=null;
	    });
	    
	});
	jqid(getId('_print')).click(()=>{
	    this.print();
	});
	jqid(getId('_polyline')).click(function(){
	    let active = !($(this).attr('active')+''== 'true');
	    $(this).attr('active',active);
	    _this.currentGlyph = null;
	    if(!active) {
		_this.addGlyph = false;
		jqid(getId('_polyline')).css(CSS_BACKGROUND,'transparent');
		return;
	    }
	    jqid(getId('_polyline')).css(CSS_BACKGROUND,'#ccc');
	    _this.addGlyph = true;
	});

	this.canvas.mousemove(()=>{
	    this.canvas[0].focus();
	});
	this.canvas[0].addEventListener("keydown", (e)=>{
	    if(e.which==27) {
		//esc
		if(this.currentGlyph) {
		    Utils.removeItem(this.glyphs, this.currentGlyph);
		    this.currentGlyph = null;
		    this.redraw();
		}
	    }

	});
	this.canvas[0].addEventListener("keypress", function(e){
	    let c = String.fromCharCode(e.which);
	    if(c=='c') {
		_this.currentGlyph = null;
	    } else if(c=='t') {
		let cbx = jqid(_this.getId('_toggle'));
		cbx.prop('checked',!HU.isChecked(cbx));
		_this.toggleVisibility(HU.isChecked(cbx));
	    } else if(c=='p') {
		_this.print();
	    }
	});

	this.canvas.mousemove(function(e){
	    if(!_this.currentGlyph) return;
	    let parentOffset = $(this).parent().offset(); 
	    let x = parseInt(e.pageX - parentOffset.left);
	    let y = parseInt(e.pageY - parentOffset.top);
	    let l = _this.currentGlyph.points.length;
	    _this.currentGlyph.points[l-2] = x;
	    _this.currentGlyph.points[l-1] = y;	

	    _this.redraw();
	});
	this.canvas.dblclick(function(e){
	    if(_this.currentGlyph) {
		let points = _this.currentGlyph.points;
		let newOnes = [];
		for(let i=0;i<points.length;i+=2) {
		    if(i>0) {
			if(points[i]==points[i-2] && points[i+1] == points[i-1]) continue;
		    }
		    newOnes.push(points[i],points[i+1]);
		}
		_this.currentGlyph.points = newOnes;
		_this.currentGlyph = null;
		_this.redraw();
	    }
	});

	this.canvas.click(function(e){
	    $(this).focus();
	    let parentOffset = $(this).parent().offset(); 
	    let x =parseInt(e.pageX - parentOffset.left);
	    let y = parseInt(e.pageY - parentOffset.top);
	    if(_this.addGlyph && !_this.currentGlyph) {
		_this.currentGlyph = _this.makeGlyph();
		_this.currentGlyph.color =jqid(getId('_color')).val()??'red';
		_this.currentGlyph.width=jqid(getId('_width')).val()??1;
		_this.currentGlyph.arrow = HU.isChecked(jqid(getId('_arrow')));
		_this.glyphs.push(_this.currentGlyph);
		_this.currentGlyph.points.push(x,y);
	    }

	    if(!_this.currentGlyph) {
		let line="";
		if(x>_this.canvas.width()/2) {
		    line += HU.css(CSS_RIGHT,HU.px((_this.canvas.width()-x)));
		} else {
		    line += HU.css(CSS_LEFT,HU.px(x));
		}
		if(y>_this.canvas.height()/2) {
		    line += HU.css(CSS_BOTTOM,HU.px((_this.canvas.height()-y)));
		} else {
		    line += HU.css(CSS_TOP,HU.px(y));
		}	    
		Utils.copyToClipboard(line);
		_this.message(line);
		return;
	    }
	    _this.currentGlyph.points.push(x,y);
	    _this.redraw();
	});
    },	

    initDrag:function() {
	let _this = this;
	let divs = this.div.find('.ramadda-abs');
	divs = this.div.children('div[style*="position:absolute"]');
	divs.draggable({
	    start: function( e, ui ) {
		$(this).css(CSS_RIGHT,'');
		$(this).css(CSS_LEFT,'');
		$(this).css(CSS_BOTTOM,'');
		$(this).css(CSS_TOP,'');
		$(this).css('-webkit-transform','');
		$(this).css(CSS_TRANSFORM,'');				
	    },
	    drag: function( e, ui ) {
		_this.message(_this.makePosition($(this)));
	    },
	    stop: function( e, ui ) {
		let line = _this.makePosition($(this));
		Utils.copyToClipboard(line);
		_this.message(line);
	    }
	});
    },	

    getId:function(suffix) {
	return this.id+"_"+ suffix;
    },
    message: function(msg) {
	jqid(this.getId('_message')).html(msg);
    },
    makePosition: function(div) {
	let cw = this.canvas.width();
	let ch = this.canvas.height();		
	let cmx = cw/2;
	let cmy = ch/2;		
	let w = div.width();
	let l = div.position().left;
	let r = l+w;
	let h = div.height();
	let t = div.position().top;		
	let b = t+h;
	let mx = parseInt(l+w/2);
	let my = parseInt(t+h/2);		
	let line = "";
	if(Math.abs(mx-cmx)<30) {
	    line += "absLeft=50% absTranslateX=-50% ";
	} else if(l<cw-r) {
	    line += "absLeft=" + HU.px(parseInt(l))+" ";
	} else {
	    line += "absRight=" +HU.px(parseInt(cw-r))+" ";
	}
	if(Math.abs(my-cmy)<30) {
	    line += "absTop=50% absTranslateY=50% ";
	} else if(t<ch-b) {
	    line += "absTop=" + HU.px(parseInt(t))+" ";
	} else {
	    line += "absBottom=" + HU.px(parseInt(ch-b))+" ";
	}
	return line
    },	
    makeGlyph:function() {
	return {
	    points:[],
	    type:'line',
	    color:'blue',
	    width:2
	};
    },

    print: function() {
	let result = "";
	this.glyphs.forEach((g,idx)=>{
	    if(g.points.length==0) return;
	    if(idx>0) result+="\n";
	    let s = "line" + (idx+1)+"=\"type:line;";
	    if(g.arrow) 
		s+="arrow:true;";
	    s+="points:"+ Utils.join(g.points,",");
	    s+=";color:" +g.color +";width:" + g.width;
	    s+="\" ";
	    result+=s;
	});
	console.log(result);
	Utils.copyToClipboard(result);
	this.message('Lines copied to clipboard');
    },
    redraw: function() {
	let ctx = this.ctx;
	ctx.clearRect(0, 0, this.canvas.width(), this.canvas.height());
	this.glyphs.forEach(g=>{
	    if(g.highlight) {
		ctx.strokeStyle ='#000';
		ctx.lineWidth=4;
	    } else {
		ctx.strokeStyle =g.color;
		ctx.lineWidth=g.width;
	    }
	    let p = g.points;
	    for(let i=0;i<p.length;i+=2) {
		let x = p[i];
		let y = p[i+1];
		if(i==0) {
		    ctx.beginPath();
		    ctx.moveTo(x,y);
		} else {
		    ctx.lineTo(x,y);
		    ctx.stroke();
		}
	    }
	    ctx.closePath();

	    if(g.arrow) {
		function arrow(ctx, fromx, fromy, tox, toy) {
		    var arrowLength = 10; 
		    var dx = tox - fromx;
		    var dy = toy - fromy;
		    var angle = Math.atan2(dy, dx);
		    ctx.moveTo(fromx, fromy);
		    ctx.lineTo(tox, toy);
		    ctx.lineTo(tox - arrowLength * Math.cos(angle - Math.PI / 6), toy - arrowLength * Math.sin(angle - Math.PI / 6));
		    ctx.moveTo(tox, toy);
		    ctx.lineTo(tox - arrowLength * Math.cos(angle + Math.PI / 6), toy - arrowLength * Math.sin(angle + Math.PI / 6));
		}
		ctx.beginPath();
		let x1 = p[p.length-4];
		let x2 = p[p.length-2];
		let y1 = p[p.length-3];
		let y2 = p[p.length-1];	    	    
		arrow(ctx,x1,y1,x2,y2);
		ctx.stroke();
		ctx.closePath();
	    }
	    ctx.beginPath();
	    ctx.fillStyle =g.color;
	    //	    ctx.arc(p[0],p[1], 2, 0, 2 * Math.PI);
	    ctx.fill();
	    ctx.arc(p[g.points.length-2],p[p.length-1], 2, 0, 2 * Math.PI);
	    ctx.fill();
	    ctx.closePath();
	});
    },	
    toggleVisibility:function(visible) {
	let comps = this.canvas.parent().children(TAG_DIV);
	comps.each(function() {
	    if(!$(this).hasClass('ramadda-bgimage')) {
		if(visible)
		    $(this).show();
		else
		    $(this).hide();
	    }
	});
    }
}

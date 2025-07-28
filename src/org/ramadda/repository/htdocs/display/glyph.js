/**
   Copyright (c) 2008-2025 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/



function Glyph(display, scale, fields, records, args, attrs) {
    var props = this.properties = this.p = {};
    $.extend(this,{
	display: display,
	records:records,
	type:"label",
	toString: function() {
	    return this.properties.type;
	}
    });


    $.extend(this.properties,{
	dx:0,
	dy:0,
	label:"",
	baseHeight:0,
	baseWidth:0,
	width:8,
	fill:true,
	stroke:true,
    });
    $.extend(this.properties,args??{});

    let cnt=0;
    attrs.split(",").forEach(attr=>{
	let toks = attr.split(":");
	let name = toks[0];
	let value="";
	if(!Utils.stringDefined(name) && !Utils.stringDefined(value)) return;
	if(cnt==0 && toks.length==1) {
	    value = name;
	    name='type';
	} else {
	    for(let i=1;i<toks.length;i++) {
		if(i>1) value+=":";
		value+=toks[i];
	    }
	}
	cnt++;
//	console.log('\t'+name+'='+ value);
	value = value.replace(/_nl_/g,"\n").replace(/_colon_/g,":").replace(/_comma_/g,",").replace(/\\n/g,'\n');
	//Check for the ${...} macros
//	if(name=='colorBy')  value = value.replace('\${','').replace('}','');

	if(value=="true") value=true;
	else if(value=="false") value=false;
	this.properties[name] = value;
    });

    if(props.glyphField|| props.defaultField ) {
	['requiredField','colorBy','label'].forEach(prop=>{
	    if(props[prop]) {
		props[prop] = props[prop].replace(/\${_field}/g,props.glyphField??props.defaultField);
	    }
	});
    }

    if(props.labelBy) {
	props.labelField=display.getFieldById(fields,props.labelBy);
	if(!props.labelField) {
	    console.log("Could not find label field: " + props.labelBy);
	}
    }



    if(props.type=="image") {
	props.imageField=display.getFieldById(fields,props.imageField);
	props.myImage= new Image();
    }
    props.scale = scale;
    if(props.height==null) {
	if(props.type == "3dbar")
	    props.height=20;
	else
	    props.height=8;
    }
    if(props.pos==null) {
	if(props.type == "3dbar")
	    props.color = "blue";
	else if(props.type == "rect") {
	    props.pos = "c";
	}
	else
	    props.pos = "nw";
    }	
    
    let cvrt= this.cvrt = s=>{
	if(!isNaN(+s)) return +s;
	s  = String(s);
	s = s.replace(/canvasWidth2/g,""+(props.canvasWidth/2)).replace(/canvasWidth/g,props.canvasWidth);
	s = s.replace(/cw2/g,""+(props.canvasWidth/2)).replace(/cw/g,props.canvasWidth);
	s = s.replace(/canvasHeight2/g,""+(props.canvasHeight/2)).replace(/canvasHeight/g,props.canvasHeight);
	s = s.replace(/ch2/g,""+(props.canvasHeight/2)).replace(/ch/g,props.canvasHeight);		
	s = s.replace(/width2/g,""+(props.width/2)).replace(/width/g,props.width);	
	s = s.replace(/height2/g,""+(props.height/2)).replace(/height/g,props.width);	
	try {
	    s = eval(s);
	} catch(err) {
	    console.error('error evaling glyph value:' + s,err);
	}
	return s;
    };
    props.width = cvrt(props.width);
    props.height = cvrt(props.height);

    props.dx = cvrt(props.dx);
    props.dy = cvrt(props.dy);    
    props.baseWidth = +props.baseWidth;
    props.width = (+props.width)*scale;
    props.height = (+props.height)*scale;
    props.dx = (+props.dx)*scale;
    props.dy = (+props.dy)*scale;
    if(props.sizeBy) {
	props.sizeByField=display.getFieldById(fields,props.sizeBy);
	if(!props.sizeByField) {
	    console.log("Could not find sizeBy field:" + props.sizeBy);
	} else  {
	    let colorProps = {
		Min:props.sizeByMin,
		Max:props.sizeByMax,
	    };
	    props.sizeByInfo =  new ColorByInfo(display, fields, records, props.sizeBy,props.sizeBy, null, props.sizeBy,props.sizeByField,colorProps);
	}
    }

    props.wasNaN = false;
    props.dontShow =false;
    if(!props.colorByInfo && props.colorBy) {
	props.colorByField=display.getFieldById(fields,props.colorBy);
	let ct = props.colorTable?display.getColorTableInner(true, props.colorTable):null;
	if(!props.colorByField) {
	    console.log("Could not find colorBy field:" + props.colorBy);
	    console.log("Fields:" + fields);
	    props.dontShow =true;
	} else {
	    let colorByProps = {
		Min:props.colorByMin,
		Max:props.colorByMax,
	    };	    
	    props.colorByInfo =  new ColorByInfo(display, fields, records, props.colorBy,props.colorBy+".colorByMap", ct, props.colorBy,props.colorByField, colorByProps);
	}
    }

    if(props.requiredField) {
	if(!(this.theRequiredField = display.getFieldById(fields,props.requiredField))) {
	    props.dontShow = true;
	}
    }
}



Glyph.prototype = {
    okToShow:function() {
	return !this.properties.dontShow;
    },
    hadMissingValue:function() {
	return this.properties.wasNaN;
    },    
    getColorByInfo:function() {
	return this.properties.colorByInfo;
    },
    isImage: function() {
	return this.properties.type=='image';
    },

    draw: function(opts, canvas, ctx, x,y,args,debug) {

	let props = this.properties;
	if(props.dontShow)return;
	debug = props.debug??debug;
	let color =   null;
	if(props.colorByInfo) {
	    if(props.colorByField) {
		let v = args.record.getValue(props.colorByField.getIndex());
		if(isNaN(v)) props.wasNaN = true;
		color=  props.colorByInfo.getColor(v);
	    } else if(args.colorValue) {
		color=  props.colorByInfo.getColor(args.colorValue);
		color = props.colorByInfo.convertColor(color, args.colorValue);
	    }
	}
	let lengthPercent = 1.0;
	if(props.sizeByInfo) {
	    let v = args.record.getValue(props.sizeByField.getIndex());
	    if(isNaN(v)) props.wasNaN = true;
	    lengthPercent = props.sizeByInfo.getValuePercent(v);
	}

	if(args.alphaByCount && args.cell && args.grid) {
	    if(args.grid.maxCount!=args.grid.minCount) {
		let countPerc = (args.cell.count-args.grid.minCount)/(args.grid.maxCount-args.grid.minCount);
		color = Utils.addAlphaToColor(c,countPerc);
	    }
	}
	ctx.fillStyle =color || props.fillStyle || props.color || 'transparent';
	ctx.strokeStyle =props.strokeStyle ?? props.color ?? opts.strokeStyle ?? '#000';
	ctx.lineWidth=props.lineWidth??props.strokeWidth??opts.lineWidth??1;
	if(props.type=='label') {
	    let label = props.labelField?args.record.getValue(props.labelField.getIndex()):props.label;
	    if(label===null) {
		console.log('No label value');
		return;
	    }
	    let text = String(label);
	    if(args.record) {
		text = this.display.applyRecordTemplate(args.record, null,null,text,{
		    records:args.records,
		    findNonNan:args.findNonNan,
		    entryname:props.entryname,
		    unit:props.unit
		});
		if(text.indexOf('NaN')>=0) {
		    props.wasNaN = true;
		}
	    }

	    if(!isNaN(parseFloat(text))) {
		if(props.valueScale) {
		    text = text* +props.valueScale;
		}
		if(Utils.isDefined(props.decimals))
		    text = number_format(text,+props.decimals);
	    }
	    if(props.template) {
		text = props.template.replace('${value}',text);
	    }

	    text = text.replace(/\${.*}/g,'');
	    if(props.prefix) text = props.prefix.replaceAll('_space_',' ')+text
	    if(props.suffix) text = text+props.suffix.replaceAll('_space_',' ');
	    text = text.replace(/_nl_/g,'\n').replace(/\\n/g,'\n').split('\n');

	    //Normalize the font
	    if(props.font && props.font.match(/\d+(px|pt)$/)) {
		props.font = props.font +' sans-serif';
	    }

	    ctx.font = props.font ??  this.display.getProperty('glyphFont','12pt sans-serif');
	    ctx.fillStyle = ctx.strokeStyle =    color || props.color|| this.display.getProperty('glyphColor','#000');

	    if(debug) console.log('glyph label: font=' + ctx.font +' fill:' + ctx.fillStyle +' stroke:' + ctx.strokeStyle);


	    let h = 0;
	    let hgap = 3;
	    let maxw = 0;
	    let pady = +(props.pady??2);
	    text.forEach((t,idx)=>{
		let dim = ctx.measureText(t);
		if(idx>0) h+=hgap;
		maxw=Math.max(maxw,dim.width);
		h +=dim.actualBoundingBoxAscent+dim.actualBoundingBoxDescent;
	    });
	    let pt = Utils.translatePoint(x, y, maxw,  h, props.pos,{dx:props.dx,dy:props.dy});
	    if(debug) console.log('position:',{point:pt,x:x,y:y,text_width:maxw,text_height:h,pos:props.pos,dx:props.dx,dy:props.dy});
	    let bg = props.bg;
	    text.forEach(t=>{
		let dim = ctx.measureText(t);
		if(bg) {
		    ctx.fillStyle = bg;
		    let pad = +(props.bgpad??6);
		    if(debug) console.log('drawing background:' + bg +' padding:' + pad);
		    let rh = dim.actualBoundingBoxAscent+dim.actualBoundingBoxDescent;
		    let rw = dim.width;
		    ctx.fillRect(pt.x-pad,pt.y-rh-pad,rw+2*pad,rh+2*pad);
		}
		ctx.fillStyle = ctx.strokeStyle =    color || props.color|| this.display.getProperty('glyphColor','#000');
		dim = ctx.measureText(t);
		let offset =dim.actualBoundingBoxAscent+dim.actualBoundingBoxDescent;
		if(debug) console.log('draw text:' + t +' x:' + pt.x +' y:'+ (pt.y+offset));
		ctx.fillText(t,pt.x,pt.y+offset);
		pt.y += pady+dim.actualBoundingBoxAscent + dim.actualBoundingBoxDescent + hgap;
	    });
	} else 	if(props.type == 'circle') {
	    ctx.beginPath();
	    let w = props.width*lengthPercent+ props.baseWidth;
	    let pt = Utils.translatePoint(x, y, w,  w, props.pos,{dx:props.dx,dy:props.dy});
	    let cx = pt.x+w/2;
	    let cy = pt.y+w/2;
	    if(debug) console.log('draw circle',{cx:cx,cy:cy,w:w});
	    ctx.arc(cx,cy, w/2, 0, 2 * Math.PI);
	    if(props.fill)  {
		ctx.fill();
	    }
	    if(props.stroke) 
		ctx.stroke();
	} else if(props.type=='rect') {
	    let pt = Utils.translatePoint(x, y, props.width,  props.height, props.pos,{dx:props.dx,dy:props.dy});
	    if(props.fill)  
		ctx.fillRect(pt.x,pt.y, props.width, props.height);
	    if(props.stroke) 
		ctx.strokeRect(pt.x,pt.y, props.width, props.height);
	} else if(this.isImage()) {
	    let src = props.url;
	    if(!src && props.imageField) {
		src =  args.record.getValue(props.imageField.getIndex());
	    }
	    if(src) {
		src= src.replace('\${root}',ramaddaBaseUrl);
		props.width = +(props.width??50);
		props.height = +(props.height??50);		
		let pt = Utils.translatePoint(x, y, props.width,  props.height, props.pos,{dx:props.dx,dy:props.dy});
		if(props.debug) console.log('image glyph:' + src,{pos:props.pos,pt:pt,x:x,y:y,dx:props.dx,dy:props.dy,width:props.width,height:props.height});
		let i = new Image();
		i.src = src;
		let drawImage = () =>{
		    let a = ctx.globalAlpha;
		    if(Utils.isDefined(this.properties.imageAlpha))		    
			ctx.globalAlpha = this.properties.imageAlpha;		    
		    ctx.drawImage(i,pt.x,pt.y,props.width,props.width);
		    ctx.globalAlpha = a;		    
		}
		if(!i.complete) {
		    let loaded = false;
		    i.onload=()=>{
			drawImage();
			loaded=true;
		    }
		    return () =>{
			return loaded;
		    }
		} else {
		    drawImage();

		}
	    } else {
		console.log('No url defined for glyph image');
	    }
	} else 	if(props.type == 'gauge') {
	    let pt = Utils.translatePoint(x, y, props.width,  props.height, props.pos,{dx:props.dx,dy:props.dy});
	    ctx.fillStyle =  props.fillColor || '#F7F7F7';
	    ctx.beginPath();
	    let cx= pt.x+props.width/2;
	    let cy = pt.y+props.height;
	    ctx.arc(cx,cy, props.width/2,  1 * Math.PI,0);
	    ctx.fill();
	    ctx.strokeStyle =   '#000';
	    ctx.stroke();
	    ctx.beginPath();
	    ctx.beginPath();
	    let length = props.width/2*0.75;
            let degrees = (180*lengthPercent);
	    let ex = cx-props.width*0.4;
	    let ey = cy;
	    let ep = Utils.rotate(cx,cy,ex,ey,degrees);
	    ctx.strokeStyle =  props.color || '#000';
	    ctx.lineWidth=props.lineWidth||2;
	    ctx.moveTo(cx,cy);
	    ctx.lineTo(ep.x,ep.y);
	    ctx.stroke();
	    ctx.lineWidth=1;
	    props.showLabel = true;
	    if(props.showLabel && props.sizeByInfo) {
		ctx.fillStyle='#000';
		let label = String(props.sizeByInfo.minValue);
		ctx.font = props.font || '9pt arial'
		let dim = ctx.measureText(label);
		ctx.fillText(label,cx-props.width/2-dim.width-2,cy);
		ctx.fillText(props.sizeByInfo.maxValue,cx+props.width/2+2,cy);
	    }
	} else if(props.type=='line') {
	    let x1= this.cvrt(props.x1);
	    let y1= this.cvrt(props.y1);
 	    let x2= this.cvrt(props.x2);
	    let y2= this.cvrt(props.y2);	    	    
	    ctx.strokeStyle = props.strokeStyle||'#000';
	    ctx.beginPath();
	    ctx.moveTo(x1,y1);
	    ctx.lineTo(x2,y2);
	    ctx.stroke();
	} else if(props.type=='3dbar') {
	    let pt = Utils.translatePoint(x, y, props.width,  props.height, props.pos,{dx:props.dx,dy:props.dy});
	    let height = lengthPercent*(props.height) + parseFloat(props.baseHeight);
	    ctx.fillStyle =   color || props.color;
	    ctx.strokeStyle = props.strokeStyle||'#000';
	    this.draw3DRect(canvas,ctx,pt.x, 
			    canvas.height-pt.y-props.height,
			    +props.width,height,+props.width);
	    
	} else if(props.type=='axis') {
	    let pt = Utils.translatePoint(x, y, props.width,  props.height, props.pos,{dx:props.dx,dy:props.dy});
	    let height = lengthPercent*(props.height) + parseFloat(props.baseHeight);
	    ctx.strokeStyle = props.strokeStyle||'#000';
	    ctx.beginPath();
	    ctx.moveTo(pt.x,pt.y);
	    ctx.lineTo(pt.x,pt.y+props.height);
	    ctx.lineTo(pt.x+props.width,pt.y+props.height);
	    ctx.stroke();
	} else if(props.type == 'vector') {
	    if(!props.sizeByInfo) {
		console.log('make Vector: no sizeByInfo');
		return;
	    }
	    ctx.strokeStyle =   color || props.color;
	    let v = args.record.getValue(props.sizeByField.getIndex());
	    lengthPercent = props.sizeByInfo.getValuePercent(v);
	    let length = opts.cellSizeH;
	    if(opts.lengthBy && opts.lengthBy.index>=0) {
		length = opts.lengthBy.scaleToValue(v);
	    }
	    let x2=x+length;
	    let y2=y;
	    let arrowLength = opts.display.getArrowLength();
	    /*
	      if(opts.angleBy && opts.angleBy.index>=0) {
	      let perc = opts.angleBy.getValuePercent(v);
	      let degrees = (360*perc);
	      let rads = degrees * (Math.PI/360);
	      x2 = length*Math.cos(rads)-0* Math.sin(rads);
	      y2 = 0*Math.cos(rads)-length* Math.sin(rads);
	      x2+=x;
	      y2+=y;
	      }
	    */
	    if(opts.colorBy && opts.colorBy.index>=0) {
                let perc = opts.colorBy.getValuePercent(v);
                let degrees = (180*perc)+90;
		degrees = degrees*(Math.PI / 360)
                x2 = length*Math.cos(degrees)-0* Math.sin(degrees);
		y2 = 0*Math.cos(degrees)-length* Math.sin(degrees);
                x2+=x;
                y2+=y;
            }
	    //Draw the circle if no arrow
	    if(arrowLength<=0) {
		ctx.save();
		ctx.fillStyle='#000';
		ctx.beginPath();
		ctx.arc(x,y, 1, 0, 2 * Math.PI);
		ctx.fill();
		ctx.restore();
	    }
	    ctx.beginPath();
	    ctx.moveTo(x,y);
	    ctx.lineTo(x2,y2);
	    ctx.lineWidth=opts.display.getLineWidth();
	    ctx.stroke();
	    if(arrowLength>0) {
		ctx.beginPath();
		this.drawArrow(ctx, x,y,x2,y2,arrowLength);
		ctx.stroke();
	    }
	} else if(props.type=='tile'){
	    let crx = x+opts.cellSizeX/2;
	    let cry = y+opts.cellSizeY/2;
 	    if((args.row%2)==0)  {
		crx = crx+opts.cellSizeX/2;
		cry = cry-opts.cellSizeY/2;
	    }
	    let sizex = opts.cellSizeX/2;
	    let sizey = opts.cellSizeY/2;
	    ctx.beginPath();
	    let quarter = Math.PI/2;
	    ctx.moveTo(crx + sizex * Math.cos(quarter), cry + sizey * Math.sin(quarter));
	    for (let side=0; side < 7; side++) {
		ctx.lineTo(crx + sizex * Math.cos(quarter+side * 2 * Math.PI / 6), cry + sizey * Math.sin(quarter+side * 2 * Math.PI / 6));
	    }
	    ctx.strokeStyle = '#000';
	    //	    ctx.fill();
	    ctx.stroke();
	} else {
	    console.log('Unknown cell shape:' + props.type);
	}
    },
    draw3DRect:function(canvas,ctx,x,y,width, height, depth) {
	// Dimetric projection functions
	let dimetricTx = function(x,y,z) { return x + z/2; };
	let dimetricTy = function(x,y,z) { return y + z/4; };
	
	// Isometric projection functions
	let isometricTx = function(x,y,z) { return (x -z) * Math.cos(Math.PI/6); };
	let isometricTy = function(x,y,z) { return y + (x+z) * Math.sin(Math.PI/6); };
	
	let drawPoly = (function(ctx,tx,ty) {
	    return function() {
		let args = Array.prototype.slice.call(arguments, 0);
		// Begin the path
		ctx.beginPath();
		// Move to the first point
		let p = args.pop();
		if(p) {
		    ctx.moveTo(tx.apply(undefined, p), ty.apply(undefined, p));
		}
		// Draw to the next point
		while((p = args.pop()) !== undefined) {
		    ctx.lineTo(tx.apply(undefined, p), ty.apply(undefined, p));
		}
		ctx.closePath();
		ctx.stroke();
		ctx.fill();
	    };
	})(ctx, dimetricTx, dimetricTy);
	
	// Set some context
	ctx.save();
	ctx.scale(1,-1);
	ctx.translate(0,-canvas.height);
	ctx.save();
	
	// Move our graph
	ctx.translate(x,y);  
	// Draw the "container"
	//back
	let  baseColor = ctx.fillStyle;
	//		drawPoly([0,0,depth],[0,height,depth],[width,height,depth],[width,0,depth]);
	//left
	//		drawPoly([0,0,0],[0,0,depth],[0,height,depth],[0,height,0]);
	//right
	ctx.fillStyle =    Utils.pSBC(-0.5,baseColor);
	drawPoly([width,0,0],[width,0,depth],[width,height,depth],[width,height,0]);
	ctx.fillStyle =    baseColor;
	//front
	drawPoly([0,0,0],[0,height,0],[width,height,0],[width,0,0]);
	//top		
	ctx.fillStyle =    Utils.pSBC(0.5,baseColor);
	drawPoly([0,height,0],[0,height,depth],[width,height,depth],[width,height,0]);
	ctx.fillStyle =    baseColor;
	ctx.restore();
	ctx.restore();
    },

    drawArrow:function(context, fromx, fromy, tox, toy,headlen) {
	let dx = tox - fromx;
	let dy = toy - fromy;
	let angle = Math.atan2(dy, dx);
	context.moveTo(fromx, fromy);
	context.lineTo(tox, toy);
	context.lineTo(tox - headlen * Math.cos(angle - Math.PI / 6), toy - headlen * Math.sin(angle - Math.PI / 6));
	context.moveTo(tox, toy);
	context.lineTo(tox - headlen * Math.cos(angle + Math.PI / 6), toy - headlen * Math.sin(angle + Math.PI / 6));
    },

}

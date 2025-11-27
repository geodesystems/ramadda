

var CLASS_ANNOTATION_TOP = 'ramadda-annotation-top';
var CLASS_ANNOTATION = 'ramadda-annotation';
var CLASS_ANNOTATION_BAR = 'ramadda-annotation-bar';
var CLASS_ANNOTATION_BAR_TOP = 'ramadda-annotation-bar-top';
var CLASS_ANNOTATION_BAR_SIDE = 'ramadda-annotation-bar-side';



function RamaddaAnnotatedImage(attrs,id) {
    let aattrs = {locale: 'auto',
		  allowEmpty: true,
		  readOnly:!attrs.canEdit,
		  image: document.getElementById(id)	 
		 };
    if(!Utils.isDefined(attrs.showToolbar)) attrs.showToolbar= true;
    let anno = this.annotorius = Annotorious.init(aattrs);
    if(attrs.annotations) anno.setAnnotations(attrs.annotations);
    attrs.zoomable = false;
    this.annotator = new  RamaddaAnnotation(anno,id+'_annotations',id+"_top",attrs,"annotations_json");
    anno.formatters=[new RamaddaAnnotationFormatter(this).getFormatter()];
}


function RamaddaZoomableImage(attrs,id) {
    attrs.zoomable = true;
    if(!attrs.annotationsField)
	attrs.annotationsField = "annotations_json";

    if(!Utils.isDefined(attrs.showToolbar)) attrs.showToolbar= true;
    let osd =this.osd = OpenSeadragon(attrs);
    if(attrs.doBookmark) {
	//call the bookmark plugin so the location is tracked in a URL hash
	osd.bookmarkUrl();
    }
    let aattrs = {locale: 'auto',
		  allowEmpty: true,
		  readOnly:!attrs.canEdit,
		 };
    let anno =this.annotorius =  OpenSeadragon.Annotorious(osd,aattrs);
    if(attrs.annotations) anno.setAnnotations(attrs.annotations);
    this.annotator = new  RamaddaAnnotation(anno,id+'_annotations',id+"_top",attrs,attrs.annotationsField);
    anno.formatters=[new RamaddaAnnotationFormatter(this).getFormatter()];
}



function RamaddaAnnotation(annotorius,divId,topDivId,attrs,entryAttribute) {
    this.annotorius = annotorius;
    this.div = jqid(divId);
    this.entryAttribute = entryAttribute;
    this.entryId = attrs.entryId;
    this.canEdit = attrs.canEdit;
    this.authToken = attrs.authToken;
    this.annotations = attrs.annotations;
    this.zoomable = attrs.zoomable;
    this.top = Utils.isDefined(attrs.top)?attrs.top:true;
    if(this.canEdit) {
	if(attrs.showToolbar) {
	    let uid = HU.getUniqueId('toolbar_');
	    let toolbar = HU.div([ATTR_ID,uid]);
	    jqid(topDivId).html(toolbar);
	    Annotorious.Toolbar(annotorius, document.getElementById(uid));
	}
	['selectAnnotation','createSelection'].forEach(event=>{
	    annotorius.on(event, (annotation) => {
		this.addHelpField();
	    });
	});

	annotorius.on('createAnnotation', annotation=>{
	    this.annotations.push(annotation);
	    this.setAnnotations(this.annotations,true);
	});
	annotorius.on('updateAnnotation', annotation=>{
	    const i = this.findIndex(annotation.id);
	    if(i<0) return;
	    this.annotations[i] = annotation;
	    this.setAnnotations(this.annotations,true);
	});
	annotorius.on('deleteAnnotation', annotation=>{
	    const i = this.findIndex(annotation.id);
	    if(i<0) return;
	    this.annotations.splice(i, 1);
	    this.setAnnotations(this.annotations,true);
	});		
    }

    setTimeout(()=>{
	this.showAnnotations(this.annotations);
    },200);


}

function printAnnotations(label,l) {
    console.log(label);
    l.forEach(a=>{
	console.dir('\t',a.body[0].value);
    })
}

RamaddaAnnotation.prototype = {
    findIndex:function(id) {
	return  this.annotations.findIndex(a => a.id === id);
    },
    addHelpField:function() {
	setTimeout(() => {
	    const footer =$('.r6o-tag');
	    if (footer.length==0) return;
	    let help = 'Special tags: <b>label:</b>Some label <b>color:</b>line color<br> <b>width:</b>line width <b>bg:</b>background color  <br><b>border</b>:label border, e.g. 1px solid red<br><b>default:</b>Make this color the default';
	    const extra =HU.div([ATTR_STYLE,HU.css(CSS_PADDING,HU.px(5),
						   CSS_FONT_SIZE,HU.pt(9),CSS_LINE_HEIGHT,HU.em(1))],help);
	    footer.after(extra);
	}, 0);
    },
    handleChange: function(list) {
	if(list) this.annotations =  list;
	this.doSave(this.annotations);
	this.showAnnotations(this.annotations);
    },
    setAnnotations:function(list,dontUpdate) {
	this.annotations= list;
	if(!dontUpdate) {
	    this.getAnno().setAnnotations(this.annotations);
	}
	this.handleChange();
    },

    getAnnotations:function() {
	if(!this.annotations)this.annotations=this.getAnno().getAnnotations();
	return this.annotations;
    },
    showAnnotations: function(annotations) {
	let html = "";
	annotations = annotations ??this.getAnno().getAnnotations();
	let width = "100%";
	if(annotations.length>0) width = Math.floor(100/annotations.length) +"%";
	annotations.forEach((annotation,aidx)=>{
	    let contents = [];
	    let title = "";
	    annotation.body.forEach(function(b) {
		if(b.purpose != 'tagging' || !b.value) return;
		if(b.value.startsWith("label:")) {
		    title =  b.value.replace("label:","");
		}
	    });

	    annotation.body.forEach((body,bidx)=>{
		if(body.purpose=="tagging" || !body.value) return;
		Utils.split(body.value.trim(),'\n',true,true).forEach((line,idx)=>{
		    if(title=="") {
			title = line;
		    } else {
			contents.push(line);
		    }
		});
	    });
	    if(title=="") title = "Annotation"
	    if(this.top) 
		title  = HU.b(title.replace(/ /g,"&nbsp;"));
	    else
		title  = HU.b(title);
	    let body = title;
	    if(contents.length>0) {
		body += HU.div([ATTR_CLASS,'ramadda-annotation-body'],Utils.join(contents,HU.br()));
	    }
//	    body = HU.div([ATTR_TABINDEX,1],body);
	    let tt = this.canEdit?'Click to edit':'Click to view&#013;Shift-click to zoom to';
	    if(this.top)  {
		html+=HU.div([ATTR_TITLE,tt,
			     ATTR_TABINDEX,0,
			     ATTR_WIDTH,width,
			     ATTR_CLASS,HU.classes(CLASS_CLICKABLE,CLASS_HOVERABLE,CLASS_ANNOTATION,CLASS_ANNOTATION_TOP),
			     ATTR_INDEX,aidx], body);
	    }  else  {
		html+=HU.div([ATTR_TITLE,tt,
			     ATTR_TABINDEX,0,
			      ATTR_WIDTH,width,
			      ATTR_CLASS,HU.classes(CLASS_CLICKABLE,CLASS_HOVERABLE,CLASS_ANNOTATION),
			      ATTR_INDEX,aidx], body);
	    }
	});

	if(this.top)
	    html = HU.div([ATTR_CLASS,HU.classes(CLASS_ANNOTATION_BAR,CLASS_ANNOTATION_BAR_TOP) ],html);
	else
	    html = HU.div([ATTR_CLASS,HU.classes(CLASS_ANNOTATION_BAR,CLASS_ANNOTATION_BAR_SIDE)], html);
	this.div.html(html);
	if(annotations.length>0) {
	    this.div.show();
	    this.div.parent().attr(ATTR_WIDTH,HU.px(150));
	} else {
	    this.div.parent().attr(ATTR_WIDTH,HU.px(1));
	    this.div.hide();
	}

/*
	HU.findClass(this.div,CLASS_ANNOTATION).mouseover(function(event) {
	    $(this).focus();
	    });
	    */

	let _this = this;
	let handleMenu = (action,annotation) =>{
	    if(!annotation) return;
	    if(action=='z') {
		_this.getAnno().fitBounds(annotation)
		return;
	    }
	    if(action=='p') {
		_this.getAnno().panTo(annotation);
		return;
	    }	    

	    if(action=='e')  {
		_this.annotorius.selectAnnotation(annotation);
		_this.addHelpField();
		return;
	    }
	    let index =   this.findIndex(annotation.id);
	    if(index<0) return;
	    let list = [...this.annotations];
	    if(action=='f' || action=='b')  {
		if(action=='b' && index>=1) {
		    let o=list[index-1];
		    list[index-1]=annotation;
		    list[index] = o;
		} else 	if(action=='f' && index<list.length-1) {
		    let o=list[index+1];
		    list[index+1]=annotation;
		    list[index] = o;
		} else {
		    return;
		}
		_this.setAnnotations(list,true);
	    } else if(action=='d')  {
		this.annotations.splice(index, 1);
		_this.setAnnotations(this.annotations);
	    }
	};
	let popup = (event,annotation) => {
	    if(!annotation) return;
            if (event.shiftKey && _this.getAnno().fitBounds) {
		_this.getAnno().fitBounds(annotation)
	    } else if(_this.getAnno().panTo) {
		_this.getAnno().panTo(annotation);
	    }
	    if (event.metaKey || !_this.zoomable) {
		_this.getAnno().selectAnnotation(annotation);                                      
	    }
	}
	HU.findClass(this.div,CLASS_ANNOTATION).click(function(event) {
	    let index = $(this).attr(ATTR_INDEX);
	    let annotation = 	annotations[index];
	    if(!_this.canEdit) {
		popup(event,annotation);
		return;
	    }
	    let menu = '';
	    let clazz = HU.classes(CLASS_MENU_ITEM,CLASS_CLICKABLE);
	    let menuItem = (action,label)=>{
		menu+=HU.div([ATTR_CLASS,clazz,ATTR_INDEX,index, ATTR_ACTION,action],label);
	    }
	    if(_this.getAnno().fitBounds) {
		menuItem('p','Pan to');
		menuItem('z','Zoom to');
		menu+=HU.thinLine();
	    }
	    menuItem('e','Edit');	    
	    menuItem('f','Move forward');
	    menuItem('b','Move back');	    
	    menu+=HU.thinLine();
	    menuItem('d','Delete');
	    let opts = {anchor:$(this),
			decorate:true,
			at:'left bottom',
			my:'left top',
			content:menu,
			draggable:false};
	    let dialog =  HU.makeDialog(opts);
	    HU.findClass(dialog,CLASS_CLICKABLE).click(function() {
		let index = $(this).attr(ATTR_INDEX);
		let annotation = 	annotations[index];
		handleMenu($(this).attr(ATTR_ACTION),annotation);
		dialog.remove();
	    });


	});
    },
    getAnno:function() {
	return this.annotorius;
    },
    doSave:function(list) {
	list = list ??this.getAnnotations();
	this.annotations = list;
//	printAnnotations('dosave',list);
	let annotations = JSON.stringify(list);
	let success = r=>{
	    if(r && r.error) {
		alert('An error has occurred:' + r.error);
	    }
	};
	
	let error = r=>{
	    let e = r;
	    if(typeof e   == "string") e = JSON.parse(e);
	    alert("An error occurred:" + (e?e.error:r));
	};	
	RamaddaUtil.changeField(this.entryId,this.entryAttribute,annotations, success,error);
    }
}


function RamaddaAnnotationFormatter(holder) {
    this.holder= holder;
}


RamaddaAnnotationFormatter.prototype = {
    //Convert the color to a name and if we haven't written css for it then do so
    //return the name
    getFormatter:function() {
	return  (annotation) => {
	    let getBodies=annotation=>{
		if(annotation.bodies) return annotation.bodies;
		if(annotation.body) return annotation.body;
		return [];
	    };

	    let bg = null;
	    let border = null;	    
	    let getTags = annotation=>{
		return  getBodies(annotation).filter(b=>{
		    if(b.purpose != 'tagging' || !b.value) return false;
		    return true;
		});
	    };
	    let state = {
	    }
	    this.holder.annotator.getAnnotations().forEach(annotation=>{
		let tags =  getTags(annotation);
		let isDefault = false;
		tags.every(function(b) {
		    let v = b.value;
		    if(v.startsWith("default:")) {
			isDefault  =true;
			return false;
		    }
		    return true;
		});
		if(!isDefault) return;
		tags.forEach(function(b) {
		    let v = b.value;
		    if(v.startsWith("bg:")) {
			state.bg = v.replace("bg:","");		    
		    } else if(v.startsWith("b:")) {
			state.border = v.replace("b:","");
		    } else if(v.startsWith("border:")) {
			state.border = v.replace("border:","");			
		    } else if(v.startsWith("c:")) {
			state.color = v.replace("c:","");
		    } else if(v.startsWith("color:")) {
			state.color = v.replace("color:","");			
		    } else if(v.startsWith("w:")) {
			state.width = v.replace("w:","");
		    }
		});
	    });


	    let tags =  getTags(annotation);
	    tags.forEach(function(b) {
		let v = b.value;
		if(v.startsWith("bg:")) {
		    state.bg = v.replace("bg:","");
		} else 	if(v.startsWith("labelcolor:")) {
		    state.labelcolor = v.replace("labelcolor:","");		    
		} else if(v.startsWith("b:")) {
		    state.border = v.replace("b:","");
		} else if(v.startsWith("border:")) {
		    state.border = v.replace("border:","");		    
		}
	    });

	    let result = {};
	    tags.forEach((b) =>{
		let v = b.value;
		if(v.startsWith("c:")) {
		    state.color = v.replace("c:","");
		} else 	if(v.startsWith("color:")) {
		    state.color = v.replace("color:","");
		} else 	if(v.startsWith("bg:")) {
		    state.bg= v.replace("bg:","");		    		    		    
		} else if(v.startsWith("w:")) {
		    state.width = v.replace("w:","");
		} else if(v.startsWith("width:")) {
		    state.width = v.replace("width:","");		    
		} else if(v.startsWith("label:")) {
		    let label =  v.replace("label:","");
		    if(Utils.isSafari()) {
			const text = document.createElementNS(NAMESPACE_SVG, 'text');
			text.setAttribute('x', 0);
			text.setAttribute('y',  -4);
			text.setAttribute('fill', '#fff');
			text.setAttribute('font-size', '12');
			text.textContent = label;
			result.element=text;
		    } else {
			const fo = document.createElementNS(NAMESPACE_SVG, 'foreignObject');
			fo.setAttribute(ATTR_WIDTH, '1px');
			fo.setAttribute(ATTR_HEIGHT, '1px');
			let html = HU.open(TAG_DIV,[ATTR_CLASS,'a9s-shape-label-wrapper']);
			let style='';
			if(state.bg) style+=HU.css(CSS_BACKGROUND,state.bg);
			if(state.labelcolor) style+=HU.css(CSS_COLOR,state.labelcolor);
			if(state.border) style+=HU.css(CSS_BORDER,state.border);		    
			html +=HU.div([ATTR_STYLE,style,
				       ATTR_CLASS,'a9s-shape-label'],
				      HU.div([ATTR_STYLE,HU.css(CSS_PADDING,HU.px(2))],label));
			html+=HU.close(TAG_DIV);
			fo.innerHTML = html;
			result.element= fo;
		    }
		}
	    });
	    
	    let classes = "";
	    if(state.bg) {
		classes= HU.classes(classes,this.checkBg(state.bg));
	    }
	    if (state.color) {
		classes= HU.classes(classes,this.checkColor(state.color));
	    }
	    if(state.width) {
		classes= HU.classes(classes,this.checkWidth(state.width));
	    }
	    result.className  =classes;
	    return result;
	};
    },

    checkColor:function(color) {
	if(!this.colorMap) this.colorMap = {};
	if(color.startsWith("c:")) color = color.replace("c:","");
	let name ='ramadda-annotation-' + Utils.makeID(color);
	if(this.colorMap[color]) return name;
	this.colorMap[color] = true;
	let template = ".a9s-annotationlayer .a9s-annotation.{name} .a9s-inner, .a9s-annotationlayer .a9s-annotation.{name}.editable.selected .a9s-inner {stroke:{value} !important;}\n.a9s-annotationlayer .a9s-annotation.{name}:hover .a9s-inner  {stroke:yellow !important;}";
	let css = template.replace(/{name}/g,name).replace(/{value}/g,color);
	$(HU.tag(TAG_STYLE,[ATTR_TYPE,'text/css'], css)).appendTo(document.body);

	return   name;
    },
    checkBg:function(color) {
	if(!this.colorMap) this.colorMap = {};
	if(color.startsWith("c:")) color = color.replace("c:","");
	let name ='ramadda-annotation-' + Utils.makeID(color);
	//let name = 'ramadda-annotation-'+ color.replace(/#/g,"").replace(/\(/g,"_").replace(/\)/g,"_").replace(/,/g,"_");
	if(this.colorMap[color]) return name;
	this.colorMap[color] = true;
	let template = ".a9s-annotationlayer .a9s-annotation.{name} .a9s-inner, .a9s-annotationlayer .a9s-annotation.{name}.editable.selected .a9s-inner {fill:{value} !important;}\n.a9s-annotationlayer .a9s-annotation.{name}:hover .a9s-inner  {fill:yellow !important;}";
	let css = template.replace(/{name}/g,name).replace(/{value}/g,color);
	$(HU.tag(TAG_STYLE,[ATTR_TYPE,'text/css'], css)).appendTo(document.body);
	return   name;
    },

    checkWidth:function(width) {
	let name = "width_" + width;
	if(!this.widthMap) this.widthMap = {};
	if(this.widthMap[name]) return name;
	this.widthMap[name] = true;
	let template = ".a9s-annotationlayer .a9s-annotation.{name} .a9s-inner, .a9s-annotationlayer .a9s-annotation.{name}.editable.selected .a9s-inner {stroke-width:{value} !important;}";
	let css = template.replace(/{name}/g,name).replace(/{value}/g,width);
	$("<style type='text/css'>" + css+"</css>").appendTo(document.body);
	return   name;
    },
}



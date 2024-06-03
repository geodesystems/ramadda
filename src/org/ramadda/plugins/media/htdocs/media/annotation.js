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
    this.annotator = new  RamaddaAnnotation(anno,id+'_annotations',id+"_top",attrs,"edit_type_annotated_image_annotations_json");
    anno.formatters=[new RamaddaAnnotationFormatter(this).getFormatter()];
}


function RamaddaZoomableImage(attrs,id) {
    attrs.zoomable = true;
    if(!attrs.annotationsField)
	attrs.annotationsField = "media_zoomify_annotations_json";

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
    this.annotator = new  RamaddaAnnotation(anno,id+'_annotations',id+"_top",attrs,"edit_"+attrs.annotationsField);
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
	    let tt = 'Special tags:&#013;width:line width (or w:)&#013;color:line color (or c:)&#013;label:Some label&#013;bg:label background color&#013;border:label border, e.g. 1px solid red&#013;default:Make this color the default';
	    jqid(topDivId).html(HU.hbox([toolbar,HU.space(1),
					 HU.div([ATTR_TITLE,tt],
						HU.getIconImage('fas fa-question-circle'))]));
	    Annotorious.Toolbar(annotorius, document.getElementById(uid));
	}
	let changed = (a) =>{
	    this.handleChange();
	};
	annotorius.on('createAnnotation', changed);
	annotorius.on('updateAnnotation', changed);
	annotorius.on('deleteAnnotation', changed);		
    }

    setTimeout(()=>{
	this.showAnnotations(attrs.annotations);
    },200);


}

RamaddaAnnotation.prototype = {
    handleChange: function() {
	this.annotations = this.getAnno().getAnnotations();
	this.doSave();
	this.showAnnotations();
    },
    getAnnotations:function() {
	if(this.annotations) return this.annotations;
	return this.getAnno().getAnnotations();
    },
    showAnnotations: function(annotations) {
	let html = "";
	annotations = annotations ??this.getAnno().getAnnotations();
	let width = "100%";
	if(annotations.length>0) width = Math.floor(100/annotations.length) +"%";
	annotations.forEach((annotation,aidx)=>{
	    let contents = [];
	    let title = "";
	    annotation.body.forEach((body,bidx)=>{
		if(body.purpose=="tagging") return;
		let value = body.value;
		if(!value) return;
		let lines = value.trim().split("\n");
		lines.forEach((line,idx)=>{
		    if(title=="") {
			title = line;
		    } else {
			contents.push(line);
		    }
		});
	    });
	    if(title=="") {
		annotation.body.forEach(function(b) {
		    if(b.purpose != 'tagging' || !b.value) return false;
		    if(b.value.startsWith("label:")) {
			title =  b.value.replace("label:","");
		    }
		});
	    }
	    if(title=="") title = "&lt;annotation&gt;"
	    if(this.top) 
		title  = HU.b(title.replace(/ /g,"&nbsp;"));
	    else
		title  = HU.b(title);
	    let body = title;
	    if(contents.length>0) {
		body += HU.div(['class','ramadda-annotation-body'],Utils.join(contents,"<br>"));
	    }
	    body = HU.div(['tabindex','1'],body);
	    let tt = 'Click to view&#013;Shift-click to zoom to';
	    if(this.canEdit) tt+='&#013;Control-e:edit&#013;Control-d:delete&#013;Control-f:move forward&#013;Control-b:move back';
	    if(this.top) 
		html+=HU.td(['title',tt,'width',width,'class','ramadda-clickable ramadda-hoverable ramadda-annotation','index',aidx], body);
	    else
		html+=HU.div(['title',tt,'width',width,'class','ramadda-clickable ramadda-hoverable ramadda-annotation','index',aidx], body);
	});

	if(this.top)
	    html = HU.div(['class','ramadda-annotation-bar'], HU.table([],HU.tr(['valign','top'],html)));
	else
	    html = HU.div(['class','ramadda-annotation-bar'], html);
	this.div.html(html);
	if(annotations.length>0) {
	    this.div.show();
	    this.div.parent().attr('width','150px');
	} else {
	    this.div.parent().attr('width','1px');
	    this.div.hide();
	}
	let _this = this;
	this.div.find('.ramadda-annotation').keydown(function(event) {
	    if(!event.ctrlKey)
		return;
	    let annotation = 	_this.annotations[$(this).attr('index')];
	    if(!annotation) return;
	    if(event.key=='e')  {
		_this.annotorius.selectAnnotation(annotation);
	    } else if(event.key=='f' || event.key=='b')  {
		let list = [..._this.getAnno().getAnnotations()];
		let index = -1;
		list.every((a,idx)=>{
		    if(a.id==annotation.id) {
			index=idx;
			return false;
		    }
		    return true;
		});
		if(event.key=='b' && index>=1) {
		    let o=list[index-1];
		    list[index-1]=annotation;
		    list[index] = o;
		} else 	if(event.key=='f' && index<list.length-1) {
		    let o=list[index+1];
		    list[index+1]=annotation;
		    list[index] = o;
		} else {
		    return;
		}
		_this.getAnno().setAnnotations(list);
		_this.handleChange();

	    } else if(event.key=='d')  {
		_this.annotorius.removeAnnotation(annotation);
		_this.handleChange();
	    }
	});
	this.div.find('.ramadda-annotation').click(function(event) {
	    let annotation = 	annotations[$(this).attr('index')];
	    if(!annotation) return;
            if (event.shiftKey && _this.getAnno().fitBounds) {
		_this.getAnno().fitBounds(annotation)
	    } else if(_this.getAnno().panTo) {
		_this.getAnno().panTo(annotation);
	    }
	    if (event.metaKey || !_this.zoomable) {
		_this.getAnno().selectAnnotation(annotation);                                      
	    }
	});
    },
    getAnno:function() {
	return this.annotorius;
    },
    doSave:function() {
	var annotations = JSON.stringify(this.getAnno().getAnnotations());
	let args = {};
	args[this.entryAttribute] = annotations;
	let success = r=>{
	};
	
	let error = r=>{
	    let e = r;
	    if(typeof e   == "string") e = JSON.parse(e);
	    alert("An error occurred:" + (e?e.error:r));
	};	
	RamaddaUtil.doSave(this.entryId,this.authToken,args, success,error);
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
		} else if(v.startsWith("b:")) {
		    state.border = v.replace("b:","");
		} else if(v.startsWith("border:")) {
		    state.border = v.replace("border:","");		    
		}
	    });

	    let result = {};
	    tags.forEach(function(b) {
		let v = b.value;
		if(v.startsWith("c:")) {
		    state.color = v.replace("c:","");
		} else 	if(v.startsWith("color:")) {
		    state.color = v.replace("color:","");		    
		} else if(v.startsWith("w:")) {
		    state.width = v.replace("w:","");
		} else if(v.startsWith("width:")) {
		    state.width = v.replace("width:","");		    
		} else if(v.startsWith("label:")) {
		    let label =  v.replace("label:","");
		    //original from the shapelabel plugin
		    //https://github.com/recogito/recogito-client-plugins/blob/main/plugins/annotorious-shape-labels
		    //modified to check for the label: tag
		    const foreignObject = document.createElementNS('http://www.w3.org/2000/svg', 'foreignObject');
		    // Overflow is set to visible, but the foreignObject needs >0 zero size,
		    // otherwise FF doesn't render...
		    foreignObject.setAttribute('width', '1px');
		    foreignObject.setAttribute('height', '1px');
		    let html = '<div xmlns="http://www.w3.org/1999/xhtml" class="a9s-shape-label-wrapper">';
		    let style='';
		    if(state.bg) style+=HU.css('background',state.bg);
		    if(state.border) style+=HU.css('border',state.border);		    
		    html +=HU.div(['style',style,'class','a9s-shape-label'],HU.div([ATTR_STYLE,HU.css('padding','2px')],label));
		    console.log(style);
		    html+="</div>";
		    foreignObject.innerHTML = html;
		    result.element= foreignObject;
		}
	    });
	    
	    let classes = "";
	    if (state.color) {
		classes+= this.checkColor(state.color) +" ";
	    }
	    if(state.width) {
		classes+= this.checkWidth(state.width) +" ";
	    }
	    result.className  =classes;
	    return result;
	};
    },


    checkColor:function(color) {
	if(!this.colorMap) this.colorMap = {};
	if(color.startsWith("c:")) color = color.replace("c:","");
	let name = color.replace(/#/g,"").replace(/\(/g,"_").replace(/\)/g,"_").replace(/,/g,"_");
	if(this.colorMap[color]) return name;
	this.colorMap[color] = true;
	let template = ".a9s-annotationlayer .a9s-annotation.{name} .a9s-inner, .a9s-annotationlayer .a9s-annotation.{name}.editable.selected .a9s-inner {stroke:{value} !important;}\n.a9s-annotationlayer .a9s-annotation.{name}:hover .a9s-inner  {stroke:yellow !important;}";
	let css = template.replace(/{name}/g,name).replace(/{value}/g,color);
	$("<style type='text/css'>" + css+"</css>").appendTo(document.body);
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



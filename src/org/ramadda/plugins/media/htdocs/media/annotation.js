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
    this.annotator = new  RamaddaAnnotation(anno,id+'_annotations',id+"_top",attrs,"edit_media_zoomify_annotations_json");
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
	    Annotorious.Toolbar(annotorius, document.getElementById(topDivId));
	}
	let changed = (a) =>{
	    this.annotations = this.getAnno().getAnnotations();
	    this.doSave();
	    this.showAnnotations();
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
    getAnnotations:function() {
	if(this.annotations) return this.annotations;
	return this.getAnno().getAnnotations();
    },
    showAnnotations: function(annotations) {
	let html = "";
	annotations = annotations ||this.getAnno().getAnnotations();
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
	    if(this.top) 
		html+=HU.td(['title','Click to view&#013;Shift-click to zoom to','width',width,'class','ramadda-clickable ramadda-hoverable ramadda-annotation','index',aidx], body);
	    else
		html+=HU.div(['title','Click to view&#013;Shift-click to zoom to','width',width,'class','ramadda-clickable ramadda-hoverable ramadda-annotation','index',aidx], body);
	});

	if(this.top)
	    html = HU.div(['class','ramadda-annotation-bar'], HU.table([],HU.tr(['valign','top'],html)));
	else
	    html = HU.div(['class','ramadda-annotation-bar'], html);
	this.div.html(html);
	let _this = this;
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
		    } else if(v.startsWith("c:")) {
			state.color = v.replace("c:","");
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
		}
	    });

	    let result = {};
	    tags.forEach(function(b) {
		let v = b.value;
		if(v.startsWith("c:")) {
		    state.color = v.replace("c:","");
		} else if(v.startsWith("w:")) {
		    state.width = v.replace("w:","");
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
		    let style="";
		    if(state.bg) style+=HU.css('background',state.bg);
		    if(state.border) style+=HU.css('border',state.border);		    
		    html +=HU.div(['style',style,'class','a9s-shape-label'],label);
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



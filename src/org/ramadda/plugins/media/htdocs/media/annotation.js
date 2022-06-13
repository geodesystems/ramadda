
function RamaddaAnnotatedImage(attrs,id) {
    let aattrs = {locale: 'auto',
		  allowEmpty: true,
		  readOnly:!attrs.canEdit,
		  formatter:new  RamaddaAnnotationFormatter().getFormatter(),
		  image: document.getElementById(id)	 
		 };
    let anno = Annotorious.init(aattrs);
    if(attrs.annotations) anno.setAnnotations(attrs.annotations);
    this.annotator = new  RamaddaAnnotation(anno,id+'_annotations',id+"_top",attrs,"edit_type_annotated_image_annotations_json");
}

function RamaddaZoomableImage(attrs,id) {
    let osd =this.osd = OpenSeadragon(attrs);
    if(attrs.doBookmark) {
	//call the bookmark plugin so the location is tracked in a URL hash
	osd.bookmarkUrl();
    }
    let aattrs = {locale: 'auto',
		  allowEmpty: true,
		  readOnly:!attrs.canEdit,
		  formatter:new  RamaddaAnnotationFormatter().getFormatter(),
		 };
    let anno =this.annotation =  OpenSeadragon.Annotorious(osd,aattrs);
    if(attrs.annotations) anno.setAnnotations(attrs.annotations);
    this.annotator = new  RamaddaAnnotation(anno,id+'_annotations',id+"_top",attrs,"edit_media_zoomify_annotations_json");
}



function RamaddaAnnotation(annotorius,divId,topDivId,attrs,entryAttribute) {
    this.annotorius = annotorius;
    this.div = jqid(divId);
    this.entryAttribute = entryAttribute;
    this.entryId = attrs.entryId;
    this.canEdit = attrs.canEdit;
    this.authToken = attrs.authToken;
    if(this.canEdit) {
	Annotorious.Toolbar(annotorius, document.getElementById(topDivId));
	let changed = (a) =>{
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
	    if(title=="") title = "&lt;annotation&gt;"

	    title  = HU.b(title.replace(/ /g,"&nbsp;"));
	    let body = title;
	    if(contents.length>0) {
		body += HU.div(['class','ramadda-annotation-body'],Utils.join(contents,"<br>"));
	    }
	    html+=HU.td(['title','Click to view&#013;Shift-click to zoom to','width',width,'class','ramadda-clickable ramadda-hoverable ramadda-annotation','index',aidx], body);
	});

	html = HU.div(['class','ramadda-annotation-bar'], HU.table([],HU.tr(['valign','top'],html)));
	this.div.html(html);
	let _this = this;
	this.div.find('.ramadda-annotation').click(function(event) {
	    let annotation = 	annotations[$(this).attr('index')];
	    if(!annotation) return;
            if (event.shiftKey && _this.getAnno().fitBounds) {
		_this.getAnno().fitBounds(annotation)
	    } else if(_this.getAnno().panTo) {
		_this.getAnno().panTo(annotation);
	    } else {
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


function RamaddaAnnotationFormatter() {
}


RamaddaAnnotationFormatter.prototype = {
    //Convert the color to a name and if we haven't written css for it then do so
    //return the name
    getFormatter:function() {
	return  (annotation) => {
	    let color=null;
	    let width=null;	
	    let result = {};
	    annotation.bodies.forEach(function(b) {
		if(b.purpose != 'tagging' || !b.value) return false;
		if(b.value.startsWith("color:")) {
		    color = b.value;
		} else if(b.value.startsWith("width:")) {
		    width = b.value.replace("width:","");
		}  else if(b.value.startsWith("label:")) {
		    let label =  b.value.replace("label:","");
		    //original from the shapelabel plugin
		    //https://github.com/recogito/recogito-client-plugins/blob/main/plugins/annotorious-shape-labels
		    //modified to check for the label: tag
					const foreignObject = document.createElementNS('http://www.w3.org/2000/svg', 'foreignObject');

		    // Overflow is set to visible, but the foreignObject needs >0 zero size,
		    // otherwise FF doesn't render...
		    foreignObject.setAttribute('width', '1px');
		    foreignObject.setAttribute('height', '1px');

		    foreignObject.innerHTML = '<div xmlns="http://www.w3.org/1999/xhtml" class="a9s-shape-label-wrapper"><div class="a9s-shape-label">' + label +
			'</div></div>';
		    result.element= foreignObject;
		}
	    });
	    
	    let classes = "";
	    if (color) {
		classes+= this.checkColor(color) +" ";
	    }
	    if(width) {
		classes+= this.checkWidth(width) +" ";
	    }
	    result.className  =classes;
	    return result;
	};
    },


    checkColor:function(color) {
	if(!this.colorMap) this.colorMap = {};
	if(color.startsWith("color:")) color = color.replace("color:","");
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



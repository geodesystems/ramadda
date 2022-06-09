
function RamaddaZoomify(entryId,authToken,attrs,id,canEdit,annotations) {
    this.colorMap = {};
    this.widthMap = {};
    this.divId = id;
    this.bottom = jqid(id+"_bottom");
    this.entryId = entryId;
    this.authToken = authToken;
    this.canEdit = canEdit;    
    let osd =this.osd = OpenSeadragon(attrs);

    var formatter = (annotation) => {
	let color=null;
	let width=null;	
	annotation.bodies.forEach(function(b) {
            if(b.purpose != 'tagging' || !b.value) return false;
	    if(b.value.startsWith("color:")) {
		color = b.value;
	    } else if(b.value.startsWith("width:")) {
		width = b.value.replace("width:","");
	    }
	});
	
	let classes = "";
	if (color) {
	    classes+= this.checkColor(color) +" ";
	}
	if(width) {
	    classes+= this.checkWidth(width) +" ";
	}
	return classes;
    };

    let aattrs = {locale: 'auto',
		  allowEmpty: true,
		  readOnly:!canEdit,
		  formatter:formatter,
//		  disableEditor: !canEdit
		 };


    let anno =this.annotation =  OpenSeadragon.Annotorious(osd,aattrs);

    if(annotations) anno.setAnnotations(annotations);
    if(canEdit) {
	Annotorious.Toolbar(anno, document.getElementById(id+"_top"));
	let changed = (a) =>{
	    this.doSave();
	    this.showAnnotations();
	};
	anno.on('createAnnotation', changed);
	anno.on('updateAnnotation', changed);
	anno.on('deleteAnnotation', changed);		
    }
    setTimeout(()=>{
	this.showAnnotations(annotations);
    },500);
}

RamaddaZoomify.prototype = {
    //Convert the color to a name and if we haven't written css for it then do so
    //return the name
    checkColor:function(color) {
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
	if(this.widthMap[name]) return name;
	this.widthMap[name] = true;
	let template = ".a9s-annotationlayer .a9s-annotation.{name} .a9s-inner, .a9s-annotationlayer .a9s-annotation.{name}.editable.selected .a9s-inner {stroke-width:{value} !important;}";
	let css = template.replace(/{name}/g,name).replace(/{value}/g,width);
	$("<style type='text/css'>" + css+"</css>").appendTo(document.body);
	return   name;
    },

    showAnnotations: function(annotations) {
	let html = "";
	annotations = annotations ||this.annotation.getAnnotations();
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
	    title  = HU.b(title.replace(/ /g,"&nbsp;"));
	    let body = title;
	    if(contents.length>0) {
		body += HU.div(['class','ramadda-zoomify-annotation-body'],Utils.join(contents,"<br>"));
	    }
	    html+=HU.td(['title','Click to view&#013;Shift-click to highlight','width',width,'class','ramadda-clickable ramadda-hoverable ramadda-zoomify-annotation','index',aidx], body);
	});

	html = HU.div(['class','ramadda-zoomify-annotation-bar'], HU.table([],HU.tr(['valign','top'],html)));
	this.bottom.html(html);
	let _this = this;
	this.bottom.find('.ramadda-zoomify-annotation').click(function(event) {
	    let annotation = 	annotations[$(this).attr('index')];
	    if(!annotation) return;
	    _this.annotation.panTo(annotation);
//            if (event.shiftKey) {
		_this.annotation.selectAnnotation(annotation);
//	    }
	});
    },
    doSave:function() {
	var annotations = JSON.stringify(this.annotation.getAnnotations());
	let args = {
	    "edit_media_zoomify_annotations_json":annotations
	}
	let success = r=>{
	};
	
	let error = r=>{
	    if(typeof r   == "string") r = JSON.parse(r);
	    alert("An error occurred:" + r.error);
	};	
	RamaddaUtil.doSave(this.entryId,this.authToken,args, success,error);
    }
}

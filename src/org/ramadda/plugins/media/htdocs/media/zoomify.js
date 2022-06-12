
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
    if(attrs.canEdit) {
	Annotorious.Toolbar(anno, document.getElementById(id+"_top"));
	let changed = (a) =>{
	    this.doSave();
	    this.showAnnotations();
	};
	anno.on('createAnnotation', changed);
	anno.on('updateAnnotation', changed);
	anno.on('deleteAnnotation', changed);		
    }
    
    this.annotator = new  RamaddaAnnotation(anno,id+'+bottom',attrs.entryId,"edit_media_zoomify_annotations_json",attrs.authToken);
}

RamaddaZoomableImage.prototype = {
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
	    html+=HU.td(['title','Click to view&#013;Shift-click to zoom to','width',width,'class','ramadda-clickable ramadda-hoverable ramadda-zoomify-annotation','index',aidx], body);
	});

	html = HU.div(['class','ramadda-zoomify-annotation-bar'], HU.table([],HU.tr(['valign','top'],html)));
	this.bottom.html(html);
	let _this = this;
	this.bottom.find('.ramadda-zoomify-annotation').click(function(event) {
	    let annotation = 	annotations[$(this).attr('index')];
	    if(!annotation) return;
            if (event.shiftKey) {
		_this.annotation.fitBounds(annotation);
//		_this.annotation.selectAnnotation(annotation);
	    } else {
		_this.annotation.panTo(annotation);
	    }
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
	    let e = r;
	    if(typeof e   == "string") e = JSON.parse(e);
	    alert("An error occurred:" + (e?e.error:r));
	};	
	RamaddaUtil.doSave(attrs.entryId,attrs.authToken,args, success,error);
    }
}

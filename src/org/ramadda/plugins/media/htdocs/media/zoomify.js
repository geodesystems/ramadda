
function RamaddaZoomify(entryId,authToken,attrs,id,canEdit,annotations) {
    this.divId = id;
    this.bottom = jqid(id+"_bottom");
    this.entryId = entryId;
    this.authToken = authToken;
    this.canEdit = canEdit;    
    let osd =this.osd = OpenSeadragon(attrs);
    let anno =this.annotation =  OpenSeadragon.Annotorious(osd,{locale: 'auto',
								allowEmpty: true,
								readOnly:!canEdit});

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
    showAnnotations: function(annotations) {
	let html = "";
	annotations = annotations ||this.annotation.getAnnotations();
	let width = "100%";
	if(annotations.length>0) width = Math.floor(100/annotations.length) +"%";
	annotations.forEach((annotation,aidx)=>{
	    let contents = [];
	    let title = "";
	    annotation.body.forEach((body,bidx)=>{
		let value = body.value;
		if(!value) return;
		let lines = value.trim().split("\n");
		lines.forEach((line,idx)=>{
		    if(idx==0 && bidx==0) {
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
            if (event.shiftKey) {
		_this.annotation.selectAnnotation(annotation);
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
	    if(typeof r   == "string") r = JSON.parse(r);
	    alert("An error occurred:" + r.error);
	};	
	RamaddaUtil.doSave(this.entryId,this.authToken,args, success,error);
    }
}

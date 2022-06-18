
function RamaddaMediaTranscript(attrs) {
    this.media = attrs.media;
    this.attrs = attrs;
    this.id = attrs.id;
    this.div=attrs.div;
    this.points = attrs.points;
    this.searchId = attrs.searchId;
    this.init();
}

RamaddaMediaTranscript.prototype = {
    init:function() {
	let func=null;
	if(this.media == "vimeo") {
	    func = this.initVimeo();
	} else   if(this.media == "media") {
	    func = this.initMedia();
	} else   if(this.media == "soundcloud") {
	    func = this.initSoundcloud();
	} else   if(this.media == "youtube") {
	    func = this.initYoutube();
	} else {
	    console.log("Unknown media:" + this.media);
	}

	if(!func) {
	    return;
	}
	if(typeof func == "function") {
	    this.gotoTime =  func;
	} else  {
	    this.gotoTime = func.gotoTime;
	    this.getTime = func.getTime;	    
	}
	   
	if(!this.points) this.points = [];
//	this.points = [{time:30,title:"30 seconds in"}]
	if(this.gotoTime && this.points)  {
	    this.makePoints();
	}
    },	
    domId:function(suffix) {
	return this.id +"_" + suffix;
    },
    jq:function(suffix) {
	return jqid(this.domId(suffix));
    },    
    getProperty:function(key,dflt) {
	let v =  this.attrs[key];
	if(Utils.isDefined(v)) return v;
	return dflt;
    },
    clearSearch:function() {
	this.points.forEach((p,idx)=>{
	    jqid(p.rowId).show();
	    let details =  jqid(p.detailsId);
	    if(details.attr('displayed'))
		details.show();		
	    else
		details.hide();
	});
	jqid(this.searchId).val("");
	jqid(this.searchId+"_results").html("");
    },
    doSearch:function() {
	let val =     jqid(this.searchInputId).val().trim();
	if(val=="") {
	    this.clearSearch();
	    return
	}
	let results = "";
	let regexp = new RegExp(val,'i');
	let showCount = 0;
	this.points.forEach((p,idx)=>{
	    let corpus = p.title +" " + p.transcript +" " + p.synopsis + " ";
	    if(p.keywords) corpus+= p.keywords.join(" ");
	    if(p.subjects) corpus+= p.subjects.join(" ");	    
	    if(!corpus.match(regexp)) {
		jqid(p.rowId).hide();
		jqid(p.detailsId).hide();		
		return;
	    }
	    showCount++;
	    jqid(p.rowId).show();
	    let details =  jqid(p.detailsId);
	    if(details.attr('displayed'))
		details.show();		
	});
	if(showCount==0) {
	    jqid(this.searchId+"_results").html("No results");
	} else {
	    jqid(this.searchId+"_results").html(showCount +" matched");
	}
    },

    makePoints:function() {
	let _this = this;
	let canAdd  = this.getProperty("canAddTranscription");
	this.points.sort((a,b)=>{
	    return a.time - b.time;
	});
	let table="<table cellspacing=0 cellpadding=0>";
	this.points.forEach((p,idx)=>{
	    let time = this.formatTime(p.time);
	    let prefix = "";
	    if(canAdd) {
		prefix = HU.span(['title','Delete transcription','point-index',idx,'class','ramadda-clickable ramadda-media-point-delete'], HU.getIconImage('fas fa-eraser'));
		prefix+= HU.span(['title','Edit transcription','point-index',idx,'class','ramadda-clickable ramadda-media-point-edit'], HU.getIconImage('fas fa-edit'));		
		prefix = HU.td(['width','1%','style',HU.css('white-space','nowrap')],prefix);
	    }
	    p.detailsId =  this.searchId +"_details_" + idx;
	    p.rowId = HU.getUniqueId("row_");
	    table+=HtmlUtils.tr(['valign','top','id',p.rowId,'point-index',idx], 
				prefix+
				HU.td(['point-index',idx,'class','ramadda-clickable ramadda-media-point','width','5%','style',HU.css('white-space','nowrap')], time) +
				HU.td(['point-index',idx,'class','ramadda-clickable ramadda-media-point','width','95%'], HU.div([STYLE,HU.css('margin-left','10px')],p.title)));

	    let details =  HU.div([CLASS,'ramadda-clickable ramadda-media-play','data-player-time',p.time],
				  HU.getIconImage('fas fa-play') + ' ' +
				  'Play Segment' );

	    if(Utils.stringDefined(p.synopsis)) {
		details+=HU.div([],HU.b("Synopsis: ") + p.synopsis);
	    }
	    if(p.keywords && p.keywords.length>0) {
		details+=HU.div([],HU.b("Keywords: ") + p.keywords.join(" "));
	    }
	    if(p.subjects && p.subjects.length>0) {
		details+=HU.div([],HU.b("Subjects: ") + p.subjects.join(" "));
	    }
	    details = HU.div([CLASS,'ramadda-media-point-details-inner'],
			     details);
	    let detailsDiv = HU.div([ID,p.detailsId,CLASS,'ramadda-media-point-details',STYLE,HU.css('display','none')],details);
	    table+=HtmlUtils.tr([], HU.td(['colspan','3'], detailsDiv));
	});
	table+="</table>";
	table = HU.div([CLASS,"ramadda-media-points"], table);
	if(this.points.length>0) {
	    this.searchInputId = HU.getUniqueId("search_");
	    let search = HU.input("","",['class','ramadda-media-search', "placeholder","Search",ID,this.searchInputId]) + " " + HU.span([ID,this.searchInputId+"_clear",CLASS,"ramadda-clickable"], HU.getIconImage('fas fa-eraser')) +
		HU.div([ID,this.searchId+"_results",STYLE,HU.css('max-width','300px','overflow-x','auto')]) 

	    search =HU.div([STYLE,HU.css('margin-left','10px')], search);
	    let _this = this;
	    jqid(this.searchId).html(search);
	    jqid(this.searchInputId+"_clear").click((e) =>{
		this.clearSearch();
	    });
	    jqid(this.searchInputId).keypress((e) => {
		if(e.which==13) {
		    this.doSearch();
		}
	    });
	}
	let html = "";
	if(canAdd && this.getTime) {
	    html+=HU.div(['title','Add transcription','id',this.domId('_addtranscription'),'class','ramadda-clickable'], HU.getIconImage('fas fa-plus'));
	}

	html+=table;
	jqid(this.div).html(html);
	this.jq('_addtranscription').click(function(){
	    _this.addOrEditTranscription($(this));
	});
	jqid(this.div).find('.ramadda-media-point-delete').click(function(event) {
	    event.stopPropagation();
	    let index = +$(this).attr('point-index');
	    let point = _this.points[index];
	    if(!confirm('Are you sure you want to delete the transcription:' + point.title+'?')) return;
	    _this.points.splice(index,1);
	    _this.writeTranscription();
	    _this.makePoints();
	});
	jqid(this.div).find('.ramadda-media-point-edit').click(function(event) {
	    event.stopPropagation();
	    let index = +$(this).attr('point-index');
	    _this.addOrEditTranscription($(this),index);
	});

	jqid(this.div).find('.ramadda-media-point').click(function(){
	    let point = _this.points[+$(this).attr('point-index')];
	    let details = jqid(point.detailsId);
	    if(details.is(':visible')) {
		details.hide(300);
		details.attr('displayed',false);
	    } else {
		details.show(300);
		details.attr('displayed',true);
	    }
	});	    
	jqid(this.div).find('.ramadda-media-play').click(function() {
	    let time  = +$(this).attr('data-player-time');
	    if(_this.gotoTime) _this.gotoTime(time);
	});
    },

    addOrEditTranscription: function(widget, index) {
	this.closeEditDialog();
	let point;
	let add = false;
	if(!Utils.isDefined(index) || index<0) {
	    add=true;
	    point = {title:''};
	} else {
	    point = this.points[index];
	}

	let form = "<table class=formtable>";
	form+=HU.formEntry("Title:",HU.input("",point.title,['size','60','id',this.domId('edit_title')]));
	form+=HU.formEntry("Synopsis:",HU.textarea("",point.synopsis||"",['rows','5','cols','60','id',this.domId('edit_synopsis')]));
	if(!add) {
	    form+=HU.formEntry("",HU.checkbox("",['id',this.domId('edit_settime')],false,"Set time to: " +
					      this.formatTime(this.getTime())));
	} else {
	    form+=HU.formEntry("","Current time: " +
			       this.formatTime(this.getTime()));
	}
	form+="</table>";
	form+="<br>";
	form+=HU.span(['id',this.domId('edit_ok')],add?'Add Transcription':'Save Transcription');
	form+=HU.space(3);
	form+=HU.span(['id',this.domId('edit_cancel')],'Cancel');
	form=HU.div(['style',HU.css('margin','10px')], form);

	this.editDialog = HU.makeDialog({content:form,my:"left top",at:"left bottom",anchor:widget,draggable:true,header:true,xinPlace:false});
	let _this = this;
	this.jq('edit_title').focus();
	this.jq('edit_title').keypress((event)=>{
	    if((event.keyCode ? event.keyCode : event.which) == '13') {
		this.applyEdit(add,point);
	    }
	});

	_this.jq('edit_cancel').button().click(() =>{
	    this.closeEditDialog();
	});

	_this.jq('edit_ok').button().click(() =>{
	    this.applyEdit(add,point);
	});
    },
    applyEdit:function(add,point) {
	point.title = this.jq('edit_title').val();
	point.synopsis = this.jq('edit_synopsis').val();	
	if(add || this.jq('edit_settime').is(':checked')) {
	    point.time = Math.floor(this.getTime());
	}
	this.closeEditDialog();
	if(add) this.points.push(point);
	this.writeTranscription();
	this.makePoints();
    },
    closeEditDialog: function() {
	if(this.editDialog) 
	    this.editDialog.remove();
	this.editDialog = null;
    },	
    writeTranscription: function() {
	let json = JSON.stringify(this.points);
	let args = {
	    edit_type_media_transcriptions_json:json
	};
	let success = r=>{
//	    console.log("success");
	};
	let error = r=>{
	    let e = r;
	    if(typeof e   == "string") e = JSON.parse(e);
	    alert("An error occurred:" + (e?e.error:r));
	};	

	RamaddaUtil.doSave(this.attrs.entryId,this.attrs.authToken,args, success,error);
    },

    initVimeo:function() {
	let iframe = document.querySelector('#' + this.id +' iframe');    
	let player = new Vimeo.Player(iframe);
	return (seconds)=>{
	    player.setCurrentTime(seconds);
	    try {
		player.play();
	    } catch {
	    }};
    },
    initSoundcloud:function() {
	let iframe = document.querySelector('#' + this.id +' iframe');    
	let player = SC.Widget(iframe);
	player.getPosition(p=>{console.log("P:" + p)})
	return (seconds)=>{
	    player.seekTo(seconds*1000);
	    try {
		player.play();
	    } catch {
	    }
	};
    },


    initMedia:function() {
	let player = document.querySelector('#' + this.attrs.mediaId);
	let gotoTime= (seconds)=>{
	    if(player.fastSeek)
		player.fastSeek(seconds);
	    else 
		player.currentTime= seconds;	    
	    try {
		player.play();
	    } catch {
	    }
	};
	let getTime = ()=> {
	    return player.currentTime;
	}
	return {gotoTime:gotoTime,getTime:getTime};
    },


    initYoutube:function() {
	if(!window.media_state.youtubeReady) {
	    window.media_state.youtubeState.push(this);
	    return null;
	}

	let playerVars = {
	    'playsinline': 1
	}
	if(this.attrs.start)  playerVars.start = this.attrs.start;
	if(this.attrs.end)  playerVars.end = this.attrs.end;
	if(Utils.isDefined(this.attrs.autoplay))  playerVars.autoplay = this.attrs.autoplay;
	let player = new YT.Player(this.id, {
            height: Utils.isDefined(this.attrs.height) && this.attrs.height>0?this.attrs.height:'390',
            width: Utils.isDefined(this.attrs.width) && this.attrs.width>0?this.attrs.width:'640',
            videoId: this.attrs.videoId,
            playerVars: playerVars,
            events: {
            }
	});

	return {
	    getTime: ()=>{return player.getCurrentTime();},
	    gotoTime:(seconds)=>{
		player.seekTo(seconds,true)
	    }
	};
    },

    formatTime:function(seconds) {
	seconds= Math.floor(seconds);
	seconds = +seconds;
	let hours = Math.floor(seconds/3600);
	seconds-=hours*3600;
	let minutes = Math.floor(seconds/60);
	seconds -= minutes*60;
	let time = Utils.padLeft(""+hours,2,"0") +":" + Utils.padLeft(""+minutes,2,"0")+
	    ":"  + Utils.padLeft(""+seconds,2,"0");
	return time;
    },

    

};


if(!Utils.isDefined(window['media_state'])) {
    window.media_state = {
	youtubeState:[]
    };
}



function onYouTubeIframeAPIReady() {
    window.media_state.youtubeReady = true;
    window.media_state.youtubeState.forEach(obj=>{
	obj.init();
    });
}


var ATTR_POINT_INDEX='point-index';

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

	this.player = {}
	if(!func) {
	    return;
	}
	
	this.player = func;

	
	if(!this.points) this.points = [];
	//	this.points = [{time:30,title:"30 seconds in"}]
	if(this.player.gotoTime && this.points)  {
	    this.makePoints();
	}
	setTimeout(()=>{
	    this.lastPlayTime = 0;
	    this.checkPlayTime();
	},2000);

    },	
    checkPlayTime:function() {
	let cb = time=>{
	    if(time!=this.lastPlayTime) {
		this.lastPlayTime = time;
		let closest = null;
		//		console.log("tick:" + this.formatTime(time));
		this.points.every(p=>{
		    if(time <p.time) {
			return false;
		    }
		    closest=p;
		    return true;
		});
		let lastClosest = this.lastClosest;
		if(closest && closest==this.lastClosest) {
		    //		    console.dir("\tthe same:" + closest.title +" " +this.lastClosest.title);
		    closest=lastClosest=null;
		} 
		if(lastClosest) {
		    //		    console.log("\tHAD LAST:" + lastClosest.title);
		    jqid(lastClosest.rowId).removeClass("ramadda-media-point-inplay");
		    this.lastClosest=null;
		}
		if(closest) {
		    jqid(closest.rowId).addClass("ramadda-media-point-inplay");
		    this.lastClosest = closest;
		    //		    console.log("\tclosest:" + this.lastClosest.title);
		}
	    }
	    setTimeout(()=>{
		this.checkPlayTime();
	    },1000);
	};
	this.player.getTime(cb);
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
	if(val=='') {
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
		prefix = HU.span([ATTR_TITLE,'Delete transcription',
				  ATTR_POINT_INDEX,idx,
				  ATTR_CLASS,HU.classes(CLASS_CLICKABLE,'ramadda-media-point-delete')],
				 HU.getIconImage('fas fa-eraser'));
		prefix+= HU.span([ATTR_TITLE,'Edit transcription',
				  ATTR_POINT_INDEX,idx,
				  ATTR_CLASS,HU.classes(CLASS_CLICKABLE,'ramadda-media-point-edit')],
				 HU.getIconImage('fas fa-edit'));		
		prefix = HU.td([ATTR_WIDTH,HU.perc(1),
				ATTR_CLASS,'ramadda-media-point-header',
				ATTR_STYLE,HU.css(CSS_WHITE_SPACE,WHITE_SPACE_NOWRAP)],prefix);
	    }
	    p.detailsId =  this.searchId +"_details_" + idx;
	    p.rowId = HU.getUniqueId("row_");
	    table+=HU.tr([ATTR_CLASS,CLASS_HOVERABLE,
			  ATTR_VALIGN,ALIGN_TOP,
			  ATTR_ID,p.rowId,
			  ATTR_POINT_INDEX,idx], 
			 prefix+
			 HU.td([ATTR_POINT_INDEX,idx,
				ATTR_CLASS,HU.classes(CLASS_CLICKABLE,'ramadda-media-point-header ramadda-media-point'),
				ATTR_WIDTH,HU.perc(5),
				ATTR_STYLE,HU.css(CSS_WHITE_SPACE,WHITE_SPACE_NOWRAP)], time) +
			 HU.td([ATTR_POINT_INDEX,idx,
				ATTR_CLASS,HU.classes(CLASS_CLICKABLE,'ramadda-media-point-header ramadda-media-point'),
				ATTR_WIDTH,HU.perc(95)],
			       HU.div([ATTR_STYLE,HU.css(CSS_MARGIN_LEFT,HU.px(10))],p.title)));

	    let details =  HU.div([ATTR_CLASS,HU.classes(CLASS_CLICKABLE,'ramadda-media-play'),
				   'data-player-time',p.time],
				  HU.getIconImage(ICON_PLAY) + ' ' +
				  'Play Segment' );

	    if(Utils.stringDefined(p.synopsis)) {
		details+=HU.div([],HU.boldLabel("Synopsis") +p.synopsis);
	    }
	    if(p.keywords && p.keywords.length>0) {
		details+=HU.div([],HU.boldLabel("Keywords") +
				p.keywords.join(" "));
	    }
	    if(p.subjects && p.subjects.length>0) {
		details+=HU.div([],HU.boldLabel("Subjects") +
				p.subjects.join(" "));
	    }
	    details = HU.div([ATTR_CLASS,'ramadda-media-point-details-inner'],
			     details);
	    let detailsDiv = HU.div([ATTR_ID,p.detailsId,
				     ATTR_CLASS,'ramadda-media-point-details',
				     ATTR_STYLE,HU.css(CSS_DISPLAY,DISPLAY_NONE)],details);
	    table+=HU.tr([], HU.td([ATTR_COLSPAN,3], detailsDiv));
	});
	table+=HU.close(TAG_TABLE);
	let extra = '';
	if(canAdd && this.player.getTime) {
	    extra =HU.span([ATTR_TITLE,'Add transcription',
			    ATTR_ID,this.domId('_addtranscription'),
			    ATTR_STYLE,HU.css(CSS_MARGIN_RIGHT,HU.px(10)),
			    ATTR_CLASS,CLASS_CLICKABLE],
			   HU.getIconImage('fas fa-plus'));
	}

	if(canAdd) {
	    extra+=HU.span([ATTR_TITLE,'Delete all',
			    ATTR_ID,this.domId('_deletetranscription'),
			    ATTR_STYLE,HU.css(CSS_MARGIN_RIGHT,HU.px(10)),
			    ATTR_CLASS,CLASS_CLICKABLE],
			   HU.getIconImage('fas fa-trash-can'));
	}
	table = HU.div([ATTR_CLASS,"ramadda-media-points"], table);
	let search='';
	this.searchInputId = HU.getUniqueId("search_");
	let exportId =HU.getUniqueId("export_");
	if(this.points.length>0) {
	    search = HU.span([ATTR_ID, exportId,
			      ATTR_CLASS,CLASS_CLICKABLE,
			      ATTR_TITLE,'Export'],
			     HU.getIconImage('fas fa-file-export'))+SPACE2;
	    search += HU.input("","",
			       [ATTR_CLASS,'ramadda-media-search',
				ATTR_PLACEHOLDER,"Search",
				ATTR_ID,this.searchInputId]) + " " +
		HU.span([ATTR_TITLE,'Clear search',
			 ATTR_ID,this.searchInputId+"_clear",
			 ATTR_CLASS,CLASS_CLICKABLE],
			HU.getIconImage('fas fa-eraser'));
	    search+=
		HU.div([ATTR_ID,this.searchId+"_results",
			ATTR_STYLE,HU.css(CSS_MAX_WIDTH,HU.px(300),
					  CSS_OVERFLOW_X,'auto')]);
	}
	search =HU.div([ATTR_STYLE,HU.css(CSS_MARGIN_LEFT,HU.px(10))], extra+search);
	jqid(this.searchId).html(search);
	jqid(exportId).click(()=>{
	    let csv ='';
	    csv+='time,title,synopsis\n';
	    this.points.forEach(p=>{
		csv+=p.time+',';
		let  t= p.title??'';
		if(t.indexOf(',')>=0) t = '"' + t +'"';
		csv+=t+',';		
		let s = p.synopsis;
		if(s.indexOf(',')>=0 || s.indexOf('\n')>=0) s = '"' + s +'"';
		csv+=s+'\n';				
	    });
            Utils.makeDownloadFile('annotation.csv',csv);
	});
	jqid(this.searchInputId+"_clear").click((e) =>{
	    this.clearSearch();
	});
	jqid(this.searchInputId).keypress((e) => {
	    if(e.which==13) {
		this.doSearch();
	    }
	});
	let html = "";
	html+=table;
	jqid(this.div).html(html);
	this.jq('_addtranscription').click(function(){
	    _this.addOrEditTranscription($(this));
	});
	this.jq('_deletetranscription').click(() =>{
	    if(confirm('Are you sure you want to delete the entire transcription?')) {
		this.points=[];
		this.writeTranscription();
		this.makePoints();
	    }
	});
	jqid(this.div).find('.ramadda-media-point-delete').click(function(event) {
	    event.stopPropagation();
	    let index = +$(this).attr(ATTR_POINT_INDEX);
	    let point = _this.points[index];
	    if(!confirm('Are you sure you want to delete the transcription:' + point.title+'?')) return;
	    _this.points.splice(index,1);
	    _this.writeTranscription();
	    _this.makePoints();
	});
	jqid(this.div).find('.ramadda-media-point-edit').click(function(event) {
	    event.stopPropagation();
	    let index = +$(this).attr(ATTR_POINT_INDEX);
	    _this.addOrEditTranscription($(this),index);
	});

	jqid(this.div).find('.ramadda-media-point').click(function(){
	    let point = _this.points[+$(this).attr(ATTR_POINT_INDEX)];
	    let details = jqid(point.detailsId);
	    if(HU.isVisible(details)) {
		details.hide(300);
		details.attr('displayed',false);
	    } else {
		details.show(300);
		details.attr('displayed',true);
	    }
	});	    
	jqid(this.div).find('.ramadda-media-play').click(function() {
	    let time  = +$(this).attr('data-player-time');
	    if(_this.player.gotoTime) _this.player.gotoTime(time);
	});
    },

    addOrEditTranscription: function(widget, index) {
	this.closeEditDialog();
	let point;
	let add = false;
	if(this.player.pause) this.player.pause();
	if(!Utils.isDefined(index) || index<0) {
	    add=true;
	    point = {title:''};
	} else {
	    point = this.points[index];
	}

	let form = HU.open(TAG_TABLE,[ATTR_CLASS,'formtable']);
	form+=HU.formEntryLabel("Title",
				HU.input("",point.title,[ATTR_SIZE,60,
							 ATTR_ID,this.domId('edit_title')]));
	form+=HU.formEntryLabel("Synopsis",
				HU.textarea("",point.synopsis||"",[ATTR_ROWS,5,
								   ATTR_COLS,60,
								   ATTR_ID,this.domId('edit_synopsis')]));
	let timeSpan = HU.span([ATTR_ID,this.domId('_timespan')]);
	if(!add) {
	    form+=HU.formEntry("",HU.checkbox("",
					      [ATTR_ID,this.domId('edit_settime')],false,"Set time to: " +
					      timeSpan));
	} else {
	    form+=HU.formEntry("",Utils.msgLabel("Current time") + timeSpan);
	}
	form+=HU.close(TAG_TABLE);
	form+=HU.br();
	form+=HU.span([ATTR_ID,this.domId('edit_ok')],add?'Add Transcription':'Save Transcription');
	form+=HU.space(3);
	form+=HU.span([ATTR_ID,this.domId('edit_cancel')],LABEL_CANCEL);
	form=HU.div([ATTR_STYLE,HU.css(CSS_MARGIN,HU.px(10))], form);

	this.editDialog = HU.makeDialog({
	    content:form,
	    my:"left top",
	    at:"left bottom",
	    anchor:widget,
	    draggable:true,
	    header:true,
	    xinPlace:false});
	//getTime is async
	this.player.getTime(t=>{
	    this.jq('_timespan').html(this.formatTime(t));
	},true);


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
	let cb = time=>{
	    if(add || HU.isChecked(this.jq('edit_settime'))) {
		point.time = Math.floor(time);
	    }
	    this.closeEditDialog();
	    if(add) this.points.push(point);
	    this.writeTranscription();
	    this.makePoints();
	}
	this.player.getTime(cb);
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
	return {
	    pause: ()=>{
		player.pause();
	    },
	    getTime:cb=>{
		player.getCurrentTime().then(time=>{
		    cb(time);
		});
	    },
	    gotoTime:
	    (seconds)=>{
		player.setCurrentTime(seconds);
		try {
		    player.play();
		} catch {
		}}
	}
    },
    initSoundcloud:function(cnt) {
	if(!Utils.isDefined(cnt)) cnt=0;
	if(typeof SC == "undefined") {
	    if(cnt>10) {
		console.error("Error: Soundcloud api not loaded");
		return;
	    }
	    setTimeout(()=>{this.init(cnt+1);},1000);
	    return;
	};

	let iframe = document.querySelector('#' + this.id +' iframe');    
	let player = SC.Widget(iframe);
	return {
	    pause: ()=>{
		player.pause();
	    },
	    getTime: (cb)=>{
		player.getPosition((p)=> {
		    cb(p/1000);
		});
	    },
	    gotoTime:(seconds)=>{
		player.seekTo(seconds*1000);
		try {
		    player.play();
		} catch {
		}
	    }
	};
    },

    initMedia:function() {
	let player = this.myPlayer = document.querySelector('#' + this.attrs.mediaId);
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
	//	console.dir(player);
	return {gotoTime:gotoTime,
		getTime:(cb,debug)=> {
		    cb(player.currentTime);
		},
		pause:()=>{player.pause()}
	       };
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
	let args = {
            height: Utils.isDefined(this.attrs.height)?this.attrs.height:'390',
	    width: Utils.isDefined(this.attrs.width)?this.attrs.width:'100%',
//            width: Utils.isDefined(this.attrs.width)?this.attrs.width:'640',	    
            videoId: this.attrs.videoId,
            playerVars: playerVars,
            events: {
		onError: error=>{
		    console.dir('Error loading youtube video',error)
		}
	    }
	};
	let player = new YT.Player(this.id, args);
	return {
	    pause:()=>{
		player.pauseVideo();
	    },
 	    getTime: (cb)=>{cb(player.getCurrentTime?player.getCurrentTime():null);},
	    gotoTime:(seconds)=>{
		player.seekTo(seconds,true)
	    }
	};
    },

    formatTime:function(seconds) {
	if(isNaN(seconds)) return "NA";
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

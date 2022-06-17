
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
	if(typeof func == "function") {
	    this.gotoTime =  func;
	} else  {
	    this.gotoTime = func.gotoTime;
	    this.getTime = func.getTime;	    
	}
	   
	if(!this.points) this.points = [];
//	this.points = [{time:30,title:"30 seconds in"}]
	console.dir(this.points);
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
	});
	jqid(this.searchId).val("");
	jqid(this.searchId+"_results").html("");
    },
    doSearch:function() {
	let val =     jqid(this.searchId).val().trim();
	if(val=="") {
	    this.clearSearch();
	    return
	}
	let results = "";
	let regexp = new RegExp(val,'i');
	let showCount = 0;
	points.forEach((p,idx)=>{
	    let corpus = p.title +" " + p.transcript +" " + p.synopsis +"  "+ p.keywords +" " + p.subjects;
	    if(!corpus.match(regexp)) {
		jqid(p.rowId).hide();
		return;
	    }
	    showCount++;
	    jqid(p.rowId).show();
	});
	if(showCount==0) {
	    jqid(this.searchId+"_results").html("No results");
	} else {
	    jqid(this.searchId+"_results").html(showCount +" matched");
	}
    },

    makePoints:function() {
	this.points.sort((a,b)=>{
	    return a.time - b.time;
	});
	let table="<table cellspacing=0 cellpadding=0>";
	this.points.forEach((p,idx)=>{
	    let time = this.formatTime(p.time);
	    p.detailsId =  this.searchId +"_details_" + idx;
	    p.rowId = HU.getUniqueId("row_");
	    table+=HtmlUtils.tr(['id',p.rowId,'point-index',idx,'class','ramadda-clickable ramadda-ohms-point'], HU.td(['width','5%'], time) +
				HU.td(['width','95%'], HU.div([STYLE,HU.css('margin-left','10px')],p.title)));

	    let details =  HU.div([CLASS,'ramadda-clickable ramadda-ohms-play','data-player-time',p.time],
				  HU.getIconImage('fas fa-play') + ' ' +
				  'Play Segment' );

	    if(Utils.stringDefined(p.synopsis))
		details+=HU.div([],HU.b("Segment Synopsis: ") + p.synopsis);
	    if(Utils.stringDefined(p.keywords))
		details+=HU.div([],HU.b("Keywords: ") + p.keywords.replace(/;/g,"; "));
	    if(Utils.stringDefined(p.subjects))
		details+=HU.div([],HU.b("Subjects: ") + p.subjects.replace(/;/g,"; "));	
	    details = HU.div([CLASS,'ramadda-ohms-point-details-inner'],
			     details);
	    let detailsDiv = HU.div([ID,p.detailsId,CLASS,'ramadda-ohms-point-details',STYLE,HU.css('display','none')],details);
	    table+=HtmlUtils.tr([], HU.td(['colspan','2'], detailsDiv));
	});
	table+="</table>";
	table = HU.div([CLASS,"ramadda-ohms-points"], table);
	this.searchInputId = HU.getUniqueId("search_");
	let search = HU.input("","",['class','ramadda-ohms-search', "placeholder","Search",ID,this.searchInputId]) + " " + HU.span([ID,this.searchInputId+"_clear",CLASS,"ramadda-clickable"], HU.getIconImage('fas fa-eraser')) +
	    HU.div([ID,this.searchInputId+"_results",STYLE,HU.css('max-width','300px','overflow-x','auto')]) 

	search =HU.div([STYLE,HU.css('margin-left','10px')], search);
	let _this = this;
	jqid(this.searchId).html(search);
	jqid(this.searchInputId+"_clear").click((e) =>{
	    this.clearSearch();
	});
	jqid(this.searchInputId).keypress((e) => {
	    if(e.which==13) {
		this.doSearch(this.div, this.searchInputId,playFunction,points);
	    }
	});
	let html = "";
	if(this.getProperty("canAddTranscription") && this.getTime) {
	    html+=HU.div(['title','Add transcription','id',this.domId('_addtranscription'),'class','ramadda-clickable'], HU.getIconImage('fas fa-plus'));
	}

	html+=table;
	jqid(this.div).html(html);
	this.jq('_addtranscription').click(()=>{
	    this.addTranscription();
	});
	jqid(this.div).find('.ramadda-ohms-point').click(function(){
	    let point = _this.points[+$(this).attr('point-index')];
	    let details = jqid(point.detailsId);
	    if(details.is(':visible')) {
		details.hide(300);
	    } else {
		details.show(300);
	    }
	});	    
	jqid(this.div).find('.ramadda-ohms-play').click(function() {
	    let time  = +$(this).attr('data-player-time');
	    if(_this.gotoTime) _this.gotoTime(time);
	});
    },

    addTranscription: function() {
	let title = prompt('Transcription title:');
	if(!title) return;
	this.points.push({
	    title:title,
	    time:Math.floor(this.getTime())
	})
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
	this.makePoints();
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

	//player.getCurrentTime
	return (seconds)=>{
	    player.seekTo(seconds,true);
	};
    },

    formatTime:function(seconds) {
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

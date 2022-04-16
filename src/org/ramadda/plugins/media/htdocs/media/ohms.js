

function ohmsFormatTime(seconds) {
    seconds = +seconds;
    let hours = Math.floor(seconds/3600);
    seconds-=hours*3600;
    let minutes = Math.floor(seconds/60);
    seconds -= minutes*60;
    let time = Utils.padLeft(""+hours,2,"0") +":" + Utils.padLeft(""+minutes,2,"0")+
	":"  + Utils.padLeft(""+seconds,2,"0");
    return time;
}

function ohmsClearSearch(searchId) {
    jqid(searchId).val("");
    jqid(searchId+"_results").html("");
}


function ohmsDoSearch(divId,searchId,playFunction,points) {
    let val =     jqid(searchId).val();
    let results = "";
    let regexp = new RegExp(val,'i');
    let table = null;
    points.forEach((p,idx)=>{
	let corpus = p.title +" " + p.transcript +" " + p.synopsis +"  "+ p.keywords +" " + p.subjects;

	if(!corpus.match(regexp)) {
	    return;
	}
	if(!table) table="<table width=100%>";
	let time = ohmsFormatTime(p.time);
	table+=HtmlUtils.tr(['point-index',idx,'class','ramadda-clickable ramadda-ohms-point'], HU.td(['width','5%'], time) +
			    HU.td(['width','95%'], HU.div([STYLE,HU.css('margin-left','10px')],p.title)));

    });
    if(!table) {
	jqid(searchId+"_results").html("No results");
	return;
    }
    jqid(searchId+"_results").html(table);
    jqid(searchId+"_results").find('.ramadda-clickable').click(function() {
	let point = points[+$(this).attr('point-index')];
	let details = jqid(point.detailsId);
	jqid(divId).find('.ramadda-ohms-point-details').hide();
	details.show();
	playFunction(point.time);
    });

}

function ohmsMakePoints(div,points,searchId, playFunction) {
    let table="<table cellspacing=0 cellpadding=0>";
    points.forEach((p,idx)=>{
	let time = ohmsFormatTime(p.time);
	p.detailsId =  searchId +"_details_" + idx;
	table+=HtmlUtils.tr(['point-index',idx,'class','ramadda-clickable ramadda-ohms-point'], HU.td(['width','5%'], time) +
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
    let searchInputId = HU.getUniqueId("search_");
    let search = HU.input("","",['class','ramadda-ohms-search', "placeholder","Search",ID,searchInputId]) + " " + HU.span([ID,searchInputId+"_clear",CLASS,"ramadda-clickable"], HU.getIconImage('fas fa-eraser')) +
	HU.div([ID,searchInputId+"_results"]) 

    search =HU.div([STYLE,HU.css('margin-left','10px')], search);
    jqid(searchId).html(search);
    jqid(searchInputId+"_clear").click(function (e) {
	ohmsClearSearch(searchInputId);
    });
    jqid(searchInputId).keypress(function (e) {
	if(e.which==13) {
	    ohmsDoSearch(div, searchInputId,playFunction,points);
	}
    });
    let html = table;
    jqid(div).html(html);
    jqid(div).find('.ramadda-ohms-point').click(function(){
	let point = points[+$(this).attr('point-index')];
	let details = jqid(point.detailsId);
	if(details.is(':visible')) {
	    details.hide(300);
	} else {
	    details.show(300);
	}
    });	    
    jqid(div).find('.ramadda-ohms-play').click(function() {
	let time  = +$(this).attr('data-player-time');
	if(playFunction) playFunction(time);
    });
}    



function ohmsInitVimeo(id,div,points,searchId) {
    let iframe = document.querySelector('#' + id +' iframe');    
    let player = new Vimeo.Player(iframe);
    ohmsMakePoints(div,points,searchId,(seconds)=>{
	player.setCurrentTime(seconds);
	try {
	    player.play();
	} catch {
	}
    });
}



function ohmsInitSoundcloud(id,div,points,searchId) {
    let iframe = document.querySelector('#' + id +' iframe');    
    let player = SC.Widget(iframe);
    ohmsMakePoints(div,points,searchId,(seconds)=>{
	player.seekTo(seconds*1000);
	try {
	    player.play();
	} catch {
	}
    });
}


function ohmsInitMedia(id,div,points,searchId) {
    let player = document.querySelector('#' + id);
    ohmsMakePoints(div,points,searchId,(seconds)=>{
	player.fastSeek(seconds);
	try {
	    player.play();
	} catch {
	}
    });
}






let ohmsState = {
    youtubeState:[]
}

function ohmsInitYouTube(videoId, id, pointsDiv,points,searchId) {
    if(!ohmsState.youtubeReady) {
	ohmsState.youtubeState.push({id:id,videoId:videoId, pointsDiv:pointsDiv,points:points,searchId:searchId});
	return;
    }

    let player = new YT.Player(id, {
        height: '390',
        width: '640',
        videoId: videoId,
        playerVars: {
            'playsinline': 1
        },
        events: {
        }
    });

    ohmsMakePoints(pointsDiv,points,searchId,(seconds)=>{
	player.seekTo(seconds,true);
    });
}


function onYouTubeIframeAPIReady() {
    ohmsState.youtubeReady = true;
    ohmsState.youtubeState.forEach(s=>{
	ohmsInitYouTube(s.videoId,s.id,s.pointsDiv,s.points,s.searchId);
    });
}





function ohmsMakePoints(div,points,playFunction) {
    let table="<table cellspacing=0 cellpadding=0>";
    points.forEach(p=>{
	let seconds = p.time;
	let hours = Math.floor(seconds/3600);
	seconds-=hours*3600;
	let minutes = Math.floor(seconds/60);
	seconds -= minutes*60;
	let time = Utils.padLeft(""+hours,2,"0") +":" + Utils.padLeft(""+minutes,2,"0")+
	    ":"  + Utils.padLeft(""+seconds,2,"0");
	let detailsId= HU.getUniqueId("point_");
	table+=HtmlUtils.tr(['details-id',detailsId,'class','ramadda-clickable ramadda-ohms-point'], HU.td(['width','5%'], time) +
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
	details = HU.div([CLASS,'ramadda-ohms-point-details'],
			 details);
	let detailsDiv = HU.div(['class','ramadda-ohms-point-details',STYLE,HU.css("display","none"), ID,detailsId],details);
	table+=HtmlUtils.tr([], HU.td(['colspan','2'], detailsDiv));
    });
    table+="</table>";
    let html = HU.div([CLASS,"ramadda-ohms-points"], table);
    jqid(div).html(html);
    jqid(div).find('.ramadda-ohms-point').click(function(){
	let details = jqid($(this).attr('details-id'));
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



function ohmsInitVimeo(id,div,points) {
    let iframe = document.querySelector('#' + id +' iframe');    
    let player = new Vimeo.Player(iframe);
    ohmsMakePoints(div,points,(seconds)=>{
	player.setCurrentTime(seconds);
	try {
	    player.play();
	} catch {
	}
    });
}



function ohmsInitSoundcloud(id,div,points) {
    let iframe = document.querySelector('#' + id +' iframe');    
    let player = SC.Widget(iframe);
    ohmsMakePoints(div,points,(seconds)=>{
	player.seekTo(seconds*1000);
	try {
	    player.play();
	} catch {
	}
    });
}


function ohmsInitMedia(id,div,points) {
    let player = document.querySelector('#' + id);
    ohmsMakePoints(div,points,(seconds)=>{
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

function ohmsInitYouTube(videoId, id, pointsDiv,points) {
    if(!ohmsState.youtubeReady) {
	ohmsState.youtubeState.push({id:id,videoId:videoId, pointsDiv:pointsDiv,points:points});
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

    ohmsMakePoints(pointsDiv,points,(seconds)=>{
	player.seekTo(seconds,true);
    });
}


function onYouTubeIframeAPIReady() {
    ohmsState.youtubeReady = true;
    ohmsState.youtubeState.forEach(s=>{
	ohmsInitYouTube(s.videoId,s.id,s.pointsDiv,s.points);
    });
}

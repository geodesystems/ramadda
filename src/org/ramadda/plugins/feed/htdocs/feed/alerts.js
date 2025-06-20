function NwsAlerts(div,opts) {
    this.opts={};
    this.div = div;
    $.extend(this.opts,{
	debug:false,
	all:false,
	alertsUnique:false,
	urls:[],
	zones:[],
	areas:[],
	points:[],
	title:'NWS Alerts',
	noAlertsMessage:'There are currently no alerts'
    });
    if(opts) $.extend(this.opts,opts);
    this.init();
}

NwsAlerts.prototype = {
    base:'https://api.weather.gov/alerts/active',
    getProperty:function(key,dflt) {
	if(!Utils.isDefined(this.opts[key])) return dflt;
	return this.opts[key];
    },
    getFullProperty:function(key,type,value,dflt) {
	//key.value key.type key
	return this.getProperty(key+'.'+value,
				this.getProperty(key+'.'+type,
						 this.getProperty(key,dflt)));
    },
    addHeader:function(msg)  {
	if(this.seenHeader[msg]) return;
	this.seenHeader[msg] = true;
	msg = HU.div([ATTR_CLASS,'alerts-header'],msg);
	jqid(this.headerID).append(msg);
    },
    debug:function(msg) {
	if(this.opts.debug) console.log(msg);
    },
    writeEvents:function() {
	let txt='<table>\n';
	txt+=HU.tr([],HU.td([],HU.b('ID'))+
		   HU.td([ATTR_STYLE,HU.css('padding-left','1em','padding-right','1em')],HU.b('Event Type'))+HU.td([],HU.b('Description'))) +'\n';
	Object.keys(this.phenomena).forEach(key=>{
	    let info = this.phenomena[key];
	    txt+=HU.tr(['class','search-component'],
		       HU.td([ATTR_CLASS,'eventtype'],key)+
		       HU.td([ATTR_CLASS,'eventtype',ATTR_STYLE,HU.css('padding-left','1em','padding-right','1em')],info.type??'')+HU.td(['align','left'],info.label)) +'\n';
	});
	txt+='</table>';
	Utils.makeDownloadFile('wx.html',txt);

    },

    jq:function(id) {
	return jqid(this.domId(id));

    },
    domId:function(id) {
	return this.uid+id;
    },
    getDiv:function(type) {
	if(type)
	    return this.jq(type);
	return jqid(this.div);
    },
    init:function() {
//	this.writeEvents();
	this.uid =HU.getUniqueId();
	this.headerID =HU.getUniqueId();
	let loading = HU.center(
	    HU.image(RamaddaUtil.getCdnUrl('/icons/mapprogress.gif'),[ATTR_STYLE,'width:50px;'])+
		HU.div('Loading alerts'));
	this.getDiv().append(HU.div([ATTR_ID,this.domId('progress')],loading));
	this.getDiv().append(HU.div([ATTR_ID,this.headerID],''));
	this.getDiv().append(HU.div([ATTR_ID,this.domId('point')],''));	
	this.getDiv().append(HU.div([ATTR_ID,this.domId('zone')],''));
	this.getDiv().append(HU.div([ATTR_ID,this.domId('area')],''));		

	this.seenHeader={};
	this.allUrls = []
	if(this.opts.all) this.allUrls.push({url:this.base});
	let addUrl=(type,value) =>{
	    if(!Utils.stringDefined(value) || value.startsWith('#')) return;
	    this.allUrls.push({url:this.base+'?' + type+'='+ value,type:type,value:value});
	}

	this.allUrls = Utils.mergeLists(this.allUrls,this.opts.urls);
	this.opts.zones.forEach(zone=>{
	    addUrl('zone',zone);
	});
	this.opts.areas.forEach(area=>{
	    addUrl('area',area);
	});
	this.opts.points.forEach(point=>{
	    addUrl('point',point);
	});	
	this.loadedCount=0;
	this.alertCount=0;	
	if(this.allUrls.length==0) {
	    this.finish();
	}
	this.seen={};
	this.pending = 0;
	this.tbdUrls = [];

//	setTimeout(()=>{
	this.allUrls.forEach(item=>{
	    if(Utils.stringDefined(item.url)) {
		let loadFirst = Utils.getProperty(this.getFullProperty('loadFirst',item.type,item.value,true));
		if(!loadFirst) {
		    this.tbdUrls.push(item);
		    return;
		}
		this.pending++;
		this.loadAlert(item.url,item.type,item.value,this.allUrls.length>1);
	    }
	});
	//now check if there are any second phase urls to load
	if(this.tbdUrls.length) {
	    let check = ()=>{
		if(this.pending>0) {
		    setTimeout(check,50);
		    return;
		}
		this.tbdUrls.forEach(item=>{
		    this.loadAlert(item.url,item.type,item.value,this.allUrls.length>1);
		});
	    }
	    setTimeout(check,50);
	}
//	},2000);

    },
    loadAlert:function(url,type,value,multiples) {    
	if(!Utils.stringDefined(url)) return;
	let  collapseShortLines= function(input, maxLength = 100) {
	    return input;
	    return input.replace(/\n(?=([^\n]{1,100})(\n|$))/g, (_, match) => {
		return match.length < maxLength ? '' : '\n';
	    });
	}
        $.getJSON(url, data=>{
	    this.pending--;
	    this.loadedCount++;
	    let now = new Date();
	    let innerContents = '';
	    if(data.features.length==0) {
		this.finish();
		console.log('no events for:',type,value,url)
		return;
		//		innerContents+=this.noAlertsMessage;
	    }
	    let warnings = [];
	    let watches = [];
	    let other = [];
	    let myAlertCount=0;
	    data.features.forEach((alert,idx)=>{
		let props = alert.properties;
		if(this.seen[props.id] && this.opts.alertsUnique) {
		    return;
		}
		this.seen[props.id]=true;
		myAlertCount++;
		let pre = props.description??'';
		pre = pre.replace(/^\* */gm,'&bull; ');
		pre = pre.replace(/(HEALTH INFORMATION|WHAT|WHERE|WHEN|IMPACTS)\.\.\./g,"<b>$1</b>: ");
//		pre = collapseShortLines(pre);
		pre = pre.replace(/(HEALTH INFORMATION|WHAT|WHERE|WHEN|IMPACTS)/g,(_,match)=>{return Utils.camelCase(match);});
		if(Utils.stringDefined(props.instruction)) {
		    pre+='\n\n' + '&bull; '+ HU.b('Instructions')+': ';
		    pre+=props.instruction;
		}
		if(Utils.stringDefined(props.areaDesc)) {
		    pre+='\n\n' + '&bull; '+ HU.b('Area')+': ';
		    pre+=props.areaDesc;
		}		
		let title = props.headline;
		let significance='';
		let phenon='';
		let code='';
		if(props?.eventCode?.NationalWeatherService?.length) {
		    code = props.eventCode.NationalWeatherService[0];
		    phenon = code.substring(0,2);
		    significance = code.substring(2,3);		    
		    let info = this.phenomena[phenon]??{};
		    let icon = info.emoji;
		    if(icon) {
			if(icon.startsWith('/'))
			    icon = HU.image(RamaddaUtils.getUrl(icon));
			else if(icon.startsWith('fa'))
			    icon = HU.getIconImage(icon);
			title = HU.span([ATTR_CLASS,'alert-icon'], icon)+' '+ title;
		    } else {
			//console.log(code,phenon,significance);
			title = phenon +' '+ title;
		    }

		    this.debug('alert: type='+ type +' value:' + value +' phenomena:' + phenon +(info.label?' ' + info.label:'') +' event:'+ props.event);
		    let headerMessage=this.getProperty('headerMessage.'+phenon,
						       this.getProperty('headerMessage.'+info.type,
									this.getProperty('headerMessage',null)));
		    if(headerMessage) this.addHeader(headerMessage);
		    let expires = props.expires?new Date(props.expires):null;
		    let sent = new Date(props.sent);
		    //		    console.log(Utils.formatDate(sent) +" " + sent);
		}
		pre = pre.trim();
		pre = pre.replace(/\n\n/g,'<p>');
		let contents = '';
		if(props?.geocode?.UGC) {
		    let zone = props.geocode.UGC['0'];
		    if(zone) {
			let link = HU.href('https://forecast.weather.gov/MapClick.php?zoneid='+zone,'View Forecast',['target','_forecast']);
			contents += HU.center(link);
		    }
		}
		contents += pre;
		contents = HU.div([ATTR_CLASS,'alerts-contents'],contents);
		


//		contents += HU.pre([],pre);
		let l = (significance=='W'?warnings:(significance=='A'?watches:other));
		if(code=='SPS') l=warnings;

		l.push({significance:significance,
			label:title,contents:contents});
		this.alertCount++;
		//		console.log(props.severity,props.certainty);
	    });
	    if(myAlertCount==0) {
		this.finish();
		return;
	    }
	    let list = Utils.mergeLists(warnings,watches,other);
	    let accordion;
	    let label = this.getFullProperty('alertLabel',type,value,data.title);
	    if(list.length>0) {
		accordion = HU.makeAccordionHtml(list);
		innerContents+=accordion.contents;
		if(list.length>1)
		    label +=  ' - ' + list.length+' alerts';
		else if(list.length==1)
		    label +=  ' - ' + list.length+' alert';
	    }
	    let html;
	    let showToggle = this.getFullProperty('showToggle',type,value,multiples);
	    if(showToggle) {
		let toggleOpen = this.getFullProperty('toggleOpen',type,value,multiples);
		label = HU.span([ATTR_CLASS,'alert-header'],label);
		html = HU.toggleBlock(label,innerContents,toggleOpen);
	    }     else {
		html = HU.div([ATTR_CLASS,'alert-header'],label)+innerContents;
	    }
	    this.getDiv(type).append(html);
	    if(accordion) {
		accordion.init({active:-1,xdecorate:true});
	    }
	    this.finish();
	}).fail(data=>{
	    this.pending--;
	    this.loadedCount++;
	    this.getDiv().append(HU.div('NWS Alerts: failed to read alert: ' + url));
	    console.error('NWS Alerts: failed to read alert: ' +url);
	    console.error(data.responseText);
	    this.finish();
	});
    },
    finish:function() {
	this.jq('progress').hide();
	if(this.allUrls.length!=this.loadedCount) return;
	if(this.alertCount==0) {
	    this.getDiv().html(this.opts.noAlertsMessage);
	}
    },
    significance: {
	"W": "Warning",
	"A": "Watch",
	"Y": "Advisory",
	"S": "Statement",
	"F": "Forecast",
	"O": "Outlook",
	"N": "Synopsis",
	"R": "Record"
    },
    phenomena: {
	"AQ": {
	    "label": "Air Quality",
	    "emoji": "\ud83c\udfed"
	},
	"AV": {
	    "label": "Avalanche",
	    "emoji": "\ud83c\udfd4\ufe0f"
	},
	"BH": {
	    "label": "Beach Hazards",
	    "emoji": "\ud83c\udfd6\ufe0f"
	},
	"BZ": {
	    "label": "Blizzard",
	    "emoji": "\ud83c\udf28\ufe0f"
	},
	"SP": {
	    "label":"Special Weather",
	    "emoji": "\u26a0\ufe0f"
	},

	"DS": {
	    "label": "Dust Storm",
	    "emoji": "\ud83c\udf2a\ufe0f"
	},
	"DU": {
	    "label": "Blowing Dust",
	    "emoji": "\ud83c\udf2b\ufe0f"
	},
	"EC": {
	    type:'cold',
	    "label": "Extreme Cold",
	    "emoji": "\u2744\ufe0f"
	},
	"EH": {
	    type:'heat',
	    "label": "Excessive Heat",
	    "emoji": "\ud83c\udf21\ufe0f"
	},
	"XH": {
	    type:'heat',
	    "label": "Extreme Heat",
	    "emoji": "\ud83c\udf21\ufe0f"
	},
	"HT": {
	    type:'heat',
	    "label": "Heat",
	    "emoji": "\ud83d\udd25"
	},
	"CF": {
	    type:'flood',
	    "label": "Coastal Flood",
	    "emoji": "\ud83c\udf0a"
	},
	"FA": {
	    type:'flood',
	    "label": "Areal Flood",
	    "emoji": "\ud83c\udf0a"
	},
	"FF": {
	    type:'flood',
	    "label": "Flash Flood",
	    "emoji": "\ud83d\udea8\ud83c\udf0a"
	},
	"FL": {
	    type:'flood',
	    "label": "Flood",
	    "emoji": "\ud83c\udf0a"
	},
	"LS": {
	    type:'flood',
	    "label": "Lakeshore Flood",
	    "emoji": "\ud83c\udf0a"
	},
	"UW": {
	    type:'flood',
	    "label": "Urban and Small Stream Flood",
	    "emoji": "\ud83c\udf0a"
	},
	"FG": {
	    "label": "Dense Fog",
	    "emoji": "\ud83c\udf2b\ufe0f"
	},
	"FR": {
	    "label": "Frost",
	    "emoji": "\ud83c\udf2b\ufe0f"
	},
	"FW": {
	    "label": "Fire Weather",
	    "emoji": "\ud83d\udd25"
	},
	"AF": {
	    "label": "Ashfall",
	    "emoji": "\ud83c\udf0b"
	},

	"FZ": {
	    "label": "Freeze",
	    "emoji": "\ud83e\uddca"
	},
	"GL": {
	    "label": "Gale",
	    "emoji": "\ud83c\udf2c\ufe0f"
	},

	"HF": {
	    type:'wind',
	    "label": "Hurricane Force Wind",
	    "emoji": "\ud83c\udf00"
	},
	"HU": {
	    "label": "Hurricane",
	    "emoji": "\ud83c\udf00"
	},

	"HZ": {
	    "label": "Hard Freeze",
	    "emoji": "\ud83e\udd76"
	},
	"IP": {
	    "label": "Sleet",
	    "emoji": "\ud83c\udf27\ufe0f"
	},
	"IS": {
	    "label": "Ice Storm",
	    "emoji": "\ud83e\uddca"
	},
	"LB": {
	    "label": "Lake Effect Snow and Blowing Snow",
	    "emoji": "\ud83c\udf28\ufe0f\ud83d\udca8"
	},
	"LE": {
	    "label": "Lake Effect Snow",
	    "emoji": "\ud83c\udf28\ufe0f"
	},
	"LO": {
	    "label": "Low Water",
	    "emoji": "\ud83d\udca7"
	},
	"MA": {
	    "label": "Marine",
	    "emoji": "\ud83d\udea2"
	},
	"RB": {
	    "label": "Small Craft for Rough Bar",
	    "emoji": "\u26f5"
	},
	"RD": {
	    "label": "Radiological Hazard",
	    "emoji": "\u2622\ufe0f"
	},
	"RF": {
	    "label": "Small Craft for Rough Waters",
	    "emoji": "\ud83c\udf0a"
	},
	"SC": {
	    "label": "Small Craft",
	    "emoji": "\u26f5"
	},
	"SE": {
	    "label": "Hazardous Seas",
	    "emoji": "\ud83c\udf0a"
	},
	"SI": {
	    type:'wind',
	    "label": "Small Craft for Winds",
	    "emoji": "\ud83d\udca8"
	},
	"SM": {
	    "label": "Dense Smoke",
	    "emoji": "\ud83c\udf2b\ufe0f"
	},
	"SN": {
	    "label": "Snow",
	    "emoji": "\u2744\ufe0f"
	},
	"SQ": {
	    "label": "Snow Squall",
	    "emoji": "\ud83c\udf28\ufe0f"
	},
	"SR": {
	    "label": "Storm Surge",
	    "emoji": "\ud83c\udf0a"
	},
	"SS": {
	    "label": "Storm Surge",
	    "emoji": "\ud83c\udf0a"
	},
	"SU": {
	    "label": "Blowing Snow",
	    "emoji": "\ud83d\udca8"
	},
	"SV": {
	    "label": "Severe Thunderstorm",
	    "emoji": "\u26c8\ufe0f"
	},
	"SW": {
	    "label": "Small Craft for Hazardous Seas",
	    "emoji": "\ud83d\udea4"
	},
	"TO": {
	    "label": "Tornado",
	    "emoji": "\ud83c\udf2a\ufe0f"
	},
	"TR": {
	    "label": "Tropical Storm",
	    "emoji": "\ud83c\udf00"
	},
	"TS": {
	    "label": "Tsunami",
	    "emoji": "\ud83c\udf0a"
	},
	"TY": {
	    "label": "Typhoon",
	    "emoji": "\ud83c\udf00"
	},
	"UP": {
	    "label": "Unknown Precipitation",
	    "emoji": "\u2753"
	},
	"VO": {
	    "label": "Volcano",
	    "emoji": "\ud83c\udf0b"
	},
	"WC": {
	    "label": "Wind Chill",
	    "emoji": "\ud83e\udd76"
	},
	"WI": {
	    type:'wind',
	    "label": "Wind",
	    "emoji": "\ud83d\udca8"
	},
	"HW": {
	    type:'wind',
	    "label": "High Wind",
	    "emoji": "\ud83c\udf2c\ufe0f"
	},
	"BW": {
	    type:'wind',
	    "label": "Brisk Wind",
	    "emoji": "\ud83d\udca8"
	},
	"EW": {
	    type:'wind',
	    "label": "Extreme Wind",
	    "emoji": "\ud83c\udf2c\ufe0f"
	},

	"WS": {
	    type:'cold',
	    "label": "Winter Storm",
	    "emoji": "\ud83c\udf28\ufe0f"
	},
	"WW": {
	    type:'cold',
	    "label": "Winter Weather",
	    "emoji": "\u2744\ufe0f"
	},
	"ZF": {
	    "label": "Freezing Fog",
	    "emoji": "\ud83c\udf2b\ufe0f\u2744\ufe0f"
	},
	"ZR": {
	    "label": "Freezing Rain",
	    "emoji": "\ud83c\udf27\ufe0f\u2744\ufe0f"
	}
    }
}



var Translate = {
    init: function() {
//	$('.ramadda-links-extra').append('x');
	Translate.translate();
    },
    packs:{},
    missing:{},
    loadPack: function(lang, callback) {
	if(Translate.packs[lang]) {
	    callback(Translate.packs[lang]);
	    return;
	}

	let url  = RamaddaUtil.getUrl('/languages/' + lang+'.pack');
//	console.dir(url);
        $.ajax({
            url: url,
            dataType: 'text',
            success: function(data) {
		Translate.packs[lang] =Translate.makePack(data);
		callback(Translate.packs[lang]);
            }}).fail(function() {
		console.log("Failed to load url: " + url);
		Translate.packs[lang] ={};
            });
    },
    makePack:function(c) {
	let pack = {};
	if(!c) return pack;
	Utils.split(c,"\n",true,true).forEach(line=>{
	    line = line.replace(/\"/g,"");
	    if(line.startsWith('#')) return;
	    let i = line.indexOf("=");
	    if(i<0) return;
	    let key = line.substring(0,i).trim();
	    let value = line.substring(i+1).trim();	    
	    pack[key] = value;
	});
	return pack;
    },
    translate: function(selector,lang) {
	lang = lang || ramaddaLanguage || navigator.language 
            || navigator.userLanguage;
	if(!Utils.stringDefined(lang)) {
	    console.log('no language:' + lang);
	    return;
	}
	lang = lang.toLowerCase();
	Translate.loadPack(lang,(pack)=>{
	    Translate.translateInner(selector, lang,pack);
	    
	});
    },
    translateInner: function(selector,lang,pack) {
	if(!pack) {
	    lang = lang.replace(/-.*/,'');
	}
	pack = this.packs[lang];
	if(!pack) {
	    console.log('no language pack:' + lang);
	    return;
	}
	let all;
	if(selector) all = $(selector).find('*');
	else  all = $('*');
	let langFlag = (suffix) =>{
	    return 'lang-orig-' + (suffix??'');
	}

	let ok = (tag,t,suffix)=>{
	    if(tag.hasClass('ramadda-notranslate')) {
		return false;
	    }
	    if(tag.attr(langFlag(suffix))) {
		return false;
	    }
	    if(!t || t.indexOf('<')>=0) {
		return false;
	    }
	    return true;
	}
	let get = (a,t,suffix)=>{
	    if(!ok(a,t,suffix)) {
		return;
	    }
	    if(pack[t]) {
		if(pack[t]=='<skip>') return null;
		a.attr(langFlag(suffix),t);
		return pack[t];
	    }
//	    console.log('missing',t);
	    if(t=='type_datafile_json') {
//		console.log(t,suffix,a.prop('tagName'));
	    }
	    Translate.missing[t] = true;
	    return null;
	}
	let skip = {'SCRIPT':true,'BR':true,'HTML':true,'STYLE':true,'TEXTAREA':true};
	let skip2 = {'SCRIPT':true,'BR':true,'HTML':true,'STYLE':true,'TEXTAREA':true};	
	let attrs = ['placeholder','title','value'];
	all.each(function() {
	    let v;
	    let a = $(this);
	    let tag = a.prop('tagName');
	    if(skip[tag]) {
		return;
	    }
	    attrs.forEach(attr=>{
		if(attr=='value') {
		    //only handle value attr for submit
		    if(tag=='INPUT' && a.attr('type')=='submit') {
		    } else {
			return;
		    }
		}

		if(a.attr(attr)) {
		    v = get(a,a.attr(attr),attr);
		    if(v) {
			a.attr(attr,v);
		    }
		}});

	    v = get(a,a.html());
	    if(v) {
//		console.log(tag,a.html());
		a.html(v);
	    }
	});
    },
    showMissing: function() {
	let missing = '';
	Object.keys(Translate.missing).forEach(key=>{
	    missing+=key+'\n';
	});
	console.log(missing);
	Utils.makeDownloadFile('missing.txt',missing);
    }
};




$( document ).ready(function() {
    Translate.init();
});
	 


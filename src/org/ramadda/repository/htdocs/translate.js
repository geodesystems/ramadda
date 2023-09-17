var LANGUAGE_ENGLISH = 'en';
var LANGUAGE_SPANISH = 'es';

var Translate = {
    init: function() {
	this.switchPrefix = HU.getIconImage('fas fa-language') +HU.space(1)
	ramaddaLanguages.forEach(lang=>{
	    $('#ramadda_user_menu').append(HU.div(['data-language',lang.id,
						   ATTR_TITLE,'Switch language',
						   ATTR_CLASS,'ramadda-clickable ramadda-language-switch ramadda-user-link'],this.switchPrefix+lang.label));
	});
	this.switchLang = jqid('switchlang');
	let _this = this;
	$('.ramadda-language-switch').click(function() {
	    let lang = $(this).attr('data-language');
            HU.hidePopupObject();
	    _this.setLanguage(lang);
	    if(lang==LANGUAGE_ENGLISH) {
		Translate.translateInner(null, LANGUAGE_ENGLISH,{},true);
	    }   else {
		Translate.translate(null,lang);
	    }
	});
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
    setLanguage: function(lang) {
	this.language = ramaddaLanguage  =lang;
//	console.log('language:'+this.language);
    },
    translate: function(selector,lang) {
	if(!lang) {
	    lang = this.language || ramaddaLanguage || navigator?.language 
		|| navigator?.userLanguage;
	}
	if(!Utils.stringDefined(lang)) {
	    console.log('no language:' + lang);
	    return;
	}
	lang = lang.toLowerCase();
	lang = lang.replace(/-.*/,'');
	this.setLanguage(lang);
	if(lang==LANGUAGE_ENGLISH) {
	    Translate.translateInner(selector, lang,{},true);
	    return;
	}

	Translate.loadPack(lang,(pack)=>{
	    Translate.translateInner(selector, lang,pack);
	    
	});
    },
    haveDoneAnyTranslations:false,
    translateInner: function(selector,lang,pack,useDflt) {
	let all;
	let blocks;
	if(selector) {
	    all = $(selector).find('*');	   
	    blocks = $(selector).find('.ramadda-language-block');	    
	} else {
	    all = $('*');
	    blocks = $('.ramadda-language-block');	    
	}	    

	blocks.each(function() {
	    if($(this).attr('data-lang')==lang) {
		$(this).show();
	    } else {
		$(this).hide();
	    }
	});

	if(useDflt && !this.haveDoneAnyTranslations) {
	    return
	}
//	console.log('translating to:' + lang +' use dflt:' + useDflt);
	this.haveDoneAnyTranslations=true;
	if(!pack) {
	    console.log('no language pack:' + lang);
	    return;
	}
	let langFlag = (suffix) =>{
	    return 'lang-orig-' + (suffix??'');
	}

	let ok = (tag,t,suffix)=>{
	    if(tag.hasClass('ramadda-notranslate')) {
		return false;
	    }
	    if(tag.attr(langFlag(suffix))) {
//		return false;
	    }
	    if(!t || t.indexOf('<')>=0) {
		return false;
	    }
	    return true;
	}
	let get = (a,t,suffix,debug)=>{


	    if(useDflt) {
		let orig = a.attr(langFlag(suffix));
		if(orig) return orig;
		return null;
	    }
	    if(!ok(a,t,suffix)) {
		return;
	    }


	    let tag = a.prop('tagName');
	    if(pack[t]) {
		if(pack[t]=='<skip>') return null;
		a.attr(langFlag(suffix),t);
//		console.log(langFlag(suffix),a.attr(langFlag(suffix)));
		return pack[t];
	    }
	    Translate.missing[t] = true;
	    return null;
	}
	let skip = {'SCRIPT':true,'BR':true,'HTML':true,'STYLE':true,'TEXTAREA':true,'HEAD':true,'META':true,'LINK':true,'BODY':true};
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


	    v = get(a,a.html(),null,false);
	    if(v) {
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

if(ramaddaLanguagesEnabled) {
    $( document ).ready(function() {
	Translate.init();
    });
}
	 


var LANGUAGE_ENGLISH = 'en';
var LANGUAGE_SPANISH = 'es';

var Translate = {
    packs:{},
    missing:{},
    init: function() {
	this.switchPrefix = HU.getIconImage('fas fa-language') +HU.space(1);
	let menu = HU.span([ATTR_TITLE,'Change language',
			    ATTR_CLASS,CLASS_CLICKABLE,ATTR_ID,'ramadda_language_menu'],HU.getIconImage('fas fa-language'));
	menu = $(menu).appendTo(jqid('ramadda_links_prefix'));
	
	menu.click(()=>{
	    let html = '';
	    html+= HU.div([ATTR_TITLE,'Clear language',
			   ATTR_CLASS,'ramadda-clickable ramadda-language-switch ramadda-menu-language-switch ramadda-user-link'],this.switchPrefix+'Clear');

	    ramaddaLanguages.forEach(lang=>{
		html+= HU.div(['data-language',lang.id,
			       ATTR_TITLE,'Switch language',
			       ATTR_CLASS,'ramadda-clickable ramadda-language-switch ramadda-menu-language-switch ramadda-user-link'],this.switchPrefix+lang.label);
	    });
	    html = HU.div([],html);
	    if(this.menuPopup)
		this.menuPopup.remove();
	    
	    this.menuPopup = HU.makeDialog({content:html,anchor:menu,my:'right top',at:'right bottom'});
	    let _this = this;
	    this.menuPopup.find('.ramadda-menu-language-switch').click(function() {
		_this.switcherClicked($(this));
	    });
	});
	this.switchLang = jqid('switchlang');
	Translate.translate();
    },
    switcherClicked:function(link) {
	let lang = link.attr('data-language');
        HU.hidePopupObject();
	this.setLanguage(lang);
//	Utils.setLocalStorage('ramadda-language', lang==LANGUAGE_ENGLISH?null:lang);
	Utils.setLocalStorage('ramadda-language', lang);
	if(lang==LANGUAGE_ENGLISH) {
	    Translate.translateInner(null, LANGUAGE_ENGLISH,{},true);
	}   else {
	    Translate.translate(null,lang);
	}
    },

    setDefaultLanguage:function(lang) {
	this.defaultLanguage = lang?lang:LANGUAGE_ENGLISH;
    },
    addSwitcher:function(id,langs) {
	if(langs) langs=Utils.split(langs,",",true,true);
	let html = '<div>';
	ramaddaLanguages.forEach(lang=>{
	    if(langs && langs.length && !langs.includes(lang.id)) return;
	    html+= HU.span(['data-language',lang.id,
			    ATTR_TITLE,'Switch language',
			    ATTR_CLASS,'ramadda-clickable ramadda-language-switch'],lang.label);
	});
	html+='</html>';
	let block = $(html);
	block.appendTo(jqid(id));
	let _this = this;
	block.find('.ramadda-language-switch').click(function() {
	    _this.switcherClicked($(this));
	});

    },
    loadPack: function(lang, callback) {
	if(Translate.packs[lang]) {
	    callback(Translate.packs[lang]);
	    return;
	}

	let url  = RamaddaUtil.getUrl('/getlanguage?language=' + lang);
	console.dir(url);
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
    },
    translate: function(selector,lang) {
	if(!lang) lang = this.defaultLanguage;
	if(!lang) {
	    lang = this.language ?? Utils.getLocalStorage('ramadda-language');
	    //For problems with local storage
	    if(lang==='undefined' || lang==='null') lang= null;
	    lang = lang ??   ramaddaLanguage ?? navigator?.language ?? navigator?.userLanguage;
	}

	if(!Utils.stringDefined(lang)) {
	    lang = LANGUAGE_ENGLISH;
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

	$('.ramadda-language-switch').each(function() {
	    if($(this).attr('data-language')==lang) {
		$(this).addClass('ramadda-language-switch-active');
	    } else {
		$(this).removeClass('ramadda-language-switch-active');
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

	let canTranslate = (tag,t,suffix)=>{
	    if(tag.hasClass('ramadda-notranslate')) {
		return false;
	    }
	    if(!t || t.indexOf('<')>=0) {
		return false;
	    }
	    return true;
	}
	let translate = (a,text,suffix)=>{
	    if(useDflt) {
		let orig = a.attr(langFlag(suffix));
		if(orig) return orig;
		return null;
	    }
	    if(!canTranslate(a,text,suffix)) {
		return;
	    }

	    if(pack[text]) {
		if(pack[text]=='<skip>') return null;
		a.attr(langFlag(suffix),text);
		return pack[text];
	    }
	    Translate.missing[text] = true;
	    return  a.attr(langFlag(suffix));
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
		    v =translate(a,a.attr(langFlag(attr))??a.attr(attr),attr);
		    if(v) {
			a.attr(attr,v);
		    }
		}});


	    let flag = langFlag()
	    let html = a.attr(flag)??a.html();
	    if(canTranslate(a,html)) {
		v = translate(a,html);
		if(v) {
		    a.html(v);
		}
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
	 


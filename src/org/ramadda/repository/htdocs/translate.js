var LANGUAGE_ENGLISH = 'en';
var LANGUAGE_SPANISH = 'es';

var Translate = {
    packs:{},
    missing:{},
    language:null,
    
    init: function() {
	let icon =HU.getIconImage('fas fa-language ramadda-header-icon');
	let switchPrefix = icon +HU.space(1);

	let menu = HU.span([ATTR_TITLE,'Change language',
			    ATTR_CLASS,CLASS_CLICKABLE+' ramadda-page-link',
			    ATTR_ID,'ramadda_language_menu'],icon);
	menu = $(menu).appendTo(jqid('ramadda_links_prefix'));
	
	menu.click(()=>{
	    let html = '';
	    html+= HU.div([ATTR_TITLE,'Clear language',
			   ATTR_CLASS,'ramadda-clickable ramadda-language-switch ramadda-menu-language-switch ramadda-user-link'],switchPrefix+'Clear');

	    ramaddaLanguages.forEach(lang=>{
		html+= HU.div(['data-language',lang.id,
			       ATTR_TITLE,'Switch language',
			       ATTR_CLASS,'ramadda-clickable ramadda-language-switch ramadda-menu-language-switch ramadda-user-link'],switchPrefix+lang.label);
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
	if(lang=='showmissing') {
	    Translate.showMissing();
	    return;
	}
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
	this.language = lang;
	Translate.translate(null,lang);
	
    },
    checkSwitcher: function() {
	let lang = this.language;
	$('.ramadda-language-switch').each(function() {
	    let selected = $(this).attr('data-language');
	    if(selected==lang) {
		$(this).addClass('ramadda-link-bar-item-active');
	    } else {
		$(this).removeClass('ramadda-link-bar-item-active');	
	    }
	});

    },
    showLanguages:function(id,lang) {
	let sid = HU.getUniqueId('switcher');
	let lid = HU.getUniqueId('contents');
	let html  = HU.center(HU.div([ATTR_ID,sid])) +
	    HU.div([ATTR_ID,lid]);
	jqid(id).html(html);
	let callback = lang=>{
	    Translate.setLanguage(lang);
	    Translate.checkSwitcher();
	    if(lang)
		HU.addToDocumentUrl('language',lang);
	    Translate.loadPack(lang,(pack)=>{
		let searchId  = HU.getUniqueId('search');
		let html = HU.div([ATTR_ID,searchId]);
		html +=HU.open(TAG_TABLE);
		html+=HU.tr([],HU.tds([ATTR_STYLE,'min-width:400px;'],[HU.b('English'),HU.b('Translated')]));
		Object.keys(pack).sort((a,b)=>{return a.length-b.length}).forEach(key=>{
		    if(key.startsWith('language.')) return;
		    html+=HU.tr([ATTR_CLASS,'phrase'],HU.tds([],[key,pack[key]]));
		});

		html+=HU.close(TAG_TABLE);
		jqid(lid).html(html);
		HU.initPageSearch('.phrase',null,null,false,{target:'#'+searchId,focus:true});
	    })
	};

	if(lang) {
	    callback(lang);
	}
	Translate.addSwitcher(sid,null,false,{callback:callback,skipEnglish:true});

	Translate.disable();
    },
    addSwitcher:function(id,langs,addListing,opts) {
	opts = opts??{}
	if(this.disabled) return;
	if(langs) langs=Utils.split(langs,",",true,true);
	else {
	    langs = [];
	    ramaddaLanguages.forEach(lang=>{langs.push(lang.id)});
	}
	let html = HU.open("div",[ATTR_CLASS,'ramadda-link-bar']);
	let cnt = 0;
	langs.forEach(langId=>{
	    if(opts.skipEnglish && langId==LANGUAGE_ENGLISH) return;
	    ramaddaLanguages.forEach(lang=>{
		if(lang.id!= langId) return;
		html+= HU.span(['data-language',lang.id,
				ATTR_TITLE,'Switch language',
				ATTR_CLASS,'ramadda-clickable ramadda-link-bar-item ramadda-language-switch'],lang.label);
		cnt++;
	    })});
	if(addListing) {
	    html+= HU.span(['data-language','showmissing',
			    ATTR_CLASS,'ramadda-clickable ramadda-link-bar-item ramadda-language-switch'],'Download missing');
	}

	html+=HU.close('div');
	let block = $(html);
	block.appendTo(jqid(id));
	let _this = this;
	block.find('.ramadda-language-switch').click(function() {
	    if(opts.callback) {
		let lang = $(this).attr('data-language');
		opts.callback(lang);
		return;
	    }
	    _this.switcherClicked($(this));
	});
	this.checkSwitcher();

    },
    loadPack: function(lang, callback) {
	if(Translate.packs[lang]) {
	    callback(Translate.packs[lang]);
	    return;
	}
	let url  = RamaddaUtil.getUrl('/getlanguage?language=' + lang);
	if(ramaddaCurrentEntry)
	    url += '&entryid=' + ramaddaCurrentEntry;
//	console.dir(url);
        $.ajax({
            url: url,
            dataType: 'text',
            success: function(data) {
		let pack = Translate.makePack(data);
		$('.ramadda-dict[lang="' + lang+'"]').each(function() {
		    let  p = Translate.makePack($(this).text());
		    pack=   $.extend(pack,p);
		});
		Translate.packs[lang]=pack;
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
	    if(value=='NA') return;
	    pack[key] = value;
	});
	return pack;
    },
    disable: function() {
	this.disabled = true;
    },
    setLanguage: function(lang) {
	this.language = ramaddaLanguage  =lang;
    },
    translate: function(selector,lang) {
	if(this.disabled) return;
	lang = this.language;
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
    phrases:{},
    definePhrase:function(lang,from,to) {
	let map = Translate.phrases[lang];
	if(!map) map = Translate.phrases[lang] = {};
	map[from] = to;
    },
    canTranslate:function(tag,t,suffix) {
	if(tag.hasClass('ramadda-notranslate')|| tag.hasClass('ramadda-language-block')) {
	    return false;
	}
	if(!t || t.indexOf('<')>=0) {
	    return false;
	}
	if(t.length<=1) return false;
	if(t.match(/.*[0-9].*/)) return false;
	if(t.match(/^[0-9]+/)) return false;
	if(t.match(/ [0-9]+$/)) return false;	    
	return true;
    },

    translateInner: function(selector,lang,pack,useDflt) {
	lang = this.language;
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

	this.checkSwitcher();

	if(useDflt && !this.haveDoneAnyTranslations) {
	    return
	}
//	console.log('translating to:' + lang +' use dflt:' + useDflt);
	this.haveDoneAnyTranslations=true;
	if(!pack) {
	    console.log('no language pack:' + lang);
	    return;
	}
	let map = Translate.phrases[lang];
	if(map) $.extend(pack,map);
	let langFlag = (suffix) =>{
	    return 'lang-orig-' + (suffix??'');
	}

	let translate = (a,text,suffix)=>{
	    if(a.prop('tagName')=='I' && suffix!='title') {
		return null;

	    }
	    text = text.trim();
	    if(useDflt) {
		let orig = a.attr(langFlag(suffix));
		if(orig) return orig;
		return null;
	    }

	    if(text.indexOf(Utils.MSGCHAR)>=0) {
		let tokens = text.split(Utils.MSGCHAR).filter(Boolean);
		let accum = '';
		tokens.forEach(token=>{
		    let translated = translate(a,token,suffix);
		    if(translated) accum+=translated;
		    else accum+=token;
		});
		return accum;
	    }

	    if(!Translate.canTranslate(a,text,suffix)) {
		return null;
	    }
		
	    if(pack[text]) {
		if(pack[text]=='<skip>') return null;
		a.attr(langFlag(suffix),text);
		return pack[text];
	    }

//	    if(!Translate.missing[text])console.log('missing:'+text+':');
	    Translate.missing[text] = true;

	    return  a.attr(langFlag(suffix));
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
		let attrValue = a.attr(attr);
		if(attrValue) {
		    v =translate(a,a.attr(langFlag(attr))??attrValue,attr);
		    if(v) {
			a.attr(attr,v);
		    }
		}});


	    let flag = langFlag()
	    let html = a.attr(flag)??a.html();
	    if(Translate.canTranslate(a,html)) {
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
	    if(key.length>100) return;
	    if(key.match(/^[0-9]+/)) return;
	    if(key.match(/ [0-9]+$/)) return;	    
	    if(key.match(/^:.*/)) return;
	    if(key.match(/^-.*/)) return;
	    if(key.match(/.*&.*/)) return;
	    if(key.match(/.*yyyy.*/i)) return;
	    if(key.match(/.*ramadda.*/i)) return;	    	    	    
	    if(Utils.isNoMsg(key)) return;
	    console.log(key);
	    missing+=(key+'=\n');
	});
	Utils.makeDownloadFile('phrases.txt',missing);
    }
};

if(ramaddaLanguagesEnabled) {
    $( document ).ready(function() {
	Translate.init();
    });
}
	 


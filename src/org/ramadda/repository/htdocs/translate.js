var LANGUAGE_ENGLISH = 'en';
var LANGUAGE_SPANISH = 'es';
var ATTR_DATA_LANGUAGE='data-language';
var CLASS_LANGUAGE_BLOCK='ramadda-language-block';



var Translate = {
    trackMissing:false,
    packs:{},
    missing:{},
    language:null,
    
    init: function() {
	let icon =HU.getIconImage('fas fa-language ramadda-header-icon');
	let switchPrefix = icon +HU.space(1);

	let menu = HU.span([ATTR_TITLE,'Change language',
			    ATTR_CLASS,HU.classes(CLASS_CLICKABLE,'ramadda-page-link'),
			    ATTR_ID,'ramadda_language_menu'],icon);
	menu = $(menu).appendTo(jqid('ramadda_links_prefix'));
	
	menu.click(()=>{
	    let html = '';
	    html+= HU.div([ATTR_TITLE,'Clear language',
			   ATTR_CLASS,
			   HU.classes(CLASS_CLICKABLE,'ramadda-language-switch ramadda-menu-language-switch ramadda-user-link')],
			  switchPrefix+'Clear');

	    ramaddaLanguages.forEach(lang=>{
		html+= HU.div([ATTR_DATA_LANGUAGE,lang.id,
			       ATTR_TITLE,'Switch language',
			       ATTR_CLASS,HU.classes(CLASS_CLICKABLE,'ramadda-language-switch ramadda-menu-language-switch ramadda-user-link')],
			      switchPrefix+lang.label);
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
	let lang = link.attr(ATTR_DATA_LANGUAGE);
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
	    let selected = $(this).attr(ATTR_DATA_LANGUAGE);
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
		html+=HU.tr([],HU.tds([ATTR_STYLE,HU.css(CSS_MIN_WIDTH,HU.px(400))],
				      
				      [HU.b('English'),HU.b('Translated - ' + lang)]));
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
    addSwitcher:function(id,langs,addDownload,opts) {
	if(addDownload) {
	    this.trackMissing=true;
	}
	opts = opts??{}
	if(this.disabled) return;
	if(langs) {
	    langs=Utils.split(langs,",",true,true);
	} else {
	    langs = [];
	    ramaddaLanguages.forEach(lang=>{langs.push(lang.id)});
	}
	let html = HU.open(TAG_DIV,[ATTR_CLASS,'ramadda-link-bar']);
	langs.forEach(langId=>{
	    if(opts.skipEnglish && langId==LANGUAGE_ENGLISH) return;
	    let label;
	    ramaddaLanguages.forEach(lang=>{
		if(lang.id!= langId) return;
		label = lang.label;
	    });
	    if(!label) {
		label = langId;
		let toks = Utils.split(label,':');
		if(toks.length>=2) {
		    langId=toks[0];
		    label=toks[1].replace('_',' ');
		} else {
		    label=Utils.makeLabel(langId);
		}
		
	    }
	    html+= HU.span([ATTR_DATA_LANGUAGE,langId,
			    ATTR_TITLE,'Switch language',
			    ATTR_CLASS,
			    HU.classes(CLASS_CLICKABLE,'ramadda-link-bar-item ramadda-language-switch')],label);

	});
	if(addDownload) {
	    Translate.downloadMode= true;
	    html+= HU.span([ATTR_DATA_LANGUAGE,'showmissing',
			    ATTR_CLASS,
			    HU.classes(CLASS_CLICKABLE,'ramadda-link-bar-item ramadda-language-switch')],'Download missing');
	}

	html+=HU.close(TAG_DIV);
	let block = $(html);
	block.appendTo(jqid(id));
	let _this = this;
	block.find('.ramadda-language-switch').click(function() {
	    if(opts.callback) {
		let lang = $(this).attr(ATTR_DATA_LANGUAGE);
		opts.callback(lang);
		return;
	    }
	    _this.switcherClicked($(this));
	});
	this.checkSwitcher();

    },
    pending:{},
    loadPack: function(lang, callback) {
	if(Translate.packs[lang]) {
	    callback(Translate.packs[lang]);
	    return;
	}
	if(Translate.pending[lang]) {
	    Translate.pending[lang].push(callback);
	    return;
	}	    
	Translate.pending[lang] = [];
	let url  = HU.url(RamaddaUtil.getUrl('/getlanguage'),'language', lang);
	if(ramaddaCurrentEntry)   url = HU.url(url,ARG_ENTRYID,ramaddaCurrentEntry);
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
		if(Translate.pending[lang]) {
		    Translate.pending[lang].forEach(cb=>{
			cb(Translate.packs[lang]);
		    });
		}
		Translate.pending[lang] = null;
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
	if(Translate.downloadMode) {
	    if(tag.hasClass('display-metadatalist-item')) return false;
	}
	if(tag.hasClass('ramadda-notranslate')|| tag.hasClass(CLASS_LANGUAGE_BLOCK)) {
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
	    blocks = $(selector).find(HU.dotClass(CLASS_LANGUAGE_BLOCK));	    
	} else {
	    all = $('*');
	    blocks = $(HU.dotClass(CLASS_LANGUAGE_BLOCK));	    
	}	    

	blocks.each(function() {
	    if($(this).attr(ATTR_DATA_LANGUAGE)==lang) {
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
	let origValueFlag = (suffix) =>{
	    return 'lang-orig-' + (suffix??'');
	}
	let currentValueFlag = (suffix) =>{
	    return 'lang-current-' + (suffix??'');
	}	

	let translate = (a,text,suffix)=>{
	    if(suffix!='title' && a.prop('tagName')=='I') {
		return null;
	    }
	    if(useDflt) {
		let orig = a.attr(origValueFlag(suffix));
		if(orig) return orig;
		return null;
	    }

	    //	    let debug = text.indexOf("toggle")>=0;
	    let debug = false;
	    //	    debug = text.indexOf("largest")>=0;
	    if(text.indexOf(Utils.MSGCHAR)>=0) {
		//		if(debug) console.log(suffix +" has delim:" + text);
		let tokens = Utils.tokenizeMessage(text);
		let accum = '';
		tokens.forEach(chunk=>{
		    if(chunk.type=='text') {
			accum+=chunk.value;
			return;
		    }
		    //		    if(debug)	console.log('\ttoken:',chunk.value);
		    let translated = translate(a,chunk.value,suffix);
		    if(translated) accum+=translated;
		    else accum+=chunk.value;
		});
		return accum;
	    }

	    if(debug) {
		console.log('text:'+suffix,':',text,a.prop('tagName'));
		//		if(!suffix && text.indexOf('xx')>=0) console.tr

	    }
	    //	    if(debug)		console.log('text:',suffix,':',text);

	    text = text.trim();
	    if(!Translate.canTranslate(a,text,suffix)) {
		return null;
	    }
	    
	    let origText =text;
	    if(!pack[text]) {
		text =text.toLowerCase();
	    }

	    if(pack[text]) {
		if(pack[text]=='<skip>') return null;
		a.attr(origValueFlag(suffix),origText);
		return pack[text];
	    }

	    if(this.trackMissing) {
		let trackMissing = true;
		if(a) {
		    if(a.hasClass('olButton') || a.hasClass('ramadda-text')) {
			trackMissing = false;
		    } 
		}
		let tagName = a.prop('tagName');
		if(tagName=='OPTION') {
		    let parent = a.parent();
		    if(parent.hasClass('ramadda-text')) {
			trackMissing=false;
		    }
		}
		if(tagName=='TITLE') {
		    if(ramaddaThisEntry) trackMissing=false;
		}
		if(trackMissing ) {
		    trackMissing = !Utils.isNoMsg(origText);
		}		    
		if(trackMissing) {
		    if(origText=='toplevel_folders') {
			console.log(origText);
			console.dir(a);
		    }
		    Translate.missing[origText] = true;
		} 

	    }


	    return  a.attr(origValueFlag(suffix));
	}
	let skip = {'SCRIPT':true,'BR':true,'HTML':true,'STYLE':true,'TEXTAREA':true,'HEAD':true,'META':true,'LINK':true,'BODY':true};
	let attrs = [ATTR_PLACEHOLDER,ATTR_TITLE,ATTR_VALUE];
	all.each(function() {
	    let v;
	    let a = $(this);
	    let tag = a.prop('tagName');
	    if(skip[tag]) {
		return;
	    }
	    attrs.forEach(attr=>{
		if(attr==ATTR_VALUE) {
		    //only handle value attr for submit
		    if(tag=='INPUT' && a.attr('type')=='submit') {
		    } else {
			return;
		    }
		}

		let attrValue = a.attr(attr);
		if(attrValue) {
		    let currentFlag = currentValueFlag(attr);
		    //		    console.log(currentFlag,a.attr(currentFlag));
		    if(a.attr(currentFlag) != lang) {
			a.attr(currentFlag,lang);
			//			console.log('\ttranslating',attr);
			v =translate(a,a.attr(origValueFlag(attr))??attrValue,attr);
			if(v) {
			    a.attr(attr,v);
			}
		    } else {
			//			console.log('\talready translated',attr,a.attr(attr));
		    }
		}});


	    let flag = origValueFlag()
	    let currentFlag = currentValueFlag()	    
	    let html = a.attr(flag)??a.html();
	    if(Translate.canTranslate(a,html) && a.attr(currentFlag) != lang) {
		v = translate(a,html);
		if(v) {
		    a.html(v);
		    a.attr(currentFlag,lang);
		}
	    }
	});
    },
    showMissing: function() {
	let missing = '';
	missing+='#page: ' +  window.location.pathname +' title:' + document.title+'\n';
	let cnt = 0;
	Object.keys(Translate.missing).forEach(key=>{
	    if(key.length>100) return;
	    if(key.match(/^[_\{\}=0-9]+/)) return;
	    if(key.match(/ [0-9]+$/)) return;	    
	    if(key.match(/\./)) {
		if(!key.match(/\s/)) return;
	    }
	    if(key.match('©')) return;
	    if(key.match(/^Test$/)) return;	    
	    if(key.match(/^:.*/)) return;
	    if(key.match(/^-.*/)) return;
	    if(key.match(/.*&.*/)) return;
	    if(key.match(/.*yyyy.*/i)) return;
	    if(key.match(/.*ramadda.*/i)) return;	    	    	    
	    if(Utils.isNoMsg(key)) return;
	    console.log(key);
	    missing+=(key+'=\n');
	    cnt++;
	});
	if(cnt==0) {
	    alert('No missing phrases');
	    return
	} 
	Utils.makeDownloadFile('phrases.txt',missing);
    }
};

if(ramaddaLanguagesEnabled) {
    $( document ).ready(function() {
	Translate.init();
    });
}



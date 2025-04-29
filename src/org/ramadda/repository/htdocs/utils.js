//"use strict";
/**
   Copyright (c) 2008-2025 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/



var ramaddaGlobals = {
    iconWidth:'18px'
}
var root = ramaddaBaseUrl;
var urlroot = ramaddaBaseUrl;
//Used in entry.js
var icon_close = "fas fa-window-close";
var icon_stop='fas fa-stop';
var icon_play='fas fa-play';
var icon_pin = "fas fa-thumbtack";
var icon_help = "fas fa-question-circle";
var icon_command = ramaddaCdn + "/icons/command.png";
var icon_rightarrow = ramaddaCdn + "/icons/grayrightarrow.gif";
var icon_downdart = ramaddaCdn + "/icons/downdart.gif";
var icon_updart = ramaddaCdn + "/icons/updart.gif";
var icon_rightdart = ramaddaCdn + "/icons/rightdart.gif";
var icon_progress = ramaddaCdn + "/icons/progress.gif";
var icon_wait = ramaddaCdn + "/icons/wait.gif";
var icon_information = ramaddaCdn + "/icons/information.png";
var icon_folderclosed = ramaddaCdn + "/icons/folderclosed.png";
var icon_folderopen = ramaddaCdn + "/icons/togglearrowdown.gif";
var icon_folderclosed = "fas fa-caret-right";
var icon_folderopen = "fas fa-caret-down";
var icon_tree_open = ramaddaCdn + "/icons/togglearrowdown.gif";
var icon_tree_closed = ramaddaCdn + "/icons/togglearrowright.gif";
var icon_zoom = ramaddaCdn + "/icons/magnifier.png";
var icon_zoom_in = ramaddaCdn + "/icons/magnifier_zoom_in.png";
var icon_zoom_out = ramaddaCdn + "/icons/magnifier_zoom_out.png";
var icon_menuarrow = ramaddaCdn + "/icons/downdart.gif";
var icon_blank16 = ramaddaCdn + "/icons/blank16.png";
var icon_blank = ramaddaCdn + "/icons/blank.gif";
var icon_menu = ramaddaCdn + "/icons/menu.png";
var icon_trash =  "fas fa-trash-alt";

var UNIT_FT='ft';
var UNIT_MILES='mi';
var UNIT_KM='km';
var UNIT_M='m';
var UNIT_NM='nm';

var CLASS_DIALOG = 'ramadda-dialog';
var CLASS_DIALOG_BUTTON = 'ramadda-dialog-button';
var CLASS_CLICKABLE = 'ramadda-clickable';
var CLASS_HOVERABLE = 'ramadda-hoverable';


var ID = "id";
var BACKGROUND = "background";
var CLASS = "class";
var DIV = "div";
var POSITION = "position";
var WIDTH = "width";
var ALIGN = "align";
var VALIGN = "valign";
var HEIGHT = "height";
var SRC = "src";
var STYLE = "style";
var TABLE = "table";
var TITLE = "title";
var THEAD = "thead";
var TBODY = "tbody";
var TFOOT = "tfoot";
var TR = 'tr';
var TD= 'td';
var BR= 'br';
var PRE = "pre";
var TAG_A = "a";
var TAG_B = "b";
var TAG_DIV = "div";
var SELECT = "select";
var OPTION = "option";
var VALUE = "value";
var TAG_CANVAS = "canvas";
var TAG_IMG = "img";
var TAG_INPUT = "input";
var TAG_LI = "li";
var TAG_SELECT = "select";
var TAG_OPTION = "option";
var TAG_FORM = "form";
var TAG_TABLE = "table";
var TAG_TR = "tr";
var TAG_TD = "td";
var TAG_UL = "ul";
var TAG_OL = "ol";
var ATTR_SRC = "src";
var ATTR_TYPE = "type";
var ATTR_WIDTH = "width";
var ATTR_HEIGHT = "height";
var ATTR_HREF = "href";
var ATTR_PLACEHOLDER = "placeholder";
var ATTR_BORDER = "border";
var ATTR_VALUE = "value";
var ATTR_TITLE = "title";
var ATTR_ALT = "alt";
var ATTR_ID = "id";
var ATTR_CLASS = "class";
var ATTR_SIZE = "size";
var ATTR_STYLE = "style";
var ATTR_TARGET = "target";
var ATTR_ALIGN = "align";
var ATTR_VALIGN = "valign";
var SPACE = "&nbsp;";
var SPACE1 = "&nbsp;";
var SPACE2 = "&nbsp;&nbsp;";
var SPACE3 = "&nbsp;&nbsp;&nbsp;";
var SPACE4 = "&nbsp;&nbsp;&nbsp;&nbsp;";

var ARG_PAGESEARCH='pagesearch';


function noop() {}

function addHandler(obj, id) {
    if (window.globalHandlers == null) {
        window.globalHandlers = {};
    }
    if (!id) id = HtmlUtils.getUniqueId();
    window.globalHandlers[id] = obj;
    return id;
}

function getHandler(id) {
    if (!id || window.globalHandlers == null) {
        return null;
    }
    return window.globalHandlers[id];
}


//Utils
var Utils =  {
    me:"Utils",
    pageLoaded: false,
    loadFunctions:[],
    mouseMoveCnt:0,
    mouseIsDown: false,
    entryGroups: new Array(),
    groupList: new Array(),
    tooltipObject:null,
    entryDragInfo:null,
    globalEntryRows:{},
    defineGlobal: function(id,what) {
	if(!Utils.isDefined(window[id])) {
	    window[id] = what;
	}
    },
    checkLicense:function(domId,required,args) {
	let opts = {
	    message:"To access this content do you agree with the following license?",
	    showLicense:true,
	    suffix:'',
	    onlyAnonymous:false,
	    redirect:ramaddaBaseUrl,
	    logName:false
	}
	if(args) $.extend(opts,args);
	let text = jqid(domId).html();
	let key = 'licenseagree_' + required;
	let agreed = Utils.getLocalStorage(key);
	if(opts.onlyAnonymous && !Utils.isAnonymous()) return;
	if(!agreed) {
	    let buttonList = [HU.div(['action','ok',ATTR_CLASS,'ramadda-button ' + CLASS_CLICKABLE],
				     "Yes"),
			      HU.div(['action','no',ATTR_CLASS,'ramadda-button ' + CLASS_CLICKABLE],"No")]

	    let buttons = HU.buttons(buttonList);
	    let html =  opts.message;
	    html+=(opts.showLicense?HU.div([],text):'<br>')+opts.suffix;
	    if(opts.logName) {
		html+=HU.vspace('1em');
		html+=HU.div([],'Please enter your contact information');
		html+=HU.formTable();
		html+=HU.formEntry("Name:",HU.input('','',['tabindex','1',ATTR_ID,'licenseagreename','size','30']));
		html+=HU.formEntry("Email:",HU.input('','',['tabindex','2',ATTR_ID,'licenseagreeemail','size','30']));		
		html+=HU.formTableClose();
	    }


	    html+=buttons;

	    html = HU.div([ATTR_CLASS,'ramadda-license-dialog'], html);
	    let dialog =  HU.makeDialog({anchor:$(window),
					 at:'left+100 top+100',
					 my:'left top',
					 content:html,
					 remove:false,modalStrict:true,sticky:true});


	    dialog.find('.ramadda-button').click(function() {
		if($(this).attr('action')!='ok') {
		    window.location.href=opts.redirect;
		    return;
		}
		let name = "";
		let email="";
		if(opts.logName) {
		    name = jqid('licenseagreename').val().trim();
		    email = jqid('licenseagreeemail').val().trim();		    
		    if(!Utils.stringDefined(name) || !Utils.stringDefined(email)) {
			alert('Please enter your contact information');
			return;
		    }
		}
		let url = HU.url(RamaddaUtil.getUrl('/loglicense'),
				 ["licenseid",required,
				  "name",name,"email",email,
				  "entryid",opts.entryid]);
		$.getJSON(url, data=>{});
		Utils.setLocalStorage(key, true);
		dialog.remove();
	    });
	}
    },

    /**
       make and return a copy of any objects given as arguments
       this can handle null args
    */
    clone:function() {
	if(arguments.length==0) return {};
	let first =  arguments[0]?$.extend({},arguments[0]):{};
	for(let i=1;i<arguments.length;i++) {
	    if(arguments[i])
		first = $.extend(first,arguments[i]);
	}
	return first;
    },
    preventSubmit:function(event) {
        if (event.keyCode === 13) { 
            return false; 
        }
	return true;
    },

    getMin:function(a) {
	let min = NaN;
	a.forEach(v=>{
	    if(isNaN(v)) return;
	    if(isNaN(min) || v<min)  min=v;
	});
	return min;
    },
    getMax:function(a) {
	let max = NaN;
	a.forEach(v=>{
	    if(isNaN(v)) return;
	    if(isNaN(max) || v>max)  max=v;
	});
	return max;
    },

    bufferedCalls:{},
    bufferedCall:function(id, func,timeout) {
	timeout= Utils.isDefined(timeout)?timeout:1;
	if(this.bufferedCalls[id]) {
	    clearTimeout(this.bufferedCalls[id]);
	}
	this.bufferedCalls[id]=setTimeout(()=>{
	    func();
	},timeout);
    },
    throttle:function(f, delay) {
	let timer = 0;
	return function(...args) {
            clearTimeout(timer);
            timer = setTimeout(() => {f.apply(this, args)}, delay);
	}
    },
    initCopyable: function(selector,args) {
	let opts = {
	    title:null,
	    ack:null,
	    addLink:false,
	    addLinkToParent:true,
	    removeTags:false,
	    removeNL:false,
	    downloadFileName:null,
	    extraStyle:null,
	    textArea:null,
	    input:null
	}
	if(args) $.extend(opts,args);

	$(selector).each(function(){
	    let title = $(this).attr(ATTR_TITLE);
	    if(!title)
		$(this).attr(ATTR_TITLE,opts.title??'Click to copy');
	    let link = $(this);
	    if(opts.addLink) {
		let parent = opts.addLinkToParent?$(this).parent():$(this);
		parent.css('position','relative');
		let style = HU.css('position','absolute') +(opts.extraStyle??HU.css('top','5px',  'right','5px'));
		link = $(HU.tag('ramadda-copy-link', [ATTR_CLASS,'ramadda-clickable',
				 ATTR_STYLE,style],
				HtmlUtils.getIconImage('fa-copy'))).appendTo(parent);
	    } else {
		$(this).addClass('ramadda-clickable');
	    }
	    let inputs;
	    let focusedInput;
	    if(opts.input) {
		inputs = $(opts.input);
		if(inputs.length==0) {
		    console.log('InitCopyable: no inputs found with selector:' + opts.input);
		}

		inputs.on('focus', function() {
		    focusedInput=$(this);
		    inputs.each(function() {
			$(this).css('background','#fff');
		    });
		    $(this).css('background','var(--color-mellow-yellow)');
		});
	    }

	    link.click(()=>{
		let prefix = $(this).attr('data-copy-prefix');
		let suffix = $(this).attr('data-copy-suffix');		
		let text = $(this).attr('data-copy')??$(this).html();
		if(prefix) {
		    prefix = prefix.replace(/\\n/g,'\n');
		    text =prefix+text;
		}
		if(suffix) {
		    suffix = suffix.replace(/\\n/g,'\n');
		    text =text+suffix;
		}		
		text = text.replace(/<ramadda-copy-link.*<\/ramadda-copy-link.*>/,'');
		if(opts.removeTags) {
		    text = text.replace(/<br>/g,'\n').replace(/<p>/g,'\n\n').replace(/<[^>]+>/g,'');
		    text = text.replace(/&lt;/g,'<').replace(/&gt;/g,'>');
		}
		if(opts.removeNL) {
		    text = text.replace(/\n/g,' ');
		}
		if(opts.addNL) text = text+'\n';
		if(opts.input) {
		    if(!focusedInput) focusedInput = inputs.first();
		    if(focusedInput.length>0) {
			focusedInput.val(focusedInput.val() +' ' + text);
		    }
		    return;
		}

		if(opts.textArea) {
		    HU.insertIntoTextarea(opts.textArea,text);
		    return;
		}
		if(opts.downloadFileName) {
		    let f = prompt('Download file name',opts.downloadFileName);
		    if(f) {
			Utils.makeDownloadFile(f,text);
			return
		    }
		} 
		Utils.copyToClipboard(text)
		alert($(this).attr('copy-message')??opts.ack??'Text copied to clipboard');
	    });
	});
    },
    initPage: function() {
	
        this.initContent();
        this.pageLoaded = true;
        this.initDisplays();
	HU.initTooltip('.ramadda-tooltip-element');
        document.onmousemove = Utils.handleMouseMove;
        document.onmousedown = Utils.handleMouseDown;
        document.onmouseup = Utils.handleMouseUp;
        document.onkeypress = Utils.handleKeyPress;
        //allow for tabs to be added to text areas
        $(document).delegate('textarea', 'keydown', function(e) {
            var keyCode = e.keyCode || e.which;
            if (keyCode == 9) {
                e.preventDefault();
                var start = this.selectionStart;
                var end = this.selectionEnd;
                $(this).val($(this).val().substring(0, start) +
                            "\t" +
                            $(this).val().substring(end));
                this.selectionStart = this.selectionEnd = start + 1;
            }
        });
        Utils.loadFunctions.forEach(f=>{
            f();
        });
    },

    addLoadFunction: function(f) {
        Utils.loadFunctions.push(f);
    },
    getLocalStorage: function(key, toJson,addPrefix) {
        try {
	    if(addPrefix)
		key = ramaddaBaseEntry+"." + key;
            let v = localStorage.getItem(key);
            if(v!==null && toJson) {
                return JSON.parse(v);
            }
            return v;
        } catch(err) {
            console.log("Error getting local storage. key=" + key);
            return null;
        }
    },
    setLocalStorage: function(key, value, fromJson,addPrefix) {
	if(!localStorage) return;
	try {
	    if(addPrefix)
		key = ramaddaBaseEntry+"." + key;
	    if(value===null) {
		localStorage.removeItem(key);
		return;
	    }
            if(value && fromJson) value = JSON.stringify(value);
            localStorage.setItem(key, value);
        } catch(err) {
            console.log("Error setting local storage. key=" + key);
	}
    },

    getFileTail:function(url) {
	try {
	    let _url = new URL(url);
	    return    _url.pathname.replace(/.*\/([^\/]+)$/g,"$1");	    
	} catch(e) {
	    return null;
	}
    },
    isImage:function(url) {
        if(!url) return false;
	return url.search(/(\.png|\.jpg|\.jpeg|\.gif|\.webp|\.heic)/i) >= 0;
    },
    initDragAndDrop:function(target, dragOver,dragLeave,drop,type, acceptText,skipEditable) {
	if(!skipEditable)	target.attr('contenteditable',true);
        target.on('dragover', (event) => {
            event.stopPropagation();
            event.preventDefault();
            target.addClass("ramadda-drop-active");
            if(dragOver) dragOver(event);
        });

        target.on('dragleave', (event) => {
            if(dragLeave) dragLeave(event);
        });


        target.on('drop', (event) => {
            target.removeClass("ramadda-drop-active");
            event.stopPropagation();
            event.preventDefault();
            let files = event.originalEvent.target.files || event.originalEvent.dataTransfer.files
            for (let i=0; i<files.length;i++) {
                let file  = files[i];
                if(!file) continue;
                if(type) {
		    if(typeof type == 'string') {
			if(!file.type.match(type)) {
			    continue;
			}
		    } else {
			if(!type(file)) continue;
		    }
		}
                let reader = new FileReader();
                reader.onload = (onloadEvent) => {
                    if(drop) drop(onloadEvent,file,onloadEvent.target.result,true);
                };
                reader.readAsDataURL(file); 
            }
        });

        target.on('paste', (event) => {
            let items = (event.clipboardData || event.originalEvent.clipboardData).items;
            for(let i=0;i<items.length;i++) {
                let item = items[i];

                if(item.kind=="string") {
                    if(!acceptText) continue;
                    if(item.type!="text/plain") continue;
                    item.getAsString(s=>{
                        if(drop)drop(event,item,s,false);
                    });
                    continue;
                    event.stopPropagation();
                    event.preventDefault();
                } else  if(item.kind!="file") {
                    continue;
                }
                event.stopPropagation();
                event.preventDefault();

                let reader = new FileReader();
                reader.onload = (event) => {
                    if(drop)drop(event,item,event.target.result,false);
                }; 
                let blob = item.getAsFile();
                reader.readAsDataURL(blob);
                break
            }
        });
        

    },


    isPost:function() {
        let meta = $("#request-method");
        if(meta.length==0) return false;
        return meta.attr('content')=="POST";
    },
    max: function(v1,v2) {
	if(!Utils.isDefined(v1)) return v2;
	if(!Utils.isDefined(v2)) return v1;	
        if(isNaN(v1)) return v2;
        if(isNaN(v2)) return v1;        
        return Math.max(v1,v2);
    },
    min: function(v1,v2) {
	if(!Utils.isDefined(v1)) return v2;
	if(!Utils.isDefined(v2)) return v1;	
        if(isNaN(v1)) return v2;
        if(isNaN(v2)) return v1;        
        return Math.min(v1,v2);
    },    
    toRadians:function(degrees) {
        return degrees * Math.PI / 180;
    },
    toDegrees:function(radians) {
        return radians * 180 / Math.PI;
    },
    distance:function(x1,y1,x2,y2) {
	return Math.hypot(x2-x1, y2-y1);
    },
    //Originally from https://stackoverflow.com/questions/2637023/how-to-calculate-the-latlng-of-a-point-a-certain-distance-away-from-another
    reverseBearing: function(pt,brng, distKm) {
	distKm = distKm / 6371;  
	brng = Utils.toRadians(brng);
	var lat1 = Utils.toRadians(pt.lat);
	var lon1 = Utils.toRadians(pt.lon);
	var lat2 = Math.asin(Math.sin(lat1) * Math.cos(distKm) + 
                             Math.cos(lat1) * Math.sin(distKm) * Math.cos(brng));

	var lon2 = lon1 + Math.atan2(Math.sin(brng) * Math.sin(distKm) *
                                     Math.cos(lat1), 
                                     Math.cos(distKm) - Math.sin(lat1) *
                                     Math.sin(lat2));
	if (isNaN(lat2) || isNaN(lon2)) return null;
	return MapUtils.createLonLat(Utils.toDegrees(lon2),Utils.toDegrees(lat2));
    },

    //Get degrees bearing from p1 to p2. north =0,east=90, south=180,west=270
    getBearing:function(p1,p2) {
        let lat1 = Utils.toRadians(p1.lat);
        let lon1 = Utils.toRadians(p1.lon);
        let lat2 = Utils.toRadians(p2.lat);
        let lon2 = Utils.toRadians(p2.lon);             
        let X =  Math.cos(lat2) * Math.sin(lon2-lon1);
        let Y = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(lon2-lon1);
        let B = Math.atan2(X,Y);
        B = Utils.toDegrees(B);
        B = (B + 360) % 360;
        return B;
    },
    isReturnKey: function(e) {
        var keyCode = e.keyCode || e.which;
        return keyCode == 13;
    },
    //list of 2-tuples
    getBounds: function(polygon) {
        if(!polygon) return null;
        let minx = null,  maxx=null, miny=null, maxy=null;
        polygon.forEach(pair=>{
            minx = minx===null?pair[0]:Math.min(minx,pair[0]);
            maxx = maxx===null?pair[0]:Math.max(maxx, pair[0]);
            miny = miny===null?pair[1]:Math.min(miny,pair[1]);
            maxy = maxy===null?pair[1]:Math.max(maxy, pair[1]);     
        });
        return {
            getWidth: function() {return this.maxx-this.minx;},
            getHeight: function() {return this.maxy-this.miny;},            
            getCenter: function() {
                let x = this.minx+(this.maxx-this.minx)/2;
                let y = this.miny+(this.maxy-this.miny)/2;              
                return {x:x,y:y};
            },
            minx:minx,maxx:maxx,miny:miny,maxy:maxy
        };
    },
    mergeBounds(b1,b2) {
        if(b1==null) return b2;
        if(b2==null) return b1;
        let b3 = {}
        b3 = $.extend(b3,b1);
        $.extend(b3,{
            minx:Math.min(b1.minx,b2.minx),
            maxx:Math.max(b1.maxx,b2.maxx),
            miny:Math.min(b1.miny,b2.miny),
            maxy:Math.max(b1.maxy,b2.maxy),
        });
        return b3;
    },
    addCopyLink:function(id) {
	let contents=jqid(id).html();
	contents = contents.replace(/&lt;/g,"<").replace(/&gt;/g,">").replace(/&amp;/g,"&").trim();
	let div = jqid(id);
	div.css('position','relative');
	let copyId = id+"_copy";
	let downloadId = id+"_download";	
	let pos = 10;
	if(div.attr('add-copy')=='true') {
	    let copy = HU.div([ATTR_ID,copyId,
			       ATTR_TITLE,"Copy to clipboard",
			       ATTR_CLASS,CLASS_CLICKABLE,
			       ATTR_STYLE,HU.css("position","absolute","right",pos+"px","top","5px")], HU.getIconImage("fas fa-clipboard"));
	    pos+=20;
	    jqid(id).append(copy);
	}
	if(div.attr('add-download')=='true') {
	    let download = HU.div([ATTR_ID,downloadId,
				   ATTR_TITLE,"Download",
				   ATTR_CLASS,CLASS_CLICKABLE,
				   ATTR_STYLE,HU.css("position","absolute","right",pos+"px","top","5px")], HU.getIconImage("fas fa-download"));

	    jqid(id).append(download);
	}	
	jqid(copyId).click(function(){
	    Utils.copyToClipboard(contents);
	    let html = HU.div([ATTR_STYLE,'background:var(--color-mellow-yellow);padding:5px;'],
			      'Ok, result is copied');
            let dialog = HU.makeDialog({content:html,anchor:$(this),
					my:"right top",at:"right bottom"});
	    setTimeout(()=>{
		dialog.hide(1000);
		setTimeout(()=>{dialog.remove();},1100);
	    },1000)
	});
	jqid(downloadId).click(()=>{
	    Utils.makeDownloadFile(div.attr('download-file')??'download.txt',contents);
	});	
    },
    copyToClipboardOld:function(text) {
        let temp = $("<textarea></textarea>");
        $("body").append(temp);
        temp.val(text).select();
        if(!document.execCommand("copy")) {
	    console.error('Copy to clipboard failed');
	}
        temp.remove();
    },
    copyToClipboard:function(text) {
	navigator.clipboard.writeText(text)
	    .then(() => {
		//		console.log('Text successfully copied to clipboard!');
	    })
	    .catch((error) => {
		console.error('Unable to write to clipboard with navigator.clipboard.writeText:'+ error);
		Utils.copyToClipboardOld(text);
	    });
    },    
    isAnonymous: function() {
        return ramaddaUser =="anonymous";
    },
    getIcon: function(icon) {
        return ramaddaCdn + "/icons/" + icon;
    },
    imports: {},
    get: function(list,idx,dflt) {
        if(!list) return dflt;
        if(idx<list.length) return list[idx];
        return dflt;
    },
    //from: https://stackoverflow.com/questions/13002979/how-to-calculate-rotation-angle-from-rectangle-points
    getRotation:function(coords) {
	if(coords[0].x) {
	    let tmp = [];
	    coords.forEach(c=>{
		tmp.push(c.x,c.y);
	    });
	    coords = tmp;
	}
	// Get center as average of top left and bottom right
	var center = [(coords[0] + coords[4]) / 2,
                      (coords[1] + coords[5]) / 2];

	// Get differences top left minus bottom left
	var diffs = [coords[0] - coords[6], coords[1] - coords[7]];

	// Get rotation in degrees
	var rotation = Math.atan(diffs[0]/diffs[1]) * 180 / Math.PI;

	// Adjust for 2nd & 3rd quadrants, i.e. diff y is -ve.
	if (diffs[1] < 0) {
            rotation += 180;
	    
	    // Adjust for 4th quadrant
	    // i.e. diff x is -ve, diff y is +ve
	} else if (diffs[0] < 0) {
            rotation += 360;
	}
	// return array of [[centerX, centerY], rotation];
	return {center:center, angle:rotation};
    },
    rotate:function(cx, cy, x, y, angle,anticlock_wise) {
        if(angle == 0){
            return {x:parseFloat(x), y:parseFloat(y)};
        }
        if(anticlock_wise){
            var radians = (Math.PI / 180) * angle;
        }else{
            var radians = (Math.PI / -180) * angle;
        }
        var cos = Math.cos(radians);
        var sin = Math.sin(radians);
        var nx = (cos * (x - cx)) + (sin * (y - cy)) + cx;
        var ny = (cos * (y - cy)) - (sin * (x - cx)) + cy;
        return {x:nx, y:ny};
    },
    splitFirst: function(s,delim) {
	let idx = s.indexOf(delim);
	if(idx<0) return [s];
	return [s.substring(0,idx),s.substring(idx+1)];
    },
    debug:false,
    split: function(s,delim,trim,excludeEmpty,dflt) {
        if(!Utils.isDefined(s)) return dflt;
        let l = [];
	delim = delim??",";
	//Convert the escaped delims
	let regexp = new RegExp('\\\\' + delim,"g");
	s = s.replace(regexp,'_HIDEDELIM_');
	if(this.debug) console.log(s);
	//	console.log(s);
        s.split(delim).forEach((tok)=>{
            tok = tok.replace(/_comma_/g,",");
	    tok = tok.replace(/_HIDEDELIM_/g,delim);
            if(trim) tok = tok.trim();
            if(excludeEmpty && tok == "") return;
            l.push(tok);
        });
        return l;
    },
    sortNumbers:function(l) {
        l.sort((a,b)=>{return +a - +b});
        return l;
    },
    sortDates:function(l) {
        l.sort((a,b)=>{return a.getTime()-b.getTime()});
        return l;
    },    

    convertText:function(s) {
	if(!s) return s;
	if(s.startsWith('base64:'))
	    s = window.atob(s.substring(7));
	return s;
    },

    decodeText: function(t) {
        if(!t) return null;
        t = String(t);
	t = Utils.convertText(t);
        return t.replace(/_leftbracket_/g,"[").replace(/_rightbracket_/g,"]").replace(/_dq_/g,"\"\"").replace(/&quote;/gi, '\"').replace(/_quote_/gi, '\"').replace(/_qt_/gi, '\"').replace(/_newline_/gi, '\n').replace(/newline/gi, '\n').replace(/_nl_/g,'\n');
    },
    handleActionResults: function(id,url) {
        setTimeout(() =>{
            let success=json=>{
                let msg = "";
                if(json.message) msg= json.message.replace(/\n/g,"<br>");
                $("#" + id).html(msg);
                if(json.status=="running") {
                    Utils.handleActionResults(id,url);
                }
            };
            let fail=json=>{
                $("#" + id).html("Error:" + json);
            };             
            $.getJSON(url, success).fail(fail);
        },1000);
    },
    sumList: function(l) {
        let accum = 0;
        l.forEach(v=>{
            if(!isNaN(v)) accum += v;
        });
        return accum;
    },
    moveBefore: function(list,item1,item2) {
	list = this.removeItem(list,item2);
        const index1 = list.indexOf(item1);
	list.splice(index1, 0, item2); 
	return list;
    },
    toFront: function(list,element,isList) {
        const index = list.indexOf(isList?element[0]:element);
        if (index > -1) {
            list.splice(index, 1);
	    if(isList)
		list.push(...element);
	    else
		list.push(element);
        }
        return list;
    },
    toBack: function(list,element,isList) {
        const index = list.indexOf(isList?element[0]:element);
        if (index > -1) {
            list.splice(index, 1);
	    if(isList)
		list.unshift(...element);
	    else
	    	list.unshift(element);                      
        }
        return list;
    },

    //Make a list of {key:'key',value:'value'} from the given map
    makeKeyValueList:function(map) {
	return Object.keys(map).map(key=>{
	    return {key:key,value:map[key]};
	});
    },
    //Apply forEach if the list is non null
    forEach:function(list,func) {
	if(list) list.forEach(func);
    },
    mergeLists: function(l1,l2,l3,l4,l5) {
        let l = [];
        if(l1) l1.map(e=>l.push(e));
        if(l2) l2.map(e=>l.push(e));
        if(l3) l3.map(e=>l.push(e));
        if(l4) l4.map(e=>l.push(e));
        if(l5) l5.map(e=>l.push(e));
        return l;
    },
    getNameValue: function(s, skipBlank) {
        //splits name:value,name:value
        if(!s) return null;
        var map = {};
        var toks = s.split(",");
        for (var i = 0; i < toks.length; i++) {
            var toks2 = toks[i].split(":");
            var v = toks2[1].trim();
            if(skipBlank && v == "") continue;
            v = v.replace(/_colon_/g,":");
            map[toks2[0].trim()] = v;
        }
        return map;
    },
    translatePoint: function(x, y, w,  h, pt, delta) {
        x = +x;
        y = +y;
        let r = {x:x,y:y};
        if(pt=="nw") {
            r.x = x; r.y = y;
        } else if(pt == "w") {
            r.x = x; r.y = y-h/2;
        } else if(pt == "sw") {
            r.x = x; r.y = y-h;
        } else if(pt == "s") {
            r.x = x-w/2; r.y = y-h;
        } else if(pt == "se") {
            r.x = x-w; r.y = y-h;
        } else if(pt == "e") {
            r.x = x-w; r.y = y-h/2;
        } else if(pt == "ne") {
            r.x = x-w; r.y = y;
        } else if(pt == "n") {
            r.x = x-w/2; r.y = y;                   
        } else if(pt == "c") {
            r.x = x-w/2; r.y = y-h/2;               
        } else {
            throw new Error("Unknown point" + pt);
        }
        if(delta!=null) {
            r.x+= +delta.dx;
            r.y+= +delta.dy;
        }
        return r;
    },


    displayTimes: function(label,times,oneLine,labels) {
        let t = "";
        let delim = oneLine?" "  :"\n";
        let pre = oneLine?" ":"\t";
        t+=label +delim;
        for(var i=0;i<times.length-1;i++) {
            let label=null;
            if(labels && i<labels.length) label = labels[i];
            if(!label)label = "time" +(i+1)
            t+=pre + label +": " + ((times[i+1].getTime()-times[i].getTime())/1000) + delim;
        }
        console.log(t);
    },

    cloneList: function(l) {
        return l?l.slice(0):null;
    },
    removeElement: function(list,value) {
        if(!list) list = [];
        let idx = list.indexOf(value);
        if(idx>=0) list.splice(idx,1);
        return list;
    },
    addUnique: function(list,value) {
        if(list==null) list=[];
        if(!list.includes(value)) 
            list.push(value);
        return list;
    },

    replaceAll: function(s,pattern,v) {
        let idx = s.indexOf(pattern);
        if(idx<0) return s;
        return s.substring(0,idx)+v + s.substring(idx+pattern.length);
    },
    parseAttributes: function(v) {
        let attrs = {};
        let newv;
        let toks;
        while((toks = v.match(/([^ ]+) *= *"([^"]*)"/))!=null) {
            attrs[toks[1].trim()] = toks[2];
            v = Utils.replaceAll(v, toks[0],"");
        }
        while((toks = v.match(/([^ ]+) *= *'([^']*)'/))!=null) {
            attrs[toks[1].trim()] = toks[2];
            v = Utils.replaceAll(v, toks[0],"");
        }
        while((toks = v.match(/([^ ]+) *= *([^ ]*)( |$)/))!=null) {
            attrs[toks[1].trim()] = toks[2];
            v = Utils.replaceAll(v, toks[0],"");
        }
        while((toks = v.match(/([^ ]+)( |$)/))!=null) {
            attrs[toks[1].trim()] = "true";
            v = Utils.replaceAll(v, toks[0],"");
        }           
        return attrs;
    },

    parseAttributesAsList: function(s) {
        let args = [];
        let i=0;
        let       inQuote    = false;
        let       prevEscape = false;
        let sb = "";
        for (let i = 0; i < s.length; i++) {
            let    c            = s[i];
            let isQuote      = (c == '\"');
            let isEscape     = (c == '\\');
            if (prevEscape) {
                sb+=c;
                prevEscape = false;
                continue;
            }

            if (c == '\\') {
                if (prevEscape) {
                    sb +=c;
                    prevEscape = false;
                } else {
                    prevEscape = true;
                }

                continue;
            }
            if (prevEscape) {
                sb +=c;
                prevEscape = false;
                continue;
            }
            if (isQuote) {
                if (inQuote) {
                    inQuote = false;
                    args.push(sb);
                    sb="";
                } else {
                    inQuote = true;
                }
                continue;
            }
            if (inQuote) {
                sb +=c;
                continue;
            }
            if (c == ' ' || c=='\n') {
                if (sb.length > 0) {
                    args.push(sb);
                    sb = "";
                }
                continue;
            }
            sb +=c;
        }
        if(sb!="")
            args.push(sb);
        let attrs = [];
        args.forEach(a=>{
            let idx = a.indexOf("=");
            let name = a;
            let v = "";
            if(idx>=0) {
                name = a.substring(0,idx);
                v = a.substring(idx+1);         
            }
            attrs.push(name);
            attrs.push(v);          
        });
        return attrs;
    },
    
    hideMore:function(base) {
        var link = GuiUtils.getDomObject("morelink_" + base);
        var div = GuiUtils.getDomObject("morediv_" + base);
        hideObject(div);
        showObject(link);
    },


    showMore:function(base) {
        var link = GuiUtils.getDomObject("morelink_" + base);
        var div = GuiUtils.getDomObject("morediv_" + base);
        hideObject(link);
        showObject(div);
    },

    ramaddaUpdateMaps:function() {
        //This gets called from toggleBlockVisibility
        //It updates any map on the page to fix some sort of offset problem
        if (!(typeof ramaddaMaps === 'undefined')) {
            for (i = 0; i < ramaddaMaps.length; i++) {
                var ramaddaMap = ramaddaMaps[i];
                if (!ramaddaMap.map) continue;
                ramaddaMap.map.updateSize();
            }
        }
    },


    formDialogId:"",
    submitEntryForm:function(dialogId) {
        Utils.popupFormLoadingDialog(dialogId);
        return true;
    },
    closeFormLoadingDialog:function() {
        var dialog = $(Utils.formDialogId);
        dialog.dialog('close');
    },
    popupFormLoadingDialog:function(dialogId) {
        Utils.formDialogId = dialogId;
        var dialog = $(dialogId);
        dialog.dialog({
            resizable: false,
            height: 100,
            modal: true
        });
    },
    replaceRoot: function(s) {
        var  p = "\\${" +"root}";
        var pattern = new RegExp(p);
        s = s.replace(pattern,ramaddaBaseUrl);
        return s;
    },
    loadScriptInfo:{},
    loadScript:function( url, callback, noCache ) {
        let key = "js:" + url;
        let info = Utils.loadScriptInfo[key];
        if(!info) {
            info = Utils.loadScriptInfo[key] = {loaded:false,loading:false,callbacks:[]};
        }
        if (!noCache && info.loaded) {
            if(callback) callback();
            return true;
        }
        if(callback)
            info.callbacks.push(callback);
        if(info.loading) {
	    return false;
	}
        info.loading = true;

        let script = document.createElement( "script" )
        script.type = "text/javascript";
        if(script.readyState) {  // only required for IE <9
            script.onreadystatechange = function() {
                if ( script.readyState === "loaded" || script.readyState === "complete" ) {
                    script.onreadystatechange = null;
		    info.loaded =true;
                    info.callbacks.forEach(callback=>callback());
                }
            };
        } else {  //Others
            script.onload = function() {
		info.loaded =true;
                info.callbacks.forEach(callback=>callback());
            };
        }
        info.callback=[];
        script.src = url;
        document.getElementsByTagName( "head" )[0].appendChild( script );
    },
    importJS: async function(path, callback, err, noCache) {
        path =this.replaceRoot(path);
        let _this = this;
        var key = "js:" + path;
        if (!noCache && this.imports[key]) return Utils.call(callback);
        try {
            //Some urls fail  with getScript so we do a getText then eval
            //            throw new Error();
            await $.ajax({
                url: path,
                dataType: 'script',
                cache: true,
                success: function(data) {
                    _this.imports[key] = true;
                    Utils.call(callback);
                }
            }).fail((jqxhr, settings, exc) => {
                console.log("initial importJS failed: " + path);
                console.log("error:" + exc);            
            });
        } catch (e) {
            try {
                //If the  ajax call failed it might be due to the mime type not being javascript. 
                //Try just grabbing the text and adding it via a script tag
                await $.ajax({
                    url: path,
                    dataType: 'text',
                    success: function(data) {
                        console.log("got script as text -  " + path);
                        var script = document.createElement('script');
                        script.innerHTML = data;
                        document.body.appendChild(script);
                        Utils.call(callback);
                        _this.imports[key] = true;
                    }
                }).fail((jqxhr, settings, exception) => {
                    if (!err) console.log("importJS failed:" + path);
                    else err(jqxhr, settings, exception);
                });
            } catch (e) {
                //                if(!err) console.log("importJS failed:" +path+" error:" + e);
                //                else Utils.call(err,e);
            }
        }
    },
    waitOn: async function(obj, callback) {
        if (window[obj]) return;
    },
    importCSS: async function(path, callback, err, noCache) {
        path =this.replaceRoot(path);
        var key = "css:" + path;
        if (!noCache)
            if (this.imports[key]) return Utils.call(callback);
        try {
            await $.ajax({
                url: path,
                dataType: 'text',
                success: (data) => {
                    if (!noCache)
                        this.imports[key] = true;
                    //              console.log("loaded:" + data);
                    $('<style type="text/css">\n' + data + '</style>').appendTo("head");
                    Utils.call(callback);
                }
            }).fail(err);
        } catch (e) {}
    },
    doFetch: async function(path, callback, err,what) {
        path =this.replaceRoot(path);
        let calledErr = false;
        try {
            let args = {
                url: path,
                success: function(data) {
                    Utils.call(callback, data);
                }
            };
            if(!what) {
                what = "text/plain";
                args.beforeSend =  function( xhr ) {
		    //If we do this then it screws up the encoding
                    //xhr.overrideMimeType( "text/plain; charset=x-user-defined" );
                    xhr.overrideMimeType( "text/plain;" );
                };
            }
            args.xhrFields = {
                responseType: what
            };
            await $.ajax(args).fail((p1,p2,p3)=>{
                calledErr=true;
                if(err)
                    err(p1,p2,p3)
            });
        } catch (e) {
            if(!calledErr)
                Utils.call(err, e);
        }
    },
    formatXml: function(xml,args) {
        let opts = {};
        if(args) $.extend(opts,args);
        let parser = new DOMParser();
        let xmlDoc = parser.parseFromString(xml,"text/xml");
        let html ="";
        let func;
        func = function(node, path, padding) {
            let pad = "";
            for(let i=0;i<padding;i++)
                pad +="  ";
            if(padding>5) {
                html+=pad +"...\n";
                return;
            }
            if(node.nodeName=="#text") {
                let text = node.nodeValue||"";
                text = text.trim();
                if(Utils.stringDefined(text)) {
                    html +=pad + "text:" + text+"\n";
                }
                return;
            } 
            if(node.nodeName=="#cdata-section") {
                html+=pad + node.wholeText;
                html+="\n";
                return;
            }
            if(path!="") path+=".";
            path+=node.nodeName;
            html +=pad + "&lt;" + HU.span(["data-path",path,
					   ATTR_TITLE,"Add path selector",
					   ATTR_STYLE,HU.css("cursor","pointer","text-decoration","underline"),
					   ATTR_CLASS,"ramadda-xmlnode"],node.nodeName)+"&gt;" + "\n";
            node.childNodes.forEach(child=>{
                func(child,path,padding+1);
            });
            html +=pad + "&lt;/" + node.nodeName+"&gt;" + "\n";
        }
        xmlDoc.childNodes.forEach(n=>{
            if(n.nodeName == "parsererror") {
                throw new Error("Parse error:" + n);
            }
        });

        xmlDoc.childNodes.forEach(n=>{
            func(n,"",0);
        });
        return html;
    },

    formatJson: function(json,levelsShown) {
        var blob =  this.formatJsonInner(json, 0,levelsShown);
        return this.formatJsonBlob(blob,null, 0,levelsShown);
    },
    formatJsonClick: function(imageid,innerid) {
        var inner = $("#"+innerid);
        var image = $("#"+imageid);
        if (inner.is(":visible")) {
            inner.hide();
            image.attr("src",icon_tree_closed);
        } else {
            inner.show();
            image.attr("src",icon_tree_open);
        }
    },
    formatJsonBlob: function(blob,label, level,levelsShown) {
        var html ="";
        if(label!=null) html = label+": ";
        html+=  blob.value;
        if(blob.inner) {
            var imageid = HtmlUtils.getUniqueId();
            var toggle = HtmlUtils.image(icon_tree_closed,[ATTR_ID,imageid]);
            var innerid = HtmlUtils.getUniqueId();
            var click = "Utils.formatJsonClick('" + imageid +"','" + innerid +"')";
            html = HtmlUtils.div(["onclick",click,ATTR_CLASS,"json-label"],toggle + " " + html);
            var display = level<levelsShown?"block":"none";
            html += HtmlUtils.div([ATTR_STYLE,"display:" + display+";",ATTR_CLASS,"json-inner",ATTR_ID,innerid],blob.inner);
        }
        return html;
    },
    formatJsonInner: function(json, level,levelsShown) {
        if(json==null) return {value:"null"};
        var type = typeof json;
        if(type == "string") {
            var clazz="json-string";
            if(json.match("^http")) {
                clazz="json-url";
                json = HtmlUtils.href(json,json,[ATTR_CLASS,clazz]);
            }
            return {value:HtmlUtils.span([ATTR_CLASS,clazz],"\"" +json +"\"")};
        }
        if(type == "number" || type == "boolean") {
            return {value:HtmlUtils.span([ATTR_CLASS,"json-"+type],json)};
        }

        if(json.toISOString) {
            return {value:HtmlUtils.span([ATTR_CLASS,"json-date"],"\"" +json.toISOString()+"\"")};
        }
        if(type == "function") {
            var args = (json + '').replace(/[/][/].*$/mg,'').replace(/\s+/g, '');
            args = args.replace(/[/][*][^\/*]*[*][/]/g, ''); 
            args = args.split('){', 1)[0].replace(/^[^\(]*[\(]/, '').replace(/=[^,]+/g, '').split(',').filter(Boolean);
            return {value:HtmlUtils.span([ATTR_CLASS,"json-function"], json.name +"(" + args+") {...}")};
        }
        var isArray  = Array.isArray(json);
        var label = isArray?HtmlUtils.span([ATTR_CLASS,"json-number"],"["  + json.length +"]"):"";
        var  html = HtmlUtils.openTag("div");
        var labels = [];
        var indices = [];
        if(isArray) {
            for(var i=0;i<json.length;i++) {
                labels.push(i);
                indices.push(i);
            }
            if(indices.length==0) {
                html += this.formatJsonBlob({value:HtmlUtils.span([ATTR_CLASS,"json-none"],"[]")},null,level+1,levelsShown);
            }
        } else {
            for(var name in json) {
                labels.push(name);
                indices.push(name);
            }
            if(indices.length==0) {
                html += this.formatJsonBlob({value:HtmlUtils.span([ATTR_CLASS,"json-none"],"No properties")},null,level+1,levelsShown);
            }
        }

        for(var i=0;i<indices.length;i++) {
            html += "<div>";
            var blob = Utils.formatJsonInner(json[indices[i]], level+1, levelsShown);
            html += this.formatJsonBlob(blob,labels[i],level+1,levelsShown);
            html += "</div>";
        }

        html += "</div>";
        return {value:label,inner:html};
    },
    padLeft: function(s, length, pad) {
        s = "" + s;
        if (!pad) pad = " ";
        while (s.length < length)
            s = pad + s;
        return s;
    },
    reverseArray: function(a) {
        var b = [];
        for(var i=a.length-1;i>=0;i--)
            b.push(a[i]);
        return b;
    },
    //If not an array then make it one
    makeArray: function(v) {
        if(!Array.isArray(v)) return [v];
        return v;
    },
    getUniqueId: function(prefix) {
        return HtmlUtils.getUniqueId(prefix);
        
    },
    uuidv4:function() {
	return ([1e7]+-1e3+-4e3+-8e3+-1e11).replace(/[018]/g, c =>
	    (c ^ crypto.getRandomValues(new Uint8Array(1))[0] & 15 >> c / 4).toString(16)
	);
    },

    //split the given list into
    splitList: function(list,max) {
        let lists = [];
        if(list.length<max) {
            lists.push(list);
        } else {
            let num = Math.ceil(list.length/max);
            let maxPer = Math.ceil(list.length/num);
            let current = [];
            lists.push(current);
            list.forEach(o=>{
                if(current.length>=maxPer) {
                    current = [];
                    lists.push(current);
                }
                current.push(o);
            });
        }
        return lists;
    },

    removeItem:function(array,item) {
	for(var i = array.length - 1; i >= 0; i--) {
            if(array[i] == item) {
		array.splice(i,1);
            }
	}
	return array;
    },
    getStack:function(cnt) {
	if(!Utils.isDefined(cnt)) cnt=100;
	let s = "";
	let err = new Error();
	if(!err.stack) {
	    return "no stack available";
	}
	let lines = Utils.split(err.stack,"\n",true,true);
	for(let i=1;i<cnt&& i<lines.length;i++) {
	    let line = lines[i];
	    //	    s+=line+"\n";
	    //	    continue;
	    line= Utils.split(line,"@");
	    let match = line[1].match(/\/([^/]+)$/);
	    s = s+"  "+line[0]+":" + match[1].replace(/:[^:]+$/,"")+"\n";
	}
	return s;
    },
    /*
      normalize the vec arg of numbers to 0...1
    */
    normalize:function(vec) {
	let vmin = Math.min(...vec.filter(v=>{return !isNaN(v)}));
	let vmax = Math.max(...vec.filter(v=>{return !isNaN(v)}));			    
	let vdelta = vmax - vmin;
	return vec.map(value => {
	    if(vdelta==0) return 0;
	    return (value - vmin) / vdelta;
	});
    },

    getBoolean:function(v) {
	if(!Utils.isDefined(v)) return false;
	return Utils.getProperty(v);
    },

    /** if v is 'true' or 'false' then return true or false
	else return v;
    */
    getProperty:function(v) {
	if(Utils.isDefined(v)) {
	    if(typeof v!='string') return v;
	    let sv = String(v).trim();
	    if(sv==='true') return true;
	    if(sv==='false') return false;		
	    return v;
	}
	return null;
    },

    triggerDownload:function(url) {
	let link = document.createElement('a');
	link.href = url;
	link.target='_download';
	//	link.setAttribute('download', 'file.csv');
	// This part is necessary for older browsers
	if (document.createEvent) {
            let event = document.createEvent('MouseEvents');
            event.initEvent('click', true, true);
            link.dispatchEvent(event);
	} else {
            link.click();
	}
    },



    join: function(l, delimiter, offset) {

        if ((typeof offset) == "undefined") offset = 0;
        var s = "";
        for (var i = offset; i < l.length; i++) {
            if (i > offset) s += delimiter;
            s += l[i];
        }
        return s;
    },
    
    wrap: function(l, prefix, suffix) {
	if(!l) return null;
        let s= ""; 
        l.forEach(item=>{
            s+=prefix + item +suffix;
        });
        return s;
    },

    parseMap: function(str,delim1,delim2) {
        if(str==null) return null;
        let toks = str.split(delim1??",");
        let map = {};
        for (var i = 0; i < toks.length; i++) {
	    let tok = toks[i];
	    let index = tok.indexOf(delim2??":");
            if (index>=0) {
		let key = tok.substring(0,index);
		let value = tok.substring(index+1);
                map[key] = value;
            }
        }
        return map;
    },
    getUniqueValues: function(l) {
        var u = [];
        var map = {};
        for (var i = 0; i < l.length; i++) {
            var value = l[i];
            if (!this.isDefined(map[value])) {
                map[value] = true;
                u.push(value);
            }
        }
        return u;
    },
    getMacro: function(v) {
        if (v == "${states}")
            return "Alabama,Alaska,Arizona,Arkansas,California,Colorado,Connecticut,Delaware,District of Columbia,Florida,Georgia,Hawaii,Idaho,Illinois,Indiana,Iowa,Kansas,Kentucky,Louisiana,Maine,Maryland,Massachusetts,Michigan,Minnesota,Mississippi,Missouri,Montana,Nebraska,Nevada,New Hampshire,New Jersey,New Mexico,New York,North Carolina,North Dakota,Ohio,Oklahoma,Oregon,Origin State,Pennsylvania,Rhode Island,South Carolina,South Dakota,Tennessee,Texas,Utah,Vermont,Virginia,Washington,West Virginia,Wisconsin,Wyoming";
        return v;
    },
    dayNames:["Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday"],
    dayNamesShort:["Sun","Mon","Tue","Wed","Thu","Fri","Sat"],
    dayNamesShortShort:["S","M","T","W","Th","F","Sa"],
    monthNames:["January","February","March","April","May","June","July","August","September","October","November","December"],
    monthNamesShort:["Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"],
    createDate: function(d,timeZoneOffset) {
        let date = Utils.createDateInner(d);
        if(date && timeZoneOffset) {
            date = new Date(Date.UTC(date.getUTCFullYear(), date.getUTCMonth(),date.getUTCDate(),date.getUTCHours()-timeZoneOffset,date.getUTCMinutes(),date.getUTCSeconds()));
        }
        return date;
    },
    createDateInner: function(d,now) {
        if(!d) return d;
        if(d.getTime) return d;
        d = d.trim();
        let regexp = new RegExp("^(\\+|-)?([0-9]+) *(minute|hour|day|week|month|year)s?$");
        let toks = d.match(regexp);
        if(toks) {
            let mult = parseFloat(toks[1]+toks[2]);
            let what = toks[3];
	    if(!now)  now = new Date();
            let date = now.getTime();
            if(what == "minute" || what=="minutes")
                return new Date(date+mult*1000*60);
            if(what == "hour" || what == "hours")
                return new Date(date+mult*1000*60*60);
            if(what == "day" || what == "days")
                return new Date(date+mult*1000*60*60*24);
            if(what == "week" || what=="weeks") {
                return new Date(date+mult*1000*60*60*24*7);
            }
            if(what == "month" || what=="months")
                return new Date(date+mult*1000*60*60*24*7*31);
            return new Date(date+mult*1000*60*60*24*365);
        }
        if(d.match(/^[0-9,-]+$/)) {
            let commaToks = d.split(",");
            let dashToks = d.split("-");            
            toks = dashToks.length>commaToks.length?dashToks:commaToks;
            let idx = 0;
            let f = (i)=>{
                if(i>=toks.length) return null;
                if(i==1) return +toks[i]-1;
                return +toks[i];
            };
            return new Date(Date.UTC(f(idx++),f(idx++),f(idx++),f(idx++),f(idx++),f(idx++)));
        }
        return  new Date(d);
    },
    minutesSince: function(date) {
        return Math.round(this.toMinutes(new Date().getTime()-date));
    },
    toMinutes: function(ms) {
        return ms/1000/60;
    },
    formatHour: function(h,nospace) {
        let space = nospace?"":"&nbsp;";
        if(h==0) return "12" + space +"AM";
        if(h==12) return "12" + space+"PM";
        if(h>12) return (h-12)+space+"PM";
        return h +space+"AM";
    },
    formatDateMonthDayYear: function(date, options, args) {
        if(isNaN(date.getUTCMonth())) return "Unknown date";
        var m = this.monthNames[date.getUTCMonth()];
        var d = date.getUTCDate();
        if(d<10) d = "0" +d;
        return m +" " + d +", " + date.getUTCFullYear();
    },
    formatDateYearMonth: function(date, options, args) {
        if(isNaN(date.getUTCMonth())) return "Unknown date";
        var m = this.monthNames[date.getUTCMonth()];
        return m +", " + date.getUTCFullYear();
    },    
    formatDateMonthDay: function(date, options, args) {
        if(isNaN(date.getUTCMonth())) return "Unknown date";
        var m = this.monthNames[date.getUTCMonth()];
        var d = date.getUTCDate();
        if(d<10) d = "0" +d;
        return m +" " + d;
    },
    formatDateMonDay: function(date, options, args) {
        if(isNaN(date.getUTCMonth())) return "NA";
        var m = this.monthNamesShort[date.getUTCMonth()];
        var d = date.getUTCDate();
        if(d<10) d = "0" +d;
        return m +"-" + d;
    },

    formatDateMDY: function(date, options, args) {
        if(isNaN(date.getUTCMonth())) return "Unknown date:" + date;
        var m = this.monthNamesShort[date.getUTCMonth()];
        var d = date.getUTCDate();
        if(d<10) d = "0" +d;
        return m +" " + d +", " + date.getUTCFullYear();
    },
    formatDateYYYYMMDD: function(date, options, args) {
        if(isNaN(date.getUTCMonth())) return "Unknown date:" + date;
        var m = (date.getUTCMonth() + 1);
        if(m<10) m = "0" + m;
        var d = date.getUTCDate();
        if(d<10) d = "0" +d;
        return date.getUTCFullYear() + "-" + m + "-" + d;
    },
    formatDateYYYYMMDDHHMM: function(date, options, args) {
        if(isNaN(date.getUTCMonth())) return "Unknown date:" + date;
        let month = (date.getUTCMonth() + 1);
        if(month<10) month = "0" + month;
        let d = date.getUTCDate();
        if(d<10) d = "0" +d;
        let h = date.getUTCHours();
        if(h<10) h = "0" + h;
        let minute = date.getUTCMinutes();
        if(minute<10) minute = "0" + minute;
        let hhmm=  h+":" +minute;
        return date.getUTCFullYear() + "-" + month + "-" + d+" " + hhmm;
    },
    formatDateYYYYMMDDHHMMSS: function(date, options, args) {
        if(isNaN(date.getUTCMonth())) return "Unknown date:" + date;
        var month = (date.getUTCMonth() + 1);
        if(month<10) month = "0" + month;
        var d = date.getUTCDate();
        if(d<10) d = "0" +d;
        var h = date.getUTCHours()+1;
        if(h<10) h = "0" + h;
        var minute = date.getUTCMinutes();
        if(minute<10) minute = "0" + minute;
        var seconds = date.getUTCSeconds();
        if(seconds<10) seconds = "0" + seconds;	
        let hhmmss=  h+":" +minute+":"+seconds;

        return date.getUTCFullYear() + "-" + month + "-" + d+" " + hhmmss;
    },


    formatDateYYYYMMDDHH: function(date, options, args) {
        if(isNaN(date.getUTCMonth())) return "Unknown date:" + date;
        var m = (date.getUTCMonth() + 1);
        if(m<10) m = "0" + m;
        var d = date.getUTCDate();
        if(d<10) d = "0" +d;
        var h = Utils.formatHour(date.getUTCHours(),true);
        return date.getUTCFullYear() + "-" + m + "-" + d+" " + h;
    },

    formatDateYYYYMM: function(date, options, args) {
        if(isNaN(date.getUTCMonth())) return "Unknown date:" + date;
        var m = (date.getUTCMonth() + 1);
        if(m<10) m = "0" + m;
        return date.getUTCFullYear() + "-" + m;
    },
    formatDateMMDD: function(date, delimiter) {
        if(isNaN(date.getUTCMonth())) return "Unknown date:" + date;
        let m = (date.getUTCMonth() + 1);
        if(m<10) m = "0" + m;
        let d = date.getUTCDate();
        if(d<10) d = "0" +d;
        return  m + (delimiter?delimiter:"-") + d;
    },
    formatDateHHMM: function(date, delimiter) {
        let h = date.getUTCHours();
        if(h<10) h = "0" + h;
        let m = date.getUTCMinutes();
        if(m<10) m = "0" + m;
        return  h+":" +m;
    },
    formatDateYYYY: function(date, options, args) {
        return date.getUTCFullYear();
    },
    formatDateYYYYWeek: function(date, options, args) {
        return date.getUTCFullYear() + " week:" + this.formatDateWeek(date);
    },
    formatDateWeek: function(date) { 
        var yearStart = new Date(Date.UTC(date.getUTCFullYear(),0,1));
        return   Math.ceil(( ( (date - yearStart) / 86400000) + 1)/7);
    },
    formatDateWithFormat(date, fmt,returnNullIfNotFound) {
        if(fmt==null) {
            if(returnNullIfNotFound) return null;
            fmt = "yyyymmdd";
        }
        let _fmt = fmt.toLowerCase();
        //Keep these formats for backwards compat
        if (_fmt == "yyyymmdd") {
            return Utils.formatDateYYYYMMDD(date);
        } else if (_fmt == "yyyymmddhh") { 
            return Utils.formatDateYYYYMMDDHH(date);
        } else if (_fmt == "yyyymmddhhmm") { 
            return Utils.formatDateYYYYMMDDHHMM(date);
        } else if (_fmt == "yyyymmddhhmmss") { 
            return Utils.formatDateYYYYMMDDHHMMSS(date);	    
        } else if (_fmt == "yyyymm") {
            return Utils.formatDateYYYYMM(date);
        } else if (_fmt == "yearmonth") {
            return Utils.formatDateYearMonth(date);
        } else if (_fmt == "monthdayyear") {
            return Utils.formatDateMonthDayYear(date);
        } else if (_fmt == "monthday") {
            return Utils.formatDateMonthDay(date);
        } else if (_fmt == "mon_day") {
            return Utils.formatDateMonDay(date);
        } else if (_fmt == "mdy") {
            return Utils.formatDateMDY(date);
        } else if (_fmt == "hhmm") {
            return Utils.formatDateHHMM(date);
        } else {
            return String(date.format(fmt,"UTC:",true));
        }
    },
    dateOptions:{
        weekday: 'long',
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        hour: 'numeric',
        minute: 'numeric'
    },
    formatDate: function(date, options, args) {
        if(true) return this.formatDateWithFormat(date,"yyyy-mm-dd HH:MM");
        if (!args) args = {};
        if (!options) {
            options = this.dateOptions;
        }
        let suffix = args.suffix;
        if (!suffix) suffix = "";
        else suffix = " " + suffix;
        if (args.timeZone) {
            options.timeZone = args.timeZone;
        } else {
            options.timeZone = "UTC";
        }
        return date.toLocaleDateString("en-US", options) + suffix;
    },
    incrementYear: function(date, by) {
        if (by == null) by = 1;
        date.setUTCFullYear(date.getUTCFullYear() + by);
        return date;
    },
    incrementMonth: function(date, by) {
        if (by == null) by = 1;
        while (by > 0) {
            if (date.getUTCMonth() == 11) {
                date.setUTCFullYear(date.getUTCFullYear() + 1);
                date.setUTCMonth(0);
            } else {
                date.setUTCMonth(date.getUTCMonth() + 1);
            }
            by--;
        }
        return date;
    },
    incrementDay: function(date, by) {
        if (by == null) by = 1;
        date.setTime(date.getTime() + (by * 1000 * 3600 * 24));
        return date;
    },
    getDayInYear: function(date) {
        return (Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate()) - Date.UTC(date.getUTCFullYear(), 0, 0)) / 24 / 60 / 60 / 1000;
    },
    monthShortNames: ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"],
    monthLongNames: ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"],
    getMonthShortNames: function() {
        return this.monthShortNames;
    },
    xparseDate: function(date) {
        date = date.replaceAll(/\+0000$/,"");
        return new Date(date);
    },

    dateRegex:new RegExp("(rel|now|today)(\\+|-)(.*)", 'i'),
    parseDate: function(s, roundUp, rel) {
        if (s == null) return null;
	if(s=='NA') return null;
        if((typeof s)=="number") {
            return new Date(s);
        }

        s = s.trim();
        if (s == "") return null;
        var match = s.match(this.dateRegex);
        var offset = 0;
        if (match != null) {
            offset = parseFloat(match[3]);
            if (match[2] == "-") offset = -offset;
            s = match[1];
        }
        var date = null;
        if (s == "now") {} else if (s == "rel") {
            if (rel == null) return null;
            date = new Date(rel.getTime());
        } else if (s == "today") {
            date = new Date(Date.now());
            date.setMilliseconds(0);
            date.setSeconds(0);
            date.setMinutes(0);
            if (roundUp)
                date.setHours(24);
        } else {
            let d = Date.parse(s);
            if(isNaN(d)) {
                let a = s.split(/[^0-9]/).map(s=>{ return parseInt(s, 10)});
                return new Date(a[0], a[1]-1 || 0, a[2] || 1, a[3] || 0, a[4] || 0, a[5] || 0, a[6] || 0);
            }
            return new Date(d);
        }
        if (offset != 0) {
            date.setDate(date.getDate() + offset);
        }
        return date;
    },
    makeDownloadFile: function(filename, text) {
        var element = document.createElement('a');
        element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(text));
        element.setAttribute('download', filename);
        element.style.display = 'none';
        document.body.appendChild(element);
        element.click();
        document.body.removeChild(element);
    },
    later: function(callback) {
        if (callback) {
            setTimeout(callback, 1);
        }
    },
    call: function(callback, arg1, arg2, arg3, arg4) {
        if (callback) {
            var args = [];
            if (Utils.isDefined(arg1)) args.push(arg1);
            if (Utils.isDefined(arg2)) args.push(arg2);
            if (Utils.isDefined(arg3)) args.push(arg3);
            if (Utils.isDefined(arg4)) args.push(arg4);
            callback.apply(this, args);
        }
        return arg1;
    },
    isTrue: function(v) {
	if(v===null) return false;
	return (String(v)=='true');
    },
    isDefined: function(v) {
        let ok = true;
        let a = Array.from(arguments).every(v=>{
            if(v===null || typeof v === 'undefined') {
                ok =false;
            }
            return ok;
        });
        return ok;
    },
    makeLabel: function(s,dontSplitOnCaps) {
        s  = String(s).trim();
	if(!dontSplitOnCaps) {
	    s = s.replace(/([a-z]+)([A-Z])/g,"$1 $2");
	}
	s = s.replace(/_/g,' ').replace(/  +/g,' ').replace(/-+/g,' ');
        s =  this.camelCase(s);
	return s;
    },
    makeID:function(s) {
	return Utils.makeId(s);
    },
    makeId: function(s) {
        s  = String(s);
        s = s.replace(/[^\x00-\x7F]/g, "_");
        s = s.replace(/&/g,"_");
        s = s.replace(/\./g, "_");
        s = s.replace(/[:\//]+/g, "_");
        s = s.replace(/_+$/,'');
        s = s.trim().toLowerCase().replace(/ /g,"_");
        return s;
    },    
    camelCase: function(s,firstLower) {
        if (!s) return s;
        let r = "";
        let toks = s.split(" ");
        for (let i = 0; i < toks.length; i++) {
            let tok = toks[i];
            let converted = tok.substring(0, 1);
            if(i>>0 || !firstLower)
                converted = converted.toUpperCase();
            if (tok.length > 1)
                converted += tok.substring(1).toLowerCase();
            if (r != "") r += " ";
            r += converted;
        }
        return r;
    },
    stripTags: function(s) {
        var regex = /(<([^>]+)>)/ig;
        return s.replace(regex, " ");
    },
    articles: "that,this,they,those,with,them,their,have,than,there,when,more,much,many,will,were".split(","),
    stopWords: ["not", "no", "will", "must", "just", "a", "about", "above", "after", "again", "against", "all", "am", "an", "and", "any", "are", "as", "at", "be", "because", "been", "before", "being", "below", "between", "both", "but", "by", "could", "did", "do", "does", "doing", "down", "during", "each", "few", "for", "from", "further", "had", "has", "have", "having", "he", "he'd", "he'll", "he's", "her", "here", "here's", "hers", "herself", "him", "himself", "his", "how", "how's", "i", "i'd", "i'll", "i'm", "i've", "if", "in", "into", "is", "it", "it's", "its", "itself", "let's", "me", "more", "most", "my", "myself", "nor", "of", "on", "once", "only", "or", "other", "ought", "our", "ours", "ourselves", "out", "over", "own", "same", "she", "she'd", "she'll", "she's", "should", "so", "some", "such", "than", "that", "that's", "the", "their", "theirs", "them", "themselves", "then", "there", "there's", "these", "they", "they'd", "they'll", "they're", "they've", "this", "those", "through", "to", "too", "under", "until", "up", "very", "was", "we", "we'd", "we'll", "we're", "we've", "were", "what", "what's", "when", "when's", "where", "where's", "which", "while", "who", "who's", "whom", "why", "why's", "with", "would", "you", "you'd", "you'll", "you're", "you've", "your", "yours", "yourself", "yourselves"],
    tokenizeWords: function(s, stopWords, extraStopWords, removeArticles) {
        if (!stopWords) {
            stopWords = this.stopWords;
        }
        var words = [];
        s=s.replace(/https?:\/\/[^ ]+/g," ");
        var toks = s.split(/[ ,\(\)\.:\;\n\?\-!\*]/);
        for (var i = 0; i < toks.length; i++) {
            var word = toks[i].trim();
            if (word == "") continue;
            var _word = word.toLowerCase();
            if (stopWords.includes(_word)) continue;
            if (extraStopWords && extraStopWords.includes(_word)) continue;
            if (removeArticles && this.articles.includes(_word)) continue;
            if (word.match(/^[0-9]+$/)) continue;
            words.push(word);
        }
        return words;
    },
    stringEquals: function(s1,s2) {
	if(s1===null && s2===null) return true;
	if(s1===null || s2===null) return false;
	return new String(s1).valueOf()===new String(s2).valueOf();
    },
    stringDefined: function() {
	for(let i=0;i<arguments.length;i++) {
	    let v = arguments[i];
            if (!Utils.isDefined(v)) return false;
            if (v == null || v == "") return false;
	}
        return true;
    },
    /** Return the first string that is defined in the arguments */
    getStringDefined: function() {
	for(let i=0;i<arguments.length;i++) {
	    if(Utils.stringDefined(arguments[i])) return arguments[i];
	}
	return null;
    },

    getDefined: function() {
	for(let i=0;i<arguments.length;i++) {
	    let v = arguments[i];
            if (Utils.isDefined(v)) return v;
	}
        return null;
    },

    tokenizeMacros:function(s,args,debug) {
        let opts = {
        };
        if(args) $.extend(opts,args);
        let hook   = opts.hook;
        let tokens = [];
        let tokenMap = {};
        let cnt = 0;
        while(s.length>0) {
            let idx = s.indexOf("${");
            if(idx<0) break;
            let prefix = s.substring(0,idx);
            if(prefix!="") {
                tokens.push({type:"string",s:prefix});
            }
            s = s.substring(idx);
            let inner = "";
            let cidx=0;
            let inquote=false;
            while(cidx<s.length) {
                let ch = s.charAt(cidx++);
                if(ch == '"' || ch == "'") {
                    if(inquote) inquote=false;
                    else inquote=true;
                    inner+=ch;
                    continue;
                }   
                if(ch == '}') {
                    if(!inquote) break;
                }
                inner+=ch;
            }
            if(cidx>s.length) break;
            s = s.substring(cidx);
            let macro = inner.substring(2);
            let attrs ={};
            idx = macro.indexOf(" ");
            let tag = "";
            if(idx<0) {
                tag = macro;
            } else {
                tag = macro.substring(0,idx).trim();
                let attrString = macro.substring(idx).trim();
                attrs = Utils.parseAttributes(attrString);
                if(debug)
                    console.dir(attrs);
            }


            let token = {id:String(cnt++), attrs:attrs,tag:tag,macro:macro};
            tokens.push(token);
            tokenMap[tag] = token;
        }
        if(s!="") {
            tokens.push({type:"string",s:s});
        }


        return {tokens:tokens,
                tokenMap: tokenMap,
                //This gets a modified version of the source string with:
                //s...${m0}...${m1} ...
                getAttributes: function(t) {
                    let token = this.tokenMap[t];
                    if(token) return token.attrs;
                    return null;
                },
                apply: function(source, debug, handler) {
                    //              if(debug) console.log("macro:" + JSON.stringify(source,null,2));
                    let cnt = 0;
                    let tokenFunc = t=>{
			let has = (key) =>{
			    return Utils.isDefined(t.attrs[key]) || Utils.isDefined(t.attrs[key.toLowerCase()]);
			}
			let get = (key) =>{
			    return t.attrs[key]?? t.attrs[key.toLowerCase()];
			}			
                        let value = source[t.tag];
			//The tag might be a regexp or a '|' delimited list
			if(!value) {
			    let keys = Object.keys(source);
			    keys.every(key=>{
				if(key.match(t.tag)) {
				    value = source[key];
				    return false;
				}
				if(t.tag.indexOf('|')) {
				    t.tag.split('|').every(tok=>{
					if(tok==key || key.match(tok)) {
					    value = source[key];
					    return false;
					}
					return true;
				    });
				}
				if(value) return false;
				return true;
			    });
			}
			//			console.log(t.tag +" " + value);
                        let s = "";
                        if(t.tag=="func") {
                            let f = t.attrs.f;
                            if(!f) {
                                return "&lt;no function defined&gt;";
                            }
                            let code = "function macroFunc() {\n"
                            for(a in source) {
                                if(a=="default") continue;
                                let v = source[a];
                                if((typeof v)!="number") v = '"' + v +'"';
                                code += a +"=" + v+";\n";
                            }
                            code+= "return " + f+";\n}\n"
                            code+="macroFunc()";
                            try {
                                value=  eval(code);
                            } catch(err) {
                                console.log("Error applying macro function:" + code +"\n" + err);
                                return "&lt;Error:" + err+"&gt;";
                            }
                        }
                        if(t.attrs["nonblank"]) {
                            if(String(value).length==0) return "";
                        }
                        if(!Utils.isDefined(value)) {
                            return "${" + t.macro+"}";
                        } 
                        if(value.getTime) {
			    let offset = t.attrs['offset'] ?? opts.timezoneOffset; 
			    let suffix = "";
			    if(t.attrs['localTime']) {
				let offset  = new Date().getTimezoneOffset()*60*1000;
				value = new Date(value.getTime()-offset)
				//				suffix = " " +  Intl.DateTimeFormat().resolvedOptions().timeZone;
				//				console.log(value.toTimeString(undefined, { xtimeZoneName: 'short' }));
				//				console.log(value.toTimeString());
				
			    }
                            return  Utils.formatDateWithFormat(value,t.attrs['format']||opts.dateFormat) +suffix;
                        } 
                        if(t.attrs["display"]) {
                            if(handler) {
                                let result = handler(t,value);
                                if(result!==false) return result;
                            }
                        }

                        if(t.attrs["missing"] && (value=="" || isNaN(value))) {
                            return  t.attrs["missing"]
                        } 
                        if(t.attrs["bar"]) {
                            let min  = t.attrs["min"]||0;
                            let max  = t.attrs["max"];                      
                            if(value=="" || isNaN(value)) {
                                return t.attrs["missing"] ||value;
                            }
                            if(Utils.isDefined(min,max)) {
                                let color  = t.attrs["color"]||"#4E79A7";
                                let width = t.attrs[ATTR_WIDTH]||"100%";
                                let height = parseFloat(t.attrs["height"]||12);
                                let percent = (100-100*(value-min)/(max-min))+"%";
                                let border = t.attrs["border"]||"1px solid #ccc";
                                let includeValue = t.attrs["includeValue"]||true;
                                let bar =  HU.div([ATTR_TITLE,value+"/"+max,
						   ATTR_STYLE,HU.css("display","inline-block","position","relative", "height",(height+2)+"px",ATTR_WIDTH,width,"border",border,"border-left","none")],
                                                  HU.div([ATTR_STYLE,HU.css("position","absolute","left","0px","right",percent,"height",height+"px","background",color)]));
                                if(includeValue) return HU.row([[ATTR_WIDTH,"1%"],value],bar);
                                return bar;

                                
                            }
                        }

                        if(t.attrs["stars"]) {
                            if(value=="" || isNaN(value)) {
                                return t.attrs["missing"] ||value;
                            }
                            let count  = t.attrs["count"] ||5;
                            let min  = t.attrs["min"]||0;
                            let max  = t.attrs["max"];                      
                            if(Utils.isDefined(min,max)) {
                                let color  = t.attrs["color"]||"#F9CF03";
                                let starsbase = "";
                                let stars = "";
                                for(let i=0;i<count;i++) {
                                    starsbase+=HU.getIconImage("far fa-star",null,[ATTR_STYLE,HU.css("font-size","10pt","color","#000")]);
                                    stars+=HU.getIconImage("fas fa-star",null,[ATTR_STYLE,HU.css("font-size","10pt","color",color)]);                                    
                                }
                                let percent = (100-100*(value-min)/(max-min))+"%";
                                let includeValue = t.attrs["includeValue"]||true;
                                let bar =  HU.div([ATTR_TITLE,value+"/"+max,
						   ATTR_STYLE,HU.css("display","inline-block","position","relative")],
                                                  starsbase+
                                                  HU.div([ATTR_STYLE,HU.css("white-space","nowrap","overflow-x","hidden","position","absolute","left","0px","right",percent,"top","0px","bottom","0px")], stars));
                                if(includeValue) return HU.row([[ATTR_WIDTH,"1%"],value],bar);
                                return bar;
                            }
                        }

                        if(Utils.isDefined(t.attrs["nan"])) {
			    if(isNaN(value)) {
				value = t.attrs["nan"];
			    }
			}

                        if(t.attrs["positiveTemplate"] || t.attrs["negativeTemplate"]) {
                            value = +value;
                            if(value>=0) {
                                if(t.attrs["negativeTemplate"]) {
                                    value = t.attrs["positiveTemplate"].replace("${value}",value);
                                }
                            } else if(t.attrs["negativeTemplate"]) {
                                if(t.attrs["doAbsolute"]) {
                                    value = -value;
                                }
                                value = t.attrs["negativeTemplate"].replace("${value}",value);
                            }
			}

                        if(t.attrs["youtube"]) {
                            if(value.trim().length==0) return null;
			    s+=Utils.embedYoutube(value, t.attrs);
                            return s;
                        }

                        if(t.attrs["list"]) {
                            value = String(value);
                            s+="<ul>"
                            value.split("\n").forEach(line=>{
                                line = line.trim();
                                if(line!="") {
                                    s+="<li> " + line
                                }
                            });
                            s+="</ul>"
                            return s;
                        }
                        if(t.attrs["pre"]) {
                            value = String(value);
                            if(value.trim()!="") 
                                value = "<pre class=thin_pre>" + value.trim()+"</pre>"
                            return value;
                        }
                        if(t.attrs["showUrl"]) {
                            let label = t.attrs['label'] || value;
                            value = HU.href(value,label,['target','_other',ATTR_STYLE,t.attrs[ATTR_STYLE]||'']);
                        }
                        if(t.attrs['delimiter'] && t.attrs['fields']) {
                            let l = [];
                            if(Utils.stringDefined(value)) l.push(value);
                            t.attrs['fields'].split(",").forEach(f=>{
                                let v=  source[f];
                                if(Utils.stringDefined(v)) l.push(v);
                            });
                            value = Utils.join(l,t.attrs['delimiter']);
                        }


                        if(t.attrs["urlField"]) {
                            let url =  source[t.attrs["urlField"]];
                            if(Utils.stringDefined(url)) {
                                value = HU.href(url,value);
                            }
                        }
                        if(t.attrs["offset1"]) {
                            value = value+ parseFloat(t.attrs["offset1"]);
                        }
                        if(t.attrs["scale"]) {
                            value = value* parseFloat(t.attrs["scale"]);
                        }
                        if(t.attrs["offset2"]) {
                            value = value+ parseFloat(t.attrs["offset2"]);
                        }
                        if(t.attrs["decimals"]) {
                            value = parseFloat(value);
                            let scale = Math.pow(10,t.attrs["decimals"]);
                            value = (Math.floor(value * scale) /scale);
                        }
                        if(t.attrs["format"]) {
                            let fmt = t.attrs["format"]
                            if(fmt == "comma") {
                                if(isNaN(value)) value=0;
                                else if(value!=0)
                                    value = Utils.formatNumberComma(value);
                            }
                        }
                        if(t.attrs["lowercase"]) {
                            value = String(value).toLowerCase();
                        }
                        if(t.attrs["uppercase"]) {
                            value = String(value).toUpperCase();
                        }                       
                        if(t.attrs["camelcase"]) {
                            value = Utils.camelCase(String(value), true);
                        }
                        if(t.attrs["trim"]) {
			    if(value.length>t.attrs["trim"]) {
				value = value.substring(0,t.attrs["trim"]);
				if(t.attrs["ellipsis"]) value+="...";
			    }
			}
                        if(t.attrs["replace"]) {
			    let rep = t.attrs["replace"];
			    if(rep=="_space_") rep = " ";
			    let s = t.attrs["with"] ?? " ";
			    if(s=="_nl_") s="\n";
			    else if(s=="\\n") s="\n";			    
			    let re = new RegExp(rep,"g");
                            value = value.replace(re,s);
			}
                        if(t.attrs["suffix"]) {
                            value = value+t.attrs["suffix"];
                        }
                        if(t.attrs["prefix"]) {
                            value = t.attrs["prefix"]+value;
                        }
                        if(t.attrs["toggle"]) {
                            //value = HtmlUtils.toggleBlock(t.attrs["label"]||"More", value,false);
                        }
                        if(t.attrs["image"]) {
                            if(Utils.stringDefined(value) && t.attrs['urlPrefix']) {
                                value  = t.attrs['urlPrefix']+value;
                            }

                            if(!Utils.stringDefined(value) && t.attrs['defaultUrl']) {
                                value = t.attrs['defaultUrl'];
                            }

                            if(value!="") {
                                let title = "";
                                if(t.attrs[ATTR_TITLE]) {
                                    title = t.attrs[ATTR_TITLE];
                                    for(a in source)
                                        title = title.replace("{" + a+"}",source[a]);
                                }
                                let attrs = [ATTR_TITLE, title,'data-caption',title];
                                if(t.attrs["height"]) {
                                    attrs.push("height");
                                    attrs.push(t.attrs["height"]);
                                } else {
                                    attrs.push(ATTR_WIDTH);
                                    attrs.push(t.attrs[ATTR_WIDTH]||"100%");
                                }
                                attrs.push("loading");
                                attrs.push("lazy");
                                let url = value;
                                value = HtmlUtils.image(url,attrs);
                                if(t.attrs['doPopup']) {
                                    if(!t.attrs['popupBase']) {
                                        t.attrs['popupBase'] = "gallery"+Utils.getUniqueId();
                                    }
                                    let base = t.attrs['popupBase'];
                                    value = HU.href(url,value,[ATTR_CLASS,"popup_image","data-fancybox","fancybox","data-caption",title,ATTR_TITLE,title]);
                                }
                            }
                        }

                        if(t.attrs["images"]) {
                            if(value!="") {
                                value=value.trim();
                                let images;
                                if(value.startsWith("[")) {
                                    images = JSON.parse(value);
                                } else {
                                    if(t.attrs["delimiter"]) {
                                        images = value.split(t.attrs["delimiter"]);
                                    } else {
                                        images = value.split(";");
                                    }
                                }
                                let title = "";
                                if(t.attrs[ATTR_TITLE]) {
                                    title = t.attrs[ATTR_TITLE];
                                    for(a in source)
                                        title = title.replace("{" + a+"}",source[a]);
                                }
                                let attrs = [ATTR_TITLE, title];
                                if(t.attrs["height"]) {
                                    attrs.push("height");
                                    attrs.push(t.attrs["height"]);
                                } else {
                                    attrs.push(ATTR_WIDTH);
                                    attrs.push(t.attrs[ATTR_WIDTH]||"100%");
                                }
                                console.log("VALUE:" + value);
                                value ="";
                                images.forEach(image=>{
                                    value += HtmlUtils.image(image,attrs);
                                });
                                console.log(value);
                            }
                        }


			if(t.attrs['convertDegrees']) {
			    let d = [
				['NNE', 11.25, 33.75],
				['NE',33.75, 56.25],
				['ENE',56.25, 78.75],
				['E',78.75, 101.25],
				['ESE',101.25, 123.75],
				['SE',123.75, 146.25],
				['SSE',146.25, 168.75],
				['S',168.75, 191.25],
				['SSW',191.25, 213.75],
				['SW',213.75, 236.25],
				['WSW',236.25, 258.75],
				['W',258.75,281.25],
				['WNW',281.25, 303.75],
				['NW',303.75,326.25],
				['NNW',326.25,348.75]];
			    let dir = 'N';
			    value = +value;
			    d.every(t=>{
				if(value>=t[1] && value<=t[2]) {
				    dir = t[0];
				    return false;
				}
				return true;
			    });
			    return dir;
			}




                        if(t.attrs["template"]) {
                            value = t.attrs["template"].replace("{value}",value);
                        }
                        if(t.attrs["templateif"]) {
                            let toks = t.attrs["templateif"].split(":");
                            let pattern = toks[0];
                            let template = toks.slice(1).join(":");
                            value = String(value);
                            if(value.match(pattern)) {
                                value = template.replace("{value}",value);
                            }
                        }



                        let maxLength = t.attrs['maxLength'] ??t.attrs['maxlength'];
                        if(maxLength && value.length>+maxLength) {
                            value = value.substring(0,+maxLength);
			    if(t.attrs['maxLengthSuffix']??t.attrs['maxlengthsuffix'])
				value  =value+t.attrs['maxLengthSuffix'];
			}

                        if(t.attrs['crop'] && value.length>+t.attrs['crop']) {
			    value = String(value).substring(0,t.attrs['crop']) +'...';
			}

                        if(t.attrs['cropLength'] && value.length>+t.attrs['cropLength']) {
                            let idx = +t.attrs['cropLength'];
                            while(idx>=0) {
                                if(value[idx]==' ' ||value[idx]=='\n' ||value[idx]=='\t') {
                                    break;
                                }
                                idx--;
                            }
                            if(idx==0) {
                                while(idx<value.length) {
                                    if(value[idx]==' ' ||value[idx]=='\n' ||value[idx]=='\t') {
                                        break;
                                    }
                                    idx++;
                                }
                            }
                            let id = Utils.getUniqueId();

                            let pre = value.substring(0,idx) +HU.span([ATTR_ID,id+"_ellipsis"], "...");
                            let post = HU.span([ATTR_ID,id+"_post",
						ATTR_STYLE,HU.css('display','none')], value.substring(idx));                       
                            let toggle = HU.div(['onclick',"Utils.toggleShowMore('" + id+"')",ATTR_ID,id,
						 ATTR_CLASS,'ramadda-showmore ' + CLASS_CLICKABLE], "Show More " + HU.getIconImage("fas fa-sort-down"));                   
                            value = pre + post + toggle;
                        }

			if(t.attrs['tableRow']) {
			    value = "<tr><td align=right>" + t.attrs['label']+":</td><td>" + value +"</td></tr>";
			}			    

			if(has('prefixLabel')) {
			    value = get('label')+': ' + value;
			}

                        if(has('maxHeight')) {
                            value =  HU.div([ATTR_STYLE,HU.css("display","inline-block",
							       "max-height",HU.getDimension(get('maxHeight')),
							       "overflow-y","auto")],value);
                        }

                        if(has("maxWidth")) {
                            value =  HU.div([ATTR_STYLE,HU.css("display","inline-block","white-space","nowrap",
							       "max-width",HU.getDimension(get('maxWidth')),
							       "overflow-x","auto")],value);
                        }
                        s+=value;
                        return s;
                    };
                    let s = "";
                    this.tokens.forEach(t=>{
                        if(t.type=="string") {
                            s+=t.s;
                        } else {
                            let v=null;
                            if(hook) v = hook(t,  source[t.tag]);
                            if(!v) v = tokenFunc(t);
                            if(!v) return;
                            if(v.trim().length>0 && t.attrs["toggle"]) {        
                                v = HU.toggleBlock(t.attrs["label"]||"More",v);
                            }

                            s+=v;
                        }
                    });
                    return s;
                },
                getText: function() {
                    let cnt = 0;
                    let s ="";
                    this.tokens.map(t=>{
                        if(t.type=="string") {
                            s+=t.s;
                        } else {
                            s+="${macro" + cnt+"}";
                            cnt++;
                        }
                    });
                    return s;
                }
               };
    },
    embedAudio: function(url) {
	let type = 'audio/mpeg';
	return HU.tag('audio',['controls',null],
		      HU.tag('source',['src',url,'type',type],'Your browser does not support the audio tag.'));
    },
    embedYoutube:function(url, attrs) {
	attrs  =attrs||{};
        let toks = url.match(/.*watch\?v=(.*)$/);
        if(!toks || toks.length!=2) {
	    toks = url.match(/.*youtu.be\/(.*)$/);
	}
        if(!toks || toks.length!=2) {
            return  HU.href(url,url);
        } 
        let id = toks[1];
        let autoplay  = attrs["autoplay"]||"false";
        let playerId = "video_1";
        let embedUrl = "//www.youtube.com/embed/" + id +
            "?enablejsapi=1&autoplay=" + (autoplay=="true"?"1":"0") +"&playerapiid=" + playerId;
        let s =  "";
	if(attrs['includeLink']) s +=HU.href(url,"Link") +"<br>";
        s+=  HU.tag('iframe',[ATTR_ID,'ytplayer', 'allow', 'autoplay; fullscreen','type','text/html','frameborder','0',
                              ATTR_WIDTH,attrs[ATTR_WIDTH]||640,ATTR_HEIGHT,attrs['height']||360, 
                              SRC,embedUrl
                             ]);
	return s;
    },

    toggleShowMore: function(id) {
        let toggle = $("#" + id);
        let open = toggle.attr('open');
        open = !open;
        toggle.attr('open',open);
        if(open) {
            $('#'+id+'_ellipsis').hide();
            $('#'+id+'_post').show();       
            toggle.html("Show Less " + HU.getIconImage("fas fa-sort-up"));                      
        } else {
            $('#'+id+'_ellipsis').show();
            $('#'+id+'_post').hide();       
            toggle.html("Show More " + HU.getIconImage("fas fa-sort-down"));                    
        }
    },
    formatNumberComma: function(number,decimals,debug) {
        if(!Utils.isDefined(number)) {
            return "NA";
        }           
        let whole = Math.floor(number);
        let rem = number-whole;
        let wholeFormatted = whole.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
        if(rem==0) {
            return wholeFormatted;
        } else {
            let a = Math.abs(number);
	    if(!Utils.isDefined(decimals)) {
		if(a>=9999)
                    decimals = 0;
		else if(a>=999)
                    decimals = 1;
		else if(a>=99)
                    decimals = 2;
		else if(a>=9)
                    decimals = 3;
		else
                    decimals = 4
	    }
            let x =  number_format(number,decimals);
	    if(debug)
		console.log(number,decimals,x);
            return x;
            //      return wholeFormatted +"." + String(Utils.formatNumber(rem)).replace("0\.","");
        }
    },
    formatFileLength:function(bytes) {
        if (bytes < 0) {
            return "";
        }
        if (bytes < 5000) {
            return Math.round(bytes) + " bytes";
        }
        if (bytes < 1000000) {
            bytes = (Math.round((bytes * 100) / 1000.0)) / 100.0;

            return Math.round(bytes) + " KB";
        }
        bytes = Math.round(((bytes * 100) / 1000000.0)) / 100.0;

        return bytes + " MB";
    },


    numberToString:null,
    trimDecimals:function(d,decimals) {
        if (decimals == 0) {
            return parseInt(d);
        }
	d = parseFloat(d);
        let i = parseInt((d * Math.pow(10, decimals)));
        return  (i / Math.pow(10, decimals));
    },
    roundDecimals: function(value, decimals,debug) {
        //create the NumberFormat for better performance
        if(this.numberToString==null) {
            this.numberToString = new Intl.NumberFormat('fullwide',{ useGrouping: false });
        }
	//      let s = value.toLocaleString('fullwide', { useGrouping: false });
        let s = this.numberToString.format(value);
        let v =  Number(Math.round(s+'e'+decimals)+'e-'+decimals);
        if(debug)
            console.log("decimals:" +s +" " + Math.round(s+'e'+decimals));
        return v;
    },
    formatNumber: function(number, toFloat,debug) {
        let s = this.formatNumberInner(number,debug);
        if (toFloat) return parseFloat(s);
        return s;
    },
    formatNumberInner: function(number,debug) {
        let anumber = Math.abs(number);
	let inumber = Math.floor(anumber);
        if (anumber == inumber) {
	    return String(Math.floor(number));
	}
        if (anumber > 1000) {
            return number_format(number, 0);
        } else if (anumber > 100) {
            return number_format(number, 1);
        } else if (anumber > 10) {
            return number_format(number, 2);
        } else if (anumber > 1) {
            return number_format(number, 3);
        } else {
            let decimals = "" + (number - Math.floor(number));
            let s = number_format(number, Math.min(decimals.length - 2, 5),debug);
            return s;
        }
    },
    isRealNumber: function(value) {
        return !(value == Number.POSITIVE_INFINITY || isNaN(value) || !Utils.isNumber(value) || !Utils.isDefined(value) || value == null);
    },
    isNumber: function(value) {
        if ((undefined === value) || (null === value)) {
            return false;
        }
	if(value==='') return false;
        if (typeof value == 'number') {
            return true;
        }
        return !isNaN(value - 0);
    },
    isType: function(value, typeName) {
        if (value && (typeof value == typeName)) {
            return true;
        }
        return false;
    },
    cleanId: function(id) {
        id = id.replace(/:/g, "_").replace(/\./g, "_").replace(/=/g, "_").replace(/\//g, "_").replace(/[\(\)]/g,"_");
        id = id.replace(/[^a-zA-Z0-9_]/g,"_");
        return id;
    },
    isMobile: function() {
        return /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);
    },
    isFalse: function(value) {
        if (!(typeof value == 'undefined')) {
            return value == false;
        }
        return false;
    },
    toFloat: function(s, dflt) {
        if (s == null || String(s).trim() == "") {
            if (!(typeof dflt == 'undefined')) {
                return dflt;
            }
            return NaN;
        }
        return parseFloat(s);
    },
    getUrlArgs: function(qs) {
        //http://stackoverflow.com/questions/979975/how-to-get-the-value-from-the-url-parameter
        qs = qs.split('+').join(' ');
        var params = {},
            tokens,
            re = /[?&]?([^=]+)=([^&]*)/g;

        while (tokens = re.exec(qs)) {
            params[decodeURIComponent(tokens[1])] = decodeURIComponent(tokens[2]);
        }
        return params;
    },
    checkTabs:function(html) {
        while (1) {
            var re = new RegExp("id=\"(tabId[^\"]+)\"");
            var m = re.exec(html);
            if (!m) {
                break;
            }
            var s = m[1];
            if (s.indexOf("-") < 0) {
                jQuery(function() {
                    jQuery('#' + s).tabs();
                });
            }
            var idx = html.indexOf("id=\"tabId");
            if (idx < 0) {
                break;
            }
            html = html.substring(idx + 20);
        }
    },



    initRangeSelect:function() {
        let select = $(".ramadda-range-select");
        let func = function() {
            let toId = $(this).attr("to-id");
            let val = $(this).val();
            if(val=="between")
                $("#"+toId).show();
            else
                $("#"+toId).hide();

        };
        select.each(func);
        select.change(func);
    },


    initNavbarPopup:function(id,args) {
	let opts = {
	    side:'right'
	}
	if(args) $.extend(opts,args);
	jqid(id).click(function(){
	    let popup = jqid(id+"_popup");
	    if(popup.is(':visible')) {
		popup.hide();
		return
	    }		
	    popup.css('display','inline-block');
	    let args  ={
                of: $(this),
                my: opts.side+' top',
                at: opts.side +' bottom',
                collision:'fit fit'
	    };
	    popup.show(400);	    
	    popup.position(args);
	});
    },

    initPageReload:function(time, id, showLabel) {
        let cbx = $("#" + id);
        let label = $("#" + id+"_label");
        if(cbx.length>0) {
            cbx.change(()=>{
                let text = "Reload" +HtmlUtils.span([ATTR_STYLE,"color:transparent"]," in 00  seconds")
                label.html(text);
                if(cbx.is(':checked')) {
                    Utils.checkPageReload(time,id,showLabel);
                }
            });
        }
        if(cbx.length==0  || cbx.is(':checked')) {
            Utils.checkPageReload(time,id,showLabel);
        }
    },
    checkPageReloadPending: false,
    checkPageReload:function(time, id, showLabel) {
        let cbx = $("#" + id);
        let label = $("#" + id+"_label");
        if(cbx.length>0  &&!cbx.is(':checked')) {
            return;
        }
        if(time<=0) {
            location.reload();
            return;
        }
        if(showLabel) {
            let text = "Reload in " +time+" seconds";
            label.html(text);
        } 
        if(time<=5) {
            label.css("background","#eee");
        } else {
            label.css("background","transparent");
        }
        if(!Utils.checkPageReloadPending) {
            Utils.checkPageReloadPending=true;
            setTimeout(()=>{
                Utils.checkPageReloadPending=false;
                Utils.checkPageReload(time-1,id,showLabel);
            },1000);
        }
    },
    areDisplaysReady: function() {
        if(!Utils.getPageLoaded()) {
            return 0;
        }
        if(Utils.displaysList.length==0) {
            return 1;
        }
        let allReady = true;
        Utils.displaysList.forEach(display=>{
            if(!display.isDisplayFinished) return;
            if(!display.isDisplayFinished()) {
                allReady = false;
            }
        });
        if(allReady) {
            return 1
        }
        return 0;
    },
    checkForResize: function() {
        /*
          When the window resizes then clear any past timeouts
          and setTimeout for 1 second to notify the displays
        */

        $(window).resize(()=>{
            if(this.pendingResizeTimeout) {
                clearTimeout(this.pendingResizeTimeout);
                this.pendingResizeTimeout = null;
            }
            let timeoutFunc = ()=>  {
                for (let i = 0; i < Utils.displaysList.length; i++) {
                    let display = Utils.displaysList[i];
                    if (display.handleWindowResize) {
                        display.handleWindowResize();
                    }
                }
                this.pendingResizeTimeout=null;
            };
            this.pendingResizeTimeout = setTimeout(timeoutFunc, 1000);
        });
    },
    displaysList:[],
    displaysMap:[],
    addDisplay: function(display) {
        this.displaysList.push(display);
        if(display.getId) {
            this.displaysMap[display.getId()] = display;
        }
        if (display.displayId) {
            this.displaysMap[display.displayId] = display;
        }
    },
    removeDisplay: function(display) {
        let index = this.displaysList.indexOf(display);
        if(index>=0)
            this.displaysList.splice(index,1);
        if(display.getId) {
            delete this.displaysMap[display.getId()];
        }
        if (display.displayId) {
            delete this.displaysMap[display.displayId];
        }
    },
    initDisplays: function() {
        //      console.log("initDisplays");
        this.displaysList.forEach(d=>{
            if(d.pageHasLoaded) {
                let t1 = new Date();
                //              console.log("\t calling pageHasLoaded:" + d.type); 
                d.pageHasLoaded();
                let t2= new Date();
                //              Utils.displayTimes("after pageHasLoaded:" + d.type,[t1,t2],true);
            }
        });
        //      console.timeEnd("initDisplays");
    },
    getPageLoaded: function() {
        return this.pageLoaded;
    },

    initBigText:function(bigText) {
	let text= bigText.html();
	let limit = bigText.attr('bigtext-length')??400;
	if(text.length<limit) return;
	let moreLabel = bigText.attr('bigtext-label-more')??'More...';
	let lessLabel = bigText.attr('bigtext-label-less')??'Less...';		
	let height = bigText.attr('bigtext-height')??'100px';
	let fadeId = HU.getUniqueId('fade_');
	bigText.css('padding-bottom','25px').css('max-height',height).css('overflow-y','hidden').css('position','relative');
	let fade = $(HU.div([ATTR_STYLE,HU.css('height',bigText.attr('bigtext-fade-height')??'50px'),ATTR_ID,fadeId,ATTR_CLASS,'ramadda-bigtext-fade'])).appendTo(bigText);
	let toggle = HU.div([ATTR_TITLE,'Expand',ATTR_CLASS,'ramadda-clickable ramadda-bigtext-toggle'], moreLabel);
	toggle = $(toggle).appendTo(bigText);
	let open=false;
	toggle.click(function() {
	    open = !open;
	    if(open) {
		fade.hide();
		toggle.html(lessLabel);
		bigText.css('max-height','1000px');
	    } else {
		toggle.html(moreLabel);
		bigText.css('max-height',height);
		fade.show();
	    }
	});
    },

    initContent: function(parent) {
        if (!parent) parent = "";
        else parent = parent + " ";
        //tableize
        try {
            let tables = $(parent + ".ramadda-table");
            if(tables.length) {
                HtmlUtils.formatTable(tables);
            }
        } catch (e) {
            console.log("Error formatting table:" + e);
            console.log(e.stack);
        }
        let bigText = $(parent + ".ramadda-bigtext");
	bigText.each(function() {
	    Utils.initBigText($(this));
	});


        let snippets = $(parent + ".ramadda-snippet-hover");
        snippets.each(function() {
            let snippet = $(this);
	    let snippetPopup;
	    let timeOut;
	    let html = snippet.html();
	    let clazz = 'ramadda-snippet-popup';
	    if(html.length>200) {
		clazz += ' ramadda-snippet-popup-large';
	    }
	    let target = $(this).attr('hover-target')?jqid($(this).attr('hover-target')):snippet.parent();
            target.parent().attr(ATTR_TITLE,'');
	    target.tooltip({
		content:()=>{return html;},
		position: { my: "left+15 top", at: "left top+30" },
		classes: {
		    "ui-tooltip": clazz
		},
		show: { 
                    effect: "slideDown", 
                    delay: 600,
		    duration:600
		},  
	    });
        });

        let pageTitle = $(parent+".ramadda-page-title");
        let headerCenter = $(parent +".ramadda-header-fixedtitle");
        if(pageTitle.length && headerCenter.length) {
            //There might be more than one so take the first
            pageTitle= pageTitle.first();
            headerCenter = headerCenter.first();
            $(window).scroll(()=>{
                let titleTop = pageTitle.offset().top;
                let bottom = headerCenter.offset().top + headerCenter.height();
                if(titleTop<bottom+10) {
                    if(!this.showingHeaderCenter) {
                        this.showingHeaderCenter = true; 
                        headerCenter.html(pageTitle.html());
                        pageTitle.html("&nbsp;");
                    }
                } else {
                    if(this.showingHeaderCenter) {
                        this.showingHeaderCenter = false
                        pageTitle.html(headerCenter.html());
                        headerCenter.html("&nbsp;");
                    }
                }
            });
        }
        //Buttonize
        $(parent + ':submit').button().click(function(event) {});
        $(parent + '.ramadda-button').button().click(function(event) {});

        //Headers
        let headings = $(parent + '.ramadda-linkable');

        headings.mouseenter(function(){
            let id = $(this).attr(ATTR_ID);
            if(!id) return;
            $("#" + id +"-hover").html(HtmlUtils.getIconImage("fa-link",null,[ATTR_STYLE,"font-size:10pt;"]));
            $("#" + id +"-hover").show();
        });
        headings.click(function(){
            let id = $(this).attr(ATTR_ID);
            if(id) {
                HU.scrollToAnchor(id,-50);
            }
        });
        headings.mouseleave(function(){
            let id = $(this).attr(ATTR_ID);
            if(!id) return;
            $("#" + id +"-hover").hide();
        });     
        //menuize
        /*
          $(".ramadda-pulldown").selectBoxIt({});
        */
        /* for select menus with icons */
	//iconmenu
	let menus = $(parent + ".ramadda-pulldown-with-icons");
	menus.each(function() {
	    let width = $(this).attr('width');
	    $(this).iconselectmenu({width:width});
	    $(this).addClass("ui-menu-icons ramadda-select-icon");
	});
    },
    setMenuValue:function(menu,value) {
	menu.val(value);
	if(menu.iconselectmenu && menu.iconselectmenu('instance')) {
            menu.iconselectmenu('refresh');
        }
    },



    searchLastInput:"",
    searchAscending:false,
    searchCnt:0,
    searchSuggestInit:function(id, hereId, type, icon, resultsId) {
        let input = $("#"+ id);
        let here = $("#"+ hereId);      
        if(!Utils.isDefined(icon)) icon = true;
        let submitForm = false;
        if(!resultsId) {
            submitForm = true;
            resultsId = HU.getUniqueId();
            let width = input.width();
            let results = HU.div([ATTR_ID,resultsId,
				  ATTR_STYLE,HU.css("border","0px",ATTR_WIDTH,width+"px","position","absolute"),
				  ATTR_CLASS,'ramadda-popup ramadda-search-popup'],"");
            input.parent().append(results);
        }

        let results = $("#" + resultsId);
        let closer = () =>{
            results.slideUp(250);
        };

        let doSearch = () =>{
            let newVal = input.val()||"";
            this.searchCnt++;
            if(this.pendingSearch) {
                clearTimeout(this.pendingSearch);
                this.pendingSearch = null;
            }       

            if(newVal.length==0) {
                closer();
                return;
            }

            //wait a bit
            this.pendingSearch = setTimeout(()=> {
                this.pendingSearch = null;
                Utils.doSearchSuggest(this.searchCnt, input, here, results, type,submitForm);
            },200);
        };
        if(here.length) {
            here.change(doSearch);
        }

        Utils.searchLastInput = input.val();
        input.keyup(e=> {
            let keyCode = e.keyCode || e.which;
	    //          console.log("k:" + keyCode);
            if (keyCode == 27) {
                closer();
                return;
            }

            if (keyCode == 13) {
                closer();
                return;
            }
            e.stopPropagation();
            doSearch();
        });
    },

    doSearchSuggest:function(searchCnt,input, here, results, type, submitForm) {
        let _this = this;
        let newVal = input.val()||"";
        Utils.searchLastInput = newVal;
        let url = RamaddaUtil.getUrl("/search/suggest?text=" + encodeURIComponent(newVal));
        if (type) url += "&type=" + type;
        if(here.length>0 && here.is(':checked') && ramaddaThisEntry)
            url +="&ancestor=" + ramaddaThisEntry;
        url+="&ascending=" + Utils.searchAscending;
        url+="&orderby=createdate";
        let jqxhr = $.getJSON(url, function(data) {
            if(searchCnt!=_this.searchCnt) {
                return;
            }
            let opener = () =>{
                results.slideDown(400);
            };      

            if (data.values.length == 0) {
                results.html(HtmlUtils.div([ATTR_CLASS, "ramadda-search-suggestion "],"No results"))
                opener();
                return;
            }
            let html = "";
            let even = true;
            data.values.forEach((value,i) =>{
                let name = value.name;
                let id = value.id;
                let v = name.replace(/\"/g, "_quote_");
		
                let entryLink =  HU.href(RamaddaUtil.getUrl("/entry/show?entryid=" + id),
					 HU.getIconImage(value.icon||icon_blank16,[ATTR_WIDTH,ramaddaGlobals.iconWidth]) + SPACE+name,[ATTR_TITLE,"View entry",'data-type',value.typeName,'data-name',value.name,'data-icon',value.icon,
																       ATTR_STYLE,HU.css("display","inline-block",ATTR_WIDTH,"100%"), ATTR_CLASS,"ramadda-highlightable"]);
                let searchLink;
                if(submitForm) {
                    searchLink =  HU.span([ATTR_CLASS,"ramadda-highlightable ramadda-search-input","index",i,
					   ATTR_TITLE,"Search for"],HtmlUtils.getIconImage("fa-search"));
                } else {
                    searchLink =  HU.href(RamaddaUtil.getUrl( "/search/do?text=" + encodeURIComponent(v)),HtmlUtils.getIconImage("fa-search"),[ATTR_TITLE,"Search for text: " + value.name]);
                }
                let row =  searchLink +  SPACE + entryLink;
                //                      html += HtmlUtils.div([ATTR_CLASS, 'ramadda-search-suggestion ' + (even ? 'ramadda-row-even' : 'ramadda-row-odd')], row);
                html += HtmlUtils.div([ATTR_CLASS, 'ramadda-search-suggestion '], row);                      
                html +="\n";
                even = !even;
            });
            results.html(html);
	    results.find('.ramadda-highlightable').tooltip({
		show: {
		    delay: 1000,
		    duration: 300
		},
		content: function() {
		    let name = $(this).attr('data-name');
		    return 'Click to view entry<br>'+
			HU.image($(this).attr('data-icon'),[ATTR_WIDTH,ramaddaGlobals.iconWidth])+' ' +
			HU.b(name)+
			HU.div([],'Type: ' + $(this).attr('data-type'));
		}});

            if(submitForm) {
                let links = results.find(".ramadda-search-input");
                links.css("cursor","pointer");
                links.click(function(e) {
                    e.stopPropagation();
                    let v = data.values[$(this).attr("index")].name;
                    input.val(v);
                    input.closest("form").submit();

                });
            }
            opener();
        }).fail(function(jqxhr, textStatus, error) {
            console.log("fail:" + textStatus);
        });
    },

    searchLink:function() {
        let input = $("#popup_search_input");
        let val = input.val().trim();
        let url = $(this).attr('url');
        if (val != "") {
            url += "?text=" + encodeURIComponent(val);
        }
        window.location = url;
    },

    searchPopup:function(id,anchor) {
        anchor = id;
	//      anchor = anchor || id;
        let value = Utils.searchLastInput||"";
        let form = "<form action='" + RamaddaUtil.getUrl('/search/do')+"'>";
        let searchInput = HU.tag('input',['value', value, ATTR_PLACEHOLDER,'Search text', 'autocomplete','off','autofocus','true',ATTR_ID,'popup_search_input',ATTR_CLASS, 'ramadda-search-input',
					  ATTR_STYLE,HU.css('margin-left','4px', 'padding','2px',ATTR_WIDTH,'250px','border','0px'),'name','text']);
	let right = '';
        if(ramaddaThisEntry) {
            right=HU.span([ATTR_STYLE,HU.css('margin-right','5px'),ATTR_TITLE,"Search under this entry"],
			  HU.checkbox("popup_search_here",['name','ancestor', 'value',ramaddaThisEntry],false) +HU.tag("label",[ATTR_CLASS,CLASS_CLICKABLE, "for","popup_search_here"],HU.div([ATTR_STYLE,'margin-left:5px;'], HU.getIconImage('fas fa-folder-tree'))));
        }
	form+=HU.leftCenterRight(searchInput,'',right);
        form +="</form>";

        let linksId = HU.getUniqueId();
	let formLink = 
	    HU.link(RamaddaUtil.getUrl('/search/form'),
		    HU.span([ATTR_CLASS,CLASS_CLICKABLE],
			    /*HU.getIconImage('fa-solid fa-list-check') +HU.space(1) +*/HU.span([],'Search Form')),
		    [ATTR_TITLE, 'Go to search form', ATTR_STYLE,HU.css('xcolor','#888','xfont-size','16px')]);

	let typeLink = 
	    HU.link(RamaddaUtil.getUrl('/search/type'),
		    HU.span([ATTR_TITLE, 'Go to type search form',
			     ATTR_CLASS,CLASS_CLICKABLE],  /*HU.getIconImage('fa-solid fa-t')+' '+*/'Types'));

        let links =  HU.div([ATTR_ID, linksId,ATTR_STYLE,HU.css('text-align','right','font-size','12px')],
			    formLink+HU.space(1)+'|'+HU.space(1)+typeLink);
        let resultsId = HU.getUniqueId('searchresults');
        let results = HU.div([ATTR_ID,resultsId,ATTR_CLASS,'ramadda-search-popup-results']);
        let html = HU.div([ATTR_CLASS,"ramadda-search-popup"],form+results);
        let icon = $("#" + id);
        let dialog = this.dialog = HU.makeDialog({content:html,my:"right top",at:"right bottom",title:links,anchor:anchor,draggable:true,header:true,inPlace:false});
        $("#" + linksId).find(".ramadda-link").click(Utils.searchLink);
        let input = $("#popup_search_input");
        input.mousedown(function(evt) {
            evt.stopPropagation();
        });
	input.keydown(function(event) {
	    if(event.key=='Escape') dialog.remove();
	});
        Utils.searchSuggestInit('popup_search_input', 'popup_search_here',null, true, resultsId);
        input.focus();
    },
    handleKeyPress:function(event) {
        HtmlUtils.getTooltip().hide();
    },
    handleMouseDown:function(event) {
        if (HtmlUtils.hasPopupObject() || Utils.tooltipObject) {
            setTimeout(() => {
                if(Utils.tooltipObject) {
                    Utils.tooltipObject.hide();
                    Utils.tooltipObject = null;
                }
                if(HtmlUtils.hasPopupObject()) {
                    let thisId = HtmlUtils.getPopupObject().attr(ATTR_ID);
                    if (HtmlUtils.checkToHidePopup() && thisId == HtmlUtils.getPopupObject().attr(ATTR_ID)) {
                        HtmlUtils.hidePopupObject();
                    }
                }
            }, 250);
        }
        Utils.mouseIsDown = true;
        Utils.mouseMoveCnt = 0;
        return true;
    },
    handleMouseUp: function(event) {
        event = GuiUtils.getEvent(event);
        Utils.mouseIsDown = false;
        GuiUtils.setCursor('default');
        let obj = $("#ramadda-floatdiv");
        if (obj.length) {
            let dragSourceObj = Utils.entryDragInfo?GuiUtils.getDomObject(Utils.entryDragInfo.dragSource):null;
            if (dragSourceObj) {
                let tox = GuiUtils.getLeft(dragSourceObj.obj);
                let toy = GuiUtils.getTop(dragSourceObj.obj);
                let fromx = parseInt(obj.css("left"));
                let fromy = parseInt(obj.css("top"));
                let steps = 10;
                let dx = (tox - fromx) / steps;
                let dy = (toy - fromy) / steps;
                Utils.flyBackAndHide('ramadda-floatdiv', 0, steps, fromx, fromy, dx, dy);
            } else {
                obj.hide();
            }
            Utils.entryDragInfo = null;
            
        }
        return true;
    },
    flyBackAndHide: function(id, step, steps, fromx, fromy, dx, dy) {
        var obj = GuiUtils.getDomObject(id);
        if (!obj) {
            return;
        }
        step = step + 1;
        obj.style.left = fromx + dx * step + "px";
        obj.style.top = fromy + dy * step + "px";
        var opacity = 80 * (steps - step) / steps;
        if (step < steps) {
            let f = ()=>{
                Utils.flyBackAndHide(id,step,steps,fromx,fromy, dx,dy);
            };
            setTimeout(f, 30);
        } else {
            setTimeout(()=>{Utils.finalHide(id)}, 150);
        }
    },
    handleMouseMove:function(event) {
        event = GuiUtils.getEvent(event);
        if (Utils.entryDragInfo && Utils.mouseIsDown) {
            Utils.mouseMoveCnt++;
            var obj = $("#ramadda-floatdiv");
            if (Utils.mouseMoveCnt == 6) {
                GuiUtils.setCursor('move');
            }
            if (Utils.mouseMoveCnt >= 6 && obj.length) {
                Utils.moveFloatDiv(GuiUtils.getEventX(event), GuiUtils.getEventY(event));
            }
        }
        return true
    },


    finalHide:function(id) {
        var obj = GuiUtils.getDomObject(id);
        if (!obj) {
            return;
        }
        hideObject(obj);
        obj.style.filter = "alpha(opacity=80)";
        obj.style.opacity = "0.8";
    },
    centerDiv:function(c) {
	return this.div([ATTR_STYLE,'text-align:center;'],
			this.div([ATTR_STYLE,'display:inline-block;text-align:left;'],
				 c));
    },
    moveFloatDiv:function(x, y) {
        let obj = $("#ramadda-floatdiv");
        if (obj.length) {
            let visible = obj.css("display")!="none";
            if (!visible) {
                obj.show();
                let html = "";
                if (Utils.entryDragInfo) {
                    html = Utils.entryDragInfo.getHtml();
                }               
                obj.html(html + "<br>Drag to a group to copy/move/associate");
            }
            obj.css("top",y).css("left",x+10);
        }
    },
    framesClick:function(listId,viewId,listEntry, template) {
	let url = listEntry.attr('data-url');			      
	let label = listEntry.attr('data-label');
	jqid(listId).find('.ramadda-frames-entry').removeClass('ramadda-frames-entry-active');
	listEntry.addClass('ramadda-frames-entry-active');
	let href = HU.href(url,
			   HU.getIconImage('fa-solid fa-link') +  " " +  label,
			   [ATTR_TARGET,'_link',
			    ATTR_CLASS,CLASS_CLICKABLE]);
        jqid(viewId+'_header_link').html(href);
        if (template && template!='default')
            url = url + "&template=" + template;
	jqid(viewId).attr("src", url);
    },
    framesInit:function(listId,viewId,template) {
	let list = jqid(listId);
	list.find('.ramadda-frames-entry').click(function() {
	    Utils.framesClick(listId,viewId,$(this),template);
	});

	let header = jqid(viewId+'_leftheader');
	header.append(HU.div([ATTR_CLASS,'ramadda-frames-nav'],
			     HU.span([ATTR_TITLE,'View previous',ATTR_CLASS,'ramadda-clickable ramadda-frames-nav-link ramadda-frames-nav-link-prev','data-nav','prev'],
				     HU.getIconImage('fas fa-caret-left',null,
						     [ATTR_STYLE,'font-size:130%']))+
			     HU.space(1) +
			     HU.span([ATTR_TITLE,'View next',ATTR_CLASS,'ramadda-clickable ramadda-frames-nav-link ramadda-frames-nav-link-next','data-nav','next'],
				     HU.getIconImage('fas fa-caret-right',null,
						     [ATTR_STYLE,'font-size:130%']))));			     
	let navClick=nav=>{
	    //	    nav.focus();
	    let dir = nav.attr('data-nav');
	    let active = list.find('.ramadda-frames-entry-active');
	    let next;
	    if(dir=='next') next=active.nextAll('.ramadda-frames-entry');
	    else next=active.prevAll('.ramadda-frames-entry');	    
	    if(next.length>0)
		Utils.framesClick(listId,viewId,next.first(),template);
	};
	header.find('.ramadda-frames-nav-link').click(function() {
	    navClick($(this));
	});
    },
    copyText: function(str) {
        const el = document.createElement('textarea');
        el.value = str;
        document.body.appendChild(el);
        el.select();
        document.execCommand('copy');
        document.body.removeChild(el);
    },
    foregroundColors: {
        purple: "white",
    },
    getContrastYIQ: function(hexcolor){
        //From: https://stackoverflow.com/questions/11867545/change-text-color-based-on-brightness-of-the-covered-background-area
        hexcolor = hexcolor.replace("#", "");
        var r = parseInt(hexcolor.substr(0,2),16);
        var g = parseInt(hexcolor.substr(2,2),16);
        var b = parseInt(hexcolor.substr(4,2),16);
        var yiq = ((r*299)+(g*587)+(b*114))/1000;
        return (yiq >= 128) ? 'black' : 'white';
    },
    getForegroundColor: function(c) {
        if(!c) return null;
        if(c.match("rgb")) {
	    c = Utils.rgbToHex(c);
	}
        if(!c) return "#000";
        if(!this.foregroundColors[c] && c.startsWith("#")) return this.getContrastYIQ(c);
        return this.foregroundColors[c] ||"#000";
    },
    darkerColor: function(c,amt) {
        if(!c) return c;
        return this.pSBC(amt||-0.5,c);
    },
    brighterColor: function(c,amt) {
        if(!c) return c;
        return this.pSBC(amt||0.5,c);
    },    
    //From  https://stackoverflow.com/questions/5560248/programmatically-lighten-or-darken-a-hex-color-or-rgb-and-blend-colors
    //p:percent -1 - 1, co: color to darken/lighten, c1: if given then blend c0 and c1, l: linear blending if true  use linear
    pSBC: function(p,c0,c1,l) {
        c0 = this.colorToRgb[c0] || c0;
        c1 = this.colorToRgb[c1] || c1;
        let r,g,b,P,f,t,h,i=parseInt,m=Math.round,a=typeof(c1)=="string";
        if(typeof(p)!="number"||p<-1||p>1||typeof(c0)!="string"||(c0[0]!='r'&&c0[0]!='#')||(c1&&!a)) {
            return null;
        }
        if(!this.pSBCr)this.pSBCr=(d)=>{
            let n=d.length,x={};
            if(n>9){
                [r,g,b,a]=d=d.split(","),n=d.length;
                if(n<3||n>4)return null;
                x.r=i(r[3]=="a"?r.slice(5):r.slice(4)),x.g=i(g),x.b=i(b),x.a=a?parseFloat(a):-1
            } else{ 
                if(n==8||n==6||n<4)return null;
                if(n<6)d="#"+d[1]+d[1]+d[2]+d[2]+d[3]+d[3]+(n>4?d[4]+d[4]:"");
                d=i(d.slice(1),16);
                if(n==9||n==5)x.r=d>>24&255,x.g=d>>16&255,x.b=d>>8&255,x.a=m((d&255)/0.255)/1000;
                else x.r=d>>16,x.g=d>>8&255,x.b=d&255,x.a=-1
            }
            return x};
        h=c0.length>9,h=a?c1.length>9?true:c1=="c"?!h:false:h,f=this.pSBCr(c0),P=p<0,t=c1&&c1!="c"?this.pSBCr(c1):P?{r:0,g:0,b:0,a:-1}:{r:255,g:255,b:255,a:-1},p=P?p*-1:p,P=1-p;
        if(!f||!t)return null;
        if(l)r=m(P*f.r+p*t.r),g=m(P*f.g+p*t.g),b=m(P*f.b+p*t.b);
        else r=m((P*f.r**2+p*t.r**2)**0.5),g=m((P*f.g**2+p*t.g**2)**0.5),b=m((P*f.b**2+p*t.b**2)**0.5);
        a=f.a,t=t.a,f=a>=0||t>=0,a=f?a<0?t:t<0?a:a*P+t*p:0;
        if(h)return"rgb"+(f?"a(":"(")+r+","+g+","+b+(f?","+m(a*1000)/1000:"")+")";
        else return"#"+(4294967296+r*16777216+g*65536+b*256+(f?m(a*255):0)).toString(16).slice(1,f?undefined:-2)
    },
    colorToRgb:   {"aliceblue":"#f0f8ff","antiquewhite":"#faebd7","aqua":"#00ffff","aquamarine":"#7fffd4","azure":"#f0ffff",
                   "beige":"#f5f5dc","bisque":"#ffe4c4","black":"#000000","blanchedalmond":"#ffebcd","blue":"#0000ff","blueviolet":"#8a2be2","brown":"#a52a2a","burlywood":"#deb887",
                   "cadetblue":"#5f9ea0","chartreuse":"#7fff00","chocolate":"#d2691e","coral":"#ff7f50","cornflowerblue":"#6495ed","cornsilk":"#fff8dc","crimson":"#dc143c","cyan":"#00ffff",
                   "darkblue":"#00008b","darkcyan":"#008b8b","darkgoldenrod":"#b8860b","darkgray":"#a9a9a9","darkgreen":"#006400","darkkhaki":"#bdb76b","darkmagenta":"#8b008b","darkolivegreen":"#556b2f",
                   "darkorange":"#ff8c00","darkorchid":"#9932cc","darkred":"#8b0000","darksalmon":"#e9967a","darkseagreen":"#8fbc8f","darkslateblue":"#483d8b","darkslategray":"#2f4f4f","darkturquoise":"#00ced1",
                   "darkviolet":"#9400d3","deeppink":"#ff1493","deepskyblue":"#00bfff","dimgray":"#696969","dodgerblue":"#1e90ff",
                   "firebrick":"#b22222","floralwhite":"#fffaf0","forestgreen":"#228b22","fuchsia":"#ff00ff",
                   "gainsboro":"#dcdcdc","ghostwhite":"#f8f8ff","gold":"#ffd700","goldenrod":"#daa520","gray":"#808080","green":"#008000","greenyellow":"#adff2f",
                   "honeydew":"#f0fff0","hotpink":"#ff69b4",
                   "indianred ":"#cd5c5c","indigo":"#4b0082","ivory":"#fffff0","khaki":"#f0e68c",
                   "lavender":"#e6e6fa","lavenderblush":"#fff0f5","lawngreen":"#7cfc00","lemonchiffon":"#fffacd","lightblue":"#add8e6","lightcoral":"#f08080","lightcyan":"#e0ffff","lightgoldenrodyellow":"#fafad2",
                   "lightgrey":"#d3d3d3","lightgreen":"#90ee90","lightpink":"#ffb6c1","lightsalmon":"#ffa07a","lightseagreen":"#20b2aa","lightskyblue":"#87cefa","lightslategray":"#778899","lightsteelblue":"#b0c4de",
                   "lightyellow":"#ffffe0","lime":"#00ff00","limegreen":"#32cd32","linen":"#faf0e6",
                   "magenta":"#ff00ff","maroon":"#800000","mediumaquamarine":"#66cdaa","mediumblue":"#0000cd","mediumorchid":"#ba55d3","mediumpurple":"#9370d8","mediumseagreen":"#3cb371","mediumslateblue":"#7b68ee",
                   "mediumspringgreen":"#00fa9a","mediumturquoise":"#48d1cc","mediumvioletred":"#c71585","midnightblue":"#191970","mintcream":"#f5fffa","mistyrose":"#ffe4e1","moccasin":"#ffe4b5",
                   "navajowhite":"#ffdead","navy":"#000080",
                   "oldlace":"#fdf5e6","olive":"#808000","olivedrab":"#6b8e23","orange":"#ffa500","orangered":"#ff4500","orchid":"#da70d6",
                   "palegoldenrod":"#eee8aa","palegreen":"#98fb98","paleturquoise":"#afeeee","palevioletred":"#d87093","papayawhip":"#ffefd5","peachpuff":"#ffdab9","peru":"#cd853f","pink":"#ffc0cb","plum":"#dda0dd","powderblue":"#b0e0e6","purple":"#800080",
                   "rebeccapurple":"#663399","red":"#ff0000","rosybrown":"#bc8f8f","royalblue":"#4169e1",
                   "saddlebrown":"#8b4513","salmon":"#fa8072","sandybrown":"#f4a460","seagreen":"#2e8b57","seashell":"#fff5ee","sienna":"#a0522d","silver":"#c0c0c0","skyblue":"#87ceeb","slateblue":"#6a5acd","slategray":"#708090","snow":"#fffafa","springgreen":"#00ff7f","steelblue":"#4682b4",
                   "tan":"#d2b48c","teal":"#008080","thistle":"#d8bfd8","tomato":"#ff6347","turquoise":"#40e0d0",
                   "violet":"#ee82ee",
                   "wheat":"#f5deb3","white":"#ffffff","whitesmoke":"#f5f5f5",
                   "yellow":"#ffff00","yellowgreen":"#9acd32"},

    addAlphaToColor: function(c, alpha) {

        c = this.colorToRgb[c] || c;
        if(!Utils.isDefined(alpha)) {
	    alpha = "0.5";
	}

        if(c.indexOf("#")==0) {
            let rgb = Utils.hexToRgb(c);
            if(rgb) {
                c = "rgba(" + rgb.r+"," + rgb.g +"," + rgb.b+"," + alpha+")";
            }
            return c;
        }

        let c2 = c.replace(/rgba? *\(([^,]+),([^,]+),([^,\)]+).*/,'rgba($1,$2,$3,' + alpha+')');
        return c2;
    },

    enumTypeCount: -1,
    //    enumColorPalette:['#6B7280 ',"rgb(141, 211, 199)", "rgb(255, 255, 179)", "rgb(190, 186, 218)", "rgb(251, 128, 114)", "rgb(128, 177, 211)", "rgb(253, 180, 98)", "rgb(179, 222, 105)", "rgb(252, 205, 229)", "rgb(217, 217, 217)", "rgb(188, 128, 189)", "rgb(204, 235, 197)", "rgb(255, 237, 111)"],
    enumColorPalette:
    [
	"#9CA3AF", // Muted Slate (softer gray-blue)
	"#BDB7D0", // Muted Lavender (desaturated lavender)
	"#D8A29D", // Muted Coral (muted coral pink)
	"#BCCAB3", // Muted Sage (soft, grayish-green)
	"#D9C7A8", // Muted Beige (light beige)
	"#C4A69A", // Muted Terracotta (soft, muted rust)
	"#E7C4B0", // Muted Peach (muted peach)
	"#9CAAC0", // Muted Steel Blue (soft steel blue)
	"#A8A895", // Muted Olive (faded olive green)
	"#D4A5A4"  // Muted Blush (soft, muted pink)
    ],
    enumColors: {},
    getEnumColor:function(type) {
        if(type.color) return type.color;
        if(!this.enumColors[type]) {
            this.enumTypeCount++;
            if(this.enumTypeCount>=this.enumColorPalette.length)
                this.enumTypeCount = 0;
            this.enumColors[type] = this.enumColorPalette[this.enumTypeCount];
        }
        return this.enumColors[type];
    },

    hexToRgb:function(hex) {
        var result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
        return result ? {
            r: parseInt(result[1], 16),
            g: parseInt(result[2], 16),
            b: parseInt(result[3], 16)
        } : null;
    },
    componentToHex:function(c) {
        if(typeof c=="string") c = +c;
        var hex = c.toString(16);
        return hex.length == 1 ? "0" + hex : hex;
    },
    rgbToHex:function(r, g, b) {
        //its rgb,r,g,b);
        if(g==null) {
            let result = /.*\(([^,]+),([^,]+),(.*)\).*/i.exec(r);	    
            if(!result) return null;
            r = result[1].trim(); g = result[2].trim(); b = result[3].trim();
        }
        if(!r || !g || !b) return null;
        return "#" + Utils.componentToHex(r) + Utils.componentToHex(g) + Utils.componentToHex(b);
    },
    hexStringToInt:function(s) {
	return hex = parseInt(s.replace(/^#/, ''), 16);
    }
};






var GuiUtils = {
    getProxyUrl: function(url) {
        let base = RamaddaUtil.getUrl( "/proxy?trim=true&url=");
        return base + encodeURIComponent(url);
    },

    showingError: false,
    pageUnloading: false,
    handleError: function(error, extra, showAlert) {
        if (this.pageUnloading) {
            return;
        }
        console.log(error);
        console.trace();

        if (extra) {
            console.log(extra);
        }

        if (this.showingError) {
            return;
        }
        if (showAlert) {
            this.showingError = true;
            //            alert(error);
            this.showingError = false;
        }
        Utils.closeFormLoadingDialog();
    },

    isJsonError: function(data) {
        if (data == null) {
            this.handleError("Null JSON data", null, false);
            return true;
        }
        if (data.error != null) {
            var code = data.errorcode;
            if (code == null) code = "error";
            this.handleError("Error in Utils.isJsonError:" + data.error, null, false);
            return true;
        }
        return false;
    },
    loadXML: function(url, callback, arg) {
        var req = false;
        if (window.XMLHttpRequest) {
            try {
                req = new XMLHttpRequest();
            } catch (e) {
                req = false;
            }
        } else if (window.ActiveXObject) {
            try {
                req = new ActiveXObject("Msxml2.XMLHTTP");
            } catch (e) {
                try {
                    req = new ActiveXObject("Microsoft.XMLHTTP");
                } catch (e) {
                    req = false;
                }
            }
        }
        if (req) {
            req.onreadystatechange = function() {
                if (req.readyState == 4 && req.status == 200) {
                    callback(req, arg);
                }
            };
            req.open("GET", url, true);
            req.send("");
        }
    },
    loadHtml: async function(url, callback) {
        await $.get(url, function(data) {
            callback(data);
        })
            .done(function() {})
            .fail(function() {
                console.log("Failed to load url: " + url);
            });
    },
    loadUrl: function(url, callback, arg) {
        var req = false;
        if (window.XMLHttpRequest) {
            try {
                req = new XMLHttpRequest();
            } catch (e) {
                req = false;
            }
        } else if (window.ActiveXObject) {
            try {
                req = new ActiveXObject("Msxml2.XMLHTTP");
            } catch (e) {
                try {
                    req = new ActiveXObject("Microsoft.XMLHTTP");
                } catch (e) {
                    req = false;
                }
            }
        }
        if (req) {
            req.onreadystatechange = function() {
                if (req.readyState == 4 && req.status == 200) {
                    callback(req, arg);
                }
            };
            req.open("GET", url, true);
            req.send("");
        }
    },
    getUrlArg: function(name, dflt) {
        name = name.replace(/[\[]/, "\\\[").replace(/[\]]/, "\\\]");
        let regexS = "[\\?&]" + name + "=([^&#]*)";
        let regex = new RegExp(regexS);
        let results = regex.exec(window.location.href);
        if (results == null || results == "")
            return dflt;
        let value= results[1];
	value = decodeURIComponent(value);
	return value;
    },
    setCursor: function(c) {
        var cursor = document.cursor;
        if (!cursor && document.getElementById) {
            cursor = document.getElementById('cursor');
        }
        if (!cursor) {
            document.body.style.cursor = c;
        }
    },
    getDomObject: function(name) {
        let obj = new DomObject(name);
        if (obj.obj) return obj;
        return null;
    },
    getEvent: function(event) {
        if (event) return event;
        return window.event;
    },
    getEventX: function(event) {
        if (event.pageX) {
            return event.pageX;
        }
        return event.clientX + document.body.scrollLeft +
            document.documentElement.scrollLeft;
    },
    getEventY: function(event) {
        if (event.pageY) {
            return event.pageY;
        }
        return event.clientY + document.body.scrollTop +
            document.documentElement.scrollTop;

    },

    getTop: function(obj) {
        if (!obj) return 0;
        return obj.offsetTop + this.getTop(obj.offsetParent);
    },


    getBottom: function(obj) {
        if (!obj) return 0;
        return this.getTop(obj) + obj.offsetHeight;
    },


    setPosition: function(obj, x, y) {
        obj.style.top = y;
        obj.style.left = x;
    },

    getLeft: function(obj) {
        if (!obj) return 0;
        return obj.offsetLeft + this.getLeft(obj.offsetParent);
    },
    getRight: function(obj) {
        if (!obj) return 0;
        return obj.offsetRight + this.getRight(obj.offsetParent);
    },

    getStyle: function(obj) {
        if (obj.style) return obj.style;
        if (document.layers) {
            return document.layers[obj.name];
        }
        return null;
    },
    //from http://snipplr.com/view.php?codeview&id=5949
    size_format: function(filesize) {
        if (filesize >= 1073741824) {
            filesize = number_format(filesize / 1073741824, 2) + ' GB';
        } else {
            if (filesize >= 1048576) {
                filesize = number_format(filesize / 1048576, 2) + ' MB';
            } else {
                if (filesize >= 1024) {
                    filesize = number_format(filesize / 1024, 0) + ' KB';
                } else {
                    filesize = number_format(filesize, 0) + ' bytes';
                };
            };
        };
        return filesize;
    },
    inputLengthOk: function(domId, length, doMin) {
        var value = $("#" + domId).val();
        if (value == null) return true;
        if (doMin) {
            if (value.length < length) {
                Utils.closeFormLoadingDialog();
                return false;
            }
        } else {
            if (value.length > length) {
                Utils.closeFormLoadingDialog();
                return false;
            }
        }
        return true;
    },
    inputValueOk: function(domId, rangeValue, min) {
        var value = $("#" + domId).val();
        if (value == null) return true;
        if (min && value < rangeValue) {
            Utils.closeFormLoadingDialog();
            return false;
        }
        if (!min && value > rangeValue) {
            Utils.closeFormLoadingDialog();
            return false;
        }
        return true;
    },
    inputIsRequired: function(domId, rangeValue, min) {
        var value = $("#" + domId).val();
        if (value == null || value.trim().length == 0) {
            Utils.closeFormLoadingDialog();
            return false;
        }
        return true;
    }
};


//HtmlUtils
var HU = HtmlUtils = window.HtmlUtils  = window.HtmlUtil = {
    me:"HtmlUtils",

    loaded:{},
    checkInputChange:function(formId) {
	let changed = false;
    	$(window).bind('beforeunload', (event)=>{
	    if(changed) {
		event.preventDefault();
		event.returnValue= 'Changes have been made. Are you sure you want to exit?';
	    }
	});
	let form = formId?jqid(formId):$('body');
	form.find("input[type='submit']").on("click", function() {
	    changed=false;
	});
	
	form.on('change','input, select, textarea',
		function() {
		    changed = true;
		});

    },



    onReturn:function(obj,func) {
	obj.keydown(function(event) {
	    if (event.key === "Enter" || event.keyCode === 13) {
		func($(this));
	    }
	});
    },

    //this method creates the tabs
    //if cullThem=true then it also hides any tabs that are empty
    //unless all of them are empty
    //But if the first tab, the active tab,  is empty then this doesn't work as it doesn't show
    //the non culled tabs
    initTabs:function(id,cullThem) {
	let tabsContainer = jqid(id).tabs({activate: HtmlUtil.tabLoaded});
	//If there is one tab then don't hide them
	if(!cullThem) return;
	let tabsToHide =[];
	let tabs =[];
	for(let i=1;true;i++) {
	    let tabId = id +'-'+i;
	    let sel = '[aria-controls="' + tabId+'"]';
	    let tab = tabsContainer.find(sel);
	    if(tab.length==0) break;
	    tabs.push(tab);
	    let contents = jqid(tabId);
	    if(contents.length==0) break;
	    let html = contents.html();
	    if(!html || html.trim().length==0)  {
		tabsToHide.push(tab);
	    }
	}
	if(tabs.length==1 || tabs.length==tabsToHide.length) return;
	tabsToHide.forEach(tab=>{
	    tab.hide();
	});
    },

    initTypeMenu: function(selectId, textAreaId) {
	jqid(selectId).change(function() {
	    let v = $(this).val();
	    if(!Utils.stringDefined(v)) {
		return
	    }
	    $(this).val("");
	    let  t = jqid(textAreaId).val()??"";
	    t = t.trim();
	    if(t!="") t = t+"\n";
	    t+=v+":";
	    jqid(textAreaId).val(t);
	});
    },

    getTitleBr:function() {
	return "&#10;";
    },
    hide:function(id) {
	$('#' + id).hide();
    },
    toggleAllInit:function() {
	let id = Utils.getUniqueId('toggleall');
	document.write(HU.div([ATTR_ID,id],'Toggle Details'));
	let visible = false;
	jqid(id).button().click(function() {
	    visible = !visible;
	    $('.entry-toggleblock-label,.toggleblocklabel').each(function() {
		let blockId = $(this).attr('block-id');
		let imgId = $(this).attr('block-image-id');
		let i1 = 'fa-regular fa-square-minus';
		let i2 = 'fa-regular fa-square-plus';
		if($(this).hasClass('entry-toggleblock-label')) {
		    i2 = 'fas fa-caret-right';
		    i1 = 'fas fa-caret-down';		    
		}
		HU.toggleBlockVisibility(blockId,imgId, i1,i2,null,visible);
	    });
	    
	});
    },
    initTooltip:function(select) {
	$(select).tooltip({
	    delay: { show: 0, hide: 5000 },
	    open: function(event, ui) {
		let width = $(this).attr('tooltip-width');
		if(width)
		    ui.tooltip.css(ATTR_WIDTH, width).css('max-width',width);
            },
	    show: {
		delay: 1000
	    },

	    position: {
		xmy: "center bottom",  // Where the tooltip's position should be relative to itself
		xat: "center top",     // Where the tooltip should be placed relative to the target
		collision: "none" 
            },
	    hide:false,
	    content: function() {
		let title = $(this).attr(ATTR_TITLE);
		title = Utils.convertText(title??'');
		let contents = HU.div([ATTR_CLASS,'ramadda-tooltip',ATTR_STYLE,'margin:5px;'],title);
		return contents;
	    }});

    },

    initPageSearch:function(select,parentSelect,label,hideAll,opts) {

	let args = {
	    focus:true,
	    inputSize:'15',
	    target:null,
	    hideAll:hideAll,
	    linkSelector:null
	};
	if(opts) $.extend(args,opts);
	let id = HU.getUniqueId('search_');
	let initValue = HU.getUrlArgument(ARG_PAGESEARCH)??'';
	let input = HU.input('',initValue,[ATTR_CLASS,'ramadda-pagesearch-input',
					   ATTR_ID,id,ATTR_PLACEHOLDER,label??'Search','size',args.inputSize]);
	let buttonMap ={};
	if(args.buttons) {
	    args.buttons.forEach(b=>{
		b.id = HU.getUniqueId('button');
		buttonMap[b.id] =b;
		if(b.clear) {
		    input+=HU.span([ATTR_ID,b.id,'clear',true],b.label);
		    return
		}

		if(!b.value) {
		    input+=SPACE1;
		    input+=HU.b(b.label);
		    return
		}

		input+=SPACE1;
		input+=HU.span([ATTR_ID,b.id],b.label);
	    });
	}
	if(args.linkSelector) {
	    args.linksDivId = HU.getUniqueId('links');
	    input+=HU.div([ATTR_ID,args.linksDivId]);
	}


	if(args.target) {
	    $(args.target).html(input);
	} else {
	    document.write(input);
	}
	if(args.buttons) {
	    args.buttons.forEach(b=>{
		jqid(b.id).button().click(function() {
		    let button = buttonMap[$(this).attr(ATTR_ID)];
		    if(button.clear) {
			HU.doPageSearch('',select,parentSelect,args.hideAll,args);
		    }
		    HU.doPageSearch(button.value,select,parentSelect,args.hideAll,args);
		});
	    })
	}



	if(args.focus) {
	    jqid(id).focus();
	}
	jqid(id).keydown(function(event) {
	    if (event.key === "Enter") {
		event.preventDefault(); 
	    }
	});


	jqid(id).keyup(function(){
	    if (event.key === "Enter") {
		event.preventDefault(); 
		return
	    }
	    let value = $(this).val();
	    HU.doPageSearch(value,select,parentSelect,args.hideAll,args);
	    if(Utils.stringDefined(value)) {
		HU.addToDocumentUrl(ARG_PAGESEARCH,value);
	    } else {
		HU.removeFromDocumentUrl(ARG_PAGESEARCH);
	    }

	});

	if(Utils.stringDefined(initValue)) {
	    HU.doPageSearch(initValue,select,parentSelect,args.hideAll,args);
	}


    },		       
    classes:function() {
	return Utils.join(Array.from(arguments),' ');
    },
    doPageSearch:function(value,select,parentSelect,hideAll,args) {
	args = args??{}
	if(args.handler) {
	    args.handler(value,args);
	    return
	}

	let linksDiv;
	let linkNames=[];
	if(args.linksDivId) {
	    linksDiv=jqid(args.linksDivId);
	    linksDiv.html('');
	}

	let s  = $(select);
	if(hideAll) {
	    if(Utils.stringDefined(value)) {
		$(hideAll).hide();
	    } else {
		$(hideAll).show();
	    }
	}

	let pre;
	let post;
	if(Utils.stringDefined(value)) {
	    value = value.toLowerCase();
	    let idx = value.indexOf(":");
	    if(idx>=0) {
		pre = value.substring(0,idx+1);
		post = value.substring(idx+1);		

	    }
	}

	s.each(function() {
	    let textOk = true;
	    if(Utils.stringDefined(value)) {
		textOk = false;
		let html = $(this).html();
		let category = $(this).attr('data-category');
		if(category) html+=' ' +category;
		let corpus = $(this).attr('data-corpus');
		if(corpus) html+=' ' +corpus;		
		//check for title
		let match = html.match(/title *= *(\"|')([^(\"|')]+)/);
		if(match) {
		    html = html+' ' + match[2];
		} 
		html = Utils.stripTags(html);
		html = html.toLowerCase();
		if(pre && post) {
		    let i1 = html.indexOf(pre);
		    if(i1>=0) {
			let h = html.substring(i1+pre.length);
			let i2 = h.indexOf(":");
			if(i2>=0) {
			    h = h.substring(0,i2).trim();
			    if(h.startsWith(post)) 
				textOk = true;
			}
		    }
		}

		if(html.indexOf(value)>=0) {
		    textOk=true;
		} 
	    }
	    if(!textOk) {
		$(this).attr('isvisible','false');
		$(this).fadeOut();
	    } else {
		$(this).attr('isvisible','true');
		$(this).show();
		if(linksDiv && args.linkSelector) {
		    let links = $(this).find(args.linkSelector);
		    if(links.length) {
			let name = links.attr('name');
			if(name)
			    linkNames.push(name);
		    }
		}


	    }
	});
	if(parentSelect) {
	    $(parentSelect).each(function() {
		let anyVisible = false;
		$(this).find(select).each(function() {
		    if($(this).attr('isvisible')==='true') {
			anyVisible=true;
		    }
		});
		if(!anyVisible) {
		    $(this).fadeOut();
		} else {
		    $(this).show();
		}
	    });
	}
	//Only show a few of the links
	if(linkNames.length>0 && linkNames.length<15) {
	    linkNames.forEach(name=>{
		let link = HU.href('#' + name,name);
		linksDiv.append(link+' ');
	    });
	}

	if(args.callback) {
	    args.callback();
	}
    
    },				  
    initScreenshot: function(img) {
	img.width/=2
	$(img).css("display","inline-block").css("max-width","90vw");
    },
    insertIntoTextarea:function(myField, value,newLine) {
	if(typeof myField=='string') {
	    myField =  document.getElementById(myField);
	}

	value = Utils.decodeText(value);    
	if(newLine) value='\n'+value;
	let textScroll = myField.scrollTop;

	//IE support
	if (document.selection) {
            myField.focus();
            sel = document.selection.createRange();
            sel.text = value;
	} else if (myField.selectionStart || myField.selectionStart == '0') {
	    //MOZILLA/NETSCAPE support
            var startPos = myField.selectionStart;
            var endPos = myField.selectionEnd;
            myField.value = myField.value.substring(0, startPos) +
		value +
		myField.value.substring(endPos, myField.value.length);
	    let newPos = startPos + value.length;
	    myField.selectionEnd = newPos;
	} else {
            myField.value += value;
	}
	if(newLine && myField.value) {
	    myField.value = myField.value.trim();
	}


	myField.scrollTop = textScroll;
    },


    initLoadingImage:function(img) {
	setTimeout(()=>{
	    $(img).removeClass('ramadda-image-loading');
	},2000);
    },
    initRadioToggle(radios,values) {
	$(radios).find('input[type=radio]').change(function() {
	    for(a in values) {
		if(a==$(this).attr('value'))
		    $( values[a]).show();
		else
		    $( values[a]).hide();
	    }
	});
    },

    initToggleAll:function(cbx,selector,notHidden) {
	jqid(cbx).change(function(){
	    let on = $(this).is(':checked');
	    $(selector).each(function() {
		if(notHidden && $(this).is(':hidden')) return;
		$(this).prop('checked',on);
		$(this).trigger('change');
	    });
	});
    },
    getEmojis: function(cb) {
	if(this.emojis) {
	    cb(this.emojis);
	    return;
	}
        $.getJSON(RamaddaUtil.getUrl('/emojis/emojis.json'), data=>{
	    let emojis  = [];
	    let cats = {};
	    this.emojis = [];
	    let lastCat;
	    data.forEach(item=>{
		let itemCategory = item.category??lastCat;
		if(itemCategory) lastCat = itemCategory;
		let cat = cats[itemCategory];
		if(!cat) {
		    cat = {
			name:itemCategory,
			images:[]
		    }
		    cats[itemCategory] = cat;
		    emojis.push(cat);
		}
		let url = item.image;
		if(!url.startsWith("/")) url = "/emojis/" + url
		cat.images.push({name:item.name,
				 image:RamaddaUtil.getUrl(url)});

	    })
	    this.emojis=emojis;
	    cb(this.emojis);
	}).fail(data=>{
	    console.log("Failed to load json:");
	});


    },
    loadJqueryLib: function(name,css,js,selector,callback) {
	let debug = false;
        if(debug) console.log('loadJqueryLib:' + name);
        if(!HtmlUtils.loaded[name]) {
            css.forEach(url=>{
		if(debug) console.log('\tcss: ' + url);
                let css = "<link rel='stylesheet' href='" +url+ "' crossorigin='anonymous'>";
                $(css).appendTo("head");
            });
            js.forEach(url=>{
		if(debug) console.log('\tjs: ' + url);
                let html = 
                    "<script src='" + url +"'  type=text/JavaScript></script>";
                $(html).appendTo("head");
            });
            HtmlUtils.loaded[name] = true;
        }
	if(debug) console.log('\tafter loading');
        //check and wait for it
        if(!$(selector)[name]) {
	    if(debug) console.log('\tnot loaded:' + name);
            setTimeout(()=>{
                HtmlUtils.loadJqueryLib(name,css,js,selector,callback);
            },50);
            return
        }
	if(debug) console.log('\tcalling callback');
        callback();
	if(debug) console.log('\tdone calling callback');
    },
    createFancyBox: function(selector, args) {
        args = args||{};
        HtmlUtils.loadJqueryLib('fancybox',[ramaddaCdn +"/lib/fancybox-3/jquery.fancybox.min.css"],
                                [ramaddaCdn +"/lib/fancybox-3/jquery.fancybox.min.js"],
                                selector,()=>{
                                    $(selector).fancybox(args);
                                });
    },
    checkToHidePopup:function() {
        if (this.popupTime) {
            var now = new Date();
            timeDiff = now - this.popupTime;
            if (timeDiff > 1000) {
                return true;
            }
            return false;
        }
        return true;
    },
    jqname:function(value) {
	return this.jqattr('name',value);
    },
    jqattr:function(name,value) {
	let sel =  '[' + name+'="'+ value+'"]';
	return $(sel);
    },
    jqid:function(id) {
        return $("#"+id);
    },
    initInteractiveInput:function(id,url) {
        let input = HU.jqid(id);
        input.keyup(function(event) {
            HtmlUtils.hidePopupObject();
            let val = $(this).val();
            if(!Utils.stringDefined(val)) return;
            let input = $(this);
            $.getJSON(url+"?text=" + val, data=>{
                if(data.length==0) return;
                let suggest = "";
                data.forEach(d=>{
                    suggest+=HU.div([ATTR_CLASS,CLASS_CLICKABLE +' ramadda-suggest',"suggest",d],d);
                });
                let html = HU.div([ATTR_CLASS,"ramadda-search-popup",ATTR_STYLE,HU.css("max-width","200px",
										       "padding","4px")],suggest);
                let dialog = HU.makeDialog({content:html,my:"left top",at:"left bottom",anchor:input});
                dialog.find(".ramadda-suggest").click(function() {
                    HtmlUtils.hidePopupObject();
                    input.val($(this).attr("suggest"));
                });
            }).fail(
                err=>{
                    console.log("suggest call failed:" + url +"\n" + err)
                    console.dir(err);
                });
        });
    },
    getTooltip: function() {
        return $("#ramadda-popupdiv");
    },
    getPopupObject: function() {
        return this.popupObject;
    },
    hasPopupObject: function() {
        return this.popupObject !=null;
    },
    clearPopupObject: function() {
        this.popupObject = null;
    },
    isPopupObject: function(obj) {
        if(this.popupObject && obj) {
            return this.popupObject.attr(ATTR_ID) == obj.attr(ATTR_ID);
        }
        return false;
    },
    setPopupObject: function(obj) {
        if(this.isPopupObject(obj)) {
            return obj;
        }

        if(this.popupObject) {
            this.hidePopupObject(null,true);
        }
        this.popupObject = obj;
        if(!this.popupObject.attr("addedMouseListener")) {
            this.popupObject.attr("addedMouseListener","true");
            this.popupObject.mousedown(function(event) {
                event.stopPropagation();
            });
        }
	this.popupObjectTime = new Date();
	//	console.log('popup time:' +this.popupObjectTime);
        return obj;
    },
    hidePopupObject: function(event,skipTimeCheck) {
	//check for a hide event right after we set the popup object
	if(!skipTimeCheck && this.popupObjectTime) {
	    let now = new Date();
	    let diff = now.getTime()-this.popupObjectTime.getTime();
	    //wait half a second?
	    if(diff<500) {
		//		console.log('hide popup time  - too soon - diff:' + diff);
		return;
	    }
	    //	    console.log('hide popup time - ok - diff:' + diff);
	}
	this.popupObjectTime=null;

        if (this.popupObject) {
	    //	    console.log('close');
            this.popupObject.hide();
            if(this.popupObject.attr("removeonclose")== "true") {
                this.popupObject.remove();
            }
            this.popupObject = null;
        } else {
	    //	    console.log('no popup');
	}
        this.popupTime = new Date();
        if(event) {
            event.stopPropagation();
        }
    },

    lastCbxClicked:null,
    lastCbxIdClicked:null,
    checkboxClicked: function(event, cbxPrefix, id) {
        if (!event) return;
        let cbx = GuiUtils.getDomObject(id);
        if (!cbx) return;
        cbx = cbx.obj;
        let checkBoxes = new Array();
        if (!cbx.form) return;
        let elements = cbx.form.elements;
        for (i = 0; i < elements.length; i++) {
            if (elements[i].name.indexOf(cbxPrefix) >= 0 || elements[i].id.indexOf(cbxPrefix) >= 0) {
                checkBoxes.push(elements[i]);
            }
        }


        let value = cbx.checked;
        if (event.ctrlKey) {
            for (i = 0; i < checkBoxes.length; i++) {
                checkBoxes[i].checked = value;
            }
        }

        if (event.shiftKey) {
            if (HtmlUtils.lastCbxClicked) {
                let pos1 = GuiUtils.getTop(cbx);
                let pos2 = GuiUtils.getTop(HtmlUtils.lastCbxClicked);
                let lastCbx = $("#" + HtmlUtils.lastCbxIdClicked);
                let thisCbx = $("#" + id);

                if (lastCbx.position()) {
                    pos2 = lastCbx.offset().top;
                }
                if (thisCbx.position()) {
                    pos1 = thisCbx.offset().top;
                }

                if (pos1 > pos2) {
                    let tmp = pos1;
                    pos1 = pos2;
                    pos2 = tmp;
                }
                for (i = 0; i < checkBoxes.length; i++) {
                    let top = $("#" + checkBoxes[i].id).offset().top;
                    if (top >= pos1 && top <= pos2) {
                        checkBoxes[i].checked = value;
                    }
                }
            }
            return;
        }
        HtmlUtils.lastCbxClicked = cbx;
        HtmlUtils.lastCbxIdClicked = id;
    },


    tabLoaded: function(event, ui) {
        if (window["ramaddaDisplayCheckLayout"]) {
            ramaddaDisplayCheckLayout();
        }
        if (window["ramaddaMapCheckLayout"]) {
            ramaddaMapCheckLayout();
        }
    },
    elementScrolled: function(elem) {
        var docTop = $(window).scrollTop();
        var docBottom = docTop + $(window).height();
        var elemTop = $(elem).offset().top;     
        var elemBottom = elemTop + $(elem).outerHeight(true); 
        if((elemTop <= docBottom) && (elemTop >= docTop)) return true;  
        if((elemBottom <= docBottom) && (elemBottom >= docTop)) return true;
        if((elemBottom >= docBottom) && (elemTop <= docTop)) return true;

        return false;
    },
    initOdometer: function(id,value, pause, immediate) {
        if(!Utils.isDefined(pause)) pause = 0;
        $(document).ready(function(){   
            if(immediate) {
                $('#' + id).html(value);
                return;
            }
            setTimeout(function(){
                if(HtmlUtils.elementScrolled('#' + id)) {
                    setTimeout(function() {$('#' + id).html(value);},pause);
                } else {
                    $(window).scroll(function(){
                        if(HtmlUtils.elementScrolled('#' + id)) {
                            setTimeout(function() {$('#' + id).html(value);},pause);
                        }});
                }},1000);
        });
    },
    callWhenScrolled: function(id,func,pause) {
        if(!Utils.isDefined(pause)) pause = 0;
        $(document).ready(function(){
            setTimeout(function(){
                if(HtmlUtils.elementScrolled('#' + id)) {
                    setTimeout(func, pause);
                } else {
                    $(window).scroll(function(){
                        if(HtmlUtils.elementScrolled('#' + id)) {
                            setTimeout(func, pause);
                        }});
                }},1000);
        });
    },

    waitForIt: function(what,callback, error, cnt) {
        if (window[what]) {
            callback();
            return;
        }
        if(!Utils.isDefined(cnt)) cnt = 500;
        if(cnt===0) {
            if(error) error();
            return;         
        }
        setTimeout(()=>{
            HtmlUtils.waitForIt(what,callback, error, cnt-1);
        },50);
    },
    loadAndWait: function(what,load, callback,error) {
        if (!window[what]) {
            let imports = load;
            $(imports).appendTo("head");
        }
        HtmlUtils.waitForIt(what,callback, error);
    },
    makeMessage: function(what,msg) {
        return "<div class='ramadda-message ramadda-message-plain ' id='messageblock'><table width='100%'><tbody><tr valign='top'><td width='5%'><div class='ramadda-message-icon'><span><i class='" + what +"' style='font-size:32pt;'></i></span></div></td><td><div class='ramadda-message-inner'><p>" + msg +"</p></div></td></tr></tbody></table></div>"
    },
    makeErrorMessage: function(msg) {
        return HtmlUtils.makeMessage("fas fa-exclamation-triangle text-danger",msg);
    },
    makeInfoMessage: function(msg) {
        return HtmlUtils.makeMessage("fas fa-info",msg);
    },    

    makeRunningMessage: function(msg) {
        return HtmlUtils.makeMessage("fas fa-spinner fa-spin",msg);
    },    

    makeSlides: async function(id,args, tries) {
	if(!Utils.isDefined(tries)) {
	    tries = 0;
	}
	await HtmlUtils.loadSlides();
        let opts = {
	    arrows:true,
	    speed:0,
            dots:true,
	    slidesToShow:1,
	    variableWidth:true
        };
        if(args) $.extend(opts,args);
        $("#" + id).slick(opts);
        HU.swapHtml("#" + id +"_headercontents", "#" + id +"_header");
        //Do this later because of the swapHtml
        setTimeout(()=>{
            let header = jqid(id +"_header");
            let items = header.find(".ramadda-slides-header-item");
            items.click(function() {
                let index = +$(this).attr("slideindex");
                $("#" + id).slick('slickGoTo', index);
            });
            jqid(id).on('afterChange', function(event, slick, currentSlide){
                items.removeClass("ramadda-slides-header-item-selected");
                header.find(HtmlUtils.attrSelect("slideindex",currentSlide)).addClass("ramadda-slides-header-item-selected");
            });
        });

    },
    loadSlides: async function() {
        if(!HtmlUtils.slidesLoaded) {
	    $('<link>').appendTo('head').attr({type: 'text/css', rel: 'stylesheet',
					       href: RamaddaUtil.getCdnUrl("/lib/slick/slick.css")});
	    $('<link>').appendTo('head').attr({type: 'text/css', rel: 'stylesheet',
					       href: RamaddaUtil.getCdnUrl("/lib/slick/slick-theme.css")});
            await Utils.importJS(RamaddaUtil.getCdnUrl("/lib/slick/slick.min.js"));
            HtmlUtils.slidesLoaded = true;
	    return false;
	}
	return true;
    },
    loadKatex: function(callback, error) {
        if (!window["katex"]) {
            let imports = "<link rel='preload' href='https://cdn.jsdelivr.net/npm/katex@0.10.1/dist/fonts/KaTeX_Main-Regular.woff2' as='font' type='font/woff2' crossorigin='anonymous'>\n<link rel='preload' href='https://cdn.jsdelivr.net/npm/katex@0.10.1/dist/fonts/KaTeX_Math-Italic.woff2' as='font' type='font/woff2' crossorigin='anonymous'>\n<link rel='preload' href='https://cdn.jsdelivr.net/npm/katex@0.10.1/dist/fonts/KaTeX_Size2-Regular.woff2' as='font' type='font/woff2' crossorigin='anonymous'>\n<link rel='preload' href='https://cdn.jsdelivr.net/npm/katex@0.10.1/dist/fonts/KaTeX_Size4-Regular.woff2' as='font' type='font/woff2' crossorigin='anonymous'/>\n<link rel='stylesheet' href='https://fonts.googleapis.com/css?family=Lato:300,400,700,700i'>\n<link rel='stylesheet' href='https://cdn.jsdelivr.net/npm/katex@0.10.1/dist/katex.min.css' crossorigin='anonymous'>\n<script defer src='https://cdn.jsdelivr.net/npm/katex@0.10.1/dist/katex.min.js' crossorigin='anonymous'></script>";
            $(imports).appendTo("head");
        }
        HtmlUtils.waitForIt("katex",callback, error);
    },
    applyMarkdown:function(srcId,targetId) {
        let f = ()=>{
            let src = $("#"+ srcId).html();
            try {
                var converter = new showdown.Converter();
                var html = converter.makeHtml(src);
                $("#" + targetId).html(html);
            } catch(e) {
                $("#" + targetId).html("Error processing markdown:" + e);
            }
        }
        let e = ()=> {
            $("#" + targetId).html("Error loading showdown");
        };
        let imports = "<script src='" +ramaddaCdn + "/lib/showdown.min.js'/>";
        HtmlUtils.loadAndWait("showdown",imports,f,e);
    },
    applyLatex:function(srcId,targetId) {
        let f = ()=>{
            let src = $("#"+ srcId).html();
            try {
                var html = katex.renderToString(src, {
                    throwOnError: true
                });
                $("#" + targetId).html(html);
            } catch(e) {
                $("#" + targetId).html("Error processing markdown:" + e);
            }
        }
        let e = ()=> {
            $("#" + targetId).html("Error loading katex");
        };
        HtmlUtils.loadKatex(f,e);
    },

    isFontAwesome:function(icon) {
        return icon.startsWith("fa-") || icon.startsWith("fas ") || icon.startsWith("far ")
            || icon.startsWith("fab ");     
    },
    getIconImage: function(url,attrs,attrs2) {
        if(HtmlUtils.isFontAwesome(url)) {
            let clazz = "";
            let a;
            if(url.startsWith("fa-thin") || url.startsWith("fa-light") || url.startsWith("fa-regular") || url.startsWith("fa-solid") ||url.startsWith("fas ") || url.startsWith("fab ")|| url.startsWith("far")) {
                a = [ATTR_CLASS,url];
            } else {
                a = [ATTR_CLASS,"fas " + url];
            }
            if(attrs2)
                a = Utils.mergeLists(a, attrs2);
            return HtmlUtils.span(attrs,HtmlUtils.tag("i",a));
        } else {
            return HtmlUtils.image(url, attrs);
        }
    },
    getObjectURL:function(blob) {
        let urlCreator = window.URL || window.webkitURL;
        return urlCreator.createObjectURL(blob);
    },
    getErrorDialog: function(msg) {
        return "<div class=\"ramadda-message\"><table><tr valign=top><td><div class=\"ramadda-message-link\"><img border=\"0\"  src=\"/repository/icons/error.png\"  /></div></td><td><div class=\"ramadda-message-inner\">" + msg + "</div></td></tr></table></div>";
    },
    makeBreadcrumbsInit: function(id) {
        //If page isn't loaded then register a callback
        if(!Utils.getPageLoaded()) {
            let theId = id;
            Utils.addLoadFunction(() =>{
                HtmlUtils.makeBreadcrumbs(theId);
            });
            return;
        }
        HtmlUtils.makeBreadcrumbs(id);
    },
    makeBreadcrumbs: function(id) {
        let bc = jQuery("#" + id);
        let num = bc.find("li").length
        let begin = 0;
        let end = 4;
        let w = 0;
        if(num>5) {
            end=0;
        }
        bc.jBreadCrumb({
            previewWidth: w,
	    //          maxFinalElementLength:400,
	    //          minFinalElementLength:10,
            easing:'easeOutQuad',
	    //            easing: 'swing',
            //(sic)
            beginingElementsToLeaveOpen: begin,
            endElementsToLeaveOpen:end
        });
    },
    tooltipInit: function(openerId, id) {
        $("#" + id).dialog({
            autoOpen: false,
            //                draggable: true,
            minWidth: 650,
            /* use the default
               show: {
               effect: "blind",
               duration: 250
               },
            */
            hide: {
                effect: "blind",
                duration: 250
            }
        });

        $("#" + openerId).click(function() {
            $("#" + id).dialog("open");
        });
    },



    join: function(items, separator) {
        if(!items.forEach) {
            items=[items];
        }
        let html = "";
        items.forEach((item,idx)=>{
            if (idx > 0 & separator != null) html += separator;
            html += item;
        });
        return html;
    },
    qt: function(value) {
        return "\"" + value + "\"";
    },
    sqt: function(value) {
        return "\'" + value + "\'";
    },
    getUniqueId: function(prefix) {
        if (!window["uniqueCnt"]) {
            window["uniqueCnt"] = new Date().getTime();
        }
        let cnt = window["uniqueCnt"]++;
        return (prefix||"id_") + cnt;
    },
    inset: function(html, top, left, bottom, right) {
        var attrs = [];
        var style = "";
        if (top) {
            style += "padding-top: " + top + "px; ";
        }
        if (left) {
            style += "padding-left: " + left + "px; ";
        }
        if (bottom) {
            style += "padding-bottom: " + bottom + "px; ";
        }
        if (right) {
            style += "padding-right: " + right + "px; ";
        }
        return HtmlUtils.div([ATTR_STYLE, style], html);

    },
    makeTabs(list,args) {
	let opts = {};
	if(args) $.extend(opts,args);
	let id = HU.getUniqueId('tabs_');
	let html=HU.open('div',[ATTR_ID,id]);
	html+='\n';
	html+='<ul>\n';
	list.forEach((tab,idx)=>{
	    html+=HU.tag('li',[], HU.href('#' + id +'-'+(idx+1),tab.label??tab.header));
	    html+='\n';
	});
	html+='</ul>\n';
	list.forEach((tab,idx)=>{
	    let contents = tab.contents;
	    if(opts.contentsStyle) {
		contents = HU.div([ATTR_STYLE,opts.contentsStyle], contents);
	    }
	    html+=HU.div([ATTR_ID,id+'-'+(idx+1)], contents);
	    html+='\n';
	});
	html+='</div>';
	return {
	    contents:html,
	    init:()=>{
		$("#" +id ).tabs();
	    }};
    },
    makeAccordionHtml(list) {
	let id = HU.getUniqueId('accordion_');
	let html = HU.open('div',[ATTR_CLASS,'ui-accordion ui-widget ui-helper-reset',ATTR_ID,id]);
	list.forEach(item=>{
	    html+=HU.tag('h3',[ATTR_CLASS,'ui-accordion-header ui-helper-reset ui-corner-top',ATTR_STYLE,'border:0px;background:none;'],
			 HU.href('#',HU.span([ATTR_CLASS,CLASS_CLICKABLE],item.header??item.label)));
	    html+=HU.div([ATTR_ID,HU.getUniqueId('accordion_'),ATTR_CLASS,'ramadda-accordion-contents'],item.contents);
	})
	html+='</div>';
	return {id:id,contents:html,init:()=>{
	    HU.makeAccordion('#'+id);
	}};
    },
    makeAccordion: function(id, args) {
        if(args == null) args = {heightStyle: "content", collapsible: true, active: 0, decorate: false, animate:200};
	var icons = {
            header: "iconClosed",    // custom icon class
            activeHeader: "iconOpen" // custom icon class
	};
        $(function() {
            //We initially hide the accordion contents
            //Show all contents
            var contents = $(id +" .ramadda-accordion-contents");
            contents.css("display", "block");
            var ctorArgs = {
                animate:200,
		icons:icons,
                collapsible: true,
                heightStyle: "content",
                active: 0,
                decorate:true,
                activate: function() {
                    if (window["ramaddaDisplayCheckLayout"]) {
                        ramaddaDisplayCheckLayout();
                    }
                }
            }
            $.extend(ctorArgs, args);
            if(!ctorArgs.decorate) {
                var header = $(id +" .ui-accordion-header");
                header.css("padding","0em 0em 0em 0em");
            }
            if(ctorArgs.active<0) ctorArgs.active='none';
            let accordion = $(id).accordion(ctorArgs);
	    //Handle any checkboxes in the header
	    let f = function(ele,event) {
		event.stopPropagation(); 
		let header = ele.closest('.ui-accordion-header');
		let cbx = header.find(':checkbox');
		if(cbx.is(':checked')) header.css('background','#fffeec');
		else  header.css('background','transparent');		
	    }

	    let ff = function(event){f($(this),event);}
	    accordion.find(".accordion-toolbar").find(":checkbox").click(ff);
	    accordion.find(".accordion-toolbar").find("label").click(ff);
	    accordion.find(".accordion-toolbar").find(":checkbox").change(function(event) {
		f($(this),event);
	    });
        });
    },
    buttons:function(args,clazz,style) {
	let buttons = Utils.wrap(args,"<div style='display:inline-block;margin-right:6px;'>","</div>");
	return HU.div([ATTR_CLASS,clazz??'ramadda-button-bar',ATTR_STYLE,style??''], buttons);
    },
    vspace:function(dim) {
	return this.div([ATTR_STYLE,this.css('margin-top',dim??'0.5em')]);
    },
    hbox: function(args,style) {
        let row = HtmlUtils.openTag("tr", ["valign", "top"]);
        row += Utils.wrap(args, '<td align=left>' +HU.open('div',[ATTR_STYLE, style??'']),'</div></td>');
        row += "</tr>";
        return this.tag("table", ["border", "0", "cellspacing", "0", "cellpadding", "0"],
                        row);
    },
    vbox: function(args) {
        let col = HtmlUtils.join(args, "<br>");
        return this.div([ATTR_STYLE,HU.css("display","inline-block")],col);
    },    


    leftRight: function(left, right, leftWeight, rightWeight) {
        if (leftWeight == null) leftWeight = "6";
        if (rightWeight == null) rightWeight = "6";
        return this.div([ATTR_CLASS, "row"],
                        this.div([ATTR_CLASS, "col-md-" + leftWeight], left) +
                        this.div([ATTR_CLASS, "col-md-" + rightWeight, ATTR_STYLE, "text-align:right;"], right));
    },
    leftCenterRight: function(left, center, right, leftWidth, centerWidth, rightWidth, attrs,cellStyle) {
        if (!attrs) attrs = {};
        if (!attrs.valign) attrs.valign = "top";
        if(!cellStyle) cellStyle = "";
        return this.tag("table", ["border", 0, ATTR_WIDTH, "100%", "cellspacing", "0", "cellpadding", "0"],
                        this.tr(["valign", attrs.valign],
                                this.td(["align", "left", ATTR_WIDTH, leftWidth, ATTR_STYLE,cellStyle], left) +
                                this.td(["align", "center", ATTR_WIDTH, centerWidth, ATTR_STYLE,cellStyle], center) +
                                this.td(["align", "right", ATTR_WIDTH, rightWidth, ATTR_STYLE,cellStyle], right)));
    },

    row: function() {
        let row = "<table cellpadding=0 cellspacing=0 border=0 width=100%><tr valign=center>";
        Array.from(arguments).forEach(h=>{
            if(Array.isArray(h)) {
                row+=HtmlUtils.tag("td",h[0],h[1]);
            } else {
                row+=HtmlUtils.tag("td",[],h);
            }
        })
        row+="</tr></table>";
        return row;
    },
    hrow: function() {
        let row = "";
        Array.from(arguments).forEach(h=>{
            row+=HtmlUtils.div([ATTR_STYLE, "display:inline-block"],h);
        })
        return row;
    },

    leftRightTable: function(left, right, leftWidth, rightWidth, attrs) {
        if (!attrs) attrs = {};
        if (!attrs.valign) attrs.valign = "top";
        var leftAttrs = ["align", "left"];
        var rightAttrs = ["align", "right"];
        if (leftWidth) {
            leftAttrs.push(ATTR_WIDTH);
            leftAttrs.push(leftWidth);
        }
        if (rightWidth) {
            rightAttrs.push(ATTR_WIDTH);
            rightAttrs.push(rightWidth);
        }
        return this.tag("table", ["border", "0", ATTR_WIDTH, "100%", "cellspacing", "0", "cellpadding", "0"],
                        this.tr(["valign", attrs.valign],
                                this.td(leftAttrs, left) +
                                this.td(rightAttrs, right)));
    },

    heading: function(html) {
        return this.tag("h3", [], html);
    },
    bootstrapClasses: ["col-md-12", "col-md-6", "col-md-4", "col-md-4", "col-md-4", "col-md-2"],
    getBootstrapClass: function(cols) {
        cols -= 1;
        cols = Math.max(cols, 0);
        if (cols < this.bootstrapClasses.length) {
            return this.bootstrapClasses[cols];
        }
        return "col-md-1";
    },
    toggleDialogs:{},
    makeOkCancelDialog:function(anchor,msg,okFunc,cancelFunc,extra,opts) {
	opts = opts??{};
	let buttonList = [HU.div(['action','ok',ATTR_CLASS,'ramadda-button ' + CLASS_CLICKABLE],opts.okLabel??'OK'),
			  HU.div([ATTR_CLASS,'ramadda-button ' + CLASS_CLICKABLE],opts.cancelLabel??'Cancel')];

	if(extra) buttonList.push(extra);
	let buttons = HU.buttons(buttonList);
	let html = msg+HU.vspace() + buttons;
	html = HU.div([ATTR_STYLE,opts.style??'padding:5px;'],html);
	let dialog = HU.makeDialog({content:html,header:false,anchor:anchor,my:opts.my??"left top",at:opts.at??"left bottom",
				    title:opts.title,header:opts.header,draggable:opts.draggable});
	dialog.find('.' + CLASS_CLICKABLE).button().click(function() {
	    if($(this).attr('action')=='ok') {
		okFunc();
	    } else if(cancelFunc) {
		cancelFunc();
	    }
	    dialog.remove();
	});
    },

    initSideToggle:function(btn,box,opts) {
	opts = opts??{};
	btn = jqid(btn);
	box = jqid(box);
	let width = opts.width??opts.boxWidth??'200px';
	if(opts.buttonWidth) {
	    btn.css(ATTR_WIDTH,opts.buttonWidth);
	}
	if(opts.fontSize) {
	    btn.css('font-size',opts.fontSize);
	}	
	if(opts.buttonColor||opts.color) {
	    btn.css('color',opts.buttonColor??opts.color);
	}
	if(opts.buttonBackground||opts.background) {
	    btn.css('background',opts.buttonBackground??opts.background);	    
	}		
	if(opts.boxBackground||opts.background) {
	    box.css('background',opts.boxBackground??opts.background);	    
	}		


	if(opts.boxHeight) {
	    box.css('height',opts.boxHeight);
	}
	if(opts.buttonTop??opts.top) {
	    btn.css('top',opts.buttonTop??opts.top);
	}
	if(opts.boxHeight) {
	    box.css('height',opts.boxHeight);
	}
	if(opts.boxTop??opts.top) {
	    box.css('top',opts.boxTop??opts.top);
	}	
	box.css(ATTR_WIDTH,width);
	box.css('left','-'+width);
	let anim = (e,v)=>{
	    e.animate({
		left: v
	    }, Utils.isDefined(opts.speed)?parseInt(opts.speed):500);
	}
	btn.click(function() {
	    let flag = 'toggle-open';
	    if(btn.attr(flag)==='true') {
		btn.attr(flag,'false');
		anim(btn,'0px');
		anim(box,'-'+width);
	    } else {
		btn.attr(flag,'true');
		anim(btn,width);
		anim(box,'0px');
	    }
	});
    },


    makeDialog: function(args) {
        HtmlUtils.hidePopupObject(null,true);
        let opts  = {
            modal:false,
	    modalStrict:false,
            modalContentsCss:"",
            sticky:false,
            content:null,
            contentId:null,
            anchor:null,
            draggable:false,
	    resizable:false,
            decorate:true,
            header:false,
	    headerRight:null,
            remove:true,
            my: "left top",
            at: "left bottom",      
	    animate:false,
	    animateSpeed:300,
            title:"",
	    rightSideTitle:"",
            inPlace:true,
            fit:true
        };

        if(args) {
            if(args.at=="") delete args.at;
            if(args.my=="") delete args.my;
	    //          console.log(JSON.stringify(args,null,2));
            $.extend(opts, args);
        }


	//Check if there is a toggleid and the popup is visible
	if(opts.toggleid && this.toggleDialogs[opts.toggleid]) {
	    let dialog = this.toggleDialogs[opts.toggleid];
	    if(dialog.is(":visible")) {
		//If it is then hide it and return
		dialog.hide();
		this.toggleDialogs[opts.toggleid] = null;
		return;
	    }
	}



        if(opts.anchor && (typeof opts.anchor=="string")) {
            opts.anchor = $("#"  + opts.anchor);
        }

        if(opts.anchor && opts.anchor.length==0) {
	    console.log("Utils.makeDialog: Invalid anchor");
	    console.trace();
	    opts.anchor=null;
	}

        let parentId;
        let html;
        if(opts.content)  {
            //Can't be in place if its a string
            opts.inPlace = false;
        }
        if(opts.inPlace) {
            opts.remove = false;
            parentId= HtmlUtils.getUniqueId();
            html = HU.div([ATTR_ID,parentId,ATTR_STYLE,HU.css()],HU.SPACE);
        } else {
            html = opts.content;
            if(html == null) {
                if(opts.contentId) {
                    html = $("#" + opts.contentId).html();
                } else {
                    html = "No html provided";
                }
            } 
	    
        }
        let id = HtmlUtils.getUniqueId();
        if(opts.header) {
            html = HU.div([ATTR_CLASS,"ramadda-popup-inner"], html);
        }

        if(opts.header) {
            let closeImage = HtmlUtils.div([ATTR_TITLE,'Close',ATTR_CLASS,'ramadda-popup-close'],
					   HU.jsLink('',HtmlUtils.getIconImage(icon_close), [ATTR_ID,id+'_close',
											     ATTR_STYLE,HU.css('cursor','pointer')]));
            let title = HU.div([ATTR_CLASS,'ramadda-popup-title'],opts.title);
	    if(opts.rightSideTitle)
		title+=HU.div([ATTR_CLASS,'ramadda-popup-title-right'],opts.rightSideTitle);
            let hdr = closeImage+title
	    if(opts.headerRight) {
		hdr = hdr+HU.div([ATTR_STYLE,HU.css('position','absolute','top','0px','right','0px')], opts.headerRight);
	    }
            let header = HtmlUtils.div([ATTR_STYLE,HU.css('position','relative','text-align','left'),ATTR_CLASS,'ramadda-popup-header'],hdr);
            html = header + html;
        }

        if(opts.decorate || opts.header) {
            html = HU.div([ATTR_CLASS,"ramadda-popup"], html);
        }


        let innerId = HU.getUniqueId("model_inner");
        if(opts.modal || opts.modalStrict) {
            html  = HU.div([ATTR_ID, innerId, ATTR_STYLE,opts.modalContentsCss,ATTR_CLASS,"ramadda-modal-contents"],html);
            html = HU.div([ATTR_CLASS,'ramadda-modal ' + (opts.modalStrict?'ramadda-modal-strict':'')],html);
        }

        let popup=   $(html).appendTo("body");
	if(true || opts.tooltip) {
	    popup.find('a,div').tooltip({
		classes: {"ui-tooltip": "wiki-editor-tooltip"},
		content: function () {
		    return $(this).prop(ATTR_TITLE);
		},
		show: { effect: 'slide', delay: 500, duration: 400 },
		position: { my: "left top", at: "right top" }
	    });
	}

        if(opts.remove) {
            popup.attr("removeonclose","true");
        }
        if(opts.inPlace) {
            let src =  $("#" + opts.contentId);
            let dest = $("#"+ parentId);
            dest.css("display","block").css(ATTR_WIDTH,"fit-content").css("height","fit-content");
            src.appendTo(dest);
            src.show();
        }


        if(opts.anchor) {
            if(opts.width) {
                popup.css(ATTR_WIDTH,opts.width);
            }
            popup.position({
                of: opts.anchor,
                my: opts.my,
                at: opts.at,
                collision:opts.fit?"fit fit":null
            });
	    //          console.log(opts.my +" " + opts.at);
        }


        if(opts.animate && opts.animate!=="false") {
	    opts.animateSpeed = +opts.animateSpeed;
	    popup.hide();
	    if(opts.slideLeft) {
		let at = "right bottom";
		popup.position({
                    of: opts.anchor,
                    my: opts.my,
                    at: at,
                    collision:opts.fit?"fit fit":null
		});
		popup.show("slide", { direction: "right" }, +opts.animateSpeed);
	    } else {
		popup.show(opts.animateSpeed);
	    }

        } else {
            popup.show();
        }


        if(opts.resizable) {
            if(opts.modal) {
                $("#" + innerId).resizable();
            } else {
                popup.resizable();
                //          popup.resizable({containment: "parent",handles: 'se',});
            }
	}


        if(opts.draggable) {
            if(opts.modal) {
                $("#" + innerId).draggable();
            } else {
                popup.draggable();
                //          popup.resizable({containment: "parent",handles: 'se',});
            }
        } else if(!opts.sticky) {
	    //Only set this if we don't have a toggleid cause if we
	    //do have one then the popup is meant to be persistent
	    if(!opts.toggleid)
		HtmlUtils.setPopupObject(popup);
        }


        if(opts.header) {
	    $("#" + id +"_close").click(function() {
                popup.hide();
                if(opts.callback) {
		    opts.callback(popup);
		}
                if(opts.remove) {
                    popup.remove();
                }
            });
        }

        if(opts.initCall) {
            if(typeof opts.initCall == "string") {
		window.ramaddaGlobalDialog=popup;
                eval(opts.initCall);
            } else {
                opts.initCall();
            }
        }
	if(opts.toggleid) {
	    this.toggleDialogs[opts.toggleid] = popup;
	}
	Translate.translate(popup);
        return popup;
    },
    //If value==null then remove the param
    addToDocumentUrl:function(name,value,append) {
        if(Utils.isPost()) { return}
        let url = String(window.location);
        if(!append) {
            url = new URL(url);
            if(value===null) {
                url.searchParams.delete(name);
            } else {
                url.searchParams.set(name,value);
            }
            //      let regex = new RegExp("(\\&|\\?)?" + name+"=[^\&]+(\\&|$)+", 'g');
            //      url = url.replace(regex,"");
        } else  {
            if (!url.includes("?")) url += "?";
            url += "&" + HtmlUtils.urlArg(name,value);
        }

        try {
            if (window.history.replaceState)
                window.history.replaceState("", "", url);
        } catch (e) {
            console.log("err:" + e);
            console.trace();
        }
	return url;
    },
    removeFromDocumentUrl:function(arg) {
        try {
            let url = String(window.location);
	    let regex = new RegExp('[?&]' + arg + '=[^&]*(&|$)', 'gi');
	    url = url.replace(regex, '$1');
	    url = url.replace(/&$/, '').replace(/\?$/, '');
            if (window.history.replaceState) {
                window.history.replaceState("", "", url);
	    }
        } catch (e) {
            console.log("err:" + e);
        }
    },
    getUrlArgument: function(arg) {
        const urlParams = new URLSearchParams(window.location.search);
        return urlParams.get(arg);
    },
    pre: function(attrs, inner) {
        return this.tag("pre", attrs, inner);
    },
    div: function(attrs, inner) {
        return this.tag("div", attrs, inner);
    },
    center: function(inner, attrs) {
        return this.tag("center", attrs, inner);
    },
    span: function(attrs, inner) {
        return this.tag("span", attrs, inner);
    },
    movie:function(path, attrs) {
        let a = "";
        if(attrs) a=HU.attrs(attrs);
        return "<video src='" + path +"' " + a +" controls='controls'></video>";
    },
    image: function(path, attrs) {
        return "<img " + this.attrs(["src", path, "border", "0"]) + " " + this.attrs(attrs) + "/>";
    },
    swapHtml:function(srcSelector, targetSelector) {
        $(document).ready(function(){
            let src = $(srcSelector); 
            let target=$(targetSelector);
            src.appendTo(target);
        });

    },
    table: function(attrs, inner) {
        return HU.tag("table",attrs,inner);
    },
    tr: function(attrs, inner) {
        return this.tag("tr", attrs, inner);
    },
    css: function(...attrs) {
        if(attrs.length==1 && Array.isArray(attrs[0])) attrs = attrs[0]

        let css = "";
        for(let i=0;i<attrs.length;i+=2) {
            css +=attrs[i]+":" + attrs[i+1]+";";
        }
        return css;
    },
    h2: function(h) {
        return HU.tag("h2",[],h);
    },
    h3: function(h) {
        return HU.tag("h3",[],h);
    },
    cssTag: function(css) {
        return '<style type="text/css">\n' + css + '</style>';
    },
    cssLink: function(url) {
        return "<link href='" + url +"'  rel='stylesheet'  type='text/css' />";
    },  
    javascriptLink: function(url) {
        return "<script type='text/javascript' src='" + url+"'></script>";
    },  
    BR_ENTITY:"&#10;",
    makeMultiline:function(l) {
	return Utils.join(l,HtmlUtils.BR_ENTITY);
    },
    getDimension(d) {
        if(!d) return null;
        d = String(d).trim();
        if(d.match("calc")) {
            //correct for calc(x-y)
            d = d.replace(/calc *\(/,"calc(").replace(/-/," - ").replace(/\+/," + ");
            d = d.replace(/([0-9]) *\)/,"$1px)");
        }
        if(d.endsWith(")") || d.endsWith("%") || d.endsWith("px") || d.endsWith("vh")) {
            return d;
        }
        return d+"px";
    },
    td: function(attrs, inner) {
        return this.tag("td", attrs, inner);
    },
    tds: function(attrs, cols) {
	if(!cols) return "";
	if(!Array.isArray(cols)) cols = [cols];
        let html = "";
        for (let i = 0; i < cols.length; i++) {
            html += this.td(attrs, cols[i]);
        }
        return html;
    },
    ths: function(attrs, cols) {
        let html = "";
        for (let i = 0; i < cols.length; i++) {
            html += this.th(attrs, cols[i]);
        }
        return html;
    },    
    onReturnEvent: function(selector,func) {
        if((typeof selector) == "string") selector = $(selector);
        selector.keyup(function(event) {
            let keycode = (event.keyCode ? event.keyCode : event.which);
            if(keycode == 13) {
                func($(this),event);
            }
        });
    },
    formatTable: function(id, args, callback) {
        HtmlUtils.loadJqueryLib('DataTable',[ramaddaCdn +'/lib/datatables-1.13.1/datatables.min.css'],
                                [ramaddaCdn + '/lib/datatables-1.13.1/datatables.min.js'],
                                id,()=>{
                                    $(id).each(function() {
                                        if($.fn.dataTable.isDataTable(this)) {
                                            return;
                                        }
                                        let table = HtmlUtils.formatTableInner($(this),args);
                                        if(callback) callback($(this),table);
                                    });
                                });
    },
    formatTableInner: function(table, args) {
        let options = {
            paging: false,
            ordering: false,
            info: false,
            searching: false,
            scrollCollapse: true,
            retrieve: true,
            fixedHeader:false,
        };
        if (args)
            $.extend(options, args);
        
        let height = options.height || table.attr("table-height");
        if (height)
            options.scrollY = height;
        let ordering = table.attr("table-ordering");
        if (ordering)
            options.ordering = (ordering == "true");
        let searching = table.attr("table-searching");
        if (searching)
            options.searching = (searching == "true");
        let paging = table.attr("table-paging");
        if (paging)
            options.paging = (paging == "true");
        if (Utils.isDefined(options.scrollY)) {
            let sh = "" + options.scrollY;
            if (!sh.endsWith("px")) options.scrollY += "px";
        }
        table.DataTable(options);
	return table;
    },

    th: function(attrs, inner) {
        return this.tag("th", attrs, inner);
    },
    attrSelect:function(name,value) {
        return  "[" + name+"=\"" + value +"\"]";
    },
    scrollToAnchor:function(aid,offset,containerId) {
        if(!Utils.isDefined(offset)) offset=-50;
        let aTag = $("a[name='"+ aid +"']");
        if(!offset) offset=0;
        offset = +offset;
        //Offset a bit
        Utils.scrollToAnchorTime = new Date();
	if(containerId) {
	    let container = jqid(containerId);
	    let scrollTo = aTag;
	    // Or you can animate the scrolling:
	    container.animate({
		scrollTop: scrollTo.offset().top - container.offset().top + container.scrollTop()
	    },1000);
	} else {
            $('html,body').animate({scrollTop: aTag.offset().top+offset},'slow');
            location.hash = aid;
	}

    },
    scrollVisible: function(contents, child, animate) {
        if(child.length==0 || contents.length==0)  return;
        let diff = child.offset().top-contents.offset().top;
        if(!Utils.isDefined(animate)) animate= 1000;
        contents.animate({
            scrollTop: diff+contents.scrollTop()
        }, animate);
    },
    initNavPopup: function(id,args) {
        let opts = {
            align:"left"
        };
        if(args) $.extend(opts, args);
        $("#"+id).click(function() {
            let popup = HtmlUtils.setPopupObject($("#"+ id+"-popup"));
            let my,at;
            if(opts.align=="right") {
                my = "right top";
                at = "right top";
            } else {
                my = "left top";
                at = "right+5 top";
            }
            popup.show();
            popup.position({
                of: popup.parent(),
                my: my,
                at: at,
                collision: "fit fit"
            });

        });
    },


    navLinkClicked: function(id,offset) {
        let links =     $(".ramadda-nav-left-link");    
        links.removeClass("ramadda-nav-link-active");
        $("#" + id+"_href").parent().addClass("ramadda-nav-link-active");
        HtmlUtils.scrollToAnchor(id,offset);
    },
    initNavLinks: function(args) {
        let opts = {
            leftOpen:true,
            showToggle:true,
            leftWidth:"250px"
        }
        if(args) $.extend(opts,args);
        let left = $(".ramadda-nav-left");
        let right = $(".ramadda-nav-right");    
        
        if(opts.showToggle) {
            let menu = HU.div(["toggle-state",opts.leftOpen?"open":"closed",
			       ATTR_TITLE,"Toggle left",
			       ATTR_ID,"ramadda-nav-left-toggle",
			       ATTR_CLASS,"ramadda-nav-left-toggle"], HtmlUtils.getIconImage("fa-bars"));
            $(menu).appendTo(right);
            $("#ramadda-nav-left-toggle").click(function() {
                let closed = $(this).attr("toggle-state")==="closed";
                if(closed) {
                    left.show();
                    right.animate({"margin-left":opts.leftWidth});
                } else {
                    left.hide();
                    right.animate({"margin-left":"0px"});
                }
                $(this).attr("toggle-state",closed?"open":"closed");
            });
        }

        let linksContainer = $(".ramadda-nav-left-links");
        linksContainer.mouseenter(function() {
            Utils.linksMouseIn = true;
        });
        linksContainer.mouseleave(function() {
            Utils.linksMouseIn = false;
        });     

        let anchors =   $(".ramadda-nav-anchor");
        let links =     $(".ramadda-nav-left-link");    
        let lastTop = null;
        $(window).scroll(function(){
            if(Utils.linksMouseIn) return;
            let amScrolling = false;
            if(Utils.scrollToAnchorTime) {
                let now  = new Date();
                amScrolling = (now.getTime()-Utils.scrollToAnchorTime.getTime())<2000;
            }
            let docTop = $(window).scrollTop();
            let docBottom = docTop + $(window).height();
            let topMost= null;
            let top = null;
            anchors.each(function() {
                let elemTop = $(this).offset().top;     
                let elemBottom = elemTop + $(this).outerHeight(true); 
                //      console.log("doc:" + docTop + " " + docBottom +"  "+ elemTop +" " + elemBottom);
                let inView = ((elemTop <= docBottom) && (elemTop >= docTop)) ||
                    ((elemBottom <= docBottom) && (elemBottom >= docTop)) ||
                    ((elemBottom >= docBottom) && (elemTop <= docTop));
                if(!inView) return;
                if(top==null) {
                    top = elemTop;
                    topMost= $(this);
                } else if(elemTop<top) {
                    top = elemTop;
                    topMost= $(this);
                }
            });
            if(!topMost) return;
            if(lastTop && lastTop.attr("name") == topMost.attr("name")) return;
            lastTop = topMost;
            links.removeClass("ramadda-nav-link-active");
            let activeLink = null;
            links.each(function() {
                if($(this).attr("navlink") == topMost.attr("name")) {
                    $(this).addClass("ramadda-nav-link-active");
                    activeLink = $(this);
                }
            });
            if(!amScrolling && activeLink) {
                HtmlUtils.scrollVisible(activeLink.parent(), activeLink,100);
            }

        });
    },
    setFormValue: function(id, val) {
        $("#" + id).val(val);
    },
    setHtml: function(id, val) {
        $("#" + id).html(val);
    },
    formTable: function() {
        return this.openTag("table", [ATTR_CLASS, "formtable", "cellspacing", "0", "cellspacing", "0"]);
    },
    formTableClose: function() {
        return this.closeTag("table");
    },
    formEntryTop: function(label, value, value2) {
        if(value2) 
            return HU.tag("tr", ["valign", "top"],
                          HU.tag("td", [ATTR_CLASS, "formlabel", "align", "right"],
                                 label) +
                          HU.tag("td", [],   value) +
                          HU.tag("td", [],   value2));
        return this.tag("tr", ["valign", "top"],
                        this.tag("td", [ATTR_CLASS, "formlabel", "align", "right"],
                                 label) +
                        this.tag("td", [],
                                 value));

    },
    formEntry: function(label, value) {
        return this.tag("tr", [],
                        this.tag("td", [ATTR_CLASS, "formlabel", "align", "right"],
                                 label) +
                        this.tag("td", [],
                                 value));

    },
    appendArg: function(url, arg, value) {
        if (url.indexOf("?") < 0) {
            url += "?";
        } else {
            url += "&";
        }
        url += HtmlUtils.urlArg(arg, value);
        return url;
    },
    getUrl: function(path, args) {
        if (args.length > 0) {
            path += "?";
        }
        for (let i = 0; i < args.length; i += 2) {
            if (i > 0) {
                path += "&";
            }
            path += this.urlArg(args[i], args[i + 1]);
        }
        return path;
    },
    b: function(inner) {
        return this.tag("b", [], inner);
    },

    td: function(attrs, inner) {
        return this.tag("td", attrs, inner);
    },
    th: function(attrs, inner) {
        return this.tag("th", attrs, inner);
    },    

    tag: function(tagName, attrs, inner) {
        if (!inner && (typeof attrs) == "string") {
            inner = attrs;
            attrs = null;
        }
        let html = "<" + tagName + " " + this.attrs(attrs) + ">";
        if (inner != null) {
            html += inner;
        }
        html += "</" + tagName + ">";
        return html;
    },
    openTag: function(tagName, attrs,contents) {
        let html = '<' + tagName + ' ' + this.attrs(attrs) + '>';
	if(contents!=null) html+=contents;
        return html;
    },
    open: function(tagName, attrs, extra) {
        let html = '<' + tagName + ' ' + this.attrs(attrs) + '>';
        if(extra) html+= extra;
        return html;
    },
    openRow: function() {
        return this.openTag("div", [ATTR_CLASS, "row"]);
    },
    closeRow: function() {
        return this.closeTag("div");
    },


    openDiv: function(attrs) {
        return this.openTag("div", attrs);
    },
    closeDiv: function() {
        return this.closeTag("div");
    },

    closeTag: function(tagName) {
        return "</" + tagName + ">\n";
    },
    close: function(...args) {
        let html = "";
        args.forEach(a=>{html+= "</" + a + ">\n";});
        return html;
    },
    urlArg: function(name, value) {
        return name + "=" + encodeURIComponent(value);
    },
    attrSelect: function(name, value) {
        if(!Utils.isDefined(value)) return "[" + name +"]";
        return "[" + name +"='" + value+ "']";
    },
    space:function(cnt) {
	let s = "";
	for(let i=0;i<cnt;i++) s+="&nbsp;";
	return s;
    },
    attr: function(name, value) {
        if(value==null)
            return " " + name + " ";
        return " " + name + "=" + this.qt(value) + " ";
    },

    attrs: function(list) {
        if (!list) return "";
	if(typeof list=="string") return list;
        let html = "";
        if (list == null) return html;
        if (list.length == 1) return list[0];
        for (let i = 0; i < list.length; i += 2) {
            let name = list[i];
            if (!name) continue;
            let value = list[i + 1];
            if (value == null) {
                html += " " + name + " ";
            } else {
                html += this.attr(name, value);
            }

        }
        return html;
    },
    styleAttr: function(s) {
        return this.attr(ATTR_STYLE, s);
    },

    classAttr: function(s) {
        return this.attr(ATTR_CLASS, s);
    },
    makeFullScreen:function(elem) {
        if(elem==null) {
            console.log("HtmlUtils.makeFullScreen: null elem argument");
            console.trace();
            return;
        }
        if (elem.requestFullscreen) {
            elem.requestFullscreen();
        } else if (elem.webkitRequestFullscreen) { /* Safari */
            elem.webkitRequestFullscreen();
        } else if (elem.msRequestFullscreen) { /* IE11 */
            elem.msRequestFullscreen();
        } else {
            console.err("No full screen");
        }
    },
    makeEnlargable:function(id,enlargeMsg,shrinkMsg) {
	$( document ).ready(function() {
	    HU.makeEnlargableInner(id,enlargeMsg, shrinkMsg);
	});
    },
    makeEnlargableInner:function(id,enlargeMsg,shrinkMsg) {
	let outer = $('#'+id);
	let inner = $('#'+id+'_inner');
	let contents = $('#'+id+'_contents');	
	let enlarged = false;
	let shrunkHeight= contents.height();
	enlargeMsg = Utils.getStringDefined(enlargeMsg,'Show more');
	shrinkMsg = Utils.getStringDefined(shrinkMsg,'Show less');	
	let bbutton = HU.div([ATTR_ID,id+'_button',ATTR_CLASS,'ramadda-enlarge-button ramadda-enlarge-bbutton ramadda-clickable'],enlargeMsg);
	$(bbutton).appendTo(outer).click(function(){
	    enlarged = !enlarged;
	    contents.css('height',enlarged?'':shrunkHeight);
	    $(this).html(enlarged?shrinkMsg:enlargeMsg);
	});
    },
    makeExpandable:function(selector,fullScreen,args) {
	let opts = {
	    right:'0px',
	    top:'0px',
	    icon:'fa-solid fa-maximize'
	    
	}
	if(args) $.extend(opts,args);
        let icon =HtmlUtils.getIconImage(opts.icon,[ATTR_CLASS,CLASS_CLICKABLE],[]);
        let id = HtmlUtils.getUniqueId();
	let style = HU.css('display','none','cursor','pointer',
			   'text-align','right','position','absolute','margin-top','0px');
	if(opts.left) style+=HU.css('left',opts.left);
	else style+=HU.css('right',opts.right);
	style+=HU.css('top',opts.top);		
        let html= HtmlUtils.div([ATTR_ID,id,ATTR_TITLE,"Expand", ATTR_CLASS,"ramadda-expandable-link",
				 ATTR_STYLE,style],icon);
        $(selector).append(html);
        let btn = $("#"+id);
        let expandNow = $(selector).hasClass("ramadda-expand-now");
        btn.attr("data-expanded",expandNow);
        let origBackground = $(selector).css("background");
        $(selector).mouseenter(function() {
            btn.css("display","block");
        });
        $(selector).mouseleave(function() {
            btn.css("display","none");
        });
        let expandIt = function() {
            let icon;
            let expanded = $(this).attr("data-expanded")=="true";
            if(expanded) {
                icon  = HtmlUtils.getIconImage("fa-expand-arrows-alt");
                $(this).attr(ATTR_TITLE,"Expand");
                $(selector).css("left","").css("right","").css("top","").css("bottom","").css("position","relative").css("height", "").css("z-index","").css("background",origBackground?origBackground:"");
		$(selector).removeClass('ramadda-expandable-expanded');
                btn.css("display","none");
                $(selector).find(".ramadda-expandable-target").each(function() {
                    $(this).attr("isexpanded","false");
		    //                    $(this).css("height",$(this).attr("original-height"));
                    $(this).attr(ATTR_STYLE,$(this).attr("original-style"));		    
                });
            } else {
                let h = $(window).height();
                icon  = HtmlUtils.getIconImage("fa-compress-arrows-alt");
                let top = $(selector).offset().top;
                $(this).attr(ATTR_TITLE,"Contract");
                if(fullScreen) {
                    let target  = $(selector).find(".ramadda-expandable");
                    target.css("background","#fff");
                    HtmlUtils.makeFullScreen(target.get(0));
                    return
                }               
		$(selector).addClass('ramadda-expandable-expanded');
                $(selector).css("left","50px").css("right","5px").css("top","5px").css("position","fixed").css("z-index","2000").css("background","#fff").css("height",h+"px");
                $(selector).find(".ramadda-expandable-target").each(function() {
		    //                    $(this).attr("original-height",$(this).css("height"));
		    $(this).attr('original-style',$(this).attr(ATTR_STYLE));
                    $(this).attr("isexpanded","true");
                    let height = $(this).attr("expandable-height");
                    if(!height) if(height==-1) return;
                    $(this).css("height",height || "95vh");
                });
                btn.css("display","block");
            }
            $(this).attr("data-expanded",(!expanded)+"");
            $(this).html(icon);
            if (window["ramaddaDisplayCheckLayout"]) {
                ramaddaDisplayCheckLayout();
            }
            if (window["ramaddaMapCheckLayout"]) {
                ramaddaMapCheckLayout();
            }
        };
        $("#" +id).click(expandIt);
        if(expandNow) {
            expandIt();
        }
    },
    makeDraggable:function(selector) {
        $(selector).draggable({
            zIndex:1000,
            drag: function( event, ui ) {
            },
            start: function( event, ui ) {
                //Make the draggable be absolute
                if(!$(this).attr("started")) {
                    $(this).attr("started",true);
                    let o = $(this).offset();               
                    $(this).attr("oleft",o.left);
                    $(this).attr("otop",o.top);             
                    $(this).css("position","absolute").css("left",o.left+"px").css("top",o.top+"px");
                    console.log("start:" + o.left + " " + o.top);
                }
            },
        });
    },
    idAttr: function(s) {
        return this.attr(ATTR_ID, s);
    },
    link: function(url, label, attrs) {
        if (attrs == null) attrs = [];
        let a = [];
        for (i in attrs)
            a.push(attrs[i]);
        attrs = a;
        attrs.push("url");
        attrs.push(url);
        attrs.push(ATTR_CLASS);
        attrs.push("ramadda-link");
        if (attrs.style) attrs.style += "display:inline-block;";
        else attrs.style = "style='display:inline-block;'";
        return this.div(attrs, label);
    },
    getEntryImage: function(entryId, tag) {
        var index = tag.indexOf("::");
        if (index >= 0) {
            var toks = tag.split("::");
            if (toks[0].trim().length > 0) {
                entryId = toks[0].trim();
            }
            var attach = toks[1].trim();
            return RamaddaUtil.getUrl( "/metadata/view/" + attach + "?entryid=" + entryId);
        }
        return tag;
    },
    jsLink: function(inner,content,extra) {
        extra = extra||[];
        return HU.href("javascript:noop();",  
                       content,extra);

    },
    url:function(path,args) {
	let url = path;
	for(let i=0;i<args.length;i+=2) {
	    if(i==0 && url.indexOf("?")<0) url+="?";
	    else url+="&";
	    let name = args[i];
	    let value=args[i+1];
	    url += encodeURIComponent(name) + "=" + encodeURIComponent(value);
	}	
	return url;
    },
    href: function(url, label, attrs) {
        if (attrs == null) attrs = [];
        let a = [];
        for (let i in attrs)
            a.push(attrs[i]);
        attrs = a;
        attrs.push("href");
        attrs.push(url);
        if (!Utils.isDefined(label) || label == "") label = url;
        return this.tag("a", attrs, label);
    },

    onClick: function(call, html, attrs) {
        let myAttrs = ["onclick", call, ATTR_STYLE, "color:black;   cursor:pointer;"];
        if (attrs != null) {
            for (let i = 0; i < attrs.length; i++) {
                myAttrs.push(attrs[i]);
            }
        }
        return this.tag("a", myAttrs, html);
    },

    checkbox: function(id, attrs, checked,label) {
	attrs = attrs||[];
	if(!Utils.stringDefined(id)) {
	    for(let i=0;i<attrs.length;i+=2) {
		if(attrs[i]==ATTR_ID) {
		    id  = attrs[i+1];
		    break;
		}
	    }
	}
        attrs.push(ATTR_ID);
        attrs.push(id);
        attrs.push("type");
        attrs.push("checkbox");
	if(!attrs.includes("value")) {
            attrs.push("value");
            attrs.push("true");
	}
        if (checked) {
            attrs.push("checked");
            attrs.push(null);
        }
        let cbx =  this.tag("input", attrs);
        if(label) {
	    let title='';
	    for(let i=0;i<attrs.length;i+=2) {
		if(attrs[i]==ATTR_TITLE) {
		    title = attrs[i+1];
		    break;
		}
	    }
	    let cbxAttrs = ["for", id];
	    if(Utils.stringDefined(title)) {
		cbxAttrs.push(ATTR_TITLE);
		cbxAttrs.push(title);
	    }
            cbx += "&nbsp;" + HU.tag("label",cbxAttrs,label);
        }
        return cbx;
    },

    radioGroup: function(name, list,selected) {
	let html = '';
	list.forEach((item,idx)=>{
            let label = item;
            if(Array.isArray(item)) {
                label=item[1];
                item = item[0];
            } else if(Utils.isDefined(item.value)) {
		label = item.label??item.value;
		item = item.value;
	    }
	    let id = HU.getUniqueId('radio_');
	    html+=HU.radio(id,name,'',item,selected?selected==item:idx==0)+
		HU.tag('label',['for',id],label) + '<br>';
	    html+='\n';
	});
	return html;
    },

    radio: function(id, name, radioclass, value, checked, extra,label) {
        if (!extra) extra = "";
        let html = "<input id='" + id + "'  class='" + radioclass + "' name='" + name + "' type=radio value='" + value + "' ";
        if (checked) {
            html += " checked ";
        }
        html += " " + extra
        html += "/>";
	if(label) {
	    html+=HU.tag('label',['for',id],label);
	}
        return html;
    },


    handleFormChangeShowUrl: function(entryid, formId, outputId, skip, hook, params) {
	let opts = {
	    showInputField:true,
	    includeCopyArgs:false
	}
	if(params) $.extend(opts,params);
        if (skip == null) {
            skip = [".*OpenLayers_Control.*", "authtoken"];
        }
        let url = $("#" + formId).attr("action") + "?";
	url = url.replace(/ +/g,'_');
        let inputs = $("#" + formId + " :input");
        let cnt = 0;
        let pairs = [];
	let args = "";
	let seenValue = {};
	let added = (name,value) =>{
	    let key = name +'---' + value; 
	    if(seenValue[key])  return true;
	    seenValue[key] = true;
	    return false;
	}


        inputs.each(function(i, item) {
            if (item.name == "" || item.value == null || item.value == "") return;
            if (item.value == "-all-") return;
            if (item.type == "submit") {
                if (item.value == "Cancel") {
                    return
                }
            }

            if (item.type == "checkbox") {
                if (!item.checked) {
                    return;
                }
            }
            if (item.type == "radio") {
                if (!item.checked) {
                    return;
                }
            }
            if (item.attributes && item.attributes["default"]) {
                if (item.attributes["default"].value == item.value) {
                    return;
                }
            }

            if (skip != null) {
                for (var i = 0; i < skip.length; i++) {
                    let pattern = skip[i];
                    if (item.name.match(pattern)) {
                        return;
                    }
                }
            }
            if(item.id=="formurl") return;

	    let defaultValue = item.attributes["default-value"]?.value;
	    if(item.name=='dbsortdir1') {
		//		console.dir(item.attributes);
	    }
	    //            console.log("item:"   + item.id +" type:" +item.type + " value:" + item.value);
            var values = [];
            if (item.type == "select-multiple" && item.selectedOptions) {
                for (a in item.selectedOptions) {
                    option = item.selectedOptions[a];
                    if (Utils.isDefined(option.value)) {
			if(defaultValue!=option.value)
                            values.push(option.value);
                    }
                }
            } else {
		if(defaultValue!=item.value) {
                    values.push(item.value);
		}
            }


	    if(values.length==1) {
		values.forEach(value=>{
		    if(item.name=="entryid") return;
		    if(item.name=="submit") return;
		    if(item.name=="output") return;		    		    
		    args+=item.name+"=" + value+"\n";
		});
	    }

            for (v in values) {
                value = values[v];
                pairs.push({
                    item: item,
                    value: value
                });
		if(!added(item.name,value)) {
                    if (cnt > 0) url += "&";
                    cnt++;		    
                    url += encodeURIComponent(item.name) + "=" + encodeURIComponent(value);
		}
            }
        });

        let base = window.location.protocol + "//" + window.location.host;
        url = base + url;

        let input = opts.showInputField?HtmlUtils.input("formurl", url, [ATTR_SIZE, "80",ATTR_ID,"formurl"]):
	    HtmlUtils.hidden("formurl", url, [ATTR_SIZE, "80",ATTR_ID,"formurl"]);
        let html = HU.vspace() +HtmlUtils.div([ATTR_CLASS, "ramadda-form-url"], 
					      input+SPACE2+
					      (opts.includeCopyArgs?
					       HU.span([ATTR_ID,'argscopy',ATTR_CLASS,CLASS_CLICKABLE,ATTR_TITLE,'Copy json for subset action'],
						       HtmlUtils.getIconImage('fas fa-earth-americas')) +SPACE2:'')+
					      HU.span([ATTR_ID,'clipboard',ATTR_CLASS,CLASS_CLICKABLE,ATTR_TITLE,'Copy URL to clipboard'],
						      HtmlUtils.getIconImage('fas fa-clipboard')) + SPACE2+
					      HU.href(url, HtmlUtils.getIconImage('fas fa-link'),[ATTR_TITLE,'Form URL']));
        if (hook) {
            html += hook({
                entryId: entryid,
                formId: formId,
                inputs: inputs,
                itemValuePairs: pairs
            });
        }
        $("#" + outputId).html(html);
	jqid('argscopy').click(()=>{
	    Utils.copyToClipboard(args);
	    alert('args for subset are copied to clipboard');
	});
	jqid('clipboard').click(()=>{
	    Utils.copyToClipboard(jqid('formurl').val());
	    alert('URL copied to clipboard');
	});	
    },
    preventSubmitOnEnter:function(event) {
        if (event.key === "Enter") {
            event.preventDefault();
        }
    },

    makeUrlShowingForm: function(entryId, formId, outputId, skip, hook,args) {
        jqid(formId).find(':input').change(function() {
            HtmlUtils.handleFormChangeShowUrl(entryId, formId, outputId, skip, hook,args);
        });
        HtmlUtils.handleFormChangeShowUrl(entryId, formId, outputId, skip, hook,args);
    },
    makeSelectTagPopup:function(select,args) {
	args = args??{};
	let opts ={
	    label:'Select',
	    icon:false,
	    single:false,
	    buttonLabel:'Select',
	    hide: false,
	    addBreak:false,
	    after:false,
	    wrap:'${widget}',
	    makeButton:true,
	    makeButtons:true
	}
	if(typeof select =='string') select=$(select);
	if(args.icon && !args.buttonLabel)
	    args.buttonLabel = HU.getIconImage('fas fa-list-check');
	$.extend(opts,args);
	if(!opts.tooltip)
	    opts.tooltip=opts.single?'Select value':'Select multiple';

	let label = opts.label??'Select';
	if(opts.hide)
	    select.hide();

	let guid = HU.getUniqueId('btn');
	let btn =HU.span([ATTR_CLASS,'ramadda-clickable',
			  ATTR_TITLE,opts.tooltip,ATTR_ID,guid],
			 opts.buttonLabel)+(opts.addBreak?'<br>':'');
	btn = opts.wrap.replace('${widget}',btn);
	if(opts.after)
	    select.after(' ' +btn);
	else
	    select.before(btn+SPACE);
	let optionMap = {};
	let handleChange = function(cbx,trigger) {
	    let option = optionMap[cbx.attr(ATTR_ID)];
	    if(!option) return;
	    let selected=cbx.is(':checked');
	    option.prop('selected',selected);
	    if(trigger) {
		select.change();
		if(select.iconselectmenu && select.iconselectmenu('instance')) {
		    select.iconselectmenu('refresh');
		}
	    }
	}

	let cbxChange = function() {
	    handleChange($(this),true);
	}
	let makeDialog = anchor=>{
	    let html = '';
	    let cbxs=[];
	    let radioName = HU.getUniqueId('radio');
	    select.find('option').each(function() {
		let value = $(this).attr('value');
		if(!Utils.stringDefined(value)) return;
		let label = $(this).html();
		label = label.replace('&nbsp;',' ');
		if($(this).attr('img-src')) {
		    label = HU.image($(this).attr('img-src'),[ATTR_WIDTH,'16px']) +' ' +label;
		}

		let selected=$(this).is(':selected');
		let id = HU.getUniqueId('cbx')
		optionMap[id] = $(this);
		let cbx = opts.single?
		    HU.radio(id, radioName,'',label,selected,null,label):
		    HU.checkbox(id,[],selected,label);

		let cbxLabel = $(this).html() +' ' + value;
		cbx = HU.div([ATTR_CLASS,'ramadda-select-tag',
			      ATTR_TITLE,value,
			      'tag',cbxLabel], cbx);
		cbxs.push(cbx);
	    });
	    let cbxInner = HU.div([ATTR_STYLE,HU.css("margin","5px", ATTR_WIDTH,"600px;","max-height","300px","overflow-y","auto")],    Utils.wrap(cbxs,"",""));
	    let inputId = HU.getUniqueId("input_");
	    let input = HU.input("","",[ATTR_STYLE,HU.css(ATTR_WIDTH,"200px;"), ATTR_PLACEHOLDER,'Search for ' + label.toLowerCase(),ATTR_ID,inputId]);
	    if(opts.makeButtons) {
		let buttons = '';
		buttons+=HU.space(1)+HU.div([ATTR_CLASS,'ramadda-select-action','data-action','clear'],'Clear all');
		buttons+=HU.space(1)+HU.div([ATTR_CLASS,'ramadda-select-action','data-action','selectshown'],'Select shown');	    
		buttons+=HU.space(1)+HU.div([ATTR_CLASS,'ramadda-select-action','data-action','showselected'],'Show selected');
		buttons+=HU.space(1)+HU.div([ATTR_CLASS,'ramadda-select-action','data-action','showall'],'Show all');	   
		input+=HU.div([ATTR_STYLE,
			       HU.css('border-bottom','var(--basic-border)','padding','6px')],buttons);
	    }

	    let contents = HU.div([ATTR_STYLE,HU.css("margin","10px")], HU.center(input) + cbxInner);
	    let dialog = HU.makeDialog({content:contents,anchor:anchor,title:label,
					draggable:true,header:true});

	    dialog.find(":checkbox").change(cbxChange);
	    dialog.find(":radio").change(cbxChange);
	    let tags = dialog.find(".ramadda-select-tag");
	    dialog.find('.ramadda-select-action').button().click(function() {
		let action =$(this).attr('data-action');
		if(action=='showall') {
		    tags.show();
		    return
		}
		if(action=='showselected') {
		    tags.each(function() {
			let cbx = $(this).find(':checkbox');
			let selected=cbx.is(':checked');
			if(!selected) $(this).hide();
		    });
		    return

		}
		if(action=='selectshown') {
		    tags.each(function() {
			if($(this).is(':visible')) {
			    let cbx = $(this).find(':checkbox');
			    cbx.prop('checked',true);
			    handleChange(cbx,false);
			}
		    });
		    select.change();
		}


		if(action=='clear') {
		    tags.each(function() {
			let cbx = $(this).find(':checkbox');
			cbx.prop('checked',false);
			handleChange(cbx,false);
		    });
		    select.change();
		}
	    });
	    $("#"+inputId).keyup(function(event) {
		let text = $(this).val().trim().toLowerCase();
		tags.each(function() {
		    if(text=='')
			$(this).show();
		    else {
			let tag = $(this).attr("tag");
			if(tag) {
			    tag = tag.toLowerCase();
			    if(tag.indexOf(text)>=0)
				$(this).show();
			    else
				$(this).hide();
			}
		    }
		});
	    });
	}
	if(opts.makeButton &&!opts.icon)
	    jqid(guid).button();
	jqid(guid).click(function() {
	    makeDialog($(this));
	});
    },

    select: function(name, attrs,list, selected,maxWidth,debug) {
       var select = this.openTag("select", attrs);
        select+=HU.makeOptions(list,selected,maxWidth,debug);
        select+=this.closeTag("select");
        return select;
    },
    makeOptions: function(list, selected,maxWidth,debug) {
        let options = "";
        list.forEach(item=>{
	    let attrs = [];
            let label = item;
            if(Array.isArray(item)) {
                label=item[1];
                item = item[0];
            } else if(Utils.isDefined(item.value)) {
		if(item.imgsrc)
		    attrs.push('img-src',item.imgsrc);
		if(item.datatitle)
		    attrs.push(ATTR_TITLE,item.datatitle);
		if(item.datastyle)
		    attrs.push('data-style',item.datastyle);		
		let value = String(item.value).replace(/"/g,'\\"');
		label = item.label??value;
		item = value;
	    }
	    let fullLabel  = label;
            if(maxWidth && label.length>maxWidth)
                label = label.substring(0,maxWidth)+'...';
            let extra = '';
            if(selected && Array.isArray(selected)) {
                if(selected.indexOf(item)>=0) {
                    extra=' selected ';
                }
            } else {
                if(selected == item) extra=" selected ";
            }
	    let tt = fullLabel;
	    if(item && item!=tt) {
		//check for encoding
		if(item.indexOf && item.indexOf('base64')<0) 
		    tt = tt+HU.getTitleBr() + item;
	    }
	    attrs.push(ATTR_TITLE,tt,extra,null,'value',item);
            options+=HU.tag("option",attrs,label);
        });
        return options;
    },

    datePicker: function(name,value,attrs) {
        attrs.push(ATTR_SIZE);
        attrs.push("8");
        return  HtmlUtils.input(name, value,attrs);
    },

    makeClearDatePickerArgs:function(args) {
	let clear = {
	    showButtonPanel: true,
	    closeText: 'Clear',
	    onClose: function (e) {
		if ($(window.event.srcElement).hasClass('ui-datepicker-close')) {
		    $(this).val('');
		}
	    }
	}
	if(args)
	    $.extend(clear,args);
	return clear;
    },
    datePickerInit: function(id) {
        $("#" + id).datepicker({
	    onSelect:function(d,i) {
		if(d!==i.lastVal) {
		    $(this).change();
		}
	    },
	    dateFormat: 'yy-mm-dd',
	    changeMonth: true,
	    changeYear: true,
	    constrainInput:false,
	    yearRange: '1900:2100'});
    },
    rangeInput: function(name,id) {
        var html = '<form><div><input type="text" class="ramadda-slider-value sliderValue" data-index="0" value="10" /> <input type="text" class="ramadda-slider-value  sliderValue" data-index="1" value="90" /></div>' + HtmlUtils.div([ATTR_ID,id]) +"</form>";
        console.log(html);
        return html;
    },
    rangeInputInit: function(id) {
        $("#" + id ).slider({
            min: 0,
            max: 100,
            step: 1,
            values: [10, 90],
            slide: function(event, ui) {
                for (var i = 0; i < ui.values.length; ++i) {
                    $("input.sliderValue[data-index=" + i + "]").val(ui.values[i]);
                }
            }
        });
        $("input.sliderValue").change(function() {
            var $this = $(this);
            $("#slider").slider("values", $this.data("index"), $this.val());
        });
    },
    input: function(name, value, attrs) {
        return "<input " + HtmlUtils.attrs(attrs) + HtmlUtils.attrs(["value", value]) +(name==null?'':HtmlUtils.attrs("name", name))  + ">";
    },
    hidden: function(name, value, attrs) {
        return "<input type=hidden " + HtmlUtils.attrs(attrs) + HtmlUtils.attrs(["name", name, "value", value]) + ">";
    },    
    textarea: function(name, value, attrs) {
        return "<textarea " + HtmlUtils.attrs(attrs) + HtmlUtils.attrs(["name", name]) + ">" + value + "</textarea>";
    },
    initSelect: function(selector, args) {
        if(selector.length==0) return;
        let opts = {
            showEffect: "fadeIn",
            showEffectSpeed: 400,
            hideEffect: "fadeOut",
            hideEffectSpeed: 400,
	    autoWidth: false
        };
        if(args) $.extend(opts,args);
	$(document).ready(function() {
            HtmlUtils.loadJqueryLib('selectBoxIt',
				    [RamaddaUtil.getCdnUrl("/lib/selectboxit/stylesheets/jquery.selectBoxIt.css")],
                                    [RamaddaUtil.getCdnUrl("/lib/selectboxit/javascripts/jquery.selectBoxIt.min.js")],
                                    selector, ()=>{
					//Call later. This somehow fixes a major performance problem with
					//menus that are hidden
					setTimeout(()=>{
					    $(selector).selectBoxIt(opts);
					},100);
				    });
	});
    },
    valueDefined: function(value) {
        if (value != "" && value.indexOf("--") != 0) {
            return true;
        }
        return false;
    },


    squote: function(s) {
        return "'" + s + "'";
    },
    makeToggle: function(imageId,blockId,visible) {
        if(visible===null) visible = true;
        let img1 = ramaddaCdn + "/icons/togglearrowdown.gif";
        let img2 = ramaddaCdn + "/icons/togglearrowright.gif";
        $("#" + imageId).attr("state","open");
        $("#" + imageId).attr("src",img1);
        $("#" + imageId).css("cursor","pointer");       
        let open = (img) =>{
            img.attr("state","open");
            img.attr("src",img1);
            $("#" + blockId).show();
        };
        let close = (img) =>{
            img.attr("state","close");
            img.attr("src",img2);
            $("#" + blockId).hide();
        };
        $("#" + imageId).click(function(e){
            let state = $(this).attr("state");
            if(state=="open") {
                close($(this));
            } else {
                open($(this));
            }
            e.preventDefault();
        });
        if(!visible) {
            close($("#" + imageId));
        }
    },
    makeSplash: function(message,args) {
        let opts = {
            width:"50%",
            style:"",
            showOk:true
        };
        if(args) $.extend(opts,args);
        if(opts.src) {
            message = $("#" + opts.src).html();
	    
        }
        if(message == null || message.trim()=="") return;
        message=  HU.div([ATTR_STYLE,"margin:10px;"], message);

        let closeId = Utils.getUniqueId("close_");
        let close = HU.div([ATTR_ID,closeId,ATTR_CLASS,CLASS_CLICKABLE,                           
                            ATTR_STYLE,HU.css("position","absolute","right","10px","top","10px")],
                           HU.getIconImage("far fa-window-close"));
        let inner = close + message;
        if(opts.showOk) {
            inner += HU.center(HU.div([ATTR_ID,closeId+"_button"],"OK"));
        }
        inner = HU.div([ATTR_CLASS,"ramadda-shadow-box ramadda-splash",
                        ATTR_STYLE,HU.css(ATTR_WIDTH,opts.width)+ opts.style],
                       inner);

        let container = $(HU.div([ATTR_CLASS,"ramadda-splash-container"],inner)).appendTo($("body"));
        $('body').addClass("ramadda-splash-body");
        let closer = () =>{
            $('body').removeClass("ramadda-splash-body");           
            container.remove();
        };

        container.click( (event) => {
	    //          closer();
        }); 
        $("#"+ closeId).click(() =>{
            closer();
        });
        $("#"+ closeId+"_button").button().click(() =>{
            closer();
        });     


    },

    makeToggleImage: function(img,style) {
        style = (style||"");// + HU.css('color','#000');
        return HU.div([ATTR_STYLE,HU.css('display','inline-block',"min-width","10px")], HtmlUtils.getIconImage(img, ["align", "bottom"],[ATTR_STYLE,style]));
    },
    toggleBlockListeners:{},
    toggleBlockVisibility:function(id, imgid, showimg, hideimg,anim,forceVisible) {
	let visible = true;
	if(!Utils.isDefined(visible)) visible = true;
	if (toggleVisibility(id, 'block',anim,forceVisible)) {
            if(HU.isFontAwesome(showimg)) {
		$("#" + imgid).html(HU.makeToggleImage(showimg));
            } else {
		$("#" + imgid).attr('src', showimg);
            }
	} else {
	    visible = false;
            if(HU.isFontAwesome(showimg)) {
		$("#" + imgid).html(HU.makeToggleImage(hideimg));
            } else {
		$("#" + imgid).attr('src', hideimg);
            }
	}
	Utils.ramaddaUpdateMaps();
	if(HtmlUtils.toggleBlockListeners[id]) {
	    HtmlUtils.toggleBlockListeners[id](id,visible);
	}
    },
    toggleBlock: function(label, contents, visible, args,result) {
        let opts = {
            headerClass:'ramadda-noselect entry-toggleblock-label ramadda-hoverable ' + CLASS_CLICKABLE,
            headerStyle:'',
	    orientation:'h',
	    imgopen:'fas fa-caret-down',
	    imgclosed:'fas fa-caret-right',	    
        };

        if(args) $.extend(opts, args);
	let horizontal = opts.orientation=='horizontal';
	if(horizontal)
	    opts.headerStyle+= 'line-height:1px;';
        let id = Utils.getUniqueId('block_');
        let imgid = id + '_img';
	if(result) {
	    result.id = id;
	    result.imgid = imgid;
	}
        let img1 = opts.imgopen;
        let img2 = opts.imgclosed;
        let clickArgs = HtmlUtils.join([HtmlUtils.squote(id), HtmlUtils.squote(imgid), HtmlUtils.squote(img1), HtmlUtils.squote(img2),opts.animated], ',');
        let click = 'HtmlUtils.toggleBlockVisibility(' + clickArgs + ');';
        let img = HU.span([ATTR_ID,imgid], HU.makeToggleImage(visible ? img1 : img2));
        let header = HtmlUtils.div([ATTR_STYLE,opts.headerStyle,ATTR_CLASS, opts.headerClass, 'onClick', click],  img +  ' ' + label);
        let style = (visible ? 'display:block;visibility:visible' : 'display:none;');
        let body = HtmlUtils.div([ATTR_CLASS, 'hideshowblock', ATTR_ID, id, ATTR_STYLE, style],
                                 contents);

	if(opts.listener)
	    HtmlUtils.toggleBlockListeners[id] = opts.listener;


	if(opts.separate)
	    return {header:header,body:body,id:id};
	if(horizontal)
	    return HU.hbox([header,body]);
        return header + body;
    },
    toggleBlockNew: function(label, contents, visible, args) {
	if(!Utils.isDefined(visible))  visible=false;
        let opts = {
            headerClass:"ramadda-noselect ramadda-toggleblock-label " + CLASS_CLICKABLE,
            headerStyle:""
        };
        if(args) $.extend(opts, args);
        let id = Utils.getUniqueId("block_");
        let imgid = id + "_img";
        let img1 = "fas fa-caret-down";
        let img2 = "fas fa-caret-right";        
        let img = HU.span([ATTR_ID,imgid], HU.makeToggleImage(visible ? img1 : img2));
	let attrs = ['toggle-block-id',id,'toggle-block-visible',visible,ATTR_STYLE,opts.headerStyle,ATTR_CLASS, opts.headerClass];
	if(opts.extraAttributes) {
	    attrs = Utils.mergeLists(attrs,opts.extraAttributes);
	}
        let header = HtmlUtils.div(attrs,  img +  " " + label);
        let style = (visible ? "display:block;visibility:visible" : "display:none;");
        let body = HtmlUtils.div([ATTR_CLASS, "hideshowblock", ATTR_ID, id, ATTR_STYLE, style],
                                 contents);
	if(opts.separate)
	    return {header:header,body:body,id:id};
        return header + body;
    },
    initToggleBlock:function(dom,callback) {
	dom.find('[toggle-block-id]').click(function() {
	    let id = $(this).attr('toggle-block-id');
            let imgid = id + "_img";
            let img1 = "fas fa-caret-down";
            let img2 = "fas fa-caret-right";        
	    let visible = $(this).attr('toggle-block-visible')==='true';
	    visible=!visible;
	    $(this).attr('toggle-block-visible',''+visible);
	    $('#'+imgid).html(HU.makeToggleImage(visible?img1:img2));
	    if(visible) $('#'+id).show();
	    else $('#'+id).hide();	    
	    if(callback) callback(id,visible,$(this));
	});
    }
    
}


var StringUtil = {
    endsWith: function(str, suffix) {
        return (str.length >= suffix.length) &&
            (str.lastIndexOf(suffix) + suffix.length == str.length);
    },
    startsWith: function(str, prefix) {
        return (str.length >= prefix.length) &&
            (str.lastIndexOf(prefix, 0) === 0);
    }
}






var blockCnt = 0;

function DomObject(name) {
    this.obj = null;
    // DOM level 1 browsers: IE 5+, NN 6+
    if (document.getElementById) {
        this.obj = document.getElementById(name);
        if (this.obj)
            this.style = this.obj.style;
    }
    // IE 4
    else if (document.all) {
        this.obj = document.all[name];
        if (this.obj)
            this.style = this.obj.style;
    }
    // NN 4
    else if (document.layers) {
        this.obj = document.layers[name];
        this.style = document.layers[name];
    }
    if (this.obj) {
        this.id = this.obj.id;
        if (!this.id) {
            this.id = "obj" + (blockCnt++);
        }
    }
}



function pageIsUnloading() {
    GuiUtils.pageUnloading = true;
    //Gotta do this for IE (uggh)
    window.onbeforeunload = confirmExit;
}

function confirmExit() {
    return null;
}

$.widget("custom.iconselectmenu", $.ui.selectmenu, {
    _renderItem: function(ul, item) {
        let li = $("<li>");
        let wrapper = $("<span>");

        if (item.disabled) {
            li.addClass("ui-state-disabled");
        }

        let label = item.label;
        let img = item.element.attr("img-src");
        let title = item.element.attr(ATTR_TITLE);	
	if(label=='<blank>') label='&lt;blank&gt;';
        if(img) {
            if(img.startsWith("fa")) {
                img = HU.getIconImage(img);
            } else {
                img = HU.image(img, [ATTR_STYLE,item.element.attr("data-style")||"",
				     ATTR_WIDTH,64, ATTR_TITLE,title,
				     ATTR_CLASS, "ui-icon " + item.element.attr("data-class")]);
            }
            $(img).appendTo(wrapper);
        }

	if(!item.element.attr("isheader")) {
            label = HU.span([ATTR_TITLE,label,ATTR_STYLE,
			     HU.css('display','inline-block',ATTR_WIDTH,'100%',
				    'margin-left',img?'32px':'4px','white-space','nowrap')], label);
        } else {
	    wrapper.css('padding-left','0px').css('pointer-events','none');
	    li.css('pointer-events','none');

	}

        let labelClass = item.element.attr("label-class");
        if(labelClass) {
            label = HU.div([ATTR_STYLE,HU.css(ATTR_WIDTH,'100%'), ATTR_CLASS, labelClass],label);
        }

        wrapper.append(label);
        return li.append(wrapper).appendTo(ul);
    }
});



window.onbeforeunload = pageIsUnloading;


function Div(contents, clazz) {
    this.id = HtmlUtils.getUniqueId();
    this.contents = contents || "";
    this.extra = "";
    this.clazz = clazz;
    this.setHidden = function() {
    }
    this.toString = function() {
        return HtmlUtils.div([ATTR_CLASS, clazz || "", ATTR_ID, this.id], this.contents);
    }
    this.getId = function() {
        return this.id;
    }
    this.jq = function() {
        return $("#" + this.id)
    }
    this.set = function(html, attrs) {
        if (attrs) this.extra = HtmlUtils.attrs(attrs);
        this.contents = html;
        this.jq().html(html);
        return this;
    }
    this.append = function(html) {
        if (!this.content) this.content = html;
        else this.content += html;
        $("#" + this.id).append(html);
    }
    this.msg = function(msg) {
        return this.set(HtmlUtils.div([ATTR_CLASS, "display-message"], msg));
    }
}


(function($, undefined) {
    $.fn.getCursorPosition = function() {
        var el = $(this).get(0);
        var pos = 0;
        if ('selectionStart' in el) {
            pos = el.selectionStart;
        } else if ('selection' in document) {
            el.focus();
            var Sel = document.selection.createRange();
            var SelLength = document.selection.createRange().text.length;
            Sel.moveStart('character', -el.value.length);
            pos = Sel.text.length - SelLength;
        }
        return pos;
    }
})(jQuery);




function number_format(number, decimals,debug) {
    let negative = number<0;
    let n = Utils.roundDecimals(number,decimals,false && debug);

    let i;
    if(negative) {
        i = Math.ceil(n);
        i = Math.abs(i);
    }   else {
        i = Math.floor(n);
    }
    let toks = String(n).match(/\.(.*)/);
    let dec =toks?toks[1]:"";
    let s = String(i).replace(/(.)(?=(\d{3})+$)/g,'$1,')
    if(dec!="") {
        s+= "." + dec;
    }
    if(debug) console.log("number:" + number +" n:" + n +" i:" + i +" s:" +s);
    if(negative && i==0) return "-" +s;
    if(negative) return "-"+s;
    return s;
}



//SVG Utils
var SU;
var SvgUtils  = SU = {
    translate: function(x,y) {
        return ' translate(' + x + ',' + y+') ';
    },
    scale: function(s) {
        return ' scale(' + s + ') ';
    },
    rotate: function(s) {
        return ' rotate(' + s + ') ';
    },
    skewX: function(s) {
        return ' skewX(' + s + ') ';
    },   
    transform: function(svg,...args) {
        svg.attr('transform',args.join(" "));
        return svg;
    },
    makeBlur:function(svg,id,blur) {
        var filter = svg.append("defs")
            .append("filter")
            .attr(ATTR_ID, id)
            .append("feGaussianBlur")
            .attr("stdDeviation", blur);
        return filter;
    }
    
}


/*
 * Date Format 1.2.3
 * (c) 2007-2009 Steven Levithan <stevenlevithan.com>
 * MIT license
 *
 * Includes enhancements by Scott Trenda <scott.trenda.net>
 * and Kris Kowal <cixar.com/~kris.kowal/>
 *
 * Accepts a date, a mask, or a date and a mask.
 * Returns a formatted version of the given date.
 * The date defaults to the current date/time.
 * The mask defaults to dateFormat.masks.default.
 */

var dateFormat = function() {
    var token = /d{1,4}|m{1,4}|yy(?:yy)?|([HhMsTt])\1?|[LloSZ]|"[^"]*"|'[^']*'/g,
        timezone = /\b(?:[PMCEA][SDP]T|(?:Pacific|Mountain|Central|Eastern|Atlantic) (?:Standard|Daylight|Prevailing) Time|(?:GMT|UTC)(?:[-+]\d{4})?)\b/g,
        timezoneClip = /[^-+\dA-Z]/g,
        pad = function(val, len) {
            val = String(val);
            len = len || 2;
            while (val.length < len) val = "0" + val;
            return val;
        };

    // Regexes and supporting functions are cached through closure
    return function(date, mask, utc) {

        var dF = dateFormat;

        // You can't provide utc if you skip other args (use the "UTC:" mask prefix)
        if (arguments.length == 1 && Object.prototype.toString.call(date) == "[object String]" && !/\d/.test(date)) {
            mask = date;
            date = undefined;
        }

        // Passing date through Date applies Date.parse, if necessary
        date = date ? new Date(date) : new Date;
        if (isNaN(date)) throw SyntaxError("invalid date");

        mask = String(dF.masks[mask] || mask || dF.masks["default"]);

        // Allow setting the utc argument via the mask
        if (mask.slice(0, 4) == "UTC:") {
            mask = mask.slice(4);
            utc = true;
        }


        var _ = utc ? "getUTC" : "get",
            d = date[_ + "Date"](),
            D = date[_ + "Day"](),
            m = date[_ + "Month"](),
            y = date[_ + "FullYear"](),
            H = date[_ + "Hours"](),
            M = date[_ + "Minutes"](),
            s = date[_ + "Seconds"](),
            L = date[_ + "Milliseconds"](),
            o = utc ? 0 : date.getTimezoneOffset(),
            flags = {
                d: d,
                dd: pad(d),
                ddd: dF.i18n.dayNames[D],
                dddd: dF.i18n.dayNames[D + 7],
                m: m + 1,
                mm: pad(m + 1),
                mmm: dF.i18n.monthNames[m],
                mmmm: dF.i18n.monthNames[m + 12],
                yy: String(y).slice(2),
                yyyy: y,
                h: H % 12 || 12,
                hh: pad(H % 12 || 12),
                H: H,
                HH: pad(H),
                M: M,
                MM: pad(M),
                s: s,
                ss: pad(s),
                l: pad(L, 3),
                L: pad(L > 99 ? Math.round(L / 10) : L),
                t: H < 12 ? "a" : "p",
                tt: H < 12 ? "am" : "pm",
                T: H < 12 ? "A" : "P",
                TT: H < 12 ? "AM" : "PM",
                Z: utc ? "UTC" : (String(date).match(timezone) || [""]).pop().replace(timezoneClip, ""),
                o: (o > 0 ? "-" : "+") + pad(Math.floor(Math.abs(o) / 60) * 100 + Math.abs(o) % 60, 4),
                S: ["th", "st", "nd", "rd"][d % 10 > 3 ? 0 : (d % 100 - d % 10 != 10) * d % 10]
            };

        return mask.replace(token, function($0) {
            return $0 in flags ? flags[$0] : $0.slice(1, $0.length - 1);
        });
    };
}();

// Some common format strings
dateFormat.masks = {
    "default": "ddd mmm dd yyyy HH:MM:ss",
    shortDate: "m/d/yy",
    mediumDate: "mmm d, yyyy",
    longDate: "mmmm d, yyyy",
    fullDate: "dddd, mmmm d, yyyy",
    shortTime: "h:MM TT",
    mediumTime: "h:MM:ss TT",
    longTime: "h:MM:ss TT Z",
    isoDate: "yyyy-mm-dd",
    isoTime: "HH:MM:ss",
    isoDateTime: "yyyy-mm-dd'T'HH:MM:ss",
    isoUtcDateTime: "UTC:yyyy-mm-dd'T'HH:MM:ss'Z'"
};

// Internationalization strings
dateFormat.i18n = {
    dayNames: [
        "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat",
        "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
    ],
    monthNames: [
        "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
        "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"
    ]
};


//convenience method
function jqid(id) {
    return HtmlUtils.jqid(id);
}



// For convenience...
Date.prototype.format = function(mask, utc) {
    return dateFormat(this, mask, utc);
};

$( document ).ready(function() {
    HU.documentReady = true;
    Utils.checkForResize();
});


Utils.areDisplaysReady()






/**
   Copyright (c) 2008-2025 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/



var  displayDebug= {
    getProperty:false,
    loadPointJson:false,
    pointDataLoaded:false,
    notifyEvent:false,
    notifyEventAll:false,
    handleEventPropertyChanged:false,
    getSelectedFields:false,
    filterData:false,
    getStandardData:false,
    makeDataTable:false,
    checkSearchBar:false,
    handleNoData:false,
    displayMapUpdateUI:false,
    displayMapCreateMap:false,
    displayMapAddPoints:false,
    groupBy:false,
    gridPoints:false,
    setEntry:false,
    initMap:false,
    colorTable:false
}


const CATEGORY_CHARTS = "Basic Charts";
const CATEGORY_TABLE = "Tables";
const CATEGORY_MISC = "Misc Charts";
const CATEGORY_MAPS = "Maps";
const CATEGORY_IMAGES = "Images";
const CATEGORY_RADIAL_ETC = "Trees, etc";
const CATEGORY_TEXT = "Text";
const CATEGORY_ENTRIES = "Entries";
const CATEGORY_CONTROLS = "Controls";
const DISPLAY_CATEGORIES = [CATEGORY_CHARTS,CATEGORY_TABLE,CATEGORY_MAPS,CATEGORY_IMAGES,CATEGORY_MISC,CATEGORY_TEXT,CATEGORY_RADIAL_ETC,CATEGORY_CONTROLS,CATEGORY_ENTRIES];



//Ids of DOM components
const ID_BOTTOM = "bottom";
const ID_COLORTABLE = "colortable";
const ID_LEGEND = "legend";
const ID_FIELDS = "fields";
const ID_HEADER = "header";
const ID_HEADER0 = "header0";
const ID_HEADER1 = "header1";
const ID_HEADER2 = "header2";
const ID_HEADER2_PREFIX = "header2prefix";
const ID_HEADER2_PREPREFIX = "header2preprefix";
const ID_HEADER2_PREPREPREFIX = "header2prepreprefix";
const ID_HEADER2_SUFFIX = "header2suffix";
const ID_FILTERBAR = "filterbar";
const ID_TAGBAR = "tagbar";
const ID_TITLE = ATTR_TITLE;
const ID_TITLE_EDIT = "title_edit";
const ID_LEFT = "left";
const ID_RIGHT = "right";
const ID_TITLE_FIELD="titlefield";
const ID_TOP = "top";
const ID_TOP_RIGHT = "topright";
const ID_TOP_LEFT = "topleft";
const ID_DETAILS = "details";
const ID_DETAILS_SNIPPET = "snippet";
const ID_DISPLAY_CONTENTS = "contents";
const ID_DISPLAY_CONTAINER = "container";
const ID_DISPLAY_TOP = "top";
const ID_DISPLAY_BOTTOM = "bottom";
const ID_GROUP_CONTENTS = "group_contents";
const ID_DETAILS_MAIN = "detailsmain";
const ID_GROUPBY_FIELDS= "groupdbyfields";
const ID_TOOLBAR = "toolbar";
const ID_TOOLBAR_INNER = "toolbarinner";
const ID_LIST = "list";
const ID_DISPLAY_MESSAGE = "displaymessage";
const ID_DIALOG = "dialog";
const ID_DIALOG_TABS = "dialog_tabs";
const ID_FOOTER = "footer";
const ID_FOOTER_LEFT = "footer_left";
const ID_FOOTER_RIGHT = "footer_right";
const ID_MENU_BUTTON = "menu_button";
const ID_MENU_OUTER = "menu_outer";
const ID_MENU_INNER = "menu_inner";
const ID_DISPLAY_PROGRESS = "display_progress";
const ID_REPOSITORY = "repository";
const ID_REQUEST_PROPERTIES = "request_properties";
const ID_PAGE_COUNT = "pagecount";
const ID_PAGE_PREV = "pageprev";
const ID_PAGE_NEXT = "pagenext";
const ID_PAGE_LABEL = "pagelabel";
const ID_PAGE_BUTTONS = "pagebuttons";
const ID_FILTER_HIGHLIGHT = "filterhighlight";
const ID_FILTER_DATE = "filterdate";
const ID_FILTER_COUNT = "filtercount";
const ID_ENTRIES_MENU = "entries_menu";
const ID_ENTRIES_PREV = "entries_prev";
const ID_ENTRIES_NEXT = "entries_next";
const ID_NEXT = "next";
const ID_PREV = "prev";
const ID_PREVNEXT_LABEL = "prevnext_label";

const PROP_DISPLAY_FILTER = "displayFilter";
const PROP_EXCLUDE_ZERO = "excludeZero";
const PROP_EXCLUDE_NAN = "excludeUndefined";
const PROP_DIVID = "divid";
const PROP_FIELDS = "fields";
const PROP_LAYOUT_HERE = "layoutHere";
const PROP_HEIGHT = "height";
const PROP_WIDTH = "width";

const RECORD_INDEX = "recordindex";
const RECORD_ID = "recordid";
const TEXT_HIGHLIGHT_COLOR = "yellow";
const HIGHLIGHT_COLOR = "#436EEE";

const VALUE_NONE = "--none--";

var CLASS_HEADER_SPAN = "display-header-span";
var CLASS_HEADER_LABEL = "display-header-label";

const DisplayEvent = {
};

function displayDefineEvent(event,dflt) {
    if(!(dflt===false)) dflt=true;
    DisplayEvent[event] = {
	name:event,
	share: event+".share",
	accept: event+".accept",
	shareGroup: event+".shareGroup",
	acceptGroup: event+".acceptGroup",
	default:dflt,
	handler:"handleEvent" + event[0].toUpperCase() + event.substring(1),
	toString:function() {
	    return this.name;
	}
    }
}


displayDefineEvent("setEntry");
displayDefineEvent("filteredTimes",false);
displayDefineEvent("recordSelection");
displayDefineEvent("dateRange");
displayDefineEvent("recordList");
displayDefineEvent("recordHighlight");
displayDefineEvent("propertyChanged");
displayDefineEvent("pointDataLoaded");
displayDefineEvent("dataSelection");
displayDefineEvent("fieldsSelected");
displayDefineEvent("filterFieldsSelected");
displayDefineEvent("fieldsChanged");
displayDefineEvent("fieldValueSelected");
displayDefineEvent("entrySelection");
displayDefineEvent("entriesChanged");
displayDefineEvent("mapBoundsChanged",false);
displayDefineEvent("animationChanged");
displayDefineEvent("entryMouseOver");
displayDefineEvent("entryMouseOut");
displayDefineEvent("removeDisplay");
displayDefineEvent("filterChanged");
displayDefineEvent("filteredDataChanged",false);


var globalDisplayCount = 0;
var DISPLAY_COUNT=0;
function addGlobalDisplayProperty(name, value,displayType) {
    if (window.globalDisplayProperties == null) {
        window.globalDisplayProperties = {};
    }
    if(value==="true") value = true;
    else if(value==="false") value=false;
    if(displayType) name=displayType+'.' + name;
    window.globalDisplayProperties[name] = value;
}

function getGlobalDisplayProperty(name,displayType) {
    if (window.globalDisplayProperties == null) {
        return null;
    }

    if(displayType) {
	let name2=displayType+'.' + name;
	if(Utils.isDefined(window.globalDisplayProperties[name2])) {
	    return window.globalDisplayProperties[name2];
	}
    }
    return window.globalDisplayProperties[name];
}



function addGlobalDisplayType(type, front) {
    if (window.globalDisplayTypes == null) {
        window.globalDisplayTypes = [];
	window.globalDisplayTypesMap = {};
    }

    if(type.preview && !type.tooltip) {
	type.tooltip= makeDisplayTooltip(type.label,type.preview,type.desc);
    }
    if(type.type) {
	window.globalDisplayTypesMap[type.type] = type;
    }

    if(front) {
	window.globalDisplayTypes.unshift(type);
    } else {
	window.globalDisplayTypes.push(type);
    }
}


var RamaddaDisplayUtils = {
    sparklineProps:  [
	{label:'Sparkline'},
	{p:'showDate',ex:'true'},
	{p:'showMin',ex:'true'},
	{p:'showMax',ex:'true'},
	{p:'sparklineUseAllRecords',ex:'true',tt:'Use all of the records for the min/max.\nUseful for multiples'},	
	{p:'labelStyle',ex:''},			
	{p:'sparklineWidth',d:60, canCache:true},
	{p:'sparklineHeight',d:20, canCache:true},
	{p:'sparklineLineColor',d:'#000', canCache:true},
	{p:'sparklineBarColor',d:'MediumSeaGreen', canCache:true},
	{p:'sparklineCircleColor',d:'#000', canCache:true},
	{p:'sparklineCircleRadius',d:'1', canCache:true},
	{p:'sparklineLineWidth',d:'1', canCache:true},
	{p:'sparklineShowLines',d:true, canCache:true},
	{p:'sparklineShowBars',d:false, canCache:true},
	{p:'sparklineShowCircles',d:true, canCache:true},
	{p:'sparklineShowEndPoints',d:true, canCache:true},
	{p:'sparklineEndPointRadius',d:2, canCache:true},
	{p:'sparklineEndPoint1Color',d:'steelblue', canCache:true},
	{p:'sparklineEndPointRadius',d:'2', canCache:true},
	{p:'sparklineEndPoint2Color',d:'', canCache:true},
	{p:'sparklineEndPoint2Color',d:'tomato', canCache:true},
	{p:'sparklineDoTooltip',d:true, canCache:true},
    ],




    getCanvasProps: function() {
	return [
	    {p:'canvasWidth',d:100,ex:"100",tt:'Canvas width'},
	    {p:'canvasHeight',d:100,ex:"100",tt:'Canvas height'},
	    {p:'canvasOrigin',d:"sw",ex:"center",tt:'Origin point for drawing glyphs'},
	    {label:'label glyph',p:"glyph1",ex:'type:label,pos:sw,dx:10,dy:-10,label:field_colon_ ${field}_nl_field2_colon_ ${field2}'},
	    {label:'rect glyph', p:"glyph1",ex:'type:rect,pos:sw,dx:10,dy:0,colorBy:field,width:150,height:100'},
	    {label:'circle glyph',p:"glyph1",ex:'type:circle,pos:n,dx:10,dy:-10,fill:true,colorBy:field,width:20,baseWidth:5,sizeBy:field,#sizeByMin:0,#sizeByMax:100'},
	    {label:'3dbar glyph', p:"glyph1",ex:'type:3dbar,pos:sw,dx:10,dy:-10,height:30,width:8,baseHeight:5,sizeBy:field,#sizeByMin:0,#sizeByMax:100'},
	    {label:'gauge glyph',p:"glyph1",ex:'type:gauge,color:#000,pos:sw,width:50,height:50,dx:10,dy:-10,sizeBy:field,sizeByMin:0'}
	];
    },
    getGlyphs:function(display,fields,records, canvasWidth, canvasHeight) {
	let glyphs=[];
	let cnt = 1;
	if(!Utils.isDefined(canvasWidth)) {
	    canvasWidth = display.getProperty("canvasWidth",100);
	    canvasHeight = display.getProperty("canvasHeight",100);	    
	}
	while(cnt<11) {
	    let attr = display.getProperty("glyph" + (cnt++));
	    if(!attr)
		continue;
	    glyphs.push(new Glyph(display,1.0, fields,records,{
		canvasWidth:canvasWidth,
		canvasHeight: canvasHeight
	    },attr));
	}
	return glyphs;
    }

}



function makeDisplayTooltip(header,imgs,text) {
    let h =  "";
    if(header!=null) h +=HU.b(header);
    if(imgs) {
        if(!Array.isArray(imgs)) {
	    imgs  = [imgs];
	}
	let imgHtml = imgs.reduce((acc,img)=>{
	    if(!img.startsWith("/")) {
		img = RamaddaUtil.getUrl("/help/display/" + img);
	    }
	    return acc+"<img src="+ img +" width=250px><br>";
	},'');
//	if(h!="") h+="<br>";
	h+=HU.div([],imgHtml);
    }
    if(Utils.stringDefined(text)) h+=HU.div([],text);
    h  = h.replace(/"/g,"&quot;");
    return h;
}



let addDisplayListener = null;
function addRamaddaDisplay(display) {
    if(addDisplayListener) addDisplayListener(display);
    Utils.addDisplay(display);
    display.displayCount=globalDisplayCount++;
    return display;
}

async function ramaddaDisplaySetSelectedEntry(entryId, displays,except) {
    await getGlobalRamadda().getEntry(entryId, e => {
	displays = displays||Utils.displaysList;
	if(displays) {
		displays.forEach(d=>{
		    if(d==except) return
		    if(d.setEntry) d.setEntry(e);
		});
	}
    });
}


function ramaddaDisplayCheckLayout() {
    Utils.displaysList.forEach(d=>{
        if (d.checkLayout) {
	    let t1= new Date();
//	    console.log("before:" + d.type);
            d.checkLayout();
	    let t2= new Date();
//	    Utils.displayTimes("after:" + d.type,[t1,t2],true);
        }
    });
}


function getRamaddaDisplay(id) {
    let display =  Utils.displaysMap[id];
    if(display) return display;
    //Lazily set up the display map as when they are first created they don't have their getId() function defined yet
    Utils.displaysList.forEach(display=>{
	if(display.getId) {
	    Utils.displaysMap[display.getId()] = display;
	}
	if (display.displayId) {
            Utils.displaysMap[display.displayId] = display;
	}
    });
    return Utils.displaysMap[id];
}

function removeRamaddaDisplay(id) {
    let display = getRamaddaDisplay(id);
    if (display) {
        display.removeDisplay();
	Utils.removeDisplay(display);
    }
}

function displayGetFunctionValue(v) {
    if(v.getTime) {
	return v.getTime();
    }
    if(isNaN(v)) {
	if((typeof v) == "string")return v;
	return 0;
    }
    return v;
}

function ramaddaDisplayStepAnimation() {
    Utils.displaysList.forEach(d=>{
	if(d.getProperty && d.getAnimation)  {
	    if(d.getProperty("doAnimation")) {
		d.getAnimation().doNext();
	    }
	}
    });
}


function displayDefineMembers(display, props, members) {
    RamaddaUtil.defineMembers(display, members);
    if(props && display.defineProperties) display.defineProperties(props);
    return display;
}


function defineDisplay(display, SUPER, props, members) {
    RamaddaUtil.inherit(display, SUPER);
    displayDefineMembers(display, props, members);
    if(members.ctor) {
	display.ctor();
    }
    return display;
}



addGlobalDisplayType({
    type: "group",
    label: "Group",
    requiresData: false,
    forUser: true,
    category: "Basic Charts",
    tooltip: makeDisplayTooltip("Group",null,"This allows you to layout displays and share common attributes"),
    helpUrl:true

},true);


/**
   Base class for all displays oriented things
*/
function DisplayThing(argId, argProperties) {
    this.isDisplayThing = true;

    if (argProperties == null) {
        argProperties = {};
    }


    //check for booleans as strings
    for (let i in argProperties) {
        if (typeof argProperties[i] == "string") {
            if (argProperties[i] == "true") argProperties[i] = true;
            else if (argProperties[i] == "false") argProperties[i] = false;
        }
    }


    //Now look for the structured foo.bar=value
    for (let key in argProperties) {
        let toks = key.split(".");
        if (toks.length <= 1) {
            continue;
        }
	//let map = argProperties;
	// Don't this for now as it screws up doing something like colorTable.field=...
	let map = {};
        //graph.axis.foo=bar
        let v = argProperties[key];
        if (v == "true") v = true;
        else if (v == "false") v = false;
        for (let i = 0; i < toks.length; i++) {
            let tok = toks[i];
            if (i == toks.length - 1) {
                map[tok] = v;
                break;
            }
            let nextMap = map[tok];
            if (nextMap == null) {
                map[tok] = {};
                map = map[tok];
            } else {
                map = nextMap;
            }
        }
    }

    this.ignoreGlobals = argProperties.ignoreGlobals;

    this.displayId = null;

    displayDefineMembers(this,null, {
        objectId: argId,
        properties: argProperties,
        displayParent: null,
        getId: function() {
            return this.objectId;
        },
        setId: function(id) {
            this.objectId = id;
        },
        removeDisplay: function() {
	    if(this.dialogElement)  this.dialogElement.remove();
        },
	setEntry: function(entry) {
	},
	handleEntryMenu: async function(entryId) {
            await getGlobalRamadda().getEntry(entryId, e => {
		this.setEntry(e);
	    });

	},
	getEntriesMenu: function(argProperties) {
	    if(argProperties && argProperties.entryCollection) {
		let entries  = argProperties.entryCollection.split(",");
		this.changeEntries = [];
		let enums = [];
		entries.forEach(t=>{
		    let toks = t.split(":");
		    this.changeEntries.push(toks[0]);
		    enums.push([toks[0],toks[1]]);
		});
		let noun = this.getProperty("noun", "Data");
		let prev = HU.span([CLASS,"display-changeentries-button", TITLE,"Previous " +noun, ID, this.getDomId(ID_ENTRIES_PREV), TITLE,"Previous"], HU.getIconImage("fa-chevron-left"));
 		let next = HU.span([CLASS, "display-changeentries-button", TITLE,"Next " + noun, ID, this.getDomId(ID_ENTRIES_NEXT), TITLE,"Next"], HU.getIconImage("fa-chevron-right")); 
		let label = argProperties.changeEntriesLabel||"Select " + noun;
		if(label!="") label = label+"<br>";

		return  HU.center(HU.div([CLASS,"display-filter"], label + prev +" " + HU.select("",[ATTR_ID, this.getDomId(ID_ENTRIES_MENU)],enums) +" " + next));
	    }
	    return "";
	},
        initializeEntriesMenu: function() {
	    this.jq(ID_ENTRIES_PREV).click(e=>{
		let index = this.jq(ID_ENTRIES_MENU)[0].selectedIndex;
		if(index<=0) {
		    index = this.changeEntries.length;
		}
		let entry  =this.changeEntries[index-1];
		this.jq(ID_ENTRIES_MENU).val(entry);
		this.handleEntryMenu(entry);
	    });
	    this.jq(ID_ENTRIES_NEXT).click(e=>{
		let index = this.jq(ID_ENTRIES_MENU)[0].selectedIndex;
		if(index>=this.changeEntries.length-1) {
		    index = 0;
		}
		let entry  =this.changeEntries[index+1];
		this.jq(ID_ENTRIES_MENU).val(entry);
		this.handleEntryMenu(entry);
	    });
	    this.jq(ID_ENTRIES_MENU).change(e=>{
		let entry = this.jq(ID_ENTRIES_MENU).val();
		this.handleEntryMenu(entry);
	    });
	},


        popup: function(srcId, popupId, srcObj, popup) {
            popup = popup || $("#"+popupId);
            let src = srcObj || $("#"+srcId);
            let myalign = 'left top';
            let atalign = 'left bottom';
            popup.show();
	    //	    console.log(srcObj +" " + srcId + " " + "pop:" + popup.length +" src:" + src.length);
            popup.position({
                of: src,
                my: myalign,
                at: atalign,
                collision: "none none"
            });
            //Do it again to fix a bug on safari
            popup.position({
                of: src,
                my: myalign,
                at: atalign,
                collision: "none none"
            });
            popup.draggable();
            popup.show();	    
        },

	initDialog: function() {
	},
        showDialog: function(text, from, initDialog) {
	    if(!this.dialogElement) {
		$(document.body).append(HU.div([ATTR_CLASS, "display-dialog",ID,this.getDomId(ID_DIALOG)]));
		this.dialogElement = this.jq(ID_DIALOG);
	    }
	    this.dialogElement.html(this.makeDialog(text));
            this.popup(from || this.getDomId(ID_MENU_BUTTON), null,null, this.dialogElement);
	    if(initDialog) initDialog();
            else this.initDialog();
        },
        getShowMenu: function() {
	    if(this.getProperty('isContained',false)) {
		return false;
	    }
            if (Utils.isDefined(this.showMenu)) {
		return this.showMenu;
	    }
	    let dflt = false;
            if (this.displayParent != null) {
		dflt = this.displayParent.getProperty("showChildMenu",dflt);
	    }
	    let v = this.getProperty(PROP_SHOW_MENU, dflt);
	    return v;
        },
	canEdit:function() {
	    return this.getProperty("canEdit");
	},
        getShowTitle: function() {
            if (this.getProperty("showTitle")) {
		return this.getProperty("showTitle");
	    }
	    let dflt = false;
            if (this.displayParent != null) {
		dflt = this.displayParent.getShowChildTitle(dflt);
	    }
	    return this.getProperty("showTitle", dflt);
        },
        formatDate: function(date, args, useToStringIfNeeded) {
	    if(!date || !date.getTime) return "";
            try {
                return this.formatDateInner(date, args, useToStringIfNeeded);
            } catch (e) {
		console.log(e.stack);
                console.error("Error formatting date:\"" + date +"\" error:" +e);
                if (!date.getTime && date.v) date = date.v;
                return "" + date;
            }
        },
	dateProps:null,
	getDateProps:function() {
	    if(!this.dateProps) {
		this.dateProps = {
		    dateFormat:this.getProperty("dateFormat", this.getProperty("dateFormat2")),
		    dateSuffix:this.getProperty("dateSuffix"),
		    timeZone:this.getTimeZone(),
		    dateFormatDaysAgo:this.getProperty("dateFormatDaysAgo",false)
		}
	    }
	    return this.dateProps;
	},
        formatDateInner: function(date, args,useToStringIfNeeded) {
	    let info = this.getDateProps();
	    if(!info.dateFormat && useToStringIfNeeded) {
		return String(date);
	    }
            //Check for date object from charts
            if (!date.getTime && date.v) date = date.v;
	    if(date.getTime && isNaN(date.getTime())) return "Invalid date";
	    if(info.dateFormat) {
		let dttm = Utils.formatDateWithFormat(date,info.dateFormat,true);
		if(dttm) {
		    return String(dttm);
		}
	    }
            if (!date.toLocaleDateString) {
                return String(date);
            }
            let suffix;
            if (args && !Utils.isDefined(args.suffix))
                suffix = args.suffix;
            else
                suffix = info.dateSuffix;
            let timeZone = this.getTimeZone();
            if (!suffix && timeZone) suffix = timeZone;
	    return Utils.formatDate(date, args?args.options:null, {
                timeZone: timeZone,
                suffix: suffix
	    });
        },
        getUniqueId: function(base) {
            return HU.getUniqueId(base);
        },
        toString: function() {
            return "DisplayThing:" + this.getId();
        },
        domId: function(suffix) {
	    return this.getDomId(suffix);
	},
        getDomId: function(suffix) {
            return this.getId() + "_" + suffix;
        },
	gid: function(suffix) {
            return this.getId() + "_" + suffix;
        },
	find: function(selector) {
	    return this.getContents().find(selector);
	},
	getContents: function() {
	    return this.jq(ID_DISPLAY_CONTENTS);
	},
	getContainer: function() {
	    return this.jq(ID_DISPLAY_CONTAINER);
	},		
        jq: function(componentId) {
            return $("#" + this.getDomId(componentId));
        },
	selectboxit: function(selector, args) {
	    let opts = {
		showEffect: "fadeIn",
		showEffectSpeed: 400,
		hideEffect: "fadeOut",
		hideEffectSpeed: 400,
	    };
	    if(args) $.extend(opts,args);
            HtmlUtils.initSelect(selector,opts);
	},
        writeHtml: function(idSuffix, html) {
	    try {
		$("#" + this.getDomId(idSuffix)).html(html);
	    } catch(err) {
		console.log("writeHtml error:" + err);
		console.log("idSuffix:" + idSuffix);
		console.log("html:" + html);
	    }
        },
	defaultTemplateProps:null,
	getDefaultTemplateProps: function() {
	    if(!this.defaultTemplateProps) {
		this.defaultTemplateProps = {
		    iconField: this.getProperty("iconField"),
		    iconSize: parseFloat(this.getProperty("iconSize",16)),
		    colorBy:this.getProperty("colorBy"),
		    colorByMap: this.getColorByMap(),
		    iconMap: this.getIconMap(),
		    imageWidth:this.getProperty("imageWidth")
		}
	    }
	    return this.defaultTemplateProps;
	},
	getTemplateProps: function(fields) {
	    let dflt = this.getDefaultTemplateProps();
	    return {
		iconField: this.getFieldById(fields, dflt.iconField),
		iconSize: dflt.iconSize,
		colorBy:dflt.colorBy,
		colorByMap: dflt.colorByMap,
		iconMap: dflt.iconMap
	    }
	},
	macroHook: function(token,value) {
	    return null;
	},
	fieldFormats:{},
	formatFieldValue:function(f,record,v) {
	    let info = this.fieldFormats[f.getId()];
	    if(!info) {
		info = {
		    template: this.getProperty(f.getId()+".template")
		}
		this.fieldFormats[f.getId()] = info;
	    }
	    if(info.template) {
		let tv = this.applyRecordTemplate(record,this.getDataValues(record),null, info.template);
		tv = tv.replace(/\${value}/g, v);
		v = tv;
	    }
	    return v;
	},

	createCommandText:function(commandText,commandMap) {
	    let cmd='';
	    let macros = Utils.tokenizeMacros(commandText);
	    macros.tokens.forEach(token=>{
		if(token.type=='string') {
		    cmd+=token.s;
		    return;
		}
		commandMap[token.id] = token;
		if(token.tag=='attribute') {
		    if(!token.attrs.labels) {
			console.dir('No labels:',token);
			return;
		    }
		    let labels = token.attrs.labels.split(',');
		    cmd+=HU.select('',['class','display-command','commandId',token.id],labels);
		    return;
		}
		console.dir('Unknown command token',token);
	    });
	    return cmd;
	},
	initCommandText:function(commandMap,div) {
	    let _this = this;
	    div.find('.display-command').change(function() {
		let commandId = $(this).attr('commandId');
		let command = commandMap[commandId];
		if(!command) {
		    console.log('Could not find command:' + commandId);
		    return;
		}
		//		    console.dir(command);
		let index = $(this).prop('selectedIndex');
		Object.keys(command.attrs).forEach(key=>{
		    if(key=='labels') return;
		    let list = command.attrs[key].split(',');
		    let value = list[index];
		    if(!value) {
			console.log('could not find command value in list:', list);
			return
		    }
		    _this.setProperty(key,value);
		});
		_this.updateUI();
	    });
	},

	applyRecordTemplate: function(record, row, fields, template, props,macros, debug) {
	    if(!row) row = this.getDataValues(record);
	    if(!fields) fields = record.getFields();
	    fields = this.getFields(fields);
	    if(!props) {
		props = this.getTemplateProps(fields);
	    }
	    if(!macros)
		macros = Utils.tokenizeMacros(template,{hook:(token,value)=>{return this.macroHook(record, token,value)},
							dateFormat:this.getDateProps().dateFormat});
	    let attrs = {};
	    if(props.iconMap && props.iconField) {
		let value = row[props.iconField.getIndex()];
		let icon = props.iconMap[value];
		if(icon) {
		    attrs[props.iconField.getId() +"_icon"] =  HU.image(icon,["width",props.iconSize]);
		}
	    }

	    let makeImage = (f, value) =>{
		let tokenAttrs  = macros.getAttributes(f.getId()+"_image");
		let imageAttrs = [];
		let width = tokenAttrs?tokenAttrs["width"]:null;
		if(width) {
		    imageAttrs.push("width");
		    imageAttrs.push(width);
		} else if(this.getDefaultTemplateProps().imageWidth) {
		    imageAttrs.push("width");
		    imageAttrs.push(this.getDefaultTemplateProps().imageWidth); 
		} else  {
		    imageAttrs.push("width");
		    imageAttrs.push("300");
		}
		imageAttrs.push("style");
		imageAttrs.push("vertical-align:top");
		return HU.image(value, imageAttrs);
	    };


	    let idToField = {}
	    fields.forEach(f=>idToField[f.getId()] = f);
	    //Look for a list
	    macros.tokens.forEach(t=>{
		if(!t.attrs) return;
		let debug = t.attrs['debug'];
		//check for numeric 
		let field;
		if(t.tag=="#") {
		    if(debug) console.log("checking for numeric");
		    for (let col = 0; col < fields.length; col++) {
			let f = fields[col];
			if(f.isNumeric()) {
			    field=f;
			    if(debug) console.log("found numeric:" + f);
			    break;
			}
		    }
		} else if(t.tag=='match' && (t.attrs['pattern'] || t.attrs['numeric'])) {
		    let pattern = t.attrs['pattern'];
		    let numeric = t.attrs['numeric'];
		    if(debug) console.log("checking for pattern:" + pattern +' or number:'+ numeric);
		    fields.every(f=>{
			if(numeric && f.isNumeric()){
			    field=f;
			    if(debug) console.log("found number:" + f);
			    return false;
			}
			if(pattern) {
			    if(f.getId().indexOf(pattern)>=0 || f.getLabel().indexOf(pattern)>=0 ||
			       f.getId().match(pattern) || f.getLabel().match(pattern)) {
				field =f;
				if(debug) console.log("found pattern:" + f);
				return false;
			    }
			}
			return true;
		    });
		} else if(t.tag) {
		    fields.every(f=>{
			if(f.getId().match(t.tag) || f.getLabel().match(t.tag)) {
			    field =f;
			    if(debug) console.log("found pattern:" + f);
			    return false;
			}
			return true;
		    });
		}
		if(field) {
		    t.tag = field.getId();
		    t.attrs['label'] = field.getLabel();
		    let unit = field.getUnit();
		    if(!unit) unit=props.unit;
		    if(!unit) unit='';
		    if(t.attrs.suffix) {
			t.attrs.suffix = String(t.attrs.suffix).replace('\${unit}', unit).replace('\${fieldName}',field.getLabel()).replace('\${fieldId}',field.getId());
		    }
		    if(t.attrs.prefix) {
			t.attrs.prefix = String(t.attrs.prefix).replace('\${unit}', unit).replace('\${fieldName}',field.getLabel()+": ").replace('\${fieldId}',field.getId()+": ");
		    }		    
		} else {
		    let unit = props.unit??'';
		    if(t.attrs.suffix) {
			t.attrs.suffix = String(t.attrs.suffix).replace('\${unit}', unit).replace('\${fieldName}','').replace('\${fieldId}','');
		    }
		    if(t.attrs.prefix) {
			t.attrs.prefix = String(t.attrs.suffix).replace('\${unit}', unit).replace('\${fieldName}','').replace('\${fieldId}','');
		    }

		}

		if(t.tag=="default") {
		    attrs[t.tag] =  this.getRecordHtml(record, fields, "${default}",t.attrs);
		} else if(t.tag=="entryname") {
		    attrs[t.tag] = props["entryname"];
		} else 	if(t.attrs["type"]=="list" && t.attrs["fields"]) {
		    let html = "<table class=display-table>";
		    t.attrs.fields.split(",").forEach(fieldName=>{
			let f = idToField[fieldName];
			let value = row[f.getIndex()];
			if(f.getType()=="image") {
			    value = makeImage(f,value);
			} else  if(f.getType()=="url") {
			    if(value!="") 
				value =  HU.href(value,value);
			}
			html+="<tr><td align=right><b>" +f.getLabel()+"</b>:</td><td>  " + value+"</td></tr>";
		    });
		    html +="</table>";
		    attrs[t.tag] = html;
		}
	    });


//	    debug = true;
	    for (let col = 0; col < fields.length; col++) {
		let f = fields[col];
		let mattrs  = macros.getAttributes(f.getId());
		if(mattrs && !mattrs['label']) mattrs['label'] = f.getLabel();
		let value = row[f.getIndex()];
		if(f.isNumeric() && isNaN(value) && props.findNonNan && props.records) {
		    for(let i=props.records.length-1;i>=0;i--) {
			value = f.getValue(props.records[i]);
			if(!isNaN(value)) break;
		    }
		}
		if(debug) console.log("macro:" + col +" field:" + f.getId() +" type:" +f.getType() + " value:" + value);
		if(props.iconMap) {
		    let icon = props.iconMap[f.getId()+"."+value];
		    if(icon) {
			s = s.replace("${" + f.getId() +"_icon}", HU.image(icon,["size",props.iconSize]));
		    }
		}
		if(f.getType()=="image") {
		    if(value && value.trim().length>1) {
			let tokenAttrs  = macros.getAttributes(f.getId()+"_image");
			let imageAttrs = [];
			let width = tokenAttrs?tokenAttrs["width"]:null;
			if(width) {
			    imageAttrs.push("width");
			    imageAttrs.push(width);
			} else if(this.getDefaultTemplateProps().imageWidth) {
			    imageAttrs.push("width");
			    imageAttrs.push(this.getDefaultTemplateProps().imageWidth); 
			} else  {
			    imageAttrs.push("width");
			    imageAttrs.push("100%");
			}
			imageAttrs.push("style");
			imageAttrs.push("vertical-align:top");
			let img =  HU.image(value, imageAttrs);

			attrs[f.getId() +"_image"] =  img;
			attrs[f.getId() +"_url"] =  value;
		    } else {
			attrs[f.getId() +"_url"] =  RamaddaUtil.getCdnUrl("/icons/blank.gif");
			attrs[f.getId() +"_image"] =  "";
		    }
		} else if(f.getType()=="movie") {
		    if(value && value.trim().length>0) {
			let movieAttrs = [];
			if(this.getProperty("movieWidth")) {
			    movieAttrs.push("width");
			    movieAttrs.push(this.getProperty("movieWidth"));
			}
			let movie =  HU.movie(value,movieAttrs);
			attrs[f.getId() +"_movie"] =  movie;
			attrs[f.getId() +"_url"] =  value;
		    }
		} else if(f.getType()=="url") {
		    if(value && value.trim().length>1) {
			let tokenAttrs  = macros.getAttributes(f.getId()+"_href");
			let label = tokenAttrs?tokenAttrs["label"]:null;
			attrs[f.getId() +"_href"] =  HU.href(value,label||value);
			attrs[f.getId()]=  value;
		    } else {
			attrs[f.getId() +"_href"] =  "";
			attrs[f.getId()] =  "";
		    }
		    continue;
		} else if(f.isDate) {
		    if(value) {
			attrs[f.getId()]= value;
		    }
		    continue;
		}
		let color;
		if(props.colorByMap) {
		    if(props.colorBy && props.colorBy == f.getId()) {
			color = props.colorByMap[value];
		    } else {
			color = props.colorByMap[f.getId()+"."+value];				    
		    }
		}
		if(color) {
		    attrs[f.getId()+"_color"] =  color;
		}
		attrs[f.getId()]=  value;
		if(f.isNumeric()) {
		    //TODO: nuke this
		    attrs[f.getId() +"_format"] = Utils.formatNumberComma(value);
		}
	    }
	    this.addMacroAttributes(macros,row,attrs);
	    let handler = (tag,value) =>{
		if(tag.attrs["display"] =="tags") {
		    let type = tag.tag;
		    let filter = this.filterMap[type];
		    let color = Utils.getEnumColor(type);
		    let result = "";
		    value = String(value).trim();
		    if(value=="") return "";
		    value.split(",").forEach(tagValue=>{
			result+= HU.div(["metadata-type",type,"metadata-value",tagValue,
					 ATTR_TITLE,tagValue,
					 ATTR_STYLE, HU.css("background", color),
					 ATTR_CLASS,"display-search-tag"],tagValue);
		    });
		    if(filter) result = filter.getLabel()+": " + result+"<br>";
		    return result;
		}
		return "Unknown tag handler:" + tag.attrs["handle"];
	    };
	    attrs.recordIndex = record.rowIndex+1;
	    return macros.apply(attrs,debug,handler);
	},
	addMacroAttributes:function(macros,row,attrs) {
	},
	getFields: function(fields) {
            if (!fields) {
		if(this.convertedFields) {
		    return this.convertedFields;
		} else {
                    let pointData = this.pointData || this.getData();
                    if (pointData == null) {
			return null;
		    }
                    fields = pointData.getRecordFields();
		}
	    }
	    return fields;
	},
	fieldLabelCache:{},
	getFieldLabel:function(field) {
	    if(this.fieldLabelCache[field.getId()])
		return this.fieldLabelCache[field.getId()].value;
	    let value=  this.getProperty(field.getId()+".label",field.getLabel());
	    this.fieldLabelCache[field.getId()] = {value:value};
	    return value;
	},
	getRecordUrlHtml: function(attrs, field, record) {
	    let value = record.getValue(field.getIndex());
	    if(!Utils.stringDefined(value)) return '';
	    let linkLabel = value||"Link";
	    linkLabel = linkLabel.replace(/^https?:\/\//,"");
	    linkLabel = linkLabel.replace(/\?.*/,"");
	    linkLabel = linkLabel.replace(/\/$/,"");	    
	    let label = attrs[field.getId()+".label"] || attrs["url.label"] ||attrs["label"] || linkLabel;
	    return  HU.href(value,label,["target","_link"]);
	},

	getSortedFields: function(fields) {
	    let anyGroups = fields.filter(f=>{
		if(f==null) return true;
		return f.getGroup()!=null;
	    }).length>0;

	    if(!anyGroups) return fields;
	    let groups = [];
	    let map = {};
	    for(let i=0;i<fields.length;i++) {
		let field = fields[i];
		if(field==null) continue;
		group = field.getGroup();
		if(group==null) {
		    group = group+"_"+ i;
		}
		if(!map[group]) {
		    map[group] = [];
		    groups.push(group);
		}
		map[group].push(field);
	    }
	    fields = [];
	    groups.forEach(group=>{
		fields = Utils.mergeLists(fields,map[group]);
	    });
	    return fields;
	},
	dfltRecordHtmlProps:null,
	getRecordHtmlProps:function() {
	    if(!this.dfltRecordHtmlProps) {
		let urlField = this.getFieldById(null, this.getProperty("urlField", "url"),false,true);
		let linkField = this.getFieldById(null,this.getProperty("linkField"))|| urlField;
		this.dfltRecordHtmlProps = {
		    urlField : urlField,
		    linkField : linkField,
		    titleField : this.getFieldById(null,this.getProperty("titleField")),
		    titleTemplate : this.getProperty("titleTemplate"),	    
		    descField : this.getFieldById(null,this.getProperty("descriptionField")),
		    showDate : this.getProperty("showDate", true),
		    showImage : this.getProperty("showImage", true),
		    showMovie : this.getProperty("showMovie", true),	    
		    showElevation : this.getProperty("showElevation",false),
		}
	    }
	    return this.dfltRecordHtmlProps;
	},
        getRecordHtml: function(record, fields, template, props, debug) {
	    props= props??{};
	    if(!props.labelStyle) props.labelStyle=this.getLabelStyle();
	    fields = this.getFields(fields);
	    if(!fields) return "";
	    let dflt = this.getRecordHtmlProps();
	    let link  =  dflt.linkField?record.getValue(dflt.linkField.getIndex()):null;
            let showGeo = this.getTooltipShowGeo();
	    if(template=="") return "";
	    if(!Utils.stringDefined(template))
		template = this.getProperty("recordTemplate");

	    if(Utils.stringDefined(template)) {
		if(!template.startsWith("${default") && template!="${fields}") {
		    return this.applyRecordTemplate(record,this.getDataValues(record), fields, template, null, null,debug);
		}
	    }
	    let ttf = this.getTooltipFields();
	    if(template=="${fields}") {
		fields = this.getFieldsByIds(null,ttf??this.getPropertyFields());
	    } else {
		if(ttf) {
		    fields = this.getFieldsByIds(null,ttf);
		}
	    }

	    let templateProps = {};
	    let itemsPerColumn=this.getItemsPerColumn();
	    let attrs={};
	    if(template) {
		attrs = Utils.tokenizeMacros(template,{hook:(token,value)=>{return this.macroHook(record, token,value)},dateFormat:this.getDateFormat()}).getAttributes("default")||{};
	    }
	    itemsPerColumn = attrs["itemsPerColumn"] || itemsPerColumn;
	    let maxWidth = attrs["maxWidth"]?? attrs["maxwidth"];
	    let values = "";
	    if(dflt.titleField || dflt.titleTemplate) {
		let title="";
		if(dflt.titleTemplate) {
		    let titleTemplate = this.applyTitleTemplate(dflt.titleTemplate);
		    if(!dflt.titleTemplate.startsWith("${default")) {
			title = this.getRecordHtml(record, fields, titleTemplate, {},debug);
		    }

		} else {
		    title = record.getValue(dflt.titleField.getIndex());
		    if(title.getTime)
			title = this.formatDate(title);
		    title = HU.center(HU.h3(title));
		}
		if(link)
		    title = HU.href(link,title,["target","_target"]);
		values+=title;
		link = null;
	    }

	    if(dflt.descField) {
		let desc = record.getValue(dflt.descField.getIndex());
		values+=desc;
	    }

	    let tooltipNots = {};
	    this.getTooltipNotFields().split(",").forEach(f=>{
		tooltipNots[f] = true;
	    });

	    let rows = [];
	    let hadDate = false;
	    let labelColAttrs = [];
	    if(this.getLabelColumnAttrs()) {
		labelColAttrs = this.getLabelColumnAttrs().split(",");
	    } else {
		labelColAttrs = ["align","right"];
	    }
	    let labelWidth = this.getLabelWidth();
	    fields= this.getSortedFields(fields);
	    let excludes = props.excludes?props.excludes.split(","):[];
	    let skipEmpty=props.skipEmpty=='true';
	    let group = null;
	    let includeDesc = this.getIncludeFieldDescriptionInTooltip();
            for (let doDerived = 0; doDerived < 2; doDerived++) {
                for (let i = 0; i < fields.length; i++) {
                    let field = fields[i];
		    let ok = true;
		    excludes.every(ex=>{
			[field.getLabel(), field.getId()].every(v=>{
			    if(v.toLowerCase().match(ex)) ok  = false;
			    return ok;
			});
			return ok;
		    });
		    if(!ok) continue;
		    if(tooltipNots[field.getId()]) continue;
		    if(attrs[field.getId()+".hide"]) {
			continue;
		    }
		    if(field==dflt.titleField || field==dflt.descField) continue;
                    if (doDerived == 0 && !field.derived) continue;
                    else if (doDerived == 1 && field.derived) continue;
                    if (!field.getForDisplay()) {
			continue;
		    }
		    if(field.isRecordDate()) {
			if(!dflt.showDate || hadDate) {
			    continue;
			}
			hadDate = true;
		    }
		    if(field.isFieldDate()) hadDate = true;
                    if (!showGeo) {
                        if (field.isFieldGeo()) {
                            continue;
                        }
                    }
		    if(group!=field.getGroup()) {
			group = field.getGroup();
			if(Utils.isDefined(group)) {
			    rows.push(HU.tr([],HU.td(["colspan","2"],HU.div([ATTR_CLASS,"ramadda-header-small"],group))));
			}
		    }
                    let initValue = record.getValue(field.getIndex());
                    let value = initValue;
                    let svalue = String(initValue);		    
		    if(skipEmpty && !Utils.stringDefined(svalue)) continue;
		    let fieldValue = value;
		    if(fieldValue)
			fieldValue = svalue.replace(/"/g,"'");
                    if (typeof value == "number") {
			value = this.formatNumber(value, field.getId());
		    } 
                    if (field.isFieldDate()) {
			value = this.formatDate(value);
		    }
		    if(field.getType() == "image" && value!="") {
			if(!dflt.showImage) continue;
			let imageAttrs = [];
			if(this.getImageWidth()) {
			    imageAttrs.push("width");
			    imageAttrs.push(this.getImageWidth());
			} else  {
			    imageAttrs.push("width");
			    imageAttrs.push("200");
			}
			imageAttrs.push("align");
			imageAttrs.push("top");
			value = HU.image(value,imageAttrs);
		    }
		    if(field.getType() == "movie" && value!="") {
			if(!dflt.showMovie) continue;
			let movieAttrs = [];
			movieAttrs.push("width");
			movieAttrs.push("200");
			value = HU.movie(value,movieAttrs);
		    }		    
		    if(field.getType() == "url" || svalue.match(/^http[^ ]+$/)) {
			value = this.getRecordUrlHtml(attrs, field, record);
		    }
		    let labelValue = field.getLabel();
		    labelValue = this.getProperty('label.'+field.getId(),labelValue);
		    value = value + field.getUnitSuffix();
		    let tt;
		    if(!includeDesc) {
			tt = field.getDescription();
			if(tt) tt+=HU.getTitleBr();
		    }
		    tt = tt??"";
		    tt+=labelValue+"=" + svalue;
		    tt = tt.replace(/"/g,"'");
		    if(svalue.indexOf("\"")>=0) tt="";
		    if(value.length>100) {
			//Only if its not an image
			if(!String(value).match('<img')) {
			    value  = HU.div([STYLE,HU.css("max-height","100px","overflow-y","auto")],value);
			}
		    }
		    let label = this.formatRecordLabel(labelValue)+":";
		    if(labelWidth) {
			label = HU.div([STYLE,HU.css("max-width" ,HU.getDimension(labelWidth),"overflow-x","auto")], label); 
		    } 
		    label  = HU.div([TITLE,tt],label);
                    let row = HU.open(TR,['valign','top']);
		    let labelAttrs = [CLASS,"display-record-table-label"]
		    if(props.labelStyle) labelAttrs.push(ATTR_STYLE,props.labelStyle);
		    let displayValue = value;
		    let valueStyle = HU.css('margin-left','5px','max-width','90vw');
		    if(maxWidth) {
			valueStyle+=HU.css('max-width',HU.getDimension(maxWidth,'px'));
		    }



		    row += HU.td(labelColAttrs,HU.div(labelAttrs, label));
		    row+='\n';
		    row += HU.td(["field-id",field.getId(),"field-value",fieldValue, "align","left"], HU.div([ATTR_STYLE,valueStyle], displayValue));
		    if(includeDesc) {
			row +=HU.td([],field.getDescription()??"");
		    }
                    row += HU.close('tr');
		    rows.push(row);
                }
            }
	    if(!hadDate && dflt.showDate) {
		if(record.hasDate()) {
                    let row = HU.open(TR,['valign','top']);
		    let label = this.formatRecordLabel("Date");
		    row += HU.td([],HU.b(label+":"));
		    row += HU.td(["align","left"], HU.div([STYLE,HU.css('margin-left','5px')],
							  this.formatDate(record.getDate())));
		    row += HU.close(TR);
		    rows.push(row);
		}
	    }
            if (dflt.showElevation && record.hasElevation()) {
                rows.push(HU.tr([],HU.td([ALIGN,'right'],HU.b('Elevation:')) +
			       HU.td([ALIGN,'left'], number_format(record.getElevation(), 4, '.', ''))));
            }
	    let rowCnt = 0;
	    values += "<table><tr valign=top>";



	    let		lists   = Utils.splitList(rows,itemsPerColumn);
	    let tdStyle =lists.length>1?"margin-right:5px;":"";
	    lists.forEach(list=>{
		values += "<td><div style='" + tdStyle+"'><table>" + Utils.join(list,"") +"</table></div></td>";
	    });
            values += "</tr></table>";
	    if(this.getRecordHtmlStyle()){
		values = HU.div([ATTR_CLASS,"ramadda-shadow-box display-tooltip", ATTR_STYLE,this.getRecordHtmlStyle()], values);
	    }
	    
            return values;
        },
        formatRecordLabel: function(label) {
            label = label.replace(/!!/g, " -- ");
	    label = label.replace(/ /g,"&nbsp;");
            return label;
        },
        getFormValue: function(what, dflt) {
            let fromForm = $("#" + this.getDomId(what)).val();
            if (fromForm != null) {
                if (fromForm.length > 0) {
                    this.setProperty(what, fromForm);
                }
                if (fromForm == "none") {
                    this.setProperty(what, null);
                }
                return fromForm;
            }
            return this.getProperty(what, dflt);
        },

        getName: function() {
            return this.getFormValue("name", this.getId());
        },
        getEventSource: function() {
            return this.getFormValue("eventSource", "");
        },
        setDisplayParent: function(parent) {
            this.displayParent = parent;
        },
        getDisplayParent: function() {
            if (this.displayParent == null) {
                this.displayParent = this.getLayoutManager();
            }
            return this.displayParent;
        },
        removeProperty: function(key) {
            this.properties[key] = null;
        },
        setProperty: function(key, value) {
	    //            this[key] = value;
            this.properties[key] = value;
	    this.transientProperties[key]  = value;
	    this.propertiesCache[key] = value;
        },
        getSelfProperty: function(key, dflt) {
            if (this[key] != null) {
                return this[key];
            }
            return this.getProperty(key, dflt);
        },
        initTooltip: function() {
            //don't do this for now                $( document ).tooltip();
        },
	formatInfo: {},
        formatNumber: function(number, propPrefix,debug) {
	    propPrefix = propPrefix??'';
	    let info = this.formatInfo[propPrefix];
	    if(!info) {
		info = {
		    doFormatNumber:this.getProperty(propPrefix?[propPrefix+".doFormatNumber","doFormatNumber"]:"doFormatNumber",true),
		    fmt:this.getProperty(propPrefix?[propPrefix+".numberTemplate","numberTemplate"]:"numberTemplate"),
		    scale:this.getProperty(propPrefix?[propPrefix+".formatNumberScale","formatNumberScale"]:"formatNumberScale",1),
		    decimals:this.getProperty(propPrefix?[propPrefix+".formatNumberDecimals","formatNumberDecimals"]:"formatNumberDecimals",-1),
		    comma:this.getProperty(propPrefix?[propPrefix+".formatNumberComma","formatNumberComma"]:"formatNumberComma", false),
                    nanValue: this.getNanValue()
		}
		this.formatInfo[propPrefix] = info;
	    }
	    if(!info.doFormatNumber) {
		return number;
	    }
	    if(isNaN(number)) {
                return info.nanValue;
	    }
	    let f = this.formatNumberInner(number, propPrefix,info,debug);
	    if(info.fmt) f = info.fmt.replace("${number}", f);
	    f = String(f);
	    if(f.endsWith(".")) f = f.substring(0,f.length-1);
	    return f;
	},
        formatNumberInner: function(number,propPrefix,info,debug) {
	    number = +number;
	    number = number*info.scale;
            if (info.decimals>=0) {
		return number_format(number, info.decimals);
	    }
            if (info.comma) {
		return   Utils.formatNumberComma(number);
	    }
            return Utils.formatNumber(number,false,debug);
        },
        propertyDefined: function(key) {
            return Utils.isDefined(this.getProperty(key));
        },
        setPropertyOn: function(object, myProperty, objectProperty, dflt) {
            let prop = this.getProperty(myProperty, dflt);
            if (Utils.isDefined(prop) && prop != null) {
                object[objectProperty] = prop;
            }
        },
        getDisplayProp: function(source, prop, dflt) {
            if (Utils.isDefined(this[prop])) {
                return this[prop];
            }
            let prop2 = "map-" + prop;
            if (Utils.isDefined(source[prop2])) {
                return source[prop2];
            }
	    if(source.getProperty) {
		return source.getProperty(prop, dflt);
	    }
	    return null;
        },
        getPropertyFromUrl: function(key, dflt,checkKey) {
	    let fromUrl = HU.getUrlArgument("display"+ this.displayCount+"." + key);
	    if(fromUrl) return fromUrl;
	    if(checkKey) {
		fromUrl = HU.getUrlArgument(key);
		if(fromUrl) return fromUrl;
	    }
	    return this.getProperty(key,dflt);
	},
	getPropertyFields: function(dflt) {
	    return this.getPropertyFromUrl(PROP_FIELDS,dflt);
	},
	transientProperties:{},
	getPropertyCounts:{},
	priorProps:{},
	getAliasForField:function(field) {
	    if(!this.aliasList) {
		this.aliasList  =[];
		Object.keys(this.properties).forEach(key=>{
		    key = String(key);
		    if(key.startsWith("alias.")) {
			let pattern = this.properties[key];
			key = key.substring("alias.".length);
			this.aliasList.push({alias:key,pattern:pattern});
		    }
		});
	    }
	    let list = [];
	    this.aliasList.forEach(alias=>{
		if(field.getId()==alias.pattern || field.getId().match(alias.pattern)) {
		    list.push(alias.alias)
		}
	    });
	    return list;
	},
	getFieldProperty:function(field,prop,dflt){
	    let v =   this.getProperty(field.getId()+'.'+prop);
	    if(Utils.isDefined(v)) return v;
	    //check for aliases properties
	    let aliases = this.getAliasForField(field);
	    aliases.every(alias=>{
		v =   this.getProperty(alias+'.'+prop);
		if(Utils.isDefined(v)) return false;		
		return true;
	    });
	    if(Utils.isDefined(v)) return v;
	    return this.getProperty(prop,dflt);
	},

	getTooltip:function(dflt) {
	    let tooltip = this.getProperty('tooltip',dflt);
	    if(tooltip=='none') tooltip=null;
	    return tooltip;
	},
        getProperty: function(key, dflt, skipThis, skipParent) {
	    let debug = displayDebug.getProperty;
	    if(!this.getPropertyCounts[key]) {
		this.getPropertyCounts[key]=0;
	    }
/*
  Don't try to optimize. It causes problems
	    if(typeof this.transientProperties[key]!='undefined') {
		if(debug) {
		    console.log("getProperty:" + key +"  dflt:"+ dflt +" transient:" + this.transientProperties[key]);
		}
		return   this.transientProperties[key];
	    }
*/

	    debug|=this.debugGetProperty;
	    this.getPropertyCount++;
	    this.getPropertyCounts[key]++;
//	    debug = this.getPropertyCounts[key]==1;
//	    if(debug)
//		console.log("getProperty:" + key +"  dflt:"+ dflt);
	    let value =  this.getPropertyInner(key,null,skipThis, skipParent,this);
	    if(debug) 
		console.log("getProperty:" + key +"  dflt:"+ dflt +" got:" + value);
	    if(this.writePropertyDef!=null) {
		if(!this.seenWriteProperty) this.seenWriteProperty = {};
		if(!this.seenWriteProperty[key]) {
		    let f = (v)=>{
			return v?"'" + v+"'":"null";
		    };
		    this.writePropertyDef+="{p:'" + key +"',d:" + f(dflt)+",wikiValue:" + f(value||dflt)+"},\n"
		    this.seenWriteProperty[key] = true;
		}
	    }
	    if(!Utils.isDefined(value)) {
//		if(debug)  console.log("\treturning dflt:" + dflt);
		value= dflt;
	    }
	    if(this.getPropertyCounts[key]>10) {
		//If we keep calling getProperty then set the transient property so on the next call we don't take the full hit
		this.transientProperties[key]  = Utils.isDefined(value)?value:null;
	    }
//	    if(debug)console.log("\treturning value:" + value);
//	    this.priorProps[key] = value;
	    return value;
	},
        getPropertyInner: function(keys, dflt,skipThis, skipParent,srcDisplay) {	    
	    let debug = displayDebug.getProperty;
	    debug = this.debugGetProperty;
	    srcDisplay = srcDisplay??this;
	    if(!Array.isArray(keys)) keys = [keys];
//	    debug = keys.includes('iconSize');

	    for(let i=0;i<keys.length;i++) {
		let key = keys[i];
//		if(key == "colorTable") debug = true;
		if(debug) console.log("getProperty:" + key +" dflt:" + dflt);
		if(this.dynamicProperties) {
		    if(debug)
			console.log("key:" + key +" value:" +this.dynamicProperties[key]);
		    if(Utils.isDefined(this.dynamicProperties[key])) {
			return this.dynamicProperties[key];
		    }
		}
		let value = this.properties[key];
		if (value != null) {
		    if(debug) console.log("\tgot property from this.properties:" + value);
                    return value;
		}
	    }
	    if(!skipParent) {
		for(let i=0;i<keys.length;i++) {
		    let key = keys[i];
		    let fromParent=null;
		    if (this.displayParent != null) {
			fromParent =  this.displayParent.getPropertyInner("inherit."+key, dflt,skipThis,null, srcDisplay);
			if(debug) console.log("\tgetProperty from display parent:" + fromParent);
		    }
		    if (!fromParent && this.getDisplayManager) {
			fromParent=  this.getDisplayManager().getPropertyInner("inherit."+key);
			if(debug) console.log("\tgetProperty from display manager:" + fromParent);
		    }
		    if(fromParent) {
			if(debug) console.log("\tgetProperty from parent:" + fromParent);
			return fromParent;
		    }
		}
	    }
	    if(!this.ignoreGlobals) {
		if(!skipParent) {
		    if (this.displayParent != null) {
			if(debug) console.log("\tgetProperty calling parent");
			return this.displayParent.getPropertyInner(keys, dflt, skipThis,null,srcDisplay);
		    }
		    if (this.getDisplayManager) {
			if(debug) console.log("\tgetProperty-5");
			return   this.getDisplayManager().getPropertyInner(keys,null,null,null,srcDisplay);
		    }
		}
		for(let i=0;i<keys.length;i++) {
		    let key = keys[i];
		    value = getGlobalDisplayProperty(key,srcDisplay?srcDisplay.type:this.type);
		    if (Utils.isDefined(value)) {
			if(debug) console.log("\tgetProperty-6:" + value);
			return value;
		    }
		}
	    }
	    if(debug) console.log("\tgetProperty-6 dflt:" + dflt);
            return dflt;
        },
    });
}




/**
   Base class for all displays 
*/
function RamaddaDisplay(argDisplayManager, argId, argType, argProperties) {

    const SUPER  = new DisplayThing(argId, argProperties);
    RamaddaUtil.inherit(this, SUPER);


    if(window.globalDisplayTypesMap) {
	this.typeDef = window.globalDisplayTypesMap[argType];
    }

    this._wikiTags  = [];
    
    this.propertiesCache = {};
    this.defineProperties = function(props) {
	let tagList = [];
	props.forEach(prop=>{
	    tagList.push(prop);
	    if(!prop.p) {
		return;
	    }
	    if(prop.p.indexOf("&")<0) {
		if(!Utils.isDefined(prop.doGetter) || prop.doGetter) {
		    let getFunc = (dflt,debug)=>{
			let checkCache = prop.canCache && !dflt;
			if(checkCache) {
			    if(this.propertiesCache[prop.p])
				return this.propertiesCache[prop.p].value; 
			}
			if(!Utils.isDefined(dflt)) dflt = prop.d;
			let value =  this.getProperty(prop.p,dflt);
			if(checkCache) {
			    this.propertiesCache[prop.p] = {value:value}; 
			}
			return value;
		    };
		    let funcName =  'getProperty' + prop.p.substring(0, 1).toUpperCase() + prop.p.substring(1);
		    if(!this[funcName])
			this[funcName] = getFunc;
		    funcName =  'get' + prop.p.substring(0, 1).toUpperCase() + prop.p.substring(1);
		    if(!this[funcName])
			this[funcName] = getFunc;
		}
	    }
	    prop.wikiValue = prop.wikiValue||prop.w;
	});
	this._wikiTags  = Utils.mergeLists(tagList,this._wikiTags);
    }

    let myProps = [
	{label:'Display'},
	{p:'fields',doGetter:false,
	 ex:'comma separated list of field ids or indices - e.g. #1,#4-#7,@2,@3 (use @ for numeric fields) or *'},
	{p:'notFields',ex:'regexp',tt:'regexp to not include fields'},		
	{p:'fieldsPatterns',ex:'comma separated list of regexp patterns to match on fields to display'},
	{p:'fieldAliases',canCache:true},
	{p:'prefixFields',tt:'Field to always add to the beginning of the list'},
	{p:'showMenu',ex:true},	      
	{p:'showMenuRight',ex:true},	      
	{p:'showTitle',ex:true},
	{p:'showChildTitle',canCache:true},
	{p:'showEntryIcon',ex:true},
	{p:'layoutHere',ex:true},
	{p:'headerDiv',tt:'div id of an alternate place to display the header'},
	{p:'footerDiv',tt:'div id of an alternate place to display the footer'},		
	{p:'width',doGetter:false,ex:'100%'},
	{p:'height',doGetter:false,ex:'400px'},
	{p:'noWrapper',ex:true,tt:'Don\'t make the header and footer. Just this core display'},
	{p:'imageWidth',canCache:true},		
	{p:'includeFieldDescriptionInTooltip',canCache:true,d:true},
	{p:'recordTemplate',doGetter:false,ex:'${default}',tt:'Template for popups etc. Can be ${default attrs} or \'${field} .. ${fieldn}...\''},
	{p:'recordHtmlStyle',canCache:true},
	{p:'labelStyle',ex:''},			
	{p:'title',ex:''},
	{p:'titleTemplate',ex:'${title} - ${field1} Date range: ${recordDate_first format=yyyymmdd} - ${recordDate_last format=yyyymmdd}',
	 tt:'Template for title. Use ${title} for the default title. Use ${field} for field values of first record. Use ${field_first} for first record value. Use ${field_ast} for last record value '},	
	{p:'itemsPerColumn',canCache:true,d:50,tt:'How many items to show in each column in a tooltip'},
	{p:'labelColumnAttrs',canCache:true,ex:'align,right',tt:'Attributes of the label column in the record templates'},
	{p:'labelWidth',canCache:true,ex:'10',tt:'Width of labels the record templates'},	
	{p:'displayStyle',ex:'css styles',tt:'Specify styles for display'},
	{p:'primaryPage',ex:'true',tt:'Set to true if you only want this display to show in the  primary for the entry '},
	{p:'titleBackground',ex:'color'},
	{p:'linkField',ex:''},
	{p:'titleField',ex:''},
	{p:'descriptionField',ex:''},
	{p:'textColor',ex:'color'},
	{p:'backgroundImage',ex:'',tt:'Image url to display in background'},
	{p:'background',ex:'color'},
	{p:'showProgress',ex:true},
	{p:'loadingMessage',ex:'',tt:'Message to show when loading data'},	
	{p:'inlineDataSrc',tt:'div id that holds the CSV inline'},
	{p:'showRecordPager',ex:true,tt:'Show the prev/next pager'},
	{p:'recordPagerNumber',d:100,tt:'How many records to show'},	
	{p:'noun',ex:'images'},
	{p:'sortFields',tt:'Comma separated list of fields to sort the data on'},
	{p:'sortAscending',ex:'true|false',d:true},
	{p:'showSortDirection',ex:true},		
	{p:'sortOnDate',ex:'true'},
	{p:'sortByFields',ex:'',tt:'Show sort by fields in a menu'},
	{p:'sortHighlight',ex:true,tt:'Sort based on highlight from the filters'},
	{p:'reverse',ex:'true',t:'Reverse the records'},
	{p:'selectUniqueFields',ex:'',tt:'Show list of fields to make data unique'},
	{p:'doEntries',ex:true,tt:'Make the children entries be data'},
	{p:'propagateDataReload',ex:'true',tt:'Propagate to other displays when the data is reloaded'},
	{p:'propagateFilteredTimes',ex:'true',tt:'Propagate to other displays the list of times when we have filtered data. The other displays need to have filteredTimes.accept=true '},
	{p:'addAttributes',ex:true,tt:'Include the extra attributes of the children'},
	{p:'orderby',ex:'date|fromdate|todate|name|number',tt:'When showing entries as data how to sort or order the entries'},
	{p:'ascending',ex:'true',tt:'When showing entries as data how to sort or order the entries'},		

	{p:'showDisplayFieldsMenu',ex:true},
	{p:'displayFieldsMenuMultiple',ex:true},
	{p:'displayFieldsMenuSide',ex:'left'},
	{p:'displayHeaderSide',ex:'left'},
	{p:'leftSideWidth',ex:'150px'},		
	{label:'Tooltips'},
	{p:'tooltip',doGetter:false,ex:'\"${default}\"'},
	{p:'tooltipDelay',d:1000},
	{p:'tooltipEffect',d:'fadeIn'},
	{p:'tooltipDuration',d:500},	
	{p:'tooltipImmediate',d:false,ex:'true',tt:'Show tooltip immediately'},	
	{p:'tooltipPositionMy',ex:'left top'},
	{p:'tooltipPositionAt',ex:'left bottom+2'},		
	{p:'tooltipCollision'},
	{p:'tooltipFields',canCache:true},
	{p:'tooltipNotFields',d:''},
	{p:'tooltipShowGeo',tt:'show the record lat/lon in the tooltip',ex:'true'},
	{p:'selectPopup',ex:'${default}',tt:'Template to use to make a popup when a record is selected'},
	{p:'selectPopupTitle'},
	{p:'headerText',ex:'blah blah ${command labels=\"log scale,linear scale\" xAxisType=log,linear} blah',
	 tt:'Text to show above the display. Can contain ${command ...} templates'},
	{p:'headerTextDiv',tt:'divid to put header text in'},



	{label:'Formatting'},
	{p:'dateFormat',canCache:true,
	 ex:'yyyy|yyyymmdd|yyyymmddhh|yyyymmddhhmm|yyyymm|yearmonth|monthdayyear|monthday|mon_day|mdy|hhmm'},
	{p:'dateFormatDaysAgo',ex:true},
	{p:'timeZone',canCache:true},
	{p:'timeZoneOffset',canCache:true,d:0},
	{p:'doFormatNumber',ex:false},
 	{p:'formatNumberDecimals',ex:0},
	{p:'formatNumberScale',ex:100},
	{p:'numberTemplate',ex:'${number}%'},
	{p:'nanValue',d:'--',canCache:true},
	{p:'&lt;field_id&gt;.&lt;format&gt;',ex:'...'},
	{label:'Data Requests'},
	{p:'remote',tt:'URL to an external RAMADDA entry',ex:''},
	{p:'request.startdate',tt:'Start date of data',ex:'yyyy-MM-dd or relative:-1 week|-6 months|-2 years|etc'},
	{p:'request.enddate',tt:'End date of data',ex:'yyyy-MM-dd or relative:-1 week|-6 months|-2 years|etc'},
	{p:'requestFields',tt:'Comma separated list of fields for querying server side data'},
	{p:'requestFieldsShow',d:true,ex:'false',tt:'Show the request fields'},
	{p:'requestFieldsDefault',d:true,tt:'Use the default date,stride,skip,limit fields'},
	{p:'requestPrefix',ex:'search.', tt:'Prefix to prepend to the url argument'},
	{p:'requestFieldsLive',d:true,tt:'Is the request applied when a widget changes'},
	{p:'requestFieldsToggle',d:false,tt:'Put the request fields in a toggle'},
	{p:'requestFieldsToggleOpen',d:true,tt:'And leave the toggle open'},

	{p:'request.&lt;request field&gt;.multiple',ex:'true',tt:'Support multiple enumerated selections'},
	{p:'request.&lt;field&gt;.type',tt:'date,number,enumeration'},
	{p:'request.&lt;field&gt;.type',tt:'date,number,enumeration'},			
	{p:'request.&lt;field&gt;.values',tt:'Comma separated list of enum values',
	 d:'0:None,1,2,3,4,5,6,7,8,9,10,15,20,30,40,50,75,100'},
	{p:'request.&lt;field&gt;.visible',d:true},
	{p:'request.&lt;field&gt;.default',tt:'Default value'},
	{p:'request.&lt;field&gt;_from.default',tt:'Default date from'},
	{p:'request.&lt;field&gt;_to.default',tt:'Default date to'},		
	{p:'request.&lt;field&gt;.multiple',d:false,tt:'For enums show multiples'},
	{p:'request.&lt;field&gt;.rows',d:4,tt:'For multiple enums how may rows to show'},
	{p:'request.&lt;field&gt;.includeNone',d:true,tt:'For enums include the none value'},		
	{p:'request.&lt;field&gt;.includeAll',d:true,tt:'For enums include the all value'},		
	{p:'request.&lt;field&gt;.triggerReload',d:true, tt:''},			
	{p:'request.&lt;field&gt;.title',tt:'Tooltip'},
	{p:'request.&lt;field&gt;.urlarg',t:'url arg to use'},	

	{label:'Filter Data'},
	{p:'headerLabel',ex:''},
	{p:'max',ex:'1000',tt:'Specify the max number of records to fetch from the server'},
	{p:'lastRecords',ex:'1',tt:'Only get the last N records from the server'},	
	{p:'maxRecords',tt:'only use this number of records'},
	{p:'fieldsNumeric',ex:true,tt:'Only get numeric fields'},
	{p:'filterFields',ex:''},
	{p:'filterFieldsToPropagate'},
	{p:'filterSetValueFieldId',tt:'when a record is selected in another display this is the field to set the filter value on the target display'},
	{p:'hideFilterWidget',ex:true},
	{p:'filterHighlight',d:false,ex:true,tt:'Highlight the records'},
	{p:'isMasterFilter',ex:true,tt:'Does this display provide filters for all the other displays'},
        {p:'showFilterTags',d: false,canCache:true},
        {p:'tagDiv',tt:'Div id to show tags in'},		
	{p:'showFilterHighlight',ex:false,tt:'show/hide the filter highlight widget'},
	{p:'unhighlightColor',d:'#eee', canCache:true},

	{p:'headerOrientation',ex:'vertical'},
	{p:'filterSliderImmediate',ex:true,tt:'Apply the change while sliding'},
	{p:'filterLogic',ex:'and|or',tt:'Specify logic to apply filters'},		
	{p:'&lt;field&gt;.type',ex:'enumeration|string|boolean'},
	{p:'&lt;field&gt;.filterShow',ex:'false'},
	{p:'&lt;field&gt;.filterLabel'},
	{p:'&lt;field&gt;.filterValue'},
	{p:'&lt;field&gt;.filterValueMin'},
	{p:'&lt;field&gt;.filterValueMax'},
	{p:'&lt;field&gt;.filterValues'},
	{p:'&lt;field&gt;.filterMultiple',ex:true},
	{p:'&lt;field&gt;.filterMultipleSize',ex:5},
	{p:'&lt;field&gt;.filterIncludeAll',ex:true},
	{p:'filterLive',ex:'true',tt:'Search live as the user presses a key'},
	{p:'&lt;field&gt;.filterLive',ex:'true',tt:'Search live as the user presses a key'},
	{p:'&lt;field&gt;.filterDateSelects',
	 ex:'-30 days:Last 30 days,-60 days:Last 60 days,-90 days:Last 90 days,ytd:Year to date,thisyear:This year,year_2022:2022',
	 tt:'Add a menu of select choices for dates'},	
	{p:'&lt;field&gt;.filterDateShowRange',ex:true},
	{p:'&lt;field&gt;.filterDateShowRadio',ex:true},

	{p:'filterDateSelectRadio',ex:true},
	{p:'filterShowCount',ex:false},
	{p:'filterShowTotal',ex:true},		
	{p:'&lt;field&gt;.filterLabel'},
	{p:'&lt;field&gt;.showFilterLabel'},
	{p:'&lt;field&gt;.filterLabelVertical',ex:true},
	{p:'filterLabelVertical',ex:true},				
	{p:'&lt;field&gt;.filterByStyle',ex:'background:white;'},
	{p:'&lt;field&gt;.filterSuggest',tt:'For text input popup a list of matching records',ex:true},
	{p:'&lt;field&gt;.includeAll',ex:true},
	{p:'&lt;field&gt;.filterSort',ex:true},
	{p:'&lt;field&gt;.filterSortCount',ex:true},		
	{p:'&lt;field&gt;.filterStartsWith',ex:true},
	{p:'&lt;field&gt;.filterDisplay',ex:'menu|tab|button|image'},
	{p:'&lt;field&gt;.filterOps',ex:'<,5000000,label1;>,5000000',tt:'Add menu with fixed filters'},
	{p:'excludeUndefined',ex:true,tt:'Exclude any records with an undefined value'},
	{p:'excludeZero',ex:true,tt:'Exclude any records with a 0 value'},
	{p:'filterPaginate',ex:'true',tt:'Show the record pagination'},
	{p:'pageCount',d:1000,tt:'How many records to show when paginating'},	
	{p:'recordSelectFilterFields',tt:'Set the value of other displays filter fields'},
	{p:'selectFields',ex:'prop:label:field1,...fieldN;prop:....'},
	{p:'dataFilters',canCache:true},
	{p:'match value', ex:"dataFilters=\"match(field=field,value=value,label=,enabled=);\"",tt:"Only show records that match"}, 		
	{p:"not match value",ex:"dataFilters=\"notmatch(field=field,value=value,label=,enabled=);\"",tt:"Only show records that dont match"},
	{p:'no missing values',ex:'dataFilters=\"nomissing(field=field,label=,enabled=);\"',tt:'Dont show missing values'},
	{p:'less than',ex:'dataFilters=\"lessthan(field=field,value=value,label=,enabled=);\"'},
	{p:'greater than',ex:'dataFilters=\"greaterthan(field=field,value=value,label=,enabled=);\"'},
	{p:'equals',ex:'dataFilters=\"equals(field=field,value=value,label=,enabled=);\"'},
	{p:'not equals',ex:'dataFilters=\"notequals(field=field,value=value,label=,enabled=);\"'},
	{p:'filterLatest',ex:'fields',tt:'Only show the latest records grouped by fields'},		
	{p:'filterDate',ex:'year',tt:'Show a simple pull down menu to select a year to display'},
	{p:'filterDateIncludeAll',ex:true,tt:'Include all years'},
	{p:'startDate',ex:'yyyy,MM,dd,hh,mm,ss',tt:'Filter data on date'},
	{p:'endDate',ex:'yyyy,MM,dd,hh,mm,ss',tt:'Filter data on date'},

	{label:'Events'},

	{p:DisplayEvent.filterChanged.share,ex:true,tt:'Share filter changed'},
	{p:DisplayEvent.filterChanged.accept,ex:true,tt:'Accept filter changed'},
	{p:DisplayEvent.filterChanged.shareGroup,tt:'Only share in this group'},
	{p:DisplayEvent.filterChanged.acceptGroup,tt:'Only share in this group'},

	{p:DisplayEvent.filteredDataChanged.share,ex:true,tt:'Share filtered data changed'},
	{p:DisplayEvent.filteredDataChanged.accept,ex:true,tt:'Accept filtered data changed'},
	{p:DisplayEvent.filteredDataChanged.shareGroup,tt:'Only share in this group'},
	{p:DisplayEvent.filteredDataChanged.acceptGroup,tt:'Only share in this group'},			




	{p:DisplayEvent.recordSelection.share,ex:true,tt:'Share record selection'},
	{p:DisplayEvent.recordSelection.accept,ex:true,tt:'Accept record selection'},
	{p:DisplayEvent.recordSelection.shareGroup,tt:'Only share in this group'},
	{p:DisplayEvent.recordSelection.acceptGroup,tt:'Only share in this group'},
	{p:'selectNearestDate',tt:'find the closest record'},
	{p:'acceptDateRange',tt:'Accept date range changes'},	


	{p:DisplayEvent.recordHighlight.share,ex:true,tt:'Share record highlight'},
	{p:DisplayEvent.recordHighlight.accept,ex:true,tt:'Accept record highlight'},
	{p:DisplayEvent.recordHighlight.shareGroup,tt:'Only share in this group'},
	{p:DisplayEvent.recordHighlight.acceptGroup,tt:'Only share in this group'},			

	{p:DisplayEvent.recordList.share,ex:true,tt:'Share record list'},
	{p:DisplayEvent.recordList.accept,ex:true,tt:'Accept record list'},
	{p:DisplayEvent.recordList.shareGroup,tt:'Only share in this group'},
	{p:DisplayEvent.recordList.acceptGroup,tt:'Only share in this group'},


	{p:DisplayEvent.fieldsChanged.share,ex:true,tt:'Share fields changed'},
	{p:DisplayEvent.fieldsChanged.accept,ex:true,tt:'Accept fields changed'},
	{p:DisplayEvent.fieldsChanged.shareGroup,tt:'Only share in this group'},
	{p:DisplayEvent.fieldsChanged.acceptGroup,tt:'Only share in this group'},

	{p:DisplayEvent.fieldsSelected.share,ex:true,tt:'Share fields selected'},
	{p:DisplayEvent.fieldsSelected.accept,ex:true,tt:'Accept fields selected'},
	{p:DisplayEvent.fieldsSelected.shareGroup,tt:'Only share in this group'},
	{p:DisplayEvent.fieldsSelected.acceptGroup,tt:'Only share in this group'},			

	{p:DisplayEvent.setEntry.share,ex:true,tt:'When displaying entries as data this shares the selected entry with other displays'},
	{p:DisplayEvent.setEntry.accept,ex:true,tt:'When displaying entries as data this accepts the new entry'},
	{p:DisplayEvent.setEntry.shareGroup,tt:'When sharing the entry this groups what displays to share with'},
	{p:DisplayEvent.setEntry.acceptGroup,tt:'When sharing the entry this must match with the shareGroup'},		

	{p:'acceptEventDataSelection',ex:true,tt:'accept new data coming from other displays'},
	{p:'recordSelectField',tt:'Field to match selection on instead of date'},

	{label:'Convert Data'},
	{p:'applyConvertAfter',ex:true},
	{p:'offset1',canCache:true},
	{p:'offset2',canCache:true},
	{p:'scale',canCache:true},
	{p:'unit',canCache:true},			
	{p:'binDate',ex:'day|month|year',tt:'Bin the dates'},
	{p:'binType',ex:'count|average|total'},
	{p:'groupBy',ex:'field',tt:'Group the data'},
	{p:'aggregateBy',tt:'Add an extra row for the aggregated rows'},
	{p:'aggregateOperator',ex:'sum|percent',tt:'Operator to apply on the aggregated rows'},
	{p:'aggregateOperator.fieldName',ex:'sum|percent',tt:'Operator to apply on the aggregated rows for the given field'},	
	{p:'convertData', label:'Derived data',
	 ex:'derived(field=new_field_id, function=foo*bar);',
	 tt:'Add derived field'},
	{p:'convertData',label:'Convert date',
	 ex:'roundDate(round=hour|day|week|month|year);'},
	{p:'convertData',label:'Filter date',
	 ex:'filterDate(one of month=0);',	 
	 tt:'Round the dates'},
	{p:'convertData',label:'Nominal time',
	 ex:'groupTime(field=field to group time on);',
	 tt:'Round the dates'},	
	{p:'convertData', label:'Replace',
	 ex:'replace(fields=field_ids, pattern=,with=);',
	 tt:'Replace pattern in text field'},
	{p:'convertData',label:'Merge rows',
	 ex:'mergeRows(keyFields=f1\\\\,f2, operator=count|sum|average, valueFields=);',
	 tt:'Merge rows together'},
	{p:'convertData',label:'Percent increase',
	 ex:'addPercentIncrease(replaceValues=false);',
	 tt:'Add percent increase'},
	{p:'convertData',label:'Doubling rate',
	 ex:'doublingRate(fields=f1\\\\,f2, keyFields=f3);',
	 tt:'Calculate # days to double'},
	{p:'convertData',label:'Add fixed',
	 ex:'addFixed(id=max_pool_elevation\\\\,value=3700,type=double);"',
	 tt:'add fixed value'},	
	{p:'convertData',label:'Accumulate data',
	 ex:'accum(fields=);',
	 tt:'Accumulate'},
	{p:'convertData',label:'Add an average field',
	 ex:'mean(fields=);',
	 tt:'Mean'},
	{p:'convertData',label:'Unique rows',
	 ex:'unique(groupFields=f1\\\\,f2,valueField=);',
	 tt:'Uniquify rows'},
	{p:'convertData',label:'Count uniques',
	 ex:'count(field=,sort=true);',
	 tt:'Count uniques'},
	{p:'convertData',label:'Unfurl',
	 ex:'unfurl(headerField=field to get header from,uniqueField=e.g. date,valueFields=);',
	 tt:'Unfurl'},
	{p:'convertData',label:'Rotate data',
	 ex:'rotateData(includeFields=true,includeDate=true,flipColumns=true);',
	 tt:'Rotate data'},
	{p:'convertData',label:'Prune where fields are all NaN',
	 ex:'prune(fields=);',
	 tt:'Prune'},		
	{p:'convertData',label:'Scale and offset',
	 ex:'accum(scale=1,offset1=0,offset2=0,unit=,fields=);',
	 tt:'(d + offset1) * scale + offset2'},		
	{p:'convertDataPost',label:'Same as above but after filtering is done',
	 tt:'Same as above but after filtering is done'},		
	{label:'Color'},
	{p:'colors',ex:'color1,...,colorN',tt:'Comma separated array of colors'},
	{p:'colorBy',ex:'',tt:'Field id to color by'},
	{p:'colorByFields',ex:'',tt:'Show color by fields in a menu'},
	{p:'colorByLog',ex:'true',tt:'Use a log scale for the color by'},
	{p:'colorByMap',ex:'value1:color1,...,valueN:colorN',tt:'Specify colors for color by text values'},
	{p:'colorByLiteral',ex:'true',tt:'use the value as a color'},
	{p:'colorTableAlpha',ex:0.5,tt:'Set transparency on color table values'},
	{p:'colorTableInverse',ex:true,tt:'Inverse the color table'},
	{p:'colorTablePruneLeft',ex:'N',tt:'Prune first N colors'},
	{p:'colorTablePruneRight',ex:'N',tt:'Prune last N colors'},
	{p:'colorByMin',ex:'value',tt:'Min scale value'},
	{p:'colorByMax',ex:'value',tt:'Max scale value'},
	{p:'colorScale',tt:'a semi-colon delimited list of min,max,color1,color2',
	 ex:'9,14.99,palegreen,darkgreen;15,19.99,#ffc966,#ffa500;20, 24.99,red,darkred;25, 27.99,mediumpurple,purple'},



	{p:'nullColor',ex:'transparent'},
	{p:'showColorTable',ex:'false',tt:'Display the color table'},
	{p:'colorTableLabel',ex:'${field}'},
	{p:'colorTableDisplayId',tt:'Dom id to where to place the color table'},
	{p:'colorTableDots',ex:true,tt:'Show as dots'},
	{p:'colorTableDotsWidth',ex:'24px'},
	{p:'colorTableDotsDecimals',ex:'0'},
	{p:'colorTableSide',ex:'bottom|right|left|top'},
	{p:'showColorTableStride',ex:1,tt:'How many colors should be shown'},
	{p:'colorByAllRecords',ex:true,tt:'use all records for color range'},
	{p:'convertColorIntensity',ex:true},
	{p:'intensitySourceMin',ex:'0'},
	{p:'intensitySourceMax',ex:100},
	{p:'intensityTargetMin',ex:1},
	{p:'intensityTargetMax',ex:0},
	{p:'convertColorAlpha',ex:true},
	{p:'alphaSourceMin',ex:0,tt:'map value into range then map it into transparency'},
	{p:'alphaSourceMax',ex:100,tt:'map value into range then map it into transparency'},
	{p:'alphaTargetMin',ex:0,tt:'map value into range then map it into transparency'},
	{p:'alphaTargetMax',ex:1,tt:'map value into range then map it into transparency'},
	{p:'alphaMin',ex:1,tt:'set to transparent any value below the alpha min'},
	{p:'alphaMax',ex:1,tt:'set to transparent any value above the alpha max'},	
	{label:'Animation'},
	{p:'doAnimation',ex:true},
	{p:'animationMode',ex:'sliding|frame|cumulative'},
	{p:'animationUseIndex',ex:'true'},
	{p:'animationInitRange',ex:'start idx,end idx  e.g. "-50,end" or "0,10" or "0,end"'},
	{p:'animationHighlightRecord',ex:true},
	{p:'animationHighlightRecordList',ex:true},
	{p:'animationPropagateRecordSelection',ex:true,tt:'If the animation is in frame mode then propagate the date'},
	{p:'animationAcceptRecordSelection',ex:true,tt:'change the animation date on record select'},
	{p:'acceptEventAnimationChange',ex:false},
	{p:'acceptDateRangeChange',ex:true},
	{p:'animationDateFormat',ex:'yyyy'},
	{p:'animationLabelTemplate'},
	{p:'animationLabelSize',ex:'12pt'},
	{p:'animationStyle'},				
	{p:'animationTooltipShow',ex:'true'},
	{p:'animationTooltipDateFormat',ex:'yyyymmddhhmm'},		
	{p:'animationWindow',ex:'1 day|2 weeks|3 months|1 year|2 decades|etc'},
	{p:'animationStep',ex:'1 day|2 weeks|3 months|1 year|2 decades|etc'},
	{p:'animationSpeed',ex:500},
	{p:'animationLoop',ex:true},
	{p:'animationDwell',ex:1000},
	{p:'animationStartShowAll',ex:true,tt:'Show full range at start'},
	{p:'animationShowButtons',ex:false},
	{p:'animationShowLabel',ex:false},
	{p:'animationShowSlider',ex:false},
	{p:'animationWidgetShort',ex:true},
	{p:'selectFirst',ex:true,tt:'Select the first record when animating so other displays will hilight it'},
	{p:'selectLast',ex:true,tt:'Select the last record when animating so other displays will hilight it'},
    ];

    displayDefineMembers(this,myProps, {
        displayReady: Utils.getPageLoaded(),
        type: argType,
        displayManager: argDisplayManager,
        filters: [],
        dataCollection: new DataCollection(),
        selectedCbx: [],
        entries: [],
        wikiAttrs: [TITLE, "showTitle", "showDetails", "minDate", "maxDate"],
	_properties:[],
	callHook:function(func,arg1,arg2,arg3,arg4) {
	    func = "hook_" + func;
	    func = this.getProperty(func,func);
	    if(func=="none") return null;
	    if(!window[func]) {
		func = this.type+"_"+func;
	    }
	    if(window[func]) {
//		console.log("calling:" + func);
		return window[func](this,arg1,arg2,arg3,arg4);
	    } else {
//		console.log("no hook:" + func);
	    }

	},

	getWikiEditorTags: function() {
	    return this._wikiTags;
	},
	getTypeDef: function() {
	    return this.typeDef;
	},
	getTypeLabel: function() {
	    if(!this.typeDef) return null;
	    return this.typeDef.label;
	},
	getTypeHelpUrl: function() {
	    if(!this.typeDef) return null;
	    let helpUrl = this.typeDef.helpUrl;
	    if(!helpUrl) return null;
	    if(helpUrl===true) {
		return "https://ramadda.org/repository/alias/help_" + this.typeDef.type;
	    }
	    return helpUrl;
	},
	defineSizeByProperties: function() {
	    this.defineProperties([
		{inlineLabel:'Size By'},
	    	{p:'sizeBy',ex:'field',tt:'Field to size points by'},
		{p:'sizeByLog',ex:true,tt:'Use log scale for size by'},
		{p:'sizeByMap', ex:'value1:size,...,valueN:size',tt:'Define sizes if sizeBy is text'},
		{p:'sizeByRadiusMin',ex:'2',tt:'Scale size by'},
		{p:'sizeByRadiusMax',ex:'20',tt:'Scale size by'},
		{p:'sizeByLegendSide',ex:'bottom|top|left|right'},,
		{p:'sizeByLegendStyle'},
		{p:'sizeByLegendLabel'},		
		{p:'sizeBySteps',ex:'value1:size1,v2:s2,...',tt:'Use steps for sizes'},
	    ]);
	},

        getDisplayManager: function() {
            return this.displayManager;
        },
        getLayoutManager: function() {
            return this.getDisplayManager().getLayoutManager();
        },
        addToDocumentUrl: function(key, value,skipPrefix) {
	    HU.addToDocumentUrl(
		(skipPrefix?'':"display"+ this.displayCount+".") + key,value);
	},

	createTagDialog: function(cbxs,  anchor,cbxChange, type,label) { 
	    let cbxInner = HU.div([ATTR_STYLE,HU.css("margin","5px", "width","600px;","max-height","300px","overflow-y","auto")],    Utils.wrap(cbxs,"",""));
	    let inputId = HU.getUniqueId("input_");
	    let input = HU.input("","",[ATTR_STYLE,HU.css("width","300px;"), ATTR_PLACEHOLDER,'Search for ' + label.toLowerCase(),ID,inputId]);
	    let contents = HU.div([ATTR_STYLE,HU.css("margin","10px")], HU.center(input) + cbxInner);
	    if(!this.tagDialogs) this.tagDialogs = {};
	    if(this.tagDialogs[type]) this.tagDialogs[type].remove();
	    let dialog = HU.makeDialog({content:contents,anchor:anchor,title:label,
					draggable:true,header:true});
	    this.tagDialogs[type] = dialog;
	    dialog.find(":checkbox").change(cbxChange);
	    let tags = dialog.find(".display-search-tag");
	    $("#"+inputId).keyup(function(event) {
		let text = $(this).val().trim().toLowerCase();
		tags.each(function() {
		    if(text=="")
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
	    return dialog;
	},
	getAnimationEnabled: function() {
	    return this.getProperty("doAnimation", false);
	},
	getAnimation: function() {
	    if(!this.animationControl) {
		this.animationControl = new DisplayAnimation(this,this.getAnimationEnabled());
	    }
	    return this.animationControl;
	},
        propagateEvent: function(event, data,notThis) {
	    this.getDisplayManager().notifyEvent(event,notThis?null:this,data);
        },
        displayError: function(msg) {
            this.displayHtml(HU.getErrorDialog(msg));
        },
        clearHtml: function() {
            this.displayHtml("");
        },
        displayHtml: function(html) {
            this.setContents(html);
        },
	getEventHandler:function(event) {
	    return this[event.handler];
	},
        notifyEvent: function(event, source, data) { 
	    let func = this.getEventHandler(event);
            if (func==null) {
		if(displayDebug.notifyEventAll)
		    console.log(this.type+".notifyEvent no event handler function:" + event.name  +" " + event.handler);
                return;
            }
	    if(displayDebug.notifyEvent) {
		console.log(this.getLogLabel() +".notifyEvent calling function:" + func.name);
	    }		
            func.apply(this, [source, data]);
        },
	wikify:function(wiki,entryId,wikiCallback,wikiError,containerId) {
	    if(containerId) {
		wikiCallback = html=>{this.addWikiHtml(jqid(containerId),html);};
		wikiError = html=>{jqid(containerId).html(html);};
	    } 
	    wikiError = wikiError ?? (error=>{this.handleError(error);});
	    let url = RamaddaUtil.getUrl("/wikify");
	    $.post(url,{
		doImports:"false",
		entryid:entryId??this.getProperty("entryId"),
		wikitext:wiki},
		   wikiCallback).fail(wikiError);
	},


	myDisplayCount:DISPLAY_COUNT++,
	logMsg:function(msg, time) {
	    let label = this.getLogLabel();
	    if(time) {
		let dt = new Date();
		label += dt.getMinutes()+":" + dt.getSeconds()+":"+ dt.getMilliseconds();
	    }
	    label+=": ";
	    console.log(label+msg);
	},
	getLogLabel: function() {
	    let label = this.type + ("#"+this.myDisplayCount);
	    let name = this.getProperty("name");
	    if(name) label+=" [" + name+"]";
	    return label;
	},
	getColorTableHorizontal: function() {
	    return this.getProperty("colorTableSide","bottom") == "bottom" || this.getProperty("colorTableSide","bottom") == "top";
	},
        displayColorTableHtml: function(html, domId){
	    domId = this.getColorTableDisplayId()?? this.domId(domId);
	    let dom = jqid(domId);
	    dom.html(html);
	},

        displayColorTable: function(ct, domId, min, max, args) {
	    domId = this.getColorTableDisplayId()?? this.domId(domId);
	    //Check if it is a date
	    if(min && min.getTime)  {min  =this.formatDate(min);}
	    if(max && max.getTime)  {max  =this.formatDate(max);}	    
	    if(!args) args = {};
	    let prefix = args.field?args.field.getId()+'.':'';
	    let getProperty = (id,dflt)=>{
		return this.getProperty(prefix+id,
					this.getProperty(id,dflt));
					
	    }

	    if(args.field) args.title=args.field.getLabel();
	    args.showColorTableDots = this.getProperty(prefix+'showColorTableDots',this.getColorTableDots(this.getProperty('showColorTableDots')));
	    args.dotWidth = getProperty('colorTableDotsWidth');
	    args.decimals = getProperty('colorTableDotsDecimals',-1);
	    args.showRange = getProperty('colorTableShowRange');
	    let labels = getProperty('colorTableLabels');
	    args.labels = labels?labels.split(','):null;
	    args.labelStyle=getProperty('colorTableLabelStyle');
	    args.horizontal= this.getColorTableHorizontal();
	    args.stride = getProperty('showColorTableStride',1);
            Utils.displayColorTable(ct, domId, min, max, args);
	    let label = args.label ?? this.getProperty((args.field?args.field.getId():'')+'.colorTableLabel',this.getColorTableLabel());
	    let dom = jqid(domId);
	    if(label) {
		if(args.field) label = label.replace('${field}', args.field.getLabel(false,true));
		if(args.showColorTableDots)
		    dom.prepend(HU.center(label));
		else
		    dom.append(HU.center(label));		
	    }
	    if(!args || !args.colorByInfo) return;
	    dom.find('.display-colortable-slice').css('cursor','pointer');
	    let _this = this;
	    if(!this.originalColorRange) {
		this.originalColorRange = [min,max];
	    }		
	    dom.find('.display-colortable-slice').click(function(e) {
		let val = $(this).attr('data-value');
		let html = '';
		let items = [];
		items.push(HU.b('Range: ') +  HU.input('',min,['size',4,'class','colortable-min']) + ' - ' +
			   HU.input('',max,['size',4,'class','colortable-max']));
		items.push(HU.div([CLASS,'ramadda-clickable ramadda-menu-item','what','reset'],'Reset range'),
			   HU.div([CLASS,'ramadda-clickable ramadda-menu-item','what','ussedata'],'Use data range'));
		items.push(HU.checkbox('colortableuselog',['id','colortableuselog'],
				       _this.getProperty('colorByLog'),'Use Log Scale'));
		html = Utils.wrap(items,'<div style=margin-bottom:4px;>','</div>');
		html = HU.hbox([html, HU.space(3),HU.b('Color Table') +'<br>' +Utils.getColorTablePopup(null,null,null,false)]);
		html =HU.div(['style',HU.css('padding','8px')], html);
		if(_this.colorTableDialog) _this.colorTableDialog.remove();
		let dialog =  _this.colorTableDialog = HU.makeDialog({content:html,title:'Color Table Settings',anchor:$(this),
					     draggable:true,header:true});

		let minInput =dialog.find('.colortable-min');
		let maxInput =dialog.find('.colortable-max');		
		minInput.keypress(function(event) {
		    let keycode = (event.keyCode ? event.keyCode : event.which);
                    if (keycode!= 13) return;
		    if(!Utils.isDefined(_this.getProperty('colorByMinOrig'))) {
			_this.setProperty('colorByMinOrig',_this.getProperty('colorByMin'));
		    }
		    _this.setProperty('colorByMin',$(this).val());
		    _this.setProperty('overrideColorRange', true);
		    _this.forceUpdateUI();
		});

		maxInput.keypress(function(event) {
		    let keycode = (event.keyCode ? event.keyCode : event.which);
                    if (keycode!= 13) return;
		    if(!Utils.isDefined(_this.getProperty('colorByMaxOrig'))) {
			_this.setProperty('colorByMaxOrig',_this.getProperty('colorByMax'));
		    }
		    _this.setProperty('colorByMax',$(this).val());
		    _this.setProperty('overrideColorRange', true);
		    _this.forceUpdateUI();
		});


		dialog.find('.ramadda-colortable-select').click(function() {
		    let ct = $(this).attr('colortable');
		    if(ct) {
			_this.setProperty('colorTable',ct);
			_this.forceUpdateUI();
		    }		    
		});
		dialog.find('#colortableuselog').change(function() {
		    _this.setProperty('colorByLog',$(this).is(':checked'));
		    _this.forceUpdateUI();
		});
		dialog.find('.ramadda-menu-item').button().click(function() {
		    let what = $(this).attr('what');
		    _this.setProperty('useDataForColorRange', false);
		    if(what == 'reset') {
			_this.setProperty('colorByMin',_this.getProperty('colorByMinOrig'));
			_this.setProperty('colorByMax',_this.getProperty('colorByMaxOrig'));
			_this.setProperty('overrideColorRange', false);
		    } else  if(what == 'ussedata') {
			_this.setProperty('useDataForColorRange', true);
		    }
		    _this.forceUpdateUI();
		    minInput.val(_this.getProperty('colorByMinOrig')??min);
		    maxInput.val(_this.getProperty('colorByMaxOrig')??max);
		});
	    });
        },
	getColorList:function() {
	    if(this.colorList && this.colorList.length>0) {
		return this.colorList;
	    }
	    if (this.getProperty("colors") && this.getProperty("colors")!="default") {
		let v = this.getProperty("colors");
		if(!Array.isArray(v)) {
		    v = v.split(",");
		}
		this.colorList =  v;
	    }
	    if(!this.colorList || this.colorList.length==0) {
		this.colorList= ['blue', 'red', 'green', 'orange', 'fuchsia', 'aqua',   'navy', 'brown','cadetblue','blueviolet','coral','cornflowerblue','darkcyan','darkgoldenrod','darkorange','darkseagreen'];
	    }
	    return this.colorList;
	},
        getColorTableName: function(names) {
	    if(names && !Array.isArray(names)) {
		names  = [names];
	    }
            let ct = null;
            if (names) {
		names.every(name=>{
                    ct = this.getProperty(name);
		    if(ct) {
			if(displayDebug.colorTable)
			    this.logMsg("getColorTableName: name:" + name +" ct1:"  + ct);
			return false;
		    }
		    return true;
		});
            } else {
		let colorBy = this.getProperty("colorBy");
		if(colorBy) {
                    ct = this.getProperty("colorTable." + colorBy);
		    if(ct && displayDebug.colorTable)
			this.logMsg("getColorTableName: " + "colorTable." + colorBy+" ct2:"  + ct);
		}
		if(!ct) {
                    ct = this.getProperty("colorBar", this.getProperty("colorTable"));
		    if(ct && displayDebug.colorTable)
			this.logMsg("getColorTableName: ct3:"  + ct);
		}
            }
            if (ct == "none") return null;
	    if(displayDebug.colorTable) this.logMsg("getColorTableName:" + names +" color table:" + ct);
            return ct;
        },
	getColorTable: function(justColors, names, dflt) {
	    if(names && !Array.isArray(names)) {
		names  = [names];
	    }
	    if(names) {
		names =names.filter(name=>{
		    return name!=null;
		});
	    }

	    if(names && justColors && this.dynamicProperties && names.includes("colorTable")) {
		let ct;
		let gotOne = false;
		names.every(name=>{
		    if(this.dynamicProperties[name])
			gotOne = true;
		    return !gotOne;
		});
		if(!gotOne && this.dynamicProperties['colors']) {
		    let colors = this.dynamicProperties['colors'];
		    if(!Array.isArray(colors)) {
			//Check for commas
			colors = colors.replace(/\\,/g,"_comma_");
			colors = colors.split(",");
			colors = colors.map(c=>{
			    return c.replace(/_comma_/g,",");
			});
			return colors;
		    }

		}
	    }
	    //Check the dynamic properties for
            let colorTable = this.getColorTableName(names);
            if (!colorTable) {
                colorTable = dflt;
            }

	    if(displayDebug.colorTable) this.logMsg("CT:" + names +" " + justColors +" name:" + colorTable);
	    return this.getColorTableInner(justColors, colorTable);
	},
	getColorTableInner: function(justColors, colorTable) {
	    let list;
            if (colorTable) {
                let ct = null;
 		if(colorTable.startsWith("colors:")) {
		    list = colorTable.substring("colors:".length).split(",");
                    return this.convertColors(list);
		}
                ct = Utils.ColorTables[colorTable];
                if (ct && justColors) {
		    return this.convertColors(ct.colors);
		}
                if (!ct && name) {
                    return this.convertColors(colorTable.split(","));
                }
                return ct;
            }
            if (this.getProperty("colors") && this.getProperty("colors")!="default") {
                let colors = this.getProperty("colors");
                if ((typeof colors) != "object") colors = colors.split(",");
		return this.convertColors(colors);
            }
            return null;
        },
	addAlpha: function(colors, alpha) {
	    if(!colors) return null;
	    alpha = Utils.isDefined(alpha)?alpha:this.getProperty("colorTableAlpha");
	    if(!Utils.isDefined(alpha)) return colors;
	    colors=  Utils.cloneList(colors);
	    let ac = [];
	    colors.forEach((c)=>{
		ac.push(Utils.addAlphaToColor(c,alpha));
	    });
	    return ac;
        },
        convertColors: function(colors) {
	    colors = this.addAlpha(colors);
	    if(this.getColorTableInverse()) {
		let tmp = [];
		for(let i=colors.length-1;i>=0;i--)
		    tmp.push(colors[i]);
		colors = tmp;
	    }
	    if(this.getProperty('colorTablePruneLeft')) {
		let tmp = [];
		for(let i=+this.getProperty('colorTablePruneLeft');i<colors.length;i++) {
		    tmp.push(colors[i]);
		}
		colors = tmp;
	    }
	    if(this.getProperty('colorTablePruneRight')) {
		let tmp = [];
		let d = +this.getProperty('colorTablePruneRight');
		for(let i=0;i<colors.length-d;i++) {
		    tmp.push(colors[i]);
		}
		colors = tmp;
	    }
	    return colors;
	},

        getColorByColors: function(records, dfltColorTable) {
            let colorBy = this.getProperty('colorBy');
            if (!colorBy) {
                return null;
            }
            let colorByField = this.getFieldById(fields, colorBy);
            if (!colorByField) {
                return null;
            }
            let obj = this.getColumnValues(records, colorByField);
            let colors = this.getColorTable();
            if (!colors) colors = Utils.getColorTable(dfltColorTable || 'blue_white_red');
            if (!colors) return null;
            let min = parseFloat(this.getProperty('colorByMin', obj.min));
            let max = parseFloat(this.getProperty('colorByMax', obj.max));
            if (colors.colors) colors = colors.colors;
            let range = max - min;
            let colorValues = [];
            for (let i = 0; i < obj.values.length; i++) {
                let value = obj.values[i];
                let percent = (value - min) / range;
                let index = parseInt(percent * colors.length);
                if (index >= colors.length) index = colors.length - 1;
                else if (index < 0) index = 0;
                colorValues.push(colors[index]);
            }
            return {
                colors: colorValues,
                min: min,
                max: max
            };
        },
	getDefaultGridByArgs: function() {
	    let doHeatmap=this.getProperty('doHeatmap',false);
	    let args =  {
		display:this,
		shape:this.getProperty('cellShape','rect'),
		color: this.getProperty('cellColor','blue'),
		stroke: !this.getProperty('cellFilled',true),
		cellSize: this.getProperty('cellSize',doHeatmap?0:4),
		cellSizeH: this.getProperty('cellSizeH',20),
		cellSizeHBase: this.getProperty('cellSizeHBase',0),
		cell3D:this.getProperty('cell3D',false),
		cellShowText:this.getProperty('cellShowText',false),
		cellLabels:Utils.split(this.getProperty('cellLabels')),
		cellFonts:Utils.split(this.getProperty('cellFonts')),
		cellLabelColors:Utils.split(this.getProperty('cellLabelColor')),
		cellLabelPositions:Utils.split(this.getProperty('cellLabelPositions')),
		cellLabelOffsetsX:Utils.split(this.getProperty('cellLabelOffsetsX')),
		cellLabelOffsetsY:Utils.split(this.getProperty('cellLabelOffsetsY')),
		doHeatmap:doHeatmap,
		operator:this.getProperty('hm.operator',this.getProperty('hmOperator','count')),
		filter:this.getProperty('hm.filter',this.getProperty('hmFilter'))
	    };
	    args.cellSizeX = +this.getProperty('cellSizeX',args.cellSize);
	    args.cellSizeY = +this.getProperty('cellSizeY',args.cellSize);
	    return args;
	},
	getIconMap: function() {
	    let iconMap;
	    let iconMapProp = this.getProperty('iconMap');
	    if (iconMapProp) {
                let toks = iconMapProp.split(',');
		iconMap = {};
                for (let i = 0; i < toks.length; i++) {
		    let toks2 = toks[i].split(':');
		    if (toks2.length > 1) {
                        iconMap[toks2[0]] = toks2[1];
		    }
		}
            }
	    return iconMap;
	},
	getColorByInfo: function(records, prop,colorByMapProp, defaultColorTable,propPrefix,lastColorBy,props) {
	    if(this.getColorByAllRecords()) {
		records = this.getRecords();
	    }
	    if(!records) return null;
	    let fields = this.getFields();
	    return new ColorByInfo(this, fields??[], records, prop,colorByMapProp, defaultColorTable, propPrefix,null,props,lastColorBy);
	},
	getColorByMap: function(prop) {
	    prop = this.getProperty(prop||'colorByMap');
	    this.debugGetProperty=false;
	    return Utils.parseMap(prop);
        },
        toString: function() {
            return  this.type + ' - ' + this.getId();
        },
        getType: function() {
            return this.type;
        },
        getClass: function(suffix) {
            if (suffix == null) {
                return this.getBaseClass();
            }
            return this.getBaseClass() + "-" + suffix;
        },
        getBaseClass: function() {
            return "display-" + this.getType();
        },
        setDisplayManager: function(cm) {
            this.displayManager = cm;
            this.setDisplayParent(cm.getLayoutManager());
        },
        setContents: function(contents,dontWrap) {
            this.clearDisplayMessage();
            if(!dontWrap)
		contents = HU.div([ATTR_STYLE,this.getProperty("displayInnerStyle",""),ATTR_CLASS, "display-contents-inner display-" + this.getType() + "-inner"], contents);
            this.writeHtml(ID_DISPLAY_CONTENTS, contents);
        },
        addEntry: function(entry) {
            this.entries.push(entry);
        },
        clearCachedData: function() {},
        setEntry: function(entry) {
	    if(displayDebug.setEntry)
		this.logMsg("setEntry:" + entry);
            this.entries = [];
            this.addEntry(entry);
            this.entry = entry;
            this.entryId = entry.getId();
            this.clearCachedData();
            if (this.properties.theData) {
		//Clear this out
		this.cacheUrl = null;
                this.dataCollection = new DataCollection();
                let attrs = {
                    entryId: this.entryId,
                    lat: this.getProperty("latitude"),
                    lon: this.getProperty("longitude"),
                };
		let oldUrl=  this.properties.theData.url;
		if(!oldUrl) {
		    oldUrl = this.getRamadda().getRoot() + "/entry/show?entryid=" + entry.getId() + "&output=points.product&product=points.json&max=5000";
		} else {
		    //this should work
		    oldUrl = oldUrl.replace(/entryid=.*?&/,"entryid=" + entry.getId()+"&");
		}
                this.properties.theData = this.data = new PointData(entry.getName(), null, null, oldUrl, attrs);
		this.startProgress();
		if(displayDebug.setEntry)
		    this.logMsg("setEntry: calling loadData:" + oldUrl);
                this.data.loadData(this);
            } else {
		this.callUpdateUI();
	    }
            let title = "";
            if (this.getShowTitle()) {
                this.jq(ID_TITLE).html(entry.getName());
            }
        },
        getTextColor: function(property, dflt) {
            if (property) return this.getProperty(property, this.getProperty("textColor",dflt));
            return this.getProperty("textColor", "#000");
        },
        getTitleHtml: function(title) {
            let titleToShow = "";
            if (this.getShowTitle()) {
                let titleStyle = HU.css("color" , this.getTextColor("titleColor","#000"));
                let bg = this.getProperty("titleBackground");
                if (bg) titleStyle += HU.css('background', bg,'padding','2px','padding-right','6px','padding-left','6px');
                titleToShow = this.getShowTitle() ? this.getDisplayTitle(title) : "";
		let entryId = this.getProperty("entryId") || this.entryId;
                if (entryId) {
                    titleToShow = HU.href(this.getRamadda().getEntryUrl(entryId), titleToShow, [ATTR_CLASS, "display-title",  ATTR_STYLE, titleStyle]);
		}
		titleToShow =HU.span([ID,this.domId(ID_TITLE)],titleToShow);
            }

	    if(this.getProperty("showEntryIcon")) {
		let icon = this.getProperty("entryIcon");
		if(icon) titleToShow  = HU.image(icon) +" " + titleToShow;
	    }
            return titleToShow;
        },
        handleEventMapClick: function(source, args) {
            if (!this.dataCollection) return;
            let pointData = this.dataCollection.getList();
            for (let i = 0; i < pointData.length; i++) {
                pointData[i].handleEventMapClick(this, source, args.lon, args.lat);
            }
        },
	acceptEvent:function(event,dflt) {
	    return this.getProperty(event.accept,dflt);
	},
	shareEvent:function(event,dflt) {
	    return this.getProperty(event.share,dflt);
	},	
        handleEventMapBoundsChanged: function(source, args) {
	    if(this.acceptEvent(DisplayEvent.mapBoundsChanged,this.getProperty("acceptBoundsChange"))) {
		this.filterBounds  = args.bounds;
		this.callUpdateUI();
            }
        },

        handleEventFilterFieldsSelected: function(source, fields) {
	    if(fields.length>0 && (typeof fields[0] =="string")) {
		let tmp = [];
		fields.forEach(f=>{
		    f = this.getFieldById(null, f);
		    if(f) tmp.push(f);
		});
		fields=tmp;
	    }
	    let prop = "";
	    fields.forEach(f=>{
		if(prop!="") prop+=",";
		prop+=f.getId();
	    });

	    this.setProperty("filterFields",prop);
	    this.haveCalledUpdateUI = false;
            this.checkSearchBar();
        },


        handleEventFieldValueSelected: function(source, args) {
            this.setProperty("filterPattern", args.value);
            this.setProperty("patternFilterField", args.field.getId());
            this.callUpdateUI();
        },
        setDateRange: function(min, max, doDay) {
	    this.minDateObj = min;
	    this.maxDateObj = max;
	    this.dateRangeDoDay = doDay;
//	    console.log("setDateRange: " + this.minDateObj +" " + this.maxDateObj);
	},
        handleDateRangeChanged: function(source, prop) {
	    this.setDateRange(prop.minDate, prop.maxDate);
	    if(this.getAnimation().getEnabled()) {
		this.getAnimation().setDateRange(prop.minDate, prop.maxDate);
	    }
	    this.haveCalledUpdateUI = false;
	    this.dataFilterChanged();
	},
	displayFieldsChanged:  function(val, fromElsewhere) {
	    this.addToDocumentUrl(PROP_FIELDS,val);
	    this.setProperty(PROP_FIELDS,val);
	    this.callUpdateUI();
    
	    if(this.displayFieldsMenuEnums && fromElsewhere && this.getProperty("showDisplayFieldsMenu")) {
		let selected = [];
		this.jq("displayfields").val(val);
	    }
	},
        handleEventFilterChanged: function(source, prop) {
	    if(!this.acceptEvent(DisplayEvent.filterChanged, this.getProperty('acceptEventFilter',true))) {
		return;
	    }
	    this.haveCalledUpdateUI = false;
	    let properties = prop.properties;
	    if(!properties) {
		properties=[];
		properties.push(prop);
	    }
	    this.settingFilterValue = true;
	    properties.forEach(prop=> {
		let filter = this.filterMap?this.filterMap[prop.fieldId]:null;
		if(!filter) return;
		let widgetId = this.getFilterId(prop.fieldId);
		if(prop.id && prop.id.endsWith("date_from")) {
		    widgetId+="_date_from";
		} else 	if(prop.id && prop.id.endsWith("date_to")) {
		    widgetId+="_date_to";
		}
		if(prop.fieldId == "_highlight") {
		    this.jq(ID_FILTER_HIGHLIGHT).val(prop.value);
		    this.setProperty("filterHighlight", prop.value=="highlight");
		    this.dfltFilterHighlight = null;
		} else 	if(Utils.isDefined(prop.value2)) {
		    $("#" +widgetId+"_min").val(prop.value);
		    $("#" +widgetId+"_min").attr("data-value", prop.value);
		    $("#" +widgetId+"_max").val(prop.value2);
		    $("#" +widgetId+"_max").attr("data-value", prop.value2);
		} else {
		    filter.handleEventPropertyChanged(prop);
		}
	    });
	    this.settingFilterValue = false;
	    this.dataFilterChanged();
	},
        handleEventPropertyChanged: function(source, prop) {
	    let debug = displayDebug.handleEventPropertyChanged;
	    if(prop.property == "dateRange") {
		if(this.getProperty("acceptDateRangeChange")) {
		    this.handleDateRangeChanged(source, prop);
		}
		return;
	    }

	    if(prop.property == "displayFields") {
		if(!this.acceptEvent(DisplayEvent.fieldsChanged,!this.getProperty("acceptEventDisplayFieldsChange",false))) {
		    return;
		}
		this.displayFieldsChanged(prop.value, true);
		return
	    }

	    if(prop.property == "macroValue") {
		if(prop.entryId!= this.entryId) return;
		if(!this.getProperty("acceptRequestChangeEvent",true)) {
		    return;
		}
		let macros = this.getRequestMacros();
		let macro = null;
		macros.every(m=>{
		    if(m.isMacro(prop.id)) {
			macro = m;
			return false;
		    }
		    return true;
		});

		if(!macro) {
		    return;
		}
		if(macro.type=='date') return
		if(!this.getProperty("request." + macro.name + ".acceptChangeEvent",true)) {
		    return;
		}

		macro.setValue(prop);
		if(debug)
		    console.log(this.getId() +" event-reloading");
		this.reloadData();
		return;
	    }

            this.setProperty(prop.property, prop.value);
            this.callUpdateUI();
        },
        handleEventRecordList: function(source, args) {
	    if(this.getAnimationEnabled() && this.getProperty("animationHighlightRecordList")) {
		this.getAnimation().setRecordListHighlight(args.recordList);
	    }
	    if(this.getProperty("acceptEventRecordList",false)) {
		this.recordListOverride = args.recordList;
		this.haveCalledUpdateUI = false;
		this.callUpdateUI();
	    }
	},
        handleEventRecordHighlight: function(source, args) {
	    if(this.getAnimation().getEnabled() &&  !args.skipAnimation) {
		this.getAnimation().handleEventRecordHighlight(source, args);
	    }
	},
        handleEventAnimationChanged: function(source, args) {
	    if(!this.getProperty("acceptEventAnimationChange",true)) return;
	    if(this.getAnimation().getEnabled()) {
		this.getAnimation().handleEventAnimationChanged(args);
	    }
	},
        handleEventSetEntry: function(source, args) {
//	    this.logMsg("handleEventSetEntry ");
//	    console.dir(args);
	    if(this.acceptEvent(DisplayEvent.setEntry,this.getProperty(DisplayEvent.setEntry.acceptGroup,this.getProperty("acceptShareSelectedEntry",false)))) {
		if(displayDebug.setEntry)
		    console.log(this.type+".handleEventSetEntry calling setEntry:" + args.entry);
		this.setEntry(args.entry);
	    } else {
		if(displayDebug.setEntry)
		    console.log(this.type+".handleEventSetEntry not calling setEntry:" + args.entry);
	    }
	},
        propagateEventRecordSelection: function(args) {
	    if(this.getSelectPopup() && args.record) {
		let html = this.applyRecordTemplate(args.record,this.getDataValues(args.record),null, this.getSelectPopup());
		this.showDialog(html,null,null,this.getSelectPopupTitle());
	    }


	    if(displayDebug.notifyEvent)
		console.log(this.type+".propagateEventRecordSelection");
	    if(this.shareEvent(DisplayEvent.setEntry,this.getProperty(DisplayEvent.setEntry.shareGroup,this.getProperty("shareSelectedEntry")))) {
		let entryId = args.record.getValueFromField("id");
		if(displayDebug.setEntry)
		    console.log(this.type+" sharing entry:" + entryId);
		if(entryId) {
		    let _this = this;
		    setTimeout(async function(){
			await getGlobalRamadda().getEntry(entryId, entry => {
			    if(displayDebug.setEntry)
				console.log(_this.type+" calling notifyEvent with entry:" + entry);
			    _this.getDisplayManager().notifyEvent(DisplayEvent.setEntry, _this, {entry:entry});
			});
		    });
		}
	    }
	    if(this.shareEvent(DisplayEvent.recordSelection,true)) {
		this.getDisplayManager().notifyEvent(DisplayEvent.recordSelection, this, args);
	    }
	    if(this.getProperty("recordSelectFilterFields")) {
		let fields = this.getFieldsByIds(null,this.getProperty("recordSelectFilterFields"));
		if(fields && fields.length>0) {
		    let props = {
			properties:[]
		    };
		    fields.forEach(field=>{
			props.properties.push({
			    id:field.getId(),
			    fieldId: field.getId(),
			    value: args.record.getValue(field.getIndex())
			});
		    })
		    this.propagateEvent(DisplayEvent.filterChanged, props);
		}
	    }
	},
	findRecordIndex:function(records,record) {
	    let index =-1;
	    for(let i=0;i<records.length;i++) {
		if(records[i].getId() == record.getId()) {
		    index = i;
		    break;
		}
	    }
	    if(index<0 && this.getSelectNearestDate()) {
		let date = record.getDate();
		let nearestDate = null;
		let min =0;
		let nearestIndex = -1;
		for(let i=0;i<records.length;i++) {
		    let record = records[i];
		    let diff = Math.abs(date.getTime()-record.getDate().getTime());
		    if(nearestIndex==-1) {
			nearestIndex=0;
			nearestDate = record.getDate();
			min = diff;
			continue;
		    }
		    if(diff<min) {
			nearestIndex = i;
			min = diff;
		    }
		}
		index=nearestIndex;
	    }
	    return index;
	},

        handleEventRecordSelection: function(source, args) {
	    this.selectedRecord= args.record;
	    if(this.selectedRecord) {
		if(this.getProperty("colorThresholdField")) {
		    this.haveCalledUpdateUI = false;
		    this.callUpdateUI();
		}
		if(this.getAnimationEnabled() && source!=this.getAnimation()) {
		    if(this.getProperty("animationAcceptRecordSelection",false)) {
			let date = this.selectedRecord.getDate();
			if(date) 
			    this.getAnimation().setDateRange(date,date);
		    }
		}
		let fieldId = this.getFilterSetValueFieldId();
		if(fieldId) {
		    let field =this.getFieldById(null,fieldId);
		    if(field) {
			let value = field.getValue(this.selectedRecord);
			this.handleEventFilterChanged(this,{id:'',fieldId:field.getId(),value:value});
		    }
		}
	    }

            if (!source.getEntries) {
                return;
            }
            let entries = source.getEntries();
            for (let i = 0; i < entries.length; i++) {
                let entry = entries[i];
                let containsEntry = this.getEntries().indexOf(entry) >= 0;
                if (containsEntry) {
                    this.highlightEntry(entry);
                    break;
                }
            }
        },
        areaClear: function() {
            this.getDisplayManager().notifyEvent("areaClear", this);
        },
        handleEventEntryMouseover: function(source, args) {},
        handleEventEntryMouseout: function(source, args) {},
        handleEventFilteredTimes: function(source, args) {
	    let times=args.times;
	    if(!times) return;
	    this.getAnimation().setTimes(times);
	},
        handleEventEntrySelection: function(source, args) {
            let containsEntry = this.getEntries().indexOf(args.entry) >= 0;
            if (!containsEntry) {
                return;
            }
            if (args.selected) {
                this.jq(ID_TITLE).addClass("display-title-select");
            } else {
                this.jq(ID_TITLE).removeClass("display-title-select");
            }
        },
        highlightEntry: function(entry) {
            this.jq(ID_TITLE).addClass("display-title-select");
        },
        getEntries: function() {
            return this.entries;
        },
        getDisplayEntry: async function(callback) {
            let entries = this.getEntries();
            if (entries != null && entries.length > 0) {
                return Utils.call(callback, entries[0]);
            }
	    let entryId = this.entryId|| this.getProperty("entryId");
            if (entryId) {
                let entry;
                await this.getRamadda().getEntry(entryId, e => {
                    entry = e
                    Utils.call(callback, entry);
                });
            } else {
		Utils.call(callback, null);
	    }
        },
        hasEntries: function() {
            return this.entries != null && this.entries.length > 0;
        },
        getWaitImage: function() {
            return HU.image(RamaddaUtil.getCdnUrl("/icons/progress.gif"));
        },
	useDisplayMessage:function() {
	    return true;
	},
	setDisplayMessage:function(msg) {
	    if(this.dataLoadFailed) {
		return;
	    }
	    if(!Utils.stringDefined(msg)) {
		this.jq(ID_DISPLAY_MESSAGE).html("").hide();
		return;
	    }
	    let contents =  this.jq(ID_DISPLAY_CONTENTS);
	    let minHeight = contents.css("min-height");
	    if(!minHeight || minHeight=="0px") {
		contents.css("min-height","75px");
		contents.attr("display-set-minheight","true");
	    }
	    this.jq(ID_DISPLAY_MESSAGE).html(msg).show();
	},
	clearDisplayMessage:function() {
	    let contents =  this.jq(ID_DISPLAY_CONTENTS);
	    this.jq(ID_DISPLAY_MESSAGE).hide();
	    if(contents.attr("display-set-minheight")!=null) {
		contents.css("min-height","");
	    }
	},	
        getLoadingMessage: function(msg) {
	    if(this.getAcceptEventDataSelection()) {
		return "";
	    }

	    //Check if we didn't have any data specified
	    if(!msg && !this.getProperty("theData") && !this.getInlineDataSrc()) {
		msg = "No data specified"
	    }
	    if (!msg) msg = this.getProperty("loadingMessage", "icon_progress Loading data...");
	    if(msg=='') return '';
	    let plain = (msg=='icon_progress');
	    msg = msg.replace("icon_progress",HU.image(icon_progress));
	    if(plain) return msg;
	    if(this.useDisplayMessage()) {
		return SPACE+msg;
	    } 
            return HU.div([ATTR_STYLE, HU.css("text-align","center")], this.getMessage(SPACE + msg));
        },
	reloadData: function() {
	    this.dataLoadFailed = false;
	    this.startProgress();
	    this.haveCalledUpdateUI = false;
	    if(this.getProperty("okToLoadData",true))  {
		let pointData = this.dataCollection.getList()[0];
		pointData.loadData(this,true);
	    }
	},
        getMessage: function(msg) {
            return HU.div([ATTR_CLASS, "display-output-message"], msg);
        },
	getNoDataMessage: function() {
	    return this.getProperty("noDataMessage","No data available");
	},
        getFieldValue: function(id, dflt) {
            let jq = $("#" + id);
            if (jq.length > 0) {
                return jq.val();
            }
            return dflt;
        },
        getFieldValues: function(id, dflt) {
            let jq = $("#" + id);
            if (jq.length > 0) {
		let v = [];
		jq.each(function(){
		    v.push($(this).val());
		});
		return v;
            }
            return dflt;
        },

        getFooter: function() {
            return HU.div([ATTR_ID, this.getDomId(ID_FOOTER), ATTR_CLASS, "display-footer"],
			  HU.leftRight(HU.div([ATTR_ID, this.getDomId(ID_FOOTER_LEFT), ATTR_CLASS, "display-footer-left"], ""),
				       HU.div([ATTR_ID, this.getDomId(ID_FOOTER_RIGHT), ATTR_CLASS, "display-footer-right"], "")));
        },
        shouldSkipField: function(field) {
            if (this.skipFields && !this.skipFieldsList) {
                this.skipFieldsList = this.skipFields.split(",");
            }

            if (this.skipFieldsList) {
                return this.skipFieldsList.indexOf(field.getId()) >= 0;
            }
            return false;
        },
        fieldSelected: function(event) {
            this.userHasSelectedAField = true;
            this.selectedFields = null;
            this.overrideFields = null;
            this.removeProperty(PROP_FIELDS);
            this.fieldSelectionChanged();
            if (event.shiftKey) {
                let fields = this.getSelectedFields();
                this.propagateEvent(DisplayEvent.fieldsSelected, fields);
            }
        },
        addFieldsCheckboxes: function(argFields) {
            if (!this.hasData()) {
                return;
            }
            let fixedFields = this.getPropertyFields()
            if (fixedFields != null) {
                if (fixedFields.length == 0) {
                    fixedFields = null;
                }
            }

	    let fieldsMap = null;
	    if(fixedFields!=null) {
		if(!Array.isArray(fixedFields)) fixedFields=fixedFields.split(",");
		fieldsMap = {};
		fixedFields.forEach(id=>{
		    //look for numeric field
		    if(id.startsWith("@")) {
			let index = parseInt(id.substring(1).trim())-1;
			let fidx=0;
			let fields = this.getFields();
			fields.every(field=>{
			    if(!field.isNumeric()) return true;
			    if(fidx==index) {
				fieldsMap[field.getId()]  = true;
				return false;
			    }
			    fidx++;
			    return true;
			});
			return
		    }


		    if(id.startsWith("#")) {
			let toks = id.split("-");
			if(toks.length==2) {
			    let idx1 = +toks[0].replace("#","");
			    let idx2 = +toks[1].replace("#","");			
			    for(let i=idx1;i<=idx2;i++) {
				fieldsMap["#"+i] = true;
			    }
			}
		    }
		    fieldsMap[id]  = true;
		});
	    }

	    let makeCheckboxes = this.showFieldsInDialog();
            let html = "";
            let checkboxClass = this.getId() + "_checkbox";
            let groupByClass = this.getId() + "_groupby";
            let dataList = this.dataCollection.getList();

            if (argFields != null) {
                this.overrideFields = [];
            }
            let seenLabels = {};


            let badFields = {};
            let flags = null;
            for (let collectionIdx = 0; collectionIdx < dataList.length; collectionIdx++) {
                let pointData = dataList[collectionIdx];
                let fields = this.getFieldsToSelect(pointData);
                if (this.canDoGroupBy()) {
                    let allFields = pointData.getRecordFields();
                    let cnt = 0;
                    for (i = 0; i < allFields.length; i++) {
                        let field = allFields[i];
                        if (field.getType() != "string") continue;
                        if (cnt == 0) {
                            html += HU.div([ATTR_CLASS, "display-dialog-subheader"], "Group By");
                            html += HU.open(TAG_DIV, [ATTR_CLASS, "display-fields"]);
                            let on = this.groupBy == null || this.groupBy == "";
                            html += HU.tag(TAG_DIV, [ATTR_TITLE, "none"],
					   HU.radio("none", this.getDomId("groupby"), groupByClass, "none", !on) + " None");
                        }
                        cnt++;
                        let on = this.groupBy == field.getId();
                        let idBase = "groupby_" + collectionIdx + "_" + i;
                        field.radioId = this.getDomId(idBase);
                        html += HU.tag(TAG_DIV, [ATTR_TITLE, field.getId()],
				       HU.radio(field.radioId, this.getDomId("groupby"), groupByClass, field.getId(), on) + " " + field.getUnitLabel() + " (" + field.getId() + ")"
				      );
                    }
                    if (cnt > 0) {
                        html += HU.close(TAG_DIV);
                    }
                }

                let disabledFields = "";
                if ( /*this.canDoMultiFields() && */ fields.length > 0) {
                    let selected = this.getSelectedFields([]);
                    let selectedIds = [];
                    for (let i = 0; i < selected.length; i++) {
                        selectedIds.push(selected[i].getId());
                    }
                    html += HU.div([ATTR_CLASS, "display-dialog-subheader"], "Displayed Fields");
                    html += HU.open(TAG_DIV, [ATTR_CLASS, "display-fields"]);
                    for (let tupleIdx = 0; tupleIdx < fields.length; tupleIdx++) {
                        let field = fields[tupleIdx];
                        let idBase = "cbx_" + collectionIdx + "_" + tupleIdx;
                        field.checkboxId = this.getDomId(idBase);
                        let on = false;
                        let hasValues = (flags ? flags[field.getIndex()] : true);
//                        console.log(tupleIdx + " field: " + field.getId() + "has values:" + hasValues);
                        if (argFields != null) {
                            //console.log("argFields:" + argFields);
                            for (let fIdx = 0; fIdx < argFields.length; fIdx++) {
                                if (argFields[fIdx].getId() == field.getId()) {
                                    on = true;
                                    //                                        console.log("argField:"+ argFields[fIdx].getId()+ " field.id:" + field.getId() +" on:" +on);
                                    this.overrideFields.push(field.getId());
                                    break;
                                }
                            }
                        } else if (selectedIds.length > 0) {
                            on = selectedIds.indexOf(field.getId()) >= 0;
//                            console.log("selected ids   on:" + on +" " + field.getId());
                        }
			if(!on) {
			    if (fieldsMap != null) {
				on = fieldsMap[field.getId()];
				if (!on) {
                                    on = fieldsMap["#" + (tupleIdx + 1)];
				}
                            } else if (this.overrideFields != null) {
				on = this.overrideFields.indexOf(field.getId()) >= 0;
				if (!on) {
                                    on = (this.overrideFields.indexOf("#" + (tupleIdx + 1)) >= 0);
				}
				//                                console.log("override fields  on:" + on +" " + field.getId());
                            } else {
				if (this.selectedCbx.indexOf(field.getId()) >= 0) {
                                    on = true;
				} else if (this.selectedCbx.length == 0) {
                                    on = (tupleIdx == 0);
				}
			    }
                            //                                console.log("cbx fields:" + on + " " + field.getId());
                        }
//			if(on)    console.log(field.getId(),on);
                        let label = field.getUnitLabel();
                        if (seenLabels[label]) {
                            label = label + " " + seenLabels[label];
                            seenLabels[label]++;
                        } else {
                            seenLabels[label] = 1;
                        }

                        if (!hasValues) {
                            disabledFields += HU.div([], label);
                        } else {
                            if (field.derived) {
                                label += " (derived)";
                            }
                            let widget;
			    if(!makeCheckboxes) {
				html += HU.tag(TAG_DIV, [],label + ' - ' +field.getId());
			    } else {
				if (this.canDoMultiFields()) {
                                    widget = HU.checkbox(field.checkboxId, [ATTR_CLASS, checkboxClass,ATTR_TITLE,field.getId() +' - '+ field.getType()], on,label);
				    html += HU.tag(TAG_DIV, [ATTR_TITLE, field.getId()], widget);
				} else {
                                    widget = HU.radio(field.checkboxId, "field_radio", checkboxClass, "", on);
				    html += HU.tag(TAG_DIV, [ATTR_TITLE, field.getId()],  widget + " " + label);
				}
			    }
                        }
                        //                        html+= "<br>";
                    }
                }
                if (disabledFields != "") {
                    html += HU.div([ATTR_STYLE, HU.css("border-top","1px #888  solid")], "<b>No Data Available</b>" + disabledFields);
                }
                html += HU.close(TAG_DIV);
            }


            this.writeHtml(ID_FIELDS, html);

            this.userHasSelectedAField = false;
            let theDisplay = this;
            //Listen for changes to the checkboxes
            $("." + checkboxClass).click(function(event) {
                theDisplay.fieldSelected(event);
            });

            $("." + groupByClass).change(function(event) {
                theDisplay.groupBy = $(this).val();
                if (theDisplay.displayData) {
                    theDisplay.displayData();
                }
            });
        },
        fieldSelectionChanged: function() {
            this.setDisplayTitle();
            if (this.displayData) {
                this.clearCachedData();
                this.displayData();
            }
        },
        defaultSelectedToAll: function() {
            return true;
        },
        setSelectedFields: function(fields) {
            this.clearCachedData();
            this.selectedFields = fields;
            this.addFieldsCheckboxes(fields);
        },
        getSelectedFields: function(dfltList) {
	    let prefixFields = this.getProperty('prefixFields');
	    let debug = displayDebug.getSelectedFields || this.getProperty('debugFields');
	    if(debug)
		console.log(this.type +".getSelectedFields");
	    if(this.getBinDate()) {
		let binType = this.getBinType("total");
		let binCount = binType=="count";
		if(binCount) {
		    let fields = [];
		    fields.push(new RecordField({
			id:binType,
			label:this.getProperty("binDateLabel", this.getProperty("binCountLabel","Count")),
			type:"double",
			chartable:true
		    }));		    
		    return fields;
		} 
	    }

            this.debugSelected = debug;
            this.lastSelectedFields = this.getSelectedFieldsInner(dfltList);
	    let notFields = this.getProperty("notFields");
	    if(notFields) {
		this.lastSelectedFields = this.lastSelectedFields.filter(f=>{
		    if(f.getId().match(notFields) || f.getLabel().match(notFields)) return false;
		    return true;
		});
	    }


	    if(debug)
		console.log("\tsetting lastSelectedFields:" + this.lastSelectedFields);
            this.setDisplayTitle();
	    if(this.getBinDate()) {
		let binType = this.getProperty("binType","total");
		let fields = [];
		this.lastSelectedFields.forEach(field=>{
		    if(!field.isNumeric()) {
			fields.push(field);
		    } else {
			const prefix = binType;
			if(field.getId().startsWith(prefix)) {
			    fields.push(field);
			} else {
			    fields.push(new RecordField({
				id:prefix +"_"+ field.getId(),
				index:  field.getIndex(),
				label:this.getProperty("binDateLabel", Utils.camelCase(binType) +" of " + field.getLabel()),
				type:"double",
				chartable:field.isChartable()
			    }));
			}
		    }
		});
		this.lastSelectedFields = fields;
//		console.log("BIN DATE:" + this.lastSelectedFields);
	    }
	    //	    console.log("fields:" + this.lastSelectedFields);

	    let result =  Utils.cloneList(this.lastSelectedFields??[]);
	    if(prefixFields) {
		let p  =this.getFieldsByIds(null, prefixFields);
		if(p.length) result = [...p,...result];
	    }
	    return result;

        },
        getSelectedFieldsInner: function(dfltList) {
            if (this.debugSelected) {
                console.log("getSelectedFieldsInner dflt:" + (dfltList ? dfltList : "null"));
                console.log("\tlast selected = " + this.lastSelectedFields);
	    }
            if (this.selectedFields) {
                if (this.debugSelected)
                    console.log("\treturning this.selectedFields:" + this.selectedFields);
                return this.selectedFields;
            }
            let df = [];
            let dataList = this.dataCollection.getList();
            //If we have fixed fields then clear them after the first time
            let fixedFields = this.getPropertyFields();
            if (fixedFields && (typeof fixedFields) == "string") {
                fixedFields  = fixedFields.split(",");
	    }
	    if(fixedFields) {
		let tmpFields  = [];
		fixedFields.forEach(tok=>{
		    if(!tok.match("-")) {
			tmpFields.push(tok);
			return;
		    }
		    let pair = tok.split("-");
		    let i1 = parseFloat(pair[0].trim().substring(1));
		    let i2 = parseFloat(pair[1].trim().substring(1));
		    for(let i=i1;i<=i2;i++) tmpFields.push("#" + i);
		});
		fixedFields = tmpFields;
	    }


	    let aliases= {};
	    let tmp = this.getFieldAliases();
	    if(tmp) {
		tmp.split(",").forEach(tok=>{
		    [name,alias] =   tok.split(":");
		    aliases[alias] = name;
		});
	    }
            for (let collectionIdx = 0; collectionIdx < dataList.length; collectionIdx++) {
                let pointData = dataList[collectionIdx];
		//A hack in case we already have a pointData set (e.g., in the case of a convertDataPost)
		if(this.pointData) pointData = this.pointData;
                let fields = this.getFieldsToSelect(pointData);
		//xxx
//		console.log(fields.map(f=>{return f.getId() + '-' + f.getLabel();}));
                if (fixedFields != null && fixedFields.length > 0) {
                    if (this.debugSelected)
                        console.log("\thave fixed fields:" + fixedFields);
		    let selected = [];
                    for (let i = 0; i < fixedFields.length; i++) {
                        let sfield = fixedFields[i];
			if(sfield =="*") {
			    selected  =fields;
			    break;
			}
			let field = this.getFieldById(fields, sfield);
                        if(field) {
			    selected.push(field);
			}
                    }
		    if(this.getProperty("fieldsNumeric")) {
			selected = selected.filter(f=>f.isNumeric());
		    }		    
		    df = selected;
		}
	    }

	    
            if (!this.userHasSelectedAField && fixedFields != null && fixedFields.length > 0) {
                if (this.debugSelected)
                    console.log("\tfrom fixed:" + df.length);
                return df;
            }

	    this.userHasSelectedAField = false;
            if (this.debugSelected)
                console.log("\tuser has selected");
            let fieldsToSelect = null;
            let firstField = null;
            this.selectedCbx = [];
            let cbxExists = false;
            for (let collectionIdx = 0; collectionIdx < dataList.length; collectionIdx++) {
                let pointData = dataList[collectionIdx];
                fieldsToSelect = this.getFieldsToSelect(pointData);
                for (let i = 0; i < fieldsToSelect.length; i++) {
                    let field = fieldsToSelect[i];
                    if (firstField == null && field.isNumeric()) firstField = field;
                    let idBase = "cbx_" + collectionIdx + "_" + i;
                    let cbxId = this.getDomId(idBase);
                    let cbx = $("#" + cbxId);
                    if (cbx.length>0) {
                        cbxExists = true;
                    } else {
                        continue;
                    }
                    if (cbx.is(':checked')) {
                        this.selectedCbx.push(field.getId());
                        df.push(field);
                    }
                }
            }

            if (df.length == 0 && !cbxExists) {
                if (this.lastSelectedFields && this.lastSelectedFields.length > 0) {
                    if (this.debugSelected)
                        console.log("\tlastSelectedFields:" + this.lastSelectedFields);
                    return this.lastSelectedFields;
                }
            }
            if (df.length == 0) {
                df = this.getDefaultSelectedFields(fieldsToSelect, dfltList,this.debugSelected);
                if (this.debugSelected)
                    console.log("\tusing default selected:" + df);
            }
            return df;
        },
        getDefaultSelectedFields: function(fields, dfltList,debugArg) {
	    let debug = debugArg||displayDebug.getDefaultSelectedFields;
	    if(debug)
		console.log("getDefaultSelectedFields");
	    let patterns = this.getProperty("fieldsPatterns");
	    if(patterns) {
		let allFields = this.getFields();
		if(allFields) {
		    let debugPatterns = false;
		    let matched=[];
		    if(debugPatterns)
			console.log("fields:" +allFields);
		    patterns.split(",").forEach(pattern=>{
			if(debugPatterns)
			    console.log("\tpattern:" + pattern);
			allFields.every(f=>{
			    if(!f.isFieldNumeric()) return true;
			    let id = f.getId().toLowerCase();
			    if(debugPatterns)
				console.log("\t\tid:" + id);
			    if(id.match(pattern)) {
				if(debugPatterns)
				    console.log("\t\tmatch");
				if(!matched.includes(f)) {
				    if(debugPatterns)
					console.log("\t\tadd to matched");
				    //				    console.log("\tmatches:"+ id);
				    matched.push(f);
				    return false;
				}
			    }
			    return true;
			});
		    });
		    if(debugPatterns)
			console.log("returning:" + matched);
		    if(matched.length)
			return matched;
		}
	    }
            if (this.defaultSelectedToAll()) {
		let allFields = this.getFields();
                let tmp = [];
		if(allFields) {
                    for (i = 0; i < allFields.length; i++) {
			let field = allFields[i];
			if (!field.isFieldGeo()) {
                            tmp.push(field);
			}
                    }
		}
		if(debug)
		    console.log("\treturning allFields:" + tmp);
                return tmp;
            }

            if (dfltList != null) {
		if(debug)
		    console.log("\treturning dfltList:" + dfltList);
                return dfltList;
            }
            for (i = 0; i < fields.length; i++) {
                let field = fields[i];
                if (field.isNumeric() && !field.isFieldGeo()) return [field];
            }
            return [];
	},

	    
	sortRecords: function(records, sortFields,sortOnDate) {
	    if(this.getSortOnDate() || sortOnDate) {
		//Clone the list
		records = Utils.cloneList(records);
		records.sort(function(a, b) {
		    if (a.getDate() && b.getDate()) {
			if (a.getDate().getTime() < b.getDate().getTime()) return -1;
			if (a.getDate().getTime() > b.getDate().getTime()) return 1;
			return 0;
		    }
		});
	    }

	    if(sortOnDate) return records;


	    if(!sortFields) {
		let f = this.getProperty("sortFields", "", true);
		if(f=="${fields}") f = this.getProperty("fields", "", true);
		sortFields = this.getFieldsByIds(null, f);
		if(sortFields.length==0 && this.sortByFields && this.sortByFields.length>0) {
		    sortFields = [this.sortByFields[0]];
		}
	    }


	    if(sortFields.length>0) {
		records = Utils.cloneList(records);
		let sortAscending = this.getSortAscending();
		let cnt = 0;
		records.sort((a,b)=>{
		    let row1 = this.getDataValues(a);
		    let row2 = this.getDataValues(b);
		    let result = 0;
		    for(let i=0;i<sortFields.length;i++) {
			let sortField = sortFields[i];
			let v1 = row1[sortField.getIndex()];
			let v2 = row2[sortField.getIndex()];
//			console.log(v1,v2);
			if(sortField.isNumeric() || sortField.isFieldDate()) {
			    if(isNaN(v1) && isNaN(v2)) {
				result= 0;
			    } else if(isNaN(v1)) {
				result = sortAscending?-1:1;
			    } else if(isNaN(v2)) {
				result = sortAscending?1:-1;
			    } else {
				if(v1<v2) result = sortAscending?-1:1;
				else if(v1>v2) result = sortAscending?1:-1;
				else result = 0;
			    }
			} else {
			    result = String(v1).localeCompare(String(v2));
			    if(!sortAscending) result=-result;
			}
			if(result!=0) break;
		    }
		    return result;
		});
	    }

	    
	    /*
	    console.log("results " + sortFields);
	    records.forEach(record=>{
		let row1 = this.getDataValues(record);
		let sortField = sortFields[0];
		console.log('\t'+row1[1] +' '+ row1[sortField.getIndex()]);
		});
	    */
	    

	    if(this.getProperty("sortHighlight")) {
		records = Utils.cloneList(records);
		records.sort((a,b)=>{
		    let h1 = a.isHighlight(this);
		    let h2 = b.isHighlight(this);
		    if(h1 && !h2)
			return -1;		    
		    if(!h1 && h2)
			return 1;		    
		    return 0;
		});
	    }

	    if(this.getReverse()) {
		records = Utils.cloneList(records);
		let tmp = [];
		for(let i=records.length-1;i>=0;i--)
		    tmp.push(records[i]);
		records = tmp;
	    }

	    return records;
	},
        getFieldById: function(fields, id,debug,ignore,foo) {
	    //Support one arg
	    if(debug)
		console.log("getFieldById:" + id);
	    if(fields!=null && id==null) {
		if(typeof fields!="string") {
		    if(debug)
			console.log("\tbadfields:" + fields);
		    return null;
		}
		id = fields;
		fields=null;
	    }
            if (!id) {
		if(debug)
		    console.log("\tno id");
		return null;
	    }
	    id = String(id).trim();
	    if (!fields) {
                let pointData = this.getData();
                if (pointData == null) {
		    if(debug)
			console.log("\tno data");
		    return null;
		}
                fields = pointData.getRecordFields();
		if(debug) {
		    console.log("\tusing  fields:" + fields);
		}
            }
	    if (!fields) {
		return null;
	    }
	    let aliases= {};
	    let tmp = this.getFieldAliases();
	    if(tmp) {
		tmp.split(",").forEach(tok=>{
		    [name,alias] =   tok.split(":");
		    aliases[alias] = name;
		});
	    }
	    let theField = null;
	    id.split("|").every(fieldId=>{
		let alias = aliases[fieldId];
		let hasRegexp = fieldId.indexOf("*")>=0;
		let numericCnt = 0;
		for (let i = 0; i < fields.length; i++) {
                    let field = fields[i];
		    if(field.isFieldNumeric()) numericCnt++;
		    if(debug)	{
			console.log("\tField:" + field.getId());
		    }

		    if(fieldId=="#") {
			if(field.isFieldNumeric()) {
			    theField =  field;
			    return false;
			}
			continue;
		    }
		    if(fieldId==('@'+numericCnt)) {
			theField = field;
			return false;
		    }

                    if (field.getId() == fieldId || fieldId == ("#" + (i+1)) || field.getId()==alias) {
			theField =  field;
			if(debug)
			    console.log("\tgot it:" + theField);
			return false;
                    }
		    if(hasRegexp) {
			if(field.getId().match(fieldId)) {
			    theField =  field;
			    if(debug)
				console.log("\tgot it from pattern:" + theField);
			    return false;
			}
		    }
		}
		return true;
	    });
	    if(debug || displayDebug.getSelectedFields)
		console.log("\tgot:" + theField);
	    if(!theField && !ignore) {
		if(debug || displayDebug.getSelectedFields || this.getProperty('debugFields')) {
		    this.logMsg("can't find field field:" + id);
		    console.log(fields.reduce((acc,f)=>{
			return acc+' ' + f.getId();
		    },''));
		}

	    }
	    
            return theField;
        },

        getFieldsByIds: function(fields, ids) {
	    if (!fields) {
                let pointData = this.getData();
                if (pointData != null) {
                    fields = pointData.getRecordFields();

		}
            }

	    if(!fields) return [];

            let result = [];
            if (!ids) {
		return result;
	    }
	    if(ids=="*") return fields;
	    if(ids=="#") {
		return  fields.filter(f=>{
		    return !f.isFieldLongitude() &&
			!f.isFieldLatitude() &&
			f.isFieldNumeric();
		});
	    }
            if ((typeof ids) == "string")
                ids = ids.split(",");
            if (!fields) {
                let pointData = this.getData();
                if (pointData == null) {
		    return null;
		}
                fields = pointData.getRecordFields();
            }

            for (let i = 0; i < ids.length; i++) {
		let id = ids[i];
		//Check for numeric range
		if(id.startsWith("#")) {
		    let toks = id.split("-");
		    if(toks.length==2) {
			let idx1 = +toks[0].replace("#","");
			let idx2 = +toks[1].replace("#","");			
			for(let j=idx1;j<=idx2;j++) {
			    let f = this.getFieldById(fields, "#" + idx1);
			    if (f) result.push(f);
			}
			continue;
		    }
		}
                let f = this.getFieldById(fields, ids[i]);
                if (f) result.push(f);
            }
            return result;
        },

        getFieldByType: function(fields, type) {
            fields = this.getFieldsByType(fields, type);
            if (fields.length == 0) return null;
            return fields[0];
        },
        getFieldsByType: function(fields, type) {
            if (!fields) {
                let pointData = this.getData();
                if (pointData == null) return null;
                fields = pointData.getRecordFields();
            }
            let list = [];
            let numeric = (type == "numeric");
            let isString = (type == "string");
            for (a in fields) {
                let field = fields[a];
		if(field.isRecordDate()) continue;
                if (type == null) return field;
                if (numeric) {
                    if (field.isFieldNumeric()) {
                        list.push(field);
                    }
                } else if(isString) {
                    if (field.isFieldString()) {
                        list.push(field);
                    }
                    
                } else if (field.getType() == type) {
                    list.push(field);
                }
            }
            return list;
        },
	getDateValues: function(records) {
	    let dates = [];
	    records.forEach(r=>{
		dates.push(r.getDate());
	    });
	    return dates;
	},
        getColumnValues: function(records, field) {
            let values = [];
            let min = Number.MAX_VALUE;
            let max = Number.MIN_VALUE;
            for (let rowIdx = 0; rowIdx < records.length; rowIdx++) {
                let record = records[rowIdx];
                let row = record.getData();
                let value = row[field.getIndex()];
                values.push(value);
                if (Utils.isNumber(value) && !isNaN(value)) {
                    min = Math.min(min, value);
                    max = Math.max(max, value);
                }
            }
            return {
                values: values,
                min: min,
                max: max
            };
        },
        requiresGrouping:  function() {
            return false;
        },
	makeTree: function(records) {
	    if(records==null)  {
		records = this.getRecords();
            }
	    if(records==null)  return;
	    let treeTemplate = this.getProperty("treeTemplate");
	    let treeTooltip = this.getProperty("treeTooltip");
	    let roots = [];
	    let idToNode = {};
	    let nodes=[];
	    let idToRoot = {};
	    let labelField = this.getFieldById(null, this.getProperty("labelField"));
	    let nodeFields = this.getFieldsByIds(null, this.getProperty("nodeFields"));
	    let treeRootLabel = this.getProperty("treeRoot");
	    let treeRoot = null;
	    if(treeRootLabel) {
		treeRoot = {id:treeRootLabel,label:treeRootLabel,children:[],parent:null};
		roots.push(treeRoot);
	    }
	    if(nodeFields.length>0) {
		let cnt = 0;
		let valueToNode = {};
		let parentId = "";
		records.forEach(r=>{
		    let label= labelField==null?id:r.getValue(labelField.getIndex());		
		    let parentId = null;
		    let parentNode= null;
		    //		    console.log("record:" + label);

		    nodeFields.forEach(nodeField=>{
			let id = r.getValue(nodeField.getIndex());
			let nodeId = parentId?parentId+"-"+id:id;
			let tmpNode = idToNode[nodeId];
			if(!tmpNode) {
			    tmpNode = {id:nodeId,label:id,children:[],parent:parentNode};
			    idToNode[nodeId] = tmpNode;
			    if(!parentNode) {
				if(treeRoot) {
				    tmpNode.parent = treeRoot;
				    treeRoot.children.push(tmpNode);
				} else {
				    roots.push(tmpNode);
				}
			    }
			    if(parentNode) {
				parentNode.children.push(tmpNode);
			    }
			}
			parentId = nodeId;
			parentNode = tmpNode;
		    });
		    let id= "leaf" + (cnt++);
		    let node = {id:id,label:label,children:[],record:r, parent:parentNode};
		    parentNode.children.push(node);
		    idToNode[id] = node;
		    nodes.push(node);
		});
		return roots;
	    }

	    //{label:..., id:...., record:...,	    children:[]}
            let parentField = this.getFieldById(null, this.getProperty("parentField"));
	    let idField = this.getFieldById(null, this.getProperty("idField"));
	    if(!parentField) {
		throw new Error("No parent field specified");
	    }
	    if(!idField) {
                throw new Error("No id field specified");
	    }
	    records.forEach(r=>{
		let parent = r.getValue(parentField.getIndex());
		let id = r.getValue(idField.getIndex());
		let label= labelField==null?id:r.getValue(labelField.getIndex());		
		let node = {id:id,label:label,children:[],record:r,parentId:parent, parent:null};
		if(treeTemplate) {
		    node.display = this.getRecordHtml(r, null, treeTemplate);
		}
		if(treeTooltip) {
		    node.tooltip = this.getRecordHtml(r, null, treeTooltip);
		}
		idToNode[id] = node;
		nodes.push(node);
		if(parent=="") {
		    //is a root
		    idToRoot[id]=node;
		    if(treeRoot) {
			node.parent = treeRoot;
			node.parentId = treeRoot.id;
			treeRoot.children.push(node);
		    } else {
			roots.push(node);
		    }
		}
	    });
	    nodes.forEach(node=>{
		let parentNode = idToNode[node.parentId];
		if(!parentNode) {
		    if(!idToRoot[node.id]) {
			throw new Error("No parent :" + node.parentId +" for node:" + node.label);
		    }
		    return;
		}
		node.parent= parentNode;
		parentNode.children.push(node);
	    });

	    return roots;
	},
	getSegments: function() {
	    let segments = this.getProperty("timeSegments");
	    if(!segments) return null;
	    let result = [];
	    let segmentList = segments.split(",");
	    segmentList.forEach((tok,segmentIdx)=>{
		let toks = tok.split(";");
		let name = toks[0];
		let start = Utils.parseDate(toks[1],false);
		let end = Utils.parseDate(toks[2],false);
		result.push({name: name, start:start,end:end});
	    });
	    return  result;
	},
	requiresGeoLocation: function() {
	    return false;
	},
	checkDataFilters: function(dataFilters, record) {
	    if(!dataFilters) {return true;}
	    for(let i=0;i<dataFilters.length;i++) {
		if(!dataFilters[i].isRecordOk(record)) return false;
	    }
	    return true;
	},
	getTheDataFilters: function(v) {
	    return DataUtils.getDataFilters(this, v || this.getDataFilters());
	},
	dfltFilterHighlight:null,
	getFilterHighlight: function() {
	    if(!this.dfltFilterHighlight) {
		this.dfltFilterHighlight = {
		    filterHighlight:this.getProperty("filterHighlight",false)
		}
	    }
	    return this.dfltFilterHighlight.filterHighlight;
	},
	getFilterTextMatchers: function() {
	    let highlight  = [];
	    if(this.filters) {
		for(let filterIdx=0;filterIdx<this.filters.length;filterIdx++) {
		    let filter = this.filters[filterIdx];
		    let widget =jqid(filter.getWidgetId());
		    if(widget.length==0 || !widget.val || widget.val()==null) continue;
		    let value = widget.val()??"";
		    if(!Utils.stringDefined(value)) continue;
		    let id = filter.getId();
		    if(id=='_text_') id=null;
		    highlight.push(new TextMatcher(value,id));
		}
	    }
	    return highlight;
	},

	filterDataPhase2:function(records) {
	    return records;
	},

	filterData: function(records, fields, args) {
            if (!records)  records =this.getRecords();
	    let filteredRecords = this.filterDataInner(records,fields, args);
	    if(filteredRecords) {
		if(this.getPropagateFilteredTimes()) {
		    if(filteredRecords.length!=records.length) {
			let times=[];
			let seen = {};
			filteredRecords.forEach(record=>{
			    let date = record.getDate();
			    if(!date) return;
			    if(!seen[date]) {
				seen[date] = true;
				times.push(date);
			    }
			});
			if(times.length) {
	                    this.propagateEvent(DisplayEvent.filteredTimes, {times:times});
			}
		    }
		}		    
	    }
	    if(this.shareEvent(DisplayEvent.filteredDataChanged,false)) {
		this.getDisplayManager().notifyEvent(DisplayEvent.filteredDataChanged, this, {
		    records:filteredRecords
		});
	    }

	    if(this.getApplyConvertAfter()) {
		let tmp = new  PointData("pointdata", this.originalPointData.recordFields, filteredRecords);
		//xxxx
		tmp = this.convertPointData(tmp);
		this.convertedFields = tmp.getRecordFields();
		filteredRecords = tmp.getRecords();
	    }	    


	    return filteredRecords
	},
        handleEventFilteredDataChanged: function(source, args) {
	    this.propagatedFilteredRecords = args.records;
	    if(args.records) {
		this.forceUpdateUI();
	    }
	},
	findUnique:function(records, ufield) {
	    if(!ufield) return records;
	    let umap = {};
	    let ulist = [];
	    for(let i=records.length-1;i>=0;i--) {
		let record = records[i];
		let v = record.getValue(ufield.getIndex());
		if(!Utils.isDefined(umap[v])) {
		    umap[v] = true;
		    ulist.push(record);
		}
	    }
	    return ulist;
	},
	filterDataInner: function(records, fields, args) {
	    if(this.propagatedFilteredRecords && this.originalPointData) {
//		console.log('filter data before',this.propagatedFilteredRecords[0].data);
		let tmp = new  PointData("pointdata", this.originalPointData.recordFields, this.propagatedFilteredRecords);
		tmp = this.convertPointData(tmp);
//		console.log('filter data after convert:',tmp.getRecords()[0].data);
		records =tmp.getRecords();
	    }

	    if(this.recordListOverride) {
		return this.recordListOverride;
	    }
	    let opts = {
		doGroup:false,
		skipFirst:false,
		applyDateRange: true,
		recordOk:null
	    }
	    if(args)
		$.extend(opts,args);
	    let debug =  displayDebug.filterData;
	    if(debug) this.logMsg("filterData");


	    if(this.getAnimationEnabled()) {
		if(this.getProperty("animationFilter", true)) {
		    this.setDateRange(this.getAnimation().begin, this.getAnimation().end);
		}
	    }


	    let highlight =  this.getFilterHighlight();
	    let startDate = this.getProperty("startDate");
	    let endDate = this.getProperty("endDate");
	    if(startDate) {
		this.startDateObject = Utils.createDate(startDate,+this.getTimeZoneOffset());
		if(debug)
		    this.logMsg(this.type +" start date:" + startDate + " dttm:" + this.startDateObject.toUTCString());
	    } 
	    if(endDate) {
		this.endDateObject = Utils.createDate(endDate,+this.getTimeZoneOffset());
		if(debug)
		    this.logMsg(this.type +"end date:" +this.endDateObject.toUTCString());
	    } 



	    let filterDate = this.getProperty("filterDate");
	    if(filterDate) {
		let date = $("#"+ this.getFilterId(ID_FILTER_DATE)).val();
		if(date) {
		    if(date=="all") {
			this.setDateRange(null,null);
		    } else {
			date = new Date(date);
			if(filterDate == "year") {
			    let end = date.getFullYear()+1;
			    this.setDateRange(new Date(date.getFullYear()+"-01-01"),
					      new Date(end+"-01-01"));
			} else if(filterDate == "day") {
			    let f = date.getUTCFullYear() + "-" + (date.getUTCMonth() + 1) +"-" + date.getUTCDate();
			    let dttm = new Date(f);
			    this.setDateRange(dttm,dttm, true);
			} else {
			    //TODO month and day
			}
		    }
		}
	    }


            if (!records) {
		return null;
	    }


            if (!fields) {
                fields = this.getFields();
            }
            if(opts.doGroup || this.requiresGrouping()) {
		let pointData = this.getData();
                records = pointData.extractGroup(this.dataGroup, records);
            }

	    if(debug)   this.logMsg("filter #records:" + records.length);
	    if(this.getProperty("filterLatest")) {
		let fields = this.getFieldsByIds(null,this.getProperty("filterLatest"));
		let max = {};
		let keyToRecord = {};
		let tmp = [];
		let keys = [];
		records.forEach(record=>{
		    if(!record.getTime()) return;
		    let key = "";
		    fields.forEach(f=>{
			key+="_" + record.getValue(f.getIndex());
		    });
		    let maxRecord = keyToRecord[key];
		    if(!maxRecord) {
			keyToRecord[key] = record;
			keys.push(key);
		    } else {
			if(record.getDate().getTime()>maxRecord.getDate().getTime()) keyToRecord[key] = record;
		    }
		});


		keys.forEach(key=>{
		    tmp.push(keyToRecord[key]);
		});
		records  =tmp;
	    }


	    records.forEach(r=>{
		r.clearHighlight(this);
	    });

//	    debug=true;
//	    if(debug)   this.logMsg("checking dates");
	    records = records.filter((record,idx)=>{
                let date = record.getDate();
		return this.dateInRange(date,idx,idx<5 && debug);
	    });
	    if(debug)   this.logMsg("filter Fields:" + this.filters.length +" #records:" + records.length);



//	    debug = this.type=='template';
	    if(this.filters.length) {
		let newData = [];
		let logic = this.getProperty("filterLogic","and");
		this.filters.forEach(f=>f.prepareToFilter(debug));
		if(debug)
		    this.logMsg("filter:" + this.filters.length+' #records:' + records.length);
		records.forEach((record,rowIdx)=>{
		    let _debug = rowIdx<5&&debug;
		    let allOk = true;
		    let anyOk = false;		    
		    this.filters.forEach(filter=>{
			if(!filter.isEnabled()) {
			    if(_debug) this.logMsg('filter not enabled');
			    return;
			}
			let filterOk = filter.isRecordOk(record, _debug);
			if(_debug) this.logMsg('Filter ok:' + filterOk);
			if(!filterOk) allOk = false;
			else anyOk = true;
		    });
		    let ok = logic=="and"?allOk:anyOk;
		    if(opts.skipFirst && rowIdx==0) {
			ok = true;
		    }
//		    this.logMsg("\trow:" + rowIdx+" ok:" + ok);
		    if(highlight) {
			newData.push(record);
			record.setHighlight(this, ok);
		    } else {
			record.clearHighlight(this);
			if(ok) {
			    newData.push(record);
			}
		    }
		});
		debug = false;
		records = newData;
	    }


	    if(debug)   this.logMsg("filterData-2 #records:" + records.length);

            let stride = parseInt(this.getProperty("stride", -1));
            if (stride < 0) {
		let maxSize = parseInt(this.getProperty("maxDisplayedPoints", -1));		
		if(maxSize>0 && records.length>0) {
		    stride = 1;
		    while(records.length/stride>maxSize) {
			stride++;
		    }
		}
	    }

            if (stride > 0) {
                let list = [];
                let cnt = 0;
                for (let i = 0; i < records.length; i += (stride + 1)) {
                    list.push(records[i]);
                }
                records = list;
		//		this.logMsg("stride: " + stride +"  size:" + list.length);
		if(debug)   this.logMsg("R-3:" + records.length);
            }



	    records = this.filterDataPhase2(records);
	    let maxRecords  = this.getMaxRecords();
	    if(Utils.isDefined(maxRecords)) {
		let tmp =[];
		records.every((r,idx)=>{
		    if(idx>=maxRecords) return false;
		    tmp.push(r);
		    return true;
		});
		records = tmp;
	    }




	    if(debug)   this.logMsg("filterData-3 #records:" + records.length);
	    if(this.getFilterPaginate()) {
		let skip = this.pageSkip||0;
		let count = +this.getPageCount();
		if(skip>0 || count<records.length) {
		    let tmp = [];
		    let newSkip = skip;
		    count = Utils.max(count, 1000);
		    while(true) {
			if(newSkip<records.length) break;
			newSkip-=count;
			if(newSkip<0) {
			    break;
			}
		    }
		    if(newSkip<0) newSkip=0;
		    if(newSkip!=skip)
			this.updatePaginateLabel(skip,count,records.length);
		    skip = newSkip;
		    this.logMsg("skip:" + skip +" count:" + count +" " + records.length);
		    for(let i=skip;i<records.length;i++) {
			tmp.push(records[i]);
			if(tmp.length>=count) break;
		    }
		    records=tmp;
		}
	    }


	    if(this.getProperty("binDate")) {
		let what = this.getProperty("binDate");
		let binType = this.getProperty("binType","total");
		let binCount = binType=="count";
		let binned = [];
		let record = records[0];
		let map ={};
		let counts ={};
		this.binRecordToRecords = {};
		let keyToRecord={};
		for (let i = 0; i < records.length; i++) {
		    let record = records[i];
		    let tuple = this.getDataValues(record);
		    let key;
		    let baseDate=null
		    if(what=="month") {
			key = record.getDate().getUTCFullYear() + "-" + (record.getDate().getUTCMonth() + 1);
		    } else if(what=="day") {
			key = record.getDate().getUTCFullYear() + "-" + (record.getDate().getUTCMonth() + 1) +"-" + record.getDate().getUTCDate();
		    } else if(what=="week") {
			let week = +Utils.formatDateWeek(record.getDate());
			key = record.getDate().getUTCFullYear()+"-"+week;
			let d =  (1 + (week - 1) * 7);
			baseDate = new Date(record.getDate().getUTCFullYear(), 0, d);			
		    } else {
			key = record.getDate().getUTCFullYear()+"";
		    }
		    if(!Utils.isDefined(map[key])) {
			counts[key]=1;
			let date = baseDate;
			if(!baseDate) {
			    date = Utils.parseDate(key);
			}
			let data = Utils.cloneList(record.getData());
			if(binCount) {
			    for(k=0;k<data.length;k++) data[k]=1;
			}
			let newRecord = new  PointRecord(fields, record.getLatitude(),record.getLongitude(),
							 record.getElevation(),date,data);

			keyToRecord[key] = newRecord;
			this.binRecordToRecords[newRecord.getId()] = {
			    records:[record],
			}

			map[key] = data;
			binned.push(newRecord);
		    } else {
			let newRecord = keyToRecord[key];
			this.binRecordToRecords[newRecord.getId()].records.push(record);
			counts[key]++;
			let tuple1 = map[key];
			if(binCount) {
			    for(k=0;k<tuple1.length;k++) tuple1[k]++;
			    continue;
			} 
			let tuple2 = record.getData();
			for(let j=0;j<tuple2.length;j++) {
			    let v = tuple2[j];
			    if((typeof v) !="number") continue;
			    if(isNaN(v)) continue;
			    if(isNaN(tuple1[j])) tuple1[j] = v;
			    else tuple1[j] +=v;
			}
		    }
		}
		if(binType == "average") {
		    for(key in counts) {
			let tuple = map[key];
			for(let j=0;j<tuple.length;j++) {
			    let v = tuple[j];
			    if((typeof v) !="number") continue;
			    if(isNaN(v)) continue;
			    tuple[j] = v/counts[key];
			}
		    }
		}

		records = binned;
	    }

	    if(debug)   this.logMsg("filterData-4 #records:" + records.length);
	    if(this.requiresGeoLocation()) {
		records = records.filter(r=>{return r.hasLocation();});
	    }
	    if(debug)   this.logMsg("filterData-5 #records:" + records.length);
	    let dataFilters = this.getTheDataFilters();
	    if(dataFilters.length) {
		if(debug)   this.logMsg("have filters:" + dataFilters);
//		console.time('filters');
		records = records.filter((r,idx)=> {
		    if(!this.checkDataFilters(dataFilters, r)) {
			return false;
		    } 
		    return true;
		});
//		console.timeEnd('filters');
	    }
	    if(debug)   this.logMsg("filterData-6 #records:" + records.length);

	    if(this.uniqueFields && this.uniqueFields.length>0) {
		let selected= this.jq('uniquefields').val();
		if(selected.length>0) {
		    let fields=this.uniqueFields.filter(f=>{
			return selected.includes(f.getId())
		    });
		    fields.forEach(ufield=>{
			records = this.findUnique(records, ufield);
		    });
		}
	    }

	    if(this.getProperty("uniqueField")) {
		let ufield =  this.getFieldById(null, this.getProperty("uniqueField"));
		records = this.findUnique(records, ufield);
	    }

	    


	    //	    let t2=  new Date();
	    //	    Utils.displayTimes("filterData",[t1,t2]);
	    records = this.sortRecords(records);

	    this.recordToIndex = {};
	    this.indexToRecord = {};
	    for(let i=0;i<records.length;i++) {
		let record = records[i];
		this.recordToIndex[record.getId()] = i;
		this.indexToRecord[i] = record;
	    }


	    let convertPost = this.getProperty("convertDataPost");
	    if(convertPost) {
		let pointData = this.getData();
		let newPointData = new  PointData("pointdata", pointData.getRecordFields(), records,null,{parent:pointData});
		this.pointData =  new CsvUtil().process(this, newPointData, convertPost);
		records = this.pointData.getRecords();
//		this.logMsg("post:" + this.pointData.getRecordFields());
	    }
	    if(debug)
		this.logMsg("filtered:" + records.length);
	    this.jq(ID_FILTER_COUNT).html("Count: " + records.length);
	    this.filteredRecords = records;
	    if(this.getSelectFirst()) {
		this.propagateEventRecordSelection({record:records[0]});
	    } else if(this.getSelectLast()) {
		this.propagateEventRecordSelection({record:records[records.length-1]});
	    }
	    if(opts.recordOk) {
		records=records.filter(record=>{
		    if(opts.recordOk(record)) return record;
		    return null;});
	    }

	    if(this.getShowRecordPager()) {
		let number = +this.getRecordPagerNumber();
		if(this.rowStartIndex>=records.length) {
		    this.rowStartIndex=Math.max(0, records.length-(records.length%number)-1);
		}
		let tmp = [];
		let rowIdx;
		let cnt = 0;
		for (rowIdx = this.rowStartIndex; (rowIdx < records.length) && (cnt<number); rowIdx++) {
		    cnt++;

		    let record = records[rowIdx];
		    tmp.push(record);
		}
		let header = "";
		if(this.rowStartIndex>0) {
		    header += HU.span([ID,this.domId(ID_PREV)],"Previous")+" ";
		}

		if(rowIdx<records.length) {
		    header += HU.span([ID,this.domId(ID_NEXT)],"Next") +" ";
		}
		header  += HU.span([ID,this.domId(ID_PREVNEXT_LABEL)]);
		if(header!="") {
		    header = HU.div([ATTR_STYLE,HU.css('margin-right','10px', "display","inline-block")],header);
		    this.jq(ID_HEADER2_PREFIX).html(header);
		    this.jq(ID_HEADER2).css("text-align","left");
		}
		if(number<records.length) {
		    this.jq(ID_PREVNEXT_LABEL).html("Showing " + (this.rowStartIndex+1) +" - " +(this.rowStartIndex+cnt) +
						    " of " + records.length +" " + this.getNoun(""));
		}
		this.jq(ID_PREV).button().click(()=>{
		    this.rowStartIndex-=  +this.getRecordPagerNumber();
		    if(this.rowStartIndex<0) this.rowStartIndex=0;
		    this.forceUpdateUI();
		});
		this.jq(ID_NEXT).button().click(()=>{
		    let num = +this.getRecordPagerNumber();
		    this.rowStartIndex+=num;
		    this.forceUpdateUI();
		});
		records = tmp;
	    }


	    if(this.animationControl) {
		this.animationControl.applyLabelTemplate(records);
	    }


	    if(debug)   this.logMsg("filterData-final #records:" + records.length);

            return this.handleResult("filterData",records);
        },
	//TODO: this will support a handler pattern that allows for insertion
	//of custom functionality
	handleResult: function(type,data) {
	    return data;
	},
	getBinnedRecords: function(record) {
	    if(this.binRecordToRecords)
		return this.binRecordToRecords[record.getId()].records;
	    return record.parentRecords;
	},
        canDoGroupBy: function() {
            return false;
        },
        canDoMultiFields: function() {
            return true;
        },
        useChartableFields: function() {
            return false;
        },
        getFieldsToSelect: function(pointData) {
	    if(this.convertedFields) return this.convertedFields;
            if (this.useChartableFields())
                return pointData.getChartableFields(this);
            return pointData.getRecordFields();
        },
        getGet: function() {
            return "getRamaddaDisplay('" + this.getId() + "')";
        },
	assembleWikiText: function(type) {
	    let wiki =  "";
	    if(window.globalDisplayProperties) {
		for(key in window.globalDisplayProperties) {
		    wiki += '{{displayProperty name="' + key +'" value="' + window.globalDisplayProperties[key]+'"}}\n';

		}
	    }
            wiki += this.getWikiText();
            for (let i = 0; i < this.displays.length; i++) {
                let display = this.displays[i];
                if (display.getIsLayoutFixed()) {
                    continue;
                }
                wiki += display.getWikiText();
            }
	    return wiki;
	},
        showWikiText: function(type) {
	    let wiki =  this.assembleWikiText();
	    HtmlUtils.setPopupObject(HtmlUtils.getTooltip());
	    wiki = wiki.replace(/</g,"&lt;").replace(/>/g,"&gt;");
	    wiki = HU.pre([ATTR_STYLE,HU.css("max-width","500px","max-height","400px","overflow-x","auto","overflow-y","auto")], wiki);
	    this.showDialog(wiki);
	},
        copyWikiText: function(type) {
	    Utils.copyText(this.assembleWikiText());
	    alert("Wiki text has been copied to the clipboard");
	},
        publish: function(type) {
            if (type == null) type = "wikipage";
            let args = [];
            let name = prompt("Name", "");
            if (name == null) return;
            args.push("name");
            args.push(name);

            args.push("type");
            args.push(type);


            let desc = "";
            //                let desc = prompt("Description", "");
            //                if(desc == null) return;

            let wiki = "";
            if (type == "wikipage") {
                wiki += "+section label=\"{{name}}\"\n${extra}\n";
            } else if (type == "blogentry") {
                wiki = "<wiki>\n";
            }
            wiki += desc;
	    wiki += this.assembleWikiText();
            if (type == "wikipage") {
                wiki += "-section\n\n";
            } else if (type == "blogentry") {}
            let from = "";
            let entries = this.getChildEntries();
            for (let i = 0; i < entries.length; i++) {
                let entry = entries[i];
                from += entry.getId() + ",";
            }

            if (entries.length > 0) {
                args.push("entries");
                args.push(from);
            }

            if (type == "media_photoalbum") {
                wiki = "";
            }

            args.push("description_encoded");
            args.push(window.btoa(wiki));
            let url = HU.getUrl(RamaddaUtil.getUrl("/entry/publish"), args);
            window.open(url, '_blank');
        },
        getChildEntries: function(includeFixed) {
            let seen = {};
            let allEntries = [];
            for (let i = 0; i < this.displays.length; i++) {
                let display = this.displays[i];
                if (!includeFixed && display.getIsLayoutFixed()) {
                    continue;
                }
                if (display.getEntries) {
                    let entries = display.getEntries();
                    if (entries) {
                        for (let entryIdx = 0; entryIdx < entries.length; entryIdx++) {
                            if (seen[entries[entryIdx].getId()] != null) {
                                continue;
                            }
                            seen[entries[entryIdx].getId()] = true;
                            allEntries.push(entries[entryIdx]);
                        }
                    }
                }
            }
            return allEntries;
        },
        copyDisplayedEntries: function() {
            let allEntries = [];
            for (let i = 0; i < this.displays.length; i++) {
                let display = this.displays[i];
                if (display.getIsLayoutFixed()) {
                    continue;
                }
                if (display.getEntries) {
                    let entries = display.getEntries();
                    if (entries) {
                        for (let entryIdx = 0; entryIdx < entries.length; entryIdx++) {
                            allEntries.push(entries[entryIdx]);
                        }
                    }
                }
            }
            return this.copyEntries(allEntries);
        },
        defineWikiAttributes: function(list) {
            for (let i = 0; i < list.length; i++) {
                if (this.wikiAttrs.indexOf(list[i]) < 0) {
                    this.wikiAttrs.push(list[i]);
                }
            }
        },
        getWikiAttributes: function(attrs) {
            for (let i = 0; i < this.wikiAttrs.length; i++) {
                let v = this[this.wikiAttrs[i]];
                if (Utils.isDefined(v)) {
                    attrs.push(this.wikiAttrs[i]);
                    attrs.push(v);
                }
            }
        },
        getWikiText: function() {
            let attrs = [
			 "layoutHere", "false",
			 "type", this.type,
			 "column", this.getColumn(),
			 "row", this.getRow()
			];
	    if(this.getProperty("entryId")) {
		attrs.push("entry");
		attrs.push(this.getProperty("entryId"));
	    }
            this.getWikiAttributes(attrs);
            let entryId = null;
            if (this.getEntries) {
                let entries = this.getEntries();
                if (entries && entries.length > 0) {
                    entryId = entries[0].getId();
                }
            }
            if (!entryId) {
                entryId = this.entryId;
            }
            if (entryId) {
                attrs.push("entry");
                attrs.push(entryId);
            }
            let wiki = "{{display " + HU.attrs(attrs) + "}}\n\n"

            return wiki;
        },
        copyEntries: function(entries) {
            let allEntries = [];
            let seen = {};
            for (let entryIdx = 0; entryIdx < entries.length; entryIdx++) {
                let entry = entries[entryIdx];
                if (seen[entry.getId()] != null) continue;
                seen[entry.getId()] = entry;
                allEntries.push(entry);
            }
            let from = "";
            for (let i = 0; i < allEntries.length; i++) {
                let entry = allEntries[i];
                from += entry.getId() + ",";
            }


            let url = RamaddaUtil.getUrl("/entry/copy?action.force=copy&from=" + from);
            window.open(url, '_blank');

        },
        entryHtmlHasBeenDisplayed: async function(entry) {
            if (entry.getIsGroup() /* && !entry.isRemote*/ ) {
                let theDisplay = this;
                let callback = function(entries) {
                    let html = HU.open(TAG_OL, [ATTR_CLASS, "display-entrylist-list", ATTR_ID, theDisplay.getDomId(ID_LIST)]);
                    html += theDisplay.getEntriesTree(entries);
                    html += HU.close(TAG_OL);
                    theDisplay.jq(ID_GROUP_CONTENTS + entry.getIdForDom()).html(html);
                    theDisplay.addEntrySelect();
                };
                await entry.getChildrenEntries(callback);
            }
        },
        getEntryHtml: function(entry, props) {
            let dfltProps = {
                showHeader: true,
                headerRight: false,
                showDetails: this.getShowDetails(),
		showImage:this.getProperty("showEntryImage")
            };
            $.extend(dfltProps, props);

            props = dfltProps;
            let menu = this.getEntryMenuButton(entry);
            let html = "";
            if (props.showHeader) {
                let left = menu + SPACE + entry.getLink(null, true, ["target","_entries"]);
                if (props.headerRight) html += HU.leftRight(left, props.headerRight);
                else html += left;
            }

            let divid = HU.getUniqueId("entry_");
            html += HU.div([ID, divid], "");
            let metadata = entry.getMetadata();
	    //Don't this now since this gets shown in the embed details
	    if(dfltProps.showImage) {
		if (entry.isImage()) {
                    let img = HU.tag(TAG_IMG, ["src", entry.getImageUrl(), /*ATTR_WIDTH,"100%",*/
					       ATTR_CLASS, "display-entry-image"
					      ]);

                    html += HU.href(entry.getResourceUrl(), img,["download",null]) + "<br>";
		} else {
                    for (let i = 0; i < metadata.length; i++) {
			if (metadata[i].type == "content.thumbnail") {
                            let image = metadata[i].value.attr1;
                            let url;
                            if (image.indexOf("http") == 0) {
				url = image;
                            } else {
				url = RamaddaUtil.getUrl("/metadata/view/" + image + "?element=1&entryid=" + entry.getId() + "&metadata_id=" + metadata[i].id + "&thumbnail=false");
                            }
                            html += HU.image(url, [ATTR_CLASS, "display-entry-thumbnail"]);
			}
                    }
		}
	    }
            if (entry.getIsGroup() /* && !entry.isRemote*/ ) {
                html += HU.div([ATTR_ID, this.getDomId(ID_GROUP_CONTENTS + entry.getIdForDom())], "" /*this.getWaitImage()*/ );
            }


            html += HU.formTable();

            if (props.showDetails) {
                if (entry.url) {
                    html += HU.formEntry("URL:", HU.href(entry.url, entry.url));
                }

                if (entry.remoteUrl) {
                    html += HU.formEntry("URL:", HU.href(entry.remoteUrl, entry.remoteUrl));
                }
                if (entry.remoteRepository) {
                    html += HU.formEntry("From:", HU.href(entry.remoteRepository.url, entry.remoteRepository.name));
                }
            }

            let columns = entry.getAttributes();

            if (entry.getFilesize() > 0) {
		let link =  entry.getFilename() + " " +HU.getIconImage('fas fa-download');
                html += HU.formEntry("File:", 
				     HU.href(entry.getResourceUrl(), link,["download",null]) + " " +
				     entry.getFormattedFilesize());
            }

            for (let colIdx = 0; colIdx < columns.length; colIdx++) {
                let column = columns[colIdx];
                let columnValue = column.value;
                if (column.getCanShow && !column.getCanShow()) {
                    continue;
                }
                if (Utils.isFalse(column.canshow)) {
                    continue;
                }

                if (column.isUrl && column.isUrl()) {
                    let tmp = "";
                    let toks = columnValue.split("\n");
                    for (let i = 0; i < toks.length; i++) {
                        let url = toks[i].trim();
                        if (url.length == 0) continue;
                        tmp += HU.href(url, url);
                        tmp += "<br>";
                    }
                    columnValue = tmp;
                }
                html += HU.formEntry(column.label + ":", columnValue);
            }

            html += HU.close(TAG_TABLE);
            return html;
        },

        getEntriesTree: function(entries, props) {
            if (!props) props = {};
	    let columns = this.getProperty('entryColumns', null);
	    let showSnippet = this.getProperty('showSnippetInList');
	    let metadataDisplay = RamaddaUtil.makeMetadataDisplay(this.getProperty('metadataDisplay'));
	    let mainMetadataDisplay = RamaddaUtil.makeMetadataDisplay(this.getProperty('mainMetadataDisplay'));	    
	    let nameStyle = this.getProperty('nameStyle','font-size:120%;color:#2178B5;');
	    let showIcon = this.getProperty('showIcon',true);
	    let showToggle = this.getProperty('showToggle',true);
	    let showThumbnail = this.getProperty('showThumbnail',false);	    	    
	    let showEntryType = this.getProperty('showEntryType',false);	    	    
	    let placeholderImage = this.getProperty('placeholderImage',RamaddaUtils.getCdnUrl('/images/placeholder.png'));
	    
            if (columns != null) {
                let columnNames = this.getProperty('columnNames', null);
                if (columnNames != null) {
                    columnNames = columnNames.split(',');
                }
                columns = columns.split(',');
                let ids = [];
                let names = [];
                for (let i = 0; i < columns.length; i++) {
                    let toks = columns[i].split(':');
                    let id = null,
                        name = null;
                    if (toks.length > 1) {
                        if (toks[0] == 'property') {
                            name = 'property';
                            id = columns[i];
                        } else {
                            id = toks[0];
                            name = toks[1];
                        }
                    } else {
                        id = columns[i];
                        name = id;
                    }
                    ids.push(id);
                    names.push(name);
                }
                columns = ids;
                if (columnNames == null) {
                    columnNames = names;
                }
                return this.getEntriesTable(entries, columns, columnNames);
            }
            let suffix = props.suffix;
            let domIdSuffix = '';
            if (!suffix) {
                suffix = 'null';
            } else {
                domIdSuffix = suffix;
                suffix = "'" + suffix + "'";
            }

            let handler = getHandler(props.handlerId);
            let showIndex = props.showIndex;
            let html = "";
            let rowClass = "entryrow_" + this.getId();
            let even = true;
            if (this.entriesMap == null)
                this.entriesMap = {};
	    let doWorkbench = this.getProperty("doWorkbench");
            for (let i = 0; i < entries.length; i++) {
                even = !even;
                let entry = entries[i];
		if(entry.displayHtml) {
		    html+=entry.displayHtml;
		    continue;
		}

                this.entriesMap[entry.getId()] = entry;
                let toolbar = this.makeEntryToolbar(entry, handler, props.handlerId);
                let entryMenuButton = doWorkbench?this.getEntryMenuButton(entry):"";

                let entryName = entry.getDisplayName(true);
                if (entryName.length > 100) {
                    entryName = entryName.substring(0, 99) + "...";
                }
                let icon = showIcon?entry.getIconImage([ATTR_TITLE, "View entry"]):'';
		entryName = HU.span([ATTR_STYLE,nameStyle],entryName);
                let entryIdForDom = entry.getIdForDom() + domIdSuffix;
                let entryId = entry.getId();
                let arrow = HU.image(icon_tree_closed, [ATTR_BORDER, "0",
							"tree-open", "false",
							ATTR_ID,
							this.getDomId(ID_TREE_LINK + entryIdForDom)
						       ]);
                let toggleCall = this.getGet() + ".toggleEntryDetails(event, '" + entryId + "'," + suffix + ",'" + props.handlerId + "');";
                let toggleCall2 = showToggle?this.getGet() + ".entryHeaderClick(event, '" + entryId + "'," + suffix + "); ":'';
                let open = showToggle?HU.onClick(toggleCall, arrow):'';
                let extra = "";
                if (showIndex) {
                    extra = "#" + (i + 1) + " ";
                }
                if (handler && handler.getEntryPrefix) {
                    extra += handler.getEntryPrefix(props.handlerId, entry);
                }

		entryName = icon + " " + entryName;
                let link = showToggle?HU.tag(TAG_A, ["target","_entries",ATTR_HREF, entry.getEntryUrl()], entryName):entryName;
		let main = entryMenuButton + " " + open + " " + extra + link;
                let left = HU.div([ATTR_CLASS, "display-entrylist-name"], main);
		if(showEntryType) {
		    left =  HU.leftRightTable(left,HU.span([ATTR_STYLE,'font-style:italic;'],entry.getTypeName()));
		}

		if(mainMetadataDisplay && mainMetadataDisplay.length) {
		    let mtd = RamaddaUtil.formatMetadata(entry,mainMetadataDisplay,{doBigText:false,wrapInDiv:false});
		    if(Utils.stringDefined(mtd)) {
			left +=HU.div([],mtd);
		    }
		}

		if(showThumbnail) {
		    let thumb = entry.getThumbnail();
		    if(!thumb) thumb = placeholderImage;
		    if(thumb) {
			thumb = HU.image(thumb,[ATTR_WIDTH,'80px',ATTR_STYLE,'margin-right:5px;']);
			thumb = HU.div([ATTR_STYLE,HU.css('max-height','100px','overflow-y','auto')], thumb);
			left = HU.table([ATTR_WIDTH,'100%'],
					HU.tr([ATTR_VALIGN,'top'],
					      HU.td([ATTR_WIDTH,'80px'],thumb +HU.td(left))));
						    
		    }
		}

		if(!showToggle) {
		    left= HU.href(entry.getEntryUrl(),left);
		}
		let snippet = entry.getSnippet()??'';
		if(showSnippet && Utils.stringDefined(snippet)) {
		    snippet = HU.div([ATTR_CLASS, "display-entrylist-details-snippet", ATTR_ID, this.getDomId(ID_DETAILS_SNIPPET + entryIdForDom)],snippet);
		    left+=snippet;
		    snippet = "";
		}
		let extraDetails ='';
		if(metadataDisplay && metadataDisplay.length) {
		    let mtd = RamaddaUtil.formatMetadata(entry,metadataDisplay);
		    if(Utils.stringDefined(mtd)) {
			extraDetails +=HU.div([],mtd);
		    }
		}

		let inner = HU.div([ATTR_CLASS, "display-entrylist-details-inner", ATTR_ID, this.getDomId(ID_DETAILS_INNER + entryIdForDom)], "");
                let details = HU.div([ATTR_ID, this.getDomId(ID_DETAILS + entryIdForDom), ATTR_CLASS, "display-entrylist-details"], 
				     HU.div([ATTR_CLASS, "display-entrylist-details-ancestors", ATTR_ID, this.getDomId(ID_DETAILS_ANCESTORS + entryIdForDom)], "") +
				     snippet +
				     HU.div([ATTR_CLASS, "display-entrylist-details-tags", ATTR_ID, this.getDomId(ID_DETAILS_TAGS + entryIdForDom)], "")+
				     extraDetails+
				     inner

				    );

                //                    console.log("details:" + details);
                let line;
                if (doWorkbench && this.getProperty("showToolbar", true)) {
                    line = HU.leftCenterRight(left, "", toolbar, "80%", "1%", "19%");
                } else {
                    line = left;
                }
                //                    line = HU.leftRight(left,toolbar,"60%","30%");
                let mainLine = HU.div(["onclick", toggleCall2, ATTR_ID, this.getDomId(ID_DETAILS_MAIN + entryIdForDom),  ATTR_ENTRYID, entryId], line);
		//mainline
                mainLine = HU.div([ATTR_CLASS, "display-entrylist-entry-main" + " " + "entry-main-display-entrylist-" + (even ? "even" : "odd"),
				   ATTR_ENTRYID, entryId], mainLine);
                line = HU.div([CLASS, (even ? "ramadda-row-even" : "ramadda-row-odd"), ATTR_ID, this.getDomId("entryinner_" + entryIdForDom)], mainLine + details);


                html += HU.div([ATTR_ID,
				this.getDomId("entry_" + entryIdForDom),
				ATTR_ENTRYID, entryId, ATTR_CLASS, "display-entrylist-entry" + rowClass
			       ], line);
                html += "\n";
            }
            return html;
        },
        addEntrySelect: function() {
            let theDisplay = this;
            let entryRows = $("#" + this.getDomId(ID_DISPLAY_CONTENTS) + "  ." + this.getClass("entry-main"));

            entryRows.unbind();
            entryRows.mouseover(async function(event) {
                //TOOLBAR
                let entryId = $(this).attr(ATTR_ENTRYID);
                let entry;
                await theDisplay.getEntry(entryId, e => {
                    entry = e
                });
                if (!entry) {
                    console.log("no entry:" + entryId);
                    return;
                }
                theDisplay.propagateEvent("entryMouseover", {
                    entry: entry
                });


                if (true) return;
                let domEntryId = Utils.cleanId(entryId);
                let toolbarId = theDisplay.getEntryToolbarId(domEntryId);

                let toolbar = $("#" + toolbarId);
                toolbar.show();
                let myalign = 'right top+1';
                let atalign = 'right top';
                let srcId = theDisplay.getDomId(ID_DETAILS_MAIN + domEntryId);
                toolbar.position({
                    of: $("#" + srcId),
                    my: myalign,
                    at: atalign,
                    collision: "none none"
                });

            });
            entryRows.mouseout(async function(event) {
                let entryId = $(this).attr(ATTR_ENTRYID);
                let entry;
                await theDisplay.getEntry(entryId, e => {
                    entry = e
                });
                if (!entry) return;
                theDisplay.propagateEvent("entryMouseout", {
                    entry: entry
                });
                let domEntryId = Utils.cleanId(entryId);
                let toolbarId = theDisplay.getEntryToolbarId(entryId);
                let toolbar = $("#" + toolbarId);
                //TOOLBAR                        toolbar.hide();
            });

            if (this.madeList) {
                //                    this.jq(ID_LIST).selectable( "destroy" );
            }
            this.madeList = true;
            if (false) {
                this.jq(ID_LIST).selectable({
                    //                        delay: 0,
                    //                        filter: 'li',
                    cancel: 'a',
                    selected: async function(event, ui) {
                        let entryId = ui.selected.getAttribute(ATTR_ENTRYID);
                        theDisplay.toggleEntryDetails(event, entryId);
                        if (true) return;

                        theDisplay.hideEntryDetails(entryId);
                        let entry;
                        await this.getEntry(entryId, e => {
                            entry = e
                        });
                        if (entry == null) return;

                        let zoom = null;
                        if (event.shiftKey) {
                            zoom = {
                                zoomIn: true
                            };
                        }
                        theDisplay.selectedEntries.push(entry);
                        theDisplay.propagateEvent(DisplayEvent.entrySelection, {
                            entry: entry,
                            selected: true,
                            zoom: zoom
                        });
                        theDisplay.lastSelectedEntry = entry;
                    },
                    unselected: async function(event, ui) {
                        if (true) return;
                        let entryId = ui.unselected.getAttribute(ATTR_ENTRYID);
                        let entry;
                        await this.getEntry(entryId, e => {
                            entry = e
                        });
                        let index = theDisplay.selectedEntries.indexOf(entry);
                        //                            console.log("remove:" +  index + " " + theDisplay.selectedEntries);
                        if (index > -1) {
                            theDisplay.selectedEntries.splice(index, 1);
                            theDisplay.propagateEvent(DisplayEvent.entrySelection, {
                                entry: entry,
                                selected: false
                            });
                        }
                    },

                });
            }

        },
        getEntriesTable: function(entries, columns, columnNames) {
            if (this.entriesMap == null)
                this.entriesMap = {};
            let columnWidths = this.getProperty("columnWidths", null);
            if (columnWidths != null) {
                columnWidths = columnWidths.split(",");
            }
            let html = HU.open(TAG_TABLE, [ATTR_WIDTH, "100%", "cellpadding", "0", "cellspacing", "0"]);
            html += HU.open(TAG_TR, ["valign", "top"]);
            for (let i = 0; i < columnNames.length; i++) {
                html += HU.td([ATTR_ALIGN, "center", ATTR_CLASS, "display-entrytable-header"], columnNames[i]);
            }
            html += HU.close(TAG_TR);

            for (let i = 0; i < entries.length; i++) {
                html += HU.open(TAG_TR, ["valign", "top"]);
                let entry = entries[i];
                this.entriesMap[entry.getId()] = entry;
                for (let j = 0; j < columns.length; j++) {
                    let columnWidth = null;
                    if (columnWidths != null) {
                        columnWidth = columnWidths[j];
                    }
                    let column = columns[j];
                    let value = "";
                    if (column == "name") {
                        value = HU.tag(TAG_A, [ATTR_HREF, entry.getEntryUrl()], entry.getName());
                    } else if (column.match(".*property:.*")) {
                        let type = column.substring("property:".length);
                        let metadata = entry.getMetadata();
                        value = "";
                        for (let j = 0; j < metadata.length; j++) {
                            let m = metadata[j];
                            if (m.type == type) {
                                if (value != "") {
                                    value += "<br>";
                                }
                                value += m.value.attr1;
                            }
                        }
                    } else if (column == "description") {
                        value = entry.getDescription();
                    } else if (column == "date") {
                        value = entry.ymd;
                        if (value == null) {
                            value = entry.startDate;
                        }

                    } else {
                        value = entry.getAttributeValue(column);
                    }
                    let attrs = [ATTR_CLASS, "display-entrytable-cell"];
                    if (columnWidth != null) {
                        attrs.push(ATTR_WIDTH);
                        attrs.push(columnWidth);
                    }

                    html += HU.td(attrs, value);
                }
                html += HU.close(TAG_TR);
            }
            html += HU.close(TAG_TABLE);
            return html;
        },

        makeEntryToolbar: function(entry, handler, handlerId) {
            let get = this.getGet();
            let toolbarItems = [];
	    let props = "{showMenu:true,showTitle:true}";
            //                 toolbarItems.push(HU.tag(TAG_A, [ATTR_HREF, entry.getEntryUrl(),"target","_"], 
            //                                                HU.image(RamaddaUtil.getCdnUrl("/icons/application-home.png"),["border",0,ATTR_TITLE,"View Entry"])));
            if (entry.getType().getId() == "type_wms_layer") {
                toolbarItems.push(HU.tag(TAG_A, ["onclick", get + ".addMapLayer(" + HU.sqt(entry.getId()) + ");"],
					 HU.image(RamaddaUtil.getCdnUrl("/icons/map.png"), ["border", 0, ATTR_TITLE, "Add Map Layer"])));

            }
            if (entry.getType().getId() == "geo_shapefile" || entry.getType().getId() == "geo_geojson") {
                toolbarItems.push(HU.tag(TAG_A, ["onclick", get + ".addMapLayer(" + HU.sqt(entry.getId()) + ");"],
					 HU.image(RamaddaUtil.getCdnUrl("/icons/map.png"), ["border", 0, ATTR_TITLE, "Add Map Layer"])));

            }

            let jsonUrl = this.getPointUrl(entry);
            if (jsonUrl != null) {
                jsonUrl = jsonUrl.replace(/\'/g, "_");
                toolbarItems.push(HU.tag(TAG_A, ["onclick", get + ".createDisplay(" + HU.sqt(entry.getFullId()) + "," +
						 HU.sqt("table") + "," + HU.sqt(jsonUrl) + "," + props+");"
						],
					 HU.getIconImage("fa-table", [ATTR_TITLE, "Create Tabular Display"])));

                let x;
                toolbarItems.push(x = HU.tag(TAG_A, ["onclick", get + ".createDisplay(" + HU.sqt(entry.getFullId()) + "," +
 						     HU.sqt("linechart") + "," + HU.sqt(jsonUrl) + "," + props +");"
						    ],
					     HU.getIconImage("fa-chart-line", [ATTR_TITLE, "Create Chart"])));
		//		console.log("X:" + x);
            }
            toolbarItems.push(HU.tag(TAG_A, ["onclick", get + ".createDisplay(" + HU.sqt(entry.getFullId()) + "," +
					     HU.sqt("entrydisplay") + "," + HU.sqt(jsonUrl) + "," + props +");"
					    ],
				     HU.getIconImage("fa-file", ["border", 0, ATTR_TITLE, "Show Entry"])));
            if (entry.getFilesize() > 0) {
                toolbarItems.push(HU.tag(TAG_A, [ATTR_HREF, entry.getResourceUrl(),"download",null],
					 HU.image(RamaddaUtil.getCdnUrl("/icons/download.png"), ["border", 0, ATTR_TITLE, "Download (" + entry.getFormattedFilesize() + ")"])));

            }


            let entryMenuButton = this.getEntryMenuButton(entry);
            let tmp = [];



            if (handler && handler.addToToolbar) {
                handler.addToToolbar(handlerId, entry, toolbarItems);
            }

            for (let i = 0; i < toolbarItems.length; i++) {
                tmp.push(HU.div([ATTR_CLASS, "display-entry-toolbar-item"], toolbarItems[i]));
            }
            toolbarItems = tmp;
            return HU.div([ATTR_CLASS, "display-entry-toolbar", ATTR_ID,
			   this.getEntryToolbarId(entry.getIdForDom())
			  ],
			  HU.join(toolbarItems, ""));
        },
        getEntryToolbarId: function(entryId) {
            let id = entryId.replace(/:/g, "_").replace(/\//g,"_").replace(/[\(\)]/g,"_");
            id = id.replace(/=/g, "_");
            return this.getDomId(ID_TOOLBAR + "_" + id);
        },

        hideEntryDetails: function(entryId) {
            //                let popupId = "#"+ this.getDomId(ID_DETAILS + entryId);
            //                $(popupId).hide();
            //                this.currentPopupEntry = null;
        },
        entryHeaderClick: function(event, entryId, suffix) {
            let target = event.target;
            //A hack to see if this was the div clicked on or a link in the div
            if (target.outerHTML) {
                if (target.outerHTML.indexOf("<div") != 0) {
                    return;
                }
            }
            this.toggleEntryDetails(event, entryId);
        },
	makeEntryTags:function(entry,groupThem,prefix,metadataMap) {
	    prefix = prefix||"";
	    let metadata = "";
	    let map = {};
	    let list = [];
	    entry.getMetadata().forEach(m=>{
		
		//Check for exclusions
		if(["content.pagestyle", "content.pagetemplate","content.thumbnail","content.attachment"].includes(m.type)) return;
		if(m.type.startsWith("map")) return;
		if(m.type.startsWith("spatial")) return;		
                let tt = m.label+": " + m.value.attr1;
                let label =String(m.value.attr1);
		if(m.type=="property") {
		    tt +=":" + m.value.attr2;
		    label +=":" + m.value.attr2;
		}
		if(label.length>20) label = label.substring(0,19) +"...";
		label = prefix +label;
		let id = Utils.getUniqueId("metadata_");
		let tag = HU.div(["metadata-type",m.type,"metadata-value", m.value.attr1,ID,id,
				  ATTR_CLASS,"display-search-tag",ATTR_TITLE, tt,
				  ATTR_STYLE, HU.css("background", getMetadataColor(m.type))],label);
		if(!groupThem)
		    metadata+= tag;
		else {
		    if(!map[m.type]) {
			map[m.type] = [];
			list.push(m);
		    }
		    map[m.type].push(tag);
		}
		if(metadataMap)
		    metadataMap[id] = m;
	    });
	    if(groupThem) {
		list.forEach(m=>{
		    metadata+=m.label +": " +map[m.type].join(" ");
		    metadata+="<br>";
		});
	    }

	    return metadata;
	},
	addSearchToTags: function() {
	    return true;
	},
	typeSearchEnabled:function() {
	    return true;
	},

        toggleEntryDetails: async function(event, entryId, suffix, handlerId, entry) {
	    if(!entry) {
		await this.getEntry(entryId, e => {
		    this.toggleEntryDetails(event, entryId, suffix, handlerId, e);
		});
		return;
	    }


//            console.log("toggleEntryDetails:" + entry.getName() +" " + entry.getId());
            if (suffix == null) suffix = "";
            let link = this.jq(ID_TREE_LINK + entry.getIdForDom() + suffix);
            let id = ID_DETAILS + entry.getIdForDom() + suffix;
            let details = this.jq(id);
            if (event && event.shiftKey) {
                let id = Utils.cleanId(entryId);
                let line = this.jq(ID_DETAILS_MAIN + id);
                if (!this.selectedEntriesFromTree) {
                    this.selectedEntriesFromTree = {};
                }
                let selected = line.attr("ramadda-selected") == "true";
                if (selected) {
                    line.removeClass("display-entrylist-entry-main-selected");
                    line.attr("ramadda-selected", "false");
                    this.selectedEntriesFromTree[entry.getId()] = null;
                } else {
                    line.addClass("display-entrylist-entry-main-selected");
                    line.attr("ramadda-selected", "true");
                    this.selectedEntriesFromTree[entry.getId()] = entry;
                }
                this.propagateEvent(DisplayEvent.entrySelection, {
                    "entry": entry,
                    "selected": !selected
                });
                return;
            }

            let open = link.attr("tree-open") == "true";
            if (open) {
                link.attr("src", icon_tree_closed);
            } else {
                link.attr("src", icon_tree_open);
            }
            link.attr("tree-open", open ? "false" : "true");

	    let handleContent = ()=>{
		if (open) {
                    details.hide();
		} else {
                    details.show();
		}
		if (event && event.stopPropagation) {
                    event.stopPropagation();
		}
	    }

            let _this = this;
            let hereBefore = details.attr("has-content") != null;
            details.attr("has-content", "true");
            if (hereBefore) {
		handleContent();
		return;
            } 
	    let detailsInner = this.jq(ID_DETAILS_INNER + entry.getIdForDom() + suffix);
	    let embedWiki=	(!entry.isRemote)?entry.getEmbedWikiText():null;

            if (!embedWiki && !entry.isSynth() && entry.getIsGroup() /* && !entry.isRemote*/ ) {
                detailsInner.html(HU.image(icon_progress));
                let callback = function(entries) {
                    _this.displayChildren(entry, entries, suffix, handlerId);
                };
                let entries = entry.getChildrenEntries(callback);
            } else {
		let details = this.getEntryHtml(entry, {
                    showHeader: false
                })
		let uid;
		if(Utils.stringDefined(embedWiki)) {
		    uid =this.getUniqueId("details");
		    details+=HU.div([ATTR_ID,uid,
				     ATTR_CLASS,'ramadda-button ramadda-clickable'],
				    'Details');
		    details+=HU.div([ATTR_ID,uid+'_contents',ATTR_CLASS,'display-entry-embed']);
		}
                detailsInner.html(details);

		if(uid) {
		    let open = false;
		    let shown=false;
		    jqid(uid).button().click(()=>{
			open=!open;
			let contents = jqid(uid+'_contents');
			if(!open) {
			    contents.hide();
			    return;
			}
			contents.show();
			if(shown) return;
			shown=true;
			contents.html(HU.div([ATTR_CLASS,'ramadda-image-loading']));
			this.wikify(embedWiki,entry.getId(),
				    (html)=>{
					this.addWikiHtml(contents, html);
				    },
				    (error)=>{
					contents.html(error);
				    });
		    });
		}
            }
	    handleContent();
	    let metadataMap  = {};
	    let prefix = entry.isSynth()?"":HU.getIconImage("fas fa-search") + SPACE;
	    if(!this.addSearchToTags()) prefix='';
	    if(!this.typeSearchEnabled()) prefix='';
	    let metadata = this.makeEntryTags(entry,false,prefix,metadataMap);
	    let bar = this.jq(ID_DETAILS_TAGS + entry.getIdForDom() + suffix);
	    let typeTag = $(HU.span([ATTR_CLASS,"display-search-tag"],prefix + "Type: " + entry.getType().getLabel())).appendTo(bar);
	    if(!entry.isSynth()) {
		typeTag.click(function() {
		    _this.typeTagClicked(entry.getType());
		});
	    }
	    let tags = $(metadata).appendTo(bar);
	    if(!entry.isSynth()) {
		tags.click(function() {
		    _this.metadataTagClicked(metadataMap[$(this).attr("id")]);
		});
	    }


	    if(!entry.isSynth() && this.getProperty("showEntryBreadcrumbs",true)) {
		let ancestorContent = "";
		let handleAncestor = ancestor=>{
		    if(!ancestor) {
			this.jq(ID_DETAILS_ANCESTORS + entry.getIdForDom() + suffix).html(ancestorContent);
		    } else {
			let href= ancestor.getLink(null, false,["target","_entries"]);
			if(ancestorContent!="")
			    href = href + HU.div([CLASS,"breadcrumb-delimiter"]);
			ancestorContent = href +  ancestorContent;
			ancestor.getParentEntry(handleAncestor);
		    }
		};
		entry.getParentEntry(handleAncestor);
	    }
        },
	addWikiHtml:function(container,html) {
	    let debug = true;
	    let js =[];
	    //Parse out any script tags 
	    let regexp = /<script *src=("|')?([^ "']+)("|')?.*?<\/script>/gs;
	    let array = [...html.matchAll(regexp)];
	    array.forEach(tuple=>{
		html = html.replace(tuple[0],'');
		let url = tuple[2];
		url = url.replace(/'/g,'');
		js.push(url);
	    });
	    //Run through any script tags and load them
	    let cb = ()=>{
		if(js.length==0 && js[0]==null) {
		    container.html(html);
		    return;
		}
		let url = js[0];
		js.splice(0,1);
		Utils.loadScript(url,cb);
	    };
	    cb();
	},


	metadataTagClicked:function(metadata) {
	},
	typeTagClicked:function(metadata) {
	},	
        getSelectedEntriesFromTree: function() {
            let selected = [];
            if (this.selectedEntriesFromTree) {
                for (let id in this.selectedEntriesFromTree) {
                    let entry = this.selectedEntriesFromTree[id];
                    if (entry != null) {
                        selected.push(entry);
                    }
                }
            }
            return selected;
        },
        displayChildren: function(entry, entries, suffix, handlerId) {
            if (!suffix) suffix = "";
            let detailsInner = this.jq(ID_DETAILS_INNER + entry.getIdForDom() + suffix);
            let details = this.getEntryHtml(entry, {
                showHeader: false,
		showImage:entries.length==0
            });
            if (entries.length == 0) {
                detailsInner.html(details);
            } else {
                let entriesHtml = details;
                if (this.getProperty('showDetailsForGroup')) {
                    entriesHtml += details;
                }
                entriesHtml += this.getEntriesTree(entries, {
                    handlerId: handlerId
                });
                detailsInner.html(entriesHtml);
                this.addEntrySelect();
            }
        },


        getEntryMenuButton: function(entry) {
            let menuButton = HU.onClick(this.getGet() + ".showEntryMenu(event, '" + entry.getId() + "');",
					HU.image(RamaddaUtil.getCdnUrl("/icons/menu.png"),
						 [ATTR_CLASS, "display-entry-toolbar-item", ATTR_ID, this.getDomId(ID_MENU_BUTTON + entry.getIdForDom())]));
            return menuButton;
        },
        setOriginalRamadda: function(e) {
            this.originalRamadda = e;
        },
        setRamadda: function(e) {
            this.ramadda = e;
        },	
        getRamadda: function() {
            if (this.ramadda != null) {
                return this.ramadda;
            }
            if (this.ramaddaBaseUrl != null) {
                this.ramadda = getRamadda(this.ramaddaBaseUrl);
                return this.ramadda;
            }
            return getGlobalRamadda();
        },
        getEntry: async function(entryId, callback) {
            if (this.entriesMap && this.entriesMap[entryId]) {
                return Utils.call(callback, this.entriesMap[entryId]);
            }
            let ramadda = this.getRamadda();
            let toks = entryId.split(",");
            if (toks.length == 2) {
                entryId = toks[1];
                ramadda = getRamadda(toks[0]);
            }
            let entry = null;
            if (this.entryList != null) {
                await this.entryList.getEntry(entryId, e => entry = e);
            }
            if (entry == null) {
                await ramadda.getEntry(entryId, e => entry = e);
            }

            if (entry == null) {
                await this.getRamadda().getEntry(entryId, e => entry = e);
            }
            return Utils.call(callback, entry);
        },
        addMapLayer: async function(entryId) {
            let entry;
            await this.getEntry(entryId, e => {
                entry = e
            });
            if (entry == null) {
                console.log("No entry:" + entryId);
                return;
            }
            this.getDisplayManager().addMapLayer(this, entry);
        },
        doit: function() {
            console.log("doit");
        },
        createDisplay: async function(entryId, displayType, jsonUrl, displayProps) {
            let entry;
            await this.getEntry(entryId, e => {
                entry = e
            });
            if (entry == null) {
                console.log("No entry:" + entryId);
                return null;
            }
            let props = {
                sourceEntry: entry,
                entryId: entry.getId(),
		//                showMenu: false,
		//                showTitle: false,
                showDetails: true,
                title: entry.getName(),
		layoutHere:false,
            };
            if (displayProps) {
		$.extend(props, displayProps);
	    }

            //TODO: figure out when to create data, check for grids, etc
            if (displayType != DISPLAY_ENTRYLIST) {
                if (jsonUrl == null) {
                    jsonUrl = this.getPointUrl(entry);
                }
                let pointDataProps = {
                    entry: entry,
                    entryId: entry.getId()
                };
                props.data = new PointData(entry.getName(), null, null, jsonUrl, pointDataProps);
            }
            if (this.lastDisplay != null) {
                props.column = this.lastDisplay.getColumn();
                props.row = this.lastDisplay.getRow();
            } else {
                props.column = this.getProperty("newColumn", this.getColumn());
                props.row = this.getProperty("newRow", this.getRow());
            }
            this.lastDisplay = this.getDisplayManager().createDisplay(displayType, props);
        },
        getPointUrl: function(entry) {
            //check if it has point data
            let service = entry.getService("points.json");
            if (service != null) {
                return service.url;
            }
            service = entry.getService("grid.point.json");
            if (service != null) {
                return service.url;
            }
            return null;
        },
        getEntryMenu: async function(entryId, callback) {
            let entry;
            await this.getEntry(entryId, e => {
                entry = e
            });
            if (entry == null) {
                return Utils.call(callback, "null entry");
            }

            let get = this.getGet();
            let menus = [];
            let fileMenuItems = [];
            let viewMenuItems = [];
            let newMenuItems = [];
            viewMenuItems.push(HU.tag(TAG_LI, [], HU.tag(TAG_A, ["href", entry.getEntryUrl(), "target", "_"], "View Entry")));
            if (entry.getFilesize() > 0) {
                fileMenuItems.push(HU.tag(TAG_LI, [], HU.tag(TAG_A, ["download",null, "href", entry.getResourceUrl()], "Download " + entry.getFilename() + " (" + entry.getFormattedFilesize() + ")")));
            }

            if (this.jsonUrl != null) {
                fileMenuItems.push(HU.tag(TAG_LI, [], "Data: " + HU.onClick(get + ".fetchUrl('json');", "JSON") +
					  HU.onClick(get + ".fetchUrl('csv');", "CSV")));
            }

	    let props = "{showMenu:true,showTitle:true}";
            let newMenu = "<a>New</a><ul>";
            newMenu += HU.tag(TAG_LI, [], HU.onClick(get + ".createDisplay('" + entry.getFullId() + "','entrydisplay',null,null," + props+");", "New Entry Display"));
            newMenuItems.push(HU.tag(TAG_LI, [], HU.onClick(get + ".createDisplay('" + entry.getFullId() + "','entrydisplay',null,null," + props+");", "New Entry Display")));

            //check if it has point data
            let pointUrl = this.getPointUrl(entry);
            //            console.log("entry:" + entry.getName() + " url:" + pointUrl);

            if (pointUrl != null) {
                let types = window.globalDisplayTypes;
                let catMap = {};
                if (types) {
                    for (let i = 0; i < types.length; i++) {
                        let type = types[i];

                        if (!type.requiresData || !type.forUser) continue;
                        if (!Utils.isDefined(catMap[type.category])) {
                            catMap[type.category] = "<li> <a>" + type.category + "</a><ul>\n";
                        }
                        pointUrl = pointUrl.replace(/\'/g, "_");
                        let call = get + ".createDisplay(" + HU.sqt(entry.getFullId()) + "," + HU.sqt(type.type) + "," + HU.sqt(pointUrl) + ",null," + props +");";
                        let li = HU.tag(TAG_LI, [], HU.tag(TAG_A, ["onclick", call], type.label));
                        catMap[type.category] += li + "\n";
                        newMenuItems.push(li);
                    }
                }

                for (a in catMap) {
                    newMenu += catMap[a] + "</li></ul>";
                }
            }


            if (fileMenuItems.length > 0)
                menus.push("<a>File</a>" + HU.tag(TAG_UL, [], HU.join(fileMenuItems)));
            if (viewMenuItems.length > 0)
                menus.push("<a>View</a>" + HU.tag(TAG_UL, [], HU.join(viewMenuItems)));
            if (newMenuItems.length > 0)
                menus.push(newMenu);

            let topMenus = "";
            for (let i = 0; i < menus.length; i++) {
                topMenus += HU.tag(TAG_LI, [], menus[i]);
            }

            let menu = HU.tag(TAG_UL, [ATTR_ID, this.getDomId(ID_MENU_INNER + entry.getIdForDom()), ATTR_CLASS, "sf-menu"],
			      topMenus);
            callback(menu);
        },
        showEntryMenu: async function(event, entryId) {
            let menu;
            await this.getEntryMenu(entryId, m => {
                menu = m
            });
            this.writeHtml(ID_MENU_OUTER, menu);
            let srcId = this.getDomId(ID_MENU_BUTTON + Utils.cleanId(entryId));
	    this.dialog = HU.makeDialog({content:menu,anchor:srcId,draggable:false,header:false});
            $("#" + this.getDomId(ID_MENU_INNER + Utils.cleanId(entryId))).superfish({
                speed: 'fast',
                delay: 300
            });
        },
        fetchUrl: function(as, url) {
            if (url == null) {
                url = this.jsonUrl;
            }
            url = this.getDisplayManager().getJsonUrl(url, this);
            if (url == null) return;
            if (as != null && as != "json") {
                url = url.replace("points.json", "points." + as);
            }
            window.open(url, '_blank');
        },
        getMenuItems: function(menuItems) {

        },
        getDisplayMenuSettings: function() {
            let get = "getRamaddaDisplay('" + this.getId() + "')";
            let moveRight = HU.onClick(get + ".moveDisplayRight();", "Right");
            let moveLeft = HU.onClick(get + ".moveDisplayLeft();", "Left");
            let moveTop = HU.onClick(get + ".moveDisplayTop();", "Top");
            let moveUp = HU.onClick(get + ".moveDisplayUp();", "Up");
            let moveDown = HU.onClick(get + ".moveDisplayDown();", "Down");


            let menu = HU.open(TABLE,[CLASS,'formtable']) +
                "<tr><td align=right><b>Move:</b></td><td>" + moveTop + " " + moveUp + " " + moveDown + " " + moveRight + " " + moveLeft + "</td></tr>" +
                "<tr><td align=right><b>Row:</b></td><td> " + HU.input("", this.getProperty("row", ""), ["size", "7", ATTR_ID, this.getDomId("row")]) + " &nbsp;&nbsp;<b>Col:</b> " + HU.input("", this.getProperty("column", ""), ["size", "7", ATTR_ID, this.getDomId("column")]) + "</td></tr>" +
                "<tr><td align=right><b>Width:</b></td><td> " + HU.input("", this.getProperty("width", ""), ["size", "7", ATTR_ID, this.getDomId("width")]) + "  " + "<b>Height:</b> " + HU.input("", this.getProperty("height", ""), ["size", "7", ATTR_ID, this.getDomId("height")]) + "</td></tr>" +
                "</table>";
            let tmp =
                HU.checkbox(this.getDomId("showtitle"), [], this.getProperty("showTitle")) + " Title  " +
                HU.checkbox(this.getDomId("showdetails"), [], this.getProperty("showDetails")) + " Details " +
                "&nbsp;&nbsp;&nbsp;" +
                HU.onClick(get + ".askSetTitle();", "Set Title");
            menu += HU.formTable() + HU.formEntry("Show:", tmp) + HU.close(TABLE);
            return menu;
        },
        loadInitialData: function() {
	    if(!this.getProperty("okToLoadData",true)) return;
	    if(this.getInlineDataSrc()) {
		this.getData();
		return;
	    }



            if (!this.needsData() || this.properties.theData == null) {
                return;
            }
            if (this.getProperty("latitude")) {
                this.properties.theData.lat = this.getProperty("latitude");
                this.properties.theData.lon = this.getProperty("longitude", "-105");
            }
            if (this.properties.theData.hasData()) {
                this.addData(this.properties.theData);
                return;
            }



            this.properties.theData.loadData(this);
        },
        getData: function() {
            if (!this.hasData()) {
		//Inline data
		if (this.properties.theData) {
		    return this.properties.theData;
		} 
		if(this.getInlineDataSrc()) {
		    this.addData(makeInlineData(this,this.getInlineDataSrc()));
		} else {
		    return null;
		}
	    }
            let dataList = this.dataCollection.getList();
            return dataList[0];
        },
        hasData: function() {
            if (this.dataCollection == null) return false;
            return this.dataCollection.hasData();
        },
        getCreatedInteractively: function() {
            return this.createdInteractively == true;
        },
        needsData: function() {
            return false;
        },
        askSetTitle: function() {
            let t = this.getTitle(false);
            let v = prompt(TITLE, t);
            if (v != null) {
                this.title = v;
                this.setProperty(ATTR_TITLE, v);
                this.setDisplayTitle(this.title);
            }
        },
        getShowDetails: function() {
            return this.getSelfProperty("showDetails", true);
        },
        setShowDetails: function(v) {
            this.showDetails = v;
            if (this.showDetails) {
                this.jq(ID_DETAILS).show();
            } else {
                this.jq(ID_DETAILS).hide();
            }
        },
        setShowTitle: function(v) {
	    if(v==="true") v = true;
	    else if(v==="false") v = true;	    
            this.setProperty("showTitle", v);
            if (v) {
                this.jq(ID_TITLE).show();
            } else {
                this.jq(ID_TITLE).hide();
            }
        },
        setDisplayProperty: function(key, value) {
            this.setProperty(key, value);
            $("#" + this.getDomId(key)).val(value);
        },
        deltaColumn: function(delta) {
            let column = parseInt(this.getProperty("column", 0));
            column += delta;
            if (column < 0) column = 0;
            this.setDisplayProperty("column", column);
            this.getLayoutManager().layoutChanged(this);
	    this.jq("col").val(column);
        },
        deltaRow: function(delta) {
            let row = parseInt(this.getProperty("row", 0));
	    if(isNaN(row)) row = 0;
            row += delta;
            if (row < 0) row = 0;
            this.setDisplayProperty("row", row);
            this.getLayoutManager().layoutChanged(this);
	    this.jq("row").val(row);
        },
        moveDisplayRight: function() {
            if (this.getLayoutManager().isLayoutColumns()) {
                this.deltaColumn(1);
            } else {
                this.getLayoutManager().moveDisplayDown(this);
            }
        },
        moveDisplayLeft: function() {
            if (this.getLayoutManager().isLayoutColumns()) {
                this.deltaColumn(-1);
            } else {
                this.getLayoutManager().moveDisplayUp(this);
            }
        },
        moveDisplayUp: function() {
            if (this.getLayoutManager().isLayoutRows()) {
                this.deltaRow(-1);
            } else {
                this.getLayoutManager().moveDisplayUp(this);
            }
        },
        moveDisplayDown: function() {
            if (this.getLayoutManager().isLayoutRows()) {
                this.deltaRow(1);
            } else {
                this.getLayoutManager().moveDisplayDown(this);
            }
        },
        moveDisplayTop: function() {
            this.getLayoutManager().moveDisplayTop(this);
        },
        getDialogContents: function(tabTitles, tabContents) {
	    this.getDisplayDialogContents(tabTitles, tabContents);
        },
	showFieldsInDialog: function() {
	    return false;
	},
        getDisplayDialogContents: function(tabTitles, tabContents) {

            if (this.hasData()) {
	        let html = HU.div([ATTR_ID, this.domId(ID_FIELDS)],"");
		tabTitles.push("Fields");
		tabContents.push(html);
	    } 

            let get = this.getGet();
            let menuItems = [];
            this.getMenuItems(menuItems);
            let form = "<form>";

            form += this.getDisplayMenuSettings();
            for (let i = 0; i < menuItems.length; i++) {
                form += HU.div([ATTR_CLASS, "display-menu-item"], menuItems[i]);
            }
            form += "</form>";
            tabTitles.push("Display"); 
            tabContents.push(form);
        },	
        checkLayout: function() {
	},
	isActiveDisplay: function() {
            return this.getMainDiv().length;
	},
	handleWindowResize:function() {
	    if(this.isActiveDisplay()) {
		this.displayData();
	    } else {
		//TODO: remove this display from the global list?
		//		this.logMsg("not active");
	    }
	},
        displayData: function() {},
        setDisplayReady: function() {
//	    console.log("setDisplayReady");
	    let callUpdate = !this.displayReady;
            this.displayReady = true;
	    if(callUpdate) {
		this.callUpdateUI({force:true});
	    }
        },
        getDisplayReady: function() {
            return this.displayReady;
        },
        pageHasLoaded: function() {
	    if(!this.displayReady) {
		this.setDisplayReady(true);
	    }
        },
	checkFinished: function() {
	    return false;
	},
	getIsFinished() {
	    return this.isFinished;
	},
	setIsFinished() {
//	    console.log(this.type+" isFinished");
	    this.isFinished = true;
	},	
	isDisplayFinished: function() {
	    if(this.checkFinished()) {
		return this.getIsFinished();
	    }
            if (!this.hasData()) {
		if(this.needsData()) {
		    return false;
		}
	    }
	    return true;
	},
	doFinalInitialization:function() {
	},
        initDisplay: function() {
	    if(this.inError) return
            this.createUI();
	    if(this.getAnimation().getEnabled()) {
		this.getAnimation().makeControls();
            }
            this.checkSearchBar();
	    this.callUpdateUI({force:true});
	    if(this.getProperty("reloadSeconds")) {
		this.runReload();
	    }
        },
	runReload: function() {
	    setTimeout(() =>{
		this.reloadData();
		this.runReload();
	    }, this.getProperty("reloadSeconds")*1000);
	},
        getMainDiv: function() {
	    //Don't check the parent for the targetDiv
	    let divId = this.getProperty("targetDiv",this.getProperty(PROP_DIVID,null,null,true),null,true);
	    return $("#" + divId); 
	},
        getGroupDiv: function() {
	    return $("#" + this.getProperty("groupDiv"));
	},	
        createUI: function() {
	    let divId = this.getProperty("targetDiv",this.getProperty(PROP_DIVID,null,null,true),null,true);
            if (divId != null) {
                let html = this.getHtml();
//		console.log("*****",html,"*******");
		let div = $("#" + divId);
		let inline = this.getProperty("displayInline");
		if(inline) {
		    div.css("display","inline-block");
		    div.css("vertical-align","bottom");
		} 
		let width = this.getWidth("100%");
		if(width && width!="-1") {
                    div.css("width",HU.getDimension(width));
		}
		let commandText = this.getHeaderText();
		let commandMap = {};
		let headerDiv=div;
		if(commandText) {
		    let cmd= this.createCommandText(commandText, commandMap);
		    if(this.getHeaderTextDiv()) {
			headerDiv=jqid(this.getHeaderTextDiv());
			headerDiv.html(cmd);
		    } else {
			html = cmd+ html;
		    }
		}
		div.html(html);
		if(commandText)
		    this.initCommandText(commandMap,headerDiv);
            } else {
                console.log("error: no div defined for display:" + this.getType());
            }
        },
	getMenuButton:function() {
            let get = this.getGet();
            let button = HU.onClick(get + ".showDialog();",
				    HU.image(RamaddaUtil.getCdnUrl("/icons/downdart.png"),
					     ['title','Show display menu',ATTR_CLASS, "display-dialog-button", ATTR_ID, this.getDomId(ID_MENU_BUTTON)]));
	    button+=" ";
	    return button;
	},

        /*
          This creates the default layout for a display
          Its a table:
          <td>title id=ID_HEADER</td><td>align-right popup menu</td>
          <td colspan=2><div id=ID_DISPLAY_CONTENTS></div></td>
          the getDisplayContents method by default returns:
          <div id=ID_DISPLAY_CONTENTS></div>
          but can be overwritten by sub classes

          After getHtml is called the DisplayManager will add the html to the DOM then call
          initDisplay
          That needs to call setContents with the html contents of the display
        */
        getHtml: function() {
            let contents = this.getContentsDiv();
	    if(this.getNoWrapper()) return contents;

            let get = this.getGet();
            let button = "";
            if (this.getShowMenu() || this.getShowMenuRight()) {
                button = this.getMenuButton();
            }
            let title = "";
            if (this.getShowTitle()) {
                title = this.getTitle(false).trim();
            }

	    let rightContents='';
	    if(this.getShowMenuRight()) {
		rightContents =button;
		button='';
	    }
            let topLeft = "";
            if (button != "" || title != "") {
                let titleDiv = this.getTitleHtml(title);
                if (button == "") {
                    topLeft = titleDiv;
                } else {
                    topLeft = HU.div(["class","display-header"], button + SPACE + titleDiv);
                }
		
            }
            topLeft = HU.div([ID, this.getDomId(ID_TOP_LEFT),CLASS,"display-header-block"], topLeft);

	    let headerStyle = this.getProperty('headerStyle','');
	    let h2Separate = this.getAnimationEnabled();
	    let h1 = 	HU.div(['style',headerStyle,ID,this.getDomId(ID_HEADER1),CLASS,"display-header-block display-header1"], "");
	    let h2 = HU.div(['style',headerStyle,ID,this.getDomId(ID_HEADER2),CLASS,"display-header-block display-header2"], "");
            let topCenter = HU.div([ID, this.getDomId(ID_TOP),CLASS,"display-header-block"], h2Separate?"":h2);
            let topRight = HU.div([ID, this.getDomId(ID_TOP_RIGHT)], rightContents);
	    let top =  this.getProperty("showHeader",true)?HU.leftCenterRight(topLeft, topCenter, topRight, null, null, null,{
                valign: "bottom"
            }):"";
	    //See if we are drawing the header somewhere else
	    if(this.getHeaderDiv()) {
		$('#'+ this.getHeaderDiv()).html(top);
		top='';
	    }

            let header = h1;
	    if(h2Separate) header+=h2;
	    top =  header +  top;	    
	    let colorTable = HU.div([ID,this.getDomId(ID_COLORTABLE)]);
	    let rightInner="";
	    let leftInner="";

	    let bottom='';
	    let legend = HU.div([ID,this.getDomId(ID_LEGEND)]);

	    let ctSide = this.getProperty("colorTableSide","bottom");
	    if(ctSide=="top") {
		top+=colorTable;
	    } else if(ctSide=="right") {
		rightInner += colorTable;
	    } else if(ctSide=="left") {
		leftInner += colorTable;
	    } else {
		bottom+=colorTable;
	    }
	    bottom += HU.div([ATTR_CLASS, "", ATTR_ID, this.getDomId(ID_BOTTOM)]);
	    bottom+=legend;
	    let leftStyle = "";
	    if(this.getProperty("leftSideWidth"))
		leftStyle = HU.css("width",HU.getDimension(this.getProperty("leftSideWidth")));
	    let left = HU.div([ATTR_ID, this.getDomId(ID_LEFT),STYLE,leftStyle],leftInner);
	    let right = HU.div([ATTR_ID, this.getDomId(ID_RIGHT)],rightInner);
	    let sideWidth = "1px";
	    let centerWidth = "100%";	    
	    let h0 = 	HU.div([ID,this.getDomId(ID_HEADER0),CLASS,"display-header-block display-header0"], "");
	    //Gack! We set a transparent 1px border here because for some reason the google charts will have a little bit of scroll in them if we don't set a border	
            let table =   h0+HU.open('table', [STYLE,this.isGoogleChart?"border:1px solid transparent;":'',CLASS, 'display-ui-table', 'width','100%','border','0','cellpadding','0','cellspacing','0']);
	    if(this.getProperty('showDisplayTop',true)) {
		table+= HU.tr([],HU.td(['width',sideWidth]) + HU.td(['width',centerWidth],top) +HU.td(['width',sideWidth]));
	    }
	    table+= HU.tr(["valign","top"],HU.td(['width',sideWidth],left) + HU.td(['width',centerWidth],contents) +HU.td(['width',sideWidth],right));
	    if(this.getProperty('showDisplayBottom',true)) {
		if(this.getFooterDiv(this.getProperty('bottomDiv'))) {
		    jqid(this.getFooterDiv(this.getProperty('bottomDiv'))).html(bottom);
		    bottom = "";
		}
		table+= HU.tr([],HU.td(['width',sideWidth]) + HU.td(['width',centerWidth],bottom) +HU.td(['width',sideWidth]));
	    }
	    table+=HU.close('table');
	    let message= HU.div([ID,this.domId(ID_DISPLAY_MESSAGE),CLASS,"display-output-message", STYLE,HU.css("display","none","position","absolute","top","10px","left","50%",
									"-webkit-transform","translateX(-50%)","transform","translateX(-50%)")],"");
            let html =  HU.div([ATTR_CLASS, 'ramadda-popup', ATTR_STYLE,"display:none;", ATTR_ID, this.getDomId(ID_MENU_OUTER)], '');
            let style = this.getProperty('displayStyle', '');
            html += HU.div([ATTR_ID,this.domId(ID_DISPLAY_CONTAINER),'spellcheck', 'false',ATTR_CLASS, 'display-contents display-' + this.type +'-contents', ATTR_STYLE, HU.css('position','relative') + style],table + message);
            return html;
        },
        getWidthForStyle: function(dflt) {
            let width = this.getProperty("width", -1);
            if (width == -1) return dflt;
	    return HU.getDimension(width);
        },
        getHeightForStyle: function(dflt) {
            let height = this.getProperty("height", -1);
            if (height == -1) return dflt;
            if (new String(height).match("^[0-9]+$")) {
                height = height + "px";
	    }
            return height;
        },
        getContentsStyle: function() {

            let style = "";
            let height = this.getHeightForStyle();
            if (height) {
                style += HU.css(HEIGHT, height);
            }

            let maxheight = this.getProperty("maxHeight");
            if (maxheight) {
                style += HU.css("max-height", HU.getDimension(maxheight),"overflow-y","auto");
            }	    
            return style;
        },
	getContentsClass: function() {
	    return "ramadda-expandable-target display-contents-inner display-" + this.type;
	},
        getContentsDiv: function() {
            let style = this.getContentsStyle();
            style += this.getProperty("contentsStyle", "");
            let image = this.getProperty("backgroundImage");
            if (image) {
                image = HU.getEntryImage(this.entryId, image);
                style += HU.css("background-attachment","auto","background-size","100% auto","background-image","url('" + image + "')");
            }
            let background = this.getProperty("background");
            if (background)
                style += HU.css("background", background);
            let topBottomStyle = "";
	    //            let width = this.getWidthForStyle();
	    //            if (width) {
	    //                topBottomStyle += HU.css("width", width);
	    //            }
            let top = HU.div([STYLE, topBottomStyle, ATTR_ID, this.getDomId(ID_DISPLAY_TOP)], "");
            let bottom = HU.div([STYLE, topBottomStyle, ATTR_ID, this.getDomId(ID_DISPLAY_BOTTOM)], "");
	    let expandedHeight  = this.getProperty("expandedHeight");
	    if(expandedHeight)
		style+=HU.css(HEIGHT,expandedHeight);
	    if(!this.getProperty("showInnerContents",true)) {
		style+="display:none;";
	    }		
	    let contentsAttrs =[ATTR_CLASS, this.getContentsClass(), STYLE, style, ATTR_ID, this.getDomId(ID_DISPLAY_CONTENTS)];

	    if(this.getProperty("expandableHeight")) {
		contentsAttrs.push("expandable-height");
		contentsAttrs.push(this.getProperty("expandableHeight"));
	    }
	    let mainDiv = HU.div(contentsAttrs, "");
	    if(this.getNoWrapper()) return mainDiv;

	    let contents =  top + "\n" +mainDiv + "\n" +bottom;
            return contents;
        },

        //Gets called before the displays are laid out
        prepareToLayout: function() {
            //Force setting the property from the input dom (which is about to go away)
            this.getColumn();
            this.getWidth();
            this.getHeight();
            this.getName();
            this.getEventSource();
        },
        getColumn: function() {
            return this.getFormValue("column", 0);
        },
        getRow: function() {
            return this.getFormValue("row", 0);
        },
        getWidth: function(dflt) {
            return this.getFormValue("width", dflt);
        },
        getHeight: function() {
            return this.getFormValue("height", 0);
        },
        getDisplayTitle: function(title) {
            if (!title) title = this.title != null ? this.title : "";
            let text = title;
            let fields = this.lastSelectedFields;
            if (fields && fields.length > 0)
                text = text.replace("{field}", fields[0].getLabel());
            else
                text = text.replace("{field}", HU.span([ID,this.getDomId(ID_TITLE_FIELD)],"&nbsp;"));
            return text;
        },

        setDisplayTitle: function(title) {
            if (!Utils.stringDefined(title)) {
                title = this.getTitle(false).trim();
            }
            let text = this.getTitleHtml(title);
            if (this.getShowTitle()) {
                this.jq(ID_TITLE).show();
            } else {
                this.jq(ID_TITLE).hide();
            }
            this.writeHtml(ID_TITLE, text);
        },
	applyTitleTemplate:function(titleTemplate) {
	    let title = titleTemplate.replace(/\${title}/g,this.getProperty(ATTR_TITLE)??'');
	    let data = this.getPointData();
	    if (data && data.records && data.records.length)  {
		title = title.replace(/_first/g,'');
		title = this.applyRecordTemplate(data.records[0],null,null, title);
		title = title.replace(/_last/g,'');
		title = this.applyRecordTemplate(data.records[data.records.length-1],null,null, title);		    
	    }
	    title = title.replace(/\${.*?}/g,'');
	    return title;
	},
        getTitle: function(showMenuButton) {
            let prefix = "";
            if (showMenuButton && this.hasEntries()) {
                prefix = this.getEntryMenuButton(this.getEntries()[0]) + " ";
            }
	    

            let title = this.getProperty(ATTR_TITLE);
            let titleTemplate = this.getTitleTemplate();
            if (titleTemplate != null) {
		title = this.applyTitleTemplate(titleTemplate);
                return prefix + title;
            }
	    if(Utils.stringDefined(title))  return title;
            if (this.dataCollection == null) {
                return prefix;
            }
            let dataList = this.dataCollection.getList();
            title = "";
            for (let collectionIdx = 0; collectionIdx < dataList.length; collectionIdx++) {
                let pointData = dataList[collectionIdx];
                if (collectionIdx > 0) title += "/";
                title += pointData.getName();
            }

            return prefix + title;
        },
        getIsLayoutFixed: function() {
            return this.getProperty(PROP_LAYOUT_HERE, true);
        },

        makeToolbar: function(props) {
            let toolbar = "";
            let get = this.getGet();
            let addLabel = props.addLabel;
            let images = [];
            let calls = [];
            let labels = [];
            if (!this.getIsLayoutFixed()) {
                calls.push("removeRamaddaDisplay('" + this.getId() + "')");
                images.push("fa-cut");
		labels.push("Delete display");
            }
            calls.push(get + ".copyDisplay();");
	    images.push("fa-copy");
            labels.push("Copy Display");
            if (this.jsonUrl != null) {
                calls.push(get + ".fetchUrl('json');");
                images.push(RamaddaUtil.getCdnUrl("/icons/json.png"));
                labels.push("Download JSON");

                calls.push(get + ".fetchUrl('csv');");
                images.push(RamaddaUtil.getCdnUrl("/icons/csv.png"));
                labels.push("Download CSV");
            }
            for (let i = 0; i < calls.length; i++) {
                let inner = HU.getIconImage(images[i], [ATTR_TITLE, labels[i], ATTR_CLASS, "display-dialog-header-icon"]);
                if (addLabel) inner += " " + labels[i] + "<br>";
                toolbar += HU.onClick(calls[i], inner);
            }
            return toolbar;
        },

	getHeader2:function() {
	    return "";
	},
	initHeader2:function() {
	},
	writeHeader:function(header,html) {
	    if(html=="") {
		this.jq(header).css("display","none");
	    } else {
		this.jq(header).css("display","inline-block");
	    }
	    this.jq(header).html(html);
	},

        //This keeps checking the width of the chart element if its zero
        //we do this for displaying in tabs
        checkLayout: function() {
            let d = this.jq(ID_DISPLAY_CONTENTS);
	    let w= d.width();
            if (this.lastWidth != w) {
		this.lastWidth = w;
                this.displayData();
            }
	},


        forceUpdateUI: function() {
	    this.haveCalledUpdateUI = false;
	    this.callUpdateUI();
	},
	callUpdateUI: function(args) {
	    args = args || {};
	    try {
		if(args.force)
		    this.haveCalledUpdateUI = false;
		this.updateUI(args);
	    } catch(err) {
		this.handleError("Error:" + err,err);
	    }
	},
        updateUI: function(args) {
	},
	getFilterId: function(id) {
	    return  this.getDomId("filterby_" + id);
	},
	getRequestMacros: function() {
	    if(!this.requestMacros) {
		this.requestMacros  = this.getRequestMacrosInner();
	    }
	    return this.requestMacros;
	},
	getRequestMacrosInner: function() {
	    if(this.getProperty('requestFieldsDefault')|| this.getProperty('requestFields')) {
		//clear them out
		this.requestMacros = null;
		[['requestFields','date,stride,skip,limit'],
		 ['requestFieldsToggleOpen',true],
		 ['request.date.type','daterange'],
		 ['request.stride.title','Specify a skip factor'],
		 ['request.stride.includeNone',false],
		 ['request.stride.type','enumeration'],
		 ['request.stride.values','0:None,1,2,3,4,5,6,7,8,9,10,15,20,30,40,50,75,100'],
		 ['request.stride.default',0],
		 ['request.limit.label','# Records'],
		 ['request.limit.title','Limit how many records to return'],
		 ['request.limit.size','4'],
		 ['request.skip.size','4'],
		 ['request.skip.type','skip'],		 
		 ['request.skip.default',this.getProperty('skip','0')],
		 ['request.limit.default',this.getProperty('max','20000')],
		 ['requestFieldsLive',false]].forEach(pair=>{
		     if(!Utils.isDefined(this.getProperty(pair[0]))) {
			 this.setProperty(pair[0],pair[1]);
		     }
		 });
	    }		


	    let macros =[];
	    let p = this.getProperty("requestFields","");
	    let e1 = this.getProperty("extraFields1","");
	    let e2 = this.getProperty("extraFields2","");
	    let list = Utils.mergeLists(e1.split(","),p.split(","),e2.split(","));
//	    if(p!="")console.log("requestFields=" + p);
	    list.forEach(macro=>{
		if(macro=="") return;
		macros.push(new RequestMacro(this, macro));
	    });
	    macros.forEach(macro=>{
		macro.initMacros(macros);
	    });


	    return macros;
	},
	applyRequestProperties: function(props) {
	    if(!props) return;
	    this.requestMacros = null;
	    this.dynamicProperties = props;
	    this.createRequestProperties();
	},
	createRequestProperties: function() {

	    let requestProps = "";
	    let macros = this.getRequestMacros();
	    let macroDateIds = [];
	    let live = this.getRequestFieldsLive();
	    let list = [];
	    let space = HU.space(2);
	    if(!live)
		requestProps+=HU.span(['class','ramadda-button-small',
				       'title','Reload data','style','','class','','id',this.domId('requestapply')],HU.getIconImage('fa-solid fa-rotate-right')) + space;

	    macros.forEach(macro=>{
		requestProps+=macro.getWidget(macroDateIds);
		if(macro.isVisible()) {
		    requestProps+=space;
		}
	    });
	    if(this.getRequestFieldsToggle()) {
		requestProps=HU.toggleBlock(this.getProperty('requestFieldsToggleLabel','Select') + HU.space(3),requestProps,
					    this.getProperty('requestFieldsToggleOpen',false),
					    {orientation:'horizontal'});
	    }
	    if(Utils.stringDefined(requestProps)) {
		let show = this.getRequestFieldsShow();
		if(!show) {
		    requestProps = HU.div(['style','display:none;'], requestProps);
		} 
		this.writeHeader(ID_REQUEST_PROPERTIES,  HU.div([],requestProps));
		if(!this.getRequestFieldsShow()) {
		    //Hide this so it doesn't screw up the spacing
//		    this.jq(ID_REQUEST_PROPERTIES).css(
		}
	    }
	    //Keep track of the values because there can be spurious changes triggered
	    //when the user clicks in a time range field
	    let valueMap = {}
	    let applyButton = this.jq('requestapply').button();
	    let applyRequest = ()=>{
		applyButton.css('background','');
		applyButton.css('border-color','');		    
		this.macroChanged();
		this.reloadData();
	    };
	    let macroChange = (macro,value,what,force,apply)=>{
		if(macro) macro.notifyChange();
		if(what) {
		    if(!force && value == valueMap[macro.urlarg+'_'+what]) {
			//console.log('duplicate:' + value);
			return
		    }
		    valueMap[macro.urlarg+'_'+what]  =value;
		    //console.dir('new value:' + macro.urlarg + ' ' + what +' :' +value+':')
		}

		if(this.settingMacroValue) return;
//		console.log('macro change-2',apply,live,macro.triggerReload);
		let reloaded = false;

		if((apply || live) && macro.triggerReload) {
		    reloaded=true;
		    applyRequest();
		} else {
		    applyButton.css('background','yellow');
		    applyButton.css('border-color','#000');		    
		}
		if(!macro.name) return;
		this.settingMacroValue = true;
		let args = {
		    entryId:this.entryId,
		    property: "macroValue",
		    id:macro.name,
		    what:what,
		    value: value
		};
		this.propagateEvent(DisplayEvent.propertyChanged, args);
		this.settingMacroValue = false;
	    };

	    applyButton.click(()=>{
		applyRequest();
	    });

	    let sliderFunc = function() {
		//		macroChangeinputFunc
	    };

	    macroDateIds.forEach(id=>{
		HU.datePickerInit(id);
	    });
	    this.jq(ID_HEADER2).find(".display-request-reload").click(()=>{
		macroChange({triggerReload:true});
	    });
	    macros.every(macro=>{
		macro.initWidget(macroChange);
		let  keyup =function(e,what,val,force) {
		      let keyCode = e.keyCode || e.which;
		      if (keyCode == 13) {
			  macroChange(macro, val,what,force,true);
		      }
		};

		$("#" + this.getDomId(macro.getId())+"," +
		  "#" + this.getDomId(macro.getId()+"_min")+ "," +
		  "#" + this.getDomId(macro.getId()+"_max")).keyup(function(e){
		      keyup(e,null,$(this).val())});
		if(macro.type == "bounds") {
		    this.jq(macro.getId()).change(function(e) {
			macroChange(macro,$(this).is(':checked'));
		    });
		}
		if(macro.type=="enumeration") {
		    this.jq(macro.getId()).change(function(e) {
			macroChange(macro, $(this).val());
		    });
		}
		this.jq(macro.getId()+"_min").change(function(e) {
		    //		    macroChange(macro, $(this).val(),"min");
		});
		this.jq(macro.getId()+"_max").change(function(e) {
		    //		    macroChange(macro, $(this).val(),"max");
		});
		let from = this.jq(macro.getId()+"_from");
		let to = this.jq(macro.getId()+"_to");
		if(from.length) {
		    //true->force the change
		    from.keyup(function(e){
			keyup(e,'from',$(this).val(),true)});
		    to.keyup(function(e){
			keyup(e,'to',$(this).val(),true)});		    


		    valueMap[macro.urlarg+'_from']  =from.val();
		    valueMap[macro.urlarg+'_to']  =to.val();		    
		    from.change(function(e) {
			macroChange(macro, $(this).val(),"from");
		    });
		    to.change(function(e) {
			macroChange(macro, $(this).val(),"to");
		    });
		}
		return true;
	    });
	},
	makeFilterWidget:function(name,label, widget, title) {
	    if(!label)
		return HU.div([CLASS,"display-filter-widget"],widget);
	    label = this.makeFilterLabel(label,title)+(label.trim().length==0?" ":": ");
	    if(this.getFilterLabelVertical(this.getProperty(name+'.filterLabelVertical')))
		label = label+'<br>'+widget;							       
	    else
		label = label+widget;
	    return HU.div([CLASS,"display-filter-widget"],label);
	},
	makeFilterLabel: function(label,tt,vertical) {
	    let clazz = "display-filter-label";
	    if(vertical)
		clazz+= " display-filter-label-vertical ";
	    let attrs = [CLASS,clazz];
	    if(tt)  {
		attrs.push(TITLE);
		attrs.push(tt);
	    }
	    return HU.span(attrs,label);
	},

	stepFilterDateAnimation: function(inputFunc, dir){
	    let select = $("#" +this.getFilterId(ID_FILTER_DATE));
	    let index = select[0].selectedIndex;
	    let length = select.find('option').length;
	    index+=dir;
	    if(index>=length) {
		return;
//		index =0;
	    } else if(index<0) {
		return;
//		index = length-1;
	    }
	    select[0].selectedIndex = index;
	    inputFunc(select);
	    if(this.filterDatePlayingAnimation) {
		setTimeout(()=>{
		    this.stepFilterDateAnimation(inputFunc,1);
		},this.getProperty("filterDateAnimationSleep",1000));
	    }
	},

	addFilters: function(filters) {
	},
	initializeRangeSlider:function(jq, inputFunc, immediate) {
	    let _this = this;
	    jq.mousedown(function(){
		let id = $(this).attr(ID);
		//Do these like this in case we have a field that ends with _max
		let type = $(this).attr('data-type');
		if(id.endsWith("_min")) {
		    id = id.replace(/_min$/,"");
		} else if(id.endsWith("_max")) {
			id = id.replace(/_max$/,"");
		}
		let min = $("#" + id+"_min");
		let max = $("#" + id+"_max");
		let range = {
		    min: parseFloat(min.attr("data-min")),
		    max: parseFloat(max.attr("data-max"))};
		let smin =  String(min.attr("data-min")).replace(/.*\./,"");
		let smax =  String(max.attr("data-max")).replace(/.*\./,"");		
		let numDecimals = Math.max(2,Math.max(smin.length,smax.length));
		let minValue = parseFloat(min.val());
		let maxValue = parseFloat(max.val());
		if(minValue==parseInt(minValue) && maxValue==parseInt(maxValue))
		    numDecimals = 0;

		let html = HU.div([ID,"filter-range",STYLE,HU.css("width","200px")],"");
		let popup = HtmlUtils.getTooltip();
		popup.html(html);
		popup.show();

		popup.position({
		    of: min,
		    my: "left top",
		    at: "left bottom+2",
		    collision: "fit fit"
                });

		if(isNaN(minValue)) minValue = range.min;	
		if(isNaN(maxValue)) maxValue = range.max;
		let step = 1;
		if(type == "double" || parseInt(range.max)!=range.max || parseInt(range.min) != range.min) 
		    step = (range.max-range.min)/100000;
		$( "#filter-range" ).slider({
		    range: true,
		    min: range.min,
		    max: range.max,
		    step: step,
		    values: [minValue, maxValue],
		    slide: function( event, ui ) {
			let minv = String(Utils.roundDecimals(ui.values[0], numDecimals));
			let maxv = String(Utils.roundDecimals(ui.values[1], numDecimals));
			if(minv.endsWith(".")) minv = minv.replace(/\./,"");
			if(maxv.endsWith(".")) maxv = maxv.replace(/\./,"");			
			min.val(minv);
			max.val(maxv);
			min.attr("data-value",min.val());
			max.attr("data-value",max.val());
			if(immediate) {
			    inputFunc(min,max);
			}
		    },
		    stop: function() {
			let popup = HtmlUtils.getTooltip();
			popup.hide();
			_this.checkFilterField(max);
			inputFunc(min,max);
		    }
		});
	    });
	},
	getRecordFilter: function(fieldId) {
	    if(this.filters) {
		for(let i=0;i<this.filters.length;i++) {
		    let filter = this.filters[i];
		    if(filter.field && filter.field.getId() == fieldId) return filter;
		    if(filter.id == fieldId) return filter;		    
		}
	    }
	    return null;
	},
        checkSearchBar: function() {
            if (!this.hasData()) {
		return
	    }


	    let hideFilterWidget = this.getProperty("hideFilterWidget",false, true);
	    let vertical =  this.getProperty("headerOrientation","horizontal") == "vertical";
	    let filterClass = "display-filter";
	    let debug = displayDebug.checkSearchBar;
	    if(debug) console.log("checkSearchBar");
            let _this = this;

            let colorBy = this.getFieldById(null, this.getProperty("colorBy",""));
            this.colorByFields = this.getFieldsByIds(null, this.getProperty("colorByFields", "", true));
            this.sizeByFields = this.getFieldsByIds(null, this.getProperty("sizeByFields", "", true));
            this.sortByFields = this.getFieldsByIds(null, this.getProperty("sortByFields", "", true));	    

	    let pointData = this.getData();
            if (pointData == null) return;
            let fields= this.getFields();
            let records = this.getRecords();
	    records = this.sortRecords(records);
	    let header2="";
	    //	    header2 +=HU.div([ID,this.getDomId("test")],"test");
	    if(this.getShowProgress(false)) {
		header2 += HU.div([ID,this.getDomId(ID_DISPLAY_PROGRESS), STYLE,HU.css("display","inline-block","margin-right","4px","min-width","20px")]);
	    }
	    let headerLabel  = this.getHeaderLabel();
	    if(Utils.stringDefined(headerLabel)) {
		header2 += HU.div([ATTR_CLASS,CLASS_HEADER_SPAN+' '+CLASS_HEADER_LABEL],headerLabel);
	    }
	    header2 += HU.div([ID,this.getDomId(ID_HEADER2_PREPREPREFIX),ATTR_CLASS,CLASS_HEADER_SPAN],"");
	    header2 += HU.div([ID,this.getDomId(ID_HEADER2_PREPREFIX),ATTR_CLASS,CLASS_HEADER_SPAN],"");
	    header2 += HU.div([ID,this.getDomId(ID_HEADER2_PREFIX),ATTR_CLASS,CLASS_HEADER_SPAN],"");
	    header2 +=  this.getHeader2();
	    if(this.getProperty("pageRequest",false) || this.getFilterPaginate()) {
		
		header2 += HU.div([ATTR_CLASS,CLASS_HEADER_SPAN+" display-filter",ATTR_ID,this.getDomId(ID_PAGE_COUNT)]);
	    }
	    header2+=HU.div([ATTR_ID,this.getDomId(ID_REQUEST_PROPERTIES),ATTR_CLASS,CLASS_HEADER_SPAN],"");


	    if(this.getProperty("legendFields") || this.getProperty("showFieldLegend",false)) {
		let colors = this.getColorList();
		let fields =  this.getFieldsByIds(null, this.getProperty("legendFields", this.getPropertyFields(this.getProperty("sumFields"))));
		let html = "";
		let colorCnt = 0;
		fields.forEach((f)=>{
		    if(colorCnt>=colors.length) colorCnt = 0;
		    let color  = colors[colorCnt];
		    html += HU.div([STYLE,HU.css("display","inline-block","width","8px","height","8px","background",color)]) +" " + f.getLabel() +"&nbsp;&nbsp; ";
		    colorCnt++;
		});
		header2+= HU.div([CLASS,"display-field-legend"], html);

	    }

	    if(this.getProperty("showDisplayFieldsMenu",false)) {
		let displayFields =  pointData.getChartableFields();
		if(displayFields.length) {
		    let fields = this.getSelectedFields();
		    let selected =[];
		    fields.forEach(f=>selected.push(f.getId()));
		    let enums = [];
		    displayFields.forEach(field=>{
			if(field.isFieldGeo()) return;
			enums.push([field.getId(),field.getLabel()]);
		    });
		    let attrs = [ID,this.getDomId("displayfields")];
		    if(this.getProperty("displayFieldsMenuMultiple",false)) {
			attrs.push("multiple");
			attrs.push("true");
			attrs.push("size");
			attrs.push("4");
		    }
		    this.displayFieldsMenuEnums = enums;
		    let html =  HU.span([CLASS,filterClass],
				       this.makeFilterLabel("Display: ") + HU.select("",attrs,enums,selected))+SPACE;
		    let side = this.getProperty("displayFieldsMenuSide","top");
		    if(side == "left") {
			this.jq(ID_LEFT).append(html);
		    } else {
			//TODO: do the other sides
			header2+=html;
		    }
		}
	    }


	    let selectFields = this.getProperty("selectFields");
	    let selectFieldProps = [];
	    if(selectFields) {
		selectFields.split(";").forEach(t=>{
		    //htmlLayerField:Sparkline Field:field1,field2
		    let [prop,label,fields]  = t.split(":");
		    if(fields==null) {
			fields = label;
			label = Utils.makeLabel(prop);
		    }
		    let selectFields = this.getFieldsByIds(null,fields);
		    let enums = [];
		    selectFields.forEach(field=>{
			if(field.isFieldGeo()) return;
			enums.push([field.getId(),field.getLabel()]);
		    });
		    header2 += HU.span([CLASS,filterClass],
				       (label==""?"":this.makeFilterLabel(label+": ")) + 
				       HU.select("",[ID,this.getDomId("fieldselect_" + prop)],enums,this.getProperty(prop,"")))+SPACE;

		    selectFieldProps.push(prop);
		});
	    }


	    if(this.colorByFields.length>0) {
		let enums = [];
		this.colorByFields.forEach(field=>{
		    if(field.isFieldGeo()) return;
		    enums.push([field.getId(),field.getLabel(this)]);
		});
		let selected = colorBy?colorBy.getId():"";
		let label = this.makeFilterLabel(this.getProperty("colorByLabel", "Color by:&nbsp;"));
		header2 += HU.span([CLASS,filterClass],
				   label+ HU.select("",[ID,this.getDomId("colorbyselect")],enums,selected,20))+SPACE;
	    }
	    let sortAscending = this.getProperty("sortAscending",true);
	    if(this.sortByFields.length>0) {
		let enums = [];
		this.sortByFields.forEach(field=>{
		    if(field.isFieldGeo()) return;
		    let id = field.getId();
		    let label = field.getLabel();
		    if(Utils.stringDefined(field.getGroup())) {
			label = field.getGroup()+"-" + label;
		    }
		    let suffix1=this.getProperty('sortSuffixUp',' _uparrow_').replace('_uparrow_','&uarr;');
		    
		    let suffix2=this.getProperty('sortSuffixDown',' _downarrow_').replace('_downarrow_','&darr;');
		    if(field.isFieldString()) {
			suffix1 = "A-Z";
			suffix2 = "Z-A";
		    }
		    if(sortAscending || field.isFieldString()) {
			enums.push([id+"_up",label + " " + suffix1]);
			enums.push([id+"_down",label + " " + suffix2]);
		    } else {
			enums.push([id+"_down",label + " " + suffix2]);
			enums.push([id+"_up",label + " " + suffix1]);
		    }
		});
		header2 += HU.span([ATTR_CLASS,filterClass],
				   this.makeFilterLabel("Order: ") + HU.select("",[ATTR_ID,this.getDomId("sortbyselect")],enums,this.getProperty("sortFields","")))+SPACE;
	    }

	    
	    this.uniqueFields =
		this.getFieldsByIds(null, this.getSelectUniqueFields());
	    if(this.uniqueFields && this.uniqueFields.length>0) {
		let enums =[];
		this.uniqueFields.forEach(field=>{
		    if(field.isFieldGeo()) return;
		    let id = field.getId();
		    let label = field.getLabel();
		    enums.push([id,label]);
		});
		header2 += HU.span([ATTR_CLASS,filterClass],
				   this.makeFilterLabel("Unique: ") +
				   HU.select("",['multiple',true,'size',Math.min(this.uniqueFields.length,4),ATTR_ID,this.getDomId("uniquefields")],
					     enums,null))+SPACE;
	    }


	    if(this.getProperty("showSortDirection")) {
		header2 +=HU.select("",[ID,this.getDomId("sortdirection")],[["up", "Sort Up"],["down","Sort Down"]],
				    sortAscending?"up":"down") + SPACE;
	    }


	    if(this.sizeByFields.length>0) {
		let enums = [];
		this.sizeByFields.forEach(field=>{
		    enums.push([field.getId(),field.getLabel()]);
		});
		header2 += HU.span([CLASS,filterClass],
				   this.makeFilterLabel("Size by: ") + HU.select("",[ID,this.getDomId("sizebyselect")],enums,this.getProperty("sizeBy","")))+SPACE;
	    }


	    let  highlight = this.getFilterHighlight();
	    if(this.getShowFilterHighlight()) {
		let enums =[["filter","Filter"],["highlight","Highlight"]];
		let select =  HU.select("",["fieldId","_highlight", ID,this.getDomId(ID_FILTER_HIGHLIGHT)],enums,!highlight?"filter":"highlight") + SPACE2;
		if(hideFilterWidget) {
		    select = HU.div([STYLE,HU.css("display","none")], select);
		}
		header2+=select;
	    }


	    let dataFilterIds = [];
	    this.getTheDataFilters().forEach(f=>{
		if(!f.label) return;
		let cbxid = this.getDomId("datafilterenabled_" + f.id);
		dataFilterIds.push(cbxid);
		header2 +=  HU.checkbox(cbxid,[ID,cbxid],f.enabled,
//					this.makeFilterLabel(f.label +"&nbsp;&nbsp;"));
					f.label +"&nbsp;&nbsp;");
	    });

	    if(this.getProperty("filterDate")) { 
		let type = this.getProperty("filterDate");
		//get dates
		let enums = [];
		if(this.getProperty("filterDateIncludeAll")) {
		    enums.push(["all","All"]);
		}
		let selected = null;
		let seen  = {};
		let dates  = [];
		records.forEach(record=>dates.push(record.getDate()));
		dates.sort(function(a,b) {
		    return a.getTime()-b.getTime();
		});
		dates.forEach(dttm=>{
		    let value = null;
		    if(type == "year") {
			value = dttm.getFullYear();
		    } else if(type== "day") {
			value = Utils.formatDateMonthDayYear(dttm);
		    }
		    if(!seen[value]) {
			selected = String(dttm);
			enums.push([String(dttm), value]);
			seen[value] = true;
		    }
		});

		let label = type=="year"?"Year":type=="month"?"Month":type=="day"?"Day":type;
		let style="";
		if(!this.getProperty("filterDateShow",true))
		    style +="display:none;";
		let selectId = this.getFilterId(ID_FILTER_DATE);
		
		label =  this.makeFilterLabel("Select " + label+": ");
		let prefix="";
		prefix += HU.div([ID,this.getDomId("filterDateStepBackward"),STYLE,HU.css("display","inline-block"),TITLE,"Step Back"],
 				 HU.getIconImage("fa-step-backward",[STYLE,HU.css("cursor","pointer")])) +SPACE1;
		prefix+=HU.div([ID,this.getDomId("filterDatePlay"),STYLE,HU.css("display","inline-block"),TITLE,"Play/Stop Animation"],
			       HU.getIconImage("fa-play",[STYLE,HU.css("cursor","pointer")])) + SPACE1;
		prefix += HU.div([ID,this.getDomId("filterDateStepForward"),STYLE,HU.css("display","inline-block"),TITLE,"Step Forward"],
 				 HU.getIconImage("fa-step-forward",[STYLE,HU.css("cursor","pointer")])) +SPACE1;

		let widget =  HU.span([CLASS,filterClass,STYLE,style],
				      prefix +
				      HU.select("",["fieldId","filterDate", ATTR_ID,selectId],enums,selected))+SPACE;
		if(hideFilterWidget) {
		    widget = HU.div([STYLE,HU.css("display","none")], widget);
		}
		header2+=widget;

	    }
	    

            let filterBy = this.getProperty("filterFields","").split(",").map(tok=>{return tok.trim();}); 
	    let fieldMap = {};
	    //Have this here so it can be used in the menu change events later. May cause problems if more than  one
	    let displayType = "";
	    this.filters = [];
	    this.filterMap = {};
	    this.addFilters(this.filters);
            if(filterBy.length>0) {
		let group = null;
                for(let i=0;i<filterBy.length;i++) {
		    if(filterBy[i]=="") continue;
		    if(filterBy[i].startsWith("group:")) {
			group = filterBy[i].substring(6);
			if(group=="none") group = null;
			continue;
		    }
		    let filter = new RecordFilter(this, filterBy[i]);
		    filter.group = group;
		    this.filters.push(filter);
		    this.filterMap[filter.getId()] = filter;
		}
		let searchBar = "";
		let bottom = [""];
		group = null;
		groupHtml = null;
		this.filters.forEach(filter=>{
		    if(!filter.isEnabled()) return;
		    let widget = filter.getWidget(fieldMap, bottom,records, vertical);
		    if(!vertical)
			widget = HU.span(['class','display-filter-container display-filter-'+ filter.displayType,ID,this.domId("filtercontainer_" + filter.id)], widget);
		    if(filter.group!=null) {
			if(filter.group!=group && groupHtml!=null) {
			    searchBar+=HU.toggleBlock(group,groupHtml,false);
			    groupHtml = null;
			}
			group = filter.group;
			if(groupHtml==null) {
			    groupHtml= "";
			}
			groupHtml+=widget;
			return;
		    }
		    if(groupHtml!=null) {
			searchBar+=HU.toggleBlock(group,groupHtml,false);
			groupHtml=null;
		    }
		    searchBar +=widget;
		});
		if(groupHtml!=null) searchBar+=HU.toggleBlock(group,groupHtml,false);
		style = (hideFilterWidget?"display:none;":"") + this.getProperty("filterByStyle","");
		if(this.getProperty("showFilterTotal",false)) {
		    searchBar+= HU.span([CLASS,"display-filter-label",ID,this.getDomId(ID_FILTER_COUNT)],"");
		}
		let filterBar = searchBar+bottom[0] + HU.div([ID,this.domId(ID_TAGBAR)],"");
		header2+=HU.div([CLASS,CLASS_HEADER_SPAN+" " +  filterClass,STYLE,style,ID,this.getDomId(ID_FILTERBAR)],filterBar);
	    }



	    if(vertical) {
		header2 = HU.div([CLASS,"display-header-vertical"],header2);
	    } else {
		let style = HU.css('line-height','0px');
		if(this.getProperty('headerCenter',true)) style +=HU.css('text-align','center');
		header2=HU.div([STYLE,style],   header2);
	    }
	    header2 = HU.leftRightTable(header2,
					HU.div([ATTR_ID,this.getDomId(ID_HEADER2_SUFFIX)],''));
	    					
	    let headerSide = this.getDisplayHeaderSide();
	    if(headerSide == "left") 
		this.jq(ID_LEFT).html(header2);
	    else if(headerSide == "right") 
		this.jq(ID_RIGHT).html(header2);	    	    
	    else
		this.jq(ID_HEADER2).html(header2);

	    this.initHeader2();
	    this.jq("test").button().click(()=>{
		this.haveCalledUpdateUI = false;
		this.callUpdateUI();
	    });
	    this.createRequestProperties();
 	    let inputFunc = (input, input2, value) =>{
		let debug = false;
		if(this.ignoreFilterChange) return;
		if(input.attr('ignore')) return;
                let id = input.attr(ID);
		if(!id) {
		    console.log("No ID attribute for filter");
		    return;
		}
		if(debug)
		    console.log(this.type+" filter change");

		let changedFilter;
		let changedFilterId;
		this.filters.every(filter=>{
		    if(filter.widgetId == id) {
			changedFilter = filter;
			changedFilterId = filter.id;
			return false;
		    }
		    return true;
		});

		if(debug)
		    console.log("changed filter:" + changedFilter)
		let dependentFilters =[];
		if(changedFilter) {
		    this.filters.forEach(filter=>{
			if(filter.depends == changedFilter.id) {
			    dependentFilters.push(filter);
			    let widget = $("#" + filter.widgetId);
			    this.ignoreFilterChange = true; 
			    filter.lastValue = widget.val();
			    widget.val(FILTER_ALL);
			    this.ignoreFilterChange = false; 
			}
		    });
		}


		if(!input2) {
		    if(id.endsWith("_min")) {
			input2 = $("#" + id.replace(/_min$/,"_max"));
		    } else if(id.endsWith("_max")) {
			let tmp = input;
			input =$("#" + id.replace(/_max$/,"_min"));
			input2 = tmp;
		    }
		}
		if(input.attr("isCheckbox")) {
		    let on = input.attr("onValue")||true;
		    let off = input.attr("offValue")||false;
		    if (input.is(':checked')) {
			value = on;
			console.log(_this.type +" cbx is checked value:" + value +" on:" + on +" off:" + off);
		    } else {
			value=off;
			console.log(_this.type +" cbx is not checked value:" + value +" on:" + on +" off:" + off);
		    }
		}
		if(!value) {
		    value = input.val();
		} 
		if(value===null || value==="") {
		    value = input.attr("data-value")  || input.val();
		}
		
		if(value==null) {
		    if(debug)
			console.log("no value:" + value);
		    return;
		}
		if(!Array.isArray(value) && input.attr("isButton")) {
		    //			console.log(_this.type +" " +Array.isArray(value));
		    let tmp = [];
		    value.split(",").forEach(v=>{
			tmp.push(v.replace(/_comma_/g,","));
		    });
		    value = tmp;
		}

                let fieldId = input.attr("fieldId");
		_this.checkFilterField(input);
		_this.haveCalledUpdateUI = false;
		if(_this.settingFilterValue) {
		    return;
		}
		_this.settingFilterValue = true;
		this.filteredRecords = null;
		if(debug)
		    console.log("calling dataFilterChanged");
		_this.dataFilterChanged();


		let records =[];
		let predecessorChanged = false;
		dependentFilters.forEach(filter=>{
		    if(this.filteredRecords == null )
			this.filteredRecords =  this.filterRecords();
		    let widget = filter.getWidget({}, [],this.filteredRecords);
		    this.jq("filtercontainer_" + filter.id).html(widget);
		    if(filter.initWidget)
			filter.initWidget(inputFunc);
		    if(filter.widgetId) {
			let widget = $("#" + filter.widgetId);
			if(!widget.length) {
			    console.log("Could not find dependent widget:" + filter.id);
			    return;
			}
			if(filter.lastValue) {
			    if(widget[0].options) {
				let values= $.map(widget[0].options,(option)=>{return option.value});
				if(!values.includes(filter.lastValue)) filter.lastValue = FILTER_ALL;
			    }
			    widget.val(filter.lastValue);
			}
			widget.change(function() {
			    inputFunc($(this));
			});
		    }
		    return true;
		});

		if(this.getIsMasterFilter()) {
		    //true=>don't add the display id prefix
		    this.addToDocumentUrl(fieldId+".filterValue",value,true);
		} else {
		    this.addToDocumentUrl(fieldId+".filterValue",value);
		}
		let args = {
		    id:id,
		    fieldId: fieldId,
		    value: value
		};
		if(input2) {
		    args.value2 = input2.val();
		}
		_this.propagateEvent(DisplayEvent.filterChanged, args);
		_this.settingFilterValue = false;
            };

	    dataFilterIds.forEach(id=>{
		$("#" + id).click(function(e){
		    inputFunc($(this));
		});
	    });

	    
	    this.filters.forEach(f=>{
		if(f.initWidget)
		    f.initWidget(inputFunc);
	    });


	    this.jq(ID_FILTERBAR).find(".display-filter-items").each(function(){
		let parent = $(this);
		$(this).find(".display-filter-item").click(function(event){
		    let isAll = $(this).hasClass("display-filter-item-all");
		    let selectClazz = "display-filter-item-selected"
		    let wasSelected = $(this).hasClass(selectClazz);
		    let fieldId = $(this).attr("fieldId");
		    let multiples = _this.getProperty(fieldId +".filterMultiple",false);
		    if(!event.metaKey || isAll || !multiples) {
			parent.find(".display-filter-item").removeClass(selectClazz);
		    } else {
			parent.find(".display-filter-item-all").removeClass(selectClazz);
		    }
		    if(wasSelected) {
		//		    if(wasSelected  && event.metaKey) {
			$(this).removeClass(selectClazz);
		    } else {
			$(this).addClass(selectClazz);
		    }
		    let values = [];
		    parent.find("." + selectClazz).each(function() {
			values.push($(this).attr("data-value").replace(/,/g,"_comma_"));
		    });
		    if(values.length==0) {
			parent.find(".display-filter-item-all").addClass(selectClazz);
			values.push(FILTER_ALL);
		    }
		    let value =  Utils.join(values,",");
		    parent.attr("data-value", value);
		    $("#"+parent.attr(ID) +"_label").html(values.includes(FILTER_ALL)?SPACE:value);
		    inputFunc(parent,null, values);
		});

	    });
	    this.jq(ID_FILTERBAR).find(".display-filter-input").keyup(function(e) {
		if($(this).attr("istext")) return;
		let keyCode = e.keyCode || e.which;
		if (keyCode == 13) {return;}
		HtmlUtils.hidePopupObject();

		let input = $(this);
		let val = $(this).val().trim();
		if(val=="") return;
                let fieldId = $(this).attr("fieldId");
		let field = fieldMap[fieldId].field;
		let values = fieldMap[fieldId].values;
		let items=[];
		let regexp=null;
		try {
		    val = val.replace(/\./g,"\\.");
		    regexp = new RegExp("(" + val+")",'i');
		} catch(ignore) {
		    //todo
		}
		for(let i=0;i<values.length;i++) {
		    let text= values[i].toString();
		    let match  = regexp?text.match(regexp):text.indexOf(val)>=0;
		    if(match) {
			items.push([match[1], values[i]]);
		    }
		    if(items.length>30) break;
		}
		if(items.length>0) {
		    let html = "";
		    let itemCnt = 0;
		    items.forEach(item=>{
			let match = item[0];
			item =  item[1];
//			if(item.length>50) return;
			let label = item.replace(regexp,"<span style='background:" + TEXT_HIGHLIGHT_COLOR +";'>" + match +"</span>");
			item = item.replace(/\'/g,"\'");
			html+=HU.div([TITLE,item,CLASS,"ramadda-hoverable ramadda-clickable display-filter-popup-item","item",item],label)+"\n";
			itemCnt++;
		    });	
		    if(itemCnt>0) {
			let popup =HtmlUtils.setPopupObject(HtmlUtils.getTooltip());
			popup.html(HU.div([STYLE,HU.css("margin","5px"), CLASS, "ramadda-popup-inner ramadda-snippet-popup"], html));
			popup.show();
			popup.position({
			    of: $(this),
			    my: "left top",
			    at: "left bottom",
			});
			$(".display-filter-popup-item").click(function(){
			    HtmlUtils.hidePopupObject();
			    input.val($(this).attr("item"));
			    inputFunc(input);
			});
		    }
		}

	    });



	    this.initializeRangeSlider(this.jq(ID_FILTERBAR).find(".display-filter-range"), inputFunc, this.getProperty("filterSliderImmediate"));

	    this.jq(ID_FILTER_HIGHLIGHT).change(function() {
		_this.setProperty("filterHighlight", $(this).val()=="highlight");
		_this.dfltFilterHighlight = null;
		_this.haveCalledUpdateUI = false;
		inputFunc($(this));
	    });


	    $("#" + this.getFilterId(ID_FILTER_DATE)).change(function() {
		inputFunc($(this));
	    });
	    this.jq("filterDatePlay").click(function() {
		_this.filterDatePlayingAnimation = !_this.filterDatePlayingAnimation;
		let icon = _this.filterDatePlayingAnimation?"fa-stop":"fa-play";
		$(this).html(HU.getIconImage(icon,[STYLE,HU.css("cursor","pointer")]));
		if(_this.filterDatePlayingAnimation) {
		    _this.stepFilterDateAnimation(inputFunc,1);
		}
	    });
	    this.jq("filterDateStepBackward").click(function() {
		_this.filterDatePlayingAnimation = false;
		let icon = _this.filterDatePlayingAnimation?"fa-stop":"fa-play";
		_this.jq("filterDatePlay").html(HU.getIconImage(icon,[STYLE,HU.css("cursor","pointer")]));
		_this.stepFilterDateAnimation(inputFunc,-1);
	    });
	    this.jq("filterDateStepForward").click(function() {
		_this.filterDatePlayingAnimation = false;
		let icon = _this.filterDatePlayingAnimation?"fa-stop":"fa-play";
		_this.jq("filterDatePlay").html(HU.getIconImage(icon,[STYLE,HU.css("cursor","pointer")]));
		_this.stepFilterDateAnimation(inputFunc,1);
	    });

            this.jq("displayfields").change(function(){
		let val = $(this).val();
		if(Array.isArray(val)) {
		    val = val.join(",");
		}
		_this.displayFieldsChanged(val);
		_this.propagateEvent(DisplayEvent.propertyChanged, {
		    property:'displayFields',
		    value: val
		});
	    });


	    selectFieldProps.forEach(prop=>{
                this.jq("fieldselect_" + prop).change(function(){
		    _this.fieldSelectedChanged(prop,$(this).val());
		});
	    });


            this.jq("colorbyselect").change(function(){
		_this.colorByFieldChanged($(this).val());
	    });
	    this.jq('uniquefields').change(()=>{
		this.callUpdateUI();
	    });

            this.jq("sortbyselect").change(function(){
		let val = $(this).val();
		if(val.endsWith("_up")) {
		    _this.setProperty("sortAscending",true);
		    val = val.replace(/_up$/,"");
		} else {
		    val = val.replace(/_down$/,"");
		    _this.setProperty("sortAscending",false);
		}
		_this.sortByFieldChanged(val);
	    });
	    this.jq("sortdirection").change(function(){
		let val = $(this).val();
		_this.setProperty("sortAscending",val=="up");
		_this.forceUpdateUI();
	    });
            this.jq("sizebyselect").change(function(){
		_this.sizeByFieldChanged($(this).val());
	    });

            this.jq(ID_FILTERBAR).find("input").keyup(function(e){
		let keyCode = e.keyCode || e.which;
		if (keyCode == 13) {
		    inputFunc($(this));
		}
	    });
	    this.jq(ID_FILTERBAR).find("input:radio,select").change(function() {
		inputFunc($(this));
	    });
	    this.jq(ID_FILTERBAR).find("input:checkbox").change(function() {
		inputFunc($(this));
	    });
	    

	    let dates = [];
	    if(debug) console.log("checkSearchBar-getting filtered data");
	    let filteredRecords  = this.filterData();
	    if(debug) console.log("checkSearchBar-done getting filtered data");
	    if(filteredRecords) {
		this.initializeAnimation(filteredRecords);
	    }
	    if(debug) console.log("checkSearchBar-done");
        },
	initializeAnimation:function(filteredRecords) {
	    let debug = false;
	    if(!filteredRecords) filteredRecords = this.getRecords();
	    let dateInfo = this.getDateInfo(filteredRecords);
	    if(debug) console.log("initializeAnimation-1");
	    if (dateInfo.dateMax) {
		if(debug) console.log("initializeAnimationr-getAnimation");
		let animation = this.getAnimation();
		if(animation.getEnabled()) {
		    if(debug) console.log("initializeAnimation-calling animation.init");
		    //		    console.log("dateMin:" + dateMin.toUTCString());
		    animation.init(dateInfo.dateMin, dateInfo.dateMax,filteredRecords);
		    if(debug) console.log("initializeAnimation-done calling animation.init");
		    if(!this.minDateObj) {
			if(debug) console.log("initializeAnimation-calling setDateRange");
			if(this.getProperty("animationFilter", true)) {
			    this.setDateRange(animation.begin, animation.end);
			}
			if(debug) console.log("initializeAnimation-done calling setDateRange");
		    }
		}
	    }
	},


	getDateInfo:function(records) {
	    let dateMin = null;
	    let dateMax = null;
	    let dates =[];
	    records.every(record=>{
		if (dateMin == null) {
		    dateMin = record.getDate();
		    dateMax = record.getDate();
		} else {
		    let date = record.getDate();
		    if (date) {
			dates.push(date);
			if (date.getTime() < dateMin.getTime())
			    dateMin = date;
			if (date.getTime() > dateMax.getTime())
			    dateMax = date;
		    }
		}
		return true;
	    });
	    return { dateMin:dateMin, dateMax:dateMax, dates:dates};
	},
	    
	getHighlightColor: function() {
	    return this.getProperty("highlightColor", HIGHLIGHT_COLOR);
	},
	checkFilterField: function(f) {
	    let min = f.attr("data-min");
	    let max = f.attr("data-max");
	    let value = f.val();
	    if(Utils.isDefined(min)) {
		if(value != min) {
		    f.css("background",TEXT_HIGHLIGHT_COLOR);
		} else {
		    f.css("background","white");
		}
	    } else if(Utils.isDefined(max)) {
		if(value != max) {
		    f.css("background",TEXT_HIGHLIGHT_COLOR);
		} else {
		    f.css("background","white");
		}
	    }

	},
	fieldSelectedChanged: function(prop,val) {
	    this.setProperty(prop,val);
	    this.haveCalledUpdateUI = false;
	    this.callUpdateUI();
	},
	colorByFieldChanged:function(field) {
	    this.setProperty("colorBy", field);
	    this.callUpdateUI();
	},
	sortByFieldChanged:function(field) {
	    this.setProperty("sortFields", field);
	    this.callUpdateUI();
	},
	sizeByFieldChanged:function(field) {
	},
	someFieldChanged:function(type,field) {
	},	
	macroChanged: function() {
	    this.pageSkip = 0;
	},
	rowStartIndex:0,
	dataFilterChanged: function(args) {
	    this.rowStartIndex=0;
	    args = args||{};
	    args.dataFilterChanged = true;
	    this.callUpdateUI(args);
	},
	addFieldClickHandler: function(jq, records, addHighlight) {
	    let _this = this;
	    if(records) {
		if(!jq) jq = this.jq(ID_DISPLAY_CONTENTS);
		let map = this.makeIdToRecords(records);
		let func = function() {
		    if(addHighlight) {
			$(this).parent().find(".display-row-highlight").removeClass("display-row-highlight");
			$(this).addClass("display-row-highlight");
		    }
		    let record = records[$(this).attr(RECORD_INDEX)];
		    if(!record) record = map[$(this).attr(RECORD_ID)];
		    if(record)
			_this.propagateEventRecordSelection({record:record});
		};
		let children = jq.find("[" +RECORD_INDEX+"]");
		if(!children.length) children = jq.find("[" +RECORD_ID+"]");
		if(!children.length) children = jq;
		children.click(func);
	    }

	    if(this.getProperty("propagateValueClick",true)) {
		let _this = this;
		if(!jq) jq = this.jq(ID_DISPLAY_CONTENTS);
		jq.find("[field-id]").click(function() {
		    let fieldId = $(this).attr('field-id');
		    let args = {
			id:fieldId,
			fieldId: fieldId,
			value: $(this).attr("field-value")
		    };
		    _this.propagateEvent(DisplayEvent.filterChanged, args);
		});
	    }

	},
	makeIdToRecords: function(records) {
	    let idToRecord = {};
	    records.forEach(r=>idToRecord[r.getId()] = r);	    
	    return idToRecord;
	},
	makeTooltipClick: function(selector, records) {
	    let tooltipClick = this.getProperty("tooltipClick");
	    if(!tooltipClick) return;
	    selector.css("cursor","pointer");
	    let idToRecord = this.makeIdToRecords(records);
	    let _this = this;
	    selector.click(function() {
		let record = idToRecord[$(this).attr(RECORD_ID)];
		if(!record) return;
		if(_this.tooltipDialog) {
		    _this.tooltipDialog.remove();
		    _this.tooltipDialog = null;
		} 
		let tt =  _this.getRecordHtml(record,null,tooltipClick);
		tt = HU.div([STYLE,HU.css("width","600px")], tt);
		_this.tooltipDialog =  HU.makeDialog({content:tt,anchor:$(this),
						      draggable:true,header:true});
		if(_this.getProperty("dialogListener"))
		    _this.getProperty("dialogListener")(this, _this.tooltipDialog);
		_this.initTemplatePopup(_this.tooltipDialog);
	    });
	},

	initTemplatePopup: function(dialog) {
	    let _this = this;
	    dialog.find(".display-search-tag").click(function() {
		let type = $(this).attr("metadata-type");
		if(type==null) return;
		let filter = _this.filterMap[type];
		if(filter==null) return;
		let value = $(this).attr("metadata-value");
		filter.toggleTag(value,true,null, true);
	    });

	},

	//Make sure to set the title attribute on the elements
	makeTooltips: function(selector, records, callback, tooltipArg,propagateHighlight) {		
	    let tooltipClick = this.getProperty("tooltipClick");
	    if(tooltipClick) {
		this.makeTooltipClick(selector,records);
	    }
	    if(!Utils.isDefined(propagateHighlight) || propagateHighlight==null)
		propagateHighlight = this.shareEvent(DisplayEvent.recordHighlight, this.getProperty("propagateEventRecordHighlight",false));
	    if(!this.getProperty("showTooltips",true)) {
		return;
	    }
	    let tooltip = tooltipArg ?? this.getTooltip();
	    if(tooltip==null || tooltip=='none') {
		return;
	    }
	    let _this = this;
	    let idToRecord = this.makeIdToRecords(records);
	    let tooltipFunc = {
		content: function() {
		    let record = idToRecord[$(this).attr(RECORD_ID)];
		    if(!record)  record = records[parseFloat($(this).attr(RECORD_INDEX))];
		    if(!record) return null;
		    let propagateOk = true;
		    if(callback && callback(true, record) === false) {
			propagateOk = false;
		    }
		    if(propagateOk && propagateHighlight) {
			_this.getDisplayManager().notifyEvent(DisplayEvent.recordHighlight, _this, {highlight:true,record: record});
		    }
		    if(tooltip=="" || tooltip=="none") return null;
		    let style = _this.getProperty("tooltipStyle","font-size:10pt;");
		    let tt =  _this.getRecordHtml(record,null,tooltip);
		    if(style) tt=HU.div([ATTR_STYLE,style],tt);
		    return tt;
		},
		close: function(event,ui) {
		    let record = idToRecord[$(this).attr(RECORD_ID)];
		    if(!record)
			record = records[parseFloat($(this).attr(RECORD_INDEX))];
		    let propagateOk = true;
		    if(callback && callback(false, record) === false) {
			propagateOk = false;
		    }
		    if(propagateOk && propagateHighlight)
			_this.getDisplayManager().notifyEvent(DisplayEvent.recordHighlight, _this, {highlight:false,record: record});
		},
		position: {
		    my: _this.getTooltipPositionMy("left top"),
		    at: _this.getTooltipPositionAt("left bottom+2"),
		    collision: _this.getTooltipCollision("flip")
		},
		classes: {
		    "ui-tooltip": _this.getProperty("tooltipClass", "ramadda-shadow-box  display-tooltip " + 'display-' + this.getType() +'-tooltip')
		}
	    };
	    if(this.getTooltipImmediate()) {
		$.extend(tooltipFunc,{show: false, hide:false});
	    } else {
		$.extend(tooltipFunc,{
		    show: {
			delay: parseFloat(_this.getTooltipDelay()),
			effect:this.getTooltipEffect(),
			duration: parseFloat(_this.getTooltipDuration()),
		    }});
	    }
	    if(selector.length>500) {
		//A hack to fix really slow tooltip calls when there are lots of elements
		selector.mouseenter(function() {
		    let tooltip = $(this).tooltip(tooltipFunc);
		    tooltip.tooltip('open');
		});
		selector.mouseleave(function() {
		    let tooltip = $(this).tooltip({});
		    tooltip.tooltip('close');
		});
	    } else {
		selector.tooltip(tooltipFunc);
	    }
	},
	makeRecordSelect: function(selector,idToRecords, callback) {
	    let _this = this;
	    selector.click(function(event){
		let record = idToRecords[$(this).attr(RECORD_ID)];
		if(!record) return;
		if(callback) callback(record);
		_this.propagateEventRecordSelection({select:true,record: record});
	    });
	},
	makePopups: function(selector, records, callback, popupTemplate) {
	    if(!popupTemplate)
		popupTemplate = this.getProperty("popupTemplate");
	    if(!popupTemplate) return;
	    let _this = this;
	    selector.click(function(event){
		let record = records[parseFloat($(this).attr(RECORD_INDEX))];
		if(!record) return;
		if(callback) callback(record);
		_this.propagateEventRecordSelection({select:true,record: record});
		_this.showRecordPopup($(this),record, callback,popupTemplate);
	    });
	},
	showRecordPopup: function(element, record, popupTemplate) {
	    if(!record) return;
	    if(!popupTemplate)
		popupTemplate = this.getProperty("popupTemplate");
	    if(!popupTemplate) return;
	    let _this = this;
	    HtmlUtils.hidePopupObject();
	    let html =  _this.getRecordHtml(record,null,popupTemplate);
	    html = HU.div([CLASS, "display-popup " + _this.getProperty("popupClass",""),STYLE, _this.getProperty("popupStyle","")],html);
	    let popup = HtmlUtils.setPopupObject(HtmlUtils.getTooltip());
	    popup.html(html);
	    popup.show();
	    popup.position({
		of: element,
		my: _this.getProperty("popupPositionMy", "left top"),
		at: _this.getProperty("popupPositionAt", "left bottom+2"),
		collision: _this.getProperty("popupCollision", "none none")
	    });
	},
	animationStart: function(animation) {
	},
	animationApply: function(animation, skipUpdateUI) {
	    if(this.getProperty("animationFilter", true))
		this.setDateRange(animation.begin, animation.end);
	    if(!skipUpdateUI) {
		this.haveCalledUpdateUI = false;
		this.dataFilterChanged({source:"animation"});
	    }
	    this.propagateEvent(DisplayEvent.propertyChanged, {
		property: "dateRange",
		minDate: animation.begin,
		maxDate: animation.end
	    });
	},
        makeDialog: function(text) {
            let html = "";
	    if(!text) {
		let tabTitles = [];
		let tabContents = [];
		this.getDialogContents(tabTitles, tabContents);
		tabTitles.push("Edit");
		tabContents.push(this.makeToolbar({
                    addLabel: true
		}));
		let tabLinks = "<ul>";
		let tabs = "";
		for (let i = 0; i < tabTitles.length; i++) {
                    let id = this.getDomId("tabs") + i;
                    tabLinks += HU.tag("li", [], HU.tag("a", ["href", "#" + id],
							tabTitles[i]));
                    tabLinks += "\n";
                    let contents = HU.div([ATTR_CLASS, "display-dialog-tab"], tabContents[i]);
                    tabs += HU.div([ID, id], contents);
                    tabs += "\n";
		}
		tabLinks += "</ul>\n";
		text =  HU.div([ID, this.getDomId(ID_DIALOG_TABS)], tabLinks + tabs);
	    }
	    return text;
        },
        initDialog: function() {
            let _this = this;
            let updateFunc = function(e) {
                if (e && e.which != 13 && e.which!=0) {
                    return;
                }
		let changed = false;
		["column","row","width","height"].forEach(f=>{
                    if(_this[f] != _this.jq(f).val() && (_this[f] || _this.jq(f).val().trim()!="")) {
			changed = true;
			_this[f] = _this.jq(f).val();
		    }});
		

                if(changed) {
		    _this.getLayoutManager().doLayout();
		}
            };
	    ["column","row","width","height"].forEach(f=>{
		this.jq(f).blur(updateFunc);
		this.jq(f).keypress(updateFunc);
	    });

            this.jq("showtitle").change(function() {
                _this.setShowTitle(_this.jq("showtitle").is(':checked'));
            });
            this.jq("showdetails").change(function() {
                _this.setShowDetails(_this.jq("showdetails").is(':checked'));
            });
            this.jq(ID_DIALOG_TABS).tabs();

        },
        showDialog: function(text, from, initDialog, title) {
	    if(this.dialog) this.dialog.remove();
	    if(!this.dialogElement) {
//		$(document.body).append(HU.div([ATTR_CLASS, "display-dialog",ID,this.getDomId(ID_DIALOG)]));
//		this.dialogElement = this.jq(ID_DIALOG);
	    }
	    let html = this.makeDialog(text);
	    if(this.getProperty('dialogHook')) {
		html =this.getProperty('dialogHook')('contents',html);
	    }

	    let at = 'left bottom';
	    if(!from && this.jq(ID_MENU_BUTTON).length) {
		from=this.jq(ID_MENU_BUTTON);
	    }
	    if(!from) {
		from=this.jq(ID_DISPLAY_CONTENTS);
		at = 'left top';
	    }

	    this.dialog = HU.makeDialog({content:html,title:title||this.getTitle(),anchor:from,at:at,draggable:true,header:true});
	    if(initDialog) initDialog();
            else this.initDialog();
	    if(this.getProperty('dialogHook')) {
		this.getProperty('dialogHook')('init',this.dialog);
	    }
	    return this.dialog;
        },
        copyDisplay: function() {
            let newOne = {};
            $.extend(true, newOne, this);
            newOne.setId(newOne.getId() + this.getUniqueId("display"));
            addRamaddaDisplay(newOne);
            this.getDisplayManager().addDisplay(newOne);
        },
        removeDisplay: function() {
            this.getDisplayManager().removeDisplay(this);
	    if(this.dialogElement)  this.dialogElement.remove();
        },
        doingQuickEntrySearch: false,
        doQuickEntrySearch: function(request, callback) {
            if (this.doingQuickEntrySearch) return;
            let text = request.term;
            if (text == null || text.length <= 1) return;
            this.doingQuickEntrySearch = true;
            let searchSettings = new EntrySearchSettings({
                name: text,
                max: 10,
            });
            if (this.searchSettings) {
                searchSettings.clearAndAddType(this.searchSettings.entryType);
            }
	    let _this = this;
            let jsonUrl = this.getRamadda().getSearchUrl(searchSettings, OUTPUT_JSON);
            let handler = {
                entryListChanged: function(entryList) {
                    _this.doneQuickEntrySearch(entryList, callback);
                }
            };
            let entryList = new EntryList(this.getRamadda(), jsonUrl, handler, true);
        },
        doneQuickEntrySearch: function(entryList, callback) {
            let names = [];
            let entries = entryList.getEntries();
            for (let i = 0; i < entries.length; i++) {
                names.push(entries[i].getName());
            }
            callback(names);
            this.doingQuickEntrySearch = false;

        },
        handleWarning: function(message) {
	    if(!window.location.hash  || window.location.hash!="#fortest") {
		console.warn(message);
	    }
	},
        handleLog: function(message) {
	    if(!window.location.hash  || window.location.hash!="#fortest") {
		this.logMsg(message);
	    }
	},
        handleError: function(message, exc) {
	    this.setErrorMessage(message);
            console.error(this.type +" " + message);
	    if(exc && exc.stack) {
		let err = "";
		let limit=15;
		exc.stack.split("\n").every(line=>{
		    if(limit--<0) {
			err+="...\n";
			return false;
		    }
		    err+=line+"\n";
		    return true;
		});
		console.error(err);
	    }
        },
	setErrorMessage: function(msg) {
            this.setContents(this.getMessage(msg));
	},
	clearProgress: function() {
	    this.jq(ID_DISPLAY_PROGRESS).html("");
	},
	startProgress: function() {
	    if(this.jq(ID_DISPLAY_PROGRESS).length>0) 
		this.jq(ID_DISPLAY_PROGRESS).html(HU.image(icon_progress));
	    else {
		if(this.useDisplayMessage()) {
                    this.setDisplayMessage(this.getLoadingMessage());
		} else {
		    this.setContents(this.getLoadingMessage());
		}
	    }
	},
	handleNoData: function(pointData,reload) {
	    this.dataLoadFailed  =true;
	    let debug = displayDebug.handleNoData;
	    this.jq(ID_PAGE_COUNT).html("");
            if (!reload) {
		if(debug) console.log("\tno reload");
                this.addData(pointData);
                this.checkSearchBar();
            } else {
		if(!this.dataCollection)
		    this.dataCollection = new DataCollection();
		this.dataCollection.setData(pointData);
	    }
	    let message = this.getNoDataMessage();
	    this.setNoDataMessage(message);
	},
	setNoDataMessage:function(message) {
	    if(Utils.stringDefined(message)) {
		this.setContents(this.getMessage(this.getNoDataMessage()));
	    } else {
		this.setContents('');
	    }
	},
        pointDataLoadFailed: function(data) {
	    this.dataLoadFailed  =true;
	    this.clearProgress();
            this.inError = true;
            errorMessage = this.getProperty("errorMessage", null);
            if (errorMessage != null) {
                this.setContents(errorMessage);
                return;
            }
            let msg = "";
	    if(data && data.error) {
		msg = data.error;
		msg  = String(msg).replace(/</g,"&lt;").replace(/>/g,"&gt;");
	    } else   if (data && data.errorcode && data.errorcode == "warning") {
                msg = data.error;
		msg  = String(msg).replace(/</g,"&lt;").replace(/>/g,"&gt;");
            } else {
                msg = HU.b("An error has occurred:");
		msg+=' '+this.getLogLabel()+'<br>';
                if (!data) data = this.getNoDataMessage();
                let error = data.error ? data.error : data;
                error = error.replace(/<[^>]*>/g, "");
                let tmp = "";
                let lines = error.split("\n");
                let seen = {};
                for (let i = 0; i < lines.length; i++) {
                    let line = lines[i].trim();
                    if (line == "") continue;
                    if (seen[line]) continue;
                    seen[line] = true;
                    tmp += line + "\n";
                }
		tmp = tmp.replace('Error:java.lang.RuntimeException:','');
		tmp = tmp.replace(/\\n/g,'<br>');
                error = tmp;
                error = HU.tag("pre", [STYLE, HU.css("white-space","nowrap","max-height","300px","overflow-y","auto","max-width","600px","overflow-x","auto")], error);
                msg += error;
            }
	    
	    msg = msg.replace(/\n/g,"<br>");
	    this.setErrorMessage(msg);
        },
        //callback from the pointData.loadData call
        clearCache: function() {},
	handleEventDataSelection: function(source, args) {
	    if(this.getAcceptEventDataSelection()) {
		this.pointDataLoaded(args.data,"",true);
	    }
	},
	getRequirement:function() {
	    return null;
	},
	updatePaginateLabel:function(skip, count,max) {
	    let paginate = this.getFilterPaginate();
	    let label = count;
	    if(skip!=null && skip>0)
		label = String(skip+1)+"-"+(count+skip);
	    else if(count<max)
		label = "1" +"-"+count;
	    label = this.getProperty("pageRequestLabel","Showing: ${count}").replace("${count}",label);
	    this.jq(ID_PAGE_LABEL).html(label);
	    let gotAll=false;
	    if(paginate) {
	    } else {
	    }
	    let buttons = "";
	    if(skip!=null && skip>0) {
		buttons+= HU.getIconImage("fa-step-backward",[ID,this.getDomId(ID_PAGE_PREV),CLASS,"display-page-button",TITLE,"View previous"])
	    }  else if(!gotAll) {
		buttons+= HU.getIconImage("fa-step-backward",[CLASS,"display-page-button fa-disabled"])
	    }
	    if(count<max) {
		buttons+= HU.getIconImage("fa-step-forward",[ID,this.getDomId(ID_PAGE_NEXT),CLASS,"display-page-button",TITLE,"View next"])
	    }  else if(!gotAll) {
		buttons+= HU.getIconImage("fa-step-forward",[CLASS,"display-page-button fa-disabled"])
	    }
	    this.jq(ID_PAGE_BUTTONS).html(buttons);
	    let _this = this;
	    this.jq(ID_PAGE_NEXT).click(()=>{
		if(!this.pageSkip)
		    this.pageSkip=0;
		if(paginate) {
		    this.pageSkip+= +this.getPageCount();
		    _this.haveCalledUpdateUI = false;
		    _this.dataFilterChanged();
		    _this.updatePaginateLabel(this.pageSkip, count,max);			
		} else {
		    this.pageSkip+=max;
		    this.reloadData();
		}
	    });
	    this.jq(ID_PAGE_PREV).click(()=>{
		if(!this.pageSkip)
		    this.pageSkip=0;
		if(paginate) {
		    this.pageSkip-= +this.getPageCount();
		    if(this.pageSkip<0) this.pageSkip=0;
		    _this.haveCalledUpdateUI = false;
		    _this.updatePaginateLabel(this.pageSkip, count,max);			
		    _this.dataFilterChanged();
		} else {
		    this.pageSkip-=max;
		    if(this.pageSkip<0) this.pageSkip=0;
		    this.reloadData();
		}
	    });		
	},

        addData: async function(pointData,skipUpdateUI) {
            let records = pointData.getRecords();
            if (records && records.length > 0) {
                this.hasElevation = records[0].hasElevation();
            } else {
                this.hasElevation = false;
            }
	    this.originalPointData=pointData;
	    if(!this.getApplyConvertAfter()) {
		pointData = this.convertPointData(pointData);
	    }
            this.dataCollection.addData(pointData);
	    try {
		if(!skipUpdateUI)
		    this.updateUI();
	    } catch(err) {}
            let entry = pointData.entry;
            if (entry == null && pointData.entryId) {
                await this.getRamadda().getEntry(pointData.entryId, e => {
                    entry = e
                });
            }
            if (entry) {
                pointData.entry = entry;
                this.addEntry(entry);
            }
        },

	convertPointData: function(pointData) {
	    let originalPointData = pointData;
	    let segments = this.getSegments();
	    if(segments) {
                let dataList = pointData.getRecords();
		let newData  =[];
		let header = [];
		newData.push(header);
		let rowIdx = 0; 
		//timeSegments="Obama;2008-02-01;2016-01-31,Trump;2016-02-01;2020-01-31"
		segments.forEach((segment,segmentIdx)=>{
		    let name = segment.name;
		    header.push(name);
		    let start = segment.start;
		    let end = segment.end;
		    let cnt = 1;
	    	    for (; rowIdx <dataList.length; rowIdx++) {
			let record = dataList[rowIdx];
			if(record.getTime()<start.getTime()) {
			    continue;
			}
			if(record.getTime()>end.getTime()) {
			    break;
			}
			let value = record.getValue(1);
			let row=null;
			if(cnt>=newData.length) {
			    row = [];
			    for(let sidx=0;sidx<segments.length;sidx++) row.push(NaN);
			    newData.push(row);
			} else {
			    row = newData[cnt];
			}
			row[segmentIdx] = value;
			cnt++;
		    }
		});
		pointData = convertToPointData(newData);
		pointData.entryId = originalPointData.entryId;
	    }

	    try {
		pointData = new CsvUtil().process(this, pointData, this.getProperty("convertData"));
	    } catch(exc) {
		this.handleError("Error:" + exc, exc);
		return null;
	    }
	    return pointData;
	},

        pointDataLoaded: function(pointData, url, reload) {
//	    this.logMsg("pointDataLoaded: "+ url);
	    if(!this.cacheUrl && pointData.cacheUrl) {
		this.cacheUrl = pointData.cacheUrl;
	    }


	    this.dataLoadFailed  =false;
	    let debug = displayDebug.pointDataLoaded;
	    this.clearProgress();
            this.inError = false;
            this.clearCache();
	    if(debug)
		this.logMsg("pointDataLoad:" + this.getId() + " " + this.type +" #records:" + pointData.getRecords().length);
	    if(debug)
		console.log("\tclearing last selected fields");
	    let records = pointData.getRecords();
	    this.lastSelectedFields = null;
            if (!reload) {
		if(debug) console.log("\tcalling addData");
                this.addData(pointData,true);
		//		if(debug) console.log("\tcalling checkSearchBar");
                this.checkSearchBar();
		//		if(debug) console.log("\done calling checkSearchBar");
            } else {
		pointData = this.convertPointData(pointData);
		if(!this.dataCollection)
		    this.dataCollection = new DataCollection();
		if(debug) console.log("\tcalling setData");
		this.dataCollection.setData(pointData);
	    }
	    let paginate = this.getFilterPaginate();
	    if(this.getProperty("pageRequest") || paginate) {
		if(debug) console.log("\tupdating pageRequest");
		let count = pointData.getRecords().length;
		let skip = null;
		let skipToks = url?url.match(/skip=([0-9]+)/):null;
		if(skipToks) skip = +skipToks[1];
		let max = +this.getProperty("max",5000);
		//		console.log("max:" +max +" count:" + count +" skip:" + skip);
		let label = count;
		if(skip!=null && skip>0)
		    label = String(skip+1)+"-"+(count+skip);
		else if(count==max)
		    label = "1" +"-"+count;
		let pageInfo = this.getProperty("pageRequestLabel","Showing: ${count}").replace("${count}",label) +" ";
		let gotAll = !skip &&  count<max;

		if(skip!=null && skip>0) {
		    pageInfo+= HU.getIconImage("fa-step-backward",[ID,this.getDomId(ID_PAGE_PREV),CLASS,"display-page-button",TITLE,"View previous"])
		}  else if(!gotAll) {
		    pageInfo+= HU.getIconImage("fa-step-backward",[CLASS,"display-page-button fa-disabled"])
		}
		if(count==max) {
		    pageInfo+= HU.getIconImage("fa-step-forward",[ID,this.getDomId(ID_PAGE_NEXT),CLASS,"display-page-button",TITLE,"View next"])
		}  else if(!gotAll) {
		    pageInfo+= HU.getIconImage("fa-step-forward",[CLASS,"display-page-button fa-disabled"])
		}
		this.jq(ID_PAGE_COUNT).html(pageInfo+"&nbsp;&nbsp;");
		this.jq(ID_PAGE_NEXT).click(()=>{
		    if(!this.pageSkip)
			this.pageSkip=0;
		    this.pageSkip+=max;
		    this.reloadData();
		});
		this.jq(ID_PAGE_PREV).click(()=>{
		    if(!this.pageSkip)
			this.pageSkip=0;
		    this.pageSkip-=max;
		    if(this.pageSkip<0) this.pageSkip=0;
		    this.reloadData();
		});		
	    }

            if (url != null) {
                this.jsonUrl = url;
            } else {
                this.jsonUrl = null;
            }
            if (!this.getDisplayReady()) {
		if(debug)console.log("pointDataLoaded: display not ready");
                return;
            }

	    
	    if(!this.getDateProps().dateFormat) {
                pointData.getRecordFields().forEach(f=>{
		    if(f.isFieldDate() && f.getId() == "year") {
			this.setProperty("dateFormat","yyyy");
		    }
		});
	    }
	    
	    this.haveCalledUpdateUI = false;
	    if(debug) console.log("\tcalling updateUI");
	    if(reload && this.getAnimation().getEnabled()) {
		this.initializeAnimation();
	    }

	    try {
		let requirement = this.getRequirement();
		if(requirement) {
//		    console.log("waiting on:" + requirement);
		    HU.waitForIt(requirement,()=>{
			this.updateUI({reload:reload});
		    });
		} else {
		    this.updateUI({reload:reload});
		}
	    } catch(err) {
		this.handleError("Error creating display:<br>" + err,err);
		return;
	    }
	    if (!reload || this.getPropagateDataReload()) {
                this.lastPointData = pointData;
		if(debug) console.log("\tcalling propagateEvent");		
                this.propagateEvent(DisplayEvent.pointDataLoaded, pointData);
            }
        },
        getHasDate: function(records) {
            let lastDate = null;
            this.hasDate = false;
            for (j = 0; j < records.length; j++) {
                let record = records[j];
                let date = record.getDate();
                if (date == null) {
                    continue;
                }
                if (lastDate != null && lastDate.getTime() != date.getTime()) {
                    this.hasDate = true;
                    break
                }
                lastDate = date;
            }
            return this.hasDate;
        },
        dateInRange: function(date, idx, debug) {
	    if(debug) {
		this.logMsg("dateInRange: date:" + date +" minDate:" + this.minDateObj +" maxDate:" + this.maxDateObj);
	    }

//	    if(this.minDateObj)console.log("index:" +this.minDateObj.index +" " +this.maxDateObj.index)
	    if(this.minDateObj &&this.minDateObj.isIndex) {
		if(debug)
		    console.dir('min',this.minDateObj.index,idx);
		if(idx<this.minDateObj.index) return false;
	    }
	    if(this.maxDateObj &&this.maxDateObj.isIndex) {
		if(debug)
		    console.dir('max',this.maxDateObj.index,idx);
		if(idx>this.maxDateObj.index) return false;
		return true;
	    }	    

            if (date != null) {
		if(this.dateRangeDoDay && this.minDateObj) {
		    if(date.getUTCFullYear()!=this.minDateObj.getUTCFullYear() ||
		       date.getUTCMonth()!=this.minDateObj.getUTCMonth() ||
 		       date.getUTCDate()!=this.minDateObj.getUTCDate())  {
			return false;
		    }
		    
		} else {
                    if (this.minDateObj != null && date.getTime() < this.minDateObj.getTime()) {
			if(debug) {
			    console.log("minDate: " + date +"   " + this.minDateObj);
			}
			return false;
                    }
                    if (this.maxDateObj != null && date.getTime() > this.maxDateObj.getTime()) {
			if(debug) {
			    let diff = (this.maxDateObj.getTime()-date.getTime())/1000;
			}
			return false;
		    }
                }
		
//		let str =  date.toUTCString() +" " +(date.getTime() < this.startDateObject.getTime());
		if (this.startDateObject != null && date.getTime() < this.startDateObject.getTime()) {
		    if(debug) {
//			console.log("    startDate:\n\t" + date.getTime() +"\n\t" + this.startDateObject.getTime());
		    }
//		    console.log("skip " + str);
                    return false;
                }
//		console.log("no skip " + str +" " + date);
                if (this.endDateObject != null && date.getTime() > this.endDateObject.getTime()) {
		    if(debug) {
			console.log("    endDate:\n\t" + date.getTime() +"\n\t" + this.endDateObject.getTime());
		    }
                    return false;
                }
            }
	    if(debug) console.log('ok');
            return true;
        },
        getPointData: function() {
	    if(this.pointData) return this.pointData;
            if (this.dataCollection.getList().length == 0) return null;
            return this.dataCollection.getList()[0];
        },
	getRecords: function() {
            let pointData = this.getData();
            if (pointData == null) return null;
            return  pointData.getRecords();
        },
        //get an array of arrays of data 
        getDataValues: function(obj) {
            if (obj.tuple) return obj.tuple;
            else if (obj.getData) return obj.getData();
            return obj;
        },
	indexToRecord: {},
	recordToIndex: {},
	findMatchingDates: function(date, records, within) {
	    if(!Utils.isDefined(within)) within=0;
	    let good = [];
	    let millis = date.getTime();
	    records.forEach(r=>{
		let rd = r.getDate();
		if(!rd) return;
		let diff = Math.abs(rd.getTime()-millis);
		if(diff<=within) {
		    good.push(r);
		}
	    });
	    return good;
	},
	findMatchingIndex: function(record) {
	    let fail = {index:-1,record:null};
	    if(!record) return fail;
	    let recordSelectField=this.getRecordSelectField();
	    if(recordSelectField) {
		let f = this.getFieldById(null, recordSelectField);
		if(!f)    return fail;
		let v = f.getValue(record);
		if(!Utils.isDefined(v)) return fail;
		if(!this.filteredRecords) return fail;
		for(let i=0;i<this.filteredRecords.length;i++) {
		    if(v == f.getValue(this.filteredRecords[i])) {
			return {index:i,record:this.filteredRecords[i]};
		    }
		}
		return fail;

	    }
	    let index = this.recordToIndex[record.getId()];
	    if(Utils.isDefined(index)) {
		return {index:index, record:this.indexToRecord[index]}
	    }
	    if(!record.hasDate()) return -1;
	    return this.findClosestDate(record.getDate());
	},
	findClosestDate:function(date) {
	    let records =this.filteredRecords;
	    if(!records) {
		records = [];
		for(i in this.indexToRecord) {
		    records.push(this.indexToRecord[i]);
		}
	    }
	    let closest;
	    let min  =0;
	    records.forEach((r,idx)=>{
		if(!r.hasDate()) {
		    return -1;
		}
		let diff = Math.abs(date.getTime()-r.getDate().getTime());
		if(!closest) {
		    min = diff;
		    closest = r;
		} else {
		    if(diff<min) {
			min = diff;
			closest = r;
		    }
		}
	    });
	    if(!closest) 
		return {index:-1, record:null}
	    return {index:this.recordToIndex[closest.getId()], record:closest};
	},
        makeDataArray: function(dataList) {
	    if(!dataList) return null;
            if (dataList.length == 0) return dataList;
            let data = [];
            if (dataList[0].getData) {
                for (let i = 0; i < dataList.length; i++) {
		    data.push(dataList[i].getData()[0]);
		}
            } else if (dataList[0].tuple) {
                for (let i = 0; i < dataList.length; i++) {
                    data.push(dataList[i].tuple);
		}
            } else {
                data = dataList;
            }
            return data;
        },

        printFields: function(label, fields) {
            console.log(label);
            if (!fields) {
                console.log("   null fields");
                return;
            }

            for (a in fields)
                console.log("   " + fields[a].getId());
        },
	makeIndexValue: function(indexField, value, offset) {
	    return value+offset;
	},
        getStandardData: function(fields, args) {
	    let defaultIndexName= this.getProperty("indexName", "Index");
	    if(!args) args = {};
	    let debug = displayDebug.getStandardData;
	    if(debug) console.log("getStandardData:" + this.type +"  fields:" + fields);
	    let showUnit  = this.getProperty("showUnit",this.getProperty("showUnitInSeries",true));
	    this.recordToIndex = {};
	    this.indexToRecord = {};
            let pointData = this.getPointData();
            let excludeZero = this.getProperty(PROP_EXCLUDE_ZERO, false);
            let excludeNan = this.getProperty(PROP_EXCLUDE_NAN, false);	    
            if (fields == null) {
                fields = pointData.getRecordFields();
		if(debug) console.log("\tgetRecordFields: " + fields.length);
            } else {
		//		if(debug) console.log("\tfields 2: " + fields.length);
	    }
            props = {
                makeObject: true,
                includeIndex: true,
                includeIndexIfDate: false,
                groupByIndex: -1,
                raw: false,
            };
            if (args != null) {
                $.extend(props, args);
            }



            let groupByIndex = props.groupByIndex;
            let groupByList = [];
 	    let groupByValues = {};

	    let groupByRecords = [];
	    let groupByDate = this.getProperty("groupByDate");
	    let groupByFill = this.getProperty("groupByFill");
	    let groupByDateMap = {};
	    let groupByDates = [];

            let dataList = [];
            //The first entry in the dataList is the array of names
            //The first field is the domain, e.g., time or index
            let fieldNames = [];
            let fieldsForTuple = [];	    
	    if(this.getProperty("binDate")) {
		if(debug)
		    console.log("binning date");
		let binType = this.getProperty("binType","total");
		let binCount = binType=="count";
		if(binCount) {
		    let f = [];
		    fields.forEach((field)=>{
			f.push(new RecordField({
			    index:0,
			    id:field.getId(),
			    label:this.getProperty("binDateLabel", this.getProperty("binCountLabel","Count")),
			    type:"double",
			    chartable:true
			}));
		    });
		    fields=f;
		}
	    }

	    let seenDate = false;
	    fields  = fields.filter(f=>{
		if(f.isFieldDate()) {
		    if(seenDate  && f.isRecordDate()) return null;
		    seenDate = true;
		}
		return f;
	    });
            for (i = 0; i < fields.length; i++) {
                let field = fields[i];
                if (field.isFieldNumeric() && field.isFieldDate()) {
                    //                        console.log("Skipping:" + field.getLabel());
                    //                        continue;
                }
                let name = field.getLabel(this);
                if (showUnit && field.getUnit() != null) {
                    name += " (" + field.getUnit() + ")";
                }
                //                    name = name.replace(/!!/g,"<br><hr>&nbsp;&nbsp;&nbsp;")
                name = name.replace(/!!/g, " -- ")
                fieldNames.push(name);
		fieldsForTuple.push(field);
            }
            if (props.makeObject) {
                dataList.push({
                    tuple: fieldNames,
		    fields:fieldsForTuple,
                    record: null
                });
            } else {
                dataList.push(fieldNames);
            }
            //console.log(fieldNames);



            groupByList.push("");
	    groupByRecords.push(null);
	    if(!this.minDateObj) {
		this.minDateObj = Utils.parseDate(this.minDate, false);
		if(debug)
		    console.log("getStandardData setting min date:" + this.minDateObj);
	    }
	    if(!this.minDateObj) {
		this.maxDateObj = Utils.parseDate(this.maxDate, true, this.minDateObj);
		if(debug)
		    console.log("getStandardData setting max date:" + this.maxDateObj);
	    }

            if (this.minDateObj == null && this.maxDateObj != null) {
                this.minDateObj = Utils.parseDate(this.minDate, false, this.maxDateObj);
            }



            let offset = 0;
            if (Utils.isDefined(this.offset)) {
                offset = parseFloat(this.offset);
            }

            let nonNullRecords = 0;
            let records = args.records?args.records:this.filterData();
	    if(debug)
		console.log("getStandardData #fields:" + fields.length +" #records:" + records.length);
            let allFields = pointData.getRecordFields();

            //Check if there are dates and if they are different
            this.hasDate = this.getHasDate(records);
            let date_formatter = null;
            let rowCnt = -1;
            let indexField = this.getFieldById(null,this.getProperty("indexField"));
	    let t1 = new Date();
            for (let rowIdx = 0; rowIdx < records.length; rowIdx++) {
                let record = records[rowIdx];
                let date = record.getDate();
                if (!this.dateInRange(date,rowIdx)) {
		    continue;
		}
                rowCnt++;
		this.recordToIndex[record.getId()] = rowCnt;
		this.indexToRecord[rowCnt] = record;
                let values = [];
                if (props && (props.includeIndex || props.includeIndexIfDate)) {
                    let indexName = null;
                    if (indexField) {
			let value = this.makeIndexValue(indexField,record.getValue(indexField.getIndex()),rowIdx);
                        values.push(value);
                        indexName = indexField.getLabel();
                    } else {
                        if (this.hasDate) {
			    let dttm = this.getDateValue(date, date_formatter);
                            values.push(dttm);
                            indexName = "Date";
                        } else {
                            if (!props.includeIndexIfDate) {
                                values.push(rowIdx);
				indexName = defaultIndexName;
                            }
                        }
                    }
                    if (indexName != null && rowCnt == 0) {
                        fieldNames.unshift(indexName);
                    }
                }

                let allNull = true;
                let allZero = true;
                let hasNumber = false;
		let hasNan = false;
                for (let fieldIdx = 0; fieldIdx < fields.length; fieldIdx++) {
                    let field = fields[fieldIdx];
                    if (field.isFieldNumeric() && field.isFieldDate()) {
                        //                            continue;
                    }
                    let value = record.getValue(field.getIndex());
                    if (offset != 0) {
                        value += offset;
                    }

		    //		    if(debug&& rowIdx<10)
		    //			console.log("   v:"+ value);
                    if (value != null) {
                        allNull = false;
                    }
                    if (typeof value == 'number') {
			if(excludeNan &&  isNaN(value))  {
			    hasNan=true;
			}
                        hasNumber = true;
                        if (value != 0) {
                            allZero = false;
                        }
                    }
                    if (field.isFieldDate()) {
                        value = this.getDateValue(value, date_formatter);
                    }
                    values.push(value);
		}


		if(hasNan) {
		    continue;
		}
                if (hasNumber && allZero && excludeZero) {
		    //		    console.log(" skipping due to zero: " + values);
                    continue;
                }
                //TODO: when its all null values we get some errors
                if (groupByIndex >= 0) {
		    let value = record.getValue(groupByIndex);
		    if(!groupByValues[value]) groupByValues[value] = true;
		    if(groupByDate)
			groupByList.push(record.getDate() +"-"+value);
		    else
			groupByList.push(value);
		    groupByRecords.push(record);
                }
                if (props.makeObject)
                    dataList.push({
                        tuple: values,
                        record: record
                    });
                else
                    dataList.push(values);
                if (!allNull) {
                    nonNullRecords++;
                }
	    }

	    let t2= new Date();
//	    console.log("#records:" + records.length);
//	    Utils.displayTimes("chart.standardData loop:",[t1,t2], true);
            if (nonNullRecords == 0) {
		//		console.log("Num non null:" + nonNullRecords);
		console.log(this.type+" no nonNull records");
		return [];
            }

            if (groupByIndex >= 0) {
                let groupToTuple = {};
                let groups = [];
                let agg = [];
                let title = [];
		let groupByCount = this.getProperty("groupByCount");
                title.push(props.groupByField.getLabel());
		if(groupByCount) {
		    title.push(this.getProperty("groupByCountLabel", "Count"));
		} else {
                    for (let fieldIdx = 0; fieldIdx < fields.length; fieldIdx++) {
			let field = fields[fieldIdx];
			if (field.getIndex() != groupByIndex) {
                            title.push(field.getLabel());
			}
                    }
		}
		//                agg.push(title);
		let groupByValueTuples = {};
                for (let rowIdx = 0; rowIdx < dataList.length; rowIdx++) {
                    let data = this.getDataValues(dataList[rowIdx]);
                    if (rowIdx == 0) {
                        continue;
                    }
                    let groupBy = groupByList[rowIdx];
		    let record = groupByRecords[rowIdx];
		    let groupByValue = record.getValue(groupByIndex);
                    let tuple = groupToTuple[groupBy];
                    if (tuple == null) {
                        tuple = new Array();
                        groups.push(groupBy);
			if(groupByDate) {
			    let dateList = groupByDateMap[record.getDate()];
			    if(dateList == null) {
				groupByDateMap[record.getDate()] = dateList = [];
				groupByDates.push(record.getDate());
			    }
			    dateList.push(tuple);
			}
			if(!groupByValueTuples[groupByValue]) groupByValueTuples[groupByValue] = [];
			groupByValueTuples[groupByValue].push(tuple)
			tuple.record = record;
                        agg.push(tuple);
                        tuple.push(groupByValue);
			if(groupByCount) {
			    tuple.push(0);
			} else {
                            for (let fieldIdx = 0; fieldIdx < fields.length; fieldIdx++) {
				let field = fields[fieldIdx];
				if (field.getIndex() == groupByIndex) {
                                    continue;
				}
				tuple.push(0);
                            }
			}
                        groupToTuple[groupBy] = tuple;
                    }
                    let index = 0;
		    if(groupByCount) {
			tuple[1]++;
			continue;
		    }
                    for (let fieldIdx = 0; fieldIdx < fields.length; fieldIdx++) {
                        let field = fields[fieldIdx];
                        if (field.getIndex() == groupByIndex) {
                            continue;
                        }
                        let dataValue = data[fieldIdx];
                        index++;
                        if (Utils.isNumber(dataValue)) {
                            if (typeof tuple[index] == "string") {
                                tuple[index] = 0;
                            }
                            tuple[index] += parseFloat(dataValue);
                        } else {
                            if (tuple[index] == 0) {
                                tuple[index] = "";
                            }
                            let s = tuple[index];
                            if (!Utils.isDefined(s)) {
                                s = "";
                            }
                            //Only concat string values for a bit
                            if (s.length < 150) {
                                if (!Utils.isDefined(dataValue)) {
                                    dataValue = "";
                                }
                                let sv = ("" + dataValue);
                                if (s.indexOf(sv) < 0) {
                                    if (s != "") {
                                        s += ", ";
                                    }
                                    s += sv;
                                    tuple[index] = s;
                                }
                            }

                        }
                    }
		}

		if(groupByFill) {
		    groupByDates.forEach(date=>{
			let dateList = groupByDateMap[date];
			let seen = {};
			dateList.forEach(tuple =>{
			    seen[tuple[0]] = true;
			});
			for(v in groupByValues) {
			    if(!seen[v]) {
				seen[v] = true;
				let tuple = [v,0];
				tuple.date = date;
				agg.push(tuple);
			    }
			}
		    });
		}

		if(this.getProperty("groupBySort")) {
		    agg.sort(function(a,b) {return b[1]-a[1]});
		}
		if(this.getProperty("groupByMaxNumber")) {
		    let cnt = +this.getProperty("groupByMaxNumber");
		    agg = agg.filter((t,idx)=>{
			return idx<cnt;
			
		    });

		}
		let tmp = [];
		tmp.push(title);
		agg.forEach(t=>tmp.push(t));
                return tmp;
            }

	    //	    console.log("display.getStandardData returning "+ dataList.length);
	    if(this.getProperty("movingAverageSteps")) {
		let steps = +this.getProperty("movingAverageSteps");
		let tmp = [dataList[i]];
		let isNumeric = dataList[1].tuple.map((v,idx)=>{return Utils.isNumber(v);});
		dataList.forEach((o,rowIdx)=>{
		    if(rowIdx==0) return;
		    let tuple = Utils.mergeLists(o.tuple);
		    tmp.push({
			record:o.record,
			tuple:tuple});
		    tuple[0] = "x"; tuple[1] = 5;
		    tuple.forEach((v,colIdx)=>{
			if(!isNumeric[colIdx]) return;
			tuple[colIdx]=5;
		    });
		});
		dataList = tmp;
	    }
            return dataList;
        },
        isGoogleLoaded: function() {
            if ((typeof google === 'undefined') || (typeof google.visualization === 'undefined') || (typeof google.visualization.DateFormat === 'undefined')) {
                return false;
            }
            return true;
        },
        initDateFormats: function() {
            if (!this.isGoogleLoaded()) {
                //                    console.log("google hasn't loaded");
                return false;
            }
            if (this.fmt_yyyy) return true;
            let tz = 0;
            this.timezone = this.getTimeZone();
            if (Utils.isDefined(this.timezone)) {
                tz = parseFloat(this.timezone);
            }
            this.fmt_yyyymmddhhmm = new google.visualization.DateFormat({
                pattern: "yyyy-MM-dd HH:mm Z",
                timeZone: tz
            });
            this.fmt_yyyymmdd = new google.visualization.DateFormat({
                pattern: "yyyy-MM-dd",
                timeZone: tz
            });
            this.fmt_yyyy = new google.visualization.DateFormat({
                pattern: "yyyy",
                timeZone: tz
            });
            return true;
        },
        getDateValue: function(arg) {
	    if(!arg) return {v:null,f:'NA'};
            if (!this.initDateFormats()) {
                return arg;
            }
            if (!(typeof arg == "object")) {
                date = new Date(arg);
            } else {
                date = arg;
            }
	    if(isNaN(date.getUTCFullYear())) return {v:date,f:"NA"};


	    if(this.getDateProps().dateFormatDaysAgo) {
		let now = new Date();
		let diff = Math.round((now.getTime()-date.getTime())/1000/60/60/24);
		return {v:date,f:diff+" days ago"};
	    }

            return  {
                v: date,
                f: this.formatDate(date)
            };
        },
        applyFilters: function(record, values) {
	    this.filters.forEach(filter=>{
                if (!filter.isRecordOk(record)) {
                    return false;
                }
            });
            return true;
        }
    });

    let filter = this.getProperty(PROP_DISPLAY_FILTER);
    if (filter != null) {
        //semi-colon delimited list of filter definitions
        //display.filter="filtertype:params;filtertype:params;
        //display.filter="month:0-11;
        let filters = filter.split(";");
        for (let i = 0; i < filters.length; i++) {
            filter = filters[i];
            let toks = filter.split(":");
            let type = toks[0];
            if (type == "month") {
                this.filters.push(new MonthFilter(toks[1]));
            } else {
                this.handleError("Unknown filter:" + type);
            }
        }
    }
}




function DisplayGroup(argDisplayManager, argId, argProperties, type) {
    const LAYOUT_TABLE = "table";
    const LAYOUT_HTABLE = "htable";
    const LAYOUT_TABS = "tabs";
    const LAYOUT_COLUMNS = "columns";
    const LAYOUT_ROWS = "rows";
    const SUPER = new RamaddaDisplay(argDisplayManager, argId, type||"group", argProperties);
    RamaddaUtil.inherit(this, SUPER);
    let myProps = [
	{label:'Group Properties'},
	{p:PROP_LAYOUT_TYPE,ex:Utils.join([LAYOUT_TABLE,LAYOUT_HTABLE,LAYOUT_TABS,LAYOUT_COLUMNS,LAYOUT_ROWS],",")},
	{p:PROP_LAYOUT_COLUMNS,d:1},
	{p:'targetDiv',tt:'Div id to put the displays in for this group'},
	]

    displayDefineMembers(this, myProps, {
        displays: [],
        layout: this.getProperty(PROP_LAYOUT_TYPE, LAYOUT_TABLE),
        columns: this.getProperty(PROP_LAYOUT_COLUMNS, 1),
        isLayoutColumns: function() {
            return this.layout == LAYOUT_COLUMNS;
        },
        getWikiText: function() {
            let attrs = ["layoutType", this.layout,
			 "layoutColumns",
			 this.columns,
			 "showMenu",
			 "false",
			 "groupDiv",			 
			 "$entryid_maindiv"
			];
            let wiki = "";
            wiki += "<div id=\"{{entryid}}_maindiv\"></div>\n";
            wiki += "{{group " + HU.attrs(attrs) + "}}\n"
            return wiki;
        },

        walkTree: function(func, data) {
            for (let i = 0; i < this.displays.length; i++) {
                let display = this.displays[i];
                if (display.walkTree != null) {
                    display.walkTree(func, data);
                } else {
                    func.call(data, display);
                }
            }
        },
        collectEntries: function(entries) {
            if (entries == null) entries = [];
            for (let i = 0; i < this.displays.length; i++) {
                let display = this.displays[i];
                if (display.collectEntries != null) {
                    display.collectEntries(entries);
                } else {
                    let displayEntries = display.getEntries();
                    if (displayEntries != null && displayEntries.length > 0) {
                        entries.push({
                            source: display,
                            entries: displayEntries
                        });
                    }
                }
            }
            return entries;
        },
        isLayoutRows: function() {
            return this.layout == LAYOUT_ROWS;
        },

        getPosition: function() {
            for (let i = 0; i < this.displays.length; i++) {
                let display = this.displays[i];
                if (display.getPosition) {
                    return display.getPosition();
                }
            }
        },
        getDisplays: function() {
            return this.displays;
        },
        notifyEvent: function(event, source, data) {
            let displays = this.getDisplays();
	    let debug = false;
	    //debug=event.name=='filteredDataChanged';

	    if(debug)
		console.log('notifyEvent',event,displays.length);
	    let group = (source!=null&&source.getProperty?source.getProperty(event.shareGroup):"");
	    if(displayDebug.notifyEvent)
		console.log("displayManager.notifyEvent:" + event);

            for (let i = 0; i < this.displays.length; i++) {
                let display = this.displays[i];
                if (display == source) {
		    if(debug) console.log('\tskipping source');
                    continue;
                }
		let acceptGroup = (display!=null&&display.getProperty?display.getProperty(event.acceptGroup):"");
		if(group) {
		    if(acceptGroup!=group)  {
			if(debug || displayDebug.notifyEvent)
			    console.log("\t" + display.type+" not in group:" + group);
			continue;
		    }
		} else if(acceptGroup) {
		    if(debug||displayDebug.notifyEvent)
			console.log("\t" + display.type+" incoming not in accept group:" + acceptGroup);
		    continue;
		}
		if(!display.acceptEvent(event,  event.default)) {
		    if(debug||displayDebug.notifyEvent)
			console.log("\t" + display.type+" not accepting");
		    continue;
		}
//		console.log("notifyEvent:" + display.type+" " + event +" group:" + group);
                let eventSource = display.getEventSource();
                if (eventSource != null && eventSource.length > 0) {
                    if (eventSource != source.getId() && eventSource != source.getName()) {
                        continue;
                    }
                }
		if(debug ||displayDebug.notifyEvent)
		    console.log("\t" + display.type+" calling notifyEvent:" + event);
                display.notifyEvent(event, source, data);
            }
        },
        getDisplaysToLayout: function() {
            let result = [];
            for (let i = 0; i < this.displays.length; i++) {
                if (this.displays[i].getIsLayoutFixed()) {
		    continue;
		}
                result.push(this.displays[i]);
            }
            return result;
        },
        pageHasLoaded: function(display) {
	    //Maybe we don't need to do this since the displays get called globally
	    //            for (let i = 0; i < this.displays.length; i++) {
	    //                this.displays[i].setDisplayReady(true);
	    //            }
            this.doLayout();
        },
        addDisplay: function(display) {
            this.displays.push(display);
            if (display.getIsLayoutFixed()) {
		display.initDisplay();
	    } else {
		if (Utils.getPageLoaded()) {
                    this.doLayout();
		}
	    }
        },
        layoutChanged: function(display) {
            this.doLayout();
        },
        removeDisplay: function(display) {
	    Utils.removeItem(this.displays,display);
            this.doLayout();
        },
        doLayout: function() {
	    let html = "";
            let colCnt = 100;
            let displaysToLayout = this.getDisplaysToLayout();
            let displaysToPrepare = this.displays;
	    displaysToPrepare.forEach(display=>{
                if (display.prepareToLayout != null) {
                    display.prepareToLayout();
                }
            });

            let weightIdx = 0;
            let weights = null;
            if (typeof this.weights != "undefined") {
                weights = this.weights.split(",");
            }

            for (let i=0; i < displaysToLayout.length; i++) {
                let divId = HU.getUniqueId("divid_");
                let div =  HU.div([CLASS, " display-wrapper",ID,divId],"");
                displaysToLayout[i].setProperty(PROP_DIVID,divId);
                displaysToLayout[i].layoutDiv=div;
            }
            let tabId = HU.getUniqueId("tabs_");
            if (this.layout == LAYOUT_TABLE) {
                if  (displaysToLayout.length== 1) {
                    html += displaysToLayout[0].layoutDiv;
                } else {
                    let weight = 12 / this.columns;
                    let i = 0;
                    let map = {};
                    for (; i < displaysToLayout.length; i++) {
                        let d = displaysToLayout[i];
                        if (Utils.isDefined(d.column) && Utils.isDefined(d.row) && d.columns >= 0 && d.row >= 0) {
                            let key = d.column + "_" + d.row;
                            if (map[key] == null) map[key] = [];
                            map[key].push(d);
                        }
                    }

                    i = 0;
                    for (; i < displaysToLayout.length; i++) {
                        colCnt++;
                        if (colCnt >= this.columns) {
                            if (i > 0) {
                                html += HU.close(TAG_DIV);
                            }
                            html += HU.open("div", [CLASS, "row"]);
                            colCnt = 0;
                        }
                        let weightToUse = weight;
                        if (weights != null) {
                            if (weightIdx >= weights.length) {
                                weightIdx = 0;
                            }
                            weightToUse = weights[weightIdx];
                            weightIdx++;
                        }
                        html += HU.div([CLASS, "col-md-" + weightToUse + " display-wrapper display-cell"], displaysToLayout[i].layoutDiv);
			html+="\n";
		    }

                    if (i > 0) {
                        html += HU.close(TAG_DIV);
                    }
                }
	    } else if (this.layout == LAYOUT_HTABLE) {
                if  (displaysToLayout.length== 1) {
                    html += displaysToLayout[0].layoutDiv;
                } else {
                    let percent = Math.round((100 / this.columns))+"%";
                    let i = 0;
		    html+=HU.open(TABLE,[ATTR_WIDTH,'100%']);
		    let colCnt = 100;
                    for (let i =0;i < displaysToLayout.length; i++) {
                        colCnt++;
                        if (colCnt >= this.columns) {
                            if (i > 0) {
                                html += HU.close(TAG_TR);
                            }
                            html += HU.open("tr", ["valign", "top"]);
			    html+="\n";
                            colCnt = 0; 
                        }
			//Add in the max-width so the width of the td doesn't exceed the percent value
                        html += HU.td([ATTR_STYLE,"max-width:300px;", ATTR_WIDTH,percent], displaysToLayout[i].layoutDiv);
			html+="\n";
		    }
                    if (i > 0) {
                        html += HU.close(TAG_TR);
                    }
                }
            } else if (this.layout == LAYOUT_TABS) {
                html += HU.open(TAG_DIV, [ID, tabId, CLASS, "ui-tabs"]);
                html += HU.open(TAG_UL, []);
                let hidden = "";
                let cnt = 0;
                for (let i = 0; i < displaysToLayout.length; i++) {
                    let display = displaysToLayout[i];
                    let label = display.getTitle(false);
                    if (label.length > 20) {
                        label = label.substring(0, 19) + "...";
                    }
                    html += HU.tag(TAG_LI, [], HU.tag(TAG_A, ["href", "#" + tabId + "-" + cnt], label));
                    hidden += HU.div([ID, tabId + "-" + cnt, CLASS, "ui-tabs-hide"], display.layoutDiv);
                    cnt++;
                }
                html += HU.close(TAG_UL);
                html += hidden;
                html += HU.close(TAG_DIV);
            } else if (this.layout == LAYOUT_ROWS) {
                let rows = [];
                for (let i = 0; i < displaysToLayout.length; i++) {
                    let display = displaysToLayout[i];
                    let row = display.getRow();
                    if (("" + row).length == 0) row = 0;
                    while (rows.length <= row) {
                        rows.push([]);
                    }
                    rows[row].push(display.layoutDiv);
                }
                for (let i = 0; i < rows.length; i++) {
                    let cols = rows[i];
                    let width = Math.round(100 / cols.length) + "%";
                    html += HU.open(TAG_TABLE, ["border", "0", "width", "100%", "cellpadding", "0", "cellspacing", "0"]);
                    html += HU.open(TAG_TR, ["valign", "top"]);
                    for (let col = 0; col < cols.length; col++) {
                        let cell = cols[col];
                        cell = HU.div([CLASS, "display-cell"], cell);
                        html += HU.tag(TAG_TD, ["width", width], cell);
                    }
                    html += HU.close(TAG_TR);
                    html += HU.close(TAG_TABLE);
                }
            } else if (this.layout == LAYOUT_COLUMNS) {
                let cols = [];
                for (let i = 0; i < displaysToLayout.length; i++) {
                    let display = displaysToLayout[i];
                    let column = display.getColumn();
                    //                        console.log("COL:" + column);
                    if (("" + column).length == 0) column = 0;
                    while (cols.length <= column) {
                        cols.push([]);
                    }
                    cols[column].push(display.layoutDiv);
                    //                        cols[column].push("HTML");
                }
                html += HU.open(TAG_DIV, [CLASS, "row"]);
                let width = Math.round(100 / cols.length) + "%";
                let weight = 12 / cols.length;
                for (let i = 0; i < cols.length; i++) {
                    let rows = cols[i];
                    let contents = "";
                    for (let j = 0; j < rows.length; j++) {
                        contents += rows[j];
                    }
                    let weightToUse = weight;
                    if (weights != null) {
                        if (weightIdx >= weights.length) {
                            weightIdx = 0;
                        }
                        weightToUse = weights[weightIdx];
                        weightIdx++;
                    }
                    html += HU.div([CLASS, "col-md-" + weightToUse], contents);
                }
                html += HU.close(TAG_DIV);
            } else {
                html += "Unknown layout:" + this.layout;
            }

	    //If we don't  have any displays to show then hide us
	    if(!this.getShowMenu() && displaysToLayout.length==0) {
		//TODO: This hides the change entry group menu 
//		$("#" + this.getId()).hide();
	    } else {
		$("#" + this.getId()).show();
	    }
	    let div = this.getGroupDiv();
	    if(div.length>0) {
		div.html(html);
	    } else {
		this.writeHtml(ID_DISPLAYS, html);
	    }
            if (this.layout == LAYOUT_TABS) {
                $("#" + tabId).tabs({activate: HtmlUtil.tabLoaded});
            }
            this.initDisplays();
        }, 
	initDisplays: function() {
	    this.getDisplaysToLayout().forEach(display=>{
		try {
                    display.initDisplay();
		} catch (e) {
		    display.handleError("Error creating display:<br>" + e,e);
		}
            });
	},
        displayData: function() {},
        setLayout: function(layout, columns) {
            this.layout = layout;
            if (columns) {
                this.columns = columns;
            }
            this.doLayout();
        },
        askMinZAxis: function() {
            let v = prompt("Minimum axis value", "0");
            if (v != null) {
                v = parseFloat(v);
                for (let i = 0; i < this.displays.length; i++) {
                    let display = this.displays[i];
                    if (display.setMinZAxis) {
                        display.setMinZAxis(v);
                    }
                }
            }
        },

        askMaxZAxis: function() {
            let v = prompt("Maximum axis value", "0");
            if (v != null) {
                v = parseFloat(v);
                for (let i = 0; i < this.displays.length; i++) {
                    let display = this.displays[i];
                    if (display.setMaxZAxis) {
                        display.setMaxZAxis(v);
                    }
                }
            }
        },

        askMinDate: function() {
            let d = this.minDate;
            if (!d) d = "1950-0-0";
            this.minDate = prompt("Minimum date", d);
            if (this.minDate != null) {
                for (let i = 0; i < this.displays.length; i++) {
                    let display = this.displays[i];
                    if (display.setMinDate) {
                        display.setMinDate(this.minDate);
                    }
                }
            }
        },

        askMaxDate: function() {
            let d = this.maxDate;
            if (!d) d = "2020-0-0";
            this.maxDate = prompt("Maximum date", d);
            if (this.maxDate != null) {
                for (let i = 0; i < this.displays.length; i++) {
                    let display = this.displays[i];
                    if (display.setMaxDate) {
                        display.setMaxDate(this.maxDate);
                    }
                }
            }
        },


        titlesOff: function() {
            for (let i = 0; i < this.displays.length; i++) {
                this.displays[i].setShowTitle(false);
            }
        },
        titlesOn: function() {
            for (let i = 0; i < this.displays.length; i++) {
                this.displays[i].setShowTitle(true);
            }
        },
        detailsOff: function() {
            for (let i = 0; i < this.displays.length; i++) {
                this.displays[i].setShowDetails(false);
            }
            this.doLayout();
        },
        detailsOn: function() {
            for (let i = 0; i < this.displays.length; i++) {
                this.displays[i].setShowDetails(true);
            }
            this.doLayout();
        },

        deleteAllDisplays: function() {
            this.displays = [];
            this.doLayout();
        },
        moveDisplayUp: function(display) {
            let index = this.displays.indexOf(display);
            if (index <= 0) {
                return;
            }
            this.displays.splice(index, 1);
            this.displays.splice(index - 1, 0, display);
            this.doLayout();
        },
        moveDisplayDown: function(display) {
            let index = this.displays.indexOf(display);
            if (index >= this.displays.length) {
                return;
            }
            this.displays.splice(index, 1);
            this.displays.splice(index + 1, 0, display);
            this.doLayout();
        },

        moveDisplayTop: function(display) {
            let index = this.displays.indexOf(display);
            if (index >= this.displays.length) {
                return;
            }
            this.displays.splice(index, 1);
            this.displays.splice(0, 0, display);
            this.doLayout();
        },


    });

}



/*
 */
function RamaddaFieldsDisplay(displayManager, id, type, properties) {
    const SUPER = new RamaddaDisplay(displayManager, id, type, properties);
    defineDisplay(this, SUPER, [], {
        needsData: function() {
            return true;
        },
        initDisplay: function() {
            SUPER.initDisplay.call(this);
            if (this.needsData()) {
		if(this.useDisplayMessage()) {
                    this.setDisplayMessage(this.getLoadingMessage());
		} else {
                    this.setContents(this.getLoadingMessage());
		}
            }
            this.callUpdateUI();
        },
        updateUI: function(args) {
            this.addFieldsCheckboxes();
        },
        getWikiAttributes: function(attrs) {
            SUPER.getWikiAttributes.call(this, attrs);
            if (this.lastSelectedFields) {
                attrs.push(PROP_FIELDS);
                let v = "";
                for (let i = 0; i < this.lastSelectedFields.length; i++) {
                    v += this.lastSelectedFields[i].getId();
                    v += ",";
                }
                attrs.push(v);
            }
        },
        initDialog: function() {
            SUPER.initDialog.call(this);
            this.addFieldsCheckboxes();
        },
        getDialogContents: function(tabTitles, tabContents) {
            let height = "600";
//            let html = HU.div([ATTR_ID, this.getDomId(ID_FIELDS), STYLE, HU.css("overflow-y","auto","max-height", height + "px")], "");
//            tabTitles.push("Fields");
//            tabContents.push(html);
            SUPER.getDialogContents.call(this, tabTitles, tabContents);
        },
        handleEventFieldsSelected: function(source, fields) {
	    let tmp = [];
	    fields.forEach(f=>{
		let fieldId = f.getId?f.getId():f;
		f = this.getFieldById(null, fieldId);
		if(f) tmp.push(f);
	    });
	    fields=tmp;
//	    console.log("fields before:" + this.getSelectedFields());
//	    console.log("fields after:" + fields);

            this.userHasSelectedAField = true;
            this.overrideFields = null;
            this.removeProperty(PROP_FIELDS);
            this.setSelectedFields(fields);
            this.fieldSelectionChanged();
        },

        getFieldsToSelect: function(pointData) {
            return pointData.getRecordFields();
        },
        canDoMultiFields: function() {
            return true;
        }
    })
}




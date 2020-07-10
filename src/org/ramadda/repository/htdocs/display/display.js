/**
   Copyright 2008-2020 Geode Systems LLC
*/


const displayDebug = {
    getProperty:false,
    handleEventPropertyChanged:false,
    getSelectedFields:false,
    filterData:false,
    getStandardData:false,
    makeDataTable:false,
    checkSearchBar:false,
    handleNoData:false,
    pointDataLoaded:false,
    displayMapUpdateUI:false,
    displayMapCreateMap:false,
    displayMapAddPoints:false,
    loadPointJson:false,
    groupBy:false,
    gridPoints:false,

}


const CATEGORY_CHARTS = "Basic Charts";
const CATEGORY_TABLE = "Tables";
const CATEGORY_MISC = "Misc Charts";
const CATEGORY_MAPS_IMAGES = "Maps and Images";
const CATEGORY_RADIAL_ETC = "Radial, trees, etc";
const CATEGORY_TEXT = "Text Displays";
const CATEGORY_ENTRIES = "Entry Displays";
const CATEGORY_CONTROLS = "Controls";
const DISPLAY_CATEGORIES = [CATEGORY_CHARTS,CATEGORY_TABLE,CATEGORY_MAPS_IMAGES,CATEGORY_MISC,CATEGORY_TEXT,CATEGORY_RADIAL_ETC,CATEGORY_CONTROLS,CATEGORY_ENTRIES];



//Ids of DOM components
const ID_BOTTOM = "bottom";
const ID_COLORTABLE = "colortable";
const ID_LEGEND = "legend";
const ID_FIELDS = "fields";
const ID_HEADER = "header";
const ID_HEADER1 = "header1";
const ID_HEADER2 = "header2";
const ID_HEADER2_PREFIX = "header2prefix";
const ID_HEADER2_PREPREFIX = "header2preprefix";
const ID_HEADER2_SUFFIX = "header2suffix";
const ID_FILTERBAR = "filterbar";
const ID_TITLE = ATTR_TITLE;
const ID_TITLE_EDIT = "title_edit";
const ID_LEFT = "left";
const ID_RIGHT = "right";
const ID_TOP = "top";
const ID_TOP_RIGHT = "topright";
const ID_TOP_LEFT = "topleft";
const ID_DETAILS = "details";
const ID_DISPLAY_CONTENTS = "contents";
const ID_DISPLAY_TOP = "top";
const ID_DISPLAY_BOTTOM = "bottom";
const ID_GROUP_CONTENTS = "group_contents";
const ID_DETAILS_MAIN = "detailsmain";
const ID_GROUPBY_FIELDS= "groupdbyfields";
const ID_TOOLBAR = "toolbar";
const ID_TOOLBAR_INNER = "toolbarinner";
const ID_LIST = "list";
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
const ID_FILTER_HIGHLIGHT = "filterhighlight";
const ID_FILTER_DATE = "filterdate";
const ID_ENTRIES_MENU = "entries_menu";
const ID_ENTRIES_PREV = "entries_prev";
const ID_ENTRIES_NEXT = "entries_next";
const PROP_DISPLAY_FILTER = "displayFilter";
const PROP_EXCLUDE_ZERO = "excludeZero";
const PROP_EXCLUDE_NAN = "excludeUndefined";
const PROP_DIVID = "divid";
const PROP_FIELDS = "fields";
const PROP_LAYOUT_HERE = "layoutHere";
const PROP_HEIGHT = "height";
const PROP_WIDTH = "width";
const PROP_FILTER_VALUE = "fitlerValue";
const HIGHLIGHT_COLOR = "yellow";
const RECORD_INDEX = "recordIndex";


let globalDisplayCount = 0;
function addGlobalDisplayProperty(name, value) {
    if (window.globalDisplayProperties == null) {
        window.globalDisplayProperties = {};
    }
    if(value==="true") value = true;
    else if(value==="false") value=false;
    window.globalDisplayProperties[name] = value;
}


function getGlobalDisplayProperty(name) {
    if (window.globalDisplayProperties == null) {
        return null;
    }
    return window.globalDisplayProperties[name];
}


function addRamaddaDisplay(display) {
    Utils.addDisplay(display);
    display.displayCount=globalDisplayCount++;
}

async function ramaddaDisplaySetSelectedEntry(entryId, displays) {
    await getGlobalRamadda().getEntry(entryId, e => {
	displays = displays||Utils.displaysList;
	if(displays) {
		displays.forEach(d=>{
		    if(d.setEntry) d.setEntry(e);
		});
	}
    });
}


function ramaddaDisplayCheckLayout() {
    Utils.displaysList.forEach(d=>{
        if (d.checkLayout) {
            d.checkLayout();
        }
    });
}


function getRamaddaDisplay(id) {
    return Utils.displaysMap[id];
}

function removeRamaddaDisplay(id) {
    var display = getRamaddaDisplay(id);
    if (display) {
        display.removeDisplay();
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



/**
   Base class for all displays oriented things
*/
function DisplayThing(argId, argProperties) {
    
    if (argProperties == null) {
        argProperties = {};
    }


    //check for booleans as strings
    for (var i in argProperties) {
        if (typeof argProperties[i] == "string") {
            if (argProperties[i] == "true") argProperties[i] = true;
            else if (argProperties[i] == "false") argProperties[i] = false;
        }
    }


    //Now look for the structured foo.bar=value
    for (var key in argProperties) {
        var toks = key.split(".");
        if (toks.length <= 1) {
            continue;
        }
	//var map = argProperties;
	// Don't this for now as it screws up doing something like colorTable.field=...
	let map = {};
        //graph.axis.foo=bar
        var v = argProperties[key];
        if (v == "true") v = true;
        else if (v == "false") v = false;
        for (var i = 0; i < toks.length; i++) {
            var tok = toks[i];
            if (i == toks.length - 1) {
                map[tok] = v;
                break;
            }
            var nextMap = map[tok];
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
    //IMPORTANT: might breqak a bunch of displays
    //    $.extend(this, argProperties);

    RamaddaUtil.defineMembers(this, {
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
		var entries  = argProperties.entryCollection.split(",");
		this.changeEntries = [];
		let enums = [];
		entries.forEach(t=>{
		    var toks = t.split(":");
		    this.changeEntries.push(toks[0]);
		    enums.push([toks[0],toks[1]]);
		});
		var prev = HU.span([CLASS,"display-changeentries-button", TITLE,"Previous entry", ID, this.getDomId(ID_ENTRIES_PREV), TITLE,"Previous"], HU.getIconImage("fa-chevron-left"));
 		var next = HU.span([CLASS, "display-changeentries-button", TITLE,"Next entry", ID, this.getDomId(ID_ENTRIES_NEXT), TITLE,"Next"], HU.getIconImage("fa-chevron-right")); 
		var label = argProperties.changeEntriesLabel||"";
		if(label!="") label = label+"<br>";
		return  HU.center(label + prev +" " + HU.select("",[ATTR_ID, this.getDomId(ID_ENTRIES_MENU)],enums) +" " + next);
	    }
	    return "";
	},
        initializeEntriesMenu: function() {
	    this.jq(ID_ENTRIES_PREV).click(e=>{
		var index = this.jq(ID_ENTRIES_MENU)[0].selectedIndex;
		if(index<=0) return;
		var entry  =this.changeEntries[index-1];
		this.jq(ID_ENTRIES_MENU).val(entry);
		this.handleEntryMenu(entry);
	    });
	    this.jq(ID_ENTRIES_NEXT).click(e=>{
		var index = this.jq(ID_ENTRIES_MENU)[0].selectedIndex;
		if(index>=this.changeEntries.length-1) {
		    return;
		}
		var entry  =this.changeEntries[index+1];
		this.jq(ID_ENTRIES_MENU).val(entry);
		this.handleEntryMenu(entry);
	    });
	    this.jq(ID_ENTRIES_MENU).change(e=>{
		var entry = this.jq(ID_ENTRIES_MENU).val();
		this.handleEntryMenu(entry);
	    });
	},


        popup: function(srcId, popupId, srcObj, popupObject) {
            var popup = popupObject || $("#"+popupId);
            var src = srcObj || $("#"+srcId);
            var myalign = 'left top';
            var atalign = 'left bottom';
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
            if (Utils.isDefined(this.showMenu)) {
		return this.showMenu;
	    }
	    var dflt = false;
            if (this.displayParent != null) {
		dflt = this.displayParent.getProperty("showChildMenu",dflt);
	    }
	    var v = this.getProperty(PROP_SHOW_MENU, dflt);
	    return v;
        },
        getShowTitle: function() {
            if (Utils.isDefined(this.showTitle)) {
		return this.showTitle;
	    }
	    var dflt = false;
            if (this.displayParent != null) {
		dflt = this.displayParent.getProperty("showChildTitle",dflt);
	    }
	    var v = this.getProperty("showTitle", dflt);
	    return v;
        },

        getTimeZone: function() {
            return this.getProperty("timeZone");
        },
        formatDate: function(date, args, useToStringIfNeeded) {
	    if(!date || !date.getTime) return "";
            try {
                return this.formatDateInner(date, args, useToStringIfNeeded);
            } catch (e) {
                console.log("Error formatting date:" + e);
                if (!date.getTime && date.v) date = date.v;
                return "" + date;
            }
        },
	dateFormat:null,
        formatDateInner: function(date, args,useToStringIfNeeded) {
	    if(!this.dateFormat)
		this.dateFormat =  this.getProperty("dateFormat", this.getProperty("dateFormat2"));
	    if(!this.dateFormat && useToStringIfNeeded) {
		return String(date);
	    }

            //Check for date object from charts
            if (!date.getTime && date.v) date = date.v;
	    if(this.dateFormat) {
		let dttm = Utils.formatDateWithFormat(date,this.dateFormat,true);
		if(dttm) {
		    return dttm;
		}
	    }
            if (!date.toLocaleDateString) {
                return "" + date;
            }
            var suffix;
            if (args && !Utils.isDefined(args.suffix))
                suffix = args.suffix;
            else
                suffix = this.getProperty("dateSuffix");
            var timeZone = this.getTimeZone();
            if (!suffix && timeZone) suffix = timeZone;
            return Utils.formatDate(date, args?args.options:null, {
                timeZone: timeZone,
                suffix: suffix
            });
        },
        getUniqueId: function(base) {
            return HU.getUniqueId(base);
        },
        handleError: function(code, message) {
            GuiUtils.handleError("An error has occurred:" + message, true, true);
        },
        toString: function() {
            return "DisplayThing:" + this.getId();
        },
        getDomId: function(suffix) {
            return this.getId() + "_" + suffix;
        },
	gid: function(suffix) {
            return this.getId() + "_" + suffix;
        },
        jq: function(componentId) {
            return $("#" + this.getDomId(componentId));
        },
        writeHtml: function(idSuffix, html) {
            $("#" + this.getDomId(idSuffix)).html(html);
        },
	getTemplateProps: function(fields) {
	    return {
		iconField: this.getFieldById(fields, this.getProperty("iconField")),
		iconSize: parseFloat(this.getProperty("iconSize",16)),
		iconMap: this.getIconMap(),
		colorBy:this.getProperty("colorBy"),
		colorByMap: this.getColorByMap()
	    }
	},
	applyRecordTemplate: function(row, fields, template, props,macros) {
	    fields = this.getFields(fields);
	    if(!props) {
		props = this.getTemplateProps(fields);
	    }
	    if(!macros) macros = Utils.tokenizeMacros(template);
	    let attrs = {};
	    if(props.iconMap && props.iconField) {
		var value = row[props.iconField.getIndex()];
		var icon = props.iconMap[value];
		if(icon) {
		    attrs[props.iconField.getId() +"_icon"] =  HU.image(icon,["width",props.iconSize]);
		}
	    }
	    for (var col = 0; col < fields.length; col++) {
		var f = fields[col];
		var value = row[f.getIndex()];
		if(props.iconMap) {
		    var icon = props.iconMap[f.getId()+"."+value];
		    if(icon) {
			s = s.replace("${" + f.getId() +"_icon}", HU.image(icon,["size",props.iconSize]));
		    }
		}
		if(f.getType()=="image") {
		    if(value && value.trim().length>1) {
			var imgAttrs = [];
			imgAttrs.push("width");
			imgAttrs.push(this.getProperty("imageWidth","100%")) ;
			var img =  HU.image(value, imgAttrs);
			attrs[f.getId() +"_image"] =  img;
			attrs[f.getId() +"_url"] =  value;
		    } else {
			attrs[f.getId() +"_url"] =  ramaddaBaseUrl+"/icons/blank.gif";
			attrs[f.getId() +"_image"] =  "";
		    }
		} else if(f.getType()=="url") {
		    if(value && value.trim().length>1) {
			attrs[f.getId() +"_href"] =  HU.href(value,value);
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
		var color;
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
	    return macros.apply(attrs);
	},
	getFields: function(fields) {
            if (!fields) {
                var pointData = this.getData();
                if (pointData == null) {
		    return null;
		}
                fields = pointData.getRecordFields();
	    }
	    return fields;
	},
        getRecordHtml: function(record, fields, template) {
	    fields = this.getFields(fields);
	    if(!fields) return "";
	    let linkField = this.getFieldById(null,this.getProperty("linkField"));
	    let titleField = this.getFieldById(null,this.getProperty("titleField"));
	    let descField = this.getFieldById(null,this.getProperty("descriptionField"));
	    let link  = linkField?record.getValue(linkField.getIndex()):null;
	    let showDate = this.getProperty("showDate", true);
	    let showImage = this.getProperty("showImage", true);
            let showGeo = false;
            if (Utils.isDefined(this.showGeo)) {
                showGeo = ("" + this.showGeo) == "true";
            }
	    if(!template)
		template = this.getProperty("recordTemplate");
	    if(template) {
		if(template!="${default}" && template!="${fields}") {
		    return this.applyRecordTemplate(this.getDataValues(record), fields, template);
		}
	    }
	    if(template=="${fields}") {
		fields = this.getFieldsByIds(null,this.getProperty("tooltipFields",this.getPropertyFields()));
	    }

	    let values = "";
	    if(titleField) {
		let title = record.getValue(titleField.getIndex());
		if(link)
		    title = HU.href(link,title);
		values+=HU.center(HU.h3(title));
		link = null;
	    }

	    if(descField) {
		let desc = record.getValue(descField.getIndex());
		values+=desc;
	    }

            values += HU.open(TABLE);
            for (var doDerived = 0; doDerived < 2; doDerived++) {
                for (let i = 0; i < fields.length; i++) {
                    var field = fields[i];
		    if(field==titleField || field==descField) continue;
                    if (doDerived == 0 && !field.derived) continue;
                    else if (doDerived == 1 && field.derived) continue;
                    if (!field.getForDisplay()) {
			continue;
		    }
		    if(!showDate) {
                        if (field.isFieldDate()) {
                            continue;
                        }
		    }
                    if (!showGeo) {
                        if (field.isFieldGeo()) {
                            continue;
                        }
                    }
                    var value = record.getValue(field.getIndex());
		    let fieldValue = value;
                    if (typeof value == "number") {
			value = this.formatNumber(value, field.getId());
		    } 
                    if (field.isFieldDate()) {
			value = this.formatDate(value);
		    }
		    if(field.getType() == "image" && value!="") {
			if(!showImage) continue;
			value = HU.image(value,["width","200"]);
		    }
		    if(field.getType() == "url") {
			value = HU.href(value,value);
		    }
		    value = value + field.getUnitSuffix();
		    if(value.length>200) {
			value  = HU.div([STYLE,HU.css("max-height","200px","overflow-y","auto")],value);
		    }
		    let label = this.formatRecordLabel(field.getLabel());
                    values += HU.open(TR,['valign','top']);
		    values += HU.td([],HU.b(label + ':'));
		    values += HU.td(["field-id",field.getId(),"field-value",fieldValue, "align","left"], HU.div([STYLE,HU.css('margin-left','5px')], value));
		    values += HU.close(TR);
                }
            }
            if (record.hasElevation()) {
                values += HU.tr([],HU.td([ALIGN,'right'],HU.b('Elevation:')) +
				HU.td([ALIGN,'left'], number_format(record.getElevation(), 4, '.', '')));
            }
            values += HU.close(TABLE);
	    if(this.getProperty("recordHtmlStyle")){
		values = HU.div([STYLE,this.getProperty("recordHtmlStyle")], values);
	    }
            return values;
        },
        formatRecordLabel: function(label) {
            label = label.replace(/!!/g, " -- ");
            return label;
        },
        getFormValue: function(what, dflt) {
            var fromForm = $("#" + this.getDomId(what)).val();
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
        formatNumber: function(number, propPrefix) {
	    if(!this.getProperty([propPrefix+".doFormatNumber","doFormatNumber"],true)) {
		return number;
	    }
	    if(isNaN(number)) {
		return "--";
	    }
	    let f = this.formatNumberInner(number, propPrefix);
	    let fmt = this.getProperty([propPrefix+".numberTemplate","numberTemplate"]);
	    if(fmt) f = fmt.replace("${number}", f);
	    return f;
	},
        formatNumberInner: function(number,propPrefix) {
	    number = +number;
	    let scale = this.getProperty([propPrefix+".formatNumberScale","formatNumberScale"]);
            if (Utils.isDefined(scale))
		number = number*scale;
	    let decimals = this.getProperty([propPrefix+".formatNumberDecimals","formatNumberDecimals"]);
            if (Utils.isDefined(decimals)) {
		return number_format(number, decimals);
	    }
            if (this.getProperty([propPrefix+".formatNumberComma","formatNumberComma"], false)) {
		return Utils.formatNumberComma(number);

	    }
            return Utils.formatNumber(number);

        },
        propertyDefined: function(key) {
            return Utils.isDefined(this.getProperty(key));
        },
        setPropertyOn: function(object, myProperty, objectProperty, dflt) {
            var prop = this.getProperty(myProperty, dflt);
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
        getPropertyFromUrl: function(key, dflt) {
	    let fromUrl = HU.getUrlArgument("display"+ this.displayCount+"." + key);
	    if(fromUrl) return fromUrl;
	    return this.getProperty(key,dflt);
	},
	getPropertyFields: function(dflt) {
	    return this.getPropertyFromUrl(PROP_FIELDS,dflt);
	},
        getProperty: function(key, dflt, skipThis) {
	    if(this.debugGetProperty)
		console.log("\tgetProperty:" + key);
	    let value =  this.getPropertyInner(key,null,skipThis);
	    if(this.debugGetProperty)
		console.log("\tgot:" + value);
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
		if(this.debugGetProperty)
		    console.log("\treturning dflt:" + dflt);
		return dflt;
	    }
	    if(this.debugGetProperty)
		console.log("\treturning value:" + value);
	    return value;
	},
        getPropertyInner: function(keys, dflt,skipThis) {	    
	    let debug = displayDebug.getProperty;
	    debug = this.debugGetProperty;
	    if(!Array.isArray(keys)) keys = [keys];
	    for(let i=0;i<keys.length;i++) {
		let key = keys[i];
		if(debug) console.log("getProperty:" + key +" dflt:" + dflt);
		if(this.dynamicProperties) {
		    if(Utils.isDefined(this.dynamicProperties[key])) {
			return this.dynamicProperties[key];
		    }
		}
		var value = this.properties[key];
		if (value != null) {
		    if(debug) console.log("\tgot property from this.properties:" + value);
                    return value;
		}
	    }
	    for(let i=0;i<keys.length;i++) {
		let key = keys[i];
		var fromParent=null;
		if (this.displayParent != null) {
                    fromParent =  this.displayParent.getPropertyInner("inherit."+key, skipThis);
		}
		if (!fromParent && this.getDisplayManager) {
                    fromParent=  this.getDisplayManager().getPropertyInner("inherit."+key);
		}
		if(fromParent) {
		    if(debug) console.log("\tgetProperty-3");
		    return fromParent;
		}
	    }
	    if(!this.ignoreGlobals) {
		if (this.displayParent != null) {
		    if(debug) console.log("\tgetProperty calling parent");
                    return this.displayParent.getPropertyInner(keys, skipThis);
		}
		if (this.getDisplayManager) {
		    if(debug) console.log("\tgetProperty-5");
                    return   this.getDisplayManager().getPropertyInner(keys);
		}
		for(let i=0;i<keys.length;i++) {
		    let key = keys[i];
		    value = getGlobalDisplayProperty(key);
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

    
    RamaddaUtil.defineMembers(this, {
        displayReady: Utils.getPageLoaded(),
        type: argType,
        displayManager: argDisplayManager,
        filters: [],
        dataCollection: new DataCollection(),
        selectedCbx: [],
        entries: [],
        wikiAttrs: [TITLE, "showTitle", "showDetails", "minDate", "maxDate"],
	_properties:[],
	_wikiTags:[],
	defineProperties:function(props) {
	    props.forEach(p=>{
		if(!p.p && p.label) {
		    this._wikiTags.push('label:' + p.label);
		    return;
		}
		if(!p.p && p.inlineLabel) {
		    this._wikiTags.push('inlinelabel:' + p.inlineLabel);
		    return;
		}		
		let tag=p.p+'="' + (p.wikiValue?p.wikiValue:p.d?p.d:"")+'"'
		let w = [];
		let tt = p.tt||"";
		if(p.label) {
		    w.push(p.label);
		    w.push(tag);
		} else {
		    w.push(tag);
		}
		w.push(p.tt);
		this._wikiTags.push(w);
		if(!Utils.isDefined(p.doGetter) || p.doGetter) {
		    let funcName =  'getProperty' + p.p.substring(0, 1).toUpperCase() + p.p.substring(1);
		    this[funcName] = (dflt)=>{
			if(!Utils.isDefined(dflt)) dflt = p.d;
			return this.getProperty(p.p,dflt);
		    };
		}
	    });
	},
	getWikiEditorTags: function() {
	    let l =   [
		"label:Display Attributes",
		['fields=""','comma separated list of field ids or indices - e.g. #1,#2,#4-#7,etc or *'],
		['notFields="regexp"','regexp to not include fields'],		
		"showMenu=\"true\"",	      
		"showTitle=\"true\"",
		"showEntryIcon=true",
		"layoutHere=\"true\"",
		"width=\"100%\"",
		"height=\"400\"",
		'tooltip=${default}',
		['displayStyle="css styles"',"Specify styles for display"],
		"title=\"\"",
		"titleBackground=\"color\"",
		"linkField=",
		"titleField=",
		"descriptionField=",
		"textColor=\"color\"",
		["backgroundImage=\"\"","Image url to display in background"],
		"background=\"color\"",
		'showProgress=true',
		['doEntries=true','Make the children entries be data'],
		['addAttributes=true','Include the extra attributes of the children'],
		["sortFields=\"\"","Comma separated list of fields to sort the data on"],
		["sortAscending=true|false",""],
		["sortByFields=\"\"","Show sort by fields in a menu"],
		['sortHighlight=true','Sort based on highlight from the filters'],
		['showDisplayFieldsMenu=true'],
		['displayFieldsMenuMultiple=true'],
		['displayFieldsMenuSide=left'],
		['acceptDisplayFieldsChangeEvent=true'],
		'inlinelabel:Formatting',
		'dateFormat=yyyy|yyyymmdd|yyyymmddhh|yyyymmddhhmm|yyyymm|yearmonth|monthdayyear|monthday|mon_day|mdy|hhmm',
		'dateFormatDaysAgo=true',
		'doFormatNumber=false',
 		'formatNumberDecimals=0',
		'formatNumberScale=100',
		'numberTemplate="${number}%"',
		'&lt;field_id&gt;.&lt;format&gt;="..."',
		"label:Filter Data",
		['fieldsNumeric=true','Only get numeric fields'],
		"filterFields=\"\"",
		'filterFieldsToPropagate=""',
		"hideFilterWidget=true",
		['filterHighlight=true',"Highlight the records"],
		['showFilterHighlight=false',"show/hide the filter highlight widget"],
		"acceptFilterEvent=false",
		"propagateHighlightEvent=true",
		['filterSliderImmediate=true',"Apply the change while sliding"],
		['filterLogic=and|or',"Specify logic to apply filters"],		
		"&lt;field&gt;.filterValue=\"\"",
		"&lt;field&gt;.filterValueMin=\"\"",
		"&lt;field&gt;.filterValueMax=\"\"",
		"&lt;field&gt;.filterValues=\"\"",
		"&lt;field&gt;.filterMultiple=\"true\"",
		"&lt;field&gt;.filterMultipleSize=\"5\"",
		"&lt;field&gt;.filterLabel=\"\"",
		"&lt;field&gt;.showFilterLabel=\"false\"",
		"&lt;field&gt;.filterByStyle=\"background:white;\"",
		"&lt;field&gt;.includeAll=false",
		"&lt;field&gt;.filterSort=false",
		"&lt;field&gt;.filterStartsWith=\"true\"",
		"&lt;field&gt;.filterDisplay=\"menu|tab|button|image\"",
		['&lt;field&gt;.filterOps="<,5000000,label1;>,5000000"','Add menu with fixed filters'],
		['excludeUndefined=true','Exclude any records with an undefined value'],
		['excludeZero=true','Exclude any records with a 0 value'],
		['recordSelectFilterFields=""','Set the value of other displays filter fields'],
		'selectFields=prop:label:field1,...fieldN;prop:....',
		['match value', 'dataFilters="match(field=field,value=value,label=,enabled=);"','Only show records that match'], 		
		['not match value','dataFilters="notmatch(field=field,value=value,label=,enabled=);"','Only show records that dont match'],
		['no missing values','dataFilters="nomissing(field=field,label=,enabled=);"','Dont show missing values'],
		['less than','dataFilters="lessthan(field=field,value=value,label=,enabled=);"',''],
		['greater than','dataFilters="greaterthan(field=field,value=value,label=,enabled=);"',''],
		['equals','dataFilters="equals(field=field,value=value,label=,enabled=);"',''],
		['not equals','dataFilters="notequals(field=field,value=value,label=,enabled=); "',''],
		['filterLatest=fields','Only show the latest records grouped by fields'],		
		['filterDate=year',"Show a simple pull down menu to select a year to display"],
		['filterDateIncludeAll=true',"Include all years"],
		['startDate="yyyy-MM-dd"',"Filter data on date"],
		['endDate="yyyy-MM-dd"',"Filter data on date"],
		"inlinelabel:Convert Data",
		['binDate="day|month|year"',"Bin the dates"],
		'binType="count|average|total"',
		['groupBy=field','Group the data'],
		['derived data', 'convertData="derived(field=new_field_id, function=foo*bar);"','Add derived field'],
		['merge rows','convertData="mergeRows(keyFields=f1\\\\,f2, operator=count|sum|average, valueFields=);"',"Merge rows together"],
		["rotate data", 'convertData="rotateData(includeFields=true,includeDate=true,flipColumns=true);"',"Rotate data"],
		["add percent", 'convertData="addPercentIncrease(replaceValues=false);"',"Add percent increase"],
		['doubling rate','convertData="doublingRate(fields=f1\\\\,f2, keyFields=f3);"',"Calculate # days to double"],
		["unfurl", 'convertData="unfurl(headerField=,uniqueField=,valueFields=);"',"Unfurl"],
		"label:Color Attributes",
		["colors=\"color1,...,colorN\"","Comma separated array of colors"],
		["colorBy=\"\"","Field id to color by"],
		["colorByFields=\"\"","Show color by fields in a menu"],
		["colorByLog=\"true\"","Use a log scale for the color by"],
		["colorByMap=\"value1:color1,...,valueN:colorN\"","Specify colors for color by text values"],
		['colorByInverse=true','Inverse the values'],
		["colorTableAlpha=\"0.5\"","Set transparency on color table values"],
		['colorTableInverse=true',"Inverse the color table"],
		['colorTablePruneLeft=N',"Prune first N colors"],
		['colorTablePruneRight=N',"Prune last N colors"],
		["colorByMin=\"value\"","Min scale value"],
		["colorByMax=\"value\"","Max scale value"],
		['showColorTable=false',"Display the color table"],
		'showColorTableDots=true',
		'colorTableDotsDecimals=0',
		'colorTableSide=bottom|right|left|top',
		['showColorTableStride=1','How many colors should be shown'],
		['colorByAllRecords=true',"use all records for color range"],
		'convertColorIntensity=true',
		'intensitySourceMin=0',
		'intensitySourceMax=100',
		'intensityTargetMin=1',
		'intensityTargetMax=0',
		'convertColorAlpha=true',
		'alphaSourceMin=0',
		'alphaSourceMax=100',
		'alphaTargetMin=0',
		'alphaTargetMax=1',
		"inlinelabel:Animation Attributes",
		"doAnimation=true",
		"acceptDateRangeChange=true",
		"animationDateFormat=\"yyyy\"",
		"animationWindow=\"decade|halfdecade|year|month|week|day|hour|minute\"",
		"animationMode=\"sliding|frame|cumulative\"",
		"animationSpeed=\"500\"",
		"animationLoop=\"true\"",
		'animationDwell=1000',
		"animationShowButtons=\"false\"",
		"animationShowSlider=\"false\"",
		"animationWidgetShort=\"true\""
	    ];
	    return  Utils.mergeLists(l,this._wikiTags);
        },

	defineSizeByProperties: function() {
	    this.defineProperties([
		{inlineLabel:'Size By'},
	    	{p:'sizeBy',wikiValue:'field',tt:'Field to size points by'},
		{p:'sizeByLog',wikiValue:true,tt:'Use log scale for size by'},
		{p:'sizeByMap', wikiValue:'value1:size,...,valueN:size',tt:'Define sizes if sizeBy is text'},
		{p:'sizeByRadiusMin',wikiValue:'2',tt:'Scale size by'},
		{p:'sizeByRadiusMax',wikiValue:'20',tt:'Scale size by'},
		{p:'sizeByLegendSide',wikiValue:'bottom|top|left|right'},,
		{p:'sizeByLegendStyle'},
		{p:'sizeBySteps',wikiValue:'value1:size1,v2:s2,...',tt:'Use steps for sizes'},
	    ]);
	},

        getDisplayManager: function() {
            return this.displayManager;
        },
        getLayoutManager: function() {
            return this.getDisplayManager().getLayoutManager();
        },
        addToDocumentUrl: function(key, value) {
	    HU.addToDocumentUrl("display"+ this.displayCount+"." + key,value);
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
        propagateEvent: function(func, data) {
            var dm = this.getDisplayManager();
            dm[func](this, data);
        },
        displayError: function(msg) {
            this.displayHtml(HU.getErrorDialog(msg));
        },
        clearHtml: function() {
            this.displayHtml("");
        },
        displayHtml: function(html) {
            this.jq(ID_DISPLAY_CONTENTS).html(html);
        },
        notifyEvent: function(func, source, data) {
            if (this[func] == null) {
                return;
            }
            this[func].apply(this, [source, data]);
        },
	getColorTableHorizontal: function() {
	    return this.getProperty("colorTableSide","bottom") == "bottom" || this.getProperty("colorTableSide","bottom") == "top";
	},
        displayColorTable: function(ct, domId, min, max, args) {
	    if(!args) args = {};
	    args.showColorTableDots = this.getProperty("showColorTableDots");
	    args.decimals = this.getProperty("colorTableDotsDecimals",-1);
	    args.showRange = this.getProperty("colorTableShowRange");
	    let labels = this.getProperty("colorTableLabels");
	    args.labels = labels?labels.split(","):null;
	    args.labelStyle=this.getProperty("colorTableLabelStyle","font-size:12pt;");
	    args.horizontal= this.getColorTableHorizontal();
	    args.stride = this.getProperty("showColorTableStride",1);
            Utils.displayColorTable(ct, this.getDomId(domId), min, max, args);
	    if(!args || !args.colorByInfo) return;
	    this.jq(domId).find(".display-colortable-slice").css("cursor","pointer");
	    let _this = this;
	    if(!this.originalColorRange) {
		this.originalColorRange = [min,max];
	    }		
	    this.jq(domId).find(".display-colortable-slice").click(function(e) {
		let val = $(this).attr("data-value");
		let popup = getTooltip();
		popupObject = popup;
		let html = "";
		html += HU.div([CLASS,"ramadda-menu-item","what","setmin"],"Set range min to " + Utils.formatNumber(val));
		html += HU.div([CLASS,"ramadda-menu-item","what","setmax"],"Set range max to " + Utils.formatNumber(val));
		html += HU.div([CLASS,"ramadda-menu-item","what","reset"],"Reset range");
		if(_this.getProperty("colorByLog")) {
		    html += HU.div([CLASS,"ramadda-menu-item","what","togglelog"],"Use linear scale");
		} else {
		    html += HU.div([CLASS,"ramadda-menu-item","what","togglelog"],"Use log scale");
		}
		html += HU.div([CLASS,"ramadda-menu-item","what","reset"],"Reset range");
		html += Utils.getColorTablePopup();
		popup.html(html);
		$(popup).find(".ramadda-colortable-select").click(function() {
		    let ct = $(this).attr("colortable");
		    if(ct) {
			_this.setProperty("colorTable",ct);
			_this.forceUpdateUI();
		    }		    
		});
		popup.show();
		popup.position({
                    of: $(this),
                    my: 'left top',
                    at: 'left bottom',
                    collision: "none none"
		});
		popup.find(".ramadda-menu-item").click(function() {
		    let what = $(this).attr("what");
		    if(what == "reset") {
			_this.setProperty("colorByMin",_this.originalColorRange[0]);
			_this.setProperty("colorByMax",_this.originalColorRange[1]);
			_this.setProperty("overrideColorRange", false);
		    } else if(what == "togglelog") {
			if(!_this.getProperty("colorByLog")) 
			    _this.setProperty("colorByLog",true);
			else
			    _this.setProperty("colorByLog",false);
 		    } else if(what == "setmin") {
			_this.setProperty("colorByMin",val);
			_this.setProperty("overrideColorRange", true);
		    } else {
			_this.setProperty("colorByMax",val);
			_this.setProperty("overrideColorRange", true);
		    }
		    _this.forceUpdateUI();
		});
	    });
        },
	getUnhighlightColor: function() {
	    return this.getProperty("unhighlightColor","#eee");
	},
	getColorList:function() {
	    if(this.colorList && this.colorList.length>0) {
		return this.colorList;
	    }
	    if (this.getProperty("colors") && this.getProperty("colors")!="default") {
		var v = this.getProperty("colors");
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
	    if(names && !Array.isArray(names)) names  = [name];
            let ct = null;
            if (names) {
		names.every(name=>{
                    ct = this.getProperty(name);
		    if(ct) return false;
		    return true;
		});
            } else {
		var colorBy = this.getProperty("colorBy");
		if(colorBy) {
                    ct = this.getProperty("colorTable." + colorBy);
		}
		if(!ct) {
                    ct = this.getProperty("colorBar", this.getProperty("colorTable"));
		}
            }
            if (ct == "none") return null;
            return ct;
        },
	getColorTable: function(justColors, name, dflt) {
            let colorTable = this.getColorTableName(name);
            if (!colorTable) {
                colorTable = dflt;
            }
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
                var colors = this.getProperty("colors");
                if ((typeof colors) != "object") colors = colors.split(",");
		return this.convertColors(colors);
            }
            return null;
        },
	addAlpha: function(colors, alpha) {
	    if(!colors) return null;
	    alpha = Utils.isDefined(alpha)?alpha:this.getProperty("colorTableAlpha");
	    if(!alpha) return colors;
	    colors=  Utils.cloneList(colors);
	    var ac = [];
	    colors.forEach((c)=>{
		ac.push(Utils.addAlphaToColor(c,alpha));
	    });
	    return ac;
        },
        convertColors: function(colors) {
	    colors = this.addAlpha(colors);
	    if(this.getProperty("colorTableInverse")) {
		let tmp = [];
		for(let i=colors.length-1;i>=0;i--)
		    tmp.push(colors[i]);
		colors = tmp;
	    }
	    if(this.getProperty("colorTablePruneLeft")) {
		let tmp = [];
		for(let i=+this.getProperty("colorTablePruneLeft");i<colors.length;i++) {
		    tmp.push(colors[i]);
		}
		colors = tmp;
	    }
	    if(this.getProperty("colorTablePruneRight")) {
		let tmp = [];
		let d = +this.getProperty("colorTablePruneRight");
		for(let i=0;i<colors.length-d;i++) {
		    tmp.push(colors[i]);
		}
		colors = tmp;
	    }
	    return colors;
	},

        getColorByColors: function(records, dfltColorTable) {
            var colorBy = this.getProperty("colorBy");
            if (!colorBy) {
                return null;
            }
            var colorByField = this.getFieldById(fields, colorBy);
            if (!colorByField) {
                return null;
            }
            var obj = this.getColumnValues(records, colorByField);
            var colors = this.getColorTable();
            if (!colors) colors = Utils.getColorTable(dfltColorTable || "blue_white_red");
            if (!colors) return null;
            var min = parseFloat(this.getProperty("colorByMin", obj.min));
            var max = parseFloat(this.getProperty("colorByMax", obj.max));
            if (colors.colors) colors = colors.colors;
            var range = max - min;
            var colorValues = [];
            for (var i = 0; i < obj.values.length; i++) {
                var value = obj.values[i];
                var percent = (value - min) / range;
                var index = parseInt(percent * colors.length);
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
	    let doHeatmap=this.getProperty("doHeatmap",false);
	    let args =  {
		display:this,
		shape:this.getProperty("cellShape","rect"),
		color: this.getProperty("cellColor","blue"),
		stroke: !this.getProperty("cellFilled",true),
		cellSize: this.getProperty("cellSize",doHeatmap?0:4),
		cellSizeH: this.getProperty("cellSizeH",20),
		cellSizeHBase: this.getProperty("cellSizeHBase",0),
		cell3D:this.getProperty("cell3D",false),
		cellShowText:this.getProperty("cellShowText",false),
		cellLabels:Utils.split(this.getProperty("cellLabels")),
		cellFonts:Utils.split(this.getProperty("cellFonts")),
		cellLabelColors:Utils.split(this.getProperty("cellLabelColor")),
		cellLabelPositions:Utils.split(this.getProperty("cellLabelPositions")),
		cellLabelOffsetsX:Utils.split(this.getProperty("cellLabelOffsetsX")),
		cellLabelOffsetsY:Utils.split(this.getProperty("cellLabelOffsetsY")),
		doHeatmap:doHeatmap,
		operator:this.getProperty("hm.operator","count"),
		filter:this.getProperty("hm.filter")
	    };
	    args.cellSizeX = +this.getProperty("cellSizeX",args.cellSize);
	    args.cellSizeY = +this.getProperty("cellSizeY",args.cellSize);
	    return args;
	},
	getIconMap: function() {
	    var iconMap;
	    var iconMapProp = this.getProperty("iconMap");
	    if (iconMapProp) {
                var toks = iconMapProp.split(",");
		iconMap = {};
                for (var i = 0; i < toks.length; i++) {
		    var toks2 = toks[i].split(":");
		    if (toks2.length > 1) {
                        iconMap[toks2[0]] = toks2[1];
		    }
		}
            }
	    return iconMap;
	},
	getColorByInfo: function(records, prop,colorByMapProp, defaultColorTable,propPrefix) {
            var pointData = this.getData();
            if (pointData == null) return null;
	    if(this.getProperty("colorByAllRecords")) {
		records = pointData.getRecords();
	    }
	    var fields = pointData.getRecordFields();
	    return new ColorByInfo(this, fields, records, prop,colorByMapProp, defaultColorTable, propPrefix);
	},
	getColorByMap: function(prop) {
	    prop = this.getProperty(prop||"colorByMap");
	    this.debugGetProperty=false;
	    return Utils.parseMap(prop);
        },
        toString: function() {
            return "RamaddaDisplay:" + this.type + " - " + this.getId();
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
        setContents: function(contents) {
            let style = "";
            contents = HU.div([ATTR_CLASS, "display-contents-inner display-" + this.getType() + "-inner", STYLE, style], contents);
            this.writeHtml(ID_DISPLAY_CONTENTS, contents);
        },
        addEntry: function(entry) {
            this.entries.push(entry);
        },
        clearCachedData: function() {},
        setEntry: function(entry) {
	    if(this.getProperty("shareSelected")) {
		return;
	    }
	    if(!this.getProperty("acceptSetEntry",true)) {
		return;
	    }	    
            this.entries = [];
            this.addEntry(entry);
            this.entry = entry;
            this.entryId = entry.getId();
            this.clearCachedData();
            if (this.properties.theData) {
                this.dataCollection = new DataCollection();
                var attrs = {
                    entryId: this.entryId,
                    lat: this.getProperty("latitude"),
                    lon: this.getProperty("longitude"),
                };
                this.properties.theData = this.data = new PointData(entry.getName(), null, null, this.getRamadda().getRoot() + "/entry/show?entryid=" + entry.getId() + "&output=points.product&product=points.json&max=5000", attrs);
		this.startProgress();
                this.data.loadData(this);
            } else {
		this.callUpdateUI();
	    }
            var title = "";
            if (this.getShowTitle()) {
                this.jq(ID_TITLE).html(entry.getName());
            }
        },
        getTextColor: function(property) {
            if (property) return this.getProperty(property, this.getProperty("textColor"));
            return this.getProperty("textColor", "#000");
        },
        getTitleHtml: function(title) {
            var titleToShow = "";
            if (this.getShowTitle()) {
                var titleStyle = HU.css("color" , this.getTextColor("titleColor"));
                var bg = this.getProperty("titleBackground");
                if (bg) titleStyle += HU.css('background', bg,'padding','2px','padding-right','6px','padding-left','6px');
                titleToShow = this.getShowTitle() ? this.getDisplayTitle(title) : "";
		let entryId = this.getProperty("entryId") || this.entryId;
                if (entryId)
                    titleToShow = HU.href(this.getRamadda().getEntryUrl(entryId), titleToShow, [ATTR_CLASS, "display-title",  STYLE, titleStyle]);
            }

	    if(this.getProperty("showEntryIcon")) {
		let icon = this.getProperty("entryIcon");
		if(icon) titleToShow  = HU.image(icon) +" " + titleToShow;
	    }
            return titleToShow;
        },
        handleEventMapClick: function(source, args) {
	    console.log(this.type+".mapClick");
            if (!this.dataCollection) return;
            var pointData = this.dataCollection.getList();
            for (var i = 0; i < pointData.length; i++) {
                pointData[i].handleEventMapClick(this, source, args.lon, args.lat);
            }
        },

        handleEventMapBoundsChanged: function(source, args) {
	    if(this.getProperty("acceptBoundsChange")) {
		this.filterBounds  = args.bounds;
		this.callUpdateUI();
            }
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
        handleEventPropertyChanged: function(source, prop) {
	    let debug = displayDebug.handleEventPropertyChanged;
	    if(prop.property == "dateRange") {
		if(this.getProperty("acceptDateRangeChange")) {
		    this.handleDateRangeChanged(source, prop);
		}
		return;
	    }

	    
	    if(prop.property == "displayFields") {
		if(!this.getProperty("acceptDisplayFieldsChangeEvent",false)) {
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

		if(!this.getProperty("request." + macro.name + ".acceptChangeEvent",true)) {
		    return;
		}

		macro.setValue(prop);



		if(debug)
		    console.log(this.getId() +" event-reloading");
		this.reloadData();
		return;
	    }

	    if (prop.property == PROP_FILTER_VALUE) {
		if(!this.getProperty("acceptFilterEvent",true)) {
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
		    let widgetId = this.getFilterId(prop.fieldId);
		    if(prop.id && prop.id.endsWith("date1")) {
			widgetId+="_date1";
		    } else 	if(prop.id && prop.id.endsWith("date2")) {
			widgetId+="_date2";
		    }
		    if(prop.fieldId == "_highlight") {
			this.jq(ID_FILTER_HIGHLIGHT).val(prop.value);
			this.setProperty("filterHighlight", prop.value=="highlight");
		    } else 	if(Utils.isDefined(prop.value2)) {
			$("#" +widgetId+"_min").val(prop.value);
			$("#" +widgetId+"_min").attr("data-value", prop.value);
			$("#" +widgetId+"_max").val(prop.value2);
			$("#" +widgetId+"_max").attr("data-value", prop.value2);
		    } else {
			var widget = $("#"+widgetId);
			if(widget.attr("isCheckbox")) {
			    var on = widget.attr("onValue");
			    var off = widget.attr("offValue");
			    widget.prop('checked',prop.value.includes(on));
			} else {
			    widget.val(prop.value);
			}
			widget.attr("data-value",prop.value);
			if(widget.attr("isButton")) {
			    widget.find(".display-filter-button").removeClass("display-filter-button-selected");
			    widget.find("[value='" + prop.value +"']").addClass("display-filter-button-selected");
			}
		    }
		});
		this.settingFilterValue = false;
		this.dataFilterChanged();
		return;
            }
            this.setProperty(prop.property, prop.value);
            this.callUpdateUI();
        },
        handleEventRecordHighlight: function(source, args) {
	    if(this.getAnimation().getEnabled() &&  !args.skipAnimation) {
		this.getAnimation().handleEventRecordHighlight(source, args);
	    }
	},
        propagateEventRecordSelection: function(args) {
	    this.getDisplayManager().notifyEvent("handleEventRecordSelection", this, args);
	    if(this.getProperty("recordSelectFilterFields")) {
		let fields = this.getFieldsByIds(null,this.getProperty("recordSelectFilterFields"));
		if(fields && fields.length>0) {
		    let props = {
			property: PROP_FILTER_VALUE,
			properties:[]
		    };
		    fields.forEach(field=>{
			props.properties.push({
			    id:field.getId(),
			    fieldId: field.getId(),
			    value: args.record.getValue(field.getIndex())
			});
		    })
		    this.propagateEvent("handleEventPropertyChanged", props);
		}
	    }
	},
        handleEventRecordSelection: function(source, args) {
	    this.selectedRecord= args.record;
	    if(this.selectedRecord) {
		if(this.getProperty("colorThresholdField")) {
		    this.haveCalledUpdateUI = false;
		    this.callUpdateUI();
		}
	    }


            if (!source.getEntries) {
                return;
            }
            var entries = source.getEntries();
            for (var i = 0; i < entries.length; i++) {
                var entry = entries[i];
                var containsEntry = this.getEntries().indexOf(entry) >= 0;
                if (containsEntry) {
                    this.highlightEntry(entry);
                    break;
                }
            }
        },
        areaClear: function() {
            this.getDisplayManager().notifyEvent("handleEventAreaClear", this);
        },
        handleEventEntryMouseover: function(source, args) {},
        handleEventEntryMouseout: function(source, args) {},
        handleEventEntrySelection: function(source, args) {
            var containsEntry = this.getEntries().indexOf(args.entry) >= 0;
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
            var entries = this.getEntries();
            if (entries != null && entries.length > 0) {
                return Utils.call(callback, entries[0]);
            }
            if (this.entryId) {
                var entry;
                await this.getRamadda().getEntry(this.entryId, e => {
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
            return HU.image(ramaddaBaseUrl + "/icons/progress.gif");
        },
        getLoadingMessage: function(msg) {
            if (!msg) msg = this.getProperty("loadingMessage", "Loading data...");
	    if(msg=="") return "";
            return HU.div([STYLE, HU.css("text-align","center")], this.getMessage(SPACE + msg));
        },
	reloadData: function() {
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
            var jq = $("#" + id);
            if (jq.length > 0) {
                return jq.val();
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
                var fields = this.getSelectedFields();
                this.propagateEvent("handleEventFieldsSelected", fields);
            }
        },
        addFieldsCheckboxes: function(argFields) {
            if (!this.hasData()) {
                return;
            }
            var fixedFields = this.getPropertyFields()
            if (fixedFields != null) {
                if (fixedFields.length == 0) {
                    fixedFields = null;
                }
            }

            var html = "";
            var checkboxClass = this.getId() + "_checkbox";
            var groupByClass = this.getId() + "_groupby";
            var dataList = this.dataCollection.getList();

            if (argFields != null) {
                this.overrideFields = [];
            }
            var seenLabels = {};


            var allFields = this.dataCollection.getList()[0].getRecordFields();
            var badFields = {};
            var flags = null;
            /*
              var tuples = this.getStandardData(null, {
              includeIndex: false
              });
              for (var rowIdx = 1; rowIdx < tuples.length; rowIdx++) {
              var tuple = this.getDataValues(tuples[rowIdx]);
              if (flags == null) {
              flags = [];
              for (var tupleIdx = 0; tupleIdx < tuple.length; tupleIdx++) {
              flags.push(false);
              }
              }

              for (var tupleIdx = 0; tupleIdx < tuple.length; tupleIdx++) {
              if (!flags[tupleIdx]) {
              if (tuple[tupleIdx] != null) {
              flags[tupleIdx] = true;
              //                                console.log("Flag[" + tupleIdx+"] value:" + tuple[tupleIdx]);
              }
              }
              }

              }

              for (var tupleIdx = 0; tupleIdx < tuple.length; tupleIdx++) {
              //                    console.log("#" + tupleIdx + " " + (tupleIdx<allFields.length?allFields[tupleIdx].getId():"") +" ok:" + flags[tupleIdx] );
              }
            */

            for (var collectionIdx = 0; collectionIdx < dataList.length; collectionIdx++) {
                var pointData = dataList[collectionIdx];
                var fields = this.getFieldsToSelect(pointData);
                if (this.canDoGroupBy()) {
                    var allFields = pointData.getRecordFields();
                    var cnt = 0;
                    for (i = 0; i < allFields.length; i++) {
                        var field = allFields[i];
                        if (field.getType() != "string") continue;
                        if (cnt == 0) {
                            html += HU.div([ATTR_CLASS, "display-dialog-subheader"], "Group By");
                            html += HU.open(TAG_DIV, [ATTR_CLASS, "display-fields"]);
                            var on = this.groupBy == null || this.groupBy == "";
                            html += HU.tag(TAG_DIV, [ATTR_TITLE, "none"],
					   HU.radio("none", this.getDomId("groupby"), groupByClass, "none", !on) + " None");
                        }
                        cnt++;
                        var on = this.groupBy == field.getId();
                        var idBase = "groupby_" + collectionIdx + "_" + i;
                        field.radioId = this.getDomId(idBase);
                        html += HU.tag(TAG_DIV, [ATTR_TITLE, field.getId()],
				       HU.radio(field.radioId, this.getDomId("groupby"), groupByClass, field.getId(), on) + " " + field.getUnitLabel() + " (" + field.getId() + ")"
				      );
                    }
                    if (cnt > 0) {
                        html += HU.close(TAG_DIV);
                    }
                }

                if ( /*this.canDoMultiFields() && */ fields.length > 0) {
                    var selected = this.getSelectedFields([]);
                    var selectedIds = [];
                    for (i = 0; i < selected.length; i++) {
                        selectedIds.push(selected[i].getId());
                    }
                    var disabledFields = "";
                    html += HU.div([ATTR_CLASS, "display-dialog-subheader"], "Displayed Fields");
                    html += HU.open(TAG_DIV, [ATTR_CLASS, "display-fields"]);
                    for (var tupleIdx = 0; tupleIdx < fields.length; tupleIdx++) {
                        var field = fields[tupleIdx];
                        var idBase = "cbx_" + collectionIdx + "_" + tupleIdx;
                        field.checkboxId = this.getDomId(idBase);
                        var on = false;
                        var hasValues = (flags ? flags[field.getIndex()] : true);
                        //                            console.log(tupleIdx + " field: " + field.getId() + "has values:" + hasValues);
                        if (argFields != null) {
                            //                                console.log("argFields:" + argFields);
                            for (var fIdx = 0; fIdx < argFields.length; fIdx++) {
                                if (argFields[fIdx].getId() == field.getId()) {
                                    on = true;
                                    //                                        console.log("argField:"+ argFields[fIdx].getId()+ " field.id:" + field.getId() +" on:" +on);
                                    this.overrideFields.push(field.getId());
                                    break;
                                }
                            }
                        } else if (selectedIds.length > 0) {
                            on = selectedIds.indexOf(field.getId()) >= 0;
                            //                                console.log("selected ids   on:" + on +" " + field.getId());
                        } else if (fixedFields != null) {
                            on = (fixedFields.indexOf(field.getId()) >= 0);
                            if (!on) {
                                on = (fixedFields.indexOf("#" + (tupleIdx + 1)) >= 0);
                            }
                            //                                console.log("fixed fields  on:" + on +" " + field.getId());
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
                                on = (i == 0);
                            }
                            //                                console.log("cbx fields:" + on + " " + field.getId());
                        }
                        var label = field.getUnitLabel();
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
                            var widget;
                            if (this.canDoMultiFields()) {
                                widget = HU.checkbox(field.checkboxId, [CLASS, checkboxClass], on);
                            } else {
                                widget = HU.radio(field.checkboxId, "field_radio", checkboxClass, "", on);
                            }

                            html += HU.tag(TAG_DIV, [ATTR_TITLE, field.getId()],
					   widget + " " + label
					  );
                        }
                        //                        html+= "<br>";
                    }
                }
                if (disabledFields != "") {
                    html += HU.div([STYLE, HU.css("border-top","1px #888  solid")], "<b>No Data Available</b>" + disabledFields);
                }
                html += HU.close(TAG_DIV);
            }


            this.writeHtml(ID_FIELDS, html);

            this.userHasSelectedAField = false;
            var theDisplay = this;
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
            var name = "the display";
            this.setDisplayTitle();
            if (this.displayData) {
                this.clearCachedData();
                this.displayData();
            }
        },
        defaultSelectedToAll: function() {
            return false;
        },
        setSelectedFields: function(fields) {
            this.clearCachedData();
            this.selectedFields = fields;
            this.addFieldsCheckboxes(fields);
        },

        getSelectedFields: function(dfltList) {
	    let debug = displayDebug.getSelectedFields;
	    if(debug)
		console.log(this.type +".getSelectedFields");
	    if(this.getProperty("binDate")) {
		var binType = this.getProperty("binType","total");
		var binCount = binType=="count";
		if(binCount) {
		    var fields = [];
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
		let tmp = [];
		this.lastSelectedFields.forEach(f=>{
		    if(f.getId().match(notFields) || f.getLabel().match(notFields)) return;
		    tmp.push(f);
		});
		this.lastSelectedFields = tmp;
	    }


	    if(debug)
		console.log("\tsetting lastSelectedFields:" + this.lastSelectedFields);
            var fixedFields = this.getPropertyFields();

	    //NOT NOW as this nukes the fields property
            //if (fixedFields) fixedFields.length = 0;

            this.setDisplayTitle();
	    if(this.getProperty("binDate")) {
		var binType = this.getProperty("binType","total");
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
	    }
	    //	    console.log("fields:" + this.lastSelectedFields);
	    return this.lastSelectedFields;
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
            var df = [];
            var dataList = this.dataCollection.getList();
            //If we have fixed fields then clear them after the first time
            var fixedFields = this.getPropertyFields();
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
	    var tmp = this.getProperty("fieldAliases");
	    if(tmp) {
		tmp.split(",").forEach(tok=>{
		    [name,alias] =   tok.split(":");
		    aliases[alias] = name;
		});
	    }
            for (var collectionIdx = 0; collectionIdx < dataList.length; collectionIdx++) {
                var pointData = dataList[collectionIdx];
                var fields = this.getFieldsToSelect(pointData);
                if (fixedFields != null && fixedFields.length > 0) {
                    if (this.debugSelected)
                        console.log("\thave fixed fields:" + fixedFields.length);
		    let selected = [];
                    for (var i = 0; i < fixedFields.length; i++) {
                        var sfield = fixedFields[i];
			if(sfield =="*") {
			    selected  =fields;
			    break;
			}
			var field = this.getFieldById(fields, sfield);
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

            if (fixedFields != null && fixedFields.length > 0) {
                if (this.debugSelected)
                    console.log("\tfrom fixed:" + df.length);
                return df;
            }

            var fieldsToSelect = null;
            var firstField = null;
            this.selectedCbx = [];
            var cbxExists = false;

            for (var collectionIdx = 0; collectionIdx < dataList.length; collectionIdx++) {
                var pointData = dataList[collectionIdx];
                fieldsToSelect = this.getFieldsToSelect(pointData);
                for (i = 0; i < fieldsToSelect.length; i++) {
                    var field = fieldsToSelect[i];
                    if (firstField == null && field.isNumeric()) firstField = field;
                    var idBase = "cbx_" + collectionIdx + "_" + i;
                    var cbxId = this.getDomId(idBase);
                    var cbx = $("#" + cbxId);
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
                df = this.getDefaultSelectedFields(fieldsToSelect, dfltList);
                if (this.debugSelected)
                    console.log("\tusing default selected:" + df);
            }
            return df;
        },
        getDefaultSelectedFields: function(fields, dfltList) {
	    let debug = displayDebug.getDefaultSelectedFields;
            if (this.defaultSelectedToAll() && this.allFields != null) {
                var tmp = [];
                for (i = 0; i < this.allFields.length; i++) {
                    var field = this.allFields[i];
                    if (!field.isFieldGeo()) {
                        tmp.push(field);
                    }
                }
		if(debug)
		    console.log("\treturning this.allFields:" + tmp);
                return tmp;
            }
            if (dfltList != null) {
		if(debug)
		    console.log("\treturning dfltList:" + dfltList);
                return dfltList;
            }
            for (i = 0; i < fields.length; i++) {
                var field = fields[i];
                if (field.isNumeric() && !field.isFieldGeo()) return [field];
            }
            return [];
	},
	sortRecords: function(records, sortFields) {
	    if(!sortFields) {
		sortFields = this.getFieldsByIds(null, this.getProperty("sortFields", "", true));
	    }
	    if(sortFields.length>0) {
		records = Utils.cloneList(records);
		var sortAscending = this.getProperty("sortAscending",true);
		let cnt = 0;
		records.sort((a,b)=>{
		    var row1 = this.getDataValues(a);
		    var row2 = this.getDataValues(b);
		    var result = 0;
		    for(var i=0;i<sortFields.length;i++) {
			var sortField = sortFields[i];
			var v1 = row1[sortField.getIndex()];
			var v2 = row2[sortField.getIndex()];
			if(sortField.isNumeric()) {
			    if(v1<v2) result = sortAscending?-1:1;
			    else if(v1>v2) result = sortAscending?1:-1;
			    else result = 0;
			} else {
			    result = String(v1).localeCompare(String(v2));
			    if(!sortAscending) result=-result;
			}
			if(result!=0) break;
		    }
		    return result;
		});
	    }

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

	    if(this.getProperty("reverse",false)) {
		records = Utils.cloneList(records);
		let tmp = [];
		for(let i=records.length-1;i>=0;i--)
		    tmp.push(records[i]);
		records = tmp;
	    }

	    return records;
	},
        getFieldById: function(fields, id) {
            if (!id) return null;
	    id = String(id).trim();
	    if (!fields) {
                let pointData = this.getData();
                if (pointData == null) return null;
                fields = pointData.getRecordFields();
            }
	    let aliases= {};
	    let tmp = this.getProperty("fieldAliases");
	    if(tmp) {
		tmp.split(",").forEach(tok=>{
		    [name,alias] =   tok.split(":");
		    aliases[alias] = name;
		});
	    }
	    let theField = null;
	    id.split("|").every(fieldId=>{
		let alias = aliases[fieldId];
		for (let i = 0; i < fields.length; i++) {
                    let field = fields[i];
                    if (field.getId() == fieldId || fieldId == ("#" + (i+1)) || field.getId()==alias) {
			theField =  field;
			return false;
                    }
		}
		return true;
	    });
            return theField;
        },

        getFieldsByIds: function(fields, ids) {
            var result = [];
            if (!ids) {
		return result;
	    }
            if ((typeof ids) == "string")
                ids = ids.split(",");
            if (!fields) {
                var pointData = this.getData();
                if (pointData == null) {
		    return null;
		}
                fields = pointData.getRecordFields();
            }
            for (var i = 0; i < ids.length; i++) {
                var f = this.getFieldById(fields, ids[i]);
                if (f) result.push(f);
            }
            return result;
        },

        getFieldOfType: function(fields, type) {
            fields = this.getFieldsOfType(fields, type);
            if (fields.length == 0) return null;
            return fields[0];
        },
        getFieldsOfType: function(fields, type) {
            if (!fields) {
                var pointData = this.getData();
                if (pointData == null) return null;
                fields = pointData.getRecordFields();
            }
            var list = [];
            var numeric = (type == "numeric");
            var isString = (type == "string");
            for (a in fields) {
                var field = fields[a];
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
	    var dates = [];
	    records.forEach(r=>{
		dates.push(r.getDate());
	    });
	    return dates;
	},
        getColumnValues: function(records, field) {
            var values = [];
            var min = Number.MAX_VALUE;
            var max = Number.MIN_VALUE;
            for (var rowIdx = 0; rowIdx < records.length; rowIdx++) {
                var record = records[rowIdx];
                var row = record.getData();
                var value = row[field.getIndex()];
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
		var pointData = this.getData();
                if (pointData == null) return null;
                records = pointData.getRecords();
            }
	    let treeTemplate = this.getProperty("treeTemplate");
	    let treeTooltip = this.getProperty("treeTooltip");
	    let roots = [];
	    let idToNode = {};
	    let nodes=[];
	    let idToRoot = {};
	    var labelField = this.getFieldById(null, this.getProperty("labelField"));
	    var nodeFields = this.getFieldsByIds(null, this.getProperty("nodeFields"));
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
		    var label= labelField==null?id:r.getValue(labelField.getIndex());		
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
		    var id= "leaf" + (cnt++);
		    var node = {id:id,label:label,children:[],record:r, parent:parentNode};
		    parentNode.children.push(node);
		    idToNode[id] = node;
		    nodes.push(node);
		});
		return roots;
	    }

	    //{label:..., id:...., record:...,	    children:[]}
            var parentField = this.getFieldById(null, this.getProperty("parentField"));
	    var idField = this.getFieldById(null, this.getProperty("idField"));
	    if(!parentField) {
		throw new Error("No parent field specified");
		return;
	    }
	    if(!idField) {
                throw new Error("No id field specified");
	    }
	    records.forEach(r=>{
		var parent = r.getValue(parentField.getIndex());
		var id = r.getValue(idField.getIndex());
		var label= labelField==null?id:r.getValue(labelField.getIndex());		
		var node = {id:id,label:label,children:[],record:r,parentId:parent, parent:null};
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
	    var segments = this.getProperty("timeSegments");
	    if(!segments) return null;
	    var result = [];
	    var segmentList = segments.split(",");
	    segmentList.forEach((tok,segmentIdx)=>{
		var toks = tok.split(";");
		var name = toks[0];
		var start = Utils.parseDate(toks[1],false);
		var end = Utils.parseDate(toks[2],false);
		result.push({name: name, start:start,end:end});
	    });
	    return  result;
	},
	convertPointData: function(pointData) {
	    let originalPointData = pointData;
	    let segments = this.getSegments();
	    if(segments) {
                let dataList = pointData.getRecords();
		let newData  =[];
		let header = [];
		newData.push(header);
		var rowIdx = 0; 
		//timeSegments="Obama;2008-02-01;2016-01-31,Trump;2016-02-01;2020-01-31"
		segments.forEach((segment,segmentIdx)=>{
		    var name = segment.name;
		    header.push(name);
		    var start = segment.start;
		    var end = segment.end;
		    var cnt = 1;
	    	    for (; rowIdx <dataList.length; rowIdx++) {
			var record = dataList[rowIdx];
			if(record.getTime()<start.getTime()) {
			    continue;
			}
			if(record.getTime()>end.getTime()) {
			    break;
			}
			var value = record.getValue(1);
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
		this.setErrorMessage(exc);
		//		console.log(exc.trace);
		return null;
	    }


	    return pointData;
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
	getDataFilters: function(v) {
	    return DataUtils.getDataFilters(this, v || this.getProperty("dataFilters"));
	},
	getFilterHighlight: function() {
	    return this.getProperty("filterHighlight",false);
	},
	filterData: function(records, fields, args) {
	    if(!args) args = {};
	    let opts = {
		doGroup:false,
		skipFirst:false,
		applyDateRange: true
	    }
	    $.extend(opts,args);
	    let debug = displayDebug.filterData;
	    let highlight =  this.getFilterHighlight();
	    let startDate = this.getProperty("startDate");
	    let endDate = this.getProperty("endDate");
	    if(startDate) {
		this.startDateObject = Utils.createDate(startDate);
	    } 
	    if(endDate) {
		this.endDateObject = Utils.createDate(endDate);
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
			    this.setDateRange(new Date(date.getFullYear()+"-01-01"),
					      new Date(date.getFullYear()+"-12-31"));
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

            let pointData = this.getData();
            if (!records) {
                if (pointData == null) return null;
                records = pointData.getRecords();
            }
            if (!fields) {
                fields = pointData.getRecordFields();
            }
            if(opts.doGroup || this.requiresGrouping()) {
                records = pointData.extractGroup(this.dataGroup, records);
            }


	    if(debug)   console.log("R-1:" + records.length);
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

	    records = records.filter(record=>{
                let date = record.getDate();
		if(!date) return true;
		return this.dateInRange(date);
	    });
	    if(debug)   console.log("filter Fields:" + this.filters.length +" #records:" + records.length);

	    if(this.filters.length) {
		let newData = [];
		let logic = this.getProperty("filterLogic","and");
		this.filters.forEach(f=>f.prepareToFilter());
		records.forEach((record,rowIdx)=>{
		    let allOk = true;
		    let anyOk = false;		    
		    for(let i=0;i<this.filters.length;i++) {
			let filter= this.filters[i];
			if(!filter.isEnabled()) continue;
			let filterOk = filter.isRecordOk(record);
			if(!filterOk) allOk = false;
			else anyOk = true;
		    }
		    let ok = logic=="and"?allOk:anyOk;
		    if(opts.skipFirst && rowIdx==0) {
			ok = true;
		    }
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

		records = newData;
	    }

	    if(debug)   console.log("R-2:" + records.length);

            var stride = parseInt(this.getProperty("stride", -1));
            if (stride < 0) {
		var maxSize = parseInt(this.getProperty("maxDisplayedPoints", -1));		
		if(maxSize>0 && records.length>0) {
		    stride = 1;
		    while(records.length/stride>maxSize) {
			stride++;
		    }
		}
	    }

            if (stride > 0) {
                var list = [];
                var cnt = 0;
                for (var i = 0; i < records.length; i += (stride + 1)) {
                    list.push(records[i]);
                }
                records = list;
		//		console.log("stride: " + stride +"  size:" + list.length);
		if(debug)   console.log("R-3:" + records.length);
            }


	    if(this.getProperty("binDate")) {
		let what = this.getProperty("binDate");
		let binType = this.getProperty("binType","total");
		let binCount = binType=="count";
		let binned = [];
		let record = records[0];
		let map ={};
		let counts ={};
		for (var i = 0; i < records.length; i++) {
		    let record = records[i];
		    var tuple = this.getDataValues(record);
		    var key;
		    var baseDate=null
		    if(what=="month") {
			key = record.getDate().getUTCFullYear() + "-" + (record.getDate().getUTCMonth() + 1);
		    } else if(what=="day") {
			key = record.getDate().getUTCFullYear() + "-" + (record.getDate().getUTCMonth() + 1) +"-" + record.getDate().getUTCDate();
		    } else if(what=="week") {
			var week = +Utils.formatDateWeek(record.getDate());
			key = record.getDate().getUTCFullYear()+"-"+week;
			var d =  (1 + (week - 1) * 7);
			baseDate = new Date(record.getDate().getUTCFullYear(), 0, d);			
		    } else {
			key = record.getDate().getUTCFullYear()+"";
		    }
		    if(!Utils.isDefined(map[key])) {
			counts[key]=1;
			var date = baseDate;
			if(!baseDate) {
			    date = Utils.parseDate(key);
			}
			var data = Utils.cloneList(record.getData());
			if(binCount) {
			    for(k=0;k<data.length;k++) data[k]=1;
			}
			var newRecord = new  PointRecord(fields, record.getLatitude(),record.getLongitude(),
							 record.getElevation(),date,data);
			map[key] = data;
			binned.push(newRecord);
		    } else {
			counts[key]++;
			var tuple1 = map[key];
			if(binCount) {
			    for(k=0;k<tuple1.length;k++) tuple1[k]++;
			    continue;
			} 
			var tuple2 = record.getData();
			for(var j=0;j<tuple2.length;j++) {
			    var v = tuple2[j];
			    if((typeof v) !="number") continue;
			    if(isNaN(v)) continue;
			    if(isNaN(tuple1[j])) tuple1[j] = v;
			    else tuple1[j] +=v;
			}
		    }
		}
		if(binType == "average") {
		    for(key in counts) {
			var tuple = map[key];
			for(var j=0;j<tuple.length;j++) {
			    var v = tuple[j];
			    if((typeof v) !="number") continue;
			    if(isNaN(v)) continue;
			    tuple[j] = v/counts[key];
			}
		    }
		}

		records = binned;
	    }

	    if(this.requiresGeoLocation()) {
		records = records.filter(r=>{return r.hasLocation();});
	    }
	    let dataFilters = this.getDataFilters();
	    if(dataFilters.length) {
		records = records.filter((r,idx)=> {
		    if(!this.checkDataFilters(dataFilters, r)) return false;
		    return true;
		});
	    }
	    //	    var t2=  new Date();
	    //	    Utils.displayTimes("filterData",[t1,t2]);
	    records = this.sortRecords(records);

	    if(this.getProperty("uniqueField")) {
		let ufield =  this.getFieldById(null, this.getProperty("uniqueField"));
		let umap = {};
		let ulist = [];
		for(var i=records.length-1;i>=0;i--) {
		    var record = records[i];
		    var v = record.getValue(ufield.getIndex());
		    if(!Utils.isDefined(umap[v])) {
			umap[v] = true;
			ulist.push(record);
		    }
		}
		records  = ulist;
	    }

	    this.recordToIndex = {};
	    this.indexToRecord = {};
	    for(var i=0;i<records.length;i++) {
		var record = records[i];
		this.recordToIndex[record.getId()] = i;
		this.indexToRecord[i] = record;
	    }
	    if(debug)
		console.log("filtered:" + records.length);
            return records;
        },
        canDoGroupBy: function() {
            return false;
        },
        canDoMultiFields: function() {
            return true;
        },
        useChartableFields: function() {
            return true;
        },
        getFieldsToSelect: function(pointData) {
            if (this.useChartableFields())
                return pointData.getChartableFields(this);
            return pointData.getRecordFields();
        },
        getGet: function() {
            return "getRamaddaDisplay('" + this.getId() + "')";
        },
	assembleWikiText: function(type) {
	    var wiki =  "";
	    if(window.globalDisplayProperties) {
		for(key in window.globalDisplayProperties) {
		    wiki += '{{displayProperty name="' + key +'" value="' + window.globalDisplayProperties[key]+'"}}\n';

		}
	    }
            wiki += this.getWikiText();
            for (var i = 0; i < this.displays.length; i++) {
                var display = this.displays[i];
                if (display.getIsLayoutFixed()) {
                    continue;
                }
                wiki += display.getWikiText();
            }
	    return wiki;
	},
        showWikiText: function(type) {
	    var wiki =  this.assembleWikiText();
	    popupObject = getTooltip();
	    wiki = wiki.replace(/</g,"&lt;").replace(/>/g,"&gt;");
	    wiki = HU.pre([STYLE,HU.css("max-width","500px","max-height","400px","overflow-x","auto","overflow-y","auto")], wiki);
	    this.showDialog(wiki);
	},
        copyWikiText: function(type) {
	    Utils.copyText(this.assembleWikiText());
	    alert("Wiki text has been copied to the clipboard");
	},
        publish: function(type) {
            if (type == null) type = "wikipage";
            var args = [];
            var name = prompt("Name", "");
            if (name == null) return;
            args.push("name");
            args.push(name);

            args.push("type");
            args.push(type);


            var desc = "";
            //                var desc = prompt("Description", "");
            //                if(desc == null) return;

            var wiki = "";
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
            var from = "";
            var entries = this.getChildEntries();
            for (var i = 0; i < entries.length; i++) {
                var entry = entries[i];
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
	    console.log(wiki);
            args.push(window.btoa(wiki));
            var url = HU.getUrl(ramaddaBaseUrl + "/entry/publish", args);
            window.open(url, '_blank');
        },
        getChildEntries: function(includeFixed) {
            var seen = {};
            var allEntries = [];
            for (var i = 0; i < this.displays.length; i++) {
                var display = this.displays[i];
                if (!includeFixed && display.getIsLayoutFixed()) {
                    continue;
                }
                if (display.getEntries) {
                    var entries = display.getEntries();
                    if (entries) {
                        for (var entryIdx = 0; entryIdx < entries.length; entryIdx++) {
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
            var allEntries = [];
            for (var i = 0; i < this.displays.length; i++) {
                var display = this.displays[i];
                if (display.getIsLayoutFixed()) {
                    continue;
                }
                if (display.getEntries) {
                    var entries = display.getEntries();
                    if (entries) {
                        for (var entryIdx = 0; entryIdx < entries.length; entryIdx++) {
                            allEntries.push(entries[entryIdx]);
                        }
                    }
                }
            }
            return this.copyEntries(allEntries);
        },
        defineWikiAttributes: function(list) {
            for (var i = 0; i < list.length; i++) {
                if (this.wikiAttrs.indexOf(list[i]) < 0) {
                    this.wikiAttrs.push(list[i]);
                }
            }
        },
        getWikiAttributes: function(attrs) {
            for (var i = 0; i < this.wikiAttrs.length; i++) {
                var v = this[this.wikiAttrs[i]];
                if (Utils.isDefined(v)) {
                    attrs.push(this.wikiAttrs[i]);
                    attrs.push(v);
                }
            }
        },
        getWikiText: function() {
            var attrs = [
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
            var entryId = null;
            if (this.getEntries) {
                var entries = this.getEntries();
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
            var wiki = "{{display " + HU.attrs(attrs) + "}}\n\n"

            return wiki;
        },
        copyEntries: function(entries) {
            var allEntries = [];
            var seen = {};
            for (var entryIdx = 0; entryIdx < entries.length; entryIdx++) {
                var entry = entries[entryIdx];
                if (seen[entry.getId()] != null) continue;
                seen[entry.getId()] = entry;
                allEntries.push(entry);
            }
            var from = "";
            for (var i = 0; i < allEntries.length; i++) {
                var entry = allEntries[i];
                from += entry.getId() + ",";
            }


            var url = ramaddaBaseUrl + "/entry/copy?action.force=copy&from=" + from;
            window.open(url, '_blank');

        },
        entryHtmlHasBeenDisplayed: async function(entry) {
            if (entry.getIsGroup() /* && !entry.isRemote*/ ) {
                var theDisplay = this;
                var callback = function(entries) {
                    var html = HU.open(TAG_OL, [ATTR_CLASS, "display-entrylist-list", ATTR_ID, theDisplay.getDomId(ID_LIST)]);
                    html += theDisplay.getEntriesTree(entries);
                    html += HU.close(TAG_OL);
                    theDisplay.jq(ID_GROUP_CONTENTS + entry.getIdForDom()).html(html);
                    theDisplay.addEntrySelect();
                };
                await entry.getChildrenEntries(callback);
            }
        },
        getEntryHtml: function(entry, props) {
            var dfltProps = {
                showHeader: true,
                headerRight: false,
                showDetails: this.getShowDetails()
            };
            $.extend(dfltProps, props);

            props = dfltProps;
            var menu = this.getEntryMenuButton(entry);
            var html = "";
            if (props.showHeader) {
                var left = menu + " " + entry.getLink(entry.getIconImage() + " " + entry.getName());
                if (props.headerRight) html += HU.leftRight(left, props.headerRight);
                else html += left;
                //                    html += "<hr>";
            }
            var divid = HU.getUniqueId("entry_");
            html += HU.div([ID, divid], "");

            if (false) {
                var url = this.getRamadda().getRoot() + "/entry/show?entryid=" + entry.getId() + "&decorate=false&output=metadataxml&details=true";
                //                console.log(url);
                $("#" + divid).load(url, function() {
                    alert("Load was performed.");
                });
            }

            var desc = entry.getDescription();
            if (desc)
                desc = desc.replace(/\n/g, "<br>");
            else
                desc = "";
            html += desc;

            var metadata = entry.getMetadata();
            if (entry.isImage()) {
                var img = HU.tag(TAG_IMG, ["src", entry.getResourceUrl(), /*ATTR_WIDTH,"100%",*/
					   ATTR_CLASS, "display-entry-image"
					  ]);

                html += HU.href(entry.getResourceUrl(), img) + "<br>";
            } else {
                for (var i = 0; i < metadata.length; i++) {
                    if (metadata[i].type == "content.thumbnail") {
                        var image = metadata[i].value.attr1;

                        var url;
                        if (image.indexOf("http") == 0) {
                            url = image;
                        } else {
                            url = ramaddaBaseUrl + "/metadata/view/" + image + "?element=1&entryid=" + entry.getId() + "&metadata_id=" + metadata[i].id + "&thumbnail=false";
                        }
                        html += HU.image(url, [ATTR_CLASS, "display-entry-thumbnail"]);
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

            var columns = entry.getAttributes();

            if (entry.getFilesize() > 0) {
                html += HU.formEntry("File:", entry.getFilename() + " " +
				     HU.href(entry.getResourceUrl(), HU.image(ramaddaBaseUrl + "/icons/download.png")) + " " +
				     entry.getFormattedFilesize());
            }
            for (var colIdx = 0; colIdx < columns.length; colIdx++) {
                var column = columns[colIdx];
                var columnValue = column.value;
                if (column.getCanShow && !column.getCanShow()) {
                    continue;
                }
                if (Utils.isFalse(column.canshow)) {
                    continue;
                }

                if (column.isUrl && column.isUrl()) {
                    var tmp = "";
                    var toks = columnValue.split("\n");
                    for (var i = 0; i < toks.length; i++) {
                        var url = toks[i].trim();
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
            var columns = this.getProperty("entryColumns", null);
            if (columns != null) {
                var columnNames = this.getProperty("columnNames", null);
                if (columnNames != null) {
                    columnNames = columnNames.split(",");
                }
                columns = columns.split(",");
                var ids = [];
                var names = [];
                for (var i = 0; i < columns.length; i++) {
                    var toks = columns[i].split(":");
                    var id = null,
                        name = null;
                    if (toks.length > 1) {
                        if (toks[0] == "property") {
                            name = "property";
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

            var suffix = props.suffix;
            var domIdSuffix = "";
            if (!suffix) {
                suffix = "null";
            } else {
                domIdSuffix = suffix;
                suffix = "'" + suffix + "'";
            }

            var handler = getHandler(props.handlerId);
            var showIndex = props.showIndex;
            var html = "";
            var rowClass = "entryrow_" + this.getId();
            var even = true;
            if (this.entriesMap == null)
                this.entriesMap = {};
            for (var i = 0; i < entries.length; i++) {
                even = !even;
                var entry = entries[i];
                this.entriesMap[entry.getId()] = entry;
                var toolbar = this.makeEntryToolbar(entry, handler, props.handlerId);
                var entryMenuButton = this.getEntryMenuButton(entry);

                var entryName = entry.getDisplayName();
                if (entryName.length > 100) {
                    entryName = entryName.substring(0, 99) + "...";
                }
                var icon = entry.getIconImage([ATTR_TITLE, "View entry"]);
                var link = HU.tag(TAG_A, [ATTR_HREF, entry.getEntryUrl()], icon + " " + entryName);
                entryName = "";
                var entryIdForDom = entry.getIdForDom() + domIdSuffix;
                var entryId = entry.getId();
                var arrow = HU.image(icon_tree_closed, [ATTR_BORDER, "0",
							"tree-open", "false",
							ATTR_ID,
							this.getDomId(ID_TREE_LINK + entryIdForDom)
						       ]);
                var toggleCall = this.getGet() + ".toggleEntryDetails(event, '" + entryId + "'," + suffix + ",'" + props.handlerId + "');";
                var toggleCall2 = this.getGet() + ".entryHeaderClick(event, '" + entryId + "'," + suffix + "); ";
                var open = HU.onClick(toggleCall, arrow);
                var extra = "";

                if (showIndex) {
                    extra = "#" + (i + 1) + " ";
                }
                if (handler && handler.getEntryPrefix) {
                    extra += handler.getEntryPrefix(props.handlerId, entry);
                }
                var left = HU.div([ATTR_CLASS, "display-entrylist-name"], entryMenuButton + " " + open + " " + extra + link + " " + entryName);
                var details = HU.div([ATTR_ID, this.getDomId(ID_DETAILS + entryIdForDom), ATTR_CLASS, "display-entrylist-details"], HU.div([ATTR_CLASS, "display-entrylist-details-inner", ATTR_ID, this.getDomId(ID_DETAILS_INNER + entryIdForDom)], ""));

                //                    console.log("details:" + details);

                var line;
                if (this.getProperty("showToolbar", true)) {
                    line = HU.leftCenterRight(left, "", toolbar, "80%", "1%", "19%");
                } else {
                    line = left;
                }
                //                    line = HU.leftRight(left,toolbar,"60%","30%");


                var mainLine = HU.div(["onclick", toggleCall2, ATTR_ID, this.getDomId(ID_DETAILS_MAIN + entryIdForDom), ATTR_CLASS, "display-entrylist-entry-main" + " " + "entry-main-display-entrylist-" + (even ? "even" : "odd"), ATTR_ENTRYID, entryId], line);
                var line = HU.div([CLASS, (even ? "ramadda-row-even" : "ramadda-row-odd"), ATTR_ID, this.getDomId("entryinner_" + entryIdForDom)], mainLine + details);

                html += HU.tag(TAG_DIV, [ATTR_ID,
					 this.getDomId("entry_" + entryIdForDom),
					 ATTR_ENTRYID, entryId, ATTR_CLASS, "display-entrylist-entry" + rowClass
					], line);
                html += "\n";
            }
            return html;
        },
        addEntrySelect: function() {
            var theDisplay = this;
            var entryRows = $("#" + this.getDomId(ID_DISPLAY_CONTENTS) + "  ." + this.getClass("entry-main"));

            entryRows.unbind();
            entryRows.mouseover(async function(event) {
                //TOOLBAR
                var entryId = $(this).attr(ATTR_ENTRYID);
                var entry;
                await theDisplay.getEntry(entryId, e => {
                    entry = e
                });
                if (!entry) {
                    console.log("no entry:" + entryId);
                    return;
                }
                theDisplay.propagateEvent("handleEventEntryMouseover", {
                    entry: entry
                });


                if (true) return;
                var domEntryId = Utils.cleanId(entryId);
                var toolbarId = theDisplay.getEntryToolbarId(domEntryId);

                var toolbar = $("#" + toolbarId);
                toolbar.show();
                var myalign = 'right top+1';
                var atalign = 'right top';
                var srcId = theDisplay.getDomId(ID_DETAILS_MAIN + domEntryId);
                toolbar.position({
                    of: $("#" + srcId),
                    my: myalign,
                    at: atalign,
                    collision: "none none"
                });

            });
            entryRows.mouseout(async function(event) {
                var entryId = $(this).attr(ATTR_ENTRYID);
                var entry;
                await theDisplay.getEntry(entryId, e => {
                    entry = e
                });
                if (!entry) return;
                theDisplay.propagateEvent("handleEventEntryMouseout", {
                    entry: entry
                });
                var domEntryId = Utils.cleanId(entryId);
                var toolbarId = theDisplay.getEntryToolbarId(entryId);
                var toolbar = $("#" + toolbarId);
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
                        var entryId = ui.selected.getAttribute(ATTR_ENTRYID);
                        theDisplay.toggleEntryDetails(event, entryId);
                        if (true) return;

                        theDisplay.hideEntryDetails(entryId);
                        var entry;
                        await this.getEntry(entryId, e => {
                            entry = e
                        });
                        if (entry == null) return;

                        var zoom = null;
                        if (event.shiftKey) {
                            zoom = {
                                zoomIn: true
                            };
                        }
                        theDisplay.selectedEntries.push(entry);
                        theDisplay.propagateEvent("handleEventEntrySelection", {
                            entry: entry,
                            selected: true,
                            zoom: zoom
                        });
                        theDisplay.lastSelectedEntry = entry;
                    },
                    unselected: async function(event, ui) {
                        if (true) return;
                        var entryId = ui.unselected.getAttribute(ATTR_ENTRYID);
                        var entry;
                        await this.getEntry(entryId, e => {
                            entry = e
                        });
                        var index = theDisplay.selectedEntries.indexOf(entry);
                        //                            console.log("remove:" +  index + " " + theDisplay.selectedEntries);
                        if (index > -1) {
                            theDisplay.selectedEntries.splice(index, 1);
                            theDisplay.propagateEvent("handleEventEntrySelection", {
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
            var columnWidths = this.getProperty("columnWidths", null);
            if (columnWidths != null) {
                columnWidths = columnWidths.split(",");
            }
            var html = HU.open(TAG_TABLE, [ATTR_WIDTH, "100%", "cellpadding", "0", "cellspacing", "0"]);
            html += HU.open(TAG_TR, ["valign", "top"]);
            for (var i = 0; i < columnNames.length; i++) {
                html += HU.td([ATTR_ALIGN, "center", ATTR_CLASS, "display-entrytable-header"], columnNames[i]);
            }
            html += HU.close(TAG_TR);

            for (var i = 0; i < entries.length; i++) {
                html += HU.open(TAG_TR, ["valign", "top"]);
                var entry = entries[i];
                this.entriesMap[entry.getId()] = entry;
                for (var j = 0; j < columns.length; j++) {
                    var columnWidth = null;
                    if (columnWidths != null) {
                        columnWidth = columnWidths[j];
                    }
                    var column = columns[j];
                    var value = "";
                    if (column == "name") {
                        value = HU.tag(TAG_A, [ATTR_HREF, entry.getEntryUrl()], entry.getName());
                    } else if (column.match(".*property:.*")) {
                        var type = column.substring("property:".length);
                        var metadata = entry.getMetadata();
                        value = "";
                        for (var j = 0; j < metadata.length; j++) {
                            var m = metadata[j];
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
                    var attrs = [ATTR_CLASS, "display-entrytable-cell"];
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
            var get = this.getGet();
            var toolbarItems = [];
	    var props = "{showMenu:true,showTitle:true}";
            //                 toolbarItems.push(HU.tag(TAG_A, [ATTR_HREF, entry.getEntryUrl(),"target","_"], 
            //                                                HU.image(ramaddaBaseUrl +"/icons/application-home.png",["border",0,ATTR_TITLE,"View Entry"])));
            if (entry.getType().getId() == "type_wms_layer") {
                toolbarItems.push(HU.tag(TAG_A, ["onclick", get + ".addMapLayer(" + HU.sqt(entry.getId()) + ");"],
					 HU.image(ramaddaBaseUrl + "/icons/map.png", ["border", 0, ATTR_TITLE, "Add Map Layer"])));

            }
            if (entry.getType().getId() == "geo_shapefile" || entry.getType().getId() == "geo_geojson") {
                toolbarItems.push(HU.tag(TAG_A, ["onclick", get + ".addMapLayer(" + HU.sqt(entry.getId()) + ");"],
					 HU.image(ramaddaBaseUrl + "/icons/map.png", ["border", 0, ATTR_TITLE, "Add Map Layer"])));

            }

            var jsonUrl = this.getPointUrl(entry);
            if (jsonUrl != null) {
                jsonUrl = jsonUrl.replace(/\'/g, "_");
                toolbarItems.push(HU.tag(TAG_A, ["onclick", get + ".createDisplay(" + HU.sqt(entry.getFullId()) + "," +
						 HU.sqt("table") + "," + HU.sqt(jsonUrl) + "," + props+");"
						],
					 HU.getIconImage("fa-table", [ATTR_TITLE, "Create Tabular Display"])));

                var x;
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
                toolbarItems.push(HU.tag(TAG_A, [ATTR_HREF, entry.getResourceUrl()],
					 HU.image(ramaddaBaseUrl + "/icons/download.png", ["border", 0, ATTR_TITLE, "Download (" + entry.getFormattedFilesize() + ")"])));

            }


            var entryMenuButton = this.getEntryMenuButton(entry);
            /*
              entryMenuButton =  HU.onClick(this.getGet()+".showEntryDetails(event, '" + entry.getId() +"');", 
              HU.image(ramaddaBaseUrl+"/icons/downdart.png", 
              [ATTR_CLASS, "display-dialog-button", ATTR_ID,  this.getDomId(ID_MENU_BUTTON + entry.getId())]));

            */
            //            toolbarItems.push(entryMenuButton);

            var tmp = [];



            if (handler && handler.addToToolbar) {
                handler.addToToolbar(handlerId, entry, toolbarItems);
            }

            for (var i = 0; i < toolbarItems.length; i++) {
                tmp.push(HU.div([ATTR_CLASS, "display-entry-toolbar-item"], toolbarItems[i]));
            }
            toolbarItems = tmp;
            return HU.div([ATTR_CLASS, "display-entry-toolbar", ATTR_ID,
			   this.getEntryToolbarId(entry.getIdForDom())
			  ],
			  HU.join(toolbarItems, ""));
        },
        getEntryToolbarId: function(entryId) {
            var id = entryId.replace(/:/g, "_");
            id = id.replace(/=/g, "_");
            return this.getDomId(ID_TOOLBAR + "_" + id);
        },

        hideEntryDetails: function(entryId) {
            //                var popupId = "#"+ this.getDomId(ID_DETAILS + entryId);
            //                $(popupId).hide();
            //                this.currentPopupEntry = null;
        },
        entryHeaderClick: function(event, entryId, suffix) {
            var target = event.target;
            //A hack to see if this was the div clicked on or a link in the div
            if (target.outerHTML) {
                if (target.outerHTML.indexOf("<div") != 0) {
                    return;
                }
            }
            this.toggleEntryDetails(event, entryId);
        },
        toggleEntryDetails: async function(event, entryId, suffix, handlerId) {
            var entry;
            await this.getEntry(entryId, e => {
                entry = e
            });
            //                console.log("toggleEntryDetails:" + entry.getName() +" " + entry.getId());
            if (suffix == null) suffix = "";
            var link = this.jq(ID_TREE_LINK + entry.getIdForDom() + suffix);
            var id = ID_DETAILS + entry.getIdForDom() + suffix;
            var details = this.jq(id);
            var detailsInner = this.jq(ID_DETAILS_INNER + entry.getIdForDom() + suffix);


            if (event && event.shiftKey) {
                var id = Utils.cleanId(entryId);
                var line = this.jq(ID_DETAILS_MAIN + id);
                if (!this.selectedEntriesFromTree) {
                    this.selectedEntriesFromTree = {};
                }
                var selected = line.attr("ramadda-selected") == "true";
                if (selected) {
                    line.removeClass("display-entrylist-entry-main-selected");
                    line.attr("ramadda-selected", "false");
                    this.selectedEntriesFromTree[entry.getId()] = null;
                } else {
                    line.addClass("display-entrylist-entry-main-selected");
                    line.attr("ramadda-selected", "true");
                    this.selectedEntriesFromTree[entry.getId()] = entry;
                }
                this.propagateEvent("handleEventEntrySelection", {
                    "entry": entry,
                    "selected": !selected
                });
                return;
            }


            var open = link.attr("tree-open") == "true";
            if (open) {
                link.attr("src", icon_tree_closed);
            } else {
                link.attr("src", icon_tree_open);
            }
            link.attr("tree-open", open ? "false" : "true");

            var hereBefore = details.attr("has-content") != null;
            details.attr("has-content", "true");
            if (hereBefore) {
                //                    detailsInner.html(HU.image(icon_progress));
            } else {
                if (entry.getIsGroup() /* && !entry.isRemote*/ ) {
                    detailsInner.html(HU.image(icon_progress));
                    let _this = this;
                    var callback = function(entries) {
                        _this.displayChildren(entry, entries, suffix, handlerId);
                    };
                    var entries = entry.getChildrenEntries(callback);
                } else {
                    detailsInner.html(this.getEntryHtml(entry, {
                        showHeader: false
                    }));
                }
            }


            if (open) {
                details.hide();
            } else {
                details.show();
            }
            if (event && event.stopPropagation) {
                event.stopPropagation();
            }
        },
        getSelectedEntriesFromTree: function() {
            var selected = [];
            if (this.selectedEntriesFromTree) {
                for (var id in this.selectedEntriesFromTree) {
                    var entry = this.selectedEntriesFromTree[id];
                    if (entry != null) {
                        selected.push(entry);
                    }
                }
            }
            return selected;
        },
        displayChildren: function(entry, entries, suffix, handlerId) {
            if (!suffix) suffix = "";
            var detailsInner = this.jq(ID_DETAILS_INNER + entry.getIdForDom() + suffix);
            var details = this.getEntryHtml(entry, {
                showHeader: false
            });
            if (entries.length == 0) {
                detailsInner.html(details);
            } else {
                var entriesHtml = "";
                if (this.showDetailsForGroup) {
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
            var menuButton = HU.onClick(this.getGet() + ".showEntryMenu(event, '" + entry.getId() + "');",
					HU.image(ramaddaBaseUrl + "/icons/menu.png",
						 [ATTR_CLASS, "display-entry-toolbar-item", ATTR_ID, this.getDomId(ID_MENU_BUTTON + entry.getIdForDom())]));
            return menuButton;
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
            var ramadda = this.getRamadda();
            var toks = entryId.split(",");
            if (toks.length == 2) {
                entryId = toks[1];
                ramadda = getRamadda(toks[0]);
            }
            var entry = null;
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
            var entry;
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
            var entry;
            await this.getEntry(entryId, e => {
                entry = e
            });
            if (entry == null) {
                console.log("No entry:" + entryId);
                return null;
            }
            var props = {
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
                var pointDataProps = {
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
            var service = entry.getService("points.json");
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
            var entry;
            await this.getEntry(entryId, e => {
                entry = e
            });
            if (entry == null) {
                return Utils.call(callback, "null entry");
            }

            var get = this.getGet();
            var menus = [];
            var fileMenuItems = [];
            var viewMenuItems = [];
            var newMenuItems = [];
            viewMenuItems.push(HU.tag(TAG_LI, [], HU.tag(TAG_A, ["href", entry.getEntryUrl(), "target", "_"], "View Entry")));
            if (entry.getFilesize() > 0) {
                fileMenuItems.push(HU.tag(TAG_LI, [], HU.tag(TAG_A, ["href", entry.getResourceUrl()], "Download " + entry.getFilename() + " (" + entry.getFormattedFilesize() + ")")));
            }

            if (this.jsonUrl != null) {
                fileMenuItems.push(HU.tag(TAG_LI, [], "Data: " + HU.onClick(get + ".fetchUrl('json');", "JSON") +
					  HU.onClick(get + ".fetchUrl('csv');", "CSV")));
            }

	    var props = "{showMenu:true,showTitle:true}";
            var newMenu = "<a>New</a><ul>";
            newMenu += HU.tag(TAG_LI, [], HU.onClick(get + ".createDisplay('" + entry.getFullId() + "','entrydisplay',null,null," + props+");", "New Entry Display"));
            newMenuItems.push(HU.tag(TAG_LI, [], HU.onClick(get + ".createDisplay('" + entry.getFullId() + "','entrydisplay',null,null," + props+");", "New Entry Display")));

a
            //check if it has point data
            var pointUrl = this.getPointUrl(entry);
            //            console.log("entry:" + entry.getName() + " url:" + pointUrl);

            if (pointUrl != null) {
                var types = window.globalDisplayTypes;
                var catMap = {};
                if (types) {
                    for (var i = 0; i < types.length; i++) {
                        var type = types[i];

                        if (!type.requiresData || !type.forUser) continue;
                        if (!Utils.isDefined(catMap[type.category])) {
                            catMap[type.category] = "<li> <a>" + type.category + "</a><ul>\n";
                        }
                        pointUrl = pointUrl.replace(/\'/g, "_");
                        var call = get + ".createDisplay(" + HU.sqt(entry.getFullId()) + "," + HU.sqt(type.type) + "," + HU.sqt(pointUrl) + ",null," + props +");";
                        var li = HU.tag(TAG_LI, [], HU.tag(TAG_A, ["onclick", call], type.label));
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


            var topMenus = "";
            for (var i = 0; i < menus.length; i++) {
                topMenus += HU.tag(TAG_LI, [], menus[i]);
            }

            var menu = HU.tag(TAG_UL, [ATTR_ID, this.getDomId(ID_MENU_INNER + entry.getIdForDom()), ATTR_CLASS, "sf-menu"],
			      topMenus);
            callback(menu);
        },
        showEntryMenu: async function(event, entryId) {
            var menu;
            await this.getEntryMenu(entryId, m => {
                menu = m
            });
            this.writeHtml(ID_MENU_OUTER, menu);
            var srcId = this.getDomId(ID_MENU_BUTTON + Utils.cleanId(entryId));
            showPopup(event, srcId, this.getDomId(ID_MENU_OUTER), false, "left top", "left bottom");
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
            var get = "getRamaddaDisplay('" + this.getId() + "')";
            var moveRight = HU.onClick(get + ".moveDisplayRight();", "Right");
            var moveLeft = HU.onClick(get + ".moveDisplayLeft();", "Left");
            var moveTop = HU.onClick(get + ".moveDisplayTop();", "Top");
            var moveUp = HU.onClick(get + ".moveDisplayUp();", "Up");
            var moveDown = HU.onClick(get + ".moveDisplayDown();", "Down");


            var menu = HU.open(TABLE,[CLASS,'formtable']) +
                "<tr><td align=right><b>Move:</b></td><td>" + moveTop + " " + moveUp + " " + moveDown + " " + moveRight + " " + moveLeft + "</td></tr>" +
                "<tr><td align=right><b>Row:</b></td><td> " + HU.input("", this.getProperty("row", ""), ["size", "7", ATTR_ID, this.getDomId("row")]) + " &nbsp;&nbsp;<b>Col:</b> " + HU.input("", this.getProperty("column", ""), ["size", "7", ATTR_ID, this.getDomId("column")]) + "</td></tr>" +
                "<tr><td align=right><b>Width:</b></td><td> " + HU.input("", this.getProperty("width", ""), ["size", "7", ATTR_ID, this.getDomId("width")]) + "  " + "<b>Height:</b> " + HU.input("", this.getProperty("height", ""), ["size", "7", ATTR_ID, this.getDomId("height")]) + "</td></tr>" +
                "</table>";
            var tmp =
                HU.checkbox(this.getDomId("showtitle"), [], this.showTitle) + " Title  " +
                HU.checkbox(this.getDomId("showdetails"), [], this.showDetails) + " Details " +
                "&nbsp;&nbsp;&nbsp;" +
                HU.onClick(get + ".askSetTitle();", "Set Title");
            menu += HU.formTable() + HU.formEntry("Show:", tmp) + HU.close(TABLE);

            return menu;
        },
        isLayoutHorizontal: function() {
	    return this.getProperty("orientation","horizontal")== "horizontal";
        },
        loadInitialData: function() {
	    if(!this.getProperty("okToLoadData",true)) return;
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
            if (!this.hasData()) return null;
            var dataList = this.dataCollection.getList();
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
            var t = this.getTitle(false);
            var v = prompt(TITLE, t);
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
            this.showTitle = v;
            if (this.showTitle) {
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
            var column = parseInt(this.getProperty("column", 0));
            column += delta;
            if (column < 0) column = 0;
            this.setDisplayProperty("column", column);
            this.getLayoutManager().layoutChanged(this);
	    this.jq("col").val(column);
        },
        deltaRow: function(delta) {
            var row = parseInt(this.getProperty("row", 0));
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
        getDisplayDialogContents: function(tabTitles, tabContents) {
            var get = this.getGet();
            var menuItems = [];
            this.getMenuItems(menuItems);
            var form = "<form>";

            form += this.getDisplayMenuSettings();
            for (var i = 0; i < menuItems.length; i++) {
                form += HU.div([ATTR_CLASS, "display-menu-item"], menuItems[i]);
            }
            form += "</form>";
            tabTitles.push("Display"); 
            tabContents.push(form);
        },	
        checkLayout: function() {},
        displayData: function() {},
        setDisplayReady: function() {
	    var callUpdate = !this.displayReady;
            this.displayReady = true;
	    if(callUpdate) {
		this.callUpdateUI();
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
	doFinalInitialization:function() {
	},
        initDisplay: function() {
            this.createUI();
	    if(this.getAnimation().getEnabled()) {
		this.getAnimation().makeControls();
            }
            this.checkSearchBar();
	    this.callUpdateUI();
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
	    return $("#" + this.getProperty(PROP_DIVID));
	},
        createUI: function() {
            var divid = this.getProperty(PROP_DIVID);
            if (divid != null) {
                var html = this.getHtml();
		let div = $("#" + divid);
		let inline = this.getProperty("displayInline");
		if(inline) {
		    div.css("display","inline-block");
		    div.css("vertical-align","bottom");
		} 
		let width = this.getWidth("100%");
		if(width && width!="-1") {
                    div.css("width",HU.getDimension(width));
		}
		div.html(html);
            } else {
                console.log("error: no div defined for display:" + this.getType());
            }
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
            let get = this.getGet();
            let button = "";

            if (this.getShowMenu()) {
                button = HU.onClick(get + ".showDialog();",
				    HU.image(ramaddaBaseUrl + "/icons/downdart.png",
					     [ATTR_CLASS, "display-dialog-button", ATTR_ID, this.getDomId(ID_MENU_BUTTON)]));
		button+=" ";
            }
	    if(this.getProperty("showProgress",false)) {
		//		button += HU.image(icon_progress,[ID,this.getDomId(ID_DISPLAY_PROGRESS)]);
	    }
            let title = "";
            if (this.getShowTitle()) {
                title = this.getTitle(false).trim();
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
	    let h2Separate = this.getAnimationEnabled();
	    let h1 = 	HU.div([ID,this.getDomId(ID_HEADER1),CLASS,"display-header-block display-header1"], "");
	    let h2 = HU.div([ID,this.getDomId(ID_HEADER2),CLASS,"display-header-block display-header2"], "");
            let topCenter = HU.div([ID, this.getDomId(ID_TOP),CLASS,"display-header-block"], h2Separate?"":h2);
            let topRight = HU.div([ID, this.getDomId(ID_TOP_RIGHT)], "");
	    let top =  HU.leftCenterRight(topLeft, topCenter, topRight, null, null, null,{
                valign: "bottom"
            });
            let header = h1;
	    if(h2Separate) header+=h2;
	    top =  header +  top;	    


	    let colorTable = HU.div([ID,this.getDomId(ID_COLORTABLE)]);
	    let rightInner="";
	    let leftInner="";

	    let bottom = HU.div([ATTR_CLASS, "", ATTR_ID, this.getDomId(ID_BOTTOM)]);
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
	    bottom+=legend;
	    let left = HU.div([ATTR_ID, this.getDomId(ID_LEFT)],leftInner);
	    let right = HU.div([ATTR_ID, this.getDomId(ID_RIGHT)],rightInner);


	    let sideWidth = "1%";
            let contents = this.getContentsDiv();
            let table =   HU.open("table", ["width","100%","border",0]);
	    table+= HU.tr([],HU.td(["width",sideWidth]) + HU.td(["width","99%"],top) +HU.td(["width",sideWidth]));
	    table+= HU.tr([],HU.td(["width",sideWidth],left) + HU.td(["width","99%"],contents) +HU.td(["width",sideWidth],right));
	    table+= HU.tr([],HU.td(["width",sideWidth]) + HU.td(["width","99%"],bottom) +HU.td(["width",sideWidth]));
	    table+=HU.close("table");

            let html =  HU.div([ATTR_CLASS, "ramadda-popup", ATTR_ID, this.getDomId(ID_MENU_OUTER)], "");
            let style = this.getProperty("displayStyle", "");
            html += HU.div([CLASS, "display-contents", STYLE, style],table);
            return html;
        },
        getWidthForStyle: function(dflt) {
            var width = this.getProperty("width", -1);
            if (width == -1) return dflt;
	    return HU.getDimension(width);
        },
        getHeightForStyle: function(dflt) {
            var height = this.getProperty("height", -1);
            if (height == -1) return dflt;
            if (height.match("^[0-9]+$"))
                height = height + "px";
            return height;
        },
        getContentsStyle: function() {
            var style = "";
            var height = this.getHeightForStyle();
            if (height) {
                style += " height:" + height + ";overflow-y:auto;";
            }
	    //            var width = this.getWidthForStyle();
	    //            if (width) {
	    //                style += " width:" + width + ";";
	    //            }
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
	    let contentsAttrs =[ATTR_CLASS, this.getContentsClass(), STYLE, style, ATTR_ID, this.getDomId(ID_DISPLAY_CONTENTS)];
	    if(this.getProperty("expandableHeight")) {
		contentsAttrs.push("expandable-height");
		contentsAttrs.push(this.getProperty("expandableHeight"));
	    }
	    let contents =  top + "\n" +HU.div(contentsAttrs, "") + "\n" +bottom;
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
            var text = title;
            var fields = this.lastSelectedFields;
            if (fields && fields.length > 0)
                text = text.replace("{field}", fields[0].getLabel());
            else
                text = text.replace("{field}", "");
            return text;
        },

        setDisplayTitle: function(title) {
            if (!Utils.stringDefined(title)) {
                title = this.getTitle(false).trim();
            }
            var text = this.getTitleHtml(title);
            if (this.getShowTitle()) {
                this.jq(ID_TITLE).show();
            } else {
                this.jq(ID_TITLE).hide();
            }
            this.writeHtml(ID_TITLE, text);
        },
        getTitle: function(showMenuButton) {
            var prefix = "";
            if (showMenuButton && this.hasEntries()) {
                prefix = this.getEntryMenuButton(this.getEntries()[0]) + " ";
            }
            var title = this.getProperty(ATTR_TITLE);
            if (title != null) {
                return prefix + title;
            }
            if (this.dataCollection == null) {
                return prefix;
            }
            var dataList = this.dataCollection.getList();
            title = "";
            for (var collectionIdx = 0; collectionIdx < dataList.length; collectionIdx++) {
                var pointData = dataList[collectionIdx];
                if (collectionIdx > 0) title += "/";
                title += pointData.getName();
            }

            return prefix + title;
        },
        getIsLayoutFixed: function() {
            return this.getProperty(PROP_LAYOUT_HERE, true);
        },

        makeToolbar: function(props) {
            var toolbar = "";
            var get = this.getGet();
            var addLabel = props.addLabel;
            var images = [];
            var calls = [];
            var labels = [];
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
                images.push(ramaddaBaseUrl + "/icons/json.png");
                labels.push("Download JSON");

                calls.push(get + ".fetchUrl('csv');");
                images.push(ramaddaBaseUrl + "/icons/csv.png");
                labels.push("Download CSV");
            }
            for (var i = 0; i < calls.length; i++) {
                var inner = HU.getIconImage(images[i], [ATTR_TITLE, labels[i], ATTR_CLASS, "display-dialog-header-icon"]);
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

        forceUpdateUI: function() {
	    this.haveCalledUpdateUI = false;
	    this.callUpdateUI();
	},
	callUpdateUI: function() {
	    try {
		this.updateUI();
	    } catch(err) {
                this.setContents(this.getMessage(err));
		console.log("Error:" + err);
		console.log(err.stack);
	    }
	},
        updateUI: function() {
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
	    let macros =[];
	    let p = this.getProperty("requestFields","");
	    let e1 = this.getProperty("extraFields1","");
	    let e2 = this.getProperty("extraFields2","");
	    let list = Utils.mergeLists(e1.split(","),p.split(","),e2.split(","));
	    if(p!="")
		console.log("requestFields=" + p);
	    list.forEach(macro=>{
		if(macro=="") return;
		macros.push(new RequestMacro(this, macro));
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
	    macros.forEach(macro=>{
		requestProps+=macro.getWidget(macroDateIds) +"&nbsp;&nbsp;";
		if(macro.isVisible()) {
		    requestProps+=SPACE2;
		}
	    });
	    this.writeHeader(ID_REQUEST_PROPERTIES, requestProps);
	    let macroChange = (macro,value,what)=>{
		if(this.settingMacroValue) return;
		if(macro.triggerReload) {
		    this.macroChanged();
		    this.reloadData();
		}
		if(!macro.name) return;
		this.settingMacroValue = true;
		var args = {
		    entryId:this.entryId,
		    property: "macroValue",
		    id:macro.name,
		    what:what,
		    value: value
		};
		this.propagateEvent("handleEventPropertyChanged", args);
		this.settingMacroValue = false;
	    };

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
		$("#" + this.getDomId(macro.getId())+"," +
		  "#" + this.getDomId(macro.getId()+"_min")+ "," +
		  "#" + this.getDomId(macro.getId()+"_max")+ "," +
		  "#" + this.getDomId(macro.getId()+"_from")+ "," +
		  "#" + this.getDomId(macro.getId()+"_to")).keyup(function(e) {
		      var keyCode = e.keyCode || e.which;
		      if (keyCode == 13) {
			  macroChange(macro, $(this).val());
		      }
		  });
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
		this.jq(macro.getId()+"_from").change(function(e) {
		    macroChange(macro, $(this).val(),"from");
		});
		this.jq(macro.getId()+"_to").change(function(e) {
		    macroChange(macro, $(this).val(),"to");
		});		

		return true;
	    });
	},
	makeFilterWidget:function(label, widget, title) {
	    if(!label)
		return HU.div([CLASS,"display-filter-widget"],widget);
	    return HU.div([CLASS,"display-filter-widget"],this.makeFilterLabel(label,title)+(label.trim().length==0?" ":": ") +
			  widget);
	},
	makeFilterLabel: function(label,tt) {
	    let attrs = [CLASS,"display-filter-label"];
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
		index =0;
	    } else if(index<0) {
		return;
		index = length-1;
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
		id = id.replace(/_min$/,"").replace(/_max$/,"");
		let min = $("#" + id+"_min");
		let max = $("#" + id+"_max");
		let range = {
		    min: parseFloat(min.attr("data-min")),
		    max: parseFloat(max.attr("data-max"))};
		let minValue = parseFloat(min.val());
		let maxValue = parseFloat(max.val());
		let html = HU.div([ID,"filter-range",STYLE,HU.css("width","200px")],"");
		let popup = getTooltip();
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
		var step = 1;
		if(parseInt(range.max)!=range.max || parseInt(range.min) != range.min) 
		    step = (range.max-range.min)/100000;
		$( "#filter-range" ).slider({
		    range: true,
		    min: range.min,
		    max: range.max,
		    step: step,
		    values: [minValue, maxValue],
		    slide: function( event, ui ) {
			min.val(ui.values[0]);
			max.val(ui.values[1]);
			min.attr("data-value",min.val());
			max.attr("data-value",max.val());
			if(immediate) {
			    inputFunc(min,max);
			}
		    },
		    stop: function() {
			var popup = getTooltip();
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
		    if(filter.field.getId() == fieldId) return this.filters[i];
		}
	    }
	    return null;
	},
        checkSearchBar: function() {
	    let debug = displayDebug.checkSearchBar;
	    if(debug) console.log("checkSearchBar");
            let _this = this;

            this.colorByFields = this.getFieldsByIds(null, this.getProperty("colorByFields", "", true));
            this.sizeByFields = this.getFieldsByIds(null, this.getProperty("sizeByFields", "", true));
            this.sortByFields = this.getFieldsByIds(null, this.getProperty("sortByFields", "", true));	    
	    let pointData = this.getData();
            if (pointData == null) return;
            let fields= pointData.getRecordFields();
            let records = pointData.getRecords();
	    records = this.sortRecords(records);
	    let header2="";
	    //	    header2 +=HU.div([ID,this.getDomId("test")],"test");
	    if(this.getProperty("showProgress",false)) {
		header2 += HU.div([ID,this.getDomId(ID_DISPLAY_PROGRESS), STYLE,HU.css("display","inline-block","margin-right","4px","min-width","20px")]);
	    }
	    header2 += HU.div([ID,this.getDomId(ID_HEADER2_PREPREFIX),CLASS,"display-header-span"],"");
	    header2 += HU.div([ID,this.getDomId(ID_HEADER2_PREFIX),CLASS,"display-header-span"],"");
	    header2 +=  this.getHeader2();
	    if(this.getProperty("pageRequest",false)) {
		header2 += HU.span([ID,this.getDomId(ID_PAGE_COUNT)]);
	    }
	    header2+=HU.div([ID,this.getDomId(ID_REQUEST_PROPERTIES),CLASS,"display-header-span"],"");
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
		header2+= html;

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
		    let html =  HU.span([CLASS,"display-filter"],
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
		    header2 += HU.span([CLASS,"display-filter"],
				       (label==""?"":this.makeFilterLabel(label+": ")) + 
				       HU.select("",[ID,this.getDomId("fieldselect_" + prop)],enums,this.getProperty(prop,"")))+SPACE;

		    selectFieldProps.push(prop);
		});
	    }


	    if(this.colorByFields.length>0) {
		let enums = [];
		this.colorByFields.forEach(field=>{
		    if(field.isFieldGeo()) return;
		    enums.push([field.getId(),field.getLabel()]);
		});
		header2 += HU.span([CLASS,"display-filter"],
				   this.makeFilterLabel("Color by: ") + HU.select("",[ID,this.getDomId("colorbyselect")],enums,this.getProperty("colorBy","")))+SPACE;
	    }
	    if(this.sortByFields.length>0) {
		let enums = [];
		this.sortByFields.forEach(field=>{
		    if(field.isFieldGeo()) return;
		    enums.push([field.getId(),field.getLabel()]);
		});
		header2 += HU.span([CLASS,"display-filter"],
				   this.makeFilterLabel("Sort by: ") + HU.select("",[ID,this.getDomId("sortbyselect")],enums,this.getProperty("sortFields","")))+SPACE;
	    }

	    if(this.sizeByFields.length>0) {
		let enums = [];
		this.sizeByFields.forEach(field=>{
		    enums.push([field.getId(),field.getLabel()]);
		});
		header2 += HU.span([CLASS,"display-filter"],
				   this.makeFilterLabel("Size by: ") + HU.select("",[ID,this.getDomId("sizebyselect")],enums,this.getProperty("sizeBy","")))+SPACE;
	    }
	    let  highlight = this.getFilterHighlight();
	    if(this.getProperty("showFilterHighlight")) {
		let enums =[["filter","Filter"],["highlight","Highlight"]];
		header2 += HU.select("",["fieldId","_highlight", ID,this.getDomId(ID_FILTER_HIGHLIGHT)],enums,!highlight?"filter":"highlight") + SPACE2;
	    }


	    let dataFilterIds = [];
	    this.getDataFilters().forEach(f=>{
		if(!f.label) return;
		let cbxid = this.getDomId("datafilterenabled_" + f.id);
		dataFilterIds.push(cbxid);
		header2 +=  HU.checkbox("",[ID,cbxid],f.enabled) +" " +
		    this.makeFilterLabel(f.label +"&nbsp;&nbsp;")
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

		header2 += HU.span([CLASS,"display-filter",STYLE,style],
				   prefix +
				   HU.select("",["fieldId","filterDate", ATTR_ID,selectId],enums,selected))+SPACE;
	    }
	    

            let filterBy = this.getProperty("filterFields","",true).split(","); 
	    let hideFilterWidget = this.getProperty("hideFilterWidget",false, true);
	    let fieldMap = {};
	    //Have this here so it can be used in the menu change events later. May cause problems if more than  one
	    let displayType = "";
	    this.filters = [];
	    this.addFilters(this.filters);
            if(filterBy.length>0) {
                for(let i=0;i<filterBy.length;i++) {
		    if(filterBy[i]!="") {
			this.filters.push(new RecordFilter(this, filterBy[i]));
		    }
		}
		let searchBar = "";
		let bottom = [""];
		this.filters.forEach(filter=>{
		    let widget = filter.getWidget(fieldMap, bottom,records);
		    searchBar +=widget;
		});
		style = (hideFilterWidget?"display:none;":"") + this.getProperty("filterByStyle","");
		let filterBar = searchBar+bottom[0];
		if(filterBar!="") {
		    header2+=HU.span([CLASS,"display-filter",STYLE,style,ID,this.getDomId(ID_FILTERBAR)],searchBar+bottom);
		}
	    }

	    header2+=HU.div([ID,this.getDomId(ID_HEADER2_SUFFIX),CLASS,"display-header-span"],"");
	    this.jq(ID_HEADER2).html(header2);
	    this.initHeader2();
	    this.jq("test").button().click(()=>{
		this.haveCalledUpdateUI = false;
		this.callUpdateUI();
	    });
	    this.createRequestProperties();
 	    let inputFunc = function(input, input2, value){
		if(this.ignoreFilterChange) return;
                var id = input.attr(ID);
		if(!id) {
		    console.log("No ID attribute");
		    return;
		}
		if(!input2) {
		    if(id.endsWith("_min")) {
			input2 = $("#" + id.replace(/_min$/,"_max"));
		    } else if(id.endsWith("_max")) {
			var tmp = input;
			input =$("#" + id.replace(/_max$/,"_min"));
			input2 = tmp;
		    }
		}
		if(input.attr("isCheckbox")) {
		    var on = input.attr("onValue")||true;
		    var off = input.attr("offValue")||false;
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
		if(!value || value=="") {
		    value = input.attr("data-value");
		}
		if(!value) {
		    value = input.val();
		}
		
		if(value==null) return;
		if(!Array.isArray(value) && input.attr("isButton")) {
		    //			console.log(_this.type +" " +Array.isArray(value));
		    var tmp = [];
		    value.split(",").forEach(v=>{
			tmp.push(v.replace(/_comma_/g,","));
		    });
		    value = tmp;
		}

                var fieldId = input.attr("fieldId");
		_this.checkFilterField(input);
		_this.haveCalledUpdateUI = false;
		if(_this.settingFilterValue) {
		    return;
		}
		_this.settingFilterValue = true;
		_this.dataFilterChanged();

//		console.log("id:" + fieldId +" value:" + value);
		_this.addToDocumentUrl(fieldId+".filterValue",value);
		var args = {
		    property: PROP_FILTER_VALUE,
		    id:id,
		    fieldId: fieldId,
		    value: value
		};
		if(input2) {
		    args.value2 = input2.val();
		}
		_this.propagateEvent("handleEventPropertyChanged", args);
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
		    var isAll = $(this).hasClass("display-filter-item-all");
		    var selectClazz = "display-filter-item-" + displayType +"-selected"
		    var wasSelected = $(this).hasClass(selectClazz);
		    var fieldId = $(this).attr("fieldId");
		    var multiples = _this.getProperty(fieldId +".filterMultiple",false);
		    if(!event.metaKey || isAll || !multiples) {
			parent.find(".display-filter-item").removeClass(selectClazz);
		    } else {
			parent.find(".display-filter-item-all").removeClass(selectClazz);
		    }
		    if(wasSelected  && event.metaKey) {
			$(this).removeClass(selectClazz);
		    } else {
			$(this).addClass(selectClazz);
		    }
		    var values = [];
		    parent.find("." + selectClazz).each(function() {
			values.push($(this).attr("data-value").replace(/,/g,"_comma_"));
		    });
		    if(values.length==0) {
			parent.find(".display-filter-item-all").addClass(selectClazz);
			values.push(FILTER_ALL);
		    }
		    var value =  Utils.join(values,",");
		    parent.attr("data-value", value);
		    $("#"+parent.attr(ID) +"_label").html(values.includes(FILTER_ALL)?SPACE:value);
		    inputFunc(parent,null, values);
		});

	    });
	    this.jq(ID_FILTERBAR).find(".display-filter-input").keyup(function(e) {
		var keyCode = e.keyCode || e.which;
		if (keyCode == 13) {return;}
		hidePopupObject();
		var input = $(this);
		var val = $(this).val().trim();
		if(val=="") return;
                var fieldId = $(this).attr("fieldId");
		var field = fieldMap[fieldId].field;
		var values = fieldMap[fieldId].values;
		var items=[];
		var regexp=null;
		try {
		    val = val.replace(/\./g,"\\.");
		    regexp = new RegExp("(" + val+")",'i');
		} catch(ignore) {
		    //todo
		}
		for(var i=0;i<values.length;i++) {
		    var text= values[i].toString();
		    var match  = regexp?text.match(regexp):text.indexOf(val)>=0;
		    if(match) {
			items.push([match[1], values[i]]);
		    }
		    if(items.length>30) break;
		}
		if(items.length>0) {
		    var html = "";
		    var itemCnt = 0;
		    items.forEach(item=>{
			var match = item[0];
			item =  item[1];
			if(item.length>50) return;
			var label = item.replace(regexp,"<span style='background:" + HIGHLIGHT_COLOR +";'>" + match +"</span>");
			item = item.replace(/\'/g,"\'");
			html+=HU.div([CLASS,"display-filter-popup-item","item",item],label)+"\n";
			itemCnt++;
		    });	
		    if(itemCnt>0) {
			popupObject = getTooltip();
			popupObject.html(HU.div([CLASS, "ramadda-popup-inner ramadda-snippet-popup"], html));
			popupObject.show();
			popupObject.position({
			    of: $(this),
			    my: "left top",
			    at: "left bottom",
			});
			$(".display-filter-popup-item").click(function(){
			    hidePopupObject();
			    input.val($(this).attr("item"));
			    inputFunc(input);
			});
		    }
		}

	    });



	    this.initializeRangeSlider(this.jq(ID_FILTERBAR).find(".display-filter-range"), inputFunc, this.getProperty("filterSliderImmediate"));

	    this.jq(ID_FILTER_HIGHLIGHT).change(function() {
		_this.setProperty("filterHighlight", $(this).val()=="highlight");
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
		_this.propagateEvent("handleEventPropertyChanged", {
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
            this.jq("sortbyselect").change(function(){
		_this.sortByFieldChanged($(this).val());
	    });
            this.jq("sizebyselect").change(function(){
		_this.sizeByFieldChanged($(this).val());
	    });

            this.jq(ID_FILTERBAR).find("input").keyup(function(e){
		var keyCode = e.keyCode || e.which;
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
	    
	    var dateMin = null;
	    var dateMax = null;
	    var dates = [];
	    if(debug) console.log("checkSearchBar-getting filtered data");
	    let filteredRecords  = this.filterData();
	    if(debug) console.log("checkSearchBar-done getting filtered data");
	    filteredRecords.every(record=>{
		if (dateMin == null) {
		    dateMin = record.getDate();
		    dateMax = record.getDate();
		} else {
		    var date = record.getDate();
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

	    if(debug) console.log("checkSearchBar-11");
            if (dateMax) {
		if(debug) console.log("checkSearchBar-getAnimation");
		var animation = this.getAnimation();
		if(animation.getEnabled()) {
		    if(debug) console.log("checkSearchBar-calling animation.init");
		    animation.init(dateMin, dateMax,filteredRecords);
		    if(debug) console.log("checkSearchBar-done calling animation.init");
		    if(!this.minDateObj) {
			if(debug) console.log("checkSearchBar-calling setDateRange");
			if(this.getProperty("animationFilter", true)) {
			    this.setDateRange(animation.begin, animation.end);
			}
			if(debug) console.log("checkSearchBar-done calling setDateRange");
		    }
		}
            }

	    if(debug) console.log("checkSearchBar-done");
        },
	checkFilterField: function(f) {
	    var min = f.attr("data-min");
	    var max = f.attr("data-max");
	    var value = f.val();
	    if(Utils.isDefined(min)) {
		if(value != min) {
		    f.css("background",HIGHLIGHT_COLOR);
		} else {
		    f.css("background","white");
		}
	    } else if(Utils.isDefined(max)) {
		if(value != max) {
		    f.css("background",HIGHLIGHT_COLOR);
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
	macroChanged: function() {
	    this.pageSkip = 0;
	},
	dataFilterChanged: function() {
	    this.callUpdateUI();
	},
	addFieldClickHandler: function(jq, records, addHighlight) {
	    let _this = this;
	    if(records) {
		if(!jq) jq = this.jq(ID_DISPLAY_CONTENTS);
		let func = function() {
		    if(addHighlight) {
			$(this).parent().find(".display-row-highlight").removeClass("display-row-highlight");
			$(this).addClass("display-row-highlight");
		    }
		    let record = records[$(this).attr(RECORD_INDEX)];
		    if(record)
			_this.propagateEventRecordSelection({record:record});
		};
		let children = jq.find("[recordIndex]");
		if(!children.length) children = jq;
		children.click(func);
	    }

	    if(this.getProperty("propagateValueClick",true)) {
		let _this = this;
		if(!jq) jq = this.jq(ID_DISPLAY_CONTENTS);
		jq.find("[field-id]").click(function() {
		    let fieldId = $(this).attr("field-id");
		    let value = $(this).attr("field-value");
		    let args = {
			property: PROP_FILTER_VALUE,
			id:fieldId,
			fieldId: fieldId,
			value: value
		    };
		    _this.propagateEvent("handleEventPropertyChanged", args);
		});
	    }

	},
	//Make sure to set the title attribute on the elements
	makeTooltips: function(selector, records, callback, tooltipArg) {
	    if(!this.getProperty("showTooltips",true)) {
		return;
	    }
	    var tooltip = tooltipArg || this.getProperty("tooltip");
	    if(!tooltip) return;
	    let _this = this;
	    selector.tooltip({
		content: function() {
		    var record = records[parseFloat($(this).attr('recordIndex'))];
		    if(callback) callback(true, record);
		    _this.getDisplayManager().notifyEvent("handleEventRecordHighlight", _this, {highlight:true,record: record});
		    let style = _this.getProperty("tooltipStyle");
		    let tt =  _this.getRecordHtml(record,null,tooltip);
		    if(style) tt=HU.div([STYLE,style],tt);
		    return tt;
		},
		close: function(event,ui) {
		    var record = records[parseFloat($(this).attr('recordIndex'))];
		    if(callback) callback(true, record);
		    _this.getDisplayManager().notifyEvent("handleEventRecordHighlight", _this, {highlight:false,record: record});
		},
		position: {
		    my: _this.getProperty("tooltipPositionMy", "left top"),
		    at: _this.getProperty("tooltipPositionAt", "left bottom+2"),
		    collision: _this.getProperty("tooltipCollision", "none none")
		},
		show: {
		    delay: parseFloat(_this.getProperty("tooltipDelay",1000)),
		    duration: parseFloat(_this.getProperty("tooltipDuration",500)),
	    
		},
		classes: {
		    "ui-tooltip": _this.getProperty("tooltipClass", "display-tooltip")
		}
	    });
	},
	makePopups: function(selector, records, callback, popupTemplate) {
	    if(!popupTemplate)
		popupTemplate = this.getProperty("popupTemplate");
	    if(!popupTemplate) return;
	    let _this = this;
	    selector.click(function(event){
		var record = records[parseFloat($(this).attr('recordIndex'))];
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
	    hidePopupObject();
	    var html =  _this.getRecordHtml(record,null,popupTemplate);
	    html = HU.div([CLASS, "display-popup " + _this.getProperty("popupClass",""),STYLE, _this.getProperty("popupStyle","")],html);
	    popupObject = getTooltip();
	    popupObject.html(html);
	    popupObject.show();
	    popupObject.position({
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
		//		var t1 = new Date();
		this.dataFilterChanged({source:"animation"});
		//		var t2 = new Date();
		//		Utils.displayTimes("timeChanged",[t1,t2]);
	    }
	    this.propagateEvent("handleEventPropertyChanged", {
		property: "dateRange",
		minDate: animation.begin,
		maxDate: animation.end
	    });
	},
        makeDialog: function(text) {
            var html = "";
            html += HU.div([ATTR_ID, this.getDomId(ID_HEADER), ATTR_CLASS, "display-header"]);
            var closeImage = HU.getIconImage(icon_close, []);
            var close = HU.onClick("$('#" + this.getDomId(ID_DIALOG) + "').hide();", closeImage);
            var right = close;
            var left = "";
            //                var left = this.makeToolbar({addLabel:true});
            var header = HU.div([ATTR_CLASS, "display-dialog-header"], HU.leftRight(left, right));
	    if(!text) {
		var tabTitles = [];
		var tabContents = [];
		this.getDialogContents(tabTitles, tabContents);
		tabTitles.push("Edit");
		tabContents.push(this.makeToolbar({
                    addLabel: true
		}));
		var tabLinks = "<ul>";
		var tabs = "";
		for (var i = 0; i < tabTitles.length; i++) {
                    var id = this.getDomId("tabs") + i;
                    tabLinks += HU.tag("li", [], HU.tag("a", ["href", "#" + id],
							tabTitles[i]));
                    tabLinks += "\n";
                    var contents = HU.div([ATTR_CLASS, "display-dialog-tab"], tabContents[i]);
                    tabs += HU.div([ID, id], contents);
                    tabs += "\n";
		}
		tabLinks += "</ul>\n";
		text =  HU.div([ID, this.getDomId(ID_DIALOG_TABS)], tabLinks + tabs);
	    }
	    return  header + text;
        },
        initDialog: function() {
            var _this = this;
            var updateFunc = function(e) {
                if (e && e.which != 13 && e.which!=0) {
                    return;
                }
		var changed = false;
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
            var text = request.term;
            if (text == null || text.length <= 1) return;
            this.doingQuickEntrySearch = true;
            var searchSettings = new EntrySearchSettings({
                name: text,
                max: 10,
            });
            if (this.searchSettings) {
                searchSettings.clearAndAddType(this.searchSettings.entryType);
            }
	    let _this = this;
            var jsonUrl = this.getRamadda().getSearchUrl(searchSettings, OUTPUT_JSON);
            var handler = {
                entryListChanged: function(entryList) {
                    _this.doneQuickEntrySearch(entryList, callback);
                }
            };
            var entryList = new EntryList(this.getRamadda(), jsonUrl, handler, true);
        },
        doneQuickEntrySearch: function(entryList, callback) {
            var names = [];
            var entries = entryList.getEntries();
            for (var i = 0; i < entries.length; i++) {
                names.push(entries[i].getName());
            }
            callback(names);
            this.doingQuickEntrySearch = false;

        },
        addData: async function(pointData) {
            var records = pointData.getRecords();
            if (records && records.length > 0) {
                this.hasElevation = records[0].hasElevation();
            } else {
                this.hasElevation = false;
            }
	    pointData = this.convertPointData(pointData);
            this.dataCollection.addData (pointData);
            var entry = pointData.entry;
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
	setErrorMessage: function(msg) {
            this.setContents(this.getMessage(msg));
	},
	clearProgress: function() {
	    this.jq(ID_DISPLAY_PROGRESS).html("");
	},
	startProgress: function() {
	    if(this.jq(ID_DISPLAY_PROGRESS).length>0) 
		this.jq(ID_DISPLAY_PROGRESS).html(HU.image(icon_progress));
	    else
		this.setContents(this.getLoadingMessage());
	},
	handleNoData: function(pointData,reload) {
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
            this.setContents(this.getMessage(this.getNoDataMessage()));
	},
        pointDataLoadFailed: function(data) {
	    this.clearProgress();
            this.inError = true;
            errorMessage = this.getProperty("errorMessage", null);
            if (errorMessage != null) {
                this.setContents(errorMessage);
                return;
            }
            var msg = "";
            if (data && data.errorcode && data.errorcode == "warning") {
                msg = data.error;
            } else {
                msg = "<b>An error has occurred:</b>";
                if (!data) data = this.getNoDataMessage();
                var error = data.error ? data.error : data;
                error = error.replace(/<[^>]*>/g, "");
                var tmp = "";
                var lines = error.split("\n");
                var seen = {};
                for (var i = 0; i < lines.length; i++) {
                    var line = lines[i].trim();
                    if (line == "") continue;
                    if (seen[line]) continue;
                    seen[line] = true;
                    tmp += line + "\n";
                }
                error = tmp;
                error = HU.tag("pre", [STYLE, HU.css("max-height","300px","overflow-y","auto","max-width","100%","overflow-x","auto")], error);
                msg += error;
            }
	    this.setErrorMessage(msg);
        },
        //callback from the pointData.loadData call
        clearCache: function() {},
        pointDataLoaded: function(pointData, url, reload) {
//	    console.log(this.type +".pointDataLoaded");
	    let debug = displayDebug.pointDataLoaded;
	    this.clearProgress();
            this.inError = false;
            this.clearCache();
	    if(debug) console.log(this.type+" pointDataLoad:" + this.getId() + " " + this.type +" #records:" + pointData.getRecords().length);
	    if(debug)
		console.log("\tclearing last selected fields");
	    
	    this.lastSelectedFields = null;
	    this.allFields = null;
            if (!reload) {
		if(debug) console.log("\tcalling addData");
                this.addData(pointData);
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

	    if(this.getProperty("pageRequest")) {
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

	    if(!this.getProperty("dateFormat")) {
                pointData.getRecordFields().forEach(f=>{
		    if(f.isFieldDate() && f.getId() == "year") {
			this.setProperty("dateFormat","yyyy");
		    }
		});
	    }
	    
	    this.haveCalledUpdateUI = false;
	    if(debug) console.log("\tcalling updateUI");
            this.updateUI({reload:reload});
            if (!reload) {
                this.lastPointData = pointData;
                this.propagateEvent("handleEventPointDataLoaded", pointData);
            }
        },
        getDateFormatter: function() {
            var date_formatter = null;
            if (this.isGoogleLoaded()) {
                var df = this.getProperty("dateFormat", null);
                if (df) {
                    var tz = 0;
                    this.timezone = this.getProperty("timezone");
                    if (Utils.isDefined(this.timezone)) {
                        tz = parseFloat(this.timezone);
                    }
                    date_formatter = new google.visualization.DateFormat({
                        pattern: df,
                        timeZone: tz
                    });
                }
            }
            return date_formatter;
        },
        getHasDate: function(records) {
            var lastDate = null;
            this.hasDate = false;
            for (j = 0; j < records.length; j++) {
                var record = records[j];
                var date = record.getDate();
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
        dateInRange: function(date, debug) {
	    if(debug) {
		console.log("dateInRange: date:" + date +" minDate:" + this.minDateObj +" maxDate:" + this.maxDateObj);
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
			    console.log("    minDate:\n\t" + date.getTime() +"\n\t" + this.minDateObj.getTime());
			}
			return false;
                    }
                    if (this.maxDateObj != null && date.getTime() > this.maxDateObj.getTime()) {
			if(debug) {
			    console.log("    maxDate:\n\t" + date.getTime() +"\n\t" + this.minDateObj.getTime());
			}
			return false;
		    }
                }
		
		if (this.startDateObject != null && date.getTime() < this.startDateObject.getTime()) {
		    if(debug) {
			console.log("    startDate:\n\t" + date.getTime() +"\n\t" + this.startDateObject.getTime());
		    }
                    return false;
                }
                if (this.endDateObject != null && date.getTime() > this.endDateObject.getTime()) {
		    if(debug) {
			console.log("    endDate:\n\t" + date.getTime() +"\n\t" + this.endDateObject.getTime());
		    }
                    return false;
                }
            }
            return true;
        },
        getPointData: function() {
            if (this.dataCollection.getList().length == 0) return null;
            return this.dataCollection.getList()[0];
        },
        //get an array of arrays of data 
        getDataValues: function(obj) {
            if (obj.tuple) return obj.tuple;
            else if (obj.getData) return obj.getData();
            return obj;
        },
	indexToRecord: {},
	recordToIndex: {},
	findMatchingIndex: function(record) {
	    if(!record) return -1;
	    var index = this.recordToIndex[record.getId()];
	    if(Utils.isDefined(index)) {
		return index;
	    }
	    if(!record.hasDate()) return -1;
	    var closest;
	    var min  =0;
	    for(i in this.indexToRecord) {
		var r = this.indexToRecord[i];
		if(!r.hasDate()) return -1;
		var diff = Math.abs(record.getDate().getTime()-r.getDate().getTime());
		if(!closest) {
		    min = diff;
		    closest = r;
		} else {
		    if(diff<min) {
			min = diff;
			closest = r;
		    }
		}
	    }
	    if(!closest) 
		return -1;
	    return this.recordToIndex[closest.getId()];
	},
        makeDataArray: function(dataList) {
            if (dataList.length == 0) return dataList;
            var data = [];
            if (dataList[0].getData) {
                for (var i = 0; i < dataList.length; i++) {
		    data.push(dataList[i].getData()[0]);
		}
            } else if (dataList[0].tuple) {
                for (var i = 0; i < dataList.length; i++) {
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
	    let debug = displayDebug.getStandardData;
	    if(debug) console.log("getStandardData:" + this.type +"  fields:" + fields);
	    let showUnit  = this.getProperty("showUnit",true);
	    this.recordToIndex = {};
	    this.indexToRecord = {};
            var pointData = this.getPointData();
            var excludeZero = this.getProperty(PROP_EXCLUDE_ZERO, false);
            var excludeNan = this.getProperty(PROP_EXCLUDE_NAN, false);	    
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

            var dataList = [];
            //The first entry in the dataList is the array of names
            //The first field is the domain, e.g., time or index
            var fieldNames = [];
            var fieldsForTuple = [];	    
	    if(this.getProperty("binDate")) {
		if(debug)
		    console.log("binning date");
		var binType = this.getProperty("binType","total");
		var binCount = binType=="count";
		if(binCount) {
		    var f = [];
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
                var field = fields[i];
                if (field.isFieldNumeric() && field.isFieldDate()) {
                    //                        console.log("Skipping:" + field.getLabel());
                    //                        continue;
                }
                var name = field.getLabel();
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



            var offset = 0;
            if (Utils.isDefined(this.offset)) {
                offset = parseFloat(this.offset);
            }

            var nonNullRecords = 0;
            var records = this.filterData();
	    if(debug)
		console.log("getStandardData #fields:" + fields.length +" #records:" + records.length);
            var allFields = pointData.getRecordFields();

            //Check if there are dates and if they are different
            this.hasDate = this.getHasDate(records);
            var date_formatter = this.getDateFormatter();
            var rowCnt = -1;
            var indexField = this.getFieldById(null,this.getProperty("indexField"));
            for (var rowIdx = 0; rowIdx < records.length; rowIdx++) {
                var record = records[rowIdx];
                var date = record.getDate();
		
                if (!this.dateInRange(date)) {
		    continue;
		}
                rowCnt++;
		this.recordToIndex[record.getId()] = rowCnt;
		this.indexToRecord[rowCnt] = record;
                let values = [];
                if (props && (props.includeIndex || props.includeIndexIfDate)) {
                    var indexName = null;
                    if (indexField) {
			var value = this.makeIndexValue(indexField,record.getValue(indexField.getIndex()),rowIdx);
                        values.push(value);
                        indexName = indexField.getLabel();
                    } else {
                        if (this.hasDate) {
                            //                                console.log(this.getDateValue(date, date_formatter));
                            values.push(this.getDateValue(date, date_formatter));
                            indexName = "Date";
                        } else {
                            if (!props.includeIndexIfDate) {
                                values.push(rowIdx);
				indexName = this.getProperty("indexName", "Index");
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
                    var value = record.getValue(field.getIndex());
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
		    var value = record.getValue(groupByIndex);
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

            if (nonNullRecords == 0) {
		//		console.log("Num non null:" + nonNullRecords);
		console.log("no nonNull records");
		return [];
            }

            if (groupByIndex >= 0) {
                var groupToTuple = {};
                var groups = [];
                var agg = [];
                var title = [];
		let groupByCount = this.getProperty("groupByCount");
                title.push(props.groupByField.getLabel());
		if(groupByCount) {
		    title.push(this.getProperty("groupByCountLabel", "Count"));
		} else {
                    for (var fieldIdx = 0; fieldIdx < fields.length; fieldIdx++) {
			var field = fields[fieldIdx];
			if (field.getIndex() != groupByIndex) {
                            title.push(field.getLabel());
			}
                    }
		}
		//                agg.push(title);
		let groupByValueTuples = {};
                for (var rowIdx = 0; rowIdx < dataList.length; rowIdx++) {
                    var data = this.getDataValues(dataList[rowIdx]);
                    if (rowIdx == 0) {
                        continue;
                    }
                    var groupBy = groupByList[rowIdx];
		    var record = groupByRecords[rowIdx];
		    var groupByValue = record.getValue(groupByIndex);
                    var tuple = groupToTuple[groupBy];
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
                            for (var fieldIdx = 0; fieldIdx < fields.length; fieldIdx++) {
				var field = fields[fieldIdx];
				if (field.getIndex() == groupByIndex) {
                                    continue;
				}
				tuple.push(0);
                            }
			}
                        groupToTuple[groupBy] = tuple;
                    }
                    var index = 0;
		    if(groupByCount) {
			tuple[1]++;
			continue;
		    }
                    for (var fieldIdx = 0; fieldIdx < fields.length; fieldIdx++) {
                        var field = fields[fieldIdx];
                        if (field.getIndex() == groupByIndex) {
                            continue;
                        }
                        var dataValue = data[fieldIdx];
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
                            var s = tuple[index];
                            if (!Utils.isDefined(s)) {
                                s = "";
                            }
                            //Only concat string values for a bit
                            if (s.length < 150) {
                                if (!Utils.isDefined(dataValue)) {
                                    dataValue = "";
                                }
                                var sv = ("" + dataValue);
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
            var tz = 0;
            this.timezone = this.getProperty("timezone");
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
        getDateValue: function(arg, formatter) {
            if (!this.initDateFormats()) {
                return arg;
            }
            if (!(typeof arg == "object")) {
                date = new Date(arg);
            } else {
                date = arg;
            }
	    if(isNaN(date.getUTCFullYear())) return {v:date,f:"NA"};
	    if(this.getProperty("dateFormatDaysAgo",false)) {
		let now = new Date();
		let diff = Math.round((now.getTime()-date.getTime())/1000/60/60/24);
		return {v:date,f:diff+" days ago"};
	    }
            if (!formatter) {
                formatter = this.fmt_yyyymmddhhmm;
            }

            var s = formatter.formatValue(date);
            date = {
                v: date,
                f: s
            };
            return date;
        },
        applyFilters: function(record, values) {
            for (var i = 0; i < this.filters.length; i++) {
                if (!this.filters[i].isRecordOk(record)) {
                    return false;
                }
            }
            return true;
        }
    });

    var filter = this.getProperty(PROP_DISPLAY_FILTER);
    if (filter != null) {
        //semi-colon delimited list of filter definitions
        //display.filter="filtertype:params;filtertype:params;
        //display.filter="month:0-11;
        var filters = filter.split(";");
        for (var i = 0; i < filters.length; i++) {
            filter = filters[i];
            var toks = filter.split(":");
            var type = toks[0];
            if (type == "month") {
                this.filters.push(new MonthFilter(toks[1]));
            } else {
                console.log("unknown filter:" + type);
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
    RamaddaUtil.defineMembers(this, {
        displays: [],
        layout: this.getProperty(PROP_LAYOUT_TYPE, LAYOUT_TABLE),
        columns: this.getProperty(PROP_LAYOUT_COLUMNS, 1),
        isLayoutColumns: function() {
            return this.layout == LAYOUT_COLUMNS;
        },
        getWikiText: function() {
            var attrs = ["layoutType", this.layout,
			 "layoutColumns",
			 this.columns,
			 "showMenu",
			 "false",
			 "divid",
			 "$entryid_maindiv"
			];
            let wiki = "";
            wiki += "<div id=\"{{entryid}}_maindiv\"></div>\n";
            wiki += "{{group " + HU.attrs(attrs) + "}}\n"
            return wiki;
        },

        walkTree: function(func, data) {
            for (var i = 0; i < this.displays.length; i++) {
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
        notifyEvent: function(func, source, data) {
            let displays = this.getDisplays();
            for (let i = 0; i < this.displays.length; i++) {
                let display = this.displays[i];
                if (display == source) {
                    continue;
                }
		if(!display.getProperty("accept." + func, true)) continue;
                let eventSource = display.getEventSource();
                if (eventSource != null && eventSource.length > 0) {
                    if (eventSource != source.getId() && eventSource != source.getName()) {
                        continue;
                    }
                }
                display.notifyEvent(func, source, data);
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
            let index = this.displays.indexOf(display);
            if (index >= 0) {
                this.displays.splice(index, 1);
            }
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
		    html+=HU.open(TABLE,[WIDTH,'100%']);
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
                        html += HU.td(["width",percent], displaysToLayout[i].layoutDiv);
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
            this.writeHtml(ID_DISPLAYS, html);
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
                    display.displayError("Error creating display:<br>" + e);
                    console.log("error creating display: " + display.type);
                    console.log(e.stack)
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
    RamaddaUtil.inherit(this, this.RamaddaDisplay = SUPER);
    RamaddaUtil.defineMembers(this, {
        needsData: function() {
            return true;
        },
        initDisplay: function() {
            SUPER.initDisplay.call(this);
            if (this.needsData()) {
                this.setContents(this.getLoadingMessage());
            }
            this.callUpdateUI();
        },
        updateUI: function() {
            this.addFieldsCheckboxes();
        },
        //This keeps checking the width of the chart element if its zero
        //we do this for displaying in tabs
        checkLayout: function() {
            var d = this.jq(ID_DISPLAY_CONTENTS);
            if (this.lastWidth != d.width()) {
                this.displayData();
            }
        },
        getWikiAttributes: function(attrs) {
            SUPER.getWikiAttributes.call(this, attrs);
            if (this.lastSelectedFields) {
                attrs.push(PROP_FIELDS);
                var v = "";
                for (var i = 0; i < this.lastSelectedFields.length; i++) {
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
            var height = "600";
            var html = HU.div([ATTR_ID, this.getDomId(ID_FIELDS), STYLE, HU.css("overflow-y","auto","max-height", height + "px")], " FIELDS ");
            tabTitles.push("Fields");
            tabContents.push(html);
            SUPER.getDialogContents.call(this, tabTitles, tabContents);
        },
        handleEventFieldsSelected: function(source, fields) {
	    if(fields.length>0 && (typeof fields[0] =="string")) {
		var tmp = [];
		fields.forEach(f=>{
		    f = this.getFieldById(null, f);
		    if(f) tmp.push(f);
		});
		fields=tmp;
	    }
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



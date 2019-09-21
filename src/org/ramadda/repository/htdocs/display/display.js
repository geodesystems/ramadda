/**
   Copyright 2008-2019 Geode Systems LLC
*/


var displayDebug = false;

//Ids of DOM components
var ID_BOTTOM = "bottom";
var ID_COLORTABLE = "colortable";
var ID_FIELDS = "fields";
var ID_HEADER = "header";
var ID_HEADER1 = "header1";
var ID_HEADER2 = "header2";
var ID_HEADER3 = "header3";
var ID_FILTERBAR = "filterbar";
var ID_TITLE = ATTR_TITLE;
var ID_TITLE_EDIT = "title_edit";
var ID_TOP_RIGHT = "topright";
var ID_TOP_LEFT = "topleft";
var ID_DETAILS = "details";
var ID_DISPLAY_CONTENTS = "contents";
var ID_DISPLAY_TOP = "top";
var ID_DISPLAY_BOTTOM = "bottom";
var ID_GROUP_CONTENTS = "group_contents";
var ID_DETAILS_MAIN = "detailsmain";
var ID_GROUPBY_FIELDS= "groupdbyfields";
var ID_TOOLBAR = "toolbar";
var ID_TOOLBAR_INNER = "toolbarinner";
var ID_LIST = "list";
var ID_DIALOG = "dialog";
var ID_DIALOG_TABS = "dialog_tabs";
var ID_DIALOG_BUTTON = "dialog_button";
var ID_FOOTER = "footer";
var ID_FOOTER_LEFT = "footer_left";
var ID_FOOTER_RIGHT = "footer_right";
var ID_MENU_BUTTON = "menu_button";
var ID_MENU_OUTER = "menu_outer";
var ID_MENU_INNER = "menu_inner";
var ID_REPOSITORY = "repository";

var PROP_DISPLAY_FILTER = "displayFilter";
var PROP_EXCLUDE_ZERO = "excludeZero";
var PROP_DIVID = "divid";
var PROP_FIELDS = "fields";
var PROP_LAYOUT_HERE = "layoutHere";
var PROP_HEIGHT = "height";
var PROP_WIDTH = "width";
var FILTER_ALL = "-all-";
var HIGHLIGHT_COLOR = "yellow";

function initRamaddaDisplays() {
    ramaddaCheckForResize();
    if (window.globalDisplaysList == null) {
        return;
    }
    for (var i = 0; i < window.globalDisplaysList.length; i++) {
        window.globalDisplaysList[i].pageHasLoaded();
    }
}

function addGlobalDisplayProperty(name, value) {
    if (window.globalDisplayProperties == null) {
        window.globalDisplayProperties = {};
    }
    window.globalDisplayProperties[name] = value;
}


function getGlobalDisplayProperty(name) {
    if (window.globalDisplayProperties == null) {
        return null;
    }
    return window.globalDisplayProperties[name];
}


function addRamaddaDisplay(display) {
    if (window.globalDisplays == null) {
        window.globalDisplays = {};
        window.globalDisplaysList = [];
    }
    window.globalDisplaysList.push(display);
    window.globalDisplays[display.getId()] = display;
    if (display.displayId) {
        window.globalDisplays[display.displayId] = display;
    }
}

function ramaddaDisplayCheckLayout() {
    for (var i = 0; i < window.globalDisplaysList.length; i++) {
        if (window.globalDisplaysList[i].checkLayout) {
            window.globalDisplaysList[i].checkLayout();
        }
    }


}



function ramaddaCheckForResize() {
    var redisplayPending = false;
    var redisplayPendingCnt = 0;
    //A hack to redraw the chart after the window is resized
    $(window).resize(function() {
        if (window.globalDisplaysList == null) {
            return;
        }
        //This handles multiple resize events but keeps only having one timeout pending at a time
        if (redisplayPending) {
            redisplayPendingCnt++;
            return;
        }
        var timeoutFunc = function(myCnt) {
            if (myCnt == redisplayPendingCnt) {
                redisplayPending = false;
                redisplayPendingCnt = 0;
                for (var i = 0; i < window.globalDisplaysList.length; i++) {
                    var display = window.globalDisplaysList[i];
                    if (display.displayData)
                        display.displayData();
                }
            } else {
                //Had a resize event during the previous timeout
                setTimeout(timeoutFunc.bind(null, redisplayPendingCnt), 1000);
            }
        }
        redisplayPending = true;
        setTimeout(timeoutFunc.bind(null, redisplayPendingCnt), 1000);
    });
}




function getRamaddaDisplay(id) {
    if (window.globalDisplays == null) {
        return null;
    }
    return window.globalDisplays[id];
}


function removeRamaddaDisplay(id) {
    var display = getRamaddaDisplay(id);
    if (display) {
        display.removeDisplay();
    }
}


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
        var map = argProperties;
        var topMap = map;
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
    this.displayId = null;
    $.extend(this, argProperties);

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
        getTimeZone: function() {
            return this.getProperty("timeZone");
        },
        formatDate: function(date, args) {
            try {
                return this.formatDateInner(date, args);
            } catch (e) {
                console.log("Error formatting date:" + e);
                if (!date.getTime && date.v) date = date.v;
                return "" + date;
            }
        },
        formatDateInner: function(date, args) {

            //Check for date object from charts
            if (!date.getTime && date.v) date = date.v;
            if (!date.toLocaleDateString) {
                return "" + date;
            }
            if (!args) args = {};
            var suffix;
            if (!Utils.isDefined(args.suffix))
                suffix = args.suffix;
            else
                suffix = this.getProperty("dateSuffix");
            var timeZone = this.getTimeZone();
            if (!suffix && timeZone) suffix = timeZone;
            return Utils.formatDate(date, args.options, {
                timeZone: timeZone,
                suffix: suffix
            });
        },
        getUniqueId: function(base) {
            return HtmlUtils.getUniqueId(base);
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
	xcnt:0,	
	applyRecordTemplate: function(row, fields, s, props) {
	    if(!props) {
		props = this.getTemplateProps(fields);
	    }
	    if(props.iconMap && props.iconField) {
		var value = row[props.iconField.getIndex()];
		var icon = props.iconMap[value];
		if(icon) {
		    s = s.replace("${" + props.iconField.getId() +"_icon}", HtmlUtils.image(icon,["width",props.iconSize]));
		}
	    }
	    for (var col = 0; col < fields.length; col++) {
		var f = fields[col];
		var value = row[f.getIndex()];
		if(props.iconMap) {
		    var icon = props.iconMap[f.getId()+"."+value];
		    if(icon) {
			s = s.replace("${" + f.getId() +"_icon}", HtmlUtils.image(icon,["size",props.iconSize]));
		    }
		}
		if(f.getType()=="image") {
		    if(value && value.trim().length>1) {
			var attrs = [];
			if(this.getProperty("imageWidth","")!="") {
			    attrs.push("width");
			    attrs.push(this.getProperty("imageWidth","")) ;
			}
			var img =  HtmlUtils.image(value, attrs);
			s = s.replace("${" + f.getId() +"_image}", img);
			s = s.replace("${" + f.getId() +"_url}", value);
		    } else {
			s = s.replace("${" + f.getId() +"_url}", ramaddaBaseUrl+"/icons/blank.gif");
			s = s.replace("${" + f.getId() +"_image}", "");
		    }
		} else if(f.getType()=="url") {
		    if(value && value.trim().length>1) {
			value = HtmlUtils.href(value,value);
		    }
		} else if(f.isDate) {
		    if(value) {
			s = s.replace("${" + f.getId() +"}", value);
			s = s.replace("${" + f.getId() +"_yyyy}", Utils.formatDateYYYY(value));
			s = s.replace("${" + f.getId() +"_yyyymmdd}", Utils.formatDateYYYYMMDD(value));
			s = s.replace("${" + f.getId() +"_monthdayyear}", Utils.formatDateMonthDayYear(value));
			s = s.replace("${" + f.getId() +"_monthday}", Utils.formatDateMonthDay(value));
			s = s.replace("${" + f.getId() +"_mdy}", Utils.formatDateMDY(value));
		    }
		    continue;
		}
		if(typeof value == "number") {
		    value = Utils.formatNumber(value);
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
		    s = s.replace("${" + f.getId()+"_color}", color);
//		    value = HtmlUtils.span(["style","color:" + color],value);
		}
		
		s = s.replace(new RegExp("\\${" + f.getId() +"}","g"), value);
		if(f.isNumeric()) {
		    s = s.replace(new RegExp("\\${" + f.getId() +"_format}","g"),Utils.formatNumberComma(value));
		}
	    }
	    return s;
	},
        getRecordHtml: function(record, fields, template) {
            if (!fields) {
                var pointData = this.getData();
                if (pointData == null) return null;
                fields = pointData.getRecordFields();
            }
	    
            var showGeo = false;
            if (Utils.isDefined(this.showGeo)) {
                showGeo = ("" + this.showGeo) == "true";
            }
	    if(!template)
		template = this.getProperty("recordTemplate");
	    if(template) {
		if(template!="${default}") {
		    var row = this.getDataValues(record);
		    return this.applyRecordTemplate(row, fields, template);
		}
	    }
            var values = "<table class=formtable>";
            if (false && record.hasLocation()) {
                var latitude = record.getLatitude();
                var longitude = record.getLongitude();
                values += "<tr><td align=right><b>Latitude:</b></td><td>" + number_format(latitude, 4, '.', '') + "</td></tr>";
                values += "<tr><td align=right><b>Longitude:</b></td><td>" + number_format(longitude, 4, '.', '') + "</td></tr>";
            }
            for (var doDerived = 0; doDerived < 2; doDerived++) {
                for (var i = 0; i < record.getData().length; i++) {
                    var field = fields[i];
                    if (doDerived == 0 && !field.derived) continue;
                    else if (doDerived == 1 && field.derived) continue;
                    var label = field.getLabel();
                    label = this.formatRecordLabel(label);
                    if (!showGeo) {
                        if (field.isFieldGeo()) {
                            continue;
                        }
                    }
                    var value = record.getValue(i);
                    if (typeof value == "number") {
                        var sv = value + "";
                        //total hack to decimals format numbers
                        if (sv.indexOf('.') >= 0) {
                            var decimals = 1;
                            //?
                            if (Math.abs(value) < 1.5) decimals = 3;
                            value = number_format(value, decimals, '.', '');
                        }
                    } 
		    if(field.getType() == "image" && value!="") {
			value = HtmlUtils.image(value,["width","200"]);
		    }

		    if(field.getType() == "url") {
			value = HtmlUtils.href(value,value);
		    }
                    values += "<tr><td align=right><b>" + label + ":</b></td><td>" + value + field.getUnitSuffix() + "</td></tr>";
                }
            }
            if (record.hasElevation()) {
                values += "<tr><td  align=right><b>Elevation:</b></td><td>" + number_format(record.getElevation(), 4, '.', '') + "</td></tr>";
            }
            values += "</table>";
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
            this[key] = value;
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
        formatNumber: function(number) {
            if (!this.getProperty("format", true)) return number;
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
            prop = "map-" + prop;
            if (Utils.isDefined(source[prop])) {
                return source[prop];
            }
	    if(source.getProperty)
		return source.getProperty(prop, dflt);
	    return null;
        },


        getProperty: function(key, dflt,skipThis) {
            if(!skipThis && this[key]) {
                return this[key];
            }
            var value = this.properties[key];
            if (value != null) {
                return value;
            }
            if (this.displayParent != null) {
                return this.displayParent.getProperty(key, dflt, skipThis);
            }
            if (this.getDisplayManager) {
                return this.getDisplayManager().getProperty(key, dflt);
            }
            value = getGlobalDisplayProperty(key);
            if (value) {
		return value;
	    }
            return dflt;
        },
    });
}





function RamaddaDisplay(argDisplayManager, argId, argType, argProperties) {

    RamaddaUtil.initMembers(this, {
    });

    var SUPER;
    RamaddaUtil.inherit(this, SUPER = new DisplayThing(argId, argProperties));
    this.getSuper = function() {
        return SUPER;
    }

    if (this.derived) {
        //        console.log("derived:" + this.derived);
        if (this.derived.indexOf("[") == 0) {
            this.derived = JSON.parse(this.derived);
        } else {
            this.derived = [JSON.parse(this.derived)];
        }
        //Init the derived
        for (var i = 0; i < this.derived.length; i++) {
            var d = this.derived[i];
            if (!Utils.isDefined(d.isRow) && !Utils.isDefined(d.isColumn)) {
                d.isRow = true;
                d.isColumn = false;
            }
            if (!d.isRow && !d.isColumn) {
                d.isRow = true;
            }
        }
    } else {
        /*
          this.derived = [
          {"name":"temp_f",
          "label":"Temp F", 
          "function":"temp_c*9/5+32",
          "decimals":2},
          //                        {"name":"sum","function":"$2+$1"}
          ]
        */
    }

    RamaddaUtil.defineMembers(this, {
        displayReady: Utils.getPageLoaded(),
        type: argType,
        displayManager: argDisplayManager,
        filters: [],
        dataCollection: new DataCollection(),
        selectedCbx: [],
        entries: [],
        wikiAttrs: ["title", "showTitle", "showDetails", "minDate", "maxDate"],

        getDisplayManager: function() {
            return this.displayManager;
        },
        getLayoutManager: function() {
            return this.getDisplayManager().getLayoutManager();
        },
	getAnimation: function() {
	    if(!this.animation) {
		this.animation = new DisplayAnimation(this);
	    }
	    return this.animation;
	},

        propagateEvent: function(func, data) {
            var dm = this.getDisplayManager();
            dm[func](this, data);
        },
        displayError: function(msg) {
            this.displayHtml(HtmlUtils.getErrorDialog(msg));
        },
        clearHtml: function() {
            this.displayHtml("");
        },
        displayHtml: function(html) {
            this.jq(ID_DISPLAY_CONTENTS).html(html);
        },
        notifyEvent: function(func, source, data) {
//	    console.log(this.type +".notifyEvent:" + func);
            if (this[func] == null) {
                return;
            }
            this[func].apply(this, [source, data]);
        },
        displayColorTable: function(ct, domId, min, max, args) {
            Utils.displayColorTable(ct, this.getDomId(domId), min, max, args);
        },
        getColorTableName: function(name) {
            var ct;
            if (name) {
                ct = this.getProperty(name);
            } else {
                ct = this.getProperty("colorBar", this.getProperty("colorTable"));
            }
            if (ct == "none") return null;
            return ct;
        },
	addAlpha: function(colors) {
	    var alpha = this.getProperty("colorTableAlpha");
	    if(!alpha) return colors;
	    colors=  Utils.cloneList(colors);
	    var ac = [];
	    colors.map((c)=>{
		if(c.indexOf("#")==0) {
		    var rgb = Utils.hexToRgb(c);
		    if(rgb) {
			c = "rgba(" + rgb.r+"," + rgb.g +"," + rgb.b+"," + alpha+")";
		    }
		    ac.push(c);
		    return;
		}
		c = c.replace(/rgb *\((.*),(.*),(.*)\)/,"rgba($1,$2,$3,_alpha_)");
		c = c.replace("_alpha_",alpha);
		ac.push(c);
	    });
	    return ac;
        },

        getColorTable: function(justColors, name, dflt) {
            var colorTable = this.getColorTableName(name);
            if (!colorTable) {
                colorTable = dflt;
            }
	    var list;
            if (colorTable) {
                var ct = Utils.ColorTables[colorTable];
                if (ct && justColors) return this.addAlpha(ct.colors);
                if (!ct && name) {
                    return this.addAlpha(colorTable.split(","));
                }
                return ct;
            }
            if (this.getProperty("colors")) {
                var colors = this.getProperty("colors");
                if ((typeof colors) == "object") return this.addAlpha(colors);
                return this.addAlpha(colors.split(","));
            }
            return null;
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
	getColorByInfo: function(records) {
            var pointData = this.getData();
            if (pointData == null) return null;
            var fields = pointData.getRecordFields();
            var colorByAttr = this.getProperty("colorBy", null);
            var excludeZero = this.getProperty(PROP_EXCLUDE_ZERO, false);
	    var _this = this;

	    var colorBy = {
                id: colorByAttr,
		fields:fields,
                minValue: 0,
                maxValue: 0,
                field: null,
                index: -1,
                isString: false,
                stringMap: null,
		colorByMap: {},
		colorByValues:[],
		colorByMinPerc: this.getDisplayProp(this, "colorByMinPercentile", -1),
		colorByMaxPerc: this.getDisplayProp(this, "colorByMaxPercentile", -1),
		colorByOffset: 0,
                pctFields:null,
		displayColorTable: function() {
		    if(this.stringMap) {
			var colors = [];
			this.colorByValues= [];
			for (var i in this.stringMap) {
			    this.colorByValues.push(i);
			    colors.push(this.stringMap[i]);
			}
			_this.displayColorTable(colors, ID_COLORTABLE, this.origMinValue, this.origMaxValue, {
			    stringValues: this.colorByValues});
		    } else {
			var colors = this.colors;
			if(_this.getProperty("clipColorTable") && this.colorByValues.length) {
			    var tmp = [];
			    for(var i=0;i<this.colorByValues.length && i<colors.length;i++) 
				tmp.push(this.colors[i]);
			    colors = tmp;
			}
			_this.displayColorTable(colors, ID_COLORTABLE, this.origMinValue, this.origMaxValue, {
			    stringValues: this.colorByValues
			});
		    }
		},

		getColor: function(value, pointRecord) {
		    var percent = 0;
                    if (this.showPercent) {
                        var total = 0;
                        var data = pointRecord.getData();
                        for (var j = 0; j < data.length; j++) {
                            var ok = this.fields[j].isNumeric() && !this.fields[j].isFieldGeo();
                            if (ok && this.pctFields != null) {
                                ok = this.pctFields.indexOf(this.fields[j].getId()) >= 0 ||
                                    this.pctFields.indexOf("#" + (j + 1)) >= 0;
                            }
                            if (ok) {
                                total += data[j];
                            }
                        }
                        if (total != 0) {
                            percent = percent = value / total * 100;
                            percent = (percent - this.minValue) / (this.maxValue - this.minValue);
                        }
                    } else {
                        var v = value;
			if(this.stringMap) {
			    var color = this.stringMap[value];
			    if(!Utils.isDefined(color)) {
				return this.stringMap["default"];
			    }
			    return color;
			}
                        if (this.isString) {
                            color = this.colorByMap[v];
			    if(color) return color;
                        }
                        v += this.colorByOffset;
                        if (this.colorByLog) {
                            v = this.colorByFunc(v);
                        }
                        percent = (v - this.minValue) / this.range;
                    }

                    var index = parseInt(percent * this.colors.length);
                    if (index >= this.colors.length) index = this.colors.length - 1;
                    else if (index < 0) index = 0;
		    if(this.stringMap) {
			var color = this.stringMap[value];
			if(!Utils.isDefined(color)) {
			    return this.stringMap["default"];
			}
			return color;
		    } else {
			return this.colors[index];
		    }
		    return null;
		}
            };

            if (this.percentFields != null) {
                colorBy.pctFields = this.percentFields.split(",");
            }
	    var colors = this.getColorTable(true,colorByAttr +".colorTable");
	    if(!colors) {
		var c = this.getProperty(colorByAttr +".colors");
		if(c)
		    colors = c.split(",");
	    }
	    if(!colors)
		colors = this.getColorTable(true);
	    colorBy.colors = colors;

	    if (!colorBy.colors && this.colors && this.colors.length > 0) {
                colorBy.colors = source.colors;
                if (colorBy.colors.length == 1 && Utils.ColorTables[colorBy.colors[0]]) {
                    colorBy.colors = Utils.ColorTables[colorBy.colors[0]].colors;
                }
            }

            if (colorBy.colors == null) {
                colorBy.colors = Utils.ColorTables.grayscale.colors;
            }

	    for (var i = 0; i < fields.length; i++) {
                var field = fields[i];
                if (field.getId() == colorBy.id || ("#" + (i + 1)) == colorBy.id) {
                    colorBy.field = field;
		    if (field.isString()) colorBy.isString = true;
                }
	    }
            colorBy.index = colorBy.field != null ? colorBy.field.getIndex() : -1;
	    colorBy.stringMap = this.getColorByMap();
	    if(colorBy.index>=0) {
		records.map(record=>{
                    var tuple = record.getData();
                    var v = tuple[colorBy.index];
                    if (colorBy.isString) {
			if (!Utils.isDefined(colorBy.colorByMap[v])) {
			    var index = colorBy.colorByValues.length;
                            colorBy.colorByValues.push(v);
                            var color  = index>=colorBy.colors.length?colorBy.colors[colorBy.colors.length-1]:colorBy.colors[index];
			    colorBy.colorByMap[v] = color;
                            colorBy.minValue = 1;
                            colorBy.maxValue = colorBy.colorByValues.length;
			}
			return;
                    }
                    if (excludeZero && v === 0) {
			return;
                    }
		    if (!isNaN(v) && !(v === null)) {
			if (i == 0 || v > colorBy.maxValue) colorBy.maxValue = v;
			if (i == 0 || v < colorBy.minValue) colorBy.minValue = v;
		    }
		});
	    }

            if (this.showPercent) {
                colorBy.minValue = 0;
                colorBy.maxValue = 100;
            }
            colorBy.minValue = this.getDisplayProp(this, "colorByMin", colorBy.minValue);
            colorBy.maxValue = this.getDisplayProp(this, "colorByMax", colorBy.maxValue);
            colorBy.origMinValue = colorBy.minValue;
            colorBy.origMaxValue = colorBy.maxValue;

            var colorByLog = this.getProperty("colorByLog", false);
            colorBy.colorByFunc = Math.log;
            if (colorByLog) {
                if (colorBy.minValue < 1) {
                    colorBy.colorByOffset = 1 - colorBy.minValue;
                }
                colorBy.minValue = colorBy.colorByFunc(colorBy.minValue + colorBy.colorByOffset);
                colorBy.maxValue = colorBy.colorByFunc(colorBy.maxValue + colorBy.colorByOffset);
            }
            colorBy.range = colorBy.maxValue - colorBy.minValue;



	    return colorBy;
	},
	getColorByMap: function() {
		var colorByMapProp = this.getProperty("colorByMap");
		if (colorByMapProp) {
                    var toks = colorByMapProp.split(",");
		    var stringMap = {};
                    for (var i = 0; i < toks.length; i++) {
			var toks2 = toks[i].split(":");
			if (toks2.length > 1) {
                            stringMap[toks2[0].trim()] = toks2[1].trim();
			}
                    }
		    return stringMap;
		}
		return null;
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
            var style = "";
            contents = HtmlUtils.div([ATTR_CLASS, "display-contents-inner display-" + this.getType() + "-inner", "style", style], contents);
            this.writeHtml(ID_DISPLAY_CONTENTS, contents);
        },
        addEntry: function(entry) {
            this.entries.push(entry);
        },
        clearCachedData: function() {},
        setEntry: function(entry) {
            this.entries = [];
            this.addEntry(entry);
            this.entry = entry;
            this.entryId = entry.getId();
            this.clearCachedData();
            if (this.properties.data) {
                this.dataCollection = new DataCollection();
                var attrs = {
                    entryId: this.entryId,
                    lat: this.getProperty("latitude"),
                    lon: this.getProperty("longitude"),
                };
                this.properties.data = this.data = new PointData(entry.getName(), null, null, this.getRamadda().getRoot() + "/entry/show?entryid=" + entry.getId() + "&output=points.product&product=points.json&max=5000", attrs);
                this.data.loadData(this);
            }
            this.updateUI();
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
                var titleStyle = " color:" + this.getTextColor("titleColor") + ";";
                var bg = this.getProperty("titleBackground");
                if (bg) titleStyle += "background:" + bg + "; padding:2px;padding-right:6px;padding-left:6px;";
                titleToShow = this.getShowTitle() ? this.getDisplayTitle(title) : "";
                if (this.entryId)
                    titleToShow = HtmlUtils.href(this.getRamadda().getEntryUrl(this.entryId), titleToShow, [ATTR_CLASS, "display-title", ATTR_ID, this.getDomId(ID_TITLE), "style", titleStyle]);
            }
            return titleToShow;
        },
        handleEventMapBoundsChanged: function(source, args) {
	    if(this.getProperty("acceptBoundsChange")) {
		this.filterBounds  = args.bounds;
		this.updateUI();
            }
        },

        handleEventFieldValueSelected: function(source, args) {
            this.setProperty("filterPattern", args.value);
            this.setProperty("patternFilterField", args.field.getId());
            this.updateUI();
        },
        handleDateRangeChanged: function(source, prop) {
	    this.minDateObj = prop.minDate;
	    this.maxDateObj = prop.maxDate;
	    this.getAnimation().setDateRange(prop.minDate, prop.maxDate);
	    this.haveCalledUpdateUI = false;
	    this.dataFilterChanged();
	},
        handleEventPropertyChanged: function(source, prop) {
	    if(prop.property == "dateRange") {
		if(this.getProperty("acceptDateRangeChange")) {
		    this.handleDateRangeChanged(source, prop);
		}
		return;
	    }

	    if (prop.property == "filterValue") {
		if(!this.getProperty("acceptFilterEvent",true)) return;
		this.haveCalledUpdateUI = false;
		var widgetId = this.getFilterId(prop.fieldId);
		this.settingFilterValue = true;
		if(Utils.isDefined(prop.value2)) {
		    $("#" +widgetId+"_min").val(prop.value);
		    $("#" +widgetId+"_min").attr("data-value", prop.value);
		    $("#" +widgetId+"_max").val(prop.value2);
		    $("#" +widgetId+"_max").attr("data-value", prop.value2);
		} else {
		    var widget = $("#"+widgetId);
//		    console.log(this.type +" v:" + Array.isArray(prop.value) +" length:" + prop.value.length +" " +prop.value);
		    if(widget.attr("isCheckbox")) {
			var on = widget.attr("onValue");
			var off = widget.attr("offValue");
			console.log(this.type +".handleEvent cbx:"  + on +" " + off + " " + prop.value);
			widget.prop('checked',prop.value.includes(on));
		    } else {
			widget.val(prop.value);
		    }
		    widget.attr("data-value",prop.value);
		    if(widget.attr("isButton")) {
			widget.find(".display-filterby-button").removeClass("display-filterby-button-selected");
			widget.find("[value='" + prop.value +"']").addClass("display-filterby-button-selected");
		    }
		}
		this.settingFilterValue = false;
		this.dataFilterChanged();
		return;
            }
            this.setProperty(prop.property, prop.value);
            this.updateUI();
        },
        handleEventRecordHighlight: function(source, args) {
            if (this.getProperty("doAnimation", false) && !args.skipAnimation) {
		this.getAnimation().handleEventRecordHighlight(source, args);
	    }
	},
        handleEventRecordSelection: function(source, args) {
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
            }
            Utils.call(callback, null);
        },
        hasEntries: function() {
            return this.entries != null && this.entries.length > 0;
        },
        getWaitImage: function() {
            return HtmlUtils.image(ramaddaBaseUrl + "/icons/progress.gif");
        },
        getLoadingMessage: function(msg) {
            if (!msg) msg = this.getProperty("loadingMessage", "Loading data...");
	    if(msg=="") return "";
            return HtmlUtils.div(["text-align", "center"], this.getMessage("&nbsp;" + msg));
        },
        getMessage: function(msg) {
            return HtmlUtils.div([ATTR_CLASS, "display-message"], msg);
        },
        getFieldValue: function(id, dflt) {
            var jq = $("#" + id);
            if (jq.length > 0) {
                return jq.val();
            }
            return dflt;
        },
        getFooter: function() {
            return HtmlUtils.div([ATTR_ID, this.getDomId(ID_FOOTER), ATTR_CLASS, "display-footer"],
				 HtmlUtils.leftRight(HtmlUtils.div([ATTR_ID, this.getDomId(ID_FOOTER_LEFT), ATTR_CLASS, "display-footer-left"], ""),
						     HtmlUtils.div([ATTR_ID, this.getDomId(ID_FOOTER_RIGHT), ATTR_CLASS, "display-footer-right"], "")));
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
            var fixedFields = this.getProperty(PROP_FIELDS);
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
                            html += HtmlUtils.div([ATTR_CLASS, "display-dialog-subheader"], "Group By");
                            html += HtmlUtils.openTag(TAG_DIV, [ATTR_CLASS, "display-fields"]);
                            var on = this.groupBy == null || this.groupBy == "";
                            html += HtmlUtils.tag(TAG_DIV, [ATTR_TITLE, "none"],
						  HtmlUtils.radio("none", this.getDomId("groupby"), groupByClass, "none", !on) + " None");
                        }
                        cnt++;
                        var on = this.groupBy == field.getId();
                        var idBase = "groupby_" + collectionIdx + "_" + i;
                        field.radioId = this.getDomId(idBase);
                        html += HtmlUtils.tag(TAG_DIV, [ATTR_TITLE, field.getId()],
					      HtmlUtils.radio(field.radioId, this.getDomId("groupby"), groupByClass, field.getId(), on) + " " + field.getUnitLabel() + " (" + field.getId() + ")"
					     );
                    }
                    if (cnt > 0) {
                        html += HtmlUtils.closeTag(TAG_DIV);
                    }
                }

                if ( /*this.canDoMultiFields() && */ fields.length > 0) {
                    var selected = this.getSelectedFields([]);
                    var selectedIds = [];
                    for (i = 0; i < selected.length; i++) {
                        selectedIds.push(selected[i].getId());
                    }
                    var disabledFields = "";
                    html += HtmlUtils.div([ATTR_CLASS, "display-dialog-subheader"], "Displayed Fields");
                    html += HtmlUtils.openTag(TAG_DIV, [ATTR_CLASS, "display-fields"]);
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
                            disabledFields += HtmlUtils.div([], label);
                        } else {
                            if (field.derived) {
                                label += " (derived)";
                            }
                            var widget;
                            if (this.canDoMultiFields()) {
                                widget = HtmlUtils.checkbox(field.checkboxId, ["class", checkboxClass], on);
                            } else {
                                widget = HtmlUtils.radio(field.checkboxId, "field_radio", checkboxClass, "", on);
                            }

                            html += HtmlUtils.tag(TAG_DIV, [ATTR_TITLE, field.getId()],
						  widget + " " + label
						 );
                        }
                        //                        html+= "<br>";
                    }
                }
                if (disabledFields != "") {
                    html += HtmlUtils.div(["style", "border-top:1px #888  solid;"], "<b>No Data Available</b>" + disabledFields);
                }

                html += HtmlUtils.closeTag(TAG_DIV);
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
	    if(this.getProperty("binDate")) {
		var binType = this.getProperty("binType","total");
		var binCount = binType=="count";
		if(binCount) {
		    var fields = [];
		    fields.push(new RecordField({
			id:"count",
			label:this.getProperty("binCountLabel","Count"),
			type:"double",
			chartable:true
		    }));		    
		    return fields;
		}
	    }


            this.debugSelected = false;
            this.lastSelectedFields = this.getSelectedFieldsInner(dfltList);
            var fixedFields = this.getProperty(PROP_FIELDS);
            if (fixedFields) fixedFields.length = 0;
            this.setDisplayTitle();
            return this.lastSelectedFields;
        },
        getSelectedFieldsInner: function(dfltList) {
            if (this.debugSelected)
                console.log("getSelectedFields dflt:" + (dfltList ? dfltList.length : "null"));
            if (this.selectedFields) {
                if (this.debugSelected)
                    console.log("\tgot this.selectedFields");
                return this.selectedFields;
            }
            var df = [];
            var dataList = this.dataCollection.getList();
            //If we have fixed fields then clear them after the first time
            var fixedFields = this.getProperty(PROP_FIELDS);
            if (fixedFields && (typeof fixedFields) == "string") {
                fixedFields = fixedFields.split(",");
            }

            for (var collectionIdx = 0; collectionIdx < dataList.length; collectionIdx++) {
                var pointData = dataList[collectionIdx];
                var fields = this.getFieldsToSelect(pointData);
                if (fixedFields != null && fixedFields.length > 0) {
                    if (this.debugSelected)
                        console.log("\thave fixed fields:" + fixedFields.length);
                    for (var i = 0; i < fixedFields.length; i++) {
                        var sfield = fixedFields[i];
                        if (this.debugSelected)
                            console.log("\t\tfield:" + sfield);
                        for (var j = 0; j < fields.length; j++) {
                            var field = fields[j];
                            var id = field.getId();
                            if (this.debugSelected)
                                console.log("\t\t\tid:" + id);
                            if (id == sfield || ("#" + (j + 1)) == sfield) {
                                if (this.debugSelected)
                                    console.log("\t\t\tgot:" + field.getLabel());
                                df.push(field);
                                break;
                            }
                        }
                    }
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
                        console.log("\tlastSelectedFields");
                    return this.lastSelectedFields;
                }
            }
            if (df.length == 0) {
                df = this.getDefaultSelectedFields(fieldsToSelect, dfltList);
                if (this.debugSelected)
                    console.log("\tgetDefault:" + df.length);
            }
            return df;
        },
        getDefaultSelectedFields: function(fields, dfltList) {
            if (this.defaultSelectedToAll() && this.allFields != null) {
                var tmp = [];
                for (i = 0; i < this.allFields.length; i++) {
                    var field = this.allFields[i];
                    if (!field.isFieldGeo()) {
                        tmp.push(field);
                    }
                }
                return tmp;
            }

            if (dfltList != null) {
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
	    if(sortFields.length==0) return records;
	    records = Utils.cloneList(records);
	    var sortAscending = this.getProperty("sortAscending",true);
	    records.sort((a,b)=>{
		var row1 = this.getDataValues(a);
		var row2 = this.getDataValues(b);
		var result = 0;
		for(var i=0;i<sortFields.length;i++) {
		    var sortField = sortFields[i];
		    var v1 = row1[sortField.getIndex()];
		    var v2 = row2[sortField.getIndex()];
		    if(v1<v2) result = sortAscending?-1:1;
		    else if(v1>v2) result = sortAscending?1:-1;
		    else result = 0;
		    if(result!=0) break;
		}
		return result;
	    });
	    return records;
	},
        getFieldById: function(fields, id) {
            if (!id) return null;
            if (!fields) {
                var pointData = this.getData();
                if (pointData == null) return null;
                fields = pointData.getRecordFields();
            }

            for (var i = 0; i < fields.length; i++) {
                var field = fields[i];
                if (field.getId() == id || id == ("#" + i)) {
                    return field;
                }
            }
            return null;
        },

        getFieldsByIds: function(fields, ids) {
            var result = [];
            if (!ids) return result;
            if ((typeof ids) == "string")
                ids = ids.split(",");
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
		if(field.getId() == "recordDate") continue;
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
	    records.map(r=>{
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
                if (Utils.isNumber(value)) {
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
	getFilterFieldValues:function(field) {
	    var element =$("#" + this.getDomId("filterby_" + field.getId()));
	    var value=null;
	    if(element.attr("isCheckbox")) {
		if(element.is(':checked')) {
		    value = element.attr("onValue");
		} else {
		    value = element.attr("offValue");
		}
		if(!value) value = FILTER_ALL;
	    } else if(element.attr("isButton")) {
		value = element.attr("data-value");
	    } else {
		value = element.val();
	    }
	    if(!Array.isArray(value)) value = value.split(",");
	    var tmp = [];
	    value.map(v=>tmp.push(v.trim()));
	    value = tmp;
//	    console.log(this.type +".getFilterFieldValues:" + Array.isArray(value) +" " + value.length +" " +value);
	    return value;
	},
	filterData: function(dataList, fields, doGroup, skipFirst) {
//	    var t1=  new Date();
            var pointData = this.getData();
            if (!dataList) {
                if (pointData == null) return null;
                dataList = pointData.getRecords();
            }
            if (!fields) {
                fields = pointData.getRecordFields();
            }
            if(doGroup || this.requiresGrouping()) {
                dataList = pointData.extractGroup(this.dataGroup, dataList);
            }

	    var newData = [];
	    var filters = {};
	    var allIsUsed = false;
	    for(var i=0;i<this.filterFields.length;i++) {
		var filterField = this.filterFields[i];
		if(filterField.isString()) {
		    var values = this.getFilterFieldValues(filterField);
		    if(!values && !Array.isArray(values)) values = [values];
		    if(values.includes(FILTER_ALL)) {
			allIsUsed = true;
			break;
		    }
		}
	    }


	    for(var i=0;i<this.filterFields.length;i++) {
		var filterField = this.filterFields[i];
/*
		var useIfAll = this.getProperty(filterField.getId() +".filterUseIfAllIsSet",true);
 		if(allIsUsed && !useIfAll) {
		    continue;
		}
*/
		var prefix = this.getProperty(filterField.getId() +".filterPrefix");
		var suffix = this.getProperty(filterField.getId() +".filterSuffix");
		if (prefix) pattern = prefix + value;
		if (suffix) pattern = value + suffix;
		var value=null;
		var values=null;
		var _values =[];
		var regexps =[];
		var values=null;
		if(filterField.isNumeric()) {
		    var minField = $("#" + this.getDomId("filterby_" + filterField.getId()+"_min"));
		    var maxField = $("#" + this.getDomId("filterby_" + filterField.getId()+"_max"));
		    var minValue = parseFloat(minField.val().trim());
		    var maxValue = parseFloat(maxField.val().trim());
		    var dfltMinValue = parseFloat(minField.attr("data-min"));
		    var dfltMaxValue = parseFloat(maxField.attr("data-max"));
//		    console.log(this.type +" filterData:" + filterField.getId() +" " +minValue +" " + maxValue);

		    if(minValue!= dfltMinValue || maxValue!= dfltMaxValue) {
			value = [minValue,maxValue];
		    }
 		} else if(filterField.getType()=="date"){
		    var date1 = $("#" + this.getDomId("filterby_" + filterField.getId()+"_date1")).val();
		    var date2 = $("#" + this.getDomId("filterby_" + filterField.getId()+"_date2")).val();
		    if(date1!=null && date1.trim()!="") 
			date1 =  Utils.parseDate(date1);
		    else
			date1=null;
		    if(date2!=null && date2.trim()!="") 
			date2 =  Utils.parseDate(date2);
		    else
			date2=null;
		    if(date1!=null && date2!=null)
			value = [date1,date2]; 
		}  else {
		    values = this.getFilterFieldValues(filterField);
		    if(!values && !Array.isArray(values)) values = [values];
		    if(values.length==0) continue;
		    var tmp = [];
		    values.map(v=>{
			tmp.push(v.replace(/_comma_/g,","));
		    });
		    values = tmp;
		    values.map(v=>{
			_values.push((""+v).toLowerCase());
			try {
			    regexps.push(new RegExp(v,"i"));
			} catch(skipIt){}
		    });
		}
		var filterStartsWith = this.getProperty(filterField.getId() +".filterStartsWith",false);
		var anyValues = value!=null;
		if(!anyValues && values) {
		    values.map(v=>{if(v.length>0 && v!= FILTER_ALL)anyValues = true});
		}
		if(anyValues) {
		    filters[filterField.getId()] = {
			value:value,
			values:values,
			regexps:regexps,
			_values:_values,
			anyValues:anyValues,
			startsWith:filterStartsWith,
		
		    };

		}
	    }
//	    console.log(this.type +" filterData:" + JSON.stringify(filters,null,2));
	    for (var rowIdx = 0; rowIdx <dataList.length; rowIdx++) {
		var record = dataList[rowIdx];
                var date = record.getDate();
                if (!this.dateInRange(date)) {
		    continue;
		}
		if(this.filterBounds && record.hasLocation()) {
		    var b = this.filterBounds;
		    var lat = record.getLatitude();
		    var lon = record.getLongitude();
		    if(lat>b.top || lat<b.bottom || lon <b.left || lon>b.right)
			continue;
		}
		var row = this.getDataValues(record);
		var ok = true;
		for(var i=0;i<this.filterFields.length && ok;i++) {
		    var filterField = this.filterFields[i];
		    var filter = filters[filterField.getId()];
		    if(filter==null) continue;
		    var rowValue = row[filterField.getIndex()];
		    var debug = false; //(rowIdx<10);
		    if(debug)
			console.log("filter field:" + filterField.getId() + " value:" + rowValue);
		    if(filterField.getType() == "enumeration") {
			ok = filter.values.includes(""+rowValue);
			if(debug)
			    console.log("enum:" + rowValue + " ok:" + ok);
		    } else if(filterField.isNumeric()) {
			if(isNaN(filter.value[0]) && isNaN(filter.value[0])) continue;
			if(!isNaN(filter.value[0]) && rowValue<filter.value[0]) ok = false;
			else if(!isNaN(filter.value[1]) && rowValue>filter.value[1]) ok = false;
		    } else if(filterField.getType()=="date"){
			if(filter.value && typeof filter.value[0] && Array.isArray(filter.value)) {
			    if(rowValue == null) {
				ok = false;
			    }    else  {
				var date1 = filter.value[0];
				var date2 = filter.value[1];
				if(date1 && rowValue.getTime()<date1.getTime())
				    ok = false;
				else if(date2 && rowValue.getTime()>date2.getTime())
				    ok = false;
			    }
			}
		    } else {
			var startsWith = filter.startsWith;
			ok = false;
			roWValue  = (""+rowValue).toLowerCase();
			for(var j=0;j<filter._values.length;j++) {
			    var fv = _values[j];
			    if(startsWith) {
				if(rowValue.startsWith(fv)) {
				    ok = true;
				    break;
				}
			    } else  if(rowValue.indexOf(fv)>=0) {
				ok = true;
				break;
			    }
			}
			if(!ok) {
			    for(ri=0;ri<filter.regexps.length;ri++) {
				if(rowValue.match(filter.regexps[ri])) {
				    ok = true;
				    break;
				}
			    }
			}
		    }
		}
		if(skipFirst && rowIdx==0) {
		    ok = true;
		}
		if(ok) {
		    newData.push(dataList[rowIdx]);
		}
	    }
	    dataList = newData;


            var stride = parseInt(this.getProperty("stride", -1));
            if (stride > 0) {
                var list = [];
                var cnt = 0;
                //
                //1,2,3,4,5,6,7,8,9,10
                for (var i = 0; i < dataList.length; i += (stride + 1)) {
                    list.push(dataList[i]);
                }
                dataList = list;
            }


	    if(this.getProperty("binDate")) {
		var what = this.getProperty("binDate");
		var binType = this.getProperty("binType","total");
		var binCount = binType=="count";
		var binned = [];
		var record = dataList[0];
		/*
		  if(binCount) {
		  var data = record.getData();
		  data = [data[0],"Count"];
		  var newRecord = new  PointRecord(record.getLatitude(),record.getLongitude(),
		  record.getElevation(),record.getDate(),data);

		  binned.push(newRecord);
		  } else {
		  binned.push(record);
		  }
		*/
		var map ={};
		for (var i = 0; i < dataList.length; i++) {
		    var record = dataList[i];
		    var tuple = this.getDataValues(record);
		    var key;
		    if(what=="month") {
			key = record.getDate().getUTCFullYear() + "-" + (record.getDate().getUTCMonth() + 1);
		    } else if(what=="day") {
			key = record.getDate().getUTCFullYear() + "-" + (record.getDate().getUTCMonth() + 1) +"-" + record.getDate().getUTCDate();
		    } else {
			key = record.getDate().getUTCFullYear()+"";
		    }
		    if(!Utils.isDefined(map[key])) {
			var date = Utils.parseDate(key);
			var data = Utils.cloneList(record.getData());
			if(binCount) {
			    for(k=0;k<data.length;k++) data[k]=0;
			    //			    data =  [0];
			}
			var newRecord = new  PointRecord(record.getLatitude(),record.getLongitude(),
							 record.getElevation(),date,data);
			map[key] = data;
			binned.push(newRecord);
		    } else {
			var tuple1 = map[key];
			if(binCount) {
			    for(k=0;k<tuple1.length;k++) tuple1[k]++;
			    //			    tuple1[0]++; 
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
		dataList = binned;
	    }
//	    var t2=  new Date();
//	    Utils.displayTimes("filterData",[t1,t2]);
	    dataList = this.sortRecords(dataList);

	    this.recordToIndex = {};
	    this.indexToRecord = {};
	    for(var i=0;i<dataList.length;i++) {
		var record = dataList[i];
		this.recordToIndex[record.getId()] = i;
		this.indexToRecord[i] = record;
	    }
            return dataList;
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
            wiki += this.getWikiText();
            for (var i = 0; i < this.displays.length; i++) {
                var display = this.displays[i];
                if (display.getIsLayoutFixed()) {
                    continue;
                }
                wiki += display.getWikiText();
            }
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
            args.push(window.btoa(wiki));


            var url = HtmlUtils.getUrl(ramaddaBaseUrl + "/entry/publish", args);
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
            var attrs = ["layoutHere", "false",
			 "type", this.type,
			 "column", this.getColumn(),
			 "row", this.getRow()
			];
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
            var wiki = "{{display " + HtmlUtils.attrs(attrs) + "}}\n\n"

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
                    var html = HtmlUtils.openTag(TAG_OL, [ATTR_CLASS, "display-entrylist-list", ATTR_ID, theDisplay.getDomId(ID_LIST)]);
                    html += theDisplay.getEntriesTree(entries);
                    html += HtmlUtils.closeTag(TAG_OL);
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
                if (props.headerRight) html += HtmlUtils.leftRight(left, props.headerRight);
                else html += left;
                //                    html += "<hr>";
            }
            var divid = HtmlUtils.getUniqueId("entry_");
            html += HtmlUtils.div(["id", divid], "");

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
                var img = HtmlUtils.tag(TAG_IMG, ["src", entry.getResourceUrl(), /*ATTR_WIDTH,"100%",*/
						  ATTR_CLASS, "display-entry-image"
						 ]);

                html += HtmlUtils.href(entry.getResourceUrl(), img) + "<br>";
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
                        html += HtmlUtils.image(url, [ATTR_CLASS, "display-entry-thumbnail"]);
                    }
                }
            }
            if (entry.getIsGroup() /* && !entry.isRemote*/ ) {
                html += HtmlUtils.div([ATTR_ID, this.getDomId(ID_GROUP_CONTENTS + entry.getIdForDom())], "" /*this.getWaitImage()*/ );
            }


            html += HtmlUtils.formTable();

            if (props.showDetails) {
                if (entry.url) {
                    html += HtmlUtils.formEntry("URL:", HtmlUtils.href(entry.url, entry.url));
                }

                if (entry.remoteUrl) {
                    html += HtmlUtils.formEntry("URL:", HtmlUtils.href(entry.remoteUrl, entry.remoteUrl));
                }
                if (entry.remoteRepository) {
                    html += HtmlUtils.formEntry("From:", HtmlUtils.href(entry.remoteRepository.url, entry.remoteRepository.name));
                }
            }

            var columns = entry.getAttributes();

            if (entry.getFilesize() > 0) {
                html += HtmlUtils.formEntry("File:", entry.getFilename() + " " +
					    HtmlUtils.href(entry.getResourceUrl(), HtmlUtils.image(ramaddaBaseUrl + "/icons/download.png")) + " " +
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
                        tmp += HtmlUtils.href(url, url);
                        tmp += "<br>";
                    }
                    columnValue = tmp;
                }
                html += HtmlUtils.formEntry(column.label + ":", columnValue);
            }

            html += HtmlUtils.closeTag(TAG_TABLE);
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
                var link = HtmlUtils.tag(TAG_A, [ATTR_HREF, entry.getEntryUrl()], icon + " " + entryName);
                entryName = "";
                var entryIdForDom = entry.getIdForDom() + domIdSuffix;
                var entryId = entry.getId();
                var arrow = HtmlUtils.image(icon_tree_closed, [ATTR_BORDER, "0",
							       "tree-open", "false",
							       ATTR_ID,
							       this.getDomId(ID_TREE_LINK + entryIdForDom)
							      ]);
                var toggleCall = this.getGet() + ".toggleEntryDetails(event, '" + entryId + "'," + suffix + ",'" + props.handlerId + "');";
                var toggleCall2 = this.getGet() + ".entryHeaderClick(event, '" + entryId + "'," + suffix + "); ";
                var open = HtmlUtils.onClick(toggleCall, arrow);
                var extra = "";

                if (showIndex) {
                    extra = "#" + (i + 1) + " ";
                }
                if (handler && handler.getEntryPrefix) {
                    extra += handler.getEntryPrefix(props.handlerId, entry);
                }
                var left = HtmlUtils.div([ATTR_CLASS, "display-entrylist-name"], entryMenuButton + " " + open + " " + extra + link + " " + entryName);
                var details = HtmlUtils.div([ATTR_ID, this.getDomId(ID_DETAILS + entryIdForDom), ATTR_CLASS, "display-entrylist-details"], HtmlUtils.div([ATTR_CLASS, "display-entrylist-details-inner", ATTR_ID, this.getDomId(ID_DETAILS_INNER + entryIdForDom)], ""));

                //                    console.log("details:" + details);

                var line;
                if (this.getProperty("showToolbar", true)) {
                    line = HtmlUtils.leftCenterRight(left, "", toolbar, "80%", "1%", "19%");
                } else {
                    line = left;
                }
                //                    line = HtmlUtils.leftRight(left,toolbar,"60%","30%");


                var mainLine = HtmlUtils.div(["onclick", toggleCall2, ATTR_ID, this.getDomId(ID_DETAILS_MAIN + entryIdForDom), ATTR_CLASS, "display-entrylist-entry-main" + " " + "entry-main-display-entrylist-" + (even ? "even" : "odd"), ATTR_ENTRYID, entryId], line);
                var line = HtmlUtils.div(["class", (even ? "ramadda-row-even" : "ramadda-row-odd"), ATTR_ID, this.getDomId("entryinner_" + entryIdForDom)], mainLine + details);

                html += HtmlUtils.tag(TAG_DIV, [ATTR_ID,
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
            var html = HtmlUtils.openTag(TAG_TABLE, [ATTR_WIDTH, "100%", "cellpadding", "0", "cellspacing", "0"]);
            html += HtmlUtils.openTag(TAG_TR, ["valign", "top"]);
            for (var i = 0; i < columnNames.length; i++) {
                html += HtmlUtils.td([ATTR_ALIGN, "center", ATTR_CLASS, "display-entrytable-header"], columnNames[i]);
            }
            html += HtmlUtils.closeTag(TAG_TR);

            for (var i = 0; i < entries.length; i++) {
                html += HtmlUtils.openTag(TAG_TR, ["valign", "top"]);
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
                        value = HtmlUtils.tag(TAG_A, [ATTR_HREF, entry.getEntryUrl()], entry.getName());
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

                    html += HtmlUtils.td(attrs, value);
                }
                html += HtmlUtils.closeTag(TAG_TR);
            }
            html += HtmlUtils.closeTag(TAG_TABLE);
            return html;
        },

        makeEntryToolbar: function(entry, handler, handlerId) {
            var get = this.getGet();
            var toolbarItems = [];
            //                 toolbarItems.push(HtmlUtils.tag(TAG_A, [ATTR_HREF, entry.getEntryUrl(),"target","_"], 
            //                                                HtmlUtils.image(ramaddaBaseUrl +"/icons/application-home.png",["border",0,ATTR_TITLE,"View Entry"])));
            if (entry.getType().getId() == "type_wms_layer") {
                toolbarItems.push(HtmlUtils.tag(TAG_A, ["onclick", get + ".addMapLayer(" + HtmlUtils.sqt(entry.getId()) + ");"],
						HtmlUtils.image(ramaddaBaseUrl + "/icons/map.png", ["border", 0, ATTR_TITLE, "Add Map Layer"])));

            }
            if (entry.getType().getId() == "geo_shapefile" || entry.getType().getId() == "geo_geojson") {
                toolbarItems.push(HtmlUtils.tag(TAG_A, ["onclick", get + ".addMapLayer(" + HtmlUtils.sqt(entry.getId()) + ");"],
						HtmlUtils.image(ramaddaBaseUrl + "/icons/map.png", ["border", 0, ATTR_TITLE, "Add Map Layer"])));

            }

            var jsonUrl = this.getPointUrl(entry);
            if (jsonUrl != null) {
                jsonUrl = jsonUrl.replace(/\'/g, "_");
                toolbarItems.push(HtmlUtils.tag(TAG_A, ["onclick", get + ".createDisplay(" + HtmlUtils.sqt(entry.getFullId()) + "," +
							HtmlUtils.sqt("table") + "," + HtmlUtils.sqt(jsonUrl) + ");"
						       ],
						HtmlUtils.getIconImage("fa-table", [ATTR_TITLE, "Create Tabular Display"])));

                var x;
                toolbarItems.push(x = HtmlUtils.tag(TAG_A, ["onclick", get + ".createDisplay(" + HtmlUtils.sqt(entry.getFullId()) + "," +
							    HtmlUtils.sqt("linechart") + "," + HtmlUtils.sqt(jsonUrl) + ");"
							   ],
						    HtmlUtils.getIconImage("fa-chart-line", [ATTR_TITLE, "Create Chart"])));
            }
            toolbarItems.push(HtmlUtils.tag(TAG_A, ["onclick", get + ".createDisplay(" + HtmlUtils.sqt(entry.getFullId()) + "," +
						    HtmlUtils.sqt("entrydisplay") + "," + HtmlUtils.sqt(jsonUrl) + ");"
						   ],
					    HtmlUtils.getIconImage("fa-file", ["border", 0, ATTR_TITLE, "Show Entry"])));

            if (entry.getFilesize() > 0) {
                toolbarItems.push(HtmlUtils.tag(TAG_A, [ATTR_HREF, entry.getResourceUrl()],
						HtmlUtils.image(ramaddaBaseUrl + "/icons/download.png", ["border", 0, ATTR_TITLE, "Download (" + entry.getFormattedFilesize() + ")"])));

            }


            var entryMenuButton = this.getEntryMenuButton(entry);
            /*
              entryMenuButton =  HtmlUtils.onClick(this.getGet()+".showEntryDetails(event, '" + entry.getId() +"');", 
              HtmlUtils.image(ramaddaBaseUrl+"/icons/downdart.png", 
              [ATTR_CLASS, "display-dialog-button", ATTR_ID,  this.getDomId(ID_MENU_BUTTON + entry.getId())]));

            */
            //            toolbarItems.push(entryMenuButton);

            var tmp = [];



            if (handler && handler.addToToolbar) {
                handler.addToToolbar(handlerId, entry, toolbarItems);
            }

            for (var i = 0; i < toolbarItems.length; i++) {
                tmp.push(HtmlUtils.div([ATTR_CLASS, "display-entry-toolbar-item"], toolbarItems[i]));
            }
            toolbarItems = tmp;
            return HtmlUtils.div([ATTR_CLASS, "display-entry-toolbar", ATTR_ID,
				  this.getEntryToolbarId(entry.getIdForDom())
				 ],
				 HtmlUtils.join(toolbarItems, ""));
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
                //                    detailsInner.html(HtmlUtils.image(icon_progress));
            } else {
                if (entry.getIsGroup() /* && !entry.isRemote*/ ) {
                    detailsInner.html(HtmlUtils.image(icon_progress));
                    var theDisplay = this;
                    var callback = function(entries) {
                        theDisplay.displayChildren(entry, entries, suffix, handlerId);
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
            var menuButton = HtmlUtils.onClick(this.getGet() + ".showEntryMenu(event, '" + entry.getId() + "');",
					       HtmlUtils.image(ramaddaBaseUrl + "/icons/menu.png",
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
                showMenu: false,
                sourceEntry: entry,
                entryId: entry.getId(),
                showTitle: false,
                showDetails: true,
                title: entry.getName(),
		layoutHere:false,
            };
            if (displayProps) $.extend(props, displayProps);

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
            viewMenuItems.push(HtmlUtils.tag(TAG_LI, [], HtmlUtils.tag(TAG_A, ["href", entry.getEntryUrl(), "target", "_"], "View Entry")));
            if (entry.getFilesize() > 0) {
                fileMenuItems.push(HtmlUtils.tag(TAG_LI, [], HtmlUtils.tag(TAG_A, ["href", entry.getResourceUrl()], "Download " + entry.getFilename() + " (" + entry.getFormattedFilesize() + ")")));
            }

            if (this.jsonUrl != null) {
                fileMenuItems.push(HtmlUtils.tag(TAG_LI, [], "Data: " + HtmlUtils.onClick(get + ".fetchUrl('json');", "JSON") +
						 HtmlUtils.onClick(get + ".fetchUrl('csv');", "CSV")));
            }

            var newMenu = "<a>New</a><ul>";
            newMenu += HtmlUtils.tag(TAG_LI, [], HtmlUtils.onClick(get + ".createDisplay('" + entry.getFullId() + "','entrydisplay');", "New Entry Display"));
            newMenuItems.push(HtmlUtils.tag(TAG_LI, [], HtmlUtils.onClick(get + ".createDisplay('" + entry.getFullId() + "','entrydisplay');", "New Entry Display")));


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
                        var call = get + ".createDisplay(" + HtmlUtils.sqt(entry.getFullId()) + "," + HtmlUtils.sqt(type.type) + "," + HtmlUtils.sqt(pointUrl) + ");";
                        var li = HtmlUtils.tag(TAG_LI, [], HtmlUtils.tag(TAG_A, ["onclick", call], type.label));
                        catMap[type.category] += li + "\n";
                        newMenuItems.push(li);
                    }
                }

                for (a in catMap) {
                    newMenu += catMap[a] + "</li></ul>";
                }
            }



            if (fileMenuItems.length > 0)
                menus.push("<a>File</a>" + HtmlUtils.tag(TAG_UL, [], HtmlUtils.join(fileMenuItems)));
            if (viewMenuItems.length > 0)
                menus.push("<a>View</a>" + HtmlUtils.tag(TAG_UL, [], HtmlUtils.join(viewMenuItems)));
            if (newMenuItems.length > 0)
                menus.push(newMenu);


            var topMenus = "";
            for (var i = 0; i < menus.length; i++) {
                topMenus += HtmlUtils.tag(TAG_LI, [], menus[i]);
            }

            var menu = HtmlUtils.tag(TAG_UL, [ATTR_ID, this.getDomId(ID_MENU_INNER + entry.getIdForDom()), ATTR_CLASS, "sf-menu"],
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
            var moveRight = HtmlUtils.onClick(get + ".moveDisplayRight();", "Right");
            var moveLeft = HtmlUtils.onClick(get + ".moveDisplayLeft();", "Left");
            var moveTop = HtmlUtils.onClick(get + ".moveDisplayTop();", "Top");
            var moveUp = HtmlUtils.onClick(get + ".moveDisplayUp();", "Up");
            var moveDown = HtmlUtils.onClick(get + ".moveDisplayDown();", "Down");


            var menu = "<table class=formtable>" +
                "<tr><td align=right><b>Move:</b></td><td>" + moveTop + " " + moveUp + " " + moveDown + " " + moveRight + " " + moveLeft + "</td></tr>" +
                "<tr><td align=right><b>Row:</b></td><td> " + HtmlUtils.input("", this.getProperty("row", ""), ["size", "7", ATTR_ID, this.getDomId("row")]) + " &nbsp;&nbsp;<b>Col:</b> " + HtmlUtils.input("", this.getProperty("column", ""), ["size", "7", ATTR_ID, this.getDomId("column")]) + "</td></tr>" +

                //                    "<tr><td align=right><b>Name:</b></td><td> " + HtmlUtils.input("", this.getProperty("name",""), ["size","7",ATTR_ID,  this.getDomId("name")]) + "</td></tr>" +
                //                    "<tr><td align=right><b>Source:</b></td><td>"  + 
                //                    HtmlUtils.input("", this.getProperty("eventsource",""), ["size","7",ATTR_ID,  this.getDomId("eventsource")]) +
                //                    "</td></tr>" +
                "<tr><td align=right><b>Width:</b></td><td> " + HtmlUtils.input("", this.getProperty("width", ""), ["size", "7", ATTR_ID, this.getDomId("width")]) + "  " + "<b>Height:</b> " + HtmlUtils.input("", this.getProperty("height", ""), ["size", "7", ATTR_ID, this.getDomId("height")]) + "</td></tr>" +
                "</table>";
            var tmp =
                HtmlUtils.checkbox(this.getDomId("showtitle"), [], this.showTitle) + " Title  " +
                HtmlUtils.checkbox(this.getDomId("showdetails"), [], this.showDetails) + " Details " +
                "&nbsp;&nbsp;&nbsp;" +
                HtmlUtils.onClick(get + ".askSetTitle();", "Set Title");
            menu += HtmlUtils.formTable() + HtmlUtils.formEntry("Show:", tmp) + "</table>";

            return menu;
        },
        isLayoutHorizontal: function() {
	    return this.getProperty("orientation","")!= "horizontal";
        },
        loadInitialData: function() {
            if (!this.needsData() || this.properties.data == null) {
                return;
            }
            if (this.getProperty("latitude")) {
                this.data.lat = this.getProperty("latitude");
                this.data.lon = this.getProperty("longitude", "-105");
            }

            if (this.properties.data.hasData()) {
                this.addData(this.properties.data);
                return;
            }
            this.properties.data.derived = this.derived;
            this.properties.data.loadData(this);
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
        getShowMenu: function() {
            if (Utils.isDefined(this.showMenu)) return this.showMenu;
            return this.getProperty(PROP_SHOW_MENU, true);
        },
        askSetTitle: function() {
            var t = this.getTitle(false);
            var v = prompt("Title", t);
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
        getShowTitle: function() {
            return this.getSelfProperty("showTitle", false);
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
        },
        deltaRow: function(delta) {
            var row = parseInt(this.getProperty("row", 0));
            row += delta;
            if (row < 0) row = 0;
            this.setDisplayProperty("row", row);
            this.getLayoutManager().layoutChanged(this);
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
            var get = this.getGet();
            var menuItems = [];
            this.getMenuItems(menuItems);
            var form = "<form>";

            form += this.getDisplayMenuSettings();
            for (var i = 0; i < menuItems.length; i++) {
                form += HtmlUtils.div([ATTR_CLASS, "display-menu-item"], menuItems[i]);
            }
            form += "</form>";
            tabTitles.push("Display");
            tabContents.push(form);
        },
        popup: function(srcId, popupId) {
            var popup = GuiUtils.getDomObject(popupId);
            var srcObj = GuiUtils.getDomObject(srcId);
            if (!popup || !srcObj) return;
            var myalign = 'left top';
            var atalign = 'left bottom';
            showObject(popup);
            jQuery("#" + popupId).position({
                of: jQuery("#" + srcId),
                my: myalign,
                at: atalign,
                collision: "none none"
            });
            //Do it again to fix a bug on safari
            jQuery("#" + popupId).position({
                of: jQuery("#" + srcId),
                my: myalign,
                at: atalign,
                collision: "none none"
            });

            $("#" + popupId).draggable();
        },
        checkLayout: function() {},
        displayData: function() {},
        createUI: function() {
            var divid = this.getProperty(PROP_DIVID);
            if (divid != null) {
                var html = this.getHtml();
                $("#" + divid).html(html);
            } else {
                console.log("error: no div defined for display:" + this.getType());
            }
        },
        setDisplayReady: function() {
            this.displayReady = true;
        },
        getDisplayReady: function() {
            return this.displayReady;
        },
        pageHasLoaded: function() {
            this.setDisplayReady(true);
        },
        initDisplay: function() {
            this.createUI();
            if (this.getProperty("doAnimation", false)) {
		this.getAnimation().makeControls();
            }
            this.checkSearchBar();
        },
	getFilterId: function(id) {
	    return  this.getDomId("filterby_" + id);
	},
        checkSearchBar: function() {
            let _this = this;
            var pointData = this.getData();
            if (pointData == null) return;

	    this.filterFields = [];
            this.colorByFields = this.getFieldsByIds(null, this.getProperty("colorByFields", "", true));
            this.sizeByFields = this.getFieldsByIds(null, this.getProperty("sizeByFields", "", true));

	    
            var filterBy = this.getProperty("filterFields","",true).split(",");
            var fields= pointData.getRecordFields();
            var records = pointData.getRecords();

	    var dateMin = null;
	    var dateMax = null;
	    var dates = [];
	    records.map(record=>{
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
	    });

            if (dateMax) {
		this.getAnimation().init(dateMin, dateMax,records);
            }



	    var header2="";
	    var searchBar  = "";

	    if(this.getProperty("showChartFieldsMenu")) {
		var chartFields =  pointData.getChartableFields();
		if(chartFields.length) {
		    var enums = [];
		    chartFields.map(field=>{
			enums.push([field.getId(),field.getLabel()]);
		    });
		    header2 += HtmlUtils.span(["class","display-filterby"],
					      HtmlUtils.span(["class","display-filterby-label"], "Display: ") + HtmlUtils.select("",["style","", "id",this.getDomId("chartfields")],enums,this.getProperty("fields","")))+"&nbsp;";
		}
	    }


	    if(this.colorByFields.length>0) {
		var enums = [];
		this.colorByFields.map(field=>{
		    enums.push([field.getId(),field.getLabel()]);
		});
		header2 += HtmlUtils.span(["class","display-filterby"],
					  HtmlUtils.span(["class","display-filterby-label"], "Color by: ") + HtmlUtils.select("",["style","", "id",this.getDomId("colorbyselect")],enums,this.getProperty("colorBy","")))+"&nbsp;";
	    }
	    if(this.sizeByFields.length>0) {
		var enums = [];
		this.sizeByFields.map(field=>{
		    enums.push([field.getId(),field.getLabel()]);
		});
		header2 += HtmlUtils.span(["class","display-filterby"],
					  HtmlUtils.span(["class","display-filterby-label"],"Size by: ") + HtmlUtils.select("",["style","", "id",this.getDomId("sizebyselect")],enums,this.getProperty("sizeBy","")))+"&nbsp;";
	    }

            if(filterBy.length>0) {
		var searchBar = "";
		var bottom = "";
		var dateIds = [];
		var hideFilterWidget = this.getProperty("hideFilterWidget",false, true);
		var widgetStyle = "";
		if(hideFilterWidget)
		    widgetStyle = "display:none;";
		var fieldMap = {};
                for(var i=0;i<filterBy.length;i++) {
                    var filterField  = this.getFieldById(fields,filterBy[i]);
		    if(!filterField) continue;
		    fieldMap[filterField.getId()] = {
			field: filterField,
			values:[],
		    };
		    
                    if(!filterField) continue;
                    this.filterFields.push(filterField);
                    var widget;
		    var widgetId = this.getFilterId(filterField.getId());
                    if(filterField.getType() == "enumeration") {
			var dfltValue = this.getProperty(filterField.getId() +".filterValue",FILTER_ALL);
			var filterValues = this.getProperty(filterField.getId()+".filterValues");
                        var enums = null;
			if (filterValues) {
			    var toks;
			    if ((typeof filterValues) == "string") {
				filterValues = Utils.getMacro(filterValues);
				toks = filterValues.split(",");
			    } else {
				toks = filterValues;
			    }
			    enums=[];
			    toks.map(tok=>{
				var tmp = tok.split(":");
				if(tmp.length>1) {
				    tok = [tmp[0],tmp[1]];
				}
				enums.push({value:tok});
			    })
			}

			var includeAll = this.getProperty(filterField.getId() +".includeAll",true);
			if(enums == null) {
			    var allName = this.getProperty(filterField.getId() +".allName","All");
			    enums = [];
			    var enumValues = [];
			    var imageField=this.getFieldOfType(null, "image");
			    var seen = {};
			    var valuesAreNumbers = true;
			    records.map(record=>{
				var value = this.getDataValues(record)[filterField.getIndex()];
				if(!seen[value]) {
				    seen[value]  = true;
				    var obj = {};
				    if(imageField)
					obj.image = this.getDataValues(record)[imageField.getIndex()];
				    if((+value+"") != value) valuesAreNumbers = false;
				    var label = value;
				    if(label.length>20) {
					label=  label.substring(0,19)+"...";
				    }
				    if(typeof value == "string")
					value = value.replace(/\'/g,"&apos;");
				    var tuple = [value, label];
				    obj.value = tuple;
				    enumValues.push(obj);
				}
			    });

			    enumValues.sort((a,b)  =>{
				a= a.value;
				b = b.value;
				if(valuesAreNumbers) {
				    return +a - +b;
				}
				    
				return (""+a[1]).localeCompare(""+b[1]);
			    });
			    if(includeAll) {
				enums.push({value:[FILTER_ALL,allName]});
			    }
			    for(var j=0;j<enumValues.length;j++) {
				var v = enumValues[j];
				enums.push(v);
			    }
			}
			var attrs= ["style",widgetStyle, "id",widgetId,"fieldId",filterField.getId()];
			if(this.getProperty(filterField.getId() +".filterMultiple",false)) {
			    attrs.push("multiple");
			    attrs.push("");
			    attrs.push("size");
			    attrs.push(this.getProperty(filterField.getId() +".filterMultipleSize","3"));
			    dfltValue = dfltValue.split(",");
			}
			if(this.getProperty(filterField.getId() +".filterShowButtons")) {
			    if(!includeAll && dfltValue == FILTER_ALL) dfltValue = enums[0].value;
			    var buttons = "";
			    var useImage = this.getProperty(filterField.getId() +".filterUseImage");
			    var imageAttrs = [];
			    var imageMap = Utils.getNameValue(this.getProperty(filterField.getId() +".filterImages"));
			    if(useImage) {
				var w = this.getProperty(filterField.getId() +".filterImageWidth");
				var h = this.getProperty(filterField.getId() +".filterImageHeight");
				if(h) {
				    imageAttrs.push("height");
				    imageAttrs.push(h);
				}
				if(w) {
				    imageAttrs.push("width");
				    imageAttrs.push(w);
				}
				if(!h && !w) {
				    imageAttrs.push("width");
				    imageAttrs.push("50");
				}

				imageAttrs.push("style");
				imageAttrs.push(this.getProperty(filterField.getId() +".filterImageStyle","border-radius:50%;"));
			    }
			    for(var j=0;j<enums.length;j++) {
				var extra = "";
				var v = enums[j].value;
				var label;
				if(Array.isArray(v)) {
				    label = v[1];
				    v = v[0];
				} else {
				    label = v;
				}
				if(v == dfltValue) extra = " display-filterby-button-selected ";
				if(v == FILTER_ALL) extra = " display-filterby-button-all ";
				if(useImage) {
				    var image=null;
				    if(imageMap) image = imageMap[v];
				    if(!image || image=="") image = enums[j].image;
				    if(image) {
					buttons+=HtmlUtils.div(["fieldId",filterField.getId(),"class","display-filterby-button display-filterby-button-image" + extra,"data-value",v,"title",label],
							       HtmlUtils.image(image,imageAttrs));
				    } else {
					buttons+=HtmlUtils.div(["fieldId",filterField.getId(),"class","display-filterby-button display-filterby-button-image" + extra,"data-value",v,"title",label],label);
				    }
				} else {
				    buttons+=HtmlUtils.div(["fieldId",filterField.getId(),"class","display-filterby-button  display-filterby-button-noimage" + extra,"data-value",v],label);
				}
				buttons+="\n";
			    }

			    if(useImage && this.getProperty(filterField.getId() +".filterShowButtonsLabel")) {
				buttons+=HtmlUtils.div(["class","display-filterby-button-label","id",this.getDomId("filterby_" + filterField.getId() +"_label")],"&nbsp;");
			    }
			    bottom+= HtmlUtils.div(["data-value",dfltValue,"class","display-filterby-buttons","id",widgetId,"isButton","true", "fieldId",
						    filterField.getId()], buttons);
			    continue;
			} else if(this.getProperty(filterField.getId() +".filterCheckbox")) {
			    attrs.push("isCheckbox");
			    attrs.push(true);
			    var tmp = [];
			    enums.map(e=>tmp.push(e.value));
			    var checked = tmp.includes(dfltValue);
			    if(tmp.length>0) {
				attrs.push("onValue");
				attrs.push(tmp[0]);
			    }
			    if(tmp.length>1) {
				attrs.push("offValue");
				attrs.push(tmp[1]);
			    }
			    widget = HtmlUtils.checkbox("",attrs,checked);
//			    console.log(widget);
			} else {
			    var tmp = [];
			    enums.map(e=>tmp.push(e.value));
                            widget = HtmlUtils.select("",attrs,tmp,dfltValue);
			}
		    } else if(filterField.isNumeric()) {
			var min=0;
			var max=0;
			var cnt=0;
			records.map(record=>{
			    var value = this.getDataValues(record)[filterField.getIndex()];
			    if(isNaN(value))return;
			    if(cnt==0) {min=value;max=value;}
			    else {
				min = Math.min(min, value);
				max = Math.max(max, value);
			    }
			    cnt++;
			});
			var dfltValueMin = this.getProperty(filterField.getId() +".filterValueMin",min);
			var dfltValueMax = this.getProperty(filterField.getId() +".filterValueMax",max);

                        widget = HtmlUtils.input("",dfltValueMin,["data-min",min,"class","display-filter-range display-filter-input","style",widgetStyle, "id",widgetId+"_min","size",5,"fieldId",filterField.getId()]);
			widget += " - ";
                        widget += HtmlUtils.input("",dfltValueMax,["data-max",max,"class","display-filter-range display-filter-input","style",widgetStyle, "id",widgetId+"_max","size",5,"fieldId",filterField.getId()]);
		    } else if(filterField.getType() == "date") {
                        widget =HtmlUtils.datePicker("","",["style",widgetStyle, "id",widgetId+"_date1"]) +" - " +
			    HtmlUtils.datePicker("","",["style",widgetStyle, "id",widgetId+"_date2"]);
			dateIds.push(widgetId+"_date1");
			dateIds.push(widgetId+"_date2");
                    } else {
			var dfltValue = this.getProperty(filterField.getId() +".filterValue","");
			var attrs =["style",widgetStyle, "id",widgetId,"fieldId",filterField.getId(),"class","display-filter-input"];
			var placeholder = this.getProperty(filterField.getId() +".filterPlaceholder");
			if(placeholder) {
			    attrs.push("placeholder");
			    attrs.push(placeholder);
			}
                        widget =HtmlUtils.input("",dfltValue,attrs);
			var values=fieldMap[filterField.getId()].values;
			var seen = {};
			records.map(record=>{
			    var value = this.getDataValues(record)[filterField.getIndex()];
			    if(!seen[value]) {
				seen[value] = true;
				values.push(value);
			    }
			});
                    }
		    var label =   this.getProperty(filterField.getId()+".filterLabel",filterField.getLabel());
		    if(!hideFilterWidget) {
			var tt = label;
			if(label.length>50) label = label.substring(0,49)+"...";
			if(!this.getProperty(filterField.getId() +".showFilterLabel",true)) {
			    label = "";
			}
			else
			    label = label+": ";
			widget = HtmlUtils.div(["style","display:inline-block;"], HtmlUtils.span(["class","display-filterby-label","title",tt], label) + widget);
		    }
		    //                    if(i==0) searchBar += "<br>Display: ";
		    
                    searchBar+=widget +(hideFilterWidget?"":"&nbsp;&nbsp;");
                }

		var style = (hideFilterWidget?"display:none;":"") + this.getProperty("filterByStyle","");
		header2+=HtmlUtils.span(["class","display-filterby","style",style,"id",this.getDomId(ID_FILTERBAR)],searchBar+bottom);
	    }

	    this.jq(ID_HEADER2).html(header2);
	    var theDisplay = this;

            if(filterBy.length>0) {
		if(!hideFilterWidget) {
		    for(var i=0;i<dateIds.length;i++) {
			HtmlUtils.datePickerInit(dateIds[i]);
		    }
		}

		var inputFunc = function(input, input2, value){
                    var id = input.attr("id");
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
		    
		    if(!value) return;
		    if(!Array.isArray(value) && input.attr("isButton")) {
//			console.log(_this.type +" " +Array.isArray(value));
			var tmp = [];
			value.split(",").map(v=>{
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
		    var args = {
			property: "filterValue",
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

		this.jq(ID_FILTERBAR).find(".display-filterby-buttons").each(function(){
		    let parent = $(this);
		    $(this).find(".display-filterby-button").click(function(event){
			var isAll = $(this).hasClass("display-filterby-button-all");
			var wasSelected = $(this).hasClass("display-filterby-button-selected");
			var fieldId = $(this).attr("fieldId");
			var multiples = _this.getProperty(fieldId +".filterMultiple",false);
			if(!event.metaKey || isAll || !multiples) {
			    parent.find(".display-filterby-button").removeClass("display-filterby-button-selected");
			} else {
			    parent.find(".display-filterby-button-all").removeClass("display-filterby-button-selected");
			}
			if(wasSelected  && event.metaKey) {
			    $(this).removeClass("display-filterby-button-selected");
			} else {
			    $(this).addClass("display-filterby-button-selected");
			}
			var values = [];
			parent.find(".display-filterby-button-selected").each(function() {
			    values.push($(this).attr("data-value").replace(/,/g,"_comma_"));
			});
			if(values.length==0) {
			    parent.find(".display-filterby-button-all").addClass("display-filterby-button-selected");
			    values.push(FILTER_ALL);
			}
			var value =  Utils.join(values,",");
			parent.attr("data-value", value);
			$("#"+parent.attr("id") +"_label").html(values.includes(FILTER_ALL)?"&nbsp;":value);
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
		    var regexp = new RegExp("(" + val+")",'i');
		    for(var i=0;i<values.length;i++) {
			var match  = values[i].match(regexp);
			if(match) {
			    items.push([match[1], values[i]]);
			}
			if(items.length>30) break;
		    }
		    if(items.length>0) {
			var html = "";
			var itemCnt = 0;
			items.map(item=>{
			    var match = item[0];
			    item =  item[1];
			    if(item.length>50) return;
			    var label = item.replace(regexp,"<span style='background:" + HIGHLIGHT_COLOR +";'>" + match +"</span>");
			    item = item.replace(/\'/g,"\'");
			    html+=HtmlUtils.div(["class","display-filterby-popup-item","item",item],label)+"\n";
			    itemCnt++;
			});	
			if(itemCnt>0) {
			    popupObject = getTooltip();
			    popupObject.html(HtmlUtils.div(["class", "ramadda-popup-inner ramadda-snippet-popup"], html));
			    popupObject.show();
			    popupObject.position({
				of: $(this),
				my: "left top",
				at: "left bottom",
			    });
			    $(".display-filterby-popup-item").click(function(){
				hidePopupObject();
				input.val($(this).attr("item"));
				inputFunc(input);
			    });
			}
		    }

		});



                this.jq(ID_FILTERBAR).find(".display-filter-range").mousedown(function(){
		    var id = $(this).attr("id");
		    id = id.replace(/_min$/,"").replace(/_max$/,"");
		    var min = $("#" + id+"_min");
		    var max = $("#" + id+"_max");
		    range = {min: parseFloat(min.attr("data-min")),
			     max: parseFloat(max.attr("data-max"))};
		    var minValue = parseFloat(min.val());
		    var maxValue = parseFloat(max.val());
		    var html = HtmlUtils.div(["id","filterby-range","style","width:200px;"],"");
		    var popup = getTooltip();
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
		    $( "#filterby-range" ).slider({
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
			},
			stop: function() {
			    var popup = getTooltip();
			    popup.hide();
			    theDisplay.checkFilterField(max);
			    inputFunc(min,max);
			}
		    });
		});


		HtmlUtils.initSelect(this.jq("colorbyselect"));
		HtmlUtils.initSelect(this.jq("sizebyselect"));
		HtmlUtils.initSelect(this.jq("chartfields"));

                this.jq("chartfields").change(function(){
		    _this.setProperty("fields",$(this).val());
		    _this.updateUI();
		});
                this.jq("colorbyselect").change(function(){
		    _this.colorByFieldChanged($(this).val());
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
//		HtmlUtils.initSelect(this.jq(ID_FILTERBAR).find("select"));
		this.jq(ID_FILTERBAR).find("input:radio,select").change(function() {
		    inputFunc($(this));
		});
		this.jq(ID_FILTERBAR).find("input:checkbox").change(function() {
		    inputFunc($(this));
		});
            }
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
	colorByFieldChanged:function(field) {
	},

	sizeByFieldChanged:function(field) {
	},
	dataFilterChanged: function() {
	    this.updateUI();
	},
        updateUI: function() {
	},

	makeTooltips: function(selector, records, callback) {
	    var tooltip = this.getProperty("tooltip");
	    if(!tooltip) return;
	    let _this = this;
	    selector.tooltip({
		content: function() {
		    var record = records[parseFloat($(this).attr('recordIndex'))];
		    if(callback) callback(true, record);
		    _this.getDisplayManager().notifyEvent("handleEventRecordHighlight", _this, {highlight:true,record: record});
		    return _this.getRecordHtml(record,null,tooltip);
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
	animationStart: function(animation) {
	    
	},
	animationApply: function(animation, skipUpdateUI) {
	    this.minDateObj = animation.begin;
	    this.maxDateObj = animation.end;
	    if(!skipUpdateUI) {
		this.haveCalledUpdateUI = false;
//		var t1 = new Date();
		this.dataFilterChanged();
//		var t2 = new Date();
//		Utils.displayTimes("timeChanged",[t1,t2]);
	    }
	    this.propagateEvent("handleEventPropertyChanged", {
		property: "dateRange",
		minDate: animation.begin,
		maxDate: animation.end
	    });
	},
        getDoBs: function() {
            if (!(typeof this.dobs === 'undefined')) {
                return dobs;
            }
            if (this.displayParent) {
                return this.displayParent.getDoBs();

            }
            return false;
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
        cnt: 0,
        getHtml: function() {
            var dobs = this.getDoBs();
            var html = "";
            var menu = HtmlUtils.div([ATTR_CLASS, "display-dialog", ATTR_ID, this.getDomId(ID_DIALOG)], "");
            html += menu;
            html += HtmlUtils.div([ATTR_CLASS, "ramadda-popup", ATTR_ID, this.getDomId(ID_MENU_OUTER)], "");
            var width = this.getWidth();
            if (dobs) {
                html += HtmlUtils.openDiv(["class", "minitron"]);
            }
            var style = this.getProperty("displayStyle", "");
            if (width > 0) {
                style += "width:" + width + "px;"
            }
            html += HtmlUtils.openDiv(["class", "display-contents", "style", style]);
            var get = this.getGet();
            var button = "";
            if (this.getShowMenu()) {
                button = HtmlUtils.onClick(get + ".showDialog();",
					   HtmlUtils.image(ramaddaBaseUrl + "/icons/downdart.png",
							   [ATTR_CLASS, "display-dialog-button", ATTR_ID, this.getDomId(ID_DIALOG_BUTTON)]));
            }
            var title = "";
            if (this.getShowTitle()) {
                title = this.getTitle(false).trim();
            }

            var left = "";
            if (button != "" || title != "") {
                this.cnt++;
                var titleDiv = this.getTitleHtml(title);
                if (button == "") {
                    left = titleDiv;
                } else {
                    left = "<div class=display-header>" + button + "&nbsp;" + titleDiv + "</div>";
                }
            }
            left = HtmlUtils.div(["id", this.getDomId(ID_TOP_LEFT)], left);
            var right = HtmlUtils.div(["id", this.getDomId(ID_TOP_RIGHT)], "");
            html += HtmlUtils.div(["id",this.getDomId(ID_HEADER1),"class","display-header1"], "");
            html += HtmlUtils.div(["id",this.getDomId(ID_HEADER2),"class","display-header2"], "");
            html += HtmlUtils.leftRightTable(left, right, {
                valign: "bottom"
            });
            html += HtmlUtils.div(["id",this.getDomId(ID_HEADER3),"class","display-header3"], "");
            var contents = this.getContentsDiv();
            html += contents;
            html += HtmlUtils.closeTag(TAG_DIV);
            if (dobs) {
                html += HtmlUtils.closeTag(TAG_DIV);
            }
	    html += HtmlUtils.div([ATTR_CLASS, "", ATTR_ID, this.getDomId(ID_BOTTOM)],
				  HtmlUtils.div(["id",this.getDomId(ID_COLORTABLE)]));

            return html;
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
                var inner = HtmlUtils.getIconImage(images[i], [ATTR_TITLE, labels[i], ATTR_CLASS, "display-dialog-header-icon"]);
                if (addLabel) inner += " " + labels[i] + "<br>";
                toolbar += HtmlUtils.onClick(calls[i], inner);
            }
            return toolbar;
        },
	getWikiEditorTags: function() {
	    return  [
		"label:Display Attributes",
		"showMenu=\"true\"",	      
		"showTitle=\"true\"",
		"layoutHere=\"true\"",
		"width=\"100%\"",
		"height=\"400\"",
		"title=\"\"",
		"titleBackground=\"color\"",
		"textColor=\"color\"",
		"backgroundImage=\"\"",
		"background=\"color\"",
		"label:Filter Attributes",
		"filterFields=\"\"",
		"hideFilterWidget=true",
		"acceptFilterEvent=false",
		"&lt;field&gt;.filterValue=\"\"",
		"&lt;field&gt;.filterValues=\"\"",
		"&lt;field&gt;.filterMultiple=\"true\"",
		"&lt;field&gt;.filterMultipleSize=\"5\"",
		"&lt;field&gt;.filterByStyle=\"background:white;\"",
		"&lt;field&gt;.includeAll=false",
		"&lt;field&gt;.filterStartsWith=\"true\"",
		"binDate=\"day|month|year\"",
		"label:Color Attributes",
		"colorTable=\"\"",
		"colors=\"color1,...,colorN\"",
		"colorBy=\"\"",
		"colorByFields=\"\"",
		"colorTableAlpha=\"0.5\"",
		"colorByMin=\"value\"",
		"colorByMax=\"value\"",
		"inlinelabel:Animation Attributes",
		"doAnimation=true",
		"acceptDateRangeChange=true",
		"animationDateFormat=\"yyyy\"",
		"animationWindow=\"decade|halfdecade|year|month|week|day|hour|minute\"",
		"animationMode=\"sliding\"",
		"animationShowSlider=\"true|false\","

	    ];
        },
        makeDialog: function() {
            var html = "";
            html += HtmlUtils.div([ATTR_ID, this.getDomId(ID_HEADER), ATTR_CLASS, "display-header"]);
            var closeImage = HtmlUtils.getIconImage(icon_close, []);
            var close = HtmlUtils.onClick("$('#" + this.getDomId(ID_DIALOG) + "').hide();", closeImage);
            var right = close;
            var left = "";
            //                var left = this.makeToolbar({addLabel:true});
            var header = HtmlUtils.div([ATTR_CLASS, "display-dialog-header"], HtmlUtils.leftRight(left, right));
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
                tabLinks += HtmlUtils.tag("li", [], HtmlUtils.tag("a", ["href", "#" + id],
								  tabTitles[i]));
                tabLinks += "\n";
                var contents = HtmlUtils.div([ATTR_CLASS, "display-dialog-tab"], tabContents[i]);
                tabs += HtmlUtils.div(["id", id], contents);
                tabs += "\n";
            }
            tabLinks += "</ul>\n";
            var tabs = HtmlUtils.div(["id", this.getDomId(ID_DIALOG_TABS)], tabLinks + tabs);
            dialogContents = header + tabs;
            return dialogContents;
        },
        initDialog: function() {
            var _this = this;
            var updateFunc = function(e) {
                if (e && e.which != 13) {
                    return;
                }
                _this.column = _this.jq("column").val();
                _this.row = _this.jq("row").val();
                _this.getLayoutManager().doLayout();
            };
            this.jq("column").keypress(updateFunc);
            this.jq("row").keypress(updateFunc);
            this.jq("showtitle").change(function() {
                _this.setShowTitle(_this.jq("showtitle").is(':checked'));
            });
            this.jq("showdetails").change(function() {
                _this.setShowDetails(_this.jq("showdetails").is(':checked'));
            });
            this.jq(ID_DIALOG_TABS).tabs();

        },
        showDialog: function() {
            var dialog = this.getDomId(ID_DIALOG);
            this.writeHtml(ID_DIALOG, this.makeDialog());
            this.popup(this.getDomId(ID_DIALOG_BUTTON), dialog);
            this.initDialog();
        },
        getWidthForStyle: function(dflt) {
            var width = this.getProperty("width", -1);
            if (width == -1) return dflt;
            if (!width.endsWith("px") && !width.endsWith("%"))
                width = width + "px";
            return width;
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
            var width = this.getWidthForStyle();
            if (width) {
                style += " width:" + width + ";";
            }
            return style;
        },
        getContentsDiv: function() {
            var style = this.getContentsStyle();
            style += this.getProperty("contentsStyle", "");
            var image = this.getProperty("backgroundImage");
            if (image) {
                image = HtmlUtils.getEntryImage(this.entryId, image);
                style += "background-attachment:auto;background-size:100% auto; background-image: url('" + image + "'); ";
            }
            var background = this.getProperty("background");
            if (background)
                style += "background: " + background + ";";
            var topBottomStyle = "";
            var width = this.getWidthForStyle();
            if (width) {
                topBottomStyle += " width:" + width + ";";
            }
            var top = HtmlUtils.div([ATTR_STYLE, topBottomStyle, ATTR_ID, this.getDomId(ID_DISPLAY_TOP)], "");
            var bottom = HtmlUtils.div([ATTR_STYLE, topBottomStyle, ATTR_ID, this.getDomId(ID_DISPLAY_BOTTOM)], "");

            var contents =  top + "\n" +HtmlUtils.div([ATTR_CLASS, "display-contents-inner display-" + this.type, "style", style, ATTR_ID, this.getDomId(ID_DISPLAY_CONTENTS)], "") + "\n" +bottom;
            return contents;
        },

        copyDisplay: function() {
            var newOne = {};
            $.extend(true, newOne, this);
            newOne.setId(newOne.getId() + this.getUniqueId("display"));
            addRamaddaDisplay(newOne);
            this.getDisplayManager().addDisplay(newOne);
        },
        removeDisplay: function() {
            this.getDisplayManager().removeDisplay(this);
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
        getWidth: function() {
            return this.getFormValue("width", 0);
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
            var theDisplay = this;
            var jsonUrl = this.getRamadda().getSearchUrl(searchSettings, OUTPUT_JSON);
            var handler = {
                entryListChanged: function(entryList) {
                    theDisplay.doneQuickEntrySearch(entryList, callback);
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
            this.dataCollection.addData(pointData);
            var entry = pointData.entry;
            if (entry == null) {
                await this.getRamadda().getEntry(pointData.entryId, e => {
                    entry = e
                });
            }
            if (entry) {
                pointData.entry = entry;
                this.addEntry(entry);
            }
        },
        pointDataLoadFailed: function(data) {
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
                msg = "<b>Sorry, but an error has occurred:</b>";
                if (!data) data = "No data returned from server";
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
                error = HtmlUtils.tag("pre", ["style", "max-height:300px;overflow-y:auto;max-width:100%;overflow-x:auto;"], error);
                msg += error;
            }
            this.setContents(this.getMessage(msg));
        },
        //callback from the pointData.loadData call
        clearCache: function() {},

        pointDataLoaded: function(pointData, url, reload) {
            this.inError = false;
            this.clearCache();
            if (!reload) {
                this.addData(pointData);
                this.checkSearchBar();
            }
            if (url != null) {
                this.jsonUrl = url;
            } else {
                this.jsonUrl = null;
            }

            /**
               This makes the date error in makeDataTable. not sure why
               var records = pointData.getRecords();
               var allFields = this.getData().getRecordFields();
               var fields = this.getSelectedFields(allFields);
               if (fields.length == 0)
               fields = allFields;
            **/

            if (!this.getDisplayReady()) {
                return;
            }
            this.updateUI();
            if (!reload) {
                this.lastPointData = pointData;
                this.propagateEvent("handleEventPointDataLoaded", pointData);
            }
        },
        getDateFormatter: function() {
            var date_formatter = null;
            if (this.googleLoaded()) {
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
        dateInRange: function(date) {
            if (date != null) {
                if (this.minDateObj != null && date.getTime() < this.minDateObj.getTime()) {
                    return false;
                }
                if (this.maxDateObj != null && date.getTime() > this.maxDateObj.getTime()) {
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
        getStandardData: function(fields, args) {
	    this.recordToIndex = {};
	    this.indexToRecord = {};
            var pointData = this.getPointData();
            var excludeZero = this.getProperty(PROP_EXCLUDE_ZERO, false);
            if (fields == null) {
                fields = pointData.getRecordFields();
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



            var groupByIndex = props.groupByIndex;
            var groupByList = [];

            var dataList = [];
            //The first entry in the dataList is the array of names
            //The first field is the domain, e.g., time or index
            var fieldNames = [];

	    if(this.getProperty("binDate")) {
		var binType = this.getProperty("binType","total");
		var binCount = binType=="count";
		if(binCount) {
		    var f = [];
		    fields.map((field)=>{
			f.push(new RecordField({
			    index:0,
			    id:field.getId(),
			    label:this.getProperty("binCountLabel","Count"),
			    type:"double",
			    chartable:true
			}));
		    });
		    fields=f;
		}
	    }

            for (i = 0; i < fields.length; i++) {
                var field = fields[i];
                if (field.isFieldNumeric() && field.isFieldDate()) {
                    //                        console.log("Skipping:" + field.getLabel());
                    //                        continue;
                }
                var name = field.getLabel();
                if (field.getUnit() != null) {
                    name += " (" + field.getUnit() + ")";
                }
                //                    name = name.replace(/!!/g,"<br><hr>&nbsp;&nbsp;&nbsp;")
                name = name.replace(/!!/g, " -- ")
                fieldNames.push(name);
            }
            if (props.makeObject) {
                dataList.push({
                    tuple: fieldNames,
                    record: null
                });
            } else {
                dataList.push(fieldNames);
            }
            //console.log(fieldNames);


            groupByList.push("");
            //These are Record objects 


	    if(!this.minDateObj)
		this.minDateObj = Utils.parseDate(this.minDate, false);
	    if(!this.minDateObj)
		this.maxDateObj = Utils.parseDate(this.maxDate, true, this.minDateObj);

            if (this.minDateObj == null && this.maxDateObj != null) {
                this.minDateObj = Utils.parseDate(this.minDate, false, this.maxDateObj);
            }


            var minDate = (this.minDateObj != null ? this.minDateObj.getTime() : -1);
            var maxDate = (this.maxDateObj != null ? this.maxDateObj.getTime() : -1);
            if (this.minDateObj != null || this.maxDateObj != null) {
                //                    console.log("dates: "  + this.minDateObj +" " + this.maxDateObj);
            }


            var offset = 0;
            if (Utils.isDefined(this.offset)) {
                offset = parseFloat(this.offset);
            }

            var nonNullRecords = 0;
            var records = this.filterData();
            var allFields = pointData.getRecordFields();

            //Check if there are dates and if they are different
            this.hasDate = this.getHasDate(records);
            var date_formatter = this.getDateFormatter();
            var rowCnt = -1;
            var indexField = this.getFieldById(null,this.getProperty("indexField"));
            for (j = 0; j < records.length; j++) {
                var record = records[j];
                var date = record.getDate();
                if (!this.dateInRange(date)) {
		    continue;
		}
                rowCnt++;
		this.recordToIndex[record.getId()] = rowCnt;
		this.indexToRecord[rowCnt] = record;
                var values = [];
                if (props && (props.includeIndex || props.includeIndexIfDate)) {
                    var indexName = null;
                    if (indexField) {
                        values.push(record.getValue(indexField.getIndex()) + offset);
                        indexName = indexField.getLabel();
                    } else {
                        if (this.hasDate) {
                            //                                console.log(this.getDateValue(date, date_formatter));
                            values.push(this.getDateValue(date, date_formatter));
                            indexName = "Date";
                        } else {
                            if (!props.includeIndexIfDate) {
                                values.push(j);
                                indexName = "Index";
                            }
                        }
                    }
                    if (indexName != null && rowCnt == 0) {
                        fieldNames.unshift(indexName);
                    }
                }




                var allNull = true;
                var allZero = true;
                var hasNumber = false;
                for (var i = 0; i < fields.length; i++) {
                    var field = fields[i];
                    if (field.isFieldNumeric() && field.isFieldDate()) {
                        //                            continue;
                    }
                    var value = record.getValue(field.getIndex());
		    //		    if(j<10)
		    //			console.log(field.getId() +" = " + value);
                    if (offset != 0) {
                        value += offset;
                    }
                    if (value != null) {
                        allNull = false;
                    }
                    if (typeof value == 'number') {
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

                if (hasNumber && allZero && excludeZero) {
		    //		    console.log(" skipping due to zero: " + values);
                    continue;
                }
                if (this.filters != null) {
                    if (!this.applyFilters(record, values)) {
                        console.log(" skipping due to filters");
                        continue;
                    }
                }
                //TODO: when its all null values we get some errors
                if (groupByIndex >= 0) {
                    groupByList.push(record.getValue(groupByIndex));
                }
                if (props.makeObject)
                    dataList.push({
                        tuple: values,
                        record: record
                    });
                else
                    dataList.push(values);
                //                    console.log("values:" + values);
                if (!allNull) {
                    nonNullRecords++;
                }
	    }

            if (nonNullRecords == 0) {
		//		console.log("Num non null:" + nonNullRecords);
                return [];
            }


            if (groupByIndex >= 0) {
                var groupToTuple = {};
                var groups = [];
                var agg = [];
                var title = [];
                title.push(props.groupByField.getLabel());
                for (var j = 0; j < fields.length; j++) {
                    var field = fields[j];
                    if (field.getIndex() != groupByIndex) {
                        title.push(field.getLabel());
                    }
                }
                agg.push(title);

                for (var rowIdx = 0; rowIdx < dataList.length; rowIdx++) {
                    var data = this.getDataValues(dataList[rowIdx]);
                    if (rowIdx == 0) {
                        continue;
                    }
                    var groupBy = groupByList[rowIdx];
                    var tuple = groupToTuple[groupBy];
                    if (tuple == null) {
                        groups.push(groupBy);
                        tuple = new Array();
                        agg.push(tuple);
                        tuple.push(groupBy);
                        for (var fieldIdx = 0; fieldIdx < fields.length; fieldIdx++) {
                            var field = fields[fieldIdx];
                            if (field.getIndex() == groupByIndex) {
                                continue;
                            }
                            tuple.push(0);
                        }
                        groupToTuple[groupBy] = tuple;
                    }
                    var index = 0;
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
                return agg;
            }

            return dataList;
        },
        googleLoaded: function() {
            if ((typeof google === 'undefined') || (typeof google.visualization === 'undefined') || (typeof google.visualization.DateFormat === 'undefined')) {
                return false;
            }
            return true;
        },
        initDateFormats: function() {
            if (!this.googleLoaded()) {
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
                if (!this.filters[i].recordOk(this, record, values)) {
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
    var LAYOUT_TABLE = "table";
    var LAYOUT_HTABLE = "htable";
    var LAYOUT_TABS = "tabs";
    var LAYOUT_COLUMNS = "columns";
    var LAYOUT_ROWS = "rows";
    if (!type) type = "group";
    let SUPER = new RamaddaDisplay(argDisplayManager, argId, type, argProperties);
    RamaddaUtil.inherit(this, SUPER);
    RamaddaUtil.defineMembers(this, {
        layout: this.getProperty(PROP_LAYOUT_TYPE, LAYOUT_TABLE)
    });

    RamaddaUtil.defineMembers(this, {
        displays: [],
        layout: this.getProperty(PROP_LAYOUT_TYPE, LAYOUT_TABLE),
        columns: this.getProperty(PROP_LAYOUT_COLUMNS, 1),
        isLayoutColumns: function() {
            return this.layout == LAYOUT_COLUMNS;
        },
        getDoBs: function() {
            return this.dobs;
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
            var wiki = "";
            wiki += "<div id=\"{{entryid}}_maindiv\"></div>\n\n";
            wiki += "{{group " + HtmlUtils.attrs(attrs) + "}}\n\n"
            return wiki;
        },

        walkTree: function(func, data) {
            for (var i = 0; i < this.displays.length; i++) {
                var display = this.displays[i];
                if (display.walkTree != null) {
                    display.walkTree(func, data);
                } else {
                    func.call(data, display);
                }
            }
        },
        collectEntries: function(entries) {
            if (entries == null) entries = [];
            for (var i = 0; i < this.displays.length; i++) {
                var display = this.displays[i];
                if (display.collectEntries != null) {
                    display.collectEntries(entries);
                } else {
                    var displayEntries = display.getEntries();
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
            for (var i = 0; i < this.displays.length; i++) {
                var display = this.displays[i];
                if (display.getPosition) {
                    return display.getPosition();
                }
            }
        },
        getDisplays: function() {
            return this.display;
        },
        notifyEvent: function(func, source, data) {
            var displays = this.getDisplays();
            for (var i = 0; i < this.displays.length; i++) {
                var display = this.displays[i];
                if (display == source) {
                    continue;
                }
                var eventSource = display.getEventSource();
                if (eventSource != null && eventSource.length > 0) {
                    if (eventSource != source.getId() && eventSource != source.getName()) {
                        continue;
                    }
                }
                display.notifyEvent(func, source, data);
            }
        },
        getDisplaysToLayout: function() {
            var result = [];
            for (var i = 0; i < this.displays.length; i++) {
                if (this.displays[i].getIsLayoutFixed()) {
		    continue;
		}
                result.push(this.displays[i]);
            }
            return result;
        },
        pageHasLoaded: function(display) {
            for (var i = 0; i < this.displays.length; i++) {
                this.displays[i].setDisplayReady(true);
            }
            this.doLayout();
        },
        addDisplay: function(display) {
            this.displays.push(display);
            if (Utils.getPageLoaded()) {
                this.doLayout();
            }
        },
        layoutChanged: function(display) {
            this.doLayout();
        },
        removeDisplay: function(display) {
            var index = this.displays.indexOf(display);
            if (index >= 0) {
                this.displays.splice(index, 1);
            }
            this.doLayout();
        },
        doLayout: function() {
	    var html = "";
            var colCnt = 100;
            var displaysToLayout = this.getDisplaysToLayout();
            var displaysToPrepare = this.displays;


            for (var i = 0; i < displaysToPrepare.length; i++) {
                var display = displaysToPrepare[i];
                if (display.prepareToLayout != null) {
                    display.prepareToLayout();
                }
            }

            var weightIdx = 0;
            var weights = null;
            if (typeof this.weights != "undefined") {
                weights = this.weights.split(",");
            }

            for (var i=0; i < displaysToLayout.length; i++) {
                var divId = HtmlUtils.getUniqueId("divid_");
                var div =  HtmlUtils.div(["class", " display-wrapper","id",divId],"");
                displaysToLayout[i].setProperty(PROP_DIVID,divId);
                displaysToLayout[i].layoutDiv=div;
            }
            if (this.layout == LAYOUT_TABLE) {
                if  (displaysToLayout.length== 1) {
                    html += displaysToLayout[0].layoutDiv;
                } else {
                    var weight = 12 / this.columns;
                    var i = 0;
                    var map = {};
                    for (; i < displaysToLayout.length; i++) {
                        var d = displaysToLayout[i];
                        if (Utils.isDefined(d.column) && Utils.isDefined(d.row) && d.columns >= 0 && d.row >= 0) {
                            var key = d.column + "_" + d.row;
                            if (map[key] == null) map[key] = [];
                            map[key].push(d);
                        }
                    }

                    i = 0;
                    for (; i < displaysToLayout.length; i++) {
                        colCnt++;
                        if (colCnt >= this.columns) {
                            if (i > 0) {
                                html += HtmlUtils.closeTag(TAG_DIV);
                            }
                            html += HtmlUtils.openTag("div", ["class", "row"]);
                            colCnt = 0;
                        }
                        var weightToUse = weight;
                        if (weights != null) {
                            if (weightIdx >= weights.length) {
                                weightIdx = 0;
                            }
                            weightToUse = weights[weightIdx];
                            weightIdx++;
                        }
                        html += HtmlUtils.div(["class", "col-md-" + weightToUse + " display-wrapper display-cell"], displaysToLayout[i].layoutDiv);
			html+="\n";
		    }

                    if (i > 0) {
                        html += HtmlUtils.closeTag(TAG_DIV);
                    }
                }
	    } else if (this.layout == LAYOUT_HTABLE) {
                if  (displaysToLayout.length== 1) {
                    html += displaysToLayout[0].layoutDiv;
                } else {
                    var percent = Math.round((100 / this.columns))+"%";
                    var i = 0;
		    html+="<table width=100%>\n";
		    var colCnt = 100;
                    for (var i =0;i < displaysToLayout.length; i++) {
                        colCnt++;
                        if (colCnt >= this.columns) {
                            if (i > 0) {
                                html += HtmlUtils.closeTag(TAG_TR);
                            }
                            html += HtmlUtils.openTag("tr", ["valign", "top"]);
			    html+="\n";
                            colCnt = 0; 
                        }
                        html += HtmlUtils.td(["width",percent], displaysToLayout[i].layoutDiv);
			html+="\n";
		    }
                    if (i > 0) {
                        html += HtmlUtils.closeTag("tr");
                    }
                }
            } else if (this.layout == LAYOUT_TABS) {
                var tabId = HtmlUtils.getUniqueId("tabs_");
                html += HtmlUtils.openTag(TAG_DIV, ["id", tabId, "class", "ui-tabs"]);
                html += HtmlUtils.openTag(TAG_UL, []);
                var hidden = "";
                var cnt = 0;
                for (var i = 0; i < displaysToLayout.length; i++) {
                    var display = displaysToLayout[i];
                    var label = display.getTitle(false);
                    if (label.length > 20) {
                        label = label.substring(0, 19) + "...";
                    }
                    html += HtmlUtils.tag(TAG_LI, [], HtmlUtils.tag(TAG_A, ["href", "#" + tabId + "-" + cnt], label));
                    hidden += HtmlUtils.div(["id", tabId + "-" + cnt, "class", "ui-tabs-hide"], display.layoutDiv);
                    cnt++;
                }
                html += HtmlUtils.closeTag(TAG_UL);
                html += hidden;
                html += HtmlUtils.closeTag(TAG_DIV);
            } else if (this.layout == LAYOUT_ROWS) {
                var rows = [];
                for (var i = 0; i < displaysToLayout.length; i++) {
                    var display = displaysToLayout[i];
                    var row = display.getRow();
                    if (("" + row).length == 0) row = 0;
                    while (rows.length <= row) {
                        rows.push([]);
                    }
                    rows[row].push(display.layoutDiv);
                }
                for (var i = 0; i < rows.length; i++) {
                    var cols = rows[i];
                    var width = Math.round(100 / cols.length) + "%";
                    html += HtmlUtils.openTag(TAG_TABLE, ["border", "0", "width", "100%", "cellpadding", "0", "cellspacing", "0"]);
                    html += HtmlUtils.openTag(TAG_TR, ["valign", "top"]);
                    for (var col = 0; col < cols.length; col++) {
                        var cell = cols[col];
                        cell = HtmlUtils.div(["class", "display-cell"], cell);
                        html += HtmlUtils.tag(TAG_TD, ["width", width], cell);
                    }
                    html += HtmlUtils.closeTag(TAG_TR);
                    html += HtmlUtils.closeTag(TAG_TABLE);
                }
            } else if (this.layout == LAYOUT_COLUMNS) {
                var cols = [];
                for (var i = 0; i < displaysToLayout.length; i++) {
                    var display = displaysToLayout[i];
                    var column = display.getColumn();
                    //                        console.log("COL:" + column);
                    if (("" + column).length == 0) column = 0;
                    while (cols.length <= column) {
                        cols.push([]);
                    }
                    cols[column].push(display.layoutDiv);
                    //                        cols[column].push("HTML");
                }
                html += HtmlUtils.openTag(TAG_DIV, ["class", "row"]);
                var width = Math.round(100 / cols.length) + "%";
                var weight = 12 / cols.length;
                for (var i = 0; i < cols.length; i++) {
                    var rows = cols[i];
                    var contents = "";
                    for (var j = 0; j < rows.length; j++) {
                        contents += rows[j];
                    }
                    var weightToUse = weight;
                    if (weights != null) {
                        if (weightIdx >= weights.length) {
                            weightIdx = 0;
                        }
                        weightToUse = weights[weightIdx];
                        weightIdx++;
                    }
                    html += HtmlUtils.div(["class", "col-md-" + weightToUse], contents);
                }
                html += HtmlUtils.closeTag(TAG_DIV);
                //                    console.log("HTML:" + html);

            } else {
                html += "Unknown layout:" + this.layout;
            }

            this.writeHtml(ID_DISPLAYS, html);
            if (this.layout == LAYOUT_TABS) {
                $("#" + tabId).tabs({activate: HtmlUtil.tabLoaded});
            }
            this.initDisplays();
        },
        initDisplays: function() {
            for (var i = 0; i < this.displays.length; i++) {
                try {
                    this.displays[i].initDisplay();
                } catch (e) {
                    this.displays[i].displayError("Error creating display:<br>" + e);
                    console.log("error creating display: " + this.displays[i].getType());
                    console.log(e.stack)
                }
            }
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
            var v = prompt("Minimum axis value", "0");
            if (v != null) {
                v = parseFloat(v);
                for (var i = 0; i < this.displays.length; i++) {
                    var display = this.displays[i];
                    if (display.setMinZAxis) {
                        display.setMinZAxis(v);
                    }
                }
            }
        },

        askMaxZAxis: function() {
            var v = prompt("Maximum axis value", "0");
            if (v != null) {
                v = parseFloat(v);
                for (var i = 0; i < this.displays.length; i++) {
                    var display = this.displays[i];
                    if (display.setMaxZAxis) {
                        display.setMaxZAxis(v);
                    }
                }
            }
        },

        askMinDate: function() {
            var d = this.minDate;
            if (!d) d = "1950-0-0";
            this.minDate = prompt("Minimum date", d);
            if (this.minDate != null) {
                for (var i = 0; i < this.displays.length; i++) {
                    var display = this.displays[i];
                    if (display.setMinDate) {
                        display.setMinDate(this.minDate);
                    }
                }
            }
        },

        askMaxDate: function() {
            var d = this.maxDate;
            if (!d) d = "2020-0-0";
            this.maxDate = prompt("Maximum date", d);
            if (this.maxDate != null) {
                for (var i = 0; i < this.displays.length; i++) {
                    var display = this.displays[i];
                    if (display.setMaxDate) {
                        display.setMaxDate(this.maxDate);
                    }
                }
            }
        },


        titlesOff: function() {
            for (var i = 0; i < this.displays.length; i++) {
                this.displays[i].setShowTitle(false);
            }
        },
        titlesOn: function() {
            for (var i = 0; i < this.displays.length; i++) {
                this.displays[i].setShowTitle(true);
            }
        },
        detailsOff: function() {
            for (var i = 0; i < this.displays.length; i++) {
                this.displays[i].setShowDetails(false);
            }
            this.doLayout();
        },
        detailsOn: function() {
            for (var i = 0; i < this.displays.length; i++) {
                this.displays[i].setShowDetails(true);
            }
            this.doLayout();
        },

        deleteAllDisplays: function() {
            this.displays = [];
            this.doLayout();
        },
        moveDisplayUp: function(display) {
            var index = this.displays.indexOf(display);
            if (index <= 0) {
                return;
            }
            this.displays.splice(index, 1);
            this.displays.splice(index - 1, 0, display);
            this.doLayout();
        },
        moveDisplayDown: function(display) {
            var index = this.displays.indexOf(display);
            if (index >= this.displays.length) {
                return;
            }
            this.displays.splice(index, 1);
            this.displays.splice(index + 1, 0, display);
            this.doLayout();
        },

        moveDisplayTop: function(display) {
            var index = this.displays.indexOf(display);
            if (index >= this.displays.length) {
                return;
            }
            this.displays.splice(index, 1);
            this.displays.splice(0, 0, display);
            this.doLayout();
        },


    });

}


function DisplayAnimation(display) {
    var ID_RUN = "animrun";
    var ID_NEXT = "animnext";
    var ID_PREV= "animprev";
    var ID_BEGIN= "animbegin";
    var ID_END= "animend";
    var ID_SLIDER = "slider";
    var ID_TICKS = "ticks";
    var ID_SHOWALL = "showall";
    var ID_ANIMATION_LABEL = "animationlabel";
    this.display = display;
    $.extend(this,{
        running: false,
        inAnimation: false,
        begin: null,
        end: null,
        dateMin: null,
        dateMax: null,
        dateRange: 0,
        dateFormat: display.getProperty("animationDateFormat", "yyyyMMdd"),
        mode: display.getProperty("animationMode", "cumulative"),
        speed: parseInt(display.getProperty("animationSpeed", 250)),
        toggleAnimation: function() {
	    this.running = !this.running;
	    if(this.btnRun)
		this.btnRun.html(HtmlUtils.getIconImage(this.running ? "fa-stop" : "fa-play"));
	    if (this.running)
		this.startAnimation();
	},
        getDomId: function(id) {
	    return this.display.getDomId(id);
	},
	jq: function(id) {
	    return this.display.jq(id);
	},
	init: function(dateMin, dateMax, records) {
	    let _this = this;
	    this.dateMin = dateMin;
	    this.dateMax = dateMax;
	    this.begin = this.dateMin;
	    this.end = this.dateMax;
	    if(!this.dateMin) return;

            this.dateRange = this.dateMax.getTime() - this.dateMin.getTime();
	    this.steps= parseFloat(this.display.getProperty("animationSteps", 60));
	    this.windowUnit = this.display.getProperty("animationWindow", "");

	    if (this.windowUnit != "") {
		if (this.windowUnit == "decade") {
		    this.window = 1000 * 60 * 60 * 24 * 365 * 10;// + 1000 * 60 * 60 * 24 * 365;
		} else 	if (this.windowUnit == "century") {
		    this.window = 1000 * 60 * 60 * 24 * 365 * 100;// + 1000 * 60 * 60 * 24 * 365;
		} else 	if (this.windowUnit == "halfdecade") {
		    this.window = 1000 * 60 * 60 * 24 * 365 * 5;// + 1000 * 60 * 60 * 24 * 365;
		} else if (this.windowUnit == "year") {
		    this.window = 1000 * 60 * 60 * 24 * 366;
		} else if (this.windowUnit == "month") {
		    this.window = 1000 * 60 * 60 * 24 * 32;
		} else if (this.windowUnit == "week") {
		    this.window = 1000 * 60 * 60 * 24*7;
		} else if (this.windowUnit == "day") {
		    this.window = 1000 * 60 * 60 * 24;
		} else if (this.windowUnit == "hour") {
		    this.window = 1000 * 60 * 60;
		} else if (this.windowUnit == "minute") {
		    this.window = 1000 * 61;
		} else {
		    this.window = 1001;
		}
	    } else if(this.steps>0){
		this.window = this.dateRange / this.steps;
	    }


	    this.updateLabels();
	    

	    this.jq(ID_SLIDER).slider({
		range: true,
		min: this.dateMin.getTime(),
		max: this.dateMax.getTime(),
		values: [this.dateMin.getTime(),this.dateMax.getTime()],
		slide: function( event, ui ) {
		    _this.stopAnimation();
		    _this.begin = new Date(ui.values[0]);
		    _this.end = new Date(ui.values[1]);
		    //			    _this.applyAnimation(true);
		    _this.updateLabels();
		},
		stop: function(event,ui) {
		    _this.stopAnimation();
		    _this.begin = new Date(ui.values[0]);
		    _this.end = new Date(ui.values[1]);
		    _this.applyAnimation(true);
		}
	    });

	    if(records && this.display.getProperty("animationShowTicks",true)) {
		var ticks = "";
		var min = this.dateMin.getTime();
		var max = this.dateMax.getTime();
		var p = 0;
		for(var i=0;i<records.length;i++) {
		    var record = records[i];
		    var date = record.getDate().getTime();
		    var perc = Math.round((date-min)/(max-min)*100);
		    var tt = this.formatAnimationDate(record.getDate());
		    ticks+=HtmlUtils.div(["id",this.display.getId()+"-"+record.getId(), "class","display-animation-tick","style","left:" + perc+"%;","title",tt,"recordIndex",i],"");
		}
		this.jq(ID_TICKS).html(ticks);
		this.display.makeTooltips(this.jq(ID_TICKS).find(".display-animation-tick"), records,(open,record) =>{
		    this.display.handleEventRecordHighlight(this, {highlight: open,record:record, skipAnimation:true});
		});
	    }
	},
        handleEventRecordHighlight: function(source, args) {
	    var element = $("#" + this.display.getId()+"-"+args.record.getId());
//	    console.log(args.highlight +" " + element.length);
	    if(args.highlight) {
		element.addClass("display-animation-tick-highlight");
	    } else {
		element.removeClass("display-animation-tick-highlight");
	    }
	},
	makeControls:function() {
            var buttons =  "";
	    if(this.display.getProperty("animationShowButtons",true)) {
		buttons +=
		    HtmlUtils.span(["id", this.getDomId(ID_BEGIN),"title","Go to beginning"], HtmlUtils.getIconImage("fa-fast-backward")) + 
		    HtmlUtils.span(["id", this.getDomId(ID_PREV), "title","Previous"], HtmlUtils.getIconImage("fa-step-backward")) + 
		    HtmlUtils.span(["id", this.getDomId(ID_RUN),  "title","Run/Stop"], HtmlUtils.getIconImage("fa-play")) + 
		    HtmlUtils.span(["id", this.getDomId(ID_NEXT), "title","Next"], HtmlUtils.getIconImage("fa-step-forward")) +
		    HtmlUtils.span(["id", this.getDomId(ID_END), "title","Go to end"], HtmlUtils.getIconImage("fa-fast-forward")) + 
		    HtmlUtils.span(["id", this.getDomId(ID_SHOWALL), "title","Show all"], HtmlUtils.getIconImage("fa-sync"));
	    }
	    buttons+=HtmlUtils.span(["id", this.getDomId(ID_ANIMATION_LABEL), "class", "display-animation-label"]);
            buttons = HtmlUtils.div([ "class","display-animation-buttons"], buttons);
	    if(display.getProperty("animationShowSlider",true)) {
		buttons+=   HtmlUtils.div(["class","display-animation-slider","id",this.getDomId(ID_SLIDER)],
					  HtmlUtils.div(["class","display-animation-ticks","id",this.getDomId(ID_TICKS)]));
	    }
	    

            this.jq(ID_TOP_LEFT).append(HtmlUtils.div(["style",this.display.getProperty("animationStyle")], buttons));
            this.btnRun = this.jq(ID_RUN);
            this.btnPrev = this.jq(ID_PREV);
            this.btnNext = this.jq(ID_NEXT);
            this.btnBegin = this.jq(ID_BEGIN);
            this.btnEnd = this.jq(ID_END);
            this.btnShowAll = this.jq(ID_SHOWALL);
            this.label = this.jq(ID_ANIMATION_LABEL);
            this.btnRun.button().click(() => {
                this.toggleAnimation();
            });
            this.btnBegin.button().click(() => {
		this.begin = this.dateMin;
		if (this.mode == "sliding") {
		    this.end = new Date(this.dateMin.getTime()+this.window);
		} else {
		    this.end = new Date(this.dateMin.getTime()+this.window);
		    //			this.end =this.begin;
		}
		this.stopAnimation();
		this.applyAnimation();
            });
            this.btnEnd.button().click(() => {
		this.end = this.dateMax;
		if (this.mode == "sliding") {
		    this.begin = new Date(this.dateMax.getTime()-this.window);
		} else {
		    this.end =this.dateMax;
		}
		this.stopAnimation();
		this.applyAnimation();
            });
            this.btnPrev.button().click(() => {
		if (this.mode == "sliding") {
		    this.begin = new Date(this.begin.getTime()-this.window);
		    if(this.begin.getTime()<this.dateMin.getTime())
			this.begin = this.dateMin;
		    this.end = new Date(this.begin.getTime()+this.window);
		} else {
		    this.end = new Date(this.end.getTime()-this.window);
		    if(this.end.getTime()<=this.begin.getTime()) {
			this.end = new Date(this.begin.getTime()+this.window);
		    }
		}
		this.stopAnimation();
		this.applyAnimation();
            });
            this.btnNext.button().click(() => {
		this.stopAnimation();
		this.doNext();
            });
            this.btnShowAll.button().click(() => {
		this.begin = this.dateMin;
		this.end = this.dateMax;
		this.inAnimation = false;
		this.running = false;
		if(this.btnRun)
		    this.btnRun.html(HtmlUtils.getIconImage("fa-play"));
		this.applyAnimation();
            });
        },

	doNext: function() {
	    if (this.mode == "sliding") {
		this.begin = this.end;
		this.end = new Date(this.end.getTime()+this.window);
		if(this.end.getTime()>this.dateMax.getTime()) {
		    this.end = this.dateMax;
		    this.begin = new Date(this.end.getTime()-this.window);
		    this.inAnimation = false;
		    this.stopAnimation();
		}
	    } else {
		this.end = new Date(this.end.getTime()+this.window);
		if(this.end.getTime()>=this.dateMax.getTime()) {
		    this.end = this.dateMax;
		    this.inAnimation = false;
		    this.stopAnimation();
		}
	    }
	    this.applyAnimation();
	},

	startAnimation: function() {
	    //		if (!this.display.points) {
	    //		    return;
	    //		}
            if (!this.dateMax) return;
            if (!this.inAnimation) {
                this.inAnimation = true;
                this.label.html("");
                var date = this.dateMin;
                this.begin = date;
                var unit = this.windowUnit;
                if (unit != "") {
                    var tmp = 0;
                    if (unit == "decade") {
                        this.begin = new Date(date.getUTCFullYear(), 0);
                    } else if (unit == "year") {
                        this.begin = new Date(date.getUTCFullYear(), 0);
                    } else if (unit == "month") {
                        this.begin = new Date(date.getUTCFullYear(), date.getMonth());
                    } else if (unit == "day") {
                        this.begin = new Date(date.getUTCFullYear(), date.getMonth(), date.getDay());
                    } else if (unit == "hour") {
                        this.begin = new Date(date.getUTCFullYear(), date.getMonth(), date.getDay(), date.getHours());
                    } else if (unit == "minute") {
                        this.begin = new Date(date.getUTCFullYear(), date.getMonth(), date.getDay(), date.getHours(), date.getMinutes());
                    } else {
                        this.begin = new Date(date.getUTCFullYear(), date.getMonth(), date.getDay(), date.getHours(), date.getSeconds());
                    }
                } 
                this.end = this.begin;
		this.display.animationStart();
            }
	    this.doNext();
        },
	stopAnimation:function() {
	    if(this.btnRun)
		this.btnRun.html(HtmlUtils.getIconImage("fa-play"));
            this.running = false;
	},
	setDateRange: function(begin,end) {
	    this.begin = begin;
	    this.end = end;
	    this.stopAnimation();
	    this.updateUI();
	},
	applyAnimation: function(skipSlider) {
	    this.display.animationApply(this);
	    this.updateUI();
	},
	updateUI: function(skipSlider) {
	    if(!skipSlider) {
		this.jq(ID_SLIDER).slider('values',0,this.begin.getTime());
		this.jq(ID_SLIDER).slider('values',1,this.end.getTime());
	    }
	    this.updateLabels();
            var windowEnd = this.end.getTime();
            if (windowEnd <= this.dateMax.getTime()) {
                if (this.running) {
                    setTimeout(() => {
			if(!this.running) return;
			this.doNext()}, this.speed);
                }
            } else {
                this.running = false;
                this.inAnimation = false;
		if(this.btnRun)
                    this.btnRun.html(HtmlUtils.getIconImage("fa-play"));
            }
	},

	updateLabels: function() {
	    if(this.label)
		this.label.html(this.formatAnimationDate(this.begin) + " - " + this.formatAnimationDate(this.end));
	},
        formatAnimationDate: function(d) {
            if (this.dateFormat == "yyyy") {
                return Utils.formatDateYYYY(d);
            } else if (this.dateFormat == "yyyyMMdd") {
                return Utils.formatDateYYYYMMDD(d);
	    } else if (this.dateFormat == "monthdayyear") {
                return Utils.formatDateMonthDayYear(d);
	    } else if (this.dateFormat == "mdy") {
                return Utils.formatDateMDY(d);
	    } else if (this.dateFormat == "hhmm") {
                return Utils.formatDateHHMM(d);
            } else {
                return Utils.formatDate(d);
            }
        },

    });
}

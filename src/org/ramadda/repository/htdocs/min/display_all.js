/**
Copyright 2008-2019 Geode Systems LLC
*/






function AreaWidget(display) {
    var ID_CONTAINS = "contains";
    var ID_NORTH = "north";
    var ID_SOUTH = "south";
    var ID_EAST = "east";
    var ID_WEST = "west";

    var ID_AREA_LINK = "arealink";

    RamaddaUtil.inherit(this, {
        display: display,
        getHtml: function() {
            var callback = this.display.getGet();
            //hack, hack
            var cbx = HtmlUtils.checkbox(this.display.getDomId(ID_CONTAINS), ["title", "Search mode: checked - contains, unchecked - overlaps"], false);
            var link = HtmlUtils.onClick(callback + ".areaWidget.areaLinkClick();", HtmlUtils.image(root + (this.linkArea ? "/icons/link.png" : "/icons/link_break.png"), [ATTR_TITLE, "Set bounds from map", ATTR_CLASS, "display-area-link", "border", "0", ATTR_ID, this.display.getDomId(ID_AREA_LINK)]));

            var mylocation = HtmlUtils.onClick(callback + ".areaWidget.useMyLocation();", HtmlUtils.image(root + "/icons/compass.png"), [ATTR_TITLE, "Set my location", ATTR_CLASS, "display-area-link", "border", "0"]);


            var erase = HtmlUtils.onClick(callback + ".areaWidget.areaClear();", HtmlUtils.image(root + "/icons/eraser.png", [ATTR_TITLE, "Clear form", ATTR_CLASS, "display-area-link", "border", "0"]));

            var areaForm = HtmlUtils.openTag(TAG_TABLE, [ATTR_CLASS, "display-area", "border", "0", "cellpadding", "0", "cellspacing", "0"]);
            areaForm += HtmlUtils.tr([],
                HtmlUtils.td(["align", "center"],
                    HtmlUtils.leftCenterRight(mylocation,
                        HtmlUtils.input(ID_NORTH, "", ["placeholder", " N", ATTR_CLASS, "input display-area-input", "size", "5", ATTR_ID,
                            this.display.getDomId(ID_NORTH), ATTR_TITLE, "North"
                        ]), link, "20%", "60%", "20%")));

            areaForm += HtmlUtils.tr([], HtmlUtils.td([],
                HtmlUtils.input(ID_WEST, "", ["placeholder", " W", ATTR_CLASS, "input  display-area-input", "size", "5", ATTR_ID,
                    this.display.getDomId(ID_WEST), ATTR_TITLE, "West"
                ]) +
                HtmlUtils.input(ID_EAST, "", ["placeholder", " E", ATTR_CLASS, "input  display-area-input", "size", "5", ATTR_ID,
                    this.display.getDomId(ID_EAST), ATTR_TITLE, "East"
                ])));
            areaForm += HtmlUtils.tr([],
                HtmlUtils.td(["align", "center"],
                    HtmlUtils.leftCenterRight(erase, HtmlUtils.input(ID_SOUTH, "", ["placeholder", " S", ATTR_CLASS, "input  display-area-input", "size", "5", ATTR_ID,
                        this.display.getDomId(ID_SOUTH), ATTR_TITLE, "South"
                    ]), cbx)));

            areaForm += HtmlUtils.closeTag(TAG_TABLE);
            return areaForm;
        },
        areaClear: function() {
            $("#" + this.display.getDomId(ID_NORTH)).val("");
            $("#" + this.display.getDomId(ID_WEST)).val("");
            $("#" + this.display.getDomId(ID_SOUTH)).val("");
            $("#" + this.display.getDomId(ID_EAST)).val("");
            this.display.areaClear();
        },
        useMyLocation: function() {
            if (navigator.geolocation) {
                var _this = this;
                navigator.geolocation.getCurrentPosition(function(position) {
                    _this.setUseMyLocation(position);
                });
            } else {}
        },
        setUseMyLocation: function(position) {
            var lat = position.coords.latitude;
            var lon = position.coords.longitude;
            var offset = 5.0;
            if (this.display.myLocationOffset)
                offset = parseFloat(this.display.myLocationOffset);

            $("#" + this.display.getDomId(ID_NORTH)).val(lat + offset);
            $("#" + this.display.getDomId(ID_WEST)).val(lon - offset);
            $("#" + this.display.getDomId(ID_SOUTH)).val(lat - offset);
            $("#" + this.display.getDomId(ID_EAST)).val(lon + offset);
            if (this.display.submitSearchForm)
                this.display.submitSearchForm();
        },
        areaLinkClick: function() {
            this.linkArea = !this.linkArea;
            var image = root + (this.linkArea ? "/icons/link.png" : "/icons/link_break.png");
            $("#" + this.display.getDomId(ID_AREA_LINK)).attr("src", image);
            if (this.linkArea && this.lastBounds) {
                var b = this.lastBounds;
                $("#" + this.display.getDomId(ID_NORTH)).val(formatLocationValue(b.top));
                $("#" + this.display.getDomId(ID_WEST)).val(formatLocationValue(b.left));
                $("#" + this.display.getDomId(ID_SOUTH)).val(formatLocationValue(b.bottom));
                $("#" + this.display.getDomId(ID_EAST)).val(formatLocationValue(b.right));
            }
        },
        linkArea: false,
        lastBounds: null,
        handleEventMapBoundsChanged: function(source, args) {
            bounds = args.bounds;
            this.lastBounds = bounds;
            if (!args.force && !this.linkArea) return;
            $("#" + this.display.getDomId(ID_NORTH)).val(formatLocationValue(bounds.top));
            $("#" + this.display.getDomId(ID_WEST)).val(formatLocationValue(bounds.left));
            $("#" + this.display.getDomId(ID_SOUTH)).val(formatLocationValue(bounds.bottom));
            $("#" + this.display.getDomId(ID_EAST)).val(formatLocationValue(bounds.right));
        },
        setSearchSettings: function(settings) {
            var cbx = $("#" + this.display.getDomId(ID_CONTAINS));
            if (cbx.is(':checked')) {
                settings.setAreaContains(true);
            } else {
                settings.setAreaContains(false);
            }
            settings.setBounds(this.display.getFieldValue(this.display.getDomId(ID_NORTH), null),
                this.display.getFieldValue(this.display.getDomId(ID_WEST), null),
                this.display.getFieldValue(this.display.getDomId(ID_SOUTH), null),
                this.display.getFieldValue(this.display.getDomId(ID_EAST), null));
        },
    });
}




function DateRangeWidget(display) {
    var ID_DATE_START = "date_start";
    var ID_DATE_END = "date_end";

    RamaddaUtil.inherit(this, {
        display: display,
        initHtml: function() {
            this.display.jq(ID_DATE_START).datepicker();
            this.display.jq(ID_DATE_END).datepicker();
        },
        setSearchSettings: function(settings) {
            var start = this.display.jq(ID_DATE_START).val();
            var end = this.display.jq(ID_DATE_START).val();
            settings.setDateRange(start, end);
        },
        getHtml: function() {
            var html = HtmlUtils.input(ID_DATE_START, "", ["class", "display-date-input", "placeholder", " start date", ATTR_ID,
                    display.getDomId(ID_DATE_START), "size", "10"
                ]) + " - " +
                HtmlUtils.input(ID_DATE_END, "", ["class", "display-date-input", "placeholder", " end date", ATTR_ID,
                    display.getDomId(ID_DATE_END), "size", "10"
                ]);
            return html;
        }
    });
}/**
Copyright 2008-2019 Geode Systems LLC
*/


//Ids of DOM components
var ID_FIELDS = "fields";
var ID_HEADER = "header";
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

var displayDebug = false;


var PROP_DISPLAY_FILTER = "displayFilter";
var PROP_EXCLUDE_ZERO = "excludeZero";


var PROP_DIVID = "divid";
var PROP_FIELDS = "fields";
var PROP_LAYOUT_HERE = "layoutHere";
var PROP_HEIGHT = "height";
var PROP_WIDTH = "width";







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
        getRecordHtml: function(record, fields) {
            var showGeo = false;
            if (Utils.isDefined(this.showGeo)) {
                showGeo = ("" + this.showGeo) == "true";
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
            if (value) return value;
            return dflt;
            },
    });
}





function RamaddaDisplay(argDisplayManager, argId, argType, argProperties) {
    RamaddaUtil.initMembers(this, {
        orientation: "horizontal",
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
        getColorTable: function(justColors, name, dflt) {
            var colorTable = this.getColorTableName(name);
            if (!colorTable) {
                colorTable = dflt;
            }
            if (colorTable) {
                var ct = Utils.ColorTables[colorTable];
                if (ct && justColors) return ct.colors;
                if (!ct && name) {
                    return colorTable.split(",");
                }
                return ct;
            }
            if (this.getProperty("colors")) {
                var colors = this.getProperty("colors");
                if ((typeof colors) == "object") return colors;
                return colors.split(",");
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
        handleEventFieldValueSelected: function(source, args) {
            this.setProperty("filterPattern", args.value);
            this.setProperty("patternFilterField", args.field.getId());
            this.updateUI();
        },
        handleEventPropertyChanged: function(source, prop) {
            if (prop.property == "filterPattern") {
                this.jq("filterValueMenu").val(prop.value);
            }

            this.setProperty(prop.property, prop.value);
            this.updateUI();
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
            if (!msg) msg = "Loading data...";
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
                    if (firstField == null && field.isNumeric) firstField = field;
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
                if (field.isNumeric && !field.isFieldGeo()) return [field];
            }
            return [];
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
            for (a in fields) {
                var field = fields[a];
                if (type == null) return field;
                if (numeric) {
                    if (field.isFieldNumeric()) {
                        list.push(field);
                    }
                } else if (field.getType() == type) {
                    list.push(field);
                }
            }
            return list;
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
        filterData: function(dataList, fields) {
            if (!dataList) {
                var pointData = this.getData();
                if (pointData == null) return null;
                dataList = pointData.getRecords();
            }

            if (!fields) {
                fields = pointData.getRecordFields();
            }
            var patternFieldId = this.getProperty("patternFilterField");
            var numericFieldId = this.getProperty("numericFilterField");
            var pattern = this.getProperty("filterPattern");
            var prefix = this.getProperty("filterPatternPrefix");
            if (prefix) pattern = prefix + pattern;
            var suffix = this.getProperty("filterPatternSuffix");
            if (suffix) pattern = pattern + suffix;

            var notPattern = false;
            if (pattern) {
                notPattern = pattern.startsWith("!");
                if (notPattern) {
                    pattern = pattern.substring(1);
                }
            }

            var filterSValue = this.getProperty("numericFilterValue");
            var filterOperator = this.getProperty("numericFilterOperator", "<");

            if ((numericFieldId || patternFieldId) && (pattern || (filterSValue && filterOperator))) {
                var patternFields = this.getFieldsByIds(fields, patternFieldId);
                var numericField = null;
                for (var i = 0; i < fields.length; i++) {
                    if (!numericField && (fields[i].getId() == numericFieldId || numericFieldId == "#" + (i + 1))) {
                        numericField = fields[i];
                    }
                    if (patternField && numericField) break;
                }
                if (patternFields.length || numericField) {
                    var list = [];
                    var filterValue;
                    if (filterSValue) {
                        filterValue = parseFloat(filterSValue);
                    }
                    var isPointRecord = false;
                    if (dataList.length > 0)
                        isPointRecord = dataList[0].isPointRecord;
                    for (var i = 0; i < dataList.length; i++) {
                        var obj = dataList[i];
                        var row = this.getDataValues(obj);
                        var array = row;
                        if (!isPointRecord && i == 0) {
                            list.push(obj);
                            continue;
                        }
                        var ok = false;
                        if (numericField && filterSValue && filterOperator) {
                            var value = parseFloat(array[numericField.getIndex()]);
                            var filterValue = parseFloat(filterSValue);
                            if (filterOperator == "<") {
                                ok = value < filterValue;
                            } else if (filterOperator == "<=") {
                                ok = value <= filterValue;
                            } else if (filterOperator == "==") {
                                ok = value == filterValue;
                            } else if (filterOperator == ">") {
                                ok = value > filterValue;
                            } else if (filterOperator == ">=") {
                                ok = value >= filterValue;
                            }
                            if (!ok) {
                                continue;
                            }
                        }
                        if (patternFields.length && pattern) {
                            for (var fieldIdx = 0; fieldIdx < patternFields.length; fieldIdx++) {
                                var patternField = patternFields[fieldIdx];
                                var value = "" + array[patternField.getIndex()];
                                ok = value.match(pattern);
                                if (notPattern) ok = !ok;
                                if (ok) break;
                            }
                        }
                        if (ok) {
                            list.push(obj);
                        }
                    }
                    dataList = list;
                }
            }

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
                    HtmlUtils.image(ramaddaBaseUrl + "/icons/table.png", ["border", 0, ATTR_TITLE, "Create Tabular Display"])));

                var x;
                toolbarItems.push(x = HtmlUtils.tag(TAG_A, ["onclick", get + ".createDisplay(" + HtmlUtils.sqt(entry.getFullId()) + "," +
                        HtmlUtils.sqt("linechart") + "," + HtmlUtils.sqt(jsonUrl) + ");"
                    ],
                    HtmlUtils.image(ramaddaBaseUrl + "/icons/chart_line_add.png", ["border", 0, ATTR_TITLE, "Create Chart"])));
            }
            toolbarItems.push(HtmlUtils.tag(TAG_A, ["onclick", get + ".createDisplay(" + HtmlUtils.sqt(entry.getFullId()) + "," +
                    HtmlUtils.sqt("entrydisplay") + "," + HtmlUtils.sqt(jsonUrl) + ");"
                ],
                HtmlUtils.image(ramaddaBaseUrl + "/icons/layout_add.png", ["border", 0, ATTR_TITLE, "Show Entry"])));

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
                showMenu: true,
                sourceEntry: entry,
                entryId: entry.getId(),
                showTitle: true,
                showDetails: true,
                title: entry.getName(),
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
            return this.orientation == "horizontal";
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
            return this.getSelfProperty("showTitle", true);
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
            this.checkFilterValueMenu();
        },
        checkFilterValueMenu: function() {
            if (!this.getProperty("showFilterWidget", true)) return;
            var filterValues = this.getProperty("filterValues");
            var filterValuesLabel = this.getProperty("filterValuesLabel", "");
            if (!filterValues) {
                var filterFieldId = this.getProperty("patternFilterField");
                if (filterFieldId) {
                    var filterField = this.getFieldById(null, filterFieldId);
                    if (filterField) {
                        var pointData = this.getData();
                        if (pointData == null) return;
                        if (filterValuesLabel == "") filterValuesLabel = filterField.getLabel() + ": ";
                        filterValues = this.getColumnValues(pointData.getRecords(), filterField).values;
                        filterValues = Utils.getUniqueValues(filterValues);
                        filterValues.unshift("");
                    }
                }
            }
            var filterValue = this.getProperty("filterPattern");
            if (filterValues) {
                var toks;
                if ((typeof filterValues) == "string") {
                    filterValues = Utils.getMacro(filterValues);
                    toks = filterValues.split(",");
                } else {
                    toks = filterValues;
                }
                var menu = "<select class='ramadda-pulldown' id='" + this.getDomId("filterValueMenu") + "'>";
                for (var i = 0; i < toks.length; i++) {
                    var extra = "";
                    if (filterValue == toks[i]) extra = "selected ";
                    if (toks[i] == "") {
                        menu += "<option value='' " + extra + ">" + "All" + "</option>\n";
                    } else {
                        menu += "<option " + extra + ">" + toks[i] + "</option>\n";
                    }
                }

                menu += "</select>";
                let _this = this;
                this.writeHtml(ID_TOP_RIGHT, filterValuesLabel + menu);
                this.jq("filterValueMenu").change(function() {
                    var value = $(this).val();
                    if (_this.getProperty("filterPattern") == value) return;
                    if (value == "") value = null;
                    _this.setProperty("filterPattern", value);
                    _this.updateUI();
                    _this.propagateEvent("handleEventPropertyChanged", {
                        property: "filterPattern",
                        value: value
                    });
                });
            }
        },
        updateUI: function() {},
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
            html += HtmlUtils.leftRightTable(left, right, {
                valign: "bottom"
            });
            var contents = this.getContentsDiv();
            html += contents;
            html += HtmlUtils.closeTag(TAG_DIV);
            if (dobs) {
                html += HtmlUtils.closeTag(TAG_DIV);
            }
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
                images.push(ramaddaBaseUrl + "/icons/page_delete.png");
                labels.push("Delete display");
            }
            calls.push(get + ".copyDisplay();");
            images.push(ramaddaBaseUrl + "/icons/page_copy.png");
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
                var inner = HtmlUtils.image(images[i], [ATTR_TITLE, labels[i], ATTR_CLASS, "display-dialog-header-icon"]);
                if (addLabel) inner += " " + labels[i] + "<br>";
                toolbar += HtmlUtils.onClick(calls[i], inner);
            }
            return toolbar;
        },
        makeDialog: function() {
            var html = "";
            html += HtmlUtils.div([ATTR_ID, this.getDomId(ID_HEADER), ATTR_CLASS, "display-header"]);
            var close = HtmlUtils.onClick("$('#" + this.getDomId(ID_DIALOG) + "').hide();", HtmlUtils.image(icon_close, [ATTR_CLASS, "display-dialog-close", ATTR_TITLE, "Close Dialog"]));
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
            if (!height.endsWith("px") && !height.endsWith("%"))
                height = height + "px";
            return height;
        },
        getContentsStyle: function() {
            var style = "";
            var height = this.getHeightForStyle();
            if (height) {
                style += " height:" + height + ";";
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

            return top + HtmlUtils.div([ATTR_CLASS, "display-contents-inner display-" + this.type, "style", style, ATTR_ID, this.getDomId(ID_DISPLAY_CONTENTS)], "") + bottom;
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
                this.checkFilterValueMenu();
            }
            if (url != null) {
                this.jsonUrl = url;
            } else {
                this.jsonUrl = null;
            }
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
        makeDataArray: function(dataList) {
            if (dataList.length == 0) return dataList;
            var data = [];
            if (dataList[0].getData) {
                for (var i = 0; i < dataList.length; i++)
                    data.push(dataList[i].getData());
            } else if (dataList[0].tuple) {
                for (var i = 0; i < dataList.length; i++)
                    data.push(dataList[i].tuple);
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


            this.minDateObj = Utils.parseDate(this.minDate, false);
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
            var indexField = this.indexField;
            var records = pointData.getRecords();
            var allFields = pointData.getRecordFields();

            //Check if there are dates and if they are different
            this.hasDate = this.getHasDate(records);
            var date_formatter = this.getDateFormatter();
            var rowCnt = -1;
            for (j = 0; j < records.length; j++) {
                var record = records[j];
                var date = record.getDate();
                if (!this.dateInRange(date)) continue;
                rowCnt++;
                var values = [];
                if (props && (props.includeIndex || props.includeIndexIfDate)) {
                    var indexName = null;
                    if (indexField >= 0) {
                        var field = allFields[indexField];
                        values.push(record.getValue(indexField) + offset);
                        indexName = field.getLabel();
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
                    //                        console.log(field.getId() +" = " + value);
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
                    //                        console.log(" skipping due to zero: " + values);
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
                //                    console.log("Num non null:" + nonNullRecords);
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
    var LAYOUT_TABLE = TAG_TABLE;
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
                if (this.displays[i].getIsLayoutFixed()) continue;
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
                //                console.log("table:" + this.columns);
                if (displaysToLayout.length == 1) {
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
                    }

                    if (i > 0) {
                        html += HtmlUtils.closeTag(TAG_DIV);
                    }
                }
                //                    console.log("HTML " + html);
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

}/**
Copyright 2008-2019 Geode Systems LLC
*/



/*
This package supports charting and mapping of georeferenced time series data
*/

var pointDataCache = {};


function DataCollection() {
    RamaddaUtil.defineMembers(this, {
        data: [],
        hasData: function() {
            for (var i = 0; i < this.data.length; i++) {
                if (this.data[i].hasData()) return true;
            }
            return false;
        },
        getList: function() {
            return this.data;
        },
        addData: function(data) {
            this.data.push(data);
        },
        handleEventMapClick: function(myDisplay, source, lon, lat) {
            var anyHandled = false;
            for (var i = 0; i < this.data.length; i++) {
                if (this.data[i].handleEventMapClick(myDisplay, source, lon, lat)) {
                    anyHandled = true;
                }
            }
            return anyHandled;

        },


    });

}

function BasePointData(name, properties) {
    if (properties == null) properties = {};

    RamaddaUtil.defineMembers(this, {
        recordFields: null,
        records: null,
        entryId: null,
        entry: null
    });

    $.extend(this, properties);

    RamaddaUtil.defineMembers(this, {
        name: name,
        properties: properties,
        initWith: function(thatPointData) {
            this.recordFields = thatPointData.recordFields;
            this.records = thatPointData.records;
        },
        handleEventMapClick: function(myDisplay, source, lon, lat) {
            return false;
        },
        hasData: function() {
            return this.records != null;
        },
        clear: function() {
            this.records = null;
            this.recordFields = null;
        },
        getProperties: function() {
            return this.properties;
        },
        getProperty: function(key, dflt) {
            var value = this.properties[key];
            if (value == null) return dflt;
            return value;
        },

        getRecordFields: function() {
            return this.recordFields;
        },
        addRecordField: function(field) {
            this.recordFields.push(field);
        },
        getRecords: function() {
            return this.records;
        },
        getNumericFields: function() {
            var recordFields = this.getRecordFields();
            var numericFields = [];
            for (var i = 0; i < recordFields.length; i++) {
                var field = recordFields[i];
                if (field.isNumeric) numericFields.push(field);
            }
            return numericFields;
        },
        getChartableFields: function(display) {
            var recordFields = this.getRecordFields();
            var numericFields = [];
            var skip = /(TIME|HOUR|MINUTE|SECOND|YEAR|MONTH|DAY|LATITUDE|LONGITUDE|^ELEVATION$)/g;
            var skip = /(xxxnoskip)/g;
            for (var i = 0; i < recordFields.length; i++) {
                var field = recordFields[i];
                if (!field.isNumeric || !field.isChartable()) {
                    continue;
                }
                var ID = field.getId().toUpperCase();
                if (ID.match(skip)) {
                    continue;
                }
                numericFields.push(field);
            }

            return RecordUtil.sort(numericFields);
        },
        getNonGeoFields: function(display) {
            var recordFields = this.getRecordFields();
            var numericFields = [];
            //                var skip = /(TIME|HOUR|MINUTE|SECOND|YEAR|MONTH|DAY|LATITUDE|LONGITUDE|ELEVATION)/g;
            var hadDate = false;
            for (var i = 0; i < recordFields.length; i++) {
                var field = recordFields[i];
                if (field.isFieldGeo()) {
                    continue;
                }
                if (field.isFieldDate()) {
                    if (hadDate && field.getId() == "recordDate") {
                        continue;
                    }
                    hadDate = true;
                }

                //                    var ID = field.getId().toUpperCase() ;
                //                    if(ID.match(skip)) {
                //                        continue;
                //                    }
                numericFields.push(field);
            }
            return numericFields;
            //                return RecordUtil.sort(numericFields);
        },

        loadData: function(display) {},
        getName: function() {
            return this.name;
        },
        getTitle: function() {
            if (this.records != null && this.records.length > 0)
                return this.name + " - " + this.records.length + " points";
            return this.name;
        }
    });
}





function convertToPointData(array) {
    var fields = [];
    var records = [];
    var header = array[0];
    var samples = array[1];
    for(var i=0;i<header.length;i++) {
        var label = header[i];
        var id = label.toLowerCase().replace(/ /g,"_");
        var sample =samples[i];
        var tof= typeof sample;
        var type;
        if(tof=="string")
            type = "string";
        else if(tof=="number")
            type = "double";
        else if(sample.getTime)
            type = "date";
        else 
            console.log("Unknwon type:" + tof);
        fields.push(new RecordField({
                    id:id,
                        label:label,
                        type:type,
                        chartable:true
                        }));
    }
    for(var i=1;array.length;i++) {
        records.push(new  PointRecord(NaN, NaN, NaN, null, array[i]));
    }
    return new  PointData("pointdata", fields, records,null,null);
}


/*
This encapsulates some instance of point data. 
name - the name of this data
recordFields - array of RecordField objects that define the metadata
data - array of Record objects holding the data
*/
function PointData(name, recordFields, records, url, properties) {
    RamaddaUtil.inherit(this, new BasePointData(name, properties));
    RamaddaUtil.defineMembers(this, {
        recordFields: recordFields,
        records: records,
        url: url,
        loadingCnt: 0,
        equals: function(that) {
            return this.url == that.url;
        },
        getIsLoading: function() {
            return this.loadingCnt > 0;
        },
        handleEventMapClick: function(myDisplay, source, lon, lat) {
            this.lon = lon;
            this.lat = lat;
            if (myDisplay.getDisplayManager().hasGeoMacro(this.url)) {
                this.loadData(myDisplay, true);
                return true;
            }
            return false;
        },
        startLoading: function() {
            this.loadingCnt++;
        },
        stopLoading: function() {
            this.loadingCnt--;
        },
        loadData: function(display, reload) {
            if (this.url == null) {
                console.log("No URL");
                return;
            }
            var props = {
                lat: this.lat,
                lon: this.lon,
            };
            var jsonUrl = display.displayManager.getJsonUrl(this.url, display, props);
            this.loadPointJson(jsonUrl, display, reload);
        },
        loadPointJson: function(url, display, reload) {
            var pointData = this;
            this.startLoading();
            var _this = this;
            var obj = pointDataCache[url];
            if (obj == null) {
                obj = {
                    pointData: null,
                    pending: []
                };
                //                    console.log("created new obj in cache: " +url);
                pointDataCache[url] = obj;
            }
            if (obj.pointData != null) {
                //                    console.log("from cache " +url);
                display.pointDataLoaded(obj.pointData, url, reload);
                return;
            }
            obj.pending.push(display);
            if (obj.pending.length > 1) {
                //                    console.log("Waiting on callback:" + obj.pending.length +" " + url);
                return;
            }
            var fail = function(jqxhr, textStatus, error) {
                var err = textStatus + ": " + error;
                console.log("JSON error:" + err);
                display.pointDataLoadFailed(err);
                pointData.stopLoading();
            }

            var success=function(data) {
                    if (GuiUtils.isJsonError(data)) {
                        //                        console.log("fail");
                        display.pointDataLoadFailed(data);
                        return;
                    }
                    var newData = makePointData(data, _this.derived, display);
                    obj.pointData = pointData.initWith(newData);
                    var tmp = obj.pending;
                    obj.pending = [];
                    for (var i = 0; i < tmp.length; i++) {
                        //                            console.log("Calling: " + tmp[i]);
                        tmp[i].pointDataLoaded(pointData, url, reload);
                    }
                    pointData.stopLoading();
                }
            console.log("load data:" + url);
            //                console.log("loading point url:" + url);
            Utils.doFetch(url, success,fail,"text");
            //var jqxhr = $.getJSON(url, success).fail(fail);
        }

    });
}


function DerivedPointData(displayManager, name, pointDataList, operation) {
    RamaddaUtil.inherit(this, new BasePointData(name));
    RamaddaUtil.defineMembers(this, {
        displayManager: displayManager,
        operation: operation,
        pointDataList: pointDataList,
        loadDataCalls: 0,
        display: null,
        pointDataLoaded: function(pointData) {
            this.loadDataCalls--;
            if (this.loadDataCalls <= 0) {
                this.initData();
            }
        },
        equals: function(that) {
            if (that.pointDataList == null) return false;
            if (this.pointDataList.length != that.pointDataList.length) return false;
            for (var i in this.pointDataList) {
                if (!this.pointDataList[i].equals(that.pointDataList[i])) {
                    return false;
                }
            }
            return true;
        },
        initData: function() {
            var pointData1 = this.pointDataList[0];
            if (this.pointDataList.length == 1) {
                this.records = pointData1.getRecords();
                this.recordFields = pointData1.getRecordFields();
            } else if (this.pointDataList.length > 1) {
                var results = this.combineData(pointData1, this.pointDataList[1]);
                this.records = results.records;
                this.recordFields = results.recordFields;
            }
            this.display.pointDataLoaded(this);
        },
        combineData: function(pointData1, pointData2) {
            var records1 = pointData1.getRecords();
            var records2 = pointData2.getRecords();
            var newRecords = [];
            var newRecordFields;

            //TODO:  we really need visad here to sample

            if (records1.length != records2.length) {
                console.log("bad records:" + records1.length + " " + records2.length);
            }

            if (this.operation == "average") {
                for (var recordIdx = 0; recordIdx < records1.length; recordIdx++) {
                    var record1 = records1[recordIdx];
                    var record2 = records2[recordIdx];
                    if (record1.getDate() != record2.getDate()) {
                        console.log("Bad record date:" + record1.getDate() + " " + record2.getDate());
                        break;
                    }
                    var newRecord = $.extend(true, {}, record1);
                    var data1 = newRecord.getData();
                    var data2 = record2.getData();
                    for (var colIdx = 0; colIdx < data1.length; colIdx++) {
                        data1[colIdx] = (data1[colIdx] + data2[colIdx]) / 2;
                    }
                    newRecords.push(newRecord);
                }
                newRecordFields = pointData1.getRecordFields();
            } else if (this.operation == "other func") {}
            if (newRecordFields == null) {
                //for now just use the first operand
                newRecords = records1;
                newRecordFields = pointData1.getRecordFields();
            }
            return {
                records: newRecords,
                recordFields: newRecordFields
            };
        },
        loadData: function(display) {
            this.display = display;
            this.loadDataCalls = 0;
            for (var i in this.pointDataList) {
                var pointData = this.pointDataList[i];
                if (!pointData.hasData()) {
                    this.loadDataCalls++;
                    pointData.loadData(this);
                }
                if (this.loadDataCalls == 0) {
                    this.initData();
                }
            }
            //TODO: notify display
        }
    });
}





/*
This class defines the metadata for a record column. 
index - the index i the data array
id - string id
label - string label to show to user
type - for now not used but once we have string or other column types we'll need it
missing - the missing value forthis field. Probably not needed and isn't used
as I think RAMADDA passes in NaN
unit - the unit of the value
 */
function RecordField(props) {
    $.extend(this, {
        isDate: props.type == "date",
        isLatitude: false,
        isLongitude: false,
        isElevation: false,
    });
    $.extend(this, props);
    $.extend(this, {
        isNumeric: props.type == "double" || props.type == "integer",
        properties: props
    });


    RamaddaUtil.defineMembers(this, {
        getIndex: function() {
            return this.index;
        },
        isFieldGeo: function() {
            return this.isFieldLatitude() || this.isFieldLongitude() || this.isFieldElevation();
        },
        isFieldLatitude: function() {
            return this.isLatitude || this.id.toLowerCase() == "latitude";
        },
        isFieldLongitude: function() {
            return this.isLongitude || this.id.toLowerCase() == "longitude";
        },
        isFieldElevation: function() {
            return this.isElevation || this.id.toLowerCase() == "elevation" || this.id.toLowerCase() == "altitude";
        },
        isFieldNumeric: function() {
            return this.isNumeric;
        },
        isFieldDate: function() {
            return this.isDate;
        },
        isChartable: function() {
            return this.chartable;
        },
        getSortOrder: function() {
            return this.sortorder;
        },
        getId: function() {
            return this.id;
        },
        getUnitLabel: function() {
            var label = this.getLabel();
            if (this.unit && this.unit != "")
                label = label + " [" + this.unit + "]";
            return label;
        },

        getUnitSuffix: function() {
            if (this.unit && this.unit != "")
                return " [" + this.unit + "]";
            return "";
        },

        getLabel: function() {
            if (this.label == null || this.label.length == 0) return this.id;
            return this.label;
        },
        setLabel: function(l) {
            this.label = l;
        },
        getType: function() {
            return this.type;
        },
        getMissing: function() {
            return this.missing;
        },
        setUnit: function(u) {
            this.unit = u;
        },
        getUnit: function() {
            return this.unit;
        }
    });

}




/*
The main data record. This holds a lat/lon/elevation, time and an array of data
The data array corresponds to the RecordField fields
 */
function PointRecord(lat, lon, elevation, time, data) {
    this.isPointRecord = true;
    RamaddaUtil.defineMembers(this, {
        latitude: lat,
        longitude: lon,
        elevation: elevation,
        recordTime: time,
        data: data,
        getData: function() {
            return this.data;
        },
        allZeros: function() {
            var tuple = this.getData();
            var allZeros = false;
            var nums = 0;
            var nonZero = 0;
            for (var j = 0; j < tuple.length; j++) {
                if (typeof tuple[j] == "number") {
                    nums++;
                    if (!isNaN(tuple[j]) && tuple[j] != 0) {
                        nonZero++;
                        break;
                    }
                }
            }
            if (nums > 0 && nonZero == 0) {
                return true;
            }
            return false;
        },
        getValue: function(index) {
            return this.data[index];
        },
        push: function(v) {
            this.data.push(v);
        },
        hasLocation: function() {
            return !isNaN(this.latitude);
        },
        hasElevation: function() {
            return !isNaN(this.elevation);
        },
        getLatitude: function() {
            return this.latitude;
        },
        getLongitude: function() {
            return this.longitude;
        },
        getTime: function() {
            return this.recordTime;
        },
        getElevation: function() {
            return this.elevation;
        },
        getDate: function() {
            return this.recordTime;
        }
    });
}



function makePointData(json, derived, source) {

    var fields = [];
    var latitudeIdx = -1;
    var longitudeIdx = -1;
    var elevationIdx = -1;
    var dateIdx = -1;
    var dateIndexes = [];

    var offsetFields = [];
    var lastField = null;
    for (var i = 0; i < json.fields.length; i++) {
        var field = json.fields[i];
        var recordField = new RecordField(field);
        if (recordField.isFieldNumeric()) {
            if (source.getProperty) {
                var offset1 = source.getProperty(recordField.getId() + ".offset1", source.getProperty("offset1"));
                var offset2 = source.getProperty(recordField.getId() + ".offset2", source.getProperty("offset2"));
                var scale = source.getProperty(recordField.getId() + ".scale", source.getProperty("scale"));

                if (offset1 || offset2 || scale) {
                    var unit = source.getProperty(recordField.getId() + ".unit", source.getProperty("unit"));
                    if (unit) {
                        recordField.unit = unit;
                    }
                    var o = {
                        offset1: 0,
                        offset2: 0,
                        scale: 1
                    };
                    if (offset1) o.offset1 = parseFloat(offset1);
                    if (offset2) o.offset2 = parseFloat(offset2);
                    if (scale) o.scale = parseFloat(scale);
                    recordField.offset = o;
                    offsetFields.push(recordField);
                }
            }
        }


        lastField = recordField;
        fields.push(recordField);
        //        console.log("field:" + recordField.getId());
        if (recordField.isFieldLatitude()) {
            latitudeIdx = recordField.getIndex();
        } else if (recordField.isFieldLongitude()) {
            longitudeIdx = recordField.getIndex();
            //            console.log("Longitude idx:" + longitudeIdx);
        } else if (recordField.isFieldElevation()) {
            elevationIdx = recordField.getIndex();
            //            console.log("Elevation idx:" + elevationIdx);
        } else if (recordField.isFieldDate()) {
            dateIdx = recordField.getIndex();
            dateIndexes.push(dateIdx);
        }

    }


    if (!derived) {
        derived = [
            //               {'name':'temp_f','label':'Temp F', 'columns':'temperature','function':'v1*9/5+32', 'isRow':true,'decimals':2,},
            //               {'name':'Avg. Temperature','function':'return A.average(5, c1);','columns':'temperature','isColumn',true},
            //               {'name':'max_temp_f','function':'return A.max(c1);','columns':'temp_f'},
            //               {'name':'min_temp_f','function':'return A.min(c1);','columns':'temp_f'},
            //               {'name':'mavg_temp_f_10','function':'return A.movingAverage(10, c1);','columns':'temp_f'},
            //               {'name':'mavg_temp_f_20','function':'return A.movingAverage(20, c1);','columns':'temp_f'},
        ]
    }


    if (derived) {
        var index = lastField.getIndex() + 1;
        for (var dIdx = 0; dIdx < derived.length; dIdx++) {
            var d = derived[dIdx];
            //            if(!d.isRow) continue;
            var label = d.label;
            if (!label) label = d.name;
            var recordField = new RecordField({
                type: "double",
                index: (index + dIdx),
                chartable: true,
                id: d.name,
                label: label,
            });
            recordField.derived = d;
            fields.push(recordField);
        }
    }

    var pointRecords = [];
    var rows = [];

    for (var i = 0; i < json.data.length; i++) {
        var tuple = json.data[i];
        var values = tuple.values;
        //lat,lon,alt,time,data values
        var date = null;
        if ((typeof tuple.date === 'undefined')) {
            if (dateIdx >= 0) {
                date = new Date(values[dateIdx]);
            }
        } else {
            if (tuple.date != null && tuple.date != 0) {
                date = new Date(tuple.date);
            }
        }
        if ((typeof tuple.latitude === 'undefined')) {
            if (latitudeIdx >= 0)
                tuple.latitude = values[latitudeIdx];
            else
                tuple.latitude = NaN;
        }
        if ((typeof tuple.longitude === 'undefined')) {
            if (longitudeIdx >= 0)
                tuple.longitude = values[longitudeIdx];
            else
                tuple.longitude = NaN;
        }

        if ((typeof tuple.elevation === 'undefined')) {
            if (elevationIdx >= 0)
                tuple.elevation = values[elevationIdx];
            else
                tuple.elevation = NaN;
        }

        for (var j = 0; j < dateIndexes.length; j++) {
            values[dateIndexes[j]] = new Date(values[dateIndexes[j]]);
        }

        //        console.log("before:" + values);
        var h = values[2];
        for (var col = 0; col < values.length; col++) {
            if(values[col]==null) {
                values[col] = NaN;
            } 
        }


        if (derived) {
            for (var dIdx = 0; dIdx < derived.length; dIdx++) {
                var d = derived[dIdx];
                if (!d.isRow) {
                    continue;
                }
                if (!d.compiledFunction) {
                    var funcParams = [];
                    var params = (d.columns.indexOf(";") >= 0 ? d.columns.split(";") : d.columns.split(","));
                    d.fieldsToUse = [];
                    for (var i = 0; i < params.length; i++) {
                        var param = params[i].trim();
                        funcParams.push("v" + (i + 1));
                        var theField = null;
                        for (var fIdx = 0; fIdx < fields.length; fIdx++) {
                            var f = fields[fIdx];
                            if (f.getId() == param) {
                                theField = f;
                                break;
                            }
                        }
                        d.fieldsToUse.push(theField);

                    }
                    var code = "";
                    for (var i = 0; i < funcParams.length; i++) {
                        code += "var v" + (i + 1) + "=args[" + i + "];\n";
                    }
                    var tmp = d["function"];
                    if (tmp.indexOf("return") < 0) tmp = "return " + tmp;
                    code += tmp + "\n";
                    d.compiledFunction = new Function("args", code);
                    //                    console.log("Func:" + d.compiledFunction);
                }
                //TODO: compile the function once and call it
                var args = [];

                var anyNotNan = false;
                for (var fIdx = 0; fIdx < d.fieldsToUse.length; fIdx++) {
                    var f = d.fieldsToUse[fIdx];
                    var v = NaN;
                    if (f != null) {
                        v = values[f.getIndex()];
                        if (v == null) v = NaN;
                    }
                    if (!isNaN(v)) {
                        anyNotNan = true;
                    } else {}
                    args.push(v);
                }
                //                console.log("anyNot:" + anyNotNan);
                //                console.log(args);
                try {
                    var result = NaN;
                    if (anyNotNan) {
                        result = d.compiledFunction(args);
                        if (d.decimals >= 0) {
                            result = result.toFixed(d.decimals);
                        }
                        result = parseFloat(result);
                    } else {
                        //                        console.log("NAN");
                    }
                    //                    console.log("in:" + result +" out: " + result);
                    values.push(result);
                } catch (e) {
                    console.log("Error evaluating function:" + d["function"] + "\n" + e);
                    values.push(NaN);
                }
            }
        }





        for (var fieldIdx = 0; fieldIdx < offsetFields.length; fieldIdx++) {
            var field = offsetFields[fieldIdx];
            var offset = field.offset;
            var value = values[field.getIndex()];
            value = (value + offset.offset1) * offset.scale + offset.offset2;
            values[field.getIndex()] = value;
        }


        rows.push(values);
        var record = new PointRecord(tuple.latitude, tuple.longitude, tuple.elevation, date, values);
        pointRecords.push(record);
    }



    for (var dIdx = 0; dIdx < derived.length; dIdx++) {
        var d = derived[dIdx];
        if (!d.isColumn) continue;
        var f = d["function"];
        var funcParams = [];
        //TODO: allow for no columns and choose all
        var params = d.columns.split(",");
        for (var i = 0; i < params.length; i++) {
            var index = -1;
            for (var fIdx = 0; fIdx < fields.length; fIdx++) {
                var f = fields[fIdx];
                //                console.log("F:" + f.getId() +" " + f.getLabel() );
                if (f.getId() == params[i] || f.getLabel() == params[i]) {
                    index = f.getIndex();
                    break;
                }
            }
            if (index < 0) {
                console.log("Could not find column index for field: " + params[i]);
                continue;
            }
            funcParams.push("c" + (i + 1));
        }
        var columnData = RecordUtil.slice(rows, index);
        //        var newData = A.movingAverage(columnData,{step:100});
        var daFunk = new Function(funcParams, d["function"]);
        console.log("daFunk - " + daFunk);
        var newData = daFunk(columnData);
        console.log("got new:" + newData + " " + (typeof newData));
        if ((typeof newData) == "number") {
            for (var rowIdx = 0; rowIdx < pointRecords.length; rowIdx++) {
                var record = pointRecords[rowIdx];
                record.push(newData);
            }
        } else if (Utils.isDefined(newData.length)) {
            console.log("newData.length:" + newData.length + " records.length:" + pointRecords.length);
            for (var rowIdx = 0; rowIdx < newData.length; rowIdx++) {
                var record = pointRecords[rowIdx];
                if (!record) {
                    console.log("bad record: " + rowIdx);
                    record.push(NaN);
                } else {
                    //                    console.log("    date:" + record.getDate() +" v: " + newData[rowIdx]);
                    var v = newData[rowIdx];
                    if (d.decimals >= 0) {
                        v = parseFloat(v.toFixed(d.decimals));
                    }
                    record.push(v);
                }
            }
        }
    }

    if (source != null) {
        for (var i = 0; i < fields.length; i++) {
            var field = fields[i];
            var prefix = "field." + field.getId() + ".";
            if (Utils.isDefined(source[prefix + "unit"])) {
                field.setUnit(source[prefix + "unit"]);
            }
            if (Utils.isDefined(source[prefix + "label"])) {
                field.setLabel(source[prefix + "label"]);
            }
            if (Utils.isDefined(source[prefix + "scale"]) || Utils.isDefined(source[prefix + "offset1"]) || Utils.isDefined(source[prefix + "offset2"])) {
                var offset1 = Utils.isDefined(source[prefix + "offset1"]) ? parseFloat(source[prefix + "offset1"]) : 0;
                var offset2 = Utils.isDefined(source[prefix + "offset2"]) ? parseFloat(source[prefix + "offset2"]) : 0;
                var scale = Utils.isDefined(source[prefix + "scale"]) ? parseFloat(source[prefix + "scale"]) : 1;
                var index = field.getIndex();
                for (var rowIdx = 0; rowIdx < pointRecords.length; rowIdx++) {
                    var record = pointRecords[rowIdx];
                    var values = record.getData();
                    var value = values[index];
                    values[index] = (value + offset1) * scale + offset2;
                }
            }

        }
    }



    var name = json.name;
    if ((typeof name === 'undefined')) {
        name = "Point Data";
    }

    pointRecords.sort(function(a, b) {
        if (a.getDate() && b.getDate()) {
            if (a.getDate().getTime() < b.getDate().getTime()) return -1;
            if (a.getDate().getTime() > b.getDate().getTime()) return 1;
            return 0;
        }
    });

    return new PointData(name, fields, pointRecords);
}






function makeTestPointData() {
    var json = {
        fields: [{
                index: 0,
                id: "field1",
                label: "Field 1",
                type: "double",
                missing: "-999.0",
                unit: "m"
            },

            {
                index: 1,
                id: "field2",
                label: "Field 2",
                type: "double",
                missing: "-999.0",
                unit: "c"
            },
        ],
        data: [
            [-64.77, -64.06, 45, null, [8.0, 1000]],
            [-65.77, -64.06, 45, null, [9.0, 500]],
            [-65.77, -64.06, 45, null, [10.0, 250]],
        ]
    };

    return makePointData(json);

}









/*
function InteractiveDataWidget (theChart) {
    this.jsTextArea =  id + "_js_textarea";
    this.jsSubmit =  id + "_js_submit";
    this.jsOutputId =  id + "_js_output";
        var jsInput = "<textarea rows=10 cols=80 id=\"" + this.jsTextArea +"\"/><br><input value=\"Try it out\" type=submit id=\"" + this.jsSubmit +"\">";

        var jsOutput = "<div id=\"" + this.jsOutputId +"\"/>";
        $("#" + this.jsSubmit).button().click(function(event){
                var js = "var chart = ramaddaGlobalChart;\n";
                js += "var data = chart.pointData.getData();\n";
                js += "var fields= chart.pointData.getRecordFields();\n";
                js += "var output= \"#" + theChart.jsOutputId  +"\";\n";
                js += $("#" + theChart.jsTextArea).val();
                eval(js);
            });
        html += "<table width=100%>";
        html += "<tr valign=top><td width=50%>";
        html += jsInput;
        html += "</td><td width=50%>";
        html += jsOutput;
        html += "</td></tr></table>";
*/



function RecordFilter(properties) {
    if (properties == null) properties = {};
    RamaddaUtil.defineMembers(this, {
        properties: properties,
        recordOk: function(display, record, values) {
            return true;
        }
    });
}


function MonthFilter(param) {
    RamaddaUtil.inherit(this, new RecordFilter());
    RamaddaUtil.defineMembers(this, {
        months: param.split(","),
        recordOk: function(display, record, values) {
            for (i in this.months) {
                var month = this.months[i];
                var date = record.getDate();
                if (date == null) return false;
                if (date.getMonth == null) {
                    //console.log("bad date:" + date);
                    return false;
                }
                if (date.getMonth() == month) return true;
            }
            return false;
        }
    });
}


var A = {
    add: function(v1, v2) {
        if (isNaN(v1) || isNaN(v2)) return NaN;
        return v1 + v2;
    },

    average: function(values) {
        var sum = 0;
        if (values.length == 0) return 0;
        for (var i = 0; i < values.length; i++) {
            sum += values[i];
        }
        return sum / values.length;
    },
    percentIncrease: function(values) {
        var percents = [];
        var sum = 0;
        if (values.length == 0) return 0;
        var lastValue;
        for (var i = 0; i < values.length; i++) {
            var v = values[i];
            var incr = NaN;
            if (i > 0 && lastValue != 0) {
                incr = (v - lastValue) / lastValue;
            }
            lastValue = v;
            percents.push(incr * 100);
        }
        return percents;
    },
    movingAverage: function(values, props) {
        if (!props) {
            props = {};
        }
        if (!props.step) props.step = 5;
        var newValues = [];
        console.log("STEP:" + props.step);
        for (var i = props.step; i < values.length; i++) {
            var total = 0;
            var cnt = 0;
            for (var j = i - props.step; j < i; j++) {
                if (values[j] == values[j]) {
                    total += values[j];
                    cnt++;
                }
            }
            var value = (cnt > 0 ? total / cnt : NaN);
            if (newValues.length == 0) {
                for (var extraIdx = 0; extraIdx < props.step; extraIdx++) {
                    newValues.push(value);
                }
            }
            newValues.push(value);
        }

        console.log("MovingAverage: values:" + values.length + " new:" + newValues.length);
        return newValues;
    },
    expMovingAverage: function(values, props) {
        if (!props) {
            props = {};
        }
        if (!props.step) props.step = 5;
        var sma = A.movingAverage(values, props);
        var mult = (2.0 / (props.step + 1));
        var newValues = [];
        console.log("STEP:" + props.step);
        for (var i = props.step; i < values.length; i++) {
            var total = 0;
            var cnt = 0;
            for (var j = i - props.step; j < i; j++) {
                if (values[j] == values[j]) {
                    total += values[j];
                    cnt++;
                }
            }
            var value = (cnt > 0 ? total / cnt : NaN);
            if (newValues.length == 0) {
                for (var extraIdx = 0; extraIdx < props.step; extraIdx++) {
                    newValues.push(value);
                }
            }
            newValues.push(value);
        }

        console.log("MovingAverage: values:" + values.length + " new:" + newValues.length);
        return newValues;
    },

    max: function(values) {
        var max = NaN;
        for (var i = 0; i < values.length; i++) {
            if (i == 0 || values[i] > max) {
                max = values[i];
            }
        }
        return max;
    },
    min: function(values) {
        var min = NaN;
        for (var i = 0; i < values.length; i++) {
            if (i == 0 || values[i] < min) {
                min = values[i];
            }
        }
        return min;
    },

}

var RecordUtil = {
    getRanges: function(fields, records) {
        var maxValues = [];
        var minValues = [];
        for (var i = 0; i < fields.length; i++) {
            maxValues.push(NaN);
            minValues.push(NaN);
        }

        for (var row = 0; row < records.length; row++) {
            for (var col = 0; col < fields.length; col++) {
                var value = records[row].getValue(col);
                if (isNaN(value)) continue;
                maxValues[col] = (isNaN(maxValues[col]) ? value : Math.max(value, maxValues[col]));
                minValues[col] = (isNaN(minValues[col]) ? value : Math.min(value, minValues[col]));
            }
        }

        var ranges = [];
        for (var col = 0; col < fields.length; col++) {
            ranges.push([minValues[col], maxValues[col]]);
        }
        return ranges;
    },



    getElevationRange: function(fields, records) {
        var maxValue = NaN;
        var minValue = NaN;

        for (var row = 0; row < records.length; row++) {
            if (records[row].hasElevation()) {
                var value = records[row].getElevation();
                maxValue = (isNaN(maxValue) ? value : Math.max(value, maxValue));
                minValue = (isNaN(minValue) ? value : Math.min(value, minValue));
            }
        }
        return [minValue, maxValue];
    },


    slice: function(records, index) {
        var values = [];
        for (var rowIdx = 0; rowIdx < records.length; rowIdx++) {
            var row = records[rowIdx];
            if (row.getValue) {
                values.push(row.getValue(index));
            } else {
                values.push(row[index]);
            }
        }
        return values;
    },


    sort: function(fields) {
        fields = fields.slice(0);
        fields.sort(function(a, b) {
            var s1 = a.getSortOrder();
            var s2 = b.getSortOrder();
            return s1 < s2;
        });
        return fields;
    },
    getPoints: function(records, bounds) {
        var points = [];
        var north = NaN,
            west = NaN,
            south = NaN,
            east = NaN;
        if (records != null) {
            for (j = 0; j < records.length; j++) {
                var record = records[j];
                if (!isNaN(record.getLatitude()) && !isNaN(record.getLongitude())) {
                    if (j == 0) {
                        north = record.getLatitude();
                        south = record.getLatitude();
                        west = record.getLongitude();
                        east = record.getLongitude();
                    } else {
                        north = Math.max(north, record.getLatitude());
                        south = Math.min(south, record.getLatitude());
                        west = Math.min(west, record.getLongitude());
                        east = Math.max(east, record.getLongitude());
                    }
                    if (record.getLongitude() < -180 || record.getLatitude() > 90) {
                        console.log("Bad index=" + j + " " + record.getLatitude() + " " + record.getLongitude());
                    }
                    points.push(new OpenLayers.Geometry.Point(record.getLongitude(), record.getLatitude()));
                }
            }
        }
        bounds.north = north;
        bounds.west = west;
        bounds.south = south;
        bounds.east = east;
        return points;
    },
    findClosest: function(records, lon, lat, indexObj) {
        if (records == null) return null;
        var closestRecord = null;
        var minDistance = 1000000000;
        var index = -1;
        for (j = 0; j < records.length; j++) {
            var record = records[j];
            if (isNaN(record.getLatitude())) {
                continue;
            }
            var distance = Math.sqrt((lon - record.getLongitude()) * (lon - record.getLongitude()) + (lat - record.getLatitude()) * (lat - record.getLatitude()));
            if (distance < minDistance) {
                minDistance = distance;
                closestRecord = record;
                index = j;
            }
        }
        if (indexObj != null) {
            indexObj.index = index;
        }
        return closestRecord;
    },
    clonePoints: function(points) {
        var result = [];
        for (var i = 0; i < points.length; i++) {
            var point = points[i];
            result.push(new OpenLayers.Geometry.Point(point.x, point.y));
        }
        return result;
    }
};/**
Copyright 2008-2019 Geode Systems LLC
*/


var DISPLAY_FILTER = "filter";
var DISPLAY_ANIMATION = "animation";
var DISPLAY_LABEL = "label";


addGlobalDisplayType({
    type: DISPLAY_FILTER,
    label: "Filter",
    requiresData: false,
    category: "Controls"
});
addGlobalDisplayType({
    type: DISPLAY_ANIMATION,
    label: "Animation",
    requiresData: false,
    category: "Controls"
});
addGlobalDisplayType({
    type: DISPLAY_LABEL,
    label: "Text",
    requiresData: false,
    category: "Misc"
});




function RamaddaFilterDisplay(displayManager, id, properties) {
    RamaddaUtil.inherit(this, new RamaddaDisplay(displayManager, id, properties));
    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
        html: "<p>&nbsp;&nbsp;&nbsp;Nothing selected&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<p>",
        initDisplay: function() {
            this.createUI();
            this.setContents(this.html);
        },
    });
}


function RamaddaAnimationDisplay(displayManager, id, properties) {
    var ID_START = "start";
    var ID_STOP = "stop";
    var ID_TIME = "time";
    RamaddaUtil.inherit(this, new RamaddaDisplay(displayManager, id, DISPLAY_ANIMATION, properties));
    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
        running: false,
        timestamp: 0,
        index: 0,
        sleepTime: 500,
        iconStart: ramaddaBaseUrl + "/icons/display/control.png",
        iconStop: ramaddaBaseUrl + "/icons/display/control-stop-square.png",
        iconBack: ramaddaBaseUrl + "/icons/display/control-stop-180.png",
        iconForward: ramaddaBaseUrl + "/icons/display/control-stop.png",
        iconFaster: ramaddaBaseUrl + "/icons/display/plus.png",
        iconSlower: ramaddaBaseUrl + "/icons/display/minus.png",
        iconBegin: ramaddaBaseUrl + "/icons/display/control-double-180.png",
        iconEnd: ramaddaBaseUrl + "/icons/display/control-double.png",
        deltaIndex: function(i) {
            this.stop();
            this.setIndex(this.index + i);
        },
        setIndex: function(i) {
            if (i < 0) i = 0;
            this.index = i;
            this.applyStep(true, !Utils.isDefined(i));
        },
        toggle: function() {
            if (this.running) {
                this.stop();
            } else {
                this.start();
            }
        },
        tick: function() {
            if (!this.running) return;
            this.index++;
            this.applyStep();
            var theAnimation = this;
            setTimeout(function() {
                theAnimation.tick();
            }, this.sleepTime);
        },
        applyStep: function(propagate, goToEnd) {
            if (!Utils.isDefined(propagate)) propagate = true;
            var data = this.displayManager.getDefaultData();
            if (data == null) return;
            var records = data.getRecords();
            if (records == null) {
                $("#" + this.getDomId(ID_TIME)).html("no records");
                return;
            }
            if (goToEnd) this.index = records.length - 1;
            if (this.index >= records.length) {
                this.index = 0;
            }
            var record = records[this.index];
            var label = "";
            if (record.getDate() != null) {
                var dttm = this.formatDate(record.getDate(), {
                    suffix: this.getTimeZone()
                });
                label += HtmlUtils.b("Date:") + " " + dttm;
            } else {
                label += HtmlUtils.b("Index:") + " " + this.index;
            }
            $("#" + this.getDomId(ID_TIME)).html(label);
            if (propagate) {
                this.displayManager.propagateEventRecordSelection(this, data, {
                    index: this.index
                });
            }
        },
        handleEventRecordSelection: function(source, args) {
            var data = this.displayManager.getDefaultData();
            if (data == null) return;
            if (data != args.data) {
                return;
            }
            if (!data) return;
            this.index = args.index;
            this.applyStep(false);
        },
        faster: function() {
            this.sleepTime = this.sleepTime / 2;
            if (this.sleepTime == 0) this.sleepTime = 100;
        },
        slower: function() {
            this.sleepTime = this.sleepTime * 1.5;
        },
        start: function() {
            if (this.running) return;
            this.running = true;
            this.timestamp++;
            $("#" + this.getDomId(ID_START)).attr("src", this.iconStop);
            this.tick();
        },
        stop: function() {
            if (!this.running) return;
            this.running = false;
            this.timestamp++;
            $("#" + this.getDomId(ID_START)).attr("src", this.iconStart);
        },
        initDisplay: function() {
            this.createUI();
            this.stop();

            var get = this.getGet();
            var html = "";
            html += HtmlUtils.onClick(get + ".setIndex(0);", HtmlUtils.image(this.iconBegin, [ATTR_TITLE, "beginning", ATTR_CLASS, "display-animation-button", "xwidth", "32"]));
            html += HtmlUtils.onClick(get + ".deltaIndex(-1);", HtmlUtils.image(this.iconBack, [ATTR_TITLE, "back 1", ATTR_CLASS, "display-animation-button", "xwidth", "32"]));
            html += HtmlUtils.onClick(get + ".toggle();", HtmlUtils.image(this.iconStart, [ATTR_TITLE, "play/stop", ATTR_CLASS, "display-animation-button", "xwidth", "32", ATTR_ID, this.getDomId(ID_START)]));
            html += HtmlUtils.onClick(get + ".deltaIndex(1);", HtmlUtils.image(this.iconForward, [ATTR_TITLE, "forward 1", ATTR_CLASS, "display-animation-button", "xwidth", "32"]));
            html += HtmlUtils.onClick(get + ".setIndex();", HtmlUtils.image(this.iconEnd, [ATTR_TITLE, "end", ATTR_CLASS, "display-animation-button", "xwidth", "32"]));
            html += HtmlUtils.onClick(get + ".faster();", HtmlUtils.image(this.iconFaster, [ATTR_CLASS, "display-animation-button", ATTR_TITLE, "faster", "xwidth", "32"]));
            html += HtmlUtils.onClick(get + ".slower();", HtmlUtils.image(this.iconSlower, [ATTR_CLASS, "display-animation-button", ATTR_TITLE, "slower", "xwidth", "32"]));
            html += HtmlUtils.div(["style", "display:inline-block; min-height:24px; margin-left:10px;", ATTR_ID, this.getDomId(ID_TIME)], "&nbsp;");
            this.setDisplayTitle("Animation");
            this.setContents(html);
        },
    });
}


function RamaddaLabelDisplay(displayManager, id, properties) {
    var ID_TEXT = "text";
    var ID_EDIT = "edit";
    var SUPER;
    if (properties && !Utils.isDefined(properties.showTitle)) {
        properties.showTitle = false;
    }

    RamaddaUtil.inherit(this, SUPER = new RamaddaDisplay(displayManager, id, DISPLAY_LABEL, properties));
    addRamaddaDisplay(this);
    this.text = "";
    this.editMode = properties.editMode;
    if (properties.text) this.text = properties.text;
    else if (properties.label) this.text = properties.label;
    else if (properties.html) this.text = properties.html;
    if (properties["class"]) this["class"] = properties["class"];
    else this["class"] = "display-text";

    RamaddaUtil.defineMembers(this, {
        initDisplay: function() {
            var theDisplay = this;
            this.createUI();
            var textClass = this["class"];
            if (this.editMode) {
                textClass += " display-text-edit ";
            }
            var style = "color:" + this.getTextColor("contentsColor") + ";";
            var html = HtmlUtils.div([ATTR_CLASS, textClass, ATTR_ID, this.getDomId(ID_TEXT), "style", style], this.text);
            if (this.editMode) {
                html += HtmlUtils.textarea(ID_EDIT, this.text, ["rows", 5, "cols", 120, ATTR_SIZE, "120", ATTR_CLASS, "display-text-input", ATTR_ID, this.getDomId(ID_EDIT)]);
            }
            this.setContents(html);
            if (this.editMode) {
                var editObj = this.jq(ID_EDIT);
                editObj.blur(function() {
                    theDisplay.text = editObj.val();
                    editObj.hide();
                    theDisplay.initDisplay();
                });
                this.jq(ID_TEXT).click(function() {
                    var src = theDisplay.jq(ID_TEXT);
                    var edit = theDisplay.jq(ID_EDIT);
                    edit.show();
                    edit.css('z-index', '9999');
                    edit.position({
                        of: src,
                        my: "left top",
                        at: "left top",
                        collision: "none none"
                    });
                    theDisplay.jq(ID_TEXT).html("");
                });
            }


        },
        getWikiAttributes: function(attrs) {
            SUPER.getWikiAttributes(attrs);
            attrs.push("text");
            attrs.push(this.text);
        },
    });
}/**
Copyright 2008-2019 Geode Systems LLC
*/

var DISPLAY_NOTEBOOK = "notebook";
addGlobalDisplayType({
    type: DISPLAY_NOTEBOOK,
    label: "Notebook",
    requiresData: false,
    category: "Misc"
});

var pluginDefintions = {
    'jsx': {
        "languageId": "jsx",
        "displayName": "React JSX",
        "url": "https://raw.githubusercontent.com/hamilton/iodide-jsx/master/docs/evaluate-jsx.js",
        "module": "jsx",
        "evaluator": "evaluateJSX",
        "pluginType": "language"
    },
    "lisp": {
        "languageId": "lisp",
        "displayName": "Microtalk Lisp",
        "url": "https://ds604.neocities.org/js/microtalk.js",
        "module": "MICROTALK",
        "evaluator": "evaluate",
        "pluginType": "language",
        "outputHandler": "processLispOutput",
    },
    "sql": {
        "languageId": "sql",
        "displayName": "SqlLite",
        "url": "${htdocs}/lib/notebook/sqllite.js",
        "module": "SqlLite",
        "evaluator": "evaluate",
        "pluginType": "language"
    },
    "plantuml": {
        "languageId": "plantuml",
        "displayName": "PlantUml",
        "codeMirrorMode": "",
        "keybinding": "x",
        "url": "https://raw.githubusercontent.com/six42/iodide-plantuml-plugin/master/src/iodide-plantuml-plugin.js",
        "depends": [{
            "type": "js",
            "url": "https://raw.githubusercontent.com/johan/js-deflate/master/rawdeflate.js"
        }],
        "module": "plantuml",
        "evaluator": "plantuml_img",
        "pluginType": "language"
    },
    "ml": {
        "languageId": "ml",
        "displayName": "ocaml",
        "codeMirrorMode": "mllike",
        "keybinding": "o",
        "url": "https://louisabraham.github.io/domical/eval.js",
        "module": "evaluator",
        "evaluator": "execute",
        "pluginType": "language",
        "depends": [{
            "type": "css",
            "url": "https://louisabraham.github.io/domical/style.css"
        }]
    }
};




function RamaddaNotebookDisplay(displayManager, id, properties) {
    var ID_NOTEBOOK = "notebook";
    var ID_IMPORTS = "imports";
    var ID_CELLS = "cells";
    var ID_CELLS_BOTTOM = "cellsbottom";
    var ID_INPUTS = "inputs";
    var ID_OUTPUTS = "outputs";
    var ID_CONSOLE = "console";
    var ID_CONSOLE_TOOLBAR = "consoletoolbar";
    var ID_CONSOLE_CONTAINER = "consolecontainer";
    var ID_CONSOLE_OUTPUT = "consoleout";
    var ID_CELL = "cell";
    var ID_MENU = "menu";
    this.properties = properties || {};
    let SUPER = new RamaddaDisplay(displayManager, id, DISPLAY_NOTEBOOK, properties);
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
        runOnLoad: this.getProperty("runOnLoad", true),
        displayMode: this.getProperty("displayMode", false),
        showConsole: this.getProperty("showConsole", true),
        consoleHidden: this.getProperty("consoleHidden", false),
        layout: this.getProperty("layout", "horizontal"),
        columns: this.getProperty("columns", 1),
    });

    RamaddaUtil.defineMembers(this, {
        cells: [],
        cellCount: 0,
        fetchedNotebook: false,
        currentEntries: {},
        globals: {},
        baseEntries: {},
        outputRenderers: [],
        initDisplay: async function() {
            this.createUI();
            var imports = HtmlUtils.div(["id", this.getDomId(ID_IMPORTS)]);
            var contents = imports + HtmlUtils.div([ATTR_CLASS, "display-notebook-cells", ATTR_ID, this.getDomId(ID_CELLS)], "&nbsp;&nbsp;Loading...") +
                HtmlUtils.div([ATTR_ID, this.getDomId(ID_CELLS_BOTTOM)]);
            var popup = HtmlUtils.div(["class", "ramadda-popup", ATTR_ID, this.getDomId(ID_MENU)]);
            contents = HtmlUtils.div([ATTR_ID, this.getDomId(ID_NOTEBOOK)], popup + contents);
            this.setContents(contents);
            this.makeCellLayout();
            this.jq(ID_NOTEBOOK).hover(() => {}, () => {
                this.jq(ID_MENU).hide()
            });
            if (!this.fetchedNotebook) {
                this.initOutputRenderers();
                if (!this.fetchingNotebook) {
                    this.fetchingNotebook = true;
                    await Utils.importCSS(ramaddaBaseHtdocs + "/lib/fontawesome/font-awesome.css");
                    await Utils.importJS(ramaddaBaseHtdocs + "/lib/ace/src-min/ace.js");
                    await Utils.importJS(ramaddaBaseUrl + "/lib/showdown.min.js");
                    var imports = "<link rel='preload' href='https://cdn.jsdelivr.net/npm/katex@0.10.1/dist/fonts/KaTeX_Main-Regular.woff2' as='font' type='font/woff2' crossorigin='anonymous'>\n<link rel='preload' href='https://cdn.jsdelivr.net/npm/katex@0.10.1/dist/fonts/KaTeX_Math-Italic.woff2' as='font' type='font/woff2' crossorigin='anonymous'>\n<link rel='preload' href='https://cdn.jsdelivr.net/npm/katex@0.10.1/dist/fonts/KaTeX_Size2-Regular.woff2' as='font' type='font/woff2' crossorigin='anonymous'>\n<link rel='preload' href='https://cdn.jsdelivr.net/npm/katex@0.10.1/dist/fonts/KaTeX_Size4-Regular.woff2' as='font' type='font/woff2' crossorigin='anonymous'/>\n<link rel='stylesheet' href='https://fonts.googleapis.com/css?family=Lato:300,400,700,700i'>\n<link rel='stylesheet' href='https://cdn.jsdelivr.net/npm/katex@0.10.1/dist/katex.min.css' crossorigin='anonymous'>\n<link rel='stylesheet' href='static/index.css'><script defer src='https://cdn.jsdelivr.net/npm/katex@0.10.1/dist/katex.min.js' crossorigin='anonymous'></script>";
                    $(imports).appendTo("head");
                    setTimeout(() => this.fetchNotebook(1), 10);
                }
            } else {
                this.layoutCells();
            }
        },
        fetchNotebook: async function(cnt) {
            if (!window["ace"]) {
                if (cnt > 50) {
                    alert("Could not load ace.js");
                    return;
                }
                setTimeout(() => this.fetchNotebook(cnt + 1), cnt * 10);
                return;
            }
            var dttm = new Date().getTime();
            ace.config.set('basePath', ramaddaBaseUrl + "/htdocs_v" + dttm + "/lib/ace/src-min");
            let _this = this;
            this.fetchedNotebook = true;
            await this.getEntry(this.getProperty("entryId", ""), entry => {
                this.baseEntry = entry;
            });
            await this.baseEntry.getRoot(entry => {
                this.rootEntry = entry;
            });
            var id = this.getProperty("entryId", "");
            var url = ramaddaBaseUrl + "/getnotebook?entryid=" + id;
            url += "&notebookId=" + this.getProperty("notebookId", "default_notebook");
            var jqxhr = $.getJSON(url, function(data) {
                _this.loadJson(data);
            }).fail(function() {
                var props = {
                    showInput: true,
                }
                this.addCell("init cell", props, false).run();
                this.cells[0].focus();
            });

        },
        formatObject: function(value) {
            return Utils.formatJson(value);
        },
        initOutputRenderers: function() {
            let notebook = this;
            this.outputRenderers = [];
            /*
            this.addOutputRenderer({
                    shouldRender: (value) => {return typeof value === "object";},
                        render: (value) => {if(Array.isArray(value)) return HtmlUtils.div(["style"," white-space: pre;"], JSON.stringify(value)); return HtmlUtils.div(["style"," white-space: pre;"],JSON.stringify(value,null,2))},
                        });
            */
            this.addOutputRenderer({
                shouldRender: (value) => {
                    return Array.isArray(value);
                },
                render: (value) => {
                    return Utils.formatJson(value);
                },
            });
            this.addOutputRenderer({
                shouldRender: (value) => {
                    return Array.isArray(value) && value.length > 0 && Array.isArray(value[0]);
                },
                render: (value) => {
                    var table = "<table>";
                    for (var rowIdx = 0; rowIdx < value.length; rowIdx++) {
                        var row = value[rowIdx];
                        table += "<tr>";
                        for (var colIdx = 0; colIdx < row.length; colIdx++) {
                            table += "<td>&nbsp;" + row[colIdx] + "</td>";
                        }
                        table += "</tr>";
                    }
                    table += "</table>";
                    return table;

                }
            });


            this.addOutputRenderer({
                shouldRender: (value) => {
                    return typeof value === "object" && value.getTime;
                },
                render: (value) => {
                    return notebook.formatDate(value)
                },
            });

            this.addOutputRenderer({
                shouldRender: (value) => {
                    var t = typeof value;
                    return t === "string" || t === "number" || t === "boolean";
                },
                render: (value) => {
                    if (typeof value === "string") {
                        if (value.split("\n").length > 1) {
                            return HtmlUtils.div(["style", " white-space: pre;"], value);
                        }
                    }
                    return value
                },
            });
            this.addOutputRenderer({
                shouldRender: (value) => {
                    return typeof value === "object" && "lat" in value && "lon" in value;
                },
                render: (value) => {
                    var url = 'http://staticmap.openstreetmap.de/staticmap.php?center=' + value.lat + ',' + value.lon + '&zoom=17&size=400x150&maptype=mapnik';
                    return "<img src='" + url + "'/>"
                },
            });

        },
        addOutputRenderer: function(renderer) {
            if (this.outputRenderers.indexOf(renderer) < 0) {
                this.outputRenderers.push(renderer);
            }
        },
        formatOutput: function(value) {
            if (!value) return null;
            for (var i = this.outputRenderers.length - 1; i >= 0; i--) {
                var renderer = this.outputRenderers[i];
                if (renderer.shouldRender && renderer.shouldRender(value)) {
                    return renderer.render(value);
                }
            }
            var v = null;
            if (value.iodideRender) {
                v = value.iodideRender();
            } else if (value.notebookRender) {
                v = value.notebookRender();
            }
            if (v) {
                //TODO handle elements
                if (typeof v == "string") {
                    return v;
                }
            }
            return null;
        },
        getBaseEntry: function() {
            return this.baseEntry;
        },
        getRootEntry: function() {
            return this.rootEntry;
        },
        getPopup: function() {
            return this.jq(ID_MENU);
        },
        loadJson: async function(data) {
            if (data.error) {
                this.setContents(_this.getMessage("Failed to load notebook: " + data.error));
                return;
            }
            if (!Utils.isDefined(this.properties.runOnLoad) && Utils.isDefined(data.runOnLoad)) {
                this.runOnLoad = data.runOnLoad;
            }
            if (!Utils.isDefined(this.properties.displayMode) && Utils.isDefined(data.displayMode)) {
                this.displayMode = data.displayMode;
            }
            if (!Utils.isDefined(this.properties.showConsole) && Utils.isDefined(data.showConsole)) {
                this.showConsole = data.showConsole;
            }
            if (Utils.isDefined(data.consoleHidden)) {
                this.consoleHidden = data.consoleHidden;
            }
            if (!Utils.isDefined(this.properties.columns) && Utils.isDefined(data.columns)) {
                this.columns = data.columns;
            }
            if (!Utils.isDefined(this.properties.layout) && Utils.isDefined(data.layout)) {
                this.layout = data.layout;
            }

            if (Utils.isDefined(data.currentEntries)) {
                for (a in data.currentEntries) {
                    var obj = {};
                    if (this.currentEntries[a]) continue;
                    obj.name = a;
                    obj.entryId = data.currentEntries[a].entryId;
                    try {
                        await this.getEntry(obj.entryId, e => obj.entry = e);
                        this.currentEntries[a] = obj;
                    } catch (e) {}
                }
            }
            if (Utils.isDefined(data.cells)) {
                this.cells = [];
                data.cells.map(cell => this.addCell(cell.outputHtml, cell, true));
                this.layoutCells();
            }
            if (this.cells.length == 0) {
                var props = {
                    showInput: true,
                }
                this.addCell("%%wiki\n", props, false);
                this.layoutCells();
                this.cells[0].focus();
            }
            if (this.runOnLoad) {
                this.runAll();
            }
        },
        addEntry: async function(name, entryId) {
            var entry;
            await this.getEntry(entryId, e => entry = e);
            this.currentEntries[name] = {
                entryId: entryId,
                entry: entry
            };
        },
        getCurrentEntries: function() {
            return this.currentEntries;
        },
        clearEntries: function() {
            this.currentEntries = {};
            for (a in this.baseEntries)
                this.currentEntries[a] = this.baseEntries[a];
        },
        saveNotebook: function(output) {
            var json = this.getJson(output);
            json = JSON.stringify(json, null, 2);
            var args = {
                entryid: this.getProperty("entryId", ""),
                notebookId: this.getProperty("notebookId", "default_notebook"),
                notebook: json
            };
            var url = ramaddaBaseUrl + "/savenotebook";
            $.post(url, args, (result) => {
                if (result.error) {
                    alert("Error saving notebook: " + result.error);
                    return;
                }
                if (result.result != "ok") {
                    alert("Error saving notebook: " + result.result);
                    return;
                }
                if (!this.getShowConsole()) {
                    alert("Notebook saved");
                } else {
                    this.log("Notebook saved", "info", "nb");
                }
            });
        },
        showInput: function() {
            if (this.displayMode && !this.user) {
                return false;
            }
            if (this.getProperty("showInput", true) == false)
                return false;
            return true;
        },
        getJson: function(output) {
            var obj = {
                cells: [],
                currentEntries: {},
                runOnLoad: this.runOnLoad,
                displayMode: this.displayMode,
                showConsole: this.showConsole,
                consoleHidden: this.consoleHidden,
                layout: this.layout,
                columns: this.columns,
            };
            for (var name in this.currentEntries) {
                var e = this.currentEntries[name];
                obj.currentEntries[name] = {
                    entryId: e.entryId
                };
            }
            this.cells.map(cell => obj.cells.push(cell.getJson(output)));
            return obj;
        },
        initConsole: function() {
            if (!this.showInput()) {
                return;
            }
            let _this = this;
            this.console = this.jq(ID_CONSOLE_OUTPUT);
            if (this.consoleHidden)
                this.console.hide();
            this.jq(ID_CONSOLE).find(".ramadda-image-link").click(function(e) {
                var what = $(this).attr("what");
                if (what == "clear") {
                    _this.console.html("");
                }
                e.stopPropagation();
            });

            this.consoleToolbar = this.jq(ID_CONSOLE_TOOLBAR);
            this.consoleToolbar.click(() => {
                if (this.console.is(":visible")) {
                    this.console.hide(400);
                    this.consoleHidden = true;
                } else {
                    this.consoleHidden = false;
                    this.console.show(400);
                }
            });
        },
        getShowConsole: function() {
            return this.showInput() && this.showConsole;
        },
        makeConsole: function() {
            this.console = null;
            if (!this.getShowConsole()) {
                return "";
            }
            var contents = this.jq(ID_CONSOLE_OUTPUT).html();
            var consoleToolbar = HtmlUtils.div(["id", this.getDomId(ID_CONSOLE_TOOLBAR), "class", "display-notebook-console-toolbar", "title", "click to hide/show console"],
                HtmlUtils.leftRight("",
                    HtmlUtils.span(["class", "ramadda-image-link", "title", "Clear", "what", "clear"],
                        HtmlUtils.image(Utils.getIcon("clear.png")))));
            return HtmlUtils.div(["id", this.getDomId(ID_CONSOLE), "class", "display-notebook-console"],
                consoleToolbar +
                HtmlUtils.div(["class", "display-notebook-console-output", "id", this.getDomId(ID_CONSOLE_OUTPUT)], contents || ""));
        },

        makeCellLayout: function() {
            var html = "";
            var consoleContainer = HtmlUtils.div(["id", this.getDomId(ID_CONSOLE_CONTAINER)]);
            this.jq(ID_CELLS_BOTTOM).html("");
            if (this.showInput() && this.layout == "horizontal") {
                var left = HtmlUtils.div(["id", this.getDomId(ID_INPUTS), "style", "width:100%;"]);
                var right = HtmlUtils.div(["id", this.getDomId(ID_OUTPUTS), "style", "width:100%;"]);
                var center = HtmlUtils.div([], "");
                left += consoleContainer;
                html = "<table style='table-layout:fixed;' border=0 width=100%><tr valign=top><td width=50%>" + left + "</td><td style='border-left:1px #ccc solid;' width=1>" + center + "</td><td width=49%>" + right + "</td></tr></table>";
            } else {
                this.jq(ID_CELLS_BOTTOM).html(consoleContainer);
            }
            this.jq(ID_CELLS).html(html);
        },
        plugins: {},
        addPlugin: async function(plugin, chunk) {
            var error;
            if (plugin.depends) {
                for (var i = 0; i < plugin.depends.length; i++) {
                    var obj = plugin.depends[i];
                    var type = obj.type;
                    var url = obj.url;
                    if (type == "js") {
                        await Utils.importJS(url,
                            () => {},
                            (jqxhr, settings, exception) => {
                                error = "Error fetching plugin url:" + url;
                            });
                    } else if (type == "css") {
                        await Utils.importCSS(url,
                            () => {},
                            (jqxhr, settings, exception) => {
                                error = "Error fetching plugin url:" + url;
                            });
                    }
                    if (error) {
                        this.log(error, "error", "nb", chunk ? chunk.div : null);
                        return;
                    }
                }
            }

            var url = Utils.replaceRoot(plugin.url);
            await Utils.importJS(url,
                () => {},
                (jqxhr, settings, exception) => {
                    error = "Error fetching plugin url:" + url;
                });
            if (!error) {
                var module = plugin.module;
                var tries = 200;
                //Wait 20 seconds max
                while (window[module] == null && tries-- > 0) {
                    await new Promise(resolve => setTimeout(resolve, 100));
                }
                if (!window[module]) {
                    error = "Could not load plugin module: " + module;
                } else {
                    if (window[module].isPluginReady) {
                        var tries = 200;
                        while (!window[module].isPluginReady() && tries-- > 0) {
                            //                            console.log("not ready yet:" + tries);
                            await new Promise(resolve => setTimeout(resolve, 100));
                        }
                        //                        console.log("final ready:" + window[module].isPluginReady() );
                        if (!window[module].isPluginReady())
                            error = "Could not load plugin module: " + module;
                    }
                }
            }
            if (error) {
                this.log(error, "error", "nb", chunk ? chunk.div : null);
                return;
            }
            this.plugins[plugin.languageId] = plugin;
        },
        hasPlugin: async function(id, callback) {
            if (!this.plugins[id]) {
                if (window.pluginDefintions[id]) {
                    await this.addPlugin(window.pluginDefintions[id], null);
                }
            }
            Utils.call(callback, this.plugins[id] != null);
        },
        processChunkWithPlugin: async function(id, chunk, callback) {
            var module = this.plugins[id].module;
            var func = this.plugins[id].evaluator;
            var result = window[module][func](chunk.getContent(), chunk);
            return Utils.call(callback, result);

        },
        processPluginOutput: function(id, chunk, result) {
            if (!result) return;
            var module = this.plugins[id].module;
            var func = window[this.plugins[id].outputHandler];
            if (func) {
                chunk.div.append(func(result));
            } else {
                if (typeof result == "object") {
                    //TODO: for now don't format this as some results are recursive
                    //                   console.log(result);
                    //                   chunk.div.set(this.formatObject(result));
                } else {
                    chunk.div.set(result);
                }
            }
        },
        log: function(msg, type, from, div) {
            var icon = "";
            var clazz = "display-notebook-console-item";
            if (typeof msg == "object") {
                msg = Utils.formatJson(msg);
            }
            if (type == "error") {
                clazz += " display-notebook-console-item-error";
                icon = HtmlUtils.image(Utils.getIcon("cross-octagon.png"));
                if (div) {
                    div.append(HtmlUtils.div(["class", "display-notebook-chunk-error"], msg));
                }
            } else if (type == "output") {
                clazz += " display-notebook-console-item-output";
                icon = HtmlUtils.image(Utils.getIcon("arrow-000-small.png"));
            } else if (type == "info") {
                clazz += " display-notebook-console-item-info";
                icon = HtmlUtils.image(Utils.getIcon("information.png"));
            }
            if (!this.console) return;
            if (!from) from = "";
            else from = HtmlUtils.div(["class", "display-notebook-console-from"], from);
            var block = HtmlUtils.div(["style", "margin-left:5px;"], msg);
            var html = "<table width=100%><tr valign=top><td width=10>" + icon + "</td><td>" +
                block +
                "</td><td width=10>" +
                from +
                "</td></tr></table>";
            var item = HtmlUtils.div(["class", clazz], html);
            this.console.append(item);
            //200 is defined in display.css
            var height = this.console.prop('scrollHeight');
            if (height > 200)
                this.console.scrollTop(height - 200);
        },
        clearConsole: function() {
            this.console.html("");
        },
        layoutCells: function() {
            for (var i = 0; i < this.cells.length; i++) {
                var cell = this.cells[i];
                cell.prepareToLayout();
            }
            this.makeCellLayout();
            if (this.showInput() && this.layout == "horizontal") {
                var left = "";
                var right = "";
                var id;
                for (var i = 0; i < this.cells.length; i++) {
                    var cell = this.cells[i];
                    id = cell.id;
                    cell.index = i + 1;
                    left += HtmlUtils.div([ATTR_CLASS, "display-notebook-cell", ATTR_ID, cell.id + "_cellinput"], "");
                    left += "\n";
                    right += HtmlUtils.div([ATTR_CLASS, "display-notebook-cell", ATTR_ID, cell.id + "_celloutput"], "");
                }
                this.jq(ID_INPUTS).html(left);
                this.jq(ID_OUTPUTS).html(right);
            } else {
                var html = "<div class=row style='padding:0px;margin:0px;'>";
                var clazz = HtmlUtils.getBootstrapClass(this.columns);
                var colCnt = 0;
                for (var i = 0; i < this.cells.length; i++) {
                    var cell = this.cells[i];
                    cell.index = i + 1;
                    html += HtmlUtils.openTag("div", ["class", clazz]);
                    html += HtmlUtils.openTag("div", ["style", "max-width:100%;overflow-x:auto;padding:0px;margin:px;"]);
                    html += HtmlUtils.div([ATTR_CLASS, "display-notebook-cell", ATTR_ID, cell.id + "_cellinput"], "");
                    html += HtmlUtils.div([ATTR_CLASS, "display-notebook-cell", ATTR_ID, cell.id + "_celloutput"], "");
                    html += HtmlUtils.closeTag("div");
                    html += HtmlUtils.closeTag("div");
                    html += "\n";
                    colCnt++;
                    if (colCnt >= this.columns) {
                        colCnt = 0;
                        html += HtmlUtils.closeTag("div");
                        html += "<div class=row style='padding:0px;margin:0px;'>";
                    }
                };
                html += HtmlUtils.closeTag("div");
                this.jq(ID_CELLS).append(html);
            }
            for (var i = 0; i < this.cells.length; i++) {
                var cell = this.cells[i];
                cell.createCell();
            };
            this.jq(ID_CONSOLE_CONTAINER).html(this.makeConsole());
            this.initConsole();
        },
        addCell: function(content, props, layoutLater) {
            cell = this.createCell(content, props);
            this.cells.push(cell);
            if (!layoutLater) {
                if (this.showInput() && this.layout == "horizontal") {
                    this.jq(ID_INPUTS).append(HtmlUtils.div([ATTR_CLASS, "display-notebook-cell", ATTR_ID, cell.id + "_cellinput"], ""));
                    this.jq(ID_OUTPUTS).append(HtmlUtils.div([ATTR_CLASS, "display-notebook-cell", ATTR_ID, cell.id + "_celloutput"], ""));
                } else {
                    this.jq(ID_CELLS).append(HtmlUtils.div([ATTR_CLASS, "display-notebook-cell", ATTR_ID, cell.id + "_cellinput"], ""));
                    this.jq(ID_CELLS).append(HtmlUtils.div([ATTR_CLASS, "display-notebook-cell", ATTR_ID, cell.id + "_celloutput"], ""));
                }
                cell.createCell();
            }
            return cell;
        },
        createCell: function(content, props) {
            if (!props) props = {
                showInput: true
            };
            var cellId = this.getId() + "_" + this.cellCount;
            //Override any saved id
            props.id = cellId;
            this.cellCount++;
            this.jq(ID_CELLS).append(HtmlUtils.div([ATTR_CLASS, "display-notebook-cell", ATTR_ID, cellId], ""));
            var cell = new RamaddaNotebookCell(this, cellId, content, props);
            return cell;
        },
        clearOutput: function() {
            this.cells.map(cell => cell.clearOutput());
        },
        getIndex: function(cell) {
            var idx = 0;
            for (var i = 0; i < this.cells.length; i++) {
                if (cell.id == this.cells[i].id) {
                    idx = i;
                    break;
                }
            }
            return idx;
        },
        moveCellUp: function(cell) {
            var cells = [];
            var newCell = null;
            var idx = this.getIndex(cell);
            if (idx == 0) return;
            this.cells.splice(idx, 1);
            this.cells.splice(idx - 1, 0, cell);
            this.layoutCells();
            cell.focus();
        },
        moveCellDown: function(cell) {
            var cells = [];
            var newCell = null;
            var idx = this.getIndex(cell);
            if (idx == this.cells.length - 1) return;
            this.cells.splice(idx, 1);
            this.cells.splice(idx + 1, 0, cell);
            this.layoutCells();
            cell.focus();
        },

        newCellAbove: function(cell) {
            var cells = [];
            var newCell = null;
            for (var i = 0; i < this.cells.length; i++) {
                if (cell.id == this.cells[i].id) {
                    newCell = this.createCell("%%wiki\n", {
                        showInput: true,
                        showHeader: false
                    });
                    cells.push(newCell);
                }
                cells.push(this.cells[i]);
            }
            this.cells = cells;
            this.layoutCells();
            newCell.focus();
        },

        newCellBelow: function(cell) {
            var cells = [];
            var newCell = null;
            for (var i = 0; i < this.cells.length; i++) {
                cells.push(this.cells[i]);
                if (cell.id == this.cells[i].id) {
                    newCell = this.createCell("%%wiki\n", {
                        showInput: true,
                        showHeader: false
                    });
                    cells.push(newCell);
                }
            }
            this.cells = cells;
            this.layoutCells();
            newCell.focus();
        },
        deleteCell: function(cell) {
            cell.jq(ID_CELL).remove();
            var cells = [];
            this.cells.map(c => {
                if (cell.id != c.id) {
                    cells.push(c);
                }
            });
            this.cells = cells;
            if (this.cells.length == 0) {
                this.addCell("", null);
            }
        },
        cellValues: {},
        setCellValue: function(name, value) {
            this.cellValues[name] = value;
        },
        getCellValues: function() {
            return this.cellValues;
        },
        convertInput: function(input) {
            for (name in this.cellValues) {
                var re = new RegExp("\\$\\{" + name + "\\}", "g");
                input = input.replace(re, this.cellValues[name]);
            }
            return input;
        },
        inGlobalChanged: false,
        globalChanged: async function(name, value) {
                var globalChangeCalled = this.inGlobalChanged;
                var top = !this.inGlobalChanged;
                if(this.inRunAll) {
                    top =  false;
                }
                this.inGlobalChanged=true;
                if(top) {
                    this.cells.map(cell=>cell.prepareToRun());
                }
                for(var i=0;i<this.cells.length;i++) {
                    await this.cells[i].globalChanged(name,value);
                }
                if(!globalChangeCalled) {
                    this.inGlobalChanged = false;
                }
        },
        addGlobal: async function(name, value, dontPropagate) {
            //TODO: more var name cleanup
            name = name.trim().replace(/[ -]/g, "_");
            var oldValue = this.getGlobalValue(name);
            if (Utils.isDefined(window[name])) window[name] = value;
            this.globals[name] = value;
            if(!dontPropagate) {
                var newValue = this.getGlobalValue(name);
                if(newValue!=oldValue) {
                    await this.globalChanged(name, newValue);
                }
            }
        },
        getGlobalValue: function(name) {
                if(!this.globals[name]) return null;
                if(typeof this.globals[name] =="function") return this.globals[name]();
                return this.globals[name];
        },
        inRunAll: false,
        runAll: async function() {
            this.inRunAll = true;
            var ok = true;
            this.cellValues = {};
            for (var i = 0; i < this.cells.length; i++) {
                var cell = this.cells[i];
                cell.prepareToRun();
            }
            for (var i = 0; i < this.cells.length; i++) {
                var cell = this.cells[i];
                if (!cell.runFirst) continue;
                await this.runCell(cell).then(result => ok = result);
            }
            if (!ok) return;
            for (var i = 0; i < this.cells.length; i++) {
                var cell = this.cells[i];
                if (cell.runFirst) continue;
                await this.runCell(cell, true).then(result => ok = result);
            }
            this.inRunAll = false;
        },
        runCell: async function(cell, doingAll) {
            if (cell.hasRun) return true;
            await cell.run(result => ok = result, {
                doingAll: doingAll
            });
            if (!ok) return false;
            var raw = cell.getRawOutput();
            if (raw) {
                raw = raw.trim();
                if (Utils.stringDefined(cell.cellName)) {
                    this.cellValues[cell.cellName] = raw;
                }
            }
            return true;
        },
        toggleAll: function(on) {
            this.cells.map(cell => {
                cell.showInput = on;
                cell.applyStyle();
            });
        },

    });
}




var iodide = {
    addOutputRenderer: function(renderer) {
        notebook.addOutputRenderer(renderer);
    },
    addOutputHandler: function(renderer) {
        notebook.addOutputHandler(renderer);
    },
    output: {
        text: function(t) {
            notebook.write(t);
        },
        element: function(tag) {
            var id = HtmlUtils.getUniqueId();
            notebook.write(HtmlUtils.tag(tag, ["id", id]));
            return document.getElementById(id);
        }
    },
};

var notebook;


function NotebookState(cell, div) {
    this.id = HtmlUtils.getUniqueId();
    this.cell = cell;
    this.notebook = cell.notebook;
    $.extend(this, {
        entries: {},
        div: div,
        stopFlag: false,
        result: null,
        log: function(msg, type, from) {
            this.getNotebook().log(msg, type, from, this.div);
        },
        clearConsole: function() {
            this.getNotebook().clearConsole();
        },
        getStop: function() {
            return this.stopFlag;
        },
        getCell: function() {
            return this.cell;
        },
        addGlobal: async function(name,value) {
                await this.getNotebook().addGlobal(name,value);
        },

        globalChanged: async function(name,value) {
                await this.getNotebook().globalChanged(name,value);
        },
        setValue: function(name, value) {
            this.notebook.setCellValue(name, value);
        },
        makeData: async function(entry) {
            if (!entry)
                await this.getCurrentEntry(e => entry = e);
            if ((typeof entry) == "string") {
                await this.notebook.getEntry(entry, e => entry = e);
            }
            var jsonUrl = this.notebook.getPointUrl(entry);
            if (jsonUrl == null) {
                this.writeError("Not a point type:" + entry.getName());
                return null;
            }
            var pointDataProps = {
                entry: entry,
                entryId: entry.getId()
            };
            return new PointData(entry.getName(), null, null, jsonUrl, pointDataProps);
        },
        log: function(msg, type) {
            this.getNotebook().log(msg, type, "js");
        },
        getNotebook: function() {
            return this.notebook;
        },

        save: function(output) {
            this.notebook.saveNotebook(output);
            return "notebook saved";
        },

        clearEntries: function() {
            this.clearEntries();
        },

        ls: async function(entry) {
            var div = new Div();
            if (!entry)
                await this.getCurrentEntry(e => entry = e);
            this.call.getEntryHeading(entry, div);
            this.write(div.toString());
        },

        lsEntries: function() {
            var h = "";
            var entries = this.currentEntries;
            for (var name in entries) {
                var e = entries[name];
                h += name + "=" + e.entry.getName() + "<br>";
            }
            this.write(h);
        },

        stop: function() {
            this.stopFlag = true;
        },
        setGlobal: async function(name, value) {
                await this.cell.notebook.addGlobal(name, value);
        },
        setEntry: function(name, entryId) {
            this.cell.notebook.addEntry(name, entryId);
        },
        getEntry: async function(entryId, callback) {
            await this.cell.notebook.getEntry(e => entry = e);
            return Utils.call(callback, entry);
        },
        wiki: async function(s, entry, callback) {
            if (!callback) {
                var wdiv = new Div();
                this.div.append(wdiv.toString());
                callback = h => wdiv.append(h);
            }
            if (entry == null)
                await this.cell.getCurrentEntry(e => entry = e);
            if ((typeof entry) != "string") entry = entry.getId();
            await GuiUtils.loadHtml(ramaddaBaseUrl + "/wikify?doImports=false&entryid=" + entry + "&text=" + encodeURIComponent(s),
                callback);
        },
        //These are for the iodiode mimic
        addOutputRenderer: function(renderer) {
            this.getNotebook().addOutputRenderer(renderer);
        },
        addOutputHandler: function(renderer) {
            this.getNotebook().addOutputRenderer(renderer);
        },
        output: {
            text: function(t) {
                notebook.write(t);
            },
            element: function(tag) {
                var id = HtmlUtils.getUniqueId();
                notebook.write(HtmlUtils.tag(tag, ["id", id]));
                return document.getElementById(id);
            }
        },
        clearOutput: function() {
            this.cell.clearOutput();
        },
        clearAllOutput: function() {
            this.getNotebook().clearOutput();
        },
        write: function(value, clear) {
            if (!value) return;
            var s = this.getNotebook().formatOutput(value);
            if (s == null && (typeof value) == "object") {
                s = this.notebook.formatObject(value);
            }
            if (clear)
                this.div.set(s);
            else
                this.div.append(s);
        },
        linechart: async function(entry, props) {
            if (!entry)
                await this.cell.getCurrentEntry(e => entry = e);
            this.cell.createDisplay(this, entry, DISPLAY_LINECHART, props);
        },
    });
}


var notebookStates = {};

function RamaddaNotebookCell(notebook, id, content, props) {
    this.notebook = notebook;

    var ID_CELL = "cell";
    var ID_HEADER = "header";
    var ID_CELLNAME = "cellname";
    var ID_INPUT = "input";
    var ID_INPUT_TOOLBAR = "inputtoolbar";
    var ID_OUTPUT = "output";
    var ID_MESSAGE = "message";
    var ID_BUTTON_MENU = "menubutton";
    var ID_BUTTON_RUN = "runbutton";
    var ID_BUTTON_TOGGLE = "togglebutton";
    var ID_MENU = "menu";
    var ID_CELLNAME_INPUT = "cellnameinput";
    var ID_SHOWHEADER_INPUT = "showheader";
    var ID_SHOWEDIT = "showedit";
    var ID_RUN_ON_LOAD = "runonload";
    var ID_DISPLAY_MODE = "displaymode";
    var ID_LAYOUT_TYPE = "layouttype";
    var ID_SHOWCONSOLE = "showconsole";
    var ID_LAYOUT_COLUMNS = "layoutcolumns";
    var ID_RUNFIRST = "runfirst";
    var ID_SHOW_OUTPUT = "showoutput";
    var ID_RUN_ICON = "runningicon";

    let SUPER = new DisplayThing(id, {});
    RamaddaUtil.inherit(this, SUPER);

    RamaddaUtil.defineMembers(this, {
        id: id,
        inputRows: 1,
        index: 0,
        content: content,
        outputHtml: "",
        showInput: false,
        showHeader: false,
        cellName: "",
        runFirst: false,
        showOutput: true,
    });

    if (props) {
        $.extend(this, props);
    }
    RamaddaUtil.defineMembers(this, {
        getJson: function(output) {
            var obj = {
                id: this.id,
                inputRows: this.inputRows,
                content: this.getInputText(),
                showInput: this.showInput,
                showHeader: this.showHeader,
                runFirst: this.runFirst,
                showOutput: this.showOutput,
                cellName: this.cellName,
            };
            if (this.currentEntry)
                obj.currentEntryId = this.currentEntry.getId();
            if (output)
                obj.outputHtml = this.outputHtml;
            return obj;
        },
        createCell: function() {
            if (this.content == null) {
                this.content = "%% wiki";
            }
            this.editId = addHandler(this);
            addHandler(this, this.editId + "_entryid");
            addHandler(this, this.editId + "_wikilink");
            var _this = this;
            var buttons =
                this.makeButton(ID_BUTTON_MENU, icon_menu, "Show menu", "showmenu") +
                this.makeButton(ID_BUTTON_RUN, Utils.getIcon("run.png"), "Run this cell", "run") +
                this.makeButton(ID_BUTTON_RUN, Utils.getIcon("runall.png"), "Run all", "runall");

            var runIcon = HtmlUtils.image(icon_blank, ["align", "right", "id", this.getDomId(ID_RUN_ICON), "style", "padding-bottom:2px;padding-top:2px;padding-right:5px;"]);
            buttons = buttons + "&nbsp;" + HtmlUtils.span(["id", this.getDomId(ID_CELLNAME)], this.cellName);
            buttons += runIcon;
            var header = HtmlUtils.div([ATTR_CLASS, "display-notebook-header", ATTR_ID, this.getDomId(ID_HEADER), "tabindex", "0", "title", "Click to toggle input\nShift-click to clear output"], "&nbsp;" + buttons);

            //Strip out the meta chunks
            var content = "";
            var lines = this.content.split("\n");
            var inMeta = false;
            for (var i = 0; i < lines.length; i++) {
                var line = lines[i];
                var _line = line.trim();
                if (_line.startsWith("%%")) {
                    if (_line.match(/^%% *meta/)) {
                        inMeta = true;
                    } else {
                        inMeta = false;
                    }
                }
                if (!inMeta) {
                    content += line + "\n";
                }
            }


            content = content.replace(/</g, "&lt;").replace(/>/g, "&gt;");
            var input = HtmlUtils.div([ATTR_CLASS, "display-notebook-input ace_editor", ATTR_ID, this.getDomId(ID_INPUT), "title", "shift-return: run chunk\nctrl-return: run to end"], content);
            var inputToolbar = HtmlUtils.div(["id", this.getDomId(ID_INPUT_TOOLBAR)], "");

            input = HtmlUtils.div(["class", "display-notebook-input-container"], inputToolbar + input);
            var output = HtmlUtils.div([ATTR_CLASS, "display-notebook-output", ATTR_ID, this.getDomId(ID_OUTPUT)], this.outputHtml);
            output = HtmlUtils.div(["class", "display-notebook-output-container"], output);
            var menu = HtmlUtils.div(["id", this.getDomId(ID_MENU), "class", "ramadda-popup"], "");
            var html = header + input;
            html = HtmlUtils.div(["id", this.getDomId(ID_CELL)], html);
            $("#" + this.id + "_cellinput").html(html);
            $("#" + this.id + "_celloutput").html(output);
            var url = ramaddaBaseUrl + "/wikitoolbar?entryid=" + this.entryId + "&handler=" + this.editId;
            url += "&extrahelp=" + ramaddaBaseUrl + "/userguide/notebook.html|Notebook Help";
            GuiUtils.loadHtml(url, h => {
                this.inputToolbar = h;
                this.jq(ID_INPUT_TOOLBAR).html(h);
                $("#" + this.editId + "_prefix").html(HtmlUtils.span(["id", this.getDomId("toolbar_notebook"),
                    "style", "border-right:1px #ccc solid;",
                    "class", "ramadda-menubar-button"
                ], "Notebook"));
                this.jq("toolbar_notebook").click(() => this.showNotebookMenu());

            });
            this.header = this.jq(ID_HEADER);
            this.header.click((e) => {
                if (e.shiftKey)
                    this.processCommand("clear");
                else {
                    this.hidePopup();
                    this.processCommand("toggle");
                }

            });

            this.editor = HtmlUtils.initAceEditor("", this.getDomId(ID_INPUT), false, {
                maxLines: 30,
                minLines: 5
            });
            this.editor.getSession().on('change', () => {
                this.inputChanged();
            });
            this.menuButton = this.jq(ID_BUTTON_MENU);
            this.toggleButton = this.jq(ID_BUTTON_TOGGLE);
            this.cell = this.jq(ID_CELL);
            this.input = this.jq(ID_INPUT);
            this.output = this.jq(ID_OUTPUT);
            this.inputContainer = this.cell.find(".display-notebook-input-container");
            this.inputMenu = this.cell.find(".display-notebook-input-container");
            this.applyStyle();
            this.header.find(".display-notebook-menu-button").click(function(e) {
                _this.processCommand($(this).attr("what"));
                e.stopPropagation();
            });

            this.calculateInputHeight();
            this.input.focus(() => this.hidePopup());
            this.input.click(() => this.hidePopup());
            this.output.click(() => this.hidePopup());
            this.input.on('input selectionchange propertychange', () => this.calculateInputHeight());
            var moveFunc = (e) => {
                var key = e.key;
                if (key == 'v' && e.ctrlKey) {
                    this.notebook.moveCellDown(_this);
                    return;
                }
                if (key == 6 && e.ctrlKey) {
                    this.notebook.moveCellUp(_this);
                    return;
                }

            };
            this.input.keydown(moveFunc);
            this.header.keydown(moveFunc);
            this.input.keydown(function(e) {
                var key = e.key;
                if (key == 's' && e.ctrlKey) {
                    _this.notebook.saveNotebook(false);
                    return;
                }
                if (key == 'Enter') {
                    //                    console.log(key +"  shift:"  + e.shiftKey +" ctrl:" + e.ctrlKey);
                    if (e.shiftKey || e.ctrlKey) {
                        if (e.preventDefault) {
                            e.preventDefault();
                        }
                        if (e.shiftKey && e.ctrlKey) {
                            //run all
                            _this.run(null);
                        } else {
                            //run current, run to end
                            _this.run(null, {
                                justCurrent: true,
                                toEnd: e.ctrlKey
                            });
                            if (!e.ctrlKey) {
                                _this.stepToNextChunk();
                            }
                        }
                    }
                }

            });
        },
        selectClick(type, id, entryId, value) {
            if (type == "entryid") {
                this.insertText(entryId);
            } else {
                this.insertText("[[" + entryId + "|" + value + "]]");
            }
            this.input.focus();
        },
        insertTags: function(tagOpen, tagClose, sampleText) {
            var id = this.getDomId(ID_INPUT);
            var textComp = GuiUtils.getDomObject(id);
            insertTagsInner(id, textComp.obj, tagOpen, tagClose, sampleText);
            this.calculateInputHeight();
        },
        insertText: function(value) {
            var id = this.getDomId(ID_INPUT);
            var textComp = GuiUtils.getDomObject(id);
            insertAtCursor(id, textComp.obj, value);
            this.calculateInputHeight();
        },
        showNotebookMenu: function() {
            var link = this.jq("toolbar_notebook");
            this.makeMenu(link, "left bottom");
        },
        makeButton: function(id, icon, title, command) {
            if (!command) command = "noop";
            return HtmlUtils.div(["what", command, "title", title, "class", "display-notebook-menu-button", "id", this.getDomId(id)], HtmlUtils.image(icon, []));
        },
        makeMenu: function(src, at) {
            if (!src) {
                src = this.input;
            }
            if (!src.is(":visible")) {
                src = this.header;
            }
            if (!src.is(":visible")) {
                src = this.output;
            }
            if (!at) at = "left top";
            let _this = this;
            var space = "&nbsp;&nbsp;";
            var line = "<div style='border-top:1px #ccc solid;margin-top:4px;margin-bottom:4px;'></div>"
            var menu = "";
            menu += HtmlUtils.input(ID_CELLNAME_INPUT, _this.cellName, ["placeholder", "Cell name", "style", "width:100%;", "id", _this.getDomId(ID_CELLNAME_INPUT)]);
            menu += "<br>";
            menu += "<table  width=100%> ";
            menu += "<tr><td align=right><b>New cell:</b>&nbsp;</td><td>";
            menu += HtmlUtils.div(["class", "ramadda-link", "what", "newabove"], "Above") + space;
            menu += HtmlUtils.div(["class", "ramadda-link", "what", "newbelow"], "Below");
            menu += "</td></tr>"
            menu += "<tr><td align=right><b>Move:</b>&nbsp;</td><td>";
            menu += HtmlUtils.div(["title", "ctrl-^", "class", "ramadda-link", "what", "moveup"], "Up") + space;
            menu += HtmlUtils.div(["title", "ctrl-v", "class", "ramadda-link", "what", "movedown"], "Down");
            menu += "</td></tr>"

            menu += "</table>";

            menu += line;
            menu += HtmlUtils.div(["title", "ctrl-return", "class", "ramadda-link", "what", "hideall"], "Hide all inputs");
            menu += "<br>"
            menu += HtmlUtils.div(["class", "ramadda-link", "what", "clearall"], "Clear all outputs");
            menu += "<br>";
            var cols = this.notebook.columns;
            var colId = _this.getDomId(ID_LAYOUT_COLUMNS);
            menu += "<b>Layout:</b> ";
            menu += HtmlUtils.checkbox(_this.getDomId(ID_LAYOUT_TYPE), [], _this.notebook.layout == "horizontal") + " Horizontal" + "<br>";
            //            menu += "Columns: ";
            //            menu += HtmlUtils.input(colId, this.notebook.columns, ["size", "3", "id", _this.getDomId(ID_LAYOUT_COLUMNS)]);
            menu += line;

            menu += HtmlUtils.checkbox(_this.getDomId(ID_SHOW_OUTPUT), [], _this.showOutput) + " Output enabled" + "<br>";
            menu += HtmlUtils.checkbox(_this.getDomId(ID_SHOWCONSOLE), [], _this.notebook.showConsole) + " Show console" + "<br>";

            menu += HtmlUtils.checkbox(_this.getDomId(ID_RUNFIRST), [], _this.runFirst) + " Run first" + "<br>";
            menu += HtmlUtils.checkbox(_this.getDomId(ID_RUN_ON_LOAD), [], _this.notebook.runOnLoad) + " Run on load" + "<br>";
            menu += HtmlUtils.div(["title", "Don't show the left side and input for anonymous users"], HtmlUtils.checkbox(_this.getDomId(ID_DISPLAY_MODE), [], _this.notebook.displayMode) + " Display mode" + "<br>");

            menu += line;
            menu += HtmlUtils.div(["class", "ramadda-link", "what", "savewithout"], "Save notebook") + "<br>";
            menu += line;
            menu += HtmlUtils.div(["class", "ramadda-link", "what", "delete"], "Delete cell") + "<br>";
            menu += HtmlUtils.div(["class", "ramadda-link", "what", "help"], "Help") + "<br>";
            menu = HtmlUtils.div(["class", "display-notebook-menu"], menu);


            var popup = this.getPopup();
            this.dialogShown = true;
            popup.html(HtmlUtils.div(["class", "ramadda-popup-inner"], menu));
            popup.show();
            popup.position({
                of: src,
                my: "left top",
                at: at,
                collision: "fit fit"
            });
            _this.jq(ID_SHOWHEADER_INPUT).focus();

            _this.jq(ID_SHOWCONSOLE).change(function(e) {
                _this.notebook.showConsole = _this.jq(ID_SHOWCONSOLE).is(':checked');
                _this.hidePopup();
                _this.notebook.layoutCells();
            });


            _this.jq(ID_SHOWHEADER_INPUT).change(function(e) {
                _this.showHeader = _this.jq(ID_SHOWHEADER_INPUT).is(':checked');
                _this.applyStyle();
            });


            _this.jq(ID_RUNFIRST).change(function(e) {
                _this.runFirst = _this.jq(ID_RUNFIRST).is(':checked');
            });

            _this.jq(ID_SHOW_OUTPUT).change(function(e) {
                _this.showOutput = _this.jq(ID_SHOW_OUTPUT).is(':checked');
                _this.applyStyle();
            });
            _this.jq(ID_RUN_ON_LOAD).change(function(e) {
                _this.notebook.runOnLoad = _this.jq(ID_RUN_ON_LOAD).is(':checked');
            });
            _this.jq(ID_DISPLAY_MODE).change(function(e) {
                _this.notebook.displayMode = _this.jq(ID_DISPLAY_MODE).is(':checked');
            });
            _this.jq(ID_SHOWEDIT).change(function(e) {
                _this.showInput = _this.jq(ID_SHOWEDIT).is(':checked');
                _this.applyStyle();
            });

            _this.jq(ID_LAYOUT_TYPE).change(function(e) {
                if (_this.jq(ID_LAYOUT_TYPE).is(':checked')) {
                    _this.notebook.layout = "horizontal";
                } else {
                    _this.notebook.layout = "vertical";
                }
                _this.hidePopup();
                _this.notebook.layoutCells();
            });
            _this.jq(ID_LAYOUT_COLUMNS).keypress(function(e) {
                var keyCode = e.keyCode || e.which;
                if (keyCode != 13) {
                    return;
                }
                var cols = parseInt(_this.jq(ID_LAYOUT_COLUMNS).val());
                if (isNaN(cols)) {
                    _this.jq(ID_LAYOUT_COLUMNS).val("bad:" + _this.jq(ID_LAYOUT_COLUMNS).val());
                    return;
                }
                _this.hidePopup();
            });
            _this.jq(ID_CELLNAME_INPUT).keypress(function(e) {
                var keyCode = e.keyCode || e.which;
                if (keyCode == 13) {
                    _this.hidePopup();
                    return;
                }
            });
            popup.find(".ramadda-link").click(function() {
                var what = $(this).attr("what");
                _this.processCommand(what);
            });
        },
        hidePopup: function() {
            var popup = this.getPopup();
            if (popup && this.dialogShown) {
                var cols = parseInt(this.jq(ID_LAYOUT_COLUMNS).val());
                this.cellName = this.jq(ID_CELLNAME_INPUT).val();
                this.jq(ID_CELLNAME).html(this.cellName);
                popup.hide();
                this.applyStyle();

                if (!isNaN(cols) && this.notebook.columns != cols) {
                    this.notebook.columns = cols;
                    this.notebook.layoutCells();
                }
            }
            this.dialogShown = false;
        },
        processCommand: function(command) {
            if (command == "showmenu") {
                this.makeMenu();
                return;
            } else if (command == "toggle") {
                this.showInput = !this.showInput;
                this.applyStyle(true);
            } else if (command == "showthis") {
                this.showInput = true;
                this.applyStyle();
            } else if (command == "hidethis") {
                this.showInput = false;
                this.applyStyle();
            } else if (command == "showall") {
                this.notebook.toggleAll(true);
            } else if (command == "hideall") {
                this.notebook.toggleAll(false);
            } else if (command == "run") {
                this.notebook.runCell(this);
            } else if (command == "runall") {
                this.notebook.runAll();
            } else if (command == "clear") {
                this.clearOutput();
            } else if (command == "clearall") {
                this.notebook.clearOutput();
            } else if (command == "moveup") {
                this.notebook.moveCellUp(this);
            } else if (command == "movedown") {
                this.notebook.moveCellDown(this);
            } else if (command == "newabove") {
                this.notebook.newCellAbove(this);
            } else if (command == "newbelow") {
                this.notebook.newCellBelow(this);
            } else if (command == "savewith") {
                this.notebook.saveNotebook(true);
            } else if (command == "savewithout") {
                this.notebook.saveNotebook(false);
            } else if (command == "help") {
                var win = window.open(ramaddaBaseUrl + "/userguide/notebook.html", '_blank');
                win.focus();
            } else if (command == "delete") {
                this.askDelete();
                return;
            } else {
                console.log("unknown command:" + command);
            }
            this.hidePopup();
        },
        shouldShowInput: function() {
            return this.showInput && this.notebook.showInput();
        },
        applyStyle: function(fromUser) {
            if (this.shouldShowInput()) {
                this.jq(ID_INPUT_TOOLBAR).css("display", "block");
                this.inputContainer.show(400, () => this.editor.resize());
                this.showHeader = true;
            } else {
                this.jq(ID_INPUT_TOOLBAR).css("display", "none");
                this.inputContainer.hide(fromUser ? 200 : 0);
                this.showHeader = false;
            }
            this.showHeader = this.notebook.showInput();
            if (this.showHeader) {
                this.header.css("display", "block");
            } else {
                this.header.css("display", "none");
            }
            if (this.showOutput) {
                this.output.css("display", "block");
            } else {
                this.output.css("display", "none");
            }
        },
        getPopup: function() {
            return this.notebook.getPopup();
        },
        askDelete: function() {
            let _this = this;
            var menu = "";
            menu += "Are you sure you want to delete this cell?<br>";
            menu += HtmlUtils.span(["class", "ramadda-link", "what", "yes"], "Yes");
            menu += HtmlUtils.span(["style", "margin-left:50px;", "class", "ramadda-link", "what", "cancel"], "No");
            var popup = this.getPopup();

            popup.html(HtmlUtils.div(["class", "ramadda-popup-inner"], menu));
            popup.show();
            var src = this.input;
            if (!src.is(":visible")) {
                src = this.output;
            }
            if (!src.is(":visible")) {
                src = this.header;
            }
            popup.position({
                of: src,
                my: "left top",
                at: "left top",
                collision: "fit fit"
            });
            popup.find(".ramadda-link").click(function() {
                var what = $(this).attr("what");
                _this.hidePopup();
                if (what == "yes") {
                    _this.notebook.deleteCell(_this);
                }
            });
        },
        inputChanged: function() {
            var value = this.getInputText();
            var lines = value.split("\n");
            var cursor = this.editor.getCursorPosition();
            for (var i = cursor.row; i >= 0; i--) {
                var line = lines[i].trim();
                if (line.startsWith("%%")) {
                    var type = line.substring(2).trim();
                    if (type.startsWith("md") || type.startsWith("html") || type.startsWith("css") || type.startsWith("raw")) {
                        var doRows = {};
                        doRows[i] = true;
                        this.runInner(value, doRows);
                    }
                    break;
                }
            }
        },
        stepToNextChunk: function() {
            var value = this.getInputText();
            var lines = value.split("\n");
            var cursor = this.editor.getCursorPosition();
            for (var i = cursor.row + 1; i < lines.length; i++) {
                if (lines[i].trim().startsWith("%%")) {
                    var ll = lines[i].length;
                    this.editor.selection.moveTo(i, ll);
                    this.editor.scrollToLine(i, true, true, function() {});
                    break;
                }
            }

        },
        run: async function(callback, args) {
            if (!args) args = {};
            var justCurrent = args.justCurrent;
            var toEnd = args.toEnd;
            var doingAll = args.doingAll;
            if (this.running) return Utils.call(callback, true);
            this.running = true;
            var doRows = null;
            try {
                var ok = true;
                var value = this.getInputText();
                if (justCurrent) {
                    doRows = {};
                    var cursor = this.editor.getCursorPosition();
                    var row = cursor.row;
                    var lines = value.split("\n");
                    var percentCnt = 0;
                    if (toEnd) {
                        justCurrent = false;
                        while (row >= 0) {
                            if (lines[row].trim().startsWith("%%")) {
                                break;
                            }
                            row--;
                        }
                        if (row < 0) row = 0;
                        while (row < lines.length) {
                            doRows[row] = true;
                            row++;
                        }
                    } else {
                        //go to the next chunk
                        row++;
                        while (row < lines.length) {
                            if (lines[row].trim().startsWith("%%")) {
                                row--;
                                break;
                            }
                            row++;
                        }
                        if (row >= lines.length) row = lines.length - 1;
                        while (row >= 0) {
                            var line = lines[row].trim();
                            doRows[row] = true;
                            if (line.startsWith("%%")) break;
                            row--;
                        }
                    }
                }

                this.jq(ID_RUN_ICON).attr("src", icon_progress);
                await this.runInner(value, doRows, doingAll).then(r => ok = r);
                this.jq(ID_RUN_ICON).attr("src", icon_blank);
                if (!ok) {
                    this.running = false;
                    return Utils.call(callback, false);
                }
                this.outputUpdated();
            } catch (e) {
                this.jq(ID_RUN_ICON).attr("src", icon_blank);
                this.running = false;
                this.writeOutput("An error occurred:" + e.toString() + " " + (typeof e));
                console.log("error:" + e.toString());
                if (e.stack)
                    console.log(e.stack);
                return Utils.call(callback, false);
            }
            this.running = false;
            return Utils.call(callback, true);
        },
        prepareToLayout: function() {
            this.content = this.getInputText();
        },
        getInputText: function() {
            if (!this.editor) return this.content;
            return this.editor.getValue();
        },
        globalChanged: async function(name, value) {
            for(var i=0;i<this.chunks.length;i++) {
                var chunk = this.chunks[i];
                if(chunk.hasRun) continue;
                if(chunk.depends.includes(name)) {
                   var ok = true;
                   await this.runChunk(chunk,r=>ok=r);
                   if(!ok) break;
                }
            }
        },
        prepareToRun: function() {
            this.hasRun = false;
            if(this.chunks) {
                this.chunks.map(chunk=>chunk.hasRun = false);
            }
        },
        runInner: async function(value, doRows, doingAll) {
            value = value.trim();
            value = value.replace(/{cellname}/g, this.cellName);
            value = this.notebook.convertInput(value);
            if (!this.chunks) this.chunks = [];
            var chunks = this.chunks;
            var type = "wiki";
            var rest = "";
            var commands = value.split("\n");
            var prevChunk = null;
            var chunkCnt = 0;
            var _cell = this;
            var getChunk = (cell,type, content,  doChunk, rest) => {
                var props = Utils.parseAttributes(rest);
                props.type = type;
                props.doChunk = doChunk;
                props.content   = content;
                var chunk = (chunkCnt < chunks.length ? chunks[chunkCnt] : null);
                chunkCnt++;
                if (chunk) {
                    if (chunk.div.jq().length == 0) {
                        chunk = null;
                    } else {}
                } else {}
                if (!chunk) {
                    chunk = new NotebookChunk(cell, props);
                    chunks.push(chunk);
                    if (prevChunk) prevChunk.div.jq().after(chunk.div.toString());
                    else cell.output.html(chunk.div.toString());
                } else {
                    chunk.initChunk(props);
                }
                prevChunk = chunk;
                chunk.div.jq().show();
                return chunk;
            };
            var content = "";
            var doChunk = true;
            for (var rowIdx = 0; rowIdx < commands.length; rowIdx++) {
                var command = commands[rowIdx];
                var _command = command.trim();
                if (_command.startsWith("//")) continue;
                if (_command.startsWith("%%")) {
                    var newRest = _command.substring(2).trim();
                    var newType;
                    var index = newRest.indexOf(" ");
                    if (index < 0) {
                        newType = newRest;
                        newRest = "";
                    } else {
                        newType = newRest.substring(0, index).trim();
                        newRest = newRest.substring(index);
                    }
                    if (content != "") {
                        getChunk(this, type, content, doChunk, rest);
                    }
                    doChunk = doRows ? doRows[rowIdx] : true;
                
                    content = "";
                    if (content != "") content += "\n";
                    if (newType != "")
                        type = newType;
                    rest = newRest;
                    continue;
                }
                content = content + command + "\n";
            }

            if (content != "") {
                getChunk(this,type, content, doChunk, rest);
            }

            this.chunkMap = {};
            for (var i = 0; i < this.chunks.length; i++) {
                var chunk = this.chunks[i];
                if (chunk.name) {
                    this.chunkMap[chunk.name] = chunk;
                }
            }
            for (var i = chunkCnt; i < this.chunks.length; i++) {
                this.chunks[i].div.jq().hide();
            }
            this.rawOutput = "";
            var ok = true;
            await this.runChunks(this.chunks, doingAll, true, r => ok = r);
            if (!ok) return false;
            await this.runChunks(this.chunks, doingAll, false, r => ok = r);
            if (!ok) return false;
            Utils.initContent("#" + this.getDomId(ID_OUTPUT));
            return true;
        },
        runChunks: async function(chunks, doingAll, justFirst, callback) {
            for (var i = 0; i < chunks.length; i++) {
                var chunk = chunks[i];
                var ok = true;
                if (justFirst === true && !chunk.props["runfirst"]) {
                    continue;
                }
                if (justFirst === false && chunk.props["runfirst"] === true) {
                    continue;
                }
                if (doingAll && chunk.props["skiprunall"] === true) {
                    continue;
                }
                if (!chunk.doChunk) {
                    continue;
                }
                await this.runChunk(chunk, (r => ok = r));
                if (!ok) return Utils.call(callback, false);
            }
            return Utils.call(callback, true);
        },
        runChunk: async function(chunk,   callback) {
            if (chunk.hasRun) {
                //                console.log("runChunk: chunk has run");
                return Utils.call(callback, true);
            }
            chunk.ok = true;
            chunk.div.set("");
            chunk.hasRun = true;
            for (var i = 0; i < chunk.depends.length; i++) {
                var name = chunk.depends[i];
                if (this.chunkMap[name] && !this.chunkMap[name].hasRun) {
                    var ok = true;
                    var otherChunk = this.chunkMap[name];
                    await this.runChunk(otherChunk, false, null, (r => ok = r));
                    if (!ok || !otherChunk.ok) {
                        return Utils.call(callback, false);
                    }
                }
            }
            await this.processChunk(chunk);
            if (!chunk.ok) {
                Utils.call(callback, false);
                return;
            }
            if (chunk.name && (typeof chunk.name == "string")) {
                var name = chunk.name.trim();
                if (chunk.output) {
                    if (name != "") {
                        await this.notebook.addGlobal(name, chunk.output);
                    }
                } else {
                    await this.notebook.addGlobal(name, null);
                }
            }
            return Utils.call(callback, true);
        },

        writeOutput: function(h) {
            if (!this.output) {
                err = new Error();
                console.log("no output:" + err.stack);
                return;
            }
            this.output.html(h);
            this.outputUpdated();
        },
        outputUpdated: function() {
            this.outputHtml = this.jq(ID_OUTPUT).html();
        },
        getRawOutput: function() {
            return this.rawOutput;
        },
        focus: function() {
            this.input.focus();
        },
        clearOutput: function() {
            if (this.chunks)
                this.chunks.map(chunk => chunk.div.set(""));
            this.outputHtml = "";
        },
        processHtml: async function(chunk) {
            var content = chunk.getContent();
            if (content.match("%\n*$")) {
                content = content.trim();
                content = content.substring(0, content.length - 1);
            }
            this.rawOutput += content + "\n";
            chunk.output = content;
            chunk.div.set(content);
        },
        processCss: async function(chunk) {
            var css = HtmlUtils.tag("style", ["type", "text/css"], chunk.getContent());
            this.rawOutput += css + "\n";
            chunk.output = css;
            chunk.div.set(css);
        },
        handleError: function(chunk, error, from) {
            chunk.ok = false;
            console.log("An error occurred:" + error);
            this.notebook.log(error, "error", from, chunk.div);
        },
        getFetchUrl: async function(url, type, callback) {
            //Check for entry id
            url = Utils.replaceRoot(url);
            if (url.match(/^[a-z0-9]+-[a-z0-9].*/)) {
                return Utils.call(callback, ramaddaBaseUrl + "/entry/get?entryid=" + url);
            } else {
                if (!url.startsWith("http")) {
                    if ((url.startsWith("/") && !url.startsWith(ramaddaBaseUrl)) || url.startsWith("..") || !url.startsWith("/")) {
                        var entry;
                        await this.getEntryFromPath(url, e => entry = e);
                        if (!entry) {
                            return Utils.call(callback, null);
                        }
                        return Utils.call(callback, ramaddaBaseUrl + "/entry/get?entryid=" + entry.getId());
                    }
                }
                return Utils.call(callback, url);
            }
        },
        processFetch: async function(chunk) {
            var lines = chunk.getContent().split("\n");
            for (var i = 0; i < lines.length; i++) {
                var line = lines[i].trim();
                if (line == "") continue;
                var origLine = line;
                var error = null;
                var msgExtra = "";
                var idx = line.indexOf(":");
                if (idx < 0) {
                    this.handleError(chunk, "Bad fetch line:" + line, "io");
                    return;
                }
                var tag = line.substring(0, idx);
                line = line.substring(idx + 1).trim();
                var idx = line.indexOf(" //");
                if (idx >= 0) {
                    line = line.substring(0, idx).trim();
                }


                var url = null;
                var variable = null;
                if (["text", "json", "blob"].includes(tag)) {
                    var args = line.match(/^([a-zA-Z0-9_]+) *= *(.*)$/);
                    if (args) {
                        variable = args[1];
                        line = args[2].trim();
                        msgExtra = " (var " + variable + ")";
                    }
                }

                await this.getFetchUrl(line, tag, u => url = u);
                if (!url) {
                    this.handleError(chunk, "Unable to get entry url:" + line, "io");
                    return;
                }

                if (tag == "js") {
                    //Don't import jquery
                    if (url.match("jquery-.*\\.js")) return;
                    await Utils.importJS(url,
                        () => {},
                        (jqxhr, settings, exception) => {
                            error = "Error fetching " + origLine + " " + (exception ? exception.toString() : "");
                        },
                        //Check the cache
                        false
                    );
                } else if (tag == "css") {
                    await Utils.importCSS(url,
                        null,
                        (jqxhr, settings, exception) => error = "Error fetching " + origLine + " " + exception, true);
                } else if (tag == "html") {
                    await Utils.doFetch(url, h => chunk.div.append(h), (jqxhr, settings, exception) => error = "Error fetching " + origLine + " " + exception);
                } else if (tag == "text" || tag == "json" || tag == "blob") {
                    var isJson = tag == "json";
                    var isBlob = tag == "blob";
                    var results = null;
                    await Utils.doFetch(url, h => results = h, (jqxhr, settings, err) => error = "Error fetching " + origLine + " error:" + (err ? err.toString() : ""), tag == "blob" ? "blob" : "text");
                    if (results) {
                        if (isJson) {
                            if (typeof results == "string")
                                results = JSON.parse(results);
                        } else if (isBlob) {
                            results = new Blob([results], {});
                        }
                        if (variable) {
                            await this.notebook.addGlobal(variable, results);
                        } else {
                            if (isJson) {
                                chunk.div.append(Utils.formatJson(results));
                            } else {
                                chunk.div.append(HtmlUtils.pre(["style", "max-width:100%;overflow-x:auto;"], results));
                            }
                        }
                    }
                } else {
                    error = "Unknown fetch:" + origLine;
                }
                if (error) {
                    this.handleError(chunk, error, "io");
                    return;
                } else {
                    this.notebook.log("Loaded: " + url + msgExtra, "output", "io");
                }
            }
        },
        processMd: async function(chunk) {
            //            await Utils.importJS(ramaddaBaseUrl + "/lib/katex/lib/katex/katex.min.css");
            //            await Utils.importJS(ramaddaBaseUrl + "/lib/katex/lib/katex/katex.min.js");

            var content = chunk.content;
            this.rawOutput += content + "\n";
            if (content.match("%\n*$")) {
                content = content.trim();
                content = content.substring(0, content.length - 1);
            }
            var o = "";
            var tex = null;
            var lines = content.split("\n");
            var texs = [];
            for (var i = 0; i < lines.length; i++) {
                var line = lines[i];
                var _line = line.trim();
                if (_line.startsWith("$$")) {
                    if (tex != null) {
                        try {
                            var html = katex.renderToString(tex, {
                                throwOnError: true
                            });
                            o += "tex:" + texs.length + ":\n";
                            texs.push(html);
                        } catch (e) {
                            o += "Error parsing tex:" + e + "<pre>" + tex + "</pre>";
                        }
                        tex = null;
                    } else {
                        tex = "";
                    }
                } else if (tex != null) {
                    tex += line + "\n";
                } else {
                    o += line + "\n";
                }
            }

            var converter = new showdown.Converter();
            var html = converter.makeHtml(o);
            for (var i = 0; i < texs.length; i++) {
                html = html.replace("tex:" + i + ":", texs[i]);
            }
            var md = HtmlUtils.div(["class", "display-notebook-md"], html);
            chunk.output = html;
            chunk.div.set(md);
        },
        processPy: async function(chunk) {
            if (!this.notebook.loadedPyodide) {
                chunk.div.set("Loading Python...");
                await Utils.importJS(ramaddaBaseHtdocs + "/lib/pyodide/pyodide.js");
                await languagePluginLoader.then(() => {
                    pyodide.runPython('import sys\nsys.version;');
                    //                        pyodide.runPython('print ("hello python")');
                }, (e) => console.log("error:" + e));
                await pyodide.loadPackage(['numpy', 'cycler', 'pytz', 'matplotlib'])
                chunk.div.set("");
                this.notebook.loadedPyodide = true;
            }

            pyodide.runPython(chunk.getContent());
        },
        processPlugin: async function(chunk) {
            var plugin = JSON.parse(chunk.getContent());
            await this.notebook.addPlugin(plugin, chunk);
        },
        processWiki: async function(chunk) {
            this.rawOutput += chunk.getContent() + "\n";
            var id = this.notebook.getProperty("entryId", "");
            await this.getCurrentEntry(e => entry = e);
            if (entry) id = entry.getId();
            let _this = this;
            let divId = HtmlUtils.getUniqueId();
            var wikiCallback = function(html) {
                var h = HtmlUtils.div(["id", divId, "style"], html);
                chunk.div.set(h);
                chunk.output = h;
            }
            var wiki = "{{group showMenu=false}}\n" + chunk.getContent();
            await GuiUtils.loadHtml(ramaddaBaseUrl + "/wikify?doImports=false&entryid=" + id + "&text=" + encodeURIComponent(chunk.getContent()),
                wikiCallback);
        },
        processSh: async function(chunk) {
            var r = "";
            var lines = chunk.getContent().split("\n");
            var commands = [];
            for (var i = 0; i < lines.length; i++) {
                var fullLine = lines[i].trim();
                if (fullLine == "") continue;
                var cmds = fullLine.split(";");
                for (var cmdIdx = 0; cmdIdx < cmds.length; cmdIdx++) {
                    var line = cmds[cmdIdx].trim();
                    if (line == "" || line.startsWith("#") || line.startsWith("//")) continue;
                    var toks = line.split(" ");

                    var command = toks[0].trim();
                    var proc = null;
                    var extra = null;
                    if (this["processCommand_" + command]) {
                        proc = this["processCommand_" + command];
                    } else {
                        proc = this.processCommand_help;
                        extra = "Unknown command: <i>" + command + "</i>";
                    }
                    var div = new Div("");
                    commands.push({
                        proc: proc,
                        line: line,
                        toks: toks,
                        extra: extra,
                        div: div
                    });
                    r += div.set("");
                }
            }
            let _this = this;
            chunk.div.set(r);
            var i = 0;
            for (i = 0; i < commands.length; i++) {
                var cmd = commands[i];
                if (cmd.extra) {
                    cmd.div.append(extra);
                }
                await cmd.proc.call(_this, cmd.line, cmd.toks, cmd.div, cmd.extra);
            }
        },
        processJs: async function(chunk,state) {
            var lines;
            var topLines = 0;
            await this.getCurrentEntry(e => {
                    current = e
                });
            if(!notebookStates[state.id]) {
                throw new Error("Null NB:" + state.id);
            }
            try {
                var notebookEntries = this.notebook.getCurrentEntries();
                for (name in notebookEntries) {
                    state.entries[name] = notebookEntries[name].entry;
                }
                var jsSet = "";
                state.entries["current"] = current;
                state.entries["parent"] = this.parentEntry;
                state.entries["base"] = this.notebook.getBaseEntry();
                state.entries["root"] = this.notebook.getRootEntry();

                var stateJS = "notebookStates['" + state.id + "']";
                topLines++;
                jsSet += "var notebook= " + stateJS + ";\n";
                topLines++;
                for (name in state.entries) {
                    var e = state.entries[name];
                    topLines++;
                    jsSet += "var " + name + "= notebook.entries['" + name + "'];\n"
                }
                for (name in this.notebook.cellValues) {
                    var clean = name.replace(/ /g, "_").replace(/[^a-zA-Z0-9_]+/g, "_");
                    topLines++;
                    jsSet += "var " + clean + "= notebook.getNotebook().cellValues['" + name + "'];\n";
                }
                for (name in this.notebook.globals) {
                    name = name.trim();
                    if (name == "") continue;
                    //                    if (!Utils.isDefined(window[name])) {
                        topLines++;
                        jsSet += "var " + name + "= notebook.getNotebook().getGlobalValue('" + name + "');\n";
                        //                    }
                }
                var js = chunk.getContent().trim();
                lines = js.split("\n");
                js = jsSet + "\n" + js;
                var result = eval.call(null, js);
                if (state.getStop()) {
                    chunk.ok = false;
                }
                var html = "";
                if (result != null) {
                    chunk.output = result;
                    var rendered = this.notebook.formatOutput(result);
                    if (rendered != null) {
                        html = rendered;
                        this.rawOutput += html + "\n";
                    } else {
                        var type = typeof result;
                        if (type != "object" && type != "function") {
                            html = result;
                            this.rawOutput += html + "\n";
                        }
                    }
                }
                chunk.div.append(html);
            } catch (e) {
                chunk.ok = false;
                var line = lines[e.lineNumber - topLines - 1];
                console.log("Error:" + e.stack);
                this.notebook.log("Error: " + e.message + "<br>&gt;" + (line ? line : ""), "error", "js", chunk.div);
            }
        },
        processChunk: async function(chunk) {
            var state = new NotebookState(this, chunk.div);
            window.notebook = state;
            notebookStates[state.id] = state;
            if (chunk.type == "html") {
                await this.processHtml(chunk, state);
            } else if (chunk.type == "plugin") {
                await this.processPlugin(chunk,state);
            } else if (chunk.type == "wiki") {
                await this.processWiki(chunk,state);
            } else if (chunk.type == "css") {
                await this.processCss(chunk,state);
            } else if (chunk.type == "fetch") {
                await this.processFetch(chunk,state);
            } else if (chunk.type == "raw") {
                var content = chunk.getContent();
                chunk.output = content;
                this.rawOutput += content;
            } else if (chunk.type == "js") {
                await this.processJs(chunk,state);
            } else if (chunk.type == "sh") {
                await this.processSh(chunk,state);
            } else if (chunk.type == "meta") {
                //noop
            } else if (chunk.type == "md") {
                await this.processMd(chunk,state);
            } else if (chunk.type == "py") {
                await this.processPy(chunk,state);
            } else {
                var hasPlugin;
                await this.notebook.hasPlugin(chunk.type, p => hasPlugin = p);
                if (hasPlugin) {
                    chunk.div.set("");
                    var result;
                    await this.notebook.processChunkWithPlugin(chunk.type, chunk, r => result = r);
                    //TODO: what to do with the result
                    if (result) {
                        this.notebook.processPluginOutput(chunk.type, chunk, result);
                    }
                    return;
                }
                this.notebook.log("Unknown type:" + chunk.type, "error", null, chunk.div);
                chunk.ok = false;
            }
            delete  notebookStates[state.id];
            if (state.getStop()) {
                chunk.ok = false;
            }

        },



        calculateInputHeight: function() {
            this.content = this.getInputText();
            if (!this.content) return;
            var lines = this.content.split("\n");
            if (lines.length != this.inputRows) {
                this.inputRows = lines.length;
                this.input.attr("rows", Math.max(1, this.inputRows));
            }
        },

        writeStatusMessage: function(v) {
            var msg = this.jq(ID_MESSAGE);
            if (!v) {
                msg.hide();
                msg.html("");
            } else {
                msg.show();
                msg.position({
                    of: this.getOutput(),
                    my: "left top",
                    at: "left+4 top+4",
                    collision: "none none"
                });
                msg.html(v);
            }
        },
        handleControlKey: function(event) {
            var k = event.which;
        },
        getOutput: function() {
            return this.jq(ID_OUTPUT);
        },
        getInput: function() {
            return this.jq(ID_INPUT);
        },
        writeResult: function(html) {
            this.writeStatusMessage(null);
            html = HtmlUtils.div([ATTR_CLASS, "display-notebook-result"], html);
            var output = this.jq(ID_OUTPUT);
            output.append(html);
            output.animate({
                scrollTop: output.prop("scrollHeight")
            }, 1000);
            this.currentOutput = output.html();
            this.currentInput = this.getInputText();
        },
        writeError: function(msg) {
            this.writeStatusMessage(msg);
            //                this.writeResult(msg);
        },
        header: function(msg) {
            return HtmlUtils.div([ATTR_CLASS, "display-notebook-header"], msg);
        },
        processCommand_help: function(line, toks, div, callback, prefix) {
            if (div == null) div = new Div();
            var help = "";
            if (prefix != null) help += prefix;
            help += "<pre>pwd, ls, cd</pre>";
            return div.append(help);
        },
        entries: {},

        selectEntry: function(entryId) {
            var cnt = 1;
            var entries = this.notebook.getCurrentEntries();
            while (entries["entry" + cnt]) {
                cnt++;
            }
            var id = prompt("Set an ID", "entry" + cnt);
            if (id == null || id.trim() == "") return;
            this.notebook.addEntry(id, entryId);
        },
        setId: function(entryId) {
            var cursor = this.editor.getCursorPosition();
            this.editor.insert(entryId);
            //            this.editor.selection.moveTo(cursor.row, cursor.column);
            //            this.editor.focus();
        },
        cdEntry: function(entryId) {
            var div = new Div("");
            this.currentEntry = this.entries[entryId];
            notebookState.entries["current"] = this.currentEntry;
            this.output.html(div.toString());
            this.processCommand_pwd("pwd", [], div);
            this.outputUpdated();
        },
        addToToolbar: function(id, entry, toolbarItems) {
            var call = "getHandler('" + id + "').setId('" + entry.getId() + "')";
            var icon = HtmlUtils.image(ramaddaBaseUrl + "/icons/setid.png", ["border", 0, ATTR_TITLE, "Set ID in Input"]);
            toolbarItems.unshift(HtmlUtils.tag(TAG_A, ["onclick", call], icon));
            var call = "getHandler('" + id + "').selectEntry('" + entry.getId() + "')";
            var icon = HtmlUtils.image(ramaddaBaseUrl + "/icons/circle-check.png", ["border", 0, ATTR_TITLE, "Select Entry"]);
            toolbarItems.unshift(HtmlUtils.tag(TAG_A, ["onclick", call], icon));
        },
        getEntryPrefix: function(id, entry) {
            this.entries[entry.getId()] = entry;
            var call = "getHandler('" + id + "').cdEntry('" + entry.getId() + "')";
            return HtmlUtils.div(["style", "padding-right:4px;", "title", "cd to entry", "onclick", call, "class", "ramadda-link"], HtmlUtils.image(ramaddaBaseUrl + "/icons/go.png"));
        },
        displayEntries: function(entries, div) {
            if (div == null) div = new Div();
            this.currentEntries = entries;
            if (entries == null || entries.length == 0) {
                return div.msg("No children");
            }
            var handlerId = addHandler(this);
            var html = this.notebook.getEntriesTree(entries, {
                handlerId: handlerId,
                showIndex: false,
                suffix: "_shell_" + (this.uniqueCnt++)
            });
            div.set(HtmlUtils.div(["style", "max-height:200px;overflow-y:auto;"], html));
            this.outputUpdated();
        },
        getEntryFromArgs: function(args, dflt) {
            var currentEntries = this.currentEntries;
            if (currentEntries == null) {
                return dflt;
            }
            for (var i = 0; i < args.length; i++) {
                var arg = args[i];
                if (arg.match("^\d+$")) {
                    var index = parseInt(arg);
                    break;
                }
                if (arg == "-entry") {
                    i++;
                    var index = parseInt(args[i]) - 1;
                    if (index < 0 || index >= currentEntries) {
                        this.writeError("Bad entry index:" + index + " should be between 1 and " + currentEntries.length);
                        return;
                    }
                    return currentEntries[index];
                }
            }
            return dflt;
        },
        setCurrentEntry: async function(entry) {
            this.currentEntry = entry;
            this.parentEntry = null;
            if (this.currentEntry)
                await this.currentEntry.getParentEntry(entry => {
                    this.parentEntry = entry;
                });
        },
        getCurrentEntry: async function(callback) {
            if (this.currentEntry == null) {
                await this.setCurrentEntry(this.notebook.getBaseEntry());
            }
            if (this.currentEntry == null) {
                if (Utils.isDefined(dflt)) return dflt;
                this.rootEntry = new Entry({
                    id: ramaddaBaseEntry,
                    name: "Root",
                    type: "group"
                });
                this.currentEntry = this.rootEntry;
            }
            return Utils.call(callback, this.currentEntry);
        },
        createDisplay: async function(state, entry, displayType, displayProps) {
            if (!entry) await this.getCurrentEntry(e => entry = e);
            if ((typeof entry) == "string") {
                await this.notebook.getEntry(entry, e => entry = e);
            }

            if (!state.displayManager) {
                var divId = HtmlUtils.getUniqueId();
                state.div.append(HtmlUtils.div(["id", divId], ""));
                state.displayManager = new DisplayManager(divId, {
                    "showMap": false,
                    "showMenu": false,
                    "showTitle": false,
                    "layoutType": "table",
                    "layoutColumns": 1,
                    "defaultMapLayer": "osm",
                    "entryId": ""
                });
            }

            var divId = HtmlUtils.getUniqueId();
            state.div.append(HtmlUtils.div(["id", divId], "DIV"));
            var props = {
                layoutHere: true,
                divid: divId,
                showMenu: true,
                sourceEntry: entry,
                entryId: entry.getId(),
                showTitle: true,
                showDetails: true,
                title: entry.getName(),
            };

            if (displayProps) {
                $.extend(props, displayProps);
            }
            if (!props.data && displayType != DISPLAY_ENTRYLIST) {
                var jsonUrl = this.notebook.getPointUrl(entry);
                if (jsonUrl == null) {
                    this.writeError("Not a point type:" + entry.getName());
                    return;
                }
                if (jsonUrl == null) {
                    jsonUrl = this.getPointUrl(entry);
                }
                var pointDataProps = {
                    entry: entry,
                    entryId: entry.getId()
                };
                props.data = new PointData(entry.getName(), null, null, jsonUrl, pointDataProps);
            }
            state.displayManager.createDisplay(displayType, props);
        },
        createPointDisplay: async function(toks, displayType) {
            await this.getCurrentEntry(e => current = e);
            var entry = this.getEntryFromArgs(toks, currentEntry);
            var jsonUrl = this.notebook.getPointUrl(entry);
            if (jsonUrl == null) {
                this.writeError("Not a point type:" + entry.getName());
                return;
            }
            this.notebook.createDisplay(entry.getId(), displayType, jsonUrl);
        },
        processCommand_table: function(line, toks) {
            this.createPointDisplay(toks, DISPLAY_TABLE);
        },
        processCommand_linechart: function(line, toks) {
            this.createPointDisplay(toks, DISPLAY_LINECHART);
        },

        processCommand_barchart: function(line, toks) {
            this.createPointDisplay(toks, DISPLAY_BARCHART);
        },
        processCommand_bartable: function(line, toks) {
            this.createPointDisplay(toks, DISPLAY_BARTABLE);
        },
        processCommand_hello: function(line, toks) {
            this.writeResult("Hello, how are you?");
        },
        processCommand_scatterplot: function(line, toks) {
            this.createPointDisplay(toks, DISPLAY_SCATTERPLOT)
        },
        processCommand_blog: function(line, toks) {
            this.getLayoutManager().publish('blogentry');
        },
        getEntryHeading: function(entry, div) {
            var entries = [entry];
            var handlerId = addHandler(this);
            var html = this.notebook.getEntriesTree(entries, {
                handlerId: handlerId,
                showIndex: false,
                suffix: "_shell_" + (this.uniqueCnt++)
            });
            div.set(HtmlUtils.div(["style", "max-height:200px;overflow-y:auto;"], html));
            return div;
            //            var icon = entry.getIconImage([ATTR_TITLE, "View entry"]);
            //            return "&gt; "+ icon +" " +entry.getName();
        },
        processCommand_pwd: async function(line, toks, div) {
            if (div == null) div = new Div();
            await this.getCurrentEntry(e => entry = e);
            return this.getEntryHeading(entry, div);
        },
        processCommand_set: async function(line, toks, div) {
            if (div == null) div = new Div();
            if (toks.length < 2) {
                div.append("Error: usage: set &lt;name&gt; &lt;value&gt;");
                return;
            }
            var name = toks[1];
            if (toks.length == 2) {
                var v = this.notebook.getGlobalValue(name);
                if (v) {
                    div.append(v);
                } else {
                    div.append("Unknown: " + name);
                }
            } else {
                var v = Utils.join(toks, " ", 2);
                v = v.replace(/\"/g, "");
                await this.notebook.addGlobal(name, v);
            }
        },
        processCommand_clearEntries: function(line, toks, div) {
            this.notebook.clearEntries();
            div.set("Entries cleared");
        },
        processCommand_printEntries: async function(line, toks, div) {
            var h = "";
            await this.getCurrentEntry(e => current = e);
            h += "current" + "=" + current.getName() + "<br>";
            var entries = this.notebook.getCurrentEntries();
            for (var name in entries) {
                var e = entries[name];
                h += name + "=" + e.entry.getName() + "<br>";
            }
            if (h == "") h = "No entries";
            div.set(h);
        },
        processCommand_echo: async function(line, toks, div) {
            line = line.replace(/^echo */, "");
            div.set(line);
        },
        processCommand_print: async function(line, toks, div) {
            line = line.replace(/^print */, "");
            div.set(line);
        },

        processCommand_info: async function(line, toks, div) {
            await this.getCurrentEntry(e => entry = e);
            div.append("current:" + entry.getName() + " id:" + entry.getId() + "<br>");
        },

        processCommand_cd: async function(line, toks, div) {
            if (div == null) div = new Div();
            if (toks.length <= 1) {
                await this.setCurrentEntry(this.notebook.getBaseEntry());
                return;
                //                return this.getEntryHeading(this.currentEntry, div);
            }
            var arg = Utils.join(toks, " ", 1).trim();
            var entry;
            await this.getEntryFromPath(arg, e => entry = e);
            if (!entry) {
                div.msg("Could not get entry:" + arg);
                return;
            }
            await this.setCurrentEntry(entry);
        },
        getEntryFromPath: async function(arg, callback) {
            var entry;
            await this.getCurrentEntry(e => entry = e);
            if (arg.startsWith("/")) {
                await entry.getRoot(e => {
                    entry = e
                });
            }
            var dirs = arg.split("/");
            for (var i = 0; i < dirs.length; i++) {
                var dir = dirs[i];
                if (dir == "") continue;
                if (dir == "..") {
                    await entry.getParentEntry(e => {
                        entry = e
                    });
                    if (!entry) {
                        break;
                    }
                } else {
                    await entry.getChildrenEntries(c => children = c);
                    var child = null;
                    var startsWith = false;
                    var endsWith = false;
                    if (dir.endsWith("*")) {
                        dir = dir.substring(0, dir.length - 1);
                        startsWith = true;
                    }
                    if (dir.startsWith("*")) {
                        dir = dir.substring(1);
                        endsWith = true;
                    }
                    for (var childIdx = 0; childIdx < children.length; childIdx++) {
                        var name = children[childIdx].getName();
                        if (startsWith && endsWith) {
                            if (name.includes(dir)) {
                                child = children[childIdx];
                                break;
                            }
                        } else if (startsWith) {
                            if (name.startsWith(dir)) {
                                child = children[childIdx];
                                break;
                            }
                        } else if (endsWith) {
                            if (name.endsWith(dir)) {
                                child = children[childIdx];
                                break;
                            }
                        }
                        if (children[childIdx].getName() == dir) {
                            child = children[childIdx];
                            break;
                        }
                    }
                    if (!child) {
                        break;
                    }
                    entry = child;
                }
            }
            return Utils.call(callback, entry);
        },


        processCommand_ls: async function(line, toks, div) {
            if (div == null) div = new Div();
            div.set("Listing entries...");
            await this.getCurrentEntry(e => entry = e);
            await entry.getChildrenEntries(children => {
                this.displayEntries(children, div)
            }, "");
        },
        entryListChanged: function(entryList) {
            var entries = entryList.getEntries();
            if (entries.length == 0) {
                this.writeStatusMessage("Sorry, nothing found");
            } else {
                this.displayEntries(entries);
            }
        },
        processCommand_search: async function(line, toks, div) {
            var text = "";
            for (var i = 1; i < toks.length; i++) text += toks[i] + " ";
            text = text.trim();
            var settings = new EntrySearchSettings({
                text: text,
            });
            var jsonUrl = this.notebook.getRamadda().getSearchUrl(settings, OUTPUT_JSON);
            let _this = this;
            var myCallback = {
                entryListChanged: function(list) {
                    var entries = list.getEntries();
                    div.set("");
                    if (entries.length == 0) {
                        div.append("Nothing found");
                    } else {
                        _this.displayEntries(entries, div)
                    }
                }
            };
            var entryList = new EntryList(this.notebook.getRamadda(), jsonUrl, myCallback, false);
            div.set("Searching...");
            await entryList.doSearch();
        },
        processCommand_clear: function(line, toks, div) {
            this.clearOutput();
        },
        processCommand_save: function(line, toks, div) {
            this.notebook.saveNotebook();
        },

    });

}


function processLispOutput(r) {
    if (r && r.val) return r.val;
    return Utils.formatJson(r);
}




function NotebookChunk(cell, props) {
    this.div =  new Div(null, "display-notebook-chunk");
    this.cell = cell;
    $.extend(this, {
            getContent: function() {
                var content = this.content;
                for (name in this.cell.notebook.globals) {
                    var value = this.cell.notebook.getGlobalValue(name);
                    if (typeof value == "object") {
                        value = Utils.formatJson(value);
                    }
                    content = content.replace("${" + name.trim() + "}", value);
                }
                return content;
            },
           initChunk: function(props) {
                if (props["skipoutput"] === true) {
                    this.div.set("");
                    this.div = new Div();
                }
                var depends = [];
                if (props["depends"] && typeof props["depends"] == "string") depends = props["depends"].split(",");
                var content = props.content||"";
                var regexp = RegExp(/\${([^ }]+)}/g);
                while((result = regexp.exec(content)) !== null) {
                    var param = result[1];
                    if(!depends.includes(param)) depends.push(param);
                }
                $.extend(this, {
                        name: props["name"],
                            depends: depends,
                            output: null,
                            runFirst: props["runFirst"],
                            hasRun: false,
                            content: content,
                            type: props.type,
                            props: props,
                            doChunk: !!props.doChunk,
                            ok: true
                            });
            }
        });
    this.initChunk(props);
}/**
Copyright 2008-2019 Geode Systems LLC
*/

var CATEGORY_CHARTS = "Charts";
var CATEGORY_OTHER = "Other Charts";
var CATEGORY_MISC = "Misc";
var DISPLAY_LINECHART = "linechart";
var DISPLAY_AREACHART = "areachart";
var DISPLAY_BARCHART = "barchart";
var DISPLAY_BARTABLE = "bartable";
var DISPLAY_BARSTACK = "barstack";
var DISPLAY_PIECHART = "piechart";
var DISPLAY_TIMELINECHART = "timelinechart";
var DISPLAY_SANKEY = "sankey";
var DISPLAY_CALENDAR = "calendar";
var DISPLAY_SCATTERPLOT = "scatterplot";
var DISPLAY_HISTOGRAM = "histogram";
var DISPLAY_BUBBLE = "bubble";
var DISPLAY_GAUGE = "gauge";
var DISPLAY_STATS = "stats";
var DISPLAY_RECORDS = "records";
var DISPLAY_TABLE = "table";
var DISPLAY_CROSSTAB = "crosstab";
var DISPLAY_CORRELATION = "correlation";
var DISPLAY_RANKING = "ranking";
var DISPLAY_TSNE = "tsne";
var DISPLAY_HEATMAP = "heatmap";
var DISPLAY_WORDTREE = "wordtree";
var DISPLAY_TREEMAP = "treemap";
var ID_CHART = "chart";


var googleChartsLoaded = false;

function googleChartsHaveLoaded() {
    googleChartsLoaded = true;
}
google.charts.setOnLoadCallback(googleChartsHaveLoaded);

function haveGoogleChartsLoaded() {
    if (!googleChartsLoaded) {
        if (Utils.isDefined(google.visualization)) {
            if (Utils.isDefined(google.visualization.Gauge)) {
                googleChartsLoaded = true;
            }
        }
    }
    return googleChartsLoaded;
}


addGlobalDisplayType({
    type: DISPLAY_LINECHART,
    label: "Line Chart",
    requiresData: true,
    forUser: true,
    category: CATEGORY_CHARTS
});
addGlobalDisplayType({
    type: DISPLAY_BARCHART,
    label: "Bar Chart",
    requiresData: true,
    forUser: true,
    category: CATEGORY_CHARTS
});
addGlobalDisplayType({
    type: DISPLAY_BARSTACK,
    label: "Stacked Bar Chart",
    requiresData: true,
    forUser: true,
    category: CATEGORY_CHARTS
});
addGlobalDisplayType({
    type: DISPLAY_AREACHART,
    label: "Area Chart",
    requiresData: true,
    forUser: true,
    category: CATEGORY_CHARTS
});

addGlobalDisplayType({
    type: DISPLAY_BARTABLE,
    label: "Bar Table",
    requiresData: true,
    forUser: true,
    category: CATEGORY_CHARTS
});
addGlobalDisplayType({
    type: DISPLAY_SCATTERPLOT,
    label: "Scatter Plot",
    requiresData: true,
    forUser: true,
    category: CATEGORY_CHARTS
});
addGlobalDisplayType({
    type: DISPLAY_HISTOGRAM,
    label: "Histogram",
    requiresData: true,
    forUser: true,
    category: CATEGORY_CHARTS
});
addGlobalDisplayType({
    type: DISPLAY_BUBBLE,
    label: "Bubble Chart",
    requiresData: true,
    forUser: true,
    category: CATEGORY_CHARTS
});
addGlobalDisplayType({
    type: DISPLAY_PIECHART,
    label: "Pie Chart",
    requiresData: true,
    forUser: true,
    category: CATEGORY_CHARTS
});

addGlobalDisplayType({
    type: DISPLAY_GAUGE,
    label: "Gauge",
    requiresData: true,
    forUser: true,
    category: CATEGORY_MISC
});
addGlobalDisplayType({
    type: DISPLAY_TIMELINECHART,
    label: "Timeline",
    requiresData: true,
    forUser: true,
    category: CATEGORY_MISC
});
addGlobalDisplayType({
    type: DISPLAY_SANKEY,
    label: "Sankey Chart",
    requiresData: true,
    forUser: true,
    category: CATEGORY_MISC
});

addGlobalDisplayType({
    type: DISPLAY_CALENDAR,
    label: "Calendar Chart",
    requiresData: true,
    forUser: true,
    category: CATEGORY_MISC
});
addGlobalDisplayType({
    type: DISPLAY_STATS,
    label: "Stats Table",
    requiresData: false,
    forUser: true,
    category: CATEGORY_MISC
});
addGlobalDisplayType({
    type: DISPLAY_RECORDS,
    label: "Records",
    requiresData: true,
    forUser: true,
    category: CATEGORY_MISC
});
addGlobalDisplayType({
    type: DISPLAY_TABLE,
    label: "Table",
    requiresData: true,
    forUser: true,
    category: CATEGORY_MISC
});
addGlobalDisplayType({
    type: DISPLAY_RANKING,
    label: "Ranking",
    requiresData: true,
    forUser: true,
    category: CATEGORY_MISC
});
addGlobalDisplayType({
    type: DISPLAY_CORRELATION,
    label: "Correlation",
    requiresData: true,
    forUser: true,
    category: CATEGORY_MISC
});
addGlobalDisplayType({
    type: DISPLAY_TSNE,
    label: "TSNE",
    requiresData: true,
    forUser: true,
    category: CATEGORY_MISC
});
addGlobalDisplayType({
    type: DISPLAY_HEATMAP,
    label: "Heatmap",
    requiresData: true,
    forUser: true,
    category: CATEGORY_MISC
});
addGlobalDisplayType({
    type: DISPLAY_WORDTREE,
    label: "Word Tree",
    requiresData: true,
    forUser: true,
    category: "Text"
});
addGlobalDisplayType({
    type: DISPLAY_TREEMAP,
    label: "Tree Map",
    requiresData: true,
    forUser: true,
    category: CATEGORY_MISC
});




var PROP_CHART_MIN = "chartMin";
var PROP_CHART_MAX = "chartMax";
var DFLT_WIDTH = "600px";
var DFLT_HEIGHT = "200px";



/*
 */
function RamaddaFieldsDisplay(displayManager, id, type, properties) {
    let _this = this;
    this.TYPE = "RamaddaFieldsDisplay";
    let SUPER = new RamaddaDisplay(displayManager, id, type, properties);
    RamaddaUtil.inherit(this, this.RamaddaDisplay = SUPER);
    RamaddaUtil.defineMembers(this, {
        needsData: function() {
            return true;
        },
        handleEventMapClick: function(source, args) {
            if (!this.dataCollection) return;
            var pointData = this.dataCollection.getList();
            for (var i = 0; i < pointData.length; i++) {
                pointData[i].handleEventMapClick(this, source, args.lon, args.lat);
            }
        },
        initDisplay: function() {
            SUPER.initDisplay.call(this);
            if (this.needsData()) {
                this.setContents(this.getLoadingMessage());
            }
            this.updateUI();
        },
        updateUI: function() {
            this.addFieldsCheckboxes();
        },
        //This keeps checking the width of the chart element if its zero
        //we do this for displaying in tabs
        checkLayout: function() {
            var _this = this;
            var d = _this.jq(ID_DISPLAY_CONTENTS);
            //       console.log("checklayout:  widths:" + this.lastWidth +" " + d.width());
            if (this.lastWidth != d.width()) {
                _this.displayData();
            }
            if (true) return;

            if (d.width() == 0) {
                var cb = function() {
                    _this.checkWidth(cnt + 1);
                };
                setTimeout(cb, 5000);
            } else {
                //                    console.log("checkWidth:"+ _this.getTitle() +" calling displayData");
                _this.displayData();
            }
        },

        getWikiAttributes: function(attrs) {
            SUPER.getWikiAttributes.call(this, attrs);
            if (this.lastSelectedFields) {
                attrs.push("fields");
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
            var html = HtmlUtils.div([ATTR_ID, this.getDomId(ID_FIELDS), "style", "overflow-y: auto;    max-height:" + height + "px;"], " FIELDS ");
            tabTitles.push("Fields");
            tabContents.push(html);
            SUPER.getDialogContents.call(this, tabTitles, tabContents);
        },
        handleEventFieldsSelected: function(source, fields) {
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







/*
Create a chart
id - the id of this chart. Has to correspond to a div tag id 
pointData - A PointData object (see below)
 */
function RamaddaGoogleChart(displayManager, id, chartType, properties) {
    var ID_TRENDS_CBX = "trends_cbx";
    var ID_PERCENT_CBX = "percent_cbx";
    var ID_COLORS = "colors";

    var _this = this;


    //Init the defaults first
    $.extend(this, {
        indexField: -1,
        colorList: ['blue', 'red', 'green', 'orange', 'fuchsia', 'teal', 'navy', 'silver'],
        curveType: 'none',
        fontSize: 0,
        showPercent: false,
        percentFields: null,
    });
    if (properties.colors) {
        this.colorList = ("" + properties.colors).split(",");
    }
    let SUPER = new RamaddaFieldsDisplay(displayManager, id, chartType, properties);
    RamaddaUtil.inherit(this, SUPER);

    RamaddaUtil.defineMembers(this, {
        clearCachedData: function() {
            SUPER.clearCachedData();
            this.computedData = null;
        },
        updateUI: function() {
            SUPER.updateUI.call(this);
            if (!this.getDisplayReady()) {
                return;
            }
            this.displayData();
        },
        getWikiAttributes: function(attrs) {
            this.defineWikiAttributes(["vAxisMinValue", "vAxisMaxValue"]);
            SUPER.getWikiAttributes.call(this, attrs);
            if (this.colorList.join(",") != "blue,red,green") {
                attrs.push("colors");
                attrs.push(this.colorList.join(", "));
            }
        },

        initDialog: function() {
            SUPER.initDialog.call(this);
            var _this = this;
            var updateFunc = function() {
                _this.vAxisMinValue = Utils.toFloat(_this.jq("vaxismin").val());
                _this.vAxisMaxValue = Utils.toFloat(_this.jq("vaxismax").val());
                _this.minDate = _this.jq("mindate").val();
                _this.maxDate = _this.jq("maxdate").val();
                _this.displayData();

            };
            this.jq("vaxismin").blur(updateFunc);
            this.jq("vaxismax").blur(updateFunc);
            this.jq("mindate").blur(updateFunc);
            this.jq("maxdate").blur(updateFunc);



            this.jq(ID_COLORS).keypress(function(e) {
                if (e.which != 13) {
                    return;
                }
                var v = _this.jq(ID_COLORS).val();
                _this.colorList = v.split(",");
                _this.displayData();
                var pointData = _this.dataCollection.getList();
                _this.getDisplayManager().handleEventPointDataLoaded(_this, _this.lastPointData);
            });

            this.jq(ID_TRENDS_CBX).click(function() {
                _this.showTrendLines = _this.jq(ID_TRENDS_CBX).is(':checked');
                _this.displayData();

            });
            this.jq(ID_PERCENT_CBX).click(function() {
                _this.showPercent = _this.jq(ID_PERCENT_CBX).is(':checked');
                _this.displayData();

            });

        },
        setColor: function() {
            var v = prompt("Enter comma separated list of colors to use", this.colorList.join(","));
            if (v != null) {
                this.colorList = v.split(",");
                this.displayData();
                var pointData = this.dataCollection.getList();
                this.getDisplayManager().handleEventPointDataLoaded(this, this.lastPointData);
            }
        },
        getVAxisMinValue: function() {
            return parseFloat(this.getProperty("vAxisMinValue", NaN));
        },
        getVAxisMaxValue: function() {
            return parseFloat(this.getProperty("vAxisMaxValue", NaN));
        },
        getMenuItems: function(menuItems) {
            SUPER.getMenuItems.call(this, menuItems);
            var get = this.getGet();
            //                menuItems.push(HtmlUtils.onClick(get+".setColor();", "Set color"));

            var min = "0";
            if (!isNaN(this.getVAxisMinValue())) {
                min = "" + this.getVAxisMinValue();
            }
            var max = "";
            if (!isNaN(this.getVAxisMaxValue())) {
                max = "" + this.getVAxisMaxValue();
            }

            var tmp = HtmlUtils.formTable();
            tmp += HtmlUtils.formEntry("Axis Range:", HtmlUtils.input("", min, ["size", "7", ATTR_ID, this.getDomId("vaxismin")]) + " - " +
                HtmlUtils.input("", max, ["size", "7", ATTR_ID, this.getDomId("vaxismax")]));
            tmp += HtmlUtils.formEntry("Date Range:", HtmlUtils.input("", this.minDate, ["size", "10", ATTR_ID, this.getDomId("mindate")]) + " - " +
                HtmlUtils.input("", this.maxDate, ["size", "10", ATTR_ID, this.getDomId("maxdate")]));


            tmp += HtmlUtils.formEntry("Colors:",
                HtmlUtils.input("", this.colorList.join(","), ["size", "35", ATTR_ID, this.getDomId(ID_COLORS)]));
            tmp += "</table>";
            menuItems.push(tmp);

        },
        getChartType: function() {
            return this.getType();
        },
        askMinZAxis: function() {
            this.setMinZAxis(prompt("Minimum axis value", "0"));
        },
        setMinZAxis: function(v) {
            if (v != null) {
                this.vAxisMinValue = parseFloat(v);
                this.displayData();
            }
        },
        askMaxZAxis: function() {
            this.setMaxZAxis(prompt("Maximum axis value", "100"));
        },
        setMaxZAxis: function(v) {
            if (v != null) {
                this.vAxisMaxValue = parseFloat(v);
                this.displayData();
            }
        },
        askMinDate: function() {
            var ex = this.minDate;
            if (ex == null || ex == "") {
                ex = "1800-01-01";
            }
            var v = prompt("Minimum date", ex);
            if (v == null) return;
            this.setMinDate(v);
        },
        setMinDate: function(v) {
            this.minDate = v;
            this.displayData();
        },
        askMaxDate: function() {
            var ex = this.maxDate;
            if (ex == null || ex == "") {
                ex = "2100-01-01";
            }
            var v = prompt("Maximum date", ex);
            if (v == null) return;
            this.setMaxDate(v);
        },
        setMaxDate: function(v) {
            this.maxDate = v;
            this.displayData();
        },
        trendLineEnabled: function() {
            return false;
        },
        getDialogContents: function(tabTitles, tabContents) {
            var height = "600";
            var html = HtmlUtils.div([ATTR_ID, this.getDomId(ID_FIELDS), "style", "overflow-y: auto;    max-height:" + height + "px;"], " FIELDS ");

            if (this.trendLineEnabled()) {
                html += HtmlUtils.div([ATTR_CLASS, "display-dialog-subheader"], "Other");

                html += HtmlUtils.checkbox(this.getDomId(ID_TRENDS_CBX),
                    [],
                    this.getProperty("showTrendLines", false)) + "  " + "Show trend line";
                html += " ";
                html += HtmlUtils.checkbox(this.getDomId(ID_PERCENT_CBX),
                    [],
                    this.showPercent) + "  " + "Show percent of displayed total" + "<br>";
                html += "<br>";
            }

            tabTitles.push("Fields");
            tabContents.push(html);
            SUPER.RamaddaDisplay.getDialogContents.call(this, tabTitles, tabContents);
        },
        okToHandleEventRecordSelection: function() {
            return true;
        },
        handleEventRecordSelection: function(source, args) {
            //TODO: don't do this in index space, do it in time or space space
            if (source == this) {
                return;
            }
            if (!this.okToHandleEventRecordSelection())
                return;
            var data = this.dataCollection.getList()[0];
            if (data != args.data) {
                return;
            }
            this.setChartSelection(args.index);
        },
        getFieldsToSelect: function(pointData) {
            return pointData.getChartableFields();
        },
        canDoGroupBy: function() {
            return false;
        },
        clearCache: function() {
            this.computedData = null;
        },
        googleChartCallbackPending: false,
        includeIndexInData: function() {
            return false;
        },
        getGroupBy: function() {
            return this.groupBy;
        },
        getIncludeIndexIfDate: function() {
            return false;
        },
        displayData: function() {
            var _this = this;

            if (!this.getDisplayReady()) {
                return;
            }
            if (this.inError) {
                return;
            }
            if (!haveGoogleChartsLoaded()) {
                if (!this.googleChartCallbackPending) {
                    this.googleChartCallbackPending = true;
                    var func = function() {
                        _this.googleChartCallbackPending = false;
                        _this.displayData();
                    }
                    this.setContents(this.getLoadingMessage());
                    setTimeout(func, 500);
                }
                return;
            }
            if (this.inError) {
                return;
            }
            if (!this.hasData()) {
                this.clearChart();
                this.setContents(this.getLoadingMessage());
                return;
            }

            this.setContents(HtmlUtils.div([ATTR_CLASS, "display-message"],
                "Building display..."));


            this.allFields = this.dataCollection.getList()[0].getRecordFields();
            var pointData = this.dataCollection.getList()[0];
            //            var selectedFields = this.getSelectedFields(this.getFieldsToSelect(pointData));
            var selectedFields = this.getSelectedFields();
            if (selectedFields.length == 0 && this.lastSelectedFields != null) {
                selectedFields = this.lastSelectedFields;
            }


            if (selectedFields == null || selectedFields.length == 0) {
                if (this.getChartType() == DISPLAY_TABLE || this.getChartType() == DISPLAY_TREEMAP) {
                    selectedFields = this.dataCollection.getList()[0].getNonGeoFields();
                } else {
                    selectedFields = this.getSelectedFields();
                }
            }

            if (selectedFields.length == 0) {
                this.setContents("No fields selected");
                return;
            }

            //Check for the skip
            var tmpFields = [];
            for (var i = 0; i < selectedFields.length; i++) {
                if (!this.shouldSkipField(selectedFields[i])) {
                    tmpFields.push(selectedFields[i]);
                }
            }
            selectedFields = tmpFields;
            this.lastSelectedFields = selectedFields;

            var props = {
                includeIndex: this.includeIndexInData()
            };
            props.groupByIndex = -1;


            var groupBy = this.getGroupBy();
            if (groupBy) {
                for (var i = 0; i < this.allFields.length; i++) {
                    var field = this.allFields[i];
                    if (field.getId() == groupBy) {
                        props.groupByIndex = field.getIndex();
                        props.groupByField = field;
                        this.groupByField = field;
                        break;
                    }
                }
            }

            var fieldsToSelect = selectedFields;
            if (this.raw) {
                fieldsToSelect = this.dataCollection.getList()[0].getRecordFields();
                props.raw = true;
            }

            props.includeIndexIfDate = this.getIncludeIndexIfDate();

            var dataHasIndex = props.includeIndex;
            var dataList = this.computedData;
            if (this["function"] && this.computedData == null) {
                var pointData = this.dataCollection.getList()[0];
                var allFields = pointData.getRecordFields();
                var records = pointData.getRecords();
                var indexField = this.indexField;
                var chartableFields = this.getFieldsToSelect(pointData);
                this.hasDate = this.getHasDate(records);
                var date_formatter = this.getDateFormatter();
                var setVars = "";
                for (var i = 0; i < chartableFields.length; i++) {
                    var field = chartableFields[i];
                    setVars += "\tvar " + field.getId() + "=args." + field.getId() + ";\n";
                }
                var code = "function displayChartEval(args) {\n" + setVars + "\treturn  " + this["function"] + "\n}";
                eval(code);
                var newList = [];
                var fieldNames = null;
                var rowCnt = -1;
                var indexField = this.indexField;
                for (var rowIdx = 0; rowIdx < records.length; rowIdx++) {
                    var record = records[rowIdx];
                    var row = record.getData();
                    var date = record.getDate();
                    if (!this.dateInRange(date)) continue;
                    rowCnt++;
                    var values = [];
                    var indexName = null;
                    if (indexField >= 0) {
                        var field = allFields[indexField];
                        values.push(record.getValue(indexField) + offset);
                        indexName = field.getLabel();
                    } else {
                        if (this.hasDate) {
                            values.push(this.getDateValue(date, date_formatter));
                            indexName = "Date";
                        } else {
                            values.push(rowIdx);
                            indexName = "Index";
                        }
                    }
                    if (fieldNames == null) {
                        fieldNames = [indexName, this.functionName ? this.functionName : "value"];
                        newList.push(fieldNames);
                    }
                    var args = {};
                    for (var j = 0; j < chartableFields.length; j++) {
                        var field = chartableFields[j];
                        var value = row[field.getIndex()];
                        args[field.getId()] = value;
                    }
                    var value = displayChartEval(args);
                    values.push(value);
                    newList.push(values);
                }
                dataList = newList;
            }

            if (dataList == null) {
                dataList = this.getStandardData(fieldsToSelect, props);
            }
            this.computedData = dataList;

            if (this.rotateTable && dataList.length) {
                var header = dataList[0];
                var flipped = [];
                for (var rowIdx = 1; rowIdx < dataList.length; rowIdx++) {
                    var row = dataList[rowIdx];
                }
            }

            if (dataList.length == 0 && !this.userHasSelectedAField) {
                var pointData = this.dataCollection.getList()[0];
                var chartableFields = this.getFieldsToSelect(pointData);
                for (var i = 0; i < chartableFields.length; i++) {
                    var field = chartableFields[i];
                    dataList = this.getStandardData([field], props);
                    if (dataList.length > 0) {
                        this.setSelectedFields([field]);
                        break;
                    }
                }
            }

            if (dataList.length == 0) {
                this.setContents(HtmlUtils.div([ATTR_CLASS, "display-message"],
                    "No data available"));
                return;
            }

            if (this.showPercent) {
                var newList = [];
                var isNumber = [];
                var isOk = [];
                var headerRow = null;
                var fields = null;
                if (this.percentFields != null) {
                    fields = this.percentFields.split(",");
                }
                for (var i = 0; i < dataList.length; i++) {
                    var row = this.getDataValues(dataList[i]);
                    if (i == 0) {
                        headerRow = row;
                        continue;
                    }
                    if (i == 1) {
                        var seenIndex = false;
                        for (var j = 0; j < row.length; j++) {
                            var valueIsNumber = (typeof row[j] == "number");
                            var valueIsDate = (typeof row[j] == "object");
                            if (valueIsNumber) {
                                if (dataHasIndex && !seenIndex) {
                                    valueIsNumber = false;
                                    seenIndex = true;
                                }
                            }
                            if (valueIsDate) {
                                seenIndex = true;
                            }

                            if (valueIsNumber && fields != null) {
                                valueIsNumber = fields.indexOf(fieldsToSelect[j].getId()) >= 0 ||
                                    fields.indexOf("#" + (j + 1)) >= 0;
                            }
                            isNumber.push(valueIsNumber);
                        }
                        var newHeader = [];
                        for (var j = 0; j < headerRow.length; j++) {
                            var v = headerRow[j];
                            if (!isNumber[j]) {
                                newHeader.push(v);
                            } else {
                                newHeader.push("% " + v);
                            }
                        }
                        newList.push(newHeader);
                    }

                    var total = 0;
                    var cnt = 0;
                    for (var j = 0; j < row.length; j++) {
                        if (isNumber[j]) {
                            total += parseFloat(row[j]);
                            cnt++;
                        }
                    }
                    var newRow = [];
                    for (var j = 0; j < row.length; j++) {
                        if (isNumber[j]) {
                            if (total != 0) {
                                var v = parseFloat(((row[j] / total) * 100).toFixed(1));
                                newRow.push(v);
                            } else {
                                newRow.push(NaN);
                            }
                        } else {
                            newRow.push(row[j]);
                        }
                    }
                    newList.push(newRow);
                }
                dataList = newList;
            }

            try {
                this.makeChart(dataList, props, selectedFields);
            } catch (e) {
                console.log(e.stack);
                this.displayError("" + e);
                return;
            }

            var d = _this.jq(ID_CHART);
            this.lastWidth = d.width();
        },
        printDataList: function(dataList) {
            console.log("data list:" + dataList.length);
            for (var i = 0; i < dataList.length; i++) {
                var row = dataList[i];
                var s = "";
                for (var j = 0; j < row.length; j++) {
                    if (j > 0) s += ", ";
                    s += row[j];
                }
                console.log("row: " + i + "  " + s);
            }
        },
        clearChart: function() {
            if (this.chart != null && this.chart.clearChart) {
                this.chart.clearChart();
            }
        },
        setChartSelection: function(index) {
            if (this.chart != null) {
                if (this.chart.setSelection) {
                    this.chart.setSelection([{
                        row: index,
                        column: null
                    }]);
                }
            }
        },
        tableHeaderMouseover: function(i, tooltip) {},
        makeDataTable: function(dataList, props, selectedFields) {
            dataList = this.filterData(dataList, selectedFields);
            if (dataList.length == 1) {
                return google.visualization.arrayToDataTable(this.makeDataArray(dataList));
            }


            var justData = [];
            var begin = props.includeIndex ? 1 : 0;
            var tooltipFields = [];
            var toks = this.getProperty("tooltipFields", "").split(",");
            for (var i = 0; i < toks.length; i++) {
                var tooltipField = this.getFieldById(null, toks[i]);
                if (tooltipField)
                    tooltipFields.push(tooltipField);
            }

            var dataTable = new google.visualization.DataTable();
            var header = this.getDataValues(dataList[0]);
            var sample = this.getDataValues(dataList[1]);
            for (var j = 0; j < header.length; j++) {
                var value = sample[j];
                if (j == 0 && props.includeIndex) {
                    //This might be a number or a date
                    if ((typeof value) == "object") {
                        //assume its a date
                        dataTable.addColumn('date', header[j]);
                    } else {
                        dataTable.addColumn((typeof value), header[j]);
                    }
                } else {
                    //Assume all remaining fields are numbers
                    dataTable.addColumn('number', header[j]);
                    dataTable.addColumn({
                        type: 'string',
                        role: 'tooltip',
                        'p': {
                            'html': true
                        }
                    });
                }
            }




            for (var i = 1; i < dataList.length; i++) {
                var row = this.getDataValues(dataList[i]);
                row = row.slice(0);
                var label = "";
                if (dataList[i].record) {
                    for (var j = 0; j < tooltipFields.length; j++) {
                        label += "<b>" + tooltipFields[j].getLabel() + "</b>: " +
                            dataList[i].record.getValue(tooltipFields[j].getIndex()) + "<br>";
                    }
                }

                var tooltip = "<div style='padding:8px;'>";
                tooltip += label;
                for (var j = 0; j < row.length; j++) {
                    if (j > 0)
                        tooltip += "<br>";
                    label = header[j].replace(/ /g, "&nbsp;");
                    value = row[j];
                    if (!value) value = "NA";
                    if (value && (typeof value) == "object") {
                        if (value.f) {
                            value = value.f;
                        }
                    }
                    if (Utils.isNumber(value)) {
                        value = this.formatNumber(value);
                    }
                    value = "" + value;
                    value = value.replace(/ /g, "&nbsp;");
                    tooltip += "<b>" + label + "</b>:&nbsp;" + value;
                }
                tooltip += "</div>";

                newRow = [];
                for (var j = 0; j < row.length; j++) {
                    var value = row[j];
                    newRow.push(value);
                    if (j == 0 && props.includeIndex) {
                        //is the index so don't add a tooltip
                    } else {
                        newRow.push(tooltip);
                    }
                }
                //                    newRow.push("annotation");
                justData.push(newRow);
            }
            dataTable.addRows(justData);
            return dataTable;
        },

        makeChartOptions: function(dataList, props, selectedFields) {
            var chartOptions = {
                tooltip: {
                    isHtml: true
                },
            };

            $.extend(chartOptions, {
                lineWidth: 1,
                colors: this.colorList,
                curveType: this.curveType,
                vAxis: {}
            });

            chartOptions.backgroundColor = {};
            chartOptions.chartArea = {};
            chartOptions.chartArea.backgroundColor = {};

            chartOptions.legend = {
                textStyle: {}
            };
            chartOptions.hAxis = {
                gridlines: {},
                textStyle: {}
            };
            chartOptions.vAxis = {
                gridlines: {},
                textStyle: {}
            };
            chartOptions.hAxis.titleTextStyle = {};
            chartOptions.vAxis.titleTextStyle = {};
            this.setPropertyOn(chartOptions.backgroundColor, "chart.fill", "fill", null);
            this.setPropertyOn(chartOptions.backgroundColor, "chart.stroke", "stroke", this.getProperty("chartArea.fill", ""));
            this.setPropertyOn(chartOptions.backgroundColor, "chart.strokeWidth", "strokeWidth", null);

            this.setPropertyOn(chartOptions.chartArea.backgroundColor, "chartArea.fill", "fill", null);
            this.setPropertyOn(chartOptions.chartArea.backgroundColor, "chartArea.stroke", "stroke", null);
            this.setPropertyOn(chartOptions.chartArea.backgroundColor, "chartArea.strokeWidth", "strokeWidth", null);
            this.setPropertyOn(chartOptions.hAxis.gridlines, "hAxis.gridlines.color", "color", this.getProperty("gridlines.color", null));
            this.setPropertyOn(chartOptions.vAxis.gridlines, "vAxis.gridlines.color", "color", this.getProperty("gridlines.color", null));

            var textColor = this.getProperty("textColor", "#000");
            this.setPropertyOn(chartOptions.hAxis.textStyle, "hAxis.text.color", "color", this.getProperty("axis.text.color", textColor));
            this.setPropertyOn(chartOptions.vAxis.textStyle, "vAxis.text.color", "color", this.getProperty("axis.text.color", textColor));

            this.setPropertyOn(chartOptions.hAxis.titleTextStyle, "hAxis.text.color", "color", textColor);
            this.setPropertyOn(chartOptions.vAxis.titleTextStyle, "vAxis.text.color", "color", textColor);

            this.setPropertyOn(chartOptions.legend.textStyle, "legend.text.color", "color", textColor);


            if (this.lineWidth) {
                chartOptions.lineWidth = this.lineWidth;
            }
            if (this.fontSize > 0) {
                chartOptions.fontSize = this.fontSize;
            }

            var defaultRange = this.getDisplayManager().getRange(selectedFields[0]);
            var range = [NaN, NaN];
            if (!isNaN(this.getVAxisMinValue())) {
                range[0] = this.getVAxisMinValue();
            } else if (defaultRange != null) {
                range[0] = defaultRange[0];
            }
            if (!isNaN(this.getVAxisMaxValue())) {
                range[1] = this.getVAxisMaxValue();
            } else if (defaultRange != null) {
                range[1] = defaultRange[1];
            }
            //console.log("range:" + range[0]+" " + range[1]);

            if (!isNaN(range[0])) {
                chartOptions.vAxis.minValue = range[0];
            }
            if (!isNaN(range[1])) {
                chartOptions.vAxis.maxValue = range[1];
            }
            this.chartDimensions = {
                width: "90%",
                left: "10%",
                right: 10,
            }

            useMultipleAxes = this.getProperty("useMultipleAxes", true);

            if ((selectedFields.length > 1 && useMultipleAxes) || this.getProperty("padRight", false) === true) {
                this.chartDimensions.width = "80%";
            }

            if (this.getProperty("showTrendLines", false)) {
                chartOptions.trendlines = {
                    0: {
                        type: 'linear',
                        color: 'green',
                    }
                };
            }
            this.setContents(this.getChartDiv());
            return chartOptions;
        },
        getChartHeight: function() {
            return this.getProperty("height");
        },
        getChartWidth: function() {
            return this.getProperty("width");
        },
        getChartDiv: function() {

            var chartId = this.getDomId(ID_CHART);
            var divAttrs = [ATTR_ID, chartId];
            divAttrs.push("style");
            var style = "";
            var width = this.getChartWidth();
            if (width) {
                if (width > 0)
                    style += "width:" + width + "px;";
                else if (width < 0)
                    style += "width:" + (-width) + "%;";
                else
                    style += "width:" + width + ";";
            } else {
                style += "width:" + "100%;";
            }
            var height = this.getChartHeight();
            if (height) {
                if (height > 0)
                    style += "height:" + height + "px;";
                else if (height < 0)
                    style += "height:" + (-height) + "%;";
                else
                    style += "height:" + height + ";";
            } else {
                style += "height:" + "100%;";
            }
            divAttrs.push(style);
            return HtmlUtils.div(divAttrs, "");
        },
        makeGoogleChart: function(dataList, props, selectedFields) {
            if (typeof google == 'undefined') {
                this.setContents("No google");
                return;
            }
            this.chartOptions = this.makeChartOptions(dataList, props, selectedFields);
            this.chart = this.doMakeGoogleChart(dataList, props, selectedFields, this.chartOptions);
            if (this.chart != null) {
                var dataTable = this.makeDataTable(dataList, props, selectedFields);
                if (!dataTable) {
                    this.setContents(this.getMessage("No data available"));
                    return;
                }
                if (!Utils.isDefined(this.chartOptions.height)) {
                    this.chartOptions.height = "100%";
                }
                //                console.log("draw:" +" " +JSON.stringify(this.chartOptions,null,3));
                this.chart.draw(dataTable, this.chartOptions);
                var theDisplay = this;
                google.visualization.events.addListener(this.chart, 'onmouseover', function(event) {
                    mapVar = theDisplay.getProperty("mapVar", null);
                    if (!Utils.stringDefined(mapVar)) {
                        return;
                    }
                    row = event.row;
                    pointData = theDisplay.dataCollection.getList()[0];
                    var fields = pointData.getRecordFields();
                    var records = pointData.getRecords();
                    var record = records[row];
                    map = ramaddaMapMap[mapVar];
                    if (map) {
                        if (theDisplay.mouseOverPoint)
                            map.removePoint(theDisplay.mouseOverPoint);
                    } else {}
                    if (record && map) {
                        latField = null;
                        lonField = null;
                        for (i = 0; i < fields.length; i++) {
                            if (fields[i].isFieldLatitude()) latField = fields[i];
                            else if (fields[i].isFieldLongitude()) lonField = fields[i];
                        }
                        if (latField && lonField) {
                            lat = record.getValue(latField.getIndex());
                            lon = record.getValue(lonField.getIndex());
                            theDisplay.mouseOverPoint = map.addPoint(chartId, new OpenLayers.LonLat(lon, lat));
                        }
                    }
                });
                //always propagate the event when loaded
                theDisplay.displayManager.propagateEventRecordSelection(theDisplay,
                    theDisplay.dataCollection.getList()[0], {
                        index: 0
                    });
                google.visualization.events.addListener(this.chart, 'select', function(event) {
                    if (theDisplay.chart.getSelection) {
                        var selected = theDisplay.chart.getSelection();
                        if (selected && selected.length > 0) {
                            var index = selected[0].row;
                            theDisplay.displayManager.propagateEventRecordSelection(theDisplay,
                                theDisplay.dataCollection.getList()[0], {
                                    index: index
                                });
                        }
                    }
                });
            }
        },
        getChartId: function() {
            return this.getDomId(ID_CHART);
        },
        doMakeGoogleChart: function(dataList, props, selectedFields, chartOptions) {
            throw new Error("doMakeGoogleChart undefined");
        }
    });
    this.makeChart = this.makeGoogleChart;
}



function RamaddaAxisChart(displayManager, id, chartType, properties) {
    let SUPER = new RamaddaGoogleChart(displayManager, id, chartType, properties);
    RamaddaUtil.inherit(this, SUPER);
    $.extend(this, {
        makeChartOptions: function(dataList, props, selectedFields) {
            chartOptions = SUPER.makeChartOptions.call(this, dataList, props, selectedFields);

            var useMultipleAxes = this.getProperty("useMultipleAxes", true);
            chartOptions.height = this.getProperty("chartHeight", this.getProperty("height", "150"));
            if (!chartOptions.legend)
                chartOptions.legend = {};

            $.extend(chartOptions.legend, {
                position: this.getProperty("legendPosition", 'bottom')
            });

            if (!chartOptions.chartArea) {
                chartOptions.chartArea = {};
            }
            /*
            chartOptions.chartArea={};
            chartOptions.chartArea.backgroundColor =  {
                'fill': '#ccc',
                'opacity': 1
            }
            */
            //            chartOptions.chartArea.backgroundColor =  "green";
            $.extend(chartOptions.chartArea, {
                left: this.getProperty("chartLeft", this.chartDimensions.left),
                right: this.getProperty("chartRight", this.chartDimensions.right),
                top: this.getProperty("chartTop", "10"),
                height: this.getProperty("chartHeight", "70%"),
                width: this.getProperty("chartWidth", this.chartDimensions.width),
            });

            if (useMultipleAxes) {
                $.extend(chartOptions, {
                    series: [{
                        targetAxisIndex: 0
                    }, {
                        targetAxisIndex: 1
                    }]
                });
            }


            if (this.hAxis) {
                if (chartOptions.hAxis) {
                    chartOptions.hAxis.title = this.hAxis;
                } else {
                    chartOptions.hAxis = {
                        title: this.hAxis
                    }
                }
            }
            if (this.vAxis) {
                if (chartOptions.vAxis) {
                    chartOptions.vAxis.title = this.vAxis;
                } else {
                    chartOptions.vAxis = {
                        title: this.vAxis
                    }
                }
            }
            if (Utils.isDefined(this.chartHeight)) {
                chartOptions.height = this.chartHeight;
            }

            return chartOptions;

        }
    });

}



function RamaddaSeriesChart(displayManager, id, chartType, properties) {
    let SUPER = new RamaddaAxisChart(displayManager, id, chartType, properties);
    RamaddaUtil.inherit(this, SUPER);
    $.extend(this, {
        includeIndexInData: function() {
            return true;
        },
        trendLineEnabled: function() {
            return true;
        },
    });
}


function LinechartDisplay(displayManager, id, properties) {
    RamaddaUtil.inherit(this, new RamaddaSeriesChart(displayManager, id, DISPLAY_LINECHART, properties));
    addRamaddaDisplay(this);
    $.extend(this, {
        doMakeGoogleChart: function(dataList, props, selectedFields, chartOptions) {
            return new google.visualization.LineChart(document.getElementById(this.getChartId()));
        },
    });
}



function AreachartDisplay(displayManager, id, properties) {
    RamaddaUtil.inherit(this, new RamaddaSeriesChart(displayManager, id, DISPLAY_AREACHART, properties));
    addRamaddaDisplay(this);
    $.extend(this, {
        doMakeGoogleChart: function(dataList, props, selectedFields, chartOptions) {
            if (this.isStacked)
                chartOptions.isStacked = true;
            return new google.visualization.AreaChart(document.getElementById(this.getChartId()));
        }
    });
}


function RamaddaBaseBarchart(displayManager, id, type, properties) {
    RamaddaUtil.inherit(this, new RamaddaSeriesChart(displayManager, id, type, properties));
    $.extend(this, {
        doMakeGoogleChart: function(dataList, props, selectedFields, chartOptions) {
            var chartType = this.getChartType();
            if (chartType == DISPLAY_BARSTACK) {
                chartOptions.isStacked = true;
            }
            if (this.getProperty("barWidth")) {
                chartOptions.bar = {
                    groupWidth: this.getProperty("barWidth")
                }
            }
            chartOptions.orientation = "horizontal";
            return new google.visualization.BarChart(document.getElementById(this.getChartId()));
        }
    });
}


function BarchartDisplay(displayManager, id, properties) {
    RamaddaUtil.inherit(this, new RamaddaBaseBarchart(displayManager, id, DISPLAY_BARCHART, properties));
    addRamaddaDisplay(this);
}

function BarstackDisplay(displayManager, id, properties) {
    properties = $.extend({
        "isStacked": true
    }, properties);
    RamaddaUtil.inherit(this, new RamaddaBaseBarchart(displayManager, id, DISPLAY_BARSTACK, properties));
    addRamaddaDisplay(this);
}


function HistogramDisplay(displayManager, id, properties) {
    RamaddaUtil.inherit(this, new RamaddaGoogleChart(displayManager, id, DISPLAY_HISTOGRAM, properties));
    addRamaddaDisplay(this);
    RamaddaUtil.inherit(this, {
        okToHandleEventRecordSelection: function() {
            return false;
        },
        makeDataTable: function(dataList, props, selectedFields) {
            return google.visualization.arrayToDataTable(this.makeDataArray(dataList));
        },
        doMakeGoogleChart: function(dataList, props, selectedFields, chartOptions) {
            //            chartOptions = {};
            if (this.legendPosition) {
                if (!chartOptions.legend)
                    chartOptions.legend = {};
                chartOptions.legend.position = this.legendPosition;
            }
            var isStacked = this.getProperty("isStacked", null);
            if (isStacked)
                chartOptions.isStacked = isStacked == "true" ? true : isStacked == "false" ? false : isStacked;
            chartOptions.vAxis = {};
            chartOptions.vAxis.viewWindow = {};
            if (Utils.isDefined(this.logScale)) {
                chartOptions.vAxis.logScale = ("" + this.logScale) == true;
            }
            if (this.textPosition) {
                chartOptions.vAxis.textPosition = this.textPosition;
            }

            if (Utils.isDefined(this.minValue)) {
                chartOptions.vAxis.viewWindow.min = parseFloat(this.minValue);
            }
            if (Utils.isDefined(this.maxValue)) {
                chartOptions.vAxis.viewWindow.max = parseFloat(this.maxValue);
            }
            if (!isNaN(this.getVAxisMaxValue())) {
                chartOptions.vAxis.maxValue = this.getVAxisMaxValue();
            }
            if (!isNaN(this.getVAxisMinValue())) {
                chartOptions.vAxis.minValue = parseFloat(this.getVAxisMinValue());
            }
            return new google.visualization.Histogram(document.getElementById(this.getChartId()));
        },

    });
}


function RamaddaTextChart(displayManager, id, chartType, properties) {
    let SUPER = new RamaddaGoogleChart(displayManager, id, chartType, properties);
    RamaddaUtil.inherit(this, SUPER);
    $.extend(this, {
        getFieldsToSelect: function(pointData) {
            return pointData.getNonGeoFields();
        },
    });
}




function PiechartDisplay(displayManager, id, properties) {
    RamaddaUtil.inherit(this, new RamaddaTextChart(displayManager, id, DISPLAY_PIECHART, properties));
    addRamaddaDisplay(this);
    $.extend(this, {
        canDoGroupBy: function() {
            return true;
        },
        getGroupBy: function() {
            if (!this.groupBy && this.groupBy != "") {
                var stringField = this.getFieldOfType(this.allFields, "string");
                if (stringField) {
                    this.groupBy = stringField.getId();
                }
            }
            return this.groupBy;
        },
        getChartDiv: function() {
            var chartId = this.getDomId(ID_CHART);
            var divAttrs = [ATTR_ID, chartId];
            divAttrs.push("style");
            var style = "";
            var width = this.getChartWidth();
            if (width) {
                if (width > 0)
                    style += "width:" + width + "px;";
                else if (width < 0)
                    style += "width:" + (-width) + "%;";
                else
                    style += "width:" + width + ";";
            } else {
                style += "width:" + "100%;";
            }
            var height = this.getChartHeight();
            if (height) {
                if (height > 0)
                    style += "height:" + height + "px;";
                else if (height < 0)
                    style += "height:" + (-height) + "%;";
                else
                    style += "height:" + height + ";";
            } else {
                style += "height:" + "100%;";
            }
            divAttrs.push(style);
            return HtmlUtils.div(divAttrs, "");
        },
        doMakeGoogleChart: function(dataList, props, selectedFields, chartOptions) {
            chartOptions.tooltip = {
                textStyle: {
                    color: '#000000'
                },
                showColorCode: true
            };
            if (this.getProperty("bins", null)) {
                chartOptions.title = "Bins: " + this.getDataValues(dataList[0])[1];
            } else {
                chartOptions.title = this.getDataValues(dataList[0])[0] + " - " + this.getDataValues(dataList[0])[1];
            }

            if (this.is3D) {
                chartOptions.is3D = true;
            }
            if (this.pieHole) {
                chartOptions.pieHole = this.pieHole;
            }
            if (this.sliceVisibilityThreshold) {
                chartOptions.sliceVisibilityThreshold = this.sliceVisibilityThreshold;
            }
            return new google.visualization.PieChart(document.getElementById(this.getChartId()));
        },
        makeDataTable: function(dataList, props, selectedFields) {
            var dataTable = new google.visualization.DataTable();
            var list = [];
            var groupBy = this.groupByField;
            var header = this.getDataValues(dataList[0]);
            dataTable.addColumn("string", header[0]);
            dataTable.addColumn("number", header[1]);
            //                    dataTable.addColumn({type:'string',role:'tooltip'});
            if (this.getProperty("bins", null)) {
                var bins = parseInt(this.getProperty("bins", null));
                var min = Number.MAX_VALUE;
                var max = Number.MIN_VALUE;
                var haveMin = false;
                var haveMax = false;
                if (this.getProperty("binMin")) {
                    min = parseFloat(this.getProperty("binMin"));
                    haveMin = true;
                }
                if (this.getProperty("binMax")) {
                    max = parseFloat(this.getProperty("binMax"));
                    haveMax = true;
                }

                var goodValues = [];
                for (var i = 1; i < dataList.length; i++) {
                    var tuple = this.getDataValues(dataList[i]);
                    var value = tuple[1];
                    if (!Utils.isRealNumber(value)) {
                        continue;
                    }
                    if (!haveMin)
                        min = Math.min(value, min);
                    if (!haveMax)
                        max = Math.max(value, max);
                    goodValues.push(value);
                }


                var binList = [];
                var step = (max - min) / bins;
                for (var binIdx = 0; binIdx < bins; binIdx++) {
                    binList.push({
                        min: min + binIdx * step,
                        max: min + (binIdx + 1) * step,
                        values: []
                    });
                }

                for (var rowIdx = 0; rowIdx < goodValues.length; rowIdx++) {
                    var value = goodValues[rowIdx];
                    var ok = false;

                    for (var binIdx = 0; binIdx < binList.length; binIdx++) {
                        if (value < binList[binIdx].min || (value >= binList[binIdx].min && value <= binList[binIdx].max)) {
                            binList[binIdx].values.push(value);
                            ok = true;
                            break;
                        }
                    }
                    if (!ok) {
                        binList[binList.length - 1].values.push(value);
                    }
                }
                for (var binIdx = 0; binIdx < bins; binIdx++) {
                    var bin = binList[binIdx];
                    list.push(["Bin:" + this.formatNumber(bin.min) + "-" + this.formatNumber(bin.max),
                        bin.values.length
                    ]);
                }
            } else {
                for (var i = 1; i < dataList.length; i++) {
                    var tuple = this.getDataValues(dataList[i]);
                    var s = "" + (tuple.length == 1 ? "#" + i : tuple[0]);
                    var v = tuple.length == 1 ? tuple[0] : tuple[1];
                    list.push([s, v]);
                }
            }
            dataTable.addRows(list);
            return dataTable;
        }
    });


}


function SankeyDisplay(displayManager, id, properties) {
    RamaddaUtil.inherit(this, new RamaddaTextChart(displayManager, id, DISPLAY_SANKEY, properties));
    addRamaddaDisplay(this);
    $.extend(this, {
        doMakeGoogleChart: function(dataList, props, selectedFields, chartOptions) {
            chartOptions.height = parseInt(this.getProperty("chartHeight", this.getProperty("height", "400")));
            chartOptions.sankey = {
                node: {
                    colors: this.colors,
                    width: 5,
                },
                link: {
                    colorMode: 'source',
                    colors: this.colors,
                    color: {
                        //                                stroke:'black',
                        //strokeWidth:1,
                    }
                }
            }
            return new google.visualization.Sankey(document.getElementById(this.getChartId()));
        },
        defaultSelectedToAll: function() {
            return true;
        },
        makeDataTable: function(dataList, props, selectedFields) {
            dataList = this.filterData(dataList, selectedFields);
            if (!this.getProperty("doCategories", false)) {
                var values = this.makeDataArray(dataList);
                return google.visualization.arrayToDataTable(values);
            }
            var strings = [];
            for (var i = 0; i < selectedFields.length; i++) {
                var field = selectedFields[i];
                if (field.getType() == "string") {
                    strings.push(field);
                }
            }
            var values = [];
            values.push(["characteristic 1", "characteristic 2", "value"]);
            for (var i = 1; i < strings.length; i++) {
                var field1 = strings[i - 1];
                var field2 = strings[i];
                var cnts = {};
                for (var r = 1; r < dataList.length; r++) {
                    var row = this.getDataValues(dataList[r]);
                    var value1 = row[i - 1];
                    var value2 = row[i];
                    var key = value1 + "-" + value2;
                    if (!cnts[key]) {
                        cnts[key] = {
                            v1: value1,
                            v2: value2,
                            cnt: 0
                        }
                    }
                    cnts[key].cnt++;
                }
                for (a in cnts) {
                    values.push([cnts[a].v1, cnts[a].v2, cnts[a].cnt]);
                }
            }
            return google.visualization.arrayToDataTable(values);
        }
    });
}

function WordtreeDisplay(displayManager, id, properties) {
    RamaddaUtil.inherit(this, new RamaddaTextChart(displayManager, id, DISPLAY_WORDTREE, properties));
    addRamaddaDisplay(this);
    $.extend(this, {
        handleEventRecordSelection: function(source, args) {},
        doMakeGoogleChart: function(dataList, props, selectedFields, chartOptions) {
            if (this.getProperty("chartHeight"))
                chartOptions.height = parseInt(this.getProperty("chartHeight"));
            if (this.getProperty("wordColors")) {
                var tmp = this.getProperty("wordColors").split(",");
                var colors = [];
                for (var i = 0; i < 3 && i < tmp.length; i++) {
                    colors.push(tmp[i]);
                }
                if (colors.length == 3)
                    chartOptions.colors = colors;
            }

            if (this.getProperty("chartWidth")) {
                chartOptions.width = parseInt(this.getProperty("chartWidth"));
            }

            chartOptions.wordtree = {
                format: 'implicit',
                wordSeparator: "_SEP_",
                word: this.getProperty("treeRoot", "root"),
                //                    type: this.getProperty("treeType","double")

            }
            if (this.getProperty("maxFontSize")) {
                chartOptions.maxFontSize = parseInt(this.getProperty("maxFontSize"));
            }

            return new google.visualization.WordTree(document.getElementById(this.getChartId()));
        },


        makeDataTable: function(dataList, props, selectedFields) {
            //null ->get all data
            var root = this.getProperty("treeRoot", "root");
            var records = this.filterData(null, selectedFields);
            var fields = this.getSelectedFields(this.getData().getRecordFields());
            var valueField = this.getFieldById(null, this.getProperty("colorBy"));
            var values = [];
            var typeTuple = ["phrases"];
            values.push(typeTuple);
            var fixedSize = this.getProperty("fixedSize");
            if (valueField)
                fixedSize = 1;
            if (fixedSize) typeTuple.push("size");
            if (valueField)
                typeTuple.push("value");
            var fieldInfo = {};

            var header = "";
            for (var i = 0; i < fields.length; i++) {
                var field = fields[i];
                if (header != "")
                    header += " -&gt;";
                header += field.getLabel();
                if (!field.isFieldNumeric()) continue;
                var column = this.getColumnValues(records, field);
                var buckets = [];
                var argBuckets = this.getProperty("buckets." + field.getId(), this.getProperty("buckets", null));
                var min, max;
                if (argBuckets) {
                    var argBucketLabels = this.getProperty("bucketLabels." + field.getId(), this.getProperty("bucketLabels", null));
                    var bucketLabels;
                    if (argBucketLabels)
                        bucketLabels = argBucketLabels.split(",");
                    var bucketList = argBuckets.split(",");
                    var prevValue = 0;
                    for (var bucketIdx = 0; bucketIdx < bucketList.length; bucketIdx++) {
                        var v = parseFloat(bucketList[bucketIdx]);
                        if (bucketIdx == 0) {
                            min = v;
                            max = v;
                        }
                        min = Math.min(min, v);
                        max = Math.max(max, v);
                        if (bucketIdx > 0) {
                            var label;
                            if (bucketLabels && i <= bucketLabels.length)
                                label = bucketLabels[bucketIdx - 1];
                            else
                                label = Utils.formatNumber(prevValue, true) + "-" + Utils.formatNumber(v, true);
                            buckets.push({
                                min: prevValue,
                                max: v,
                                label: label
                            });
                        }
                        prevValue = v;
                    }
                } else {
                    var numBuckets = parseInt(this.getProperty("numBuckets." + field.getId(), this.getProperty("numBuckets", 10)));
                    min = column.min;
                    max = column.max;
                    var step = (column.max - column.min) / numBuckets;
                    for (var bucketIdx = 0; bucketIdx < numBuckets; bucketIdx++) {
                        var r1 = column.min + (bucketIdx * step);
                        var r2 = column.min + ((bucketIdx + 1) * step);
                        var label = Utils.formatNumber(r1, true) + "-" + Utils.formatNumber(r2, true);
                        buckets.push({
                            min: r1,
                            max: r2,
                            label: label
                        });
                    }
                }
                fieldInfo[field.getId()] = {
                    min: min,
                    max: max,
                    buckets: buckets
                };
            }

            var sep = "_SEP_";
            for (var rowIdx = 0; rowIdx < records.length; rowIdx++) {
                var row = this.getDataValues(records[rowIdx]);
                var string = root;
                for (var fieldIdx = 0; fieldIdx < fields.length; fieldIdx++) {
                    var field = fields[fieldIdx];
                    string += sep;
                    var value = row[field.getIndex()];
                    if (field.isFieldNumeric()) {
                        var info = fieldInfo[field.getId()];
                        for (var bucketIdx = 0; bucketIdx < info.buckets.length; bucketIdx++) {
                            var bucket = info.buckets[bucketIdx];
                            if (value >= bucket.min && value <= bucket.max) {
                                value = bucket.label;
                                break;
                            }
                        }
                    }
                    string += value;
                }
                var data = [string.trim()];
                if (fixedSize) data.push(parseInt(fixedSize));
                if (valueField)
                    data.push(row[valueField.getIndex()]);
                values.push(data);
            }
            if (this.getProperty("header")) {
                header = this.getProperty("header", "");
            } else {
                header = "<b>Fields: </b>" + header;
                if (this.getProperty("headerPrefix"))
                    header = this.getProperty("headerPrefix") + " " + header;
            }
            this.writeHtml(ID_DISPLAY_TOP, header);
            return google.visualization.arrayToDataTable(values);
        },
    });
}



function TableDisplay(displayManager, id, properties) {
    RamaddaUtil.inherit(this, new RamaddaTextChart(displayManager, id, DISPLAY_TABLE, properties));
    addRamaddaDisplay(this);
    $.extend(this, {
        canDoGroupBy: function() {
            return true;
        },
        defaultSelectedToAll: function() {
            return true;
        },
        doMakeGoogleChart: function(dataList, props, selectedFields, chartOptions) {
            chartOptions.height = null;
            if (this.chartHeight) {
                chartOptions.height = this.chartHeight;
            }
            if (chartOptions.height == null) {
                var height = this.getProperty("height", null);
                if (height) {
                    chartOptions.height = height;
                }
            }
            if (chartOptions.height == null) {
                chartOptions.height = "300px";
            }
            chartOptions.allowHtml = true;
            if (dataList.length && this.getDataValues(dataList[0]).length > 4) {
                chartOptions.cssClassNames = {
                    headerCell: 'display-table-header-max'
                };
            } else {
                chartOptions.cssClassNames = {
                    headerCell: 'display-table-header'
                };
            }
            return new google.visualization.Table(document.getElementById(this.getChartId()));
        },
        makeDataTable: function(dataList, props, selectedFields) {
            dataList = this.filterData(dataList, selectedFields);
            var rows = this.makeDataArray(dataList);
            var data = [];
            for (var rowIdx = 0; rowIdx < rows.length; rowIdx++) {
                var row = rows[rowIdx];
                for (var colIdx = 0; colIdx < row.length; colIdx++) {
                    if ((typeof row[colIdx]) == "string") {
                        row[colIdx] = row[colIdx].replace(/\n/g, "<br>");
                    }
                }
                data.push(row);
            }
            return google.visualization.arrayToDataTable(data);
        }
    });

}



function BubbleDisplay(displayManager, id, properties) {
    RamaddaUtil.inherit(this, new RamaddaTextChart(displayManager, id, DISPLAY_BUBBLE, properties));
    addRamaddaDisplay(this);
    $.extend(this, {
        makeDataTable: function(dataList, props, selectedFields) {
            return google.visualization.arrayToDataTable(this.makeDataArray(dataList));
        },
        doMakeGoogleChart: function(dataList, props, selectedFields, chartOptions) {
            var ct = this.getColorTable(true);
            if (ct) {
                chartOptions.colors = ct;
            } else if (!this.colors) {
                chartOptions.colors = this.colorList;
            }
            if (chartOptions.colors) {
                chartOptions.colors = Utils.getColorTable("rainbow", true);
            }

            chartOptions.chartArea = {
                left: 100,
                top: 10,
                width: '98%',
                height: '90%'
            }
            chartOptions.colorAxis = {
                legend: {
                    position: "in"
                }
            }

            chartOptions.bubble = {
                textStyle: {
                    auraColor: "none"
                },
                stroke: "#666"
            };
            chartOptions.hAxis = {};
            chartOptions.vAxis = {};
            header = this.getDataValues(dataList[0]);
            chartOptions.hAxis.format = this.getProperty("hAxisFormat", null);
            chartOptions.vAxis.format = this.getProperty("vAxisFormat", null);
            chartOptions.hAxis.title = this.getProperty("hAxisTitle", header.length > 1 ? header[1] : null);
            chartOptions.vAxis.title = this.getProperty("vAxisTitle", header.length > 2 ? header[2] : null);
            return new google.visualization.BubbleChart(document.getElementById(this.getChartId()));
        }

    });
}


function BartableDisplay(displayManager, id, properties) {
    RamaddaUtil.inherit(this, new RamaddaSeriesChart(displayManager, id, DISPLAY_BARTABLE, properties));
    addRamaddaDisplay(this);
    $.extend(this, {
        xgetIncludeIndexIfDate: function() {
            return true;
        },
        doMakeGoogleChart: function(dataList, props, selectedFields, chartOptions) {
            var height = "";
            if (Utils.isDefined(this.chartHeight)) {
                height = this.chartHeight;
            } else {
                if (dataList.length > 1) {
                    var numBars = dataList.length;
                    if (this.isStacked) {
                        height = numBars * 22;
                    } else {
                        height = numBars * 22 + numBars * 14 * (this.getDataValues(dataList[0]).length - 2);
                    }
                }
            }

            $.extend(chartOptions, {
                title: "the title",
                bars: 'horizontal',
                colors: this.colorList,
                width: (Utils.isDefined(this.chartWidth) ? this.chartWidth : "100%"),
                chartArea: {
                    left: '30%',
                    top: 0,
                    width: '70%',
                    height: '80%'
                },
                height: height,
                bars: 'horizontal',
                tooltip: {
                    showColorCode: true
                },
                legend: {
                    position: 'none'
                },
            });

            if (Utils.isDefined(this.isStacked)) {
                chartOptions.isStacked = this.isStacked;
            }

            if (this.hAxis)
                chartOptions.hAxis = {
                    title: this.hAxis
                };
            if (this.vAxis)
                chartOptions.vAxis = {
                    title: this.vAxis
                };
            return new google.charts.Bar(document.getElementById(this.getChartId()));
        },
        getDefaultSelectedFields: function(fields, dfltList) {
            var f = [];
            for (i = 0; i < fields.length; i++) {
                var field = fields[i];
                if (!field.isNumeric) {
                    f.push(field);
                    break;
                }
            }
            for (i = 0; i < fields.length; i++) {
                var field = fields[i];
                if (field.isNumeric) {
                    f.push(field);
                    break;
                }
            }
            return f;
        }
    });


}


function TreemapDisplay(displayManager, id, properties) {
    let SUPER = new RamaddaTextChart(displayManager, id, DISPLAY_TREEMAP, properties);
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);
    $.extend(this, {
        handleEventRecordSelection: function(source, args) {},
        getFieldsToSelect: function(pointData) {
            return pointData.getRecordFields();
        },
        tooltips: {},
        makeChartOptions: function(dataList, props, selectedFields) {
            let _this = this;
            var tooltip = function(row, size, value) {
                if (_this.tooltips[row]) {
                    return _this.tooltips[row];
                }
                return "<div class='display-treemap-tooltip-outer'><div class='display-treemap-tooltip''><i>left-click: go down<br>right-click: go up</i></div></div>";
            };
            var chartOptions = SUPER.makeChartOptions.call(this, dataList, props, selectedFields);
            $.extend(chartOptions, {
                highlightOnMouseOver: true,
                generateTooltip: tooltip,
                maxDepth: parseInt(this.getProperty("maxDepth", 2)),
                maxPostDepth: parseInt(this.getProperty("maxPostDepth", 3)),
            });

            return chartOptions;
        },
        defaultSelectedToAll: function() {
            return true;
        },

        doMakeGoogleChart: function(dataList, props, selectedFields, chartOptions) {
            var dataTable = this.makeDataTable(dataList, props, selectedFields);
            if (!dataTable) return null;
            return new google.visualization.TreeMap(document.getElementById(this.getChartId()));
        },

        addTuple: function(data, colorField, seen, value, parent, n1, n2) {
            var ovalue = value;
            var cnt = 0;
            if (Utils.isDefined(seen[value]) && parent) {
                value = parent + ":" + value;
            }
            while (true) {
                if (!Utils.isDefined(seen[value])) {
                    seen[value] = true;
                    break;
                }
                value = ovalue + " " + (++cnt);
            }
            var tuple = [value, parent, n1];
            if (colorField) tuple.push(n2);
            data.push(tuple);
            return value;
        },

        valueClicked: function(field, value) {
            var allFields = this.getData().getRecordFields();
            field = this.getFieldById(allFields, field);
            this.propagateEvent("handleEventFieldValueSelect", {
                field: field,
                value: value
            });
        },
        makeDataTable: function(dataList, props, selectedFields) {
            var records = this.filterData();
            if (!records) {
                return null;
            }
            var allFields = this.getData().getRecordFields();
            var fields = this.getSelectedFields(allFields);
            if (fields.length == 0)
                fields = allFields;
            var strings = this.getFieldsOfType(fields, "string");
            if (strings.length < 2) {
                this.displayError("No string fields specified");
                return null;
            }
            var addPrefix = this.getProperty("addPrefix", true);
            var sizeField = this.getFieldById(allFields, this.getProperty("sizeBy"));
            var colorField = this.getFieldById(allFields, this.getProperty("colorBy"));
            var values = this.getFieldsOfType(fields, "numeric");
            if (!sizeField && values.length > 0)
                sizeField = values[0];
            if (!colorField && values.length > 1)
                colorField = values[1];

            var tooltipFields = [];
            var toks = this.getProperty("tooltipFields", "").split(",");
            for (var i = 0; i < toks.length; i++) {
                var tooltipField = this.getFieldById(null, toks[i]);
                if (tooltipField)
                    tooltipFields.push(tooltipField);
            }
            if (tooltipFields.length == 0) tooltipFields = allFields;

            this.tooltips = {};

            var columns = [];
            for (var fieldIndex = 0; fieldIndex < strings.length; fieldIndex++) {
                var field = strings[fieldIndex];
                columns.push(this.getColumnValues(records, field).values);
            }

            var data = [];
            var leafs = [];
            var tmptt = [];
            var seen = {};
            this.addTuple(data, colorField, {}, "Node", "Parent", "Value", "Color");
            var root = strings[0].getLabel();
            this.addTuple(data, colorField, seen, root, null, 0, 0);
            var keys = {};
            var call = this.getGet();
            for (var rowIdx = 0; rowIdx < records.length; rowIdx++) {
                //               if(rowIdx>20) break;
                var row = this.getDataValues(records[rowIdx]);
                var key = "";
                var parentKey = "";
                for (var fieldIndex = 0; fieldIndex < strings.length - 1; fieldIndex++) {
                    var values = columns[fieldIndex];
                    if (key != "")
                        key += ":";
                    key += values[rowIdx];
                    if (!Utils.isDefined(keys[key])) {
                        var parent = Utils.isDefined(keys[parentKey]) ? keys[parentKey] : root;
                        var value = values[rowIdx];
                        if (addPrefix && fieldIndex > 0)
                            value = parent + ":" + value;
                        keys[key] = this.addTuple(data, colorField, seen, value, parent, 0, 0);
                    }
                    parentKey = key;
                }
                var parent = Utils.isDefined(keys[parentKey]) ? keys[parentKey] : root;
                var value = row[strings[strings.length - 1].getIndex()];
                var size = sizeField ? row[sizeField.getIndex()] : 1;
                var color = colorField ? row[colorField.getIndex()] : 0;
                value = this.addTuple(leafs, colorField, seen, value, parent, size, color);
                var tt = "<div class='display-treemap-tooltip-outer'><div class='display-treemap-tooltip''>";
                for (var f = 0; f < tooltipFields.length; f++) {
                    var v = row[tooltipFields[f].getIndex()];
                    var field = tooltipFields[f];
                    v = HtmlUtils.onClick(call + ".valueClicked('" + field.getId() + "','" + v + "')", v, []);
                    tt += "<b>" + field.getLabel() + "</b>" + ": " + v + "<br>";
                }
                tt += "</div></div>";
                tmptt.push(tt);
            }
            for (var i = 0; i < leafs.length; i++) {
                data.push(leafs[i]);
                this.tooltips[data.length - 2] = tmptt[i];
            }
            return google.visualization.arrayToDataTable(data);
        },
    });
}



function TimelinechartDisplay(displayManager, id, properties) {
    RamaddaUtil.inherit(this, new RamaddaTextChart(displayManager, id, DISPLAY_TIMELINECHART, properties));
    addRamaddaDisplay(this);
    $.extend(this, {
        doMakeGoogleChart: function(dataList, props, selectedFields, chartOptions) {
            return new google.visualization.Timeline(document.getElementById(this.getChartId()));
        },
        makeDataTable: function(dataList, props, selectedFields) {
            var records = this.filterData();
            var strings = [];
            var stringField = this.getFieldOfType(selectedFields, "string");
            if (!stringField)
                stringField = this.getFieldOfType(null, "string");
            var showLabel = this.getProperty("showLabel", true);
            var labelFields = [];
            var labelFieldsTemplate = this.getProperty("labelFieldsTemplate");
            var toks = this.getProperty("labelFields", "").split(",");
            for (var i = 0; i < toks.length; i++) {
                var field = this.getFieldById(null, toks[i]);
                if (field)
                    labelFields.push(field);
            }
            var dateFields = this.getFieldsOfType(selectedFields, "date");
            if (dateFields.length == 0)
                dateFields = this.getFieldsOfType(null, "date");
            var values = [];
            var dataTable = new google.visualization.DataTable();
            if (dateFields.length < 2) {
                throw new Error("Need to have at least 2 date fields");
            }
            if (stringField) {
                dataTable.addColumn({
                    type: 'string',
                    id: stringField.getLabel()
                });
            } else {
                dataTable.addColumn({
                    type: 'string',
                    id: "Index"
                });
            }
            if (labelFields.length > 0) {
                dataTable.addColumn({
                    type: 'string',
                    id: 'Label'
                });
            }
            dataTable.addColumn({
                type: 'date',
                id: dateFields[0].getLabel()
            });
            dataTable.addColumn({
                type: 'date',
                id: dateFields[1].getLabel()
            });
            for (var r = 0; r < records.length; r++) {
                var row = this.getDataValues(records[r]);
                var tuple = [];
                values.push(tuple);
                if (stringField && showLabel)
                    tuple.push(row[stringField.getIndex()]);
                else
                    tuple.push("#" + (r + 1));
                if (labelFields.length > 0) {
                    var label = "";
                    if (labelFieldsTemplate)
                        label = labelFieldsTemplate;
                    for (var l = 0; l < labelFields.length; l++) {
                        var f = labelFields[l];
                        var value = row[f.getIndex()];
                        if (labelFieldsTemplate) {
                            label = label.replace("{" + f.getId() + "}", value);
                        } else {
                            label += value + " ";
                        }

                    }
                    tuple.push(label);
                }
                tuple.push(row[dateFields[0].getIndex()]);
                tuple.push(row[dateFields[1].getIndex()]);
            }
            dataTable.addRows(values);
            return dataTable;
        }
    });
}



function CalendarDisplay(displayManager, id, properties) {
    RamaddaUtil.inherit(this, new RamaddaGoogleChart(displayManager, id, DISPLAY_CALENDAR, properties));
    addRamaddaDisplay(this);
    RamaddaUtil.inherit(this, {
        doMakeGoogleChart: function(dataList, props, selectedFields, chartOptions) {
            chartOptions.calendar = {
                cellSize: parseInt(this.getProperty("cellSize", 15))
            };
            //If a calendar is show in tabs then it never returns from the draw
            if (this.jq(ID_CHART).width() == 0) {
                return;
            }
            return new google.visualization.Calendar(document.getElementById(this.getChartId()));
        },

        defaultSelectedToAll: function() {
            return true;
        },
        xgetContentsStyle: function() {
            var height = this.getProperty("height", 200);
            if (height > 0) {
                return " height:" + height + "px; " + " max-height:" + height + "px; overflow-y: auto;";
            }
            return "";
        },
        canDoMultiFields: function() {
            return false;
        },
        getIncludeIndexIfDate: function() {
            return true;
        },
        makeDataTable: function(dataList, props, selectedFields) {
            var dataTable = new google.visualization.DataTable();
            var header = this.getDataValues(dataList[0]);
            if (header.length < 2) return null;
            dataTable.addColumn({
                type: 'date',
                id: 'Date'
            });
            dataTable.addColumn({
                type: 'number',
                id: header[1]
            });
            dataTable.addColumn({
                type: 'string',
                role: 'tooltip',
                'p': {
                    'html': true
                }
            });
            var haveMissing = false;
            var missing = this.getProperty("missingValue", null);
            if (missing) {
                haveMissing = true;
                missing = parseFloat(missing);
            }
            var list = [];
            var cnt = 0;
            var options = {
                weekday: 'long',
                year: 'numeric',
                month: 'long',
                day: 'numeric'
            };
            for (var i = 1; i < dataList.length; i++) {
                var value = this.getDataValues(dataList[i])[1];
                if (value == NaN) continue;
                if (haveMissing && value == missing) {
                    continue;
                }
                cnt++;
                var dttm = this.formatDate(this.getDataValues(dataList[i])[0], {
                    options: options
                });
                dttm = dttm.replace(/ /g, "&nbsp;");
                var tooltip = "<center><b>" + dttm + "</b></center>" +
                    "<b>" + header[1].replace(/ /g, "&nbsp;") + "</b>:&nbsp;" + this.formatNumber(value);
                tooltip = HtmlUtils.tag("div", ["style", "padding:5px;"], tooltip);
                list.push([this.getDataValues(dataList[i])[0], value, tooltip]);
            }
            dataTable.addRows(list);
            return dataTable;
        }
    });
}




function GaugeDisplay(displayManager, id, properties) {
    RamaddaUtil.inherit(this, new RamaddaGoogleChart(displayManager, id, DISPLAY_GAUGE, properties));
    addRamaddaDisplay(this);
    RamaddaUtil.inherit(this, {
        getChartHeight: function() {
            return this.getProperty("height", this.getChartWidth());
        },
        getChartWidth: function() {
            return this.getProperty("width", "150");
        },
        doMakeGoogleChart: function(dataList, props, selectedFields, chartOptions) {
            this.dataList = dataList;
            this.chartOptions = chartOptions;
            var min = Number.MAX_VALUE;
            var max = Number.MIN_VALUE;
            var setMinMax = true;
            for (var row = 1; row < dataList.length; row++) {
                var tuple = this.getDataValues(dataList[row]);
                //                        if(tuple.length>2) setMinMax = false;
                for (var col = 0; col < tuple.length; col++) {
                    if (!Utils.isNumber(tuple[col])) {
                        continue;
                    }
                    var value = tuple[col];
                    min = Math.min(min, value);
                    max = Math.max(max, value);
                }
            }
            min = Utils.formatNumber(min, true);
            max = Utils.formatNumber(max, true);
            if (Utils.isDefined(this.gaugeMin)) {
                setMinMax = true;
                min = parseFloat(this.gaugeMin);
            }
            if (Utils.isDefined(this.gaugeMax)) {
                setMinMax = true;
                max = parseFloat(this.gaugeMax);
            }
            if (setMinMax) {
                chartOptions.min = min;
                chartOptions.max = max;
            }
            return new google.visualization.Gauge(document.getElementById(this.getChartId()));
        },

        makeDataTable: function(dataList, props, selectedFields) {
            dataList = this.makeDataArray(dataList);
            if (!Utils.isDefined(this.index)) this.index = dataList.length - 1;
            var index = this.index + 1;
            var list = [];
            list.push(["Label", "Value"]);
            var header = this.getDataValues(dataList[0]);
            if (index >= dataList.length) index = dataList.length - 1;
            var row = this.getDataValues(dataList[index]);
            for (var i = 0; i < row.length; i++) {
                if (!Utils.isNumber(row[i])) continue;
                var h = header[i];
                if (h.length > 20) {
                    var index = h.indexOf("(");
                    if (index > 0) {
                        h = h.substring(0, index);
                    }
                }
                if (h.length > 20) {
                    h = h.substring(0, 19) + "...";
                }
                if (this.getProperty("gaugeLabel"))
                    h = this.getProperty("gaugeLabel");
                else if (this["gaugeLabel" + (i + 1)]) h = this["gaugeLabel" + (i + 1)];
                var value = row[i];
                list.push([h, Utils.formatNumber(value, true)]);
            }
            return google.visualization.arrayToDataTable(list);
        },
        setChartSelection: function(index) {
            if (this.chart) {
                this.index = index;
                var dataTable = this.makeDataTable(this.dataList);
                this.chart.draw(dataTable, this.chartOptions);
            }
        },
    });
}




function ScatterplotDisplay(displayManager, id, properties) {
    let SUPER = new RamaddaGoogleChart(displayManager, id, DISPLAY_SCATTERPLOT, properties);
    RamaddaUtil.inherit(this, SUPER);
    $.extend(this, {
        makeChartOptions: function(dataList, props, selectedFields) {
            var chartOptions = SUPER.makeChartOptions.call(this, dataList, props, selectedFields);
            chartOptions.curveType = null;
            chartOptions.lineWidth = 0;
            $.extend(chartOptions, {
                title: '',
                tooltip: {
                    isHtml: true
                },
                legend: 'none',
            });

            if (!chartOptions.chartArea) chartOptions.chartArea = {};
            $.extend(chartOptions.chartArea, {
                left: "10%",
                top: 10,
                height: "80%",
                width: "90%"
            });
            if (this.getShowTitle()) {
                chartOptions.title = this.getTitle(true);
            }

            if (dataList.length > 0 && this.getDataValues(dataList[0]).length > 1) {
                if (!chartOptions.hAxis) chartOptions.hAxis = {};
                $.extend(chartOptions.hAxis, {
                    title: this.getDataValues(dataList[0])[0]
                });
                if (!chartOptions.vAxis) chartOptions.vAxis = {};
                $.extend(chartOptions.vAxis, {
                    title: this.getDataValues(dataList[0])[1]
                });
                //We only have the one vAxis range for now
                if (!isNaN(this.getVAxisMinValue())) {
                    chartOptions.hAxis.minValue = this.getVAxisMinValue();
                    chartOptions.vAxis.minValue = this.getVAxisMinValue();
                }
                if (!isNaN(this.getVAxisMaxValue())) {
                    chartOptions.hAxis.maxValue = this.getVAxisMaxValue();
                    chartOptions.vAxis.maxValue = this.getVAxisMaxValue();
                }
            }
            return chartOptions;
        },
        doMakeGoogleChart: function(dataList, props, selectedFields, chartOptions) {
            var height = 400;
            if (Utils.isDefined(this.chartHeight)) {
                height = this.chartHeight;
            }
            var width = "100%";
            if (Utils.isDefined(this.chartWidth)) {
                width = this.chartWidth;
            }

            var chartId = this.getChartId();
            $("#" + chartId).css("width", width);
            $("#" + chartId).css("height", height);
            return new google.visualization.ScatterChart(document.getElementById(this.getChartId()));
        },

        getDefaultSelectedFields: function(fields, dfltList) {
            var f = [];
            for (i = 0; i < fields.length; i++) {
                var field = fields[i];
                if (field.isNumeric) {
                    f.push(field);
                    if (f.length >= 2)
                        break;
                }
            }
            return f;
        }
    });

    addRamaddaDisplay(this);
}




function RamaddaStatsDisplay(displayManager, id, properties, type) {
    var dflt = Utils.isDefined(properties["showDefault"]) ? properties["showDefault"] : true;
    $.extend(this, {
        showMin: dflt,
        showMax: dflt,
        showAverage: dflt,
        showStd: dflt,
        showPercentile: dflt,
        showCount: dflt,
        showTotal: dflt,
        showPercentile: dflt,
        showMissing: dflt,
        showUnique: dflt,
        showType: dflt,
        showText: dflt,
    });
    let SUPER = new RamaddaFieldsDisplay(displayManager, id, type || DISPLAY_STATS, properties);
    RamaddaUtil.inherit(this, SUPER);

    if (!type)
        addRamaddaDisplay(this);


    RamaddaUtil.defineMembers(this, {
        "map-display": false,
        needsData: function() {
            return true;
            //                return this.getProperty("loadData", false) || this.getCreatedInteractively();
        },
        handleEventPointDataLoaded: function(source, pointData) {
            if (!this.needsData()) {
                if (this.dataCollection == null) {
                    this.dataCollection = source.dataCollection;
                    this.updateUI();
                }
            }
        },
        getDefaultSelectedFields: function(fields, dfltList) {
            if (dfltList != null && dfltList.length > 0) {
                return dfltList;
            }
            var tuples = this.getStandardData(null, {
                includeIndex: false
            });
            var justOne = (tuples.length == 2);

            //get the numeric fields
            var l = [];
            for (i = 0; i < fields.length; i++) {
                var field = fields[i];
                if (!justOne && (!this.showText && !field.isNumeric)) continue;
                var lbl = field.getLabel().toLowerCase();
                if (lbl.indexOf("latitude") >= 0 || lbl.indexOf("longitude") >= 0) {
                    continue;
                }
                l.push(field);
            }
            return l;
        },

        getFieldsToSelect: function(pointData) {
            return pointData.getRecordFields();
        },
        defaultSelectedToAll: function() {
            return true;
        },
        fieldSelectionChanged: function() {
            SUPER.fieldSelectionChanged.call(this);
            this.updateUI();
        },
        updateUI: function() {
            SUPER.updateUI.call(this);
            if (!this.hasData()) {
                this.setContents(this.getLoadingMessage());
                return;
            }
            var dataList = this.getStandardData(null, {
                includeIndex: false
            });
            var allFields = this.dataCollection.getList()[0].getRecordFields();
            this.allFields = allFields;
            var fields = this.getSelectedFields([]);
            var fieldMap = {};
            var stats = [];
            var justOne = (dataList.length == 2);
            for (var rowIdx = 1; rowIdx < dataList.length; rowIdx++) {
                var tuple = this.getDataValues(dataList[rowIdx]);
                if (rowIdx == 1) {
                    for (var col = 0; col < tuple.length; col++) {
                        stats.push({
                            isNumber: false,
                            count: 0,
                            min: Number.MAX_SAFE_INTEGER,
                            uniqueMap: {},
                            unique: 0,
                            std: 0,
                            max: Number.MIN_SAFE_INTEGER,
                            total: 0,
                            numMissing: 0,
                            numNotMissing: 0,
                            type: null,
                            values: []
                        });
                    }
                }
                for (var fieldIdx = 0; fieldIdx < fields.length; fieldIdx++) {
                    var field = fields[fieldIdx];
                    var col = field.getIndex()
                    stats[col].type = field.getType();
                    var v = tuple[col];
                    if (v) {
                        if (!Utils.isDefined(stats[col].uniqueMap[v])) {
                            stats[col].uniqueMap[v] = 1;
                            stats[col].unique++;
                        } else {
                            stats[col].uniqueMap[v]++;
                        }
                    }
                    stats[col].isNumber = field.isNumeric;
                    stats[col].count++;
                    if (v == null) {
                        stats[col].numMissing++;
                    } else {
                        stats[col].numNotMissing++;
                    }
                    if (v && (typeof v == 'number')) {
                        var label = field.getLabel().toLowerCase();
                        if (label.indexOf("latitude") >= 0 || label.indexOf("longitude") >= 0) {
                            continue;
                        }
                        stats[col].total += v;
                        stats[col].max = Math.max(stats[col].max, v);
                        stats[col].min = Math.min(stats[col].min, v);
                        stats[col].values.push(v);
                    }
                }
            }


            if (this.showUnique) {
                for (var fieldIdx = 0; fieldIdx < fields.length; fieldIdx++) {
                    var field = fields[fieldIdx];
                    var col = field.getIndex();
                    stats[col].uniqueMax = 0;
                    stats[col].uniqueValue = "";
                    for (var v in stats[col].uniqueMap) {
                        var count = stats[col].uniqueMap[v];
                        if (count > stats[col].uniqueMax) {
                            stats[col].uniqueMax = count;
                            stats[col].uniqueValue = v;
                        }
                    }
                }
            }

            if (this.showStd) {
                for (var fieldIdx = 0; fieldIdx < fields.length; fieldIdx++) {
                    var field = fields[fieldIdx];
                    var col = field.getIndex();
                    var values = stats[col].values;
                    if (values.length > 0) {
                        var average = stats[col].total / values.length;
                        var stdTotal = 0;
                        for (var i = 0; i < values.length; i++) {
                            var diff = values[i] - average;
                            stdTotal += diff * diff;
                        }
                        var mean = stdTotal / values.length;
                        stats[col].std = Math.sqrt(mean);
                    }
                }
            }
            var border = (justOne ? "0" : "1");
            var html = HtmlUtils.openTag("table", ["border", border, "bordercolor", "#ccc", "class", "display-stats", "cellspacing", "1", "cellpadding", "5"]);
            var dummy = ["&nbsp;"];
            if (!justOne) {
                header = [""];
                if (this.showCount) {
                    header.push("Count");
                    dummy.push("&nbsp;");
                }
                if (this.showMin) {
                    header.push("Min");
                    dummy.push("&nbsp;");
                }
                if (this.showPercentile) {
                    header.push("25%");
                    dummy.push("&nbsp;");
                    header.push("50%");
                    dummy.push("&nbsp;");
                    header.push("75%");
                    dummy.push("&nbsp;");
                }
                if (this.showMax) {
                    header.push("Max");
                    dummy.push("&nbsp;");
                }
                if (this.showTotal) {
                    header.push("Total");
                    dummy.push("&nbsp;");
                }
                if (this.showAverage) {
                    header.push("Average");
                    dummy.push("&nbsp;");
                }
                if (this.showStd) {
                    header.push("Std");
                    dummy.push("&nbsp;");
                }
                if (this.showUnique) {
                    header.push("# Unique");
                    dummy.push("&nbsp;");
                    header.push("Top");
                    dummy.push("&nbsp;");
                    header.push("Freq.");
                    dummy.push("&nbsp;");
                }
                if (this.showMissing) {
                    header.push("Not&nbsp;Missing");
                    dummy.push("&nbsp;");
                    header.push("Missing");
                    dummy.push("&nbsp;");
                }
                if (this.showType) {
                    header.push("Type");
                    dummy.push("&nbsp;");
                }
                html += HtmlUtils.tr(["valign", "bottom"], HtmlUtils.tds(["class", "display-stats-header", "align", "center"], header));
            }
            var cats = [];
            var catMap = {};
            for (var fieldIdx = 0; fieldIdx < fields.length; fieldIdx++) {
                var field = fields[fieldIdx];
                var col = field.getIndex();
                var field = allFields[col];
                var right = "";
                var total = "&nbsp;";
                var label = field.getLabel().toLowerCase();
                var avg = stats[col].numNotMissing == 0 ? "NA" : this.formatNumber(stats[col].total / stats[col].numNotMissing);
                //Some guess work about when to show a total
                if (label.indexOf("%") < 0 && label.indexOf("percent") < 0 && label.indexOf("median") < 0) {
                    total = this.formatNumber(stats[col].total);
                }
                if (justOne) {
                    right = HtmlUtils.tds(["xalign", "right"], [this.formatNumber(stats[col].min)]);
                    continue;
                }
                var values = [];
                if (!stats[col].isNumber && this.showText) {
                    if (this.showCount)
                        values.push(stats[col].count);
                    if (this.showMin)
                        values.push("-");
                    if (this.showPercentile) {
                        values.push("-");
                        values.push("-");
                        values.push("-");
                    }
                    if (this.showMax)
                        values.push("-");
                    values.push("-");
                    if (this.showAverage) {
                        values.push("-");
                    }
                    if (this.showStd) {
                        values.push("-");
                    }
                    if (this.showUnique) {
                        values.push(stats[col].unique);
                        values.push(stats[col].uniqueValue);
                        values.push(stats[col].uniqueMax);
                    }
                    if (this.showMissing) {
                        values.push(stats[col].numNotMissing);
                        values.push(stats[col].numMissing);
                    }
                } else {
                    if (this.showCount) {
                        values.push(stats[col].count);
                    }
                    if (this.showMin) {
                        values.push(this.formatNumber(stats[col].min));
                    }
                    if (this.showPercentile) {
                        var range = stats[col].max - stats[col].min;
                        values.push(this.formatNumber(stats[col].min + range * 0.25));
                        values.push(this.formatNumber(stats[col].min + range * 0.50));
                        values.push(this.formatNumber(stats[col].min + range * 0.75));
                    }
                    if (this.showMax) {
                        values.push(this.formatNumber(stats[col].max));
                    }
                    if (this.showTotal) {
                        values.push(total);
                    }
                    if (this.showAverage) {
                        values.push(avg);
                    }
                    if (this.showStd) {
                        values.push(this.formatNumber(stats[col].std));
                    }
                    if (this.showUnique) {
                        values.push(stats[col].unique);
                        if (Utils.isNumber(stats[col].uniqueValue)) {
                            values.push(this.formatNumber(stats[col].uniqueValue));
                        } else {
                            values.push(stats[col].uniqueValue);
                        }
                        values.push(stats[col].uniqueMax);
                    }
                    if (this.showMissing) {
                        values.push(stats[col].numNotMissing);
                        values.push(stats[col].numMissing);
                    }

                }
                if (this.showType) {
                    values.push(stats[col].type);
                }
                right = HtmlUtils.tds(["align", "right"], values);
                var align = (justOne ? "right" : "left");
                var label = field.getLabel();
                var toks = label.split("!!");
                var tooltip = "";
                tooltip += field.getId();
                if (field.description && field.description != "") {
                    tooltip += "\n" + field.description + "\n";
                }
                label = toks[toks.length - 1];
                if (justOne) {
                    label += ":";
                }
                label = label.replace(/ /g, "&nbsp;")
                var row = HtmlUtils.tr([], HtmlUtils.td(["align", align], "<b>" + HtmlUtils.tag("div", ["title", tooltip], label) + "</b>") + right);
                if (justOne) {
                    html += row;
                } else {
                    html += row;
                }
            }
            html += "</table>";
            this.setContents(html);
            this.initTooltip();
            //always propagate the event when loaded
            this.displayManager.propagateEventRecordSelection(this,
                this.dataCollection.getList()[0], {
                    index: 0
                });
        },
        handleEventRecordSelection: function(source, args) {
            //                this.lastHtml = args.html;
            //                this.setContents(args.html);
        }
    });
}



function RamaddaRecordsDisplay(displayManager, id, properties, type) {
    let SUPER = new RamaddaFieldsDisplay(displayManager, id, DISPLAY_RECORDS, properties);
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
        needsData: function() {
            return true;
        },
        handleEventPointDataLoaded: function(source, pointData) {
            if (!this.needsData()) {
                if (this.dataCollection == null) {
                    this.dataCollection = source.dataCollection;
                    this.updateUI();
                }
            }
        },
        getFieldsToSelect: function(pointData) {
            return pointData.getRecordFields();
        },
        defaultSelectedToAll: function() {
            return true;
        },
        fieldSelectionChanged: function() {
            SUPER.fieldSelectionChanged.call(this);
            this.updateUI();
        },
        updateUI: function() {
            SUPER.updateUI.call(this);
            var records = this.filterData();
            if (!records) {
                this.setContents(this.getLoadingMessage());
                return;
            }
            var fields = this.getSelectedFields(this.getData().getRecordFields());
            var html = "";
            for (var rowIdx = 0; rowIdx < records.length; rowIdx++) {
                var tuple = this.getDataValues(records[rowIdx]);
                for (var fieldIdx = 0; fieldIdx < fields.length; fieldIdx++) {
                    var field = fields[fieldIdx];
                    var v = tuple[field.getIndex()];
                    html += HtmlUtil.b(field.getLabel()) + ": " + v + "</br>";
                }
                html += "<p>";
            }
            var height = this.getProperty("maxHeight", "400px");
            if (!height.endsWith("px")) {
                height = height + "px";
            }
            this.setContents(HtmlUtil.div(["style", "max-height:" + height + ";overflow-y:auto;"], html));
        },
        handleEventRecordSelection: function(source, args) {
            //                this.lastHtml = args.html;
            //                this.setContents(args.html);
        }
    });
}





function RamaddaCrosstabDisplay(displayManager, id, properties) {
    let SUPER = new RamaddaFieldsDisplay(displayManager, id, DISPLAY_CROSSTAB, properties);
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);
    this.columns = this.getProperty("columns", "").split(",");
    this.rows = this.getProperty("rows", "").split(",");
    RamaddaUtil.defineMembers(this, {
        "map-display": false,
        needsData: function() {
            return true;
        },
        handleEventPointDataLoaded: function(source, pointData) {
            if (!this.needsData()) {
                if (this.dataCollection == null) {
                    this.dataCollection = source.dataCollection;
                    this.updateUI();
                }
            }
        },
        fieldSelectionChanged: function() {
            this.updateUI();
        },
        updateUI: function(pointData) {
            SUPER.updateUI.call(this, pointData);
            if (!this.hasData()) {
                this.setContents(this.getLoadingMessage());

                return;
            }
            var dataList = this.getStandardData(null, {
                includeIndex: false
            });
            var allFields = this.dataCollection.getList()[0].getRecordFields();
            var fieldMap = {};
            cols = [];
            rows = [];

            for (var j = 0; j < this.columns.length; j++) {
                var name = this.columns[j];
                for (var i = 0; i < allFields.length; i++) {
                    field = allFields[i];
                    if (name == field.getLabel() || name == ("#" + (i + 1))) {
                        cols.push(allFields[i]);
                        break;
                    }
                }
            }
            for (var j = 0; j < this.rows.length; j++) {
                var name = this.rows[j];
                for (var i = 0; i < allFields.length; i++) {
                    if (name == allFields[i].getLabel() || name == ("#" + (i + 1))) {
                        rows.push(allFields[i]);
                        break;
                    }
                }
            }
            var html = HtmlUtils.openTag("table", ["border", "1px", "bordercolor", "#ccc", "class", "display-stats", "cellspacing", "1", "cellpadding", "5"]);
            var uniques = {};
            var seen = {};
            for (var j = 0; j < cols.length; j++) {
                var col = cols[j];
                var key = col.getLabel();
                for (var rowIdx = 1; rowIdx < dataList.length; rowIdx++) {
                    var tuple = this.getDataValues(dataList[rowIdx]);
                    var colValue = tuple[col.getIndex()];
                    if (!(key in uniques)) {
                        uniques[key] = [];
                    }
                    var list = uniques[key];
                    if (list.indexOf(colValue) < 0) {
                        console.log(colValue);
                        list.push(colValue);
                    }
                }
            }

            for (key in uniques) {
                uniques[key].sort();
                console.log(uniques[key]);
            }


            for (var j = 0; j < cols.length; j++) {
                var col = cols[j];
                for (var rowIdx = 1; rowIdx < dataList.length; rowIdx++) {
                    var tuple = this.getDataValues(dataList[rowIdx]);
                    //                        var colValue = tuple;
                    //html += HtmlUtils.tr([],HtmlUtils.tds(["class","display-stats-header","align","center"],["","Min","Max","Total","Average"]));
                    for (var i = 0; i < rows.length; i++) {
                        var row = rows[j];


                    }
                }
            }
            html += "</table>";
            this.setContents(html);
            this.initTooltip();
        },
    });
}




function RamaddaCorrelationDisplay(displayManager, id, properties) {
    var ID_BOTTOM = "bottom";
    $.extend(this, {
        colorTable: "red_white_blue",
        colorByMin: "-1",
        colorByMax: "1",
    });

    let SUPER = new RamaddaFieldsDisplay(displayManager, id, DISPLAY_CORRELATION, properties);
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);

    RamaddaUtil.defineMembers(this, {
        "map-display": false,
        needsData: function() {
            return true;
        },
        getMenuItems: function(menuItems) {
            SUPER.getMenuItems.call(this, menuItems);
            var get = this.getGet();
            var tmp = HtmlUtils.formTable();
            var colorTable = this.getColorTableName();
            var ct = "<select id=" + this.getDomId("colortable") + ">";
            for (table in Utils.ColorTables) {
                if (table == colorTable)
                    ct += "<option selected>" + table + "</option>";
                else
                    ct += "<option>" + table + "</option>";
            }
            ct += "</select>";

            tmp += HtmlUtils.formEntry("Color Bar:", ct);

            tmp += HtmlUtils.formEntry("Color By Range:", HtmlUtils.input("", this.colorByMin, ["size", "7", ATTR_ID, this.getDomId("colorbymin")]) + " - " +
                HtmlUtils.input("", this.colorByMax, ["size", "7", ATTR_ID, this.getDomId("colorbymax")]));
            tmp += "</table>";
            menuItems.push(tmp);
        },
        initDialog: function() {
            SUPER.initDialog.call(this);
            var _this = this;
            var updateFunc = function() {
                _this.colorByMin = _this.jq("colorbymin").val();
                _this.colorByMax = _this.jq("colorbymax").val();
                _this.updateUI();

            };
            var func2 = function() {
                _this.colorTable = _this.jq("colortable").val();
                _this.updateUI();

            };
            this.jq("colorbymin").blur(updateFunc);
            this.jq("colorbymax").blur(updateFunc);
            this.jq("colortable").change(func2);
        },

        handleEventPointDataLoaded: function(source, pointData) {
            if (!this.needsData()) {
                if (this.dataCollection == null) {
                    this.dataCollection = source.dataCollection;
                    this.updateUI();
                }
            }
        },
        fieldSelectionChanged: function() {
            this.updateUI();
        },
        updateUI: function(pointData) {
            SUPER.updateUI.call(this, pointData);
            if (!this.hasData()) {
                this.setContents(this.getLoadingMessage());
                return;
            }
            var dataList = this.getStandardData(null, {
                includeIndex: false
            });
            var allFields = this.dataCollection.getList()[0].getRecordFields();
            var fields = this.getSelectedFields([]);
            if (fields.length == 0) fields = allFields;
            var fieldCnt = 0;
            for (var fieldIdx = 0; fieldIdx < fields.length; fieldIdx++) {
                var field1 = fields[fieldIdx];
                if (!field1.isFieldNumeric() || field1.isFieldGeo()) continue;
                fieldCnt++;
            }

            var html = HtmlUtils.openTag("table", ["border", "0", "class", "display-correlation", "width", "100%"]);
            var col1Width = 10 + "%";
            var width = 90 / fieldCnt + "%";
            html += "\n<tr valign=bottom><td class=display-heading width=" + col1Width + ">&nbsp;</td>";
            var short = this.getProperty("short", fieldCnt > 8);
            var showValue = this.getProperty("showValue", !short);
            var useId = this.getProperty("useId", true);
            var useIdTop = this.getProperty("useIdTop", useId);
            var useIdSide = this.getProperty("useIdSide", useId);
            for (var fieldIdx = 0; fieldIdx < fields.length; fieldIdx++) {
                var field1 = fields[fieldIdx];
                if (!field1.isFieldNumeric() || field1.isFieldGeo()) continue;
                var label = useIdTop ? field1.getId() : field1.getLabel();
                if (short) label = "";
                html += "<td align=center width=" + width + ">" + HtmlUtils.tag("div", ["class", "display-correlation-heading-top"], label) + "</td>";
            }
            html += "</tr>\n";

            var colors = null;
            colorByMin = parseFloat(this.colorByMin);
            colorByMax = parseFloat(this.colorByMax);
            colors = this.getColorTable(true);
            for (var fieldIdx1 = 0; fieldIdx1 < fields.length; fieldIdx1++) {
                var field1 = fields[fieldIdx1];
                if (!field1.isFieldNumeric() || field1.isFieldGeo()) continue;
                var label = useIdSide ? field1.getId() : field1.getLabel();
                html += "<tr valign=center><td>" + HtmlUtils.tag("div", ["class", "display-correlation-heading-side"], label.replace(/ /g, "&nbsp;")) + "</td>";
                var rowName = field1.getLabel();
                for (var fieldIdx2 = 0; fieldIdx2 < fields.length; fieldIdx2++) {
                    var field2 = fields[fieldIdx2];
                    if (!field2.isFieldNumeric() || field2.isFieldGeo()) continue;
                    var colName = field2.getLabel();
                    var t1 = 0;
                    var t2 = 0;
                    var cnt = 0;

                    for (var rowIdx = 1; rowIdx < dataList.length; rowIdx++) {
                        var tuple = this.getDataValues(dataList[rowIdx]);
                        var v1 = tuple[field1.getIndex()];
                        var v2 = tuple[field2.getIndex()];
                        t1 += v1;
                        t2 += v2;
                        cnt++;
                    }
                    var avg1 = t1 / cnt;
                    var avg2 = t2 / cnt;
                    var sum1 = 0;
                    var sum2 = 0;
                    var sum3 = 0;
                    for (var rowIdx = 1; rowIdx < dataList.length; rowIdx++) {
                        var tuple = this.getDataValues(dataList[rowIdx]);
                        var v1 = tuple[field1.getIndex()];
                        var v2 = tuple[field2.getIndex()];
                        sum1 += (v1 - avg1) * (v2 - avg2);
                        sum2 += (v1 - avg1) * (v1 - avg1);
                        sum3 += (v2 - avg2) * (v2 - avg2);
                    }
                    r = sum1 / Math.sqrt(sum2 * sum3);

                    var style = "";
                    if (colors != null) {
                        var percent = (r - colorByMin) / (colorByMax - colorByMin);
                        var index = parseInt(percent * colors.length);
                        if (index >= colors.length) index = colors.length - 1;
                        else if (index < 0) index = 0;
                        style = "background-color:" + colors[index];
                    }
                    var value = r.toFixed(3);
                    var label = value;
                    if (!showValue || short) label = "&nbsp;";
                    html += "<td class=display-correlation-cell align=right style=\"" + style + "\">" + HtmlUtils.tag("div", ["class", "display-correlation-element", "title", "&rho;(" + rowName + "," + colName + ") = " + value], label) + "</td>";
                }
                html += "</tr>";
            }
            html += "<tr><td></td><td colspan = " + (fieldCnt + 1) + ">" + HtmlUtils.div(["id", this.getDomId(ID_BOTTOM)], "") + "</td></tr>";
            html += "</table>";
            this.setContents(html);
            this.displayColorTable(colors, ID_BOTTOM, colorByMin, colorByMax);
            this.initTooltip();
            this.displayManager.propagateEventRecordSelection(this,
                this.dataCollection.getList()[0], {
                    index: 0
                });

        },
    });
}



function RamaddaRankingDisplay(displayManager, id, properties) {
    var ID_TABLE = "table";
    $.extend(this, {
        height: "500px;"
    });
    let SUPER = new RamaddaFieldsDisplay(displayManager, id, DISPLAY_RANKING, properties);
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);

    RamaddaUtil.defineMembers(this, {
        needsData: function() {
            return true;
        },
        handleEventPointDataLoaded: function(source, pointData) {
            if (!this.needsData()) {
                if (this.dataCollection == null) {
                    this.dataCollection = source.dataCollection;
                    this.updateUI();
                }
            }
        },
        fieldSelectionChanged: function() {
            this.updateUI();
        },
        updateUI: function(pointData) {
            SUPER.updateUI.call(this, pointData);
            if (!this.hasData()) {
                this.setContents(this.getLoadingMessage());
                return;
            }
            var dataList = this.getStandardData(null, {
                includeIndex: false
            });
            var allFields = this.dataCollection.getList()[0].getRecordFields();
            var fields = this.getSelectedFields([]);
            if (fields.length == 0) fields = allFields;
            var numericFields = this.getFieldsOfType(fields, "numeric");
            var sortField = this.getFieldById(numericFields, this.getProperty("sortField"));
            if (numericFields.length == 0) {
                this.setContents("No fields specified");
                return;
            }
            if (!sortField) {
                sortField = numericFields[0];
            }
            if (!sortField) {
                this.setContents("No fields specified");
                return;
            }

            var stringField = this.getFieldOfType(fields, "string");
            var menu = "<select class='ramadda-pulldown' id='" + this.getDomId("sortfields") + "'>";
            for (var i = 0; i < numericFields.length; i++) {
                var field = numericFields[i];
                var extra = "";
                if (field.getId() == sortField.getId()) extra = " selected ";
                menu += "<option value='" + field.getId() + "'  " + extra + " >" + field.getLabel() + "</option>\n";
            }
            menu += "</select>";
            if (this.getProperty("showRankingMenu", true)) {
                this.jq(ID_TOP_LEFT).html(menu);
            }
            var html = "";
            html += HtmlUtils.openTag("div", ["style", "max-height:100%;overflow-y:auto;"]);
            html += HtmlUtils.openTag("table", ["id", this.getDomId(ID_TABLE)]);
            var tmp = [];
            for (var rowIdx = 1; rowIdx < dataList.length; rowIdx++) {
                var obj = dataList[rowIdx];
                obj.originalRow = rowIdx;
                tmp.push(obj);
            }

            var cnt = 0;
            tmp.sort((a, b) => {
                var t1 = this.getDataValues(a);
                var t2 = this.getDataValues(b);
                var v1 = t1[sortField.getIndex()];
                var v2 = t2[sortField.getIndex()];
                if (v1 < v2) return 1;
                if (v1 > v2) return -1;
                return 0;
            });


            for (var rowIdx = 0; rowIdx < tmp.length; rowIdx++) {
                var obj = tmp[rowIdx];
                var tuple = this.getDataValues(obj);
                var label = "";
                if (stringField)
                    label = tuple[stringField.getIndex()];
                value = tuple[sortField.getIndex()];
                if (isNaN(value) || value === null) value = "NA";
                html += "<tr valign=top class='display-ranking-row' what='" + obj.originalRow + "'><td> #" + (rowIdx + 1) + "</td><td>&nbsp;" + label + "</td><td align=right>&nbsp;" +
                    value + "</td></tr>";
            }
            html += HtmlUtils.closeTag("table");
            html += HtmlUtils.closeTag("div");
            this.setContents(html);
            let _this = this;
            this.jq(ID_TABLE).find(".display-ranking-row").click(function(e) {
                _this.getDisplayManager().propagateEventRecordSelection(_this, _this.getPointData(), {
                    index: parseInt($(this).attr("what")) - 1
                });
            });
            this.jq("sortfields").change(function() {
                _this.setProperty("sortField", $(this).val());
                _this.updateUI();
            });
        },
    });
}



function RamaddaTsneDisplay(displayManager, id, properties) {
    var ID_BOTTOM = "bottom";
    var ID_CANVAS = "tsnecanvas";
    var ID_DETAILS = "tsnedetails";
    var ID_RUN = "tsnerun";
    var ID_RESET = "tsnereset";
    var ID_STEP = "tsnestep";
    var ID_SEARCH = "tsnesearch";
    $.extend(this, {
        colorTable: "red_white_blue",
        colorByMin: "-1",
        colorByMax: "1",
        height: "500px;"
    });

    let SUPER = new RamaddaFieldsDisplay(displayManager, id, DISPLAY_TSNE, properties);
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);

    RamaddaUtil.defineMembers(this, {
        nameToIndex: {},
        needsData: function() {
            return true;
        },
        handleEventPointDataLoaded: function(source, pointData) {
            if (!this.needsData()) {
                if (this.dataCollection == null) {
                    this.dataCollection = source.dataCollection;
                    this.updateUI();
                }
            }
        },
        updateUI: async function(pointData) {
            SUPER.updateUI.call(this, pointData);
            if (!this.hasData()) {
                this.setContents(this.getLoadingMessage());
                return;
            }
            await Utils.importJS(ramaddaBaseUrl + "/lib/tsne.js");
            //Height is the height of the overall display including the menu bar
            var height = this.getProperty("height");
            if (height.endsWith("px")) height = height.replace("px", "");
            height = parseInt(height);
            //            height-=30;
            var details = HtmlUtils.div(["style", "height:" + height + "px;max-height:" + height + "px", "class", "display-tnse-details", "id", this.getDomId(ID_DETAILS)], "");
            var canvas = HtmlUtils.div(["class", "display-tnse-canvas-outer", "style", "height:" + height + "px"], HtmlUtils.div(["class", "display-tnse-canvas", "id", this.getDomId(ID_CANVAS)], ""));
            var buttons = HtmlUtils.div(["id", this.getDomId(ID_RUN), "class", "ramadda-button", "what", "run"], "Stop") + "&nbsp;" +
                HtmlUtils.div(["id", this.getDomId(ID_STEP), "class", "ramadda-button", "what", "step"], "Step") + "&nbsp;" +
                HtmlUtils.div(["id", this.getDomId(ID_RESET), "class", "ramadda-button", "what", "reset"], "Reset") + "&nbsp;" +
                HtmlUtils.input("", "", ["id", this.getDomId(ID_SEARCH), "placeholder", "search"]);

            buttons = HtmlUtils.div(["class", "display-tnse-toolbar"], buttons);
            this.jq(ID_TOP_LEFT).append(buttons);
            this.setContents("<table  width=100%><tr valign=top><td width=80%>" + canvas + "</td><td width=20%>" + details + "</td></tr></table>");
            this.search = this.jq(ID_SEARCH);
            this.search.keyup(e => {
                var v = this.search.val().trim();
                this.canvas.find(".display-tnse-mark").removeClass("display-tnse-highlight");
                if (v == "") return;
                v = v.toLowerCase();
                for (name in this.nameToIndex) {
                    if (name.toLowerCase().startsWith(v)) {
                        this.jq("element-" + this.nameToIndex[name]).addClass("display-tnse-highlight");
                    }
                }
            });
            this.details = this.jq(ID_DETAILS);
            this.reset = this.jq(ID_RESET);
            this.step = this.jq(ID_STEP);
            this.step.button().click(() => {
                this.running = false;
                this.run.html(this.running ? "Stop" : "Run");
                this.takeStep();
            });
            this.reset.button().click(() => {
                this.start();
            });
            this.run = this.jq(ID_RUN);
            this.run.button().click(() => {
                this.running = !this.running;
                if (this.running) this.takeStep();
                this.run.html(this.running ? "Stop" : "Run");
            });
            this.canvas = this.jq(ID_CANVAS);
            this.running = true;
            this.start();
        },
        start: function() {
            this.canvas.html("");
            this.haveStepped = false;
            this.dataList = this.getStandardData(null, {
                includeIndex: false
            });
            var allFields = this.dataCollection.getList()[0].getRecordFields();
            if (!this.fields) {
                this.fields = this.getSelectedFields([]);
                if (this.fields.length == 0) this.fields = allFields;
                var strings = this.getFieldsOfType(this.fields, "string");
                if (strings.length > 0)
                    this.textField = strings[0];
            }
            var data = [];
            for (var rowIdx = 1; rowIdx < this.dataList.length; rowIdx++) {
                var tuple = this.getDataValues(this.dataList[rowIdx]);
                var nums = [];
                for (var i = 0; i < this.fields.length; i++) {
                    if (this.fields[i].isNumeric)
                        nums.push(tuple[this.fields[i].getIndex()]);
                }
                data.push(nums);
            }

            var opt = {}
            opt.epsilon = 10; // epsilon is learning rate
            opt.perplexity = 30; // how many neighbors each point influences
            opt.dim = 2; // dimensionality of the embedding (2 = default)
            this.tsne = new tsnejs.tSNE(opt);
            this.tsne.initDataRaw(data);
            this.takeStep();
        },
        takeStep: function() {
            var numSteps = 10;
            for (var step = 0; step < numSteps; step++) {
                this.tsne.step();
            }

            var pts = this.tsne.getSolution();
            var minx, miny, maxx, maxy;
            for (var i = 0; i < pts.length; i++) {
                if (i == 0) {
                    maxx = minx = pts[i][0];
                    maxy = miny = pts[i][1];
                } else {
                    maxx = Math.max(maxx, pts[i][0]);
                    minx = Math.min(minx, pts[i][0]);
                    maxy = Math.max(maxy, pts[i][1]);
                    miny = Math.min(miny, pts[i][1]);
                }
            }
            //            this.canvas.html("");

            var sleep = 250;
            for (var i = 0; i < pts.length; i++) {
                var x = pts[i][0];
                var y = pts[i][1];
                var px = 100 * (x - minx) / (maxx - minx);
                var py = 100 * (y - miny) / (maxy - miny);
                if (!this.haveStepped) {
                    var title = "";
                    if (this.textField) {
                        var tuple = this.getDataValues(this.dataList[i]);
                        title = tuple[this.textField.getIndex()];
                    }
                    if (title.length > 10) {
                        title.length = 10;
                    }
                    this.nameToIndex[title] = i;
                    this.canvas.append(HtmlUtils.div(["title", title, "index", i, "id", this.getDomId("element-" + i), "class", "display-tnse-mark", "style", "left:" + px + "%;" + "top:" + py + "%;"], title));
                } else {
                    this.jq("element-" + i).animate({
                        left: px + "%",
                        top: py + "%"
                    }, sleep, "linear");
                }

            }
            let _this = this;
            if (!this.haveStepped) {
                this.canvas.find(".display-tnse-mark").click(function(e) {
                    var index = parseInt($(this).attr("index"));
                    if (index < 0 || index >= _this.dataList.length) return;
                    var tuple = _this.getDataValues(_this.dataList[index]);
                    var details = "<table class=formtable width=100% >";
                    for (var i = 0; i < _this.fields.length; i++) {
                        var field = _this.fields[i];
                        details += "<tr><td align=right class=formlabel>" + field.getLabel() + ":</td><td>" + tuple[field.getIndex()] + "</td></tr>";
                    }
                    details += "</table>";
                    _this.details.html(details);
                });
            }
            if (!this.haveStepped) {
                //                this.haveStepped = true;
                //                this.takeStep();
                //                return;
            }
            this.haveStepped = true;
            if (this.running)
                setTimeout(() => this.takeStep(), sleep);
        },
    });
}


function RamaddaHeatmapDisplay(displayManager, id, properties) {
    $.extend(this, {
        colorTable: "red_white_blue",
    });
    let SUPER = new RamaddaFieldsDisplay(displayManager, id, DISPLAY_HEATMAP, properties);
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
        "map-display": false,
        needsData: function() {
            return true;
        },
        getMenuItems: function(menuItems) {
            SUPER.getMenuItems.call(this, menuItems);
            var get = this.getGet();
            var tmp = HtmlUtils.formTable();
            var colorTable = this.getColorTableName();
            var ct = "<select id=" + this.getDomId("colortable") + ">";
            for (table in Utils.ColorTable) {
                if (table == colorTable)
                    ct += "<option selected>" + table + "</option>";
                else
                    ct += "<option>" + table + "</option>";
            }
            ct += "</select>";

            tmp += HtmlUtils.formEntry("Color Table:", ct);

            tmp += HtmlUtils.formEntry("Color By Range:", HtmlUtils.input("", this.colorByMin, ["size", "7", ATTR_ID, this.getDomId("colorbymin")]) + " - " +
                HtmlUtils.input("", this.colorByMax, ["size", "7", ATTR_ID, this.getDomId("colorbymax")]));
            tmp += "</table>";
            menuItems.push(tmp);
        },
        initDialog: function() {
            SUPER.initDialog.call(this);
            var _this = this;
            var updateFunc = function() {
                _this.colorByMin = _this.jq("colorbymin").val();
                _this.colorByMax = _this.jq("colorbymax").val();
                _this.updateUI();

            };
            var func2 = function() {
                _this.colorTable = _this.jq("colortable").val();
                _this.updateUI();

            };
            this.jq("colorbymin").blur(updateFunc);
            this.jq("colorbymax").blur(updateFunc);
            this.jq("colortable").change(func2);
        },

        handleEventPointDataLoaded: function(source, pointData) {
            if (!this.needsData()) {
                if (this.dataCollection == null) {
                    this.dataCollection = source.dataCollection;
                    this.updateUI();
                }
            }
        },
        fieldSelectionChanged: function() {
            this.updateUI();
        },
        getContentsStyle: function() {
            var height = this.getProperty("height", -1);
            if (height > 0) {
                return " height:" + height + "px; " + " max-height:" + height + "px; overflow-y: auto;";
            }
            return "";
        },
        updateUI: function(pointData) {
            var _this = this;
            if (!haveGoogleChartsLoaded()) {
                var func = function() {
                    _this.updateUI();
                }
                this.setContents(this.getLoadingMessage());
                setTimeout(func, 1000);
                return;
            }

            SUPER.updateUI.call(this, pointData);
            if (!this.hasData()) {
                this.setContents(this.getLoadingMessage());
                return;
            }
            var dataList = this.getStandardData(null, {
                includeIndex: true
            });
            var header = this.getDataValues(dataList[0]);
            var showIndex = this.getProperty("showIndex", true);
            var showValue = this.getProperty("showValue", true);
            var textColor = this.getProperty("textColor", "black");

            var cellHeight = this.getProperty("cellHeight", null);
            var extraTdStyle = "";
            if (this.getProperty("showBorder", "false") == "true") {
                extraTdStyle = "border-bottom:1px #666 solid;";
            }
            var extraCellStyle = "";
            if (cellHeight)
                extraCellStyle += "height:" + cellHeight + "px; max-height:" + cellHeight + "px; min-height:" + cellHeight + "px;";
            var allFields = this.dataCollection.getList()[0].getRecordFields();
            var fields = this.getSelectedFields([]);

            if (fields.length == 0) fields = allFields;
            var html = "";
            var colors = null;
            var colorByMin = null;
            var colorByMax = null;
            if (Utils.stringDefined(this.getProperty("colorByMins"))) {
                colorByMin = [];
                var c = this.getProperty("colorByMins").split(",");
                for (var i = 0; i < c.length; i++) {
                    colorByMin.push(parseFloat(c[i]));
                }
            }
            if (Utils.stringDefined(this.getProperty("colorByMaxes"))) {
                colorByMax = [];
                var c = this.getProperty("colorByMaxes").split(",");
                for (var i = 0; i < c.length; i++) {
                    colorByMax.push(parseFloat(c[i]));
                }
            }

            if (Utils.stringDefined(this.getProperty("colorTables"))) {
                var c = this.getProperty("colorTables").split(",");
                colors = [];
                for (var i = 0; i < c.length; i++) {
                    var name = c[i];
                    if (name == "none") {
                        colors.push(null);
                        continue;
                    }
                    var ct = Utils.getColorTable(name, true);
                    //                        console.log("ct:" + name +" " +(ct!=null));
                    colors.push(ct);
                }
            } else {
                colors = [this.getColorTable(true)];
            }
            var mins = null;
            var maxs = null;
            for (var rowIdx = 1; rowIdx < dataList.length; rowIdx++) {
                var row = this.getDataValues(dataList[rowIdx]);
                if (mins == null) {
                    mins = [];
                    maxs = [];
                    for (var colIdx = 1; colIdx < row.length; colIdx++) {
                        mins.push(Number.MAX_VALUE);
                        maxs.push(Number.MIN_VALUE);
                    }
                }

                for (var colIdx = 0; colIdx < fields.length; colIdx++) {
                    var field = fields[colIdx];
                    //Add one to the field index to account for the main time index
                    var index = field.getIndex() + 1;
                    if (!field || !field.isFieldNumeric() || field.isFieldGeo()) continue;

                    var value = row[index];
                    if (value == Number.POSITIVE_INFINITY || isNaN(value) || !Utils.isNumber(value) || !Utils.isDefined(value) || value == null) {
                        continue;
                    }
                    mins[colIdx] = Math.min(mins[colIdx], value);
                    maxs[colIdx] = Math.max(maxs[colIdx], value);
                }
            }

            html += HtmlUtils.openTag("table", ["border", "0", "class", "display-heatmap"]);
            html += "<tr valign=bottom>";
            if (showIndex) {
                html += "<td align=center>" + HtmlUtils.tag("div", ["class", "display-heatmap-heading-top"], header[0]) + "</td>";
            }
            for (var fieldIdx = 0; fieldIdx < fields.length; fieldIdx++) {
                var field = fields[fieldIdx];
                if ((!field.isFieldNumeric() || field.isFieldGeo())) continue;
                html += "<td align=center>" + HtmlUtils.tag("div", ["class", "display-heatmap-heading-top"], field.getLabel()) + "</td>";
            }
            html += "</tr>\n";




            for (var rowIdx = 1; rowIdx < dataList.length; rowIdx++) {
                var row = this.getDataValues(dataList[rowIdx]);
                var index = row[0];
                //check if its a date
                if (index.f) {
                    index = index.f;
                }
                var rowLabel = index;
                html += "<tr valign='center'>\n";
                if (showIndex) {
                    html += HtmlUtils.td(["class", "display-heatmap-heading-side", "style", extraCellStyle], rowLabel);
                }
                var colCnt = 0;
                for (var colIdx = 0; colIdx < fields.length; colIdx++) {
                    var field = fields[colIdx];
                    //Add one to the field index to account for the main time index
                    var index = field.getIndex() + 1;
                    if (!field || !field.isFieldNumeric() || field.isFieldGeo()) continue;
                    var style = "";
                    var value = row[index];
                    var min = mins[colIdx];
                    var max = maxs[colIdx];
                    if (colorByMin && colCnt < colorByMin.length)
                        min = colorByMin[colCnt];
                    if (colorByMax && colCnt < colorByMax.length)
                        max = colorByMax[colCnt];


                    var ok = min != max && !(value == Number.POSITIVE_INFINITY || isNaN(value) || !Utils.isNumber(value) || !Utils.isDefined(value) || value == null);
                    var title = header[0] + ": " + rowLabel + " - " + field.getLabel() + ": " + value;
                    if (ok && colors != null) {
                        var ct = colors[Math.min(colCnt, colors.length - 1)];
                        if (ct) {
                            var percent = (value - min) / (max - min);
                            var ctIndex = parseInt(percent * ct.length);
                            if (ctIndex >= ct.length) ctIndex = ct.length - 1;
                            else if (ctIndex < 0) ctIndex = 0;
                            style = "background-color:" + ct[ctIndex] + ";";
                        }
                    }
                    var number;
                    if (!ok) {
                        number = "-";
                    } else {
                        number = Utils.formatNumber(value)
                    }
                    if (!showValue) number = "";
                    html += HtmlUtils.td(["valign", "center", "align", "right", "style", style + extraCellStyle + extraTdStyle, "class", "display-heatmap-cell"], HtmlUtils.div(["title", title, "style", extraCellStyle + "color:" + textColor], number));
                    colCnt++;
                }
                html += "</tr>";
            }
            html += "</table>";
            this.setContents(html);
            this.initTooltip();

        },
    });
}/**
Copyright 2008-2019 Geode Systems LLC
*/


//Note: I put all of the chart definitions together at the top so one can see everything that is available here
var DISPLAY_D3_GLIDER_CROSS_SECTION = "GliderCrossSection";
var DISPLAY_D3_PROFILE = "profile";
var DISPLAY_D3_LINECHART = "D3LineChart";
var DISPLAY_SKEWT = "skewt";
var DISPLAY_VENN = "venn";
var DISPLAY_CHERNOFF = "chernoff";

//Note: Added requiresData and category
addGlobalDisplayType({
    type: DISPLAY_D3_LINECHART,
    forUser: false,
    label: "D3 LineChart",
    requiresData: true,
    category: "Charts"
});
addGlobalDisplayType({
    type: DISPLAY_D3_PROFILE,
    forUser: false,
    label: "Profile",
    requiresData: true,
    category: "Charts"
});
addGlobalDisplayType({
    type: DISPLAY_D3_GLIDER_CROSS_SECTION,
    forUser: false,
    label: "Glider cross section",
    requiresData: true,
    category: "Charts"
});


addGlobalDisplayType({
    type: DISPLAY_VENN,
    forUser: true,
    label: "Venn Diagram",
    requiresData: true,
    category: "Misc"
});

addGlobalDisplayType({
    type: DISPLAY_CHERNOFF,
    forUser: true,
    label: "Chernoff Faces",
    requiresData: true,
    category: "Misc"
});

addGlobalDisplayType({
    type: DISPLAY_SKEWT,
    forUser: false,
    label: "SkewT",
    requiresData: true,
    category: "Charts"
});

//Note: define meaningful things as variables not as string literals
var FIELD_TIME = "time";
var FIELD_DEPTH = "depth";
var FIELD_VALUE = "value";
var FIELD_SELECTEDFIELD = "selectedfield";

var TYPE_LATITUDE = "latitude";
var TYPE_LONGITUDE = "longitude";
var TYPE_TIME = "time";
var TYPE_VALUE = "value";
var TYPE_ELEVATION = "elevation";


var FUNC_MOVINGAVERAGE = "movingAverage";

var D3Util = {
    foo: "bar",
    getAxis: function(axisType, range) {
        var axis;
        if (axisType == FIELD_TIME) {
            axis = d3.time.scale().range(range);
        } else {
            axis = d3.scale.linear().range(range);
        }
        return axis;
    },
    getDataValue: function(axis, record, index) {
        var data;
        if (axis.fieldIdx >= 0) {
            data = record.getData()[axis.fieldIdx];
        } else {
            switch (axis.type) {
                case TYPE_TIME:
                    data = new Date(record.getDate());
                    break;
                case TYPE_ELEVATION:
                    //console.log(record.getElevation());
                    data = record.getElevation();
                    break;
                case TYPE_LATITUDE:
                    data = record.getLatitude();
                case TYPE_LONGITUDE:
                    data = record.getLongitude();
                default:
                    data = record.getData()[index];
            }
        }


        if (axis.reverse == true) {
            return -1 * data;
        } else {
            return data;
        }
    },
    // This will be the default but we can add more colorscales
    getColorFromColorBar: function(value, range) {
        var colors = ["#00008B", "#0000CD", "#0000FF", "#00FFFF", "#7CFC00", "#FFFF00", "#FFA500", "#FF4500", "#FF0000", "#8B0000"];
        var colorScale = d3.scale.linear()
            .domain([0, colors.length - 1])
            .range(range);

        var colorScaler = d3.scale.linear()
            .range(colors)
            .domain(d3.range(0, colors.length).map(colorScale));

        color = colorScaler(value);
        return color;
    },
    // This is for the path lines the previous function for generic ones. 
    addColorBar: function(svg, colors, colorSpacing, displayWidth) {
        //Note: this originally had this.displayWidth which was undefined
        var colorBar = svg.append("g")
            .attr({
                "id": "colorBarG",
                "transform": "translate(" + (displayWidth - 40) + ",0)"
            });

        colorBar.append("g")
            .append("defs")
            .append("linearGradient")
            .attr({
                id: "colorBarGradient",
                x1: "0%",
                y1: "100%",
                x2: "0%",
                y2: "0%"
            })
            .selectAll("stop")
            .data(colors)
            .enter()
            .append("stop")
            .attr({
                "offset": function(d, i) {
                    return colorSpacing * (i) + "%"
                },
                "stop-color": function(d, i) {
                    return colors[i]
                },
                "stop-opacity": 1
            });

        return colorBar;
    }
}


function RamaddaSkewtDisplay(displayManager, id, properties) {
    let SUPER  = new RamaddaDisplay(displayManager, id, DISPLAY_SKEWT, properties);
    var ID_SKEWT = "skewt";
    var ID_DATE_LABEL = "skewtdate";
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);

    RamaddaUtil.defineMembers(this, {
        needsData: function() {
            return true;
        },
        initDisplay:  function() {
            SUPER.initDisplay.call(this);
        },
        handleEventMapClick: function(source, args) {
            if (!this.dataCollection) {
                return;
            }
            var pointData = this.dataCollection.getList();
            for (var i = 0; i < pointData.length; i++) {
                pointData[i].handleEventMapClick(this, source, args.lon, args.lat);
            }
        },
        handleEventPointDataLoaded: function(source, pointData) {
                //TODO: this results in a double  call to updateUI when first created
                this.updateUI();
        },
        updateUI: async function() {
         if(!this.loadedResources) {
             var time = new Date();
             await Utils.importCSS(ramaddaBaseUrl +"/htdocs_v_" + time.getTime()+"/lib/skewt/sounding.css");
           //            await Utils.importCSS(ramaddaBaseHtdocs+"/lib/skewt/sounding.css");
            //            await Utils.importJS(ramaddaBaseHtdocs +"/lib/skewt/d3skewt.js");
             await Utils.importJS(ramaddaBaseUrl +"/htdocs_v_" + time.getTime()+"/lib/skewt/d3skewt.js");
            this.loadedResources = true;
         }

         if(!window["D3Skewt"]) {
             setTimeout(()=>this.updateUI(),100);
             return;
         }
         SUPER.updateUI.call(this);

         var skewtId = this.getDomId(ID_SKEWT);
         var html = HtmlUtils.div(["id", skewtId], "");
         this.setContents(html);
         var pointData = this.getData();
         if (pointData == null) return;
         var records =  pointData.getRecords();
         if (!records || records.length==0) {
             console.log("no data yet");
             return;
         }
         var date = records[0].getDate();
         if(this.jq(ID_DATE_LABEL).length==0) {
             this.jq(ID_TOP_LEFT).append(HtmlUtils.div(["id",this.getDomId(ID_DATE_LABEL)]));
         }
         if(date!=null) {
             this.jq(ID_DATE_LABEL).html("Date: " + this.formatDate(date));
         } else {
             this.jq(ID_DATE_LABEL).html("");
         }
            var options = {};
            if (this.propertyDefined("showHodograph"))
                options.showHodograph = this.getProperty("showHodograph", true);
            if (this.propertyDefined("showText"))
                options.showText = this.getProperty("showText", true);
            if (this.propertyDefined("skewtWidth"))
                options.skewtWidth = parseInt(this.getProperty("skewtWidth"));
            if (this.propertyDefined("skewtHeight"))
                options.skewtHeight = parseInt(this.getProperty("skewtHeight"));
            if (this.propertyDefined("hodographWidth")){
                options.hodographWidth = parseInt(this.getProperty("hodographWidth"));
            }
            //            options.hodographWidth = 200;
            var fields = this.getData().getRecordFields();
            var names = [
                         {id:"pressure",aliases:["vertCoord"]},
                         {id:"height",aliases:["Geopotential_height_isobaric"]},
                         {id:"temperature",aliases:["Temperature_isobaric"]},
                         {id:"dewpoint",aliases:[]},
                         {id:"rh",aliases:["Relative_humidity_isobaric"]},
                         {id:"wind_direction",aliases:[]},
                         {id:"wind_speed",aliases:[]},
                         {id:"uwind",aliases:["u-component_of_wind_isobaric"]},
                         {id:"vwind",aliases:["v-component_of_wind_isobaric"]},
                         ];
            //TODO: check for units
            var data ={};
            var dataFields ={};
            for(var i=0;i<names.length;i++) {
                var obj = names[i];
                var id = obj.id;
                var field = this.getFieldById(fields,id);
                if(field == null) {
                    for(var j=0;j<obj.aliases.length;j++) {
                        field = this.getFieldById(fields,obj.aliases[j]);
                        if(field) break;
                    }
                }
                if(field) {
                    data[id] = this.getColumnValues(records, field).values;
                    dataFields[id]=field;
                }
            }

            if(!data.pressure) {
                this.displayError("No pressure defined in data");
                return;
            }

            if(!data.temperature) {
                this.displayError("No temperature defined in data");
                return;
            }

            if(!data.height) {
                var pressures = [
                    1013.25, 954.61, 898.76, 845.59, 795.01, 746.91, 701.21,
                    657.80, 616.6, 577.52, 540.48, 505.39, 472.17, 440.75,
                    411.05, 382.99, 356.51, 331.54, 303.00, 285.85, 264.99,
                    226.99, 193.99, 165.79, 141.70, 121.11, 103.52, 88.497,
                    75.652, 64.674, 55.293, 25.492, 11.970, 5.746, 2.871,
                    1.491, 0.798, 0.220, 0.052, 0.010,];
                var alts = [
                            0, 500, 1000, 1500, 2000, 2500, 3000, 3500, 4000, 4500, 5000,
                            5500, 6000, 6500, 7000, 7500, 8000, 8500, 9000, 9500, 10000,
                            11000, 12000, 13000, 14000, 15000, 16000, 17000, 18000, 19000,
                            20000, 25000, 30000, 35000, 40000, 45000, 50000, 60000, 70000,
                            80000,
                            ];
                
                data.height = [];
                for(var i=0;i<data.pressure.length;i++) {
                    var pressure = data.pressure[i];
                    var alt = alts[alts.length-1];
                    for(var j=0;j<pressures.length;j++) {
                        if(pressure>=pressures[j]) {
                            if(j==0) alt = 0;
                            else {
                                var p1 = pressures[j-1];
                                var p2 = pressures[j];
                                var a1 = alts[j-1];
                                var a2 = alts[j];
                                var percent = 1-(pressure-p2)/(p1-p2);
                                alt = (a2-a1)*percent+a1;
                            }
                            break;
                        }
                    }
                    data.height.push(alt);
                }
            }

            if(!data.dewpoint) {
                if(!data.rh) {
                    this.displayError("No dewpoint or rh");
                    return;
                }
                data.dewpoint = [];
                for(var i=0;i<data.rh.length;i++) {
                    var rh=data.rh[i];
                    var t=data.temperature[i];
                    var dp = t-(100-rh)/5;
                    data.dewpoint.push(dp);
                }
            }


            if(!data.wind_speed) {
                if(!data.uwind || !data.vwind) {
                    this.displayError("No wind speed defined in data");
                    return;
                }
                data.wind_speed = [];
                data.wind_direction = [];
                for(var i=0;i<data.uwind.length;i++) {
                    var u = data.uwind[i];
                    var v = data.vwind[i];
                    var ws = Math.sqrt(u*u+v*v);
                    var wdir = 180+(180/Math.PI)*Math.atan2(v,u);
                    data.wind_speed.push(ws);
                    data.wind_direction.push(wdir);
                }
            }


            if(data.height.length>1) {
                if(data.height[0]>data.height[1]) {
                    for(name in data)
                        data[name] = Utils.reverseArray(data[name]);
                }
            }
            options.myid = this.getId();
            try {
                this.skewt = new D3Skewt(skewtId, options,data);
            } catch(e) {
                this.displayError("An error occurred: " +e);
                console.log("error:" + e.stack);
                return;
            }
            await this.getDisplayEntry((e)=>{
                    var q= e.getAttribute("variables");
                    if(!q) return;
                    q = q.value;
                    q = q.replace(/\r\n/g,"\n");
                    q = q.replace(/^ *\n/,"");
                    q = q.replace(/^ *([^:]+):([^\n].*)$/gm,"<div title='$1' class=display-skewt-index-label>$1</div>: <div title='$2'  class=display-skewt-index>$2</div>");
                    q = q.replace(/[[\r\n]/g,"\n");
                    q = HtmlUtils.div(["class", "display-skewt-text"],q);
                    $("#" + this.skewt.textBoxId).html(q);
                });
        }
    });
}


function RamaddaD3Display(displayManager, id, properties) {
    // To get it to the console
    testProperties = properties;

    var ID_SVG = "svg";
    var SUPER;
    RamaddaUtil.inherit(this, SUPER = new RamaddaDisplay(displayManager, id, "d3", properties));
    addRamaddaDisplay(this);

    RamaddaUtil.defineMembers(this, {
        initDisplay: function() {
            this.createUI();
            this.setDisplayTitle(properties.graph.title);

            //Note: use innerHeight/innerWidth wiki attributes
            var width = this.getProperty("innerWidth", 600);
            var height = this.getProperty("innerHeight", 300);
            var margin = {
                top: 20,
                right: 50,
                bottom: 30,
                left: 50
            };
            var divStyle =
                "height:" + height + "px;" +
                "width:" + width + "px;";
            var html = HtmlUtils.div([ATTR_ID, this.getDomId(ID_SVG), ATTR_STYLE, divStyle], "");
            this.setContents(html);

            // To create dynamic size of the div
            this.displayHeight = parseInt((d3.select("#" + this.getDomId(ID_SVG)).style("height")).split("px")[0]) - margin.top - margin.bottom; //this.getProperty("height",300);//d3.select("#"+this.getDomId(ID_SVG)).style("height");//
            this.displayWidth = parseInt((d3.select("#" + this.getDomId(ID_SVG)).style("width")).split("px")[0]) - margin.left - margin.right; //this.getProperty("width",600);//d3.select("#"+this.getDomId(ID_SVG)).style("width");//

            //                console.log("WxH:" + this.displayHeight +" " + this.displayWidth);

            // To solve the problem with the classess within the class
            var myThis = this;
            var zoom = d3.behavior.zoom()
                .on("zoom", function() {
                    myThis.zoomBehaviour()
                });
            this.zoom = zoom;
            this.svg = d3.select("#" + this.getDomId(ID_SVG)).append("svg")
                .attr("width", this.displayWidth + margin.left + margin.right)
                .attr("height", this.displayHeight + margin.top + margin.bottom)
                .attr(ATTR_CLASS, "D3graph")
                .call(zoom)
                .on("click", function() {
                    myThis.click(d3.event)
                })
                .on("dblclick", function() {
                    myThis.dbclick(d3.event)
                })
                .append("g")
                .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

            // Define the Axis
            // 100 pixels for the legend... lets see if we keep it
            this.x = D3Util.getAxis(properties.graph.axis.x.type, [0, this.displayWidth - 100]);
            this.y = D3Util.getAxis(properties.graph.axis.y.type, [this.displayHeight, 0]);

            this.xAxis = d3.svg.axis()
                .scale(this.x)
                .orient("bottom");

            this.yAxis = d3.svg.axis()
                .scale(this.y)
                .orient("left");

            // Add Axis to the plot
            this.svg.append("g")
                .attr(ATTR_CLASS, "x axis")
                .attr("transform", "translate(0," + this.displayHeight + ")")
                .call(this.xAxis);

            this.svg.append("g")
                .attr(ATTR_CLASS, "y axis")
                .call(this.yAxis);


            // Color Bar
            var colors = ["#00008B", "#0000CD", "#0000FF", "#00FFFF", "#7CFC00", "#FFFF00", "#FFA500", "#FF4500", "#FF0000", "#8B0000"];

            var colorSpacing = 100 / (colors.length - 1);

            var colorBar = D3Util.addColorBar(this.svg, colors, colorSpacing, this.displayWidth);
            this.color = d3.scale.category10();
            this.updateUI();
        },
        needsData: function() {
            return true;
        },
        initDialog: function() {
            this.addFieldsCheckboxes();
        },
        getDialogContents: function() {
            var height = this.getProperty(PROP_HEIGHT, "400");
            var html = HtmlUtils.div([ATTR_ID, this.getDomId(ID_FIELDS), ATTR_CLASS, "display-fields", ]);
            html += SUPER.getDialogContents.apply(this);
            return html;
        },
        fieldSelectionChanged: function() {
            this.updateUI();
        },
        // onlyZoom is not updating the axis
        updateUI: function() {
            //Note: Not sure why onlyZoom was a function param. The pointData gets passes in 
            //when the json is loaded
            //            updateUI: function(onlyZoom) {
            var onlyZoom = false;

            //Note: if we write to the SVG dom element then we lose the svg object that got created in initDisplay
            //Not sure how to show a message to the user
            if (!this.hasData()) {
                //this.writeHtml(ID_SVG, HtmlUtils.div([ATTR_CLASS,"display-message"], this.getLoadingMessage()));
                return;
            }
            test = this;
            var selectedFields = this.getSelectedFields();
            if (selectedFields.length == 0) {
                //this.writeHtml(ID_SVG, "No fields selected");
                return;
            }
            this.addFieldsCheckboxes();
            pointData = this.getData();
            if (pointData == null) {
                //this.writeHtml(ID_SVG, "No data");
                console.log("no data");
                return;
            }

            var fields = pointData.getNumericFields();
            var records = pointData.getRecords();
            var ranges = RecordUtil.getRanges(fields, records);
            var elevationRange = RecordUtil.getElevationRange(fields, records);
            var offset = (elevationRange[1] - elevationRange[0]) * 0.05;
            // To be used inside a function we can use this.x inside them so we extract as variables. 
            var x = this.x;
            var y = this.y;
            var color = this.color;
            var axis = properties.graph.axis;

            if (onlyZoom) {
                this.zoom.x(this.x);
                this.zoom.y(this.y);
            } else {
                // Update axis for the zoom and other changes
                this.x.domain(d3.extent(records, function(d) {
                    return D3Util.getDataValue(axis.x, d, selectedFields[0].getIndex());
                }));
                // the y domain depends on the first selected element I have to think about it.
                this.y.domain(d3.extent(records, function(d) {
                    return D3Util.getDataValue(axis.y, d, selectedFields[0].getIndex());
                }));

                this.zoom.x(this.x);
                this.zoom.y(this.y);
            }

            this.svg.selectAll(".y.axis").call(this.yAxis);
            this.svg.selectAll(".x.axis").call(this.xAxis);

            // Remove previous lines
            this.svg.selectAll(".line").remove();
            this.svg.selectAll(".legendElement").remove();

            var myThis = this;
            for (var fieldIdx = 0; fieldIdx < selectedFields.length; fieldIdx++) {
                var dataIndex = selectedFields[fieldIdx].getIndex();
                var range = ranges[dataIndex];
                // Plot line for the values
                var line = d3.svg.line()
                    .x(function(d) {
                        return x(D3Util.getDataValue(axis.x, d, selectedFields[fieldIdx].getIndex()));
                    })
                    .y(function(d) {
                        return y(D3Util.getDataValue(axis.y, d, selectedFields[fieldIdx].getIndex()));
                    });

                displayLine = this.svg.append("path")
                    .datum(records)
                    .attr(ATTR_CLASS, "line")
                    .attr("d", line)
                    .on("mousemove", function() {
                        myThis.mouseover(d3.event)
                    })
                    .attr("fill", "none")
                    .attr("stroke", function(d) {
                        return color(fieldIdx);
                    })
                    .attr("stroke-width", "0.5px");

                if (properties.graph.axis.z == FIELD_SELECTEDFIELD) {
                    displayLine.attr("stroke", "url(#colorBarGradient)");
                }

                if (properties.graph.derived != null) {
                    var funcs = properties.graph.derived.split(",");
                    for (funcIdx = 0; funcIdx < funcs.length; funcIdx++) {
                        var func = funcs[funcIdx];
                        if (func == FUNC_MOVINGAVERAGE) {
                            // Plot moving average Line
                            var movingAverageLine = d3.svg.line()
                                .x(function(d) {
                                    return x(D3Util.getDataValue(axis.x, d, selectedFields[fieldIdx].getIndex()));
                                })
                                .y(function(d, i) {
                                    if (i == 0) {
                                        return _movingSum = D3Util.getDataValue(axis.y, d, selectedFields[fieldIdx].getIndex());
                                    } else {
                                        _movingSum += D3Util.getDataValue(axis.y, d, selectedFields[fieldIdx].getIndex());
                                    }
                                    return y(_movingSum / i);
                                })
                                .interpolate("basis");
                            this.svg.append("path")
                                .attr(ATTR_CLASS, "line")
                                .attr("d", movingAverageLine(records))
                                .attr("fill", "none")
                                .attr("stroke", function(d) {
                                    return color(fieldIdx);
                                })
                                .attr("stroke-width", "1.5px")
                                .attr("viewBox", "50 50 100 100 ")
                                .style("stroke-dasharray", ("3, 3"));
                        } else {
                            console.log("Error: Unknown derived function:" + func);
                        }

                    }
                }

                // Legend element Maybe create a function or see how we implement the legend
                this.svg.append("svg:rect")
                    .attr(ATTR_CLASS, "legendElement")
                    .attr("x", this.displayWidth - 100)
                    .attr("y", (50 + 50 * fieldIdx))
                    .attr("stroke", function(d) {
                        return color(fieldIdx);
                    })
                    .attr("height", 2)
                    .attr("width", 40);

                this.svg.append("svg:text")
                    .attr(ATTR_CLASS, "legendElement")
                    .attr("x", this.displayWidth - 100 + 40 + 10) // position+color rect+padding
                    .attr("y", (55 + 55 * fieldIdx))
                    .attr("stroke", function(d) {
                        return color(fieldIdx);
                    })
                    .attr("style", "font-size:50%")
                    .text(selectedFields[fieldIdx].getLabel());
            }
        },

        zoomBehaviour: function() {
            // Call redraw with only zoom don't update extent of the data.
            this.updateUI(true);
        },
        //this gets called when an event source has selected a record
        handleEventRecordSelection: function(source, args) {},
        mouseover: function() {
            // TO DO
            testing = d3.event;
            console.log("mouseover");
        },
        click: function(event) {
            // TO DO
            console.log("click:" + event);
        },
        dbclick: function(event) {
            // Unzoom
            this.zoom();
            this.updateUI();
        },
        getSVG: function() {
            return this.svg;
        }
    });
}


function RamaddaD3LineChartDisplay(displayManager, id, properties) {
    var dfltProperties = {};
    //Note: use json structures to define the props
    dfltProperties.graph = {
        title: "Line chart",
        //Note: changed this to "derived" from "extraLine".
        //This is a comma separated list of functions (for now just one)
        derived: FUNC_MOVINGAVERAGE,
        axis: {
            y: {
                type: TYPE_VALUE,
                fieldname: FIELD_SELECTEDFIELD
            },
            x: {
                type: TYPE_TIME,
                fieldname: FIELD_TIME,
            }
        }
    };

    properties = $.extend(dfltProperties, properties);
    return new RamaddaD3Display(displayManager, id, properties);
}


function RamaddaProfileDisplay(displayManager, id, properties) {
    var dfltProperties = {};
    //Note: use json structures to define the props
    dfltProperties.graph = {
        title: "Profile chart",
        derived: null,
        axis: {
            y: {
                type: TYPE_ELEVATION,
                fieldname: FIELD_DEPTH,
                fieldIdx: 3,
                reverse: true
            },
            x: {
                type: TYPE_VALUE,
                fieldname: FIELD_VALUE,
            },
        }
    };
    //Note: now set the properties
    properties = $.extend(dfltProperties, properties);
    return new RamaddaD3Display(displayManager, id, properties);
}




function RamaddaGliderCrossSectionDisplay(displayManager, id, properties) {
    var dfltProperties = {};
    //Note: use json structures to define the props
    dfltProperties.graph = {
        title: "Glider cross section",
        derived: null,
        axis: {
            y: {
                type: TYPE_ELEVATION,
                fieldname: FIELD_DEPTH,
                reverse: true
            },
            x: {
                type: TYPE_TIME,
                fieldname: FIELD_TIME,
            },
            z: FIELD_SELECTEDFIELD,
        }
    };
    properties = $.extend(dfltProperties, properties);

    return new RamaddaD3Display(displayManager, id, properties);
}






function RamaddaVennDisplay(displayManager, id, properties) {
    var ID_VENN = "venn";
    let SUPER = new RamaddaFieldsDisplay(displayManager, id, DISPLAY_VENN, properties);
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);
    $.extend(this, {
        getContentsStyle: function() {
            return "";
        },
        checkLayout: function() {
            this.updateUIInner();
        },
        updateUI: function() {
            var includes = "<script src='" + ramaddaBaseUrl + "/lib/venn.js'></script>";
            this.writeHtml(ID_DISPLAY_TOP, includes);
            let _this = this;
            var func = function() {
                _this.updateUIInner();
            };
            setTimeout(func, 10);
        },
        updateUIInner: function() {
            let records = this.filterData();
            if (!records) {
                return;
            }
            var allFields = this.getData().getRecordFields();
            var fields = this.getSelectedFields(allFields);
            if (fields.length == 0)
                fields = allFields;
            var strings = this.getFieldsOfType(fields, "string");
            if (strings.length == 0) {
                this.displayError("No string fields specified");
                return;
            }
            /*
              var sets = [{sets : [0], label : 'SE', size : 28,}, 
              {sets : [1], label : 'Treat', size: 35},
              {sets : [2], label : 'Anti-CCP', size : 108}, 
              {sets : [3], label : 'DAS28', size:106},
              {sets : [0,1], size:1},
              {sets : [0,2], size:1},
              {sets : [0,3], size:14},
            */
            var setInfos = {};
            var setCnt = 0;
            for (var rowIdx = 0; rowIdx < records.length; rowIdx++) {
                var row = this.getDataValues(records[rowIdx]);
                var keys = [];
                var key = "";
                for (var fieldIdx = 0; fieldIdx < strings.length; fieldIdx++) {
                    var field = strings[fieldIdx];
                    var value = row[field.getIndex()];
                    var setKey = field.getId() + "--" + value;
                    keys.push(setKey);
                    key += "--" + setKey;
                    if (!Utils.isDefined(setInfos[setKey])) {
                        setInfos[setKey] = {
                            count: 0,
                            setIds: [setCnt],
                            label: value,
                        };
                        setCnt++;
                    }
                    setInfos[setKey].count++;
                }
                var ids = [];
                if (!Utils.isDefined(setInfos[key])) {
                    for (var i = 0; i < keys.length; i++) {
                        ids.push(setInfos[keys[i]].setIds[0]);
                    }
                    setInfos[key] = {
                        count: 0,
                        setIds: ids,
                        label: null,
                    };
                }
                setInfos[key].count++;
            }

            var sets = [];
            for (var a in setInfos) {
                var setInfo = setInfos[a];
                var obj = {
                    sets: setInfo.setIds,
                    size: setInfo.count
                };
                if (setInfo.label)
                    obj.label = setInfo.label;
                sets.push(obj);
            }
            this.writeHtml(ID_DISPLAY_CONTENTS, HtmlUtils.div(["id", this.getDomId(ID_VENN), "style", "height:300px;"], ""));
            var chart = venn.VennDiagram()
                .width(600)
                .height(400);
            var id = "#" + this.getDomId(ID_VENN);
            var strokeColors = this.getColorTable(true, "strokeColors", "nice");
            var fillColors = this.getColorTable(true, "fillColors", "nice");
            var textColors = this.getColorTable(true, "textColors");
            if (!textColors)
                textColors = strokeColors;
            d3.select(id).datum(sets).call(chart);
            d3.selectAll(id + " .venn-circle path")
                .style("fill-opacity", parseFloat(this.getProperty("fillOpacity", 0.5)))
                .style("stroke-width", parseInt(this.getProperty("strokeWidth", 1)))
                .style("stroke-opacity", parseFloat(this.getProperty("strokeOpacity", 0.5)))
                .style("stroke", function(d, i) {
                    return i < strokeColors.length ? strokeColors[i] : strokeColors[i % strokeColors.length];
                })
                .style("fill", function(d, i) {
                    return i < fillColors.length ? fillColors[i] : fillColors[i % fillColors.length];
                })
            d3.selectAll(id + " .venn-circle text")
                .style("fill", function(d, i) {
                    return i < textColors.length ? textColors[i] : textColors[i % textColors.length];
                })
                .style("font-size", this.getProperty("fontSize", "16px"))
                .style("font-weight", this.getProperty("fontWeight", "100"));

        }
    });
}



function RamaddaChernoffDisplay(displayManager, id, properties) {
    var ID_VENN = "venn";
    let SUPER = new RamaddaFieldsDisplay(displayManager, id, DISPLAY_VENN, properties);
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);
    $.extend(this, {
        getContentsStyle: function() {
            return "";
        },
        checkLayout: function() {
            this.updateUIInner();
        },
        timeout: 100,
        written: false,
        updateUI: function() {
            if (!this.written) {
                this.written = true;
                var includes = "<script src='" + ramaddaBaseUrl + "/lib/chernoff.js'></script>";
                this.writeHtml(ID_DISPLAY_TOP, includes);
            }
            this.updateUIInner();
        },
        updateUIInner: function() {
            let _this = this;
            if (!Utils.isDefined(d3.chernoff)) {
                console.log("not there");
                this.timeout = this.timeout * 2;
                var func = function() {
                    _this.updateUIInner();
                };
                setTimeout(func, this.timeout);
                return;
            }
            let records = this.filterData();
            if (!records) {
                return;
            }
            var allFields = this.getData().getRecordFields();
            var fields = this.getSelectedFields(allFields);
            var numericFields = this.getFieldsOfType(fields, "numeric");
            if (numericFields.length == 0) {
                this.displayError("No numeric fields specified");
                return;
            }
            if (fields.length == 0)
                fields = allFields;
            var string = this.getFieldOfType(fields, "string");
            var legend = "";
            var colorField = this.getFieldById(allFields, this.getProperty("colorBy"));
            var colorscale;
            if (colorField) {
                var colors = this.getColorTable(true, null, "blue_white_red");
                var colorValues = this.getColumnValues(records, colorField);
                colorscale = [];
                var min = parseFloat(this.getProperty("colorByMin", colorValues.min));
                var max = parseFloat(this.getProperty("colorByMax", colorValues.max));
                var range = max - min;
                //                console.log("range:" +  max +" " +min +" #colors:" + colors.length);
                for (var i = 0; i < colorValues.values.length; i++) {
                    var value = colorValues.values[i];
                    var percent = (value - min) / range;
                    var idx = Math.round(percent * (colors.length - 1));
                    //                    console.log(idx+" " +colors[idx] +" " + value + " " + percent);
                    colorscale.push(colors[idx]);
                }
                this.displayColorTable(colors, ID_DISPLAY_BOTTOM, min, max);
                legend += "<b>Colored by</b>: " + colorField.getLabel() + "&nbsp;&nbsp;";
            }
            var attrs = [{
                label: "Face width",
                name: "face",
                key: "f",
                min: 0,
                max: 1
            }, {
                label: "Hair",
                name: "hair",
                key: "h",
                min: -1,
                max: 1
            }, {
                label: "Mouth",
                name: "mouth",
                key: "m",
                min: -1,
                max: 1
            }, {
                label: "Nose height",
                name: "noseHeight",
                key: "nh",
                min: 0,
                max: 1
            }, {
                label: "Nose width",
                name: "noseWidth",
                key: "nw",
                min: 0,
                max: 1
            }, {
                label: "Eyes height",
                name: "eyesHeight",
                key: "eh",
                min: 0,
                max: 1
            }, {
                label: "Eyes width",
                name: "eyesWidth",
                key: "ew",
                min: 0,
                max: 1
            }, {
                label: "Brow",
                name: "brow",
                key: "b",
                min: -1,
                max: 1
            }];
            var html = "";
            var showHelp = this.getProperty("showHelp", false);
            if (showHelp) {
                html += "Settings:<br><table class=ramadda-table><thead><tr><th>Attribute&nbsp;</th><th>&nbsp;Default range&nbsp;</th><th>&nbsp;Set field&nbsp;</th><th>&nbsp;Set min&nbsp;</th><th>&nbsp;Set max&nbsp;</th></tr></thead><tbody>";
            }

            for (a in attrs) {
                var attr = attrs[a];
                if (showHelp) {
                    html += "<tr><td>" + attr.label + "</td><td align=center>" + attr.min + " - " + attr.max + "</td><td>" + attr.name + "Field=&lt;field_id&gt;" + "</td><td>" + attr.name + "Min=&lt;min_value&gt;" + "</td><td>" + attr.name + "Max=&lt;max_value&gt;" + "</td></tr>";
                }
                attr.field = this.getFieldById(allFields, this.getProperty(attr.name + "Field"));
                if (attr.field) {
                    legend += "<b>" + attr.label + "</b>: " + attr.field.getLabel() + "&nbsp;&nbsp;";
                    if (Utils.isDefined(this.getProperty(attr.name + "Min"))) {
                        attr.min = parseFloat(this.getProperty(attr.name + "Min"));
                    }
                    if (Utils.isDefined(this.getProperty(attr.name + "Max"))) {
                        attr.max = parseFloat(this.getProperty(attr.name + "Max"));
                    }
                    attr.column = this.getColumnValues(records, attr.field);
                }
            }
            if (showHelp) {
                html += "</tbody></table>";
            }

            var sortField = this.getFieldById(allFields, this.getProperty("sortBy"));

            var rows = [];
            for (var rowIdx = 0; rowIdx < records.length; rowIdx++) {
                var blob = {
                    values: this.getDataValues(records[rowIdx])
                };
                if (colorscale) blob.color = colorscale[rowIdx]
                rows.push(blob);
            }

            if (sortField) {
                rows.sort(function(a, b) {
                    var v1 = a.values[sortField.getIndex()];
                    var v2 = b.values[sortField.getIndex()];
                    if (v1 < v2) return 1;
                    if (v1 > v2) return -1;
                    return 0;
                });
            }

            var data = [];
            for (var rowIdx = 0; rowIdx < rows.length; rowIdx++) {
                var blob = rows[rowIdx];
                var row = blob.values;
                var color = blob.color;
                var faceData = {
                    f: 0.5, //0-1
                    h: 0, //-1-1
                    m: 0, //-1-1
                    nh: 0.5, //0-1
                    nw: 0.5, //0-1
                    eh: 0.5, //0-1
                    ew: 0.5, //0-1
                    b: 0 //-1-1
                };
                data.push({
                    faceData: faceData,
                    color: color
                });
                var tt = "";
                for (a in attrs) {
                    var attr = attrs[a];
                    var field = attr.field;
                    if (!field) {
                        faceData[attr.key] = attr.min + (attr.max - attr.min) / 2;
                    } else {
                        var value = row[field.getIndex()];
                        var min = attr.column.min;
                        var max = attr.column.max;
                        tt += field.getLabel() + ": " + value + " range: " + min + " - " + max + " (" + attr.label + ")\n";
                        if (max != min) {
                            var percent = (value - min) / (max - min);
                            var adjValue = attr.min + (attr.max - attr.min) * percent;
                            //                            console.log(" %:" + percent + " v:" + value +" min:" + min +" max:" + max +" adj:" + adjValue);
                            faceData[attr.key] = adjValue;
                        }
                    }
                }
                var label = (string ? row[string.getIndex()] : "Row: " + rowIdx);
                var labelValue = (string ? row[string.getIndex()] : "");
                label = HtmlUtils.div(["class", "display-chernoff-label"], label);
                var div = HtmlUtils.div(["id", this.getDomId("chernoff") + "_" + rowIdx, "class", "display-chernoff-face"], "");
                html += HtmlUtils.div(["title", tt, "class", "display-chernoff-wrapper ramadda-div-link", "value", labelValue], div + label);
            }
            legend = HtmlUtils.div(["class", "display-chernoff-legend"], legend);
            var height = this.getProperty("height", "400px");
            if (!height.endsWith("px")) height += "px";
            this.writeHtml(ID_DISPLAY_CONTENTS, legend + HtmlUtils.div(["style", "height:" + height + ";", "class", "display-chernoff-container", "id", this.getDomId("chernoff")], html));
            for (var rowIdx = 0; rowIdx < records.length; rowIdx++) {
                var div = "#" + this.getDomId("chernoff") + "_" + rowIdx;
                this.makeFace(div, data[rowIdx].faceData, data[rowIdx].color);
            }

            if (string) {
                $("#" + this.getDomId(ID_DISPLAY_CONTENTS)).find(".ramadda-div-link").click(function() {
                    var value = $(this).attr("value");
                    _this.propagateEvent("handleEventFieldValueSelect", {
                        field: string,
                        value: value
                    });
                });
            }
        },
        makeFace: function(div, faceData, color) {
            function chernoffFace() {
                var width = 400,
                    height = 200;
                var chernoff = d3.chernoff()
                    .face(function(d) {
                        return d.f;
                    })
                    .hair(function(d) {
                        return d.h;
                    })
                    .mouth(function(d) {
                        return d.m;
                    })
                    .nosew(function(d) {
                        return d.nw;
                    })
                    .noseh(function(d) {
                        return d.nh;
                    })
                    .eyew(function(d) {
                        return d.ew;
                    })
                    .eyeh(function(d) {
                        return d.eh;
                    })
                    .brow(function(d) {
                        return d.b;
                    });

                function data() {
                    return [faceData];
                }

                function drawFace(selection) {
                    var svg = selection.append("svg")
                        .attr("width", width)
                        .attr("height", height);
                    var face = svg.selectAll("g.chernoff")
                        .data(data())
                        .enter().append("g")
                        .attr("class", "chernoff")
                        .call(chernoff);
                    if (color)
                        face.attr("style", "fill:" + color);
                }

                function draw(selection) {
                    selection.call(drawFace);
                }
                return draw;
            }
            d3.select(div)
                .call(chernoffFace());
        }





    });
}/**
Copyright 2008-2019 Geode Systems LLC
*/


var DISPLAY_WORDCLOUD = "wordcloud";
var DISPLAY_TEXTSTATS = "textstats";
var DISPLAY_TEXTANALYSIS = "textanalysis";
var DISPLAY_TEXTRAW = "textraw";
var DISPLAY_TEXT = "text";
var DISPLAY_IMAGES = "images";


addGlobalDisplayType({
    type: DISPLAY_TEXT,
    label: "Text Readout",
    requiresData: false,
    forUser: true,
    category: CATEGORY_MISC
});

addGlobalDisplayType({
    type: DISPLAY_IMAGES,
    label: "Images",
    requiresData: true,
    forUser: true,
    category: CATEGORY_MISC
});


addGlobalDisplayType({
    type: DISPLAY_WORDCLOUD,
    forUser: true,
    label: "Word Cloud",
    requiresData: true,
    category: "Text"
});

addGlobalDisplayType({
    type: DISPLAY_TEXTSTATS,
    forUser: true,
    label: "Text Stats",
    requiresData: true,
    category: "Text"
});
addGlobalDisplayType({
    type: DISPLAY_TEXTRAW,
    forUser: true,
    label: "Text Raw",
    requiresData: true,
    category: "Text"
});
addGlobalDisplayType({
    type: DISPLAY_TEXTANALYSIS,
    forUser: true,
    label: "Text Analysis",
    requiresData: true,
    category: "Text"
});



function RamaddaBaseTextDisplay(displayManager, id, type, properties) {
    var ID_TEXTBLOCK = "textblock";
    let SUPER = new RamaddaFieldsDisplay(displayManager, id, type, properties);
    RamaddaUtil.inherit(this, SUPER);
    $.extend(this, {
        processText: function(cnt) {
            let records = this.filterData();
            if (!records) {
                return null;
            }
            var allFields = this.getData().getRecordFields();
            var fields = this.getSelectedFields(allFields);
            if (fields.length == 0)
                fields = allFields;
            var strings = this.getFieldsOfType(fields, "string");
            if (strings.length == 0) {
                this.displayError("No string fields specified");
                return null;
            }
            var fieldInfo = {};
            var minLength = parseInt(this.getProperty("minLength", 0));
            var maxLength = parseInt(this.getProperty("maxLength", 100));
            var stopWords = this.getProperty("stopWords");
            if (stopWords) {
                if (stopWords == "default") {
                    stopWords = Utils.stopWords;
                } else {
                    stopWords = stopWords.split(",");
                }
            }
            var extraStopWords = this.getProperty("extraStopWords");
            if (extraStopWords) extraStopWords = extraStopWords.split(",");

            var stripTags = this.getProperty("stripTags", false);
            var tokenize = this.getProperty("tokenize", false);
            var lowerCase = this.getProperty("lowerCase", false);
            var removeArticles = this.getProperty("removeArticles", false);
            if (cnt) {
                cnt.count = 0;
                cnt.total = 0;
                cnt.lengths = {};
            }
            for (var rowIdx = 0; rowIdx < records.length; rowIdx++) {
                var row = this.getDataValues(records[rowIdx]);
                for (var fieldIdx = 0; fieldIdx < strings.length; fieldIdx++) {
                    var field = strings[fieldIdx];
                    if (!fieldInfo[field.getId()]) {
                        fieldInfo[field.getId()] = {
                            field: field,
                            words: [],
                            counts: {},
                            divId: this.getDomId(ID_TEXTBLOCK + (field.getIndex())),
                        }
                    }
                    var fi = fieldInfo[field.getId()];
                    var value = row[field.getIndex()];
                    if (stripTags) {
                        value = Utils.stripTags(value);
                    }
                    var values = [value];
                    if (tokenize) {
                        values = Utils.tokenizeWords(values[0], stopWords, extraStopWords, removeArticles);
                    }
                    for (var valueIdx = 0; valueIdx < values.length; valueIdx++) {
                        var value = values[valueIdx];
                        var _value = value.toLowerCase();
                        if (cnt) {
                            cnt.count++;
                            cnt.total += value.length;
                            if (!Utils.isDefined(cnt.lengths[value.length]))
                                cnt.lengths[value.length] = 0;
                            cnt.lengths[value.length]++;
                        }


                        if (!tokenize) {
                            if (stopWords && stopWords.includes(_value)) continue;
                            if (extraStopWords && extraStopWords.includes(_value)) continue;
                        }
                        if (value.length > maxLength) continue;
                        if (value.length < minLength) continue;
                        if (lowerCase) value = value.toLowerCase();
                        if (!Utils.isDefined(fi.counts[value])) {
                            fi.counts[value] = 0;
                        }
                        fi.counts[value]++;
                    }
                }
            }



            return fieldInfo;
        }
    });
}


function RamaddaWordcloudDisplay(displayManager, id, properties) {
    var SUPER;
    RamaddaUtil.inherit(this, SUPER = new RamaddaBaseTextDisplay(displayManager, id, DISPLAY_WORDCLOUD, properties));
    addRamaddaDisplay(this);
    $.extend(this, {
        getContentsStyle: function() {
            return "";
        },
        checkLayout: function() {
            this.updateUIInner();
        },
        updateUI: function() {
            var includes = "<link rel='stylesheet' href='" + ramaddaBaseUrl + "/lib/jqcloud.min.css'>";
            includes += "<script src='" + ramaddaBaseUrl + "/lib/jqcloud.min.js'></script>";
            this.writeHtml(ID_DISPLAY_TOP, includes);
            let _this = this;
            var func = function() {
                _this.updateUIInner();
            };
            setTimeout(func, 10);
        },
        updateUIInner: function() {
            var fieldInfo = this.processText();
            if (fieldInfo == null) return;
            let records = this.filterData();
            var allFields = this.getData().getRecordFields();
            var fields = this.getSelectedFields(allFields);
            if (fields.length == 0)
                fields = allFields;
            var strings = this.getFieldsOfType(fields, "string");
            let _this = this;
            var divs = "";
            var words = [];
            var maxWords = parseInt(this.getProperty("maxWords", -1));
            var minCount = parseInt(this.getProperty("minCount", 0));
            var width = (100 * 1 / strings.length) + "%;";
            for (a in fieldInfo) {
                var fi = fieldInfo[a];
                let field = fi.field;
                var handlers = null;
                if (this.getProperty("handleClick", true)) {
                    handlers = {
                        click: function(w) {
                            var word = w.target.innerText;
                            _this.showRows(records, field, word);
                        }
                    }
                };

                var counts = [];
                for (word in fi.counts) {
                    var count = fi.counts[word];
                    counts.push({
                        word: word,
                        count: count
                    });
                }
                counts.sort(function(a, b) {
                    if (a.count < b.count) return -1;
                    if (a.count > b.count) return 1;
                    return 0;
                });
                if (minCount > 0) {
                    var tmp = [];
                    for (var i = 0; i < counts.length; i++) {
                        if (counts[i].count >= minCount)
                            tmp.push(counts[i]);
                    }
                    counts = tmp;
                }
                if (maxWords > 0) {
                    var tmp = [];
                    for (var i = 0; i <= maxWords && i < counts.length; i++) {
                        if (counts[i].count >= minCount)
                            tmp.push(counts[i]);
                    }
                    counts = tmp;
                }

                for (var wordIdx = 0; wordIdx < counts.length; wordIdx++) {
                    var word = counts[wordIdx];
                    var obj1 = {
                        weight: word.count,
                        handlers: handlers,
                        text: word.word,
                    };
                    var obj2 = {
                        weight: word.count,
                        handlers: handlers,
                        text: field.getLabel() + ":" + word.word,
                    };
                    fi.words.push(obj1);
                    words.push(obj2);
                }
                var label = "";
                if (this.getProperty("showFieldLabel", true))
                    label = "<b>" + fi.field.getLabel() + "</b>";

                divs += "<div style='display:inline-block;width:" + width + "'>" + label + HtmlUtils.div(["style", "border: 1px #ccc solid;height:300px;", "id", fi.divId], "") + "</div>";
            }

            this.writeHtml(ID_DISPLAY_CONTENTS, "");
            var options = {
                autoResize: true,
            };


            var colors = this.getColorTable(true);
            if (colors) {
                options.colors = colors,
                    options.classPattern = null;
                options.fontSize = {
                    from: 0.1,
                    to: 0.02
                };
            }
            if (this.getProperty("shape"))
                options.shape = this.getProperty("shape");
            if (this.getProperty("combined", false)) {
                this.writeHtml(ID_DISPLAY_CONTENTS, HtmlUtils.div(["id", this.getDomId("words"), "style", "height:300px;"], ""));
                $("#" + this.getDomId("words")).jQCloud(words, options);
            } else {
                this.writeHtml(ID_DISPLAY_CONTENTS, divs);
                for (a in fieldInfo) {
                    var fi = fieldInfo[a];
                    $("#" + fi.divId).jQCloud(fi.words, options);
                }
            }
        },
        showRows: function(records, field, word) {
            var tokenize = this.getProperty("tokenize", false);
            if (word.startsWith(field.getLabel() + ":")) {
                word = word.replace(field.getLabel() + ":", "");
            }
            var tableFields;
            if (this.getProperty("tableFields")) {
                tableFields = {};
                var list = this.getProperty("tableFields").split(",");
                for (a in list) {
                    tableFields[list[a]] = true;
                }
            }
            var fields = this.getData().getRecordFields();
            var html = "";
            var data = [];
            var header = [];
            data.push(header);
            for (var fieldIdx = 0; fieldIdx < fields.length; fieldIdx++) {
                var f = fields[fieldIdx];
                if (tableFields && !tableFields[f.getId()]) continue;
                header.push(fields[fieldIdx].getLabel());
            }
            var showRecords = this.getProperty("showRecords", false);
            if (showRecords) {
                html += "<br>";
            }
            var re = new RegExp("(\\b" + word + "\\b)", 'i');
            for (var rowIdx = 0; rowIdx < records.length; rowIdx++) {
                var row = this.getDataValues(records[rowIdx]);
                var value = "" + row[field.getIndex()];
                if (tokenize) {
                    if (!value.match(re)) {
                        continue;
                    }
                } else {
                    if (word != value) {
                        continue;
                    }
                }
                var tuple = [];
                data.push(tuple);
                for (var col = 0; col < fields.length; col++) {
                    var f = fields[col];
                    if (tableFields && !tableFields[f.getId()]) continue;
                    var v = row[f.getIndex()];
                    if (tokenize) {
                        v = v.replace(re, "<span style=background:yellow;>$1</span>");
                    }
                    if (showRecords) {
                        html += HtmlUtil.b(f.getLabel()) + ": " + v + "</br>";
                    } else {
                        tuple.push(v);
                    }
                }
                if (showRecords) {
                    html += "<p>";
                }
            }
            if (showRecords) {
                this.writeHtml(ID_DISPLAY_BOTTOM, html);
            } else {
                var prefix = "";
                if (!tokenize) {
                    prefix = field.getLabel() + "=" + word
                }
                this.writeHtml(ID_DISPLAY_BOTTOM, prefix + HtmlUtils.div(["id", this.getDomId("table"), "style", "height:300px"], ""));
                var dataTable = google.visualization.arrayToDataTable(data);
                this.chart = new google.visualization.Table(document.getElementById(this.getDomId("table")));
                this.chart.draw(dataTable, {
                    allowHtml: true
                });
            }
        }
    });
}



function RamaddaImagesDisplay(displayManager, id, properties) {
    var ID_RESULTS = "results";
    var ID_SEARCHBAR = "searchbar";
    let SUPER =  new RamaddaFieldsDisplay(displayManager, id, DISPLAY_IMAGES, properties);
    RamaddaUtil.inherit(this,SUPER);
    addRamaddaDisplay(this);
    $.extend(this, {
        getContentsStyle: function() {
            return "";
        },
        updateUI: function() {
            this.records = this.filterData();
            if(!this.records) return;
            var allFields = this.getData().getRecordFields();
            var fields = this.getSelectedFields(allFields);
            if (fields.length == 0)
                fields = allFields;
            this.splitField = this.getFieldById(fields, this.getProperty("splitBy"));
            this.imageField = this.getFieldOfType(fields, "image");
            this.tooltipFields = this.getFieldsByIds(fields, this.getProperty("tooltipFields","",true).split(","));
            this.labelField = this.getFieldById(fields, this.getProperty("labelField", null, true));
            this.sortField = this.getFieldById(fields, this.getProperty("sortField", null, true));
            if(!this.imageField)  {
                this.displayError("No image field specified");
                return;
            }
            if(this.sortField) {
                var sortAscending = this.getProperty("sortAscending",true);
                this.records.sort((a,b)=>{
                        var row1 = this.getDataValues(a);
                        var row2 = this.getDataValues(b);
                        var result = 0;
                        var v1 = row1[this.sortField.getIndex()];
                        var v2 = row2[this.sortField.getIndex()];
                        if(v1<v2) result = -1;
                        else if(v1>v2) result = 1;
                        if(sortAscending) return result;
                        return result==0?result:-result;
                     });
            }
            this.searchFields = [];
            var contents = "";
            var searchBy = this.getProperty("searchFields","",true).split(",");
            var searchBar = "";

            for(var i=0;i<searchBy.length;i++) {
                var searchField  = this.getFieldById(fields,searchBy[i]);
                if(!searchField) continue;
                this.searchFields.push(searchField);
                var widget;
                var widgetId = this.getDomId("searchby_" + searchField.getId());
                if(searchField.getType() == "enumeration") {
                    var enums = [["","All"]];
                    this.records.map(record=>{
                            var value = this.getDataValues(record)[searchField.getIndex()];
                            if(!enums.includes(value)) enums.push(value);
                        });
                    widget = HtmlUtils.select("",["id",widgetId],enums);
                } else {
                    widget =HtmlUtils.input("","",["id",widgetId]);
                }
                widget =searchField.getLabel() +": " + widget;
                searchBar+=widget +"&nbsp;&nbsp;";
            }

            contents += HtmlUtils.div(["id",this.getDomId(ID_SEARCHBAR)], "<center>" +searchBar+"</center>");
            contents += HtmlUtils.div(["id",this.getDomId(ID_RESULTS)]);
            this.writeHtml(ID_DISPLAY_CONTENTS, contents);
            this.jq(ID_SEARCHBAR).find("select").selectBoxIt({});
            this.jq(ID_SEARCHBAR).find("input, input:radio,select").change(()=>{
                    this.doSearch();
                });
            this.displaySearchResults(this.records);
         },
         doSearch: function() {
                var records = [];
                var values = [];
                for(var i=0;i<this.searchFields.length;i++) {
                    var searchField = this.searchFields[i];
                    values.push($("#" + this.getDomId("searchby_" + searchField.getId())).val());
                }
                for (var rowIdx = 0; rowIdx <this.records.length; rowIdx++) {
                    var row = this.getDataValues(this.records[rowIdx]);
                    var ok = true;
                    for(var i=0;i<this.searchFields.length;i++) {
                        var searchField = this.searchFields[i];
                        var searchValue = values[i];
                        if(searchValue=="") continue;
                        var value = row[searchField.getIndex()];
                        if(searchField.getType() == "enumeration") {
                            if(value!=searchValue) {
                                ok = false;
                                break;
                            }
                        } else {
                            value  = (""+value).toLowerCase();
                            if(value.indexOf(searchValue.toLowerCase())<0) {
                                ok = false;
                                break;
                            }
                        }
                    }
                    if(ok) records.push(this.records[rowIdx]);
                }
                this.displaySearchResults(records);
         },
         displaySearchResults: function(records) {

            var width = this.getProperty("imageWidth","50");
            var margin = this.getProperty("imageMargin","0");
            var splits = {};
            var splitHeaders = [];
            if(!this.splitField) splits[""]="";
            for (var rowIdx = 0; rowIdx <records.length; rowIdx++) {
                var row = this.getDataValues(records[rowIdx]);
                var img = row[this.imageField.getIndex()];
                var tooltip = "";
                var label = "";
                if(this.labelField) label = "<br>" + row[this.labelField.getIndex()];
                for(var i=0;i<this.tooltipFields.length;i++) {
                    if(tooltip!="") tooltip+="&#10;";
                    tooltip+=row[this.tooltipFields[i].getIndex()];
                }
                var img = HtmlUtils.href(img, HtmlUtils.image(img,["width",width]),["class","display-images-popup"]);
                var html =HtmlUtils.div(["class","display-images-item", "title", tooltip, "style","margin:" + margin+"px;"], img+label);
                if(this.splitField) {
                    var splitOn = row[this.splitField.getIndex()];
                    if(!splits[splitOn]) {
                        splits[splitOn] = "";
                        splitHeaders.push(splitOn);
                    }
                    splits[splitOn]+=html;
                } else {
                    splits[""]+=html;
                }
                //                if(rowIdx>10) break;
            }
            var html = "";
            if(!this.splitField) html = splits[""];
            else {
                var width = splitHeaders.length==0?"100%":100/splitHeaders.length;
                html +="<table width=100%><tr valign=top>";
                for(var i=0;i<splitHeaders.length;i++) {
                    var header = splitHeaders[i];
                    html+="<td width=" + width+"%><center><b>" + header+"</b></center>" + HtmlUtils.div(["class","display-images-items"], splits[header])+"</td>";
                }
                html +="</tr></table>";
            }
            this.writeHtml(ID_RESULTS, html);
            this.jq(ID_RESULTS).find("a.display-images-popup").fancybox({});
            }
    });
}



function RamaddaTextstatsDisplay(displayManager, id, properties) {
    let SUPER = new RamaddaBaseTextDisplay(displayManager, id, DISPLAY_TEXTSTATS, properties);
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);
    $.extend(this, {
        updateUI: function() {
            var cnt = {};
            var fieldInfo = this.processText(cnt);
            if (fieldInfo == null) return;
            let records = this.filterData();
            var allFields = this.getData().getRecordFields();
            var fields = this.getSelectedFields(allFields);
            if (fields.length == 0)
                fields = allFields;

            var strings = this.getFieldsOfType(fields, "string");
            let _this = this;
            var divs = "";
            var words = [];
            var html = "";
            var counts = [];
            var maxWords = parseInt(this.getProperty("maxWords", -1));
            var minCount = parseInt(this.getProperty("minCount", 0));
            var showBars = this.getProperty("showBars", true);
            var scale = this.getProperty("barsScale", 10);
            var barColor = this.getProperty("barColor", "blue");

            var barWidth = parseInt(this.getProperty("barWidth", "400"));
            for (a in fieldInfo) {
                var fi = fieldInfo[a];
                let field = fi.field;
                for (word in fi.counts) {
                    var count = fi.counts[word];
                    counts.push({
                        word: word,
                        count: count
                    });
                }
                counts.sort(function(a, b) {
                    if (a.count < b.count) return -1;
                    if (a.count > b.count) return 1;
                    return 0;
                });
                if (minCount > 0) {
                    var tmp = [];
                    for (var i = 0; i < counts.length; i++) {
                        if (counts[i].count >= minCount)
                            tmp.push(counts[i]);
                    }
                    counts = tmp;
                }

                if (maxWords > 0) {
                    var tmp = [];
                    for (var i = 0; i <= maxWords && i < counts.length; i++) {
                        if (counts[i].count >= minCount)
                            tmp.push(counts[i]);
                    }
                    counts = tmp;
                }

                var min = 0;
                var max = 0;
                if (counts.length > 0) {
                    max = counts[0].count;
                    min = counts[counts.length - 1].count;
                }

                var tmp = [];
                for (a in cnt.lengths) {
                    tmp.push({
                        length: parseInt(a),
                        count: cnt.lengths[a]
                    });
                }
                tmp.sort(function(a, b) {
                    if (a.length < b.length) return -1;
                    if (a.length > b.length) return 1;
                    return 0;
                });
                var min = 0;
                var max = 0;
                for (var i = 0; i < tmp.length; i++) {
                    max = (i == 0 ? tmp[i].count : Math.max(max, tmp[i].count));
                    min = (i == 0 ? tmp[i].count : Math.min(min, tmp[i].count));
                }
                if (this.getProperty("showFieldLabel", true))
                    html += "<b>" + fi.field.getLabel() + "</b><br>";
                var td1Width = "20%";
                var td2Width = "10%";
                if (this.getProperty("showSummary", true)) {
                    html += HtmlUtils.openTag("table", ["class", "nowrap ramadda-table", "id", this.getDomId("table_summary")]);
                    html += HtmlUtils.openTag("thead", []);
                    html += HtmlUtils.tr([], HtmlUtils.th(["width", td1Width], "Summary") + HtmlUtils.th([], "&nbsp;"));
                    html += HtmlUtils.closeTag("thead");
                    html += HtmlUtils.openTag("tbody", []);
                    html += HtmlUtils.tr([], HtmlUtils.td(["align", "right"], "Total words:") + HtmlUtils.td([], cnt.count));
                    html += HtmlUtils.tr([], HtmlUtils.td(["align", "right"], "Average word length:") + HtmlUtils.td([], Math.round(cnt.total / cnt.count)));
                    html += HtmlUtils.closeTag("tbody");

                    html += HtmlUtils.closeTag("table");
                    html += "<br>"
                }
                if (this.getProperty("showCounts", true)) {
                    html += HtmlUtils.openTag("table", ["class", "row-border nowrap ramadda-table", "id", this.getDomId("table_counts")]);
                    html += HtmlUtils.openTag("thead", []);
                    html += HtmlUtils.tr([], HtmlUtils.th(["width", td1Width], "Word Length") + HtmlUtils.th(["width", td2Width], "Count") + (showBars ? HtmlUtils.th([], "") : ""));
                    html += HtmlUtils.closeTag("thead");
                    html += HtmlUtils.openTag("tbody", []);
                    for (var i = 0; i < tmp.length; i++) {
                        var row = HtmlUtils.td([], tmp[i].length) + HtmlUtils.td([], tmp[i].count);
                        if (showBars) {
                            var wpercent = (tmp[i].count - min) / max;
                            var width = 2 + wpercent * barWidth;
                            var color = barColor;
                            var div = HtmlUtils.div(["style", "height:10px;width:" + width + "px;background:" + color], "");
                            row += HtmlUtils.td([], div);
                        }
                        html += HtmlUtils.tr([], row);
                    }
                    html += HtmlUtils.closeTag("tbody");
                    html += HtmlUtils.closeTag("table");
                    html += "<br>"
                }
                if (this.getProperty("showFrequency", true)) {
                    html += HtmlUtils.openTag("table", ["class", "row-border ramadda-table", "id", this.getDomId("table_frequency")]);
                    html += HtmlUtils.openTag("thead", []);
                    html += HtmlUtils.tr([], HtmlUtils.th(["width", td1Width], "Word") + HtmlUtils.th(["width", td2Width], "Frequency") + (showBars ? HtmlUtils.th([], "") : ""));
                    html += HtmlUtils.closeTag("thead");
                    html += HtmlUtils.openTag("tbody", []);
                    var min = 0;
                    var max = 0;
                    if (counts.length > 0) {
                        min = counts[0].count;
                        max = counts[counts.length - 1].count;
                    }
                    var totalWords = 0;
                    for (var i = 0; i < counts.length; i++) {
                        totalWords += counts[i].count;
                    }
                    for (var i = counts.length - 1; i >= 0; i--) {
                        var percent = Math.round(10000 * (counts[i].count / totalWords)) / 100;
                        var row = HtmlUtils.td([], counts[i].word + "&nbsp;:&nbsp;") +
                            HtmlUtils.td([], counts[i].count + "&nbsp;&nbsp;(" + percent + "%)&nbsp;:&nbsp;");
                        if (showBars) {
                            var wpercent = (counts[i].count - min) / max;
                            var width = 2 + wpercent * barWidth;
                            var color = barColor;
                            var div = HtmlUtils.div(["style", "height:10px;width:" + width + "px;background:" + color], "");
                            row += HtmlUtils.td([], div);
                        }
                        html += HtmlUtils.tr([], row);
                    }
                    html += HtmlUtils.closeTag("tbody");
                    html += HtmlUtils.closeTag("table");
                }
            }
            this.writeHtml(ID_DISPLAY_CONTENTS, html);
            var tableHeight = this.getProperty("tableHeight", "200");

            if (this.getProperty("showSummary", true))
                HtmlUtils.formatTable("#" + this.getDomId("table_summary"), {
                    scrollY: this.getProperty("tableSummaryHeight", tableHeight)
                });
            if (this.getProperty("showCounts", true))
                HtmlUtils.formatTable("#" + this.getDomId("table_counts"), {
                    scrollY: this.getProperty("tableCountsHeight", tableHeight)
                });
            if (this.getProperty("showFrequency", true))
                HtmlUtils.formatTable("#" + this.getDomId("table_frequency"), {
                    scrollY: this.getProperty("tableFrequenecyHeight", tableHeight),
                    searching: this.getProperty("showSearch", true)
                });
        },
    });
}

function RamaddaTextanalysisDisplay(displayManager, id, properties) {
    let SUPER = new RamaddaBaseTextDisplay(displayManager, id, DISPLAY_TEXTANALYSIS, properties);
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);
    $.extend(this, {
        checkLayout: function() {
            this.updateUIInner();
        },
        updateUI: function() {
            var includes = "<script src='" + ramaddaBaseUrl + "/lib/compromise.min.js'></script>";
            this.writeHtml(ID_DISPLAY_TOP, includes);
            let _this = this;
            var func = function() {
                _this.updateUIInner();
            };
            setTimeout(func, 10);
        },
        updateUIInner: function() {
            let records = this.filterData();
            if (!records) {
                return null;
            }
            let _this = this;
            this.setContents(this.getMessage("Processing text..."));
            var func = function() {
                _this.updateUIInnerInner();
            };
            setTimeout(func, 10);
        },
        updateUIInnerInner: function() {
            let records = this.filterData();
            var allFields = this.getData().getRecordFields();
            var fields = this.getSelectedFields(allFields);
            if (fields.length == 0)
                fields = allFields;
            var strings = this.getFieldsOfType(fields, "string");
            if (strings.length == 0) {
                this.displayError("No string fields specified");
                return null;
            }
            var corpus = "";
            for (var rowIdx = 0; rowIdx < records.length; rowIdx++) {
                var row = this.getDataValues(records[rowIdx]);
                var line = "";
                for (var col = 0; col < fields.length; col++) {
                    var f = fields[col];
                    line += " ";
                    line += row[f.getIndex()];
                }
                corpus += line;
                corpus += "\n";
            }
            var nlp = window.nlp(corpus);
            var cols = [];
            if (this.getProperty("showPeople", false)) {
                cols.push(this.printList("People", nlp.people().out('topk')));
            }
            if (this.getProperty("showPlaces", false)) {
                cols.push(this.printList("Places", nlp.places().out('topk')));
            }
            if (this.getProperty("showOrganizations", false)) {
                cols.push(this.printList("Organizations", nlp.organizations().out('topk')));
            }
            if (this.getProperty("showTopics", false)) {
                cols.push(this.printList("Topics", nlp.topics().out('topk')));
            }
            if (this.getProperty("showNouns", false)) {
                cols.push(this.printList("Nouns", nlp.nouns().out('topk')));
            }
            if (this.getProperty("showVerbs", false)) {
                cols.push(this.printList("Verbs", nlp.verbs().out('topk')));
            }
            if (this.getProperty("showAdverbs", false)) {
                cols.push(this.printList("Adverbs", nlp.adverbs().out('topk')));
            }
            if (this.getProperty("showAdjectives", false)) {
                cols.push(this.printList("Adjectives", nlp.adjectives().out('topk')));
            }
            if (this.getProperty("showClauses", false)) {
                cols.push(this.printList("Clauses", nlp.clauses().out('topk')));
            }
            if (this.getProperty("showContractions", false)) {
                cols.push(this.printList("Contractions", nlp.contractions().out('topk')));
            }
            if (this.getProperty("showPhoneNumbers", false)) {
                cols.push(this.printList("Phone Numbers", nlp.phoneNumbers().out('topk')));
            }
            if (this.getProperty("showValues", false)) {
                cols.push(this.printList("Values", nlp.values().out('topk')));
            }
            if (this.getProperty("showAcronyms", false)) {
                cols.push(this.printList("Acronyms", nlp.acronyms().out('topk')));
            }
            if (this.getProperty("showNGrams", false)) {
                cols.push(this.printList("NGrams", nlp.ngrams().out('topk')));
            }
            if (this.getProperty("showDates", false)) {
                cols.push(this.printList("Dates", nlp.dates().out('topk')));
            }
            if (this.getProperty("showQuotations", false)) {
                cols.push(this.printList("Quotations", nlp.quotations().out('topk')));
            }
            if (this.getProperty("showUrls", false)) {
                cols.push(this.printList("URLs", nlp.urls().out('topk')));
            }
            if (this.getProperty("showStatements", false)) {
                cols.push(this.printList("Statements", nlp.statements().out('topk')));
            }
            if (this.getProperty("showTerms", false)) {
                cols.push(this.printList("Terms", nlp.terms().out('topk')));
            }
            if (this.getProperty("showPossessives", false)) {
                cols.push(this.printList("Possessives", nlp.possessives().out('topk')));
            }
            if (cols.length == 0) {
                this.writeHtml(ID_DISPLAY_CONTENTS, this.getMessage("No text types specified"));
                return;
            }
            var height = this.getProperty("height", "400");
            var html = HtmlUtils.openTag("div", ["id", this.getDomId("tables")]);

            for (var i = 0; i < cols.length; i += 3) {
                var c1 = cols[i];
                var c2 = i + 1 < cols.length ? cols[i + 1] : null;
                var c3 = i + 2 < cols.length ? cols[i + 2] : null;
                var width = c2 ? (c3 ? "33%" : "50%") : "100%";
                var style = "padding:5px";
                var row = "";
                row += HtmlUtils.td(["width", width], HtmlUtils.div(["style", style], c1));
                if (c2)
                    row += HtmlUtils.td(["width", width], HtmlUtils.div(["style", style], c2));
                if (c3)
                    row += HtmlUtils.td(["width", width], HtmlUtils.div(["style", style], c3));
                html += HtmlUtils.tag("table", ["width", "100%"], HtmlUtils.tr(row));
            }
            html += HtmlUtils.closeTag("div");
            this.writeHtml(ID_DISPLAY_CONTENTS, html);
            HtmlUtils.formatTable("#" + this.getDomId("tables") + " .ramadda-table", {
                scrollY: this.getProperty("tableHeight", "200")
            });
        },
        printList: function(title, l) {
            var maxWords = parseInt(this.getProperty("maxWords", 10));
            var minCount = parseInt(this.getProperty("minCount", 0));
            var table = HtmlUtils.openTag("table", ["width", "100%", "class", "stripe hover ramadda-table"]) + HtmlUtils.openTag("thead", []);
            table += HtmlUtils.tr([], HtmlUtils.th([], title) + HtmlUtils.th([], "&nbsp;"));
            table += HtmlUtils.closeTag("thead");
            table += HtmlUtils.openTag("tbody");
            var cnt = 0;
            for (var i = 0; i < l.length; i++) {
                if (l[i].count < minCount) continue;
                var row = HtmlUtils.td([], l[i].normal) +
                    HtmlUtils.td([], l[i].count + " (" + l[i].percent + "%)");
                table += HtmlUtils.tr([], row);
                if (cnt++ > maxWords) break;
            }
            table += HtmlUtils.closeTag("tbody") + HtmlUtils.closeTag("table");
            return table;
        }
    });
}


function RamaddaTextrawDisplay(displayManager, id, properties) {
    var ID_TEXT = "text";
    let SUPER = new RamaddaBaseTextDisplay(displayManager, id, DISPLAY_TEXTRAW, properties);
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);
    $.extend(this, {
        checkLayout: function() {
            this.updateUI();
        },
        updateUI: function() {
            let records = this.filterData();
            if (!records) {
                return null;
            }

            var pattern = this.getProperty("pattern");
            if (pattern && pattern.length == 0) pattern = null;
            this.writeHtml(ID_TOP_RIGHT, HtmlUtils.input("pattern", (pattern ? pattern : ""), ["placeholder", "Search text", "id", this.getDomId("search")]));
            let _this = this;
            this.jq("search").keypress(function(event) {
                if (event.which == 13) {
                    _this.setProperty("pattern", $(this).val());
                    _this.updateUI();
                }
            });
            this.writeHtml(ID_DISPLAY_CONTENTS, HtmlUtils.div(["id", this.getDomId(ID_TEXT)], ""));
            this.showText();
        },
        showText: function() {
            let records = this.filterData();
            if (!records) {
                return null;
            }
            var pattern = this.getProperty("pattern");
            if (pattern && pattern.length == 0) pattern = null;
            var asHtml = this.getProperty("asHtml", true);
            var addLineNumbers = this.getProperty("addLineNumbers", true);
            if (addLineNumbers) asHtml = true;
            var maxLines = parseInt(this.getProperty("maxLines", 100000));
            var lineLength = parseInt(this.getProperty("lineLength", 10000));
            var breakLines = this.getProperty("breakLines", true);


            var includeEmptyLines = this.getProperty("includeEmptyLines", false);
            var allFields = this.getData().getRecordFields();
            var fields = this.getSelectedFields(allFields);
            if (fields.length == 0)
                fields = allFields;
            var strings = this.getFieldsOfType(fields, "string");
            if (strings.length == 0) {
                this.displayError("No string fields specified");
                return null;
            }
            var corpus = "";
            if (addLineNumbers) {
                corpus = "<table width=100%>";
            }
            var lineCnt = 0;
            var displayedLineCnt = 0;
            var re;
            if (pattern) {
                re = new RegExp("(" + pattern + ")");
            }

            for (var rowIdx = 0; rowIdx < records.length; rowIdx++) {
                var row = this.getDataValues(records[rowIdx]);
                var line = "";
                for (var col = 0; col < fields.length; col++) {
                    var f = fields[col];
                    line += " ";
                    line += row[f.getIndex()];
                }
                line = line.trim();
                if (!includeEmptyLines && line.length == 0) continue;
                line = line.replace(/</g, "&lt;").replace(/>/g, "&gt;");
                lineCnt++;
                if (re) {
                    if (!line.toLowerCase().match(re)) continue;
                    line = line.replace(re, "<span style=background:yellow;>$1</span>");
                }
                displayedLineCnt++;

                if (displayedLineCnt > maxLines) break;

                if (addLineNumbers) {
                    corpus += HtmlUtils.tr(["valign", "top"], HtmlUtils.td(["width", "10px"], "<a name=line_" + lineCnt + "></a>" +
                            "<a href=#line_" + lineCnt + ">#" + lineCnt + "</a>&nbsp;  ") +
                        HtmlUtils.td([], line));
                } else {
                    corpus += line;
                    if (asHtml) {
                        if (breakLines)
                            corpus += "<p>";
                        else
                            corpus += "<br>";
                    } else {
                        corpus += "\n";
                        if (breakLines)
                            corpus += "\n";
                    }
                }
            }
            if (addLineNumbers) {
                corpus += "</table>";
            }
            var height = this.getProperty("height", "600");
            if (!asHtml)
                corpus = HtmlUtils.tag("pre", [], corpus);
            var html = HtmlUtils.div(["style", "padding:4px;border:1px #ccc solid;border-top:1px #ccc solid; max-height:" + height + "px;overflow-y:auto;"], corpus);
            this.writeHtml(ID_TEXT, html);
        },
    });
}


function RamaddaTextDisplay(displayManager, id, properties) {
    var SUPER;
    $.extend(this, SUPER = new RamaddaDisplay(displayManager, id, DISPLAY_TEXT, properties));
    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
        lastHtml: "<p>&nbsp;<p>&nbsp;<p>",
        initDisplay: function() {
            SUPER.initDisplay.call(this);
            this.setContents(this.lastHtml);
        },
        handleEventRecordSelection: function(source, args) {
            this.lastHtml = args.html;
            this.setContents(args.html);
        }
    });
}/**
Copyright 2008-2019 Geode Systems LLC
*/



var DISPLAY_ENTRYLIST = "entrylist";
var DISPLAY_TESTLIST = "testlist";
var DISPLAY_ENTRYDISPLAY = "entrydisplay";
var DISPLAY_ENTRY_GALLERY = "entrygallery";
var DISPLAY_ENTRY_GRID = "entrygrid";
var DISPLAY_OPERANDS = "operands";
var DISPLAY_METADATA = "metadata";
var DISPLAY_TIMELINE = "timeline";
var DISPLAY_REPOSITORIES = "repositories";

var ID_RESULTS = "results";
var ID_ENTRIES = "entries";
var ID_DETAILS = "details";
var ID_DETAILS_INNER = "detailsinner";
var ID_PROVIDERS = "providers";
var ID_SEARCH_SETTINGS = "searchsettings";


var ATTR_ENTRYID = "entryid";
var ID_TREE_LINK = "treelink";

addGlobalDisplayType({
    type: DISPLAY_ENTRYLIST,
    label: "Entry List",
    requiresData: false,
    category: "Entry Displays"
});
addGlobalDisplayType({
    type: DISPLAY_TESTLIST,
    label: "Test  List",
    requiresData: false,
    category: "Entry Displays"
});
addGlobalDisplayType({
    type: DISPLAY_ENTRYDISPLAY,
    label: "Entry Display",
    requiresData: false,
    category: "Entry Displays"
});
addGlobalDisplayType({
    type: DISPLAY_ENTRY_GALLERY,
    label: "Entry Gallery",
    requiresData: false,
    category: "Entry Displays"
});
addGlobalDisplayType({
    type: DISPLAY_ENTRY_GRID,
    label: "Entry Date Grid",
    requiresData: false,
    category: "Entry Displays"
});
//addGlobalDisplayType({type: DISPLAY_OPERANDS, label:"Operands",requiresData:false,category:"Entry Displays"});
addGlobalDisplayType({
    type: DISPLAY_METADATA,
    label: "Metadata Table",
    requiresData: false,
    category: "Entry Displays"
});

//addGlobalDisplayType({type: DISPLAY_TIMELINE, label:"Timeline",requiresData:false,category:"Test"});


function RamaddaEntryDisplay(displayManager, id, type, properties) {
    var SUPER;

    RamaddaUtil.inherit(this, SUPER = new RamaddaDisplay(displayManager, id, type, properties));

    this.ramaddas = new Array();
    var repos = this.getProperty("repositories", this.getProperty("repos", null));
    if (repos != null) {
        var toks = repos.split(",");
        //OpenSearch;http://adasd..asdasdas.dasdas.,
        for (var i = 0; i < toks.length; i++) {
            var tok = toks[i];
            tok = tok.trim();
            this.ramaddas.push(getRamadda(tok));
        }
        if (this.ramaddas.length > 0) {
            var container = new RepositoryContainer("all", "All entries");
            addRepository(container);
            for (var i = 0; i < this.ramaddas.length; i++) {
                container.addRepository(this.ramaddas[i]);
            }
            this.ramaddas.push(container);
            this.setRamadda(this.ramaddas[0]);
        }
    }



    RamaddaUtil.defineMembers(this, {
        searchSettings: new EntrySearchSettings({
            parent: properties.entryParent,
            provider: properties.provider,
            text: properties.entryText,
            entryType: properties.entryType,
            orderBy: properties.orderBy
        }),
        entryList: properties.entryList,
        entryMap: {},
        getSearchSettings: function() {
            if (this.providers != null) {
                var provider = this.searchSettings.provider;
                var fromSelect = this.jq(ID_PROVIDERS).val();
                if (fromSelect != null) {
                    provider = fromSelect;
                } else {
                    var toks = this.providers.split(",");
                    if (toks.length > 0) {
                        var tuple = toks[0].split(":");
                        provider = tuple[0];
                    }
                }
                this.searchSettings.provider = provider;
            }
            return this.searchSettings;
        },
        getEntries: function() {
            if (this.entryList == null) return [];
            return this.entryList.getEntries();
        },

    });
    if (properties.entryType != null) {
        this.searchSettings.addType(properties.entryType);
    }
}



function RamaddaSearcher(displayManager, id, type, properties) {
    var NONE = "-- None --";
    var ID_TEXT_FIELD = "textfield";
    var ID_TYPE_FIELD = "typefield";
    var ID_TYPE_DIV = "typediv";
    var ID_FIELDS = "typefields";
    var ID_METADATA_FIELD = "metadatafield";
    var ID_SEARCH = "search";
    var ID_FORM = "form";
    var ID_COLUMN = "column";


    RamaddaUtil.initMembers(this, {
        showForm: true,
        searchText: "",
        showSearchSettings: true,
        showEntries: true,
        showType: true,
        doSearch: true,
        formOpen: true,
        fullForm: true,
        showMetadata: true,
        showToggle: true,
        showArea: true,
        showText: true,
        showDate: true,
        fields: null,
        formWidth: 0,
        entriesWidth: 0,
        //List of type names from user
        types: null,
        entryTypes: null,
        metadataTypeList: [],
        showDetailsForGroup: false,
    });

    var SUPER;
    RamaddaUtil.inherit(this, SUPER = new RamaddaEntryDisplay(displayManager, id, type, properties));


    if (this.showMetadata && this.showSearchSettings) {
        var metadataTypesAttr = this.getProperty("metadataTypes", "enum_tag:Tag");
        //look for type:value:label, or type:label,
        var toks = metadataTypesAttr.split(",");
        for (var i = 0; i < toks.length; i++) {
            var type = toks[i];
            var label = type;
            var value = null;
            var subToks = type.split(":");
            if (subToks.length > 1) {
                type = subToks[0];
                if (subToks.length >= 3) {
                    value = subToks[1];
                    label = subToks[2];
                } else {
                    label = subToks[1];
                }
            }
            this.metadataTypeList.push(new MetadataType(type, label, value));
        }
    }

    RamaddaUtil.defineMembers(this, {
        haveSearched: false,
        haveTypes: false,
        metadata: {},
        metadataLoading: {},
        getDefaultHtml: function() {
            var html = "";
            var horizontal = this.isLayoutHorizontal();
            var footer = this.getFooter();
            if (!this.getProperty("showFooter", true)) {
                footer = "";
            }
            displayDebug = false;
            var entriesDivAttrs = [ATTR_ID, this.getDomId(ID_ENTRIES), ATTR_CLASS, this.getClass("content")];
            var innerHeight = this.getProperty("innerHeight", null);
            if (innerHeight == null) {
                innerHeight = this.getProperty("entriesHeight", null);
            }
            if (innerHeight != null) {
                entriesDivAttrs.push(ATTR_STYLE);
                entriesDivAttrs.push("margin: 0px; padding: 0px;  min-height:" + innerHeight + "px; max-height:" + innerHeight + "px; overflow-y: auto;");
            }
            var resultsDiv = "";
            if (this.getProperty("showHeader", true)) {
                resultsDiv = HtmlUtils.div([ATTR_CLASS, "display-entries-results", ATTR_ID, this.getDomId(ID_RESULTS)], "&nbsp;");
            }

            var entriesDiv =
                resultsDiv +
                HtmlUtils.div(entriesDivAttrs, this.getLoadingMessage());


            if (horizontal) {
                html += HtmlUtils.openTag(TAG_DIV, ["class", "row"]);
                var entriesAttrs = ["class", "col-md-12"];
                if (this.showForm) {
                    var attrs = [];
                    if (this.formWidth === "") {
                        attrs = [];
                    } else if (this.formWidth != 0) {
                        attrs = [ATTR_WIDTH, this.formWidth];
                    }
                    html += HtmlUtils.tag(TAG_DIV, ["class", "col-md-4"], this.makeSearchForm());
                    entriesAttrs = ["class", "col-md-8"];
                }
                if (this.showEntries) {
                    var attrs = [ATTR_WIDTH, "75%"];
                    if (this.entriesWidth === "") {
                        attrs = [];
                    } else if (this.entriesWidth != 0) {
                        attrs = [ATTR_WIDTH, this.entriesWidth];
                    }
                    html += HtmlUtils.tag(TAG_DIV, entriesAttrs, entriesDiv);
                }
                html += HtmlUtils.closeTag("row");

                html += HtmlUtils.openTag(TAG_DIV, ["class", "row"]);
                if (this.showForm) {
                    html += HtmlUtils.tag(TAG_DIV, ["class", "col-md-6"], "");
                }
                if (this.showEntries) {
                    if (this.getProperty("showFooter", true)) {
                        html += HtmlUtils.tag(TAG_DIV, ["class", "col-md-6"], footer);
                    }
                }
                html += HtmlUtils.closeTag(TAG_DIV);
            } else {
                if (this.showForm) {
                    html += this.makeSearchForm();
                }
                if (this.showEntries) {
                    html += entriesDiv;
                    html += footer;
                }
            }
            html += HtmlUtils.div([ATTR_CLASS, "display-entry-popup", ATTR_ID, this.getDomId(ID_DETAILS)], "&nbsp;");
            return html;
        },
        initDisplay: function() {
            var theDisplay = this;


            this.jq(ID_SEARCH).button().click(function(event) {
                theDisplay.submitSearchForm();
                event.preventDefault();
            });

            this.jq(ID_TEXT_FIELD).autocomplete({
                source: function(request, callback) {
                    //                            theDisplay.doQuickEntrySearch(request, callback);
                }
            });


            //                $(".display-metadatalist").selectBoxIt({});

            this.jq(ID_REPOSITORY).selectBoxIt({});
            this.jq(ID_REPOSITORY).change(function() {
                var v = theDisplay.jq(ID_REPOSITORY).val();
                var ramadda = getRamadda(v);
                theDisplay.setRamadda(ramadda);
                theDisplay.addTypes(null);
                theDisplay.typeChanged();
            });

            this.jq(ID_FORM).submit(function(event) {
                theDisplay.submitSearchForm();
                event.preventDefault();
            });


            this.addTypes(this.entryTypes);
            for (var i = 0; i < this.metadataTypeList.length; i++) {
                var type = this.metadataTypeList[i];
                this.addMetadata(type, null);
            }
            if (!this.haveSearched) {
                if (this.doSearch) {
                    this.submitSearchForm();
                }
            }
        },
        showEntryDetails: async function(event, entryId, src, leftAlign) {
            if (true) return;
            var entry;
            await this.getEntry(entryId, e => {
                entry = e
            });
            var popupId = "#" + this.getDomId(ID_DETAILS + entryId);
            if (this.currentPopupEntry == entry) {
                this.hideEntryDetails(entryId);
                return;
            }
            var myloc = 'right top';
            var atloc = 'right bottom';
            if (leftAlign) {
                myloc = 'left top';
                atloc = 'left bottom';
            }
            this.currentPopupEntry = entry;
            if (src == null) src = this.getDomId("entry_" + entry.getIdForDom());
            var close = HtmlUtils.onClick(this.getGet() + ".hideEntryDetails('" + entryId + "');",
                HtmlUtils.image(icon_close));

            var contents = this.getEntryHtml(entry, {
                headerRight: close
            });
            $(popupId).html(contents);
            $(popupId).show();
            /*
            $(popupId).position({
                    of: jQuery( "#" +src),
                        my: myloc,
                        at: atloc,
                        collision: "none none"
                        });
            */
        },

        getResultsHeader: function(entries) {
            var left = "Showing " + (this.searchSettings.skip + 1) + "-" + (this.searchSettings.skip + Math.min(this.searchSettings.max, entries.length));
            var nextPrev = [];
            var lessMore = [];
            if (this.searchSettings.skip > 0) {
                nextPrev.push(HtmlUtils.onClick(this.getGet() + ".loadPrevUrl();", HtmlUtils.image(ramaddaBaseUrl + "/icons/arrow_left.png", [ATTR_TITLE, "Previous", "border", "0"]), [ATTR_CLASS, "display-link"]));
            }
            var addMore = false;
            if (entries.length == this.searchSettings.getMax()) {
                nextPrev.push(HtmlUtils.onClick(this.getGet() + ".loadNextUrl();", HtmlUtils.image(ramaddaBaseUrl + "/icons/arrow_right.png", [ATTR_TITLE, "Next", "border", "0"]), [ATTR_CLASS, "display-link"]));
                addMore = true;
            }

            lessMore.push(HtmlUtils.onClick(this.getGet() + ".loadLess();", HtmlUtils.image(ramaddaBaseUrl + "/icons/greenminus.png", [ATTR_ALT, "View less", ATTR_TITLE, "View less", "border", "0"]), [ATTR_CLASS, "display-link"]));
            if (addMore) {
                lessMore.push(HtmlUtils.onClick(this.getGet() + ".loadMore();", HtmlUtils.image(ramaddaBaseUrl + "/icons/greenplus.png", [ATTR_ALT, "View more", ATTR_TITLE, "View more", "border", "0"]), [ATTR_CLASS, "display-link"]));
            }
            var results = "";
            var spacer = "&nbsp;&nbsp;&nbsp;"
            results = left + spacer +
                HtmlUtils.join(nextPrev, "&nbsp;") + spacer +
                HtmlUtils.join(lessMore, "&nbsp;");
            return results;
        },
        submitSearchForm: function() {
            if (this.fixedEntries) {
                return;
            }
            this.haveSearched = true;
            var settings = this.getSearchSettings();
            settings.text = this.getFieldValue(this.getDomId(ID_TEXT_FIELD), settings.text);

            if (this.textRequired && (settings.text == null || settings.text.trim().length == 0)) {
                this.writeHtml(ID_ENTRIES, "");
                return;
            }

            if (this.haveTypes) {
                settings.entryType = this.getFieldValue(this.getDomId(ID_TYPE_FIELD), settings.entryType);
            }
            settings.clearAndAddType(settings.entryType);

            if (this.areaWidget) {
                this.areaWidget.setSearchSettings(settings);
            }
            if (this.dateRangeWidget) {
                this.dateRangeWidget.setSearchSettings(settings);
            }
            settings.metadata = [];
            for (var i = 0; i < this.metadataTypeList.length; i++) {
                var metadataType = this.metadataTypeList[i];
                var value = metadataType.getValue();
                if (value == null) {
                    value = this.getFieldValue(this.getMetadataFieldId(metadataType), null);
                }
                if (value != null) {
                    settings.metadata.push({
                        type: metadataType.getType(),
                        value: value
                    });
                }
            }

            //Call this now because it sets settings


            var theRepository = this.getRamadda()

            if (theRepository.children) {
                console.log("Searching  multiple ramaddas");
                this.entryList = new EntryListHolder(theRepository, this);
                this.multiSearch = {
                    count: 0,
                };

                for (var i = 0; i < theRepository.children.length; i++) {
                    var ramadda = theRepository.children[i];
                    var jsonUrl = this.makeSearchUrl(ramadda);
                    this.updateForSearching(jsonUrl);
                    this.entryList.addEntryList(new EntryList(ramadda, jsonUrl, null, false));
                    this.multiSearch.count++;
                }
                this.entryList.doSearch(this);
            } else {
                this.multiSearch = null;
                var jsonUrl = this.makeSearchUrl(this.getRamadda());
                console.log(jsonUrl);
                this.entryList = new EntryList(this.getRamadda(), jsonUrl, this, true);
                this.updateForSearching(jsonUrl);
            }


        },
        handleSearchError: function(url, msg) {
            this.writeHtml(ID_ENTRIES, "");
            this.writeHtml(ID_RESULTS, "");
            console.log("Error performing search:" + msg);
            //alert("There was an error performing the search\n" + msg);
        },
        updateForSearching: function(jsonUrl) {
            var outputs = this.getRamadda().getSearchLinks(this.getSearchSettings());
            this.footerRight = outputs == null ? "" : "Links: " + HtmlUtils.join(outputs, " - ");
            this.writeHtml(ID_FOOTER_RIGHT, this.footerRight);
            var msg = this.searchMessage;
            if (msg == null) {
                msg = this.getRamadda().getSearchMessage();
            }
            var provider = this.getSearchSettings().provider;
            if (provider != null) {
                msg = null;
                if (this.providerMap != null) {
                    msg = this.providerMap[provider];
                }
                if (msg == null) {
                    msg = provider;
                }
                msg = "Searching " + msg;
            }

            //            this.showMessage(msg, HtmlUtils.div([ATTR_STYLE, "margin:20px;"], this.getWaitImage()));
            this.showMessage(this.getWaitImage() + " " + msg, HtmlUtils.div([ATTR_STYLE, "margin:20px;"], ""));
            this.hideEntryDetails();
        },
        showMessage: function(title, inner) {
            this.writeHtml(ID_RESULTS, title);
            this.writeHtml(ID_ENTRIES, inner);
        },
        prepareToLayout: function() {
            SUPER.prepareToLayout.apply(this);
            this.savedValues = {};
            var cols = this.getSearchableColumns();
            for (var i = 0; i < cols.length; i++) {
                var col = cols[i];
                var id = this.getDomId(ID_COLUMN + col.getName());
                var value = $("#" + id).val();
                if (value == null || value.length == 0) continue;
                this.savedValues[id] = value;
            }
        },
        makeSearchUrl: function(repository) {
            var extra = "";
            var cols = this.getSearchableColumns();
            for (var i = 0; i < cols.length; i++) {
                var col = cols[i];
                var value = this.jq(ID_COLUMN + col.getName()).val();
                if (value == null || value.length == 0) continue;
                extra += "&" + col.getSearchArg() + "=" + encodeURI(value);
            }
            this.getSearchSettings().setExtra(extra);
            var jsonUrl = repository.getSearchUrl(this.getSearchSettings(), OUTPUT_JSON);
            return jsonUrl;
        },
        makeSearchForm: function() {
            var form = HtmlUtils.openTag("form", [ATTR_ID, this.getDomId(ID_FORM), "action", "#"]);
            var extra = "";
            var text = this.getSearchSettings().text;
            if (text == null) {
                var args = Utils.getUrlArgs(document.location.search);
                text = args.text;
            }
            if (text == null) {
                text = this.searchText;
            }

            var eg = " search text";
            if (this.eg) {
                eg = " " + this.eg;
            }



            var buttonLabel = HtmlUtils.image(ramaddaBaseUrl + "/icons/magnifier.png", [ATTR_BORDER, "0", ATTR_TITLE, "Search"]);
            var topItems = [];
            var extra = "";
            extra += HtmlUtils.formTable();
            if (this.showArea) {
                this.areaWidget = new AreaWidget(this);
                extra += HtmlUtils.formEntry("Area:", this.areaWidget.getHtml());
            }

            var searchButton = HtmlUtils.div([ATTR_ID, this.getDomId(ID_SEARCH), ATTR_CLASS, "display-button"], buttonLabel);


            if (this.ramaddas.length > 0) {
                var select = HtmlUtils.openTag(TAG_SELECT, [ATTR_ID, this.getDomId(ID_REPOSITORY), ATTR_CLASS, "display-repositories-select"]);
                var icon = ramaddaBaseUrl + "/icons/favicon.png";
                for (var i = 0; i < this.ramaddas.length; i++) {
                    var ramadda = this.ramaddas[i];
                    var attrs = [ATTR_TITLE, "", ATTR_VALUE, ramadda.getId(),
                        "data-iconurl", icon
                    ];
                    if (this.getRamadda().getId() == ramadda.getId()) {
                        attrs.push("selected");
                        attrs.push(null);
                    }
                    var label =
                        select += HtmlUtils.tag(TAG_OPTION, attrs,
                            ramadda.getName());
                }
                select += HtmlUtils.closeTag(TAG_SELECT);
                topItems.push(select);
            }


            this.providerMap = {};
            if (this.providers != null) {
                var options = "";
                var selected = Utils.getUrlArgs(document.location.search).provider;
                var toks = this.providers.split(",");
                var currentCategory = null;
                var catToBuff = {};
                var cats = [];

                for (var i = 0; i < toks.length; i++) {
                    var tuple = toks[i].split(":");
                    var id = tuple[0];

                    id = id.replace(/_COLON_/g, ":");
                    var label = tuple.length > 1 ? tuple[1] : id;
                    if (label.length > 40) {
                        label = label.substring(0, 39) + "...";
                    }
                    this.providerMap[id] = label;
                    var extraAttrs = "";
                    if (id == selected) {
                        extraAttrs += " selected ";
                    }
                    var category = "";

                    if (tuple.length > 3) {
                        category = tuple[3];
                    }
                    var buff = catToBuff[category];
                    if (buff == null) {
                        cats.push(category);
                        catToBuff[category] = "";
                        buff = "";
                    }
                    if (tuple.length > 2) {
                        var img = tuple[2];
                        img = img.replace(/\${urlroot}/g, ramaddaBaseUrl);
                        img = img.replace(/\${root}/g, ramaddaBaseUrl);
                        extraAttrs += " data-iconurl=\"" + img + "\" ";
                    }
                    buff += "<option " + extraAttrs + " value=\"" + id + "\">" + label + "</option>\n";
                    catToBuff[category] = buff;
                }

                for (var catIdx = 0; catIdx < cats.length; catIdx++) {
                    var category = cats[catIdx];
                    if (category != "")
                        options += "<optgroup label=\"" + category + "\">\n";
                    options += catToBuff[category];
                    if (category != "")
                        options += "</optgroup>";

                }
                topItems.push(HtmlUtils.tag("select", ["multiple", null, "id", this.getDomId(ID_PROVIDERS), ATTR_CLASS, "display-search-providers"], options));
            }


            if (this.showType) {
                topItems.push(HtmlUtils.span([ATTR_ID, this.getDomId(ID_TYPE_DIV)], HtmlUtils.span([ATTR_CLASS, "display-loading"], "Loading types...")));
            }

            var textField = HtmlUtils.input("", text, ["placeholder", eg, ATTR_CLASS, "display-search-input", ATTR_SIZE, "30", ATTR_ID, this.getDomId(ID_TEXT_FIELD)]);

            if (this.showText) {
                topItems.push(textField);
            }




            var horizontal = this.isLayoutHorizontal();

            if (horizontal) {
                var tmp = HtmlUtils.join(topItems, "<br>");
                form += "<table><tr valign=top><td>" + searchButton + "</td><td>" + tmp + "</td></tr></table>";
            } else {
                form += searchButton + " " + HtmlUtils.join(topItems, " ");
            }

            if (this.showDate) {
                this.dateRangeWidget = new DateRangeWidget(this);
                extra += HtmlUtils.formEntry("Date Range:", this.dateRangeWidget.getHtml());
            }

            if (this.showMetadata) {
                for (var i = 0; i < this.metadataTypeList.length; i++) {
                    var type = this.metadataTypeList[i];
                    var value = type.getValue();
                    var metadataSelect;
                    if (value != null) {
                        metadataSelect = value;
                    } else {
                        metadataSelect = HtmlUtils.tag(TAG_SELECT, [ATTR_ID, this.getMetadataFieldId(type),
                                ATTR_CLASS, "display-metadatalist"
                            ],
                            HtmlUtils.tag(TAG_OPTION, [ATTR_TITLE, "", ATTR_VALUE, ""],
                                NONE));
                    }
                    extra += HtmlUtils.formEntry(type.getLabel() + ":", metadataSelect);
                }
            }
            extra += HtmlUtils.closeTag(TAG_TABLE);
            extra += HtmlUtils.div([ATTR_ID, this.getDomId(ID_FIELDS)], "");


            if (this.showSearchSettings) {
                var id = this.getDomId(ID_SEARCH_SETTINGS);
                if (this.showToggle) {
                    form += HtmlUtils.div([ATTR_CLASS, "display-search-extra", ATTR_ID, id],
                        HtmlUtils.toggleBlock("Search Settings", HtmlUtils.div([ATTR_CLASS, "display-search-extra-inner"], extra), this.formOpen));
                } else {
                    form += HtmlUtils.div([ATTR_CLASS, "display-search-extra", ATTR_ID, id],
                        HtmlUtils.div([ATTR_CLASS, "display-search-extra-inner"], extra));
                }
            }

            //Hide the real submit button
            form += "<input type=\"submit\" style=\"position:absolute;left:-9999px;width:1px;height:1px;\"/>";
            form += HtmlUtils.closeTag("form");

            return form;

        },
        handleEventMapBoundsChanged: function(source, args) {
            if (this.areaWidget) {
                this.areaWidget.handleEventMapBoundsChanged(source, args);
            }
        },
        typeChanged: function() {
            var settings = this.getSearchSettings();
            settings.skip = 0;
            settings.max = 50;
            settings.entryType = this.getFieldValue(this.getDomId(ID_TYPE_FIELD), settings.entryType);
            settings.clearAndAddType(settings.entryType);
            this.addExtraForm();
            this.submitSearchForm();
        },
        addMetadata: function(metadataType, metadata) {
            if (metadata == null) {
                metadata = this.metadata[metadataType.getType()];
            }
            if (metadata == null) {
                var theDisplay = this;
                if (!this.metadataLoading[metadataType.getType()]) {
                    this.metadataLoading[metadataType.getType()] = true;
                    metadata = this.getRamadda().getMetadataCount(metadataType, function(metadataType, metadata) {
                        theDisplay.addMetadata(metadataType, metadata);
                    });
                }
            }
            if (metadata == null) {
                return;
            }

            this.metadata[metadataType.getType()] = metadata;


            var select = HtmlUtils.tag(TAG_OPTION, [ATTR_TITLE, "", ATTR_VALUE, ""], NONE);
            for (var i = 0; i < metadata.length; i++) {
                var count = metadata[i].count;
                var value = metadata[i].value;
                var label = metadata[i].label;
                var optionAttrs = [ATTR_VALUE, value, ATTR_CLASS, "display-metadatalist-item"];
                var selected = false;
                if (selected) {
                    optionAttrs.push("selected");
                    optionAttrs.push(null);
                }
                select += HtmlUtils.tag(TAG_OPTION, optionAttrs, label + " (" + count + ")");
            }
            $("#" + this.getMetadataFieldId(metadataType)).html(select);
        },

        getMetadataFieldId: function(metadataType) {
            var id = metadataType.getType();
            id = id.replace(".", "_");
            return this.getDomId(ID_METADATA_FIELD + id);
        },

        findEntryType: function(typeName) {
            if (this.entryTypes == null) return null;
            for (var i = 0; i < this.entryTypes.length; i++) {
                var type = this.entryTypes[i];
                if (type.getId() == typeName) return type;
            }
            return null;
        },
        addTypes: function(newTypes) {
            if (!this.showType) {
                return;
            }
            if (newTypes == null) {
                var theDisplay = this;
                newTypes = this.getRamadda().getEntryTypes(function(ramadda, types) {
                    theDisplay.addTypes(types);
                });
            }
            if (newTypes == null) {
                return;
            }

            this.entryTypes = newTypes;

            if (this.types) {
                var showType = {};
                var listOfTypes = this.types.split(",");
                for (var i = 0; i < listOfTypes.length; i++) {
                    var type = listOfTypes[i];
                    showType[type] = true;
                }
                var tmp = [];
                for (var i = 0; i < this.entryTypes.length; i++) {
                    var type = this.entryTypes[i];
                    if (showType[type.getId()]) {
                        tmp.push(type);
                    } else if (type.getCategory() != null && showType[type.getCategory()]) {
                        tmp.push(type);
                    }
                }
                this.entryTypes = tmp;
            }

            this.haveTypes = true;
            var cats = [];
            var catMap = {};
            var select = HtmlUtils.openTag(TAG_SELECT, [ATTR_ID, this.getDomId(ID_TYPE_FIELD),
                ATTR_CLASS, "display-typelist",
                "onchange", this.getGet() + ".typeChanged();"
            ]);
            //                HtmlUtils.tag(TAG_OPTION,[ATTR_TITLE,"",ATTR_VALUE,""], " Choose Type "));
            select += HtmlUtils.tag(TAG_OPTION, [ATTR_TITLE, "", ATTR_VALUE, ""], "Any Type");

            for (var i = 0; i < this.entryTypes.length; i++) {
                var type = this.entryTypes[i];
                //                    var style = " background: URL(" + type.getIcon() +") no-repeat;";
                var icon = type.getIcon();
                var optionAttrs = [ATTR_TITLE, type.getLabel(), ATTR_VALUE, type.getId(), ATTR_CLASS, "display-typelist-type",
                    //                                        ATTR_STYLE, style,
                    "data-iconurl", icon
                ];
                var selected = this.getSearchSettings().hasType(type.getId());
                if (selected) {
                    optionAttrs.push("selected");
                    optionAttrs.push(null);
                }
                var option = HtmlUtils.tag(TAG_OPTION, optionAttrs, type.getLabel() + " (" + type.getEntryCount() + ")");
                var map = catMap[type.getCategory()];
                if (map == null) {
                    catMap[type.getCategory()] = HtmlUtils.tag(TAG_OPTION, [ATTR_CLASS, "display-typelist-category", ATTR_TITLE, "", ATTR_VALUE, ""], type.getCategory());
                    cats.push(type.getCategory());
                }
                catMap[type.getCategory()] += option;

            }
            for (var i in cats) {
                select += catMap[cats[i]];
            }

            select += HtmlUtils.closeTag(TAG_SELECT);
            //                this.writeHtml(ID_TYPE_FIELD, "# " + entryTypes.length);
            //                this.writeHtml(ID_TYPE_FIELD, select);
            this.writeHtml(ID_TYPE_DIV, select);
            this.jq(ID_TYPE_FIELD).selectBoxIt({});
            this.addExtraForm();
        },
        getSelectedType: function() {
            if (this.entryTypes == null) return null;
            for (var i = 0; i < this.entryTypes.length; i++) {
                var type = this.entryTypes[i];
                if (type.getId) {
                    if (this.getSearchSettings().hasType(type.getId())) {
                        return type;
                    }
                }
            }
            return null;
        },
        getSearchableColumns: function() {
            var searchable = [];
            var type = this.getSelectedType();
            if (type == null) {
                return searchable;
            }
            var cols = type.getColumns();
            if (cols == null) {
                return searchable;
            }
            for (var i = 0; i < cols.length; i++) {
                var col = cols[i];
                if (!col.getCanSearch()) continue;
                searchable.push(col);
            }
            return searchable;
        },
        addExtraForm: function() {
            if (this.savedValues == null) this.savedValues = {};
            var extra = "";
            var cols = this.getSearchableColumns();
            for (var i = 0; i < cols.length; i++) {
                var col = cols[i];
                if (this.fields != null && this.fields.indexOf(col.getName()) < 0) {
                    continue;
                }


                if (extra.length == 0) {
                    extra += HtmlUtils.formTable();
                }
                var field = "";
                var id = this.getDomId(ID_COLUMN + col.getName());
                var savedValue = this.savedValues[id];
                if (savedValue == null) {
                    savedValue = this.jq(ID_COLUMN + col.getName()).val();
                }
                if (savedValue == null) savedValue = "";
                if (col.isEnumeration()) {
                    field = HtmlUtils.openTag(TAG_SELECT, [ATTR_ID, id, ATTR_CLASS, "display-menu"]);
                    field += HtmlUtils.tag(TAG_OPTION, [ATTR_TITLE, "", ATTR_VALUE, ""],
                        "-- Select --");
                    var values = col.getValues();
                    for (var vidx in values) {
                        var value = values[vidx].value;
                        var label = values[vidx].label;
                        var extraAttr = "";
                        if (value == savedValue) {
                            extraAttr = " selected ";
                        }
                        field += HtmlUtils.tag(TAG_OPTION, [ATTR_TITLE, label, ATTR_VALUE, value, extraAttr, null],
                            label);
                    }
                    field += HtmlUtils.closeTag(TAG_SELECT);
                } else {
                    field = HtmlUtils.input("", savedValue, [ATTR_CLASS, "input", ATTR_SIZE, "15", ATTR_ID, id]);
                }
                extra += HtmlUtils.formEntry(col.getLabel() + ":", field + " " + col.getSuffix());

            }
            if (extra.length > 0) {
                extra += HtmlUtils.closeTag(TAG_TABLE);
            }

            this.writeHtml(ID_FIELDS, extra);

            $(".display-menu").selectBoxIt({});


        },
        getEntries: function() {
            if (this.entryList == null) return [];
            return this.entryList.getEntries();
        },
        loadNextUrl: function() {
            this.getSearchSettings().skip += this.getSearchSettings().max;
            this.submitSearchForm();
        },
        loadMore: function() {
            this.getSearchSettings().max = this.getSearchSettings().max += 50;
            this.submitSearchForm();
        },
        loadLess: function() {
            var max = this.getSearchSettings().max;
            max = parseInt(0.75 * max);
            this.getSearchSettings().max = Math.max(1, max);
            this.submitSearchForm();
        },
        loadPrevUrl: function() {
            this.getSearchSettings().skip = Math.max(0, this.getSearchSettings().skip - this.getSearchSettings().max);
            this.submitSearchForm();
        },
        entryListChanged: function(entryList) {
            this.entryList = entryList;
        }
    });
}


function RamaddaEntrylistDisplay(displayManager, id, properties, theType) {
    var SUPER;
    if (theType == null) {
        theType = DISPLAY_ENTRYLIST;
    }
    RamaddaUtil.inherit(this, SUPER = new RamaddaSearcher(displayManager, id, DISPLAY_ENTRYLIST, properties));
    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
        haveDisplayed: false,
        selectedEntries: [],
        getSelectedEntries: function() {
            return this.selectedEntries;
        },
        initDisplay: function() {
            var _this = this;
            if (this.getIsLayoutFixed() && this.haveDisplayed) {
                return;
            }
            this.haveDisplayed = true;
            this.createUI();
            this.setContents(this.getDefaultHtml());
            if (this.dateRangeWidget) {
                this.dateRangeWidget.initHtml();
            }
            SUPER.initDisplay.apply(this);
            if (this.entryList != null && this.entryList.haveLoaded) {
                this.entryListChanged(this.entryList);
            }
            this.jq(ID_PROVIDERS).selectBoxIt({});
            this.jq(ID_PROVIDERS).change(function() {
                _this.providerChanged();
            });
        },
        providerChanged: function() {
            var provider = this.jq(ID_PROVIDERS).val();
            if (provider != "this") {
                this.jq(ID_SEARCH_SETTINGS).hide();
            } else {
                this.jq(ID_SEARCH_SETTINGS).show();
            }
        },
        getMenuItems: function(menuItems) {
            SUPER.getMenuItems.apply(this, menuItems);
            if (this.getSelectedEntriesFromTree().length > 0) {
                var get = this.getGet();
                menuItems.push(HtmlUtils.onClick(get + ".makeDisplayList();", "Make List"));
                menuItems.push(HtmlUtils.onClick(get + ".makeDisplayGallery();", "Make Gallery"));
            }
        },
        makeDisplayList: function() {
            var entries = this.getSelectedEntriesFromTree();
            if (entries.length == 0) {
                return;
            }
            var props = {
                selectedEntries: entries,
                showForm: false,
                showMenu: true,
                fixedEntries: true
            };
            props.entryList = new EntryList(this.getRamadda(), "", this, false);
            props.entryList.setEntries(entries);
            this.getDisplayManager().createDisplay(DISPLAY_ENTRYLIST, props);
        },
        makeDisplayGallery: function() {
            var entries = this.getSelectedEntriesFromTree();
            if (entries.length == 0) {
                return;
            }

            //xxxx
            var props = {
                selectedEntries: entries
            };
            this.getDisplayManager().createDisplay(DISPLAY_ENTRY_GALLERY, props);
        },
        handleEventEntrySelection: function(source, args) {
            this.selectEntry(args.entry, args.selected);
        },
        selectEntry: function(entry, selected) {
            var changed = false;
            if (selected) {
                this.jq("entry_" + entry.getIdForDom()).addClass("ui-selected");
                var index = this.selectedEntries.indexOf(entry);
                if (index < 0) {
                    this.selectedEntries.push(entry);
                    changed = true;
                }
            } else {
                this.jq("entry_" + entry.getIdForDom()).removeClass("ui-selected");
                var index = this.selectedEntries.indexOf(entry);
                if (index >= 0) {
                    this.selectedEntries.splice(index, 1);
                    changed = true;
                }
            }
        },
        makeEntriesDisplay: function(entries) {
            return this.getEntriesTree(entries);
        },

        entryListChanged: function(entryList) {
            if (this.multiSearch) {
                this.multiSearch.count--;
            }
            SUPER.entryListChanged.apply(this, [entryList]);
            var entries = this.entryList.getEntries();

            if (entries.length == 0) {
                this.getSearchSettings().skip = 0;
                this.getSearchSettings().max = 50;
                var msg = "Nothing found";
                if (this.multiSearch) {
                    if (this.multiSearch.count > 0) {
                        msg = "Nothing found so far. Still searching " + this.multiSearch.count + " repositories";
                    } else {}
                }
                this.writeHtml(ID_ENTRIES, this.getMessage(msg));
                this.writeHtml(ID_FOOTER_LEFT, "");
                this.writeHtml(ID_RESULTS, "&nbsp;");
                this.getDisplayManager().handleEventEntriesChanged(this, []);
                return;
            }
            this.writeHtml(ID_RESULTS, this.getResultsHeader(entries));


            var get = this.getGet();
            this.writeHtml(ID_FOOTER_LEFT, "");
            if (this.footerRight != null) {
                this.writeHtml(ID_FOOTER_RIGHT, this.footerRight);
            }

            //                var entriesHtml  = this.getEntriesTree(entries);
            var entriesHtml = this.makeEntriesDisplay(entries);

            var html = "";
            html += HtmlUtils.openTag(TAG_OL, [ATTR_CLASS, this.getClass("list"), ATTR_ID, this.getDomId(ID_LIST)]);
            html += entriesHtml;
            html += HtmlUtils.closeTag(TAG_OL);
            this.writeHtml(ID_ENTRIES, html);
            this.addEntrySelect();

            this.getDisplayManager().handleEventEntriesChanged(this, entries);
        },
    });
}



function RamaddaTestlistDisplay(displayManager, id, properties) {
    var SUPER;
    RamaddaUtil.inherit(this, SUPER = new RamaddaEntrylistDisplay(displayManager, id, properties, DISPLAY_TESTLIST));
    RamaddaUtil.defineMembers(this, {
        //This gets called by the EntryList to actually make the display
        makeEntriesDisplay: function(entries) {

            return "Overridden display<br>" + this.getEntriesTree(entries);
        },
    });

}



var RamaddaListDisplay = RamaddaEntrylistDisplay;



function RamaddaEntrygalleryDisplay(displayManager, id, properties) {
    var ID_GALLERY = "gallery";
    var SUPER;
    RamaddaUtil.inherit(this, SUPER = new RamaddaEntryDisplay(displayManager, id, DISPLAY_ENTRY_GALLERY, properties));
    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
        entries: properties.entries,
        initDisplay: function() {
            var _this = this;
            this.createUI();
            var html = HtmlUtils.div([ATTR_ID, this.getDomId(ID_GALLERY)], "Gallery");
            this.setContents(html);

            if (this.selectedEntries != null) {
                this.jq(ID_GALLERY).html(this.getEntriesGallery(this.selectedEntries));
                return;
            }
            if (this.entries) {
                var props = {
                    entries: this.entries
                };
                var searchSettings = new EntrySearchSettings(props);
                var jsonUrl = this.getRamadda().getSearchUrl(searchSettings, OUTPUT_JSON, "BAR");
                var myCallback = {
                    entryListChanged: function(list) {
                        var entries = list.getEntries();
                        _this.jq(ID_GALLERY).html(_this.getEntriesGallery(entries));
                        $("a.popup_image").fancybox({
                            'titleShow': false
                        });
                    }
                };
                var entryList = new EntryList(this.getRamadda(), jsonUrl, myCallback, true);
            }

            if (this.entryList != null && this.entryList.haveLoaded) {
                this.entryListChanged(this.entryList);
            }
        },
        getEntriesGallery: function(entries) {
            var nonImageHtml = "";
            var html = "";
            var imageCnt = 0;
            var imageEntries = [];
            for (var i = 0; i < entries.length; i++) {
                var entry = entries[i];
                //Don: Right now this just shows all of the images one after the other.
                //If there is just one image we should just display it
                //We should do a gallery here if more than 1

                if (entry.isImage()) {
                    imageEntries.push(entry);
                    var link = HtmlUtils.tag(TAG_A, [ATTR_HREF, entry.getEntryUrl()], entry.getName());
                    imageCnt++;
                    html += HtmlUtils.tag(TAG_IMG, ["src", entry.getResourceUrl(), ATTR_WIDTH, "500", ATTR_ID,
                            this.getDomId("entry_" + entry.getIdForDom()),
                            ATTR_ENTRYID, entry.getId(), ATTR_CLASS, "display-entrygallery-entry"
                        ]) + "<br>" +
                        link + "<p>";
                } else {
                    var icon = entry.getIconImage([ATTR_TITLE, "View entry"]);
                    var link = HtmlUtils.tag(TAG_A, [ATTR_HREF, entry.getEntryUrl()], icon + " " + entry.getName());
                    nonImageHtml += link + "<br>";
                }
            }

            if (imageCnt > 1) {
                //Show a  gallery instead
                var newHtml = "";
                newHtml += "<div class=\"row\">\n";
                var columns = parseInt(this.getProperty("columns", "3"));
                var colClass = "col-md-" + (12 / columns);
                for (var i = 0; i < imageEntries.length; i++) {
                    if (i >= columns) {
                        newHtml += "</div><div class=\"row\">\n";
                    }
                    newHtml += "<div class=" + colClass + ">\n";
                    var entry = imageEntries[i];
                    var link = HtmlUtils.tag(TAG_A, [ATTR_HREF, entry.getEntryUrl()], entry.getName());
                    //Don: right now I just replicate what I do above
                    var img = HtmlUtils.image(entry.getResourceUrl(), [ATTR_WIDTH, "100%", ATTR_ID,
                        this.getDomId("entry_" + entry.getIdForDom()),
                        ATTR_ENTRYID, entry.getId(), ATTR_CLASS, "display-entrygallery-entry"
                    ]);
                    img = HtmlUtils.href(entry.getResourceUrl(), img, ["class", "popup_image"]);
                    newHtml += HtmlUtils.div(["class", "image-outer"], HtmlUtils.div(["class", "image-inner"], img) +
                        HtmlUtils.div(["class", "image-caption"], link));

                    newHtml += "</div>\n";
                }
                newHtml += "</div>\n";
                html = newHtml;
            }


            //append the links to the non image entries
            if (nonImageHtml != "") {
                if (imageCnt > 0) {
                    html += "<hr>";
                }
                html += nonImageHtml;
            }
            return html;
        }
    });
}




function RamaddaEntrygridDisplay(displayManager, id, properties) {
    var SUPER;
    var ID_CONTENTS = "contents";
    var ID_GRID = "grid";
    var ID_AXIS_LEFT = "axis_left";
    var ID_AXIS_BOTTOM = "axis_bottom";
    var ID_CANVAS = "canvas";
    var ID_TOP = "top";
    var ID_RIGHT = "right";

    var ID_SETTINGS = "gridsettings";
    var ID_YAXIS_ASCENDING = "yAxisAscending";
    var ID_YAXIS_SCALE = "scaleHeight";
    var ID_XAXIS_ASCENDING = "xAxisAscending";
    var ID_XAXIS_TYPE = "xAxisType";
    var ID_YAXIS_TYPE = "yAxisType";
    var ID_XAXIS_SCALE = "scaleWidth";
    var ID_SHOW_ICON = "showIcon";
    var ID_SHOW_NAME = "showName";
    var ID_COLOR = "boxColor";

    RamaddaUtil.inherit(this, SUPER = new RamaddaEntryDisplay(displayManager, id, DISPLAY_ENTRY_GRID, properties));
    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
        entries: properties.entries,
        initDisplay: function() {
            var _this = this;
            this.createUI();
            var html = HtmlUtils.div([ATTR_ID, this.getDomId(ID_CONTENTS)], this.getLoadingMessage("Loading entries..."));
            this.setContents(html);
            if (!this.entryIds) {
                _this.jq(ID_CONTENTS).html(this.getLoadingMessage("No entries specified"));
                return;
            }
            var props = {
                entries: this.entryIds
            };
            var searchSettings = new EntrySearchSettings(props);
            var jsonUrl = this.getRamadda().getSearchUrl(searchSettings, OUTPUT_JSON, "BAR");
            var myCallback = {
                entryListChanged: function(list) {
                    _this.entries = list.getEntries();
                    if (_this.entries.length == 0) {
                        _this.jq(ID_CONTENTS).html(_this.getLoadingMessage("No entries selected"));
                        return;
                    }
                    _this.drag = null;
                    _this.jq(ID_CONTENTS).html(_this.makeFramework());

                    _this.canvas = $("#" + _this.getDomId(ID_CANVAS));
                    _this.gridPopup = $("#" + _this.getDomId(ID_GRID) + " .display-grid-popup");
                    var debugMouse = false;
                    var xAxis = _this.jq(ID_AXIS_BOTTOM);
                    var yAxis = _this.jq(ID_AXIS_LEFT);
                    var mousedown = function(evt) {
                        if (debugMouse)
                            console.log("mouse down");
                        _this.handledClick = false;
                        _this.drag = {
                            dragging: false,
                            x: GuiUtils.getEventX(evt),
                            y: GuiUtils.getEventY(evt),
                            X: {
                                minDate: _this.axis.X.minDate ? _this.axis.X.minDate : _this.minDate,
                                maxDate: _this.axis.X.maxDate ? _this.axis.X.maxDate : _this.maxDate,
                            },
                            Y: {
                                minDate: _this.axis.Y.minDate ? _this.axis.Y.minDate : _this.minDate,
                                maxDate: _this.axis.Y.maxDate ? _this.axis.Y.maxDate : _this.maxDate,
                            }
                        }
                    }
                    var mouseleave = function(evt) {
                        if (debugMouse)
                            console.log("mouse leave");
                        _this.drag = null;
                        _this.handledClick = false;
                    }
                    var mouseup = function(evt) {
                        if (debugMouse)
                            console.log("mouse up");
                        if (_this.drag) {
                            if (_this.drag.dragging) {
                                if (debugMouse)
                                    console.log("mouse up-was dragging");
                                _this.handledClick = true;
                            }
                            _this.drag = null;
                        }
                    }
                    var mousemove = function(evt, doX, doY) {
                        if (debugMouse)
                            console.log("mouse move");
                        var drag = _this.drag;
                        if (!drag) return;
                        drag.dragging = true;
                        var x = GuiUtils.getEventX(evt);
                        var deltaX = drag.x - x;
                        var y = GuiUtils.getEventY(evt);
                        var deltaY = drag.y - y;
                        var width = $(this).width();
                        var height = $(this).height();
                        var percentX = (x - drag.x) / width;
                        var percentY = (y - drag.y) / height;
                        var ascX = _this.getXAxisAscending();
                        var ascY = _this.getXAxisAscending();
                        var diffX = (drag.X.maxDate.getTime() - drag.X.minDate.getTime()) * percentX;
                        var diffY = (drag.Y.maxDate.getTime() - drag.Y.minDate.getTime()) * percentY;

                        if (doX) {
                            _this.axis.X.minDate = new Date(drag.X.minDate.getTime() + ((ascX ? -1 : 1) * diffX));
                            _this.axis.X.maxDate = new Date(drag.X.maxDate.getTime() + ((ascX ? -1 : 1) * diffX));
                        }
                        if (doY) {
                            _this.axis.Y.minDate = new Date(drag.Y.minDate.getTime() + ((ascY ? 1 : -1) * diffY));
                            _this.axis.Y.maxDate = new Date(drag.Y.maxDate.getTime() + ((ascY ? 1 : -1) * diffY));
                        }
                        _this.makeGrid(_this.entries);
                    }
                    var mouseclick = function(evt, doX, doY) {
                        if (_this.handledClick) {
                            if (debugMouse)
                                console.log("mouse click-other click");
                            _this.handledClick = false;
                            return;
                        }
                        if (_this.drag && _this.drag.dragging) {
                            if (debugMouse)
                                console.log("mouse click-was dragging");
                            _this.drag = null;
                            return;
                        }
                        if (debugMouse)
                            console.log("mouse click");
                        _this.drag = null;
                        var action;
                        if (evt.metaKey || evt.ctrlKey) {
                            action = "reset";
                        } else {
                            var zoomOut = evt.shiftKey;
                            if (zoomOut)
                                action = "zoomout";
                            else
                                action = "zoomin";
                        }
                        _this.doZoom(action, doX, doY);
                    };

                    var mousemoveCanvas = function(evt) {
                        mousemove(evt, true, true);
                    }
                    var mousemoveX = function(evt) {
                        mousemove(evt, true, false);
                    }
                    var mousemoveY = function(evt) {
                        mousemove(evt, false, true);
                    }

                    var mouseclickCanvas = function(evt) {
                        mouseclick(evt, true, true);
                    }
                    var mouseclickX = function(evt) {
                        mouseclick(evt, true, false);
                    }
                    var mouseclickY = function(evt) {
                        mouseclick(evt, false, true);
                    }


                    _this.canvas.mousedown(mousedown);
                    _this.canvas.mouseleave(mouseleave);
                    _this.canvas.mouseup(mouseup);
                    _this.canvas.mousemove(mousemoveCanvas);
                    _this.canvas.click(mouseclickCanvas);

                    xAxis.mousedown(mousedown);
                    xAxis.mouseleave(mouseleave);
                    xAxis.mouseup(mouseup);
                    xAxis.mousemove(mousemoveX);
                    xAxis.click(mouseclickX);

                    yAxis.mousedown(mousedown);
                    yAxis.mouseleave(mouseleave);
                    yAxis.mouseup(mouseup);
                    yAxis.mousemove(mousemoveY);
                    yAxis.click(mouseclickY);

                    var links =
                        HtmlUtils.image(icon_zoom, ["class", "display-grid-action", "title", "reset zoom", "action", "reset"]) +
                        HtmlUtils.image(icon_zoom_in, ["class", "display-grid-action", "title", "zoom in", "action", "zoomin"]) +
                        HtmlUtils.image(icon_zoom_out, ["class", "display-grid-action", "title", "zoom out", "action", "zoomout"]);
                    _this.jq(ID_TOP).html(links);
                    $("#" + _this.getDomId(ID_GRID) + " .display-grid-action").click(function() {
                        var action = $(this).attr("action");
                        _this.doZoom(action);
                    });


                    _this.jq(ID_AXIS_LEFT).html("");
                    _this.jq(ID_AXIS_BOTTOM).html("");
                    _this.makeGrid(_this.entries);
                }
            };
            var entryList = new EntryList(this.getRamadda(), jsonUrl, myCallback, true);
        },
        initDialog: function() {
            SUPER.initDialog.call(this);
            var _this = this;
            var cbx = this.jq(ID_SETTINGS + " :checkbox");
            cbx.click(function() {
                _this.setProperty($(this).attr("attr"), $(this).is(':checked'));
                _this.makeGrid(_this.entries);
            });
            var input = this.jq(ID_SETTINGS + " :input");
            input.blur(function() {
                _this.setProperty($(this).attr("attr"), $(this).val());
                _this.makeGrid(_this.entries);
            });
            input.keypress(function(event) {
                var keycode = (event.keyCode ? event.keyCode : event.which);
                if (keycode == 13) {
                    _this.setProperty($(this).attr("attr"), $(this).val());
                    _this.makeGrid(_this.entries);
                }
            });

        },
        getDialogContents: function(tabTitles, tabContents) {
            var height = "600";
            var html = "";
            html += HtmlUtils.openTag("div", ["id", this.getDomId(ID_SETTINGS)]);

            html += HtmlUtils.formTable();
            html += HtmlUtils.formEntry("",
                HtmlUtils.checkbox(this.getDomId(ID_SHOW_ICON),
                    ["attr", ID_SHOW_ICON],
                    this.getProperty(ID_SHOW_ICON, "true")) + " Show Icon" +
                "&nbsp;&nbsp;" +
                HtmlUtils.checkbox(this.getDomId(ID_SHOW_NAME),
                    ["attr", ID_SHOW_NAME],
                    this.getProperty(ID_SHOW_NAME, "true")) + " Show Name");
            html += HtmlUtils.formEntry("X-Axis:",
                HtmlUtils.checkbox(this.getDomId(ID_XAXIS_ASCENDING),
                    ["attr", ID_XAXIS_ASCENDING],
                    this.getXAxisAscending()) + " Ascending" +
                "&nbsp;&nbsp;" +
                HtmlUtils.checkbox(this.getDomId(ID_XAXIS_SCALE),
                    ["attr", ID_XAXIS_SCALE],
                    this.getXAxisScale()) + " Scale Width");
            html += HtmlUtils.formEntry("Y-Axis:",
                HtmlUtils.checkbox(this.getDomId(ID_YAXIS_ASCENDING),
                    ["attr", ID_YAXIS_ASCENDING],
                    this.getYAxisAscending()) + " Ascending" +
                "&nbsp;&nbsp;" +
                HtmlUtils.checkbox(this.getDomId(ID_YAXIS_SCALE),
                    ["attr", ID_YAXIS_SCALE],
                    this.getYAxisScale()) + " Scale Height");

            html += HtmlUtils.formEntry("Box Color:",
                HtmlUtils.input(this.getDomId(ID_COLOR),
                    this.getProperty(ID_COLOR, "lightblue"),
                    ["attr", ID_COLOR]));

            html += HtmlUtils.formTableClose();
            html += HtmlUtils.closeTag("div");
            tabTitles.push("Entry Grid");
            tabContents.push(html);
            SUPER.getDialogContents.call(this, tabTitles, tabContents);
        },

        doZoom: function(action, doX, doY) {
            if (!Utils.isDefined(doX)) doX = true;
            if (!Utils.isDefined(doY)) doY = true;
            if (action == "reset") {
                this.axis.Y.minDate = null;
                this.axis.Y.maxDate = null;
                this.axis.X.minDate = null;
                this.axis.X.maxDate = null;
            } else {
                var zoomOut = (action == "zoomout");
                if (doX) {
                    var d1 = this.axis.X.minDate.getTime();
                    var d2 = this.axis.X.maxDate.getTime();
                    var dateRange = d2 - d1;
                    var diff = (zoomOut ? 1 : -1) * dateRange * 0.1;
                    this.axis.X.minDate = new Date(d1 - diff);
                    this.axis.X.maxDate = new Date(d2 + diff);
                }
                if (doY) {
                    var d1 = this.axis.Y.minDate.getTime();
                    var d2 = this.axis.Y.maxDate.getTime();
                    var dateRange = d2 - d1;
                    var diff = (zoomOut ? 1 : -1) * dateRange * 0.1;
                    this.axis.Y.minDate = new Date(d1 - diff);
                    this.axis.Y.maxDate = new Date(d2 + diff);
                }
            }
            this.makeGrid(this.entries);
        },
        initGrid: function(entries) {
            var _this = this;
            var items = this.canvas.find(".display-grid-entry");
            items.click(function(evt) {
                var index = parseInt($(this).attr("index"));
                entry = entries[index];
                var url = entry.getEntryUrl();
                if (_this.urlTemplate) {
                    url = _this.urlTemplate.replace("{url}", url).replace(/{entryid}/g, entry.getId()).replace(/{resource}/g, entry.getResourceUrl());
                }

                _this.handledClick = true;
                _this.drag = null;
                window.open(url, "_entry");
                //                        evt.stopPropagation();
            });
            items.mouseout(function() {
                var id = $(this).attr("entryid");
                if (id) {
                    var other = _this.canvas.find("[entryid='" + id + "']");
                    other.each(function() {
                        if ($(this).attr("itemtype") == "box") {
                            $(this).attr("prevcolor", $(this).css("background"));
                            $(this).css("background", $(this).attr("prevcolor"));
                        }
                    });
                }

                _this.gridPopup.hide();
            });
            items.mouseover(function(evt) {
                var id = $(this).attr("entryid");
                if (id) {
                    var other = _this.canvas.find("[entryid='" + id + "']");
                    other.each(function() {
                        if ($(this).attr("itemtype") == "box") {
                            $(this).attr("prevcolor", $(this).css("background"));
                            $(this).css("background", "rgba(0,0,255,0.5)");
                        }
                    });
                }
                var x = GuiUtils.getEventX(evt);
                var index = parseInt($(this).attr("index"));
                entry = entries[index];
                var thumb = entry.getThumbnail();
                var html = "";
                if (thumb) {
                    html = HtmlUtils.image(thumb, ["width", "300;"]) + "<br>";
                } else if (entry.isImage()) {
                    html += HtmlUtils.image(entry.getResourceUrl(), ["width", "300"]) + "<br>";
                }
                html += entry.getIconImage() + " " + entry.getName() + "<br>";
                var start = entry.getStartDate().getUTCFullYear() + "-" + Utils.padLeft(entry.getStartDate().getUTCMonth() + 1, 2, "0") + "-" + Utils.padLeft(entry.getStartDate().getUTCDate(), 2, "0");
                var end = entry.getEndDate().getUTCFullYear() + "-" + Utils.padLeft(entry.getEndDate().getUTCMonth() + 1, 2, "0") + "-" + Utils.padLeft(entry.getEndDate().getUTCDate(), 2, "0");
                html += "Date: " + start + " - " + end + " UTC";
                _this.gridPopup.html(html);
                _this.gridPopup.show();
                _this.gridPopup.position({
                    of: $(this),
                    at: "left bottom",
                    my: "left top",
                    collision: "none none"
                });
                _this.gridPopup.position({
                    of: $(this),
                    my: "left top",
                    at: "left bottom",
                    collision: "none none"
                });
            });
        },
        makeFramework: function(entries) {
            var html = "";
            var mouseInfo = "click:zoom in;shift-click:zoom out;command/ctrl click: reset";
            html += HtmlUtils.openDiv(["class", "display-grid", "id", this.getDomId(ID_GRID)]);
            html += HtmlUtils.div(["class", "display-grid-popup ramadda-popup"], "");
            html += HtmlUtils.openTag("table", ["border", "0", "class", "", "cellspacing", "0", "cellspacing", "0", "width", "100%", "style", "height:100%;"]);
            html += HtmlUtils.openTag("tr", ["valign", "bottom"]);
            html += HtmlUtils.tag("td");
            html += HtmlUtils.tag("td", [], HtmlUtils.div(["id", this.getDomId(ID_TOP)], ""));
            html += HtmlUtils.closeTag("tr");
            html += HtmlUtils.openTag("tr", ["style", "height:100%;"]);
            html += HtmlUtils.openTag("td", ["style", "height:100%;"]);
            html += HtmlUtils.openDiv(["class", "display-grid-axis-left ramadda-noselect", "id", this.getDomId(ID_AXIS_LEFT)]);
            html += HtmlUtils.closeDiv();
            html += HtmlUtils.closeDiv();
            html += HtmlUtils.closeTag("td");
            html += HtmlUtils.openTag("td", ["style", "height:" + this.getProperty("height", "400") + "px"]);
            html += HtmlUtils.openDiv(["class", "display-grid-canvas ramadda-noselect", "id", this.getDomId(ID_CANVAS)]);
            html += HtmlUtils.closeDiv();
            html += HtmlUtils.closeDiv();
            html += HtmlUtils.closeTag("td");
            html += HtmlUtils.closeTag("tr");
            html += HtmlUtils.openTag("tr", []);
            html += HtmlUtils.tag("td", ["width", "100"], "&nbsp;");
            html += HtmlUtils.openTag("td", []);
            html += HtmlUtils.div(["class", "display-grid-axis-bottom ramadda-noselect", "title", mouseInfo, "id", this.getDomId(ID_AXIS_BOTTOM)], "");
            html += HtmlUtils.closeTag("table");
            html += HtmlUtils.closeTag("td");
            return html;
        },


        getXAxisType: function() {
            return this.getProperty(ID_XAXIS_TYPE, "date");
        },
        getYAxisType: function() {
            return this.getProperty(ID_YAXIS_TYPE, "month");
        },
        getXAxisAscending: function() {
            return this.getProperty(ID_XAXIS_ASCENDING, true);
        },
        getYAxisAscending: function() {
            return this.getProperty(ID_YAXIS_ASCENDING, true);
        },
        getXAxisScale: function() {
            return this.getProperty(ID_XAXIS_SCALE, true);
        },
        getYAxisScale: function() {
            return this.getProperty(ID_YAXIS_SCALE, false);
        },


        makeGrid: function(entries) {
            var showIcon = this.getProperty(ID_SHOW_ICON, true);
            var showName = this.getProperty(ID_SHOW_NAME, true);

            if (!this.minDate) {
                var minDate = null;
                var maxDate = null;
                for (var i = 0; i < entries.length; i++) {
                    var entry = entries[i];
                    minDate = minDate == null ? entry.getStartDate() : (minDate.getTime() > entry.getStartDate().getTime() ? entry.getStartDate() : minDate);
                    maxDate = maxDate == null ? entry.getEndDate() : (maxDate.getTime() < entry.getEndDate().getTime() ? entry.getEndDate() : maxDate);
                }
                this.minDate = new Date(Date.UTC(minDate.getUTCFullYear(), 0, 1));
                this.maxDate = new Date(Date.UTC(maxDate.getUTCFullYear() + 1, 0, 1));
            }

            var axis = {
                width: this.canvas.width(),
                height: this.canvas.height(),
                Y: {
                    vertical: true,
                    axisType: this.getYAxisType(),
                    ascending: this.getYAxisAscending(),
                    scale: this.getYAxisScale(),
                    skip: 1,
                    maxTicks: Math.ceil(this.canvas.height() / 80),
                    minDate: this.minDate,
                    maxDate: this.maxDate,
                    ticks: [],
                    lines: "",
                    html: "",
                    minDate: this.minDate,
                    maxDate: this.maxDate,
                },
                X: {
                    vertical: false,
                    axisType: this.getXAxisType(),
                    ascending: this.getXAxisAscending(),
                    scale: this.getXAxisScale(),
                    skip: 1,
                    maxTicks: Math.ceil(this.canvas.width() / 80),
                    minDate: this.minDate,
                    maxDate: this.maxDate,
                    ticks: [],
                    lines: "",
                    html: ""
                }
            }
            if (!this.axis) {
                this.axis = axis;
            } else {
                if (this.axis.X.minDate) {
                    axis.X.minDate = this.axis.X.minDate;
                    axis.X.maxDate = this.axis.X.maxDate;
                } else {
                    this.axis.X.minDate = axis.X.minDate;
                    this.axis.X.maxDate = axis.X.maxDate;
                }
                if (this.axis.Y.minDate) {
                    axis.Y.minDate = this.axis.Y.minDate;
                    axis.Y.maxDate = this.axis.Y.maxDate;
                } else {
                    this.axis.Y.minDate = axis.Y.minDate;
                    this.axis.Y.maxDate = axis.Y.maxDate;
                }
            }

            if (axis.Y.axisType == "size") {
                this.calculateSizeAxis(axis.Y);
            } else if (axis.Y.axisType == "date") {
                this.calculateDateAxis(axis.Y);
            } else {
                this.calculateMonthAxis(axis.Y);
            }
            for (var i = 0; i < axis.Y.ticks.length; i++) {
                var tick = axis.Y.ticks[i];
                var style = (axis.Y.ascending ? "bottom:" : "top:") + tick.percent + "%;";
                var style = "bottom:" + tick.percent + "%;";
                var lineClass = tick.major ? "display-grid-hline-major" : "display-grid-hline";
                axis.Y.lines += HtmlUtils.div(["style", style, "class", lineClass], " ");
                axis.Y.html += HtmlUtils.div(["style", style, "class", "display-grid-axis-left-tick"], tick.label + " " + HtmlUtils.div(["class", "display-grid-htick"], ""));
            }

            if (axis.X.axisType == "size") {
                this.calculateSizeAxis(axis.X);
            } else if (axis.X.axisType == "date") {
                this.calculateDateAxis(axis.X);
            } else {
                this.calculateMonthAxis(axis.X);
            }
            for (var i = 0; i < axis.X.ticks.length; i++) {
                var tick = axis.X.ticks[i];
                if (tick.percent > 0) {
                    var lineClass = tick.major ? "display-grid-vline-major" : "display-grid-vline";
                    axis.X.lines += HtmlUtils.div(["style", "left:" + tick.percent + "%;", "class", lineClass], " ");
                }
                axis.X.html += HtmlUtils.div(["style", "left:" + tick.percent + "%;", "class", "display-grid-axis-bottom-tick"], HtmlUtils.div(["class", "display-grid-vtick"], "") + " " + tick.label);
            }

            var items = "";
            var seen = {};
            for (var i = 0; i < entries.length; i++) {
                var entry = entries[i];
                var vInfo = this[axis.Y.calculatePercent].call(this, entry, axis.Y);
                var xInfo = this[axis.X.calculatePercent].call(this, entry, axis.X);
                if (vInfo.p1 < 0) {
                    vInfo.p2 = vInfo.p2 + vInfo.p1;
                    vInfo.p1 = 0;
                }
                if (vInfo.p1 + vInfo.p2 > 100) {
                    vInfo.p2 = 100 - vInfo.p1;
                }

                var style = "";
                var pos = "";

                if (axis.X.ascending) {
                    style += "left:" + xInfo.p1 + "%;";
                    pos += "left:" + xInfo.p1 + "%;";
                } else {
                    style += "right:" + xInfo.p1 + "%;";
                    pos += "left:" + (100 - xInfo.p2) + "%;";
                }

                if (axis.X.scale) {
                    if (xInfo.delta > 1) {
                        style += "width:" + xInfo.delta + "%;";
                    } else {
                        style += "width:" + this.getProperty("fixedWidth", "5") + "px;";
                    }
                }


                var namePos = pos;
                if (axis.Y.ascending) {
                    style += " bottom:" + vInfo.p2 + "%;";
                    pos += " bottom:" + vInfo.p2 + "%;";
                    namePos += " bottom:" + vInfo.p2 + "%;";
                } else {
                    style += " top:" + vInfo.p2 + "%;";
                    pos += " top:" + vInfo.p2 + "%;";
                    namePos += " top:" + vInfo.p2 + "%;";
                    namePos += "margin-top:-15px;"
                }
                if (axis.Y.scale) {
                    if (vInfo.p2 > 1) {
                        style += "height:" + vInfo.delta + "%;";
                    } else {
                        style += "height:" + this.getProperty("fixedHeight", "5") + "px;";
                    }
                }

                if (entry.getName().includes("rilsd")) {
                    console.log("pos:" + namePos);
                }
                if (showIcon) {
                    items += HtmlUtils.div(["class", "display-grid-entry-icon display-grid-entry", "entryid", entry.getId(), "index", i, "style", pos], entry.getIconImage());
                }
                var key = Math.round(xInfo.p1) + "---" + Math.round(vInfo.p1);
                if (showName && !seen[key]) {
                    seen[key] = true;
                    var name = entry.getName().replace(/ /g, "&nbsp;");
                    items += HtmlUtils.div(["class", "display-grid-entry-text display-grid-entry", "entryid", entry.getId(), "index", i, "style", namePos], name);
                }
                var boxStyle = style + "background:" + this.getProperty(ID_COLOR, "lightblue");
                items += HtmlUtils.div(["class", "display-grid-entry-box display-grid-entry", "itemtype", "box", "entryid", entry.getId(), "style", boxStyle, "index", i], "");
            }
            this.jq(ID_AXIS_LEFT).html(axis.Y.html);
            this.jq(ID_CANVAS).html(axis.Y.lines + axis.X.lines + items);
            this.jq(ID_AXIS_BOTTOM).html(axis.X.html);
            this.initGrid(entries);
        },
        calculateSizeAxis: function(axisInfo) {
            var min = Number.MAX_VALUE;
            var max = Number.MIN_VALUE;
            for (var i = 0; i < this.entries.length; i++) {
                var entry = this.entries[i];
                min = Math.min(min, entry.getSize());
                max = Math.max(max, entry.getSize());
            }
        },
        checkOrder: function(axisInfo, percents) {
            /*
            if(!axisInfo.ascending) {
                percents.p1 = 100-percents.p1;
                percents.p2 = 100-percents.p2;
                var tmp  =percents.p1;
                percents.p1=percents.p2;
                percents.p2=tmp;
            }
            */
            return {
                p1: percents.p1,
                p2: percents.p2,
                delta: Math.abs(percents.p2 - percents.p1)
            };
        },
        calculateDatePercent: function(entry, axisInfo) {
            var p1 = 100 * (entry.getStartDate().getTime() - axisInfo.min) / axisInfo.range;
            var p2 = 100 * (entry.getEndDate().getTime() - axisInfo.min) / axisInfo.range;
            return this.checkOrder(axisInfo, {
                p1: p1,
                p2: p2,
                delta: Math.abs(p2 - p1)
            });
        },
        calculateMonthPercent: function(entry, axisInfo) {
            var d1 = entry.getStartDate();
            var d2 = entry.getEndDate();
            var t1 = new Date(Date.UTC(1, d1.getUTCMonth(), d1.getUTCDate()));
            var t2 = new Date(Date.UTC(1, d2.getUTCMonth(), d2.getUTCDate()));
            var p1 = 100 * ((t1.getTime() - axisInfo.min) / axisInfo.range);
            var p2 = 100 * ((t2.getTime() - axisInfo.min) / axisInfo.range);
            if (entry.getName().includes("rilsd")) {
                console.log("t1:" + t1);
                console.log("t2:" + t2);
                console.log("before:" + p1 + " " + p2);
            }
            return this.checkOrder(axisInfo, {
                p1: p1,
                p2: p2,
                delta: Math.abs(p2 - p1)
            });
        },
        calculateMonthAxis: function(axisInfo) {
            axisInfo.calculatePercent = "calculateMonthPercent";
            axisInfo.minDate = new Date(Date.UTC(0, 11, 15));
            axisInfo.maxDate = new Date(Date.UTC(1, 11, 31));
            axisInfo.min = axisInfo.minDate.getTime();
            axisInfo.max = axisInfo.maxDate.getTime();
            axisInfo.range = axisInfo.max - axisInfo.min;
            var months = Utils.getMonthShortNames();
            for (var month = 0; month < months.length; month++) {
                var t1 = new Date(Date.UTC(1, month));
                var percent = (axisInfo.maxDate.getTime() - t1.getTime()) / axisInfo.range;
                if (axisInfo.ascending)
                    percent = 1 - percent;
                axisInfo.ticks.push({
                    percent: 100 * percent,
                    label: months[month],
                    major: false
                });
            }
        },
        calculateDateAxis: function(axisInfo) {
            axisInfo.calculatePercent = "calculateDatePercent";
            var numYears = axisInfo.maxDate.getUTCFullYear() - axisInfo.minDate.getUTCFullYear();
            var years = numYears;
            axisInfo.type = "year";
            axisInfo.skip = Math.max(1, Math.floor(numYears / axisInfo.maxTicks));
            if ((numYears / axisInfo.skip) <= (axisInfo.maxTicks / 2)) {
                var numMonths = 0;
                var tmp = new Date(axisInfo.minDate.getTime());
                while (tmp.getTime() < axisInfo.maxDate.getTime()) {
                    Utils.incrementMonth(tmp);
                    numMonths++;
                }
                axisInfo.skip = Math.max(1, Math.floor(numMonths / axisInfo.maxTicks));
                axisInfo.type = "month";
                if ((numMonths / axisInfo.skip) <= (axisInfo.maxTicks / 2)) {
                    var tmp = new Date(axisInfo.minDate.getTime());
                    var numDays = 0;
                    while (tmp.getTime() < axisInfo.maxDate.getTime()) {
                        Utils.incrementDay(tmp);
                        numDays++;
                    }
                    axisInfo.skip = Math.max(1, Math.floor(numDays / axisInfo.maxTicks));
                    axisInfo.type = "day";
                }
            }


            axisInfo.min = axisInfo.minDate.getTime();
            axisInfo.max = axisInfo.maxDate.getTime();
            axisInfo.range = axisInfo.max - axisInfo.min;
            var months = Utils.getMonthShortNames();
            var lastYear = null;
            var lastMonth = null;
            var tickDate;
            if (axisInfo.type == "year") {
                tickDate = new Date(Date.UTC(axisInfo.minDate.getUTCFullYear()));
            } else if (axisInfo.type == "month") {
                tickDate = new Date(Date.UTC(axisInfo.minDate.getUTCFullYear(), axisInfo.minDate.getUTCMonth()));
            } else {
                tickDate = new Date(Date.UTC(axisInfo.minDate.getUTCFullYear(), axisInfo.minDate.getUTCMonth(), axisInfo.minDate.getUTCDate()));
            }
            //                if(axisInfo.vertical)
            //                    console.log(axisInfo.type+" skip:" + axisInfo.skip + "   min:" + Utils.formatDateYYYYMMDD(axisInfo.minDate)+"   max:" + Utils.formatDateYYYYMMDD(axisInfo.maxDate));
            while (tickDate.getTime() < axisInfo.maxDate.getTime()) {
                var percent = (tickDate.getTime() - axisInfo.minDate.getTime()) / axisInfo.range;
                if (!axisInfo.ascending)
                    percent = (1 - percent);
                percent = 100 * percent;
                //                    console.log("    perc:"+ percent +" " + Utils.formatDateYYYYMMDD(tickDate));
                if (percent >= 0 && percent < 100) {
                    var label = "";
                    var year = tickDate.getUTCFullYear();
                    var month = tickDate.getUTCMonth();
                    var major = false;
                    if (axisInfo.type == "year") {
                        label = year;
                    } else if (axisInfo.type == "month") {
                        label = months[tickDate.getUTCMonth()];
                        if (lastYear != year) {
                            label = label + "<br>" + year;
                            lastYear = year;
                            major = true;
                        }
                    } else {
                        label = tickDate.getUTCDate();
                        if (lastYear != year || lastMonth != month) {
                            label = label + "<br>" + months[month] + " " + year;
                            lastYear = year;
                            lastMonth = month;
                            major = true;
                        }
                    }
                    axisInfo.ticks.push({
                        percent: percent,
                        label: label,
                        major: major
                    });
                }
                if (axisInfo.type == "year") {
                    Utils.incrementYear(tickDate, axisInfo.skip);
                } else if (axisInfo.type == "month") {
                    Utils.incrementMonth(tickDate, axisInfo.skip);
                } else {
                    Utils.incrementDay(tickDate, axisInfo.skip);
                }
            }

        }
    });
}


function RamaddaMetadataDisplay(displayManager, id, properties) {
    if (properties.formOpen == null) {
        properties.formOpen = false;
    }
    var SUPER;
    RamaddaUtil.inherit(this, SUPER = new RamaddaSearcher(displayManager, id, DISPLAY_METADATA, properties));
    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
        haveDisplayed: false,
        initDisplay: function() {
            this.createUI();
            this.setContents(this.getDefaultHtml());
            SUPER.initDisplay.apply(this);
            if (this.haveDisplayed && this.entryList) {
                this.entryListChanged(this.entryList);
            }
            this.haveDisplayed = true;
        },
        entryListChanged: function(entryList) {
            this.entryList = entryList;
            var entries = this.entryList.getEntries();
            if (entries.length == 0) {
                this.writeHtml(ID_ENTRIES, "Nothing found");
                this.writeHtml(ID_RESULTS, "&nbsp;");
                return;
            }
            var mdtsFromEntries = [];
            var mdtmap = {};
            var tmp = {};
            for (var i = 0; i < entries.length; i++) {
                var entry = entries[i];
                var metadata = entry.getMetadata();
                for (var j = 0; j < metadata.length; j++) {
                    var m = metadata[j];
                    if (tmp[m.type] == null) {
                        tmp[m.type] = "";
                        mdtsFromEntries.push(m.type);
                    }
                    mdtmap[metadata[j].type] = metadata[j].label;
                }
            }

            var html = "";
            html += HtmlUtils.openTag(TAG_TABLE, ["id", this.getDomId("table"), ATTR_CLASS, "cell-border stripe ramadda-table", ATTR_WIDTH, "100%", "cellpadding", "5", "cellspacing", "0"]);
            html += "<thead>"
            var type = this.findEntryType(this.searchSettings.entryType);
            var typeName = "Entry";
            if (type != null) {
                typeName = type.getLabel();
            }
            this.writeHtml(ID_RESULTS, this.getResultsHeader(entries));



            var mdts = null;
            //Get the metadata types to show from either a property or
            //gather them from all of the entries
            // e.g., "project_pi,project_person,project_funding"
            var prop = this.getProperty("metadataTypes", null);
            if (prop != null) {
                mdts = prop.split(",");
            } else {
                mdts = mdtsFromEntries;
                mdts.sort();
            }

            var skip = {
                "content.pagestyle": true,
                "content.pagetemplate": true,
                "content.sort": true,
                "spatial.polygon": true,
            };
            var headerItems = [];
            headerItems.push(HtmlUtils.th([], HtmlUtils.b(typeName)));
            for (var i = 0; i < mdts.length; i++) {
                var type = mdts[i];
                if (skip[type]) {
                    continue;
                }
                var label = mdtmap[mdts[i]];
                if (label == null) label = mdts[i];
                headerItems.push(HtmlUtils.th([], HtmlUtils.b(label)));
            }
            var headerRow = HtmlUtils.tr(["valign", "bottom"], HtmlUtils.join(headerItems, ""));
            html += headerRow;
            html += "</thead><tbody>"
            var divider = "<div class=display-metadata-divider></div>";
            var missing = this.missingMessage;
            if (missing = null) missing = "&nbsp;";
            for (var entryIdx = 0; entryIdx < entries.length; entryIdx++) {
                var entry = entries[entryIdx];
                var metadata = entry.getMetadata();
                var row = [];
                var buttonId = this.getDomId("entrylink" + entry.getIdForDom());
                var link = entry.getLink(entry.getIconImage() + " " + entry.getName());
                row.push(HtmlUtils.td([], HtmlUtils.div([ATTR_CLASS, "display-metadata-entrylink"], link)));
                for (var mdtIdx = 0; mdtIdx < mdts.length; mdtIdx++) {
                    var mdt = mdts[mdtIdx];
                    if (skip[mdt]) {
                        continue;
                    }
                    var cell = null;
                    for (var j = 0; j < metadata.length; j++) {
                        var m = metadata[j];
                        if (m.type == mdt) {
                            var item = null;
                            if (m.type == "content.thumbnail" || m.type == "content.logo") {
                                var url = this.getRamadda().getRoot() + "/metadata/view/" + m.value.attr1 + "?element=1&entryid=" + entry.getId() + "&metadata_id=" + m.id;
                                item = HtmlUtils.image(url, [ATTR_WIDTH, "100"]);
                            } else if (m.type == "content.url" || m.type == "dif.related_url") {
                                var label = m.value.attr2;
                                if (label == null || label == "") {
                                    label = m.value.attr1;
                                }
                                item = HtmlUtils.href(m.value.attr1, label);
                            } else if (m.type == "content.attachment") {
                                var toks = m.value.attr1.split("_file_");
                                var filename = toks[1];
                                var url = this.getRamadda().getRoot() + "/metadata/view/" + m.value.attr1 + "?element=1&entryid=" + entry.getId() + "&metadata_id=" + m.id;
                                item = HtmlUtils.href(url, filename);
                            } else {
                                item = m.value.attr1;
                                //                                    console.log("Item:" + item);
                                if (m.value.attr2 && m.value.attr2.trim().length > 0) {
                                    item += " - " + m.value.attr2;
                                }
                            }
                            if (item != null) {
                                if (cell == null) {
                                    cell = "";
                                } else {
                                    cell += divider;
                                }
                                cell += HtmlUtils.div([ATTR_CLASS, "display-metadata-item"], item);
                            }

                        }
                    }
                    if (cell == null) {
                        cell = missing;
                    }
                    if (cell == null) {
                        cell = "";
                    }
                    var add = HtmlUtils.tag(TAG_A, [ATTR_STYLE, "color:#000;", ATTR_HREF, this.getRamadda().getRoot() + "/metadata/addform?entryid=" + entry.getId() + "&metadata_type=" + mdt,
                        "target", "_blank", "alt", "Add metadata", ATTR_TITLE, "Add metadata"
                    ], "+");
                    add = HtmlUtils.div(["class", "display-metadata-table-add"], add);
                    var cellContents = add + divider;
                    if (cell.length > 0) {
                        cellContents += cell;
                    }
                    row.push(HtmlUtils.td([], HtmlUtils.div([ATTR_CLASS, "display-metadata-table-cell-contents"], cellContents)));
                }
                html += HtmlUtils.tr(["valign", "top"], HtmlUtils.join(row, ""));
                //Add in the header every 10 rows
                if (((entryIdx + 1) % 10) == 0) html += headerRow;
            }
            html += "</tbody>"
            html += HtmlUtils.closeTag(TAG_TABLE);
            this.jq(ID_ENTRIES).html(html);
            HtmlUtils.formatTable("#" + this.getDomId("table"), {
                scrollY: 400
            });
        },
    });

}



function RamaddaTimelineDisplay(displayManager, id, properties) {
    if (properties.formOpen == null) {
        properties.formOpen = false;
    }
    var SUPER;
    RamaddaUtil.inherit(this, SUPER = new RamaddaSearcher(displayManager, id, DISPLAY_TIMELINE, properties));
    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
        initDisplay: function() {
            this.createUI();
            this.setContents(this.getDefaultHtml());
            SUPER.initDisplay.apply(this);
        },
        entryListChanged: function(entryList) {
            this.entryList = entryList;
            var entries = this.entryList.getEntries();
            var html = "";
            if (entries.length == 0) {
                this.writeHtml(ID_ENTRIES, "Nothing found");
                this.writeHtml(ID_RESULTS, "&nbsp;");
                return;
            }

            var data = {
                "timeline": {
                    "headline": "The Main Timeline Headline Goes here",
                    "type": "default",
                    "text": "<p>Intro body text goes here, some HTML is ok</p>",
                    "asset": {
                        "media": "http://yourdomain_or_socialmedialink_goes_here.jpg",
                        "credit": "Credit Name Goes Here",
                        "caption": "Caption text goes here"
                    },
                    "date": [{
                            "startDate": "2011,12,10",
                            "endDate": "2011,12,11",
                            "headline": "Headline Goes Here",
                            "text": "<p>Body text goes here, some HTML is OK</p>",
                            "tag": "This is Optional",
                            "classname": "optionaluniqueclassnamecanbeaddedhere",
                            "asset": {
                                "media": "http://twitter.com/ArjunaSoriano/status/164181156147900416",
                                "thumbnail": "optional-32x32px.jpg",
                                "credit": "Credit Name Goes Here",
                                "caption": "Caption text goes here"
                            }
                        }, {
                            "startDate": "2012,12,10",
                            "endDate": "2012,12,11",
                            "headline": "Headline Goes Here",
                            "text": "<p>Body text goes here, some HTML is OK</p>",
                            "tag": "This is Optional",
                            "classname": "optionaluniqueclassnamecanbeaddedhere",
                            "asset": {
                                "media": "http://twitter.com/ArjunaSoriano/status/164181156147900416",
                                "thumbnail": "optional-32x32px.jpg",
                                "credit": "Credit Name Goes Here",
                                "caption": "Caption text goes here"
                            }
                        }, {
                            "startDate": "2013,12,10",
                            "endDate": "2013,12,11",
                            "headline": "Headline Goes Here",
                            "text": "<p>Body text goes here, some HTML is OK</p>",
                            "tag": "This is Optional",
                            "classname": "optionaluniqueclassnamecanbeaddedhere",
                            "asset": {
                                "media": "http://twitter.com/ArjunaSoriano/status/164181156147900416",
                                "thumbnail": "optional-32x32px.jpg",
                                "credit": "Credit Name Goes Here",
                                "caption": "Caption text goes here"
                            }
                        }

                    ],
                    "era": [{
                            "startDate": "2011,12,10",
                            "endDate": "2011,12,11",
                            "headline": "Headline Goes Here",
                            "text": "<p>Body text goes here, some HTML is OK</p>",
                            "tag": "This is Optional"
                        }

                    ]
                }
            };


            for (var i = 0; i < entries.length; i++) {
                var entry = entries[i];

            }
            createStoryJS({
                type: 'timeline',
                width: '800',
                height: '600',
                source: data,
                embed_id: this.getDomId(ID_ENTRIES),
            });

        },
    });

}







function RamaddaEntrydisplayDisplay(displayManager, id, properties) {
    var SUPER;
    var e = new Error();

    $.extend(this, {
        sourceEntry: properties.sourceEntry
    });
    RamaddaUtil.inherit(this, SUPER = new RamaddaDisplay(displayManager, id, DISPLAY_ENTRYDISPLAY, properties));
    if (properties.sourceEntry == null && properties.entryId != null) {
        var _this = this;
        var f = async function() {
            await _this.getEntry(properties.entryId, entry => {
                _this.sourceEntry = entry;
                _this.initDisplay()
            });

        }
        f();
    }


    addRamaddaDisplay(this);
    $.extend(this, {
        selectedEntry: null,
        initDisplay: function() {
            this.createUI();
            var title = this.title;
            if (this.sourceEntry != null) {
                this.addEntryHtml(this.sourceEntry);
                var url = this.sourceEntry.getEntryUrl();

                if (title == null) {
                    title = this.sourceEntry.getName();
                }
                title = HtmlUtils.tag("a", ["href", url, "title", this.sourceEntry.getName(), "alt", this.sourceEntry.getName()], title);
            } else {
                this.addEntryHtml(this.selectedEntry);
                if (title == null) {
                    title = "Entry Display";
                }
            }
            this.setDisplayTitle(title);
        },
        handleEventEntrySelection: function(source, args) {
            //Ignore select events
            if (this.sourceEntry != null) return;
            var selected = args.selected;
            var entry = args.entry;
            if (!selected) {
                if (this.selectedEntry != entry) {
                    //not mine
                    return;
                }
                this.selectedEntry = null;
                this.setContents("");
                return;
            }
            this.selectedEntry = entry;
            this.addEntryHtml(this.selectedEntry);
        },
        getEntries: function() {
            return [this.sourceEntry];
        },
        addEntryHtml: function(entry) {
            if (entry == null) {
                this.setContents("&nbsp;");
                return;
            }
            var html = this.getEntryHtml(entry, {
                showHeader: false
            });
            var height = this.getProperty("height", "400px");
            if (!height.endsWith("px")) height += "px";
            this.setContents(HtmlUtils.div(["class", "display-entry-description", "style", "height:" + height + ";"],
                html));
            this.entryHtmlHasBeenDisplayed(entry);
        },
    });
}





function RamaddaOperandsDisplay(displayManager, id, properties) {
    var ID_SELECT = TAG_SELECT;
    var ID_SELECT1 = "select1";
    var ID_SELECT2 = "select2";
    var ID_NEWDISPLAY = "newdisplay";

    $.extend(this, new RamaddaEntryDisplay(displayManager, id, DISPLAY_OPERANDS, properties));
    addRamaddaDisplay(this);
    $.extend(this, {
        baseUrl: null,
        initDisplay: function() {
            this.createUI();
            this.baseUrl = this.getRamadda().getSearchUrl(this.searchSettings, OUTPUT_JSON);
            if (this.entryList == null) {
                this.entryList = new EntryList(this.getRamadda(), jsonUrl, this);
            }
            var html = "";
            html += HtmlUtils.div([ATTR_ID, this.getDomId(ID_ENTRIES), ATTR_CLASS, this.getClass("entries")], "");
            this.setContents(html);
        },
        entryListChanged: function(entryList) {
            var html = "<form>";
            html += "<p>";
            html += HtmlUtils.openTag(TAG_TABLE, [ATTR_CLASS, "formtable", "cellspacing", "0", "cellspacing", "0"]);
            var entries = this.entryList.getEntries();
            var get = this.getGet();

            for (var j = 1; j <= 2; j++) {
                var select = HtmlUtils.openTag(TAG_SELECT, [ATTR_ID, this.getDomId(ID_SELECT + j)]);
                select += HtmlUtils.tag(TAG_OPTION, [ATTR_TITLE, "", ATTR_VALUE, ""],
                    "-- Select --");
                for (var i = 0; i < entries.length; i++) {
                    var entry = entries[i];
                    var label = entry.getIconImage() + " " + entry.getName();
                    select += HtmlUtils.tag(TAG_OPTION, [ATTR_TITLE, entry.getName(), ATTR_VALUE, entry.getId()],
                        entry.getName());

                }
                select += HtmlUtils.closeTag(TAG_SELECT);
                html += HtmlUtils.formEntry("Data:", select);
            }

            var select = HtmlUtils.openTag(TAG_SELECT, [ATTR_ID, this.getDomId(ID_CHARTTYPE)]);
            select += HtmlUtils.tag(TAG_OPTION, [ATTR_TITLE, "", ATTR_VALUE, "linechart"],
                "Line chart");
            select += HtmlUtils.tag(TAG_OPTION, [ATTR_TITLE, "", ATTR_VALUE, "barchart"],
                "Bar chart");
            select += HtmlUtils.tag(TAG_OPTION, [ATTR_TITLE, "", ATTR_VALUE, "barstack"],
                "Stacked bars");
            select += HtmlUtils.tag(TAG_OPTION, [ATTR_TITLE, "", ATTR_VALUE, "bartable"],
                "Bar table");
            select += HtmlUtils.tag(TAG_OPTION, [ATTR_TITLE, "", ATTR_VALUE, "piechart"],
                "Pie chart");
            select += HtmlUtils.tag(TAG_OPTION, [ATTR_TITLE, "", ATTR_VALUE, "scatterplot"],
                "Scatter Plot");
            select += HtmlUtils.closeTag(TAG_SELECT);
            html += HtmlUtils.formEntry("Chart Type:", select);

            html += HtmlUtils.closeTag(TAG_TABLE);
            html += "<p>";
            html += HtmlUtils.tag(TAG_DIV, [ATTR_CLASS, "display-button", ATTR_ID, this.getDomId(ID_NEWDISPLAY)], "New Chart");
            html += "<p>";
            html += "</form>";
            this.writeHtml(ID_ENTRIES, html);
            var theDisplay = this;
            this.jq(ID_NEWDISPLAY).button().click(function(event) {
                theDisplay.createDisplay();
            });
        },
        createDisplay: function() {
            var entry1 = this.getEntry(this.jq(ID_SELECT1).val());
            var entry2 = this.getEntry(this.jq(ID_SELECT2).val());
            if (entry1 == null) {
                alert("No data selected");
                return;
            }
            var pointDataList = [];

            pointDataList.push(new PointData(entry1.getName(), null, null, ramaddaBaseUrl + "/entry/show?&output=points.product&product=points.json&numpoints=1000&entryid=" + entry1.getId()));
            if (entry2 != null) {
                pointDataList.push(new PointData(entry2.getName(), null, null, ramaddaBaseUrl + "/entry/show?&output=points.product&product=points.json&numpoints=1000&entryid=" + entry2.getId()));
            }

            //Make up some functions
            var operation = "average";
            var derivedData = new DerivedPointData(this.displayManager, "Derived Data", pointDataList, operation);
            var pointData = derivedData;
            var chartType = this.jq(ID_CHARTTYPE).val();
            displayManager.createDisplay(chartType, {
                "layoutFixed": false,
                "data": pointData
            });
        }

    });
}


function RamaddaRepositoriesDisplay(displayManager, id, properties) {
    RamaddaUtil.inherit(this, new RamaddaEntryDisplay(displayManager, id, DISPLAY_REPOSITORIES, properties));
    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
        initDisplay: function() {
            var theDisplay = this;
            this.createUI();
            var html = "";
            if (this.ramaddas.length == 0) {
                html += this.getMessage("No repositories specified");
            } else {
                html += this.getMessage("Loading repository listing");
            }
            this.numberWithTypes = 0;
            this.finishedInitDisplay = false;
            //Check for and remove the all repositories
            if (this.ramaddas.length > 1) {
                if (this.ramaddas[this.ramaddas.length - 1].getRoot() == "all") {
                    this.ramaddas.splice(this.ramaddas.length - 1, 1);
                }
            }
            for (var i = 0; i < this.ramaddas.length; i++) {
                if (i == 0) {}
                var ramadda = this.ramaddas[i];
                var types = ramadda.getEntryTypes(function(ramadda, types) {
                    theDisplay.gotTypes(ramadda, types);
                });
                if (types != null) {
                    this.numberWithTypes++;
                }
            }
            this.setDisplayTitle("Repositories");
            this.setContents(html);
            this.finishedInitDisplay = true;
            this.displayRepositories();
        },
        displayRepositories: function() {
            //                console.log("displayRepositories " + this.numberWithTypes + " " + this.ramaddas.length);
            if (!this.finishedInitDisplay || this.numberWithTypes != this.ramaddas.length) {
                return;
            }
            var typeMap = {};
            var allTypes = [];
            var html = "";
            html += HtmlUtils.openTag(TAG_TABLE, [ATTR_CLASS, "display-repositories-table", ATTR_WIDTH, "100%", ATTR_BORDER, "1", "cellspacing", "0", "cellpadding", "5"]);
            for (var i = 0; i < this.ramaddas.length; i++) {
                var ramadda = this.ramaddas[i];
                var types = ramadda.getEntryTypes();
                for (var typeIdx = 0; typeIdx < types.length; typeIdx++) {
                    var type = types[typeIdx];
                    if (typeMap[type.getId()] == null) {
                        typeMap[type.getId()] = type;
                        allTypes.push(type);
                    }
                }
            }

            html += HtmlUtils.openTag(TAG_TR, ["valign", "bottom"]);
            html += HtmlUtils.th([ATTR_CLASS, "display-repositories-table-header"], "Type");
            for (var i = 0; i < this.ramaddas.length; i++) {
                var ramadda = this.ramaddas[i];
                var link = HtmlUtils.href(ramadda.getRoot(), ramadda.getName());
                html += HtmlUtils.th([ATTR_CLASS, "display-repositories-table-header"], link);
            }
            html += "</tr>";

            var onlyCats = [];
            if (this.categories != null) {
                onlyCats = this.categories.split(",");
            }



            var catMap = {};
            var cats = [];
            for (var typeIdx = 0; typeIdx < allTypes.length; typeIdx++) {
                var type = allTypes[typeIdx];
                var row = "";
                row += "<tr>";
                row += HtmlUtils.td([], HtmlUtils.image(type.getIcon()) + " " + type.getLabel());
                for (var i = 0; i < this.ramaddas.length; i++) {
                    var ramadda = this.ramaddas[i];
                    var repoType = ramadda.getEntryType(type.getId());
                    var col = "";
                    if (repoType == null) {
                        row += HtmlUtils.td([ATTR_CLASS, "display-repositories-table-type-hasnot"], "");
                    } else {
                        var label =
                            HtmlUtils.tag(TAG_A, ["href", ramadda.getRoot() + "/search/type/" + repoType.getId(), "target", "_blank"],
                                repoType.getEntryCount());
                        row += HtmlUtils.td([ATTR_ALIGN, "right", ATTR_CLASS, "display-repositories-table-type-has"], label);
                    }

                }
                row += "</tr>";

                var catRows = catMap[type.getCategory()];
                if (catRows == null) {
                    catRows = [];
                    catMap[type.getCategory()] = catRows;
                    cats.push(type.getCategory());
                }
                catRows.push(row);
            }

            for (var i = 0; i < cats.length; i++) {
                var cat = cats[i];
                if (onlyCats.length > 0) {
                    var ok = false;
                    for (var patternIdx = 0; patternIdx < onlyCats.length; patternIdx++) {
                        if (cat == onlyCats[patternIdx]) {
                            ok = true;
                            break;
                        }
                        if (cat.match(onlyCats[patternIdx])) {
                            ok = true;
                            break;

                        }
                    }
                    if (!ok) continue;

                }
                var rows = catMap[cat];
                html += "<tr>";
                html += HtmlUtils.th(["colspan", "" + (1 + this.ramaddas.length)], cat);
                html += "</tr>";
                for (var row = 0; row < rows.length; row++) {
                    html += rows[row];
                }

            }


            html += HtmlUtils.closeTag(HtmlUtils.TAG_TABLE);
            this.setContents(html);
        },
        gotTypes: function(ramadda, types) {
            this.numberWithTypes++;
            this.displayRepositories();
        }
    });
}


var RamaddaGalleryDisplay = RamaddaEntrygalleryDisplay;/**
Copyright 2008-2019 Geode Systems LLC
*/


//uncomment this to add this type to the global list
//addGlobalDisplayType({type: "example", label:"Example"});


/*
  This gets created by the displayManager.createDisplay('example')
 */
function RamaddaExampleDisplay(displayManager, id, properties) {

    //Dom id for example
    //The displays use display.getDomId(ID_CLICK) to get a unique (based on the display id) id
    var ID_CLICK = "click";

    var ID_DATA = "data";

    //Create the base class
    RamaddaUtil.inherit(this, new RamaddaDisplay(displayManager, id, "example", properties));

    //Add this display to the list of global displays
    addRamaddaDisplay(this);

    //Define the methods
    RamaddaUtil.defineMembers(this, {
        //gets called by displaymanager after the displays are layed out
        initDisplay: function() {
            //Call base class to init menu, etc
            this.createUI();

            //I've been calling back to this display with the following
            //this returns "getRamaddaDisplay('" + this.getId() +"')";
            var get = this.getGet();
            var html = "<p>";
            html += HtmlUtils.onClick(get + ".click();", HtmlUtils.div([ATTR_ID, this.getDomId(ID_CLICK)], "Click me"));
            html += "<p>";
            html += HtmlUtils.div([ATTR_ID, this.getDomId(ID_DATA)], "");

            //Set the contents
            this.setContents(html);

            //Add the data
            this.updateUI();
        },
        //this tells the base display class to loadInitialData
        needsData: function() {
            return true;
        },
        //this gets called after the data has been loaded
        updateUI: function() {
            var pointData = this.getData();
            if (pointData == null) return;
            var recordFields = pointData.getRecordFields();
            var records = pointData.getRecords();
            var html = "";
            html += "#records:" + records.length;
            //equivalent to:
            //$("#" + this.getDomId(ID_DATA)).html(html);
            this.jq(ID_DATA).html(html);
        },
        //this gets called when an event source has selected a record
        handleEventRecordSelection: function(source, args) {
            //args: index, record, html
            //this.setContents(args.html);
        },
        click: function() {
            this.jq(ID_CLICK).html("Click again");
        }
    });
}/**
Copyright 2008-2019 Geode Systems LLC
*/


//Properties
var PROP_LAYOUT_TYPE = "layoutType";
var PROP_LAYOUT_COLUMNS = "layoutColumns";
var PROP_SHOW_MAP = "showMap";
var PROP_SHOW_MENU = "showMenu";
var PROP_FROMDATE = "fromDate";
var PROP_TODATE = "toDate";

var DISPLAY_MULTI = "multi";
addGlobalDisplayType({
    type: DISPLAY_MULTI,
    label: "Multi Chart",
    requiresData: true,
    forUser: false,
    category: "Misc"
});



//
//adds the display manager to the list of global display managers
//
function addDisplayManager(displayManager) {
    if (window.globalDisplayManagers == null) {
        window.globalDisplayManagers = {};
        // window.globalDisplayManager = null;
    }
    window.globalDisplayManagers[displayManager.getId()] = displayManager;
    window.globalDisplayManager = displayManager;
}


function addGlobalDisplayType(type) {
    if (window.globalDisplayTypes == null) {
        window.globalDisplayTypes = [];
    }
    window.globalDisplayTypes.push(type);
}

//
//This will get the currently created global displaymanager or will create a new one
//
function getOrCreateDisplayManager(id, properties, force) {
    if (!force) {
        var displayManager = getDisplayManager(id);
        if (displayManager != null) {
            return displayManager;
        }
        if (window.globalDisplayManager != null) {
            return window.globalDisplayManager;
        }
    }
    var displayManager = new DisplayManager(id, properties);
    if (window.globalDisplayManager == null) {
        window.globalDisplayManager = displayManager;
    }
    return displayManager;
}

//
//return the global display manager with the given id, null if not found
//
function getDisplayManager(id) {
    if (window.globalDisplayManagers == null) {
        return null;
    }
    var manager = window.globalDisplayManagers[id];
    return manager;
}



var ID_DISPLAYS = "displays";

//
//DisplayManager constructor
//

function DisplayManager(argId, argProperties) {


    var ID_MENU_BUTTON = "menu_button";
    var ID_MENU_CONTAINER = "menu_container";
    var ID_MENU_OUTER = "menu_outer";
    var ID_MENU_INNER = "menu_inner";

    RamaddaUtil.inherit(this, this.SUPER = new DisplayThing(argId, argProperties));
    addRamaddaDisplay(this);

    RamaddaUtil.initMembers(this, {
        dataList: [],
        displayTypes: [],
        initMapBounds: null,
    });

    RamaddaUtil.defineMembers(this, {
        group: new DisplayGroup(this, argId, argProperties),
        showmap: this.getProperty(PROP_SHOW_MAP, null),
        setDisplayReady: function() {
            SUPER.setDisplayReadyCall(this);
            this.getLayoutManager().setDisplayReady();
        },
        getLayoutManager: function() {
            return this.group;
        },
        collectEntries: function() {
            var entries = this.getLayoutManager().collectEntries();
            return entries;
        },
        getData: function() {
            return this.dataList;
        },
        handleEventFieldValueSelect: function(source, args) {
            this.notifyEvent("handleEventFieldValueSelected", source, args);
        },
        handleEventFieldsSelected: function(source, fields) {
            this.notifyEvent("handleEventFieldsSelected", source, fields);
        },
        handleEventPropertyChanged: function(source, prop) {
            this.notifyEvent("handleEventPropertyChanged", source, prop);
        },
        handleEventEntriesChanged: function(source, entries) {
            this.notifyEvent("handleEventEntriesChanged", source, entries);
        },
        handleEventMapBoundsChanged: function(source, bounds, forceSet) {
            var args = {
                "bounds": bounds,
                "force": forceSet
            };
            this.notifyEvent("handleEventMapBoundsChanged", source, args);
        },
        addMapLayer: function(source, entry) {
            this.notifyEvent("addMapLayer", source, {
                entry: entry
            });
        },
        handleEventMapClick: function(mapDisplay, lon, lat) {
            var indexObj = [];
            var records = null;
            for (var i = 0; i < this.dataList.length; i++) {
                var pointData = this.dataList[i];
                records = pointData.getRecords();
                if (records != null) break;
            }
            var indexObj = [];
            var closest = RecordUtil.findClosest(records, lon, lat, indexObj);
            if (closest != null) {
                this.propagateEventRecordSelection(mapDisplay, pointData, {
                    index: indexObj.index
                });
            }
            this.notifyEvent("handleEventMapClick", mapDisplay, {
                display: mapDisplay,
                lon: lon,
                lat: lat
            });
        },
        propagateEventRecordSelection: function(source, pointData, args) {
            var index = args.index;
            if (pointData == null && this.dataList.length > 0) {
                pointData = this.dataList[0];
            }
            var fields = pointData.getRecordFields();
            var records = pointData.getRecords();
            if (records == null) {
                return;
            }
            if (index < 0 || index >= records.length) {
                console.log("propagateEventRecordSelection: bad index= " + index);
                return;
            }
            var record = records[index];
            if (record == null) return;
            var values = this.getRecordHtml(record, fields);
            if (source.recordSelectionCallback) {
                var func = source.recordSelectionCallback;
                if ((typeof func) == "string") {
                    func = window[func];
                }
                func({
                    display: source,
                    pointData: pointData,
                    index: index,
                    pointRecord: record
                });
            }
            var params = {
                index: index,
                record: record,
                html: values,
                data: pointData
            };
            this.notifyEvent("handleEventRecordSelection", source, params);
            var entries = source.getEntries();
            if (entries != null && entries.length > 0) {
                this.handleEventEntrySelection(source, {
                    entry: entries[0],
                    selected: true
                });
            }
        },
        handleEventEntrySelection: function(source, props) {
            this.notifyEvent("handleEventEntrySelection", source, props);
        },
        handleEventEntryMouseover: function(source, props) {
            this.notifyEvent("handleEventEntryMouseover", source, props);
        },
        handleEventEntryMouseout: function(source, props) {
            this.notifyEvent("handleEventEntryMouseout", source, props);
        },
        handleEventPointDataLoaded: function(source, pointData) {
            this.notifyEvent("handleEventPointDataLoaded", source, pointData);
        },
        ranges: {
            //               "TRF": [0,100],
        },
        setRange: function(field, range) {
            if (this.ranges[field.getId()] == null) {
                this.ranges[field.getId()] = range;
            }
        },
        getRange: function(field) {
            return this.ranges[field.getId()];
        },
        makeMainMenu: function() {
            if (!this.getProperty(PROP_SHOW_MENU, true)) {
                return "";
            }
            //How else do I refer to this object in the html that I add 
            var get = "getDisplayManager('" + this.getId() + "')";
            var layout = "getDisplayManager('" + this.getId() + "').getLayoutManager()";
            var html = "";
            var newMenus = {};
            var cats = [];
            var chartMenu = "";
            var displayTypes = [];
            if (window.globalDisplayTypes != null) {
                displayTypes = window.globalDisplayTypes;
            }
            for (var i = 0; i < displayTypes.length; i++) {
                //The ids (.e.g., 'linechart' have to match up with some class function with the name 
                var type = displayTypes[i];
                if (Utils.isDefined(type.forUser) && !type.forUser) {
                    continue;
                }
                var category = type.category;
                if (!category) {
                    category = "Misc";
                }
                if (newMenus[category] == null) {
                    newMenus[category] = "";
                    cats.push(category);
                }
                newMenus[category] += HtmlUtils.tag(TAG_LI, [], HtmlUtils.tag(TAG_A, ["onclick", get + ".userCreateDisplay('" + type.type + "');"], type.label));
            }

            var newMenu = "";
            for (var i = 0; i < cats.length; i++) {
                var cat = cats[i];
                if (cat == "Charts") {
                    chartMenu = newMenus[cat];
                }
                var subMenu = HtmlUtils.tag("ul", [], newMenus[cat]);
                var catLabel = HtmlUtils.tag(TAG_A, [], cat);
                newMenu += HtmlUtils.tag(TAG_LI, [], catLabel + subMenu);
                //                    newMenu  += HtmlUtils.tag(TAG_LI,[], "SUB " + i);
            }
            var publishMenu =
                HtmlUtils.tag(TAG_LI, [], HtmlUtils.onClick(layout + ".publish('media_photoalbum');", "New Photo Album")) + "\n" +
                HtmlUtils.tag(TAG_LI, [], HtmlUtils.onClick(layout + ".publish('wikipage');", "New Wiki Page")) + "\n" +
                HtmlUtils.tag(TAG_LI, [], HtmlUtils.onClick(layout + ".publish('blogentry');", "New Blog Post")) + "\n";


            var fileMenu =
                HtmlUtils.tag(TAG_LI, [], "<a>Publish</a>" + HtmlUtils.tag("ul", [], publishMenu)) + "\n" +
                HtmlUtils.tag(TAG_LI, [], HtmlUtils.onClick(layout + ".copyDisplayedEntries();", "Save entries")) + "\n";


            var titles = HtmlUtils.tag(TAG_DIV, ["class", "ramadda-menu-block"], "Titles: " + HtmlUtils.onClick(layout + ".titlesOn();", "On") + "/" + HtmlUtils.onClick(layout + ".titlesOff();", "Off"));
            var dates = HtmlUtils.tag(TAG_DIV, ["class", "ramadda-menu-block"],
                "Set date range: " +
                HtmlUtils.onClick(layout + ".askMinDate();", "Min") + "/" +
                HtmlUtils.onClick(layout + ".askMaxDate();", "Max"));
            var editMenu =
                HtmlUtils.tag(TAG_LI, [], HtmlUtils.tag(TAG_DIV, ["class", "ramadda-menu-block"],
                    "Set axis range :" +
                    HtmlUtils.onClick(layout + ".askMinZAxis();", "Min") + "/" +
                    HtmlUtils.onClick(layout + ".askMaxZAxis();", "Max"))) +
                HtmlUtils.tag(TAG_LI, [], dates) +
                HtmlUtils.tag(TAG_LI, [], titles) + "\n" +
                HtmlUtils.tag(TAG_LI, [], HtmlUtils.tag(TAG_DIV, ["class", "ramadda-menu-block"], "Details: " + HtmlUtils.onClick(layout + ".detailsOn();", "On", []) + "/" +
                    HtmlUtils.onClick(layout + ".detailsOff();", "Off", []))) +
                HtmlUtils.tag(TAG_LI, [], HtmlUtils.onClick(layout + ".deleteAllDisplays();", "Delete all displays")) + "\n" +
                "";


            var table = HtmlUtils.tag(TAG_DIV, ["class", "ramadda-menu-block"], "Table: " +
                HtmlUtils.onClick(layout + ".setLayout('table',1);", "1 column") + " / " +
                HtmlUtils.onClick(layout + ".setLayout('table',2);", "2 column") + " / " +
                HtmlUtils.onClick(layout + ".setLayout('table',3);", "3 column") + " / " +
                HtmlUtils.onClick(layout + ".setLayout('table',4);", "4 column"));
            var layoutMenu =
                HtmlUtils.tag(TAG_LI, [], table) +
                HtmlUtils.tag(TAG_LI, [], HtmlUtils.onClick(layout + ".setLayout('rows');", "Rows")) + "\n" +
                HtmlUtils.tag(TAG_LI, [], HtmlUtils.onClick(layout + ".setLayout('columns');", "Columns")) + "\n" +
                HtmlUtils.tag(TAG_LI, [], HtmlUtils.onClick(layout + ".setLayout('tabs');", "Tabs"));



            var menuBar = HtmlUtils.tag(TAG_LI, [], "<a>File</a>" + HtmlUtils.tag("ul", [], fileMenu));
            if (chartMenu != "") {
                menuBar += HtmlUtils.tag(TAG_LI, [], "<a>Charts</a>" + HtmlUtils.tag("ul", [], chartMenu));
            }
            menuBar += HtmlUtils.tag(TAG_LI, [], "<a>Edit</a>" + HtmlUtils.tag("ul", [], editMenu)) +
                HtmlUtils.tag(TAG_LI, [], "<a>New</a>" + HtmlUtils.tag("ul", [], newMenu)) +
                HtmlUtils.tag(TAG_LI, [], "<a>Layout</a>" + HtmlUtils.tag("ul", [], layoutMenu));
            var menu = HtmlUtils.div([ATTR_CLASS, "ramadda-popup", ATTR_ID, this.getDomId(ID_MENU_OUTER)],
                HtmlUtils.tag("ul", [ATTR_ID, this.getDomId(ID_MENU_INNER), ATTR_CLASS, "sf-menu"], menuBar));

            html += menu;
            //                html += HtmlUtils.tag(TAG_A, [ATTR_CLASS, "display-menu-button", ATTR_ID, this.getDomId(ID_MENU_BUTTON)],"&nbsp;");
            //                html+="<br>";
            return html;
        },
        hasGeoMacro: function(jsonUrl) {
            return jsonUrl.match(/(\${latitude})/g) != null;
        },
        getJsonUrl: function(jsonUrl, display, props) {
            var fromDate = display.getProperty(PROP_FROMDATE);
            if (fromDate != null) {
                jsonUrl += "&fromdate=" + fromDate;
            }
            var toDate = display.getProperty(PROP_TODATE);
            if (toDate != null) {
                jsonUrl += "&todate=" + toDate;
            }

            if (this.hasGeoMacro(jsonUrl)) {
                var lon = props.lon;
                var lat = props.lat;

                if ((lon == null || lat == null) && this.map != null) {
                    var tuple = this.getPosition();
                    if (tuple != null) {
                        lat = tuple[0];
                        lon = tuple[1];
                    }
                }
                if (lon != null && lat != null) {
                    jsonUrl = jsonUrl.replace("${latitude}", lat.toString());
                    jsonUrl = jsonUrl.replace("${longitude}", lon.toString());
                }
            }
            jsonUrl = jsonUrl.replace("${numpoints}", 1000);
            return jsonUrl;
        },
        getDefaultData: function() {
            for (var i in this.dataList) {
                var data = this.dataList[i];
                var records = data.getRecords();
                if (records != null) {
                    return data;
                }
            }
            if (this.dataList.length > 0) {
                return this.dataList[0];
            }
            return null;
        },

        writeDisplay: function() {
            if (this.originalLocation == null) {
                this.originalLocation = document.location;
            }
            var url = this.originalLocation + "#";
            url += "&display0=linechart";
            for (var attr in document) {
                //                   if(attr.toString().contains("location")) 
                //                       console.log(attr +"=" + document[attr]);
            }
            document.location = url;

        },
        userCreateDisplay: function(type, props) {
            if (props == null) {
                props = {};
            }
            props.editMode = true;
            props.layoutHere = false;
            if (type == DISPLAY_LABEL && props.text == null) {
                var text = prompt("Text");
                if (text == null) return;
                props.text = text;
            }
            return this.createDisplay(type, props);
        },
        createDisplay: function(type, props) {
            if (props == null) {
                props = {};
            }

            if (props.data != null) {
                var haveItAlready = false;
                for (var i = 0; i < this.dataList.length; i++) {
                    var existingData = this.dataList[i];
                    if (existingData.equals(props.data)) {
                        props.data = existingData;
                        haveItAlready = true;
                        break;
                    }
                }
                if (!haveItAlready) {
                    this.dataList.push(props.data);
                }
                //                console.log("data:" + haveItAlready);
            }

            //            console.log("props:" + JSON.stringify(props));
            //Upper case the type name, e.g., linechart->Linechart
            var proc = type.substring(0, 1).toUpperCase() + type.substring(1);

            //Look for global functions  Ramadda<Type>Display, <Type>Display, <Type> 
            //e.g. - RamaddaLinechartDisplay, LinechartDisplay, Linechart 
            var classname = null;
            var names = ["Ramadda" + proc + "Display",
                proc + "Display",
                proc
            ];
            var func = null;
            var funcName = null;
            var msg = "";
            for (var i = 0; i < names.length; i++) {
                msg += ("trying:" + names[i] + "\n");
                if (window[names[i]] != null) {
                    funcName = names[i];
                    func = window[names[i]];
                    break;
                }

            }
            if (func == null) {
                console.log("Error: could not find display function:" + type);
                //                    alert("Error: could not find display function:" + type);
                alert("Error: could not find display function:" + type + " msg: " + msg);
                return;
            }
            var displayId = this.getUniqueId("display");
            if (props.data == null && this.dataList.length > 0) {
                props.data = this.dataList[0];
            }
            props.createdInteractively = true;
            if (!props.entryId) {
                props.entryId = this.group.entryId;
            }
            var display = eval(" new " + funcName + "(this,'" + displayId + "', props);");
            if (display == null) {
                console.log("Error: could not create display using:" + funcName);
                alert("Error: could not create display using:" + funcName);
                return;
            }
            this.addDisplay(display);
            return display;
        },
        pageHasLoaded: function(display) {
            this.getLayoutManager().pageHasLoaded();
        },
        addDisplay: function(display) {
            display.setDisplayManager(this);
            display.loadInitialData();
            this.getLayoutManager().addDisplay(display);
        },
        notifyEvent: function(func, source, data) {
            this.getLayoutManager().notifyEvent(func, source, data);
        },
        removeDisplay: function(display) {
            this.getLayoutManager().removeDisplay(display);
            this.notifyEvent("handleEventRemoveDisplay", this, display);
        },
    });

    addDisplayManager(this);

    var displaysHtml = HtmlUtils.div([ATTR_ID, this.getDomId(ID_DISPLAYS), ATTR_CLASS, "display-container"]);
    var html = HtmlUtils.openTag(TAG_DIV);
    html += HtmlUtils.div(["id", this.getDomId(ID_MENU_CONTAINER)]);
    //    html += this.makeMainMenu();
    if (this.getProperty(PROP_SHOW_MENU, true)) {
        html += HtmlUtils.tag(TAG_A, [ATTR_CLASS, "display-menu-button", ATTR_ID, this.getDomId(ID_MENU_BUTTON)], "&nbsp;");
    }
    var targetDiv = this.getProperty("target");
    var _this = this;
    if (targetDiv != null) {
        $(document).ready(function() {
            $("#" + targetDiv).html(displaysHtml);
            _this.getLayoutManager().doLayout();
        });

    } else {
        html += displaysHtml;
    }
    html += HtmlUtils.closeTag(TAG_DIV);
    $("#" + this.getId()).html(html)
    if (this.showmap) {
        this.createDisplay('map');
    }
    var theDisplayManager = this;

    $("#" + this.getDomId(ID_MENU_BUTTON)).button({
        icons: {
            primary: "ui-icon-gear",
            secondary: "ui-icon-triangle-1-s"
        }
    }).click(function(event) {
        var html = theDisplayManager.makeMainMenu();
        theDisplayManager.jq(ID_MENU_CONTAINER).html(html);
        var id = theDisplayManager.getDomId(ID_MENU_OUTER);
        showPopup(event, theDisplayManager.getDomId(ID_MENU_BUTTON), id, false, null, "left bottom");
        theDisplayManager.jq(ID_MENU_INNER).superfish({
            //Don't set animation - it is broke on safari
            //                    animation: {height:'show'},
            speed: 'fast',
            delay: 300
        });
    });

}


function RamaddaMultiDisplay(displayManager, id, properties) {
    this.props = properties;
    let SUPER = new DisplayGroup(displayManager, id, properties, DISPLAY_MULTI);
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
        inInitDisplay: false,
        haveInited: false,
        needsData: function() {
            return true;
        },



        pointDataLoaded: function(pointData, url, reload) {
            SUPER.pointDataLoaded.call(this, pointData, url, reload);
            this.initDisplay();
        },
        processMacros: function(selectedFields, value, makeList) {
            if ((typeof value) != "string") return null;
            var toks = [];
            if (value.includes("${fieldLabel}")) {
                for (i = 0; i < selectedFields.length; i++) {
                    var v = value.replace("\${fieldLabel}", selectedFields[i].getLabel());
                    toks.push(v);
                }
            } else if (value.includes("${fieldId}")) {
                for (i = 0; i < selectedFields.length; i++) {
                    var v = value.replace("\${fieldId}", selectedFields[i].getId());
                    toks.push(v);
                }
            } else if (value.includes("${fieldCnt}")) {
                var v = value.replace("\${fieldCnt}", selectedFields.length);
                toks.push(v);
            } else if (value.includes("${")) {

            } else {
                return null;
            }
            if (makeList) return toks;
            return Utils.join(toks, ",");
        },
        initDisplay: function() {
            try {
                this.initDisplayInner();
            } catch (e) {
                this.setContents(this.getMessage("An error occurred:" + e));
                console.log("An error occurred:" + e);
                console.log(e.stack);
            }
        },
        useChartableFields: function() {
            return false;
        },
        initDisplayInner: function() {
            SUPER.initDisplay.call(this);
            let records = this.filterData();
            if (!records) {
                this.setContents(this.getLoadingMessage());
                return null;
            }

            var allFields = this.getData().getRecordFields();
            var fields = this.getSelectedFields(allFields);
            if (fields.length == 0) {
                this.setContents(this.getMessage("no fields"));
                return;
            }

            if (this.inInitDisplay) return;
            if (this.haveInited) return;
            this.haveInited = true;
            this.inInitDisplay = true;

            var props = {};
            var foreachMap = {};
            var foreachList = [];
            var cnt = 0;
            for (a in this.props) {
                if (a.startsWith("foreach_")) {
                    var value = this.props[a];
                    var toks;
                    var tmp = this.processMacros(fields, value, true);
                    if (tmp) {
                        toks = tmp;
                    } else {
                        toks = value.split(",");
                    }
                    a = a.substring(8);
                    if (toks.length > cnt) cnt = toks.length;
                    foreachMap[a] = toks;
                    foreachList.push({
                        attr: a,
                        toks: toks
                    });
                }
                if (a.startsWith("sub_")) {
                    var value = this.props[a];
                    a = a.substring(4);
                    var tmp = this.processMacros(fields, value, false);
                    if (tmp) {
                        value = tmp;
                    }
                    props[a] = value;
                }
            }
            var html = HtmlUtils.div([ATTR_ID, this.getDomId(ID_DISPLAYS), ATTR_CLASS, "display-container"]);
            this.writeHtml(ID_DISPLAY_CONTENTS, html);
            var groupProps = {
                target: this.getDomId(ID_DISPLAYS),
            }
            groupProps[PROP_LAYOUT_TYPE] = 'table';
            groupProps[PROP_LAYOUT_COLUMNS] = this.getProperty(PROP_LAYOUT_COLUMNS, "2");
            this.displayManager = new DisplayManager(this.getId() + "_manager", groupProps);


            props.layoutHere = false;
            if (this.props['data'])
                props['data'] = this.props['data'];
            var subType = this.getProperty("subType", "table");
            if (cnt == 0) cnt = 1;
            for (var i = 0; i < cnt; i++) {
                var dprops = {};
                $.extend(dprops, props);
                for (var j = 0; j < foreachList.length; j++) {
                    if (i < foreachList[j].toks.length) {
                        dprops[foreachList[j].attr] = foreachList[j].toks[i];
                    }
                }
                if (dprops['fields']) dprops['fields'] = dprops['fields'].split(",");
                this.displayManager.createDisplay(subType, dprops);
            }
            this.inInitDisplay = false;
        }
    });
}/**
Copyright 2008-2019 Geode Systems LLC
*/

var DISPLAY_MAP = "map";

var displayMapMarkers = ["marker.png", "marker-blue.png", "marker-gold.png", "marker-green.png"];
var displayMapCurrentMarker = -1;
var displayMapUrlToVectorListeners = {};
var displayMapMarkerIcons = {};

addGlobalDisplayType({
    type: DISPLAY_MAP,
    label: "Map"
});

function MapFeature(source, points) {
    RamaddaUtil.defineMembers(this, {
        source: source,
        points: points
    });
}




function RamaddaMapDisplay(displayManager, id, properties) {
    var ID_LATFIELD = "latfield";
    var ID_LONFIELD = "lonfield";
    var ID_MAP = "map";
    var ID_BOTTOM = "bottom";
    var ID_RUN = "maprun";
    var ID_STEP = "mapstep";
    var ID_SHOWALl = "showall";
    var ID_ANIMATION_LABEL = "animationlabel";
    var SUPER;
    RamaddaUtil.defineMembers(this, {
        showLocationReadout: false,
        showBoxes: true,
        showPercent: false,
        percentFields: null,
        kmlLayer: null,
        kmlLayerName: "",
        geojsonLayer: null,
        geojsonLayerName: "",
        theMap: null
    });

    RamaddaUtil.inherit(this, SUPER = new RamaddaDisplay(displayManager, id,
        DISPLAY_MAP, properties));
    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
        mapBoundsSet: false,
        features: [],
        myMarkers: {},
        mapEntryInfos: {},
        sourceToLine: {},
        sourceToPoints: {},
        snarf: true,
        initDisplay: function() {
            SUPER.initDisplay.call(this);
            var _this = this;
            var html = "";
            var extraStyle = "min-height:200px;";
            var width = this.getWidth();
            if (Utils.isDefined(width)) {
                if (width > 0) {
                    extraStyle += "width:" + width + "px; ";
                } else if (width < 0) {
                    extraStyle += "width:" + (-width) + "%;";
                } else if (width != "") {
                    extraStyle += "width:" + width + ";";
                }
            }

            var height = this.getProperty("height", 300);
            // var height = this.getProperty("height",-1);
            if (height > 0) {
                extraStyle += " height:" + height + "px; ";
            } else if (height < 0) {
                extraStyle += " height:" + (-height) + "%; ";
            } else if (height != "") {
                extraStyle += " height:" + (height) + ";";
            }

            if (this.getProperty("doAnimation", false)) {

                var buttons = HtmlUtils.div(["id", this.getDomId(ID_RUN), "class", "ramadda-button", "what", "run"], "Start Animation") + "&nbsp;" +
                    HtmlUtils.div(["id", this.getDomId(ID_STEP), "class", "ramadda-button", "what", "run"], "Step") + "&nbsp;" +
                    HtmlUtils.div(["id", this.getDomId(ID_SHOWALl), "class", "ramadda-button", "what", "run"], "Show All") + "&nbsp;" +
                    HtmlUtils.span(["id", this.getDomId(ID_ANIMATION_LABEL), "class", "display-map-animation-label"]);
                buttons = HtmlUtils.div(["class", "display-map-toolbar"], buttons);
                this.jq(ID_TOP_LEFT).append(buttons);
                this.run = this.jq(ID_RUN);
                this.step = this.jq(ID_STEP);
                this.showAll = this.jq(ID_SHOWALl);
                this.animation.label = this.jq(ID_ANIMATION_LABEL);
                this.run.button().click(() => {
                    this.toggleAnimation();
                });
                this.step.button().click(() => {
                    if (!this.animation.running)
                        this.startAnimation(true);
                });
                this.showAll.button().click(() => {
                    this.animation.running = false;
                    this.animation.inAnimation = false;
                    this.animation.label.html("");
                    this.run.html("Start Animation");
                    this.showAllPoints();
                });
            }

            html += HtmlUtils.div([ATTR_CLASS, "display-map-map", "style",
                extraStyle, ATTR_ID, this.getDomId(ID_MAP)
            ]);
            html += HtmlUtils.div([ATTR_CLASS, "", ATTR_ID, this.getDomId(ID_BOTTOM)]);

            if (this.showLocationReadout) {
                html += HtmlUtils.openTag(TAG_DIV, [ATTR_CLASS,
                    "display-map-latlon"
                ]);
                html += HtmlUtils.openTag("form");
                html += "Latitude: " +
                    HtmlUtils.input(this.getDomId(ID_LATFIELD), "", ["size",
                        "7", ATTR_ID, this.getDomId(ID_LATFIELD)
                    ]);
                html += "  ";
                html += "Longitude: " +
                    HtmlUtils.input(this.getDomId(ID_LONFIELD), "", ["size",
                        "7", ATTR_ID, this.getDomId(ID_LONFIELD)
                    ]);
                html += HtmlUtils.closeTag("form");
                html += HtmlUtils.closeTag(TAG_DIV);
            }
            this.setContents(html);

            if (!this.map) {
                this.createMap();
            } else {
                this.map.setMapDiv(this.getDomId(ID_MAP));
            }

            if (!this.haveCalledUpdateUI) {
                var callback = function() {
                    _this.updateUI();
                }
                setTimeout(callback, 1);
            }
        },
        checkLayout: function() {
            if (!this.map) {
                return;
            }
            var d = this.jq(ID_MAP);
            if (d.width() > 0 && this.lastWidth != d.width() && this.map) {
                this.lastWidth = d.width();
                this.map.getMap().updateSize();
            }
        },

        createMap: function() {
            var theDisplay = this;

            var params = {
                "defaultMapLayer": this.getProperty("defaultMapLayer",
                    map_default_layer),

            };
            var displayDiv = this.getProperty("displayDiv", null);
            if (displayDiv) {
                params.displayDiv = displayDiv;
            }
            if (!this.getProperty("showLocationSearch", true)) {
                params.showLocationSearch = false;
            }
            var mapLayers = this.getProperty("mapLayers", null);
            if (mapLayers) {
                params.mapLayers = [mapLayers];
            }

            this.map = this.getProperty("theMap", null);
            if (this.map) {
                this.map.setMapDiv(this.getDomId(ID_MAP));
            } else {
                this.map = new RepositoryMap(this.getDomId(ID_MAP), params);
                this.lastWidth = this.jq(ID_MAP).width();
            }
            if (this.doDisplayMap()) {
                this.map.setDefaultCanSelect(false);
            }
            this.map.initMap(false);

            this.map.addRegionSelectorControl(function(bounds) {
                theDisplay.getDisplayManager().handleEventMapBoundsChanged(this, bounds, true);
            });
            this.map.addClickHandler(this.getDomId(ID_LONFIELD), this
                .getDomId(ID_LATFIELD), null, this);
            this.map.map.events.register("zoomend", "", function() {
                theDisplay.mapBoundsChanged();
            });
            this.map.map.events.register("moveend", "", function() {
                theDisplay.mapBoundsChanged();
            });

            var overrideBounds = false;
            if (this.getProperty("bounds")) {
                overrideBounds = true;
                var toks = this.getProperty("bounds", "").split(",");
                if (toks.length == 4) {
                    if (this.getProperty("showBounds", true)) {
                        var attrs = {};
                        if (this.getProperty("boundsColor")) {
                            attrs.strokeColor = this.getProperty("boundsColor", "");
                        }
                        this.map.addRectangle("bounds", parseFloat(toks[0]), parseFloat(toks[1]), parseFloat(toks[2]), parseFloat(toks[3]), attrs, "");
                    }
                    this.setInitMapBounds(parseFloat(toks[0]), parseFloat(toks[1]), parseFloat(toks[2]), parseFloat(toks[3]));
                }
            }

            var currentFeatures = this.features;
            this.features = [];
            for (var i = 0; i < currentFeatures.length; i++) {
                this.addFeature(currentFeatures[i]);
            }
            var entries = this.getDisplayManager().collectEntries();
            for (var i = 0; i < entries.length; i++) {
                var pair = entries[i];
                this.handleEventEntriesChanged(pair.source, pair.entries);
            }

            if (this.layerEntries) {
                var selectCallback = function(layer) {
                    _this.handleLayerSelect(layer);
                }
                var unselectCallback = function(layer) {
                    _this.handleLayerUnselect(layer);
                }
                var toks = this.layerEntries.split(",");
                for (var i = 0; i < toks.length; i++) {
                    var tok = toks[i];
                    var url = ramaddaBaseUrl + "/entry/show?output=shapefile.kml&entryid=" + tok;
                    this.map.addKMLLayer("layer", url, true, selectCallback, unselectCallback);
                    //TODO: Center on the kml
                }
            }
            if (this.showDataLayers()) {
                if (theDisplay.kmlLayer != null) {
                    var url = ramaddaBaseUrl + "/entry/show?output=shapefile.kml&entryid=" + theDisplay.kmlLayer;
                    theDisplay.addBaseMapLayer(url, true);
                }
                if (theDisplay.geojsonLayer != null) {
                    url = theDisplay.getRamadda().getEntryDownloadUrl(theDisplay.geojsonLayer);
                    theDisplay.addBaseMapLayer(url, false);
                }
            }
            if (this.getProperty("latitude")) {
                this.map.setCenter(createLonLat(parseFloat(this.getProperty("longitude", -105)),
                    parseFloat(this.getProperty("latitude", 40))));
            }


        },
        addBaseMapLayer: function(url, isKml) {
            var theDisplay = this;
            mapLoadInfo = displayMapUrlToVectorListeners[url];
            if (mapLoadInfo == null) {
                mapLoadInfo = {
                    otherMaps: [],
                    layer: null
                };
                selectFunc = function(layer) {
                    theDisplay.mapFeatureSelected(layer);
                }
                var hasBounds = this.getProperty("bounds") != null;
                if (isKml)
                    this.map.addKMLLayer(this.kmlLayerName, url, this.doDisplayMap(), selectFunc, null, null,
                        function(map, layer) {
                            theDisplay.baseMapLoaded(layer, url);
                        }, !hasBounds);
                else
                    this.map.addGeoJsonLayer(this.geojsonLayerName, url, this.doDisplayMap(), selectFunc, null, null,
                        function(map, layer) {
                            theDisplay.baseMapLoaded(layer, url);
                        }, !hasBounds);
            } else if (mapLoadInfo.layer) {
                this.cloneLayer(mapLoadInfo.layer);
            } else {
                this.map.showLoadingImage();
                mapLoadInfo.otherMaps.push(this);
            }
        },
        mapFeatureSelected: function(layer) {
            if (!this.getPointData()) {
                return;
            }
            this.map.onFeatureSelect(layer);
            if (!Utils.isDefined(layer.feature.dataIndex)) {
                return;
            }
            this.getDisplayManager().propagateEventRecordSelection(this, this.getPointData(), {
                index: layer.feature.dataIndex
            });
        },
        showDataLayers: function() {
            return this.getProperty("showLayers", true);
        },
        doDisplayMap: function() {
            if (!this.showDataLayers()) return false;
            if (!this.getProperty("displayAsMap", true)) return false;
            return this.kmlLayer != null || this.geojsonLayer != null;
        },
        cloneLayer: function(layer) {
            var theDisplay = this;
            this.map.hideLoadingImage();
            layer = layer.clone();
            var features = layer.features;
            var clonedFeatures = [];
            for (var j = 0; j < features.length; j++) {
                feature = features[j];
                feature = feature.clone();
                if (feature.style) {
                    oldStyle = feature.style;
                    feature.style = {};
                    for (var a in oldStyle) {
                        feature.style[a] = oldStyle[a];
                    }
                }
                feature.layer = layer;
                clonedFeatures.push(feature);
            }
            layer.removeAllFeatures();
            this.map.map.addLayer(layer);
            layer.addFeatures(clonedFeatures);
            this.vectorLayer = layer;
            this.applyVectorMap();
            this.map.addSelectCallback(layer, this.doDisplayMap(), function(layer) {
                theDisplay.mapFeatureSelected(layer);
            });
        },
        handleEventPointDataLoaded: function(source, pointData) {
        },
        baseMapLoaded: function(layer, url) {
            this.vectorLayer = layer;
            this.applyVectorMap();
            mapLoadInfo = displayMapUrlToVectorListeners[url];
            if (mapLoadInfo) {
                mapLoadInfo.layer = layer;
                for (var i = 0; i < mapLoadInfo.otherMaps.length; i++) {
                    mapLoadInfo.otherMaps[i].cloneLayer(layer);
                }
                mapLoadInfo.otherMaps = [];
            }
        },
        handleLayerSelect: function(layer) {
            var args = this.layerSelectArgs;
            if (!this.layerSelectPath) {
                if (!args) {
                    this.map.onFeatureSelect(layer);
                    return;
                }
                //If args was defined then default to search
                this.layerSelectPath = "/search/do";
            }
            var url = ramaddaBaseUrl + this.layerSelectPath;
            if (args) {
                var toks = args.split(",");
                for (var i = 0; i < toks.length; i++) {
                    var tok = toks[i];
                    var toktoks = tok.split(":");
                    var urlArg = toktoks[0];
                    var layerField = toktoks[1];
                    var attrs = layer.feature.attributes;
                    var fieldValue = null;
                    for (var attr in attrs) {
                        var attrName = "" + attr;
                        if (attrName == layerField) {
                            var attrValue = null;
                            if (typeof attrs[attr] == 'object' || typeof attrs[attr] == 'Object') {
                                var o = attrs[attr];
                                attrValue = o["value"];
                            } else {
                                attrValue = attrs[attr];
                            }
                            url = HtmlUtils.appendArg(url, urlArg, attrValue);
                            url = url.replace("${" + urlArg + "}", attrValue);
                        }
                    }
                }
            }
            url = HtmlUtils.appendArg(url, "output", "json");
            var entryList = new EntryList(this.getRamadda(), url, null, false);
            entryList.doSearch(this);
            this.getEntryList().showMessage("Searching", HtmlUtils.div([ATTR_STYLE, "margin:20px;"], this.getWaitImage()));
        },
        getEntryList: function() {
            if (!this.entryListDisplay) {
                var props = {
                    showMenu: true,
                    showTitle: true,
                    showDetails: true,
                    layoutHere: false,
                    showForm: false,
                    doSearch: false,
                };
                var id = this.getUniqueId("display");
                this.entryListDisplay = new RamaddaEntrylistDisplay(this.getDisplayManager(), id, props);
                this.getDisplayManager().addDisplay(this.entryListDisplay);
            }
            return this.entryListDisplay;
        },
        entryListChanged: function(entryList) {
            var entries = entryList.getEntries();
            this.getEntryList().entryListChanged(entryList);
        },
        handleLayerUnselect: function(layer) {
            this.map.onFeatureUnselect(layer);
        },
        addMapLayer: function(source, props) {
            var _this = this;
            var entry = props.entry;
            if (!this.addedLayers) this.addedLayers = {};
            if (this.addedLayers[entry.getId()]) {
                var layer = this.addedLayers[entry.getId()];
                if (layer) {
                    this.map.removeKMLLayer(layer);
                    this.addedLayers[entry.getId()] = null;
                }
                return;
            }

            var type = entry.getType().getId();
            if (type == "geo_shapefile" || type == "geo_geojson") {
                var bounds = createBounds(entry.getWest(), entry.getSouth(), entry.getEast(), entry.getNorth());
                if (bounds.left < -180 || bounds.right > 180 || bounds.bottom < -90 || bounds.top > 90) {
                    bounds = null;
                }

                var selectCallback = function(layer) {
                    _this.handleLayerSelect(layer);
                }
                var unselectCallback = function(layer) {
                    _this.handleLayerUnselect(layer);
                }
                var layer;
                if (type == "geo_geojson") {
                    var url = entry.getRamadda().getEntryDownloadUrl(entry);
                    layer = this.map.addGeoJsonLayer(this.geojsonLayerName, url, this.doDisplayMap(), selectCallback, unselectCallback, null, null, true);
                } else {
                    var url = ramaddaBaseUrl + "/entry/show?output=shapefile.kml&entryid=" + entry.getId();
                    layer = this.map.addKMLLayer(entry.getName(), url, true, selectCallback, unselectCallback, null, null, true);
                }
                this.addedLayers[entry.getId()] = layer;
                return;
            }

            var baseUrl = entry.getAttributeValue("base_url");
            if (!Utils.stringDefined(baseUrl)) {
                console.log("No base url:" + entry.getId());
                return;
            }
            var layer = entry.getAttributeValue("layer_name");
            if (layer == null) {
                layer = entry.getName();
            }
            this.map.addWMSLayer(entry.getName(), baseUrl, layer, false);
        },
        mapBoundsChanged: function() {
            var bounds = this.map.map.calculateBounds();
            bounds = bounds.transform(this.map.sourceProjection,
                this.map.displayProjection);
            this.getDisplayManager().handleEventMapBoundsChanged(this, bounds);
        },
        addFeature: function(feature) {
            this.features.push(feature);
            feature.line = this.map.addPolygon("lines_" +
                feature.source.getId(), RecordUtil
                .clonePoints(feature.points), null);
        },
        xloadInitialData: function() {
            if (this.getDisplayManager().getData().length > 0) {
                this.handleEventPointDataLoaded(this, this.getDisplayManager()
                    .getData()[0]);
            }
        },

        getContentsDiv: function() {
            return HtmlUtils.div([ATTR_CLASS, "display-contents", ATTR_ID,
                this.getDomId(ID_DISPLAY_CONTENTS)
            ], "");
        },
        handleEventEntryMouseover: function(source, args) {
            if (!this.map) {
                return;
            }
            id = args.entry.getId() + "_mouseover";
            attrs = {
                lineColor: "red",
                fillColor: "red",
                fillOpacity: 0.5,
                lineOpacity: 0.5,
                doCircle: true,
                lineWidth: 1,
                fill: true,
                circle: {
                    lineColor: "black"
                },
                polygon: {
                    lineWidth: 4,
                }
            }
            this.addOrRemoveEntryMarker(id, args.entry, true, attrs);
        },
        handleEventEntryMouseout: function(source, args) {
            if (!this.map) {
                return;
            }
            id = args.entry.getId() + "_mouseover";
            this.addOrRemoveEntryMarker(id, args.entry, false);
        },
        handleEventAreaClear: function() {
            if (!this.map) {
                return;
            }
            this.map.clearRegionSelector();
        },
        handleClick: function(theMap, lon, lat) {
            if (!this.map) {
                return;
            }
            if (this.doDisplayMap()) {
                return;
            }
            var justOneMarker = this.getProperty("justOneMarker",false);
            if(justOneMarker) {
                var pointData = this.getPointData();
                if(pointData) {
                    pointData.handleEventMapClick(this, source, lon, lat);
                }
            }
            this.getDisplayManager().handleEventMapClick(this, lon, lat);
        },

        getPosition: function() {
            var lat = $("#" + this.getDomId(ID_LATFIELD)).val();
            var lon = $("#" + this.getDomId(ID_LONFIELD)).val();
            if (lat == null)
                return null;
            return [lat, lon];
        },

        haveInitBounds: false,
        setInitMapBounds: function(north, west, south, east) {
            if (!this.map) return;
            if (this.haveInitBounds) return;
            this.haveInitBounds = true;
            this.map.centerOnMarkers(new OpenLayers.Bounds(west, south, east,
                north));
        },

        sourceToEntries: {},
        handleEventEntriesChanged: function(source, entries) {
            if (!this.map) {
                return;
            }
            //debug
            if (source == this.lastSource) {
                this.map.clearSelectionMarker();
            }
            if ((typeof source.forMap) != "undefined" && !source.forMap) {
                return;
            }
            var oldEntries = this.sourceToEntries[source.getId()];
            if (oldEntries != null) {
                for (var i = 0; i < oldEntries.length; i++) {
                    var id = source.getId() + "_" + oldEntries[i].getId();
                    this.addOrRemoveEntryMarker(id, oldEntries[i], false);
                }
            }

            this.sourceToEntries[source.getId()] = entries;

            var markers = new OpenLayers.Layer.Markers("Markers");
            var lines = new OpenLayers.Layer.Vector("Lines", {});
            var north = -90,
                west = 180,
                south = 90,
                east = -180;
            var didOne = false;
            for (var i = 0; i < entries.length; i++) {
                var entry = entries[i];
                var id = source.getId() + "_" + entry.getId();
                var mapEntryInfo = this.addOrRemoveEntryMarker(id, entries[i], true);
                if (entry.hasBounds()) {
                    if (entry.getNorth() > 90 ||
                        entry.getSouth() < -90 ||
                        entry.getEast() > 180 ||
                        entry.getWest() < -180) {
                        console.log("bad bounds on entry:" + entry.getName() + " " +
                            entry.getNorth() + " " +
                            entry.getSouth() + " " +
                            entry.getEast() + " " +
                            entry.getWest());
                        continue;
                    }

                    north = Math.max(north, entry.getNorth());
                    south = Math.min(south, entry.getSouth());
                    east = Math.max(east, entry.getEast());
                    west = Math.min(west, entry.getWest());
                    didOne = true;
                }
            }
            var bounds = (didOne ? createBounds(west, south, east, north) : null);
            //debug                    this.map.centerOnMarkers(bounds, true);
        },
        handleEventEntrySelection: function(source, args) {
            if (!this.map) {
                return;
            }
            var _this = this;
            var entry = args.entry;
            if (entry == null) {
                this.map.clearSelectionMarker();
                return;
            }
            var selected = args.selected;

            if (!entry.hasLocation()) {
                return;
            }

            /*
            if (selected) {
                this.lastSource = source;
                this.map.setSelectionMarker(entry.getLongitude(), entry.getLatitude(), true, args.zoom);
            } else if (source == this.lastSource) {
                this.map.clearSelectionMarker();
            }
            */
        },
        addOrRemoveEntryMarker: function(id, entry, add, args) {
            if (!args) {
                args = {};
            }
            var dflt = {
                lineColor: entry.lineColor,
                fillColor: entry.lineColor,
                lineWidth: entry.lineWidth,
                doCircle: false,
                doRectangle: this.showBoxes,
                fill: false,
                fillOpacity: 0.75,
                pointRadius: 12,
                polygon: {},
                circle: {}
            }
            dfltPolygon = {}
            dfltCircle = {}
            $.extend(dflt, args);
            if (!dflt.lineColor) dflt.lineColor = "blue";

            $.extend(dfltPolygon, dflt);
            if (args.polygon)
                $.extend(dfltPolygon, args.polygon);
            $.extend(dfltCircle, dflt);
            if (args.circle)
                $.extend(dfltCircle, args.circle);

            var mapEntryInfo = this.mapEntryInfos[id];
            if (!add) {
                if (mapEntryInfo != null) {
                    mapEntryInfo.removeFromMap(this.map);
                    this.mapEntryInfos[id] = null;
                }
            } else {
                if (mapEntryInfo == null) {
                    mapEntryInfo = new MapEntryInfo(entry);
                    this.mapEntryInfos[id] = mapEntryInfo;
                    if (entry.hasBounds() && dflt.doRectangle) {
                        var attrs = {};
                        mapEntryInfo.rectangle = this.map.addRectangle(id,
                            entry.getNorth(), entry.getWest(), entry
                            .getSouth(), entry.getEast(), attrs);
                    }
                    var latitude = entry.getLatitude();
                    var longitude = entry.getLongitude();
                    if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
                        return;
                    }
                    var point = new OpenLayers.LonLat(longitude, latitude);
                    if (dflt.doCircle) {
                        attrs = {
                            pointRadius: dfltCircle.pointRadius,
                            stroke: true,
                            strokeColor: dfltCircle.lineColor,
                            strokeWidth: dfltCircle.lineWidth,
                            fillColor: dfltCircle.fillColor,
                            fillOpacity: dfltCircle.fillOpacity,
                            fill: dfltCircle.fill,
                        };
                        mapEntryInfo.circle = this.map.addPoint(id, point, attrs);
                    } else {
                        mapEntryInfo.marker = this.map.addMarker(id, point, entry.getIconUrl(), "", this.getEntryHtml(entry));
                    }
                    if (entry.polygon) {
                        var points = []
                        for (var i = 0; i < entry.polygon.length; i += 2) {
                            points.push(new OpenLayers.Geometry.Point(entry.polygon[i + 1], entry.polygon[i]));
                        }
                        var attrs = {
                            strokeColor: dfltPolygon.lineColor,
                            strokeWidth: Utils.isDefined(dfltPolygon.lineWidth) ? dfltPolygon.lineWidth : 2
                        };
                        mapEntryInfo.polygon = this.map.addPolygon(id, entry.getName(), points, attrs, mapEntryInfo.marker);
                    }
                    var theDisplay = this;
                    if (mapEntryInfo.marker) {
                        mapEntryInfo.marker.entry = entry;
                        mapEntryInfo.marker.ramaddaClickHandler = function(marker) {
                            theDisplay.handleMapClick(marker);
                        };
                        if (this.handledMarkers == null) {
                            this.map.centerToMarkers();
                            this.handledMarkers = true;
                        }
                    }
                }
                return mapEntryInfo;
            }
        },
        handleMapClick: function(marker) {
            if (this.selectedMarker != null) {
                this.getDisplayManager().handleEventEntrySelection(this, {
                    entry: this.selectedMarker.entry,
                    selected: false
                });
            }
            this.getDisplayManager().handleEventEntrySelection(this, {
                entry: marker.entry,
                selected: true
            });
            this.selectedMarker = marker;
        },
        getDisplayProp: function(source, prop, dflt) {
            if (Utils.isDefined(this[prop])) {
                return this[prop];
            }
            prop = "map-" + prop;
            if (Utils.isDefined(source[prop])) {
                return source[prop];
            }
            return source.getProperty(prop, dflt);
        },
        applyVectorMap: function(force) {
            if (!force && this.vectorMapApplied) {
                return;
            }
            if (!this.doDisplayMap()) {
                return;
            }
            if (!this.vectorLayer) {
                return;
            }
            if (!this.points) {
                return;
            }
            this.vectorMapApplied = true;
            var features = this.vectorLayer.features.slice();
            var circles = this.points;
            for (var i = 0; i < circles.length; i++) {
                var circle = circles[i];
                if (circle.style && circle.style.display == "none") continue;
                var center = circle.center;
                var matchedFeature = null;
                var index = -1;

                for (var j = 0; j < features.length; j++) {
                    var feature = features[j];
                    var geometry = feature.geometry;
                    if (!geometry) {
                        break;
                    }
                    bounds = geometry.getBounds();
                    if (!bounds.contains(center.x, center.y)) {
                        continue;
                    }
                    if (geometry.components) {
                        for (var sub = 0; sub < geometry.components.length; sub++) {
                            comp = geometry.components[sub];
                            bounds = comp.getBounds();
                            if (!bounds.contains(center.x, center.y)) {
                                continue;
                            }
                            if (comp.containsPoint && comp.containsPoint(center)) {
                                matchedFeature = feature;
                                index = j;
                                break;
                            }
                        }
                        if (matchedFeature)
                            break;
                        continue;
                    }
                    if (!geometry.containsPoint) {
                        console.log("unknown geometry:" + geometry.CLASS_NAME);
                        continue;
                    }
                    if (geometry.containsPoint(center)) {
                        matchedFeature = feature;
                        index = j;
                        break;
                    }
                }
                if (matchedFeature) {
                    features.splice(index, 1);
                    style = matchedFeature.style;
                    if (!style) style = {
                        "stylename": "from display"
                    };
                    $.extend(style, circle.style);
                    matchedFeature.style = style;
                    matchedFeature.popupText = circle.text;
                    matchedFeature.dataIndex = i;
                }
            }
            for (var i = 0; i < features.length; i++) {
                var feature = features[i];
                style = feature.style;
                if (!style) style = {
                    "stylename": "from display"
                };
                $.extend(style, {
                    "display": "none"
                });
            }

            /*
            if (("" + this.getProperty("pruneFeatures", "")) == "true") {
                this.vectorLayer.removeFeatures(features);
                var dataBounds = this.vectorLayer.getDataExtent();
                bounds = this.map.transformProjBounds(dataBounds);
                if(!force && this.getProperty("bounds") == null)
                    this.map.centerOnMarkers(bounds, true);
            }
            */
            this.vectorLayer.redraw();
        },
        needsData: function() {
            return true;
        },
        animation: {
            running: false,
            inAnimation: false,
            begin: null,
            end: null,
            dateMin: null,
            dateMax: null,
            dateRange: 0,
            dateFormat: this.getProperty("animationDateFormat", "yyyyMMdd"),
            mode: this.getProperty("animationMode", "cumulative"),
            steps: this.getProperty("animationSteps", 60),
            windowUnit: this.getProperty("animationWindow", ""),
            window: 0,
            speed: parseInt(this.getProperty("animationSpeed", 250)),
        },
        toggleAnimation: function() {
            this.animation.running = !this.animation.running;
            this.run.html(this.animation.running ? "Stop Animation" : "Start Animation");
            if (this.animation.running)
                this.startAnimation();
        },
        startAnimation: function(justOneStep) {
            if (!this.points) {
                return;
            }
            if (!this.animation.dateMax) return;
            if (!justOneStep)
                this.animation.running = true;
            if (!this.animation.inAnimation) {
                this.animation.inAnimation = true;
                this.animation.label.html("");
                var date = this.animation.dateMin;
                this.animation.begin = date;
                var unit = this.animation.windowUnit;
                if (unit != "") {
                    var tmp = 0;
                    var size = 0;
                    //Pad the size
                    if (unit == "decade") {
                        this.animation.begin = new Date(date.getUTCFullYear(), 0);
                        size = 1000 * 60 * 60 * 24 * 365 * 10 + 1000 * 60 * 60 * 24 * 365;
                    } else if (unit == "year") {
                        this.animation.begin = new Date(date.getUTCFullYear(), 0);
                        size = 1000 * 60 * 60 * 24 * 366;
                    } else if (unit == "month") {
                        this.animation.begin = new Date(date.getUTCFullYear(), date.getMonth());
                        size = 1000 * 60 * 60 * 24 * 32;
                    } else if (unit == "day") {
                        this.animation.begin = new Date(date.getUTCFullYear(), date.getMonth(), date.getDay());
                        size = 1000 * 60 * 60 * 25;
                    } else if (unit == "hour") {
                        this.animation.begin = new Date(date.getUTCFullYear(), date.getMonth(), date.getDay(), date.getHours());
                        size = 1000 * 60 * 61;
                    } else if (unit == "minute") {
                        this.animation.begin = new Date(date.getUTCFullYear(), date.getMonth(), date.getDay(), date.getHours(), date.getMinutes());
                        size = 1000 * 61;
                    } else {
                        this.animation.begin = new Date(date.getUTCFullYear(), date.getMonth(), date.getDay(), date.getHours(), date.getSeconds());
                        size = 1001;
                    }
                    this.animation.window = size;
                } else {
                    this.animation.window = this.animation.dateRange / this.animation.steps;
                }
                this.animation.end = this.animation.begin;
                for (var i = 0; i < this.points.length; i++) {
                    var point = this.points[i];
                    point.style.display = 'none';
                }
                if (this.map.circles)
                    this.map.circles.redraw();
            }
            this.stepAnimation();
        },
        stepAnimation: function() {
            if (!this.points) return;
            if (!this.animation.dateMax) return;
            var oldEnd = this.animation.end;
            var unit = this.animation.windowUnit;
            var date = new Date(this.animation.end.getTime() + this.animation.window);
            if (unit == "decade") {
                this.animation.end = new Date(date.getUTCFullYear(), 0);
            } else if (unit == "year") {
                this.animation.end = new Date(date.getUTCFullYear(), 0);
            } else if (unit == "month") {
                this.animation.end = new Date(date.getUTCFullYear(), date.getMonth());
            } else if (unit == "day") {
                this.animation.end = new Date(date.getUTCFullYear(), date.getMonth(), date.getDay());
            } else if (unit == "hour") {
                this.animation.end = new Date(date.getUTCFullYear(), date.getMonth(), date.getDay(), date.getHours());
            } else if (unit == "minute") {
                this.animation.end = new Date(date.getUTCFullYear(), date.getMonth(), date.getDay(), date.getHours(), date.getMinutes());
            } else {
                this.animation.end = new Date(this.animation.end.getTime() + this.animation.window);
            }
            if (this.animation.mode == "sliding") {
                this.animation.begin = oldEnd;
            }
            //                console.log("step:" + date  +" -  " + this.animation.end);
            var windowStart = this.animation.begin.getTime();
            var windowEnd = this.animation.end.getTime();
            var atLoc = {};
            for (var i = 0; i < this.points.length; i++) {
                var point = this.points[i];
                if (point.date < windowStart || point.date > windowEnd) {
                    point.style.display = 'none';
                    continue;
                }
                if (atLoc[point.location]) {
                    var other = atLoc[point.location];
                    if (other.date < point.date) {
                        atLoc[point.location] = point;
                        other.style.display = 'none';
                        point.style.display = 'inline';
                    } else {
                        point.style.display = 'none';
                    }
                    continue;
                }
                atLoc[point.location] = point;
                point.style.display = 'inline';
            }

            if (this.map.circles)
                this.map.circles.redraw();
            if (windowEnd < this.animation.dateMax.getTime()) {
                this.animation.label.html(this.formatAnimationDate(this.animation.begin) + " - " + this.formatAnimationDate(this.animation.end));
                if (this.animation.running) {
                    setTimeout(() => this.stepAnimation(), this.animation.speed);
                }
            } else {
                this.animation.running = false;
                this.animation.label.html("");
                this.animation.inAnimation = false;
                this.animation.label.html("");
                this.run.html("Start Animation");
            }
            this.applyVectorMap(true);
        },
        formatAnimationDate: function(d) {
            if (this.animation.dateFormat == "yyyy") {
                return Utils.formatDateYYYY(d);
            } else if (this.animation.dateFormat == "yyyyMMdd") {
                return Utils.formatDateYYYYMMDD(d);
            } else {
                return Utils.formatDate(d);
            }
        },
        showAllPoints: function() {
            if (!this.points) return;
            for (var i = 0; i < this.points.length; i++) {
                var point = this.points[i];
                point.style.display = 'inline';
            }
            if (this.map.circles)
                this.map.circles.redraw();
            this.applyVectorMap(true);
        },

        updateUI: function() {
            this.haveCalledUpdateUI = true;
            SUPER.updateUI.call(this);
            if (!this.getDisplayReady()) {
                return;
            }
            if (!this.hasData()) {
                return;
            }
            if (!this.getProperty("showData", true)) {
                return;
            }

            var pointData = this.getPointData();
            var records = this.filterData();
            if (records == null) {
                err = new Error();
                console.log("null records:" + err.stack);
                return;
            }
            var fields = pointData.getRecordFields();
            var bounds = {};
            var points = RecordUtil.getPoints(records, bounds);
            if (isNaN(bounds.north)) {
                console.log("no bounds:" + bounds);
                return;
            }
            //console.log("bounds:" + bounds.north +" " + bounds.west +" " + bounds.south +" " + bounds.east);
            this.initBounds = bounds;
            this.setInitMapBounds(bounds.north, bounds.west, bounds.south,
                bounds.east);
            if (this.map == null) {
                return;
            }
            if (points.length == 0) {
                console.log("points.length==0");
                return;
            }

            source = this;
            var radius = parseFloat(this.getDisplayProp(source, "radius", 8));
            var strokeWidth = parseFloat(this.getDisplayProp(source, "strokeWidth", "1"));
            var strokeColor = this.getDisplayProp(source, "strokeColor", "#000");
            var colorByAttr = this.getDisplayProp(source, "colorBy", null);
            var colors = this.getColorTable(true);
            var sizeByAttr = this.getDisplayProp(source, "sizeBy", null);
            var isTrajectory = this.getDisplayProp(source, "isTrajectory", false);
            if (isTrajectory) {
                var attrs = {
                    strokeWidth: 2,
                    strokeColor: "blue"
                }

                this.map.addPolygon("id", "", points, attrs, null);
                return;
            }
            if (!colors && source.colors && source.colors.length > 0) {
                colors = source.colors;
                if (colors.length == 1 && Utils.ColorTables[colors[0]]) {
                    colors = Utils.ColorTables[colors[0]].colors;
                }
            }

            if (colors == null) {
                colors = Utils.ColorTables.grayscale.colors;
            }

            var latField1 = this.getFieldById(fields, this.getProperty("latField1"));
            var latField2 = this.getFieldById(fields, this.getProperty("latField2"));
            var lonField1 = this.getFieldById(fields, this.getProperty("lonField1"));
            var lonField2 = this.getFieldById(fields, this.getProperty("lonField2"));
            var showSegments = this.getProperty("showSegments", false);
            var sizeSegments = this.getProperty("sizeSegments", false);
            var sizeEndPoints = this.getProperty("sizeEndPoints", true);
            var showEndPoints = this.getProperty("showEndPoints", false);
            var endPointSize = parseInt(this.getProperty("endPointSize", "4"));
            var dfltEndPointSize = endPointSize;
            var segmentWidth = parseInt(this.getProperty("segmentWidth", "1"));
            var dfltSegmentWidth = segmentWidth;
            var showPoints = this.getProperty("showPoints", true);
            var lineColor = this.getProperty("lineColor", "green");
            var colorBy = {
                id: colorByAttr,
                minValue: 0,
                maxValue: 0,
                field: null,
                index: -1,
                isString: false,
            };


            var sizeBy = {
                id: this.getDisplayProp(source, "sizeBy", null),
                minValue: 0,
                maxValue: 0,
                field: null,
                index: -1,
                isString: false,
                stringMap: {}
            };

            var sizeByMap = this.getProperty("sizeByMap");
            if (sizeByMap) {
                var toks = sizeByMap.split(",");
                for (var i = 0; i < toks.length; i++) {
                    var toks2 = toks[i].split(":");
                    if (toks2.length > 1) {
                        sizeBy.stringMap[toks2[0]] = toks2[1];
                    }
                }
            }

            for (var i = 0; i < fields.length; i++) {
                var field = fields[i];
                if (field.getId() == colorBy.id || ("#" + (i + 1)) == colorBy.id) {
                    colorBy.field = field;
                    if (field.getType() == "string") colorBy.isString = true;
                }
                if (field.getId() == sizeBy.id || ("#" + (i + 1)) == sizeBy.id) {
                    sizeBy.field = field;
                    if (field.getType() == "string") sizeBy.isString = true;
                }
            }


            if (this.getProperty("showColorByMenu", false) && colorBy.field && !this.madeColorByMenu) {
                this.madeColorByMenu = true;
                var menu = "<select class='ramadda-pulldown' id='" + this.getDomId("colorByMenu") + "'>";
                for (var i = 0; i < fields.length; i++) {
                    var field = fields[i];
                    if (!field.isNumeric || field.isFieldGeo()) continue;
                    var extra = "";
                    if (colorBy.field.getId() == field.getId()) extra = "selected ";
                    menu += "<option value='" + field.getId() + "' " + extra + ">" + field.getLabel() + "</option>\n";
                }
                menu += "</select>";
                this.writeHtml(ID_TOP_RIGHT, "Color by: " + menu);
                /*
                this.jq("colorByMenu").superfish({
                        //Don't set animation - it is broke on safari
                        //                    animation: {height:'show'},
                        speed: 'fast',
                            delay: 300
                            });
                */
                this.jq("colorByMenu").change(() => {
                    var value = this.jq("colorByMenu").val();
                    this.vectorMapApplied = false;
                    this.setProperty("colorBy", value);
                    this.updateUI();
                });
            }

            sizeBy.index = sizeBy.field != null ? sizeBy.field.getIndex() : -1;
            colorBy.index = colorBy.field != null ? colorBy.field.getIndex() : -1;
            var excludeZero = this.getProperty(PROP_EXCLUDE_ZERO, false);
            this.animation.dateMin = null;
            this.animation.dateMax = null;
            var colorByMap = {};
            var colorByValues = [];

            var colorByMinPerc = this.getDisplayProp(source, "colorByMinPercentile", -1);
            var colorByMaxPerc = this.getDisplayProp(source, "colorByMaxPercentile", -1);

            var justOneMarker = this.getProperty("justOneMarker",false);
            for (var i = 0; i < points.length; i++) {
                var pointRecord = records[i];

                if (this.animation.dateMin == null) {
                    this.animation.dateMin = pointRecord.getDate();
                    this.animation.dateMax = pointRecord.getDate();
                } else {
                    var date = pointRecord.getDate();
                    if (date) {
                        if (date.getTime() < this.animation.dateMin.getTime())
                            this.animation.dateMin = date;
                        if (date.getTime() > this.animation.dateMax.getTime())
                            this.animation.dateMax = date;
                    }
                }
                var tuple = pointRecord.getData();
                var v = tuple[colorBy.index];
                if (colorBy.isString) {
                    if (!Utils.isDefined(colorByMap[v])) {
                        colorByValues.push(v);
                        colorByMap[v] = colorByValues.length;
                        colorBy.minValue = 1;
                        colorBy.maxValue = colorByValues.length;
                        //                        console.log("cb:" +colorBy.minValue +" -  " +colorBy.maxValue);
                    }
                }


                if (isNaN(v) || v === null)
                    continue;
                if (excludeZero && v == 0) {
                    continue;
                }
                if (!colorBy.isString) {
                    if (i == 0 || v > colorBy.maxValue) colorBy.maxValue = v;
                    if (i == 0 || v < colorBy.minValue) colorBy.minValue = v;
                }
                if (!sizeBy.isString) {
                    v = tuple[sizeBy.index];
                    if (i == 0 || v > sizeBy.maxValue) sizeBy.maxValue = v;
                    if (i == 0 || v < sizeBy.minValue) sizeBy.minValue = v;
                }
            }
            sizeBy.radiusMin = parseFloat(this.getProperty("sizeByRadiusMin", -1));
            sizeBy.radiusMax = parseFloat(this.getProperty("sizeByRadiusMax", -1));
            var sizeByOffset = 0;
            var sizeByLog = this.getProperty("sizeByLog", false);
            var sizeByFunc = Math.log;
            if (sizeByLog) {
                if (sizeBy.minValue < 1) {
                    sizeByOffset = 1 - sizeBy.minValue;
                }
                sizeBy.minValue = sizeByFunc(sizeBy.minValue + sizeByOffset);
                sizeBy.maxValue = sizeByFunc(sizeBy.maxValue + sizeByOffset);
            }
            sizeBy.range = sizeBy.maxValue - sizeBy.minValue;



            if (this.animation.dateMax) {
                this.animation.dateRange = this.animation.dateMax.getTime() - this.animation.dateMin.getTime();
            }


            if (this.showPercent) {
                colorBy.minValue = 0;
                colorBy.maxValue = 100;
            }
            colorBy.minValue = this.getDisplayProp(source, "colorByMin", colorBy.minValue);
            colorBy.maxValue = this.getDisplayProp(source, "colorByMax", colorBy.maxValue);
            colorBy.origMinValue = colorBy.minValue;
            colorBy.origMaxValue = colorBy.maxValue;

            var colorByOffset = 0;
            var colorByLog = this.getProperty("colorByLog", false);
            var colorByFunc = Math.log;
            if (colorByLog) {
                if (colorBy.minValue < 1) {
                    colorByOffset = 1 - colorBy.minValue;
                }
                colorBy.minValue = colorByFunc(colorBy.minValue + colorByOffset);
                colorBy.maxValue = colorByFunc(colorBy.maxValue + colorByOffset);
            }
            colorBy.range = colorBy.maxValue - colorBy.minValue;
            //            console.log("cb:" + colorBy.minValue +" " + colorBy.maxValue+ " off:" + colorByOffset);


            if (this.points) {
                for (var i = 0; i < this.points.length; i++)
                    this.map.removePoint(this.points[i]);
                for (var i = 0; i < this.lines.length; i++)
                    this.map.removePolygon(this.lines[i]);
                this.points = [];
                this.lines = [];
            }

            if (!this.points) {
                this.points = [];
                this.lines = [];
            }

            var dontAddPoint = this.doDisplayMap();
            var didColorBy = false;
            var seen = {};

            for (var i = 0; i < points.length; i++) {
                var pointRecord = records[i];
                var point = points[i];
                if(justOneMarker) {
                    if(this.justOneMarker)
                        this.map.removeMarker(this.justOneMarker);
                    if(!isNaN(point.x) && !isNaN(point.y)) {
                        this.justOneMarker= this.map.addMarker(id, [point.x,point.y], null, "", "");
                        return;
                    } else {
                        continue;
                    }
                }



                var values = pointRecord.getData();
                var props = {
                    pointRadius: radius,
                    strokeWidth: strokeWidth,
                    strokeColor: strokeColor,
                };

                if (sizeBy.index >= 0) {
                    var value = values[sizeBy.index];
                    if (sizeBy.isString) {
                        if (Utils.isDefined(sizeBy.stringMap[value])) {
                            var v = parseInt(sizeBy.stringMap[value]);
                            segmentWidth = dfltSegmentWidth + v;
                            props.pointRadius = v;
                        } else if (Utils.isDefined(sizeBy.stringMap["*"])) {
                            var v = parseInt(sizeBy.stringMap["*"]);
                            segmentWidth = dfltSegmentWidth + v;
                            props.pointRadius = v;
                        } else {
                            segmentWidth = dfltSegmentWidth;
                        }
                    } else {
                        var denom = sizeBy.range;
                        var v = value + sizeByOffset;
                        if (sizeByLog) v = sizeByFunc(v);
                        var percent = (denom == 0 ? NaN : (v - sizeBy.minValue) / denom);
                        if (sizeBy.radiusMax >= 0 && sizeBy.radiusMin >= 0) {
                            props.pointRadius = Math.round(sizeBy.radiusMin + percent * (sizeBy.radiusMax - sizeBy.radiusMin));
                        } else {
                            props.pointRadius = 6 + parseInt(15 * percent);
                        }
                        if (sizeEndPoints) {
                            endPointSize = dfltEndPointSize + parseInt(10 * percent);
                        }
                        if (sizeSegments) {
                            segmentWidth = dfltSegmentWidth + parseInt(10 * percent);
                        }
                    }
                    //                            console.log("percent:" + percent +  " radius: " + props.pointRadius +" Value: " + value  + " range: " + sizeBy.minValue +" " + sizeBy.maxValue);
                }
                if (colorBy.index >= 0) {
                    var value = pointRecord.getData()[colorBy.index];
                    //                            console.log("value:" + value +" index:" + colorBy.index+" " + pointRecord.getData());
                    var percent = 0;
                    var msg = "";
                    var pctFields = null;
                    if (this.percentFields != null) {
                        pctFields = this.percentFields.split(",");
                    }
                    if (this.showPercent) {
                        var total = 0;
                        var data = pointRecord.getData();
                        var msg = "";
                        for (var j = 0; j < data.length; j++) {
                            var ok = fields[j].isNumeric && !fields[j].isFieldGeo();
                            if (ok && pctFields != null) {
                                ok = pctFields.indexOf(fields[j].getId()) >= 0 ||
                                    pctFields.indexOf("#" + (j + 1)) >= 0;
                            }
                            if (ok) {
                                total += data[j];
                                msg += " " + data[j];
                            }
                        }
                        if (total != 0) {
                            percent0 = percent = value / total * 100;
                            percent = (percent - colorBy.minValue) / (colorBy.maxValue - colorBy.minValue);
                            //                                    console.log("%:" + percent0 +" range:" + percent +" value"+ value +" " + total+"data: " + msg);
                        }

                    } else {
                        var v = value;
                        if (colorBy.isString) {
                            v = colorByMap[v];
                        }
                        v += colorByOffset;
                        if (colorByLog) {
                            v = colorByFunc(v);
                        }
                        percent = (v - colorBy.minValue) / colorBy.range;
                        //console.log("cbv:" +value +" %:" + percent);
                    }

                    var index = parseInt(percent * colors.length);
                    //                            console.log(colorBy.index +" value:" + value+ " " + percent + " " +index + " " + msg);
                    if (index >= colors.length) index = colors.length - 1;
                    else if (index < 0) index = 0;
                    //                            console.log("value:" + value+ " %:" + percent +" index:" + index +" c:" + colors[index]);

                    props.fillOpacity = 0.8;
                    props.fillColor = colors[index];
                    didColorBy = true;
                }

                var html = this.getRecordHtml(pointRecord, fields);
                if (showSegments && latField1 && latField2 && lonField1 && lonField2) {
                    var lat1 = values[latField1.getIndex()];
                    var lat2 = values[latField2.getIndex()];
                    var lon1 = values[lonField1.getIndex()];
                    var lon2 = values[lonField2.getIndex()];
                    var attrs = {};
                    if (props.fillColor)
                        attrs.strokeColor = props.fillColor;
                    else
                        attrs.strokeColor = lineColor;
                    attrs.strokeWidth = segmentWidth;
                    this.lines.push(this.map.addLine("line-" + i, "", lat1, lon1, lat2, lon2, attrs, html));
                    if (showEndPoints) {
                        var pointProps = {};
                        $.extend(pointProps, props);
                        pointProps.fillColor = attrs.strokeColor;
                        pointProps.strokeColor = attrs.strokeColor;
                        pointProps.pointRadius = dfltEndPointSize;
                        pointProps.pointRadius = endPointSize;
                        var p1 = new OpenLayers.LonLat(lon1, lat1);
                        var p2 = new OpenLayers.LonLat(lon2, lat2);
                        if (!Utils.isDefined(seen[p1])) {
                            seen[p1] = true;
                            var point = this.map.addPoint("endpt-" + i, p1, pointProps, html);
                            this.points.push(point);
                        }
                        if (!Utils.isDefined(seen[p2])) {
                            seen[p2] = true;
                            this.points.push(this.map.addPoint("endpt2-" + i, p2, pointProps, html));
                        }

                    }
                }

                if (showPoints) {
                    //We do this because openlayers gets really slow when there are lots of features at one point
                    if (!Utils.isDefined(seen[point])) seen[point] = 0;
                    if (seen[point] > 500) continue;
                    seen[point]++;
                    var mapPoint = this.map.addPoint("pt-" + i, point, props, html, dontAddPoint);
                    var date = pointRecord.getDate();
                    if (date) {
                        mapPoint.date = date.getTime();
                    }
                    this.points.push(mapPoint);
                }
            }
            if (didColorBy) {
                this.displayColorTable(colors, ID_BOTTOM, colorBy.origMinValue, colorBy.origMaxValue, {
                    stringValues: colorByValues
                });
            }

            this.applyVectorMap();
        },
        handleEventRemoveDisplay: function(source, display) {
            if (!this.map) {
                return;
            }
            var mapEntryInfo = this.mapEntryInfos[display];
            if (mapEntryInfo != null) {
                mapEntryInfo.removeFromMap(this.map);
            }
            var feature = this.findFeature(display, true);
            if (feature != null) {
                if (feature.line != null) {
                    this.map.removePolygon(feature.line);
                }
            }
        },
        findFeature: function(source, andDelete) {
            for (var i in this.features) {
                var feature = this.features[i];
                if (feature.source == source) {
                    if (andDelete) {
                        this.features.splice(i, 1);
                    }
                    return feature;
                }
            }
            return null;
        },

        getMarkerIcon: function() {
            if (this.getProperty("markerIcon")) {
                var icon = this.getProperty("markerIcon");
                if (icon.startsWith("/"))
                    return ramaddaBaseUrl + icon;
                else
                    return icon;
            }
            displayMapCurrentMarker++;
            if (displayMapCurrentMarker >= displayMapMarkers.length) displayMapCurrentMarker = 0;
            return ramaddaBaseUrl + "/lib/openlayers/v2/img/" + displayMapMarkers[displayMapCurrentMarker];
        },
        handleEventRecordSelection: function(source, args) {
            if (!this.getProperty("showRecordSelection", true)) return;
            if (!this.map) {
                return;
            }
            var record = args.record;
            if (record.hasLocation()) {
                var latitude = record.getLatitude();
                var longitude = record.getLongitude();
                if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) return;
                var point = new OpenLayers.LonLat(longitude, latitude);
                var marker = this.myMarkers[source];
                if (marker != null) {
                    this.map.removeMarker(marker);
                }
                var icon = displayMapMarkerIcons[source];
                if (icon == null) {
                    icon = this.getMarkerIcon();
                    displayMapMarkerIcons[source] = icon;
                }
                this.myMarkers[source] = this.map.addMarker(source.getId(), point, icon, "", args.html, null, 24);
            }
        }
    });
}

function MapEntryInfo(entry) {
    RamaddaUtil.defineMembers(this, {
        entry: entry,
        marker: null,
        rectangle: null,
        removeFromMap: function(map) {
            if (this.marker != null) {
                map.removeMarker(this.marker);
            }
            if (this.rectangle != null) {
                map.removePolygon(this.rectangle);
            }
            if (this.polygon != null) {
                map.removePolygon(this.polygon);
            }
            if (this.circle != null) {
                map.removePoint(this.circle);
            }
        }

    });
}/**
Copyright 2008-2019 Geode Systems LLC
*/



function RamaddaXlsDisplay(displayManager, id, properties) {

    var COORD_X = "xaxis";
    var COORD_Y = "yaxis";
    var COORD_GROUP = "group";


    var ID_SEARCH = "search";
    var ID_SEARCH_PREFIX = "table";
    var ID_SEARCH_EXTRA = "searchextra";
    var ID_SEARCH_HEADER = "searchheader";
    var ID_RESULTS = "results";
    var ID_DOWNLOADURL = "downloadurl";
    var ID_CONTENTS = "tablecontents";
    var ID_SEARCH_DIV = "searchdiv";
    var ID_SEARCH_FORM = "searchform";
    var ID_SEARCH_TEXT = "searchtext";
    var ID_TABLE_HOLDER = "tableholder";
    var ID_TABLE = "table";
    var ID_CHARTTOOLBAR = "charttoolbar";
    var ID_CHART = "chart";

    RamaddaUtil.inherit(this, new RamaddaDisplay(displayManager, id, "xls", properties));
    addRamaddaDisplay(this);


    this.url = properties.url;
    this.tableProps = {
        fixedRowsTop: 0,
        fixedColumnsLeft: 0,
        rowHeaders: true,
        colHeaders: true,
        headers: null,
        skipRows: 0,
        skipColumns: 0,
    };
    if (properties != null) {
        $.extend(this.tableProps, properties);
    }


    RamaddaUtil.defineMembers(this, {
        initDisplay: function() {
            this.createUI();
            this.setDisplayTitle("Table Data");
            var body =
                HtmlUtils.div(["id", this.getDomId(ID_SEARCH_HEADER)]) +
                HtmlUtils.div(["id", this.getDomId(ID_TABLE_HOLDER)]) +
                HtmlUtils.div(["id", this.getDomId(ID_CHARTTOOLBAR)]) +
                HtmlUtils.div(["id", this.getDomId(ID_CHART)]);
            this.setContents(body);
            this.loadTableData(this.url);
        },
    });


    RamaddaUtil.defineMembers(this, {
        currentSheet: 0,
        currentData: null,
        columnLabels: null,
        startRow: 0,
        groupIndex: -1,
        xAxisIndex: -1,
        yAxisIndex: -1,
        header: null,
        cellSelected: function(row, col) {
            this.startRow = row;
            if (this.jq("params-xaxis-select").attr("checked")) {
                this.xAxisIndex = col;
            } else if (this.jq("params-group-select").attr("checked")) {
                this.groupIndex = col;
            } else {
                this.yAxisIndex = col;
            }
            var label = "";
            var p1 = "";
            var p2 = "";

            this.setAxisLabel(COORD_X, this.getHeading(this.xAxisIndex, true));
            this.setAxisLabel(COORD_GROUP, this.getHeading(this.groupIndex, true));
            this.setAxisLabel(COORD_Y, this.getHeading(this.yAxisIndex, true));
        },
        getAxisLabelId: function(root) {
            return "params-" + root + "-label"
        },
        setAxisLabel: function(fieldId, lbl) {
            fieldId = this.getAxisLabelId(fieldId);
            var id = HtmlUtils.getUniqueId();
            if (lbl.length > 25) {
                lbl = lbl.substring(0, 25) + "...";
            }
            if (lbl.trim() != "") {
                lbl = HtmlUtils.span(["id", id, "class", "ramadda-tag-box"], "&nbsp;&nbsp;" + lbl + "&nbsp;&nbsp;");
            }
            this.jq(fieldId).html(lbl);
        },
        loadSheet: function(sheetIdx) {

            var all = $("[id^=" + this.getDomId("sheet_") + "]");
            var sel = $("#" + this.getDomId("sheet_") + sheetIdx);

            all.css('font-weight', 'normal');
            sel.css('font-weight', 'bold');

            all.css('border', '1px #ccc solid');
            sel.css('border', '1px #666 solid');

            this.currentSheet = sheetIdx;
            var sheet = this.sheets[sheetIdx];
            if (sheet) {
                var rows = sheet.rows.slice(0);
                if (rows.length > 0) {
                    this.header = rows[0];
                }
            }

            var html = "";
            var _this = this;
            var args = {
                contextMenu: true,
                stretchH: 'all',
                colHeaders: true,
                rowHeaders: true,
                minSpareRows: 1,
                afterSelection: function() {
                    if (arguments.length > 2) {
                        for (var i = 0; i < arguments.length; i++) {
                            //                                console.log("a[" + i +"]=" + arguments[i]);
                        }
                        var row = arguments[0];
                        var col = arguments[1];
                        _this.cellSelected(row, col);
                    }
                },
            };
            $.extend(args, this.tableProps);
            if (this.tableProps.useFirstRowAsHeader) {
                var headers = rows[0];
                args.colHeaders = headers;
                rows = rows.splice(1);
            }
            for (var i = 0; i < this.tableProps.skipRows; i++) {
                rows = rows.splice(1);
            }

            if (rows.length == 0) {
                this.displayMessage("No data found");
                this.jq(ID_RESULTS).html("");
                return;
            }

            this.jq(ID_RESULTS).html("Found: " + rows.length);
            args.data = rows;
            this.currentData = rows;

            if (this.tableProps.headers != null) {
                args.colHeaders = this.tableProps.headers;
            }

            if (this.getProperty("showTable", true)) {
                this.jq(ID_TABLE).handsontable(args);
            }

        },
        getDataForSheet: function(sheetIdx, args) {
            var sheet = this.sheets[sheetIdx];
            var rows = sheet.rows.slice(0);
            if (rows.length > 0) {
                this.header = rows[0];
            }

            if (this.tableProps.useFirstRowAsHeader) {
                var headers = rows[0];
                if (args) {
                    args.colHeaders = headers;
                }
                rows = rows.splice(1);
            }
            for (var i = 0; i < this.tableProps.skipRows; i++) {
                rows = rows.splice(1);
            }
            return rows;
        },

        makeChart: function(chartType, props) {
            if (typeof google == 'undefined') {
                this.jq(ID_CHART).html("No google chart available");
                return;
            }

            if (props == null) props = {};
            var xAxisIndex = Utils.getDefined(props.xAxisIndex, this.xAxisIndex);
            var groupIndex = Utils.getDefined(props.groupIndex, this.groupIndex);
            var yAxisIndex = Utils.getDefined(props.yAxisIndex, this.yAxisIndex);

            //                console.log("y:" + yAxisIndex +" props:" + props.yAxisIndex);

            if (yAxisIndex < 0) {
                alert("You must select a y-axis field.\n\nSelect the desired axis with the radio button.\n\nClick the column in the table to chart.");
                return;
            }

            var sheetIdx = this.currentSheet;
            if (!(typeof props.sheet == "undefined")) {
                sheetIdx = props.sheet;
            }

            var rows = this.getDataForSheet(sheetIdx);
            if (rows == null) {
                this.jq(ID_CHART).html("There is no data");
                return;
            }


            //remove the first header row
            var rows = rows.slice(1);

            for (var i = 0; i < this.startRow - 1; i++) {
                rows = rows.slice(1);
            }

            var subset = [];
            console.log("x:" + xAxisIndex + " " + " y:" + yAxisIndex + " group:" + groupIndex);
            for (var rowIdx = 0; rowIdx < rows.length; rowIdx++) {
                var row = [];
                var idx = 0;
                if (xAxisIndex >= 0) {
                    row.push(rows[rowIdx][xAxisIndex]);
                } else {
                    row.push(rowIdx);
                }
                if (yAxisIndex >= 0) {
                    row.push(rows[rowIdx][yAxisIndex]);
                }
                subset.push(row);
                if (rowIdx < 2)
                    console.log("row:" + row);
            }
            rows = subset;

            for (var rowIdx = 0; rowIdx < rows.length; rowIdx++) {
                var cols = rows[rowIdx];


                for (var colIdx = 0; colIdx < cols.length; colIdx++) {
                    var value = cols[colIdx] + "";
                    cols[colIdx] = parseFloat(value.trim());
                }
            }


            var lbl1 = this.getHeading(xAxisIndex, true);
            var lbl2 = this.getHeading(yAxisIndex, true);
            var lbl3 = this.getHeading(groupIndex, true);
            this.columnLabels = [lbl1, lbl2];


            var labels = this.columnLabels != null ? this.columnLabels : ["Field 1", "Field 2"];
            rows.splice(0, 0, labels);
            /*
            for(var rowIdx=0;rowIdx<rows.length;rowIdx++) {
                var cols = rows[rowIdx];
                var s = "";
                for(var colIdx=0;colIdx<cols.length;colIdx++) {
                    if(colIdx>0)
                        s += ", ";
                    s += "'" +cols[colIdx]+"'" + " (" + (typeof cols[colIdx]) +")";
                }
                console.log(s);
                if(rowIdx>5) break;
            }
            */

            var dataTable = google.visualization.arrayToDataTable(rows);
            var chartOptions = {};
            var width = "95%";
            $.extend(chartOptions, {
                legend: {
                    position: 'top'
                },
            });

            if (this.header != null) {
                if (xAxisIndex >= 0) {
                    chartOptions.hAxis = {
                        title: this.header[xAxisIndex]
                    };
                }
                if (yAxisIndex >= 0) {
                    chartOptions.vAxis = {
                        title: this.header[yAxisIndex]
                    };
                }
            }

            var chartDivId = HtmlUtils.getUniqueId();
            var divAttrs = ["id", chartDivId];
            if (chartType == "scatterplot") {
                divAttrs.push("style");
                divAttrs.push("width: 450px; height: 450px;");
            }
            this.jq(ID_CHART).append(HtmlUtils.div(divAttrs));

            if (chartType == "barchart") {
                chartOptions.chartArea = {
                    left: 75,
                    top: 10,
                    height: "60%",
                    width: width
                };
                chartOptions.orientation = "horizontal";
                this.chart = new google.visualization.BarChart(document.getElementById(chartDivId));
            } else if (chartType == "table") {
                this.chart = new google.visualization.Table(document.getElementById(chartDivId));
            } else if (chartType == "motion") {
                this.chart = new google.visualization.MotionChart(document.getElementById(chartDivId));
            } else if (chartType == "scatterplot") {
                chartOptions.chartArea = {
                    left: 50,
                    top: 30,
                    height: 400,
                    width: 400
                };
                chartOptions.legend = 'none';
                chartOptions.axisTitlesPosition = "in";
                this.chart = new google.visualization.ScatterChart(document.getElementById(chartDivId));
            } else {
                $.extend(chartOptions, {
                    lineWidth: 1
                });
                chartOptions.chartArea = {
                    left: 75,
                    top: 10,
                    height: "60%",
                    width: width
                };
                this.chart = new google.visualization.LineChart(document.getElementById(chartDivId));
            }
            if (this.chart != null) {
                this.chart.draw(dataTable, chartOptions);
            }
        },

        addNewChartListener: function(makeChartId, chartType) {
            var _this = this;
            $("#" + makeChartId + "-" + chartType).button().click(function(event) {
                console.log("make chart:" + chartType);
                _this.makeChart(chartType);
            });
        },

        makeSheetButton: function(id, index) {
            var _this = this;
            $("#" + id).button().click(function(event) {
                _this.loadSheet(index);
            });
        },
        clear: function() {
            this.jq(ID_CHART).html("");
            this.startRow = 0;
            this.groupIndex = -1;
            this.xAxisIndex = -1;
            this.yAxisIndex = -1;
            this.setAxisLabel(COORD_GROUP, "");
            this.setAxisLabel(COORD_X, "");
            this.setAxisLabel(COORD_Y, "");
        },
        getHeading: function(index, doField) {
            if (index < 0) return "";
            if (this.header != null && index >= 0 && index < this.header.length) {
                var v = this.header[index];
                v = v.trim();
                if (v.length > 0) return v;
            }
            if (doField)
                return "Field " + (index + 1);
            return "";
        },
        showTableData: function(data) {
            var _this = this;

            this.data = data;
            this.sheets = this.data.sheets;
            this.columns = data.columns;



            var buttons = "";
            for (var sheetIdx = 0; sheetIdx < this.sheets.length; sheetIdx++) {
                var id = this.getDomId("sheet_" + sheetIdx);
                buttons += HtmlUtils.div(["id", id, "class", "ramadda-xls-button-sheet"],
                    this.sheets[sheetIdx].name);

                buttons += "\n";
            }

            var weight = "12";

            var tableHtml = "<table width=100% style=\"max-width:1000px;\" > ";
            if (this.sheets.length > 1) {
                weight = "10";
            }

            tableHtml += "<tr valign=top>";

            if (this.sheets.length > 1) {
                //                    tableHtml += HtmlUtils.openTag(["class","col-md-2"]);
                tableHtml += HtmlUtils.td(["width", "140"], HtmlUtils.div(["class", "ramadda-xls-buttons"], buttons));
                weight = "10";
            }


            var makeChartId = HtmlUtils.getUniqueId();

            var tableWidth = this.getProperty("tableWidth", "");
            var tableHeight = this.getProperty("tableHeight", "500px");

            var style = "";
            if (tableWidth != "") {
                style += " width:" + tableWidth + ";";
            }
            style += " height: " + tableHeight + ";";
            style += " overflow: auto;";
            tableHtml += HtmlUtils.td([], HtmlUtils.div(["id", this.getDomId(ID_TABLE), "class", "ramadda-xls-table", "style", style]));


            tableHtml += "</tr>";
            tableHtml += "</table>";

            var chartToolbar = "";
            var chartTypes = ["barchart", "linechart", "scatterplot"];
            for (var i = 0; i < chartTypes.length; i++) {
                chartToolbar += HtmlUtils.div(["id", makeChartId + "-" + chartTypes[i], "class", "ramadda-xls-button"], "Make " + chartTypes[i]);
                chartToolbar += "&nbsp;";
            }

            chartToolbar += "&nbsp;";
            chartToolbar += HtmlUtils.div(["id", this.getDomId("removechart"), "class", "ramadda-xls-button"], "Clear Charts");


            chartToolbar += "<p>";
            chartToolbar += "<form>Fields: ";
            chartToolbar += "<input type=radio checked name=\"param\" id=\"" + this.getDomId("params-yaxis-select") + "\"> y-axis:&nbsp;" +
                HtmlUtils.div(["id", this.getDomId("params-yaxis-label"), "style", "border-bottom:1px #ccc dotted;min-width:10em;display:inline-block;"], "");

            chartToolbar += "&nbsp;&nbsp;&nbsp;";
            chartToolbar += "<input type=radio  name=\"param\" id=\"" + this.getDomId("params-xaxis-select") + "\"> x-axis:&nbsp;" +
                HtmlUtils.div(["id", this.getDomId("params-xaxis-label"), "style", "border-bottom:1px #ccc dotted;min-width:10em;display:inline-block;"], "");

            chartToolbar += "&nbsp;&nbsp;&nbsp;";
            chartToolbar += "<input type=radio  name=\"param\" id=\"" + this.getDomId("params-group-select") + "\"> group:&nbsp;" +
                HtmlUtils.div(["id", this.getDomId("params-group-label"), "style", "border-bottom:1px #ccc dotted;min-width:10em;display:inline-block;"], "");

            chartToolbar += "</form>";

            if (this.getProperty("showSearch", true)) {
                var results = HtmlUtils.div(["style", "display:inline-block;", "id", this.getDomId(ID_RESULTS)], "");
                var download = HtmlUtils.div(["style", "display:inline-block;", "id", this.getDomId(ID_DOWNLOADURL)]);
                var searchDiv = HtmlUtils.div(["id", this.getDomId(ID_SEARCH_DIV), "class", "ramadda-xls-search-form"]);


                var search = "";
                search += HtmlUtils.openTag("form", ["action", "#", "id", this.getDomId(ID_SEARCH_FORM)]);
                search += HtmlUtils.image(icon_tree_closed, ["id", this.getDomId(ID_SEARCH + "_open")]);
                search += "\n";
                search += HtmlUtils.input(ID_SEARCH_TEXT, this.jq(ID_SEARCH_TEXT).val(), ["size", "60", "id", this.getDomId(ID_SEARCH_TEXT), "placeholder", "Search"]);
                search += "<input type=submit name='' style='display:none;'>";

                search += HtmlUtils.openTag("div", ["id", this.getDomId(ID_SEARCH_EXTRA), "class", "ramadda-xls-search-extra"], "");
                if (this.columns) {
                    var extra = HtmlUtils.openTag("table", ["class", "formtable"]);
                    for (var i = 0; i < this.columns.length; i++) {
                        var col = this.columns[i];
                        var id = ID_SEARCH_PREFIX + "_" + col.name;
                        var widget = HtmlUtils.input(id, this.jq(id).val(), ["id", this.getDomId(id), "placeholder", "Search"]);
                        extra += HtmlUtils.formEntry(col.name.replace("_", " ") + ":", widget);
                    }
                    extra += HtmlUtils.closeTag("table");
                    search += extra;
                }


                if (this.searchFields) {
                    var extra = HtmlUtils.openTag("table", ["class", "formtable"]);
                    for (var i = 0; i < this.searchFields.length; i++) {
                        var col = this.searchFields[i];
                        var id = ID_SEARCH_PREFIX + "_" + col.name;
                        var widget = HtmlUtils.input(id, this.jq(id).val(), ["id", this.getDomId(id), "placeholder", "Search"]);
                        extra += HtmlUtils.formEntry(col.label + ":", widget);
                    }
                    extra += HtmlUtils.closeTag("table");
                    search += extra;
                }




                search += "\n";
                search += HtmlUtils.closeTag("div");
                search += "\n";
                search += HtmlUtils.closeTag("form");

                this.jq(ID_SEARCH_HEADER).html(HtmlUtils.leftRight(searchDiv, results + " " + download));

                this.jq(ID_SEARCH_DIV).html(search);

                if (!this.extraOpen) {
                    this.jq(ID_SEARCH_EXTRA).hide();
                }


                this.jq(ID_SEARCH + "_open").button().click(function(event) {
                    _this.jq(ID_SEARCH_EXTRA).toggle();
                    _this.extraOpen = !_this.extraOpen;
                    if (_this.extraOpen) {
                        _this.jq(ID_SEARCH + "_open").attr("src", icon_tree_open);
                    } else {
                        _this.jq(ID_SEARCH + "_open").attr("src", icon_tree_closed);
                    }
                });

            }


            if (this.getProperty("showTable", true)) {
                this.jq(ID_TABLE_HOLDER).html(tableHtml);
                chartToolbar += "<br>";
                if (this.getProperty("showChart", true)) {
                    this.jq(ID_CHARTTOOLBAR).html(chartToolbar);
                }
            }

            if (this.getProperty("showSearch", true)) {
                this.jq(ID_SEARCH_FORM).submit(function(event) {
                    event.preventDefault();
                    _this.loadTableData(_this.url, "Searching...");
                });
                this.jq(ID_SEARCH_TEXT).focus();
                this.jq(ID_SEARCH_TEXT).select();
            }


            for (var i = 0; i < chartTypes.length; i++) {
                this.addNewChartListener(makeChartId, chartTypes[i]);
            }
            this.jq("removechart").button().click(function(event) {
                _this.clear();
            });

            for (var sheetIdx = 0; sheetIdx < this.sheets.length; sheetIdx++) {
                var id = this.getDomId("sheet_" + sheetIdx);
                this.makeSheetButton(id, sheetIdx);
            }
            var sheetIdx = 0;
            var rx = /sheet=([^&]+)/g;
            var arr = rx.exec(window.location.search);
            if (arr) {
                sheetIdx = arr[1];
            }
            this.loadSheet(sheetIdx);


            if (this.defaultCharts) {
                for (var i = 0; i < this.defaultCharts.length; i++) {
                    var dflt = this.defaultCharts[i];
                    this.makeChart(dflt.type, dflt);
                }
            }
            this.setAxisLabel("params-yaxis-label", this.getHeading(this.yAxisIndex, true));

            this.displayDownloadUrl();

        },
        displayMessage: function(msg, icon) {
            if (!icon) {
                icon = icon_information;
            }
            var html = HtmlUtils.hbox(HtmlUtils.image(icon, ["align", "left"]),
                HtmlUtils.inset(msg, 10, 10, 5, 10));
            html = HtmlUtils.div(["class", "note"], html);
            this.jq(ID_TABLE_HOLDER).html(html);
        },
        displayDownloadUrl: function() {
            var url = this.lastUrl;
            if (url == null) {
                this.jq(ID_DOWNLOADURL).html("");
                return
            }
            url = url.replace("xls_json", "media_tabular_extractsheet");
            url += "&execute=true";
            var img = HtmlUtils.image(ramaddaBaseUrl + "/icons/xls.png", ["title", "Download XLSX"]);
            this.jq(ID_DOWNLOADURL).html(HtmlUtils.href(url, img));
        },
        loadTableData: function(url, message) {
            this.url = url;
            if (!message) message = this.getLoadingMessage();
            this.displayMessage(message, icon_progress);
            var _this = this;

            var text = this.jq(ID_SEARCH_TEXT).val();
            if (text && text != "") {
                url = url + "&table.text=" + encodeURIComponent(text);
            }
            if (this.columns) {
                for (var i = 0; i < this.columns.length; i++) {
                    var col = this.columns[i];
                    var id = ID_SEARCH_PREFIX + "_" + col.name;
                    var text = this.jq(id).val();
                    if (text) {
                        url = url + "&table." + col.name + "=" + encodeURIComponent(text);
                    }
                }
            }

            if (this.searchFields) {
                for (var i = 0; i < this.searchFields.length; i++) {
                    var col = this.searchFields[i];
                    var id = ID_SEARCH_PREFIX + "_" + col.name;
                    var text = this.jq(id).val();
                    if (text) {
                        url = url + "&table." + col.name + "=" + encodeURIComponent(text);
                    }
                }
            }



            console.log("url:" + url);
            this.lastUrl = url;

            var jqxhr = $.getJSON(url, function(data) {
                    if (GuiUtils.isJsonError(data)) {
                        _this.displayMessage("Error: " + data.error);
                        return;
                    }
                    _this.showTableData(data);
                })
                .fail(function(jqxhr, textStatus, error) {
                    var err = textStatus + ", " + error;
                    _this.displayMessage("An error occurred: " + error);
                    console.log("JSON error:" + err);
                });
        }
    });

}/**
Copyright 2008-2019 Geode Systems LLC
*/

var CATEGORY_PLOTLY = "More Charts";
var DISPLAY_PLOTLY_RADAR = "radar";
var DISPLAY_PLOTLY_WINDROSE = "windrose";
var DISPLAY_PLOTLY_DENSITY = "density";
var DISPLAY_PLOTLY_DOTPLOT = "dotplot";
var DISPLAY_PLOTLY_SPLOM = "splom";
var DISPLAY_PLOTLY_3DSCATTER = "3dscatter";
var DISPLAY_PLOTLY_3DMESH = "3dmesh";
var DISPLAY_PLOTLY_TREEMAP = "ptreemap";
var DISPLAY_PLOTLY_TERNARY = "ternary";

addGlobalDisplayType({
    type: DISPLAY_PLOTLY_RADAR,
    label: "Radar",
    requiresData: true,
    forUser: true,
    category: CATEGORY_PLOTLY
});
addGlobalDisplayType({
    type: DISPLAY_PLOTLY_WINDROSE,
    label: "Wind Rose",
    requiresData: true,
    forUser: true,
    category: CATEGORY_PLOTLY
});
addGlobalDisplayType({
    type: DISPLAY_PLOTLY_DENSITY,
    label: "Density",
    requiresData: true,
    forUser: true,
    category: CATEGORY_PLOTLY
});
addGlobalDisplayType({
    type: DISPLAY_PLOTLY_DOTPLOT,
    label: "Dot Plot",
    requiresData: true,
    forUser: true,
    category: CATEGORY_PLOTLY
});
addGlobalDisplayType({
    type: DISPLAY_PLOTLY_SPLOM,
    label: "Splom",
    requiresData: true,
    forUser: true,
    category: CATEGORY_PLOTLY
});
addGlobalDisplayType({
    type: DISPLAY_PLOTLY_3DSCATTER,
    label: "3D Scatter",
    requiresData: true,
    forUser: true,
    category: CATEGORY_PLOTLY
});
addGlobalDisplayType({
    type: DISPLAY_PLOTLY_3DMESH,
    label: "3D Mesh",
    requiresData: true,
    forUser: true,
    category: CATEGORY_PLOTLY
});
//Ternary doesn't work
//addGlobalDisplayType({type: DISPLAY_PLOTLY_TERNARY, label:"Ternary",requiresData:true,forUser:true,category:CATEGORY_PLOTLY});
//Treempap doesn't work
//addGlobalDisplayType({type: DISPLAY_PLOTLY_PTREEMAP, label:"Tree Map",requiresData:true,forUser:true,category:CATEGORY_PLOTLY});


function RamaddaPlotlyDisplay(displayManager, id, type, properties) {
    let SUPER = new RamaddaFieldsDisplay(displayManager, id, type, properties);
    RamaddaUtil.inherit(this, SUPER);
    RamaddaUtil.defineMembers(this, {
        needsData: function() {
            return true;
        },
        setDimensions: function(layout, widthDelta) {
            //                var width  = parseInt(this.getProperty("width","400").replace("px","").replace("%",""));
            var height = parseInt(this.getProperty("height", "400").replace("px", "").replace("%", ""));
            //                layout.width = width-widthDelta;
            layout.height = height;
        },
        pointDataLoaded: function(pointData, url, reload) {
            SUPER.pointDataLoaded.call(this, pointData, url, reload);
            if (this.dataCollection)
                this.displayManager.propagateEventRecordSelection(this,
                    this.dataCollection.getList()[0], {
                        index: 0
                    });

        },
        displayData: function() {
            this.updateUI();
        },
        pageHasLoaded: function() {
            SUPER.pageHasLoaded.call(this);
            this.updateUI();
        },

        fieldSelectionChanged: function() {
            SUPER.fieldSelectionChanged.call(this);
            this.updateUI();
        },
        makeAxis: function(title, tickangle) {
            return {
                title: title,
                titlefont: {
                    size: 20
                },
                tickangle: tickangle,
                tickfont: {
                    size: 15
                },
                tickcolor: 'rgba(0,0,0,0)',
                ticklen: 5,
                showline: true,
                showgrid: true
            };
        },
        getDisplayStyle: function() {
            return "";
        },
        makePlot: function(data, layout) {
            this.clearHtml();
            //For some reason plotly won't display repeated times in the DISPLAY div
            this.jq(ID_DISPLAY_CONTENTS).html(HtmlUtils.div(["id", this.getDomId("tmp"), "style", this.getDisplayStyle()], ""));
            //               Plotly.plot(this.getDomId(ID_DISPLAY_CONTENTS), data, layout)
            var plot = Plotly.plot(this.getDomId("tmp"), data, layout);
            var myPlot = document.getElementById(this.getDomId("tmp"));
            this.addEvents(plot, myPlot);
        },
        addEvents: function(plot, myPlot) {}
    });
}



function RamaddaRadialDisplay(displayManager, id, type, properties) {
    var SUPER;
    $.extend(this, {
        width: "400px",
        height: "400px",
    });
    RamaddaUtil.inherit(this, SUPER = new RamaddaPlotlyDisplay(displayManager, id, type, properties));
    RamaddaUtil.defineMembers(this, {
        getPlotType: function() {
            return 'barpolar';
        },
        updateUI: function() {
            var records = this.filterData();
            if (!records) {
                return;
            }
            var fields = this.getSelectedFields(this.getData().getRecordFields());
            var stringField = this.getFieldOfType(fields, "string");
            if (!stringField) {
                this.displayError("No string field specified");
                return;
            }
            var numericFields = this.getFieldsOfType(fields, "numeric");
            if (numericFields.length == 0) {
                this.displayError("No numeric fields specified");
                return;
            }
            var theta = this.getColumnValues(records, stringField).values;
            var values = [];
            var min = Number.MAX_VALUE;
            var max = Number.MIN_VALUE;
            var plotData = [];
            for (var i = 0; i < numericFields.length; i++) {
                var field = numericFields[i];
                var column = this.getColumnValues(records, field);
                min = Math.min(min, column.min);
                max = Math.max(max, column.max);
                plotData.push({
                    type: this.getPlotType(),
                    r: column.values,
                    theta: theta,
                    fill: 'toself',
                    name: field.getLabel()
                });
            }

            layout = {
                polar: {
                    radialaxis: {
                        visible: true,
                        range: [min, max]
                    }
                },
            }
            this.setDimensions(layout, 2);
            this.makePlot(plotData, layout);
        },
    });
}

function RamaddaRadarDisplay(displayManager, id, properties) {
    var SUPER;
    RamaddaUtil.inherit(this, SUPER = new RamaddaRadialDisplay(displayManager, id, DISPLAY_PLOTLY_RADAR, properties));

    RamaddaUtil.defineMembers(this, {
        getPlotType: function() {
            return 'scatterpolar';
        },
    });
    addRamaddaDisplay(this);
}

function RamaddaWindroseDisplay(displayManager, id, properties) {
    var SUPER;
    RamaddaUtil.inherit(this, SUPER = new RamaddaRadialDisplay(displayManager, id, DISPLAY_PLOTLY_WINDROSE, properties));
    RamaddaUtil.defineMembers(this, {
        getPlotType: function() {
            return 'barpolar';
        },
    });
    addRamaddaDisplay(this);
}




function RamaddaDensityDisplay(displayManager, id, properties) {
    var SUPER;
    $.extend(this, {
        width: "600px",
        height: "400px",
    });
    RamaddaUtil.inherit(this, SUPER = new RamaddaPlotlyDisplay(displayManager, id, DISPLAY_PLOTLY_DENSITY, properties));
    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
        updateUI: function() {
            var records = this.filterData();
            if (!records) return;
            var fields = this.getSelectedFields(this.getData().getRecordFields());
            var numericFields = this.getFieldsOfType(fields, "numeric");
            if (numericFields.length < 2) {
                this.displayError("No numeric fields specified");
                return;
            }

            var x = this.getColumnValues(records, numericFields[0]);
            var y = this.getColumnValues(records, numericFields[1]);
            var markers = {
                x: x.values,
                y: y.values,
                mode: 'markers',
                name: "",
                marker: {
                    color: this.getProperty("pointColor", 'rgb(102,0,0)'),
                    size: parseInt(this.getProperty("markerSize", "4")),
                    opacity: 0.4
                },
                type: 'scatter'
            };
            var density = {
                x: x.values,
                y: y.values,
                name: 'density',
                ncontours: 20,
                colorscale: 'Hot',
                reversescale: true,
                type: 'histogram2dcontour'
            };
            var plotData = [];
            if (this.getProperty("showDensity", true))
                plotData.push(density);
            if (this.getProperty("showPoints", true))
                plotData.push(markers);
            var layout = {
                showlegend: true,
                autosize: true,
                margin: {
                    t: 50
                },
                hovermode: 'closest',
                bargap: 0,
                xaxis: {
                    domain: [x.min, x.max],
                    showline: this.getProperty("showLines", true),
                    showgrid: this.getProperty("showLines", true),
                    title: fields[0].getLabel()
                },
                yaxis: {
                    domain: [y.min, y.max],
                    showline: this.getProperty("showLines", true),
                    showgrid: this.getProperty("showLines", true),
                    title: fields[1].getLabel()
                },
            };
            this.setDimensions(layout, 2);
            this.makePlot(plotData, layout);
        },
    });
}


function RamaddaPlotly3DDisplay(displayManager, id, type, properties) {
    var SUPER;
    $.extend(this, {
        width: "400px",
        height: "400px",
    });
    RamaddaUtil.inherit(this, SUPER = new RamaddaPlotlyDisplay(displayManager, id, type, properties));

    RamaddaUtil.defineMembers(this, {
        addEvents: function(plot, myPlot) {
            myPlot.on('plotly_click', function() {
                //                        alert('You clicked this Plotly chart!');
            });
        },

        getDisplayStyle: function() {
            return "border: 1px #ccc solid;";
        },
        get3DType: function() {
            //                'mesh3d'
            return 'scatter3d';
        },
        updateUI: function() {
            var records = this.filterData();
            if (!records) return;
            var fields = this.getSelectedFields(this.getData().getRecordFields());
            var stringField = this.getFieldOfType(fields, "string");
            var fields = this.getFieldsOfType(fields, "numeric");
            if (fields.length == 0) {
                this.displayError("No numeric fields specified");
                return;
            }
            if (fields.length < 3) {
                this.displayError("Don't have 3 numeric fields specified");
                return;
            }
            var x = this.getColumnValues(records, fields[0]);
            var y = this.getColumnValues(records, fields[1]);
            var z = this.getColumnValues(records, fields[2]);

            var trace1 = {
                x: x.values,
                y: y.values,
                z: z.values,
                mode: 'markers',
                marker: {
                    size: 12,
                    line: {
                        color: 'rgba(217, 217, 217, 0.14)',
                        width: 0.5
                    },
                    opacity: 0.8
                },
                type: this.get3DType()
            };


            var plotData = [trace1];


            var layout = {
                scene: {
                    xaxis: {
                        backgroundcolor: "rgb(200, 200, 230)",
                        gridcolor: "rgb(255, 255, 255)",
                        showbackground: true,
                        zerolinecolor: "rgb(255, 255, 255)",
                        title: fields[0].getLabel(),
                    },
                    yaxis: {
                        backgroundcolor: "rgb(230, 200,230)",
                        gridcolor: "rgb(255, 255, 255)",
                        showbackground: true,
                        zerolinecolor: "rgb(255, 255, 255)",
                        title: fields[1].getLabel(),
                    },
                    zaxis: {
                        backgroundcolor: "rgb(230, 230,200)",
                        gridcolor: "rgb(255, 255, 255)",
                        showbackground: true,
                        zerolinecolor: "rgb(255, 255, 255)",
                        title: fields[2].getLabel(),
                    }
                },
                margin: {
                    l: 0,
                    r: 0,
                    b: 50,
                    t: 50,
                    pad: 4
                },
            };
            this.setDimensions(layout, 2);
            this.makePlot(plotData, layout);
        },
    });
}


function Ramadda3dmeshDisplay(displayManager, id, properties) {
    var SUPER;
    $.extend(this, {
        width: "100%",
        height: "100%",
    });
    RamaddaUtil.inherit(this, SUPER = new RamaddaPlotly3DDisplay(displayManager, id, DISPLAY_PLOTLY_3DMESH, properties));
    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
        get3DType: function() {
            return 'mesh3d';
        },
    });
}



function Ramadda3dscatterDisplay(displayManager, id, properties) {
    var SUPER;
    $.extend(this, {
        width: "100%",
        height: "100%",
    });
    RamaddaUtil.inherit(this, SUPER = new RamaddaPlotly3DDisplay(displayManager, id, DISPLAY_PLOTLY_3DSCATTER, properties));
    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
        get3DType: function() {
            return 'scatter3d';
        },
    });
}




function RamaddaTernaryDisplay(displayManager, id, properties) {
    var SUPER;
    $.extend(this, {
        width: "400px",
        height: "400px",
    });
    RamaddaUtil.inherit(this, SUPER = new RamaddaPlotlyDisplay(displayManager, id, DISPLAY_PLOTLY_TERNARY, properties));
    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
        updateUI: function() {
            var records = this.filterData();
            if (!records) return;
            var fields = this.getSelectedFields(this.getData().getRecordFields());
            var stringField = this.getFieldOfType(fields, "string");
            var fields = this.getFieldsOfType(fields, "numeric");
            if (fields.length == 0) {
                this.displayError("No numeric fields specified");
                return;
            }
            if (fields.length < 3) {
                this.displayError("Don't have 3 numeric fields specified");
                return;
            }

            var rawData = [];
            var a = this.getColumnValues(records, fields[0]);
            var b = this.getColumnValues(records, fields[1]);
            var c = this.getColumnValues(records, fields[2]);
            for (var i = 0; i < a.length; i++) {
                rawData.push({
                    a: 100 * a[i] / a.max,
                    b: 100 * b[i] / b.max,
                    c: 100 * c[i] / c.max,

                    label: stringField ? stringField.getLabel() : "Point " + (i + 1)
                });
            }
            var xrawData = [{
                a: 75,
                b: 25,
                c: 0,
                label: 'point 1'
            }, {
                a: 70,
                b: 10,
                c: 20,
                label: 'point 2'
            }, {
                a: 75,
                b: 20,
                c: 5,
                label: 'point 3'
            }, {
                a: 5,
                b: 60,
                c: 35,
                label: 'point 4'
            }, {
                a: 10,
                b: 80,
                c: 10,
                label: 'point 5'
            }, {
                a: 10,
                b: 90,
                c: 0,
                label: 'point 6'
            }, {
                a: 20,
                b: 70,
                c: 10,
                label: 'point 7'
            }, {
                a: 10,
                b: 20,
                c: 70,
                label: 'point 8'
            }, {
                a: 15,
                b: 5,
                c: 80,
                label: 'point 9'
            }, {
                a: 10,
                b: 10,
                c: 80,
                label: 'point 10'
            }, {
                a: 20,
                b: 10,
                c: 70,
                label: 'point 11'
            }, ];


            var plotData = [{
                type: 'scatterternary',
                mode: 'markers',
                a: rawData.map(function(d) {
                    return d.a;
                }),
                b: rawData.map(function(d) {
                    return d.b;
                }),
                c: rawData.map(function(d) {
                    return d.c;
                }),
                text: rawData.map(function(d) {
                    return d.label;
                }),
                marker: {
                    symbol: 100,
                    color: '#DB7365',
                    size: 14,
                    line: {
                        width: 2
                    }
                },
            }];
            var layout = {
                ternary: {
                    sum: 100,
                    aaxis: this.makeAxis(fields[0].getLabel(), 0),
                    baxis: this.makeAxis(fields[1].getLabel(), 45),
                    caxis: this.makeAxis(fields[2].getLabel(), -45),
                    bgcolor: '#fff1e0'
                },
                annotations: [{
                    showarrow: false,
                    text: this.getProperty("chartTitle", ""),
                    x: 1.0,
                    y: 1.3,
                    font: {
                        size: 15
                    }
                }],
                paper_bgcolor: '#fff1e0',
            };


            this.setDimensions(layout, 2);
            this.makePlot(plotData, layout);
        },
    });
}


function RamaddaDotplotDisplay(displayManager, id, properties) {

    $.extend(this, {
        width: "600px",
        height: "400px",
    });
    let SUPER = new RamaddaPlotlyDisplay(displayManager, id, DISPLAY_PLOTLY_DOTPLOT, properties);
    RamaddaUtil.inherit(this, SUPER);


    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
        getDisplayStyle: function() {
            return "";
            return "border: 1px #ccc solid;";
        },

        updateUI: function() {
            var records = this.filterData();
            if (!records) return;
            var fields = this.getSelectedFields(this.getData().getRecordFields());
            var stringField = this.getFieldOfType(fields, "string");
            if (!stringField) {
                stringField = fields[0];
            }

            var fields = this.getFieldsOfType(fields, "numeric");
            if (fields.length == 0) {
                this.displayError("No numeric fields specified");
                return;
            }

            var labels = null;
            var labelName = "";
            if (stringField) {
                labels = this.getColumnValues(records, stringField).values;
                labelName = stringField.getLabel();
            }
            var colors = this.getColorTable(true);
            if (!colors)
                colors = ['rgba(156, 165, 196, 0.95)', 'rgba(204,204,204,0.95)', 'rgba(255,255,255,0.85)', 'rgba(150,150,150,0.95)']
            var plotData = [];
            for (i in fields) {
                var color = i >= colors.length ? colors[0] : colors[i];
                var field = fields[i];
                var values = this.getColumnValues(records, field).values;
                if (!labels) {
                    labels = [];
                    for (var j = 0; j < values.length; j++) {
                        labels.push("Point " + (j + 1));
                    }
                }

                plotData.push({
                    type: 'scatter',
                    x: values,
                    y: labels,
                    mode: 'markers',
                    name: field.getLabel(),
                    marker: {
                        color: color,
                        line: {
                            color: 'rgba(156, 165, 196, 1.0)',
                            width: 1,
                        },
                        symbol: 'circle',
                        size: 16
                    }
                });
            }



            var layout = {
                title: '',
                yaxis: {
                    title: this.getProperty("yAxisTitle", labelName),
                    showline: this.getProperty("yAxisShowLine", true),
                    showgrid: this.getProperty("yAxisShowGrid", true),
                },
                xaxis: {
                    title: this.getProperty("xAxisTitle", fields[i].getLabel()),
                    showgrid: this.getProperty("xAxisShowGrid", false),
                    showline: this.getProperty("xAxisShowLine", true),
                    linecolor: 'rgb(102, 102, 102)',
                    titlefont: {
                        font: {
                            color: 'rgb(204, 204, 204)'
                        }
                    },
                    tickfont: {
                        font: {
                            color: 'rgb(102, 102, 102)'
                        }
                    },
                    autotick: true,
                    ticks: 'outside',
                    tickcolor: 'rgb(102, 102, 102)'
                },
                margin: {
                    l: this.getProperty("marginLeft", 140),
                    r: this.getProperty("marginRight", 40),
                    b: this.getProperty("marginBottom", 50),
                    t: this.getProperty("marginTop", 20),
                },
                legend: {
                    font: {
                        size: 10,
                    },
                    yanchor: 'middle',
                    xanchor: 'right'
                },
                paper_bgcolor: this.getProperty("chart.fill", 'rgb(254, 247, 234)'),
                plot_bgcolor: this.getProperty("chartArea.fill", 'rgb(254, 247, 234)'),
                hovermode: 'closest'
            };
            this.setDimensions(layout, 2);
            this.makePlot(plotData, layout);
        },
    });
}



function RamaddaSplomDisplay(displayManager, id, properties) {
    var SUPER;
    $.extend(this, {
        width: "600px",
        height: "600px",
    });
    RamaddaUtil.inherit(this, SUPER = new RamaddaPlotlyDisplay(displayManager, id, DISPLAY_PLOTLY_SPLOM, properties));

    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
        setDimensions: function(layout, widthDelta) {
            var width = parseInt(this.getProperty("width", "400").replace("px", "").replace("%", ""));
            var height = parseInt(this.getProperty("height", "400").replace("px", "").replace("%", ""));
            layout.width = width - widthDelta;
            layout.height = height;
        },
        makeAxis: function() {
            return {
                showline: false,
                zeroline: false,
                gridcolor: this.getProperty("gridColor", "white"),
                ticklen: 2,
                tickfont: {
                    size: this.getProperty("tickFontSize", 12)
                },
                titlefont: {
                    size: this.getProperty("titleFontSize", 12)
                }
            }
        },
        updateUI: function() {
            var records = this.filterData();
            if (!records) return;
            var fields = this.getSelectedFields(this.getData().getRecordFields());
            if (fields.length == 0) {
                fields = this.getData().getRecordFields();
            }
            var labels;
            if (this.getProperty("labels"))
                labels = this.getProperty("labels").split(",");

            var dataObj = {
                type: 'splom',
                dimensions: [],
                marker: {
                    size: parseInt(this.getProperty("markerSize", 5)),
                    line: {
                        color: this.getProperty("lineColor", 'white'),
                        width: 0.5
                    }
                }
            };


            var colorByField = this.getFieldById(fields, this.getProperty("colorBy"));
            var colorBy = this.getProperty("colorBy");
            if (colorBy) {
                var colorByField = this.getFieldById(fields, colorBy);
                if (colorByField) {
                    var obj = this.getColumnValues(records, colorByField);
                    var colors = this.getColorTable();
                    if (!colors) colors = Utils.getColorTable("blue_white_red");
                    var colorscale = [];
                    var min = parseFloat(this.getProperty("colorByMin", obj.min));
                    var max = parseFloat(this.getProperty("colorByMax", obj.max));
                    if (Utils.isDefined(colors.min)) {
                        var clippedColors = [];
                        for (var i = 0; i < colors.colors.length; i++) {
                            var percent = i / colors.colors.length;
                            var value = colors.min + (colors.max - colors.min) * percent;
                            if (value >= min && value <= max)
                                clippedColors.push(colors.colors[i]);
                        }
                        colors = clippedColors;
                    }
                    if (colors.colors) colors = colors.colors;
                    var range = max - min;
                    var colorValues = [];
                    for (var i = 0; i < obj.values.length; i++) {
                        var value = obj.values[i];
                        var percent = (value - min) / range;
                        colorValues.push(percent);
                    }
                    for (var i = 0; i < colors.length; i++) {
                        var value = i / colors.length;
                        var next = (i + 1) / colors.length;
                        colorscale.push([value, colors[i]]);
                        colorscale.push([next, colors[i]]);
                    }
                    dataObj.marker.color = colorValues;
                    dataObj.marker.colorscale = colorscale;
                }

                this.displayColorTable(colors, ID_DISPLAY_BOTTOM, min, max);
            }

            var stringField = this.getFieldOfType(fields, "string");
            if (stringField) {
                var l = this.getColumnValues(records, stringField).values;
                dataObj.text = [];
                for (var i = 0; i < l.length; i++)
                    dataObj.text.push(stringField.getLabel() + ": " + l[i]);
            }

            var plotData = [dataObj];
            var layout = {
                autosize: false,
                hovermode: 'closest',
                dragmode: 'select',
                plot_bgcolor: this.getProperty("bgColor", 'rgba(240,240,240, 0.95)'),
                margin: {
                    l: this.getProperty("marginLeft", 140),
                    r: this.getProperty("marginRight", 40),
                    b: this.getProperty("marginBottom", 50),
                    t: this.getProperty("marginTop", 20),
                },
            }

            var cnt = 0;
            for (var i = 0; i < fields.length; i++) {
                var field = fields[i];
                if (!field.isFieldNumeric()) continue;
                var values = this.getColumnValues(records, field).values;
                var label;
                if (labels && i < labels.length)
                    label = labels[i];
                else
                    label = field.getUnitLabel();
                dataObj.dimensions.push({
                    label: label,
                    values: values
                });
                var key = "axis" + (cnt == 0 ? "" : "" + (cnt + 1));
                layout["x" + key] = this.makeAxis();
                layout["y" + key] = this.makeAxis();
                cnt++;
            }
            this.setDimensions(layout, 2);
            this.makePlot(plotData, layout);
        },
    });
}



function RamaddaPTreemapDisplay(displayManager, id, properties) {
    var SUPER;
    $.extend(this, {
        width: "400px",
        height: "400px",
    });
    RamaddaUtil.inherit(this, SUPER = new RamaddaPlotlyDisplay(displayManager, id, DISPLAY_PLOTLY_TREEMAP, properties));
    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
        updateUI: function() {
            var records = this.filterData();
            if (!records) return;
            var selectedFields = this.getSelectedFields(this.getData().getRecordFields());
            var field = this.getFieldOfType(selectedFields, "numeric");
            if (!field) {
                this.displayError("No numeric field specified");
                return;
            }
            var values = this.getColumnValues(records, field).data;
            // declaring arrays
            var shapes = [];
            var annotations = [];
            var counter = 0;

            // For Hover Text
            var x_trace = [];
            var y_trace = [];
            var text = [];

            //colors
            var colors = this.getColorTable();
            if (colors.colors) colors = colors.colors;

            // Generate Rectangles using Treemap-Squared
            var rectangles = Treemap.generate(values, 100, 100);

            for (var i in rectangles) {
                var shape = {
                    type: 'rect',
                    x0: rectangles[i][0],
                    y0: rectangles[i][1],
                    x1: rectangles[i][2],
                    y1: rectangles[i][3],
                    line: {
                        width: 2
                    },
                    fillcolor: colors[counter]
                };
                shapes.push(shape);
                var annotation = {
                    x: (rectangles[i][0] + rectangles[i][2]) / 2,
                    y: (rectangles[i][1] + rectangles[i][3]) / 2,
                    text: String(values[counter]),
                    showarrow: false
                };
                annotations.push(annotation);

                // For Hover Text
                x_trace.push((rectangles[i][0] + rectangles[i][2]) / 2);
                y_trace.push((rectangles[i][1] + rectangles[i][3]) / 2);
                text.push(String(values[counter]));

                // Incrementing Counter
                counter++;
            }

            // Generating Trace for Hover Text
            var trace0 = {
                x: x_trace,
                y: y_trace,
                text: text,
                mode: 'text',
                type: 'scatter'
            };

            var layout = {
                height: 700,
                width: 700,
                shapes: shapes,
                hovermode: 'closest',
                annotations: annotations,
                xaxis: {
                    showgrid: false,
                    zeroline: false
                },
                yaxis: {
                    showgrid: false,
                    zeroline: false
                }
            };

            var data = {
                data: [trace0]
            };
            this.setDimensions(layout, 2);
            this.makePlot([trace0], layout);
        },
    });





}
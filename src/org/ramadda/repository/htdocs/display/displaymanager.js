/**
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
    var ID_ENTRIES_MENU = "entries_menu";

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
		var fields = mapDisplay.getFieldsByIds(null, mapDisplay.getProperty("filterFieldsToPropagate"));
		fields.map(field=>{
		    var args = {
			property: "filterValue",
			fieldId:field.getId(),
			value:closest.getValue(field.getIndex())
		    };
		    mapDisplay.propagateEvent("handleEventPropertyChanged", args);
		});

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
	    if(!this.getShowMenu()) {
		return "";
	    }
//            if (!this.getProperty(PROP_SHOW_MENU, true)) {
//                return "";
//            }
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
                HtmlUtils.tag(TAG_LI, [], HtmlUtils.onClick(layout + ".showWikiText();", "Show Text")) + "\n" +
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
            var displayId = props.displayId;
	    if(!displayId) 
		displayId = this.getUniqueId("display");
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
	    if(props.dummy) return display;
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
	handleEntryMenu: async function(entryId) {
            await getGlobalRamadda().getEntry(entryId, e => {
		var displays = this.getLayoutManager().getDisplays();
		for (var i = 0; i < displays.length; i++) {
		    var display = displays[i];
		    display.setEntry(e);
		}
	    });

	}
    });

    addDisplayManager(this);


    var displaysHtml = HtmlUtils.div([ATTR_ID, this.getDomId(ID_DISPLAYS), ATTR_CLASS, "display-container"]);
    var html = HtmlUtils.openTag(TAG_DIV);
    html += HtmlUtils.div(["id", this.getDomId(ID_MENU_CONTAINER)]);
    if(argProperties && argProperties.entryCollection) {
	let entries = argProperties.entryCollection.split(",");
	let enums = [];
	entries.map(t=>{
	    var toks = t.split(":");
	    enums.push([toks[0],toks[1]]);
	});
	html += (argProperties.changeEntriesLabel||"Change Entry: ") + HtmlUtils.select("",[ATTR_ID, this.getDomId(ID_ENTRIES_MENU)],enums);
    }

    //    html += this.makeMainMenu();
    if(this.getShowMenu()) {
//    if (this.getProperty(PROP_SHOW_MENU, true)) {
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
    this.jq(ID_ENTRIES_MENU).change(e=>{
	var entry = this.jq(ID_ENTRIES_MENU).val();
	this.handleEntryMenu(entry);
    });


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
}

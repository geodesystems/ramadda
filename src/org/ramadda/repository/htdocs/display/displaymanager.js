/**
   Copyright 2008-2024 Geode Systems LLC
*/


//Properties
const PROP_LAYOUT_TYPE = "layoutType";
const PROP_LAYOUT_COLUMNS = "layoutColumns";
const PROP_SHOW_MAP = "showMap";
const PROP_SHOW_MENU = "showMenu";
const PROP_FROMDATE = "fromDate";
const PROP_TODATE = "toDate";
const DISPLAY_MULTI = "multi";



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




addGlobalDisplayType({
    type: DISPLAY_MULTI,
    label: "Multi Chart",
    requiresData: true,
    forUser: false,
    category: "Misc"
});





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
            return  this.getLayoutManager().collectEntries();
        },
        getData: function() {
            return this.dataList;
        },
        handleEventFieldValueSelect: function(source, args) {
            this.notifyEvent(DisplayEvent.fieldValueSelected, source, args);
        },
        handleEventFieldsSelected: function(source, fields) {
            this.notifyEvent(DisplayEvent.fieldsSelected, source, fields);
        },
        handleEventPropertyChanged: function(source, prop) {
            this.notifyEvent(DisplayEvent.propertyChanged, source, prop);
        },
        handleEventEntriesChanged: function(source, entries) {
            this.notifyEvent(DisplayEvent.entriesChanged, source, entries);
        },
        handleEventMapBoundsChanged: function(source, bounds, forceSet) {
            var args = {
                "bounds": bounds,
                "force": forceSet
            };
            this.notifyEvent(DisplayEvent.mapBoundsChanged, source, args);
        },
        addMapLayer: function(source, entry) {
            this.notifyEvent("addMapLayer", source, {
                entry: entry
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
            var values = source?source.getRecordHtml(record, fields):"";
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
            this.notifyEvent(DisplayEvent.recordSelection, source, params);
            var entries = source.getEntries();
            if (entries != null && entries.length > 0) {
                this.handleEventEntrySelection(source, {
                    entry: entries[0],
                    selected: true
                });
            }
        },
        handleEventEntrySelection: function(source, props) {
            this.notifyEvent(DisplayEvent.entrySelection, source, props);
        },
        handleEventEntryMouseover: function(source, props) {
            this.notifyEvent(DisplayEvent.entryMouseover, source, props);
        },
        handleEventEntryMouseout: function(source, props) {
            this.notifyEvent(DisplayEvent.entryMouseout, source, props);
        },
        handleEventPointDataLoaded: function(source, pointData) {
            this.notifyEvent(DisplayEvent.pointDataLoaded, source, pointData);
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
            //How else do I refer to this object in the html that I add 
            var get = "getDisplayManager('" + this.getId() + "')";
            var layout = "getDisplayManager('" + this.getId() + "').getLayoutManager()";
            var html = "";

            var newMenus = {};
            var cats = [];
            var displayTypes = [];
            if (window.globalDisplayTypes != null) {
                displayTypes = window.globalDisplayTypes;
            }
	    DISPLAY_CATEGORIES.forEach(category=>{
                newMenus[category] = [];
                cats.push(category);
	    });
            for (var i = 0; i < displayTypes.length; i++) {
                //The ids (.e.g., 'linechart' have to match up with some class function with the name 
                var type = displayTypes[i];
                if (Utils.isDefined(type.forUser) && !type.forUser) {
                    continue;
                }
		var category = type.category;
                if (!category) {
                    category = CATEGORY_MISC;
                }
                if (newMenus[category] == null) {
                    newMenus[category] = [];
                    cats.push(category);
                }
		let menuAttrs = ["onclick", get + ".userCreateDisplay('" + type.type + "');"];
		if(type.desc) {
		    menuAttrs.push(TITLE);
		    menuAttrs.push(type.desc);
		}
                newMenus[category].push(HU.tag(TAG_LI, [], HU.tag(TAG_A, menuAttrs, type.label)));
            }
            let newMenu = "";
            for (var i = 0; i < cats.length; i++) {
                var cat = cats[i];
		var menu = Utils.join(newMenus[cat],"");
                var subMenu = HU.tag("ul", [], menu);
                var catLabel = HU.tag(TAG_A, [], cat);
                newMenu += HU.tag(TAG_LI, [], catLabel + subMenu);
            }


            var publishMenu =
                HU.tag(TAG_LI, [], HU.onClick(layout + ".publish('media_photoalbum');", "New Photo Album")) + "\n" +
                HU.tag(TAG_LI, [], HU.onClick(layout + ".publish('wikipage');", "New Wiki Page")) + "\n" +
                HU.tag(TAG_LI, [], HU.onClick(layout + ".publish('blogentry');", "New Blog Post")) + "\n";


            var fileMenu =
                HU.tag(TAG_LI, [], "<a>Publish</a>" + HU.tag("ul", [], publishMenu)) + "\n" +
                HU.tag(TAG_LI, [], HU.onClick(layout + ".showWikiText();", "Show Text")) + "\n" +
                HU.tag(TAG_LI, [], HU.onClick(layout + ".copyWikiText();", "Copy Text")) + "\n" +		
                HU.tag(TAG_LI, [], HU.onClick(layout + ".copyDisplayedEntries();", "Save entries")) + "\n";


            var titles = HU.tag(TAG_DIV, ["class", "ramadda-menu-block"], "Titles: " + HU.onClick(layout + ".titlesOn();", "On") + "/" + HU.onClick(layout + ".titlesOff();", "Off"));
            var dates = HU.tag(TAG_DIV, ["class", "ramadda-menu-block"],
				      "Set date range: " +
				      HU.onClick(layout + ".askMinDate();", "Min") + "/" +
				      HU.onClick(layout + ".askMaxDate();", "Max"));
            var editMenu =
                HU.tag(TAG_LI, [], HU.tag(TAG_DIV, ["class", "ramadda-menu-block"],
							"Set axis range :" +
							HU.onClick(layout + ".askMinZAxis();", "Min") + "/" +
							HU.onClick(layout + ".askMaxZAxis();", "Max"))) +
                HU.tag(TAG_LI, [], dates) +
                HU.tag(TAG_LI, [], titles) + "\n" +
                HU.tag(TAG_LI, [], HU.tag(TAG_DIV, ["class", "ramadda-menu-block"], "Details: " + HU.onClick(layout + ".detailsOn();", "On", []) + "/" +
							HU.onClick(layout + ".detailsOff();", "Off", []))) +
                HU.tag(TAG_LI, [], HU.onClick(layout + ".deleteAllDisplays();", "Delete all displays")) + "\n" +
                "";


            var table = HU.tag(TAG_DIV, ["class", "ramadda-menu-block"], "Table: " +
				      HU.onClick(layout + ".setLayout('table',1);", "1 column") + " / " +
				      HU.onClick(layout + ".setLayout('table',2);", "2 column") + " / " +
				      HU.onClick(layout + ".setLayout('table',3);", "3 column") + " / " +
				      HU.onClick(layout + ".setLayout('table',4);", "4 column"));
            var layoutMenu =
                HU.tag(TAG_LI, [], table) +
                HU.tag(TAG_LI, [], HU.onClick(layout + ".setLayout('rows');", "Rows")) + "\n" +
                HU.tag(TAG_LI, [], HU.onClick(layout + ".setLayout('columns');", "Columns")) + "\n" +
                HU.tag(TAG_LI, [], HU.onClick(layout + ".setLayout('tabs');", "Tabs"));


            var menuBar = HU.tag(TAG_LI, [], "<a>File</a>" + HU.tag("ul", [], fileMenu));
            menuBar += HU.tag(TAG_LI, [], "<a>Edit</a>" + HU.tag("ul", [], editMenu)) +
                HU.tag(TAG_LI, [], "<a>New</a>" + HU.tag("ul", [], newMenu)) +
                HU.tag(TAG_LI, [], "<a>Layout</a>" + HU.tag("ul", [], layoutMenu));


            var menu = HU.div([STYLE,"background:#fff;z-index:1000;", ATTR_CLASS, "xramadda-popup", ATTR_ID, this.getDomId(ID_MENU_OUTER)],
			      HU.tag("ul", [ATTR_ID, this.getDomId(ID_MENU_INNER), ATTR_CLASS, "sf-menu"], menuBar));

            html += menu;
            //                html += HU.tag(TAG_A, [ATTR_CLASS, "display-menu-button", ATTR_ID, this.getDomId(ID_MENU_BUTTON)],"&nbsp;");
            //                html+="<br>";
            return html;
        },
        hasGeoMacro: function(jsonUrl) {
	    if(!jsonUrl) return false;
            return jsonUrl.match(/(\${latitude})/g) != null;
        },
        getJsonUrl: function(jsonUrl, display, props) {
	    display.getRequestMacros().forEach(m=>{
		jsonUrl = m.apply(jsonUrl);
	    });
	    if(display.getAnimationEnabled()) {
		//Not now. Once was needed for gridded data
		//jsonUrl +='&doAnimation=true'
	    }
	    if(display.getProperty('dbSelect')) {
		jsonUrl +="&" + "dbSelect" +"=" +display.getProperty("select");
	    }
	    if(display.getProperty("requestArgs")) {
		let args = display.getProperty("requestArgs").split(",");
		for(let i=0;i<args.length;i+=2) {
		    jsonUrl +="&" + args[i] +"=" + args[i+1];
		}
	    }

	    if(display.pageSkip) {
		jsonUrl+="&skip=" + display.pageSkip;
	    }


            var fromDate = display.getProperty(PROP_FROMDATE);
            if (fromDate != null) {
                jsonUrl += "&fromdate=" + fromDate;
            }
            var toDate = display.getProperty(PROP_TODATE);
            if (toDate != null) {
                jsonUrl += "&todate=" + toDate;
            }


//	    jsonUrl=jsonUrl+"&FOO+BAR";
            let pattern = new RegExp(/startdate=([^&$]+)(&|$)/);
	    if(match = jsonUrl.match(pattern)) {
		let sep  = match[2];
//		jsonUrl = jsonUrl.replace(pattern,"startdate=-1 month" + sep);
//		console.log("URL:" + jsonUrl);
	    }

	    /*
	    if(display.getBounds) {
		let b = display.getBounds();
		//NWSE
		jsonUrl = HU.url(jsonUrl,["bounds",b.top+","+b.right+","+b.bottom+","+b.left]);
		console.log(b);
		console.log(jsonUrl);
	    }
	    //	    https://localhost:8430/repository/entry/show?entryid=89516542-f88f-43cf-98ce-f8ea2d3111b0&map_bounds=63.6307%2C-183.21358%2C29.38993%2C15.41923&zoomLevel=3&mapCenter=49.38817%2C-83.89717
*/

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
		props.theData = props.data;
		props.data = null;
	    }

            if (props.theData != null && !props.theData.hasData()) {
                let haveItAlready = false;
                for (var i = 0; i < this.dataList.length; i++) {
                    let existingData = this.dataList[i];
                    if (existingData.equals(props.theData)) {
                        props.theData = existingData;
                        haveItAlready = true;
                        break;
                    }
                }
                if (!haveItAlready) {
                    this.dataList.push(props.theData);
                }
                //                console.log("data:" + haveItAlready);
            }

	    if(type==null || type.trim().length==0) return null;
            //            console.log("props:" + JSON.stringify(props));
            //Upper case the type name, e.g., linechart->Linechart
            var proc = type.substring(0, 1).toUpperCase() + type.substring(1);


            //Look for global functions  Ramadda<Type>Display, <Type>Display, <Type> 
            //e.g. - RamaddaLinechartDisplay, LinechartDisplay, Linechart 
            var classname = null;
            var names = ["Ramadda" + proc + "Display",
			 proc + "Display",
			 "Display"+ proc,
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
            let displayId = props.displayId;
	    if(!displayId)  {
		displayId = this.getUniqueId("display");
	    }
            if (props.theData == null && this.dataList.length > 0) {
                props.theData = this.dataList[0];
            }
            props.createdInteractively = true;
            if (!props.entryId) {
                props.entryId = this.group.entryId;
            }
            let display = eval(" new " + funcName + "(this,'" + displayId + "', props);");
            if (display == null) {
                console.log("Error: could not create display using:" + funcName);
                alert("Error: could not create display using:" + funcName);
                return;
            }
	    if(props.dummy) return display;
            this.addDisplay(display);
	    display.doFinalInitialization();
            return display;
        },
        pageHasLoaded: function(display) {
            this.getLayoutManager().pageHasLoaded();
        },
        addDisplay: function(display) {
            display.setDisplayManager(this);
            this.getLayoutManager().addDisplay(display);
	    //Call loadInitialData a bit later so the display (e.g., the map) can be initialized
	    setTimeout(()=>{
		display.loadInitialData();
	    },1);
        },
	getDisplays: function() {
	    return this.getLayoutManager().getDisplays();
	},
        notifyEvent: function(event, source, data) {
            this.getLayoutManager().notifyEvent(event, source, data);
        },
        removeDisplay: function(display) {
            this.getLayoutManager().removeDisplay(display);
            this.notifyEvent(DisplayEvent.removeDisplay, this, display);
        },
	setEntry: function(entry) {
	    this.getLayoutManager().getDisplays().forEach(display=>{
		display.setEntry(entry);
	    });
	},
    });

    addDisplayManager(this);

    let displaysHtml = HU.div([ATTR_ID, this.getDomId(ID_DISPLAYS), ATTR_CLASS, "display-container",STYLE,HU.css("display","block")]);
    let html = HU.openTag(TAG_DIV,["style","position:relative;"]);
    html += HU.div(["id", this.getDomId(ID_MENU_CONTAINER)]);
    html +=  this.getEntriesMenu(argProperties);

    if(this.getShowMenu()) {
        html += HU.tag(TAG_A, [ATTR_CLASS, "display-menu-button", ATTR_ID, this.getDomId(ID_MENU_BUTTON)], SPACE);
    }
    let targetDiv = this.getProperty("target",this.getProperty("targetDiv"));
    let _this = this;
    if (targetDiv != null) {
	targetDiv = targetDiv.replace("${entryid}",this.getProperty("entryId"));
	if($("#" + targetDiv).length==0) {
	    console.log("Error: display group could not find targetDiv:" + targetDiv);
	    targetDiv=null;
	}
    }

    if (targetDiv != null) {
        $(document).ready(function() {
            $("#" + targetDiv).html(displaysHtml);
            _this.getLayoutManager().doLayout();
	});
    } else {
        html += displaysHtml;
    }
    html += HU.closeTag(TAG_DIV);
    let divid = this.getProperty("divId",this.getId());
    $("#" + divid).html(html)
    this.initializeEntriesMenu();

    this.jq(ID_MENU_BUTTON).html(HU.getIconImage("fas fa-cog",[TITLE,"Display menu"],['style','color:#aaa;'] )).button({
	classes: {
	    "ui-button": "display-manager-button",
	}	
    }).click(function(event) {
	if(this.dialog) {
	    this.dialog.remove();
	}
        let html = _this.makeMainMenu();
	this.dialog = HU.makeDialog({content:html,title:"Displays",my:"left top",at:"left bottom",anchor:_this.jq(ID_MENU_BUTTON)});
        _this.jq(ID_MENU_INNER).superfish({
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
                this.setDisplayMessage(this.getLoadingMessage());
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
            var html = HU.div([ATTR_ID, this.getDomId(ID_DISPLAYS), ATTR_CLASS, "display-container"]);
            this.setContents(html);
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


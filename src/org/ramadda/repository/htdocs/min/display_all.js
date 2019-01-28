/**
Copyright 2008-2014 Geode Systems LLC
*/






function AreaWidget(display) {
    var ID_CONTAINS = "contains";
    var ID_NORTH = "north";
    var ID_SOUTH = "south";
    var ID_EAST = "east";
    var ID_WEST = "west";

    var ID_AREA_LINK = "arealink";

    RamaddaUtil.inherit(this, {
            display:display,
            getHtml: function() {
                var callback = this.display.getGet();
                //hack, hack
                var cbx = HtmlUtil.checkbox(this.display.getDomId(ID_CONTAINS),["title","Search mode: checked - contains, unchecked - overlaps"], false);
                var link = HtmlUtil.onClick(callback+".areaWidget.areaLinkClick();", HtmlUtil.image(root +(this.linkArea?"/icons/link.png":"/icons/link_break.png"),[ATTR_TITLE,"Set bounds from map", ATTR_CLASS, "display-area-link", "border","0",ATTR_ID, this.display.getDomId(ID_AREA_LINK)]));

                var mylocation = HtmlUtil.onClick(callback+".areaWidget.useMyLocation();", HtmlUtil.image(root +"/icons/compass.png"),[ATTR_TITLE,"Set my location", ATTR_CLASS, "display-area-link", "border","0"]);


                var erase = HtmlUtil.onClick(callback+".areaWidget.areaClear();", HtmlUtil.image(root +"/icons/eraser.png",[ATTR_TITLE,"Clear form", ATTR_CLASS, "display-area-link", "border","0"]));

                var areaForm = HtmlUtil.openTag(TAG_TABLE,[ATTR_CLASS,"display-area", "border","0","cellpadding","0","cellspacing","0"]);
                areaForm += HtmlUtil.tr([],
                                        HtmlUtil.td(["align","center"],
                                                    HtmlUtil.leftCenterRight(mylocation, 
                                                                             HtmlUtil.input(ID_NORTH,"",["placeholder","N",ATTR_CLASS,"input display-area-input", "size", "5",ATTR_ID, 
                                                                                                         this.display.getDomId(ID_NORTH),  ATTR_TITLE,"North"]), link,"20%","60%","20%")));

                areaForm += HtmlUtil.tr([],HtmlUtil.td([],
                                                       HtmlUtil.input(ID_WEST,"",["placeholder","W",ATTR_CLASS,"input  display-area-input", "size", "5",ATTR_ID, 
                                                                                  this.display.getDomId(ID_WEST),  ATTR_TITLE,"West"]) +
                                                       HtmlUtil.input(ID_EAST,"",["placeholder","E",ATTR_CLASS,"input  display-area-input", "size", "5",ATTR_ID, 
                                                                                  this.display.getDomId(ID_EAST),  ATTR_TITLE,"East"])));
                areaForm += HtmlUtil.tr([],
                                        HtmlUtil.td(["align","center"],
                                                    HtmlUtil.leftCenterRight(erase, HtmlUtil.input(ID_SOUTH,"",["placeholder","S",ATTR_CLASS,"input  display-area-input", "size", "5",ATTR_ID, 
                                                                                                             this.display.getDomId(ID_SOUTH),  ATTR_TITLE,"South"]),cbx)));
                                        
                areaForm += HtmlUtil.closeTag(TAG_TABLE);
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
                    navigator.geolocation.getCurrentPosition(function(position){_this.setUseMyLocation(position);});
                } else {
                }
            },
            setUseMyLocation: function(position) {
                var lat = position.coords.latitude;
                var lon = position.coords.longitude;
                var offset = 5.0;
                if(this.display.myLocationOffset)
                    offset = parseFloat(this.display.myLocationOffset);
                    
                $("#" + this.display.getDomId(ID_NORTH)).val(lat+offset);
                $("#" + this.display.getDomId(ID_WEST)).val(lon-offset);
                $("#" + this.display.getDomId(ID_SOUTH)).val(lat-offset);
                $("#" + this.display.getDomId(ID_EAST)).val(lon + offset);
                if(this.display.submitSearchForm)
                    this.display.submitSearchForm();
            },
            areaLinkClick: function() {
                this.linkArea = !this.linkArea;
                var image = root +( this.linkArea? "/icons/link.png":"/icons/link_break.png");
                $("#" + this.display.getDomId(ID_AREA_LINK)).attr("src", image);
                if(this.linkArea && this.lastBounds) {
                    var b  = this.lastBounds;
                    $("#" + this.display.getDomId(ID_NORTH)).val(formatLocationValue(b.top));
                    $("#" + this.display.getDomId(ID_WEST)).val(formatLocationValue(b.left));
                    $("#" + this.display.getDomId(ID_SOUTH)).val(formatLocationValue(b.bottom));
                    $("#" + this.display.getDomId(ID_EAST)).val(formatLocationValue(b.right));
                }
            },
           linkArea: false,
           lastBounds: null,
           handleEventMapBoundsChanged: function (source,  args) {
                bounds = args.bounds;
                this.lastBounds = bounds;
                if(!args.force && !this.linkArea) return;
                $("#" + this.display.getDomId(ID_NORTH)).val(formatLocationValue(bounds.top));
                $("#" + this.display.getDomId(ID_WEST)).val(formatLocationValue(bounds.left));
                $("#" + this.display.getDomId(ID_SOUTH)).val(formatLocationValue(bounds.bottom));
                $("#" + this.display.getDomId(ID_EAST)).val(formatLocationValue(bounds.right));
            },
            setSearchSettings: function(settings) {
                var cbx =$("#" + this.display.getDomId(ID_CONTAINS));
                if(cbx.is(':checked')) {
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
            display:display,
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
                var html = HtmlUtil.input(ID_DATE_START,"",["placeholder","Start date", ATTR_ID,
                                                            display.getDomId(ID_DATE_START),"size","10"]) + " - " +
                    HtmlUtil.input(ID_DATE_END,"",["placeholder","End date", ATTR_ID,
                                                            display.getDomId(ID_DATE_END),"size","10"]);
                return html;
            }
        });
}


/**
Copyright 2008-2015 Geode Systems LLC
*/


//Ids of DOM components
var ID_FIELDS = "fields";
var ID_HEADER = "header";
var ID_TITLE = ATTR_TITLE;
var ID_TITLE_EDIT = "title_edit";
var ID_DETAILS = "details";
var ID_DISPLAY_CONTENTS = "contents";
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
var ID_MENU_OUTER =  "menu_outer";
var ID_MENU_INNER =  "menu_inner";



var ID_REPOSITORY = "repository";

var  displayDebug = false;


var PROP_DISPLAY_FILTER = "displayFilter";
var PROP_EXCLUDE_ZERO = "excludeZero";


var PROP_DIVID = "divid";
var PROP_FIELDS = "fields";
var PROP_LAYOUT_HERE = "layoutHere";
var PROP_HEIGHT  = "height";
var PROP_WIDTH  = "width";





function initRamaddaDisplays() {
    ramaddaCheckForResize();
    if(window.globalDisplaysList == null) {
        return;
    }
    for(var i=0;i<window.globalDisplaysList.length;i++) {
        window.globalDisplaysList[i].pageHasLoaded();
    }
}

function addRamaddaDisplay(display) {
    if(window.globalDisplays == null) {
        window.globalDisplays = {};
        window.globalDisplaysList = [];
    }
    window.globalDisplaysList.push(display);
    window.globalDisplays[display.getId()] = display;
    if(display.displayId) {
        window.globalDisplays[display.displayId] = display;
    }
}


function ramaddaCheckForResize() {
    var redisplayPending = false;
    var redisplayPendingCnt = 0;
    //A hack to redraw the chart after the window is resized
    $(window).resize(function() {
            if(window.globalDisplaysList == null) {
                return;
            }
            //This handles multiple resize events but keeps only having one timeout pending at a time
            if(redisplayPending) {
                redisplayPendingCnt++;
                return;
            }
            var timeoutFunc = function(myCnt){
                if(myCnt == redisplayPendingCnt) {
                    redisplayPending = false;
                    redisplayPendingCnt=0;
                    for(var i=0;i<window.globalDisplaysList.length;i++) {
                        var display  =  window.globalDisplaysList[i];
                        if(display.displayData)
                            display.displayData();
                    }
                } else {
                    //Had a resize event during the previous timeout
                    setTimeout(timeoutFunc.bind(null, redisplayPendingCnt),1000);
                }
            }
            redisplayPending = true;
            setTimeout(timeoutFunc.bind(null,redisplayPendingCnt),1000);
        });
}



function ramaddaDisplayCheckLayout() {
    for(var i=0;i<window.globalDisplaysList.length;i++) {
        if(window.globalDisplaysList[i].checkLayout) {
            window.globalDisplaysList[i].checkLayout();
        }
    }


}

function getRamaddaDisplay(id) {
    if(window.globalDisplays == null) {
        return null;
    }
    return window.globalDisplays[id];
}


function removeRamaddaDisplay(id) {
    var display =getRamaddaDisplay(id);
    if(display) {
        display.removeDisplay();
    }
}


function DisplayThing(argId, argProperties) {
    if(argProperties == null) {
       argProperties = {};
    }
    
    //check for booleans as strings
    for(var i in argProperties) {
        if(typeof  argProperties[i]  == "string") {
            if(argProperties[i] == "true") argProperties[i] =true;
            else if(argProperties[i] == "false") argProperties[i] =false;
        }
    }


    //Now look for the structured foo.bar=value
    for(var key  in argProperties) {
        var toks = key.split(".");
        if(toks.length<=1) {
            continue;
        }
        var map = argProperties;
        var topMap  = map;
        //graph.axis.foo=bar
        var v = argProperties[key];
        if(v == "true") v = true;
        else if(v == "false") v = false;
        for(var i=0;i<toks.length;i++) {
            var tok = toks[i];
            if(i == toks.length-1) {
                map[tok] = v;
                break;
            }
            var nextMap = map[tok];
            if(nextMap == null) {
                map[tok] = {};
                map = map[tok];
            }  else {
                map = nextMap;
            }
        }
    }
    this.displayId = null;
    $.extend(this, argProperties);

    RamaddaUtil.defineMembers(this, {
            objectId: argId,
            properties:argProperties,
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
            formatDate: function(date,args) {
                //Check for date object from charts
                if(!date.getTime && date.v) date= date.v;
                if(!date.toLocaleDateString) {
                    return ""+ date;
                }
                if(!args) args = {};
                var suffix;
                if(!Utils.isDefined(args.suffix)) 
                    suffix = args.suffix;
                else
                    suffix = this.getProperty("dateSuffix");
                var timeZone = this.getTimeZone();
                if(!suffix && timeZone) suffix = timeZone;
                return Utils.formatDate(date,args.options,{timeZone:timeZone,suffix:suffix});
            },
                getUniqueId: function(base) {
                return HtmlUtil.getUniqueId(base);
        },
        handleError: function(code, message) {
                GuiUtils.handleError("An error has occurred:" + message, true, true);
        },
        toString: function() {
                return "DisplayThing:" + this.getId();
         },
       getDomId:function(suffix) {
                return this.getId() +"_" + suffix;
       },
       jq: function(componentId) {
             return $("#"+ this.getDomId(componentId));
       },
       writeHtml: function(idSuffix, html) {
                $("#" + this.getDomId(idSuffix)).html(html);
       },
                getRecordHtml: function(record,fields) {
                var showGeo = false;
                if(Utils.isDefined(this.showGeo))  {
                    showGeo = (""+this.showGeo) == "true";
                }
                var values = "<table class=formtable>";
                if(false && record.hasLocation()) {
                    var latitude = record.getLatitude();
                    var longitude = record.getLongitude();
                    values+= "<tr><td align=right><b>Latitude:</b></td><td>" +  number_format(latitude, 4, '.', '') + "</td></tr>";
                    values+= "<tr><td align=right><b>Longitude:</b></td><td>" + number_format(longitude, 4, '.', '') + "</td></tr>";
                }
                for(var doDerived=0;doDerived<2;doDerived++) {
                    for(var i=0;i<record.getData().length;i++) {
                        var field = fields[i];
                        if(doDerived == 0 && !field.derived) continue;
                        else if(doDerived == 1 && field.derived) continue;
                        var label = field.getLabel();
                        label = this.formatRecordLabel(label);

                        if(!showGeo) {
                            if(field.isFieldGeo()){
                                continue;
                            }
                        }
                        var value = record.getValue(i);
                        if(typeof value  == "number") {
                            var sv = value+"";
                            //total hack to decimals format numbers
                            if(sv.indexOf('.')>=0) {
                                var decimals = 1;
                                //?
                                if(Math.abs(value) <1.5) decimals = 3;
                                value  = number_format(value, decimals,'.','');
                            }
                        }
                        values+= "<tr><td align=right><b>" + label +":</b></td><td>" + value + "</td></tr>";
                    }
                }
                if(record.hasElevation()) {
                    values+= "<tr><td  align=right><b>Elevation:</b></td><td>" + number_format(record.getElevation(), 4, '.', '') + "</td></tr>";
                }
                values += "</table>";
                return values;
            },
            formatRecordLabel: function(label) {
                label = label.replace(/!!/g," -- ");
                return label;
            },

       getFormValue: function(what, dflt) {
           var fromForm = $("#" + this.getDomId(what)).val();
           if(fromForm!=null) {
               if(fromForm.length>0) {
                   this.setProperty(what,fromForm);
               }
               if(fromForm == "none") {
                   this.setProperty(what,null);
               }
               return fromForm;
           }
             return this.getProperty(what,dflt);
        },

       getName: function() {
         return this.getFormValue("name",this.getId());
       },
       getEventSource: function() {
            return this.getFormValue("eventSource","");
       },
       setDisplayParent:  function (parent) {
             this.displayParent = parent;
            },
       getDisplayParent:  function () {
                if(this.displayParent == null) {
                    this.displayParent = this.getLayoutManager();
                }
            return this.displayParent;
       },
       removeProperty: function(key) {
                this.properties[key] = null;
       },
       setProperty: function(key, value) {
           this.properties[key] = value;
        },

       getSelfProperty: function(key, dflt) {
          if(this[key] !=null) {
              return this[key];
          }
          return this.getProperty(key, dflt);
       },
       initTooltip: function() {
                //don't do this for now                $( document ).tooltip();
            },
       formatNumber: function(number) {
          if(!this.getProperty("format", true)) return number;
          return Utils.formatNumber(number);
            },
       getProperty: function(key, dflt) {
           if(this[key]) return this[key];
            var value = this.properties[key];
            if(value != null) {
                return value;
            }
            if(this.displayParent!=null) {
                return this.displayParent.getProperty(key, dflt);
             }
            if(this.getDisplayManager) {
                return this.getDisplayManager().getProperty(key, dflt);

            }
            return dflt;
         }

        });
}





function RamaddaDisplay(argDisplayManager, argId, argType, argProperties) {
    RamaddaUtil.initMembers(this, {
            orientation: "horizontal",
        });

    var SUPER;
    RamaddaUtil.inherit(this,SUPER = new DisplayThing(argId, argProperties));
    this.getSuper = function() {return SUPER;}

    if(this.derived) {
        //        console.log("derived:" + this.derived);
        if(this.derived.indexOf("[") == 0) {
            this.derived=JSON.parse(this.derived);
        } else  {
            this.derived=[JSON.parse(this.derived)];
        }
        //Init the derived
        for(var i=0;i<this.derived.length;i++) {
            var d = this.derived[i];
            if(!Utils.isDefined(d.isRow) && !Utils.isDefined(d.isColumn)) {
                d.isRow = true;
                d.isColumn = false;
            }
            if(!d.isRow && !d.isColumn) {
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
            displayManager:argDisplayManager,
            filters: [],
            dataCollection: new DataCollection(),
            selectedCbx: [],
            entries: [],
            wikiAttrs: ["title","showTitle","showDetails","minDate","maxDate"],

            getDisplayManager: function() {
               return this.displayManager;
            },
            getLayoutManager: function() {
                return this.getDisplayManager().getLayoutManager();
            },
            displayError: function(msg) {
                this.displayHtml(HtmlUtil.getErrorDialog(msg));
            },
           clearHtml: function() {
                this.displayHtml("");
            },
            displayHtml: function(html) {
                this.jq(ID_DISPLAY_CONTENTS).html(html);
            },
            notifyEvent:function(func, source, data) {
                if(this[func] == null) { return;}
                this[func].apply(this, [source,data]);
            },
            getColorTableName: function() {
                var ct =   this.getProperty("colorBar", this.getProperty("colorTable"));
                if(ct == "none") return null;
                return ct;
            },
            getColorTable: function() {
                var colorTable = this.getColorTableName();
                if(colorTable) {
                    return  Utils.ColorTables[colorTable];
                }
                return null;
            },
            displayColorTable: function(domId, min,max) {
                min = parseFloat(min);
                max = parseFloat(max);
                var ct = this.getColorTable();
                if(!ct) return;
                var html = HtmlUtil.openTag("div",["class","display-colortable"]) + "<table cellpadding=0 cellspacing=0 width=100% border=0><tr><td width=1%>" + this.formatNumber(min)+"&nbsp;</td>";
                var step = (max-min)/ct.length;
                for(var i=0;i<ct.length;i++) {
                    html+=HtmlUtil.td(["class","display-colortable-slice","style","background:" + ct[i]+";","width","1"],HtmlUtil.div(["style","background:" + ct[i]+";" + "width:100%;height:10px;min-width:1px;","title",this.formatNumber(min+step*i)],""));
                }
                html+="<td width=1%>&nbsp;" + this.formatNumber(max)+"</td>";
                html+="</tr></table>";
                html+= HtmlUtil.closeTag("div");
                this.jq(domId).html(html);
            },
            toString: function() {
                 return "RamaddaDisplay:" + this.type +" - " + this.getId();
            },
            getType: function() {
                return this.type;
            },
            getClass: function(suffix) {
                if(suffix == null) {
                    return this.getBaseClass();
                }
                return this.getBaseClass()+"-" + suffix;
            },
            getBaseClass: function() {
                return "display-" + this.getType();
            },
            setDisplayManager: function(cm) {
                this.displayManager = cm;
                this.setDisplayParent(cm.getLayoutManager());
            },
            setContents: function(contents) {
                contents = HtmlUtil.div([ATTR_CLASS,"display-contents-inner display-" + this.getType() +"-inner"], contents);
                this.writeHtml(ID_DISPLAY_CONTENTS, contents);
            },
            addEntry: function(entry) {
                this.entries.push(entry);
            },
            clearCachedData: function() {
            },
            setEntry: function(entry) {
                this.entries = [];
                this.addEntry(entry);
                this.entry = entry;
                this.entryId = entry.getId();
                this.clearCachedData();
                if(this.properties.data) {
                    this.dataCollection = new DataCollection();
                    this.properties.data= this.data= new PointData(entry.getName(), null, null, this.getRamadda().getRoot()+"/entry/show?entryid=" + entry.getId() +"&output=points.product&product=points.json&max=5000",{entryId:this.entryId});
                    this.data.loadData(this);
                }
                this.updateUI();
                var title = "";
                if(this.getShowTitle()) {
                    title= entry.getName();
                    title = HtmlUtil.href(this.getRamadda().getEntryUrl(this.entryId),title);
                    this.jq(ID_TITLE).html(title);
                }
            },
            handleEventRecordSelection: function(source, args) {
                if(!source.getEntries) {
                    return;
                }
                var entries = source.getEntries();
                for(var i =0;i<entries.length;i++) {
                    var entry = entries[i];
                    var containsEntry = this.getEntries().indexOf(entry) >=0;
                    if(containsEntry) {
                        this.highlightEntry(entry);
                        break;
                    }
                }
            },
            areaClear:  function() {
                this.getDisplayManager().notifyEvent("handleEventAreaClear", this);
            },
            handleEventEntryMouseover: function(source, args) {
            },
            handleEventEntryMouseout: function(source, args) {
            },
            handleEventEntrySelection: function(source, args) {
                var containsEntry = this.getEntries().indexOf(args.entry) >=0;
                if(!containsEntry) {
                    return;
                }
                if(args.selected) {
                    $("#" + this.getDomId(ID_TITLE)).addClass("display-title-select");
                } else {
                    $("#" + this.getDomId(ID_TITLE)).removeClass("display-title-select");
                }
            },
            highlightEntry: function(entry) {
                $("#" + this.getDomId(ID_TITLE)).addClass("display-title-select");
            },
            getEntries: function() {
                return this.entries;
            },
            getDisplayEntry: function() {
                var entries =this.getEntries();
                if(entries!=null && entries.length>0) {
                    return entries[0];
                }
                if(this.entryId) {
                    var callback = {call:function(entry) {console.log("got entry " + entry);}};
                    return this.getRamadda().getEntry(this.entryId, callback);
                }
                return null;
            },
            hasEntries: function() {
                return this.entries !=null && this.entries.length>0;
            },
           getWaitImage: function() {
                return HtmlUtil.image(ramaddaBaseUrl + "/icons/progress.gif");
            },
            getLoadingMessage: function(msg) {
                if(!msg) msg = "Loading data...";
                return HtmlUtil.div(["text-align","center"], this.getMessage("&nbsp;" + msg));
            },
            getMessage: function(msg) {
                return HtmlUtil.div([ATTR_CLASS,"display-message"], msg);
            },
            getFieldValue: function(id, dflt) {
                var jq = $("#" + id);
                if(jq.size()>0) {
                    return jq.val();
                } 
                return dflt;
            },
            getFooter: function() {
                return  HtmlUtil.div([ATTR_ID,this.getDomId(ID_FOOTER),ATTR_CLASS,"display-footer"], 
                                           HtmlUtil.leftRight(HtmlUtil.div([ATTR_ID,this.getDomId(ID_FOOTER_LEFT),ATTR_CLASS,"display-footer-left"],""),
                                                              HtmlUtil.div([ATTR_ID,this.getDomId(ID_FOOTER_RIGHT),ATTR_CLASS,"display-footer-right"],"")));
            },
           checkFixedLayout: function() {
                if(this.getIsLayoutFixed()) {
                    var divid = this.getProperty(PROP_DIVID);
                    if(divid!=null) {
                        var html = this.getHtml();
                        $("#" + divid).html(html);
                    }
                }
            },
            shouldSkipField: function(field) {
                if(this.skipFields && !this.skipFieldsList) {
                    this.skipFieldsList = this.skipFields.split(",");
                }

                if(this.skipFieldsList) {
                    return this.skipFieldsList.indexOf(field.getId()) >=0;
                }
                return false;
            },
            fieldSelected:function(event) {
                this.userHasSelectedAField = true;
                this.selectedFields = null;
                this.overrideFields = null;
                this.removeProperty(PROP_FIELDS);
                this.fieldSelectionChanged();
                if(event.shiftKey) {
                    var fields = this.getSelectedFields();
                    this.getDisplayManager().handleEventFieldsSelected(this, fields);
                }
            },
            addFieldsCheckboxes: function(argFields) {
                if(!this.hasData()) {
                    return;
                }
                var fixedFields = this.getProperty(PROP_FIELDS);
                if(fixedFields!=null) {
                    if(fixedFields.length==0) {
                        fixedFields = null;
                    } 
                }
                
                var html =  "";
                var checkboxClass = this.getId() +"_checkbox";
                var groupByClass = this.getId() +"_groupby";
                var dataList =  this.dataCollection.getList();

                if(argFields!=null) {
                    this.overrideFields = [];
                }
                var seenLabels = {};


                var tuples = this.getStandardData(null,{includeIndex: false});
                var allFields = this.dataCollection.getList()[0].getRecordFields();
                var badFields = {};
                var flags=null;
                for(var rowIdx=1;rowIdx<tuples.length;rowIdx++) {
                    var tuple = this.getDataValues(tuples[rowIdx]);
                    if(flags == null) {
                        flags = [];
                        for(var tupleIdx=0;tupleIdx<tuple.length;tupleIdx++) {
                            flags.push(false);
                        }
                    }
                    
                    for(var tupleIdx=0;tupleIdx<tuple.length;tupleIdx++) {
                        if(!flags[tupleIdx]) {
                            if(tuple[tupleIdx] != null) {
                                flags[tupleIdx] = true;
                                //                                console.log("Flag[" + tupleIdx+"] value:" + tuple[tupleIdx]);
                            }
                        }
                    }

                }

                for(var tupleIdx=0;tupleIdx<tuple.length;tupleIdx++) {
                    //                    console.log("#" + tupleIdx + " " + (tupleIdx<allFields.length?allFields[tupleIdx].getId():"") +" ok:" + flags[tupleIdx] );
                }

                for(var collectionIdx=0;collectionIdx<dataList.length;collectionIdx++) {             
                    var pointData = dataList[collectionIdx];
                    var fields =this.getFieldsToSelect(pointData);
                    if(this.canDoGroupBy()) {
                        var allFields = pointData.getRecordFields();
                        var cnt = 0;
                        for(i=0;i<allFields.length;i++) { 
                            var field = allFields[i];
                            if(field.getType() != "string") continue;
                            if(cnt ==0) {
                                html += HtmlUtil.div([ATTR_CLASS,"display-dialog-subheader"],  "Group By");
                                html += HtmlUtil.openTag(TAG_DIV, [ATTR_CLASS, "display-fields"]);
                                var on = this.groupBy == null || this.groupBy == "";
                                html += HtmlUtil.tag(TAG_DIV, [ATTR_TITLE, "none"],
                                                     HtmlUtil.radio("none", this.getDomId("groupby"), groupByClass, "none", !on) +" None");
                            }
                            cnt++;
                            var on = this.groupBy == field.getId();
                            var idBase = "groupby_" + collectionIdx +"_" +i;
                            field.radioId  = this.getDomId(idBase);
                            html += HtmlUtil.tag(TAG_DIV, [ATTR_TITLE, field.getId()],
                                                 HtmlUtil.radio(field.radioId, this.getDomId("groupby"), groupByClass, field.getId(), on) +" " +field.getLabel() +" (" + field.getId() +")"
                                             );
                        }
                        if(cnt >0) {
                            html+= HtmlUtil.closeTag(TAG_DIV);
                        }
                    }

                    if(/*this.canDoMultiFields() && */fields.length>0) {
                        var selected = this.getSelectedFields([]);
                        var selectedIds = [];
                        for(i=0;i<selected.length;i++) { 
                            selectedIds.push(selected[i].getId());
                        }
                        var disabledFields = "";
                        html += HtmlUtil.div([ATTR_CLASS,"display-dialog-subheader"],  "Displayed Fields");
                        html += HtmlUtil.openTag(TAG_DIV, [ATTR_CLASS, "display-fields"]);
                        for(var tupleIdx=0;tupleIdx<fields.length;tupleIdx++) { 
                            var field = fields[tupleIdx];
                            var idBase = "cbx_" + collectionIdx +"_" +tupleIdx;
                            field.checkboxId  = this.getDomId(idBase);
                            var on = false;
                            var hasValues  = (flags?flags[field.getIndex()]:true);
                            //                            console.log(tupleIdx + " field: " + field.getId() + "has values:" + hasValues);
                            if(argFields!= null) {
                                //                                console.log("argFields:" + argFields);
                                for(var fIdx=0;fIdx<argFields.length;fIdx++) {
                                    if(argFields[fIdx].getId() == field.getId()) {
                                        on = true;
                                        //                                        console.log("argField:"+ argFields[fIdx].getId()+ " field.id:" + field.getId() +" on:" +on);
                                        this.overrideFields.push(field.getId());
                                        break;
                                    }
                                }
                            }   else if(selectedIds.length>0) {
                                on = selectedIds.indexOf(field.getId())>=0;
                                //                                console.log("selected ids   on:" + on +" " + field.getId());
                            } else if(fixedFields!=null) {
                                on = (fixedFields.indexOf(field.getId())>=0);
                                if(!on) {
                                    on = (fixedFields.indexOf("#" + (tupleIdx+1))>=0);
                                }
                                //                                console.log("fixed fields  on:" + on +" " + field.getId());
                            } else if(this.overrideFields!=null) {
                                on = this.overrideFields.indexOf(field.getId())>=0;
                                if(!on) {
                                    on = (this.overrideFields.indexOf("#" + (tupleIdx+1))>=0);
                                }
                                //                                console.log("override fields  on:" + on +" " + field.getId());
                            } else {
                                if(this.selectedCbx.indexOf(field.getId())>=0) {
                                    on = true;
                                }  else if(this.selectedCbx.length==0) {
                                    on = (i==0);
                                }
                                //                                console.log("cbx fields:" + on + " " + field.getId());
                            }
                            var label = field.getLabel();
                            if(seenLabels[label]) {
                                if(Utils.stringDefined(field.getUnit())) {
                                    label = label +" (" +field.getUnit() +")";
                                } else {
                                    label = label +" " +seenLabels[label];
                                }
                                seenLabels[label]++;
                            } else {
                                seenLabels[label] = 1;
                            }

                            if(!hasValues) {
                                disabledFields+= HtmlUtil.div([], label);
                            } else {
                                if(field.derived) {
                                    label += " (derived)";
                                }
                                var widget;
                                if(this.canDoMultiFields()) {
                                    widget = HtmlUtil.checkbox(field.checkboxId, ["class", checkboxClass], on);
                                } else {
                                    widget = HtmlUtil.radio(field.checkboxId, "field_radio", checkboxClass, "", on);
                                }

                                html += HtmlUtil.tag(TAG_DIV, [ATTR_TITLE, field.getId()],
                                                     widget +" " +label
                                                     );
                            }
                            //                        html+= "<br>";
                        }
                    }
                    if(disabledFields!="") {
                        html+=HtmlUtil.div(["style","border-top:1px #888  solid;"],"<b>No Data Available</b>" + disabledFields);
                    }

                    html+= HtmlUtil.closeTag(TAG_DIV);
                }


                this.writeHtml(ID_FIELDS,html);

                this.userHasSelectedAField = false;
                var theDisplay = this;
                //Listen for changes to the checkboxes
                $("." + checkboxClass).click(function(event) {
                        console.log("field selected");
                        theDisplay.fieldSelected(event);
                    });

                $("." + groupByClass).change(function(event) {
                        theDisplay.groupBy = $(this).val();
                        if(theDisplay.displayData) {
                            theDisplay.displayData();
                        }
                    });
            },
            fieldSelectionChanged: function() {
                var name  = "the display";
                this.setDisplayTitle();
                if(this.displayData) {
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
            getSelectedFields:function(dfltList) {
                this.lastSelectedFields =  this.getSelectedFieldsInner(dfltList);
                var fixedFields = this.getProperty(PROP_FIELDS);
                if(fixedFields) fixedFields.length = 0;
                this.setDisplayTitle();
                return this.lastSelectedFields;
            },
            getSelectedFieldsInner:function(dfltList) {
                if(this.selectedFields) {
                    return this.selectedFields;
                }
                var df = [];
                var dataList =  this.dataCollection.getList();
                //If we have fixed fields then clear them after the first time
                var fixedFields = this.getProperty(PROP_FIELDS);
                for(var collectionIdx=0;collectionIdx<dataList.length;collectionIdx++) {             
                    var pointData = dataList[collectionIdx];
                    var fields = this.getFieldsToSelect(pointData);
                    if(fixedFields !=null && fixedFields.length>0) {
                        for(var i=0;i<fixedFields.length;i++) {
                            var sfield = fixedFields[i];
                            for(var j=0;j<fields.length;j++) { 
                                var field = fields[j];
                                var id = field.getId();
                                if(id == sfield || ("#"+(j+1)) == sfield) {
                                    df.push(field);
                                    break;
                                }
                            }
                        }
                    }
                }

                if(fixedFields !=null && fixedFields.length>0) {
                    return df;
                }

                var fieldsToSelect = null;
                var firstField = null;
                this.selectedCbx = [];
                var cbxExists = false;

                for(var collectionIdx=0;collectionIdx<dataList.length;collectionIdx++) {             
                    var pointData = dataList[collectionIdx];
                    fieldsToSelect = this.getFieldsToSelect(pointData);
                    for(i=0;i<fieldsToSelect.length;i++) { 
                        var field = fieldsToSelect[i];
                        if(firstField==null && field.isNumeric) firstField = field;
                        var idBase = "cbx_" + collectionIdx +"_" +i;
                        var cbxId =  this.getDomId(idBase);
                        var cbx = $("#" + cbxId);
                        if(cbx.size()) {
                            cbxExists  = true;
                        } else {
                            continue;
                        }
                        if(cbx.is(':checked')) {
                            this.selectedCbx.push(field.getId());
                            df.push(field);
                        }
                    }
                }

                if(df.length == 0 && !cbxExists) {
                    if(this.lastSelectedFields && this.lastSelectedFields.length>0) {
                        return this.lastSelectedFields;
                    }
                }

                if(df.length == 0) {
                    return this.getDefaultSelectedFields(fieldsToSelect, dfltList);
                }
                return df;
            },
            getDefaultSelectedFields: function(fields, dfltList) {
                if(this.defaultSelectedToAll() && this.allFields!=null) {
                    var tmp = [];
                    for(i=0;i<this.allFields.length;i++) { 
                        var field = this.allFields[i];
                        if(!field.isFieldGeo()) {
                            tmp.push(field);
                        }
                    }
                    return  tmp;
                }

                if(dfltList!=null) {
                    return dfltList;
                }
                for(i=0;i<fields.length;i++) { 
                    var field = fields[i];
                    if(field.isNumeric && !field.isFieldGeo()) return [field];
                }
                return [];
            },
            getFieldById : function(fields, id) {
                if(!id) return null;
                if(!fields) {
                    var pointData = this.getData();
                    if(pointData == null) return null;
                    fields=  pointData.getRecordFields();
                }
                for(a in fields) {
                    var field = fields[a];
                    if(field.getId() == id) return field;
                }
                return null;
            },
            getFieldOfType: function(fields,type) {
                fields = this.getFieldsOfType(fields,type);
                if(fields.length==0) return null;
                return fields[0];
            },
            getFieldsOfType: function(fields,type) {
                var  list =[];
                var numeric = type == "numeric";
                for(a in fields) {
                    var field = fields[a];
                    if(numeric) {
                        if(field.isFieldNumeric()) {
                            list.push(field);
                        }
                    } else if(field.getType() == type) {
                        list.push(field);
                    }
                }
                return list;
            },
            getColumnValues: function(records, field) {
                var values=[];
                var min = Number.MAX_VALUE;
                var max = Number.MIN_VALUE;
                for(var rowIdx=0;rowIdx<records.length;rowIdx++)  {
                    var record = records[rowIdx];
                    var row = record.getData();
                    var value = row[field.getIndex()];
                    values.push(value);
                    if(Utils.isNumber(value)) {
                        min = Math.min(min,value);
                        max = Math.max(max,value);
                    }
                }
                return {values:values,min:min, max:max};
            },
            filterData: function(dataList, fields) {
                if(!dataList) {
                    var pointData = this.getData();
                    if(pointData == null) return null;
                    dataList=  pointData.getRecords();

                 }
                if(!fields) {
                    fields = pointData.getRecordFields();
                }
                var patternFieldId = this.getProperty("patternFilterField");
                var numericFieldId = this.getProperty("numericFilterField");
                var pattern = this.getProperty("filterPattern");
                var notPattern = false;
                if(pattern) {
                    notPattern= pattern.startsWith("!");
                    if(notPattern)  {
                        pattern=pattern.substring(1);
                    }
                }
                    
                var filterSValue = this.getProperty("numericFilterValue");
                var filterOperator = this.getProperty("numericFilterOperator","<");
               
                if((numericFieldId || patternFieldId)  && (pattern || (filterSValue && filterOperator))) {
                    var patternField = null;
                    var numericField = null;
                    for(var i=0;i<fields.length;i++) {
                        if(!patternField && (fields[i].getId() == patternFieldId || patternFieldId == "#"+(i+1))) {
                            patternField = fields[i];
                        }
                        if(!numericField && (fields[i].getId() == numericFieldId || numericFieldId == "#"+(i+1))) {
                            numericField = fields[i];
                        }
                        if(patternField && numericField) break;
                    }
                    if(patternField || numericField) {
                        var list = [];
                        var filterValue;
                        if(filterSValue) {
                            filterValue = parseFloat(filterSValue);
                        }
                        var standard = true;
                        for(var i=0;i<dataList.length;i++) {
                            var obj = dataList[i];
                            var row = this.getDataValues(obj);
                            var array  = row;
                            if(row.getData) {
                                standard = false;
                                array = row.getData();
                            }
                            if(standard  && i==0) {
                                list.push(obj);
                                continue;
                            }
                            var ok = false;
                            if(numericField && filterSValue && filterOperator) {
                                var value = parseFloat(array[numericField.getIndex()]);
                                var filterValue = parseFloat(filterSValue);
                                if(filterOperator == "<") {
                                    ok  = value<filterValue;
                                } else  if(filterOperator == "<=") {
                                    ok  = value<=filterValue;
                                } else  if(filterOperator == "==") {
                                    ok  = value==filterValue;
                                } else  if(filterOperator == ">") {
                                    ok  = value>filterValue;
                                } else  if(filterOperator == ">=") {
                                    ok  = value>=filterValue;
                                }
                                if(!ok) 
                                    continue;
                            }
                            if(patternField && pattern) {
                                var value = ""+array[patternField.getIndex()];
                                ok = value.match(pattern);
                                if(notPattern) ok = !ok;
                            }
                            if(ok) {
                                list.push(obj);
                            }
                        }
                        dataList = list;
                    }
                }
                return dataList;
            },


            canDoGroupBy: function() {
                return false;
            },
            canDoMultiFields: function() {
                return true;
            },
            getFieldsToSelect: function(pointData) {
                return  pointData.getChartableFields(this);
            },
            getGet: function() {
                return  "getRamaddaDisplay('" + this.getId() +"')";
            },
            publish: function(type) {
                if(type == null) type = "wikipage";
                var args = [];
                var name = prompt("Name", "");
                if(name == null) return;
                args.push("name");
                args.push(name);

                args.push("type");
                args.push(type);


                var desc = "";
                //                var desc = prompt("Description", "");
                //                if(desc == null) return;

                var wiki = "";
                if(type == "wikipage") {
                    wiki += "+section label=\"{{name}}\"\n${extra}\n";
                } else if(type == "blogentry") {
                    wiki = "<wiki>\n";
                }
                wiki += desc;
                wiki += this.getWikiText();
                for(var i=0;i<this.displays.length;i++) {
                    var display = this.displays[i];
                    if(display.getIsLayoutFixed()) {
                        continue;
                    }
                    wiki += display.getWikiText();
                }
                if(type == "wikipage") {
                    wiki += "-section\n\n";
                } else if(type == "blogentry") {
                }
                var from = "";
                var entries = this.getChildEntries();
                for(var i=0;i<entries.length;i++) {
                    var entry = entries[i];
                    from += entry.getId() +",";
                }

                if(entries.length>0) {
                    args.push("entries");
                    args.push(from);
                }

                if(type == "media_photoalbum") {
                    wiki = "";
                }

                args.push("description_encoded");
                args.push(window.btoa(wiki));
        

                var url = HtmlUtil.getUrl(ramaddaBaseUrl +"/entry/publish",args);
                window.open(url,  '_blank');
            },
            getChildEntries: function(includeFixed) {
                var seen = {};
                var allEntries = [];
                for(var i=0;i<this.displays.length;i++) {
                    var display = this.displays[i];
                    if(!includeFixed && display.getIsLayoutFixed()) {
                        continue;
                    }
                    if(display.getEntries) {
                        var entries = display.getEntries();
                        if(entries) {
                            for(var entryIdx=0;entryIdx<entries.length;entryIdx++) {
                                if(seen[entries[entryIdx].getId()]!=null) {
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
                for(var i=0;i<this.displays.length;i++) {
                    var display = this.displays[i];
                    if(display.getIsLayoutFixed()) {
                        continue;
                    }
                    if(display.getEntries) {
                        var entries = display.getEntries();
                        if(entries) {
                            for(var entryIdx=0;entryIdx<entries.length;entryIdx++) {
                                allEntries.push(entries[entryIdx]);
                            }
                        }
                    } 
                }
                return this.copyEntries(allEntries);
            },
            defineWikiAttributes: function(list) {
                for(var i=0;i<list.length;i++) {
                    if(this.wikiAttrs.indexOf(list[i])<0) {
                        this.wikiAttrs.push(list[i]);
                    }
                }
            },
            getWikiAttributes: function(attrs) {
                for(var i=0;i<this.wikiAttrs.length;i++) {
                    var v = this[this.wikiAttrs[i]];
                    if(Utils.isDefined(v)) {
                        attrs.push(this.wikiAttrs[i]);
                        attrs.push(v);
                    }
                }
            },
            getWikiText: function() {
                var attrs = ["layoutHere","false",
                             "type",this.type,
                             "column",  this.getColumn(),
                             "row",          this.getRow()];
                this.getWikiAttributes(attrs);
                var entryId = null;
                if(this.getEntries) {
                    var entries = this.getEntries();
                    if(entries && entries.length>0) {
                        entryId = entries[0].getId();
                    }
                }
                if(!entryId) {
                    entryId = this.entryId;
                }
                if(entryId) {
                    attrs.push("entry");
                    attrs.push(entryId);
                }
                var wiki ="{{display " +  HtmlUtil.attrs(attrs) + "}}\n\n"

                return wiki;
            },
            copyEntries: function(entries) {
                var allEntries = [];
                var seen = {};
                for(var entryIdx=0;entryIdx<entries.length;entryIdx++) {
                    var entry = entries[entryIdx];
                    if(seen[entry.getId()]!=null) continue;
                    seen[entry.getId()] = entry;
                    allEntries.push(entry);
                }
                var from = "";
                for(var i=0;i<allEntries.length;i++) {
                    var entry = allEntries[i];
                    from += entry.getId() +",";
                }


                var url = ramaddaBaseUrl +"/entry/copy?action.force=copy&from="  + from;
                window.open(url,  '_blank');

            },
            entryHtmlHasBeenDisplayed: function(entry) {
                if(entry.getIsGroup()/* && !entry.isRemote*/) {
                    var theDisplay = this;
                    var callback = function(entries) {
                        var html =  HtmlUtil.openTag(TAG_OL,[ATTR_CLASS,"display-entrylist-list", ATTR_ID,theDisplay.getDomId(ID_LIST)]);
                        html += theDisplay.getEntriesTree(entries);
                        html += HtmlUtil.closeTag(TAG_OL);
                        theDisplay.jq(ID_GROUP_CONTENTS + entry.getIdForDom()).html(html);
                        theDisplay.addEntrySelect();
                    };
                    var entries = entry.getChildrenEntries(callback);
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
                if(props.showHeader) {
                    var left = menu +" " + entry.getLink(entry.getIconImage() +" " + entry.getName());
                    if(props.headerRight) html += HtmlUtil.leftRight(left,props.headerRight);
                    else html += left;
                    //                    html += "<hr>";
                }
                var divid = HtmlUtil.getUniqueId("entry_");
                html += HtmlUtil.div(["id", divid],"");

                if(false) {
                    var url = this.getRamadda().getRoot() +"/entry/show?entryid=" + entry.getId() +"&decorate=false&output=metadataxml&details=true";
                    console.log(url);
                    $( "#" + divid).load( url, function() {
                            alert( "Load was performed." );
                        });
                }

                html += entry.getDescription();

                var metadata = entry.getMetadata();
                if(entry.isImage()) {
                    var img = HtmlUtil.tag(TAG_IMG,["src", entry.getResourceUrl(),  /*ATTR_WIDTH,"100%",*/
                                                    ATTR_CLASS,"display-entry-image"]);

                    html  += HtmlUtil.href(entry.getResourceUrl(), img) +"<br>";
                } else {
                    for(var i=0;i<metadata.length;i++) {
                        if(metadata[i].type == "content.thumbnail") {
                            var image = metadata[i].value.attr1;

                            var url;
                            if(image.indexOf("http") == 0) {
                                url = image;
                            } else {
                                url = ramaddaBaseUrl + "/metadata/view/" + image+"?element=1&entryid=" + entry.getId() +"&metadata.id=" + metadata[i].id + "&thumbnail=false";
                            }
                            html += HtmlUtil.image(url,[ATTR_CLASS,"display-entry-thumbnail"]);
                        }
                    }
                }
                if(entry.getIsGroup()/* && !entry.isRemote*/) {
                    html += HtmlUtil.div([ATTR_ID, this.getDomId(ID_GROUP_CONTENTS + entry.getIdForDom())],""/*this.getWaitImage()*/);
                }


                html += HtmlUtil.formTable();
                
                if(props.showDetails) {
                    if(entry.url) {
                        html+= HtmlUtil.formEntry("URL:", HtmlUtil.href(entry.url,entry.url));
                    }

                    if(entry.remoteUrl) {
                        html+= HtmlUtil.formEntry("URL:", HtmlUtil.href(entry.remoteUrl,entry.remoteUrl));
                    }
                    if(entry.remoteRepository) {
                        html+= HtmlUtil.formEntry("From:", HtmlUtil.href(entry.remoteRepository.url,entry.remoteRepository.name));
                    }
                }

                var columns = entry.getAttributes();

                if(entry.getFilesize()>0) {
                    html+= HtmlUtil.formEntry("File:", entry.getFilename() +" " +
                                              HtmlUtil.href(entry.getResourceUrl(), HtmlUtil.image(ramaddaBaseUrl +"/icons/download.png")) + " " +
                                              entry.getFormattedFilesize());
                }
                for(var colIdx =0;colIdx< columns.length;colIdx++) {
                    var column = columns[colIdx];
                    var columnValue = column.value;
                    if(column.getCanShow && !column.getCanShow()) {
                        continue;
                    }
                    if (Utils.isFalse(column.canshow)) {
                        continue;
                    }

                    if(column.isUrl && column.isUrl()) {
                        var tmp = "";
                        var toks = columnValue.split("\n");
                        for(var i=0;i<toks.length;i++) {
                            var url = toks[i].trim();
                            if(url.length==0) continue;
                            tmp += HtmlUtil.href(url, url);
                            tmp += "<br>";
                        }
                        columnValue = tmp;
                    }
                    html+= HtmlUtil.formEntry(column.label+":", columnValue);
                }

                html += HtmlUtil.closeTag(TAG_TABLE);


                return html;
        },

         getEntriesTree:function (entries, props) {
              if(!props) props = {};
                var columns = this.getProperty("columns",null);
                if(columns!=null) {
                    var columnNames = this.getProperty("columnNames",null);
                    if(columnNames!=null) {
                        columnNames = columnNames.split(",");
                    }
                    columns = columns.split(",");
                    var ids = [];
                    var names = [];
                    for(var i=0;i<columns.length;i++) {
                        var toks = columns[i].split(":");
                        var id=null, name = null;
                        if(toks.length>1) {
                            if(toks[0] == "property") {
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
                    if(columnNames==null) {
                        columnNames = names;
                    } 
                    return this.getEntriesTable(entries, columns, columnNames);
                }

                var suffix = props.suffix;
                var domIdSuffix = "";
                if(!suffix) {
                    suffix = "null";
                } else {
                    domIdSuffix = suffix;
                    suffix = "'" + suffix +"'";
                }

                var showIndex  = props.showIndex;

                var html = "";
                var rowClass = "entryrow_" + this.getId();
                var even = true;
                if(this.entriesMap==null) 
                    this.entriesMap = {};
                for(var i=0;i<entries.length;i++) {
                    even = !even;
                    var entry = entries[i];
                    this.entriesMap[entry.getId()] = entry;
                    var toolbar = this.makeEntryToolbar(entry);
                    var entryName = entry.getDisplayName();
                    if(entryName.length>100) {
                        entryName = entryName.substring(0,99)+"...";
                    }
                    var icon = entry.getIconImage([ATTR_TITLE,"View entry"]);
                    var link  =  HtmlUtil.tag(TAG_A,[ATTR_HREF, entry.getEntryUrl()],icon+" "+ entryName);
                    entryName  = "";
                    var entryIdForDom = entry.getIdForDom()+ domIdSuffix;
                    var entryId = entry.getId();
                    var  arrow = HtmlUtil.image(icon_tree_closed,[ATTR_BORDER,"0",
                                                                  "tree-open","false",
                                                                  ATTR_ID,
                                                                  this.getDomId(ID_TREE_LINK+entryIdForDom)]);

                    //                    console.log("ID:" + ID_TREE_LINK+entryIdForDom);

                    var toggleCall = this.getGet()+".toggleEntryDetails(event, '" + entryId+ "'," + suffix +");"; 
                    var toggleCall2 = this.getGet()+".entryHeaderClick(event, '" + entryId+ "'," + suffix +"); "; 
                    var open =  HtmlUtil.onClick(toggleCall,  arrow);
                    var extra = "";
                    if(showIndex) {
                        extra = "#" + (i+1) +" ";
                    }
                    var left =   HtmlUtil.div([ATTR_CLASS,"display-entrylist-name"],open +" " + extra + link +" " +  entryName);
                    var details = HtmlUtil.div([ATTR_ID,this.getDomId(ID_DETAILS+entryIdForDom), ATTR_CLASS,"display-entrylist-details"],HtmlUtil.div([ATTR_CLASS,"display-entrylist-details-inner",ATTR_ID,this.getDomId(ID_DETAILS_INNER+entryIdForDom)],""));

                    //                    console.log("details:" + details);

                    var line;
                    if(this.getProperty("showToolbar",true)) {
                        line = HtmlUtil.leftRight(left,toolbar,"8","4");
                    } else {
                        line = left;
                    }
                    //                    line = HtmlUtil.leftRight(left,toolbar,"60%","30%");


                    var mainLine = HtmlUtil.div(["onclick",toggleCall2, ATTR_ID, this.getDomId(ID_DETAILS_MAIN+ entryIdForDom), ATTR_CLASS,"display-entrylist-entry-main" + " " + "entry-main-display-entrylist-" +(even?"even":"odd"), ATTR_ENTRYID,entryId], line);
                    var line = HtmlUtil.div([ATTR_ID, this.getDomId("entryinner_" + entryIdForDom)], mainLine + details);

                    html  += HtmlUtil.tag(TAG_DIV,[ATTR_ID,
                                                this.getDomId("entry_" + entryIdForDom),
                                                  ATTR_ENTRYID,entryId, ATTR_CLASS,"display-entrylist-entry"+ rowClass], line);
                    html  += "\n";
                }
                return html;
            },
            addEntrySelect: function() {
                var theDisplay   =this;
                var entryRows = $("#" + this.getDomId(ID_DISPLAY_CONTENTS) +"  ." + this.getClass("entry-main"));

                entryRows.unbind();
                entryRows.mouseover(function(event){
                        //TOOLBAR
                        var entryId = $( this ).attr(ATTR_ENTRYID);
                        entry = theDisplay.getEntry(entryId);
                        if(!entry) {
                            console.log("no entry:" + entryId);
                            return;
                        }
                        theDisplay.getDisplayManager().handleEventEntryMouseover(theDisplay, {entry:entry});


                        if(true) return;
                        var domEntryId  =Utils.cleanId(entryId);
                        var toolbarId = theDisplay.getEntryToolbarId(domEntryId);

                        var toolbar = $("#" + toolbarId);
                        toolbar.show();
                        var myalign = 'right top+1';
                        var atalign = 'right top';
                        var srcId =  theDisplay.getDomId(ID_DETAILS_MAIN + domEntryId);
                        toolbar.position({
                                of: $( "#" +srcId ),
                                    my: myalign,
                                    at: atalign,
                                    collision: "none none"
                                    });

                    });
                entryRows.mouseout(function(event){
                        var entryId = $( this ).attr(ATTR_ENTRYID);
                        entry = theDisplay.getEntry(entryId);
                        if(!entry) return;
                        theDisplay.getDisplayManager().handleEventEntryMouseout(theDisplay, {entry:entry});
                        var domEntryId  =Utils.cleanId(entryId);
                        var toolbarId = theDisplay.getEntryToolbarId(entryId);
                        var toolbar = $("#" + toolbarId);
                        //TOOLBAR                        toolbar.hide();
                    });

                if(this.madeList) {
                    //                    this.jq(ID_LIST).selectable( "destroy" );
                }
                this.madeList = true;
                if(false) {
                this.jq(ID_LIST).selectable({
                        //                        delay: 0,
                        //                        filter: 'li',
                        cancel: 'a',
                        selected: function( event, ui ) {
                            var entryId = ui.selected.getAttribute(ATTR_ENTRYID);
                            theDisplay.toggleEntryDetails(event, entryId);
                            if(true)     return;

                            theDisplay.hideEntryDetails(entryId);
                            var entry = theDisplay.getEntry(entryId);
                            if(entry == null) return;

                            var zoom = null;
                            if(event.shiftKey) {
                                zoom = {zoomIn:true};
                            }
                            theDisplay.selectedEntries.push(entry);
                            theDisplay.getDisplayManager().handleEventEntrySelection(theDisplay, {entry:entry, selected:true, zoom:zoom});
                            theDisplay.lastSelectedEntry = entry;
                        },
                        unselected: function( event, ui ) {
                            if(true) return;
                            var entryId = ui.unselected.getAttribute(ATTR_ENTRYID);
                            var entry = theDisplay.getEntry(entryId);
                            var index = theDisplay.selectedEntries.indexOf(entry);
                            //                            console.log("remove:" +  index + " " + theDisplay.selectedEntries);
                            if (index > -1) {
                                theDisplay.selectedEntries.splice(index, 1);
                                theDisplay.getDisplayManager().handleEventEntrySelection(theDisplay, {entry:entry, selected:false});
                            }
                        },
                            
                    });
                }

            },
            getEntriesTable:function (entries, columns, columnNames) {
                if(this.entriesMap==null) 
                    this.entriesMap = {};
                var columnWidths = this.getProperty("columnWidths",null);
                if(columnWidths!=null) {
                    columnWidths  = columnWidths.split(",");
                }
                var html = HtmlUtil.openTag(TAG_TABLE,[ATTR_WIDTH,"100%","cellpadding", "0","cellspacing","0"]);
                html += HtmlUtil.openTag(TAG_TR,["valign","top"]);
                for(var i=0;i<columnNames.length;i++) {
                    html += HtmlUtil.td([ATTR_ALIGN,"center", ATTR_CLASS, "display-entrytable-header"],columnNames[i]);
                }
                html += HtmlUtil.closeTag(TAG_TR);

                for(var i=0;i<entries.length;i++) {
                    html += HtmlUtil.openTag(TAG_TR,["valign","top"]);
                    var entry = entries[i];
                    this.entriesMap[entry.getId()] = entry;
                    for(var j=0;j<columns.length;j++) {
                        var columnWidth = null;
                        if(columnWidths!=null) {
                            columnWidth= columnWidths[j];
                        }
                        var column = columns[j];
                        var value = "";
                        if(column == "name") {
                            value =   HtmlUtil.tag(TAG_A,[ATTR_HREF, entry.getEntryUrl()],entry.getName());
                        } else if(column.match(".*property:.*")) {
                            var type = column.substring("property:".length);
                            var metadata = entry.getMetadata();
                            value = "";
                            for(var j=0;j<metadata.length;j++) {
                                var m = metadata[j];
                                if(m.type == type) {
                                    if(value!="") {
                                        value+="<br>";
                                    }
                                    value += m.value.attr1;
                                }
                            }
                        } else if(column == "description") {
                            value = entry.getDescription();
                        } else if(column == "date") {
                            value = entry.ymd;
                            if(value == null) {
                                value = entry.startDate;
                            }

                        } else {
                            value = entry.getAttributeValue(column);
                        }
                        var attrs = [ATTR_CLASS, "display-entrytable-cell"];
                        if(columnWidth!=null) {
                            attrs.push(ATTR_WIDTH);
                            attrs.push(columnWidth);
                        }

                        html += HtmlUtil.td(attrs,value);
                    }
                    html += HtmlUtil.closeTag(TAG_TR);
                }
                html += HtmlUtil.closeTag(TAG_TABLE);
                return html;
            },

             makeEntryToolbar: function(entry) {
                 var get = this.getGet();
                 var toolbarItems = [];




                 //                 toolbarItems.push(HtmlUtil.tag(TAG_A, [ATTR_HREF, entry.getEntryUrl(),"target","_"], 
                 //                                                HtmlUtil.image(ramaddaBaseUrl +"/icons/application-home.png",["border",0,ATTR_TITLE,"View Entry"])));
                 if(entry.getType().getId() == "type_wms_layer") {
                     toolbarItems.push(HtmlUtil.tag(TAG_A, ["onclick", get+".addMapLayer(" + HtmlUtil.sqt(entry.getId()) + ");"],
                                                            HtmlUtil.image(ramaddaBaseUrl +"/icons/map.png",["border",0,ATTR_TITLE,"Add Map Layer"])));

                 }
                 if(entry.getType().getId() == "geo_shapefile") {
                     toolbarItems.push(HtmlUtil.tag(TAG_A, ["onclick", get+".addMapLayer(" + HtmlUtil.sqt(entry.getId()) + ");"],
                                                            HtmlUtil.image(ramaddaBaseUrl +"/icons/map.png",["border",0,ATTR_TITLE,"Add Map Layer"])));

                 }

                 var jsonUrl = this.getPointUrl(entry);
                 if(jsonUrl!=null) {
                     toolbarItems.push(HtmlUtil.tag(TAG_A, ["onclick", get+".createDisplay(" + HtmlUtil.sqt(entry.getFullId()) +"," +
                                                            HtmlUtil.sqt("table") +"," + HtmlUtil.sqt(jsonUrl)+");"],
                                                            HtmlUtil.image(ramaddaBaseUrl +"/icons/table.png",["border",0,ATTR_TITLE,"Create Tabular Display"])));

                     var x;
                     toolbarItems.push(x = HtmlUtil.tag(TAG_A, ["onclick", get+".createDisplay(" + HtmlUtil.sqt(entry.getFullId()) +"," +
                                                            HtmlUtil.sqt("linechart") +"," + HtmlUtil.sqt(jsonUrl)+");"],
                                                            HtmlUtil.image(ramaddaBaseUrl +"/icons/chart_line_add.png",["border",0,ATTR_TITLE,"Create Chart"])));
                 }
                 toolbarItems.push(HtmlUtil.tag(TAG_A, ["onclick", get+".createDisplay(" + HtmlUtil.sqt(entry.getFullId()) +"," +
                                                            HtmlUtil.sqt("entrydisplay") +"," + HtmlUtil.sqt(jsonUrl)+");"],
                                                            HtmlUtil.image(ramaddaBaseUrl +"/icons/layout_add.png",["border",0,ATTR_TITLE,"Show Entry"])));

                 if(entry.getFilesize()>0) {
                     toolbarItems.push(HtmlUtil.tag(TAG_A, [ATTR_HREF, entry.getResourceUrl()], 
                                                    HtmlUtil.image(ramaddaBaseUrl +"/icons/download.png",["border",0,ATTR_TITLE,"Download (" + entry.getFormattedFilesize() +")"])));
                     
                 }


                 var entryMenuButton = this.getEntryMenuButton(entry);
                 /*
                 entryMenuButton =  HtmlUtil.onClick(this.getGet()+".showEntryDetails(event, '" + entry.getId() +"');", 
                                               HtmlUtil.image(ramaddaBaseUrl+"/icons/downdart.png", 
                                                              [ATTR_CLASS, "display-dialog-button", ATTR_ID,  this.getDomId(ID_MENU_BUTTON + entry.getId())]));

                 */

                 toolbarItems.push(entryMenuButton);

                 var tmp = [];
                 for(var i=0;i<toolbarItems.length;i++) {
                     tmp.push(HtmlUtil.div([ATTR_CLASS,"display-entry-toolbar-item"], toolbarItems[i]));
                 }
                 toolbarItems = tmp;
                 return HtmlUtil.div([ATTR_CLASS,"display-entry-toolbar",ATTR_ID,
                                      this.getEntryToolbarId(entry.getIdForDom())],
                                     HtmlUtil.join(toolbarItems,""));
             },
             getEntryToolbarId: function(entryId) {
                 var id = entryId.replace(/:/g,"_");
                 id = id.replace(/=/g,"_");
                 return this.getDomId(ID_TOOLBAR +"_" + id);
            },

            hideEntryDetails: function(entryId) {
                //                var popupId = "#"+ this.getDomId(ID_DETAILS + entryId);
                //                $(popupId).hide();
                //                this.currentPopupEntry = null;
            },
            entryHeaderClick: function(event, entryId, suffix) {
                var target  = event.target; 
                //A hack to see if this was the div clicked on or a link in the div
                if(target.outerHTML) {
                    if(target.outerHTML.indexOf("<div")!=0) {
                        return;
                    }
                }
                this.toggleEntryDetails(event, entryId);
            },
            toggleEntryDetails: function(event, entryId, suffix) {
                var entry = this.getEntry(entryId);
                //                console.log("toggleEntryDetails:" + entry.getName() +" " + entry.getId());
                if(suffix == null) suffix = "";
                var link = this.jq(ID_TREE_LINK+entry.getIdForDom()+suffix);
                var id = ID_DETAILS + entry.getIdForDom()+suffix;
                var details = this.jq(id);
                var detailsInner = this.jq(ID_DETAILS_INNER + entry.getIdForDom()+suffix);


                if(event && event.shiftKey) {
                    var id = Utils.cleanId(entryId);
                    var line  = this.jq(ID_DETAILS_MAIN+ id);
                    if(!this.selectedEntriesFromTree) {
                        this.selectedEntriesFromTree = {};
                    }
                    var selected  =  line.attr("ramadda-selected") =="true";
                    if(selected) {
                        line.removeClass("display-entrylist-entry-main-selected");
                        line.attr("ramadda-selected","false");
                        this.selectedEntriesFromTree[entry.getId()] = null;
                    } else {
                        line.addClass("display-entrylist-entry-main-selected");
                        line.attr("ramadda-selected","true");
                        this.selectedEntriesFromTree[entry.getId()] = entry;
                    }
                    this.getDisplayManager().handleEventEntrySelection(this,  {"entry":entry, "selected":!selected});
                    return;
                }


                var open = link.attr("tree-open")=="true";
                if(open) {
                    link.attr("src", icon_tree_closed);
                } else {
                    link.attr("src", icon_tree_open);
                }
                link.attr("tree-open", open?"false":"true");

                var hereBefore  =  details.attr("has-content") !=null;
                details.attr("has-content","true");
                if(hereBefore) {
                    //                    detailsInner.html(HtmlUtil.image(icon_progress));
                } else {
                    if(entry.getIsGroup()/* && !entry.isRemote*/) {
                        detailsInner.html(HtmlUtil.image(icon_progress));
                        var theDisplay = this;
                        var callback = function(entries) {
                            theDisplay.displayChildren(entry, entries, suffix);
                        };
                        var entries = entry.getChildrenEntries(callback);
                    } else {
                        detailsInner.html(this.getEntryHtml(entry,{showHeader:false}));
                    }
                }


                if(open) {
                    details.hide();
                } else {
                    details.show();
                }
                if(event &&  event.stopPropagation) { 
                    event.stopPropagation(); 
                }
            },
            getSelectedEntriesFromTree: function() {
                var selected = [];
                if(this.selectedEntriesFromTree) {
                    for(var id in this.selectedEntriesFromTree) {
                        var entry = this.selectedEntriesFromTree[id];
                        if(entry!=null) {
                            selected.push(entry);
                        }
                    }
                }
                return selected;
            },
            displayChildren: function(entry, entries, suffix) {
                if(!suffix) suffix = "";
                var detailsInner = this.jq(ID_DETAILS_INNER + entry.getIdForDom()+suffix);
                var details = this.getEntryHtml(entry,{showHeader:false});
                if(entries.length==0) {
                    detailsInner.html(details);
                } else {
                    var entriesHtml  = "";
                    if(this.showDetailsForGroup) {
                        entriesHtml += details;
                    }
                    entriesHtml +=this.getEntriesTree(entries);
                    detailsInner.html(entriesHtml);
                    this.addEntrySelect();
                }
            },


        getEntryMenuButton: function(entry) {
             var menuButton = HtmlUtil.onClick(this.getGet()+".showEntryMenu(event, '" + entry.getId() +"');", 
                                               HtmlUtil.image(ramaddaBaseUrl+"/icons/menu.png", 
                                                              [ATTR_CLASS, "display-entry-toolbar-item", ATTR_ID,  this.getDomId(ID_MENU_BUTTON + entry.getIdForDom())]));
             return menuButton;
         },
         setRamadda: function(e) {
                this.ramadda = e;
         },
         getRamadda: function() {
                if(this.ramadda!=null) {
                    return this.ramadda;
                }
                if(this.ramaddaBaseUrl !=null) {
                    this.ramadda =  getRamadda(this.ramaddaBaseUrl);
                    return this.ramadda;
                }
                return getGlobalRamadda();
        },
       getEntry: function(entryId, callback) {
                if(this.entriesMap && this.entriesMap[entryId])
                    return this.entriesMap[entryId];

                var ramadda = this.getRamadda();
                var toks = entryId.split(",");
                if(toks.length==2) {
                    entryId = toks[1];
                    ramadda = getRamadda(toks[0]);
                }
                var entry = null;
                if(this.entryList!=null) {
                    entry = this.entryList.getEntry(entryId);
                }
                if(entry == null) {
                    entry = ramadda.getEntry(entryId);
                }
                if(entry == null) {
                    //                    var e = new Error('dummy');
                    console.log("Display.getEntry: entry not found id=" + entryId +" repository=" + ramadda.getRoot());
                    //                    var stack = e.stack;
                    //                    console.log(stack);
                    entry = this.getRamadda().getEntry(entryId, callback);
                }
                return entry;
            },
            addMapLayer: function(entryId) {
                var entry = this.getEntry(entryId);
                if(entry == null) {
                    console.log("No entry:" + entryId);
                    return;
                }
                this.getDisplayManager().addMapLayer(this, entry);
              
            },
            createDisplay: function(entryId, displayType, jsonUrl) {
                var entry = this.getEntry(entryId);
                console.log("createDisplay: " + entryId + " " + displayType +" " + jsonUrl);
                if(entry == null) {
                    console.log("No entry:" + entryId);
                    return null;
                }
                var props = {
                    showMenu: true,
                    sourceEntry: entry,
                    entryId: entry.getId(),
                    showTitle: true,
                    showDetails:true,
                    title: entry.getName(),
                };

                //TODO: figure out when to create data, check for grids, etc
                if(displayType != DISPLAY_ENTRYLIST) {
                    if(jsonUrl == null) {
                        jsonUrl = this.getPointUrl(entry);
                    }
                    var pointDataProps = {
                        entry: entry,
                        entryId: entry.getId()
                    };
                    props.data = new PointData(entry.getName(), null, null, jsonUrl,pointDataProps);
                }
                if(this.lastDisplay!=null) {
                    props.column = this.lastDisplay.getColumn();
                    props.row = this.lastDisplay.getRow();
                } else {
                    props.column = this.getProperty("newColumn",this.getColumn());
                    props.row = this.getProperty("newRow",this.getRow());
                }
                this.lastDisplay = this.getDisplayManager().createDisplay(displayType, props);
            },
            getPointUrl: function(entry) {
                //check if it has point data
                var service = entry.getService("points.json");
                if(service!=null) {
                    return  service.url;
                }
                service = entry.getService("grid.point.json");
                if(service!=null) {
                    return  service.url;
                }
                return null;
            },
            getEntryMenu: function(entryId) {
                var entry = this.getEntry(entryId);
                if(entry == null) {
                    return "null entry";
                }


                var get = this.getGet();
                var menus = [];
                var fileMenuItems = [];
                var viewMenuItems = [];
                var newMenuItems = [];
                viewMenuItems.push(HtmlUtil.tag(TAG_LI,[], HtmlUtil.tag(TAG_A, ["href", entry.getEntryUrl(),"target","_"], "View Entry")));
                if(entry.getFilesize()>0) {
                    fileMenuItems.push(HtmlUtil.tag(TAG_LI,[], HtmlUtil.tag(TAG_A, ["href", entry.getResourceUrl()], "Download " + entry.getFilename() + " (" + entry.getFormattedFilesize() +")")));
                }

                if(this.jsonUrl!=null) {
                    fileMenuItems.push(HtmlUtil.tag(TAG_LI,[], "Data: " + HtmlUtil.onClick(get+".fetchUrl('json');", "JSON")
                                                    + HtmlUtil.onClick(get+".fetchUrl('csv');", "CSV")));
                }

                newMenuItems.push(HtmlUtil.tag(TAG_LI,[], HtmlUtil.onClick(get+".createDisplay('" + entry.getFullId() +"','entrydisplay');", "New Entry Display")));


                //check if it has point data
                var pointUrl = this.getPointUrl(entry);
                console.log("entry:" + entry.getName() +" url:" +  pointUrl);

                if(pointUrl!=null) {
                    var newMenu = "";
                    for(var i=0;i<this.getDisplayManager().displayTypes.length;i++) {
                        var type = this.getDisplayManager().displayTypes[i];
                        if(!type.requiresData || !type.forUser) continue;
                        
                        newMenuItems.push(HtmlUtil.tag(TAG_LI,[], HtmlUtil.tag(TAG_A, ["onclick", get+".createDisplay(" + HtmlUtil.sqt(entry.getFullId()) +"," + HtmlUtil.sqt(type.type)+"," +HtmlUtil.sqt(pointUrl) +");"], type.label)));
                    }
                    //                    menus.push("<a>New Chart</a>" + HtmlUtil.tag(TAG_UL,[], newMenu));
                }


                if(fileMenuItems.length>0)
                    menus.push("<a>File</a>" + HtmlUtil.tag(TAG_UL,[], HtmlUtil.join(fileMenuItems)));
                if(viewMenuItems.length>0)
                    menus.push("<a>View</a>" + HtmlUtil.tag(TAG_UL,[], HtmlUtil.join(viewMenuItems)));
                if(newMenuItems.length>0)
                    menus.push("<a>New</a>" + HtmlUtil.tag(TAG_UL,[], HtmlUtil.join(newMenuItems)));


                var topMenus = "";
                for(var i=0;i<menus.length;i++) {
                    topMenus += HtmlUtil.tag(TAG_LI,[], menus[i]);
                }

                var menu = HtmlUtil.tag(TAG_UL, [ATTR_ID, this.getDomId(ID_MENU_INNER+entry.getIdForDom()),ATTR_CLASS, "sf-menu"], 
                                        topMenus);
                return menu;
            },
            showEntryMenu: function(event, entryId) {
                var menu = this.getEntryMenu(entryId);               
                this.writeHtml(ID_MENU_OUTER, menu);
                var srcId = this.getDomId(ID_MENU_BUTTON + Utils.cleanId(entryId));

                showPopup(event, srcId, this.getDomId(ID_MENU_OUTER), false,"right top","left bottom+9");


                $("#"+  this.getDomId(ID_MENU_INNER+Utils.cleanId(entryId))).superfish({
                    //Don't set animation - it is broke on safari
                        //                        animation: {height:'show'},
                        speed: 'fast',
                            delay: 300
                            });
           },
           fetchUrl: function(as, url) {
                if(url == null) {
                    url = this.jsonUrl;
                }
                url =  this.getDisplayManager().getJsonUrl(url, this);
                if(url == null) return;
                if(as !=null && as != "json") {
                    url = url.replace("points.json","points." + as);
                }
                window.open(url,'_blank');
            },
            getMenuItems: function(menuItems) {
                
            },
            getDisplayMenuSettings: function() {
                var get = "getRamaddaDisplay('" + this.getId() +"')";
                var moveRight = HtmlUtil.onClick(get +".moveDisplayRight();", "Right");
                var moveLeft = HtmlUtil.onClick(get +".moveDisplayLeft();", "Left");
                var moveTop = HtmlUtil.onClick(get +".moveDisplayTop();", "Top");
                var moveUp = HtmlUtil.onClick(get +".moveDisplayUp();", "Up");
                var moveDown = HtmlUtil.onClick(get +".moveDisplayDown();", "Down");


                var menu =  "<table class=formtable>" +
                    "<tr><td align=right><b>Move:</b></td><td>" + moveTop + " " + moveUp + " " +moveDown+  " " +moveRight+ " " + moveLeft +"</td></tr>"  +
                    "<tr><td align=right><b>Row:</b></td><td> " + HtmlUtil.input("", this.getProperty("row",""), ["size","7",ATTR_ID,  this.getDomId("row")]) + " &nbsp;&nbsp;<b>Col:</b> " +  HtmlUtil.input("", this.getProperty("column",""), ["size","7",ATTR_ID,  this.getDomId("column")]) + "</td></tr>" +

                    //                    "<tr><td align=right><b>Name:</b></td><td> " + HtmlUtil.input("", this.getProperty("name",""), ["size","7",ATTR_ID,  this.getDomId("name")]) + "</td></tr>" +
                    //                    "<tr><td align=right><b>Source:</b></td><td>"  + 
                    //                    HtmlUtil.input("", this.getProperty("eventsource",""), ["size","7",ATTR_ID,  this.getDomId("eventsource")]) +
                    //                    "</td></tr>" +
                    "<tr><td align=right><b>Width:</b></td><td> " + HtmlUtil.input("", this.getProperty("width",""), ["size","7",ATTR_ID,  this.getDomId("width")]) + "  " +     "<b>Height:</b> " + HtmlUtil.input("", this.getProperty("height",""), ["size","7",ATTR_ID,  this.getDomId("height")]) + "</td></tr>" +
                    "</table>";
                var tmp = 
                    HtmlUtil.checkbox(this.getDomId("showtitle"), [], this.showTitle) +" Title  " +
                    HtmlUtil.checkbox(this.getDomId("showdetails"), [], this.showDetails) +" Details " +
                    "&nbsp;&nbsp;&nbsp;" +
                    HtmlUtil.onClick(get +".askSetTitle();", "Set Title");
                menu += HtmlUtil.formTable() + HtmlUtil.formEntry("Show:", tmp) +"</table>";

                return menu;
           },
           isLayoutHorizontal: function(){ 
                return this.orientation == "horizontal";
            },
            loadInitialData: function() {
                if(!this.needsData() || this.properties.data==null) {
                    return;
                } 
                if(this.properties.data.hasData()) {
                    this.addData(this.properties.data);
                    return;
                } 
                this.properties.data.derived = this.derived;
                this.properties.data.loadData(this);
            },
            getData: function() {
                if(!this.hasData()) return null;
                var dataList =  this.dataCollection.getList();
                return dataList[0];
            },
            hasData: function() {
                if(this.dataCollection == null) return false;
                return this.dataCollection.hasData();
            },
            getCreatedInteractively: function() {
                return this.createdInteractively == true;
            },
            needsData: function() {
                return false;
            },
            getShowMenu: function() {
                if(Utils.isDefined(this.showMenu)) return this.showMenu;
                return this.getProperty(PROP_SHOW_MENU, true);
            },
            askSetTitle: function() {
                var t  =  this.getTitle(false);
                var v = prompt("Title", t);
                if(v!=null) {
                    this.title = v;
                    this.setProperty(ATTR_TITLE, v);
                    this.setDisplayTitle(this.title);
                }
           },
           getShowDetails:function() {
                return this.getSelfProperty("showDetails", true);
            },
           setShowDetails:function(v) {
                this.showDetails = v;
                if(this.showDetails) {
                    this.jq(ID_DETAILS).show();
                } else {
                    this.jq(ID_DETAILS).hide();
                }
            },
           setShowTitle:function(v) {
                this.showTitle = v;
                if(this.showTitle) {
                    this.jq(ID_TITLE).show();
                } else {
                    this.jq(ID_TITLE).hide();
                }
            },
            getShowTitle: function() {
                return this.getSelfProperty("showTitle", true);
            },
            setDisplayProperty: function(key,value) {
                this.setProperty(key, value);
                $("#" + this.getDomId(key)).val(value);
            },
            deltaColumn: function(delta) {
                var column = parseInt(this.getProperty("column",0));
                column += delta;
                if(column<0) column = 0;
                this.setDisplayProperty("column",column);
                this.getLayoutManager().layoutChanged(this);
            },
            deltaRow: function(delta) {
                var row = parseInt(this.getProperty("row",0));
                row += delta;
                if(row<0) row = 0;
                this.setDisplayProperty("row",row);
                this.getLayoutManager().layoutChanged(this);
            },
            moveDisplayRight: function() {
                if(this.getLayoutManager().isLayoutColumns()) {
                    this.deltaColumn(1);
                } else {
                    this.getLayoutManager().moveDisplayDown(this);
                }
            },
            moveDisplayLeft: function() {
                if(this.getLayoutManager().isLayoutColumns()) {
                    this.deltaColumn(-1);
                } else {
                    this.getLayoutManager().moveDisplayUp(this);
                }
            },
            moveDisplayUp: function() {
                if(this.getLayoutManager().isLayoutRows()) {
                    this.deltaRow(-1);
                } else {
                    this.getLayoutManager().moveDisplayUp(this);
                }
            },
            moveDisplayDown: function() {
                if(this.getLayoutManager().isLayoutRows()) {
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
                for(var i=0;i<menuItems.length;i++) {
                    form += HtmlUtil.div([ATTR_CLASS,"display-menu-item"], menuItems[i]);
                }
                form += "</form>";
                tabTitles.push("Display"); 
                tabContents.push(form);
            },
           popup: function(srcId, popupId) {
                var popup = GuiUtils.getDomObject(popupId);
                var srcObj = GuiUtils.getDomObject(srcId);
                if(!popup || !srcObj) return;
                var myalign = 'left top';
                var atalign = 'left bottom';
                showObject(popup);
                jQuery("#"+popupId ).position({
                        of: jQuery( "#" + srcId ),
                            my: myalign,
                            at: atalign,
                            collision: "none none"
                            });
                //Do it again to fix a bug on safari
                jQuery("#"+popupId ).position({
                        of: jQuery( "#" + srcId ),
                            my: myalign,
                            at: atalign,
                            collision: "none none"
                            });

                $("#" + popupId).draggable();
            },
            checkLayout: function() {
            },
            displayData: function() {
            },
            createUI:function() {
                this.checkFixedLayout();
            },
            setDisplayReady: function() {
                this.displayReady = true;
            },
            getDisplayReady: function() {
                return this.displayReady;
            },
            pageHasLoaded:function() {
                this.setDisplayReady(true);
            },
            initDisplay:function() {
                this.createUI();
            },
            updateUI: function() {
            },
            getDoBs: function() {
                if (!(typeof this.dobs === 'undefined')) {
                    return dobs;
                }
                if(this.displayParent) {
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
                var width = this.getWidth();
                if(dobs) {
                    html += HtmlUtil.openDiv(["class","minitron"]);
                }
                if(width>0) {
                    html += HtmlUtil.openDiv(["class","display-contents","style","width:" + width +"px;"]);
                } else {
                    html += HtmlUtil.openDiv(["class","display-contents"]);
                }

                html+= HtmlUtil.div([ATTR_CLASS,"ramadda-popup", ATTR_ID, this.getDomId(ID_MENU_OUTER)], "");
                var menu = HtmlUtil.div([ATTR_CLASS, "display-dialog", ATTR_ID, this.getDomId(ID_DIALOG)], "");
                var get = this.getGet();
                var button = "";
                if(this.getShowMenu()) {
                    button = HtmlUtil.onClick(get+".showDialog();", 
                                              HtmlUtil.image(ramaddaBaseUrl+"/icons/downdart.png",
                                                             [ATTR_CLASS, "display-dialog-button", ATTR_ID,  this.getDomId(ID_DIALOG_BUTTON)]));
                }
                var title = "";
                if(this.getShowTitle()) {
                    title= this.getTitle(false).trim();
                }

                if(button!= "" || title!="") {
                    this.cnt++;
                    var titleDiv = "";
                    var label = title;
                    if(title!="" && this.entryId) {
                        label = HtmlUtil.href(this.getRamadda().getEntryUrl(this.entryId),title);
                    }
                    titleDiv = HtmlUtil.tag("span", [ATTR_CLASS,"display-title",ATTR_ID,this.getDomId(ID_TITLE)], this.getDisplayTitle(title));
                    if(button== "") {
                        html += titleDiv;
                    } else {
                        html += "<div class=display-header>" + button +"&nbsp;" + titleDiv +"</div>";
                    }
                }

                var contents = this.getContentsDiv();
                //                contents  = "CONTENTS";
                html += contents;
                html += menu;
                html += HtmlUtil.closeTag(TAG_DIV);
                if(dobs) {
                    html += HtmlUtil.closeTag(TAG_DIV);
                }
                return html;
            },
            makeToolbar : function(props) {
                var toolbar = "";
                var get = this.getGet();
                var addLabel = props.addLabel;
                var images = [];
                var calls = [];
                var labels = [];
                if(!this.getIsLayoutFixed()) {
                    calls.push("removeRamaddaDisplay('" + this.getId() +"')");
                    images.push(ramaddaBaseUrl+"/icons/page_delete.png");
                    labels.push("Delete display");
                }
                calls.push(get+".copyDisplay();");
                images.push(ramaddaBaseUrl+"/icons/page_copy.png");
                labels.push("Copy Display");
                if(this.jsonUrl!=null) {
                    calls.push(get+".fetchUrl('json');");
                    images.push(ramaddaBaseUrl+"/icons/json.png");
                    labels.push("Download JSON");
                    
                    calls.push(get+".fetchUrl('csv');"); 
                    images.push(ramaddaBaseUrl+"/icons/csv.png");
                    labels.push("Download CSV");
                }
                for(var i=0;i<calls.length;i++) {
                    var inner = HtmlUtil.image(images[i],[ATTR_TITLE,labels[i], ATTR_CLASS,"display-dialog-header-icon"]);
                    if(addLabel)  inner += " " + labels[i] +"<br>";
                    toolbar += HtmlUtil.onClick(calls[i], inner);
                }
                return toolbar;
            },
             makeDialog: function() {
                var html = "";
                html +=   HtmlUtil.div([ATTR_ID, this.getDomId(ID_HEADER),ATTR_CLASS, "display-header"]);
                var close =  HtmlUtil.onClick("$('#" +this.getDomId(ID_DIALOG) +"').hide();",HtmlUtil.image(ramaddaBaseUrl +"/icons/close.gif",[ATTR_CLASS,"display-dialog-close", ATTR_TITLE,"Close Dialog"]));
                var right = close;
                var left = "";
                //                var left = this.makeToolbar({addLabel:true});
                var header = HtmlUtil.div([ATTR_CLASS,"display-dialog-header"], HtmlUtil.leftRight(left,right));
                var tabTitles = [];
                var tabContents = [];
                this.getDialogContents(tabTitles, tabContents);
                tabTitles.push("Edit");
                tabContents.push(this.makeToolbar({addLabel:true}));
                var tabLinks = "<ul>";
                var tabs = "";
                for(var i=0;i<tabTitles.length;i++) {
                    var id = this.getDomId("tabs")+ i;
                    tabLinks += HtmlUtil.tag("li", [], HtmlUtil.tag("a", ["href", "#" + id], 
                                                                    tabTitles[i]));
                    tabLinks += "\n";
                    var contents = HtmlUtil.div([ATTR_CLASS,"display-dialog-tab"],tabContents[i]);
                    tabs+= HtmlUtil.div(["id", id],contents);
                    tabs += "\n";
                }
                tabLinks += "</ul>\n";
                var tabs = HtmlUtil.div(["id",this.getDomId(ID_DIALOG_TABS)], tabLinks + tabs);
                dialogContents  = header + tabs;
                return dialogContents;
            },
            initDialog: function() {
                var _this  = this;
                var updateFunc  = function(e) {
                    if(e && e.which != 13) {
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
                var dialog =this.getDomId(ID_DIALOG); 
                this.writeHtml(ID_DIALOG, this.makeDialog());
                this.popup(this.getDomId(ID_DIALOG_BUTTON), dialog);
                this.initDialog();
            },
            getWidthForStyle: function(dflt) {
                var width = this.getProperty("width",-1);
                if(width == -1) return dflt;
                if(!width.endsWith("px") && !width.endsWith("%"))
                    width = width+"px";
                return width;
            },
            getHeightForStyle: function(dflt) {
                var height = this.getProperty("height",-1);
                if(height == -1) return dflt;
                if(!height.endsWith("px") && !height.endsWith("%"))
                    height = height+"px";
                return height;
            },
            getDimensionsStyle: function() {
                var style = "";
                var height = this.getHeightForStyle();
                if(height) {
                    style +=  " height:" + height +";";
                }
                var width = this.getWidthForStyle();
                if(width) {
                    style +=  " width:" + width+";";
                }
                return style;
            },
            getContentsDiv: function() {
                var extraStyle =  this.getDimensionsStyle();
                return  HtmlUtil.div([ATTR_CLASS,"display-contents-inner display-" +this.type, "style", extraStyle, ATTR_ID, this.getDomId(ID_DISPLAY_CONTENTS)],"");
            },
            copyDisplay: function() {
                var newOne = {};
                $.extend(true, newOne, this);
                newOne.setId(newOne.getId() +this.getUniqueId("display"));
                addRamaddaDisplay(newOne);
                this.getDisplayManager().addDisplay(newOne);
            },
            removeDisplay: function() {
                this.getDisplayManager().removeDisplay(this);
            },
            //Gets called before the displays are laid out
            prepareToLayout:function() {
                //Force setting the property from the input dom (which is about to go away)
                this.getColumn();
                this.getWidth();
                this.getHeight();
                this.getName();
                this.getEventSource();
            },
            getColumn: function() {
                return this.getFormValue("column",0);
            },
            getRow: function() {
                return this.getFormValue("row",0);
            },
            getWidth: function() {
                return this.getFormValue("width",0);
            },
            getHeight: function() {
                return this.getFormValue("height",0);
            },
            getDisplayTitle: function(title) {
                if(!title) title = this.title!=null?this.title:"";
                var text = title;
                var fields = this.lastSelectedFields;
                if(fields && fields.length>0) 
                    text = text.replace("{field}", fields[0].getLabel());
                else
                    text = text.replace("{field}", "");
                return text;
            },

            setDisplayTitle: function(title) {
                if(!Utils.stringDefined(title)) {
                    title= this.getTitle(false).trim();
                }
                var text = this.getDisplayTitle(title);
                if(this.entryId) {
                    text = HtmlUtil.href(this.getRamadda().getEntryUrl(this.entryId),text);
                }
                if(this.getShowTitle()) {
                    this.jq(ID_TITLE).show();
                } else {
                    this.jq(ID_TITLE).hide();
                }
                this.writeHtml(ID_TITLE,text);
            },
            getTitle: function (showMenuButton) {
                var prefix  = "";
                if(showMenuButton && this.hasEntries()) {
                    prefix = this.getEntryMenuButton(this.getEntries()[0])+" ";
                }
                var title = this.getProperty(ATTR_TITLE);
                if(title!=null) {
                    return prefix +title;
                }
                if(this.dataCollection == null) {
                    return prefix;
                }
                var dataList =  this.dataCollection.getList();
                title = "";
                for(var collectionIdx=0;collectionIdx<dataList.length;collectionIdx++) {             
                    var pointData = dataList[collectionIdx];
                    if(collectionIdx>0) title+="/";
                    title += pointData.getName();
                }

                return prefix+title;
            },
            getIsLayoutFixed: function() {
                return this.getProperty(PROP_LAYOUT_HERE,false);
            },
            doingQuickEntrySearch: false,
            doQuickEntrySearch: function(request, callback) {
                if(this.doingQuickEntrySearch) return;
                var text = request.term;
                if(text == null || text.length<=1) return;
                this.doingQuickEntrySearch = true;
                var searchSettings = new EntrySearchSettings({
                        name: text,
                        max: 10,
                    });
                if(this.searchSettings) {
                    searchSettings.clearAndAddType(this.searchSettings.entryType);
                }
                var theDisplay = this;
                var jsonUrl = this.getRamadda().getSearchUrl(searchSettings, OUTPUT_JSON);
                var handler = {
                    entryListChanged: function(entryList) {
                        theDisplay.doneQuickEntrySearch(entryList, callback);
                    }
                };
                var entryList =  new EntryList(this.getRamadda(), jsonUrl, handler, true);
            },
            doneQuickEntrySearch: function(entryList, callback) {
                var names = [];
                var entries = entryList.getEntries();
                for(var i=0;i<entries.length;i++) {
                    names.push(entries[i].getName());
                }
                callback(names);
                this.doingQuickEntrySearch = false;

            },
            addData: function(pointData) { 
                var records = pointData.getRecords();
                if(records && records.length>0) {
                    this.hasElevation = records[0].hasElevation();
                } else {
                    this.hasElevation = false;
                }
                this.dataCollection.addData(pointData);
                var entry  =  pointData.entry;
                if(entry == null) {
                    entry  = this.getRamadda().getEntry(pointData.entryId);
                } 
                if(entry) {
                    pointData.entry = entry;
                    this.addEntry(entry);
                }
            },
            pointDataLoadFailed: function(data)  {
                this.inError = true;
                errorMessage =  this.getProperty("errorMessage",null);
                if(errorMessage!=null) {
                    this.setContents(errorMessage);
                    return;
                }
                var msg = "";
                if(data && data.errorcode && data.errorcode=="warning") {
                    msg =  data.error;
                } else {
                    msg = "<b>Sorry, but an error has occurred:</b>";
                    if(!data) data = "No data returned from server";
                    msg +=  HtmlUtil.tag("div", ["style","background:#fff;margin:10px;padding:10px;border:1px #ccc solid;   border-radius: 0px;"],
                                         (data.error?data.error:data)); 
                }
                this.setContents(this.getMessage(msg));
            },
            //callback from the pointData.loadData call
            pointDataLoaded: function(pointData, url, reload)  {
                if(!reload) {
                    this.addData(pointData);
                }
                if(url!=null) {
                    this.jsonUrl = url;
                } else {
                    this.jsonUrl = null;
                }
                if(!this.getDisplayReady()) {
                    return;
                }
                this.updateUI();
                if(!reload) {
                    this.lastPointData = pointData;
                    this.getDisplayManager().handleEventPointDataLoaded(this, pointData);
                }
            },

            getDateFormatter: function() {
                var date_formatter = null;
                if (this.googleLoaded()) {
                    var df = this.getProperty("dateFormat",null);
                    if (df) {
                        var tz = 0;
                        this.timezone = this.getProperty("timezone");
                        if(Utils.isDefined(this.timezone)) {
                            tz=parseFloat(this.timezone);
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
                for(j=0;j<records.length;j++) { 
                    var record = records[j];
                    var date = record.getDate();
                    if(date==null) {
                        continue;
                    }
                    if(lastDate!=null && lastDate.getTime()!=date.getTime()) {
                        this.hasDate = true;
                        break
                    }
                    lastDate = date;
                }
                return this.hasDate;
            },
            dateInRange: function(date) {
                if(date!=null) {
                    if(this.minDateObj!=null && date.getTime()< this.minDateObj.getTime()) {
                        return false;
                    }
                    if(this.maxDateObj!=null && date.getTime()> this.maxDateObj.getTime()) {
                        return false;
                    }
                }
                return true;
            },

            getPointData : function() {
                if(this.dataCollection.getList().length == 0) return null;
                return  this.dataCollection.getList()[0];
            },
            //get an array of arrays of data 
                getDataValues : function(obj) {
                if(obj.tuple) return obj.tuple;
                else if(obj.getData) return obj.getData();
                return obj;
            },
            makeDataArray : function(dataList) {
                if(dataList.length==0) return dataList;
                var data = [];
                if(dataList[0].getData) {
                    for(var i=0;i<dataList.length;i++) 
                        data.push(dataList[i].getData());
                } else if(dataList[0].tuple) {
                    for(var i=0;i<dataList.length;i++) 
                        data.push(dataList[i].tuple);
                } else {
                    data = dataList;
                }
                return data;
            },

            getStandardData : function(fields, args) {
                var pointData = this.getPointData();
                var excludeZero = this.getProperty(PROP_EXCLUDE_ZERO,false);
                if(fields == null) {
                    fields = pointData.getRecordFields();
                }

                props = {
                    makeObject:true,
                    includeIndex: true,
                    includeIndexIfDate: false,
                    groupByIndex:-1,
                    raw: false,
                };
                if(args!=null) {
                    $.extend(props, args);
                }



                var groupByIndex = props.groupByIndex;
                var groupByList = [];

                var dataList = [];
                //The first entry in the dataList is the array of names
                //The first field is the domain, e.g., time or index
                var fieldNames = [];
                for(i=0;i<fields.length;i++) { 
                    var field = fields[i];
                    if(field.isFieldNumeric() && field.isFieldDate()) {
                        //                        console.log("Skipping:" + field.getLabel());
                        //                        continue;
                    }
                    var name  = field.getLabel();
                    if(field.getUnit()!=null) {
                        name += " (" + field.getUnit()+")";
                    }
                    //                    name = name.replace(/!!/g,"<br><hr>&nbsp;&nbsp;&nbsp;")
                    name = name.replace(/!!/g," -- ")
                    fieldNames.push(name);
                }
                if(props.makeObject)  {
                    dataList.push({tuple:fieldNames, record:null});
                }  else  {
                    dataList.push(fieldNames);
                }
                //console.log(fieldNames);


                groupByList.push("");
                //These are Record objects 


                this.minDateObj = Utils.parseDate(this.minDate, false);
                this.maxDateObj  = Utils.parseDate(this.maxDate,true, this.minDateObj);

                if(this.minDateObj==null && this.maxDateObj!=null) {
                    this.minDateObj = Utils.parseDate(this.minDate, false, this.maxDateObj);
                }


                var minDate = (this.minDateObj!=null?this.minDateObj.getTime():-1);
                var maxDate = (this.maxDateObj!=null?this.maxDateObj.getTime():-1);
                if(this.minDateObj!=null || this.maxDateObj!=null) {
                    //                    console.log("dates: "  + this.minDateObj +" " + this.maxDateObj);
                }



                var offset = 0;
                if(Utils.isDefined(this.offset)) {
                    offset =  parseFloat(this.offset);
                }

                var nonNullRecords = 0;
                var indexField = this.indexField;
                var records = pointData.getRecords();
                var allFields = pointData.getRecordFields();

                //Check if there are dates and if they are different
                this.hasDate = this.getHasDate(records);
                var date_formatter = this.getDateFormatter();
                var rowCnt = -1;
                for(j=0;j<records.length;j++) { 
                    var record = records[j];
                    var date = record.getDate();
                    if(!this.dateInRange(date)) continue;
                    rowCnt++;
                    var values = [];
                    if(props && (props.includeIndex || props.includeIndexIfDate)) {
                        var indexName = null;
                        if(indexField>=0) {
                            var field = allFields[indexField];
                            values.push(record.getValue(indexField)+offset);
                            indexName =  field.getLabel();
                        } else {
                            if(this.hasDate) {
                                //                                console.log(this.getDateValue(date, date_formatter));
                                values.push(this.getDateValue(date, date_formatter));
                                indexName = "Date";
                            } else {
                                if(!props.includeIndexIfDate) {
                                    values.push(j);
                                    indexName = "Index";
                                }
                            }
                        }
                        if(indexName!=null && rowCnt == 0) {
                            fieldNames.unshift(indexName);
                        }
                    }

                    var allNull  = true;
                    var allZero  = true;
                    var hasNumber = false;
                    for(var i=0;i<fields.length;i++) { 
                        var field = fields[i];
                        if(field.isFieldNumeric() && field.isFieldDate()) {
                            //                            continue;
                        }
                        var value = record.getValue(field.getIndex());
                        //                        console.log(field.getId() +" = " + value);
                        if(offset!=0) {
                            value+=offset;
                        }
                        if(value!=null) {
                            allNull = false;
                        }
                        if (typeof value == 'number') {
                            hasNumber = true;
                            if(value !=0) {
                                allZero = false;
                            }
                        }
                        if(field.isFieldDate()) {
                            value = this.getDateValue(value,date_formatter);
                        }
                        values.push(value);
                    }

                    if(hasNumber && allZero && excludeZero) {
                        //                        console.log(" skipping due to zero: " + values);
                        continue;
                    }
                    if(this.filters!=null) {
                        if(!this.applyFilters(record, values)) {
                            console.log(" skipping due to filters");
                            continue;
                        }
                    }
                    //TODO: when its all null values we get some errors
                    if(groupByIndex>=0) {
                        groupByList.push(record.getValue(groupByIndex));
                    }
                    if(props.makeObject) 
                        dataList.push({tuple:values, record:record});
                    else
                        dataList.push(values);
                    //                    console.log("values:" + values);
                    if(!allNull) {
                        nonNullRecords++;
                    }
                }
                if(nonNullRecords==0) {
                    //                    console.log("Num non null:" + nonNullRecords);
                    return [];
                }


                if(groupByIndex>=0) {
                    //                    console.log("index:" +groupByIndex);
                    var groupToTuple  ={};
                    var groups  =[];
                    var agg = [];
                    var title = [];
                    title.push(props.groupByField.getLabel());
                    for(var j=0;j<fields.length;j++) {
                        var field = fields[j];
                        if(field.getIndex() == groupByIndex) {
                            continue;
                        }
                        title.push(field.getLabel());
                    }
                    agg.push(title);

                    for(var i=0;i< dataList.length;i++) {
                        var data = dataList[i];
                        if(i == 0) {
                            continue;
                        }
                        var groupBy = groupByList[i];
                        var debug = false;
                        var tuple = groupToTuple[groupBy];
                        if(tuple == null) {
                            groups.push(groupBy);
                            tuple = new Array();
                            agg.push(tuple);
                            tuple.push(groupBy);
                            //props.includeIndex?1:0
                            for(var j=0;j<data.length;j++) {
                                var field = fields[j];
                                if(field.getIndex() == groupByIndex) {
                                    continue;
                                }
                                tuple.push(0);
                            }
                            //                            console.log("new group:" + groupBy+" tuple:" + tuple);
                            groupToTuple[groupBy]= tuple;
                        } else {
                            //                            console.log("old group:" + groupBy+" tuple:" + tuple);
                        }
                        var index =0;
                        //                        console.log("data:" + data);
                        for(var j=0;j<data.length;j++) {
                            var field = fields[j];
                            if(field.getIndex() == groupByIndex) {
                                continue;
                            }
                            var dataValue = data[j];
                            index++;
                            //                            console.log("data value:" + dataValue);
                            if(Utils.isNumber(dataValue)) {
                                if(typeof tuple[index] == "string") {
                                    tuple[index] = 0;
                                }
                                tuple[index] += parseFloat(dataValue);
                            } else {
                                if(tuple[index] == 0) {
                                    tuple[index] = "";
                                }
                                var s = tuple[index];
                                if(!Utils.isDefined(s)) {
                                    s = "";
                                }
                                //Only concat string values for a bit
                                if(s.length<150) {
                                    if(!Utils.isDefined(dataValue)) {
                                        dataValue = "";
                                    }

                                    var sv =(""+dataValue);
                                    //                                    console.log("   sv:" + groupBy+" sv:" + sv);
                                    if(s.indexOf(sv)<0) {
                                        if(s!="") {
                                            s+=", ";
                                        }
                                        s+= sv;
                                        tuple[index]=s;
                                    }
                                }

                            }
                        }
                    }
                    for(var j=0;j<agg.length; j++) {
                        var row = agg[j];
                        var s = null;
                        for(var h=0;h<row.length; h++) {
                            if(s) s+=",";
                            s +=  row[h];
                        }
                        //                        console.log(s);
                    }
                   return agg;
                }

                return dataList;
            },
            googleLoaded: function() {
                if ( (typeof google === 'undefined') || (typeof google.visualization === 'undefined') || (typeof google.visualization.DateFormat === 'undefined')) {
                  return false;
               }
               return true;
            },
            initDateFormats: function() {
                if (!this.googleLoaded()) {
                    //                    console.log("google hasn't loaded");
                    return false;
                }
                if(this.fmt_yyyy) return true;
                var tz = 0;
                this.timezone = this.getProperty("timezone");
                if(Utils.isDefined(this.timezone)) {
                    tz=parseFloat(this.timezone);
                }
                this.fmt_yyyymmddhhmm =   new google.visualization.DateFormat({pattern: "yyyy-MM-dd HH:mm'Z'",timeZone:tz});
                this.fmt_yyyymmdd =   new google.visualization.DateFormat({pattern: "yyyy-MM-dd",timeZone:tz});
                this.fmt_yyyy =   new google.visualization.DateFormat({pattern: "yyyy",timeZone:tz});
                return true;
            },
            getDateValue: function(arg, formatter) {
                if (!this.initDateFormats()) {
                    return arg;
                }
                if(!(typeof arg=="object")) {
                   date = new Date(arg);
                } else {
                    date = arg;
                }
                if (!formatter) {
                   formatter = this.fmt_yyyymmddhhmm;
                }
                var s  = formatter.formatValue(date);
                date = {v:date,f:s};
                return date;
            },
            applyFilters: function(record, values) {
                for(var i=0;i<this.filters.length;i++) {
                    if(!this.filters[i].recordOk(this, record, values)) {
                        return false;
                    }
                }
                return true;
            }
        }
        );

        var filter = this.getProperty(PROP_DISPLAY_FILTER);
        if(filter!=null) {
            //semi-colon delimited list of filter definitions
            //display.filter="filtertype:params;filtertype:params;
            //display.filter="month:0-11;
            var filters = filter.split(";");
            for(var i=0;i<filters.length;i++) {
                filter = filters[i];
                var toks = filter.split(":");
                var type  = toks[0];
                if(type == "month") {
                    this.filters.push(new MonthFilter(toks[1]));
                } else {
                    console.log("unknown filter:" + type);
                }
            }
        }
}



function DisplayGroup(argDisplayManager, argId, argProperties) {
    var LAYOUT_TABLE = TAG_TABLE;
    var LAYOUT_TABS = "tabs";
    var LAYOUT_COLUMNS = "columns";
    var LAYOUT_ROWS = "rows";

    var SUPER;
    RamaddaUtil.inherit(this, SUPER = new RamaddaDisplay(argDisplayManager, argId, "group", argProperties));

    RamaddaUtil.defineMembers(this, {
            layout:this.getProperty(PROP_LAYOUT_TYPE, LAYOUT_TABLE)});


    RamaddaUtil.defineMembers(this, {
            displays : [],
            layout:this.getProperty(PROP_LAYOUT_TYPE, LAYOUT_TABLE),
            columns:this.getProperty(PROP_LAYOUT_COLUMNS, 1),
            isLayoutColumns: function() {
                return this.layout == LAYOUT_COLUMNS;
            },
            getDoBs: function() {
                return this.dobs;
            },
            getWikiText: function() {
                var attrs = ["layoutType",this.layout,
                             "layoutColumns",
                             this.columns,
                             "showMenu",
                             "false",
                             "divid",
                             "$entryid_maindiv" ];
                var wiki = "";
                wiki += "<div id=\"{{entryid}}_maindiv\"></div>\n\n";
                wiki +="{{group " +  HtmlUtil.attrs(attrs) + "}}\n\n"
                return wiki;
            },

            walkTree: function(func, data) {
                for(var i=0;i<this.displays.length;i++) {
                    var display  = this.displays[i];
                    if(display.walkTree!=null) {
                        display.walkTree(func, data);
                    } else {
                        func.call(data, display);
                    }
                }
            }, 
            collectEntries: function(entries) {
                if(entries == null) entries = [];
                for(var i=0;i<this.displays.length;i++) {
                    var display  = this.displays[i];
                    if(display.collectEntries!=null) {
                        display.collectEntries(entries);
                    } else {
                        var displayEntries = display.getEntries();
                        if(displayEntries!=null && displayEntries.length>0) {
                            entries.push({source: display, entries: displayEntries});
                        }
                    }
                }
                return entries;
            },
            isLayoutRows: function() {
                return this.layout == LAYOUT_ROWS;
            },

            getPosition:function() {
                for(var i=0;i<this.displays.length;i++) {
                    var display  = this.displays[i];
                    if(display.getPosition) {
                        return display.getPosition();
                    }
                }
            },
            getDisplays:function() {
                return this.display;
            },
            notifyEvent:function(func, source, data) {
               var displays  = this.getDisplays();
               for(var i=0;i<this.displays.length;i++) {
                   var display = this.displays[i];
                   if(display == source) {
                       continue;
                   }
                   var eventSource  = display.getEventSource();
                   if(eventSource!=null && eventSource.length>0) {
                       if(eventSource!= source.getId() && eventSource!= source.getName()) {
                           continue;
                        }
                   }
                   display.notifyEvent(func, source, data);
               }
            }, 
            getDisplaysToLayout:function() {
                var result = [];
                for(var i=0;i<this.displays.length;i++) {
                    if(this.displays[i].getIsLayoutFixed()) continue;
                    result.push(this.displays[i]);
                }
                return result;
            },
            pageHasLoaded: function(display) {
                for(var i=0;i<this.displays.length;i++) {
                    this.displays[i].setDisplayReady(true);
                }
                this.doLayout();
            },
            addDisplay: function(display) {
                this.displays.push(display);
                if(Utils.getPageLoaded()) {
                    this.doLayout();
                }
            },
           layoutChanged: function(display) {
               this.doLayout();
           },
           removeDisplay:function(display) {
                var index = this.displays.indexOf(display);
                if(index >= 0) { 
                    this.displays.splice(index, 1);
                }   
                this.doLayout();
            },
            doLayout:function() {
                var html = "";
                var colCnt=100;
                var displaysToLayout = this.getDisplaysToLayout();
                var displaysToPrepare = this.displays;

                for(var i=0;i<displaysToPrepare.length;i++) {
                    var display = displaysToPrepare[i];
                    if(display.prepareToLayout!=null) {
                        display.prepareToLayout();
                    }
                }

                var weightIdx = 0;
                var weights = null;
                if(typeof  this.weights  != "undefined") {
                    weights = this.weights.split(",");
                }

                if(this.layout == LAYOUT_TABLE) {
                    if(displaysToLayout.length == 1) {
                        html+=  HtmlUtil.div(["class"," display-wrapper"], 
                                             displaysToLayout[0].getHtml());
                    } else {
                        var weight = 12 / this.columns;
                        var  i =0;
                        var map = {};
                        for(;i<displaysToLayout.length;i++) {
                            var d = displaysToLayout[i];
                            if(Utils.isDefined(d.column) && Utils.isDefined(d.row) && d.columns>=0 && d.row>=0) {
                                var key = d.column+"_" + d.row;
                                if(map[key] == null) map[key] = [];
                                map[key].push(d);
                            }
                        }



                        i = 0;
                        for(;i<displaysToLayout.length;i++) {
                            colCnt++;
                            if(colCnt>=this.columns) {
                                if(i>0) {
                                    html+= HtmlUtil.closeTag(TAG_DIV);
                                }
                                html += HtmlUtil.openTag("div",["class","row"]);
                                colCnt=0;
                            }
                            var weightToUse = weight;
                            if(weights!=null) {
                                if(weightIdx >=weights.length) {
                                    weightIdx = 0;
                                }
                                weightToUse = weights[weightIdx];
                                weightIdx ++;
                            }
                            html+= HtmlUtil.div(["class","col-md-" + weightToUse +" display-wrapper display-cell"],  displaysToLayout[i].getHtml());
                        }

                        if(i>0) {
                            html+= HtmlUtil.closeTag(TAG_DIV);
                        }
                    }
                    //                    console.log("HTML " + html);
                } else if(this.layout==LAYOUT_TABS) {
                    var tabId =  HtmlUtil.getUniqueId("tabs_");
                    html += HtmlUtil.openTag(TAG_DIV,["id", tabId, "class","ui-tabs"]);
                    html += HtmlUtil.openTag(TAG_UL,[]);
                    var hidden = "";
                    var cnt = 0;
                    for(var i=0;i<displaysToLayout.length;i++) {
                        var display =displaysToLayout[i];
                        var label = display.getTitle(false);
                        if(label.length >20) {
                            label = label.substring(0,19) +"...";
                        }
                        html += HtmlUtil.tag(TAG_LI, [], HtmlUtil.tag(TAG_A, ["href","#"  + tabId + "-" + cnt],label));
                        hidden += HtmlUtil.div(["id", tabId  + "-" + cnt,"class","ui-tabs-hide"], display.getHtml());
                        cnt++;
                    }
                    html += HtmlUtil.closeTag(TAG_UL);
                    html += hidden;
                    html += HtmlUtil.closeTag(TAG_DIV);
                } else if(this.layout==LAYOUT_ROWS) {
                    var rows = [];
                    for(var i=0;i<displaysToLayout.length;i++) {
                        var display =displaysToLayout[i];
                        var row = display.getRow();
                        if((""+row).length==0) row = 0;
                        while(rows.length<=row) {
                            rows.push([]);
                        }
                        rows[row].push(display.getHtml());
                    }
                    for(var i=0;i<rows.length;i++) {
                        var cols = rows[i];
                        var width = Math.round(100/cols.length)+"%";
                        html+=HtmlUtil.openTag(TAG_TABLE, ["border","0","width", "100%", "cellpadding", "0",  "cellspacing", "0"]);
                        html+=HtmlUtil.openTag(TAG_TR, ["valign","top"]);
                        for(var col=0;col<cols.length;col++) {
                            var cell = cols[col];
                            cell  = HtmlUtil.div(["class","display-cell"], cell);
                            html+=HtmlUtil.tag(TAG_TD,["width", width], cell);
                        }
                        html+= HtmlUtil.closeTag(TAG_TR);
                        html+= HtmlUtil.closeTag(TAG_TABLE);
                    }
                } else if(this.layout==LAYOUT_COLUMNS) {
                    var cols = [];
                    for(var i=0;i<displaysToLayout.length;i++) {
                        var display =displaysToLayout[i];
                        var column = display.getColumn();
                        //                        console.log("COL:" + column);
                        if((""+column).length==0) column = 0;
                        while(cols.length<=column) {
                            cols.push([]);
                        }
                        cols[column].push(display.getHtml());
                        //                        cols[column].push("HTML");
                    }
                    html+=HtmlUtil.openTag(TAG_DIV, ["class","row"]);
                    var width = Math.round(100/cols.length)+"%";
                    var weight = 12 / cols.length;
                    for(var i=0;i<cols.length;i++) {
                        var rows = cols[i];
                        var contents = "";
                        for(var j=0;j<rows.length;j++) {
                            contents+= rows[j];
                        }
                        var weightToUse = weight;
                        if(weights!=null) {
                            if(weightIdx >=weights.length) {
                                weightIdx = 0;
                            }
                            weightToUse = weights[weightIdx];
                            weightIdx ++;
                        }
                        html+=HtmlUtil.div(["class","col-md-" + weightToUse], contents);
                    }
                    html+= HtmlUtil.closeTag(TAG_DIV);
                    //                    console.log("HTML:" + html);

                } else {
                    html+="Unknown layout:" + this.layout;
                }
                this.writeHtml(ID_DISPLAYS, html);

                if(this.layout==LAYOUT_TABS) {
                    $("#"+ tabId).tabs({});
                }
                this.initDisplays();
            },
            initDisplays: function() {
                for(var i=0;i<this.displays.length;i++) {
                    try {
                        this.displays[i].initDisplay();
                    } catch(e) {
                        this.displays[i].displayError("Error creating display:" + e);
                        console.log("error creating display:" + this.displays[i].getType());
                        console.log(e.stack)
                    }
                }
            },
            displayData: function() {
            },
            setLayout:function(layout, columns) {
                this.layout  = layout;
                if(columns) {
                    this.columns  = columns;
                }
                this.doLayout();
            },
            askMinZAxis: function() {
                var v = prompt("Minimum axis value", "0");
                if(v!=null) {
                    v =  parseFloat(v);
                    for(var i=0;i<this.displays.length;i++) {
                        var display  = this.displays[i];
                        if(display.setMinZAxis) {
                            display.setMinZAxis(v);
                        }
                    }
                }
            },

            askMaxZAxis: function() {
                var v = prompt("Maximum axis value", "0");
                if(v!=null) {
                    v =  parseFloat(v);
                    for(var i=0;i<this.displays.length;i++) {
                        var display  = this.displays[i];
                        if(display.setMaxZAxis) {
                            display.setMaxZAxis(v);
                        }
                    }
                }
            },

            askMinDate: function() {
                var d = this.minDate;
                if(!d) d = "1950-0-0";
                this.minDate = prompt("Minimum date", d);
                if(this.minDate!=null) {
                    for(var i=0;i<this.displays.length;i++) {
                        var display  = this.displays[i];
                        if(display.setMinDate) {
                            display.setMinDate(this.minDate);
                        }
                    }
                }
            },

            askMaxDate: function() {
                var d = this.maxDate;
                if(!d) d = "2020-0-0";
                this.maxDate = prompt("Maximum date", d);
                if(this.maxDate!=null) {
                    for(var i=0;i<this.displays.length;i++) {
                        var display  = this.displays[i];
                        if(display.setMaxDate) {
                            display.setMaxDate(this.maxDate);
                        }
                    }
                }
            },


            titlesOff: function() {
                for(var i=0;i<this.displays.length;i++) {
                    this.displays[i].setShowTitle(false);
                }
            },
            titlesOn: function() {
                for(var i=0;i<this.displays.length;i++) {
                    this.displays[i].setShowTitle(true);
                }
            },
            detailsOff: function() {
                for(var i=0;i<this.displays.length;i++) {
                    this.displays[i].setShowDetails(false);
                }
                this.doLayout();
            },
            detailsOn: function() {
                for(var i=0;i<this.displays.length;i++) {
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
                if(index <= 0) { 
                    return;
                }
                this.displays.splice(index, 1);
                this.displays.splice(index-1, 0,display);
                this.doLayout();
            },
            moveDisplayDown: function(display) {
                var index = this.displays.indexOf(display);
                if(index >=this.displays.length) { 
                    return;
                }
                this.displays.splice(index, 1);
                this.displays.splice(index+1, 0,display);
                this.doLayout();
           },

            moveDisplayTop: function(display) {
                var index = this.displays.indexOf(display);
                if(index >=this.displays.length) { 
                    return;
                }
                this.displays.splice(index, 1);
                this.displays.splice(0, 0,display);
                this.doLayout();
           },


        });

}




/**
Copyright 2008-2014 Geode Systems LLC
*/



/*
This package supports charting and mapping of georeferenced time series data
*/

var pointDataCache = {};


function DataCollection() {
    RamaddaUtil.defineMembers(this,{ 
            data: [],
            hasData: function() {
                for(var i=0;i<this.data.length;i++) {
                    if(this.data[i].hasData()) return true;
                }
                return false;
            },
            getList: function() {
                return this.data;
            },
            addData: function(data) {
                this.data.push(data);
            },
            handleEventMapClick: function (myDisplay, source, lon, lat) {
                for(var i=0;i<this.data.length;i++ ) {
                    this.data[i].handleEventMapClick(myDisplay, source, lon, lat);
                }

            },


});
    
}

function BasePointData(name, properties) {
    if(properties == null) properties = {};

    RamaddaUtil.defineMembers(this, {
            recordFields : null,
                records : null,
                entryId: null,
                entry: null});

    $.extend(this, properties);

    RamaddaUtil.defineMembers(this, {
            name : name,
            properties : properties,
            initWith : function(thatPointData) {
                this.recordFields = thatPointData.recordFields;
                this.records = thatPointData.records;
            },
            handleEventMapClick: function (myDisplay, source, lon, lat) {
            },
            hasData : function() {
                return this.records!=null;
            },
            clear: function() {
                this.records = null;
                this.recordFields = null;
            },
            getProperties : function() {
                return this.properties;
            },
            getProperty : function(key, dflt) {
                var value = this.properties[key];
                if(value == null) return dflt;
                return value;
            },


            getRecordFields : function() {
                return this.recordFields;
            },
            addRecordField : function(field) {
                this.recordFields.push(field);
            },
            getRecords : function() {
                return this.records;
            },
            getNumericFields : function() {
                var recordFields = this.getRecordFields();
                var numericFields = [];
                for(var i=0;i<recordFields.length;i++) {
                    var field = recordFields[i];
                    if(field.isNumeric) numericFields.push(field);
                }
                return numericFields;
            },
            getChartableFields : function(display) {
                var recordFields = this.getRecordFields();
                var numericFields = [];
                var skip = /(TIME|HOUR|MINUTE|SECOND|YEAR|MONTH|DAY|LATITUDE|LONGITUDE|^ELEVATION$)/g;
                var skip = /(xxxnoskip)/g;
                for(var i=0;i<recordFields.length;i++) {
                    var field = recordFields[i];
                    if(!field.isNumeric || !field.isChartable()) {
                        continue;
                    }
                    var ID = field.getId().toUpperCase() ;
                    if(ID.match(skip)) {
                        continue;
                    }
                    numericFields.push(field);
                }

                return RecordUtil.sort(numericFields);
            },
            getNonGeoFields : function(display) {
                var recordFields = this.getRecordFields();
                var numericFields = [];
                //                var skip = /(TIME|HOUR|MINUTE|SECOND|YEAR|MONTH|DAY|LATITUDE|LONGITUDE|ELEVATION)/g;
                for(var i=0;i<recordFields.length;i++) {
                    var field = recordFields[i];
                    if(field.isFieldGeo()) {
                        continue;
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

            loadData: function(display) {
            },
            getName : function() {
                return this.name;
            },
            getTitle : function() {
                if(this.records !=null && this.records.length>0)
                    return this.name +" - " + this.records.length +" points";
                return this.name;
            }
        });
}




/*
This encapsulates some instance of point data. 
name - the name of this data
recordFields - array of RecordField objects that define the metadata
data - array of Record objects holding the data
*/
function PointData(name, recordFields, records, url, properties) {
    RamaddaUtil.inherit(this, new  BasePointData(name, properties));
    RamaddaUtil.defineMembers(this, {
            recordFields : recordFields,
            records : records,
            url : url,
            loadingCnt: 0,
            equals : function(that) {
                return this.url == that.url;
            },
            getIsLoading: function() {
                return this.loadingCnt>0;
            },
            handleEventMapClick: function (myDisplay, source, lon, lat) {
                this.lon = lon;
                this.lat = lat;
                if(myDisplay.getDisplayManager().hasGeoMacro(this.url)) {
                    this.loadData(myDisplay, true);
                }
            },
            startLoading: function() {
                this.loadingCnt++;
            },
            stopLoading: function() {
                this.loadingCnt--;
            },
            loadData: function(display, reload) {
                if(this.url==null) {
                    console.log("No URL");
                    return;
                }
                var props = {
                    lat:this.lat,                    
                    lon:this.lon,
                };
                var jsonUrl = display.displayManager.getJsonUrl(this.url, display, props);
                this.loadPointJson(jsonUrl, display, reload);
            },
            loadPointJson: function(url, display, reload) {
                var pointData = this;
                this.startLoading();
                var _this = this;
                var obj = pointDataCache[url];
                if(obj == null) {
                    obj =     {
                        pointData: null,
                        pending: []
                    };
                    //                    console.log("created new obj in cache: " +url);
                    pointDataCache[url] = obj;
                } 
                if(obj.pointData!=null) {
                    //                    console.log("from cache " +url);
                    display.pointDataLoaded(obj.pointData, url, reload);
                    return;
                }
                obj.pending.push(display);
                if(obj.pending.length>1) {
                    //                    console.log("Waiting on callback:" + obj.pending.length +" " + url);
                    return;
                }
                console.log("load data:" + url);
                //                console.log("loading point url:" + url);
                var jqxhr = $.getJSON( url, function(data) {
                        if(GuiUtils.isJsonError(data)) {
                            display.pointDataLoadFailed(data);
                            return;
                        }
                        var newData =    makePointData(data, _this.derived, display);
                        obj.pointData  = pointData.initWith(newData);
                        var tmp = obj.pending;
                        obj.pending = [];
                        for(var i=0;i<tmp.length;i++) {
                            //                            console.log("Calling: " + tmp[i]);
                            tmp[i].pointDataLoaded(pointData, url, reload);
                        }
                        pointData.stopLoading();
                    })
                    .fail(function(jqxhr, textStatus, error) {
                            var err = textStatus + ": " + error;
                            console.log("JSON error:" +err);
                            display.pointDataLoadFailed(err);
                            pointData.stopLoading();
                        });
            }

        });
}


function DerivedPointData(displayManager, name, pointDataList, operation) {
    RamaddaUtil.inherit(this, new  BasePointData(name));
    RamaddaUtil.defineMembers(this, {
            displayManager: displayManager,
            operation: operation,
            pointDataList: pointDataList,
            loadDataCalls: 0,
            display: null,
            pointDataLoaded: function(pointData) {
                this.loadDataCalls--;
                if(this.loadDataCalls<=0) {
                    this.initData();
                }
            },
            equals : function(that) {
                if(that.pointDataList == null) return false;
                if(this.pointDataList.length!=that.pointDataList.length) return false;
                for(var i in this.pointDataList) {
                    if(!this.pointDataList[i].equals(that.pointDataList[i])) {
                        return false;
                    }
                }
                return true;
            },
            initData: function() {
                var pointData1 = this.pointDataList[0];
                if(this.pointDataList.length == 1) {
                    this.records = pointData1.getRecords();
                    this.recordFields = pointData1.getRecordFields();
                } else if(this.pointDataList.length > 1) {
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

                if(records1.length!=records2.length) {
                    console.log("bad records:" + records1.length +" " + records2.length);
                }

                if(this.operation == "average") {
                    for(var recordIdx=0;recordIdx<records1.length;recordIdx++) {
                        var record1 = records1[recordIdx];
                        var record2 = records2[recordIdx];
                        if(record1.getDate()!=record2.getDate()) {
                            console.log("Bad record date:" + record1.getDate() + " " + record2.getDate());
                            break;
                        }
                        var newRecord = $.extend(true, {}, record1);
                        var data1 = newRecord.getData();
                        var data2 = record2.getData();
                        for(var colIdx=0;colIdx<data1.length;colIdx++) {
                            data1[colIdx]= (data1[colIdx]+data2[colIdx])/2;
                        }
                        newRecords.push(newRecord);
                    }
                    newRecordFields = pointData1.getRecordFields();
                } else  if(this.operation == "other func") {
                }
                if(newRecordFields==null) {
                    //for now just use the first operand
                    newRecords = records1;
                    newRecordFields = pointData1.getRecordFields();
                }
                return {records: newRecords,
                        recordFields: newRecordFields};
            },
            loadData: function(display) {
                this.display = display;
                this.loadDataCalls=0;
                for(var i in this.pointDataList) {
                    var pointData = this.pointDataList[i];
                    if(!pointData.hasData()) {
                        this.loadDataCalls++;
                        pointData.loadData(this);
                    }
                    if(this.loadDataCalls==0) {
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
            isDate:props.type == "date",
            isLatitude:false,
            isLongitude:false,
            isElevation:false,
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
             getLabel: function() { 
                if(this.label == null || this.label.length==0) return this.id;
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
    RamaddaUtil.defineMembers(this, {
            latitude : lat,
            longitude : lon,
            elevation : elevation,
            recordTime : time,
            data : data,
            getData : function() {
                return this.data;
            }, 
            allZeros: function() {
                var tuple = this.getData();
                var allZeros = false;
                var nums  = 0;
                var nonZero = 0;
                for(var j=0;j<tuple.length;j++) {
                    if(typeof tuple[j] == "number") {
                        nums++;
                        if(!isNaN(tuple[j]) && tuple[j] != 0) {
                            nonZero ++;
                            break;
                        }
                    }
                }
                if(nums>0 && nonZero == 0) {
                    return true;
                }
                return false;
            },
            getValue : function(index) {
                return this.data[index];
            }, 
            push: function(v) {
                this.data.push(v);
            },
            hasLocation : function() {
                return ! isNaN(this.latitude);
            }, 
            hasElevation : function() {
                return ! isNaN(this.elevation);
            }, 
            getLatitude : function() {
                return this.latitude;
            }, 
            getLongitude : function() {
                return this.longitude;
            }, 
            getTime : function() {
            	return this.time;
            },
            getElevation : function() {
                return this.elevation;
            }, 
            getDate : function() {
                return this.recordTime;
            }
        });
}



function makePointData(json, derived,source) {

    var fields = [];
    var latitudeIdx = -1;
    var longitudeIdx = -1;
    var elevationIdx = -1;
    var dateIdx = -1;
    var dateIndexes= [];

    var lastField = null;
    for(var i=0;i<json.fields.length;i++) {
        var field  = json.fields[i];
        var recordField = new RecordField(field);
        lastField = recordField;
        fields.push(recordField);
        //        console.log("field:" + recordField.getId());
        if(recordField.isFieldLatitude()) {
            latitudeIdx = recordField.getIndex();
        } else if(recordField.isFieldLongitude()) {
            longitudeIdx = recordField.getIndex();
            //            console.log("Longitude idx:" + longitudeIdx);
        } else if(recordField.isFieldElevation()) {
            elevationIdx = recordField.getIndex();
            //            console.log("Elevation idx:" + elevationIdx);
        } else if(recordField.isFieldDate()) {
            dateIdx = recordField.getIndex();
            dateIndexes.push(dateIdx);
        }

    }


    if(!derived) {
        derived = [
                   //               {'name':'temp_f','label':'Temp F', 'columns':'temperature','function':'v1*9/5+32', 'isRow':true,'decimals':2,},
               //               {'name':'Avg. Temperature','function':'return A.average(5, c1);','columns':'temperature','isColumn',true},
               //               {'name':'max_temp_f','function':'return A.max(c1);','columns':'temp_f'},
               //               {'name':'min_temp_f','function':'return A.min(c1);','columns':'temp_f'},
               //               {'name':'mavg_temp_f_10','function':'return A.movingAverage(10, c1);','columns':'temp_f'},
               //               {'name':'mavg_temp_f_20','function':'return A.movingAverage(20, c1);','columns':'temp_f'},
               ]
        }


    if(derived) {
        var index = lastField.getIndex()+1;
        for(var dIdx=0;dIdx<derived.length;dIdx++) {
            var d = derived[dIdx];
            //            if(!d.isRow) continue;
            var label = d.label;
            if(!label) label = d.name;
            var recordField = new RecordField({
                        type:"double",
                            index: (index+dIdx),
                            chartable: true,
                            id: d.name,
                            label:label,
                });
            recordField.derived= d;
            fields.push(recordField);
        }
    }

    var pointRecords =[];
    var rows = [];

    for(var i=0;i<json.data.length;i++) {
        var tuple = json.data[i];
        var values = tuple.values;
        //lat,lon,alt,time,data values
        var date  = null;
        if ((typeof tuple.date === 'undefined')) {
            if(dateIdx>=0) {
                date = new Date(values[dateIdx]);
            }
        } else {
            if(tuple.date!=null && tuple.date!=0) {
                date = new Date(tuple.date);
            }
        }
        if ((typeof tuple.latitude === 'undefined')) {
            if(latitudeIdx>=0) 
                tuple.latitude = values[latitudeIdx];
            else
                tuple.latitude = NaN;
        }
        if ((typeof tuple.longitude === 'undefined')) {
            if(longitudeIdx>=0) 
                tuple.longitude = values[longitudeIdx];
            else 
                tuple.longitude = NaN;
        }

        if ((typeof tuple.elevation === 'undefined')) {
            if(elevationIdx>=0) 
                tuple.elevation = values[elevationIdx];
            else
                tuple.elevation = NaN;
        }

        for(var j=0;j<dateIndexes.length;j++) {
            values[dateIndexes[j]] = new Date(values[dateIndexes[j]]);
        }


        if(derived) {
            for(var dIdx=0;dIdx<derived.length;dIdx++) {
                var d = derived[dIdx];
                if(!d.isRow) {
                    continue;
                }
                if(!d.compiledFunction) {
                    var funcParams = [];
                    var params = (d.columns.indexOf(";")>=0?d.columns.split(";"):d.columns.split(","));
                    d.fieldsToUse =  [];
                    for(var i=0;i<params.length;i++) {
                        var param = params[i].trim();
                        funcParams.push("v" +(i+1));
                        var theField = null;
                        for(var fIdx=0;fIdx<fields.length;fIdx++) {
                            var f = fields[fIdx];
                            if(f.getId() == param) {
                                theField = f;
                                break;
                            }
                        }
                        d.fieldsToUse.push(theField);
                      
                    }
                    var code =  "";
                    for(var i=0;i<funcParams.length;i++) {
                        code += "var v" +  (i+1) +"=args[" + i +"];\n";
                    }
                    var tmp = d["function"];
                    if(tmp.indexOf("return")<0) tmp = "return " + tmp;
                    code += tmp +"\n";
                    d.compiledFunction = new Function("args",code);
                    //                    console.log("Func:" + d.compiledFunction);
                }
                //TODO: compile the function once and call it
                var args  = [];

                var anyNotNan  = false;
                for(var fIdx=0;fIdx<d.fieldsToUse.length;fIdx++) {
                    var f = d.fieldsToUse[fIdx];
                    var v = NaN;
                    if(f!=null)  {
                        v = values[f.getIndex()];
                        if(v == null) v = NaN;
                    } 
                    if(!isNaN(v)) {
                        anyNotNan = true;
                    } else {
                    }
                    args.push(v);
                } 
                //                console.log("anyNot:" + anyNotNan);
                //                console.log(args);
                try {
                    var result  = NaN;
                    if(anyNotNan) {
                        result = d.compiledFunction(args);
                        if(d.decimals>=0) {
                            result = result.toFixed(d.decimals);
                        }
                        result = parseFloat(result);
                    } else {
                        //                        console.log("NAN");
                    }
                    //                    console.log("in:" + result +" out: " + result);
                    values.push(result);
                } catch(e) {
                    console.log("Error evaluating function:" + d["function"] + "\n"+e);
                    values.push(NaN);            
                }
            }
        }
        rows.push(values);
        var record = new PointRecord(tuple.latitude, tuple.longitude,tuple.elevation, date, values);
        pointRecords.push(record);
    }



    for(var dIdx=0;dIdx<derived.length;dIdx++) {
        var d = derived[dIdx];
        if(!d.isColumn) continue;
        var f = d["function"];
        var funcParams = [];
        //TODO: allow for no columns and choose all
        var params = d.columns.split(",");
        for(var i=0;i<params.length;i++) {
            var index = -1;
            for(var fIdx=0;fIdx<fields.length;fIdx++) {
                var f = fields[fIdx];
                //                console.log("F:" + f.getId() +" " + f.getLabel() );
                if(f.getId() == params[i] ||f.getLabel() == params[i]) {
                    index = f.getIndex();
                    console.log("index:" + index +" f:" + f.getId());
                    break;
                }
            }
            if(index<0) {
                console.log("Could not find column index for field: " + params[i]);
                continue;
            }
            funcParams.push("c" +(i+1));
        }
        var columnData = RecordUtil.slice(rows, index);
        //        var newData = A.movingAverage(columnData,{step:100});
        var daFunk =  new Function(funcParams, d["function"]);
        console.log("daFunk - "  + daFunk);
        var newData = daFunk(columnData);
        console.log("got new:"+ newData + " " +(typeof newData));
        if((typeof newData) ==  "number") {
            for(var rowIdx=0;rowIdx<pointRecords.length;rowIdx++) {
                var record  = pointRecords[rowIdx];
                record.push(newData);
            }
        } else if(Utils.isDefined(newData.length)) {
            console.log("newData.length:" + newData.length +" records.length:" + pointRecords.length);
            for(var rowIdx=0;rowIdx<newData.length;rowIdx++) {
                var record  = pointRecords[rowIdx];
                if(!record) {
                    console.log("bad record: " + rowIdx);
                    record.push(NaN);
                } else {
                    //                    console.log("    date:" + record.getDate() +" v: " + newData[rowIdx]);
                    var v  = newData[rowIdx];
                    if(d.decimals>=0) {
                        v = parseFloat(v.toFixed(d.decimals));
                    }
                    record.push(v);
                }
            }
        }
    }

    if(source!=null) {
        for(var i=0;i<fields.length;i++) {
            var field  = fields[i];
            var prefix = "field." + field.getId()+".";
            if(Utils.isDefined(source[prefix+"unit"])) {
                field.setUnit(source[prefix+"unit"]);
            }
            if(Utils.isDefined(source[prefix+"label"])) {
                field.setLabel(source[prefix+"label"]);
            }
            if(Utils.isDefined(source[prefix+"scale"])  || Utils.isDefined(source[prefix+"offset1"]) || Utils.isDefined(source[prefix+"offset2"])) {
                var offset1 = Utils.isDefined(source[prefix+"offset1"])?parseFloat(source[prefix+"offset1"]):0;
                var offset2 = Utils.isDefined(source[prefix+"offset2"])?parseFloat(source[prefix+"offset2"]):0;
                var scale = Utils.isDefined(source[prefix+"scale"])?parseFloat(source[prefix+"scale"]):1;
                var index = field.getIndex();
                for(var rowIdx=0;rowIdx<pointRecords.length;rowIdx++) {
                    var record  = pointRecords[rowIdx];
                    var values=  record.getData();
                    var value =  values[index];
                    values[index]= (value+offset1)*scale+offset2;
                }
            } 

        } 
    }



    var name = json.name;
    if ((typeof name === 'undefined')) {
        name =  "Point Data";
    }

    pointRecords.sort(function(a,b) {
            if(a.getDate() && b.getDate()) {
                if(a.getDate().getTime()<b.getDate().getTime()) return -1;
                if(a.getDate().getTime()>b.getDate().getTime()) return 1;
                return 0;
            }
        }); 

    return new  PointData(name,  fields, pointRecords);
}






function makeTestPointData() {
    var json = {
        fields:
        [{index:0,
               id:"field1",
               label:"Field 1",
               type:"double",
               missing:"-999.0",
               unit:"m"},

        {index:1,
               id:"field2",
               label:"Field 2",
               type:"double",
               missing:"-999.0",
               unit:"c"},
            ],
        data: [
               [-64.77,-64.06,45, null,[8.0,1000]],
               [-65.77,-64.06,45, null,[9.0,500]],
               [-65.77,-64.06,45, null,[10.0,250]],
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
    if(properties == null) properties = {};
    RamaddaUtil.defineMembers(this, {
            properties: properties,
            recordOk:function(display, record, values) {
                return true;
            }
        });
}


function MonthFilter(param) {
    RamaddaUtil.inherit(this,new RecordFilter());
    RamaddaUtil.defineMembers(this,{
            months: param.split(","),
            recordOk: function(display, record, values) {
                for(i in this.months) {
                    var month = this.months[i];
                    var date = record.getDate();
                    if(date == null) return false;
                    if(date.getMonth == null) {
                        //console.log("bad date:" + date);
                        return false;
                    }
                    if(date.getMonth()==month) return true;
                }
                return false;
            }
        });
}


var A = {
    add : function(v1,v2) {
        if(isNaN(v1) || isNaN(v2))  return NaN;
        return v1+v2;
    },

    average : function(values) {
        var sum = 0;
        if(values.length==0) return 0;
        for(var i=0;i<values.length;i++) {
            sum += values[i];
        }
        return sum/values.length;
    },
    percentIncrease : function(values) {
        var percents = [];
        var sum = 0;
        if(values.length==0) return 0;
        var lastValue;
        for(var i=0;i<values.length;i++) {
            var v = values[i];
            var incr = NaN;
            if(i>0 && lastValue!=0) {
                incr = (v-lastValue)/lastValue;
            }
            lastValue = v;
            percents.push(incr*100);
        }
        return percents;
    },
    movingAverage : function(values,props) {
        if(!props) {
            props = {};
        }
        if(!props.step) props.step = 5;
        var newValues = [];
        console.log("STEP:" + props.step);
        for(var i=props.step;i<values.length;i++) {
            var total = 0;
            var cnt = 0;
            for(var j=i-props.step;j<i;j++) {
                if(values[j] == values[j]) {
                    total += values[j];
                    cnt++;
                }
            }
            var value = (cnt>0?total/cnt:NaN);
            if(newValues.length==0) {
                for(var extraIdx=0;extraIdx<props.step;extraIdx++) {
                    newValues.push(value);
                }
            }
            newValues.push(value);
        }

        console.log("MovingAverage: values:" + values.length +" new:" + newValues.length);
        return newValues;
    },
   expMovingAverage : function(values,props) {
        if(!props) {
            props = {};
        }
        if(!props.step) props.step = 5;
        var sma = A.movingAverage(values, props);
        var mult   = (2.0 / (props.step + 1) );
        var newValues = [];
        console.log("STEP:" + props.step);
        for(var i=props.step;i<values.length;i++) {
            var total = 0;
            var cnt = 0;
            for(var j=i-props.step;j<i;j++) {
                if(values[j] == values[j]) {
                    total += values[j];
                    cnt++;
                }
            }
            var value = (cnt>0?total/cnt:NaN);
            if(newValues.length==0) {
                for(var extraIdx=0;extraIdx<props.step;extraIdx++) {
                    newValues.push(value);
                }
            }
            newValues.push(value);
        }

        console.log("MovingAverage: values:" + values.length +" new:" + newValues.length);
        return newValues;
    },

    max : function(values) {
        var max = NaN;
        for(var i=0;i<values.length;i++) {
            if(i==0 || values[i]>max) {
                max = values[i];
            }
        }
        return max;
    },
    min : function(values) {
        var min = NaN;
        for(var i=0;i<values.length;i++) {
            if(i==0 || values[i]<min) {
                min = values[i];
            }
        }
        return min;
    },

}

var RecordUtil = {
    getRanges: function(fields,records) {
        var maxValues = [];
        var minValues = [];
        for(var i=0;i<fields.length;i++) {
            maxValues.push(NaN);
            minValues.push(NaN);
        }

        for(var row=0;row<records.length;row++) {
            for(var col=0;col<fields.length;col++) {
                var value  = records[row].getValue(col);
                if(isNaN(value)) continue;    
                maxValues[col] = (isNaN(maxValues[col])?value:Math.max(value, maxValues[col]));
                minValues[col] = (isNaN(minValues[col])?value:Math.min(value, minValues[col]));
            }
        }

        var ranges = [];
        for(var col=0;col<fields.length;col++) {
            ranges.push([minValues[col],maxValues[col]]);
        }
        return ranges;
    },



    getElevationRange: function(fields,records) {
        var maxValue =NaN;
        var minValue = NaN;

        for(var row=0;row<records.length;row++) {
            if(records[row].hasElevation()) { 
                var value = records[row].getElevation();
                maxValue = (isNaN(maxValue)?value:Math.max(value, maxValue));
                minValue = (isNaN(minValue)?value:Math.min(value, minValue));
            }
        }
        return [minValue,maxValue];
    },


    slice: function(records,index) {
        var values = [];
        for(var rowIdx=0;rowIdx<records.length;rowIdx++) {
            var row = records[rowIdx];
            if(row.getValue) {
                values.push(row.getValue(index));
            } else {
                values.push(row[index]);
            }
        }
        return values;
    },


    sort : function(fields) {
        fields = fields.slice(0);
        fields.sort(function(a,b){
                var s1 = a.getSortOrder();
                var s2 = b.getSortOrder();
                return s1<s2;
            });
        return fields;
    },
    getPoints: function (records, bounds) {
        var points =[];
        var north=NaN,west=NaN,south=NaN,east=NaN;
        if(records!=null) {
            for(j=0;j<records.length;j++) { 
                var record = records[j];
                if(!isNaN(record.getLatitude()) && !isNaN(record.getLongitude())) { 
                    if(j == 0) {
                        north  =  record.getLatitude();
                        south  = record.getLatitude();
                        west  =  record.getLongitude();
                        east  = record.getLongitude();
                    } else {
                        north  = Math.max(north, record.getLatitude());
                        south  = Math.min(south, record.getLatitude());
                        west  = Math.min(west, record.getLongitude());
                        east  = Math.max(east, record.getLongitude());
                    }
                    if(record.getLongitude()<-180 || record.getLatitude()>90) {
                        console.log("Bad index=" + j +" " + record.getLatitude() +" " + record.getLongitude());
                    }
                    points.push(new OpenLayers.Geometry.Point(record.getLongitude(),record.getLatitude()));
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
        if(records == null) return null;
        var closestRecord = null;
        var minDistance = 1000000000;
        var index = -1;
        for(j=0;j<records.length;j++) { 
            var record = records[j];
            if(isNaN(record.getLatitude())) { 
                continue;
            }
            var distance = Math.sqrt((lon-record.getLongitude())*(lon-record.getLongitude()) + (lat-record.getLatitude())*(lat-record.getLatitude()));
            if(distance<minDistance) {
                minDistance = distance;
                closestRecord = record;
                index = j;
            }
        }
        if(indexObj!=null) {
            indexObj.index = index;
        }
        return closestRecord;
    },
    clonePoints: function(points) {
        var result = [];
        for(var i=0;i<points.length;i++) {
            var point = points[i];
            result.push(new OpenLayers.Geometry.Point(point.x,point.y));
        }
        return result;
    }
};

 /**
Copyright 2008-2015 Geode Systems LLC
*/


var DISPLAY_FILTER = "filter";
var DISPLAY_ANIMATION = "animation";
var DISPLAY_LABEL = "label";
var DISPLAY_SHELL = "shell";

addGlobalDisplayType({type:DISPLAY_FILTER , label: "Filter",requiresData:false,category:"Controls"});
addGlobalDisplayType({type:DISPLAY_ANIMATION , label: "Animation",requiresData:false,category:"Controls"});
addGlobalDisplayType({type:DISPLAY_LABEL , label: "Text",requiresData:false,category:"Misc"});

addGlobalDisplayType({type:DISPLAY_SHELL , label: "Analysis Shell",requiresData:false,category:"Misc"});


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
            iconStart: ramaddaBaseUrl+"/icons/display/control.png",
            iconStop: ramaddaBaseUrl+"/icons/display/control-stop-square.png",
            iconBack: ramaddaBaseUrl+"/icons/display/control-stop-180.png",
            iconForward: ramaddaBaseUrl+"/icons/display/control-stop.png",
            iconFaster: ramaddaBaseUrl+"/icons/display/plus.png",
            iconSlower: ramaddaBaseUrl+"/icons/display/minus.png",
            iconBegin: ramaddaBaseUrl+"/icons/display/control-double-180.png",
            iconEnd: ramaddaBaseUrl+"/icons/display/control-double.png",
            deltaIndex: function(i) {
                this.stop();
                this.setIndex(this.index+i);
            }, 
            setIndex: function(i) {
                if(i<0) i=0;
                this.index = i;
                this.applyStep(true, !Utils.isDefined(i));
            },
            toggle: function() {
                if(this.running) {
                    this.stop();
                } else {
                    this.start();
                }
            },
            tick: function() {
                if(!this.running) return;
                this.index++;
                this.applyStep();
                var theAnimation = this;
                setTimeout(function() {theAnimation.tick();}, this.sleepTime);
            },
             applyStep: function(propagate, goToEnd) {
                if(!Utils.isDefined(propagate)) propagate = true;
                var data = this.displayManager.getDefaultData();
                if(data == null) return;
                var records = data.getRecords();
                if(records == null) {
                    $("#" + this.getDomId(ID_TIME)).html("no records");
                    return;
                }
                if(goToEnd) this.index = records.length-1;
                if(this.index>=records.length) {
                    this.index = 0;
                }
                var record = records[this.index];
                var label = "";
                if(record.getDate()!=null) {
                    var dttm = this.formatDate(record.getDate(),{suffix:this.getTimeZone()});
                    label += HtmlUtil.b("Date:") + " "  + dttm;
                } else {
                    label += HtmlUtil.b("Index:") +" " + this.index;
                }
                $("#" + this.getDomId(ID_TIME)).html(label);
                if(propagate) {
                    this.displayManager.propagateEventRecordSelection(this, data, {index:this.index});
                }
            },
            handleEventRecordSelection: function(source, args) {
                var data = this.displayManager.getDefaultData();
                if(data == null) return;
                if(data != args.data) {
                    return;
                }
                if(!data) return;
                this.index = args.index;
                this.applyStep(false);
            },
            faster: function() {
                this.sleepTime = this.sleepTime/2;
                if(this.sleepTime==0) this.sleepTime  = 100;
            },
            slower: function() {
                this.sleepTime = this.sleepTime*1.5;
            },
            start: function() {
                if(this.running) return;
                this.running = true;
                this.timestamp++;
                $("#"+this.getDomId(ID_START)).attr("src",this.iconStop);
                this.tick();
            },
            stop: function() {
                if(!this.running) return;
                this.running = false;
                this.timestamp++;
                $("#"+this.getDomId(ID_START)).attr("src",this.iconStart);
            },
            initDisplay: function() {
                this.createUI();
                this.stop();

                var get = this.getGet();
                var html =  "";
                html+=  HtmlUtil.onClick(get +".setIndex(0);", HtmlUtil.image(this.iconBegin,[ATTR_TITLE,"beginning", ATTR_CLASS, "display-animation-button", "xwidth","32"]));
                html+=  HtmlUtil.onClick(get +".deltaIndex(-1);", HtmlUtil.image(this.iconBack,[ATTR_TITLE,"back 1", ATTR_CLASS, "display-animation-button", "xwidth","32"]));
                html+=  HtmlUtil.onClick(get +".toggle();", HtmlUtil.image(this.iconStart,[ATTR_TITLE,"play/stop", ATTR_CLASS, "display-animation-button", "xwidth","32", ATTR_ID, this.getDomId(ID_START)]));
                html+=  HtmlUtil.onClick(get +".deltaIndex(1);", HtmlUtil.image(this.iconForward,[ATTR_TITLE,"forward 1", ATTR_CLASS, "display-animation-button", "xwidth","32"]));
                html+=  HtmlUtil.onClick(get +".setIndex();", HtmlUtil.image(this.iconEnd,[ATTR_TITLE,"end", ATTR_CLASS, "display-animation-button", "xwidth","32"]));
                html+=  HtmlUtil.onClick(get +".faster();", HtmlUtil.image(this.iconFaster,[ATTR_CLASS, "display-animation-button", ATTR_TITLE,"faster", "xwidth","32"]));
                html+=  HtmlUtil.onClick(get +".slower();", HtmlUtil.image(this.iconSlower,[ATTR_CLASS, "display-animation-button", ATTR_TITLE,"slower", "xwidth","32"]));
                html+=  HtmlUtil.div(["style","display:inline-block; min-height:24px; margin-left:10px;",ATTR_ID, this.getDomId(ID_TIME)],"&nbsp;");
                this.setDisplayTitle("Animation");
                this.setContents(html);
            },
        });
}

function RamaddaLabelDisplay(displayManager, id, properties) {
    var ID_TEXT = "text";
    var ID_EDIT = "edit";
    var SUPER; 
    if(properties && !Utils.isDefined(properties.showTitle)) {
        properties.showTitle = false;
    }

    RamaddaUtil.inherit(this, SUPER = new RamaddaDisplay(displayManager, id, DISPLAY_LABEL, properties));
    addRamaddaDisplay(this);
    this.text = "";
    this.editMode = properties.editMode;

    if(properties.text) this.text = properties.text;
    else     if(properties.label) this.text = properties.label;
    else     if(properties.html) this.text = properties.html;
    else     if(properties.title) this.text = properties.title;
    if(properties["class"]) this["class"] = properties["class"];
    else this["class"]="display-text";

    RamaddaUtil.defineMembers(this, {
            initDisplay: function() {
                var theDisplay = this;
                this.createUI();
                var textClass = this["class"];
                if(this.editMode) {
                    textClass += " display-text-edit ";
                }
                var html  = HtmlUtil.div([ATTR_CLASS,textClass,ATTR_ID,this.getDomId(ID_TEXT)], this.text);
                if(this.editMode) {
                    html += HtmlUtil.textarea(ID_EDIT, this.text, ["rows",5,"cols",120, ATTR_SIZE, "120", ATTR_CLASS,"display-text-input",ATTR_ID,this.getDomId(ID_EDIT)]);
                }
                this.setContents(html);
                if(this.editMode) {
                    var editObj = this.jq(ID_EDIT);
                    editObj.blur(function() {
                            theDisplay.text = editObj.val();
                            editObj.hide();
                            theDisplay.initDisplay();
                        });
                this.jq(ID_TEXT).click(function() {
                        var src  = theDisplay.jq(ID_TEXT);
                        var edit  = theDisplay.jq(ID_EDIT);
                        edit.show();
                        edit.css('z-index','9999');
                        edit.position({
                                of: src,
                                    my: "left top",
                                    at: "left top",
                                    collision: "none none"
                                    });
                        theDisplay.jq(ID_TEXT).html("hello there");
                    });
                }


            },
            getWikiAttributes: function(attrs) {
                SUPER.getWikiAttributes(attrs);
                attrs.push("text");
                attrs.push(this.text);
            },
        });
}



function RamaddaShellDisplay(displayManager, id, properties) {
    var ID_INPUT = "input";
    var ID_OUTPUT = "output";
    var ID_MESSAGE = "message";
    var SUPER; 
    RamaddaUtil.inherit(this, SUPER = new RamaddaDisplay(displayManager, id, DISPLAY_SHELL, properties));
    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
            history:[],
            historyIndex: -1,
                rootEntry: null,
            currentEntry: null,
            currentEntries: [],
            currentOutput: null,
            currentInput: null,
            initDisplay: function() {
                var _this = this;
                this.createUI();
                var msg  = HtmlUtil.div([ATTR_CLASS,"display-shell-message",ATTR_ID,this.getDomId(ID_MESSAGE)], "");
                var output  = HtmlUtil.div([ATTR_CLASS,"display-shell-output",ATTR_ID,this.getDomId(ID_OUTPUT)], "");
                var input  = HtmlUtil.tag(TAG_INPUT, ["placeholder","Enter JS here", ATTR_CLASS,"display-shell-input",ATTR_ID,this.getDomId(ID_INPUT)]);
                var html  = msg + output + input;
                this.setContents(html);

                if(this.currentOutput) this.getOutput().html(this.currentOutput);
                if(this.currentInput)  this.getInput().val(this.currentInput);


                this.jq(ID_INPUT).keypress(function(e) {
                        if(e.which == 0) {
                            return;
                        }
                        
                        if(e.ctrlKey) {
                            _this.handleControlKey(e);
                            return;
                        }

                        //return
                        if(e.which != 13) {
                            return;
                        }
                        

                        if(e.preventDefault) {
                            e.preventDefault();
                        }
                        var input = _this.jq(ID_INPUT).val();
                        _this.processInput(input);

                    });
            },
            writeStatusMessage: function(v) {
                var msg  = this.jq(ID_MESSAGE);
                if(!v) {
                    msg.hide();
                    msg.html("");
                } else {
                    msg.show();
                    msg.position({
                            of:this.getOutput(),
                                    my: "left top",
                                    at: "left+4 top+4",
                                    collision: "none none"
                                    });
                    msg.html(v);
                }
            },
            clearInput: function() {
                this.jq(ID_INPUT).val("");
            },
            clearOutput: function() {
                this.writeStatusMessage(null);
                this.jq(ID_OUTPUT).html("");
            },
            handleControlKey: function(event) {
                var k = event.which;
                var h = this.history;
                if(h.length<=0) {
                    return;
                }
                var index = this.historyIndex;
                var nextIndex = -1;
                if(index <0 || index>= h.length) {
                    index = h.length;
                }


                if(k == 112) {
                    //P
                    nextIndex = index-1;
                } else if(k == 110) {
                    //N
                    nextIndex = index+1;
                }
                if(nextIndex>=0 && nextIndex< h.length) {
                    this.historyIndex = nextIndex;
                    this.writeInput(h[nextIndex]);
                }
            },
            writeInput: function(v) {
                var input = this.getInput();
                if(input.val() == v) {
                    return;
                }
                input.val(v).focus();
                this.currentInput  = this.getInput().val();
            },
            getOutput: function() {
                return this.jq(ID_OUTPUT);
            },
            getInput: function() {
                return this.jq(ID_INPUT);
            },
            writeResult: function(html) {
                this.writeStatusMessage(null);
                html = HtmlUtil.div([ATTR_CLASS,"display-shell-result"], html);
                var output = this.jq(ID_OUTPUT);
                output.append(html);
                output.animate({ scrollTop: output.prop("scrollHeight")}, 1000);
                this.currentOutput = output.html();
                this.currentInput  = this.getInput().val();

            },
            addHistory: function(cmd) {
                if(this.history.length>0 && this.history[this.history.length-1] == cmd) {
                    return;
                }
                this.history.push(cmd);
                this.historyIndex = -1;
            },
            writeError: function(msg) {
                this.writeStatusMessage(msg);
                //                this.writeResult(msg);
            },
            header: function(msg) {
                return HtmlUtil.div([ATTR_CLASS, "display-shell-header"],msg);
            },
            processCommand_help: function(line, toks, prefix) {
                var help = "";
                if(prefix!=null) help += prefix;
                help += this.header("RAMADDA Data Explorer Shell Help");
                help += "Navigation commands:<pre>pwd, ls, cd</pre>" ;
                help += "UI commands:<pre>history: ctrl-p, ctrl-n, !!  \nUI: clear, taller, shorter</pre>"
                this.writeResult(help);
            },
            uniqueCnt: 0,
            displayEntries: function(entries) {
                this.currentEntries = entries;
                var html = this.getEntriesTree(entries,{showIndex:true,suffix:"_shell_"+  (this.uniqueCnt++)});
                this.writeResult(html);
            },
            getEntryFromArgs: function(args, dflt) {
                var currentEntries = this.currentEntries;
                if(currentEntries==null) {
                    return dflt;
                }
                for(var i=0;i<args.length;i++) {
                    var arg = args[i];
                    if(arg.match("^\d+$")) {
                        var index = parseInt(arg);
                        break;
                    } 
                    if(arg == "-entry") {
                        i++;
                        var index = parseInt(args[i])-1;
                        if(index<0 || index>=currentEntries) {
                            this.writeError("Bad entry index:" + index +" should be between 1 and " + currentEntries.length);
                            return;
                        }
                        return currentEntries[index];
                    }
                }
                return dflt;
            },
            getCurrentEntry: function() {
                if(this.currentEntry == null) {
                    this.rootEntry =  new Entry({id:ramaddaBaseEntry,name:"Root",type:"group"});
                    this.currentEntry =  this.rootEntry;
                } 
                return this.currentEntry;
            },
            processCommand_pwd: function(line, toks) {
                var entry  =this.getCurrentEntry();
                var icon = entry.getIconImage([ATTR_TITLE,"View entry"]);
                var html = this.getEntriesTree([entry],{suffix:"_YYY"});
                //                var link  =  HtmlUtil.tag(TAG_A,[ATTR_HREF, entry.getEntryUrl()],icon+" "+ entry.getName());
                this.writeResult("Current entry:<br>" + html);
            },
            createPointDisplay: function(line, toks, displayType) {
                var entry  = this.getEntryFromArgs(toks, this.getCurrentEntry());
                console.log("createDisplay got:" + entry.getName());
                var jsonUrl = this.getPointUrl(entry);
                if(jsonUrl == null) {
                    this.writeError("Not a point type:" + entry.getName());
                    return;
                }
                this.createDisplay(entry.getId(), displayType,jsonUrl);
            },
            processCommand_table: function(line, toks) {
                this.createPointDisplay(line, toks, DISPLAY_TABLE);
            },
            processCommand_linechart: function(line, toks) {
                this.createPointDisplay(line, toks, DISPLAY_LINECHART);
            },
            processCommand_barchart: function(line, toks) {
                this.createPointDisplay(line, toks, DISPLAY_BARCHART);
            },
            processCommand_bartable: function(line, toks) {
                this.createPointDisplay(line, toks, DISPLAY_BARTABLE);
            },
            processCommand_hello: function(line, toks) {
                this.writeResult("Hello, how are you?");
            },
            processCommand_scatterplot: function(line, toks) {
                this.createPointDisplay(line, toks, DISPLAY_SCATTERPLOT)
            },
            processCommand_blog: function(line, toks) {
                this.getLayoutManager().publish('blogentry');
            },
            processCommand_cd: function(line, toks) {
                if(toks.length==0) {
                    this.currentEntry =  this.rootEntry;
                    this.processCommand_pwd("pwd",[]);
                    return;
                }
                var index = parseInt(toks[1])-1;
                if(index<0 || index>=this.currentEntries.length) {
                    this.writeError("Out of bounds: between 1 and " + this.currentEntries.length); 
                    return;
                }
                this.currentEntry  =this.currentEntries[index];
                this.processCommand_pwd("pwd",[]);
            },
            processCommand_ls: function(line, toks) {
                var _this = this;
                var callback = function(children) {
                    _this.displayEntries(children);
                };
                var children = this.getCurrentEntry().getChildrenEntries(callback, "");
                if(children !=null) {
                    this.displayEntries(children);
                } else {
                    this.writeStatusMessage("Listing entries...");
                }
            },
            entryListChanged: function(entryList) {
                var entries = entryList.getEntries();
                if(entries.length==0) {
                    this.writeStatusMessage("Sorry, nothing found");
                } else {
                    this.displayEntries(entries);
                }
            },
            processCommand_search: function(line, toks) {
                var text = "";
                for(var i=1;i<toks.length;i++) text+= toks[i] +" ";
                text = text.trim();
                var searchSettings =  new EntrySearchSettings({
                        text: text,
                    });
                var repository= this.getRamadda();
                this.writeStatusMessage("Searching...");
                var jsonUrl = repository.getSearchUrl(searchSettings, OUTPUT_JSON);
                this.entryList = new EntryList(repository, jsonUrl, this, true);
                //                this.writeResult(line);
            },
            processCommand_taller: function(line, toks) {
                if(!this.outputHeight) {
                    this.outputHeight  = 300;
                }
                this.outputHeight += parseInt(this.outputHeight*.30);
                this.jq(ID_OUTPUT).css('max-height',this.outputHeight);
                this.jq(ID_OUTPUT).css('height',this.outputHeight);
            },
            processCommand_shorter: function(line, toks) {
                if(!this.outputHeight) {
                    this.outputHeight  = 300;
                }
                this.outputHeight -= parseInt(this.outputHeight*.30);
                this.jq(ID_OUTPUT).css('max-height',this.outputHeight);
                this.jq(ID_OUTPUT).css('height',this.outputHeight);
            },

            processCommand_clear: function(toks) {
                this.clearOutput();
                this.clearInput();
            },
            processInput: function(value) {
                value = value.trim();
                if(value == "") {
                    return;
                }
                var input = this.jq(ID_INPUT);
                var output = this.jq(ID_OUTPUT);
                var commands   = value.split(";");
                for(var i=0;i<commands.length;i++ ) {
                    value = commands[i];
                    if(value == "!!") {
                        if(this.history.length==0) {
                            this.writeError("No commands in history");
                            return;
                        }
                        value = this.history[this.history.length-1];
                    }
                    var toks  = value.split(" ");
                    var command = toks[0].trim();
                    if(this["processCommand_" + command]) {
                        this["processCommand_" + command](value, toks);
                        this.addHistory(value);
                        this.clearInput();
                    } else {
                        this.processCommand_help(value, toks, "Unknown command: <i>"+ value + "</i>");
                        return;
                    }
                }
            },
                });
}
/**
Copyright 2008-2018 Geode Systems LLC
*/

var CHARTS_CATEGORY = "Charts";
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
var DISPLAY_TABLE = "table";
var DISPLAY_TEXT = "text";
var DISPLAY_CROSSTAB = "crosstab";
var DISPLAY_CORRELATION = "correlation";
var DISPLAY_HEATMAP = "heatmap";

var googleChartsLoaded = false;
function googleChartsHaveLoaded () {
    googleChartsLoaded= true;
}
google.charts.setOnLoadCallback(googleChartsHaveLoaded);

function haveGoogleChartsLoaded () {
    if(!googleChartsLoaded) {
        if (typeof google.visualization !== "undefined") { 
            if (typeof google.visualization.Gauge !== "undefined") { 
                googleChartsLoaded = true;
            }
        }
    }
    return googleChartsLoaded;
}


addGlobalDisplayType({type: DISPLAY_LINECHART, label:"Line Chart",requiresData:true,forUser:true,category:CHARTS_CATEGORY});
addGlobalDisplayType({type:DISPLAY_BARCHART,label: "Bar Chart",requiresData:true,forUser:true,category:CHARTS_CATEGORY});
addGlobalDisplayType({type:DISPLAY_BARSTACK,label: "Stacked Bar Chart",requiresData:true,forUser:true,category:CHARTS_CATEGORY});
addGlobalDisplayType({type: DISPLAY_AREACHART, label:"Area Chart",requiresData:true,forUser:true,category:CHARTS_CATEGORY});
addGlobalDisplayType({type:DISPLAY_BARTABLE,label: "Bar Table",requiresData:true,forUser:true,category:CHARTS_CATEGORY});
addGlobalDisplayType({type:DISPLAY_SCATTERPLOT,label: "Scatter Plot",requiresData:true,forUser:true,category:CHARTS_CATEGORY});
addGlobalDisplayType({type:DISPLAY_HISTOGRAM,label: "Histogram",requiresData:true,forUser:true,category:CHARTS_CATEGORY});
addGlobalDisplayType({type:DISPLAY_BUBBLE,label: "Bubble Chart",requiresData:true,forUser:true,category:CHARTS_CATEGORY});
addGlobalDisplayType({type:DISPLAY_GAUGE,label: "Gauge",requiresData:true,forUser:true,category:CHARTS_CATEGORY});
addGlobalDisplayType({type:DISPLAY_TIMELINECHART,label: "Timeline",requiresData:true,forUser:true,category:CHARTS_CATEGORY});
addGlobalDisplayType({type:DISPLAY_PIECHART,label: "Pie Chart",requiresData:true,forUser:true,category:CHARTS_CATEGORY});
addGlobalDisplayType({type:DISPLAY_SANKEY,label: "Sankey Chart",requiresData:true,forUser:true,category:CHARTS_CATEGORY});

addGlobalDisplayType({type:DISPLAY_CALENDAR,label: "Calendar Chart",requiresData:true,forUser:true,category:"Misc"});
addGlobalDisplayType({type:DISPLAY_STATS , label: "Stats Table",requiresData:false,forUser:true,category:"Misc"});
addGlobalDisplayType({type:DISPLAY_TABLE , label: "Table",requiresData:true,forUser:true,category:"Misc"});
addGlobalDisplayType({type:DISPLAY_TEXT , label: "Text Readout",requiresData:false,forUser:true,category:"Misc"});
addGlobalDisplayType({type:DISPLAY_CORRELATION , label: "Correlation",requiresData:true,forUser:true,category:"Misc"});
addGlobalDisplayType({type:DISPLAY_HEATMAP , label: "Heatmap",requiresData:true,forUser:true,category:"Misc"});




var PROP_CHART_MIN = "chartMin";
var PROP_CHART_MAX = "chartMax";
var PROP_CHART_TYPE = "chartType";
var DFLT_WIDTH = "600px";
var DFLT_HEIGHT = "200px";



/*
 */
function RamaddaFieldsDisplay(displayManager, id, type, properties) {
    var _this = this;
    this.TYPE = "RamaddaFieldsDisplay";
    var SUPER;
    RamaddaUtil.inherit(this, this.RamaddaDisplay = SUPER = new RamaddaDisplay(displayManager, id, type, properties));



    RamaddaUtil.defineMembers(this, {
            needsData: function() {
                return true;
            },
            initDisplay:function() {
                this.createUI();
                if(this.needsData()) {
                    this.setContents(this.getLoadingMessage());
                }
                this.updateUI();
            },
            updateUI: function() {
                this.addFieldsCheckboxes();
            },
            getWikiAttributes: function(attrs) {
                SUPER.getWikiAttributes.call(this, attrs);
                if(this.lastSelectedFields) {
                    attrs.push("fields");
                    var v = "";
                    for(var i=0;i<this.lastSelectedFields.length;i++) {
                        v +=this.lastSelectedFields[i].getId();
                        v+= ",";
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
                var html  =  HtmlUtil.div([ATTR_ID,  this.getDomId(ID_FIELDS),"style","overflow-y: auto;    max-height:" + height +"px;"]," FIELDS ");
                tabTitles.push("Fields"); 
                tabContents.push(html);
                SUPER.getDialogContents.call(this,tabTitles, tabContents);
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
        })}
        


        



/*
Create a chart
id - the id of this chart. Has to correspond to a div tag id 
pointData - A PointData object (see below)
 */
function RamaddaMultiChart(displayManager, id, properties) {
    var ID_CHART = "chart";
    var ID_TRENDS_CBX = "trends_cbx";
    var ID_PERCENT_CBX = "percent_cbx";
    var ID_COLORS = "colors";

    var _this = this;


    //Init the defaults first
    $.extend(this, {
            indexField: -1,
                colorList: ['blue', 'red', 'green', 'orange','fuchsia','teal','navy','silver'],
                curveType: 'none',
                fontSize: 0,
                vAxisMinValue:NaN,
                vAxisMaxValue:NaN,
                showPercent: false,
                percentFields: null
                });
    if(properties.colors)  {
        this.colorList = (""+properties.colors).split(",");
    }
    var SUPER;
    RamaddaUtil.inherit(this, SUPER = new RamaddaFieldsDisplay(displayManager, id, properties.chartType, properties));


    RamaddaUtil.defineMembers(this, {
            getType: function () {
                return this.getProperty(PROP_CHART_TYPE,DISPLAY_LINECHART);
            },
            initDisplay:function() {
                this.createUI();
                this.updateUI();
            },
            clearCachedData: function() {
                SUPER.clearCachedData();
                this.computedData = null;
            },
            updateUI: function() {
                SUPER.updateUI.call(this);
                if(!this.getDisplayReady()) {
                    return;
                }
                this.displayData();
            },
            getWikiAttributes: function(attrs) {
                this.defineWikiAttributes(["vAxisMinValue","vAxisMaxValue"]);
                SUPER.getWikiAttributes.call(this, attrs);
                if(this.colorList.join(",") != "blue,red,green") {
                    attrs.push("colors");
                    attrs.push(this.colorList.join(", "));
                }
            },
               
           initDialog: function() {
                SUPER.initDialog.call(this);
                var _this  = this;
                var updateFunc  = function() {
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
                        if(e.which != 13) {
                            return;
                        }
                        var v = _this.jq(ID_COLORS).val();
                        _this.colorList = v.split(",");
                        _this.displayData();
                        var pointData =   _this.dataCollection.getList();
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
                if(v!=null) {
                    this.colorList = v.split(",");
                    this.displayData();
                    var pointData =   this.dataCollection.getList();
                    this.getDisplayManager().handleEventPointDataLoaded(this, this.lastPointData);
                }
            },
                            getMenuItems: function(menuItems) {
                SUPER.getMenuItems.call(this,menuItems);
                var get = this.getGet();
                //                menuItems.push(HtmlUtil.onClick(get+".setColor();", "Set color"));

                var min = "0";
                if(!isNaN(this.vAxisMinValue)) {
                    min = "" +this.vAxisMinValue;
                }
                var max = "";
                if(!isNaN(this.vAxisMaxValue)) {
                    max = "" +this.vAxisMaxValue;
                }
                var tmp = HtmlUtil.formTable();
                tmp += HtmlUtil.formEntry("Axis Range:", HtmlUtil.input("", min, ["size","7",ATTR_ID,  this.getDomId("vaxismin")]) + " - " +
                                          HtmlUtil.input("", max, ["size","7",ATTR_ID,  this.getDomId("vaxismax")]));
                tmp += HtmlUtil.formEntry("Date Range:", HtmlUtil.input("", this.minDate, ["size","10",ATTR_ID,  this.getDomId("mindate")]) + " - " +
                                          HtmlUtil.input("", this.maxDate, ["size","10",ATTR_ID,  this.getDomId("maxdate")]));


                tmp += HtmlUtil.formEntry("Colors:",
                                          HtmlUtil.input("", this.colorList.join(","), ["size","35",ATTR_ID,  this.getDomId(ID_COLORS)]));
                tmp += "</table>";
                menuItems.push(tmp);

            },
             getChartType: function() {
                return this.getProperty(PROP_CHART_TYPE,DISPLAY_LINECHART);
            },
            defaultSelectedToAll: function() {
                if(this.chartType == DISPLAY_TABLE || this.chartType == DISPLAY_SANKEY) {
                    return true;
                }
                return SUPER.defaultSelectedToAll.call(this);
            },
            askMinZAxis: function() {
                this.setMinZAxis(prompt("Minimum axis value", "0"));
            },
            setMinZAxis: function(v) {
                if(v!=null) {
                    this.vAxisMinValue = parseFloat(v);
                    this.displayData();
                }
            },
            askMaxZAxis: function() {
                this.setMaxZAxis(prompt("Maximum axis value", "100"));
            },
            setMaxZAxis: function(v) {
                if(v!=null) {
                    this.vAxisMaxValue = parseFloat(v);
                    this.displayData();
                }
            },
            askMinDate: function() {
                var ex = this.minDate;
                if(ex == null || ex == "") {
                    ex = "1800-01-01";
                }
                var v = prompt("Minimum date", ex);
                if(v == null) return;
                this.setMinDate(v);
            },
               setMinDate: function(v) {
                this.minDate =v;
                this.displayData();
            },
            askMaxDate: function() {
                var ex = this.maxDate;
                if(ex == null || ex == "") {
                    ex = "2100-01-01";
                } 
                var v = prompt("Maximum date", ex);
                if(v == null) return;
                this.setMaxDate(v);
            },
            setMaxDate: function(v) {
                this.maxDate =v;
                this.displayData();
            },
            trendLineEnabled: function() {
                var chartType = this.getProperty(PROP_CHART_TYPE,DISPLAY_LINECHART);
                if(chartType == DISPLAY_LINECHART || chartType == DISPLAY_AREACHART || chartType == DISPLAY_BARCHART ) {
                    return true;
                }
                return false;
            },
           getDialogContents: function(tabTitles, tabContents) {
                var height = "600";
                var html  =  HtmlUtil.div([ATTR_ID,  this.getDomId(ID_FIELDS),"style","overflow-y: auto;    max-height:" + height +"px;"]," FIELDS ");

                if(this.trendLineEnabled()) {
                    html += HtmlUtil.div([ATTR_CLASS,"display-dialog-subheader"],  "Other");
                
                    html += HtmlUtil.checkbox(this.getDomId(ID_TRENDS_CBX),
                                              [],
                                              this.getProperty("showTrendLines", false)) +"  "  + "Show trend line";
                    html += " ";
                    html += HtmlUtil.checkbox(this.getDomId(ID_PERCENT_CBX),
                                              [],
                                              this.showPercent) +"  "  + "Show percent of displayed total" + "<br>";
                    html +=  "<br>";
                }

                tabTitles.push("Fields"); 
                tabContents.push(html);
                SUPER.RamaddaDisplay.getDialogContents.call(this,tabTitles, tabContents);
            },
            handleEventMapClick: function (source,args) {
                var pointData =   this.dataCollection.getList();
                for(var i=0;i<pointData.length;i++) {
                    pointData[i].handleEventMapClick(this, source, args.lon,args.lat);
                }
            },
             okToHandleEventRecordSelection: function() {
                return true;
            },
            handleEventRecordSelection: function(source, args) {
                //TODO: don't do this in index space, do it in time or space space
                if(source==this) {
                    return;
                }
                if(!this.okToHandleEventRecordSelection()) 
                    return;
                var data =   this.dataCollection.getList()[0];
                if(data != args.data) {
                    return;
                }
                //                console.log("chart index="+ args.index);
                this.setChartSelection(args.index);
            },
            getFieldsToSelect: function(pointData) {
                var chartType = this.getProperty(PROP_CHART_TYPE,DISPLAY_LINECHART);
                if(this.chartType == DISPLAY_TABLE || this.chartType == DISPLAY_BARTABLE || this.chartType == DISPLAY_BUBBLE || this.chartType == DISPLAY_SANKEY || this.chartType == DISPLAY_TIMELINECHART) {
                    //                    return pointData.getRecordFields();
                    return pointData.getNonGeoFields();
                } 
                return  pointData.getChartableFields();
            },
            canDoGroupBy: function() {
                return this.chartType == DISPLAY_PIECHART || this.chartType == DISPLAY_TABLE;
            },
            displayData: function() {
                var _this =this;
                if(!this.getDisplayReady()) {
                    return;
                }
                if(this.inError) {
                    return;
                }
                if(!haveGoogleChartsLoaded ()) {
                    var func = function() {
                        _this.displayData();
                    }
                    this.setContents(this.getLoadingMessage());
                    setTimeout(func,500);
                    return;
                }
                if(this.inError) {
                    return;
                }
                if(!this.hasData()) {
                    this.clearChart();
                    this.setContents(this.getLoadingMessage());
                    return;
                }
                this.setContents(HtmlUtil.div([ATTR_CLASS,"display-message"],
                                              "Building display..."));

                this.allFields =  this.dataCollection.getList()[0].getRecordFields();
                var chartType = this.getProperty(PROP_CHART_TYPE,DISPLAY_LINECHART);
                var selectedFields = this.getSelectedFields([]);
                if(selectedFields.length==0 && this.lastSelectedFields!=null) { 
                    selectedFields = this.lastSelectedFields;
                }

                if(selectedFields == null || selectedFields.length == 0) {
                    if(this.chartType == DISPLAY_TABLE) {
                        //                        selectedFields = this.allFields;
                        selectedFields = this.dataCollection.getList()[0].getNonGeoFields();
                    } else {
                        selectedFields = this.getSelectedFields();
                    }
                }
                        
                if(selectedFields.length==0) {
                    this.setContents("No fields selected");
                    return;
                }

                //Check for the skip
                var tmpFields = [];
                for(var i=0;i<selectedFields.length;i++) {
                    if(!this.shouldSkipField(selectedFields[i])) {
                        tmpFields.push(selectedFields[i]);
                    }
                }
                selectedFields = tmpFields;

                this.lastSelectedFields= selectedFields;

                var props = {
                    includeIndex: true,
                };
                if(this.chartType == DISPLAY_TABLE || this.chartType == DISPLAY_BARTABLE || chartType == DISPLAY_PIECHART || chartType == DISPLAY_SCATTERPLOT || chartType == DISPLAY_HISTOGRAM|| chartType == DISPLAY_BUBBLE|| chartType == DISPLAY_GAUGE || chartType==DISPLAY_SANKEY || chartType==DISPLAY_TIMELINECHART)  {
                    props.includeIndex = false;
                }
                props.groupByIndex = -1;

                if(chartType == DISPLAY_PIECHART) {
                    if(!this.groupBy && this.groupBy!="") {
                        for(var i=0;i<this.allFields.length;i++) {
                            var field = this.allFields[i];
                            if(field.getType() == "string") {
                                this.groupBy = field.getId();
                                break;
                            }
                        }
                    }

                }


                if(this.groupBy) {
                    for(var i=0;i<this.allFields.length;i++) {
                        var field = this.allFields[i];
                        if(field.getId() == this.groupBy) {
                            props.groupByIndex = field.getIndex();
                            props.groupByField = field;
                            this.groupByField  = field;
                            break;
                        }
                    }
                }

                var fieldsToSelect =selectedFields;
                if(this.raw) {
                    fieldsToSelect  = this.dataCollection.getList()[0].getRecordFields();
                    props.raw = true;
                }
                


                if(chartType == DISPLAY_BARTABLE) {
                    props.includeIndexIfDate =  true;
                }


                var dataHasIndex = props.includeIndex;
                var dataList = this.computedData;
                if(this["function"] && this.computedData==null) {
                    var pointData =   this.dataCollection.getList()[0];
                    var allFields = pointData.getRecordFields();
                    var records = pointData.getRecords();
                    var indexField = this.indexField;
                    var chartableFields =this.getFieldsToSelect(pointData);
                    this.hasDate = this.getHasDate(records);
                    var date_formatter = this.getDateFormatter();
                    var setVars = "";
                    for(var i=0;i<chartableFields.length;i++) {
                        var field = chartableFields[i];
                        setVars+="\tvar " + field.getId() +"=args." + field.getId()+";\n";
                    }
                    var code = "function displayChartEval(args) {\n" + setVars +"\treturn  " + this["function"]+"\n}";
                    eval(code);
                    var newList = [];
                    var fieldNames = null;
                    var rowCnt = -1;
                    var indexField = this.indexField;
                    for(var rowIdx=0;rowIdx<records.length;rowIdx++)  {
                        var record = records[rowIdx];
                        var row = record.getData();
                        var date = record.getDate();
                        if(!this.dateInRange(date)) continue;
                        rowCnt++;
                        var values = [];
                        var indexName = null;
                        if(indexField>=0) {
                            var field = allFields[indexField];
                            values.push(record.getValue(indexField)+offset);
                            indexName =  field.getLabel();
                        } else {
                            if(this.hasDate) {
                                values.push(this.getDateValue(date, date_formatter));
                                indexName = "Date";
                            } else {
                                values.push(rowIdx);
                                indexName = "Index";
                            }
                        }
                        if(fieldNames==null) {
                            fieldNames = [indexName, this.functionName?this.functionName:"value"];
                            newList.push(fieldNames);
                        }
                        var args = {};
                        for(var j=0;j<chartableFields.length;j++) {
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

                

                if(dataList == null) {
                    dataList = this.getStandardData(fieldsToSelect, props);
                }
                this.computedData= dataList;

                if(this.rotateTable && dataList.length) {
                    var header = dataList[0];
                    var flipped = [];
                    for(var rowIdx=1;rowIdx<dataList.length;rowIdx++) {
                        var row = dataList[rowIdx];
                    }
                }


                //                console.log("fields:" + selectedFields +" data.length = " + dataList.length +" " + dataList);

                if(dataList.length==0 && !this.userHasSelectedAField) {
                    var pointData =   this.dataCollection.getList()[0];
                    var chartableFields =this.getFieldsToSelect(pointData);
                    for(var i=0;i<chartableFields.length;i++) {
                        var field = chartableFields[i];
                        dataList = this.getStandardData([field], props);
                        if(dataList.length>0) {
                            this.setSelectedFields([field]);
                            //                            console.log("Defaulting to field:" + field.getId());
                            break;
                        }
                    }
                }


                if(dataList.length==0) {
                    this.setContents(HtmlUtil.div([ATTR_CLASS,"display-message"],
                                                  "No data available"));
                    return;
                }



                if(this.showPercent) {
                    var newList = [];
                    var isNumber = [];
                    var isOk = [];
                    var headerRow = null;
                    var fields = null;
                    if(this.percentFields!=null) {
                        fields = this.percentFields.split(",");
                    }
                    for(var i=0;i<dataList.length;i++)  {
                        var row = this.getDataValues(dataList[i]);
                        if(i == 0) {
                            headerRow = row;
                            continue;
                        }
                        if(i == 1) {
                            var seenIndex = false;
                            for(var j=0;j<row.length;j++)  {
                                var valueIsNumber = (typeof row[j] == "number");
                                var valueIsDate = (typeof row[j] == "object");
                                if(valueIsNumber) {
                                    if(dataHasIndex && !seenIndex) {
                                        valueIsNumber = false;
                                        seenIndex = true;
                                    }
                                } 
                                if(valueIsDate) {
                                    seenIndex = true;
                                }

                                if(valueIsNumber && fields!=null) {
                                    valueIsNumber =  fields.indexOf(fieldsToSelect[j].getId())>=0 ||
                                        fields.indexOf("#"+(j+1))>=0;
                                } 
                                //xb                                console.log("fields:" + fields.length +" j:" + j +" id:" +fieldsToSelect[j].getId() +" is:" + valueIsNumber);

                                isNumber.push(valueIsNumber);
                            }
                            var newHeader = [];
                            for(var j=0;j<headerRow.length;j++)  {
                                var v = headerRow[j];
                                if(!isNumber[j]) {
                                    newHeader.push(v);
                                } else {
                                    newHeader.push("% "  + v);
                                }
                            }
                            //                            console.log("header:"  + newHeader)
                            newList.push(newHeader);
                        }

                        var total  = 0;
                        var cnt = 0;
                        for(var j=0;j<row.length;j++)  {
                            if(isNumber[j]) {
                                total +=  parseFloat(row[j]);
                                cnt++;
                            }
                        }
                        var newRow = [];
                        for(var j=0;j<row.length;j++)  {
                            if(isNumber[j]) {
                                if(total!=0)  {
                                    var v = parseFloat(((row[j]/total)*100).toFixed(1));
                                    newRow.push(v);
                                }  else {
                                    newRow.push(NaN);
                                }
                            } else {
                                newRow.push(row[j]);
                            }
                        }
                        //                        console.log("row "  + j +":"  + newRow)
                        newList.push(newRow);
                    }
                    dataList = newList;
                }

                try {
                    this.makeChart(chartType, dataList, props, selectedFields);
                } catch(e) {
                    console.log(e.stack);
                    this.displayError(""+e);
                    return;
                }

                var d = _this.jq(ID_CHART);
                this.lastWidth = d.width();
                if(d.width() == 0) {
                    //                    _this.checkWidth(0);
                }
            },
           //This keeps checking the width of the chart element if its zero
           //we do this for displaying in tabs
            checkLayout: function() {
                var _this = this;
                var d = _this.jq(ID_CHART);
                //       console.log("checklayout:  widths:" + this.lastWidth +" " + d.width());
                if(this.lastWidth!=d.width()) {
                    _this.displayData();
                }
                if(true)return;

                if(d.width() ==0) {
                    var cb = function() {
                        _this.checkWidth(cnt+1);
                    };
                    setTimeout(cb,5000);
                } else {
                    //                    console.log("checkWidth:"+ _this.getTitle() +" calling displayData");
                    _this.displayData();
                }
            },
            printDataList:function(dataList) {
                console.log("data list:" + dataList.length);
                for(var i=0;i<dataList.length;i++)  {
                    var row = dataList[i];
                    var s = "";
                    for(var j=0;j<row.length;j++)  {
                        if(j>0) s+= ", ";
                        s +=  row[j];
                    }
                    console.log("row: " +i +"  " + s);
                }
            },
            clearChart: function() {
                if(this.chart !=null) {
                    this.chart.clearChart();
                }
            },
            setChartSelection: function(index) {
                if(this.chart!=null) {
                    if(this.chart.setSelection) {
                        this.chart.setSelection([{row:index, column:null}]); 
                    }
                    //                    var container = $('#table_div').find('.google-visualization-table-table:eq(0)').parent();
                    //                    var header = $('#table_div').find('.google-visualization-table-table:eq(1)').parent();
                    //                    var row = $('.google-visualization-table-tr-sel');
                    //                    $(container).prop('scrollTop', $(row).prop('offsetTop') - $(header).height());
                }
            },

            tableHeaderMouseover: function(i,tooltip) {
                //alert("new:" + tooltip);
            },
            makeDataTable:function(chartType,dataList,props,selectedFields) {
                dataList = this.filterData(dataList, selectedFields);
                if(this.chartType == DISPLAY_SANKEY) {
                    if(!this.getProperty("doCategories",false)) {
                        return  google.visualization.arrayToDataTable(this.makeDataArray(dataList));
                    }
                    var strings = [];
                    for(var i=0;i<selectedFields.length;i++) {
                        var field = selectedFields[i];
                        if(field.getType()=="string") {
                            strings.push(field);
                        }
                    }
                    var values = [];
                    values.push(["characteristic 1","characteristic 2","value"]);
                    for(var i=1;i<strings.length;i++) {
                        var field1 = strings[i-1];
                        var field2 = strings[i];
                        var cnts = {};
                        for(var r=1;r<dataList.length;r++) {
                            var row = this.getDataValues(dataList[r]);
                            var value1 = row[i-1];
                            var value2 = row[i];
                            var key = value1+"-"+value2;
                            if(!cnts[key]) {
                                cnts[key] ={
                                    v1:value1,
                                    v2:value2,
                                    cnt:0}
                            }
                            cnts[key].cnt++;
                        }
                        for(a in  cnts) {
                            values.push([cnts[a].v1,cnts[a].v2,cnts[a].cnt]);
                        }
                    }
                    return  google.visualization.arrayToDataTable(values);
                }

                if(dataList.length==1) {
                    return  google.visualization.arrayToDataTable(this.makeDataArray(dataList));
                }


                if(this.chartType == DISPLAY_TIMELINECHART) {
                    var records = this.filterData();
                    var strings = [];
                    var stringField = this.getFieldOfType(selectedFields,"string");
                    if(!stringField)
                        stringField = this.getFieldOfType(null,"string");
                    var showLabel = this.getProperty("showLabel",true);
                    var labelFields = [];
                    var labelFieldsTemplate = this.getProperty("labelFieldsTemplate");
                    var toks =  this.getProperty("labelFields","").split(",");
                    for(var i=0;i<toks.length;i++ ) {
                        var field = this.getFieldById(null,toks[i]);
                        if(field) 
                            labelFields.push(field);
                    }
                    


                    var dateFields = this.getFieldsOfType(selectedFields,"date");
                    if(dateFields.length==0) 
                        dateFields = this.getFieldsOfType(null,"date");
                    var values = [];
                    var dataTable = new google.visualization.DataTable();
                    if(dateFields.length<2) {
                        throw new Error("Need to have at least 2 date fields");
                    }
                    if(stringField)
                        dataTable.addColumn({ type: 'string', id: stringField.getLabel() });
                    else
                        dataTable.addColumn({ type: 'string', id: "Index"});
                    if(labelFields.length>0)
                        dataTable.addColumn({ type: 'string', id: "Label"});
                    dataTable.addColumn({ type: 'date', id: dateFields[0].getLabel()});
                    dataTable.addColumn({ type: 'date', id: dateFields[1].getLabel()});
                    for(var r=0;r<records.length;r++) {
                        var row = this.getDataValues(records[r]);
                        var tuple=[];
                        values.push(tuple);
                        if(stringField && showLabel)
                            tuple.push(row[stringField.getIndex()]);
                        else
                            tuple.push("#"+(r+1));
                        if(labelFields.length>0) {
                            var label = "";
                            if(labelFieldsTemplate)
                                label = labelFieldsTemplate;
                            for(var l=0;l<labelFields.length;l++) {
                                var f = labelFields[l];
                                var value = row[f.getIndex()]; 
                                if(labelFieldsTemplate) {
                                    label= label.replace("{" + f.getId() +"}", value);
                                } else {
                                    label+=value +" ";
                                }

                            }
                            tuple.push(label);
                        }
                        tuple.push(row[dateFields[0].getIndex()]);
                        tuple.push(row[dateFields[1].getIndex()]);
                    }
                    dataTable.addRows(values);
                    return  dataTable;
                }



                if(this.chartType == DISPLAY_TABLE) {
                    return  google.visualization.arrayToDataTable(this.makeDataArray(dataList));
                }
                if(this.chartType == DISPLAY_GAUGE) {
                    return this.makeGaugeDataTable(this.makeDataArray(dataList));
                }
                if(this.chartType == DISPLAY_HISTOGRAM) {
                    return  google.visualization.arrayToDataTable(this.makeDataArray(dataList));
                }
                if(this.chartType == DISPLAY_BUBBLE) {
                    return  google.visualization.arrayToDataTable(this.makeDataArray(dataList));
                }

                if(this.chartType == DISPLAY_CALENDAR) {
                    var dataTable = new google.visualization.DataTable();
                    var header = this.getDataValues(dataList[0]);
                    dataTable.addColumn({ type: 'date', id: 'Date' });
                    dataTable.addColumn({ type: 'number', id: header[1]});
                    dataTable.addColumn({type:'string',role:'tooltip', 'p': {'html': true}});
                    var haveMissing = false;
                    var missing = this.getProperty("missingValue",null);
                    if(missing) {
                        haveMissing = true;
                        missing = parseFloat(missing);
                    }
                    var list = [];
                    var cnt = 0;
                    var options =  { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric'};
                    for(var i=1;i<dataList.length;i++) {
                        var value = this.getDataValues(dataList[i])[1];
                        if(value == NaN) continue;
                        if(haveMissing && value == missing) {
                            continue;
                        }
                        cnt++;
                        var dttm = this.formatDate(this.getDataValues(dataList[i])[0],{options:options});
                        dttm = dttm.replace(/ /g,"&nbsp;");
                        var tooltip = "<center><b>" + dttm +"</b></center>" +
                            "<b>" + header[1].replace(/ /g,"&nbsp;")+"</b>:&nbsp;" + this.formatNumber(value) ;
                        tooltip =HtmlUtil.tag("div",["style","padding:5px;"],tooltip);
                        list.push([this.getDataValues(dataList[i])[0],value,tooltip]);
                    }
                    dataTable.addRows(list);
                    return dataTable;
                }

                if(this.chartType == DISPLAY_PIECHART) {
                    var dataTable = new google.visualization.DataTable();
                    var list = [];
                    var groupBy = this.groupByField;
                    var data = selectedFields[0];
                    var header = this.getDataValues(dataList[0]);
                    dataTable.addColumn("string",header[0]);
                    dataTable.addColumn("number",header[1]);
                    //                    dataTable.addColumn({type:'string',role:'tooltip'});
                    if(this.getProperty("bins",null)) {
                        var bins = parseInt(this.getProperty("bins",null));
                        var min =Number.MAX_VALUE;
                        var max =Number.MIN_VALUE;
                        var haveMin = false;
                        var haveMax= false;
                        if(this.getProperty("binMin")) {
                            min = parseFloat(this.getProperty("binMin"));
                            haveMin = true;
                        }
                        if(this.getProperty("binMax")) {
                            max = parseFloat(this.getProperty("binMax"));
                            haveMax = true;
                        }

                        var goodRows = [];
                        for(var i=1;i<dataList.length;i++) {
                            var value  = this.getDataValues(dataList[i])[1];
                            //                            console.log(value +" " + isNaN(value));
                            if(value == Number.POSITIVE_INFINITY || isNaN(value) || !Utils.isNumber(value) ||!Utils.isDefined(value) || value == null) {
                                //                                console.log("bad value:" + value);
                                continue;
                            }
                            if(!haveMin)
                                min = Math.min(value, min);
                            if(!haveMax)
                                max = Math.max(value, max);
                            goodRows.push(this.getDataValues(dataList[i]));
                            //                            console.log(value +" " + min +" " + max);
                        }
                        var binList = [];


                        var step = (max-min)/bins;
                        for(var binIdx=0;binIdx<bins;binIdx++) {
                            binList.push({min:min+binIdx*step,max:min+(binIdx+1)*step,values:[]});
                        }
                        for(var rowIdx=1;rowIdx<goodRows.length;rowIdx++) {
                            var value =  goodRows[rowIdx][1];
                            var ok = false;
                            for(var binIdx=0;binIdx<bins;binIdx++) {
                                if(value<binList[binIdx].min || (value>=binList[binIdx].min && value<=binList[binIdx].max)) {
                                    binList[binIdx].values.push(value);
                                    ok = true;
                                    break;
                                }
                            }
                            if(!ok) {
                                binList[binList.length-1].values.push(value);
                            }
                        }
                        for(var binIdx=0;binIdx<bins;binIdx++) {
                            var bin = binList[binIdx];
                            //                            console.log("bin:" + bin.min +" " + bin.max + " count:" + bin.values.length);
                            list.push(["Bin:" +this.formatNumber(bin.min)+"-" + this.formatNumber(bin.max),
                                       bin.values.length]);
                        }
                    } else {
                        for(var i=1;i<dataList.length;i++) {
                            list.push([this.getDataValues(dataList[i])[0],this.getDataValues(dataList[i])[1]]);
                        }
                    }
                    dataTable.addRows(list);
                    return dataTable;
                }

                var dataTable = new google.visualization.DataTable();
                var header = this.getDataValues(dataList[0]);
                var sample = this.getDataValues(dataList[1]);
                for(var j=0;j<header.length;j++) {
                    var value = sample[j];
                    if(j==0 && props.includeIndex) {
                        //This might be a number or a date
                        if((typeof value) == "object") {
                            //assume its a date
                            dataTable.addColumn('date', header[j]);
                        } else {
                            dataTable.addColumn((typeof value), header[j]);
                        }
                   } else {
                        //Assume all remaining fields are numbers
                        dataTable.addColumn('number', header[j]);
                        dataTable.addColumn({type: 'string', role: 'tooltip','p': {'html': true}});
                    }
                }
                //                dataTable.addColumn({type:'string',  role: 'annotation' });


                var justData= [];
                var begin = props.includeIndex?1:0;
                var tooltipFields = [];
                var toks =  this.getProperty("tooltipFields","").split(",");
                for(var i=0;i<toks.length;i++ ) {
                    var tooltipField = this.getFieldById(null,toks[i]);
                    if(tooltipField)
                        tooltipFields.push(tooltipField);
                }


                for(var i=1;i<dataList.length;i++) {
                    var row = this.getDataValues(dataList[i]);
                    row  = row.slice(0);
                    var label = "";
                    if(dataList[i].record) {
                        for(var j=0;j<tooltipFields.length;j++ ) {
                            label += "<b>"+tooltipFields[j].getLabel()+"</b>: " +
                                dataList[i].record.getValue(tooltipFields[j].getIndex())+"<br>";
                        }
                    }

                    var tooltip = "<div style='padding:8px;'>";
                    tooltip+=label;
                    for(var j=0;j<row.length;j++) {
                        if(j>0)
                            tooltip+="<br>";
                        label = header[j].replace(/ /g,"&nbsp;");
                        value = row[j];
                        if(!value) value ="NA";
                        if(value && (typeof value) =="object") {
                            if(value.f) {
                                value = value.f;
                            }  
                        }
                        if(Utils.isNumber(value)) {
                            value  = this.formatNumber(value);
                        }
                        value = ""+value;
                        value = value.replace(/ /g,"&nbsp;");
                        tooltip+="<b>" +label+"</b>:&nbsp;" +value;
                    }
                    tooltip+="</div>";

                    newRow = [];
                    for(var j=0;j<row.length;j++) {
                        var value = row[j];
                        newRow.push(value);
                        if(j==0 && props.includeIndex) {
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

            makeGoogleChart: function(chartType, dataList, props, selectedFields) {
                //                for(var i=0;i<selectedFields.length;i++) 
                //                    console.log(selectedFields[i].getId());
                //                console.log("makeGoogleChart:" + chartType);
                if(typeof google == 'undefined') {
                    this.setContents("No google");
                    return;
                }

                if(chartType == DISPLAY_TABLE && dataList.length>0) {
                    var header = this.getDataValues(dataList[0]);
                    /*
                    var get = this.getGet();
                    for(var i=0;i<header.length;i++) {
                        var s = header[i];
                        var tt = "tooltip";
                        s = HtmlUtil.tag("div",["onmouseover", get +".tableHeaderMouseover(" + i+",'" + tt +"');"],s);
                        header[i] = s;
                    }
                    */
                }

                var dataTable = this.makeDataTable(chartType, dataList, props,selectedFields);
                var   chartOptions = {
                    tooltip: {isHtml: true},
                };
                $.extend(chartOptions, {
                        lineWidth: 1,
                        colors: this.colorList,
                        curveType:this.curveType,
                        vAxis: {}});


                if(this.lineWidth) {
                    chartOptions.lineWidth = this.lineWidth;
                }
                if(this.fontSize>0) {
                    chartOptions.fontSize = this.fontSize;
                }



                var defaultRange = this.getDisplayManager().getRange(selectedFields[0]);

                var range = [NaN,NaN];
                if(!isNaN(this.vAxisMinValue)) {
                    range[0] = parseFloat(this.vAxisMinValue);
                } else if(defaultRange!=null) {
                    range[0] = defaultRange[0];
                }
                if(!isNaN(this.vAxisMaxValue)) {
                    range[1] = parseFloat(this.vAxisMaxValue);
                } else if(defaultRange!=null) {
                    range[1] = defaultRange[1];
                }
                //console.log("range:" + range[0]+" " + range[1]);
                

                if(!isNaN(range[0])) {
                    chartOptions.vAxis.minValue =range[0];
                }
                if(!isNaN(range[1])) {
                    chartOptions.vAxis.maxValue =range[1];
                }
                var width = "90%";
                var left = "10%";
                useMultipleAxes = this.getProperty("useMultipleAxes",true);

                if((selectedFields.length>1 && useMultipleAxes) || this.getProperty("padRight",false)===true) {
                    width = "80%";
                }
                var chartId = this.getDomId(ID_CHART);
                var divAttrs = [ATTR_ID, chartId];
                if(chartType == DISPLAY_PIECHART) {
                    divAttrs.push("style");
                    var style = "";
                    if(this.getProperty("width"))  
                       style += "width:" + this.getProperty("width") +";" ;                    
                    else 
                        style += "width:" + "100%;";
                    if(this.getProperty("height"))
                        style += "height:" + this.getProperty("height") +";" ;                    
                    else 
                        style += "height:" + "100%;";
                    divAttrs.push(style);
                } else {
                    //                    divAttrs.push("style");
                    //                    divAttrs.push("width:600px;");
                }
                divAttrs.push("style");
                divAttrs.push("height:100%;");
                this.setContents(HtmlUtil.div(divAttrs,""));


                if(chartType == DISPLAY_SANKEY) {
                    chartOptions.height = parseInt(this.getProperty("chartHeight",this.getProperty("height","400")));
                    chartOptions.sankey =  {
                        node: {
                            colors: this.colors,
                            width:5,
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
                }

                if(chartType == DISPLAY_LINECHART || chartType == DISPLAY_AREACHART || 
                   chartType == DISPLAY_BARCHART ||
                   chartType == DISPLAY_HISTOGRAM ||
                   chartType == DISPLAY_BARSTACK ) {
                    chartOptions.height = this.getProperty("chartHeight",this.getProperty("height","150"));
                    $.extend(chartOptions, {
                            //series: [{targetAxisIndex:0},{targetAxisIndex:1},],
                            legend: { position: 'bottom' },
                                chartArea:{
                                left: this.getProperty("chartLeft",left),
                                    top:this.getProperty("chartTop","10"),
                                    height:this.getProperty("chartHeight","70%"),
                                    width:this.getProperty("chartWidth",width),
                                },
                        });
                    //                    console.log(JSON.stringify(chartOptions));
                    if (useMultipleAxes) {
                        $.extend(chartOptions, {
                            series: [{targetAxisIndex:0},{targetAxisIndex:1}]
                            });
                    }

                    if(this.getProperty("showTrendLines", false)) {
                        chartOptions.trendlines = {
                            0: {
                                type: 'linear',
                                color: 'green',
                            }
                        };
                    }

                    if(this.hAxis) {
                       if (chartOptions.hAxis) {
                           chartOptions.hAxis.title = this.hAxis;
                       } else {
                           chartOptions.hAxis = {title: this.hAxis}
                       }
                    }
                    if(this.vAxis) {
                       if (chartOptions.vAxis) {
                           chartOptions.vAxis.title = this.vAxis;
                       } else {
                           chartOptions.vAxis = {title: this.vAxis}
                       }
                    }

                    if(Utils.isDefined(this.chartHeight)) {
                        chartOptions.height=this.chartHeight;
                    }
                }

                if(chartType == DISPLAY_BARTABLE) {
                    var height  = "";
                    if(Utils.isDefined(this.chartHeight)) {
                        height = this.chartHeight;
                    } else {
                        if(dataList.length>1)  {
                            var numBars  = dataList.length;
                            if(this.isStacked) {
                                height = numBars*22;
                            } else {
                                height = numBars*22+numBars*14*(this.getDataValues(dataList[0]).length-2);
                            }
                        }
                    }
                    
                    $.extend(chartOptions, {
                        title: "the title",
                        bars: 'horizontal',
                        colors: this.colorList,
                        width: (Utils.isDefined(this.chartWidth)?this.chartWidth:"100%"),
                        chartArea: {left:'30%',top:0,width:'70%',height:'80%'},
                        height: height,
                        bars: 'horizontal',
                        tooltip: {showColorCode: true},
                        legend: { position: 'none' },
                                });

                    if(Utils.isDefined(this.isStacked)) {
                        chartOptions.isStacked = this.isStacked;                        
                    }

                    if(this.hAxis)
                        chartOptions.hAxis= {title: this.hAxis};
                    if(this.vAxis)
                        chartOptions.vAxis= {title: this.vAxis};
                    //                    console.log(chartOptions);
                    //                    this.chart = new google.visualization.BarChart(document.getElementById(chartId));
                    this.chart = new google.charts.Bar(document.getElementById(chartId));


                } else if(chartType == DISPLAY_BARCHART || chartType == DISPLAY_BARSTACK) {
                    if(chartType == DISPLAY_BARSTACK) {
                        chartOptions.isStacked = true;
                    }
                    if(this.getProperty("barWidth")) {
                        chartOptions.bar=  {groupWidth: this.getProperty("barWidth")}
                    }
                    chartOptions.orientation =  "horizontal";
                    this.chart = new google.visualization.BarChart(document.getElementById(chartId));
                } else if(chartType == DISPLAY_SANKEY) {
                    this.chart = new google.visualization.Sankey(document.getElementById(chartId));
                } else  if(chartType == DISPLAY_SCATTERPLOT) {
                    var height  = 400;
                    if(Utils.isDefined(this.chartHeight)) {
                        height = this.chartHeight;
                    }
                    var width  = "100%";
                    if(Utils.isDefined(this.chartWidth)) {
                        width = this.chartWidth;
                    }
                    //                    $("#" + chartId).css("border","1px red solid");
                    //                    $("#" + chartId).css("width",width);
                    $("#" + chartId).css("width",width);
                    $("#" + chartId).css("height",height);
                    chartOptions = {
                        title: '',
                        tooltip: {isHtml: true},
                        legend: 'none',
                        chartArea: {left:"10%", top:10, height:"80%",width:"90%"}
                        };

                    if(this.getShowTitle()) {
                        chartOptions.title =  this.getTitle(true);
                    }

                    if(dataList.length>0 && this.getDataValues(dataList[0]).length>1) { 
                        chartOptions.hAxis =  {title: this.getDataValues(dataList[0])[0]};
                        chartOptions.vAxis =  {title: this.getDataValues(dataList[0])[1]};
                        //We only have the one vAxis range for now
                        if(!isNaN(this.vAxisMinValue)) {
                            chartOptions.hAxis.minValue = this.vAxisMinValue;
                            chartOptions.vAxis.minValue = this.vAxisMinValue;
                        }
                        if(!isNaN(this.vAxisMaxValue)) {
                            chartOptions.hAxis.maxValue = this.vAxisMaxValue;
                            chartOptions.vAxis.maxValue = this.vAxisMaxValue;
                        }
                    }


                    this.chart = new google.visualization.ScatterChart(document.getElementById(chartId));
                } else  if(chartType == DISPLAY_HISTOGRAM) {
                    if(this.legendPosition) {
                        chartOptions.legend={};
                        chartOptions.legend.position=this.legendPosition;
                    }
                    var isStacked = this.getProperty("isStacked",null);
                    if(isStacked) 
                        chartOptions.isStacked = isStacked=="true"?true:isStacked=="false"?false:isStacked;
                    chartOptions.vAxis={};
                    chartOptions.vAxis.viewWindow={};
                    if(Utils.isDefined(this.logScale)) {
                        chartOptions.vAxis.logScale = (""+this.logScale) == true;
                    }
                    if(this.textPosition) {
                        chartOptions.vAxis.textPosition = this.textPosition;
                    }
                    
                    if(Utils.isDefined(this.minValue)) {
                        chartOptions.vAxis.viewWindow.min = parseFloat(this.minValue);
                    }
                    if(Utils.isDefined(this.maxValue)) {
                        chartOptions.vAxis.viewWindow.max = parseFloat(this.maxValue);
                    }
                    if(!isNaN(this.vAxisMaxValue)) {
                        chartOptions.vAxis.maxValue = parseFloat(this.vAxisMaxValue);
                    }
                    //                    console.log(JSON.stringify(chartOptions));
                    if(!isNaN(this.vAxisMinValue)) {
                        chartOptions.vAxis.minValue = parseFloat(this.vAxisMinValue);
                    }
                    this.chart = new google.visualization.Histogram(document.getElementById(chartId));

                } else  if(chartType == DISPLAY_GAUGE) {
                    this.dataList = dataList;
                    this.chartOptions = chartOptions;
                    var min =Number.MAX_VALUE;
                    var max =Number.MIN_VALUE;
                    var setMinMax = true;
                    for(var row=1;row<dataList.length;row++) {
                        var tuple = this.getDataValues(dataList[row]);
                        //                        if(tuple.length>2) setMinMax = false;
                        for(var col=0;col<tuple.length;col++) {
                            if(!Utils.isNumber(tuple[col])) {
                                continue;
                            }
                            var value = tuple[col];
                            min = Math.min(min, value);
                            max = Math.max(max, value);
                        }
                    }
                    min = Utils.formatNumber(min,true);
                    max = Utils.formatNumber(max,true);
                    if(Utils.isDefined(this.gaugeMin))  {
                        setMinMax = true;
                        min  = parseFloat(this.gaugeMin);
                    }
                    if(Utils.isDefined(this.gaugeMax)) {
                        setMinMax = true;
                        max  = parseFloat(this.gaugeMax);
                    }
                    if(setMinMax) {
                        chartOptions.min = min;
                        chartOptions.max = max;
                    }
                    this.chart = new google.visualization.Gauge(document.getElementById(chartId));
                } else  if(chartType == DISPLAY_BUBBLE) {
                    var ct = this.getColorTable();
                    if(ct)
                        chartOptions.colors=ct;
                    else if(!this.colors)
                        chartOptions.colors=this.colorList;
                    if(chartOptions.colors)
                       chartOptions.colors=Utils.getColorTable("rainbow");

                    chartOptions.chartArea = {left:100,top:10,width:'98%',height:'90%'}
                    chartOptions.colorAxis  = {
                        legend: {
                            position:"in"
                        }
                    }

                    chartOptions.bubble  = {
                        textStyle: {auraColor:"none"},
                        stroke:"#666"
                    };
                    chartOptions.hAxis = {};
                    chartOptions.vAxis = {};
                    header = this.getDataValues(dataList[0]);
                    chartOptions.hAxis.format =  this.getProperty("hAxisFormat", null);
                    chartOptions.vAxis.format =  this.getProperty("vAxisFormat", null);
                    chartOptions.hAxis.title =  this.getProperty("hAxisTitle", header.length>1?header[1]:null);
                    chartOptions.vAxis.title =  this.getProperty("vAxisTitle", header.length>2?header[2]:null);
                    this.chart = new google.visualization.BubbleChart(document.getElementById(chartId));
                } else  if(chartType == DISPLAY_CALENDAR) {
                    chartOptions.calendar = {
                        cellSize: parseInt(this.getProperty("cellSize",15))
                    };
                    //If a calendar is show in tabs then it never returns from the draw
                    if(this.jq(ID_CHART).width() == 0) {
                        return;
                    }
                    this.chart = new google.visualization.Calendar(document.getElementById(chartId));
                } else  if(chartType == DISPLAY_TIMELINECHART) {
                    this.chart = new google.visualization.Timeline(document.getElementById(chartId));
                } else  if(chartType == DISPLAY_PIECHART) {
                    chartOptions.tooltip = {textStyle: {color: '#000000'}, showColorCode: true};
                    if(this.getProperty("bins",null)) {
                        chartOptions.title="Bins: " +this.getDataValues(dataList[0])[1];
                    } else {
                        chartOptions.title=this.getDataValues(dataList[0])[0] +" - " +this.getDataValues(dataList[0])[1];
                    }

                    if(this.is3D) {
                        chartOptions.is3D = true;
                    }
                    if(this.pieHole) {
                        chartOptions.pieHole = this.pieHole;
                    }
                    if(this.sliceVisibilityThreshold) {
                        chartOptions.sliceVisibilityThreshold = this.sliceVisibilityThreshold;
                    }
                    this.chart = new google.visualization.PieChart(document.getElementById(chartId));
                } else  if(chartType == DISPLAY_TABLE) {

                    chartOptions.height = null;
                    if(this.chartHeight)  {
                        chartOptions.height = this.chartHeight;
                    }
                    if(chartOptions.height == null) {
                        var height = this.getProperty("height",null);
                        if(height) {
                            chartOptions.height = height;
                        }
                    }
                    if(chartOptions.height == null) {
                        chartOptions.height = "300px";
                    }
                    chartOptions.allowHtml = true;
                    if(dataList.length && this.getDataValues(dataList[0]).length>4) {
                        chartOptions.cssClassNames = {headerCell: 'display-table-header-max' };
                    } else {
                        chartOptions.cssClassNames = {headerCell: 'display-table-header' };
                    }
                    this.chart = new google.visualization.Table(document.getElementById(chartId));
                } else  if(chartType == DISPLAY_AREACHART) {
                    if(this.isStacked)
                        chartOptions.isStacked = true;
                    this.chart = new google.visualization.AreaChart(document.getElementById(chartId));
                } else {
                    //                    this.chart =  new Dygraph.GVizChart(
                    //                    document.getElementById(chartId));
                    this.chart = new google.visualization.LineChart(document.getElementById(chartId));
                }
                if(this.chart!=null) {
                    if(!Utils.isDefined(chartOptions.height)) {
                        chartOptions.height = "100%";
                    }
                    this.chart.draw(dataTable, chartOptions); 
                    var theDisplay = this;

                    google.visualization.events.addListener(this.chart, 'onmouseover', function(event) {
                            mapVar  = theDisplay.getProperty("mapVar",null);
                            if(!Utils.stringDefined(mapVar)) {
                                return;
                            }
                            row = event.row;
                            pointData = theDisplay.dataCollection.getList()[0];
                            var fields =  pointData.getRecordFields();
                            var records = pointData.getRecords();
                            var record = records[row];
                            map = ramaddaMapMap[mapVar];
                            if(map) {
                                if(theDisplay.mouseOverPoint)
                                    map.removePoint(theDisplay.mouseOverPoint);
                            } else {
                            }
                            if(record && map) {
                                latField = null;
                                lonField = null;
                                for(i=0;i<fields.length;i++) {
                                    if(fields[i].isFieldLatitude()) latField = fields[i];
                                    else if(fields[i].isFieldLongitude()) lonField = fields[i];
                                }
                                if(latField && lonField) {
                                    lat = record.getValue(latField.getIndex());
                                    lon = record.getValue(lonField.getIndex());
                                    theDisplay.mouseOverPoint  = map.addPoint(chartId, new OpenLayers.LonLat(lon,lat));
                                }
                            }
                        });
                    google.visualization.events.addListener(this.chart, 'select', function(event) {
                            if(theDisplay.chart.getSelection) {
                                var selected = theDisplay.chart.getSelection();
                                if(selected && selected.length>0) {
                                    var index = selected[0].row;
                                    theDisplay.displayManager.propagateEventRecordSelection(theDisplay, 
                                                                                            theDisplay.dataCollection.getList()[0], {index:index});
                                }
                            }
                        });
                }
            }
        });



    this.makeChart = this.makeGoogleChart;
}



function LinechartDisplay(displayManager, id, properties) {
    properties = $.extend({"chartType": DISPLAY_LINECHART}, properties);
    RamaddaUtil.inherit(this, new RamaddaMultiChart(displayManager, id, properties));
    addRamaddaDisplay(this);
}

function AreachartDisplay(displayManager, id, properties) {
    properties = $.extend({"chartType": DISPLAY_AREACHART}, properties);
    RamaddaUtil.inherit(this, new RamaddaMultiChart(displayManager, id, properties));
    addRamaddaDisplay(this);
}

function BarchartDisplay(displayManager, id, properties) {
    properties = $.extend({"chartType": DISPLAY_BARCHART}, properties);
    RamaddaUtil.inherit(this, new RamaddaMultiChart(displayManager, id, properties));
    addRamaddaDisplay(this);
}

function BarstackDisplay(displayManager, id, properties) {
    properties = $.extend({"chartType": DISPLAY_BARSTACK,"isStacked":true}, properties);
    RamaddaUtil.inherit(this, new RamaddaMultiChart(displayManager, id, properties));
    addRamaddaDisplay(this);
}

function PiechartDisplay(displayManager, id, properties) {
    properties = $.extend({"chartType": DISPLAY_PIECHART}, properties);
    RamaddaUtil.inherit(this, new RamaddaMultiChart(displayManager, id, properties));
    addRamaddaDisplay(this);
}

function TimelinechartDisplay(displayManager, id, properties) {
    properties = $.extend({"chartType": DISPLAY_TIMELINECHART}, properties);
    RamaddaUtil.inherit(this, new RamaddaMultiChart(displayManager, id, properties));
    addRamaddaDisplay(this);
}

function SankeyDisplay(displayManager, id, properties) {
    properties = $.extend({"chartType": DISPLAY_SANKEY}, properties);
    RamaddaUtil.inherit(this, new RamaddaMultiChart(displayManager, id, properties));
    addRamaddaDisplay(this);
}

function CalendarDisplay(displayManager, id, properties) {
    properties = $.extend({"chartType": DISPLAY_CALENDAR}, properties);
    RamaddaUtil.inherit(this, new RamaddaMultiChart(displayManager, id, properties));
    addRamaddaDisplay(this);
    RamaddaUtil.inherit(this,{
            xgetDimensionsStyle: function() {
                var height = this.getProperty("height",200);
                if(height>0) {
                    return " height:" + height +"px; " + " max-height:" + height +"px; overflow-y: auto;";
                }
                return "";
            },
            canDoMultiFields: function() {
                return false;
            }
        });
}


function TableDisplay(displayManager, id, properties) {
    properties = $.extend({"chartType": DISPLAY_TABLE}, properties);
    RamaddaUtil.inherit(this, new RamaddaMultiChart(displayManager, id, properties));
    addRamaddaDisplay(this);
}

function HistogramDisplay(displayManager, id, properties) {
    properties = $.extend({"chartType": DISPLAY_HISTOGRAM}, properties);
    RamaddaUtil.inherit(this, new RamaddaMultiChart(displayManager, id, properties));
    addRamaddaDisplay(this);
    RamaddaUtil.inherit(this,{
             okToHandleEventRecordSelection: function() {
                return false;
            },
                });
}

function GaugeDisplay(displayManager, id, properties) {
    properties = $.extend({"chartType": DISPLAY_GAUGE}, properties);
    RamaddaUtil.inherit(this, new RamaddaMultiChart(displayManager, id, properties));
    addRamaddaDisplay(this);
    RamaddaUtil.inherit(this,{
            makeGaugeDataTable: function(dataList) {
                if(!Utils.isDefined(this.index)) this.index = dataList.length-1;
                var index = this.index+1;
                var list = [];
                list.push(["Label","Value"]);
                var header = this.getDataValues(dataList[0]);
                if(index>=dataList.length) index = dataList.length-1;
                var row = this.getDataValues(dataList[index]);
                for(var i=0;i<row.length;i++) {
                    if(!Utils.isNumber(row[i])) continue;
                    var h  = header[i];
                    if(h.length>20) {
                        var index = h.indexOf("(");
                        if(index>0) {
                            h = h.substring(0,index);
                        } 
                    }
                    if(h.length>20) {
                        h = h.substring(0,19)+"...";
                    }
                    if(this.gaugeLabel) h = this.gaugeLabel;
                    else if(this["gaugeLabel" + (i+1)]) h = this["gaugeLabel" + (i+1)];
                    var value = row[i];
                    list.push([h,Utils.formatNumber(value,true)]);
                }
                return  google.visualization.arrayToDataTable(list);
        },
        setChartSelection: function(index) {
                if(this.chart) {
                    this.index  = index;
                    var dataTable = this.makeGaugeDataTable(this.dataList);
                    this.chart.draw(dataTable, this.chartOptions);
                }
            },
                });
}

function BubbleDisplay(displayManager, id, properties) {
    properties = $.extend({"chartType": DISPLAY_BUBBLE}, properties);
    RamaddaUtil.inherit(this, new RamaddaMultiChart(displayManager, id, properties));
    addRamaddaDisplay(this);
}


function BartableDisplay(displayManager, id, properties) {
    properties = $.extend({"chartType": DISPLAY_BARTABLE}, properties);
    RamaddaUtil.inherit(this, new RamaddaMultiChart(displayManager, id, properties));
    $.extend(this, {
            getDefaultSelectedFields: function(fields, dfltList) {
                var f = [];
                for(i=0;i<fields.length;i++) { 
                    var field = fields[i];
                    if(!field.isNumeric) {
                        f.push(field);
                        break;
                    }
                }
                for(i=0;i<fields.length;i++) { 
                    var field = fields[i];
                    if(field.isNumeric) {
                        f.push(field);
                        break;
                    }
                }
                return f;
            }
        });

    addRamaddaDisplay(this);
}



function ScatterplotDisplay(displayManager, id, properties) {
    properties = $.extend({"chartType": DISPLAY_SCATTERPLOT}, properties);
    RamaddaUtil.inherit(this, new RamaddaMultiChart(displayManager, id, properties));
    $.extend(this, {
            getDefaultSelectedFields: function(fields, dfltList) {
                var f = [];
                for(i=0;i<fields.length;i++) { 
                    var field = fields[i];
                    if(field.isNumeric) {
                        f.push(field);
                        if(f.length>=2) 
                            break;
                    }
                }
                return f;
            }
        });

    addRamaddaDisplay(this);
}


function RamaddaTextDisplay(displayManager, id, properties) {
    var SUPER;
    $.extend(this, SUPER = new RamaddaDisplay(displayManager, id, DISPLAY_TEXT, properties));
    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
            lastHtml:"<p>&nbsp;<p>&nbsp;<p>",
            initDisplay: function() {
                this.createUI();
                this.setContents(this.lastHtml);
            },
            handleEventRecordSelection: function(source,  args) {
                this.lastHtml = args.html;
                this.setContents(args.html);
            }
        });
}


function RamaddaStatsDisplay(displayManager, id, properties,type) {
    var SUPER;
    var dflt = Utils.isDefined(properties["showDefault"])?properties["showDefault"]:true;
    $.extend(this, {
            showMin:dflt,
                showMax: dflt,
                showAverage:dflt,
                showStd:dflt,
                showPercentile:dflt,
                showCount:dflt,
                showTotal: dflt,
                showPercentile:dflt,
                showMissing:dflt,
                showUnique:dflt,
                showType:dflt,
                showText: dflt,
                });
    RamaddaUtil.inherit(this, SUPER = new RamaddaFieldsDisplay(displayManager, id, type||DISPLAY_STATS, properties));

    if(!type)
        addRamaddaDisplay(this);


    RamaddaUtil.defineMembers(this, {
            "map-display": false,
            needsData: function() {
                return true;
                //                return this.getProperty("loadData", false) || this.getCreatedInteractively();
            },
            handleEventPointDataLoaded : function(source, pointData) {
                if(!this.needsData()) {
                    if(this.dataCollection == null) {
                        this.dataCollection =  source.dataCollection;
                        this.updateUI();
                    }
                }
            },
            getDefaultSelectedFields: function(fields, dfltList) {
                if(dfltList!=null && dfltList.length>0) {
                    return dfltList;
                }
                var tuples = this.getStandardData(null,{includeIndex: false});
                var justOne = (tuples.length == 2);

                //get the numeric fields
                var l = [];
                for(i=0;i<fields.length;i++) { 
                    var field = fields[i];
                    if(!justOne && (!this.showText && !field.isNumeric)) continue;
                    var lbl  =field.getLabel().toLowerCase();
                    if(lbl.indexOf("latitude")>=0 || lbl.indexOf("longitude")>=0) {
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
                if(!this.hasData()) {
                    this.setContents(this.getLoadingMessage());
                    return;
                }
                var dataList = this.getStandardData(null,{includeIndex: false});
                var allFields =  this.dataCollection.getList()[0].getRecordFields();
                this.allFields =  allFields;
                var fields = this.getSelectedFields([]);
                var fieldMap = {};
                var stats = [];
                var justOne = (dataList.length == 2);
                for(var rowIdx=1;rowIdx<dataList.length;rowIdx++) {
                    var tuple = this.getDataValues(dataList[rowIdx]);
                    if(rowIdx == 1) {
                        for(var col=0;col<tuple.length;col++) {
                            stats.push({isNumber:false,count:0,min:Number.MAX_SAFE_INTEGER,uniqueMap:{},unique:0,std:0,max:Number.MIN_SAFE_INTEGER,total:0,numMissing:0,numNotMissing:0,type:null,values:[]});
                        }
                    }
                    for(var fieldIdx=0;fieldIdx<fields.length;fieldIdx++) {
                        var field = fields[fieldIdx];
                        var col = field.getIndex()
                        stats[col].type = field.getType();
                        var v  = tuple[col];
                        if(v) {
                            if(!Utils.isDefined(stats[col].uniqueMap[v])) {
                                stats[col].uniqueMap[v] = 1;
                                stats[col].unique++;
                            } else {
                                stats[col].uniqueMap[v]++;
                            }
                        }
                        stats[col].isNumber = field.isNumeric;
                        stats[col].count++;
                        if(v==null) {
                            stats[col].numMissing++;
                        } else {
                            stats[col].numNotMissing++;
                        }
                        if(v && (typeof v == 'number')) {
                            var label = field.getLabel().toLowerCase();
                            if(label.indexOf("latitude")>=0 || label.indexOf("longitude")>=0) {
                                continue;
                            }
                            stats[col].total+=v;
                            stats[col].max=Math.max(stats[col].max, v);
                            stats[col].min=Math.min(stats[col].min, v);
                            stats[col].values.push(v);
                        }
                    }
                }


                if(this.showUnique) {
                    for(var fieldIdx=0;fieldIdx<fields.length;fieldIdx++) {
                        var field = fields[fieldIdx];
                        var col = field.getIndex();
                        stats[col].uniqueMax = 0;
                        stats[col].uniqueValue = "";
                        for(var v in stats[col].uniqueMap) {
                            var count = stats[col].uniqueMap[v];
                            if(count>stats[col].uniqueMax) {
                                stats[col].uniqueMax =  count;
                                stats[col].uniqueValue  =v ;
                            }
                        }
                    }
                }

                if(this.showStd) {
                    for(var fieldIdx=0;fieldIdx<fields.length;fieldIdx++) {
                        var field = fields[fieldIdx];
                        var col = field.getIndex();
                        var values = stats[col].values;
                        if(values.length>0) {
                            var average = stats[col].total/values.length;
                            var stdTotal = 0;
                            for(var i=0;i<values.length;i++) {
                                var diff = values[i]-average;
                                stdTotal += diff*diff;
                            }
                            var mean = stdTotal/values.length;
                            stats[col].std = Math.sqrt(mean);
                        }
                    }
                }
                var border = (justOne?"0":"1");
                var html = HtmlUtil.openTag("table",["border", border ,"bordercolor","#ccc","class","display-stats","cellspacing","1","cellpadding","5"]);
                var dummy = ["&nbsp;"];
                if(!justOne) {
                    header = [""];
                    if(this.showCount) {
                        header.push("Count");
                        dummy.push("&nbsp;");
                    }
                    if(this.showMin) {
                        header.push("Min");
                        dummy.push("&nbsp;");
                    }
                    if(this.showPercentile) {
                        header.push("25%");
                        dummy.push("&nbsp;");
                        header.push("50%");
                        dummy.push("&nbsp;");
                        header.push("75%");
                        dummy.push("&nbsp;");
                    }
                    if(this.showMax) {
                        header.push("Max");
                        dummy.push("&nbsp;");
                    }
                    if(this.showTotal) {
                        header.push("Total");
                        dummy.push("&nbsp;");
                    }
                    if(this.showAverage) {
                        header.push("Average");
                        dummy.push("&nbsp;");
                    }
                    if(this.showStd) {
                        header.push("Std");
                        dummy.push("&nbsp;");
                    }
                    if(this.showUnique) {
                        header.push("# Unique");
                        dummy.push("&nbsp;");
                        header.push("Top");
                        dummy.push("&nbsp;");
                        header.push("Freq.");
                        dummy.push("&nbsp;");
                    }
                    if(this.showMissing) {
                        header.push("Not&nbsp;Missing");
                        dummy.push("&nbsp;");
                        header.push("Missing");
                        dummy.push("&nbsp;");
                    }
                    if(this.showType) {
                        header.push("Type");
                        dummy.push("&nbsp;");                        
                    }
                    html += HtmlUtil.tr(["valign","bottom"],HtmlUtil.tds(["class","display-stats-header","align","center"],header));
                }
                var cats = [];
                var catMap = {};
                for(var fieldIdx=0;fieldIdx<fields.length;fieldIdx++) {
                    var field = fields[fieldIdx];
                    var col = field.getIndex();
                    var field = allFields[col];
                    var right = "";
                    var total = "&nbsp;";
                    var label = field.getLabel().toLowerCase();
                    var avg  = stats[col].numNotMissing==0?"NA":this.formatNumber(stats[col].total/stats[col].numNotMissing);
                    //Some guess work about when to show a total
                    if(label.indexOf("%")<0 && label.indexOf("percent")<0 && label.indexOf("median")<0) {
                        total  =  this.formatNumber(stats[col].total);
                    } 
                    if(justOne) {
                        right = HtmlUtil.tds(["xalign","right"],[this.formatNumber(stats[col].min)]);
                        continue;
                    } 
                    var values = [];
                    if(!stats[col].isNumber && this.showText) {
                        if(this.showCount)
                            values.push(stats[col].count);
                        if(this.showMin)
                            values.push("-");
                        if(this.showPercentile) {
                            values.push("-");
                            values.push("-");
                            values.push("-");
                        }
                        if(this.showMax)
                            values.push("-");
                        values.push("-");
                        if(this.showAverage) {
                            values.push("-");
                        }
                        if(this.showStd) {
                            values.push("-");
                        }
                        if(this.showUnique) {
                            values.push(stats[col].unique);
                            values.push(stats[col].uniqueValue);
                            values.push(stats[col].uniqueMax);
                        }
                        if(this.showMissing) {
                            values.push(stats[col].numNotMissing);
                            values.push(stats[col].numMissing);
                        }
                    } else {
                        if(this.showCount) {
                            values.push(stats[col].count); 
                        }
                        if(this.showMin) {
                            values.push(this.formatNumber(stats[col].min));
                        }
                        if(this.showPercentile) {
                            var range = stats[col].max-stats[col].min;
                            values.push(this.formatNumber(stats[col].min+range*0.25));
                            values.push(this.formatNumber(stats[col].min+range*0.50));
                            values.push(this.formatNumber(stats[col].min+range*0.75));
                        }
                        if(this.showMax) {
                            values.push(this.formatNumber(stats[col].max));
                        }
                        if(this.showTotal) {
                            values.push(total);
                        }
                        if(this.showAverage) {
                            values.push(avg);
                        }
                        if(this.showStd) { 
                           values.push(this.formatNumber(stats[col].std));
                        }
                        if(this.showUnique) {
                            values.push(stats[col].unique);
                            if(Utils.isNumber(stats[col].uniqueValue)) {
                                values.push(this.formatNumber(stats[col].uniqueValue));
                            } else  {
                                values.push(stats[col].uniqueValue);
                            }
                            values.push(stats[col].uniqueMax);
                        }
                        if(this.showMissing) {
                            values.push(stats[col].numNotMissing);
                            values.push(stats[col].numMissing);
                        }

                    } 
                    if(this.showType) {
                        values.push(stats[col].type);
                    }
                    right = HtmlUtil.tds(["align","right"],values);
                    var align = (justOne?"right":"left");
                    var label = field.getLabel();
                    var toks =  label.split("!!");
                    var title = field.getId();
                    label = toks[toks.length-1];
                    if(justOne) {
                        label +=":";
                    }
                    label = label.replace(/ /g,"&nbsp;")
                    var row =  HtmlUtil.tr([],HtmlUtil.td(["align",align],"<b>" +HtmlUtil.tag("div", ["title",title], label)+"</b>") + right);
                    if(justOne) {
                        html += row;
                    } else {
                        html += row;
                    }
                }
                html += "</table>";
                this.setContents(html);
                this.initTooltip();
            },
            handleEventRecordSelection: function(source,  args) {
                //                this.lastHtml = args.html;
                //                this.setContents(args.html);
            }
        });
}





function RamaddaCrosstabDisplay(displayManager, id, properties) {
    var SUPER;
    RamaddaUtil.inherit(this, SUPER = new RamaddaFieldsDisplay(displayManager, id, DISPLAY_CROSSTAB, properties));
    addRamaddaDisplay(this);
    this.columns=this.getProperty("columns","").split(",");
    this.rows=this.getProperty("rows","").split(",");
    RamaddaUtil.defineMembers(this, {
            "map-display": false,
            needsData: function() {
                return true;
            },
            handleEventPointDataLoaded : function(source, pointData) {
                if(!this.needsData()) {
                    if(this.dataCollection == null) {
                        this.dataCollection =  source.dataCollection;
                        this.updateUI();
                    }
                }
            },
            fieldSelectionChanged: function() {
                this.updateUI();
            },
            updateUI: function(pointData) {
                SUPER.updateUI.call(this,pointData);
                if(!this.hasData()) {
                    this.setContents(this.getLoadingMessage());

                    return;
                }
                var dataList = this.getStandardData(null,{includeIndex: false});
                var allFields =  this.dataCollection.getList()[0].getRecordFields();
                var fieldMap = {};
                cols = [];
                rows = [];
                
                for(var j=0;j<this.columns.length;j++) {
                    var name = this.columns[j];
                    for(var i=0;i<allFields.length;i++) {
                        field = allFields[i];
                        if(name == field.getLabel()|| name == ("#" +(i+1))) {
                            cols.push(allFields[i]);
                            break;
                        }
                    }
                }
                for(var j=0;j<this.rows.length;j++) {
                    var name = this.rows[j];
                    for(var i=0;i<allFields.length;i++) {
                        if(name == allFields[i].getLabel()|| name == ("#" +(i+1))) {
                            rows.push(allFields[i]);
                            break;
                        }
                    }
                }
                var html = HtmlUtil.openTag("table",["border", "1px" ,"bordercolor","#ccc","class","display-stats","cellspacing","1","cellpadding","5"]);
                var uniques = {};
                var seen = {};
                for(var j=0;j<cols.length;j++) {
                    var col = cols[j];
                    var key = col.getLabel();
                    for(var rowIdx=1;rowIdx<dataList.length;rowIdx++) {
                        var tuple = this.getDataValues(dataList[rowIdx]);
                        var colValue = tuple[col.getIndex()];
                        if (!(key in uniques)) {
                            uniques[key] = [];
                        }
                        var list = uniques[key];
                        if (list.indexOf(colValue)<0) {
                            console.log(colValue);
                            list.push(colValue);
                        }
                    }
                }

                for(key in uniques) {
                    uniques[key].sort();
                    console.log(uniques[key]);
                }


                for(var j=0;j<cols.length;j++) {
                    var col = cols[j];
                    for(var rowIdx=1;rowIdx<dataList.length;rowIdx++) {
                        var tuple = this.getDataValues(dataList[rowIdx]);
                        //                        var colValue = tuple;
                        //html += HtmlUtil.tr([],HtmlUtil.tds(["class","display-stats-header","align","center"],["","Min","Max","Total","Average"]));
                        for(var i=0;i<rows.length;i++) {
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
    var SUPER;
    var ID_BOTTOM = "bottom";
    $.extend(this, {
            colorTable:"red_white_blue",
            colorByMin:"-1",
            colorByMax:"1",
                });

    RamaddaUtil.inherit(this, SUPER = new RamaddaFieldsDisplay(displayManager, id, DISPLAY_CORRELATION, properties));
    addRamaddaDisplay(this);

    RamaddaUtil.defineMembers(this, {
            "map-display": false,
            needsData: function() {
                return true;
            },
            getMenuItems: function(menuItems) {
                SUPER.getMenuItems.call(this,menuItems);
                var get = this.getGet();
                var tmp = HtmlUtil.formTable();
                var colorTable = this.getColorTableName();
                var ct = "<select id=" + this.getDomId("colortable")+">";
                for(table in Utils.ColorTables) {
                    if(table == colorTable)
                        ct+="<option selected>"+ table+"</option>";
                    else
                        ct+="<option>"+ table+"</option>";
                }
                ct+= "</select>";

                tmp += HtmlUtil.formEntry("Color Bar:",ct);
                                          
                tmp += HtmlUtil.formEntry("Color By Range:", HtmlUtil.input("", this.colorByMin, ["size","7",ATTR_ID,  this.getDomId("colorbymin")]) + " - " +
                                          HtmlUtil.input("", this.colorByMax, ["size","7",ATTR_ID,  this.getDomId("colorbymax")]));
                tmp += "</table>";
                menuItems.push(tmp);
            },
           initDialog: function() {
                SUPER.initDialog.call(this);
                var _this  = this;
                var updateFunc  = function() {
                    _this.colorByMin = _this.jq("colorbymin").val();
                    _this.colorByMax = _this.jq("colorbymax").val();
                    _this.updateUI();
                    
                };
                var func2  = function() {
                    _this.colorTable = _this.jq("colortable").val();
                    _this.updateUI();
                    
                };
                this.jq("colorbymin").blur(updateFunc);
                this.jq("colorbymax").blur(updateFunc);
                this.jq("colortable").change(func2);
        },

            handleEventPointDataLoaded : function(source, pointData) {
                if(!this.needsData()) {
                    if(this.dataCollection == null) {
                        this.dataCollection =  source.dataCollection;
                        this.updateUI();
                    }
                }
            },
            fieldSelectionChanged: function() {
                this.updateUI();
            },
            updateUI: function(pointData) {
                SUPER.updateUI.call(this,pointData);
                if(!this.hasData()) {
                    this.setContents(this.getLoadingMessage());
                    return;
                }
                var dataList = this.getStandardData(null,{includeIndex: false});
                var allFields =  this.dataCollection.getList()[0].getRecordFields();
                var fields = this.getSelectedFields([]);
                if(fields.length==0) fields = allFields;
                var html = HtmlUtil.openTag("table",["border", "0" ,"class","display-correlation"]);
                html+="<tr valign=bottom><td class=display-heading>&nbsp;</td>";
                for(var fieldIdx=0;fieldIdx<fields.length;fieldIdx++) {
                    var field1 = fields[fieldIdx];
                    if(!field1.isFieldNumeric() || field1.isFieldGeo()) continue;
                    html+= "<td align=center class=top-heading>" + HtmlUtil.tag("div",["class","top-heading"],field1.getLabel())+"</td>";
                }
                html+="</tr>";

                var colors = null;
                colorByMin = parseFloat(this.colorByMin);
                colorByMax = parseFloat(this.colorByMax);
                colors = this.getColorTable();
                var colCnt = 0;
                for(var fieldIdx1=0;fieldIdx1<fields.length;fieldIdx1++) {
                    var field1 = fields[fieldIdx1];
                    if(!field1.isFieldNumeric() || field1.isFieldGeo())  continue;
                    colCnt++;
                    html+="<tr><td>" + HtmlUtil.tag("div",["class","side-heading"], field1.getLabel().replace(/ /g,"&nbsp;")) +"</td>";
                    var rowName = field1.getLabel();
                    for(var fieldIdx2=0;fieldIdx2<fields.length;fieldIdx2++) {
                        var field2 = fields[fieldIdx2];
                        if(!field2.isFieldNumeric() || field2.isFieldGeo()) continue;
                        var colName = field2.getLabel();
                        var t1=0;
                        var t2=0;
                        var cnt=0;

                        for(var rowIdx=1;rowIdx<dataList.length;rowIdx++) {
                            var tuple = this.getDataValues(dataList[rowIdx]);
                            var v1  = tuple[field1.getIndex()];
                            var v2  = tuple[field2.getIndex()];
                            t1+=v1;
                            t2+=v2;
                            cnt++;
                        }
                        var avg1 = t1/cnt;
                        var avg2 = t2/cnt;
                        var sum1 = 0;
                        var sum2 = 0;
                        var sum3 = 0;
                        for(var rowIdx=1;rowIdx<dataList.length;rowIdx++) {
                            var tuple = this.getDataValues(dataList[rowIdx]);
                            var v1  = tuple[field1.getIndex()];
                            var v2  = tuple[field2.getIndex()];
                            sum1+= (v1-avg1)*(v2-avg2);
                            sum2+= (v1-avg1)*(v1-avg1);
                            sum3+= (v2-avg2)*(v2-avg2);
                        }                        
                        r = sum1/Math.sqrt(sum2*sum3);

                        var style="";
                        if(colors!=null) {
                            var percent = (r- colorByMin)/(colorByMax-colorByMin);
                            var index = parseInt(percent*colors.length);
                            if(index>=colors.length) index = colors.length-1;
                            else if(index<0) index = 0;
                            style = "background-color:" + colors[index];
                        }
                        html+="<td align=right style=\"" + style +"\">" + HtmlUtil.tag("div",["class","display-correlation-element", "title","&rho;(" + rowName +"," + colName+")"], r.toFixed(3)) +"</td>";
                    }
                    html+="</tr>";
                }
                html+="<tr><td></td><td colspan = " + colCnt+">" + HtmlUtil.div(["id",this.getDomId(ID_BOTTOM)],"") +"</td></tr>";
                html += "</table>";
                this.setContents(html);
                this.displayColorTable(ID_BOTTOM, colorByMin, colorByMax);
                this.initTooltip();

            },
        });
}


function RamaddaHeatmapDisplay(displayManager, id, properties) {
    var SUPER;
    $.extend(this, {
            colorTable:"red_white_blue",
                });

    RamaddaUtil.inherit(this, SUPER = new RamaddaFieldsDisplay(displayManager, id, DISPLAY_HEATMAP, properties));
    addRamaddaDisplay(this);

    RamaddaUtil.defineMembers(this, {
            "map-display": false,
            needsData: function() {
                return true;
            },
            getMenuItems: function(menuItems) {
                SUPER.getMenuItems.call(this,menuItems);
                var get = this.getGet();
                var tmp = HtmlUtil.formTable();
                var colorTable = this.getColorTableName();
                var ct = "<select id=" + this.getDomId("colortable")+">";
                for(table in Utils.ColorTable) {
                    if(table == colorTabler)
                        ct+="<option selected>"+ table+"</option>";
                    else
                        ct+="<option>"+ table+"</option>";
                }
                ct+= "</select>";

                tmp += HtmlUtil.formEntry("Color Table:",ct);
                                          
                tmp += HtmlUtil.formEntry("Color By Range:", HtmlUtil.input("", this.colorByMin, ["size","7",ATTR_ID,  this.getDomId("colorbymin")]) + " - " +
                                          HtmlUtil.input("", this.colorByMax, ["size","7",ATTR_ID,  this.getDomId("colorbymax")]));
                tmp += "</table>";
                menuItems.push(tmp);
            },
           initDialog: function() {
                SUPER.initDialog.call(this);
                var _this  = this;
                var updateFunc  = function() {
                    _this.colorByMin = _this.jq("colorbymin").val();
                    _this.colorByMax = _this.jq("colorbymax").val();
                    _this.updateUI();
                    
                };
                var func2  = function() {
                    _this.colorTable = _this.jq("colortable").val();
                    _this.updateUI();
                    
                };
                this.jq("colorbymin").blur(updateFunc);
                this.jq("colorbymax").blur(updateFunc);
                this.jq("colortable").change(func2);
        },

            handleEventPointDataLoaded : function(source, pointData) {
                if(!this.needsData()) {
                    if(this.dataCollection == null) {
                        this.dataCollection =  source.dataCollection;
                        this.updateUI();
                    }
                }
            },
            fieldSelectionChanged: function() {
                this.updateUI();
            },
            getDimensionsStyle: function() {
                var height = this.getProperty("height",-1);
                if(height>0) {
                    return  " height:" + height +"px; " + " max-height:" + height +"px; overflow-y: auto;";
                }
                return "";
            },
            updateUI: function(pointData) {
                var _this = this;
                if(!haveGoogleChartsLoaded ()) {
                    var func = function() {
                        _this.updateUI();
                    }
                    this.setContents(this.getLoadingMessage());
                    setTimeout(func,1000);
                    return;
                }

                SUPER.updateUI.call(this,pointData);
                if(!this.hasData()) {
                    this.setContents(this.getLoadingMessage());
                    return;
                }
                var dataList = this.getStandardData(null,{includeIndex: true});
                var header = this.getDataValues(dataList[0]);
                var showIndex = this.getProperty("showIndex",true);
                var showValue = this.getProperty("showValue",true);
                var textColor = this.getProperty("textColor","black");

                var cellHeight = this.getProperty("cellHeight",null);
                var extraTdStyle = "";
                if(this.getProperty("showBorder","false") == "true") {
                    extraTdStyle = "border-bottom:1px #666 solid;";
                }
                var extraCellStyle = "";
                if(cellHeight) 
                    extraCellStyle += "height:" + cellHeight+"px; max-height:" + cellHeight+"px; min-height:" + cellHeight+"px;";
                var allFields =  this.dataCollection.getList()[0].getRecordFields();
                var fields = this.getSelectedFields([]);

                if(fields.length==0) fields = allFields;
                var html = "";
                var colors = null;
                var colorByMin = null;
                var colorByMax = null;
                if(Utils.stringDefined(this.getProperty("colorByMins"))) {
                    colorByMin = [];
                    var c = this.getProperty("colorByMins").split(",");
                    for(var i=0;i<c.length;i++) {
                        colorByMin.push(parseFloat(c[i]));
                    }
                }
                if(Utils.stringDefined(this.getProperty("colorByMaxes"))) {
                    colorByMax = [];
                    var c = this.getProperty("colorByMaxes").split(",");
                    for(var i=0;i<c.length;i++) {
                        colorByMax.push(parseFloat(c[i]));
                    }
                }

                if(Utils.stringDefined(this.getProperty("colorTables"))) {
                    var c = this.getProperty("colorTables").split(",");
                    colors = [];
                    for(var i=0;i<c.length;i++) {
                        var name = c[i];
                        if(name == "none") {
                            colors.push(null);
                            continue;
                        }
                        var ct = Utils.ColorTables[name];
                        //                        console.log("ct:" + name +" " +(ct!=null));
                        colors.push(ct);
                    }
                }  else {
                    colors  = [this.getColorTable()];
                }
                var mins = null;
                var maxs = null;
                for(var rowIdx=1;rowIdx<dataList.length;rowIdx++) {
                    var row = this.getDataValues(dataList[rowIdx]);
                    if(mins==null) {
                        mins = [];
                        maxs = [];
                        for(var colIdx=1;colIdx<row.length;colIdx++) {
                            mins.push(Number.MAX_VALUE);
                            maxs.push(Number.MIN_VALUE);
                        }
                    }

                    for(var colIdx=0;colIdx<fields.length;colIdx++) {
                        var field = fields[colIdx];
                        //Add one to the field index to account for the main time index
                        var index =field.getIndex()+1;
                        if(!field || !field.isFieldNumeric() || field.isFieldGeo())  continue;

                        var value = row[index];
                        if(value == Number.POSITIVE_INFINITY || isNaN(value) || !Utils.isNumber(value) ||!Utils.isDefined(value) || value == null) {
                            continue;
                        }
                        mins[colIdx] = Math.min(mins[colIdx], value);
                        maxs[colIdx] = Math.max(maxs[colIdx], value);
                    }
                }

                html += HtmlUtil.openTag("table",["border", "0" ,"class","display-heatmap"]);
                html+="<tr valign=bottom>";
                if(showIndex) {
                    html+= "<td align=center class=top-heading>" + HtmlUtil.tag("div",["class","top-heading"],header[0])+"</td>";
                }
                for(var fieldIdx=0;fieldIdx<fields.length;fieldIdx++) {
                    var field = fields[fieldIdx];
                    if((!field.isFieldNumeric() || field.isFieldGeo())) continue;
                    html+= "<td align=center class=top-heading>" + HtmlUtil.tag("div",["class","top-heading"],field.getLabel())+"</td>";
                }
                html+="</tr>\n";




                for(var rowIdx=1;rowIdx<dataList.length;rowIdx++) {
                    var row = this.getDataValues(dataList[rowIdx]);
                    var index = row[0];
                    //check if its a date
                    if(index.f) {
                        index = index.f;
                    }
                    var rowLabel = index;
                    html+="<tr valign='center'>\n";
                    if(showIndex) {
                        //                        html+="<td>" + HtmlUtil.tag("div",[HtmlUtil.attr("class","side-heading")+ extraCellStyle], rowLabel) +"</td>";
                        html+=HtmlUtil.td(["class","side-heading", "style",extraCellStyle], rowLabel);
                    }
                    var colCnt = 0;
                    for(var colIdx=0;colIdx<fields.length;colIdx++) {
                        var field = fields[colIdx];
                        //Add one to the field index to account for the main time index
                        var index =field.getIndex()+1;
                        if(!field || !field.isFieldNumeric() || field.isFieldGeo())  continue;
                        var style="";
                        var value = row[index];
                        var min = mins[colIdx];
                        var max = maxs[colIdx];
                        if(colorByMin && colCnt<colorByMin.length) 
                            min = colorByMin[colCnt];
                        if(colorByMax && colCnt<colorByMax.length) 
                            max = colorByMax[colCnt];


                        var ok = min!=max && !(value == Number.POSITIVE_INFINITY || isNaN(value) || !Utils.isNumber(value) ||!Utils.isDefined(value) || value == null);
                        var title = header[0] +": " +rowLabel +" - " +field.getLabel()+": " +value;
                        if(ok && colors!=null) {
                            var ct  = colors[Math.min(colCnt,colors.length-1)];
                            if(ct) {
                                var percent = (value- min)/(max-min);
                                var ctIndex = parseInt(percent*ct.length);
                                if(ctIndex>=ct.length) ctIndex = ct.length-1;
                                else if(ctIndex<0) ctIndex = 0;
                                style = "background-color:" + ct[ctIndex]+";";
                            }
                        }
                        var number;
                        if(!ok) {
                            number = "-";
                        } else {
                            number = Utils.formatNumber(value)
                        }
                        if(!showValue) number ="";
                        html+=HtmlUtil.td(["valign","center", "align","right", "style",style+extraCellStyle+extraTdStyle, "class", "display-heatmap-cell"], HtmlUtil.div(["title",title,"style",extraCellStyle+"color:" + textColor],number));
                        colCnt++;
                    }
                    html+="</tr>";
                }
                html += "</table>";
                this.setContents(html);
                this.initTooltip();

            },
        });
}


/**
Copyright 2008-2015 Geode Systems LLC
*/



//Note: I put all of the chart definitions together at the top so one can see everything that is available here
var DISPLAY_D3_GLIDER_CROSS_SECTION = "GliderCrossSection";
var DISPLAY_D3_PROFILE = "profile";
var DISPLAY_D3_LINECHART = "D3LineChart";

//Note: Added requiresData and category
addGlobalDisplayType({type: DISPLAY_D3_LINECHART, forUser:false, label:"D3 LineChart",requiresData:true,category:"Charts"});
addGlobalDisplayType({type: DISPLAY_D3_PROFILE, forUser:false, label:"Profile",requiresData:true,category:"Charts"});
addGlobalDisplayType({type:DISPLAY_D3_GLIDER_CROSS_SECTION , forUser:false, label:"Glider cross section",requiresData:true,category:"Charts"});

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
    foo:"bar",
    getAxis: function(axisType,range) {
        var axis;
        if (axisType == FIELD_TIME) {
            axis = d3.time.scale().range(range);
        } else {
            axis = d3.scale.linear().range(range);
        }
        return axis;
    }, 
    getDataValue: function(axis,record,index) {
        var data;
        if(axis.fieldIdx>=0){
            data = record.getData()[axis.fieldIdx];
        } else {	
            switch(axis.type) {
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


        if(axis.reverse==true) {
            return -1*data;
        } else {
            return data;
        }
    }, 
// This will be the default but we can add more colorscales
    getColorFromColorBar: function(value,range) {
	var colors = ["#00008B","#0000CD","#0000FF","#00FFFF","#7CFC00","#FFFF00","#FFA500","#FF4500","#FF0000","#8B0000"];
	var colorScale = d3.scale.linear()
        .domain([0, colors.length-1])
        .range(range);
	
	var colorScaler = d3.scale.linear()
        .range(colors)
        .domain(d3.range(0,colors.length).map(colorScale));
	
	color = colorScaler(value);
	return color;
    },
// This is for the path lines the previous function for generic ones. 
    addColorBar: function(svg,colors,colorSpacing, displayWidth) {
        //Note: this originally had this.displayWidth which was undefined
        var colorBar = svg.append("g")
        .attr({
                "id"        : "colorBarG",
                "transform" : "translate(" + (displayWidth-40) + ",0)"
            });

        colorBar.append("g")
        .append("defs")
        .append("linearGradient")
        .attr({
                id : "colorBarGradient",
                    x1 : "0%",
                    y1 : "100%",
                    x2 : "0%",
                    y2 : "0%"
                    })
        .selectAll("stop")
        .data(colors)
        .enter()
        .append("stop")
        .attr({
                "offset": function(d,i){return colorSpacing * (i) + "%"},
                    "stop-color":function(d,i){return colors[i]},
			"stop-opacity":1
                            });
	
        return colorBar;
    }
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
                var height = this.getProperty("innerHeight",300);
                var margin = {top: 20, right: 50, bottom: 30, left: 50};
                var divStyle = 
                    "height:" + height +"px;" +
                    "width:" + width +"px;";
                var html = HtmlUtil.div([ATTR_ID, this.getDomId(ID_SVG),ATTR_STYLE,divStyle],"");
                this.setContents(html);

                // To create dynamic size of the div
                this.displayHeight = parseInt((d3.select("#"+this.getDomId(ID_SVG)).style("height")).split("px")[0])-margin.top-margin.bottom;//this.getProperty("height",300);//d3.select("#"+this.getDomId(ID_SVG)).style("height");//
                this.displayWidth  = parseInt((d3.select("#"+this.getDomId(ID_SVG)).style("width")).split("px")[0])-margin.left-margin.right;//this.getProperty("width",600);//d3.select("#"+this.getDomId(ID_SVG)).style("width");//
                	
                //                console.log("WxH:" + this.displayHeight +" " + this.displayWidth);

                // To solve the problem with the classess within the class
                var myThis = this;
                var zoom = d3.behavior.zoom()
                    .on("zoom", function() {myThis.zoomBehaviour()});
                this.zoom = zoom;
                this.svg = d3.select("#" + this.getDomId(ID_SVG)).append("svg")
                    .attr("width", this.displayWidth + margin.left + margin.right)
                    .attr("height", this.displayHeight + margin.top + margin.bottom)
                    .attr(ATTR_CLASS,"D3graph")
                    .call(zoom)
                    .on("click", function(){myThis.click(d3.event)})
                    .on("dblclick", function(){myThis.dbclick(d3.event)})
                    .append("g")
                    .attr("transform", "translate(" + margin.left + "," + margin.top + ")");
                
                // Define the Axis
                // 100 pixels for the legend... lets see if we keep it
                this.x = D3Util.getAxis(properties.graph.axis.x.type,[0, this.displayWidth-100]);
                this.y = D3Util.getAxis(properties.graph.axis.y.type,[this.displayHeight,0]);
		
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
                var colors = ["#00008B","#0000CD","#0000FF","#00FFFF","#7CFC00","#FFFF00","#FFA500","#FF4500","#FF0000","#8B0000"];

                var colorSpacing = 100 / ( colors.length - 1 );

                var colorBar = D3Util.addColorBar(this.svg,colors,colorSpacing,this.displayWidth);
                this.color = d3.scale.category10();
                this.updateUI();
            },
            needsData: function() {return true;},
            initDialog: function() {
                this.addFieldsCheckboxes();
            },
            getDialogContents: function() {
                var height = this.getProperty(PROP_HEIGHT,"400");
                var html  =  HtmlUtil.div([ATTR_ID,  this.getDomId(ID_FIELDS),ATTR_CLASS, "display-fields",]);
                html +=  SUPER.getDialogContents.apply(this);
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
                if(!this.hasData()) {
                    //this.writeHtml(ID_SVG, HtmlUtil.div([ATTR_CLASS,"display-message"], this.getLoadingMessage()));
                    return;
                }
                test= this;
                var selectedFields = this.getSelectedFields();
                if(selectedFields.length==0) {
                    //this.writeHtml(ID_SVG, "No fields selected");
                    return;
                }
                this.addFieldsCheckboxes();
                pointData = this.getData();
                if(pointData == null) {
                    //this.writeHtml(ID_SVG, "No data");
                    console.log("no data");
                    return;
                }
				
                var fields = pointData.getNumericFields();
                var records = pointData.getRecords();
                var ranges =  RecordUtil.getRanges(fields,records);
                var elevationRange =  RecordUtil.getElevationRange(fields,records);
                var offset = (elevationRange[1] - elevationRange[0])*0.05;
                // To be used inside a function we can use this.x inside them so we extract as variables. 
                var x = this.x;
                var y = this.y;
                var color = this.color;
                var axis = properties.graph.axis;

                if(onlyZoom){
                    this.zoom.x(this.x);
                    this.zoom.y(this.y);
                } else {
                    // Update axis for the zoom and other changes
                    this.x.domain(d3.extent(records, function(d) { return D3Util.getDataValue(axis.x,d,selectedFields[0].getIndex()); }));
                    // the y domain depends on the first selected element I have to think about it.
                    this.y.domain(d3.extent(records, function(d) { return D3Util.getDataValue(axis.y,d,selectedFields[0].getIndex()); }));
                    
                    this.zoom.x(this.x);
                    this.zoom.y(this.y);
                }
				
                this.svg.selectAll(".y.axis").call(this.yAxis);
                this.svg.selectAll(".x.axis").call(this.xAxis);
				
                // Remove previous lines
                this.svg.selectAll(".line").remove();
                this.svg.selectAll(".legendElement").remove();

                var myThis=this;
                for(var fieldIdx=0;fieldIdx<selectedFields.length;fieldIdx++) {
                    var dataIndex=selectedFields[fieldIdx].getIndex();
                    var range = ranges[dataIndex];
                    // Plot line for the values
                    var line = d3.svg.line()
                        .x(function(d) { return x( D3Util.getDataValue(axis.x,d,selectedFields[fieldIdx].getIndex())); })
                        .y(function(d) { return y( D3Util.getDataValue(axis.y,d,selectedFields[fieldIdx].getIndex())); });
                    
                    displayLine = this.svg.append("path")
                        .datum(records)
                        .attr(ATTR_CLASS, "line")
                        .attr("d", line)
                        .on("mousemove", function(){myThis.mouseover(d3.event)})
                        .attr("fill", "none")
                        .attr("stroke",function(d){return color(fieldIdx);})
                        .attr("stroke-width","0.5px");

                    if(properties.graph.axis.z==FIELD_SELECTEDFIELD) {
                        displayLine.attr("stroke", "url(#colorBarGradient)");
                    }
					
                    if(properties.graph.derived !=null) {
                        var funcs = properties.graph.derived.split(",");
                        for(funcIdx=0;funcIdx<funcs.length;funcIdx++) {
                            var func  = funcs[funcIdx];
                            if(func==FUNC_MOVINGAVERAGE) {
                                // Plot moving average Line
                                var movingAverageLine = d3.svg.line()
                                    .x(function(d) { return x(D3Util.getDataValue(axis.x,d,selectedFields[fieldIdx].getIndex())); })
                                    .y(function(d,i) {
                                            if (i == 0) {
                                                return _movingSum = D3Util.getDataValue(axis.y,d,selectedFields[fieldIdx].getIndex());
                                            } else {
                                                _movingSum +=  D3Util.getDataValue(axis.y,d,selectedFields[fieldIdx].getIndex());
                                            }
                                            return y(_movingSum / i);
                                        })
                                    .interpolate("basis");
                                this.svg.append("path")
                                    .attr(ATTR_CLASS, "line")
                                    .attr("d", movingAverageLine(records))
                                    .attr("fill","none")
                                    .attr("stroke",function(d){return color(fieldIdx);})
                                    .attr("stroke-width","1.5px")
                                    .attr("viewBox", "50 50 100 100 ")
                                    .style("stroke-dasharray", ("3, 3"));
                            } else {
                                console.log("Error: Unknown derived function:" + func);
                            }                            

                        }
                    }

                    // Legend element Maybe create a function or see how we implement the legend
                    this.svg.append("svg:rect")
                        .attr(ATTR_CLASS,"legendElement")
                        .attr("x", this.displayWidth-100)
                        .attr("y", (50+50*fieldIdx))
                        .attr("stroke", function(d){return color(fieldIdx);})
                        .attr("height", 2)
                        .attr("width", 40);
					   
                    this.svg.append("svg:text")
                        .attr(ATTR_CLASS,"legendElement")
                        .attr("x", this.displayWidth-100+40+10) // position+color rect+padding
                        .attr("y", (55+55*fieldIdx))
                        .attr("stroke", function(d){return color(fieldIdx);})
                        .attr("style","font-size:50%")
                        .text(selectedFields[fieldIdx].getLabel());
                }
            },

            zoomBehaviour: function(){
                // Call redraw with only zoom don't update extent of the data.
                this.updateUI(true);
            },
            //this gets called when an event source has selected a record
            handleEventRecordSelection: function(source, args) {
            },
            mouseover: function() {
                // TO DO
                testing=d3.event;
                console.log("mouseover");
            },
            click: function(event) {
                // TO DO
                console.log("click:" + event);
            },
            dbclick:function(event) {
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
        }};

    properties = $.extend(dfltProperties, properties);
    return new RamaddaD3Display(displayManager,id,properties);
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
                reverse: true},
            x: {
                type: TYPE_VALUE,
                fieldname: FIELD_VALUE,
            },
        }};
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
                reverse: true},
            x: {
                type: TYPE_TIME,
                fieldname: FIELD_TIME,
            },
            z: FIELD_SELECTEDFIELD,
        }};
    properties = $.extend(dfltProperties, properties);

    return new RamaddaD3Display(displayManager,id,properties);
}


/**
Copyright 2008-2015 Geode Systems LLC
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
var ID_SEARCH_SETTINGS= "searchsettings";


var ATTR_ENTRYID = "entryid";
var ID_TREE_LINK = "treelink";

addGlobalDisplayType({type: DISPLAY_ENTRYLIST, label:"Entry List",requiresData:false,category:"Entry Displays"});
addGlobalDisplayType({type: DISPLAY_TESTLIST, label:"Test  List",requiresData:false,category:"Entry Displays"});
addGlobalDisplayType({type: DISPLAY_ENTRYDISPLAY, label:"Entry Display",requiresData:false,category:"Entry Displays"});
addGlobalDisplayType({type: DISPLAY_ENTRY_GALLERY, label:"Entry Gallery",requiresData:false,category:"Entry Displays"});
addGlobalDisplayType({type: DISPLAY_ENTRY_GRID, label:"Entry Date Grid",requiresData:false,category:"Entry Displays"});
//addGlobalDisplayType({type: DISPLAY_OPERANDS, label:"Operands",requiresData:false,category:"Entry Displays"});
addGlobalDisplayType({type: DISPLAY_METADATA, label:"Metadata Table",requiresData:false,category:"Entry Displays"});

//addGlobalDisplayType({type: DISPLAY_TIMELINE, label:"Timeline",requiresData:false,category:"Test"});


function RamaddaEntryDisplay(displayManager, id, type, properties) {
     var SUPER;

     RamaddaUtil.inherit(this, SUPER = new RamaddaDisplay(displayManager, id, type, properties));

     this.ramaddas = new Array();
     var repos = this.getProperty("repositories",this.getProperty("repos",null));
     if(repos != null) {
         var toks = repos.split(",");
         //OpenSearch;http://adasd..asdasdas.dasdas.,
         for(var i=0;i<toks.length;i++) {
             var tok = toks[i];
             tok = tok.trim();
             this.ramaddas.push(getRamadda(tok));
         }
         if(this.ramaddas.length>0) {
             var container = new RepositoryContainer("all", "All entries");
             addRepository(container);
             for(var i=0;i<this.ramaddas.length;i++) {
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
                 if(this.providers!=null) {
                     var provider = this.searchSettings.provider;
                     var fromSelect =  this.jq(ID_PROVIDERS).val();
                     if(fromSelect !=null) {
                         provider = fromSelect;
                     } 
                     this.searchSettings.provider = provider;
                 }
                 return this.searchSettings;
             },
            getEntries: function() {
                if(this.entryList == null) return [];
                return  this.entryList.getEntries();
            },

        });
     if(properties.entryType!=null) {
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
            showSearchSettings: true,            
            showEntries: true,
            showType: true,           
            doSearch: true,
            formOpen: true,
            fullForm: true,            
            showMetadata: true,
            showToggle:true,
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

    
    if(this.showMetadata && this.showSearchSettings) {
        var metadataTypesAttr= this.getProperty("metadataTypes","enum_tag:Tag");
        //look for type:value:label, or type:label,
        var toks  = metadataTypesAttr.split(",");
        for(var i=0;i<toks.length;i++) {
            var type = toks[i];
            var label = type;
            var value = null;
            var subToks  = type.split(":");
            if(subToks.length>1) {
                type = subToks[0];
                if(subToks.length>=3) {
                    value = subToks[1];
                    label  = subToks[2];
                } else {
                    label  = subToks[1];
                }
            }
            this.metadataTypeList.push(new MetadataType(type, label,value));
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
                var footer =  this.getFooter();
                if(!this.getProperty("showFooter", true)) {
                    footer = "";
                }
                displayDebug  =false;
                var entriesDivAttrs = [ATTR_ID,this.getDomId(ID_ENTRIES),ATTR_CLASS,this.getClass("content")];
                var innerHeight = this.getProperty("innerHeight",null);
                if(innerHeight == null) {
                    innerHeight = this.getProperty("entriesHeight",null);
                }
                if(innerHeight!=null) {
                    entriesDivAttrs.push(ATTR_STYLE);
                    entriesDivAttrs.push("margin: 0px; padding: 0px;  min-height:" + innerHeight +"px; max-height:" + innerHeight +"px; overflow-y: auto;");
                }
                var resultsDiv = "";
                if(this.getProperty("showHeader", true)) {
                    resultsDiv = HtmlUtil.div([ATTR_CLASS,"display-entries-results", ATTR_ID,this.getDomId(ID_RESULTS)],"&nbsp;"); 
                }

                var entriesDiv = 
                    resultsDiv +
                    HtmlUtil.div(entriesDivAttrs, this.getLoadingMessage());
                

                if(horizontal) {
                    html += HtmlUtil.openTag(TAG_DIV,["class","row"]);
                    var entriesAttrs = ["class","col-md-12"];
                    if(this.showForm) {
                        var attrs = [];
                        if(this.formWidth === "") {
                            attrs = [];
                        } else if(this.formWidth!=0) {
                            attrs = [ATTR_WIDTH,this.formWidth];
                        }
                        html += HtmlUtil.tag(TAG_DIV,["class","col-md-4"],this.makeSearchForm());
                        entriesAttrs = ["class","col-md-8"];
                    }
                    if(this.showEntries) {
                        var attrs = [ATTR_WIDTH,"75%"];
                        if(this.entriesWidth === "") {
                            attrs = [];
                        }  else if(this.entriesWidth!=0) {
                            attrs = [ATTR_WIDTH,this.entriesWidth];
                        }
                        html += HtmlUtil.tag(TAG_DIV,entriesAttrs,entriesDiv);
                    }
                    html += HtmlUtil.closeTag("row");

                    html += HtmlUtil.openTag(TAG_DIV,["class","row"]);
                    if(this.showForm) {
                        html += HtmlUtil.tag(TAG_DIV,["class","col-md-6"],"");
                    }
                    if(this.showEntries) {
                        if(this.getProperty("showFooter", true)) {
                            html += HtmlUtil.tag(TAG_DIV,["class","col-md-6"],footer);
                        }
                    }
                    html += HtmlUtil.closeTag(TAG_DIV);
                } else {
                    if(this.showForm) {
                        html += this.makeSearchForm();
                    }
                    if(this.showEntries) {
                        html += entriesDiv;
                        html += footer;
                    }
                }
                html += HtmlUtil.div([ATTR_CLASS,"display-entry-popup", ATTR_ID,this.getDomId(ID_DETAILS)],"&nbsp;");
                return html;
            },
            initDisplay: function() {
                var theDisplay  = this;


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

                this.jq(ID_FORM).submit(function( event ) {
                        theDisplay.submitSearchForm();
                        event.preventDefault();
                    });


                this.addTypes(this.entryTypes);
                for(var i=0;i<this.metadataTypeList.length;i++) {
                    var type  = this.metadataTypeList[i];
                    this.addMetadata(type, null);
                }
                if(!this.haveSearched) {
                    if(this.doSearch) {
                        this.submitSearchForm();
                    }
                }
            },
            showEntryDetails: function(event, entryId, src,leftAlign) {
                if(true) return;
                var entry = this.getEntry(entryId);
                var popupId = "#"+ this.getDomId(ID_DETAILS+ entryId);
                if(this.currentPopupEntry ==  entry) {
                    this.hideEntryDetails(entryId);
                    return;
                }
                var myloc = 'right top';
                var atloc = 'right bottom';
                if(leftAlign) {
                    myloc = 'left top';
                    atloc = 'left bottom';
                }
                this.currentPopupEntry = entry;
                if(src == null) src =  this.getDomId("entry_" + entry.getIdForDom());
                var close  = HtmlUtil.onClick(this.getGet()+ ".hideEntryDetails('" + entryId +"');",
                                              HtmlUtil.image(ramaddaBaseUrl +"/icons/close.gif"));
                
                var contents = this.getEntryHtml(entry, {headerRight:close});
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
                var left = "Showing " + (this.searchSettings.skip+1) +"-" +(this.searchSettings.skip+Math.min(this.searchSettings.max, entries.length));
                var nextPrev = [];
                var lessMore = [];
                if(this.searchSettings.skip>0) {
                    nextPrev.push(HtmlUtil.onClick(this.getGet()+".loadPrevUrl();", "Previous",[ATTR_CLASS,"display-link"]));
                }
                var addMore = false;
                if(entries.length == this.searchSettings.getMax()) {
                    nextPrev.push(HtmlUtil.onClick(this.getGet()+".loadNextUrl();", "Next",[ATTR_CLASS,"display-link"]));
                    addMore = true;
                }

                lessMore.push(HtmlUtil.onClick(this.getGet()+".loadLess();", HtmlUtil.image(ramaddaBaseUrl +"/icons/minus-small-white.png",[ATTR_ALT, "View less", ATTR_TITLE, "View less", "border","0"]),[ATTR_CLASS,"display-link"]));
                if(addMore) {
                    lessMore.push(HtmlUtil.onClick(this.getGet()+".loadMore();", HtmlUtil.image(ramaddaBaseUrl +"/icons/plus-small-white.png",[ATTR_ALT, "View more", ATTR_TITLE, "View more","border","0"]),[ATTR_CLASS,"display-link"]));
                }
                var results = "";
                var spacer = "&nbsp;&nbsp;&nbsp;"
                results = left + spacer +
                    HtmlUtil.join(nextPrev,  "&nbsp;") + spacer +
                    HtmlUtil.join(lessMore,  "&nbsp;") ;
                return results;
        },
        submitSearchForm: function() {
              if(this.fixedEntries) {
                  return;
              }
              this.haveSearched = true;
              var settings = this.getSearchSettings();
              settings.text = this.getFieldValue(this.getDomId(ID_TEXT_FIELD), settings.text);

              if(this.textRequired && (settings.text==null || settings.text.trim().length==0)) {
                  this.writeHtml(ID_ENTRIES, "");
                  return;
              }

              if(this.haveTypes) {
                  settings.entryType  = this.getFieldValue(this.getDomId(ID_TYPE_FIELD), settings.entryType);
              }
              settings.clearAndAddType(settings.entryType);
                
              if(this.areaWidget) {
                  this.areaWidget.setSearchSettings(settings);
              }
              if(this.dateRangeWidget) {
                  this.dateRangeWidget.setSearchSettings(settings);
              }
              settings.metadata = [];
              for(var i=0;i<this.metadataTypeList.length;i++) {
                    var metadataType  = this.metadataTypeList[i];
                    var value = metadataType.getValue();
                    if(value == null) {
                        value = this.getFieldValue(this.getMetadataFieldId(metadataType), null);
                    }
                    if(value!=null) {
                        settings.metadata.push({type:metadataType.getType(),value:value});
                    }
                }

                //Call this now because it sets settings


              var theRepository= this.getRamadda()

              if(theRepository.children) {
                  console.log("Searching  multiple ramaddas");
                  this.entryList = new EntryListHolder(theRepository, this);
                  this.multiSearch  = {
                      count: 0,
                  };

                  for(var i =0;i<theRepository.children.length;i++) {
                      var ramadda = theRepository.children[i];
                      var jsonUrl = this.makeSearchUrl(ramadda);
                      this.updateForSearching(jsonUrl);
                      this.entryList.addEntryList(new EntryList(ramadda, jsonUrl, null, false));
                      this.multiSearch.count++;
                  }
                  this.entryList.doSearch(this);
              } else {
                  this.multiSearch  = null;
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
                this.footerRight  = outputs == null?"":"Links: " + HtmlUtil.join(outputs," - "); 
                this.writeHtml(ID_FOOTER_RIGHT, this.footerRight);
                var msg = this.searchMessage;
                if(msg == null) {
                    msg  = this.getRamadda().getSearchMessage();
                }
                var provider = this.getSearchSettings().provider;
                if(provider!=null) {
                    msg = null;
                    if(this.providerMap!=null) {
                        msg  = this.providerMap[provider];
                    }
                    if(msg == null) {
                        msg = provider;
                    }
                    msg = "Searching " + msg;
                }

                this.showMessage(msg, HtmlUtil.div([ATTR_STYLE,"margin:20px;"], this.getWaitImage()));
                this.hideEntryDetails();
            },
            showMessage: function(title, inner) {
                this.writeHtml(ID_RESULTS, title);
                this.writeHtml(ID_ENTRIES, inner);
            },
            prepareToLayout:function() {
                SUPER.prepareToLayout.apply(this);
                this.savedValues = {};
                var cols  = this.getSearchableColumns();
                for(var i =0;i<cols.length;i++) {
                    var col = cols[i];
                    var id = this.getDomId(ID_COLUMN+col.getName());
                    var value = $("#" + id).val();
                    if(value == null || value.length == 0) continue;
                    this.savedValues[id] = value;
                }
            },
            makeSearchUrl: function(repository) {
                var extra = "";
                var cols  = this.getSearchableColumns();
                for(var i =0;i<cols.length;i++) {
                    var col = cols[i];
                    var value = this.jq(ID_COLUMN+col.getName()).val();
                    if(value == null || value.length == 0)continue;
                    extra+= "&" + col.getSearchArg() +"=" + encodeURI(value);
                }
                this.getSearchSettings().setExtra(extra);
                var jsonUrl = repository.getSearchUrl(this.getSearchSettings(), OUTPUT_JSON);
                return jsonUrl;
            },
            makeSearchForm: function() {
                var form =  HtmlUtil.openTag("form",[ATTR_ID,this.getDomId(ID_FORM),"action","#"]);
                var extra = "";
                var text = this.getSearchSettings().text;
                if(text == null) {
                    var args = Utils.getUrlArgs(document.location.search);
                    text  =  args.text;
                }
                if(text == null) {
                    text = "";
                }

                var eg = "search text";
                if(this.eg) {
                    eg = this.eg;
                }
                var textField =  HtmlUtil.input("", text, ["placeholder",eg,ATTR_CLASS, "display-search-input", ATTR_SIZE,"30",ATTR_ID,  this.getDomId(ID_TEXT_FIELD)]);



                var buttonLabel =  HtmlUtil.image(ramaddaBaseUrl +"/icons/magnifier.png",[ATTR_BORDER,"0",ATTR_TITLE,"Search"]);
                var topItems = [];
                var extra = "";
                extra +=   HtmlUtil.formTable();
                if(this.showArea) {
                    this.areaWidget = new AreaWidget(this);
                    extra += HtmlUtil.formEntry("Area:",this.areaWidget.getHtml());
                }

                var searchButton  = HtmlUtil.div([ATTR_ID, this.getDomId(ID_SEARCH),ATTR_CLASS,"display-button"],buttonLabel);


                if(this.ramaddas.length>0) {
                    var select  = HtmlUtil.openTag(TAG_SELECT,[ATTR_ID, this.getDomId(ID_REPOSITORY), ATTR_CLASS,"display-repositories-select"]);
                    var icon = ramaddaBaseUrl +"/icons/favicon.png";
                    for(var i=0;i<this.ramaddas.length;i++) {
                        var ramadda = this.ramaddas[i];
                        var attrs = [ATTR_TITLE,"",ATTR_VALUE,ramadda.getId(),
                                     "data-iconurl",icon];
                        if(this.getRamadda().getId() == ramadda.getId()) {
                            attrs.push("selected");
                            attrs.push(null);
                        }
                        var label = 
                            select += HtmlUtil.tag(TAG_OPTION,attrs,
                                                   ramadda.getName());
                    }
                    select += HtmlUtil.closeTag(TAG_SELECT);
                    topItems.push(select);
                }


                this.providerMap = {};
                if(this.providers!=null) {
                    var options = "";
                    var selected = Utils.getUrlArgs(document.location.search).provider;
                    var toks = this.providers.split(",");
                    var currentCategory = null;
                    var catToBuff = {};
                    var cats = [];

                    for(var i=0;i<toks.length;i++) {
                        var tuple = toks[i].split(":");
                        var id = tuple[0];

                        id  = id.replace(/_COLON_/g,":");
                        var label = tuple.length>1?tuple[1]:id;
                        if(label.length>40) {
                            label =  label.substring(0,39) +"...";
                        }
                        this.providerMap[id] = label;
                        var extraAttrs = "";
                        if(id == selected) {
                            extraAttrs += " selected ";
                        }
                        var category = "";

                        if(tuple.length>3) {
                            category = tuple[3];
                        }
                        var buff = catToBuff[category];
                        if(buff == null) {
                            cats.push(category);
                            catToBuff[category] = "";
                            buff = "";
                        }
                        if(tuple.length>2) {
                            var img = tuple[2];
                            img = img.replace(/\${urlroot}/g,ramaddaBaseUrl);
                            img = img.replace(/\${root}/g,ramaddaBaseUrl);
                            extraAttrs += " data-iconurl=\"" + img + "\" ";
                        }
                        buff+= "<option " + extraAttrs +" value=\"" + id +"\">" + label+"</option>\n";
                        catToBuff[category] = buff;
                    }

                    for(var catIdx=0;catIdx<cats.length;catIdx++) {
                        var category = cats[catIdx];
                        if(category!="")
                            options += "<optgroup label=\"" + category +"\">\n";
                        options += catToBuff[category];
                        if(category!="")
                            options += "</optgroup>";

                    }
                    topItems.push(HtmlUtil.tag("select",["id",this.getDomId(ID_PROVIDERS),ATTR_CLASS,"display-search-providers"],options));
                }


                if(this.showType) {
                    topItems.push(HtmlUtil.span([ATTR_ID, this.getDomId(ID_TYPE_DIV)],HtmlUtil.span([ATTR_CLASS, "display-loading"], "Loading types...")));
                } 

                if(this.showText) {
                    topItems.push(textField);
                }


                

                var horizontal = this.isLayoutHorizontal();
                
                if(horizontal) {
                    var tmp  = HtmlUtil.join(topItems,"<br>");
                    form += "<table><tr valign=top><td>" + searchButton+"</td><td>" + tmp +"</td></tr></table>";
                } else {
                    form += searchButton +" " + HtmlUtil.join(topItems," ");
                }

                if(this.showDate) {
                    this.dateRangeWidget  = new DateRangeWidget(this);
                    extra += HtmlUtil.formEntry("Date Range:",this.dateRangeWidget.getHtml());
                }

                if(this.showMetadata) {
                    for(var i =0;i<this.metadataTypeList.length;i++) {
                        var type  = this.metadataTypeList[i];
                        var value = type.getValue();
                        var metadataSelect;
                        if(value!=null) {
                            metadataSelect= value;
                        } else {
                            metadataSelect= HtmlUtil.tag(TAG_SELECT,[ATTR_ID, this.getMetadataFieldId(type),
                                                                   ATTR_CLASS,"display-metadatalist"],
                                                         HtmlUtil.tag(TAG_OPTION,[ATTR_TITLE,"",ATTR_VALUE,""],
                                                                      NONE));
                        }
                        extra+= HtmlUtil.formEntry(type.getLabel() +":", metadataSelect);
                    }
                }
                extra+= HtmlUtil.closeTag(TAG_TABLE);
                extra +=   HtmlUtil.div([ATTR_ID,this.getDomId(ID_FIELDS)],"");


                if(this.showSearchSettings) {
                    var id = this.getDomId(ID_SEARCH_SETTINGS);
                    if(this.showToggle) {
                        form += HtmlUtil.div([ATTR_CLASS, "display-search-extra", ATTR_ID, id],
                                             HtmlUtil.toggleBlock("Search Settings", HtmlUtil.div([ATTR_CLASS, "display-search-extra-inner"], extra), this.formOpen));
                    } else {
                        form += HtmlUtil.div([ATTR_CLASS, "display-search-extra", ATTR_ID, id],
                                             HtmlUtil.div([ATTR_CLASS, "display-search-extra-inner"], extra));
                    }
                }

                //Hide the real submit button
                form += "<input type=\"submit\" style=\"position:absolute;left:-9999px;width:1px;height:1px;\"/>";
                form += HtmlUtil.closeTag("form");

                return form;

            },
            handleEventMapBoundsChanged: function (source,  args) {
                if(this.areaWidget) {
                    this.areaWidget.handleEventMapBoundsChanged (source,  args);
                }
            },
            typeChanged: function() {
                var settings = this.getSearchSettings();
                settings.skip=0;
                settings.max=50;
                settings.entryType  = this.getFieldValue(this.getDomId(ID_TYPE_FIELD), settings.entryType);
                settings.clearAndAddType(settings.entryType);
                this.addExtraForm();
                this.submitSearchForm();
            },
            addMetadata: function(metadataType, metadata) {
                if(metadata == null) {
                    metadata = this.metadata[metadataType.getType()];
                }
                if(metadata == null) {
                    var theDisplay = this;
                    if(!this.metadataLoading[metadataType.getType()]) {
                        this.metadataLoading[metadataType.getType()] = true;
                        metadata = this.getRamadda().getMetadataCount(metadataType, function(metadataType, metadata) {
                                theDisplay.addMetadata(metadataType, metadata);
                            });
                    }
                }
                if(metadata == null) {
                    return;
                }

                this.metadata[metadataType.getType()] = metadata;


                var select = HtmlUtil.tag(TAG_OPTION,[ATTR_TITLE,"",ATTR_VALUE,""],NONE);
                for(var i =0;i<metadata.length;i++) {
                    var count = metadata[i].count;
                    var value = metadata[i].value;
                    var label = metadata[i].label;
                    var optionAttrs  = [ATTR_VALUE,value,ATTR_CLASS, "display-metadatalist-item"];
                    var selected =  false;
                    if(selected) {
                        optionAttrs.push("selected");
                        optionAttrs.push(null);
                    }
                    select +=  HtmlUtil.tag(TAG_OPTION,optionAttrs, label +" (" + count +")");
                }
                $("#" + this.getMetadataFieldId(metadataType)).html(select);
            },
                
            getMetadataFieldId: function(metadataType) {
                var id = metadataType.getType();
                id = id.replace(".","_");
                return this.getDomId(ID_METADATA_FIELD +id);
            },

            findEntryType: function(typeName) {
                if(this.entryTypes == null) return null;
                for(var i = 0;i< this.entryTypes.length;i++) {
                    var type = this.entryTypes[i];
                    if(type.getId() == typeName) return type;
                }
                return null;
            },
            addTypes: function(newTypes) {
                if(!this.showType) {
                    return;
                }
                if(newTypes == null) {
                    var theDisplay = this;
                    newTypes = this.getRamadda().getEntryTypes(function(ramadda, types) {theDisplay.addTypes(types);});
                }
                if(newTypes == null) {
                    return;
                }

                this.entryTypes = newTypes;

                if(this.types) {
                   var showType = {};
                   var listOfTypes = this.types.split(",");
                    for(var i=0;i<listOfTypes.length;i++) {
                        var type  = listOfTypes[i];
                        showType[type] = true;
                    }
                    var tmp = [];
                    for(var i=0;i<this.entryTypes.length;i++) {
                        var type  = this.entryTypes[i];
                        if(showType[type.getId()]) {
                            tmp.push(type);
                        } else if(type.getCategory()!=null && showType[type.getCategory()]) {
                            tmp.push(type);
                        }
                    }
                    this.entryTypes = tmp;
                }

                this.haveTypes = true;
                var cats =[];
                var catMap = {}; 
                var select =  HtmlUtil.openTag(TAG_SELECT,[ATTR_ID, this.getDomId(ID_TYPE_FIELD),
                                                           ATTR_CLASS,"display-typelist",
                                                           "onchange", this.getGet()+".typeChanged();"]);
                //                HtmlUtil.tag(TAG_OPTION,[ATTR_TITLE,"",ATTR_VALUE,""], " Choose Type "));
                select += HtmlUtil.tag(TAG_OPTION,[ATTR_TITLE,"",ATTR_VALUE,""],"Any Type");

                for(var i = 0;i< this.entryTypes.length;i++) {
                    var type = this.entryTypes[i];
                    //                    var style = " background: URL(" + type.getIcon() +") no-repeat;";
                    var icon =                     type.getIcon();
                    var optionAttrs  = [ATTR_TITLE,type.getLabel(),ATTR_VALUE,type.getId(),ATTR_CLASS, "display-typelist-type",
                                        //                                        ATTR_STYLE, style,
                                        "data-iconurl",icon];
                    var selected =  this.getSearchSettings().hasType(type.getId());
                    if(selected) {
                        optionAttrs.push("selected");
                        optionAttrs.push(null);
                    }
                    var option = HtmlUtil.tag(TAG_OPTION,optionAttrs,  type.getLabel() +" (" + type.getEntryCount() +")");
                    var map = catMap[type.getCategory()];
                    if(map == null) {
                        catMap[type.getCategory()] = HtmlUtil.tag(TAG_OPTION,[ATTR_CLASS, "display-typelist-category", ATTR_TITLE,"",ATTR_VALUE,""],type.getCategory());
                        cats.push(type.getCategory());
                    }
                    catMap[type.getCategory()] += option;

                }
                for(var i in cats) {
                    select += catMap[cats[i]];
                }

                select+=  HtmlUtil.closeTag(TAG_SELECT);
                //                this.writeHtml(ID_TYPE_FIELD, "# " + entryTypes.length);
                //                this.writeHtml(ID_TYPE_FIELD, select);
                this.writeHtml(ID_TYPE_DIV, select);
                this.jq(ID_TYPE_FIELD).selectBoxIt({});
                this.addExtraForm();
           },
           getSelectedType: function() {
                if(this.entryTypes == null) return null;
                for(var i = 0;i< this.entryTypes.length;i++) {
                    var type = this.entryTypes[i];
                    if(type.getId) {
                        if(this.getSearchSettings().hasType(type.getId())) {
                            return type;
                        }
                    }
                }
                return null;
            },
            getSearchableColumns: function() {
                var searchable = [];
                var type = this.getSelectedType();
                if(type==null) {
                    return searchable;
                }
                var cols = type.getColumns();
                if(cols == null) {
                    return searchable;
                }
                for(var i = 0;i< cols.length;i++) {
                    var col = cols[i];
                    if(!col.getCanSearch()) continue;
                    searchable.push(col);
                }
                return searchable;
           },
           addExtraForm: function() {
                if(this.savedValues == null) this.savedValues = {};
                var extra   = "";
                var cols = this.getSearchableColumns();
                for(var i = 0;i< cols.length;i++) {
                    var col = cols[i];
                    if(this.fields!=null && this.fields.indexOf(col.getName())<0) {
                        continue;
                    }


                    if(extra.length==0) {
                        extra+=HtmlUtil.formTable();
                    }
                    var field  ="";
                    var id = this.getDomId(ID_COLUMN+col.getName());
                    var savedValue  = this.savedValues[id];
                    if(savedValue == null) {
                        savedValue = this.jq(ID_COLUMN+col.getName()).val();
                    }
                    if(savedValue == null) savedValue = "";
                    if(col.isEnumeration()) {
                        field  = HtmlUtil.openTag(TAG_SELECT,[ATTR_ID, id, ATTR_CLASS,"display-menu"]);
                        field += HtmlUtil.tag(TAG_OPTION,[ATTR_TITLE,"",ATTR_VALUE,""],
                                              "-- Select --");
                        var values = col.getValues();
                        for(var vidx in values) {
                            var value = values[vidx].value;
                            var label = values[vidx].label;
                            var extraAttr = "";
                            if(value == savedValue) {
                                extraAttr =  " selected ";
                            }
                            field += HtmlUtil.tag(TAG_OPTION,[ATTR_TITLE,label,ATTR_VALUE,value, extraAttr,  null],
                                                  label);
                        }
                        field  += HtmlUtil.closeTag(TAG_SELECT);
                    } else {
                        field = HtmlUtil.input("", savedValue, [ATTR_CLASS,"input", ATTR_SIZE,"15",ATTR_ID,  id]);
                    }
                    extra+= HtmlUtil.formEntry(col.getLabel() +":" ,field + " " + col.getSuffix());

                }
                if(extra.length>0) {
                    extra+=HtmlUtil.closeTag(TAG_TABLE);
                }
                
                this.writeHtml(ID_FIELDS, extra);

                $(".display-menu").selectBoxIt({});


           },
            getEntries: function() {
                if(this.entryList == null) return [];
                return  this.entryList.getEntries();
            },
            loadNextUrl: function() {
                this.getSearchSettings().skip+= this.getSearchSettings().max;
                this.submitSearchForm();
            },
            loadMore: function() {
                this.getSearchSettings().max = this.getSearchSettings().max+=50;
                this.submitSearchForm();
            },
            loadLess: function() {
                var max = this.getSearchSettings().max;
                max = parseInt(0.75*max);
                this.getSearchSettings().max = Math.max(1, max);
                this.submitSearchForm();
            },
            loadPrevUrl: function() {
                this.getSearchSettings().skip = Math.max(0, this.getSearchSettings().skip-this.getSearchSettings().max);
                this.submitSearchForm();
            },
            entryListChanged: function(entryList) {
                this.entryList = entryList;
            }
        });
}


function RamaddaEntrylistDisplay(displayManager, id, properties, theType) {
    var SUPER;
    if(theType == null) {
        theType = DISPLAY_ENTRYLIST;
    }
    RamaddaUtil.inherit(this, SUPER = new RamaddaSearcher(displayManager, id, DISPLAY_ENTRYLIST, properties));
    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
            haveDisplayed: false,
            selectedEntries: [],            
            getSelectedEntries: function() {return this.selectedEntries;},
            initDisplay: function() {
                var _this = this;
                if(this.getIsLayoutFixed() && this.haveDisplayed) {
                    return;
                }
                this.haveDisplayed =true;
                this.createUI();
                this.setContents(this.getDefaultHtml());
                if(this.dateRangeWidget) {
                    this.dateRangeWidget.initHtml();
                }
                SUPER.initDisplay.apply(this);
                if(this.entryList!=null && this.entryList.haveLoaded) {
                    this.entryListChanged(this.entryList);
                }
                this.jq(ID_PROVIDERS).selectBoxIt({});
                this.jq(ID_PROVIDERS).change(function() {
                        _this.providerChanged();
                });
            },
            providerChanged: function() {
                var provider  =  this.jq(ID_PROVIDERS).val();
                if(provider!="this") {
                    this.jq(ID_SEARCH_SETTINGS).hide();
                } else {
                    this.jq(ID_SEARCH_SETTINGS).show();
                }
            },
            getMenuItems: function(menuItems) {
                SUPER.getMenuItems.apply(this,menuItems);
                if(this.getSelectedEntriesFromTree().length>0) {
                    var get = this.getGet();
                    menuItems.push(HtmlUtil.onClick(get+".makeDisplayList();", "Make List"));
                    menuItems.push(HtmlUtil.onClick(get+".makeDisplayGallery();", "Make Gallery"));
                }
            },
            makeDisplayList: function() {
                var entries = this.getSelectedEntriesFromTree();
                if(entries.length==0) {
                    return;
                }
                var props = {
                    selectedEntries: entries,
                    showForm: false,
                    showMenu: true,
                    fixedEntries: true};
                props.entryList =   new EntryList(this.getRamadda(), "", this, false);
                props.entryList.setEntries(entries);
                this.getDisplayManager().createDisplay(DISPLAY_ENTRYLIST, props);
            },
            makeDisplayGallery: function() {
                var entries = this.getSelectedEntriesFromTree();
                if(entries.length==0) {
                    return;
                }

                //xxxx
                var props = {selectedEntries: entries};
                this.getDisplayManager().createDisplay(DISPLAY_ENTRY_GALLERY, props);
            },
            handleEventEntrySelection: function(source, args) {
                this.selectEntry(args.entry, args.selected);
            },
            selectEntry: function(entry, selected) {
                var changed  = false;
                if(selected) {
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
                return  this.getEntriesTree(entries);
            },

            entryListChanged: function(entryList) {
                if(this.multiSearch) {
                    this.multiSearch.count--;
                }
                SUPER.entryListChanged.apply(this,[entryList]);
                var entries = this.entryList.getEntries();

                if(entries.length==0) {
                    this.getSearchSettings().skip=0;
                    this.getSearchSettings().max=50;
                    var msg = "Nothing found";
                    if(this.multiSearch) {
                        if(this.multiSearch.count>0) {
                            msg = "Nothing found so far. Still searching " + this.multiSearch.count +" repositories";
                        } else {
                        }
                    }
                    this.writeHtml(ID_ENTRIES, this.getMessage(msg));
                    this.writeHtml(ID_FOOTER_LEFT,"");
                    this.writeHtml(ID_RESULTS,"&nbsp;");
                    this.getDisplayManager().handleEventEntriesChanged(this, []);
                    return;
                }
                this.writeHtml(ID_RESULTS, this.getResultsHeader(entries));


                var get = this.getGet();
                this.writeHtml(ID_FOOTER_LEFT,"");
                if(this.footerRight!=null) {
                    this.writeHtml(ID_FOOTER_RIGHT, this.footerRight);
                }

                //                var entriesHtml  = this.getEntriesTree(entries);
                var entriesHtml  = this.makeEntriesDisplay(entries);

                var html = "";
                html += HtmlUtil.openTag(TAG_OL,[ATTR_CLASS,this.getClass("list"), ATTR_ID,this.getDomId(ID_LIST)]);
                html += entriesHtml;
                html += HtmlUtil.closeTag(TAG_OL);
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

                return  "Overridden display<br>" + this.getEntriesTree(entries);
            },
     });

}



var  RamaddaListDisplay = RamaddaEntrylistDisplay;



function RamaddaEntrygalleryDisplay(displayManager, id, properties) {
    var ID_GALLERY = "gallery";
    var SUPER;
    RamaddaUtil.inherit(this, SUPER = new RamaddaEntryDisplay(displayManager, id, DISPLAY_ENTRY_GALLERY, properties));
    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
            entries: properties.entries,
            initDisplay: function() {
                var _this =this;
                this.createUI();
                var html =    HtmlUtil.div([ATTR_ID,this.getDomId(ID_GALLERY)],"Gallery");
                this.setContents(html);

                if(this.selectedEntries!=null) {
                    this.jq(ID_GALLERY).html(this.getEntriesGallery(this.selectedEntries));
                    return;
                }
                if(this.entries) {
                    var props = {entries:this.entries};
                    var searchSettings= new EntrySearchSettings(props);
                    var jsonUrl = this.getRamadda().getSearchUrl(searchSettings, OUTPUT_JSON,"BAR");
                    console.log(jsonUrl);
                    var myCallback = {
                        entryListChanged: function(list) {
                            var entries  = list.getEntries();
                            _this.jq(ID_GALLERY).html(_this.getEntriesGallery(entries));
                            $("a.popup_image").fancybox({'titleShow' : false});
                        }
                    };
                    var entryList = new EntryList(this.getRamadda(), jsonUrl, myCallback, true);
                }

                if(this.entryList!=null && this.entryList.haveLoaded) {
                    this.entryListChanged(this.entryList);
                }
            },
            getEntriesGallery:function (entries) {
                var nonImageHtml = "";
                var html = "";
                var imageCnt = 0;
                var imageEntries = [];
                for(var i=0;i<entries.length;i++) {
                    var entry = entries[i];
                    //Don: Right now this just shows all of the images one after the other.
                    //If there is just one image we should just display it
                    //We should do a gallery here if more than 1

                    if(entry.isImage()) {
                        imageEntries.push(entry);
                        var link  =  HtmlUtil.tag(TAG_A,[ATTR_HREF, entry.getEntryUrl()],entry.getName());
                        imageCnt++;
                        html  += HtmlUtil.tag(TAG_IMG,["src", entry.getResourceUrl(), ATTR_WIDTH,"500",ATTR_ID,
                                                  this.getDomId("entry_" + entry.getIdForDom()),
                                                       ATTR_ENTRYID,entry.getId(), ATTR_CLASS,"display-entrygallery-entry"]) +"<br>" +
                            link+"<p>";
                    } else  {
                        var icon = entry.getIconImage([ATTR_TITLE,"View entry"]);
                        var link  =  HtmlUtil.tag(TAG_A,[ATTR_HREF, entry.getEntryUrl()],icon+" "+ entry.getName());
                        nonImageHtml += link +"<br>";
                    }
                }

                if(imageCnt>1) {
                    //Show a  gallery instead
                    var newHtml = "";
                    newHtml+="<div class=\"row\">\n";
                    var columns = parseInt(this.getProperty("columns","3"));
                    var colClass="col-md-" + (12/columns);
                    for(var i=0;i<imageEntries.length;i++) {
                        if(i>=columns) {
                            newHtml+="</div><div class=\"row\">\n";
                        }
                        newHtml+="<div class=" + colClass+">\n";
                        var entry = imageEntries[i];
                        var link  =  HtmlUtil.tag(TAG_A,[ATTR_HREF, entry.getEntryUrl()],entry.getName());
                        //Don: right now I just replicate what I do above
                        var img = HtmlUtil.image(entry.getResourceUrl(), [ATTR_WIDTH,"100%",ATTR_ID,
                                                                          this.getDomId("entry_" + entry.getIdForDom()),
                                                                          ATTR_ENTRYID,entry.getId(), ATTR_CLASS,"display-entrygallery-entry"]);
                        img =  HtmlUtil.href(entry.getResourceUrl(),img,["class","popup_image"]);
                        newHtml += HtmlUtil.div(["class","image-outer"],HtmlUtil.div(["class","image-inner"], img) +
                                               HtmlUtil.div(["class","image-caption"], link));

                        newHtml+="</div>\n";
                    }
                    newHtml+="</div>\n";
                    html = newHtml;
                }


                //append the links to the non image entries
                if(nonImageHtml!="") {
                    if(imageCnt>0) {
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
                var _this =this;
                this.createUI();
                var html =    HtmlUtil.div([ATTR_ID,this.getDomId(ID_CONTENTS)],this.getLoadingMessage("Loading entries..."));
                this.setContents(html);
                if(!this.entryIds) {
                    _this.jq(ID_CONTENTS).html(this.getLoadingMessage("No entries specified"));
                    return;
                }
                var props = {entries:this.entryIds};
                var searchSettings= new EntrySearchSettings(props);
                var jsonUrl = this.getRamadda().getSearchUrl(searchSettings, OUTPUT_JSON,"BAR");
                var myCallback = {
                    entryListChanged: function(list) {
                        _this.entries  = list.getEntries();
                        if(_this.entries.length==0) {
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
                            if(debugMouse)
                                console.log("mouse down");
                            _this.handledClick =false;
                            _this.drag = {
                                dragging:false,
                                x: GuiUtils.getEventX(evt),
                                y: GuiUtils.getEventY(evt),
                                X: {
                                    minDate:_this.axis.X.minDate?_this.axis.X.minDate:_this.minDate,
                                    maxDate:_this.axis.X.maxDate?_this.axis.X.maxDate:_this.maxDate,
                                },
                                Y: {
                                    minDate:_this.axis.Y.minDate?_this.axis.Y.minDate:_this.minDate,
                                    maxDate:_this.axis.Y.maxDate?_this.axis.Y.maxDate:_this.maxDate,
                                }
                            }
                        }
                        var mouseleave = function(evt) {
                            if(debugMouse)
                                console.log("mouse leave");
                            _this.drag = null;
                            _this.handledClick =false;
                        }
                        var mouseup = function(evt) {
                            if(debugMouse)
                                console.log("mouse up");
                            if(_this.drag) {
                                if(_this.drag.dragging) {
                                    if(debugMouse)
                                        console.log("mouse up-was dragging");
                                    _this.handledClick =true;
                                }
                                _this.drag = null;
                            }
                            }
                        var mousemove = function(evt,doX,doY) {
                            if(debugMouse)
                                console.log("mouse move");
                            var drag = _this.drag;
                            if(!drag) return;
                            drag.dragging = true;
                            var x = GuiUtils.getEventX(evt);
                            var deltaX = drag.x-x;
                            var y = GuiUtils.getEventY(evt);
                            var deltaY = drag.y-y;
                            var width = $(this).width();
                            var height = $(this).height();
                            var percentX = (x-drag.x)/width;
                            var percentY = (y-drag.y)/height;
                            var ascX = _this.getXAxisAscending();
                            var ascY = _this.getXAxisAscending();
                            var diffX = (drag.X.maxDate.getTime()-drag.X.minDate.getTime())*percentX;
                            var diffY = (drag.Y.maxDate.getTime()-drag.Y.minDate.getTime())*percentY;

                            if(doX) {
                                _this.axis.X.minDate = new Date(drag.X.minDate.getTime()+((ascX?-1:1)*diffX));
                                _this.axis.X.maxDate = new Date(drag.X.maxDate.getTime()+((ascX?-1:1)*diffX));
                            }
                            if(doY) {
                                _this.axis.Y.minDate = new Date(drag.Y.minDate.getTime()+((ascY?1:-1)*diffY));
                                _this.axis.Y.maxDate = new Date(drag.Y.maxDate.getTime()+((ascY?1:-1)*diffY));
                            }
                            _this.makeGrid(_this.entries);
                        }
                        var mouseclick = function(evt, doX,doY) {
                            if(_this.handledClick) {
                                if(debugMouse)
                                    console.log("mouse click-other click");
                                _this.handledClick =false;
                                return;
                            }
                            if(_this.drag && _this.drag.dragging) {
                                if(debugMouse)
                                    console.log("mouse click-was dragging");
                                _this.drag = null;
                                return;
                            }
                            if(debugMouse)
                                console.log("mouse click");
                            _this.drag = null;
                            var action;
                            if(evt.metaKey || evt.ctrlKey) {
                                action="reset";
                            } else {
                                var zoomOut = evt.shiftKey;
                                if(zoomOut)
                                    action = "zoomout";
                                else
                                    action="zoomin";
                            }
                            _this.doZoom(action,doX,doY);
                        };

                        var mousemoveCanvas = function(evt) {
                            mousemove(evt,true,true);
                        }
                        var mousemoveX = function(evt) {
                            mousemove(evt,true,false);
                        }
                        var mousemoveY = function(evt) {
                            mousemove(evt,false,true);
                        }

                        var mouseclickCanvas = function(evt) {
                            mouseclick(evt,true,true);
                        }
                        var mouseclickX = function(evt) {
                            mouseclick(evt,true,false);
                        }
                        var mouseclickY = function(evt) {
                            mouseclick(evt,false,true);
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
                        HtmlUtil.image(icon_zoom,["class","display-grid-action","title","reset zoom","action","reset"]) +
                        HtmlUtil.image(icon_zoom_in,["class","display-grid-action","title","zoom in","action","zoomin"]) +
                        HtmlUtil.image(icon_zoom_out,["class","display-grid-action","title","zoom out","action","zoomout"]);
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
                var cbx= this.jq(ID_SETTINGS +" :checkbox");
                console.log(cbx.size());
                cbx.click(function() {
                        _this.setProperty($(this).attr("attr"),$(this).is(':checked'));
                        _this.makeGrid(_this.entries);
                    });
                var input= this.jq(ID_SETTINGS +" :input");
                input.blur(function() {
                        _this.setProperty($(this).attr("attr"),$(this).val());
                        _this.makeGrid(_this.entries);
                    });
                input.keypress(function(event){
                        var keycode = (event.keyCode ? event.keyCode : event.which);
                        if(keycode == 13){
                            _this.setProperty($(this).attr("attr"),$(this).val());
                            _this.makeGrid(_this.entries);
                        }});

            },
           getDialogContents: function(tabTitles, tabContents) {
                var height = "600";
                var html  =  "";
                html += HtmlUtil.openTag("div", ["id", this.getDomId(ID_SETTINGS)]);

                html += HtmlUtil.formTable();
                html+=HtmlUtil.formEntry("",
                                         HtmlUtil.checkbox(this.getDomId(ID_SHOW_ICON),
                                                           ["attr",ID_SHOW_ICON],
                                                           this.getProperty(ID_SHOW_ICON,"true")) +" Show Icon"+
                                         "&nbsp;&nbsp;" +
                                         HtmlUtil.checkbox(this.getDomId(ID_SHOW_NAME),
                                                           ["attr",ID_SHOW_NAME],
                                                           this.getProperty(ID_SHOW_NAME,"true")) +" Show Name");
                html += HtmlUtil.formEntry("X-Axis:",
                                         HtmlUtil.checkbox(this.getDomId(ID_XAXIS_ASCENDING),
                                                           ["attr",ID_XAXIS_ASCENDING],
                                                           this.getXAxisAscending()) +" Ascending" +
                                         "&nbsp;&nbsp;" +
                                         HtmlUtil.checkbox(this.getDomId(ID_XAXIS_SCALE),
                                                           ["attr",ID_XAXIS_SCALE],
                                                           this.getXAxisScale()) +" Scale Width");
                html+=HtmlUtil.formEntry("Y-Axis:",
                                         HtmlUtil.checkbox(this.getDomId(ID_YAXIS_ASCENDING),
                                                           ["attr",ID_YAXIS_ASCENDING],
                                                           this.getYAxisAscending()) +" Ascending" +
                                         "&nbsp;&nbsp;" +
                                         HtmlUtil.checkbox(this.getDomId(ID_YAXIS_SCALE),
                                                           ["attr",ID_YAXIS_SCALE],
                                                           this.getYAxisScale()) +" Scale Height");

                html+=HtmlUtil.formEntry("Box Color:",
                                         HtmlUtil.input(this.getDomId(ID_COLOR),
                                                        this.getProperty(ID_COLOR,"lightblue"),
                                                        ["attr",ID_COLOR]));
                                       
                html += HtmlUtil.formTableClose();
                html += HtmlUtil.closeTag("div");
                tabTitles.push("Entry Grid"); 
                tabContents.push(html);
                SUPER.getDialogContents.call(this,tabTitles, tabContents);
            },

            doZoom: function(action, doX, doY) {
                if(!Utils.isDefined(doX)) doX = true;
                if(!Utils.isDefined(doY)) doY = true;
                if(action=="reset") {
                    this.axis.Y.minDate = null;
                    this.axis.Y.maxDate = null;
                    this.axis.X.minDate = null;
                    this.axis.X.maxDate = null;
                } else {
                    var zoomOut = (action=="zoomout");
                    if(doX) {
                        var d1 = this.axis.X.minDate.getTime();
                        var d2 = this.axis.X.maxDate.getTime();
                        var dateRange =  d2- d1;
                        var diff = (zoomOut?1:-1)*dateRange*0.1;
                        this.axis.X.minDate = new Date(d1-diff);
                        this.axis.X.maxDate = new Date(d2+diff);
                    }
                    if(doY) {
                        var d1 = this.axis.Y.minDate.getTime();
                        var d2 = this.axis.Y.maxDate.getTime();
                        var dateRange =  d2- d1;
                        var diff = (zoomOut?1:-1)*dateRange*0.1;
                        this.axis.Y.minDate = new Date(d1-diff);
                        this.axis.Y.maxDate = new Date(d2+diff);
                    }
                }
                this.makeGrid(this.entries);
            },
            initGrid:function (entries) {
                var _this = this;
                var items = this.canvas.find(".display-grid-entry");
                items.click(function(evt){
                        var index = parseInt($(this).attr("index"));
                        entry = entries[index];
                        var url = entry.getEntryUrl();
                        if(_this.urlTemplate) {
                            url = _this.urlTemplate.replace("{url}",url).replace(/{entryid}/g,entry.getId()).replace(/{resource}/g,entry.getResourceUrl());
                        }

                        _this.handledClick =true;
                        _this.drag  = null;
                        window.open(url,"_entry");
                        //                        evt.stopPropagation();
                    });
                items.mouseout(function(){
                        var id = $(this).attr("entryid");
                        if(id) {
                            var other = _this.canvas.find("[entryid='"+ id+"']");
                            other.each(function() {
                                    if($(this).attr("itemtype") == "box") {
                                        $(this).attr("prevcolor",$(this).css("background"));
                                        $(this).css("background",$(this).attr("prevcolor"));
                                    }
                                });
                        }

                        _this.gridPopup.hide();
                    });
                items.mouseover(function(evt){
                        var id = $(this).attr("entryid");
                        if(id) {
                            var other = _this.canvas.find("[entryid='"+ id+"']");
                            other.each(function() {
                                    if($(this).attr("itemtype") == "box") {
                                        $(this).attr("prevcolor",$(this).css("background"));
                                        $(this).css("background","rgba(0,0,255,0.5)");
                                    }
                                });
                        }
                        var x = GuiUtils.getEventX(evt);
                        var index = parseInt($(this).attr("index"));
                        entry = entries[index];
                        var thumb = entry.getThumbnail();
                        var html ="";
                        if(thumb){
                            html = HtmlUtil.image(thumb,["width","300;"])+"<br>";
                        }  else if(entry.isImage()) {
                            html  += HtmlUtil.image(entry.getResourceUrl(), ["width","300"]) +"<br>";
                        }
                        html+= entry.getIconImage() +" " +entry.getName()+"<br>";
                        var start  = entry.getStartDate().getUTCFullYear()+"-" + Utils.padLeft(entry.getStartDate().getUTCMonth()+1,2,"0")+"-" + Utils.padLeft(entry.getStartDate().getUTCDate(),2,"0");
                        var end  = entry.getEndDate().getUTCFullYear()+"-" + Utils.padLeft(entry.getEndDate().getUTCMonth()+1,2,"0")+"-" + Utils.padLeft(entry.getEndDate().getUTCDate(),2,"0");
                        html+="Date: " + start +" - " +end+" UTC";
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
            makeFramework:function (entries) {
                var html = "";
                var mouseInfo = "click:zoom in;shift-click:zoom out;command/ctrl click: reset";
                html += HtmlUtil.openDiv(["class","display-grid","id", this.getDomId(ID_GRID)]);
                html += HtmlUtil.div(["class","display-grid-popup ramadda-popup"],"");
                html += HtmlUtil.openTag("table",["border","0", "class","","cellspacing","0","cellspacing","0","width","100%","style","height:100%;"]);
                html += HtmlUtil.openTag("tr",["valign","bottom"]);
                html += HtmlUtil.tag("td");
                html += HtmlUtil.tag("td",[],HtmlUtil.div(["id",this.getDomId(ID_TOP)],""));
                html += HtmlUtil.closeTag("tr");
                html += HtmlUtil.openTag("tr",["style","height:100%;"]);
                html += HtmlUtil.openTag("td",["style","height:100%;"]);
                html += HtmlUtil.openDiv(["class","display-grid-axis-left ramadda-noselect","id",this.getDomId(ID_AXIS_LEFT)]);
                html += HtmlUtil.closeDiv();
                html += HtmlUtil.closeDiv();
                html += HtmlUtil.closeTag("td");
                html += HtmlUtil.openTag("td",["style","height:" + this.getProperty("height","400")+"px"]);
                html += HtmlUtil.openDiv(["class","display-grid-canvas ramadda-noselect","id", this.getDomId(ID_CANVAS)]);
                html+= HtmlUtil.closeDiv();
                html+= HtmlUtil.closeDiv();
                html+=HtmlUtil.closeTag("td");
                html+=HtmlUtil.closeTag("tr");
                html+=HtmlUtil.openTag("tr",[]);
                html+=HtmlUtil.tag("td",["width","100"],"&nbsp;");
                html+=HtmlUtil.openTag("td",[]);
                html+= HtmlUtil.div(["class","display-grid-axis-bottom ramadda-noselect","title",mouseInfo, "id",this.getDomId(ID_AXIS_BOTTOM)],"");
                html+=HtmlUtil.closeTag("table");
                html+=HtmlUtil.closeTag("td");
                return html;
            },


            getXAxisType: function() {
                return this.getProperty(ID_XAXIS_TYPE,"date");
            },
            getYAxisType: function() {
                return this.getProperty(ID_YAXIS_TYPE,"month");
            },
            getXAxisAscending: function() {
                return this.getProperty(ID_XAXIS_ASCENDING,true);
            },
            getYAxisAscending: function() {
                return this.getProperty(ID_YAXIS_ASCENDING,true);
            },
            getXAxisScale: function() {
                return this.getProperty(ID_XAXIS_SCALE,true);
            },
            getYAxisScale: function() {
                return this.getProperty(ID_YAXIS_SCALE,false);
            },


            makeGrid:function (entries) {
                var showIcon = this.getProperty(ID_SHOW_ICON,true);
                var showName = this.getProperty(ID_SHOW_NAME,true);

                if(!this.minDate) {
                    var minDate= null;
                    var maxDate = null;
                    for(var i=0;i<entries.length;i++) {
                        var entry = entries[i];
                        minDate = minDate==null?entry.getStartDate():(minDate.getTime()>entry.getStartDate().getTime()?entry.getStartDate():minDate);
                        maxDate = maxDate==null?entry.getEndDate():(maxDate.getTime()<entry.getEndDate().getTime()?entry.getEndDate():maxDate);
                    }
                    this.minDate  = new Date(Date.UTC(minDate.getUTCFullYear(),0,1));
                    this.maxDate  = new Date(Date.UTC(maxDate.getUTCFullYear()+1,0,1));
                }

                var axis = {
                    width: this.canvas.width(),
                    height: this.canvas.height(),
                    Y: {
                        vertical:true,
                        axisType:this.getYAxisType(),
                        ascending: this.getYAxisAscending(),
                        scale: this.getYAxisScale(),
                        skip:1,
                        maxTicks:Math.ceil(this.canvas.height()/80),                        
                        minDate:this.minDate,
                        maxDate:this.maxDate,
                        ticks:[],
                        lines:"",
                        html:"",
                        minDate:this.minDate,
                        maxDate:this.maxDate,
                    },
                    X: {
                        vertical:false,
                        axisType:this.getXAxisType(),
                        ascending: this.getXAxisAscending(),
                        scale: this.getXAxisScale(),
                        skip:1,
                        maxTicks:Math.ceil(this.canvas.width()/80),                        
                        minDate:this.minDate,
                        maxDate:this.maxDate,
                        ticks:[],
                        lines:"",
                        html:""
                    }
                }
                if(!this.axis) {
                    this.axis  = axis;
                } else {
                    if(this.axis.X.minDate) {
                        axis.X.minDate = this.axis.X.minDate;
                        axis.X.maxDate = this.axis.X.maxDate;
                    } else {
                        this.axis.X.minDate = axis.X.minDate;
                        this.axis.X.maxDate = axis.X.maxDate;
                    }
                    if(this.axis.Y.minDate) {
                        axis.Y.minDate = this.axis.Y.minDate;
                        axis.Y.maxDate = this.axis.Y.maxDate;
                    } else {
                        this.axis.Y.minDate = axis.Y.minDate;
                        this.axis.Y.maxDate = axis.Y.maxDate;
                    }
                }
 
                if(axis.Y.axisType == "size") {
                    this.calculateSizeAxis(axis.Y);
                } else if(axis.Y.axisType == "date") {
                    this.calculateDateAxis(axis.Y);
                } else {
                    this.calculateMonthAxis(axis.Y);
                }
                for(var i=0;i<axis.Y.ticks.length;i++) {
                    var tick = axis.Y.ticks[i];
                    var style = (axis.Y.ascending?"bottom:":"top:") + tick.percent +"%;";
                    var style = "bottom:"+ tick.percent +"%;";
                    var lineClass =tick.major?"display-grid-hline-major":"display-grid-hline";
                    axis.Y.lines+=HtmlUtil.div(["style",style,"class",lineClass]," ");
                    axis.Y.html+=HtmlUtil.div(["style",style,"class","display-grid-axis-left-tick"],tick.label+" " + HtmlUtil.div(["class","display-grid-htick"],""));
                }

                if(axis.X.axisType == "size") {
                    this.calculateSizeAxis(axis.X);
                } else if(axis.X.axisType == "date") {
                    this.calculateDateAxis(axis.X);
                } else {
                    this.calculateMonthAxis(axis.X);
                }
                for(var i=0;i<axis.X.ticks.length;i++) {
                    var tick = axis.X.ticks[i];
                    if(tick.percent>0)  {
                        var lineClass =tick.major?"display-grid-vline-major":"display-grid-vline";
                        axis.X.lines+=HtmlUtil.div(["style","left:" + tick.percent+"%;", "class",lineClass]," ");
                    }
                    axis.X.html+=HtmlUtil.div(["style","left:" + tick.percent+"%;","class","display-grid-axis-bottom-tick"], HtmlUtil.div(["class","display-grid-vtick"],"")+" " + tick.label);
                }

                var items = "";
                var seen = {};
                for(var i=0;i<entries.length;i++) {
                    var entry = entries[i];
                    var vInfo = this[axis.Y.calculatePercent].call(this,entry,axis.Y);
                    var xInfo = this[axis.X.calculatePercent].call(this,entry,axis.X);
                    if(vInfo.p1<0) {
                        vInfo.p2 = vInfo.p2+vInfo.p1;
                        vInfo.p1=0;
                    }
                    if(vInfo.p1+vInfo.p2>100) {
                        vInfo.p2=100-vInfo.p1;
                    }

                    var style = "";
                    var pos = "";

                    if(axis.X.ascending) {
                        style += "left:"+  xInfo.p1 + "%;";
                        pos += "left:"+  xInfo.p1 + "%;";
                    } else {
                        style += "right:"+  xInfo.p1 + "%;";
                        pos += "left:"+  (100-xInfo.p2) + "%;";
                    }
                    
                    if(axis.X.scale) {
                        if(xInfo.delta>1) {
                            style+="width:" + xInfo.delta+"%;";
                        } else {
                            style+="width:" + this.getProperty("fixedWidth","5") +"px;";
                        }
                    }


                    var namePos = pos;
                    if(axis.Y.ascending) {
                        style+=" bottom:" + vInfo.p2+"%;";
                        pos +=" bottom:" + vInfo.p2+"%;";
                        namePos +=" bottom:" + vInfo.p2+"%;";
                    } else {
                        style+=" top:" + vInfo.p2+"%;";
                        pos+=" top:" + vInfo.p2+"%;";
                        namePos+=" top:" + vInfo.p2+"%;";
                        namePos+="margin-top:-15px;"
                    }
                    if(axis.Y.scale) {
                        if(vInfo.p2>1) {
                            style+="height:" + vInfo.delta+"%;";
                        } else {
                            style+="height:" + this.getProperty("fixedHeight","5")+"px;";
                        }
                    }

                    if(entry.getName().includes("rilsd")) {
                        console.log("pos:" + namePos);
                    }
                    if(showIcon) {
                        items += HtmlUtil.div(["class","display-grid-entry-icon display-grid-entry", "entryid",entry.getId(),"index",i, "style", pos],entry.getIconImage());
                    }
                    var key = Math.round(xInfo.p1) + "---" + Math.round(vInfo.p1);
                    if(showName && !seen[key]) {
                        seen[key] = true;
                        var name = entry.getName().replace(/ /g,"&nbsp;");
                        items += HtmlUtil.div(["class","display-grid-entry-text display-grid-entry", "entryid",entry.getId(),"index",i,"style", namePos],name);
                    }
                    var boxStyle = style+"background:" + this.getProperty(ID_COLOR,"lightblue");
                    items+= HtmlUtil.div(["class","display-grid-entry-box display-grid-entry","itemtype","box", "entryid",entry.getId(), "style", boxStyle,"index",i],"");
                }
                this.jq(ID_AXIS_LEFT).html(axis.Y.html);
                this.jq(ID_CANVAS).html(axis.Y.lines+axis.X.lines+items);
                this.jq(ID_AXIS_BOTTOM).html(axis.X.html);
                this.initGrid(entries);
            },
            calculateSizeAxis:function(axisInfo) {
                var min =Number.MAX_VALUE;
                var max =Number.MIN_VALUE;
                for(var i=0;i<this.entries.length;i++) {
                    var entry = this.entries[i];
                    min = Math.min(min, entry.getSize());
                    max = Math.max(max, entry.getSize());
                }
            },
            checkOrder:function(axisInfo,percents) {
                /*
                if(!axisInfo.ascending) {
                    percents.p1 = 100-percents.p1;
                    percents.p2 = 100-percents.p2;
                    var tmp  =percents.p1;
                    percents.p1=percents.p2;
                    percents.p2=tmp;
                }
                */
                return {p1:percents.p1,p2:percents.p2,delta:Math.abs(percents.p2-percents.p1)};
            },
            calculateDatePercent:function(entry, axisInfo) {
                var p1 = 100*(entry.getStartDate().getTime()-axisInfo.min)/axisInfo.range;
                var p2 = 100*(entry.getEndDate().getTime()-axisInfo.min)/axisInfo.range;
                return this.checkOrder(axisInfo, {p1:p1,p2:p2,delta:Math.abs(p2-p1)});
            },
            calculateMonthPercent:function(entry, axisInfo) {
                var d1 = entry.getStartDate();
                var d2 = entry.getEndDate();
                var t1 = new Date(Date.UTC(1,d1.getUTCMonth(),d1.getUTCDate()));
                var t2 = new Date(Date.UTC(1,d2.getUTCMonth(),d2.getUTCDate()));
                var p1 = 100*((t1.getTime()-axisInfo.min)/axisInfo.range);
                var p2 = 100*((t2.getTime()-axisInfo.min)/axisInfo.range);
                if(entry.getName().includes("rilsd")) {
                    console.log("t1:" + t1);
                    console.log("t2:" + t2);
                    console.log("before:" + p1 +" " + p2);
                }
                return this.checkOrder(axisInfo, {p1:p1,p2:p2,delta:Math.abs(p2-p1)});
            },
             calculateMonthAxis:function(axisInfo) {
                axisInfo.calculatePercent = "calculateMonthPercent";
                axisInfo.minDate = new Date(Date.UTC(0,11,15));
                axisInfo.maxDate = new Date(Date.UTC(1,11,31));
                axisInfo.min = axisInfo.minDate.getTime();
                axisInfo.max = axisInfo.maxDate.getTime();
                axisInfo.range = axisInfo.max-axisInfo.min;
                var months = Utils.getMonthShortNames();
                for(var month=0;month<months.length;month++) {
                    var t1 = new Date(Date.UTC(1,month));
                    var percent = (axisInfo.maxDate.getTime()-t1.getTime())/axisInfo.range;
                    if(axisInfo.ascending)
                        percent = 1-percent;
                    axisInfo.ticks.push({percent:100*percent,label:months[month],major:false});
                }
            },
             calculateDateAxis: function(axisInfo) {
                axisInfo.calculatePercent = "calculateDatePercent";
                var numYears = axisInfo.maxDate.getUTCFullYear()  - axisInfo.minDate.getUTCFullYear();
                var years = numYears;
                axisInfo.type  = "year";
                axisInfo.skip = Math.max(1, Math.floor(numYears/axisInfo.maxTicks));
                if((numYears/axisInfo.skip)<=(axisInfo.maxTicks/2)) {
                    var numMonths = 0;
                    var tmp  = new Date(axisInfo.minDate.getTime());
                    while(tmp.getTime()<axisInfo.maxDate.getTime()) {
                        Utils.incrementMonth(tmp);
                        numMonths++;
                    }
                    axisInfo.skip=Math.max(1,Math.floor(numMonths/axisInfo.maxTicks));
                    axisInfo.type  = "month";
                    if((numMonths/axisInfo.skip)<=(axisInfo.maxTicks/2)) {
                        var tmp  = new Date(axisInfo.minDate.getTime());
                        var numDays =  0;
                        while(tmp.getTime()<axisInfo.maxDate.getTime()) {
                            Utils.incrementDay(tmp);
                            numDays++;
                        }
                        axisInfo.skip=Math.max(1, Math.floor(numDays/axisInfo.maxTicks));
                        axisInfo.type  = "day";
                    }
                }


                axisInfo.min = axisInfo.minDate.getTime();
                axisInfo.max = axisInfo.maxDate.getTime();
                axisInfo.range= axisInfo.max-axisInfo.min;
                var months = Utils.getMonthShortNames();
                var lastYear= null;
                var lastMonth= null;
                var tickDate;
                if(axisInfo.type == "year") {
                    tickDate  = new Date(Date.UTC(axisInfo.minDate.getUTCFullYear()));
                } else if(axisInfo.type == "month") {
                    tickDate  = new Date(Date.UTC(axisInfo.minDate.getUTCFullYear(),axisInfo.minDate.getUTCMonth()));
                } else {
                    tickDate  = new Date(Date.UTC(axisInfo.minDate.getUTCFullYear(),axisInfo.minDate.getUTCMonth(),axisInfo.minDate.getUTCDate()));
                }
                //                if(axisInfo.vertical)
                //                    console.log(axisInfo.type+" skip:" + axisInfo.skip + "   min:" + Utils.formatDateYYYYMMDD(axisInfo.minDate)+"   max:" + Utils.formatDateYYYYMMDD(axisInfo.maxDate));
                while(tickDate.getTime()<axisInfo.maxDate.getTime()) {
                    var percent = (tickDate.getTime()-axisInfo.minDate.getTime())/axisInfo.range;
                    if(!axisInfo.ascending)
                        percent = (1-percent);
                    percent = 100*percent;
                    //                    console.log("    perc:"+ percent +" " + Utils.formatDateYYYYMMDD(tickDate));
                    if(percent>=0 && percent<100) {
                        var label="";
                        var year = tickDate.getUTCFullYear(); 
                        var month = tickDate.getUTCMonth(); 
                        var major  = false;
                        if(axisInfo.type == "year") {
                            label = year;
                        } else if(axisInfo.type=="month") {
                            label = months[tickDate.getUTCMonth()];
                            if(lastYear!=year) {
                                label = label +"<br>" +  year;
                                lastYear = year;
                                major  =true;
                            }
                        }  else {
                            label = tickDate.getUTCDate();
                            if(lastYear!=year || lastMonth!=month) {
                                label = label +"<br>" +months[month] +" " +  year;
                                lastYear = year;
                                lastMonth  = month;
                                major  =true;
                            }
                        }
                        axisInfo.ticks.push({percent:percent,label:label,major:major});
                    }
                    if(axisInfo.type == "year") {
                        Utils.incrementYear(tickDate, axisInfo.skip);
                    }  else  if(axisInfo.type == "month") {
                        Utils.incrementMonth(tickDate,axisInfo.skip);
                    } else {
                        Utils.incrementDay(tickDate,axisInfo.skip);
                    }
                }

            }
        });
}


function RamaddaMetadataDisplay(displayManager, id, properties) {
    if(properties.formOpen == null) {
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
                if(this.haveDisplayed && this.entryList) {
                    this.entryListChanged(this.entryList);
                }
                this.haveDisplayed =true;
            },
            entryListChanged: function(entryList) {
                this.entryList = entryList;
                var entries = this.entryList.getEntries();
                if(entries.length==0) {
                    this.writeHtml(ID_ENTRIES, "Nothing found");
                    this.writeHtml(ID_RESULTS, "&nbsp;");
                    return;
                }
                var mdtsFromEntries = [];
                var mdtmap = {};
                var tmp = {};
                for(var i=0;i<entries.length;i++) {
                    var entry = entries[i];
                    var metadata = entry.getMetadata();
                    for(var j=0;j<metadata.length;j++) {
                        var m = metadata[j];
                        if(tmp[m.type] == null) {
                            tmp[m.type] = "";
                            mdtsFromEntries.push(m.type);
                        }
                        mdtmap[metadata[j].type] =metadata[j].label;
                    }
                }

                var html = "";
                html += HtmlUtil.openTag(TAG_TABLE,[ATTR_CLASS,"display-metadata-table",ATTR_WIDTH,"100%","cellpadding", "5","cellspacing","0"]);
                var type = this.findEntryType(this.searchSettings.entryType);
                var typeName = "Entry";
                if(type!=null) {
                    typeName  = type.getLabel();
                }
                this.writeHtml(ID_RESULTS, this.getResultsHeader(entries));



                var mdts =  null;
                //Get the metadata types to show from either a property or
                //gather them from all of the entries
                // e.g., "project_pi,project_person,project_funding"
                var prop = this.getProperty("metadataTypes",null);
                if(prop!=null) {
                    mdts = prop.split(",");
                } else {
                    mdts = mdtsFromEntries;
                    mdts.sort();
                }

                var skip ={
                    "content.pagestyle": true,
                    "content.pagetemplate": true,
                    "content.sort": true,
                    "spatial.polygon":true,
                };
                var headerItems = [];
                headerItems.push(HtmlUtil.th([ATTR_CLASS, "display-metadata-table-cell"],HtmlUtil.b(typeName)));
                for(var i=0;i<mdts.length;i++) {
                    var type = mdts[i];
                    if(skip[type]) {
                        continue;
                    }
                    var label = mdtmap[mdts[i]];
                    if(label == null) label = mdts[i];
                    headerItems.push(HtmlUtil.th([ATTR_CLASS, "display-metadata-table-cell"], HtmlUtil.b(label)));
                }
                var headerRow = HtmlUtil.tr(["valign", "bottom"],HtmlUtil.join(headerItems,""));
                html += headerRow;
                var divider = "<div class=display-metadata-divider></div>";
                var missing = this.missingMessage;
                if(missing = null) missing = "&nbsp;";
                for(var entryIdx=0;entryIdx<entries.length;entryIdx++) {
                    var entry = entries[entryIdx];
                    var metadata = entry.getMetadata();
                    var row = [];
                    var buttonId = this.getDomId("entrylink" + entry.getIdForDom());
                    var link =  entry.getLink(entry.getIconImage() +" " + entry.getName());
                    row.push(HtmlUtil.td([ATTR_CLASS, "display-metadata-table-cell"],HtmlUtil.div([ATTR_CLASS,"display-metadata-entrylink"], link)));
                    for(var mdtIdx=0;mdtIdx<mdts.length;mdtIdx++) {
                        var mdt = mdts[mdtIdx];
                        if(skip[mdt]) {
                            continue;
                        }
                        var cell = null;
                        for(var j=0;j<metadata.length;j++) {
                            var m = metadata[j];
                            if(m.type == mdt) {
                                var item = null;
                                if(m.type == "content.thumbnail" || m.type == "content.logo") {
                                    var url =this.getRamadda().getRoot() +"/metadata/view/" + m.value.attr1 +"?element=1&entryid=" + entry.getId() +"&metadata.id=" + m.id;
                                    item =  HtmlUtil.image(url,[ATTR_WIDTH,"100"]);
                                } else if(m.type == "content.url" || m.type == "dif.related_url") {
                                    var label = m.value.attr2;
                                    if(label == null || label == "") {
                                        label = m.value.attr1;
                                    }
                                    item =  HtmlUtil.href(m.value.attr1,label);
                                } else if(m.type == "content.attachment") {
                                    var toks = m.value.attr1.split("_file_");
                                    var filename = toks[1];
                                    var url =this.getRamadda().getRoot()+"/metadata/view/" + m.value.attr1 +"?element=1&entryid=" + entry.getId() +"&metadata.id=" + m.id;
                                    item =  HtmlUtil.href(url,filename);
                                } else {
                                    item = m.value.attr1;
                                    //                                    console.log("Item:" + item);
                                    if(m.value.attr2 && m.value.attr2.trim().length>0) {
                                        item += " - " + m.value.attr2;
                                    }
                                }
                                if(item!=null) {
                                    if(cell==null) {
                                        cell = "";
                                    } else {
                                        cell += divider;
                                    }
                                    cell += HtmlUtil.div([ATTR_CLASS, "display-metadata-item"], item);
                                }
                                
                            }
                        }
                        if(cell ==null) {
                            cell = missing;
                        }
                        if(cell ==null) {
                            cell = "";
                        }
                        var add = HtmlUtil.tag(TAG_A, [ATTR_STYLE,"color:#000;", ATTR_HREF, this.getRamadda().getRoot() + "/metadata/addform?entryid=" + entry.getId() +"&metadata.type=" + mdt,
                                                     "target","_blank","alt","Add metadata",ATTR_TITLE,"Add metadata"],"+");
                        add  = HtmlUtil.div(["class","display-metadata-table-add"], add);
                        var cellContents = add + divider;
                        if(cell.length>0)  {
                            cellContents +=  cell;
                        }
                        row.push(HtmlUtil.td([ATTR_CLASS, "display-metadata-table-cell"],HtmlUtil.div([ATTR_CLASS,"display-metadata-table-cell-contents"], cellContents)));
                    }
                    html += HtmlUtil.tr(["valign", "top"],HtmlUtil.join(row,""));
                    //Add in the header every 10 rows
                    if(((entryIdx+1) %10) == 0) html += headerRow;
                }
                html += HtmlUtil.closeTag(TAG_TABLE);
                this.jq(ID_ENTRIES).html(html);
            },
                });

}



function RamaddaTimelineDisplay(displayManager, id, properties) {
    if(properties.formOpen == null) {
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
                if(entries.length==0) {
                    this.writeHtml(ID_ENTRIES, "Nothing found");
                    this.writeHtml(ID_RESULTS, "&nbsp;");
                    return;
                }

                var data = {
                    "timeline":
                    {
                        "headline":"The Main Timeline Headline Goes here",
                        "type":"default",
                        "text":"<p>Intro body text goes here, some HTML is ok</p>",
                        "asset": {
                            "media":"http://yourdomain_or_socialmedialink_goes_here.jpg",
                            "credit":"Credit Name Goes Here",
                            "caption":"Caption text goes here"
                        },
                        "date": [
                {
                    "startDate":"2011,12,10",
                    "endDate":"2011,12,11",
                    "headline":"Headline Goes Here",
                    "text":"<p>Body text goes here, some HTML is OK</p>",
                    "tag":"This is Optional",
                    "classname":"optionaluniqueclassnamecanbeaddedhere",
                    "asset": {
                        "media":"http://twitter.com/ArjunaSoriano/status/164181156147900416",
                        "thumbnail":"optional-32x32px.jpg",
                        "credit":"Credit Name Goes Here",
                        "caption":"Caption text goes here"
                    }
                }
                ,
                {
                    "startDate":"2012,12,10",
                    "endDate":"2012,12,11",
                    "headline":"Headline Goes Here",
                    "text":"<p>Body text goes here, some HTML is OK</p>",
                    "tag":"This is Optional",
                    "classname":"optionaluniqueclassnamecanbeaddedhere",
                    "asset": {
                        "media":"http://twitter.com/ArjunaSoriano/status/164181156147900416",
                        "thumbnail":"optional-32x32px.jpg",
                        "credit":"Credit Name Goes Here",
                        "caption":"Caption text goes here"
                    }
                },
                {
                    "startDate":"2013,12,10",
                    "endDate":"2013,12,11",
                    "headline":"Headline Goes Here",
                    "text":"<p>Body text goes here, some HTML is OK</p>",
                    "tag":"This is Optional",
                    "classname":"optionaluniqueclassnamecanbeaddedhere",
                    "asset": {
                        "media":"http://twitter.com/ArjunaSoriano/status/164181156147900416",
                        "thumbnail":"optional-32x32px.jpg",
                        "credit":"Credit Name Goes Here",
                        "caption":"Caption text goes here"
                    }
                }

                                 ],
                        "era": [
                {
                    "startDate":"2011,12,10",
                    "endDate":"2011,12,11",
                    "headline":"Headline Goes Here",
                    "text":"<p>Body text goes here, some HTML is OK</p>",
                    "tag":"This is Optional"
                }

        ]
                    }
                };


                for(var i=0;i<entries.length;i++) {
                    var entry = entries[i];

                }
                createStoryJS({
                        type:       'timeline',
                            width:      '800',
                            height:     '600',
                            source:     data,
                            embed_id:   this.getDomId(ID_ENTRIES),  
                            });

            },
                });

}







function RamaddaEntrydisplayDisplay(displayManager, id, properties) {
    var SUPER;
    $.extend(this, {sourceEntry: properties.sourceEntry});
    RamaddaUtil.inherit(this, SUPER = new RamaddaDisplay(displayManager, id, DISPLAY_ENTRYDISPLAY, properties));
    if(properties.sourceEntry == null && properties.entryId!=null) {
        var _this = this;
        var callback  = function(entries) {
            var entry = entries[0];
            _this.sourceEntry = entry;
            _this.initDisplay();
        }
        properties.sourceEntry = this.getEntry(properties.entryId, callback);
    }


    addRamaddaDisplay(this);
    $.extend(this, {
            selectedEntry: null,
                initDisplay: function() {
                this.createUI();
                var title = this.title;
                if(this.sourceEntry!=null) {
                    this.addEntryHtml(this.sourceEntry);
                    var url = this.sourceEntry.getEntryUrl();
                    
                    if(title == null) {
                        title = this.sourceEntry.getName();
                    }
                    title = HtmlUtil.tag("a", ["href", url, "title", this.sourceEntry.getName(),"alt",this.sourceEntry.getName()],  title);
                } else {
                    this.addEntryHtml(this.selectedEntry);
                    if(title == null) {
                        title = "Entry Display";
                    }
                }
                this.setDisplayTitle(title);
            },
            handleEventEntrySelection: function(source, args) {
                //Ignore select events
                if(this.sourceEntry !=null) return;
                var selected = args.selected;
                var entry = args.entry;
                if(!selected) {
                    if(this.selectedEntry != entry) {
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
                if(entry==null) {
                    this.setContents("&nbsp;");
                    return;
                }
                var html = this.getEntryHtml(entry,{showHeader:false});
                this.setContents(html);
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
                if(this.entryList == null) {
                    this.entryList = new EntryList(this.getRamadda(), jsonUrl, this);
                }
                var html = "";
                html += HtmlUtil.div([ATTR_ID,this.getDomId(ID_ENTRIES),ATTR_CLASS,this.getClass("entries")], "");
                this.setContents(html);
            },
            entryListChanged: function(entryList) {
                var html = "<form>";
                html += "<p>";
                html += HtmlUtil.openTag(TAG_TABLE,[ATTR_CLASS,"formtable","cellspacing","0","cellspacing","0"]);
                var entries = this.entryList.getEntries();
                var get = this.getGet();

                for(var j=1;j<=2;j++) {
                    var select= HtmlUtil.openTag(TAG_SELECT,[ATTR_ID, this.getDomId(ID_SELECT +j)]);
                    select += HtmlUtil.tag(TAG_OPTION,[ATTR_TITLE,"",ATTR_VALUE,""],
                                         "-- Select --");
                    for(var i=0;i<entries.length;i++) {
                        var entry = entries[i];
                        var label = entry.getIconImage() +" " + entry.getName();
                        select += HtmlUtil.tag(TAG_OPTION,[ATTR_TITLE,entry.getName(),ATTR_VALUE,entry.getId()],
                                             entry.getName());
                        
                    }
                    select += HtmlUtil.closeTag(TAG_SELECT);
                    html += HtmlUtil.formEntry("Data:",select);
                }

                var select  = HtmlUtil.openTag(TAG_SELECT,[ATTR_ID, this.getDomId(ID_CHARTTYPE)]);
                select += HtmlUtil.tag(TAG_OPTION,[ATTR_TITLE,"",ATTR_VALUE,"linechart"],
                                     "Line chart");
                select += HtmlUtil.tag(TAG_OPTION,[ATTR_TITLE,"",ATTR_VALUE,"barchart"],
                                     "Bar chart");
                select += HtmlUtil.tag(TAG_OPTION,[ATTR_TITLE,"",ATTR_VALUE,"barstack"],
                                     "Stacked bars");
                select += HtmlUtil.tag(TAG_OPTION,[ATTR_TITLE,"",ATTR_VALUE,"bartable"],
                                     "Bar table");
                select += HtmlUtil.tag(TAG_OPTION,[ATTR_TITLE,"",ATTR_VALUE,"piechart"],
                                     "Pie chart");
                select += HtmlUtil.tag(TAG_OPTION,[ATTR_TITLE,"",ATTR_VALUE,"scatterplot"],
                                     "Scatter Plot");
                select += HtmlUtil.closeTag(TAG_SELECT);
                html += HtmlUtil.formEntry("Chart Type:",select);

                html += HtmlUtil.closeTag(TAG_TABLE);
                html += "<p>";
                html +=  HtmlUtil.tag(TAG_DIV, [ATTR_CLASS, "display-button", ATTR_ID,  this.getDomId(ID_NEWDISPLAY)],"New Chart");
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
                if(entry1 == null) {
                    alert("No data selected");
                    return;
                }
                var pointDataList = [];

                pointDataList.push(new PointData(entry1.getName(), null, null, ramaddaBaseUrl +"/entry/show?&output=points.product&product=points.json&numpoints=1000&entryid=" +entry1.getId()));
                if(entry2!=null) {
                    pointDataList.push(new PointData(entry2.getName(), null, null, ramaddaBaseUrl +"/entry/show?&output=points.product&product=points.json&numpoints=1000&entryid=" +entry2.getId()));
                }

                //Make up some functions
                var operation = "average";
                var derivedData = new  DerivedPointData(this.displayManager, "Derived Data", pointDataList,operation);
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
                if(this.ramaddas.length==0) {
                    html += this.getMessage("No repositories specified");
                } else {
                    html += this.getMessage("Loading repository listing");
                }
                this.numberWithTypes = 0;
                this.finishedInitDisplay = false;
                //Check for and remove the all repositories
                if(this.ramaddas.length>1) {
                    if(this.ramaddas[this.ramaddas.length-1].getRoot() == "all") {
                        this.ramaddas.splice(this.ramaddas.length-1,1);
                    }
                }
                for(var i=0;i<this.ramaddas.length;i++) {
                    if(i == 0) {
                    }
                    var ramadda = this.ramaddas[i];
                    var types = ramadda.getEntryTypes(function(ramadda, types) {theDisplay.gotTypes(ramadda, types);});
                    if(types !=null) {
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
                if(!this.finishedInitDisplay || this.numberWithTypes != this.ramaddas.length) {
                    return;
                }
                var typeMap = {};
                var allTypes = [];
                var html = "";
                html += HtmlUtil.openTag(TAG_TABLE, [ATTR_CLASS, "display-repositories-table",ATTR_WIDTH,"100%",ATTR_BORDER,"1","cellspacing","0","cellpadding","5"]);
                for(var i=0;i<this.ramaddas.length;i++) {
                    var ramadda = this.ramaddas[i]; 
                    var types = ramadda.getEntryTypes();
                    for(var typeIdx=0;typeIdx<types.length;typeIdx++) {
                        var type = types[typeIdx];
                        if(typeMap[type.getId()] == null) {
                            typeMap[type.getId()] = type;
                            allTypes.push(type);
                        }
                    }
                }

                html += HtmlUtil.openTag(TAG_TR, ["valign", "bottom"]);
                html += HtmlUtil.th([ATTR_CLASS,"display-repositories-table-header"],"Type");
                for(var i=0;i<this.ramaddas.length;i++) {
                    var ramadda = this.ramaddas[i];
                    var link = HtmlUtil.href(ramadda.getRoot(),ramadda.getName());
                    html += HtmlUtil.th([ATTR_CLASS,"display-repositories-table-header"],link);
                }
                html += "</tr>";

                var onlyCats = [];
                if(this.categories!=null) {
                    onlyCats = this.categories.split(",");
                }



                var catMap = {};
                var cats = [];
                for(var typeIdx =0;typeIdx<allTypes.length;typeIdx++) {
                    var type = allTypes[typeIdx];
                    var row = "";
                    row += "<tr>";
                    row += HtmlUtil.td([],HtmlUtil.image(type.getIcon()) +" " + type.getLabel());
                    for(var i=0;i<this.ramaddas.length;i++) {
                        var ramadda = this.ramaddas[i];
                        var repoType = ramadda.getEntryType(type.getId());
                        var col = "";
                        if(repoType == null) {
                            row += HtmlUtil.td([ATTR_CLASS,"display-repositories-table-type-hasnot"],"");
                        } else {
                            var label  =
                                HtmlUtil.tag(TAG_A, ["href", ramadda.getRoot()+"/search/type/" + repoType.getId(),"target","_blank"],
                                             repoType.getEntryCount());
                            row += HtmlUtil.td([ATTR_ALIGN, "right", ATTR_CLASS,"display-repositories-table-type-has"],label);
                        }

                    }
                    row += "</tr>";

                    var catRows = catMap[type.getCategory()];
                    if(catRows == null) {
                        catRows = [];
                        catMap[type.getCategory()] = catRows;
                        cats.push(type.getCategory());
                    }
                    catRows.push(row);
                }

                for(var i=0;i<cats.length;i++) {
                    var cat = cats[i];
                    if(onlyCats.length>0) {
                        var ok = false;
                        for(var patternIdx=0;patternIdx<onlyCats.length;patternIdx++) {
                            if(cat == onlyCats[patternIdx]) {
                                ok = true;
                                break;
                            }
                            if(cat.match(onlyCats[patternIdx])) {
                                ok = true;
                                break;
                                
                            }
                        }
                        if(!ok) continue;

                    }
                    var rows = catMap[cat];
                    html +=  "<tr>";
                    html += HtmlUtil.th(["colspan", ""+(1 + this.ramaddas.length)], cat);
                    html +=  "</tr>";
                    for(var row=0;row<rows.length;row++) {
                        html += rows[row];
                    }

                }


                html += HtmlUtil.closeTag(HtmlUtil.TAG_TABLE);
                this.setContents(html);
            },
            gotTypes: function(ramadda,  types) {
                this.numberWithTypes++;
                this.displayRepositories();
            }
        });
}


var  RamaddaGalleryDisplay = RamaddaEntrygalleryDisplay;/**
Copyright 2008-2015 Geode Systems LLC
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
                var html =  "<p>";
                html +=HtmlUtil.onClick(get +".click();", HtmlUtil.div([ATTR_ID, this.getDomId(ID_CLICK)], "Click me"));
                html +=  "<p>";
                html += HtmlUtil.div([ATTR_ID, this.getDomId(ID_DATA)], "");

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
                if(pointData == null) return;
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
}


/**
Copyright 2008-2015 Geode Systems LLC
*/


//Properties
var PROP_LAYOUT_TYPE = "layoutType";
var PROP_LAYOUT_COLUMNS = "layoutColumns";
var PROP_SHOW_MAP = "showMap";
var PROP_SHOW_MENU  = "showMenu";
var PROP_FROMDATE = "fromDate";
var PROP_TODATE = "toDate";

//
//adds the display manager to the list of global display managers
//
function addDisplayManager(displayManager) {
    if(window.globalDisplayManagers == null) {
        window.globalDisplayManagers =  {};
        // window.globalDisplayManager = null;
    }
    window.globalDisplayManagers[displayManager.getId()] = displayManager;
    window.globalDisplayManager = displayManager;
}


function addGlobalDisplayType(type) {
    if(window.globalDisplayTypes == null) {
        window.globalDisplayTypes=  [];
    }
    window.globalDisplayTypes.push(type);
}

//
//This will get the currently created global displaymanager or will create a new one
//
function getOrCreateDisplayManager(id, properties, force) {
    if(!force) {
        var displayManager = getDisplayManager(id);
        if(displayManager != null) {
            return displayManager;
        }
        if(window.globalDisplayManager!=null) {
            return window.globalDisplayManager;
        }
    }
    var displayManager =     new DisplayManager(id, properties);
    if(window.globalDisplayManager==null) {
        window.globalDisplayManager  = displayManager;
    }
    return displayManager;
}

//
//return the global display manager with the given id, null if not found
//
function getDisplayManager(id) {
    if(window.globalDisplayManagers==null) {
        return null;
    }
    var manager =  window.globalDisplayManagers[id];
    return manager;
}



var ID_DISPLAYS = "displays";

//
//DisplayManager constructor
//

function DisplayManager(argId,argProperties) {


    var ID_MENU_BUTTON = "menu_button";
    var ID_MENU_OUTER =  "menu_outer";
    var ID_MENU_INNER =  "menu_inner";

    RamaddaUtil.inherit(this, this.SUPER = new DisplayThing(argId, argProperties));
    addRamaddaDisplay(this);

    RamaddaUtil.initMembers(this, {
                dataList : [],
                displayTypes: [],
                initMapBounds : null,
                });

    if(window.globalDisplayTypes!=null) {
        for(var i=0;i<window.globalDisplayTypes.length;i++) {
            this.displayTypes.push(window.globalDisplayTypes[i]);
        }
    }




    RamaddaUtil.defineMembers(this, {
           group: new DisplayGroup(this, argId,argProperties),
           showmap : this.getProperty(PROP_SHOW_MAP,null),
           setDisplayReady: function() {
                SUPER.setDisplayReadyCall(this);
                console.log("displaymanager.displayReady");
                this.getLayoutManager().setDisplayReady();
           },
           addDisplayType: function(type,label) {
               this.displayTypes.push({type:label});
           },
           getLayoutManager: function () {
               return this.group;
           },
           collectEntries: function() {
               var entries = this.getLayoutManager().collectEntries();
               return entries;
           },
           getData: function() {
               return this.dataList;
           },
           handleEventFieldsSelected: function (source, fields) {
               this.notifyEvent("handleEventFieldsSelected", source, fields);
           },
           handleEventEntriesChanged: function (source, entries) {
               this.notifyEvent("handleEventEntriesChanged", source, entries);
           },
           handleEventMapBoundsChanged: function (source,  bounds, forceSet) {
                var args = {
                    "bounds":bounds,
                    "force":forceSet
                };
                this.notifyEvent("handleEventMapBoundsChanged", source, args);
           },
           addMapLayer: function(source, entry) {
               this.notifyEvent("addMapLayer", source, {entry:entry});
           },
           handleEventMapClick: function (mapDisplay, lon, lat) {
                var indexObj = [];
                var records = null;
                for(var i=0;i<this.dataList.length;i++) {
                    var pointData = this.dataList[i];
                    records = pointData.getRecords();
                    if(records!=null) break;
                }
                var indexObj = [];
                var closest =  RecordUtil.findClosest(records, lon, lat, indexObj);
                if(closest!=null) {
                    this.propagateEventRecordSelection(mapDisplay, pointData, {index:indexObj.index});
                }
                this.notifyEvent("handleEventMapClick", mapDisplay, {mapDisplay:mapDisplay,lon:lon,lat:lat});
            },
            propagateEventRecordSelection: function(source, pointData, args) {
                var index = args.index;
                if(pointData ==null && this.dataList.length>0) {
                    pointData = this.dataList[0];
                }
                var fields =  pointData.getRecordFields();
                var records = pointData.getRecords();
                if(records == null) {
                    return;
                }
                if(index<0 || index>= records.length) {
                    console.log("propagateEventRecordSelection: bad index= " + index);
                    return;
                 }
                var record = records[index];
                if(record == null) return;
                var values = this.getRecordHtml(record,fields);
                if(source.recordSelectionCallback) {
                    var func = source.recordSelectionCallback;
                    if((typeof  func) == "string") {
                        func = window[func];
                    }
                    func({display:source, pointData: pointData, index:index, pointRecord:record});
                }
                var params =  {index:index, record:record, html:values,data:pointData};
                this.notifyEvent("handleEventRecordSelection", source,params);
                var entries  =source.getEntries();
                if(entries!=null && entries.length>0) {
                    this.handleEventEntrySelection(source, {entry:entries[0], selected:true});
                }
            },
            handleEventEntrySelection: function(source,  props) {
               this.notifyEvent("handleEventEntrySelection", source, props);
            },
            handleEventEntryMouseover: function(source,  props) {
               this.notifyEvent("handleEventEntryMouseover", source, props);
            },
            handleEventEntryMouseout: function(source,  props) {
               this.notifyEvent("handleEventEntryMouseout", source, props);
            },
            handleEventPointDataLoaded: function(source, pointData) {
                this.notifyEvent("handleEventPointDataLoaded", source, pointData);
            },
           ranges: {
               //               "TRF": [0,100],
           },
           setRange: function(field, range) {
               if(this.ranges[field.getId()] == null) {
                   this.ranges[field.getId()] = range;
               } 
           },
           getRange: function(field) {
               return this.ranges[field.getId()];
           },
            makeMainMenu: function() {
                if(!this.getProperty(PROP_SHOW_MENU, true))  {
                    return "";
                }
                //How else do I refer to this object in the html that I add 
                var get = "getDisplayManager('" + this.getId() +"')";
                var layout = "getDisplayManager('" + this.getId() +"').getLayoutManager()";
                var html = "";
                var newMenus = {};
                var cats = [];
                var chartMenu = "";
                for(var i=0;i<this.displayTypes.length;i++) {
                    //The ids (.e.g., 'linechart' have to match up with some class function with the name 
                    var type = this.displayTypes[i];
                    if(Utils.isDefined(type.forUser) && !type.forUser) continue;
                    var category = type.category;
                    if(category == null) {
                        category = "Misc";
                    }
                    if(newMenus[category] == null) {
                        newMenus[category] = "";
                        cats.push(category);
                    }
                    newMenus[category]+= HtmlUtil.tag(TAG_LI,[], HtmlUtil.tag(TAG_A, ["onclick", get+".userCreateDisplay('" + type.type+"');"], type.label));
                }

                var newMenu = "";
                for(var i=0;i<cats.length;i++) {
                    var cat =  cats[i];
                    if(cat == "Charts") {
                        chartMenu = newMenus[cat];
                    }
                    var subMenu = HtmlUtil.tag("ul",[], newMenus[cat]);
                    var catLabel = HtmlUtil.tag(TAG_A,[],cat);
                    newMenu  += HtmlUtil.tag(TAG_LI,[],catLabel+subMenu);
                    //                    newMenu  += HtmlUtil.tag(TAG_LI,[], "SUB " + i);
                }
                var publishMenu = 
                    HtmlUtil.tag(TAG_LI,[], HtmlUtil.onClick(layout +".publish('media_photoalbum');", "New Photo Album")) +"\n" +
                    HtmlUtil.tag(TAG_LI,[], HtmlUtil.onClick(layout +".publish('wikipage');", "New Wiki Page")) +"\n" +
                    HtmlUtil.tag(TAG_LI,[], HtmlUtil.onClick(layout +".publish('blogentry');", "New Blog Post")) +"\n";


                var fileMenu = 
                    HtmlUtil.tag(TAG_LI,[], "<a>Publish</a>" + HtmlUtil.tag("ul",[], publishMenu)) +"\n" +
                    HtmlUtil.tag(TAG_LI,[], HtmlUtil.onClick(layout +".copyDisplayedEntries();", "Save entries")) +"\n";


                var titles = HtmlUtil.tag(TAG_DIV,["class","ramadda-menu-block"],"Titles: " + HtmlUtil.onClick(layout +".titlesOn();", "On") +"/" + HtmlUtil.onClick(layout +".titlesOff();", "Off"));
                var dates =   HtmlUtil.tag(TAG_DIV,["class","ramadda-menu-block"], 
                                           "Set date range: " + 
                                           HtmlUtil.onClick(layout +".askMinDate();", "Min") +"/" +
                                           HtmlUtil.onClick(layout +".askMaxDate();", "Max"));
                var editMenu = 
                    HtmlUtil.tag(TAG_LI,[], HtmlUtil.tag(TAG_DIV,["class","ramadda-menu-block"],
                                                         "Set axis range :" +
                                                         HtmlUtil.onClick(layout +".askMinZAxis();", "Min") +"/" +
                                                         HtmlUtil.onClick(layout +".askMaxZAxis();", "Max"))) +
                    HtmlUtil.tag(TAG_LI,[], dates) +
                    HtmlUtil.tag(TAG_LI,[], titles) +"\n" +
                    HtmlUtil.tag(TAG_LI,[],HtmlUtil.tag(TAG_DIV,["class", "ramadda-menu-block"], "Details: " + HtmlUtil.onClick(layout +".detailsOn();", "On",[]) +"/" +
                                                        HtmlUtil.onClick(layout +".detailsOff();", "Off",[]))) +
                    HtmlUtil.tag(TAG_LI,[], HtmlUtil.onClick(layout +".deleteAllDisplays();", "Delete all displays")) +"\n" +
                    "";


                var table = HtmlUtil.tag(TAG_DIV,["class","ramadda-menu-block"],"Table: " + 
                                         HtmlUtil.onClick(layout +".setLayout('table',1);", "1 column")+" / " +
                                         HtmlUtil.onClick(layout +".setLayout('table',2);", "2 column") +" / " +
                                         HtmlUtil.onClick(layout +".setLayout('table',3);", "3 column") +" / " +
                                         HtmlUtil.onClick(layout +".setLayout('table',4);", "4 column"));
                var layoutMenu = 
                    HtmlUtil.tag(TAG_LI,[], table) +
                    HtmlUtil.tag(TAG_LI,[], HtmlUtil.onClick(layout +".setLayout('rows');", "Rows")) +"\n" +
                    HtmlUtil.tag(TAG_LI,[], HtmlUtil.onClick(layout +".setLayout('columns');", "Columns")) +"\n" +
                    HtmlUtil.tag(TAG_LI,[], HtmlUtil.onClick(layout +".setLayout('tabs');", "Tabs"));



                var menuBar =  HtmlUtil.tag(TAG_LI,[],"<a>File</a>" + HtmlUtil.tag("ul",[], fileMenu));
                if(chartMenu!="") {
                    menuBar += HtmlUtil.tag(TAG_LI,[],"<a>Charts</a>" + HtmlUtil.tag("ul",[], chartMenu));
                }
                menuBar+= HtmlUtil.tag(TAG_LI,[],"<a>Edit</a>" + HtmlUtil.tag("ul",[], editMenu)) +
                    HtmlUtil.tag(TAG_LI,[],"<a>New</a>" + HtmlUtil.tag("ul",[], newMenu)) +
                    HtmlUtil.tag(TAG_LI,[],"<a>Layout</a>" + HtmlUtil.tag("ul", [], layoutMenu));
                var menu = HtmlUtil.div([ATTR_CLASS,"ramadda-popup", ATTR_ID, this.getDomId(ID_MENU_OUTER)], 
                                        HtmlUtil.tag("ul", [ATTR_ID, this.getDomId(ID_MENU_INNER),ATTR_CLASS, "sf-menu"], menuBar));

                html += menu;
                html += HtmlUtil.tag(TAG_A, [ATTR_CLASS, "display-menu-button", ATTR_ID, this.getDomId(ID_MENU_BUTTON)],"&nbsp;");
                //                html+="<br>";
                return html;
            },
            hasGeoMacro: function(jsonUrl) {
               return  jsonUrl.match(/(\${latitude})/g) !=null;
            },
            getJsonUrl:function(jsonUrl, display, props) {
                var fromDate  = display.getProperty(PROP_FROMDATE);
                if(fromDate!=null) {
                    jsonUrl += "&fromdate=" + fromDate;
                }
                var toDate  = display.getProperty(PROP_TODATE);
                if(toDate!=null) {
                    jsonUrl += "&todate=" + toDate;
                }
                
                if(this.hasGeoMacro(jsonUrl)) {
                    var lon = props.lon;
                    var lat = props.lat;
                    
                    if((lon == null || lat == null) &&  this.map!=null) {
                        var tuple = this.getPosition();
                        if(tuple!=null) {
                            lat = tuple[0];
                            lon = tuple[1];
                        }
                    } 
                    if(lon != null && lat != null) {
                    	jsonUrl = jsonUrl.replace("${latitude}",lat.toString());
                        jsonUrl = jsonUrl.replace("${longitude}",lon.toString());
                    } 
                }
                jsonUrl = jsonUrl.replace("${numpoints}",1000);
                return jsonUrl;
            },
            getDefaultData: function() {
                for(var i in this.dataList) {
                    var data = this.dataList[i];
                    var records = data.getRecords();
                    if(records!=null) {
                        return data;
                    }
                }
                if(this.dataList.length>0) {
                    return this.dataList[0];
                }
                return null;
            },

           writeDisplay: function() {
               if(this.originalLocation==null) {
                   this.originalLocation = document.location;
               }
               var url = this.originalLocation+"#";
               url += "&display0=linechart";
               for(var attr in document) {
                   //                   if(attr.toString().contains("location")) 
                   //                       console.log(attr +"=" + document[attr]);
               }
               document.location = url;

           },
           userCreateDisplay: function(type, props) {
               if(props == null) {
                   props ={};
               }
               props.editMode = true;
               if(type == DISPLAY_LABEL && props.text == null) {
                   var text = prompt("Text");
                   if(text == null) return;
                   props.text = text;
               }
               return this.createDisplay(type, props);
           },
           createDisplay: function(type, props) {
               if(props == null) {
                   props ={};
               }
                if(props.data!=null) {
                    var haveItAlready = false;
                    for(var i=0;i<this.dataList.length;i++) {
                        var existingData = this.dataList[i];
                        if(existingData.equals(props.data)) {
                            props.data = existingData;
                            haveItAlready = true;
                            break;
                        }
                    }
                    if(!haveItAlready) {
                        this.dataList.push(props.data);
                    }
                }

                //Upper case the type name, e.g., linechart->Linechart
                var proc = type.substring(0,1).toUpperCase() + type.substring(1);

                //Look for global functions  Ramadda<Type>Display, <Type>Display, <Type> 
                //e.g. - RamaddaLinechartDisplay, LinechartDisplay, Linechart 
                var classname = null;
                var names = ["Ramadda" +proc + "Display",
                            proc +"Display",
                            proc];
                var func = null;
                var funcName = null;
                var msg = "";
                for(var i=0;i<names.length;i++) {
                    msg += ("trying:" + names[i] +"\n");
                    if(window[names[i]]!=null) {
                        funcName = names[i];
                        func = window[names[i]];
                        break;
                    }

                }
                if(func==null) {
                    console.log("Error: could not find display function:" + type);
                    //                    alert("Error: could not find display function:" + type);
                    alert("Error: could not find display function:" + type +" msg: " + msg);
                    return;
                }
                var displayId = this.getUniqueId("display");
                if(props.data==null && this.dataList.length>0) {
                    props.data =  this.dataList[0];
                }
                props.createdInteractively = true;
                if(!props.entryId) {
                    props.entryId = this.group.entryId;
                }


                var display =  eval(" new " + funcName+"(this,'" +  displayId+"', props);");
                if(display == null) {
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
            notifyEvent:function(func, source, data) {
               this.getLayoutManager().notifyEvent(func, source, data);
            }, 
            removeDisplay:function(display) {
                this.getLayoutManager().removeDisplay(display);
                this.notifyEvent("handleEventRemoveDisplay", this, display);
            },
        });

    addDisplayManager(this);

    var displaysHtml = HtmlUtil.div([ATTR_ID, this.getDomId(ID_DISPLAYS),ATTR_CLASS,"display-container"]);
    var html = HtmlUtil.openTag(TAG_DIV);
    html += this.makeMainMenu();
    var targetDiv = this.getProperty("target");
    var _this = this;
    if(targetDiv!=null) {
        $( document ).ready(function() {
                $("#" + targetDiv).html(displaysHtml);
                _this.getLayoutManager().doLayout();
            });

    } else {
        html +=   displaysHtml;
    }
    html += HtmlUtil.closeTag(TAG_DIV);

    $("#"+ this.getId()).html(html)
    if(this.showmap) {
        this.createDisplay('map');
    }
    var theDisplayManager = this;

    $("#"+this.getDomId(ID_MENU_BUTTON)).button({ icons: { primary: "ui-icon-gear", secondary: "ui-icon-triangle-1-s"}}).click(function(event) {
            var id =theDisplayManager.getDomId(ID_MENU_OUTER); 
            showPopup(event, theDisplayManager.getDomId(ID_MENU_BUTTON), id, false,null,"left bottom");
            $("#"+  theDisplayManager.getDomId(ID_MENU_INNER)).superfish({
                    //Don't set animation - it is broke on safari
                    //                    animation: {height:'show'},
                        speed: 'fast',
                            delay: 300
                        });
        });

}


/**
Copyright 2008-2018 Geode Systems LLC
*/


var DISPLAY_MAP = "map";

var displayMapMarkers = ["marker.png", "marker-blue.png","marker-gold.png","marker-green.png"];

var displayMapCurrentMarker =-1;
var displayMapUrlToVectorListeners = {};
var displayMapMarkerIcons = {};

function displayMapGetMarkerIcon() {
    displayMapCurrentMarker++;
    if(displayMapCurrentMarker>= displayMapMarkers.length)  displayMapCurrentMarker = 0;
    return  ramaddaBaseUrl + "/lib/openlayers/v2/img/" + displayMapMarkers[displayMapCurrentMarker];
}

addGlobalDisplayType({type : DISPLAY_MAP,label : "Map"});

function MapFeature(source, points) {
	RamaddaUtil.defineMembers(this, {
		source : source,
		points : points
	});
}




function RamaddaMapDisplay(displayManager, id, properties) {
	var ID_LATFIELD = "latfield";
	var ID_LONFIELD = "lonfield";
	var ID_MAP = "map";
	var ID_BOTTOM = "bottom";
	var SUPER;
	RamaddaUtil.defineMembers(this, {
                showLocationReadout: false,
                showBoxes:true,
                    showPercent: false,
                    percentFields: null,
                    kmlLayer:null,
                    kmlLayerName:"",
                    geojsonLayer:null,
                    geojsonLayerName:"",
                    theMap: null
            });

	RamaddaUtil.inherit(this, SUPER = new RamaddaDisplay(displayManager, id,
			DISPLAY_MAP, properties));
	addRamaddaDisplay(this);
	RamaddaUtil.defineMembers(this, {
		initBounds : displayManager.initMapBounds,
		mapBoundsSet : false,
		features : [],
		myMarkers : {},
		mapEntryInfos : {},
		sourceToLine : {},
		sourceToPoints : {},
		snarf : true,
		initDisplay : function() {
                    this.createUI();
                    var _this = this;
                    var html = "";
                    var extraStyle = "min-height:200px;";
                    var width = this.getWidth();
                    if(Utils.isDefined(width)) {
                        if (width > 0) {
                            extraStyle += "width:" + width + "px; ";
                        } else if(width !="") {
                            extraStyle += "width:" + width+";";
                        }
                    }

                    var height = this.getProperty("height", 300);
                    // var height = this.getProperty("height",-1);
                    if (height > 0) {
                        extraStyle += " height:" + height + "px; ";
                    }
                    
                    html += HtmlUtil.div([ ATTR_CLASS, "display-map-map", "style",
                                           extraStyle, ATTR_ID, this.getDomId(ID_MAP) ]);
                    html += HtmlUtil.div([ ATTR_CLASS, "",  ATTR_ID, this.getDomId(ID_BOTTOM) ]);

                    if(this.showLocationReadout) {
                        html += HtmlUtil.openTag(TAG_DIV, [ ATTR_CLASS,
                                                            "display-map-latlon" ]);
                        html += HtmlUtil.openTag("form");
                        html += "Latitude: "
                            + HtmlUtil.input(this.getDomId(ID_LATFIELD), "", [ "size",
                                                                               "7", ATTR_ID, this.getDomId(ID_LATFIELD) ]);
                        html += "  ";
                        html += "Longitude: "
                            + HtmlUtil.input(this.getDomId(ID_LONFIELD), "", [ "size",
                                                                               "7", ATTR_ID, this.getDomId(ID_LONFIELD) ]);
                        html += HtmlUtil.closeTag("form");
                        html += HtmlUtil.closeTag(TAG_DIV);
                    }
                    this.setContents(html);

                    if(!this.map) {
                        this.createMap();
                    } else  {
                        this.map.setMapDiv(this.getDomId(ID_MAP));
                    }

                    if(!this.haveCalledUpdateUI) {
                        var callback  = function() {
                            _this.updateUI();
                        }
                        setTimeout(callback,1);
                    }
		},
                createMap: function() {
                    var theDisplay  =this;

                    var params = {
                        "defaultMapLayer" : this.getProperty("defaultMapLayer",
                                                             map_default_layer),
                                
                    };
                    var displayDiv = this.getProperty("displayDiv", null);
                    if(displayDiv) {
                        params.displayDiv = displayDiv;
                    }
                    var mapLayers = this.getProperty("mapLayers", null);
                    if(mapLayers) {
                        params.mapLayers =  [mapLayers];
                    }

                    this.map =  this.getProperty("theMap",null);
                    if(this.map) {
                        this.map.setMapDiv(this.getDomId(ID_MAP));
                    } else {
                        this.map = new RepositoryMap(this.getDomId(ID_MAP), params);
                    }
                    if(this.doDisplayMap()) {
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

                    if (this.initBounds != null) {
                        var b = this.initBounds;
                        this.setInitMapBounds(b.north, b.west, b.south, b.east);
                    }

                    var currentFeatures = this.features;
                    this.features = [];
                    for ( var i = 0; i < currentFeatures.length; i++) {
                        this.addFeature(currentFeatures[i]);
                    }
                    var entries = this.getDisplayManager().collectEntries();
                    for ( var i = 0; i < entries.length; i++) {
                        var pair = entries[i];
                        this.handleEventEntriesChanged(pair.source, pair.entries);
                    }

                    if(this.layerEntries) {
                        var selectCallback = function(layer) {
                            _this.handleLayerSelect(layer);
                        }
                        var unselectCallback = function(layer) {
                            _this.handleLayerUnselect(layer);
                        }
                        var toks = this.layerEntries.split(",");
                        for(var i=0;i<toks.length;i++) {
                            var tok = toks[i];
                            var url = ramaddaBaseUrl + "/entry/show?output=shapefile.kml&entryid=" + tok;
                            this.map.addKMLLayer("layer", url, true, selectCallback, unselectCallback);
                            //TODO: Center on the kml
                        }
                    }

                    if(theDisplay.kmlLayer!=null) {
                        var url = ramaddaBaseUrl + "/entry/show?output=shapefile.kml&entryid=" + theDisplay.kmlLayer;
                        theDisplay.addBaseMapLayer(url, true);
                    }
                    if(theDisplay.geojsonLayer!=null) {
                        url = theDisplay.getRamadda().getEntryDownloadUrl(theDisplay.geojsonLayer);
                        theDisplay.addBaseMapLayer(url, false);
                    }
                },
                addBaseMapLayer: function(url, isKml) {
                    var theDisplay = this;
                    mapLoadInfo = displayMapUrlToVectorListeners[url];
                    if(mapLoadInfo == null) {
                        mapLoadInfo = {otherMaps:[], layer:null};
                        selectFunc  = function(layer) {
                            theDisplay.mapFeatureSelected(layer);
                        }
                        if(isKml)
                            this.map.addKMLLayer(this.kmlLayerName,url,this.doDisplayMap(),selectFunc,null,null,
                                                 function(map,layer) {theDisplay.baseMapLoaded(layer,url);});
                        else
                           this.map.addGeoJsonLayer(this.geojsonLayerName,url,this.doDisplayMap(),selectFunc,null,null,
                                                     function(map,layer) {
                                                         theDisplay.baseMapLoaded(layer,url);
                                                     });
                    } else if(mapLoadInfo.layer) {
                        this.cloneLayer(mapLoadInfo.layer);
                    } else {
                        this.map.showLoadingImage();
                        mapLoadInfo.otherMaps.push(this);
                    }
                },
                mapFeatureSelected: function(layer) {
                    if(!this.getPointData()) {
                        //                        console.log("no point data");
                        return;
                    }
                    this.map.onFeatureSelect(layer);
                    if(!Utils.isDefined(layer.feature.dataIndex)) {
                        return;
                    }
                    //                    console.log("map index:" + layer.feature.dataIndex);
                    this.getDisplayManager().propagateEventRecordSelection(this,this.getPointData(),{index:layer.feature.dataIndex});
                },
               doDisplayMap:  function() {
                    var v = (this.kmlLayer!=null || this.geojsonLayer!=null) && ((""+this.getProperty("displayAsMap","")) == "true");
//                    console.log("doDisplayMap:" + v +" " +this.getProperty("displayAsMap",""));
                    return  v;
                },
                cloneLayer: function(layer) {
                    var theDisplay  = this;
                    this.map.hideLoadingImage();
                    layer = layer.clone();
                    var features = layer.features;
                    var clonedFeatures = [];
                    for (var j = 0; j < features.length; j++) {
                        feature = features[j];
                        feature = feature.clone();
                        if(feature.style) {
                            oldStyle = feature.style;
                            feature.style={};
                            for(var a in oldStyle) {
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
                    this.map.addSelectCallback(layer,this.doDisplayMap(),function(layer) {theDisplay.mapFeatureSelected(layer);});
                },
                baseMapLoaded: function(layer, url) {
                    this.vectorLayer = layer;
                    this.applyVectorMap();
                    mapLoadInfo = displayMapUrlToVectorListeners[url];
                    if(mapLoadInfo) {
                        mapLoadInfo.layer = layer;
                        for(var i=0;i<mapLoadInfo.otherMaps.length;i++) {
                            mapLoadInfo.otherMaps[i].cloneLayer(layer);
                        }
                        mapLoadInfo.otherMaps =[];
                    }
                },
                handleLayerSelect: function(layer) {
                    var args = this.layerSelectArgs;
                    if(!this.layerSelectPath) {
                        if(!args) {
                            this.map.onFeatureSelect(layer);
                            return;
                        }
                        //If args was defined then default to search
                        this.layerSelectPath = "/search/do";
                    }
                    var url = ramaddaBaseUrl + this.layerSelectPath;
                    if(args) {
                        var toks = args.split(",");
                        for(var i=0;i<toks.length;i++) {
                            var tok = toks[i];
                            var toktoks = tok.split(":");                            
                            var urlArg = toktoks[0];
                            var layerField = toktoks[1];
                            var attrs = layer.feature.attributes;
                            var fieldValue = null;
                            for (var attr in attrs) {
                                var attrName =""+ attr;
                                if(attrName == layerField) {
                                    var attrValue = null;
                                    if (typeof attrs[attr] == 'object' || typeof attrs[attr] == 'Object') {
                                        var o = attrs[attr];
                                        attrValue=  o["value"];
                                    } else {
                                        attrValue = attrs[attr];
                                    }
                                    url = HtmlUtil.appendArg(url, urlArg, attrValue);
                                    url = url.replace("${" + urlArg +"}", attrValue);
                                    //                                    console.log("Got the value:" + urlArg +"=" + attrValue);
                                }
                            }
                        }
                    }
                    url = HtmlUtil.appendArg(url, "output", "json");
                    console.log("Doing search: " + url);
                    var entryList = new EntryList(this.getRamadda(), url, null, false);
                    entryList.doSearch(this);
                    this.getEntryList().showMessage("Searching", HtmlUtil.div([ATTR_STYLE,"margin:20px;"], this.getWaitImage()));
                },
                getEntryList: function() {
                    if(!this.entryListDisplay) {
                        var props = {
                            showMenu: true,
                            showTitle: true,
                            showDetails:true,
                            layoutHere: false,
                            showForm: false,
                            doSearch: false,
                        };
                        var id = this.getUniqueId("display");
                        this.entryListDisplay = new RamaddaEntrylistDisplay(this.getDisplayManager(),id,props);
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
		addMapLayer : function(source, props) {
                    var _this = this;
                    var entry = props.entry;
                    //                    console.log("addMapLayer:" + entry.getName()+  " " + entry.getType());
                    if(!this.addedLayers) this.addedLayers = {}; 
                    if(this.addedLayers[entry.getId()]) {
                        var layer = this.addedLayers[entry.getId()];
                        if(layer) {
                            //                            console.log("removeKMLayer");
                            this.map.removeKMLLayer(layer);
                            this.addedLayers[entry.getId()] = null;
                        } 
                        return;
                    }
                    
                    if(entry.getType().getId() == "geo_shapefile") {
                        var bounds = createBounds(entry.getWest(),entry.getSouth(), entry.getEast(),entry.getNorth());
                        if(bounds.left<-180||bounds.right>180 || bounds.bottom<-90 || bounds.top>90) {
                            console.log("entry has bad bounds:" + entry.getName() +" " + bounds);
                            return;
                        }

                        var selectCallback = function(layer) {_this.handleLayerSelect(layer);}
                        var unselectCallback = function(layer) {_this.handleLayerUnselect(layer);}
                        var url = ramaddaBaseUrl + "/entry/show?output=shapefile.kml&entryid=" + entry.getId();
                        var layer = this.map.addKMLLayer(entry.getName(), url, true, selectCallback, unselectCallback);
                        this.addedLayers[entry.getId()] = layer;
                        bounds = this.map.transformLLBounds(bounds);
                        this.map.map.zoomToExtent(bounds, true);
                        return;
                    }

                    var baseUrl = entry.getAttributeValue("base_url");
                    if (!Utils.stringDefined(baseUrl)) {
                        console.log("No base url:" + entry.getId());
                        return;
                    }
                    var layer = entry.getAttributeValue("layer_name");
                    if(layer == null) {
                        layer = entry.getName();
                    }
                    console.log("layer:" + layer);
                    this.map.addWMSLayer(entry.getName(), baseUrl,  layer, false);
		},
		mapBoundsChanged : function() {
                    var bounds = this.map.map.calculateBounds();
			bounds = bounds.transform(this.map.sourceProjection,
					this.map.displayProjection);
			this.getDisplayManager().handleEventMapBoundsChanged(this, bounds);
		},
		addFeature : function(feature) {
                    this.features.push(feature);
                    feature.line = this.map.addPolygon("lines_"
                                                       + feature.source.getId(), RecordUtil
                                                       .clonePoints(feature.points), null);
		},
		xloadInitialData : function() {
                    if (this.getDisplayManager().getData().length > 0) {
                        this.handleEventPointDataLoaded(this, this.getDisplayManager()
                                                        .getData()[0]);
                    }
		},

		getContentsDiv : function() {
                    return HtmlUtil.div([ ATTR_CLASS, "display-contents", ATTR_ID,
                                          this.getDomId(ID_DISPLAY_CONTENTS) ], "");
		},
                handleEventEntryMouseover: function(source, args) {
                    id = args.entry.getId() +"_mouseover";
                    attrs = {
                        lineColor:"red",
                        fillColor:"red",
                        fillOpacity:0.5,
                        lineOpacity:0.5,
                        doCircle:true,
                        lineWidth:1,
                        fill: true,
                        circle: {
                            lineColor: "black"
                        },
                        polygon: {
                            lineWidth:4,
                        }
                    }
                    this.addOrRemoveEntryMarker(id, args.entry, true, attrs);
                },
                handleEventEntryMouseout: function(source, args) {
                    id = args.entry.getId() +"_mouseover";
                    this.addOrRemoveEntryMarker(id, args.entry, false);
                },
                handleEventAreaClear:  function() {
                    this.map.clearRegionSelector();
                },
		handleClick : function(theMap, lon, lat) {
                    if(this.doDisplayMap()) {
                        return;
                    }
                    this.getDisplayManager().handleEventMapClick(this, lon, lat);
		},

		getPosition : function() {
			var lat = $("#" + this.getDomId(ID_LATFIELD)).val();
			var lon = $("#" + this.getDomId(ID_LONFIELD)).val();
			if (lat == null)
				return null;
			return [ lat, lon ];
		},

		setInitMapBounds : function(north, west, south, east) {
                    if (!this.map) return;
                    this.map.centerOnMarkers(new OpenLayers.Bounds(west, south, east,
                                                                   north));
		},

		sourceToEntries : {},
		handleEventEntriesChanged : function(source, entries) {
                    //                    console.log("handleEventEntriesChanged");
                    if (source == this.lastSource) {
                        this.map.clearSelectionMarker();
                    }
                    if((typeof source.forMap)!="undefined" && !source.forMap) {
                        return;
                    }
                    var oldEntries = this.sourceToEntries[source.getId()];
                    if (oldEntries != null) {
                        for ( var i = 0; i < oldEntries.length; i++) {
                            var id = source.getId() + "_" + oldEntries[i].getId();
                            this.addOrRemoveEntryMarker(id, oldEntries[i], false);
                        }
                    }

                    this.sourceToEntries[source.getId()] = entries;

                    var markers = new OpenLayers.Layer.Markers("Markers");
                    var lines = new OpenLayers.Layer.Vector("Lines", {});
                    var north = -90, west=180,south=90,east = -180;
                    var didOne = false;
                    for ( var i = 0; i < entries.length; i++) {
                        var  entry = entries[i];
                        var id = source.getId() + "_" + entry.getId();
                        var mapEntryInfo  = this.addOrRemoveEntryMarker(id, entries[i], true);
                        if (entry.hasBounds()) {
                            if(entry.getNorth()>90 ||
                               entry.getSouth()<-90 ||
                               entry.getEast()>180 ||
                               entry.getWest()<-180) {
                                console.log("bad bounds on entry:" + entry.getName() +" " +
                                            entry.getNorth() + " " +
                                            entry.getSouth()+ " " +
                                            entry.getEast()+ " " +
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
                    var bounds = (didOne? createBounds(west, south, east, north):null);
                    this.map.centerOnMarkers(bounds, true);
		},
		handleEventEntrySelection : function(source, args) {
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
                addOrRemoveEntryMarker : function(id, entry, add, args) {
                    if(!args) {
                        args = {};
                    }
                    var dflt = {
                        lineColor: entry.lineColor,
                        fillColor: entry.lineColor,
                        lineWidth: entry.lineWidth,
                        doCircle:false,
                        doRectangle: this.showBoxes,
                        fill:false,
                        fillOpacity: 0.75,
                        pointRadius : 12,
                        polygon: {},
                        circle:{}
                    }
                    dfltPolygon = {
                    }
                    dfltCircle = {
                    }
                    $.extend(dflt, args);
                    if(!dflt.lineColor) dflt.lineColor = "blue";

                    $.extend(dfltPolygon, dflt);
                    if(args.polygon)
                        $.extend(dfltPolygon, args.polygon);
                    $.extend(dfltCircle, dflt);
                    if(args.circle)
                        $.extend(dfltCircle, args.circle);

                    //                    console.log("addOrRemoveEntryMarker");
                    var mapEntryInfo = this.mapEntryInfos[id];
                    if (!add) {
                        if (mapEntryInfo != null) {
                            mapEntryInfo.removeFromMap(this.map);
                            this.mapEntryInfos[id] = null;
                        }
                    } else  {
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
                            if(latitude<-90 || latitude>90 || longitude<-180 || longitude>180) {
                                return;
                            }
                            var point = new OpenLayers.LonLat(longitude, latitude);
                            if(dflt.doCircle) {
                                attrs = {pointRadius : dfltCircle.pointRadius, 
                                         stroke: true,
                                         strokeColor : dfltCircle.lineColor,
                                         strokeWidth : dfltCircle.lineWidth,
                                         fillColor: dfltCircle.fillColor,
                                         fillOpacity: dfltCircle.fillOpacity,
                                         fill: dfltCircle.fill,};
                                mapEntryInfo.circle = this.map.addPoint(id, point,attrs);
                            } else {
                                mapEntryInfo.marker = this.map.addMarker(id, point, entry.getIconUrl(), "", this.getEntryHtml(entry));
                            }
                            if(entry.polygon) {
                                var points = []
                                    for(var i=0;i<entry.polygon.length;i+=2) { 
                                        points.push(new OpenLayers.Geometry.Point(entry.polygon[i+1],entry.polygon[i]));
                                    }
                                var attrs = {
                                    strokeColor:dfltPolygon.lineColor,
                                    strokeWidth:Utils.isDefined(dfltPolygon.lineWidth)?dfltPolygon.lineWidth:2
                                };
                                mapEntryInfo.polygon =  this.map.addPolygon(id, entry.getName(), points, attrs, mapEntryInfo.marker);
                            }
                            var theDisplay = this;
                            if(mapEntryInfo.marker) {
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
		handleMapClick : function(marker) {
			if (this.selectedMarker != null) {
				this.getDisplayManager().handleEventEntrySelection(this, {
					entry : this.selectedMarker.entry,
					selected : false
				});
			}
			this.getDisplayManager().handleEventEntrySelection(this, {
				entry : marker.entry,
				selected : true
			});
			this.selectedMarker = marker;
		},
                getDisplayProp: function(source, prop, dflt) {
                    if(Utils.isDefined(this[prop])) {
                        return this[prop];
                    }
                    prop = "map-" + prop;
                    if(Utils.isDefined(source[prop])) {
                        return source[prop];
                    }
                    return source.getProperty(prop, dflt);
                },
                applyVectorMap: function() {
                    if(this.vectorMapApplied) {
                        return;
                    }
                    if(!this.doDisplayMap()) {
                        return;
                    }
                    if(!this.vectorLayer) {
                        //                        console.log("applyVectorMap-no vector yet");
                        return;
                    }
                    if(!this.points) {
                        //                        console.log("applyVectorMap-no points yet");
                        return;
                    }
                    //                    console.log("applyVectorMap");
                    this.vectorMapApplied = true;
                    var features = this.vectorLayer.features.slice();
                    var circles = this.points;
                    for (var i = 0; i < circles.length; i++) {
                        var circle = circles[i];
                        var center = circle.center;
                        var matchedFeature = null;
                        var index = -1;

                        for (var j = 0; j < features.length; j++) {
                            var feature = features[j];
                            var geometry = feature.geometry;
                            if(!geometry) {
                                break;
                            }
                            bounds = geometry.getBounds();
                            if(!bounds.contains(center.x,center.y)) {
                                continue;
                            }
                            if(geometry.components) {
                                for(var sub=0;sub<geometry.components.length;sub++) {
                                    comp = geometry.components[sub];
                                    bounds = comp.getBounds();
                                    if(!bounds.contains(center.x,center.y)) {
                                        continue;
                                    }
                                    if(comp.containsPoint && comp.containsPoint(center)) {
                                        matchedFeature = feature;
                                        index = j;
                                        break;
                                    }
                                }
                                if(matchedFeature)
                                    break;
                                continue;
                            }
                            if(!geometry.containsPoint) {
                                console.log("unknown geometry:" + geometry.CLASS_NAME);
                                continue;
                            }
                            if(geometry.containsPoint(center)) {
                                matchedFeature = feature;
                                index = j;
                                break;
                            }
                       }
                       if(matchedFeature) {
                           features.splice(index,1);
                           style = matchedFeature.style;
                           if(!style) style = {
                                   "stylename": "from display"
                               };
                           $.extend(style, circle.style);
                           matchedFeature.style = style;
                           matchedFeature.popupText =  circle.text;
                           matchedFeature.dataIndex = i;
                       } 
                    }
                    if((""+this.getProperty("pruneFeatures","")) == "true") {
                        this.vectorLayer.removeFeatures(features);
                        var dataBounds = this.vectorLayer.getDataExtent();
                        bounds = this.map.transformProjBounds(dataBounds);
                        this.map.centerOnMarkers(bounds,true);
                    } 
                    this.vectorLayer.redraw();
                },
                needsData:function() {
                    return true;
                },
               updateUI: function() {
                    this.haveCalledUpdateUI = true;
                    SUPER.updateUI.call(this);
                    if(!this.getDisplayReady()) {
                        console.log("not ready");
                        return;
                    }
                    
                    if(!this.hasData()) {
                        return;
                    }
                    var pointData = this.getPointData();
                    var records = pointData.getRecords();
                    if(records == null) {
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
                    if(points.length ==0) {
                        console.log("points.legnth==0");
                        return;
                    }

                    source = this;
                    var radius = parseFloat(this.getDisplayProp(source,"radius",8));
                    var strokeWidth = parseFloat(this.getDisplayProp(source,"strokeWidth","1"));
                    var strokeColor = this.getDisplayProp(source, "strokeColor", "#000");
                    var colorByAttr = this.getDisplayProp(source, "colorBy", null);
                    var colors = this.getColorTable();
                    var sizeByAttr = this.getDisplayProp(source, "sizeBy",null);
                    var isTrajectory =  this.getDisplayProp(source,"isTrajectory",false);
                    if(isTrajectory) {
                        this.map.addPolygon("id", points, null,null);
                        return;
                    }
                     if(!colors && source.colors && source.colors.length>0) {
                        colors = source.colors;
                        if(colors.length==1 &&  Utils.ColorTables[colors[0]]) {
                            colors = Utils.ColorTables[colors[0]];
                        }
                    }

                    if(colors == null) {
                        colors = Utils.ColorTables.grayscale;
                    }
                    var records = pointData.getRecords();

                    var colorBy = {
                        id:colorByAttr,
                        minValue:0,
                        maxValue:0,
                        field: null,
                        index:-1,
                    };

                    
                    var sizeBy = {
                        id:this.getDisplayProp(source,"sizeBy",null),
                        minValue:0,
                        maxValue:0,
                        field: null,
                        index:-1,
                    };

                    for(var i=0;i<fields.length;i++) {
                        var field = fields[i];
                        if(field.getId() == colorBy.id || ("#"+(i+1)) ==colorBy.id) {
                            colorBy.field = field;
                        }
                        if(field.getId() == sizeBy.id || ("#"+(i+1)) == sizeBy.id) {
                            sizeBy.field = field;
                        }
                    }


                    sizeBy.index = sizeBy.field!=null?sizeBy.field.getIndex():-1;
                    colorBy.index = colorBy.field!=null?colorBy.field.getIndex():-1;
                    var excludeZero = this.getProperty(PROP_EXCLUDE_ZERO,false);
                    for(var i=0;i<points.length;i++) {
                        var pointRecord  = records[i];
                        var tuple = pointRecord.getData();
                        var v = tuple[colorBy.index];
                        if(excludeZero && v == 0) {
                            continue;
                        }
                        if(i == 0 || v>colorBy.maxValue) colorBy.maxValue = v;
                        if(i == 0 || v<colorBy.minValue) colorBy.minValue = v;

                        v = tuple[sizeBy.index];
                        if(i == 0 || v>sizeBy.maxValue) sizeBy.maxValue = v;
                        if(i == 0 || v<sizeBy.minValue) sizeBy.minValue = v;
                    }


                    if(this.showPercent) {
                        colorBy.minValue = 0;
                        colorBy.maxValue = 100;
                    } 
                    colorBy.minValue = this.getDisplayProp(source, "colorByMin", colorBy.minValue);
                    colorBy.maxValue = this.getDisplayProp(source, "colorByMax", colorBy.maxValue);

                    //                    console.log("Color by:" + " Min: " + colorBy.minValue +" Max: " + colorBy.maxValue);

                    var dontAddPoint = this.doDisplayMap();
                    var didColorBy = false;
                    for(var i=0;i<points.length;i++) {
                        var pointRecord  = records[i];
                        var point = points[i];
                        var props = {
                            pointRadius:radius,
                            strokeWidth: strokeWidth,
                            strokeColor: strokeColor,
                        };

                        if(sizeBy.index>=0) {
                            var value = pointRecord.getData()[sizeBy.index];
                            var denom = (sizeBy.maxValue-sizeBy.minValue);
                            var percent = (denom == 0?NaN:(value-sizeBy.minValue)/denom);
                            props.pointRadius = 6 + parseInt(15*percent);
                            //                            console.log("percent:" + percent +  " radius: " + props.pointRadius +" Value: " + value  + " range: " + sizeBy.minValue +" " + sizeBy.maxValue);
                        }
                        if(colorBy.index>=0) {
                            var value = pointRecord.getData()[colorBy.index];
                            //                            console.log("value:" + value +" index:" + colorBy.index+" " + pointRecord.getData());
                            var percent = 0;
                            var msg = "";
                            var pctFields = null;
                            if(this.percentFields!=null) {
                                pctFields = this.percentFields.split(",");
                            }
                            if(this.showPercent) {
                                var total = 0;
                                var data= pointRecord.getData();
                                var msg ="";
                                for(var j=0;j<data.length;j++) {
                                    var ok = fields[j].isNumeric && !fields[j].isFieldGeo();
                                    if(ok && pctFields!=null) {
                                        ok =  pctFields.indexOf(fields[j].getId())>=0 ||
                                            pctFields.indexOf("#"+(j+1))>=0;
                                    }
                                    if(ok) {
                                        total+=data[j];
                                        msg += " " + data[j];
                                    }
                                }
                                if(total!=0) {
                                    percent0 = percent  = value/total*100;
                                    percent = (percent-colorBy.minValue)/(colorBy.maxValue-colorBy.minValue);
                                    //                                    console.log("%:" + percent0 +" range:" + percent +" value"+ value +" " + total+"data: " + msg);
                                }

                            } else {
                                percent = (value-colorBy.minValue)/(colorBy.maxValue-colorBy.minValue);
                            }

                            var index = parseInt(percent*colors.length);
                            //                            console.log(colorBy.index +" value:" + value+ " " + percent + " " +index + " " + msg);
                            if(index>=colors.length) index = colors.length-1;
                            else if(index<0) index = 0;
                            //                            console.log("value:" + value+ " %:" + percent +" index:" + index +" c:" + colors[index]);

                            props.fillOpacity=0.8;
                            props.fillColor = colors[index];
                            didColorBy = true;
                        }
                        var html = this.getRecordHtml(pointRecord,fields);
                        point = this.map.addPoint("pt-"  + i, point, props, html,dontAddPoint);
                        if(!this.points) 
                            this.points=[];
                        this.points.push(point);
                    }
                    if(didColorBy)
                        this.displayColorTable(ID_BOTTOM, colorBy.minValue, colorBy.maxValue);
                    this.applyVectorMap();
                },
		handleEventRemoveDisplay : function(source, display) {
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
		findFeature : function(source, andDelete) {
			for ( var i in this.features) {
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

		handleEventRecordSelection : function(source, args) {
                    var record = args.record;
                    if (record.hasLocation()) {
                        var latitude = record.getLatitude();
                        var longitude = record.getLongitude();
                        if(latitude<-90 || latitude>90 || longitude<-180 || longitude>180) return;
                        var point = new OpenLayers.LonLat(longitude, latitude);
                        var marker = this.myMarkers[source];
                        if (marker != null) {
                            this.map.removeMarker(marker);
                        }
                        var icon = displayMapMarkerIcons[source];
                        if(icon == null) {
                            icon =  displayMapGetMarkerIcon();
                            displayMapMarkerIcons[source] = icon;
                        }
                        this.myMarkers[source] = this.map.addMarker(source.getId(), point, icon, "", args.html,null,24);
                    }
		}
	});
}

function MapEntryInfo(entry) {
	RamaddaUtil.defineMembers(this, {
		entry : entry,
		marker : null,
		rectangle : null,
		removeFromMap : function(map) {
			if (this.marker != null) {
				map.removeMarker(this.marker);
			}
			if (this.rectangle != null) {
				map.removePolygon(this.rectangle);
			}
                        if(this.polygon!=null) {
				map.removePolygon(this.polygon);
                        }
                        if(this.circle!=null) {
                            map.removePoint(this.circle);
                        }
		}

	});
}
/**
Copyright 2008-2015 Geode Systems LLC
*/



function RamaddaXlsDisplay(displayManager, id, properties) {  

    var COORD_X = "xaxis";
    var COORD_Y= "yaxis";
    var COORD_GROUP= "group";


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
    if(properties!=null) {
        $.extend(this.tableProps, properties);
    }


    RamaddaUtil.defineMembers(this, {
            initDisplay: function() {
                this.createUI();
                this.setDisplayTitle("Table Data");
                var body = 
                    HtmlUtil.div(["id", this.getDomId(ID_SEARCH_HEADER)]) +
                    HtmlUtil.div(["id", this.getDomId(ID_TABLE_HOLDER)]) +
                    HtmlUtil.div(["id", this.getDomId(ID_CHARTTOOLBAR)]) +
                    HtmlUtil.div(["id", this.getDomId(ID_CHART)]);
                this.setContents(body);
                this.loadTableData(this.url);
            },
         });


    RamaddaUtil.defineMembers(this, {
            currentSheet:  0,
            currentData: null,
            columnLabels: null,
            startRow:0,
            groupIndex: -1,
            xAxisIndex: -1,
            yAxisIndex: -1,
            header: null,
            cellSelected: function(row, col) {
                this.startRow = row;
                if(this.jq("params-xaxis-select").attr("checked")) {
                    this.xAxisIndex = col;
                } else if(this.jq("params-group-select").attr("checked")) {
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
                return "params-" + root +"-label"
            },
           setAxisLabel: function(fieldId, lbl) {
                fieldId = this.getAxisLabelId(fieldId);
                var id= HtmlUtil.getUniqueId();
                if(lbl.length>25) {
                    lbl = lbl.substring(0,25) +"...";
                }
                if(lbl.trim()!="") {
                    lbl= HtmlUtil.span(["id",id,"class","ramadda-tag-box"], "&nbsp;&nbsp;" + lbl + "&nbsp;&nbsp;");
                }
                this.jq(fieldId).html(lbl);
            },
            loadSheet: function(sheetIdx) {
                
                var all = $("[id^=" + this.getDomId("sheet_")+"]");
                var sel = $("#" + this.getDomId("sheet_")+sheetIdx);

                all.css('font-weight','normal');
                sel.css('font-weight','bold');

                all.css('border','1px #ccc solid');
                sel.css('border','1px #666 solid');

                this.currentSheet = sheetIdx;
                var sheet = this.sheets[sheetIdx];
                if(sheet) {
                    var rows =sheet.rows.slice(0);
                    if(rows.length>0) {
                        this.header = rows[0];
                    }
                }

                var html = "";
                var _this = this;
                var args  = {
                    contextMenu: true,
                    stretchH: 'all',
                    colHeaders: true,
                    rowHeaders: true,
                    minSpareRows: 1,
                    afterSelection: function() {
                        if(arguments.length>2) {
                            for(var i=0;i<arguments.length;i++) {
                                //                                console.log("a[" + i +"]=" + arguments[i]);
                            }
                            var row = arguments[0];
                            var col = arguments[1];
                            _this.cellSelected(row, col);
                        }
                    },
                };
                $.extend(args, this.tableProps);
                if(this.tableProps.useFirstRowAsHeader) {
                    var headers = rows[0];
                    args.colHeaders = headers;
                    rows = rows.splice(1);
                }
                for(var i=0;i<this.tableProps.skipRows;i++) {
                    rows = rows.splice(1);
                }

                if(rows.length==0) {
                    this.displayMessage("No data found");
                    this.jq(ID_RESULTS).html("");
                    return;
                }

                this.jq(ID_RESULTS).html("Found: " + rows.length);
                args.data = rows;
                this.currentData = rows;

                if(this.tableProps.headers!=null) {
                    args.colHeaders = this.tableProps.headers;
                }

                if(this.getProperty("showTable",true)) {
                    this.jq(ID_TABLE).handsontable(args);
                }

            },
            getDataForSheet: function(sheetIdx, args) {
                var sheet = this.sheets[sheetIdx];
                var rows =sheet.rows.slice(0);
                if(rows.length>0) {
                    this.header = rows[0];
                }

                if(this.tableProps.useFirstRowAsHeader) {
                    var headers = rows[0];
                    if(args) {
                        args.colHeaders = headers;
                    }
                    rows = rows.splice(1);
                }
                for(var i=0;i<this.tableProps.skipRows;i++) {
                    rows = rows.splice(1);
                }
                return rows;
            },

            makeChart: function(chartType, props) {
                if(typeof google == 'undefined') {
                    this.jq(ID_CHART).html("No google chart available");
                    return;
                }

                if(props==null) props = {};
                var xAxisIndex = Utils.getDefined(props.xAxisIndex, this.xAxisIndex);
                var groupIndex = Utils.getDefined(props.groupIndex, this.groupIndex);
                var yAxisIndex = Utils.getDefined(props.yAxisIndex, this.yAxisIndex);

                //                console.log("y:" + yAxisIndex +" props:" + props.yAxisIndex);

                if(yAxisIndex<0) {
                    alert("You must select a y-axis field.\n\nSelect the desired axis with the radio button.\n\nClick the column in the table to chart.");
                    return;
                }

                var sheetIdx  = this.currentSheet;
                if(!(typeof props.sheet == "undefined")) {
                    sheetIdx = props.sheet;
                }

                var rows = this.getDataForSheet(sheetIdx);
                if(rows ==null) {
                    this.jq(ID_CHART).html("There is no data");
                    return;
                }


                //remove the first header row
                var rows =rows.slice(1);
                
                for(var i=0;i<this.startRow-1;i++) {
                    rows =rows.slice(1);
                }

                var subset = [];
                console.log("x:" + xAxisIndex +" " + " y:" + yAxisIndex +" group:" + groupIndex);
                for(var rowIdx=0;rowIdx<rows.length;rowIdx++) {
                    var row = [];
                    var idx = 0;
                    if(xAxisIndex>=0) {
                        row.push(rows[rowIdx][xAxisIndex]);
                    }   else {
                        row.push(rowIdx);
                    }
                    if(yAxisIndex>=0) {
                        row.push(rows[rowIdx][yAxisIndex]);
                    }
                    subset.push(row);
                    if(rowIdx<2)
                        console.log("row:" + row);
                }
                rows = subset;

                for(var rowIdx=0;rowIdx<rows.length;rowIdx++) {
                    var cols = rows[rowIdx];


                    for(var colIdx=0;colIdx<cols.length;colIdx++) {
                        var value = cols[colIdx]+"";
                        cols[colIdx] = parseFloat(value.trim());
                    }
               }


                var lbl1 = this.getHeading(xAxisIndex,true);
                var lbl2 = this.getHeading(yAxisIndex, true);
                var lbl3 = this.getHeading(groupIndex,true);
                this.columnLabels = [lbl1,lbl2];


                var labels = this.columnLabels!=null?this.columnLabels:["Field 1","Field 2"];
                rows.splice(0,0,labels);
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
                var   chartOptions = {};
                var width = "95%";
                $.extend(chartOptions, {
                      legend: { position: 'top' },
                 });

                if(this.header!=null) {
                    if(xAxisIndex>=0) {
                        chartOptions.hAxis =  {title: this.header[xAxisIndex]};
                    }
                    if(yAxisIndex>=0) {
                        chartOptions.vAxis =  {title: this.header[yAxisIndex]};
                    }
                }

                var chartDivId = HtmlUtil.getUniqueId();
                var divAttrs = ["id",chartDivId];
                if(chartType == "scatterplot") {
                    divAttrs.push("style");
                    divAttrs.push("width: 450px; height: 450px;");
                }
                this.jq(ID_CHART).append(HtmlUtil.div(divAttrs));

                if(chartType == "barchart") {
                    chartOptions.chartArea = {left:75,top:10,height:"60%",width:width};
                    chartOptions.orientation =  "horizontal";
                    this.chart = new google.visualization.BarChart(document.getElementById(chartDivId));
                } else  if(chartType == "table") {
                    this.chart = new google.visualization.Table(document.getElementById(chartDivId));
                } else  if(chartType == "motion") {
                    this.chart = new google.visualization.MotionChart(document.getElementById(chartDivId));
                } else  if(chartType == "scatterplot") {
                    chartOptions.chartArea = {left:50,top:30,height:400,width:400};
                    chartOptions.legend = 'none';
                    chartOptions.axisTitlesPosition = "in";
                    this.chart = new google.visualization.ScatterChart(document.getElementById(chartDivId));
                } else {
                    $.extend(chartOptions, {lineWidth: 1});
                    chartOptions.chartArea = {left:75,top:10,height:"60%",width:width};
                    this.chart = new google.visualization.LineChart(document.getElementById(chartDivId));
                }
                if(this.chart!=null) {
                    this.chart.draw(dataTable, chartOptions);
                }
            },

            addNewChartListener: function(makeChartId, chartType) {
                var _this = this;
                $("#" + makeChartId+"-" + chartType).button().click(function(event){
                        console.log("make chart:" + chartType);
                        _this.makeChart(chartType);
                    });
            },

            makeSheetButton: function(id, index) {
                var _this = this;
                $("#" + id).button().click(function(event){
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
                if(index<0) return "";
                if(this.header != null && index>=0 && index< this.header.length) {
                    var v=  this.header[index];
                    v  = v.trim();
                    if(v.length>0) return v;
                }
                if(doField)
                    return "Field " + (index+1);
                return "";
            },
            showTableData: function(data) {
                var _this = this;

                this.data = data;
                this.sheets = this.data.sheets;
                this.columns = data.columns;



                var buttons = "";
                for(var sheetIdx =0;sheetIdx<this.sheets.length;sheetIdx++) {
                    var id = this.getDomId("sheet_"+ sheetIdx);
                    buttons+=HtmlUtil.div(["id", id,"class","ramadda-xls-button-sheet"],
                                          this.sheets[sheetIdx].name);

                    buttons += "\n";
                }

                var weight = "12";

                var tableHtml =  "<table width=100% style=\"max-width:1000px;\" > ";
                if(this.sheets.length>1) {
                    weight = "10";
                }

                tableHtml +=  "<tr valign=top>";

                if(this.sheets.length>1) {
                    //                    tableHtml += HtmlUtil.openTag(["class","col-md-2"]);
                    tableHtml += HtmlUtil.td(["width","110"],HtmlUtil.div(["class","ramadda-xls-buttons"], buttons));
                    weight = "10";
                }


                var makeChartId =  HtmlUtil.getUniqueId();

                var tableWidth = this.getProperty("tableWidth", "");
                var tableHeight = this.getProperty("tableHeight", "300px");

                var style = "";
                if(tableWidth!="") {
                    style += " width:" + tableWidth + ";";
                }
                style += " height: "+ tableHeight +";";
                style += " overflow: auto;";
                tableHtml += HtmlUtil.td([],HtmlUtil.div(["id",this.getDomId(ID_TABLE),"class","ramadda-xls-table","style",style]));


                tableHtml += "</tr>";
                tableHtml += "</table>";
                
                var chartToolbar  = "";
                var chartTypes = ["barchart","linechart","scatterplot"];
                for(var i=0;i<chartTypes.length;i++) {
                    chartToolbar+=HtmlUtil.div(["id", makeChartId+"-" + chartTypes[i],"class","ramadda-xls-button"],  "Make " + chartTypes[i]);
                    chartToolbar+= "&nbsp;";
                }

                chartToolbar+= "&nbsp;";
                chartToolbar+=HtmlUtil.div(["id", this.getDomId("removechart"),"class","ramadda-xls-button"],  "Clear Charts");


                chartToolbar += "<p>";
                chartToolbar += "<form>Fields: ";
                chartToolbar +=  "<input type=radio checked name=\"param\" id=\"" + this.getDomId("params-yaxis-select")+"\"> y-axis:&nbsp;" +
                    HtmlUtil.div(["id", this.getDomId("params-yaxis-label"), "style","border-bottom:1px #ccc dotted;min-width:10em;display:inline-block;"], "");

                chartToolbar += "&nbsp;&nbsp;&nbsp;";
                chartToolbar += "<input type=radio  name=\"param\" id=\"" + this.getDomId("params-xaxis-select")+"\"> x-axis:&nbsp;" +
                    HtmlUtil.div(["id", this.getDomId("params-xaxis-label"), "style","border-bottom:1px #ccc dotted;min-width:10em;display:inline-block;"], "");

                chartToolbar += "&nbsp;&nbsp;&nbsp;";
                chartToolbar += "<input type=radio  name=\"param\" id=\"" + this.getDomId("params-group-select")+"\"> group:&nbsp;" +
                    HtmlUtil.div(["id", this.getDomId("params-group-label"), "style","border-bottom:1px #ccc dotted;min-width:10em;display:inline-block;"], "");
 
               chartToolbar+= "</form>";

                if(this.getProperty("showSearch",true)) {
                    var results = HtmlUtil.div(["style","display:inline-block;","id",this.getDomId(ID_RESULTS)],"");
                    var download = HtmlUtil.div(["style","display:inline-block;","id",this.getDomId(ID_DOWNLOADURL)]);
                    var searchDiv =  HtmlUtil.div(["id", this.getDomId(ID_SEARCH_DIV),"class","ramadda-xls-search-form"]);


                    var search = "";
                    search += HtmlUtil.openTag("form",["action","#","id",this.getDomId(ID_SEARCH_FORM)]);
                    search += HtmlUtil.image(icon_tree_closed,["id",this.getDomId(ID_SEARCH+"_open")]);
                    search += "\n";
                    search += HtmlUtil.input(ID_SEARCH_TEXT, this.jq(ID_SEARCH_TEXT).val(),["size","60", "id", this.getDomId(ID_SEARCH_TEXT),"placeholder","Search"]);
                    search += "<input type=submit name='' style='display:none;'>";

                    search += HtmlUtil.openTag("div", ["id", this.getDomId(ID_SEARCH_EXTRA),"class","ramadda-xls-search-extra"],"");                    
                    if(this.columns) {
                        var extra = HtmlUtil.openTag("table", ["class","formtable"]);
                        for(var i =0;i<this.columns.length;i++) {
                            var col = this.columns[i];
                            var id = ID_SEARCH_PREFIX+"_" + col.name;
                            var widget = HtmlUtil.input(id, this.jq(id).val(), ["id", this.getDomId(id),"placeholder","Search"]);
                            extra += HtmlUtil.formEntry(col.name.replace("_"," ")+":",widget);
                        }
                        extra += HtmlUtil.closeTag("table");
                        search += extra;
                    }


                    if(this.searchFields) {
                        var extra = HtmlUtil.openTag("table", ["class","formtable"]);
                        for(var i =0;i<this.searchFields.length;i++) {
                            var col = this.searchFields[i];
                            var id = ID_SEARCH_PREFIX+"_" + col.name;
                            var widget = HtmlUtil.input(id, this.jq(id).val(), ["id", this.getDomId(id),"placeholder","Search"]);
                            extra += HtmlUtil.formEntry(col.label+ ":",widget);
                        }
                        extra += HtmlUtil.closeTag("table");
                        search += extra;
                    }

                    


                    search += "\n";
                    search+= HtmlUtil.closeTag("div");
                    search += "\n";
                    search+= HtmlUtil.closeTag("form");

                    this.jq(ID_SEARCH_HEADER).html(HtmlUtil.leftRight(searchDiv,results+" " + download));

                    this.jq(ID_SEARCH_DIV).html(search);

                    if(!this.extraOpen) {
                        this.jq(ID_SEARCH_EXTRA).hide();
                    } 


                    this.jq(ID_SEARCH+"_open").button().click(function(event){
                            _this.jq(ID_SEARCH_EXTRA).toggle();
                            _this.extraOpen = !_this.extraOpen;
                            if(_this.extraOpen) {
                                _this.jq(ID_SEARCH+"_open").attr("src",icon_tree_open);
                            } else {
                                _this.jq(ID_SEARCH+"_open").attr("src",icon_tree_closed);
                            }
                        });
                
                }


               if(this.getProperty("showTable",true)) {
                   this.jq(ID_TABLE_HOLDER).html(tableHtml);
                   chartToolbar += "<br>";
                   if(this.getProperty("showChart",true)) {
                       this.jq(ID_CHARTTOOLBAR).html(chartToolbar);
                   }
               }

                if(this.getProperty("showSearch",true)) {
                    this.jq(ID_SEARCH_FORM).submit(function( event ) {
                            event.preventDefault();
                            _this.loadTableData(_this.url,"Searching...");
                        });
                    this.jq(ID_SEARCH_TEXT).focus();
                    this.jq(ID_SEARCH_TEXT).select();
                }


                for(var i=0;i<chartTypes.length;i++) {
                    this.addNewChartListener(makeChartId, chartTypes[i]);
                }
                this.jq("removechart").button().click(function(event){
                        _this.clear();
                    });

              for(var sheetIdx =0;sheetIdx<this.sheets.length;sheetIdx++) {
                  var id = this.getDomId("sheet_"+ sheetIdx);
                  this.makeSheetButton(id, sheetIdx);
                }
                var sheetIdx = 0;
                var rx = /sheet=([^&]+)/g;
                var arr = rx.exec(window.location.search);
                if(arr) {
                    sheetIdx =  arr[1]; 
                }
                this.loadSheet(sheetIdx);


                if(this.defaultCharts) {
                    for(var i=0;i<this.defaultCharts.length;i++) {
                        var dflt  =this.defaultCharts[i];
                        this.makeChart(dflt.type,dflt);
                    }
                }
                this.setAxisLabel("params-yaxis-label", this.getHeading(this.yAxisIndex, true));

                this.displayDownloadUrl();

            },
             displayMessage: function(msg, icon) {
                if(!icon) {
                    icon = icon_information;
                }
                var html  = HtmlUtil.hbox(HtmlUtil.image(icon,["align","left"]),
                                          HtmlUtil.inset(msg,10,10,5,10));
                html = HtmlUtil.div(["class","note"],html);
                this.jq(ID_TABLE_HOLDER).html(html);
           },
           displayDownloadUrl: function() {
                var url = this.lastUrl;
                if(url == null) {
                    this.jq(ID_DOWNLOADURL).html("");
                    return
                }
                url = url.replace("xls_json","media_tabular_extractsheet");
                url += "&execute=true";
                var img = HtmlUtil.image(ramaddaBaseUrl +"/icons/xls.png",["title","Download XLSX"]);
                this.jq(ID_DOWNLOADURL).html(HtmlUtil.href(url,img));
            },
            loadTableData:  function(url, message) {
                this.url = url;
                if(!message) message = this.getLoadingMessage();
                this.displayMessage(message, icon_progress);
                var _this = this;

                var text = this.jq(ID_SEARCH_TEXT).val();
                if(text && text!="") {
                    url = url + "&table.text=" + encodeURIComponent(text);
                }
                if(this.columns) {
                    for(var i =0;i<this.columns.length;i++) {
                        var col = this.columns[i];
                        var id = ID_SEARCH_PREFIX+"_" + col.name;
                        var text = this.jq(id).val();
                        if(text) {
                            url = url + "&table." + col.name +"=" + encodeURIComponent(text);
                        }
                    }
                }

                if(this.searchFields) {
                    for(var i =0;i<this.searchFields.length;i++) {
                        var col = this.searchFields[i];
                        var id = ID_SEARCH_PREFIX+"_" + col.name;
                        var text = this.jq(id).val();
                        if(text) {
                            url = url + "&table." + col.name +"=" + encodeURIComponent(text);
                        }
                    }
                }



                console.log("url:" + url);
                this.lastUrl = url;

                var jqxhr = $.getJSON(url, function(data) {
                        if(GuiUtils.isJsonError(data)) {
                            _this.displayMessage("Error: " + data.error);
                            return;
                        }
                        _this.showTableData(data);
                    })
                    .fail(function(jqxhr, textStatus, error) {
                            var err = textStatus + ", " + error;
                            _this.displayMessage("An error occurred: " + error);
                            console.log("JSON error:" +err);
                        });
            }
        });

     }

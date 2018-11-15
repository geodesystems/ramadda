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



function addRamaddaDisplay(display) {
    if(window.globalDisplays == null) {
        window.globalDisplays = {};
    }
    window.globalDisplays[display.getId()] = display;
    if(display.displayId) {
        window.globalDisplays[display.displayId] = display;
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
       formatNumber: function(number) {
          if(!this.getProperty("format", true)) return number;
          return Utils.formatNumber(number);
            },
       getProperty: function(key, dflt) {
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
            notifyEvent:function(func, source, data) {
                if(this[func] == null) { return;}
                this[func].apply(this, [source,data]);
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
            setEntry: function(entry) {
                this.entries = [];
                this.addEntry(entry);
                this.entry = entry;
                this.entryId = entry.getId();
                if(this.properties.data) {
                    this.dataCollection = new DataCollection();
                    this.properties.data= this.data= new PointData(entry.getName(), null, null, this.getRamadda().getRoot()+"/entry/show?entryid=" + entry.getId() +"&output=points.product&product=points.json&numpoints=5000",{entryId:this.entryId});
                    this.data.loadData(this);
                }
                this.updateUI();
                var title = "";
                if(this.getShowTitle()) {
                    title= entry.getName();
                    console.log(title);
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
            getLoadingMessage: function() {
                return HtmlUtil.div(["text-align","center"], this.getMessage("&nbsp;Loading data..."));
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
                    var tuple = tuples[rowIdx];
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

                    if(this.canDoMultiFields() && fields.length>0) {
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
                                for(var fIdx=0;fIdx<argFields.length;fIdx++) {
                                    if(argFields[fIdx].getId() == field.getId()) {
                                        //                                        console.log(" > Override:" + field.getId());
                                        on = true;
                                        this.overrideFields.push(field.getId());
                                        break;
                                    }
                                }
                            } else if(fixedFields!=null) {
                                on = (fixedFields.indexOf(field.getId())>=0);
                                if(!on) {
                                    on = (fixedFields.indexOf("#" + (tupleIdx+1))>=0);
                                }
                                //                                if(on)
                                //                                    console.log(" > fixed:" + field.getId());
                            } else if(this.overrideFields!=null) {
                                on = this.overrideFields.indexOf(field.getId())>=0;
                                if(!on) {
                                    on = (this.overrideFields.indexOf("#" + (tupleIdx+1))>=0);
                                }
                            } else {
                                //                                console.log("Field:" +  field.getId());
                                if(selectedIds.length>0) {
                                    on = selectedIds.indexOf(field.getId())>=0;
                                } else {
                                    if(this.selectedCbx.indexOf(field.getId())>=0) {
                                        on = true;
                                    }  else if(this.selectedCbx.length==0) {
                                        on = (i==0);
                                    }
                                }
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
                                html += HtmlUtil.tag(TAG_DIV, [ATTR_TITLE, field.getId()],
                                                     HtmlUtil.checkbox(field.checkboxId, ["class", checkboxClass],
                                                                       on) +" " +label
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
                        theDisplay.fieldSelected(event);
                    });

                $("." + groupByClass).change(function(event) {
                        theDisplay.groupBy = $(this).val();
                        theDisplay.displayData();
                    });



            },
            fieldSelectionChanged: function() {
                if(this.displayData) {
                    this.displayData();
                }
            },
            defaultSelectedToAll: function() {
                return false;
            },
            setSelectedFields: function(fields) {
                this.selectedFields = fields;
                this.addFieldsCheckboxes(fields);
            },
            getSelectedFields:function(dfltList) {
                this.lastSelectedFields =  this.getSelectedFieldsInner(dfltList);
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
                if(fixedFields!=null) {
                    if(fixedFields.length==0) {
                        fixedFields = null;
                    } 
                }

                for(var collectionIdx=0;collectionIdx<dataList.length;collectionIdx++) {             
                    var pointData = dataList[collectionIdx];
                    var fields = this.getFieldsToSelect(pointData);
                    if(fixedFields !=null) {
                        for(i=0;i<fields.length;i++) { 
                            var field = fields[i];
                            var id = field.getId();
                            if(fixedFields.indexOf(id)>=0 ||
			       fixedFields.indexOf("#"+ (i+1))>=0) {
                                df.push(field);
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
                        }
                        if(cbx.is(':checked')) {
                            //                            console.log("cbx is on " + field.getId());
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
                //                console.log("selectedCbx:" + this.selectedCbx +" exists:" + cbxExists +" df:" + df.length);

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
                //                console.log(wiki);
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
                //                console.log(wiki);
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
                for(var i=0;i<entries.length;i++) {
                    even = !even;
                    var entry = entries[i];
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
                        if(true) return;
                        var entryId = $( this ).attr(ATTR_ENTRYID);
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
                //                console.log("loadInitialData");
                if(!this.needsData() || this.properties.data==null) {
                    //                    console.log(" returning " + this.needsData() );
                    return;
                } 
                if(this.properties.data.hasData()) {
                    //                    console.log(" hasData");
                    this.addData(this.properties.data);
                    return;
                } 
                this.properties.data.derived = this.derived;
                //                console.log("calling loadData");
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
                var myalign = 'right top';
                var atalign = 'right bottom';
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
            initUI:function() {
                this.checkFixedLayout();
            },
            initDisplay:function() {
                this.initUI();
                this.setContents("<p>default html<p>");
            },
            updateUI: function(data) {
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
                    html += HtmlUtil.openDiv(["class","display-contents","style","width:" + width +";"]);
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
                        //xxxx
                        label = HtmlUtil.href(this.getRamadda().getEntryUrl(this.entryId),title);
                    }
                    titleDiv = HtmlUtil.tag("div", [ATTR_CLASS,"display-title",ATTR_ID,this.getDomId(ID_TITLE)], label);
                    if(button== "") {
                        html += titleDiv;
                    } else {
                        html += "<table class=display-header-table cellspacing=0 cellpadding=0 width=100%><tr><td>" + titleDiv +"</td><td align=right>" + button +"</td></tr></table>";
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
            getContentsDiv: function() {
                var extraStyle = "";
                var width = this.getWidth();
                if(width>0) {
                    extraStyle += "width:" + width +"px; /*overflow-x: auto;*/";
                }
                var height = this.getProperty("height",-1);
                if(height>0) {
                    extraStyle += " height:" + height +"px; " + " max-height:" + height +"px; /*overflow-y: auto;*/";
                }
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
            setDisplayTitle: function(title) {
                var text = title;
                if(this.entryId) {
                    //xxx
                    text = HtmlUtil.href(this.getRamadda().getEntryUrl(this.entryId),title);
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
                //                console.log("pointDataLoaded  reload:" + reload);
                if(!reload) {
                    this.addData(pointData);
                }
                this.updateUI(pointData);
                if(!reload) {
                    this.lastPointData = pointData;
                    this.getDisplayManager().handleEventPointDataLoaded(this, pointData);
                }
                if(url!=null) {
                    this.jsonUrl = url;
                } else {
                    this.jsonUrl = null;
                }
            },
            //get an array of arrays of data 
            getStandardData : function(fields, props) {
                var pointData = this.dataCollection.getList()[0];
                var excludeZero = this.getProperty(PROP_EXCLUDE_ZERO,false);
                if(fields == null) {
                    fields = pointData.getRecordFields();
                }
                if(props == null) {
                    props = {
                        includeIndex: true,
                        includeIndexIfDate: false,
                        groupByIndex:-1,
                        raw: false,
                    };
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
                dataList.push(fieldNames);
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
                    console.log("dates: "  + this.minDateObj +" " + this.maxDateObj);
                }



                var offset = 0;
                if(Utils.isDefined(this.offset)) {
                    offset =  parseFloat(this.offset);
                }

                var nonNullRecords = 0;
                var indexField = this.indexField;
                var records = pointData.getRecords();

                //Check if there are dates and if they are different
                this.hasDate = false;
                var lastDate = null;
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



                var rowCnt = -1;
                var date_formatter;
                if (!( (typeof google === 'undefined') || (typeof google.visualization == 'undefined') )) {
                  var df = this.getProperty("dateFormat",null);
                  if (df) {
                    date_formatter = new google.visualization.DateFormat({ 
                         pattern: df,
                         timeZone: 0
                    }); 
                  }
                }
                for(j=0;j<records.length;j++) { 
                    var record = records[j];
                    var date = record.getDate();
                    if(date!=null) {
                        if(this.minDateObj!=null && date.getTime()< minDate) {
                            //                            console.log("skipping min:" + date +"  -- " + date.getTime() +" -- " + minDate);
                            continue;
                        }
                        if(this.maxDateObj!=null && date.getTime()> maxDate) {
                            //                            console.log("skipping max:" + date);
                            continue;
                        }
                    }

                    rowCnt++;


                    var values = [];
                    if(props.includeIndex || props.includeIndexIfDate) {
                        var indexName = null;
                        var date = record.getDate();
                        if(indexField>=0) {
                            var field = allFields[indexField];
                            values.push(record.getValue(indexField)+offset);
                            indexName =  field.getLabel();
                        } else {
                            if(this.hasDate) {
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
                if (!this.googleLoaded()) return false;
                if(this.fmt_yyyy) return true;
                this.fmt_yyyymmddhhmm =   new google.visualization.DateFormat({pattern: "yyyy-MM-dd HH:mm'Z'",timeZone:0});
                this.fmt_yyyymmdd =   new google.visualization.DateFormat({pattern: "yyyy-MM-dd",timeZone:0});
                this.fmt_yyyy =   new google.visualization.DateFormat({pattern: "yyyy",timeZone:0});
                return true;
            },
            getDateValue: function(arg, formatter) {
                if (!this.initDateFormats()) return arg;
                date = new Date(arg);
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
            addDisplay: function(display) {
                this.displays.push(display);
                this.doLayout();
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


                this.initDisplay();
            },
            initDisplay: function() {
                for(var i=0;i<this.displays.length;i++) {
                    this.displays[i].initDisplay();
                }
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





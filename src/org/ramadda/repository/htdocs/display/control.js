/**
   Copyright 2008-2019 Geode Systems LLC
*/


const DISPLAY_FILTER = "filter";
const DISPLAY_ANIMATION = "animation";
const DISPLAY_LABEL = "label";
const DISPLAY_DOWNLOAD = "download";
const DISPLAY_LEGEND = "legend";
const DISPLAY_RELOADER = "reloader";
const DISPLAY_MESSAGE = "message";
const DISPLAY_FIELDSLIST = "fieldslist";
const DISPLAY_TICKS = "ticks";
const DISPLAY_MENU = "menu";

addGlobalDisplayType({
    type: DISPLAY_DOWNLOAD,
    label: "Download",
    requiresData: true,
    forUser: true,
    category: CATEGORY_CONTROLS,
    tooltip: makeDisplayTooltip("Show a download link",null,"Allows user to select fields and<br>download CSV and JSON")                                        
});
addGlobalDisplayType({
    type: DISPLAY_RELOADER,
    label: "Reloader",
    requiresData: true,
    forUser: true,
    category: CATEGORY_CONTROLS,
    tooltip: makeDisplayTooltip("Reload the displays",null,"Automatically reloads the displays on a set frequency")                                            
});
addGlobalDisplayType({
    type: DISPLAY_FILTER,
    label: "Filter",
    requiresData: false,
    category: CATEGORY_CONTROLS,
    tooltip: makeDisplayTooltip("No data, just provides data filtering")
});
addGlobalDisplayType({
    type: DISPLAY_ANIMATION,
    label: "Animation",
    requiresData: true,
    category: CATEGORY_CONTROLS,
    tooltip: makeDisplayTooltip("Steps through time to drive other displays","animation.png","")                                                        
});
addGlobalDisplayType({
    type: DISPLAY_MESSAGE,
    label: "Message",
    requiresData: true,
    forUser: true,
    category: CATEGORY_CONTROLS,
    tooltip: makeDisplayTooltip("No data, just a formatted message",null,"")                                                    
});
addGlobalDisplayType({
    type: DISPLAY_MENU,
    label: "Menu",
    requiresData: true,
    forUser: true,
    category: CATEGORY_CONTROLS,
    tooltip: makeDisplayTooltip("Shows records in a menu to be selected")
});

addGlobalDisplayType({
    type: DISPLAY_FIELDSLIST,
    label: "Fields List",
    requiresData: true,
    forUser: true,
    category: CATEGORY_CONTROLS,
//    tooltip: makeDisplayTooltip("No data, just a formatted message",null,"")                                                    
});

addGlobalDisplayType({
    type: DISPLAY_LABEL,
    label: "Label",
    requiresData: false,
    category: CATEGORY_CONTROLS,
    tooltip: makeDisplayTooltip("No data, just a text label",null,"Useful to add text to the display layout")                                                
});
addGlobalDisplayType({
    type: DISPLAY_LEGEND,
    label: "Legend",
    requiresData: false,
    forUser: true,
    category: CATEGORY_CONTROLS,
    tooltip: makeDisplayTooltip("No data, just a configurable legend","legend.png")
});
addGlobalDisplayType({
    type: DISPLAY_TICKS,
    label: "Ticks",
    requiresData: true,
    forUser: true,
    category: CATEGORY_CONTROLS,
    tooltip: makeDisplayTooltip("Shows records as ticks in a timeline","ticks.png")
});

function RamaddaFilterDisplay(displayManager, id, properties) {
    let SUPER  = new RamaddaDisplay(displayManager, id, DISPLAY_FILTER, properties);
    let myProps =[];
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
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
    let SUPER  = new RamaddaDisplay(displayManager, id, DISPLAY_ANIMATION, properties);
    let myProps =[];
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
        running: false,
        timestamp: 0,
        index: 0,
        sleepTime: 500,
        iconStart: "fa-play",
        iconStop: "fa-stop",
        iconBack: "fa-step-backward",
        iconForward: "fa-step-forward",
        iconSlower: "fa-minus",
	iconFaster: "fa-plus",
	iconBegin: "fa-fast-backward",
	iconEnd: "fa-fast-forward",
        needsData: function() {
            return true;
        },
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
	    let records = this.currentRecords;
            if (records == null) {
                $("#" + this.getDomId(ID_TIME)).html("No data");
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
                label += HU.b("Date:") + " " + dttm;
            } else {
                label += HU.b("Index:") + " " + this.index;
            }
            $("#" + this.getDomId(ID_TIME)).html(label);
            if (propagate) {
		this.propagateEventRecordSelection({record: record});
            }
        },
        handleEventRecordSelection: function(source, args) {
	    if(!args.record) return;
	    let records = this.currentRecords;
	    if(!records) {
		if(args.record) records = [args.record];
	    }
	    records.every((r,idx)=>{
		if(r.getId() == args.record.getId()) {
		    this.index = idx;
		    this.applyStep(false);
		    return false;
		}
		return true;
	    });
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
            $("#" + this.getDomId(ID_START)).html(HU.getIconImage(this.iconStop));
            this.tick();
        },
        stop: function() {
            if (!this.running) return;
            this.running = false;
            this.timestamp++;
            $("#" + this.getDomId(ID_START)).html(HU.getIconImage(this.iconStart));
        },
        updateUI: function() {
	    let records = this.filterData();
	    if(!records) return;
	    this.currentRecords = records;
	    //            this.createUI();
            this.stop();

            var get = this.getGet();
            var html = "";
	    let c = "display-animation-button";
            html += HU.onClick(get + ".setIndex(0);", HU.div([ATTR_TITLE, "beginning", ATTR_CLASS, c],HU.getIconImage(this.iconBegin)));
            html += HU.onClick(get + ".deltaIndex(-1);", HU.div([ATTR_TITLE, "step back", ATTR_CLASS, c],HU.getIconImage(this.iconBack)));
            html += HU.onClick(get + ".toggle();", HU.div([ATTR_ID, this.getDomId(ID_START),ATTR_TITLE, "play/stop",ATTR_CLASS, c], HU.getIconImage(this.iconStart)));
            html += HU.onClick(get + ".deltaIndex(1);", HU.div([ATTR_TITLE, "step forward", ATTR_CLASS, c],HU.getIconImage(this.iconForward)));
            html += HU.onClick(get + ".setIndex();", HU.div([ATTR_TITLE, "end", ATTR_CLASS, c],HU.getIconImage(this.iconEnd)));
            html += HU.onClick(get + ".faster();", HU.div([ATTR_TITLE, "faster", ATTR_CLASS, c],HU.getIconImage(this.iconFaster)));
            html += HU.onClick(get + ".slower();", HU.div([ATTR_TITLE, "slower", ATTR_CLASS, c],HU.getIconImage(this.iconSlower)));
            html += HU.div(["style", "display:inline-block; min-height:24px; margin-left:10px;", ATTR_ID, this.getDomId(ID_TIME)], "&nbsp;");
            this.setDisplayTitle("Animation");
            this.setContents(html);
        },
    });
}



function RamaddaMessageDisplay(displayManager, id, properties) {
    const SUPER  = new RamaddaDisplay(displayManager, id, DISPLAY_MESSAGE, properties);
    let myProps =[];
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
        needsData: function() {
            return false;
        },
	updateUI: function() {
	    if(this.getProperty("decorate",true)) {
		this.setContents(this.getMessage(this.getProperty("message",this.getNoDataMessage())));
	    } else {
		this.setContents(this.getProperty("message",this.getNoDataMessage()));
	    }
	}});
}

function RamaddaFieldslistDisplay(displayManager, id, properties) {
    const ID_POPUP = "popup";
    const ID_POPUP_BUTTON = "popupbutton";    
    const SUPER  = new RamaddaDisplay(displayManager, id, DISPLAY_FIELDSLIST, properties);
    let myProps =[
	{label:"Metadata"},
	{p:"decorate",ex:true},
	{p:"asList",ex:true},
	{p:"reverseFields",ex:true},		
	{p:"selectable",ex:true},
	{p:"showFieldDetails",ex:true},
	{p:"showPopup",d:true,ex:false,tt:"Popup the selector"},	
	{p:"numericOnly",ex:true},
    ];
    
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
	needsData: function() {
            return true;
        },
	updateUI: function() {
	    let records = this.filterData();
	    if(records == null) return;
	    let html = "";
            let selectedFields = this.getFieldsByIds(null,this.getProperty("fields", ""));
	    if(this.selectedMap ==null)  {
		this.selectedMap={};
		if(selectedFields && selectedFields.length!=0) {
		    selectedFields.forEach(f=>{
			this.selectedMap[f.getId()] = true;
		    });
		} else {
		    selectedFields = null;
		}
	    } else {
		selectedFields = null;
	    }
            let fields =   this.getData().getRecordFields();
	    if(this.getNumericOnly()) {
		fields  = fields.filter(f=>{
		    return f.isFieldNumeric();
		});
	    }
	    if(this.getReverseFields()) {
		let tmp = [];
		fields.forEach(f=>{
		    tmp.unshift(f);
		});
		fields=tmp;
	    }
	    this.fields	     = fields;
	    this.fieldsMap={};
	    this.fields.forEach(f=>{
		this.fieldsMap[f.getId()] = f;
	    });
//	    html += HU.center("#" + records.length +" records");
	    let fs = [];
	    let clazz = " display-fields-field ";
	    let asList = this.getAsList();
	    if(this.getDecorate(true)) clazz+= " display-fields-field-decorated ";
	    if(asList)
		clazz+=" display-fields-list-field";
	    let selectable = this.getSelectable(true);
	    let details = this.getShowFieldDetails(false);	    
	    fields.forEach((f,idx)=>{
		let block  =f.getLabel();
		if(details) {
		    block+= "<br>" +
			f.getId() + f.getUnitSuffix()+"<br>" +
			f.getType();
		}
		let c = clazz;
		let selected = this.selectedMap[f.getId()];
		if(selectable) c += " display-fields-field-selectable ";
		if(selectable && selected) c += " display-fields-field-selected ";
		let title = "";
		if(selectable)
		    title = "Click to toggle. Shift-click toggle all";
		block =HU.div([TITLE,title,"field-selected",selected, "field-id", f.getId(),'class',c], block);
		fs.push(block);
	    });
	    let fhtml = Utils.wrap(fs,"","");
	    html += fhtml;

	    if(this.getShowPopup()) {
		html = HU.div([ID,this.domId(ID_POPUP_BUTTON)],"Select fields") +
		    HU.div([ID,this.domId(ID_POPUP),STYLE,"display:none;max-height:300px;overflow-y:auto;width:600px;"], html);
	    }
	    this.setContents(html);
	    if(this.getShowPopup()) {
		this.jq(ID_POPUP_BUTTON).button().click(()=>{
		    HU.makeDialog({contentId:this.domId(ID_POPUP),inPlace:true,anchor:this.domId(ID_POPUP_BUTTON),draggable:true,header:true,sticky:true});
		});
	    }
	    if(selectable) {
		let _this = this;
		let fieldBoxes = this.find(".display-fields-field");
		fieldBoxes.click(function(event) {
		    let shift = event.shiftKey ;
		    let selected  = $(this).attr("field-selected")=="true";
		    selected = !selected;
		    $(this).attr("field-selected",selected);
		    if(selected) {
			$(this).addClass("display-fields-field-selected");
		    } else {
			$(this).removeClass("display-fields-field-selected");
		    }

		    if(shift) {
			fieldBoxes.attr("field-selected",selected);
			if(selected) {
			    fieldBoxes.addClass("display-fields-field-selected");
			} else {
			    fieldBoxes.removeClass("display-fields-field-selected");
			}

		    }
		    let selectedFields = [];
		    fieldBoxes.each(function(){
			let selected  = $(this).attr("field-selected")=="true";
			if(selected) {
			    let id = $(this).attr("field-id");
			    let field = _this.fieldsMap[id];
			    if(field) selectedFields.push(field);
			}
		    });
		    _this.selectedMap = {};
		    selectedFields.forEach(f=>{
			_this.selectedMap[f.getId()] = true;
		    });
		    
		    setTimeout(()=>{
			_this.propagateEvent("handleEventFieldsSelected", selectedFields);
		    },20);
		});
	    }
	}});
}


function RamaddaLabelDisplay(displayManager, id, properties) {
    var ID_TEXT = "text";
    var ID_EDIT = "edit";

    if (properties && !Utils.isDefined(properties.showTitle)) {
        properties.showTitle = false;
    }
    this.text = "";
    this.editMode = properties.editMode;
    if (properties.text) this.text = properties.text;
    else if (properties.label) this.text = properties.label;
    else if (properties.html) this.text = properties.html;
    if (properties["class"]) this["class"] = properties["class"];
    else this["class"] = "display-text";

    const SUPER  = new RamaddaDisplay(displayManager, id, DISPLAY_LABEL, properties);
    let myProps =[];
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
        initDisplay: function() {
            var theDisplay = this;
            this.createUI();
            var textClass = this["class"];
            if (this.editMode) {
                textClass += " display-text-edit ";
            }
            var style = "color:" + this.getTextColor("contentsColor") + ";";
            var html = HU.div([ATTR_CLASS, textClass, ATTR_ID, this.getDomId(ID_TEXT), "style", style], this.text);
            if (this.editMode) {
                html += HU.textarea(ID_EDIT, this.text, ["rows", 5, "cols", 120, ATTR_SIZE, "120", ATTR_CLASS, "display-text-input", ATTR_ID, this.getDomId(ID_EDIT)]);
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
}



function RamaddaLegendDisplay(displayManager, id, properties) {
    if(!properties.width) properties.width='100%';
    let SUPER =  new RamaddaDisplay(displayManager, id, DISPLAY_LEGEND, properties);
    RamaddaUtil.inherit(this,SUPER);
    let myProps = [
	{label:'Legend'},
	{p:'labels',ex:''},
	{p:'colors',ex:''},
	{p:'inBox',ex:'true'},
	{p:'labelColor',ex:'#fff'},
	{p:'labelColors',ex:'color1,color2,...'},
	{p:'orientation',ex:'vertical'}
    ];

    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
	needsData: function() {
            return false;
	},
	updateUI: function() {
	    let labels = this.getProperty("labels","").split(",");
	    let colors = this.getColorList();
	    let html = "";
	    let colorWidth = this.getProperty("colorWidth","20px");
	    let labelColor = this.getProperty("labelColor","#000");
	    let labelColors = this.getProperty("labelColors")?this.getProperty("labelColors").split(","):null;
	    let inBox = this.getProperty("inBox",false);
	    let orientation = this.getProperty("orientation","horizontal");
	    let delim = orientation=="horizontal"?" ":"<br>";
	    for(let i=0;i<labels.length;i++) {
		let label = labels[i];
		let color = colors[i]||"#fff";
		if(i>0) html+=delim;
		if(!inBox) {
		    html+=HU.div(["class","display-legend-item"], HU.div(["class","display-legend-color","style","background:" + color+";width:" + colorWidth+";"+
									  "height:15px;"]) +
				 HU.div(["class","display-legend-label"],label));
		} else {
		    let lc = labelColors?labelColors[i]:labelColor || labelColor;
		    html+=HU.div(["class","display-legend-color","style","margin-left:8px;background:" + color+";"],
				 HU.div(["class","display-legend-label","style","margin-left:8px;margin-right:8x;color:" + lc+";"],label));
		}
	    }
	    if(orientation!="vertical") {
		html = HU.center(html); 
	    }
	    this.setContents(html);
	},
    });
}



function RamaddaDownloadDisplay(displayManager, id, properties) {
    const SUPER =  new RamaddaFieldsDisplay(displayManager, id, DISPLAY_DOWNLOAD, properties);
    const ID_DOWNLOAD_CSV = "downloadcsv";
    const ID_DOWNLOAD_JSON = "downloadjson";    
    const ID_CANCEL = "cancel";    
    let myProps =[
	{label:'Download'},
	{p:'csvLabel',ex:'Download'},
	{p:'useIcon',d:'false',ex:'false'},
	{p:'fileName',d:'download',ex:'download'},
	{p:'askFields',d:'false',ex:'true'},
//	{p:'doSave',d:false,tt:'Show the save file button'}
    ];
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
	fieldOn:{},
	needsData: function() {
            return true;
	},
	updateUI: function() {
	    let label = this.getPropertyCsvLabel("Download Data");
	    label = label.replace("${title}",this.getProperty("title",""));
	    let useIcon = this.getPropertyUseIcon(true);
	    label = HU.span([ID,this.getDomId("csv")], useIcon?HU.getIconImage("fa-download",[STYLE,"cursor:pointer;",TITLE,label]):label);
	    /*
	    if(!Utils.isAnonymous() && this.getDoSave()) {
		label+=SPACE2 +HU.span([ID,this.domId("save"),CLASS,"ramadda-clickable"], HU.getIconImage("fas fa-save")) +SPACE +HU.span([ID,this.domId("savelabel")]);
	    }
	    */
	    this.setContents(HU.div([],label));
	    /*
	    if(!Utils.isAnonymous() && this.getDoSave()) {
		let _this  = this;
		this.jq("save").click(()=>{
		    if(!confirm("Are you sure you want to change the file?")) return;
		    let records = this.filterData();
		    let fields = this.getData().getRecordFields();
		    let csv = DataUtils.getCsv(fields, records);
		    let data = new FormData();
		    data.append("file",csv);
		    data.append("entryid",this.getProperty("entryId"));
		    jQuery.ajax({
			url: ramaddaBaseUrl+"/entry/setfile",
			data: data,
			cache: false,
			contentType: false,
			processData: false,
			method: 'POST',
			type: 'POST',
			success: function(data){
			    if(data.message)
				_this.jq("savelabel").html(data.message);
			    else if(data.error)
				_this.jq("savelabel").html(data.error);			    
			    else
				console.log("response:" + JSON.stringify(data));
			    setTimeout(()=>{
				_this.jq("savelabel").html("&nbsp;");
			    },3000);
			},
			fail: function(data) {
			    _this.jq("savelabel").html("An error occurred:" + data);			    
			    console.log("An error occurred:" + data);			    
			}
		    });
		});
	    }
*/
	    if(useIcon) {
		this.jq("csv").click(() => {
		    this.doDownload();
		});
	    } else {
		this.jq("csv").button().click(() => {
		    this.doDownload();
		});
	    }
	},
	getCsv: function(fields, records) {
            fields = fields || this.getData().getRecordFields();
	    let csv = DataUtils.getCsv(fields, records);
	    Utils.makeDownloadFile(this.getPropertyFileName()+".csv", csv);	    
	},
	getJson: function(fields, records) {
            fields = fields || this.getData().getRecordFields();
	    DataUtils.getJson(fields, records,this.getPropertyFileName()+".json");
	},

	applyFieldSelection: function() {
	    this.getData().getRecordFields().forEach(f=>{
		let cbx = this.jq("cbx_" + f.getId());
		let on = cbx.is(':checked');
		this.fieldOn[f.getId()] = on;
	    });
	},
	getDownloadDialog: function(records) {
            let selectedFields = this.getSelectedFields();
	    if(selectedFields) {
		this.fieldOn = {};
		this.getData().getRecordFields().forEach(f=>{
		    this.fieldOn[f.getId()] = false;
		});
		selectedFields.forEach(f=>{
		    this.fieldOn[f.getId()] = true;
		});
	    }
	    
	    let html = HU.center("#" +records.length +" records") +
		HU.center(HU.div([ID,this.getDomId(ID_DOWNLOAD_CSV)],"Download CSV") +"&nbsp;&nbsp;" +
			  HU.div([ID,this.getDomId(ID_DOWNLOAD_JSON)],"Download JSON") +"&nbsp;&nbsp;" +			     			     HU.div([ID,this.getDomId(ID_CANCEL)],"Cancel"));
	    html += "<b>Include:<br></b>";
	    let cbx = "";
	    this.getData().getRecordFields().forEach((f,idx)=>{
		let on = this.fieldOn[f.getId()];
		if(!Utils.isDefined(on)) {
		    on = true;
		}
		cbx += HU.checkbox(this.getDomId("cbx_" + f.getId()),[],on) +" " + f.getLabel() +"<br>";
	    });
	    
	    html = HU.div([STYLE,HU.css("margin","5px")],html);
	    html += HU.div([STYLE,HU.css("max-height","200px","overflow-y","auto","margin","5px")], cbx);
	    return html;
	},
	doDownload: function() {
	    let records = this.filterData();
	    let func = json=>{
		this.jq(ID_DIALOG).hide();
		let fields = [];
		this.applyFieldSelection();
		this.getData().getRecordFields().forEach(f=>{
		    if(this.fieldOn[f.getId()]) {
			fields.push(f);
		    }
		});
		if(json) 
		    this.getJson(fields, records);
		else
		    this.getCsv(fields, records);
		if(this.dialog) this.dialog.remove();
		this.dialog =null;	    };
	    if(this.getPropertyAskFields(true)) {
		let html = this.getDownloadDialog(records);
		let init = ()=>{
		    this.jq(ID_CANCEL).button().click(() =>{
			this.applyFieldSelection();
			this.jq(ID_DIALOG).hide();
			if(this.dialog) this.dialog.remove();
			this.dialog =null;
		    });
		    this.jq(ID_DOWNLOAD_CSV).button().click(() =>{
			func(false);
		    });
		    this.jq(ID_DOWNLOAD_JSON).button().click(() =>{
			func(true);
		    });		
		};
		this.showDialog(html,this.getDomId(ID_DISPLAY_CONTENTS),init,this.getTitle());
	    } else  {
		this.getCsv(null, records);
	    }
	},
    });
}

function RamaddaReloaderDisplay(displayManager, id, properties) {
    const ID_CHECKBOX= "cbx";
    const ID_COUNTDOWN= "countdown";
    const SUPER =  new RamaddaFieldsDisplay(displayManager, id, DISPLAY_RELOADER, properties);
    let myProps = [
	{label:'Reloader'},
	{p:'interval',ex:'30',d:30,label:"Interval"},
	{p:'showCheckbox',ex:'false',d:true,label:"Show Checkbox"},
	{p:'showCountdown',ex:'false',d:true,label:"Show Countdown"},	
    ];
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
        needsData: function() {
            return true;
        },
        xxpointDataLoaded: function(pointData, url, reload) {
	},
	reloadData: function() {
	    let pointData = this.dataCollection.getList()[0];
	    pointData.loadData(this,true);
	},
	updateUI: function() {
	    let html = "";
	    //If we are already displaying then don't update the UI
	    if(this.jq(ID_COUNTDOWN).length>0) return;
	    if(this.getPropertyShowCheckbox()) {
		html += HU.checkbox(this.getDomId(ID_CHECKBOX),[],true);
	    }		
	    if(this.getPropertyShowCountdown()) {
		html+=" " + HU.span([CLASS,"display-reloader-label", ID,this.getDomId(ID_COUNTDOWN)],this.getCountdownLabel(this.getPropertyInterval()));
	    } else {
		if(this.getPropertyShowCheckbox()) {
		    html+=" " + HU.span([ID,this.getDomId(ID_COUNTDOWN)],"Reload");
		}
	    }
	    this.setContents(html);
            this.clearDisplayMessage();
	    this.jq(ID_CHECKBOX).change(()=>{
		let cbx = this.jq(ID_CHECKBOX);
		if(cbx.is(':checked')) {
		    this.setTimer(this.lastTime);
		}
	    });
	    this.jq(ID_COUNTDOWN).addClass("ramadda-clickable").css("cursor","pointer").attr("title","Reload").click(()=>{
		this.checkReload(-1);
	    });
	    this.setTimer(this.getPropertyInterval());
	},
	setDisplayMessage:function(msg) {
	    //noop
	},
	okToRun: function() {
	    let cbx = this.jq(ID_CHECKBOX);
	    if(cbx.length==0) return true;
	    return cbx.is(':checked');
	},
	getCountdownLabel: function(time) {
	    let pad = "";
	    if(time<10) pad = "&nbsp;";
	    if(time>60) {
		let minutes = Math.round((time-time%60)/60);
		let seconds = time%60;
		if(minutes<10) minutes  = "0" + String(minutes);
		if(seconds<10) seconds = "0"+String(seconds);
		return "Reload in " + minutes +":" + seconds+pad;
	    }
	    return "Reload in " + time +" seconds"+pad;
	},
	updateCountdown(time) {
	    if(this.getPropertyShowCountdown()) {
		this.jq(ID_COUNTDOWN).html(this.getCountdownLabel(time));
	    } else {
		this.jq(ID_COUNTDOWN).html("Reload");
	    }
	},
	setTimer(time) {
	    if(!this.okToRun()) return;
	    this.lastTime = time;
	    this.updateCountdown(time);
	    if(this.lastTimeout) clearTimeout(this.lastTimeout);
	    this.lastTimeout = setTimeout(()=>{
		this.checkReload(time);
	    },1000);
	},
	checkReload: function(time) {
	    time--;
	    if(time<=0) {
		this.jq(ID_COUNTDOWN).html("Reloading..." +HU.span([STYLE,"color:transparent;"],""));
		this.reloadData();
		time = this.getPropertyInterval();
		//Start up again in a bit so the reloading... label is shown
		if(this.lastTimeout) clearTimeout(this.lastTimeout);
		this.lastTimeout = setTimeout(()=>{
		    this.setTimer(time);
		},1000);
	    } else {
		this.setTimer(time);
	    }
	}
    });
}

function RamaddaTicksDisplay(displayManager, id, properties) {
    if(!properties.animationHeight) properties.animationHeight = "30px";
    properties.doAnimation =  false;
    properties.animationShowButtons = false;
    properties.animationMakeSlider = false;
    const ID_ANIMATION = "animation";
    const SUPER =  new RamaddaFieldsDisplay(displayManager, id, DISPLAY_TICKS, properties);
    let myProps = [
	{label:'Time Ticks'},
	{p:'animationHeight',ex:'30px'},
	{p:'showYears',ex:'true'},
    ];
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {    
        needsData: function() {
            return true;
        },
	dataFilterChanged: function() {
	    SUPER.dataFilterChanged.call(this);
	},
	updateUI: function() {
	    let records = this.filterData();
	    if(!records) return;
	    let dateInfo = this.getDateInfo(records);
	    let years = {
	    }
	    if(!this.getPropertyShowYears(false)) {
		years["all"] = {
		    records:records,
		    yearCnt:"all"
		}
	    } else {
		let yearCnt=0;
		records.forEach(record=>{
		    if(!record.getDate()) return;
		    let year = record.getDate().getUTCFullYear();
		    if(years[year] == null) {
			years[year] = {
			    records:[],
			}
		    }
		    years[year].records.push(record);
		});
	    }
	    let html = "";
	    Object.keys(years).sort().forEach(year=>{
		html+=HU.div([CLASS,'display-ticks-ticks', ID,this.getDomId(ID_ANIMATION+year)]);
	    })
	    this.setContents(html);
	    Object.keys(years).sort().forEach((year,idx)=>{		 
		let info=years[year];
		let dateInfo = this.getDateInfo(info.records);
		let animation = new DisplayAnimation(this,true,{baseDomId:ID_ANIMATION+year,targetDiv:this.jq(ID_ANIMATION+year)});
		animation.makeControls();
		if(year!="all") {
		    dateInfo.dateMin = new Date(Date.UTC(year,0,1));
		    dateInfo.dateMax = new Date(Date.UTC(year,11,31));
		}
		animation.init(dateInfo.dateMin, dateInfo.dateMax,info.records);
	    });
	}
    });
}



function RamaddaMenuDisplay(displayManager, id, properties) {
    const ID_MENU = "menu";
    const ID_PREV = "prev";
    const ID_NEXT = "next";
    const SUPER =  new RamaddaFieldsDisplay(displayManager, id, DISPLAY_MENU, properties);
    let myProps = [
	{label:'Record Menu'},
	{p:'labelTemplate',d:'${name}'},
	{p:'menuLabel',ex:''},
	{p:'showArrows',d:false,ex:true},	
    ];
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {    
        needsData: function() {
            return true;
        },
        handleEventRecordSelection: function(source, args) {
	    SUPER.handleEventRecordSelection.call(this, source, args);
	    if(!this.recordToIdx) return;
	    let idx = this.recordToIdx[args.record.getId()];
	    this.jq(ID_MENU).val(idx);
	},
	updateUI: function() {
	    let records = this.filterData();
	    if(!records) return;
	    let options = [];
	    let labelTemplate = this.getLabelTemplate();
	    this.recordToIdx = {};
	    records.forEach((record,idx)=>{
		let label = this.getRecordHtml(record, null, labelTemplate);
		options.push([idx,label]);
		this.recordToIdx[record.getId()] = idx;
	    });

	    let menu =  HU.select("",[ATTR_ID, this.getDomId(ID_MENU)],options);
	    if(this.getShowArrows(false)) {
		let noun = this.getProperty("noun", "Data");
		let prev = HU.span([CLASS,"display-changeentries-button", TITLE,"Previous " +noun, ID, this.getDomId(ID_PREV), TITLE,"Previous"], HU.getIconImage("fa-chevron-left"));
 		let next = HU.span([CLASS, "display-changeentries-button", TITLE,"Next " + noun, ID, this.getDomId(ID_NEXT), TITLE,"Next"], HU.getIconImage("fa-chevron-right")); 
		menu = prev + " " + menu + " " +next;
	    }
	    let label = this.getMenuLabel();
	    if(label) menu = label+" " + menu;
	    this.setContents(menu);
	    if(this.getShowArrows(false)) {
		this.jq(ID_PREV).click(e=>{
		    let index = +this.jq(ID_MENU).val()-1;
		    if(index<0) {
			index = records.length-1;
		    }
		    this.jq(ID_MENU).val(index).trigger("change");
		});
		this.jq(ID_NEXT).click(e=>{
		    let index = +this.jq(ID_MENU).val()+1;
		    if(index>=records.length) {
			index = 0;
		    }
		    this.jq(ID_MENU).val(index).trigger("change");
		});
	    }
	    this.jq(ID_MENU).change(()=> {
		let record = records[+this.jq(ID_MENU).val()];
		this.propagateEventRecordSelection({record: record});
	    });

	}
    });
}



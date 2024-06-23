/**
   Copyright 2008-2024 Geode Systems LLC
*/



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



function RamaddaAnimationDisplay(displayManager, id, properties) {
    var ID_START = "start";
    var ID_STOP = "stop";
    var ID_TIME = "time";
    var ID_STEP="step";
    let SUPER  = new RamaddaDisplay(displayManager, id, DISPLAY_ANIMATION, properties);
    let myProps =[
	{label:"Animation Control"},
	{p:"sleepTime",ex:'100',tt:'sleep in milliseconds'},
	{p:'startIndex',d:0}
    ];
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
        running: false,
        timestamp: 0,
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
		if(r.getId() == args.record.getId() || r.getTime() == args.record.getTime()) {
		    this.index = idx;
		    this.applyStep(false);
		    return false;
		}
		return true;
	    });

        },
        faster: function() {
            this.sleepTime = this.sleepTime / 2;
            if (this.sleepTime == 0) this.sleepTime = 10;
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
	    

	    let btn = (data,title,icon,id)=>{
		let attrs = ['style','margin-right:6px;','class','ramadda-clickable','title',title,'command',data];
		if(id) attrs.push('id',this.domId(id));
		html +=HU.span(attrs,HU.getIconImage(icon))
	    }

	    btn("start","Go to start",this.iconBegin);
	    btn("-1","Step back; shift-click step back 10",this.iconBack);	    
	    btn("play","Start/Stop",this.iconStart,ID_START);	    
	    btn("1","Step Forward; shift-click step forward 10",this.iconForward);
	    btn("end","Go to end",this.iconEnd);	    	    
	    html+=SPACE2;
	    btn("-","Slower",this.iconSlower);
	    btn("+","Faster",this.iconFaster);	    	    	    
    
            html += HU.div(["style", "display:inline-block; min-height:24px; margin-left:10px;", ATTR_ID, this.getDomId(ID_TIME)], "&nbsp;");
            this.setDisplayTitle("Animation");
            this.setContents(html);

	    let _this = this;
	    this.getContents().find("[command]").click(function(event) {
		let cmd = $(this).attr("command");
		let shift = event.shiftKey;
		switch(cmd) {
		case 'start': _this.setIndex(0);break;
		case 'end': _this.setIndex();break;		    
		case '-1': _this.deltaIndex(shift?-10:-1);break;
		case '1': _this.deltaIndex(shift?10:1);break;		    
		case 'play':_this.toggle();break;
		case '+':_this.faster();break;
		case '-':_this.slower();break;
		}
	

	    });
	    if(!Utils.isDefined(this.index)) {
		this.setIndex(Math.min(records.length-1,this.getStartIndex(0)));
	    }

        },
    });

    this.sleepTime = +this.getSleepTime(this.getProperty('animationSpeed',500));
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
    const ID_POPUP = 'popup';
    const ID_POPUP_BUTTON = 'popupbutton';    
    const SUPER  = new RamaddaDisplay(displayManager, id, DISPLAY_FIELDSLIST, properties);
    let myProps =[
	{label:"Fields List"},
	{p:'displayFields',tt:'Fields to display'},
	{p:'numericOnly',ex:true},
	{p:'reverseFields',ex:true},
	{p:'decorate',ex:true},
	{p:'asList',d:true},
	{p:'sortFields',d:true,ex:true},
	{p:'includeLatLon',d:false,ex:true},				
	{p:'selectable',ex:true},
	{p:'showFieldDetails',ex:true},
	{p:'showPopup',d:false,ex:true,tt:'Popup the selector'},	
	{p:'selectOne',ex:true},
	{p:'some_field.color',ex:'red',tt:'add a color block'},
	{p: 'selectLabel',tt:'Label to use for the button'},
	{p: 'filterSelect',ex:true,tt:'Use this display to select filter fields'},
	{p: 'filterSelectLabel',tt:'Label to use for the button'}	
    ];
    
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
	needsData: function() {
            return true;
        },
	fieldsToMap:{},
	getFieldsKey:function() {
	    let fields = this.getFields();
	    let key ='';
	    fields.forEach(f=>{
		key+='_'+f.getId();
	    });
	    return key;
	},
        setEntry: function(entry) {
	    //When we change the data then cache the existing fieldMap
	    if(this.selectedMap) {
		let key = this.getFieldsKey();
		this.fieldsToMap[key] = this.selectedMap;
	    }
	    this.selectedMap=null;
	    this.fields =null;
	    SUPER.setEntry.call(this, entry);
	},
	updateUI: function() {
	    let records = this.filterData();
	    if(records == null) return;
	    let html = "";
            let selectedFields;
	    if(this.getFilterSelect()) {
		selectedFields = this.getFieldsByIds(null,this.getProperty('filterFields', ''));
	    } else  {
		selectedFields = this.getSelectedFields();
	    }
	    //Check if we've displayed a pointdata with the same set of fields
	    let key = this.getFieldsKey();
	    this.selectedMap = this.selectedMap || this.fieldsToMap[key];
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
            let fields = this.fields;
	    if(!fields) {
		if(this.getDisplayFields())
		    fields =this.getFieldsByIds(null, this.getDisplayFields(), true);
		else
		    fields = this.getData().getRecordFields();
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
		if(!this.getIncludeLatLon()) {
		    fields = fields.filter(f=>{
			return !f.isFieldGeo();
		    });
		}
		if(this.getSortFields(true)) {
		    fields = fields.sort((f1,f2)=>{
			return f1.getDescription().localeCompare(f2.getDescription());
		    });
		}
	    }
	    this.fields	     = fields;
	    this.fieldsMap={};
	    this.fields.forEach(f=>{
		this.fieldsMap[f.getId()] = f;
	    });

//	    html += HU.center('#' + records.length +' records');
	    let fs = [];
	    let clazz = ' ramadda-clickable display-fields-field ';
	    let asList = this.getAsList();
	    if(this.getDecorate(true)) clazz+= ' display-fields-field-decorated ';
	    if(asList)
		clazz+=' display-fields-list-field';
	    let selectable = this.getSelectable(true);
	    let details = this.getShowFieldDetails(false);	    
	    fields.forEach((f,idx)=>{
		
		let block  =f.getLabel();
		let color = this.getProperty(f.getId()+'.color');
		if(color)
		    block+=HU.div([ATTR_CLASS,'display-fields-field-color',ATTR_STYLE,HU.css('background',color)]);
		block = HU.div([ATTR_STYLE,HU.css('position','relative')],block);

		if(details) {
		    block+= '<br>' +
			f.getId() + f.getUnitSuffix()+'<br>' +
			f.getType();
		}
		let c = clazz;
		let selected = this.selectedMap[f.getId()];
		if(selectable) c += ' display-fields-field-selectable ';
		if(selectable && selected) c += ' display-fields-field-selected ';
		let title = '';
		if(selectable)
		    title = 'Click to toggle. Shift-click toggle all';
		block =HU.div([TITLE,title,'field-selected',selected, 'field-id', f.getId(),'class',c], block);
		fs.push(block);
	    });
	    let fhtml = Utils.wrap(fs,'','');
	    html += fhtml;

	    let label = this.getSelectLabel(this.getFilterSelect()?this.getFilterSelectLabel('Select Filter Fields'):'Select fields');
	    if(this.getShowPopup()) {
		html = HU.div([ID,this.domId(ID_POPUP_BUTTON)], label) +
		    HU.div([ID,this.domId(ID_POPUP),STYLE,'display:none;max-height:300px;overflow-y:auto;width:600px;'], html);
	    }
	    this.setContents(html);
	    if(this.getShowPopup()) {
		this.jq(ID_POPUP_BUTTON).button().click(()=>{
		    this.fieldsDialog = HU.makeDialog({contentId:this.domId(ID_POPUP),title:label,inPlace:true,anchor:this.domId(ID_POPUP_BUTTON),draggable:true,header:true,sticky:true});
		});
	    }
	    if(selectable) {
		let _this = this;
		let fieldBoxes = this.find('.display-fields-field');
		fieldBoxes.draggable({
		    stop: function() {
		    },
		    containment:this.domId(ID_DISPLAY_CONTENTS),
		    revert: true
		});
		fieldBoxes.droppable( {
		    hoverClass: 'display-fields-droppable',
		    accept:'.display-fields-field',
		    drop: function(event,ui) {
			let draggedId = ui.draggable.attr('field-id');
			let targetId = $(this).attr('field-id');			
			let draggedField = _this.fields.find(f=>{
			    if(f.getId()==draggedId) return f;
			    return null;
			});
			let targetField = _this.fields.find(f=>{
			    if(f.getId()==targetId) return f;
			    return null;
			});			
			if(!draggedField) {
			    console.log('could not find dragged field:' + draggedId);
			    return
			}
			if(!targetField) {
			    console.log('could not find target field:' + targetId);
			    return
			}
			
			Utils.moveBefore(_this.fields,targetField,draggedField);
			_this.printFields();
			_this.updateUI();
			_this.handleFieldSelect();
		    }
		});


		fieldBoxes.click(function(event) {
		    if(event.metaKey) {
			_this.printFields();
			return
		    }
		    let shift = event.shiftKey ;
		    let doAll = shift;
		    let allSelected;
		    let selected  = $(this).attr('field-selected')=='true';
		    if(_this.getSelectOne()) {
			if(selected) return;
			selected=true;
			allSelected=false;
			doAll = true;
		    } else {
			selected = !selected;
			allSelected=selected;
		    }
		    if(doAll) {
			fieldBoxes.attr('field-selected',allSelected);
			if(allSelected) {
			    fieldBoxes.addClass('display-fields-field-selected');
			} else {
			    fieldBoxes.removeClass('display-fields-field-selected');
			}

		    }

		    $(this).attr('field-selected',selected);
		    if(selected) {
			$(this).addClass('display-fields-field-selected');
		    } else {
			$(this).removeClass('display-fields-field-selected');
		    }

		    _this.handleFieldSelect();
		});
	    }
	},
	printFields:function() {
	    if(!this.canEdit()) return;
	    let out=this.fields.reduce((v,f,idx)=>{return v+(idx==0?'':',')+f.getId()},'fields=\"');
	    out+='\"\n\n';
	    out+=this.getActiveFields().reduce((v,f,idx)=>{return v+(idx==0?'':',')+f.getId()},'displayFields=\"');
	    out+='\"\n';
	    Utils.copyToClipboard(out);
	    console.log(out);

	},
	getActiveFields:function() {
	    let _this=this;
	    let fieldBoxes = (this.fieldsDialog&&this.fieldsDialog.length)?
		this.fieldsDialog.find(".display-fields-field"):
		this.find(".display-fields-field");
	    let isSelected = {};
	    let selectedFields = [];
	    fieldBoxes.each(function(){
		let selected  = $(this).attr("field-selected")=="true";
		if(selected) {
		    let id = $(this).attr("field-id");
		    let field = _this.fieldsMap[id];
		    if(field) isSelected[field.getId()]=true;
		}
	    });
	    this.fields.forEach(f=>{
		if(isSelected[f.getId()]) selectedFields.push(f);
	    });

	    _this.selectedMap = {};
	    selectedFields.forEach(f=>{
		_this.selectedMap[f.getId()] = true;
	    });
	    return selectedFields;
	},
	handleFieldSelect:function() {
	    let _this = this;
	    let selectedFields  = this.getActiveFields();
	    setTimeout(()=>{
		if(_this.getFilterSelect()) {
		    _this.propagateEvent(DisplayEvent.filterFieldsSelected, selectedFields);
		} else {
		    _this.propagateEvent(DisplayEvent.fieldsSelected, selectedFields);
		}
	    },20);
	}	
});
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
	{p:'circles',ex:'true'},	
	{p:'inBox',ex:'true'},
	{p:'labelColor',ex:'#fff'},
	{p:'labelColors',ex:'color1,color2,...'},
	{p:'labelField',ex:''},	
	{p:'orientation',ex:'vertical'}
    ];

    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
	getColorList:function() {
	    if (this.getProperty("colorTable")) {
		let ct =this.getColorTable();
		return ct.colors;
	    }	    
	    return SUPER.getColorList.call(this);
	},

	needsData: function() {
            return this.getProperty("labelField")!=null;
	},
	updateUI: function() {
	    let labels = this.getProperty("labels","").split(",");
            let labelField = this.getFieldById(null, this.getProperty("labelField"));
	    if(labelField) {
		let records = this.filterData();
		if (!records) {
                    this.setDisplayMessage(this.getLoadingMessage());
                    return;
		}
		labels = [];
		records.forEach(record=>{
		    labels.push(labelField.getValue(record));
		});
	    }

            let colorBy;
	    if(this.getProperty("colorTable"))  {
		colorBy = new ColorByInfo(this, [], []);
		labels=[];
	    }

	    let colors = this.getColorList();
	    let html = "";
	    let colorWidth = this.getProperty("colorWidth","20px");
	    let labelColor = this.getProperty("labelColor","#000");
	    let labelColors = this.getProperty("labelColors")?this.getProperty("labelColors").split(","):null;
	    let inBox = this.getProperty("inBox",false);
	    let orientation = this.getProperty("orientation","horizontal");
	    let delim = orientation=="horizontal"?" ":"<br>";
	    let circles = this.getCircles();
	    for(let i=0;i<labels.length;i++) {
		let label = labels[i];
		let color = colors[i]||"#fff";
		if(i>0) html+=delim;
		if(!inBox) {
		    html+=HU.div(["class","display-legend-item"], HU.div(["class","display-legend-color " + (circles?"display-colortable-dot":""),"style","background:" + color+";width:" + colorWidth+";"+(circles?"height:" + colorWidth+";":	  "height:15px;")]) +
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
	    if(colorBy) {
		this.displayColorTable(colorBy,ID_COLORTABLE,this.getProperty("colorByMin",10),
				       this.getProperty("colorByMax",100));
	    }
	},
    });
}



function RamaddaDownloadDisplay(displayManager, id, properties) {
    const SUPER =  new RamaddaFieldsDisplay(displayManager, id, DISPLAY_DOWNLOAD, properties);
    const ID_DOWNLOAD_CSV = "downloadcsv";
    const ID_DOWNLOAD_JSON = "downloadjson";
    const ID_DOWNLOAD_COPY = "downloadcopy";
    const ID_DOWNLOAD_FROMSERVER = "fromserver";    
    const ID_FROMDATE = "fromdate";
    const ID_TODATE = "todate";                
    const ID_CANCEL = "cancel";
    const ID_COUNT = "count";        
    let myProps =[
	{label:'Download'},
	{p:'downloadLabel',ex:'Download'},
	{p:'useIcon',d:'false',ex:'false'},
	{p:'iconSize',ex:'',d:'16pt'},	
	{p:'fileName',d:'download',ex:'download'},
	{p:'askFields',d:'false',ex:'true'},
	{p:'showRecordCount',ex:true,tt:'Show # records'},
	{p:'showCsvButton',ex:false,tt:'Show/hide the CSV button'},
	{p:'showJsonButton',ex:false,tt:'Show/hide the JSON button'},
	{p:'showCopyButton',ex:false,tt:'Show/hide the Copy button'},
	{p:'showDateSelect',d:false,ex:true,tt:'Show date select'},			
//	{p:'doSave',d:false,tt:'Show the save file button'}
    ];
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
	fieldOn:{},
	needsData: function() {
            return true;
	},
	updateUI: function() {
	    let records = this.filterData();
	    let label = this.getDownloadLabel(this.getProperty("csvLabel","Download Data"));
	    label = label.replace("${title}",this.getProperty("title",""));
	    let useIcon = this.getUseIcon(true);
	    let iconSize = this.getIconSize();
	    label = HU.div(['style','display:inline-block;',ID,this.getDomId("csv")], useIcon?HU.getIconImage("fa-download",['style','line-height:0px;display:block;'],[STYLE,"cursor:pointer;font-size:" + iconSize+";",TITLE,label]):label);
	    if(this.getShowRecordCount()) {
		label=label+HU.space(2)+HU.span([ATTR_ID,this.domId(ID_COUNT)],records?('# '+records.length+' records'):'');
	    }
	    this.setContents(HU.div([],label));
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

	getSubsetFunction: function() {
	    let fromDate = Utils.stringDefined(this.selectFromDate)?new Date(this.selectFromDate):null;
	    let toDate = Utils.stringDefined(this.selectToDate)?new Date(this.selectToDate):null;	    
	    //Offset the to date by 24 hours to get the end of the day
	    if(toDate)
		toDate = new Date(toDate.getTime()+1000*60*60*24);
	    if(fromDate|| toDate) {
		return record=>{
		    if(!record.hasDate()) return false;
		    let date = record.getDate();
		    if(fromDate && date.getTime()<fromDate.getTime()) {
			return false;
		    }
		    if(toDate && date.getTime()>toDate.getTime()) return false;		    
		    return true;
		};
	    }
	    return null;
	},

	getCsv: function(fields, records,copy) {
            fields = fields || this.getData().getRecordFields();
	    
	    let cnt = parseInt(this.jq('number_records').val().trim());
	    let csv = DataUtils.getCsv(fields, records,this.getSubsetFunction(),cnt);
	    if(copy) {
		Utils.copyToClipboard(csv);
		alert("Copied to clipboard");
	    } else {
		Utils.makeDownloadFile(this.getPropertyFileName()+".csv", csv);
	    }
	},
	getJson: function(fields, records) {
            fields = fields || this.getData().getRecordFields();
	    let cnt = parseInt(this.jq('number_records').val().trim());
	    DataUtils.getJson(fields, records,this.getPropertyFileName()+".json",this.getSubsetFunction(),cnt);
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
	    
	    let space = SPACE;
	    let buttons = "";
	    if(this.getShowCsvButton(true)) {
		buttons+=HU.div([ID,this.getDomId(ID_DOWNLOAD_CSV)],"CSV") +space;
	    }
	    if(this.getShowJsonButton(true))
		buttons+=HU.div([ID,this.getDomId(ID_DOWNLOAD_JSON)],"JSON") +space;
	    if(this.getShowCopyButton(true))
		buttons+=  HU.div([ID,this.getDomId(ID_DOWNLOAD_COPY)],"Copy") +space;
	    buttons+=  HU.div([ID,this.getDomId(ID_CANCEL)],"Cancel");
	    let html = HU.center("#" +HU.input('',records.length,[ATTR_ID,this.getDomId('number_records'),'title','Select # records to download','size','4']) +" records");
	    html+=HU.center(HU.span([ATTR_STYLE,'font-size:80%;'], 'Note: this downloads the data currently<br>being shown in the browser'));
	    html+=HU.center(buttons);
	    if(this.getShowDateSelect()) {
		html+=HU.formTable();
		html+=HU.formEntry('From date:',
				   HU.tag("input",[ATTR_ID,this.domId(ID_FROMDATE),'placeholder','yyyy-MM-dd','size','10','value',this.selectFromDate??'']));
		html+=HU.formEntry('To date:',
				   HU.tag("input",['id',this.domId(ID_TODATE),'placeholder','yyyy-MM-dd','size','10','value',this.selectToDate??'']));
		html+=HU.formTableClose();
	    }
    
	    html += "<b>Include:</b>";
	    let cbx = "";
	    cbx += HU.checkbox(this.getDomId("cbx_toggle_all"),[],true,"Toggle all") +"<br>";
	    this.getData().getRecordFields().forEach((f,idx)=>{
		let on = this.fieldOn[f.getId()];
		if(!Utils.isDefined(on)) {
		    on = true;
		}
		cbx += HU.checkbox(this.getDomId("cbx_" + f.getId()),[CLASS,"display-downloader-field-cbx"],on,f.getLabel()) +"<br>";
	    });
	    html += HU.div([STYLE,HU.css("max-height","200px","overflow-y","auto","margin-left","10px")], cbx);
	    html = HU.div([STYLE,HU.css("margin","5px")],html);
	    return html;
	},
	doDownload: function() {
	    let records = this.filterData();
	    let func = (json,copy)=>{
		if(this.getShowDateSelect()) {
		    this.selectFromDate=this.jq(ID_FROMDATE).val();
		    this.selectToDate=this.jq(ID_TODATE).val();		    
		}

		this.jq(ID_DIALOG).hide();
		let allFields = this.getData().getRecordFields();
		let fields = [];
		this.applyFieldSelection();
		allFields.forEach(f=>{
		    if(this.fieldOn[f.getId()]) {
			fields.push(f);
		    }
		});

		/*
		let pointData = this.dataCollection.getList()[0];
		let url = new URL('https://localhost/' + pointData.getUrl());
		if(allFields.length!=fields.length) {
		    fields.forEach(f=>{
			url = HU.url(url.toString(),['field_use',f.getId()]);
		    });
		}
		if(!json)
		    url = HU.url(url.toString(),['product','points.csv']);
		console.log(url);
		window.open(url,'_download');
		return
		*/

		if(json) 
		    this.getJson(fields, records);
		else	
		    this.getCsv(fields, records,copy);
		if(this.dialog) this.dialog.remove();
		this.dialog =null;	    };
	    if(this.getPropertyAskFields(true)) {
		let html = this.getDownloadDialog(records);
		let dialog;
		let init = ()=>{
		    let _this = this;
		    this.jq("cbx_toggle_all").click(function() {
			let on = $(this).is(':checked');
			dialog.find(".display-downloader-field-cbx").each(function() {
			    $(this).prop("checked",on);
			});
		    });
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
		    this.jq(ID_DOWNLOAD_COPY).button().click(() =>{
			func(false,true);
		    });				    
		};
		dialog = this.showDialog(html,this.getDomId(ID_DISPLAY_CONTENTS),init,this.getTitle());


		if(this.getShowDateSelect()) {
		    this.jq(ID_FROMDATE).datepicker({ dateFormat: 'yy-mm-dd',changeMonth: true, changeYear: true,constrainInput:false, yearRange: '1900:2100'  });
		    this.jq(ID_TODATE).datepicker({ dateFormat: 'yy-mm-dd',changeMonth: true, changeYear: true,constrainInput:false, yearRange: '1900:2100'  });		    
		}		


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
	{p:'doPage',ex:'true',tt:'Reload the entire page'},
    ];
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
        needsData: function() {
            return true;
        },
	reloadData: function() {
	    let pointData = this.dataCollection.getList()[0];
	    if(pointData)
		pointData.loadData(this,true);
	    else
		console.log('No data to reload');
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
	doReload: function(time) {
	    if(this.getDoPage()) {
		location.reload(true);
	    } else {
		this.reloadData();
	    }
	},
	checkReload: function(time) {
	    time--;
	    if(time<=0) {
		this.jq(ID_COUNTDOWN).html("Reloading..." +HU.span([STYLE,"color:transparent;"],""));
		this.doReload();
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
    const SUPER =  new RamaddaFieldsDisplay(displayManager, id, DISPLAY_MENU, properties);
    let myProps = [
	{label:'Record Menu'},
	{p:'labelTemplate',d:'${name}'},
	{p:'menuLabel',ex:''},
	{p:'showArrows',d:false,ex:true},
	{p:'showButtons',d:false,ex:true},
	{p:'maxPerRow',tt:'When showing buttons how many buttons per row',ex:6},	
	{p:'buttonStyle',d:''},
	{p:'buttonStyleOn',d:''},	
    ];
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {    
        needsData: function() {
            return true;
        },
        handleEventRecordSelection: function(source, args) {
	    SUPER.handleEventRecordSelection.call(this, source, args);
	    if(!this.recordToIdx) return;
	    let idx = this.recordToIdx[args.record.getId()];
	    if(Utils.isDefined(idx)) {
		this.jq(ID_MENU).val(idx);
	    }
	},
        pointDataLoaded: function(pointData, url, reload) {
//	    this.logMsg("pointDataLoaded");
	    SUPER.pointDataLoaded.call(this, pointData,url,reload);
	    if(this.haveLoadedData && this.records) {
		setTimeout(()=>{
		    let record = this.records[+this.jq(ID_MENU).val()];
		    if(record) {
			this.propagateEventRecordSelection({record: record});
		    }},100);
	    }
	    this.haveLoadedData= true;
	},

	updateUI: function() {
	    let _this = this;
//	    this.logMsg("updateUI");
	    this.records = this.filterData();
	    if(!this.records) return;
	    let options = [];
	    let labelTemplate = this.getLabelTemplate();
	    this.recordToIdx = {};
	    let showButtons = this.getShowButtons();
	    if(showButtons) {
		let buttonStyle = this.getButtonStyle();
		let buttonStyleOn = this.getButtonStyleOn();		
		let tabs = [];
		this.idToRecord = {};
		let count = 0;
		let maxPerRow  = this.getProperty('maxPerRow',-1);
		let html = '';
		if(maxPerRow>=0) {
		    html=HU.open('div',['style','text-align:center;']);
		}
		this.records.forEach((record,idx)=>{
		    if(maxPerRow>=0) {
			count++;
			if(count>maxPerRow) {
			    count=1;
			    //Add a spacer
			    tabs.push('<div style=\'margin-top:4px;\'></div>');
			}
		    }
		    let label = this.getRecordHtml(record, null, labelTemplate);
		    let style = buttonStyle;
		    if(idx==0) style+=buttonStyleOn;
		    tabs.push(HU.span(['class','display-menu-button-item ramadda-hoverable ramadda-clickable ' + (idx==0?'display-menu-button-item-on':''),'style',style,RECORD_ID,record.getId()], label));
		    this.idToRecord[record.getId()] = record;
		});
		html+=Utils.join(tabs,"");
		if(maxPerRow>=0) {
		    html+=HU.close('div');
		}
		this.setContents(html);
		let items = this.getContents().find('.display-menu-button-item');
		items.click(function() {
		    if($(this).hasClass('display-menu-button-item-on')) return;
		    let record = _this.idToRecord[$(this).attr(RECORD_ID)];
		    items.removeClass('display-menu-button-item-on');
		    items.attr('style',buttonStyle);		    
		    items.removeClass('display-menu-button-item-on');
		    $(this).addClass('display-menu-button-button-on');
		    $(this).attr('style',buttonStyle+buttonStyleOn);
		    _this.propagateEventRecordSelection({record: record});
		});
		return
	    }

	    this.records.forEach((record,idx)=>{
		let label = this.getRecordHtml(record, null, labelTemplate);
		options.push([idx,label]);
		this.recordToIdx[record.getId()] = idx;
	    });

	    let menu =  HU.select("",[ATTR_ID, this.getDomId(ID_MENU)],options);
	    if(this.getShowArrows(false)) {
		let noun = this.getProperty("noun", "Data");
		let prev = HU.span([CLASS,"display-changeentries-button ramadda-clickable", TITLE,"Previous " +noun, ID, this.getDomId(ID_PREV), TITLE,"Previous"], HU.getIconImage("fa-chevron-left"));
 		let next = HU.span([CLASS, "display-changeentries-button ramadda-clickable", TITLE,"Next " + noun, ID, this.getDomId(ID_NEXT), TITLE,"Next"], HU.getIconImage("fa-chevron-right")); 
		menu = menu.replace(/\n/g,"");
		menu = prev + "&nbsp;" + menu +  "&nbsp;" +next;
	    }
	    let label = this.getMenuLabel();
	    if(label) menu = label+"&nbsp;" + menu;
	    this.setContents(menu);
	    if(this.getShowArrows(false)) {
		this.jq(ID_PREV).click(e=>{
		    let index = +this.jq(ID_MENU).val()-1;
		    if(index<0) {
			index = this.records.length-1;
		    }
		    this.jq(ID_MENU).val(index).trigger("change");
		});
		this.jq(ID_NEXT).click(e=>{
		    let index = +this.jq(ID_MENU).val()+1;
		    if(index>=this.records.length) {
			index = 0;
		    }
		    this.jq(ID_MENU).val(index).trigger("change");
		});
	    }
	    this.jq(ID_MENU).change(()=> {
		let record = this.records[+this.jq(ID_MENU).val()];
		this.propagateEventRecordSelection({record: record});
	    });

	}
    });
}



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
const DISPLAY_TICKS = "ticks";

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
    category: CATEGORY_CONTROLS
});
addGlobalDisplayType({
    type: DISPLAY_ANIMATION,
    label: "Animation",
    requiresData: true,
    category: CATEGORY_CONTROLS
});
addGlobalDisplayType({
    type: DISPLAY_MESSAGE,
    label: "Message",
    requiresData: true,
    forUser: true,
    category: CATEGORY_CONTROLS
});

addGlobalDisplayType({
    type: DISPLAY_LABEL,
    label: "Text",
    requiresData: false,
    category: CATEGORY_CONTROLS
});
addGlobalDisplayType({
    type: DISPLAY_LEGEND,
    label: "Legend",
    requiresData: true,
    forUser: true,
    category: CATEGORY_CONTROLS
});
addGlobalDisplayType({
    type: DISPLAY_TICKS,
    label: "Ticks",
    requiresData: true,
    forUser: true,
    category: CATEGORY_CONTROLS
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
                label += HtmlUtils.b("Date:") + " " + dttm;
            } else {
                label += HtmlUtils.b("Index:") + " " + this.index;
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
            $("#" + this.getDomId(ID_START)).html(HtmlUtils.getIconImage(this.iconStop));
            this.tick();
        },
        stop: function() {
            if (!this.running) return;
            this.running = false;
            this.timestamp++;
            $("#" + this.getDomId(ID_START)).html(HtmlUtils.getIconImage(this.iconStart));
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
            html += HtmlUtils.onClick(get + ".setIndex(0);", HtmlUtils.div([ATTR_TITLE, "beginning", ATTR_CLASS, c],HtmlUtils.getIconImage(this.iconBegin)));
            html += HtmlUtils.onClick(get + ".deltaIndex(-1);", HtmlUtils.div([ATTR_TITLE, "step back", ATTR_CLASS, c],HtmlUtils.getIconImage(this.iconBack)));
            html += HtmlUtils.onClick(get + ".toggle();", HtmlUtils.div([ATTR_ID, this.getDomId(ID_START),ATTR_TITLE, "play/stop",ATTR_CLASS, c], HtmlUtils.getIconImage(this.iconStart)));
            html += HtmlUtils.onClick(get + ".deltaIndex(1);", HtmlUtils.div([ATTR_TITLE, "step forward", ATTR_CLASS, c],HtmlUtils.getIconImage(this.iconForward)));
            html += HtmlUtils.onClick(get + ".setIndex();", HtmlUtils.div([ATTR_TITLE, "end", ATTR_CLASS, c],HtmlUtils.getIconImage(this.iconEnd)));
            html += HtmlUtils.onClick(get + ".faster();", HtmlUtils.div([ATTR_TITLE, "faster", ATTR_CLASS, c],HtmlUtils.getIconImage(this.iconFaster)));
            html += HtmlUtils.onClick(get + ".slower();", HtmlUtils.div([ATTR_TITLE, "slower", ATTR_CLASS, c],HtmlUtils.getIconImage(this.iconSlower)));
            html += HtmlUtils.div(["style", "display:inline-block; min-height:24px; margin-left:10px;", ATTR_ID, this.getDomId(ID_TIME)], "&nbsp;");
            this.setDisplayTitle("Animation");
            this.setContents(html);
        },
    });
}



function RamaddaMessageDisplay(displayManager, id, properties) {
    const SUPER =  new RamaddaFieldsDisplay(displayManager, id, DISPLAY_MESSAGE, properties);
    RamaddaUtil.inherit(this,SUPER);
    addRamaddaDisplay(this);
    $.extend(this, {
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


    addRamaddaDisplay(this,SUPER, myProps, {
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
		    html+=HtmlUtils.div(["class","display-legend-item"], HtmlUtils.div(["class","display-legend-color","style","background:" + color+";width:" + colorWidth+";"+
					 "height:15px;"]) +
					HtmlUtils.div(["class","display-legend-label"],label));
		} else {
		    let lc = labelColors?labelColors[i]:labelColor || labelColor;
		    html+=HtmlUtils.div(["class","display-legend-color","style","margin-left:8px;background:" + color+";"],
					HtmlUtils.div(["class","display-legend-label","style","margin-left:8px;margin-right:8x;color:" + lc+";"],label));
		}
	    }
	    if(orientation!="vertical") {
		html = HtmlUtils.center(html); 
	    }
	    this.writeHtml(ID_DISPLAY_CONTENTS, html);
	},
    });
}



function RamaddaDownloadDisplay(displayManager, id, properties) {
    const SUPER =  new RamaddaFieldsDisplay(displayManager, id, DISPLAY_DOWNLOAD, properties);
    const ID_DOWNLOAD_CSV = "downloadcsv";
    const ID_DOWNLOAD_JSON = "downloadjson";    
    const ID_CANCEL = "cancel";    
    RamaddaUtil.inherit(this,SUPER);
    addRamaddaDisplay(this);
    this.defineProperties([
	{label:'Download'},
	{p:'csvLabel',ex:'Download'},
	{p:'useIcon',d:'false',ex:'false'},
	{p:'fileName',d:'download',ex:'download'},
	{p:'askFields',d:'false',ex:'true'},		
    ]);
    $.extend(this, {
	fieldOn:{},
	needsData: function() {
        return true;
    },
    updateUI: function() {
	let label = this.getPropertyCsvLabel("Download Data");
	label = label.replace("${title}",this.getProperty("title",""));
	let useIcon = this.getPropertyUseIcon(true);
	label = useIcon?HU.getIconImage("fa-download",[STYLE,"cursor:pointer;",TITLE,label]):label;
	this.setContents(HU.div([],HU.span([ID,this.getDomId("csv")],label)));
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
    getCsv: function(fields) {
        fields = fields || this.getData().getRecordFields();
	let records = this.filterData();
	DataUtils.getCsv(fields, records,this.getPropertyFileName()+".csv");
    },
    getJson: function(fields) {
        fields = fields || this.getData().getRecordFields();
	let records = this.filterData();
	DataUtils.getJson(fields, records,this.getPropertyFileName()+".json");
    },

    applyFieldSelection: function() {
	this.getData().getRecordFields().forEach(f=>{
	    let cbx = this.jq("cbx_" + f.getId());
	    let on = cbx.is(':checked');
	    this.fieldOn[f.getId()] = on;
	});
    },
    getDownloadDialog: function() {
	let html = HU.center(HU.div([ID,this.getDomId(ID_DOWNLOAD_CSV)],"Download CSV") +"&nbsp;&nbsp;" +
			     HU.div([ID,this.getDomId(ID_DOWNLOAD_JSON)],"Download JSON") +"&nbsp;&nbsp;" +			     			     HU.div([ID,this.getDomId(ID_CANCEL)],"Cancel"));
	html += "<b>Include:<br></b>";
	let cbx = "";
	this.getData().getRecordFields().forEach((f,idx)=>{
	    let on = this.fieldOn[f.getId()];
	    if(!Utils.isDefined(on)) on = true;
	    cbx += HU.checkbox(this.getDomId("cbx_" + f.getId()),[],on) +" " + f.getLabel() +"<br>";
	});
	
	html = HU.div([STYLE,HU.css("margin","5px")],html);
	html += HU.div([STYLE,HU.css("max-height","200px","overflow-y","auto","margin","5px")], cbx);
	return html;
    },
    doDownload: function() {
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
		this.getJson(fields);
	    else
		this.getCsv(fields);
	};
	if(this.getPropertyAskFields(true)) {
	    let html = this.getDownloadDialog();
	    let init = ()=>{
		this.jq(ID_CANCEL).button().click(() =>{
		    this.applyFieldSelection();
		    this.jq(ID_DIALOG).hide();
		});
		this.jq(ID_DOWNLOAD_CSV).button().click(() =>{
		    func(false);
		});
		this.jq(ID_DOWNLOAD_JSON).button().click(() =>{
		    func(true);
		});		
	    };
	    this.showDialog(html,this.getDomId(ID_DISPLAY_CONTENTS),init);
	} else  {
	    this.getCsv();
	}
    },
    });
}

function RamaddaReloaderDisplay(displayManager, id, properties) {
    const ID_CHECKBOX= "cbx";
    const ID_COUNTDOWN= "countdown";
    const SUPER =  new RamaddaFieldsDisplay(displayManager, id, DISPLAY_RELOADER, properties);
    RamaddaUtil.inherit(this,SUPER);
    addRamaddaDisplay(this);
    this.defineProperties([
	{label:'Reloader'},
	{p:'interval',ex:'30',d:30,label:"Interval"},
	{p:'showCheckbox',ex:'false',d:true,label:"Show Checkbox"},
	{p:'showCountdown',ex:'false',d:true,label:"Show Countdown"},
    ]);

    $.extend(this, {
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
	    this.jq(ID_CHECKBOX).change(()=>{
		let cbx = this.jq(ID_CHECKBOX);
		if(cbx.is(':checked')) {
		    this.setTimer(this.lastTime);
		}
	    });
	    this.setTimer(this.getPropertyInterval());
	},
	okToRun: function() {
	    let cbx = this.jq(ID_CHECKBOX);
	    if(cbx.length==0) return true;
	    return cbx.is(':checked');
	},
	getCountdownLabel: function(time) {
	    let pad = "";
	    if(time<10) pad = "&nbsp;";
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
	    if(this.timerPending) return;
	    this.lastTime = time;
	    this.updateCountdown(time);
	    this.timerPending = true;
	    setTimeout(()=>{
		this.timerPending = false;
		this.checkReload(time);
	    },1000);
	},
	checkReload: function(time) {
	    time--;
	    if(time<=0) {
		this.jq(ID_COUNTDOWN).html("Reloading..." +HU.span([STYLE,"color:transparent;"],"xseconds"));
		this.reloadData();
		time = this.getPropertyInterval();
		//Start up again in a bit so the reloading... label is shown
		setTimeout(()=>{
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
    RamaddaUtil.inherit(this,SUPER);
    addRamaddaDisplay(this);

    this.defineProperties([
	{label:'Time Ticks'},
	{p:'animationHeight',ex:'30px'},
	{p:'showYears',ex:'true'},
    ]);

    $.extend(this, {
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
	    this.writeHtml(ID_DISPLAY_CONTENTS, html);
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



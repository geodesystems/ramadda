/**
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


var DISPLAY_LEGEND = "legend";
addGlobalDisplayType({
    type: DISPLAY_LEGEND,
    label: "Legend",
    requiresData: true,
    forUser: true,
    category: CATEGORY_MISC
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
	    if(!args.record) return;
            var data = this.displayManager.getDefaultData();
            if (data == null) return;
	    let records  = data.getRecords();
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
        initDisplay: function() {
            this.createUI();
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
    let SUPER =  new RamaddaDisplay(displayManager, id, DISPLAY_LEGEND, properties);
    RamaddaUtil.inherit(this,SUPER);
    addRamaddaDisplay(this);
    $.extend(this, {
	getWikiEditorTags: function() {
	    return Utils.mergeLists(SUPER.getWikiEditorTags(),
				    [
					"label:Legend Display",
					'labels=""',
					'colors=""',
				    ]);
	},
	updateUI: function() {
	    let labels = this.getProperty("labels","").split(",");
	    let colors = this.getColorList();
	    let html = "";
	    let colorWidth = this.getProperty("colorWidth","20px");
	    for(let i=0;i<labels.length;i++) {
		let label = labels[i];
		let color = colors[i]||"#fff";
		html+=HtmlUtils.div(["class","display-legend-color","style","background:" + color+";width:" + colorWidth+";"]) +
		    HtmlUtils.div(["class","display-legend-label"],label);
	    }
	    this.writeHtml(ID_DISPLAY_CONTENTS, HtmlUtils.center(html)); 
	},
    })
}

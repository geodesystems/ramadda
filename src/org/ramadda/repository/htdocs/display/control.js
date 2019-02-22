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
    else if (properties.title) this.text = properties.title;
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
            var html = HtmlUtils.div([ATTR_CLASS, textClass, ATTR_ID, this.getDomId(ID_TEXT)], this.text);
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




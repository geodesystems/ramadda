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

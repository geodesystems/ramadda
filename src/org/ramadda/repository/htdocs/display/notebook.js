/**
Copyright 2008-2019 Geode Systems LLC
*/



var DISPLAY_NOTEBOOK = "notebook";


addGlobalDisplayType({type:DISPLAY_NOTEBOOK , label: "Notebook",requiresData:false,category:"Misc"});



function RamaddaNotebookDisplay(displayManager, id, properties) {
    var SUPER;
    var ID_CELLS = "cells";
    var ID_CELL = "cell";
    RamaddaUtil.inherit(this, SUPER = new RamaddaDisplay(displayManager, id, DISPLAY_NOTEBOOK, properties));
    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
        cells:[],
        cellId:0,
        initDisplay: function() {
            var _this = this;
            this.createUI();
            var cells = HtmlUtils.div([ATTR_CLASS, "display-notebook-cells", ATTR_ID, this.getDomId(ID_CELLS)], "");
            var html = cells;
            this.setContents(html);
            if(this.cells.length>0) {
                this.layoutCells();
            } else {
                this.cells.push(this.addCell("html:hello world"));
                this.cells.push(this.addCell("js:var x = 'hello world';\nreturn x"));
                this.cells.push(this.addCell("sh:ls"));
            }
            this.cells[0].focus();
        },
        layoutCells: function() {
             this.jq(ID_CELLS).html("");
             for(var i=0;i<this.cells.length;i++) {
                  var cell = this.cells[i];
                  this.jq(ID_CELLS).append(HtmlUtils.div([ATTR_CLASS, "display-notebook-cell", ATTR_ID, cell.id], ""));
                  cell.createCell();
              }
        },

        addCell: function(content) {
            var cellId = this.getId() +"_" + this.cellId;
            this.cellId++;
            this.jq(ID_CELLS).append(HtmlUtils.div([ATTR_CLASS, "display-notebook-cell", ATTR_ID, cellId], ""));
            var cell = new RamaddaNotebookCell(this,cellId, content);
            cell.createCell();
            return cell;
         },
        clearOutput: function() {
                for(var i=0;i<this.cells.length;i++) {
                    var cell = this.cells[i];
                    cell.clearOutput();
                }
        },

       newCellAbove: function(cell) {
                var cells = [];
                var newCell = null;
                for(var i=0;i<this.cells.length;i++) {
                    if(cell.id == this.cells[i].id) {
                        newCell = this.addCell();
                        cells.push(newCell);
                    }
                    cells.push(this.cells[i]);
                }
                this.cells = cells;
                this.layoutCells();
                newCell.focus();
            },

       newCellBelow: function(cell) {
                var cells = [];
                var newCell = null;
                for(var i=0;i<this.cells.length;i++) {
                    cells.push(this.cells[i]);
                    if(cell.id == this.cells[i].id) {
                        newCell = this.addCell();
                        cells.push(newCell);
                    }
                }
                this.cells = cells;
                this.layoutCells();
                newCell.focus();
            },
       deleteCell: function(cell) {
                cell.jq(ID_CELL).remove();
                var cells = [];
                for(var i=0;i<this.cells.length;i++) {
                    if(cell.id != this.cells[i].id) {
                        cells.push(this.cells[i]);
                    }
                }
                this.cells = cells;
                if(this.cells.length==0) {
                    this.cells.push(this.addCell());
                }
        },
        processCells: function() {
                for(var i=0;i<this.cells.length;i++) {
                    var cell = this.cells[i];
                    if(!cell.doProcess()) {
                        break;
                    }
                }
                    
        }

    });
}


function nbClear() {
    notebookState.cell.notebook.clearOutput();
}

function nbStop() {
    notebookState.stop = true;
}

var notebookState = {
    cell:null,
    stop:false,
    displayNotebookResult:null,
}


function RamaddaNotebookCell(notebook, id, content) {
    this.notebook = notebook;
    this.id = id;
    this.title = "";
    var ID_CELL = "cell";
    var ID_GUTTER = "gutter";
    var ID_INPUT_GUTTER = "inputgutter";
    var ID_OUTPUT_GUTTER = "inputgutter";
    var ID_HEADER = "header";
    var ID_INPUT = "input";
    var ID_OUTPUT = "output";
    var ID_MESSAGE = "message";
    var ID_MENU_BUTTON = "menubutton";
    var ID_MENU = "menu";
    let SUPER  = new DisplayThing(id, {});
    RamaddaUtil.inherit(this, SUPER);
    RamaddaUtil.defineMembers(this, {
        inputRows:1,
        content:content,
        outputHtml:"",
        createCell: function() {
            if(this.content == null) this.content= "html:";
            var _this = this;
            var gutter = HtmlUtils.div([ATTR_CLASS, "display-notebook-gutter", ATTR_ID, this.getDomId(ID_GUTTER)], "");
            var header = HtmlUtils.div([ATTR_CLASS, "display-notebook-header", ATTR_ID, this.getDomId(ID_HEADER)], "");
            var input = HtmlUtils.textarea(TAG_INPUT, this.content, ["rows", this.inputRows, ATTR_CLASS, "display-notebook-input", ATTR_ID, this.getDomId(ID_INPUT)]);

            input = HtmlUtils.div(["class", "display-notebook-input-container"], input);
            var output = HtmlUtils.div([ATTR_CLASS, "display-notebook-output", ATTR_ID, this.getDomId(ID_OUTPUT)], this.outputHtml);
            output = HtmlUtils.div(["class", "display-notebook-output-container"], output);
            var menu = HtmlUtils.div(["id", this.getDomId(ID_MENU),"class","ramadda-popup"],"");
            var inputGutter = HtmlUtils.div(["class", "display-notebook-gutter", ATTR_ID, this.getDomId(ID_INPUT_GUTTER)],
                                            menu +
                                            HtmlUtils.div(["class", "display-notebook-menu-button","id", this.getDomId(ID_MENU_BUTTON)],HtmlUtils.image(icon_menu,[])));
            var outputGutter = HtmlUtils.div(["class", "display-notebook-gutter", ATTR_ID, this.getDomId(ID_OUTPUT_GUTTER)]);

            var html = header+
                HtmlUtils.tag("table",["cellspacing","0", "cellpadding", "0", "width","100%","border","0"],HtmlUtils.tr(["valign","top"],
                                                                                 HtmlUtils.td(["width","5","class", "display-notebook-gutter-container"],inputGutter) +
                                                      HtmlUtils.td([],input)) +
                              HtmlUtils.tr(["valign","top"],
                                           HtmlUtils.td(["width","5","class", "display-notebook-gutter-container"],outputGutter) +
                                           HtmlUtils.td([],output)));
            html= HtmlUtils.div(["id", this.getDomId(ID_CELL)],html);
            $("#" + this.id).html(html);
            this.menuButton = this.jq(ID_MENU_BUTTON);
            this.cell = this.jq(ID_CELL);
            this.input = this.jq(ID_INPUT);
            this.output = this.jq(ID_OUTPUT);
            this.menuButton.click(function() {
                    var menu = "";
                    menu += HtmlUtils.div(["class", "ramadda-link","what","run"],"Run (shift-return)")+"<br>";
                    menu += HtmlUtils.div(["class", "ramadda-link","what","runall"],"Run All (ctrl-return)")+"<br>";
                    menu += HtmlUtils.div(["class", "ramadda-link","what","clear"],"Clear")+"<br>";
                    menu += HtmlUtils.div(["class", "ramadda-link","what","clearall"],"Clear All")+"<br>";
                    menu += HtmlUtils.div(["class", "ramadda-link","what","newabove"],"New Cell Above")+"<br>";
                    menu += HtmlUtils.div(["class", "ramadda-link","what","newbelow"],"New Cell Below")+"<br>";
                    menu += HtmlUtils.div(["class", "ramadda-link","what","delete"],"Delete")+"<br>";
                    menu += HtmlUtils.div(["class", "ramadda-link","what","help"],"Help")+"<br>";
                    var popup =_this.jq(ID_MENU);
                    popup.html(HtmlUtils.div(["class", "ramadda-popup-inner"], menu));
                    popup.show();
                    popup.position({
                            of: _this.menuButton,
                                my: "left top",
                                at: "right top",
                        collision: "fit fit"
                    });
                    popup.find(".ramadda-link").click(function(){
                        var what = $(this).attr("what");
                        popup.hide();
                        if(what == "run") {
                            _this.doProcess();
                        } else  if(what == "runall") {
                            _this.notebook.processCells();
                        } else  if(what == "clear") {
                            _this.clearOutput();
                        } else  if(what == "clearall") {
                            _this.notebook.clearOutput();
                        } else  if(what == "newabove") {
                            _this.notebook.newCellAbove(_this);
                        } else  if(what == "newbelow") {
                            _this.notebook.newCellBelow(_this);
                        } else  if(what == "help") {
                            var win = window.open(ramaddaBaseUrl+"/userguide/wikidisplay.html#notebook", '_blank');
                            win.focus();
                        } else  if(what == "delete") {
                            _this.askDelete();
                        }
                    });

                });
            var hoverIn = function() {
                _this.menuButton.css("display","block");
                _this.cell.find(".display-notebook-gutter-container").css("background","rgb(250,250,250)");
                _this.cell.find(".display-notebook-gutter-container").css("border-right","1px #ccc solid");   
            } 
           var hoverOut = function() {
               _this.jq(ID_MENU).hide();
                _this.menuButton.css("display","none");
                _this.cell.find(".display-notebook-gutter-container").css("background","#fff");
                _this.cell.find(".display-notebook-gutter-container").css("border-right","1px #fff solid");   
            }
            this.cell.hover(hoverIn, hoverOut);
            //            this.output.hover(hoverIn, hoverOut);
            this.calculateInputHeight();
            this.input.on('input selectionchange propertychange', function() {
                    _this.calculateInputHeight();
                });
            this.input.keypress(function(e) {
                if (e.which == 0) {
                    return;
                }
                if (e.which == 13  && e.shiftKey)  {
                    if (e.preventDefault) {
                        e.preventDefault();
                    }
                    _this.doProcess();
                }
                if (e.which == 13  && e.ctrlKey)  {
                    if (e.preventDefault) {
                        e.preventDefault();
                    }

                    _this.notebook.processCells();
                }

            });
       },
        askDelete: function() {
                let _this = this;
                var menu = "";
                menu += "You sure you want to delete this cell?<br>";
                menu += HtmlUtils.div(["class", "ramadda-link","what","yes"],"Yes")+"<br>";
                menu += HtmlUtils.div(["class", "ramadda-link","what","cancel"],"No");
                var popup =_this.jq(ID_MENU);
                popup.html(HtmlUtils.div(["class", "ramadda-popup-inner"], menu));
                popup.show();
                popup.position({
                        of: this.menuButton,
                            my: "left top",
                            at: "right top",
                            collision: "fit fit"
                            });
                popup.find(".ramadda-link").click(function(){
                        var what = $(this).attr("what");
                        popup.hide();
                        if(what == "yes") {
                            _this.notebook.deleteCell(_this);
                        }
                    });


         },
        doProcess: function() {
            var value = this.input.val();
            value = value.trim();
            if (value == "") {
                return true;
            }
            var result = "";
            var type = "html";
            var blob = "";
            var commands = value.split("\n");
            var types = ["html","sh","js"];
            var ok = true;
            for (var i = 0; i < commands.length; i++) {
                if(!ok) break;
                value = commands[i].trim();
                var isType = false;
                for(var typeIdx=0;typeIdx<types.length;typeIdx++) {
                    var t = types[typeIdx];
                    if(value.startsWith(t+":") ) {
                        var r = {ok:true}
                        var blobResult = this.processBlob(type,blob,r);
                        if(blobResult) {
                            if(blobResult!="") {
                                blobResult +="<br>";
                            }
                            result += blobResult;
                        }
                        ok = r.ok;
                        blob = value.substring((t+":").length);
                        if(blob!="") blob+="\n";
                        type = t;
                        isType = true;
                        break;
                    }
                }
                if(isType) continue;
                blob= blob+value +"\n";
            } 
           if(ok) {
                var r = {ok:true}
                var blobResult = this.processBlob(type,blob,r);
                if(blobResult) {
                    if(blobResult!="") {
                        blobResult +="<br>";
                    }
                    result += blobResult;
                }
                ok = r.ok;
            }
           this.outputHtml  = result;
            this.output.html(result);
            return ok;
        },
       outputUpdated: function() {
            this.outputHtml =  this.jq(ID_OUTPUT).html();
            
        },
        focus: function() {
            this.input.focus();
        },
        clearOutput: function() {
            this.output.html("");
            this.outputHtml  = "";
        },
        processHtml: function(blob, result) {
                blob=blob.trim();
                blob = blob.replace(/\n/g,"<br>");
                return blob;
        },
        processSh: function(blob, result) {
                var r = "";
                var lines =blob.split("\n");
                for(var i=0;i<lines.length;i++) {
                    var line = lines[i];
                    if(line == "") continue;
                    var toks = line.split(" ");
                    var command = toks[0].trim();
                    if (this["processCommand_" + command]) {
                        r+=this["processCommand_" + command](line, toks);
                        r+="<br>";
                    } else {
                        r += this.processCommand_help(line, toks, "Unknown command: <i>" + command + "</i>");
                        r+="<br>";
                    }
                }
                return r;
        },
         processJs: function(blob, result) {
                try {
                    notebookState.stop = false;
                    notebookState.cell = this;
                    notebookState.displayNotebookResult = null;
                    var js = "function nbFunc() {\n" + blob +"\n}\nnotebookState.displayNotebookResult = nbFunc();";
                    var r = eval(js);
                    if(notebookState.stop) {
                        result.ok = false;
                    }
                    if(!notebookState.displayNotebookResult) return "";
                    return notebookState.displayNotebookResult;
                } catch(e) {
                    result.ok = false;
                    var lines = blob.split("\n");
                    var line = lines[e.lineNumber-2];
                    return "Error: " + e.message+ "<br>" +HtmlUtils.span(["class","display-notebook-error"], " &gt; " +line);
                }
        },

        processBlob: function(type,blob,result) {
                if(blob=="") {
                    return "";
                }
                if(type == "html") {
                    return this.processHtml(blob,result);
                } else if(type == "js") {
                    return this.processJs(blob,result);
                } else {
                    return this.processSh(blob,result);
                }
                return "unknown:" + blob;
        },


        calculateInputHeight: function() {
              this.content = this.input.val();
              var lines = this.content.split("\n");
              if(lines.length!=this.inputRows) {
                  this.inputRows = lines.length;
                  this.input.attr("rows", Math.max(1,this.inputRows));
              }
        },

        writeStatusMessage: function(v) {
            var msg = this.jq(ID_MESSAGE);
            if (!v) {
                msg.hide();
                msg.html("");
            } else {
                msg.show();
                msg.position({
                    of: this.getOutput(),
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
        },
        writeInput: function(v) {
            var input = this.getInput();
            if (input.val() == v) {
                return;
            }
            input.val(v).focus();
            this.currentInput = this.getInput().val();
        },
        getOutput: function() {
            return this.jq(ID_OUTPUT);
        },
        getInput: function() {
            return this.jq(ID_INPUT);
        },
        writeResult: function(html) {
            this.writeStatusMessage(null);
            html = HtmlUtils.div([ATTR_CLASS, "display-notebook-result"], html);
            var output = this.jq(ID_OUTPUT);
            output.append(html);
            output.animate({
                scrollTop: output.prop("scrollHeight")
            }, 1000);
            this.currentOutput = output.html();
            this.currentInput = this.getInput().val();

        },
        writeError: function(msg) {
            this.writeStatusMessage(msg);
            //                this.writeResult(msg);
        },
        header: function(msg) {
            return HtmlUtils.div([ATTR_CLASS, "display-notebook-header"], msg);
        },
        processCommand_help: function(line, toks, prefix) {
            var help = "";
            if (prefix != null) help += prefix;
            help += "<pre>pwd, ls, cd</pre>";
            return help;
        },
         displayEntries: function(entries, divId) {
            this.currentEntries = entries;
            var html = this.notebook.getEntriesTree(entries, {
                showIndex: true,
                suffix: "_shell_" + (this.uniqueCnt++)
            });
            $("#"+ divId).html(html);
            this.outputUpdated();
            },
        getEntryFromArgs: function(args, dflt) {
            var currentEntries = this.currentEntries;
            if (currentEntries == null) {
                return dflt;
            }
            for (var i = 0; i < args.length; i++) {
                var arg = args[i];
                if (arg.match("^\d+$")) {
                    var index = parseInt(arg);
                    break;
                }
                if (arg == "-entry") {
                    i++;
                    var index = parseInt(args[i]) - 1;
                    if (index < 0 || index >= currentEntries) {
                        this.writeError("Bad entry index:" + index + " should be between 1 and " + currentEntries.length);
                        return;
                    }
                    return currentEntries[index];
                }
            }
            return dflt;
        },
        getCurrentEntry: function() {
            if (this.currentEntry == null) {
                this.rootEntry = new Entry({
                    id: ramaddaBaseEntry,
                    name: "Root",
                    type: "group"
                });
                this.currentEntry = this.rootEntry;
            }
            return this.currentEntry;
        },
        processCommand_pwd: function(line, toks) {
            var entry = this.getCurrentEntry();
            var icon = entry.getIconImage([ATTR_TITLE, "View entry"]);
            var html = this.getEntriesTree([entry], {
                suffix: "_YYY"
            });
            //                var link  =  HtmlUtils.tag(TAG_A,[ATTR_HREF, entry.getEntryUrl()],icon+" "+ entry.getName());
            this.writeResult("Current entry:<br>" + html);
        },
        createPointDisplay: function(line, toks, displayType) {
            var entry = this.getEntryFromArgs(toks, this.getCurrentEntry());
            console.log("createDisplay got:" + entry.getName());
            var jsonUrl = this.getPointUrl(entry);
            if (jsonUrl == null) {
                this.writeError("Not a point type:" + entry.getName());
                return;
            }
            this.createDisplay(entry.getId(), displayType, jsonUrl);
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
            if (toks.length == 0) {
                this.currentEntry = this.rootEntry;
                this.processCommand_pwd("pwd", []);
                return;
            }
            var index = parseInt(toks[1]) - 1;
            if (index < 0 || index >= this.currentEntries.length) {
                this.writeError("Out of bounds: between 1 and " + this.currentEntries.length);
                return;
            }
            this.currentEntry = this.currentEntries[index];
            this.processCommand_pwd("pwd", []);
        },
        processCommand_ls: function(line, toks) {
            let _this = this;
            let divId = HtmlUtils.getUniqueId();
            var callback = function(children) {
                _this.displayEntries(children, divId);
            };
            var children = this.getCurrentEntry().getChildrenEntries(callback, "");
            if (children != null) {
                setTimeout(function() {_this.displayEntries(children, divId);},1);
            } 
            return  HtmlUtils.div(["style","max-height:200px;overflow-y:auto;", "id",divId],"Listing entries...");
        },
        entryListChanged: function(entryList) {
            var entries = entryList.getEntries();
            if (entries.length == 0) {
                this.writeStatusMessage("Sorry, nothing found");
            } else {
                this.displayEntries(entries);
            }
        },
        processCommand_search: function(line, toks) {
            var text = "";
            for (var i = 1; i < toks.length; i++) text += toks[i] + " ";
            text = text.trim();
            var searchSettings = new EntrySearchSettings({
                text: text,
            });
            var repository = this.getRamadda();
            this.writeStatusMessage("Searching...");
            var jsonUrl = repository.getSearchUrl(searchSettings, OUTPUT_JSON);
            this.entryList = new EntryList(repository, jsonUrl, this, true);
            //                this.writeResult(line);
        },
        processCommand_taller: function(line, toks) {
            if (!this.outputHeight) {
                this.outputHeight = 300;
            }
            this.outputHeight += parseInt(this.outputHeight * .30);
            this.jq(ID_OUTPUT).css('max-height', this.outputHeight);
            this.jq(ID_OUTPUT).css('height', this.outputHeight);
        },
        processCommand_shorter: function(line, toks) {
            if (!this.outputHeight) {
                this.outputHeight = 300;
            }
            this.outputHeight -= parseInt(this.outputHeight * .30);
            this.jq(ID_OUTPUT).css('max-height', this.outputHeight);
            this.jq(ID_OUTPUT).css('height', this.outputHeight);
        },

        processCommand_clear: function(toks) {
            this.clearOutput();
            this.clearInput();
        },
                });

}


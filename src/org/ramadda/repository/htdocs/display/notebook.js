/**
Copyright 2008-2019 Geode Systems LLC
*/




var DISPLAY_NOTEBOOK = "notebook";
addGlobalDisplayType({
    type: DISPLAY_NOTEBOOK,
    label: "Notebook",
    requiresData: false,
    category: "Misc"
});

function RamaddaNotebookDisplay(displayManager, id, properties) {
    var ID_NOTEBOOK = "notebook";
    var ID_CELLS = "cells";
    var ID_CELL = "cell";
    var ID_MENU = "menu";
    let SUPER =  new RamaddaDisplay(displayManager, id, DISPLAY_NOTEBOOK, properties);
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
        cells: [],
        cellCount: 0,
        runOnLoad: true,
        displayMode:false,
        fetchedNotebook: false, 
        currentEntries:{},
        baseEntries:{},
        columns:1,
        initDisplay: async function() {
            let _this = this;
            this.createUI();
            var contents = HtmlUtils.div([ATTR_CLASS, "display-notebook-cells", ATTR_ID, this.getDomId(ID_CELLS)], "Loading...");
            var popup =  HtmlUtils.div(["class","ramadda-popup",ATTR_ID, this.getDomId(ID_MENU)]);
            contents = HtmlUtils.div([ATTR_ID, this.getDomId(ID_NOTEBOOK)], popup +contents);
            this.setContents(contents);
            this.jq(ID_NOTEBOOK).hover(()=>{}, ()=>{this.jq(ID_MENU).hide()});
            if (!this.fetchedNotebook) {
                this.fetchedNotebook = true;
                await this.getEntry(this.getProperty("entryId",""),entry=> {
                        this.baseEntry=entry;});
                await this.baseEntry.getRoot(entry=> {this.rootEntry=entry;});
                var id = this.getProperty("entryId", "");
                var url = ramaddaBaseUrl + "/getnotebook?entryid=" + id;
                url += "&notebookId=" + this.getProperty("notebookId", "default_notebook");
                var jqxhr = $.getJSON(url,  function(data) {
                        _this.loadJson(data);
                    }).fail(function() {
                            var props = {
                                showInput: true,
                            }
                            this.addCell("init cell", props,false).run();
                            this.cells[0].focus();
                        });
                
            } else {
                this.layoutCells();
            }
        },
        getBaseEntry: function() {
                return this.baseEntry;
        },
        getRootEntry: function() {
                return this.rootEntry;
        },
        getPopup: function() {
            return  this.jq(ID_MENU);
        },
        loadJson: async function(data) {
                if (data.error) {
                    this.setContents(_this.getMessage("Failed to load notebook: " + data.error));
                    return;
                }
                if (Utils.isDefined(data.runOnLoad)) {
                    this.runOnLoad= data.runOnLoad;
                }
                if (Utils.isDefined(data.displayMode)) {
                    this.displayMode =data.displayMode;
                }
                if (Utils.isDefined(data.columns)) {
                    this.columns =data.columns;
                }

                if (Utils.isDefined(data.currentEntries)) {
                    for(a in data.currentEntries) {
                        var obj = {};
                        if(this.currentEntries[a]) continue;
                        obj.name=a;
                        obj.entryId = data.currentEntries[a].entryId;
                        try {
                            await this.getEntry(obj.entryId,e=>obj.entry=e);
                            this.currentEntries[a] = obj;
                        } catch(e) {
                        }
                    }
                }
                if (Utils.isDefined(data.cells)) {
                    this.cells = [];
                    data.cells.map(cell=>this.addCell(cell.outputHtml, cell, true));
                    this.layoutCells();
                }
                if(this.cells.length==0) {
                    var props = {
                        showInput: true,
                    }
                    this.addCell("", props,false);
                    this.layoutCells();
                    this.cells[0].focus();
                }

                if(this.runOnLoad) {
                    this.runAll();
                }
            },
        addEntry: async function(name,entryId) {
                var entry;
                await this.getEntry(entryId, e=> entry=e);
                this.currentEntries[name] = {entryId:entryId,entry:entry};
       },
        getCurrentEntries: function() {
                return this.currentEntries;
         },
        clearEntries: function() {
                this.currentEntries = {};
                for(a in this.baseEntries)
                    this.currentEntries[a] = this.baseEntries[a];
         },
        saveNotebook: function(output) {
            var json = this.getJson(output);
            json = JSON.stringify(json, null, 2);
            var args = {
                entryid: this.getProperty("entryId", ""),
                notebookId: this.getProperty("notebookId", "default_notebook"),
                notebook: json
            };
            var url = ramaddaBaseUrl + "/savenotebook";
            $.post(url, args, function(result) {
                if (result.error) {
                    alert("Error saving notebook: " + result.error);
                    return;
                }
                if (result.result != "ok") {
                    alert("Error saving notebook: " + result.result);
                    return;
                }
                alert("Notebook saved");
            });
        },
        showInput: function() {
                if(this.displayMode && !this.user)
                    return false;
                return true;
        },
        showGutter: function() {
                if(this.displayMode && !this.user)
                    return false;
                return true;
        },
        getJson: function(output) {
            var obj = {
                cells: [],
                currentEntries:{},
                runOnLoad: this.runOnLoad,
                displayMode: this.displayMode,
                columns:this.columns,
            };
            for(var name in this.currentEntries) {
                var e = this.currentEntries[name];
                obj.currentEntries[name] = {entryId:e.entryId};
            }
            this.cells.map(cell=>obj.cells.push(cell.getJson(output)));
            return obj;
        },
        layoutCells: function() {
            this.jq(ID_CELLS).html("");
            var html = "<div class=row style='padding:0px;margin:0px;'>";
            var clazz= HtmlUtils.getBootstrapClass(this.columns);
            var colCnt = 0;
            for(var i=0;i<this.cells.length;i++) {
                var cell = this.cells[i];
                cell.index = i+1;
                html+=HtmlUtils.openTag("div",["class", clazz]);
                html+=HtmlUtils.openTag("div",["style","max-width:100%;overflow-x:auto;padding:0px;margin:px;"]);
                html+=HtmlUtils.div([ATTR_CLASS, "display-notebook-cell", ATTR_ID, cell.id], "");
                html+=HtmlUtils.closeTag("div");
                html+=HtmlUtils.closeTag("div");
                colCnt++;
                if(colCnt>=this.columns) {
                    colCnt=0;
                    html+=HtmlUtils.closeTag("div");
                    html+="<div class=row style='padding:0px;margin:0px;'>";
                }
            };
            html+=HtmlUtils.closeTag("div");

            this.jq(ID_CELLS).append(html);
            for(var i=0;i<this.cells.length;i++) {
                var cell = this.cells[i];
                cell.createCell();
            };
        },
        addCell: function(content, props,layoutLater) {
                cell = this.createCell(content, props);
                this.cells.push(cell);
                if(!layoutLater) {
                    this.jq(ID_CELLS).append(HtmlUtils.div([ATTR_CLASS, "display-notebook-cell", ATTR_ID, cell.id], ""));
                    cell.createCell();
                }
                return cell;
        },
        createCell: function(content, props) {
            if (!props) props = {
                    showInput:true
                };
            var cellId = this.getId() + "_" + this.cellCount;
            //Override any saved id
            props.id  = cellId;
            this.cellCount++;
            this.jq(ID_CELLS).append(HtmlUtils.div([ATTR_CLASS, "display-notebook-cell", ATTR_ID, cellId], ""));
            var cell = new RamaddaNotebookCell(this, cellId, content, props);
            return cell;
        },
        clearOutput: function() {
                this.cells.map(cell=> cell.clearOutput());
        },
        getIndex: function(cell) {
            var idx = 0;
            for (var i = 0; i < this.cells.length; i++) {
                if (cell.id == this.cells[i].id) {
                    idx = i;
                    break;
                }
            }
            return idx;
        },
        moveCellUp: function(cell) {
            var cells = [];
            var newCell = null;
            var idx = this.getIndex(cell);
            if (idx == 0) return;
            this.cells.splice(idx, 1);
            this.cells.splice(idx - 1, 0, cell);
            this.layoutCells();
            cell.focus();
        },
        moveCellDown: function(cell) {
            var cells = [];
            var newCell = null;
            var idx = this.getIndex(cell);
            if (idx == this.cells.length - 1) return;
            this.cells.splice(idx, 1);
            this.cells.splice(idx + 1, 0, cell);
            this.layoutCells();
            cell.focus();
        },

        newCellAbove: function(cell) {
            var cells = [];
            var newCell = null;
            for (var i = 0; i < this.cells.length; i++) {
                if (cell.id == this.cells[i].id) {
                    newCell = this.createCell(null, {
                        showInput: true,
                        showHeader: false
                    });
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
            for (var i = 0; i < this.cells.length; i++) {
                cells.push(this.cells[i]);
                if (cell.id == this.cells[i].id) {
                    newCell = this.createCell(null, {
                        showInput: true,
                        showHeader: false
                    });
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
            this.cells.map(c=>{
                if (cell.id != c.id) {cells.push(c);}
                });
            this.cells = cells;
            if (this.cells.length == 0) {
                this.addCell("",null);
            }
        },
        cellValues:{},
        setCellValue: function(name,value) {
            this.cellValues[name] = value;
        },
        getCellValues: function () {
            return this.cellValues;
        },
        convertInput: function(input) {
            for(name in this.cellValues) {
                var re = new RegExp("\\$\\{" + name +"\\}","g");
                input = input.replace(re,this.cellValues[name]);
            }
            return input;
        },
        runAll: async function() {
            var ok = true;
            this.cellValues={};
            for (var i = 0; i < this.cells.length; i++) {
                var cell = this.cells[i];
                cell.hasRun = false;
            }
            for (var i = 0; i < this.cells.length; i++) {
                var cell = this.cells[i];
                if(!cell.runFirst) continue;
                await this.runCell(cell).then(result=>ok=result);
            }
            if(!ok) return;
            for (var i = 0; i < this.cells.length; i++) {
                var cell = this.cells[i];
                if(cell.runFirst) continue;
                await this.runCell(cell).then(result=>ok=result);
            }
        },
        runCell: async function(cell) {
                if(cell.hasRun) return true;
                await cell.run(result=>ok=result);
                if(!ok) return false;
                var raw = cell.getRawOutput();
                if(raw) {
                    raw= raw.trim();
                    if(Utils.stringDefined(cell.cellName)) {
                        this.cellValues[cell.cellName] = raw;
                    }
                }
                return true;
            },
        toggleAll: function(on) {
                this.cells.map(cell=>{
                        cell.showInput = on;
                        cell.applyStyle();
                    });
            },

    });
}


function NotebookState(cell,div) {
    this.id = HtmlUtils.getUniqueId();
    this.cell = cell;
    this.notebook = cell.notebook;
    $.extend(this, {
            entries:{},
                div:div,
                stopFlag: false,
                prefix: null,
                result: null,
                getStop:function(){
                return this.stopFlag;
            },
                getCell:function() {
                return this.cell;
            },
            setValue: function(name,value) {
                this.notebook.setCellValue(name,value);
            },
            makeData: async function(entry) {
                if(!entry)
                    await this.getCurrentEntry(e=>entry=e);
                if((typeof entry) =="string") {
                    await this.notebook.getEntry(entry,e=>entry=e);
                }    
                var jsonUrl = this.notebook.getPointUrl(entry);
                if (jsonUrl == null) {
                    this.writeError("Not a point type:" + entry.getName());
                    return null;
                }
                var pointDataProps = {
                    entry: entry,
                    entryId: entry.getId()
                };
                return  new PointData(entry.getName(), null, null, jsonUrl, pointDataProps);
            },
            getNotebook:    function() {
                return this.notebook;
            },

                clear:    function() {
                this.cell.clearOutput();
            },

                save: function(output) {
                this.notebook.saveNotebook(output);
                return "notebook saved";
            },

                clearEntries: function() {
                this.clearEntries();
            },

                ls: async function(entry) {
                var div = new Div();
                if(!entry)
                    await this.getCurrentEntry(e=>entry=e);
                this.call.getEntryHeading(entry, div);
                this.write(div.toString());
            },

                lsEntries: function() {
                var h = "";
                var entries = this.currentEntries;
                for(var name in entries) {
                    var e = entries[name];
                    h += name +"=" + e.entry.getName()+"<br>";
                }
                this.write(h);
            },

                stop: function() {
                this.stopFlag = true;
            },
                setEntry: function(name,entryId) {
                this.cell.notebook.addEntry(name,entryId);
            },
                getEntry: async function(entryId, callback) {
                await this.cell.notebook.getEntry(e=>entry=e);
                return Utils.call(callback, entry);
            },
                wiki: async function(s, entry, callback) {
                var write = false;
                if(!callback)
                    callback = h=>this.write(h);
                if(entry == null) 
                    await this.cell.getCurrentEntry(e=>entry=e);
                if((typeof entry)!="string") entry = entry.getId();
                await GuiUtils.loadHtml(ramaddaBaseUrl + "/wikify?doImports=false&entryid=" + entry + "&text=" + encodeURIComponent(s),
                                        callback);
            },
                write: function(s) {
                if(this.prefix==null) {
                    this.prefix = s;
                } else {
                    this.prefix += s;
                }
            },

            linechart: async function(entry,props) {
                if(!entry) 
                    await this.cell.getCurrentEntry(e=>entry=e);
                this.cell.createDisplay(this,entry,DISPLAY_LINECHART,props);
            },

             help: function() {
                return "Enter an expression, e.g.:<pre>1+2</pre> or wrap you code in brackets:<pre>{\nvar x=1+2;\nreturn x;\n}</pre>" +
                    "functions:<br>nbHelp() : print this help<br>nbClear() : clear the output<br>nbCell : this cell<br>nbNotebook: this notebook<br>" +
                    "nbLs(): list current entry; nbLs(parent): list parent; nbLs(root): list root" +
                    "nbLinechart(entry): create a linechart, defaults to current entry" +
                    "nbNotebook.addCell('wiki:some wiki text'): add a new cell with the given output<br>" +
                    "nbSave(includeOutput): save the notebook<br>nbWrite(html) : add some output<br>nbStop(): stop running";
            }
        });
}


var notebookStates = {};

function RamaddaNotebookCell(notebook, id, content, props) {
    this.notebook = notebook;

    var ID_CELL = "cell";
    var ID_GUTTER = "gutter";
    var ID_GUTTER_CONTAINER = "guttercontainer";
    var ID_HEADER = "header";
    var ID_INPUT = "input";
    var ID_INPUT_TOOLBAR = "inputtoolbar";
    var ID_OUTPUT = "output";
    var ID_MESSAGE = "message";
    var ID_BUTTON_MENU = "menubutton";
    var ID_BUTTON_RUN = "runbutton";
    var ID_BUTTON_TOGGLE = "togglebutton";
    var ID_MENU = "menu";
    var ID_CELLNAME_INPUT = "cellnameinput";
    var ID_SHOWHEADER_INPUT = "showheader";
    var ID_SHOWBORDER = "showborder";
    var ID_SHOWEDIT = "showedit";
    var ID_RUN_ON_LOAD = "runonload";
    var ID_DISPLAY_MODE = "displaymode";
    var ID_LAYOUT_COLUMNS = "layoutcolumns";
    var ID_RUNFIRST = "runfirst";
    var ID_SHOW_OUTPUT= "showoutput";

    let SUPER = new DisplayThing(id, {});
    RamaddaUtil.inherit(this, SUPER);

    RamaddaUtil.defineMembers(this, {
        id: id,
        inputRows: 1,
        index: 0,
        content: content,
        outputHtml: "",
        showInput: false,
        showHeader: false,
        showBorder: false,
        cellName: "",
        runFirst:false,
        showOutput:true,
    });

    if (props) {
        $.extend(this, props);
    }
    RamaddaUtil.defineMembers(this, {
        getJson: function(output) {
                var obj =  {
                id: this.id,
                inputRows: this.inputRows,
                content: this.content,
                showInput: this.showInput,
                showHeader: this.showHeader,
                showBorder: this.showBorder,
                runFirst: this.runFirst,
                showOutput: this.showOutput,
                cellName: this.cellName,
                };
                if(this.currentEntry)
                    obj.currentEntryId = this.currentEntry.getId();
                if(output)
                    obj.outputHtml= this.outputHtml;
                return obj;
        },
        createCell: function() {
                if (this.content == null) {
                    this.content = "wiki:";
                }
            this.editId= addHandler(this);
            addHandler(this,this.editId+"_entryid");
            addHandler(this,this.editId+"_wikilink");
            var _this = this;
            var buttons = 
                this.makeButton(ID_BUTTON_MENU, icon_menu, "Show menu","showmenu") +
                this.makeButton(ID_BUTTON_TOGGLE, Utils.getIcon("edit.png"), "Toggle input","toggle") +
                this.makeButton(ID_BUTTON_RUN, Utils.getIcon("run.png"), "Run","run") +
                this.makeButton(ID_BUTTON_RUN, Utils.getIcon("runall.png"), "Run all","runall");
            var hbuttons = 
                this.makeButton(ID_BUTTON_MENU, icon_menu, "Show menu","showmenu",true) +
                this.makeButton(ID_BUTTON_TOGGLE, Utils.getIcon("edit.png"), "Toggle input","toggle",true) +
                this.makeButton(ID_BUTTON_RUN, Utils.getIcon("run.png"), "Run","run",true) +
                this.makeButton(ID_BUTTON_RUN, Utils.getIcon("runall.png"), "Run all","runall",true);


            var header = HtmlUtils.div([ATTR_CLASS, "display-notebook-header", ATTR_ID, this.getDomId(ID_HEADER)],"&nbsp;" + hbuttons + "&nbsp;" + this.cellName);
            var input = HtmlUtils.textarea(TAG_INPUT, this.content, ["rows", this.inputRows, ATTR_CLASS, "display-notebook-input", ATTR_ID, this.getDomId(ID_INPUT)]);
            var inputToolbar = HtmlUtils.div(["id",this.getDomId(ID_INPUT_TOOLBAR)],"");

            input = HtmlUtils.div(["class", "display-notebook-input-container"], input);
            var output = HtmlUtils.div([ATTR_CLASS, "display-notebook-output", ATTR_ID, this.getDomId(ID_OUTPUT)], this.outputHtml);
            output = HtmlUtils.div(["class", "display-notebook-output-container"], output);
            var menu = HtmlUtils.div(["id", this.getDomId(ID_MENU), "class", "ramadda-popup"], "");
            var gutter = HtmlUtils.div(["class", "display-notebook-gutter", ATTR_ID, this.getDomId(ID_GUTTER)],
                                       menu +
                                       buttons);

            var table = HtmlUtils.openTag("table", ["cellspacing", "0", "cellpadding", "0", "width", "100%", "border", "0"]);
            table += HtmlUtils.tr(["valign", "top"], HtmlUtils.td([],"") + HtmlUtils.td([], inputToolbar));
            if(this.notebook.showGutter()) {
                table += HtmlUtils.tr(["valign", "top"],
                                      HtmlUtils.td(["id",this.getDomId(ID_GUTTER_CONTAINER),"rowspan", "3", "width", "5", "class", "display-notebook-gutter-container"], gutter) +
                                      HtmlUtils.td([], ""));
            }
            table += HtmlUtils.tr(["valign", "top"], "\n" + HtmlUtils.td([], input));
            table += HtmlUtils.tr(["valign", "top"], "\n" + HtmlUtils.td([], output));
            table += "</table>";

            var html =  header +table;
            html = HtmlUtils.div(["id", this.getDomId(ID_CELL)], html);
            $("#" + this.id).html(html);
            var url = ramaddaBaseUrl +"/wikitoolbar?entryid=" + this.entryId +"&handler=" + this.editId;
            url+="&extrahelp=" + ramaddaBaseUrl +"/userguide/notebook.html|Notebook Help";
            GuiUtils.loadHtml(url,  h=> {
                    this.inputToolbar =h; 
                    this.jq(ID_INPUT_TOOLBAR).html(h);
                    $("#"+this.editId +"_prefix").html(HtmlUtils.span(["id",this.getDomId("toolbar_notebook"),
                                                                       "style","border-right:1px #ccc solid;",
                                                                       "class","ramadda-menubar-button"],"Notebook"));
                    this.jq("toolbar_notebook").click(()=>this.showNotebookMenu());

                });
            this.header = this.jq(ID_HEADER);
            this.menuButton = this.jq(ID_BUTTON_MENU);
            this.toggleButton = this.jq(ID_BUTTON_TOGGLE);
            this.cell = this.jq(ID_CELL);
            this.input = this.jq(ID_INPUT);
            this.output = this.jq(ID_OUTPUT);
            this.inputContainer = this.cell.find(".display-notebook-input-container");
            this.inputMenu = this.cell.find(".display-notebook-input-container");
            this.gutter = this.jq(ID_GUTTER);
            this.gutterContainer = this.jq(ID_GUTTER_CONTAINER);
            this.applyStyle();
            this.gutter.find(".display-notebook-menu-button").click(function(){
                    _this.processCommand($(this).attr("what"));
                });
            this.header.find(".display-notebook-menu-button").click(function(){
                    _this.processCommand($(this).attr("what"));
                });

            this.cell.hover(()=>this.checkHover(true), ()=>this.checkHover(false));
            this.gutterContainer.mousemove(()=>this.checkHover(true));
            this.calculateInputHeight();
            this.input.focus(()=>this.getPopup().hide());
            this.input.click(()=>this.getPopup().hide());
            this.output.click(()=>this.getPopup().hide());
            this.input.on('input selectionchange propertychange', ()=>this.calculateInputHeight());
            this.input.keydown(function(e) {
                var key = e.key;
                if (key == 'v' && e.ctrlKey) {
                    _this.notebook.moveCellDown(_this);
                    return;
                }
                if (key == 6 && e.ctrlKey) {
                    _this.notebook.moveCellUp(_this);
                    return;
                }
            });
            this.input.keypress(function(e) {
                var key = e.key;
                if (key == 'Enter') {
                    if (e.shiftKey) {
                        if (e.preventDefault) {
                            e.preventDefault();
                        }
                        _this.run();
                    } else if (e.ctrlKey) {
                        if (e.preventDefault) {
                            e.preventDefault();
                        }
                        _this.notebook.runAll();
                    }
                }

            });
        },
        selectClick(type,id, entryId, value) {
                if(type == "entryid") {
                    this.insertText(entryId);
                } else {
                     this.insertText("[[" + entryId +"|" + value+"]]");
                }
                this.input.focus();
            },
            insertTags: function(tagOpen, tagClose, sampleText) {
                var id = this.getDomId(ID_INPUT);
                var textComp = GuiUtils.getDomObject(id);
                insertTagsInner(id, textComp.obj, tagOpen, tagClose, sampleText);
                this.calculateInputHeight();
            },
            insertText: function(value) {
                var id = this.getDomId(ID_INPUT);
                var textComp = GuiUtils.getDomObject(id);
                insertAtCursor(id, textComp.obj, value);
                this.calculateInputHeight();
            },
         showNotebookMenu: function() {
             var link = this.jq("toolbar_notebook");
             this.makeMenu(link,"left bottom");
         },
          makeButton: function(id, icon,title, command, horiz) {
             if(!command) command="noop";
             var style = "";
             if(horiz) style = "display:inline-block;";
             return  HtmlUtils.div(["what", command, "title",title, "class", "display-notebook-menu-button", "style",style,"id", this.getDomId(id)], HtmlUtils.image(icon, []));
         },
         makeMenu: function(src, at) {
            if(!src) {
                src = this.input;
            }
            if(!src.is(":visible"))  {
                src  = this.output;
            }
            if(!src.is(":visible"))  {
                src = this.header;
            }
             if(!at) at = "left top";
             let _this = this;
                var space = "&nbsp;&nbsp;";
                var line =  "<tr><td colspan=2 style='border-top:1px #ccc solid;'</td></tr>";
                var line2 =  "<div style='border-top:1px #ccc solid;'</div>"
                var menu = "";
                menu += HtmlUtils.input(ID_CELLNAME_INPUT, _this.cellName, ["placeholder", "Cell name", "style", "width:100%;", "xsize", "20", "id", _this.getDomId(ID_CELLNAME_INPUT)]);
                menu += "<br>";
                menu += "<table>";

                menu += "<tr><td align=right><b>Run:</b>&nbsp;</td><td>";
                menu += HtmlUtils.div(["title", "shift-return", "class", "ramadda-link", "what", "run"], "This cell") + space;
                menu += HtmlUtils.div(["title", "ctrl-return", "class", "ramadda-link", "what", "runall"], "All");
                menu += "</td></tr>"

                menu += "<tr><td align=right><b>Clear:</b>&nbsp;</td><td>";
                menu += HtmlUtils.div(["class", "ramadda-link", "what", "clear"], "This cell") + space;
                menu += HtmlUtils.div(["class", "ramadda-link", "what", "clearall"], "All");
                menu += "</td></tr>"

                menu += "<tr><td align=right><b>New Cell:</b>&nbsp;</td><td>";
                menu += HtmlUtils.div(["class", "ramadda-link", "what", "newabove"], "Above") + space;
                menu += HtmlUtils.div(["class", "ramadda-link", "what", "newbelow"], "Below");
                menu += "</td></tr>"



                menu += "<tr><td align=right><b>Move:</b>&nbsp;</td><td>";
                menu += HtmlUtils.div(["title", "ctrl-^", "class", "ramadda-link", "what", "moveup"], "Up") + space;
                menu += HtmlUtils.div(["title", "ctrl-v", "class", "ramadda-link", "what", "movedown"], "Down");
                menu += "</td></tr>"

                menu += line;

                menu += "<tr valign=top><td align=right><b>Show:</b>&nbsp;</td><td>";
                menu += HtmlUtils.checkbox(_this.getDomId(ID_SHOWEDIT), [],
                    _this.showInput) + " Input";
                menu += space;
                menu += HtmlUtils.checkbox(_this.getDomId(ID_SHOW_OUTPUT), [],_this.showOutput) +" Output";
                //                menu += HtmlUtils.checkbox(_this.getDomId(ID_SHOWHEADER_INPUT), [],  _this.showHeader) + " Header";
                //                menu += "<br>";
                //                menu += HtmlUtils.checkbox(_this.getDomId(ID_SHOWBORDER), [],  _this.showBorder) + " Border";

                menu += "</td></tr>"

                menu += "<tr><td align=right><b>Inputs:</b>&nbsp;</td><td>";
                menu += HtmlUtils.div(["title", "ctrl-return", "class", "ramadda-link", "what", "showall"], "Show All");
                menu += space;
                menu += HtmlUtils.div(["title", "ctrl-return", "class", "ramadda-link", "what", "hideall"], "Hide All");
                menu += "</td></tr>";

                menu += line;
                menu += "</table>";
                menu += HtmlUtils.checkbox(_this.getDomId(ID_RUNFIRST), [],_this.runFirst) +" Run first" +"<br>";
                menu += HtmlUtils.checkbox(_this.getDomId(ID_RUN_ON_LOAD), [],_this.notebook.runOnLoad) +" Run on load" +"<br>";
                menu += HtmlUtils.div(["title","Don't show the left side and input for anonymous users"],HtmlUtils.checkbox(_this.getDomId(ID_DISPLAY_MODE), [],_this.notebook.displayMode) +" Display mode" +"<br>");

                menu += line2;
                //                menu += "<b>Layout</b><br>";
                var cols = this.notebook.columns;
                var colId = _this.getDomId(ID_LAYOUT_COLUMNS);
                menu+="Columns: ";
                menu += HtmlUtils.input(colId, this.notebook.columns, ["size", "3", "id", _this.getDomId(ID_LAYOUT_COLUMNS)]);
                menu+="<br>";

                menu += line2;
                menu += HtmlUtils.div(["class", "ramadda-link", "what", "savewithout"], "Save Notebook (w/o output)") + "<br>";
                menu += HtmlUtils.div(["class", "ramadda-link", "what", "savewith"], "Save Notebook (with output)") + "<br>";
                menu += line2;
                menu += HtmlUtils.div(["class", "ramadda-link", "what", "delete"], "Delete") + "<br>";
                menu += HtmlUtils.div(["class", "ramadda-link", "what", "help"], "Help") + "<br>";
                var popup = this.getPopup();
                popup.html(HtmlUtils.div(["class", "ramadda-popup-inner"], menu));
                popup.show();
                popup.position({
                    of: src,
                    my: "left top",
                    at: at,
                    collision: "fit fit"
                });
                _this.jq(ID_SHOWHEADER_INPUT).focus();

                _this.jq(ID_SHOWHEADER_INPUT).change(function(e) {
                    _this.showHeader = _this.jq(ID_SHOWHEADER_INPUT).is(':checked');
                    _this.applyStyle();
                });


                _this.jq(ID_RUNFIRST).change(function(e) {
                        _this.runFirst = _this.jq(ID_RUNFIRST).is(':checked');
                    });
                _this.jq(ID_SHOW_OUTPUT).change(function(e) {
                        _this.showOutput = _this.jq(ID_SHOW_OUTPUT).is(':checked');
                        _this.applyStyle();
                    });
                _this.jq(ID_RUN_ON_LOAD).change(function(e) {
                        _this.notebook.runOnLoad = _this.jq(ID_RUN_ON_LOAD).is(':checked');
                    });
                _this.jq(ID_DISPLAY_MODE).change(function(e) {
                        _this.notebook.displayMode = _this.jq(ID_DISPLAY_MODE).is(':checked');
                    });
                _this.jq(ID_SHOWBORDER).change(function(e) {
                    _this.showBorder = _this.jq(ID_SHOWBORDER).is(':checked');
                    _this.applyStyle();
                });
                _this.jq(ID_SHOWEDIT).change(function(e) {
                    _this.showInput = _this.jq(ID_SHOWEDIT).is(':checked');
                    _this.applyStyle();
                });

                _this.jq(ID_LAYOUT_COLUMNS).keypress(function(e) {
                        var keyCode = e.keyCode || e.which;
                        if (keyCode != 13) {
                            return;
                        }
                        var cols = parseInt(_this.jq(ID_LAYOUT_COLUMNS).val());
                        if(isNaN(cols)) {
                            _this.jq(ID_LAYOUT_COLUMNS).val("bad:" + _this.jq(ID_LAYOUT_COLUMNS).val());
                            return;
                        }
                        _this.notebook.columns = cols;
                        popup.hide();
                        _this.notebook.layoutCells();
                    });
                _this.jq(ID_CELLNAME_INPUT).keypress(function(e) {
                    var keyCode = e.keyCode || e.which;
                    if (keyCode == 13) {
                        _this.cellName = $(this).val();
                        _this.header.html("&nbsp;" + _this.cellName);
                        _this.applyStyle();
                        popup.hide();
                        return;
                    }
                });
                popup.find(".ramadda-link").click(function() {
                    var what = $(this).attr("what");
                    //                    popup.hide();
                    _this.processCommand(what);
                });
        },
             processCommand: function(command) {
                    if (command == "run") {
                        this.notebook.runCell(this);
                    } else if(command== "showmenu") {
                        this.makeMenu();
                    } else if (command == "toggle") {
                        this.showInput = !this.showInput;
                        this.applyStyle();
                    } else if (command == "showthis") {
                        this.showInput = true;
                        this.applyStyle();
                    } else if (command == "hidethis") {
                        this.showInput = false;
                        this.applyStyle();
                    } else if (command == "showall") {
                        this.notebook.toggleAll(true);
                    } else if (command == "hideall") {
                        this.notebook.toggleAll(false);
                    } else if (command == "runall") {
                        this.notebook.runAll();
                    } else if (command == "clear") {
                        this.clearOutput();
                    } else if (command == "clearall") {
                        this.notebook.clearOutput();
                    } else if (command == "moveup") {
                        this.notebook.moveCellUp(this);
                    } else if (command == "movedown") {
                        this.notebook.moveCellDown(this);
                    } else if (command == "newabove") {
                        this.notebook.newCellAbove(this);
                    } else if (command == "newbelow") {
                        this.notebook.newCellBelow(this);
                    } else if (command == "savewith") {
                        this.getPopup().hide();
                        this.notebook.saveNotebook(true);
                    } else if (command == "savewithout") {
                        this.getPopup().hide();
                        this.notebook.saveNotebook(false);
                    } else if (command == "help") {
                        this.getPopup().hide();
                        var win = window.open(ramaddaBaseUrl + "/userguide/notebook.html", '_blank');
                        win.focus();
                    } else if (command == "delete") {
                        this.askDelete();
                    } else  {
                        console.log("unknown command:" + command);
                    }

         },
        checkHover: function(vis) {
            if(Utils.isDefined(this.lastHover)) {
                if(vis == this.lastHover) return;
            }
            this.lastHover = vis;
            var showingHeader = this.header.is(":visible");
            if (vis) {
                if(!showingHeader)
                    this.gutter.css("display", "block");
                this.cell.find(".display-notebook-gutter-container").css("background", "rgb(250,250,250)");
            } else {
                //                this.getPopup().hide();
                this.gutter.css("display", "none");
                this.cell.find(".display-notebook-gutter-container").css("background", "#fff");
            }
            if(showingHeader)
                this.gutter.css("display", "none");                
        },
        applyStyle: function() {
            this.checkHover(false);
            //Always hide the header. Its only shown when there is no output and no input
            this.header.css("display", "none");
            /*
            if (this.showHeader) { 
                this.header.css("display", "block");
            } else {
                this.header.css("display", "none");
            }
            */
            if (this.showInput && this.notebook.showInput()) {
                //                this.toggleButton.html(HtmlUtils.image(Utils.getIcon("togglearrowdown.gif")));
                this.jq(ID_INPUT_TOOLBAR).css("display", "block");
                this.inputContainer.show("show");
            } else {
                //                this.toggleButton.html(HtmlUtils.image(Utils.getIcon("togglearrowright.gif")));
                this.jq(ID_INPUT_TOOLBAR).css("display", "none");
                this.inputContainer.hide();
            }
            if(this.showOutput) {
                this.output.css("display", "block");
            } else {
                this.output.css("display", "none");
                if(!this.showInput) {
                    if(this.notebook.showInput()) {
                        this.header.css("background", "rgb(250,250,250)");
                        this.header.css("display", "block");
                    }
                }
            }

            if (this.showBorder) {
                this.cell.css("border-top", "1px #ccc solid");
            } else {
                this.cell.css("border-top", "1px #fff solid");
            }
        },
         getPopup: function() {
            return this.notebook.getPopup();
        },
        askDelete: function() {
            let _this = this;
            var menu = "";
            menu += "Are you sure you want to delete this cell?<br>";
            menu += HtmlUtils.div(["class", "ramadda-link", "what", "yes"], "Yes") + "<br>";
            menu += HtmlUtils.div(["class", "ramadda-link", "what", "cancel"], "No");
            var popup = this.getPopup();
            popup.html(HtmlUtils.div(["class", "ramadda-popup-inner"], menu));
            popup.show();
            var src = this.input;
            if(!src.is(":visible"))  {
                src  = this.output;
            }
            if(!src.is(":visible"))  {
                src = this.header;
            }
            popup.position({
                of: src,
                my: "left top",
                at: "left top",
                collision: "fit fit"
            });
            popup.find(".ramadda-link").click(function() {
                var what = $(this).attr("what");
                popup.hide();
                if (what == "yes") {
                    _this.notebook.deleteCell(_this);
                }
            });


        },
        run: async function(callback) {
            if(this.running) return Utils.call(callback, true);
            this.running = true;
            try {
                var ok = true;
                await this.runInner().then(r=>ok=r);
                if(!ok) {
                    this.running = false;
                    return Utils.call(callback,false);
                }
                this.outputUpdated();
            } catch(e) {
                this.running = false;
                this.writeOutput("An error occurred:" + e);
                console.log("error:" + e);
                console.log(e.stack);
                return Utils.call(callback, false);
            }
            this.running = false;
            return Utils.call(callback, true);
        },
        runInner: async function() {
            var value = this.input.val();
            value = value.trim();
            var help = "More information <a target=_help href='" + ramaddaBaseUrl + "/userguide/notebook.html'>here</a>";
            value = value.replace(/{cellname}/g, this.cellName);
            value = value.replace(/{help}/g, help);
            value = this.notebook.convertInput(value);
            var type = "wiki";
            var blobs = [];
            var blob = "";
            var commands = value.split("\n");
            var types = ["wiki", "html", "sh", "js","raw"];
            var ok = true;
            for (var i = 0; i < commands.length; i++) {
                if (!ok) break;
                value = commands[i].trim();
                if(value.startsWith("//")) continue;
                var isType = false;
                for (var typeIdx = 0; typeIdx < types.length; typeIdx++) {
                    var t = types[typeIdx];
                    if (value.startsWith(t + ":")) {
                        if(blob!="") {
                            blobs.push({blob:blob,type:type,div:new Div(),ok:true});
                        }
                        blob = value.substring((t + ":").length);
                        if (blob != "") blob += "\n";
                        type = t;
                        isType = true;
                        break;
                    }
                }
                if (isType) continue;
                blob = blob + value + "\n";
            }

            if(blob!="") {
                blobs.push({blob:blob,type:type,div:new Div(),ok:true});
            }

            
            var result = "";
            for(var i=0;i<blobs.length;i++) {
                result+=blobs[i].div.toString()+"\n";
            }
            this.output.html(result);
            this.rawOutput = "";
            for(var i=0;i<blobs.length;i++) {
                var blob = blobs[i];
                await this.processBlob(blob);
                if(!blob.ok) {
                    console.log("not ok");
                    return false;
                }
            }
            Utils.initContent("#"+this.getDomId(ID_OUTPUT));
            return ok;
        },
        writeOutput: function(h) {
                if(!this.output) {
                    err = new Error();
                    console.log("no output:" + err.stack);
                    return;
                }
                this.output.html(h);
                this.outputUpdated();
        },
        outputUpdated: function() {
            this.outputHtml = this.jq(ID_OUTPUT).html();
        },
        getRawOutput: function() {
            return  this.rawOutput;
        },
        focus: function() {
            this.input.focus();
        },
        clearOutput: function() {
            this.output.html("");
            this.outputHtml = "";
        },
        processHtml: async function(blob) {
            this.rawOutput+=blob.blob+"\n";
            blob.div.set(blob.blob);
        },
        processWiki: async function(blob) {
            this.rawOutput+=blob.blob+"\n";
            var id = this.notebook.getProperty("entryId", "");
            await this.getCurrentEntry(e=>entry=e);
            if (entry) id = entry.getId();
            let _this = this;
            let divId = HtmlUtils.getUniqueId();
            var wikiCallback = function(html) {
                var h = HtmlUtils.div(["id", divId,"style"], html);
                blob.div.set(h);
            }
            var wiki =  "{{group showMenu=false}}\n" + blob.blob;
            await GuiUtils.loadHtml(ramaddaBaseUrl + "/wikify?doImports=false&entryid=" + id + "&text=" + encodeURIComponent(blob.blob),
                             wikiCallback);
        },
        processSh: async function(blob) {
            var r = "";
            var lines = blob.blob.split("\n");
            var commands = [];
            for (var i = 0; i < lines.length; i++) {
                var fullLine = lines[i].trim();
                if (fullLine == "") continue;
                var cmds = fullLine.split(";");
                for(var cmdIdx=0;cmdIdx<cmds.length;cmdIdx++) {
                    var line = cmds[cmdIdx].trim();
                    if(line.startsWith("#") || line.startsWith("//")) continue;
                    var toks = line.split(" ");
                    var command = toks[0].trim();
                    var proc = null;
                    var extra = null;
                    if (this["processCommand_" + command]) {
                        proc = this["processCommand_" + command]; 
                    } else {
                        proc = this.processCommand_help;
                        extra =  "Unknown command: <i>" + command + "</i>";
                    }
                    var div = new Div();
                    //                    console.log("line:" + line);
                    commands.push({proc:proc,line:line,toks:toks,extra:extra,div:div});
                    r+=div.set("");
                }
            }
            let _this = this;
            blob.div.set(r);
            var i = 0;
            for(i=0;i<commands.length;i++) {
                var cmd = commands[i];
                await cmd.proc.call(_this,cmd.line, cmd.toks,cmd.div, cmd.extra);
            }
        },
         processJs: async function(blob) {
            try {
                await this.getCurrentEntry(e=>{current=e});
                var state = new NotebookState(this,blob.div);
                notebookStates[state.id] = state;
                var notebookEntries = this.notebook.getCurrentEntries();
                for(name in notebookEntries) {
                    state.entries[name] = notebookEntries[name].entry;
                }
                var jsSet = "";
                state.entries["current"] = current;
                state.entries["parent"] = this.parentEntry;
                state.entries["base"] = this.notebook.getBaseEntry();
                state.entries["root"] = this.notebook.getRootEntry();
                
                var stateJS = "notebookStates['" + state.id +"']";
                jsSet+= "state= " + stateJS+";\n";
                for(name in state.entries) {
                    var e = state.entries[name];
                    jsSet+= name +"= state.entries['" + name +"'];\n"
                }
                nbCell = this;
                nbNotebook = this.notebook;
                var js =  blob.blob.trim();
                var lines = js.split("\n");
                if(lines.length==1 && !js.startsWith("return ") && !js.startsWith("{")) {
                    js = "return " + js;
                }

                if(lines.length>1 ||js.startsWith("return ") || js.startsWith("{")) {
                    js = "async function nbFunc() {\n" + jsSet +"\n" + js + "\n}";
                } 
                eval(js);
                await nbFunc().then(r=>{state.result=r;});
                if (state.getStop()) {
                    blob.ok = false;
                }
                var html = "";
                if (Utils.stringDefined(state.prefix)) html +=state.prefix;
                if (state.result) {
                    html +=state.result;
                    this.rawOutput+=html+"\n";
                }
                blob.div.append(html);
            } catch (e) {
                blob.ok = false;
                var lines = blob.blob.split("\n");
                var line = lines[e.lineNumber - 2];
                blob.div.append("Error: " + e.message + "<br>" + HtmlUtils.span(["class", "display-notebook-error"], " &gt; " + line));
                console.log(e.stack);
            }
            notebookStates[state.id] = null;
        },
         processBlob: async function(blob) {
            if (blob.type == "html") {
                await  this.processHtml(blob);
            } else if (blob.type == "wiki") {
                await this.processWiki(blob);
            } else if (blob.type == "raw") {
                this.rawOutput+=blob.blob+"\n";
                return;
            } else if (blob.type == "js") {
                await this.processJs(blob);
            } else if (blob.type == "sh") {
                await this.processSh(blob);
            } else {
                blob.div.set("Unknown type:" + blob.type);
            }
        },


        calculateInputHeight: function() {
            this.content = this.input.val();
            if(!this.content) return;
            var lines = this.content.split("\n");
            if (lines.length != this.inputRows) {
                this.inputRows = lines.length;
                this.input.attr("rows", Math.max(1, this.inputRows));
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
       processCommand_help: function(line, toks, div, callback, prefix) {
            if(div==null) div = new Div();
            var help = "";
            if (prefix != null) help += prefix;
            help += "<pre>pwd, ls, cd</pre>";
            return div.set(help);
        },
        entries: {},

       selectEntry: function(entryId) {
                var cnt=1;
                var entries = this.notebook.getCurrentEntries();
                while(entries["entry" + cnt]) {
                    cnt++;
                }
                var id = prompt("Set an ID", "entry" + cnt);
                if(id == null || id.trim()=="") return;
                this.notebook.addEntry(id, entryId);
            },
       setId: function(entryId) {
                var caretPos = this.input.getCursorPosition();
                var t = this.input.val();
                this.input.val(t.substring(0, caretPos) + '"' +entryId+'"'+ t.substring(caretPos) );
       },
       cdEntry: function(entryId) {
                var div = new Div("");
                this.currentEntry = this.entries[entryId];
                notebookState.entries["current"] = this.currentEntry;
                this.output.html(div.toString());
                this.processCommand_pwd("pwd", [],div);
                this.outputUpdated();
         },
         addToToolbar: function(id, entry, toolbarItems) {
                var call = "getHandler('" + id +"').setId('" + entry.getId() +"')";
                var icon = HtmlUtils.image(ramaddaBaseUrl + "/icons/setid.png", ["border", 0, ATTR_TITLE, "Set ID in Input"]);
                toolbarItems.unshift(HtmlUtils.tag(TAG_A, ["onclick", call],icon));
                var call = "getHandler('" + id +"').selectEntry('" + entry.getId() +"')";
                var icon = HtmlUtils.image(ramaddaBaseUrl + "/icons/circle-check.png", ["border", 0, ATTR_TITLE, "Select Entry"]);
                toolbarItems.unshift(HtmlUtils.tag(TAG_A, ["onclick", call],icon));
         },
         getEntryPrefix: function(id, entry) {
               this.entries[entry.getId()] = entry;
                var call = "getHandler('" + id +"').cdEntry('" + entry.getId() +"')";
                return HtmlUtils.div(["style","padding-right:4px;", "title","cd to entry", "onclick", call,"class","ramadda-link"], HtmlUtils.image(ramaddaBaseUrl+"/icons/go.png"));
         },
        displayEntries: function(entries, div) {
            if(div==null) div = new Div();
            this.currentEntries = entries;
            if(entries == null || entries.length==0) {
                return div.msg("No children");
            }
            var handlerId = addHandler(this);
            var html = this.notebook.getEntriesTree(entries, {
                handlerId: handlerId,
                showIndex: false,
                suffix: "_shell_" + (this.uniqueCnt++)
            });
            div.set(HtmlUtils.div(["style", "max-height:200px;overflow-y:auto;"],html));
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
        setCurrentEntry: async function(entry) {
                this.currentEntry = entry;
                this.parentEntry = null;
                if(this.currentEntry)
                    await this.currentEntry.getParentEntry(entry=> {this.parentEntry=entry;});
        },
        getCurrentEntry: async function(callback) {
            if (this.currentEntry == null) {
                await this.setCurrentEntry(this.notebook.getBaseEntry());
            }
            if (this.currentEntry == null) {
                if (Utils.isDefined(dflt)) return dflt;
                this.rootEntry = new Entry({
                    id: ramaddaBaseEntry,
                    name: "Root",
                    type: "group"
                });
                this.currentEntry = this.rootEntry;
            }
            return Utils.call(callback, this.currentEntry);
        },
        createDisplay: async function(state, entry, displayType, displayProps) {
            if(!entry) await this.getCurrentEntry(e=>entry=e);
            if((typeof entry) =="string") {
                await this.notebook.getEntry(entry,e=>entry=e);
            }    

            if(!state.displayManager) {
                var divId  = HtmlUtils.getUniqueId();
                state.div.append(HtmlUtils.div(["id", divId],""));
                state.displayManager =  new DisplayManager(divId, {"showMap":false,
                                                                   "showMenu":false,
                                                                   "showTitle":false,
                                                                   "layoutType":"table",
                                                                   "layoutColumns":1,
                                                                   "defaultMapLayer":"osm",
                                                                   "entryId":""});
            }

            var divId  = HtmlUtils.getUniqueId();
            state.div.append(HtmlUtils.div(["id", divId],"DIV"));
            var props = {
                layoutHere:true,
                divid:divId,
                showMenu: true,
                sourceEntry: entry,
                entryId: entry.getId(),
                showTitle: true,
                showDetails: true,
                title: entry.getName(),
            };
            
            if(displayProps) {
                $.extend(props, displayProps);
            }
            if (!props.data && displayType != DISPLAY_ENTRYLIST) {
                var jsonUrl = this.notebook.getPointUrl(entry);
                if (jsonUrl == null) {
                    this.writeError("Not a point type:" + entry.getName());
                    return;
                }
                if (jsonUrl == null) {
                    jsonUrl = this.getPointUrl(entry);
                }
                var pointDataProps = {
                    entry: entry,
                    entryId: entry.getId()
                };
                props.data = new PointData(entry.getName(), null, null, jsonUrl, pointDataProps);
            }
            state.displayManager.createDisplay(displayType, props);
        },
        createPointDisplay: async function(toks, displayType) {
            await this.getCurrentEntry(e=>current=e);
            var entry = this.getEntryFromArgs(toks, currentEntry);
            var jsonUrl = this.notebook.getPointUrl(entry);
            if (jsonUrl == null) {
                this.writeError("Not a point type:" + entry.getName());
                return;
            }
            this.notebook.createDisplay(entry.getId(), displayType, jsonUrl);
        },
        processCommand_table: function(line, toks) {
            this.createPointDisplay(toks, DISPLAY_TABLE);
        },
        processCommand_linechart: function(line, toks) {
            this.createPointDisplay(toks, DISPLAY_LINECHART);
        },

        processCommand_barchart: function(line, toks) {
            this.createPointDisplay(toks, DISPLAY_BARCHART);
        },
        processCommand_bartable: function(line, toks) {
            this.createPointDisplay(toks, DISPLAY_BARTABLE);
        },
        processCommand_hello: function(line, toks) {
            this.writeResult("Hello, how are you?");
        },
        processCommand_scatterplot: function(line, toks) {
            this.createPointDisplay(toks, DISPLAY_SCATTERPLOT)
        },
        processCommand_blog: function(line, toks) {
            this.getLayoutManager().publish('blogentry');
        },
          getEntryHeading: function(entry, div) {
            var entries = [entry];
            var handlerId = addHandler(this);
            var html = this.notebook.getEntriesTree(entries, {
                handlerId: handlerId,
                showIndex: false,
                suffix: "_shell_" + (this.uniqueCnt++)
            });
            div.set(HtmlUtils.div(["style", "max-height:200px;overflow-y:auto;"],html));
            return div;
            //            var icon = entry.getIconImage([ATTR_TITLE, "View entry"]);
            //            return "&gt; "+ icon +" " +entry.getName();
          },
         processCommand_pwd: async function(line, toks,div) {
            if(div==null) div = new Div();
            await this.getCurrentEntry(e=>entry=e);
            return this.getEntryHeading(entry, div);
        },
        processCommand_clearEntries: function(line, toks, div) {
             this.notebook.clearEntries();
             div.set("Entries cleared");
        },
        processCommand_printEntries: async function(line, toks, div) {
                var h = "";
                await this.getCurrentEntry(e=>current=e);
                h += "current" +"=" + current.getName()+"<br>";
                var entries = this.notebook.getCurrentEntries();
                for(var name in entries) {
                    var e = entries[name];
                    h += name +"=" + e.entry.getName()+"<br>";
                }
                if(h=="") h="No entries";
                div.set(h);
            },
        processCommand_echo: async function(line, toks,div) {
             line = line.replace(/^echo */,"");
             div.set(line);
        },
        processCommand_print: async function(line, toks,div) {
             line = line.replace(/^print */,"");
             div.set(line);
        },

        processCommand_info: async function(line, toks,div) {
            await this.getCurrentEntry(e=>entry=e);
            div.append("current:" + entry.getName() +" id:" + entry.getId()+"<br>");
        },
        processCommand_cd: async function(line, toks,div) {
            if(div==null) div = new Div();
            if (toks.length <= 1) {
                await this.setCurrentEntry(this.notebook.getBaseEntry());
                return;
                //                return this.getEntryHeading(this.currentEntry, div);
            }
            var arg =Utils.join(toks," ",1).trim();
            if(arg.startsWith("/")) {
                await this.getCurrentEntry(e=>entry=e);
                var root;
                await entry.getRoot(e=>{root = e});
                this.setCurrentEntry(root);
            }
            var dirs = arg.split("/");
            await this.getCurrentEntry(e=>entry=e);
            for(var i=0;i<dirs.length;i++) {
                var dir = dirs[i];
                if(dir=="") continue;
                if(dir == "..") {
                    if(!this.parentEntry) {
                        div.msg("No parent entry");
                        break;
                    }
                    await this.setCurrentEntry(this.parentEntry);
                } else  {
                    await this.currentEntry.getChildrenEntries(c=>children=c);
                    var child = null;
                    var startsWith = false;
                    var endsWith = false;
                    if(dir.endsWith("*")) {
                        dir = dir.substring(0,dir.length-1);
                        startsWith=true;
                    }
                    if(dir.startsWith("*")) {
                        dir = dir.substring(1);
                        endsWith=true;
                    }
                    for(var childIdx=0;childIdx<children.length;childIdx++) {
                        var name = children[childIdx].getName();
                        if(startsWith && endsWith) {
                            if(name.includes(dir)) {
                                child = children[childIdx];
                                break;
                            }
                        } else  if(startsWith) {
                            if(name.startsWith(dir)) {
                                child = children[childIdx];
                                break;
                            }
                        } else  if(endsWith) {
                            if(name.endsWith(dir)) {
                                child = children[childIdx];
                                break;
                            }
                        }
                        if(children[childIdx].getName() == dir) {
                            child = children[childIdx];
                            break;
                        }
                    }
                    if(!child) {
                        div.msg("No entry:" + dir);
                        break;
                    }
                    await this.setCurrentEntry(child);
                }
            }
        },
         processCommand_ls: async function(line, toks,div) {
            if(div==null) div = new Div();
            div.set("Listing entries...");
            await this.getCurrentEntry(e=>entry=e);
            await entry.getChildrenEntries(children=>{this.displayEntries(children, div)}, "");
        },
        entryListChanged: function(entryList) {
            var entries = entryList.getEntries();
            if (entries.length == 0) {
                this.writeStatusMessage("Sorry, nothing found");
            } else {
                this.displayEntries(entries);
            }
        },
          processCommand_search: function(line, toks, div) {
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
        processCommand_clear: function(line,toks,div) {
            this.clearOutput();
        },
        processCommand_save: function(line,toks,div) {
             this.notebook.saveNotebook();
        },

    });

}
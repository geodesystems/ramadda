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
    var SUPER;
    var ID_CELLS = "cells";
    var ID_CELL = "cell";
    RamaddaUtil.inherit(this, SUPER = new RamaddaDisplay(displayManager, id, DISPLAY_NOTEBOOK, properties));
    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
        cells: [],
        cellId: 0,
        fetchedNotebook: false, 
        currentEntries:{},
        baseEntries:{},
        initDisplay: async function() {
            let _this = this;
            this.createUI();
            this.setContents(HtmlUtils.div([ATTR_CLASS, "display-notebook-cells", ATTR_ID, this.getDomId(ID_CELLS)], ""));
            if (!this.fetchedNotebook) {
                this.fetchedNotebook = true;
                await this.getEntry(this.getProperty("entryId",""),entry=> {
                        this.currentEntry=entry;});
                this.baseEntries["base"] = {
                    entry:this.currentEntry,
                    entryId:this.currentEntry.getId(),
                }
                var parent;
                await this.currentEntry.getParentEntry(entry=> {parent=entry;});
                if(parent) {
                    this.baseEntries["parent"] = {
                        entry:parent,
                        entryId:parent.getId(),
                    }
                    var root;
                    await this.currentEntry.getRoot(entry=> {root=entry;});
                    if(root) {
                        this.baseEntries["root"] = {
                            entry:root,
                            entryId:root.getId(),
                        }
                    }
                }
                for(a in this.baseEntries)
                    this.currentEntries[a] = this.baseEntries[a];

                
                var id = this.getProperty("entryId", "");
                var url = ramaddaBaseUrl + "/getnotebook?entryid=" + id;
                url += "&id=" + this.getProperty("notebookId", "default_notebook");
                var jqxhr = $.getJSON(url,  function(data) {
                        _this.loadJson(data);
                    });

            }
            if (this.cells.length > 0) {
                this.layoutCells();
            } else {
                var props = {
                    showEdit: true,
                }
                this.addCell("", props,false).run();
            }
            this.cells[0].focus();
        },
        loadJson: async function(data) {
                if (data.error) {
                    this.setContents(_this.getMessage("Failed to load notebook: " + data.error));
                    return;
                }
                if (Utils.isDefined(data.currentEntries)) {
                    for(a in data.currentEntries) {
                        var obj = {};
                        if(this.currentEntries[a]) continue;
                        obj.name=a;
                        obj.entryId = data.currentEntries[a].entryId;
                        await this.getEntry(obj.entryId,e=>obj.entry=e);
                        this.currentEntries[a] = obj;
                    }
                }
                if (Utils.isDefined(data.cells)) {
                    this.cells = [];
                    for (var i = 0; i < data.cells.length; i++) {
                        var props = data.cells[i];
                        this.addCell(props.outputHtml, props, true);
                    }
                    this.layoutCells();
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
        getJson: function(output) {
            var obj = {
                cells: [],
                currentEntries:{}
            };
            for(var name in this.currentEntries) {
                var e = this.currentEntries[name];
                obj.currentEntries[name] = {entryId:e.entryId};
            }
            for (var i = 0; i < this.cells.length; i++) {
                var cell = this.cells[i];
                obj.cells.push(cell.getJson(output));
            }
            return obj;
        },
        layoutCells: function() {
            this.jq(ID_CELLS).html("");
            for (var i = 0; i < this.cells.length; i++) {
                var cell = this.cells[i];
                this.jq(ID_CELLS).append(HtmlUtils.div([ATTR_CLASS, "display-notebook-cell", ATTR_ID, cell.id], ""));
                cell.createCell();
            }
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
                    showEdit:true
                };
            var cellId = this.getId() + "_" + this.cellId;
            this.cellId++;
            this.jq(ID_CELLS).append(HtmlUtils.div([ATTR_CLASS, "display-notebook-cell", ATTR_ID, cellId], ""));
            var cell = new RamaddaNotebookCell(this, cellId, content, props);
            return cell;
        },
        clearOutput: function() {
            for (var i = 0; i < this.cells.length; i++) {
                var cell = this.cells[i];
                cell.clearOutput();
            }
        },

        moveCellUp: function(cell) {
            var cells = [];
            var newCell = null;
            var idx = 0;
            for (var i = 0; i < this.cells.length; i++) {
                if (cell.id == this.cells[i].id) {
                    idx = i;
                    break;
                }
            }
            if (idx == 0) return;
            this.cells.splice(idx, 1);
            this.cells.splice(idx - 1, 0, cell);
            this.layoutCells();
            cell.focus();
        },
        moveCellDown: function(cell) {
            var cells = [];
            var newCell = null;
            var idx = 0;
            for (var i = 0; i < this.cells.length; i++) {
                if (cell.id == this.cells[i].id) {
                    idx = i;
                    break;
                }
            }
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
                        showEdit: true,
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
                        showEdit: true,
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
            for (var i = 0; i < this.cells.length; i++) {
                if (cell.id != this.cells[i].id) {
                    cells.push(this.cells[i]);
                }
            }
            this.cells = cells;
            if (this.cells.length == 0) {
                this.addCell("",null);
            }
        },
        runAll: function() {
            for (var i = 0; i < this.cells.length; i++) {
                var cell = this.cells[i];
                if (!cell.run()) {
                    break;
                }
            }

        },
        toggleAll: function(on) {
            for (var i = 0; i < this.cells.length; i++) {
                var cell = this.cells[i];
                cell.showEdit = on;
                cell.applyStyle();
            }
        },

    });
}


var nbCell;
var nbNotebook;

function nbGetCell() {
    return notebookState.cell;
}

function nbGetNotebook() {
    return notebookState.cell.notebook;
}

function nbClear() {
    notebookState.cell.notebook.clearOutput();
}

function nbSave(output) {
    nbGetNotebook().saveNotebook(output);
    return "notebook saved";
}

function nbClearEntries() {
    nbGetNotebook().clearEntries();
}

function nbLs(entry) {
    var div = new Div();
    if(!entry)
        entry = nbCell.getCurrentEntry(null);
    nbCell.getEntryHeading(entry, div);
    nbWrite(div.toString());
}

function nbLsEntries() {
    var h = "";
    var entries = notebookState.currentEntries;
    for(var name in entries) {
        var e = entries[name];
        h += name +"=" + e.entry.getName()+"<br>";
    }
    nbWrite(h);
}

function nbStop() {
    notebookState.stop = true;
}


function nbSetEntry(name,entryId) {
    nbNotebook.addEntry(name,entryId);
}

function nbWrite(s) {
    if(notebookState.prefix==null) {
        notebookState.prefix = s;
    } else {
        notebookState.prefix += "<br>";
        notebookState.prefix += s;
    }
}

function nbLinechart(entry) {
    nbCell.createDisplay(entry,DISPLAY_LINECHART);
}

function nbHelp() {
    return "Enter an expression, e.g.:<pre>1+2</pre> or wrap you code in brackets:<pre>{\nvar x=1+2;\nreturn x;\n}</pre>" +
        "functions:<br>nbHelp() : print this help<br>nbClear() : clear the output<br>nbCell : this cell<br>nbNotebook: this notebook<br>" +
        "nbLs(): list current entry; nbLs(parent): list parent; nbLs(root): list root" +
        "nbLinechart(entry): create a linechart, defaults to current entry" +
        "nbNotebook.addCell('wiki:some wiki text'): add a new cell with the given output<br>" +
        "nbSave(includeOutput): save the notebook<br>nbWrite(html) : add some output<br>nbStop(): stop running";
}


var notebookState = {
    entries:{},
    cell: null,
    stop: false,
    prefix: null,
    notebookResult: null,

}


function RamaddaNotebookCell(notebook, id, content, props) {
    this.notebook = notebook;

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
    var ID_CELLNAME_INPUT = "cellnameinput";
    var ID_SHOWHEADER_INPUT = "showheader";
    var ID_SHOWBORDER_INPUT = "showborder";
    var ID_SHOWEDIT_INPUT = "showedit";
    var ID_PINNED_INPUT = "pinned";
    let SUPER = new DisplayThing(id, {});
    RamaddaUtil.inherit(this, SUPER);

    RamaddaUtil.defineMembers(this, {
        id: id,
        inputRows: 1,
        content: content,
        outputHtml: "",
        pinned: false,
        showEdit: false,
        showHeader: false,
        showBorder: false,
        cellName: "",
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
                pinned: this.pinned,
                showEdit: this.showEdit,
                showHeader: this.showHeader,
                showBorder: this.showBorder,
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
                    this.content = "html:";
                }
            var _this = this;
            var header = HtmlUtils.div([ATTR_CLASS, "display-notebook-header", ATTR_ID, this.getDomId(ID_HEADER)], this.cellName);
            var input = HtmlUtils.textarea(TAG_INPUT, this.content, ["rows", this.inputRows, ATTR_CLASS, "display-notebook-input", ATTR_ID, this.getDomId(ID_INPUT)]);

            input = HtmlUtils.div(["class", "display-notebook-input-container"], input);
            var output = HtmlUtils.div([ATTR_CLASS, "display-notebook-output", ATTR_ID, this.getDomId(ID_OUTPUT)], this.outputHtml);
            output = HtmlUtils.div(["class", "display-notebook-output-container"], output);
            var menu = HtmlUtils.div(["id", this.getDomId(ID_MENU), "class", "ramadda-popup"], "");
            var gutter = HtmlUtils.div(["class", "display-notebook-gutter", ATTR_ID, this.getDomId(ID_INPUT_GUTTER)],
                menu +
                HtmlUtils.div(["class", "display-notebook-menu-button", "id", this.getDomId(ID_MENU_BUTTON)], HtmlUtils.image(icon_menu, [])));

            var table = HtmlUtils.openTag("table", ["cellspacing", "0", "cellpadding", "0", "width", "100%", "border", "0"]);
            table += HtmlUtils.tr(["valign", "top"],
                HtmlUtils.td(["rowspan", "3", "width", "5", "class", "display-notebook-gutter-container"], gutter) +
                HtmlUtils.td([], header));
            table += HtmlUtils.tr(["valign", "top"], "\n" + HtmlUtils.td([], input));
            table += HtmlUtils.tr(["valign", "top"], "\n" + HtmlUtils.td([], output));
            table += "</table>";

            var html = table;
            html = HtmlUtils.div(["id", this.getDomId(ID_CELL)], html);
            $("#" + this.id).html(html);
            this.header = this.jq(ID_HEADER);
            this.menuButton = this.jq(ID_MENU_BUTTON);
            this.cell = this.jq(ID_CELL);
            this.input = this.jq(ID_INPUT);
            this.output = this.jq(ID_OUTPUT);
            this.inputContainer = this.cell.find(".display-notebook-input-container");
            this.applyStyle();
            this.menuButton.click(()=>this.makeMenu());
            var hoverIn = function() {
                _this.checkMenuButton(true);
            }
            var hoverOut = function() {
                _this.jq(ID_MENU).hide();
                _this.checkMenuButton(false);
            }
            this.cell.hover(hoverIn, hoverOut);
            this.calculateInputHeight();
            this.input.on('input selectionchange propertychange', function() {
                _this.calculateInputHeight();
            });
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
        makeMenu: function() {
                let _this = this;
                var space = "&nbsp;&nbsp;";
                var line =  "<tr><td colspan=2 style='border-top:1px #ccc solid;'</td></tr>";
                var menu = "";
                menu += HtmlUtils.input(ID_CELLNAME_INPUT, _this.cellName, ["placeholder", "Cell name", "style", "width:100%;", "xsize", "20", "id", _this.getDomId(ID_CELLNAME_INPUT)]);
                menu += "<br>";
                menu += "<table>";

                menu += "<tr><td align=right><b>Run:</b>&nbsp;</td><td>";
                menu += HtmlUtils.div(["title", "shift-return", "class", "ramadda-link", "what", "run"], "This cell") + space;
                menu += HtmlUtils.div(["title", "ctrl-return", "class", "ramadda-link", "what", "runall"], "All");
                menu += "</td></tr>"

                menu += "<tr><td align=right><b>New Cell:</b>&nbsp;</td><td>";
                menu += HtmlUtils.div(["class", "ramadda-link", "what", "newabove"], "Above") + space;
                menu += HtmlUtils.div(["class", "ramadda-link", "what", "newbelow"], "Below");
                menu += "</td></tr>"

                menu += "<tr><td align=right><b>Clear:</b>&nbsp;</td><td>";
                menu += HtmlUtils.div(["class", "ramadda-link", "what", "clear"], "This cell") + space;
                menu += HtmlUtils.div(["class", "ramadda-link", "what", "clearall"], "All");
                menu += "</td></tr>"

                menu += "<tr><td align=right><b>Move:</b>&nbsp;</td><td>";
                menu += HtmlUtils.div(["title", "ctrl-^", "class", "ramadda-link", "what", "moveup"], "Up") + space;
                menu += HtmlUtils.div(["title", "ctrl-v", "class", "ramadda-link", "what", "movedown"], "Down");
                menu += "</td></tr>"

                menu += line;

                menu += "<tr valign=top><td align=right><b>Show:</b>&nbsp;</td><td>";
                menu += HtmlUtils.checkbox(_this.getDomId(ID_SHOWEDIT_INPUT), [],
                    _this.showEdit) + " Edit";
                menu += space;
                menu += HtmlUtils.checkbox(_this.getDomId(ID_SHOWHEADER_INPUT), [],
                    _this.showHeader) + " Header";
                menu += "<br>";
                menu += HtmlUtils.checkbox(_this.getDomId(ID_SHOWBORDER_INPUT), [],
                    _this.showBorder) + " Border";

                menu += space;
                menu += HtmlUtils.checkbox(_this.getDomId(ID_PINNED_INPUT), [],
                    _this.pinned) + " Left side";

                menu += "</td></tr>"

                menu += "<tr><td align=right><b>Edit:</b>&nbsp;</td><td>";
                menu += HtmlUtils.div(["title", "ctrl-return", "class", "ramadda-link", "what", "showall"], "Show All");
                menu += space;
                menu += HtmlUtils.div(["title", "ctrl-return", "class", "ramadda-link", "what", "hideall"], "Hide All");
                menu += "</td></tr>";

                menu += line;


                menu += "</table>";
                menu += HtmlUtils.div(["class", "ramadda-link", "what", "savewithout"], "Save Notebook (w/o output)") + "<br>";
                menu += HtmlUtils.div(["class", "ramadda-link", "what", "savewith"], "Save Notebook (with output)") + "<br>";
                menu += HtmlUtils.div(["class", "ramadda-link", "what", "delete"], "Delete") + "<br>";
                menu += HtmlUtils.div(["class", "ramadda-link", "what", "help"], "Help") + "<br>";
                var popup = _this.jq(ID_MENU);
                popup.html(HtmlUtils.div(["class", "ramadda-popup-inner"], menu));
                popup.show();
                popup.position({
                    of: _this.menuButton,
                    my: "left top",
                    at: "right top",
                    collision: "fit fit"
                });
                _this.jq(ID_SHOWHEADER_INPUT).focus();
                _this.jq(ID_SHOWHEADER_INPUT).change(function(e) {
                    _this.showHeader = _this.jq(ID_SHOWHEADER_INPUT).is(':checked');
                    _this.applyStyle();
                });
                _this.jq(ID_SHOWBORDER_INPUT).change(function(e) {
                    _this.showBorder = _this.jq(ID_SHOWBORDER_INPUT).is(':checked');
                    _this.applyStyle();
                });
                _this.jq(ID_SHOWEDIT_INPUT).change(function(e) {
                    _this.showEdit = _this.jq(ID_SHOWEDIT_INPUT).is(':checked');
                    _this.applyStyle();
                });
                _this.jq(ID_PINNED_INPUT).change(function(e) {
                    _this.pinned = _this.jq(ID_PINNED_INPUT).is(':checked');
                    _this.checkMenuButton(false);
                });

                _this.jq(ID_CELLNAME_INPUT).keypress(function(e) {
                    var keyCode = e.keyCode || e.which;
                    if (keyCode == 13) {
                        _this.cellName = $(this).val();
                        _this.applyStyle();
                        popup.hide();
                        return;
                    }
                });
                popup.find(".ramadda-link").click(function() {
                    var what = $(this).attr("what");
                    popup.hide();
                    if (what == "run") {
                        _this.run();
                    } else if (what == "showthis") {
                        _this.showEdit = true;
                        _this.applyStyle();
                    } else if (what == "hidethis") {
                        _this.showEdit = false;
                        _this.applyStyle();
                    } else if (what == "showall") {
                        _this.notebook.toggleAll(true);
                    } else if (what == "hideall") {
                        _this.notebook.toggleAll(false);
                    } else if (what == "runall") {
                        _this.notebook.runAll();
                    } else if (what == "clear") {
                        _this.clearOutput();
                    } else if (what == "clearall") {
                        _this.notebook.clearOutput();
                    } else if (what == "moveup") {
                        _this.notebook.moveCellUp(_this);
                    } else if (what == "movedown") {
                        _this.notebook.moveCellDown(_this);
                    } else if (what == "newabove") {
                        _this.notebook.newCellAbove(_this);
                    } else if (what == "newbelow") {
                        _this.notebook.newCellBelow(_this);
                    } else if (what == "savewith") {
                        _this.notebook.saveNotebook(true);
                    } else if (what == "savewithout") {
                        _this.notebook.saveNotebook(false);
                    } else if (what == "help") {
                        var win = window.open(ramaddaBaseUrl + "/userguide/wikidisplay.html#notebook", '_blank');
                        win.focus();
                    } else if (what == "delete") {
                        _this.askDelete();
                    }
                });
        },
        checkMenuButton: function(vis) {
            if (vis || this.pinned) {
                this.menuButton.css("display", "block");
                this.cell.find(".display-notebook-gutter-container").css("background", "rgb(250,250,250)");
                this.cell.find(".display-notebook-gutter-container").css("border-right", "1px #ccc solid");
            } else {
                this.menuButton.css("display", "none");
                this.cell.find(".display-notebook-gutter-container").css("background", "#fff");
                this.cell.find(".display-notebook-gutter-container").css("border-right", "1px #fff solid");
            }
        },
        applyStyle: function() {
            this.checkMenuButton(false);
            if (this.showHeader) {
                this.header.css("display", "block");
            } else {
                this.header.css("display", "none");
            }
            this.header.html(this.cellName);
            if (this.showEdit) {
                this.inputContainer.css("display", "block");
            } else {
                this.inputContainer.css("display", "none");
            }
            if (this.showBorder) {
                this.cell.css("border-top", "1px #ccc solid");
                //                    this.cell.css("border-bottom","1px #ccc solid");
            } else {
                this.cell.css("border-top", "1px #fff solid");
                //                    this.cell.css("border-bottom","1px #fff solid");
            }
        },
        askDelete: function() {
            let _this = this;
            var menu = "";
            menu += "You sure you want to delete this cell?<br>";
            menu += HtmlUtils.div(["class", "ramadda-link", "what", "yes"], "Yes") + "<br>";
            menu += HtmlUtils.div(["class", "ramadda-link", "what", "cancel"], "No");
            var popup = _this.jq(ID_MENU);
            popup.html(HtmlUtils.div(["class", "ramadda-popup-inner"], menu));
            popup.show();
            popup.position({
                of: this.menuButton,
                my: "left top",
                at: "right top",
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
        run: function() {
                if(this.running) return "already running";
                this.running = true;
                try {
                    this.runInner();
                } catch(e) {
                    this.writeOutput("An error occurred:" + e);
                    console.log(e.stack);
                }
                this.running = false;
            },
            runInner: async function() {
            var value = this.input.val();
            value = value.trim();
            var help = "More information <a target=_help href='" + ramaddaBaseUrl + "/userguide/wikidisplay.html#notebook'>here</a>";
            value = value.replace(/{cellname}/g, this.cellName);
            value = value.replace(/{help}/g, help);
            var result = "";
            var type = "html";
            var blob = "";
            var commands = value.split("\n");
            var types = ["html", "wiki", "sh", "js"];
            var ok = true;
            for (var i = 0; i < commands.length; i++) {
                if (!ok) break;
                value = commands[i].trim();
                var isType = false;
                for (var typeIdx = 0; typeIdx < types.length; typeIdx++) {
                    var t = types[typeIdx];
                    if (value.startsWith(t + ":")) {
                        var r = {
                            ok: true
                        }
                        var blobResult;
                        await this.processBlob(type, blob, r,result=>blobResult=result);
                        if (blobResult) {
                            if (blobResult != "") {
                                blobResult += "<br>";
                            }
                            result += blobResult;
                        }
                        ok = r.ok;
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
            if (ok) {
                var r = {
                    ok: true
                }
                var blobResult;
                await this.processBlob(type, blob, r,result=>{blobResult=result});
                if (blobResult) {
                    if (blobResult != "") {
                        blobResult += "<br>";
                    }
                    result += blobResult;
                }
                ok = r.ok;
            }
            this.outputHtml = result;
            this.output.html(result);
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
        focus: function() {
            this.input.focus();
        },
        clearOutput: function() {
            this.output.html("");
            this.outputHtml = "";
        },
        processHtml: function(blob, result) {
            blob = blob.trim();
            blob = blob.replace(/\n/g, "<br>");
            return blob;
        },
         processWiki: async function(blob, result, callback) {
            var id = this.notebook.getProperty("entryId", "");
            var entry = this.getCurrentEntry(null);
            if (entry) id = entry.getId();
            let _this = this;
            let divId = HtmlUtils.getUniqueId();
            var wikiCallback = function(html) {
                $("#" + divId).html(html);
                Utils.initContent("#"+_this.getDomId(ID_OUTPUT));
                _this.outputUpdated();
            }
            blob = "{{group showMenu=false}}\n" + blob;
            GuiUtils.loadHtml(ramaddaBaseUrl + "/wikify?doImports=false&entryid=" + id + "&text=" + encodeURIComponent(blob),
                              wikiCallback);
            var h = HtmlUtils.div(["id", divId], "Processing...");
            return Utils.call(callback, h);
        },
        processSh: function(blob, result) {
            var r = "";
            var lines = blob.split("\n");
            var commands = [];
            for (var i = 0; i < lines.length; i++) {
                var fullLine = lines[i].trim();
                if (fullLine == "") continue;
                var cmds = fullLine.split(";");
                for(var cmdIdx=0;cmdIdx<cmds.length;cmdIdx++) {
                    var line = cmds[cmdIdx].trim();
                    if(line.startsWith("#")) continue;
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
            var process = async function() {
                var i = 0;
                for(i=0;i<commands.length;i++) {
                    var cmd = commands[i];
                    //                    console.log("calling:" + cmd.line);
                    await cmd.proc.call(_this,cmd.line, cmd.toks,cmd.div, cmd.extra);
                    //                    console.log("done");
                }
            }
            setTimeout(process,1);
            return r;
        },
         processJs: async function(blob, result,callback) {
            try {
                var current = this.getCurrentEntry();
                var entries = this.notebook.getCurrentEntries();
                notebookState.entries = {};
                var jsSet = "";
                notebookState.entries["current"] = current;
                name = "current";
                jsSet+= name +"= notebookState.entries['" + name +"'];\n"
                for(var name in entries) {
                    var e = entries[name];
                    notebookState.entries[name] = e.entry;
                    jsSet+= name +"= notebookState.entries['" + name +"'];\n"
                }
                nbCell = this;
                nbNotebook = this.notebook;
                notebookState.stop = false;
                notebookState.cell = this;
                notebookState.notebookResult = null;
                notebookState.prefix = null;
                blob = blob.trim();
                var lines = blob.split("\n");
                var js;
                if(lines.length==1 && !blob.startsWith("return ") && !blob.startsWith("{")) {
                    blob = "return " + blob;
                }

                if(lines.length>1 || blob.startsWith("return ") || blob.startsWith("{")) {
                     js = "async function nbFunc() {\n" + jsSet +"\n" + blob + "\n}\nasync function tmp() {await nbFunc().then(r=>{console.log('then:' + r);notebookState.notebookResult=result;});};";
                } 
                var jsReturn;
                eval(js);
                await nbFunc().then(function(r) {jsReturn=r;});
                if (notebookState.stop) {
                    result.ok = false;
                }

                var html = "";
                if (notebookState.prefix) html +=notebookState.prefix+ "<br>";
                if (jsReturn) html +=jsReturn;
                return Utils.call(callback, html);
            } catch (e) {
                result.ok = false;
                var lines = blob.split("\n");
                var line = lines[e.lineNumber - 2];
                return Utils.call(callback, "Error: " + e.message + "<br>" + HtmlUtils.span(["class", "display-notebook-error"], " &gt; " + line));
            }
        },

           processBlob: async function(type, blob, result, callback) {
            if (blob == "") {
                return Utils.call(callback,"");
            }
            if (type == "html") {
                return Utils.call(callback, this.processHtml(blob, result));
            } else if (type == "wiki") {
                await this.processWiki(blob, result,callback);
                return;
            } else if (type == "js") {
                await this.processJs(blob, result,callback);
                return;
            } else {
                return Utils.call(callback, this.processSh(blob, result));
            }
            return Utils.call(callback, "unknown type:" + type+" blob:"  + blob);
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
            Utils.later(callback);
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
        setCurrentEntry: function(entry) {
                this.currentEntry = entry;
        },
        getCurrentEntry: function(dflt) {
            if (this.currentEntry == null) {
                this.setCurrentEntry(this.notebook.currentEntry);
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
            return this.currentEntry;
        },
        createDisplay: async function(entry, displayType) {
            if(!entry) entry = this.getCurrentEntry();
            if((typeof entry) =="string") {
                await this.notebook.getEntry(entry,e=> {entry=e});
            }    
            var jsonUrl = this.notebook.getPointUrl(entry);
            if (jsonUrl == null) {
                this.writeError("Not a point type:" + entry.getName());
                return;
            }
            this.notebook.createDisplay(entry.getId(), displayType, jsonUrl);
        },
        createPointDisplay: function(toks, displayType) {
            var entry = this.getEntryFromArgs(toks, this.getCurrentEntry());
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
         processCommand_pwd: function(line, toks,div) {
            if(div==null) div = new Div();
            var entry = this.getCurrentEntry();
            return this.getEntryHeading(entry, div);
        },
        processCommand_clearEntries: function(line, toks, div) {
             this.notebook.clearEntries();
             div.set("Entries cleared");
        },
        processCommand_printEntries: function(line, toks, div) {
                var h = "";
                var entries = this.notebook.getCurrentEntries();
                for(var name in entries) {
                    var e = entries[name];
                    h += name +"=" + e.entry.getName()+"<br>";
                }
                if(h=="") h="No entries";
                div.set(h);
            },
        processCommand_cd: async function(line, toks,div) {
            if(div==null) div = new Div();
            if (toks.length <= 1) {
                this.setCurrentEntry(this.notebook.currentEntry);
                return this.getEntryHeading(this.currentEntry, div);
            }
            if(toks[1].startsWith("/")) {
                //                toks = toks[1].split("/");
                //                console.log(toks);
                var entry = this.getCurrentEntry();
                var root;
                await entry.getRoot(e=>{root = e});

                this.setCurrentEntry(root);
                this.getEntryHeading(root, div);
                return;

            }
            if(toks[1].startsWith("..")) {
                var entry = this.getCurrentEntry();
                div.set("Retrieving entry...");
                await entry.getParentEntry(entry=>{
                        if(entry) { 
                            this.setCurrentEntry(entry);
                            this.getEntryHeading(entry, div);
                        } else {
                            div.msg("No parent entry");
                        }});
                return;
            }
            return div.set("NA");
        },
         processCommand_ls: async function(line, toks,div) {
            if(div==null) div = new Div();
            div.set("Listing entries...");
            await this.getCurrentEntry().getChildrenEntries(children=>{this.displayEntries(children, div)}, "");
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
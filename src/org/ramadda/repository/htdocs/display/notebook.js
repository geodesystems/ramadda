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
        initDisplay: function() {
            let _this = this;
            this.createUI();

            var cells = HtmlUtils.div([ATTR_CLASS, "display-notebook-cells", ATTR_ID, this.getDomId(ID_CELLS)], "");
            var html = cells;
            this.setContents(html);
            if (!this.fetchedNotebook) {
                this.currentEntry = this.getEntry(this.getProperty("entryId",""),entry=>{_this.currentEntry=entry});
                this.fetchedNotebook = true;
                var id = this.getProperty("entryId", "");
                var url = ramaddaBaseUrl + "/getnotebook?entryid=" + id;
                url += "&id=" + this.getProperty("notebookId", "default_notebook");
                var jqxhr = $.getJSON(url, function(data) {
                    if (data.error) {
                        _this.setContents(_this.getMessage("Failed to load notebook: " + data.error));
                        return;
                    }
                    if (!Utils.isDefined(data.cells)) {
                        return;
                    }
                    _this.cells = [];
                    for (var i = 0; i < data.cells.length; i++) {
                        var props = data.cells[i];
                        _this.cells.push(_this.addCell(props.outputHtml, props));
                    }
                    _this.layoutCells();
                });
            }
            if (this.cells.length > 0) {
                this.layoutCells();
            } else {
                var props = {
                    showEdit: true,
                }
                this.cells.push(this.addCell("html:{help}", props));
                this.processCells();
            }
            this.cells[0].focus();

        },
        saveNotebook: function() {
            var json = this.getJson();
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
        getJson: function() {
            var obj = {
                cells: [],
            };
            for (var i = 0; i < this.cells.length; i++) {
                var cell = this.cells[i];
                obj.cells.push(cell.getJson());
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
        addCell: function(content, props) {
            if (!props) props = {};
            var cellId = this.getId() + "_" + this.cellId;
            this.cellId++;
            this.jq(ID_CELLS).append(HtmlUtils.div([ATTR_CLASS, "display-notebook-cell", ATTR_ID, cellId], ""));
            var cell = new RamaddaNotebookCell(this, cellId, content, props);
            cell.createCell();
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
                    newCell = this.addCell(null, {
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
                    newCell = this.addCell(null, {
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
                this.cells.push(this.addCell());
            }
        },
        processCells: function() {
            for (var i = 0; i < this.cells.length; i++) {
                var cell = this.cells[i];
                if (!cell.processCell()) {
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


function nbClear() {
    notebookState.cell.notebook.clearOutput();
}

function nbStop() {
    notebookState.stop = true;
}

var notebookState = {
    cell: null,
    stop: false,
    displayNotebookResult: null,
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

    if (props)
        $.extend(this, props);
    RamaddaUtil.defineMembers(this, {
        getJson: function() {
            return {
                id: this.id,
                inputRows: this.inputRows,
                content: this.content,
                outputHtml: this.outputHtml,
                pinned: this.pinned,
                showEdit: this.showEdit,
                showHeader: this.showHeader,
                showBorder: this.showBorder,
                cellName: this.cellName,
            }
        },
        createCell: function() {
            if (this.content == null) this.content = "html:";
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

            //            console.log(table);
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
            this.menuButton.click(function() {
                var space = "&nbsp;&nbsp;";
                var menu = "";
                menu += HtmlUtils.input(ID_CELLNAME_INPUT, _this.cellName, ["placeholder", "Cell name", "style", "width:100%;", "xsize", "20", "id", _this.getDomId(ID_CELLNAME_INPUT)]);
                menu += "<br>";
                menu += "<table>";

                menu += "<tr><td align=right><b>Run:</b>&nbsp;</td><td>";
                menu += HtmlUtils.div(["title", "shift-return", "class", "ramadda-link", "what", "run"], "This cell") + space;
                menu += HtmlUtils.div(["title", "ctrl-return", "class", "ramadda-link", "what", "runall"], "All");
                menu += "</td></tr>"

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
                menu += "</td></tr>"

                menu += "<tr><td align=right><b>Clear:</b>&nbsp;</td><td>";
                menu += HtmlUtils.div(["class", "ramadda-link", "what", "clear"], "This cell") + space;
                menu += HtmlUtils.div(["class", "ramadda-link", "what", "clearall"], "All");
                menu += "</td></tr>"

                menu += "<tr><td align=right><b>Move:</b>&nbsp;</td><td>";
                menu += HtmlUtils.div(["title", "ctrl-^", "class", "ramadda-link", "what", "moveup"], "Up") + space;
                menu += HtmlUtils.div(["title", "ctrl-v", "class", "ramadda-link", "what", "movedown"], "Down");
                menu += "</td></tr>"

                menu += "<tr><td align=right><b>New Cell:</b>&nbsp;</td><td>";
                menu += HtmlUtils.div(["class", "ramadda-link", "what", "newabove"], "Above") + space;
                menu += HtmlUtils.div(["class", "ramadda-link", "what", "newbelow"], "Below");
                menu += "</td></tr>"


                menu += "</table>";
                menu += HtmlUtils.div(["class", "ramadda-link", "what", "delete"], "Delete") + "<br>";
                menu += HtmlUtils.div(["class", "ramadda-link", "what", "save"], "Save Notebook") + "<br>";
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
                        _this.processCell();
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
                        _this.notebook.processCells();
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
                    } else if (what == "save") {
                        _this.notebook.saveNotebook();
                    } else if (what == "help") {
                        var win = window.open(ramaddaBaseUrl + "/userguide/wikidisplay.html#notebook", '_blank');
                        win.focus();
                    } else if (what == "delete") {
                        _this.askDelete();
                    }
                });

            });
            var hoverIn = function() {
                _this.checkMenuButton(true);
            }
            var hoverOut = function() {
                _this.jq(ID_MENU).hide();
                _this.checkMenuButton(false);
            }
            this.cell.hover(hoverIn, hoverOut);
            //            this.output.hover(hoverIn, hoverOut);
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
                        _this.processCell();
                    } else if (e.ctrlKey) {
                        if (e.preventDefault) {
                            e.preventDefault();
                        }
                        _this.notebook.processCells();
                    }
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
        processCell: function() {
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
                        var blobResult = this.processBlob(type, blob, r);
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
                var blobResult = this.processBlob(type, blob, r);
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
        processWiki: function(blob, result) {
            var id = this.notebook.getProperty("entryId", "");
            var entry = this.getCurrentEntry(null);
            if (entry) id = entry.getId();
            let _this = this;
            let divId = HtmlUtils.getUniqueId();
            var callback = function(html) {
                $("#" + divId).html(html);
                Utils.initContent(_this.getDomId(ID_OUTPUT));
                _this.outputUpdated();
            }
            blob = "{{group showMenu=false}}\n" + blob;
            GuiUtils.loadHtml(ramaddaBaseUrl + "/wikify?doImports=false&entryid=" + id + "&text=" + encodeURIComponent(blob),
                callback);
            var h = HtmlUtils.div(["id", divId], "Processing...");
            return h;
        },
        processSh: function(blob, result) {
            var r = "";
            var lines = blob.split("\n");
            for (var i = 0; i < lines.length; i++) {
                var line = lines[i];
                if (line == "") continue;
                var toks = line.split(" ");
                var command = toks[0].trim();
                if (this["processCommand_" + command]) {
                    r += this["processCommand_" + command](line, toks);
                    r += "<br>";
                } else {
                    r += this.processCommand_help(line, toks, "Unknown command: <i>" + command + "</i>");
                    r += "<br>";
                }
            }
            return r;
        },
        processJs: function(blob, result) {
            try {
                notebookState.stop = false;
                notebookState.cell = this;
                notebookState.displayNotebookResult = null;
                var js = "function nbFunc() {\n" + blob + "\n}\nnotebookState.displayNotebookResult = nbFunc();";
                var r = eval(js);
                if (notebookState.stop) {
                    result.ok = false;
                }
                if (!notebookState.displayNotebookResult) return "";
                return notebookState.displayNotebookResult;
            } catch (e) {
                result.ok = false;
                var lines = blob.split("\n");
                var line = lines[e.lineNumber - 2];
                return "Error: " + e.message + "<br>" + HtmlUtils.span(["class", "display-notebook-error"], " &gt; " + line);
            }
        },

        processBlob: function(type, blob, result) {
            if (blob == "") {
                return "";
            }
            if (type == "html") {
                return this.processHtml(blob, result);
            } else if (type == "wiki") {
                return this.processWiki(blob, result);
            } else if (type == "js") {
                return this.processJs(blob, result);
            } else {
                return this.processSh(blob, result);
            }
            return "unknown:" + blob;
        },


        calculateInputHeight: function() {
            this.content = this.input.val();
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
        processCommand_help: function(line, toks, prefix) {
            var help = "";
            if (prefix != null) help += prefix;
            help += "<pre>pwd, ls, cd</pre>";
            return help;
        },
        entries: {},

       cdEntry: function(entryId) {
                this.currentEntry = this.entries[entryId];
                this.output.html(this.processCommand_pwd("pwd", []));
                this.outputUpdated();
            },
         getEntryPrefix: function(id, entry) {
               this.entries[entry.getId()] = entry;
                var call = "getHandler('" + id +"').cdEntry('" + entry.getId() +"')";
                return HtmlUtils.div(["style","padding-right:4px;", "title","cd to entry", "onclick", call,"class","ramadda-link"], HtmlUtils.image(ramaddaBaseUrl+"/icons/go.png"));
         },
        displayEntries: function(entries, divId) {
            this.currentEntries = entries;
            var handlerId = addHandler(this);
            var html = this.notebook.getEntriesTree(entries, {
                handlerId: handlerId,
                showIndex: false,
                suffix: "_shell_" + (this.uniqueCnt++)
            });
            $("#" + divId).html(html);
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
        getCurrentEntry: function(dflt) {
            if (this.currentEntry == null) {
                this.currentEntry = this.notebook.currentEntry;
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
        processCommand_pwd: function(line, toks) {
            var entry = this.getCurrentEntry();
            var icon = entry.getIconImage([ATTR_TITLE, "View entry"]);
            var html = this.notebook.getEntriesTree([entry], {
                suffix: "_YYY"
            });
            //                var link  =  HtmlUtils.tag(TAG_A,[ATTR_HREF, entry.getEntryUrl()],icon+" "+ entry.getName());
            return "Current entry:<br>" + html;
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
                let _this = this;
                console.log("cd:" + toks);
            if (toks.length <= 1) {
                this.currentEntry = this.notebook.currentEntry;
                return this.processCommand_pwd("pwd", []);
            }
            if(toks[1]=="..") {
                var entry = this.getCurrentEntry();
                this.currentEntry = entry.getParentEntry(entry=>{console.log(entry);
                                                                 if(entry) {
                                                                     _this.currentEntry=entry;
                                                                     _this.processCommand_cd(line, toks)
                                                                         }});
                if(this.currentEntry) {
                    return this.processCommand_pwd("pwd", []);
                }
                return "Retrieving entry...";
            }
            return "NA";

        },
        processCommand_ls: function(line, toks) {
            let _this = this;
            let divId = HtmlUtils.getUniqueId();
            var callback = function(children) {
                _this.displayEntries(children, divId);
            };
            var children = this.getCurrentEntry().getChildrenEntries(callback, "");
            if (children != null) {
                setTimeout(function() {
                    _this.displayEntries(children, divId);
                }, 1);
            }
            return HtmlUtils.div(["style", "max-height:200px;overflow-y:auto;", "id", divId], "Listing entries...");
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
        processCommand_clear: function(toks) {
            this.clearOutput();
        },
    });

}
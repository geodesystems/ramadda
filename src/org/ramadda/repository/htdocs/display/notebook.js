/**
   Copyright 2008-2024 Geode Systems LLC
*/

const DISPLAY_NOTEBOOK = "notebook";
addGlobalDisplayType({
    type: DISPLAY_NOTEBOOK,
    label: "Notebook",
    requiresData: false,
    category: CATEGORY_CONTROLS
});

var pluginDefintions = {
    'jsx': {
        "languageId": "jsx",
        "displayName": "React JSX",
        "url": "https://raw.githubusercontent.com/hamilton/iodide-jsx/master/docs/evaluate-jsx.js",
        "module": "jsx",
        "evaluator": "evaluateJSX",
        "pluginType": "language"
    },
    "lisp": {
        "languageId": "lisp",
        "displayName": "Microtalk Lisp",
        "url": "https://ds604.neocities.org/js/microtalk.js",
        "module": "MICROTALK",
        "evaluator": "evaluate",
        "pluginType": "language",
        "outputHandler": "processLispOutput",
    },
    "sql": {
        "languageId": "sql",
        "displayName": "SqlLite",
        "url": ramaddaBaseHtdocs+"/lib/notebook/sqllite.js",
        "module": "SqlLite",
        "evaluator": "evaluate",
        "pluginType": "language"
    },
    "plantuml": {
        "languageId": "plantuml",
        "displayName": "PlantUml",
        "codeMirrorMode": "",
        "keybinding": "x",
        "url": "https://raw.githubusercontent.com/six42/iodide-plantuml-plugin/master/src/iodide-plantuml-plugin.js",
        "depends": [{
            "type": "js",
            "url": "https://raw.githubusercontent.com/johan/js-deflate/master/rawdeflate.js"
        }],
        "module": "plantuml",
        "evaluator": "plantuml_img",
        "pluginType": "language"
    },
    "ml": {
        "languageId": "ml",
        "displayName": "ocaml",
        "codeMirrorMode": "mllike",
        "keybinding": "o",
        "url": "https://louisabraham.github.io/domical/eval.js",
        "module": "evaluator",
        "evaluator": "execute",
        "pluginType": "language",
        "depends": [{
            "type": "css",
            "url": "https://louisabraham.github.io/domical/style.css"
        }]
    }
};




function RamaddaNotebookDisplay(displayManager, id, properties) {
    var ID_NOTEBOOK = "notebook";
    var ID_IMPORTS = "imports";
    var ID_CELLS = "cells";
    var ID_CELLS_BOTTOM = "cellsbottom";
    var ID_INPUTS = "inputs";
    var ID_OUTPUTS = "outputs";
    var ID_CONSOLE = "console";
    var ID_CONSOLE_TOOLBAR = "consoletoolbar";
    var ID_CONSOLE_CONTAINER = "consolecontainer";
    var ID_CONSOLE_OUTPUT = "consoleout";
    var ID_CELL = "cell";
    var ID_MENU = "menu";
    this.properties = properties || {};
    let SUPER = new RamaddaDisplay(displayManager, id, DISPLAY_NOTEBOOK, properties);
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
        runOnLoad: this.getProperty("runOnLoad", true),
        displayMode: this.getProperty("displayMode", false),
        showConsole: this.getProperty("showConsole", true),
        consoleHidden: this.getProperty("consoleHidden", false),
        layout: this.getProperty("layout", "horizontal"),
        columns: this.getProperty("columns", 1),
    });

    RamaddaUtil.defineMembers(this, {
        cells: [],
        cellCount: 0,
        fetchedNotebook: false,
        currentEntries: {},
        globals: {},
        baseEntries: {},
        outputRenderers: [],
        initDisplay: async function() {
            this.createUI();
            var imports = HtmlUtils.div(["id", this.getDomId(ID_IMPORTS)]);
            var contents = imports + HtmlUtils.div([ATTR_CLASS, "display-notebook-cells", ATTR_ID, this.getDomId(ID_CELLS)], "&nbsp;&nbsp;Loading...") +
                HtmlUtils.div([ATTR_ID, this.getDomId(ID_CELLS_BOTTOM)]);
            var popup = HtmlUtils.div(["class", "ramadda-popup", ATTR_ID, this.getDomId(ID_MENU)]);
            contents = HtmlUtils.div([ATTR_ID, this.getDomId(ID_NOTEBOOK)], popup + contents);
            this.setContents(contents);
            this.makeCellLayout();
            this.jq(ID_NOTEBOOK).hover(() => {}, () => {
                this.jq(ID_MENU).hide()
            });
            if (!this.fetchedNotebook) {
                this.initOutputRenderers();
                if (!this.fetchingNotebook) {
                    this.fetchingNotebook = true;
                    await Utils.importJS(ramaddaBaseHtdocs + "/lib/ace/src-min/ace.js");
                    await Utils.importJS(ramaddaBaseUrl + "/lib/showdown.min.js");
                    var imports = "<link rel='preload' href='https://cdn.jsdelivr.net/npm/katex@0.10.1/dist/fonts/KaTeX_Main-Regular.woff2' as='font' type='font/woff2' crossorigin='anonymous'>\n<link rel='preload' href='https://cdn.jsdelivr.net/npm/katex@0.10.1/dist/fonts/KaTeX_Math-Italic.woff2' as='font' type='font/woff2' crossorigin='anonymous'>\n<link rel='preload' href='https://cdn.jsdelivr.net/npm/katex@0.10.1/dist/fonts/KaTeX_Size2-Regular.woff2' as='font' type='font/woff2' crossorigin='anonymous'>\n<link rel='preload' href='https://cdn.jsdelivr.net/npm/katex@0.10.1/dist/fonts/KaTeX_Size4-Regular.woff2' as='font' type='font/woff2' crossorigin='anonymous'/>\n<link rel='stylesheet' href='https://fonts.googleapis.com/css?family=Lato:300,400,700,700i'>\n<link rel='stylesheet' href='https://cdn.jsdelivr.net/npm/katex@0.10.1/dist/katex.min.css' crossorigin='anonymous'>\n<script defer src='https://cdn.jsdelivr.net/npm/katex@0.10.1/dist/katex.min.js' crossorigin='anonymous'></script>";
                    $(imports).appendTo("head");
                    setTimeout(() => this.fetchNotebook(1), 10);
                }
            } else {
                this.layoutCells();
            }
        },
        fetchNotebook: async function(cnt) {
            if (!window["ace"]) {
                if (cnt > 50) {
                    alert("Could not load ace.js");
                    return;
                }
                setTimeout(() => this.fetchNotebook(cnt + 1), cnt * 10);
                return;
            }
            var dttm = new Date().getTime();
            ace.config.set('basePath', ramaddaBaseUrl + "/htdocs_v" + dttm + "/lib/ace/src-min");
            let _this = this;
            this.fetchedNotebook = true;
            await this.getEntry(this.getProperty("entryId", ""), entry => {
                this.baseEntry = entry;
            });
            await this.baseEntry.getRoot(entry => {
                this.rootEntry = entry;
            });
            var id = this.getProperty("entryId", "");
            var url = ramaddaBaseUrl + "/getnotebook?entryid=" + id;
            url += "&notebookId=" + this.getProperty("notebookId", "default_notebook");
            var jqxhr = $.getJSON(url, function(data) {
                _this.loadJson(data);
            }).fail(function() {
                var props = {
                    showInput: true,
                }
                this.addCell("init cell", props, false).run();
                this.cells[0].focus();
            });

        },
        formatObject: function(value) {
            return Utils.formatJson(value);
        },
        initOutputRenderers: function() {
            let notebook = this;
            this.outputRenderers = [];
            /*
            this.addOutputRenderer({
                    shouldRender: (value) => {return typeof value === "object";},
                        render: (value) => {if(Array.isArray(value)) return HtmlUtils.div(["style"," white-space: pre;"], JSON.stringify(value)); return HtmlUtils.div(["style"," white-space: pre;"],JSON.stringify(value,null,2))},
                        });
            */
            this.addOutputRenderer({
                shouldRender: (value) => {
                    return Array.isArray(value);
                },
                render: (value) => {
                    return Utils.formatJson(value);
                },
            });
            this.addOutputRenderer({
                shouldRender: (value) => {
                    return Array.isArray(value) && value.length > 0 && Array.isArray(value[0]);
                },
                render: (value) => {
                    var table = "<table>";
                    for (var rowIdx = 0; rowIdx < value.length; rowIdx++) {
                        var row = value[rowIdx];
                        table += "<tr>";
                        for (var colIdx = 0; colIdx < row.length; colIdx++) {
                            table += "<td>&nbsp;" + row[colIdx] + "</td>";
                        }
                        table += "</tr>";
                    }
                    table += "</table>";
                    return table;

                }
            });


            this.addOutputRenderer({
                shouldRender: (value) => {
                    return typeof value === "object" && value.getTime;
                },
                render: (value) => {
                    return notebook.formatDate(value)
                },
            });

            this.addOutputRenderer({
                shouldRender: (value) => {
                    var t = typeof value;
                    return t === "string" || t === "number" || t === "boolean";
                },
                render: (value) => {
                    if (typeof value === "string") {
                        if (value.split("\n").length > 1) {
                            return HtmlUtils.div(["style", " white-space: pre;"], value);
                        }
                    }
                    return value
                },
            });
            this.addOutputRenderer({
                shouldRender: (value) => {
                    return typeof value === "object" && "lat" in value && "lon" in value;
                },
                render: (value) => {
                    var url = 'http://staticmap.openstreetmap.de/staticmap.php?center=' + value.lat + ',' + value.lon + '&zoom=17&size=400x150&maptype=mapnik';
                    return "<img src='" + url + "'/>"
                },
            });

        },
        addOutputRenderer: function(renderer) {
            if (this.outputRenderers.indexOf(renderer) < 0) {
                this.outputRenderers.push(renderer);
            }
        },
        formatOutput: function(value) {
            if (!value) return null;
            for (var i = this.outputRenderers.length - 1; i >= 0; i--) {
                var renderer = this.outputRenderers[i];
                if (renderer.shouldRender && renderer.shouldRender(value)) {
                    return renderer.render(value);
                }
            }
            var v = null;
            if (value.iodideRender) {
                v = value.iodideRender();
            } else if (value.notebookRender) {
                v = value.notebookRender();
            }
            if (v) {
                //TODO handle elements
                if (typeof v == "string") {
                    return v;
                }
            }
            return null;
        },
        getBaseEntry: function() {
            return this.baseEntry;
        },
        getRootEntry: function() {
            return this.rootEntry;
        },
        getPopup: function() {
            return this.jq(ID_MENU);
        },
        loadJson: async function(data) {
            if (data.error) {
                this.setContents(_this.getMessage("Failed to load notebook: " + data.error));
                return;
            }
            if (!Utils.isDefined(this.properties.runOnLoad) && Utils.isDefined(data.runOnLoad)) {
                this.runOnLoad = data.runOnLoad;
            }
            if (!Utils.isDefined(this.properties.displayMode) && Utils.isDefined(data.displayMode)) {
                this.displayMode = data.displayMode;
            }
            if (!Utils.isDefined(this.properties.showConsole) && Utils.isDefined(data.showConsole)) {
                this.showConsole = data.showConsole;
            }
            if (Utils.isDefined(data.consoleHidden)) {
                this.consoleHidden = data.consoleHidden;
            }
            if (!Utils.isDefined(this.properties.columns) && Utils.isDefined(data.columns)) {
                this.columns = data.columns;
            }
            if (!Utils.isDefined(this.properties.layout) && Utils.isDefined(data.layout)) {
                this.layout = data.layout;
            }

            if (Utils.isDefined(data.currentEntries)) {
                for (a in data.currentEntries) {
                    var obj = {};
                    if (this.currentEntries[a]) continue;
                    obj.name = a;
                    obj.entryId = data.currentEntries[a].entryId;
                    try {
                        await this.getEntry(obj.entryId, e => obj.entry = e);
                        this.currentEntries[a] = obj;
                    } catch (e) {}
                }
            }
            if (Utils.isDefined(data.cells)) {
                this.cells = [];
                data.cells.forEach(cell => this.addCell(cell.outputHtml, cell, true));
                this.layoutCells();
            }
            if (this.cells.length == 0) {
                var props = {
                    showInput: true,
                }
                this.addCell("%%wiki\n", props, false);
                this.layoutCells();
                this.cells[0].focus();
            }
            if (this.runOnLoad) {
                this.runAll();
            }
        },
        addEntry: async function(name, entryId) {
            var entry;
            await this.getEntry(entryId, e => entry = e);
            this.currentEntries[name] = {
                entryId: entryId,
                entry: entry
            };
        },
        getCurrentEntries: function() {
            return this.currentEntries;
        },
        clearEntries: function() {
            this.currentEntries = {};
            for (a in this.baseEntries)
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
            $.post(url, args, (result) => {
                if (result.error) {
                    alert("Error saving notebook: " + result.error);
                    return;
                }
                if (result.result != "ok") {
                    alert("Error saving notebook: " + result.result);
                    return;
                }
                if (!this.getShowConsole()) {
                    alert("Notebook saved");
                } else {
                    this.log("Notebook saved", "info", "nb");
                }
            });
        },
        showInput: function() {
            if (this.displayMode && !this.getProperty("user")) {
                return false;
            }
            if (this.getProperty("showInput", true) == false) {
                return false;
	    }
            return true;
        },
        getJson: function(output) {
            var obj = {
                cells: [],
                currentEntries: {},
                runOnLoad: this.runOnLoad,
                displayMode: this.displayMode,
                showConsole: this.showConsole,
                consoleHidden: this.consoleHidden,
                layout: this.layout,
                columns: this.columns,
            };
            for (var name in this.currentEntries) {
                var e = this.currentEntries[name];
                obj.currentEntries[name] = {
                    entryId: e.entryId
                };
            }
            this.cells.forEach(cell => obj.cells.push(cell.getJson(output)));
            return obj;
        },
        initConsole: function() {
            if (!this.showInput()) {
                return;
            }
            let _this = this;
            this.console = this.jq(ID_CONSOLE_OUTPUT);
            if (this.consoleHidden)
                this.console.hide();
            this.jq(ID_CONSOLE).find(".ramadda-image-link").click(function(e) {
                var what = $(this).attr("what");
                if (what == "clear") {
                    _this.console.html("");
                }
                e.stopPropagation();
            });

            this.consoleToolbar = this.jq(ID_CONSOLE_TOOLBAR);
            this.consoleToolbar.click(() => {
                if (this.console.is(":visible")) {
                    this.console.hide(400);
                    this.consoleHidden = true;
                } else {
                    this.consoleHidden = false;
                    this.console.show(400);
                }
            });
        },
        getShowConsole: function() {
            return this.showInput() && this.showConsole;
        },
        makeConsole: function() {
            this.console = null;
            if (!this.getShowConsole()) {
                return "";
            }
            var contents = this.jq(ID_CONSOLE_OUTPUT).html();
            var consoleToolbar = HtmlUtils.div(["id", this.getDomId(ID_CONSOLE_TOOLBAR), "class", "display-notebook-console-toolbar", "title", "click to hide/show console"],
                HtmlUtils.leftRight("",
                    HtmlUtils.span(["class", "ramadda-image-link", "title", "Clear", "what", "clear"],
                        HtmlUtils.image(Utils.getIcon("clear.png")))));
            return HtmlUtils.div(["id", this.getDomId(ID_CONSOLE), "class", "display-notebook-console"],
                consoleToolbar +
                HtmlUtils.div(["class", "display-notebook-console-output", "id", this.getDomId(ID_CONSOLE_OUTPUT)], contents || ""));
        },

        makeCellLayout: function() {
            var html = "";
            var consoleContainer = HtmlUtils.div(["id", this.getDomId(ID_CONSOLE_CONTAINER)]);
            this.jq(ID_CELLS_BOTTOM).html("");
            if (this.showInput() && this.layout == "horizontal") {
                var left = HtmlUtils.div(["id", this.getDomId(ID_INPUTS), "style", "width:100%;"]);
                var right = HtmlUtils.div(["id", this.getDomId(ID_OUTPUTS), "style", "width:100%;"]);
                var center = HtmlUtils.div([], "");
                left += consoleContainer;
                html = "<table style='table-layout:fixed;' border=0 width=100%><tr valign=top><td width=50%>" + left + "</td><td style='border-left:1px #ccc solid;' width=1>" + center + "</td><td width=49%>" + right + "</td></tr></table>";
            } else {
                this.jq(ID_CELLS_BOTTOM).html(consoleContainer);
            }
            this.jq(ID_CELLS).html(html);
        },
        plugins: {},
        addPlugin: async function(plugin, chunk) {
            var error;
            if (plugin.depends) {
                for (var i = 0; i < plugin.depends.length; i++) {
                    var obj = plugin.depends[i];
                    var type = obj.type;
                    var url = obj.url;
                    if (type == "js") {
                        await Utils.importJS(url,
                            () => {},
                            (jqxhr, settings, exception) => {
                                error = "Error fetching plugin url:" + url;
                            });
                    } else if (type == "css") {
                        await Utils.importCSS(url,
                            () => {},
                            (jqxhr, settings, exception) => {
                                error = "Error fetching plugin url:" + url;
                            });
                    }
                    if (error) {
                        this.log(error, "error", "nb", chunk ? chunk.div : null);
                        return;
                    }
                }
            }

            var url = Utils.replaceRoot(plugin.url);
            await Utils.importJS(url,
                () => {},
                (jqxhr, settings, exception) => {
                    error = "Error fetching plugin url:" + url;
                });
            if (!error) {
                var module = plugin.module;
                var tries = 200;
                //Wait 20 seconds max
                while (window[module] == null && tries-- > 0) {
                    await new Promise(resolve => setTimeout(resolve, 100));
                }
                if (!window[module]) {
                    error = "Could not load plugin module: " + module;
                } else {
                    if (window[module].isPluginReady) {
                        var tries = 200;
                        while (!window[module].isPluginReady() && tries-- > 0) {
                            //                            console.log("not ready yet:" + tries);
                            await new Promise(resolve => setTimeout(resolve, 100));
                        }
                        //                        console.log("final ready:" + window[module].isPluginReady() );
                        if (!window[module].isPluginReady())
                            error = "Could not load plugin module: " + module;
                    }
                }
            }
            if (error) {
                this.log(error, "error", "nb", chunk ? chunk.div : null);
                return;
            }
            this.plugins[plugin.languageId] = plugin;
        },
        hasPlugin: async function(id, callback) {
            if (!this.plugins[id]) {
                if (window.pluginDefintions[id]) {
                    await this.addPlugin(window.pluginDefintions[id], null);
                }
            }
            Utils.call(callback, this.plugins[id] != null);
        },
        processChunkWithPlugin: async function(id, chunk, callback) {
            var module = this.plugins[id].module;
            var func = this.plugins[id].evaluator;
            var result = window[module][func](chunk.getContent(), chunk);
            return Utils.call(callback, result);

        },
        processPluginOutput: function(id, chunk, result) {
            if (!result) return;
            var module = this.plugins[id].module;
            var func = window[this.plugins[id].outputHandler];
            if (func) {
                chunk.div.append(func(result));
            } else {
                if (typeof result == "object") {
                    //TODO: for now don't format this as some results are recursive
                    //                   console.log(result);
                    //                   chunk.div.set(this.formatObject(result));
                } else {
                    chunk.div.set(result);
                }
            }
        },
        log: function(msg, type, from, div) {
            var icon = "";
            var clazz = "display-notebook-console-item";
            if (typeof msg == "object") {
                msg = Utils.formatJson(msg);
            }
            if (type == "error") {
                clazz += " display-notebook-console-item-error";
                icon = HtmlUtils.image(Utils.getIcon("cross-octagon.png"));
                if (div) {
                    div.append(HtmlUtils.div(["class", "display-notebook-chunk-error"], msg));
                }
            } else if (type == "output") {
                clazz += " display-notebook-console-item-output";
                icon = HtmlUtils.image(Utils.getIcon("arrow-000-small.png"));
            } else if (type == "info") {
                clazz += " display-notebook-console-item-info";
                icon = HtmlUtils.image(Utils.getIcon("information.png"));
            }
            if (!this.console) return;
            if (!from) from = "";
            else from = HtmlUtils.div(["class", "display-notebook-console-from"], from);
            var block = HtmlUtils.div(["style", "margin-left:5px;"], msg);
            var html = "<table width=100%><tr valign=top><td width=10>" + icon + "</td><td>" +
                block +
                "</td><td width=10>" +
                from +
                "</td></tr></table>";
            var item = HtmlUtils.div(["class", clazz], html);
            this.console.append(item);
            //200 is defined in display.css
            var height = this.console.prop('scrollHeight');
            if (height > 200)
                this.console.scrollTop(height - 200);
        },
        clearConsole: function() {
            this.console.html("");
        },
        layoutCells: function() {
            for (var i = 0; i < this.cells.length; i++) {
                var cell = this.cells[i];
                cell.prepareToLayout();
            }
            this.makeCellLayout();
            if (this.showInput() && this.layout == "horizontal") {
                var left = "";
                var right = "";
                var id;
                for (var i = 0; i < this.cells.length; i++) {
                    var cell = this.cells[i];
                    id = cell.id;
                    cell.index = i + 1;
                    left += HtmlUtils.div([ATTR_CLASS, "display-notebook-cell", ATTR_ID, cell.id + "_cellinput"], "");
                    left += "\n";
                    right += HtmlUtils.div([ATTR_CLASS, "display-notebook-cell", ATTR_ID, cell.id + "_celloutput"], "");
                }
                this.jq(ID_INPUTS).html(left);
                this.jq(ID_OUTPUTS).html(right);
            } else {
                var html = "<div class=row style='padding:0px;margin:0px;'>";
                var clazz = HtmlUtils.getBootstrapClass(this.columns);
                var colCnt = 0;
                for (var i = 0; i < this.cells.length; i++) {
                    var cell = this.cells[i];
                    cell.index = i + 1;
                    html += HtmlUtils.openTag("div", ["class", clazz]);
                    html += HtmlUtils.openTag("div", ["style", "max-width:100%;overflow-x:auto;padding:0px;margin:px;"]);
                    html += HtmlUtils.div([ATTR_CLASS, "display-notebook-cell", ATTR_ID, cell.id + "_cellinput"], "");
                    html += HtmlUtils.div([ATTR_CLASS, "display-notebook-cell", ATTR_ID, cell.id + "_celloutput"], "");
                    html += HtmlUtils.closeTag("div");
                    html += HtmlUtils.closeTag("div");
                    html += "\n";
                    colCnt++;
                    if (colCnt >= this.columns) {
                        colCnt = 0;
                        html += HtmlUtils.closeTag("div");
                        html += "<div class=row style='padding:0px;margin:0px;'>";
                    }
                };
                html += HtmlUtils.closeTag("div");
                this.jq(ID_CELLS).append(html);
            }
            for (var i = 0; i < this.cells.length; i++) {
                var cell = this.cells[i];
                cell.createCell();
            };
            this.jq(ID_CONSOLE_CONTAINER).html(this.makeConsole());
            this.initConsole();
        },
        addCell: function(content, props, layoutLater) {
            cell = this.createCell(content, props);
            this.cells.push(cell);
            if (!layoutLater) {
                if (this.showInput() && this.layout == "horizontal") {
                    this.jq(ID_INPUTS).append(HtmlUtils.div([ATTR_CLASS, "display-notebook-cell", ATTR_ID, cell.id + "_cellinput"], ""));
                    this.jq(ID_OUTPUTS).append(HtmlUtils.div([ATTR_CLASS, "display-notebook-cell", ATTR_ID, cell.id + "_celloutput"], ""));
                } else {
                    this.jq(ID_CELLS).append(HtmlUtils.div([ATTR_CLASS, "display-notebook-cell", ATTR_ID, cell.id + "_cellinput"], ""));
                    this.jq(ID_CELLS).append(HtmlUtils.div([ATTR_CLASS, "display-notebook-cell", ATTR_ID, cell.id + "_celloutput"], ""));
                }
                cell.createCell();
            }
            return cell;
        },
        createCell: function(content, props) {
            if (!props) props = {
                showInput: true
            };
            var cellId = this.getId() + "_" + this.cellCount;
            //Override any saved id
            props.id = cellId;
            this.cellCount++;
            this.jq(ID_CELLS).append(HtmlUtils.div([ATTR_CLASS, "display-notebook-cell", ATTR_ID, cellId], ""));
            var cell = new RamaddaNotebookCell(this, cellId, content, props);
            return cell;
        },
        clearOutput: function() {
            this.cells.forEach(cell => cell.clearOutput());
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
                    newCell = this.createCell("%%wiki\n", {
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
                    newCell = this.createCell("%%wiki\n", {
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
            this.cells.forEach(c => {
                if (cell.id != c.id) {
                    cells.push(c);
                }
            });
            this.cells = cells;
            if (this.cells.length == 0) {
                this.addCell("", null);
            }
        },
        cellValues: {},
        setCellValue: function(name, value) {
            this.cellValues[name] = value;
        },
        getCellValues: function() {
            return this.cellValues;
        },
        convertInput: function(input) {
            for (name in this.cellValues) {
                var re = new RegExp("\\$\\{" + name + "\\}", "g");
                input = input.replace(re, this.cellValues[name]);
            }
            return input;
        },
        inGlobalChanged: false,
        globalChanged: async function(name, value) {
                var globalChangeCalled = this.inGlobalChanged;
                var top = !this.inGlobalChanged;
                if(this.inRunAll) {
                    top =  false;
                }
                this.inGlobalChanged=true;
                if(top) {
                    this.cells.forEach(cell=>cell.prepareToRun());
                }
                for(var i=0;i<this.cells.length;i++) {
                    await this.cells[i].globalChanged(name,value);
                }
                if(!globalChangeCalled) {
                    this.inGlobalChanged = false;
                }
        },
        addGlobal: async function(name, value, dontPropagate) {
            //TODO: more var name cleanup
            name = name.trim().replace(/[ -]/g, "_");
            var oldValue = this.getGlobalValue(name);
            if (Utils.isDefined(window[name])) window[name] = value;
            this.globals[name] = value;
            if(!dontPropagate) {
                var newValue = this.getGlobalValue(name);
                if(newValue!=oldValue) {
                    //TODO:
                    //                    await this.globalChanged(name, newValue);
                }
            }
        },
        getGlobalValue: function(name) {
                if(!this.globals[name]) return null;
                if(typeof this.globals[name] =="function") return this.globals[name]();
                return this.globals[name];
        },
        inRunAll: false,
        runAll: async function() {
            this.inRunAll = true;
            var ok = true;
            this.cellValues = {};
            for (var i = 0; i < this.cells.length; i++) {
                var cell = this.cells[i];
                cell.prepareToRun();
            }
            for (var i = 0; i < this.cells.length; i++) {
                var cell = this.cells[i];
                if (!cell.runFirst) continue;
                await this.runCell(cell).then(result => ok = result);
            }
            if (!ok) return;
            for (var i = 0; i < this.cells.length; i++) {
                var cell = this.cells[i];
                if (cell.runFirst) continue;
                await this.runCell(cell, true).then(result => ok = result);
            }
            this.inRunAll = false;
        },
        runCell: async function(cell, doingAll) {
            if (cell.hasRun) return true;
            await cell.run(result => ok = result, {
                doingAll: doingAll
            });
            if (!ok) return false;
            var raw = cell.getRawOutput();
            if (raw) {
                raw = raw.trim();
                if (Utils.stringDefined(cell.cellName)) {
                    this.cellValues[cell.cellName] = raw;
                }
            }
            return true;
        },
        toggleAll: function(on) {
            this.cells.forEach(cell => {
                cell.showInput = on;
                cell.applyStyle();
            });
        },

    });
}




var iodide = {
    addOutputRenderer: function(renderer) {
        notebook.addOutputRenderer(renderer);
    },
    addOutputHandler: function(renderer) {
        notebook.addOutputHandler(renderer);
    },
    output: {
        text: function(t) {
            notebook.write(t);
        },
        element: function(tag) {
            var id = HtmlUtils.getUniqueId();
            notebook.write(HtmlUtils.tag(tag, ["id", id]));
            return document.getElementById(id);
        }
    },
};

var notebook;


function NotebookState(cell, div) {
    this.id = HtmlUtils.getUniqueId();
    this.cell = cell;
    this.notebook = cell.notebook;
    $.extend(this, {
        entries: {},
        div: div,
        stopFlag: false,
        result: null,
        log: function(msg, type, from) {
            this.getNotebook().log(msg, type, from, this.div);
        },
        clearConsole: function() {
            this.getNotebook().clearConsole();
        },
        getStop: function() {
            return this.stopFlag;
        },
        getCell: function() {
            return this.cell;
        },
        addGlobal: async function(name,value) {
                await this.getNotebook().addGlobal(name,value);
        },

        globalChanged: async function(name,value) {
                await this.getNotebook().globalChanged(name,value);
        },
        setValue: function(name, value) {
            this.notebook.setCellValue(name, value);
        },
        makeData: async function(entry) {
            if (!entry)
                await this.getCurrentEntry(e => entry = e);
            if ((typeof entry) == "string") {
                await this.notebook.getEntry(entry, e => entry = e);
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
            return new PointData(entry.getName(), null, null, jsonUrl, pointDataProps);
        },
        log: function(msg, type) {
            this.getNotebook().log(msg, type, "js");
        },
        getNotebook: function() {
            return this.notebook;
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
            if (!entry)
                await this.getCurrentEntry(e => entry = e);
            this.call.getEntryHeading(entry, div);
            this.write(div.toString());
        },

        lsEntries: function() {
            var h = "";
            var entries = this.currentEntries;
            for (var name in entries) {
                var e = entries[name];
                h += name + "=" + e.entry.getName() + "<br>";
            }
            this.write(h);
        },

        stop: function() {
            this.stopFlag = true;
        },
        setGlobal: async function(name, value) {
                await this.cell.notebook.addGlobal(name, value);
        },
        setEntry: function(name, entryId) {
            this.cell.notebook.addEntry(name, entryId);
        },
        getEntry: async function(entryId, callback) {
            await this.cell.notebook.getEntry(e => entry = e);
            return Utils.call(callback, entry);
        },
        wiki: async function(s, entry, callback) {
            if (!callback) {
                var wdiv = new Div();
                this.div.append(wdiv.toString());
                callback = h => wdiv.append(h);
            }
            if (entry == null)
                await this.cell.getCurrentEntry(e => entry = e);
            if ((typeof entry) != "string") entry = entry.getId();
            await GuiUtils.loadHtml(ramaddaBaseUrl + "/wikify?doImports=false&entryid=" + entry + "&wikitext=" + encodeURIComponent(s),
                callback);
        },
        //These are for the iodiode mimic
        addOutputRenderer: function(renderer) {
            this.getNotebook().addOutputRenderer(renderer);
        },
        addOutputHandler: function(renderer) {
            this.getNotebook().addOutputRenderer(renderer);
        },
        output: {
            text: function(t) {
                notebook.write(t);
            },
            element: function(tag) {
                var id = HtmlUtils.getUniqueId();
                notebook.write(HtmlUtils.tag(tag, ["id", id]));
                return document.getElementById(id);
            }
        },
        clearOutput: function() {
            this.cell.clearOutput();
        },
        clearAllOutput: function() {
            this.getNotebook().clearOutput();
        },
        write: function(value, clear) {
            if (!value) return;
            var s = this.getNotebook().formatOutput(value);
            if (s == null && (typeof value) == "object") {
                s = this.notebook.formatObject(value);
            }
            if (clear)
                this.div.set(s);
            else
                this.div.append(s);
        },
        linechart: async function(entry, props) {
            if (!entry)
                await this.cell.getCurrentEntry(e => entry = e);
            this.cell.createDisplay(this, entry, DISPLAY_LINECHART, props);
        },
    });
}


var notebookStates = {};

function RamaddaNotebookCell(notebook, id, content, props) {
    this.notebook = notebook;

    var ID_CELL = "cell";
    var ID_HEADER = "header";
    var ID_CELLNAME = "cellname";
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
    var ID_SHOWEDIT = "showedit";
    var ID_RUN_ON_LOAD = "runonload";
    var ID_DISPLAY_MODE = "displaymode";
    var ID_LAYOUT_TYPE = "layouttype";
    var ID_SHOWCONSOLE = "showconsole";
    var ID_LAYOUT_COLUMNS = "layoutcolumns";
    var ID_RUNFIRST = "runfirst";
    var ID_SHOW_OUTPUT = "showoutput";
    var ID_RUN_ICON = "runningicon";

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
        cellName: "",
        runFirst: false,
        showOutput: true,
    });

    if (props) {
        $.extend(this, props);
    }
    RamaddaUtil.defineMembers(this, {
        getJson: function(output) {
            var obj = {
                id: this.id,
                inputRows: this.inputRows,
                content: this.getInputText(),
                showInput: this.showInput,
                showHeader: this.showHeader,
                runFirst: this.runFirst,
                showOutput: this.showOutput,
                cellName: this.cellName,
            };
            if (this.currentEntry)
                obj.currentEntryId = this.currentEntry.getId();
            if (output)
                obj.outputHtml = this.outputHtml;
            return obj;
        },
        createCell: function() {
            if (this.content == null) {
                this.content = "%% wiki";
            }
            this.editId = addHandler(this);
            addHandler(this, this.editId + "_entryid");
            addHandler(this, this.editId + "_wikilink");
            var _this = this;
            var buttons =
                this.makeButton(ID_BUTTON_MENU, icon_menu, "Show menu", "showmenu") +
                this.makeButton(ID_BUTTON_RUN, Utils.getIcon("run.png"), "Run this cell", "run") +
                this.makeButton(ID_BUTTON_RUN, Utils.getIcon("runall.png"), "Run all", "runall");

            var runIcon = HtmlUtils.image(icon_blank, ["align", "right", "id", this.getDomId(ID_RUN_ICON), "style", "padding-bottom:2px;padding-top:2px;padding-right:5px;"]);
            buttons = buttons + "&nbsp;" + HtmlUtils.span(["id", this.getDomId(ID_CELLNAME)], this.cellName);
            buttons += runIcon;
            var header = HtmlUtils.div([ATTR_CLASS, "display-notebook-header", ATTR_ID, this.getDomId(ID_HEADER), "tabindex", "0", "title", "Click to toggle input\nShift-click to clear output"], "&nbsp;" + buttons);

            //Strip out the meta chunks
            var content = "";
            var lines = this.content.split("\n");
            var inMeta = false;
            for (var i = 0; i < lines.length; i++) {
                var line = lines[i];
                var _line = line.trim();
                if (_line.startsWith("%%")) {
                    if (_line.match(/^%% *meta/)) {
                        inMeta = true;
                    } else {
                        inMeta = false;
                    }
                }
                if (!inMeta) {
                    content += line + "\n";
                }
            }


            content = content.replace(/</g, "&lt;").replace(/>/g, "&gt;");
            var input = HtmlUtils.div([ATTR_CLASS, "display-notebook-input ace_editor", ATTR_ID, this.getDomId(ID_INPUT), "title", "shift-return: run chunk\nctrl-return: run to end"], content);
            var inputToolbar = HtmlUtils.div(["id", this.getDomId(ID_INPUT_TOOLBAR)], "");

            input = HtmlUtils.div(["class", "display-notebook-input-container"], inputToolbar + input);
            var output = HtmlUtils.div([ATTR_CLASS, "display-notebook-output", ATTR_ID, this.getDomId(ID_OUTPUT)], this.outputHtml);
            output = HtmlUtils.div(["class", "display-notebook-output-container"], output);
            var menu = HtmlUtils.div(["id", this.getDomId(ID_MENU), "class", "ramadda-popup"], "");
            var html = header + input;
            html = HtmlUtils.div(["id", this.getDomId(ID_CELL)], html);
            $("#" + this.id + "_cellinput").html(html);
            $("#" + this.id + "_celloutput").html(output);
            var url = ramaddaBaseUrl + "/wikitoolbar?doImports=false&entryid=" + this.entryId + "&handler=" + this.editId;
            url += "&extrahelp=" + ramaddaBaseUrl + "/userguide/notebook.html|Notebook Help";
            GuiUtils.loadHtml(url, h => {
                this.inputToolbar = h;
                this.jq(ID_INPUT_TOOLBAR).html(h);
                $("#" + this.editId + "_prefix").html(HtmlUtils.span(["id", this.getDomId("toolbar_notebook"),
                    "style", "border-right:1px #ccc solid;",
                    "class", "ramadda-menubar-button"
                ], "Notebook"));
                this.jq("toolbar_notebook").click(() => this.showNotebookMenu());

            });
            this.header = this.jq(ID_HEADER);
            this.header.click((e) => {
                if (e.shiftKey)
                    this.processCommand("clear");
                else {
                    this.hidePopup();
                    this.processCommand("toggle");
                }

            });

            let wikiEditor = new WikiEditor("", "", this.getDomId(ID_INPUT), false, {
                maxLines: 30,
                minLines: 5
            });
	    this.editor = wikiEditor.getEditor();
            this.editor.getSession().on('change', () => {
                this.inputChanged();
            });
            this.menuButton = this.jq(ID_BUTTON_MENU);
            this.toggleButton = this.jq(ID_BUTTON_TOGGLE);
            this.cell = this.jq(ID_CELL);
            this.input = this.jq(ID_INPUT);
            this.output = this.jq(ID_OUTPUT);
            this.inputContainer = this.cell.find(".display-notebook-input-container");
            this.inputMenu = this.cell.find(".display-notebook-input-container");
            this.applyStyle();
            this.header.find(".display-notebook-menu-button").click(function(e) {
                _this.processCommand($(this).attr("what"));
                e.stopPropagation();
            });

            this.calculateInputHeight();
            this.input.focus(() => this.hidePopup());
            this.input.click(() => this.hidePopup());
            this.output.click(() => this.hidePopup());
            this.input.on('input selectionchange propertychange', () => this.calculateInputHeight());
            var moveFunc = (e) => {
                var key = e.key;
                if (key == 'v' && e.ctrlKey) {
                    this.notebook.moveCellDown(_this);
                    return;
                }
                if (key == 6 && e.ctrlKey) {
                    this.notebook.moveCellUp(_this);
                    return;
                }

            };
            this.input.keydown(moveFunc);
            this.header.keydown(moveFunc);
            this.input.keydown(function(e) {
                var key = e.key;
                if (key == 's' && e.ctrlKey) {
                    _this.notebook.saveNotebook(false);
                    return;
                }
                if (key == 'Enter') {
                    //                    console.log(key +"  shift:"  + e.shiftKey +" ctrl:" + e.ctrlKey);
                    if (e.shiftKey || e.ctrlKey) {
                        if (e.preventDefault) {
                            e.preventDefault();
                        }
                        if (e.shiftKey && e.ctrlKey) {
                            //run all
                            _this.run(null);
                        } else {
                            //run current, run to end
                            _this.run(null, {
                                justCurrent: true,
                                toEnd: e.ctrlKey
                            });
                            if (!e.ctrlKey) {
                                _this.stepToNextChunk();
                            }
                        }
                    }
                }

            });
        },
        selectClick(type, id, entryId, value) {
            if (type == "entryid") {
                this.insertText(entryId);
            } else {
                this.insertText("[[" + entryId + "|" + value + "]]");
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
            WikiUtil.insertAtCursor(id, textComp.obj, value);
            this.calculateInputHeight();
        },
        showNotebookMenu: function() {
            var link = this.jq("toolbar_notebook");
            this.makeMenu(link, "left bottom");
        },
        makeButton: function(id, icon, title, command) {
            if (!command) command = "noop";
            return HtmlUtils.div(["what", command, "title", title, "class", "display-notebook-menu-button", "id", this.getDomId(id)], HtmlUtils.image(icon, []));
        },
        makeMenu: function(src, at) {
            if (!src) {
                src = this.input;
            }
            if (!src.is(":visible")) {
                src = this.header;
            }
            if (!src.is(":visible")) {
                src = this.output;
            }
            if (!at) at = "left top";
            let _this = this;
            var space = "&nbsp;&nbsp;";
            var line = "<div style='border-top:1px #ccc solid;margin-top:4px;margin-bottom:4px;'></div>"
            var menu = "";
            menu += HtmlUtils.input(ID_CELLNAME_INPUT, _this.cellName, ["placeholder", "Cell name", "style", "width:100%;", "id", _this.getDomId(ID_CELLNAME_INPUT)]);
            menu += "<br>";
            menu += "<table  width=100%> ";
            menu += "<tr><td align=right><b>New cell:</b>&nbsp;</td><td>";
            menu += HtmlUtils.div(["class", "ramadda-link", "what", "newabove"], "Above") + space;
            menu += HtmlUtils.div(["class", "ramadda-link", "what", "newbelow"], "Below");
            menu += "</td></tr>"
            menu += "<tr><td align=right><b>Move:</b>&nbsp;</td><td>";
            menu += HtmlUtils.div(["title", "ctrl-^", "class", "ramadda-link", "what", "moveup"], "Up") + space;
            menu += HtmlUtils.div(["title", "ctrl-v", "class", "ramadda-link", "what", "movedown"], "Down");
            menu += "</td></tr>"

            menu += "</table>";

            menu += line;
            menu += HtmlUtils.div(["title", "ctrl-return", "class", "ramadda-link", "what", "hideall"], "Hide all inputs");
            menu += "<br>"
            menu += HtmlUtils.div(["class", "ramadda-link", "what", "clearall"], "Clear all outputs");
            menu += "<br>";
            var cols = this.notebook.columns;
            var colId = _this.getDomId(ID_LAYOUT_COLUMNS);
            menu += "<b>Layout:</b> ";
            menu += HtmlUtils.checkbox(_this.getDomId(ID_LAYOUT_TYPE), [], _this.notebook.layout == "horizontal") + " Horizontal" + "<br>";
            //            menu += "Columns: ";
            //            menu += HtmlUtils.input(colId, this.notebook.columns, ["size", "3", "id", _this.getDomId(ID_LAYOUT_COLUMNS)]);
            menu += line;

            menu += HtmlUtils.checkbox(_this.getDomId(ID_SHOW_OUTPUT), [], _this.showOutput) + " Output enabled" + "<br>";
            menu += HtmlUtils.checkbox(_this.getDomId(ID_SHOWCONSOLE), [], _this.notebook.showConsole) + " Show console" + "<br>";

            menu += HtmlUtils.checkbox(_this.getDomId(ID_RUNFIRST), [], _this.runFirst) + " Run first" + "<br>";
            menu += HtmlUtils.checkbox(_this.getDomId(ID_RUN_ON_LOAD), [], _this.notebook.runOnLoad) + " Run on load" + "<br>";
            menu += HtmlUtils.div(["title", "Don't show the left side and input for anonymous users"], HtmlUtils.checkbox(_this.getDomId(ID_DISPLAY_MODE), [], _this.notebook.displayMode) + " Display mode" + "<br>");

            menu += line;
            menu += HtmlUtils.div(["class", "ramadda-link", "what", "savewithout"], "Save notebook") + "<br>";
            menu += line;
            menu += HtmlUtils.div(["class", "ramadda-link", "what", "delete"], "Delete cell") + "<br>";
            menu += HtmlUtils.div(["class", "ramadda-link", "what", "help"], "Help") + "<br>";
            menu = HtmlUtils.div(["class", "display-notebook-menu"], menu);


            var popup = this.getPopup();
            this.dialogShown = true;
            popup.html(HtmlUtils.div(["class", "ramadda-popup-inner"], menu));
            popup.show();
            popup.position({
                of: src,
                my: "left top",
                at: at,
                collision: "fit fit"
            });
            _this.jq(ID_SHOWHEADER_INPUT).focus();

            _this.jq(ID_SHOWCONSOLE).change(function(e) {
                _this.notebook.showConsole = _this.jq(ID_SHOWCONSOLE).is(':checked');
                _this.hidePopup();
                _this.notebook.layoutCells();
            });


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
            _this.jq(ID_SHOWEDIT).change(function(e) {
                _this.showInput = _this.jq(ID_SHOWEDIT).is(':checked');
                _this.applyStyle();
            });

            _this.jq(ID_LAYOUT_TYPE).change(function(e) {
                if (_this.jq(ID_LAYOUT_TYPE).is(':checked')) {
                    _this.notebook.layout = "horizontal";
                } else {
                    _this.notebook.layout = "vertical";
                }
                _this.hidePopup();
                _this.notebook.layoutCells();
            });
            _this.jq(ID_LAYOUT_COLUMNS).keypress(function(e) {
                var keyCode = e.keyCode || e.which;
                if (keyCode != 13) {
                    return;
                }
                var cols = parseInt(_this.jq(ID_LAYOUT_COLUMNS).val());
                if (isNaN(cols)) {
                    _this.jq(ID_LAYOUT_COLUMNS).val("bad:" + _this.jq(ID_LAYOUT_COLUMNS).val());
                    return;
                }
                _this.hidePopup();
            });
            _this.jq(ID_CELLNAME_INPUT).keypress(function(e) {
                var keyCode = e.keyCode || e.which;
                if (keyCode == 13) {
                    _this.hidePopup();
                    return;
                }
            });
            popup.find(".ramadda-link").click(function() {
                var what = $(this).attr("what");
                _this.processCommand(what);
            });
        },
        hidePopup: function() {
            var popup = this.getPopup();
            if (popup && this.dialogShown) {
                var cols = parseInt(this.jq(ID_LAYOUT_COLUMNS).val());
                this.cellName = this.jq(ID_CELLNAME_INPUT).val();
                this.jq(ID_CELLNAME).html(this.cellName);
                popup.hide();
                this.applyStyle();

                if (!isNaN(cols) && this.notebook.columns != cols) {
                    this.notebook.columns = cols;
                    this.notebook.layoutCells();
                }
            }
            this.dialogShown = false;
        },
        processCommand: function(command) {
            if (command == "showmenu") {
                this.makeMenu();
                return;
            } else if (command == "toggle") {
                this.showInput = !this.showInput;
                this.applyStyle(true);
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
            } else if (command == "run") {
                this.notebook.runCell(this);
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
                this.notebook.saveNotebook(true);
            } else if (command == "savewithout") {
                this.notebook.saveNotebook(false);
            } else if (command == "help") {
                var win = window.open(ramaddaBaseUrl + "/userguide/notebook.html", '_blank');
                win.focus();
            } else if (command == "delete") {
                this.askDelete();
                return;
            } else {
                console.log("unknown command:" + command);
            }
            this.hidePopup();
        },
        shouldShowInput: function() {
            return this.showInput && this.notebook.showInput();
        },
        applyStyle: function(fromUser) {
            if (this.shouldShowInput()) {
                this.jq(ID_INPUT_TOOLBAR).css("display", "block");
                this.inputContainer.show(400, () => this.editor.resize());
                this.showHeader = true;
            } else {
                this.jq(ID_INPUT_TOOLBAR).css("display", "none");
                this.inputContainer.hide(fromUser ? 200 : 0);
                this.showHeader = false;
            }
            this.showHeader = this.notebook.showInput();
            if (this.showHeader) {
                this.header.css("display", "block");
            } else {
                this.header.css("display", "none");
            }
            if (this.showOutput) {
                this.output.css("display", "block");
            } else {
                this.output.css("display", "none");
            }
        },
        getPopup: function() {
            return this.notebook.getPopup();
        },
        askDelete: function() {
            let _this = this;
            var menu = "";
            menu += "Are you sure you want to delete this cell?<br>";
            menu += HtmlUtils.span(["class", "ramadda-link", "what", "yes"], "Yes");
            menu += HtmlUtils.span(["style", "margin-left:50px;", "class", "ramadda-link", "what", "cancel"], "No");
            var popup = this.getPopup();

            popup.html(HtmlUtils.div(["class", "ramadda-popup-inner"], menu));
            popup.show();
            var src = this.input;
            if (!src.is(":visible")) {
                src = this.output;
            }
            if (!src.is(":visible")) {
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
                _this.hidePopup();
                if (what == "yes") {
                    _this.notebook.deleteCell(_this);
                }
            });
        },
        inputChanged: function() {
            var value = this.getInputText();
            var lines = value.split("\n");
            var cursor = this.editor.getCursorPosition();
            for (var i = cursor.row; i >= 0; i--) {
                var line = lines[i].trim();
                if (line.startsWith("%%")) {
                    var type = line.substring(2).trim();
                    if (type.startsWith("md") || type.startsWith("html") || type.startsWith("css") || type.startsWith("raw")) {
                        var doRows = {};
                        doRows[i] = true;
                        this.runInner(value, doRows);
                    }
                    break;
                }
            }
        },
        stepToNextChunk: function() {
            var value = this.getInputText();
            var lines = value.split("\n");
            var cursor = this.editor.getCursorPosition();
            for (var i = cursor.row + 1; i < lines.length; i++) {
                if (lines[i].trim().startsWith("%%")) {
                    var ll = lines[i].length;
                    this.editor.selection.moveTo(i, ll);
                    this.editor.scrollToLine(i, true, true, function() {});
                    break;
                }
            }

        },
        run: async function(callback, args) {
            if (!args) args = {};
            var justCurrent = args.justCurrent;
            var toEnd = args.toEnd;
            var doingAll = args.doingAll;
            if (this.running) return Utils.call(callback, true);
            this.running = true;
            var doRows = null;
            try {
                var ok = true;
                var value = this.getInputText();
                if (justCurrent) {
                    doRows = {};
                    var cursor = this.editor.getCursorPosition();
                    var row = cursor.row;
                    var lines = value.split("\n");
                    var percentCnt = 0;
                    if (toEnd) {
                        justCurrent = false;
                        while (row >= 0) {
                            if (lines[row].trim().startsWith("%%")) {
                                break;
                            }
                            row--;
                        }
                        if (row < 0) row = 0;
                        while (row < lines.length) {
                            doRows[row] = true;
                            row++;
                        }
                    } else {
                        //go to the next chunk
                        row++;
                        while (row < lines.length) {
                            if (lines[row].trim().startsWith("%%")) {
                                row--;
                                break;
                            }
                            row++;
                        }
                        if (row >= lines.length) row = lines.length - 1;
                        while (row >= 0) {
                            var line = lines[row].trim();
                            doRows[row] = true;
                            if (line.startsWith("%%")) break;
                            row--;
                        }
                    }
                }

                this.jq(ID_RUN_ICON).attr("src", icon_progress);
                await this.runInner(value, doRows, doingAll).then(r => ok = r);
                this.jq(ID_RUN_ICON).attr("src", icon_blank);
                if (!ok) {
                    this.running = false;
                    return Utils.call(callback, false);
                }
                this.outputUpdated();
            } catch (e) {
                this.jq(ID_RUN_ICON).attr("src", icon_blank);
                this.running = false;
                this.writeOutput("An error occurred:" + e.toString() + " " + (typeof e));
                console.log("error:" + e.toString());
                if (e.stack)
                    console.log(e.stack);
                return Utils.call(callback, false);
            }
            this.running = false;
            return Utils.call(callback, true);
        },
        prepareToLayout: function() {
            this.content = this.getInputText();
        },
        getInputText: function() {
            if (!this.editor) return this.content;
            return this.editor.getValue();
        },
        globalChanged: async function(name, value) {
            for(var i=0;i<this.chunks.length;i++) {
                var chunk = this.chunks[i];
                if(chunk.hasRun) continue;
                if(chunk.depends.includes(name)) {
                   var ok = true;
                   await this.runChunk(chunk,r=>ok=r);
                   if(!ok) break;
                }
            }
        },
        prepareToRun: function() {
            this.hasRun = false;
            if(this.chunks) {
                this.chunks.forEach(chunk=>chunk.hasRun = false);
            }
        },
        runInner: async function(value, doRows, doingAll) {
            value = value.trim();
            value = value.replace(/{cellname}/g, this.cellName);
            value = this.notebook.convertInput(value);
            if (!this.chunks) this.chunks = [];
            var chunks = this.chunks;
            var type = "wiki";
            var rest = "";
            var commands = value.split("\n");
            var prevChunk = null;
            var chunkCnt = 0;
            var _cell = this;
            var getChunk = (cell,type, content,  doChunk, rest) => {
                var props = Utils.parseAttributes(rest);
                props.type = type;
                props.doChunk = doChunk;
                props.content   = content;
                var chunk = (chunkCnt < chunks.length ? chunks[chunkCnt] : null);
                chunkCnt++;
                if (chunk) {
                    if (chunk.div.jq().length == 0) {
                        chunk = null;
                    } else {}
                } else {}
                if (!chunk) {
                    chunk = new NotebookChunk(cell, props);
                    chunks.push(chunk);
                    if(!chunk.skipOutput) {
                        if (prevChunk) prevChunk.div.jq().after(chunk.div.toString());
                        else cell.output.html(chunk.div.toString());
                    }
                } else {
                    chunk.initChunk(props);
                }
                prevChunk = chunk;
                chunk.div.jq().show();
                return chunk;
            };
            var content = "";
            var doChunk = true;
            for (var rowIdx = 0; rowIdx < commands.length; rowIdx++) {
                var command = commands[rowIdx];
                var _command = command.trim();
                if (_command.startsWith("//")) continue;
                if (_command.startsWith("%%")) {
                    var newRest = _command.substring(2).trim();
                    var newType;
                    var index = newRest.indexOf(" ");
                    if (index < 0) {
                        newType = newRest;
                        newRest = "";
                    } else {
                        newType = newRest.substring(0, index).trim();
                        newRest = newRest.substring(index);
                    }
                    if (content != "") {
                        getChunk(this, type, content, doChunk, rest);
                    }
                    doChunk = doRows ? doRows[rowIdx] : true;
                
                    content = "";
                    if (content != "") content += "\n";
                    if (newType != "")
                        type = newType;
                    rest = newRest;
                    continue;
                }
                content = content + command + "\n";
            }

            if (content != "") {
                getChunk(this,type, content, doChunk, rest);
            }

            this.chunkMap = {};
            for (var i = 0; i < this.chunks.length; i++) {
                var chunk = this.chunks[i];
                if (chunk.name) {
                    this.chunkMap[chunk.name] = chunk;
                }
            }
            for (var i = chunkCnt; i < this.chunks.length; i++) {
                this.chunks[i].div.jq().hide();
            }
            this.rawOutput = "";
            var ok = true;
            await this.runChunks(this.chunks, doingAll, true, r => ok = r);
            if (!ok) return false;
            await this.runChunks(this.chunks, doingAll, false, r => ok = r);
            if (!ok) return false;
            Utils.initContent("#" + this.getDomId(ID_OUTPUT));
            return true;
        },
        runChunks: async function(chunks, doingAll, justFirst, callback) {
            for (var i = 0; i < chunks.length; i++) {
                var chunk = chunks[i];
                var ok = true;
                if (justFirst === true && !chunk.props["runfirst"]) {
                    continue;
                }
                if (justFirst === false && chunk.props["runfirst"] === true) {
                    continue;
                }
                if (doingAll && chunk.props["skiprunall"] === true) {
                    continue;
                }
                if (!chunk.doChunk) {
                    continue;
                }
                await this.runChunk(chunk, (r => ok = r));
                if (!ok) return Utils.call(callback, false);
            }
            return Utils.call(callback, true);
        },
        runChunk: async function(chunk,   callback) {
            if (chunk.hasRun) {
                //                console.log("runChunk: chunk has run");
                return Utils.call(callback, true);
            }
            chunk.ok = true;
            chunk.div.set("");
            chunk.hasRun = true;
            for (var i = 0; i < chunk.depends.length; i++) {
                var name = chunk.depends[i];
                if (this.chunkMap[name] && !this.chunkMap[name].hasRun) {
                    var ok = true;
                    var otherChunk = this.chunkMap[name];
                    await this.runChunk(otherChunk, false, null, (r => ok = r));
                    if (!ok || !otherChunk.ok) {
                        return Utils.call(callback, false);
                    }
                }
            }
            await this.processChunk(chunk);
            if (!chunk.ok) {
                Utils.call(callback, false);
                return;
            }
            if (chunk.name && (typeof chunk.name == "string")) {
                var name = chunk.name.trim();
                if (chunk.output) {
                    if (name != "") {
                        await this.notebook.addGlobal(name, chunk.output);
                    }
                } else {
                    await this.notebook.addGlobal(name, null);
                }
            }
            return Utils.call(callback, true);
        },

        writeOutput: function(h) {
            if (!this.output) {
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
            return this.rawOutput;
        },
        focus: function() {
            this.input.focus();
        },
        clearOutput: function() {
            if (this.chunks)
                this.chunks.forEach(chunk => chunk.div.set(""));
            this.outputHtml = "";
        },
        processHtml: async function(chunk) {
            var content = chunk.getContent();
            if (content.match("%\n*$")) {
                content = content.trim();
                content = content.substring(0, content.length - 1);
            }
            this.rawOutput += content + "\n";
            chunk.output = content;
            chunk.div.set(content);
        },
        processCss: async function(chunk) {
            var css = HtmlUtils.tag("style", ["type", "text/css"], chunk.getContent());
            this.rawOutput += css + "\n";
            chunk.output = css;
            chunk.div.set(css);
        },
        handleError: function(chunk, error, from) {
            chunk.ok = false;
            console.log("An error occurred:" + error);
            this.notebook.log(error, "error", from, chunk.div);
        },
        getFetchUrl: async function(url, type, callback) {
            //Check for entry id
            url = Utils.replaceRoot(url);

            if (url.match(/^[a-z0-9]+-[a-z0-9].*/)) {
                return Utils.call(callback, ramaddaBaseUrl + "/entry/get?entryid=" + url);
            } else {
                if (!url.startsWith("http")) {
                    if ((url.startsWith("/") && !url.startsWith(ramaddaBaseUrl)) || url.startsWith("..") || !url.startsWith("/")) {
                        var entry;
                        await this.getEntryFromPath(url, e => entry = e);
                        if (!entry) {
                            return Utils.call(callback, null);
                        }
                        return Utils.call(callback, ramaddaBaseUrl + "/entry/get?entryid=" + entry.getId());
                    }
                }
                return Utils.call(callback, url);
            }
        },
        processFetch: async function(chunk) {
            var lines = chunk.getContent().split("\n");
            for (var i = 0; i < lines.length; i++) {
                var line = lines[i].trim();
                if (line == "") continue;
                var origLine = line;
                var error = null;
                var msgExtra = "";
                var idx = line.indexOf(":");
                if (idx < 0) {
                    this.handleError(chunk, "Bad fetch line:" + line, "io");
                    return;
                }
                var tag = line.substring(0, idx);
                line = line.substring(idx + 1).trim();
                var idx = line.indexOf(" //");
                if (idx >= 0) {
                    line = line.substring(0, idx).trim();
                }


                var url = null;
                var variable = null;
                if (["text", "json", "blob"].includes(tag)) {
                    var args = line.match(/^([a-zA-Z0-9_]+) *= *(.*)$/);
                    if (args) {
                        variable = args[1];
                        line = args[2].trim();
                        msgExtra = " (var " + variable + ")";
                    }
                }

                await this.getFetchUrl(line, tag, u => url = u);
                if (!url) {
                    this.handleError(chunk, "Unable to get entry url:" + line, "io");
                    return;
                }

                if (tag == "js") {
                    //Don't import jquery
                    if (url.match("jquery-.*\\.js")) return;
                    await Utils.importJS(url,
                        () => {},
                        (jqxhr, settings, exception) => {
                            error = "Error fetching " + origLine + " " + (exception ? exception.toString() : "");
                        },
                        //Check the cache
                        false
                    );
                } else if (tag == "css") {
                    await Utils.importCSS(url,
                        null,
                        (jqxhr, settings, exception) => error = "Error fetching " + origLine + " " + exception, true);
                } else if (tag == "html") {
                    await Utils.doFetch(url, h => chunk.div.append(h), (jqxhr, settings, exception) => error = "Error fetching " + origLine + " " + exception);
                } else if (tag == "text" || tag == "json" || tag == "blob") {
                    var isJson = tag == "json";
                    var isBlob = tag == "blob";
                    var results = null;
                    await Utils.doFetch(url, h => results = h, (jqxhr, settings, err) => error = "Error fetching " + origLine + " error:" + (err ? err.toString() : ""), tag == "blob" ? "blob" : "text");
                    if (results) {
                        if (isJson) {
                            if (typeof results == "string")
                                results = JSON.parse(results);
                        } else if (isBlob) {
                            results = new Blob([results], {});
                        }
                        if (variable) {
                            await this.notebook.addGlobal(variable, results);
                        } else {
                            if (isJson) {
                                chunk.div.append(Utils.formatJson(results));
                            } else {
                                chunk.div.append(HtmlUtils.pre(["style", "max-width:100%;overflow-x:auto;"], results));
                            }
                        }
                    }
                } else {
                    error = "Unknown fetch:" + origLine;
                }
                if (error) {
                    this.handleError(chunk, error, "io");
                    return;
                } else {
                    this.notebook.log("Loaded: " + url + msgExtra, "output", "io");
                }
            }
        },
        processMd: async function(chunk) {
            //            await Utils.importJS(ramaddaBaseUrl + "/lib/katex/lib/katex/katex.min.css");
            //            await Utils.importJS(ramaddaBaseUrl + "/lib/katex/lib/katex/katex.min.js");

            var content = chunk.getContent();
            this.rawOutput += content + "\n";
            if (content.match("%\n*$")) {
                content = content.trim();
                content = content.substring(0, content.length - 1);
            }
            var o = "";
            var tex = null;
            var lines = content.split("\n");
            var texs = [];
            for (var i = 0; i < lines.length; i++) {
                var line = lines[i];
                var _line = line.trim();
                if (_line.startsWith("$$")) {
                    if (tex != null) {
                        try {
                            var html = katex.renderToString(tex, {
                                throwOnError: true
                            });
                            o += "tex:" + texs.length + ":\n";
                            texs.push(html);
                        } catch (e) {
                            o += "Error parsing tex:" + e + "<pre>" + tex + "</pre>";
                        }
                        tex = null;
                    } else {
                        tex = "";
                    }
                } else if (tex != null) {
                    tex += line + "\n";
                } else {
                    o += line + "\n";
                }
            }

            var converter = new showdown.Converter();
            var html = converter.makeHtml(o);
            for (var i = 0; i < texs.length; i++) {
                html = html.replace("tex:" + i + ":", texs[i]);
            }
            var md = HtmlUtils.div(["class", "display-notebook-md"], html);
            chunk.output = html;
            chunk.div.set(md);
        },
        processPy: async function(chunk) {
            if (!this.notebook.loadedPyodide) {
                chunk.div.set("Loading Python...");
                await Utils.importJS(ramaddaBaseHtdocs + "/lib/pyodide/pyodide.js");
                await languagePluginLoader.then(() => {
                    pyodide.runPython('import sys\nsys.version;');
                    //                        pyodide.runPython('print ("hello python")');
                }, (e) => console.log("error:" + e));
                await pyodide.loadPackage(['numpy', 'cycler', 'pytz', 'matplotlib'])
                chunk.div.set("");
                this.notebook.loadedPyodide = true;
            }

            pyodide.runPython(chunk.getContent());
        },
        processPlugin: async function(chunk) {
            var plugin = JSON.parse(chunk.getContent());
            await this.notebook.addPlugin(plugin, chunk);
        },
        processWiki: async function(chunk) {
            this.rawOutput += chunk.getContent() + "\n";
            var id = this.notebook.getProperty("entryId", "");
            await this.getCurrentEntry(e => entry = e);
            if (entry) id = entry.getId();
            let _this = this;
            let divId = HtmlUtils.getUniqueId();
            var wikiCallback = function(html) {
                var h = HtmlUtils.div(["id", divId, "style"], html);
                chunk.div.set(h);
                chunk.output = h;
            }
            var wiki = "{{group showMenu=false}}\n" + chunk.getContent();
            await GuiUtils.loadHtml(ramaddaBaseUrl + "/wikify?doImports=false&entryid=" + id + "&wikitext=" + encodeURIComponent(chunk.getContent()),
                wikiCallback);
        },
        processSh: async function(chunk) {
            var r = "";
            var lines = chunk.getContent().split("\n");
            var commands = [];
            for (var i = 0; i < lines.length; i++) {
                var fullLine = lines[i].trim();
                if (fullLine == "") continue;
                var cmds = fullLine.split(";");
                for (var cmdIdx = 0; cmdIdx < cmds.length; cmdIdx++) {
                    var line = cmds[cmdIdx].trim();
                    if (line == "" || line.startsWith("#") || line.startsWith("//")) continue;
                    var toks = line.split(" ");

                    var command = toks[0].trim();
                    var proc = null;
                    var extra = null;
                    if (this["processCommand_" + command]) {
                        proc = this["processCommand_" + command];
                    } else {
                        proc = this.processCommand_help;
                        extra = "Unknown command: <i>" + command + "</i>";
                    }
                    var div = new Div("");
                    commands.push({
                        proc: proc,
                        line: line,
                        toks: toks,
                        extra: extra,
                        div: div
                    });
                    r += div.set("");
                }
            }
            let _this = this;
            chunk.div.set(r);
            var i = 0;
            for (i = 0; i < commands.length; i++) {
                var cmd = commands[i];
                if (cmd.extra) {
                    cmd.div.append(extra);
                }
                await cmd.proc.call(_this, cmd.line, cmd.toks, cmd.div, cmd.extra);
            }
        },
        processJs: async function(chunk,state) {
            var lines;
            var topLines = 0;
            await this.getCurrentEntry(e => {
                    current = e
                });
            if(!notebookStates[state.id]) {
                throw new Error("Null NB:" + state.id);
            }
            try {
                var notebookEntries = this.notebook.getCurrentEntries();
                for (name in notebookEntries) {
                    state.entries[name] = notebookEntries[name].entry;
                }
                var jsSet = "";
                state.entries["current"] = current;
                state.entries["parent"] = this.parentEntry;
                state.entries["base"] = this.notebook.getBaseEntry();
                state.entries["root"] = this.notebook.getRootEntry();

                var stateJS = "notebookStates['" + state.id + "']";
                topLines++;
                jsSet += "var notebook= " + stateJS + ";\n";
                topLines++;
                for (name in state.entries) {
                    var e = state.entries[name];
                    topLines++;
                    jsSet += "var " + name + "= notebook.entries['" + name + "'];\n"
                }
                for (name in this.notebook.cellValues) {
                    var clean = name.replace(/ /g, "_").replace(/[^a-zA-Z0-9_]+/g, "_");
                    topLines++;
                    jsSet += "var " + clean + "= notebook.getNotebook().cellValues['" + name + "'];\n";
                }
                for (name in this.notebook.globals) {
                    name = name.trim();
                    if (name == "") continue;
                    //                    if (!Utils.isDefined(window[name])) {
                        topLines++;
                        jsSet += "var " + name + "= notebook.getNotebook().getGlobalValue('" + name + "');\n";
                        //                    }
                }
                var js = chunk.getContent().trim();
                lines = js.split("\n");
                js = jsSet + "\n" + js;
                var result = eval.call(null, js);
                if (state.getStop()) {
                    chunk.ok = false;
                }
                var html = "";
                if (result != null) {
                    chunk.output = result;
                    var rendered = this.notebook.formatOutput(result);
                    if (rendered != null) {
                        html = rendered;
                        this.rawOutput += html + "\n";
                    } else {
                        var type = typeof result;
                        if (type != "object" && type != "function") {
                            html = result;
                            this.rawOutput += html + "\n";
                        }
                    }
                }
                chunk.div.append(html);
            } catch (e) {
                chunk.ok = false;
                var line = lines[e.lineNumber - topLines - 1];
                console.log("Error:" + e.stack);
                this.notebook.log("Error: " + e.message + "<br>&gt;" + (line ? line : ""), "error", "js", chunk.div);
            }
        },
        processChunk: async function(chunk) {
            var state = new NotebookState(this, chunk.div);
            window.notebook = state;
            notebookStates[state.id] = state;
            if (chunk.type == "html") {
                await this.processHtml(chunk, state);
            } else if (chunk.type == "plugin") {
                await this.processPlugin(chunk,state);
            } else if (chunk.type == "wiki") {
                await this.processWiki(chunk,state);
            } else if (chunk.type == "css") {
                await this.processCss(chunk,state);
            } else if (chunk.type == "fetch") {
                await this.processFetch(chunk,state);
            } else if (chunk.type == "raw") {
                var content = chunk.getContent();
                chunk.output = content;
                this.rawOutput += content;
            } else if (chunk.type == "js") {
                await this.processJs(chunk,state);
            } else if (chunk.type == "sh") {
                await this.processSh(chunk,state);
            } else if (chunk.type == "meta") {
                //noop
            } else if (chunk.type == "md") {
                await this.processMd(chunk,state);
            } else if (chunk.type == "py") {
                await this.processPy(chunk,state);
            } else {
                var hasPlugin;
                await this.notebook.hasPlugin(chunk.type, p => hasPlugin = p);
                if (hasPlugin) {
                    chunk.div.set("");
                    var result;
                    await this.notebook.processChunkWithPlugin(chunk.type, chunk, r => result = r);
                    //TODO: what to do with the result
                    if (result) {
                        this.notebook.processPluginOutput(chunk.type, chunk, result);
                    }
                    return;
                }
                this.notebook.log("Unknown type:" + chunk.type, "error", null, chunk.div);
                chunk.ok = false;
            }
            delete  notebookStates[state.id];
            if (state.getStop()) {
                chunk.ok = false;
            }

        },



        calculateInputHeight: function() {
            this.content = this.getInputText();
            if (!this.content) return;
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
            this.currentInput = this.getInputText();
        },
        writeError: function(msg) {
            this.writeStatusMessage(msg);
            //                this.writeResult(msg);
        },
        header: function(msg) {
            return HtmlUtils.div([ATTR_CLASS, "display-notebook-header"], msg);
        },
        processCommand_help: function(line, toks, div, callback, prefix) {
            if (div == null) div = new Div();
            var help = "";
            if (prefix != null) help += prefix;
            help += "<pre>pwd, ls, cd</pre>";
            return div.append(help);
        },
        entries: {},

        selectEntry: function(entryId) {
            var cnt = 1;
            var entries = this.notebook.getCurrentEntries();
            while (entries["entry" + cnt]) {
                cnt++;
            }
            var id = prompt("Set an ID", "entry" + cnt);
            if (id == null || id.trim() == "") return;
            this.notebook.addEntry(id, entryId);
        },
        setId: function(entryId) {
            var cursor = this.editor.getCursorPosition();
            this.editor.insert(entryId);
            //            this.editor.selection.moveTo(cursor.row, cursor.column);
            //            this.editor.focus();
        },
        cdEntry: function(entryId) {
            var div = new Div("");
            this.currentEntry = this.entries[entryId];
            notebookState.entries["current"] = this.currentEntry;
            this.output.html(div.toString());
            this.processCommand_pwd("pwd", [], div);
            this.outputUpdated();
        },
        addToToolbar: function(id, entry, toolbarItems) {
            var call = "getHandler('" + id + "').setId('" + entry.getId() + "')";
            var icon = HtmlUtils.image(ramaddaBaseUrl + "/icons/setid.png", ["border", 0, ATTR_TITLE, "Set ID in Input"]);
            toolbarItems.unshift(HtmlUtils.tag(TAG_A, ["onclick", call], icon));
            var call = "getHandler('" + id + "').selectEntry('" + entry.getId() + "')";
            var icon = HtmlUtils.image(ramaddaBaseUrl + "/icons/circle-check.png", ["border", 0, ATTR_TITLE, "Select Entry"]);
            toolbarItems.unshift(HtmlUtils.tag(TAG_A, ["onclick", call], icon));
        },
        getEntryPrefix: function(id, entry) {
            this.entries[entry.getId()] = entry;
            var call = "getHandler('" + id + "').cdEntry('" + entry.getId() + "')";
            return HtmlUtils.div(["style", "padding-right:4px;", "title", "cd to entry", "onclick", call, "class", "ramadda-link"], HtmlUtils.image(ramaddaBaseUrl + "/icons/go.png"));
        },
        displayEntries: function(entries, div) {
            if (div == null) div = new Div();
            this.currentEntries = entries;
            if (entries == null || entries.length == 0) {
                return div.msg("No children");
            }
            var handlerId = addHandler(this);
            var html = this.notebook.getEntriesTree(entries, {
                handlerId: handlerId,
                showIndex: false,
                suffix: Utils.getUniqueId("_shell_")
            });
            div.set(HtmlUtils.div(["style", "max-height:200px;overflow-y:auto;"], html));
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
            if (this.currentEntry)
                await this.currentEntry.getParentEntry(entry => {
                    this.parentEntry = entry;
                });
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
            if (!entry) await this.getCurrentEntry(e => entry = e);
            if ((typeof entry) == "string") {
                await this.notebook.getEntry(entry, e => entry = e);
            }

            if (!state.displayManager) {
                var divId = HtmlUtils.getUniqueId();
                state.div.append(HtmlUtils.div(["id", divId], ""));
                state.displayManager = new DisplayManager(divId, {
                    "showMap": false,
                    "showMenu": false,
                    "showTitle": false,
                    "layoutType": "table",
                    "layoutColumns": 1,
                    "defaultMapLayer": "osm",
                    "entryId": ""
                });
            }

            var divId = HtmlUtils.getUniqueId();
            state.div.append(HtmlUtils.div(["id", divId], "DIV"));
            var props = {
                layoutHere: true,
                divid: divId,
                showMenu: true,
                sourceEntry: entry,
                entryId: entry.getId(),
                showTitle: true,
                showDetails: true,
                title: entry.getName(),
            };

            if (displayProps) {
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
            await this.getCurrentEntry(e => current = e);
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
                suffix: Utils.getUniqueId("_shell_")
            });
            div.set(HtmlUtils.div(["style", "max-height:200px;overflow-y:auto;"], html));
            return div;
            //            var icon = entry.getIconImage([ATTR_TITLE, "View entry"]);
            //            return "&gt; "+ icon +" " +entry.getName();
        },
        processCommand_pwd: async function(line, toks, div) {
            if (div == null) div = new Div();
            await this.getCurrentEntry(e => entry = e);
            return this.getEntryHeading(entry, div);
        },
        processCommand_set: async function(line, toks, div) {
            if (div == null) div = new Div();
            if (toks.length < 2) {
                div.append("Error: usage: set &lt;name&gt; &lt;value&gt;");
                return;
            }
            var name = toks[1];
            if (toks.length == 2) {
                var v = this.notebook.getGlobalValue(name);
                if (v) {
                    div.append(v);
                } else {
                    div.append("Unknown: " + name);
                }
            } else {
                var v = Utils.join(toks, " ", 2);
                v = v.replace(/\"/g, "");
                await this.notebook.addGlobal(name, v);
            }
        },
        processCommand_clearEntries: function(line, toks, div) {
            this.notebook.clearEntries();
            div.set("Entries cleared");
        },
        processCommand_printEntries: async function(line, toks, div) {
            var h = "";
            await this.getCurrentEntry(e => current = e);
            h += "current" + "=" + current.getName() + "<br>";
            var entries = this.notebook.getCurrentEntries();
            for (var name in entries) {
                var e = entries[name];
                h += name + "=" + e.entry.getName() + "<br>";
            }
            if (h == "") h = "No entries";
            div.set(h);
        },
        processCommand_echo: async function(line, toks, div) {
            line = line.replace(/^echo */, "");
            div.set(line);
        },
        processCommand_print: async function(line, toks, div) {
            line = line.replace(/^print */, "");
            div.set(line);
        },

        processCommand_info: async function(line, toks, div) {
            await this.getCurrentEntry(e => entry = e);
            div.append("current:" + entry.getName() + " id:" + entry.getId() + "<br>");
        },

        processCommand_cd: async function(line, toks, div) {
            if (div == null) div = new Div();
            if (toks.length <= 1) {
                await this.setCurrentEntry(this.notebook.getBaseEntry());
                return;
                //                return this.getEntryHeading(this.currentEntry, div);
            }
            var arg = Utils.join(toks, " ", 1).trim();
            var entry;
            await this.getEntryFromPath(arg, e => entry = e);
            if (!entry) {
                div.msg("Could not get entry:" + arg);
                return;
            }
            await this.setCurrentEntry(entry);
        },
        getEntryFromPath: async function(arg, callback) {
            var entry;
            await this.getCurrentEntry(e => entry = e);
            if (arg.startsWith("/")) {
                await entry.getRoot(e => {
                    entry = e
                });
            }
            var dirs = arg.split("/");
            for (var i = 0; i < dirs.length; i++) {
                var dir = dirs[i];
                if (dir == "") continue;
                if (dir == "..") {
                    await entry.getParentEntry(e => {
                        entry = e
                    });
                    if (!entry) {
                        break;
                    }
                } else {
                    await entry.getChildrenEntries(c => children = c);
                    var child = null;
                    var startsWith = false;
                    var endsWith = false;
                    if (dir.endsWith("*")) {
                        dir = dir.substring(0, dir.length - 1);
                        startsWith = true;
                    }
                    if (dir.startsWith("*")) {
                        dir = dir.substring(1);
                        endsWith = true;
                    }
                    for (var childIdx = 0; childIdx < children.length; childIdx++) {
			let theChild = children[childIdx];
                        var name = theChild.getName();
                        if (startsWith && endsWith) {
                            if (name.includes(dir)) {
                                child = theChild;
                                break;
                            }
                        } else if (startsWith) {
                            if (name.startsWith(dir)) {
                                child = theChild;
                                break;
                            }
                        } else if (endsWith) {
                            if (name.endsWith(dir)) {
                                child = theChild;
                                break;
                            }
                        }
                        if (theChild.getName() == dir || theChild.getFilename()==dir) {
                            child = theChild;
                            break;
                        }
                    }
                    if (!child) {
                        break;
                    }
                    entry = child;
                }
            }
            return Utils.call(callback, entry);
        },


        processCommand_ls: async function(line, toks, div) {
            if (div == null) div = new Div();
            div.set("Listing entries...");
            await this.getCurrentEntry(e => entry = e);
            await entry.getChildrenEntries(children => {
                this.displayEntries(children, div)
            }, "");
        },
        entryListChanged: function(entryList) {
            var entries = entryList.getEntries();
            if (entries.length == 0) {
                this.writeStatusMessage("Sorry, nothing found");
            } else {
                this.displayEntries(entries);
            }
        },
        processCommand_search: async function(line, toks, div) {
            var text = "";
            for (var i = 1; i < toks.length; i++) text += toks[i] + " ";
            text = text.trim();
            var settings = new EntrySearchSettings({
                text: text,
            });
            var jsonUrl = this.notebook.getRamadda().getSearchUrl(settings, OUTPUT_JSON);
            let _this = this;
            var myCallback = {
                entryListChanged: function(list) {
                    var entries = list.getEntries();
                    div.set("");
                    if (entries.length == 0) {
                        div.append("Nothing found");
                    } else {
                        _this.displayEntries(entries, div)
                    }
                }
            };
            var entryList = new EntryList(this.notebook.getRamadda(), jsonUrl, myCallback, false);
            div.set("Searching...");
            await entryList.doSearch();
        },
        processCommand_clear: function(line, toks, div) {
            this.clearOutput();
        },
        processCommand_save: function(line, toks, div) {
            this.notebook.saveNotebook();
        },

    });

}


function processLispOutput(r) {
    if (r && r.val) return r.val;
    return Utils.formatJson(r);
}




function NotebookChunk(cell, props) {
    for(name in props)
        props[name.toLowerCase()] = props[name];
    this.div =  new Div(null, "display-notebook-chunk");
    this.cell = cell;
    $.extend(this, {
            getContent: function() {
                var content = this.content;
                for (name in this.cell.notebook.globals) {
                    var value = this.cell.notebook.getGlobalValue(name);
                    if (typeof value == "object") {
                        value = Utils.formatJson(value);
                    }
                    content = content.replace("${" + name.trim() + "}", value);
                }
                return content;
            },
           initChunk: function(props) {
                this.skipOutput = false;
                if (props["skipoutput"] === true) {
                    this.skipOutput = true;
                    this.div.set("");
                    this.div = new Div();
                }
                var depends = [];
                if (props["depends"] && typeof props["depends"] == "string") depends = props["depends"].split(",");
                var content = props.content||"";
                var regexp = RegExp(/\${([^ }]+)}/g);
                while((result = regexp.exec(content)) !== null) {
                    var param = result[1];
                    if(!depends.includes(param)) depends.push(param);
                }
                $.extend(this, {
                        name: props["name"],
                            depends: depends,
                            output: null,
                            runFirst: props["runFirst"],
                            hasRun: false,
                            content: content,
                            type: props.type,
                            props: props,
                            doChunk: !!props.doChunk,
                            ok: true
                            });
            }
        });
    this.initChunk(props);
}

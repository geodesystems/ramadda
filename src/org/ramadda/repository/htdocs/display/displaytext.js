
/**
Copyright 2008-2019 Geode Systems LLC
*/


var DISPLAY_WORDCLOUD = "wordcloud";
var DISPLAY_TEXTSTATS = "textstats";
var DISPLAY_TEXTANALYSIS = "textanalysis";
var DISPLAY_TEXTRAW = "textraw";
addGlobalDisplayType({
    type: DISPLAY_WORDCLOUD,
    forUser: true,
    label: "Word Cloud",
    requiresData: true,
    category: "Text"
});

addGlobalDisplayType({
    type: DISPLAY_TEXTSTATS,
    forUser: true,
    label: "Text Stats",
    requiresData: true,
    category: "Text"
});
addGlobalDisplayType({
    type: DISPLAY_TEXTRAW,
    forUser: true,
    label: "Text Raw",
    requiresData: true,
    category: "Text"
});
addGlobalDisplayType({
    type: DISPLAY_TEXTANALYSIS,
    forUser: true,
    label: "Text Analysis",
    requiresData: true,
    category: "Text"
});



function RamaddaBaseTextDisplay(displayManager, id, type, properties) {
    var ID_TEXTBLOCK = "textblock";
    let SUPER = new RamaddaFieldsDisplay(displayManager, id, type, properties);
    RamaddaUtil.inherit(this, SUPER);
    $.extend(this, {
        processText: function() {
            let records = this.filterData();
            if (!records) {
                return null;
            }
            var allFields = this.getData().getRecordFields();
            var fields = this.getSelectedFields(allFields);
            if (fields.length == 0)
                fields = allFields;
            var strings = this.getFieldsOfType(fields, "string");
            if (strings.length == 0) {
                this.displayError("No string fields specified");
                return null;
            }
            var fieldInfo = {};
            var minLength = parseInt(this.getProperty("minLength", 0));
            var maxLength = parseInt(this.getProperty("maxLength", 100));
            var stopWords = this.getProperty("stopWords");
            if (stopWords) {
                if(stopWords=="default") {
                    stopWords = Utils.stopWords;
                } else {
                    stopWords = stopWords.split(",");
                }
            }
            var extraStopWords = this.getProperty("extraStopWords");
            if (extraStopWords) extraStopWords = extraStopWords.split(",");

            var stripTags = this.getProperty("stripTags", false);
            var tokenize = this.getProperty("tokenize", false);
            var lowerCase = this.getProperty("lowerCase", false);
            var removeArticles = this.getProperty("removeArticles", false);
            for (var rowIdx = 0; rowIdx < records.length; rowIdx++) {
                var row = this.getDataValues(records[rowIdx]);
                for (var fieldIdx = 0; fieldIdx < strings.length; fieldIdx++) {
                    var field = strings[fieldIdx];
                    if (!fieldInfo[field.getId()]) {
                        fieldInfo[field.getId()] = {
                            field: field,
                            words: [],
                            counts: {},
                            divId: this.getDomId(ID_TEXTBLOCK + (field.getIndex())),
                        }
                    }
                    var fi = fieldInfo[field.getId()];
                    var value = row[field.getIndex()];
                    if (stripTags) {
                        value = Utils.stripTags(value);
                    }
                    var values = [value];
                    if (tokenize) {
                        values = Utils.tokenizeWords(values[0], stopWords, extraStopWords, removeArticles);
                    }
                    for (var valueIdx = 0; valueIdx < values.length; valueIdx++) {
                        var value = values[valueIdx];
                        var _value = value.toLowerCase();
                        if (!tokenize) {
                            if (stopWords && stopWords.includes(_value)) continue;
                            if (extraStopWords && extraStopWords.includes(_value)) continue;
                        }
                        if (value.length > maxLength) continue;
                        if (value.length < minLength) continue;
                        if (lowerCase) value = value.toLowerCase();
                        if (!Utils.isDefined(fi.counts[value])) {
                            fi.counts[value] = 0;
                        }
                        fi.counts[value]++;
                    }
                }
            }



            return fieldInfo;
        }
    });
}


function RamaddaWordcloudDisplay(displayManager, id, properties) {
    var SUPER;
    RamaddaUtil.inherit(this, SUPER = new RamaddaBaseTextDisplay(displayManager, id, DISPLAY_WORDCLOUD, properties));
    addRamaddaDisplay(this);
    $.extend(this, {
        getContentsStyle: function() {
            return "";
        },
        checkLayout: function() {
            this.updateUIInner();
        },
        updateUI: function() {
            var includes = "<link rel='stylesheet' href='" + ramaddaBaseUrl + "/lib/jqcloud.min.css'>";
            includes += "<script src='" + ramaddaBaseUrl + "/lib/jqcloud.min.js'></script>";
            this.writeHtml(ID_DISPLAY_TOP, includes);
            let _this = this;
            var func = function() {
                _this.updateUIInner();
            };
            setTimeout(func, 10);
        },
        updateUIInner: function() {
            var fieldInfo = this.processText();
            if (fieldInfo == null) return;
            let records = this.filterData();
            var allFields = this.getData().getRecordFields();
            var fields = this.getSelectedFields(allFields);
            if (fields.length == 0)
                fields = allFields;
            var strings = this.getFieldsOfType(fields, "string");
            let _this = this;
            var divs = "";
            var words = [];
            var maxWords = parseInt(this.getProperty("maxWords", -1));
            var minCount = parseInt(this.getProperty("minCount", 0));
            var width = (100 * 1 / strings.length) + "%;";
            for (a in fieldInfo) {
                var fi = fieldInfo[a];
                let field = fi.field;
                var handlers = null;
                if (this.getProperty("handleClick", true)) {
                    handlers = {
                        click: function(w) {
                            var word = w.target.innerText;
                            _this.showRows(records, field, word);
                        }
                    }
                };
                for (word in fi.counts) {
                    var obj1 = {
                        weight: fi.counts[word],
                        handlers: handlers,
                        text: word,
                    };
                    var obj2 = {
                        weight: fi.counts[word],
                        handlers: handlers,
                        text: field.getLabel() + ":" + word,
                    };
                    fi.words.push(obj1);
                    words.push(obj2);
                }
                var label = "";
                if (this.getProperty("showFieldLabel", true))
                    label = "<b>" + fi.field.getLabel() + "</b>";

                divs += "<div style='display:inline-block;width:" + width + "'>" + label + HtmlUtils.div(["style", "border: 1px #ccc solid;height:300px;", "id", fi.divId], "") + "</div>";
            }

            this.writeHtml(ID_DISPLAY_CONTENTS, "");
            var options = {
                autoResize: true,
            };


            var colors = this.getColorTable(true);
            if (colors) {
                options.colors = colors,
                    options.classPattern = null;
                options.fontSize = {
                    from: 0.1,
                    to: 0.02
                };
            }
            if (this.getProperty("shape"))
                options.shape = this.getProperty("shape");
            if (this.getProperty("combined", false)) {
                this.writeHtml(ID_DISPLAY_CONTENTS, HtmlUtils.div(["id", this.getDomId("words"), "style", "height:300px;"], ""));
                $("#" + this.getDomId("words")).jQCloud(words, options);
            } else {
                this.writeHtml(ID_DISPLAY_CONTENTS, divs);
                for (a in fieldInfo) {
                    var fi = fieldInfo[a];
                    $("#" + fi.divId).jQCloud(fi.words, options);
                }
            }
        },
        showRows: function(records, field, word) {
            if (word.startsWith(field.getLabel() + ":")) {
                word = word.replace(field.getLabel() + ":", "");
            }
            var tableFields;
            if (this.getProperty("tableFields")) {
                tableFields = {};
                var list = this.getProperty("tableFields").split(",");
                for (a in list) {
                    tableFields[list[a]] = true;
                }
            }
            var fields = this.getData().getRecordFields();
            var html = "";
            var data = [];
            var header = [];
            data.push(header);
            for (var fieldIdx = 0; fieldIdx < fields.length; fieldIdx++) {
                var f = fields[fieldIdx];
                if (tableFields && !tableFields[f.getId()]) continue;
                header.push(fields[fieldIdx].getLabel());
            }
            var showRecords = this.getProperty("showRecords", false);
            if (showRecords) {
                html += "<br>";
            }
            for (var rowIdx = 0; rowIdx < records.length; rowIdx++) {
                var row = this.getDataValues(records[rowIdx]);
                if (word != row[field.getIndex()]) {
                    continue;
                }
                var tuple = [];
                data.push(tuple);
                for (var col = 0; col < fields.length; col++) {
                    var f = fields[col];
                    if (tableFields && !tableFields[f.getId()]) continue;
                    var v = row[f.getIndex()];
                    if (showRecords) {
                        html += HtmlUtil.b(f.getLabel()) + ": " + v + "</br>";
                    } else {
                        tuple.push(v);
                    }
                }
                if (showRecords) {
                    html += "<p>";
                }
            }
            if (showRecords) {
                this.writeHtml(ID_DISPLAY_BOTTOM, html);
            } else {
                this.writeHtml(ID_DISPLAY_BOTTOM, field.getLabel() + "=" + word + HtmlUtils.div(["id", this.getDomId("table"), "style", "height:300px"], ""));
                var dataTable = google.visualization.arrayToDataTable(data);
                this.chart = new google.visualization.Table(document.getElementById(this.getDomId("table")));
                this.chart.draw(dataTable, {});
            }
        }
    });
}



function RamaddaTextstatsDisplay(displayManager, id, properties) {
    let SUPER = new RamaddaBaseTextDisplay(displayManager, id, DISPLAY_TEXTSTATS, properties);
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);
    $.extend(this, {
        updateUI: function() {
            var fieldInfo = this.processText();
            if (fieldInfo == null) return;
            let records = this.filterData();
            var allFields = this.getData().getRecordFields();
            var fields = this.getSelectedFields(allFields);
            if (fields.length == 0)
                fields = allFields;
            var strings = this.getFieldsOfType(fields, "string");
            let _this = this;
            var divs = "";
            var words = [];
            var html = "";
            var counts = [];
            var maxWords = parseInt(this.getProperty("maxWords", -1));
            var minCount = parseInt(this.getProperty("minCount", 0));
            var showBars = this.getProperty("showBars", true);
            var scale = this.getProperty("barsScale", 10);
            var barColor = this.getProperty("barColor", "blue");
            var height = this.getProperty("height", "400");
            var barWidth = parseInt(this.getProperty("barWidth", "400"));
            for (a in fieldInfo) {
                var fi = fieldInfo[a];
                let field = fi.field;
                for (word in fi.counts) {
                    var count = fi.counts[word];
                    counts.push({
                        word: word,
                        count: count
                    });
                }
                counts.sort(function(a, b) {
                    return a.count < b.count;
                });
                if (minCount > 0) {
                    var tmp = [];
                    for (var i = 0; i < counts.length; i++) {
                        if (counts[i].count >= minCount)
                            tmp.push(counts[i]);
                    }
                    counts = tmp;
                }

                if (maxWords > 0) {
                    var tmp = [];
                    for (var i = 0; i <= maxWords && i < counts.length; i++) {
                        if (counts[i].count >= minCount)
                            tmp.push(counts[i]);
                    }
                    counts = tmp;
                }

                var min = 0;
                var max = 0;
                if (counts.length > 0) {
                    max = counts[0].count;
                    min = counts[counts.length - 1].count;
                }
                if (this.getProperty("showFieldLabel", true))
                    html += "<b>" + fi.field.getLabel() + "</b><br>";
                var table = "<table>";
                var totalWords = 0;
                for (var i = 0; i < counts.length; i++) {
                    totalWords += counts[i].count;
                }
                for (var i = 0; i < counts.length; i++) {
                    var percent = Math.round(10000 * (counts[i].count / totalWords)) / 100;
                    var row = HtmlUtils.td([], counts[i].word + "&nbsp;:&nbsp;") +
                        HtmlUtils.td([], counts[i].count + "&nbsp;&nbsp;(" + percent + "%)&nbsp;:&nbsp;");
                    row += "\n";
                    if (showBars) {
                        row += HtmlUtils.td([], "&nbsp;");
                        var wpercent = (counts[i].count - min) / max;
                        var width = 2 + wpercent * barWidth;
                        var color = barColor;
                        var div = HtmlUtils.div(["style", "height:10px;width:" + width + "px;background:" + color], "");
                        row += HtmlUtils.td([], div);
                    }
                    table += HtmlUtils.tr([], row);
                }
                table += "</table>";
                html += HtmlUtils.div(["style", "padding-left:4px;border-bottom:1px #ccc solid; border-top: 1px #ccc solid;max-height:" + height + "px;overflow-y:auto;height:" + height + "px;"], table);

            }
            this.writeHtml(ID_DISPLAY_CONTENTS, html);
        },
    });
}

function RamaddaTextanalysisDisplay(displayManager, id, properties) {
    let SUPER = new RamaddaBaseTextDisplay(displayManager, id, DISPLAY_TEXTANALYSIS, properties);
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);
    $.extend(this, {
        checkLayout: function() {
            this.updateUIInner();
        },
        updateUI: function() {
            var includes = "<script src='" + ramaddaBaseUrl + "/lib/compromise.min.js'></script>";
            this.writeHtml(ID_DISPLAY_TOP, includes);
            let _this = this;
            var func = function() {
                _this.updateUIInner();
            };
            setTimeout(func, 10);
        },
        updateUIInner: function() {
            let records = this.filterData();
            if (!records) {
                return null;
            }
            var allFields = this.getData().getRecordFields();
            var fields = this.getSelectedFields(allFields);
            if (fields.length == 0)
                fields = allFields;
            var strings = this.getFieldsOfType(fields, "string");
            if (strings.length == 0) {
                this.displayError("No string fields specified");
                return null;
            }
            var corpus = "";
            for (var rowIdx = 0; rowIdx < records.length; rowIdx++) {
                var row = this.getDataValues(records[rowIdx]);
                var line = "";
                for (var col = 0; col < fields.length; col++) {
                    var f = fields[col];
                    line += " ";
                    line += row[f.getIndex()];
                }
                corpus += line;
                corpus += "\n";
            }
            var nlp = window.nlp(corpus);
            var headers = [];
            var cols = [];
            if (this.getProperty("showPeople", true)) {
                headers.push("<b>People<b>");
                cols.push(this.printList(nlp.people().out('topk')));
            }
            if (this.getProperty("showPlaces", true)) {
                headers.push("<b>Places<b>");
                cols.push(this.printList(nlp.places().out('topk')));
            }
            if (this.getProperty("showOrganizations", true)) {
                headers.push("<b>Organizations<b>");
                cols.push(this.printList(nlp.organizations().out('topk')));
            }
            if (this.getProperty("showTopics", false)) {
                headers.push("<b>Topics<b>");
                cols.push(this.printList(nlp.topics().out('topk')));
            }
            if (this.getProperty("showNouns", false)) {
                headers.push("<b>Nouns<b>");
                cols.push(this.printList(nlp.nouns().out('topk')));
            }
            if (this.getProperty("showVerbs", false)) {
                headers.push("<b>Verbs<b>");
                cols.push(this.printList(nlp.verbs().out('topk')));
            }
            if (this.getProperty("showAdverbs", false)) {
                headers.push("<b>Adverbs<b>");
                cols.push(this.printList(nlp.adverbs().out('topk')));
            }
            if (this.getProperty("showAdjectives", false)) {
                headers.push("<b>Adjectives<b>");
                cols.push(this.printList(nlp.adjectives().out('topk')));
            }
            if (this.getProperty("showClauses", false)) {
                headers.push("<b>Clauses<b>");
                cols.push(this.printList(nlp.clauses().out('topk')));
            }
            if (this.getProperty("showContractions", false)) {
                headers.push("<b>Contractions<b>");
                cols.push(this.printList(nlp.contractions().out('topk')));
            }
            if (this.getProperty("showPhoneNumbers", false)) {
                headers.push("<b>Phone Numbers<b>");
                cols.push(this.printList(nlp.phoneNumbers().out('topk')));
            }
            if (this.getProperty("showValues", false)) {
                headers.push("<b>Values<b>");
                cols.push(this.printList(nlp.values().out('topk')));
            }
            if (this.getProperty("showAcronyms", false)) {
                headers.push("<b>Acronyms<b>");
                cols.push(this.printList(nlp.acronyms().out('topk')));
            }

            if (this.getProperty("showNGrams", false)) {
                headers.push("<b>NGrams<b>");
                cols.push(this.printList(nlp.ngrams().out('topk')));
            }
            if (this.getProperty("showDates", false)) {
                headers.push("<b>Dates<b>");
                cols.push(this.printList(nlp.dates().out('topk')));
            }
            if (this.getProperty("showQuotations", false)) {
                headers.push("<b>Quotations<b>");
                cols.push(this.printList(nlp.quotations().out('topk')));
            }
            if (this.getProperty("showUrls", false)) {
                headers.push("<b>URLs<b>");
                cols.push(this.printList(nlp.urls().out('topk')));
            }
            if (this.getProperty("showStatements", false)) {
                headers.push("<b>Statements<b>");
                cols.push(this.printList(nlp.statements().out('topk')));
            }
            if (this.getProperty("showTerms", false)) {
                headers.push("<b>Terms<b>");
                cols.push(this.printList(nlp.terms().out('topk')));
            }
            if (this.getProperty("showPossessives", false)) {
                headers.push("<b>Possessives<b>");
                cols.push(this.printList(nlp.possessives().out('topk')));
            }
            var height = this.getProperty("height", "400");
            var html = "";
            var colCnt = 0;
            var row = "";
            var header = "";
            for (var i = 0; i < cols.length; i++) {
                if (colCnt >= 3) {
                    var table = "<table width=100%>";
                    table += HtmlUtil.tr([], header);
                    table += HtmlUtil.tr(["valign", "top"], row);
                    table += "</table>";
                    html += HtmlUtils.div(["style", "margin-top:10px;padding:4px;border-top:1px #ccc solid; max-height:" + height + "px;overflow-y:auto;"], table);
                    colCnt = 0;
                    row = "";
                    header = "";
                }
                row += HtmlUtils.td(["width", "33%"], cols[i]);
                header += HtmlUtils.td(["width", "33%"], headers[i]);
                colCnt++;
            }
            if (row != "") {
                var table = "<table width=100%>";
                table += HtmlUtil.tr([], header);
                table += HtmlUtil.tr(["valign", "top"], row);
                table += "</table>";
                html += HtmlUtils.div(["style", "margin-top:10px;padding:4px;border-top:1px #ccc solid;" + height + "px;overflow-y:auto;"], table);
            }
            this.writeHtml(ID_DISPLAY_CONTENTS, html);
        },
        printList: function(l) {
            var maxWords = parseInt(this.getProperty("maxWords", 10));
            var minCount = parseInt(this.getProperty("minCount", 0));
            var table = "<table>";
            var cnt = 0;
            for (var i = 0; i < l.length; i++) {
                if (l[i].count < minCount) continue;
                var row = HtmlUtils.td([], l[i].normal + "&nbsp;&nbsp;") +
                    HtmlUtils.td([], l[i].count + " (" + l[i].percent + "%)");
                table += HtmlUtils.tr([], row);
                if (cnt++ > maxWords) break;
            }
            table += "</table>"
            return table;
        }
    });
}


function RamaddaTextrawDisplay(displayManager, id, properties) {
    var ID_TEXT = "text";
    let SUPER = new RamaddaBaseTextDisplay(displayManager, id, DISPLAY_TEXTRAW, properties);
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);
    $.extend(this, {
        checkLayout: function() {
            this.updateUI();
        },
        updateUI: function() {
            let records = this.filterData();
            if (!records) {
                return null;
            }

            var pattern = this.getProperty("pattern");
            if(pattern && pattern.length==0) pattern=null;
            this.writeHtml(ID_TOP_RIGHT,HtmlUtils.input("pattern", (pattern?pattern:""),["placeholder", "Search text", "id" , this.getDomId("search")]));
            let  _this = this;
            this.jq("search").keypress(function(event) {
                    if (event.which == 13) {
                        _this.setProperty("pattern", $(this).val());
                        _this.updateUI();
                    }
                });
            this.writeHtml(ID_DISPLAY_CONTENTS, HtmlUtils.div(["id",this.getDomId(ID_TEXT)],""));
            this.showText();
            },
         showText: function() {
                let records = this.filterData();
                if (!records) {
                return null;
            }
            var pattern = this.getProperty("pattern");
            if(pattern && pattern.length==0) pattern=null;
            var asHtml = this.getProperty("asHtml", true);
            var addLineNumbers = this.getProperty("addLineNumbers", true);
            if (addLineNumbers) asHtml = true;
            var maxLines = parseInt(this.getProperty("maxLines", 100000));
            var lineLength = parseInt(this.getProperty("lineLength", 10000));
            var breakLines = this.getProperty("breakLines", true);


            var includeEmptyLines = this.getProperty("includeEmptyLines", false);
            var allFields = this.getData().getRecordFields();
            var fields = this.getSelectedFields(allFields);
            if (fields.length == 0)
                fields = allFields;
            var strings = this.getFieldsOfType(fields, "string");
            if (strings.length == 0) {
                this.displayError("No string fields specified");
                return null;
            }
            var corpus = "";
            if (addLineNumbers) {
                corpus = "<table width=100%>";
            }
            var lineCnt = 0;
            var displayedLineCnt = 0;
            var re;
            if(pattern) {
                re= new RegExp("("+pattern+")");
            }

            for (var rowIdx = 0; rowIdx < records.length; rowIdx++) {
                var row = this.getDataValues(records[rowIdx]);
                var line = "";
                for (var col = 0; col < fields.length; col++) {
                    var f = fields[col];
                    line += " ";
                    line += row[f.getIndex()];
                }
                line = line.trim();
                if (!includeEmptyLines && line.length == 0) continue;
                line = line.replace(/</g, "&lt;").replace(/>/g, "&gt;");
                lineCnt++;
                if(re) {
                    if(!line.toLowerCase().match(re)) continue;
                    line = line.replace(re,"<span style=background:yellow;>$1</span>");
                }
                displayedLineCnt++;

                if(displayedLineCnt>maxLines) break;

                if (addLineNumbers) {
                    corpus += HtmlUtils.tr(["valign", "top"], HtmlUtils.td(["width", "10px"], "<a name=line_" + lineCnt + "></a>" +
                            "<a href=#line_" + lineCnt + ">#" + lineCnt + "</a>&nbsp;  ") +
                        HtmlUtils.td([], line));
                } else {
                    corpus += line;
                    if (asHtml) {
                        if (breakLines)
                            corpus += "<p>";
                        else
                            corpus += "<br>";
                    } else {
                        corpus += "\n";
                        if (breakLines)
                            corpus += "\n";
                    }
                }
            }
            if (addLineNumbers) {
                corpus += "</table>";
            }
            var height = this.getProperty("height", "600");
            if (!asHtml)
                corpus = HtmlUtils.tag("pre", [], corpus);
            var html = HtmlUtils.div(["style", "padding:4px;border:1px #ccc solid;border-top:1px #ccc solid; max-height:" + height + "px;overflow-y:auto;"], corpus);
            this.writeHtml(ID_TEXT, html);
        },
    });
}
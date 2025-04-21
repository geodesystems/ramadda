/**
   Copyright (c) 2008-2025 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/


const DISPLAY_WORDCLOUD = "wordcloud";
const DISPLAY_TEXTSTATS = "textstats";
const DISPLAY_FREQUENCY = "frequency";
const DISPLAY_TEXTANALYSIS = "textanalysis";
const DISPLAY_TEXTRAW = "textraw";
const DISPLAY_TEXT = "text";

const DISPLAY_BLOCKS = "blocks";
const DISPLAY_TEMPLATE = "template";
const DISPLAY_TOPFIELDS = "topfields";
const DISPLAY_GLOSSARY = "glossary";

addGlobalDisplayType({
    type: DISPLAY_TEXT,
    label: "Text Readout",
    requiresData: true,
    forUser: true,
    category: CATEGORY_TEXT,
    tooltip: makeDisplayTooltip("Simple text display","text.png")
});

addGlobalDisplayType({
    type: DISPLAY_TEMPLATE,
    label: "Template",
    requiresData: true,
    forUser: true,
    category: CATEGORY_TEXT,
    tooltip: makeDisplayTooltip("Flexible text template to show records","template.png")    
});


addGlobalDisplayType({
    type: DISPLAY_TOPFIELDS,
    label: "Top Fields",
    requiresData: true,
    forUser: true,
    category: CATEGORY_MISC,
    tooltip: makeDisplayTooltip("List Fields","topfields.png","For every row it sorts the field values and lists the field names"),    
});

addGlobalDisplayType({
    type: DISPLAY_WORDCLOUD,
    forUser: true,
    label: "Word Cloud",
    requiresData: true,
    category: CATEGORY_TEXT,
    tooltip: makeDisplayTooltip("Cloud of words","wordcloud.png")
});

addGlobalDisplayType({
    type: DISPLAY_TEXTSTATS,
    forUser: true,
    label: "Text Stats",
    requiresData: true,
    category: CATEGORY_TEXT,
    tooltip: makeDisplayTooltip("Summary statistics for text","textstats.png","Incudes line/word count, word length and frequency")    
});

addGlobalDisplayType({
    type: DISPLAY_FREQUENCY,
    forUser: true,
    label: "Frequency",
    requiresData: true,
    category: CATEGORY_TEXT,
    tooltip: makeDisplayTooltip("Text field based frequencies","frequency.png")
});
addGlobalDisplayType({
    type: DISPLAY_TEXTRAW,
    forUser: true,
    label: "Text Raw",
    requiresData: true,
    category: CATEGORY_TEXT,
    tooltip: makeDisplayTooltip("Shows raw text","textraw.png","Provides a search field")                                        
});
addGlobalDisplayType({
    type: DISPLAY_TEXTANALYSIS,
    forUser: true,
    label: "Text Analysis",
    requiresData: true,
    category: CATEGORY_TEXT,
    tooltip: makeDisplayTooltip("Analyzes text","textanalysis.png")
});
addGlobalDisplayType({
    type: DISPLAY_BLOCKS,
    forUser: true,
    label: "Blocks",
    requiresData: true,
    category: CATEGORY_MISC,
    tooltip: makeDisplayTooltip("Blocks","blocks.png","Shows a certain number of small blocks or icons color coded from the data"),        
});

addGlobalDisplayType({
    type: DISPLAY_GLOSSARY,
    forUser: true,
    label: "Glossary",
    requiresData: true,
    category: CATEGORY_TEXT,
    tooltip: makeDisplayTooltip("Searchable glossary","glossary.png")    
});


function RamaddaBaseTextDisplay(displayManager, id, type, properties) {
    const ID_TEXTBLOCK = "textblock";
    const SUPER = new RamaddaFieldsDisplay(displayManager, id, type, properties);
    defineDisplay(this, SUPER, [], {
        processText: function(cnt,fields) {
            let records = this.filterData();
            if (!records) {
                return null;
            }
            var fieldInfo = {};
            var allFields = this.getData().getRecordFields();
            fields = fields || this.getSelectedFields(allFields);
            if (fields.length == 0)
                fields = allFields;
            var strings = this.getFieldsByType(fields, "string");
            if (strings.length == 0) {
                this.displayError("No string fields specified");
                return null;
            }
            var minLength = parseInt(this.getProperty("minLength", 0));
            var maxLength = parseInt(this.getProperty("maxLength", 100));
            var stopWords = this.getProperty("stopWords");
            if (stopWords) {
                if (stopWords == "default") {
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
            if (cnt) {
                cnt.count = 0;
                cnt.total = 0;
                cnt.lengths = {};
            }
            for (var rowIdx = 0; rowIdx < records.length; rowIdx++) {
                var row = this.getDataValues(records[rowIdx]);
                for (var fieldIdx = 0; fieldIdx < strings.length; fieldIdx++) {
                    var field = strings[fieldIdx];
                    if (!fieldInfo[field.getId()]) {
                        fieldInfo[field.getId()] = {
                            field: field,
                            words: [],
                            counts: {},
                            divId: this.domId(ID_TEXTBLOCK + (field.getIndex())),
                        }
                    }
                    var fi = fieldInfo[field.getId()];
                    var value = row[field.getIndex()];
                    if (stripTags) {
                        value = Utils.stripTags(value);
                    }
                    var values = [value];
                    if (tokenize) {
			values[0] = values[0].replace(/\"/g," ");
                        values = Utils.tokenizeWords(values[0], stopWords, extraStopWords, removeArticles);
                    }
                    for (var valueIdx = 0; valueIdx < values.length; valueIdx++) {
                        var value = values[valueIdx].trim();
			if(values.length>1 && value.length<=1) continue;
			if(value.startsWith("&")) continue;  
			var _value = value.toLowerCase();
                        if (cnt) {
                            cnt.count++;
                            cnt.total += value.length;
                            if (!Utils.isDefined(cnt.lengths[value.length]))
                                cnt.lengths[value.length] = 0;
                            cnt.lengths[value.length]++;
                        }


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
    const SUPER = new RamaddaBaseTextDisplay(displayManager, id, DISPLAY_WORDCLOUD, properties);
    let myProps = [
	{label:"Wordcloud"},
	{p:'termField'},
	{p:'fields'},	
	{p:'tableFields'},
	{p:'countField'},
	{p:'tokenize',ex:true},
	{p:'handleClick',ex:true},
	{p:'showFieldLabel',ex:true},
	{p:'showRecords'},
	{p:'combined'},
	{p:'shape',ex:'rectangular'},
	{p:'stopWords',ex:'word1,word2'},
	{p:'showFieldLabel',ex:'false'}	
    ];
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
        getContentsStyle: function() {
            return "";
        },
        checkLayout: function() {
            this.updateUIInner();
        },
	hasJq:false,
        updateUI: function() {
	    if(!this.loadedJq) {
		this.loadedJq = true;
		let includes = "<link rel='stylesheet' href='" + RamaddaUtil.getCdnUrl("/lib/jqcloud.min.css")+"'>";
		includes += "<script src='" + RamaddaUtil.getCdnUrl("/lib/jqcloud.min.js")+"'></script>";
		this.writeHtml(ID_DISPLAY_TOP, includes);
		let _this = this;
		let tmp = $("body");
		let func = function() {
		    if(!tmp.jQCloud) {
			setTimeout(func, 100);
		    } else {
			_this.hasJq = true;
			_this.updateUIInner();
		    }
		};
		setTimeout(func, 100);
	    } else if(this.hasJq) {
                this.updateUIInner();
	    }
        },
        updateUIInner: function() {
            let records = this.filterData();
	    if(records == null) return;
	    let countField = this.getFieldById(null, this.getProperty("countField"));
	    let termField = this.getFieldById(null, this.getProperty("termField"));
            let allFields = this.getData().getRecordFields();
            let fields = termField?[termField]:this.getSelectedFields(allFields);
            let fieldInfo = this.processText(null,fields);
            if (fieldInfo == null) return;
            if (fields.length == 0)
                fields = allFields;



            let options = {
                autoResize: true,
            };
            let colors = this.getColorTable(true);
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


	    if(countField && termField) {
		let minLength = parseInt(this.getProperty("minLength", 0));
		let maxLength = parseInt(this.getProperty("maxLength", 100));
		let stopWords = this.getProperty("stopWords");
		if (stopWords) {
                    if (stopWords == "default") {
			stopWords = Utils.stopWords;
                    } else {
			stopWords = stopWords.split(",");
                    }
		}
		let info = [];
		let wordToWeight = {};
		records.every(record=>{
		    let word =termField.getValue(record);
		    let _word = word.toLowerCase();
                    if (stopWords && stopWords.includes(_word)) return true;
		    if(!wordToWeight[word]) {
			wordToWeight[word]=0;
		    }
		    wordToWeight[word]+=countField.getValue(record);
		    return true;
		});
                let handlers = null;
		let _this = this;
                if (this.getProperty("handleClick", true)) {
                    handlers = {
                        click: function(w) {
			    console.dir(w);
                            let word = w.target.innerText;
                            _this.showRows(records, null, word, fields);
                        }
                    }
                };

		    
		for(word in wordToWeight) {
		    info.push({
			text: word,
			html:{style:"cursor:pointer;"},
			weight:wordToWeight[word] ,
			handlers:handlers,
		    });

		}
                this.setContents(HU.div([ATTR_ID, this.domId("words"), ATTR_STYLE, HU.css('height','300px')], ""));
                $("#" + this.domId("words")).jQCloud(info, options);
		return
	    }
            let strings = this.getFieldsByType(fields, "string");
            let _this = this;
            let divs = "";
            let words = [];
            let maxWords = parseInt(this.getProperty("maxWords", -1));
            let minCount = parseInt(this.getProperty("minCount", 0));
            let width = (100 * 1 / strings.length) + "%;";
            for (a in fieldInfo) {
                let fi = fieldInfo[a];
                let field = fi.field;
                let handlers = null;
                if (this.getProperty("handleClick", true)) {
                    handlers = {
                        click: function(w) {
                            let word = w.target.innerText;
                            _this.showRows(records, field, word, fields);
                        }
                    }
                };

                let counts = [];
                for (word in fi.counts) {
                    let count = fi.counts[word];
                    counts.push({
                        word: word,
                        count: count
                    });
                }
                counts.sort(function(a, b) {
                    if (a.count < b.count) return -1;
                    if (a.count > b.count) return 1;
                    return 0;
                });
                if (minCount > 0) {
                    let tmp = [];
                    for (let i = 0; i < counts.length; i++) {
                        if (counts[i].count >= minCount)
                            tmp.push(counts[i]);
                    }
                    counts = tmp;
                }
                if (maxWords > 0) {
                    let tmp = [];
                    for (let i = 0; i <= maxWords && i < counts.length; i++) {
                        if (counts[i].count >= minCount)
                            tmp.push(counts[i]);
                    }
                    counts = tmp;
                }

                for (let wordIdx = 0; wordIdx < counts.length; wordIdx++) {
                    let word = counts[wordIdx];
                    let obj1 = {
                        weight: word.count,
			html:{style:"cursor:pointer;"},
                        handlers: handlers,
                        text: word.word,
                    };
                    let obj2 = {
                        weight: word.count,
			html:{style:"cursor:pointer;"},
                        handlers: handlers,
                        text: field.getLabel() + ":" + word.word,
                    };
                    fi.words.push(obj1);
                    words.push(obj2);
                }
                let label = "";
                if (this.getProperty("showFieldLabel", true))
                    label = HU.b(fi.field.getLabel());

                divs += HU.div([ATTR_STYLE,HU.css('display','inline-block','width', width)], 
			       label + HU.div([ATTR_STYLE, HU.css('border','1px #ccc solid','height','300px'), ATTR_ID, fi.divId], ""));
            }

            this.setContents("");
            if (this.getProperty("combined", false)) {
                this.setContents(HU.div([ATTR_ID, this.domId("words"), ATTR_STYLE, HU.css('height','300px')], ""));
                $("#" + this.domId("words")).jQCloud(words, options);
            } else {
                this.setContents(divs);
                for (a in fieldInfo) {
                    let fi = fieldInfo[a];
                    $("#" + fi.divId).jQCloud(fi.words, options);
                }
            }
        },
        showRows: function(records, field, word, stringFields) {
	    if(!field)
		field = this.getFieldById(null, this.getProperty("termField"));
            let tokenize = this.getProperty("tokenize", false);
            if (stringFields && word.startsWith(field.getLabel() + ":")) {
                word = word.replace(field.getLabel() + ":", "");
            }
            let tableFields;
            if (this.getProperty("tableFields")) {
                tableFields = {};
                let list = this.getProperty("tableFields").split(",");
                for (a in list) {
                    tableFields[list[a]] = true;
                }
            }
            let fields = this.getData().getRecordFields();
            let html = "";
            let data = [];
            let header = [];
            data.push(header);
            for (let fieldIdx = 0; fieldIdx < fields.length; fieldIdx++) {
                let f = fields[fieldIdx];
                if (tableFields && !tableFields[f.getId()]) continue;
                header.push(fields[fieldIdx].getLabel());
            }
            let showRecords = this.getProperty("showRecords", false);
            if (showRecords) {
                html += "<br>";
            }
            let re = new RegExp("(\\b" + word + "\\b)", 'i');
            for (let rowIdx = 0; rowIdx < records.length; rowIdx++) {
                let row = this.getDataValues(records[rowIdx]);
                let value =  row[field.getIndex()];
                if (tokenize) {
                    if (!value.match(re)) {
                        continue;
                    }
                } else {
                    if (word != value) {
                        continue;
                    }
                }
                let tuple = [];
                data.push(tuple);
                for (let col = 0; col < fields.length; col++) {
                    let f = fields[col];
                    if (tableFields && !tableFields[f.getId()]) continue;
                    let v = row[f.getIndex()];
		    if(v.getTime) {
			v = {v:v,f:this.formatDate(v)};
		    } else {
			if (tokenize) {
                            v = v.replace(re, "<span style=background:yellow;>$1</span>");
			}
		    }
                    if (showRecords) {
                        html += HU.b(f.getLabel()) + ": " + v + "</br>";
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
                let prefix = "";
                if (!tokenize) {
                    prefix = (field?field.getLabel():"Word") + "=" + word
                }
                this.writeHtml(ID_DISPLAY_BOTTOM, HU.center(prefix + HU.div([ATTR_ID, this.domId("table"), ATTR_STYLE, HU.css('height','300px')], "")));
		let step2=()=>{
                    let dataTable = google.visualization.arrayToDataTable(data);
                    this.chart = new google.visualization.Table(document.getElementById(this.domId("table")));
                    this.chart.draw(dataTable, {
			allowHtml: true
                    });
		}
		let step1=()=>{
		    if(!ramaddaLoadGoogleChart('table',step2)) {
			return
		    }
		}
		if(!haveGoogleChartsLoaded(step1)) {
		    return;
		}
		step2();
            }
        }
    });
}




function RamaddaTemplateDisplay(displayManager, id, properties) {
    if(!Utils.isDefined(properties.showTitle)) properties.showTitle=false;
    if(!Utils.isDefined(properties.showMenu)) properties.showMenu=false;
//    if(!Utils.isDefined(properties.displayStyle)) properties.displayStyle = "background:rgba(0,0,0,0);";
    const SUPER =  new RamaddaFieldsDisplay(displayManager, id, DISPLAY_TEMPLATE, properties);
    let myProps = [
	{label:"Template"},
	{p: "template",ex:'${default}'},
	{p:"toggleTemplate",ex:"",tt:'Used as the toggle label for hiding/showing the main template'},
	{p:"headerTemplate",ex:"... ${totalCount} ... ${selectedCount} ${filter_field} ..."},
	{p:"footerTemplate",ex:"... ${totalCount} ... ${selectedCount} ${filter_.field} ... "},
	{p:"templateStyle",ex:'display:inline-block;',tt:'Style for the wrapper div'},	
	{p:"emptyMessage",tt:'Text to show when there are no records to show'},
	{p:"select",ex:"max|min|<|>|=|<=|>=|contains"},
	{p:"selectField"},
	{p:"selectValue"},
	{p:'onlyShowSelected',ex:'true'},
	{p:'useNearestDate',tt:'Sample on the selected record time',ex:'true'},	
	{p:'showFirst',ex:'true',tt:'Show first record'},
	{p:'showLast',ex:'true',tt:'Show last record'},		
	{p:'showRecords',tt:'comma separated list of record indices',ex:'0,3,4'},
	{p:'dontShowRecords',tt:'comma separated list of record indices to not show',ex:'0,3,4'},	
	{p:'selectHighlight',ex:'true'},	
	{p:'handleSelectOnClick',ex:'false',tt:"Don't select the record on a click"},
	{p:"groupByField"},
	{p:"groupDelimiter",ex:"<br>"},	
	{p:"groupTemplate",wikivalue:"<b>${group}</b><ul>${contents}</ul>"},
	{p:"sortGroups",wikivalue:"true"},
	{p:'${&lt;field&gt;_total}'},
	{p:'${&lt;field&gt;_max}'},
	{p:'${&lt;field&gt;_min}'},
	{p:'${&lt;field&gt;_average}'},
	{p:'highlightOnScroll',ex:'true'},
	{p:'scrollOnHighlight',ex:'true',d:'true',tt:'Scroll to the record when it is highlighted'},

        {p:'highlightFilterText',ex:'true',tt:'Highlight any filter text'},	
	{p:'colorBackground',d:false, canCache:true},
	{p:'addCopyToClipboard',ex:true,tt:'Add a link to copy the output to the clipboard'},
	{p:'copyToClipboardDownloadFile',ex:'somefile.txt',tt:'Instead of copying to the clipboard download the file'}	
    ];


    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
	dataFilterChanged: function() {
	    if(this.getOnlyShowSelected() && this.selectedRecord ) {
		this.selectedRecord = null;
		this.selectedRecords = null;		
		this.setContents("");
	    }
	    SUPER.dataFilterChanged.call(this);
	},
        displayData: function(reload, debug) {
	    this.updateUI();
	},
	updateUI: function() {
	    let records = this.myRecords = this.filterData();
	    if(!records) return;
	    let fields = this.getFields();
            fields = this.getSelectedFields();
	    if(!fields) return;
	    if(this.getOnlyShowSelected()) {
		if(!this.selectedRecord && !this.selectedRecords) {
		    if(this.getShowFirst(true)) {
			this.selectedRecord = records[0];
		    }
		}
		if(!this.selectedRecord && !this.selectedRecords) {
		    this.setContents(this.getEmptyMessage("<br>"));
		    return;
		}
		records = this.selectedRecords|| [this.selectedRecord];
		records= this.sortRecords(records);
	    } else {
		records= this.sortRecords(records);
		if(this.getShowFirst(false)) {
		    records = [records[0]];
		} else 	if(this.getShowLast(false)) {
		    records = [records[records.length-1]];
		}
	    }
	    let showRecords = this.getShowRecords();
	    if(showRecords) {
		let tmp = [];
		showRecords.split(",").forEach(idx=>{
		    tmp.push(records[+idx]);
		});
		records = tmp;
	    }
	    let dontShowRecords = this.getDontShowRecords();
	    if(dontShowRecords) {
		let notOK = {};
		dontShowRecords.split(",").forEach(idx=>{
		    notOK[+idx]=true;
		});
		let tmp = [];
		for(let i=0;i<records.length;i++) {
		    if(notOK[i]) continue;
		    tmp.push(records[i]);
		}
		records = tmp;
	    }	    
	    let uniqueFields  = this.getFieldsByIds(fields, this.getProperty("uniqueFields"));
	    let uniqueMap ={};
	    let template = this.getTemplate();
	    let toggleTemplate = this.getToggleTemplate();
	    let select = this.getProperty("select","all");
	    let selected = [];
	    let summary = {};
	    let goodRecords = [];
	    let matchers = this.getHighlightFilterText()?this.getFilterTextMatchers():null;
	    records.forEach(record=>{
		if(uniqueFields.length>0) {
		    var key= "";
		    uniqueFields.map(uf=>{
			key += "__" +uf.getValue(record);
		    });
		    if(Utils.isDefined(uniqueMap[key])) {
			return;
		    }
		    uniqueMap[key] = true;
		}
		goodRecords.push(record);
		for(var i=0;i<fields.length;i++) {
		    var f = fields[i];
		    var v =f.getValue(record);
		    if(!summary[f.getId()]) {
			summary[f.getId()] = {
			    total: 0,
			    min: v,
			    max:v,
			    count:0,
			    uniques:{},
			    uniqueCount:0
			}
		    }
		    var s = summary[f.getId()];
		    if(f.isString()) {
			if(!s.uniques[v]) {
			    s.uniqueCount++;
			    s.uniques[v] = true;
			}
			continue;
		    } 
		    if(f.isDate&& v && v.getTime) {
			if(v.getTime()<s.min.getTime()) s.min = v;
			if(v.getTime()>s.max.getTime()) s.max = v;
		    }  else if(!isNaN(v)) {
			s.total+=v;
			s.min = Math.min(s.min,v);
			s.max = Math.max(s.max,v);
			s.count++;
		    }
		}
	    });

	    records=goodRecords;

	    for(var i=0;i<fields.length;i++) {
		var f = fields[i];
		if(!f.isNumeric()) continue;
		var s = summary[f.getId()];
		if(s && s.count) {
		    s.average =  s.total/s.count;
		}
	    }
	    

	    if(select == "max" || select=="min" || select=="=" || select=="<" || select == ">" ||
	       select == "<=" || 	       select == "?>=" || select=="match") {
		var selectField = this.getProperty("selectField",null);
		if(selectField) selectField  =this.getFieldById(null, selectField);
		if(!selectField) {
		    this.setContents("No selectField specified");
		    return;
		}
		var selectValue = this.getProperty("selectValue","0");
		var selectValueNum = parseFloat(selectValue);
		var max =0; 
		var min = 0;
		var cnt = 0;
		var maxRecord;
		var minRecord;
		var equalsRecord;
		records.map(record=>{
		    var v =selectField.getValue(record);
		    if(select == "match") {
			if(v.match(selectValue)) {
			    selected.push(record);
			}
			return;
		    }
		    if(select == "=") {
			if(v == selectValue) {
			    selected.push(record);
			}
			return;
		    }
		    if(isNaN(v)) return;
		    if(select == "<") {
			if(v < selectValueNum) {
			    selected.push(record);
			}
			return;
		    }
		    if(select == ">") {
			if(v > selectValueNum) {
			    selected.push(record);
			}
			return;
		    }
		    if(select == ">=") {
			if(v >= selectValueNum) {
			    selected.push(record);
			}
			return;
		    }
		    if(select == "<=") {
			if(v <= selectValueNum) {
			    selected.push(record);
			}
			return;
		    }
		    if(cnt++ == 0) {
			min  = v;
			max = v;
			minRecord = record;
			maxRecord = record;
			return;
		    }
		    if(v<min) {
			min  = v;
			minRecord = record;
		    }
		    if(v > max) {
			max =v;
			maxRecord = record;
		    }
		});
		if(select == "min") {
		    if(minRecord)
			selected.push(minRecord);
		} else 	if(select == "max") {
		    if(maxRecord)
			selected.push(maxRecord);
		}
	    } else {
		selected = records;
	    }
	    let contents = "";
	    if(selected.length==0) {
		contents = this.getEmptyMessage("Nothing found");
	    }

            let colorBy = this.getColorByInfo(selected);


	    let attrs = {};
	    attrs["selectedCount"] = selected.length;
	    attrs["totalCount"] = records.length;


	    for(var i=0;i<fields.length;i++) {
		let f = fields[i];
		let s = summary[f.getId()];
		if(!s) continue;
		if(f.isDate) {
		    attrs[f.getId()+"_min"] = s.min;
		    attrs[f.getId()+"_max"] = s.max;
		    continue;
		}
		if(s && f.isString()) {
		    attrs[f.getId() +"_uniques"] =  s.uniqueCount;
		    continue;
		}
		if(!f.isNumeric()) continue;
		if(s) {
		    attrs[f.getId() +"_total"] = s.total;
		    attrs[f.getId() +"_min"] = s.min;
		    attrs[f.getId() +"_max"] = s.max;
		    attrs[f.getId() +"_average"] = s.average;
		}
	    }

	    let headerTemplate = this.getProperty("headerTemplate","");
	    let footerTemplate = this.getProperty("footerTemplate","");

	    if(selected.length==1) {
		let row = this.getDataValues(selected[0]);
		headerTemplate = this.applyRecordTemplate(selected[0],row,fields,headerTemplate);
		footerTemplate = this.applyRecordTemplate(selected[0],row,fields,footerTemplate);
	    }

	    let replace = (pattern,value)=>{
		headerTemplate = headerTemplate.replace(pattern,value);
		footerTemplate = footerTemplate.replace(pattern,value);		    
	    };

	    fields.forEach(f=>{
		if(!f.isNumeric()) return;
		let total = 0;
		let cnt = 0;
		let min=0;
		let max=0;
		selected.forEach(record=>{
		    let v= f.getValue(record);
		    if(!isNaN(v)) {
			if(cnt==0) {
			    min=v;
			    max=v;
			}
			min = Math.min(v,min);
			max = Math.max(v,max);			
			total+=v;
			cnt++;
		    }

		});
		replace('${'+ f.getId()+'_average}}', cnt==0?'NA':Utils.formatNumberComma(total/cnt));
		replace('${'+ f.getId()+'_min}', Utils.formatNumberComma(min));
		replace('${'+ f.getId()+'_max}', Utils.formatNumberComma(max));		
		replace('${'+ f.getId()+'_total}', Utils.formatNumberComma(total));
	    });
	    if(this.filters) {
		for(let filterIdx=0;filterIdx<this.filters.length;filterIdx++) {
		    let filter = this.filters[filterIdx];
		    if(!filter.isEnabled()) {
			continue;
		    }
		    let f = filter.getField();
		    let fid = "filter_" + f.getId();
		    if(f.isNumeric()) {
			let min = $("#" + this.domId("filterby_" + f.getId()+"_min")).val()?.trim();
			let max = $("#" + this.domId("filterby_" + f.getId()+"_max")).val()?.trim();
			attrs[fid +"_min"] = min;
			attrs[fid +"_max"] = max;
		    } else {
			let widget =$("#" + this.domId("filterby_" + f.getId())); 
			if(!widget.val || widget.val()==null) continue;
			let value = widget.val();
			if(!value) continue;
			if(Array.isArray(value)) {
			    let tmp = "";
			    value.forEach(v=>{
				if(tmp!="") tmp+=", ";
				tmp+=v;
			    });
			    value = tmp;
			}
			value = value.trim();
			if(value==FILTER_ALL) {
			    let regexp = new RegExp("\\${filter_" + f.getId()+"[^}]*\\}",'g');
//			    attrs[fid] = "";
			    replace(regexp,"");
			} else {
			    /*
			    let regexp = new RegExp("\\${filter_" + f.getId()+" +prefix='([^']*)' +suffix='([^']*)' *\\}",'g');

			    replace(regexp,"$1" + value +"$2");
			    regexp = new RegExp("\\${filter_" + f.getId()+" +prefix='([^']*)' *\\}",'g');
			    replace(regexp,"$1" + value);
			    regexp = new RegExp("\\${filter_" + f.getId()+" +suffix='([^']*)' *\\}",'g');
			    replace(regexp,value +"$1");
			    regexp = new RegExp("\\${filter_" + f.getId()+" *\\}",'g');
			    replace(regexp,value);
			    */
			    attrs[fid] = value;
			}
		    }
		}
	    }

	    let th = Utils.tokenizeMacros(headerTemplate);
	    let tf = Utils.tokenizeMacros(footerTemplate);
	    headerTemplate = th.apply(attrs);
	    footerTemplate = tf.apply(attrs);	    

	    if(selected.length>0) {
		contents+= headerTemplate;
	    }

	    let handleSelectOnClick = this.getPropertyHandleSelectOnClick(true);


	    if(Utils.stringDefined(template)) {
		let groupByField  =this.getFieldById(null, this.getProperty("groupByField"));
		let groupDelimiter  = this.getProperty("groupDelimiter"," ");
		let groupTemplate  = this.getProperty("groupTemplate","<b>${group}</b><ul>${contents}</ul>");
		let groupList = [];
		let groups = {};
		var props = this.getTemplateProps(fields);
		var max = parseFloat(this.getProperty("maxNumber",-1));
		var cols = parseFloat(this.getProperty("templateColumns",-1));
		var colTag;
		if(cols>0) {
		    colTag = "col-md-" +Math.round(12/cols);
		    contents += '<div class="row-tight row">';
		}
		var colCnt = 0;
		var style = this.getTemplateStyle("");
		let noWrapper = this.getNoWrapper();
		for(var rowIdx=0;rowIdx<selected.length;rowIdx++) {
		    if(max!=-1 && rowIdx>=max) break;
		    if(cols>0) {
			if(colCnt>=cols) {
			    colCnt=0;
			    contents += '</div>\n';
			    contents += '<div class="row-tight row">\n';
			}
			contents+='<div  class="' + colTag+'">\n';
			colCnt++;
		    }
		    var record = selected[rowIdx];
		    var color = null;
                    if (colorBy.index >= 0) {
			var value =  record.getData()[colorBy.index];
			color =  colorBy.getColor(value, record);
		    }
		    let s = template;
		    let row = this.getDataValues(record);
		    if(s.trim()=="${default}") {
			s = this.getRecordHtml(record,fields,s);
		    } else  if(s.startsWith("${fields")) {
			s = this.getRecordHtml(record,fields,s);
		    } else {
			s= this.applyRecordTemplate(record, row,fields,s,props);
		    }
		    if(matchers) {
			let sv = String(s);
			fields.forEach(field=>{
			    matchers.forEach(h=>{
				sv  = h.highlight(sv,field.getId());
			    });
			});
			s = sv;
		    }



		    let macros = Utils.tokenizeMacros(s);
		    let rowAttrs = {};
		    rowAttrs["selectCount"] = selected.length;
		    rowAttrs["totalCount"] = records.length;
		    rowAttrs[RECORD_INDEX] = rowIdx+1;
		    let dataFilters = this.getTheDataFilters();
		    this.filters.forEach(f=>{
			if(!f.isEnabled() || !f.getField) return;
			rowAttrs["filter." + f.getField().getId()] =  f.getFieldValues();
		    });
		    let recordStyle = style;
		    if(color) {
			if(this.getColorBackground()) {
			    recordStyle = HU.css("background",color) + recordStyle;
			}
			rowAttrs["color"] = color;
		    }
		    if(!handleSelectOnClick)			recordStyle+=HU.css("cursor","default");
		    let tag = HU.openTag("div",[ATTR_CLASS,noWrapper?'':'display-template-record',ATTR_STYLE,recordStyle, ATTR_ID, this.getId() +"-" + record.getId(), TITLE,"",RECORD_ID,record.getId(),RECORD_INDEX, rowIdx]);
		    s = macros.apply(rowAttrs);
		    if(s.startsWith("<td")) {
			s = s.replace(/<td([^>]*)>/,"<td $1>"+tag);
			s = s.replace(/<\/td>$/,"</div></td>");
		    } else if(s.startsWith("<tr")) {
			s = s.replace(/<td([^>]*)>/g,"<td $1>"+tag);
			s = s.replace(/<\/td>/g,"</div></td>");
		    }  else {
			s = tag +s +HU.close(DIV);
		    }
		    if(toggleTemplate) {
			let t =this.applyRecordTemplate(record, row,fields,toggleTemplate,props); 
			s =  HU.toggleBlock(t, s,false);
		    }

		    if (groupByField) {
			let groupValue =groupByField.getValue(record);
			if(groupValue.getTime) groupValue = this.formatDate(groupValue);
			if(!groups[groupValue]) {
			    groupList.push(groupValue);
			    groups[groupValue] = "";
			} else {
			    groups[groupValue]+=groupDelimiter;
			}
			groups[groupValue]+=s;
		    } else {
			contents+=s;
		    }


		    if(cols>0) {
			contents+=HU.close(DIV);
		    }
		}
		if (groupByField) {
		    if(this.getPropertySortGroups(false))
			groupList = groupList.sort();
		    groupList.forEach(group=>{
			contents+=  groupTemplate.replace("${group}",group).replace("${contents}",groups[group]);
		    });
		}
		if(cols>0) {
		    contents += HU.close(DIV);
		}
	    }

	    if(selected.length>0) 
		contents+= footerTemplate;
	    this.setContents(contents,true);
	    HU.createFancyBox( this.jq(ID_DISPLAY_CONTENTS).find("a.popup_image"), {
                caption : function( instance, item ) {
		    let caption =   $(this).attr('data-caption');
		    if(!Utils.stringDefined(caption)) caption = $(this).attr(ATTR_TITLE) || '';
		    return caption;
                }});

	    this.addFieldClickHandler(null,null,false);
	    let recordElements = this.find(".display-template-record");
	    this.makeTooltips(recordElements, selected);
	    this.makePopups(recordElements, selected);
	    let _this = this;
	    if(this.getProperty('addCopyToClipboard')) {
		this.jq(ID_DISPLAY_CONTENTS).find('.display-template-record').css('cursor','pointer');
		Utils.initCopyable(this.jq(ID_DISPLAY_CONTENTS),{addLink:true,removeTags:true,removeNL:true,
								 downloadFileName:this.getProperty('copyToClipboardDownloadFile')});
	    }
	    if(handleSelectOnClick) {
		this.find(".display-template-record").click(function() {
		    var record = selected[$(this).attr(RECORD_INDEX)];
		    _this.handleEventRecordHighlight(this, {record:record,highlight:true,immediate:true,skipScroll:true});
		    _this.propagateEventRecordSelection({highlight:true,record: record});
		});
	    }


	    if(this.getPropertyHighlightOnScroll(false)) {
		let items = this.find(".display-template-record");
		this.getContents().css('overflow-y','scroll');
		this.getContents().scroll(()=>{
		    let topElement = null;
		    items.each(function() {
			let pos  = $(this).position();
			if(pos.top<0) {
			    topElement = $(this);
			}
		    });
		    if(topElement) {
			var record = selected[topElement.attr(RECORD_INDEX)];
			if(record && this.currentTopRecord && record!=this.currentTopRecord) {
			    this.propagateEventRecordSelection({highlight:true,record: record});
			}
			this.currentTopRecord = record;
		    }
		});
	    }





	},
	highlightCount:0,
        handleEventRecordSelection: function(source, args) {
	    this.selectedRecords = args.records;
	    this.selectedRecord = args.record;
	    if(this.selectedRecord && this.getUseNearestDate() && this.myRecords) {
		this.selectedRecord = this.findClosestDate(this.selectedRecord.getDate()).record;
	    }

	    if(this.getOnlyShowSelected()) {
		this.updateUI();
	    } else {
		args.highlight = true;
		this.handleEventRecordHighlight(source, args);
	    }
	},
        handleEventRecordHighlight: function(source, args) {

	    if(this.getPropertySelectHighlight()) {
		this.selectedRecord=args.record;
		this.callUpdateUI();
		return
	    }

	    this.currentTopRecord = null;
	    let myCount = ++this.highlightCount;
	    var id = "#" + this.getId()+"-"+args.record.getId();
	    if(this.highlightedElement) {
		this.unhighlightElement(this.highlightedElement);
		this.highlightedElement = null;
	    }
//	    console.log(this.type+ " handleEventRecordHighlight " + args.highlight); 
	    if(args.highlight) {
		if(args.immediate) {
		    this.highlightElement(args);
		} else {
		    setTimeout(() =>{
			if(myCount == this.highlightCount) {
			    this.highlightElement(args);
			}
		    },100);
		}
	    } else {
		var id = "#" + this.getId()+"-"+args.record.getId();
		var element = $(id);
		this.unhighlightElement(element);
	    }
	},
	unhighlightElement: function(element) {
	    this.currentTopRecord = null;
	    element.removeClass("display-template-record-highlight");
	    var css = this.getProperty("highlightOffCss","").split(";");
	    if(css.length>0) {
		css.map(tok=>{
		    var c = tok.split(":");
		    var a = c[0];
		    var v = c.length>1?c[1]:null;
		    if(!v || v=="") {
			v = element.attr("prev-" + a);
		    }
		    if(v) {
			//			    console.log("un highlight css:" + element.css("background")+ " idx:" + element.attr(RECORD_INDEX));
			element.css(a,v);
		    }
		});
	    } 
	},
	highlightElement: function(args) {
	    let id = "#" + this.getId()+"-"+args.record.getId();
	    let element = $(id);
	    this.highlightedElement = element;
	    element.addClass("display-template-record-highlight");
	    let css = this.getProperty("highlightOnCss","").split(";");
	    if(css.length>0) {
		css.map(tok=>{
		    let c = tok.split(":");
		    let a = c[0];
		    let v = c.length>1?c[1]:null;
		    if(!v || v=="") {
			v = element.attr("prev-" + a);
		    }
		    if(v) {
			let oldV = element.css(a);
			if(oldV) {
			    element.attr("prev-" + a,oldV);
			}
			element.css(a,v);
		    }
		});
	    } 

	    try {
		if(this.getScrollOnHighlight() && !args.skipScroll) {
		    let eo = element.offset();
		    if(eo==null) return;
		    let container =  element.parent();
		    if(this.getProperty("orientation","vertical")== "vertical") {
			let c = container.offset().top;
			let s = container.scrollTop();
			container.scrollTop(eo.top- c + s)
		    } else {
			let c = container.offset().left;
			let s = container.scrollLeft();
			container.scrollLeft(eo.left- c + s)
		    }
		}

	    } catch(err) {
		console.log("Error:" + err);
	    }

	}
    })}





function RamaddaTopfieldsDisplay(displayManager, id, properties) {
    const ID_SLIDE = "slide";
    const SUPER =  new RamaddaFieldsDisplay(displayManager, id, DISPLAY_TOPFIELDS, properties);
    let myProps = [
	{label:'Top Fields'},
	{p:'fieldCount',tt:''},
	{p:'labelField',tt:''},
	{p:'dateFormat',ex:'yyyy'},
	{p:'labelField'},
	{p:'scaleFont',ex:'false'}
    ]
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
        needsData: function() {
            return true;
        },
	updateUI: function() {
	    var pointData = this.getData();
	    if (pointData == null) return;
	    let records = this.filterData();
	    if(!records) return;

            var fields = this.getData().getNonGeoFields();
	    var labelField = this.getFieldById(fields,this.getProperty("labelField"));
	    if(labelField==null) {
		labelField = this.getFieldById(fields, TITLE);
	    }
	    if(labelField==null) {
		labelField = this.getFieldById(fields, "name");
	    }
	    var fieldsToUse = this.getFieldsByIds(fields,this.getPropertyFields());
	    if(fieldsToUse.length==0) fieldsToUse = fields;
	    var html = "";
	    var fieldCount = +this.getProperty("fieldCount",10);
	    var dataList = [];
	    var min = Number.MAX_VALUE;
	    var max = Number.MIN_VALUE;
	    var dateFormat = this.getProperty("dateFormat");
	    for(var i=0;i<records.length;i++) {
		var record = records[i]; 
		var tuple = record.getData();
		var data =[];
		for(var j=0;j<fieldsToUse.length;j++) {
		    var field = fieldsToUse[j];
		    if(!field.isNumeric()) continue;
		    var value  =tuple[field.getIndex()];
		    if(!isNaN(value)) {
			min  = Math.min(min, value);
			max  = Math.max(max, value);
		    }
		    data.push({value: value, field: field});
		}
		data.sort((a,b)=>{
		    return b.value-a.value;
		});
		var header = labelField?tuple[labelField.getIndex()]:" Record:" + (i+1);
		if((labelField && labelField.isFieldDate()) || (typeof header =="date"))  {
		    header = Utils.formatDateWithFormat(header, dateFormat);
		}
		dataList.push({data:data,record:record,header:header});
	    }
	    var scaleFont = this.getProperty("scaleFont",true);
	    for(var i=0;i<dataList.length;i++) {
		var data = dataList[i].data;
		var record = dataList[i].record;
		var header = dataList[i].header;
		var tuple = record.getData();
		var div = "";
		var contents = "";
		for(var j=0;j<data.length && j<fieldCount;j++) {
		    var value = data[j].value;
		    var percent = max==min?1:(value-min)/(max-min);
		    var fontSize = 6+Math.round(percent*24)+"pt";
		    if(!scaleFont) fontSize = "100%";
		    var field = data[j].field;
		    contents += HU.div(["field-id",field.getId(), "data-value",field.getLabel(), TITLE,"Value: " + value, ATTR_CLASS,"display-topfields-row",ATTR_STYLE,"font-size:" + fontSize+";"], field.getLabel());
		}
		div += HU.div([ATTR_CLASS,"display-topfields-header",RECORD_INDEX,i],header);
		div += HU.div([ATTR_CLASS,"display-topfields-values"], contents);
		html+=HU.div([ATTR_CLASS,"display-topfields-record"], div);
	    }
	    this.setContents(html);
	    let _this = this;
	    this.find(".display-topfields-header").click(function(){
		var idx = $(this).attr(RECORD_INDEX);
		_this.find(".display-topfields-record").removeClass("display-topfields-selected");
		$(this).parent().addClass("display-topfields-selected");
		var record = records[idx];
		if(record) {
		    _this.propagateEventRecordSelection({record: record});
		}
	    });
	    let rows =this.find(".display-topfields-row");
	    rows.hover(function() {
		rows.removeClass("display-topfields-highlight");
		var value = $(this).attr("data-value");
		_this.find(HU.attrSelect("data-value", value)).addClass("display-topfields-highlight");
		
	    });
	    rows.click(function() {
		var field = $(this).attr("field-id");
		_this.getDisplayManager().notifyEvent(DisplayEvent.fieldsSelected, _this, [field]);
		
	    });
	},
        handleEventRecordSelection: function(source, args) {
	    if(!args.record) return;
	    var index = this.recordToIndex[args.record.getId()];
	    if(!Utils.isDefined(index)) return;
	    var container = this.getContents();
	    container.find(".display-topfields-record").removeClass("display-topfields-selected");
	    var element =   container.find(HU.attrSelect(RECORD_INDEX, index)).parent();
	    element.addClass("display-topfields-selected");
	    var c = container.offset().top;
	    var s = container.scrollTop();
	    var eo = element.offset();
	    var diff = eo.top- c + s;
	    container.scrollTop(diff)
	},



    })}










function RamaddaBlocksDisplay(displayManager, id, properties) {
    const SUPER =  new RamaddaFieldsDisplay(displayManager, id, DISPLAY_BLOCKS, properties);
    const ID_BLOCKS_HEADER = "blocks_header";
    const ID_BLOCKS = "blocks";
    const ID_BLOCKS_FOOTER = "blocks_footer";
    let myProps = [
	{label:'Block'},
	{p:'animStep',d:0,ex:"1000",tt:'Animation delay (ms)'},
	{p:'doSum',d:true,ex:"false",tt:''},
	{p:'numBlocks',d:1000,ex:"1000",tt:'How many blocks to show'},
	{p:'header',d:true,ex:"Each block represents ${blockValue} ... There were a total of ${total} ...",tt:''},
	{p:'blockIcon',d:null,ex:"fa-male",tt:'Use an icon'},
	{p:'emptyBlockStyle',d:"background:transparent;border:1px solid #ccc;",tt:'Style for blocks not displayed yet'},	
    ];


    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
        getContentsStyle: function() {
            return "";
        },
        updateUI: function() {
	    let f = this.getPropertyFields();
	    let records;
	    if(f) {
		records = this.filterData();
		if(!records) return;
	    }
	    this.counts = [];
	    this.counts2 = [];
	    if(f) {
		let fields = this.getFieldsByIds(null,f);
		if(!fields) return;
		this.footers = [];
		this.headers = [];
		let numBlocks  = this.getNumBlocks(1000);
		this.total = 0;
		fields.forEach(f=>{
		    this.footers.push("${count} " + f.getLabel());
		    let v = f.getValue(records[0]);
		    if(!isNaN(v)) this.total+=v;
		    this.counts.push(v);
		});

		if(this.total>0) {
		    this.blockValue = this.total/numBlocks;
		    this.counts.forEach(v=>{
			let percent = isNaN(v)?0:v/this.total;
			let scaledValue = percent*numBlocks;
			this.counts2.push(scaledValue);
		    });
		}
	    } else {
		//todo: set this
		this.blockValue = 0;
		let counts = this.getProperty("counts","100",true).split(";");
		for(let i=0;i<counts.length;i++) 
		    this.counts.push(parseFloat(counts[i]));
		let doSum = this.getPropertyDoSum();
		if(doSum) {
		    this.counts2 = this.counts;
		} else {
		    this.total = 0;
		    for(let i=0;i<this.counts.length;i++) {
			let tmp = this.counts[i];
			this.counts2.push(this.counts[i]-this.total);
			this.total+= tmp;
		    }
		}
		this.footers = this.getProperty("footers","",true).split(";");
		this.headers = this.getProperty("headers","",true).split(";");
	    }
	    while(this.footers.length< this.counts.length)
		this.footers.push("");
	    while(this.headers.length< this.counts.length)
		this.headers.push("");
	    this.setContents(
		HU.div([ATTR_CLASS,"display-blocks-header",ATTR_STYLE, this.getProperty("headerStyle","", true),ATTR_ID,this.domId(ID_BLOCKS_HEADER)]) +
		    HU.div([ATTR_CLASS,"display-blocks-blocks",ATTR_ID,this.domId(ID_BLOCKS)],"")+
		    HU.div([ATTR_CLASS,"display-blocks-footer", ATTR_STYLE, this.getProperty("footerStyle","", true), ATTR_ID,this.domId(ID_BLOCKS_FOOTER)]));
	    //Show the outline
	    this.showBlocks(true);
	    if(this.getProperty("displayOnScroll")) {
		HU.callWhenScrolled(this.domId(ID_DISPLAY_CONTENTS),()=>{
		    if(!this.displayedBlocks) {
			this.displayedBlocks = true;
			if(this.timeout)
			    clearTimeout(this.timeout);
			this.tmeout = setTimeout(()=>{this.showBlocks(false)},this.getPropertyAnimStep());
		    }
		},500);
	    }  else {
		this.showBlocks(false);
	    }
	},
	showBlocks: function(initial, step) {
	    if(!Utils.isDefined(step)) {
		if(initial)step = this.counts.length;
		else step = 0;
	    }
	    let contents = "";
	    contents += HU.openDiv([ATTR_CLASS,"display-blocks"]);
	    let tmp =this.getProperty("colors","#d73027,#fdae61,#74add1,#423E3B,red,green,blue",true); 
	    let ct = (typeof tmp) =="string"?tmp.split(","):tmp;
	    let footer ="";
	    while(ct.length<this.counts.length) {
		ct.push(ct[ct.length-1]);
	    }

	    let divider = parseFloat(this.getProperty("divider","1",true));
	    let dim=this.getProperty("blockDimensions","8",true);
	    let labelStyle = this.getProperty("labelStyle","", true);
	    let blockCnt = 0;
	    let iconProp = this.getProperty("blockIcon");
	    let clazz = iconProp?"display-block-icon":"display-block";
	    let emptyStyle = this.getEmptyBlockStyle();
	    for(let i=0;i<this.counts2.length;i++) {
		let num = this.counts[i];
		if(isNaN(num)) num = 0;
		let label = this.footers[i].replace("${count}",Utils.formatNumberComma(num));
		let style =  iconProp?"":"width:" + dim+"px;height:" + dim+"px;";
		let iconStyle = "";
		if(!initial) {
		    if(i<step) {
			if(!iconProp)
			    style += HU.css("background", ct[i]);
			else
			    iconStyle+=HU.css("color" ,ct[i]);
			let footerIcon =  iconProp?HU.getIconImage(iconProp, null, [ATTR_STYLE, iconStyle]):"";
			footer += HU.div([ATTR_CLASS,clazz,ATTR_STYLE,style],footerIcon) +" " + HU.span([ATTR_STYLE,labelStyle], label)+"&nbsp;&nbsp;";
		    } else {
			if(iconProp)
			    style += HU.css("background","transparent");
			else
			    style += HU.css("background","transparent","border","1px solid #ccc");			    			
			footer += "&nbsp;&nbsp;";
		    }
		} else {
		    style+=emptyStyle;
		}
		let icon = iconProp?HU.getIconImage(iconProp, null, [ATTR_STYLE, iconStyle]):"";
		let cnt = this.counts2[i]/divider;
		for(let j=0;j<10000 && j<cnt;j++) {
		    contents += HU.div([ATTR_CLASS,clazz,ATTR_STYLE,style,TITLE,label],icon);
		}
		blockCnt++;
	    }
	    contents += HU.closeDiv();
	    let header = this.getProperty("header","");
	    header = header.replace("${divider}",divider).replace("${total}",Utils.formatNumberComma(this.total)).replace("${blockValue}", Utils.formatNumberComma(Math.round(this.blockValue)));

	    this.jq(ID_BLOCKS_HEADER).html(header);
	    this.jq(ID_BLOCKS).html(contents);
	    this.jq(ID_BLOCKS_FOOTER).html(footer);
	    if(step < this.counts.length) {
		if(!this.getPropertyAnimStep()) {
		    this.showBlocks(false, step+1);
		} else {
		    if(this.timeout)
			clearTimeout(this.timeout);
		    this.timeout = setTimeout(()=>{this.showBlocks(false, step+1)},this.getPropertyAnimStep());
		}
	    }
	}
    });
}





function RamaddaTextstatsDisplay(displayManager, id, properties) {
    const SUPER = new RamaddaBaseTextDisplay(displayManager, id, DISPLAY_TEXTSTATS, properties);
    defineDisplay(addRamaddaDisplay(this), SUPER, [], {
        updateUI: function() {
            let cnt = {};
            let fieldInfo = this.processText(cnt);
            if (fieldInfo == null) return;
            let records = this.filterData();
            let allFields = this.getData().getRecordFields();
            let fields = this.getSelectedFields(allFields);
            if (fields.length == 0)
                fields = allFields;

            let strings = this.getFieldsByType(fields, "string");
            let _this = this;
            let divs = "";
            let words = [];
            let html = "";
            let counts = [];
            let maxWords = parseInt(this.getProperty("maxWords", -1));
            let minCount = parseInt(this.getProperty("minCount", 0));
            let showBars = this.getProperty("showBars", true);
            let scale = this.getProperty("barsScale", 10);
            let barColor = this.getProperty("barColor", "blue");
            let barWidth = parseInt(this.getProperty("barWidth", "400"));
            for (a in fieldInfo) {
                let fi = fieldInfo[a];
                let field = fi.field;
                for (word in fi.counts) {
                    let count = fi.counts[word];
                    counts.push({
                        word: word,
                        count: count
                    });
                }
                counts.sort(function(a, b) {
                    if (a.count < b.count) return -1;
                    if (a.count > b.count) return 1;
                    return 0;
                });
                if (minCount > 0) {
                    let tmp = [];
                    for (let i = 0; i < counts.length; i++) {
                        if (counts[i].count >= minCount)
                            tmp.push(counts[i]);
                    }
                    counts = tmp;
                }

                if (maxWords > 0) {
                    let tmp = [];
                    for (let i = 0; i <= maxWords && i < counts.length; i++) {
                        if (counts[i].count >= minCount)
                            tmp.push(counts[i]);
                    }
                    counts = tmp;
                }

                let min = 0;
                let max = 0;
                if (counts.length > 0) {
                    max = counts[0].count;
                    min = counts[counts.length - 1].count;
                }

                let tmp = [];
                for (a in cnt.lengths) {
                    tmp.push({
                        length: parseInt(a),
                        count: cnt.lengths[a]
                    });
                }
                tmp.sort(function(a, b) {
                    if (a.length < b.length) return -1;
                    if (a.length > b.length) return 1;
                    return 0;
                });
                min = 0;
                max = 0;
                for (let i = 0; i < tmp.length; i++) {
                    max = (i == 0 ? tmp[i].count : Math.max(max, tmp[i].count));
                    min = (i == 0 ? tmp[i].count : Math.min(min, tmp[i].count));
                }
                if (this.getProperty("showFieldLabel", true))
                    html += HU.b(fi.field.getLabel()) + "<br>";
                let td1Width = "20%";
                let td2Width = "10%";
                if (this.getProperty("showSummary", true)) {
                    html += HU.openTag("table", [ATTR_CLASS, "nowrap ramadda-table", ATTR_ID, this.domId("table_summary")]);
                    html += HU.openTag("thead", []);
                    html += HU.tr([], HU.th(["width", td1Width], "Summary") + HU.th([], "&nbsp;"));
                    html += HU.closeTag("thead");
                    html += HU.openTag("tbody", []);
                    html += HU.tr([], HU.td(["align", "right"], "Total lines:") + HU.td([], records.length));
                    html += HU.tr([], HU.td(["align", "right"], "Total words:") + HU.td([], cnt.count));
                    html += HU.tr([], HU.td(["align", "right"], "Average word length:") + HU.td([], Math.round(cnt.total / cnt.count)));
                    html += HU.closeTag("tbody");

                    html += HU.closeTag("table");
                    html += "<br>"
                }
                if (this.getProperty("showCounts", true)) {
                    html += HU.openTag("table", [ATTR_CLASS, "row-border nowrap ramadda-table", ATTR_ID, this.domId("table_counts")]);
                    html += HU.openTag("thead", []);
                    html += HU.tr([], HU.th(["width", td1Width], "Word Length") + HU.th(["width", td2Width], "Count") + (showBars ? HU.th([], "") : ""));
                    html += HU.closeTag("thead");
                    html += HU.openTag("tbody", []);
                    for (let i = 0; i < tmp.length; i++) {
                        let row = HU.td([], tmp[i].length) + HU.td([], tmp[i].count);
                        if (showBars) {
                            let wpercent = (tmp[i].count - min) / max;
                            let width = 2 + wpercent * barWidth;
                            let color = barColor;
                            let div = HU.div([ATTR_STYLE, "height:10px;width:" + width + "px;background:" + color], "");
                            row += HU.td([], div);
                        }
                        html += HU.tr([], row);
                    }
                    html += HU.closeTag("tbody");
                    html += HU.closeTag("table");
                    html += "<br>"
                }
                if (this.getProperty("showFrequency", true)) {
                    html += HU.openTag("table", [ATTR_CLASS, "row-border ramadda-table", ATTR_ID, this.domId("table_frequency")]);
                    html += HU.openTag("thead", []);
                    html += HU.tr([], HU.th(["width", td1Width], "Word") + HU.th(["width", td2Width], "Frequency") + (showBars ? HU.th([], "") : ""));
                    html += HU.closeTag("thead");
                    html += HU.openTag("tbody", []);
                    let min = 0;
                    let max = 0;
                    if (counts.length > 0) {
                        min = counts[0].count;
                        max = counts[counts.length - 1].count;
                    }
                    let totalWords = 0;
                    for (let i = 0; i < counts.length; i++) {
                        totalWords += counts[i].count;
                    }
                    for (let i = counts.length - 1; i >= 0; i--) {
                        let percent = Math.round(10000 * (counts[i].count / totalWords)) / 100;
                        let row = HU.td([], counts[i].word + "&nbsp;:&nbsp;") +
                            HU.td([], counts[i].count + "&nbsp;&nbsp;(" + percent + "%)&nbsp;:&nbsp;");
                        if (showBars) {
                            let wpercent = (counts[i].count - min) / max;
                            let width = 2 + wpercent * barWidth;
                            let color = barColor;
                            let div = HU.div([ATTR_STYLE, "height:10px;width:" + width + "px;background:" + color], "");
                            row += HU.td([], div);
                        }
                        html += HU.tr([], row);
                    }
                    html += HU.closeTag("tbody");
                    html += HU.closeTag("table");
                }
            }
            this.setContents(html);
            let tableHeight = this.getProperty("tableHeight", "200");

            if (this.getProperty("showSummary", true))
                HU.formatTable("#" + this.domId("table_summary"), {
                    scrollY: this.getProperty("tableSummaryHeight", tableHeight)
                });
            if (this.getProperty("showCounts", true))
                HU.formatTable("#" + this.domId("table_counts"), {
                    scrollY: this.getProperty("tableCountsHeight", tableHeight)
                });
            if (this.getProperty("showFrequency", true)) {
                HU.formatTable("#" + this.domId("table_frequency"), {
		    scrollY: this.getProperty("tableFrequenecyHeight", tableHeight),
		    searching: this.getProperty("showSearch", true)
		}, table =>{
		    this.frequencyTable = table;
		    this.frequencyTable.on( 'search.dt', ()=>{
			if(this.settingSearch) return;
			this.propagateEvent(DisplayEvent.propertyChanged, {
			    property: "searchValue",
			    value: this.frequencyTable.search()
			});
		    });
		});
	    }
        },
        handleEventPropertyChanged: function(source, prop) {
            if (prop.property == "searchValue") {
		this.settingSearch=true;
		this.setProperty("searchValue", prop.value);
		this.frequencyTable.search(prop.value);
		this.frequencyTable.draw();
		this.settingSearch=false;
		return;
	    }
            SUPER.handleEventPropertyChanged.call(this,source, prop);
	},

    });
}



function RamaddaFrequencyDisplay(displayManager, id, properties) {
    const SUPER = new RamaddaBaseTextDisplay(displayManager, id, DISPLAY_FREQUENCY, properties);
    let myProps = [
	{label:'Frequency'},
	{p:'orientation',ex:'vertical'},
	{p:'tableHeight',ex:'300px'},
	{p:'showPercent',ex:'false'},
	{p:'showCount',ex:'false'},
	{p:'showBars',ex:'true'},
	{p:'showBars',ex:'false'},
	{p:'showHeader',ex:'false'},
	{p:'banner',ex:'true'},
	{p:'barWidth',ex:'200'},
	{p:'clickFunction',ex:'selectother'}
    ];
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
        updateUI: function() {
            let records = this.filterData();
	    if(!records) return;
            let allFields = this.getData().getRecordFields();
            let fields = this.getSelectedFields(allFields);
            if (fields.length == 0)
                fields = allFields;
	    let summary={};
	    for (let col = 0; col < fields.length; col++) {
		let f = fields[col];
		if(!Utils.isDefined(summary[f.getId()])) {
		    summary[f.getId()] = {
			counts:{},
			values:[],
			min:null,
			max:null,
			total:0,
			numbers:[],
			field:f
		    }
		}
	    }

	    let showCount = this.getProperty("showCount",true);
	    let showPercent = this.getProperty("showPercent",true);
	    let showBars = this.getProperty("showBars",false);
	    let barWidth = +this.getProperty("barWidth",200);

            for (let rowIdx = 0; rowIdx < records.length; rowIdx++) {
                let row = this.getDataValues(records[rowIdx]);
                for (let col = 0; col < fields.length; col++) {
                    let f = fields[col];
		    let s = summary[f.getId()];
                    let value =  row[f.getIndex()];
		    if(!Utils.isDefined(s.min)) {
			s.min=value;
			s.max=value;
		    }
		    if(f.isNumeric()) {
			s.numbers.push(value);
			if(isNaN(s.max)) s.max = value;
			else if(!isNaN(value))s.max = Math.max(value,s.max);
			if(isNaN(s.min)) s.min = value;
			else if(!isNaN(value))s.min = Math.min(value,s.min);
		    } else {
			if(!Utils.isDefined(s.counts[value])) {
			    let tuple = {value:value,count:0}
			    s.counts[value] = tuple;
			    s.values.push(tuple);
			}
			s.total++;
			s.counts[value].count++;
		    }
                }
	    }
	    let html = "";
	    let bannerHtml = "";
	    for (let col = 0; col < fields.length; col++) {
		bannerHtml += "<div style='text-align:center;'>";
		let f = fields[col];
		let s = summary[f.getId()];
		//		if(col>0) html+="<br>";
		if(f.isNumeric()) {
		    let numBins = parseFloat(this.getProperty("numBins",10,true));
		    s.bins = [];
		    let range = s.max-s.min;
		    let binWidth = (s.max-s.min)/numBins;
		    let label = "Not defined";
		    s.bins.push(label);
		    let tuple = {value:label,count:0}
		    s.counts[label] = tuple;
		    s.values.push(tuple);
                    let binsProp = this.getProperty(f.getId() +".bins");
		    let hasBins = Utils.stringDefined(binsProp);
		    let binValues;
                    if(hasBins) {
                        let l  = binsProp.split(",");
			binValues = [];
			l.map(v=>binValues.push(+v));
                        for(let i=0;i<l.length-1;i++) {
			    let v1 = +l[i];
			    let v2 = +l[i+1];
			    let label = v1 +" - " + v2;
			    s.bins.push(label);
			    let tuple = {value:label,count:0}
			    s.counts[label] = tuple;
			    s.values.push(tuple);
                        }
		    } else {
			for(let i=0;i<numBins;i++) {
			    let label = (Utils.formatNumber(s.min+binWidth*i)) +" - " + Utils.formatNumber(s.min+binWidth*(i+1));
			    s.bins.push(label);
			    let tuple = {value:label,count:0}
			    s.counts[label] = tuple;
			    s.values.push(tuple);
			}
		    }
		    for(let i=0;i<s.numbers.length;i++) {
			let value = s.numbers[i];
			let bin=0;
			if(!isNaN(value)) {
			    if(binValues) {
				for(let j=0;j<binValues.length-1;j++) {
				    if(value>=binValues[j] && value< binValues[j+1]) {
					bin = j;
					break;
				    }
				}
			    } else {
				if(binWidth!=0) {
				    let perc = (value-s.min)/range;
				    bin = Math.round(perc/(1/numBins))+1;
				    if(bin>numBins) bin = numBins;
				}
			    }
			}
			let label = s.bins[bin]; 
			if(!Utils.isDefined(s.counts[label])) {
			    let tuple = {value:s.bins[bin],count:0}
			    s.counts[label] = tuple;
			    s.values.push(tuple);
			}
			s.total++;
			s.counts[label].count++;
		    }
		}

		let hor = this.getProperty("orientation","") != "vertical";
		if(this.getProperty("floatTable") !=null) {
		    hor = this.getProperty("floatTable")==true;
		}
		html += HU.openTag("div", [ATTR_CLASS,"display-frequency-table",ATTR_STYLE,hor?"":"display:block;"]);
		html += HU.openTag("table", ["cellpadding","3",ATTR_ID,this.domId("summary"+col),"table-height",this.getProperty("tableHeight","300",true), ATTR_CLASS, "stripe row-border nowrap ramadda-table"]);
		if(this.getProperty("showHeader",true)) {
		    html += HU.openTag("thead", []);
		    let label =  HU.span([TITLE,"Click to reset",ATTR_CLASS,"display-frequency-label","data-field",s.field.getId()],f.getLabel());

		    
		    label = HU.div([ATTR_STYLE,"max-width:500px;overflow-x:auto;"], label);
		    let count = showCount? HU.th(["align","right","width","20%"],HU.div([ATTR_STYLE,"text-align:right"],"Count")):"";
		    let percent  = showPercent?HU.th(["align","right","width","20%"],  HU.div([ATTR_STYLE,"text-align:right"],"Percent")):"";
		    let bars = showBars? HU.th(["align","right","width",barWidth],HU.div([ATTR_STYLE,"text-align:right"],"&nbsp;")):"";
		    html += HU.tr([], HU.th(["xxwidth","60%"],  label+ count+ percent+bars));
		    html += HU.closeTag("thead");
		}

		html += HU.openTag("tbody", []);
		let colors = this.getColorTable(true);
		let dfltColor = this.getProperty("barColor","blue");
		if(colors) {
		    for(let i=0;i<s.values.length;i++) {
			let value = s.values[i].value;
			if(i<colors.length)
			    s.values[i].color = colors[i];
			else
			    s.values[i].color = colors[colors.length-1];
		    }
		}

	    	if(!f.isNumeric()) {
		    s.values.sort((a,b)=>{
			if(a.count<b.count) return 1;
			if(a.count>b.count) return -1;
			return 0;
		    });
		}

		let maxPercent = 0;
		for(let i=0;i<s.values.length;i++) {
		    let count = s.values[i].count;
		    if(count==0) continue;
		    let perc = count/s.total;
		    maxPercent = Math.max(maxPercent, perc);
		}

//		let csv = '';
		for(let i=0;i<s.values.length;i++) {
		    let value = s.values[i].value;
		    let label = value;
		    if(label=="") label="&lt;blank&gt;";
		    let count = s.values[i].count;
		    if(count==0) continue;
		    let perc = count/s.total;
		    value = value.replace(/\'/g,"&apos;");
		    let countLabel = count
		    let color = s.values[i].color;
		    if(!color) color = dfltColor;

		    if(showPercent) countLabel+=" (" + Math.round(perc*100)+"%)";
//		    csv+=value+','+perc+'\n';

		    bannerHtml += HU.div([TITLE,"Click to select",ATTR_CLASS," display-frequency-item","data-field",s.field.getId(),"data-value",value], value +"<br>" + countLabel);
		    let tdv = HU.td([], value);
		    let tdc =  (showCount?HU.td(["align", "right"], count):"");
		    let tdp =  showPercent?HU.td(["align", "right"], s.total==0?"0":Math.round(perc*100)+"%"):"";
		    let bw = perc/maxPercent;
		    let tdb = showBars?HU.td(["valign","center","width",barWidth], HU.div([TITLE,Math.round(perc*100)+"%",ATTR_STYLE,"background:" + color+";height:10px;width:"+ (Math.round(bw*barWidth))+"px"],"")):"";
		    html += HU.tr([], 
					 tdv + tdc + tdp + tdb
					);
		}
//		Utils.makeDownloadFile('percents.csv',csv);

		html += HU.close(TBODY,TABLE,DIV);
		bannerHtml += HU.close(TD);
	    }

	    let doBanner = this.getProperty("banner",false);
	    if(doBanner) html = HU.div([ATTR_CLASS,"display-frequency-banner"], bannerHtml);
	    this.setContents(html);
	    let _this = this;
	    let cnt = 0;
	    let items = this.find(".display-frequency-item");
	    items.click(function(){
		let click = _this.getProperty("clickFunction")
		let value = $(this).attr("data-value");
		let fieldId = $(this).attr("data-field");
		let parent = $(this).parent();
		let isSelected = $(this).hasClass("display-frequency-item-selected");
		items.removeClass("display-frequency-item-selected");
		if(!isSelected) {
		    $(this).addClass("display-frequency-item-selected");
		} else {
		    value = FILTER_ALL;
		}
		if(!click || click =="select") {
		    _this.handleEventPropertyChanged(_this,{
			property: "pattern",
			fieldId: fieldId,
			value: value
		    });
		} else if(click == "selectother") {
		    _this.propagateEvent(DisplayEvent.fitlerChanged, {
			value: value,
			id:_this.getFilterId(fieldId),
			fieldId: fieldId,
		    });
		}
	    });
	    this.find(".display-frequency-label").click(function(){
		let field = $(this).attr("data-field");
		//		    _this.find("[data-field=" + field+"]").css("color","black");
		_this.handleEventFilterChanged(_this,{
		    id:ATTR_ID,
		    fieldId: field,
		    value: "-all-"
		});
	    });

	    if(this.getProperty("showHeader",true)) {
		for (let col = 0; col < fields.length; col++) {
		    HU.formatTable("#" +this.domId("summary"+col),{});
		}
	    }
	}
    });
}

function RamaddaTextanalysisDisplay(displayManager, id, properties) {
    const SUPER = new RamaddaBaseTextDisplay(displayManager, id, DISPLAY_TEXTANALYSIS, properties);
    defineDisplay(addRamaddaDisplay(this), SUPER, [], {
        checkLayout: function() {
            this.updateUIInner();
        },
        updateUI: function() {
            var includes = "<script src='" + RamaddaUtil.getCdnUrl("/lib/compromise.min.js")+"'></script>";
            this.writeHtml(ID_DISPLAY_TOP, includes);
            let _this = this;
            var func = function() {
		if(window.nlp) {
                    _this.updateUIInner();
		} else {
		    setTimeout(func, 100);
		}
            };
            setTimeout(func, 100);
        },
        updateUIInner: function() {
            let records = this.filterData();
            if (!records) {
                return null;
            }
            let _this = this;
            this.setDisplayMessage("Processing text...");
            var func = function() {
                _this.updateUIInnerInner();
            };
            setTimeout(func, 10);
        },
        updateUIInnerInner: function() {
            let records = this.filterData();
            var allFields = this.getData().getRecordFields();
            var fields = this.getSelectedFields(allFields);
            if (fields.length == 0)
                fields = allFields;
	    var strings = this.getFieldsByIds(fields,"fields");
            if (strings.length == 0) {
		strings = this.getFieldsByType(fields, "string");
	    }
            if (strings.length == 0) {
                this.displayError("No string fields specified");
                return null;
            }
	    if(!this.lastRecords || this.lastRecords.length!= records.length) {
		this.lastRecords = records;
		var corpus = "";
		for (var rowIdx = 0; rowIdx < records.length; rowIdx++) {
                    var row = this.getDataValues(records[rowIdx]);
                    var line = "";
                    for (var col = 0; col < strings.length; col++) {
			var f = fields[col];
			line += " ";
			line += row[f.getIndex()];
                    }
                    corpus += line;
		    corpus += "\n";
		}
		//		console.log("corpus:" + corpus.length +"\n" + corpus.substring(0,1000));
		this.nlp = window.nlp(corpus);
		//		console.log("after");
	    }
	    var nlp = this.nlp;
            var cols = [];
            if (this.getProperty("showPeople", false)) {
                cols.push(this.printList("People", nlp.people().out('topk')));
            }
            if (this.getProperty("showPlaces", false)) {
                cols.push(this.printList("Places", nlp.places().out('topk')));
            }
            if (this.getProperty("showOrganizations", false)) {
                cols.push(this.printList("Organizations", nlp.organizations().out('topk')));
            }
            if (this.getProperty("showTopics", false)) {
                cols.push(this.printList("Topics", nlp.topics().out('topk')));
            }
            if (this.getProperty("showNouns", false)) {
                cols.push(this.printList("Nouns", nlp.nouns().out('topk')));
            }
            if (this.getProperty("showVerbs", false)) {
                cols.push(this.printList("Verbs", nlp.verbs().out('topk')));
            }
            if (this.getProperty("showAdverbs", false)) {
                cols.push(this.printList("Adverbs", nlp.adverbs().out('topk')));
            }
            if (this.getProperty("showAdjectives", false)) {
                cols.push(this.printList("Adjectives", nlp.adjectives().out('topk')));
            }
            if (this.getProperty("showClauses", false)) {
                cols.push(this.printList("Clauses", nlp.clauses().out('topk')));
            }
            if (this.getProperty("showContractions", false)) {
                cols.push(this.printList("Contractions", nlp.contractions().out('topk')));
            }
            if (this.getProperty("showPhoneNumbers", false)) {
                cols.push(this.printList("Phone Numbers", nlp.phoneNumbers().out('topk')));
            }
            if (this.getProperty("showValues", false)) {
                cols.push(this.printList("Values", nlp.values().out('topk')));
            }
            if (this.getProperty("showAcronyms", false)) {
                cols.push(this.printList("Acronyms", nlp.acronyms().out('topk')));
            }
            if (this.getProperty("showNGrams", false)) {
                cols.push(this.printList("NGrams", nlp.ngrams().out('topk')));
            }
            if (this.getProperty("showDates", false)) {
                cols.push(this.printList("Dates", nlp.dates().out('topk')));
            }
            if (this.getProperty("showQuotations", false)) {
                cols.push(this.printList("Quotations", nlp.quotations().out('topk')));
            }
            if (this.getProperty("showUrls", false)) {
                cols.push(this.printList("URLs", nlp.urls().out('topk')));
            }
            if (this.getProperty("showStatements", false)) {
                cols.push(this.printList("Statements", nlp.statements().out('topk')));
            }
            if (this.getProperty("showTerms", false)) {
                cols.push(this.printList("Terms", nlp.terms().out('topk')));
            }
            if (this.getProperty("showPossessives", false)) {
                cols.push(this.printList("Possessives", nlp.possessives().out('topk')));
            }
            if (cols.length == 0) {
                this.setDisplayMessage("No text types specified");
                return;
            }
            var height = this.getProperty("height", "400");
            var html = HU.openTag("div", [ATTR_ID, this.domId("tables")]);

            for (var i = 0; i < cols.length; i += 3) {
                var c1 = cols[i];
                var c2 = i + 1 < cols.length ? cols[i + 1] : null;
                var c3 = i + 2 < cols.length ? cols[i + 2] : null;
                var width = c2 ? (c3 ? "33%" : "50%") : "100%";
                var style = "padding:5px";
                var row = "";
                row += HU.td(["width", width], HU.div([ATTR_STYLE, style], c1));
                if (c2)
                    row += HU.td(["width", width], HU.div([ATTR_STYLE, style], c2));
                if (c3)
                    row += HU.td(["width", width], HU.div([ATTR_STYLE, style], c3));
                html += HU.tag("table", ["width", "100%"], HU.tr(row));
            }
            html += HU.closeTag("div");
            this.setContents(html);
            HU.formatTable("#" + this.domId("tables") + " .ramadda-table", {
                scrollY: this.getProperty("tableHeight", "200")
            });
        },
        printList: function(title, l) {
            var maxWords = parseInt(this.getProperty("maxWords", 10));
            var minCount = parseInt(this.getProperty("minCount", 0));
            var table = HU.openTag("table", ["width", "100%", ATTR_CLASS, "stripe hover ramadda-table"]) + HU.openTag("thead", []);
            table += HU.tr([], HU.th([], title) + HU.th([], "&nbsp;"));
            table += HU.close(THEAD);
            table += HU.open(TBODY);
            var cnt = 0;
            for (var i = 0; i < l.length; i++) {
                if (l[i].count < minCount) continue;
                var row = HU.td([], l[i].normal) +
                    HU.td([], l[i].count + " (" + l[i].percent + "%)");
                table += HU.tr([], row);
                if (cnt++ > maxWords) break;
            }
            table += HU.close(TBODY,TABLE);
            return table;
        }
    });
}


function RamaddaTextrawDisplay(displayManager, id, properties) {
    const ID_TEXT = "text";
    const ID_OVERLAY = "overlay";
    const ID_OVERLAY_TABLE = "overlaytable";
    const ID_LABEL = "label";
    const ID_SEARCH = "search";
    const ID_HIGHLIGHT = "highlight"; 
    const ID_SHRINK = "shrink";
    const SUPER = new RamaddaBaseTextDisplay(displayManager, id, DISPLAY_TEXTRAW, properties);
    let myProps = [
	{label:'Raw Text'},
	{p:'doBubble',ex:'true'},
	{p:'addLineNumbers',ex:'false'},
	{p:'labelTemplate',ex:'${lineNumber}'},
	{p:'maxLines',ex:'1000'},
	{p:'pattern',ex:'initial search pattern'},
	{p:'fromField',ex:''},
	{p:'linesDescriptor',ex:''},
	{p:'asHtml',ex:'false'},
	{p:'breakLines',ex:'true'},
	{p:'includeEmptyLines',ex:'false'},
    ];
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
	doShrink: properties["initialShrink"],
        checkLayout: function() {
            this.updateUI();
        },
        updateUI: function() {
            let records = this.filterData();
            if (!records) {
                return null;
            }
            let pointData = this.getData();
            this.allRecords = pointData.getRecords();
            let pattern = this.getProperty("pattern");
            if (pattern && pattern.length == 0) pattern = null;
	    if(pattern) pattern = pattern.replace(/"/g,"&quot;");
	    let input = "";
	    if(!this.filters || this.filters.length==0) 
		input += " " + HU.input("pattern", (pattern ? pattern : "") ,
					[ATTR_PLACEHOLDER, "Search text",
					 ATTR_ID, this.domId(ID_SEARCH)]);
	    this.showShrink = this.getProperty("showShrink",false);
	    if(this.showShrink) {
		input += " " + HU.checkbox("shrink",[ATTR_ID,this.domId(ID_SHRINK)], this.getProperty("initialShrink", true)) +" Shrink ";
	    }

            this.writeHtml(ID_TOP_RIGHT, HU.span([ATTR_ID,this.domId(ID_LABEL)]," ") + input);
            let _this = this;
	    this.jq(ID_SHRINK).click(function() {
		_this.doShrink = _this.jq(ID_SHRINK).is(':checked');
		_this.setProperty("initialShrink",_this.doShrink);
		_this.updateUI();
	    });
            this.jq(ID_SEARCH).keypress(function(event) {
                if (event.which == 13) {
                    _this.setProperty("pattern", $(this).val());
		    _this.propagateEvent(DisplayEvent.propertyChanged, {
			property: "pattern",
			value: $(this).val()
		    });
                    _this.updateUI();
                }
            });
            var height = this.getProperty("height", "600");
	    var style = this.getProperty("displayInnerStyle","");
            var html = HU.div([ATTR_ID, this.domId(ID_TEXT), ATTR_STYLE, "padding:4px;border:1px #ccc solid; max-height:" + height + "px;overflow-y:auto;" + style]);
            this.setContents(html);
	    let t1 = new Date();
            this.showText();
	    let t2 = new Date();
//	    Utils.displayTimes("T",[t1,t2]);
        },
        handleEventPropertyChanged: function(source, prop) {
            if (prop.property == "pattern") {
		this.setProperty("pattern", prop.value);
		this.updateUI();
		return;
	    }
            SUPER.handleEventPropertyChanged.call(this,source, prop);
	},

        showText: function() {
	    let _this  = this;
            let records = this.filterData();
            if (!records) {
                return null;
            }
	    this.records = records;
 	    this.recordToIndex = {};
	    this.indexToRecord = {};
            let pattern = this.getProperty("pattern");
            if (pattern && pattern.length == 0) pattern = null;
	    let asHtml = this.getProperty("asHtml", true);
            let addLineNumbers = this.getProperty("addLineNumbers", true);
	    let labelTemplate = this.getProperty("labelTemplate","");
	    let labelWidth = "10px";
	    if(labelTemplate == "") {
		labelWidth = "1px";
	    }
	    if(labelTemplate == "" && addLineNumbers) {
		labelTemplate = "${lineNumber}";
	    }

            if (labelTemplate) asHtml = true;
            var maxLines = parseInt(this.getProperty("maxLines", 100000));
            var lineLength = parseInt(this.getProperty("lineLength", 10000));
            var breakLines = this.getProperty("breakLines", true);


            var includeEmptyLines = this.getProperty("includeEmptyLines", false);
            var allFields = this.getData().getRecordFields();
            var fields = this.getSelectedFields(allFields);
            if (fields.length == 0)
                fields = allFields;
            var strings = this.getFieldsByType(fields, "string");
            if (strings.length == 0) {
                this.displayError("No string fields specified");
                return null;
            }
	    var highlights;
	    var highlightStyles;
	    if(this.getProperty("highlights")) {
		highlights=[];
		highlightStyles = this.getProperty("highlightStyles","background:rgb(250_comma_0_comma_0);").split(",");
		this.getProperty("highlights","").split(",").map(h=>{
		    if(h.indexOf("(")<0) h = "(" + h +")";
		    highlights.push(RegExp(h,'ig'));
		});
	    }

            var corpus = HU.openTag("div", [ATTR_STYLE,"position:relative;"]);
	    corpus+=HU.div([ATTR_ID,this.domId(ID_OVERLAY),ATTR_STYLE,"position:absolute;top:0;left:0;"],
				  HU.tag("table",[ATTR_ID,this.domId(ID_OVERLAY_TABLE)]));

	    var fromField = this.getFieldById(null,this.getProperty("fromField"));
	    var bubble=this.getProperty("doBubble",false);
            if (labelTemplate) {
                corpus += "<table width=100%>";
            }
            var lineCnt = 0;
            var displayedLineCnt = 0;
	    var patternMatch = new TextMatcher(pattern);
	    var regexpMaps = {};
	    var filterFieldMap = {};
	    if(this.filters) {
		this.filters.map(f=>{if(f.field && f.field.isString)filterFieldMap[f.field.getId()]=f;});
	    }
	    var templates = {};
	    fields.map(f=>{
		templates[f.getId()] = this.getProperty(f.getId() +".template");
	    });
            var colorBy = this.getColorByInfo(records);
	    var delimiter = this.getProperty("delimiter","");
	    var rowScale = this.showShrink?this.getProperty("rowScale",0.3):null;

	    if(this.showShrink) {
		corpus+=HU.tr([],HU.td([],HU.getIconImage("fa-caret-down")));
	    }
	    let templateFields = this.getFields();
	    let templateProps = this.getTemplateProps(templateFields);	    
	    let templateMacros = Utils.tokenizeMacros(labelTemplate?labelTemplate:"");
            for (var rowIdx = 0; rowIdx < records.length; rowIdx++) {
		var record = records[rowIdx];
		if(!Utils.isDefined(record.lineNumber)) {
		    record.lineNumber = (rowIdx+1);
		}
		this.indexToRecord[rowIdx] = record;
		this.recordToIndex[record.getId()] = rowIdx;
                var row = this.getDataValues(record);
                var line = "";
                for (var col = 0; col < fields.length; col++) {
                    var f = fields[col];
		    if(rowIdx==0) {
			if(filterFieldMap[f.getId()]) {
			    let filter = filterFieldMap[f.getId()];
			    var value = filter.getFieldValues();
			    if(value) {
				if(!Array.isArray(value)) {
				    value = [value];
				}
				try {
				    regexpMaps[f.getId()] =  [];
				    value.map(v=>{
					if(v == "" || v == FILTER_ALL) return;
					var re = new TextMatcher(v);
					regexpMaps[f.getId()].push(re);
				    })
				} catch(e) {console.log("Error making regexp:" + e);}
			    }
			}
		    }
		    var value = ""+row[f.getIndex()];
                    value = value.replace(/</g, "&lt;").replace(/>/g, "&gt;");
		    if(regexpMaps[f.getId()]) {
			regexpMaps[f.getId()].map(re=>{
			    value  = re.highlight(value);
			});
		    }
		    
		    if(line!="") 
			line += delimiter+" ";
		    if(templates[f.getId()]) {
			value = templates[f.getId()].replace("${value}",value);
		    }
                    line += value;
                }
                line = line.trim();
                if (!includeEmptyLines && line.length == 0) continue;
                lineCnt++;
		var rowAttrs =["valign", "top"];
		var rowStyle="";
                if (colorBy.index >= 0) {
		    var value = record.getData()[colorBy.index];
		    var color =  colorBy.getColor(value, record);
		    if(color) {
			rowAttrs.push(ATTR_STYLE);
			rowStyle +="background:" + Utils.addAlphaToColor(color,"0.25")+";";
		    }
                }
		rowAttrs.push(ATTR_CLASS);
		rowAttrs.push("display-raw-row");
		let matches=patternMatch.matches(line);
		let hasMatch = matches && patternMatch.hasPattern();
		if(hasMatch) {
		    rowAttrs.push("matched");
		    rowAttrs.push(true);
		}
		if(rowScale) {
		    if(!hasMatch) {
			rowStyle += "-webkit-transform: scale(1," + rowScale +");";
			rowStyle += "line-height:"+ rowScale +";";
			rowAttrs.push(ATTR_STYLE);
			rowAttrs.push(rowStyle);
		    }
		} else  if(!matches) {
		    continue;
		}
                line = patternMatch.highlight(line);
                displayedLineCnt++;
                if (displayedLineCnt > maxLines) break;
		let lineAttrs = [TITLE," ",ATTR_CLASS, " display-raw-line ",RECORD_INDEX,rowIdx]
		if(bubble) line = HU.div([ATTR_CLASS,"ramadda-bubble"],line);
		if(fromField) line+=HU.div([ATTR_CLASS,"ramadda-bubble-from"],  ""+row[fromField.getIndex()]);

		if(highlights) {
		    for(var hi=0;hi<highlights.length;hi++) {
			var h = highlights[hi];
			var s = hi<highlightStyles.length?highlightStyles[hi]:highlightStyles[highlightStyles.length-1];
			s = s.replace(/_comma_/g,",");
			line= line.replace(h, "<span style='" + s +"'>$1</span>");
		    }
		}
		line = HU.div(lineAttrs,line);
                if (labelTemplate) {
		    let row = this.getDataValues(record);
		    let label = this.applyRecordTemplate(record, row, templateFields,labelTemplate, templateProps,templateMacros);
		    var num = record.lineNumber;
		    if(!Utils.isDefined(num)) {
			num - lineCnt;
		    }
		    label = label.replace("${lineNumber}", "#" +(num));
		    label = label.replace(/ /g,"&nbsp;");
		    var r =  "";
		    if(this.showShrink) {
			r+= HU.td([WIDTH, "5px",ATTR_STYLE,HU.css('background','#ccc')],  HU.getIconImage("fa-caret-right",null, [ATTR_STYLE,"line-height:0px;"]));
		    }
		    r+= HU.td([WIDTH, labelWidth], "<a name=line_" + lineCnt + "></a>" +
				     "<a href=#line_" + lineCnt + ">" + label + "</a>&nbsp;  ") +
			HU.td([], line);
		    corpus += HU.tr(rowAttrs, r);
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
                corpus += HU.close(TABLE);
            }
            corpus+= HU.close(DIV);

            if (!asHtml)
                corpus = HU.tag(PRE, [], corpus);
            this.writeHtml(ID_TEXT, corpus);
	    colorBy.displayColorTable();
	    var linesWord = " "+ this.getProperty("linesDescriptor","lines");
	    var label =displayedLineCnt +linesWord;
	    if(this.allRecords.length!=displayedLineCnt) {
		label = displayedLineCnt+"/" + this.allRecords.length+linesWord+" (" + Math.round(displayedLineCnt/this.allRecords.length*100)+"%)";
	    }
	    this.jq(ID_LABEL).html(label);
	    this.jq(ID_SEARCH).focus();
	    if(rowScale) {
		var rows =  this.jq(ID_TEXT).find(".display-raw-row");
		var open = function() {
		    $(this).css("transform","scaleY(1)");		    
		    $(this).css("line-height","1.5");
		    $(this).css("border-bottom","1px solid #ccc");
		    $(this).css("border-top","1px solid #ccc");
		};
		var close = function() {
		    var row = this;
		    if(!$(row).attr("matched")) {	
			$(row).css("transform","scaleY(" + rowScale +")");
			$(row).css("line-height",rowScale);
			$(row).css("border-bottom","0px solid #ccc");
			$(row).css("border-top","0px solid #ccc");
		    }
		}
		rows.each(close);
		rows.mouseenter(open);
		rows.mousemove(open);
		rows.mouseout(close);
	    }
	    let lines =this.jq(ID_TEXT).find(".display-raw-line");
	    lines.click(function() {
		var idx = $(this).attr(RECORD_INDEX);
		var record = _this.indexToRecord[idx];
		if(record) {
		    _this.showRecordPopup($(this),record);
		    _this.propagateEventRecordSelection({record: record});
		    _this.highlightLine(idx);
		}
	    });
	    this.makeTooltips(lines,records);
        },
	highlightLine: function(index) {
	    var container = this.jq(ID_TEXT);
	    container.find(".display-raw-line").removeClass("display-raw-line-selected");
	    var sel = HU.attrSelect(RECORD_INDEX,index);
	    var element =  container.find(sel);
	    element.addClass("display-raw-line-selected");

	},
        handleEventRecordSelection: function(source, args) {
	    var index = this.findMatchingIndex(args.record).index;
	    if(index<0 || !Utils.isDefined(index)) {
		return;
	    }
	    this.highlightLine(index);
	    var container = this.jq(ID_TEXT);
	    var sel = HU.attrSelect(RECORD_INDEX,index);
	    var element =  container.find(sel);
	    var c = container.offset().top;
	    var s = container.scrollTop();
	    var eo = element.offset();
	    var diff = eo.top- c + s;
	    container.scrollTop(diff)
	},
    });
}


function RamaddaTextDisplay(displayManager, id, properties) {
    const SUPER = new RamaddaDisplay(displayManager, id, DISPLAY_TEXT, properties);
    let myProps = [
	{label:'Text Display'},
	{p:'recordTemplate',ex:''},
	{p:'showDefault',d:true,ex:"false"},
	{p:'message',d:null,ex:""},
    ];
    if(!properties["recordTemplate"]) {
	properties["recordTemplate"] = "${default}";
    }

    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
        needsData: function() {
            return true;
        },
	handleAnnotation: function() {
	    let annotation = null;
	    let template = this.getProperty("template","${date} - ${description}");
	    let  macros = Utils.tokenizeMacros(template);
	    let attrs = {};
	    let date = null;
	    if(this.selectedRecord) {
		date = this.selectedRecord.getDate()
		if(date)
		    annotation = this.annotations.getAnnotationFromDate(date);
	    }
	    if(!annotation) {
		date=this.getAnimation().getBeginTime()
		if(date)
		    annotation = this.annotations.getAnnotationFromDate(date);
	    }
	    if(date) {
		attrs.date = date;
	    }  else {
		attrs.date = "";
	    }
	    if(!annotation) {
//		this.setContents("");
		return;
	    }
	    attrs.description = annotation.description;
	    attrs.label = annotation.label;
	    if(this.annotations.getShowLegend()) {
		attrs.legend  = this.annotations.getLegend();
	    } else {
		attrs.legend  = "";
	    }
	    let html =  macros.apply(attrs);
	    if(this.getProperty("decorate",true)) {
		html = this.getMessage(html);
	    }
	    this.setContents(html);
	},
	updateUI: function() {
            SUPER.updateUI.call(this);
	    if(this.getProperty("annotations")) {
		let pointData = this.getPointData();
		if (pointData == null) return;
		if(!this.annotations) {
		    this.annotations  = new Annotations(this,this.filterData());
		} 
		if(this.annotations.isEnabled()) {
		    this.handleAnnotation();
		}
	    }
	    if(this.selectedRecord) {
		this.setContents(this.getRecordHtml(this.selectedRecord));
	    } else  if(this.getPropertyShowDefault()) {
		this.recordMap = {};
		let records = this.filterData();
		if(records && records.length>0) {
		    records.forEach((record,idx)=>{
			this.recordMap[record.getId()] = record;
		    });
		    this.selectedRecord =records[0];
		    this.setContents(this.getRecordHtml(records[0]));
		}
	    } else  if(this.getPropertyMessage()) {
		this.setDisplayMessage(this.getPropertyMessage());
	    }
        },
        pointDataLoaded: function(pointData, url, reload) {
	    this.selectedRecord= null;
	    SUPER.pointDataLoaded.call(this, pointData,url,reload);
	},
        handleEventRecordSelection: function(source, args) {
	    if(this.recordMap) {
		if(!this.recordMap[args.record.getId()]) {
		    return;
		}
	    }
	    this.selectedRecord= args.record;
	    this.updateUI();
        }
    });
}




function RamaddaGlossaryDisplay(displayManager, id, properties) {
    const SUPER =  new RamaddaFieldsDisplay(displayManager, id, DISPLAY_GLOSSARY, properties);
    const ID_GLOSSARY_HEADER = "glossary_header";
    let myProps = [
	{label:'Glossary'},
	{p:'wordField',ex:""},
	{p:'definitionField',ex:""},	
    ];

    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
        updateUI: function() {
	    let records = this.filterData();
	    if(!records) return;
	    let wordField = this.getFieldById(null,this.getProperty("wordField"));
	    let definitionField = this.getFieldById(null,this.getProperty("definitionField"));	    
	    if(!wordField) {
                this.displayError("No word field specified");
                return;
	    }
	    if(!definitionField) {
                this.displayError("No definition field specified");
                return;
	    }	    
	    let letters = {};
	    records.forEach(record=>{
		let word = String(wordField.getValue(record)).trim();
		let definition = definitionField.getValue(record);
		let letter = word.substring(0,1).toUpperCase();
		let list = letters[letter] || (letters[letter] = []);
		list.push({word:word,definition:definition, record:record});
	    });
	    let highlight  = this.getFilterTextMatchers();
	    let header =  HU.div([ATTR_CLASS,"display-glossary-letter"], "All");
	    let html = "";
	    Object.keys(letters).sort().forEach(letter=>{
		let _letter = letter.trim();
		_letter = _letter==""?"_":_letter;
		let clazz = " display-glossary-letter ";
		if(this.searchLetter == _letter) {
		    clazz+=" display-glossary-letter-highlight ";
		}
		header += HU.div([ATTR_CLASS,clazz,"letter",_letter], _letter);
		if(this.searchLetter && this.searchLetter!=_letter) return;
		let group =  HU.div([ATTR_CLASS,"display-glossary-group-header"],  _letter) +
		    HU.openTag(DIV,[ATTR_CLASS,"display-glossary-group-inner"]);
		letters[letter].sort((a,b)=>{
		    return a.word.localeCompare(b.word);
		}).forEach(info=>{
		    let def = String(info.definition);
		    highlight.forEach(h=>{
			def  = h.highlight(def);
		    });

		    let entry  = HU.div([ATTR_CLASS,"display-glossary-word"], info.word) + HU.div([ATTR_CLASS,"display-glossary-definition"], def); 
		    group+=HU.div([TITLE,"",ATTR_CLASS,"display-glossary-entry",RECORD_ID,info.record.getId()],entry);
		});
		group += HU.closeTag(DIV);
		html+=group;
	    });

	    let height = this.getProperty("glossaryHeight","600px");
	    header = HU.div([ATTR_ID,this.domId(ID_GLOSSARY_HEADER), ATTR_CLASS,"display-glossary-header"], header);
	    html = HU.div([ATTR_STYLE,HU.css("max-height",HU.getDimension(height),"overflow-y","auto")], html);
	    this.setContents(header  + html);
	    let _this = this;
	    this.jq(ID_GLOSSARY_HEADER).find(".display-glossary-letter").click(function() {
		_this.searchLetter =  $(this).attr("letter");
		_this.forceUpdateUI();
	    });
	    this.makeTooltips(this.find(".display-glossary-entry"),records);
	},
    });
}




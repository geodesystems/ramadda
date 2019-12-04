/*
   Copyright 2008-2019 Geode Systems LLC
*/


var DISPLAY_WORDCLOUD = "wordcloud";
var DISPLAY_TEXTSTATS = "textstats";
var DISPLAY_FREQUENCY = "frequency";
var DISPLAY_TEXTANALYSIS = "textanalysis";
var DISPLAY_TEXTRAW = "textraw";
var DISPLAY_TEXT = "text";
var DISPLAY_CARDS = "cards";
var DISPLAY_BLOCKS = "blocks";
var DISPLAY_TREE = "tree";
var DISPLAY_TEMPLATE = "template";
var DISPLAY_SLIDES = "slides";
var DISPLAY_IMAGES = "images";
var DISPLAY_BLANK = "blank";
var DISPLAY_TOPFIELDS = "topfields";
var DISPLAY_TIMELINE = "timeline";
var DISPLAY_PERCENTCHANGE = "percentchange";



addGlobalDisplayType({
    type: DISPLAY_TEXT,
    label: "Text Readout",
    requiresData: true,
    forUser: true,
    category: CATEGORY_MISC
});


addGlobalDisplayType({
    type: DISPLAY_CARDS,
    label: "Cards",
    requiresData: true,
    forUser: true,
    category: CATEGORY_MISC
});
addGlobalDisplayType({
    type: DISPLAY_IMAGES,
    label: "Images",
    requiresData: true,
    forUser: true,
    category: CATEGORY_MISC
});

addGlobalDisplayType({
    type: DISPLAY_TEMPLATE,
    label: "Template",
    requiresData: true,
    forUser: true,
    category: "Text"
});

addGlobalDisplayType({
    type: DISPLAY_SLIDES,
    label: "Slides",
    requiresData: true,
    forUser: true,
    category: "Text"
});

addGlobalDisplayType({
    type: DISPLAY_BLANK,
    label: "Blank",
    requiresData: true,
    forUser: true,
    category: CATEGORY_MISC
});


addGlobalDisplayType({
    type: DISPLAY_TOPFIELDS,
    label: "Top Fields",
    requiresData: true,
    forUser: true,
    category: CATEGORY_MISC
});


addGlobalDisplayType({
    type: DISPLAY_TIMELINE,
    label: "Timeline",
    requiresData: true,
    forUser: true,
    category: CATEGORY_MISC
});

addGlobalDisplayType({
    type: DISPLAY_PERCENTCHANGE,
    label: "Percent Change",
    requiresData: true,
    forUser: true,
    category: CATEGORY_MISC
});



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
    type: DISPLAY_FREQUENCY,
    forUser: true,
    label: "Frequency",
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

addGlobalDisplayType({
    type: DISPLAY_BLOCKS,
    forUser: true,
    label: "Blocks",
    requiresData: false,
    category: "Other Charts"
});


addGlobalDisplayType({
    type: DISPLAY_TREE,
    forUser: true,
    label: "Tree",
    requiresData: false,
    category: "Text"
});



function RamaddaBaseTextDisplay(displayManager, id, type, properties) {
    var ID_TEXTBLOCK = "textblock";
    let SUPER = new RamaddaFieldsDisplay(displayManager, id, type, properties);
    RamaddaUtil.inherit(this, SUPER);
    $.extend(this, {
        processText: function(cnt) {
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
			values[0] = values[0].replace(/\"/g," ");
                        values = Utils.tokenizeWords(values[0], stopWords, extraStopWords, removeArticles);
                    }
                    for (var valueIdx = 0; valueIdx < values.length; valueIdx++) {
                        var value = values[valueIdx].trim();
			if(value.length<=1) continue;
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

                var counts = [];
                for (word in fi.counts) {
                    var count = fi.counts[word];
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

                for (var wordIdx = 0; wordIdx < counts.length; wordIdx++) {
                    var word = counts[wordIdx];
                    var obj1 = {
                        weight: word.count,
                        handlers: handlers,
                        text: word.word,
                    };
                    var obj2 = {
                        weight: word.count,
                        handlers: handlers,
                        text: field.getLabel() + ":" + word.word,
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
            var tokenize = this.getProperty("tokenize", false);
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
            var re = new RegExp("(\\b" + word + "\\b)", 'i');
            for (var rowIdx = 0; rowIdx < records.length; rowIdx++) {
                var row = this.getDataValues(records[rowIdx]);
                var value = "" + row[field.getIndex()];
                if (tokenize) {
                    if (!value.match(re)) {
                        continue;
                    }
                } else {
                    if (word != value) {
                        continue;
                    }
                }
                var tuple = [];
                data.push(tuple);
                for (var col = 0; col < fields.length; col++) {
                    var f = fields[col];
                    if (tableFields && !tableFields[f.getId()]) continue;
                    var v = row[f.getIndex()];
                    if (tokenize) {
                        v = v.replace(re, "<span style=background:yellow;>$1</span>");
                    }
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
                var prefix = "";
                if (!tokenize) {
                    prefix = field.getLabel() + "=" + word
                }
                this.writeHtml(ID_DISPLAY_BOTTOM, prefix + HtmlUtils.div(["id", this.getDomId("table"), "style", "height:300px"], ""));
                var dataTable = google.visualization.arrayToDataTable(data);
                this.chart = new google.visualization.Table(document.getElementById(this.getDomId("table")));
                this.chart.draw(dataTable, {
                    allowHtml: true
                });
            }
        }
    });
}



function RamaddaCardsDisplay(displayManager, id, properties) {
    var ID_RESULTS = "results";
    let SUPER =  new RamaddaFieldsDisplay(displayManager, id, DISPLAY_CARDS, properties);
    Utils.importJS(ramaddaBaseUrl + "/lib/color-thief.umd.js",
		   () => {},
		   (jqxhr, settings, exception) => {
		       console.log("err");
		   });

    
    RamaddaUtil.inherit(this,SUPER);
    addRamaddaDisplay(this);
    $.extend(this, {
	getWikiEditorTags: function() {
	    return Utils.mergeLists(SUPER.getWikiEditorTags(),
				    [
					"label:Cards Attributes",
					"groupByFields=\"\"",
					"groupBy=\"\"",
					"tooltipFields=\"\"",
					"initGroupFields=\"\"",
					"captionTemplate=\"${name}\"",
					"sortFields=\"\"",
					"labelField=\"\"",
					'imageWidth="100"',
					'imageMargin="5"',
				    ])},
        getContentsStyle: function() {
            return "";
        },
        updateUI: function() {
            this.colorAnalysisEnabled = this.getProperty("doColorAnalysis");
            var pointData = this.getData();
            if (pointData == null) return;
            var allFields = pointData.getRecordFields();
	    var fields = this.getSelectedFields(allFields);
            if (fields == null || fields.length == 0) {
                fields = allFields;
	    }
            var records = this.filterData();
            if(!records) return;
	    let theFields = fields;
	    this.initGrouping  = this.getFieldsByIds(fields, this.getProperty("initGroupFields","",true));
            this.groupByFields = this.getFieldsByIds(fields, this.getProperty("groupByFields","",true));
            this.groupByMenus= +this.getProperty("groupByMenus",this.groupByFields.length);
            this.imageField = this.getFieldOfType(fields, "image");
            this.urlField = this.getFieldOfType(fields, "url");
            this.tooltipFields = this.getFieldsByIds(fields, this.getProperty("tooltipFields","",true));
            this.labelFields = this.getFieldsByIds(fields, this.getProperty("labelFields", null, true));
	    if(this.labelFields.length==0) {
		var tmp = this.getFieldById(fields,this.getProperty("labelField", null, true));
		if(tmp) {
		    this.labelFields.push(tmp);
		}
	    }
            this.onlyShowImages =this.getProperty("onlyShowImages", false);
            this.altLabelField = this.getFieldById(fields, this.getProperty("altLabelField", null, true));
            this.captionFields = this.getFieldsByIds(fields, this.getProperty("captionFields", "", true));
            this.captionTemplate = this.getProperty("captionTemplate",null, true);
            if(this.captionFields.length==0) this.captionFields = this.tooltipFields;
            this.colorByField = this.getFieldById(fields, this.getProperty("colorBy", null, true));
            this.colorList = this.getColorTable(true);
            this.foregroundList = this.getColorTable(true,"foreground");
            if(!this.getProperty("showImages",true)) this.imageField = null;

            if(!this.imageField)  {
                if(this.captionFields.length==0) {
                    this.displayError("No image or caption fields specified");
                    return;
                }
            }
            var contents = "";

	    if(!this.groupByHtml) {
		this.groupByHtml = "";
		if(this.colorAnalysisEnabled)
		    this.groupByHtml +=  HtmlUtils.span(["class","ramadda-button","id",this.getDomId("docolors")], "Do colors")+" " +
		    HtmlUtils.span(["class","ramadda-button","id",this.getDomId("docolorsreset")], "Reset");
		if(this.groupByFields.length>0) {
		    var options = [["","--"]];
		    this.groupByFields.map(field=>{
			options.push([field.getId(),field.getLabel()]);
		    });

		    this.groupByHtml +=  HtmlUtils.span(["class","display-fitlerby-label"], " Group by: ");
		    for(var i=0;i<this.groupByMenus;i++) {
			var selected = "";
			if(i<this.initGrouping.length) {
			    selected = this.initGrouping[i].getId();
			}
			this.groupByHtml+= HtmlUtils.select("",["id",this.getDomId(ID_GROUPBY_FIELDS+i)],options,selected)+"&nbsp;";
		    }
		    this.groupByHtml+="&nbsp;";
		    this.jq(ID_HEADER1).html(HtmlUtils.div(["class","display-filterby"],this.groupByHtml));
		    this.jq("docolors").button().click(()=>{
			this.analyzeColors();
		    });
		    this.jq("docolorsreset").button().click(()=>{
			this.updateUI();
		    });


		}
	    }



            contents += HtmlUtils.div(["id",this.getDomId(ID_RESULTS)]);
            this.writeHtml(ID_DISPLAY_CONTENTS, contents);
            let _this = this;
            this.jq(ID_HEADER1).find("input, input:radio,select").change(function(){
                _this.updateUI();
            });

            this.displaySearchResults(records,theFields);
        },
	analyzeColors: function() {
	    if(!window["ColorThief"]) {
		setTimeout(()=>this.analyzeColors(),1000);
		return;
	    }
	    const colorThief = new ColorThief();
	    var cnt = 0;
	    while(true) {
		var img = document.querySelector('#' + this.getDomId("gallery")+"img" + cnt);
		var div = $('#' + this.getDomId("gallery")+"div" + cnt);
		cnt++;
		if(!img) {
		    return;
		    
		}
		img.crossOrigin = 'Anonymous';
		// Make sure image is finished loading
		//		    if (img.complete) {
		var c = colorThief.getColor(img);
		var p = colorThief.getPalette(img);
		var width = img.width/p.length;
		var html = "";
		for(var i=0;i<p.length;i++) {
		    var c = p[i];
		    html+=HtmlUtils.div(["style","display:inline-block;width:" + width + "px;height:" + img.height +"px;background:rgb(" + c[0]+"," + c[1] +"," + c[2]+");"],"");
		}
		div.css("width",img.width);
		div.css("height",img.height);
		div.html(html);
		//			div.css("background","rgb(" + c[0]+"," + c[1] +"," + c[2]);
		img.style.display = "none";
	    }
	},
	displaySearchResults: function(records, fields) {
	    records= this.sortRecords(records);
            var fontSize = this.getProperty("fontSize",null);
            var cardStyle = this.getProperty("cardStyle",null);

            var width = this.getProperty("imageWidth","50");
            var margin = this.getProperty("imageMargin","0");
            var groupFields = [];
            var seen=[];
            for(var i=0;i<this.groupByMenus;i++) {
                var id =  this.jq(ID_GROUPBY_FIELDS+i).val();
                if(!seen[id]) {
                    seen[id] = true;
                    var field= this.getFieldById(fields, id);
                    if(field) {
                        groupFields.push(field);
                        if(field.isNumeric() && !field.range) {
                            var min = Number.MAX_VALUE;
                            var max = Number.MIN_VALUE;
                            records.map(r=>{
                                r  =  this.getDataValues(r);
                                var v =field.getValue(r);
                                if(isNaN(v)) return;
                                if(v<min) min  = v;
                                if(v > max) max =v;
                            });
                            field.range = [min,max];
                            var binsProp = this.getProperty(field.getId() +".bins");
                            field.bins = [];
                            if(binsProp) {
                                var l  = binsProp.split(",");
                                for(var i=0;i<l.length-1;i++) {
                                    field.bins.push([+l[i],+l[i+1]]);
                                }
                            } else {
                                var numBins = +this.getProperty(field.getId() +".binCount",10); 
                                field.binSize = (max-min)/numBins;
                                for(var bin=0;bin<numBins;bin++) {
				    field.bins.push([min+field.binSize*bin,min+field.binSize*(bin+1)]);
				}
                            }
                        }
                    }
                }
            }

            function groupNode(id,field) {
                $.extend(this,{
                    id: id,
		    field:field,
		    members:[],
                    isGroup:true,
                    getCount: function() {
                        if(this.members.length==0) return 0;
                        if(this.members[0].isGroup) {
                            var cnt = 0;
                            this.members.map(node=>cnt+= node.getCount());
                            return cnt;
                        }
                        return this.members.length;
                    },
                    findGroup: function(v) {
                        for(var i=0;i<this.members.length;i++) {
                            if(this.members[i].isGroup && this.members[i].id == v) return this.members[i];
                        }
                        return null;
                    },
                });
            }
            var topGroup = new groupNode("");
            var colorMap ={};
            var colorCnt = 0;
	    var imgCnt = 0;
            for (var rowIdx = 0; rowIdx <records.length; rowIdx++) {
                var row = this.getDataValues(records[rowIdx]);
                var contents = "";
                var tooltip = "";
                this.tooltipFields.map(field=>{
                    if(tooltip!="") tooltip+="&#10;";
                    tooltip+=field.getValue(row);
                });
		tooltip =tooltip.replace(/\"/g,"&quot;");
                var label = "";
                var caption="";
                if(this.captionFields.length>0) {
                    if(this.captionTemplate) caption  = this.captionTemplate;
                    this.captionFields.map(field=>{
			var value = (""+field.getValue(row)).replace(/\"/g,"&quot;");
                        if(this.captionTemplate)
                            caption = caption.replace("\${" + field.getId()+"}",value);
                        else
                            caption+=value+"<br>";
                    });
                    if(this.urlField) {
                        var url = this.urlField.getValue(row);
                        if(url && url!="") {
                            caption = "<a style='color:inherit;'  href='" +url+"' target=_other>" +caption+"</a>";
                        }
                    }
                }
		this.labelFields.map(f=>{
		    label += row[f.getIndex()]+" ";
		});
		label = label.trim();
                var html ="";
                var img = null;
                if(this.imageField) {
                    img = row[this.imageField.getIndex()];
		    
                    if(this.onlyShowImages && !Utils.stringDefined(img)) continue;
                } 
                
                var  imgAttrs= ["class","display-cards-popup","data-fancybox",this.getDomId("gallery"),"data-caption",caption];
		if(img) img = img.trim();
                if(img!="") {
		    if(this.colorAnalysisEnabled)
			img = ramaddaBaseUrl+"/proxy?url=" + img;
                    img =  HtmlUtils.href(img, HtmlUtils.div(["id",this.getDomId("gallery")+"div" + imgCnt], HtmlUtils.image(img,["width",width,"id",this.getDomId("gallery")+"img" + imgCnt])),imgAttrs)+label;
		    imgCnt++;
                    html = HtmlUtils.div(["class","display-cards-item", "title", tooltip, "style","margin:" + margin+"px;"], img);
                } else {
                    var style = "";
                    if(fontSize) {
                        style+= " font-size:" + fontSize +"; ";
                    }
                    if(this.colorByField && this.colorList) {
                        var value = this.colorByField.getValue(row);
                        if(!Utils.isDefined(colorMap[value])) {
                            colorMap[value] = colorCnt++;
                        }
                        var index = colorMap[value];
                        if(index>=this.colorList.length) {
                            index = this.colorList.length%index;
                        }
                        style+="background:" + this.colorList[index]+";";
                        if(this.foregroundList) {
                            if(index<this.foregroundList.length) {
                                style+="color:" + this.foregroundList[index]+" !important;";
                            } else {
                                style+="color:" + this.foregroundList[this.foregroundList-1]+" !important;";
                            }
                        }
                    }
                    if(cardStyle)
                        style +=cardStyle;
                    var attrs = ["title",tooltip,"class","ramadda-gridbox display-cards-card","style",style];
                    if(this.altLabelField) {
                        html = HtmlUtils.div(attrs,this.altLabelField.getValue(row));
                    } else {
                        html = HtmlUtils.div(attrs,caption);
                    }
                    html =  HtmlUtils.href("", html,imgAttrs);
                }
                var group = topGroup;
                for(var groupIdx=0;groupIdx<groupFields.length;groupIdx++) {
                    var groupField  = groupFields[groupIdx];
                    var value = row[groupField.getIndex()];
                    if(groupField.isNumeric()) {
                        for(var binIdx=0;binIdx<groupField.bins.length;binIdx++) {
                            var bin= groupField.bins[binIdx];
                            if(value<=bin[1] || binIdx == groupField.bins.length-1) {
                                value = Utils.formatNumber(bin[0]) +" - " + Utils.formatNumber(bin[1]);
                                break;
                            }
                        }
                    }
                    var child = group.findGroup(value);
                    if(!child) {
                        group.members.push(child = new groupNode(value,groupField));
                    }
                    group = child;
                }
                group.members.push(html);
            }
            var html = HtmlUtils.tag("div",["class","display-cards-header"],"Total" +" (" + topGroup.getCount()+")");
            html+=this.makeGroupHtml(topGroup);
            this.writeHtml(ID_RESULTS, html);
            this.jq(ID_RESULTS).find("a.display-cards-popup").fancybox({
                caption : function( instance, item ) {
                    return  $(this).data('caption') || '';
                }});
        },
        makeGroupHtml: function(group) {
            if(group.members.length==0) return "";
            var html="";
            if(group.members[0].isGroup) {
                group.members.sort((a,b)=>{
                    if(a.id<b.id) return -1;
                    if(a.id>b.id) return 1;
                    return 0;
                });
                var width = group.members.length==0?"100%":100/group.members.length;
                html +="<table width=100% border=0><tr valign=top>";
                for(var i=0;i<group.members.length;i++) {
                    var child = group.members[i];
		    var prefix="";
		    if(child.field)
			prefix = child.field.getLabel()+": ";
                    html+="<td width=" + width+"%>";
		    html+=HtmlUtils.tag("div",["class","display-cards-header"],prefix+child.id +" (" + child.getCount()+")");
		    html+= this.makeGroupHtml(child);
                    html+="</td>";
                }
                html +="</tr></table>";
            } else {
                html+=Utils.join(group.members,"");
            }
            return html;
        }
    });
}




function RamaddaImagesDisplay(displayManager, id, properties) {
    var ID_GALLERY = "gallery";
    var ID_NEXT = "next";
    var ID_PREV = "prev";
    let SUPER =  new RamaddaFieldsDisplay(displayManager, id, DISPLAY_IMAGES, properties);
    
    RamaddaUtil.inherit(this,SUPER);
    addRamaddaDisplay(this);
    $.extend(this, {
	startIndex:0,
	getWikiEditorTags: function() {
	    return Utils.mergeLists(SUPER.getWikiEditorTags(),
				    [
					"label:Gallery Attributes",
					'labelFields=""',
					'tooltipFields=""',
					"numberOfImages=\"100\"",
					"imageWidth=\"150\"",
				    ])},
	dataFilterChanged: function() {
	    this.startIndex=0;
	    this.updateUI();
	},
        updateUI: function() {
            var pointData = this.getData();
            if (pointData == null) return;
            var records = this.filterData();
            if(!records) return;
            var fields = pointData.getRecordFields();
	    var imageField = this.getFieldOfType(fields,"image");
            var labelFields = this.getFieldsByIds(fields, this.getProperty("labelFields", null, true));
            var tooltipFields = this.getFieldsByIds(fields, this.getProperty("tooltipFields", null, true));
	    if(!imageField) {
		this.setContents(this.getMessage("No image field in data"));
		return;
	    }
	    var number = parseFloat(this.getProperty("numberOfImages",100));
	    var width = parseFloat(this.getProperty("imageWidth",150));
	    var contents = "";
	    var cnt = 1;
	    if(this.startIndex<0) this.startIndex=0;
	    if(this.startIndex>records.length) this.startIndex=records.length-number;
            for (var rowIdx = this.startIndex; rowIdx < records.length; rowIdx++) {
		if(cnt>number) break;
		cnt++;
                var row = this.getDataValues(records[rowIdx]);
		var image = row[imageField.getIndex()];
		if(image=="") {
		    continue;
		}
		var label = "";
		var tt = "";
		if(labelFields.length>0) {
		    labelFields.map(l=>{label += " " + row[l.getIndex()]});
		    label = HtmlUtils.div(["class","display-images-label"], label.trim());
		}
		if(tooltipFields.length>0) {
		    tooltipFields.map(l=>{tt += "\n" + l.getLabel()+": " + row[l.getIndex()]});
		}
		tt = tt.trim();
		contents += HtmlUtils.div(["class", "display-images-block","title",tt],
					  HtmlUtils.image(image,["width",width])+
					  label);

	    }
	    var header = "";
	    if(this.startIndex>0) {
		header += HtmlUtils.span(["id",this.getDomId(ID_PREV)],"Previous")+" ";
	    }
	    if(this.startIndex+number<records.length) {
		header += HtmlUtils.span(["id",this.getDomId(ID_NEXT)],"Next") +" ";
	    }
	    cnt--;
	    header += "Showing images " + (this.startIndex+1) +" - " +(this.startIndex+cnt);
	    if(number<records.length)
		header += " of " + records.length + " total images";

	    if(header!="") header  = header +"<br>";

            this.writeHtml(ID_DISPLAY_CONTENTS, header + contents);
	    this.jq(ID_PREV).button().click(()=>{
		this.startIndex-=number;
		this.updateUI();
	    });
	    this.jq(ID_NEXT).button().click(()=>{
		this.startIndex+=number;
		this.updateUI();
	    });
	}
    })
}



function RamaddaBlankDisplay(displayManager, id, properties) {
    properties.showMenu = false;
    properties.showTitle = false;
    let SUPER =  new RamaddaFieldsDisplay(displayManager, id, DISPLAY_BLANK, properties);
    RamaddaUtil.inherit(this,SUPER);
    addRamaddaDisplay(this);
    $.extend(this, {
        needsData: function() {
            return true;
        },
	updateUI: function() {
	    var records = this.filterData();
	    this.writeHtml(ID_DISPLAY_CONTENTS, "");
	    if(records) {
		var colorBy = this.getColorByInfo(records);
		if(colorBy.index>=0) {
		    records.map(record=>{
			color =  colorBy.getColor(record.getData()[colorBy.index], record);
		    });
		    colorBy.displayColorTable();
		}
	    }
	}});
}



function RamaddaTemplateDisplay(displayManager, id, properties) {
    if(!Utils.isDefined(properties.showTitle)) properties.showTitle=false;
    if(!Utils.isDefined(properties.showMenu)) properties.showMenu=false;
    if(!Utils.isDefined(properties.displayStyle)) properties.displayStyle = "background:rgba(0,0,0,0);";
    let SUPER =  new RamaddaFieldsDisplay(displayManager, id, DISPLAY_TEMPLATE, properties);
    RamaddaUtil.inherit(this,SUPER);
    addRamaddaDisplay(this);
    $.extend(this, {
	getWikiEditorTags: function() {
	    return Utils.mergeLists(SUPER.getWikiEditorTags(),
				    [
					"label:Template Attributes",
					"template=\"\"",
					"headerTemplate=\"... ${totalCount} ... ${selectedCount}\"",
					"footerTemplate=\"... ${totalCount} ... ${selectedCount}\"",
					"emptyMessage=\"\"",
					"select=\"max|min|<|>|=|<=|>=|contains\"",
					"selectField=\"\"",
					"selectValue=\"\"",
					'${&lt;field&gt;_total}',
					'${&lt;field&gt;_max}',
					'${&lt;field&gt;_min}',
					'${&lt;field&gt;_average}',
				    ]);
	},
        handleEventRecordSelection: function(source, args) {
//	    console.log(this.type+".recordSelection");
	    this.selectedRecord = args.record;
	    if(this.getProperty("onlyShowSelected")) {
		this.updateUI();
	    }
	},
	dataFilterChanged: function() {
	    if(this.getProperty("onlyShowSelected")&& this.selectedRecord ) {
		this.selectedRecord = null;
		this.writeHtml(ID_DISPLAY_CONTENTS, "");
	    }
	    SUPER.dataFilterChanged.call(this);
	},
	updateUI: function() {
//	    console.log(this.type+".updateUI");
	    var pointData = this.getData();
	    if (pointData == null) return;
	    var records = this.filterData();
	    if(!records) return;
	    if(this.getProperty("onlyShowSelected")) {
		if(!this.selectedRecord) {
		    this.writeHtml(ID_DISPLAY_CONTENTS, "");
		    return;
		}
		records = [this.selectedRecord];
	    }
	    records= this.sortRecords(records);
	    var fields = pointData.getRecordFields();
	    var uniqueFields  = this.getFieldsByIds(fields, this.getProperty("uniqueFields"));
	    var uniqueMap ={};
	    var template = this.getProperty("template","");
	    var select = this.getProperty("select","all");
	    var selected = [];
	    var summary = {};
	    var goodRecords = [];
	    records.map(record=>{
		r  =  this.getDataValues(record);
		if(uniqueFields.length>0) {
		    var key= "";
		    uniqueFields.map(uf=>{
			key += "__" +uf.getValue(r);
		    });
		    if(Utils.isDefined(uniqueMap[key])) {
			return;
		    }
		    uniqueMap[key] = true;
		}

		goodRecords.push(record);
		for(var i=0;i<fields.length;i++) {
		    var f = fields[i];
		    var v =f.getValue(r);
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
		    if(f.isDate&& v.getTime) {
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
		    s.average =  Utils.formatNumber(s.total/s.count);
		}
	    }
	    
	    if(select == "max" || select=="min" || select=="=" || select=="<" || select == ">" ||
	       select == "<=" || 	       select == "?>=" || select=="match") {
		var selectField = this.getProperty("selectField",null);
		if(selectField) selectField  =this.getFieldById(null, selectField);
		if(!selectField) {
		    this.writeHtml(ID_DISPLAY_CONTENTS, "No selectField specified");
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
		    var v =record.getValue(selectField.getIndex());
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
	    var contents = "";
	    if(selected.length==0) {
		contents = this.getProperty("emptyMessage","Nothing found");
	    }

            var colorBy = this.getColorByInfo(selected);

	    var headerTemplate = this.getProperty("headerTemplate","");
	    var footerTemplate = this.getProperty("footerTemplate","");
	    headerTemplate = headerTemplate.replace("${selectedCount}",selected.length);
	    headerTemplate = headerTemplate.replace("${totalCount}",records.length);
	    footerTemplate = footerTemplate.replace("${selectedCount}",selected.length);
	    footerTemplate = footerTemplate.replace("${totalCount}",records.length);

	    for(var i=0;i<fields.length;i++) {
		var f = fields[i];
		var s = summary[f.getId()];
		if(!s) continue;
		if(f.isDate) {
		    headerTemplate = headerTemplate.replace("${" + f.getId() +"_min_yyyymmdd}",Utils.formatDateYYYYMMDD(s.min)).replace("${" + f.getId() +"_max_yyyymmdd}",Utils.formatDateYYYYMMDD(s.max)).replace("${" + f.getId() +"_min_yyyy}",Utils.formatDateYYYY(s.min)).replace("${" + f.getId() +"_max_yyyy}",Utils.formatDateYYYY(s.max));
		    footerTemplate = footerTemplate.replace("${" + f.getId() +"_min_yyyymmdd}",Utils.formatDateYYYYMMDD(s.min)).replace("${" + f.getId() +"_max_yyyymmdd}",Utils.formatDateYYYYMMDD(s.max)).replace("${" + f.getId() +"_min_yyyy}",Utils.formatDateYYYY(s.min)).replace("${" + f.getId() +"_max_yyyy}",Utils.formatDateYYYY(s.max));
		    continue;
		    
		}
		if(s && f.isString()) {
		    headerTemplate = headerTemplate.replace("${" + f.getId() +"_uniques}",
							    s.uniqueCount);
		    footerTemplate = footerTemplate.replace("${" + f.getId() +"_uniques}",
							    s.uniqueCount);
		    continue;
		}
		if(!f.isNumeric()) continue;
		if(s) {
		    headerTemplate = headerTemplate.replace("${" + f.getId() +"_total}",s.total).replace("${" + f.getId() +"_min}",s.min).replace("${" + f.getId() +"_max}",s.max).replace("${" + f.getId() +"_average}",s.average);
		    headerTemplate = headerTemplate.replace("${" + f.getId() +"_total_round}",Math.round(s.total)).replace("${" + f.getId() +"_min_round}",Math.round(s.min)).replace("${" + f.getId() +"_max_round}",Math.round(s.max)).replace("${" + f.getId() +"_average_round}",Math.round(s.average));
		    headerTemplate = headerTemplate.replace("${" + f.getId() +"_total_format}",Utils.formatNumberComma(s.total)).replace("${" + f.getId() +"_min_format}",Utils.formatNumberComma(s.min)).replace("${" + f.getId() +"_max_format}",Utils.formatNumberComma(s.max)).replace("${" + f.getId() +"_average_format}",Utils.formatNumberComma(s.average));		    

		    footerTemplate = footerTemplate.replace("${" + f.getId() +"_total}",s.total).replace("${" + f.getId() +"_min}",s.min).replace("${" + f.getId() +"_max}",s.max).replace("${" + f.getId() +"_average}",s.average);
		    footerTemplate = footerTemplate.replace("${" + f.getId() +"_total_round}",Math.round(s.total)).replace("${" + f.getId() +"_min_round}",Math.round(s.min)).replace("${" + f.getId() +"_max_round}",Math.round(s.max)).replace("${" + f.getId() +"_average_round}",Math.round(s.average));
		    footerTemplate = footerTemplate.replace("${" + f.getId() +"_total_format}",Utils.formatNumberComma(s.total)).replace("${" + f.getId() +"_min_format}",Utils.formatNumberComma(s.min)).replace("${" + f.getId() +"_max_format}",Utils.formatNumberComma(s.max)).replace("${" + f.getId() +"_average_format}",Utils.formatNumberComma(s.average));		    
		}
	    }
	    if(selected.length==1) {
		var row = this.getDataValues(selected[0]);
		headerTemplate = this.applyRecordTemplate(row,fields,headerTemplate);
		footerTemplate = this.applyRecordTemplate(row,fields,footerTemplate);
	    }


	    if(this.filterFields) {
		for(var filterIdx=0;filterIdx<this.filterFields.length;filterIdx++) {
		    var f = this.filterFields[filterIdx];
		    if(f.isNumeric()) {
			var min = $("#" + this.getDomId("filterby_" + f.getId()+"_min")).val().trim();
			var max = $("#" + this.getDomId("filterby_" + f.getId()+"_max")).val().trim();
			headerTemplate = headerTemplate.replace("${filter_" + f.getId() +"_min}",min);
			headerTemplate = headerTemplate.replace("${filter_" + f.getId() +"_max}",max);
			footerTemplate = footerTemplate.replace("${filter_" + f.getId() +"_min}",min);
			footerTemplate = footerTemplate.replace("${filter_" + f.getId() +"_max}",max);
		    } else {
			var widget =$("#" + this.getDomId("filterby_" + f.getId())); 
			if(!widget.val || widget.val()==null) continue;
			var value = widget.val();
			if(!value) continue;
			if(Array.isArray(value)) {
			    var tmp = "";
			    value.map(v=>{
				if(tmp!="") tmp+=", ";
				tmp+=v;
			    });
			    value = tmp;
			}
			value = value.trim();
			if(value==FILTER_ALL) {
			    var regexp = new RegExp("\\${filter_" + f.getId()+"[^}]*\\}",'g');
			    headerTemplate = headerTemplate.replace(regexp,"");
			    footerTemplate = footerTemplate.replace(regexp,"");
			} else {
			    var regexp = new RegExp("\\${filter_" + f.getId()+" +prefix='([^']*)' +suffix='([^']*)' *\\}",'g');
			    headerTemplate = headerTemplate.replace(regexp,"$1" + value +"$2");
			    footerTemplate = footerTemplate.replace(regexp,"$1" + value +"$2");
			    var regexp = new RegExp("\\${filter_" + f.getId()+" +prefix='([^']*)' *\\}",'g');
			    headerTemplate = headerTemplate.replace(regexp,"$1" + value);
			    footerTemplate = footerTemplate.replace(regexp,"$1" + value);
			    var regexp = new RegExp("\\${filter_" + f.getId()+" +suffix='([^']*)' *\\}",'g');
			    headerTemplate = headerTemplate.replace(regexp,value +"$1");
			    footerTemplate = footerTemplate.replace(regexp,value +"$1");
			    var regexp = new RegExp("\\${filter_" + f.getId()+" *\\}",'g');
			    headerTemplate = headerTemplate.replace(regexp,value);
			    footerTemplate = footerTemplate.replace(regexp,value);
			}
		    }
		}
	    }

	    if(selected.length>0) {
		contents+= headerTemplate;
	    }
	    if(template!= "") {
		var props = this.getTemplateProps(fields);
		var max = parseFloat(this.getProperty("maxNumber",-1));
		var cols = parseFloat(this.getProperty("templateColumns",-1));
		var colTag;
		if(cols>0) {
		    colTag = "col-md-" +Math.round(12/cols);
		    contents += '<div class="row-tight row">';
		}
		var colCnt = 0;
		var style = this.getProperty("templateStyle","");

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
			var value = record.getData()[colorBy.index];
			color =  colorBy.getColor(value, record);
                    }
		    var row = this.getDataValues(record);
		    var s = template.trim();
		    s = s.replace("${selectCount}",selected.length);
		    s = s.replace("${totalCount}",records.length);
		    s= this.applyRecordTemplate(row,fields,s,props);
		    s = s.replace(/\${recordIndex}/g,(rowIdx+1));
		    var recordStyle = style;
		    if(color) {
			if(this.getProperty("colorBackground",false))
			    recordStyle = "background: " + color+";" + recordStyle;
			s = s.replace("${color}",color);
		    }
		    var tag = HtmlUtils.openTag("div",["style",recordStyle, "id", this.getId() +"-" + record.getId(), "title","","class","display-template-record","recordIndex",rowIdx]);
//		    console.log(tag);
		    if(s.startsWith("<td")) {
			s = s.replace(/<td([^>]*)>/,"<td $1>"+tag);
			s = s.replace(/<\/td>$/,"</div></td>");
			contents += s;
		    } else if(s.startsWith("<tr")) {
			s = s.replace(/<td([^>]*)>/g,"<td $1>"+tag);
			s = s.replace(/<\/td>/g,"</div></td>");
			contents += s;
		    }  else {
			contents += tag +s +"</div>"
		    }
		    if(cols>0) {
			contents+='</div>\n';
		    }
		}
		if(cols>0) {
		    contents += '</div>\n';
		}
	    }
//	    console.log("CONTENTS:\n" +contents);
	    if(selected.length>0) 
		contents+= footerTemplate;
	    this.writeHtml(ID_DISPLAY_CONTENTS, contents);
	    var recordElements = this.jq(ID_DISPLAY_CONTENTS).find(".display-template-record");
	    this.makeTooltips(recordElements, selected);
	    this.makePopups(recordElements, selected);
	    let _this = this;
	    this.jq(ID_DISPLAY_CONTENTS).find(".display-template-record").click(function() {
		var record = selected[$(this).attr("recordIndex")];
		_this.handleEventRecordHighlight(this, {record:record,highlight:true,immediate:true,skipScroll:true});
		_this.getDisplayManager().notifyEvent("handleEventRecordSelection", _this, {highlight:true,record: record});
	    });
	},
	highlightCount:0,
        handleEventRecordHighlight: function(source, args) {
//	    console.log(this.type+ ".recordHighlight");
	    let myCount = ++this.highlightCount;
	    var id = "#" + this.getId()+"-"+args.record.getId();
	    if(this.highlightedElement) {
		this.unhighlightElement(this.highlightedElement);
		this.highlightedElement = null;
	    }
	    if(args.highlight) {
		if(args.immediate) {
		    this.highlightElement(args);
		} else {
		    setTimeout(() =>{
			if(myCount == this.highlightCount) {
			    this.highlightElement(args);
			}
		    },500);
		}
	    } else {
		var id = "#" + this.getId()+"-"+args.record.getId();
		var element = $(id);
		this.unhighlightElement(element);
	    }
	},
	unhighlightElement: function(element) {
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
			//			    console.log("un highlight css:" + element.css("background")+ " idx:" + element.attr("recordIndex"));
			element.css(a,v);
		    }
		});
	    } 
	},
	highlightElement: function(args) {
//	    console.log(this.type+".highlightElement");
	    var id = "#" + this.getId()+"-"+args.record.getId();
	    var element = $(id);
	    this.highlightedElement = element;
	    element.addClass("display-template-record-highlight");
	    var css = this.getProperty("highlightOnCss","").split(";");
	    if(css.length>0) {
		css.map(tok=>{
		    var c = tok.split(":");
		    var a = c[0];
		    var v = c.length>1?c[1]:null;
		    if(!v || v=="") {
			v = element.attr("prev-" + a);
		    }
		    if(v) {
			var oldV = element.css(a);
			if(oldV) {
			    element.attr("prev-" + a,oldV);
			}
			element.css(a,v);
		    }
		});
	    } 
	    

	    try {
		if(!args.skipScroll) {
		    var eo = element.offset();
		    if(eo==null) return;
		    var container = this.jq(ID_DISPLAY_CONTENTS);
		    if(this.getProperty("orientation","vertical")== "vertical") {
			var c = container.offset().top;
			var s = container.scrollTop();
			container.scrollTop(eo.top- c + s)
		    } else {
			var c = container.offset().left;
			var s = container.scrollLeft();
			container.scrollLeft(eo.left- c + s)
		    }
		}

	    } catch(err) {
		console.log("Error:" + err);
	    }

	}
    })}




function RamaddaPercentchangeDisplay(displayManager, id, properties) {
    let SUPER =  new RamaddaFieldsDisplay(displayManager, id, DISPLAY_PERCENTCHANGE, properties);
    RamaddaUtil.inherit(this,SUPER);
    addRamaddaDisplay(this);
    $.extend(this, {
	getWikiEditorTags: function() {
	    return Utils.mergeLists(SUPER.getWikiEditorTags(),
				    [
					"label:Percent change Attributes",
					'template="${date1} ${date2} ${value1} ${value2} ${percent} ${per_hour} ${per_day} ${per_week} ${per_month} ${per_year}"'
				    ]);
	},
	updateUI: function() {
	    var records = this.filterData();
	    if(!records) return;
            var allFields = this.getData().getRecordFields();
            var fields = this.getSelectedFields(allFields);
	    fields = this.getFieldsOfType(fields, "numeric");
	    let  record1 = records[0];
	    let  record2 = records[records.length-1];
	    var template = this.getProperty("template",null);
	    var headerTemplate = this.getProperty("headerTemplate","");
	    var footerTemplate = this.getProperty("footerTemplate","");
	    let date1 = record1.getDate();
	    let date2 = record2.getDate();
	    let label1 ="Start Value";
	    let label2 ="End Value";
	    let hours = 1;
	    let days = 1;
	    let years = 1;
	    let months = 1;
	    if(date1)
		label1 = this.formatDate(date1);
	    if(date2)
		label2 = this.formatDate(date2);
	    if(date1 && date2) {
		var diff = date2.getTime() - date1.getTime();
		days = diff/1000/60/60/24;
		hours = days*24;
		years = days/365;
		months = years*12;
	    }
            let html =  "";
	    if(template) {
		html= headerTemplate;
	    } else {
		html = "<br>";		
		html += HtmlUtils.openTag("table", ["class", "nowrap ramadda-table", "id", this.getDomId("percentchange")]);
		html += HtmlUtils.openTag("thead", []);
		html += "\n";
		html += HtmlUtils.tr([], HtmlUtils.th(["style","text-align:center"], "Field") + HtmlUtils.th(["style","text-align:center"], label1) + HtmlUtils.th(["style","text-align:center"], label2)
				     + HtmlUtils.th(["style","text-align:center"], "Percent Change"));
		html += HtmlUtils.closeTag("thead");
		html += HtmlUtils.openTag("tbody", []);
	    }
	    var tuples= [];
	    fields.map(f=>{
		var val1 = record1.getValue(f.getIndex());
		var val2 = record2.getValue(f.getIndex());
		var percent = parseInt(1000*(val2-val1)/val1)/10;
		tuples.push({field:f,val1:val1,val2:val2,percent:percent});
	    });

	    tuples.sort((a,b)=>{
		return -(a.percent-b.percent);
	    })
	    tuples.map(t=>{
		if(template) {
		    var h = template.replace("${field}", t.field.getLabel()).replace("${value1}",t.val1).replace("${value2}",t.val2).replace("${percent}",this.formatNumber(t.percent)).replace("${date1}",label1).replace("${date2}",label2).replace("${difference}", t.val2-t.val1);
		    
		    h = h.replace(/\${per_hour}/g,this.formatNumber(t.percent/hours));
		    h = h.replace(/\${per_day}/g,this.formatNumber(t.percent/days));
		    h = h.replace(/\${per_week}/g,this.formatNumber(t.percent/(days/7)));
		    h = h.replace(/\${per_month}/g,this.formatNumber(months));
		    h = h.replace(/\${per_year}/g,this.formatNumber(years));
		    html+=h;
		} else {
		    html += HtmlUtils.tr([], HtmlUtils.td([], t.field.getLabel()) + 
					 HtmlUtils.td(["align","right"], t.val1) +
					 HtmlUtils.td(["align","right"], t.val2)
					 + HtmlUtils.td(["align","right"], t.percent+"%"));
		}
	    });

	    if(template) {
		html+= footerTemplate;
	    } else {
		html += HtmlUtils.closeTag("tbody");
		html += HtmlUtils.closeTag("table");
	    }
	    this.writeHtml(ID_DISPLAY_CONTENTS, html); 
           HtmlUtils.formatTable("#" + this.getDomId("percentchange"), {
                //scrollY: this.getProperty("tableSummaryHeight", tableHeight)
            });
	},
    })
}






function RamaddaSlidesDisplay(displayManager, id, properties) {
    var ID_SLIDE = "slide";
    var ID_PREV = "prev";
    var ID_NEXT = "next";
    if(!Utils.isDefined(properties.displayStyle)) properties.displayStyle = "background:rgba(0,0,0,0);";
    let SUPER =  new RamaddaFieldsDisplay(displayManager, id, DISPLAY_SLIDES, properties);
    RamaddaUtil.inherit(this,SUPER);
    addRamaddaDisplay(this);
    $.extend(this, {
	slideIndex:0,
	getWikiEditorTags: function() {
	    return Utils.mergeLists(SUPER.getWikiEditorTags(),
				    [
					"label:Slides Attributes",
					"template=\"\"",
				    ]);
	},
        handleEventRecordSelection: function(source, args) {
	    //	    console.log(this.type+ ".recordSelection");
	    if(!this.records) return;
	    var index =-1;
	    for(var i=0;i<this.records.length;i++) {
		if(this.records[i].getId() == args.record.getId()) {
		    index = i;
		    break;
		}
	    }
	    if(index>=0) {
		this.slideIndex=index;
		this.displaySlide();
	    }
	    
	},
        getContentsStyle: function() {
            var style = "";
            var height = this.getHeightForStyle();
            if (height) {
		style += " height:" + height + ";";
            }
            var width = this.getWidthForStyle();
            if (width) {
                style += " width:" + width + ";";
            }
            return style;
        },

	updateUI: function() {
	    var pointData = this.getData();
	    if (pointData == null) return;
	    this.records = this.filterData();
	    if(!this.records) return;
            this.fields = this.getData().getRecordFields();
	    this.records= this.sortRecords(this.records);
	    var template = this.getProperty("template","");
	    var slideWidth = this.getProperty("slideWidth","100%");
            var height = this.getHeightForStyle("400");
	    var left = HtmlUtils.div(["id", this.getDomId(ID_PREV), "style","font-size:200%;","class","display-slides-arrow-left fas fa-angle-left"]);
	    var right = HtmlUtils.div(["id", this.getDomId(ID_NEXT), "style","font-size:200%;", "class","display-slides-arrow-right fas fa-angle-right"]);
	    var slide = HtmlUtils.div(["style","overflow-y:auto;max-height:" + height+";", "id", this.getDomId(ID_SLIDE), "class","display-slides-slide"]);

	    var navStyle = "padding-top:20px;";
	    var contents = HtmlUtils.div(["style","position:relative;"], "<table width=100%><tr valign=top><td width=20>" + HtmlUtils.div(["style",navStyle], left) + "</td><td>" +
					 slide + "</td>" +
					 "<td width=20>" + HtmlUtils.div(["style",navStyle],right) + "</td></tr></table>");
	    this.writeHtml(ID_DISPLAY_CONTENTS, contents);
	    this.jq(ID_PREV).click(() =>{
		this.slideIndex--;
		this.displaySlide(true);
	    });
	    this.jq(ID_NEXT).click(() =>{
		this.slideIndex++;
		this.displaySlide(true);
	    });
	    setTimeout(()=>{
		this.displaySlide();},200);

	},
	displaySlide: function(propagateEvent) {
	    if(this.slideIndex<0) this.slideIndex=0;
	    if(this.slideIndex>=this.records.length) this.slideIndex=this.records.length-1;
	    if(this.slideIndex==0)
		this.jq(ID_PREV).hide();
	    else
		this.jq(ID_PREV).show();
	    if(this.slideIndex==this.records.length-1)
		this.jq(ID_NEXT).hide();
	    else
		this.jq(ID_NEXT).show();
	    var record = this.records[this.slideIndex];
	    var row = this.getDataValues(record);
	    var html = this.applyRecordTemplate(row,this.fields,this.getProperty("template",""));
	    html = html.replace(/\${recordIndex}/g,(this.slideIndex+1));
	    this.jq(ID_SLIDE).html(html);
	    var args = {highlight:true,record: record};
	    if(propagateEvent)
		this.getDisplayManager().notifyEvent("handleEventRecordHighlight", this, args);
	},
        handleEventRecordHighlight: function(source, args) {
	}
    })}




function RamaddaTopfieldsDisplay(displayManager, id, properties) {
    var ID_SLIDE = "slide";
    let SUPER =  new RamaddaFieldsDisplay(displayManager, id, DISPLAY_TOPFIELDS, properties);
    RamaddaUtil.inherit(this,SUPER);
    addRamaddaDisplay(this);
    $.extend(this, {
        needsData: function() {
            return true;
        },
	getWikiEditorTags: function() {
	    return Utils.mergeLists(SUPER.getWikiEditorTags(),
				    [
					"label:Top Fields Attributes",
					'fieldCount=""',
					'labelField=""',
					'dateFormat"yyyy"',
					'labelField=""',
					'scaleFont="false"',
				    ]);
	},
	updateUI: function() {
	    var pointData = this.getData();
	    if (pointData == null) return;
	    let records = this.filterData();
	    if(!records) return;

            var fields = this.getData().getNonGeoFields();
	    var labelField = this.getFieldById(fields,this.getProperty("labelField"));
	    if(labelField==null) {
		labelField = this.getFieldById(fields, "title");
	    }
	    if(labelField==null) {
		labelField = this.getFieldById(fields, "name");
	    }
	    var fieldsToUse = this.getFieldsByIds(fields,this.getProperty("fields"));
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
		    contents += HtmlUtils.div(["field-id",field.getId(), "data-value",field.getLabel(), "title","Value: " + value, "class","display-topfields-row","style","font-size:" + fontSize+";"], field.getLabel());
		}
		div += HtmlUtils.div(["class","display-topfields-header","recordIndex",i],header);
		div += HtmlUtils.div(["class","display-topfields-values"], contents);
		html+=HtmlUtils.div(["class","display-topfields-record"], div);
	    }
	    this.writeHtml(ID_DISPLAY_CONTENTS, html);
	    let _this = this;
	    this.jq(ID_DISPLAY_CONTENTS).find(".display-topfields-header").click(function(){
		var idx = $(this).attr("recordIndex");
		_this.jq(ID_DISPLAY_CONTENTS).find(".display-topfields-record").removeClass("display-topfields-selected");
		$(this).parent().addClass("display-topfields-selected");
		var record = records[idx];
		if(record) {
		    _this.getDisplayManager().notifyEvent("handleEventRecordSelection", _this, {record: record});
		}
	    });
	    let rows =this.jq(ID_DISPLAY_CONTENTS).find(".display-topfields-row");
	    rows.hover(function() {
		rows.removeClass("display-topfields-highlight");
		var value = $(this).attr("data-value");
		_this.jq(ID_DISPLAY_CONTENTS).find("[data-value='" + value +"']").addClass("display-topfields-highlight");
		
	    });
	    rows.click(function() {
		var field = $(this).attr("field-id");
		_this.getDisplayManager().notifyEvent("handleEventFieldsSelected", _this, [field]);
		
	    });
	},
        handleEventRecordSelection: function(source, args) {
	    if(!args.record) return;
	    var index = this.recordToIndex[args.record.getId()];
	    if(!Utils.isDefined(index)) return;
	    var container = this.jq(ID_DISPLAY_CONTENTS);
	    container.find(".display-topfields-record").removeClass("display-topfields-selected");
	    var element =   container.find("[recordIndex='" + index +"']").parent();
	    element.addClass("display-topfields-selected");
	    var c = container.offset().top;
	    var s = container.scrollTop();
	    var eo = element.offset();
	    var diff = eo.top- c + s;
	    container.scrollTop(diff)
	},



    })}




function RamaddaTimelineDisplay(displayManager, id, properties) {
    var ID_TIMELINE = "timeline";
    let SUPER =  new RamaddaFieldsDisplay(displayManager, id, DISPLAY_TIMELINE, properties);
    RamaddaUtil.inherit(this,SUPER);
    addRamaddaDisplay(this);
    
    Utils.importJS(ramaddaBaseUrl+"/lib/timeline3/timeline.js");
    var css = "https://cdn.knightlab.com/libs/timeline3/latest/css/timeline.css";
//    css =  ramaddaBaseUrl+"/lib/timeline3/timeline.css";
    $('<link rel="stylesheet" href="' + css +'" type="text/css" />').appendTo("head");

    $.extend(this, {
        needsData: function() {
            return true;
        },
	getWikiEditorTags: function() {
	    return Utils.mergeLists(SUPER.getWikiEditorTags(),
				    [
					"label:Timeline",
					'justTimeline="true"',
					'titleField=""',
					'startDateField=""',
					'endDateField=""',
					'textTemplate=""',
					'timeTo="year|day|hour|second"',
					'hideBanner="true"',
					'startAtEnd=true',
					'navHeight=250'
				    ]);
	},
	loadCnt:0,
	timelineLoaded: false,
	updateUI: function() {
	    if(!this.timelineLoaded) {
		try {
		    var tmp =  TL.Timeline;
		    this.timelineLoaded = true;
		} catch(err) {
		    if(this.loadCnt++<100) {
			setTimeout(()=>this.updateUI(),100);
			return;
		    }
		}
	    }
	    if(!this.timelineLoaded) {
		this.writeHtml(ID_DISPLAY_CONTENTS, "Could not load timeline");
		return;
	    }
            var records = this.filterData();
	    if(records==null) return;
	    var timelineId = this.getDomId(ID_TIMELINE);
	    this.writeHtml(ID_DISPLAY_CONTENTS, HtmlUtils.div(["id",timelineId]));
	    this.timelineReady = false;
	    var opts = {
		start_at_end: this.getProperty("startAtEnd",false),
//		default_bg_color: {r:0, g:0, b:0},
		timenav_height: this.getProperty("navHeight",150),
//		menubar_height:100,
		gotoCallback: (slide)=>{
		    if(this.timelineReady) {
			var record = records[slide];
			if(record) {
			    this.getDisplayManager().notifyEvent("handleEventRecordSelection", this, {record: record});
			}
		    }
		}
            };
	    var json = {};
	    var events = [];
	    json.events = events;
	    var titleField = this.getFieldById(null,this.getProperty("titleField"));
	    if(titleField==null) {
		titleField = this.getFieldById(null, "title");
	    }
	    if(titleField==null) {
		titleField = this.getFieldById(null, "name");
	    }

	    var startDateField = this.getFieldById(null,this.getProperty("startDateField"));
	    var endDateField = this.getFieldById(null,this.getProperty("endDateField"));
	    var textTemplate = this.getProperty("textTemplate","${default}");
	    var timeTo = this.getProperty("timeTo","day");
	    this.recordToIndex = {};
	    for(var i=0;i<records.length;i++) {
		var record = records[i]; 
		this.recordToIndex[record.getId()] = i;
		var tuple = record.getData();
		var event = {
		};	
		var text =  this.getRecordHtml(record, null, textTemplate);
		event.text = {
		    headline: titleField? tuple[titleField.getIndex()]:" record:" + (i+1),
		    text:text
		};
		if(startDateField)
		    event.start_date = tuple[startDateField.getIndex()];
		else
		    event.start_date = this.getDate(record.getTime());
		if(endDateField) {
		    event.end_date = tuple[endDateField.getIndex()];
		}
		events.push(event);
	    }
	    this.timeline = new TL.Timeline(timelineId,json,opts);
	    if(this.getProperty("hideBanner",false)) {
		this.jq(ID_TIMELINE).find(".tl-storyslider").css("display","none");
	    }
	    this.jq(ID_TIMELINE).find(".tl-text").css("padding","0px");
	    this.jq(ID_TIMELINE).find(".tl-slide-content").css("padding","0px 0px");
//	    this.jq(ID_TIMELINE).find(".tl-slide-content").css("width","100%");
	    this.jq(ID_TIMELINE).find(".tl-slidenav-description").css("display","none");
	    this.timelineReady = true;

	},
        handleEventRecordSelection: function(source, args) {
	    if(!args.record) return;
	    var index = this.recordToIndex[args.record.getId()];
	    if(!Utils.isDefined(index)) return;
	    this.timeline.goTo(index);
	},
	getDate: function(time) {
	    var timeTo = this.getProperty("timeTo","day");
	    var dt =  {year: time.getUTCFullYear()};
	    if(timeTo!="year") {
		dt.month = time.getUTCMonth()+1;
		if(timeTo!="month") {
		    dt.day = time.getUTCDate();
		    if(timeTo!="day") {
			dt.hour = time.getHours();
			dt.minute = time.getMinutes();
			if(timeTo!="hour") {
			    dt.second = time.getSeconds();
			}
		    }
		}
	    }
	    return dt;
	}
    });
}







function RamaddaBlocksDisplay(displayManager, id, properties) {
    let SUPER =  new RamaddaFieldsDisplay(displayManager, id, DISPLAY_BLOCKS, properties);
    var ID_BLOCKS_HEADER = "blocks_header";
    var ID_BLOCKS = "blocks";
    var ID_BLOCKS_FOOTER = "blocks_footer";
    var animStep=1000;
    RamaddaUtil.inherit(this,SUPER);
    addRamaddaDisplay(this);
    $.extend(this, {
        getContentsStyle: function() {
            return "";
        },
        updateUI: function() {
	    this.counts = [];
	    this.counts2 = [];
	    var counts = this.getProperty("counts","100",true).split(";");
	    for(var i=0;i<counts.length;i++) 
		this.counts.push(parseFloat(counts[i]));
	    var doSum = this.getProperty("doSum","true") == "true";
	    if(doSum) {
		this.counts2 = this.counts;
	    } else {
		var total = 0;
		for(var i=0;i<this.counts.length;i++) {
		    var tmp = this.counts[i];
		    this.counts2.push(this.counts[i]-total);
		    total+= tmp;
		}
	    }

	    this.footers = this.getProperty("footers","",true).split(";");
	    this.headers = this.getProperty("headers","",true).split(";");
	    while(this.footers.length< this.counts.length)
		this.footers.push("");
	    while(this.headers.length< this.counts.length)
		this.headers.push("");
	    this.showBlocks(true);
	    this.writeHtml(ID_DISPLAY_CONTENTS, 
			   HtmlUtils.div(["class","display-blocks-header","style", this.getProperty("headerStyle","", true),"id",this.getDomId(ID_BLOCKS_HEADER)]) +
			   HtmlUtils.div(["class","display-blocks-blocks","id",this.getDomId(ID_BLOCKS)])+
			   HtmlUtils.div(["class","display-blocks-footer", "style", this.getProperty("footerStyle","", true), "id",this.getDomId(ID_BLOCKS_FOOTER)]));
	    //Show the outline
	    this.showBlocks(true);
	    HtmlUtils.callWhenScrolled(this.getDomId(ID_DISPLAY_CONTENTS),()=>{
		if(!this.displayedBlocks) {
		    this.displayedBlocks = true;
		    setTimeout(()=>{this.showBlocks(false)},animStep);
		}
	    },500);

	},
	showBlocks: function(initial, step) {
	    if(!Utils.isDefined(step)) {
		if(initial)step = this.counts.length;
		else step = 0;
	    }
	    var contents = "";
	    contents += HtmlUtils.openDiv(["class","display-blocks"]);
	    var tmp =this.getProperty("colors","red,blue,gray,green",true); 
	    var ct = (typeof tmp) =="string"?tmp.split(","):tmp;
	    var footer ="";
	    while(ct.length<this.counts.length) {
		ct.push(ct[ct.length-1]);
	    }
	    var multiplier = parseFloat(this.getProperty("multiplier","1",true));
	    var dim=this.getProperty("blockDimensions","8",true);
	    var labelStyle = this.getProperty("labelStyle","", true);
	    var blockCnt = 0;
	    for(var i=0;i<this.counts2.length;i++) {
		var label = this.footers[i].replace("${count}",multiplier*this.counts[i]) ;
		var style =  "width:" + dim+"px;height:" + dim+"px;";
		if(!initial) {
		    if(i<step) {
			style += "background:" + ct[i]+";" ;
			footer += HtmlUtils.div(["class","display-block","style",style],"") +" " + HtmlUtils.span(["style",labelStyle], label)+"&nbsp;&nbsp;";
		    } else {
			footer += "&nbsp;&nbsp;";
		    }
		}
		var cnt = this.counts2[i];
		for(var j=0;j<this.counts2[i];j++) {
		    contents += HtmlUtils.div(["class","display-block","style",style,"title",label],"");
		}
		blockCnt++;
	    }
	    contents += HtmlUtils.closeDiv();
	    this.jq(ID_BLOCKS_HEADER).html(this.getProperty("header",""));
	    this.jq(ID_BLOCKS).html(contents);
	    this.jq(ID_BLOCKS_FOOTER).html(footer);
	    if(step < this.counts.length) {
		setTimeout(()=>{this.showBlocks(false, step+1)},animStep);
	    }
	}
    }
	    )
}





function RamaddaTextstatsDisplay(displayManager, id, properties) {
    let SUPER = new RamaddaBaseTextDisplay(displayManager, id, DISPLAY_TEXTSTATS, properties);
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);
    $.extend(this, {
        updateUI: function() {
            var cnt = {};
            var fieldInfo = this.processText(cnt);
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
                    if (a.count < b.count) return -1;
                    if (a.count > b.count) return 1;
                    return 0;
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

                var tmp = [];
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
                var min = 0;
                var max = 0;
                for (var i = 0; i < tmp.length; i++) {
                    max = (i == 0 ? tmp[i].count : Math.max(max, tmp[i].count));
                    min = (i == 0 ? tmp[i].count : Math.min(min, tmp[i].count));
                }
                if (this.getProperty("showFieldLabel", true))
                    html += "<b>" + fi.field.getLabel() + "</b><br>";
                var td1Width = "20%";
                var td2Width = "10%";
                if (this.getProperty("showSummary", true)) {
                    html += HtmlUtils.openTag("table", ["class", "nowrap ramadda-table", "id", this.getDomId("table_summary")]);
                    html += HtmlUtils.openTag("thead", []);
                    html += HtmlUtils.tr([], HtmlUtils.th(["width", td1Width], "Summary") + HtmlUtils.th([], "&nbsp;"));
                    html += HtmlUtils.closeTag("thead");
                    html += HtmlUtils.openTag("tbody", []);
                    html += HtmlUtils.tr([], HtmlUtils.td(["align", "right"], "Total lines:") + HtmlUtils.td([], records.length));
                    html += HtmlUtils.tr([], HtmlUtils.td(["align", "right"], "Total words:") + HtmlUtils.td([], cnt.count));
                    html += HtmlUtils.tr([], HtmlUtils.td(["align", "right"], "Average word length:") + HtmlUtils.td([], Math.round(cnt.total / cnt.count)));
                    html += HtmlUtils.closeTag("tbody");

                    html += HtmlUtils.closeTag("table");
                    html += "<br>"
                }
                if (this.getProperty("showCounts", true)) {
                    html += HtmlUtils.openTag("table", ["class", "row-border nowrap ramadda-table", "id", this.getDomId("table_counts")]);
                    html += HtmlUtils.openTag("thead", []);
                    html += HtmlUtils.tr([], HtmlUtils.th(["width", td1Width], "Word Length") + HtmlUtils.th(["width", td2Width], "Count") + (showBars ? HtmlUtils.th([], "") : ""));
                    html += HtmlUtils.closeTag("thead");
                    html += HtmlUtils.openTag("tbody", []);
                    for (var i = 0; i < tmp.length; i++) {
                        var row = HtmlUtils.td([], tmp[i].length) + HtmlUtils.td([], tmp[i].count);
                        if (showBars) {
                            var wpercent = (tmp[i].count - min) / max;
                            var width = 2 + wpercent * barWidth;
                            var color = barColor;
                            var div = HtmlUtils.div(["style", "height:10px;width:" + width + "px;background:" + color], "");
                            row += HtmlUtils.td([], div);
                        }
                        html += HtmlUtils.tr([], row);
                    }
                    html += HtmlUtils.closeTag("tbody");
                    html += HtmlUtils.closeTag("table");
                    html += "<br>"
                }
                if (this.getProperty("showFrequency", true)) {
                    html += HtmlUtils.openTag("table", ["class", "row-border ramadda-table", "id", this.getDomId("table_frequency")]);
                    html += HtmlUtils.openTag("thead", []);
                    html += HtmlUtils.tr([], HtmlUtils.th(["width", td1Width], "Word") + HtmlUtils.th(["width", td2Width], "Frequency") + (showBars ? HtmlUtils.th([], "") : ""));
                    html += HtmlUtils.closeTag("thead");
                    html += HtmlUtils.openTag("tbody", []);
                    var min = 0;
                    var max = 0;
                    if (counts.length > 0) {
                        min = counts[0].count;
                        max = counts[counts.length - 1].count;
                    }
                    var totalWords = 0;
                    for (var i = 0; i < counts.length; i++) {
                        totalWords += counts[i].count;
                    }
                    for (var i = counts.length - 1; i >= 0; i--) {
                        var percent = Math.round(10000 * (counts[i].count / totalWords)) / 100;
                        var row = HtmlUtils.td([], counts[i].word + "&nbsp;:&nbsp;") +
                            HtmlUtils.td([], counts[i].count + "&nbsp;&nbsp;(" + percent + "%)&nbsp;:&nbsp;");
                        if (showBars) {
                            var wpercent = (counts[i].count - min) / max;
                            var width = 2 + wpercent * barWidth;
                            var color = barColor;
                            var div = HtmlUtils.div(["style", "height:10px;width:" + width + "px;background:" + color], "");
                            row += HtmlUtils.td([], div);
                        }
                        html += HtmlUtils.tr([], row);
                    }
                    html += HtmlUtils.closeTag("tbody");
                    html += HtmlUtils.closeTag("table");
                }
            }
            this.writeHtml(ID_DISPLAY_CONTENTS, html);
            var tableHeight = this.getProperty("tableHeight", "200");

            if (this.getProperty("showSummary", true))
                HtmlUtils.formatTable("#" + this.getDomId("table_summary"), {
                    scrollY: this.getProperty("tableSummaryHeight", tableHeight)
                });
            if (this.getProperty("showCounts", true))
                HtmlUtils.formatTable("#" + this.getDomId("table_counts"), {
                    scrollY: this.getProperty("tableCountsHeight", tableHeight)
                });
            if (this.getProperty("showFrequency", true)) {
                this.frequencyTable = HtmlUtils.formatTable("#" + this.getDomId("table_frequency"), {
		    scrollY: this.getProperty("tableFrequenecyHeight", tableHeight),
		    searching: this.getProperty("showSearch", true)
		});
		this.frequencyTable.on( 'search.dt', ()=>{
		    if(this.settingSearch) return;
		    this.propagateEvent("handleEventPropertyChanged", {
			property: "searchValue",
			value: this.frequencyTable.search()
		    });

		} );
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
    let SUPER = new RamaddaBaseTextDisplay(displayManager, id, DISPLAY_FREQUENCY, properties);
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);
    $.extend(this, {
	getWikiEditorTags: function() {
	    return Utils.mergeLists(SUPER.getWikiEditorTags(),
				    [
					"label:Frequency Attributes",
					'orientation="vertical"',
					'tableHeight="300px"',
					'showBars=true',
					'barWidth=200'
				    ]);
	},



        updateUI: function() {
            let records = this.filterData();
	    if(!records) return;
            var allFields = this.getData().getRecordFields();
            var fields = this.getSelectedFields(allFields);
            if (fields.length == 0)
                fields = allFields;
	    var summary={};
	    for (var col = 0; col < fields.length; col++) {
		var f = fields[col];
		if(!Utils.isDefined(summary[f.getId()])) {
		    summary[f.getId()] = {
			counts:{},
			values:[],
			min:value,
			max:value,
			total:0,
			numbers:[],
			field:f
		    }
		}
	    }

	    var showCount = this.getProperty("showCount",true);
	    var showPercent = this.getProperty("showPercent",true);
	    var showBars = this.getProperty("showBars",false);
	    var barWidth = +this.getProperty("barWidth",200);

            for (var rowIdx = 0; rowIdx < records.length; rowIdx++) {
                var row = this.getDataValues(records[rowIdx]);
                for (var col = 0; col < fields.length; col++) {
                    var f = fields[col];
		    var s = summary[f.getId()];
                    var value =  row[f.getIndex()];
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
			    var tuple = {value:value,count:0}
			    s.counts[value] = tuple;
			    s.values.push(tuple);
			}
			s.total++;
			s.counts[value].count++;
		    }
                }
	    }
	    var html = "";
	    for (var col = 0; col < fields.length; col++) {
		var f = fields[col];
		var s = summary[f.getId()];
		//		if(col>0) html+="<br>";
		if(f.isNumeric()) {
		    var numBins = parseFloat(this.getProperty("numBins",10,true));
		    s.bins = [];
		    var range = s.max-s.min;
		    var binWidth = (s.max-s.min)/numBins;
		    var label = "Not defined";
		    s.bins.push(label);
		    var tuple = {value:label,count:0}
		    s.counts[label] = tuple;
		    s.values.push(tuple);
                    var binsProp = this.getProperty(f.getId() +".bins");
		    var hasBins = Utils.stringDefined(binsProp);
		    var binValues;
                    if(hasBins) {
                        var l  = binsProp.split(",");
			binValues = [];
			l.map(v=>binValues.push(+v));
                        for(var i=0;i<l.length-1;i++) {
			    var v1 = +l[i];
			    var v2 = +l[i+1];
			    var label = v1 +" - " + v2;
			    s.bins.push(label);
			    var tuple = {value:label,count:0}
			    s.counts[label] = tuple;
			    s.values.push(tuple);
                        }
		    } else {
			for(var i=0;i<numBins;i++) {
			    var label = (Utils.formatNumber(s.min+binWidth*i)) +" - " + Utils.formatNumber(s.min+binWidth*(i+1));
			    s.bins.push(label);
			    var tuple = {value:label,count:0}
			    s.counts[label] = tuple;
			    s.values.push(tuple);
			}
		    }
		    for(var i=0;i<s.numbers.length;i++) {
			var value = s.numbers[i];
			var bin=0;
			if(!isNaN(value)) {
			    if(binValues) {
				for(var j=0;j<binValues.length-1;j++) {
				    if(value>=binValues[j] && value< binValues[j+1]) {
					bin = j;
					break;
				    }
				}
			    } else {
				if(binWidth!=0) {
				    var perc = (value-s.min)/range;
				    bin = Math.round(perc/(1/numBins))+1;
				    if(bin>numBins) bin = numBins;
				}
			    }
			}
			var label = s.bins[bin]; 
			if(!Utils.isDefined(s.counts[label])) {
			    var tuple = {value:s.bins[bin],count:0}
			    s.counts[label] = tuple;
			    s.values.push(tuple);
			}
			s.total++;
			s.counts[label].count++;
		    }
		}

		var hor = this.getProperty("orientation","") != "vertical";
		if(this.getProperty("floatTable") !=null) {
		    hor = this.getProperty("floatTable")==true;
		}
		html += HtmlUtils.openTag("div", ["class","display-frequency-table","style",hor?"":"display:block;"]);
		html += HtmlUtils.openTag("table", ["cellpadding","3","id",this.getDomId("summary"+col),"table-height",this.getProperty("tableHeight","300",true), "class", "stripe row-border nowrap ramadda-table"]);
		if(this.getProperty("showHeader",true)) {
		    html += HtmlUtils.openTag("thead", []);
		    var label =  HtmlUtils.span(["title","Click to reset","class","display-frequency-label","data-field",s.field.getId()],f.getLabel());

		    
		    label = HtmlUtils.div(["style","max-width:500px;overflow-x:auto;"], label);
		    var count = showCount? HtmlUtils.th(["align","right","width","20%"],HtmlUtils.div(["style","text-align:right"],"Count")):"";
		    var percent  = showPercent?HtmlUtils.th(["align","right","width","20%"],  HtmlUtils.div(["style","text-align:right"],"Percent")):"";
		    var bars = showBars? HtmlUtils.th(["align","right","width",barWidth],HtmlUtils.div(["style","text-align:right"],"&nbsp;")):"";
		    html += HtmlUtils.tr([], HtmlUtils.th(["xxwidth","60%"],  label+ count+ percent+bars));
		    html += HtmlUtils.closeTag("thead");
		}
		html += HtmlUtils.openTag("tbody", []);

		var colors = this.getColorTable(true);
		var dfltColor = this.getProperty("barColor","blue");
		if(colors) {
		    for(var i=0;i<s.values.length;i++) {
			var value = s.values[i].value;
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

		var maxPercent = 0;
		for(var i=0;i<s.values.length;i++) {
		    var count = s.values[i].count;
		    if(count==0) continue;
		    var perc = count/s.total;
		    maxPercent = Math.max(maxPercent, perc);
		}

		for(var i=0;i<s.values.length;i++) {
		    var value = s.values[i].value;
		    var label = value;
		    if(label=="") label="&lt;blank&gt;";
		    var count = s.values[i].count;
		    if(count==0) continue;
		    value = value.replace(/\'/g,"&apos;");
		    value = HtmlUtils.span(["title","Click to select","class","display-frequency-value","data-field",s.field.getId(),"data-value",value],label);
		    var tdv = HtmlUtils.td([], value);
		    var tdc =  (showCount?HtmlUtils.td(["align", "right"], count):"");
		    var perc = count/s.total;
		    var tdp =  showPercent?HtmlUtils.td(["align", "right"], s.total==0?"0":Math.round(perc*100)+"%"):"";
		    var color = s.values[i].color;
		    if(!color) color = dfltColor;

		    var bw = perc/maxPercent;
		    var tdb = showBars?HtmlUtils.td(["valign","center","width",barWidth], HtmlUtils.div(["title",Math.round(perc*100)+"%","style","background:" + color+";height:10px;width:"+ (Math.round(bw*barWidth))+"px"],"")):"";
		    html += HtmlUtils.tr([], 
					 tdv + tdc + tdp+tdb
					);
		}
		html += HtmlUtils.closeTag("tbody");
		html += HtmlUtils.closeTag("table");
		html += HtmlUtils.closeTag("div");
	    }

	    this.writeHtml(ID_DISPLAY_CONTENTS, html);
	    let _this = this;
	    this.jq(ID_DISPLAY_CONTENTS).find(".display-frequency-value").click(function(){
		var click = _this.getProperty("clickFunction")
		var value = $(this).attr("data-value");
		var fieldId = $(this).attr("data-field");
		if(!click || click =="select") {
		    _this.handleEventPropertyChanged(_this,{
			property: "pattern",
			fieldId: fieldId,
			value: value
		    });
		} else if(click == "selectother") {
		    _this.propagateEvent("handleEventPropertyChanged", {
			property: "filterValue",
			value: value,
			id:_this.getFilterId(fieldId),
			fieldId: fieldId,
		    });
		}
	    });
	    this.jq(ID_DISPLAY_CONTENTS).find(".display-frequency-label").click(function(){
		var field = $(this).attr("data-field");
		//		    _this.jq(ID_DISPLAY_CONTENTS).find("[data-field=" + field+"]").css("color","black");
		_this.handleEventPropertyChanged(_this,{
		    property: "filterValue",
		    id:"id",
		    fieldId: field,
		    value: "-all-"
		});
	    });

	    if(this.getProperty("showHeader",true)) {
		for (var col = 0; col < fields.length; col++) {
		    HtmlUtils.formatTable("#" +this.getDomId("summary"+col),{});
		}
	    }
	}
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
            let _this = this;
            this.setContents(this.getMessage("Processing text..."));
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
		strings = this.getFieldsOfType(fields, "string");
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
                this.writeHtml(ID_DISPLAY_CONTENTS, this.getMessage("No text types specified"));
                return;
            }
            var height = this.getProperty("height", "400");
            var html = HtmlUtils.openTag("div", ["id", this.getDomId("tables")]);

            for (var i = 0; i < cols.length; i += 3) {
                var c1 = cols[i];
                var c2 = i + 1 < cols.length ? cols[i + 1] : null;
                var c3 = i + 2 < cols.length ? cols[i + 2] : null;
                var width = c2 ? (c3 ? "33%" : "50%") : "100%";
                var style = "padding:5px";
                var row = "";
                row += HtmlUtils.td(["width", width], HtmlUtils.div(["style", style], c1));
                if (c2)
                    row += HtmlUtils.td(["width", width], HtmlUtils.div(["style", style], c2));
                if (c3)
                    row += HtmlUtils.td(["width", width], HtmlUtils.div(["style", style], c3));
                html += HtmlUtils.tag("table", ["width", "100%"], HtmlUtils.tr(row));
            }
            html += HtmlUtils.closeTag("div");
            this.writeHtml(ID_DISPLAY_CONTENTS, html);
            HtmlUtils.formatTable("#" + this.getDomId("tables") + " .ramadda-table", {
                scrollY: this.getProperty("tableHeight", "200")
            });
        },
        printList: function(title, l) {
            var maxWords = parseInt(this.getProperty("maxWords", 10));
            var minCount = parseInt(this.getProperty("minCount", 0));
            var table = HtmlUtils.openTag("table", ["width", "100%", "class", "stripe hover ramadda-table"]) + HtmlUtils.openTag("thead", []);
            table += HtmlUtils.tr([], HtmlUtils.th([], title) + HtmlUtils.th([], "&nbsp;"));
            table += HtmlUtils.closeTag("thead");
            table += HtmlUtils.openTag("tbody");
            var cnt = 0;
            for (var i = 0; i < l.length; i++) {
                if (l[i].count < minCount) continue;
                var row = HtmlUtils.td([], l[i].normal) +
                    HtmlUtils.td([], l[i].count + " (" + l[i].percent + "%)");
                table += HtmlUtils.tr([], row);
                if (cnt++ > maxWords) break;
            }
            table += HtmlUtils.closeTag("tbody") + HtmlUtils.closeTag("table");
            return table;
        }
    });
}


function RamaddaTextrawDisplay(displayManager, id, properties) {
    var ID_TEXT = "text";
    var ID_OVERLAY = "overlay";
    var ID_OVERLAY_TABLE = "overlaytable";
    var ID_LABEL = "label";
    var ID_SEARCH = "search";
    var ID_HIGHLIGHT = "highlight"; 
    var ID_SHRINK = "shrink";
    
    let SUPER = new RamaddaBaseTextDisplay(displayManager, id, DISPLAY_TEXTRAW, properties);
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);
    $.extend(this, {
	doShrink: this.getProperty("initialShrink",false),
	getWikiEditorTags: function() {
	    return Utils.mergeLists(SUPER.getWikiEditorTags(),
				    [
					"label:Raw Text Attributes",
					'doBubble=true',
					'addLineNumbers=false',
					'labelTemplate="${lineNumber}"',
					'maxLines=1000',
					'pattern="initial search pattern"',
					'fromField=""',
					'linesDescriptor=""'
				    ]);
	},

        checkLayout: function() {
            this.updateUI();
        },
        updateUI: function() {
            let records = this.filterData();
            if (!records) {
                return null;
            }
            var pointData = this.getData();
            this.allRecords = pointData.getRecords();
            var pattern = this.getProperty("pattern");
            if (pattern && pattern.length == 0) pattern = null;
	    if(pattern) pattern = pattern.replace(/"/g,"&quot;");
	    var input = "";
	    if(!this.filterFields || this.filterFields.length==0) 
		input = " " + HtmlUtils.input("pattern", (pattern ? pattern : "") , ["placeholder", "Search text", "id", this.getDomId(ID_SEARCH)]);
	    this.showShrink = this.getProperty("showShrink",false);
	    if(this.showShrink) {
		input += " " + HtmlUtils.checkbox("shrink",["id",this.getDomId(ID_SHRINK)], this.getProperty("initialShrink", true)) +" Shrink ";
	    }

            this.writeHtml(ID_TOP_RIGHT, HtmlUtils.span(["id",this.getDomId(ID_LABEL)]," ") + input);
            let _this = this;
	    this.jq(ID_SHRINK).click(function() {
		_this.doShrink = _this.jq(ID_SHRINK).is(':checked');
		_this.setProperty("initialShrink",_this.doShrink);
		_this.updateUI();
	    });
            this.jq(ID_SEARCH).keypress(function(event) {
                if (event.which == 13) {
                    _this.setProperty("pattern", $(this).val());
		    _this.propagateEvent("handleEventPropertyChanged", {
			property: "pattern",
			value: $(this).val()
		    });
                    _this.updateUI();
                }
            });
            var height = this.getProperty("height", "600");
	    var style = this.getProperty("displayInnerStyle","");
            var html = HtmlUtils.div(["id", this.getDomId(ID_TEXT), "style", "padding:4px;border:1px #ccc solid; max-height:" + height + "px;overflow-y:auto;" + style]);
            this.writeHtml(ID_DISPLAY_CONTENTS, html);
            this.showText();
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
            var pattern = this.getProperty("pattern");
            if (pattern && pattern.length == 0) pattern = null;
	    var asHtml = this.getProperty("asHtml", true);
            var addLineNumbers = this.getProperty("addLineNumbers", true);
	    var labelTemplate = this.getProperty("labelTemplate","");
	    var labelWidth = "10px";
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
            var strings = this.getFieldsOfType(fields, "string");
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

            var corpus = HtmlUtils.openTag("div", ["style","position:relative;"]);
	    corpus+=HtmlUtils.div(["id",this.getDomId(ID_OVERLAY),"style","position:absolute;top:0;left:0;"],
				  HtmlUtils.tag("table",["id",this.getDomId(ID_OVERLAY_TABLE)]));

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
	    if(this.filterFields) {
		this.filterFields.map(f=>{if(f.isString)filterFieldMap[f.getId()]=true;});
	    }
	    var templates = {};
	    fields.map(f=>{
		templates[f.getId()] = this.getProperty(f.getId() +".template");
	    });
            var colorBy = this.getColorByInfo(records);
	    var delimiter = this.getProperty("delimiter","");
	    var rowScale = this.showShrink?this.getProperty("rowScale",0.3):null;

	    if(this.showShrink) {
		corpus+="<tr><td>" + HtmlUtils.getIconImage("fa-caret-down") +"</td></tr>";
	    }
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
			    var value = this.getFilterFieldValues(f);
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
			}
);
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
			rowAttrs.push("style");
			rowStyle +="background:" + Utils.addAlphaToColor(color,"0.25")+";";
		    }
                }
		rowAttrs.push("class");
		rowAttrs.push("display-raw-row");
		var matches=patternMatch.matches(line);
		var hasMatch = matches && patternMatch.hasPattern();
		if(hasMatch) {
		    rowAttrs.push("matched");
		    rowAttrs.push(true);
		}
		if(rowScale) {
		    if(!hasMatch) {
			rowStyle += "-webkit-transform: scale(1," + rowScale +");";
			rowStyle += "line-height:"+ rowScale +";";
			rowAttrs.push("style");
			rowAttrs.push(rowStyle);
		    }
		} else  if(!matches) {
		    continue;
		}
                line = patternMatch.highlight(line);
                displayedLineCnt++;

                if (displayedLineCnt > maxLines) break;

		var lineAttrs = ["title"," ","class", " display-raw-line ","recordIndex",rowIdx]

		if(bubble) line = HtmlUtils.div(["class","ramadda-bubble"],line);
		if(fromField) line+=HtmlUtils.div(["class","ramadda-bubble-from"],  ""+row[fromField.getIndex()]);

		if(highlights) {
		    for(var hi=0;hi<highlights.length;hi++) {
			var h = highlights[hi];
			var s = hi<highlightStyles.length?highlightStyles[hi]:highlightStyles[highlightStyles.length-1];
			s = s.replace(/_comma_/g,",");
			line= line.replace(h, "<span style='" + s +"'>$1</span>");
		    }
		}
		line = HtmlUtils.div(lineAttrs,line);
                if (labelTemplate) {
		    var label =  this.getRecordHtml(record, null, labelTemplate);
		    var num = record.lineNumber;
		    if(!Utils.isDefined(num)) {
			num - lineCnt;
		    }
		    label = label.replace("${lineNumber}", "#" +(num));
		    label = label.replace(/ /g,"&nbsp;");
		    var r =  "";
		    if(this.showShrink) {
			r+= HtmlUtils.td(["width", "5px","style","background:#ccc;"],  HtmlUtils.getIconImage("fa-caret-right",null, ["style","line-height:0px;"]));
		    }
		    r+= HtmlUtils.td(["width", labelWidth], "<a name=line_" + lineCnt + "></a>" +
				       "<a href=#line_" + lineCnt + ">" + label + "</a>&nbsp;  ") +
			HtmlUtils.td([], line);
		    
		    corpus += HtmlUtils.tr(rowAttrs, r);
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
            corpus+= HtmlUtils.closeTag("div");

            if (!asHtml)
                corpus = HtmlUtils.tag("pre", [], corpus);
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
	    var lines =this.jq(ID_TEXT).find(".display-raw-line");
	    lines.click(function() {
		var idx = $(this).attr("recordIndex");
		var record = _this.indexToRecord[idx];
		if(record) {
		    _this.showRecordPopup($(this),record);
		    _this.getDisplayManager().notifyEvent("handleEventRecordSelection", _this, {record: record});
		    _this.highlightLine(idx);
		}
	    });
	    this.makeTooltips(lines,records);
        },
	highlightLine: function(index) {
	    var container = this.jq(ID_TEXT);
	    container.find(".display-raw-line").removeClass("display-raw-line-selected");
	    var sel = "[recordIndex='" + index+"']";
	    var element =  container.find(sel);
	    element.addClass("display-raw-line-selected");

	},
        handleEventRecordSelection: function(source, args) {
	    var index = this.findMatchingIndex(args.record);
	    if(index<0 || !Utils.isDefined(index)) {
		return;
	    }
	    this.highlightLine(index);
	    var container = this.jq(ID_TEXT);
	    var sel = "[recordIndex='" + index+"']";
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
    let SUPER = new RamaddaDisplay(displayManager, id, DISPLAY_TEXT, properties);
    $.extend(this, SUPER);
    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
        lastHtml: "<p>&nbsp;<p>&nbsp;<p>",
        needsData: function() {
            return true;
        },
	updateUI: function() {
            SUPER.updateUI.call(this);
	    if(!this.getProperty("recordTemplate")) {
		this.setProperty("recordTemplate","${default}");
	    }
            let records = this.filterData();
	    if(records && records.length>0) {
		this.lastHtml = this.getRecordHtml(records[0]);
		this.setContents(this.lastHtml);
	    }
        },
        handleEventRecordSelection: function(source, args) {
            this.lastHtml = this.getRecordHtml(args.record);
            this.setContents(this.lastHtml);
        }
    });
}




function RamaddaTreeDisplay(displayManager, id, properties) {
    let SUPER = new RamaddaDisplay(displayManager, id, DISPLAY_TREE, properties);
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
	countToRecord: {},
        needsData: function() {
            return true;
        },
	getWikiEditorTags: function() {
	    return Utils.mergeLists(SUPER.getWikiEditorTags(),
				    [
					"label:Tree Attributes",
					"maxDepth=3",
					"showDetails=false",
				    ])},

        updateUI: function() {
            var records = this.filterData();
            if (!records) return;
	    let roots=null;
	    try {
		roots = this.makeTree();
	    } catch(error) {
                this.setContents(this.getMessage(error.toString()));
		return;
	    }
	    var html = "";
	    let baseId = this.getDomId("node");
	    let cnt=0;
	    let depth = 0;
	    let maxDepth = +this.getProperty("maxDepth",10);
	    let template = this.getProperty("recordTemplate","${default}");
	    var showDetails = this.getProperty("showDetails",true);
	    let _this =this;
	    let func = function(node) {
		cnt++;
		if(node.record) {
		    _this.countToRecord[cnt] = node.record;
		}
		depth++;
		let on = node.children.length>0 && depth<=maxDepth;
		let details = null;
		if(showDetails && node.record) {
		    details = _this.getRecordHtml(node.record,null, template);
		    if(details == "") details = null;
		}
		let image = "";
		if(node.children.length>0 || details) {
		    image = HtmlUtils.image(on?icon_downdart:icon_rightdart,["id",baseId+"_toggle_image" + cnt]) + " ";
		}
		html+=HtmlUtils.div(["class","display-tree-toggle","id",baseId+"_toggle" + cnt,"toggle-state",on,"block-count",cnt], image +  node.label);
		html+=HtmlUtils.openTag("div",["id", baseId+"_block"+cnt,"class","display-tree-block","style","display:" + (on?"block":"none")]);
		if(details && details!="") {
		    if(node.children.length>0) {
			html+= HtmlUtils.div(["class","display-tree-toggle-details","id",baseId+"_toggle_details" + cnt,"toggle-state",false,"block-count",cnt], HtmlUtils.image(icon_rightdart,["id",baseId+"_toggle_details_image" + cnt]) + " Details");
			html+=HtmlUtils.div(["id", baseId+"_block_details"+cnt,"class","display-tree-block","style","display:none"],details);
		    } else {
			html+=details;
		    }
		}
			     
		if(node.children.length>0) {
		    node.children.map(func);
		}
		depth--;
		html+=HtmlUtils.closeTag("div");
	    }
//	    console.log("roots:" + roots.length);
	    roots.map(func);
	    this.myRecords = [];
            this.displayHtml(html);
	    this.jq(ID_DISPLAY_CONTENTS).find(".display-tree-toggle").click(function() {
		var state = (/true/i).test($(this).attr("toggle-state"));
		state = !state;
		var cnt = $(this).attr("block-count");
		var block = $("#"+ baseId+"_block"+cnt);
		var img = $("#"+ baseId+"_toggle_image"+cnt);
		$(this).attr("toggle-state",state);
		if(state)  {
		    block.css("display","block");
		    img.attr("src",icon_downdart);
		} else {
		    block.css("display","none");
		    img.attr("src",icon_rightdart);
		}
		let record = _this.countToRecord[cnt];
		if(record) {
		    _this.getDisplayManager().notifyEvent("handleEventRecordSelection", this, {record: record});
		}
	    });
	    this.jq(ID_DISPLAY_CONTENTS).find(".display-tree-toggle-details").click(function() {
		var state = (/true/i).test($(this).attr("toggle-state"));
		state = !state;
		var cnt = $(this).attr("block-count");
		var block = $("#"+ baseId+"_block_details"+cnt);
		var img = $("#"+ baseId+"_toggle_details_image"+cnt);
		$(this).attr("toggle-state",state);
		if(state)  {
		    block.css("display","block");
		    img.attr("src",icon_downdart);
		} else {
		    block.css("display","none");
		    img.attr("src",icon_rightdart);
		}
	    });
        },
    });
}

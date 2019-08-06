/**
Copyright 2008-2019 Geode Systems LLC
*/


var DISPLAY_WORDCLOUD = "wordcloud";
var DISPLAY_TEXTSTATS = "textstats";
var DISPLAY_TEXTANALYSIS = "textanalysis";
var DISPLAY_TEXTRAW = "textraw";
var DISPLAY_TEXT = "text";
var DISPLAY_CARDS = "cards";



addGlobalDisplayType({
    type: DISPLAY_TEXT,
    label: "Text Readout",
    requiresData: false,
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
                        values = Utils.tokenizeWords(values[0], stopWords, extraStopWords, removeArticles);
                    }
                    for (var valueIdx = 0; valueIdx < values.length; valueIdx++) {
                        var value = values[valueIdx];
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
    RamaddaUtil.inherit(this,SUPER);
    addRamaddaDisplay(this);
    $.extend(this, {
        getContentsStyle: function() {
            return "";
        },
        updateUI: function() {
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
            this.labelField = this.getFieldById(fields, this.getProperty("labelField", null, true));
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
            this.sortFields = this.getFieldsByIds(fields, this.getProperty("sortFields", "", true));
            var contents = "";

	    if(!this.groupByHtml) {
		this.groupByHtml = "";
		if(this.groupByFields.length>0) {
		    var options = [["","--"]];
		    this.groupByFields.map(field=>{
			    options.push([field.getId(),field.getLabel()]);
			});
		    this.groupByHtml =  " Group by: ";
		    for(var i=0;i<this.groupByMenus;i++) {
			var selected = "";
			if(i<this.initGrouping.length) {
			    selected = this.initGrouping[i].getId();
			}
			this.groupByHtml+= HtmlUtils.select("",["id",this.getDomId(ID_GROUPBY_FIELDS+i)],options,selected)+"&nbsp;";
		    }
		    this.groupByHtml+="&nbsp;";
		    this.jq(ID_HEADER1).html(this.groupByHtml);
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
	 displaySearchResults: function(records, fields) {
            records = Utils.cloneList(records);
            if(this.sortFields.length) {
                var sortAscending = this.getProperty("sortAscending",true);
                records.sort((a,b)=>{
                        var row1 = this.getDataValues(a);
                        var row2 = this.getDataValues(b);
                        var result = 0;
                        for(var i=0;i<this.sortFields.length;i++) {
                            var sortField = this.sortFields[i];
                            var v1 = row1[sortField.getIndex()];
                            var v2 = row2[sortField.getIndex()];
                            if(v1<v2) result = sortAscending?-1:-1;
                            else if(v1>v2) result = sortAscending?1:-1;
                            else result = 0;
                            if(result!=0) break;
                        }
                        return result;
                    });
            }


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
                        if(field.isNumeric && !field.range) {
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

            function groupNode(id) {
                $.extend(this,{
                        id: id,
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
            for (var rowIdx = 0; rowIdx <records.length; rowIdx++) {
                var row = this.getDataValues(records[rowIdx]);
                var contents = "";
                var tooltip = "";
                this.tooltipFields.map(field=>{
                        if(tooltip!="") tooltip+="&#10;";
                        tooltip+=field.getValue(row);
                    });
                var label = "";
                var caption="";
                if(this.captionFields.length>0) {
                    if(this.captionTemplate) caption  = this.captionTemplate;
                    this.captionFields.map(field=>{
                            if(this.captionTemplate)
                                caption = caption.replace("\${" + field.getId()+"}",field.getValue(row));
                            else
                                caption+=field.getValue(row)+"<br>";
                        });
                    if(this.urlField) {
                        var url = this.urlField.getValue(row);
                        if(url && url!="") {
                            caption = "<a style='color:inherit;'  href='" +url+"' target=_other>" +caption+"</a>";
                        }
                    }
                }
                if(this.labelField) label = "<br>" + row[this.labelField.getIndex()];
                var html ="";
                var img = null;
                if(this.imageField) {
                    img = row[this.imageField.getIndex()];
                    if(this.onlyShowImages && !Utils.stringDefined(img)) continue;
                } 
                
                var  imgAttrs= ["class","display-cards-popup","data-fancybox",this.getDomId("gallery"),"data-caption",caption];
                if(Utils.stringDefined(img)) {
                    img =  HtmlUtils.href(img, HtmlUtils.image(img,["width",width]),imgAttrs)+label;
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
                    if(groupField.isNumeric) {
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
                        group.members.push(child = new groupNode(value));
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
                        html+="<td width=" + width+"%>";
                        html+=HtmlUtils.tag("div",["class","display-cards-header"],child.id +" (" + child.getCount()+")");
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
            if (this.getProperty("showFrequency", true))
                HtmlUtils.formatTable("#" + this.getDomId("table_frequency"), {
                    scrollY: this.getProperty("tableFrequenecyHeight", tableHeight),
                    searching: this.getProperty("showSearch", true)
                });
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
            if (pattern && pattern.length == 0) pattern = null;
            this.writeHtml(ID_TOP_RIGHT, HtmlUtils.input("pattern", (pattern ? pattern : ""), ["placeholder", "Search text", "id", this.getDomId("search")]));
            let _this = this;
            this.jq("search").keypress(function(event) {
                if (event.which == 13) {
                    _this.setProperty("pattern", $(this).val());
                    _this.updateUI();
                }
            });
            this.writeHtml(ID_DISPLAY_CONTENTS, HtmlUtils.div(["id", this.getDomId(ID_TEXT)], ""));
            this.showText();
        },
        showText: function() {
            let records = this.filterData();
            if (!records) {
                return null;
            }
            var pattern = this.getProperty("pattern");
            if (pattern && pattern.length == 0) pattern = null;
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
            if (pattern) {
                re = new RegExp("(" + pattern + ")");
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
                if (re) {
                    if (!line.toLowerCase().match(re)) continue;
                    line = line.replace(re, "<span style=background:yellow;>$1</span>");
                }
                displayedLineCnt++;

                if (displayedLineCnt > maxLines) break;

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


function RamaddaTextDisplay(displayManager, id, properties) {
    var SUPER;
    $.extend(this, SUPER = new RamaddaDisplay(displayManager, id, DISPLAY_TEXT, properties));
    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
        lastHtml: "<p>&nbsp;<p>&nbsp;<p>",
        initDisplay: function() {
            SUPER.initDisplay.call(this);
            this.setContents(this.lastHtml);
        },
        handleEventRecordSelection: function(source, args) {
            this.lastHtml = args.html;
            this.setContents(args.html);
        }
    });
}
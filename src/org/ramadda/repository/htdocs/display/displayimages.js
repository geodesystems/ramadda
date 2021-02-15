/*
  Copyright 2008-2020 Geode Systems LLC
*/

const DISPLAY_SLIDES = "slides";
const DISPLAY_IMAGES = "images";
const DISPLAY_IMAGEZOOM = "imagezoom";
const DISPLAY_CARDS = "cards";

addGlobalDisplayType({
    type: DISPLAY_CARDS,
    label: "Cards",
    requiresData: true,
    forUser: true,
    category: CATEGORY_MAPS_IMAGES
});


addGlobalDisplayType({
    type: DISPLAY_IMAGES,
    label: "Images",
    requiresData: true,
    forUser: true,
    category:CATEGORY_MAPS_IMAGES
});

addGlobalDisplayType({
    type: DISPLAY_IMAGEZOOM,
    label: "Image Zoom",
    requiresData: true,
    forUser: true,
    category:CATEGORY_MAPS_IMAGES
});

addGlobalDisplayType({
    type: DISPLAY_SLIDES,
    label: "Slides",
    requiresData: true,
    forUser: true,
    category: CATEGORY_MAPS_IMAGES
});



function RamaddaCardsDisplay(displayManager, id, properties) {
    const ID_RESULTS = "results";
    const SUPER =  new RamaddaFieldsDisplay(displayManager, id, DISPLAY_CARDS, properties);
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
            this.imageField = this.getFieldByType(fields, "image");
            this.urlField = this.getFieldByType(fields, "url");
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
		    this.groupByHtml +=  HU.span([CLASS,"ramadda-button",ID,this.getDomId("docolors")], "Do colors")+" " +
		    HU.span([CLASS,"ramadda-button",ID,this.getDomId("docolorsreset")], "Reset");
		if(this.groupByFields.length>0) {
		    var options = [["","--"]];
		    this.groupByFields.map(field=>{
			options.push([field.getId(),field.getLabel()]);
		    });

		    this.groupByHtml +=  HU.span([CLASS,"display-fitlerby-label"], " Group by: ");
		    for(var i=0;i<this.groupByMenus;i++) {
			var selected = "";
			if(i<this.initGrouping.length) {
			    selected = this.initGrouping[i].getId();
			}
			this.groupByHtml+= HU.select("",[ID,this.getDomId(ID_GROUPBY_FIELDS+i)],options,selected)+"&nbsp;";
		    }
		    this.groupByHtml+="&nbsp;";
		    this.jq(ID_HEADER1).html(HU.div([CLASS,"display-filterby"],this.groupByHtml));
		    this.jq("docolors").button().click(()=>{
			this.analyzeColors();
		    });
		    this.jq("docolorsreset").button().click(()=>{
			this.updateUI();
		    });


		}
	    }



            contents += HU.div([ID,this.getDomId(ID_RESULTS)]);
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
		    html+=HU.div([STYLE,HU.css('display','inline-block','width', width + "px','height', img.height +'px','background','rgb(" + c[0]+"," + c[1] +"," + c[2]+")")],"");
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
                
                var  imgAttrs= [CLASS,"display-cards-popup","data-fancybox",this.getDomId("gallery"),"data-caption",caption];
		if(img) img = img.trim();
                if(Utils.stringDefined(img)) {
		    if(this.colorAnalysisEnabled)
			img = ramaddaBaseUrl+"/proxy?url=" + img;
                    img =  HU.href(img, HU.div([ID,this.getDomId("gallery")+"div" + imgCnt], HU.image(img,["width",width,ID,this.getDomId("gallery")+"img" + imgCnt])),imgAttrs)+label;
		    imgCnt++;
                    html = HU.div([CLASS,"display-cards-item", TITLE, tooltip, STYLE,HU.css('margin', margin+'px')], img);
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
                    var attrs = [TITLE,tooltip,CLASS,"ramadda-gridbox display-cards-card",STYLE,style];
                    if(this.altLabelField) {
                        html = HU.div(attrs,this.altLabelField.getValue(row));
                    } else {
                        html = HU.div(attrs,caption);
                    }
                    html =  HU.href("", html,imgAttrs);
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
	    let total = topGroup.getCount();
            let topHtml = HU.div([CLASS,"display-cards-header"],"Total" +" (" + total+")");
            topHtml+=this.makeGroupHtml(topGroup, topGroup);
            this.writeHtml(ID_RESULTS, topHtml);
            this.jq(ID_RESULTS).find("a.display-cards-popup").fancybox({
                caption : function( instance, item ) {
                    return  $(this).data('caption') || '';
                }});
        },
        makeGroupHtml: function(group, topGroup) {
            if(group.members.length==0) return "";
            var html="";
            if(group.members[0].isGroup) {
                group.members.sort((a,b)=>{
                    if(a.id<b.id) return -1;
                    if(a.id>b.id) return 1;
                    return 0;
                });
                var width = group.members.length==0?"100%":100/group.members.length;
                html +=HU.open(TABLE,[WIDTH,'100%','border',0]) +HU.open(TR,['valign','top']);
                for(var i=0;i<group.members.length;i++) {
                    var child = group.members[i];
		    var prefix="";
		    if(child.field)
			prefix = child.field.getLabel()+": ";
                    html+=HU.open(TD,[WIDTH, width+"%"]);
		    let perc = Math.round(100*child.getCount()/topGroup.getCount());
		    html+=HU.div([CLASS,"display-cards-header"],prefix+child.id +" (#" + child.getCount()+" - " + perc +"%)");
		    html+= this.makeGroupHtml(child, topGroup);
                    html+=HU.close(TD);
                }
                html +=HU.close(TR, TABLE);
            } else {
                html+=Utils.join(group.members,"");
            }
            return html;
        }
    });
}




function RamaddaImagesDisplay(displayManager, id, properties) {
    const ID_GALLERY = "gallery";
    const ID_NEXT = "next";
    const ID_PREV = "prev";
    const ID_IMAGES = "images";
    const SUPER =  new RamaddaFieldsDisplay(displayManager, id, DISPLAY_IMAGES, properties);
    
    RamaddaUtil.inherit(this,SUPER);
    addRamaddaDisplay(this);
    this.defineProperties([
	{label:'Image Gallery Properties'},
	{p:'imageField',wikiValue:''},
	{p:'labelFields',wikiValue:''},
	{p:'topLabelTemplate',wikiValue:''},	
	{p:'bottomLabelTemplate',wikiValue:''},	
	{p:'tooltipFields',wikiValue:''},
	{p:'numberOfImages',wikiValue:'100'},	
	{p:'includeBlanks',wikiValue:'true'},
	{p:'imageWidth',wikiValue:'150'},
	{p:'imageMargin',wikiValue:'10px'},
	{p:'decorate',wikiValue:'false'},
	{p:'doPopup',wikiValue:'false'},
	{p:'imageStyle',wikiValue:''},			
	{p:'minHeightGallery',wikiValue:150},
	{p:'maxHeightGallery',wikiValue:150},	
	{p:'columns',wikiValue:'5'},
    ]);
    $.extend(this, {
	startIndex:0,
	dataFilterChanged: function() {
	    this.startIndex=0;
	    this.updateUI();
	},
        handleEventRecordSelection: function(source, args) {
	    let blocks = this.jq(ID_DISPLAY_CONTENTS).find(".display-images-block");
	    let select = HU.attrSelect(RECORD_ID, args.record.getId());
	    let block = this.jq(ID_DISPLAY_CONTENTS).find(select);
	    blocks.css('background','transparent');
	    block.css('background',HIGHLIGHT_COLOR);
	    HU.scrollVisible(this.jq(ID_IMAGES),block);
	},
        updateUI: function() {
            let records = this.filterData();
            if(!records) return;
	    let includeBlanks  = this.getPropertyIncludeBlanks(false);
	    let imageField = this.getFieldById(null, this.getProperty("imageField"));
	    if(!imageField) {
		imageField = this.getFieldByType(null,"image");
	    }

            let pointData = this.getData();
            let fields = pointData.getRecordFields();

            let labelFields = this.getFieldsByIds(null, this.getProperty("labelFields", null, true));
            let topLabelTemplate = this.getPropertyTopLabelTemplate();
            let bottomLabelTemplate = this.getPropertyBottomLabelTemplate();	    
            let tooltipFields = this.getFieldsByIds(null, this.getProperty("tooltipFields", null, true));
	    if(!imageField) {
		this.setContents(this.getMessage("No image field in data"));
		return;
	    }
	    let decorate = this.getPropertyDecorate(true);
	    let columns = +this.getPropertyColumns(0);
	    let number = +this.getPropertyNumberOfImages(50);
	    let width = this.getPropertyImageWidth("150");
	    let imageStyle = this.getPropertyImageStyle("");
	    let contents = "";
	    let uid = HtmlUtils.getUniqueId();
	    let base = "gallery"+uid;
	    let displayedRecords = [];
	    let doPopup = this.getPropertyDoPopup(true);
	    let recordIndex = 0;
	    let columnCnt = -1;
	    let columnMap = {};
	    let class1= "display-images-image-outer display-images-block ";
	    let class2 = "display-images-image-inner";
	    let style = "";
	    let idToRecord = {};
	    if(!decorate) {
		class2 = "";
		class1 = "display-images-block";
		style = HU.css("margin",this.getPropertyImageMargin("10px"));
	    }
	    if(columns) {
		if(width.endsWith("%"))
		    style+=HU.css(WIDTH,width);
	    }
	    if(this.startIndex<0) this.startIndex=0;
	    if(this.startIndex>records.length) this.startIndex=records.length-number;
	    let cnt = 1;
            for (let rowIdx = this.startIndex; rowIdx < records.length; rowIdx++) {
		let record = records[rowIdx];
                let row = this.getDataValues(record);
		let image = record.getValue(imageField.getIndex());
		if(image=="" && !includeBlanks) {
		    continue;
		}
		if(cnt++>number) break;
		displayedRecords.push(record);
		idToRecord[record.getId()] = record;
		let topLabel = null;
		if(topLabelTemplate) {
		    topLabel = this.getRecordHtml(record,fields,topLabelTemplate);
		}
		let label = "";
		if(bottomLabelTemplate) {
		    label = this.getRecordHtml(record,fields,bottomLabelTemplate);
		} else {
		    labelFields.forEach(l=>{
			let value  = record.getValue(l.getIndex());
			if(value.getTime) {
			    value = this.formatDate(value);
			} 
			label += " " + value; 
		    });
		}
		let tt = "";
		tooltipFields.forEach(l=>{tt += "\n" + l.getLabel()+": " + row[l.getIndex()]});
		tt = tt.trim();
		let img = image==""?SPACE1:HU.image(image,[STYLE,imageStyle,"alt",label,ID,base+"image" + rowIdx, WIDTH,width]);
		let topLbl = (topLabel!=null?HU.div([CLASS,"display-images-toplabel"], topLabel):"");
		let lbl = HU.div([CLASS,"display-images-label"], label.trim());
		let block = 
		    HU.div([STYLE, style, RECORD_ID,record.getId(),RECORD_INDEX,recordIndex++,ID,base+"div"+  rowIdx, CLASS, class1,TITLE,tt],
			   HU.div([CLASS,class2], topLbl + img + lbl));
		if(doPopup) {
		    block = HU.href(image,block,[CLASS,"popup_image","data-fancybox",base,"data-caption",label]);
		}
		if(columns) {
		    if(++columnCnt>=columns) {
			columnCnt=0;
		    }
		    if(!columnMap[columnCnt]) columnMap[columnCnt] = "";
		    columnMap[columnCnt] += block;
		} else {
		    contents += block;
		}
	    }
	    if(columns) {
		contents = "<table border=0 width=100%><tr valign=top>";
		for(let col=0;true;col++) {
		    if(!columnMap[col]) break;
		    contents+=HU.td(['align','center'],columnMap[col]);
		}
		contents+="</tr></table>";
	    }

	    let header = "";
	    if(this.startIndex>0) {
		header += HU.span([ID,this.getDomId(ID_PREV)],"Previous")+" ";
	    }
	    if(this.startIndex+number<records.length) {
		header += HU.span([ID,this.getDomId(ID_NEXT)],"Next") +" ";
	    }
	    cnt--;
	    if(number<records.length) {
		header += "Showing " + (this.startIndex+1) +" - " +(this.startIndex+cnt);
		header += " of " + records.length +" images";
	    }

	    if(header!="") header  = header +"<br>";

	    if(this.getPropertyMinHeightGallery() || this.getPropertyMaxHeightGallery()) {
		let css = "";
		if(this.getPropertyMinHeightGallery()) css+=HU.css("min-height",HU.getDimension(this.getPropertyMinHeightGallery()));
		if(this.getPropertyMinHeightGallery())	css+= HU.css("max-height",HU.getDimension(this.getPropertyMaxHeightGallery()));
		contents = HU.div([ID,this.getDomId(ID_IMAGES),STYLE,css+HU.css("overflow-y","auto")], contents);
	    }

            this.writeHtml(ID_DISPLAY_CONTENTS, header + contents);
	    let blocks = this.jq(ID_DISPLAY_CONTENTS).find(".display-images-block");
	    this.makeTooltips(blocks,displayedRecords);
	    if(!doPopup) {
		let _this = this;
		blocks.click(function() {
		    let record = idToRecord[$(this).attr(RECORD_ID)];
		    if(record) {
			_this.propagateEventRecordSelection({record: record});
		    }
		});
	    }
	    this.jq(ID_PREV).button().click(()=>{
		this.startIndex-=number;
		this.updateUI();
	    });
	    this.jq(ID_NEXT).button().click(()=>{
		this.startIndex+=number;
		this.updateUI();
	    });

	    if(this.getProperty("propagateEventRecordList",false)) {
		this.getDisplayManager().notifyEvent("handleEventRecordList", this, {
		    recordList: displayedRecords,
		});
	    }
	}
    })
}



function RamaddaImagezoomDisplay(displayManager, id, properties) {
    const ID_THUMBS = "thumbs";
    const ID_THUMB = "thumb";    
    const ID_IMAGE = "image";
    const ID_IMAGEINNER = "imageinner";    
    const ID_POPUP = "imagepopup";
    const ID_POPUPIMAGE = "imagepopupimage";
    const ID_RECT = "imagerect";            
    const SUPER =  new RamaddaFieldsDisplay(displayManager, id, DISPLAY_IMAGEZOOM, properties);
    RamaddaUtil.inherit(this,SUPER);
    addRamaddaDisplay(this);
    this.defineProperties([
	{label:"Image Zoom Attributes"},
	{p:'labelFields'},
	{p:'thumbField'},
	{p:'thumbWidth',wikiValue:'100'},
	{p:'imageWidth',wikiValue:'150'},
	{p:'urlField'},
	{p:'popupWidth'},
	{p:'popupHeight'},	
	{p:"popupImageWidth"}
    ]);


    $.extend(this, {
	dataFilterChanged: function() {
	    this.updateUI();
	},
        updateUI: function() {
            let pointData = this.getData();
            if (pointData == null) return;
            let records = this.filterData();
            if(!records) return;
            let fields = pointData.getRecordFields();
            this.urlField = this.getFieldById(fields, this.getProperty("urlField", "url"));
	    this.imageField = this.getFieldById(fields,"image");
	    if(!this.imageField)
		this.imageField = this.getFieldByType(fields,"image");
	    if(!this.imageField) {
		this.setContents(this.getMessage("No image field in data"));
		return;
	    }
	    this.labelFields = this.getFieldsByIds(fields, this.getPropertyLabelFields());
            let thumbField = this.getFieldById(fields, this.getProperty("thumbField", "thumb")) || this.imageField;
	    let thumbWidth = parseFloat(this.getProperty("thumbWidth",100));
	    let height=this.getHeightForStyle();
	    let imageWidth = this.getProperty("imageWidth",500);
	    this.popupWidth =  +this.getProperty("popupWidth",imageWidth);
	    this.popupHeight = +this.getProperty("popupHeight",300);

	    let rect = HU.div([STYLE,HU.css("border","1px solid " + HIGHLIGHT_COLOR,"width","20px","height","20px","left","10px","top","10px","display","none","position","absolute","z-index",1000,"pointer-events","none"),ID, this.getDomId(ID_RECT)]);
	    let imageDiv = HU.div(["style","position:relative"],
				  rect+
				  HU.div([ID,this.getDomId(ID_IMAGE),STYLE,HU.css("position","relative") ]) +
				  HU.div([ID,this.getDomId(ID_POPUP),CLASS,"display-imagezoom-popup",STYLE,HU.css("z-index","100","display","none",WIDTH,this.popupWidth+"px",HEIGHT,this.popupHeight+"px","overflow-y","hidden","overflow-x","hidden", "position","absolute","top","0px","left", imageWidth+"px")],""));

	    let contents = HU.table(["border",0,WIDTH,"100%"],
				    HU.tr(["valign","top"],
					  HU.td([WIDTH,"2%"],
						HU.div([ID,this.getDomId(ID_THUMBS), STYLE,HU.css("max-height",height,"overflow-y","auto","display","inline-block")],"")) +
					  HU.td([WIDTH,"90%"],
						imageDiv)));
	    let thumbsHtml = "";
	    let first = null;
            for (let rowIdx = 0; rowIdx < records.length; rowIdx++) {
                let row = this.getDataValues(records[rowIdx]);
		let image = row[this.imageField.getIndex()];
		if(image=="") {
		    continue;
		}
		if(!first) first=records[rowIdx];
		let thumb = row[thumbField.getIndex()];		
		thumbsHtml += HU.image(thumb,[RECORD_INDEX,rowIdx,ID,this.getDomId(ID_THUMB)+rowIdx,WIDTH, thumbWidth,CLASS,"display-imagezoom-thumb"])+"<br>\n";
	    }
            this.writeHtml(ID_DISPLAY_CONTENTS, contents);
	    this.jq(ID_THUMBS).html(thumbsHtml);
	    let _this = this;
	    let thumbs = this.jq(ID_THUMBS).find(".display-imagezoom-thumb");
	    let thumbSelect = (thumb=>{
		thumbs.css("border","1px solid transparent");
		thumb.css("border","1px solid " + HIGHLIGHT_COLOR);
		let index = parseFloat(thumb.attr(RECORD_INDEX));
		HU.addToDocumentUrl("imagezoom_thumb",index);
		let record = records[index]
		_this.handleImage(record);
		_this.propagateEventRecordSelection({record: record});
	    });

	    thumbs.mouseover(function() {	
		thumbSelect($(this));
	    });
	    this.jq(ID_THUMBS).css("border","1px solid transparent");
	    let selectedIndex =  HU.getUrlArgument("imagezoom_thumb");
	    let x = HU.getUrlArgument("imagezoom_x");
	    let y = HU.getUrlArgument("imagezoom_y");	    
	    let selectedThumb = this.jq(ID_THUMB+(selectedIndex||"0"));
	    if(selectedThumb.length)
		thumbSelect(selectedThumb);
	    if(selectedIndex) this.showPopup();
	    if(Utils.isDefined(x)) {
		setTimeout(()=>{
		    this.handleMouseMove({x:parseFloat(x),y:parseFloat(y)});
		},250);
	    }

	    this.jq(ID_IMAGE).click((e)=>{
		let width = +this.getProperty("popupImageWidth",500);
                if (event.shiftKey) {
		    this.setProperty("popupImageWidth",Math.max(width*0.9,500));
		} else {
		    this.setProperty("popupImageWidth",width*1.2);
		}
		this.showPopup();
		this.handleMouseMove();
	    });
	},
	showPopup: function() {
	    if(!this.currentRecord) return;
	    let row = this.getDataValues(this.currentRecord);
	    let image = row[this.imageField.getIndex()];
	    this.jq(ID_POPUP).css("display","block");
	    let imageAttrs = [ID,this.getDomId(ID_POPUPIMAGE),STYLE,HU.css("xposition","absolute")];
	    if(this.getPropertyPopupImageWidth()) {
		imageAttrs.push(WIDTH);
		imageAttrs.push(this.getPropertyPopupImageWidth());
	    } 
	    this.jq(ID_POPUP).html(HU.image(image,imageAttrs));
	},
	handleImage: function(record, offset) {
	    let _this = this;
	    this.currentRecord = record;
            let row = this.getDataValues(record);
	    let image = row[this.imageField.getIndex()];
	    let width = this.getProperty("imageWidth",500);
    	    let label = "";
	    if(this.labelFields.length>0) {
		this.labelFields.map(l=>{label += " " + row[l.getIndex()]});
		if(this.urlField) {
                    var url = this.urlField.getValue(row);
                    if(url && url!="") {
                        label = "<a style='color:inherit;'  href='" +url+"' target=_other>" +label+ "</a>";

                    }
		}
	    }
	    let html =  HU.image(image,["x","+:zoom in/-:zoom out",STYLE,HU.css("z-index",1000),WIDTH, width,ID,this.getDomId(ID_IMAGEINNER)]);
	    if(label!="")
		html+=HU.div([STYLE,"color:#000"],label);
	    this.jq(ID_IMAGE).html(html);

	    this.jq(ID_POPUP).html("");
	    this.jq(ID_POPUP).css("display","none");
	    this.jq(ID_IMAGEINNER).mouseenter(()=>{
		this.showPopup();
	    });
	    this.jq(ID_IMAGEINNER).mouseout(()=>{
		this.jq(ID_POPUP).html("");
		this.jq(ID_POPUP).css("display","none");
		this.jq(ID_RECT).css("display","none");		
	    });

	    this.jq(ID_IMAGEINNER).mousemove((e)=>{
		this.handleMouseMove({
		    event:e});
	    });
	    if(offset)
		this.jq(ID_POPUPIMAGE).offset(offset);
	},
	handleMouseMove(params) {
	    if(!params) params = {event:this.currentMouseEvent};
	    this.currentMouseEvent=params.event;
	    let image = this.jq(ID_IMAGEINNER);
	    let w = image.width();
	    let h = image.height();
	    let popupImage = this.jq(ID_POPUPIMAGE);
	    let iw = popupImage.width();
	    let ih = popupImage.height();		
	    if(h==0 || ih==0) return false;
	    let popupWidth = popupImage.parent().width();
	    let popupHeight = popupImage.parent().height(); 	    	    
	    let scaleX = w/iw;
	    let scaleY = h/ih;
	    let scaledWidth = scaleX*popupWidth;
	    let scaledHeight = scaleY*popupHeight;
	    let sw2 = scaledWidth/2;
	    let sh2 = scaledHeight/2;	    

	    let parentOffset = image.parent().offset();
	    if(!Utils.isDefined(params.x)) 
		params.x = params.event.pageX - parentOffset.left;
	    if(!Utils.isDefined(params.y)) 
		params.y = params.event.pageY - parentOffset.top;
	    if(params.x<sw2) params.x=sw2;
	    if(params.y<sh2) params.y=sh2;
	    if(params.x>w-sw2) params.x=w-sw2;
	    if(params.y>h-sh2) params.y=h-sh2;	    	    
	    


	    HU.addToDocumentUrl("imagezoom_x",params.x);
	    HU.addToDocumentUrl("imagezoom_y",params.y);	    

	    let offX = scaleX*iw/2;
	    let offY = scaleY*ih/2;		
	    let percentW = (params.x-sw2)/w;
	    let percentH = (params.y-sh2)/h;

	    if(popupImage.parent().length==0) return;
	    let pp = popupImage.parent().offset();
	    let offset = {
		left:pp.left-percentW*iw,
		top:pp.top-percentH*ih};
	    this.jq(ID_POPUPIMAGE).offset(offset);		    
	    let rect = this.jq(ID_RECT);
	    rect.css({"display":"block",top:params.y-sh2+"px",left:params.x-sw2+"px",width:scaledWidth+"px",height:scaledHeight+"px"});
	    return true;
	},

    })
}






function RamaddaSlidesDisplay(displayManager, id, properties) {
    const ID_SLIDE = "slide";
    const ID_PREV = "prev";
    const ID_NEXT = "next";
    if(!Utils.isDefined(properties.displayStyle)) properties.displayStyle = "background:rgba(0,0,0,0);";
    const SUPER =  new RamaddaFieldsDisplay(displayManager, id, DISPLAY_SLIDES, properties);
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
	    var left = HU.div([ID, this.getDomId(ID_PREV), STYLE,HU.css('font-size','200%'),CLASS,"display-slides-arrow-left fas fa-angle-left"]);
	    var right = HU.div([ID, this.getDomId(ID_NEXT), STYLE,HU.css('font-size','200%'), CLASS,"display-slides-arrow-right fas fa-angle-right"]);
	    var slide = HU.div([STYLE,HU.css('overflow-y','auto','max-height', height), ID, this.getDomId(ID_SLIDE), CLASS,"display-slides-slide"]);

	    var navStyle = "padding-top:20px;";
	    var contents = HU.div([STYLE,HU.css('position','relative')], "<table width=100%><tr valign=top><td width=20>" + HU.div([STYLE,navStyle], left) + "</td><td>" +
					 slide + "</td>" +
					 "<td width=20>" + HU.div([STYLE,navStyle],right) + "</td></tr></table>");
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




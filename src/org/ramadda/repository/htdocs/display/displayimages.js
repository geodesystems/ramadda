/*
  Copyright 2008-2021 Geode Systems LLC
*/

const DISPLAY_SLIDES = "slides";
const DISPLAY_IMAGES = "images";
const DISPLAY_IMAGEZOOM = "imagezoom";
const DISPLAY_CARDS = "cards";


addGlobalDisplayType({
    type: DISPLAY_IMAGES,
    label: "Images",
    requiresData: true,
    forUser: true,
    category:CATEGORY_IMAGES,
    tooltip: makeDisplayTooltip("Image Gallery","images.png"),                    
});

addGlobalDisplayType({
    type: DISPLAY_IMAGEZOOM,
    label: "Image Zoom",
    requiresData: true,
    forUser: true,
    category:CATEGORY_IMAGES,
    tooltip: makeDisplayTooltip("Image Zoom","imagezoom.png","Show a set of images and allow for zooming in"),                        
});

addGlobalDisplayType({
    type: DISPLAY_SLIDES,
    label: "Slides",
    requiresData: true,
    forUser: true,
    category: CATEGORY_IMAGES,
    tooltip: makeDisplayTooltip("Show records in a slide like format","slides.png")
});


addGlobalDisplayType({
    type: DISPLAY_CARDS,
    label: "Cards",
    requiresData: true,
    forUser: true,
    category: CATEGORY_IMAGES,
    tooltip: makeDisplayTooltip("Group records hierachically showing images","cards.png"),                
});


function RamaddaCardsDisplay(displayManager, id, properties) {
    const ID_RESULTS = "results";
    const SUPER =  new RamaddaFieldsDisplay(displayManager, id, DISPLAY_CARDS, properties);
    Utils.importJS(ramaddaBaseUrl + "/lib/color-thief.umd.js",
		   () => {},
		   (jqxhr, settings, exception) => {
		       console.log("err");
		   });
  
    let myProps = [
	{label:'Cards Attributes'},
	{p:'groupByFields',ex:''},
	{p:'initGroupFields',ex:''},
	{p:'tooltipFields',ex:''},
	{p:'captionFields'},
	{p:'captionTemplate',ex:'${name}'},
	{p:'sortFields',ex:''},
	{p:'labelField',ex:''},
	{p:'imageWidth',ex:'100'},
	{p:'imageMargin',ex:'5'},
    ];
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
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
		    this.groupByHtml +=  HU.span([CLASS,"ramadda-button",ID,this.domId("docolors")], "Do colors")+" " +
		    HU.span([CLASS,"ramadda-button",ID,this.domId("docolorsreset")], "Reset");
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
			this.groupByHtml+= HU.select("",[ID,this.domId(ID_GROUPBY_FIELDS+i)],options,selected)+"&nbsp;";
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



            contents += HU.div([ID,this.domId(ID_RESULTS)]);
            this.setContents(contents);
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
		var img = document.querySelector('#' + this.domId("gallery")+"img" + cnt);
		var div = $('#' + this.domId("gallery")+"div" + cnt);
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
                            records.map(record=>{
                                var v =field.getValue(record);
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
		let record = records[rowIdx];
                var row = this.getDataValues(records[rowIdx]);
                var contents = "";
                var tooltip = "";
                this.tooltipFields.map(field=>{
                    if(tooltip!="") tooltip+="&#10;";
                    tooltip+=field.getValue(record);
                });
		tooltip =tooltip.replace(/\"/g,"&quot;");
                var label = "";
                var caption="";
                if(this.captionFields.length>0) {
                    if(this.captionTemplate) caption  = this.captionTemplate;
                    this.captionFields.map(field=>{
			var value = (""+field.getValue(record)).replace(/\"/g,"&quot;");
                        if(this.captionTemplate)
                            caption = caption.replace("\${" + field.getId()+"}",value);
                        else
                            caption+=value+"<br>";
                    });
                    if(this.urlField) {
                        var url = this.urlField.getValue(record);
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
                
                var  imgAttrs= [CLASS,"display-cards-popup","data-fancybox",this.domId("gallery"),"data-caption",caption];
		if(img) img = img.trim();
                if(Utils.stringDefined(img)) {
		    if(this.colorAnalysisEnabled)
			img = ramaddaBaseUrl+"/proxy?url=" + img;
                    img =  HU.href(img, HU.div([ID,this.domId("gallery")+"div" + imgCnt], HU.image(img,["width",width,ID,this.domId("gallery")+"img" + imgCnt])),imgAttrs)+label;
		    imgCnt++;
                    html = HU.div([CLASS,"display-cards-item", TITLE, tooltip, STYLE,HU.css('margin', margin+'px')], img);
                } else {
                    var style = "";
                    if(fontSize) {
                        style+= " font-size:" + fontSize +"; ";
                    }
                    if(this.colorByField && this.colorList) {
                        var value = this.colorByField.getValue(record);
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
                        html = HU.div(attrs,this.altLabelField.getValue(record));
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
    let myProps = [
	{label:'Image Gallery Properties'},
	{p:'imageField',ex:''},
	{p:'labelFields',ex:''},
	{p:'topLabelTemplate',ex:''},	
	{p:'bottomLabelTemplate',ex:''},	
	{p:'tooltipFields',ex:''},
	{p:'numberOfImages',ex:'100'},	
	{p:'includeBlanks',ex:'true'},
	{p:'blockWidth',ex:'150'},
	{p:'imageWidth',ex:'150'},
	{p:'imageHeight',ex:'150'},	
	{p:'imageMargin',ex:'10px'},
	{p:'decorate',ex:'false'},
	{p:'doPopup',ex:'false'},
	{p:'imageStyle',ex:''},			
	{p:'minHeightGallery',ex:150},
	{p:'maxHeightGallery',ex:150},	
	{p:'columns',ex:'5'},
	{p:'noun',ex:'images'},
    ];

    const SUPER =  new RamaddaFieldsDisplay(displayManager, id, DISPLAY_IMAGES, properties);
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
	startIndex:0,
	dataFilterChanged: function() {
	    this.startIndex=0;
	    this.updateUI();
	},
        handleEventRecordSelection: function(source, args) {
	    let blocks = this.find(".display-images-block");
	    let select = HU.attrSelect(RECORD_ID, args.record.getId());
	    let block = this.find(select);
	    blocks.css('border',null);
	    block.css('border',"1px solid " +this.getHighlightColor());
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
	    let urlField = this.getFieldById(null, this.getProperty("urlField"));
	    let tooltipClick = this.getProperty("tooltipClick");
            let pointData = this.getData();
            let fields = pointData.getRecordFields();

            let labelFields = this.getFieldsByIds(null, this.getProperty("labelFields", null, true));
            let topLabelTemplate = this.getPropertyTopLabelTemplate();
            let bottomLabelTemplate = this.getPropertyBottomLabelTemplate();	    
            let tooltipFields = this.getFieldsByIds(null, this.getProperty("tooltipFields", null, true));
	    if(!imageField) {
		this.setDisplayMessage("No image field in data");
		return;
	    }
	    let decorate = this.getPropertyDecorate(true);
	    let columns = +this.getPropertyColumns(0);
	    let number = +this.getPropertyNumberOfImages(50);
	    let colorBy = this.getColorByInfo(records);
	    let width = this.getPropertyImageWidth();
	    let blockWidth = this.getBlockWidth("200px");	    
	    let height = this.getPropertyImageHeight();	    
	    if(!width && !height) width="100%";
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
	    this.idToRecord = {};
	    let baseStyle = "";
	    if(!decorate) {
		class2 = "";
		class1 = "display-images-block";
		baseStyle = HU.css("margin",this.getPropertyImageMargin("10px"));
	    }
	    if(columns) {
		if(width && width.endsWith("%"))
		    baseStyle+=HU.css(WIDTH,width);
	    }
	    baseStyle+=this.getProperty("blockStyle","");
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
		this.idToRecord[record.getId()] = record;
		let topLabel = null;
		if(topLabelTemplate) {
		    topLabel = this.getRecordHtml(record,fields,topLabelTemplate);
		}
		let label = "";
		let galleryLabel = "";
		if(Utils.stringDefined(bottomLabelTemplate)) {
		    label = this.getRecordHtml(record,fields,bottomLabelTemplate);
		} 
		labelFields.forEach(l=>{
		    let value  = record.getValue(l.getIndex());
		    if(value.getTime) {
			value = this.formatDate(value);
		    } 
		    galleryLabel += " " + value; 
		});
		if(galleryLabel=="") galleryLabel=label;
		else if(label=="") label = galleryLabel;		
		let tt = "";
		tooltipFields.forEach(l=>{tt += "\n" + l.getLabel()+": " + row[l.getIndex()]});
		tt = tt.trim();
		let style = baseStyle;
		let imgAttrs = [STYLE,imageStyle,"alt",galleryLabel,ID,base+"image" + rowIdx,"loading","lazy"];
		if(width) imgAttrs.push(WIDTH,width);
		else if(height) imgAttrs.push(HEIGHT,height);		
		let img = image==""?SPACE1:HU.image(image,imgAttrs);
		let topLbl = (topLabel!=null?HU.div([CLASS,"ramadda-clickable display-images-toplabel"], topLabel):"");
		let lbl = HU.div([CLASS,"ramadda-clickable display-images-label"], label.trim());
		if(urlField) {
		    if(topLbl!="")
			topLbl = HU.href(urlField.getValue(record), topLbl,["target","_target"]);
		    lbl = HU.href(urlField.getValue(record), lbl,["target","_target"]);
		    galleryLabel = HU.href(urlField.getValue(record), galleryLabel,["target","_target"]);
		    galleryLabel = galleryLabel.replace(/"/g,"'");
		}
		if(!this.getProperty("showBottomLabel",true))
		    lbl="";
		if(colorBy.isEnabled()) {
		    let c = colorBy.getColorFromRecord(record);
		    style+=HU.css(BACKGROUND,c);
		}

		style+=HU.css("vertical-align","top","width",blockWidth);
		if(doPopup) {
		    img = HU.href(image,img,[CLASS,"popup_image","data-fancybox",base,"data-caption",galleryLabel]);
		} else if(urlField&& !tooltipClick) {
		    img = HU.href(urlField.getValue(record),img,["target","_target"]);
		}
		let block = 
		    HU.div([STYLE, style, RECORD_ID,record.getId(),RECORD_INDEX,recordIndex++,ID,base+"div"+  rowIdx, CLASS, class1,TITLE,tt],
			   HU.div([CLASS,class2], topLbl + img + lbl));
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
	    } else {
	    }

	    let header = "";
	    if(this.startIndex>0) {
		header += HU.span([ID,this.domId(ID_PREV)],"Previous")+" ";
	    }
	    if(this.startIndex+number<records.length) {
		header += HU.span([ID,this.domId(ID_NEXT)],"Next") +" ";
	    }
	    cnt--;
	    if(number<records.length) {
		header += "Showing " + (this.startIndex+1) +" - " +(this.startIndex+cnt);
		header += " of " + records.length +" " + this.getNoun("images");
	    }

	    if(header!="") {
		header = HU.div([STYLE,HU.css('margin-right','10px', "display","inline-block")],header);
		this.jq(ID_HEADER2_PREFIX).html(header);
		this.jq(ID_HEADER2).css("text-align","left");
	    }
	    header ="";

	    if(this.getPropertyMinHeightGallery() || this.getPropertyMaxHeightGallery()) {
		let css = "";
		if(this.getPropertyMinHeightGallery()) css+=HU.css("min-height",HU.getDimension(this.getPropertyMinHeightGallery()));
		if(this.getPropertyMinHeightGallery())	css+= HU.css("max-height",HU.getDimension(this.getPropertyMaxHeightGallery()));
		contents = HU.div([ID,this.domId(ID_IMAGES),STYLE,css+HU.css("overflow-y","auto")], contents);
	    }

	    contents  = HU.div([CLASS,"ramadda-grid"],contents);
            this.setContents(header + contents);
	    let blocks = this.find(".display-images-block");
	    let _this = this;
	    blocks.mouseenter(function() {
		$(this).attr("oldborder",$(this).css("border"));
		$(this).css("border","1px solid " + _this.getHighlightColor());
	    });
	    blocks.mouseleave(function() {
		$(this).css("border",$(this).attr("oldborder"));
	    });			      


	    this.makeTooltips(blocks,displayedRecords);
	    if(!doPopup) {
		let _this = this;
		if(!tooltipClick) {
		    blocks.click(function() {
			let record = _this.idToRecord[$(this).attr(RECORD_ID)];
			if(record) {
			    _this.propagateEventRecordSelection({record: record});
			}
		    });
		}
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
		this.getDisplayManager().notifyEvent(DisplayEvent.recordList, this, {
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
    let myProps = [
	{label:"Image Zoom Attributes"},
	{p:'labelFields'},
	{p:'thumbField'},
	{p:'thumbWidth',ex:'100'},
	{p:'imageWidth',ex:'150'},
	{p:'urlField'},
	{p:'popupWidth'},
	{p:'popupHeight'},	
	{p:"popupImageWidth",d:2000}
    ];


    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
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
		this.setDisplayMessage("No image field in data");
		return;
	    }
	    this.labelFields = this.getFieldsByIds(fields, this.getPropertyLabelFields());
            let thumbField = this.getFieldById(fields, this.getProperty("thumbField", "thumb")) || this.imageField;
	    let thumbWidth = parseFloat(this.getProperty("thumbWidth",100));
	    let height=this.getHeightForStyle();
	    let imageWidth = this.getProperty("imageWidth",500);
	    this.popupWidth =  +this.getProperty("popupWidth",imageWidth);
	    this.popupHeight = +this.getProperty("popupHeight",300);

	    let rect = HU.div([STYLE,HU.css("border","1px solid " +this.getHighlightColor(),"width","20px","height","20px","left","10px","top","10px","display","none","position","absolute","z-index",1000,"pointer-events","none"),ID, this.domId(ID_RECT)]);
	    let imageDiv = HU.div(["style","position:relative"],
				  rect+
				  HU.div([ID,this.domId(ID_IMAGE),STYLE,HU.css("position","relative") ]) +
				  HU.div([ID,this.domId(ID_POPUP),CLASS,"display-imagezoom-popup",STYLE,HU.css("z-index","100","display","none",WIDTH,this.popupWidth+"px",HEIGHT,this.popupHeight+"px","overflow-y","hidden","overflow-x","hidden", "position","absolute","top","0px","left", imageWidth+"px")],""));

	    let contents = HU.table(["border",0,WIDTH,"100%"],
				    HU.tr(["valign","top"],
					  HU.td([WIDTH,"2%"],
						HU.div([ID,this.domId(ID_THUMBS), STYLE,HU.css("max-height",height,"overflow-y","auto","display","inline-block")],"")) +
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
		thumbsHtml += HU.image(thumb,[RECORD_INDEX,rowIdx,ID,this.domId(ID_THUMB)+rowIdx,WIDTH, thumbWidth,CLASS,"display-imagezoom-thumb"])+"<br>\n";
	    }
            this.setContents(contents);
	    this.jq(ID_THUMBS).html(thumbsHtml);
	    let _this = this;
	    let thumbs = this.jq(ID_THUMBS).find(".display-imagezoom-thumb");
	    let thumbSelect = (thumb=>{
		thumbs.css("border","1px solid transparent");
		thumb.css("border","1px solid " + this.getHighlightColor());
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
		let width = +this.getPopupImageWidth();
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
	    let imageAttrs = [ID,this.domId(ID_POPUPIMAGE),STYLE,HU.css("xposition","absolute")];
	    if(this.getPopupImageWidth()) {
		imageAttrs.push(WIDTH);
		imageAttrs.push(this.getPopupImageWidth());
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
                    var url = this.urlField.getValue(record);
                    if(url && url!="") {
                        label = "<a style='color:inherit;'  href='" +url+"' target=_other>" +label+ "</a>";

                    }
		}
	    }
	    let html =  HU.image(image,["x","+:zoom in/-:zoom out",STYLE,HU.css("z-index",1000),WIDTH, width,ID,this.domId(ID_IMAGEINNER)]);
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
	    

	    //This causes problems
//	    HU.addToDocumentUrl("imagezoom_x",params.x);
//	    HU.addToDocumentUrl("imagezoom_y",params.y);	    

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
    let myProps = [
	{label:'Slides Attributes'},
	{p:'template',ex:''}
    ];
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
	slideIndex:0,

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
	    var left = HU.div([ID, this.domId(ID_PREV), STYLE,HU.css('font-size','200%'),CLASS,"display-slides-arrow-left fas fa-angle-left"]);
	    var right = HU.div([ID, this.domId(ID_NEXT), STYLE,HU.css('font-size','200%'), CLASS,"display-slides-arrow-right fas fa-angle-right"]);
	    var slide = HU.div([STYLE,HU.css('overflow-y','auto','max-height', height), ID, this.domId(ID_SLIDE), CLASS,"display-slides-slide"]);

	    var navStyle = "padding-top:20px;";
	    var contents = HU.div([STYLE,HU.css('position','relative')], "<table width=100%><tr valign=top><td width=20>" + HU.div([STYLE,navStyle], left) + "</td><td>" +
					 slide + "</td>" +
					 "<td width=20>" + HU.div([STYLE,navStyle],right) + "</td></tr></table>");
	    this.setContents(contents);
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
	    var html = this.applyRecordTemplate(record, row,this.fields,this.getProperty("template",""));
	    html = html.replace(/\${recordIndex}/g,(this.slideIndex+1));
	    this.jq(ID_SLIDE).html(html);
	    var args = {highlight:true,record: record};
	    if(propagateEvent)
		this.getDisplayManager().notifyEvent(DisplayEvent.recordHighlight, this, args);
	},
        handleEventRecordHighlight: function(source, args) {
	}
    })}




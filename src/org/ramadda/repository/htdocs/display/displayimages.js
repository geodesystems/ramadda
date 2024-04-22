/*
  Copyright 2008-2024 Geode Systems LLC
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
    Utils.importJS(RamaddaUtil.getCdnUrl("/lib/color-thief.umd.js"),
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
	    HU.createFancyBox( this.jq(ID_RESULTS).find("a.display-cards-popup"), {
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
    const ID_IMAGES = "images";
    if(!Utils.isDefined(properties["showRecordPager"])) {
	properties["showRecordPager"] = true;
    }
    if(!Utils.isDefined(properties["noun"])) {
	properties["noun"] = "images";
    }    
    if(Utils.isDefined(properties["numberOfImages"])) {
	properties["recordPagerNumber"] = properties["numberOfImages"];
    }


    let myProps = [
	{label:'Image Gallery Properties'},
	{p:'imageField',ex:''},
	{p:'urlPrefix'},
	{p:'template',ex:''},
	{p:'labelFields',ex:''},
	{p:'topLabelTemplate',ex:''},	
	{p:'bottomLabelTemplate',ex:''},	
	{p:'tooltipFields',ex:''},
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
    ];

    const SUPER =  new RamaddaFieldsDisplay(displayManager, id, DISPLAY_IMAGES, properties);
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
	dataFilterChanged: function() {
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
	    let includeBlanks  = this.getPropertyIncludeBlanks(false);
	    let imageField = null;
	    let showBottomLabel = this.getProperty("showBottomLabel",true);
            let records = this.filterData(null,null,{recordOk:record=>{
		if(imageField == null) imageField = this.getFieldById(null, this.getProperty("imageField"));
		if(!imageField) {
		    imageField = this.getFieldByType(null,"image");
		}
		if(!imageField) {
		    return false;
		}

		return true;
		/*** TODO?
		let image = record.getValue(imageField.getIndex());
		if(!Utils.stringDefined(image) && !includeBlanks) {
		    return false;
		}
		return true;
		*/
	    }});
            if(!records) return;

	    if(!imageField) {
		this.setDisplayMessage("No image field in data");
		return false;
	    }

	    let urlPrefix = this.getUrlPrefix();
	    let urlField = this.getFieldById(null, this.getProperty("urlField"));
	    let tooltipClick = this.getProperty("tooltipClick");
            let pointData = this.getData();
            let fields = pointData.getRecordFields();

            let labelFields = this.getFieldsByIds(null, this.getProperty("labelFields", null, true));
            let template = this.getTemplate();
            let topLabelTemplate = this.getTopLabelTemplate();
            let bottomLabelTemplate = this.getPropertyBottomLabelTemplate();	    
            let tooltipFields = this.getFieldsByIds(null, this.getProperty("tooltipFields", null, true));



	    let decorate = this.getPropertyDecorate(true);
	    let columns = +this.getPropertyColumns(0);
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
	    let class3 = "display-images-image-wrapper";
	    this.idToRecord = {};
	    let baseStyle = "";
	    if(records.length<10) {
		if(records.length<2) {
		    width='500px';
		    blockWidth='510px';
		} else {
		    width='300px';
		    blockWidth='310px';
		}
	    }


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
	    let cnt = 1;

	    let blankImage =this.getProperty('showPlaceholderImage',true)?HU.image(ramaddaBaseUrl+'/images/placeholder.png',[ATTR_WIDTH,'100%']):
		HU.space(1);
	    
	    records.forEach((record,rowIdx)=>{

                let row = this.getDataValues(record);
		let image = record.getValue(imageField.getIndex());
		if(urlPrefix) image = urlPrefix+image;
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
		let imgAttrs = [ATTR_STYLE,imageStyle,"alt",galleryLabel,ATTR_ID,base+"image" + rowIdx,"loading","lazy"];
		if(width) imgAttrs.push(WIDTH,width);
		else if(height) imgAttrs.push(HEIGHT,height);		
		let img = (!Utils.stringDefined(image))?blankImage:HU.div([ATTR_CLASS,class3],HU.image(image,imgAttrs));
		let topLbl = (topLabel!=null?HU.div([CLASS,"ramadda-clickable display-images-toplabel"], topLabel):"");
		let lbl = HU.div([CLASS,"ramadda-clickable display-images-label"], label.trim());
		if(urlField) {
		    if(topLbl!="")
			topLbl = HU.href(urlField.getValue(record), topLbl,["target","_target"]);
		    lbl = HU.href(urlField.getValue(record), lbl,["target","_target"]);
		    galleryLabel = HU.href(urlField.getValue(record), galleryLabel,["target","_target"]);
		    galleryLabel = galleryLabel.replace(/"/g,"'");
		}
		if(!showBottomLabel)
		    lbl="";
		if(colorBy.isEnabled()) {
		    let c = colorBy.getColorFromRecord(record);
		    style+=HU.css(BACKGROUND,c);
		}

		let recordContents;
		let block;
		if(template) {
		    let row = this.getDataValues(record);
		    block = recordContents= this.applyRecordTemplate(record, row,fields,template);
		    style+=HU.css("text-align","left");
		} else {
		    style+=HU.css("vertical-align","top","width",blockWidth);
		    if(doPopup) {
			img = HU.href(image,img,[CLASS,"popup_image","data-fancybox",base,"data-caption",galleryLabel]);
		    } else if(urlField&& !tooltipClick) {
			img = HU.href(urlField.getValue(record),img,["target","_target"]);
		    }
		    recordContents = HU.div([CLASS,class2], topLbl + img + lbl);
		}


		block = 
		    HU.div([STYLE, style, RECORD_ID,record.getId(),RECORD_INDEX,recordIndex++,ID,base+"div"+  rowIdx, CLASS, class1,TITLE,tt],
			   recordContents);
		if(columns) {
		    if(++columnCnt>=columns) {
			columnCnt=0;
		    }
		    if(!columnMap[columnCnt]) columnMap[columnCnt] = "";
		    columnMap[columnCnt] += block;
		} else {
		    contents += block;
		}
	    });
	    if(columns) {
		contents = "<table border=0 width=100%><tr valign=top>";
		for(let col=0;true;col++) {
		    if(!columnMap[col]) break;
		    contents+=HU.td(['align','center'],columnMap[col]);
		}
		contents+="</tr></table>";
	    } else {
	    }

	    if(this.getPropertyMinHeightGallery() || this.getPropertyMaxHeightGallery()) {
		let css = "";
		if(this.getPropertyMinHeightGallery()) css+=HU.css("min-height",HU.getDimension(this.getPropertyMinHeightGallery()));
		if(this.getPropertyMinHeightGallery())	css+= HU.css("max-height",HU.getDimension(this.getPropertyMaxHeightGallery()));
		contents = HU.div([ID,this.domId(ID_IMAGES),STYLE,css+HU.css("overflow-y","auto")], contents);
	    }

	    contents  = HU.div([CLASS,"ramadda-grid"],contents);
            this.setContents(contents);
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
	    } else {
		HU.createFancyBox( this.jq(ID_RESULTS).find("a.popup_image"), {
                    caption : function( instance, item ) {
			return  $(this).data('data-caption') || '';
                    }});
		
	    }

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
	{p:'labelFields',d:'name'},
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
            this.urlField = this.getFieldById(fields, this.getUrlField("url"));
	    if(!this.urlField) this.urlField = this.getFieldById(fields, 'entry_url');
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
    const ID_STRIP = "strip";
    //If we are showing the strip then make sure there is a width set
    if(properties.imageField && properties.showStrip && !Utils.isDefined(properties.width)) {
	//properties.width="100%";
    }
    
    const SUPER =  new RamaddaFieldsDisplay(displayManager, id, DISPLAY_SLIDES, properties);
    let myProps = [
	{label:'Slides Attributes'},
	{p:'template',ex:''},
	{p:'mediaField',ex:'',tt:'Field that contains a URL to an image, youtube, etc'},
	{p:'showStrip',ex:'true',tt:'Show the navigation strip'},
	{p:'slideWidth',ex:'100px'},
	{p:'thumbnailField',ex:''},
	{p:'thumbnailWidth',ex:'100px'},	
	{p:'urlField',ex:''},
	{p:'labelField',ex:''},
	{p:'labelTemplate',ex:'${name} ...'},
	{p:'topLabelTemplate',ex:'${name} ...'},		
	{p:'tooltipFields',ex:''},	
    ];
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
	slideIndex:0,
	handleEventRecordSelection: function(source, args) {
	    if(!this.records) return;
	    let index = this.findRecordIndex(this.records,args.record);
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
	    this.theTemplate = this.getProperty('template','');
//	    this.fields.forEach(f=>{console.log(f.getId());});
	    this.urlField = this.getFieldById(null, this.getProperty("urlField"));
	    this.labelField = this.getFieldById(null, this.getLabelField());
	    this.topLabelTemplate =  this.getTopLabelTemplate();
	    this.labelTemplate =  this.getLabelTemplate();
	    this.tooltipFields = this.getFieldsByIds(null, this.getProperty("tooltipFields"));	    	    
	    this.mediaField = this.getFieldById(null, this.getProperty("mediaField",this.getProperty("imageField")));
	    this.thumbnailField = this.getFieldById(null, this.getProperty("thumbnailField")) || this.mediaField;
            let height = this.getHeightForStyle('400');
	    let left = HU.div([ID, this.domId(ID_PREV), STYLE,HU.css('padding-right','10px','font-size','200%'),CLASS,'ramadda-clickable display-slides-arrow-left fas fa-angle-left']);
	    let right = HU.div([ID, this.domId(ID_NEXT), STYLE,HU.css('padding-left','10px','font-size','200%'), CLASS,'ramadda-clickable  display-slides-arrow-right fas fa-angle-right']);
	    let slide = HU.div([ATTR_CLASS,'display-slides-slide',
				ATTR_STYLE,HU.css('overflow-y','auto','max-height', height), ID, this.domId(ID_SLIDE), CLASS,'display-slides-slide']);

	    let top = "";
	    this.showStrip = this.thumbnailField && this.getProperty("showStrip");
	    if(this.showStrip) {
		let stripStyle = HU.css('overflow-x','auto','max-width','100%') +this.getProperty('stripStyle','');
		top = HU.div([ID,this.domId(ID_STRIP),CLASS,'display-slides-strip','tabindex','0','style',stripStyle]);
	    }
	    let contents = top+HU.div([ATTR_STYLE,HU.css('position','relative')],
				      slide + left + right);

	    this.setContents(contents);

	    if(this.showStrip) {
		let width = HU.getDimension(this.getThumbnailWidth("100px"));
		let strip="";
		this.records.forEach((record,idx)=>{
		    let url = this.thumbnailField.getValue(record);
		    //The null in the thumbnail file gets turned into a NaN
		    if((""+url)=="NaN") url = null;
		    if(!Utils.stringDefined(url)) {
			if(this.mediaField) 
			    url = this.mediaField.getValue(record);
		    }
		    if(!Utils.stringDefined(url)) {return;}
		    let clazz = 'display-slides-strip-image';
		    if(idx==0) clazz+=' display-slides-strip-image-selected';
		    let tt = HU.makeMultiline(this.tooltipFields.map(f=>{
			return  f.getValue(record);
		    }));


		    if(Utils.isImage(url)) {
			strip += HU.div([],HU.image(url,['loading','lazy','title',tt,'width',width,'class',clazz,RECORD_INDEX,idx]));
		    } else {
			let label = "";
			if(this.labelField) {
			    label = this.labelField.getValue(record);
			} else {
			    label = "Record:" + idx;
			    let tail = Utils.getFileTail(url);
			    if(tail) label+="<br>" + tail;
			}
			if(tt=="") tt = label;
			if(url.match(/youtube.com\/watch/)) {
			    label = HU.image(ramaddaBaseUrl +"/media/youtube.png") +" " + label;
			}
			tt = tt.replace(/<br>/g,HtmlUtils.BR_ENTITY);
			strip += HU.div(['title',tt,'style',HU.css('display','inline-block','min-width',width,'width',width,'overflow-x','hidden'),'class',clazz,RECORD_INDEX,idx],label);
		    }
		});
		let stripDom = this.jq(ID_STRIP);
		stripDom.html(strip);
		let _this = this;
		this.stripImages = stripDom.find('.display-slides-strip-image');
		stripDom.mouseenter(function(event) {
		    stripDom.focus();
		});
		stripDom.keydown(function(event) {
		    if(event.which==39) {
			_this.slideIndex++;
			_this.displaySlide(true);
		    } else if(event.which==37) {
			_this.slideIndex--;
			_this.displaySlide(true);
		    }
		});
		this.stripImages.click(function() {
		    _this.stripImages.removeClass('display-slides-strip-image-selected');
		    $(this).addClass('display-slides-strip-image-selected');
		    _this.slideIndex = $(this).attr(RECORD_INDEX);
		    _this.displaySlide(true,true);
		});
	    }


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
	displaySlide: function(propagateEvent,fromStrip) {
	    let _this = this;
	    let slideWidth = this.getSlideWidth('100%');
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
	    if(!fromStrip && this.showStrip) {
		this.stripImages.removeClass('display-slides-strip-image-selected');
		this.stripImages.find(HtmlUtils.attrSelect(RECORD_INDEX,this.slideIndex)).addClass('display-slides-strip-image-selected');
		this.stripImages.each(function() {
		    if(+$(this).attr(RECORD_INDEX) == _this.slideIndex) {
			$(this).addClass('display-slides-strip-image-selected');
			$(this).scrollintoview({
			    direction:'x'
			});
		    }
		});
	    }

	    let record = this.records[this.slideIndex];
	    let row = this.getDataValues(record);
	    let html = "";
	    let mainLink="";
	    let mainUrl;
	    if(this.urlField) {
		mainUrl = this.urlField.getValue(record);
	    }

	    if(Utils.stringDefined(this.theTemplate)) {
		html = this.applyRecordTemplate(record, row,this.fields,this.theTemplate);
	    } else if(this.mediaField) {
		let url = this.mediaField.getValue(record);
		if(Utils.isImage(url)) {
		    html = HU.image(url,[STYLE,HU.css('width',slideWidth)]);
		} else if(url.match('.mp3')) {
		    html =HU.center( Utils.embedAudio(url));
		} else if(url.match('soundcloud')) {
		    html = HU.center("<iframe scrolling='no' src='https://w.soundcloud.com/player/?visual=true&url=" +
				     url +"&maxwidth=450' width='450' height='390' frameborder='no'></iframe>");
		} else {
		    if(url.match(/youtube.com\/watch/)||url.match(/youtu.be/)) {
			
			html = HU.center(Utils.embedYoutube(url));
		    } else {
			html = HU.center(HU.tag("iframe",['src',url,'width','640','height','351','frameborder','0',
							  'webkitallowfullscreen',true,'mozallowfullscreen','true','allowfullscreen','true']));
		    }
		}
		if(html&&mainUrl && !this.topLabelTemplate)
		    html = html+"<br>"+HU.href(mainUrl, "Link",['target','_link']);
		if(this.tooltipFields) {
		    let tt = HU.makeMultiline(this.tooltipFields.map(f=>{
			return  f.getValue(record);
		    }));
		    html = HU.div([TITLE,tt], html);
		}
		if(this.labelField) {
		    html = html+HU.div(['class','display-slides-label'], this.labelField.getValue(record));
		}
		if(this.topLabelTemplate) {
		    let label = this.applyRecordTemplate(record,this.getDataValues(record),null, this.topLabelTemplate);
		    if(mainUrl) label = HU.href(mainUrl,label);
		    html=HU.div(['class','display-slides-label'], label)+html;
		}
		if(this.labelTemplate) {
		    let label = this.applyRecordTemplate(record,this.getDataValues(record),null, this.labelTemplate);
		    html=html+HU.div(['class','display-slides-label'], label);
		}
			

		
	    }
	    html = html.replace(/\${recordIndex}/g,(this.slideIndex+1));
	    this.jq(ID_SLIDE).html(html);
	    let args = {highlight:true,record: record};
	    if(propagateEvent)
		this.getDisplayManager().notifyEvent(DisplayEvent.recordHighlight, this, args);
	},
        handleEventRecordHighlight: function(source, args) {
	}
    })}




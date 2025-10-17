/**
   Copyright (c) 2008-2025 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/


var DISPLAY_SLIDES = "slides";
var DISPLAY_IMAGES = "images";
var DISPLAY_IMAGEZOOM = "imagezoom";
var DISPLAY_CARDS = "cards";

var ID_GALLERY = "gallery";

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
		    this.groupByHtml +=  HU.span([ATTR_CLASS,CLASS_BUTTON,
						  ATTR_ID,this.domId("docolors")], "Do colors")+" " +
		    HU.span([ATTR_CLASS,CLASS_BUTTON,
			     ATTR_ID,this.domId("docolorsreset")], "Reset");
		if(this.groupByFields.length>0) {
		    var options = [["","--"]];
		    this.groupByFields.map(field=>{
			options.push([field.getId(),field.getLabel()]);
		    });

		    this.groupByHtml +=  HU.span([ATTR_CLASS,"display-fitlerby-label"], " Group by: ");
		    for(var i=0;i<this.groupByMenus;i++) {
			var selected = "";
			if(i<this.initGrouping.length) {
			    selected = this.initGrouping[i].getId();
			}
			this.groupByHtml+= HU.select("",[ATTR_ID,this.domId(ID_GROUPBY_FIELDS+i)],options,selected)+SPACE;
		    }
		    this.groupByHtml+=SPACE;
		    this.jq(ID_HEADER1).html(HU.div([ATTR_CLASS,"display-filterby"],this.groupByHtml));
		    this.jq("docolors").button().click(()=>{
			this.analyzeColors();
		    });
		    this.jq("docolorsreset").button().click(()=>{
			this.updateUI();
		    });


		}
	    }

            contents += HU.div([ATTR_ID,this.domId(ID_RESULTS)]);
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
		var img = document.querySelector('#' + this.domId(ID_GALLERY)+"img" + cnt);
		var div = jqid(this.domId(ID_GALLERY)+TAG_DIV + cnt);
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
		    html+=HU.div([ATTR_STYLE,HU.css(CSS_DISPLAY,DISPLAY_INLINE_BLOCK,
						    CSS_WIDTH, HU.px(width),
						    CSS_HEIGHT, HU.px(img.height),
						    CSS_BACKGROUND,HU.rgb(c[0],c[1],c[2]))],'');
		}
		div.css(CSS_WIDTH,img.width);
		div.css(CSS_HEIGHT,img.height);
		div.html(html);
		img.style.display = DISPLAY_NONE;
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
                            caption+=value+HU.br();
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
                
                var  imgAttrs= [ATTR_CLASS,"display-cards-popup",
				ATTR_DATA_FANCYBOX,this.domId(ID_GALLERY),
				ATTR_DATA_CAPTION,caption];
		if(img) img = img.trim();
                if(Utils.stringDefined(img)) {
		    if(this.colorAnalysisEnabled)
			img = RamaddaUtil.getUrl("/proxy?url=" + img);
                    img =  HU.href(img, HU.div([ATTR_ID,this.domId(ID_GALLERY)+'div' + imgCnt],
					       HU.image(img,[ATTR_WIDTH,width,
							     ATTR_ID,this.domId(ID_GALLERY)+"img" + imgCnt])),imgAttrs)+label;
		    imgCnt++;
                    html = HU.div([ATTR_CLASS,"display-cards-item",
				   ATTR_TITLE, tooltip,
				   ATTR_STYLE,HU.css(CSS_MARGIN, HU.px(margin))], img);
                } else {
                    var style = "";
                    if(fontSize) {
                        style+= HU.css(CSS_FONT_SIZE,fontSize);
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
                        style+=HU.css(CSS_BACKGROUND,this.colorList[index]);
                        if(this.foregroundList) {
                            if(index<this.foregroundList.length) {
                                style+=HU.css(CSS_COLOR,HU.important(this.foregroundList[index]));
                            } else {
                                style+=HU.css(CSS_COLOR, HU.important(this.foregroundList[this.foregroundList-1]));
                            }
                        }
                    }
                    if(cardStyle)
                        style +=cardStyle;
                    var attrs = [ATTR_TITLE,tooltip,
				 ATTR_CLASS,"ramadda-gridbox display-cards-card",
				 ATTR_STYLE,style];
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
            let topHtml = HU.div([ATTR_CLASS,"display-cards-header"],"Total" +" (" + total+")");
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
                var width = group.members.length==0?HU.perc(100):100/group.members.length;
                html +=HU.open(TAG_TABLE,[ATTR_WIDTH,HU.perc(100),
					  ATTR_BORDER,0]) +
		    HU.open(TAG_TR,[ATTR_VALIGN,ALIGN_TOP]);
                for(var i=0;i<group.members.length;i++) {
                    var child = group.members[i];
		    var prefix="";
		    if(child.field)
			prefix = child.field.getLabel()+": ";
                    html+=HU.open(TAG_TD,[ATTR_WIDTH, HU.perc(width)]);
		    let perc = Math.round(100*child.getCount()/topGroup.getCount());
		    html+=HU.div([ATTR_CLASS,'display-cards-header'],
				 prefix+child.id +' (#' + child.getCount()+' - ' + HU.perc(perc) +')');
		    html+= this.makeGroupHtml(child, topGroup);
                    html+=HU.close(TAG_TD);
                }
                html +=HU.close(TAG_TR, TAG_TABLE);
            } else {
                html+=Utils.join(group.members,'');
            }
            return html;
        }
    });
}




function RamaddaImagesDisplay(displayManager, id, properties) {
    const ID_IMAGES = "images";
    //never want to do this
    properties.tooltipClick=null;
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
	{p:'includeNonImages',d:true},
	{p:'showPlaceholderImage',d:true},
    ];

    const SUPER =  new RamaddaFieldsDisplay(displayManager, id, DISPLAY_IMAGES, properties);
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
	dataFilterChanged: function() {
	    this.updateUI();
	},
        handleEventRecordSelection: function(source, args) {
	    let blocks = this.find(HU.dotClass('display-images-block'));
	    let select = HU.attrSelect(ATTR_RECORD_ID, args.record.getId());
	    let block = this.find(select);
	    blocks.css(CSS_BORDER,null);
	    block.css(CSS_BORDER,HU.border(1,this.getHighlightColor()));
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
	    let blockWidth = this.getBlockWidth(HU.px(200));	    
	    let height = this.getPropertyImageHeight();	    
	    if(!width && !height) width=HU.perc(100);
	    let imageStyle = this.getPropertyImageStyle("");
	    let contents = "";
	    let uid = HU.getUniqueId();
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
		baseStyle = HU.css(CSS_MARGIN,this.getPropertyImageMargin(HU.px(10)));
	    }
	    if(columns) {
		if(width && width.endsWith("%"))
		    baseStyle+=HU.css(ATTR_WIDTH,width);
	    }
	    baseStyle+=this.getProperty("blockStyle","");
	    let cnt = 1;

	    let includeNonImages = this.getIncludeNonImages();
	    let blankImage =this.getShowPlaceholderImage(true)?HU.image(RamaddaUtil.getCdnUrl('/images/placeholder.png'),
									[ATTR_WIDTH,HU.perc(100)]):
		HU.space(1);
	    
	    let anyNoImages= false;
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
		let imgAttrs = [ATTR_STYLE,imageStyle,
				ATTR_TITLE,galleryLabel,
				ATTR_ID,base+"image" + rowIdx,
				ATTR_LOADING,"lazy"];
		if(width) imgAttrs.push(ATTR_WIDTH,width);
		else if(height) imgAttrs.push(ATTR_HEIGHT,height);		
		if(!Utils.stringDefined(image) &&!includeNonImages) return;

		let hasImage = Utils.stringDefined(image);
		if(!hasImage) {
		    anyNoImages=true;
		    if(this.hideNoImages) return;
		}
		let img = !hasImage?blankImage:HU.div([ATTR_CLASS,class3],HU.image(image,imgAttrs));
		let topLbl = (topLabel!=null?HU.div([ATTR_CLASS,HU.classes(CLASS_CLICKABLE,'display-images-toplabel')], topLabel):"");
		let lbl = HU.div([ATTR_CLASS,HU.classes(CLASS_CLICKABLE,'display-images-label')], label.trim());
		if(urlField) {
		    if(topLbl!="")
			topLbl = HU.href(urlField.getValue(record), topLbl,[ATTR_TARGET,"_target"]);
		    lbl = HU.href(urlField.getValue(record), lbl,[ATTR_TARGET,"_target"]);
		    galleryLabel = HU.href(urlField.getValue(record), galleryLabel,[ATTR_TARGET,"_target"]);
		    galleryLabel = galleryLabel.replace(/"/g,"'");
		}
		if(!showBottomLabel)
		    lbl="";
		if(colorBy.isEnabled()) {
		    let c = colorBy.getColorFromRecord(record);
		    style+=HU.css(ATTR_BACKGROUND,c);
		}

		let recordContents;
		let block;
		if(template) {
		    let row = this.getDataValues(record);
		    block = recordContents= this.applyRecordTemplate(record, row,fields,template);
		    style+=HU.css(CSS_TEXT_ALIGN,ALIGN_LEFT);
		} else {
		    style+=HU.css(CSS_VERTICAL_ALIGN,ALIGN_TOP,CSS_WIDTH,blockWidth);
		    if(doPopup) {
			img = HU.href(image,img,[ATTR_CLASS,"popup_image",
						 ATTR_DATA_FANCYBOX,base,
						 ATTR_DATA_CAPTION,galleryLabel]);
		    } else if(urlField&& !tooltipClick) {
			img = HU.href(urlField.getValue(record),img,[ATTR_TARGET,"_target"]);
		    }
		    recordContents = HU.div([ATTR_CLASS,class2], topLbl + img + lbl);
		}


		block = 
		    HU.div([ATTR_STYLE, style,
			    ATTR_RECORD_ID,record.getId(),
			    ATTR_RECORD_INDEX,recordIndex++,
			    ATTR_ID,base+'div'+  rowIdx,
			    ATTR_CLASS, class1,
			    ATTR_TITLE,tt],
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
		contents = HU.open(TAG_TABLE,[ATTR_BORDER,0,
					      ATTR_WIDTH,HU.perc(100)]) +
		    HU.open(TAG_TR,[ATTR_VALIGN,ALIGN_TOP]);
		for(let col=0;true;col++) {
		    if(!columnMap[col]) break;
		    contents+=HU.td([ATTR_ALIGN,ALIGN_CENTER],columnMap[col]);
		}
		contents+=HU.close(TAG_TR,TAG_TABLE);
	    } else {
	    }

	    if(this.getPropertyMinHeightGallery() || this.getPropertyMaxHeightGallery()) {
		let css = "";
		if(this.getPropertyMinHeightGallery())
		    css+=HU.css(CSS_MIN_HEIGHT,HU.getDimension(this.getPropertyMinHeightGallery()));
		if(this.getPropertyMinHeightGallery())
		    css+= HU.css(CSS_MAX_HEIGHT,HU.getDimension(this.getPropertyMaxHeightGallery()));
		contents = HU.div([ATTR_ID,this.domId(ID_IMAGES),
				   ATTR_STYLE,css+HU.css(CSS_OVERFLOW_Y,OVERFLOW_AUTO)],
				  contents);
	    }

	    contents  = HU.div([ATTR_CLASS,"ramadda-grid"],contents);
	    if(this.getShowPlaceholderImage() && anyNoImages) {
		contents = HU.div([ATTR_STYLE,HU.css(CSS_MARGIN_LEFT,HU.px(8),CSS_MARGIN_TOP,HU.px(8))],
				  HU.checkbox('',[ATTR_ID,this.domId('onlyimages')],
					      this.hideNoImages,'Show entries with images')) +
		    contents;
	    }

            this.setContents(contents);
	    if(anyNoImages) {
		this.jq('onlyimages').change(function() {
		    _this.hideNoImages = $(this).is(':checked');
		    _this.forceUpdateUI();
		});
	    }
	    let blocks = this.find(".display-images-block");
	    let _this = this;
	    blocks.mouseenter(function() {
		$(this).attr("oldborder",$(this).css(CSS_BORDER));
		$(this).css(CSS_BORDER,HU.border(1,_this.getHighlightColor()));
	    });
	    blocks.mouseleave(function() {
		$(this).css(CSS_BORDER,$(this).attr("oldborder"));
	    });			      


	    this.makeTooltips(blocks,displayedRecords);
	    if(!doPopup) {
		let _this = this;
		if(!tooltipClick) {
		    blocks.click(function() {
			let record = _this.idToRecord[$(this).attr(ATTR_RECORD_ID)];
			if(record) {
			    _this.propagateEventRecordSelection({record: record});
			}
		    });
		}
	    } else {
		HU.createFancyBox( this.jq(ID_RESULTS).find("a.popup_image"), {
                    caption : function( instance, item ) {
			return  $(this).data(ATTR_DATA_CAPTION) || '';
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

	    let rect = HU.div([ATTR_STYLE,HU.css(CSS_BORDER,HU.border(1,this.getHighlightColor()),
						 CSS_WIDTH,HU.px(20),
						 CSS_HEIGHT,HU.px(20),
						 CSS_LEFT,HU.px(10),
						 CSS_TOP,HU.px(10),
						 CSS_DISPLAY,DISPLAY_NONE,
						 CSS_POSITION,POSITION_ABSOLUTE,
						 CSS_Z_INDEX,1000,CSS_POINTER_EVENTS,"none"),
			       ATTR_ID, this.domId(ID_RECT)]);
	    let imageDiv = HU.div([ATTR_STYLE,HU.css(CSS_POSITION,POSITION_RELATIVE)],
				  rect+
				  HU.div([ATTR_ID,this.domId(ID_IMAGE),
					  ATTR_STYLE,HU.css(CSS_POSITION,POSITION_RELATIVE) ]) +
				  HU.div([ATTR_ID,this.domId(ID_POPUP),
					  ATTR_CLASS,"display-imagezoom-popup",
					  ATTR_STYLE,HU.css(CSS_Z_INDEX,"100",
							    CSS_DISPLAY,DISPLAY_NONE,
							    CSS_WIDTH,HU.px(this.popupWidth),
							    CSS_HEIGHT,HU.px(this.popupHeight),
							    CSS_OVERFLOW_Y,OVERFLOW_HIDDEN,
							    CSS_OVERFLOW_X,OVERFLOW_HIDDEN,
							    CSS_POSITION,POSITION_ABSOLUTE,
							    CSS_TOP,HU.px(0),
							    CSS_LEFT, HU.px(imageWidth))],""));

	    let contents = HU.table([ATTR_BORDER,0,
				     ATTR_WIDTH,HU.perc(100)],
				    HU.tr([ATTR_VALIGN,CSS_TOP],
					  HU.td([ATTR_WIDTH,HU.perc(2)],
						HU.div([ATTR_ID,this.domId(ID_THUMBS),
							ATTR_STYLE,HU.css(CSS_MAX_HEIGHT,height,
									  CSS_OVERFLOW_Y,OVERFLOW_AUTO,
									  CSS_DISPLAY,DISPLAY_INLINE_BLOCK)],"")) +
					  HU.td([ATTR_WIDTH,HU.perc(90)],
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
		thumbsHtml += HU.image(thumb,[ATTR_RECORD_INDEX,rowIdx,
					      ATTR_ID,this.domId(ID_THUMB)+rowIdx,
					      ATTR_WIDTH, thumbWidth,
					      ATTR_CLASS,"display-imagezoom-thumb"])+HU.br()+"\n";
	    }
            this.setContents(contents);
	    this.jq(ID_THUMBS).html(thumbsHtml);
	    let _this = this;
	    let thumbs = this.jq(ID_THUMBS).find(".display-imagezoom-thumb");
	    let thumbSelect = (thumb=>{
		thumbs.css(CSS_BORDER,HU.border(1,COLOR_TRANSPARENT));
		thumb.css(CSS_BORDER,HU.border(1,this.getHighlightColor()));
		let index = parseFloat(thumb.attr(ATTR_RECORD_INDEX));
		HU.addToDocumentUrl("imagezoom_thumb",index);
		let record = records[index]
		_this.handleImage(record);
		_this.propagateEventRecordSelection({record: record});
	    });

	    thumbs.mouseover(function() {	
		thumbSelect($(this));
	    });
	    this.jq(ID_THUMBS).css(CSS_BORDER,HU.border(1,COLOR_TRANSPARENT));
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
	    this.jq(ID_POPUP).css(CSS_DISPLAY,DISPLAY_BLOCK);
	    let imageAttrs = [ATTR_ID,this.domId(ID_POPUPIMAGE)];
	    if(this.getPopupImageWidth()) {
		imageAttrs.push(ATTR_WIDTH, this.getPopupImageWidth());
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
	    let html =  HU.image(image,["x","+:zoom in/-:zoom out",
					ATTR_STYLE,HU.css(CSS_Z_INDEX,1000),
					ATTR_WIDTH, width,
					ATTR_ID,this.domId(ID_IMAGEINNER)]);
	    if(label!="")
		html+=HU.div([ATTR_STYLE,HU.css(CSS_COLOR,COLOR_BLACK)],label);
	    this.jq(ID_IMAGE).html(html);

	    this.jq(ID_POPUP).html("");
	    this.jq(ID_POPUP).css(CSS_DISPLAY,DISPLAY_NONE);
	    this.jq(ID_IMAGEINNER).mouseenter(()=>{
		this.showPopup();
	    });
	    this.jq(ID_IMAGEINNER).mouseout(()=>{
		this.jq(ID_POPUP).html("");
		this.jq(ID_POPUP).css(CSS_DISPLAY,DISPLAY_NONE);
		this.jq(ID_RECT).css(CSS_DISPLAY,DISPLAY_NONE);		
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
	    rect.css({display:DISPLAY_BLOCK,top:HU.px(params.y-sh2),
		      left:HU.px(params.x-sw2),
		      width:HU.px(scaledWidth),
		      height:HU.px(scaledHeight)});
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
		style += HU.css(CSS_HEIGHT,height);
            }
            var width = this.getWidthForStyle();
            if (width) {
                style += HU.css(CSS_WIDTH,width);
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
	    let left = HU.div([ATTR_ID, this.domId(ID_PREV),
			       ATTR_STYLE,HU.css(CSS_PADDING_RIGHT,HU.px(10),CSS_FONT_SIZE,HU.perc(200)),
			       ATTR_CLASS,HU.classes(CLASS_CLICKABLE,'display-slides-arrow-left fas fa-angle-left')]);
	    let right = HU.div([ATTR_ID, this.domId(ID_NEXT),
				ATTR_STYLE,HU.css(CSS_PADDING_LEFT,HU.px(10),CSS_FONT_SIZE,HU.perc(200)),
				ATTR_CLASS,HU.classes(CLASS_CLICKABLE,'display-slides-arrow-right fas fa-angle-right')]);
	    let slide = HU.div([ATTR_CLASS,'display-slides-slide',
				ATTR_STYLE,HU.css(CSS_OVERFLOW_Y,OVERFLOW_AUTO,CSS_MAX_HEIGHT, height),
				ATTR_ID, this.domId(ID_SLIDE),
				ATTR_CLASS,'display-slides-slide']);

	    let top = "";
	    this.showStrip = this.thumbnailField && this.getProperty("showStrip");
	    if(this.showStrip) {
		let stripStyle = HU.css(CSS_OVERFLOW_X,OVERFLOW_AUTO,CSS_MAX_WIDTH,HU.perc(100)) +this.getProperty('stripStyle','');
		top = HU.div([ATTR_ID,this.domId(ID_STRIP),
			      ATTR_CLASS,'display-slides-strip',
			      ATTR_TABINDEX,'0',
			      ATTR_STYLE,stripStyle]);
	    }
	    let contents = top+HU.div([ATTR_STYLE,HU.css(CSS_POSITION,POSITION_RELATIVE)],
				      slide + left + right);

	    this.setContents(contents);

	    if(this.showStrip) {
		let width = HU.getDimension(this.getThumbnailWidth(HU.px(100)));
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
			strip += HU.div([],HU.image(url,[ATTR_LOADING,'lazy',
							 ATTR_TITLE,tt,
							 ATTR_WIDTH,width,
							 ATTR_CLASS,clazz,
							 ATTR_RECORD_INDEX,idx]));
		    } else {
			let label = "";
			if(this.labelField) {
			    label = this.labelField.getValue(record);
			} else {
			    label = "Record:" + idx;
			    let tail = Utils.getFileTail(url);
			    if(tail) label+=HU.br() + tail;
			}
			if(tt=="") tt = label;
			if(url.match(/youtube.com\/watch/)) {
			    label = HU.image(RamaddaUtil.getCdnUrl("/media/youtube.png")) +" " + label;
			}
			tt = tt.replace(/<br>/g,HU.BR_ENTITY);
			strip += HU.div([ATTR_TITLE,tt,
					 ATTR_STYLE,HU.css(CSS_DISPLAY,DISPLAY_INLINE_BLOCK,
							   CSS_MIN_WIDTH,width,
							   CSS_WIDTH,width,
							   CSS_OVERFLOW_X,OVERFLOW_HIDDEN),
					 ATTR_CLASS,clazz,
					 ATTR_RECORD_INDEX,idx],label);
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
		    _this.slideIndex = $(this).attr(ATTR_RECORD_INDEX);
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
	    let slideWidth = this.getSlideWidth(HU.perc(100));
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
		this.stripImages.find(HU.attrSelect(ATTR_RECORD_INDEX,this.slideIndex)).addClass('display-slides-strip-image-selected');
		this.stripImages.each(function() {
		    if(+$(this).attr(ATTR_RECORD_INDEX) == _this.slideIndex) {
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
		    html = HU.image(url,[ATTR_STYLE,HU.css(CSS_WIDTH,slideWidth)]);
		} else if(url.match('.mp3')) {
		    html =HU.center( Utils.embedAudio(url));
		} else if(url.match('soundcloud')) {
		    html = HU.center(HU.open(TAG_IFRAME,['scrolling','no',
							 ATTR_SRC,HU.url('https://w.soundcloud.com/player/','visual','true','url', url, 'maxwidth',450),
							 ATTR_WIDTH,450,ATTR_HEIGHT,390,'frameborder']));
		} else {
		    if(url.match(/youtube.com\/watch/)||url.match(/youtu.be/)) {
			
			html = HU.center(Utils.embedYoutube(url));
		    } else {
			html = HU.center(HU.tag(TAG_IFRAME,[ATTR_SRC,url,ATTR_WIDTH,640,ATTR_HEIGHT,351,
							    'frameborder',0,
							    'webkitallowfullscreen',true,
							    'mozallowfullscreen','true',
							    'allowfullscreen','true']));
		    }
		}
		if(html&&mainUrl && !this.topLabelTemplate)
		    html = html+HU.br()+HU.href(mainUrl, "Link",[ATTR_TARGET,'_link']);
		if(this.tooltipFields) {
		    let tt = HU.makeMultiline(this.tooltipFields.map(f=>{
			return  f.getValue(record);
		    }));
		    html = HU.div([ATTR_TITLE,tt], html);
		}
		if(this.labelField) {
		    html = html+HU.div([ATTR_CLASS,'display-slides-label'], this.labelField.getValue(record));
		}
		if(this.topLabelTemplate) {
		    let label = this.applyRecordTemplate(record,this.getDataValues(record),null, this.topLabelTemplate);
		    if(mainUrl) label = HU.href(mainUrl,label);
		    html=HU.div([ATTR_CLASS,'display-slides-label'], label)+html;
		}
		if(this.labelTemplate) {
		    let label = this.applyRecordTemplate(record,this.getDataValues(record),null, this.labelTemplate);
		    html=html+HU.div([ATTR_CLASS,'display-slides-label'], label);
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




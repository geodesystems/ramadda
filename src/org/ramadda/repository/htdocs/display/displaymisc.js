/*
  Copyright 2008-2023 Geode Systems LLC
*/


const DISPLAY_GRAPH = "graph";
const DISPLAY_TREE = "tree";
const DISPLAY_TIMELINE = "timeline";
const DISPLAY_HOURS = "hours";
const DISPLAY_BLANK = "blank";
const DISPLAY_FILTER = "filter";
const DISPLAY_HOOK = "hook";
const DISPLAY_PRE = "pre";
const DISPLAY_HTMLTABLE = "htmltable";
const DISPLAY_RECORDS = "records";
const DISPLAY_TSNE = "tsne";
const DISPLAY_HEATMAP = "heatmap";
const DISPLAY_WAFFLE = "waffle";
const DISPLAY_CROSSTAB = "crosstab";
const DISPLAY_CORRELATION = "correlation";
const DISPLAY_RANKING = "ranking";
const DISPLAY_STATS = "stats";
const DISPLAY_COOCCURENCE = "cooccurence";
const DISPLAY_BOXTABLE = "boxtable";
const DISPLAY_DATATABLE = "datatable";
const DISPLAY_PERCENTCHANGE = "percentchange";
const DISPLAY_SPARKLINE = "sparkline";
const DISPLAY_POINTIMAGE = "pointimage";
const DISPLAY_CANVAS = "canvas";
const DISPLAY_FIELDTABLE = "fieldtable";
const DISPLAY_SELECTEDRECORDS = "selectedrecords";
const DISPLAY_DATEGRID = "dategrid";
const DISPLAY_STRIPES = "stripes";

addGlobalDisplayType({
    type: DISPLAY_RANKING,
    label: "Ranking",
    requiresData: true,
    forUser: true,
    category: CATEGORY_TABLE,
    tooltip: makeDisplayTooltip("Show fields ordered by values","ranking.png")                            
});

addGlobalDisplayType({
    type: DISPLAY_STRIPES,
    label: "Stripes",
    requiresData: true,
    forUser: true,
    category: CATEGORY_MISC,
    tooltip: makeDisplayTooltip("Show color stripes","stripes.png")                            
});


addGlobalDisplayType({
    type: DISPLAY_CORRELATION,
    label: "Correlation",
    requiresData: true,
    forUser: true,
    category: CATEGORY_TABLE,
    tooltip: makeDisplayTooltip('Correlation','correlation.png')                            
});
addGlobalDisplayType({
    type: DISPLAY_CROSSTAB,
    label: "Crosstab",
    requiresData: true,
    forUser: true,
    category: CATEGORY_TABLE,
    tooltip: makeDisplayTooltip("Cross Tabulation","crosstab.png")                                
});

addGlobalDisplayType({
    type: DISPLAY_STATS,
    label: "Stats Table",
    requiresData: false,
    forUser: true,
    category: CATEGORY_TABLE,
    tooltip: makeDisplayTooltip("Statistical Summary","stats.png"),
});
addGlobalDisplayType({
    type: DISPLAY_RECORDS,
    label: "Records",
    requiresData: true,
    forUser: true,
    category: CATEGORY_TEXT,
    tooltip: makeDisplayTooltip("Displays records as text","records.png")
});
addGlobalDisplayType({
    type: DISPLAY_TSNE,
    label: "TSNE",
    requiresData: true,
    forUser: false,
    category: CATEGORY_MISC
});
addGlobalDisplayType({
    type: DISPLAY_HEATMAP,
    label: "Heatmap",
    requiresData: true,
    forUser: true,
    category: CATEGORY_MISC,
    tooltip: makeDisplayTooltip("Table showing colored fields","heatmap.png"),    
});
addGlobalDisplayType({
    type: DISPLAY_WAFFLE,
    label: "Waffle",
    requiresData: true,
    forUser: true,
    category: CATEGORY_MISC,
    tooltip: makeDisplayTooltip('Waffle Chart','waffle.png')
});
addGlobalDisplayType({
    type: DISPLAY_GRAPH,
    label: "Graph",
    requiresData: true,
    forUser: true,
    category: CATEGORY_MISC,
    tooltip: makeDisplayTooltip("Display a force-directed graph","graph.png")
});

addGlobalDisplayType({
    type: DISPLAY_PERCENTCHANGE,
    label: "Percent Change",
    requiresData: true,
    forUser: true,
    category: CATEGORY_TEXT,
    tooltip: makeDisplayTooltip("Percent Change","percentchange.png","Show percent change over a given time in a text template")    
});

addGlobalDisplayType({
    type: DISPLAY_SPARKLINE,
    label: "Sparkline",
    requiresData: true,
    forUser: true,
    category: CATEGORY_IMAGES,
    tooltip: makeDisplayTooltip("Embed little sparkline plots in text","sparkline.png"),    
});

addGlobalDisplayType({
    type: DISPLAY_CANVAS,
    label: "Canvas",
    requiresData: true,
    forUser: true,
    category: CATEGORY_IMAGES,
    tooltip: makeDisplayTooltip("Draw records into a canvas","canvas.png"),        
});

addGlobalDisplayType({
    type: DISPLAY_POINTIMAGE,
    label: "Point Image",
    requiresData: true,
    forUser: true,
    category: CATEGORY_IMAGES,
    tooltip: makeDisplayTooltip("Embed 2D images into text","pointimage.png"),            
});
addGlobalDisplayType({
    type: DISPLAY_FIELDTABLE,
    label: "Field Table",
    requiresData: true,
    forUser: true,
    category: CATEGORY_TABLE,
    tooltip: makeDisplayTooltip('Field Table',"fieldtable.png"),
});
addGlobalDisplayType({
    type: DISPLAY_SELECTEDRECORDS,
    label: "Selected Records",
    requiresData: true,
    forUser: true,
    category: CATEGORY_TABLE,
    tooltip: makeDisplayTooltip("Display selected records in a table")
});
addGlobalDisplayType({
    type: DISPLAY_TREE,
    forUser: true,
    label: "Tree",
    requiresData: false,
    category: CATEGORY_RADIAL_ETC,
    tooltip: makeDisplayTooltip('Tree','tree.png')                                    
});

addGlobalDisplayType({
    type: DISPLAY_TIMELINE,
    label: "Timeline",
    requiresData: true,
    forUser: true,
    category:  CATEGORY_MISC,
    tooltip: makeDisplayTooltip("Timeline showing text and images","timeline.png")
});
addGlobalDisplayType({
    type: DISPLAY_HOURS,
    label: "Hours",
    requiresData: true,
    forUser: true,
    category:  CATEGORY_MISC,
    tooltip: makeDisplayTooltip("Hourly timeline","timeline.png","Show data by the day and hour")    
});
addGlobalDisplayType({
    type: DISPLAY_BLANK,
    label: "Blank",
    requiresData: true,
    forUser: true,
    category: CATEGORY_CONTROLS,
    tooltip: makeDisplayTooltip("Shows no data",null,"Useful for just showing filters, etc")                                                
});
addGlobalDisplayType({
    type: DISPLAY_FILTER,
    label: "Filter",
    requiresData: false,
    category: CATEGORY_CONTROLS,
    tooltip: makeDisplayTooltip("No data, just provides data filtering")
});


addGlobalDisplayType({
    type: DISPLAY_HOOK,
    label: "Hook",
    requiresData: true,
    forUser: true,
    category: CATEGORY_CONTROLS,
    tooltip: makeDisplayTooltip("Add your own Javascript",null,"Integrate your own Javascript")                                                
});
addGlobalDisplayType({
    type: DISPLAY_PRE,
    label: "Preformat",
    requiresData: true,
    forUser: true,
    category: CATEGORY_TABLE,
    tooltip: makeDisplayTooltip("Shows records in an HTML PRE tag",null,"Useful for looking at the data")                                                
});
addGlobalDisplayType({
    type: DISPLAY_HTMLTABLE,
    label: "HTML Table",
    requiresData: true,
    forUser: true,
    category: CATEGORY_TABLE,
    tooltip: makeDisplayTooltip("Shows records in an HTML table",null,"Useful for looking at the data")                                                    
});
addGlobalDisplayType({
    type: DISPLAY_COOCCURENCE,
    label: "Cooccurence",
    requiresData: true,
    forUser: true,
    category: CATEGORY_TABLE,
    tooltip: makeDisplayTooltip("Cooccurence Table","cooccurrence.png","Tabular plot showing number of records that share values from two fields"),    

});
addGlobalDisplayType({
    type: DISPLAY_BOXTABLE,
    label: "Box Table",
    requiresData: true,
    forUser: true,
    category: CATEGORY_TABLE,
    tooltip: makeDisplayTooltip("Box Table","boxtable.png","Shows number of records that share the same category field value"),    
});
addGlobalDisplayType({
    type: DISPLAY_DATATABLE,
    label: "Data Table",
    requiresData: true,
    forUser: true,
    category: CATEGORY_TABLE,
    tooltip: makeDisplayTooltip("Data Table",["datatable1.png","datatable2.png"],"Selectable record grouping. Can be colored or show pie charts"),        
});
addGlobalDisplayType({
    type: DISPLAY_DATEGRID,
    label: "Date Grid",
    requiresData: true,
    forUser: true,
    category: CATEGORY_TABLE,
    tooltip: makeDisplayTooltip("Date Grid",["dategrid.png"],"Show records grouped by category and date"),        
});





function RamaddaGraphDisplay(displayManager, id, properties) {
    const ID_GRAPH = "graph";
    const SUPER = new RamaddaDisplay(displayManager, id, DISPLAY_GRAPH, properties);
    if(!window["ForceGraph"]) {
	Utils.importJS("https://unpkg.com/force-graph");
    }
    let myProps = [
	{label:'Graph'},
	{p:'sourceField',ex:''},
	{p:'targetField',ex:''},
	{p:'labelField'},
	{p:'nodeBackground',ex:'#ccc'},
	{p:'drawCircle',ex:'true'},
	{p:'nodeWidth',d:'10'},
	{p:'linkColor',d:'#ccc'},
	{p:'linkDash',ex:'5'},
	{p:'linkWidth',d:'1'},
	{p:'arrowLength',ex:'6'},
	{p:'arrowColor',ex:'green'},
	{p:'directionalParticles',ex:'2'}
    ]


    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
        needsData: function() {
            return true;
        },
	callbackWaiting:false,
        updateUI: function() {
            if(!window["ForceGraph"]) {
		if(!this.callbackWaiting) {
		    this.callbackWaiting = true;
                    setTimeout(()=>{
			this.callbackWaiting = false;
			this.updateUI()
		    },100);
		}
                return;
            }
	    let graphData = null;
	    let html = HU.div([ID, this.domId(ID_GRAPH)]);
	    this.setContents(html);
	    let records = this.filterData();
	    if (!records) {
                return;
	    }  
	    let seenNodes = {};
	    let nodes = [];
	    let links = [];
	    let valueFields   = this.getFieldsByIds(null, this.getProperty("valueFields","",true));
	    let labelField = this.getFieldById(null, this.getLabelField());
	    if(!labelField) {
		let strings = this.getFieldsByType(null, "string");
		if(strings.length>0) labelField = strings[0];
	    }
	    let sourceField = this.getFieldById(null, this.getProperty("sourceField","source"));
	    let targetField = this.getFieldById(null, this.getProperty("targetField","target"));
	    let textTemplate = this.getProperty("tooltip","${default}");
	    if(valueFields.length>0) {
		let seenValue = {};
		records.map((r,index)=>{
		    let label  = labelField?r.getValue(labelField.getIndex()):index;
		    let tooltip =  this.getRecordHtml(r, null, textTemplate);
		    nodes.push({id:index,label:label,tooltip:tooltip});
		    valueFields.map(f=>{
			let value = r.getValue(f.getIndex());
			if(!seenValue[value+"_" + f.getId()]) {
			    seenValue[value+"_" + f.getId()] = true;
			    nodes.push({id:value, isValue:true});
			}
			links.push({source:value, target: index});
		    });
		});
	    } else if(sourceField!=null && targetField!=null) {
		records.map(r=>{
		    let source = r.getValue(sourceField.getIndex());
		    let target = r.getValue(targetField.getIndex());
		    let label  = labelField?r.getValue(labelField.getIndex()):index;
		    if(!seenNodes[source]) {
			seenNodes[source] = true;
			nodes.push({id:source,label:label,tooltip:source});
		    }
		    if(!seenNodes[target]) {
			seenNodes[target] = true;
			nodes.push({id:target,label:label,tooltip:target});
		    }
		    links.push({source:source, target: target});
		});
	    } else {
		this.setDisplayMessage("No source/target fields specified");
		return;
	    }
	    graphData = {
		nodes: nodes,
		links: links
	    };

	    const nodeBackground = this.getNodeBackground('rgba(255, 255, 255, 0.8)');
	    const linkColor = this.getLinkColor();
	    const drawCircle = this.getDrawCircle();
	    const linkWidth = +this.getLinkWidth();
	    const linkDash = +this.getLinkDash(-1);
	    const drawText = this.getProperty("drawText",true);
	    const nodeWidth = this.getNodeWidth(10);
	    const elem = document.getElementById(this.domId(ID_GRAPH));
	    const graph = ForceGraph()(elem).graphData(graphData);
	    graph.nodeCanvasObject((node, ctx, globalScale) => {
		let label = node.label;
		if(!label) label = node.id;
		const fontSize = 12/globalScale;
		ctx.font = fontSize +"px Sans-Serif";
		let textWidth = ctx.measureText(label).width;
		if(!drawText)
		    textWidth=nodeWidth;
		if(node.isValue) {
		    let bckgDimensions = [textWidth, fontSize].map(n => n + fontSize * 0.2+2); 
		    ctx.lineWidth = 1;
		    ctx.strokeStyle = "#000";
		    ctx.fillStyle = "#fff";
		    ctx.fillRect(node.x - bckgDimensions[0] / 2, node.y - bckgDimensions[1] / 2, ...bckgDimensions);
		    ctx.strokeRect(node.x - bckgDimensions[0] / 2, node.y - bckgDimensions[1] / 2, ...bckgDimensions);
		} else  {
		    let dim = [textWidth, fontSize].map(n => n + fontSize * 0.2+2); 
		    ctx.fillStyle = nodeBackground;
		    ctx.strokeStyle = "#000";
		    if(drawCircle) {
			ctx.beginPath();
			ctx.arc(node.x, node.y, dim[0]/2, 0, 2 * Math.PI);
			ctx.fill(); 
		    } else {
			ctx.lineWidth = 1;
			ctx.fillRect(node.x - dim[0] / 2, node.y - dim[1] / 2, ...dim);
			ctx.strokeRect(node.x - dim[0] / 2, node.y - dim[1] / 2, ...dim);
		    }
		}
		if(drawText) {
		    ctx.textAlign = 'center';
		    ctx.textBaseline = 'middle';
		    ctx.fillStyle = "black";
		    ctx.fillText(label, node.x, node.y);
		}
	    });

	    if(this.getWidth())
		graph.width(this.getWidth());
	    if(this.getHeight())
		graph.height(this.getHeight());
	    graph.nodeLabel(node => node.tooltip?node.tooltip:null)
	    graph.linkWidth(+this.getLinkWidth());
	    graph.linkColor(this.getLinkColor());
	    if(this.getArrowColor()) {
		graph.linkDirectionalArrowColor(this.getArrowColor());
	    }
	    if(this.getArrowLength()) {
		graph.linkDirectionalArrowLength(+this.getArrowLength());
		graph.linkDirectionalArrowRelPos(+this.getProperty("arrowPosition",0.9));
	    }

	    if(this.getProperty("directionalParticles")) {
		graph.linkDirectionalParticles(+this.getProperty("directionalParticles"));
	    }
	}
    })
}


function RamaddaTreeDisplay(displayManager, id, properties) {
    const SUPER = new RamaddaDisplay(displayManager, id, DISPLAY_TREE, properties);
    let myProps = [
	{label:'Tree'},
	 {p:'maxDepth',ex:'3'},
	 {p:'showDetails',ex:'false'},
    ];
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
	countToRecord: {},
        needsData: function() {
            return true;
        },
        updateUI: function() {
            let records = this.filterData();
            if (!records) return;
	    let roots=null;
	    try {
		roots = this.makeTree(records);
	    } catch(error) {
                this.handleError("An error has occurred:" + error, error);
		return;
	    }

	    let html = "";
	    let baseId = this.domId("node");
	    let cnt=0;
	    let depth = 0;
	    let maxDepth = +this.getProperty("maxDepth",10);
	    let template = this.getProperty("recordTemplate","${default}");
	    let showDetails = this.getProperty("showDetails",true);
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
		    image = HU.image(on?icon_downdart:icon_rightdart,[ID,baseId+"_toggle_image" + cnt]) + " ";
		}
		html+=HU.div([CLASS,"display-tree-toggle",ID,baseId+"_toggle" + cnt,"toggle-state",on,"block-count",cnt], image +  node.label);
		html+=HU.open(DIV,[ID, baseId+"_block"+cnt,CLASS,"display-tree-block",STYLE,HU.css('display', (on?"block":"none"))]);
		if(details && details!="") {
		    if(node.children.length>0) {
			html+= HU.div([CLASS,"display-tree-toggle-details",ID,baseId+"_toggle_details" + cnt,"toggle-state",false,"block-count",cnt], HU.image(icon_rightdart,[ID,baseId+"_toggle_details_image" + cnt]) + " Details");
			html+=HU.div([ID, baseId+"_block_details"+cnt,CLASS,"display-tree-block",STYLE,HU.css('display','none')],details);
		    } else {
			html+=details;
		    }
		}
		
		if(node.children.length>0) {
		    node.children.map(func);
		}
		depth--;
		html+=HU.close(DIV);
	    }
	    //	    console.log("roots:" + roots.length);
	    roots.map(func);
	    this.myRecords = [];
            this.displayHtml(html);
	    this.find(".display-tree-toggle").click(function() {
		let state = (/true/i).test($(this).attr("toggle-state"));
		state = !state;
		let cnt = $(this).attr("block-count");
		let block = $("#"+ baseId+"_block"+cnt);
		let img = $("#"+ baseId+"_toggle_image"+cnt);
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
		    _this.propagateEventRecordSelection({record: record});
		}
	    });
	    this.find(".display-tree-toggle-details").click(function() {
		let state = (/true/i).test($(this).attr("toggle-state"));
		state = !state;
		let cnt = $(this).attr("block-count");
		let block = $("#"+ baseId+"_block_details"+cnt);
		let img = $("#"+ baseId+"_toggle_details_image"+cnt);
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




function RamaddaTimelineDisplay(displayManager, id, properties) {
    const ID_TIMELINE = "timeline";
    if(!properties.height) properties.height=400;
    const SUPER =  new RamaddaFieldsDisplay(displayManager, id, DISPLAY_TIMELINE, properties);
    let myProps = [
	{label:'Timeline'},
	{p:'titleField',ex:''},
	{p:'titleLength',ex:'100'},	
	{p:'imageField',ex:''},
	{p:'urlField',ex:''},
	{p:'textTemplate',ex:''},
	{p:'startDateField',ex:''},
	{p:'endDateField',ex:''},
	{p:'startAtSlide',ex:'0'},
	{p:'startAtEnd',ex:'true'},
	{p:'scaleFactor',ex:'10'},
	{p:'initialZoom',ex:'10'},	
	{p:'navHeight',ex:'150'},
	{p:'backgroundColor',ex:'#ccc'},
	{p:'groupField',ex:''},
	{p:'urlField',ex:''},
	{p:'timeTo',d:'day',ex:'year|day|hour|second',canCache:true},
//	{p:'justTimeline',ex:"true"},
	{p:'hideBanner',ex:"true"},
    ];

    Utils.importJS(RamaddaUtil.getCdnUrl("/lib/timeline3/timeline.js"));
    let css = "https://cdn.knightlab.com/libs/timeline3/latest/css/timeline.css";
    //    css =  RamaddaUtil.getCdnUrl("/lib/timeline3/timeline.css");
    $(HU.tag('link',['rel','stylesheet','href', css,'type','text/css'] )).appendTo("head");
   
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
        needsData: function() {
            return true;
        },
	loadCnt:0,
	timelineLoaded: false,
        checkLayout: function() {
	    //Update the ui when the tab this is in is activated
	    this.updateUI();
	},
	updateUI: function() {
	    if(!this.timelineLoaded) {
		try {
		    let tmp =  TL.Timeline;
		    this.timelineLoaded = true;
		} catch(err) {
		    if(this.loadCnt++<100) {
			setTimeout(()=>this.updateUI(),100);
			return;
		    }
		}
	    }
	    if(!this.timelineLoaded) {
		this.setDisplayMessage("Could not load timeline");
		return;
	    }
            let records = this.filterData();
	    if(records==null) return;
	    let timelineId = this.domId(ID_TIMELINE);
	    let html = HU.div([ID,timelineId]);
	    this.setContents(html);
	    this.timelineReady = false;
	    let opts = {
		timenav_position: this.getProperty("timelinePosition","top"),
//		debug:true,
		start_at_end: this.getPropertyStartAtEnd(false),
		start_at_slide: this.getPropertyStartAtSlide(0),
		timenav_height: this.getPropertyNavHeight(200),
		height:100,
		menubar_height:100,
		gotoCallback: (slide)=>{
		    if(this.timelineReady) {
			let record = records[slide];
			if(record) {
			    this.propagateEventRecordSelection({record: record});
			}
		    }
		}
            };
	    if(this.getPropertyBackgroundColor())
		opts.default_bg_color = this.getPropertyBackgroundColor();
	    if(this.getPropertyScaleFactor(0))
		opts.scale_factor = this.getPropertyScaleFactor();
	    if(this.getPropertyInitialZoom(0))
		opts.scale_factor = this.getPropertyInitialZoom();

	    let json = {};
	    let events = [];
	    json.events = events;
	    let titleField = this.getFieldById(null,this.getPropertyTitleField());
	    if(titleField==null) {
		titleField = this.getFieldById(null, "title");
	    }
	    if(titleField==null) {
		titleField = this.getFieldById(null, "name");
	    }
	    let titleLength = this.getTitleLength();

	    let startDateField = this.getFieldById(null,this.getPropertyStartDateField());
	    if(!startDateField) startDateField = this.getFieldByType(null,"date");
	    let endDateField = this.getFieldById(null,this.getPropertyEndDateField());
	    let imageField = this.getFieldById(null,this.getPropertyImageField());
	    let groupField = this.getFieldById(null,this.getPropertyGroupField());
	    let urlField = this.getFieldById(null,this.getPropertyUrlField());
	    let textTemplate = this.getPropertyTextTemplate("${default}");
	    let timeTo = this.getTimeTo();
	    let showYears = this.getProperty("showYears",false);
	    this.recordToIndex = {};
	    this.idToRecord = {};
	    for(let i=0;i<records.length;i++) {
		let record = records[i]; 
		this.idToRecord[record.getId()] = record;
		this.recordToIndex[record.getId()] = i;
		let tuple = record.getData();
		let event = {
		};	
		let headline = titleField? tuple[titleField.getIndex()]:" record:" + (i+1);
		if(titleLength && headline.length>titleLength)
		    headline = headline.substring(0,titleLength)+"...";
		let debug = false;
		let text =  this.getRecordHtml(record, null, textTemplate,debug);
		if(urlField) {
		    let url  = record.getValue(urlField.getIndex());
		    headline = HU.href(url,headline);
		}

		event.unique_id = record.getId();
		event.text = {
		    headline: headline,
		    text:text
		};
		if(groupField) {
		    event.group = record.getValue(groupField.getIndex());
		}

		if(imageField) {
		    event.media = {
			url:record.getValue(imageField.getIndex())
		    };
		    if(urlField) {
			event.media.link = record.getValue(urlField.getIndex());
			event.media.link_target = "_timelinemedia";
		    }
		}
		let startDate =this.getDate(startDateField? tuple[startDateField.getIndex()]: record.getTime());
		if (showYears) {
		    event.start_date = {
			year: startDate.year
		    }
		} else {
		    event.start_date  = startDate;
		    if(endDateField) {
			event.end_date = tuple[endDateField.getIndex()];
		    }
		}
		//		console.log(JSON.stringify(event));
		events.push(event);
	    }
//	    console.log(JSON.stringify(json,null,2));
	    if($("#" + timelineId).length==0) {
//		console.info("No timeline div:" + timelineId);
		return;
	    }

	    this.timeline = new TL.Timeline(timelineId,json,opts);
	    if(this.getPropertyHideBanner(false)) {
		this.jq(ID_TIMELINE).find(".tl-storyslider").css("display","none");
		this.jq(ID_TIMELINE).find(".tl-menubar").css("display","none");		
	    } 
	    this.jq(ID_TIMELINE).find(".tl-text").css("padding","0px");
	    this.jq(ID_TIMELINE).find(".tl-slide-content").css("padding","0px 0px");
	    //	    this.jq(ID_TIMELINE).find(".tl-slide-content").css("width","100%");
	    this.jq(ID_TIMELINE).find(".tl-slidenav-description").css("display","none");
	    this.timelineReady = true;

	},
        handleEventRecordSelection: function(source, args) {
	    if(!args.record) return;
	    let index = this.recordToIndex[args.record.getId()];
	    if(!Utils.isDefined(index)) return;
	    this.timeline.goTo(index);
	},
	getDate: function(time) {
	    let timeTo = this.getTimeTo();
	    let dt =  {year: time.getUTCFullYear()};
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

function RamaddaHoursDisplay(displayManager, id, properties) {
    const ID_TIMELINE = "timeline";
    const SUPER =  new RamaddaFieldsDisplay(displayManager, id, DISPLAY_HOURS, properties);
    const BOX_COLOR = "lightblue";
    const MULTI_ID = "multiid";
  
    let myProps = [
	{label:'Hours'},
	{p:'dateField',ex:''},
	{p:'boxWidth',ex:''},
	{p:'boxColor',ex:'blue'},	
	{p:'rowBackground',ex:''},
	{p:'dayLabelStyle',ex:''},
	{p:'fillHours',ex:'false'},			
    ];
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
        needsData: function() {
            return true;
        },
	updateUI: function() {
            let records = this.filterData();
	    if(records==null) return;
	    let _this =this;
	    let html = "";
	    let dateField = this.getFieldById(null,this.getPropertyDateField());
	    let days = [];
	    let dayToHours = {};
	    let dateFormat = this.getProperty("dateFormat","mdy");
	    this.recordToIndex = {};
	    let timeZoneOffset = +this.getProperty("timeZoneOffset",0);
	    records.forEach((record,idx)=>{
		this.recordToIndex[record.getId()] = idx;
		let dttm0 =dateField? recordtuple[dateField.getIndex()]: record.getTime();
		let dttm = dttm0;
		let newHours = dttm.getUTCHours()+timeZoneOffset;
		dttm = new Date(Date.UTC(dttm.getUTCFullYear(),dttm.getUTCMonth(),dttm.getUTCDate(),newHours));
		let hour = +dttm.getUTCHours();
		let dayDate = new Date(Date.UTC(dttm.getUTCFullYear(),dttm.getUTCMonth(),dttm.getUTCDate()));
		let dayInfo = dayToHours[dayDate];

		if(!dayInfo) {
		    dayInfo = dayToHours[dayDate] = {
			dttm:dttm,
			hours:[],
			minutesCount:{},
			hourToRecords:{},
			minHour: hour,
			maxHour:hour
		    };
		    days.push(dayDate);
		}
		dayInfo.minHour = Math.min(dayInfo.minHour,hour);
		dayInfo.maxHour = Math.max(dayInfo.maxHour,hour);		
		let minutes = dttm0.getMinutes();
		let key  = hour+"_"+minutes;
		if(!dayInfo.minutesCount[key]) dayInfo.minutesCount[key] = 0;
		dayInfo.minutesCount[key]++;
		if(!dayInfo.hourToRecords[hour]) {
		    dayInfo.hours.push(hour);
		    dayInfo.hourToRecords[hour] = [];
		}
		dayInfo.hourToRecords[hour].push(record);
	    });
	    Utils.sortDates(days);
	    html = HU.open("div",[STYLE,"position:relative;"]) + HU.open("table",["width","100%"]);
	    let boxWidth = this.getPropertyBoxWidth(10);
	    let boxColor = this.getPropertyBoxColor(BOX_COLOR);
	    let extra = "";
	    days.forEach(day=>{
		let dayInfo = dayToHours[day];
		if(this.getPropertyFillHours(true)) {
		    for(let i=dayInfo.minHour;i<dayInfo.maxHour;i++) {
			if(!dayInfo.hourToRecords[i]) {
			    dayInfo.hours.push(i);
			    dayInfo.hourToRecords[i] = [];
			}
		    }
		}
		let dayLabel = Utils.formatDateWithFormat(day,dateFormat,true);
		html +=  HU.tr([STYLE,"border-bottom:1px solid #ccc;"],HU.tds([],["",HU.div([CLASS,"display-hours-label"], dayLabel),"#"]));
		let multiCount = 0;
		Utils.sortNumbers(dayInfo.hours).forEach(hour=>{
		    let row = "<tr style='border-top:1px solid #ccc;'>";
//		    if(hour!=9) return
		    let hourLabel  = HU.div([STYLE,this.getPropertyDayLabelStyle("")], Utils.formatHour(hour));
		    row += HU.td([WIDTH,"10","align","right"],hourLabel);
		    row += HU.open("td",[STYLE,HU.css('background','#efefef'),WIDTH,"100%"]);
		    row += HU.open("div",[STYLE, HU.css(HEIGHT,"100%",POSITION,"relative",WIDTH,"100%",BACKGROUND,this.getPropertyRowBackground("#eee"))]);
		    row += "&nbsp;";
		    let displayed = {};
		    let didOne= false;
		    dayInfo.hourToRecords[hour].forEach(record=>{
			let dttm =dateField? record.getValue(dateField.getIndex()): record.getTime();
			let minutes = dttm.getMinutes();
			//pad a bit on the left
			let left =  Math.round(minutes/61.0*100)+"%";
			let key = hour+"_"+minutes;
			if(dayInfo.minutesCount[key]>1) {
			    if(!displayed[minutes])  {
				let multiId = this.domId("multi"+ (multiCount++));
				displayed[minutes] = {
				    multiid:multiId,
				    contents:""};
				row+= HU.div([ID,multiId, TITLE,"Click to view multiples","dttm",dayInfo.dttm.getTime(), "hour",hour,"minute",minutes, STYLE, HU.css('top','0px','left',left),CLASS,'display-hours-box-multi'],dayInfo.minutesCount[key]);
			    }
			    displayed[minutes].contents +=
				HU.div([MULTI_ID,displayed[minutes].multiid,RECORD_ID, record.getId(), RECORD_INDEX,_this.recordToIndex[record.getId()],
					TITLE,"",STYLE,
					HU.css(WIDTH,boxWidth+"px",BACKGROUND,boxColor),
					CLASS,'display-hours-box'],"");
			} else {
			    let css = HU.css("position","absolute","top","0px",WIDTH,boxWidth+"px",'background',boxColor,'left',left);
			    row+= HU.div([RECORD_ID, record.getId(), RECORD_INDEX,_this.recordToIndex[record.getId()],
					  TITLE,"",STYLE, css,CLASS,'display-hours-box']);
			}
			didOne=true;
		    });
		    for(minute in displayed) {
			let id = dayInfo.dttm.getTime()+"_" + hour +"_"+minute;
			extra+=HU.div([ID,this.domId(id), CLASS,"display-hours-box-extra"],displayed[minute].contents);
		    }
		    row+="</div></td>";
		    row+=HU.td([],dayInfo.hourToRecords[hour].length);
		    row +="</tr>"
 		    if(didOne) html+=row;
		});
	    });
	    html+="</table>";
	    html+=extra;
	    html+="&nbsp;</div>";
	    this.setContents(html);
	    this.multis = this.find(".display-hours-box-multi");
	    this.multis.click(function() {
		let id = $(this).attr("dttm")+"_" + $(this).attr("hour") +"_"+$(this).attr("minute");
		let div = _this.jq(id);
		if($(this).attr("showing")=="true") {
		    div.hide();
		    $(this).css("border","1px solid #ccc");
		    $(this).attr("showing",false);
		    return;
		}
		$(this).css("border","1px solid " + HIGHLIGHT_COLOR);
		$(this).attr("showing",true);
		div.show();
		div.position({
                    of: $(this),
                    my: "left top",
                    at: "left bottom+2",
                    collision: "none none"
		});
	    });
	    this.boxes = this.find(".display-hours-box");
	    this.boxes.click(function() {
		let state = (/true/i).test($(this).attr("toggle-state"));
		state = !state;
		_this.boxes.css("background",BOX_COLOR);
		if(state)  {
		    $(this).css("background",HIGHLIGHT_COLOR);
		}
		let record = records[+$(this).attr(RECORD_INDEX)];
		if(record) {
		    _this.propagateEventRecordSelection({record: record});
		}
	    });
	    this.makeTooltips(this.boxes,records,null,null,false);
	},
        handleEventRecordSelection: function(source, args) {
	    if(!args.record) return;
	    let select = ".display-hours-box[" + RECORD_ID +"='" + args.record.getId()+"']";
	    let box = this.find(select);
	    if(box.length) {
		this.boxes.css("background",BOX_COLOR);
		this.multis.css("background","#efefef");		
		let multiId = 	box.attr(MULTI_ID);
		if(multiId) {
		    let multi = this.find("#" + multiId);
		    if(multi.length>0) {
			box = multi;
			box.css("background",HIGHLIGHT_COLOR);
		    }
		}
		
		box.css("background",HIGHLIGHT_COLOR);
		HU.scrollVisible(this.getContents(), box);
	    }
	},
    });
}





function RamaddaBlankDisplay(displayManager, id, properties,type) {
    if(!properties.width) properties.width='100%';
    properties.showMenu = false;
    properties.showTitle = false;
    const SUPER =  new RamaddaFieldsDisplay(displayManager, id, type??DISPLAY_BLANK, properties);
    defineDisplay(type!=null?this:addRamaddaDisplay(this), SUPER, [], {
        needsData: function() {
            return true;
        },
	//Overwrite the display message to put in in the header suffix
	setDisplayMessage:function(msg) {
	    if(this.dataLoadFailed) {
		return;
	    }

	    if(!Utils.stringDefined(msg)) {
		this.jq(ID_HEADER2_SUFFIX).html("").hide();
		return;
	    }
	    let header = this.jq(ID_HEADER2_SUFFIX);
	    if(header.length==0)
		header = this.jq(ID_HEADER2);
	    header.show();
	    header.html(HU.span(['class','display-output-message-tight'],msg));
	},
	setNoDataMessage:function(message) {
	    if(Utils.stringDefined(message)) {
		this.jq(ID_HEADER2_SUFFIX).show();
		this.jq(ID_HEADER2_SUFFIX).html(HU.span(['class','display-output-message-tight'],message));
	    }
	},
	clearDisplayMessage:function() {
	    this.jq(ID_HEADER2_SUFFIX).hide();
	},

	updateUI: function() {
	    let records = this.filterData();
	    this.setContents("");
	    if(!records) return;
	    let colorBy = this.getColorByInfo(records);
	    if(colorBy.index>=0) {
		records.forEach(record=>{
		    color =  colorBy.getColor(record.getData()[colorBy.index], record);
		});
		colorBy.displayColorTable();
	    }
	}});
}


function RamaddaFilterDisplay(displayManager, id, properties) {
    properties.hideFilterWidget=false;
    let SUPER =  new RamaddaBlankDisplay(displayManager, id,  properties,DISPLAY_FILTER);
    let myProps =[];
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
    });
}




function RamaddaHookDisplay(displayManager, id, properties) {
    const SUPER =  new RamaddaFieldsDisplay(displayManager, id, DISPLAY_HOOK, properties);
    if(properties.hook) {
	this.hook = new window[properties.hook];
    }
    defineDisplay(addRamaddaDisplay(this), SUPER, [], {
        needsData: function() {
            return true;
        },
	call:function(what,data) {
	    if(!this.hook) return false;
	    if(!this.hook[what])  {
		return false;
	    }
	    this.hook[what](this,data);
	    return true;
	},
	updateUI: function() {
	    if(!this.hook) {
		this.setContents('No hook defined');
		return;
	    }
	    let records = this.filterData();
	    if(!records) return;
            let fields = this.getSelectedFields([]);
	    let allFields = this.getFields();
	    this.clearDisplayMessage();
	    if(!this.call('updateUI',{allFields:allFields,fields:fields,records:records})) {
		this.setContents('No updateUI defined in hook');
	    }
	}});
}


function RamaddaPreDisplay(displayManager, id, properties) {
    const SUPER =  new RamaddaFieldsDisplay(displayManager, id, DISPLAY_PRE, properties);
    let myProps = [
	{label:'Pre'},
	{p:'numRecords',ex:'100',d:1000},
	{p:'includeGeo',ex:'true',d:true},	
    ];

    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
	updateUI: function() {
	    let records = this.filterData();
	    if(!records) {
		this.setContents("Loading data...");		
		return;
	    }
            let pointData = this.dataCollection.getList()[0];
            let fields = pointData.getRecordFields();
	    let numRecords = this.getNumRecords();
	    let includeGeo = this.getIncludeGeo();
	    let html ="Number of records:" + records.length+"<pre>";
	    fields.forEach((f,idx)=>{
		if(idx>0) html+=", ";
		html+=f.getId() +"[" + f.getType()+"]";
	    });
	    if(includeGeo) html+=", latitude, longitude";
	    html+="\n";
	    records.every((r,idx)=>{
		if(numRecords>-1 && idx>numRecords) return false;
		let d = r.getData();
		d = d.map(d=>{
		    if(d.getTime) return this.formatDate(d);
		    return d;
		});
		html+="#" + idx+": ";
		html+=d.join(", ");
		if(includeGeo) {
		    html+=", " + r.getLatitude() +"," + r.getLongitude();
		}
		html+="\n";
		return true;
	    });
	    html+="</pre>"
	    this.setContents(html);
	}});
}



function RamaddaHtmltableDisplay(displayManager, id, properties,type) {
    const ID_TABLE  = "table";
    const SUPER =  new RamaddaFieldsDisplay(displayManager, id, type??DISPLAY_HTMLTABLE, properties);
    let myProps = [
	{label:'Html Table'},
	{p:'numRecords',ex:'5000',d:5000,tt:'Number of records to show'},
	{p:'scrollY',ex:'300px'},				
	{p:'includeGeo',ex:'true',d:false},
	{p:'includeDate',ex:'true',d:false},
	{p:'includeRowIndex',ex:'true',d:false},	
	{p:'includeFieldDescription'},
	{p:'includeUnits',d:true},
	{p:'fancy',ex:'true',d:true},
	{p:'maxCellHeight',ex:'200px', tt:'Max cell height',d:'200px'},	
	{p:'maxLength',ex:'500',d:-1, tt:'If string is gt maxLength then scroll it'},
	{p:'maxColumns'},
	{p:'colorCells',ex:'field1,field2'},
	{p:'iconField'},
	{p:'linkField'},
	{p:'categoryField'},
	{p:'colorRowBy'},
	{p:'colorFullRow',d:true,tt:'If doing color row by do we color the full row or just the start'},	
	{p:'colorHeaderLabel',tt:'Label for the color header'},
	{p:'colorHeaderTemplate',tt:'Template to show in row color header'},
	{p:'colorHeaderStyle',tt:'CSS for color header. defaults to rotated text'},	
        {p:'showBar',ex:'true',tt:'Default show bar'},
        {p:'highlightFilterText',ex:'true',tt:'Highlight any filter text'},	
        {p:'&lt;field&gt;.nowrap',ex:'true',tt:"Don't wrap the column"},
        {p:'&lt;field&gt;.width',ex:'30%',tt:"Column width"},
        {p:'&lt;field&gt;.template',ex:'foo:${value}',tt:"Record template"},		
        {p:'&lt;field&gt;.showBar',ex:'true',tt:'Show bar'},
        {p:'&lt;field&gt;.barMin',ex:'0',tt:'Min value'},
        {p:'&lt;field&gt;.barMax',ex:'100',tt:'Max value'},
	

	{p:'showSummary',ex:'true'},
	{p:'showSummaryTotal',ex:'true'},
	{p:'showSummaryAverage',ex:'true'},
	{p:'showSummaryMinMax',ex:'true'},
	{p:'showGrandSummary',ex:'true'},		

	{p:'barLabelInside',ex:'false'},
        {p:'barStyle',ex:'background:red;',tt:'Bar style'},			
	{p:'tableStyle',d:'border-left:var(--basic-border);border-right:var(--basic-border)'},
	{p:'tableHeaderStyle'},
 	{p:'tableCellStyle'},	
	{p:'showAddRow',ex:'true'},
    ];

    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
	displayData: function() {
	    this.updateUI();
	},
	makeColumn:function(record,field,attrs,v) {
	    return HU.td(attrs,v);
	},
	handleColumn:function(fields,aggByField,field,record,v,tdAttrs,matchers) {
	    if(!this.columnTemplates) {
		this.columnTemplates = {};
		fields.forEach((f,idx)=>{
		    let template = this.getProperty(f.getId()+"_template");
		    if(template) templates[f.getId()] = template;
		});
	    }
	    let template = this.columnTemplates[field.getId()];
	    if(template) {
		return this.getRecordHtml(record,null, template);
	    }
	    if(!aggByField) {
		let attrs=[...tdAttrs];
		attrs.push('field-id',field.getId(),'record-id',record.getId(),'record-index',record.rowIndex);
		if(matchers) {
		    let sv = String(v);
		    matchers.forEach(h=>{
			sv  = h.highlight(sv,field.getId());
		    });
		    v = sv;
		}

		return this.makeColumn(record,field,attrs,v);
	    }
	    if(field.getId() != aggByField.getId()) {
		return this.makeColumn(record,field,tdAttrs,v);
	    }
	    if(!record.isAggregate) {
		let spacer = "&nbsp;&nbsp;&nbsp;&nbsp;";
		return HU.td(tdAttrs,HU.row([["width","1%","style","padding:2px;"], spacer],[["style","padding:0px;"],v]));
	    }
	    let span = HU.span([ID,aggId+"_toggle","toggleopen","false", CLASS,"ramadda-clickable"],
			       HU.span([ID,aggId+"_toggleimage"],HU.getIconImage("fas fa-chevron-right"))+"&nbsp;" + v);
	    tdAttrs = Utils.mergeLists(tdAttrs,["nowrap",null]);
	    return HU.td(tdAttrs,span);
	},
	updateUI: function() {
	    let records = this.filterData();
	    if(!records) {
		this.setDisplayMessage("Loading data...");		
		return;
	    }
//	    console.time('HtmlTable.update');
	    this.updateHtmlTable(records);
//	    console.timeEnd('HtmlTable.update');
	},
	updateHtmlTable: function(records) {
	    let fancy  = this.getFancy();
            let pointData = this.getPointData();
            let fields = pointData.getRecordFields();
            let selectedFields = this.getSelectedFields();
	    let urlField = this.getFieldById(null,this.getProperty("urlField"));
	    let iconField = this.getFieldById(null,this.getIconField());
	    let categoryField = this.getFieldById(null,this.getProperty('categoryField'));

	    fields= (selectedFields && selectedFields.length>0)?selectedFields:fields;
	    let anyGroups = fields.filter(f=>{
		if(f==null) return true;
		return f.getGroup()!=null;
	    }).length>0;

	    let groupCnt={};
	    if(anyGroups) {
		let groups = [];
		let group = null;
		for(let i=0;i<fields.length;i++) {
		    let field = fields[i];
		    if(field==null) continue;
		    group = field.getGroup();
		    if(group==null) {
			groups.push(field);
			continue;
		    }
		    groups.push(field);
		    groupCnt[group]=1;
		    for(let j=i+1;j<fields.length;j++) {
			if(fields[j]==null) continue;
			if(fields[j].getGroup()==field.getGroup()) {
			    groupCnt[group]++;
			    groups.push(fields[j]);
			    fields[j]=null;
			}
		    }
		}
		fields=groups;
	    }

	    let aggByField = this.getFieldById(null,this.getProperty("aggregateBy"));
	    if(aggByField) {
//		aggByField.label = this.getProperty("aggregateName",this.getFieldLabel(aggByField));
		let csvUtil = new CsvUtil();
		let tmp = new PointData("", fields, records);
		let converted = csvUtil.process(this, tmp,"aggregate(includeRows=true, groupBy=" + aggByField.getId()+");");
		records = converted.getRecords();
		fields = converted.getRecordFields();
	    }

	    
	    let colorRowBy;
	    let colorFullRow = this.getColorFullRow();
	    let colorHeaderStyle =
		this.getColorHeaderStyle('font-weight:bold;margin-right:10px;margin-top:10px;margin-bottom:10px;writing-mode: vertical-lr; -ms-writing-mode: tb-rl; transform: rotate(180deg);font-size:80%;');

	    if(Utils.stringDefined(this.getProperty('colorRowBy'))) {
		colorRowBy=[];
		this.getProperty('colorRowBy').split(',').forEach(c=>{
		    let tmp =   this.getFieldById(null, c);
		    if(tmp) {
			let template = this.getProperty(c+'.colorHeaderTemplate',this.getProperty('colorHeaderTemplate'));
			let label = this.getProperty(c+'.colorHeaderLabel',this.getProperty('colorHeaderLabel'));			
			colorRowBy.push({
			    template:template,
			    label:label,
			    colorBy:new ColorByInfo(this, null, records, null,null,null, null, tmp),
			});
		    }});
	    }


	    let colorByMap = {};
	    let cbs = [];
	    this.getColorCells("").split(",").forEach(c=>{
		let f = this.getFieldById(null,c);
		if(f) {
		    colorByMap[c] = new ColorByInfo(this, null, records, null,c+".colorByMap",null, c, f);
		    cbs.push(colorByMap[c]);
		}
	    });
	    
	    let numRecords = this.getNumRecords();
	    let includeIdx = this.getIncludeRowIndex();
	    let includeGeo = this.getIncludeGeo();
	    let includeDate = this.getIncludeDate();	    
	    let includeUnits = this.getIncludeUnits();	    
	    let html='';
	    //Only do the hover when we aren't coloring the rows
	    if(!colorRowBy) {
		html+=HU.cssTag('.display-htmltable-row:hover {background:var(--highlight-background) !important;');
	    }

	    if(this.getProperty('tableCellStyle')) {
		html+=HU.cssTag('.display-htmltable-td {' +
				this.getProperty('tableCellStyle')+'}');
	    }

	    html+=HU.open('div',[ID,this.domId(ID_TABLE+'_wrapper')]);
	    html +=HU.openTag('table',[CLASS,"ramadda-table stripe", 'width','100%',ID,this.domId(ID_TABLE),ATTR_STYLE,this.getTableStyle()]);
	    html+='\n';
	    let maxColumns = this.getMaxColumns(-1);
	    let headerAttrs = [STYLE,"white-space:nowrap;background:#efefef;padding:5px; font-weight:bold;"];
	    headerAttrs = [];
	    html+="<thead>\n";
	    if(anyGroups) {
		let attrs = [STYLE,HU.css("background","#fff","width","100%")];
		html+="<tr style='background:transparent;' valign=top>\n"
		let group = null;
		let seen = {};
		fields.forEach((f,idx)=>{
		    if(f.getGroup()) {
			if(group!=f.getGroup()) {
			    group = f.getGroup();
			    if(!seen[group]) {
				seen[group] =true;
				html+=HU.th([CLASS,"display-table-group-header-th", STYLE,HU.css("border-bottom","0px solid transparent", "background","transparent"), "colspan",groupCnt[group]],HU.div([CLASS,"display-table-group-header"], group))+"\n";
			    }
			}
			return;
		    }
		    html+=HU.th([STYLE,HU.css("border-bottom","0px solid transparent", "background","transparent")],HU.div(attrs,"&nbsp;"))+"\n";
		});
		html+="</tr>\n";
	    } 

	    let header1="<tr  valign=top>\n"
	    let header2="<tr  valign=top>\n"	    
	    if(includeDate) {
		header1+=HU.th([],HU.div(headerAttrs,"Date"));
		header2+=HU.th([],HU.div(headerAttrs,"Date"));
	    } else {
		fields = fields.filter(f=>{
		    return !f.isRecordDate();
		});
	    }

	    //Add the place holder for the colored rows
	    if(colorRowBy && !colorFullRow) {
		colorRowBy.forEach(c=>{
		    header1+=HU.th(['style','max-width:16px;width:16px;'],HU.div([],c.label));
		    header2+=HU.th(['style','max-width:16px;width:16px;'],HU.div([],c.label));		    
		});
	    }


	    if(includeIdx) {
		header1+=HU.th(HU.div([],""));
		header2+=HU.th(HU.div([],""));
	    }

	    let headerStyle = this.getTableHeaderStyle("")+"text-align:center;";
	    let fieldMap = {}
	    let sortFields = this.getProperty("sortFields");
	    let sortAscending = this.getSortAscending();
	    if(sortFields) {
		let tmp = {};
		sortFields.split(",").forEach(f=>{tmp[f]=true;});
		sortFields= tmp;
	    }
	    fields.forEach((f,idx)=>{
		if(maxColumns>0 && idx>=maxColumns) return;
		fieldMap[f.getId()] = f;
		let sort = sortFields && sortFields[f.getId()];
		let title = f.getDescription();
		if(title) title+="&#10;";
		title=title??"";
		title+="Click to sort";
		let attrs = [TITLE,title,CLASS,"ramadda-clickable display-table-header", "field-id",f.getId(),STYLE,headerStyle];
		let width = this.getProperty(f.getId()+".width");
		if(width) attrs.push("width",width);

		if(sort) {
		    attrs.push("sorted");
		    attrs.push("true");
		}

		if(fancy) {
		    let label = this.getFieldLabel(f);
		    if(sort) label = HU.getIconImage(sortAscending?"fas fa-arrow-down":"fas fa-arrow-up",null, [STYLE,HU.css('font-size','8pt !important')]) +" " + label;
		    let desc = includeUnits?f.getUnitLabel(this):f.getLabel(this);
		    header1+=HU.th(attrs,HU.div(headerAttrs,desc));
		    header2+=HU.th(attrs,HU.div(headerAttrs,f.getDescription()??""));
		}
		else {
		    header1+=HU.th(attrs,HU.div(headerAttrs,f.getId() +"[" + f.getType()+"]"));
		    header2+=HU.th(attrs,HU.div(headerAttrs,f.getId() +"[" + f.getDescription()??""+"]"));
		}
		
	    });

	    if(includeGeo) header1+=HU.th([],HU.div(headerAttrs,"latitude")) + HU.th([],HU.div(headerAttrs,"longitude"));
	    if(includeGeo) header2+=HU.th([],HU.div(headerAttrs,"")) + HU.th([],HU.div(headerAttrs,""));	    
	    header1+="</tr>\n";
	    header2+="</tr>\n";	    
	    html+=header1;
	    if(this.getIncludeFieldDescription()) {
		html+=header2;
	    }
	    html+='</thead>';
	    html+=HU.open('tbody',[ATTR_STYLE,HU.css('max-height','200px','overflow-y','auto')]);
	    this.recordMap = {};
	    this.fieldMap = {};
	    fields.forEach(f=>{this.fieldMap[f.getId()] = f;})
 	    let aggId = "";
	    let aggIds = [];


	    let colAttrs = {
	    }
	    fields.forEach((f,idx)=>{
		let attrs = colAttrs[f.getId()] = []
		let width = this.getProperty(f.getId()+".width");
		if(width) attrs.push("width",width);
		if(this.getProperty(f.getId()+".nowrap",false))
		    attrs.push("nowrap","true");
	    });

	    let maxLength = this.getMaxLength();
	    let maxHeight = this.getMaxCellHeight();
	    let category;
	    let fieldProps = {};
	    fields.forEach(f=>{
		if(f.isFieldNumeric()) {
		    let showBar = this.getProperty("showBar",false);
		    fieldProps[f.getId()] = {
			isNumeric:true,
			showBar: this.getProperty(f.getId()+".showBar",showBar),
			barMin: this.getProperty(f.getId()+".barMin",0),
			barMax: this.getProperty(f.getId()+".barMax",100),
			barStyle: this.getProperty(f.getId()+".barStyle",this.getProperty("barStyle",'')),
			barLabelInside: this.getProperty(f.getId()+".barLabelInside",this.getProperty("barLabelInside")),
			barLength:this.getProperty('barLength','100px')
		    }
		}
	    });

	    let summary=[];
	    let columns;
	    let recordCnt = 0;
	    let addColumn = (td,v,field)=>{
		if(recordCnt==1) {
		    summary.push({field:field,total:0,cnt:0,min:NaN,max:NaN});
		}
		columns.push(td);
		if(!isNaN(v)) {
		    let s = summary[columns.length-1];
		    s.total+=v;
		    s.cnt++;
		    s.min = isNaN(s.min)?v:Math.min(s.min,v);
		    s.max = isNaN(s.max)?v:Math.max(s.max,v);		    
		} 
	    }
	    let cellCnt = 0;
	    let even=false;
	    records.every((record,recordIdx)=>{
		if(numRecords>-1 && recordIdx>numRecords) return false;
		even=!even;
		recordCnt++;
		let d = record.getData();
		d = d.map(v=>{
		    if(!v) return v;
		    if(v.getTime) return this.formatDate(v);
		    return v;
		});

		//		if(recordIdx>40) return true;
		let prefix = "";
		if(record.isAggregate) {
		    aggId = HU.getUniqueId("agg_")
		    aggIds.push(aggId);
		}

		let clazz =  "display-htmltable-row";
		columns = [];
		//Add the place holder for the colored rows
		if(colorRowBy && !colorFullRow) {
		    colorRowBy.forEach(c=>{
			let color =  c.colorBy.getColorFromRecord(record);
			let label = '';
			if(c.template) {
			    let template = c.template.replace(/{fieldname}/g,c.colorBy.getField().getLabel());
			    template = template.replace(/{field/g,'{'+c.colorBy.getField().getId());
			    label = this.getRecordHtml(record, null, template);
			    label = HU.div(['style',colorHeaderStyle], label);
			}
			addColumn(HU.td(['class','display-td display-htmltable-td','style','width:16px;max-width:16px;border-right:1px solid #444;background:' + color+';width:10px;'],label));
		    });
		}

		if(includeIdx) {
		    addColumn(HU.td(['width','5px'],HU.div([],"#" +(recordIdx+1))));
		}
		if(includeDate) {
		    addColumn(HU.td([],this.formatDate(record.getDate())));
		}
		this.recordMap[record.rowIndex] = record;
		this.recordMap[record.getId()] = record;
		let matchers = this.getHighlightFilterText()?this.getFilterTextMatchers():null;
		fields.forEach((f,idx)=>{
		    if(maxColumns>0 && idx>=maxColumns) return;
		    cellCnt++;
		    let value = d[f.getIndex()];
		    let svalue = String(value);
		    let sv =  this.formatFieldValue(f,record,svalue);
		    if(maxLength>0 && sv.length>maxLength && f.isString()) {
			if(!record.isAggregate) {
			    sv = HU.div([STYLE,maxHeight?HU.css('max-height',maxHeight,'overflow-y','auto'):''],sv);
			}
		    } else if(maxHeight) {
			sv = HU.div([STYLE,HU.css('max-height',maxHeight,'overflow-y','auto')],sv);
		    }
		    if(f.getType()=="image") {
			let url = record.getValue(f.getIndex());
			sv = HU.image(url,['loading','lazy',STYLE,HU.css("width","150px;")]);
		    }

		    if(idx==0 && iconField) {
			let icon = record.getValue(iconField.getIndex());
			sv = HU.image(icon,[STYLE,HU.css("max-width","50px;")]) +"&nbsp;" +v;
		    }
		    if(urlField && idx==0) {
			let url = record.getValue(urlField.getIndex());
			if(sv && Utils.stringDefined(url)) {
			    if(sv) sv = String(v).trim();
			    sv = HU.href(url,sv);
			}
		    }

		    let colorBy = colorByMap[f.getId()];
		    let color = null;
		    let foreground="#000";
		    let tdAttrs = [];
		    let props = fieldProps[f.getId()]??{};
		    if(props.isNumeric)
			tdAttrs = ["align","right"];
		    tdAttrs.push("class","display-td display-htmltable-td");		    
		    let attrs = colAttrs[f.getId()];
		    if(attrs) tdAttrs = Utils.mergeLists(tdAttrs,attrs);
		    if(svalue.trim().length==0)
			tdAttrs.push('nullvalue','true');

		    if(colorBy) {
			let color =  colorBy.getColorFromRecord(record);
			let fg =  Utils.getForegroundColor(color);
			addColumn(HU.td(Utils.mergeLists(tdAttrs, [STYLE,HU.css('background', color,'color',fg+" !important")]),sv),null,f);
		    } else if(props.showBar) {
			let percent = 1-(value-props.barMin)/(props.barMax-props.barMin);
			percent = (percent*100)+"%";
			let contents = "";
			sv = Utils.formatNumberComma(value)+"%";
			if(props.barLabelInside) {
			    contents = HU.div([STYLE,HU.css("padding-left","2px")],sv);
			    sv = "";
			}
			let bar = HU.div([CLASS,"ramadda-bar-inner", STYLE,HU.css("right",percent)+props.barStyle],contents);
			let width = props.barLength;
			let outer = HU.div([CLASS,"ramadda-bar-outer", STYLE,
					    (width?HU.css("width",HU.getDimension(width)):"")+
					    HU.css("min-width","100px")+(props.barLabelInside?HU.css("height","1.5em"):"")],bar);
			if(props.barLabelInside) {
			    addColumn(HU.td([],outer),null,f);
			} else {
			    addColumn(HU.td([],HU.row([["align","right"],sv],outer)),null,f);
			}
		    } else if(props.isNumeric) {
			let td = this.handleColumn(fields,aggByField,f,record,this.formatNumber(value,f.getId()), tdAttrs);
			addColumn(td,value,f);
		    } else {
			addColumn(this.handleColumn(fields,aggByField,f,record,sv,tdAttrs,matchers),null,f);
		    }
		    prefix="";
		});



		if(includeGeo) {
		    columns.push(HU.td([],record.getLatitude()),HU.td([],record.getLongitude()));
		}

		if(categoryField) {
		    let c = categoryField.getValue(record);
		    if(c!=category && c!='') {
			//add in the extra columns so datatables doesn't choke
			category=c;
			let extra = '';
			for(let j=0;j<columns.length-1;j++)
			    extra+=HU.td(['style','display:none;','class','display-htmltable-category-cell',],'');
			let row = HU.tr(['class','display-htmltable-category-row'], HU.td(['class','display-htmltable-category-cell','colspan',columns.length],HU.div(['class','display-htmltable-category'],c))+extra);
			html+=row;
		    }
		}		

		let rowStyle = '';
		if(colorRowBy && colorRowBy.length && colorFullRow) {
		    let color =  colorRowBy[0].colorBy.getColorFromRecord(record);
		    if(color)rowStyle+=HU.css('background',color);
		}
		if(record.isAggregate)
		    html+=HU.openTag('tr',['style',rowStyle,'aggregateRow',aggId,'style',HU.css("font-weight","550"), 'title','','valign','top','class',clazz, RECORD_ID,record.getId()]);
		else if (aggByField)
		    html+=HU.openTag('tr',['style','display:none;', "aggregateId", aggId,'title','','valign','top','class',clazz, RECORD_ID,record.getId()]);
		else
		    html+=HU.openTag('tr',['style',rowStyle,"aggregateId", aggId,'title','','valign','top','class',clazz +' display-htmltable-row-' +(even?'even':'odd'), RECORD_ID,record.getId()]);
				
		if(colorRowBy && colorRowBy.length && colorFullRow) {
		    let color =  colorRowBy[0].colorBy.getColorFromRecord(record);
		    if(color)rowStyle+=HU.css('background',color);
		}



		html+=Utils.join(columns,'');
		html+="</tr>\n";
		return true;
	    });

	    html+=HU.close('tbody');
	    html+=HU.open('tfoot');
	    html+=HU.open('tr');
	    let total = NaN;
	    if(summary && (this.getShowSummaryTotal() || this.getShowSummaryAverage()  ||
			   this.getShowSummaryMinMax())) {
		total=0;
		summary.forEach(s=>{
		    html+="<td align=right style='padding:0px;'>";
		    let ok  = (main,what)=>{
			if(s.field) {
			    if(s.field.isFieldString()) return false;
			    return this.getProperty(s.field.getId()+'.'+what,main);
			}
			return main;
		    }

		    if(s.cnt) {
			let v = [];
			if(ok(this.getShowSummaryTotal(),'showSummaryTotal')) {
			    v.push('Total: '+s.total);
			}
			if(ok(this.getShowSummaryAverage(),'showSummaryAverage')) {
			    let avg = s.total/s.cnt;
			    avg = Utils.formatNumberComma(avg,2);
			    v.push('Avg: '+ avg);
			}
			if(ok(this.getShowSummaryMinMax(),'showSummaryMinMax')) {
			    v.push('Min: '+s.min);
			    v.push('Max: '+s.max);
			}
			html+=HU.div(['style','text-align:right;'],Utils.join(v,'<br>'));
			total+=s.total;
		    }
		    html+="</td>";
		});
	    }
	    html+=HU.close('tr');
	    html+=HU.close('tfoot');
	    html+=HU.close('table');
	    html+=HU.close('div');
	    if(!isNaN(total) && this.getShowGrandSummary()) {
		html+='Total: ' + total;
	    }
	    if(this.getShowAddRow()) {
		html+=HU.div([ID,this.domId("addrow"),CLASS,"ramadda-clickable"], HU.getIconImage("fas fa-plus"));
	    }	
	    if(cellCnt==0) {
		this.setDisplayMessage('No table available for table');
		return
	    }
	    this.setContents(html);
	    aggIds.forEach(id=>{
		$("#"+ id+"_toggle").click(function() {
		    let open = $(this).attr("toggleopen")=="true";
		    $(this).attr("toggleopen",!open);
		    let row = _this.jq(ID_TABLE).find(HU.attrSelect("aggregateRow", id));
		    if(open) {
			row.find(".display-td").css("font-weight","plain").css("border-bottom","0px");
			row.css("font-weight","plain").css("border-bottom","0px");

			_this.jq(ID_TABLE).find(HU.attrSelect("aggregateId", id)).hide();
 			$(this).find("#" + id+"_toggleimage").html(HU.getIconImage("fas fa-chevron-right"));
		    } else {
			row.find(".display-td").css("font-weight","bold").css("border-bottom","1px solid #888");
			_this.jq(ID_TABLE).find(HU.attrSelect("aggregateId", id)).show();
			$(this).find("#" + id+"_toggleimage").html(HU.getIconImage("fas fa-chevron-down"));
		    }
		});
	    });
	    let dom = this.jq(ID_COLORTABLE);
	    let colorBarCnt = 0;
	    dom.html('');
	    cbs.forEach((cb,idx)=>{
		idx = (colorBarCnt++);
		let id = this.domId(ID_COLORTABLE+idx);
		dom.append(HU.div([ID,id]));
		cb.displayColorTable(null,true,ID_COLORTABLE+idx);
	    });
	    if(colorRowBy) {
		colorRowBy.forEach((cb,idx)=>{
		    if(idx>0)
			dom.append(HU.div(['style','border-bottom:1px solid #ccc']));
		    idx = (colorBarCnt++);
		    let id = this.domId(ID_COLORTABLE+idx);
		    dom.append(HU.div([ID,id]));
		    cb.colorBy.displayColorTable(null,true,ID_COLORTABLE+idx);
		});
	    }

	    let headers =  this.find(".display-table-header");
	    headers.click(function() {
		let field = fieldMap[$(this).attr("field-id")];
		if($(this).attr("sorted")==="true") {
		    _this.setProperty("sortAscending",!_this.getSortAscending());
		} 
		_this.setProperty("sortFields", field.getId());
		_this.sortByFieldChanged(field.getId());
	    });


	    let _this = this;
	    let tooltipClick = this.getProperty("tooltipClick");
	    let rows = this.jq(ID_TABLE).find(".display-htmltable-row");
	    this.makeTooltips(rows,records);
	    rows.click(function() {
		let record = _this.recordMap[$(this).attr(RECORD_ID)];
		if(!record) return;
		_this.propagateEventRecordSelection({record:record});
	    });
	    let scrollHeight=this.getScrollY("400px");
	    let opts = {
                ordering: false,
		scrollY:scrollHeight,
		paging:this.getProperty('tablePaging')
	    };

//	    console.log('HtmlTable: #cells:' +cellCnt);
	    let wrapper = this.jq(ID_TABLE+'_wrapper');
	    if(!opts.paging && cellCnt>3000) {
		wrapper.css('max-height',scrollHeight).css('height',scrollHeight).css('overflow-y','auto').css('display','flex');
		wrapper.find('th').css('top','0').css('position','sticky').css('z-index','900');
		this.jq(ID_TABLE).css('height','200px').css('overflow-y','auto').css('border-collapse','collapse');
	    } else {
		HU.formatTable("#" + this.domId(ID_TABLE), opts);
	    }

	    if(this.getShowAddRow()) {
		this.jq("addrow").click(()=>{
		    let records = this.getPointData().getRecords();
		    let newRow = records[records.length-1].clone();
		    records.push(newRow);
		    this.updateUI();
		    this.getPointData().propagateEventDataChanged(this);
		});
	    }
	    let handleChange = dom=>{
		let val;
		if(dom.attr('inputtype')=="checkbox") {
		    val = dom.is(':checked');
		} else {
		    val = dom.val();
		}
		
		let fieldId = dom.attr("fieldid");
		let recordIndex = dom.attr(RECORD_INDEX); 
		let row = _this.recordMap[recordIndex];
		let field = _this.fieldMap[fieldId];		
		row.data[field.getIndex()] = val;
		_this.getPointData().propagateEventDataChanged(_this);
	    }
	    this.jq(ID_DISPLAY_CONTENTS).find(".display-editable").change(function() {
		handleChange($(this));
	    });
	    this.jq(ID_DISPLAY_CONTENTS).find(".display-editable").keypress(function(event) {
                if (event.which == 13) {
		    handleChange($(this));
		}
	    });
//	    console.timeEnd('start2');	    
//	    console.timeEnd('start');

	}});
}

function RamaddaPolltableDisplay(displayManager, id, properties) {
    const ID_VOTE_STATS = 'votesstats';
    const ID_SHOW_VOTES = 'showvotes';
    const ID_TOGGLE_CELLS = 'togglecells';	    	    



    const SUPER =  new RamaddaHtmltableDisplay(displayManager, id, properties,'polltable');
    let myProps = [
	{label:'Poll Table'},
	{p:'pollFields',tt:'Fields to poll on'}
	]


    const TOGGLED_CSS='3em';

    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
	isPollField(field) {
	    if(!this.pollFields)
		this.pollFields = this.getPollFields('').split(',');
	    return this.pollFields.includes(field);
	},
	makeColumn:function(record,field,attrs,v) {
	    if(!this.isPollField(field.getId())) {
		return SUPER.makeColumn.call(this,record,field,attrs,v);
	    }
	    v = HU.div(['style',HU.css('overflow-y','auto','max-height',this.cellsToggled?TOGGLED_CSS:'500px'),'class','display-polltable-field','data-record-id',record.getId()],v);
	    return HU.td(attrs,v);
	},
	measureCont:function(yes,no) {
	    if(no>0 && yes>0 && yes+no>10 ) {
		let diff= Math.abs(yes-no);
		let p = diff/(yes+no);
//		console.log(yes,no," DIFF:" + diff,p,1-p);
		return 1-p;
	    }
	    return 0;
	},
	getRecords: function() {
	    let records = SUPER.getRecords.call(this);
	    if(!records) return null;
	    this.applyVotesToData(records);
	    return records;
	},
	applyVotesToData:function(records) {
    	    if(!this.votingData) {
		return;
	    } 
	    let recordToVotes = {};
	    Object.keys(this.votingData).forEach(key=>{
		[recordIndex,field] = key.split('--');
		if(!recordToVotes[recordIndex]) {
		    recordToVotes[recordIndex] = [];
		}
		recordToVotes[recordIndex].push(this.votingData[key]);
	    });
	    let upvotes,downvotes,controversial; 
	    records.forEach((record) =>{
		if(!upvotes) {
		    record.getFields().forEach(f=>{
			if(f.getId()=='upvotes') upvotes = f;
			else if(f.getId()=='downvotes') downvotes = f;
			else if(f.getId()=='controversial') controversial = f;			    
		    });
		}
		let rowIndex = record.getRowIndex();
		if(recordToVotes[rowIndex]) {
		    let yes = 0;
		    let no = 0;
		    let maxCont=0;
		    recordToVotes[rowIndex].forEach(vote=>{
			yes+=vote.yes;
			no+=vote.no;			    
			let cont = this.measureCont(vote.yes,vote.no);
			maxCont = Math.max(maxCont,cont);
		    });

		    if(upvotes)
			record.setValue(upvotes.getIndex(),yes);
		    if(downvotes)
			record.setValue(downvotes.getIndex(),no);
		    record.setValue(controversial.getIndex(),maxCont);
		}
	    });
	},
	updateUI: function() {
	    if(!this.votingData) {
		let url = ramaddaBaseUrl + "/entry/vote?entryid=" + this.getProperty("entryId", "");
		$.getJSON(url, (data) =>{
		    this.votingData = data;
		    this.updateUI();
		});
	    }
	    let _this = this;
            SUPER.updateUI.call(this);
	    if(!Utils.isDefined(this.cellsToggled)) {
		this.cellsToggled = false;
	    }
	    let buttons = HU.buttons([HU.div(['style','min-width:130px;','id',this.domId(ID_TOGGLE_CELLS)],this.cellsToggled?'Expand Cells':'Toggle Cells'),
				      HU.div(['style','min-width:130px;','id',this.domId(ID_SHOW_VOTES)],'Show Votes'),
				      HU.span(['id',this.domId(ID_VOTE_STATS),'style','display:inline-block;min-width:300px'],''),				      
				     ],'');				      
	    this.jq(ID_HEADER0).html(buttons);
	    let pollFields = this.getContents().find('.display-polltable-field');
	    this.jq(ID_TOGGLE_CELLS).button().click(()=>{
		if(_this.cellsToggled) {
		    _this.jq(ID_TOGGLE_CELLS).html('Toggle Cells');
		    pollFields.css('max-height','500px');
		} else {
		    pollFields.css('max-height',TOGGLED_CSS);
		    _this.jq(ID_TOGGLE_CELLS).html('Expand Cells');
		}
		this.cellsToggled = !this.cellsToggled
		pollFields.attr('toggled',this.cellsToggled);
	    });
	    pollFields.click(function() {
		let toggled = $(this).attr('toggled')??'false';
		let items = $(this).closest('tr').find('.display-polltable-field');
		if(toggled==='false') {
		    items.css('max-height',TOGGLED_CSS);
		    items.attr('toggled',true);
		} else {
		    items.css('max-height','500px');
		    items.attr('toggled',false);
		}
	    });

	    this.jq(ID_SHOW_VOTES).button().click(()=>{
		this.showingVotes=!this.showingVotes;
		this.applyShowVotes();
	    });
	    let entry  =this.getProperty('entryId');
	    this.applyShowVotes();
	    this.addVotes();

	},
	addVotes:function() {
	    let _this = this;
            this.jq(ID_DISPLAY_CONTENTS).find('.display-polltable-block').remove();
            this.jq(ID_DISPLAY_CONTENTS).find('.display-polltable-padding').remove();	    
            this.jq(ID_DISPLAY_CONTENTS).find('td').each(function() {
		if(!$(this).attr('field-id'))return;
		let field =$(this).attr('field-id');
		if(!_this.isPollField(field)) {
		    return
		}

		let c = $(this).html().trim();
		if(c=='' || $(this).attr('nullvalue')==='true') return
		let record =$(this).attr('record-index');		
		$(this).append(HU.div(['style','height:2em','class','display-polltable-padding']));
		let contents = HU.leftRightTable(HU.div(['field-id',field,'record-index',record,'vote','yes','class','vote ramadda-clickable','title','Up vote'],HU.getIconImage('fa-regular fa-thumbs-up')),
						 HU.div(['field-id',field,'record-index',record,'vote','no','class','vote ramadda-clickable','title','Down vote'],HU.getIconImage('fa-regular fa-thumbs-down')));
		$(this).append(HU.div(['field-id',field,'record-index',record,'class','display-polltable-block','style','border-top:1px solid #ccc;position:absolute;right:0px;left:10px;bottom:0px;'],contents));
	    });

            this.jq(ID_DISPLAY_CONTENTS).find('.vote').click(function() {
		let thumb= $(this);
		let vote =$(this).attr('vote');
		if(!Utils.isDefined(vote)) {
		    return;
		}
		let field =$(this).attr('field-id');
		let record =$(this).attr('record-index');		
		let id = _this.getProperty("entryId", "");
		let url = ramaddaBaseUrl + "/entry/vote?xreturnvotes=true&entryid=" + id;
		let key = record+'--'+field;
		url = HU.url(url,['key',key,'vote',vote]);
		$.getJSON(url, function(data) {
		    if(data.error) {
			console.log('An error occurred:' + data.error);
		    } 
		    if(!_this.canEdit()) {
			thumb.attr('vote',null);
			thumb.html('--');
		    }
		    if(_this.votingData) {
			let obj = _this.votingData[key];
			if(!obj) {
			    _this.votingData[key] = obj ={no:0,yes:0};
			}
			if(vote=='yes') obj.yes++;
			else obj.no++;
		    }
		}).fail(function(data) {
		    alert('An error occurred');
		});
	    });
	    

	},
	applyShowVotes:function () {
	    let _this = this;
	    let padding = this.jq(ID_DISPLAY_CONTENTS).find('.display-polltable-padding');
	    if(!this.showingVotes) {
		padding.css('height','2em');
		this.jq(ID_SHOW_VOTES).html('Show Votes');
		this.addVotes();
		this.jq(ID_VOTE_STATS).html('');
		return;
	    }
//	    this.jq(ID_VOTE_STATS).show();
//	    padding.css('height','4em');
	    this.jq(ID_SHOW_VOTES).html('Hide Votes');
	    let id = this.getProperty("entryId", "");
	    let url = ramaddaBaseUrl + "/entry/vote?entryid=" + id;
	    $.getJSON(url, function(data) {
		this.votingData = data;
		let blocks = _this.jq(ID_DISPLAY_CONTENTS).find('.display-polltable-block');
		let statsYes;
		let statsNo;		
		Object.keys(data).forEach(key=>{
		    let d = data[key]
		    if(Utils.isDefined(d.yes)) {
			if(!statsYes)
			    statsYes={min:d.yes,max:d.yes,total:0}
			statsYes.min = Math.min(statsYes.min, d.yes);
			statsYes.max = Math.max(statsYes.max, d.yes);			
			statsYes.total+=d.yes;
		    }
		    if(Utils.isDefined(d.no)) {
			if(!statsNo)
			    statsNo={min:d.no,max:d.no,total:0}
			statsNo.min = Math.min(statsNo.min, d.no);
			statsNo.max = Math.max(statsNo.max, d.no);			
			statsNo.total+=d.no;
		    }		    
		});
		let stats = '';
		if(statsYes) {
		    stats +='Total yes votes: ' + statsYes.total;
		    statsYes.range = statsYes.max-statsYes.min;
		}
		if(statsNo) {
		    stats +=HU.space(2)+'Total no votes: ' + statsNo.total;
		    statsNo.range = statsNo.max-statsNo.min;
		}
		stats+=HU.space(2)+'('+HU.span(['style','font-weight:bold;color:#D85F48'],'* controversial')+')';
		_this.jq(ID_VOTE_STATS).html(stats)
		let ct = Utils.getColorTable('plotly_reds',true);
		let getContColor = (yes,no,cont) =>{
		    let c;
		    if(cont>0.5) {
			let idx=Math.min(ct.length-1,Math.max(0,parseInt(cont*ct.length)))
			c=ct[idx];
		    }
		    return c;
		}
		blocks.each(function() {
		    let field =$(this).attr('field-id');
		    let record =$(this).attr('record-index');		
		    let key = record+'--'+field;
		    let voteObj = data[key];
		    let yes = 0, no = 0;
		    if(voteObj) {
			yes = voteObj.yes??yes;
			no = voteObj.no??no;				
		    } 
		    let percentYes = 0;
		    let percentNo=0;
		    if(statsYes) {
			if(statsYes.total>0)
			    percentYes = parseInt(100*(yes/statsYes.total));
			else
			    percentYes = 100
		    }
		    if(statsNo) {
			if(statsNo.total>0)
			    percentNo = parseInt(100*(no/statsNo.total));
			else
			    percentNo = 100
		    }		    
		    let label = '<table cellpadding=0 cellspacing=0 border=0 width=100%>';
		    let style = 'padding:0px !important;padding-right:4px !important;font-size:9pt;';
		    let cont = _this.measureCont(yes,no);
		    let c = getContColor(yes,no,cont);
		    if(c) {
			style+=HU.css('color',c,'font-weight','bold');
		    }
		    let icon = i=>{
			let attrs=[];
			if(c) attrs.push('style',HU.css('color',c));
			return HU.getIconImage(i,[],attrs);
		    }
		    let line = (vote,percent,i)=>{
			let title = c?'Controversial':'';
			label += HU.tr(['title',title],HU.td(['width','5px','style',style,'align','right'], icon(i))+
				       HU.td(['style',style], vote+' (' + percent+'%) ' +(c?'*':'')));
		    }
		    line(yes,percentYes,'fa-regular fa-thumbs-up');
		    line(no,percentNo,'fa-regular fa-thumbs-down');		    
		    label+='</table>'
		    $(this).html(label);
		});
	    }).fail(function(data) {
		alert('An error occurred');
	    });

	}
	

    });
}




function RamaddaTsneDisplay(displayManager, id, properties) {
    const ID_CANVAS = "tsnecanvas";
    const ID_DETAILS = "tsnedetails";
    const ID_RUN = "tsnerun";
    const ID_RESET = "tsnereset";
    const ID_STEP = "tsnestep";
    const ID_SEARCH = "tsnesearch";
    $.extend(this, {
        colorTable: "red_white_blue",
        colorByMin: "-1",
        colorByMax: "1",
        height: "500px;"
    });

    const SUPER = new RamaddaFieldsDisplay(displayManager, id, DISPLAY_TSNE, properties);
    defineDisplay(addRamaddaDisplay(this), SUPER, [], {
        nameToIndex: {},
        needsData: function() {
            return true;
        },
        handleEventPointDataLoaded: function(source, pointData) {
            if (!this.needsData()) {
                if (this.dataCollection == null) {
                    this.dataCollection = source.dataCollection;
                    this.updateUI();
                }
            }
        },
        updateUI: async function(pointData) {
            SUPER.updateUI.call(this, pointData);
            if (!this.hasData()) {
                this.setDisplayMessage(this.getLoadingMessage());
                return;
            }
            await Utils.importJS(RamaddaUtil.getCdnUrl("/lib/tsne.js"));
            //Height is the height of the overall display including the menu bar
            let height = this.getProperty("height",400);
            if (String(height).endsWith("px")) height = String(height).replace("px", "");
            height = parseInt(height);
            //            height-=30;
            let details = HU.div([STYLE, HU.css('height', height + 'px','max-height', height + "px"), CLASS, "display-tnse-details", ID, this.domId(ID_DETAILS)], "");
            let canvas = HU.div([CLASS, "display-tnse-canvas-outer", STYLE, HU.css('height', height + 'px')], HU.div([CLASS, "display-tnse-canvas", ID, this.domId(ID_CANVAS)], ""));
            let buttons = HU.div([ID, this.domId(ID_RUN), CLASS, "ramadda-button", "what", "run"], "Stop") + SPACE +
                HU.div([ID, this.domId(ID_STEP), CLASS, "ramadda-button", "what", "step"], "Step") + SPACE +
                HU.div([ID, this.domId(ID_RESET), CLASS, "ramadda-button", "what", "reset"], "Reset") + SPACE +
                HU.input("", "", [ID, this.domId(ID_SEARCH), "placeholder", "search"]);

            buttons = HU.div([CLASS, "display-tnse-toolbar"], buttons);
            this.jq(ID_TOP_LEFT).append(buttons);
            this.setContents(HU.table([WIDTH,'100%'], HU.tr(['valign','top'], HU.td(['width','80%'], canvas) + HU.td(['width','20%'], details))));
            this.search = this.jq(ID_SEARCH);
            this.search.keyup(e => {
                let v = this.search.val().trim();
                this.canvas.find(".display-tnse-mark").removeClass("display-tnse-highlight");
                if (v == "") return;
                v = v.toLowerCase();
                for (name in this.nameToIndex) {
                    if (name.toLowerCase().startsWith(v)) {
                        this.jq("element-" + this.nameToIndex[name]).addClass("display-tnse-highlight");
                    }
                }
            });
            this.details = this.jq(ID_DETAILS);
            this.reset = this.jq(ID_RESET);
            this.step = this.jq(ID_STEP);
            this.step.button().click(() => {
                this.running = false;
                this.run.html(this.running ? "Stop" : "Run");
                this.takeStep();
            });
            this.reset.button().click(() => {
                this.start();
            });
            this.run = this.jq(ID_RUN);
            this.run.button().click(() => {
                this.running = !this.running;
                if (this.running) this.takeStep();
                this.run.html(this.running ? "Stop" : "Run");
            });
            this.canvas = this.jq(ID_CANVAS);
            this.running = true;
            this.start();
        },
        start: function() {
            this.canvas.html("");
            this.haveStepped = false;
            this.dataList = this.getStandardData(null, {
                includeIndex: false
            });
            let allFields = this.dataCollection.getList()[0].getRecordFields();
            if (!this.fields) {
                this.fields = this.getSelectedFields([]);
                if (this.fields.length == 0) this.fields = allFields;
                let strings = this.getFieldsByType(this.fields, "string");
                if (strings.length > 0)
                    this.textField = strings[0];
            }
            let data = [];
            for (let rowIdx = 1; rowIdx < this.dataList.length; rowIdx++) {
                let tuple = this.getDataValues(this.dataList[rowIdx]);
                let nums = [];
                for (let i = 0; i < this.fields.length; i++) {
                    if (this.fields[i].isNumeric()){
                        let v = tuple[this.fields[i].getIndex()];
                        if(isNaN(v)) v = 0;
                        nums.push(v);
                    }
                }
                data.push(nums);
            }

            let opt = {}
            opt.epsilon = 10; // epsilon is learning rate
            opt.perplexity = 30; // how many neighbors each point influences
            opt.dim = 2; // dimensionality of the embedding (2 = default)
            this.tsne = new tsnejs.tSNE(opt);
            this.tsne.initDataRaw(data);
            this.takeStep();
        },
        takeStep: function() {
            let numSteps = 10;
            for (let step = 0; step < numSteps; step++) {
                this.tsne.step();
            }

            let pts = this.tsne.getSolution();
            let minx, miny, maxx, maxy;
            for (let i = 0; i < pts.length; i++) {
                if (i == 0) {
                    maxx = minx = pts[i][0];
                    maxy = miny = pts[i][1];
                } else {
                    maxx = Math.max(maxx, pts[i][0]);
                    minx = Math.min(minx, pts[i][0]);
                    maxy = Math.max(maxy, pts[i][1]);
                    miny = Math.min(miny, pts[i][1]);
                }
            }
            let sleep = 250;
            for (let i = 0; i < pts.length; i++) {
                let x = pts[i][0];
                let y = pts[i][1];
                let px = 100 * (x - minx) / (maxx - minx);
                let py = 100 * (y - miny) / (maxy - miny);
                if (!this.haveStepped) {
                    let title = "";
                    if (this.textField) {
                        let tuple = this.getDataValues(this.dataList[i]);
                        title = tuple[this.textField.getIndex()];
                    }
                    if (title.length > 10) {
                        title.length = 10;
                    }
                    this.nameToIndex[title] = i;
                    this.canvas.append(HU.div([TITLE, title, "index", i, ID, this.domId("element-" + i), CLASS, "display-tnse-mark", STYLE, HU.css('left', px + '%', 'top',py +'%')], title));
                } else {
                    this.jq("element-" + i).animate({
                        left: px + "%",
                        top: py + "%"
                    }, sleep, "linear");
                }
	    }
            let _this = this;
            if (!this.haveStepped) {
                this.canvas.find(".display-tnse-mark").click(function(e) {
                    let index = parseInt($(this).attr("index"));
                    if (index < 0 || index >= _this.dataList.length) return;
                    let tuple = _this.getDataValues(_this.dataList[index]);
                    let details = HU.open(TABLE,[CLASS,'formtable',WIDTH,'100%']);
                    for (let i = 0; i < _this.fields.length; i++) {
                        let field = _this.fields[i];
                        details += HU.tr([],HU.td(['align','right', CLASS,'formlabel'], this.getFieldLabel(field) + ':') + HU.td([],tuple[field.getIndex()]));
                    }
                    details += HU.close(TABLE);
                    _this.details.html(details);
                });
            }
            if (!this.haveStepped) {
                //                this.haveStepped = true;
                //                this.takeStep();
                //                return;
            }
            this.haveStepped = true;
            if (this.running)
                setTimeout(() => this.takeStep(), sleep);
        },
    });
}


function RamaddaHeatmapDisplay(displayManager, id, properties) {
    $.extend(this, {
        colorTable: "red_white_blue",
    });
    const SUPER = new RamaddaFieldsDisplay(displayManager, id, DISPLAY_HEATMAP, properties);
    defineDisplay(addRamaddaDisplay(this), SUPER, [], {
        "map-display": false,
        needsData: function() {
            return true;
        },
        getMenuItems: function(menuItems) {
            SUPER.getMenuItems.call(this, menuItems);
            let get = this.getGet();
            let tmp = HU.formTable();
            let colorTable = this.getColorTableName();
            let ct = HU.open('select',[ID,this.domId("colortable")]);
            for (table in Utils.ColorTable) {
                if (table == colorTable)
                    ct += HU.tag('option',['selected',null], table);
                else
                    ct += HU.tag('option',[], table);
            }
            ct += HU.close('select');
            tmp += HU.formEntry("Color Table:", ct);
            tmp += HU.formEntry("Color By Range:", HU.input("", this.colorByMin, ["size", "7", ATTR_ID, this.domId("colorbymin")]) + " - " +
				HU.input("", this.colorByMax, ["size", "7", ATTR_ID, this.domId("colorbymax")]));
            tmp += HU.close(TABLE);
            menuItems.push(tmp);
        },
        initDialog: function() {
            SUPER.initDialog.call(this);
            let _this = this;
            let updateFunc = function() {
                _this.colorByMin = _this.jq("colorbymin").val();
                _this.colorByMax = _this.jq("colorbymax").val();
                _this.updateUI();

            };
            let func2 = function() {
                _this.colorTable = _this.jq("colortable").val();
                _this.updateUI();

            };
            this.jq("colorbymin").blur(updateFunc);
            this.jq("colorbymax").blur(updateFunc);
            this.jq("colortable").change(func2);
        },

        handleEventPointDataLoaded: function(source, pointData) {
            if (!this.needsData()) {
                if (this.dataCollection == null) {
                    this.dataCollection = source.dataCollection;
                    this.updateUI();
                }
            }
        },
        fieldSelectionChanged: function() {
            this.updateUI();
        },
        getContentsStyle: function() {
            let height = this.getProperty("height", -1);
            if (height > 0) {
                return " height:" + height + "px; " + " max-height:" + height + "px; overflow-y: auto;";
            }
            return "";
        },
        updateUI: function(pointData) {
            let _this = this;
            if (!haveGoogleChartsLoaded()) {
                let func = function() {
                    _this.updateUI();
                }
                this.setDisplayMessage(this.getLoadingMessage());
                setTimeout(func, 1000);
                return;
            }

            SUPER.updateUI.call(this, pointData);
            if (!this.hasData()) {
                this.setDisplayMessage(this.getLoadingMessage());
                return;
            }
            let dataList = this.getStandardData(null, {
                includeIndex: true
            });
            let header = this.getDataValues(dataList[0]);
            let showIndex = this.getProperty("showIndex", true);
            let showValue = this.getProperty("showValue", true);
            let textColor = this.getProperty("textColor", "black");

            let cellHeight = this.getProperty("cellHeight", null);
            let extraTdStyle = "";
            if (this.getProperty("showBorder")) {
                extraTdStyle = HU.css("border-bottom","1px #666 solid");
            }

            let extraCellStyle = "";
            if (cellHeight)
                extraCellStyle += HU.css("height", cellHeight + "px","max-height", cellHeight + "px","min-height", cellHeight + "px");

            let allFields = this.dataCollection.getList()[0].getRecordFields();
            let fields = this.getSelectedFields([]);

            if (fields.length == 0) fields = allFields;
            let html = "";
            let colors = null;
            let colorByMin = null;
            let colorByMax = null;
            if (Utils.stringDefined(this.getProperty("colorByMins"))) {
                colorByMin = [];
                let c = this.getProperty("colorByMins").split(",");
                for (let i = 0; i < c.length; i++) {
                    colorByMin.push(parseFloat(c[i]));
                }
            }
            if (Utils.stringDefined(this.getProperty("colorByMaxes"))) {
                colorByMax = [];
                let c = this.getProperty("colorByMaxes").split(",");
                for (let i = 0; i < c.length; i++) {
                    colorByMax.push(parseFloat(c[i]));
                }
            }

            if (Utils.stringDefined(this.getProperty("colorTables"))) {
                let c = this.getProperty("colorTables").split(",");
                colors = [];
                for (let i = 0; i < c.length; i++) {
                    let name = c[i];
                    if (name == "none") {
                        colors.push(null);
                        continue;
                    }
                    let ct = Utils.getColorTable(name, true);
                    //                        console.log("ct:" + name +" " +(ct!=null));
                    colors.push(ct);
                }
            } else {
                colors = [this.getColorTable(true)];
            }
            let mins = null;
            let maxs = null;
            for (let rowIdx = 1; rowIdx < dataList.length; rowIdx++) {
                let row = this.getDataValues(dataList[rowIdx]);
                if (mins == null) {
                    mins = [];
                    maxs = [];
                    for (let colIdx = 1; colIdx < row.length; colIdx++) {
                        mins.push(Number.MAX_VALUE);
                        maxs.push(Number.MIN_VALUE);
                    }
                }

                for (let colIdx = 0; colIdx < fields.length; colIdx++) {
                    let field = fields[colIdx];
                    //Add one to the field index to account for the main time index
                    let index = field.getIndex() + 1;
                    if (!field || !field.isFieldNumeric() || field.isFieldGeo()) continue;

                    let value = row[index];
                    if (value == Number.POSITIVE_INFINITY || isNaN(value) || !Utils.isNumber(value) || !Utils.isDefined(value) || value == null) {
                        continue;
                    }
                    mins[colIdx] = Math.min(mins[colIdx], value);
                    maxs[colIdx] = Math.max(maxs[colIdx], value);
                }
            }

            html += HU.open(TABLE, ["border", "0", CLASS, "display-heatmap"]);
            html += HU.open(TR,[VALIGN,'bottom']);
            if (showIndex) {
                html += HU.td([ALIGN,'center'], HU.div([CLASS, "display-heatmap-heading-top"], header[0]));
            }
            for (let fieldIdx = 0; fieldIdx < fields.length; fieldIdx++) {
                let field = fields[fieldIdx];
                if ((!field.isFieldNumeric() || field.isFieldGeo())) continue;
                html += HU.td([ALIGN,'center'], HU.div([CLASS, "display-heatmap-heading-top"], this.getFieldLabel(field)));
            }
            html += HU.close(TR);

            for (let rowIdx = 1; rowIdx < dataList.length; rowIdx++) {
                let row = this.getDataValues(dataList[rowIdx]);
                let index = row[0];
                //check if its a date
                if (index.f) {
                    index = index.f;
                }
                let rowLabel = index;
                html += HU.open('tr',['valign','center']);
                if (showIndex) {
                    html += HU.td([CLASS, "display-heatmap-heading-side", STYLE, extraCellStyle + extraTdStyle], rowLabel);
                }
                let colCnt = 0;
                for (let colIdx = 0; colIdx < fields.length; colIdx++) {
                    let field = fields[colIdx];
                    //Add one to the field index to account for the main time index
                    let index = field.getIndex() + 1;
                    if (!field || !field.isFieldNumeric() || field.isFieldGeo()) continue;
                    let style = "";
                    let value = row[index];
                    let min = mins[colIdx];
                    let max = maxs[colIdx];
                    if (colorByMin && colCnt < colorByMin.length)
                        min = colorByMin[colCnt];
                    if (colorByMax && colCnt < colorByMax.length)
                        max = colorByMax[colCnt];

                    let ok = min != max && !(value == Number.POSITIVE_INFINITY || isNaN(value) || !Utils.isNumber(value) || !Utils.isDefined(value) || value == null);
                    let title = header[0] + ": " + rowLabel + " - " + this.getFieldLabel(field) + ": " + value;
                    if (ok && colors != null) {
                        let ct = colors[Math.min(colCnt, colors.length - 1)];
                        if (ct) {
                            let percent = (value - min) / (max - min);
                            let ctIndex = parseInt(percent * ct.length);
                            if (ctIndex >= ct.length) ctIndex = ct.length - 1;
                            else if (ctIndex < 0) ctIndex = 0;
                            style = "background-color:" + ct[ctIndex] + ";";
                        }
                    }
                    let number;
                    if (!ok) {
                        number = "-";
                    } else {
                        number = Utils.formatNumber(value)
                    }
                    if (!showValue) number = ""; 
                    html += HU.td(["valign", "center", "align", "right", STYLE, style + extraCellStyle + extraTdStyle, CLASS, "display-heatmap-cell"], HU.div([TITLE, title, STYLE, extraCellStyle + HU.css('color',textColor)], number));
                    colCnt++;
                }
                html += HU.close(TR);
            }
            html += HU.close(TABLE);
            this.setContents(html);
            this.initTooltip();

        },
    });
}




function RamaddaRankingDisplay(displayManager, id, properties) {
    const ID_TABLE = "rankingtable";
    $.extend(this, {
	height: "500px",
        sortAscending:false,
    });
    if(properties.sortAscending) this.sortAscending = "true" == properties.sortAscending;
    const SUPER = new RamaddaFieldsDisplay(displayManager, id, DISPLAY_RANKING, properties);
    let myProps = [
	{label:'Ranking'},
	{p:'sortField',ex:''},
	{p:'nameFields',ex:''},
    ];
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
        needsData: function() {
            return true;
        },
        handleEventPointDataLoaded: function(source, pointData) {
            if (!this.needsData()) {
                if (this.dataCollection == null) {
                    this.dataCollection = source.dataCollection;
                    this.updateUI();
                }
            }
        },
        fieldSelectionChanged: function() {
            this.updateUI();
        },
        updateUI: function(pointData) {
            SUPER.updateUI.call(this, pointData);
            let records = this.records =  this.filterData();
            if (records == null) {
                this.setDisplayMessage(this.getLoadingMessage());
                return;
            }
	    this.idToRecord = {};
	    records.forEach(record=>{
		this.idToRecord[record.getId()] = record;
	    });

            let allFields = this.dataCollection.getList()[0].getRecordFields();
            let fields = this.getSelectedFields([]);
            if (fields.length == 0) fields = allFields;
            let numericFields = this.getFieldsByType(fields, "numeric");
            let sortField = this.getFieldById(numericFields, this.getProperty("sortField","",true));
            if (numericFields.length == 0) {
                this.setContents("No fields specified");
                return;
            }
            if (!sortField) {
                sortField = numericFields[0];
            }
            if (!sortField) {
                this.setDisplayMessage("No fields specified");
                return;
            }

            let stringFields = this.getFieldsByIds(allFields, this.getProperty("nameFields","",true));
            if(stringFields.length==0) {
		let tmp = this.getFieldById(allFields, this.getProperty("nameField","",true));
		if(tmp) stringFields.push(tmp);
	    }
            if(stringFields.length==0) {
                let stringField = this.getFieldByType(allFields, "string");
		if(stringField) stringFields.push(stringField);
	    }
            let menu = HU.open("select",[CLASS,'ramadda-pulldown',ID, this.domId("sortfields")]);
            for (let i = 0; i < numericFields.length; i++) {
                let field = numericFields[i];
                let extra = "";
                if (field.getId() == sortField.getId()) extra = " selected ";
                menu += HU.tag('option',['value', field.getId(), extra,null], this.getFieldLabel(field));
            }
            menu += HU.close('select');
	    let top ="";
	    top += HU.span([ID,this.domId("sort")], HU.getIconImage(this.sortAscending?"fa-sort-up":"fa-sort-down", [STYLE,HU.css('cursor','pointer'),TITLE,"Change sort order"]));
            if (this.getProperty("showRankingMenu", true)) {
                top+= " " + HU.div([STYLE,HU.css('display','inline-block'), CLASS,"display-filterby"],menu);
            }
	    this.jq(ID_TOP_LEFT).html(top);
	    this.jq("sort").click(()=>{
		this.sortAscending= !this.sortAscending;
		if(this.sortAscending) 
		    this.jq("sort").html(HU.getIconImage("fa-sort-up", [STYLE,HU.css('cursor','pointer')]));
		else
		    this.jq("sort").html(HU.getIconImage("fa-sort-down", [STYLE,HU.css('cursor','pointer')]));
		this.updateUI();
	    });
            let html = "";
            html += HU.open(DIV, [STYLE, HU.css('max-height','100%','overflow-y','auto')]);
            html += HU.open(TABLE, [ID, this.domId(ID_TABLE)]);
            let tmp = records;
	    let includeNaN = this.getProperty("includeNaN",false);
	    if(!includeNaN) {
		let tmp2 = [];
		tmp.map(r=>{
		    let v = sortField.getValue(r);
		    if(!isNaN(v)) tmp2.push(r);
		});
		tmp = tmp2;
	    }
            let cnt = 0;
	    let highlight = this.getFilterHighlight();
	    let sorter = (a,b)=>{
		let r1 = a;
		let r2 = b;
		let h1 = r1.isHighlight(this);
		let h2 = r2.isHighlight(this);
		if(highlight) {
		    if(h1 && !h2) return 1;
		    if(!h1 && h2) return -1;
		}
                let t1 = this.getDataValues(a);
                let t2 = this.getDataValues(b);
                let v1 = t1[sortField.getIndex()];
                let v2 = t2[sortField.getIndex()];
                if (v1 < v2) return -1;
                if (v1 > v2) return 1;
                return 0;
	    };
            tmp.sort((a, b) => {
		let v = sorter(a,b);
		if(v==0) return 0;
		if(this.sortAscending) return v;
		return  -v;
            });


            for (let rowIdx = 0; rowIdx < tmp.length; rowIdx++) {
                let record = tmp[rowIdx];
                let tuple = this.getDataValues(record);
                let label = "";
                stringFields.map(f=>{
		    label += f.getValue(record)+" ";
		});
                label = label.trim();
		value = sortField.getValue(record);
                if (isNaN(value) || value === null) {
		    if(!includeNaN) continue;
		    value = "NA";
		} else {
		    value = this.formatNumber(value);
		}
		html += HU.tr([VALIGN,'top',CLASS,'display-ranking-row','what',record.getId()],
			      HU.td([],'#' + (rowIdx + 1)) + HU.td([],SPACE + label) +HU.td([ALIGN,'right'], SPACE +
											    value));
            }
            html += HU.close(TABLE);
            html += HU.close(DIV);
            this.setContents(html);
            let _this = this;
            this.jq(ID_TABLE).find(".display-ranking-row").click(function(e) {
		let record = _this.idToRecord[$(this).attr("what")];
		_this.propagateEventRecordSelection({record:record});
            });
	    HU.initSelect(this.jq("sortfields"));
            this.jq("sortfields").change(function() {
                _this.setProperty("sortField", $(this).val());
                _this.updateUI();
            });
        },
    });
}


function RamaddaWaffleDisplay(displayManager, id, properties) {
    const ID_TABLE = "rankingtable";
    const SUPER = new RamaddaFieldsDisplay(displayManager, id, DISPLAY_WAFFLE, properties);
    let myProps = [
	{label:'Waffle'},
	{p:'labelField',ex:''},
	{p:'boxSize',d:'15px'},
	{p:'showFieldLabel',d:true},
    ];
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
        needsData: function() {
            return true;
        },
        updateUI: function(pointData) {
            SUPER.updateUI.call(this, pointData);
            let records = this.records =  this.filterData();
            if (records == null) {
                this.setDisplayMessage(this.getLoadingMessage());
                return;
            }
            let allFields = this.dataCollection.getList()[0].getRecordFields();
            let fields = this.getSelectedFields([]);
	    let labelField = this.getFieldById(null, this.getProperty("labelField"));
	    if(!labelField) {
		let strings = this.getFieldsByType(null, "string");
		if(strings.length>0) labelField = strings[0];
	    }
	    let html = '';
	    let showFieldLabel = this.getShowFieldLabel();
	    let colors =  Utils.split(this.getColors('#F7931F,#4CC1EF,#C13018,#A6C68E,#89A7DE'),',',true,true);
	    fields.forEach((field,idx)=>{
		if(idx>0) html+='<p>';
		if(showFieldLabel)
		    html+=HU.b(field.getLabel())+'<br>';
		records.forEach((r,idx)=>{
		    let label  = labelField?r.getValue(labelField.getIndex()):index;
		    let percent  = parseInt(field.getValue(r)*100);
		    let box = "<table cellpadding=0 cellspacing=0 style='border:1px solid #ccc;border-collapse: collapse;'>";
		    
		    let percentCnt = 0;
		    let size= HU.getDimension(this.getBoxSize());
		    let color = colors[idx%colors.length];
		    for(let row=0;row<10;row++) {
			box+='<tr>';
			for(let col=0;col<10;col++) {
			    percentCnt++;
			    let style = 'width:'+ size+';height:'+ size+';border-width:1px;border-left-width:0px;';
			    let boxPercent = 100-percentCnt;
			    if(boxPercent<percent) style+=HU.css('background', color);
			    box+=HU.td(HU.td(['style',style],''));
			}
			box+='</tr>';
		    }
		    box+='</table>';
		    html+=HU.div(['style',HU.css('display','inline-block','margin-right','20px')],
				 HU.b(label+ ' ' +percent+'%') +'<br>' +box);
		});

	    });
            this.setContents(html);
	}
    });
}



function RamaddaCrosstabDisplay(displayManager, id, properties) {
    const ID_TABLE = "crosstab";
    const SUPER = new RamaddaFieldsDisplay(displayManager, id, DISPLAY_CROSSTAB, properties);
    defineDisplay(addRamaddaDisplay(this), SUPER, [], {
        needsData: function() {
            return true;
        },
        handleEventPointDataLoaded: function(source, pointData) {
            if (!this.needsData()) {
                if (this.dataCollection == null) {
                    this.dataCollection = source.dataCollection;
                    this.updateUI();
                }
            }
        },
        fieldSelectionChanged: function() {
            this.updateUI();
        },
        updateUI: function(pointData) {
            SUPER.updateUI.call(this, pointData);
            if (!this.hasData()) {
                this.setDisplayMessage(this.getLoadingMessage());
                return;
            }
            let allFields = this.dataCollection.getList()[0].getRecordFields();
	    let enums = [];
	    allFields.map(field=>{
		let label = this.getFieldLabel(field);
		if(label.length>30) label = label.substring(0,29);
		enums.push([field.getId(),label]);
	    });
	    let select = HU.span([CLASS,"display-filterby"],
				 "Display: " + HU.select("",[STYLE,"", ID,this.domId("crosstabselect")],enums,
							 this.getProperty("column", "", true)));


            this.setContents(select+HU.div([ID,this.domId(ID_TABLE)]));
	    let _this = this;
	    this.jq("crosstabselect").change(function() {
		_this.setProperty("column", $(this).val());
		_this.makeTable();
	    });
	    this.makeTable();
	},
	makeTable: function() {
            let dataList = this.getStandardData(null, {
                includeIndex: false
            });
            let allFields = this.dataCollection.getList()[0].getRecordFields();
	    let col =  this.getFieldById(null, this.getProperty("column", "", true));
	    let rows =  this.getFieldsByIds(null, this.getProperty("rows", null, true));
	    if(!col) col  = allFields[0];
	    if(rows.length==0) rows  = allFields;

            let html = HU.open(TABLE, ["border", "1px", "bordercolor", "#ccc", CLASS, "display-crosstab", "cellspacing", "1", "cellpadding", "2"]);
	    let total = dataList.length-1;
	    let cnt =0;
	    rows.map((row)=>{
		if(row.getId()==col.getId()) return;
		cnt++;
		let colValues = [];
		let rowValues = [];
		let count ={};
		let rowcount ={};
		let colcount ={};
		for (let rowIdx = 1; rowIdx < dataList.length; rowIdx++) {
		    let tuple = this.getDataValues(dataList[rowIdx]);
		    let colValue = (""+tuple[col.getIndex()]).trim();
		    let rowValue = (""+tuple[row.getIndex()]).trim();
		    let key = colValue+"--" + rowValue;
		    if(colValues.indexOf(colValue)<0) colValues.push(colValue);
		    if(rowValues.indexOf(rowValue)<0) rowValues.push(rowValue);
		    if (!(rowValue in rowcount)) {
			rowcount[rowValue] = 0;
		    }
		    rowcount[rowValue]++;
		    if (!(key in count)) {
			count[key] = 0;
		    }
		    count[key]++;
		}
		colValues.sort();
		rowValues.sort();
		if(cnt==1)
		    html+=HU.tr([],HU.td()+ HU.td([ALIGN,'center',CLASS,'display-crosstab-header','colspan',colValues.length], col.getLabel()) +HU.td([],SPACE));
		html+=HU.open(TR,[VALIGN,'bottom',CLASS,'display-crosstab-header-row'],HU.td([CLASS,'display-crosstab-header'],row.getLabel()));
		for(let j=0;j<colValues.length;j++) {
		    let colValue = colValues[j];
		    html+=HU.td([],(colValue==""?"&lt;blank&gt;":colValue));
		}
		html+=HU.td([],HU.b('Total'));
		html+=HU.close(TR);
		for(let i=0;i<rowValues.length;i++) {
		    let rowValue = rowValues[i];
		    html+=HU.open(TR);
		    html+=HU.td([], (rowValue==""?"&lt;blank&gt;":rowValue));
		    for(let j=0;j<colValues.length;j++) {
			let colValue = colValues[j];
			let key = colValue+"--" + rowValue;
			if(Utils.isDefined(count[key])) {
			    let perc = Math.round(count[key]/total*100) +"%";
			    html+=HU.td([ALIGN,'right'], count[key] +"&nbsp;(" + perc+")");
			} else {
			    html+=HU.td([],SPACE);
			}
		    }
		    let perc = Math.round(rowcount[rowValue]/total*100) +"%";
		    html+=HU.td([ALIGN,'right'], rowcount[rowValue] +SPACE +'(' + perc+')');
		    html+=HU.close(TR);
		}
	    });
            html += HU.close(TABLE);
	    this.jq(ID_TABLE).html(html);
        },
    });
}




function RamaddaCorrelationDisplay(displayManager, id, properties) {
    const ID_SLIDER_LOW = "sliderlow";
    const ID_SLIDER_LOW_MIN = "sliderlowmin";
    const ID_SLIDER_LOW_MAX = "sliderlowmax"
    const ID_SLIDER_HIGH = "sliderhigh";
    const ID_SLIDER_HIGH_MIN = "sliderhighmin";
    const ID_SLIDER_HIGH_MAX = "sliderhighmax"    
    const ID_TABLE = "table";
    const ID_LASTROW = "lastrow";
    $.extend(this, {
        colorTable: "red_white_blue",
        colorByMin: "-1",
        colorByMax: "1",
    });

    const SUPER = new RamaddaFieldsDisplay(displayManager, id, DISPLAY_CORRELATION, properties);
    let myProps = [
	{label:'Correlation'},
	{p:'showSelectSlider',ex:'false',d:true},
	{p:'showDownload',ex:true,d:false},
	{p:'range.low.min',ex:'-1'},
	{p:'range.low.max',ex:'0'},
	{p:'range.high.min',ex:'0'},
	{p:'range.high.max',ex:'1'},
	{p:'short',ex:'true',tt:'Abbreviated display'},
	{p:'showValue',ex:'false',tt:'Show the values'},
	{p:'stringsOk',ex:'true',tt:'Show string values'},	
	{p:'useId ',ex:' true',tt:'Use field id instead of label'},
	{p:'useIdTop',ex:'true',tt:'Use field id for top header'},
	{p:'useIdSide ',ex:'true',tt:'Use field id for side header'},
	{p:'labelStyle',ex:'',tt:'CSS style for labels'},
	{p:'sideHeadingStyle'}

    ];
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
        "map-display": false,
        needsData: function() {
            return true;
        },


        getMenuItems: function(menuItems) {
            SUPER.getMenuItems.call(this, menuItems);
            let get = this.getGet();
            let tmp = HU.formTable();
            let colorTable = this.getColorTableName();
            let ct = HU.open('select',[ID,this.domId("colortable")]);
            for (table in Utils.ColorTables) {
                if (table == colorTable)
                    ct += HU.tag('option',['selected',null],table);
                else
                    ct += HU.tag('option',[], table);
            }
            ct += HU.close('select');
            tmp += HU.formEntry("Color Bar:", ct);
            tmp += HU.formEntry("Color By Range:", HU.input("", this.colorByMin, ["size", "7", ATTR_ID, this.domId("colorbymin")]) + " - " +
				HU.input("", this.colorByMax, ["size", "7", ATTR_ID, this.domId("colorbymax")]));
            tmp += HU.close(TABLE);
            menuItems.push(tmp);
        },
        initDialog: function() {
            SUPER.initDialog.call(this);
            let _this = this;
            let updateFunc = function() {
                _this.colorByMin = _this.jq("colorbymin").val();
                _this.colorByMax = _this.jq("colorbymax").val();
                _this.updateUI();

            };
            let func2 = function() {
                _this.colorTable = _this.jq("colortable").val();
                _this.updateUI();

            };
            this.jq("colorbymin").blur(updateFunc);
            this.jq("colorbymax").blur(updateFunc);
            this.jq("colortable").change(func2);
        },

        handleEventPointDataLoaded: function(source, pointData) {
            if (!this.needsData()) {
                if (this.dataCollection == null) {
                    this.dataCollection = source.dataCollection;
                    this.updateUI();
                }
            }
        },
        fieldSelectionChanged: function() {
            this.updateUI();
        },
        updateUI: function(pointData) {
            SUPER.updateUI.call(this, pointData);
            if (!this.hasData()) {
                this.setDisplayMessage(this.getLoadingMessage());
                return;
            }
	    let _this  = this;
	    let html = "";
	    if(this.getProperty("showDownload",false)) {
		html+=HU.div(['id',this.domId('download')],'Download Correlation Table');
	    }
	    this.range = {
		low:{
		    min:this.getProperty("range.low.min",-1),
		    max:this.getProperty("range.low.max",0)
		},
		high: {
		    min:this.getProperty("range.high.min",0),
		    max:this.getProperty("range.high.max",1)
		}
	    }
	    if(this.getShowSelectSlider()) {
		let lowSlider = HU.div([STYLE,HU.css('display','inline-block')],"Low Range" + HU.tag(BR) + 
				       HU.div([ID,this.gid(ID_SLIDER_LOW_MIN),STYLE,HU.css(WIDTH,'50px','display','inline-block','text-align','right','margin-right','15px')],this.range.low.min) +
				       HU.div([STYLE,HU.css(HEIGHT,'20px','display','inline-block',WIDTH,'200px','background',this.getProperty('lowSliderBackground','#FD9596')), ID,this.gid(ID_SLIDER_LOW)]) +
				       HU.div([ID,this.gid(ID_SLIDER_LOW_MAX),STYLE,HU.css('text-align','left','width','50px','display','inline-block','margin-left','15px')],this.range.low.max));
		let highSlider = HU.div(["display","inline-block;"], "High Range" + HU.tag(BR) + 
					HU.div([ID,this.gid(ID_SLIDER_HIGH_MIN),STYLE,HU.css('width','50px','display','inline-block','text-align','right', 'margin-right','15px')],this.range.high.min) +
					HU.div([STYLE,HU.css(HEIGHT,'20px','display','inline-block','width','200px','background',this.getProperty('highSliderBackground','#64A982')), ID,this.gid(ID_SLIDER_HIGH)]) +
					HU.div([ID,this.gid(ID_SLIDER_HIGH_MAX),STYLE,HU.css('text-align','left','width','50px','display','inline-block','margin-left','15px')],this.range.high.max));


		html +=HU.center(HU.hrow(lowSlider, highSlider));
	    }
	    html +=HU.div([ID,this.domId(ID_TABLE)]);
            this.setContents(html);
	    this.jq('download').button().click(()=>{
		let c='';
		this.csv.forEach(row=>{
		    c+=Utils.join(row,',');
		    c+='\n';
		});
		Utils.makeDownloadFile(this.getProperty('downloadFile','correlation.csv'),c);
	    });
	    this.makeTable();
	    if(this.getShowSelectSlider()) {
		this.jq(ID_SLIDER_LOW).slider({
		    range: true,
		    min: 0,
		    max: 100,
		    values:[(this.range.low.min+1)*100,(this.range.low.max+1)*100],
		    slide: function( event, ui ) {
			let v1 = -1+ui.values[0]/100;
			let v2 = -1+ui.values[1]/100;
			let s1 = v1==-1?-1:number_format(v1,3);
			let s2 = v2==0?0:number_format(v2,3);
			_this.jq(ID_SLIDER_LOW_MIN).html(s1);
			_this.jq(ID_SLIDER_LOW_MAX).html(s2);
		    },
		    stop: function(event,ui) {
			_this.range.low.min =   -1+2*ui.values[0]/100;
			_this.range.low.max  = -1+2*ui.values[1]/100;
			_this.makeTable();
		    }
		});
		this.jq(ID_SLIDER_HIGH).slider({
		    range: true,
		    min: 0,
		    max: 100,
		    values:[this.range.high.min*100,this.range.high.max*100],
		    slide: function( event, ui ) {
			let v1 = ui.values[0]/100;
			let v2 = ui.values[1]/100;
			_this.jq(ID_SLIDER_HIGH_MIN).html(v1==0?0:number_format(v1,3));
			_this.jq(ID_SLIDER_HIGH_MAX).html(v2==1?1:number_format(v2,3));

		    },
		    stop: function(event,ui) {
			_this.range.high.min =  ui.values[0]/100;
			_this.range.high.max  = ui.values[1]/100;
			_this.makeTable();
		    }
		});


		
	    }
            this.initTooltip();
            this.displayManager.propagateEventRecordSelection(this,
							      this.dataCollection.getList()[0], {
								  index: 0
							      });
        },
	getCellLabel(row,col) {
	    return   this.getProperty('label.' + row.getId() +'.' +
				      col.getId());
	},
	makeTable: function() {
            let dataList = this.getStandardData(null, {
                includeIndex: false
            });
            let allFields = this.dataCollection.getList()[0].getRecordFields();
            let fields = this.getSelectedFields([]);
            if (fields.length == 0) fields = allFields;
	    let stringsOk = this.getStringsOk();
	    fields = fields.filter(field=>{
		if (field.isFieldGeo()) return false;
		if(!stringsOk && !field.isFieldNumeric())  return false;
		return true;
	    });
            let fieldCnt = fields.length;
	    let html = "";
	    if(fields.length>8) {
		html+=HU.openTag("div",[STYLE,HU.css('font-size','75%')]);
	    }
	    this.csv =[];
            html += HU.open(TABLE, ["cellspacing","0","cellpadding", "0", "border", "0", CLASS, "display-correlation", "width", "100%"]);
            let col1Width = 10 + "%";
            let width = 90 / fieldCnt + "%";
            html += HU.open(TR,["valign","bottom"]) + HU.td([CLASS,"display-heading","width", col1Width],SPACE);

            let short = this.getProperty("short", false);
            let showValue = this.getProperty("showValue", !short);
            let useId = this.getProperty("useId", false);
            let useIdTop = this.getProperty("useIdTop", useId);
            let useIdSide = this.getProperty("useIdSide", useId);
	    let labelStyle = this.getProperty("labelStyle","");
	    let row;
	    this.csv.push(row=[]);
	    fields.forEach(field=>{
                let label = useIdTop ? field.getId() : this.getFieldLabel(field);
		row.push(label);
                if (short) label = "";
		label = label.replace(/\/ +/g,"/").replace(/ +\//g,"/");
		label = HU.div(['class','display-correlation-header',STYLE,labelStyle], label);

                html += HU.td(["colfield", field.getId(), "align","center","width",width],
			      HU.div([CLASS, "display-correlation-heading display-correlation-heading-top"], label));
            });
            html += HU.close(TR);
	    if(!this.getProperty("colorTable"))
		this.setProperty("colorTable","red_white_green");
	    let colors =  this.getColorTable(true);
            colorByMin = parseFloat(this.colorByMin);
            colorByMax = parseFloat(this.colorByMax);
	    if(colors) colors =  this.addAlpha(colors,0.5);

	    let sideHeadingStyle=this.getSideHeadingStyle('');
	    let stringMap = {};
	    fields.forEach(field=>{
		if(!field.isNumeric()) {
		    stringMap[field.getId()] = {
			count:0,
			seen:{}
		    };
		}
	    });
	    let getValue = (field,value) =>{
		if(!field.isNumeric()) {
		    let map =  stringMap[field.getId()];
		    if(!Utils.isDefined(map.seen[value])) {
			map.seen[value]  = map.count++;
		    }
		    return  map.seen[value];
		}
		return value;
	    }


            for (let fieldIdx1 = 0; fieldIdx1 < fields.length; fieldIdx1++) {
		this.csv.push(row=[]);
                let field1 = fields[fieldIdx1];
                let label = useIdSide ? field1.getId() : this.getFieldLabel(field1);
		row.push(label);
		label.replace(/ /g, SPACE);
		label = HU.div(['class','display-correlation-header',STYLE,labelStyle], label);
                html += HU.open(TR, ["valign","center"]);
		html += HU.td(["rowfield",field1.getId(),CLASS, "display-correlation-heading"],  HU.div(['style',sideHeadingStyle,CLASS, "display-correlation-heading-side"], label));
                let rowName = this.getFieldLabel(field1);
                for (let fieldIdx2 = 0; fieldIdx2 < fields.length; fieldIdx2++) {
                    let field2 = fields[fieldIdx2];
                    let colName = this.getFieldLabel(field2);
                    let t1 = 0;
                    let t2 = 0;
                    let cnt = 0;
                    for (let rowIdx = 1; rowIdx < dataList.length; rowIdx++) {
                        let tuple = this.getDataValues(dataList[rowIdx]);
                        let v1 = getValue(field1,tuple[field1.getIndex()]);
                        let v2 = getValue(field2,tuple[field2.getIndex()]);
			if(isNaN(v1) || isNaN(v2)) {
			    continue;
			}
                        t1 += v1;
                        t2 += v2;
                        cnt++;
                    }
                    let avg1 = t1 / cnt;
                    let avg2 = t2 / cnt;
                    let sum1 = 0;
                    let sum2 = 0;
                    let sum3 = 0;
                    for (let rowIdx = 1; rowIdx < dataList.length; rowIdx++) {
                        let tuple = this.getDataValues(dataList[rowIdx]);
                        let v1 = getValue(field1,tuple[field1.getIndex()]);
                        let v2 = getValue(field2,tuple[field2.getIndex()]);			
			if(isNaN(v1) || isNaN(v2)) continue;
                        sum1 += (v1 - avg1) * (v2 - avg2);
                        sum2 += (v1 - avg1) * (v1 - avg1);
                        sum3 += (v2 - avg2) * (v2 - avg2);
                    }
                    r = sum1 / Math.sqrt(sum2 * sum3);
		    let ok = r<0?
			(r>=this.range.low.min && r<=this.range.low.max):
			(r>=this.range.high.min && r<=this.range.high.max);
                    let style = "";
    		    
		    
		    if (ok && colors != null) {
			if(fieldIdx1!=fieldIdx2) {
                            let percent = (r - colorByMin) / (colorByMax - colorByMin);
                            let index = parseInt(percent * colors.length);
                            if (index >= colors.length) index = colors.length - 1;
                            else if (index < 0) index = 0;
                            style = HU.css("background-color", colors[index]);
			} else {
                            style = HU.css("background-color", "#eee");
			    let svg = "<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'><rect width='10' height='10' fill='#fff'/><path d='M-1,1 l2,-2 M0,10 l10,-10 M9,11 l2,-2' stroke='#ddd' stroke-width='1'/></svg>";
			    style = HU.css('background-image',
					   'url(\'data:image/svg+xml;base64,' +btoa(svg) +'\')','background-repeat','repeat');
			}
                    }
                    let value = r.toFixed(3);
                    let label = value;
		    row.push(value);
		    if(fieldIdx1==fieldIdx2) label = SPACE;
                    if (!showValue || short) label = SPACE;
		    let align = "right";
		    let cellLabel =  this.getCellLabel(field1, field2);
		    if(cellLabel) {
			label = HU.span(['class','display-correlation-celllabel'],cellLabel);
			align="left";
		    }

		    let cellContents = "";
		    if(ok) {
			cellContents = HU.div([CLASS, "display-correlation-element", TITLE, "&rho;(" + rowName + "," + colName + ") = " + value], label);
		    }
                    html += HU.td(["colfield", field2.getId(), "rowfield",field1.getId(), CLASS,"display-correlation-cell","align", align, STYLE,style], cellContents);
                }
                html += HU.close(TR);
            }
            html += HU.tr([],HU.td([]) + HU.td(['colspan',(fieldCnt + 1)], HU.div([ID, this.domId(ID_LASTROW)], "")));
            html += HU.close(TABLE);
	    this.jq(ID_TABLE).html(html);
	    let _this = this;
	    let selectedRow;
	    let selectedCol;
	    this.jq(ID_TABLE).find("td").click(function(event) {
		let rowField = _this.getFieldById(null, $(this).attr("rowfield"));
		let colField = _this.getFieldById(null, $(this).attr("colfield"));
		if(event.shiftKey && _this.canEdit()) {
		    let label = prompt("Label:",  _this.getCellLabel(rowField, colField));
		    if(label) {
			let msg = 'label.' +rowField.getId() +'.' + colField.getId()+'=\"' + label +'\"';
			console.log('Copied to clipboard:');
			console.log(msg);
			Utils.copyToClipboard(msg);
		    }
		}

		let tds = _this.jq(ID_TABLE).find("td");
		if(rowField) {
		    tds.removeClass("display-correlation-row-cell-highlight");
		    if(rowField != selectedRow) {
			selectedRow = rowField;
			_this.jq(ID_TABLE).find(".display-correlation-cell[rowfield='" + rowField.getId()+"']").addClass("display-correlation-row-cell-highlight");
		    }  else {
			selectedRow = null;
		    }

		}
		if(colField) {
		    tds.removeClass("display-correlation-col-cell-highlight");
		    if(colField != selectedCol) {
			selectedCol = colField;
			_this.jq(ID_TABLE).find(".display-correlation-cell[colfield='" + colField.getId()+"']").addClass("display-correlation-col-cell-highlight");
		    }  else {
			selectedCol = null;
		    }



		}

	    });
	    this.displayColorTable(colors, ID_LASTROW, colorByMin, colorByMax);
	}
    });
}








function RamaddaRecordsDisplay(displayManager, id, properties, type) {
    const SUPER = new RamaddaFieldsDisplay(displayManager, id, DISPLAY_RECORDS, properties);
    let myProps = [
	{label:'Records'},
	{p:'maxHeight',ex:'400px'},
	{p:'showCards',ex:'true',d:true}
    ];
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {

        needsData: function() {
            return true;
        },
        handleEventPointDataLoaded: function(source, pointData) {
            if (!this.needsData()) {
                if (this.dataCollection == null) {
                    this.dataCollection = source.dataCollection;
                    this.updateUI();
                }
            }
        },
        getFieldsToSelect: function(pointData) {
            return pointData.getRecordFields();
        },
        defaultSelectedToAll: function() {
            return true;
        },
        fieldSelectionChanged: function() {
            SUPER.fieldSelectionChanged.call(this);
            this.updateUI();
        },
        updateUI: function(reload) {
            SUPER.updateUI.call(this,reload);
            let records = this.filterData();
            if (!records) {
                this.setDisplayMessage(this.getLoadingMessage());
                return;
            }
	    this.records = records;
	    let _this = this;
            let fields = this.getSelectedFields(this.getData().getRecordFields());
            let html = "";
	    let showCards = this.getShowCards();
	    if(showCards) html+=HU.open("div",[CLASS,"display-records-grid"]);
            for (let rowIdx = 0; rowIdx < records.length; rowIdx++) {
		let div = "";
                let tuple = this.getDataValues(records[rowIdx]);
                for (let fieldIdx = 0; fieldIdx < fields.length; fieldIdx++) {
                    let field = fields[fieldIdx];
                    let v = tuple[field.getIndex()];
		    if(v.getTime) v = this.formatDate(v);
                    div += HU.b(this.getFieldLabel(field)) + ": " + v + "<br>" +"\n";
                }
                let box = HU.div([CLASS,showCards?"":"display-records-record",RECORD_INDEX,rowIdx,RECORD_ID, records[rowIdx].getId()], div);

		if(showCards) box =HU.div([CLASS,"display-records-grid-box"],box);
		html+=box;
            }
	    if(showCards) html+=HU.close("div");
            let height = this.getProperty("maxHeight", "400px");
            if (!height.endsWith("px")) {
                height = height + "px";
            }
            this.setContents(HU.div([STYLE, HU.css('max-height', height,'overflow-y','auto')], html));
	    this.find(".display-records-record").click(function() {
		let record = _this.records[$(this).attr(RECORD_INDEX)];
		if(record) {
		    _this.propagateEventRecordSelection({highlight:true,record: record});
		}

	    });
        },
        handleEventRecordSelection: function(source, args) {
            //                this.lastHtml = args.html;
            //                this.setContents(args.html);
        }
    });
}


function RamaddaStatsDisplay(displayManager, id, properties, type) {
    const SUPER = new RamaddaFieldsDisplay(displayManager, id, type || DISPLAY_STATS, properties);
    if (!type)
        addRamaddaDisplay(this);
    let myProps = [
	{label:'Summary Statistics'},
	{p:'showDefault',ex:'false'},
	{p:'showMin',ex:'false',canCache:true},
	{p:'showMax',ex:'false',canCache:true},
	{p:'showRange',d:false,ex:'true',canCache:true},
        {p:'showAverage',ex:'false',canCache:true},
        {p:'showStd',ex:'false',canCache:true},
        {p:'showPercentile',ex:'false',canCache:true},
        {p:'showCount',ex:'false',canCache:true},
        {p:'showTotal',ex:'false',canCache:true},
        {p:'showPercentile',ex:'false',canCache:true},
        {p:'showMissing',ex:'false',canCache:true},
        {p:'showUnique',ex:'false',canCache:true},
        {p:'showType',ex:'false',canCache:true},
        {p:'showText',ex:'false',canCache:true},
	{p:"sortStatsBy",ex:'min|max|total|average'},
	{p:"sortStatsAscending",ex:'false'},
	{p:'doValueSelection',ex:'false'},
	{p:"fieldHeaderLabel",ex:''},
	{p:"statsTableWidth",ex:'100%'},

    ];

    defineDisplay(this, SUPER, myProps, {
        "map-display": false,
        needsData: function() {
            return true;
            //                return this.getProperty("loadData", false) || this.getCreatedInteractively();
        },
        handleEventPointDataLoaded: function(source, pointData) {
            if (!this.needsData()) {
                if (this.dataCollection == null) {
                    this.dataCollection = source.dataCollection;
                    this.updateUI();
                }
            }
        },
        getDefaultSelectedFields: function(fields, dfltList) {
            if (dfltList != null && dfltList.length > 0) {
                return dfltList;
            }
            //get the numeric fields
            let l = [];
	    fields.forEach(field=>{
                if (!this.getShowText() && !field.isNumeric()) return;
                if (field.isFieldGeo()) {
                    return;
                }
                l.push(field);
	    });
            return l;
        },

        getFieldsToSelect: function(pointData) {
            return pointData.getRecordFields();
        },
        defaultSelectedToAll: function() {
            return true;
        },
        fieldSelectionChanged: function() {
            SUPER.fieldSelectionChanged.call(this);
            this.updateUI();
        },
        updateUI: function(args) {
            SUPER.updateUI.call(this,args);
            let records = this.filterData();
            if (!records) {
		return;
	    }
            let fields = this.getSelectedFields([]);
            let stats = [];
            let justOne = false;
	    fields.forEach((field,idx)=>{
                stats.push({
		    field:field,
                    isNumber: false,
                    count: 0,
                    min: NaN,
                    uniqueMap: {},
                    unique: 0,
                    std: 0,
                    max: NaN,
                    total: 0,
                    numMissing: 0,
                    numNotMissing: 0,
                    type: field.getType(),
                    values: []
                });
            });
	    records.forEach(record=>{
                stats.forEach(stat=>{
		    let field = stat.field;
                    let v = field.getValue(record);
                    if (v) {
                        if (!Utils.isDefined(stat.uniqueMap[v])) {
                            stat.uniqueMap[v] = 1;
                            stat.unique++;
                        } else {
                            stat.uniqueMap[v]++;
                        }
                    }
                    stat.isNumber = field.isNumeric();
                    stat.count++;
                    if (v == null || isNaN(v)) {
                        stat.numMissing++;
                    } else {
                        stat.numNotMissing++;
                    }
                    if (!isNaN(v) && (v!==null) && (typeof v == 'number')) {
                        let label = this.getFieldLabel(field).toLowerCase();
                        if (label.indexOf("latitude") >= 0 || label.indexOf("longitude") >= 0) {
			    return;
                        }
                        stat.total += v;
                        stat.max = Utils.max(stat.max, v);
                        stat.min = Utils.min(stat.min, v);
                        stat.values.push(v);
                    }
		});
	    });

		
	    let dflt = this.getProperty("showDefault",true);

	    if (this.getShowUnique(dflt)) {
                stats.forEach(stat=>{
		    let field = stat.field;
                    stat.uniqueMax = 0;
                    stat.uniqueValue = "";
                    for (let v in stat.uniqueMap) {
                        let count = stat.uniqueMap[v];
                        if (count > stat.uniqueMax) {
                            stat.uniqueMax = count;
                            stat.uniqueValue = v;
                        }
                    }
                });
	    }

            if (this.getShowStd(dflt)) {
                stats.forEach(stat=>{
                    let values = stat.values;
                    if (values.length > 0) {
                        let average = stat.total / values.length;
                        let stdTotal = 0;
                        for (let i = 0; i < values.length; i++) {
                            let diff = values[i] - average;
                            stdTotal += diff * diff;
                        }
                        let mean = stdTotal / values.length;
                        stat.std = Math.sqrt(mean);
                    }
                });
            }

            let border = (justOne ? "0" : "1");
	    let attrs = [ID,this.getDomId("statstable"), CLASS, "row-border stripe  display-stats"];
	    let tableWidth = this.getStatsTableWidth();
	    if(tableWidth) {
		attrs.push("width");
		attrs.push(tableWidth);
	    }
            let html = HU.open(TABLE, attrs);
	    html+=HU.open(THEAD);
            if (!justOne) {
                header = [this.getFieldHeaderLabel("")];
                if (this.getShowCount(dflt)) 
                    header.push("Count");
                if (this.getShowMin(dflt)) 
                    header.push("Min");
                if (this.getShowPercentile(dflt)) 
                    header.push("25%","50%","75%");
                if (this.getShowMax(dflt)) 
                    header.push("Max");
                if (this.getShowRange()) 
                    header.push("Range");		
                if (this.getShowTotal(dflt)) 
                    header.push("Total");
                if (this.getShowAverage(dflt)) 
                    header.push("Average");
                if (this.getShowStd(dflt)) 
                    header.push("Std");
                if (this.getShowUnique(dflt)) 
                    header.push("# Unique","Top","Freq.");
                if (this.getShowMissing(dflt)) 
                    header.push("Not&nbsp;Missing","Missing")
                html += HU.tr(["valign", "bottom"], HU.ths([CLASS, "display-stats-header", "align", "center"], header));
            }
	    html+=HU.close(THEAD);
	    html+=HU.open(TBODY);
            let cats = [];
            let catMap = {};
	    let doValueSelection = this.getDoValueSelection(false);
            stats.forEach(stat=>{
                stat.average = stat.numNotMissing == 0 ? NaN : (stat.total / stat.numNotMissing);
	    });

	    let sortBy = this.getSortStatsBy();
	    let sortAscending = this.getSortStatsAscending(true);
	    if(sortBy) {
		let sortFunc =(a,b)=>{
		    let result  =0
		    if(sortBy == "total")
			result= a.total-b.total;
		    else if(sortBy == "min")
			result= a.min-b.min;
		    else if(sortBy == "max")
			result= a.max-b.max;
		    else if(sortBy == "average")
			result= a.average-b.average;
		    if(result==0) return result;
		    if(sortAscending) return result;
		    return -result;
		    
		};
		stats.sort(sortFunc);
	    }

            stats.forEach(stat=>{
		let field = stat.field;
                let right = "";
                let total = SPACE;
                let _label = this.getFieldLabel(field).toLowerCase();
                let avg = stat.numNotMissing == 0 ? "NA" : this.formatNumber(stat.total / stat.numNotMissing);
                //Some guess work about when to show a total
                if (_label.indexOf("%") < 0 && _label.indexOf("percent") < 0 && _label.indexOf("median") < 0) {
                    total = this.formatNumber(stat.total);
                }
                if (justOne) {
                    right = HU.tds(["xalign", "right"], [this.formatNumber(stat.min)]);
		    return;
                }
                let values = [];
                if (!stat.isNumber && this.getShowText(dflt)) {
                    if (this.getShowCount(dflt))
                        values.push(stat.count);
                    if (this.getShowMin(dflt))
                        values.push("-");
                    if (this.getShowPercentile(dflt)) 
                        values.push("-","-","-");
                    if (this.getShowMax(dflt))
                        values.push("-");
                    if (this.getShowRange())
                        values.push("-");		    
                    values.push("-");
                    if (this.getShowAverage(dflt)) 
                        values.push("-");
                    if (this.getShowStd(dflt)) 
                        values.push("-");
                    if (this.getShowUnique(dflt)) 
                        values.push(stat.unique,stat.uniqueValue,stat.uniqueMax);
                    if (this.getShowMissing(dflt)) 
                        values.push(stat.numNotMissing,stat.numMissing);
                } else {
                    if (this.getShowCount(dflt)) 
                        values.push(stat.count);
                    if (this.getShowMin(dflt)) 
                        values.push(this.formatNumber(stat.min));
                    if (this.getShowPercentile(dflt)) {
                        let range = stat.max - stat.min;
			let tmp =p=> {
                            let s = this.formatNumber(stat.min + range * p);
			    if(doValueSelection) {
				s = HU.span([CLASS,"display-stats-value","data-type", "percentile","data-value", p],s);
			    }
                            values.push(s);
			}
			let percs = [.25,.5,.75];
			percs.map(v=>tmp(v));
                    }
                    if (this.getShowMax(dflt)) 
                        values.push(this.formatNumber(stat.max));
                    if (this.getShowRange()) 
                        values.push(this.formatNumber(stat.max-stat.min));		    
                    if (this.getShowTotal(dflt)) 
                        values.push(total);
                    if (this.getShowAverage(dflt)) 
                        values.push(avg);
                    if (this.getShowStd(dflt)) 
                        values.push(this.formatNumber(stat.std));
                    if (this.getShowUnique(dflt)) {
                        values.push(stat.unique);
                        if (Utils.isNumber(stat.uniqueValue)) {
                            values.push(this.formatNumber(stat.uniqueValue));
                        } else {
                            values.push(stat.uniqueValue);
                        }
                        values.push(stat.uniqueMax);
                    }
                    if (this.getShowMissing(dflt)) 
                        values.push(stat.numNotMissing,stat.numMissing);
                }
                right = HU.tds(["align", "right"], values);
                let align = (justOne ? "right" : "left");
                let label = this.getFieldLabel(field);
                let toks = label.split("!!");
                let tooltip = "";
                tooltip += field.getId();
                if (field.description && field.description != "") {
                    tooltip += "\n" + field.description + "\n";
                }
                label = toks[toks.length - 1];
                if (Utils.stringDefined(field.unit)) 
                    label = label + " [" + field.unit + "]";
                if (justOne) 
                    label += ":";
                label = label.replace(/ /g, SPACE)
                let row = HU.tr([], HU.td(["nowrap","true","align", align], field.getTypeLabel() +SPACE + HU.b(HU.span([TITLE, tooltip], label))) + right);
                html += row;
            });
            html += HU.close(TBODY, TABLE);
            this.setContents(html);
	    //the aaSorting turns off the inital sorting
	    HU.formatTable("#" +this.getDomId("statstable"),{ordering:true,"aaSorting": []});	    
            this.initTooltip();

	    if(doValueSelection) {
		let values = this.find(".display-stats-value");
		values.each(function() {
		    let type  = $(this).attr("data-type");
		    let value  = $(this).attr("data-value");
		    let links = SPACE + HU.getIconImage("fa-less-than",[TITLE,"Filter other displays",
									CLASS,"display-stats-value-link","data-type",type,"data-value",value],
							[STYLE,HU.css('font-size','8pt')]);

		    $(this).append(links);
		});
		values = this.find(".display-stats-value-link");
		values.each(function() {
		});
	    }

	    /*???
            //always propagate the event when loaded
	    let record = records[0];
	    this.displayManager.propagateEventRecordSelection(this,
							      record, {
								  index: 0
							      });
	    */
        },
    });
}



function RamaddaCooccurenceDisplay(displayManager, id, properties) {
    const ID_TABLE = "table";
    const ID_HEADER = "coocheader";
    const ID_SORTBY = "sortby";
    const SUPER = new RamaddaDisplay(displayManager, id, DISPLAY_COOCCURENCE, properties);
    let myProps = [
	{label:'Cooccurence'},
	{p:'sourceField',ex:''},
	{p:'targetField',ex:''},
	{p:'colorBy',ex:''},
	{p:'directed',ex:'false'},
	{p:'missingBackground',ex:'#eee'},
	{p:'showSortBy',ex:'false'},
	{p:'sortBy',ex:'weight'},
	{p:'minWeight',ex:''},
	{p:'topSpace',ex:'50px'}
    ];
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
        needsData: function() {
            return true;
        },

	getHeader2:function() {
	    let html = SUPER.getHeader2.call(this);
	    let weightField = this.getFieldById(null, this.getProperty("colorBy","weight"));
	    if(weightField && this.getProperty("showSortBy",true)) {
		let enums = [["name","Name"],["weight","Weight"]];
		html +=  HU.div([STYLE,HU.css('display','inline-block')], "Sort by: " + HU.select("",[ID,this.domId(ID_SORTBY)],enums,this.getProperty("sortBy","")))+SPACE2;
		
	    }
	    return html;

	}, 
	initHeader2:function() {
	    let _this = this;
	    this.jq(ID_SORTBY).change(function() {
		_this.setProperty("sortBy",$(this).val());
		_this.updateUI();
	    });
	},
        updateUI: function() {
	    let records = this.filterData();
	    if (!records) {
                return;
	    }  
	    let html = HU.div([ID, this.domId(ID_HEADER)]) +
		HU.div([ID, this.domId(ID_TABLE)]);
	    this.setContents(html);


	    let sourceField = this.getFieldById(null, this.getProperty("sourceField","source"));
	    let targetField = this.getFieldById(null, this.getProperty("targetField","target"));
	    let weightField = this.getFieldById(null, this.getProperty("colorBy","weight"));

	    if(sourceField==null || targetField==null) {
		this.setDisplayMessage("No source/target fields specified");
		return;
	    }
	    let colors = this.getColorTable();
	    if(!colors) colors = Utils.getColorTable("blues",true);
	    let colorBy = this.getColorByInfo(records,null,null,colors);
	    let names = {};
	    let nameList = [];
	    let sources = [];
	    let targets = [];
	    let links={};
	    let maxWeight = 0;
	    let sortBy  = this.getProperty("sortBy","name");
	    let directed = this.getProperty("directed",true);
	    let missing = -999999;

	    records.map(r=>{
		let source = r.getValue(sourceField.getIndex());
		let target = r.getValue(targetField.getIndex());
		let weight = missing;
		if(weightField) {
		    weight = r.getValue(weightField.getIndex());
		    maxWeight = Math.max(maxWeight, weight);
		}
		sources.push({name:source,weight:weight});
		targets.push({name:target,weight:weight});
		if(!directed) {
		    sources.push({name:target,weight:weight});
		    targets.push({name:source,weight:weight});
		    
		}
		links[source+"--" + target] = weight;
	    });
	    maxWeight = this.getProperty("maxWeight", maxWeight);
	    let sortFunc =(a,b)=>{
		if(sortBy == "name" || sortBy=="") {
		    return a.name.localeCompare(b.name);
		} else {
		    return b.weight-a.weight;
		}} 
	    sources.sort(sortFunc);
	    targets.sort(sortFunc);
	    let minWeight = this.getProperty("minWeight",missing);
	    let seen = {}
	    let tmp =[];
	    let pruneFunc = t=>{
		if(minWeight!=missing) {
		    if(t.weight==missing || t.weight<minWeight) return;
		}
		if(!seen[t.name]) {
		    seen[t.name]=true;
		    tmp.push(t.name);
		}
	    }
	    sources.map(pruneFunc);
	    sources=tmp;
	    seen = {}
	    tmp =[];
	    targets.map(pruneFunc);
	    targets = tmp;

	    let table = HU.div([STYLE,HU.css('margin-top',this.getProperty("topSpace","100px"))]) +
		HU.open(TABLE,[STYLE,HU.css('height','100%'), CLASS,'display-cooc-table', 'order',0]);
	    table += HU.open(TR,['valign','bottom']) + HU.td(['border','none']);
	    targets.map(target=>{
		target = target.replace(/ /g,SPACE).replace(/-/g,SPACE);
		table += HU.td([STYLE,HU.css('border','none'), "width","6"],HU.div([CLASS,"display-cooc-colheader"], target));
	    });

	    let missingBackground  = this.getProperty("missingBackground","#eee");
	    sources.map(source=>{
		let label =  source.replace(/ /g,SPACE);
		table += HU.open(TR,['valign','bottom']) +HU.td([STYLE,HU.css('border','none'), 'align','right'], HU.div([CLASS,"display-cooc-rowheader"], label));
		targets.map(target=>{
		    let weight = links[source+"--" + target];
		    if(!directed && !Utils.isDefined(weight))
			weight = links[target+"--" + source];
		    let style="";
		    if(weight) {
			if(weight == missing || maxWeight == 0) 
			    style = HU.css('background','#ccc');
			else {
			    if(colorBy.index>=0) {
				color =  colorBy.getColor(weight);
				style = HU.css('background',color);
			    }
			    //			    let percent = weight/maxWeight;
			    //			    let index = parseInt(percent*colors.length);
			    // 			    if(index>=colors.length) index=colors.length-1;
			    //			    style = "background:" + colors[index]+";";
			}
		    }  else {
			style = HU.css('background', missingBackground);
		    }
		    table+=HU.td([TITLE,source+" -> " + target+(weight>0?" " + weight:""), "width","3"],HU.div([CLASS,"display-cooc-cell",STYLE,style+HU.css('height','100%')],SPACE));
		});
		table+= HU.close(TR);
	    });

	    table+=HU.close(TR,TABLE);
	    table+=HU.div([STYLE,HU.css('margin','5px')]);
	    this.jq(ID_TABLE).html(table);
	    colorBy.displayColorTable();

	}
    })
}



function RamaddaBoxtableDisplay(displayManager, id, properties) {
    const ID_HEADER = "coocheader";
    const ID_SORTBY = "sortby";
    const SUPER  = new RamaddaDisplay(displayManager, id, DISPLAY_BOXTABLE, properties);
    let myProps = [
	{label:'Color Boxes'},
	{p:'categoryField',ex:''},
	{p:'colorBy',ex:''},
	{p:'tableWidth',ex:'300'},
	{p:'labelTemplate'},
	{p:'labelStyle'},
	{p:'imageField'},
	{p:'imageWidth',d:'30px'},
	{p:'labelColumnWidth',ex:'300px'}	
    ];
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
        needsData: function() {
            return true;
        },

        updateUI: function() {
	    let records = this.filterData();
	    if (!records) {
                return;
	    }  
	    let categoryField = this.getFieldById(null, this.getProperty("categoryField","category"));
	    if(categoryField==null) {
		this.setDisplayMessage("No category field field specified");
		return;
	    }

	    let imageWidth = this.getImageWidth('30px');
	    let imageField = this.getFieldById(null,this.getImageField());
	    let labelTemplate = this.getLabelTemplate();
	    let labelStyle = this.getLabelStyle('');
	    let colors = this.getColorTable();
	    if(!colors) colors = Utils.getColorTable("blues",true);
	    let colorBy = this.getColorByInfo(records,null,null,colors);
	    let catMap =  {};
	    let cats = [];
	    records.forEach(r=>{
		let category = r.getValue(categoryField.getIndex());
		let value = colorBy.index>=0?r.getValue(colorBy.index):0;
		let list = catMap[category] && catMap[category].list;
		if(!list) {
		    list = [];
		    catMap[category] = {list:list, max:0};
		    cats.push(category);
		}
		catMap[category].max = Math.max(catMap[category].max,value);
		list.push(r);
	    });
	    let html = HU.open(TABLE,[CLASS,'display-colorboxes-table','cellpadding',5]);
	    let labelColumnWidth=this.getLabelColumnWidth();
	    let tableWidth=this.getProperty("tableWidth",300);
	    cats.sort((a,b)=>{
		return catMap[b].max - catMap[a].max;
	    });

	    cats.forEach(cat=>{
		let length = catMap[cat].list.length;
		let label = HU.span(["field-id",categoryField.getId(),
				     "field-value",cat], cat);
		let tdAttrs = ['align','right',CLASS,'display-colorboxes-header'];
		if(labelColumnWidth)
		    tdAttrs.push(WIDTH,labelColumnWidth);
		let row = HU.open(TR,['valign','center'],HU.td(tdAttrs,label+ " ("+length+")"));
		row+=	  HU.open(TD);


		if(colorBy.index) {
		    catMap[cat].list.sort((a,b)=>{
			return b.getData()[colorBy.index]-a.getData()[colorBy.index];
		    });
		}
		catMap[cat].list.forEach((record,idx)=>{
		    let color = "#ccc";
		    if(colorBy.index) {
			color =  colorBy.getColor(record.getData()[colorBy.index], record) || color;
		    }
		    let style = '';
		    let contents = '';
		    let clazz='';
		    if(imageField) {
			let url = imageField.getValue(record);
			contents  = HU.div(['style','text-align:center;'],HU.image(url,['width',imageWidth]));
			clazz ='display-colorboxes-image';
		    } else {
			style = HU.css('background', color);
			clazz ='display-colorboxes-box';
		    }
		    if(labelTemplate) {
			let label = this.applyRecordTemplate(record, null,null,labelTemplate);
			contents +=HU.div(['class','display-colorboxes-label','style',labelStyle],label);
		    }
		    row +=HU.div([TITLE,"",RECORD_ID, record.getId(), CLASS,'ramadda-clickable ' + clazz,STYLE,style],contents);
		});
		row+=HU.close(TD,TR);
		html+=row;
	    });

	    html +=HU.close(TABLE);
            this.displayHtml(html);
	    colorBy.displayColorTable(500);
	    if(!this.getProperty("tooltip"))
		this.setProperty("tooltip","${default}");
	    this.makeTooltips(this.find(".display-colorboxes-box,.display-colorboxes-image"),records);
	    this.addFieldClickHandler(null, records);
	}
    })
}



function RamaddaPercentchangeDisplay(displayManager, id, properties) {
    const SUPER =  new RamaddaFieldsDisplay(displayManager, id, DISPLAY_PERCENTCHANGE, properties);
    let myProps = [
	{label:'Percent Change'},
	{p:'template',ex:'${date1} ${date2} ${value1} ${value2} ${percent} ${per_hour} ${per_day} ${per_week} ${per_month} ${per_year}'},
	{p:'fieldLabel',ex:''},
	{p:'sortFields',ex:'false'},
	{p:'highlightPercent',ex:'50'},
	{p:'highlightPercentPositive',ex:'50'},
	{p:'highlightPercentNegative',ex:'-50'},
	{p:'highlightColor',ex:''},
	{p:'highlightColorPositive',ex:''},
	{p:'highlightColorNegative',ex:''},
    ];
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
	updateUI: function() {
	    let records = this.filterData();
	    if(!records) return;
            let allFields = this.getData().getRecordFields();
            let fields = this.getSelectedFields(allFields);
	    fields = this.getFieldsByType(fields, "numeric");
	    let  record1 = records[0];
	    let  record2 = records[records.length-1];
	    let template = this.getProperty("template",null);
	    let headerTemplate = this.getProperty("headerTemplate","");
	    let footerTemplate = this.getProperty("footerTemplate","");
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
		let diff = date2.getTime() - date1.getTime();
		days = diff/1000/60/60/24;
		hours = days*24;
		years = days/365;
		months = years*12;
	    }
            let html =  "";
	    if(template) {
		html= headerTemplate;
	    } else {
		html += HU.open(TABLE, [CLASS, "stripe nowrap ramadda-table", ID, this.domId("percentchange")]);
		html += HU.open(THEAD, []);
		html += HU.tr([], HU.th([STYLE,HU.css('text-align','center')], this.getProperty("fieldLabel", "Field")) + HU.th([STYLE,HU.css('text-align','center')], label1) + HU.th([STYLE,HU.css('text-align','center')], label2)
			      + HU.th([STYLE,HU.css('text-align','center')], "Percent Change"));
		html += HU.close(THEAD);
		html += HU.open(TBODY, []);
	    }
	    let tuples= [];
	    fields.map(f=>{
		let val1 = 0;
		for(let i=0;i<records.length;i++) {
		    let val = records[i].getValue(f.getIndex());
		    if(!isNaN(val)) {
			val1 = val;
			break;
		    }
		}
		
		let val2 = 0;
		for(let i=records.length-1;i>=0;i--) {
		    let val = records[i].getValue(f.getIndex());
		    if(!isNaN(val)) {
			val2 = val;
			break;
		    }
		}

		let percent = parseInt(1000*(val2-val1)/val1)/10;
		//		val1 = record1.getValue(f.getIndex());
		//		val2 = record2.getValue(f.getIndex());
		tuples.push({field:f,val1:val1,val2:val2,percent:percent});
	    });

	    if(this.getProperty("sortFields",true)) {
		tuples.sort((a,b)=>{
		    return -(a.percent-b.percent);
		})
	    }
	    let highlightPercent = this.getProperty("highlightPercent",NaN);
	    let highlightPercentPositive = this.getProperty("highlightPercentPositive",highlightPercent);
	    let highlightPercentNegative = this.getProperty("highlightPercentNegative",-highlightPercent);
	    let highlightColor = this.getProperty("highlightColor","#ccc"||"#FFFEEC");
	    let posColor = this.getProperty("highlightColorPositive",highlightColor);
	    let negColor = this.getProperty("highlightColorNegative",highlightColor);
	    tuples.map(t=>{
		if(template) {
		    let h = template.replace("${field}", this.getFieldLabel(t.field)).replace("${value1}",this.formatNumber(t.val1)).replace("${value2}",this.formatNumber(t.val2)).replace("${percent}",this.formatNumber(t.percent)).replace("${date1}",label1).replace("${date2}",label2).replace("${difference}", this.formatNumber(t.val2-t.val1));
		    
		    h = h.replace(/\${per_hour}/g,this.formatNumber(t.percent/hours));
		    h = h.replace(/\${per_day}/g,this.formatNumber(t.percent/days));
		    h = h.replace(/\${per_week}/g,this.formatNumber(t.percent/(days/7)));
		    h = h.replace(/\${per_month}/g,this.formatNumber(t.percent/months));
		    h = h.replace(/\${per_year}/g,this.formatNumber(t.percent/years));
		    html+=h;
		} else {
		    let style = "";
		    if(!isNaN(highlightPercentPositive))
			if(t.percent>highlightPercentPositive)
			    style += HU.css('background', posColor);
		    if(!isNaN(highlightPercentNegative))
			if(t.percent<highlightPercentNegative)
			    style += HU.css('background', negColor);
		    
		    html += HU.tr([STYLE,style], HU.td([], this.getFieldLabel(t.field)) + 
				  HU.td(["align","right"], this.formatNumber(t.val1)) +
				  HU.td(["align","right"], this.formatNumber(t.val2))
				  + HU.td(["align","right"], t.percent+"%"));
		}
	    });

	    if(template) {
		html+= footerTemplate;
	    } else {
		html += HU.close(TBODY);
		html += HU.close(TABLE);
	    }
	    this.setContents(html); 
            HU.formatTable("#" + this.domId("percentchange"), {ordering:false
								  //scrollY: this.getProperty("tableSummaryHeight", tableHeight)
								 });
	},
    })
}



function RamaddaDatatableDisplay(displayManager, id, properties) {
    const SUPER  = new RamaddaDisplay(displayManager, id, DISPLAY_DATATABLE, properties);
    const ID_COLORTABLE = 'datatable_colortable'
    const ID_PIECOLORS = 'datatable_piecolors'    
    let myProps = [
	{label:'Data Table'},
	{p:'columnSelector',ex:'date_day|date_hour|date_dow|date_month|date_year'},
	{p:'selectors',ex:'date_day,date_hour,date_dow,date_month,date_year,date_fieldid'},
	{p:'columnSelector',ex:'date_day|date_hour|date_dow|date_month'},
	{p:'rowSelector',ex:'date_day|date_hour|date_dow|date_month'},
	{p:'checkedIcon',ex:'fa-checked'},
	{p:'checkedTooltipHeader',ex:'${numberChecked}'},
	{p:'dataCheckers',ex:'match|notmatch|lessthan|greaterthan|equals|notequals(field=field,value=value,label=label,enabled=false) '}, 
	{p:'showColumnSelector',ex:'false'},
	{p:'showRowSelector',ex:'false'},
	{p:'showValues',ex:'false'},
	{p:'showColors',ex:'true',d:false},
	{p:'showPieColors',ex:'true',d:false},	
	{p:'showRowTotals',ex:'false'},
	{p:'showColumnTotals',ex:'false'},
	{p:'slantHeader',ex:'true'}
	];

    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
        needsData: function() {
            return true;
        },
        updateUI: function() {
            this.setDisplayMessage(this.getLoadingMessage());
	    let records = this.filterData();
	    if (!records) {
                return;
	    }  
	    let colors = this.getColorTable(true);
	    if (!colors) colors = Utils.ColorTables.cats.colors;
	    let checkers = this.getTheDataFilters(this.getProperty("dataCheckers"));
	    let cells = {};
	    let countFields= this.getFieldsByIds(null, this.getProperty("countFields"));

	    let selectors;
	    let fieldMap = {};
	    if(this.getProperty("selectors")) {
		selectors = [];
		let labels = {"date_day":"Day","date_dow":"Day of Week","date_hour":"Hour","date_month":"Month","date_year":"Year"};
		this.getProperty("selectors").split(",").map(s=>{
		    let label = labels[s];
		    if(!label) {
			let field = this.getFieldById(null,s);
			if(field) {
			    label = this.getFieldLabel(field);
			    fieldMap[s] = field;
			}
		    }
		    if(label) 
			selectors.push([s,label]);
		});
	    } else {
		selectors  =   [["date_day","Day"],["date_dow","Day of Week"],["date_hour","Hour"],["date_month","Month"],["date_year","Year"]];
	    }

	    let columnSelector = this.getProperty("columnSelector",selectors[0][0]);
	    let rowSelector = this.getProperty("rowSelector",selectors[1][0]);
	    let getValues =(s=>{
		let values = [];
		if(s =="date_dow") {
		    Utils.dayNamesShortShort.map((d,i)=>{
			values.push({id:i,label:d});
		    });
		}  else if(s =="date_hour") {
		    let tmp =["12&nbsp;AM","1","2","3","4","5","6","7","8","9","10","11",
			      "12&nbsp;PM","1","2","3","4","5","6","7","8","9","10","11"];
		    for(let i=0;i<24;i++)
			values.push({id:i,label:tmp[i]});
		}  else if(s =="date_day") {
		    for(let day=1;day<=31;day++)
			values.push({id:day,label:String(day)});
		}  else if(s =="date_month") {
		    Utils.monthNames.map((m,i)=>{
			values.push({id:i,label:m});
		    });
		}  else if(s =="date_year") {
		    let years =[];
		    let seen = {};
		    records.map(r=>{
			let year = r.getDate().getUTCFullYear();
			if(!seen[year]) {
			    years.push(year);
			    seen[year] = true;
			}
		    });
		    years.sort();
		    years.map((y,i)=>{
			values.push({id:y,label:String(y)});
		    });
		} else {
		    let field = fieldMap[s];
		    if(field) {
			let seen = {};
			let isNumber = false;
			this.getColumnValues(records, field).values.map(d=>{
			    isNumber = Utils.isNumber(d);
			    if(!Utils.isDefined(seen[d])) {
				seen[d] = true;
				values.push({id:d,label:String(d)});
			    }
			});
			values.sort((a,b) =>{
			    if(isNumber) {
				return a.id-b.id;
			    }
			    return a.label.localeCompare(b.label);
			});
		    }
		}
		return values;
	    });
	    let columns =getValues(columnSelector);
	    let rows =getValues(rowSelector);
	    let getId =((s,r,l)=>{
		if(s =="date_dow")  {
		    return l[r.getDate().getDay()].id;
		} else if(s =="date_hour") {
		    return l[r.getDate().getUTCHours()].id;
		} else if(s =="date_day") {
		    return  l[r.getDate().getUTCDate()-1].id;
		} else if(s =="date_month") {
		    return  l[r.getDate().getUTCMonth()].id;
		} else if(s =="date_year") {
		    return r.getDate().getUTCFullYear();
		} else {
		    let field = fieldMap[s];
		    if(field) {
			return r.getValue(field.getIndex());
		    }
		}
		return "null";
	    });

	    let uniqueValues = {};
	    records.map((r,i)=>{
		let row =getId(rowSelector,r,rows);
		let column =getId(columnSelector,r,columns);
		let key = row+"-" +column;
		let cell = cells[key];
		if(!cell) {
		    cell = cells[key]={
			checked:[],
			row:row,
			column:column,
			count:0,
			records:[],
			countFields:{}
		    };
		    countFields.forEach(f=>{
			cell.countFields[f.getId()] = {
			    values:[],
			    counts:{}
			};
		    });
		}

		if(checkers && checkers.length>0) {
		    if(this.checkDataFilters(checkers, r)) {
			cell.checked.push(r);
		    }
		}

		countFields.forEach(f=>{
		    //f=>in_or_out
		    let v = r.getValue(f.getIndex());
		    //v=incoming or outgoing
		    let cf = cell.countFields[f.getId()];
		    if(!cf.counts[v]) {
			cf.counts[v] = 0;
			uniqueValues[v] = true;
			cf.values.push(v);
		    }
		    cf.counts[v]++;
		});
		cell.count++;
		cell.records.push(r);
	    });


	    let min = 0;
	    let max  = 0;
	    let cnt = 0;
	    for(a in cells) {
		let cell = cells[a];
		min = cnt==0?cell.count:Math.min(cell.count,min);
		max = cnt==0?cell.count:Math.max(cell.count,max);
		cnt++;
		//		console.log("cell: "+ cell.row +" " + cell.column +" #:" + cell.count);
		countFields.forEach(f=>{
		    let cf = cell.countFields[f.getId()];
		    cf.values.sort();
		    cf.values.forEach(v=>{
			//			console.log("\t" + v +" = " + cf.counts[v] +" " + cell.row +" " + cell.column);
		    });
		    
		});
	    }


	    let showValues = this.getProperty("showValues", true);
	    let showColors = this.getShowColors();
	    let showPieColors = this.getShowPieColors();
	    let cellCount = columns.length;
	    let maxRowValue = 0;
	    let maxColumnValue = 0;
	    let columnTotals = {};
	    let rowTotals = {};
	    rows.map(row=>{
		let rowTotal = 0;
		columns.map(column=>{
		    let key = row.id +"-" +column.id;
		    if(cells[key]) {
			rowTotal+=cells[key].count;
		    }
		});
		rowTotals[row.id] = rowTotal;
		maxRowValue = Math.max(maxRowValue, rowTotal);
	    });
	    columns.map(column=>{
		let columnTotal = 0;
		rows.map(row=>{
		    let key = row.id +"-" +column.id;
		    if(cells[key]) {
			columnTotal+=cells[key].count;
		    }
		});
		columnTotals[column.id] = columnTotal;
		maxColumnValue = Math.max(maxColumnValue, columnTotal);
	    });

	    let showRowTotals = this.getProperty("showRowTotals",true);
	    let showColumnTotals = this.getProperty("showColumnTotals",true);
	    let width = Math.round(100/cellCount);
	    let table = "";
	    table+=HU.open('tr',['valign','bottom']) + HU.td([],"");
	    let needToRotate = this.getProperty("slantHeader",false);
	    let topSpace = 0;
	    columns.map(column=>{
		let label = column.label;
		if(label.length>15) {
		    needToRotate = true;
		    topSpace = Math.max(topSpace,Math.round(label.length*3));
		    topSpace = 80;
		}
	    });
	    
	    columns.map(column=>{
		let label = column.label;
		if(needToRotate) {
		    if(label.length>20) {
			label = label.substring(0,20)+"...";
		    }
		    label = label.replace(/ /g,SPACE).replace("-",SPACE);
		    label = HU.div(["tootltip",column.label,CLASS,"display-datatable-header-slant"],label);
		}		    
		table+=HU.td([CLASS,'display-datatable-header','align','center'],label);
	    });
	    table+=HU.close(TR);

	    rows.map(row=>{
		let name = HU.div([],row.label.replace(/ /g,SPACE));
		table+=HU.open(TR) + HU.td([CLASS,"display-datatable-name","align","right", "width","100"],name);
		columns.map(column=>{
		    let key = row.id+"-" +column.id;		    
		    let inner = "";
		    let style = "";
		    let marker = "";
		    let cell = cells[key];
		    let extra1 = "";
		    let extra2 = "";
		    if(cell) {
			if(showValues) 
			    inner = HU.div([CLASS,"display-datatable-value"],cell.count);
			extra2 = HU.div(["data-key",key,CLASS,"display-datatable-counts"]);
			if(cell.checked.length) {
			    extra1= HU.getIconImage(this.getProperty("checkedIcon","fa-check"),[TITLE,"","data-key",key,CLASS,"display-datatable-checked"]);
			}
			if(showColors) {
                            let percent = (cell.count - min) / (max - min);
                            let ctIndex = parseInt(percent * colors.length);
                            if (ctIndex >= colors.length) ctIndex = colors.length - 1;
                            else if (ctIndex < 0) ctIndex = 0;
                            style = HU.css('background-color', colors[ctIndex]);
			}
		    }
		    let cellHtml = extra1 +extra2+inner;
		    table += HU.td([CLASS,'display-datatable-cell','align','right', STYLE,style,'width',width +'%'], cellHtml);
		});
		if(showRowTotals) {
		    let total = rowTotals[row.id];
		    let dim = Math.round(total/maxRowValue*100);
		    let bar = HU.div([CLASS, "display-datatable-summary-row",STYLE,HU.css('width', dim+'px')],total);
		    table += HU.td([WIDTH,100,"valign","top"],bar);
		}
		table += HU.close(TR);
	    });
	    if(showColumnTotals) {
		table+=HU.open(TR,['valign','top']) + HU.td();
		columns.map(column=>{
		    let total = columnTotals[column.id];
		    let dim = Math.round(total/maxColumnValue*100);
		    let bar = HU.div([CLASS, "display-datatable-summary-column",STYLE,HU.css('height', dim+'px')],total);
		    table += HU.td([],bar);

		});
	    }
	    table+=HU.close(TR);
	    table+=HU.open(TR,[],HU.td());
	    table+=HU.td(['colspan',cellCount,CLASS,'display-datatable-footer','align','center',ID,this.domId(ID_COLORTABLE)]);
	    table+=HU.close(TD);
	    table+=HU.open(TR,[],HU.td());
	    table+=HU.td(['colspan',cellCount,CLASS,'display-datatable-footer','align','center',ID,this.domId(ID_PIECOLORS)]);
	    

	    table+=HU.close(TR,TABLE);

	    if(topSpace>0) {
		table  = HU.div([STYLE,HU.css('margin-top', topSpace+'px')], table);
	    }

	    let html ="";	
	    let headerRow = HU.open(TR);
	    if(this.getProperty("showRowSelector",true)) {
		headerRow+=  HU.td([CLASS,"display-datatable-selector","width","align","center"],HU.select("",[ID,this.domId("rowSelector")],
											     selectors,
												rowSelector,15));
	    }
	    if(this.getProperty("showColumnSelector",true)) {
		headerRow+=  HU.td(["colspan",columns.length, CLASS,"display-datatable-selector","width","90%","align","center"],  HU.select("",[ID,this.domId("columnSelector")],
														selectors,
														columnSelector));
	    }

	    let mainTable = HU.open(TABLE,[STYLE,HU.css('font-size', this.getProperty("fontSize",'8pt')),CLASS,'display-colorboxes-table', 'cellpadding',0,'cellspacing',0,  WIDTH,'100%']);
	    mainTable+=HU.tr([],headerRow);
	    mainTable+=table;
//	    html+=header;
	    html+=mainTable;

	    this.setContents(html);


	    let _this = this;
	    this.jq("rowSelector").change(function() {
		_this.setProperty("rowSelector",$(this).val());
		_this.updateUI();
	    });	    
	    this.jq("columnSelector").change(function() {
		_this.setProperty("columnSelector",$(this).val());
		_this.updateUI();
	    });

	    let pieWidth=this.getProperty("pieWidth", 30);

	    let colorMap = {};
	    Object.keys(uniqueValues).forEach((key,idx)=>{
		colorMap[key] = colors[idx%colors.length];
	    });



	    let xcnt = 0;
	    this.find(".display-datatable-counts").each(function() {
//		xcnt++;	if(xcnt<3|| xcnt>3) return;
		let key = $(this).attr("data-key");	
		let cell = cells[key];
		countFields.forEach((f,idx)=>{
		    let html = HU.center(HU.b(_this.getFieldLabel(f)));
		    let cf = cell.countFields[f.getId()];
		    let data=[];
		    cf.values.forEach(v=>{
			data.push([v,cf.counts[v]]);
			html+= v +":" + cf.counts[v]+SPACE + '<br>';
		    });
		    let id = _this.domId(cell.row+"-"+cell.column+"-" + f.getId());
		    $(this).append(HU.div([CLASS,"display-datatable-piechart",ID,id,TITLE,"", STYLE,HU.css(WIDTH, pieWidth+'px',HEIGHT, pieWidth+'px')]));
		    //drawPieChart(display, dom,width,height,array,min,max,colorBy,attrs) {
		    drawPieChart(_this, "#"+id,pieWidth,pieWidth,data,null,null,null,{colorMap:colorMap});
		    $("#" + id).tooltip({
			content: function() {
			    return html;
			}
		    });
		});
	    });
	    

	    this.find(".display-datatable-checked").tooltip({
		content: function() {
		    let key = $(this).attr("data-key");	
		    let cell = cells[key];
		    let checked = cell.checked;
		    if(checked.length) {
			let tooltip = _this.getProperty("tooltip","${default}");
			if(tooltip =="") return null;
			let tt = _this.getProperty("checkedTooltipHeader",HU.b('#Items: ${numberChecked}') +HU.close(BR));
			tt = tt.replace("${numberChecked}", checked.length);
			checked.map(r=>{
			    if(tt!="") tt += HU.open(DIV,[CLASS,'ramadda-hline']);
			    tt+= _this.getRecordHtml(r,null,tooltip);
			});
			return HU.div([CLASS, "display-datatable-tooltip"],tt);
		    }
		    return null;

		},
	    });
	    if(showColors) {
		this.displayColorTable(colors, ID_COLORTABLE, min,max,{});
	    }
	    if(showPieColors) {
		let stringValues = Object.keys(colorMap).map(key=>{
		    return {value:key,color:colorMap[key]};
		});
		this.jq(ID_PIECOLORS).html('pie colors');
		this.displayColorTable(colors, ID_PIECOLORS, min,max,{stringValues:stringValues});
	    }

	},
    })
}


function RamaddaSparklineDisplay(displayManager, id, properties) {
    const ID_INNER = "inner";
    if(!properties.groupBy)
	properties.displayInline = true;
    if(!Utils.isDefined(properties.showDisplayTop))
	properties.showDisplayTop = false;
    if(!Utils.isDefined(properties.showDisplayBottom))
	properties.showDisplayBottom = false;

    const SUPER =  new RamaddaFieldsDisplay(displayManager, id, DISPLAY_SPARKLINE, properties);
    let myProps = [...RamaddaDisplayUtils.sparklineProps];
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
	//Overwrite so we just have undecorated text
        getLoadingMessage: function(msg) {
	    return "Loading...";
	},
	updateUI: function() {
	    let w = this.getPropertySparklineWidth(60);
	    let h = this.getPropertySparklineHeight(20);
	    let records = this.filteredRecords = this.filterData();
	    if(!records) return;



	    let field = this.getFieldById(null, this.getProperty("field"));
	    if(field==null) {
		this.setDisplayMessage("No field specified");
		return;
	    }
	    let t1 = new Date();

	    let showDate = this.getPropertyShowDate();
	    let id = this.domId(ID_INNER);
	    let colorBy = this.getColorByInfo(records);
	    let groupByField = this.getFieldById(null,this.getProperty("groupBy"));
	    let groups = groupByField?RecordUtil.groupBy(records, this, null, groupByField):null;
	    let col = this.getColumnValues(records, field);
	    let min = col.min;
	    let max = col.max;
	    if(this.getProperty("useAllRecords")) {
		let col2 = this.getColumnValues(this.getRecords(), field);
		min  = col2.min;
		max = col2.max;
	    }

	    if(groups) {
		let labelPosition = this.getProperty("labelPosition","bottom");
		html = HU.div([ID,this.domId(ID_INNER)]);
		this.setContents(html); 
		groups.values.forEach((value,idx)=>{
		    let grecords = groups.map[value];
		    let gid = id+"_"+ +idx;
		    let c = HU.div([CLASS,"display-sparkline-sparkline",ID,gid,STYLE,HU.css('width', w+'px','height', h+  'px')]);
		    let label = HU.div([CLASS,"display-sparkline-header"], value);
		    if(labelPosition == "top")
			c = label + HU.tag(BR) + c;
		    else if(labelPosition == "bottom")
			c =  c + HU.tag(BR) + label;
		    $("#"+id).append(HU.div([STYLE,HU.css('display','inline-block','margin','4px')],c));
		    let gcol = this.getColumnValues(grecords, field);
		    drawSparkline(this, "#"+gid,w,h,gcol.values,grecords,min,max,colorBy);
		});		
	    } else {
		html = HU.div([CLASS,"display-sparkline-sparkline",ID,this.domId(ID_INNER),STYLE,HU.css('width', w+'px','height', h+'px')]);
		if(showDate) {
		    html = HU.div([CLASS,"display-sparkline-date"],this.formatDate(records[0].getTime())) + html+
			HU.div([CLASS,"display-sparkline-date"],this.formatDate(records[records.length-1].getTime()))
		}
		let left = this.getProperty("showMin")? HU.div([CLASS,"display-sparkline-value",STYLE, this.getPropertyLabelStyle("")],this.formatNumber(col.values[0])):"";
		let right = this.getProperty("showMax",true)? HU.div([CLASS,"display-sparkline-value",STYLE, this.getPropertyLabelStyle("")],this.formatNumber(col.values[col.values.length-1])):"";
		if(left!=""  || right!="")
		    html = HU.leftCenterRight(left,html,right,"1%","99%","1%",null,"padding:2px 2px;");
		this.setContents(html); 
		drawSparkline(this, "#"+id,w,h,col.values,records,min,max,colorBy);
	    }
	    let t2 = new Date();
//	    Utils.displayTimes("sparkline",[t1,t2],true);

	}
    });
}


function RamaddaPointimageDisplay(displayManager, id, properties) {
    if(!properties.width) properties.width="200";
    properties.displayInline = true;
    const SUPER =  new RamaddaFieldsDisplay(displayManager, id, DISPLAY_POINTIMAGE, properties);
    let myProps = [
	{label:'Point Image'},
	{p:'cellShape',ex:'rect|circle'},
	{p:'cellSize',ex:'4'},
	{p:'cellFilled',ex:'false'},
	{p:'cellColor',ex:'false'},
	{p:'doHeatmap',ex:'true'},
	{p:'padding',ex:'5'},
	{p:'borderColor',ex:'#ccc'},
	{p:'showTooltips',ex:'false'},
	{p:'colorBy',ex:''}
    ];
    defineDisplay(addRamaddaDisplay(this), SUPER,myProps, {
        needsData: function() {
            return true;
        },
        getLoadingMessage: function(msg) {
	    return "Loading...";
	},
	findClosest: function(records, e) {
	    let closest = null;
	    let minDistace = 0;
	    let cnt = 0;
	    let seen = {};
	    //	    console.log("find closest");
	    records.map((r,i) =>{
		let coords = r[this.getId()+"_coordinates"]
		let dx = coords.x-e.offsetX;
		let dy = coords.y-e.offsetY;
		let d = Math.sqrt(dx*dx+dy*dy);
		//		if(!seen[r.getValue(0)]) {
		//		    console.log("\t" +r.getValue(0) +" cx:" + coords.x +" cy:" + coords.y+" ex:" + e.offsetX +" ey:" + e.offsetY +" dx:" +dx +" dy:" +dy +" d:" + d);
		//		    seen[r.getValue(0)]  =true;
		//		}
		if(i==0) {
		    closest = r;
		    minDistance=d;
		} else {
		    if(d<minDistance) {
			minDistance = d;
			closest=r;
			//			console.log("\tclosest:" + minDistance +" " + r.getValue(0));
		    }
		}
	    });
	    return closest;
	},
	updateUI: function() {
	    let records = this.filterData();
	    if(!records) return;
	    if(this.getProperty("borderColor")) {
		$("#"+this.getProperty(PROP_DIVID)).css("border","1px solid " + this.getProperty("borderColor"));
	    }
	    let bounds ={};
	    RecordUtil.getPoints(records, bounds);
	    let ratio = (bounds.east-bounds.west)/(bounds.north-bounds.south);
	    let style = this.getProperty("padding")?HU.css('padding',+this.getProperty("padding")+"px") : "";
	    let html = HU.div([ID,this.domId("inner"),STYLE,style]);
	    this.setContents(html); 
	    let pad = 10;
	    let w = Math.round(this.jq("inner").width());
	    let h = Math.round(w/ratio);
            let divid = this.getProperty(PROP_DIVID);
	    //	    $("#"+ divid).css("height",h+pad);
	    html = HU.div([ID,this.domId("inner"),STYLE,HU.css('width', w +'height', h+'px') + style]);
	    html = HU.div([ID,this.domId("inner")]);
	    //this.getContents().css("height",h+pad);
	    this.setContents(html); 
	    let colorBy = this.getColorByInfo(records);
	    bounds = RecordUtil.expandBounds(bounds,0.1);
	    let args =$.extend({colorBy:colorBy, w:w, h:h,cell3D:this.getProperty("cell3D"),bounds:bounds},
			       this.getDefaultGridByArgs());

	    //The default gridby args sets operator=count
	    args.operator = this.getProperty('hm.operator',this.getProperty('hmOperator','max')),
	    args.doHeatmap=true;
	    let fields = this.getFields();
	    let img = Gfx.gridData(this.getId(),fields, records,args);
	    this.jq("inner").html(HU.image(img,[TITLE,"",ID,this.domId("image")]));
	    this.jq("inner").append(HU.div([ID,this.domId("tooltip"),STYLE,HU.css('z-index:','2000','display','none','position','absolute','background','#fff','border','1px solid #ccc','padding','0px')]));
	    let _this = this;
	    if(this.getProperty("showTooltips",true)) {
		this.jq("image").mouseout(function( event ) {
		    _this.jq("tooltip").hide();
		});
		this.jq("image").mousemove(function( event ) {
		    let closest = _this.findClosest(records,event);
		    if(closest) {
			let html =  HU.div([STYLE,HU.css('max-height','400px','overflow-y','auto')], _this.getRecordHtml(closest));
			_this.jq("tooltip").html(html);
			_this.jq("tooltip").show();
		    }
		});
	    }
	    this.jq("image").click(e=> {
		_this.mouseEvent = event;
		let closest = this.findClosest(records,e);
		if(closest)
		    this.propagateEventRecordSelection({record: closest});
	    });

	}
    });
}


function RamaddaCanvasDisplay(displayManager, id, properties) {
    const SUPER =  new RamaddaFieldsDisplay(displayManager, id, DISPLAY_CANVAS, properties);
    let myProps = [
	{label:'Canvas'},
	{p:'canvasStyle',d:"",ex:"",tt:'Canvas CSS style'},
	{p:'titleTemplate',tt:'Template to show as title'},
	{p:'topTitleTemplate',tt:'Template to show as top title'},	
	{p:'urlField',tt:'Url Field'},
	{p:'iconField',tt:'Icon Field'},
	{p:'highlightStyle',tt:'Highlight Style'},
	{p:'unHighlightStyle',tt:'Unhighlight Style'},	
    ];


    myProps.push(...RamaddaDisplayUtils.getCanvasProps());


    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
        needsData: function() {
            return true;
        },
	updateUI: function() {
	    let _this = this;
	    let records = this.filterData();
	    let fields = this.getFields();
	    if(!records) return;
	    if(records.length==0) {
		this.setDisplayMessage(this.getNoDataMessage());
		return;
	    }
	    let style = this.getCanvasStyle("");
	    let highlightStyle = this.getHighlightStyle("");
	    let unHighlightStyle = this.getUnHighlightStyle("");
	    let columns = this.getProperty("columns");
	    let html = "";
	    let canvasWidth = this.getPropertyCanvasWidth();
	    let canvasHeight = this.getPropertyCanvasHeight();
	    let titleTemplate= this.getPropertyTitleTemplate();
	    let topTitleTemplate= this.getPropertyTopTitleTemplate();
	    let urlField = this.getFieldById(null,this.getPropertyUrlField());
	    let iconField = this.getFieldById(null,this.getPropertyIconField());	    
	    let doingHighlight = this.getFilterHighlight();
	    records.forEach((record,idx)=>{
		let highlight =  record.isHighlight(this);
		let cid = this.domId("canvas_" + idx);
		let canvasClass = "display-canvas-canvas";
		let canvasStyle = style;
		if(doingHighlight) {
		    if(highlight) {
			canvasClass+= " display-canvas-canvas-highlight ";
			canvasStyle+= " " + highlightStyle;
		    } else {
			canvasClass+= " display-canvas-canvas-unhighlight ";
			canvasStyle+= " " + unHighlightStyle;
		    }
		}

		let c = HU.tag("canvas",[CLASS,canvasClass, STYLE,canvasStyle, 	
				 WIDTH,canvasWidth,HEIGHT,canvasHeight,ID,cid]);
		let icon = iconField? HU.image(record.getValue(iconField.getIndex()))+"&nbsp;":"";
		let topTitle  = topTitleTemplate?
		    HU.div([CLASS,"display-canvas-title"],
			   icon+this.getRecordHtml(record, null, topTitleTemplate)):icon;
		let title  = titleTemplate?
		    HU.div([CLASS,"display-canvas-title"], 
			   this.getRecordHtml(record, null, titleTemplate)):"";	
		let div =  HU.div([TITLE,"",CLASS,"display-canvas-block", RECORD_INDEX,idx,RECORD_ID, record.getId()], topTitle+c+title);
		if(urlField) {
		    let url = record.getValue(urlField.getIndex());
		    if(Utils.stringDefined(url))
			div = HU.href(url,div);
		}
		html+=div;
	    });
	    this.setContents(html);
	    let glyphs=RamaddaDisplayUtils.getGlyphs(this,fields,records,canvasWidth,canvasHeight);
	    let opts = {};
	    let originX = 0;
	    let originY=this.getPropertyCanvasOrigin()=="center"?canvasHeight/2:canvasHeight;
	    records.forEach((record,idx)=>{
		let cid = this.domId("canvas_" + idx);
		let canvas = document.getElementById(cid);
		let ctx = canvas.getContext("2d");
		glyphs.forEach(glyph=>{
		    glyph.draw(opts, canvas, ctx, originX,originY,{record:record});
		});
	    });
	    let blocks = this.find(".display-canvas-block");
	    this.makeTooltips(blocks,records,null,"${default}");
	    
	    if(this.getShowColorTable()) {
		glyphs.every(glyph=>{
		    let colorBy = glyph.getColorByInfo();
		    if(colorBy) {
			colorBy.displayColorTable();
			//		    return false;
		    }
		    return true;
		});
	    }




	}
    });
}


function RamaddaFieldtableDisplay(displayManager, id, properties) {
    const ID_TABLE = "fieldtable";
    const SUPER =  new RamaddaFieldsDisplay(displayManager, id, DISPLAY_FIELDTABLE, properties);
    let myProps = [
	{label:'Field Table'},
	{p:'field',ex:''},
	{p:'labelField',ex:'field'},
	{p:'columnWidth',ex:'150'},
	{p:'tableHeight',ex:'300'},
	{p:'markerShape',ex:'circle|rect|triangle|bar|arrow|dart|bar'},
	{p:'markerSize',ex:'16'},
	{p:'markerFill',ex:'#64CDCC'},
	{p:'markerStroke',ex:'#000'}
    ];
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
        needsData: function() {
            return true;
        },
	updateUI: function() {
	    let records = this.filterData();
	    if(!records) return;
	    let fields = this.getFieldsByIds(null,this.getPropertyFields());
	    if(fields.length==0) 
		fields = this.getFieldsByType(null, "numeric");
	    let labelField = this.getFieldById(null, this.getProperty("labelField"));
	    if(!labelField) labelField = this.getFieldsByType(null, "string")[0];
	    let html = HU.open(TABLE,[CLASS, "", "border",0,ID,this.domId(ID_TABLE)]);
	    html += HU.open(THEAD);
	    let width = this.getProperty("columnWidth",150)
	    html += HU.open(TR,[]);
	    html+=HU.td(["width",width],
			HU.div([CLASS,"display-fieldtable-header"],labelField?this.getFieldLabel(labelField):""));
	    let columns = {};
	    fields.forEach(f=>{
		columns[f.getId()] = this.getColumnValues(records, f);
	    });

	    fields.forEach(f=>{
		html+=HU.th(["width",width],HU.div([CLASS,"display-fieldtable-header"],this.getFieldLabel(f)));
	    });
	    html += HU.close(TR,THEAD);
	    html += HU.open(TBODY);


	    let shape = this.getProperty("markerShape","bar");
	    let canvasInfo = [];
	    let colorBys = {};
	    let cnt = 0;
	    let cw = this.getProperty("markerSize",16);

	    fields.forEach(f=>{
		colorBys[f.getId()] = this.getColorByInfo(records,f);
	    });

	    records.forEach((r,idx)=>{
		let label  = labelField?r.getValue(labelField.getIndex()):"#"+(idx+1);
		let hdrAttrs = [CLASS,"display-fieldtable-rowheader"];
		if(labelField) {
		    hdrAttrs.push("field-id");
		    hdrAttrs.push(labelField.getId());
		    hdrAttrs.push("field-value");
		    hdrAttrs.push(r.getValue(labelField.getIndex()));
		}
		html += HU.open(TR,["valign","center",RECORD_INDEX,idx,RECORD_ID, r.getId(),CLASS,"display-fieldtable-row"]);
		html+=HU.td([STYLE,HU.css('vertical-align','center'),'align','right'],HU.div(hdrAttrs,label));
		fields.forEach(f=>{
		    let v = r.getValue(f.getIndex());
		    let c = columns[f.getId()];
		    let contents = "";
		    if(isNaN(v) || c.min == c.max) return;
		    let perc = 100*(v-c.min)/(c.max-c.min);
		    let cid = this.domId("cid" + (cnt++));
		    let cinfo = {
			id: cid,
			v: v,
			percent: perc,
			field:f,
			record:r,
			colorBy: colorBys[f.getId()]
		    };
		    canvasInfo.push(cinfo);
		    let canvasWidth = cw;
		    let left =  perc+"%";
		    if(shape == "bar") {
			canvasWidth = perc*width;
			left = 0;
		    }
		    let cstyle = HU.css('position','absolute','top','0%','left',left,'margin-top','-' + (cw/2)+'px');
		    let inner = HU.tag("canvas",[TITLE,"Value:" + v +"   Range:" + c.min +" - " + c.max,STYLE,cstyle, 
						 "width",canvasWidth,"height",cw,ID,cid]);
		    contents +=HU.div([STYLE,HU.css('position','absolute','left','0px','right', cw+'px')],
				      inner);
		    html+=HU.td(["data-order", v, STYLE,HU.css('vertical-align','middle'),ALIGN,"right",TITLE, "Range:" + c.min +" - " + c.max],HU.div([STYLE,"position:relative;width:"+width+"px;" + "height:1px;margin-left:10px; margin-right:10px;border:1px solid #ccc;"],contents));
		    
		});
		html += HU.close(TR);
	    });

	    html += HU.close(TBODY);
	    html += HU.open(TFOOT);
	    html+=HU.open(TR);
	    html+=HU.td([],"");
	    fields.forEach((f,idx)=>{
		html+=HU.td([],HU.div([STYLE,HU.css('max-width', width+'px','overflow-x','auto'),ID, this.domId("footer-" + idx)],""));
	    });
	    html+=HU.close(TR);
	    html += HU.close(TFOOT);

	    html+=HU.close(TABLE);
	    this.setContents(html); 
	    let opts = {
		ordering:true
	    };
	    if(this.getProperty("tableHeight")) {
		opts.scrollY = this.getProperty("tableHeight");
	    }
	    if(this.getProperty("showColorTable")) {
		fields.forEach((f,idx)=>{
		    let colorBy = colorBys[f.getId()];
		    if(colorBy.index<0) return;
		    let domId = "footer-" + idx;
		    colorBy.displayColorTable(null,false,domId);
		});
	    }


            HU.formatTable("#" + this.domId(ID_TABLE), opts);
	    let rows = this.find(".display-fieldtable-row");
	    this.addFieldClickHandler(null, records,true);
	    let markerFill = this.getProperty("markerFill","#64CDCC");
	    let markerStroke = this.getProperty("markerStroke","#000");
	    canvasInfo.forEach(c=>{
		let canvas = document.getElementById(c.id);
		let ctx = canvas.getContext("2d");
		ctx.strokeStyle =markerStroke;
		ctx.fillStyle=c.colorBy.getColorFromRecord(c.record, markerFill);
		if(shape=="circle") {
		    ctx.beginPath();
		    ctx.arc(cw/2, cw/2, cw/2, 0, 2 * Math.PI);
		    ctx.fill();
		    ctx.stroke();
		} else if(shape=="rect") {
		    ctx.fillRect(0,0,cw,cw);
		    ctx.strokeRect(0,0,cw,cw);
		} else if(shape=="bar") {
		    ctx.fillRect(0,0,c.percent/100*width,cw);
		    ctx.strokeRect(0,0,c.percent/100*width,cw);
		} else if(shape=="line") {
		    ctx.fillRect(cw/2-2,0,4,cw);
		} else if(shape=="triangle") {
		    ctx.beginPath();
		    ctx.moveTo(cw/2,0);
		    ctx.lineTo(cw,cw);
		    ctx.lineTo(0,cw);
		    ctx.lineTo(cw/2,0);
		    ctx.fill();
		    ctx.stroke();
		} else if(shape=="dart") {
		    ctx.beginPath();
		    ctx.moveTo(1,0);
		    ctx.lineTo(cw-1,0);
		    ctx.lineTo(cw/2,cw/2);
		    ctx.lineTo(1,0);
		    ctx.fill();
		    ctx.stroke();
		} else if(shape=="arrow") {
		    ctx.beginPath();
		    ctx.moveTo(0,0);
		    ctx.lineTo(cw/2,cw/2);
		    ctx.lineTo(cw-1,0);
		    ctx.stroke();

		}
		
	    });

	}
    });
}


function RamaddaSelectedrecordsDisplay(displayManager, id, properties) {
    const ID_RECORDS = "selectedrecords";
    const SUPER =  new RamaddaFieldsDisplay(displayManager, id, DISPLAY_SELECTEDRECORDS, properties);
    let myProps = [
	{label:'Selected Records'},
	{p:'labelField',ex:'field'},
    ];
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
        needsData: function() {
            return true;
        },
	updateUI: function() {
	    if(!this.recordList) {
		this.recordList=[];
	    }
	    if(this.recordList.length==0) {
		this.setContents('');
		return;
	    }
	    let records = this.recordList;
	    let fields = this.getFieldsByIds(null,this.getPropertyFields());
	    if(fields==null) return;
	    if(fields.length==0) 
		fields = this.getFields();
	    if(fields==null) return;
	    let notFields = this.getFieldsByIds(null,this.getProperty("notFields"));
	    if(notFields) {
		let not = {};
		notFields.forEach(f=>{not[f.getId()]=true;});
		fields=fields.filter(f=>{
		    return !not[f.getId()];
		});

	    }
	    let labelField = this.getFieldById(null, this.getLabelField());
	    let html ='';
	    html+=HU.div(['id',this.domId('download')],"Download Data");
	    html+=HU.openTag('table',[CLASS,"ramadda-table stripe row-border", 'xwidth','100%',ID,this.domId(ID_RECORDS)]);
	    this.idToRecord = {};
	    this.csv =[];
	    if(labelField) {
		let row = [];
		this.csv.push(row);
		html+=HU.open('thead');
		row.push("Field");
		html+=HU.th(['align','center'],'Field');

		records.forEach(record=>{
		    this.idToRecord[record.getId()] = record;
		    let v = labelField.getValue(record);
		    row.push(v);
		    let div = HU.div(['class','ramadda-clickable display-selectedrecords-header',
				      'title','Click to remove','data-record-id',record.getId()],
				     v);
		    html+=HU.th(['align','center'],div);
		});
		html+=HU.close('thead');
	    }
	    html+=HU.open('tbody');
	    fields.forEach((f,idx)=>{
		if(labelField && labelField.getId()==f.getId()) return;
		html+=HU.open('tr');
		let row = [];
		this.csv.push(row);
		records.forEach((record,idx) =>{
		    if(idx==0) {
			row.push(f.getLabel());
			html+=HU.td(['width','20%'], HU.b(f.getLabel()));
		    }
		    let attrs=[];
		    let v = f.getValue(record);
		    row.push(v);
		    if(!isNaN(v)) attrs.push('align','right');
		    else if(String(v)==="NaN") {
			v="--";
			attrs.push('align','right');			
		    }			
		    html+=HU.td(attrs,v);
		});
		html+=HU.close('tr');		
	    });
	    html += HU.close(TBODY,TABLE);
	    this.setContents(html); 
	    this.jq('download').button().click(()=>{
		let c='';
		this.csv.forEach(row=>{
		    c+=Utils.join(row,',');
		    c+='\n';
		});
		Utils.makeDownloadFile(this.getProperty('downloadFile','data.csv'),c);
	    });


	    let opts ={};
	    if(this.getProperty("tableHeight")) {
		opts.scrollY = this.getProperty("tableHeight");
	    }
	    let _this = this;
	    let initTable = ()=>{
		//Look for headers. There are doubles because of DataTables
		let headers =_this.getContents().find('.display-selectedrecords-header');
		headers.click(function() {
		    //For some reason the div is the parent
		    let div = $(this).parent();		    
		    //A bit of a hack. For some reason the element can't access the record-id attribute
		    //So we parse it out from the html
		    let recordId = div.attr('data-record-id');
		    if(!recordId) {
			let match  = div.html().match(/data-record-id="(.*)"/);
			if(match) {
			    recordId = match[1];
			}
		    }
		    if(!recordId) return;
		    let record = _this.idToRecord[recordId];
		    if(!record) return;
		    if(_this.seenRecords)
			delete _this.seenRecords[record.getId()];
	 	    _this.recordList= Utils.removeItem(_this.recordList,record);
		    _this.updateUI();
		});
	    };
            HU.formatTable("#" + this.domId(ID_RECORDS), opts,initTable);
	},

        handleEventRecordSelection: function(source, args) {
	    SUPER.handleEventRecordSelection.call(this, source, args);
	    if(!this.seenRecords)
		this.seenRecords={};
	    if(!this.recordList) 
		this.recordList=[];
	    if(this.seenRecords[args.record.getId()]) return;
	    this.seenRecords[args.record.getId()]=true;
	    this.recordList.push(args.record);
	    this.updateUI();
	}
	
    });
}
		     


function RamaddaDotstackDisplay(displayManager, id, properties) {
    const SUPER =  new RamaddaFieldsDisplay(displayManager, id, "dotstack", properties);
    let myProps = [
	{label:'Dot Stack'},
	{p:'categoryField',ex:'field'},
    ];
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
        needsData: function() {
            return true;
        },
	updateUI: function() {
	    let records = this.filterData();
	    if(!records) return;
	    let idToIndex = {};
	    records.forEach((r,idx)=>{
		idToIndex[r.getId()] = idx;
	    });
	    let hor = this.getProperty("orientation","horizontal") == "horizontal";
	    let categoryField = this.getFieldById(null, this.getProperty("categoryField"));
	    let html = "";
	    let groups = RecordUtil.groupBy(records, this, null, categoryField);
	    let colorBy = this.getColorByInfo(records);
	    let w = this.getProperty("boxWidth",4);
	    let cols = this.getProperty("boxColumns",10);
	    let xcnt = 0;
	    groups.values.sort((a,b)=>{
		return groups.map[b].length-groups.map[a].length;
	    });

	    groups.values.forEach((value,idx)=>{
		let rows = [];
		let row = [];
		rows.push(row);
		let grecords = groups.map[value];
		let col=0;
		grecords.forEach(r=>{
		    if(row.length>cols) {
			row=[];
			rows.push(row);
		    }
		    let c = colorBy.getColorFromRecord(r,"blue");
		    let box = HU.div(
			[TITLE,"", RECORD_ID, r.getId(),RECORD_INDEX,idToIndex[r.getId()],CLASS, "display-dotstack-dot",STYLE,HU.css('width', w+'px','height', w +'px','background', c)],"");
		    row.push(box);
		});
		html += HU.open(DIV,[CLASS,"display-dotstack-block"]);
		html+=HU.div([],this.getProperty("labelTemplate","${count}").replace("${count}", grecords.length));
		html += HU.open(TABLE);
		for(let i=rows.length-1;i>=0;i--) {
		    html += HU.tr([],HU.tds([],rows[i]));
		}
		html += HU.close(TABLE);
		html +=value;
		html += HU.close(DIV);
	    });
	    this.setContents(html); 
	    let dots = this.find(".display-dotstack-dot");
	    this.addFieldClickHandler(dots,records,false);
	    this.makeTooltips(dots,records,null,"${default}");
	    if(this.getProperty("tableHeight")) {
		opts.scrollY = this.getProperty("tableHeight");
	    }
	    if(this.getProperty("showColorTable")) {
		colorBy.displayColorTable(null,false,domId);
	    }
	}
    });
}


function RamaddaDotbarDisplay(displayManager, id, properties) {
    const SUPER =  new RamaddaFieldsDisplay(displayManager, id, "dotbar", properties);
    $.extend(this, SUPER);
    addRamaddaDisplay(this);
    let myProps = [
	{label:'Dot Bar'},
	{p:'keyField'},
	{p:'dotSize',d:16}
    ];
    this.defineSizeByProperties();
    defineDisplay(this, SUPER, myProps, {
        needsData: function() {
            return true;
        },
	updateUI: function() {
	    let records = this.filterData();
	    if(!records) return;
	    let keyField = this.getFieldById(null,this.getPropertyKeyField());
	    let fields = this.getFieldsByIds(null,this.getPropertyFields());
 	    if(fields.length==0) {
		fields = this.getPointData().getRecordFields();
	    }
	    let dotSize = this.getPropertyDotSize();
	    let sizeBy = new SizeBy(this, this.getProperty("sizeByAllRecords",true)?this.getData().getRecords():records);
	    let size = dotSize;
	    let cols = {};
	    let html = HU.open(TABLE,['width','100%']);
	    let t1 = new Date();
	    let selectedRecord;
	    let maxHeight = dotSize;
	    if(sizeBy.field)
		maxHeight=2*sizeBy.getMaxSize();
	    fields.forEach((f,idx)=>{
		if(!f.isFieldNumeric()) return;


		let cb = new  ColorByInfo(this,  fields, records, null,null, null, null,f);
		let cid = this.domId("dots"+idx);
		let column = this.getColumnValues(records, f);
		html += HU.open(TR, [VALIGN,'center']);
		html += HU.td([WIDTH,'10%', ALIGN,'right'],  HU.div([STYLE,HU.css('margin-right','8px')], this.getFieldLabel(f).replace(/ /g,SPACE)));
		html += HU.td([ALIGN,'right',WIDTH,'5%'],HU.div([STYLE, 'margin-right:10px;'],this.formatNumber(column.min)));
		html +=HU.open(TD);
		html+= HU.open(DIV,[STYLE, HU.css(HEIGHT,HU.getDimension(maxHeight), WIDTH,'100%','position','relative','margin-top','4px')]);
		html+=HU.div([STYLE,HU.css('position','absolute','left','0px','right','0px','top','50%','border-top','1px solid #ccc')]);
		html+=SPACE;
		records.forEach((r,idx2)=>{
		    let v = r.getValue(f.getIndex());
		    let c = cb.getColor(v,r);
		    let darkC = Utils.pSBC(-0.25,c);
		    if(column.min == column.max) return;
		    let perc = (v-column.min)/(column.max-column.min);
		    let clazz = 'display-dotbar-dot';
		    let selected = false;
		    let style = "";
		    if(keyField && this.selectedKey) {
			if(this.selectedKey == r.getValue(keyField.getIndex())) {
			    selected = true;
			}
		    } else if(idx2==this.selectedIndex) {
			selected = true;
		    }
		    let dotBorder = "2px solid " + darkC;
		    if(this.selectedIndex>=0) {
			if(!selected) {
			    dotBorder = "1px solid " + darkC;
			    c = "rgba(200,200,200,0.2)";
			} else {
			    dotBorder = "1px solid #000";
			}
		    }
		    if(selected) {
			selectedRecord = r;
			clazz += " display-dotbar-dot-select";
		    } else {
			if(this.getFilterHighlight()) {
			    if(!r.isHighlight(this)) {
				style = HU.css('z-index','10','border','1px solid #aaa');
			    }
			}
		    }
		    perc *=100;
		    let size = dotSize;
		    if(sizeBy.field) {
			size  = 2*sizeBy.getSize(r.getData(), dotSize);
			if(size<0) return;
			style+=HU.css(HEIGHT,HU.getDimension(size),WIDTH,HU.getDimension(size));
		    }
		    let top = maxHeight/2-size/2;
		    html +=  HU.span([RECORD_INDEX,idx2,RECORD_ID, r.getId(),CLASS,clazz,STYLE,HU.css('border',dotBorder, "background",c,"position",'absolute','top',HU.getDimension(top),'left', perc+'%')+style, RECORD_INDEX,idx2, TITLE,""]); 
		});

		html += HU.close(DIV,TD);
		html += HU.td([WIDTH, (dotSize*2)]);
		html += HU.td([ALIGN,"right", WIDTH,"5%"],HU.div([STYLE,HU.css('margin-left','10px')],this.formatNumber(column.max)));
		html+=HU.close(TR);
	    });
	    let t2 = new Date();
	    html+=HU.close(TABLE);
	    this.setContents(html); 
	    let t3 = new Date();
	    let dots = this.find(".display-dotbar-dot");
	    let t4 = new Date();
	    let _this = this;
	    dots.mouseleave(function() {
		dots.removeClass("display-dotbar-dot-highlight");
	    });
	    dots.mouseover(function() {
		let idx = $(this).attr(RECORD_INDEX);
		dots.removeClass("display-dotbar-dot-highlight");
		_this.find("[" + RECORD_INDEX+"=\"" + idx+"\"]").addClass( "display-dotbar-dot-highlight");
	    });
	    dots.click(function() {
		let idx = $(this).attr(RECORD_INDEX);
		let record = records[idx];
		if(!record) return;
		dots.removeClass("display-dotbar-dot-select");
		if(_this.selectedIndex ==  idx) {
		    _this.selectedIndex =  -1;
		    _this.updateUI();
		    return;
		}
		_this.selectedIndex =  idx;
		if(keyField)
		    _this.selectedKey = record.getValue(keyField.getIndex());
		_this.find("[" + RECORD_INDEX+"=\"" + idx+"\"]").addClass( "display-dotbar-dot-select");
		_this.hadClick = true;
		_this.propagateEventRecordSelection({record: record});
		_this.updateUI();
	    });	    //Do this later so other displays get this after they apply their data filter change
	    if(selectedRecord){
		setTimeout(()=>{
		    this.propagateEventRecordSelection({record: selectedRecord});
		},10);
	    }
	    this.makeTooltips(dots,records,null);
	    let t5 = new Date();
	    //	    Utils.displayTimes("t",[t1,t2,t3,t4,t5]);

	}
    });
}




function RamaddaDategridDisplay(displayManager, id, properties) {
    const SUPER =  new RamaddaFieldsDisplay(displayManager, id, DISPLAY_DATEGRID, properties);
    $.extend(this, SUPER);
    addRamaddaDisplay(this);
    let myProps = [
	{label:'Date Box'},
	{p:'groupField'},
	{p:'boxSize',d:16},
	{p:'showStats',ex:'true',d:true,tt:'show starts per row'},
	{p:'showTotal',ex:'true',d:true,tt:'show the totals'},
	{p:'showMin',ex:'true',d:true,tt:'show min'},
	{p:'showMax',ex:'true',d:true,tt:'show max'},
	{p:'showAverage',ex:'true',d:false,tt:'show average'},				
	{p:'leftWidth',tt:'width of left column',d:'100px'},
	{p:'rightWidth',tt:'width of right column',d:'100px'},
 	{p:'leftLabel',tt:'Label for the left column'},
 	{p:'rightLabel',tt:'Label for the left column',d:'Total/Min/Max'},	
	{p:'dateHeaderStyle',tt:'Style to use for the date header'},
	{p:'dateStride',d:-1,tt:'The stride in hours to display the date label'},	
	{p:'numLabels',d:8,tt:'Hour many date labels to show if no dateStride given'},	
	{p:'boxStyle',tt:'Style to use for color boxes'},
	{p:'leftStyle',tt:'Style to use for left column'},
	{p:'rightStyle',tt:'Style to use for right column'},			

    ];
    this.defineSizeByProperties();
    defineDisplay(this, SUPER, myProps, {
	updateUI: function() {
	    let records = this.filterData();
	    if(!records) return;
	    let groupField = this.getFieldById(null,this.getPropertyGroupField());
	    let size = this.getBoxSize();
	    let cats =[];
	    let colorBy = this.getColorByInfo(records);
	    let minDate= null, maxDate=null;
	    records.forEach(r=>{
		if(!minDate) {
		    minDate = maxDate = r.getDate();
		} else {
		    let d = r.getDate();
		    minDate = d.getTime()<minDate.getTime()?d:minDate;
		    maxDate = d.getTime()>maxDate.getTime()?d:maxDate;		    
		}
		let v = r.getValue(groupField.getIndex());
		let cat = cats[v];
		if(cat == null) {
		    cat = {
			records:[]
		    };
		    cats[v] = cat;
		}
		cat.records.push(r);
	    });
	    if(!this.dateFormat)
		this.dateFormat =  this.getProperty("dateFormat", "ddd mm/dd");
	    let showStats = this.getShowStats();
	    let leftWidth = HU.getDimension(this.getLeftWidth());
	    let rightWidth = HU.getDimension(this.getRightWidth());	    
	    let html = "";
	    let width = 400;
	    let dateRange = maxDate.getTime()-minDate.getTime();

	    let scaleX = d=>{
		return  (d.getTime()-minDate.getTime())/dateRange;
	    };
	    let height = "1.5em";
	    html="<div class=display-dategrid-table><table width=100% border=0 cellpadding=0 cellspacing=0>";

	    html+="<tr><td width='" + leftWidth+"'>" + HU.div([CLASS,"display-dategrid-header"],this.getLeftLabel(this.getFieldLabel(groupField))) +"</td>";
	    let dateHeaderStyle = this.getDateHeaderStyle("background:#eee;border-bottom:1px solid #888;");
	    let boxStyle = this.getBoxStyle("");
	    let leftStyle = this.getLeftStyle("");
	    let rightStyle = this.getRightStyle("");	    	    
	    let dateHeader = HU.open("div",[CLASS,'display-dategrid-dateheader',STYLE,dateHeaderStyle]) + SPACE;
	    let date  = minDate;
	    let dateStride = this.getDateStride(-1);
	    let dateDelta;
	    if(dateStride>0) {
		dateDelta = dateStride*1000*60*60;
	    } else {
		let hours = Math.round((maxDate.getTime()- minDate.getTime())/1000/60/60);
		let numLabels = this.getNumLabels();
		let hoursPerLabel =Math.round(hours/numLabels);
		dateDelta = hoursPerLabel*1000*60*60;
	    }
	    let rem =  minDate.getTime()%dateDelta;
	    date = new Date(minDate.getTime()-rem+dateDelta);

	    while(date.getTime()<=maxDate.getTime()) {
		let perc = (100*scaleX(date))+"%";
		let style = HU.css("left",perc,"top","0%","transform","translate(-50%, 0%)");
		dateHeader+=HU.div([CLASS,"display-dategrid-header display-dategrid-date",STYLE,style],this.formatDate(date))+"\n";

		date = new Date(date.getTime() +dateDelta);
	    }
	    dateHeader +="</div>";

	    html+=HU.td([],dateHeader);
	    if(showStats) {
		let dflt = [];
		if(this.getShowTotal()) dflt.push("Total");
		if(this.getShowMin()) dflt.push("Min");
		if(this.getShowMax()) dflt.push("Max");
		if(this.getShowAverage()) dflt.push("Avg");
		html+="<td width='" + rightWidth + "'>" + HU.div([CLASS,"display-dategrid-header display-dategrid-stats"], this.getRightLabel(Utils.join(dflt,"/"))) +"</td>";
	    }
	    html +="</tr>"
	    Object.keys(cats).sort(v=>{
		let cat = cats[v];
		let row = HU.open("div",[CLASS,"display-dategrid-row", STYLE,HU.css('height',height)]);
		let sorted = cat.records.sort((a,b)=>{
		    return a.getTime()-b.getTime();
		});
		let total = 0;
		let min=NaN;
		let max=NaN;
		
		for(let i=0;i<sorted.length;i++) {
		    let r = sorted[i];
		    let perc = scaleX(r.getDate());
		    let next = sorted[i+1];
		    let boxWidth="10p";
		    let right = perc+0.05;
		    if(next) {
			let nperc = scaleX(next.getDate());
			let diff = nperc - perc;
			right = (1-nperc);
		    }
		    perc = 100*perc+"%";
		    right = 100*right+"%";
		    let color =  colorBy.getColorFromRecord(r);
		    let cv = r.getValue(colorBy.index);
		    if(!isNaN(cv)) {
			total+=cv;
			min = Utils.min(min,cv);
			max = Utils.max(max,cv);			
		    }
		    row+=HU.div(["foo","bar", RECORD_ID,r.getId(),CLASS,"display-dategrid-box",TITLE,cv,STYLE,HU.css("left",perc,"right",right, "height",height,"background",color)+boxStyle],"&nbsp;");
		}
		row+="</div>\n";
		html+="<tr><td width='"+ leftWidth+"'>" +HU.div([STYLE,leftStyle,CLASS,"display-dategrid-rowlabel"], v)+"</td><td>" + row +"</td>"
		if(showStats) {
		    let stats = [];
		    if(this.getShowTotal())
			stats.push(this.formatNumber(total));
		    if(this.getShowMin())
			stats.push(this.formatNumber(min,null));
		    if(this.getShowMax())
			stats.push(this.formatNumber(max));


		    if(this.getShowAverage())
			stats.push(this.formatNumber(total/sorted.length));		    		    		    
		    html+=HU.td(["nowrap","true"],HU.div([STYLE, rightStyle,CLASS,"display-dategrid-stats"],Utils.join(stats,SPACE)));
		}
		html+="</tr>";
	    });
	    html += "</table></div>";
	    this.setContents(html); 
	    this.boxes = this.find(".display-dategrid-box");
	    this.addFieldClickHandler(this.boxes, records,false);
	    this.makeTooltips(this.boxes,records,null);
	    this.recordMap = this.makeIdToRecords(records);
	    this.records = records;
	    colorBy.displayColorTable();
	},
        handleEventRecordSelection: function(source, args) {
	    SUPER.handleEventRecordSelection.call(this, source, args);
	    if(!this.boxes) {
		return;
	    }
	    let matched = [];
	    let record = this.recordMap[args.record.getId()];
	    if(record) {
		matched.push(record);
	    } else {
		matched = this.findMatchingDates(args.record.getDate(), this.filteredRecords);
	    }
	    if(matched.length==0) {
		console.log("none");
		return;
	    }
	    this.boxes.removeClass("display-dategrid-box-highlight");
	    let boxMap ={};
	    this.boxes.each(function() {
		boxMap[$(this).attr(RECORD_ID)] = $(this);
	    });
	    matched.forEach(record=>{
		let box =  boxMap[record.getId()];
		if(box) box.addClass("display-dategrid-box-highlight");
	    });


	}
    });
}



function RamaddaStripesDisplay(displayManager, id, properties) {
    const SUPER =  new RamaddaFieldsDisplay(displayManager, id, DISPLAY_STRIPES, properties);
    let myProps = [
	{label:'Stripes'},
	{p:'colorBy',ex:'',tt:'Field id to color by'},
	{p:'stripeHeight',d:'100px'},
	{p:'stripeWidth',d:3,tt:'Make sure this is a whole number'},
	{p:'showLabel',d:false,ex:'true'},
	{p:'showLegend',d:false,ex:'true'},
	{p:'showColorTable',d:false,ex:'true'}	,
	{p:'showColorTableBottom',d:false,ex:'true'},
	{p:'groupBy',tt:'Field id to group by'},
	{p:'showSparkline',ex:'true'},
    ];
    myProps.push(...RamaddaDisplayUtils.sparklineProps);

    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
        checkLayout: function() {
	    //This gets called when the display is in a tab
	    //If we are showing sparklings then redraw
	    if(this.getShowSparkline()) {
		this.updateUI();
	    }
	},
        updateUI: function() {
	    let allRecords = this.filterData();
	    if(!allRecords) return;
	    let fields = this.getFieldsByIds(null,this.getPropertyFields());
	    if(fields.length==0) {
                this.handleError("No field specified");
		return;
	    }

	    let groupField = this.getFieldById(null,  this.getGroupBy());
	    let groupedRecords={};
	    if(groupField) {
		allRecords.forEach((record,idx)=>{
		    let value = groupField.getValue(record);
		    if(!groupedRecords[value]) {
			groupedRecords[value] = [];
		    }
		    groupedRecords[value].push(record);
		});
	    } else {
		groupedRecords['all'] = allRecords;
	    }
	    let allFields = this.getFields();
	    let stripeHeight=this.getStripeHeight();
	    let recordMap={};
	    allRecords.forEach(record=>{
		recordMap[record.getId()]  =record;
	    });
	    let html = '<center><div style="display:inline-block;">';
	    let colorBys = [];
	    let stripeWidth=this.getStripeWidth();
	    let groups = Object.keys(groupedRecords);

	    let makeLegend = ()=>{
		let left =this.formatDate(allRecords[0].getDate());
		if(this.getShowLabel()) {
		    left  = HU.div(['style','margin-left:2em;'], left);
		}
		let center = HU.div(['style','text-align:center;'],this.formatDate(allRecords[parseInt(allRecords.length/2)].getDate()));
		html+=HU.leftCenterRight(left,
					 center,
					 this.formatDate(allRecords[allRecords.length-1].getDate()),
					 '20%','60%','20%');

	    };
	    if(this.getShowLegend()) {
		makeLegend();
	    }
	    let blocks = [];
	    let labelWidth='2em';
	    groups.forEach((key,groupIdx)=>{
		if(groupField) {
		    html+=key;
		}
		let records =groupedRecords[key];
		fields.forEach((field,fidx)=>{
		    let uid = HU.getUniqueId('table_');
		    html+=HU.open('table',['id',uid,'style',HU.css('position','relative')]);
		    blocks.push({domId:uid,records:records,field:field});
		    let isLast = (groupIdx==groups.length-1 && fidx==fields.length-1);
		    let isFirst = (groupIdx==0 && fidx==0);
		    let colorBy = new  ColorByInfo(this,allFields, records, '',null,null,null,field);
		    colorBys.push(colorBy);
		    if(fields.length>1)
			html+='<tr style="border-bottom:1px solid #efefef;">';
		    else html+='<tr>';
		    if(this.getShowLabel()) {
			html+=HU.td(['width',labelWidth,
				     'class','display-stripes-stripe-label',
				     'style','max-width:2em;width:2em'],
				    HU.div(['width',labelWidth,'style',
					    HU.css('max-width',labelWidth,'width',labelWidth,'max-height',HU.getDimension(stripeHeight)),'class','display-stripes-vertical-label'],field.getLabel()));
		    }
		    records.forEach((record,idx)=>{
			let color =  colorBy.getColorFromRecord(record);
			let data = field.getValue(record);
			let date  = record.getDate();
			let title =field.getLabel()+': '+data;
			if(date) title = this.formatDate(date) +' - ' + title;
			let contents = '';
			let attrs = [RECORD_ID,record.getId(),
				     'class','display-stripes-stripe',
				     'style',HU.css('height',HU.getDimension(stripeHeight),'background',color),
				     'title',title,'width',stripeWidth];
			html+=HU.td(attrs,contents);
		    });
		    html+='</tr>';
		    html+='</table>';
		    if((isLast &&this.getShowColorTableBottom()) || this.getShowColorTable()) {
			html+=HU.div(['id',this.domId('colortable_'  + fidx),'style',
				      HU.css('width',(stripeWidth*records.length)+'px','margin-bottom','5px','height','1em')],'');
		    }
		});
	    });
	    if(this.getShowLegend()) {
		makeLegend();
	    }
	    html+='</div></center>';
	    this.setContents(html); 
	    colorBys.forEach((colorBy,fidx)=>{
		colorBy.displayColorTable(null,null,'colortable_'+fidx);
	    });
	    if(this.getShowSparkline()) {
		blocks.forEach(block=>{
		    let table = $('#'+ block.domId);
		    let labelColumn = table.find('.display-stripes-stripe-label');
		    let w = table.width();
		    let h = table.height();		
		    if(labelColumn.length>0) {
			w -=labelColumn.width();
		    }
		    let divId = HU.getUniqueId('div_');
		    table.append(HU.div(['id',divId,'style',HU.css('position','absolute',
								   'pointer-events','none',
								   'margin-left',this.getShowLabel()?labelWidth:'0px',
								   'left','0px',
								   'top','0px',
								   'width',w+'px',
								   'height',h+'px')],''));

		    let column = this.getColumnValues(block.records, block.field);
		    let minMaxColumn = this.getColumnValues(this.getSparklineUseAllRecords()?allRecords:block.records, block.field);		    
		    let m = 5;
		    drawSparkline(this,"#"+ divId,w,h,
				  column.values,block.records,
				  minMaxColumn.min,minMaxColumn.max,null,
				  {margin:{top:m,bottom:m}});
		});
	    }

	    let _this = this;
	    let stripes =   this.getContents().find('.display-stripes-stripe');
	    this.makeTooltips(stripes,allRecords);
	    stripes.click(function() {
		let record = recordMap[$(this).attr('record-id')];
		_this.propagateEventRecordSelection({record: record});
	    });


	},
    });
}

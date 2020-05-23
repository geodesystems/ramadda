/*
  Copyright 2008-2019 Geode Systems LLC
*/

const DISPLAY_GRAPH = "graph";
const DISPLAY_TREE = "tree";
const DISPLAY_ORGCHART = "orgchart";
const DISPLAY_TIMELINE = "timeline";
const DISPLAY_BLANK = "blank";
const DISPLAY_MESSAGE = "message";
const DISPLAY_RECORDS = "records";
const DISPLAY_TSNE = "tsne";
const DISPLAY_HEATMAP = "heatmap";
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

addGlobalDisplayType({
    type: DISPLAY_RANKING,
    label: "Ranking",
    requiresData: true,
    forUser: true,
    category: CATEGORY_MISC
});
addGlobalDisplayType({
    type: DISPLAY_CORRELATION,
    label: "Correlation",
    requiresData: true,
    forUser: true,
    category: CATEGORY_MISC
});
addGlobalDisplayType({
    type: DISPLAY_CROSSTAB,
    label: "Crosstab",
    requiresData: true,
    forUser: true,
    category: CATEGORY_MISC
});

addGlobalDisplayType({
    type: DISPLAY_STATS,
    label: "Stats Table",
    requiresData: false,
    forUser: true,
    category: CATEGORY_MISC
});
addGlobalDisplayType({
    type: DISPLAY_RECORDS,
    label: "Records",
    requiresData: true,
    forUser: true,
    category: CATEGORY_MISC
});
addGlobalDisplayType({
    type: DISPLAY_TSNE,
    label: "TSNE",
    requiresData: true,
    forUser: true,
    category: CATEGORY_MISC
});
addGlobalDisplayType({
    type: DISPLAY_HEATMAP,
    label: "Heatmap",
    requiresData: true,
    forUser: true,
    category: CATEGORY_MISC
});


addGlobalDisplayType({
    type: DISPLAY_GRAPH,
    label: "Graph",
    requiresData: true,
    forUser: true,
    category: "Misc"
});

addGlobalDisplayType({
    type: DISPLAY_PERCENTCHANGE,
    label: "Percent Change",
    requiresData: true,
    forUser: true,
    category: CATEGORY_MISC
});

addGlobalDisplayType({
    type: DISPLAY_SPARKLINE,
    label: "Sparkline",
    requiresData: true,
    forUser: true,
    category: CATEGORY_MISC
});

addGlobalDisplayType({
    type: DISPLAY_CANVAS,
    label: "Canvas",
    requiresData: true,
    forUser: true,
    category: CATEGORY_MISC
});

addGlobalDisplayType({
    type: DISPLAY_POINTIMAGE,
    label: "Point Image",
    requiresData: true,
    forUser: true,
    category: CATEGORY_MISC
});


addGlobalDisplayType({
    type: DISPLAY_FIELDTABLE,
    label: "Field Table",
    requiresData: true,
    forUser: true,
    category: CATEGORY_MISC
});




addGlobalDisplayType({
    type: DISPLAY_TREE,
    forUser: true,
    label: "Tree",
    requiresData: false,
    category: "Misc"
});

addGlobalDisplayType({
    type: DISPLAY_ORGCHART,
    label: "Org Chart",
    requiresData: true,
    forUser: true,
    category: "Misc"
});

addGlobalDisplayType({
    type: DISPLAY_TIMELINE,
    label: "Timeline",
    requiresData: true,
    forUser: true,
    category:  "Misc"
});


addGlobalDisplayType({
    type: DISPLAY_BLANK,
    label: "Blank",
    requiresData: true,
    forUser: true,
    category: "Misc"
});
addGlobalDisplayType({
    type: DISPLAY_MESSAGE,
    label: "Message",
    requiresData: true,
    forUser: true,
    category: "Misc"
});

addGlobalDisplayType({
    type: DISPLAY_COOCCURENCE,
    label: "Cooccurence",
    requiresData: true,
    forUser: true,
    category: "Misc"
});

addGlobalDisplayType({
    type: DISPLAY_BOXTABLE,
    label: "Box Table",
    requiresData: true,
    forUser: true,
    category: "Misc"
});

addGlobalDisplayType({
    type: DISPLAY_DATATABLE,
    label: "Date Table",
    requiresData: true,
    forUser: true,
    category: "Misc"
});





function RamaddaGraphDisplay(displayManager, id, properties) {
    const ID_GRAPH = "graph";
    const SUPER = new RamaddaDisplay(displayManager, id, DISPLAY_GRAPH, properties);
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);
    if(!window["ForceGraph"]) {
	Utils.importJS("https://unpkg.com/force-graph");
    }
    RamaddaUtil.defineMembers(this, {
        needsData: function() {
            return true;
        },
	callbackWaiting:false,
	getWikiEditorTags: function() {
	    return Utils.mergeLists(SUPER.getWikiEditorTags(),
				    [
					"label:Graph Properties",
					'sourceField=""',
					'targetField=""',
					'nodeBackground="#ccc"',
					'drawCircle="true"',
					'nodeWidth="10"',
					'linkColor="red"',
					'linkWidth="3"',
					'linkDash="5"',
					'linkWidth="3"',
					'arrowLength="6"',
					'arrowColor="green"',
					'directionalParticles="2"'
				    ])},

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
	    let html = HU.div([ID, this.getDomId(ID_GRAPH)]);
	    this.jq(ID_DISPLAY_CONTENTS).html(html);
	    let records = this.filterData();
	    if (!records) {
                return;
	    }  
	    let seenNodes = {};
	    let nodes = [];
	    let links = [];
	    let valueFields   = this.getFieldsByIds(null, this.getProperty("valueFields","",true));
	    let labelField = this.getFieldById(null, this.getProperty("labelField"));
	    if(!labelField) {
		let strings = this.getFieldsOfType(null, "string");
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
		    if(!seenNodes[source]) {
			seenNodes[source] = true;
			nodes.push({id:source,tooltip:source});
		    }
		    if(!seenNodes[target]) {
			seenNodes[target] = true;
			nodes.push({id:target,tooltip:target});
		    }
		    links.push({source:source, target: target});
		});
	    } else {
		this.jq(ID_DISPLAY_CONTENTS).html("No source/target fields specified");
		return;
	    }
	    graphData = {
		nodes: nodes,
		links: links
	    };

	    /*
	      links = [];
	      gGraphData.edges.forEach(e=>{
	      links.push({source:e.from,target:e.to});
	      });

	      graphData = {
	      nodes:gGraphData.nodes,
	      links: links
	      }
	    */
	    const nodeBackground = this.getProperty("nodeBackground",'rgba(255, 255, 255, 0.8)');
	    const linkColor = this.getProperty("linkColor","#ccc");
	    const drawCircle = this.getProperty("drawCircle",false);
	    const linkWidth = +this.getProperty("linkWidth",1);
	    const linkDash = +this.getProperty("linkDash",-1);
	    const drawText = this.getProperty("drawText",true);
	    const nodeWidth = this.getProperty("nodeWidth",10);
	    const elem = document.getElementById(this.getDomId(ID_GRAPH));
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

	    //	    graph.linkCanvasObjectMode('replace');
	    /*
	      graph.linkCanvasObject((link, ctx) => {
	      if(linkDash>0)
	      ctx.setLineDash([linkDash, linkDash]);
	      ctx.lineWidth = linkWidth;
	      ctx.strokeStyle = linkColor;
	      ctx.moveTo(link.source.x, link.source.y);
	      ctx.lineTo(link.target.x, link.target.y);
	      (link === graphData.links[graphData.links.length - 1]) && ctx.stroke();
	      });*/
	    //	    graph.linkAutoColorBy(d => gData.nodes[d.source].group);
	    if(this.getWidth())
		graph.width(this.getWidth());
	    if(this.getHeight())
		graph.height(this.getHeight());
	    graph.nodeLabel(node => node.tooltip?node.tooltip:null)
	    graph.linkWidth(+this.getProperty("linkWidth",4));
	    graph.linkColor(this.getProperty("linkColor","#000"));
	    if(this.getProperty("arrowColor")) {
		graph.linkDirectionalArrowColor(this.getProperty("arrowColor"));
	    }
	    if(this.getProperty("arrowLength")) {
		graph.linkDirectionalArrowLength(+this.getProperty("arrowLength"));
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
					"label:Tree Properties",
					"maxDepth=3",
					"showDetails=false",
				    ])},
        updateUI: function() {
            let records = this.filterData();
            if (!records) return;
	    let roots=null;
	    try {
		roots = this.makeTree(records);
	    } catch(error) {
                this.setContents(this.getMessage(error.toString()));
		return;
	    }

	    let html = "";
	    let baseId = this.getDomId("node");
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
	    this.jq(ID_DISPLAY_CONTENTS).find(".display-tree-toggle").click(function() {
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
	    this.jq(ID_DISPLAY_CONTENTS).find(".display-tree-toggle-details").click(function() {
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



function OrgchartDisplay(displayManager, id, properties) {
    const ID_ORGCHART = "orgchart";
    const SUPER = new RamaddaDisplay(displayManager, id, DISPLAY_ORGCHART, properties);
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);
    $.extend(this, {
        handleEventRecordSelection: function(source, args) {},
	getWikiEditorTags: function() {
	    return Utils.mergeLists(SUPER.getWikiEditorTags(),
				    [
					'label:Orgchart Properties',
					'labelField=""',
					'parentField=""',
					'idField=""',
					'treeRoot="some label"',
					'treeTemplate=""',
					'treeNodeSize="small|medium|large"'
					
				    ])},



        needsData: function() {
            return true;
        },
	updateUI: function() {
	    if(!waitOnGoogleCharts(this, ()=>{
		this.updateUI();
	    })) {
		return;
	    }
            this.displayHtml(HU.div([ID,this.getDomId(ID_ORGCHART)],"HELLO"));
	    if(this.jq(ID_ORGCHART).length==0) {
		setTimeout(()=>this.updateUI(),1000);
		return;
	    }
	    let roots=null;
	    try {
		roots = this.makeTree();
	    } catch(error) {
                this.setContents(this.getMessage(error.toString()));
		return;
	    }
            let data = new google.visualization.DataTable();
            data.addColumn('string', 'Name');
            data.addColumn('string', 'Parent');
            data.addColumn('string', 'ToolTip');
	    let rows = [];
	    let func = function(node) {
		cnt++;
		let value = node.label;
		if(node.display) {
		    value = {'v':node.label,f:node.display};
		}
		let row = [value, node.parent?node.parent.label:"",node.tooltip||""];
		rows.push(row);
		if(node.children.length>0) {
		    node.children.map(func);
		}
		if(node.record) {
		    //		    _this.countToRecord[cnt] = node.record;
		}
	    }
	    roots.map(func);
            data.addRows(rows);
            let chart = new google.visualization.OrgChart(document.getElementById(this.getDomId(ID_ORGCHART)));
            // Draw the chart, setting the allowHtml option to true for the tooltips.
            chart.draw(data, {'allowHtml':true,'allowCollapse':true,
			      'size':this.getProperty("treeNodeSize","medium")});
	}
    });
}


function RamaddaTimelineDisplay(displayManager, id, properties) {
    const ID_TIMELINE = "timeline";
    const SUPER =  new RamaddaFieldsDisplay(displayManager, id, DISPLAY_TIMELINE, properties);
    RamaddaUtil.inherit(this,SUPER);
    addRamaddaDisplay(this);
    
    this.defineProperties([
	{label:'Timeline Properties'},
	{p:'titleField',wikiValue:''},
	{p:'startAtSlide',wikiValue:'0'},
	{p:'startAtEnd',wikiValue:'true'},
	{p:'navHeight',wikiValue:'150'},
	{p:'titleField',wikiValue:''},
	{p:'startDateField',wikiValue:''},
	{p:'endDateField',wikiValue:''},
	{p:'imageField',wikiValue:''},
	{p:'groupField',wikiValue:''},
	{p:'urlField',wikiValue:''},
	{p:'textTemplate',wikiValue:''},
	{p:'timeTo',wikiValue:'year|day|hour|second'},
	{p:'justTimeline',wikiVaklue:"true"},
	{p:'hideBanner',wikiVaklue:"true"},
	{p:'hideBanner',wikiVaklue:"true"},	
    ]);

    Utils.importJS(ramaddaBaseUrl+"/lib/timeline3/timeline.js");
    let css = "https://cdn.knightlab.com/libs/timeline3/latest/css/timeline.css";
    //    css =  ramaddaBaseUrl+"/lib/timeline3/timeline.css";
    $(HU.tag('link',['rel','stylesheet','href', css,'type','text/css'] )).appendTo("head");

    $.extend(this, {
        needsData: function() {
            return true;
        },
	loadCnt:0,
	timelineLoaded: false,
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
		this.writeHtml(ID_DISPLAY_CONTENTS, "Could not load timeline");
		return;
	    }
            let records = this.filterData();
	    if(records==null) return;
	    let timelineId = this.getDomId(ID_TIMELINE);
	    this.writeHtml(ID_DISPLAY_CONTENTS, HU.div([ID,timelineId]));
	    this.timelineReady = false;
	    let opts = {
		start_at_end: this.getPropertyStartAtEnd(false),
		start_at_slide: this.getPropertyStartAtSlide(0),
		//		default_bg_color: {r:0, g:0, b:0},
		timenav_height: this.getPropertyNavHeight(150),
		//		menubar_height:100,
		gotoCallback: (slide)=>{
		    if(this.timelineReady) {
			let record = records[slide];
			if(record) {
			    this.propagateEventRecordSelection({record: record});
			}
		    }
		}
            };
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

	    let startDateField = this.getFieldById(null,this.getPropertyStartDateField());
	    let endDateField = this.getFieldById(null,this.getPropertyEndDateField());
	    let imageField = this.getFieldById(null,this.getPropertyImageField());
	    let groupField = this.getFieldById(null,this.getPropertyGroupField());
	    let urlField = this.getFieldById(null,this.getPropertyUrlField());
	    let textTemplate = this.getPropertyTextTemplate("${default}");
	    let timeTo = this.getPropertyTimeTo("day");
	    this.recordToIndex = {};
	    for(let i=0;i<records.length;i++) {
		let record = records[i]; 
		this.recordToIndex[record.getId()] = i;
		let tuple = record.getData();
		let event = {
		};	
		let text =  this.getRecordHtml(record, null, textTemplate);
		event.text = {
		    headline: titleField? tuple[titleField.getIndex()]:" record:" + (i+1),
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
		if(startDateField)
		    event.start_date = tuple[startDateField.getIndex()];
		else
		    event.start_date = this.getDate(record.getTime());
		if(endDateField) {
		    event.end_date = tuple[endDateField.getIndex()];
		}
		//		console.log(JSON.stringify(event));
		events.push(event);
	    }
	    this.timeline = new TL.Timeline(timelineId,json,opts);
	    if(this.getPropertyHideBanner(false)) {
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
	    let index = this.recordToIndex[args.record.getId()];
	    if(!Utils.isDefined(index)) return;
	    this.timeline.goTo(index);
	},
	getDate: function(time) {
	    let timeTo = this.getPropertyTimeTo("day");
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


function RamaddaBlankDisplay(displayManager, id, properties) {
    if(!properties.width) properties.width='100%';
    properties.showMenu = false;
    properties.showTitle = false;
    const SUPER =  new RamaddaFieldsDisplay(displayManager, id, DISPLAY_BLANK, properties);
    RamaddaUtil.inherit(this,SUPER);
    addRamaddaDisplay(this);
    $.extend(this, {
        needsData: function() {
            return true;
        },
	updateUI: function() {
	    let records = this.filterData();
	    this.writeHtml(ID_DISPLAY_CONTENTS, "");
	    if(!records) return;
	    let colorBy = this.getColorByInfo(records);
	    if(colorBy.index>=0) {
		records.map(record=>{
		    color =  colorBy.getColor(record.getData()[colorBy.index], record);
		});
		colorBy.displayColorTable();
	    }
	}});
}


function RamaddaMessageDisplay(displayManager, id, properties) {
    const SUPER =  new RamaddaFieldsDisplay(displayManager, id, DISPLAY_MESSAGE, properties);
    RamaddaUtil.inherit(this,SUPER);
    addRamaddaDisplay(this);
    $.extend(this, {
        needsData: function() {
            return false;
        },
	updateUI: function() {
	    if(this.getProperty("decorate",true)) {
		this.setContents(this.getMessage(this.getProperty("message",this.getNoDataMessage())));
	    } else {
		this.setContents(this.getProperty("message",this.getNoDataMessage()));
	    }
	}});
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
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);

    RamaddaUtil.defineMembers(this, {
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
                this.setContents(this.getLoadingMessage());
                return;
            }
            await Utils.importJS(ramaddaBaseUrl + "/lib/tsne.js");
            //Height is the height of the overall display including the menu bar
            let height = this.getProperty("height");
            if (height.endsWith("px")) height = height.replace("px", "");
            height = parseInt(height);
            //            height-=30;
            let details = HU.div([STYLE, HU.css('height', height + 'px','max-height', height + "px"), CLASS, "display-tnse-details", ID, this.getDomId(ID_DETAILS)], "");
            let canvas = HU.div([CLASS, "display-tnse-canvas-outer", STYLE, HU.css('height', height + 'px')], HU.div([CLASS, "display-tnse-canvas", ID, this.getDomId(ID_CANVAS)], ""));
            let buttons = HU.div([ID, this.getDomId(ID_RUN), CLASS, "ramadda-button", "what", "run"], "Stop") + SPACE +
                HU.div([ID, this.getDomId(ID_STEP), CLASS, "ramadda-button", "what", "step"], "Step") + SPACE +
                HU.div([ID, this.getDomId(ID_RESET), CLASS, "ramadda-button", "what", "reset"], "Reset") + SPACE +
                HU.input("", "", [ID, this.getDomId(ID_SEARCH), "placeholder", "search"]);

            buttons = HU.div([CLASS, "display-tnse-toolbar"], buttons);
            this.jq(ID_TOP_LEFT).append(buttons);
            this.setContents(HU.table([WIDTH,'100'], HU.tr(['valign','top'], HU.td(['width','80%'], canvas) + HU.td(['width','20%'], details))));
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
                let strings = this.getFieldsOfType(this.fields, "string");
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
                    this.canvas.append(HU.div([TITLE, title, "index", i, ID, this.getDomId("element-" + i), CLASS, "display-tnse-mark", STYLE, HU.css('left', px + '%', 'top',py +'%')], title));
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
                        details += HU.tr([],HU.td(['align','right', CLASS,'formlabel'], field.getLabel() + ':') + HU.td([],tuple[field.getIndex()]));
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
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
        "map-display": false,
        needsData: function() {
            return true;
        },
        getMenuItems: function(menuItems) {
            SUPER.getMenuItems.call(this, menuItems);
            let get = this.getGet();
            let tmp = HU.formTable();
            let colorTable = this.getColorTableName();
            let ct = HU.open('select',[ID,this.getDomId("colortable")]);
            for (table in Utils.ColorTable) {
                if (table == colorTable)
                    ct += HU.tag('option',['selected',null], table);
                else
                    ct += HU.tag('option',[], table);
            }
            ct += HU.close('select');
            tmp += HU.formEntry("Color Table:", ct);
            tmp += HU.formEntry("Color By Range:", HU.input("", this.colorByMin, ["size", "7", ATTR_ID, this.getDomId("colorbymin")]) + " - " +
				HU.input("", this.colorByMax, ["size", "7", ATTR_ID, this.getDomId("colorbymax")]));
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
                this.setContents(this.getLoadingMessage());
                setTimeout(func, 1000);
                return;
            }

            SUPER.updateUI.call(this, pointData);
            if (!this.hasData()) {
                this.setContents(this.getLoadingMessage());
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
                html += HU.td([ALIGN,'center'], HU.div([CLASS, "display-heatmap-heading-top"], field.getLabel()));
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
                    let title = header[0] + ": " + rowLabel + " - " + field.getLabel() + ": " + value;
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
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);

    RamaddaUtil.defineMembers(this, {
	getWikiEditorTags: function() {
	    return Utils.mergeLists(SUPER.getWikiEditorTags(),
				    [
					"label:Chart Properties",
					"sortField=\"\"",
					'nameFields=""',
				    ]);

	},


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
                this.setContents(this.getLoadingMessage());
                return;
            }
            let dataList = this.getStandardData(null, {
                includeIndex: false
            });
            let allFields = this.dataCollection.getList()[0].getRecordFields();
            let fields = this.getSelectedFields([]);
            if (fields.length == 0) fields = allFields;
            let numericFields = this.getFieldsOfType(fields, "numeric");
            let sortField = this.getFieldById(numericFields, this.getProperty("sortField","",true));
            if (numericFields.length == 0) {
                this.setContents("No fields specified");
                return;
            }
            if (!sortField) {
                sortField = numericFields[0];
            }
            if (!sortField) {
                this.setContents("No fields specified");
                return;
            }

            let stringFields = this.getFieldsByIds(allFields, this.getProperty("nameFields","",true));
            if(stringFields.length==0) {
		let tmp = this.getFieldById(allFields, this.getProperty("nameField","",true));
		if(tmp) stringFields.push(tmp);
	    }
            if(stringFields.length==0) {
                let stringField = this.getFieldOfType(allFields, "string");
		if(stringField) stringFields.push(stringField);
	    }
            let menu = HU.open("select",[CLASS,'ramadda-pulldown',ID, this.getDomId("sortfields")]);
            for (let i = 0; i < numericFields.length; i++) {
                let field = numericFields[i];
                let extra = "";
                if (field.getId() == sortField.getId()) extra = " selected ";
                menu += HU.tag('option',['value', field.getId(), extra,null], field.getLabel());
            }
            menu += HU.close('select');
	    let top ="";
	    top += HU.span([ID,this.getDomId("sort")], HU.getIconImage(this.sortAscending?"fa-sort-up":"fa-sort-down", [STYLE,HU.css('cursor','pointer'),TITLE,"Change sort order"]));
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
            html += HU.open(TABLE, [ID, this.getDomId(ID_TABLE)]);
            let tmp = [];
            for (let rowIdx = 1; rowIdx < dataList.length; rowIdx++) {
                let obj = dataList[rowIdx];
                obj.originalRow = rowIdx;
                tmp.push(obj);
            }

	    let includeNaN = this.getProperty("includeNaN",false);
	    if(!includeNaN) {
		let tmp2 = [];
		tmp.map(r=>{
		    let t = this.getDataValues(r);
		    let v = t[sortField.getIndex()];
		    if(!isNaN(v)) tmp2.push(r);
		});
		tmp = tmp2;
	    }
            let cnt = 0;
	    let highlight = this.getFilterHighlight();
	    let sorter = (a,b)=>{
		let r1 = a.record;
		let r2 = b.record;
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
                let obj = tmp[rowIdx];
                let tuple = this.getDataValues(obj);
                let label = "";
                stringFields.map(f=>{
		    label += tuple[f.getIndex()]+" ";
		});

                label = label.trim();
		value = tuple[sortField.getIndex()];
                if (isNaN(value) || value === null) {
		    if(!includeNaN) continue;
		    value = "NA";
		} else {
		    value = this.formatNumber(value);
		}
		html += HU.tr([VALIGN,'top',CLASS,'display-ranking-row','what',obj.originalRow],
			      HU.td([],'#' + (rowIdx + 1)) + HU.td([],SPACE + label) +HU.td([ALIGN,'right'], SPACE +
											    value));
            }
            html += HU.close(TABLE);
            html += HU.close(DIV);
            this.setContents(html);
            let _this = this;
            this.jq(ID_TABLE).find(".display-ranking-row").click(function(e) {
                _this.getDisplayManager().propagateEventRecordSelection(_this, _this.getPointData(), {
                    index: parseInt($(this).attr("what")) - 1
                });
            });
	    HU.initSelect(this.jq("sortfields"));
            this.jq("sortfields").change(function() {
                _this.setProperty("sortField", $(this).val());
                _this.updateUI();
            });
        },
    });
}



function RamaddaCrosstabDisplay(displayManager, id, properties) {
    const ID_TABLE = "crosstab";
    const SUPER = new RamaddaFieldsDisplay(displayManager, id, DISPLAY_CROSSTAB, properties);
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
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
                this.setContents(this.getLoadingMessage());
                return;
            }
            let allFields = this.dataCollection.getList()[0].getRecordFields();
	    let enums = [];
	    allFields.map(field=>{
		let label = field.getLabel();
		if(label.length>30) label = label.substring(0,29);
		enums.push([field.getId(),label]);
	    });
	    let select = HU.span([CLASS,"display-filterby"],
				 "Display: " + HU.select("",[STYLE,"", ID,this.getDomId("crosstabselect")],enums,
							 this.getProperty("column", "", true)));


            this.setContents(select+HU.div([ID,this.getDomId(ID_TABLE)]));
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
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);

    RamaddaUtil.defineMembers(this, {
        "map-display": false,
        needsData: function() {
            return true;
        },
	getWikiEditorTags: function() {
	    return Utils.mergeLists(SUPER.getWikiEditorTags(),
				    [
					
					"label:Chart Properties",
					'showSelectSlider=false',
					"range.low.min=\"-1\"",
					"range.low.max=\"0\"",
					"range.high.min=\"0\"",
					"range.high.max=\"1\"",
					["short=true","Abbreviated display"],
					["showValue=false","Show the values"],
					['useId = true',"Use field id instead of label"],
					['useIdTop=true',"Use field id for top header"],
					['useIdSide = true',"Use field id for side header"],
					['labelStyle=""',"CSS style for labels"]
				    ]);

	},

        getMenuItems: function(menuItems) {
            SUPER.getMenuItems.call(this, menuItems);
            let get = this.getGet();
            let tmp = HU.formTable();
            let colorTable = this.getColorTableName();
            let ct = HU.open('select',[ID,this.getDomId("colortable")]);
            for (table in Utils.ColorTables) {
                if (table == colorTable)
                    ct += HU.tag('option',['selected',null],table);
                else
                    ct += HU.tag('option',[], table);
            }
            ct += HU.close('select');
            tmp += HU.formEntry("Color Bar:", ct);
            tmp += HU.formEntry("Color By Range:", HU.input("", this.colorByMin, ["size", "7", ATTR_ID, this.getDomId("colorbymin")]) + " - " +
				HU.input("", this.colorByMax, ["size", "7", ATTR_ID, this.getDomId("colorbymax")]));
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
                this.setContents(this.getLoadingMessage());
                return;
            }
	    let _this  = this;
	    let html = "";
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
	    if(this.getProperty("showSelectSlider",true)) {
		let lowSlider = HU.div([STYLE,HU.css('display','inline-block')],"Low Range" + HU.tag(BR) + 
				       HU.div([ID,this.gid(ID_SLIDER_LOW_MIN),STYLE,HU.css(WIDTH,'50px','display','inline-block','text-align','right','margin-right','15px')],this.range.low.min) +
				       HU.div([STYLE,HU.css(HEIGHT,'20px','display','inline-block',WIDTH,'200px','background','#A6A6FF'), ID,this.gid(ID_SLIDER_LOW)]) +
				       HU.div([ID,this.gid(ID_SLIDER_LOW_MAX),STYLE,HU.css('text-align','left','width','50px','display','inline-block','margin-left','15px')],this.range.low.max));
		let highSlider = HU.div(["display","inline-block;"], "High Range" + HU.tag(BR) + 
					HU.div([ID,this.gid(ID_SLIDER_HIGH_MIN),STYLE,HU.css('width','50px','display','inline-block','text-align','right', 'margin-right','15px')],this.range.high.min) +
					HU.div([STYLE,HU.css(HEIGHT,'20px','display','inline-block','width','200px','background','#FD9596'), ID,this.gid(ID_SLIDER_HIGH)]) +
					HU.div([ID,this.gid(ID_SLIDER_HIGH_MAX),STYLE,HU.css('text-align','left','width','50px','display','inline-block','margin-left','15px')],this.range.high.max));


		html +=HU.center(HU.hrow(lowSlider, highSlider));
	    }
	    html +=HU.div([ID,this.getDomId(ID_TABLE)]);
            this.setContents(html);
	    this.makeTable();
	    if(this.getProperty("showSelectSlider",true)) {
		this.jq(ID_SLIDER_LOW).slider({
		    range: true,
		    min: 0,
		    max: 100,
		    values:[(this.range.low.min+1)*100,(this.range.low.max+1)*100],
		    slide: function( event, ui ) {
			let v1 = -1+ui.values[0]/100;
			let v2 = -1+ui.values[1]/100;
			_this.jq(ID_SLIDER_LOW_MIN).html(v1==-1?-1:number_format(v1,3));
			_this.jq(ID_SLIDER_LOW_MAX).html(v2==0?0:number_format(v2,3));
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
	makeTable: function() {
            let dataList = this.getStandardData(null, {
                includeIndex: false
            });
            let allFields = this.dataCollection.getList()[0].getRecordFields();
            let fields = this.getSelectedFields([]);
            if (fields.length == 0) fields = allFields;
            let fieldCnt = 0;
            for (let fieldIdx = 0; fieldIdx < fields.length; fieldIdx++) {
                let field1 = fields[fieldIdx];
                if (!field1.isFieldNumeric() || field1.isFieldGeo()) continue;
                fieldCnt++;
            }

            let html = HU.open(TABLE, ["cellspacing","0","cellpadding", "0", "border", "0", CLASS, "display-correlation", "width", "100%"]);
            let col1Width = 10 + "%";
            let width = 90 / fieldCnt + "%";
            html += HU.open(TR,["valign","bottom"]) + HU.td([CLASS,"display-heading","width", col1Width],SPACE);

            let short = this.getProperty("short", fieldCnt > 8);
            let showValue = this.getProperty("showValue", !short);
            let useId = this.getProperty("useId", true);
            let useIdTop = this.getProperty("useIdTop", useId);
            let useIdSide = this.getProperty("useIdSide", useId);
	    let labelStyle = this.getProperty("labelStyle","");
            for (let fieldIdx = 0; fieldIdx < fields.length; fieldIdx++) {
                let field1 = fields[fieldIdx];
                if (!field1.isFieldNumeric() || field1.isFieldGeo()) continue;
                let label = useIdTop ? field1.getId() : field1.getLabel();
                if (short) label = "";
		label = label.replace(/\/ +/g,"/").replace(/ +\//g,"/");
		
		label = HU.span([STYLE,labelStyle], label);

                html += HU.td(["colfield", field1.getId(), "align","center","width",width],
			      HU.div([CLASS, "display-correlation-heading display-correlation-heading-top"], label));
            }
            html += HU.close(TR);
            let colors = null;
            colorByMin = parseFloat(this.colorByMin);
            colorByMax = parseFloat(this.colorByMax);
	    //            colors =  this.addAlpha(this.getColorTable(true),0.75);
	    colors =  this.getColorTable(true);
            for (let fieldIdx1 = 0; fieldIdx1 < fields.length; fieldIdx1++) {
                let field1 = fields[fieldIdx1];
                if (!field1.isFieldNumeric() || field1.isFieldGeo()) continue;
                let label = useIdSide ? field1.getId() : field1.getLabel();
		label.replace(/ /g, SPACE);
		label = HU.span([STYLE,labelStyle], label);
                html += HU.open(TR, ["valign","center"]);
		html += HU.td(["rowfield",field1.getId(),CLASS, "display-correlation-heading"],  HU.div([CLASS, "display-correlation-heading-side"], label));
                let rowName = field1.getLabel();
                for (let fieldIdx2 = 0; fieldIdx2 < fields.length; fieldIdx2++) {
                    let field2 = fields[fieldIdx2];
                    if (!field2.isFieldNumeric() || field2.isFieldGeo()) continue;
                    let colName = field2.getLabel();
                    let t1 = 0;
                    let t2 = 0;
                    let cnt = 0;

                    for (let rowIdx = 1; rowIdx < dataList.length; rowIdx++) {
                        let tuple = this.getDataValues(dataList[rowIdx]);
                        let v1 = tuple[field1.getIndex()];
                        let v2 = tuple[field2.getIndex()];
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
                        let v1 = tuple[field1.getIndex()];
                        let v2 = tuple[field2.getIndex()];
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
                        let percent = (r - colorByMin) / (colorByMax - colorByMin);
                        let index = parseInt(percent * colors.length);
                        if (index >= colors.length) index = colors.length - 1;
                        else if (index < 0) index = 0;
                        style = "background-color:" + colors[index];
                    }
                    let value = r.toFixed(3);
                    let label = value;
                    if (!showValue || short) label = SPACE;
		    let cellContents = "";
		    if(ok) {
			cellContents = HU.div([CLASS, "display-correlation-element", TITLE, "&rho;(" + rowName + "," + colName + ") = " + value], label);
		    }

                    html += HU.td(["colfield", field2.getId(), "rowfield",field1.getId(), CLASS,"display-correlation-cell","align", "right", STYLE,style], cellContents);
                }
                html += HU.close(TR);
            }
            html += HU.tr([],HU.td([]) + HU.td(['colspan',(fieldCnt + 1)], HU.div([ID, this.getDomId(ID_LASTROW)], "")));
            html += HU.close(TABLE);
	    this.jq(ID_TABLE).html(html);
	    let _this = this;
	    let selectedRow;
	    let selectedCol;
	    this.jq(ID_TABLE).find("td").click(function() {
		let rowField = _this.getFieldById(null, $(this).attr("rowfield"));
		let colField = _this.getFieldById(null, $(this).attr("colfield"));
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
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
	getWikiEditorTags: function() {
	    return Utils.mergeLists(SUPER.getWikiEditorTags(),
				    [
					"maxHeight=\"\"",
				    ]);
	},
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
                this.setContents(this.getLoadingMessage());
                return;
            }
	    this.records = records;
	    let _this = this;
            let fields = this.getSelectedFields(this.getData().getRecordFields());
            let html = "";
            for (let rowIdx = 0; rowIdx < records.length; rowIdx++) {
		let div = "";
                let tuple = this.getDataValues(records[rowIdx]);
                for (let fieldIdx = 0; fieldIdx < fields.length; fieldIdx++) {
                    let field = fields[fieldIdx];
                    let v = tuple[field.getIndex()];
                    div += HU.b(field.getLabel()) + ": " + v + HU.tag(BR);
                }
                html += HU.div([CLASS,"display-records-record",RECORD_INDEX,rowIdx], div);
            }
            let height = this.getProperty("maxHeight", "400px");
            if (!height.endsWith("px")) {
                height = height + "px";
            }
            this.setContents(HU.div([STYLE, HU.css('max-height', height,'overflow-y','auto')], html));
	    this.jq(ID_DISPLAY_CONTENTS).find(".display-records-record").click(function() {
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
    let dflt = Utils.isDefined(properties["showDefault"]) ? properties["showDefault"] : true;
    $.extend(this, {
        showMin: dflt,
        showMax: dflt,
        showAverage: dflt,
        showStd: dflt,
        showCount: dflt,
        showTotal: dflt,
        showPercentile: dflt,
        showMissing: dflt,
        showUnique: dflt,
        showType: dflt,
        showText: dflt,
    });
    const SUPER = new RamaddaFieldsDisplay(displayManager, id, type || DISPLAY_STATS, properties);
    RamaddaUtil.inherit(this, SUPER);

    if (!type)
        addRamaddaDisplay(this);


    RamaddaUtil.defineMembers(this, {
        "map-display": false,
	getWikiEditorTags: function() {
	    return Utils.mergeLists(SUPER.getWikiEditorTags(),
				    [
					'label:Summary Statistics',
					'showMin="true"',
					'showMax="true"',
                                        'showAverage="true"',
                                        'showStd="true"',
                                        'showPercentile="true"',
                                        'showCount="true"',
                                        'showTotal="true"',
                                        'showPercentile="true"',
                                        'showMissing="true"',
                                        'showUnique="true"',
                                        'showType="true"',
                                        'showText="true"',
					'doValueSelection=true'
				    ])},



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
            let tuples = this.getStandardData(null, {
                includeIndex: false
            });
            let justOne = (tuples.length == 2);

            //get the numeric fields
            let l = [];
            for (i = 0; i < fields.length; i++) {
                let field = fields[i];
                if (!justOne && (!this.showText && !field.isNumeric())) continue;
                let lbl = field.getLabel().toLowerCase();
                if (lbl.indexOf("latitude") >= 0 || lbl.indexOf("longitude") >= 0) {
                    continue;
                }
                l.push(field);
            }
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
            if (!this.hasData()) {
                this.setContents(this.getLoadingMessage());
                return;
            }
            let dataList = this.getStandardData(null, {
                includeIndex: false
            });
            let allFields = this.dataCollection.getList()[0].getRecordFields();
            this.allFields = allFields;
            let fields = this.getSelectedFields([]);
            let fieldMap = {};
            let stats = [];
            let justOne = (dataList.length == 2);
            for (let rowIdx = 1; rowIdx < dataList.length; rowIdx++) {
                let tuple = this.getDataValues(dataList[rowIdx]);
                if (rowIdx == 1) {
                    for (let col = 0; col < tuple.length; col++) {
                        stats.push({
                            isNumber: false,
                            count: 0,
                            min: Number.MAX_SAFE_INTEGER,
                            uniqueMap: {},
                            unique: 0,
                            std: 0,
                            max: Number.MIN_SAFE_INTEGER,
                            total: 0,
                            numMissing: 0,
                            numNotMissing: 0,
                            type: null,
                            values: []
                        });
                    }
                }
                for (let fieldIdx = 0; fieldIdx < fields.length; fieldIdx++) {
                    let field = fields[fieldIdx];
                    let col = field.getIndex()
                    stats[col].type = field.getType();
                    let v = tuple[col];
                    if (v) {
                        if (!Utils.isDefined(stats[col].uniqueMap[v])) {
                            stats[col].uniqueMap[v] = 1;
                            stats[col].unique++;
                        } else {
                            stats[col].uniqueMap[v]++;
                        }
                    }
                    stats[col].isNumber = field.isNumeric();
                    stats[col].count++;
                    if (v == null) {
                        stats[col].numMissing++;
                    } else {
                        stats[col].numNotMissing++;
                    }
                    if (v && (typeof v == 'number')) {
                        let label = field.getLabel().toLowerCase();
                        if (label.indexOf("latitude") >= 0 || label.indexOf("longitude") >= 0) {
			    continue;
                        }
                        stats[col].total += v;
                        stats[col].max = Math.max(stats[col].max, v);
                        stats[col].min = Math.min(stats[col].min, v);
                        stats[col].values.push(v);
                    }
                }
            }


            if (this.showUnique) {
                for (let fieldIdx = 0; fieldIdx < fields.length; fieldIdx++) {
                    let field = fields[fieldIdx];
                    let col = field.getIndex();
                    stats[col].uniqueMax = 0;
                    stats[col].uniqueValue = "";
                    for (let v in stats[col].uniqueMap) {
                        let count = stats[col].uniqueMap[v];
                        if (count > stats[col].uniqueMax) {
                            stats[col].uniqueMax = count;
                            stats[col].uniqueValue = v;
                        }
                    }
                }
            }

            if (this.showStd) {
                for (let fieldIdx = 0; fieldIdx < fields.length; fieldIdx++) {
                    let field = fields[fieldIdx];
                    let col = field.getIndex();
                    let values = stats[col].values;
                    if (values.length > 0) {
                        let average = stats[col].total / values.length;
                        let stdTotal = 0;
                        for (let i = 0; i < values.length; i++) {
                            let diff = values[i] - average;
                            stdTotal += diff * diff;
                        }
                        let mean = stdTotal / values.length;
                        stats[col].std = Math.sqrt(mean);
                    }
                }
            }
            let border = (justOne ? "0" : "1");
            let html = HU.open(TABLE, ["border", border, "bordercolor", "#ccc", CLASS, "display-stats", "cellspacing", "1", "cellpadding", "5"]);
            let dummy = [SPACE];
            if (!justOne) {
                header = [""];
                if (this.getProperty("showCount", dflt)) {
                    header.push("Count");
                    dummy.push(SPACE);
                }
                if (this.getProperty("showMin", dflt)) {
                    header.push("Min");
                    dummy.push(SPACE);
                }
                if (this.getProperty("showPercentile", dflt)) {
                    header.push("25%");
                    dummy.push(SPACE);
                    header.push("50%");
                    dummy.push(SPACE);
                    header.push("75%");
                    dummy.push(SPACE);
                }
                if (this.getProperty("showMax", dflt)) {
                    header.push("Max");
                    dummy.push(SPACE);
                }
                if (this.getProperty("showTotal", dflt)) {
                    header.push("Total");
                    dummy.push(SPACE);
                }
                if (this.getProperty("showAverage", dflt)) {
                    header.push("Average");
                    dummy.push(SPACE);
                }
                if (this.getProperty("showStd", dflt)) {
                    header.push("Std");
                    dummy.push(SPACE);
                }
                if (this.getProperty("showUnique", dflt)) {
                    header.push("# Unique");
                    dummy.push(SPACE);
                    header.push("Top");
                    dummy.push(SPACE);
                    header.push("Freq.");
                    dummy.push(SPACE);
                }
                if (this.getProperty("showMissing", dflt)) {
                    header.push("Not&nbsp;Missing");
                    dummy.push(SPACE);
                    header.push("Missing");
                    dummy.push(SPACE);
                }
                html += HU.tr(["valign", "bottom"], HU.tds([CLASS, "display-stats-header", "align", "center"], header));
            }
            let cats = [];
            let catMap = {};
	    let doValueSelection = this.getProperty("doValueSelection", false);
            for (let fieldIdx = 0; fieldIdx < fields.length; fieldIdx++) {
                let field = fields[fieldIdx];
                let col = field.getIndex();
                field = allFields[col];
                let right = "";
                let total = SPACE;
                let _label = field.getLabel().toLowerCase();
                let avg = stats[col].numNotMissing == 0 ? "NA" : this.formatNumber(stats[col].total / stats[col].numNotMissing);
                //Some guess work about when to show a total
                if (_label.indexOf("%") < 0 && _label.indexOf("percent") < 0 && _label.indexOf("median") < 0) {
                    total = this.formatNumber(stats[col].total);
                }
                if (justOne) {
                    right = HU.tds(["xalign", "right"], [this.formatNumber(stats[col].min)]);
                    continue;
                }
                let values = [];
                if (!stats[col].isNumber && this.getProperty("showText", dflt)) {
                    if (this.getProperty("showCount", dflt))
                        values.push(stats[col].count);
                    if (this.getProperty("showMin", dflt))
                        values.push("-");
                    if (this.getProperty("showPercentile", dflt)) {
                        values.push("-");
                        values.push("-");
                        values.push("-");
                    }
                    if (this.getProperty("showMax", dflt))
                        values.push("-");
                    values.push("-");
                    if (this.getProperty("showAverage", dflt)) {
                        values.push("-");
                    }
                    if (this.getProperty("showStd", dflt)) {
                        values.push("-");
                    }
                    if (this.getProperty("showUnique", dflt)) {
                        values.push(stats[col].unique);
                        values.push(stats[col].uniqueValue);
                        values.push(stats[col].uniqueMax);
                    }
                    if (this.getProperty("showMissing", dflt)) {
                        values.push(stats[col].numNotMissing);
                        values.push(stats[col].numMissing);
                    }
                } else {
                    if (this.getProperty("showCount", dflt)) {
                        values.push(stats[col].count);
                    }
                    if (this.getProperty("showMin", dflt)) {
			let s=this.formatNumber(stats[col].min);
                        values.push(s);
                    }
                    if (this.getProperty("showPercentile", dflt)) {
                        let range = stats[col].max - stats[col].min;
			let tmp =p=> {
                            let s = this.formatNumber(stats[col].min + range * p);
			    if(doValueSelection) {
				s = HU.span([CLASS,"display-stats-value","data-type", "percentile","data-value", p],s);
			    }
                            values.push(s);
			}
			let percs = [.25,.5,.75];
			percs.map(v=>tmp(v));
                    }
                    if (this.getProperty("showMax", dflt)) {
			let s=this.formatNumber(stats[col].max);
                        values.push(s);
                    }
                    if (this.getProperty("showTotal", dflt)) {
                        values.push(total);
                    }
                    if (this.getProperty("showAverage", dflt)) {
                        values.push(avg);
                    }
                    if (this.getProperty("showStd", dflt)) {
                        values.push(this.formatNumber(stats[col].std));
                    }
                    if (this.getProperty("showUnique", dflt)) {
                        values.push(stats[col].unique);
                        if (Utils.isNumber(stats[col].uniqueValue)) {
                            values.push(this.formatNumber(stats[col].uniqueValue));
                        } else {
                            values.push(stats[col].uniqueValue);
                        }
                        values.push(stats[col].uniqueMax);
                    }
                    if (this.getProperty("showMissing", dflt)) {
                        values.push(stats[col].numNotMissing);
                        values.push(stats[col].numMissing);
                    }
                }
                right = HU.tds(["align", "right"], values);
                let align = (justOne ? "right" : "left");
                let label = field.getLabel();
                let toks = label.split("!!");
                let tooltip = "";
                tooltip += field.getId();
                if (field.description && field.description != "") {
                    tooltip += "\n" + field.description + "\n";
                }
                label = toks[toks.length - 1];
                if (field.unit && field.unit != "")
                    label = label + " [" + field.unit + "]";
                if (justOne) {
                    label += ":";
                }
                label = label.replace(/ /g, SPACE)
                let row = HU.tr([], HU.td(["align", align], field.getTypeLabel() +SPACE + HU.b(HU.span([TITLE, tooltip], label))) + right);
                if (justOne) {
                    html += row;
                } else {
                    html += row;
                }
            }
            html += HU.close(TABLE);
            this.setContents(html);
            this.initTooltip();

	    if(doValueSelection) {
		let values = this.jq(ID_DISPLAY_CONTENTS).find(".display-stats-value");
		values.each(function() {
		    let type  = $(this).attr("data-type");
		    let value  = $(this).attr("data-value");
		    let links = SPACE + HU.getIconImage("fa-less-than",[TITLE,"Filter other displays",
									CLASS,"display-stats-value-link","data-type",type,"data-value",value],
							[STYLE,HU.css('font-size','8pt')]);

		    $(this).append(links);
		});
		values = this.jq(ID_DISPLAY_CONTENTS).find(".display-stats-value-link");
		values.each(function() {
		    
		});



	    }


            //always propagate the event when loaded
	    let record = this.dataCollection.getList()[0];
	    this.displayManager.propagateEventRecordSelection(this,
							      record, {
								  index: 0
							      });
        },
        handleEventRecordSelection: function(source, args) {
            //                this.lastHtml = args.html;
            //                this.setContents(args.html);
        }
    });
}



function RamaddaCooccurenceDisplay(displayManager, id, properties) {
    const ID_TABLE = "table";
    const ID_HEADER = "coocheader";
    const ID_SORTBY = "sortby";
    const SUPER = new RamaddaDisplay(displayManager, id, DISPLAY_COOCCURENCE, properties);
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
        needsData: function() {
            return true;
        },
	getWikiEditorTags: function() {
	    return Utils.mergeLists(SUPER.getWikiEditorTags(),
				    [
					"label:Cooccurence Properties",
					'sourceField=""',
					'targetField=""',
					'colorBy=""',
					'directed=false',
					'missingBackground=#eee',
					'showSortBy=false',
					'sortBy=weight',
					'minWeight=""',
					'topSpace=50px'
					
				    ])},
	getHeader2:function() {
	    let html = SUPER.getHeader2.call(this);
	    let weightField = this.getFieldById(null, this.getProperty("colorBy","weight"));
	    if(weightField && this.getProperty("showSortBy",true)) {
		let enums = [["name","Name"],["weight","Weight"]];
		html +=  HU.div([STYLE,HU.css('display','inline-block')], "Sort by: " + HU.select("",[ID,this.getDomId(ID_SORTBY)],enums,this.getProperty("sortBy","")))+SPACE2;
		
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
	    let html = HU.div([ID, this.getDomId(ID_HEADER)]) +
		HU.div([ID, this.getDomId(ID_TABLE)]);
	    this.jq(ID_DISPLAY_CONTENTS).html(html);


	    let sourceField = this.getFieldById(null, this.getProperty("sourceField","source"));
	    let targetField = this.getFieldById(null, this.getProperty("targetField","target"));
	    let weightField = this.getFieldById(null, this.getProperty("colorBy","weight"));

	    if(sourceField==null || targetField==null) {
		this.jq(ID_DISPLAY_CONTENTS).html("No source/target fields specified");
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
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
        needsData: function() {
            return true;
        },
	getWikiEditorTags: function() {
	    return Utils.mergeLists(SUPER.getWikiEditorTags(),
				    [
					"label:Color Boxes",
					'categoryField=""',
					'colorBy=""',
					'tableWidth=300',
					
				    ])},

        updateUI: function() {
	    let records = this.filterData();
	    if (!records) {
                return;
	    }  
	    let categoryField = this.getFieldById(null, this.getProperty("categoryField","category"));
	    if(categoryField==null) {
		this.jq(ID_DISPLAY_CONTENTS).html("No category field field specified");
		return;
	    }
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
	    let tableWidth=this.getProperty("tableWidth",300);
	    cats.sort((a,b)=>{
		return catMap[b].max - catMap[a].max;
	    });

	    cats.forEach(cat=>{
		let length = catMap[cat].list.length;
		let label = HU.span(["field-id",categoryField.getId(),
				     "field-value",cat], cat);
		let row = HU.open(TR,['valign','top'],HU.td(['align','right',CLASS,'display-colorboxes-header'],label+ "("+length+")"));
		row+=	  HU.open(TD,[WIDTH,'${tableWidth}']);
		if(colorBy.index) {
		    catMap[cat].list.sort((a,b)=>{
			return b.getData()[colorBy.index]-a.getData()[colorBy.index];
		    });
		}
		catMap[cat].list.map((record,idx)=>{
		    let color = "#ccc";
		    if(colorBy.index) {
			color =  colorBy.getColor(record.getData()[colorBy.index], record) || color;
		    }
		    row +=HU.div([TITLE,"",RECORD_INDEX, idx, CLASS,"display-colorboxes-box",STYLE,HU.css('background', color)],"");
		});
		row+=HU.close(TD,TR);
		html+=row;
	    });

	    html +=HU.close(TABLE);
            this.displayHtml(html);
	    colorBy.displayColorTable(500);
	    if(!this.getProperty("tooltip"))
		this.setProperty("tooltip","${default}");
	    this.makeTooltips(this.jq(ID_DISPLAY_CONTENTS).find(".display-colorboxes-box"),records);
	    this.addFieldClickHandler(null, records);
	}
    })
}



function RamaddaPercentchangeDisplay(displayManager, id, properties) {
    const SUPER =  new RamaddaFieldsDisplay(displayManager, id, DISPLAY_PERCENTCHANGE, properties);
    RamaddaUtil.inherit(this,SUPER);
    addRamaddaDisplay(this);
    $.extend(this, {
	getWikiEditorTags: function() {
	    return Utils.mergeLists(SUPER.getWikiEditorTags(),
				    [
					"label:Percent change Properties",
					'template="${date1} ${date2} ${value1} ${value2} ${percent} ${per_hour} ${per_day} ${per_week} ${per_month} ${per_year}"',
					'fieldLabel=""',
					'sortFields=false',
					'highlightPercent="50"',
					'highlightPercentPositive="50"',
					'highlightPercentNegative="-50"',
					'highlightColor=""',
					'highlightColorPositive=""',
					'highlightColorNegative=""',
					
				    ]);
	},
	updateUI: function() {
	    let records = this.filterData();
	    if(!records) return;
            let allFields = this.getData().getRecordFields();
            let fields = this.getSelectedFields(allFields);
	    fields = this.getFieldsOfType(fields, "numeric");
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
		html += HU.open(TABLE, [CLASS, "stripe nowrap ramadda-table", ID, this.getDomId("percentchange")]);
		html += HU.open(THEAD, []);
		html += "\n";
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
		    let h = template.replace("${field}", t.field.getLabel()).replace("${value1}",this.formatNumber(t.val1)).replace("${value2}",this.formatNumber(t.val2)).replace("${percent}",this.formatNumber(t.percent)).replace("${date1}",label1).replace("${date2}",label2).replace("${difference}", this.formatNumber(t.val2-t.val1));
		    
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
		    
		    html += HU.tr([STYLE,style], HU.td([], t.field.getLabel()) + 
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
	    this.writeHtml(ID_DISPLAY_CONTENTS, html); 
            HU.formatTable("#" + this.getDomId("percentchange"), {ordering:true
								  //scrollY: this.getProperty("tableSummaryHeight", tableHeight)
								 });
	},
    })
}



function RamaddaDatatableDisplay(displayManager, id, properties) {
    const SUPER  = new RamaddaDisplay(displayManager, id, DISPLAY_DATATABLE, properties);
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
        needsData: function() {
            return true;
        },
	getWikiEditorTags: function() {
	    return Utils.mergeLists(SUPER.getWikiEditorTags(),
				    [
					"label:Date Table",
					'columnSelector="date_day|date_hour|date_dow|date_month|date_year"',
					'selectors="date_day,date_hour,date_dow,date_month,date_year,date_fieldid"',
					'columnSelector="date_day|date_hour|date_dow|date_month"',
					'rowSelector="date_day|date_hour|date_dow|date_month"',
					'checkedIcon="fa-checked"',
					'checkedTooltipHeader="${numberChecked}"',
					'dataCheckers="match|notmatch|lessthan|greaterthan|equals|notequals(field=field,value=value,label=label,enabled=false) "', 
					'showColumnSelector=false',
					'showRowSelector=false',
					'showValues=false',
					'showColors=false',
					'showRowTotals=false',
					'showColumnTotals=false',
					'slantHeader=true'

				    ])},
        updateUI: function() {
            this.setContents(this.getLoadingMessage());
	    let records = this.filterData();
	    if (!records) {
                return;
	    }  
	    let colors = this.getColorTable(true);
	    if (!colors) colors = Utils.getColorTable("blues",true);
	    let checkers = this.getDataFilters(this.getProperty("dataCheckers"));
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
			    label = field.getLabel();
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
	    let showColors = this.getProperty("showColors", true);
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
	    let table = HU.open(TABLE,[STYLE,HU.css('font-size', this.getProperty("fontSize",'8pt')),CLASS,'display-colorboxes-table', 'cellpadding',0,'cellspacing',0,  WIDTH,'100%']);
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
	    table+=HU.td(['colspan',cellCount,CLASS,'display-datatable-footer','align','center',ID,this.getDomId("ct")]);
	    table+=HU.close(TR,TABLE);

	    if(topSpace>0) {
		table  = HU.div([STYLE,HU.css('margin-top', topSpace+'px')], table);
	    }

	    let html ="";
	    let header = HU.open(TABLE,[WIDTH,'100%']) + HU.open(TR);
	    if(this.getProperty("showRowSelector",true)) {
		header+=  HU.td([CLASS,"display-datatable-selector","width","10%"],HU.select("",[ID,this.getDomId("rowSelector")],
											     selectors,
											     rowSelector));
	    }
	    if(this.getProperty("showColumnSelector",true)) {
		header+=  HU.td([CLASS,"display-datatable-selector","width","90%","align","center"],  HU.select("",[ID,this.getDomId("columnSelector")],
														selectors,
														columnSelector));
	    }
	    header+=HU.close(TR,TABLE);
	    html+=header;
	    html+=table;
	    this.jq(ID_DISPLAY_CONTENTS).html(html);




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
	    this.jq(ID_DISPLAY_CONTENTS).find(".display-datatable-counts").each(function() {
		let key = $(this).attr("data-key");	
		let cell = cells[key];
		countFields.forEach(f=>{
		    let html = f.getLabel()+HU.tag(BR);
		    let cf = cell.countFields[f.getId()];
		    let data=[];
		    cf.values.forEach(v=>{
			data.push([v,cf.counts[v]]);
			html+= v +":" + cf.counts[v]+SPACE + HU.tag(BR);
		    });
		    let id = _this.getDomId(cell.row+"-"+cell.column+"-" + f.getId());
		    $(this).append(HU.div([CLASS,"display-datatable-piechart",ID,id,TITLE,"", STYLE,HU.css(WIDTH, pieWidth+'px',HEIGHT, pieWidth+'px')]));
		    drawPieChart(_this, "#"+id,pieWidth,pieWidth,data);
		    $("#" + id).tooltip({
			content: function() {
			    return html;
			}
		    });
		});
	    });
	    

	    this.jq(ID_DISPLAY_CONTENTS).find(".display-datatable-checked").tooltip({
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
		this.displayColorTable(colors, "ct", min,max,{});
	    }
	},
    })
}


function RamaddaSparklineDisplay(displayManager, id, properties) {
    const ID_INNER = "inner";
    if(!properties.groupBy)
	properties.displayInline = true;
    const SUPER =  new RamaddaFieldsDisplay(displayManager, id, DISPLAY_SPARKLINE, properties);
    RamaddaUtil.inherit(this,SUPER);
    addRamaddaDisplay(this);
    this.defineProperties([
	{label:'Sparkline Properties'},
	{p:'showDate',wikiValue:'true'},
	{p:'sparklineWidth',d:60},
	{p:'sparklineHeight',d:20},
	{p:'sparklineLineColor',wikiValue:'#000'},
	{p:'sparklineBarColor',wikiValue:'MediumSeaGreen'},
	{p:'sparklineCircleColor',wikiValue:'#000'},
	{p:'sparklineCircleRadius',wikiValue:'1'},
	{p:'sparklineLineWidth',wikiValue:'1'},
	{p:'sparklineShowLines',wikiValue:'true'},
	{p:'sparklineShowBars',wikiValue:'true'},
	{p:'sparklineShowCircles',wikiValue:'true'},
	{p:'sparklineShowEndPoints',wikiValue:'true'},
	{p:'sparklineEndPointRadius',wikiValue:'2'},
	{p:'sparklineEndPoint1Color',wikiValue:''},
	{p:'sparklineEndPoint1Color',wikiValue:'steelblue'},
	{p:'sparklineEndPointRadius',wikiValue:'2'},
	{p:'sparklineEndPoint2Color',wikiValue:''},
	{p:'sparklineEndPoint2Color',wikiValue:'tomato'},
    ]);

    $.extend(this, {
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
		this.jq(ID_DISPLAY_CONTENTS).html("No field specified");
		return;
	    }
	    let showDate = this.getPropertyShowDate();
	    let id = this.getDomId(ID_INNER);
	    let colorBy = this.getColorByInfo(records);
	    let groupByField = this.getFieldById(null,this.getProperty("groupBy"));
	    let groups = groupByField?RecordUtil.groupBy(records, this, null, groupByField):null;
	    let col = this.getColumnValues(records, field);
	    if(groups) {
		let labelPosition = this.getProperty("labelPosition","bottom");
		html = HU.div([ID,this.getDomId(ID_INNER)]);
		this.writeHtml(ID_DISPLAY_CONTENTS, html); 
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
		    drawSparkLine(this, "#"+gid,w,h,gcol.values,grecords,col.min,col.max,colorBy);
		});		
	    } else {
		html = HU.div([CLASS,"display-sparkline-sparkline",ID,this.getDomId(ID_INNER),STYLE,HU.css('width', w+'px','height', h+'px')]);
		if(showDate) {
		    html = HU.div([CLASS,"display-sparkline-date"],this.formatDate(records[0].getTime())) + html+
			HU.div([CLASS,"display-sparkline-date"],this.formatDate(records[records.length-1].getTime()))
		}
		this.writeHtml(ID_DISPLAY_CONTENTS, html); 
		drawSparkLine(this, "#"+id,w,h,col.values,records,col.min,col.max,colorBy);
	    }
	}
    });
}



function RamaddaPointimageDisplay(displayManager, id, properties) {
    if(!properties.width) properties.width="200";
    properties.displayInline = true;
    const SUPER =  new RamaddaFieldsDisplay(displayManager, id, DISPLAY_POINTIMAGE, properties);
    RamaddaUtil.inherit(this,SUPER);
    addRamaddaDisplay(this);
    $.extend(this, {
	getWikiEditorTags: function() {
	    return Utils.mergeLists(SUPER.getWikiEditorTags(),
				    [
					"label:Image Display",
					'cellShape="rect|circle"',
					'cellSize=4',
					'cellFilled=false',
					'cellColor=false',
					'doHeatmap=true',
					'padding=5',
					'borderColor=#ccc',
					'showTooltips=false',
					'colorBy=""'
				    ])},
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
	    let html = HU.div([ID,this.getDomId("inner"),STYLE,style]);
	    this.writeHtml(ID_DISPLAY_CONTENTS, html); 
	    let pad = 10;
	    let w = Math.round(this.jq("inner").width());
	    let h = Math.round(w/ratio);
            let divid = this.getProperty(PROP_DIVID);
	    //	    $("#"+ divid).css("height",h+pad);
	    html = HU.div([ID,this.getDomId("inner"),STYLE,HU.css('width', w +'height', h+'px') + style]);
	    html = HU.div([ID,this.getDomId("inner")]);
	    //	    this.jq(ID_DISPLAY_CONTENTS).css("height",h+pad);
	    this.writeHtml(ID_DISPLAY_CONTENTS, html); 
	    let colorBy = this.getColorByInfo(records);
	    bounds = RecordUtil.expandBounds(bounds,0.1);
	    let args =$.extend({colorBy:colorBy, w:w, h:h,cell3D:this.getProperty("cell3D"),bounds:bounds},
			       this.getDefaultGridByArgs());

	    args.doHeatmap=true;
	    let fields = this.getFields();
	    let img = Gfx.gridData(this.getId(),fields, records,args);
	    this.jq("inner").html(HU.image(img,[TITLE,"",ID,this.getDomId("image")]));
	    this.jq("inner").append(HU.div([ID,this.getDomId("tooltip"),STYLE,HU.css('z-index:','2000','display','none','position','absolute','background','#fff','border','1px solid #ccc','padding','0px')]));
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
    RamaddaUtil.inherit(this,SUPER);
    addRamaddaDisplay(this);
    this.defineProperties([
	{label:'Canvas Properties'},
	{p:'canvasWidth',d:100,wikiValue:"100",tt:'Canvas width'},
	{p:'canvasHeight',d:100,wikiValue:"100",tt:'Canvas height'},
	{p:'canvasStyle',d:"",wikiValue:"",tt:'Canvas CSS style'},
	{p:'titleTemplate',tt:'Template to show as title'},
	{p:'topTitleTemplate',tt:'Template to show as top title'},	
	{p:'urlField',tt:'Url Field'},
	{p:'canvasOrigin',d:"sw",wikiValue:"center",tt:'Origin point for drawing glyphs'},
	{label:'label glyph',p:"glyph1",wikiValue:"type:label,pos:sw,dx:10,dy:-10,label:field_colon_ ${field}_nl_field2_colon_ ${field2}"},
	{label:'rect glyph', p:"glyph1",wikiValue:"type:rect,pos:sw,dx:10,dy:0,colorBy:field,width:150,height:100"},
	{label:'circle glyph',p:"glyph1",wikiValue:"type:circle,pos:n,dx:10,dy:-10,fill:true,colorBy:field,width:20,baseWidth:5,sizeBy:field"},
	{label:'3dbar glyph', p:"glyph1",wikiValue:"type:3dbar,pos:sw,dx:10,dy:-10,height:30,width:8,baseHeight:5,sizeBy:field"},
	{label:'gauge glyph',p:"glyph1",wikiValue:"type:gauge,color:#000,pos:sw,width:50,height:50,dx:10,dy:-10,sizeBy:field,sizeByMin:0"},
    ]);
    $.extend(this, {
        needsData: function() {
            return true;
        },
	updateUI: function() {
	    let _this = this;
	    let records = this.filterData();
	    let fields = this.getFields();
	    if(!records) return;
	    let style = this.getPropertyCanvasStyle("");
	    let columns = this.getProperty("columns");
	    let html = "";
	    let canvasWidth = this.getPropertyCanvasWidth();
	    let canvasHeight = this.getPropertyCanvasHeight();
	    let titleTemplate= this.getPropertyTitleTemplate();
	    let topTitleTemplate= this.getPropertyTopTitleTemplate();
	    let urlField = this.getFieldById(null,this.getPropertyUrlField());
	    records.forEach((record,idx)=>{
		let cid = this.getDomId("canvas_" + idx);
		let c = HU.tag("canvas",[CLASS,"display-canvas-canvas", STYLE,style, 
					 WIDTH,canvasWidth,HEIGHT,canvasHeight,ID,cid]);
		let topTitle  =topTitleTemplate?
		    HU.div([CLASS,"display-canvas-title"], 
			   this.getRecordHtml(record, null, topTitleTemplate)):"";
		let title  = titleTemplate?
		    HU.div([CLASS,"display-canvas-title"], 
			   this.getRecordHtml(record, null, titleTemplate)):"";	
		let div =  HU.div([TITLE,"",CLASS,"display-canvas-block", "recordIndex",idx], topTitle+c+title);
		if(urlField) {
		    let url = record.getValue(urlField.getIndex());
		    if(Utils.stringDefined(url))
			div = HU.href(url,div);
		}
		html+=div;
	    });
	    this.setContents(html);
	    let glyphs=[];
	    let cnt = 1;
	    while(cnt<11) {
		let attr = this.getProperty("glyph" + (cnt++));
		if(!attr)
		    continue;
		glyphs.push(new Glyph(this,1.0, fields,records,{
		    canvasWidth:canvasWidth,
		    canvasHeight: canvasHeight
		},attr));
	    }
	    let opts = {};
	    let originX = 0;
	    let originY=this.getPropertyCanvasOrigin()=="center"?canvasHeight/2:canvasHeight;
	    records.forEach((record,idx)=>{
		let cid = this.getDomId("canvas_" + idx);
		let canvas = document.getElementById(cid);
		let ctx = canvas.getContext("2d");
		glyphs.forEach(glyph=>{
		    glyph.draw(opts, canvas, ctx, originX,originY,{record:record});
		});
	    });
	    let blocks = this.jq(ID_DISPLAY_CONTENTS).find(".display-canvas-block");
	    this.makeTooltips(blocks,records,null,"${default}");
	}
    });
}


function RamaddaFieldtableDisplay(displayManager, id, properties) {
    const ID_TABLE = "fieldtable";
    const SUPER =  new RamaddaFieldsDisplay(displayManager, id, DISPLAY_FIELDTABLE, properties);
    RamaddaUtil.inherit(this,SUPER);
    addRamaddaDisplay(this);
    $.extend(this, {
	getWikiEditorTags: function() {
	    return Utils.mergeLists(SUPER.getWikiEditorTags(),
				    [
					"label:Field Table",
					'field=""',
					'labelField=field',
					'columnWidth=150',
					'tableHeight=300',
					'markerShape=circle|rect|triangle|bar|arrow|dart|bar',
					'markerSize=16',
					'markerFill=#64CDCC',
					'markerStroke=#000'
				    ])},
        needsData: function() {
            return true;
        },
	updateUI: function() {
	    let records = this.filterData();
	    if(!records) return;
	    let fields = this.getFieldsByIds(null,this.getProperty("fields"));
	    if(fields.length==0) 
		fields = this.getFieldsOfType(null, "numeric");
	    let labelField = this.getFieldById(null, this.getProperty("labelField"));
	    if(!labelField) labelField = this.getFieldsOfType(null, "string")[0];
	    let html = HU.open(TABLE,[CLASS, "", "border",0,ID,this.getDomId(ID_TABLE)]);
	    html += HU.open(THEAD);
	    let width = this.getProperty("columnWidth",150)
	    html += HU.open(TR,[]);
	    html+=HU.td(["width",width],
			HU.div([CLASS,"display-fieldtable-header"],labelField?labelField.getLabel():""));
	    let columns = {};
	    fields.forEach(f=>{
		columns[f.getId()] = this.getColumnValues(records, f);
	    });

	    fields.forEach(f=>{
		html+=HU.th(["width",width],HU.div([CLASS,"display-fieldtable-header"],f.getLabel()));
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
		html += HU.open(TR,["valign","center",RECORD_INDEX,idx,CLASS,"display-fieldtable-row"]);
		html+=HU.td([STYLE,HU.css('vertical-align','center'),'align','right'],HU.div(hdrAttrs,label));
		fields.forEach(f=>{
		    let v = r.getValue(f.getIndex());
		    let c = columns[f.getId()];
		    let contents = "";
		    if(isNaN(v) || c.min == c.max) return;
		    let perc = 100*(v-c.min)/(c.max-c.min);
		    let cid = this.getDomId("cid" + (cnt++));
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
		html+=HU.td([],HU.div([STYLE,HU.css('max-width', width+'px','overflow-x','auto'),ID, this.getDomId("footer-" + idx)],""));
	    });
	    html+=HU.close(TR);
	    html += HU.close(TFOOT);

	    html+=HU.close(TABLE);
	    this.writeHtml(ID_DISPLAY_CONTENTS, html); 
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

            HU.formatTable("#" + this.getDomId(ID_TABLE), opts);
	    let rows = this.jq(ID_DISPLAY_CONTENTS).find(".display-fieldtable-row");
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



function RamaddaDotstackDisplay(displayManager, id, properties) {
    const SUPER =  new RamaddaFieldsDisplay(displayManager, id, "dotstack", properties);
    RamaddaUtil.inherit(this,SUPER);
    addRamaddaDisplay(this);
    $.extend(this, {
	getWikiEditorTags: function() {
	    return Utils.mergeLists(SUPER.getWikiEditorTags(),
				    [
					"label:Dot Stack",
					'categoryField=field',
				    ])},
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
			[TITLE,"", RECORD_INDEX,idToIndex[r.getId()],CLASS, "display-dotstack-dot",STYLE,HU.css('width', w+'px','height', w +'px','background', c)],"");
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
	    this.writeHtml(ID_DISPLAY_CONTENTS, html); 
	    let dots = this.jq(ID_DISPLAY_CONTENTS).find(".display-dotstack-dot");
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
    RamaddaUtil.inherit(this,SUPER);
    addRamaddaDisplay(this);
    this.defineProperties([
	{label:'Dot Bar'},
	{p:'keyField'},
	{p:'dotSize',d:16}
    ]);
    this.defineSizeByProperties();
    $.extend(this, {
        needsData: function() {
            return true;
        },
	updateUI: function() {
	    let records = this.filterData();
	    if(!records) return;
	    let keyField = this.getFieldById(null,this.getPropertyKeyField());
	    let fields = this.getFieldsByIds(null,this.getProperty("fields"));
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
		let cid = this.getDomId("dots"+idx);
		let column = this.getColumnValues(records, f);
		html += HU.open(TR, [VALIGN,'center']);
		html += HU.td([WIDTH,'10%', ALIGN,'right'],  HU.div([STYLE,HU.css('margin-right','8px')], f.getLabel().replace(/ /g,SPACE)));
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
		    html +=  HU.span([RECORD_INDEX,idx2,CLASS,clazz,STYLE,HU.css('border',dotBorder, "background",c,"position",'absolute','top',HU.getDimension(top),'left', perc+'%')+style, RECORD_INDEX,idx2, TITLE,""]); 
		});

		html += HU.close(DIV,TD);
		html += HU.td([WIDTH, (dotSize*2)]);
		html += HU.td([ALIGN,"left", WIDTH,"5%"],HU.div([STYLE,HU.css('margin-left','10px')],this.formatNumber(column.max)));
		html+=HU.close(TR);
	    });
	    let t2 = new Date();
	    html+=HU.close(TABLE);
	    this.writeHtml(ID_DISPLAY_CONTENTS, html); 
	    let t3 = new Date();
	    let dots = this.jq(ID_DISPLAY_CONTENTS).find(".display-dotbar-dot");
	    let t4 = new Date();
	    let _this = this;
	    dots.mouseleave(function() {
		dots.removeClass("display-dotbar-dot-highlight");
	    });
	    dots.mouseover(function() {
		let idx = $(this).attr(RECORD_INDEX);
		dots.removeClass("display-dotbar-dot-highlight");
		_this.jq(ID_DISPLAY_CONTENTS).find("[" + RECORD_INDEX+"=\"" + idx+"\"]").addClass( "display-dotbar-dot-highlight");
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
		_this.jq(ID_DISPLAY_CONTENTS).find("[" + RECORD_INDEX+"=\"" + idx+"\"]").addClass( "display-dotbar-dot-select");
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


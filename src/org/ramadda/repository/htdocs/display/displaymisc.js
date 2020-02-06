/*
  Copyright 2008-2019 Geode Systems LLC
*/



/*
  var url = "https://bost.ocks.org/mike/miserables/miserables.json";
  fetch(url).then(res => res.json()).then(data=>{
  var names = {};
  data.nodes.map((name,idx)=>{
  names[idx] = name.name;
  });
  var csv ="";
  data.links.map(l=>{
  var s = names[l.source];
  var t = names[l.target];
  csv += s+"," + t +"," + l.value +"\n";
  });
  Utils.makeDownloadFile("lesmiserables.csv", csv);
  });
*/


var DISPLAY_GRAPH = "graph";
var DISPLAY_TREE = "tree";
var DISPLAY_ORGCHART = "orgchart";
var DISPLAY_TIMELINE = "timeline";
var DISPLAY_BLANK = "blank";
var DISPLAY_RECORDS = "records";
var DISPLAY_TSNE = "tsne";
var DISPLAY_HEATMAP = "heatmap";
var DISPLAY_CROSSTAB = "crosstab";
var DISPLAY_CORRELATION = "correlation";
var DISPLAY_RANKING = "ranking";
var DISPLAY_STATS = "stats";
var DISPLAY_COOCCURENCE = "cooccurence";
var DISPLAY_BOXTABLE = "boxtable";
var DISPLAY_DATATABLE = "datatable";
var DISPLAY_PERCENTCHANGE = "percentchange";


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
    var SUPER;
    RamaddaUtil.inherit(this, SUPER = new RamaddaDisplay(displayManager, id,
							 DISPLAY_GRAPH, properties));
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
					"label:Graph Attributes",
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
	    let html = HtmlUtils.div(["id", this.getDomId(ID_GRAPH)]);
	    this.jq(ID_DISPLAY_CONTENTS).html(html);

	    if(doGlobalGraphData) {
		if(!globalGraphData) {
		    setTimeout(()=>{
			this.updateUI();
		    },100);
		}
		graphData = globalGraphData;
	    } else {
		var records = this.filterData();
		if (!records) {
                    return;
		}  
		let seenNodes = {};
		let nodes = [];
		let links = [];
		let valueFields   = this.getFieldsByIds(null, this.getProperty("valueFields","",true));
		let labelField = this.getFieldById(null, this.getProperty("labelField"));
		if(!labelField) {
		    var strings = this.getFieldsOfType(null, "string");
		    if(strings.length>0) labelField = strings[0];
		}
		let sourceField = this.getFieldById(null, this.getProperty("sourceField","source"));
		let targetField = this.getFieldById(null, this.getProperty("targetField","target"));
		var textTemplate = this.getProperty("tooltip","${default}");
		if(valueFields.length>0) {
		    let seenValue = {};
		    records.map((r,index)=>{
			var label  = labelField?r.getValue(labelField.getIndex()):index;
			var tooltip =  this.getRecordHtml(r, null, textTemplate);
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
			var source = r.getValue(sourceField.getIndex());
			var target = r.getValue(targetField.getIndex());
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
	    }

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
		ctx.font = `${fontSize}px Sans-Serif`;
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
		roots = this.makeTree(records);
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



function OrgchartDisplay(displayManager, id, properties) {
    let ID_ORGCHART = "orgchart";
    let SUPER = new RamaddaDisplay(displayManager, id, DISPLAY_ORGCHART, properties);
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);
    $.extend(this, {
        handleEventRecordSelection: function(source, args) {},
	getWikiEditorTags: function() {
	    return Utils.mergeLists(SUPER.getWikiEditorTags(),
				    [
					'label:Orgchart Attributes',
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
            this.displayHtml(HtmlUtils.div(["id",this.getDomId(ID_ORGCHART)],"HELLO"));
	    //	    console.log(this.jq(ID_ORGCHART).length);
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
            var data = new google.visualization.DataTable();
            data.addColumn('string', 'Name');
            data.addColumn('string', 'Parent');
            data.addColumn('string', 'ToolTip');
	    let rows = [];
	    let func = function(node) {
		cnt++;
		var value = node.label;
		if(node.display) {
		    value = {'v':node.label,f:node.display};
		}
		var row = [value, node.parent?node.parent.label:"",node.tooltip||""];
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
            var chart = new google.visualization.OrgChart(document.getElementById(this.getDomId(ID_ORGCHART)));
            // Draw the chart, setting the allowHtml option to true for the tooltips.
            chart.draw(data, {'allowHtml':true,'allowCollapse':true,
			      'size':this.getProperty("treeNodeSize","medium")});
	}
    });
}


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


function RamaddaTsneDisplay(displayManager, id, properties) {
    var ID_BOTTOM = "bottom";
    var ID_CANVAS = "tsnecanvas";
    var ID_DETAILS = "tsnedetails";
    var ID_RUN = "tsnerun";
    var ID_RESET = "tsnereset";
    var ID_STEP = "tsnestep";
    var ID_SEARCH = "tsnesearch";
    $.extend(this, {
        colorTable: "red_white_blue",
        colorByMin: "-1",
        colorByMax: "1",
        height: "500px;"
    });

    let SUPER = new RamaddaFieldsDisplay(displayManager, id, DISPLAY_TSNE, properties);
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
            var height = this.getProperty("height");
            if (height.endsWith("px")) height = height.replace("px", "");
            height = parseInt(height);
            //            height-=30;
            var details = HtmlUtils.div(["style", "height:" + height + "px;max-height:" + height + "px", "class", "display-tnse-details", "id", this.getDomId(ID_DETAILS)], "");
            var canvas = HtmlUtils.div(["class", "display-tnse-canvas-outer", "style", "height:" + height + "px"], HtmlUtils.div(["class", "display-tnse-canvas", "id", this.getDomId(ID_CANVAS)], ""));
            var buttons = HtmlUtils.div(["id", this.getDomId(ID_RUN), "class", "ramadda-button", "what", "run"], "Stop") + "&nbsp;" +
                HtmlUtils.div(["id", this.getDomId(ID_STEP), "class", "ramadda-button", "what", "step"], "Step") + "&nbsp;" +
                HtmlUtils.div(["id", this.getDomId(ID_RESET), "class", "ramadda-button", "what", "reset"], "Reset") + "&nbsp;" +
                HtmlUtils.input("", "", ["id", this.getDomId(ID_SEARCH), "placeholder", "search"]);

            buttons = HtmlUtils.div(["class", "display-tnse-toolbar"], buttons);
            this.jq(ID_TOP_LEFT).append(buttons);
            this.setContents("<table  width=100%><tr valign=top><td width=80%>" + canvas + "</td><td width=20%>" + details + "</td></tr></table>");
            this.search = this.jq(ID_SEARCH);
            this.search.keyup(e => {
                var v = this.search.val().trim();
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
            var allFields = this.dataCollection.getList()[0].getRecordFields();
            if (!this.fields) {
                this.fields = this.getSelectedFields([]);
                if (this.fields.length == 0) this.fields = allFields;
                var strings = this.getFieldsOfType(this.fields, "string");
                if (strings.length > 0)
                    this.textField = strings[0];
            }
            var data = [];
            for (var rowIdx = 1; rowIdx < this.dataList.length; rowIdx++) {
                var tuple = this.getDataValues(this.dataList[rowIdx]);
                var nums = [];
                for (var i = 0; i < this.fields.length; i++) {
                    if (this.fields[i].isNumeric()){
                        var v = tuple[this.fields[i].getIndex()];
                        if(isNaN(v)) v = 0;
                        nums.push(v);
                    }
                }
                data.push(nums);
            }

            var opt = {}
            opt.epsilon = 10; // epsilon is learning rate
            opt.perplexity = 30; // how many neighbors each point influences
            opt.dim = 2; // dimensionality of the embedding (2 = default)
            this.tsne = new tsnejs.tSNE(opt);
            this.tsne.initDataRaw(data);
            this.takeStep();
        },
        takeStep: function() {
            var numSteps = 10;
            for (var step = 0; step < numSteps; step++) {
                this.tsne.step();
            }

            var pts = this.tsne.getSolution();
            var minx, miny, maxx, maxy;
            for (var i = 0; i < pts.length; i++) {
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
            var sleep = 250;
            for (var i = 0; i < pts.length; i++) {
                var x = pts[i][0];
                var y = pts[i][1];
                var px = 100 * (x - minx) / (maxx - minx);
                var py = 100 * (y - miny) / (maxy - miny);
                if (!this.haveStepped) {
                    var title = "";
                    if (this.textField) {
                        var tuple = this.getDataValues(this.dataList[i]);
                        title = tuple[this.textField.getIndex()];
                    }
                    if (title.length > 10) {
                        title.length = 10;
                    }
                    this.nameToIndex[title] = i;
                    this.canvas.append(HtmlUtils.div(["title", title, "index", i, "id", this.getDomId("element-" + i), "class", "display-tnse-mark", "style", "left:" + px + "%;" + "top:" + py + "%;"], title));
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
                    var index = parseInt($(this).attr("index"));
                    if (index < 0 || index >= _this.dataList.length) return;
                    var tuple = _this.getDataValues(_this.dataList[index]);
                    var details = "<table class=formtable width=100% >";
                    for (var i = 0; i < _this.fields.length; i++) {
                        var field = _this.fields[i];
                        details += "<tr><td align=right class=formlabel>" + field.getLabel() + ":</td><td>" + tuple[field.getIndex()] + "</td></tr>";
                    }
                    details += "</table>";
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
    let SUPER = new RamaddaFieldsDisplay(displayManager, id, DISPLAY_HEATMAP, properties);
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
        "map-display": false,
        needsData: function() {
            return true;
        },
        getMenuItems: function(menuItems) {
            SUPER.getMenuItems.call(this, menuItems);
            var get = this.getGet();
            var tmp = HtmlUtils.formTable();
            var colorTable = this.getColorTableName();
            var ct = "<select id=" + this.getDomId("colortable") + ">";
            for (table in Utils.ColorTable) {
                if (table == colorTable)
                    ct += "<option selected>" + table + "</option>";
                else
                    ct += "<option>" + table + "</option>";
            }
            ct += "</select>";

            tmp += HtmlUtils.formEntry("Color Table:", ct);

            tmp += HtmlUtils.formEntry("Color By Range:", HtmlUtils.input("", this.colorByMin, ["size", "7", ATTR_ID, this.getDomId("colorbymin")]) + " - " +
				       HtmlUtils.input("", this.colorByMax, ["size", "7", ATTR_ID, this.getDomId("colorbymax")]));
            tmp += "</table>";
            menuItems.push(tmp);
        },
        initDialog: function() {
            SUPER.initDialog.call(this);
            var _this = this;
            var updateFunc = function() {
                _this.colorByMin = _this.jq("colorbymin").val();
                _this.colorByMax = _this.jq("colorbymax").val();
                _this.updateUI();

            };
            var func2 = function() {
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
            var height = this.getProperty("height", -1);
            if (height > 0) {
                return " height:" + height + "px; " + " max-height:" + height + "px; overflow-y: auto;";
            }
            return "";
        },
        updateUI: function(pointData) {
            var _this = this;
            if (!haveGoogleChartsLoaded()) {
                var func = function() {
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
            var dataList = this.getStandardData(null, {
                includeIndex: true
            });
            var header = this.getDataValues(dataList[0]);
            var showIndex = this.getProperty("showIndex", true);
            var showValue = this.getProperty("showValue", true);
            var textColor = this.getProperty("textColor", "black");

            var cellHeight = this.getProperty("cellHeight", null);
            var extraTdStyle = "";
            if (this.getProperty("showBorder", "false") == "true") {
                extraTdStyle = "border-bottom:1px #666 solid;";
            }
            var extraCellStyle = "";
            if (cellHeight)
                extraCellStyle += "height:" + cellHeight + "px; max-height:" + cellHeight + "px; min-height:" + cellHeight + "px;";
            var allFields = this.dataCollection.getList()[0].getRecordFields();
            var fields = this.getSelectedFields([]);

            if (fields.length == 0) fields = allFields;
            var html = "";
            var colors = null;
            var colorByMin = null;
            var colorByMax = null;
            if (Utils.stringDefined(this.getProperty("colorByMins"))) {
                colorByMin = [];
                var c = this.getProperty("colorByMins").split(",");
                for (var i = 0; i < c.length; i++) {
                    colorByMin.push(parseFloat(c[i]));
                }
            }
            if (Utils.stringDefined(this.getProperty("colorByMaxes"))) {
                colorByMax = [];
                var c = this.getProperty("colorByMaxes").split(",");
                for (var i = 0; i < c.length; i++) {
                    colorByMax.push(parseFloat(c[i]));
                }
            }

            if (Utils.stringDefined(this.getProperty("colorTables"))) {
                var c = this.getProperty("colorTables").split(",");
                colors = [];
                for (var i = 0; i < c.length; i++) {
                    var name = c[i];
                    if (name == "none") {
                        colors.push(null);
                        continue;
                    }
                    var ct = Utils.getColorTable(name, true);
                    //                        console.log("ct:" + name +" " +(ct!=null));
                    colors.push(ct);
                }
            } else {
                colors = [this.getColorTable(true)];
            }
            var mins = null;
            var maxs = null;
            for (var rowIdx = 1; rowIdx < dataList.length; rowIdx++) {
                var row = this.getDataValues(dataList[rowIdx]);
                if (mins == null) {
                    mins = [];
                    maxs = [];
                    for (var colIdx = 1; colIdx < row.length; colIdx++) {
                        mins.push(Number.MAX_VALUE);
                        maxs.push(Number.MIN_VALUE);
                    }
                }

                for (var colIdx = 0; colIdx < fields.length; colIdx++) {
                    var field = fields[colIdx];
                    //Add one to the field index to account for the main time index
                    var index = field.getIndex() + 1;
                    if (!field || !field.isFieldNumeric() || field.isFieldGeo()) continue;

                    var value = row[index];
                    if (value == Number.POSITIVE_INFINITY || isNaN(value) || !Utils.isNumber(value) || !Utils.isDefined(value) || value == null) {
                        continue;
                    }
                    mins[colIdx] = Math.min(mins[colIdx], value);
                    maxs[colIdx] = Math.max(maxs[colIdx], value);
                }
            }

            html += HtmlUtils.openTag("table", ["border", "0", "class", "display-heatmap"]);
            html += "<tr valign=bottom>";
            if (showIndex) {
                html += "<td align=center>" + HtmlUtils.tag("div", ["class", "display-heatmap-heading-top"], header[0]) + "</td>";
            }
            for (var fieldIdx = 0; fieldIdx < fields.length; fieldIdx++) {
                var field = fields[fieldIdx];
                if ((!field.isFieldNumeric() || field.isFieldGeo())) continue;
                html += "<td align=center>" + HtmlUtils.tag("div", ["class", "display-heatmap-heading-top"], field.getLabel()) + "</td>";
            }
            html += "</tr>\n";




            for (var rowIdx = 1; rowIdx < dataList.length; rowIdx++) {
                var row = this.getDataValues(dataList[rowIdx]);
                var index = row[0];
                //check if its a date
                if (index.f) {
                    index = index.f;
                }
                var rowLabel = index;
                html += "<tr valign='center'>\n";
                if (showIndex) {
                    html += HtmlUtils.td(["class", "display-heatmap-heading-side", "style", extraCellStyle], rowLabel);
                }
                var colCnt = 0;
                for (var colIdx = 0; colIdx < fields.length; colIdx++) {
                    var field = fields[colIdx];
                    //Add one to the field index to account for the main time index
                    var index = field.getIndex() + 1;
                    if (!field || !field.isFieldNumeric() || field.isFieldGeo()) continue;
                    var style = "";
                    var value = row[index];
                    var min = mins[colIdx];
                    var max = maxs[colIdx];
                    if (colorByMin && colCnt < colorByMin.length)
                        min = colorByMin[colCnt];
                    if (colorByMax && colCnt < colorByMax.length)
                        max = colorByMax[colCnt];


                    var ok = min != max && !(value == Number.POSITIVE_INFINITY || isNaN(value) || !Utils.isNumber(value) || !Utils.isDefined(value) || value == null);
                    var title = header[0] + ": " + rowLabel + " - " + field.getLabel() + ": " + value;
                    if (ok && colors != null) {
                        var ct = colors[Math.min(colCnt, colors.length - 1)];
                        if (ct) {
                            var percent = (value - min) / (max - min);
                            var ctIndex = parseInt(percent * ct.length);
                            if (ctIndex >= ct.length) ctIndex = ct.length - 1;
                            else if (ctIndex < 0) ctIndex = 0;
                            style = "background-color:" + ct[ctIndex] + ";";
                        }
                    }
                    var number;
                    if (!ok) {
                        number = "-";
                    } else {
                        number = Utils.formatNumber(value)
                    }
                    if (!showValue) number = "";
                    html += HtmlUtils.td(["valign", "center", "align", "right", "style", style + extraCellStyle + extraTdStyle, "class", "display-heatmap-cell"], HtmlUtils.div(["title", title, "style", extraCellStyle + "color:" + textColor], number));
                    colCnt++;
                }
                html += "</tr>";
            }
            html += "</table>";
            this.setContents(html);
            this.initTooltip();

        },
    });
}


function RamaddaRankingDisplay(displayManager, id, properties) {
    var ID_TABLE = "table";
    $.extend(this, {
	height: "500px;",
        sortAscending:false,
    });
    if(properties.sortAscending) this.sortAscending = "true" == properties.sortAscending;
    let SUPER = new RamaddaFieldsDisplay(displayManager, id, DISPLAY_RANKING, properties);
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);

    RamaddaUtil.defineMembers(this, {
	getWikiEditorTags: function() {
	    return Utils.mergeLists(SUPER.getWikiEditorTags(),
				    [
					"label:Chart Attributes",
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
            if (!this.hasData()) {
                this.setContents(this.getLoadingMessage());
                return;
            }
            var dataList = this.getStandardData(null, {
                includeIndex: false
            });
            var allFields = this.dataCollection.getList()[0].getRecordFields();
            var fields = this.getSelectedFields([]);
            if (fields.length == 0) fields = allFields;
            var numericFields = this.getFieldsOfType(fields, "numeric");
            var sortField = this.getFieldById(numericFields, this.getProperty("sortField","",true));
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

            var stringFields = this.getFieldsByIds(allFields, this.getProperty("nameFields","",true));
            if(stringFields.length==0) {
		var tmp = this.getFieldById(allFields, this.getProperty("nameField","",true));
		if(tmp) stringFields.push(tmp);
	    }
            if(stringFields.length==0) {
                var stringField = this.getFieldOfType(allFields, "string");
		if(stringField) stringFields.push(stringField);
	    }
            var menu = "<select class='ramadda-pulldown' id='" + this.getDomId("sortfields") + "'>";
            for (var i = 0; i < numericFields.length; i++) {
                var field = numericFields[i];
                var extra = "";
                if (field.getId() == sortField.getId()) extra = " selected ";
                menu += "<option value='" + field.getId() + "'  " + extra + " >" + field.getLabel() + "</option>\n";
            }
            menu += "</select>" ;
	    var top ="";
	    top += HtmlUtils.span(["id",this.getDomId("sort")], HtmlUtils.getIconImage(this.sortAscending?"fa-sort-up":"fa-sort-down", ["style","cursor:pointer;","title","Change sort order"]));
            if (this.getProperty("showRankingMenu", true)) {
                top+= " " + HtmlUtils.div(["style","display:inline-block;", "class","display-filterby"],menu);
            }
	    this.jq(ID_TOP_LEFT).html(top);
	    this.jq("sort").click(()=>{
		this.sortAscending= !this.sortAscending;
		if(this.sortAscending) 
		    this.jq("sort").html(HtmlUtils.getIconImage("fa-sort-up", ["style","cursor:pointer;"]));
		else
		    this.jq("sort").html(HtmlUtils.getIconImage("fa-sort-down", ["style","cursor:pointer;"]));
		this.updateUI();
	    });
            var html = "";
            html += HtmlUtils.openTag("div", ["style", "max-height:100%;overflow-y:auto;"]);
            html += HtmlUtils.openTag("table", ["id", this.getDomId(ID_TABLE)]);
            var tmp = [];
            for (var rowIdx = 1; rowIdx < dataList.length; rowIdx++) {
                var obj = dataList[rowIdx];
                obj.originalRow = rowIdx;
                tmp.push(obj);
            }

	    var includeNaN = this.getProperty("includeNaN",false);
	    if(!includeNaN) {
		var tmp2 = [];
		tmp.map(r=>{
		    var t = this.getDataValues(r);
		    var v = t[sortField.getIndex()];
		    if(!isNaN(v)) tmp2.push(r);
		});
		tmp = tmp2;
	    }
            var cnt = 0;
            tmp.sort((a, b) => {
                var t1 = this.getDataValues(a);
                var t2 = this.getDataValues(b);
                var v1 = t1[sortField.getIndex()];
                var v2 = t2[sortField.getIndex()];
		
                if (v1 < v2) return this.sortAscending?-1:1;
                if (v1 > v2) return this.sortAscending?1:-1;
                return 0;
            });


            for (var rowIdx = 0; rowIdx < tmp.length; rowIdx++) {
                var obj = tmp[rowIdx];
                var tuple = this.getDataValues(obj);
                var label = "";
                stringFields.map(f=>{
		    label += tuple[f.getIndex()]+" ";
		});

                label = label.trim();
		value = tuple[sortField.getIndex()];
                if (isNaN(value) || value === null) {
		    if(!includeNaN) continue;
		    value = "NA";
		}
		html += "<tr valign=top class='display-ranking-row' what='" + obj.originalRow + "'><td> #" + (rowIdx + 1) + "</td><td>&nbsp;" + label + "</td><td align=right>&nbsp;" +
                    value + "</td></tr>";
            }
            html += HtmlUtils.closeTag("table");
            html += HtmlUtils.closeTag("div");
            this.setContents(html);
            let _this = this;
            this.jq(ID_TABLE).find(".display-ranking-row").click(function(e) {
                _this.getDisplayManager().propagateEventRecordSelection(_this, _this.getPointData(), {
                    index: parseInt($(this).attr("what")) - 1
                });
            });
	    HtmlUtils.initSelect(this.jq("sortfields"));
            this.jq("sortfields").change(function() {
                _this.setProperty("sortField", $(this).val());
                _this.updateUI();
            });
        },
    });
}



function RamaddaCrosstabDisplay(displayManager, id, properties) {
    var ID_TABLE = "crosstab";
    let SUPER = new RamaddaFieldsDisplay(displayManager, id, DISPLAY_CROSSTAB, properties);
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
            var allFields = this.dataCollection.getList()[0].getRecordFields();
	    var enums = [];
	    allFields.map(field=>{
		var label = field.getLabel();
		if(label.length>30) label = label.substring(0,29);
		enums.push([field.getId(),label]);
	    });
	    var select = HtmlUtils.span(["class","display-filterby"],
					"Display: " + HtmlUtils.select("",["style","", "id",this.getDomId("crosstabselect")],enums,
								       this.getProperty("column", "", true)));


            this.setContents(select+HtmlUtils.div(["id",this.getDomId(ID_TABLE)]));
	    let _this = this;
	    this.jq("crosstabselect").change(function() {
		_this.setProperty("column", $(this).val());
		_this.makeTable();
	    });
	    this.makeTable();
	},
	makeTable: function() {
            var dataList = this.getStandardData(null, {
                includeIndex: false
            });
            var allFields = this.dataCollection.getList()[0].getRecordFields();
	    var col =  this.getFieldById(null, this.getProperty("column", "", true));
	    var rows =  this.getFieldsByIds(null, this.getProperty("rows", null, true));
	    if(!col) col  = allFields[0];
	    if(rows.length==0) rows  = allFields;

            var html = HtmlUtils.openTag("table", ["border", "1px", "bordercolor", "#ccc", "class", "display-crosstab", "cellspacing", "1", "cellpadding", "2"]);
	    var total = dataList.length-1;
	    var cnt =0;
	    rows.map((row)=>{
		if(row.getId()==col.getId()) return;
		cnt++;
		var colValues = [];
		var rowValues = [];
		var count ={};
		var rowcount ={};
		var colcount ={};
		for (var rowIdx = 1; rowIdx < dataList.length; rowIdx++) {
		    var tuple = this.getDataValues(dataList[rowIdx]);
		    var colValue = (""+tuple[col.getIndex()]).trim();
		    var rowValue = (""+tuple[row.getIndex()]).trim();
		    var key = colValue+"--" + rowValue;
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
		    html+="<tr><td></td><td align=center class=display-crosstab-header colspan=" + colValues.length+">" + col.getLabel()+"</td><td>&nbsp;</td></tr>";
		html+="<tr valign=bottom class=display-crosstab-header-row><td class=display-crosstab-header>" + row.getLabel() +"</td>";
		for(var j=0;j<colValues.length;j++) {
		    var colValue = colValues[j];
		    html+="<td>" + (colValue==""?"&lt;blank&gt;":colValue) +"</td>";
		}
		html+="<td><b>Total</b></td>";
		html+="</tr>";
		for(var i=0;i<rowValues.length;i++) {
		    var rowValue = rowValues[i];
		    html+="<tr>";
		    html+="<td>" + (rowValue==""?"&lt;blank&gt;":rowValue) +"</td>";
		    for(var j=0;j<colValues.length;j++) {
			var colValue = colValues[j];
			var key = colValue+"--" + rowValue;
			if(Utils.isDefined(count[key])) {
			    var perc = Math.round(count[key]/total*100) +"%";
			    html+="<td align=right>" + count[key] +"&nbsp;(" + perc+")</td>";
			} else {
			    html+="<td>&nbsp;</td>";
			}
		    }
		    var perc = Math.round(rowcount[rowValue]/total*100) +"%";
		    html+="<td align=right>" + rowcount[rowValue] +"&nbsp;(" + perc+")</td>";
		    html+="</tr>";
		}
	    });
            html += "</table>";
	    this.jq(ID_TABLE).html(html);
        },
    });
}




function RamaddaCorrelationDisplay(displayManager, id, properties) {
    var ID_BOTTOM = "bottom";
    $.extend(this, {
        colorTable: "red_white_blue",
        colorByMin: "-1",
        colorByMax: "1",
    });

    let SUPER = new RamaddaFieldsDisplay(displayManager, id, DISPLAY_CORRELATION, properties);
    RamaddaUtil.inherit(this, SUPER);
    addRamaddaDisplay(this);

    RamaddaUtil.defineMembers(this, {
        "map-display": false,
        needsData: function() {
            return true;
        },
        getMenuItems: function(menuItems) {
            SUPER.getMenuItems.call(this, menuItems);
            var get = this.getGet();
            var tmp = HtmlUtils.formTable();
            var colorTable = this.getColorTableName();
            var ct = "<select id=" + this.getDomId("colortable") + ">";
            for (table in Utils.ColorTables) {
                if (table == colorTable)
                    ct += "<option selected>" + table + "</option>";
                else
                    ct += "<option>" + table + "</option>";
            }
            ct += "</select>";

            tmp += HtmlUtils.formEntry("Color Bar:", ct);

            tmp += HtmlUtils.formEntry("Color By Range:", HtmlUtils.input("", this.colorByMin, ["size", "7", ATTR_ID, this.getDomId("colorbymin")]) + " - " +
				       HtmlUtils.input("", this.colorByMax, ["size", "7", ATTR_ID, this.getDomId("colorbymax")]));
            tmp += "</table>";
            menuItems.push(tmp);
        },
        initDialog: function() {
            SUPER.initDialog.call(this);
            var _this = this;
            var updateFunc = function() {
                _this.colorByMin = _this.jq("colorbymin").val();
                _this.colorByMax = _this.jq("colorbymax").val();
                _this.updateUI();

            };
            var func2 = function() {
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
            var dataList = this.getStandardData(null, {
                includeIndex: false
            });
            var allFields = this.dataCollection.getList()[0].getRecordFields();
            var fields = this.getSelectedFields([]);
            if (fields.length == 0) fields = allFields;
            var fieldCnt = 0;
            for (var fieldIdx = 0; fieldIdx < fields.length; fieldIdx++) {
                var field1 = fields[fieldIdx];
                if (!field1.isFieldNumeric() || field1.isFieldGeo()) continue;
                fieldCnt++;
            }

            var html = HtmlUtils.openTag("table", ["border", "0", "class", "display-correlation", "width", "100%"]);
            var col1Width = 10 + "%";
            var width = 90 / fieldCnt + "%";
            html += "\n<tr valign=bottom><td class=display-heading width=" + col1Width + ">&nbsp;</td>";
            var short = this.getProperty("short", fieldCnt > 8);
            var showValue = this.getProperty("showValue", !short);
            var useId = this.getProperty("useId", true);
            var useIdTop = this.getProperty("useIdTop", useId);
            var useIdSide = this.getProperty("useIdSide", useId);
            for (var fieldIdx = 0; fieldIdx < fields.length; fieldIdx++) {
                var field1 = fields[fieldIdx];
                if (!field1.isFieldNumeric() || field1.isFieldGeo()) continue;
                var label = useIdTop ? field1.getId() : field1.getLabel();
                if (short) label = "";
                html += "<td align=center width=" + width + ">" + HtmlUtils.tag("div", ["class", "display-correlation-heading-top"], label) + "</td>";
            }
            html += "</tr>\n";

            var colors = null;
            colorByMin = parseFloat(this.colorByMin);
            colorByMax = parseFloat(this.colorByMax);
            colors = this.getColorTable(true);
            for (var fieldIdx1 = 0; fieldIdx1 < fields.length; fieldIdx1++) {
                var field1 = fields[fieldIdx1];
                if (!field1.isFieldNumeric() || field1.isFieldGeo()) continue;
                var label = useIdSide ? field1.getId() : field1.getLabel();
                html += "<tr valign=center><td>" + HtmlUtils.tag("div", ["class", "display-correlation-heading-side"], label.replace(/ /g, "&nbsp;")) + "</td>";
                var rowName = field1.getLabel();
                for (var fieldIdx2 = 0; fieldIdx2 < fields.length; fieldIdx2++) {
                    var field2 = fields[fieldIdx2];
                    if (!field2.isFieldNumeric() || field2.isFieldGeo()) continue;
                    var colName = field2.getLabel();
                    var t1 = 0;
                    var t2 = 0;
                    var cnt = 0;

                    for (var rowIdx = 1; rowIdx < dataList.length; rowIdx++) {
                        var tuple = this.getDataValues(dataList[rowIdx]);
                        var v1 = tuple[field1.getIndex()];
                        var v2 = tuple[field2.getIndex()];
                        t1 += v1;
                        t2 += v2;
                        cnt++;
                    }
                    var avg1 = t1 / cnt;
                    var avg2 = t2 / cnt;
                    var sum1 = 0;
                    var sum2 = 0;
                    var sum3 = 0;
                    for (var rowIdx = 1; rowIdx < dataList.length; rowIdx++) {
                        var tuple = this.getDataValues(dataList[rowIdx]);
                        var v1 = tuple[field1.getIndex()];
                        var v2 = tuple[field2.getIndex()];
                        sum1 += (v1 - avg1) * (v2 - avg2);
                        sum2 += (v1 - avg1) * (v1 - avg1);
                        sum3 += (v2 - avg2) * (v2 - avg2);
                    }
                    r = sum1 / Math.sqrt(sum2 * sum3);

                    var style = "";
                    if (colors != null) {
                        var percent = (r - colorByMin) / (colorByMax - colorByMin);
                        var index = parseInt(percent * colors.length);
                        if (index >= colors.length) index = colors.length - 1;
                        else if (index < 0) index = 0;
                        style = "background-color:" + colors[index];
                    }
                    var value = r.toFixed(3);
                    var label = value;
                    if (!showValue || short) label = "&nbsp;";
                    html += "<td class=display-correlation-cell align=right style=\"" + style + "\">" + HtmlUtils.tag("div", ["class", "display-correlation-element", "title", "&rho;(" + rowName + "," + colName + ") = " + value], label) + "</td>";
                }
                html += "</tr>";
            }
            html += "<tr><td></td><td colspan = " + (fieldCnt + 1) + ">" + HtmlUtils.div(["id", this.getDomId(ID_BOTTOM)], "") + "</td></tr>";
            html += "</table>";
            this.setContents(html);
            this.displayColorTable(colors, ID_BOTTOM, colorByMin, colorByMax);
            this.initTooltip();
            this.displayManager.propagateEventRecordSelection(this,
							      this.dataCollection.getList()[0], {
								  index: 0
							      });

        },
    });
}








function RamaddaRecordsDisplay(displayManager, id, properties, type) {
    let SUPER = new RamaddaFieldsDisplay(displayManager, id, DISPLAY_RECORDS, properties);
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
            var records = this.filterData();
            if (!records) {
                this.setContents(this.getLoadingMessage());
                return;
            }
	    this.records = records;
	    let _this = this;
            var fields = this.getSelectedFields(this.getData().getRecordFields());
            var html = "";
            for (var rowIdx = 0; rowIdx < records.length; rowIdx++) {
		var div = "";
                var tuple = this.getDataValues(records[rowIdx]);
                for (var fieldIdx = 0; fieldIdx < fields.length; fieldIdx++) {
                    var field = fields[fieldIdx];
                    var v = tuple[field.getIndex()];
                    div += HtmlUtil.b(field.getLabel()) + ": " + v + "</br>";
                }
                html += HtmlUtils.div(["class","display-records-record","recordIndex",rowIdx], div);
            }
            var height = this.getProperty("maxHeight", "400px");
            if (!height.endsWith("px")) {
                height = height + "px";
            }
            this.setContents(HtmlUtil.div(["style", "max-height:" + height + ";overflow-y:auto;"], html));
	    this.jq(ID_DISPLAY_CONTENTS).find(".display-records-record").click(function() {
		var record = _this.records[$(this).attr("recordIndex")];
		if(record) {
		    _this.getDisplayManager().notifyEvent("handleEventRecordSelection", _this, {highlight:true,record: record});
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
    var dflt = Utils.isDefined(properties["showDefault"]) ? properties["showDefault"] : true;
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
    let SUPER = new RamaddaFieldsDisplay(displayManager, id, type || DISPLAY_STATS, properties);
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
            var tuples = this.getStandardData(null, {
                includeIndex: false
            });
            var justOne = (tuples.length == 2);

            //get the numeric fields
            var l = [];
            for (i = 0; i < fields.length; i++) {
                var field = fields[i];
                if (!justOne && (!this.showText && !field.isNumeric())) continue;
                var lbl = field.getLabel().toLowerCase();
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
        updateUI: function(reload) {
            SUPER.updateUI.call(this,reload);
            if (!this.hasData()) {
                this.setContents(this.getLoadingMessage());
                return;
            }
            var dataList = this.getStandardData(null, {
                includeIndex: false
            });
            var allFields = this.dataCollection.getList()[0].getRecordFields();
            this.allFields = allFields;
            var fields = this.getSelectedFields([]);
            var fieldMap = {};
            var stats = [];
            var justOne = (dataList.length == 2);
            for (var rowIdx = 1; rowIdx < dataList.length; rowIdx++) {
                var tuple = this.getDataValues(dataList[rowIdx]);
                if (rowIdx == 1) {
                    for (var col = 0; col < tuple.length; col++) {
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
                for (var fieldIdx = 0; fieldIdx < fields.length; fieldIdx++) {
                    var field = fields[fieldIdx];
                    var col = field.getIndex()
                    stats[col].type = field.getType();
                    var v = tuple[col];
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
                        var label = field.getLabel().toLowerCase();
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
                for (var fieldIdx = 0; fieldIdx < fields.length; fieldIdx++) {
                    var field = fields[fieldIdx];
                    var col = field.getIndex();
                    stats[col].uniqueMax = 0;
                    stats[col].uniqueValue = "";
                    for (var v in stats[col].uniqueMap) {
                        var count = stats[col].uniqueMap[v];
                        if (count > stats[col].uniqueMax) {
                            stats[col].uniqueMax = count;
                            stats[col].uniqueValue = v;
                        }
                    }
                }
            }

            if (this.showStd) {
                for (var fieldIdx = 0; fieldIdx < fields.length; fieldIdx++) {
                    var field = fields[fieldIdx];
                    var col = field.getIndex();
                    var values = stats[col].values;
                    if (values.length > 0) {
                        var average = stats[col].total / values.length;
                        var stdTotal = 0;
                        for (var i = 0; i < values.length; i++) {
                            var diff = values[i] - average;
                            stdTotal += diff * diff;
                        }
                        var mean = stdTotal / values.length;
                        stats[col].std = Math.sqrt(mean);
                    }
                }
            }
            var border = (justOne ? "0" : "1");
            var html = HtmlUtils.openTag("table", ["border", border, "bordercolor", "#ccc", "class", "display-stats", "cellspacing", "1", "cellpadding", "5"]);
            var dummy = ["&nbsp;"];
            if (!justOne) {
                header = [""];
                if (this.showCount) {
                    header.push("Count");
                    dummy.push("&nbsp;");
                }
                if (this.showMin) {
                    header.push("Min");
                    dummy.push("&nbsp;");
                }
                if (this.showPercentile) {
                    header.push("25%");
                    dummy.push("&nbsp;");
                    header.push("50%");
                    dummy.push("&nbsp;");
                    header.push("75%");
                    dummy.push("&nbsp;");
                }
                if (this.showMax) {
                    header.push("Max");
                    dummy.push("&nbsp;");
                }
                if (this.showTotal) {
                    header.push("Total");
                    dummy.push("&nbsp;");
                }
                if (this.showAverage) {
                    header.push("Average");
                    dummy.push("&nbsp;");
                }
                if (this.showStd) {
                    header.push("Std");
                    dummy.push("&nbsp;");
                }
                if (this.showUnique) {
                    header.push("# Unique");
                    dummy.push("&nbsp;");
                    header.push("Top");
                    dummy.push("&nbsp;");
                    header.push("Freq.");
                    dummy.push("&nbsp;");
                }
                if (this.showMissing) {
                    header.push("Not&nbsp;Missing");
                    dummy.push("&nbsp;");
                    header.push("Missing");
                    dummy.push("&nbsp;");
                }
                html += HtmlUtils.tr(["valign", "bottom"], HtmlUtils.tds(["class", "display-stats-header", "align", "center"], header));
            }
            var cats = [];
            var catMap = {};
	    let doValueSelection = this.getProperty("doValueSelection", false);
            for (var fieldIdx = 0; fieldIdx < fields.length; fieldIdx++) {
                var field = fields[fieldIdx];
                var col = field.getIndex();
                var field = allFields[col];
                var right = "";
                var total = "&nbsp;";
                var label = field.getLabel().toLowerCase();
                var avg = stats[col].numNotMissing == 0 ? "NA" : this.formatNumber(stats[col].total / stats[col].numNotMissing);
                //Some guess work about when to show a total
                if (label.indexOf("%") < 0 && label.indexOf("percent") < 0 && label.indexOf("median") < 0) {
                    total = this.formatNumber(stats[col].total);
                }
                if (justOne) {
                    right = HtmlUtils.tds(["xalign", "right"], [this.formatNumber(stats[col].min)]);
                    continue;
                }
                var values = [];
                if (!stats[col].isNumber && this.showText) {
                    if (this.showCount)
                        values.push(stats[col].count);
                    if (this.showMin)
                        values.push("-");
                    if (this.showPercentile) {
                        values.push("-");
                        values.push("-");
                        values.push("-");
                    }
                    if (this.showMax)
                        values.push("-");
                    values.push("-");
                    if (this.showAverage) {
                        values.push("-");
                    }
                    if (this.showStd) {
                        values.push("-");
                    }
                    if (this.showUnique) {
                        values.push(stats[col].unique);
                        values.push(stats[col].uniqueValue);
                        values.push(stats[col].uniqueMax);
                    }
                    if (this.showMissing) {
                        values.push(stats[col].numNotMissing);
                        values.push(stats[col].numMissing);
                    }
                } else {
                    if (this.showCount) {
                        values.push(stats[col].count);
                    }
                    if (this.showMin) {
			var s=this.formatNumber(stats[col].min);
                        values.push(s);
                    }
                    if (this.showPercentile) {
                        var range = stats[col].max - stats[col].min;
			var tmp =p=> {
                            var s = this.formatNumber(stats[col].min + range * p);
			    if(doValueSelection) {
				s = HtmlUtils.span(["class","display-stats-value","data-type", "percentile","data-value", p],s);
			    }
                            values.push(s);
			}
			var percs = [.25,.5,.75];
			percs.map(v=>tmp(v));
                    }
                    if (this.showMax) {
			var s=this.formatNumber(stats[col].max);
                        values.push(s);
                    }
                    if (this.showTotal) {
                        values.push(total);
                    }
                    if (this.showAverage) {
                        values.push(avg);
                    }
                    if (this.showStd) {
                        values.push(this.formatNumber(stats[col].std));
                    }
                    if (this.showUnique) {
                        values.push(stats[col].unique);
                        if (Utils.isNumber(stats[col].uniqueValue)) {
                            values.push(this.formatNumber(stats[col].uniqueValue));
                        } else {
                            values.push(stats[col].uniqueValue);
                        }
                        values.push(stats[col].uniqueMax);
                    }
                    if (this.showMissing) {
                        values.push(stats[col].numNotMissing);
                        values.push(stats[col].numMissing);
                    }

                }
                right = HtmlUtils.tds(["align", "right"], values);
                var align = (justOne ? "right" : "left");
                var label = field.getLabel();
                var toks = label.split("!!");
                var tooltip = "";
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
                label = label.replace(/ /g, "&nbsp;")
                var row = HtmlUtils.tr([], HtmlUtils.td(["align", align], field.getTypeLabel() +"&nbsp;<b>" + HtmlUtils.span(["title", tooltip], label) + "</b>") + right);
                if (justOne) {
                    html += row;
                } else {
                    html += row;
                }
            }
            html += "</table>";
            this.setContents(html);
            this.initTooltip();

	    if(doValueSelection) {
		var values = this.jq(ID_DISPLAY_CONTENTS).find(".display-stats-value");
		values.each(function() {
		    var type  = $(this).attr("data-type");
		    var value  = $(this).attr("data-value");
		    var links = "&nbsp;" + HtmlUtils.getIconImage("fa-less-than",["title","Filter other displays",
										  "class","display-stats-value-link","data-type",type,"data-value",value],
								  ["style","font-size:8pt;"]);

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
    let ID_TABLE = "table";
    let ID_HEADER = "coocheader";
    let ID_SORTBY = "sortby";
    var SUPER;
    RamaddaUtil.inherit(this, SUPER = new RamaddaDisplay(displayManager, id,
							 DISPLAY_COOCCURENCE, properties));
    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
        needsData: function() {
            return true;
        },
	getWikiEditorTags: function() {
	    return Utils.mergeLists(SUPER.getWikiEditorTags(),
				    [
					"label:Cooccurence Attributes",
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

        updateUI: function() {
	    var records = this.filterData();
	    if (!records) {
                return;
	    }  
	    let html = HtmlUtils.div(["id", this.getDomId(ID_HEADER)]) +
		HtmlUtils.div(["id", this.getDomId(ID_TABLE)]);
	    this.jq(ID_DISPLAY_CONTENTS).html(html);


	    let sourceField = this.getFieldById(null, this.getProperty("sourceField","source"));
	    let targetField = this.getFieldById(null, this.getProperty("targetField","target"));
	    let weightField = this.getFieldById(null, this.getProperty("colorBy","weight"));
	    if(weightField && this.getProperty("showSortBy",true)) {
		var enums = [["name","Name"],["weight","Weight"]];
		var header =  HtmlUtils.div(["style","margin-left:100px;"], "Sort by: " + HtmlUtils.select("",["id",this.getDomId(ID_SORTBY),],enums,this.getProperty("sortBy","")));
		this.jq(ID_HEADER).html(header);
		let _this = this;
		this.jq(ID_SORTBY).change(function() {
		    _this.setProperty("sortBy",$(this).val());
		    _this.updateUI();
		});
		
	    }

	    if(sourceField==null || targetField==null) {
		this.jq(ID_DISPLAY_CONTENTS).html("No source/target fields specified");
		return;
	    }
	    var colors = this.getColorTable();
	    if(!colors) colors = Utils.getColorTable("blues",true);
	    var colorBy = this.getColorByInfo(records,null,null,colors);


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
		var source = r.getValue(sourceField.getIndex());
		var target = r.getValue(targetField.getIndex());
		var weight = missing;
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
	    var seen = {}
	    var tmp =[];
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

	    let table = "<div style='margin-top:" + this.getProperty("topSpace","100px") +";'></div><table style='height:100%;' class='display-cooc-table' order=0 cellpadding=0 cellspacing=0  >"
	    table +="<tr valign=bottom><td style='border:none;'></td>";
	    targets.map(target=>{
		target = target.replace(/ /g,"&nbsp;").replace(/-/g,"&nbsp;");
		table += HtmlUtils.td(["style","border:none;", "width","6"],HtmlUtils.div(["class","display-cooc-colheader"], target));
	    });



	    var missingBackground  = this.getProperty("missingBackground","#eee");
	    sources.map(source=>{
		var label =  source.replace(/ /g,"&nbsp;");
		table += "<tr valign=bottom ><td style='   border:none;' align=right>" + HtmlUtils.div(["class","display-cooc-rowheader"], label) +"</td>";
		targets.map(target=>{
		    var weight = links[source+"--" + target];
		    if(!directed && !Utils.isDefined(weight))
			weight = links[target+"--" + source];
		    var style="";
		    if(weight) {
			if(weight == missing || maxWeight == 0) 
			    style = "background:#ccc;";
			else {
			    if(colorBy.index>=0) {
				color =  colorBy.getColor(weight);
				style = "background:" + color+";";
			    }
			    //			    var percent = weight/maxWeight;
			    //			    var index = parseInt(percent*colors.length);
			    // 			    if(index>=colors.length) index=colors.length-1;
			    //			    style = "background:" + colors[index]+";";
			}
		    }  else {
			style = "background:" + missingBackground +";";
		    }
		    table+=HtmlUtils.td(["title",source+" -> " + target+(weight>0?" " + weight:""), "width","3"],HtmlUtils.div(["class","display-cooc-cell","style",style+"height:100%;"],"&nbsp;"));
		});
		table+= "</tr>";
	    });

	    table+="</tr>";
	    table+="</table><div style='margin:5px;'></div>";
	    this.jq(ID_TABLE).html(table);
	    colorBy.displayColorTable();

	}
    })
}



function RamaddaBoxtableDisplay(displayManager, id, properties) {
    let ID_TABLE = "table";
    let ID_HEADER = "coocheader";
    let ID_SORTBY = "sortby";
    var SUPER;
    RamaddaUtil.inherit(this, SUPER = new RamaddaDisplay(displayManager, id,
							 DISPLAY_BOXTABLE, properties));
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
	    var records = this.filterData();
	    if (!records) {
                return;
	    }  
	    let categoryField = this.getFieldById(null, this.getProperty("categoryField","category"));
	    if(categoryField==null) {
		this.jq(ID_DISPLAY_CONTENTS).html("No category field field specified");
		return;
	    }
	    var colors = this.getColorTable();
	    if(!colors) colors = Utils.getColorTable("blues",true);
	    var colorBy = this.getColorByInfo(records,null,null,colors);
	    let catMap =  {};
	    let cats = [];
	    records.map(r=>{
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
	    let html = "<table class='display-colorboxes-table' border=0 cellpadding=5>";
	    let tableWidth=this.getProperty("tableWidth",300);


	    cats.sort((a,b)=>{
		return catMap[b].max - catMap[a].max;
	    });

	    cats.map(cat=>{
		let length = catMap[cat].list.length;
		let row = `<tr valign=top><td align=right class=display-colorboxes-header>${cat} (${length}) </td><td width=${tableWidth}>`;
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
		    row +=HtmlUtils.div(["title","","recordIndex", idx, "class","display-colorboxes-box","style","background:" + color+";"],"");
		});
		row+="</td></tr>"
		html+=row;
	    });

	    html +="</table>";
            this.displayHtml(html);
	    colorBy.displayColorTable(500);
	    if(!this.getProperty("tooltip"))
		this.setProperty("tooltip","${default}");
	    this.makeTooltips(this.jq(ID_DISPLAY_CONTENTS).find(".display-colorboxes-box"),records);
	}
    })
}



function RamaddaPercentchangeDisplay(displayManager, id, properties) {
    let SUPER =  new RamaddaFieldsDisplay(displayManager, id, DISPLAY_PERCENTCHANGE, properties);
    RamaddaUtil.inherit(this,SUPER);
    addRamaddaDisplay(this);
    $.extend(this, {
	getWikiEditorTags: function() {
	    return Utils.mergeLists(SUPER.getWikiEditorTags(),
				    [
					"label:Percent change Attributes",
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
		html += HtmlUtils.openTag("table", ["class", "stripe nowrap ramadda-table", "id", this.getDomId("percentchange")]);
		html += HtmlUtils.openTag("thead", []);
		html += "\n";
		html += HtmlUtils.tr([], HtmlUtils.th(["style","text-align:center"], this.getProperty("fieldLabel", "Field")) + HtmlUtils.th(["style","text-align:center"], label1) + HtmlUtils.th(["style","text-align:center"], label2)
				     + HtmlUtils.th(["style","text-align:center"], "Percent Change"));
		html += HtmlUtils.closeTag("thead");
		html += HtmlUtils.openTag("tbody", []);
	    }
	    var tuples= [];
	    fields.map(f=>{
		var val1 = 0;
		for(var i=0;i<records.length;i++) {
		    var val = records[i].getValue(f.getIndex());
		    if(!isNaN(val)) {
			val1 = val;
			break;
		    }
		}
		
		var val2 = 0;
		for(var i=records.length-1;i>=0;i--) {
		    var val = records[i].getValue(f.getIndex());
		    if(!isNaN(val)) {
			val2 = val;
			break;
		    }
		}

		var percent = parseInt(1000*(val2-val1)/val1)/10;
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
		    var h = template.replace("${field}", t.field.getLabel()).replace("${value1}",this.formatNumber(t.val1)).replace("${value2}",this.formatNumber(t.val2)).replace("${percent}",this.formatNumber(t.percent)).replace("${date1}",label1).replace("${date2}",label2).replace("${difference}", this.formatNumber(t.val2-t.val1));
		    
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
			    style += "background:" + posColor+";";
		    if(!isNaN(highlightPercentNegative))
			if(t.percent<highlightPercentNegative)
			    style += "background:" + negColor+";";
		    
		    html += HtmlUtils.tr(["style",style], HtmlUtils.td([], t.field.getLabel()) + 
					 HtmlUtils.td(["align","right"], this.formatNumber(t.val1)) +
					 HtmlUtils.td(["align","right"], this.formatNumber(t.val2))
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
            HtmlUtils.formatTable("#" + this.getDomId("percentchange"), {ordering:true
                //scrollY: this.getProperty("tableSummaryHeight", tableHeight)
            });
	},
    })
}



function RamaddaDatatableDisplay(displayManager, id, properties) {
    let SUPER;
    RamaddaUtil.inherit(this, SUPER = new RamaddaDisplay(displayManager, id,
							 DISPLAY_DATATABLE, properties));
    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
        needsData: function() {
            return true;
        },
	getWikiEditorTags: function() {
	    return Utils.mergeLists(SUPER.getWikiEditorTags(),
				    [
					"label:Date Table",
					'columnSelector="day|hour|dow|month|year"',
					'selectors="day,hour,dow,month,year,fieldid"',
					'columnSelector="day|hour|dow|month"',
					'showColumnSelector=false',
					'rowSelector="day|hour|dow|month"',
					'showRowSelector=false',
					'checkedIcon="fa-checked"',
					'checkedTooltipHeader="${numberChecked}"',
					'dataCheckers="match|notmatch|lessthan|greaterthan|equals|notequals(field=field,value=value,label=label,enabled=false) "', 
					'showRowTotals=false',
					'showColumnTotals=false',
					'slantHeader=true'

				    ])},
        updateUI: function() {
            this.setContents(this.getLoadingMessage());
	    var records = this.filterData();
	    if (!records) {
                return;
	    }  
	    let colors = this.getColorTable(true);
	    if (!colors) colors = Utils.getColorTable("blues",true);
	    let checkers = this.getDataFilters(this.getProperty("dataCheckers"));
	    let counts = {};
	    this.checked= {};


	    let selectors;
	    let fieldMap = {};
	    if(this.getProperty("selectors")) {
		selectors = [];
		let labels = {"day":"Day","dow":"Day of Week","hour":"Hour","month":"Month","year":"Year"};
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
		selectors  =   [["day","Day"],["dow","Day of Week"],["hour","Hour"],["month","Month"],["year","Year"]];
	    }

	    let columnSelector = this.getProperty("columnSelector",selectors[0][0]);
	    let rowSelector = this.getProperty("rowSelector",selectors[1][0]);
//	    console.log(rowSelector + " " + columnSelector);

	    let getValues =(s=>{
		let values = [];
		if(s =="dow") {
		    Utils.dayNamesShortShort.map((d,i)=>{
			values.push({id:i,label:d});
		    });
		}  else if(s =="hour") {
		    let tmp =["12&nbsp;AM","1","2","3","4","5","6","7","8","9","10","11",
			      "12&nbsp;PM","1","2","3","4","5","6","7","8","9","10","11"];
		    for(var i=0;i<24;i++)
			values.push({id:i,label:tmp[i]});
		}  else if(s =="day") {
		    for(var day=1;day<=31;day++)
			values.push({id:day,label:String(day)});
		}  else if(s =="month") {
		    Utils.monthNames.map((m,i)=>{
			values.push({id:i,label:m});
		    });
		}  else if(s =="year") {
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
			this.getColumnValues(records, field).values.map(d=>{
			    if(!Utils.isDefined(seen[d])) {
				seen[d] = true;
				values.push({id:d,label:String(d)});
			    }
			});
			values.sort((a,b) =>{
			    return a.label.localeCompare(b.label);
			});
		    }
		}
		return values;
	    });
	    let columns =getValues(columnSelector);
	    let rows =getValues(rowSelector);
	    let getId =((s,r,l)=>{
		if(s =="dow")  {
		    return l[r.getDate().getDay()].id;
		} else if(s =="hour") {
		    return l[r.getDate().getUTCHours()].id;
		} else if(s =="day") {
		    return  l[r.getDate().getUTCDate()-1].id;
		} else if(s =="month") {
		    return  l[r.getDate().getUTCMonth()].id;
		} else if(s =="year") {
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
		if(checkers && checkers.length>0) {
		    if(this.checkDataFilters(checkers, r)) {
			if(!this.checked[key]) this.checked[key] = [];
			this.checked[key].push(r);
		    }
		}
		if(!Utils.isDefined(counts[key])) {
		    counts[key]=0;
		}
		counts[key]++;
	    });


	    let min = 0;
	    let max  = 0;
	    let cnt = 0;
	    for(a in counts) {
		min = cnt==0?counts[a]:Math.min(counts[a],min);
		max = cnt==0?counts[a]:Math.max(counts[a],max);
		cnt++;
	    }

	    let colorBy = this.getColorByInfo(records,null,null,colors);
	    let showValues = this.getProperty("showValues", true);
	    let cellCount = columns.length;
	    let maxRowValue = 0;
	    let maxColumnValue = 0;
	    let columnTotals = {};
	    let rowTotals = {};
	    rows.map(row=>{
		let rowTotal = 0;
		columns.map(column=>{
		    let key = row.id +"-" +column.id;
		    if(counts[key]) {
			rowTotal+=counts[key];
		    }
		});
		rowTotals[row.id] = rowTotal;
		maxRowValue = Math.max(maxRowValue, rowTotal);
	    });
	    columns.map(column=>{
		let columnTotal = 0;
		rows.map(row=>{
		    let key = row.id +"-" +column.id;
		    if(counts[key]) {
			columnTotal+=counts[key];
		    }
		});
		columnTotals[column.id] = columnTotal;
		maxColumnValue = Math.max(maxColumnValue, columnTotal);
	    });




	    let showRowTotals = this.getProperty("showRowTotals",true);
	    let showColumnTotals = this.getProperty("showColumnTotals",true);
	    let width = Math.round(100/cellCount);
	    let table = "<table style='font-size:" + this.getProperty("fontSize",'8pt;') +"' class='display-colorboxes-table' border=0 cellpadding=0 cellspacing=0  width=100%>";
	    table+="<tr valign=bottom><td></td>";
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
		    label = label.replace(/ /g,"&nbsp;").replace("-","&nbsp;");
		    label = HtmlUtils.div(["tootltip",column.label,"class","display-datatable-header-slant"],label);
		}		    
		table+=`<td class=display-datatable-header align=center>${label}</td>`;
	    });
	    table+="</tr>";

	    rows.map(row=>{
		let name = HtmlUtils.div([],row.label.replace(/ /g,"&nbsp;"));
		table+="<tr>" + HtmlUtils.td(["class","display-datatable-name","align","right", "width","100"],name);
		columns.map(column=>{
		    let key = row.id+"-" +column.id;		    
		    let inner = "&nbsp;";
		    let style = "";
		    let marker = "";
		    if(counts[key]) {
			if(showValues) 
			    inner = counts[key]
			if(this.checked[key]) {
			    inner= HtmlUtils.getIconImage(this.getProperty("checkedIcon","fa-check"),["title","","data-key",key,"class","display-datatable-checked"]) +" " + inner;
			}
                        var percent = (counts[key] - min) / (max - min);
                        var ctIndex = parseInt(percent * colors.length);
                        if (ctIndex >= colors.length) ctIndex = colors.length - 1;
                        else if (ctIndex < 0) ctIndex = 0;
                        style = "background-color:" + colors[ctIndex] + ";";
		    }
		    let cell = HtmlUtils.div(["class","display-datatable-value"],inner);
		    table += "<td class=display-datatable-cell align=right style='" +style +"' width='" + width +"%'>" + cell+"</td>";
		});
		if(showRowTotals) {
		    let total = rowTotals[row.id];
		    let dim = Math.round(total/maxRowValue*100);
		    let bar = HtmlUtils.div(["class", "display-datatable-summary-row","style","width:"+ dim+"px;"],total);
		    table += HtmlUtils.td(["width",100,"valign","top"],bar);
		}
		table += "</tr>";
	    });
	    if(showColumnTotals) {
		table+="<tr valign=top><td></td>";
		columns.map(column=>{
		    let total = columnTotals[column.id];
		    let dim = Math.round(total/maxColumnValue*100);
		    let bar = HtmlUtils.div(["class", "display-datatable-summary-column","style","height:"+ dim+"px;"],total);
		    table += HtmlUtils.td([],bar);

		});
	    }
	    table+="</tr>";
	    table+="<tr><td></td>";
	    table+=`<td colspan=${cellCount} class=display-datatable-footer align=center id='` + this.getDomId("ct")+"'></td>";
	    table+="</tr>";
	    table +="</table>";

	    if(topSpace>0) {
		table  = HtmlUtils.div(["style","margin-top:" + topSpace+"px;"], table);
	    }

	    let html ="";
	    let header = "<table width=100%><tr>";
	    if(this.getProperty("showRowSelector",true)) {
		header+=  HtmlUtils.td(["class","display-datatable-selector","width","10%"],HtmlUtils.select("",["id",this.getDomId("rowSelector")],
													     selectors,
													     rowSelector));
	    }
	    if(this.getProperty("showColumnSelector",true)) {
		header+=  HtmlUtils.td(["class","display-datatable-selector","width","90%","align","center"],  HtmlUtils.select("",["id",this.getDomId("columnSelector")],
																selectors,
																columnSelector));
	    }
	    header+="</tr></table>";
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

	    this.jq(ID_DISPLAY_CONTENTS).find(".display-datatable-checked").tooltip({
		content: function() {
		    var key = $(this).attr("data-key");
		    var checked = _this.checked[key];
		    if(checked) {
			let tooltip = _this.getProperty("tooltip","${default}");
			if(tooltip =="") return null;
			let tt = _this.getProperty("checkedTooltipHeader","<b>#Items: ${numberChecked}</b><br>");
			tt = tt.replace("${numberChecked}", checked.length);
			_this.checked[key].map(r=>{
			    if(tt!="") tt +="<div class=ramadda-hline>";
			    tt+= _this.getRecordHtml(r,null,tooltip);
			});
			return HtmlUtils.div(["class", "display-datatable-tooltip"],tt);
		    }
		    return null;

		},
	    });
	    this.displayColorTable(colors, "ct", min,max,{});
	},
    })
}



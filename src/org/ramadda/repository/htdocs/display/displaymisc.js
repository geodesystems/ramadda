/*
  Copyright 2008-2019 Geode Systems LLC
*/


var DISPLAY_GRAPH = "graph";
var DISPLAY_TREE = "tree";
var DISPLAY_ORGCHART = "orgchart";
var DISPLAY_TIMELINE = "timeline";
var DISPLAY_BLANK = "blank";


addGlobalDisplayType({
    type: DISPLAY_GRAPH,
    label: "Graph",
    requiresData: true,
    forUser: true,
    category: "Misc"
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
            var records = this.filterData();
            if (!records) {
                return;
            }  
	    let html = HtmlUtils.div(["id", this.getDomId(ID_GRAPH)]);
	    this.jq(ID_DISPLAY_CONTENTS).html(html);
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
//	    links = [];

	    const graphData = {
		nodes: nodes,
		links: links
	    };

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
	    graph.linkCanvasObject((link, ctx) => {
		if(linkDash>0)
		    ctx.setLineDash([linkDash, linkDash]);
		ctx.lineWidth = linkWidth;
		ctx.strokeStyle = linkColor;
				       ctx.moveTo(link.source.x, link.source.y);
		ctx.lineTo(link.target.x, link.target.y);
		(link === graphData.links[graphData.links.length - 1]) && ctx.stroke();
	    });
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

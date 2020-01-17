/*
  Copyright 2008-2019 Geode Systems LLC
*/


var DISPLAY_GRAPH = "graph";



addGlobalDisplayType({
    type: DISPLAY_GRAPH,
    label: "Graph",
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
	cnt:0,
	callbackWaiting:false,
	getWikiEditorTags: function() {
	    return Utils.mergeLists(SUPER.getWikiEditorTags(),
				    [
					"label:Graph Attributes",
					'sourceField=""',
					'targetField=""',
					'nodeBackground="#ccc"',
					'linkColor="red"',
					'linkWidth="3"',
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
			nodes.push({id:source});
		    }
		    if(!seenNodes[target]) {
			seenNodes[target] = true;
			nodes.push({id:target});
		    }
		    links.push({source:source, target: target});
		});
	    } else {
		this.jq(ID_DISPLAY_CONTENTS).html("No source/target fields specified");
		return;
	    }

	    var graphData = {
		nodes: nodes,
		links: links
	    };

	    const nodeBackground = this.getProperty("nodeBackground",'rgba(255, 255, 255, 0.8)');
	    const elem = document.getElementById(this.getDomId(ID_GRAPH));
	    const graph = ForceGraph()(elem).graphData(graphData);
	    graph.nodeCanvasObject((node, ctx, globalScale) => {
		let label = node.label;
		if(!label) label = node.id;
		const fontSize = 12/globalScale;
		ctx.font = `${fontSize}px Sans-Serif`;
		const textWidth = ctx.measureText(label).width;
		if(node.isValue) {
		    let bckgDimensions = [textWidth, fontSize].map(n => n + fontSize * 0.2+2); 
		    ctx.lineWidth = 1;
		    ctx.strokeStyle = "#000";
		    ctx.fillStyle = "#fff";
		    ctx.fillRect(node.x - bckgDimensions[0] / 2, node.y - bckgDimensions[1] / 2, ...bckgDimensions);
		    ctx.strokeRect(node.x - bckgDimensions[0] / 2, node.y - bckgDimensions[1] / 2, ...bckgDimensions);
		} else  {
		    let bckgDimensions = [textWidth, fontSize].map(n => n + fontSize * 0.2); 
		    ctx.fillStyle = nodeBackground;
		    ctx.fillRect(node.x - bckgDimensions[0] / 2, node.y - bckgDimensions[1] / 2, ...bckgDimensions);
		}
		ctx.textAlign = 'center';
		ctx.textBaseline = 'middle';
		ctx.fillStyle = "black";
		ctx.fillText(label, node.x, node.y);
	    });

/*
	    graph.linkCanvasObjectMode('highlight');
	    let xcnt =  0 ;
	    graph.linkCanvasObject((link, ctx) => {
		if(xcnt++<5) console.log("in");
		ctx.setLineDash([5, 5]);
		ctx.lineWidth = 4;
		ctx.strokeStyle = 'red';
		ctx.moveTo(link.source.x, link.source.y);
		ctx.lineTo(link.target.x, link.target.y);
		ctx.stroke();
	    });
*/
//	    graph.linkAutoColorBy(d => gData.nodes[d.source].group);
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

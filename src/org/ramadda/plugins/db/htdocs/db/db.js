
var DB =  {
    applyMapSearch:function(map, formId) {
	let bounds = map.getBounds();
	let north = $("#"+formId).find("input[data-dir='north']");
	let west = $("#"+formId).find("input[data-dir='west']");	
	let east = $("#"+formId).find("input[data-dir='east']");
	let south = $("#"+formId).find("input[data-dir='south']");

	north.val(bounds.top);
	west.val(bounds.left);
	south.val(bounds.bottom);
	east.val(bounds.right);	
	let submit = $("#"+formId).find("input[name='db.search']");
	submit.click();
//	$("#"+formId).submit();
    },

    doDbSelect: function(forSearch, value) {
	if(!window.opener) {
	    alert("No source window");
	    return;
	}
	let [sourceName, sourceColumn, widgetId, otherCol]= forSearch.split(";");
	let widget = 	window.opener.document.getElementById(widgetId);
	if(!widget) {
	    alert("Unable to find widget in source window:" + sourceName +" id:" + widgetId);
	    return;
	}
	let v = widget.value;
	if(widget.type=="select-one") {
	    let select = $(widget);
	    select.val(value);
	} else if(widget.type=="text") {
	    widget.value = value;
	} else {
	    let v=widget.value||"";
	    v = v.trim();
	    v = v.split("\n").map(v=>{return v.trim()});
	    if(v.includes(value)) {
		return
	    }
	    v.push(value);
	    v = Utils.join(v,"\n");
	    widget.value = v;
	}
    },

    doDbSearch: function(sourceName,column,widgetId,otherTable,otherColumn) {
	console.log("c:" + column +" " + otherTable +" other column:" + otherColumn);
	let url = HtmlUtils.getUrl("/db/search/list",["sourceName",sourceName,"type", otherTable,"widgetId",widgetId,"column",column,"otherColumn",otherColumn]);
	window.open(url);
    },

    rowOver:function(rowId) {
	//    $("#"+ rowId).css("background-color",  "#edf5ff");
    },
    rowOut:function(rowId) {
	//    $("#"+ rowId).css("background-color",  "#fff");
    },
    toggleAllInit:function() {
	$("input[name='showtoggleall']").click(function(){
            var value  = $(this). prop("checked") == true;
            $("input[name^='show_']").prop("checked", value);
        });
    },

    rowClick:function(event, divId, popupId, url) {
	row = GuiUtils.getDomObject(divId);
	if(!row) {
            return;
	}
	GuiUtils.loadXML( url, DB.handleXml,{divId:divId,popupId:popupId});
    },
    addUrlShowingForm:function(args) {
	var embed = "{{db entry=\""+ args.entryId +"\" ";
	var attrs = "";
	for(i in args.itemValuePairs) {
            var tuple = args.itemValuePairs[i];
            var item = tuple.item;
            var value = tuple.value;
            if(item.type == "hidden") continue;
            //        if(item.name == "db.search"  || item.name == "Boxes" || item.name == "group_by" || item.name.match("group_agg.*") ) {
            if(item.name == "db.search"  || item.name == "Boxes") {
		continue;
            }
            value  = value.replace(/\n/g,"_nl_");
            if(attrs!="") attrs+=",";
            attrs+=item.name+":" + value;
	}
	embed+=" args=\"" + attrs +"\" ";
	embed+=" }}";
	embed = embed.replace(/\"/g,"&quot;");
	var html = "<div style=\"display:inline-block;width:16px;\"></div> <input id=dbwikiembed size=80 value=\"" +embed +"\"/>";
	return HtmlUtil.div(["class","ramadda-form-url"],  html);

    },

    hidePopup:function(popupId) {
	$("#" +popupId).hide();
    },

    handleDummy:function(event) {
	console.log("handleDummy");
	if(event.preventDefault) {
            event.preventDefault();
	} else {
	    event.returnValue = false;
            return false;
	}
    },

    handleXml:function(request,args) {
	var divId = args.divId;
	var popupId = args.popupId;
	var src = $("#" + divId);
	var popup = $("#" +popupId);
	var xmlDoc=request.responseXML.documentElement;
	text = getChildText(xmlDoc);
	var call = "DB.hidePopup(\'" + popupId +"\');";
	popup.html("<div><table width=100%><tr valign=top><td>" +
		   HtmlUtils.getIconImage(icon_close,["onmousedown", call,"id","tooltipclose"]) +"</td><td>" + text+"</td></table></div>");

	popup.show();
	popup.position({
            of: src,
            my: "left top",
            at: "left bottom",
            collision: "none none"
        });
	popup.show();

    },

    stickyDragEnd:function(id, url) {
	div  = GuiUtils.getDomObject(id);
	if(!div) return;
	url = url +"&posx=" + div.style.left +"&posy=" + div.style.top;
	GuiUtils.loadXML( url, stickyNOOP,id);
    },


    stickyNOOP:function(request,divId) {
    }
}


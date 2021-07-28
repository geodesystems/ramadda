
var DB =  {
    doDbSearch: function(columnName,widgetId,dbTable,dbColumn) {
	console.log("c:" + columnName +" " + dbTable);
	let url = HtmlUtils.getUrl("/db/search/list",["type", dbTable,"column",dbColumn,"widgetid",widgetId,"columnname",columnName]);
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


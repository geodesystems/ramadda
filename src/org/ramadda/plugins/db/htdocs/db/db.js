
function dbRowOver(rowId) {
    $("#"+ rowId).css("background-color",  "#edf5ff");
}

function dbRowOut(rowId) {
    $("#"+ rowId).css("background-color",  "#fff");
}

function dbRowClick(event, divId, popupId, url) {
    row = GuiUtils.getDomObject(divId);
    if(!row) {
        return;
    }
    GuiUtils.loadXML( url, dbHandleXml,{divId:divId,popupId:popupId});
}


function  dbAddUrlShowingForm(args) {
    var html = "<b>Wiki Embed:</b> {{db entry=\""+ args.entryId +"\" ";
    var attrs = "";
    for(i in args.itemValuePairs) {
        var tuple = args.itemValuePairs[i];
        var item = tuple.item;
        var value = tuple.value;
        if(item.type == "hidden") continue;
        if(item.name == "db.search"  || item.name == "Boxes" || item.name == "group_by" || item.name.match("group_agg.*") ) {
            continue;
        }
        if(attrs!="") attrs+=",";
        attrs+=item.name+":" + value;
    }
    html+=" args=\"" + attrs +"\" ";
    html+=" }}";
    return HtmlUtil.div(["class","ramadda-form-url"],  html);

}

function dbHidePopup(popupId) {
    $("#" +popupId).hide();
}

function dbHandleDummy(event) {
    console.log("handleDummy");
    if(event.preventDefault) {
        event.preventDefault();
    } else {
	event.returnValue = false;
        return false;
    }
}


function dbHandleXml(request,args) {
    var divId = args.divId;
    var popupId = args.popupId;
    var src = $("#" + divId);
    var popup = $("#" +popupId);
    var xmlDoc=request.responseXML.documentElement;
    text = getChildText(xmlDoc);
    var call = "dbHidePopup(\'" + popupId +"\');";
    popup.html("<div><table width=100%><tr valign=top><td><img width=\"16\" onmousedown=\"" + call +"\" id=\"tooltipclose\"  src=" + icon_close +"></td><td>" + text+"</td></table></div>");

    popup.show();
    popup.position({
            of: src,
                my: "left top",
                at: "left bottom",
                collision: "none none"
                });
    popup.show();

}


function stickyDragEnd(id, url) {
    div  = GuiUtils.getDomObject(id);
    if(!div) return;
    url = url +"&posx=" + div.style.left +"&posy=" + div.style.top;
    GuiUtils.loadXML( url, stickyNOOP,id);
}


function stickyNOOP(request,divId) {
}
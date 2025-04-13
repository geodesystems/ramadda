
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

    doDbSelectAll: function(event,forSearch, value) {
	let seen = {};
	let v = "";
	$(".db-select-link").each(function() {
	    let value = $(this).attr("select-value");
	    if(seen[value]) return;
	    v+=value+"\n";
	});
	return DB.doDbSelect(event, forSearch, v);
    },
    doDbClearAll: function(event,forSearch, value) {
	let [sourceName, sourceColumn, widgetId, otherCol]= forSearch.split(";");
	let widget = 	window.opener.document.getElementById(widgetId);
	if(!widget) {
	    alert("Unable to find widget in source window:" + sourceName +" id:" + widgetId);
	    return false;
	}

	if(widget.val)
	    widget.val("");
	widget.value = "";
	return false;
    },
    doDbSelect: function(event,forSearch, value) {
	if(event) event.stopPropagation();
	if(!window.opener) {
	    alert("No source window");
	    return false;
	}
	let [sourceName, sourceColumn, widgetId, otherCol]= forSearch.split(";");
	let widget = 	window.opener.document.getElementById(widgetId);
	if(!widget) {
	    alert("Unable to find widget in source window:" + sourceName +" id:" + widgetId);
	    return false;
	}
	value = value||"";
	value = value.trim();
	let v = widget.value;
	if(widget.type=="select-one") {
	    let select = $(widget);
	    select.val(value);
	} else if(widget.type=="text") {
	    widget.value = value;
	} else {
	    let v=widget.value||"";
	    v = v.trim();
	    let existingLines = v.split("\n").map(v=>{return v.trim()});
	    let newValue = "";
	    newValue+= Utils.join(existingLines,"\n").trim();
	    if(newValue!="") newValue+="\n";
	    value.split("\n").forEach(line=>{
		line = line.trim();
		if(line=="") return;
		if(existingLines.includes(line)) {
		    return
		}
		newValue+=line+"\n"
	    });
	    widget.value = newValue;
	}
	alert('Value has been set in search form');
	return false;
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
    toggleAllInit:function(formId) {
	if(formId) {
	    $("#"+formId).find('.ramadda-widgets-enumeration').each(function() {
		let container = $(this);
		let changeFunc = function() {
		    let widgets = container.find('select');
		    let anyBlank = false;
		    let widget;
		    if(widgets.length>0) {
			//check for multiples
			if(widgets.attr("rows")>0) return;
		    }
		    widgets.each(function() {
			widget = $(this);
			let value = widget.val();
			if(!Utils.stringDefined(value) || value=='-all-') {
			    anyBlank = true;
			}
		    });
		    if(!anyBlank && widget) {
			let html = widget.prop('outerHTML');
			html = html.replace('display:','xdisplay:');
			let newWidget = $(html).appendTo(container);
			$("<span>&nbsp;</span>").appendTo(container);			
			newWidget.change(changeFunc);
			HU.initSelect(newWidget);
			HU.makeSelectTagPopup(newWidget,{icon:true,single:true,after:true});
		    }
		}
		$(this).change(changeFunc);
	    });
	    $("#"+formId).find('.ramadda-widgets-text').each(function() {
		let container = $(this);
		let last = container.find('input').last();
		let guid = HU.getUniqueId('plus');
		let extraFieldsId = HU.getUniqueId('plus');
		container.append(HU.span(['id',extraFieldsId]));
		let extra = jqid(extraFieldsId);
		container.append(HU.span(['title','Add search field','id',guid,'class','ramadda-clickable'],HU.getIconImage('fas fa-plus',[],['style','font-size:9pt;color:#ccc;'])));
		jqid(guid).click(()=>{
		    let newWidget = last.clone();
		    newWidget.val("");
		    newWidget.appendTo(extra);
		    extra.append('&nbsp;');
		});
	    });
	}
	$("input[name='showtoggleall']").click(function(){
            var value  = $(this). prop("checked") == true;
            $("input[name^='show_']").prop("checked", value);
        });
    },

    rowClick:function(entryId, dbid) {
	let url = ramaddaBaseUrl +"/entry/show?entryid=" + entryId+"&dbid=" + dbid +"&db.entry=true&result=xml";
	GuiUtils.loadXML( url, DB.handleXml,{divId:"div_" + dbid});
    },
    initHeader:function(topId, tmpId) {
	let html = $("#" + tmpId).html();
	$("#" + topId).html(html);
	$("#" + tmpId).remove();
    },

    initTable:function(id) {
	let table =$("#" + id);
	let entryId = table.attr("entryid");
	let even = true;
	table.find("input").each(function(){
	    let id = $(this).attr("id");
	    $(this).attr("onClick","HtmlUtils.checkboxClicked(event,'dbid_selected','" + id+"')");
	});

	table.find("tr").each(function(){
	    $(this).attr("title","Click to view details");
	    $(this).attr("valign","top");	    
	    $(this).addClass(even?"ramadda-row-even" :"ramadda-row-odd");
	    $(this).addClass("dbrow");
	    even=!even;
	    let dbRowId = $(this).attr("dbrowid");
	    if(!dbRowId) return;
	    let tds=	$(this).find("td");
	    tds.click(function() {
		//Skip the checkbox
		if($(this).find("input").length>0) return;
		DB.rowClick(entryId, dbRowId);
	    });
	});
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
	var html = "<input style='margin-top:4px;' id=dbwikiembed size=80 value=\"" +embed +"\"/>";
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
	var src = $("#" + divId);
	var xmlDoc=request.responseXML.documentElement;
	let html = HU.div([STYLE,HU.css("max-height","600px","overflow-y","auto","max-width","500px","overflow-x","auto")],
			  getChildText(xmlDoc));
	HU.makeDialog({content:html,anchor:src});
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


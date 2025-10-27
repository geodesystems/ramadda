var DB =  {
    applyMapSearch:function(map, formId) {
	let bounds = map.getBounds();
	let north = jqid(formId).find("input[data-dir='north']");
	let west = jqid(formId).find("input[data-dir='west']");	
	let east = jqid(formId).find("input[data-dir='east']");
	let south = jqid(formId).find("input[data-dir='south']");

	north.val(bounds.top);
	west.val(bounds.left);
	south.val(bounds.bottom);
	east.val(bounds.right);	
	let submit = jqid(formId).find("input[name='db.search']");
	submit.click();
	//	jqid(formId).submit();
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
	let url = HU.url(RamaddaUtil.getUrl("/db/search/list"),
			 "sourceName",sourceName,
			 "type", otherTable,
			 "widgetId",widgetId,
			 "column",column,
			 "otherColumn",otherColumn);
	window.open(url);
    },

    rowOver:function(rowId) {
	//    jqid(rowId).css("background-color",  "#edf5ff");
    },
    rowOut:function(rowId) {
	//    jqid(rowId).css("background-color",  "#fff");
    },
    toggleAllInit:function(formId) {
	if(formId) {
	    jqid(formId).find('.ramadda-widgets-enumeration').each(function() {
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
			$(HU.span([],SPACE)).appendTo(container);			
			newWidget.change(changeFunc);
			HU.initSelect(newWidget);
			HU.makeSelectTagPopup(newWidget,{icon:true,single:true,after:true});
		    }
		}
		$(this).change(changeFunc);
	    });
	    HU.findClass(jqid(formId),'ramadda-widgets-text').each(function() {
		let container = $(this);
		let last = container.find(TAG_INPUT).last();
		let guid = HU.getUniqueId('plus');
		let extraFieldsId = HU.getUniqueId('plus');
		container.append(HU.span([ATTR_ID,extraFieldsId]));
		let extra = jqid(extraFieldsId);
		container.append(HU.span([ATTR_TITLE,'Add search field',
					  ATTR_ID,guid,
					  ATTR_CLASS,'ramadda-clickable'],
					 HU.getIconImage('fas fa-plus',[],
							 [ATTR_STYLE,
							  HU.css(CSS_FONT_SIZE,HU.pt(9),
								 CSS_COLOR,'#ccc')])));
		jqid(guid).click(()=>{
		    let newWidget = last.clone();
		    newWidget.val("");
		    newWidget.appendTo(extra);
		    extra.append(SPACE);
		});
	    });
	}
	$("input[name='showtoggleall']").click(function(){
            let value  = $(this). prop("checked") == true;
            $("input[name^='show_']").prop("checked", value);
        });
    },

    rowClick:function(entryId, dbid) {
	let url = HU.url(RamaddaUtil.getUrl("/entry/show"),
			 ARG_ENTRYID,entryId,
			 "dbid",dbid,"db.entry",true,
			 "result","xml");
	GuiUtils.loadXML( url, DB.handleXml,{divId:"div_" + dbid});
    },
    initHeader:function(topId, tmpId) {
	let html = jqid(tmpId).html();
	jqid(topId).html(html);
	jqid(tmpId).remove();
    },

    initTable:function(id) {
	let table =jqid(id);
	let entryId = table.attr("entryid");
	let even = true;
	table.find(TAG_INPUT).each(function(){
	    let id = $(this).attr(ATTR_ID);
	    $(this).attr("onClick","HtmlUtils.checkboxClicked(event,'dbid_selected','" + id+"')");
	});

	table.find(TAG_TR).each(function(){
	    $(this).attr(ATTR_TITLE,"Click to view details");
	    $(this).attr(ATTR_VALIGN,ALIGN_TOP);	    
	    $(this).addClass(even?"ramadda-row-even" :"ramadda-row-odd");
	    $(this).addClass("dbrow");
	    even=!even;
	    let dbRowId = $(this).attr("dbrowid");
	    if(!dbRowId) return;
	    let tds=	$(this).find(TAG_TD);
	    tds.click(function() {
		//Skip the checkbox
		if($(this).find(TAG_INPUT).length>0) return;
		DB.rowClick(entryId, dbRowId);
	    });
	});
    },
    addUrlShowingForm:function(args) {
	let embed = "{{db entry=\""+ args.entryId +"\" ";
	let attrs = "";
	for(i in args.itemValuePairs) {
            let tuple = args.itemValuePairs[i];
            let item = tuple.item;
            let value = tuple.value;
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
	let html = HU.tag(TAG_INPUT,
			  [ATTR_STYLE,HU.css(CSS_MARGIN_TOP,HU.px(4)),
			   ATTR_ID,'dbwikiembed',
			   ATTR_SIZE,80,
			   ATTR_VALUE,embed]);
	return HU.div([ATTR_CLASS,"ramadda-form-url"],  html);

    },

    hidePopup:function(popupId) {
	jqid(popupId).hide();
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
	let divId = args.divId;
	let src = jqid(divId);
	let xmlDoc=request.responseXML.documentElement;
	let html = HU.div([ATTR_STYLE,
			   HU.css(CSS_MAX_HEIGHT,HU.px(600),
				  CSS_OVERFLOW_Y,OVERFLOW_AUTO,
				  CSS_MAX_WIDTH,HU.px(500),
				  CSS_OVERFLOW_X,OVERFLOW_AUTO)],
			  getChildText(xmlDoc));
	HU.makeDialog({content:html,anchor:src});
    },

    stickyDragEnd:function(id, url) {
	div  = GuiUtils.getDomObject(id);
	if(!div) return;
	url = HU.url(url,"posx",div.style.left, "posy", div.style.top);
	GuiUtils.loadXML( url, stickyNOOP,id);
    },

    stickyNOOP:function(request,divId) {
    }
}


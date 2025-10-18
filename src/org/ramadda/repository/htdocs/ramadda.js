/**
   Copyright (c) 2008-2025 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/




var RamaddaUtils;
var RamaddaUtil;
var Ramadda = RamaddaUtils = RamaddaUtil  = {
    entryListeners:[],
    addEntryListener:function(listener) {
	this.entryListeners.push(listener);
    },

    handleEntrySelect:function(entryId,source) {
	this.entryListeners.forEach(listener=>{
	    listener(entryId,source);
	});
    },

    contents:{},
    currentRamaddaBase:null,

    currentSelector:null,
    captureScreen:function(entryId) {
	if(!confirm("Do you want to create a thumbnail image of the screen?")) return;
	let js = RamaddaUtil.getCdnUrl("/lib/html2canvas.min.js");
	let capture =()=>{
	    RamaddaUtil.hideEntryPopup();
	    let captureElement =  document.body;
            html2canvas(captureElement,{
		userCORS:true,
		allowTaint:true
	    }).then(function(originalCanvas) {
		const borderCanvas = document.createElement('canvas');
		const borderContext = borderCanvas.getContext('2d');
		const borderWidth = 10; // Width of the border
		borderCanvas.width = originalCanvas.width + borderWidth * 2; // Add border space
		borderCanvas.height = originalCanvas.height + borderWidth * 2; // Add border space
		borderContext.drawImage(originalCanvas, 0, 0);
		borderContext.fillStyle = 'white';
		borderContext.fillRect(0, 0, borderCanvas.width, borderCanvas.height);
		borderContext.strokeStyle = '#ccc'; 
		borderContext.lineWidth = borderWidth;
		borderContext.strokeRect(0, 0, borderCanvas.width, borderCanvas.height);
		borderContext.drawImage(originalCanvas, borderWidth, borderWidth);


		let dataUrl = borderCanvas.toDataURL('image/png');
		let data = new FormData();
		data.append("addthumbnail","true");
		data.append("filename","screencapture.png");
		data.append("file", dataUrl);
		data.append(ARG_ENTRYID,entryId);
		let dialog;
		let url = RamaddaUtil.getUrl("/entry/addfile");
		$.ajax({
		    url: url,
		    cache: false,
		    contentType: false,
		    processData: false,
		    method: 'POST',
		    type: 'POST', 
		    data: data,
		    success:  (data) =>{
			if(data.status!='ok') {
			    alert("An error occurred creating entry: "  + data.message);
			    return;
			}
			let html  = data.message+HU.br()+HU.image(data.imageurl,[ATTR_WIDTH,HU.px(600)]);
			let dialog =  HU.makeDialog({content:html,my:"left top",at:"left top",
						     title:'',anchor:$('body'),
						     draggable:true,header:true,inPlace:false,stick:true});
			//			alert(data.message);
		    },
		    error: function (err) {
			alert("An error occurred creating entry: "  + err);
		    }
		});
            });
	};
	Utils.importJS(js,capture);
    },

    initToggleTable:function(container) {
	$(container).find('.entry-arrow').click(function() {
	    let url = $(this).attr('data-url');
	    if(!url) return;
	    let title = $(this).attr('data-title')??'';
	    let handler = request => {
		if (request.responseXML == null) return;
		let xmlDoc = request.responseXML.documentElement;
		let script;
		let html;
		for (i = 0; i < xmlDoc.childNodes.length; i++) {
		    let childNode = xmlDoc.childNodes[i];
		    if (childNode.tagName == "javascript") {
			script = getChildText(childNode);
		    } else if (childNode.tagName == "content") {
			html = getChildText(childNode);
		    } else {}
		}
		if (!html) {
		    html = getChildText(xmlDoc);
		}
		if (html) {
		    html = HU.div([ATTR_STYLE,HU.css(CSS_MARGIN,HU.px(5),
						     CSS_MAX_HEIGHT,HU.px(300),
						     CSS_OVERFLOW_Y,OVERFLOW_AUTO)], html);
		    let dialog =  HU.makeDialog({content:html,my:"left top",at:"left bottom",
						 title:title,anchor:this,draggable:true,header:true,inPlace:false,stick:true});

		    Utils.checkTabs(html);
		}
		if (script) {
		    eval(script);
		}
	    }
            GuiUtils.loadXML(url, handler);
	});
    },
    
    handleEntryCreated: function(selectorId,entryId,entryName) {
	if(!window.opener) {
	    console.log('RamaddaUtils.handleEntryCreated: no opener');
	    return;
	}
	let entryInput =    window.opener.document.getElementById(selectorId);
	let hiddenInput =    window.opener.document.getElementById(selectorId+'_hidden');
	if(!entryInput || !hiddenInput) {
	    console.log('RamaddaUtils.handleEntryCreated: could not find entry widget:' + selectorId);
	    return;
	}
	if(entryInput.val)  entryInput.val(entryName);
	else entryInput.value = entryName;
	if(hiddenInput.val)  hiddenInput.val(entryId);
	else hiddenInput.value = entryId
	if(entryInput.title) {
	    setTimeout(()=>{
		alert('Entry ID has been set for ' + entryInput.title)
	    },1000);
	}
	
    },
    selectInitialClick:function(event, selectorId, elementId, allEntries, selecttype, localeId, entryType,baseUrl,props) {
	if(RamaddaUtils.currentSelector) {
	    RamaddaUtils.currentSelector.cancel();
	}
	RamaddaUtils.currentSelector = RamaddaUtils.selectCreate(event, selectorId, elementId, allEntries, selecttype, localeId, entryType,baseUrl,props);
	//	setTimeout(()=>{RamaddaUtils.handleEntryCreated(selectorId);},2000);
	return false;
    },
    selectCreate:function(event, selectorId, elementId, allEntries, selecttype, localeId, entryType, baseUrl,props) {
	let key = selectorId + (baseUrl??"");
	if (true || !selectors[key]) {
            return selectors[selectorId] = selectors[key] = new Selector(event, selectorId, elementId, allEntries, selecttype, localeId, entryType,baseUrl,props);
	} else {
            return selectors[key].handleClick(event);
	}
    },


    clearSelect:function(id) {
	selector = selectors[id];
	if (selector) {
            selector.clearInput();
	} else {
            //In case the user never clicked select
            let textComp = GuiUtils.getDomObject(id);
            let hiddenComp = GuiUtils.getDomObject(id + "_hidden");
            if (hiddenComp) {
		hiddenComp.obj.value = ""
            }
            if (textComp) {
		textComp.obj.value = ""
            }
	}
    },

    viewSelect:function(id) {
        let hiddenComp = GuiUtils.getDomObject(id + "_hidden");
        if (hiddenComp) {
	    let entryId = hiddenComp.obj.value;
	    if(Utils.stringDefined(entryId)) {
		let url = RamaddaUtils.getEntryUrl(entryId);
		window.open(url,'_blank');
	    }
	}
    },


    selectorRamaddas:{},
    initEntryPopup:function(id,target,entryType,showTypeSelector) {
	let getId=(suffix) =>{
	    return id+suffix;
	}
        let input = HU.input("","",[ATTR_ID,getId("_input"),
				    ATTR_CLASS,"input",
				    ATTR_PLACEHOLDER,"Search",
				    ATTR_STYLE, HU.css(CSS_WIDTH,HU.px(250))]);

	input+=SPACE+HU.span([ATTR_CLASS,CLASS_CLICKABLE,
			      ATTR_TITLE,'Submit search',
			      ATTR_ID,getId('button')],HU.getIconImage("fas fa-magnifying-glass"));
	let addTypesSelector = !Utils.stringDefined(entryType);
	if(!addTypesSelector && showTypeSelector) addTypesSelector = true;
	//If no entry types then get the list of types
	if(addTypesSelector) {
	    input = input+HU.div([ATTR_ID,getId('types')]);
	}
        let html = input +HU.div([ATTR_CLASS,"ramadda-select-search-results",
				  ATTR_ID,id+"_results"]);
        jqid(id).html(html);
	if(addTypesSelector) {
	    let addTypes = (types)=>{
		let options=[];
		let category = '';
		let cats = [];
		let catMap = {}
		types.forEach(type=>{
		    if(type.getCategory()!=category) {
			category = type.getCategory();
		    }
		    let list = catMap[category]
		    if(!list) {
			cats.push(category);
			catMap[category] = list= [];
		    }
		    let item =  {
			value:type.id,label:type.label,
			corpus:type.getSuperCategory()+ ' - '+ type.getCategory()
		    };
		    list.push(item);
		});
		cats.forEach(cat=>{
		    options.push({category:cat});
		    options= Utils.mergeLists(options,catMap[cat]);
		});
		options.unshift({value:'',label:'Select entry type'});
		let typeSelectId = getId('types_select');
		let select= HU.select('',[ATTR_STYLE,
					  HU.css(CSS_MAX_WIDTH,HU.px(250),CSS_MARGIN_TOP,HU.px(4)),
					  ATTR_ID,typeSelectId],options);
		select = HU.vspace()+select;
		jqid(getId('types')).html(select);
		HU.makeSelectTagPopup('#'+typeSelectId,{after:true,icon:true,single:true,showCategories:true});
		HU.initSelect('#'+typeSelectId);
	    };
	    let key = entryType??'';
	    let selectorRamadda=this.selectorRamaddas[key];
	    if(!selectorRamadda) {
		selectorRamadda =  this.selectorRamaddas[key] = getGlobalRamadda(true);
	    }
            let types =   selectorRamadda.getEntryTypes((ramadda, types) =>{
		addTypes(types);
	    },entryType);
	    if(types) {
		addTypes(types);
	    }
	}

        let inputWidget = jqid(getId("_input"));
	let doSearch = ()=>{
            let value =  inputWidget.val()??'';
            let searchLink =  HU.url(RamaddaUtil.getUrl("/search/do"),
				     [ARG_ORDERBY,'createdate',
				      ARG_ASCENDING,'false',
				      'text',value,
				      'output','json']);
	    let theType = entryType;
	    if(addTypesSelector) {
		let type = jqid(getId('types_select')).val();
		if(Utils.stringDefined(type)&& type!='any') {
		    theType = type;
		}
	    }
	    if(Utils.stringDefined(theType)) searchLink=HU.url(searchLink,["type",theType]);
            results.html(HU.getIconImage(icon_wait) + " Searching...");
            results.show();
            let myCallback = {
                entryListChanged: function(list) {
                    let entries = list.getEntries();
                    if(entries.length==0) {
                        results.show();
                        results.html("Nothing found");
                        return;
                    }
                    let html = "";

                    entries.forEach((entry,idx)=>{
			let title = 'Type: '+entry.getTypeName()+ HU.BR_ENTITY +
			    'Parent: '+ entry.getParentName();
                        html += HU.div([ATTR_INDEX,idx,
					ATTR_TITLE,title,
					ATTR_CLASS,HU.classes(CLASS_CLICKABLE,'ramadda-entry')],
				       entry.getIconImage() +" " + entry.getName());
                    });
                    results.html(html);
		    results.find(HU.dotClass('ramadda-entry')).click(function() {
			let entry = entries[$(this).attr(ATTR_INDEX)];
			if(!entry) return;
                        RamaddaUtils.selectClick(target, entry.getId(),entry.getName(),{
			    entry:entry,
			    entryName: entry.getName(),
			    isImage:entry.getIsImage(),
			    thumbnailUrl:entry.getThumbnail(),
			    icon:entry.getIconUrl(),
			    entryType:entry.getType().id,
			    north:entry.getNorth(),
			    west:entry.getWest(),
			    south:entry.getSouth(),
			    east:entry.getEast()
			});
		    });
		    
                    results.show(400);
                },
                handleSearchError:function(url, error) {
                    results.html("An error occurred:" + error);
                }
            };
            let entryList = new EntryList(getGlobalRamadda(), searchLink, myCallback, false);
            entryList.doSearch();
	}

	jqid(getId('button')).click(doSearch);
        let results =jqid(id +"_results");
        inputWidget.keyup(function(event){
            let value =  $(this).val();
            if(!Utils.isReturnKey(event) && value=="") {
                results.hide();
                results.html("");
                return;
            }
            let keycode = (event.keyCode ? event.keyCode : event.which);
            if(keycode == 13) {
		doSearch();
            }
        });
    },


    selectClick:function(id, entryId, value, opts) {
	selector = selectors[id];
	let handler = getHandler(id);
	if(!handler) handler = getHandler(selector.elementId);
	if (handler) {
            handler.selectClick(selector.selecttype, id, entryId, value,opts);
            selector.cancel();
            return;
	}

	if (selector.selecttype == "wikilink") {
	    let args = {entryId: entryId,name:value};
	    if(opts) $.extend(args, opts);
            WikiUtil.insertAtCursor(selector.elementId, selector.textComp.obj,args);
	} else   if (selector.selecttype == "fieldname") {
            WikiUtil.insertAtCursor(selector.elementId, selector.textComp.obj,  value);
	} else   if (selector.selecttype == "image") {
            WikiUtil.insertAtCursor(selector.elementId, selector.textComp.obj,  "{{image entry=\"" + entryId +"\" caption=\"" + value+"\" width=400px align=center}} ");	
	} else if (selector.selecttype == "entryid") {
	    let editor = WikiUtil.getWikiEditor(selector.elementId);
	    if(editor) {
		editor.insertTags(entryId, " ", "importtype");
	    } else {
		if(selector.props && selector.props.callback) {
		    selector.props.callback(entryId,opts);
		} else {
		    WikiUtil.insertText(selector.elementId,entryId,true);
		}
	    }
	} else if (selector.selecttype == "entry:entryid") {
            WikiUtil.getWikiEditor(selector.elementId).insertTags("entry:" + entryId, " ", "importtype");
	} else {
            selector.getHiddenComponent().val(entryId);
            selector.getTextComponent().val(value);
	}
	selector.cancel();
    },


    getBaseUrl:function() {
	return ramaddaBaseUrl;
    },
    isRamaddaUrl:function(url) {
	return url.startsWith(ramaddaBaseUrl);
    },
    getUrl:function(url) {
	return RamaddaUtil.getBaseUrl()+url;
    },
    getCdnUrl:function(url) {
	return ramaddaCdn+url;
    },
    getEntryUrl:function(entryId) {
	return HU.url(RamaddaUtil.getUrl("/entry/show"),ARG_ENTRYID,entryId);
    },

    fileDrops:{
    },
    changeField:function(entryId,fieldName,value,success,errorCallback) {
	let args = {
	    entryid:entryId,
	    what:fieldName,
	    value:value,
	    response:"json"
	};
	let url = RamaddaUtil.getUrl("/entry/changefield");
        $.post(url, args, (result) => {
	    if(success) {
		success(result);
	    }
	}).fail(error=>{
	    try {
		let json = JSON.parse(error.responseText);
		if(json.error)  {
		    if(errorCallback) {
			errorCallback(json.error);
		    }  else {
			alert("Error:" + json.error);
		    }
		    return;
		} else {
		}
	    } catch(err) {
	    }
	    if(errorCallback) {
		errorCallback(error.responseText);
	    } else {
		alert("Error:" + error.responseText);
	    }
	});

    },
    doSave: function(entryId,authtoken,args, success,errorFunc) {
	args = args||{};
	args.entryid = entryId;
	args.authtoken = authtoken;
	args.response = "json";
	let url = RamaddaUtil.getUrl("/entry/change");
        $.post(url, args, (result) => {
	    if(success) {
		success(result);
	    }
	}).fail(error=>{
	    try {
		let json = JSON.parse(error.responseText);
		if(json.error)  {
		    if(error) {
			error(json.error);
		    }  else {
			alert("Error:" + json.error);
		    }
		    return;
		} else {
		}
	    } catch(err) {
	    }
	    if(errorFunc) {
		errorFunc(error.responseText);
	    } else {
		alert("Error:" + error.responseText);
	    }
	});
    },


    initEntryTable:function(id,opts,json) {
	let _this=this;
	opts = opts||{};
	let entryMap = {};
	if(Utils.isDefined(opts.details) && opts.details==false) {
	    opts.simple = true;
	}
	let simple =opts.simple;
	let dflt = !simple;
	let dfltShow = opts.columns!=null;

	let props =  {
	    actions:[],
	    showName:true,
	    showCrumbs:false,
	    showCreator:dfltShow,
	    showHeader:dflt,
	    showDownload:dfltShow,
	    showTime:dfltShow,	    
	    showDate:dfltShow||dflt,
	    showCreateDate:dfltShow||dflt,
	    showChangeDate:dfltShow,	    	    
	    showSize:dfltShow||dflt,
	    showEntryOrder:dfltShow,
	    showType:dfltShow||dflt,
	    showAttachments:false,
	    showIcon:dflt,
	    showThumbnails:dflt,
	    showArrow:dflt,	    
	    showForm:dflt,
	    formOpen:false,
	    inlineEdit:false,
	    metadataDisplay:null
	}
	$.extend(props,opts);
	this.props=props;
	let entries = json.map((j,idx)=>{
	    let entry =  new Entry(j);
	    entryMap[entry.getId()]= entry;
	    return entry;
	});
	let html = "";
	let cols = [];
	let colString = props.columns;
	if(!colString) {
	    colString ='name,entryorder,creator,date,time,createdate,download,size,type,attachments';
	    if(props.inlineEdit) {
		colString+=',editcolumns';
		props.nameWidth=400;
	    }
	}
	let colList = Utils.split(colString,',',true,true);
	let dateWidth = 130;
	let typeWidth = 100;	
	let sizeWidth  =80;
	colList.forEach(c=>{
	    if(c=='name' && props.showName)
		cols.push({id:"name",label:"Name",width:props.nameWidth});
	    else if(c=='date' && props.showDate)
		cols.push({id:"fromdate",label:"Date",width:props.fromDateWidth??props.dateWidth??dateWidth});
	    else if(c=='editcolumns') {
		cols.push({cansort:false,id:"editcolumns",label:"Edit Columns",width:300});
	    }   else if(c=='geo') {
		cols.push({id:"latitude",label:"Latitude",width:100});
		cols.push({id:"longitude",label:"Longitude",width:100});
		cols.push({id:"altitude",label:"Altitude",width:100});
	    } else if(c=='altitude') {
		cols.push({id:"altitude",label:"Altitude",width:100});		
	    }	else if(c=='latlon') {
		cols.push({id:"latitude",label:"Latitude",width:100});
		cols.push({id:"longitude",label:"Longitude",width:100});
	    }  else if(c=='createdate' && props.showCreateDate)
		cols.push({id:c,label:"Create Date",width:props.createDateWidth??props.dateWidth??dateWidth});
	    else if(c=='download' && props.showDownload)
		cols.push({id:c,label:"&nbsp;Download&nbsp;",width:100,align:ALIGN_LEFT});
	    else if(c=='time' && props.showTime)
		cols.push({id:c,label:"&nbsp;Time&nbsp;",width:100,align:ALIGN_RIGHT});	    	    
	    else if(c=='creator' && props.showCreator)
		cols.push({id:c,label:"Creator",width:props.creatorWidth??200});	    
	    else if(c=='entryorder' && props.showEntryOrder)
		cols.push({id:c,label:"Order",width:75});

	    else if(c=='changedate' && props.showChangeDate)
		cols.push({id:c,label:"Change Date",width:props.changeDateWidth??props.dateWidth??dateWidth});	    
	    else if(c=='size' && props.showSize)
		cols.push({id:c,label:"Size",width:props.sizeWidth??sizeWidth});
	    else if(c=='type' && props.showType)
		cols.push({id:c,
			   label:"Type",
			   align:ALIGN_LEFT,
			   paddingLeft:HU.px(10),
			   width:props.typeWidth??typeWidth});
	    else if(c=='attachments' && props.showAttachments)
		cols.push({id:c,label:"Attachments",
			   align:ALIGN_LEFT,
			   paddingLeft:HU.px(10),width:props.attachmentsWidth??240});
	});

	let tableWidth=props.tableWidth??'100%';
	if(props.showHeader) {
	    html+=HU.open(TAG_TABLE,[ATTR_CELLSPACING,0,
				     ATTR_CELLPADDING,0,
				     ATTR_CLASS,'entry-list-header',
				     ATTR_WIDTH,tableWidth]);
	    let hdrAttrs = [ATTR_CLASS,HU.classes('entry-list-header-column','entry-list-header-column-sortable',CLASS_CLICKABLE)];
	    let noSortHdrAttrs = [ATTR_CLASS,HU.classes('entry-list-header-column')];
	    cols.forEach((col,idx)=> {
		let attrs = noSortHdrAttrs;
		if(!Utils.isDefined(col.cansort) || col.cansort) {
		    attrs = hdrAttrs;
		}
		let width = col.width;
		if(idx==0 && props.showForm) {
		    html+=HU.td([ATTR_STYLE,HU.css(CSS_PADDING_LEFT,HU.px(3),CSS_WIDTH,HU.px(10))],
				HU.div([ATTR_STYLE,HU.css(CSS_WIDTH,HU.px(15)),
					ATTR_ID,id+'_formarrow',
					ATTR_TITLE,'Click to show entry form',
					ATTR_CLASS,CLASS_CLICKABLE],
				       HU.getIconImage("fas fa-caret-right")));
		    width-=10;
		}
		if(Utils.isDefined(col.width)) {
		    attrs = Utils.mergeLists(attrs,[ATTR_WIDTH,col.width]);
		}
		attrs.push(ATTR_STYLE,HU.css(CSS_PADDING_LEFT,col.paddingLeft??HU.px(0)))
		if(!Utils.isDefined(col.cansort) || col.cansort) {
		    attrs = Utils.mergeLists(attrs,
					     [ARG_ORDERBY,col.id=='download'?'size':col.id,
					      ATTR_TITLE,'Sort by '+ (col.id=='download'?'Size':col.label)]);

		}
		let v = col.label;
		v=HU.span([ATTR_STYLE,this.props.headerStyle??''], v);
		if(col.id==props.orderby || (props.orderby=='size' && col.id=='download')) {
		    if(Utils.isDefined(props.ascending)) {
			if(props.ascending)
			    v = HU.getIconImage('fas fa-arrow-up') + SPACE+HU.span([],v);
			else
			    v = HU.getIconImage('fas fa-arrow-down') +SPACE+HU.span([],v);
		    }
		}
		html+=HU.td(attrs,v);
	    });
	    html+=HU.close(TAG_TABLE);
	} else if(!simple) {
	    html+=HU.div([ATTR_CLASS,'entry-list-noheader']);
	}
	let innerId = Utils.getUniqueId();
	let tableId = Utils.getUniqueId();	
	let classPrefix  = simple?'entry-list-simple':'entry-list';

	let attrs = [ATTR_ID,innerId,ATTR_CLASS,classPrefix];
	if(props.showHeader && props.tableWidth) {
	    attrs.push(ATTR_STYLE,HU.css(CSS_WIDTH,props.tableWidth));
	}
	html+=HU.open(TAG_DIV,attrs);
	let formId;
	if(props.showForm) {
	    formId = HU.getUniqueId('form_');
	    html+=HU.open(TAG_FORM,[ATTR_ID,formId,ATTR_METHOD,'post',
				    ATTR_ACTION,RamaddaUtil.getUrl('/entry/getentries')]);
	    let form = HU.checkbox('',[ATTR_STYLE,HU.css(CSS_MARGIN_LEFT,HU.px(3)),
				       ATTR_TITLE,'Toggle all',
				       ATTR_ID,id+'_form_cbx'],false);
	    let actions = [['','Apply action']];
	    props.actions.forEach(action=>{
		actions.push([action.id,action.label]);
	    });
	    form+=SPACE1;
	    form+= HU.select("",[ATTR_NAME,'output',
				 ATTR_ID,id+'_form_action'],actions);
	    form+=SPACE1;
	    form+=HU.open(TAG_INPUT,[ATTR_NAME,'getselected',
				     ATTR_TYPE,'submit',
				     ATTR_VALUE,'Selected',
				     ATTR_CLASS,'submit ui-button ui-corner-all ui-widget',
				     ATTR_ID,'getselected1338','role','button']);
	    form+=SPACE1;
	    form+=HU.open(TAG_INPUT,[ATTR_NAME,'getall',
				     ATTR_TYPE,'submit',
				     ATTR_VALUE,'All',
				     ATTR_CLASS,'submit ui-button ui-corner-all ui-widget',
				     ATTR_ID,'getall1337','role','button']);
	    html+=HU.div([ATTR_CLASS,classPrefix +'-row',
			  ATTR_ID,id+'_form',
			  ATTR_STYLE,HU.css(CSS_DISPLAY,DISPLAY_NONE,
					    CSS_WIDTH,HU.perc(100))],form);
	}

	let tableAttrs=[ATTR_ID,tableId];
	if(props.maxHeight) tableAttrs.push(ATTR_STYLE,HU.css(CSS_MAX_HEIGHT,props.maxHeight,CSS_OVERFLOW_Y,OVERFLOW_AUTO));
	html+=HU.open(TAG_DIV,tableAttrs);
	html+=HU.close(TAG_DIV,TAG_TABLE,TAG_DIV);
	let main = jqid(id);
	main.html(html);
	if(formId) {
	    jqid(formId).submit(( event ) =>{
		if(!Utils.stringDefined(jqid(id+'_form_action').val())) {
		    alert('No action specified');
		    event.preventDefault();
		}
	    });
	}

	main.find(HU.dotClass('entry-list-header-column-sortable')).click(function() {

	    let orderby = $(this).attr(ARG_ORDERBY);
	    let url;
	    if(props.orderby == orderby) {
		//my orderby
		if(Utils.isDefined(props.ascending)) {
		    //if we have an ascending
		    if(props.ascending)  {
			url = HU.addToDocumentUrl(ARG_ASCENDING,!props.ascending);
		    }    else {
			url = HU.addToDocumentUrl(ARG_ASCENDING,true);
			//			url = HU.addToDocumentUrl(ARG_ASCENDING,null);
			//			url = HU.addToDocumentUrl(ARG_ORDERBY,null);
		    }
		} else {
		    //add ascending
		    url = HU.addToDocumentUrl(ARG_ORDERBY,orderby);
		    url = HU.addToDocumentUrl(ARG_ASCENDING,true);
		}
	    } else {
		url = HU.addToDocumentUrl(ARG_ASCENDING,true);	    		    
		url = HU.addToDocumentUrl(ARG_ORDERBY,orderby);
	    }

            window.location = url;
	});



	//Don't do this as it screws up the width of the menu sometimes
	//	    HU.initSelect(jqid(id+"_form_action"));

	jqid(id+'_form_cbx').click(function() {
            let on = $(this).is(':checked');
	    jqid(id).find('.entry-form-select').prop('checked',on);
	});
	
	let initFunc = (id)=>{
	    if(props.formOpen) {
		jqid(id).find('.entry-form-select').show();
	    }
	}

	let formArrow = jqid(id+'_formarrow');
	let checkForm = ()=>{
	    if(props.formOpen) {
		jqid(id+'_form').show();
		jqid(id).find('.entry-form-select').show();
		formArrow.html(HU.getIconImage("fas fa-caret-down"));
	    } else {
		jqid(id+'_form').hide();
		jqid(id).find('.entry-form-select').hide();
		formArrow.html(HU.getIconImage("fas fa-caret-right"));
	    }

	}
	jqid(id+'_formarrow').click(function() {
	    props.formOpen = !props.formOpen;
	    checkForm();
	});

	if(props.formOpen) {
	    checkForm();
	}
	RamaddaUtil.showEntryTable(tableId,props,cols,id,entryMap,initFunc,entries);	



    },

    formatMetadata:function(entry,metadataDisplay,args) {
	let opts = {
	    doBigText:true,
	    wrapInDiv:true
	}
	if(args) opts = $.extend(opts, args);
	let types = {};
	metadataDisplay.forEach(m=>{
	    entry.getMetadata().forEach(metadata=>{
		if(!m.ok(metadata)) {
		    return;
		}
		let typeInfo = types[metadata.type];
		if(!typeInfo) {
		    typeInfo = types[metadata.type]  = {
			display:m,
			contents:[]
		    }
		}
		let html = m.format(metadata).trim();
		//check for long text
		let isLong = false
		Utils.split(html," ").every(t=>{
		    if(t.length>50) {
			isLong=true;
			return false;
		    }
		    return true;
		});
		if(isLong) {
		    html = HU.div([ATTR_STYLE,HU.css(CSS_WORD_BREAK,'break-all')],html);
		}
		typeInfo.contents.push(html);
	    });
	});

	let mtd='';
	if(opts.wrapInDiv) {
	    Object.keys(types).forEach(type=>{
		let info = types[type];
		if(info.display.delimiter) {
		    mtd+=HU.open(TAG_DIV,[ATTR_CLASS,'ramadda-metadata-display']);
		    if(info.display.header) mtd+=HU.b(info.display.header+':')+' ';
		    mtd+=Utils.join(info.contents,info.display.delimiter+' ');
		    mtd+=HU.close(TAG_DIV);
		} else {
		    if(info.display.header) mtd+=HU.b(info.display.header+':')+' ';
		    info.contents.forEach(html=>{
			mtd+=HU.div([ATTR_CLASS,'ramadda-metadata-display'],html);
		    });
		}
	    });
	} else {
	    Object.keys(types).forEach(type=>{
		let info = types[type];
		mtd+=HU.open(TAG_DIV,[ATTR_CLASS,'ramadda-metadata-display']);
		if(info.display.header) mtd+=HU.b(info.display.header+':')+' ';
		mtd+=Utils.join(info.contents,'; ');
		mtd+=HU.close(TAG_DIV);
	    });
	}

	if(Utils.stringDefined(mtd) && opts.doBigText) {
	    mtd = HU.div([ATTR_CLASS,'ramadda-metadata-bigtext','bigtext-length',100], mtd);
	}
	return mtd;
    },
    makeMetadataDisplay:function(props) {
	if(!props) return null;
	let mtd=[];
	Utils.split(props,",",true,true).forEach(prop=>{
	    let toks = prop.split(":");
	    if(toks.length==0) return
	    let header;
	    let delimiter;
	    let check;
	    let type;
	    let template='{attr1} {attr2}';
	    toks.forEach((tok,idx)=>{
		if(idx==0) {
		    type=tok;
		    return
		} 
		let subtoks = tok.split('=');
		if(subtoks.length!=2) return;
		let key=subtoks[0];
		let value=subtoks[1];		
		if(key.startsWith('attr')) {
		    check=mtd=>{
			return mtd.value[key]==value;
		    }
		} else if(key=='template') {
		    template=value;
		} else if(key=='delimiter') {
		    delimiter=value;
		} else if(key=='header') {
		    header=value;		    
		}		    
	    });
	    let m ={
		prop:prop,
		type:type,
		delimiter:delimiter,
		header:header,
		template:template.replace('_colon_',':','_comma_',','),
		check:check,
		ok:function(metadata) {
		    if(metadata.type!=this.type) return false;
		    if(this.check) return this.check(metadata);
		    return true;
		},
		format:function(metadata) {
		    let html=this.template;
		    let attrs = ['attr1','attr2','attr3','attr4'];
		    attrs.forEach(attr=>{
			html=html.replace('{'+ attr+'}',metadata.value[attr]??'');
		    });
		    return html;
		}
	    }
	    mtd.push(m);
	});
	return mtd;
    },
    getToggleIcon:function(open) {
	if(!open) {
	    return HU.getIconImage('fas fa-caret-right',null,[ATTR_STYLE,this.props.toggleStyle||'']);
	} else {
	    return HU.getIconImage('fas fa-caret-down',null,[ATTR_STYLE,this.props.toggleStyle||'']);
	}
    },


    applyInlineEdit:function(inlineEdit,entryOrder) {
	if(typeof inlineEdit=='string') {
	    inlineEdit = $(inlineEdit);
	}
	let applyEdit = comp=>{
	    let entryId = comp.attr(ATTR_ENTRYID);
	    let value = comp.val().trim();
	    let what = comp.attr('data-field');
	    let url = HU.url(RamaddaUtil.getUrl("/entry/changefield"),ARG_ENTRYID, entryId,'what',what,'value',value);
	    $.getJSON(url, function(data) {
		if(data.error) {
		    alert('An error has occurred: '+data.error);
		    return;
		}
	    }).fail(data=>{
		console.dir(data);
		alert('An error occurred:' + data);
	    });

	    if (event.preventDefault) {
		event.preventDefault();
	    } else {
		event.returnValue = false;
		return false;
	    }
	    comp.css(CSS_BACKGROUND,'yellow');
	    setTimeout(()=>{
		comp.css(CSS_BACKGROUND,COLOR_WHITE);
	    },4000);

	}

	let applyAll=(comp,delta)=>{
	    let start=false;
	    let index = parseInt(comp.val());
	    let startId = comp.attr(ATTR_ENTRYID);
	    entryOrder.each(function() {
		let id =$(this).attr(ATTR_ENTRYID);
		if(!start) {
		    if(id!=startId) {
			return;
		    }
		    start = true;
		} else {
		    index = index+delta;
		}
		$(this).val(index);
		applyEdit($(this));
	    });
	};
	
	inlineEdit.each(function() {
	    let tag = $(this).prop('tagName')??'';
	    tag = tag.toLowerCase();
	    if(tag=='select') {
		$(this).change(function() {
		    applyEdit($(this));
		});
	    } else {
		$(this).keypress(function(event) {
		    if(event.which!=13) {
			return;
		    }
		    if(event.shiftKey && $(this).attr('data-field') == 'entryorder') {
			if (event.preventDefault) {
			    event.preventDefault();
			} else {
			    event.returnValue = false;
			}
			
			let delta = prompt("Do you want to reorder all of the following entries. Delta:",5);
			if(delta) {
			    applyAll($(this),parseInt(delta));
			}
			return false;
		    }
		    applyEdit($(this));
		});
	    }
	});

    },

    showEntryTable:function(id,props,cols,mainId,entryMap,initFunc,entries,secondTime) {
	let main = jqid(mainId);
	let html = '';
	let space = '';
	let rowClass  = props.simple?'entry-list-simple-row':'entry-list-row entry-list-row-data';
	let metadataDisplay = RamaddaUtil.makeMetadataDisplay(this.props.metadataDisplay);
	let hasMetadata=false;
	entries.forEach((entry,entryIdx)=>{
	    let line = '';
	    let rowId = Utils.getUniqueId('row_');
	    let innerId = Utils.getUniqueId();
	    cols.forEach((col,idx)=> {
		let last = idx==cols.length-1;
		let attrs = [ATTR_CLASS,'entry-row'];
		let v = entry.getProperty(col.id,{},props.inlineEdit)??'';
		if(typeof v=='number' && isNaN(v))
		    v ='NA'
		let _v = v;
		let title = null;
		v  = HU.span([ATTR_CLASS,this.props.textClass??'',
			      ATTR_STYLE,this.props.textStyle??''],v);
		if(col.id=='name') {
		    let icon = '';
		    if(props.showIcon) {
			icon = entry.getLink(entry.getIconImage([ATTR_WIDTH,this.props.iconWidth??ramaddaGlobals.iconWidth]));
		    }
		    if(!props.inlineEdit) {
			v = entry.getLink(v);
		    }
		    
		    if(props.showIcon)
			v =  icon + SPACE +v;
		    
		    if(metadataDisplay && metadataDisplay.length) {
			let mtd = RamaddaUtil.formatMetadata(entry,metadataDisplay);
			if(Utils.stringDefined(mtd)) {
			    v +=HU.div([ATTR_STYLE,HU.css(CSS_MAX_WIDTH,HU.perc(100),
							  CSS_OVERFLOW_WRAP,'break-word')],mtd);
			    hasMetadata=true;
			}
		    }



		    let tds = [];
		    //[cbx,space,arrow,icon,thumbnail,v]
		    let cbxId = Utils.getUniqueId('entry_');
		    if(props.showForm)
			tds.push(HU.hidden('allentry',entry.getId()) +
				 HU.checkbox(cbxId,['rowid',rowId,
						    ATTR_NAME,'selentry',
						    ATTR_VALUE, entry.getId(),
						    ATTR_CLASS,'entry-form-select',
						    ATTR_STYLE,HU.css(CSS_MARGIN_RIGHT,HU.px(2),
								      CSS_DISPLAY,DISPLAY_NONE)],false));

		    tds.push(HU.div([ATTR_STYLE,HU.css(CSS_MARGIN_RIGHT,HU.px(2),
						       CSS_MIN_WIDTH,HU.px(10)),
				     'innerid',innerId,
				     ATTR_ENTRYID,entry.getId(),
				     ATTR_TITLE,'Click to show contents',
				     ATTR_CLASS,HU.classes('entry-arrow',CLASS_CLICKABLE)], this.getToggleIcon(false)));

		    if(props.showCrumbs && entry.breadcrumbs) {
			let crumbId = Utils.getUniqueId();
			v = HU.span([ATTR_ID,'breadcrumbtoggle_' + crumbId, 'breadcrumbid',crumbId,
				     ATTR_TITLE,'Show breadcrumbs',
				     ATTR_CLASS,HU.classes(CLASS_CLICKABLE,'ramadda-breadcrumb-toggle') ],
				    HU.getIconImage('fas fa-plus-square')) +SPACE2
			    + HU.span([ATTR_STYLE,HU.css(CSS_DISPLAY,DISPLAY_NONE),
				       ATTR_ID,crumbId], entry.breadcrumbs+'&nbsp;&raquo;&nbsp;') +v;
		    }

		    if(props.showThumbnails && entry.getThumbnail()) {
			v+= '<br>'+HU.div([ATTR_CLASS,'ramadda-thumbnail',
					   ATTR_STYLE,HU.css(CSS_MAX_HEIGHT,HU.px(100),CSS_OVERFLOW_Y,OVERFLOW_AUTO)],
					  HU.image(entry.getThumbnail(),[ATTR_LOADING,'lazy',
									 ATTR_CLASS,HU.classes(CLASS_CLICKABLE,'ramadda-thumbnail-image'),
									 ATTR_TITLE,'Click to enlarge',
									 ATTR_STYLE,HU.css(CSS_WIDTH,HU.px(100)),
									 'entry-url',entry.getEntryUrl()
									]));
		    }


		    tds.push(v);
		    v =  HU.table([],HU.tr([ATTR_VALIGN,ALIGN_TOP],HU.tds([],tds)));
		} else {
		    if(col.id=='attachments') {
			v='';
			entry.getMetadata().forEach(m=>{
			    if(m.type=='content.thumbnail' || m.type=='content.attachment') {
				let imgUrl;
				let f = m.value.attr1;
				let name = f.replace(/.*_file_/,'');
				let mUrl = HU.url(RamaddaUtil.getBaseUrl()+'/metadata/view/' + f,
						  ['element','1',ATTR_ENTRYID,entry.getId(),'metadata_id', m.id]);
				
				if(m.type=='content.thumbnail' || Utils.isImage(f)) {
				    imgUrl = mUrl;
				    name+='<br>'+HU.image(imgUrl,[ATTR_WIDTH,HU.px(290)]).replace(/"/g,"'");
				} else {
				    f = f.toLowerCase();
				    let icon  = null;
				    if(f.endsWith('pdf')) icon='/icons/pdf.png';
				    else if(f.endsWith('txt')) icon='/icons/txt.png';
				    else  icon='/icons/file.png';				    				    
				    if(icon)
					imgUrl = RamaddaUtil.getBaseUrl()+icon;
				}
				if(imgUrl) {
				    v+=HU.href(mUrl,HU.image(imgUrl,[ATTR_CLASS,HU.classes('ramadda-attachment',CLASS_CLICKABLE),
								     ATTR_TITLE,name,
								     ATTR_WIDTH,HU.px(75),
								     ATTR_STYLE,HU.css(CSS_MARGIN,HU.px(2))]));
				}
			    }
			});
			if(v!='') v  =HU.div([ATTR_STYLE,HU.css(CSS_MAX_WIDTH,HU.getDimension(col.width),
								CSS_OVERFLOW_X,OVERFLOW_AUTO)], v);

		    } else  if(col.id=='type') {
			v = HU.href(RamaddaUtil.getUrl('/search/type/' + entry.getType().id),v,
				    [ATTR_TITLE,Utils.delimMsg('Search for entries of type') +' ' + _v]);
		    }
		    let maxWidth = col.width-20;
		    maxWidth = col.width;		    
		    v = HU.div([ATTR_STYLE,HU.css(CSS_PADDING_LEFT,col.paddingLeft??HU.px(5),
						  CSS_WHITE_SPACE,'nowrap',
						  CSS_WIDTH,HU.getDimension(col.width),
						  CSS_TEXT_ALIGN,col.align??ALIGN_RIGHT,
						  CSS_MAX_WIDTH,HU.getDimension(maxWidth),
						  CSS_OVERFLOW_X,OVERFLOW_HIDDEN)+(last?HU.css(CSS_PADDING_LEFT,HU.px(4)):'')],v);
		    attrs.push(ATTR_ALIGN,col.align??ALIGN_RIGHT);
		}
		if(Utils.isDefined(col.width)) {
		    attrs.push(ATTR_WIDTH,HU.getDimension(col.width));
		}
		if(title) {
		    //		    attrs.push(ATTR_TITLE,title);
		}
		//		v = HU.div([ATTR_CLASS,'entry-row-label'], v);
		line+=HU.td(attrs,v);
	    });		



	    let row =  HU.open(TAG_DIV,[ATTR_ENTRYID,entry.getId(),
					ATTR_ID,rowId]);
	    row +=  HU.open(TAG_DIV,[ATTR_ENTRYID,entry.getId(),
				     ATTR_ID,rowId,
				     ATTR_CLASS,rowClass]);
	    row+= HU.open(TAG_TABLE,[ATTR_CELLSPACING,0,
				     ATTR_CELLPADDING,'border',
				     ATTR_WIDTH,'100%',
				     ATTR_CLASS,  'entry-row-table',
				     ATTR_ENTRYID,entry.getId()]);
	    let title='';
	    let rowAttrs = [ATTR_CLASS,'entry-row',
			    ATTR_VALIGN,ALIGN_TOP];
	    if(!props.simple) {
		let img = entry.getIconImage([ATTR_WIDTH,HU.px(32)]).replace(/"/g,"'");
		//			attrs.push('data-icon',entry.getIconUrl());
		title = entry.getName();
		if(entry.remoteRepository) rowAttrs.push('remote-repository', entry.remoteRepository.name);

		let type = entry.getType();
		if(type) rowAttrs.push(ATTR_DATA_TYPE, type.name);
		let thumb =entry.getThumbnail()
		if(thumb)
		    rowAttrs.push('data-thumbnail', thumb);

		let file =entry.getFilename()
		if(file)
		    rowAttrs.push('data-filename', file);
		
		rowAttrs.push(ATTR_TITLE,title,'data-icon',entry.getIconUrl());
	    }
	    row+=HU.open(TAG_TR,rowAttrs);
	    row+=line;
	    row+=HU.close(TAG_TR,TAG_TABLE,TAG_DIV);
	    row+=HU.div([ATTR_ID,innerId,ATTR_STYLE,HU.css(CSS_MARGIN_LEFT,HU.px(20))]);
	    row+=HU.close(TAG_DIV);
	    html+=row;
	});
	html+=HU.close(TAG_DIV);

	let container = jqid(id);
	if(!secondTime && props.tableWidth) {
	    container.css(ATTR_WIDTH,props.tableWidth);
	}

	html = $(html).appendTo(container);
	Translate.translate(html);
	if(hasMetadata) {
	    html.find('.ramadda-metadata-bigtext').each(function() {
		Utils.initBigText($(this));
	    });
	}


	let inlineEdit =        main.find('.ramadda-entry-inlineedit');
	let entryOrder =        main.find('.ramadda-entry-inlineedit-entryorder');
	this.applyInlineEdit(inlineEdit,entryOrder)

	if(true ||!props.inlineEdit) {
	    html.find('.entry-row').tooltip({
		show: { effect: 'slideDown', delay: 1500, duration: 300 },
		position: { my: "right top", at: "right bottom" },
		content: function () {
		    if($(this).hasClass('ramadda-edit-input')) return null;
		    let title = $(this).attr(ATTR_TITLE);
		    let icon = $(this).attr('data-icon');		
		    let thumb = $(this).attr('data-thumbnail');		
		    if(icon) {
			icon = HU.image(icon,[ATTR_WIDTH,HU.px(32)]);
			title = icon+HU.space(1) +title;
		    }
		    title = HU.div([],HU.b(title));
		    let val = $(this).val();
		    if(val)
			title+=HU.div([],val);

		    let type = $(this).attr(ATTR_DATA_TYPE);		
		    if(type) title=title+ 'Type: ' + type+'<br>';
		    let filename = $(this).attr('data-filename');		
		    if(filename) title=title+ 'File: ' + filename+'<br>';
		    let remote = $(this).attr('remote-repository');		
		    if(remote) title=title+ 'Remote: ' + remote+'<br>';
		    title = title+
			HU.div([],'Right-click to see entry menu') +
			HU.div([],'Shift-drag to copy/move');

		    if(thumb) {
			thumb = HU.image(thumb,[ATTR_WIDTH,HU.px(250)]);
			title = title +  HU.div([ATTR_STYLE,HU.css(CSS_MAX_HEIGHT,HU.px(200),
								   CSS_OVERFLOW_Y,OVERFLOW_HIDDEN)],thumb);
		    }

		    return HU.div([ATTR_STYLE,HU.css(CSS_MARGIN,HU.px(5))],title);
		}});	    
	}
	container.find('.ramadda-breadcrumb-toggle').click(function() {
	    let id = $(this).attr('breadcrumbid');
	    let crumbs = jqid(id);
	    if(crumbs.css(CSS_DISPLAY)==DISPLAY_NONE) {
		jqid('breadcrumbtoggle_' +id).html(HU.getIconImage("fas fa-minus-square"));
		crumbs.css(CSS_DISPLAY,DISPLAY_INLINE);
	    } else {
		jqid('breadcrumbtoggle_' +id).html(HU.getIconImage("fas fa-plus-square"));
		crumbs.css(CSS_DISPLAY,DISPLAY_NONE);
	    }
	});

	let _this=this;
	container.find('.entry-arrow').click(function() {
	    let entryId = $(this).attr(ATTR_ENTRYID);
	    let innerId = $(this).attr('innerid');	    
	    let inner = jqid(innerId);
	    let filled = $(this).attr("filled");
	    let open = $(this).attr("open");
	    $(this).html(_this.getToggleIcon(!open));
	    if(filled) {
		if(open) {
		    inner.hide();
		    $(this).attr('open',false);	    
		} else {
		    inner.show();
		    $(this).attr('open',true);	    
		}
		return;
	    }
	    let entry = entryMap[entryId];
	    $(this).attr('filled',true);
	    $(this).attr('open',true);	    
	    let url = RamaddaUtil.getUrl('/entry/show?output=json&includeproperties=false&includedescription=false&includeservices=false&children=true&entryid='+entryId);
	    if(props.sortby) url=HU.url(url,[ARG_ORDERBY,props.sortby]);
	    if(props.orderby) url=HU.url(url,[ARG_ORDERBY,props.orderby]);	    
	    if(props.ascending) url=HU.url(url,[ARG_ASCENDING,props.ascending]);	    
	    if(props.sortdir) {
		url=HU.url(url,[ARG_ASCENDING,props.sortdir=='up']);
	    }
            $.getJSON(url, function(data, status, jqxhr) {
                if (GuiUtils.isJsonError(data)) {
                    return;
                }
		let entries = data.map((j,idx)=>{
		    let entry=  new Entry(j);
		    entryMap[entry.getId()]= entry;
		    return entry;
		});
		if(entries.length>0) {
		    RamaddaUtil.showEntryTable(innerId,props,cols,mainId, entryMap,initFunc,entries,true);	
		} else {
		    let table = HU.open(TAG_TABLE,[ATTR_CLASS,'formtable']);
		    if(entry.getIsUrl()) {
			table+=HU.formEntry('URL:',HU.href(entry.getResourceUrl(),entry.getResourceUrl()));
		    } else if(entry.getIsFile()) {
			let url = entry.getResourceUrl();
			table+=HU.formEntry('File:',HU.href(url,entry.getFilename()) +' ' +
					    ' (' + entry.getFormattedFilesize()+')' +
					    ' ' + HU.href(url,HU.getIconImage('fas fa-download')));
		    }
		    
		    table+=HU.formEntry(Utils.delimMsg('Kind'),
					HU.href(RamaddaUtil.getUrl('/search/type/' + entry.getType().id),entry.typeName,
						[ATTR_TITLE,Utils.delimMsg('Search for entries of type') +' ' + entry.typeName]));
		    let searchUrl = RamaddaUtil.getUrl('/search/type/' + entry.getType().id+'?user_id='+ entry.creator+'&search.submit=true');
		    let created = HU.href(searchUrl,entry.creator,
					  [ATTR_TITLE,
					   Utils.delimMsg('Search for entries of this type created by') +' ' + entry.creator]);
		    table+=HU.formEntry(Utils.msgLabel('Created by'),created);
		    table+=HU.formEntry(Utils.msgLabel('Created'),entry.createDateFormat);
		    if(entry.startDate && entry.startDate.getTime()!=entry.createDate.getTime())
			table+=HU.formEntry(Utils.msgLabel('Date'),entry.startDateFormat);
		    if(entry.startDate && entry.endDate && entry.startDate.getTime()!=entry.endDate.getTime()) {
			table+=HU.formEntry(Utils.msgLabel('To Date'),entry.endDateFormat);
		    }
		    table+=HU.close(TAG_TABLE);
		    HU.jqid(innerId).html(table);
		}
		HU.handleNewContent(jqid(innerId));
	    });
	});

	if(props.simple) return;
	container.find('.entry-form-select').click(function(event) {
            let on  = $(this).is(':checked');
	    let row = jqid($(this).attr('rowid'));
	    HU.checkboxClicked(event,'entry_',$(this).attr(ATTR_ID));
	    main.find('.entry-form-select').each(function() {
		let on  = $(this).is(':checked');
		let row = jqid($(this).attr('rowid'));
		if(on) row.addClass('entry-list-row-selected');
		else row.removeClass('entry-list-row-selected');	    
	    });
	});

	container.find('.ramadda-thumbnail-image').click(function() {
	    let src = $(this).attr(ATTR_SRC);	
	    let url = $(this).attr('entry-url');
	    let contents = HU.href(url,HU.image(src),[ATTR_TITLE,'Click to view entry']);
	    HU.makeDialog({content:contents,my:'left top',at:'left bottom',anchor:this,header:true,draggable:true});
	});

	container.find('.ramadda-attachment').tooltip({
	    show: { effect: 'slideDown', delay: 500, duration: 1000 },
	    content: function () {return $(this).prop(ATTR_TITLE);}});

	let rows = container.find('.entry-list-row');
	rows.bind ('contextmenu', function(event) {
	    let entryRow = $(this);
	    let entry = entryMap[$(this).attr(ATTR_ENTRYID)];
	    if(!entry) return;
            eventX = GuiUtils.getEventX(event);
            let url = RamaddaUtil.getUrl("/entry/show?entryid=" + entry.getId() + "&output=metadataxml");
	    let handleTooltip = function(request) {
		let xmlDoc = request.responseXML.documentElement;
		text = getChildText(xmlDoc);
		HU.makeDialog({content:text,my:'left top',at:'left bottom',title:entry.getIconImage()+" "+entry.getName(),anchor:entryRow,header:true});
	    }
            GuiUtils.loadXML(url, handleTooltip, this);
	    if (event.preventDefault) {
		event.preventDefault();
	    } else {
		event.returnValue = false;
		return false;
	    }
	});

	RamaddaUtil.initDragAndDropEntries(rows,entryMap,mainId);
	initFunc(id);


    },
    initDragAndDropOnHeader:function(entryId,authToken) {
	let success = (data, newEntryId, name,isImage)=>{
	    HU.makeOkCancelDialog($('.ramadda-header'),
				  'New entry has been created: ' +name+'<br>Do you want to view it?',
				  ()=>{
				      let url =  RamaddaUtil.getUrl('/entry/show?entryid=' +newEntryId);
				      document.location = url;
				  },
				  null,null,{okLabel:'Yes',cancelLabel:'No',at:'middle bottom',my:'middle top',
					     title:'New Entry',header:true});
	};
	//initDragAndDrop:function(target, dragOver,dragLeave,drop,type, acceptText,skipEditable)
	Utils.initDragAndDrop($('.ramadda-header'),
			      event=>{},
			      event=>{},
			      (event,item,result,wasDrop) =>{
				  Ramadda.handleDropEvent(event, item, result, entryId,authToken,success);
			      },null,true,true);
    },

    initDragAndDropEntries:function(rows, entryMap,mainId) {
	rows.mousedown(function(event) {
            if (!event.shiftKey || !entryMap) return;
	    let entry = entryMap[$(this).attr(ATTR_ENTRYID)];
	    if(!entry) return;
	    let source = $(this);
	    let entries = [entry];
	    jqid(mainId).find('.entry-list-row-selected').each(function() {
		let selectedEntry = entryMap[$(this).attr(ATTR_ENTRYID)];
		if(!selectedEntry) return;
		if(selectedEntry.getId()!=entry.getId()) {
		    entries.push(selectedEntry);
		}
	    });
	    Utils.entryDragInfo = {
		dragSource: source.attr(ATTR_ID),
		entry:entry,
		getIds: function() {
		    return entries.map(entry=>{return entry.getId();}).join(",");
		},
		hasEntry:function(entry) {
		    return entries.includes(entry);
		},
		getHtml:function() {
		    let html = "";
		    entries.forEach(entry=>{
			if(html!="") html+=HU.br();
			html+=  entry.getIconImage() + SPACE +entry.getName();
		    });
		    return html;
		},
	    }
	    if(entry) {
		Utils.mouseIsDown = true;
		if (event.preventDefault) {
		    event.preventDefault();
		} else {
		    event.returnValue = false;
		    return false;
		}
	    }});


	function isTarget(comp) {
	    return comp.hasClass('ramadda-entry-target');
	}

	rows.mouseover(function(event) {
	    let bg  = "#C6E2FF";
	    if(isTarget($(this))) {
		if (Utils.mouseIsDown && Utils.entryDragInfo) {
		    $(this).css("background", bg);
		}
		return
	    } 
	    let entry = entryMap[$(this).attr(ATTR_ENTRYID)];
	    if(!entry) return;
	    if (Utils.mouseIsDown && Utils.entryDragInfo) {
		if(Utils.entryDragInfo.hasEntry(entry)) return;
		$(this).css("background", bg);
	    }
	});

	rows.mouseout(function(event) {
	    if(Utils.entryDragInfo) {
		if(isTarget($(this))) {
		    $(this).css("background", "");
		    return
		}

		let entry = entryMap[$(this).attr(ATTR_ENTRYID)];
		if(Utils.entryDragInfo.entry == entry) return;
		$(this).css("background", "");
	    }

	});

	rows.mouseup(function(event) {
	    if(!Utils.entryDragInfo) return;
	    $(this).css("background", "");
	    if(isTarget($(this))) {
		let url =  RamaddaUtil.getUrl('/entry/getentries?output=' + $(this).attr('target-type'));
		Utils.entryDragInfo.getIds().split(',').forEach(id=>{
		    url+='&selentry=' + id;
		});
		document.location = url;
		return;
	    }

	    let entry = entryMap[$(this).attr(ATTR_ENTRYID)];
	    if(!entry) return;
	    if(Utils.entryDragInfo.hasEntry(entry)) return;
	    if(!Utils.entryDragInfo.hasEntry(entry)) {
		url = RamaddaUtil.getUrl("/entry/copy?action=action.move&from=" + Utils.entryDragInfo.getIds() + "&to=" + entry.getId());
		document.location = url;
	    }
	});
    },

    initFormTags: function(formId) {
	let form = jqid(formId);
	let inputs = form.find('.metadata-tag-input');
	form.attr('autocomplete','off');
	inputs.attr('autocomplete','off');
	inputs.keyup(function(event) {
	    HU.hidePopupObject();
	    let val = $(this).val();
	    if(!Utils.stringDefined(val)) return;
	    let url = HU.getUrl(RamaddaUtil.getUrl("/metadata/suggest"),[ATTR_VALUE,val.trim()]);
	    let input = $(this);
	    $.getJSON(url, data=>{
		if(data.length==0) return;
		let suggest = "";
		data.forEach(d=>{
		    suggest+=HU.div([ATTR_CLASS,HU.classes(CLASS_CLICKABLE,'metadata-suggest'),
				     "suggest",d],d);
		});
		let html = HU.div([ATTR_CLASS,"ramadda-search-popup",
				   ATTR_STYLE,HU.css(CSS_MAX_WIDTH,HU.px(200),
						     CSS_PADDING,HU.px(4))],suggest);
		let dialog = HU.makeDialog({content:html,my:"left top",at:"left bottom",anchor:input});
		dialog.find(".metadata-suggest").click(function() {
		    HU.hidePopupObject();
		    input.val($(this).attr("suggest"));
		});
	    }).fail(
		err=>{console.log("url failed:" + url +"\n" + err)});
	});

	let tags = jqid(formId).find('.metadata-tag');
	tags.attr(ATTR_TITLE,'Click to remove');
	tags.click(function() {
	    let input = form.find("#" + $(this).attr('metadata-id'));
	    if($(this).hasClass('metadata-tag-deleted')) {
		$(this).removeClass('metadata-tag-deleted')		
		input.val("");
	    } else {
		$(this).addClass('metadata-tag-deleted')		
		input.val("delete");
	    }
	});
    },
    initFormUpload:function(argPrefix,fileInputId, targetId,multiple) {
	if(!argPrefix) argPrefix = 'upload';
	let input = jqid(fileInputId);
	if(multiple)
	    input.attr('multiple','');
	let form = input.closest('form');
	let custom = HU.div([ATTR_TITLE,"Click to select a file",
			     ATTR_ID,fileInputId+"_filewrapper",
			     ATTR_CLASS, 'fileinput_wrapper'],HU.getIconImage("fas fa-upload") +
			    SPACE +HU.div([ATTR_ID,fileInputId+"_filename",
					   ATTR_CLASS,"fileinput_label"]));
	input.after(custom);
	input.hide();
	let inputWrapper = jqid(fileInputId+"_filewrapper");
	let inputLabel = jqid(fileInputId+"_filename");
	inputWrapper.click(()=>{input.trigger('click');});
	let inputChanger = ()=>{
	    let clean = name=>{
		let idx = name.lastIndexOf("\\");
		if(idx<0)
		    idx = name.lastIndexOf("/");		
		if(idx>=0) {
		    name = name.substring(idx+1);
		}
		return name;
	    };
	    let fileName = '';
	    if(input[0].files) {
		let names=[];
		for(let i=0;i<input[0].files.length;i++) {
		    let file = input[0].files[i];
		    names.push(clean(file.name) +" (" + Utils.formatFileLength(file.size)+")");
		}
		fileName = Utils.join(names,',');
	    } else {
		fileName =  clean(input.val()); 
	    }
	    if(fileName=="")
		fileName = HU.span([ATTR_CLASS,"fileinput_label_empty"],"Click to select a file");
	    jqid(fileInputId+"_filename").html(fileName); 
	};
	input.bind('change', inputChanger);
	inputChanger();

	if(Utils.stringDefined(targetId)) {
	    let target = jqid(targetId);
	    let fileDrop = {
		files:{},
		cnt:0,
		added:false
	    };
	    target.append(HU.div([ATTR_CLASS,"ramadda-dnd-target-files",
				  ATTR_ID,fileInputId+"_dnd_files"]));
	    let files=jqid(fileInputId+"_dnd_files");
	    Utils.initDragAndDrop(target,
				  event=>{},
				  event=>{},
				  (event,item,result,wasDrop) =>{
				      fileDrop.cnt++;
				      let name = item.name;
				      if(!name && item.getAsFile) {
					  name = item.getAsFile().name;
				      }

				      if(!name) {
					  let isImage = item.type && item.type.match("image/.*");
					  name = prompt("Entry file name:",isImage?"image":"file");
					  if(!name) return;
					  if(item.type) {
					      if(item.type=="text/plain") {
						  if(!name.endsWith(".txt")) {
						      name = name+".txt";
						  }
					      } else {
						  let type  = item.type.replace(/.*\//,"");
						  name = name+"."+type;
					      }
					  }
				      }
				      let listId = fileInputId +"_list" + fileDrop.cnt;
				      let inputId = fileInputId +"_file" + fileDrop.cnt;
				      let nameInputId = fileInputId +"_file_name" + fileDrop.cnt;
				      let fileName = argPrefix+"_file_" + fileDrop.cnt;
				      let nameName = argPrefix+"_name_" + fileDrop.cnt;				  				  
				      fileDrop.files[inputId] = result;
				      let del =HU.span([ATTR_CLASS,CLASS_CLICKABLE,ID,listId+"_trash"],HU.getIconImage(icon_trash));
				      let size = Utils.isDefined(item.size)?Utils.formatFileLength(item.size):"";
				      files.append(HU.div([ATTR_ID,listId],del +" " +name+" "+size));
				      form.append(HU.tag(TAG_INPUT,[ATTR_TYPE,'hidden',
								    ATTR_NAME,fileName,
								    ATTR_ID,inputId]));
				      form.append(HU.tag(TAG_INPUT,[ATTR_TYPE,'hidden',
								    ATTR_NAME,nameName,
								    ATTR_ID,nameInputId]));				  
				      jqid(inputId).val(result);
				      jqid(nameInputId).val(name);				  
				      jqid(listId+"_trash").click(function(){
					  jqid(listId).remove();
					  jqid(inputId).remove();
					  jqid(nameInputId).remove();				      
				      });
				  },null, true);
	}			      
    },

    handleDropEvent:function(event,file, result,entryId,authToken,callback) {
	let isImage= file.type.match('^image.*');
	let url = RamaddaUtil.getUrl("/entry/addfile");
	let desc = "";
	let name = file.name??'file';
	let fileName = file.name;
	let suffix;
	if(file.type=='text/plain') suffix="txt";
	else suffix =  file.type.replace(/image\//,"");
	if(!fileName) {
	    fileName =  name+"." + suffix;
	}
	let finish = () =>{
	    if(!file.name) {
		fileName = Utils.makeId(name) +'.' + suffix;
	    }
	    
	    let data = new FormData();
	    data.append("filename",fileName);
	    if(authToken)
		data.append("authtoken",authToken);
	    //A hack for shapefiles and geojson
	    if(file.type=='application/zip') 
		data.append("filetype",'geo_shapefile');
	    else if(file.type=='application/json') 
		data.append("filetype",'geo_geojson');	
	    else
		data.append("filetype",file.type);
	    data.append("name",name);
	    data.append("group",entryId);
	    data.append("description",desc);
	    data.append("file", result);
	    let dialog;
	    $.ajax({
		url: url,
		cache: false,
		contentType: false,
		processData: false,
		method: 'POST',
		type: 'POST', 
		data: data,
		success:  (data) =>{
		    dialog.remove();
		    if(data.status!='ok') {
			alert("An error occurred creating entry: "  + data.message);
			return;
		    }
		    if(callback) callback(data,data.entryid, data.name,isImage);
		},
		error: function (err) {
		    dialog.remove();
		    alert("An error occurred creating entry: "  + err);
		}
	    });
	    let html = HU.div([ATTR_STYLE,HU.css(CSS_TEXT_ALIGN,ALIGN_CENTER,CSS_PADDING,HU.px(5))],
			      "Creating entry<br>"+HU.image(RamaddaUtil.getCdnUrl('/icons/mapprogress.gif'),
							    [ATTR_WIDTH,HU.px(50)]));
	    dialog = HU.makeDialog({content:html,anchor:$(document),my:"center top",at:"center top+100"});    
	}

	name = Utils.makeLabel(name.replace(/.[^\.]*$/,''));
	if(file.name)
	    name = prompt('Dropped file: ' + fileName +"\nNew entry name:",name);
	else
	    name = prompt('Copied file: ' + suffix +"\nNew entry name:",name);	    
	if(!name) return;
	finish();

    },


    //applies extend to the given object
    //and sets a super member to the original object
    //you can call original super class methods with:
    //this.super.<method>.call(this,...);
    inherit: function(object, parent) {
        $.extend(object, parent);
        parent.getThis = function() {
            return object;
        }
        object.getThis = function() {
            return object;
        }
        object.mysuper = parent;
        return object;
    },
    //Just a wrapper around extend. We use this so it is easy to find 
    //class definitions
    initMembers: function(object, members) {
        $.extend(object, members);
        return object;
    },
    //Just a wrapper around extend. We use this so it is easy to find 
    //class definitions
    defineMembers: function(object, members) {
        $.extend(object, members);
        return object;
    },

    showEntryPopup:function(id,entryId,label,extra) {
	let html = RamaddaUtils.contents[entryId];
	if(html) {
	    RamaddaUtils.showEntryPopupInner(id,entryId,label,html);
	} else {
	    let url = RamaddaUtil.getUrl("/entry/menu?entryid=" + entryId);
            $.ajax({
                url: url,
                dataType: 'text',
                success: function(html) {
		    RamaddaUtils.contents[entryId] = html;
		    RamaddaUtils.showEntryPopupInner(id,entryId,label,html,extra);
                }
            }).fail((jqxhr, settings, exc) => {
                console.log("/entry/menu failed:" + exc);
		alert('Failed to contact the server');
            });
	}
    },

    showEntryPopupInner:function(id,entryId,label,html,extraLink) {
	let anchor = jqid(id);
	let headerRight=null;
	/*
	  if(Utils.isAnonymous()) {
	  headerRight = HU.href('/repository/user/login',
	  HU.getIconImage('fas fa-sign-in-alt')+' Login' + HU.space(2));
	  }
	*/


	this.entryPopup =
	    HU.makeDialog({content:html,my:"left top",at:"left bottom",title:label,
			   rightSideTitle:extraLink,
			   anchor:anchor,draggable:true,header:true,inPlace:false,headerRight:headerRight});    
    },

    initEntryListForm:function(formId) {
	let visibilityGroup = Utils.entryGroups[formId];
	if (visibilityGroup) {
            visibilityGroup.on = 0;
            visibilityGroup.setVisbility();
	}
    },

    hideEntryPopup:function() {
	if(this.entryPopup) {
	    this.entryPopup.remove();
	    this.entryPopup=null;
	}
	HU.getTooltip().hide();
    },




    originalImages:new Array(),
    changeImages: new Array(),

    folderClick:function(uid, url, changeImg) {
	RamaddaUtil.changeImages[uid] = changeImg;
	let jqBlock = jqid(uid);
	if (jqBlock.length == 0) {
            return;
	}
	let jqImage = jqid("img_" + uid);
	let showing = jqBlock.css(CSS_DISPLAY) != DISPLAY_NONE;
	if (!showing) {
            RamaddaUtil.originalImages[uid] = jqImage.html();
            jqBlock.show();
            jqImage.html(HU.getIconImage("fa-caret-down"));
	    url +="&orderby=entryorder&ascending=true";
	    if(url.startsWith("/") && RamaddaUtil.currentRamaddaBase) {
		url = RamaddaUtil.currentRamaddaBase +url;
	    }

            GuiUtils.loadXML(url, RamaddaUtil.handleFolderList, uid);
	} else {
            if (changeImg) {
		jqImage.html(HU.getIconImage("fa-caret-right"));
	    }
            jqBlock.hide();
	}
    },

    handleFolderList:function(request, uid) {
	if (request.responseXML != null) {
            let xmlDoc = request.responseXML.documentElement;
            let script;
            let html;
            for (i = 0; i < xmlDoc.childNodes.length; i++) {
		let childNode = xmlDoc.childNodes[i];
		if (childNode.tagName == "javascript") {
                    script = getChildText(childNode);
		} else if (childNode.tagName == "content") {
                    html = getChildText(childNode);
		} else {}
            }
            if (!html) {
		html = getChildText(xmlDoc);
            }
            if (html) {
		jqid(uid).html(HU.div([],html));
		Utils.checkTabs(html);
            }
            if (script) {
		eval(script);
            }
	}
	
	if (RamaddaUtil.changeImages[uid]) {
            jqid("img_" + uid).attr('src', icon_folderopen);
	} else {
            jqid("img_" + uid).attr('src', RamaddaUtil.originalImages[uid]);
	}
    },


    Components: {
	init: function(id) {
	    let container = jqid(id);
	    let header = jqid(id +"_header");
	    let hasTags = false;
	    let hasLocations = false;
	    let components = container.find(".ramadda-component");
	    let years = {};	    
	    let months = {};
	    let days = {};	    
	    components.each(function() {
		let date = $(this).attr("component-date");
		if(!date) return;
		let dttm = Utils.parseDate(date)
		let tmp;
		$(this).attr("component-day",tmp = Utils.formatDateWithFormat(dttm,"mmmm d yyyy"));
		days[tmp]  =true;
		$(this).attr("component-month",tmp = Utils.formatDateWithFormat(dttm,"mmmm yyyy"));
		months[tmp]  =true;
		$(this).attr("component-year",tmp = Utils.formatDateWithFormat(dttm,"yyyy"));		
		years[tmp] = true;
		if($(this).attr('component-latitude')) hasLocations = true;
		if(Utils.stringDefined($(this).attr('component-tags'))) {
		    hasTags = true;
		}
	    });
	    header.css(CSS_TEXT_ALIGN,ALIGN_CENTER);
	    let hdr = 
		HU.div([ATTR_STYLE,HU.css(CSS_DISPLAY,DISPLAY_INLINE_BLOCK),
			ATTR_CLASS,HU.classes(CLASS_BUTTON,HU.classes(CLASS_BUTTON_BAR,'ramadda-button-on')),
			ATTR_LAYOUT,"grid"],"Grid");
	    if(Object.keys(years).length>1)
		hdr += HU.div([ATTR_STYLE,HU.css(CSS_DISPLAY,DISPLAY_INLINE_BLOCK),
			       ATTR_CLASS,HU.classes(CLASS_BUTTON,CLASS_BUTTON_BAR),
			       ATTR_LAYOUT,"year"],"Year");
	    if(Object.keys(months).length>1)
		hdr+=HU.div([ATTR_STYLE,HU.css(CSS_DISPLAY,DISPLAY_INLINE_BLOCK),
			     ATTR_CLASS,HU.classes(CLASS_BUTTON,CLASS_BUTTON_BAR),
			     ATTR_LAYOUT,"month"],"Month");
	    if(Object.keys(days).length>1)	    
		hdr+=HU.div([ATTR_STYLE,HU.css(CSS_DISPLAY,DISPLAY_INLINE_BLOCK),
			     ATTR_CLASS,HU.classes(CLASS_BUTTON,CLASS_BUTTON_BAR),
			     ATTR_LAYOUT,"day"],"Day");		
	    hdr += HU.div([ATTR_STYLE,HU.css(CSS_DISPLAY,DISPLAY_INLINE_BLOCK),
			   ATTR_CLASS,HU.classes(CLASS_BUTTON,CLASS_BUTTON_BAR),
			   ATTR_LAYOUT,ATTR_TITLE],"Title");	    
	    if(hasTags)
		hdr +=HU.div([ATTR_STYLE,HU.css(CSS_DISPLAY,DISPLAY_INLINE_BLOCK),
			      ATTR_CLASS,HU.classes(CLASS_BUTTON,CLASS_BUTTON_BAR),
			      ATTR_LAYOUT,"tags"],"Tag");

	    hdr+= HU.div([ATTR_STYLE,HU.css(CSS_DISPLAY,DISPLAY_INLINE_BLOCK),
			  ATTR_CLASS,HU.classes(CLASS_BUTTON,CLASS_BUTTON_BAR),
			  ATTR_LAYOUT,"author"],"Author");			
	    if(hasLocations) {
		hdr += HU.div([ATTR_STYLE,HU.css(CSS_DISPLAY,DISPLAY_INLINE_BLOCK),
			       ATTR_CLASS,HU.classes(CLASS_BUTTON,CLASS_BUTTON_BAR),
			       ATTR_LAYOUT,"map"],"Map");			
	    }
	    header.append(HU.div([],hdr));


	    
	    let buttons = header.find(HU.dotClass(CLASS_BUTTON));
	    buttons.click(function(){
		buttons.removeClass("ramadda-button-on");
		$(this).addClass("ramadda-button-on");	    
		let layout = $(this).attr(ATTR_LAYOUT);
		if(layout=="grid") RamaddaUtil.Components.layout(container,components,null);
		else  RamaddaUtil.Components.layout(container,components,"component-" + layout);
	    });
	},
	layout: function(container,components,by) {
	    container.find(".ramadda-group").each(function() {
		$(this).detach();
	    });
	    if(container.mapId)
		jqid(container.mapId).detach();
	    if(container.ramaddaMap) {
		ramaddaMapRemove(container.ramaddaMap);
		container.ramaddaMap = null;
	    }
	    if(by==null) {
		components.show();
		components.each(function() {
		    $(this).detach();
		    container.append($(this));
		});
		return;
	    } else if(by=='component-map') {
		components.hide();
		let id = Utils.getUniqueId();
		container.mapId = id;
		container.append(HU.div([ATTR_ID,id,STYLE,HU.css(CSS_WIDTH,HU.perc(100),CSS_HEIGHT,HU.px(400))]));
		let params={};
		let map = new RepositoryMap(id,params);
		container.ramaddaMap = map;
		map.initMap(false);
		components.each(function() {
		    let  url = $(this).attr("component-url");		    
		    let lat = +$(this).attr("component-latitude");
		    let lon = +$(this).attr("component-longitude");		    
		    if(!Utils.isNumber(lat)) return;
		    let image = $(this).attr("component-image");		    
		    let popup = HU.center(HU.b($(this).attr("component-title")));		    
		    if(image) popup +=HU.image(image,[ATTR_WIDTH,HU.px(300)]);
		    if(url) popup=HU.href(url,popup);
		    let point = new MapUtils.createLonLat(lon,lat);
		    map.addPoint("", point, {pointRadius:6,
					     strokeWidth:1,
					     strokeColor:COLOR_BLACK,
					     fillColor:"blue"},popup);
		});
		map.centerOnMarkers();
	    } else {
		components.show();

		let isDate = 		by=="component-day" || by=="component-month" || by=="component-year";
		let values = [];
		let valueMap = {};
		components.each(function() {
		    let attr = $(this).attr(by)||"";
		    if(by=="component-tags") {
			let tags = attr.split(",");
			attr = tags[0];
		    }
		    if(!valueMap[attr]) {
			valueMap[attr] = [];
			let dttm = null;
			if(isDate) {
			    let date = $(this).attr("component-date");
			    let dttm = date?Utils.parseDate(date):null;
			    values.push([attr,dttm]);
			} else {
			    values.push(attr);
			}
		    }
		    valueMap[attr].push($(this));
		});
		if(isDate) {
		    values = values.sort((a,b)=>{
			a = a[1];
			b = b[1];
			if(!a || !b) return 0;
			if(!a) return 1;
			if(!b) return -1;
			return b.getTime()-a.getTime();
		    });
		} else {
		    values = values.sort();
		}
		values.forEach(value=>{
		    if(isDate) value = value[0];
		    let group = container.append($(HU.div([ATTR_CLASS,"ramadda-group"],
							  HU.div([ATTR_CLASS,"ramadda-group-header"],value))));
		    valueMap[value].forEach(child=>{
			group.append(child);
		    })
		});


	    }
	    
	    
	},
    }

    
}



function EntryRow(entryId, rowId, cbxId, cbxWrapperId, showDetails,args) {
    if(!args) args = {
	showIcon:true
    } 
    this.args = args;
    args.entryId = entryId;
    Utils.globalEntryRows[entryId] = this;
    Utils.globalEntryRows[rowId] = this;
    Utils.globalEntryRows[cbxId] = this;
    this.entryId = entryId;
    this.showIcon = args.showIcon;
    this.onColor = "#FFFFCC";
    this.overColor = "#f4f4f4";
    this.rowId = rowId;
    this.cbxId = cbxId;
    this.cbxWrapperId = cbxWrapperId;
    this.showDetails = showDetails;
    this.getRow = function() {
        return jqid(this.rowId);
    }

    this.getCbx = function() {
        return jqid(this.cbxId);
    }

    let form = this.getCbx().closest('form');
    if (form.length) {
        let visibilityGroup = Utils.entryGroups[form.attr(ATTR_ID)];
        if (visibilityGroup) {
            visibilityGroup.addEntryRow(this);
        }
    } else {
        this.getCbx().hide();
    }

    this.setCheckbox = function(value) {
        this.getCbx().prop('checked', value);
        this.setRowColor();
    }


    this.isSelected = function() {
        return this.getCbx().is(':checked');
    }

    this.setRowColor = function() {
        this.getRow().removeClass("entry-row-hover");	    
        if (this.isSelected()) {
            this.getRow().addClass("entry-list-row-selected");
        } else {
            this.getRow().removeClass("entry-list-row-selected");	    
        }
    }


    this.mouseOver = function(event) {
        jqid("entrymenuarrow_" + rowId).attr('src', icon_menuarrow);
        this.getRow().addClass("entry-row-hover");
	//        this.getRow().css('background-color', this.overColor);
    }

    this.mouseOut = function(event) {
        jqid("entrymenuarrow_" + rowId).attr('src', icon_blank);
        this.setRowColor();
    }


    this.mouseClick = function(event) {
        eventX = GuiUtils.getEventX(event);
        let position = this.getRow().offset();
        //Don't pick up clicks on the left side
        if (eventX - position.left < 150) return;
        this.lastClick = eventX;
        let url = RamaddaUtil.getUrl("/entry/show?entryid=" + entryId + "&output=metadataxml");
        if (this.showDetails) {
            url += "&details=true";
        } else {
            url += "&details=false";
        }
        if (this.showIcon) {
            url += "&showIcon=true";
        } else {
            url += "&showIcon=false";
        }
        GuiUtils.loadXML(url, this.handleTooltip, this);
    }

    this.handleTooltip = function(request, entryRow) {
        let xmlDoc = request.responseXML.documentElement;
        text = getChildText(xmlDoc);
        let leftSide = entryRow.getRow().offset().left;
        let offset = entryRow.lastClick - leftSide;
        let close = HU.jsLink("", HU.getIconImage(ICON_CLOSE),
			      [ATTR_ONMOUSEDOWN, "RamaddaUtil.hideEntryPopup();",
			       ATTR_ID,"tooltipclose"]);
	let label = HU.image(entryRow.args.icon)+ SPACE +entryRow.args.name;
	let header =  HU.div([ATTR_CLASS,"ramadda-popup-header"],close +SPACE2 +label);
	let html = HU.div([ATTR_CLASS,"ramadda-popup",
			   ATTR_STYLE,HU.css(CSS_DISPLAY,DISPLAY_BLOCK)],
			  header + HU.table([], text));
	let popup =  HU.getTooltip();
	popup.html(html);
	popup.draggable();
        Utils.checkTabs(text);
        let pos = entryRow.getRow().offset();
        let eWidth = entryRow.getRow().outerWidth();
        let eHeight = entryRow.getRow().outerHeight();
        let mWidth = HU.getTooltip().outerWidth();
        let wWidth = $(window).width();

        let x = entryRow.lastClick;
        if (entryRow.lastClick + mWidth > wWidth) {
            x -= (entryRow.lastClick + mWidth - wWidth);
        }

        let left = HU.px(x);
        let top = HU.px((3 + pos.top + eHeight));
        popup.css({
            position: POSITION_ABSOLUTE,
            zIndex: 5000,
            left: left,
            top: top
        });
        popup.show();
    }
}


var selectors = new Array();
function Selector(event, selectorId, elementId, allEntries, selecttype, localeId, entryType, ramaddaUrl,props) {
    let _this = this;
    this.id = selectorId;
    this.elementId = elementId;
    this.domId = HU.getUniqueId('selector_');
    this.props = props||{};
    this.localeId = localeId;
    this.entryType = entryType;
    this.allEntries = allEntries;
    this.selecttype = selecttype;
    this.textComp = GuiUtils.getDomObject(this.elementId);
    this.ramaddaUrl = ramaddaUrl ?? ramaddaBaseUrl;
    this.getTextComponent = function() {
        let id = "#" + this.elementId;
        return $(id);
    }

    this.getHiddenComponent = function() {
        let id = "#" + this.elementId + "_hidden";
        return $(id);
    }

    this.cancel= function(override) {
	if(!override) {
	    if(this.pinned) return;
	}
	jqid(this.domId).remove();
    }

    this.clearInput = function() {
        this.getHiddenComponent().val("");
        this.getTextComponent().val("");
    }

    this.handleClick = function(event) {
        let src = this.props.anchor;
	if(!src) {
	    if(this.props.eventSourceId) {
		src = jqid(this.props.eventSourceId);
	    }
	    if(src==null || src.length==0) {
		if(event && event.target) {
		    src = $(event.target);
		}
	    }
	    if(src==null || src.length==0)  {
		let srcId = this.id + '_selectlink';
		src = jqid(srcId);
	    }
	    if(src==null || src.length==0)  {
		src = jqid(this.id);
	    }
	}

        HU.hidePopupObject(event);
	let container = $(HU.div([ATTR_STYLE,HU.css(CSS_POSITION,POSITION_RELATIVE)])).appendTo("body");
        $(HU.div([ATTR_STYLE,HU.css(CSS_MIN_WIDTH,HU.px(200),CSS_MIN_HEIGHT,HU.px(200)),
		  ATTR_CLASS,'ramadda-selectdiv',
		  ATTR_ID,this.domId])).appendTo(container);
        this.div = jqid(this.domId);
	if(this.props.minWidth) {
	    this.div.css('min-width',this.props.minWidth);
	}
	this.div.draggable();
	this.anchor = src;
	this.showDiv = () =>{
            this.div.show();
            this.div.position({
		of: this.anchor,
		my: this.props.locationMy??"left top",
		at: this.props.locationAt??"left bottom",
		collision: this.props.collision??"fit fit"
            });
	};


        let url =  HU.url('/entry/show',['output','selectxml','noredirect','true','firstclick',true]);
	if(this.selecttype) 
	    url = HU.url(url,['selecttype',this.selecttype]);
	url= HU.url(url,'allentries', this.allEntries,'target', this.id);

	if(this.ramaddaUrl && !this.ramaddaUrl.startsWith("/")) {
	    let pathname = new URL(this.ramaddaUrl).pathname
	    let root = this.ramaddaUrl.replace(pathname,"");
	    RamaddaUtil.currentRamaddaBase = root;
            url = this.ramaddaUrl + url;
	} else {
	    RamaddaUtil.currentRamaddaBase = null;
	    url = RamaddaUtil.getUrl(url);
	}
        if (this.localeId) {
            url = url + "&localeid=" + this.localeId;
        }
        if (this.entryType) {
            url = url + "&entrytype=" + this.entryType;
        }
	if(this.props.typeLabel) {
            url = url + "&typelabel=" + this.props.typeLabel;
	}
	if(Utils.isDefined(this.props.showTypeSelector)) {
            url = url + "&showtypeselector=" + this.props.showTypeSelector;
	}	
        GuiUtils.loadXML(url, (request,id)=>{_this.handleSelect(request,id)}, this.id);
        return false;
    }
    this.handleClick(event);
}


Selector.prototype = {
    handleSelect:function(request, id) {
	let _this = this;
	let xmlDoc = request.responseXML.documentElement;
	text = getChildText(xmlDoc);
	let pinId = this.domId +"-pin";
	let pin = HU.jsLink("",HU.getIconImage(icon_pin), [ATTR_CLASS,"ramadda-popup-pin",
							   ATTR_ID,pinId]); 
	let closeImage = HU.getIconImage(ICON_CLOSE, []);
	let closeId = id+'_close';
	let close = HU.span([ATTR_ID,closeId,
			     ATTR_CLASS,CLASS_CLICKABLE],closeImage);
	let title = (this.props?this.props.title:"")??"";
	let extra = (this.props?this.props.extra:"")??"";
	if(Utils.stringDefined(title)) {
	    title = HU.span([ATTR_STYLE,HU.css(CSS_MARGIN_LEFT,HU.px(5))], title);
	}
	let header = HU.div([ATTR_STYLE,HU.css(CSS_TEXT_ALIGN,ALIGN_LEFT),
				    ATTR_CLASS,"ramadda-popup-header"],SPACE+close+SPACE+pin+title);
	let popup = HU.div([ATTR_ID,id+"-popup"], header + extra+text);
	this.div.html(popup);
	this.showDiv();
	
	jqid(closeId).click(()=>{
	    this.cancel(true);
	});
	jqid(pinId).click(function() {
	    _this.pinned = !_this.pinned;
	    if(!_this.pinned) {
		$(this).removeClass("ramadda-popup-pin-pinned");
	    } else {
		$(this).addClass("ramadda-popup-pin-pinned");
	    }
	});
	/*
	  let arr = this.div.getElementsByTagName('script')
	  //Eval any embedded js
	  for (let n = 0; n < arr.length; n++) {
	  eval(arr[n].innerHTML);
	  }
	*/
	if(this.props && this.props.initCallback) {
	    this.props.initCallback();
	}
    }
}












function getChildText(node) {
    let text = '';
    for (childIdx = 0; childIdx < node.childNodes.length; childIdx++) {
        text = text + node.childNodes[childIdx].nodeValue;
    }
    return text;
}


function toggleBlockVisibility(id, imgid, showimg, hideimg,visible) {
    HU.toggleBlockVisibility(id, imgid, showimg, hideimg,null,visible);
}


function toggleInlineVisibility(id, imgid, showimg, hideimg) {
    let img = GuiUtils.getDomObject(imgid);
    let icon;
    toggleVisibility(id, DISPLAY_INLINE);
    if(jqid(id).is(':visible'))
        icon= showimg;
    else
        icon = hideimg;

    if(StringUtil.startsWith(icon,"fa-")) {
        jqid(imgid).attr(ATTR_CLASS,icon);
    } else {
        if(img) img.obj.src = icon;
    }
    Utils.ramaddaUpdateMaps();
}


function toggleVisibility(id, style,anim,forceVisible) {
    let display = jqid(id).css(CSS_DISPLAY);
    if(Utils.isDefined(forceVisible)) {
	if(forceVisible)
	    jqid(id).show();
	else
	    jqid(id).hide();	
	return forceVisible;
    }
    jqid(id).toggle(anim);
    return display != DISPLAY_BLOCK;
}

function hide(id) {
    jqid(id).hide();
}

function hideObject(obj) {
    if (!obj) {
        return 0;
    }
    jqid(obj.id).hide();
    return 1;
}


function showObject(obj, display) {
    if (!obj) return 0;
    jqid(obj.id).show();
    return;
}

function toggleVisibilityOnObject(obj, display) {
    if (!obj) return 0;
    jqid(obj.id).toggle();
}







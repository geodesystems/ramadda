/**
   Copyright (c) 2008-2023 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/


var RamaddaUtils;
var RamaddaUtil;
var Ramadda = RamaddaUtils = RamaddaUtil  = {
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
		data.append("entryid",entryId);
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
			let html  = data.message+"<br>"+HU.image(data.imageurl,[ATTR_WIDTH,"600px"]);
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
		    html = HU.div(['style',HU.css('margin','5px','max-height','300px','overflow-y','auto')], html);
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
    selectInitialClick:function(event, selectorId, elementId, allEntries, selecttype, localeId, entryType,baseUrl,props) {
	if(RamaddaUtils.currentSelector) {
	    RamaddaUtils.currentSelector.cancel();
	}
	RamaddaUtils.currentSelector = RamaddaUtils.selectCreate(event, selectorId, elementId, allEntries, selecttype, localeId, entryType,baseUrl,props);
	return false;
    },
    selectCreate:function(event, selectorId, elementId, allEntries, selecttype, localeId, entryType, baseUrl,props) {
	let key = selectorId + (baseUrl||"");
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


    initEntryPopup:function(id,target,entryType) {
        let input = HU.input("","",["id",id+"_input",CLASS,"input",ATTR_PLACEHOLDER,"Search", "style",
                                    HU.css("width","200px")]);
        input = HU.center(input);
        let html = input +HU.div([CLASS,"ramadda-select-search-results","id",id+"_results"]);
        $("#" +id).html(html);
        let results = $("#" + id +"_results");


        $("#" + id +"_input").keyup(function(event){
            let value =  $(this).val();
            if(value=="") {
                results.hide();
                results.html("");
                return;
            }
            let keycode = (event.keyCode ? event.keyCode : event.which);
            if(keycode == 13) {
                let searchLink =  ramaddaBaseUrl + "/search/do?text=" + encodeURIComponent(value) +"&output=json";
		if(Utils.stringDefined(entryType)) searchLink=HU.url(searchLink,["type",entryType]);
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
                            html += HU.div(['index',idx, ATTR_CLASS,'ramadda-clickable ramadda-entry'], entry.getIconImage() +" " + entry.getName());
                        });
                        results.html(html);
			results.find('.ramadda-entry').click(function() {
			    let entry = entries[$(this).attr('index')];
                            RamaddaUtils.selectClick(target, entry.getId(),entry.getName(),{
				entryName: entry.getName(),
				icon:entry.getIconUrl(),
				entryType:entry.getType().id
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
        });
    },


    selectClick:function(id, entryId, value, opts) {
	selector = selectors[id];
	let handler = getHandler(id);
	if(!handler) handler = getHandler(selector.elementId);
	if (handler) {
            handler.selectClick(selector.selecttype, id, entryId, value);
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
	return RamaddaUtil.getUrl("/entry/show?entryid=" + entryId);
    },

    fileDrops:{
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
	let colList = Utils.split(props.columns??'name,entryorder,creator,date,time,createdate,download,size,type,attachments',',',true,true);

	let dateWidth = 130;
	let typeWidth = 100;	
	let sizeWidth  =80;
	colList.forEach(c=>{
	    if(c=='name' && props.showName)
		cols.push({id:"name",label:"Name",width:props.nameWidth});
	    else if(c=='date' && props.showDate)
		cols.push({id:"fromdate",label:"Date",width:props.fromDateWidth??props.dateWidth??dateWidth});
	    else if(c=='geo') {
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
		cols.push({id:c,label:"&nbsp;Download&nbsp;",width:100,align:'left'});
	    else if(c=='time' && props.showTime)
		cols.push({id:c,label:"&nbsp;Time&nbsp;",width:100,align:'right'});	    	    
	    else if(c=='creator' && props.showCreator)
		cols.push({id:c,label:"Creator",width:props.creatorWidth??200});	    
	    else if(c=='entryorder' && props.showEntryOrder)
		cols.push({id:c,label:"Order",width:75});

	    else if(c=='changedate' && props.showChangeDate)
		cols.push({id:c,label:"Change Date",width:props.changeDateWidth??props.dateWidth??dateWidth});	    
	    else if(c=='size' && props.showSize)
		cols.push({id:c,label:"Size",width:props.sizeWidth??sizeWidth});
	    else if(c=='type' && props.showType)
		cols.push({id:c,label:"Type",align:'left',paddingLeft:'10px',width:props.typeWidth??typeWidth});
	    else if(c=='attachments' && props.showAttachments)
		cols.push({id:c,label:"Attachments",align:'left',paddingLeft:'10px',width:props.attachmentsWidth??240});
	});

	let tableWidth=props.tableWidth??'100%';
	if(props.showHeader) {
	    html+=HU.open('table',['cellspacing','0',
				   'cellpadding','0',
				   ATTR_CLASS,'entry-list-header',
				   'width',tableWidth]);
	    let hdrAttrs = [ATTR_CLASS,"entry-list-header-column ramadda-clickable"];
	    cols.forEach((col,idx)=> {

		let attrs = hdrAttrs;
		let width = col.width;
		if(idx==0 && props.showForm) {
		    html+=HU.td(['style','padding-left:3px;','width','10'],
				HU.div(['style','width:15px','id',id+'_formarrow', 'title','Click to show entry form', ATTR_CLASS,'ramadda-clickable'], HU.getIconImage("fas fa-caret-right")));
		    width-=10;
		}
		if(Utils.isDefined(col.width)) {
		    attrs = Utils.mergeLists(attrs,["width",col.width]);
		}
		attrs.push('style',HU.css('padding-left',col.paddingLeft??'0px'))
		attrs = Utils.mergeLists(attrs,['orderby',col.id=='download'?'size':col.id,'title','Sort by '+ (col.id=='download'?'Size':col.label)]);
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
	    html+="</table>";
	} else if(!simple) {
	    html+=HU.div([ATTR_CLASS,'entry-list-noheader']);
	}
	let innerId = Utils.getUniqueId();
	let tableId = Utils.getUniqueId();	
	let classPrefix  = simple?'entry-list-simple':'entry-list';

	let attrs = ['id',innerId,ATTR_CLASS,classPrefix];
	if(props.showHeader && props.tableWidth) {
	    attrs.push('style',HU.css('width',props.tableWidth));
	}
	html+=HU.open("div",attrs);
	let formId;
	if(props.showForm) {
	    formId = HU.getUniqueId('form_');
	    html+=HU.open('form',['id',formId,'method','post','action',RamaddaUtil.getUrl('/entry/getentries')]);
	    let form = HU.checkbox("",['style',HU.css('margin-left','3px'), 'title','Toggle all','id',id+'_form_cbx'],false);
	    let actions = [["","Apply action"]];
	    props.actions.forEach(action=>{
		actions.push([action.id,action.label]);
	    });
	    form+=SPACE1;
	    form+= HU.select("",['name','output','id',id+'_form_action'],actions);
	    form+=SPACE1;
	    form+=HU.open('input',['name','getselected','type','submit','value','Selected',ATTR_CLASS,'submit ui-button ui-corner-all ui-widget','id','getselected1338','role','button']);
	    form+=SPACE1;
	    form+=HU.open('input',['name','getall','type','submit','value','All',ATTR_CLASS,'submit ui-button ui-corner-all ui-widget','id','getall1337','role','button']);
	    /*
	    if(props.canDelete)
		form+=SPACE1+HU.span(['target-type','repository.delete','title','Shift-drag-and-drop entries to delete',ATTR_CLASS,'ramadda-entry-target ramadda-clickable ramadda-hoverable'], HU.getIconImage('fas fa-trash'));
	    if(props.canExport)
	    form+=SPACE1+HU.span(['target-type','zip.export','title','Shift-drag-and-drop entries to export',ATTR_CLASS,'ramadda-entry-target ramadda-clickable ramadda-hoverable',], HU.getIconImage('fas fa-file-export'));
	    */
	    html+=HU.div([ATTR_CLASS,classPrefix +'-row','id',id+'_form','style','display:none;width:100%'],form);
	}

	let tableAttrs=['id',tableId];
	if(props.maxHeight) tableAttrs.push('style',HU.css('max-height',props.maxHeight,'overflow-y','auto'));
	html+=HU.open('div',tableAttrs);
	html+='</div>';
	html+='</form>';
	html+='</div>';
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

	main.find('.entry-list-header-column').click(function() {
	    let orderby = $(this).attr('orderby');
	    let url;
	    if(props.orderby == orderby) {
		//my orderby
		if(Utils.isDefined(props.ascending)) {
		    //if we have an ascending
		    if(props.ascending) 
			url = HU.addToDocumentUrl('ascending',false);
		    else {
			url = HU.addToDocumentUrl('ascending',null);
			url = HU.addToDocumentUrl('orderby',null);
		    }
		} else {
		    //add ascending
		    url = HU.addToDocumentUrl('orderby',orderby);
		    url = HU.addToDocumentUrl('ascending',true);
		}
	    } else {
		url = HU.addToDocumentUrl('ascending',true);	    		    
		url = HU.addToDocumentUrl('orderby',orderby);
	    }
            window.location = url;
	});



	//Don't do this as it screws up the width of the menu sometimes
	//	    HU.initSelect($("#"+id+"_form_action"));

	$('#'+ id+'_form_cbx').click(function() {
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
	$('#' + id+'_formarrow').click(function() {
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
		    html = HU.div([ATTR_STYLE,'word-break: break-all;'],html);
		}
		console.log(html);
		typeInfo.contents.push(html);
	    });
	});

	let mtd='';
	if(opts.wrapInDiv) {
	    Object.keys(types).forEach(type=>{
		let info = types[type];
		if(info.display.delimiter) {
		    mtd+='<div class=ramadda-metadata-display>';
		    if(info.display.header) mtd+=HU.b(info.display.header+':')+' ';
		    mtd+=Utils.join(info.contents,info.display.delimiter+' ');
		    mtd+='</div>';
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
		mtd+='<div class=ramadda-metadata-display>';
		if(info.display.header) mtd+=HU.b(info.display.header+':')+' ';
		mtd+=Utils.join(info.contents,'; ');
		mtd+='</div>';
	    });
	}

	if(Utils.stringDefined(mtd) && opts.doBigText) {
	    mtd = HU.div([ATTR_CLASS,'ramadda-metadata-bigtext','bigtext-length','100'], mtd);
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
	    let entryId = comp.attr('entryid');
	    let value = comp.val().trim();
	    let what = comp.attr('data-field');
	    let url = ramaddaBaseUrl + "/entry/changefield?entryid=" + entryId+'&what=' + what+'&value='+ value;
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
	    comp.css('background','yellow');
	    setTimeout(()=>{
		comp.css('background','#fff');
	    },4000);

	}

	let applyAll=(comp,delta)=>{
	    let start=false;
	    let index = parseInt(comp.val());
	    let startId = comp.attr('entryid');
	    entryOrder.each(function() {
		let id =$(this).attr('entryid');
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
	let main = $('#'+ mainId);
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
		v  = HU.span([ATTR_CLASS,this.props.textClass??'',ATTR_STYLE,this.props.textStyle??''],v);
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
			    v +=HU.div([ATTR_STYLE,'max-width:100%;overflow-wrap: break-word;'],mtd);
			    hasMetadata=true;
			}
		    }



		    let tds = [];
		    //[cbx,space,arrow,icon,thumbnail,v]
		    let cbxId = Utils.getUniqueId('entry_');
		    if(props.showForm)
			tds.push(HU.hidden('allentry',entry.getId()) +
				 HU.checkbox(cbxId,['rowid',rowId,'name','selentry','value', entry.getId(),ATTR_CLASS,'entry-form-select','style',HU.css('margin-right','2px','display','none')],false));

		    tds.push(HU.div(['style',HU.css('margin-right','2px','min-width','10px'),'innerid',innerId,'entryid',entry.getId(),'title','Click to show contents',ATTR_CLASS,'entry-arrow ramadda-clickable' ], this.getToggleIcon(false)));

		    if(props.showCrumbs && entry.breadcrumbs) {
			let crumbId = Utils.getUniqueId();
			v = HU.span(['id','breadcrumbtoggle_' + crumbId, 'breadcrumbid',crumbId, 'title','Show breadcrumbs',ATTR_CLASS,'ramadda-clickable ramadda-breadcrumb-toggle' ], HU.getIconImage('fas fa-plus-square')) +SPACE2
			    + HU.span(['style',HU.css('display','none'),'id',crumbId], entry.breadcrumbs+'&nbsp;&raquo;&nbsp;') +v;
		    }

		    if(props.showThumbnails && entry.getThumbnail()) {
			v+= '<br>'+HU.div([ATTR_CLASS,'ramadda-thumbnail',ATTR_STYLE,HU.css('max-height','100px','overflow-y','auto')],
					  HU.image(entry.getThumbnail(),['loading','lazy',
									 ATTR_CLASS,'ramadda-clickable ramadda-thumbnail-image',
									 ATTR_TITLE,'Click to enlarge',
									 ATTR_STYLE,HU.css('width','100px'),
									 'entry-url',entry.getEntryUrl()
									]));
		    }


		    tds.push(v);
		    v =  HU.table([],HU.tr(['valign','top'],HU.tds([],tds)));
		} else {
		    if(col.id=='attachments') {
			v='';
			entry.getMetadata().forEach(m=>{
			    if(m.type=='content.thumbnail' || m.type=='content.attachment') {
				let imgUrl;
				let f = m.value.attr1;
				let name = f.replace(/.*_file_/,'');
				let mUrl = HU.url(RamaddaUtil.getBaseUrl()+'/metadata/view/' + f,
						  ['element','1','entryid',entry.getId(),'metadata_id', m.id]);
			
				if(m.type=='content.thumbnail' || Utils.isImage(f)) {
				    imgUrl = mUrl;
				    name+='<br>'+HU.image(imgUrl,['width','290px']).replace(/"/g,"'");
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
				    v+=HU.href(mUrl,HU.image(imgUrl,[ATTR_CLASS,'ramadda-attachment ramadda-clickable','title',name,'width','75px','style',HU.css('margin','2px')]));
				}
			    }
			});
			if(v!='') v  =HU.div(['style',HU.css('max-width',
							     HU.getDimension(col.width),'overflow-x','auto')], v);

		    } else  if(col.id=='type') {
			v = HU.href(RamaddaUtil.getUrl('/search/type/' + entry.getType().id),v,['title','Search for entries of type ' + _v]);
		    }
		    let maxWidth = col.width-20;
		    maxWidth = col.width;		    
		    v = HU.div(['style',HU.css('padding-left',col.paddingLeft??'5px','white-space','nowrap','width',HU.getDimension(col.width),'text-align',col.align??'right','max-width',HU.getDimension(maxWidth),'overflow-x','hidden')+(last?HU.css('padding-right','4px'):'')],v);
		    attrs.push('align',col.align??'right');
		}
		if(Utils.isDefined(col.width)) {
		    attrs.push('width',HU.getDimension(col.width));
		}
		if(title) {
//		    attrs.push('title',title);
		}
//		v = HU.div([ATTR_CLASS,'entry-row-label'], v);
		line+=HU.td(attrs,v);
	    });		



	    let row =  HU.open('div',['entryid',entry.getId(),'id',rowId]);
	    row +=  HU.open('div',['entryid',entry.getId(),'id',rowId,ATTR_CLASS,rowClass]);
	    row+= HU.open('table',['cellspacing','0','cellpadding','border','width','100%',ATTR_CLASS,
				   'entry-row-table','entryid',entry.getId()]);
	    let title='';
	    let rowAttrs = [ATTR_CLASS,'entry-row','valign','top'];
	    if(!props.simple) {
		let img = entry.getIconImage(['width','32px']).replace(/"/g,"'");
//			attrs.push('data-icon',entry.getIconUrl());
		title = entry.getName();
		if(entry.remoteRepository) rowAttrs.push('remote-repository', entry.remoteRepository.name);

		let type = entry.getType();
		if(type) rowAttrs.push('data-type', type.name);
		let thumb =entry.getThumbnail()
		if(thumb)
		    rowAttrs.push('data-thumbnail', thumb);

		let file =entry.getFilename()
		if(file)
		    rowAttrs.push('data-filename', file);
		
		rowAttrs.push(ATTR_TITLE,title,'data-icon',entry.getIconUrl());
	    }
	    row+=HU.open('tr',rowAttrs);
	    row+=line;
	    row+='</tr></table>';
	    row+='</div>'
	    row+=HU.div(['id',innerId,'style',HU.css('margin-left','20px')]);
	    row+='</div>';
	    html+=row;
	});
	html+=HU.close('div');

	let container = jqid(id);
	if(!secondTime && props.tableWidth) {
	    container.css('width',props.tableWidth);
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
		    let title = $(this).attr('title');
		    let icon = $(this).attr('data-icon');		
		    let thumb = $(this).attr('data-thumbnail');		
		    if(icon) {
			icon = HtmlUtils.image(icon,[ATTR_WIDTH,'32px']);
			title = icon+HU.space(1) +title;
		    }
		    title = HU.div([],HU.b(title));
		    let val = $(this).val();
		    if(val)
			title+=HU.div([],val);

		    let type = $(this).attr('data-type');		
		    if(type) title=title+ 'Type: ' + type+'<br>';
		    let filename = $(this).attr('data-filename');		
		    if(filename) title=title+ 'File: ' + filename+'<br>';
		    let remote = $(this).attr('remote-repository');		
		    if(remote) title=title+ 'Remote: ' + remote+'<br>';
		    title = title+
			HU.div([],'Right-click to see entry menu') +
			HU.div([],'Shift-drag to copy/move');

		    if(thumb) {
			thumb = HtmlUtils.image(thumb,[ATTR_WIDTH,'250px']);
			title = title +  HU.div([ATTR_STYLE,'max-height:200px;overflow-y:hidden;'],thumb);
		    }

		    return HU.div([ATTR_STYLE,HU.css('margin','5px')],title);
		}});	    
	}
	container.find('.ramadda-breadcrumb-toggle').click(function() {
	    let id = $(this).attr('breadcrumbid');
	    let crumbs = $('#'+ id);
	    if(crumbs.css('display')=='none') {
		$('#breadcrumbtoggle_' +id).html(HU.getIconImage("fas fa-minus-square"));
		crumbs.css('display','inline');
	    } else {
		$('#breadcrumbtoggle_' +id).html(HU.getIconImage("fas fa-plus-square"));
		crumbs.css('display','none');
	    }
	});

	let _this=this;
	container.find('.entry-arrow').click(function() {
	    let entryId = $(this).attr('entryid');
	    let innerId = $(this).attr('innerid');	    
	    let inner = $("#"+innerId);
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
	    if(props.sortby) url=HU.url(url,['orderby',props.sortby]);
	    if(props.orderby) url=HU.url(url,['orderby',props.orderby]);	    
	    if(props.ascending) url=HU.url(url,['ascending',props.ascending]);	    
	    if(props.sortdir) {
		url=HU.url(url,['ascending',props.sortdir=='up']);
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
		    let table = HU.open('table',[ATTR_CLASS,'formtable']);
		    if(entry.getIsUrl()) {
			table+=HU.formEntry('URL:',HU.href(entry.getResourceUrl(),entry.getResourceUrl()));
		    } else if(entry.getIsFile()) {
			let url = entry.getResourceUrl();
			table+=HU.formEntry('File:',HU.href(url,entry.getFilename()) +' ' +
					    ' (' + entry.getFormattedFilesize()+')' +
					    ' ' + HU.href(url,HU.getIconImage('fas fa-download')));
		    }
		    
		    table+=HU.formEntry('Kind:',HU.href(RamaddaUtil.getUrl('/search/type/' + entry.getType().id),entry.typeName,['title','Search for entries of type ' + entry.typeName]));
		    let searchUrl = RamaddaUtil.getUrl('/search/type/' + entry.getType().id+'?user_id='+ entry.creator+'&search.submit=true');
		    let created = HU.href(searchUrl,entry.creator,
					  ['title','Search for entries of this type created by ' + entry.creator]);
		    table+=HU.formEntry('Created by:',created);
		    table+=HU.formEntry('Created:',entry.createDateFormat);
		    if(entry.startDate && entry.startDate.getTime()!=entry.createDate.getTime())
			table+=HU.formEntry('Date:',entry.startDateFormat);
		    if(entry.startDate && entry.endDate && entry.startDate.getTime()!=entry.endDate.getTime()) {
			table+=HU.formEntry('To Date:',entry.endDateFormat);
		    }
		    table+='</table>';
		    HU.jqid(innerId).html(table);
		}
	    });
	});

	if(props.simple) return;
	container.find('.entry-form-select').click(function(event) {
            let on  = $(this).is(':checked');
	    let row = $('#' + $(this).attr('rowid'));
	    HU.checkboxClicked(event,'entry_',$(this).attr('id'));
	    main.find('.entry-form-select').each(function() {
		let on  = $(this).is(':checked');
		let row = $('#' + $(this).attr('rowid'));
		if(on) row.addClass('entry-list-row-selected');
		else row.removeClass('entry-list-row-selected');	    
	    });
	});

	container.find('.ramadda-thumbnail-image').click(function() {
	    let src = $(this).attr('src');	
	    let url = $(this).attr('entry-url');
	    let contents = HU.href(url,HU.image(src),[ATTR_TITLE,'Click to view entry']);
	    HU.makeDialog({content:contents,my:'left top',at:'left bottom',anchor:this,header:true,draggable:true});
	});

	container.find('.ramadda-attachment').tooltip({
	    show: { effect: 'slideDown', delay: 500, duration: 1000 },
	    content: function () {return $(this).prop('title');}});

	let rows = container.find('.entry-list-row');
	rows.bind ('contextmenu', function(event) {
	    let entryRow = $(this);
	    let entry = entryMap[$(this).attr('entryid')];
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
	    let entry = entryMap[$(this).attr('entryid')];
	    if(!entry) return;
	    let source = $(this);
	    let entries = [entry];
	    $('#'+mainId).find('.entry-list-row-selected').each(function() {
		let selectedEntry = entryMap[$(this).attr('entryid')];
		if(!selectedEntry) return;
		if(selectedEntry.getId()!=entry.getId()) {
		    entries.push(selectedEntry);
		}
	    });
	    Utils.entryDragInfo = {
		dragSource: source.attr('id'),
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
			if(html!="") html+="<br>";
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
	    let entry = entryMap[$(this).attr('entryid')];
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

		let entry = entryMap[$(this).attr('entryid')];
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

	    let entry = entryMap[$(this).attr('entryid')];
	    if(!entry) return;
	    if(Utils.entryDragInfo.hasEntry(entry)) return;
	    if(!Utils.entryDragInfo.hasEntry(entry)) {
		url = RamaddaUtil.getUrl("/entry/copy?action=action.move&from=" + Utils.entryDragInfo.getIds() + "&to=" + entry.getId());
		document.location = url;
	    }
	});
    },

    initFormTags: function(formId) {
	let form = $('#'+formId);
	let inputs = form.find('.metadata-tag-input');
	form.attr('autocomplete','off');
	inputs.attr('autocomplete','off');
	inputs.keyup(function(event) {
	    HtmlUtils.hidePopupObject();
	    let val = $(this).val();
	    if(val=="") return;
	    let url = HU.getUrl(RamaddaUtil.getUrl("/metadata/suggest"),["value",val.trim()]);
	    let input = $(this);
	    $.getJSON(url, data=>{
		if(data.length==0) return;
		let suggest = "";
		data.forEach(d=>{
		    suggest+=HU.div([CLASS,"ramadda-clickable metadata-suggest","suggest",d],d);
		});
		let html = HU.div([CLASS,"ramadda-search-popup",STYLE,HU.css("max-width","200px",
									     "padding","4px")],suggest);
		let dialog = HU.makeDialog({content:html,my:"left top",at:"left bottom",anchor:input});
		dialog.find(".metadata-suggest").click(function() {
		    HtmlUtils.hidePopupObject();
		    input.val($(this).attr("suggest"));
		});
	    }).fail(
		    err=>{console.log("url failed:" + url +"\n" + err)});
	});

	let tags = $('#'+formId).find('.metadata-tag');
	tags.attr('title','Click to remove');
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
    initFormUpload:function(fileInputId, targetId,multiple) {
	let input = $("#"+ fileInputId);
	if(multiple)
	    input.attr('multiple','');
	let form = input.closest('form');
	let custom = HU.div([TITLE,"Click to select a file", ID,fileInputId+"_filewrapper",CLASS, 'fileinput_wrapper'],HU.getIconImage("fas fa-upload") +" " +HU.div([ID,fileInputId+"_filename",CLASS,"fileinput_label"]));
	input.after(custom);
	input.hide();
	let inputWrapper = $("#" +fileInputId+"_filewrapper");
	let inputLabel = $("#" +fileInputId+"_filename");
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
		fileName = HU.span([CLASS,"fileinput_label_empty"],"Click to select a file");
	    $('#' + fileInputId+"_filename").html(fileName); 
	};
	input.bind('change', inputChanger);
	inputChanger();

	if(Utils.stringDefined(targetId)) {
	    let target = $("#" + targetId);
	    let fileDrop = {
		files:{},
		cnt:0,
		added:false
	    };
	    target.append(HU.div([CLASS,"ramadda-dnd-target-files",ID,fileInputId+"_dnd_files"]));
	    let files=$("#" +fileInputId+"_dnd_files");
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
				      let fileName = "upload_file_" + fileDrop.cnt;
				      let nameName = "upload_name_" + fileDrop.cnt;				  				  
				      fileDrop.files[inputId] = result;
				      let del =HU.span([CLASS,"ramadda-clickable",ID,listId+"_trash"],HU.getIconImage(icon_trash));
				      let size = Utils.isDefined(item.size)?Utils.formatFileLength(item.size):"";
				      files.append(HU.div([ID,listId],del +" " +name+" "+size));
				      form.append(HU.tag("input",['type','hidden','name',fileName,'id',inputId]));
				      form.append(HU.tag("input",['type','hidden','name',nameName,'id',nameInputId]));				  
				      $("#"+inputId).val(result);
				      $("#"+nameInputId).val(name);				  
				      $("#"+listId+"_trash").click(function(){
					  $("#"+listId).remove();
					  $("#"+inputId).remove();
					  $("#"+nameInputId).remove();				      
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
	    let html = HU.div(['style',HU.css('text-align','center','padding','5px')], "Creating entry<br>"+HU.image(ramaddaCdn + '/icons/mapprogress.gif',['width','50px']));
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
	let anchor = $("#" + id);
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
	HtmlUtils.getTooltip().hide();
    },




    originalImages:new Array(),
    changeImages: new Array(),

    folderClick:function(uid, url, changeImg) {
	RamaddaUtil.changeImages[uid] = changeImg;
	let jqBlock = $("#" + uid);
	if (jqBlock.length == 0) {
            return;
	}
	let jqImage = $("#img_" + uid);
	let showing = jqBlock.css('display') != "none";
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
		$("#" + uid).html("<div>" + html + "</div>");
		Utils.checkTabs(html);
            }
            if (script) {
		eval(script);
            }
	}
	
	if (RamaddaUtil.changeImages[uid]) {
            $("#img_" + uid).attr('src', icon_folderopen);
	} else {
            $("#img_" + uid).attr('src', RamaddaUtil.originalImages[uid]);
	}
    },


    Components: {
	init: function(id) {
	    let container = $("#" + id);
	    let header = $("#" + id +"_header");
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
	    header.css("text-align","center");
	    let hdr = 
		HU.div([STYLE,HU.css("display","inline-block"), CLASS,"ramadda-button ramadda-button-bar ramadda-button-on","layout","grid"],"Grid");
	    if(Object.keys(years).length>1)
		hdr += HU.div([STYLE,HU.css("display","inline-block"), CLASS,"ramadda-button ramadda-button-bar","layout","year"],"Year");
	    if(Object.keys(months).length>1)
		hdr+=HU.div([STYLE,HU.css("display","inline-block"), CLASS,"ramadda-button ramadda-button-bar","layout","month"],"Month");
	    if(Object.keys(days).length>1)	    
		hdr+=HU.div([STYLE,HU.css("display","inline-block"), CLASS,"ramadda-button ramadda-button-bar","layout","day"],"Day");		
	    hdr += HU.div([STYLE,HU.css("display","inline-block"), CLASS,"ramadda-button ramadda-button-bar","layout","title"],"Title");	    
	    if(hasTags)
		hdr +=HU.div([STYLE,HU.css("display","inline-block"), CLASS,"ramadda-button ramadda-button-bar","layout","tags"],"Tag");

	    hdr+= HU.div([STYLE,HU.css("display","inline-block"), CLASS,"ramadda-button ramadda-button-bar","layout","author"],"Author");			
	    if(hasLocations) {
		hdr += HU.div([STYLE,HU.css("display","inline-block"), CLASS,"ramadda-button ramadda-button-bar","layout","map"],"Map");			
	    }
	    header.append(HU.div([],hdr));


	
	    let buttons = header.find(".ramadda-button");
	    buttons.click(function(){
		buttons.removeClass("ramadda-button-on");
		$(this).addClass("ramadda-button-on");	    
		let layout = $(this).attr("layout");
		if(layout=="grid") RamaddaUtil.Components.layout(container,components,null);
		else  RamaddaUtil.Components.layout(container,components,"component-" + layout);
	    });
	},
	layout: function(container,components,by) {
	    container.find(".ramadda-group").each(function() {
		$(this).detach();
	    });
	    if(container.mapId)
		$("#"+ container.mapId).detach();
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
		container.append(HU.div([ID,id,STYLE,HU.css("width","100%","height","400px")]));
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
		    if(image) popup +=HU.image(image,[WIDTH,"300px"]);
		    if(url) popup=HU.href(url,popup);
		    let point = new MapUtils.createLonLat(lon,lat);
		    map.addPoint("", point, {pointRadius:6, strokeWidth:1, strokeColor:'#000', fillColor:"blue"},popup);
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
		    let group = container.append($(HU.div([CLASS,"ramadda-group"],HU.div([CLASS,"ramadda-group-header"],value))));
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
        return $("#" + this.rowId);
    }

    this.getCbx = function() {
        return $("#" + this.cbxId);
    }

    let form = this.getCbx().closest('form');
    if (form.length) {
        let visibilityGroup = Utils.entryGroups[form.attr('id')];
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
        $("#" + "entrymenuarrow_" + rowId).attr('src', icon_menuarrow);
        this.getRow().addClass("entry-row-hover");
//        this.getRow().css('background-color', this.overColor);
    }

    this.mouseOut = function(event) {
        $("#entrymenuarrow_" + rowId).attr('src', icon_blank);
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
        let close = HU.jsLink("", HU.getIconImage(icon_close), ["onmousedown", "RamaddaUtil.hideEntryPopup();","id","tooltipclose"]);
	let label = HU.image(entryRow.args.icon)+ SPACE +entryRow.args.name;
	let header =  HU.div([CLASS,"ramadda-popup-header"],close +SPACE2 +label);
	let html = HU.div([CLASS,"ramadda-popup",STYLE,"display:block;"],   header + "<table>" + text + "</table>");
	let popup =  HtmlUtils.getTooltip();
	popup.html(html);
	popup.draggable();
        Utils.checkTabs(text);
        let pos = entryRow.getRow().offset();
        let eWidth = entryRow.getRow().outerWidth();
        let eHeight = entryRow.getRow().outerHeight();
        let mWidth = HtmlUtils.getTooltip().outerWidth();
        let wWidth = $(window).width();

        let x = entryRow.lastClick;
        if (entryRow.lastClick + mWidth > wWidth) {
            x -= (entryRow.lastClick + mWidth - wWidth);
        }

        let left = x + "px";
        let top = (3 + pos.top + eHeight) + "px";
        popup.css({
            position: 'absolute',
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
    this.domId = HU.getUniqueId('selector_');

    this.props = props||{};
    this.elementId = elementId;
    this.localeId = localeId;
    this.entryType = entryType;
    this.allEntries = allEntries;
    this.selecttype = selecttype;
    this.textComp = GuiUtils.getDomObject(this.elementId);
    this.ramaddaUrl = ramaddaUrl || ramaddaBaseUrl;
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

        HtmlUtils.hidePopupObject(event);
	let container = $(HU.div([ATTR_STYLE,'position:relative;'])).appendTo("body");
        $(HU.div(['style',HU.css('min-width','200px','min-height','200px'),ATTR_CLASS,'ramadda-selectdiv','id',this.domId])).appendTo(container);
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


        let url =  "/entry/show?output=selectxml&selecttype=" + this.selecttype + "&allentries=" + this.allEntries + "&target=" + this.id + "&noredirect=true&firstclick=true";

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
	let pin = HU.jsLink("",HtmlUtils.getIconImage(icon_pin), [ATTR_CLASS,"ramadda-popup-pin", ATTR_ID,pinId]); 
	let closeImage = HtmlUtils.getIconImage(icon_close, []);
	let closeId = id+'_close';
	let close = HU.span([ATTR_ID,closeId,ATTR_CLASS,'ramadda-clickable'],closeImage);
	let title = (this.props?this.props.title:"")??"";
	let extra = (this.props?this.props.extra:"")??"";
	if(Utils.stringDefined(title)) {
	    title = HU.span([ATTR_STYLE,'margin-left:5px;'], title);
	}
	let header = HtmlUtils.div([ATTR_STYLE,"text-align:left;",ATTR_CLASS,"ramadda-popup-header"],SPACE+close+SPACE+pin+title);
	let popup = HtmlUtils.div(["id",id+"-popup"], header + extra+text);
	this.div.html(popup);
	this.showDiv();
	
	jqid(closeId).click(()=>{
	    this.cancel(true);
	});
	$("#" + pinId).click(function() {
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
    HtmlUtils.toggleBlockVisibility(id, imgid, showimg, hideimg,null,visible);
}


function toggleInlineVisibility(id, imgid, showimg, hideimg) {
    let img = GuiUtils.getDomObject(imgid);
    let icon;
    toggleVisibility(id, 'inline');
    if(jqid(id).is(':visible'))
        icon= showimg;
    else
        icon = hideimg;

    if(StringUtil.startsWith(icon,"fa-")) {
        $("#" + imgid).attr(ATTR_CLASS,icon);
    } else {
        if(img) img.obj.src = icon;
    }
    Utils.ramaddaUpdateMaps();
}


function toggleVisibility(id, style,anim,forceVisible) {
    let display = $("#" + id).css('display');
    if(Utils.isDefined(forceVisible)) {
	if(forceVisible)
	    $("#" + id).show();
	else
	    $("#" + id).hide();	
	return forceVisible;
    }
    $("#" + id).toggle(anim);
    return display != 'block';
}

function hide(id) {
    $("#" + id).hide();
}

function hideObject(obj) {
    if (!obj) {
        return 0;
    }
    $("#" + obj.id).hide();
    return 1;
}


function showObject(obj, display) {
    if (!obj) return 0;
    $("#" + obj.id).show();
    return;
}

function toggleVisibilityOnObject(obj, display) {
    if (!obj) return 0;
    $("#" + obj.id).toggle();
}







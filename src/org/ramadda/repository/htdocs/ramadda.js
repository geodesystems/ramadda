/**
 * Copyright (c) 2008-2021 Geode Systems LLC
 */


var RamaddaUtils;
var RamaddaUtil;
var Ramadda = RamaddaUtils = RamaddaUtil  = {
    contents:{},
    currentRamaddaBase:null,

    fileDrops:{
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
	    let url = HU.getUrl(ramaddaBaseUrl +"/metadata/suggest",["value",val.trim()]);
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
    initFormUpload:function(fileInputId, targetId) {
	let input = $("#"+ fileInputId);
	let form = input.closest('form');


	let custom = HU.div([TITLE,"Click to select a file", ID,fileInputId+"_filewrapper",CLASS, 'fileinput_wrapper'],HU.getIconImage("fas fa-cloud-upload-alt") +" " +HU.div([ID,fileInputId+"_filename",CLASS,"fileinput_label"]));
	input.after(custom);
	input.hide();
	let inputWrapper = $("#" +fileInputId+"_filewrapper");
	let inputLabel = $("#" +fileInputId+"_filename");
	inputWrapper.click(()=>{input.trigger('click');});
	let inputChanger = ()=>{
	    let file  = input[0].files? input[0].files[0]:null;
	    let fileName =  file?file.name:input.val(); 
	    let idx = fileName.lastIndexOf("\\");
	    if(idx<0)
		idx = fileName.lastIndexOf("/");		
	    if(idx>=0) {
		fileName = fileName.substring(idx+1);
	    }
	    if(file) {
		fileName = fileName +" (" + Utils.formatFileLength(file.size)+")";
	    }
	    if(fileName=="") fileName = HU.span([CLASS,"fileinput_label_empty"],"Please select a file");
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
				  event=>{
				  },
				  event=>{
				  },
				  (event,item,result,wasDrop) =>{
				      fileDrop.cnt++;
				      let name = item.name;
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
    handleDropEvent:function(event,file, result,entryId,callback) {
	let isImage= file.type.match('^image.*');
	let url = ramaddaBaseUrl +"/entry/addfile";
	let wikiCallback = (html,status,xhr) =>{
	    console.log("ok");
	}
	let wikiError = (html,status,xhr) =>{
	    console.log("error");
	};

	let desc = "";
	let name = file.name;
	if(!name) {
	    name = prompt("Name:");
	    if(!name) return;
	}

	let fileName = file.name;
	let suffix = file.type.replace(/image\//,"");
	if(!fileName) {
	    fileName =  name+"." + suffix;
	}
	let data = new FormData();
	data.append("filename",fileName);
	data.append("filetype",file.type);
	data.append("group",entryId);
	data.append("description",desc);
	data.append("file", result);
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
		    alert("An error occurred creating file:"  + data.message);
		    return;
		}
		if(callback) callback(data,data.entryid, data.name,isImage);
	    },
	    error: function (err) {
		alert("An error occurred creating file:"  + err);
	    }
	});
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

    showEntryPopup:function(id,entryId,label) {
	let html = RamaddaUtils.contents[entryId];
	if(html) {
	    RamaddaUtils.showEntryPopupInner(id,entryId,label,html);
	} else {
	    let url = ramaddaBaseUrl +"/entry/menu?entryid=" + entryId;
            $.ajax({
                url: url,
                dataType: 'text',
                success: function(html) {
		    RamaddaUtils.contents[entryId] = html;
		    RamaddaUtils.showEntryPopupInner(id,entryId,label,html);
                }
            }).fail((jqxhr, settings, exc) => {
                console.log("/entry/menu failed:" + exc);
            });
	}
    },

    showEntryPopupInner:function(id,entryId,label,html) {
	let anchor = $("#" + id);
	HU.makeDialog({content:html,my:"left top",at:"left bottom",title:label,anchor:anchor,draggable:true,header:true,inPlace:false});    
    },

    mouseOverOnEntry: function(event, entryId, targetId) {
	if (Utils.mouseIsDown && Utils.entryDragInfo) {
	    if(Utils.entryDragInfo.hasEntry(entryId)) return;
            $("#" + targetId).css("borderBottom", "2px black solid");
	}
    },

    mouseOutOnEntry:function(event, entryId, targetId) {
	if(Utils.entryDragInfo) {
	    if(Utils.entryDragInfo.entryId == entryId) return;
	}
	$("#" + targetId).css("borderBottom","");
    },

    mouseDownOnEntry:function(event, entryId, name, sourceIconId, icon) {
	event = GuiUtils.getEvent(event);
	Utils.entryDragInfo = {
	    dragSource: sourceIconId,
	    getIds: function() {
		return this.entryIds.join(",");
	    },
	    getHtml:function() {
		let html = "";
		this.entries.forEach(entry=>{
		    if(html!="") html+="<br>";
		    html+=  HU.image(entry.args.icon) + SPACE +entry.args.name;
		});
		return html;
	    },
	    hasEntry:function(id) {
		return this.entryIds.includes(id);
	    },
	    addEntry:function(entry) {
		if(!this.hasEntry(entry.entryId)) {
		    this.entries.push(entry);
		    this.entryIds.push(entry.entryId);
		}
	    },
	    entryIds:[],
	    entries:[]
	}
	let entry = Utils.globalEntryRows[entryId];
	if(entry) {
	    Utils.entryDragInfo.addEntry(entry);
	}

	$(".ramadda-entry-select:checked").each(function() {
	    let id  =$(this).attr("id");
	    let entry = Utils.globalEntryRows[id];
	    if(entry) {
		Utils.entryDragInfo.addEntry(entry);
	    }
	});

	Utils.mouseIsDown = true;
	if (event.preventDefault) {
            event.preventDefault();
	} else {
            event.returnValue = false;
            return false;
	}
    },

    mouseUpOnEntry:function(event, entryId, targetId) {
	if(!Utils.entryDragInfo) return;
	if(Utils.entryDragInfo.hasEntry(entryId)) return;
	$("#"+ targetId).css("borderBottom","");
	if(!Utils.entryDragInfo.hasEntry(entryId)) {
            url = ramaddaBaseUrl + "/entry/copy?action=action.move&from=" + Utils.entryDragInfo.getIds() + "&to=" + entryId;
            document.location = url;
	}
    },
    initEntryListForm:function(formId) {
	let visibilityGroup = Utils.entryGroups[formId];
	if (visibilityGroup) {
            visibilityGroup.on = 0;
            visibilityGroup.setVisbility();
	}
    },

    hideEntryPopup:function() {
	HtmlUtils.getTooltip().hide();
    },

    findEntryRow:function(rowId) {
	let idx;
	for (idx = 0; idx < Utils.groupList.length; idx++) {
            let entryRow = Utils.groupList[idx].findEntryRow(rowId);
            if (entryRow) return entryRow;
	}
	return null;
    },

    entryRowOver:function(rowId) {
	let entryRow = Ramadda.findEntryRow(rowId);
	if (entryRow) entryRow.mouseOver();
    },

    entryRowOut:function(rowId) {
	let entryRow = Ramadda.findEntryRow(rowId);
	if (entryRow) entryRow.mouseOut();
    },

    entryRowClick:function(event, rowId) {
	let entryRow = Ramadda.findEntryRow(rowId);
	if (entryRow) entryRow.mouseClick(event);
    },


    originalImages:new Array(),
    changeImages: new Array(),

    folderClick:function(uid, url, changeImg) {
	Ramadda.changeImages[uid] = changeImg;
	let jqBlock = $("#" + uid);
	if (jqBlock.length == 0) {
            return;
	}
	let jqImage = $("#img_" + uid);
	let showing = jqBlock.css('display') != "none";
	if (!showing) {
            Ramadda.originalImages[uid] = jqImage.html();
            jqBlock.show();
            jqImage.html(HU.getIconImage("fa-caret-down"));
	    url +="&orderby=entryorder&ascending=true";
	    if(url.startsWith("/") && Ramadda.currentRamaddaBase) {
		url = Ramadda.currentRamaddaBase +url;
	    }

            GuiUtils.loadXML(url, Ramadda.handleFolderList, uid);
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
	
	if (Ramadda.changeImages[uid]) {
            $("#img_" + uid).attr('src', icon_folderopen);
	} else {
            $("#img_" + uid).attr('src', Ramadda.originalImages[uid]);
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
		if(layout=="grid") Ramadda.Components.layout(container,components,null);
		else  Ramadda.Components.layout(container,components,"component-" + layout);
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
		container.append(HU.div([ID,id,STYLE,HU.css("width","100%","height","500px")]));
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


function EntryFormList(formId, img, selectId, initialOn) {
    this.entryRows = new Array();
    this.lastEntryRowClicked = null;
    Utils.entryGroups[formId] = this;
    Utils.groupList.push(this);
    this.formId = formId;
    this.toggleImg = img;
    this.on = initialOn;
    this.entries = new Array();

    this.groupAddEntry = function(entryId) {
        this.entries[this.entries.length] = entryId;
    }

    this.addEntryRow = function(entryRow) {
        this.groupAddEntry(entryRow.cbxWrapperId);
        this.entryRows[this.entryRows.length] = entryRow;
        if (!this.on) {
            entryRow.getCbx().hide();
        } else {
            entryRow.getCbx().show();
        }
    }

    this.groupAddEntry(selectId);
    if (!this.on) {
        hideObject(selectId);
    }

    this.groupToggleVisibility = function() {
        this.on = !this.on;
	HU.addToDocumentUrl(this.formId+"_visible",this.on);
        this.setVisibility();
    }

    this.findEntryRow = function(rowId) {
        let i;
        for (i = 0; i < this.entryRows.length; i++) {
            if (this.entryRows[i].rowId == rowId) {
                return this.entryRows[i];
            }
        }
        return null;
    }

    this.checkboxClicked = function(event, cbxId) {
        if (!event) return;
        let entryRow;
        for (i = 0; i < this.entryRows.length; i++) {
            if (this.entryRows[i].cbxId == cbxId) {
                entryRow = this.entryRows[i];
                break;
            }
        }

        if (!entryRow) return;


        let value = entryRow.isSelected();

        if (event.ctrlKey) {
            for (i = 0; i < this.entryRows.length; i++) {
                this.entryRows[i].setCheckbox(value);
            }
        }

        if (event.shiftKey) {

            if (this.lastEntryRowClicked) {
                let pos1 = this.lastEntryRowClicked.getCbx().offset().top;
                let pos2 = entryRow.getCbx().offset().top;
                if (pos1 > pos2) {
                    let tmp = pos1;
                    pos1 = pos2;
                    pos2 = tmp;
                }

                for (i = 0; i < this.entryRows.length; i++) {
                    let top = this.entryRows[i].getCbx().offset().top;
                    if (top >= pos1 && top <= pos2) {
                        this.entryRows[i].setCheckbox(value);
                    }
                }
            }
            return;
        }
        this.lastEntryRowClicked = entryRow;
    }

    this.setVisibility = function() {
        if (this.toggleImg) {
            if (this.on) {
                $("#" + this.toggleImg).html(HU.getIconImage("fas fa-caret-down"));
            } else {
                $("#" + this.toggleImg).html(HU.getIconImage("fas fa-caret-right"));
            }
        }

        let form = $("#"+this.formId);
        if(this.on) {
            form.find(':checkbox').show();
        }   else {
            form.find(':checkbox').hide();
        }
        for (i = 0; i < this.entries.length; i++) {
            obj = GuiUtils.getDomObject(this.entries[i]);
            if (!obj) continue;
            if (this.on) {
                showObject(obj, "block");
            } else {
                hideObject(obj);
            }
        }
    }

    let fromUrl = HU.getUrlArgument(this.formId+"_visible");
    if(fromUrl!==null)  {
        this.on = fromUrl =="true";
	this.setVisibility();
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
        let url = ramaddaBaseUrl + "/entry/show?entryid=" + entryId + "&output=metadataxml";
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






let selectors = new Array();

function Selector(event, selectorId, elementId, allEntries, selecttype, localeId, entryType, ramaddaUrl) {
    this.id = selectorId;
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

    this.clearInput = function() {
        this.getHiddenComponent().val("");
        this.getTextComponent().val("");
    }


    this.handleClick = function(event) {
        let srcId = this.id + '_selectlink';
        let src = null;
	if(event.target) {
	    src = $(event.target);
	}
	if(src==null)
	    src = $("#" + srcId);
	if(src.length==0) {
	    src = $("#" + this.id);
	}
        HtmlUtils.hidePopupObject(event);
        this.div = GuiUtils.getDomObject('ramadda-selectdiv');
        let selectDiv = $("#ramadda-selectdiv");
        selectDiv.show();
        selectDiv.position({
            of: src,
            my: "left top",
            at: "left bottom",
            collision: "none none"
        });

        let url =  "/entry/show?output=selectxml&selecttype=" + this.selecttype + "&allentries=" + this.allEntries + "&target=" + this.id + "&noredirect=true&firstclick=true";
	if(this.ramaddaUrl && !this.ramaddaUrl.startsWith("/")) {
	    let pathname = new URL(this.ramaddaUrl).pathname
	    let root = this.ramaddaUrl.replace(pathname,"");
	    Ramadda.currentRamaddaBase = root;
            url = this.ramaddaUrl + url;
	} else {
	    Ramadda.currentRamaddaBase = null;
	    url = ramaddaBaseUrl+url;
	}
        if (this.localeId) {
            url = url + "&localeid=" + this.localeId;
        }
        if (this.entryType) {
            url = url + "&entrytype=" + this.entryType;
        }
        GuiUtils.loadXML(url, handleSelect, this.id);
        return false;
    }
    this.handleClick(event);
}


let EntryTree = {
    initSelectAll:function() {
	$("#selectall").click(function(event) {
	    let value = $(this).is(":checked");
	    $(".ramadda-entry-select").each(function(){
		let entryRow = Utils.globalEntryRows[$(this).attr("id")];
		if(entryRow) {
		    entryRow.setCheckbox(value);
		}
	    });
	});
    },
    entryRowCheckboxClicked:function(event, cbxId) {
	let cbx = GuiUtils.getDomObject(cbxId);
	if (!cbx) return;
	cbx = cbx.obj;
	if (!cbx.form) return;
	let visibilityGroup = Utils.entryGroups[cbx.form.id];
	if (visibilityGroup) {
            visibilityGroup.checkboxClicked(event, cbxId);
	}
    }

}



function selectClick(id, entryId, value, opts) {
    selector = selectors[id];
    let handler = getHandler(id);
    if(!handler) handler = getHandler(selector.elementId);
    if (handler) {
        handler.selectClick(selector.selecttype, id, entryId, value);
        selectCancel();
	console.log("no handler");
        return;
    }

    if (selector.selecttype == "wikilink") {
	let args = {entryId: entryId,name:value};
	if(opts) $.extend(args, opts);
        insertAtCursor(selector.elementId, selector.textComp.obj,args);
    } else   if (selector.selecttype == "fieldname") {
        insertAtCursor(selector.elementId, selector.textComp.obj,  value);
    } else   if (selector.selecttype == "image") {
        insertAtCursor(selector.elementId, selector.textComp.obj,  "{{image entry=\"" + entryId +"\" caption=\"" + value+"\" width=400px align=center}} ");	
    } else if (selector.selecttype == "entryid") {
        //        insertTagsInner(selector.elementId, selector.textComp.obj, "" +entryId+"|"+value+" "," ","importtype");
	let editor = HU.getWikiEditor(selector.elementId);
	if(editor) {
	    editor.insertTags(entryId, " ", "importtype");
	} else {
	    insertText(selector.elementId,entryId);
	}
    } else if (selector.selecttype == "entry:entryid") {
        //        insertTagsInner(selector.elementId, selector.textComp.obj, "" +entryId+"|"+value+" "," ","importtype");
        HU.getWikiEditor(selector.elementId).insertTags("entry:" + entryId, " ", "importtype");
    } else {
        selector.getHiddenComponent().val(entryId);
        selector.getTextComponent().val(value);
    }
    selectCancel();
}

function selectCancel(override) {
    if(!override) {
	if($("#ramadda-selectdiv-pin").attr("data-pinned")) return;
    }
    $("#ramadda-selectdiv").hide();
}


function selectCreate(event, selectorId, elementId, allEntries, selecttype, localeId, entryType, baseUrl) {
    let key = selectorId + (baseUrl||"");
    if (!selectors[key]) {
        selectors[selectorId] = selectors[key] = new Selector(event, selectorId, elementId, allEntries, selecttype, localeId, entryType,baseUrl);
    } else {
        //Don:  alert('have selector'):
        selectors[key].handleClick(event);
    }
}


function selectInitialClick(event, selectorId, elementId, allEntries, selecttype, localeId, entryType,baseUrl) {
    selectCreate(event, selectorId, elementId, allEntries, selecttype, localeId, entryType,baseUrl);
    return false;
}


function clearSelect(id) {
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
}


function handleSelect(request, id) {
    selector = selectors[id];
    let xmlDoc = request.responseXML.documentElement;
    text = getChildText(xmlDoc);
    let pinId = selector.div.id +"-pin";
    let pin = HU.jsLink("",HtmlUtils.getIconImage(icon_pin), ["class","ramadda-popup-pin", "id",pinId]); 
    let closeImage = HtmlUtils.getIconImage(icon_close, []);
    let close = "<a href=\"javascript:selectCancel(true);\">" + closeImage+"</a>";
    let header = HtmlUtils.div(["style","text-align:left;","class","ramadda-popup-header"],SPACE+close+SPACE+pin);
    let popup = HtmlUtils.div(["id",id+"-popup"], header + text);
    selector.div.obj.innerHTML = popup;
    $("#" + selector.div.id).draggable();
    $("#" + pinId).click(function() {
	if($(this).attr("data-pinned")) {
	    $(this).removeClass("ramadda-popup-pin-pinned");
	    $(this).attr("data-pinned",false);
	} else {
	    $(this).addClass("ramadda-popup-pin-pinned");
	    $(this).attr("data-pinned",true);
	}
    });
    let arr = selector.div.obj.getElementsByTagName('script')
    //Eval any embedded js
    for (let n = 0; n < arr.length; n++) {
	eval(arr[n].innerHTML);
    }

}


function getChildText(node) {
    let text = '';
    for (childIdx = 0; childIdx < node.childNodes.length; childIdx++) {
        text = text + node.childNodes[childIdx].nodeValue;
    }
    return text;

}


function toggleBlockVisibility(id, imgid, showimg, hideimg) {
    if (toggleVisibility(id, 'block')) {
        if(HU.isFontAwesome(showimg)) {
            $("#" + imgid).html(HU.makeToggleImage(showimg));
        } else {
            $("#" + imgid).attr('src', showimg);
        }
    } else {
        if(HU.isFontAwesome(showimg)) {
            $("#" + imgid).html(HU.makeToggleImage(hideimg));
        } else {
            $("#" + imgid).attr('src', hideimg);
        }
    }
    Utils.ramaddaUpdateMaps();
}


function toggleInlineVisibility(id, imgid, showimg, hideimg) {
    let img = GuiUtils.getDomObject(imgid);
    let icon;
    if (toggleVisibility(id, 'inline')) {
        icon= showimg;
    } else {
        icon = hideimg;
    }
    if(StringUtil.startsWith(icon,"fa-")) {
        $("#" + imgid).html(HtmlUtils.getIconImage(icon,[]));
    } else {
        if(img) img.obj.src = icon;
    }
    Utils.ramaddaUpdateMaps();
}


function toggleVisibility(id, style) {
    let display = $("#" + id).css('display');
    $("#" + id).toggle();
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







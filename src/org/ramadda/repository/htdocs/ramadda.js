/**
 * Copyright (c) 2008-2021 Geode Systems LLC
 */

var currentRamaddaBase;


function mouseOverOnEntry(event, entryId, targetId) {
    if (Utils.mouseIsDown && Utils.entryDragInfo) {
	if(Utils.entryDragInfo.hasEntry(entryId)) return;
        $("#" + targetId).css("borderBottom", "2px black solid");
    }
}

function mouseOutOnEntry(event, entryId, targetId) {
    if(Utils.entryDragInfo) {
	if(Utils.entryDragInfo.entryId == entryId) return;
    }
    $("#" + targetId).css("borderBottom","");
}


function mouseDownOnEntry(event, entryId, name, sourceIconId, icon) {
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
}


function mouseUpOnEntry(event, entryId, targetId) {
    if(!Utils.entryDragInfo) return;
    if(Utils.entryDragInfo.hasEntry(entryId)) return;
    $("#"+ targetId).css("borderBottom","");
    if(!Utils.entryDragInfo.hasEntry(entryId)) {
        url = ramaddaBaseUrl + "/entry/copy?action=action.move&from=" + Utils.entryDragInfo.getIds() + "&to=" + entryId;
        document.location = url;
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
}





function initEntryListForm(formId) {
    let visibilityGroup = Utils.entryGroups[formId];
    if (visibilityGroup) {
        visibilityGroup.on = 0;
        visibilityGroup.setVisbility();
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
        let close = HU.jsLink("", HU.getIconImage(icon_close), ["onmousedown", "hideEntryPopup();","id","tooltipclose"]);
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





function hideEntryPopup() {
    HtmlUtils.getTooltip().hide();
}

function findEntryRow(rowId) {
    let idx;
    for (idx = 0; idx < Utils.groupList.length; idx++) {
        let entryRow = Utils.groupList[idx].findEntryRow(rowId);
        if (entryRow) return entryRow;
    }
    return null;
}


function entryRowOver(rowId) {
    let entryRow = findEntryRow(rowId);
    if (entryRow) entryRow.mouseOver();
}


function entryRowOut(rowId) {
    let entryRow = findEntryRow(rowId);
    if (entryRow) entryRow.mouseOut();
}

function entryRowClick(event, rowId) {
    let entryRow = findEntryRow(rowId);
    if (entryRow) entryRow.mouseClick(event);
}

var lastCbxClicked;
var lastCbxIdClicked;


function checkboxClicked(event, cbxPrefix, id) {
    if (!event) return;
    let cbx = GuiUtils.getDomObject(id);
    if (!cbx) return;
    cbx = cbx.obj;
    let checkBoxes = new Array();
    if (!cbx.form) return;
    let elements = cbx.form.elements;
    for (i = 0; i < elements.length; i++) {
        if (elements[i].name.indexOf(cbxPrefix) >= 0 || elements[i].id.indexOf(cbxPrefix) >= 0) {
            checkBoxes.push(elements[i]);
        }
    }


    let value = cbx.checked;
    if (event.ctrlKey) {
        for (i = 0; i < checkBoxes.length; i++) {
            checkBoxes[i].checked = value;
        }
    }


    if (event.shiftKey) {
        if (lastCbxClicked) {
            let pos1 = GuiUtils.getTop(cbx);
            let pos2 = GuiUtils.getTop(lastCbxClicked);

            let lastCbx = $("#" + lastCbxIdClicked);
            let thisCbx = $("#" + id);

            if (lastCbx.position()) {
                pos2 = lastCbx.position().top;
            }
            if (thisCbx.position()) {
                pos1 = thisCbx.position().top;
            }

            if (pos1 > pos2) {
                let tmp = pos1;
                pos1 = pos2;
                pos2 = tmp;
            }
            for (i = 0; i < checkBoxes.length; i++) {
                let top = $("#" + checkBoxes[i].id).position().top;
                if (top >= pos1 && top <= pos2) {
                    checkBoxes[i].checked = value;
                }
            }
        }
        return;
    }
    lastCbxClicked = cbx;
    lastCbxIdClicked = id;
}



function toggleBlockVisibility(id, imgid, showimg, hideimg) {
    if (toggleVisibility(id, 'block')) {
        if(HU.isFontAwesome(showimg)) {
            $("#" + imgid).html(HtmlUtils.getIconImage(showimg,[]));
        } else {
            $("#" + imgid).attr('src', showimg);
        }
    } else {
        if(HU.isFontAwesome(showimg)) {
            $("#" + imgid).html(HtmlUtils.getIconImage(hideimg,[]));
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


let originalImages = new Array();
let changeImages = new Array();

function folderClick(uid, url, changeImg) {
    changeImages[uid] = changeImg;
    let jqBlock = $("#" + uid);
    if (jqBlock.length == 0) {
        return;
    }
    let jqImage = $("#img_" + uid);
    let showing = jqBlock.css('display') != "none";
    if (!showing) {
        originalImages[uid] = jqImage.html();
        jqBlock.show();
        jqImage.html(HU.getIconImage("fa-caret-down"));
	url +="&orderby=entryorder&ascending=true";
	if(url.startsWith("/") && currentRamaddaBase) {
	    url = currentRamaddaBase +url;
	}



        GuiUtils.loadXML(url, handleFolderList, uid);
    } else {
        if (changeImg) {
            jqImage.html(HU.getIconImage("fa-caret-right"));
	}
        jqBlock.hide();
    }
}


function handleFolderList(request, uid) {
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
    
    if (changeImages[uid]) {
        $("#img_" + uid).attr('src', icon_folderopen);
    } else {
        $("#img_" + uid).attr('src', originalImages[uid]);
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
        let src = $("#" + srcId);
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
	    currentRamaddaBase = root;
            url = this.ramaddaUrl + url;
	} else {
	    currentRamaddaBase = null;
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

function selectClick(id, entryId, value) {
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
        insertAtCursor(selector.elementId, selector.textComp.obj, "[[" + entryId + "|" + value + "]]");
    } else   if (selector.selecttype == "fieldname") {
        insertAtCursor(selector.elementId, selector.textComp.obj,  value);
    } else if (selector.selecttype == "entryid") {
        //        insertTagsInner(selector.elementId, selector.textComp.obj, "" +entryId+"|"+value+" "," ","importtype");
	let editor = HU.getWikiEditor(selector.elementId);
	if(editor) {
	    insertTags(entryId, " ", "importtype");
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


function toggleVisibility(id, style) {
    let display = $("#" + id).css('display');
    $("#" + id).toggle();
    return display != 'block';
}

function hide(id) {
    $("#" + id).hide();
}



function showStickyPopup(event, srcId, popupId, alignLeft) {
    let myalign = 'left top';
    let atalign = 'left top';
    $("#" + popupId).show("slow");
    $("#" + popupId).position({
        of: jQuery("#" + srcId),
        my: myalign,
        at: atalign,
        collision: "none none"
    });
    //Do it again to fix a bug on safari
    $("#" + popupId).position({
        of: jQuery("#" + srcId),
        my: myalign,
        at: atalign,
        collision: "none none"
    });
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

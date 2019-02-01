/**
 * Copyright (c) 2008-2015 Geode Systems LLC
 */


//
//This is the remnant of the original (and pretty crappy) js
//lots of globals, not much jq
//

var popupObject;
var popupSrcId;
var popupTime;

document.onmousemove = mouseMove;
document.onmousedown = mouseDown;
document.onmouseup = mouseUp;


var mouseIsDown = 0;
var dragSource;
var draggedEntry;
var draggedEntryName;
var draggedEntryIcon;
var mouseMoveCnt = 0;
var objectToHide;

function hidePopupObject() {
    if (objectToHide != popupObject) {
        //	return;
    }
    if (popupObject) {
        hideObject(popupObject);
        popupObject = null;
        popupSrcId = null;
    }
}



function mouseDown(event) {
    if (popupObject) {
        if (checkToHidePopup()) {
            theObjectToHide = popupObject;
            thePopupSrcId = popupSrcId;
            var callback = function() {
                var shouldClear = (popupObject == theObjectToHide);
                hideObject(theObjectToHide);
                if (shouldClear) {
                    popupSrcId = null;
                    popupObject = null;
                }
            }
            setTimeout(callback, 250);
        }
    }
    event = GuiUtils.getEvent(event);
    mouseIsDown = 1;
    mouseMoveCnt = 0;
    return true;
}



function mouseUp(event) {
    event = GuiUtils.getEvent(event);
    mouseIsDown = 0;
    draggedEntry = null;
    GuiUtils.setCursor('default');
    var obj = GuiUtils.getDomObject('ramadda-floatdiv');
    if (obj) {
        var dragSourceObj = GuiUtils.getDomObject(dragSource);
        if (dragSourceObj) {
            var tox = GuiUtils.getLeft(dragSourceObj.obj);
            var toy = GuiUtils.getTop(dragSourceObj.obj);
            var fromx = parseInt(obj.style.left);
            var fromy = parseInt(obj.style.top);
            var steps = 10;
            var dx = (tox - fromx) / steps;
            var dy = (toy - fromy) / steps;
            flyBackAndHide('ramadda-floatdiv', 0, steps, fromx, fromy, dx, dy);
        } else {
            hideObject(obj);
        }
    }
    return true;
}




function flyBackAndHide(id, step, steps, fromx, fromy, dx, dy) {
    var obj = GuiUtils.getDomObject(id);
    if (!obj) {
        return;
    }
    step = step + 1;
    obj.style.left = fromx + dx * step + "px";
    obj.style.top = fromy + dy * step + "px";
    var opacity = 80 * (steps - step) / steps;
    //    GuiUtils.print(opacity);
    //    obj.style.filter="alpha(opacity="+opacity+")";
    //    obj.style.opacity="0." + opacity;

    if (step < steps) {
        var callback = "flyBackAndHide('" + id + "'," + step + "," + steps + "," + fromx + "," + fromy + "," + dx + "," + dy + ");"
        setTimeout(callback, 30);
    } else {
        setTimeout("finalHide('" + id + "')", 150);
        //        hideObject(obj);
    }
}

function finalHide(id) {
    var obj = GuiUtils.getDomObject(id);
    if (!obj) {
        return;
    }
    hideObject(obj);
    obj.style.filter = "alpha(opacity=80)";
    obj.style.opacity = "0.8";
}

function mouseMove(event) {
    event = GuiUtils.getEvent(event);
    if (draggedEntry && mouseIsDown) {
        mouseMoveCnt++;
        var obj = GuiUtils.getDomObject('ramadda-floatdiv');
        if (mouseMoveCnt == 6) {
            GuiUtils.setCursor('move');
        }
        if (mouseMoveCnt >= 6 && obj) {
            moveFloatDiv(GuiUtils.getEventX(event), GuiUtils.getEventY(event));
        }
    }
    return false;
}



function moveFloatDiv(x, y) {
    var obj = GuiUtils.getDomObject('ramadda-floatdiv');
    if (obj) {
        if (obj.style.visibility != "visible") {
            obj.style.visibility = "visible";
            obj.style.display = "block";
            var icon = "";
            if (draggedEntryIcon) {
                icon = "<img src=\"" + draggedEntryIcon + "\"/> ";
            }
            obj.obj.innerHTML = icon + draggedEntryName + "<br>Drag to a group to copy/move/associate";
        }
        obj.style.top = y;
        obj.style.left = x + 10;
    }
}


function mouseOverOnEntry(event, entryId, targetId) {
    event = GuiUtils.getEvent(event);
    if (entryId == draggedEntry) return;
    if (mouseIsDown) {
        var obj = GuiUtils.getDomObject(targetId);
        if (!obj) return;
        //       if(obj.style && obj.style.borderBottom) {
        obj.style.borderBottom = "2px black solid";
        //        }
    }
}

function mouseOutOnEntry(event, entryId, targetId) {
    event = GuiUtils.getEvent(event);
    if (entryId == draggedEntry) return;
    var obj = GuiUtils.getDomObject(targetId);
    if (!obj) return;
    if (mouseIsDown) {
        obj.style.borderBottom = "";
    }
}




function mouseDownOnEntry(event, entryId, name, sourceIconId, icon) {
    event = GuiUtils.getEvent(event);
    dragSource = sourceIconId;
    draggedEntry = entryId;
    draggedEntryName = name;
    draggedEntryIcon = icon;
    mouseIsDown = 1;
    if (event.preventDefault) {
        event.preventDefault();
    } else {
        event.returnValue = false;
        return false;
    }
}


function mouseUpOnEntry(event, entryId, targetId) {
    event = GuiUtils.getEvent(event);
    if (entryId == draggedEntry) {
        return;
    }
    var obj = GuiUtils.getDomObject(targetId);
    if (!obj) {
        return;
    }
    if (mouseIsDown) {
        obj.style.borderBottom = "";
    }
    if (draggedEntry && draggedEntry != entryId) {
        /*
        $("#ramadda-dialog").html("what to do....");
        $("#ramadda-dialog").dialog({
                resizable: false,
                modal: true,
                buttons: {
                   Cancel: function() {$( this ).dialog( "close" );}
                }}
        );
        */
        url = ramaddaBaseUrl + "/entry/copy?action=action.move&from=" + draggedEntry + "&to=" + entryId;
        //        alert(url);
        //        window.open(url,'move window','') ;
        document.location = url;
    }
}


function getTooltip() {
    return $("#ramadda-tooltipdiv");
}

function handleKeyPress(event) {
    getTooltip().hide();
}


document.onkeypress = handleKeyPress;

var groups = new Array();
var groupList = new Array();




function EntryFormList(formId, img, selectId, initialOn) {

    this.entryRows = new Array();
    this.lastEntryRowClicked = null;
    groups[formId] = this;
    groupList[groupList.length] = this;
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
        var i;
        for (i = 0; i < this.entryRows.length; i++) {
            if (this.entryRows[i].rowId == rowId) {
                return this.entryRows[i];
            }
        }
        return null;
    }



    this.checkboxClicked = function(event, cbxId) {
        if (!event) return;
        var entryRow;
        for (i = 0; i < this.entryRows.length; i++) {
            if (this.entryRows[i].cbxId == cbxId) {
                entryRow = this.entryRows[i];
                break;
            }
        }

        if (!entryRow) return;


        var value = entryRow.isSelected();

        if (event.ctrlKey) {
            for (i = 0; i < this.entryRows.length; i++) {
                this.entryRows[i].setCheckbox(value);
            }
        }

        if (event.shiftKey) {

            if (this.lastEntryRowClicked) {
                var pos1 = this.lastEntryRowClicked.getCbx().offset().top;
                var pos2 = entryRow.getCbx().offset().top;
                if (pos1 > pos2) {
                    var tmp = pos1;
                    pos1 = pos2;
                    pos2 = tmp;
                }

                for (i = 0; i < this.entryRows.length; i++) {
                    var top = this.entryRows[i].getCbx().offset().top;
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
                $("#" + this.toggleImg).attr('src', icon_downdart);
            } else {
                $("#" + this.toggleImg).attr('src', icon_rightdart);
            }
        }

        var form = GuiUtils.getDomObject(this.formId);
        if (form) {
            form = form.obj;
            for (i = 0; i < form.elements.length; i++) {
                if (this.on) {
                    showObject(form.elements[i], "inline");
                } else {
                    hideObject(form.elements[i]);
                }
            }
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


function entryRowCheckboxClicked(event, cbxId) {

    var cbx = GuiUtils.getDomObject(cbxId);
    if (!cbx) return;
    cbx = cbx.obj;
    if (!cbx.form) return;
    var visibilityGroup = groups[cbx.form.id];

    if (visibilityGroup) {
        visibilityGroup.checkboxClicked(event, cbxId);
    }
}

function initEntryListForm(formId) {
    var visibilityGroup = groups[formId];
    if (visibilityGroup) {
        visibilityGroup.on = 0;
        visibilityGroup.setVisbility();
    }
}


function EntryRow(entryId, rowId, cbxId, cbxWrapperId, showDetails) {
    this.entryId = entryId;

    this.onColor = "#FFFFCC";
    this.overColor = "#f6f6f6";
    this.overColor = "#edf5ff";
    this.overColor = "#ffffee";
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

    var form = this.getCbx().closest('form');
    if (form.size()) {
        var visibilityGroup = groups[form.attr('id')];
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
        if (this.isSelected()) {
            this.getRow().css("background-color", this.onColor);
        } else {
            this.getRow().css("background-color", "#ffffff");
        }
    }


    this.mouseOver = function(event) {
        $("#" + "entrymenuarrow_" + rowId).attr('src', icon_menuarrow);
        this.getRow().css('background-color', this.overColor);
    }

    this.mouseOut = function(event) {
        $("#entrymenuarrow_" + rowId).attr('src', icon_blank);
        this.setRowColor();
    }


    this.mouseClick = function(event) {
        eventX = GuiUtils.getEventX(event);
        var position = this.getRow().offset();
        //Don't pick up clicks on the left side
        if (eventX - position.left < 150) return;
        this.lastClick = eventX;
        var url = ramaddaBaseUrl + "/entry/show?entryid=" + entryId + "&output=metadataxml";
        if (this.showDetails) {
            url += "&details=true";
        } else {
            url += "&details=false";
        }
        GuiUtils.loadXML(url, this.handleTooltip, this);
    }

    this.handleTooltip = function(request, entryRow) {
        var xmlDoc = request.responseXML.documentElement;
        text = getChildText(xmlDoc);
        var leftSide = entryRow.getRow().offset().left;
        var offset = entryRow.lastClick - leftSide;
        getTooltip().html("<div class=ramadda-tooltip-inner><div id=\"tooltipwrapper\" ><table><tr valign=top><img width=\"16\" class=\"ramadda-popup-close\" onmousedown=\"hideEntryPopup();\" id=\"tooltipclose\"  src=" + icon_close + "></td><td>" + text + "</table></div></div>");
        checkTabs(text);

        var pos = entryRow.getRow().offset();
        var eWidth = entryRow.getRow().outerWidth();
        var eHeight = entryRow.getRow().outerHeight();
        var mWidth = getTooltip().outerWidth();
        var wWidth = $(window).width();

        var x = entryRow.lastClick;

        if (entryRow.lastClick + mWidth > wWidth) {
            x -= (entryRow.lastClick + mWidth - wWidth);
        }
        var left = x + "px";
        var top = (3 + pos.top + eHeight) + "px";

        getTooltip().css({
            position: 'absolute',
            zIndex: 5000,
            left: left,
            top: top
        });
        getTooltip().show();
    }



}


function checkTabs(html) {
    while (1) {
        var re = new RegExp("id=\"(tabId[^\"]+)\"");
        var m = re.exec(html);
        if (!m) {
            break;
        }
        var s = m[1];
        if (s.indexOf("-") < 0) {
            jQuery(function() {
                jQuery('#' + s).tabs();
            });
        }
        var idx = html.indexOf("id=\"tabId");
        if (idx < 0) {
            break;
        }
        html = html.substring(idx + 20);
    }
}


function hideEntryPopup() {
    getTooltip().hide();
}

function findEntryRow(rowId) {
    var idx;
    for (idx = 0; idx < groupList.length; idx++) {
        var entryRow = groupList[idx].findEntryRow(rowId);
        if (entryRow) return entryRow;
    }
    return null;
}


function entryRowOver(rowId) {
    var entryRow = findEntryRow(rowId);
    if (entryRow) entryRow.mouseOver();
}


function entryRowOut(rowId) {
    var entryRow = findEntryRow(rowId);
    if (entryRow) entryRow.mouseOut();
}

function entryRowClick(event, rowId) {
    var entryRow = findEntryRow(rowId);
    if (entryRow) entryRow.mouseClick(event);
}





function indexOf(array, object) {
    for (i = 0; i <= array.length; i++) {
        if (array[i] == object) return i;
    }
    return -1;
}


var lastCbxClicked;
var lastCbxIdClicked;

function checkboxClicked(event, cbxPrefix, id) {
    if (!event) return;
    var cbx = GuiUtils.getDomObject(id);
    if (!cbx) return;
    cbx = cbx.obj;
    var checkBoxes = new Array();
    if (!cbx.form) return;
    var elements = cbx.form.elements;
    for (i = 0; i < elements.length; i++) {
        if (elements[i].name.indexOf(cbxPrefix) >= 0 || elements[i].id.indexOf(cbxPrefix) >= 0) {
            checkBoxes.push(elements[i]);
        }
    }


    var value = cbx.checked;
    if (event.ctrlKey) {
        for (i = 0; i < checkBoxes.length; i++) {
            checkBoxes[i].checked = value;
        }
    }


    if (event.shiftKey) {
        if (lastCbxClicked) {
            var pos1 = GuiUtils.getTop(cbx);
            var pos2 = GuiUtils.getTop(lastCbxClicked);

            var lastCbx = $("#" + lastCbxIdClicked);
            var thisCbx = $("#" + id);

            if (lastCbx.position()) {
                pos2 = lastCbx.position().top;
            }
            if (thisCbx.position()) {
                pos1 = thisCbx.position().top;
            }

            if (pos1 > pos2) {
                var tmp = pos1;
                pos1 = pos2;
                pos2 = tmp;
            }
            for (i = 0; i < checkBoxes.length; i++) {
                var top = $("#" + checkBoxes[i].id).position().top;
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
        $("#" + imgid).attr('src', showimg);
    } else {
        $("#" + imgid).attr('src', hideimg);
    }
    ramaddaUpdateMaps();
}


function toggleInlineVisibility(id, imgid, showimg, hideimg) {
    var img = GuiUtils.getDomObject(imgid);
    if (toggleVisibility(id, 'inline')) {
        if (img) img.obj.src = showimg;
    } else {
        if (img) img.obj.src = hideimg;
    }
    ramaddaUpdateMaps();
}







var originalImages = new Array();
var changeImages = new Array();

function folderClick(uid, url, changeImg) {
    changeImages[uid] = changeImg;
    var jqBlock = $("#" + uid);
    if (jqBlock.size() == 0) {
        return;
    }
    var jqImage = $("#img_" + uid);
    var showing = jqBlock.css('display') != "none";
    if (!showing) {
        originalImages[uid] = jqImage.attr('src');
        jqBlock.show();
        jqImage.attr('src', icon_progress);
        GuiUtils.loadXML(url, handleFolderList, uid);
    } else {
        if (changeImg) {
            if (originalImages[uid]) {
                jqImage.attr('src', originalImages[uid]);
            } else
                jqImage.attr('src', icon_folderclosed);
        }
        jqBlock.hide();
    }
}



function handleFolderList(request, uid) {
    if (request.responseXML != null) {
        var xmlDoc = request.responseXML.documentElement;
        var script;
        var html;
        for (i = 0; i < xmlDoc.childNodes.length; i++) {
            var childNode = xmlDoc.childNodes[i];
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
            checkTabs(html);
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


var selectors = new Array();

function Selector(event, selectorId, elementId, allEntries, selecttype, localeId, entryType) {
    this.id = selectorId;
    this.elementId = elementId;
    this.localeId = localeId;
    this.entryType = entryType;
    this.allEntries = allEntries;
    this.selecttype = selecttype;
    this.textComp = GuiUtils.getDomObject(this.elementId);

    this.getTextComponent = function() {
        var id = "#" + this.elementId;
        return $(id);
    }

    this.getHiddenComponent = function() {
        var id = "#" + this.elementId + "_hidden";
        return $(id);
    }

    this.clearInput = function() {
        this.getHiddenComponent().val("");
        this.getTextComponent().val("");
    }


    this.handleClick = function(event) {
        var srcId = this.id + '_selectlink';
        this.div = GuiUtils.getDomObject('ramadda-selectdiv');
        hidePopupObject();
        var selectDiv = $("#ramadda-selectdiv");
        selectDiv.show();
        var src = $("#" + srcId);
        selectDiv.position({
            of: src,
            my: "left top",
            at: "left bottom",
            collision: "none none"
        });
        url = ramaddaBaseUrl + "/entry/show?output=selectxml&selecttype=" + this.selecttype + "&allentries=" + this.allEntries + "&target=" + this.id + "&noredirect=true&firstclick=true";
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
    if (selector.selecttype == "wikilink") {
        insertAtCursor(selector.elementId, selector.textComp.obj, "[[" + entryId + "|" + value + "]]");
    } else if (selector.selecttype == "entryid") {
        //        insertTagsInner(selector.elementId, selector.textComp.obj, "" +entryId+"|"+value+" "," ","importtype");
        insertTagsInner(selector.elementId, selector.textComp.obj, entryId, " ", "importtype");
    } else if (selector.selecttype == "entry:entryid") {
        //        insertTagsInner(selector.elementId, selector.textComp.obj, "" +entryId+"|"+value+" "," ","importtype");
        insertTagsInner(selector.elementId, selector.textComp.obj, "entry:" + entryId, " ", "importtype");
    } else {
        selector.getHiddenComponent().val(entryId);
        selector.getTextComponent().val(value);
    }
    selectCancel();
}

function selectCancel() {
    $("#ramadda-selectdiv").hide();
}


function selectCreate(event, selectorId, elementId, allEntries, selecttype, localeId, entryType) {
    if (!selectors[selectorId]) {
        selectors[selectorId] = new Selector(event, selectorId, elementId, allEntries, selecttype, localeId, entryType);
    } else {
        //Don:  alert('have selector'):
        selectors[selectorId].handleClick(event);
    }
}


function selectInitialClick(event, selectorId, elementId, allEntries, selecttype, localeId, entryType) {
    selectCreate(event, selectorId, elementId, allEntries, selecttype, localeId, entryType);
    return false;
}


function clearSelect(id) {
    selector = selectors[id];

    if (selector) {
        selector.clearInput();
    } else {
        //        console.log("No selector");
        //In case the user never clicked select
        var textComp = GuiUtils.getDomObject(id);
        var hiddenComp = GuiUtils.getDomObject(id + "_hidden");
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
    var xmlDoc = request.responseXML.documentElement;
    text = getChildText(xmlDoc);
    var close = "<a href=\"javascript:selectCancel();\"><img class=\"ramadda-popup-close\" border=0 src=" + icon_close + "></a>";
    selector.div.obj.innerHTML = "<table width=100%><tr><td align=right>" + close + "</table>" + text;
}




function getChildText(node) {
    var text = '';
    for (childIdx = 0; childIdx < node.childNodes.length; childIdx++) {
        text = text + node.childNodes[childIdx].nodeValue;
    }
    return text;

}


function toggleVisibility(id, style) {
    var display = $("#" + id).css('display');
    $("#" + id).toggle();
    return display != 'block';
}


function hide(id) {
    $("#" + id).hide();
    //    hideElementById(id);
}

function hideElementById(id) {
    hideObject(GuiUtils.getDomObject(id));
}

function checkToHidePopup() {
    if (popupTime) {
        var now = new Date();
        timeDiff = now - popupTime;
        if (timeDiff > 1000) {
            return 1;
        }
    }
}

function showPopup(event, srcId, popupId, alignLeft, myalign, atalign) {
    if (popupSrcId == srcId) {
        if (checkToHidePopup()) {
            hidePopupObject();
            return;
        }
    }

    popupTime = new Date();
    hidePopupObject();
    var popup = GuiUtils.getDomObject(popupId);
    var srcObj = GuiUtils.getDomObject(srcId);
    if (!popup || !srcObj) return;
    popupObject = popup;
    popupSrcId = srcId;

    if (!myalign)
        myalign = 'left top';
    if (!atalign)
        atalign = 'left bottom';
    if (alignLeft) {
        myalign = 'right top';
        atalign = 'left bottom';
    }
    showObject(popup);
    jQuery("#" + popupId).position({
        of: jQuery("#" + srcId),
        my: myalign,
        at: atalign,
        collision: "none none"
    });
    //Do it again to fix a bug on safari
    jQuery("#" + popupId).position({
        of: jQuery("#" + srcId),
        my: myalign,
        at: atalign,
        collision: "none none"
    });
}


function showStickyPopup(event, srcId, popupId, alignLeft) {
    var myalign = 'left top';
    var atalign = 'left top';


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


function hideMore(base) {
    var link = GuiUtils.getDomObject("morelink_" + base);
    var div = GuiUtils.getDomObject("morediv_" + base);
    hideObject(div);
    showObject(link);
}


function showMore(base) {
    var link = GuiUtils.getDomObject("morelink_" + base);
    var div = GuiUtils.getDomObject("morediv_" + base);
    hideObject(link);
    showObject(div);
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






//This gets called from toggleBlockVisibility
//It updates any map on the page to fix some sort of offset problem
function ramaddaUpdateMaps() {
    if (!(typeof ramaddaMaps === 'undefined')) {
        for (i = 0; i < ramaddaMaps.length; i++) {
            var ramaddaMap = ramaddaMaps[i];
            if (!ramaddaMap.map) continue;
            ramaddaMap.map.updateSize();
        }
    }
}




var formDialogId;

function closeFormLoadingDialog() {
    var dialog = $(formDialogId);
    dialog.dialog('close');
}





function popupFormLoadingDialog(dialogId) {
    formDialogId = dialogId;
    var dialog = $(dialogId);
    dialog.dialog({
        resizable: false,
        height: 100,
        modal: true
    });
}


function submitEntryForm(dialogId) {
    popupFormLoadingDialog(dialogId);
    return true;
}


function treeViewClick(entryId, url, label) {
    var href = "<a href='" + url + "'> <img src=\"" + ramaddaBaseUrl + "/icons/link.png" + "\" border=0> " + label + "</a>";
    $("#treeview_header").html(href);
    url = url + "&template=empty";
    $('#treeview_view').attr("src", url);
}


function treeViewGoTo() {
    var currentUrl = $('#treeview_view').attr("src");
    if (currentUrl) {
        currentUrl = currentUrl.replace("template=", "notemplate=");
        var tmp = $(location);
        tmp.attr('href', currentUrl);
    }
}

function number_format(number, decimals, dec_point, thousands_sep) {
    // http://kevin.vanzonneveld.net
    // +   original by: Jonas Raoni Soares Silva (http://www.jsfromhell.com)
    // +   improved by: Kevin van Zonneveld (http://kevin.vanzonneveld.net)
    // +     bugfix by: Michael White (http://crestidg.com)
    // +     bugfix by: Benjamin Lupton
    // +     bugfix by: Allan Jensen (http://www.winternet.no)
    // +    revised by: Jonas Raoni Soares Silva (http://www.jsfromhell.com)    
    // *     example 1: number_format(1234.5678, 2, '.', '');
    // *     returns 1: 1234.57     

    var n = number,
        c = isNaN(decimals = Math.abs(decimals)) ? 2 : decimals;
    var d = dec_point == undefined ? "." : dec_point;
    var t = thousands_sep == undefined ? "," : thousands_sep,
        s = n < 0 ? "-" : "";
    var i = parseInt(n = Math.abs(+n || 0).toFixed(c)) + "",
        j = (j = i.length) > 3 ? j % 3 : 0;

    return s + (j ? i.substr(0, j) + t : "") + i.substr(j).replace(/(\d{3})(?=\d)/g, "$1" + t) + (c ? d + Math.abs(n - i).toFixed(c).slice(2) : "");
}


function ramaddaJsonAllOpen(id) {
    $("#" + id + " .ramadda-json-button").each(function() {
        ramaddaJsonSetVisible(id, $(this), "close");
    });
}

function ramaddaJsonAllClose(id) {
    $("#" + id + " .ramadda-json-button").each(function() {
        ramaddaJsonSetVisible(id, $(this), "open");
    });
}

function ramaddaJsonSetVisible(id, button, state, all) {
    var block = button.next(".ramadda-json-block");
    var block = button.next().next();
    //    console.log("block:" + block.size());
    if (!state)
        state = block.attr("block-state");
    if (state == "close") {
        if (all) {
            block.find(".ramadda-json-button").each(function() {
                ramaddaJsonSetVisible(id, $(this), "close");
            });
        }
        state = "open";
        block.css("display", "block");
        button.attr("src", icon_tree_open);
    } else {
        if (all) {
            block.find(".ramadda-json-button").each(function() {
                ramaddaJsonSetVisible(id, $(this), "open");
            });
        }
        state = "close";
        button.attr("src", icon_tree_closed);
        block.css("display", "none");
    }
    block.attr("block-state", state);
}

function ramaddaJsonInit(id) {

    var img = HtmlUtil.image(icon_tree_open, ["class", "ramadda-json-button", "title", "shift-click: toggle all"]);
    var links = HtmlUtil.onClick("ramaddaJsonAllOpen('" + id + "')", "All Open", []) +
        "&nbsp;&nbsp;" +
        HtmlUtil.onClick("ramaddaJsonAllClose('" + id + "')", "All Close", [])
    $("#" + id).before(links);
    var block = $("#" + id + " .ramadda-json-block");
    block.prev(".ramadda-json-openbracket").before(img + " ");
    $("#" + id + " .ramadda-json-button").click(function(evt) {
        //           $(this).css("background","red");
        ramaddaJsonSetVisible(id, $(this), null, evt.shiftKey);
    });
}
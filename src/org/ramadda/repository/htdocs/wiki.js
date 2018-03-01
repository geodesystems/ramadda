/*
 * Copyright (c) 2008-2015 Geode Systems LLC
 */


function insertText(id,value) {
    var textComp = GuiUtils.getDomObject(id);
    if(textComp) {
	insertAtCursor(textComp.obj, value);
    }
}



function insertAtCursor(myField, myValue) {

    var textScroll = myField.scrollTop;


    //IE support
    if (document.selection) {
        myField.focus();
        sel = document.selection.createRange();
        sel.text = myValue;
    }
    //MOZILLA/NETSCAPE support
    else if (myField.selectionStart || myField.selectionStart == '0') {
        var startPos = myField.selectionStart;
        var endPos = myField.selectionEnd;
        myField.value = myField.value.substring(0, startPos)
            + myValue
            + myField.value.substring(endPos, myField.value.length);
    } else {
        myField.value += myValue;
    }
    myField.scrollTop = textScroll;
}



function insertTags(id, tagOpen, tagClose, sampleText) {
    var textComp = GuiUtils.getDomObject(id);
    if(textComp) {
	insertTagsInner(id, textComp.obj, tagOpen,tagClose,sampleText);
    }
}



// apply tagOpen/tagClose to selection in textarea,
// use sampleText instead of selection if there is none
function insertTagsInner(id, txtarea, tagOpen, tagClose, sampleText) {
    //    console.log(id + " " + txtarea + " " + tagOpen +" - " + tagClose +" - " + sampleText);
    var selText, isSample = false;
    tagOpen = tagOpen.replace(/&quote;/gi,'\"');
    tagClose = tagClose.replace(/&quote;/gi,'\"');

    tagOpen = tagOpen.replace(/newline/gi,'\n');
    tagClose = tagClose.replace(/newline/gi,'\n');


    if (txtarea.selectionStart || txtarea.selectionStart == '0') { // Mozilla
        //save textarea scroll position
        var textScroll = txtarea.scrollTop;
        //get current selection
        txtarea.focus();
        var startPos = txtarea.selectionStart;
        var endPos = txtarea.selectionEnd;
        selText = txtarea.value.substring(startPos, endPos);
        //insert tags
        txtarea.value = txtarea.value.substring(0, startPos)
            + tagOpen + selText + tagClose
            + txtarea.value.substring(endPos, txtarea.value.length);
        if (isSample) {
            txtarea.selectionStart = startPos + tagOpen.length;
            txtarea.selectionEnd = startPos + tagOpen.length + selText.length;
        } else {
            txtarea.selectionStart = startPos + tagOpen.length + selText.length + tagClose.length;
            txtarea.selectionEnd = txtarea.selectionStart-tagClose.length;
        }
        //restore textarea scroll position
        txtarea.scrollTop = textScroll;
        return;
    }


    if (document.selection  && document.selection.createRange) { // IE/Opera
        //save window scroll position
        if (document.documentElement && document.documentElement.scrollTop)
            var winScroll = document.documentElement.scrollTop
            else if (document.body)
                var winScroll = document.body.scrollTop;
        //get current selection  
        txtarea.focus();
        var range = document.selection.createRange();
        selText = range.text;
        //insert tags
        range.text = tagOpen + selText + tagClose;
        //mark sample text as selected
        if (isSample && range.moveStart) {
            if (window.opera)
                tagClose = tagClose.replace(/\n/g,'');
            range.moveStart('character', - tagClose.length - selText.length); 
            range.moveEnd('character', - tagClose.length); 
        }
        if(range.select) {
            range.select();   
        }
        //restore window scroll position
        if (document.documentElement && document.documentElement.scrollTop)
            document.documentElement.scrollTop = winScroll
            else if (document.body)
                document.body.scrollTop = winScroll;
    } 

   }








/*
 * Copyright (c) 2008-2019 Geode Systems LLC
 */


function insertText(id, value) {
    hidePopupObject();
    var popup = getTooltip();
    if(popup)
	popup.hide();
    var handler = getHandler(id);
    if (handler) {
        handler.insertText(value);
        return;
    }

    var editor = HtmlUtils.getAceEditor(id);
    var textComp = GuiUtils.getDomObject(id);
    if (textComp || editor) {
        insertAtCursor(id, textComp.obj, value);
    }
}



function insertAtCursor(id, myField, value) {
    var editor = HtmlUtils.getAceEditor(id);
    if (editor) {
        var cursor = editor.getCursorPosition();
        editor.insert(value);
        editor.focus();
        return;
    }

    var textScroll = myField.scrollTop;

    //IE support
    if (document.selection) {
        myField.focus();
        sel = document.selection.createRange();
        sel.text = value;
    }
    //MOZILLA/NETSCAPE support
    else if (myField.selectionStart || myField.selectionStart == '0') {
        var startPos = myField.selectionStart;
        var endPos = myField.selectionEnd;
        myField.value = myField.value.substring(0, startPos) +
            value +
            myField.value.substring(endPos, myField.value.length);
    } else {
        myField.value += value;
    }
    myField.scrollTop = textScroll;
}

function insertTags(id, tagOpen, tagClose, sampleText) {
    hidePopupObject();

    var handler = getHandler(id);
    if (handler) {
        handler.insertTags(tagOpen, tagClose, sampleText);
        return;
    }

    var textComp = GuiUtils.getDomObject(id);
    var editor = HtmlUtils.getAceEditor(id);
    if (textComp || editor) {
        insertTagsInner(id, textComp.obj, tagOpen, tagClose, sampleText);
    }
}



// apply tagOpen/tagClose to selection in textarea,
// use sampleText instead of selection if there is none
function insertTagsInner(id, txtarea, tagOpen, tagClose, sampleText) {
    var selText, isSample = false;
    tagOpen = tagOpen.replace(/&quote;/gi, '\"');
    tagClose = tagClose.replace(/&quote;/gi, '\"');

    tagOpen = tagOpen.replace(/_newline_/gi, '\n');
    tagOpen = tagOpen.replace(/newline/gi, '\n');
    tagClose = tagClose.replace(/_newline_/gi, '\n');
    tagClose = tagClose.replace(/newline/gi, '\n');
    var editor = HtmlUtils.getAceEditor(id);
    if (editor) {
        var text = tagOpen + tagClose + " ";
        var cursor = editor.getCursorPosition();
        editor.insert(text);
        if (tagOpen.endsWith("\n")) {
            cursor.row++;
            cursor.column = 0;
        } else {
            cursor.column += tagOpen.length;
        }
        editor.selection.moveTo(cursor.row, cursor.column);
        editor.focus();
        return;
    }


    if (txtarea.selectionStart || txtarea.selectionStart == '0') { // Mozilla
        //save textarea scroll position
        var textScroll = txtarea.scrollTop;
        //get current selection
        txtarea.focus();
        var startPos = txtarea.selectionStart;
        var endPos = txtarea.selectionEnd;
        selText = txtarea.value.substring(startPos, endPos);
        //insert tags
        txtarea.value = txtarea.value.substring(0, startPos) +
            tagOpen + selText + tagClose +
            txtarea.value.substring(endPos, txtarea.value.length);
        if (isSample) {
            txtarea.selectionStart = startPos + tagOpen.length;
            txtarea.selectionEnd = startPos + tagOpen.length + selText.length;
        } else {
            txtarea.selectionStart = startPos + tagOpen.length + selText.length + tagClose.length;
            txtarea.selectionEnd = txtarea.selectionStart - tagClose.length;
        }
        //restore textarea scroll position
        txtarea.scrollTop = textScroll;
        return;
    }


    if (document.selection && document.selection.createRange) { // IE/Opera
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
                tagClose = tagClose.replace(/\n/g, '');
            range.moveStart('character', -tagClose.length - selText.length);
            range.moveEnd('character', -tagClose.length);
        }
        if (range.select) {
            range.select();
        }
        //restore window scroll position
        if (document.documentElement && document.documentElement.scrollTop)
            document.documentElement.scrollTop = winScroll
        else if (document.body)
            document.body.scrollTop = winScroll;
    }

}


var wikiAttributes = {
    test: [],
}

function wikiInitEditor(info) {
    var editor = info.editor
    editor.container.addEventListener("contextmenu", function(e) {
		e.preventDefault();
		var cursor = editor.getCursorPosition();
		var t = editor.getValue();
		var s = "";
		var lines = t.split("\n");
		for(i=0;i<lines.length;i++) {
		    if(i==cursor.row) {
			s+= lines[i].substring(0,cursor.column);
			break;
		    }
		    s +=lines[i]+"\n";
		}
		var inBracket = false;
		var i;
		for(i=s.length-1;i>=0;i--) {
		    var c = s[i];
		    if(c=="}") return;
		    if(c=="{") {
			if(inBracket) {
			    break;
			} 
			inBracket = true;
			continue;
		    }
		    inBracket = false;
		}
		if(i<0) return;
		var tag = s.substring(i+2);
		var tmp = tag.match(/type *= *\"([^\"]+)\"?/); 
		var type;
                if(tmp && tmp.length>1) type=tmp[1];
		var idx = tag.indexOf(" ");
		if(idx<0) return;
		tag = tag.substring(0,idx);
		var tags = [];
		if(tag == "display" && type) {
		    try {
		    var display = 
			(new DisplayManager()).createDisplay(type,{dummy:true});
		    if(display) {
			tags = display.getWikiEditorTags();
		    }
		    } catch(e) {
			console.log("Error getting tags for:" + type +" error:" + e);
		    }
		}

		if(wikiAttributes[tag]) {
		    wikiAttributes[tag].map(a=>tags.push(a));
		}
		if(tags.length==0) return;

		var menu = "<div style=margin:5px;><table><tr valign=top><td><div>";
		tags.map(tag=>{
			if(tag.startsWith("label:")) {
			    if(menu!="") menu += "</div></td>";
			    menu+="<td><b> " + tag.substring(6)+ "</b><br><div style='margin-left:5px;max-width:400px;overflow-x:auto;max-height:400px;overflow-y:auto;'>";
			    return;
			}
			var t = " " + tag.replace(/\"/g,"&quot;")+" ";
			menu+=HtmlUtils.onClick("insertText('" + info.id +"','"+t+"')",tag)+"<br>\n";
		    });

		menu += "</div></td></tr></table></div>";

		//		editor.session.insert(editor.getCursorPosition(), "hello");
		popupObject = getTooltip();
		//		editor.session.insert(editor.getCursorPosition(), text)
		popupObject.html(menu);
		popupObject.show();
		popupObject.position({
                        of: $(window),
                        my: "left top",
                        at: "left+" +e.x +" top+" + (e.y),
                        collision: "fit fit"
                    });
		return false;
	    }, false);

}
/*
 * Copyright (c) 2008-2019 Geode Systems LLC
 */


async function  wikiPreview(entry, id, inPlace) {
    var editor = HtmlUtils.getAceEditor(id);
    var t = editor.getValue();
    let area = $("#" + id);
    
    if(!inPlace) {
	let width =$(window).width()-100;
	$("#wikieditpreview")
	    .css("z-index",1000)
	    .css("left",area.position().left-100)
	    .css("top",area.position().top-10)
	    .css("min-width","400px")
	    .css("width",width +"px")
	    .css("max-width",width+"px")
	    .css("overflow-x","auto")

    }
    let bar = HtmlUtils.div(['class','ramadda-menubar',"style","text-align:center;width:100%;border:1px solid #ccc"],
			    HtmlUtils.onClick("wikiPreview('" + entry +"','" + id +"',true);",HtmlUtils.getIconImage("fa-sync",["title","Preview Again"]),["class","ramadda-button"]) + "&nbsp;&nbsp;" +
			    HtmlUtils.onClick("wikiPreviewClose('" + id +"');",HtmlUtils.getIconImage("fa-window-close",["title","Close Preview"])));

    var wikiCallback = function(html) {
	if(inPlace) {
	    $("#wikieditpreviewinner").html(html);
	} else {
	    html = HtmlUtils.div(["id","wikieditpreviewinner", "style","height:500px;overflow-y:auto;border:1px solid #ccc;background:white;"], html);
	    html = bar + html;
	    $("#wikieditpreview").draggable();
	    //TODO: this doesn't work
	    $("#wikieditpreview").resizable();
	    $("#wikieditpreview").html(html).show();
	}
    }
    await GuiUtils.loadHtml(ramaddaBaseUrl + "/wikify?doImports=false&entryid=" + entry + "&text=" + encodeURIComponent(t),
			    wikiCallback);
}

function wikiPreviewClose() {
    $("#wikieditpreview").hide();
}

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
    map: [
	"label:Map Attributes",
	"icon=\"#/icons/dots/green.png\"" ,
 	"width=\"100%\"",
	"height=\"400\"",
	"listentries=\"true\"",
	"details=\"false\"",
	"showLocationSearch=\"true\"",
	"showCheckbox=\"true\"",
	"showSearch=\"false\"",
	"icon=\"#/icons/dots/green.png\"",
	"iconsonly=\"false\""],
    group: [
	'label:Group Attributes',
	"showMenu=\"true\"",	      
	"showTitle=\"true\"",
	'layoutType="table|flextable|tabs|columns|rows"',
	'layoutColumns="1"'
    ],
    links: [
	'label:Links Attributes',
	'showTitle=""',
	'title=""',
	'includeicon="true"',
	'innerClass=""',
	'linkresource="true"',
	'separator=" | "',
	'tagopen=""',
	'tagclose=""'
    ],
    tabs:[
	'label:Tabs Attributes',
	'tag="html"',
	'tabsStyle="min|center|minarrow"',
	'showLink="false"', 
	'includeIcon="true"',
	'textposition="top|left|right|bottom"', 
    ]
}


function wikiInitEditor(info) {
    var editor = info.editor
    var toolbar = $("#" + info.id +"_toolbar");
    editor.container.addEventListener("contextmenu", function(e) {
	e.preventDefault();
	var cursor = editor.getCursorPosition();
	var menu = toolbar.html();
	menu = menu.replace(/(menulink_[0-9]+)/g,"$1_popup");
	menu = menu.replace(/(_entryid)/g,"popup_entryid");	
	menu = menu.replace(/(_wikilink)/g,"popup_wikilink");
	menu = menu.replace(/(_fieldname)/g,"popup_fieldname");
	menu = HtmlUtils.div(["class","wiki-editor-popup-toolbar"],menu);
	var t = editor.getValue();
	var s = "";
	var lines = t.split("\n");
	/*
	  hello
	  {{there ... |
	  }} how are you
	*/
	var inTag = false;
	var charCnt=0;
	for(i=0;i<lines.length;i++) {
	    var line  = lines[i];

	}

	var index = editor.session.doc.positionToIndex(cursor);
	var text =editor.getValue();
	//	    console.log("cursor index:" + index +" text:" + text.substring(index-4,index));
	var tmp = index;
	var left = -1;
	var right = -1;
	var gotBracket=false;
	while(tmp>=0) {
	    if (text[tmp] == "{") {
		if(gotBracket) {
		    left = tmp;
		    break;
		}
		gotBracket = true;
		tmp--;
		continue;
	    }
	    gotBracket = false;
	    tmp--;
	}

	if(left>=0) {
	    gotBracket=false;
	    tmp = left;
	    while(tmp<text.length) {
		if (text[tmp] == "}") {
		    if(gotBracket) {
			right = tmp;
			break;
		    }
		    gotBracket = true;
		    tmp++;
		    continue;
		}
		tmp++;
		gotBracket = false;
	    }
	}

	if(index>=left && index<=right && left>=0 && right>=left) {
	    var chunk = text.substring(left,right+1);
	    var tag;
	    var tmp  = chunk.match(/\{\{ *([^ \n]+)/);
	    if(tmp && tmp.length>1)  tag = tmp[1];
	    if(tag) {
		var type;
		if(tag.startsWith("display_")) {
		    type = tag.substring(8);
		    tag = "display";
		} else {
		    tmp = chunk.match(/type *= *\"([^\"]+)\"?/); 
		    if(tmp && tmp.length>1) type=tmp[1];
		}
		var tags = [];
		var extra;
		if(tag == "display" && type) {
		    try {
			var display = 
			    (new DisplayManager()).createDisplay(type,{dummy:true});
			if(display) {
			    tags = display.getWikiEditorTags();
			}
		    } catch(e) {
			console.log("Error getting tags for:" + type +" error:" + e  + " stack:" +e.stack);
		    }
		    extra = "<td><div class=wiki-editor-popup-header>Color Table</div><div class=wiki-editor-popup-items>"
		    for (a in Utils.ColorTables) {
			if(Utils.ColorTables[a].label) {
			    extra+=HU.div(["style","text-decoration: underline;font-weight:bold"],Utils.ColorTables[a].label);
			    continue;
			}
			var ct = Utils.getColorTableDisplay(Utils.ColorTables[a],  0, 1, {
			    showRange: false,
			    height: "20px"
			});
			ct = HtmlUtils.div(["style","width:150px;","title",a],ct);
			var call = "insertText(" + HtmlUtils.squote(info.id) +","+HtmlUtils.squote("colorTable=" + a)+")";
			extra+=HtmlUtils.onClick(call,ct);
		    }
		    extra+="</div></div></td>";
		}
		if(wikiAttributes[tag]) {
		    wikiAttributes[tag].map(a=>tags.push(a));
		}


		if(tags.length>0)
		    menu = "<div class=wiki-editor-popup><table><tr valign=top><td><div>";
		tags.map(tag=>{
		    let tt =null;
		    let label=null;
		    if(Array.isArray(tag)) {
			if(tag.length==3) {
			    label = tag[0];
			    tt = tag[2];
			    tag = tag[1];
			} else {
			    tt = tag[1];
			    tag = tag[0];
			}
		    } 

		    if(tag.startsWith("inlinelabel:")) {
			menu+="<b>" + tag.substring("inlinelabel:".length)+"</b><br>";
			return;
		    }
		    if(tag.startsWith("label:")) {
			if(menu!="") menu += "</div></td>";
			menu+="<td><div class=wiki-editor-popup-header> " + tag.substring(6)+ "</div><div class=wiki-editor-popup-items>";
			return;
		    }
		    var t = " " + tag.replace(/\"/g,"&quot;")+" ";
		    tag = tag.replace(/=.*$/,"");
		    let tt2 = t.replace(/"/g,"&quot;");
		    if(tt) {
//			tt2 = tt2 +" - " + tt;
			tt2 = tt;
		    } else {
			tt2 = "";
		    }
		    tag = HtmlUtils.span(["title",tt2],tag);
		    if(label)
			label = HtmlUtils.span(["title",tt2],label);
		    menu+=HtmlUtils.onClick("insertText('" + info.id +"','"+t+"')",label || tag)+"<br>\n";
		});
		
		menu+="</div></td>";
		if(extra) {
		    menu+=extra;
		}
		menu += "</tr></table></div>";
	    }
	}
	

	tooltipObject = getTooltip();
	tooltipObject.html(menu);
	tooltipObject.show();
	tooltipObject.position({
	    of: $(window),
            my: "left top",
            at: "left+" +e.x +" top+" + (e.y),
            collision: "fit fit"
	});
    });
}


/*
 * Copyright (c) 2008-2019 Geode Systems LLC
 */


let wikiPopup = null;

function  wikiInitDisplaysButton(id) {
    let button = $("#displays_button"+ id);
    button.click(() =>{
	if(!window.globalDisplayTypes) return;
        var types = window.globalDisplayTypes;
        var links = {};
        var cats = [];
        var displayTypes = [];
	DISPLAY_CATEGORIES.forEach(category=>{
            links[category] = [];
            cats.push(category);
	});
	types.forEach(type=>{
            if (Utils.isDefined(type.forUser) && !type.forUser) {
		return;
            }
	    var category = type.category ||  CATEGORY_MISC;
            if (links[category] == null) {
                links[category] = [];
                cats.push(category);
            }
	    let help = type.help||"";
	    let link = HU.div([CLASS,"wiki-editor-popup-link"],HU.href("#",type.label,[CLASS,"display-link ",TITLE,help,"onclick", "insertDisplayText('" + id + "','" + type.type+"')"]));
            links[category].push(link);
        });
        let menu = "<table><tr valign=top>";
        for (var i = 0; i < cats.length; i++) {
            var cat = cats[i];
	    menu += HU.td([],HU.div([STYLE,'margin-right:5px;'], HU.b(cat)) +"<div style='margin-right:5px;max-height:250px;overflow-y:auto;'>" + Utils.join(links[cat],"<div>"));
        }
	menu = HU.div([ID,"wiki-display-popup",STYLE,"font-size:10pt;width:800px;"], menu);
	let init = ()=>{
	    $("#wiki-display-popup").tooltip({
		content: function () {
		    return $(this).prop('title');
		},
		show: { effect: 'slide', delay: 500, duration: 400 },
		position: { my: "left top", at: "right top" }		
	    });
	};
	let popup = HU.makeDialog({content:menu,my:"left top",at:"left-200px bottom",title:"",anchor:button,draggable:true,header:true,initCall:init});
	if(wikiPopup) 
	    wikiPopup.hide();
	wikiPopup =  popup;
    });
	
}


async function  wikiPreview(entry, id, inPlace) {
    var editor = HtmlUtils.getAceEditor(id);
    var t = editor.getValue();
    let area = $("#" + id);
    if(!inPlace) {
	let width =$(window).width()-100;
	$("#wikieditpreview")
	    .css("z-index",1000)
	    .css("left","5px")//area.position().left-100)
	    .css("right","5px")//area.position().left-100)
	    .css("top",area.position().top-10)
//	    .css("min-width","400px")
	    .css("width",width+"px")
	    .css("overflow-x","auto")

    }
    let bar = HtmlUtils.div(['class','ramadda-menubar',"style","text-align:center;width:100%;border:1px solid #ccc"],
			    HtmlUtils.onClick("wikiPreview('" + entry +"','" + id +"',true);",HtmlUtils.getIconImage("fa-sync",["title","Preview Again"]),["class","ramadda-button"]) + "&nbsp;&nbsp;" +
			    HtmlUtils.onClick("wikiPreviewClose('" + id +"');",HtmlUtils.getIconImage("fa-window-close",["title","Close Preview"])));

    var wikiCallback = function(html) {
	if(inPlace) {
	    $("#wikieditpreviewinner").html(html);
	} else {
	    html = HtmlUtils.div([ID,"wikieditpreviewinner", STYLE,"width:100%;height:500px;overflow-y:auto;border:1px solid #ccc;background:white;"], html);
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

function insertDisplayText(id, value) {
    let type = window.globalDisplayTypesMap[value];
    if(!type) {
	insertText(value);
	return;
    }
    let t = "{{";
    if(type.type=="group") {
	t+="group ";
    } else  {
	t +="display_" + type.type +" ";
    }
    if(type.wiki)  t += type.wiki;
    t+="}} ";
    if(wikiPopup)
	wikiPopup.hide();
    insertText(id,t);
//    xxxxx
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
    value = Utils.decodeText(value);    


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
    tagOpen = Utils.decodeText(tagOpen);
    tagClose = Utils.decodeText(tagClose);    
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


let groupAttributes = [
    "label:Collection Properties",
    "sort=name|date|changedate|createdate",
    "sortorder=up|down",
    ['entries="entryid1,entryid2,entryid3..'],
     'entries.filter="file|folder|image|type:some type|geo|name:name pattern|suffix:file suffixes"',
     'exclude="entryid1,entryid2,entryid3.."',
     'first="entryid1,entryid2,entryid3.."',
     'last="entryid1,entryid2,entryid3.."',
     'sort="name|date"',
     'sortorder="up|down"',
     'max="number of entries to use"',
];

let wikiAttributesFromServer = null;
var wikiAttributes = {
    tree: Utils.mergeLists([
	"label:Tree Properties",
	["details=true"],
	["showcategories=true"],
	["decorate=true"],	
	["form=true"],
	['message=""'],
	['treePrefix=""'],	
    ],
			   groupAttributes),		   
    links: Utils.mergeLists([
	"label:Links Properties",
	['info:List children entries'],
	['includeIcon=true'],
	['linkresource=true','Link to the resource'],
	['separator=""','Separator between links'],
	['horizontal=true','Display horizontallly'],
	['output=""',"Link to output"],
	['tagopen="html before link'],
	['tagclose="html after link'],	
	'innerClass=""',
	['class="link css class"'],
	['style="link stye"']],
			    groupAttributes),		   
    list: Utils.mergeLists([
	"label:List Properties",
	['info:List children entries'],
	['includeIcon=true'],
	['linkresource=true','Link to the resource'],
	['separator=""','Separator between links'],
	['output=""',"Link to output"],
	['tagopen="html before link'],
	['tagclose="html after link'],	
	'innerClass=""',
	['class="link css class"'],
	['style="link stye"']],
			   groupAttributes),
    information: [
	"label:Information Properties",
	'info:Show entry information',
	["details=false"],
	["showTitle=true"],
	["showResource=true"],
	["showBase=true"],
	["showDetails=true"],		
    ],
    description: [
	"label:Description Properties",
	["wikify=true"],
	['prefix=""'],
	['suffix=""'],
	['convert_newline=true'],		
    ],
    resource: [
	"label:Resource Properties",
	'info:Link to the resource',
	['title=""'],
	['includeIcon=true'],	
	['url=true','Just include the URL'],
	['message=""','Message to show when no resource'],
    ],
    link: [
	"label:Link Properties",
	'info:Link to the entry',
	['linkresource=true','Link to the resource'],
	['button=true','Make a button'],
	['title=""','Title to use'],
	['output="output type"','Link to the given output'],
    ],
    daterange: [
	"label:Date Range Properties",
	['format="yyyy-MM-dd HH:mm:ss Z"'],
	['separator=""'],
    ],
    html: [
	"label:HTML Properties",
	'info:Include the HTML of the entry',
	['children=true','Include HTML of children entries'],
    ],
    map: [
	"label:Map Properties",
	"icon=\"#/icons/dots/green.png\"" ,
 	"width=\"100%\"",
	"height=\"400\"",
	"listentries=\"true\"",
	"details=\"false\"",
	'showLines=true',
	'showBounds=false',
	'showMarkers=false',
	"showLocationSearch=\"true\"",
	"showCheckbox=\"true\"",
	"showSearch=\"false\"",
	"icon=\"#/icons/dots/green.png\"",
	"iconsonly=\"false\""],
    name: [
	'label:Name Properties',
	['link=true','Link to the entry'],
    ],
    property: [
	'label:Property Properties',
	'name=""',
	'value=""'
    ],
    group: Utils.mergeLists([
	'label:Group Properties',
	"showMenu=\"true\"",	      
	"showTitle=\"true\"",
	'layoutType="table|flextable|tabs|columns|rows"',
	'layoutColumns="1"',
	['divid=""','Specify a div id to write displays into']]),
    tabs:Utils.mergeLists([
	'label:Tabs Properties',
	'tag="html"',
	'tabsStyle="min|center|minarrow"',
	'showLink="false"', 
	'includeIcon="true"',
	'textposition="top|left|right|bottom"', ],
			  groupAttributes),
    grid: Utils.mergeLists([
	"label:Grid Properties",
	'tag="card"',
	'inner-height="100"',
	'columns="3"',
	'includeIcon="true"',
	'weights=""',
	'showSnippet="false"',
	'showSnippetHover="true"',
	'showLink="false"',
	'showHeading="true"',
	'showLine="true"'],
			   groupAttributes
			  ),
    frames: Utils.mergeLists([
	"label:Frames Properties",
	'info:Show entries in a HTML frame',
	['width=400'],
	['height=400'],
	['noTemplate=true',"Don't use the page template in the frame"]],
			     groupAttributes),
    accordian: Utils.mergeLists([
	"label:Accordian Properties",
	'tag="html"',
	'collapse="false"',
	'border="0"',
	'showLink="true"',
	'includeIcon="false"',
	'textposition="left"',],
				groupAttributes),
    table: Utils.mergeLists([
	"label:Table Properties",
	['showCategories=true'],
	['showDate=true'],
	['showCreateDate=true'],
	['showChangeDate=true'],
	['show&lt;column name&gt;=true'],],
			     groupAttributes),
			    
    recent: Utils.mergeLists([
	"label:Recent Properties",
	['info:List N days recent entries by day'],
	['days="num days"'],],
			     groupAttributes),
    multi: [
	"label:Multi Properties",
	['info:Create multiple wiki tags'],
	['_tag="tag to create"'],
	['_template="wiki text template. Escape { and } with backslash"'],
	['_entries="entries"',"entries to apply to"],
	['_headers="comma separated headers'],
	['_headerTemplate="...${header}...'],
	['_columns="number of columns"'],
	['first.&lt;some attribute&gt;="attr for first tag'],
	['last.&lt;some attribute&gt;="attr for last tag'],
	['notlast.&lt;some attribute&gt;="attr for first N tags"'],
	['notfirst.&lt;some attribute&gt;="attr for last N tags"'],
    ],        
    menu: [
	"label:Menu Properties",
	['menus=file,edit,view,feeds,other'],
	['popup=true'],
	['breadcrumbs=true'],
    ],
    t: [
	"label:",
	[''],
    ],        

}




function wikiInitEditor(info) {
    var editor = info.editor
    var toolbar = $("#" + info.id +"_toolbar");
    let eventHandler = (e=>{
	var cursor = editor.getCursorPosition();
	var t = editor.getValue();
	var s = "";
	var lines = t.split("\n");
	var inTag = false;
	var charCnt=0;
	for(i=0;i<lines.length;i++) {
	    var line  = lines[i];

	}
	
	var menu = toolbar.html();
	menu = menu.replace(/(menulink_[0-9]+)/g,"$1_popup");
	menu = menu.replace(/(_entryid)/g,"popup_entryid");	
	menu = menu.replace(/(_wikilink)/g,"popup_wikilink");
	menu = menu.replace(/(_fieldname)/g,"popup_fieldname");
	menu = HtmlUtils.div(["class","wiki-editor-popup-toolbar"],menu);

	let dmenu = "";
	let toggleLabel ="";
	let blockCnt=-1;
	let handleBlock = ()=> {
	    if(dmenu=="") return;
	    toggleLabel = HU.div([CLASS,"wiki-editor-popup-header"], toggleLabel);
	    blockCnt++;
	    if(blockCnt>5) {
		menu += HU.close('div');
		menu += HU.open('div',[CLASS,'wiki-editor-popup-section']);
		blockCnt=0;
	    }
	    dmenu = HU.div([CLASS,"wiki-editor-popup-items"],dmenu);
	    menu +=HU.toggleBlock(toggleLabel, dmenu);
	    dmenu = "";
	};


	var index = editor.session.doc.positionToIndex(cursor);
	var text =editor.getValue();
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
		if(tag=="xxgroup") {
		    type="blank"
		    tag = "display";
		}  else if(tag.startsWith("display_")) {
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
		    extra = Utils.getColorTablePopup(info);
		}
		if(!wikiAttributes[tag]) {
		    if(!wikiAttributesFromServer) {
			let url = ramaddaBaseUrl + "/wikitags";
			$.getJSON(url, function(data) {
			    wikiAttributesFromServer = data;
			    eventHandler(e);
			});
			return
		    }
		    if(wikiAttributesFromServer) {
			wikiAttributes[tag] = wikiAttributesFromServer[tag];
		    }
		}

		if(wikiAttributes[tag]) {
		    wikiAttributes[tag].forEach(a=>tags.push(a));
		}

		let processTag =(tag)=>{
		    let tt =null;
		    let label=null;
		    if(Array.isArray(tag)) {
			let a = tag;
			if(tag.length==3) {
			    label = a[0];
			    tag = a[1];
			    tt = a[2];
			} else {
			    tt = a[1];
			    tag = a[0];
			}
		    } 
		    if(tag.inline)  tag = "inlinelabel:" + tag.inline;
		    if(tag.inlineLabel)  tag = "inlinelabel:" + tag.inlineLabel;
		    if(tag.label) tag="label:" + tag.label;
		    if(!tag.startsWith) console.log(tag);

		    if(tag.startsWith("inlinelabel:")) {
			handleBlock();
			toggleLabel = tag.substring("inlinelabel:".length);
			return;
		    }
		    if(tag.startsWith("label:")) {
			handleBlock();
			toggleLabel = tag.substring(6);
			return;
		    }
		    if(tag.startsWith("info:")) {
			dmenu+="<i>"+tag.substring(5)+"</i><br>";
			return;
		    }
		    let t = " " + tag.replace(/\"/g,"&quot;")+" ";
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
		    dmenu+=HU.div([CLASS,"ramadda-menu-item"], HtmlUtils.onClick("insertText('" + info.id +"','"+t+"')",label || tag));
		};
		
		if(tags.length>0) {
		    menu = HU.open('div',[CLASS,'wiki-editor-popup']);
		    menu = HU.open('div',[CLASS,'wiki-editor-popup-section']);
		}

		tags.forEach(t=>{
		    if(t.list) {
			t.list.forEach(processTag);
		    } else {
			processTag(t);
		    }
		});
		handleBlock();
		if(extra) {
		    menu+=extra;
		}
		menu += "</div>";
		menu += "</div>";
	    }
	}

	popupObject = getTooltip();
	popupObject.html(HU.div([STYLE,HU.css("margin","5px")], menu));
	popupObject.show();
	popupObject.position({
	    of: $(window),
            my: "left top",
            at: "left+" +e.x +" top+" + (e.y),
            collision: "fit fit"
	});
    });


    editor.container.addEventListener("contextmenu", function(e) {
	e.preventDefault();
	eventHandler(e);
    })



}



function wikiEditorHandlePopup(info, tag) {
    var type;
    if(tag=="xxgroup") {
	type="blank"
	tag = "display";
    }  else if(tag.startsWith("display_")) {
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
	extra = Utils.getColorTablePopup(info);
    }
    if(wikiAttributes[tag]) {
	wikiAttributes[tag].map(a=>tags.push(a));
    }
	
    let processTag =(tag)=>{
	let tt =null;
	let label=null;
	if(Array.isArray(tag)) {
	    let a = tag;
	    if(tag.length==3) {
		label = a[0];
		tt = a[2];
		tag = a[1];
	    } else {
		tt = a[1];
		tag = a[0];
	    }
	} 
	if(tag.inline)  tag = "inlinelabel:" + tag.inline;
	if(tag.inlineLabel)  tag = "inlinelabel:" + tag.inlineLabel;
	if(tag.label) tag="label:" + tag.label;
	if(!tag.startsWith) console.log(tag);
	
	if(tag.startsWith("inlinelabel:")) {
	    handleBlock();
	    toggleLabel = tag.substring("inlinelabel:".length);
	    return;
	}
	if(tag.startsWith("label:")) {
	    handleBlock();
	    toggleLabel = tag.substring(6);
	    return;
	}
	if(tag.startsWith("info:")) {
	    dmenu+="<i>"+tag.substring(5)+"</i><br>";
	    return;
	}
	let t = " " + tag.replace(/\"/g,"&quot;")+" ";
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
	dmenu+=HtmlUtils.onClick("insertText('" + info.id +"','"+t+"')",label || tag)+"<br>\n";
    };
    
    if(tags.length>0) {
	menu = HU.open('div',[CLASS,'wiki-editor-popup']);
	menu = HU.open('div',[CLASS,'wiki-editor-popup-section']);
    }
    tags.forEach(t=>{
	if(t.list) {
	    t.list.forEach(processTag);
	} else {
	    processTag(t);
	}
    });
    handleBlock();
    if(extra) {
	menu+=extra;
    }
    menu += "</div>";
    menu += "</div>";
    popupObject = getTooltip();
    popupObject.html(menu);
    popupObject.show();
    popupObject.position({
	of: $(window),
	my: "left top",
	at: "left+" +e.x +" top+" + (e.y),
	collision: "fit fit"
    });
}



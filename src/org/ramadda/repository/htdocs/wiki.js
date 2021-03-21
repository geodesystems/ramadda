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
	    let tooltip = type.tooltip||"";
	    tooltip = tooltip.replace(/"/g,"&quot;");
	    let click = "insertDisplayText('" + id + "','" + type.type+"')";
	    let link = HU.div([CLASS,"wiki-editor-popup-link"],HU.href("#",type.label,[CLASS,"display-link ",TITLE,tooltip,"onclick", click]));
            links[category].push(link);
        });
        let menu = "<table><tr valign=top>";
        for (let i = 0; i < cats.length; i++) {
            let cat = cats[i];
	    let menuItems = Utils.join(links[cat],"<div>\n");
	    menu += HU.td([],HU.div([STYLE,'margin-right:5px;'], HU.b(cat)) +"<div style='margin-right:5px;max-height:250px;overflow-y:auto;'>" + menuItems);
        }
	menu = HU.div([ID,"wiki-display-popup",STYLE,"font-size:10pt;width:800px;"], menu);
	let init = ()=>{
	    let tt  =    $("#wiki-display-popup").tooltip({
		classes: {"ui-tooltip": "wiki-editor-tooltip"},
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
    HtmlUtils.getWikiEditor(id).doPreview(entry, inPlace);
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
    var editor = HtmlUtils.getWikiEditor(id);
    if (editor) {
        editor.insertAtCursor(value);
	return;
    }
    var textComp = GuiUtils.getDomObject(id);
    if (textComp) {
        insertAtCursor(id, textComp.obj, value);
    }
}



function insertAtCursor(id, myField, value) {
    value = Utils.decodeText(value);    
    var editor = HtmlUtils.getWikiEditor(id);
    if (editor) {
	editor.insertAtCursor(value);
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
    let editor = HtmlUtils.getWikiEditor(id);
    if(editor) {
	editor.insertTags(tagOpen,tagClose, sampleText);
	return;
    }
    console.log("Could not find editor:" + id);
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
    {label:'Collection Properties'},
    {p:'sort',ex:'name|date|changedate|createdate'},
    {p:'sortorder',ex:'up|down'},
    {p:'entries',ex:'entryid1,entryid2,entryid3..'},
    {p:'entries.filter',ex:'file|folder|image|type:some type|geo|name:name pattern|suffix:file suffixes'},
    {p:'exclude',ex:'entryid1,entryid2,entryid3..'},
    {p:'first',ex:'entryid1,entryid2,entryid3..'},
    {p:'last',ex:'entryid1,entryid2,entryid3..'},
    {p:'sort',ex:'name|date'},
    {p:'sortorder',ex:'up|down'},
    {p:'max',ex:'number of entries to use'},
];

let wikiAttributesFromServer = null;
let wikiAttributes = {
    tree: 
	Utils.mergeLists([
	    {label:'Tree Properties'},
	    {p:'details',ex:'true'},
	    {p:'showcategories',ex:'true'},
	    {p:'decorate',ex:'true'},	
	    {p:'form',ex:'true'},
	    {p:'message',ex:''},
	    {p:'treePrefix',ex:''},	
	],groupAttributes),		   
    links: Utils.mergeLists([
	{label:'Links Properties'},
	{p:'info',ex:'List children entries'},
	{p:'includeIcon',ex:'true'},
	{p:'linkresource',ex:'true',tt:'Link to the resource'},
	{p:'separator',ex:'',tt:'Separator between links'},
	{p:'horizontal',ex:'true',tt:'Display horizontallly'},
	{p:'output',ex:'',tt:'Link to output'},
	{p:'tagopen',ex:'html before link'},
	{p:'tagclose',ex:'html after link'},	
	{p:'innerClass',ex:''},
	{p:'class',ex:'',tt:'link css class'},
	{p:'style',ex:'',tt:'link style'}],
			    groupAttributes),		   
    list: Utils.mergeLists([
	{label:'List Properties'},
	{p:'info',ex:':List children entries'},
	{p:'includeIcon',ex:'true'},
	{p:'linkresource',ex:'true',tt:'Link to the resource'},
	{p:'separator',ex:'',tt:'Separator between links'},
	{p:'output',ex:'',tt:'Link to output'},
	{p:'tagopen',ex:'',tt:'html before link'},
	{p:'tagclose',ex:'',tt:'html after link'},	
	{p:'innerClass',ex:''},
	{p:'class',ex:'',tt:'link css class'},
	{p:'style',ex:'',tt:'link style'}],
			   groupAttributes),

    information: [
	{label:'Information Properties'},
	{p:'info',ex:'Show entry information'},
	{p:'details',ex:'false'},
	{p:'showTitle',ex:'true'},
	{p:'showResource',ex:'true'},
	{p:'showBase',ex:'true'},
	{p:'showDetails',ex:'true'},],
    description: [
	{label:'Description Properties'},
	{p:'wikify',ex:'true'},
	{p:'prefix',ex:''},
	{p:'suffix',ex:''},
	{p:'convert',ex:'_newline=true'},		
    ],
    resource: [
	{label:'Resource Properties'},
	{p:'info',ex:'',tt:'Link to the resource'},
	{p:'title',ex:''},
	{p:'includeIcon',ex:'true'},	
	{p:'url',ex:'true',tt:'Just include the URL'},
	{p:'message',ex:'',tt:'Message to show when no resource'},
    ],
    link: [
	{label:'Link Properties'},
	{p:'info',ex:'Link to the entry'},
	{p:'linkresource',ex:'true',tt:'Link to the resource'},
	{p:'button',ex:'true',tt:'Make a button'},
	{p:'title',ex:'',tt:'Title to use'},
	{p:'output',ex:'output type',tt:'Link to the given output'},
    ],
    daterange: [
	{label:'Date Range Properties'},
	{p:'format',ex:'yyyy-MM-dd HH:mm:ss Z'},
	{p:'separator',ex:''},
    ],
    html: [
	{label:'HTML Properties'},
	{p:'info',ex:'true', tt:'Include the HTML of the entry'},
	{p:'children',ex:'true',tt:'Include HTML of children entries'},
    ],


    map: [
	{label:'Map Properties'},
	{p:'icon',ex:'#/icons/dots/green.png' },
 	{p:'width',ex:'100%'},
	{p:'height',ex:'400'},
	{p:'listentries',ex:'true'},
	{p:'details',ex:'false'},
	{p:'showLines',ex:'true'},
	{p:'showBounds',ex:'false'},
	{p:'showMarkers',ex:'false'},
	{p:'showLocationSearch',ex:'true'},
	{p:'showCheckbox',ex:'true'},
	{p:'showSearch',ex:'false'},
	{p:'icon',ex:'/icons/dots/green.png'},
	{p:'iconsonly',ex:'false'},],
    name:[
	{label:'Name Properties'},
	{p:'link',ex:'true',tt:'Link to the entry'}],

    property: [
	{label:'Property Properties'},
	{p:'name',ex:''},
	{p:'value',ex:''}],


    group: Utils.mergeLists([
	{label:'Group Properties'},
	{p:'showMenu',ex:'true'},	      
	{p:'showTitle',ex:'true'},
	{p:'layoutType',ex:'table|flextable|tabs|columns|rows'},
	{p:'layoutColumns',ex:'1'},
	{p:'divid',ex:'',tt:'Specify a div id to write displays into'}]),
    tabs:Utils.mergeLists([
	{label:'Tabs Properties'},
	{p:'tag',ex:'html'},
	{p:'tabsStyle',ex:'min|center|minarrow'},
	{p:'showLink',ex:'false'}, 
        {p:'includeIcon',ex:'true'},
	{p:'textposition',ex:'top|left|right|bottom'}, 
    ], groupAttributes),

    grid: Utils.mergeLists([
	{label:'Grid Properties'},
	{p:'tag',ex:'card'},
	{p:'inner-height',ex:'100'},
	{p:'columns',ex:'3'},
	{p:'includeIcon',ex:'true'},
	{p:'weights',ex:''},
	{p:'showSnippet',ex:'false'},
	{p:'showSnippetHover',ex:'true'},
	{p:'showLink',ex:'false'},
	{p:'showHeading',ex:'true'},
	{p:'showLine',ex:'true'},],
			   groupAttributes),
    frames: Utils.mergeLists([
	{label:'Frames Properties'},
	{p:'width',ex:'400'},
	{p:'height',ex:'400'},
	{p:'noTemplate',ex:'true',tt:'Don\'t use the page template in the frame'}],
			     groupAttributes),
    accordian: Utils.mergeLists([
	{label:'Accordian Properties'},
	{p:'tag',ex:'html'},
	{p:'collapse',ex:'false'},
	{p:'border',ex:'0'},
	{p:'showLink',ex:'true'},
	{p:'includeIcon',ex:'false'},
	{p:'textposition',ex:'left'},],
				groupAttributes),

    table: Utils.mergeLists([
	{label:'Table Properties'},
	{p:'showCategories',ex:'true'},
	{p:'showDate',ex:'true'},
	{p:'showCreateDate',ex:'true'},
	{p:'showChangeDate',ex:'true'},
	{p:'show',ex:'&lt;column name&gt;=true'},],
			     groupAttributes),
			    
    recent: Utils.mergeLists([
	{label:'Recent Properties'},
	{p:'info',ex:'',tt:'List N days recent entries by day'},

	{p:'days',ex:'num days'},],
			     groupAttributes),


    multi: [
	{label:'Multi Properties'},
	{info:'Create multiple wiki tags'},
	{p:'_tag',ex:'tag to create'},
	{p:'_template',ex:'',tt:'wiki text template. Escape { and } with backslash'},
	{p:'_entries',ex:'entries',tt:'entries to apply to'},
	{p:'_headers',ex:'comma separated headers'},
	{p:'_headerTemplate',ex:'...${header}...'},
	{p:'_columns',ex:'number of columns'},
	{p:'first',ex:'.&lt;some attribute&gt;=\'attr for first tag\''},
	{p:'last',ex:'.&lt;some attribute&gt;=\'attr for last tag\''},
	{p:'notlast',ex:'.&lt;some attribute&gt;=\'attr for first N tags\''},
	{p:'notfirst',ex:'.&lt;some attribute&gt;=\'attr for last N tags\''},
    ],        

    menu: [
	{label:'Menu Properties'},
	{p:'menus',ex:'file,edit,view,feeds,other'},
	{p:'popup',ex:'true'},
	{p:'breadcrumbs',ex:'true'},

    ],
}


const ID_WIKI_PREVIEW = "preview";
const ID_WIKI_PREVIEW_INNER = "preview_inner";
const ID_WIKI_PREVIEW_OPEN= "preview_open";
const ID_WIKI_PREVIEW_CLOSE = "preview_close";
const ID_WIKI_MESSAGE = "message";
const ID_WIKI_MENUBAR    = "menubar";
const ID_WIKI_POPUP_EDITOR = "wiki-popup-editor";
const ID_WIKI_POPUP_OK= "wiki-popup-ok";
const ID_WIKI_POPUP_CANCEL= "wiki-popup-cancel";
function WikiEditor(formId, id, hidden,argOptions) {
    var options = {
        autoScrollEditorIntoView: true,
        copyWithEmptySelection: true,
        //            theme:'ace/theme/solarized_light',
    };
    if (argOptions)
        $.extend(options, argOptions);


    $.extend(this,
	     {id:id,
             editor:ace.edit(id),
             formId:formId,
              hidden:hidden});
    HU.addWikiEditor(this);
    this.editor.setBehavioursEnabled(false);
    this.editor.setDisplayIndentGuides(false);
    this.editor.setKeyboardHandler("emacs");
    this.editor.setShowPrintMargin(false);
    this.editor.getSession().setUseWrapMode(true);
    this.editor.setOptions(options);
    this.editor.session.setMode("ace/mode/ramadda");
    this.toolbar = $("#" + this.getId() +"_toolbar");

    this.editor.container.addEventListener("contextmenu", (e) =>{
	e.preventDefault();
	this.handlePopupMenu(e);
    });

    this.editor.container.addEventListener("mouseup", (e) =>{
	this.handleMouseUp(e);
    });

    this.editor.container.addEventListener("mouseleave", (e) => {
	this.handleMouseLeave(e);
    });

    this.editor.container.addEventListener("mousemove", (e) => {
	this.handleMouseMove(e);
    });
    this.getBlock().find("#" + this.id).append(HU.div([STYLE,HU.css("display","none"), CLASS,"wiki-editor-message",ID,this.getDomId(ID_WIKI_MESSAGE)]));
}

WikiEditor.prototype = {
    getId:function() {
	return this.id;
    },
    getDomId: function(suffix) {
	return this.getId() +"_"+ suffix;
    },
    jq: function(suffix) {
	return $("#"+this.getDomId(suffix));
    },
    showMessage: function(msg) {
	this.jq(ID_WIKI_MESSAGE).css("display","block").html(msg);
    },
    clearMessage: function(msg) {
	this.jq(ID_WIKI_MESSAGE).css("display","none");
    },

    getEditor:function() {
	return this.editor;
    },    
    getValue: function() {
	return this.getEditor().getValue();
    },
	
    getBlock:function() {
	return $("#" + this.getId()+"_block");
    },

    getScroller:function() {
	return this.getBlock().find(".ace_scroller");
    },
    getSession:function() {
	return this.getEditor().session;
    },
    insertAtCursor:function(value) {
	value = Utils.decodeText(value);    
	if(this.popupShowing) {
	    insertAtCursor(null, this.jq(ID_WIKI_POPUP_EDITOR)[0], value);
	    this.jq(ID_WIKI_POPUP_EDITOR).focus();
	    //We do this because the  SF menu stays popped up after clicking so we hide it
	    //then after a second we remove the style so subsequent menu clicks will work
	    this.jq(ID_WIKI_MENUBAR).find(".wiki-popup-menu-item").css("display","none");
	    setTimeout(()=> {
		this.jq(ID_WIKI_MENUBAR).find(".wiki-popup-menu-item").removeAttr("style");
	    },1000);
	    return;
	}
        var cursor = this.getEditor().getCursorPosition();
        this.getEditor().insert(value);
        this.getEditor().focus();
    },

    insertTags:function(tagOpen, tagClose, sampleText) {
	var selText, isSample = false;
	tagOpen = Utils.decodeText(tagOpen);
	tagClose = Utils.decodeText(tagClose);    
        var text = tagOpen + tagClose + " ";
        var cursor = this.getEditor().getCursorPosition();
        this.getEditor().insert(text);
        if (tagOpen.endsWith("\n")) {
            cursor.row++;
            cursor.column = 0;
        } else {
            cursor.column += tagOpen.length;
        }
        this.getEditor().selection.moveTo(cursor.row, cursor.column);
        this.getEditor().focus();
    },
    getTagInfo: function(cursor) {
	cursor = cursor || this.getEditor().getCursorPosition();
	let index = this.getSession().doc.positionToIndex(cursor);
	let text =this.getEditor().getValue();
	let tmp = index;
	let left = -1;
	let right = -1;
	let gotBracket=false;
	let seenNewLine = false;
	while(tmp>=0) {
	    let c = text[tmp];
	    if (c == '{') {
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


	let title = null;
	if(index>=left && index<=right && left>=0 && right>=left) {
	    let chunk = text.substring(left,right+1);
	    let tag;
	    let tmp  = chunk.match(/\{\{ *([^ \n\}]+)/);
	    if(tmp && tmp.length>1)  tag = tmp[1];
	    if(tag) {
		let type="";
		let attrs =  chunk.trim().substring(2);
		let idx1 = attrs.indexOf(" ");
		let idx2 = attrs.indexOf("}}");
		if(idx1!=-1 &&idx1<idx2) attrs = attrs.substring(idx1).trim();
		else attrs = attrs.substring(idx2).trim();
		if(attrs.endsWith("}}")) attrs = attrs.replace(/}}$/,"");
//		console.log("chunk:" + chunk+": tag:" + tag +" attrs:" + attrs);
		if(tag=="group") {
		    type="group"
		    tag = "display";
		}  else if(tag.startsWith("display_")) {
		    type = tag.substring(8);
		    tag = "display";
		} else {
		    tmp = chunk.match(/type *= *\"([^\"]+)\"?/); 
		    if(tmp && tmp.length>1) type=tmp[1];
		}


		let cursorStart  = left;
		/*
		  line 1
		  line {{hello}}
		  there
		**/
		
		let cnt = 0;
		let lines =  this.getSession().doc.getAllLines();
		let lineCnt = 0;
		let row=0;
		let  startRow=-1;
		let  endRow=-1;
		let  startCol=-1;
		let  endCol=-1;				
//		console.log(left  +" "+ right);
		let startChar = left+1;
		let endChar = right+1;		
		lines.every(line=>{
		    let nextCnt = cnt+line.length+1;
		    if(startRow == -1 && nextCnt>=startChar) {
			startRow=row;
			startCol = startChar-cnt-1;
//			console.log("start: " +startRow +" " + startCol  +" line:" + line);
		    }
		    if(startRow!=-1 && endRow == -1 && nextCnt>=endChar) {
			endRow=row;
			endCol = endChar-cnt;
//			console.log("end: " +endRow +" " + endCol  +" line:" + line);
			return false;
		    }		    
		    row++;
		    cnt=nextCnt;
		    return true;
		});
		      
		let range = {start: {row:startRow,column:startCol},
			     end: {row:endRow,column:endCol}
			    }
		return {
		    range:range,
		    tag:tag,
		    type: type,
		    left:left,
		    right:right,
		    chunk:chunk,
		    attrs:attrs
		};
	    }
	}
	return null;
    },
    handleSubmit:function() {
	$("#" + this.hidden).val(this.getEditor().getValue());
    },
    doPreview: async  function(entry,  inPlace) {
	let id = this.getId();
	let area = $("#" + id);
	if(!inPlace) {
	    //the left is set to 100px
	    let width =$(window).width()-200;
	    $("#" + this.getDomId(ID_WIKI_PREVIEW)).remove();
	    $(HU.div([ID, this.getDomId(ID_WIKI_PREVIEW), CLASS,"wiki-editor-preview"],"")).appendTo(this.getBlock());
	    let preview = $("#" + this.getDomId(ID_WIKI_PREVIEW));
	    preview.css("width",width+"px");
	}
	let bar = HtmlUtils.div(['class','ramadda-menubar',"style","text-align:center;width:100%;border:1px solid #ccc"],
				HU.span([CLASS, "ramadda-clickable",ID,this.getDomId(ID_WIKI_PREVIEW_OPEN)], HtmlUtils.getIconImage("fa-sync",["title","Preview Again"])) +
				SPACE2 +
				HU.span([CLASS, "ramadda-clickable",ID,this.getDomId(ID_WIKI_PREVIEW_CLOSE)], HtmlUtils.getIconImage("fa-window-close",["title","Close Preview"])));



	let wikiCallback = (html) =>{
	    if(inPlace) {
		$("#" + this.getDomId(ID_WIKI_PREVIEW_INNER)).html(html);
	    } else {
		html = HtmlUtils.div([ID,this.getDomId(ID_WIKI_PREVIEW_INNER), CLASS,"wiki-editor-preview-inner"], html);
		html = bar + html;
		let preview = $("#"+this.getDomId(ID_WIKI_PREVIEW));
		preview.html(html).show();
		preview.draggable();
		preview.resizable({xcontainment: "parent",handles: 'ne, nw'});	    
		$("#" + this.getDomId(ID_WIKI_PREVIEW_OPEN)).click(() =>{
		    this.doPreview(entry,true);
		});
		$("#" + this.getDomId(ID_WIKI_PREVIEW_CLOSE)).click(() =>{
		    $("#"+ this.getDomId(ID_WIKI_PREVIEW)).hide();
		});		
	    }
	}
	let text = this.getValue();
	await GuiUtils.loadHtml(ramaddaBaseUrl + "/wikify?doImports=false&entryid=" + entry + "&text=" + encodeURIComponent(text),
				wikiCallback);
    },

    getAttributeBlocks:function(tagInfo, finalCallback) {
	if(!tagInfo) {
	    return null;
	}
	let blocks = [];
	let block = null;
	let addBlock = (title)=> {
	    items = [];
	    blocks.push(block={title:title,items:items});
	};

	let { attrs, title, display }  = this.getDisplayAttributes(tagInfo);
	let extra;
	if(display)
	    extra = Utils.getColorTablePopup(this);

	let callback =a => {
	    this.getAttributeBlocks(tagInfo, finalCallback);
	};
	let wikiAttrs = this.getWikiAttributes(tagInfo,callback);
	//Callback later
	if(wikiAttrs===false) return false;
	if(wikiAttrs) {
	    attrs = Utils.mergeLists(attrs, wikiAttrs);
	}

	let processAttr =(tag)=>{
	    if(tag.inline|| tag.inlineLabel) {
		addBlock(tag.inline || tag.inlineLabel);
		return;
	    }
	    if(tag.label && !tag.p) {
		addBlock(tag.label);
		return;
	    }
	    if(tag.info) {
		block.items.push(HU.div([CLASS,"ramadda-menu-item"], "<i>"+tag.info+"</i>"));
		return;
	    }
	    if(!tag.p)  {
		console.log("Unknown arg:" + JSON.stringify(tag));
		return;
	    }
	    let label = tag.label||tag.p;
	    let attr=" " +tag.p+"=";
	    if(tag.ex)
		attr+='"'+tag.ex+'"';
	    else
		attr+="\"\"";
	    attr+=" ";
	    attr  =attr.replace(/\"/g,"&quot;");
	    block.items.push(HU.div([CLASS,"ramadda-menu-item",TITLE,tag.tt||""], HtmlUtils.onClick("insertText('" + this.getId() +"','"+attr+"')",label)));
	};
	
	attrs.forEach(attr=>{
	    processAttr(attr);
	});
	if(extra) {
	    blocks.push(extra);
	}
	if(!title) {
	    title = Utils.makeLabel(tagInfo.tag) +" Properties";
	}
	
	let r = {blocks:blocks, title:title,display:display};
	if(finalCallback) finalCallback(r);
	return r;
    },


    handlePopupMenu:function(event, result) {
	let tagInfo = this.getTagInfo();
	if(!tagInfo) {
	    return;
	}

	if(!result) {
	    this.getAttributeBlocks(tagInfo,(r)=>{
		this.handlePopupMenu(event,r);
	    });
	    return;
	}


	let blocks = result.blocks;
	let title = result.title;
	let display = result.display;

	if(blocks.length==0) return;
	let menu =  HU.open('div',[CLASS,'wiki-editor-popup']);
	if(!title) {
	    title = Utils.makeLabel(tagInfo.tag) +" Properties";
	}
	menu += HU.div([CLASS,"wiki-editor-popup-heading"], title);
	Utils.splitList(blocks,5).forEach(blocks=>{
	    menu += HU.open('div',[CLASS,'wiki-editor-popup-section']);
	    blocks.forEach(block=>{
		if(typeof block=="string") {
		    menu+=block;
		    return;
		}
		let title = HU.div([CLASS,"wiki-editor-popup-header"], block.title);
		let contents  = block.items.join("");
		contents = HU.div([CLASS,"wiki-editor-popup-items"],contents);
		menu +=HU.toggleBlock(block.title, contents);
	    });
	    menu += HU.close('div');
	});
	menu += "</div>";


	//	HU.makeDialog({content:menu,anchor:this.getScroller(),title:title,header:true,sticky:true,draggable:true,modal:true});
	popupObject = getTooltip();
	popupObject.html(menu);
	popupObject.show();
	popupObject.position({
	    of: $(window),
            my: "left top",
            at: "left+" +event.x +" top+" + (event.y),
            collision: "fit fit"
	});
    },

    handleMouseUp:function(e,result) {
	if(!e.metaKey)  return;
	let position = this.getEditor().renderer.screenToTextCoordinates(e.x, e.y);
	if(!position) return;
	let tagInfo = this.getTagInfo(position);
	if(!tagInfo) return;

	if(!result) {
	    this.getAttributeBlocks(tagInfo,r=>{
		this.handleMouseUp(e,r);
	    });
	    return
	}
	let blocks = result.blocks;
	let title = result.title;
	let display = result.display;
	let attrs = Utils.parseAttributes(tagInfo.attrs);
	let contents = tagInfo.attrs;
	let menu  = "";
	blocks.forEach((block,idx)=>{
	    if(typeof block=="string") {
		//TODO: this is the color tables
		//		menu+=block;
		return;
	    }
	    let sub = Utils.wrap(block.items,"<li>","");
	    menu += HU.tag(TAG_LI, [],HU.div([CLASS,"wiki-popup-menu-header"],block.title) + HU.tag("ul", [CLASS,"wiki-popup-menu-item"], sub));
	});
	let id = this.getDomId(ID_WIKI_MENUBAR);
	var menubar = HU.div([CLASS,"wiki-popup-menubar",  ATTR_ID, id],
			     HU.tag("ul", [ATTR_ID, id+"_inner", ATTR_CLASS, "sf-menu"], menu));
	let html = HU.div([ID,this.getDomId("wiki-editor-popup"),CLASS,"wiki-editor-editor"],
			  menubar +
			  HU.textarea("",contents,[ID,this.getDomId(ID_WIKI_POPUP_EDITOR),"rows","10","cols","120"]) + "<br>" +
			  HU.div([STYLE,HU.css("text-align","center","padding","4px")],
				 HU.div([ID,this.getDomId(ID_WIKI_POPUP_OK)],"Ok") + SPACE1 +
				 HU.div([ID,this.getDomId(ID_WIKI_POPUP_CANCEL)],"Cancel")));
	let dialog;
	dialog = HU.makeDialog({content:html,anchor:this.getScroller(),title:title,header:true,sticky:true,draggable:true,modal:true});
	this.popupShowing = true;
	this.jq(ID_WIKI_POPUP_EDITOR).focus();
	this.jq(ID_WIKI_POPUP_CANCEL).button().click(()=>{
	this.popupShowing = false;
	    dialog.remove();
	});
//	console.log(JSON.stringify(tagInfo));
	this.jq(ID_WIKI_POPUP_OK).button().click(()=>{
	    let val = this.jq(ID_WIKI_POPUP_EDITOR).val();
	    let tag =  tagInfo.tag;
	    //A hack
	    if(tag == "display" && tagInfo.type == "group") tag  = "group";
	    let text = "{{" +tag +" " + val +"}}";
	    this.popupShowing = false;
	    dialog.remove();
	    this.getSession().replace(tagInfo.range, text);
	});	    
    },
    handleMouseLeave:function(e) {
	this.setInContext(false);
    },
    setInContext:function(c) {
	this.inContext = c;
	let scroller = this.getScroller();
	if(c) {
	    scroller.css("cursor","context-menu");
	    this.showMessage("Right-click to show property menu<br>Cmd-click to edit");
	} else {
	    scroller.css("cursor","text");
	    this.clearMessage();
	    if(this.lastTimeout)
		clearTimeout(this.lastTimeout);
	}
	this.lastTimeout = null;
    },
    handleMouseMove:function(e) {
	if(this.lastTimeout)
	    clearTimeout(this.lastTimeout);
	let func = () =>{
	    let position = this.getEditor().renderer.screenToTextCoordinates(e.x, e.y);
	    if(!position) return;
	    let tagInfo = this.getTagInfo(position);
	    if(tagInfo) {
		this.setInContext(true);
	    } else {
		this.setInContext(false);
	    }
	};
	if(this.inContext) func();
	else {
	    this.lastTimeout=setTimeout(func,500);
	}
    },
    getWikiAttributes: function(tagInfo, callback) {
	if(wikiAttributes[tagInfo.tag]) {
	    return wikiAttributes[tagInfo.tag];
	}
	if(!wikiAttributesFromServer) {
	    let url = ramaddaBaseUrl + "/wikitags";
	    $.getJSON(url, (data) =>{
		wikiAttributesFromServer= data;
		callback(wikiAttributesFromServer[tagInfo.tag]);
	    });
	    return false;
	}
	return wikiAttributesFromServer[tagInfo.tag];
    },

    getDisplayAttributes: function(tagInfo) {
	let title = null;
	let attrs = [];
	let display = null;
	try {
	    display = new DisplayManager().createDisplay(tagInfo.type,{dummy:true});
	    if(display) {
		attrs = display.getWikiEditorTags();
		if(display.getTypeLabel) {
		    title = display.getTypeLabel() +" Properties";
		    let url = display.getTypeHelpUrl();
		    if(url) title = HU.href(url, title + SPACE + HU.getIconImage(icon_help),["target","_ramaddahelp"]);
		}
	    }
	} catch(e) {
	    console.log("Error getting attrs for:" + tagInfo.type +" error:" + e  + " stack:" +e.stack);
	}
	return {attrs:attrs,title:title,display:display};
    },
};



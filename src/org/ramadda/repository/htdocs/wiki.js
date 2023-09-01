/*
 * Copyright (c) 2008-2023 Geode Systems LLC
 */


var wikiPopup = null;
if(!window.WikiUtil) {
    window.WikiUtil = {
	initWikiEditor(entry, wikiId, textId,cbxId) {
            var textBlock = textId +"_block";
            var wikiBlock = wikiId +"_block";
            $("#" + cbxId).click(() => {
		var editor = WikiUtil.getWikiEditor(wikiId);
		var on  = $("#" + cbxId).is(':checked');
		if(on) {
                    $("#" + textBlock).css("display","none");
                    $("#" + wikiBlock).css("display","block");
                    var val = $("#" + textId).val();
                    editor.getEditor().setValue(val,8);
                    $("#" + wikiId).focus();
		} else {
                    var val = editor.getEditor().getValue();
                    $("#" + textId).val(val);
                    $("#" + textBlock).css("display","block");
                    $("#" + wikiBlock).css("display","none");
                    $("#" + textId).focus();
		}
            })
	},
	handleWikiEditorSubmit: function() {
            if (!this.wikiEditors) return;
            for (a in this.wikiEditors) {
		let editor= this.wikiEditors[a];
		editor.handleSubmit();
            }
	},
	getWikiEditor: function(id) {
            if (!this.wikiEditors) return null;
            return  this.wikiEditors[id];
	},
	addWikiEditor:function(editor,id) {
            if(!this.wikiEditors)  this.wikiEditors={};
            id = id || editor.getId();
            this.wikiEditors[id] = editor;
	},

	insertDisplayText:function(id, value) {
	    let type = window.globalDisplayTypesMap[value];
	    if(!type) {
		WikiUtil.insertText(value);
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
	    WikiUtil.insertText(id,t);
	},

	insertText:function(id, value) {
	    HtmlUtils.hidePopupObject();
	    var popup = HtmlUtils.getTooltip();
	    if(popup)
		popup.hide();
	    var handler = getHandler(id);
	    if (handler) {
		handler.insertText(value);
		return;
	    }
	    var editor = WikiUtil.getWikiEditor(id);
	    if (editor) {
		editor.insertAtCursor(value);
		return;
	    }
	    var textComp = GuiUtils.getDomObject(id);
	    if (textComp) {
		WikiUtil.insertAtCursor(id, textComp.obj, value);
	    }
	},



	insertAtCursor:function(id, myField, value) {
	    var editor = WikiUtil.getWikiEditor(id);
	    if(value.entryId && editor) {
		editor.handleEntryLink(value.entryId, value.name,null,false,value);
		return;
	    }


	    if (editor) {
		value = Utils.decodeText(value);    
		editor.insertAtCursor(value);
		return;
	    }

	    HtmlUtils.insertIntoTextarea(myField,value);
	},



	insertTags:function(id, tagOpen, tagClose, sampleText) {
	    HtmlUtils.hidePopupObject();
	    let handler = getHandler(id);
	    if (handler) {
		handler.insertTags(tagOpen, tagClose, sampleText);
		return;
	    }
	    let editor = WikiUtil.getWikiEditor(id);
	    if(editor) {
		editor.insertTags(tagOpen,tagClose, sampleText);
		return;
	    }
	    console.log("Could not find editor:" + id);
	},



	// apply tagOpen/tagClose to selection in textarea,
	// use sampleText instead of selection if there is none
	insertTagsInner:function(id, txtarea, tagOpen, tagClose, sampleText) {
	    let selText, isSample = false;
	    tagOpen = Utils.decodeText(tagOpen);
	    tagClose = Utils.decodeText(tagClose);    
	    var editor = WikiUtil.getWikiEditor(id);
	    if (editor) {
		editor=editor.getEditor();
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
    }
}

function  WikiEditor(entryId, formId, id, hidden,argOptions) {
    if(formId) {
	let _this = this;
	//If we are editing then listen for the submit and clear the editorChanged flag
	$('#' + formId).submit(()=>{
	    this.editorChanged = false;
	    return true;
	});
	$(window).bind('beforeunload', ()=>{
	    if(this.editorChanged) {
		return 'Changes have been made. Are you sure you want to exit?';
	    }
	});
    }

    this.entryId  = entryId;
    this.ID_WIKI_PREVIEW = "preview";
    this.ID_WIKI_PREVIEW_INNER = "preview_inner";
    this.ID_WIKI_PREVIEW_LIVE = "preview_live";
    this.ID_WIKI_PREVIEW_CONTENTS = "preview_contents";	
    this.ID_WIKI_PREVIEW_OPEN= "preview_open";
    this.ID_WIKI_PREVIEW_CLOSE = "preview_close";
    this.ID_WIKI_PREVIEW_WORDCOUNT = "preview_wordcount";
    this.ID_WIKI_PREVIEW_COPY = "preview_copy";
    this.ID_WIKI_PREVIEW_DOWNLOAD = "preview_download";
    this.ID_WIKI_MESSAGE = "message";
    this.ID_WIKI_MENUBAR   = "menubar";
    this.ID_WIKI_POPUP_EDITOR = "wiki-popup-editor";
    this.ID_WIKI_POPUP_OK= "wiki-popup-ok";
    this.ID_WIKI_POPUP_CANCEL= "wiki-popup-cancel";
    this.ID_WIKI_POPUP_TIDY ="wiki-popup-tidy";
    this.ID_WIKI_POPUP_COMPACT ="wiki-popup-compact";
    this.ID_LLM_INPUT = "llm-input";

    this.initAttributes();
    var options = {
        autoScrollEditorIntoView: true,
        copyWithEmptySelection: true,
        //            theme:'ace/theme/solarized_light',
    };
    if (argOptions)
        $.extend(options, argOptions);
    let authToken = options.authToken;
    if(options.authToken) delete options.authToken;

    $.extend(this,
	     {id:id,
	      authToken:authToken,
	      editor:ace.edit(id),
	      formId:formId,
	      hidden:hidden});
    this.myDiv =  $("#"+this.getId());
    WikiUtil.addWikiEditor(this);
    Utils.initDragAndDrop(this.getDiv(),
			  event=>{
			      let pos = this.lastPosition =  this.getEditor().renderer.screenToTextCoordinates(event.clientX, event.clientY);
			      let Range = ace.require('ace/range').Range;
			      let range =new Range(pos.row, pos.column, pos.row, pos.column+5);
			      if(this.dragMarker)    this.editor.session.removeMarker(this.dragMarker);
			      this.dragMarker = this.getEditor().session.addMarker(range, "ace_active_char", "text", false);
			  },
			  event=>{
			      this.clearDragAndDrop();
			  },
			  (event,item,result,wasDrop) =>{
			      this.clearDragAndDrop();

			      if(!wasDrop) {
				  this.lastPosition = this.getEditor().getCursorPosition();
			      }

			      Ramadda.handleDropEvent(event, item, result, this.entryId,this.authToken,
						      (data, entryid, name,isImage)=>{
							  this.handleEntryLink(entryid, name,this.lastPosition,true,{isImage:isImage});
						      });
			  });



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
    this.editor.container.addEventListener("mousedown", (e) =>{
    });
    this.editor.container.addEventListener("mouseup", (e) =>{
	this.handleMouseUp(e);
    });
    this.editor.container.addEventListener("dblclick", (e) =>{
	this.handleMouseUp(e,null, true);
    });    
    this.editor.container.addEventListener("mouseleave", (e) => {
	this.handleMouseLeave(e);
    });
    this.editor.container.addEventListener("mousemove", (e) => {
	this.handleMouseMove(e);
    });

    this.editor.getSession().on('change', (e)=> {
	this.editorChanged = true;
	if(this.previewShown && this.previewLive) {
	    if(this.previewPending) return;
	    this.previewPending = setTimeout(()=>{
		this.previewPending = null;
		this.doPreview(this.entryId,true);
	    },100);
	}

    });

    this.getBlock().find("#" + this.id).append(HU.div([STYLE,HU.css("display","none"), CLASS,"wiki-editor-message",ID,this.domId(this.ID_WIKI_MESSAGE)]));
    this.wikiInitDisplaysButton();
    this.initTagSearch();


    this.jq("previewbutton").click(()=>{
	HtmlUtils.hidePopupObject();
	this.doPreview(this.entryId);
    });
    this.jq("color").click((event)=>{
	HtmlUtils.hidePopupObject();
	this.doColor(event);
    });	
    this.jq("find").click(()=>{
	HtmlUtils.hidePopupObject();
	this.doFind();
    });	
    this.jq("wordcount").click(()=>{
	HtmlUtils.hidePopupObject();
	this.doWordcount();
    });	
    this.jq("rewrite").click(()=>{
	HtmlUtils.hidePopupObject();
	this.doLlm();
    });
    this.jq("transcribe").click(()=>{
	HtmlUtils.hidePopupObject();
	this.doTranscribe();
    });    
    this.jq("tidy").click(()=>{
	HtmlUtils.hidePopupObject();
	this.doTidy();
    });	    
}

WikiEditor.prototype = {
    getId:function() {
	return this.id;
    },

    domId:function(suffix) {
	return this.getId() +"_"+ suffix;
    },


    handleEntryLink:function(entryId, name,pos,isNew,opts) {
	let html =  HU.center(HU.b(name));
	if(isNew) {
	    html +="The new entry has been added. What do you want to insert into the document?<br>";
	} else {
	    html +="What do you want to insert into the document?<br>";
	}
	let what = [];
	if(isNew) {
	    if(opts.isImage) what.push("Image");
	    what.push("ID");
	    what.push("entry=ID");
	    what.push("Link");
	    what.push("Nothing");
	} else {
	    what.push("ID");
	    what.push("entry=ID");	    
	    what.push("Link");
	    what.push("Wiki Text");	    
	    what.push("Description");
	    if(opts.isImage) {
		what.push("Image");
	    }
	    if(opts.isGeo) 
		what.push("Map");
	    if(opts.entryType=="geo_editable_json") 
		what.push("Editable map");

	    if(opts.isGroup) {
		what.push("Tree");
		what.push("Grid");
		what.push("Gallery");	    
	    }

	    what.push("Import");	    
	}

	html += HU.select("",[ATTR_ID, this.domId("addtype")],what,this.lastWhat);
	html += "<p>";
	html += HU.div([STYLE,HU.css("display","inline-block"), CLASS,"ramadda-button",ID,this.domId("addok")],"OK")+
	    SPACE3 +
	    HU.div([STYLE,HU.css("display","inline-block"), CLASS,"ramadda-button",ID,this.domId("addcancel")],"Cancel");
	html = HU.div([STYLE,HU.css('padding','10px','width','400px')],html);
	let dialog = this.addDialog = HU.makeDialog({content:html,anchor:this.getScroller(),title:"Select Entry",header:true,
						     sticky:true,draggable:true,modal:true});
	let menu = dialog.find("#"+this.domId("addtype"));
	HU.initSelect(menu);
	dialog.find("#" +this.domId("addcancel")).button().click(()=>{
	    this.addDialog.remove();
	});

	dialog.find("#" +this.domId("addok")).button().click(()=>{
	    this.addDialog.remove();
	    let what=this.lastWhat=menu.val();
	    let text="";
	    let insert = text=>{
		if(pos) {
		    this.getEditor().session.insert(pos,text);
		}   else {
		    this.insertAtCursor(text);
		}
	    }
		
	    if(what=="Image") {
		text = "{{image entry=" + entryId+" #caption=\"" + name+"\" bordercolor=\"#ccc\" align=center width=75% }} ";
	    } else  if(what=="Map") {
		text = "{{map entry=" + entryId+" details=true}}";
	    } else  if(what=="Editable map") {
		text = "{{editable_map entry=" + entryId+" }}";
	    } else  if(what=="Tree") {
		text = "{{tabletree entry=" + entryId+" }}";
	    } else  if(what=="ID") {
		text = entryId;
	    } else  if(what=="entry=ID") {
		text = " entry=" +entryId+" ";		
	    } else  if(what=="Gallery") {
		text = "{{gallery entry=" + entryId+" }}";
	    } else  if(what=="Import") {
		text = "{{import entry=" + entryId+" }}";		
	    } else  if(what=="Grid") {
		text = "{{grid entry=" + entryId+" }}";			
	    } else if(what=="Wiki Text" || what=="Description") {
		let url = RamaddaUtils.getUrl("/entry/wikitext?entryid="+ entryId);
		if(what=="Description") url+="&usedescription=true";
		console.log(url);
		$.get(url, (data) =>{
		    data = String(data).replace(/^ *<wiki>\s/,'');
		    insert(data);
		}).fail(error=>{
		    alert("An error occurred:" + error);
		});
		return;
	    } else if(what=="Link") {
		text = "[[" + entryId +"|" + name+"]] ";
	    } else if(what=="Nothing") {
		return;
	    } else {
		text = " {{" + what.toLowerCase() +" entry=" + entryId+" }}";				
	    }
	    insert(text);
	});
    },



    clearDragAndDrop:function() {
	if(this.dragMarker)    this.getEditor().session.removeMarker(this.dragMarker);
    },

    getDiv:function() {
	return this.myDiv;
    },


    jq:function(suffix) {
	return $("#"+this.domId(suffix));
    },

    showMessage:function(msg) {
	this.jq(this.ID_WIKI_MESSAGE).css("display","block").html(msg);
    },

    clearMessage:function(msg) {
	this.jq(this.ID_WIKI_MESSAGE).css("display","none");
	this.showingEntryPopup=false;
    },

    getEditor:function() {
	return this.editor;
    },    

    getValue:function() {
	return this.getEditor().getValue();
    },
    setValue:function(text) {
	this.getEditor().setValue(text);
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
	    let popup = this.jq(this.ID_WIKI_POPUP_EDITOR)[0];
	    if(popup) {
		WikiUtil.insertAtCursor(null,popup , value);
		popup.focus();
		//We do this because the  SF menu stays popped up after clicking so we hide it
		//then after a second we remove the style so subsequent menu clicks will work
		this.jq(this.ID_WIKI_MENUBAR).find(".wiki-popup-menu-item").css("display","none");
		setTimeout(()=> {
		    this.jq(this.ID_WIKI_MENUBAR).find(".wiki-popup-menu-item").removeAttr("style");
		},1000);
		return;
	    }
	}
        let cursor = this.getEditor().getCursorPosition();
        this.getEditor().insert(value);
        this.getEditor().focus();
    },

    insertTags:function(tagOpen, tagClose, sampleText) {
	let selText, isSample = false;
	tagOpen = Utils.decodeText(tagOpen);
	tagClose = Utils.decodeText(tagClose);    
	/*
	if(sampleText) {
	    sampleText = Utils.decodeText(sampleText)+'\n';
	    if(tagOpen) tagOpen = tagOpen.trim()+'\n';
	}
        let text = (tagOpen??'') + (sampleText??'')+(tagClose??'') + " ";
	*/
        let text = (tagOpen??'') + (tagClose??'') + " ";
        let cursor = this.getEditor().getCursorPosition();
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

    getTagInfo:function(cursor) {
	let entryId;
	cursor = cursor || this.getEditor().getCursorPosition();
	let index = this.getSession().doc.positionToIndex(cursor);
	let text =this.getEditor().getValue();
	let left = -1;
	let right = -1;
	let gotBracket=false;
	let seenNewLine = false;
	let overToken='';
	let tmp = index;
	let idLeft=-1;
	let idRegExp = /[0-9a-f\-]/;
	while(tmp>=0) {
	    let c = text[tmp];
	    if(!c || !c.match(idRegExp)) break;
	    idLeft=tmp--;
	}
	let idRight=index;
	if(idLeft>=0) {
	    tmp = index+1;
	    while(tmp<text.length) {
		let c = text[tmp];
		if(!c.match(idRegExp)) {
		    break;
		}
		idRight=tmp++;
	    }
	}

	if(idLeft>=0) {
	    let tmp = text.substring(idLeft,idRight+1);
	    if(tmp.match(/[0-9a-f\-]+-[0-9a-f\-]+-[0-9a-f\-]+-[0-9a-f\-]+-[0-9a-f\-]+/)) {
		entryId = tmp;
	    }
	}

	tmp = index;
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
		var Range = ace.require('ace/range').Range;
		range  = new Range(startRow, startCol, endRow, endCol)
		
		return {
		    range:range,
		    entryId:entryId,
		    tag:tag,
		    type: type,
		    left:left,
		    right:right,
		    chunk:chunk,
		    attrs:attrs
		};
	    }
	}


	let prevIndex=index;
	if(text[prevIndex]=="\n") prevIndex--;
	while(prevIndex>=0 && text[prevIndex]!=="\n") {
	    prevIndex--;
	}

	if(prevIndex<0 && text[prevIndex+1]=="+") {
	    prevIndex=0;
	}
	if(prevIndex>=0) {
	    let substring = text.substring(prevIndex).trim();
	    //Check for the +<tag> 
	    if(substring.startsWith("+")) {
		let nlIndex = substring.indexOf("\n");
		if(nlIndex>=0)  substring = substring.substring(0,nlIndex);
		let length = substring.length;
		let attrs = substring.match("[^ ]+([^\n]*)$");
		if(attrs) attrs=attrs[1].trim();
		if(!substring) return null;
		let match = substring.match("\\+([^ \n]+)[ \n]");
		if(!match) return;
		substring = match[1]
		let Range = ace.require('ace/range').Range;
		return {
		    range:new Range(cursor.row, 0,cursor.row,length),
		    tag:substring,
		    type: "plus",
		    attrs:attrs||"",
		};
	    }
	}

	if(entryId) {
	    return {
		    entryId:entryId,
	    };
	}

	return null;
    },

    handleSubmit:function() {
	if($("#" + this.hidden).length==0) {
	    console.log("WikiEdit.handleSubmit: no hidden value");
	}
	$("#" + this.hidden).val(this.getEditor().getValue());
    },

    doFind:function(s) {
	this.getEditor().execCommand('find');
    },
    doWordcount:function(s) {
	if(s==null)
	    s = this.getEditor().getValue();
	s = s.replace(/<[^>]+>/g," ").replace(/<\/[^>]>/g," ").replace(/&nbsp;/g," ");
	s = Utils.split(s.replace(/[<>\n={}]/g," ")," ",true,true);
	alert("Approximately " + s.length +" words");
    },

    doTidy:function() {
	if(!confirm('This will remove blank links and trim each line. Continue?')) return;
	let text = this.getValue();
	let tmp = '';
	let append=(line,notTrim)=>{
	    if(notTrim)
		tmp+=line+'\n';
	    else
		tmp+=line.trim()+'\n';

	};
	let lineLength=80;
	let injs = false;
	let indent = 0;
	Utils.split(text,'\n').forEach(line=>{
	    if(line.startsWith("-javascript")) {
		injs = false;
		append(line);
		return;
	    }
	    if(line.startsWith("+javascript")) {
		injs = true;
		append(line);
		return;
	    }
	    if(injs) {
		//A poor man's JS formatting
		let _line = line.trim();
		let tabs ='';
		let checkIndent = true;
		if(_line.match(/\}+\);?/) || _line.match(/\} *,/)) {
		    indent--;
		    checkIndent = false;
		}
		if(indent<0) indent=0;
		new Array(indent).fill(0).forEach(v=>{tabs+='\t'});
		tmp+=tabs+_line+'\n';
		if(_line.startsWith('//') || !checkIndent) {
		    return;
		}

		if(_line.endsWith('{')) {
		    indent++;
		} else if(_line.endsWith('}') ||
			  _line.endsWith('});') ||
			  _line.endsWith('});')) {
		    indent--;
		}
		return;
	    }

	    if(line.length<lineLength) {
		append(line);
		return;
	    }
	    if(line.startsWith(":")) {
		append(line);
		return;
	    }
	    line = line.trim();
	    while(line.length>lineLength) {
		let prefix  = line.substring(0,lineLength);
		let suffix = line.substring(lineLength);		
		let idx = suffix.indexOf(' ');
		if(idx<0) {
		    append(line);
		    line='';
		    break;
		}
		append(prefix + suffix.substring(0,idx));
		line = suffix.substring(idx);
	    }
	    if(line!='')  append(line);
	});
	this.setValue(tmp);
    },

    transcribeStart:function() {
	if(!this.audioChunks) 	this.audioChunks = [];
	this.transcribePlaying = true;
	this.mediaRecorder.start();
	this.transcribeStartTime = new Date();
	this.jq('transcribe_play').html(HU.getIconImage('fa-solid fa-stop fa-gray'));
	let updateTime = ()=>{
	    let now = new Date();
	    let diff = now.getTime()-this.transcribeStartTime.getTime();
	    
	    let seconds = parseInt((this.transcribeElapsedTime+diff)/1000);
	    this.jq('transcribe_label').html(seconds +' seconds');
	    this.transcribeMonitor = setTimeout(updateTime,500);
	};
	this.transcribeMonitor = setTimeout(updateTime,500);
    },
    transcribeStop:function() {
	this.transcribePlaying = false;
	if(this.transcribeStartTime) {
	    let now = new Date();
	    let diff = now.getTime()-this.transcribeStartTime.getTime();
	    this.transcribeElapsedTime  += diff;
	}
	try {
	    this.mediaRecorder.stop();
	} catch(err){}
	if(this.transcribeMonitor) clearTimeout(this.transcribeMonitor);
	this.jq('transcribe_play').html(HU.getIconImage('fa-solid fa-microphone fa-gray'));
    },
    transcribeClear:function() {
	this.audioChunks = [];
	if(this.transcribeMonitor) clearTimeout(this.transcribeMonitor);
	this.transcribeElapsedTime = 0;
	try {
	    this.mediaRecorder.stop();
	} catch(err){}
    },

    transcribeDoIt:function() {
	this.callDoIt = false;
	if(!this.audioChunks || this.audioChunks.length==0) {
	    alert('No audio has been captured');
	    return
	}
	let file = new Blob(this.audioChunks, {
	    'type': this.transcribeMime
	});
	let formData = new FormData();
	let url = RamaddaUtils.getUrl("/llm/transcribe");
	let data = new FormData();
	this.jq('transcribe_loading').show();
	data.append('mimetype', this.transcribeMime);
	data.append('audio-file', file);
	data.append('entryid',this.entryId);
	if(this.jq('transcribe_addfile').is(':checked')) {
	    data.append('addfile','true');	    
	}
	$.ajax({
	    url: url,
	    data: data,
	    cache: false,
	    contentType: false,
	    processData: false,
	    method: 'POST',
	    type: 'POST',
	    success: (data)=>{
		this.jq('transcribe_loading').hide();
		this.transcribeClear();
		this.jq('transcribe_label').html('0 seconds');
		let results = data.results??data.error;
		if(!Utils.stringDefined(results)) results = "No results"
		this.jq('transcribe_text').val(results);
	    },
	    error: (data) =>{
		this.jq('transcribe_loading').hide();
		alert('transcription failed');
		console.log(data);
	    }
	});
    },
    doTranscribe:function() {
	if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
	    alert("media recording not supported");
	    return;
	}
	navigator.mediaDevices.getUserMedia({audio: true,}).then((stream) => {
	    this.transcribeMime = ['audio/mp3','audio/mp4','audio/wav', 'audio/webm','audio/mpeg']
		  .filter(MediaRecorder.isTypeSupported)[0];
	    if(!this.transcribeMime) {
		alert("No audio formats available");
		return;
	    }
	    this.mediaRecorder = new MediaRecorder(stream, {mimeType: this.transcribeMime});
	    this.mediaRecorder.addEventListener("dataavailable", event => {
//		console.log("data event");
		this.audioChunks.push(event.data);
	    });
	    this.mediaRecorder.addEventListener("stop", () => {
//		console.log("stop event");
		if(this.callDoIt) this.transcribeDoIt();
	    });
	    let html = '';
	    let controls =HU.hbox([
		HU.span(['title','Start recording','class','ramadda-clickable','id',this.domId('transcribe_play')],HU.getIconImage('fa-solid fa-microphone fa-gray')),
		HU.span(['title','Transcribe recording','class','ramadda-clickable','id',this.domId('transcribe_pen')],HU.getIconImage('fa-solid fa-pen fa-gray')),
		HU.b(' Time: ')+
		    HU.div(['style',HU.css('display','inline-block','text-align','right','width','150px','xborder','var(--basic-border)'),'id',this.domId('transcribe_label')],'0 seconds'),
		HU.span(['title','Delete recording','class','ramadda-clickable','id',this.domId('transcribe_delete')],HU.getIconImage('fa-solid fa-delete-left fa-gray')),
	    ],HU.css('margin-right','5px'));

	    html+=HU.leftRightTable(controls,
				    HU.checkbox(this.domId('transcribe_addfile'),
						['id',this.domId('transcribe_addfile'),
						 'title','Add audio file as entry'],
						false,'Add file'));
	    html+=HU.div(['style','position:relative;'],
			 HU.textarea('','',['placeholder','','id',this.domId('transcribe_text'), 'rows',6,'cols',80, 'style','border:var(--basic-border);padding:4px;margin:4px;font-style:italic;'])+
			 HU.div(['style','display:none;position:absolute;top: 50%; left: 50%; -ms-transform: translate(-50%, -50%); transform: translate(-50%, -50%);','id',this.domId('transcribe_loading')],
				HU.image(RamaddaUtil.getCdnUrl('/icons/mapprogress.gif'),['style','width:100px;'])));


	    html+=HU.buttons([HU.span(['class','ramadda-dialog-button','append','true'],"Append"),
			      HU.span(['class','ramadda-dialog-button',ID,this.domId("cancel")],"Cancel")]);
	    html = HU.div(['class','ramadda-dialog'],html);
	    let closeCallback =()=>{
		this.transcribeClear();
	    };
	    let dialog = this.transcribeDialog = HU.makeDialog({content:html,anchor:this.getScroller(),
								my: "left bottom",     
								at: "left+200" +" top-50",
								title:"Transcribe",
								callback:closeCallback,
								header:true,sticky:true,draggable:true,modal:false});	
	    this.transcribeElapsedTime = 0;
	    this.jq('transcribe_pen').click(()=>{
		if(this.transcribePlaying) {
		    this.callDoIt = true;
		    this.transcribeStop();
		} else {
		    this.transcribeDoIt();
		}
	    });
	    this.jq('transcribe_play').click(()=>{
		if(this.transcribePlaying) {
		    this.transcribeStop();
		} else {
		    this.transcribeStart();

		}
	    });
	    this.jq('transcribe_delete').click(()=>{
		this.jq('transcribe_text').val('');
		this.transcribeStop();
		this.transcribeClear();
		this.jq('transcribe_label').html('0 seconds');
	    });


	    let _this = this;
	    dialog.find('.ramadda-dialog-button').button().click(function() {
		let val = _this.jq('transcribe_text').val()??'';
		if ($(this).attr('append')) {
		    _this.getEditor().session.insert(_this.getEditor().getCursorPosition(), val.trim());
		} else {
		    dialog.remove();
		    closeCallback();
		}
	    });
	}).catch((err) => {
	    alert(`Error initializing transcription: ${err}`);
	});
    },
    doLlm:function() {
	if(!this.addedLlmListener) {
	    this.editor.getSession().selection.on('changeSelection', ()=> {
		let text = this.getEditor().getSelectedText();
		if(Utils.stringDefined(text) && !this.llmReplacing) {
		    this.jq(this.ID_LLM_INPUT).val(text);
		}
	    });
	}
	    


	let llmText = this.getEditor().getSelectedText()??'';
	let options = [{value:'',label:'Select prompt'}];
	//Some of these prompts are from https://github.com/f/awesome-chatgpt-prompts
	let prompts =
	    [{label:'Rewrite for college reader',
	      prompt:'Rewrite the following text for a college educated reader:'},
	     {label:'Rewrite for 5th grader',
	      prompt:'Rewrite the following text for a 5th grade reader:'},
	     'Can you give a short description of the below topic:',
	     'Write a comprehensive guide for the below topic:', 
	     'Write a blog post on the below topic:',
	     {label:'Make a lesson plan for the below topic',prompt:'I need help developing a lesson plan on the below topic for college studentss. The topic is:'},

	     {label:'Check spelling',prompt:'List all of the words in the following text that are misspelled:'},
	     {label:'Translate to English',
	      prompt:'I want you to act as an English translator, spelling corrector and improver. I will speak to you in any language and you will detect the language, translate it and answer in the corrected and improved version of my text, in English. I want you to replace my simplified A0-level words and sentences with more beautiful and elegant, upper level English words and sentences. Keep the meaning same, but make them more literary. I want you to only reply the correction, the improvements and nothing else, do not write explanations.'}];
	prompts.forEach((prompt,idx)=>{
	    if(prompt.label) {
		options.push({value:idx,label:prompt.label});
	    } else {
		options.push({value:idx,label:prompt});
	    }		
	 });
	let promptMenuContainerId = HU.getUniqueId();
	/*
	  todo: maybe add a menu of common prompts?
	  */
	let html= 
	    HU.formTable() +
	    HU.formEntry('Prompt:',HU.div(['id',promptMenuContainerId]))+
	    HU.formEntry('','Or enter prompt:') +
	    HU.formEntry('Prompt prefix:',HU.textarea('',this.lastPromptPrefix??'',
						      ['class','wiki-llm-input','style','width:500px;','id',this.domId('llm-prompt-prefix'),'rows',5])) +
	    HU.formEntry('Prompt suffix:',
			 HU.input('',this.lastPromptSuffix??'',['class','wiki-llm-input','style','width:500px;','id',this.domId('llm-prompt-suffix')])) +
	    HU.formTableClose();
	html+=HU.textarea('',llmText,['placeholder','Enter input or select text in editor','id',this.domId(this.ID_LLM_INPUT), 'rows',6,'cols',80, 'style','border:var(--basic-border);padding:4px;margin:4px;font-style:italic;']);

	html+='<br>';
	html+=HU.span(['id',this.domId('llm-call')],'Evaluate');	    
	
	html+=HU.div(['style','position:relative;'],
		   HU.textarea('','',['placeholder','Results','id',this.domId('rewrite-results'), 'rows',6,'cols',80, 'style','border:var(--basic-border);padding:4px;margin:4px;font-style:italic;'])+
		   HU.div(['style','display:none;position:absolute;top: 50%; left: 50%; -ms-transform: translate(-50%, -50%); transform: translate(-50%, -50%);','id',this.domId('llm-loading')],
			  HU.image(RamaddaUtil.getCdnUrl('/icons/mapprogress.gif'),['style','width:100px;'])));



	html += HU.buttons([HU.span(['class','ramadda-dialog-button','replace','true'],"Replace"),
			HU.span(['class','ramadda-dialog-button','append','true'],"Append"),
			HU.span(['class','ramadda-dialog-button',ID,this.domId("cancel")],"Cancel")]);
			 html = HU.div(['class','ramadda-dialog'],html);

			 let dialog = this.llmDialog = HU.makeDialog({content:html,anchor:this.getScroller(),
				    my: "left bottom",     
				    at: "left+200" +" top-50",
				    title:"LLM",
				    header:true,sticky:true,draggable:true,modal:false});	


	let call = (prompt) =>{
	    llmText = this.jq(this.ID_LLM_INPUT).val()??'';
	    this.jq('llm-loading').show();
	    let url = RamaddaUtils.getUrl("/llm/rewrite");
	    let args = {usegpt4:true,
			text:llmText};
	    if(prompt) {
		args.promptprefix = prompt;
	    } else {
		args.promptprefix=this.lastPromptPrefix=this.jq('llm-prompt-prefix').val();
		args.promptsuffix=this.lastPromptSuffix=this.jq('llm-prompt-suffix').val();
	    }
	    $.post(url,args,
		   data=>{
		       _this.jq('llm-loading').hide();
		       if(!Utils.stringDefined(data.result)) {
			   alert('No result');
			   return;
		       }
		       let result = data.result.trim();
		       this.jq('rewrite-results').val(result);
		   }).fail(data=>{
		       _this.jq('llm-loading').hide();
		       alert('Rewrite failed');
		   });
	}
	let makePromptMenu = () =>{
	    let promptMenu = HU.select("",['id', this.domId('llmprompts')],options);
	    jqid(promptMenuContainerId).html(promptMenu);
	    this.jq('llmprompts').change(function() {
		let idx = $(this).val();
		let prompt = prompts[idx];
		if(!prompt) {
		    return;
		}
		let val = prompt.prompt ?? prompt;
		if(!Utils.stringDefined(val)) {
		    return;
		}
		call(val);
		$(this).val('');
	    });
	}

	makePromptMenu();


	dialog.find('.wiki-llm-input').keypress(function(e){
	    if(e.keyCode == 13) {
		call();
	    }
	});
	this.jq('llm-call').button().click(()=>{
	    call();
	});
	let _this = this;
	dialog.find('.ramadda-dialog-button').button().click(function() {
	    _this.llmReplacing = true;	    
	    let val = _this.jq('rewrite-results').val();
	    if(!val) return;
	    if($(this).attr('replace')) {
		_this.getEditor().session.replace(_this.getEditor().selection.getRange(), val);
	    } else if ($(this).attr('append')) {
		_this.getEditor().session.insert(_this.getEditor().getCursorPosition(), val);
	    } else {
		dialog.remove();
	    }
	    _this.llmReplacing = false;	    
	});

	


    },    

    doColor: function (event) {
	let html = HU.tag("input",['style',HU.css('width','100px','height','100px'), 'type','color','id',this.domId('color_picker')]);
	html+= HU.div(['class','ramadda-buttons'],
		      HU.span([ID,this.domId("color_apply")],"Apply") + SPACE1 +
		      HU.span([ID,this.domId("color_ok")],"Ok") + SPACE1 +
		      HU.span([ID,this.domId("color_cancel")],"Cancel"));

	html = HU.div(['class','ramadda-dialog'],html);
	if(this.colorDialog) {
	    this.colorDialog.remove();
	}
	this.colorDialog = HU.makeDialog({content:html,anchor:this.getDiv(),
				    my: "left bottom",     
				    at: "left+200" +" top-50",
				    title:"Select Color",
				    header:true,sticky:true,draggable:true,modal:false});	
	let picker = this.jq('color_picker');
	let close = () =>{
	    picker.attr('type','text').attr('type','color');
	    picker.remove();
	    if(this.colorMarker)    this.editor.session.removeMarker(this.colorMarker);
	    this.colorDialog.remove();
	    this.colorDialog = null;
	};
	let apply = () =>{
            let pos = this.getEditor().getCursorPosition();
	    let val = picker.val();
	    this.insertAtCursor(val);
	    let Range = ace.require('ace/range').Range;
	    let range =new Range(pos.row, pos.column, pos.row, pos.column+val.length);
	    if(this.colorMarker)    this.editor.session.removeMarker(this.colorMarker);
//	    this.colorMarker = this.getEditor().session.addMarker(range, "ace_selected", "text", false);
	};
	this.jq('color_apply').button().click(()=>{
	    apply();
	});
	this.jq('color_ok').button().click(()=>{
	    apply();
	    close();
	});
	this.jq('color_cancel').button().click(()=>{
	    close();
	});	
	setTimeout(()=>{
	    picker.trigger('click');
	});
    },

    doPreview:async function (entry,  inPlace) {
	let id = this.getId();
	let area = $("#" + id);
	if(!inPlace) {
	    //the left is set to 100px
	    let width =$(window).width()-200;
	    $("#" + this.domId(this.ID_WIKI_PREVIEW)).remove();
	    $(HU.div([ID, this.domId(this.ID_WIKI_PREVIEW), CLASS,"wiki-editor-preview"],"")).appendTo(this.getBlock());
	    let preview = $("#" + this.domId(this.ID_WIKI_PREVIEW));
	    preview.css("width",width+"px");
	    this.previewShown = true;
	}
	let wikiCallback = (html,status,xhr) =>{
	    let _this = this;
	    if(inPlace) {
		this.jq(this.ID_WIKI_PREVIEW_INNER).html(html);
	    } else {
		let left = Utils.join([
		    HU.span([CLASS, 'ramadda-clickable',ID,this.domId(this.ID_WIKI_PREVIEW_OPEN)], HtmlUtils.getIconImage('fa-sync',['title','Preview Again'])),
		    HU.checkbox('',[TITLE,'Live Preview',ID,this.domId(this.ID_WIKI_PREVIEW_LIVE)],this.previewLive,'Live')+SPACE4,
		    HU.span([CLASS, 'ramadda-clickable',TITLE,'Wordcount',ID,this.domId(this.ID_WIKI_PREVIEW_WORDCOUNT)], HtmlUtils.getIconImage('fa-calculator')),

		    HU.span([CLASS, 'ramadda-clickable',TITLE,'Copy',ID,this.domId(this.ID_WIKI_PREVIEW_COPY)], HtmlUtils.getIconImage('fa-copy')),
		    HU.span([CLASS, 'ramadda-clickable',TITLE,'Download',ID,this.domId(this.ID_WIKI_PREVIEW_DOWNLOAD)], HtmlUtils.getIconImage('fa-download')),		    
		],SPACE2);

		let right  = HU.span([CLASS, 'ramadda-clickable',ID,this.domId(this.ID_WIKI_PREVIEW_CLOSE)], HtmlUtils.getIconImage('fa-window-close',['title','Close Preview']));

		let bar = HtmlUtils.div([CLASS,'ramadda-menubar','style','xtext-align:center;width:100%;border:1px solid #ccc'],
					HU.leftRight(left,right));

		html = HtmlUtils.div([ID,this.domId(this.ID_WIKI_PREVIEW_INNER), CLASS,'wiki-editor-preview-inner'], html);
		html = bar + html;
		let preview = $("#"+this.domId(this.ID_WIKI_PREVIEW));
		try {
		    preview.html(html).show();
		} catch(err) {
		    preview.html(HU.getErrorDialog("An error occurred:" + err));
		}
		preview.draggable();
		preview.resizable({handles: 'ne,nw'});	    
		this.jq(this.ID_WIKI_PREVIEW_WORDCOUNT).click(function() {
		    _this.doWordcount(_this.jq(_this.ID_WIKI_PREVIEW_INNER).html());
		});

		this.jq(this.ID_WIKI_PREVIEW_COPY).click(function() {
		    let s = _this.jq(_this.ID_WIKI_PREVIEW_INNER).html();
		    Utils.copyToClipboard(s);
		    alert("Text is copied to clipboard");
		});
		this.jq(this.ID_WIKI_PREVIEW_DOWNLOAD).click(function() {
		    let s = _this.jq(_this.ID_WIKI_PREVIEW_INNER).html();
		    Utils.makeDownloadFile('preview.html',s);
		});				
		this.jq(this.ID_WIKI_PREVIEW_LIVE).click(function() {
		    _this.previewLive = $(this).is(':checked');
		});
		this.jq(this.ID_WIKI_PREVIEW_OPEN).click(() =>{
		    this.doPreview(entry,true);
		});
		$("#" + this.domId(this.ID_WIKI_PREVIEW_CLOSE)).click(() =>{
		    this.previewShown = false;
		    $("#"+ this.domId(this.ID_WIKI_PREVIEW)).hide();
		});		
	    }
	}
	let wikiError = (html,status,xhr) =>{
	    console.log("error fetching preview:" + html);
	};
	let text = this.getValue();
	let url = ramaddaBaseUrl + "/wikify";
	$.post(url,{
	    doImports:"false",
	    entryid:entry,
	    wikitext:text},
	       wikiCallback).fail(wikiError);
    },

    getAttributeBlocks:function(tagInfo, forPopup, finalCallback) {
	if(!tagInfo) {
	    return null;
	}

	let { attrs, title, display }  = this.getDisplayAttributes(tagInfo);
	let callback =a => {
	    this.getAttributeBlocks(tagInfo, forPopup, finalCallback);
	};

	let wikiAttrs = this.getWikiAttributes(tagInfo,callback);
	//Callback later
	if(wikiAttrs===false) return false;
	if(wikiAttrs) {
	    attrs = Utils.mergeLists(attrs, wikiAttrs);
	}
	if(!attrs) return null;
	let blocks  = getWikiEditorMenuBlocks(attrs,forPopup,this.getId());
	//	if(display) {
	let ctItems =  Utils.getColorTablePopup(this, true);
	blocks.push({title:"Color table",items:ctItems});
	//	}


	if(!title) {
	    title = null;
	    if(this.wikiAttributesFromServer &&
	       this.wikiAttributesFromServer[tagInfo.tag] &&
	       this.wikiAttributesFromServer[tagInfo.tag][0]) {
		title = this.wikiAttributesFromServer[tagInfo.tag][0].label;
	    }
		
	    if(!title)   {
		title = Utils.makeLabel(tagInfo.tag) +" Properties";
	    }
	}
	
	let r = {blocks:blocks, title:title,display:display};
	if(finalCallback) finalCallback(r);
	return r;
    },


    handleEntriesPopup:function(ids,id,prefix) {
	this.getEntryNames(ids,(data)=>{
	    if(data.length) {
		let html = prefix??'';
		data.forEach(d=>{
		    html+=(d.icon?HU.getIconImage(d.icon)+HU.space(1):'')+
			HU.href(ramaddaBaseUrl+"/entry/show?entryid=" + d.id,
				d.name,['title',d.id,'target','_entries'])+"<br>";
		});
		jqid(id).html(html);
	    }
	},true);
    },


    handlePopupMenu:function(event, result) {
	let tagInfo = this.getTagInfo();
	if(!tagInfo) {
	    return;
	}

	if(!tagInfo.chunk) return

	if(!result) {
	    this.getAttributeBlocks(tagInfo,true, (r)=>{
		this.handlePopupMenu(event,r);
	    });
	    return;
	}

	let blocks = result.blocks;
	let title = result.title;
	let display = result.display;

	if(blocks.length==0) return;
	let menu =  HU.open('div',[CLASS,'wiki-editor-popup','style','min-width:400px;']);
	if(!title) {
	    title = Utils.makeLabel(tagInfo.tag) +' Properties';
	}
	let search =  HU.span(['id',this.domId('searchattributes'),CLASS,'wiki-popup-menu-header ramadda-clickable','title','Search attributes'],HU.getIconImage('fa-binoculars'));
	let edit =  HU.span(['id',this.domId('edittag'),CLASS,'wiki-popup-menu-header ramadda-clickable','title','Edit tag'],HU.getIconImage('fas fa-edit'));
	menu += HU.div([CLASS,'wiki-editor-popup-heading'], search+'' + edit +' '  +title);
	let ids = this.extractEntryIds(tagInfo.chunk);
	if(ids.length) {
	    let id = Utils.getUniqueId('entrieslist');
	    menu +=HU.toggleBlock('Entries', HU.div([ID,id,CLASS,'wiki-editor-popup-items'],'Loading...'));
	    this.handleEntriesPopup(ids,id);
	}	


	Utils.splitList(blocks,5).forEach(blocks=>{
	    menu += HU.open('div',[CLASS,'wiki-editor-popup-section']);
	    blocks.forEach(block=>{
		if(typeof block=='string') {
		    menu+=block;
		    return;
		}
		let title = block.title;
		title = HU.div([CLASS,'wiki-editor-popup-header'], title)
		let contents  = block.items.join('');
		contents = HU.div([CLASS,'wiki-editor-popup-items'],contents);
		menu +=HU.toggleBlock(block.title, contents);
	    });
	    menu += HU.close('div');
	});
	menu += '</div>';
	let dialog = HU.makeDialog({content:menu,anchor:$(window),
				    my: 'left top',     at: 'left+' +event.x +' top+' + (event.y),
				    title:title,header:true,sticky:true,draggable:true,modal:false});	
	HtmlUtils.setPopupObject(dialog);

	jqid(this.domId('edittag')).click(()=>{
	    dialog.remove();
	    this.showTagEdit(tagInfo,result);
	});

	jqid(this.domId('searchattributes')).click(()=>{
	    dialog.remove();
	    this.makeSearchAttributesDialog(this.toolbar,blocks,false);
	});


    },


    handleMouseUp:function(e,result,doubleClick) {
	let _this = this;
	let position = this.getEditor().renderer.screenToTextCoordinates(e.x, e.y);
	if(!position) return;
	let tagInfo = this.getTagInfo(position);
	if(!tagInfo) return;
	if(!e.metaKey && !doubleClick)  {
	    if(tagInfo.entryId) {
		if(e.shiftKey)  {
		    let url = RamaddaUtils.getEntryUrl(tagInfo.entryId);
		    window.open(url,'_entry');
		} else {
		    this.showEntryPopup(tagInfo.entryId);
		}
	    }
	    return;
	}

	if(!result) {
	    this.getAttributeBlocks(tagInfo,false, r=>{
		this.handleMouseUp(e,r,doubleClick);
	    });
	    return
	}
	this.showTagEdit(tagInfo,result);
    },
    showTagEdit:function(tagInfo,result) {
	let _this = this;
	//xxxx
	let blocks = result.blocks;
	let title = result.title;
	let display = result.display;
	//	let attrs = Utils.parseAttributes(tagInfo.attrs||"");
	let contents = tagInfo.attrs;

	let prefix = tagInfo.type|| tagInfo.tag;
	let id = this.domId(this.ID_WIKI_MENUBAR);
	let menubar = getWikiEditorMenuBar(blocks,id, prefix);


	let width =$(window).width()-100;
	let html = HU.div([ID,this.domId("wiki-editor-popup"),CLASS,"wiki-editor-editor"],
			  menubar +
			  HU.textarea("",contents,["spellcheck","false",STYLE,HU.css('width',width+'px','height','300px'),ID,this.domId(this.ID_WIKI_POPUP_EDITOR),"xrows","10","xcols","120"]) + "<br>" +
			  HU.div([STYLE,HU.css("text-align","center","padding","4px")],
				 HU.span([TITLE,"Tidy the text",ID,this.domId(this.ID_WIKI_POPUP_TIDY)],HU.getIconImage("fas fa-broom")) + SPACE1 +
				 HU.span([TITLE,"Compact the text",ID,this.domId(this.ID_WIKI_POPUP_COMPACT)], HU.getIconImage("fas fa-snowplow")) + SPACE1 +								 HU.span([ID,this.domId(this.ID_WIKI_POPUP_OK)],"Ok") + SPACE1 +
				 HU.span([ID,this.domId(this.ID_WIKI_POPUP_CANCEL)],"Cancel")));

	let dialog = HU.makeDialog({content:html,anchor:this.getScroller(),title:title,header:true,
				    sticky:true,draggable:true,modal:true,modalContentsCss:HU.css('left','50px')});
	jqid('searchattributes').click(function(){
	    _this.makeSearchAttributesDialog($(this),blocks,true);
	});

	this.popupShowing = true;
	this.jq(this.ID_WIKI_POPUP_EDITOR).focus();

	this.jq(this.ID_WIKI_POPUP_CANCEL).button().click(()=>{
	    this.popupShowing = false;
	    dialog.remove();
	});

	let tidyfunc =tidy=>{
	    let val = this.jq(this.ID_WIKI_POPUP_EDITOR).val().trim();
	    let attrs = Utils.parseAttributesAsList(val);
	    let nval = '';
	    let cnt = 0;
	    for(let i=0;i<attrs.length;i+=2) {
		let v =  attrs[i+1];
		if(v!=null && (v.indexOf(" ")>=0 || v.indexOf("\n")>=0 || v=="")) v = "\""  + v +"\"";
		nval+=attrs[i] +"=" +v;
		if(tidy) {
		    nval+="\n";
		}  else {
		    cnt++;		
		    if(cnt>4) {
			cnt=0
			nval+="\n";
		    } else {
			nval+="  ";
		    }
		}
	    }
	    this.jq(this.ID_WIKI_POPUP_EDITOR).val(nval);	    
	}
	this.jq(this.ID_WIKI_POPUP_TIDY).button().click(()=>{
	    tidyfunc(true);
	});

	this.jq(this.ID_WIKI_POPUP_COMPACT).button().click(()=>{
	    tidyfunc(false);
	});
	

	this.jq(this.ID_WIKI_POPUP_OK).button().click(()=>{
	    let val = this.jq(this.ID_WIKI_POPUP_EDITOR).val();
	    let tag =  tagInfo.tag;
	    //A hack
	    if(tag == "display") {
		if(tagInfo.type == "group") tag  = "group";
		else if(tagInfo.type !="") tag = "display_" + tagInfo.type;
	    }
	    let text;
	    if(tagInfo.type=="plus") {
		val  = val.replace(/\n/g," ");
		text = "+" +tag +" " + val +" ";
	    } else {
		text = "{{" +tag +" " + val +"}}";
	    }
	    this.popupShowing = false;
	    dialog.remove();
	    this.getSession().replace(tagInfo.range, text);
	});	    
    },
    makeSearchAttributesDialog:function(anchor,blocks,model) {
	let _this = this;
	let all = "";
	blocks.forEach((block,idx)=>{
	    if(typeof block=="string") {
		return;
	    }
	    if(block.title=='Color table') return;
	    let title = block.title;
	    all+=HU.div(['class','wiki-searchheader','data-block-index',block.index],HU.b(title));
	    all+="<div>";
	    let items = Utils.join(block.items," ");
	    items = items.replace(/<div/g,'<span').replace(/\/div>/g,'/span>');		
	    all+=items;
	    all+="</div>";
	});
	all = HU.center(HU.input('','',['placeholder','Search','id',_this.domId('allsearch'),'width','10'])) +
	    HU.div(['id',_this.domId('allsearch_corpus'),'style',HU.css('width','500px','max-height','400px','overflow-y','auto')], all);
	all  = HU.div(['style','margin:5px;'], all);
	let dialog = HU.makeDialog({content:all,anchor:anchor,title:"Attributes",header:true,
				    sticky:true,draggable:true,modal:model});
	let commands = jqid(_this.domId('allsearch_corpus')).find('span');
	let headers = jqid(_this.domId('allsearch_corpus')).find('.wiki-searchheader');	
	jqid(_this.domId('allsearch')).focus();
	jqid(_this.domId('allsearch')).keyup(function(event) {
	    let text = $(this).val().trim().toLowerCase();
	    let seen = {};
	    commands.each(function() {
		if(text=='') {
		    $(this).show();
		} else {
		    let corpus = $(this).attr('data-corpus');
		    if(!corpus) return;
		    corpus =  corpus.toLowerCase();
		    if(corpus.indexOf(text)>=0) {
			$(this).show();
			seen[$(this).attr('data-block-index')] = true;
		    } else {
			$(this).hide();
		    }
		}
	    });
	    if(text=='') {
		headers.show();
	    } else {
		headers.each(function(){
		    if(seen[$(this).attr('data-block-index')])
			$(this).show();
		    else
			$(this).hide();
		});

	    }
	});
    },
    handleMouseLeave:function(e) {
	this.setInContext(false);
    },
    extractEntryIds:function(chunk) {
	let regex =  /([a-z0-9]+-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]+)(.*)/;
	let ids = [];
	let match = chunk.match(regex);
	while(match) {
	    if(match.length==3) {
		ids.push(match[1]);
		match = match[2].match(regex);
	    } else {
		break;
	    }
	}
	return ids;
    },

    getEntryNames:function(ids, callback, immediate) {
	let func = ()=>{
	    let url = ramaddaBaseUrl+ "/entry/names?entryids=" + Utils.join(ids,",");
	    this.getNamesPending = true;
	    $.getJSON(url, (data) =>{
		this.getNamesPending = false;
		callback(data);
	    });
	};
	if(immediate) {
	    func();
	    return;
	}
	this.getNamesTimeout =
	    setTimeout(()=> {
		func();
	    },1000);
    },
    
    showEntryPopup:function(entryId) {
	let entryDiv = HU.getUniqueId('');
	let message= HU.div(['id',entryDiv,'style',HU.css('display','inline-block')]);

	this.showMessage(message);
	this.showingEntryPopup=true;
	this.handleEntriesPopup([entryId],entryDiv,'Shift-click to view:<br>');
    },

    setInContext:function(c,type,chunk,tagInfo) {
	this.inContext = c;
	let scroller = this.getScroller();
	if(this.tagMarker)
	    this.editor.session.removeMarker(this.tagMarker);
	this.tagMarker = null;
	if(this.getNamesTimeout)
	    clearTimeout(this.getNamesTimeout);
	this.getNamesTimeout = null;
	if(tagInfo && tagInfo.entryId) {
	    this.showEntryPopup(tagInfo.entryId);
	    return;
	}
	if(tagInfo && !tagInfo.chunk) return;
	if(c) {
	    scroller.css("cursor","context-menu");
	    let message= "Right-click to show property menu";
	    if(type!="plus")
		message+="<br>Double click to edit";
	    this.showMessage(message);
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
		if(tagInfo.entryId && this.showingEntryPopup)
		    return;
		tagInfo.entryId = null;
		this.setInContext(true, tagInfo.type,tagInfo.chunk,tagInfo);
  	    } else {
		this.setInContext(false);
	    }
	};
	if(this.inContext) {
	    func();
	} else {
	    this.lastTimeout=setTimeout(func,1000);
	}
    },

    getWikiAttributes:function(tagInfo, callback) {
	if(this.wikiAttributes[tagInfo.tag]) {
	    return this.wikiAttributes[tagInfo.tag];
	}
	let merge = (list) =>{
	    if(!list) list = [];
	    list = list.map(item=>{
		if(!item.p) return item;
		//strip off the comments that can come from the server
		if(String(item.p).startsWith('#')) {
		    item.p = String(item.p).substring(1);
		}
		return item;
	    });
	    if(this.wikiAttributes[tagInfo.tag+'_extra']) {
		list=Utils.mergeLists(list,this.wikiAttributes[tagInfo.tag+'_extra']);
	    }		
	    return list;
	}
	if(!this.wikiAttributesFromServer) {
	    let url = RamaddaUtils.getUrl('/wikitags');
	    $.getJSON(url, (data) =>{
		this.wikiAttributesFromServer= data;
		callback(merge(this.wikiAttributesFromServer[tagInfo.tag]));
	    });
	    return false;
	}
	return merge(this.wikiAttributesFromServer[tagInfo.tag]);
    },

    getDisplayAttributes:function(tagInfo) {
	let title = null;
	let attrs = [];
	let display = null;
	if(tagInfo.type=="plus") return {attrs:null,title:null,display:null};
	try {
	    display = new DisplayManager().createDisplay(tagInfo.type,{dummy:true});
	    if(display) {
		attrs = display.getWikiEditorTags();
		if(display.getTypeLabel) {
		    title = display.getTypeLabel();
		    if(title==null) {
			title = Utils.makeLabel(tagInfo.type||"");
		    }


		    title =  title +" Properties";
		    let url = display.getTypeHelpUrl();
		    if(url) title = HU.href(url, title + SPACE + HU.getIconImage(icon_help),["target","_ramaddahelp"]);
		}
	    }
	} catch(e) {
	    console.log("Error getting attrs for:" + tagInfo.type +" error:" + e  + " stack:" +e.stack);
	}
	return {attrs:attrs,title:title,display:display};
    },

    
    wikiInitDisplaysButton:function() {
	if(!window.globalDisplayTypes) return;
	let id = this.getId();
	let button = $("#displays_button"+ id);
        let types = window.globalDisplayTypes;
        let links = {};
        let cats = [];
        let displayTypes = [];
	DISPLAY_CATEGORIES.forEach(category=>{
	    links[category] = [];
	    cats.push(category);
	});
	types.forEach(type=>{
	    if (Utils.isDefined(type.forUser) && !type.forUser) {
		return;
	    }
	    let category = type.category ||  CATEGORY_MISC;
	    if (links[category] == null) {
                links[category] = [];
                cats.push(category);
	    }
	    let tooltip = type.tooltip||"";
	    tooltip = tooltip.replace(/"/g,"&quot;");
	    let click = "WikiUtil.insertDisplayText('" + id + "','" + type.type+"')";
	    let link = HU.div(['data-category',category,'data-corpus',type.label+' ' + tooltip,CLASS,"wiki-editor-popup-link"],HU.href("#",type.label,[CLASS,"display-link ",TITLE,tooltip,"onclick", click]));
	    links[category].push(link);
        });
	let menuTags = '';
	let menu = "<table><tr valign=top>";
        for (let i = 0; i < cats.length; i++) {
	    let cat = cats[i];
	    let menuItems = Utils.join(links[cat],"<div>\n");
	    menuTags+=  HU.div([ATTR_STYLE,"vertical-align:top;margin-right4px;display:inline-block;"],
			       Utils.join(links[cat],""));	    
	    menu += HU.td(['data-category',cat,CLASS,'wiki-editor-display-category'],HU.div([STYLE,'margin-right:5px;'], HU.b(cat)) +"<div style='margin-right:5px;max-height:250px;overflow-y:auto;'>" + menuItems);
        }
	menu = HU.div([ID,"wiki-display-popup",STYLE,"font-size:10pt;width:800px;"], menu);
	this.displaysText = menuTags;


	menu = HU.center(HU.input('','',['placeholder','Search','id',this.domId('displaysearch'),'width','10'])) +menu;
	menu = HU.div(['style','margin:10px;'], menu);

	button.click(() =>{
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
	    popup.tooltip();

	    
	    let headers = popup.find('.wiki-editor-display-category');
	    let _this = this;
	    let displayLinks = popup.find('.wiki-editor-popup-link');
	    this.jq('displaysearch').focus();
	    this.jq('displaysearch').keyup(function(event) {
		let text = $(this).val().trim().toLowerCase();
		let seen = {};
		displayLinks.each(function() {
		    if(text=='') {
			$(this).show();
		    } else {
			let corpus = $(this).attr('data-corpus');
			if(!corpus) return;
			corpus =  corpus.toLowerCase();
			if(corpus.indexOf(text)>=0) {
			    $(this).show();
			    seen[$(this).attr('data-category')] = true;
			} else {
			    $(this).hide();
			}
		    }
		});
		if(text=='') {
		    headers.show();
		} else {
		    headers.each(function(){
			if(seen[$(this).attr('data-category')])
			    $(this).show();
			else
			    $(this).hide();
		    });
		}
	    });

	    wikiPopup =  popup;
	});
	
    },

    initTagSearch:function() {
	let popup = '';
	if(!this.formId) return;
	$('#' + this.formId).find('.wiki-menubar-tags').each(function(){
	    if(popup!='')popup+='<thin_hr>';
	    popup+=HU.center(HU.b($(this).attr('data-title')));
	    popup+=$(this).html();
	});
	if(this.displaysText)
	    popup+='<thin_hr>' +
	    HU.center(HU.b("Data Displays")) +this.displaysText;
	popup =HU.center(HU.input('','',['placeholder','Find Tag','id', this.domId('tagsearch'),
					 'style','margin-top:4px;','width','10'])) + popup;
	popup = HU.div([ATTR_STYLE,HU.css('width','600px',
					  'height','300px',
					  'max-height','300px','overflow-y','auto')],popup);

	let _this = this;
	$('#'+this.id+'_toolbar_search').click(function(){
	    if(this.tagSelectDialog) this.tagSelectDialog.remove();
	    this.tagSelectDialog =
		HU.makeDialog({content:popup,anchor:$(this),at:'left+300 bottom+40',title:"Tags",header:true,draggable:true});
	    _this.jq('tagsearch').focus();

	    this.tagSelectDialog.find('a').tooltip({
		    classes: {"ui-tooltip": "wiki-editor-tooltip"},
		    content: function () {
			return $(this).prop('title');
		    },
		    show: { effect: 'slide', delay: 500, duration: 400 },
		position: { my: "left top", at: "right top" }
	    });


	    this.tagSelectDialog.find('.wiki-editor-popup-category').css('display','none');
	    let tags = this.tagSelectDialog.find('.wiki-editor-popup-link');
//	    let tags = this.tagSelectDialog.find('a');	    
	    _this.jq('tagsearch').keyup(function(event) {
		let text = $(this).val().trim().toLowerCase();
		HU.doPageSearch(text,tags);
	    });



	});




    },
    initAttributes:function() {
	this.groupAttributes = [
	    {label:'Collection Properties'},
	    {p:'sortby',ex:'name|date|changedate|createdate|entryorder|number',tt:'sort type -name,date, change date, create date'},
	    {p:'sortdir',ex:'up|down',tt:'direction of sort. use up for oldest to youngest'},
	    {label:'Specify entries',p:'entries',ex:'\"entryid1,entryid2,entryid3..\"',tt:'comma separated list of entry ids to use' },
	    {label:'Specify entries by search',p:'entries',ex:'search:type:<some type>;orderby:date;ascending:false',tt:'comma separated list of entry ids to use' },	    
	    {p:'entries.filter',ex:'file|folder|image|type:some type|geo|name:name pattern|suffix:file suffixes',tt:'allows you to select what entries to use'},
	    {p:'exclude',ex:'entryid1,entryid2,entryid3..',tt:'comma separated list of entry ids to not use'},
	    {p:'first',ex:'entryid1,entryid2,entryid3..',tt:'comma separated list of entry ids to use first'},
	    {p:'last',ex:'entryid1,entryid2,entryid3..',tt:'comma separated list of entry ids to use last'},
	    {p:'max',ex:'number of entries to use',tt:'max number of entries to use'},
	];

	this.wikiAttributesFromServer = null;
	let treeAttrs = Utils.mergeLists([
	    {label:'Table Tree'},
	    {p:"simple",ex:true},
	    {p:"showHeader",ex:false},
	    {p:"showDate",ex:false},
	    {p:"showCreateDate",ex:false},
	    {p:"showSize",ex:false},
	    {p:"showType",ex:false},
	    {p:"showIcon",ex:false},
	    {p:"showThumbnails",ex:false},
	    {p:"showArrow",ex:false},
	    {p:"showForm",ex:false},
	    {p:"showCrumbs",ex:true},
	    {p:'message',ex:''},
	    {p:'treePrefix',ex:''},
	    {p:'addPageSearch',d:true,tt:'Add the page search form'},
	    {p:'chunkSize',ex:'10',tt:'break up the list of entries into chunkSize lists and display each list'},
	    {p:'numChunks',ex:'2',tt:'how many entry chunks'},
	    {p:'chunkColumns',ex:'2',tt:'how many columns use \"numChunks\" to match number of chunks'},
	    {p:'chunkStyle',ex:'margin:10px;',tt:'chunk style'}
	],this.groupAttributes);
	this.wikiAttributes = {
	    tree: treeAttrs,	
	    tabletree: treeAttrs,
	    gallery_extra:this.groupAttributes,
	    links: Utils.mergeLists([
		{label:'Links Properties'},
		{p:'info',ex:'List children entries'},
		{p:'showIcon',ex:'false'},
		{p:'showDescription',ex:'true'},
		{p:'showSnippet',ex:'true'},		
		{p:'linkresource',ex:'true',tt:'Link to the resource'},
		{p:'linksBefore',ex:'url1;label1,url2;label2'},
		{p:'linksAfter',ex:'url1;label1,url2;label2'},		
		{p:'separator',ex:'',tt:'Separator between links'},
		{p:'horizontal',ex:'true',tt:'Display horizontallly'},
		{p:'output',ex:'',tt:'Link to output'},
		{p:'tagopen',ex:'html before link'},
		{p:'tagclose',ex:'html after link'},	
		{p:'linksBefore',ex:'"url;label,url;label"'},
		{p:'linksAfter',ex:'"url;label,url;label"'},		
		{p:'innerClass',ex:''},
		{p:'class',ex:'',tt:'link css class'},
		{p:'style',ex:'',tt:'link style'},
		{p:'chunkSize',ex:'10',tt:'break up the list of entries into chunkSize lists and display each list'},
		{p:'numChunks',ex:'2',tt:'how many entry chunks'},
		{p:'chunkColumns',ex:'2',tt:'how many columns use \"numChunks\" to match number of chunks'},
		{p:'chunkStyle',ex:'margin:10px;',tt:'chunk style'}		
	    ],
				    this.groupAttributes),		   
	    section: [
		{label:'Section Properties'},
		{p:'title',ex:'"{{name}}"'},
		{p:'subTitle',ex:'""'},
		{p:'style',ex:'""'},
		{p:'titleStyle',ex:'""'},
		{p:'----',tt:'Add a line'},
		{p:'#',tt:'Colored sections'}				
	    ],
	    row: [
		{label:'Row Properties'},
		{p:'tight',ex:'true',tt:'Layout rows with no space'}
	    ],
	    list: Utils.mergeLists([
		{label:'List Properties'},
		{p:'info',ex:':List children entries'},
		{p:'showIcon',ex:'false'},
		{p:'showDescription',ex:'true'},
		{p:'showSnippet',ex:'true'},		
		{p:'linkresource',ex:'true',tt:'Link to the resource'},
		{p:'separator',ex:'',tt:'Separator between links'},
		{p:'output',ex:'',tt:'Link to output'},
		{p:'tagopen',ex:'',tt:'html before link'},
		{p:'tagclose',ex:'',tt:'html after link'},	
		{p:'innerClass',ex:''},
		{p:'class',ex:'',tt:'link css class'},
		{p:'style',ex:'',tt:'link style'},
		{p:'chunkSize',ex:'10',tt:'break up the list of entries into chunkSize lists and display each list'},
		{p:'numChunks',ex:'2',tt:'how many entry chunks'},
		{p:'chunkColumns',ex:'2',tt:'how many columns use \"numChunks\" to match number of chunks'},
		{p:'chunkStyle',ex:'margin:10px;',tt:'chunk style'}],
				   this.groupAttributes),

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
		{p:'showTitle',ex:'false'},		
	    ],


	    map: [
		{label:'Map Properties'},
		{p:'icon',ex:'#/icons/dots/green.png' },
 		{p:'width',ex:'100%'},
		{p:'height',ex:'400'},
		{p:'listentries',ex:'true'},
		{p:'hideIfNoLocations',tt:"Don't show map if no georeferenced data"},
		{p:'details',ex:'false'},

		{p:'showLines',ex:'true'},
		{p:'showBounds',ex:'false'},
		{p:'showMarkers',ex:'false'},
		{p:'showCameraDirection',ex:'false'},		
		{p:'showLocationSearch',ex:'true'},
		{p:'showCheckbox',ex:'true'},
		{p:'showSearch',ex:'false'},
		{p:'showLayerToggle',ex:'true'},
		{p:'showLatLonLines',ex:'true'},
		{p:'showScaleLine',ex:'true'},
		{p:'showLayerSwitcher',ex:'true'},
		{p:'showLatLonPosition',ex:'true'},
		{p:'showZoomPanControl',ex:'true'},
		{p:'showZoomOnlyControl',ex:'true'},
		{p:'showBookmarks',ex:'true'},
		{p:'showBounds',ex:'true'},
		{p:'showOpacitySlider',ex:'true'},
		{p:'useThumbnail',ex:'true'},		

		{p:'iconSize',ex:'32'},
		{p:'iconWidth',ex:'32'},
		{p:'iconHeight',ex:'32'},				

		{p:'layerStrokeColor',ex:'red'},
		{p:'layerStrokeWidth',ex:'2'},				
		{p:'layerFillColor',ex:'red'},
		{p:'layerFillOpacity',ex:'0.5'},				

		{p:'rules',ex:'attr1:== or != or &lt; or &gt; or  ~ (like):value:fillColor:red:strokeColor:black;attr2:',
		 tt:'Specify style rules based on feature properties'},
		{p:'pointRadius',ex:''},
		{p:'strokeColor',ex:''},
		{p:'strokeWidth',ex:''},
		{p:'fillOpacity',ex:''},
		{p:'fillColor',ex:''},
		{p:'layerStrokeColor',ex:''},
		{p:'layerStrokeWith',ex:''},
		{p:'layerFillColor',ex:''},
		{p:'layerFillOpacity',ex:''},
		{p:'highlightStrokeColor',ex:''},
		{p:'highlightFillColor',ex:''},
		{p:'highlightStrokeWidth',ex:''},
		{p:'highlightFillOpacity',ex:''},


		{p:'popupWidth',ex:'200'},
		{p:'popupHeight',ex:'200'},				
		{p:'doPopupSlider',ex:'true'},
		{p:'popupSliderRight',ex:'true'},
		{p:'enableDragPan',ex:'false'},
		{p:'addMarkerOnClick',ex:'true'},
		{p:'markerIcon',ex:'/repository/icons/map/marker-blue.png'},		
		{p:'linked',ex:'true'},
		{p:'linkMouse',ex:'true'},
		{p:'linkGroup',ex:'some name'},		


		{p:'layer',ex:'osm|esri.topo|google.roads|google.hybrid|esri.street|opentopo|usfs|caltopo.mapbuilder|usgs.topo|google.terrain|google.satellite|naip|usgs.imagery|esri.shaded|esri.lightgray|esri.darkgray|esri.terrain|shadedrelief|publiclands|historic|esri.aeronautical|osm.toner|osm.toner.lite|cartolight|watercolor|lightblue|blue|white|black|gray'},
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
	    ], this.groupAttributes),

	    grid: Utils.mergeLists([
		{label:'Grid Properties'},
		{p:'tag',ex:'card'},
		{p:'width',ex:'300'},
		{p:'inner-height',ex:'100'},
		{p:'columns',ex:'3'},
		{p:'showIcon',ex:'true'},
		{p:'weights',ex:''},
		{p:'showSnippet',ex:'false'},
		{p:'showSnippetHover',ex:'true'},
		{p:'showLink',ex:'false'},
		{p:'showHeading',ex:'true'},
		{p:'showLine',ex:'true'},
		{p:'showSnippetHover',ex:'true'},		
		{p:'showPlaceholder',ex:'true',tt:'Show placeholder image'},
		{p:'addTags',ex:'true'},
		{p:'tagTypes',ex:'content.keyword'},
		{p:'addPageSearch',d:true,tt:'Add the page search form'},
		{p:'captionPrefix',ex:'Click to view example: ',tt:'To use for popup images'}],
				   this.groupAttributes),
	    frames: Utils.mergeLists([
		{label:'Frames Properties'},
		{p:'width',ex:'400'},
		{p:'height',ex:'400'},
		{p:'showIcon',ex:'false'},
		{p:'noTemplate',ex:'true',tt:'Don\'t use the page template in the frame'}],
				     this.groupAttributes),
	    accordian: Utils.mergeLists([
		{label:'Accordian Properties'},
		{p:'tag',ex:'html'},
		{p:'collapse',ex:'false'},
		{p:'border',ex:'0'},
		{p:'showLink',ex:'true'},
		{p:'includeIcon',ex:'false'},
		{p:'textposition',ex:'left'},],
					this.groupAttributes),

	    table: Utils.mergeLists([
		{label:'Table Properties'},
		{p:'showEntryDetails',ex:'false'},
		{p:'columns',ex:'name,file,createdate,changedate,fromdate,todate,#entryField1,#entryField2'},
		{p:'tableOrdering',ex:'true'},
		{p:'showCategories',ex:'true'},
		{p:'showDate',ex:'true'},
		{p:'showCreateDate',ex:'true'},
		{p:'showChangeDate',ex:'true'},
		{p:'showColumns',ex:'false'},		
		{p:'show&lt;column name&gt',ex:'false',tt:'show or hide the given column'},
		{p:'nameLabel',tt:'Override the name label'}],
				    this.groupAttributes),
	    
	    recent: Utils.mergeLists([
		{label:'Recent Properties'},
		{p:'info',ex:'',tt:'List N days recent entries by day'},

		{p:'days',ex:'num days'},],
				     this.groupAttributes),


	    multi: [
		{label:'Multi Properties'},
		{info:'Create multiple wiki tags'},
		{p:'_tag',ex:'tag to create'},
		{p:'_template',ex:'',tt:'wiki text template. Escape { and } with backslash'},
		{p:'_entries',ex:'entries',tt:'entries to apply to'},
		{p:'_headers',ex:'comma separated headers'},
		{p:'_headerTemplate',ex:':heading {{name link=true}}'},
		{p:'_columns',ex:'number of columns'},
		{p:'_width',ex:'200',tt:'Set the width and flow the blocks'},
		{p:'_style',ex:'padding:10px;',tt:'Style for each block'},		
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
    }
}



function getWikiEditorMenuBlocks(attrs,forPopup,id) {
    let blocks = [];
    let block = null;
    let addBlock = (title)=> {
	let items = [];
	blocks.push(block={index:blocks.length,title:title,items:items});
    };
    let itemClass = "ramadda-menu-item " + (forPopup?" ramadda-hoverable ramadda-highlightable ":"")
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
	    block.items.push(HU.div([CLASS, itemClass], "<i>"+tag.info+"</i>"));
	    return;
	}
	if(!tag.p)  {
	    console.log("Unknown arg:" + JSON.stringify(tag));
	    return;
	}
	let label = tag.label||tag.p;
	let attr = "";
	if(Utils.isDefined(tag.ex))
	    attr=String(tag.ex);
	else if(Utils.isDefined(tag.d))
	    attr=String(tag.d);
	else
	    attr="";
	attr = attr.trim();
	if(attr.indexOf(" ")>=0) {
	    attr = '"' + attr +'"';
	} else if(attr =="") {
	    attr = '"' + attr +'"';
	}
	attr=" " +tag.p+"=" + attr +" ";
	attr  =attr.replace(/\"/g,"&quot;");
	if(block) {
	    let corpus = label +" " + (tag.tt??"");
	    if(id)
		label = HtmlUtils.onClick("WikiUtil.insertText('" + id +"','"+attr+"')",label);
	    block.items.push(HU.div(['data-block-index',block.index,'data-corpus',corpus,CLASS,itemClass,TITLE,tag.tt||"",'data-attribute',attr], label));
	} else {
	    console.log("no attribute block");
	}
    };
    attrs.forEach(attr=>{
	processAttr(attr);
    });



    return blocks;
}

function getWikiEditorMenuBar(blocks,id, prefix) {
    let menu  = "";
    menu += HU.tag(TAG_LI, [],HU.div(['id','searchattributes',CLASS,"wiki-popup-menu-header ramadda-clickable",'title','Search attributes'],HU.getIconImage('fa-binoculars')));
    blocks.forEach((block,idx)=>{
	if(typeof block=="string") {
	    //	    console.log(block);
	    //TODO: this is the color tables
	    //		menu+=block;
	    return;
	}
	let title = block.title;
	//remove the display name from the title of the menu 
	if(prefix && title.toLowerCase().startsWith(prefix)) {
	    let tmp = Utils.makeLabel(title.substring(prefix.length).trim());
	    if(tmp!="") title = tmp;
	}
	if(block.items.length==0) return
	let sub = Utils.wrap(block.items,"<li>","");
	menu += HU.tag(TAG_LI, [],HU.div([CLASS,"wiki-popup-menu-header"],title) + HU.tag("ul", [CLASS,"wiki-popup-menu-item"], sub));
    });
    let menubar = HU.div([CLASS,"wiki-popup-menubar",  ATTR_ID, id],
			 HU.tag("ul", [ATTR_ID, id+"_inner", ATTR_CLASS, "sf-menu"], menu))
    return menubar;
}

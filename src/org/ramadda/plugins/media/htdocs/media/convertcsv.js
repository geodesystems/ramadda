
function  ConvertForm(inputId, entry) {
    const ID_SETTINGS  = "settings";
    const ID_HELP  = "help";    
    const ID_MENU = "menu";
    const ID_INPUT = "input";
    const ID_OUTPUTS = "outputs";
    const ID_TABLE = "table";
    const ID_PROCESS = "process";
    const ID_CLEAR = "clear";
    const ID_LIST = "list";
    const ID_TRASH = "trash";
    const ID_OUTPUT = "output";
    const ID_SCRATCH = "scratch";
    const ID_POPUP = "popup";
    const ID_ADDCOMMAND = "addcommand";
    const ID_CANCELCOMMAND = "cancelcommand";
    const ID_PRE = "csvpre";

    $.extend(this,{
	entry:entry,
	editor:null,
	inputId: inputId||"convertcsv_input",
	baseId: inputId||"convertcsv_input",
	applyToSiblings:false,
	save: true,
	doCommands:true,
	commands:null,
	commandsMap:null,
	header:null,
	maxRows:30,
	dbPopupTime:null});
	     

    $.extend(this,{
	domId: function(id) {
	    return this.baseId+"_"+id;
	},
	jq: function(id) {
	    return $("#"+this.domId(id));
	},
	init: function(){
	    let _this  =this;
	    let text = $("#" + this.baseId +"_lastinput").html();
	    if(text!=null) {
		text = text.replace(/_escnl_/g,"\n").replace(/_escquote_/g,'&quot;').replace(/_escslash_/g,"\\").replace(/\"/g,'&quot;');
	    }  else {
		text = this.getInput(true)||"";
	    }
	    let html = "";
	    html += "<style type='text/css'>";
	    html += ".convert_button {padding:2px;padding-left:5px;padding-right:5px;}\n.ramadda-csv-table  {font-size:10pt;}\n ";
	    html += ".convert_add {margin-left:10px; cursor:pointer;}\n";
	    html += ".convert_add:hover {text-decoration:underline;}\n";
	    html += ".ace_gutter-cell {cursor:pointer;}\n";
	    html += ".ace_editor {margin-bottom:5px;xheight:200px;}\n";
	    html += ".ace_editor_disabled {background:rgb(250,250,250);}\n";
	    html += ".ace_csv_comment {color:#B7410E;}\n";
	    html += ".ace_csv_command {color:blue;}\n";
	    html += "</style>";
	    let topLeft = HU.div([ID,this.domId(ID_MENU),"style","display:inline-block;"],"");
	    let topRight =  HU.span([CLASS,"ramadda-clickable",ID,this.domId(ID_SETTINGS),TITLE,"Settings",STYLE,HU.css("cursor","pointer")],HU.getIconImage("fa-cog")) +SPACE2 +
		HtmlUtil.span([ID,this.domId(ID_HELP),CLASS,"ramadda-clickable", TITLE,"Help"], HtmlUtils.getIconImage("fa-question-circle"))+SPACE2;
	    
	    html += HU.div(["class","ramadda-menubar","style","width:100%;"],HU.leftRightTable(topLeft,topRight));
//	    let textarea = HtmlUtil.textarea("",text,[STYLE,"position:absolute;width:100%;", ID,this.domId(ID_INPUT), "rows", "5"]);
	    let input = HtmlUtil.div([STYLE,"height:100%;top:0px;right:0px;left:0px;bottom:0px;position:absolute;width:100%;", ID,this.domId(ID_INPUT), "rows", "5"], text);	    
	    html+=HU.div([ID,this.domId("resize"),STYLE,"margin-bottom:5px;height:200px;position:relative;"],input);
	    let left ="";
	    left += HtmlUtil.span([ID,this.domId(ID_OUTPUTS),CLASS,"convert_button"], "Outputs") +" ";
	    left += HtmlUtil.span([ID,this.domId(ID_TABLE),CLASS,"convert_button", TITLE,"Display table (ctrl-t)"],"Table")+" ";
	    left += HtmlUtil.span([ID,this.domId(ID_PROCESS),CLASS,"convert_button", TITLE,"Process entire file"],"Process")+" ";	    
	    let right = "";
	    right += HtmlUtil.span([ID,this.domId(ID_CLEAR),CLASS,"ramadda-clickable", TITLE,"Clear output"],HU.getIconImage("fa-eraser")) +SPACE2;
	    right += HtmlUtil.span([ID,this.domId(ID_LIST),CLASS,"ramadda-clickable", TITLE,"List temp files"],HU.getIconImage("fa-list")) +SPACE2;
	    right += HtmlUtil.span([ID,this.domId(ID_TRASH),CLASS,"ramadda-clickable", TITLE,"Remove temp files"],HU.getIconImage("fa-trash")) +SPACE2;	    	    


	    html += HtmlUtil.leftRightTable(left,right);
	    html +=  HtmlUtil.div([ID, this.domId(ID_OUTPUT),STYLE,HU.css("margin-top","5px", "max-height","500px","overflow-y","auto")],"");
	    html += HtmlUtil.div([ID, this.domId(ID_SCRATCH)],"");

	    $("#" +  this.inputId).html(html);
	    this.jq("resize").resizable({
		handles: 's',
		stop: (event, ui) =>{
		    $(this).css("width", '');
		    this.editor.resize();
		},
	    });
	    this.makeEditor();



	    this.jq(ID_TRASH).click(()=>{
		this.call('',{clearOutput:true});
	    });
	    this.jq(ID_LIST).click(()=>{
		this.call('',{listOutput:true});
	    });

	    this.jq(ID_CLEAR).click(()=>{
		this.output("");
	    });

	    this.jq(ID_TABLE).button().click(()=>{
		this.display('-table',null,true);
	    });
	    this.jq(ID_PROCESS).button().click(()=>{
		this.display('',true);
	    });				   


	    this.jq(ID_OUTPUTS).button().click(function(){
		let html = _this.outputCommands.reduce((acc,cmd)=>{
		    if(cmd.command=="-table") return acc;
		    acc+=HU.div([CLASS,"ramadda-clickable","command",cmd.command,TITLE,cmd.command],cmd.description);
		    return acc;
		},"");
		html = HU.div(["style",HU.css("margin","10px")],html);
		let dialog = HU.makeDialog({content:html,anchor:$(this)});
		dialog.find(".ramadda-clickable").click(function(){
		    let command = $(this).attr("command");
		    dialog.remove();
		    _this.display(command);

		});
	    });

	    this.jq(ID_HELP).click((e)=>{
		this.call("-help");
	    });
	    this.jq(ID_SETTINGS).click(function(e){
		let html ="";
		html += "Rows: " + HtmlUtil.input("",_this.maxRows,["size","2", ID,_this.domId("maxrows")]) +"<br>";
		html += HtmlUtil.checkbox("",[ID,_this.domId("applytosiblings")], _this.applyToSiblings) + " Apply to siblings" +"<br>";
		html +=  HtmlUtil.checkbox("",[ID,_this.domId("save")],_this.save) +" Save" +"<br>";
		html += HtmlUtil.checkbox("",[ID,_this.domId("docommands")],_this.doCommands) +" Do commands";
		html = HU.div(["style",HU.css("margin","10px")],html);

		let dialog = HU.makeDialog({content:html,my:"right top",at:"right bottom",xtitle:"",anchor:$(this),draggable:true,header:true,inPlace:false});
		HU.onReturnEvent("#" + _this.domId("maxrows"),input=>{
	 	    _this.maxRows = input.val();
		    dialog.remove();
		});

		_this.jq("save").click(function() {
		    _this.save =  $(this).is(':checked');
		});
		_this.jq("applytosiblings").click(function() {
		    _this.applyToSiblings =  $(this).is(':checked');
		});
		_this.jq("docommands").click(function() {
		    _this.doCommands =  $(this).is(':checked');
		    if(_this.editor) {
			if(_this.doCommands) {
			    _this.editor.container.classList.remove("ace_editor_disabled")
			} else {
			    _this.editor.container.classList.add("ace_editor_disabled");
			}
		    }
		});
	    });


	    $(document).click((e)=> {
		if(this.dbPopupTime) {
		    let now = new Date();
		    let timeDiff = now-this.dbPopupTime.getTime();
		    if(timeDiff<1000)  {
			return;
		    }
		}
		let popup = this.jq(ID_POPUP);
		popup.css("display","none");
	    });

	    let helpUrl = this.getUrl("-helpjson");
	    let jqxhr = $.getJSON( helpUrl, (data) =>{
		if(data.error!=null) {
		    return;
		}
		if(Utils.isDefined(data.result)) {
		    let result = window.atob(data.result);
		    result= JSON.parse(result);
		    this.commands = result.commands;
		    this.commandsMap = {}
		    let docs ="";
		    let menus = [];
		    let fileSelect =  HU.div([ID,"convertcsv_file1_selectlink", "class","ramadda-highlightable  ramadda-menubar-button"], "Select file");
		    let categories = {};
		    let category="";
		    this.outputCommands = [];
		    this.commands.forEach(cmd=>{
			let command = cmd.command;
			if(cmd.isCategory) {
			    docs+="etlcat {" + cmd.description+"}\n";
			    category = cmd.description;
			    categories[category] = menuItems = [];
			    menus.push(HU.div(["class","ramadda-highlightable ramadda-menubar-button","category",category], category));
			    return;
			}
			if(!command || !command.startsWith("-") || command.startsWith("-help")) return;
			if(category=="Output") {
			    if(cmd.args.length==0) {
				this.outputCommands.push(cmd);
				return;
			    }
			}
			let tooltip = "";
			let desc = cmd.description;
			if(desc && desc!="") {
			    tooltip =desc+"\n";
			}
			tooltip += command;
			let docArgs="";
			cmd.args.forEach(a=>{
			    let desc = a.desc;
			    docArgs+=" {{" +a.id+"} {" + (a.description||"") +"}} "
			    if(desc!="") desc = " "  + desc;
			    tooltip +=" <" +a.id  +desc+ "> ";
			});
			tooltip = tooltip.replace(/\"/g,"&quot;").replace(/\(/g,"").replace(/\)/g,"");
			let label = cmd.label ||  Utils.camelCase(cmd.command.replace("-",""));
			docs+="etl " + "{" + command +"} {"  + label+"} {" +  desc + "} "   + docArgs +"\n"
			this.commandsMap[command] = cmd;
			menuItems.push(HU.div([TITLE,desc||"",CLASS, "ramadda-hoverable ramadda-clickable","command",command],label));
		    });
		    menus.push(fileSelect);
		    //		Utils.makeDownloadFile("etlcommands.tcl",docs);
		    let menuBar=menus.join("");
		    this.jq("menu").html(HU.div([],menuBar));
		    this.jq("menu").find(".ramadda-menubar-button").click(function() {
			let cat = $(this).attr("category");
			let button = $(this);
			if(!cat || !categories[cat]) {
			    selectInitialClick(event,'convertcsv_file1',_this.domId('input'),'true','entry:entryid','" + this.entry+"');
			    return;
			}
			let items = Utils.splitList(categories[cat],10);
			items = items.map(list=>{
			    return list.join("");
			});
			let menu = Utils.wrap(items,"<div style='vertical-align:top;margin:5px;display:inline-block;'>","</div>");
			let dialog = HU.makeDialog({content:menu,anchor:$(this)});
			dialog.find(".ramadda-clickable").click(function(event) {
			    let cmd = _this.commandsMap[$(this).attr("command")];
			    dialog.remove();
			    if(!cmd) {
				return;
			    }
			    _this.addCommand(cmd,null,button);
			});
		    });
		}
		

	    })
		.fail(function(jqxhr, textStatus, error) {
		});
	},
	output: function(html) {
	    this.jq('output').html(html);
	},
	insertTags(tagOpen, tagClose, sampleText) {
	    this.insertText(tagOpen);
	},
	makeEditor:  function() {
	    let _this = this;
	    this.editor = ace.edit(this.domId(ID_INPUT));
	    HU.addWikiEditor(this,this.domId(ID_INPUT));
	    this.editor.setBehavioursEnabled(true);
	    this.editor.setDisplayIndentGuides(false);
	    this.editor.setKeyboardHandler("emacs");
	    this.editor.setShowPrintMargin(false);
	    this.editor.getSession().setUseWrapMode(true);
	    let options = {
		autoScrollEditorIntoView: true,
		copyWithEmptySelection: true,
	    };
	    this.editor.setOptions(options);
	    this.editor.session.setMode("ace/mode/csvconvert");
	    this.editor.on("guttermousedown", (e)=> {
		if(e.domEvent.ctrlKey) {
		    let row = +e.getDocumentPosition().row+1;
		    let lines =this.editor.getValue().split("\n");
		    let text = "";
		    for(let i=0;i<row;i++) {
			text+=lines[i]+"\n";
		    }
		    this.display("-table",null,true,text);
		    this.gutterMouseDown = true;
		}
	    });
	    this.editor.commands.addCommand({
		name: "keyt",
		exec: function() {
		    _this.display('-table',null,true);
		},
		bindKey: {mac: "ctrl-t", win: "ctrl-t"}
	    })
	    this.editor.commands.addCommand({
		name: "keyh",
		exec: function() {
		    _this.display('-printheader',null,true);
		},
		bindKey: {mac: "ctrl-h", win: "ctrl-h"}
	    })

	    this.editor.container.addEventListener("mouseup", (e) =>{
		if(!e.metaKey)  return;
		this.handleMouseUp(e);
	    });

	    this.editor.container.addEventListener("contextmenu",(e)=> {
		if(e.ctrlKey) {
		    e.preventDefault();
		    return;
		}
		this.handleMouseUp(e,null,true);
	    });

	    addHandler({
		selectClick: function(selecttype, id, entryId, value) {
		    _this.insertText("entry:" + entryId);
		}
	    },this.inputId);
	},


	handleMouseUp(e,result) {
	    e.preventDefault();
	    let cursor = this.editor.getCursorPosition();
	    let index = this.editor.session.doc.positionToIndex(cursor);
	    let text = this.editor.getValue();
	    let menu = "";
	    let tmp = index;
	    let left = -1;
	    let right = -1;
	    let lastWasChar=false;
	    let lastWasHash=false;
	    while(tmp>=0) {
		let c = text[tmp];
		if (c == "-") {
		    if(lastWasChar) {
			left = tmp;
			break;
		    }
		}
		if(c=="\n" && lastWasHash) {
		    break;
		}
		lastWasChar = String(c).match(/[a-zA-Z]/);
		//	    if(c==" " || c=="\n" || c =="\"" || c=="{" || c=="}") break;
		tmp--;
		lastWasHash = (c=="#") ;
	    }

	    if(left>=0) {
		tmp = left;
		while(tmp<text.length) {
		    right  = tmp;
		    let c = text[tmp];
		    if (c == " " || c=="\n") {
			break;
		    }
		    tmp++;
		}
		if(right<0) return;
		let command = this.commandsMap[text.substring(left,right)];
		if(!command) return;
		let values = [];
		let tok = null;
		let append = (c)=>{
		    if(tok == null) tok = c;
		    else tok+=c;
		}
		let end = (c)=>{
		    if(tok != null) values.push(tok);
		    tok = null;
		}
		let inQuote = false;
		let bracketCnt = 0;
		let inEscape = false;
		while(right<text.length && values.length<command.args.length) {
		    let c = text[right++];
		    if(c=="\\") {
			if(inEscape)
			    append(c);
			else
			    inEscape = true;
			continue;
		    }
		    if(inEscape) {
			append(c);
			inEscape = false;
			continue;
		    }

		    if(bracketCnt==0) {
			if(c=="{") {
			    if(inQuote) {
				append(c);
			    } else {
				bracketCnt++;
			    }
			    continue;
			}
			if(c=="\"") {
			    inQuote = !inQuote;
			    if(!inQuote) {
				if(tok==null) tok="";
				end();
			    }
			    continue;
			}
			if(!inQuote && (c==" " || c=="\n")) {
			    end();
			} else {
			    append(c);
			}
			continue;
		    } else {
			if(c=="}") {
			    bracketCnt--;
			    if(bracketCnt==0) {
				bracketCnt=0;
				end();
			    } else if(bracketCnt<0) {
				bracketCnt=0;			    
			    } else {
				append(c);
			    }
			} else if(c=="{") {
			    append(c);
			    bracketCnt++;
			} else  {
			    append(c);
			}
		    }
		}		
		end();
		let c1 = this.editor.session.doc.indexToPosition(left);
		let c2 = this.editor.session.doc.indexToPosition(right);
		//Need to do this to prevent global conflicts
		let Range = ace.require('ace/range').Range;
		let r = new  Range(c1.row, c1.column, c2.row, c2.column);
		this.editor.selection.setRange(new Range(c1.row, c1.column, c2.row, c2.column));
		let callback = (values,args)=>{
		    if(!values) {
			this.editor.clearSelection();
			return;
		    }
		    text = text.substring(0,left) + command.command +" " +args + text.substring(right);
		    let idx = left+command.command.length +1 + args.length;
		    this.editor.setValue(text);
		    this.editor.clearSelection();
		    let cursor = this.editor.session.doc.indexToPosition(idx);
		    this.editor.selection.moveTo(cursor.row, cursor.column);
		    this.editor.focus();
		    
		};
		this.addCommand(command, {add:false,values:values,callback:callback,
					 event:e});
	    }

	},
	insertCommand:function(cmds) {
	    if(!cmds) return;
	    cmds = cmds.replace(/_quote_/g,"\"");
	    cmds = " " + cmds +" ";
	    this.insertText(cmds);
	},
	insertColumnIndex:function(index,plain) {
	    if(this.columnInput && $("#" + this.columnInput).length>0) {
		let v = $("#" + this.columnInput).val()||"";
		v = v.trim();
		if(v!="" && !v.endsWith(",")) v +=plain?"":",";
		index = String(index).split(",").join("\n");
		v+=index;
		$("#" + this.columnInput).val(v);	    
		return;
	    }
	    if(!plain) index = index+",";
	    this.insertText(index);
	},
	insertText:function(text) {
	    if (this.editor) {
		let cursor = this.editor.getCursorPosition();
		this.editor.insert(text);
		this.editor.focus();
		return;
	    }
	    let input = this.jq('input');
	    let start = input.prop('selectionStart');
	    let end = input.prop('selectionEnd');
	    input.val(input.val().substring(0, start)
		      +text 
		      + input.val().substring(end));
	    setTimeout(() =>{
		input.focus();},0);
	    input[0].selectionStart = start+text.length;
	    input[0].selectionEnd = start+text.length;
	},

	getInput:function(force) {
	    val= "";
	    if(this.doCommands || force) {
		if(this.editor)
		    val = this.editor.getValue();
		else
		    val =this.jq('input').val();
	    }
	    if(val == null) return "";
	    return val.trim();
	},
	display:function(what, process,html,command) {
	    if(!command) {
		command ="";
		lines = this.getInput().split("\n");
		for(let i=0;i<lines.length;i++){
		    line = lines[i].trim();
		    if(line =="") continue;
		    if(line.startsWith("#")) continue;
		    command +=line;
		    command +="\n";
		}
	    }
	    command = command.trim();
	    if(what!="-raw" && command.indexOf("-template ")>=0) what = "";

	    if(what == null) {
		this.call(command  +" ", {process:process,html:html});
	    }  else {
		if(what == "-raw") command = "";
		this.call(command, {process:process, csvoutput:what,html:html});
	    }
	},
	getUrl:function(cmds,rawInput) {
	    let input = "";
	    if(rawInput || rawInput==="") input = "&lastinput=" + encodeURIComponent(rawInput);
	    let url = ramaddaBaseUrl + "/entry/show?output=convert_process&entryid=" + this.entry+"&commands=" + encodeURIComponent(cmds) + input;
	    return url;
	},
	makeDbMenu:function(field,value,label) {
	    return HtmlUtil.span([CLASS,"ramadda-clickable","field",field,"value",value],(label||field));
	},
	makeHeaderMenu: function(field,value,label) {
	    return HtmlUtil.span(["field",field,"value",value,CLASS,"ramadda-menuitem-link ramadda-clickable"],(label||field));
	},
	insertHeader:function(field,value) {
	    if(this.headerInput && $("#" + this.headerInput).length>0) {
		let v = $("#" + this.headerInput).val()||"";
		value = field +"  "+ value;
		v = v.trim();
		if(v.length>0)
		    v = v +"\n";
		v+=value;
		$("#" + this.headerInput).val(v);	    
		return;
	    }

	    let popup = this.jq(ID_POPUP);
	    popup.css("display","none");
	    if(!value) value = " ";
	    value = " " + value +" ";
	    this.insertCommand(field +value);
	},
	insertDb:function(field,value) {
	    if(this.dbDialog) this.dbDialog.remove();
	    this.dbDialog = null;
	    if(!value) value = " ";
	    if(value!="true" && value!="false") {
		if(value.indexOf(" ")>=0) 
		    value = " \"" +value+"\"";
		else if(value!="") value = " " + value +" ";
	    } else {
		value = " " + value +" ";
	    }
	    if(this.dbInput && $("#" + this.dbInput).length>0) {
		let v = $("#" + this.dbInput).val()||"";
		v = v.trim();
		if(v.length>0)
		    v = v +"\n";
		v+=field+" " +value;
		$("#" + this.dbInput).val(v);	    
		return;
	    }
	    this.insertCommand(field +value);
	},
	call:function(cmds,args) {
	    let _this =this;
	    if (!args)  {
		args = {};
	    }
	    this.output(HtmlUtil.tag("pre",[],"Processing..."));

	    let cleanCmds = "";
	    let lines = cmds.split("\n");
	    let doExplode = false;
	    //Strip out the comments and anything after -quit
	    for(let i=0;i<lines.length;i++) {
		let line = lines[i];
		let tline = lines[i].trim();
		if(tline.startsWith("#")) continue;
		if(tline.startsWith("-quit")) break;
		if(tline.startsWith("-explode")) {
		    doExplode = true;
		}
		cleanCmds+=line+"\n";
	    }
	    cmds = cleanCmds;
	    //If its the -db command then force -print
	    let hasDb = cmds.match(/[^ \t]-db[ \t]+/);
	    if(hasDb && Utils.stringDefined(args.csvoutput)) {
		args.csvoutput = "-print";
	    }


	    let isScript  = args.csvoutput=="-script";
	    let isArgs  = args.csvoutput=="-args";
	    let debug = cmds.match("-debug");
	    let rawInput = this.getInput();
	    haveOutput = Utils.isDefined(args.csvoutput);
	    if(!doExplode &&  cmds.indexOf("-count")<0  && cmds.indexOf("-db") <0 && !haveOutput)  {
		args.csvoutput = "-print";
	    }

	    let raw = args.csvoutput=="-raw";
	    let isDb = args.csvoutput == "-db" || cmds.indexOf("-db")>=0;
	    let isJson = args.csvoutput=="-tojson";
	    let isXml = args.csvoutput=="-toxml";	
	    let csv = args.csvoutput=="-print";
	    let stats = args.csvoutput=="-htmlstats";
	    let table =  args.csvoutput=="-table";					    
	    let showHtml =false;
	    let printHeader = args.csvoutput == "-printheader";

	    if((!args.process && !doExplode && !isScript && !isArgs) || args.html) {
		if (this.maxRows != "") {
		    if(stats) {
		    }  else {
			cmds = "-maxrows " + this.maxRows +" " + cmds;
		    }
		}
	    }



	    let url = this.getUrl(cmds,rawInput);

	    let filename = "results.txt"
	    if(isScript) filename = "convert.sh";
	    else if(isArgs) filename = "convertargs.txt";
	    else if(isJson) filename = "results.json";
	    else if(isXml) filename = "results.xml";			


	    if(args.process)
		url += "&process=true";
	    if(this.save) {
		url += "&save=true";        
	    }

	    if(args.csvoutput) {
		url += "&csvoutput=" + args.csvoutput;
	    }
	    if(args.clearOutput)
		url += "&clearoutput=true";
	    if(args.listOutput)
		url += "&listoutput=true";
	    if(this.applyToSiblings) 
		url += "&applysiblings=true";

	    let output = this.jq("output");

	    let result;
	    let writePre =contents=>{
		contents = HU.pre([STYLE,HU.css('position','relative'),ID,this.domId(ID_PRE)],  contents);
		output.html(contents);
		let msg = $(HU.div([STYLE,HU.css("position","absolute","right","48px","top","5px")], "")).appendTo(this.jq(ID_PRE));

		let copy = $(HU.div([TITLE,"Copy to clipboard", CLASS,"ramadda-clickable", STYLE,HU.css("position","absolute","right","10px","top","5px")], HU.getIconImage("fas fa-clipboard"))).appendTo(this.jq(ID_PRE));
		copy.click(()=>{
		    Utils.copyToClipboard(result);
		    msg.html("OK, result is copied");
		    setTimeout(()=>{
			msg.fadeOut();
		    },2000);
		});
		if(filename) {
		    let file = $(HU.div([TITLE,"Download file", CLASS,"ramadda-clickable", STYLE,HU.css("position","absolute","right","32px","top","5px")], HU.getIconImage("fas fa-file-download"))).appendTo(this.jq(ID_PRE));
		    file.click(()=>{
			msg.html("");
			Utils.makeDownloadFile(filename,result);
		    });
		}

	    };
	    let jqxhr = $.getJSON( url, (data) =>{
		if(data.error!=null) {
		    this.output(HtmlUtil.tag("pre",[],"Error:" + window.atob(data.error)));
		    return;
		}
		if(data.message!=null) {
		    this.output(HtmlUtil.tag("pre",[],window.atob(data.message)));
		    return;
		}
		if(data.file) {
		    this.output(HtmlUtil.tag("pre",[],""));
		    iframe = '<iframe src="' + data.file +'"  style="display:none;"></iframe>';
		    this.jq("scratch").html(iframe);
		    return;
		} 

		if(Utils.isDefined(data.html) ) {
		    html = window.atob(data.html);
		    html = HtmlUtil.tag("pre",[],html);
		    output.html(html);
		    return;
		}

		if(Utils.isDefined(data.url)) {
		    this.output(data.url);
		    output.find(".ramadda-button").button();
		    return;
		} 


		if(Utils.isDefined(data.result)) {
		    result = window.atob(data.result).trim();
		    if(isScript) {
			//		    Utils.makeDownloadFile("script.sh",result);
			//		    return;
		    }
		    if(result.indexOf("<table")>0  || result.indexOf("<div")>0  || result.indexOf("<row")>0)
			showHtml = true;
		    if(debug) {
			result = result.replace(/</g,"&lt;").replace(/>/g,"&gt;");
			writePre(result);
		    } else if(isDb) {
			let db = result.replace(/<tables>[ \n]/,"Database:");
			db = db.replace(/<property[^>]+>/g,"");
			db = db.replace(/> *<\/column>/g,"/>");
			db = db.replace(/\n *\n/g,"\n");
			db = db.replace(/\/>/g,"");
			db = db.replace(/>/g,"");
			db = db.replace(/<table +id="(.*?)"/g,"\t<table <a class=csv_db_field field='table' onclick=noop()  title='Add to input'>$1</a>");
			db = db.replace("<table "," ");
			db = db.replace(/Database:[ \t]+/,"Database: ");
			db = db.replace("</table","");
			db = db.replace("</tables","");

			db = db.replace(/<column +name="(.*?)"/g,"\tcolumn: name=\"<a class=csv_db_field field='$1' onclick=noop() title='Add to input'>$1</a>\"");
			db = db.replace(/ ([^ ]+)="([^"]+)"/g,"\t$1:$2");
			db = db.replace(/ ([^ ]+)="([^"]*)"/g,"\t$1:\"$2\"");
			db = db.replace(/name:/g," ");
			db = db.replace(/column:[ \t]+/g,"column: ");			
			writePre(db);
			output.find(".csv_db_field").click(function(event) {
                            let space = "&nbsp;"
                            event.preventDefault();
                            let pos=$(this).offset();
                            let h=$(this).height();
                            let w=$(this).width();
                            let field  = $(this).attr("field");
                            let html = "<div style=\"margin:2px;margin-left:5px;margin-right:5px;\">\n";
			    html+="<b>Add arguments to -db tag</b><div style='margin-left:5px;'>";
                            if(field  == "table") {
				html +=_this.makeDbMenu(field+".name")+"<br>";
				html +=_this.makeDbMenu(field+".label")+"<br>";
				html +=_this.makeDbMenu("install","true")+"<br>";
				html +=_this.makeDbMenu("nukedb","true")+"<br>";								
                            } else {
				html +=_this.makeDbMenu(field+".id")+"<br>";
				html +=_this.makeDbMenu(field+".label")+"<br>";
				html +=
                                    _this.makeDbMenu(field+".type")+space +
                                    _this.makeDbMenu(field+".type","string","string")+space +
                                    _this.makeDbMenu(field+".type","double","double")+space +
                                    _this.makeDbMenu(field+".type","int","int")+space +
                                    _this.makeDbMenu(field+".type","enumeration","enumeration")+space +
                                    _this.makeDbMenu(field+".type","enumerationplus","enumeration+")+space +
                                    _this.makeDbMenu(field+".type","date","date")+space +				    
                                    "<br>";
				html +=
                                    _this.makeDbMenu(field+".cansearch")+space +
                                    _this.makeDbMenu(field+".cansearch","true","true")+space +
                                    _this.makeDbMenu(field+".cansearch","false","false")+
                                    "<br>";
				html +=
                                    _this.makeDbMenu(field+".canlist")+space +
                                    _this.makeDbMenu(field+".canlist","true","true")+space+
                                    _this.makeDbMenu(field+".canlist","false","false")+
                                    "<br>";
                            }
                            html+="</div></div>";
			    _this.dbDialog=HU.makeDialog({content:html,anchor:$(this)});
			    _this.dbDialog.find(".ramadda-clickable").click(function() {
				_this.insertDb($(this).attr('field'),$(this).attr('value'));
				

			    });
			})

		    } else if(csv) {
			let isResultJson = (result.startsWith("{") && result.endsWith("}")) || (result.startsWith("[") && result.endsWith("]"));
			if(isResultJson) {
			    try {
				let json= JSON.parse(result);
				result = Utils.formatJson(json,5);
				output.html(HU.pre(result));
				return
			    } catch(err) {
				console.log("Err:" + err);
			    }
			}

			result = result.trim();
			let isXml = result.startsWith("<") && result.endsWith(">")
			if(isXml) {
			    try {
				let html =Utils.formatXml(result.trim());
				output.html(HU.pre(html));
				output.find(".ramadda-xmlnode").click(()=>{
				    this.insertText($(this).attr("data-path"));
				});
				return;
			    } catch (err) {
				console.log("err");
				console.log("Couldn't display as xml:" + err);
			    }
			}
			result = result.replace(/</g,"&lt;").replace(/>/g,"&gt;");
			let isHeader = result.startsWith("#fields=");
			if(isHeader) {
			    let toks = result.split("\n");
			    let line = toks[0];
			    line = line.replace("#fields=","");
			    line = line.replace(/(\] *),/g,"$1\n");
			    let tmp ="";
			    toks = line.split("\n");
			    for(let i=0;i<toks.length;i++) {
				let l = toks[i];
				l = l.replace(/^(.*?)\[/,"<span class=csv_addheader_field field='$1' title='Add to input'>$1</span>[");
				tmp+=l +"\n";
			    }
			    result = tmp;
			}			    
			writePre(result);
			if(isHeader) {
			    output.find(".csv_addheader_field").click(function(event) {
				event.preventDefault();
				let field = $(this).attr("field");
				let pos=$(this).offset();
				let h=$(this).height();
				let w=$(this).width();
				let html = "<div style=\"margin:2px;margin-left:5px;margin-right:5px;\">\n";
				html +="type=" + 
				    _this.makeHeaderMenu(field+".type","enumeration","enumeration")+ SPACE2+
				    _this.makeHeaderMenu(field+".type","string","string")+ SPACE2+	
				    _this.makeHeaderMenu(field+".type","double","double")+SPACE2+
				    _this.makeHeaderMenu(field+".type","date","date")+ SPACE2 +
				    _this.makeHeaderMenu(field+".type","url","url")+SPACE2 +
				    _this.makeHeaderMenu(field+".type","image","image");
				html+="</div>";
				let dialog =   HU.makeDialog({content:html,anchor:$(this)});
				dialog.find(".ramadda-clickable").click(function() {
				    _this.insertHeader($(this).attr("field"),$(this).attr("value"));
				    dialog.remove();
				});
			    });
			}
			return;
		    } else if(stats || table) {
			output.html(result);
			let toolbar = HU.span([TITLE,"Insert field names", CLASS,"ramadda-clickable", ID,this.domId("addfields")],"Add field ids") + SPACE3;
			if(table)
			    toolbar += HU.span([ID,"csv_toggledetails"],"Show summary");

			output.find("#header").html(toolbar);
			let _this = this;
			let visible = false;
			if(table)
			    output.find(".th2").hide();
			$("#csv_toggledetails").addClass("ramadda-clickable").click(function(){
			    visible = !visible;
			    $(this).html(visible?"Hide summary":"Show summary");
			    if(visible)
				output.find(".th2").show();
			    else
				output.find(".th2").hide();
			});

			let ids = [];
			output.find( ".csv-id").each(function() {
			    ids.push($(this).attr('fieldid'));
			});
			this.jq("addfields").click(()=>{
			    let f = ids.join(",");
			    this.insertColumnIndex(f,true);
			});

			output.find( ".csv-id").css('color','blue').css('font-weight','normal').css('cursor','pointer').attr('title','Add field id').click(function() {
			    let id = $(this).attr('fieldid');
			    if(!id) return;
			    _this.insertColumnIndex(id,true);
			});
			if(table)
			    HtmlUtils.formatTable(output.find( ".ramadda-table"),{paging:false,height:"200px",fixedHeader: true,scrollX:true});
		    } else if(!raw && showHtml) {
  			let newresult = result.replace(/(<th>.*?)(#[0-9]+)(.*?<.*?>)([^<>]*?)(<.*?)<\/th>/g,"$1<a href='#' index='$2' style='color:blue;' class=csv_header_field field='table' onclick=noop()  title='Add field id'>$2</a>$3<a href='#' label='$4' style='color:blue;' class=csv_header_field field='table' onclick=noop()  title='Add field id'>$4</a>$5</th>");
			result = newresult;
			output.html(result);
			output.find(".csv_header_field").click(function(event) {
			    $(this).attr(STYLE,"color:black;");
			    let label = $(this).attr("label");
			    if(!label) {
				let index = $(this).attr("index");
				if(index) label = index.replace("#","").trim();
			    } else {
				label = Utils.makeId(label);
			    }
			    _this.insertColumnIndex(label);
			});
			HtmlUtils.formatTable(".ramadda-table",{xordering:true});
		    } else {
			result = result.trim();
			let isJson = (result.startsWith("{") && result.endsWith("}")) || (result.startsWith("[") && result.endsWith("]"));
			console.log("isJson:" + isJson);
			let isXml = result.startsWith("<") && result.endsWith(">")
			let out = "";
			if(isJson) {
			    try {
				result= JSON.parse(result.trim());
				result = Utils.formatJson(result,5);
				out=HU.pre(result);
				return
			    } catch(err) {
				console.log("Err:" + err);
				out = "<b>Error processing json:" + err+"</b><br>";
				out +=HU.pre(result);
				output.html(out);
			    }
			    return
			} else if(isXml) {
			    try {
				let html =Utils.formatXml(result.trim());
				output.find(".ramadda-xmlnode").click(function(){
				    _this.insertText($(this).attr("data-path"));
				});
				return;
			    } catch (err) {
				out="error processing xml:" + err;
				console.log("Couldn't display as xml:" + err);
			    }
			}
 			if(printHeader) {
			    let tmp = "";
			    result = result.replace("#fields=","");
			    result.split("\n").forEach(line=>{
				line = line.trim();
				if(line=="") return;
				let regexp = new RegExp("([^ ]+) ([^ \\[]+)\\[(.*)\\]$");
				let toks = line.match(regexp);
				if(!toks)  {
				    toks = line.match("([^ ]+)(.*)");
				}
				if(!toks) {
				    tmp+= "<tr>" + HU.tds([],[line]) +"</tr>";
				    return;
				}
				let index = toks[1];
				let id = toks[2];
				let attrs = toks.length>2?toks[3]:"";
				id = "<a href='#' plain='true' index='" + id+"' style='color:blue;text-decoration:none !important;' class=csv_header_field field='table' onclick=noop()  title='Add field id'>" + id +"</a>";
				line = "<tr>" + HU.tds([],[index,"",id,attrs]) +"</tr>";
				tmp+=line;
			    });
			    result = "<table><tr><td><b>Index</b></td><td>&nbsp;</td><td><b>ID</b></td><td><b>Attributes</b></td></tr>" + tmp +"</table>"; 
			} else if(cmds.indexOf("-help")>=0) {
			    let tmp = "";
			    result = result.replace(/</g,"&lt;");
			    result = result.replace(/>/,"&gt;");
			    result.split("\n").map(line=>{
				if(line.trim().startsWith("-")) {
				    let idx= line.indexOf("(");
				    let cmd = line;
				    let comment = "";
				    if(idx>0) {
					cmd = line.substring(0,idx);
					comment = "<i>" + line.substring(idx)+"</i>";
				    }
				    line = "      <a href=# onclick=\"Csv.insertCommand('" + cmd.trim() +"')\">" + cmd.trim() +"</a> " + comment;
				}  else {
				    line = "<b>" + line +"</b>"
				}
				tmp+=line +"\n";
			    });
			    result = tmp;
			} else {
			    result = result.replace(/</g,"&lt;");
			    result = result.replace(/>/,"&gt;");
			}
			let html = printHeader?result:"<pre>" + result +"</pre>";
			if(isDb) {
			    html+=HU.div([ID,this.domId(ID_POPUP), CLASS,"ramadda-popup"]);
			} else if(!printHeader) {
			    writePre(result);
			    return;
			} 
		    output.html(html);
			if(printHeader) {
			    output.find(".csv_header_field").click(function(event) {
				let index = $(this).attr("index").replace("#","").trim();
				_this.insertColumnIndex(index,$(this).attr("plain"));
			    });
			}			
		    }
		    return;
		}
		output.html(HtmlUtil.pre([],"No response given"));
	    })
		.fail(function(jqxhr, textStatus, error) {
		    let err = textStatus + ", " + error;
		    _this.output(HtmlUtil.pre([],"Error:" + err));
		});
	},
	addCommand:function(cmd, args, anchor) {
	    if(typeof cmd == "string") {
		cmd = this.commandsMap[cmd];
	    }
	    let opts = {
		add:true,
		values:null,
		callback:null
	    };
	    if(args) $.extend(opts, args);
	    let desc = cmd.description.replace(/^\(/,"").replace(/\)$/,"");
	    let label = cmd.label || Utils.camelCase(cmd.command.replace(/^-/,""));

	    let inner = HU.center(HU.h2(label)) + HU.center(desc);
	    inner+=HU.formTable();
	    this.columnInput = null;
	    this.headerInput = null;	    
	    this.dbInput = null;	    
	    cmd.args.forEach((a,idx)=>{
		let v = opts.values && idx<opts.values.length?opts.values[idx]:"";
		let label = a.label || Utils.makeLabel(a.id);
		let id = this.domId("csvcommand" + idx);
		let desc = a.description||"";
		desc = desc.replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/\n/g,"<br>");
		desc =  HU.div([STYLE,HU.css('max-width','500px','vertical-align','top')],desc);

		if(!this.headerInput && cmd.command=="-addheader" && a.id=="properties") {
		    this.headerInput = id;
		}
		if(!this.dbInput && cmd.command=="-db" && a.id=="properties") {
		    this.dbInput = id;
		}		

		if(a.rows) {
		    inner+=HU.formEntryTop(label,
					   HU.hbox([HU.textarea("",v,["cols", a.columns || "40", "rows",a.rows,ID,id,"size",10]),desc]));		
		} else if(a.type=="list" || a.type=="columns" || a.type=="rows") {
		    let delim = a.delimiter||",";
		    let lines = v.split(delim);
		    v = lines.join("\n");
		    if(!this.columnInput && (a.type == "columns" || a.type == "column")) this.columnInput = id;
		    inner+=HU.formEntryTop(label,HU.hbox([HU.textarea("",v,["cols", a.size || "10", "rows",a.rows||"5",ID,id]),
				 			  desc]));
		    

		} else if(a.values || a.type=="enumeration") {
		    let values
		    if(a.values) {
			values = a.values;
		    } else {
			console.log(JSON.stringify(a));
			value="foo,bar";
		    }
		    inner+=HU.formEntry(label,HU.hbox([HU.select("",[ID,id],values.split(",")),desc]));
		} else {
		    let size = a.size ||30;
		    if(a.type=="number") size=a.size||5;
		    if(a.type=="column") size=a.size||5;
		    let placeholder = a.placeholder || "";
		    let title = a.tooltip || "";
		    if(a.type=="pattern" && !a.placeholder)
			title = "Escapes- _leftparen_, _rightparen_, _leftbracket_, _rightbracket_, _dot_, _dollar_, _star_, _plus_, _nl_"
		    if(!this.columnInput && (a.type == "columns" || a.type == "column")) this.columnInput = id;
		    let input = HU.input("",v,[ID,id,"size",size,TITLE, title, "placeholder",placeholder]);
		    inner+=HU.formEntry(label,HU.hbox([input, desc]));
		}
	    });
	    inner+=HU.formTableClose();
	    inner += HU.div([STYLE,HU.css("margin-bottom","10px")], HU.center(HU.div([STYLE,HU.css("display","inline-block"), ID,this.domId(ID_ADDCOMMAND)],opts.add?"Add Command":"Change Command") +SPACE2+HU.div([STYLE,HU.css("display","inline-block"), ID,this.domId(ID_CANCELCOMMAND)],"Cancel")));


	    if(this.addDialog) {
		this.addDialog.hide();
	    }

	    let target = anchor;
	    let at = "left bottom";
	    if(opts.event) {
		at = "left " + "top+" + (opts.event.offsetY+10);
		target = $(opts.event.target);
	    }
	    inner = HU.div([STYLE,HU.css('margin','5px')], inner);
	    let dialog =   HU.makeDialog({content:inner,my:"left top",at:at,anchor:target,draggable:true,header:true,inPlace:false});
	    let submit = () =>{
		this.columnInput = null;
		let args = "";
		let values =[];
		cmd.args.forEach((a,idx)=>{
		    let v = this.jq("csvcommand" +idx).val();
		    if(a.type=="list" || a.type=="columns" || a.type=="rows") {
			let tmp = "";
			let delim = a.delimiter||",";
			v.split("\n").forEach(line=>{
			    line = line.trim();
			    if(line=="") return;
			    if(tmp!="") tmp+=delim;
			    tmp +=line;
			});
			v = tmp;
		    }
		    if(v.indexOf("\n")>0) {
			v = "{"+v+"}";
		    } else if(v.indexOf("{")>0) {
			v = "\""+v+"\"";
		    } else  
			if(v=="" || v.indexOf(" ")>=0 || v.indexOf(",")>=0) v = "\"" + v +"\"";
		    values.push(v);
		    args+=v +" ";
		});
		if(opts.callback) {
		    opts.callback(values,args);
		} else {
		    this.insertText(cmd.command +" " + args) ;
		}
		dialog.remove();
	    }
	    
	    HU.onReturnEvent(dialog.find("input"),()=>{submit()});
	    

	    this.jq("csvcommand0").focus();
	    this.jq(ID_ADDCOMMAND).button().click(()=>{
		submit();
	    });
	    this.jq(ID_CANCELCOMMAND).button().click(()=>{
		if(opts.callback) {
		    opts.callback(null);
		}
		this.columnInput = null;
		dialog.remove();
	    });    
	},
    });

    this.init();

}



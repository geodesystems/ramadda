
var Csv = {
    editor:null,
    inputId: "convertcsv_input",
    applyToSiblings:false,
    save: true,
    doCommands:true,
    commands:null,
    commandsMap:null,
    header:null,
    maxRows:30,
    dbPopupTime:null,
    init: function(){
	let text = convertCsvLastInput;
	if(text!=null) {
            text = text.replace(/_escnl_/g,"\n").replace(/_escquote_/g,'&quot;').replace(/_escslash_/g,"\\").replace(/\"/g,'&quot;');
	}  else {
            text = Csv.getInput(true)||"";
	}

	let html = "";
	html += "<style type='text/css'>";
	html += ".convert_button {padding:2px;padding-left:5px;padding-right:5px;}\n.ramadda-csv-table  {font-size:10pt;}\n ";
	html += ".convert_add {margin-left:10px; cursor:pointer;}\n";
	html += ".convert_add:hover {text-decoration:underline;}\n";
	html += ".ace_gutter-cell {cursor:pointer;}\n";
	html += ".ace_editor {margin-bottom:5px;height:200px;}\n";
	html += ".ace_editor_disabled {background:rgb(250,250,250);}\n";
	html += ".ace_csv_comment {color:#B7410E;}\n";
	html += ".ace_csv_command {color:blue;}\n";
	html += "</style>";
	//HtmlUtils.getIconImage("fa-file")

	let topLeft = HU.div([ID,"csvconvert_menu","style","display:inline-block;"],"");
	let topRight = 	 HU.href("#", HU.getIconImage("fa-cog"),[ID,"convertcsv_settings",TITLE,"Settings",STYLE,HU.css("cursor","pointer")]) +SPACE2 +
	    HtmlUtil.href("javascript:Csv.call('-help')",HtmlUtils.getIconImage("fa-question-circle"))+SPACE2;
	    
	html += HU.div(["class","ramadda-menubar","style","width:100%;"],HU.leftRightTable(topLeft,topRight));
	html += HtmlUtil.textarea("",text,[STYLE,"width:100%;", ID,Csv.inputId, "rows", "5"]);
	let left ="";
	left += HtmlUtil.href("javascript:Csv.display('-raw')","Raw",["title", "Don't process. Just show the raw data", CLASS,"convert_button"])+" ";
	left += HtmlUtil.href("javascript:Csv.display('-print')","Text",["title","Display text output",CLASS,"convert_button"])+" ";
	left += HtmlUtil.href("javascript:Csv.display('-printheader',null,true)","Header",["title", "Print the header (ctrl-h)", CLASS,"convert_button"])+" ";
	left += HtmlUtil.href("javascript:Csv.display('-record')","Records",["title","Display output as records", CLASS,"convert_button"])+" ";
	left += HtmlUtil.href("javascript:Csv.display('-stats',null,true)","Table",["title","Display table (ctrl-h)", CLASS,"convert_button"])+" ";
	left += HtmlUtil.href("javascript:Csv.display('',true)","Process",["title","Process entire file", CLASS,"convert_button"])+" ";
	let right = "";
	right += HtmlUtil.href("javascript:Csv.clearCommand()",HU.getIconImage("fa-eraser", [TITLE,"Clear Output"])) +SPACE2;
	right += HtmlUtil.href("javascript:Csv.call('',{listOutput:true})",HU.getIconImage("fa-list",[TITLE,"List Temp Files"])) +SPACE2;
	right += HtmlUtil.href("javascript:Csv.call('',{clearOutput:true})",HU.getIconImage("fa-trash",[TITLE, "Remove Temp Files"])) +SPACE2;

	html += HtmlUtil.leftRightTable(left,right);
	html +=  HtmlUtil.div([ID, "convertcsv_output",STYLE,HU.css("margin-top","5px", "max-height","500px","overflow-y","auto")],"");
	html += HtmlUtil.div([ID, "convertcsv_scratch"],"");

	$("#convertcsv_div").html(html);
	Csv.makeEditor();
	$(".convert_button").button();

	$("#convertcsv_settings").click(function(e) {
	    let html ="";
	    html += "Rows: " + HtmlUtil.input("",Csv.maxRows,["size","2", ID,"convertcsv_maxrows"]) +"<br>";
	    html += HtmlUtil.checkbox("convertcsv_applytosiblings",[], Csv.applyToSiblings) + " Apply to siblings" +"<br>";
	    html +=  HtmlUtil.checkbox("",[ID,"convertcsv_save"],Csv.save) +" Save" +"<br>";
	    html += HtmlUtil.checkbox("",[ID,"convertcsv_docommands"],Csv.doCommands) +" Do commands";
	    html = HU.div(["style",HU.css("margin","10px")],html);

	    let dialog = HU.makeDialog({content:html,my:"right top",at:"right bottom",xtitle:"",anchor:$(this),draggable:true,header:true,inPlace:false});
	    HU.onReturnEvent("#convertcsv_maxrows",input=>{
		Csv.maxRows = input.val();
		dialog.remove();
	    });

	    $("#convertcsv_save").click(function() {
		Csv.save =  $(this).is(':checked');
	    });
	    $("#convertcsv_applytosiblings").click(function() {
		Csv.applyToSiblings =  $(this).is(':checked');
	    });
	    $("#convertcsv_docommands").click(function() {
		Csv.doCommands =  $(this).is(':checked');
		if(Csv.editor) {
		    if(Csv.doCommands) {
			Csv.editor.container.classList.remove("ace_editor_disabled")
		    } else {
			Csv.editor.container.classList.add("ace_editor_disabled");
		    }
		}
	    });
	});


	$(document).click(function(e) {
	    if(Csv.dbPopupTime) {
		var now = new Date();
		var timeDiff = now-Csv.dbPopupTime.getTime();
		if(timeDiff<1000)  {
		    return;
		}
	    }
	    var popup = $("#csv_popup");
	    popup.css("display","none");
	});
	$("#csvconvert_add").click(function() {
	    if(!Csv.commands) return;
	    if(!Csv.addDialog) {
		let html = "";
		Csv.commands.forEach(cmd=>{
		    var command = cmd.command;
		    if(cmd.isCategory) {
			html +=HtmlUtil.div([STYLE,HU.css("font-weight","bold","font-size","14pt")], cmd.description);
			return;
		    }
		    if(!command) return;
		    if(!command.startsWith("-")) return;
		    if(command.startsWith("-help")) return;
		    var tooltip = "";
		    var desc = cmd.description;
		    if(desc && desc!="") {
			tooltip =desc+"\n";
		    }
		    tooltip += command;
		    cmd.args.forEach(a=>{
			let desc = a.desc||"";
			if(desc!="") desc = " "  + desc;
			tooltip +=" <" +a.id  +desc+ "> ";
		    });
		    tooltip = tooltip.replace(/\"/g,"&quot;").replace(/\(/g,"").replace(/\)/g,"");
		    var label = cmd.label ||  Utils.camelCase(cmd.command.replace("-",""));
		    html +=HU.div([CLASS,"convert_add",TITLE,tooltip],HtmlUtil.onClick("Csv.addCommand('" + command +"')",label));
		});
		html = HU.div([STYLE,HU.css("margin-left","10px","max-height","300px","overflow-y","auto")], html);
		Csv.addDialog =  HU.makeDialog({content:html,my:"left top",at:"right+10 top",anchor:$(this),draggable:true,header:true,inPlace:false});
	    }
	    Csv.addDialog.show();
	});


	var helpUrl = Csv.getUrl("-helpjson");
	var jqxhr = $.getJSON( helpUrl, function(data) {
	    if(data.error!=null) {
		return;
	    }
	    if(Utils.isDefined(data.result)) {
		var result = window.atob(data.result);
		result= JSON.parse(result);
		Csv.commands = result.commands;
		Csv.commandsMap = {}
		let docs ="";
		let menus = [];
		let fileSelect =  HU.div(["id","convertcsv_file1_selectlink", "class","ramadda-highlightable  ramadda-menubar-button"], "Select file");
		let categories = {};
		Csv.commands.forEach(cmd=>{
		    var command = cmd.command;
		    if(cmd.isCategory) {
			docs+="etlcat {" + cmd.description+"}\n";
			category = cmd.description;
			categories[category] = menuItems = [];
			menus.push(HU.div(["class","ramadda-highlightable ramadda-menubar-button","category",category], category));
			return;
		    }
		    if(!command) return;
		    if(!command.startsWith("-")) return;
		    if(command.startsWith("-help")) return;
		    var tooltip = "";
		    var desc = cmd.description;
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
		    var label = cmd.label ||  Utils.camelCase(cmd.command.replace("-",""));
		    docs+="etl " + "{" + command +"} {"  + label+"} {" +  desc + "} "   + docArgs +"\n"
		    Csv.commandsMap[command] = cmd;
                    menuItems.push(HU.div([TITLE,desc||"",CLASS, "ramadda-hoverable ramadda-clickable","command",command],label));
		});
		menus.push(fileSelect);
//		Utils.makeDownloadFile("etlcommands.tcl",docs);
		let menuBar=menus.join("");
		$("#csvconvert_menu").html(HU.div([],menuBar));
		$("#csvconvert_menu").find(".ramadda-menubar-button").click(function() {
		    let cat = $(this).attr("category");
		    let button = $(this);
		    if(!cat || !categories[cat]) {
			selectInitialClick(event,'convertcsv_file1','convertcsv_input','true','entry:entryid','" + convertCsvEntry+"');
			return;
		    }
		    let items = Utils.splitList(categories[cat],10);
		    items = items.map(list=>{
			return list.join("");
		    });
		    let menu = Utils.wrap(items,"<div style='vertical-align:top;margin:5px;display:inline-block;'>","</div>");
		    let dialog = HU.makeDialog({content:menu,anchor:$(this)});
		    dialog.find(".ramadda-clickable").click(function(event) {
			var cmd = Csv.commandsMap[$(this).attr("command")];
			dialog.remove();
			if(!cmd) {
			    return;
			}
			Csv.addCommand(cmd,null,button);
		    });
		});
	    }
   

	})
	    .fail(function(jqxhr, textStatus, error) {
	    });
    },
    output: function(html) {
	$('#convertcsv_output').html(html);
    },
    clearCommand:function() {
	Csv.output("");
    },
    makeEditor:  function() {
	Csv.editor = ace.edit(Csv.inputId);
	Csv.editor.setBehavioursEnabled(true);
	Csv.editor.setDisplayIndentGuides(false);
	Csv.editor.setKeyboardHandler("emacs");
	Csv.editor.setShowPrintMargin(false);
	Csv.editor.getSession().setUseWrapMode(true);
	var options = {
            autoScrollEditorIntoView: true,
            copyWithEmptySelection: true,
	};
	Csv.editor.setOptions(options);
	Csv.editor.session.setMode("ace/mode/csvconvert");
	Csv.editor.on("guttermousedown", function(e) {
	    if(e.domEvent.metaKey) {
		let row = +e.getDocumentPosition().row+1;
		let lines =Csv.editor.getValue().split("\n");
		let text = "";
		for(let i=0;i<row;i++) {
		    text+=lines[i]+"\n";
		}
		Csv.display("-table",null,true,text);
	    }
	});
	Csv.editor.commands.addCommand({
	    name: "keyt",
	    exec: function() {
		Csv.display('-stats',null,true);
	    },
	    bindKey: {mac: "ctrl-t", win: "ctrl-t"}
	})
	Csv.editor.commands.addCommand({
	    name: "keyh",
	    exec: function() {
		Csv.display('-printheader',null,true);
	    },
	    bindKey: {mac: "ctrl-h", win: "ctrl-h"}
	})

	Csv.editor.container.addEventListener("contextmenu", function(e) {
	    e.preventDefault();
	    let cursor = Csv.editor.getCursorPosition();
	    let index = Csv.editor.session.doc.positionToIndex(cursor);
	    let text =Csv.editor.getValue();
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
		let command = Csv.commandsMap[text.substring(left,right)];
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
		let c1 = Csv.editor.session.doc.indexToPosition(left);
		let c2 = Csv.editor.session.doc.indexToPosition(right);
		//Need to do this to prevent global conflicts
		var Range = ace.require('ace/range').Range;
		let r = new  Range(c1.row, c1.column, c2.row, c2.column);
		Csv.editor.selection.setRange(new Range(c1.row, c1.column, c2.row, c2.column));
		let callback = (values,args)=>{
		    if(!values) {
			Csv.editor.clearSelection();
			return;
		    }
		    text = text.substring(0,left) + command.command +" " +args + text.substring(right);
		    let idx = left+command.command.length +1 + args.length;
		    Csv.editor.setValue(text);
		    Csv.editor.clearSelection();
		    let cursor = Csv.editor.session.doc.indexToPosition(idx);
		    Csv.editor.selection.moveTo(cursor.row, cursor.column);
		    Csv.editor.focus();
		    
		};
		Csv.addCommand(command, {add:false,values:values,callback:callback,
					 event:e});
	    }
	});

	addHandler({
            selectClick: function(selecttype, id, entryId, value) {
		Csv.insertText("entry:" + entryId);
	    }
	},Csv.inputId);

	$("#convertcsv_input_container").keydown(function(e) {
	    if(e.ctrlKey) {
		let key = String.fromCharCode(event.keyCode ? event.keyCode : event.which).toLowerCase();	
		if(key == 't')
		    Csv.display('-table',null,true);
		else if(key == 'h')
		    Csv.display('-printheader',null,true);
	    }
	});
    },


    insertCommand:function(cmds) {
	if(!cmds) return;
	cmds = cmds.replace(/_quote_/g,"\"");
	cmds = " " + cmds +" ";
	Csv.insertText(cmds);
    },
    insertColumnIndex:function(index,plain) {
	if(Csv.columnInput && $("#" + Csv.columnInput).length>0) {
	    let v = $("#" + Csv.columnInput).val()||"";
	    v = v.trim();
	    if(v!="" && !v.endsWith(",")) v +=plain?"":",";
	    v+=index;
	    $("#" + Csv.columnInput).val(v);	    
	    return;
	}
	if(!plain) index = index+",";
	Csv.insertText(index);
    },
    insertText:function(text) {
	if (Csv.editor) {
            var cursor = Csv.editor.getCursorPosition();
            Csv.editor.insert(text);
            Csv.editor.focus();
	    return;
	}
	var input = $('#convertcsv_input');
	var start = input.prop('selectionStart');
	var end = input.prop('selectionEnd');
	input.val(input.val().substring(0, start)
		  +text 
		  + input.val().substring(end));
	setTimeout(function() {
            input.focus();},0);
	input[0].selectionStart = start+text.length;
	input[0].selectionEnd = start+text.length;
    },

    getInput:function(force) {
	val= "";
	if(Csv.doCommands || force) {
	    if(Csv.editor)
		val = Csv.editor.getValue();
            else
		val =$('#convertcsv_input').val();
	}
	if(val == null) return "";
	return val.trim();
    },
    display:function(what, process,html,command) {
	if(!command) {
	    command ="";
	    lines = Csv.getInput().split("\n");
	    for(var i=0;i<lines.length;i++){
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
            Csv.call(command  +" ", {process:process,html:html});
	}  else {
	    if(what == "-raw") command = "";
            Csv.call(command, {process:process, csvoutput:what,html:html});
	}
    },
    getUrl:function(cmds,rawInput) {
	var input = "";
	if(rawInput) input = "&lastinput=" + encodeURIComponent(rawInput);
	var url = ramaddaBaseUrl + "/entry/show?output=convert_process&entryid=" + convertCsvEntry+"&commands=" + encodeURIComponent(cmds) + input;
	return url;
    },
    makeDbMenu:function(field,value,label) {
	if(!value) value = "null";
	else value = "'" + value +"'";
	return HtmlUtil.tag("a",[CLASS,"ramadda-menuitem-link","onclick","Csv.insertDb('" + field+"'," +value+");"],(label||field));
    },
    makeHeaderMenu: function(field,value,label) {
	if(!value) value = "null";
	else value = "'" + value +"'";
	return HtmlUtil.tag("a",[CLASS,"ramadda-menuitem-link","onclick","Csv.insertHeader('" + field+"'," +value+");"],(label||field));
    },
    insertHeader:function(field,value) {
	var popup = $("#csv_popup");
	popup.css("display","none");
	if(!value) value = " ";
	value = " " + value +" ";
	Csv.insertCommand(field +value);
    },
    insertDb:function(field,value) {
	var popup = $("#csv_popup");
	popup.css("display","none");
	if(!value) value = " ";
	if(value!="true" && value!="false") 
            value = " \"" +value+"\"";
	else
            value = " " + value +" ";
	Csv.insertCommand(field +value);
    },
    call:function(cmds,args) {
	if (!args)  {
            args = {};
	}
	Csv.output(HtmlUtil.tag("pre",[],"Processing..."));

	var cleanCmds = "";
	var lines = cmds.split("\n");
	var doExplode = false;
	//Strip out the comments and anything after -quit
	for(var i=0;i<lines.length;i++) {
            var line = lines[i];
	    var tline = lines[i].trim();
            if(tline.startsWith("#")) continue;
            if(tline.startsWith("-quit")) break;
            if(tline.startsWith("-explode")) {
		doExplode = true;
            }
            cleanCmds+=line+"\n";
	}
	cmds = cleanCmds;
	var isScript  = cmds.trim().startsWith("-script");
	let debug = cmds.match("-debug");
	var rawInput = Csv.getInput();
	if((!args.process && !doExplode) || args.html) {
            if (Csv.maxRows != "") {
		cmds = "-maxrows " + Csv.maxRows +" " + cmds;
            }
	}

	haveOutput = Utils.isDefined(args.csvoutput);
	if(!doExplode &&  cmds.indexOf("-count")<0  && cmds.indexOf("-db") <0 && !haveOutput)  {
            args.csvoutput = "-print";
	}



	var raw = args.csvoutput=="-raw";
	var csv = args.csvoutput=="-print";
	var stats = args.csvoutput=="-stats";		
	var showHtml = args.csvoutput == "-table";
	var printHeader = args.csvoutput == "-printheader";
	var url = Csv.getUrl(cmds,rawInput);

	if(args.process)
            url += "&process=true";
	if(Csv.save) {
            url += "&save=true";        
	}

	if(args.csvoutput) {
            url += "&csvoutput=" + args.csvoutput;
	}
	if(args.clearOutput)
            url += "&clearoutput=true";
	if(args.listOutput)
            url += "&listoutput=true";
	if(Csv.applyToSiblings) 
            url += "&applysiblings=true";

	
	var jqxhr = $.getJSON( url, function(data) {
	    let output = $("#convertcsv_output");
            if(data.error!=null) {
		Csv.output(HtmlUtil.tag("pre",[],"Error:" + window.atob(data.error)));
		return;
            }
            if(data.message!=null) {
		Csv.output(HtmlUtil.tag("pre",[],window.atob(data.message)));
		return;
            }
	    

            if(data.file) {
		Csv.output(HtmlUtil.tag("pre",[],""));
		iframe = '<iframe src="' + data.file +'"  style="display:none;"></iframe>';
		$("#convertcsv_scratch").html(iframe);
		return;
            } 

            if(Utils.isDefined(data.html) ) {
		html = window.atob(data.html);
		html = HtmlUtil.tag("pre",[],html);
		output.html(html);
		return;
            }

            if(Utils.isDefined(data.url)) {
		Csv.output(data.url);
		output.find(".ramadda-button").button();
		return;
            } 


            if(Utils.isDefined(data.result)) {
		let result = window.atob(data.result);
		if(isScript) {
		    Utils.makeDownloadFile("script.sh",result);
		    return;
		}
		if(result.match(".*<(table|row|div).*")) showHtml = true;
		if(debug) {
		    result = result.replace(/</g,"&lt;").replace(/>/g,"&gt;");
		    result = "<pre>" + result+"</pre>";
                    output.html(result);
		} else if(csv) {
		    result = result.trim();
		    var isJson = (result.startsWith("{") && result.endsWith("}")) || (result.startsWith("[") && result.endsWith("]"));
		    var isXml = result.startsWith("<") && result.endsWith(">")
		    if(isJson) {
			try {
			    let json= JSON.parse(result);
			    result = Utils.formatJson(json,5);
			    output.html(HU.pre(result));
			    return
			} catch(err) {
			    console.log("Err:" + err);
			}
		    }
		    if(isXml) {
			try {
			    let html =Utils.formatXml(result.trim());
			    output.html(HU.pre(html));
			    output.find(".ramadda-xmlnode").click(function(){
				Csv.insertText($(this).attr("data-path"));
			    });
			    return;
			} catch (err) {
			    console.log("err");
			    console.log("Couldn't display as xml:" + err);
			}
		    }

		    result = result.replace(/</g,"&lt;").replace(/>/g,"&gt;");
		    result = "<pre>" + result+"</pre>";
                    output.html(result);
		} else if(stats) {
                    output.html(HU.div([],result));		    
		    
		    output.find( ".csv-id").css('color','blue').css('font-weight','normal').css('cursor','pointer').attr('title','Add field id').click(function() {
			let id = $(this).attr('fieldid');
			if(!id) return;
			Csv.insertColumnIndex(id,true);
		    });
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
			Csv.insertColumnIndex(label);
		    });
                    HtmlUtils.formatTable(".ramadda-table",{xordering:true});
		} else {
		    result = result.trim();
		    var isJson = (result.startsWith("{") && result.endsWith("}")) || (result.startsWith("[") && result.endsWith("]"));
		    var isXml = result.startsWith("<") && result.endsWith(">")
                    var isDb = result.startsWith("<tables");
		    var isHeader = result.startsWith("#fields=");
		    /*
		    if(isJson) {
			try {
			    result= JSON.parse(result.trim());
			    result = Utils.formatJson(result,5);
			    output.html(HU.pre(result));
			    return
			} catch(err) {
			    console.log("Err:" + err);
			}
		    } else if(isXml) {
			try {
			    let html =Utils.formatXml(result.trim());
			    output.html(HU.pre(html));
			    output.find(".ramadda-xmlnode").click(function(){
				Csv.insertText($(this).attr("data-path"));
			    });
			    return;
			} catch (err) {
			    console.log("err");
			    console.log("Couldn't display as xml:" + err);
			}
		    }
*/
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
		    } else if(isHeader) {
			var toks = result.split("\n");
			var line = toks[0];
			line = line.replace("#fields=","");
			line = line.replace(/(\] *),/g,"$1\n");
			var tmp ="";
			toks = line.split("\n");
			for(var i=0;i<toks.length;i++) {
			    var l = toks[i];
			    l = l.replace(/^(.*?)\[/,"<span class=csv_addheader_field field='$1' title='Add to input'>$1</span>[");
			    tmp+=l +"\n";
			}
			result = tmp;
		    } else if(isDb) {
			result = result.replace("<tables>","Database:");
			result = result.replace(/<property[^>]+>/g,"");
			result = result.replace(/> *<\/column>/g,"/>");
			result = result.replace(/\n *\n/g,"\n");
			result = result.replace(/\/>/g,"");
			result = result.replace(/>/g,"");
			result = result.replace(/<table +id="(.*?)"/g,"\t<table <a class=csv_db_field field='table' onclick=noop()  title='Add to input'>$1</a>");
			result = result.replace("<table ","table:");
			result = result.replace("</table","");
			result = result.replace("</tables","");

			result = result.replace(/<column +name="(.*?)"/g,"\tcolumn: name=\"<a class=csv_db_field field='$1' onclick=noop() title='Add to input'>$1</a>\"");
			result = result.replace(/ ([^ ]+)="([^"]+)"/g,"\t$1:$2");
			result = result.replace(/ ([^ ]+)="([^"]*)"/g,"\t$1:\"$2\"");

		    } else if(cmds.indexOf("-help")>=0) {
			var tmp = "";
			result = result.replace(/</g,"&lt;");
			result = result.replace(/>/,"&gt;");
			result.split("\n").map(line=>{
			    if(line.trim().startsWith("-")) {
				var idx= line.indexOf("(");
				var cmd = line;
				var comment = "";
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
                    var html = printHeader?result:"<pre>" + result +"</pre>";
                    if(isDb || isHeader) {
			html+="<div class=\"ramadda-popup\" xstyle=\"display: none;position:absolute;\" id=csv_popup></div>" ;
		    }
                    output.html(html);
		    if(printHeader) {
			output.find(".csv_header_field").click(function(event) {
			    var index = $(this).attr("index").replace("#","").trim();
			    Csv.insertColumnIndex(index,$(this).attr("plain"));
			});
		    }			
		    if(isHeader) {
			output.find(".csv_addheader_field").click(function(event) {
                            event.preventDefault();
			    var field = $(this).attr("field");
                            var pos=$(this).offset();
                            var h=$(this).height();
                            var w=$(this).width();
                            var html = "<div style=\"margin:2px;margin-left:5px;margin-right:5px;\">\n";
                            html +="type=" + 
				Csv.makeHeaderMenu(field+".type","enumeration","enumeration")+ "  "+
				Csv.makeHeaderMenu(field+".type","string","string")+ " "+	
				Csv.makeHeaderMenu(field+".type","double","double")+" "+
				Csv.makeHeaderMenu(field+".type","date","date")+
				Csv.makeHeaderMenu(field+".type","url","url")+
				Csv.makeHeaderMenu(field+".type","image","image")+
				"<br>";
                            html+="</div>";
                            var popup = $("#csv_popup");
                            Csv.dbPopupTime = new Date();
                            popup.css("display","block");
                            popup.html(html);
                            var myalign  = "left top";
                            var atalign  = "left bottom";
                            popup.position({
				of: $(this),
				my: myalign,
				at: atalign,
				collision: "none none"
                            });
			});
		    }
		    
                    if(isDb){
			output.find(".csv_db_field").click(function(event) {
                            var space = "&nbsp;"
                            event.preventDefault();
                            var pos=$(this).offset();
                            var h=$(this).height();
                            var w=$(this).width();
                            var field  = $(this).attr("field");
                            var html = "<div style=\"margin:2px;margin-left:5px;margin-right:5px;\">\n";
                            if(field  == "table") {
				html +=Csv.makeDbMenu(field+".name")+"<br>";
				html +=Csv.makeDbMenu(field+".label")+"<br>";
                            } else {
				html +=Csv.makeDbMenu(field+".id")+"<br>";
				html +=Csv.makeDbMenu(field+".label")+"<br>";
				html +=
                                    Csv.makeDbMenu(field+".type")+space +
                                    Csv.makeDbMenu(field+".type","string","string")+space +
                                    Csv.makeDbMenu(field+".type","double","double")+space +
                                    Csv.makeDbMenu(field+".type","int","int")+space +
                                    Csv.makeDbMenu(field+".type","enumeration","enumeration")+space +
                                    Csv.makeDbMenu(field+".type","enumerationplus","enumerationplus")+space +
                                    "<br>";
				html +=
                                    Csv.makeDbMenu(field+".cansearch")+space +
                                    Csv.makeDbMenu(field+".cansearch","true","true")+space +
                                    Csv.makeDbMenu(field+".cansearch","false","false")+
                                    "<br>";
				html +=
                                    Csv.makeDbMenu(field+".canlist")+space +
                                    Csv.makeDbMenu(field+".canlist","true","true")+space+
                                    Csv.makeDbMenu(field+".canlist","false","false")+
                                    "<br>";
                            }
                            html+="</div>";
                            var popup = $("#csv_popup");
                            Csv.dbPopupTime = new Date();
                            popup.css("display","block");
                            popup.html(html);
                            var myalign  = "left top";
                            var atalign  = "left bottom";
                            popup.position({
				of: $(this),
				my: myalign,
				at: atalign,
				collision: "none none"
                            });

			})

                    }
		}
		return;
            }
            output.html(HtmlUtil.tag("pre",[],"No response given"));
	})
            .fail(function(jqxhr, textStatus, error) {
		var err = textStatus + ", " + error;
		Csv.output(HtmlUtil.tag("pre",[],"Error:" + err));
            });
    },
    addCommand:function(cmd, args, anchor) {
	if(typeof cmd == "string") {
	    cmd = Csv.commandsMap[cmd];
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
	Csv.columnInput = null;
	cmd.args.forEach((a,idx)=>{
	    let v = opts.values && idx<opts.values.length?opts.values[idx]:"";
	    let label = a.label || Utils.makeLabel(a.id);
	    let id = "csvcommand" + idx;
	    let desc = a.description==null?"": HU.div([STYLE,HU.css('max-width','300px','vertical-align','top')],a.description);
	    if(a.rows) {
		inner+=HU.formEntryTop(label,
				       HU.hbox([HU.textarea("",v,["cols", a.columns || "40", "rows",a.rows,ID,id,"size",10]),desc]));		
	    } else if(a.type=="list" || a.type=="columns" || a.type=="rows") {
		v = v.replace(/,/g,"\n");
		if(!Csv.columnInput && (a.type == "columns" || a.type == "column")) Csv.columnInput = id;
		inner+=HU.formEntryTop(label,HU.hbox([HU.textarea("",v,["cols", a.size || "10", "rows",a.rows||"5",ID,id]),
						     desc]));
		
	    } else if(a.values) {
		inner+=HU.formEntry(label,HU.hbox([HU.select("",[ID,id],a.values.split(",")),desc]));
	    } else {
		let size = a.size ||30;
		if(a.type=="number") size=a.size||5;
		if(a.type=="column") size=a.size||5;
		let placeholder = a.placeholder || "";
		let title = a.tooltip || "";
		if(a.type=="pattern" && !a.placeholder)
		    title = "Escapes- _leftparen_, _rightparen_, _leftbracket_, _rightbracket_, _dot_, _dollar_, _star_, _plus_, _nl_"
		if(!Csv.columnInput && (a.type == "columns" || a.type == "column")) Csv.columnInput = id;
		let input = HU.input("",v,[ID,id,"size",size,TITLE, title, "placeholder",placeholder]);
		inner+=HU.formEntry(label,HU.hbox([input, desc]));
	    }
	});
	inner+=HU.formTableClose();
	inner += HU.div([STYLE,HU.css("margin-bottom","10px")], HU.center(HU.div([STYLE,HU.css("display","inline-block"), ID,"csvaddcommand"],opts.add?"Add Command":"Change Command") +SPACE2+HU.div([STYLE,HU.css("display","inline-block"), ID,"csvcancelcommand"],"Cancel")));


	if(Csv.addDialog) {
	    Csv.addDialog.hide();
	}

	let target = anchor;
	let at = "left bottom";
	if(opts.event) {
	    at = "left " + "top+" + (opts.event.offsetY+10);
	    target = $(opts.event.target);
	}
	let dialog =   HU.makeDialog({content:inner,my:"left top",at:at,anchor:target,draggable:true,header:true,inPlace:false});
	let submit = () =>{
	    Csv.columnInput = null;
	    let args = "";
	    let values =[];
	    cmd.args.forEach((a,idx)=>{
		let v = $("#csvcommand" +idx).val();
		if(a.type=="list" || a.type=="columns" || a.type=="rows") {
		    let tmp = "";
		    v.split("\n").forEach(line=>{
			line = line.trim();
			if(line=="") return;
			if(tmp!="") tmp+=",";
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
		Csv.insertText(cmd.command +" " + args) ;
	    }
	    dialog.remove();
	}
	
	HU.onReturnEvent(dialog.find("input"),()=>{submit()});

	$("#csvcommand0").focus();
	$("#csvaddcommand").button().click(()=>{
	    submit();
	});
	$("#csvcancelcommand").button().click(()=>{
	    if(opts.callback) {
		opts.callback(null);
	    }
	    Csv.columnInput = null;
	    dialog.remove();
	});    
    },
}

Csv.init();


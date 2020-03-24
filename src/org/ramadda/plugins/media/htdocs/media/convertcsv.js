

var csvDbPopupTime;
var csvEditor;

function csvGetInput(force) {
    val= "";
    if($("#convertcsv_runok").is(':checked') || force) {
	if(csvEditor)
	    val = csvEditor.getValue();
        else
	    val =$('#convertcsv_input').val();
    }
    if(val == null) return "";
    return val.trim();
}

function csvDisplay(what, process,html) {
    var command ="";
    lines = csvGetInput().split("\n");
    for(var i=0;i<lines.length;i++){
        line = lines[i].trim();
        if(line =="") continue;
        command +=line;
        command +="\n";
    }
    command = command.trim();
    if(what == null) {
        csvCall(command  +" ", {process:process,html:html});
    }  else {
	if(what == "-raw") command = "";
        csvCall(command, {process:process, csvoutput:what,html:html});
    }
}

function csvGetUrl(cmds,rawInput) {
    var input = "";
    if(rawInput) input = "&lastinput=" + encodeURIComponent(rawInput);
    var url = ramaddaBaseUrl + "/entry/show?output=csv_convert_process&entryid=" + convertCsvEntry+"&commands=" + encodeURIComponent(cmds) + input;
    return url;
}

function csvMakeDbMenu(field,value,label) {
    if(!value) value = "null";
    else value = "'" + value +"'";
    return HtmlUtil.tag("a",["class","ramadda-menuitem-link","onclick","csvInsertDb('" + field+"'," +value+");"],(label||field));
}

function csvMakeHeaderMenu(field,value,label) {
    if(!value) value = "null";
    else value = "'" + value +"'";
    return HtmlUtil.tag("a",["class","ramadda-menuitem-link","onclick","csvInsertHeader('" + field+"'," +value+");"],(label||field));
}

function csvInsertHeader(field,value) {
    var popup = $("#csv_popup");
    popup.css("display","none");
    if(!value) value = " ";
    //    if(value!="true" && value!="false") 
    //        value = " \"" +value+"\"";
    //    else
    value = " " + value +" ";
    csvInsertCommand(field +value);
}


function csvInsertDb(field,value) {
    var popup = $("#csv_popup");
    popup.css("display","none");
    if(!value) value = " ";
    if(value!="true" && value!="false") 
        value = " \"" +value+"\"";
    else
        value = " " + value +" ";
    csvInsertCommand(field +value);
}


function csvCall(cmds,args) {
    if (!args)  {
        args = {};
    }

    stop = 
        HtmlUtil.onClick("csvStop()","Stop",[]);
    csvOutput(HtmlUtil.tag("pre",[],"Processing..."));

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
    var rawInput = csvGetInput();
    if((!args.process && !doExplode) || args.html) {
        //        maxRows = args.maxRows;
        //        if(!Utils.isDefined(maxRows))
        maxRows = $("#convertcsv_maxrows").val().trim();
        if (maxRows != "") {
            cmds = "-maxrows " + maxRows +" " + cmds;
        }
    }

    haveOutput = Utils.isDefined(args.csvoutput);
    if(!doExplode &&  cmds.indexOf("-count")<0  && cmds.indexOf("-db") <0 && !haveOutput)  
        args.csvoutput = "-print";

    var showHtml = args.csvoutput == ("-table");
    var printHeader = args.csvoutput == ("-printheader");

    var url = csvGetUrl(cmds,rawInput);

    if(args.process)
        url += "&process=true";
    if($("#csvsave").is(':checked')) {
        url += "&save=true";        
    }

    if(args.csvoutput) {
        url += "&csvoutput=" + args.csvoutput;
    }
    if(args.clearOutput)
        url += "&clearoutput=true";
    if(args.listOutput)
        url += "&listoutput=true";
    if($("#convertcsv_applyall").is(':checked')) {
        url += "&applysiblings=true";
        
    }
    //    console.log(url);
    var jqxhr = $.getJSON( url, function(data) {
        if(data.error!=null) {
            csvOutput(HtmlUtil.tag("pre",[],"Error:" + window.atob(data.error)));
            return;
        }
        if(Utils.isDefined(args.func)) {
            //                args.func(data);
            //                return;
        }
        if(data.file) {
            csvOutput(HtmlUtil.tag("pre",[],""));
            iframe = '<iframe src="' + data.file +'"  style="display:none;"></iframe>';
            $("#convertcsv_scratch").html(iframe);
            return;
        } 
        if(Utils.isDefined(data.html) ) {
            html = window.atob(data.html);
            html = HtmlUtil.tag("pre",[],html);
            $("#convertcsv_output").html(html);
            return;
        }
        if(Utils.isDefined(data.url)) {
            csvOutput(data.url);
            return;
        } 
        if(Utils.isDefined(data.result)) {
            var result = window.atob(data.result);
            if(showHtml) {
		result = result.replace(/(<th>.*?)(#[0-9]+)/g,"$1<a href='#' index='$2' style='color:blue;' class=csv_header_field field='table' onclick=noop()  title='Add to input'>$2</a>");
                $("#convertcsv_output").html(result);
                $("#convertcsv_output .csv_header_field").click(function(event) {
		    $(this).attr("style","color:black;");
		    var index = $(this).attr("index").replace("#","").trim();
		    csvInsertText(index+",");
		});
                HtmlUtils.formatTable(".ramadda-table");
            } else {
  
                var isDb = result.startsWith("<tables");
		var isHeader = result.startsWith("#fields=");

		if(printHeader) {
		    result = result.replace(/(#[0-9]+) /g,"<a href='#' index='$1' style='color:blue;' class=csv_header_field field='table' onclick=noop()  title='Add to input'>$1</a> ");
		    console.log("result:" + result);
		} else if(isHeader) {
		    var toks = result.split("\n");
		    var line = toks[0];
		    line = line.replace("#fields=","");
		    line = line.replace(/(\] *),/g,"$1\n");
		    //			line = line.replace(/([^\[]+)(\[.*\])/g,"X$1$2");
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
			    line = "      <a href=# onclick=\"csvInsertCommand('" + cmd.trim() +"')\">" + cmd.trim() +"</a> " + comment;
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
                var html = "<pre>" + result +"</pre>";
                if(isDb || isHeader) {
                    html+="<div class=\"ramadda-popup\" xstyle=\"display: none;position:absolute;\" id=csv_popup></div>" ;
		}
                $("#convertcsv_output").html(html);
		if(printHeader) {
                    $("#convertcsv_output .csv_header_field").click(function(event) {
			$(this).attr("style","color:black;");
			var index = $(this).attr("index").replace("#","").trim();
			csvInsertText(index+",");
			
		    });
		}			
		if(isHeader) {
                    $("#convertcsv_output .csv_addheader_field").click(function(event) {
                        event.preventDefault();
			var field = $(this).attr("field");
                        var pos=$(this).offset();
                        var h=$(this).height();
                        var w=$(this).width();
                        var html = "<div style=\"margin:2px;margin-left:5px;margin-right:5px;\">\n";
                        html +="type=" + 
			    csvMakeHeaderMenu(field+".type","enumeration","enumeration")+ "  "+
			    csvMakeHeaderMenu(field+".type","string","string")+ " "+
			    csvMakeHeaderMenu(field+".type","double","double")+" "+
			    csvMakeHeaderMenu(field+".type","date","date")+
			    csvMakeHeaderMenu(field+".type","url","url")+
			    csvMakeHeaderMenu(field+".type","image","image")+
			    "<br>";
                        html+="</div>";
                        var popup = $("#csv_popup");
                        csvDbPopupTime = new Date();
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
                    $("#convertcsv_output .csv_db_field").click(function(event) {
                        var space = "&nbsp;"
                        event.preventDefault();
                        var pos=$(this).offset();
                        var h=$(this).height();
                        var w=$(this).width();
                        var field  = $(this).attr("field");
                        var html = "<div style=\"margin:2px;margin-left:5px;margin-right:5px;\">\n";
                        if(field  == "table") {
                            html +=csvMakeDbMenu(field+".name")+"<br>";
                            html +=csvMakeDbMenu(field+".label")+"<br>";
                        } else {
                            html +=csvMakeDbMenu(field+".id")+"<br>";
                            html +=csvMakeDbMenu(field+".label")+"<br>";
                            html +=
                                csvMakeDbMenu(field+".type")+space +
                                csvMakeDbMenu(field+".type","string","string")+space +
                                csvMakeDbMenu(field+".type","double","double")+space +
                                csvMakeDbMenu(field+".type","int","int")+space +
                                csvMakeDbMenu(field+".type","enumeration","enumeration")+space +
                                csvMakeDbMenu(field+".type","enumerationplus","enumerationplus")+space +
                                "<br>";
                            html +=
                                csvMakeDbMenu(field+".cansearch")+space +
                                csvMakeDbMenu(field+".cansearch","true","true")+space +
                                csvMakeDbMenu(field+".cansearch","false","false")+
                                "<br>";
                            html +=
                                csvMakeDbMenu(field+".canlist")+space +
                                csvMakeDbMenu(field+".canlist","true","true")+space+
                                csvMakeDbMenu(field+".canlist","false","false")+
                                "<br>";
                        }
                        html+="</div>";
                        var popup = $("#csv_popup");
                        csvDbPopupTime = new Date();
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
        $("#convertcsv_output").html(HtmlUtil.tag("pre",[],"No response given"));
    })
        .fail(function(jqxhr, textStatus, error) {
            var err = textStatus + ", " + error;
            csvOutput(HtmlUtil.tag("pre",[],"Error:" + err));
        });
}


function csvStop() {
    var url = ramaddaBaseUrl + "/entry/show?output=csv_convert_process&entryid=" + convertCsvEntry+"&stop=true";
    console.log(url);
    var jqxhr = $.getJSON( url, function(data) {
    })
        .fail(function(jqxhr, textStatus, error) {
        });
}



function csvInsertCommand(cmds) {
    if(!cmds) return;
    cmds = cmds.replace(/_quote_/g,"\"");
    cmds = " " + cmds +" ";
    csvInsertText(cmds);
}

function csvInsertText(text) {
    if (csvEditor) {
        var cursor = csvEditor.getCursorPosition();
        csvEditor.insert(text);
        csvEditor.focus();
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
}

function csvOutput(html) {
    $('#convertcsv_output').html(html);
}

function csvClearCommand() {
    csvOutput(HtmlUtil.tag("pre",[],"\n"));
}

function csvRunCommand(force) {
    val = csvGetInput(force);
    csvCall(val);
}


var         csvInputType = "input";

function csvFlipInput(text) {
    html = "";
    var val = null;
    if(text!=null) {
        text = text.replace(/_escnl_/g,"\n");
	text = text.replace(/_escquote_/g,'&quot;');
        text = text.replace(/_escslash_/g,"\\");
        text = text.replace(/\"/g,'&quot;');
        val = text;
        csvInputType = "input";
    }  else {
        val = csvGetInput(true);
    }
    if(val == null) val ="";
    let inputId = "convertcsv_input";
    if (csvInputType == "textarea") {
        val = val.replace(/\n/g, " ");
        html=HtmlUtil.input("",val,["size","120", "id",inputId]) +" " + HtmlUtil.onClick("csvFlipInput()","Expand",[]);
        csvInputType = "input";
    } else {
        html=HtmlUtil.textarea("",val,["style","width:100%;xxfont-size:11pt;", "id",inputId, "rows", "5"]);
        csvInputType = "textarea";
    }

    $("#convertcsv_input_container").append(html);
    csvEditor = ace.edit(inputId);
    csvEditor.setBehavioursEnabled(true);
    csvEditor.setDisplayIndentGuides(false);
    csvEditor.setKeyboardHandler("emacs");
    csvEditor.setShowPrintMargin(false);
    csvEditor.getSession().setUseWrapMode(true);
    var options = {
        autoScrollEditorIntoView: true,
        copyWithEmptySelection: true,
    };
    csvEditor.setOptions(options);
    csvEditor.session.setMode("ace/mode/csvconvert");
    csvEditor.commands.addCommand({
	name: "keyt",
	exec: function() {
	    csvDisplay('-table',null,true);
	},
	bindKey: {mac: "ctrl-t", win: "ctrl-t"}
    })
    csvEditor.commands.addCommand({
	name: "keyh",
	exec: function() {
	    csvDisplay('-printheader',null,true);
	},
	bindKey: {mac: "ctrl-h", win: "ctrl-h"}
    })


    $("#convertcsv_input_container").keydown(function(e) {
	if(e.ctrlKey) {
	    let key = String.fromCharCode(event.keyCode ? event.keyCode : event.which).toLowerCase();	
	    if(key == 't')
		csvDisplay('-table',null,true);
	    else if(key == 'h')
		csvDisplay('-printheader',null,true);
	}
    });
}


form="";
//form+=HtmlUtil.input("","",["size","120", "id","convertcsv_input"]);
form+=HtmlUtil.span(["id","convertcsv_input_container"],"");
html = "";
html += "<style type='text/css'>.ramadda-csv-table  {font-size:10pt;}\n ";
html += ".ace_editor {height:200px;}\n";
html += ".ace_csv_comment {color:#B7410E;}\n";
html += ".ace_csv_command {color:blue;}\n";
html += "</style>";

maxRows="Rows: " + HtmlUtil.input("","30",["size","5", "id","convertcsv_maxrows"]);
fileSelect = HtmlUtil.href("javascript:void(0);",HtmlUtils.getIconImage("fa-file"),["title", "Select file", "style", "color:black", "onClick", "selectInitialClick(event,'convertcsv_file1','convertcsv_input','true','entry:entryid','" + convertCsvEntry+"');",  "id","convertcsv_file1_selectlink"]);

html+="<table border=0 width=100%><tr><td>";
html+=HtmlUtil.href("javascript:csvCall('-help')",HtmlUtils.getIconImage("fa-question-circle"))+"&nbsp;&nbsp;";
html+=HtmlUtil.tag("span",["id","csv_commands"],"") +" ";
html += fileSelect;
html += "</td><td align=right>";
html += maxRows;
html += "&nbsp;&nbsp;";
html += HtmlUtil.checkbox("convertcsv_applyall",[], false) + " Apply to siblings";
html += "&nbsp;&nbsp;";
html +=  HtmlUtil.checkbox("csvsave",["id","csvsave"],true) +" Save";
html += "&nbsp;&nbsp;";
html += HtmlUtil.checkbox("",["id","convertcsv_runok"],true) +" Do commands";
html += "&nbsp;&nbsp;";
html +="</td></tr></table>";
html += "<form>";
html += form;
html +="<div style='margin-top:5px;'></div>";
var left = "";
left+=HtmlUtil.href("javascript:csvDisplay('-printheader',null,true)","Header",["title", "Print the header (ctrl-h)", "class","convert_button"])+" ";
left+=HtmlUtil.href("javascript:csvDisplay('-table',null,true)","Table",["title","Display output as table (ctrl-t)", "class","convert_button"])+" ";
left+=HtmlUtil.href("javascript:csvDisplay('-record')","Records",["title","Display output as records", "class","convert_button"])+" ";
left+=HtmlUtil.href("javascript:csvDisplay('-print')","CSV",["title","Display CSV output","class","convert_button"])+" ";
left+=HtmlUtil.href("javascript:csvDisplay('-raw')","Raw",["title", "Don't process. Just show the raw data", "class","convert_button"])+" ";
left+=HtmlUtil.href("javascript:csvDisplay('',true)","Process",["title","Process entire file", "class","convert_button"])+" ";
var right = "";
right+=HtmlUtil.href("javascript:csvClearCommand()","Clear Output",["class","convert_button"])+" ";
right+=HtmlUtil.href("javascript:csvCall('',{listOutput:true})","List Files",["class","convert_button"])+" ";
right+=HtmlUtil.href("javascript:csvCall('',{clearOutput:true})","Remove Files",["class","convert_button"])+" ";
html+=HtmlUtil.leftRightTable(left,right);
html += "</form>";

html +="<div style='margin-top:5px;'></div>";
html +=  HtmlUtil.div(["id", "convertcsv_output","style"," max-height: 500px;  overflow-y: auto;"],"<pre>\n</pre>");
html += HtmlUtil.div(["id", "convertcsv_scratch"],"");
$("#convertcsv_div").html(html);
csvFlipInput(convertCsvLastInput);
$(".convert_button").button();
$(document).click(function(e) {
    if(csvDbPopupTime) {
        var now = new Date();
        var timeDiff = now-csvDbPopupTime;
        if(timeDiff<1000)  {
            return;
        }
    }
    var popup = $("#csv_popup");
    popup.css("display","none");
});
$('#convertcsv_input').keyup(function(e){
    if(e.keyCode == 13) {
        //            csvRunCommand(true);
    }
})


var helpUrl = csvGetUrl("-helpjson");
var jqxhr = $.getJSON( helpUrl, function(data) {
    if(data.error!=null) {
        return;
    }
    if(Utils.isDefined(data.result)) {
        var csvCommandsMap = {}
        var result = window.atob(data.result);
        var select = "<select id=csv_command_select class=ramadda-pulldown>";
        select +=HtmlUtil.tag("option",[],"Commands");
	result= JSON.parse(result);
        result.commands.map(cmd=>{
	    var command = cmd.command;
	    if(cmd.isCategory) {
		select +=HtmlUtil.tag("option",[],cmd.description);
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
            tooltip += command + " " + cmd.args;
            tooltip = tooltip.replace(/\"/g,"&quot;").replace(/\(/g,"").replace(/\)/g,"");
            var label = command;
            label = Utils.camelCase(label.replace("-",""));
            csvCommandsMap[command] = cmd;
            select +=HtmlUtil.tag("option",["value", command,"title",tooltip],"&nbsp;&nbsp;&nbsp;" + label);
        });
        select += "</select>&nbsp;&nbsp;";

        $("#csv_commands").html(select);
        //                $(".ramadda-pulldown").selectBoxIt({});                                 
        $("#csv_command_select").change(function(evt) {
            var cmd = csvCommandsMap[$("#csv_command_select").val()];
	    if(!cmd)return;
            csvInsertCommand(cmd.command +" " + cmd.args);
        });
    }
    

})
    .fail(function(jqxhr, textStatus, error) {
    });

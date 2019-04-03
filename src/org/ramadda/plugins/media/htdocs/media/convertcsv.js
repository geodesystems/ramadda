

var csvDbPopupTime;

function csvGetInput(force) {
    val= "";
    if($("#convertcsv_runok").is(':checked') || force) {
        val =$('#convertcsv_input').val();
    }
    if(val == null) return "";
    return val.trim();
}

function csvDisplay(what, download,html) {
    var command ="";
    lines = csvGetInput().split("\n");
    for(var i=0;i<lines.length;i++){
        line = lines[i].trim();
        if(line =="") continue;
        command +=line;
        command +="\n";
    }
    command = command.trim();
    if(what == null)
        csvCall(command  +" ", {download:download,html:html});
    else
        csvCall(command, {download:download, csvoutput:what,html:html});
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

function csvInsertDb(field,value) {
    var popup = $("#csv_db_popup");
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
        var line = lines[i].trim();
        if(line.startsWith("#")) continue;
        if(line.startsWith("-quit")) break;
        if(line.startsWith("-explode")) {
            doExplode = true;
        }
        cleanCmds+=line+"\n";
    }
    cmds = cleanCmds;
    var rawInput = csvGetInput();
    if((!args.download && !doExplode) || args.html) {
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
    var url = csvGetUrl(cmds,rawInput);
    if(args.download)
        url += "&download=true";
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
                    $("#convertcsv_output").html(result);
                    HtmlUtils.formatTable(".ramadda-table");
                } else {
                    var isDb = result.startsWith("<tables");
                    if(isDb) {
                        result = result.replace("<tables>","Database:");
                        result = result.replace(/\/>/g,"");
                        result = result.replace(/>/g,"");
                        result = result.replace(/<table +id="(.*?)"/g,"\t<table <a class=csv_db_field field='table' onclick=noop() title='Add to input'>$1</a>");
                        result = result.replace("<table ","table:");
                        result = result.replace("</table","");
                        result = result.replace("</tables","");

                        result = result.replace(/<column +name="(.*?)"/g,"\tcolumn: name=\"<a class=csv_db_field field='$1' onclick=noop() title='Add to input'>$1</a>\"");
                        result = result.replace(/ ([^ ]+)="([^"]+)"/g,"\t$1:$2");
                        result = result.replace(/ ([^ ]+)="([^"]*)"/g,"\t$1:\"$2\"");

                    } else {
                        result = result.replace(/</g,"&lt;");
                        result = result.replace(/>/,"&gt;");
                    }
                    var html = "<pre>" + result +"</pre>";
                    if(isDb) {
                        html+="<div class=\"ramadda-popup\" xstyle=\"display: none;position:absolute;\" id=csv_db_popup></div>" ;
                    }
                    $("#convertcsv_output").html(html);
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
                                var popup = $("#csv_db_popup");
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
    var input = $('#convertcsv_input');
    var start = input.prop('selectionStart');
    var end = input.prop('selectionEnd');
    input.val(input.val().substring(0, start)
              +cmds 
              + input.val().substring(end));
    setTimeout(function() {
            input.focus();},0);
    input[0].selectionStart = start+cmds.length;
    input[0].selectionEnd = start+cmds.length;
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
        text = text.replace(/_newline_/g,"\n");
        text = text.replace(/_quote_/g,"\"");
        text = text.replace(/_backslash_/g,"\\");
        text = text.replace(/\"/g,'&quot;');
        val = text;
        csvInputType = "input";
    }  else {
        val = csvGetInput(true);
    }
    if(val == null) val ="";
    if (csvInputType == "textarea") {
        val = val.replace(/\n/g, " ");
        html=HtmlUtil.input("",val,["size","120", "id","convertcsv_input"]) +" " + HtmlUtil.onClick("csvFlipInput()","Expand",[]);
        csvInputType = "input";
    } else {
        html=HtmlUtil.textarea("",val,["style","width:100%;xxfont-size:11pt;", "id","convertcsv_input", "rows", "12"]);
        csvInputType = "textarea";
    }
    $("#convertcsv_input_container").html(html);
}


form="";
//form+=HtmlUtil.input("","",["size","120", "id","convertcsv_input"]);
form+=HtmlUtil.span(["id","convertcsv_input_container"],"");
html = "";

maxRows="Rows: " + HtmlUtil.input("","30",["size","5", "id","convertcsv_maxrows"]);

html+="<table border=0 width=100%><tr><td>";
html+=HtmlUtil.tag("span",["id","csv_commands"],"");
fileSelect = HtmlUtil.href("javascript:void(0);","Select file",["style", "color:black", "onClick", "selectInitialClick(event,'convertcsv_file1','convertcsv_input','true','entry:entryid','" + convertCsvEntry+"');",  "id","convertcsv_file1_selectlink"]);
html += fileSelect;
html += "</td><td align=right>";
html += maxRows;
html += "&nbsp;&nbsp;";
html += HtmlUtil.checkbox("convertcsv_applyall",[], false) + " " +"Apply to siblings";
html += "&nbsp;&nbsp;";
html +=  HtmlUtil.checkbox("csvsave",["id","csvsave"],true) +" Save";
html += "&nbsp;&nbsp;";
html += HtmlUtil.checkbox("",["id","convertcsv_runok"],true) +" Do Commands";
html += "&nbsp;&nbsp;";
html+="</td></tr></table>";
html += "<form>";
html += form;
html +="<p>";
var left = "";
left+=HtmlUtil.href("javascript:csvCall('-header')","Header",["class","convert_button"])+" ";
left+=HtmlUtil.href("javascript:csvDisplay('-table',null,true)","Table",["class","convert_button"])+" ";
left+=HtmlUtil.href("javascript:csvDisplay('-record')","Records",["class","convert_button"])+" ";
left+=HtmlUtil.href("javascript:csvDisplay('-print')","CSV",["class","convert_button"])+" ";
left+=HtmlUtil.href("javascript:csvDisplay('-raw')","Raw",["class","convert_button"])+" ";
left+=HtmlUtil.href("javascript:csvDisplay()","Run",["class","convert_button"])+" ";
left+=HtmlUtil.href("javascript:csvDisplay('',true)","Make Files",["class","convert_button"])+" ";
var right = "";
right+=HtmlUtil.href("javascript:csvClearCommand()","Clear Output",["class","convert_button"])+" ";
right+=HtmlUtil.href("javascript:csvCall('',{listOutput:true})","List Files",["class","convert_button"])+" ";
right+=HtmlUtil.href("javascript:csvCall('',{clearOutput:true})","Remove Files",["class","convert_button"])+" ";
right+=HtmlUtil.href("javascript:csvCall('-help')","Help",["class","convert_button"]);
html+=HtmlUtil.leftRight(left,right);
html += "</form>";

html += "<p>" +HtmlUtil.div(["id", "convertcsv_output","style"," max-height: 500px;  overflow-y: auto;"],"<pre>\n</pre>");
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
    var popup = $("#csv_db_popup");
    popup.css("display","none");
    });
$('#convertcsv_input').keyup(function(e){
        if(e.keyCode == 13) {
            //            csvRunCommand(true);
        }
    })


    var helpUrl = csvGetUrl("-helpraw");
    var jqxhr = $.getJSON( helpUrl, function(data) {
            if(data.error!=null) {
                return;
            }
            if(Utils.isDefined(data.result)) {
                var csvCommandsMap = {}
                var result = window.atob(data.result);
                var select = "<select id=csv_command_select class=ramadda-pulldown>";
                select +=HtmlUtil.tag("option",[],"Commands");
                var lines = result.split("\n");
                for(var i=0;i<lines.length;i++){
                    line = lines[i].trim();
                    if(line =="" || line == "CsvUtil") continue;
                    if(line.startsWith("-help")) continue;
                    var index = line.indexOf(" ");
                    var command = line;

                    if(index>0) {
                        command  = line.substring(0,index).trim();
                        //                        if(line.includes("decimate")) {
                            //                            console.log(index +" " + command +" line:" + line);
                        //}
                    }
                    if(!command.startsWith("-")) continue;
                    var tooltip="";
                    var arr = line.match(/\((.*?)\)/g);
                    if(arr && arr.length>0) {
                        tooltip = arr[0];
                        tooltip = tooltip.replace(/\"/g,"&quot;");
                        tooltip = tooltip.replace(/\(/g,"");
                        tooltip = tooltip.replace(/\)/g,"");
                        //                        console.log("tt:" + tooltip);
                    }

                    var label = command;
                    label = Utils.camelCase(label.replace("-",""));
                    line = line.replace(/\(.*?\)/g,"");
                    csvCommandsMap[command] = line;
                    select +=HtmlUtil.tag("option",["value", command,"title",tooltip],label);
                }
                select += "</select>&nbsp;&nbsp;";
                $("#csv_commands").html(select);
                //                $(".ramadda-pulldown").selectBoxIt({});                                 
                $("#csv_command_select").change(function(evt) {
                        var line = csvCommandsMap[$("#csv_command_select").val()];
                        csvInsertCommand(line);
                    });
            }
            

        })
        .fail(function(jqxhr, textStatus, error) {
            });

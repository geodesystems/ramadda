


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

function csvCall(cmds,args) {
    if (!args)  {
        args = {};
    }
    stop = 
        HtmlUtil.onClick("csvStop()","Stop",[]);
    csvOutput("<pre>\nProcessing...\n" + stop +"</pre>");

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
    console.log("commands:" + cmds);
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
                csvOutput("<pre>Error:" + window.atob(data.error) +"</pre>");
                return;
            }
            if(Utils.isDefined(args.func)) {
                //                args.func(data);
                //                return;
            }
            if(data.file) {
                csvOutput("<pre></pre>");
                iframe = '<iframe src="' + data.file +'"  style="display:none;"></iframe>';
                $("#convertcsv_scratch").html(iframe);
                return;
            } 
            if(Utils.isDefined(data.html) ) {
                html = window.atob(data.html);
                html = "<pre>" + html +"</html>";
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
                } else {
                    result = result.replace(/</g,"&lt;");
                    result = result.replace(/>/,"&gt;");
                    $("#convertcsv_output").html("<pre>" + result +"</pre>");
                }
                return;
            }
            $("#convertcsv_output").html("<pre>No response given</pre>");
        })
        .fail(function(jqxhr, textStatus, error) {
                var err = textStatus + ", " + error;
                 csvOutput("<pre>Error:" + err +"</pre>");
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


function csvAppendCommand(cmds) {
    if(!cmds) return;
    cmds = cmds.replace(/_quote_/g,"\"");
    $('#convertcsv_input').val($('#convertcsv_input').val()+" "+ cmds);
}

function csvOutput(html) {
    $('#convertcsv_output').html(html);
}

function csvClearCommand() {
    csvOutput("<pre>\n</pre>");
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
        html=HtmlUtil.textarea("",val,["style","width:100%;", "id","convertcsv_input", "rows", "10", "style", "font-size:14px;"]);
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
html+="<span id=csv_commands></span>";
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
                select +="<option>Commands</option>"
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
                    var label = command;
                    label = Utils.camelCase(label.replace("-",""));
                    line = line.replace(/\(.*?\)/g,"");
                    csvCommandsMap[command] = line;
                    select +="<option value=\"" + command +"\">" + label +"</option>\n";
                }
                select += "</select>&nbsp;&nbsp;";
                $("#csv_commands").html(select);
                $(".ramadda-pulldown").selectBoxIt({});                                 
                $("#csv_command_select").change(function() {
                        var line = csvCommandsMap[$("#csv_command_select").val()];
                        csvAppendCommand(line);
                    });
            }
            

        })
        .fail(function(jqxhr, textStatus, error) {
            });

var viewFormulas = "formulas";
var viewStyles = "styles";
var viewValues = "values";
var cols = 10;
var rows = 15;

function loadFromRamadda(entryId) {
    url = urlroot + "/entry/show?entryid="  + entryId + "&spreadsheet.getxml=true";
    util.loadXML( url, handleSpreadsheetXml, null);
}


function handleSpreadsheetXml(request, obj) {
    var xmlDoc=request.responseXML.documentElement;
    var content = getChildText(xmlDoc);
    loadSpreadsheet(content);
}



function save(format) {
  var out = "";
  if (format == "csv") out = cellsToCSV();
    else if (format == "tsv") out = cellsToTSV();
	else out = cellsToJS();

    var postParams = "entryid=" + encodeURI(entryId) +
        "&spreadsheet.storedata=" +
        encodeURI(out);
    var postUrl = urlroot + "/entry/show";
    makePOSTRequest(postUrl, postParams);
    return;


    hidden = util.getDomObject('spreadsheet.storedata');
    if(!hidden) {
        alert('could not find save form entry'); 
        return;
    }



    hidden.obj.value = out;
    form = util.getDomObject('ssform');
    if(!form) {
        alert('bad form'); 
        return;
    }
    form.obj.submit();

    setDisplay("data", "none");
    setDisplay("source", "inline");
    setValue("code", out);
}


function manual() {
    window.open(urlroot+"/spreadsheet/manual.html","manual","toolbar=no,location=no,directories=no,status=no,menubar=no,scrollbars=yes,resizable=yes,copyhistory=no,width=800,height=600");
}




function makeHeader() {
  var divider = " | ";
  var out = "<div id='ssheader' class='ssheader'><table  cellpadding='0' cellspacing='0' style='width:100%;'><tr><td nowrap>";
  out += "<textarea id='focus' onfocus='this.blur();'></textarea>";
  out += "&nbsp;";
  if (top.window!=window) {
    out += "<a href='#' onclick='window.frameElement.parentNode.style.height = window.frameElement.parentNode.offsetHeight+60+\"px\"; return false;'>(+)</a> - ";
    out += "<a href='#' onclick='window.frameElement.parentNode.style.height = window.frameElement.parentNode.offsetHeight-60+\"px\"; return false;'>(&ndash;)</a> - ";
	if (closeMethod!="") out += "<a href='#' onclick='if (confirm(\"Really close ?\")) eval(closeMethod); return false;' accesskey='q'>"+trans("Close")+"</a> - ";
  }
  if (isWriteable) {
      //jeffmc:    out += "<a href='#' onclick='if (confirm(\""+trans("Really close without saving changes ?")+"\")) loadSpreadsheet(init_data); return false;' accesskey='n'>"+trans("New")+"</a> - ";
      //jeffmc:    out += "<a href='#' onclick='loadCode(); return false;' accesskey='l'>"+trans("Load")+"</a> - ";
      if (saveMethod!="") out += "<a href='#' onclick='eval(saveMethod); return false;' accesskey='s'>"+icon("/icons/page_save.png", trans("Save"))+"</a>" + divider;
  }
  out += "<a href='#' onclick='print(); return false;' accesskey='p'>"+icon("/icons/printer.png", trans("Print"))+"</a>" + divider;
  if (allowPaging) {
      out += makeLink("pageHome()", icon("/icons/application_home.png",trans("Home")), col0-cols>=0 || row0-rows>=0)+" ";
      out += makeLink("pageLeft()", icon("/icons/arrow_left.png",trans("Page left")),col0-cols>=0)+" ";
      out += makeLink("pageRight()", icon("/icons/arrow_right.png",trans("Page right")),1)+" ";
      out += makeLink("pageUp()", icon("/icons/arrow_up.png", trans("Page up")),row0-rows>=0)+" ";
      out += makeLink("pageDown()",icon("/icons/arrow_down.png", trans("Page down")),1)+" &nbsp;";
      out += "</td><td class=\"sstd\" nowrap>";
      out += "<input class='ssinput' type='text' value='"+cols+"' style='width:28px; text-align:center;' id='cols' onfocus='active=\"dimensions\";' onblur='active=\"content\";'> x ";
      out += "<input class='ssinput' type='text' value='"+rows+"' style='width:28px; text-align:center;' id='rows' onfocus='active=\"dimensions\";' onblur='active=\"content\";'>" + divider;
  }
  out += "<input class='ssinput' type='text' value='' title='"+trans("Position")+"' id='field' style='width:30px; text-align:center;' onfocus='active=\"position\";' onblur='active=\"content\";' accesskey='g'> -&nbsp;";
  out += "</td><td class=\"sstd\" nowrap style='width:100%;'>";
  out += "<iframe src='about:blank' id='multiline'></iframe>";
  out += "<input class='ssinput' type='text' value='' title='"+trans("Formula")+"' id='value' style='width:350;' disabled onmouseover='previewValue();' onkeyup='previewValue();'> ";
  out += "<input class='ssinput'  type='text' title='"+trans("Style")+"' value='' id='styling' style='width:80;' disabled onmouseover='previewValue();' onkeyup='previewValue();'> ";
  out += "</td><td class=\"sstd\" nowrap>";
  if (isWriteable) {
      out += "&nbsp; <input type='button' value='"+trans("Save")+"' id='save' onclick='saveCell();' disabled>&nbsp;";
        out += "<input type='button' value='"+trans("X")+"' id='cancel' onclick='cancelCell();' disabled>" + divider;
  } else out += "&nbsp; ";
  if (view=="values") {
    if (isWriteable && agent=="msie") {
      out += "<a href='#' onclick='auto_recalc=!auto_recalc; display(); return false;' accesskey='m'>"+(auto_recalc?trans("Auto")+"-"+trans("Refresh"):trans("Manual"))+"</a>";
      if (!auto_recalc) out += " <a href='#' onclick='display(); return false;' accesskey='r'>"+trans("Refresh")+"</a>";
	  out += divider;
	}
    out += "<a href='#' onclick='return setView(viewFormulas);'>"+trans("Values")+"</a>" + divider;
  } else if (view=="formulas") {
    out += "<a href='#' onclick='return setView(viewStyles);'>"+trans("Formulas")+"</a>" + divider;
  } else {
    out += "<a href='#' onclick='return setView(viewValues);'>"+trans("Styles")+"</a>" + divider;
  }
  out += "<a href='#' onclick='return setView(viewValues);' accesskey='1'></a>";
  out += "<a href='#' onclick='return setView(viewFormulas);' accesskey='2'></a>";
  out += "<a href='#' onclick='return setView(viewStyles);' accesskey='3'></a>";
  out += "<a href='#' onclick='manual(); return false;' accesskey='h'>"+trans("Help")+"</a>" + divider;

  // You are not allowed to remove or alter the About button and/or the copyright.
  out += "<a href='#' onclick='showAbout(); return false;'>"+trans("About")+"</a>&nbsp;";
  out += "</td></tr></table></div>";
  return out;

}

function makeFooter() {
  var out = "<div class='ssfooter' id='ssfooter' onmouseover='getObj(\"status\").innerHTML=\"\";'>&nbsp;";
  if (isWriteable) {
    out += "<a href='#' onclick='insertRow(); return false;'>"+trans("Insert Row")+"</a> - ";
    out += "<a href='#' onclick='insertColumn(); return false;'>"+trans("Insert Column")+"</a> - ";
    out += "<a href='#' onclick='if (confirm(\""+trans("Really delete entire row ?")+"\")) deleteRow(); return false;'>"+trans("Delete Row")+"</a> - ";
    out += "<a href='#' onclick='if (confirm(\""+trans("Really delete entire column ?")+"\")) deleteColumn(); return false;'>"+trans("Delete Column")+"</a> - ";
  }
  out += "<a href='#' onclick='sort(1); return false;'>"+trans("Sort asc.")+"</a> - ";
  out += "<a href='#' onclick='sort(0); return false;'>"+trans("Sort desc.")+"</a> - ";

  if (isWriteable) {
    out += "<a href='#' onclick='cutcopy(\"cut\",\"#FFDDDD\"); return false;' title='Alt-x' accesskey='x'>"+trans("Cut")+"</a> - ";
    out += "<a href='#' onclick='cutcopy(\"copy\",\"#DDDDFF\"); return false;' title='Alt-c' accesskey='c'>"+trans("Copy")+"</a> - ";
    out += "<a href='#' onclick='paste(); return false;' title='Alt-v' accesskey='v'>"+trans("Paste")+"</a> - ";
    out += "<a href='#' onclick='if (confirm(\""+trans("Really empty cell(s) ?")+"\")) removeSelectedCell(); return false;' title='Alt-e' accesskey='e'>"+trans("Empty")+"</a> - ";
  } else {
    out += "<a href='#' onclick='save(\"js\"); return false;'>"+trans("Export to JS")+"</a> - ";
  }
  out += "<a href='#' onclick='save(\"csv\"); return false;'>"+trans("Export to CSV")+"</a> - ";
  out += "<a href='#' onclick='save(\"tsv\"); return false;'>"+trans("Export to TSV")+"</a>";
  out += "</div>";
  out += "<div id='status' class='status'></div>";
  return out;
}


function setView(newView) {
    view = newView;
    display();
    return false;
}


function showAbout() {
    alert("Simple Spreadsheet is an open source component created by Thomas Bley\nand licensed under GNU GPL v2.\n\nSimple Spreadsheet is copyright 2006-2007 by Thomas Bley.\nTranslations implemented by Sophie Lee.\n\nMore information and documentation at http://www.simple-groupware.de/\n");
}


function pageHome() {
    row0=0; 
    col0=0; 
    currCol=0; 
    currRow=0; 
    scroll(); 
    display(); 
    return false;
}

function pageLeft() {
    col0 -= cols; 
    currCol -= cols; 
    display(); 
    return false;
}


function pageRight() {
    col0 += cols; 
    currCol += cols; 
    display(); 
    return ;
}


function pageUp() {
    row0 -= rows; 
    currRow -= rows; 
    display(); 
    return false;

}

function pageDown() {
    row0 += rows; 
    currRow += rows; 
    display(); 
    return false;
}


function makeLink(action, label, enabled) {
    if(!enabled) return label;
    return  "<a href='#' onclick='" + action +";return false;'>"+label+"</a>";
}

function trans(key) {
    if (strings[key]) return strings[key]; else return key;
}


function icon(path, alt) {
    if(alt)
        return "<img src=\"" +urlroot +path+"\" border=0 title=\"" + alt +"\" alt=\"" + alt +"\">";
    return "<img src=\"" +urlroot +path+"\" border=0>";
}





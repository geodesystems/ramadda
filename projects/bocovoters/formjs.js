let textarea = $('textarea[name="search.db_boco_voters.precinct"]');
textarea.parent().append("<br><div id=formjs_generate>Generate Precinct Printing Script</div>");
$("#formjs_generate").button().click(()=>{
    let sh  = "#!/bin/sh\n"
    sh+="#\n#this needs the CAPTUREPDF env variable set pointing to capturepdf.sh\n#or to be run in the same directory as the capturepdf.sh and capturepdf.scpt files\n#\n"
    sh +="export mydir=`dirname $0`\n";
    sh+="if [ -f \"${CAPTURE_PDF_PATH}/capturepdf.sh\" ]\nthen\n";
    sh +="\tcapturepdf=${CAPTURE_PDF_PATH}/capturepdf.sh\n"
    sh+="elif [ -f \"$mydir/capturepdf.sh\" ]\nthen\n";
    sh +="\tcapturepdf=$mydir/capturepdf.sh\n"
    sh+="else\n";
    sh+="\techo \"No capturepdf.sh defined\"\n";
    sh+="\texit\n";
    sh+="fi\n";

    let url = $("#formurl").val().trim();
    url = url.replace(/search.db_boco_voters.precinct=.*?&/,"");
    url = url +"&forprint=true";
    let input = (textarea.val() ||"").trim();
    if(input=="") {
	alert('No precincts specified');
	return;
    }
    input.split("\n").forEach(line=>{
        line = line.trim();
	if(line=="") return;
	let _url = url+"&searchname=Precinct+" + line;
	console.log(_url);
	sh  = sh +"sh ${capturepdf} \"" + _url +"&search.db_boco_voters.precinct=" + line + "\"  precinct_" + line +".pdf\n"; 
    });
    sh+="\n";
    Utils.makeDownloadFile("precincts.sh",sh);
});

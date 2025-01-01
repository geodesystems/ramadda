/**
   Copyright (c) 2008-2025 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/



Ramadda.initAccessForm = function() {
    let index = 0;
    let form = $("#accessform");
    let roles = form.find("#roles");
    form.find("textarea").focus(function() {
	index = $(this).attr('roleindex');
	let holder = $("#holder_" + index);
	roles.appendTo(holder);
    });
    roles.change(function() {
	let role = roles.val();
	if(role=="") return;
	roles.val("");
	role="\n"+role+"\n";
	let textarea = $("#textarea_" + index);
	let val = (textarea.val()||"").trim();

        let start = textarea[0].selectionStart;
        let end = textarea[0].selectionEnd;
	if(!Utils.isDefined(start) || val=="") {
	    val = val+role;
	} else  {
	    val = val.substring(0, start) + role +
                val.substring(end);
	}
	let newVal = "";
	val.split("\n").forEach(line=>{
	    line = line.trim();
	    if(line=="") return;
	    line = line+"\n";
	    newVal = newVal+line;
	});
	textarea.val(newVal);
    });


}

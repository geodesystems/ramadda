/**
 * Copyright (c) 2008-2021 Geode Systems LLC
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
	let v = roles.val();
	if(v=="") return;
	roles.val("");
	let textarea = $("#textarea_" + index);
	let val = (textarea.val()||"").trim();
	if(val!="") val+="\n";
	val+=v;
	textarea.val(val);
	

    });


}

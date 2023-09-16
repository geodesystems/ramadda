/**
   Copyright (c) 2008-2023 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/


HtmlUtils.initTypeMenu = function InitTypeMenu(selectId, textAreaId) {

    jqid(selectId).change(function() {
	let v = $(this).val();
	if(!Utils.stringDefined(v)) {
	    return
	}
	$(this).val("");
	let  t = jqid(textAreaId).val()??"";
	t = t.trim();
	if(t!="") t = t+"\n";
	t+=v+":";
	jqid(textAreaId).val(t);
    });

}

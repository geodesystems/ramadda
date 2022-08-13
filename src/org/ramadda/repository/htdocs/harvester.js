
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

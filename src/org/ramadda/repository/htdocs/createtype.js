var CreateType  ={
    init:function(formId,entryId,json) {
	let storageKey=entryId+'_createtype';
//	let formData = json ?? Utils.getLocalStorage(storageKey,true);
	let formData = json ?? null;
	let form = jqid(formId);
	if(formData) {
//	    jqid(formId+'_button').html(HU.span([ATTR_CLASS,'ramadda-clickable',ATTR_ID,'clearform'],'Clear Saved State'));
//	    jqid('clearform').button().click(()=>{
//		Utils.setLocalStorage(storageKey, null);
//	    })
	    for(let i=0;i<formData.length;i++) {
		let item = formData[i];
		if(item.name) item.n = item.name;
		if(item.value) item.v = item.value;
		if(item.n=='entryid') continue;
		let input = form.find('input[name="' + item.n+'"]');
		if(input.length==0)
		    input = form.find('textarea[name="' + item.n+'"]');
		if(input.length==0)
		    input = form.find('select[name="' + item.n+'"]');		
		if(input.length==0) {
		    console.log('could not find input:' + item.n);
		} else {
		    input.val(item.v);
		}
	    };
	}
	form.submit(function(event){
	    let jsonContents = form.find('[name="json_contents"]');
	    if(jsonContents.length==0) {
		$('<input>').attr({
		    type: 'hidden',
		    name: 'json_contents',
		}).appendTo($(this));
		jsonContents = form.find('[name="json_contents"]');
	    }
            let formData = $(this).serializeArray().filter(field=>{
		return field.name !== 'json_contents' && field.name!=='entryid' && field.value!='';
	    }).map(field=>{
		field.n = field.name;
		field.v = field.value;
		delete field.name;
		delete field.value;
		return field;
	    });

	    let json =   JSON.stringify(formData);
	    jsonContents.val(json);
//	    console.log(jsonContents.length);
//            event.stopPropagation();
//	    return false;
//	    Utils.setLocalStorage(storageKey, formData,true);
	});

    }
}

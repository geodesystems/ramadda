var CreateType  ={
    init:function(formId,entryId) {
	let storageKey=entryId+'_createtype';
	let state = Utils.getLocalStorage(storageKey,true);
	let form = jqid(formId);
	if(state) {
	    jqid(formId+'_button').html(HU.span([ATTR_CLASS,'ramadda-clickable',ATTR_ID,'clearform'],'Clear Saved State'));
	    jqid('clearform').button().click(()=>{
		Utils.setLocalStorage(storageKey, null);
	    })
	    for(let i=0;i<state.length;i++) {
		let item = state[i];
		let input = form.find('input[name="' + item.name+'"]');
		if(input.length==0)
		    input = form.find('textarea[name="' + item.name+'"]');
		if(input.length==0)
		    input = form.find('select[name="' + item.name+'"]');		
		if(input.length==0) {
		    console.log('could not find input:' + item.name);
		} else {
		    input.val(item.value);
		}
	    };
	}
	form.submit(function(e){
            let formData = $(this).serializeArray();
	    Utils.setLocalStorage(storageKey, formData,true);
	});

    }
}

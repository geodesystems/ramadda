var RamaddaLicense =  {
    checkLicense:function(domId,required,args) {
	let opts = {
	    message:"To access this content do you agree with the following license?",
	    showLicense:true,
	    suffix:'',
	    onlyAnonymous:false,
	    redirect:ramaddaBaseUrl,
	    logName:false
	}
	if(args) $.extend(opts,args);
	let text = jqid(domId).html();
	let key = 'licenseagree_' + required;
	let agreed = Utils.getLocalStorage(key);
	if(opts.onlyAnonymous && !Utils.isAnonymous()) return;
	if(!agreed) {
	    let buttonList = [HU.div(['action','ok','class','ramadda-button ' + CLASS_CLICKABLE],
				     "Yes"),
			      HU.div(['action','no','class','ramadda-button ' + CLASS_CLICKABLE],"No")]

	    let buttons = HU.buttons(buttonList);
	    let html =  opts.message;
	    html+=(opts.showLicense?HU.div([],text):'<br>')+opts.suffix;
	    if(opts.logName) {
		html+=HU.vspace('1em');
		html+=HU.div([],'Please enter your contact information');
		html+=HU.formTable();
		html+=HU.formEntry("Name:",HU.input('','',['tabindex','1',ATTR_ID,'licenseagreename','size','30']));
		html+=HU.formEntry("Email:",HU.input('','',['tabindex','2',ATTR_ID,'licenseagreeemail','size','30']));		
		html+=HU.formTableClose();
	    }


	    html+=buttons;

	    html = HU.div([ATTR_CLASS,'ramadda-license-dialog'], html);
	    let dialog =  HU.makeDialog({anchor:$(window),
					 at:'left+100 top+100',
					 my:'left top',
					 content:html,
					 remove:false,modalStrict:true,sticky:true});


	    dialog.find('.ramadda-button').click(function() {
		if($(this).attr('action')!='ok') {
		    window.location.href=opts.redirect;
		    return;
		}
		let name = "";
		let email="";
		if(opts.logName) {
		    name = jqid('licenseagreename').val().trim();
		    email = jqid('licenseagreeemail').val().trim();		    
		    if(!Utils.stringDefined(name) || !Utils.stringDefined(email)) {
			alert('Please enter your contact information');
			return;
		    }
		}
		let url = HU.url(RamaddaUtil.getUrl('/loglicense'),
				 ["licenseid",required,
				  "name",name,"email",email,
				  "entryid",opts.entryid]);
		$.getJSON(url, data=>{});
		Utils.setLocalStorage(key, true);
		dialog.remove();
	    });
	}
    }
}

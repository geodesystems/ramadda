var UserInput = {
    initUserInput:function() {
	let change = (element) =>{
	    let value = element.val();
	    Utils.setLocalStorage(element.attr('userinput-key'), value,false,true);
	};


	let inputs =[];
	let download = ()=> {
	    let json = {};
	    inputs.forEach(ele=>{
		json[ele.attr('id')] = ele.val();
	    });
	    Utils.makeDownloadFile("download.json",JSON.stringify(json));
	};
	    
	let readFile = (src) =>{
	    let selectedFile;
	    let processFile=() =>{
		if (!selectedFile) return;
		const reader = new FileReader();
		reader.onload = function (evt) {
		    const text = evt.target.result;
		    try {
			let json = JSON.parse(text);
			return Object.keys(json).map(key=>{
			    jqid(key).val(json[key]);
			});
		    } catch(err) {

			alert('An error occurred:' + err);
		    }
		};
		reader.onerror = function (err) {
		    console.error("Error reading file:", err);
		};
		reader.readAsText(selectedFile);   // <-- read as text

	    };


	    let uid = HU.getUniqueId("");
	    let html =HU.tag(TAG_INPUT,[ATTR_PLACEHOLDER,'Select file',
					ATTR_TYPE,'file',
					ATTR_ID,uid,
					'accept','.json,application/json']);
	    let buttons  =
		HU.div([ATTR_CLASS,HU.classes(CLASS_BUTTON,CLASS_BUTTON_OK)],
		       LABEL_OK) + 
		HU.div([ATTR_CLASS,HU.classes(CLASS_BUTTON,CLASS_BUTTON_CANCEL)],
		       LABEL_CANCEL);	    
	    html+=HU.div([ATTR_CLASS,CLASS_BUTTONS], buttons);
	    html=HU.div([ATTR_CLASS,CLASS_DIALOG],html);
	    let dialog = HU.makeDialog({content:html,anchor:src,
					title:'Select File',
					header:true,
					decorate:true,
					modal:true,
					my:POS_LEFT_TOP,at:POS_RIGHT_BOTTOM});

	    dialog.find(HU.dotClass(CLASS_BUTTON)).button().click(function() {
		if($(this).hasClass(CLASS_BUTTON_OK)) {
		    if(!selectedFile) {
			alert('No file selected');
			return;
		    }
		    processFile();
		}
		dialog.remove();
	    });
	    jqid(uid).on("change", function (e) {
		selectedFile = e.target.files[0];
	    });
	}


	$(HU.dotClass('ramadda-user-note')).each(function() {
	    let id  =$(this).attr("id");
	    let key = 'userinput-'  + id;
	    $(this).attr('userinput-key',key);
	    let value = Utils.getLocalStorage(key,false,true);
	    if($(this).is('[type="submit"]')) {
		$(this).click(()=>{
		    let action = $(this).attr('action');
		    if(action=='download') {
			download();
		    } else  if(action=='upload') {
			readFile($(this));
		    } else  if(action=='save') {
			inputs.forEach(element=>{
			    change(element);
			});

		    } else {
			alert('Unknown button action:' + action);

		    }
		    return;

		});
		return;
	    }
	    inputs.push($(this));
	    if(Utils.isDefined(value)) {
		value = Utils.getProperty(value);
                $(this).val(value);
	    }
	    if($(this).is('textarea')) {
		$(this).keydown(function() {
		    //		    change($(this));
		});
	    } else {
		$(this).keyup(function(event) {
		    if(!Utils.isReturnKey(event)) return;
		    change($(this));
		});
	    }
	});

    },
    initChecklist:function() {
	$(HU.dotClass('ramadda-checklist')).each(function() {
	    let id  =$(this).attr("id");
	    let value = Utils.getLocalStorage(id,false,true);
	    if(Utils.isDefined(value)) {
		value = Utils.getProperty(value);
                $(this).prop('checked',value);
	    }
	    $(this).change(function() {
		let value = HU.isChecked($(this));
		Utils.setLocalStorage(id, value,false,true);
	    });
	});
    },
    
}

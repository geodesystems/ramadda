var COLUMNS_MAX_ROWS = 100;
var ID_CT_BULKTEXT='bulktext';
var ID_CT_TYPEID ='typeid';
var ID_CT_SUPER ='supertype';
var ID_CT_TYPENAME ='typename';
var ID_CT_CATEGORY ='category';
var ID_CT_SUPERCATEGORY ='supercategory';
var ID_CT_ICON='icon';
var ID_CT_HANDLER_EXTRA='handler_extra';



var CreateType  = {
    domId:function(id) {
	return this.baseDomId+id;
    },
    jq:function(id) {
	return jqid(this.domId(id));
    },
    init:function(formId,entryId,json) {
	let _this = this;
	this.baseDomId = Utils.getUniqueId('createtype_');
	let storageKey=entryId+'_createtype';
//	let formData = json ?? Utils.getLocalStorage(storageKey,true);
	let formData = json ?? null;
	let form = jqid(formId);
	jqid('headerbuttons').append(HU.span([ATTR_CLASS,'ct_bulkupload'],'Bulk Upload'));
	jqid('colbuttons').append(HU.buttons([
	    HU.span([ATTR_ID,'clearcols'],'Clear'),
	    HU.span([ATTR_CLASS,'ct_bulkupload'],'Bulk Upload'),
	    HU.span([ATTR_ID,'textdownload'],'Download Text')]));
	_this.currentColumn = null;
	this.columns = [];
	for(let i=0;i<COLUMNS_MAX_ROWS;i++) {
	    let c =HU.jqname('column_name_' + i);
	    if(!c.length) break;
	    this.columns.push({
		index:i,
		name:c,
		label:HU.jqname('column_label_' + i),
		type:HU.jqname('column_type_' + i),
		extra:HU.jqname('column_extra_'+i)
	    });
	}
	this.columns.forEach(c=>{
	    c.name.on('focus', function() {	    
		_this.currentColumn=c;
		_this.columns.forEach(c=>{
		    c.name.css(CSS_BACKGROUND,COLOR_WHITE);
		});
		$(this).css(CSS_BACKGROUND,COLOR_MELLOW_YELLOW);
	    });
	});

	let  popup =function(event,widget,c) {
	    event.preventDefault();
	    let html = '';
	    let clazz=HU.classes(CLASS_MENU_ITEM,CLASS_CLICKABLE);
	    html+=HU.div([ATTR_CLASS,clazz,
			  ATTR_DATA_ACTION,'insert-above'],'Insert rows above');
	    html+=HU.div([ATTR_CLASS,clazz,
			  ATTR_DATA_ACTION,'insert-below'],'Insert rows below');
	    html+=HU.div([ATTR_CLASS,clazz,
			  ATTR_DATA_ACTION,'clear'],'Clear row');		
	    html+=HU.div([ATTR_CLASS,clazz,
			  ATTR_DATA_ACTION,'delete'],'Delete rows');		
	    let dialog =  HU.makeDialog({anchor:widget,
					 at:POS_LEFT_BOTTOM,
					 my:POS_LEFT_TOP,
					 content:html});
	    dialog.find(HU.dotClass(CLASS_MENU_ITEM)).click(function() {
		let action = $(this).attr(ATTR_DATA_ACTION);
		_this.handleRowAction(c,action);
		dialog.remove();
	    });
	};

	this.columns.forEach(c=>{
	    [c.name,c.label,c.extra].forEach(widget=>{
		widget.on("contextmenu", function(event) {
		    popup(event,$(this),c);
		});
	    });
	});
	jqid("clearcols").button().click(()=>{
	    if (!confirm("Are you sure you want to clear the columns?")) {
		return;
	    }
	    this.clearColumns();
	});
	jqid("textdownload").button().click(function(){
	    let text = ''
	    for(let i=0;i<50;i++) {
		let name = HU.jqname('column_name_' + i).val();
		let label = HU.jqname('column_label_' + i).val();		
		let type = HU.jqname('column_type_' + i).val();
		let extra = HU.jqname('column_extra_' + i).val();
		if(Utils.stringDefined(name) ||Utils.stringDefined(label)) {
		    extra = extra.replace(/,/g,'\\,');
		    extra = extra.replace(/\n/g,'\\n');		    
		    text+=Utils.join([name,label,type,extra],',');
		    text+='\n';
		}
	    }
	    Utils.makeDownloadFile('columns.txt',text);
	});
	$('.ct_bulkupload').button().click(function(){
	    let html = 'Enter columns, one per line. "name,label,type,extra"';
	    if(_this.currentColumn) {
		html+=HU.br()+HU.checkbox('insertabove',[ATTR_ID,'insertabove'],
					  false,'Insert above selected row' +' ' + _this.currentColumn.name.val());
	    }

	    html+=HU.br();
	    let buttonList = [HU.div([ATTR_ACTION,ACTION_OK,
				      ATTR_CLASS,HU.classes(CLASS_BUTTON, CLASS_CLICKABLE)],
				     "Load"),
			      HU.div([ATTR_ACTION,ACTION_CANCEL,ATTR_CLASS,HU.classes(CLASS_BUTTON,CLASS_CLICKABLE)],LABEL_CANCEL)]

	    let buttons = HU.buttons(buttonList);
	    html+=HU.textarea(ID_CT_BULKTEXT,'',[ATTR_ID,ID_CT_BULKTEXT,'rows',10,'cols',60]);
	    html+=HU.div([],
			 HU.checkbox('bulk_force',[ATTR_TITLE,'Override any values',ATTR_ID,'bulk_force'],
				     false,'Force Override') +SPACE2 +			 
			 HU.checkbox('clearrows',[ATTR_ID,'clearrows'],true,'Clear all rows') +SPACE);

	    html+=buttons;
	    html = HU.div([ATTR_STYLE,HU.css(CSS_MIN_WIDTH,HU.px(600)),
			   ATTR_CLASS,'ramadda-license-dialog'], html);
	    let dialog =  HU.makeDialog({anchor:$(this),
					 at:'left+100 top+100',
					 my:POS_LEFT_TOP,
					 content:html,
					 title:'Bulk upload',
					 header:true,
					 draggable:true});
	    jqid(ID_CT_BULKTEXT).focus();
	    dialog.find(HU.dotClass(CLASS_BUTTON)).button().click(function() {
		if($(this).attr(ATTR_ACTION)==ACTION_OK) {
		    if(HU.isChecked(jqid('clearrows'))) {
			_this.clearColumns();
		    }
		    _this.handleBulkUpload(jqid(ID_CT_BULKTEXT).val());


		}
		dialog.remove();
	    });
	});
	this.applyFormData(form,formData);
	let extras = $('.typecreate-column-extra');
	extras.each(function() {
	    _this.checkColumnExtra($(this));
	});
	extras.on('input',function() {
	    _this.checkColumnExtra($(this));
	});
	this.initLabel();
	form.submit(function(event){
	    _this.initLabel();
	    let jsonContents = form.find('[name="json_contents"]');
	    if(jsonContents.length==0) {
		$('<input>').attr({
		    type: 'hidden',
		    name: 'json_contents',
		}).appendTo($(this));
		jsonContents = form.find('[name="json_contents"]');
	    }
            let formData = $(this).serializeArray().filter(field=>{
		return _this.canSaveInput(field.name) && field.value!='';
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

    },
    clearColumns:function() {
	for(let i=0;i<COLUMNS_MAX_ROWS;i++) {
	    let input =HU.jqname('column_name_' + i);
	    if(!input.length) break;
	    input.val('');
	    HU.jqname('column_label_' + i).val('');
	    HU.jqname('column_type_' + i).val('');
	    HU.jqname('column_extra_' + i).val('');						
	}
    },
    applyFormData:function(form,formData) {
	if(!formData) return;
	for(let i=0;i<formData.length;i++) {
	    let item = formData[i];
	    if(item.name) item.n = item.name;
	    if(item.value) item.v = item.value;
	    if(!this.canSaveInput(item.n)) continue;
	    let input = form.find('input[name="' + item.n+'"]');
	    if(input.length==0)
		input = form.find('textarea[name="' + item.n+'"]');
	    if(input.length==0)
		input = form.find('select[name="' + item.n+'"]');		
	    if(input.length==0) {
		console.log('could not find input:' + item.n);
	    } else {
		//		    console.log(item.n,input.attr('name'),item.v);
		if(input.attr("type") === "checkbox") {
		    if(String(item.v)==="true") {
			input.prop('checked',true);
		    } else {
			input.prop('checked',false);
		    }
		} else {
		    input.val(item.v);
		}
	    }
	}
    },

    checkColumnExtra:function(widget) {
	let v = widget.val();
	if(!v) return;
	let lines = Utils.split(v,'\n');
	if(lines.length>1) {
	    widget.attr('rows',lines.length);
	} else {
	    widget.attr('rows',1);
	}
    },
    initLabel:function() {
	let label = jqid('basic_tab_label');
	let  type = HU.jqname(ID_CT_TYPEID).val();
	let  name = HU.jqname(ID_CT_TYPENAME).val();	
	let basicLabel = 'Basic Configuration'
	if(Utils.stringDefined(name)) basicLabel +=" - " + name;	
	else if(Utils.stringDefined(type)) basicLabel +=" - " + type;
	label.html(basicLabel);
    },
    canSaveInput:function(name) {
	return name!='droptable' &&
	    name!='install' &&
	    name!='extrapassword' &&
	    name != 'json_contents' &&
	    name!='entryid';
    },
    handleRowAction(c,action) {
	if(action=='clear') {
	    this.setColumn(c,null);
	    return;
	}

	if(action=='delete') {
	    let rows = prompt("# rows to delete:",'1');
	    if(!rows) return;
	    for(let i=0;i<rows;i++) {
		for(let row=c.index;row<COLUMNS_MAX_ROWS;row++) {
		    this.setColumn(this.columns[row],this.columns[row+1]);
		}
	    }
	    return
	} 
	if(action=='insert-below') {
	    c = this.columns[c.index+1];
	    if(!c) return;
	    let rows = prompt("# rows to insert below:",'1');
	    if(!rows) return;
	    this.insertRows(c,rows);
	    return;
	}
	if(action=='insert-above') {
	    let rows = prompt("# rows to insert above:",'1');
	    if(!rows) return;
	    this.insertRows(c,rows);
	    return;
	}
    },
    setColumn:function (to,from){
	    if(!to) return;
//	    console.log('set ' +to.index+'='+from.index +' ' + to['name'].val() +' replaced by ' + from['name'].val());
	    ['name','label','type','extra'].forEach(attr=>{
		to[attr].val(from==null?'':from[attr].val());
	    });
    },
    insertRows(column,rows) {
	let idx = column.index;
	rows =  Math.abs(+rows);
	if(isNaN(rows)) {
	    alert("Insert rows must be positive or negative integer");
	    return
	}
	for(let row=this.columns.length-1;row>idx;row--) {
	    let c =this.columns[row];
	    this.setColumn(c,this.columns[row-rows]);
	}
	for(let d=0;d<rows;d++) {
	    let c =this.columns[idx+d];
	    this.setColumn(c,null);
	}
	
    },

    handleBulkUpload:function(v) {
	if(!v) return;
	let above = HU.isChecked(jqid('insertabove'));
	let idx=0;
	let theIdx=0;
	for(;true;idx++) {
	    let input =HU.jqname('column_name_' + idx);
	    if(!Utils.stringDefined(input.val())) {
		theIdx = idx;
		break;
	    }
	}
	v =v.replace(/\\\n/, " ");
	let fields  =
	    [ID_CT_CATEGORY,
	     ID_CT_SUPERCATEGORY,		  
	     ID_CT_ICON,
	     ['handler',ID_CT_HANDLER_EXTRA],		  
	     ['type',ID_CT_TYPEID],
	     ['super',ID_CT_SUPER],
	     ['name',ID_CT_TYPENAME]];
	

	let force = HU.isChecked(jqid('bulk_force'));
	let props = [[],[]];
	let propIdx =-1;

	let lines = Utils.split(v,'\n',true,true).filter(line=>{
	    let ok = true;
	    if(line=='<attributes>') {
		propIdx=0;
		return;
	    } else if(line=='</attributes>') {
		propIdx=-1;
		return;
	    } else   if(line=='<properties>') {
		propIdx=1;
		return;
	    } else if(line=='</properties>') {
		propIdx=-1;
		return;
	    }	    
	    if(propIdx>=0) {
		props[propIdx].push(line);
		return;
	    }
	    
	    fields.forEach(tuple=>{
		let prefix =(Array.isArray(tuple)?tuple[0]:tuple).trim();
		if(line.startsWith(prefix+':')) {
		    let name =(Array.isArray(tuple)?(tuple.length==1?tuple[0]:tuple[1]):tuple).trim();		
		    let nameField  = jqname(name);
//		    console.log('line:'+ line +' field name:' + name +' ' + nameField.length);
		    if(nameField.length==0) {
			alert('field not found:' + name);
			return false;
		    }
		    ok= false;
		    if(force || !Utils.stringDefined(nameField.val())) {
			nameField.val(line.substring((prefix+':').length).trim());
		    }
		}
	    });
	    if(!ok) return false;
	    console.log('line:'+ line);
	    return !line.startsWith('#') && line.length>0;
	});
	if(props[0].length) {
	    let textArea =    jqname('extraattributes');
	    if(force || !Utils.stringDefined(textArea.val())) {
		textArea.val(Utils.join(props[0],'\n'));
	    }
	}
	if(props[1].length) {
	    let textArea =    jqname('properties');
	    if(force || !Utils.stringDefined(textArea.val())) {
		textArea.val(Utils.join(props[1],'\n'));
	    }
	}	    

	if(above) {
	    this.insertRows(this.currentColumn,lines.length);
	    theIdx = this.currentColumn.index;
	}


	lines.forEach(line=>{
	    line = line.replace(/\\,/g,'_comma_');
	    line = line.replace(/\\n/g,'\n');	    
	    line = line.replace(/^\s+/gm,'');
	    let toks = Utils.split(line,',',true);
	    let id = Utils.makeID(toks[0]);
	    let label = toks[1];
	    let type = toks[2];	    
	    let extra = toks[3];
	    if(extra)
		extra = extra.replace(/_comma_/g,',');
	    if(!Utils.stringDefined(id)) id = Utils.makeID(label);
	    if(!Utils.stringDefined(label)) label = Utils.makeLabel(id);
	    HU.jqname('column_name_' + theIdx).val(id);
	    if(label)
		HU.jqname('column_label_' + theIdx).val(label);
	    if(type)
		HU.jqname('column_type_' + theIdx).val(type);
	    if(toks[3]) 
		HU.jqname('column_extra_' + theIdx).val(toks[3]);
	    theIdx++;
	});
    }
}

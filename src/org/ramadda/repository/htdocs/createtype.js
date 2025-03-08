var COLUMNS_MAX_ROWS = 100;
var CreateType  = {
    init:function(formId,entryId,json) {
	let _this = this;
	let storageKey=entryId+'_createtype';
//	let formData = json ?? Utils.getLocalStorage(storageKey,true);
	let formData = json ?? null;
	let form = jqid(formId);
	jqid('colbuttons').append(HU.span([ATTR_ID,'clearcols'],'Clear'));
	jqid('colbuttons').append(HU.span([],SPACE1));
	jqid('colbuttons').append(HU.span([ATTR_ID,'bulkupload'],'Bulk Upload'));

	let extras = $('.typecreate-column-extra');
	extras.each(function() {
	    _this.checkColumnExtra($(this));
	});
	extras.on('input',function() {
	    _this.checkColumnExtra($(this));
	});


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
		    c.name.css('background','#fff');
		});
		$(this).css('background','var(--color-mellow-yellow)');
	    });
	});

	let  popup =function(event,widget,c) {
	    event.preventDefault();
	    let html = '';
	    html+=HU.div([ATTR_CLASS,'ramadda-menu-item ramadda-clickable','data-action','insert-above'],'Insert rows above');
	    html+=HU.div([ATTR_CLASS,'ramadda-menu-item ramadda-clickable','data-action','insert-below'],'Insert rows below');
	    html+=HU.div([ATTR_CLASS,'ramadda-menu-item','data-action','clear'],'Clear row');		
	    html+=HU.div([ATTR_CLASS,'ramadda-menu-item','data-action','delete'],'Delete rows');		
	    let dialog =  HU.makeDialog({anchor:widget,
					 at:'left bottom',
					 my:'left top',
					 content:html});
	    dialog.find('.ramadda-menu-item').click(function() {
		let action = $(this).attr('data-action');
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
	jqid("clearcols").button().click(function(){
	    if (!confirm("Are you sure you want to clear the columns?")) {
		return;
	    }
	    for(let i=0;i<COLUMNS_MAX_ROWS;i++) {
		let input =HU.jqname('column_name_' + i);
		if(!input.length) break;
		input.val('');
		HU.jqname('column_label_' + i).val('');
		HU.jqname('column_type_' + i).val('');
		HU.jqname('column_extra_' + i).val('');						
	    }
	});
	jqid("bulkupload").button().click(function(){
	    let html = 'Enter columns, one per line.';
	    if(_this.currentColumn) {
		html+=SPACE2+HU.checkbox('insertabove',[ATTR_ID,'insertabove'],false,'Insert above selected row' +' ' + _this.currentColumn.name.val());
	    }

	    html+='<br>';
	    let buttonList = [HU.div(['action','ok',ATTR_CLASS,'ramadda-button ' + CLASS_CLICKABLE],
				     "Load"),
			      HU.div(['action','cancel',ATTR_CLASS,'ramadda-button ' + CLASS_CLICKABLE],"Cancel")]

	    let buttons = HU.buttons(buttonList);
	    html+=HU.textarea('bulktext','#name,label,type,extra\n',[ATTR_ID,'bulktext','rows',10,'cols',60]);
	    html+=buttons;
	    html = HU.div([ATTR_STYLE,HU.css('min-width','600px'),
			   ATTR_CLASS,'ramadda-license-dialog'], html);
	    let dialog =  HU.makeDialog({anchor:$(this),
					 at:'left+100 top+100',
					 my:'left top',
					 content:html,
					 title:'Bulk upload',
					 header:true,
					 draggable:true});
	    dialog.find('.ramadda-button').button().click(function() {
		if($(this).attr('action')=='ok') {
		    _this.handleBulkUpload(jqid('bulktext').val());

		}
		dialog.remove();
	    });
	});
	if(formData) {
//	    Utils.makeDownloadFile('data.json',JSON.stringify(formData));
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
	    };
	    this.initLabel();
	}
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
	let  type = HU.jqname('typeid').val();
	let  name = HU.jqname('typename').val();	
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
	let above = jqid('insertabove').is(':checked');
	let idx=0;
	let theIdx=0;
	for(;true;idx++) {
	    let input =HU.jqname('column_name_' + idx);
	    if(!Utils.stringDefined(input.val())) {
		theIdx = idx;
		break;
	    }
	}
	let lines = 	Utils.split(v,'\n',true,true).filter(line=>{
	    return !line.startsWith('#');
	});
	if(above) {
	    this.insertRows(this.currentColumn,lines.length);
	    theIdx = this.currentColumn.index;
	}


	lines.forEach(line=>{
	    line = line.replace(/\\,/g,'_comma_');
	    let toks = Utils.split(line,',',true);
	    let id = Utils.makeID(toks[0]);
	    let label = toks[1];
	    let type = toks[2];	    
	    let extra = toks[3];
	    if(extra)
		extra = extra.replace(/_comma_/g,',');

	    if(!Utils.stringDefined(id)) id = label;
	    if(!Utils.stringDefined(label)) label = Utils.makeLabel(id);
	    id = Utils.makeID(label);
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

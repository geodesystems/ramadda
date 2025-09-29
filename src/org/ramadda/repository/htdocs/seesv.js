var Seesv = {};

function  SeesvForm(inputId, entry,params) {
    this.params = params||{};
    const ATTR_CATEGORY="category";
    const ATTR_COMMAND = "command";
    const ATTR_FIELD="field";
    const ATTR_VALUE="value";
    const ATTR_INPUTID="inputid";
    const ATTR_COLUMNID="columnid";    

    const ICON_HELP = 'fas fa-question-circle';
    const ICON_SETTINGS = 'fas fa-gear';    
    const ICON_SEARCH = 'fas fa-binoculars';			     

    const ID_MAXROWS="maxrows";
    const ID_SETTINGS  = "settings";
    const ID_ALL  = "all";
    const ID_SELECTFILE  = "selectfile";    
    const ID_HELP  = "help";    
    const ID_MENU = "menu";
    const ID_INPUT = "input";
    const ID_OUTPUTS = "outputs";
    const ID_DO_COMMANDS  = "docommands";
    const ID_SAVE="save";
    const ID_APPLY_TO_SIBLINGS = "ID_APPLY_TO_SIBLINGS";
    const ID_TABLE = "table";
    const ID_RESIZE='resize';
    const ID_RECORD = "record";    
    const ID_PROCESS = "process";
    const ID_CLEAR = "clear";
    const ID_LIST = "list";
    const ID_TRASH = "trash";
    const ID_OUTPUT = "output";
    const ID_SCRATCH = "scratch";
    const ID_POPUP = "popup";
    const ID_ADDCOMMAND = "addcommand";
    const ID_CANCELCOMMAND = "cancelcommand";
    const ID_PRE = "csvpre";

    if(params.canEdit) {
	$(window).bind('beforeunload', (e)=>{
	    return this.checkChanged(e);
        });
    }

    $.extend(this,{
	entry:entry,
	editor:null,
	canEdit: params.canEdit,
	save: params.canEdit,
	inputId: inputId||"convertcsv_input",
	baseId: inputId||"convertcsv_input",
	applyToSiblings:false,
	allColumnIds:[],

	doCommands:true,
	commands:null,
	commandsMap:null,
	header:null,
	maxRows:this.params.rows||30,
	dbPopupTime:null});
    

    $.extend(this,{
	domId: function(id) {
	    return this.baseId+"_"+id;
	},
	jq: function(id) {
	    return $("#"+this.domId(id));
	},
	checkChanged: function(e) {
	    e = e || window.event;
	    let currentInput = this.getInput();
	    //	    console.log('current',currentInput);
	    //	    console.log('last',this.lastSavedInput);
	    //	    console.log("eq:" + Utils.stringEquals(currentInput,this.lastSavedInput));
	    if(!Utils.stringEquals(currentInput,this.lastSavedInput)) {
		let msg  =  'Changes have been made. Are you sure you want to leave?';
		if(e) {
		    e.preventDefault();
		    e.returnValue = msg;
		}
		return msg;
	    }
	    return;
	},
	addColumnId: function(id) {
	    if(!this.allColumnIds.includes(id))
		this.allColumnIds.push(id);
	    if(!this.columnIds.includes(id))
		this.columnIds.push(id);
	},
	init: function(){
	    this.allColumns = [];

	    let _this  =this;
	    let text = $('#' + this.baseId +'_lastinput').html();
	    if(text!=null) {
		text = text.replace(/_escnl_/g,'\n').replace(/_escquote_/g,'&quot;').replace(/_escslash_/g,'\\').replace(/\"/g,'&quot;').replace(/_esclt_/g,'&lt;').replace(/_escgt_/g,'&gt;');
	    }  else {
		text = this.getInput(true)||'';
	    }

	    let css = '';
	    css+='.seesv-table-cell {white-space:nowrap;overflow-x:auto;padding-left:2px;padding-right:2px;border-left:1px solid #ccc;}';
	    css += '.convert_button {margin-right:4px;margin-bottom:2px;margin-top:2px;padding:4px;padding-left:8px;padding-right:8px;}\n.ramadda-csv-table  {font-size:10pt;}\n ';
	    css += '.convert_add {margin-left:10px; cursor:pointer;}\n';
	    css += '.convert_add:hover {text-decoration:underline;}\n';
	    css += '.seesv-record-header {background:var(--header-background);padding:4px;padding-left:8px;}\n';
	    css += '.seesv-record-id {color:rgb(0,0,255);padding-right:8px;}\n';
	    css += '.seesv-table-summary {background:#F4F4F4;border-bottom:1px solid #000;}\n';
	    css += '.seesv-table-summary-cell {border-left:1px solid #ccc;}\n';	    
	    css += '.ace_gutter-cell {cursor:pointer;}\n';
	    css += '.ace_editor {margin-bottom:5px;xheight:200px;}\n';
	    css += '.ace_editor_disabled {background:rgb(250,250,250);}\n';
	    css += '.ace_csv_comment {color:#B7410E;}\n';
	    css += '.ace_csv_quote {color:green;}\n';
	    css += '.ace_csv_command {font-weight:bold;color:blue;}\n';
	    css += '.ramadda-seesv .ace_gutter-cell:hover {background:#ccc;}\n';	    

	    let html =  HU.cssTag(css);
	    if(this.params.extraTopLeft||this.params.extraTopRight) {
		html+=HU.leftRight(this.params.extraTopLeft ||'',this.params.extraTopRight ||'');
	    }
	    
	    let makeToolbarLink = (v,style) =>{
		return  HU.span([ATTR_TITLE,v[0],ATTR_ID,this.domId(v[1]),
				 ATTR_STYLE,style??HU.css(CSS_MARGIN_RIGHT,HU.px(10)),
				 ATTR_CLASS,'ramadda-clickable ramadda-highlightable'],
				HU.getIconImage(v[2]));
	    }

	    let searchAll  = makeToolbarLink(['View &amp; search all commands', ID_ALL,ICON_SEARCH],'');
	    let topLeft = SPACE1+searchAll+ HU.div([ATTR_ID,this.domId(ID_MENU),
						    ATTR_STYLE,HU.css(CSS_DISPLAY,DISPLAY_INLINE_BLOCK)]);
	    let topRight = [['Select file entry', ID_SELECTFILE,'fas fa-file-arrow-up'],
			    ['Settings',ID_SETTINGS,ICON_SETTINGS],
			    ['Help',ID_HELP,ICON_HELP]].reduce((current,v)=>{
				return current +makeToolbarLink(v);
			    },'');
	    html += HU.div([ATTR_CLASS,'ramadda-menubar',
			    ATTR_STYLE,HU.css(CSS_WIDTH,HU.perc(100))],
			   HU.leftRightTable(topLeft,topRight));

	    let input = HU.div([ATTR_STYLE,
				HU.css(CSS_HEIGHT,HU.perc(100),
				       CSS_TOP,HU.px(0),
				       CSS_RIGHT,HU.px(0),
				       CSS_LEFT,HU.px(0),
				       CSS_BOTTOM,HU.px(0),
				       CSS_POSITION,POSITION_ABSOLUTE,
				       CSS_WIDTH,HU.perc(100)),
				ATTR_ID,this.domId(ID_INPUT), ATTR_ROWS, 5], text);	    


	    let height=Utils.getDefined(HU.getUrlArgument('seesv_editor_height'),
					localStorage.getItem('seesv_editor_height-' + this.entry),
					this.params.height,
					HU.px(200));


	    html+=HU.div([ATTR_ID,this.domId(ID_RESIZE),
			  ATTR_STYLE,HU.css(CSS_MARGIN_BOTTOM,HU.px(5),
					    CSS_HEIGHT, HU.px(height),
					    CSS_POSITION,POSITION_RELATIVE)],input);


	    let left ='';
	    left += HU.span([ATTR_ID,this.domId(ID_TABLE),
			     ATTR_CLASS,'convert_button',
			     ATTR_TITLE,'Display table (ctrl-t)'],
			    HU.getIconImage('fas fa-table')+SPACE+'View Table')+' ';
	    left += HU.span([ATTR_ID,this.domId(ID_RECORD),
			     ATTR_CLASS,'convert_button',
			     ATTR_TITLE,'Display table (ctrl-t)'],
			    HU.getIconImage('fas fa-print')+SPACE+'View Records')+' ';	    
	    left += HU.span([ATTR_ID,this.domId(ID_OUTPUTS),
			     ATTR_CLASS,'convert_button'], 
			    HU.getIconImage('fas fa-chevron-down') + SPACE +
			    'Outputs') +' ';

	    left += HU.span([ATTR_ID,this.domId(ID_PROCESS),
			     ATTR_CLASS,'convert_button',
			     ATTR_TITLE,'Process entire file'],
			    HU.getIconImage('fas fa-cogs') + SPACE +
			    'Process')+' ';	    



	    let right = '';
	    right += HU.span([ATTR_ID,this.domId(ID_LIST),
			      ATTR_CLASS,CLASS_CLICKABLE,
			      ATTR_TITLE,'List temp files'],
			     HU.getIconImage('fa-list')) +SPACE2;
	    right += HU.span([ATTR_ID,this.domId(ID_CLEAR),
			      ATTR_CLASS,CLASS_CLICKABLE,
			      ATTR_TITLE,'Clear output'],
			     HU.getIconImage('fa-eraser')) +SPACE2;
	    right += HU.span([ATTR_ID,this.domId(ID_TRASH),
			      ATTR_CLASS,CLASS_CLICKABLE,
			      ATTR_TITLE,'Remove temp files'],
			     HU.getIconImage('fa-trash')) +SPACE2;	    	    
	    left +=SPACE + (this.params.extraButtons||'');


	    html += HU.leftRightTable(left,right);
	    html +=  HU.div([ATTR_ID, this.domId(ID_OUTPUT),
			     ATTR_STYLE,HU.css(CSS_MARGIN_TOP,HU.px(5),
					       CSS_MAX_HEIGHT,HU.px(500),
					       CSS_OVERFLOW_Y,OVERFLOW_AUTO)]);
	    html += HU.div([ATTR_ID, this.domId(ID_SCRATCH)],'');
	    jqid(this.inputId).addClass('ramadda-seesv');
	    jqid(this.inputId).html(html);

	    this.jq(ID_RESIZE).resizable({
		handles: 's',
		stop: (event, ui) =>{
		    let height = this.jq(ID_RESIZE).height();
		    localStorage.setItem('seesv_editor_height-' + this.entry,height);
		    HU.addToDocumentUrl('seesv_editor_height',height);
		    $(this).css(CSS_WIDTH, '');
		    this.editor.resize();
		},
	    });
	    this.makeEditor();

	    this.jq(ID_TRASH).click(()=>{
		this.call('',{clearOutput:true});
	    });
	    this.jq(ID_LIST).click(()=>{
		this.call('',{listOutput:true});
	    });

	    this.jq(ID_CLEAR).click(()=>{
		this.output('');
	    });

	    this.jq(ID_TABLE).button().click(()=>{
		this.display('-table',null,true);
	    });
	    this.jq(ID_RECORD).button().click(()=>{
		this.display('-record',null,true);
	    });	    
	    this.jq(ID_PROCESS).button().click(()=>{
		this.display('',true);
	    });				   


	    this.jq(ID_OUTPUTS).button().click(function(){
		let html = _this.outputCommands.reduce((acc,cmd)=>{
		    if(cmd.command=='-table') return acc;
		    if(cmd.command=='-torecord') return acc;
		    if(cmd.command=='-stats') return acc;		    
		    acc+=HU.div([ATTR_CLASS,CLASS_CLICKABLE,
				 ATTR_COMMAND,cmd.command,
				 ATTR_TITLE,cmd.command],cmd.description);
		    return acc;
		},'');
		html = HU.div([ATTR_STYLE,HU.css(CSS_MARGIN,HU.px(10))],html);
		let dialog = HU.makeDialog({content:html,anchor:$(this)});
		dialog.find('.ramadda-clickable').click(function(){
		    let command = $(this).attr(ATTR_COMMAND);
		    dialog.remove();
		    _this.display(command);

		});
	    });

	    this.jq(ID_HELP).click((e)=>{
		this.call('-help');
	    });


	    this.jq(ID_SELECTFILE).click(function(event){
		RamaddaUtils.selectInitialClick(event,'convertcsv_file1',_this.domId('input'),'true','entry:entryid',''+_this.entry,null,null,
						{anchor:$(this),locationMy:'top right',locationAt:'bottom right-50',minWidth:'300px'});
	    });
	    this.jq(ID_ALL).click(function(e){
		_this.showMenu(_this.allMenuItems,$(this),'All Commands',true);
	    });
	    this.jq(ID_SETTINGS).click(function(e){
		let html ='';
		html += 'Rows: ' + HU.input('',_this.maxRows,[ATTR_SIZE,2,
							      ATTR_ID, _this.domId(ID_MAXROWS)]) +HU.br();
		if(this.canEdit) {
		    html +=  HU.checkbox('',[ATTR_TITLE,'Save the commands every time the table is displayed',
					     ATTR_ID,_this.domId(ID_SAVE)],_this.save,'Auto save') +HU.br();
		}
		html += HU.checkbox('',[ATTR_TITLE,'Enabled/Disable the commands',
					ATTR_ID,_this.domId(ID_DO_COMMANDS)],_this.doCommands,'Do commands') +HU.br();
		html += HU.checkbox('',[ATTR_TITLE,'Apply this set of commands to all of the siblings of this entry',
					ATTR_ID,_this.domId(ID_APPLY_TO_SIBLINGS)], _this.applyToSiblings, 'Apply to siblings');
		html+=HU.div([ATTR_CLASS,CLASS_CLICKABLE],
			     HU.href(RamaddaUtils.getUrl('/userguide/seesv.html'),
				     'Main Help',[ATTR_TARGET,'_help']));

		html = HU.div([ATTR_STYLE,HU.css(CSS_MARGIN,HU.px(10))],html);

		let dialog = HU.makeDialog({content:html,my:'right top',at:'right bottom',
					    anchor:$(this),draggable:true,header:true,inPlace:false});
		HU.onReturnEvent('#' + _this.domId(ID_MAXROWS),input=>{
	 	    _this.maxRows = input.val();
		    dialog.remove();
		});

		_this.jq(ID_SAVE).click(function() {
		    _this.save =  $(this).is(':checked');
		});
		_this.jq(ID_APPLY_TO_SIBLINGS).click(function() {
		    _this.applyToSiblings =  $(this).is(':checked');
		});
		_this.jq(ID_DO_COMMANDS).click(function() {
		    _this.doCommands =  $(this).is(':checked');
		    if(_this.editor) {
			if(_this.doCommands) {
			    _this.editor.container.classList.remove('ace_editor_disabled')
			} else {
			    _this.editor.container.classList.add('ace_editor_disabled');
			}
		    }
		});
	    });


	    $(document).click((e)=> {
		if(this.dbPopupTime) {
		    let now = new Date();
		    let timeDiff = now-this.dbPopupTime.getTime();
		    if(timeDiff<1000)  {
			return;
		    }
		}
		let popup = this.jq(ID_POPUP);
		popup.css(CSS_DISPLAY,DISPLAY_NONE);
	    });



	    let helpUrl = this.getUrl('-helpjson');
	    let jqxhr = $.getJSON( helpUrl, (data) =>{
		if(data.error!=null) {
		    return;
		}
		if(Utils.isDefined(data.result)) {
		    let result = window.atob(data.result);
		    result= JSON.parse(result);
		    this.commands = result.commands;
		    this.commandsMap = {}
		    let menus = [];
		    let menuCategories = {};
		    let menuItems =[];
		    let category='';
		    this.allMenuItems = [];
		    this.outputCommands = [];
		    this.commands.forEach(cmd=>{
			let command = cmd.command;
			if(cmd.isCategory) {
			    category = cmd.label;
			    menuCategories[category] = menuItems = [];
			    menus.push(HU.div([ATTR_CLASS,'ramadda-highlightable ramadda-menubar-button',ATTR_CATEGORY,category], category));

			    return;
			}
			if(!command || !command.startsWith('-') || command.startsWith('-help')) {
			    return;
			}
			if(category=='Output') {
			    let skip = ['-fields','-tourl','-args','-pointheader','-tojson'];

			    if(cmd.args.length==0 && !skip.includes(cmd.command)) {
				//				console.log(cmd.command);
				this.outputCommands.push(cmd);
				//				return;
			    }
			}
			let desc = cmd.description;
			cmd.args.forEach(a=>{
			    let desc = a.desc;
			    if(desc!='') desc = ' '  + desc;
			});
			let corpus = cmd.label +' ' + cmd.command.replace(/-/g,' ') +' ' + cmd.description;
			corpus = corpus.replace(/[\n\"\']/g,' ');
			let label = cmd.label ||  Utils.camelCase(cmd.command.replace('-',''));
			this.commandsMap[command] = cmd;
			let menuItem = HU.div(['data-corpus',corpus,
					       ATTR_TITLE,(desc||'')+HU.br()+cmd.command,
					       ATTR_STYLE,HU.css(CSS_MARGIN,HU.px(1),CSS_BORDER,HU.border(1,'transparent')),
					       ATTR_CLASS, 'ramadda-hoverable ramadda-clickable',
					       ATTR_COMMAND,command],label);
			menuItems.push(menuItem);
			this.allMenuItems.push(menuItem);
		    });
		    let menuBar=menus.join('');
		    this.jq('menu').html(HU.div([],menuBar));
		    this.jq('menu').find('.ramadda-menubar-button').click(function() {
			let cat = $(this).attr(ATTR_CATEGORY);
			if(!cat || !menuCategories[cat]) {
			    RamaddaUtils.selectInitialClick(event,'convertcsv_file1',_this.domId('input'),'true','entry:entryid','" + this.entry+"');
			    return;
			}
			let title = $(this).attr(ATTR_CATEGORY);
			let items = Utils.splitList(menuCategories[cat],12).map(list=>{
			    return list.join('');
			});
			_this.showMenu(items,$(this),title);
		    });
		}
	    }).fail(function(jqxhr, textStatus, error) {
	    });
	},
	showMenu: function(items,anchor,title,all,args) {
	    let _this = this;
	    let menuId = HU.getUniqueId('menu_');
	    let menu;
	    if(all) menu =  Utils.wrap(items,
				       HU.open(TAG_DIV,[ATTR_STYLE,HU.css(CSS_BORDER,HU.border(1,'#ececec'),
									  CSS_VERTICAL_ALIGN,ALIGN_TOP,
									  CSS_MARGIN,HU.px(2),
									  CSS_DISPLAY,DISPLAY_INLINE_BLOCK)]),HU.close(TAG_DIV));
	    else menu = Utils.wrap(items,HU.open(TAG_DIV,[ATTR_STYLE,HU.css(CSS_VERTICAL_ALIGN,ALIGN_TOP,
									    CSS_MARGIN,HU.px(5),
									    CSS_DISPLAY,DISPLAY_INLINE_BLOCK)]),HU.close(TAG_DIV));
	    let menuAttrs = [ATTR_ID,menuId];
	    if(all)  menuAttrs.push(ATTR_STYLE,HU.css(CSS_MARGIN,HU.px(10),
						      CSS_OVERFLOW_Y,OVERFLOW_AUTO,
						      CSS_MAX_HEIGHT,HU.px(300),
						      CSS_MIN_HEIGHT,HU.px(200),
						      CSS_MAX_WIDTH,HU.px(800),
						      CSS_MIN_WIDTH,HU.px(600)));
	    menu = HU.div(menuAttrs,menu);
	    let inputId = HU.getUniqueId('input_');
	    let input = HU.div([ATTR_STYLE,HU.css(CSS_FONT_SIZE,HU.perc(80),
						  CSS_TEXT_ALIGN,ALIGN_CENTER,
						  CSS_MARGIN,HU.px(5))],
			       HU.input('','',[ATTR_AUTOFOCUS,null,
					       ATTR_STYLE,HU.css(CSS_WIDTH,HU.px(150)),
					       ATTR_PLACEHOLDER,'Search Commands',
					       ATTR_ID,inputId]));
	    menu = input+menu;
	    
	    if(_this.menuDialog) {
		_this.menuDialog.remove();
	    }
	    args = $.extend({my:all?'right top':'left top',at:all?'right bottom':'left bottom',
			     content:menu,
			     anchor:anchor,
			     header:true,
			     draggable:true,
			     title:title},args??{});
	    let dialog = _this.menuDialog = HU.makeDialog(args);
	    let commands = dialog.find('.ramadda-clickable');
	    commands.tooltip({
		show:{delay:1000},
		content: function () {
		    return $(this).prop('title');
		}
	    });
	    jqid(menuId).css(CSS_MIN_WIDTH,jqid(menuId).width());
	    let toggle = (item,on,force)=>{
		if(force) {
		    item.css(CSS_BACKGROUND,'transparent').css(CSS_BORDER,HU.border(1,'transparent'));
		}
		if(on) {
		    if(all) {
			item.parent().show();
		    } else {
			if(!force)
			    item.css(CSS_BACKGROUND,'var(--color-mellow-yellow)').css(CSS_BORDER,'var(--highlight-border');
		    }
		} else {
		    if(all)
			item.parent().hide();
		    else
			item.css(CSS_BACKGROUND,'transparent').css(CSS_BORDER,HU.border(1,'transparent'));
		}

	    };
	    jqid(inputId).focus();
	    jqid(inputId).keyup(function(event) {
		let text = $(this).val().trim().toLowerCase();
		commands.each(function() {
		    if(text=='') {
			toggle($(this),true,true);
		    } else {
			let corpus = $(this).attr('data-corpus');
			if(!corpus) return;
			corpus =  corpus.toLowerCase();
			toggle($(this),corpus.indexOf(text)>=0);
		    }
		});
	    });

	    commands.click(function(event) {
		let cmd = _this.commandsMap[$(this).attr(ATTR_COMMAND)];
		dialog.remove();
		if(!cmd) {
		    return;
		}
		_this.addCommand(cmd,null,anchor);
	    });
	},
	output: function(html) {
	    this.jq('output').html(html);
	},
	insertTags(tagOpen, tagClose, sampleText) {
	    this.insertText(tagOpen);
	},
	makeEditor:  function() {
	    let _this = this;
	    this.editor = ace.edit(this.domId(ID_INPUT));
	    WikiUtil.addWikiEditor(this,this.domId(ID_INPUT));
	    this.editor.setBehavioursEnabled(true);
	    this.editor.setDisplayIndentGuides(false);
	    this.editor.setKeyboardHandler('emacs');
	    this.editor.setShowPrintMargin(false);
	    this.editor.getSession().setUseWrapMode(true);
	    let options = {
		autoScrollEditorIntoView: true,
		copyWithEmptySelection: true,
	    };
	    this.editor.setOptions(options);
	    this.editor.session.setMode('ace/mode/csvconvert');
	    this.editor.on('guttermousedown', (e)=> {
		if(e.domEvent.ctrlKey) {
		    let row = +e.getDocumentPosition().row+1;
		    let lines =this.editor.getValue().split('\n');
		    let text = '';
		    for(let i=0;i<row;i++) {
			text+=lines[i]+'\n';
		    }
		    this.display('-table',null,true,text);
		    this.gutterMouseDown = true;
		}
	    });
	    this.editor.commands.addCommand({
		name: 'keyt',
		exec: function() {
		    _this.display('-table',null,true);
		},
		bindKey: {mac: 'ctrl-t', win: 'ctrl-t'}
	    })
	    this.editor.commands.addCommand({
		name: 'keyf',
		exec: function() {
		    _this.showMenu(_this.allMenuItems,_this.jq(ID_MENU) ,'All Commands',true);
		},
		bindKey: {mac: 'ctrl-f', win: 'ctrl-f'}
	    })


	    this.editor.commands.addCommand({
		name: 'keyh',
		exec: function() {
		    _this.display('-printheader',null,true);
		},
		bindKey: {mac: 'ctrl-h', win: 'ctrl-h'}
	    })

	    this.editor.container.addEventListener('mouseup', (e) =>{
		if(!e.metaKey)  return;
		this.handleMouseUp(e);
	    });

	    this.editor.container.addEventListener('contextmenu',(e)=> {
		if(e.ctrlKey) {
		    e.preventDefault();
		    return;
		}
		this.handleMouseUp(e,null,true);
	    });

	    addHandler({
		selectClick: function(selecttype, id, entryId, value) {
		    _this.insertText('entry:' + entryId);
		}
	    },this.inputId);
	    $('.ace_gutter').attr(ATTR_TITLE,'control-click to evaluate to here');
	},


	handleMouseUp(e,result) {
	    e.preventDefault();
	    let cursor = this.editor.getCursorPosition();
	    let index = this.editor.session.doc.positionToIndex(cursor);
	    let text = this.editor.getValue();
	    let menu = '';
	    let tmp = index;
	    let left = -1;
	    let right = -1;
	    let lastWasChar=false;
	    let lastWasHash=false;
	    while(tmp>=0) {
		let c = text[tmp];
		if (c == '-') {
		    let prev = text[tmp-1];
		    let ok = true;
		    if(prev && !prev.match(/\s/)) {
			ok = false;
		    }
		    if(ok && lastWasChar) {
			left = tmp;
			break;
		    }
		}
		if(c=='\n' && lastWasHash) {
		    break;
		}
		lastWasChar = String(c).match(/[a-zA-Z]/);
		//	    if(c==' ' || c=='\n' || c =='\'' || c=='{' || c=='}') break;
		tmp--;
		lastWasHash = (c=='#') ;
	    }

	    if(left>=0) {
		tmp = left;
		while(tmp<text.length) {
		    right  = tmp;
		    let c = text[tmp];
		    if (c == ' ' || c=='\n') {
			break;
		    }
		    tmp++;
		}
		if(right<0) {
		    return;
		}
		let substring = text.substring(left,right);
		let command = this.commandsMap[substring];
		if(!command) {
		    console.log('no command:' + substring);
		    return;
		}
		let values = [];
		let tok = null;
		let append = (c)=>{
		    if(tok == null) tok = c;
		    else tok+=c;
		}
		let end = (c)=>{
		    if(tok != null) values.push(tok);
		    tok = null;
		}
		let inQuote = false;
		let bracketCnt = 0;
		let inEscape = false;
		while(right<text.length && values.length<command.args.length) {
		    let c = text[right++];
		    if(c=='\\') {
			if(inEscape)
			    append(c);
			else
			    inEscape = true;
			continue;
		    }
		    //If we run into another command then break
		    if(c=='-' && !inQuote) {
			let prev = text[right-2];
			if(!prev || prev.match(/\s/)) {
			    right--;
			    break
			}
		    }			

		    if(inEscape) {
			append(c);
			inEscape = false;
			continue;
		    }

		    if(bracketCnt==0) {
			if(c=='{') {
			    if(inQuote) {
				append(c);
			    } else {
				bracketCnt++;
			    }
			    continue;
			}
			if(c=='\"') {
			    inQuote = !inQuote;
			    if(!inQuote) {
				if(tok==null) tok='';
				end();
			    }
			    continue;
			}
			if(!inQuote && (c==' ' || c=='\n')) {
			    end();
			} else {
			    append(c);
			}
			continue;
		    } else {
			if(c=='}') {
			    bracketCnt--;
			    if(bracketCnt==0) {
				bracketCnt=0;
				end();
			    } else if(bracketCnt<0) {
				bracketCnt=0;			    
			    } else {
				append(c);
			    }
			} else if(c=='{') {
			    append(c);
			    bracketCnt++;
			} else  {
			    append(c);
			}
		    }
		}		
		end();
		let c1 = this.editor.session.doc.indexToPosition(left);
		let c2 = this.editor.session.doc.indexToPosition(right);
		//Need to do this to prevent global conflicts
		let Range = ace.require('ace/range').Range;
		let r = new  Range(c1.row, c1.column, c2.row, c2.column);
		this.editor.selection.setRange(new Range(c1.row, c1.column, c2.row, c2.column));
		let callback = (values,args)=>{
		    if(!values) {
			this.editor.clearSelection();
			return;
		    }
		    text = text.substring(0,left) + command.command +' ' +args + text.substring(right);
		    let idx = left+command.command.length +1 + args.length;
		    this.editor.setValue(text);
		    this.editor.clearSelection();
		    let cursor = this.editor.session.doc.indexToPosition(idx);
		    this.editor.selection.moveTo(cursor.row, cursor.column);
		    this.editor.focus();
		    
		};
		this.addCommand(command, {add:false,values:values,callback:callback,
					  event:e});
	    }

	},
	insertCommand:function(cmds) {
	    if(!cmds) return;
	    cmds = cmds.replace(/_quote_/g,"\"");
	    cmds = ' ' + cmds +' ';
	    this.insertText(cmds);
	},
	insertColumnIndex:function(index,plain,id) {
	    if(!id) id = this.columnInput;
	    if(id && $('#' + id).length>0) {
		let v = $('#' + id).val()||'';
		v = v.trim();
		if(v!='') v+='\n';
		if(v!='' && !v.endsWith(',')) v +=plain?'':',';
		index = String(index).split(',').join('\n');
		v+=index+'\n';
		$('#' + id).val(v);	    
		return;
	    }
	    if(!plain) index = index+',';
	    this.insertText(index);
	},
	insertText:function(text) {
	    if (this.editor) {
		let cursor = this.editor.getCursorPosition();
		this.editor.insert(text);
		this.editor.focus();
		return;
	    }
	    let input = this.jq('input');
	    let start = input.prop('selectionStart');
	    let end = input.prop('selectionEnd');
	    input.val(input.val().substring(0, start)
		      +text 
		      + input.val().substring(end));
	    setTimeout(() =>{
		input.focus();},0);
	    input[0].selectionStart = start+text.length;
	    input[0].selectionEnd = start+text.length;
	},

	getInput:function(force) {
	    val= '';
	    if(this.doCommands || force) {
		if(this.editor)
		    val = this.editor.getValue();
		else
		    val =this.jq('input').val();
	    }
	    if(val == null) return '';
	    return val.trim();
	},
	display:function(what, process,html,command) {
	    //	    console.log('evaling:' + what);
	    if(!command) {
		command ='';
		this.lastSavedInput = this.getInput();
		lines = this.lastSavedInput.split('\n');
		for(let i=0;i<lines.length;i++){
		    line = lines[i].trim();
		    if(line =='') continue;
		    if(line.startsWith('#')) continue;
		    command +=line;
		    command +='\n';
		}
	    }

	    command = command.trim();
	    if(what!='-raw' && command.indexOf('-template ')>=0) what = '';

	    if(what == null) {
		this.call(command  +' ', {process:process,html:html});
	    }  else {
		if(what == '-raw') command = '';
		this.call(command, {process:process, csvoutput:what,html:html});
	    }
	},
	getUrl:function(cmds,rawInput) {
	    let input = '';
	    if(rawInput || rawInput==='') input = '&lastinput=' + encodeURIComponent(rawInput);
	    let url = ramaddaBaseUrl + '/entry/show?output=convert_process&entryid=' + this.entry+'&commands=' + encodeURIComponent(cmds) + input;
	    return url;
	},
	makeDbMenu:function(field,value,label) {
	    if(!label) {
		label = field.replace(/^.*?\./,'');
	    }
	    return HU.span([ATTR_CLASS,CLASS_CLICKABLE,ATTR_FIELD,field,ATTR_VALUE,value],label);
	},
	makeHeaderMenu: function(field,value,label) {
	    return HU.span([ATTR_FIELD,field,
			    ATTR_VALUE,value,
			    ATTR_CLASS,'ramadda-menuitem-link ramadda-clickable'],(label||field));
	},
	insertHeader:function(field,value) {
	    if(this.headerInput && $('#' + this.headerInput).length>0) {
		let v = $('#' + this.headerInput).val()||'';
		value = field +'  '+ value;
		v = v.trim();
		if(v.length>0)
		    v = v +'\n';
		v+=value;
		$('#' + this.headerInput).val(v);	    
		return;
	    }

	    let popup = this.jq(ID_POPUP);
	    popup.css(CSS_DISPLAY,DISPLAY_NONE);
	    if(!value) value = ' ';
	    value = ' ' + value +' ';
	    this.insertCommand(field +value);
	},
	insertDb:function(field,value) {
	    if(!value) value = ' ';
	    if(value!='true' && value!='false') {
		if(value.indexOf(' ')>=0) 
		    value = " \"" +value+"\"";
		else if(value!="") value = " " + value +" ";
	    } else {
		value = " " + value +" ";
	    }
	    if(this.dbInput && $("#" + this.dbInput).length>0) {
		let v = $("#" + this.dbInput).val()||"";
		v = v.trim();
		if(v.length>0)
		    v = v +"\n";
		v+=field+" " +value;
		$("#" + this.dbInput).val(v);	    
		return;
	    }
	    this.insertCommand(field +value);
	},
	call:function(cmds,args) {
	    let _this =this;
	    if (!args)  {
		args = {};
	    }
	    this.output(HU.tag(TAG_PRE,[],"Processing..."));
	    let cleanCmds = "";
	    let lines = cmds.split("\n");
	    let doExplode = false;
	    //Strip out the comments and anything after -quit
	    for(let i=0;i<lines.length;i++) {
		let line = lines[i];
		let tline = lines[i].trim();
		if(tline.startsWith("#")) continue;
		if(tline.startsWith("-quit")) break;
		if(tline.startsWith("-explode")) {
		    doExplode = true;
		}
		cleanCmds+=line+"\n";
	    }
	    cmds = cleanCmds;
	    //If its the -db command then force -print
	    let hasDb = cmds.match(/[^ \t]-db[ \t]+/);
	    if(hasDb && Utils.stringDefined(args.csvoutput)) {
		args.csvoutput = "-print";
	    }


	    let isTypeXml = cmds && cmds.indexOf("-typexml")>=0;
	    let isScript  = args.csvoutput=="-script";
	    let isArgs  = args.csvoutput=="-args";
	    let debug = cmds.match("-debug($| )");
	    let rawInput = this.getInput();
	    haveOutput = Utils.isDefined(args.csvoutput);
	    if(!doExplode &&  cmds.indexOf("-count")<0  && cmds.indexOf("-db") <0 && !haveOutput)  {
		args.csvoutput = "-print";
	    }

	    let isHelp = cmds.indexOf("-help")>=0;
	    let raw = args.csvoutput=="-raw";
	    let isDb = args.csvoutput == "-db" || cmds.indexOf("-db")>=0;
	    let isJson = args.csvoutput=="-tojson";
	    let isXml = args.csvoutput=="-toxml";	


	    let csv = args.csvoutput=="-print";
	    let stats = args.csvoutput=="-htmlstats";
	    let table =  args.csvoutput=="-table";					    
	    let isRecord = args.csvoutput=="-torecord" || args.csvoutput=="-record";					    				
	    let showHtml =false;
	    let printHeader = args.csvoutput == "-printheader";


	    if((!args.process && !doExplode && !isScript && !isArgs) || args.html) {
		if (this.maxRows != "") {
		    if(stats) {
		    }  else {
			cmds = "-maxrows " + this.maxRows +" " + cmds;
		    }
		}
	    }



	    let url = this.getUrl(cmds,rawInput);

	    let filename = "results.txt"
	    if(isScript) filename = "convert.sh";
	    else if(isArgs) filename = "convertargs.txt";
	    else if(isJson) filename = "results.json";
	    else if(isXml) filename = "results.xml";
	    else if(isTypeXml) filename = "mytypes.xml";				    


	    if(args.process)
		url += "&process=true";
	    if(this.save) {
		url += "&save=true";        
	    } else {
		url += "&save=false";        
	    }

	    if(args.csvoutput) {
		url += "&csvoutput=" + args.csvoutput;
	    }
	    if(args.clearOutput)
		url += "&clearoutput=true";
	    if(args.listOutput)
		url += "&listoutput=true";
	    if(this.applyToSiblings) 
		url += "&applysiblings=true";

	    let output = this.jq("output");
	    let result;
	    let writePre =contents=>{
		contents = HU.pre([ATTR_STYLE,HU.css(CSS_POSITION,DISPLAY_RELATIVE),
				   ATTR_ID,this.domId(ID_PRE)],  contents);
		output.html(contents);
		let msg = $(HU.div([ATTR_STYLE,HU.css(CSS_POSITION,POSITION_ABSOLUTE,
						      CSS_RIGHT,HU.px(48),
						      CSS_TOP,HU.px(5))], "")).appendTo(this.jq(ID_PRE));

		let copy = $(HU.div([ATTR_TITLE,"Copy to clipboard",
				     ATTR_CLASS,CLASS_CLICKABLE,
				     ATTR_STYLE,HU.css(CSS_POSITION,POSITION_ABSOLUTE,
						       CSS_RIGHT,HU.px(10),CSS_TOP,HU.px(5))],
				    HU.getIconImage("fas fa-clipboard"))).appendTo(this.jq(ID_PRE));
		copy.click(()=>{
		    Utils.copyToClipboard(result);
		    msg.html("OK, result is copied");
		    setTimeout(()=>{
			msg.fadeOut();
		    },2000);
		});
		if(filename) {
		    let file = $(HU.div([ATTR_TITLE,"Download file",
					 ATTR_CLASS,CLASS_CLICKABLE,
					 ATTR_STYLE,HU.css(CSS_POSITION,POSITION_ABSOLUTE,
							   CSS_RIGHT,HU.px(32),CSS_TOP,HU.px(5))],
					HU.getIconImage("fas fa-file-download"))).appendTo(this.jq(ID_PRE));
		    file.click(()=>{
			msg.html("");
			Utils.makeDownloadFile(filename,result);
		    });
		}
		
	    };
	    let jqxhr = $.getJSON( url, (data) =>{
		//		console.log(typeof jqxhr.getAllResponseHeaders());
		if(data.error!=null) {
		    this.output(HU.tag(TAG_PRE,[],"Error:" + window.atob(data.error)));
		    return;
		}
		if(data.message!=null) {
		    this.output(HU.tag(TAG_PRE,[],window.atob(data.message)));
		    return;
		}
		if(data.file) {
		    this.output(HU.tag(TAG_PRE,[],""));
		    iframe = HU.tag(TAG_IFRAME,[ATTR_SRC,data.file,ATTR_STYLE,HU.css(CSS_DISPLAY,DISPLAY_NONE)]);
		    this.jq("scratch").html(iframe);
		    return;
		} 

		if(Utils.isDefined(data.html) ) {
		    html = window.atob(data.html);
		    html = HU.tag(TAG_PRE,[],html);
		    output.html(html);
		    return;
		}

		if(Utils.isDefined(data.url)) {
		    this.output(data.url);
		    output.find(".ramadda-button").button();
		    return;
		} 




		if(Utils.isDefined(data.result)) {
		    try {
			result = window.atob(data.result).trim();
		    } catch(err) {
			console.log("Error decoding result:" + err);
		    }
		    
		    //Decode utf-8
		    result = decodeURIComponent(escape(result));
		    if(isScript) {
			//		    Utils.makeDownloadFile("script.sh",result);
			//		    return;
		    }

		    if(result.indexOf("<table")>0  || result.indexOf("<div")>0  || result.indexOf("<row")>0)
			showHtml = true;
		    if(debug) {
			result = result.replace(/</g,"&lt;").replace(/>/g,"&gt;");
			writePre(result);
		    } else if(isHelp) {
			let tmp = "";
			result = result.replace(/</g,"&lt;");
			result = result.replace(/>/,"&gt;");
			result.split("\n").map(line=>{
			    if(line.trim().startsWith("-")) {
				let idx= line.indexOf("(");
				let cmd = line;
				let comment = "";
				if(idx>0) {
				    cmd = line.substring(0,idx);
				    comment = "<i>" + line.substring(idx)+"</i>";
				}
				let shortCmd = cmd.trim();
				let idx2= shortCmd.indexOf(" ");
				if(idx2>0) {
				    shortCmd = shortCmd.substring(0,idx2);
				}
				let prefix = '';
				if(shortCmd.startsWith('-')) {
				    let helpUrl = RamaddaUtils.getUrl('/userguide/seesv.html#' + shortCmd);
				    
				    let help = HU.href(helpUrl,
						       HU.getIconImage('fas fa-question'),
						       [ATTR_TARGET,'_help',ATTR_TITLE,'Full help']);
				    prefix = help +' ';
				}
				line = "      " + prefix +"<a title='Insert command' href=# onclick=\"Seesv.insertCommand('" + cmd.trim() +"')\">" + cmd.trim() +"</a> " + comment;
			    }  else {
				line = "<b>" + line +"</b>"
			    }
			    tmp+=HU.div([ATTR_CLASS,'seesv-help-line'], line);
			});
			Seesv.insertCommand = (line)=>{
			    this.insertCommand(line);
			};
			result = tmp;
			let preId = HU.getUniqueId();
			let id2 = HU.getUniqueId();
			let html = printHeader?result:HU.span([ATTR_ID,id2])+
			    SPACE2+
			    'Command-click/right-click in commands area above to show command dialog'+
			    HU.tag(TAG_PRE,[ATTR_ID,preId], result);
			output.html(html);
			HU.initPageSearch('#' + preId +' .seesv-help-line',null,'Search help',null,{target:'#'+id2});
			return;
		    } else if(isDb) {
			let db = result.replace(/<tables>[ \n]/,"Database:");
			//			let doc = (new DOMParser()).parseFromString(result, "application/xml");
			//			let table = doc.getElementsByTagName('table')[0];
			db = db.replace(/<property[^>]+>/g,"");
			db = db.replace(/> *<\/column>/g,"/>");
			db = db.replace(/\n *\n/g,"\n");
			db = db.replace(/\/>/g,"");
			db = db.replace(/>/g,"");
			db = db.replace(/<table +id="(.*?)"/g,"\t<table <a class=csv_db_field field='table' onclick=noop()  title='Add to input'>$1</a>");
			db = db.replace("<table "," ");
			db = db.replace(/Database:[ \t]+/,"Database: ");
			db = db.replace("</table","");
			db = db.replace("</tables","");

			db = db.replace(/<column +name="(.*?)"/g,"\tcolumn: name=\"<a class=csv_db_field field='$1' onclick=noop() title='Add to input'>$1</a>\"");
			db = db.replace(/ ([^ ]+)="([^"]+)"/g,"\t$1:$2");
			db = db.replace(/ ([^ ]+)="([^"]*)"/g,"\t$1:\"$2\"");
			db = db.replace(/name:/g," ");
			db = db.replace(/column:[ \t]+/g,"column: ");			
			writePre(db);
			output.find(".csv_db_field").click(function(event) {
                            let space = SPACE2;
                            event.preventDefault();
                            let pos=$(this).offset();
                            let h=$(this).height();
                            let w=$(this).width();
                            let field  = $(this).attr(ATTR_FIELD);
                            let html = HU.open(TAG_DIV,[ATTR_STYLE,HU.css(CSS_MARGIN,HU.px(2),
									  CSS_MARGIN_LEFT,HU.px(5),
									  CSS_MARGIN_RIGHT,HU.px(5))]);
			    let title = "Add arguments to -db tag";
                            if(field  != "table") {
				title+=" for field: " + field;
			    } else {
				title+=" for the table";
			    }

			    html+=HU.open(TAG_DIV,[ATTR_STYLE,HU.css(CSS_MARGIN_LEFT,HU.px(5))]);
                            if(field  == "table") {
				html+=HU.b("Basic")+HU.br();
				html +=_this.makeDbMenu(field+".name")+space+
				    _this.makeDbMenu(field+".label")+HU.br();
				html+=  HU.b("Defaults")+HU.br() +
				    _this.makeDbMenu(field+".cansearch","false")+space+
				    _this.makeDbMenu(field+".canlist","false")+HU.br();
				html+= HU.b("Install")+HU.br()+
				    _this.makeDbMenu("install","true")+space+
				    _this.makeDbMenu("nukedb","true")+HU.br()
				html +=_this.makeDbMenu(field+".category")+space+
				    _this.makeDbMenu(field+".superCategory")+HU.br();	
				html +=_this.makeDbMenu(field+".icon","/icon/icon.png")+space;
				
                            } else {
				html+=HU.b("Basic")+HU.br();
				html +=_this.makeDbMenu(field+".id")+space;
				html +=_this.makeDbMenu(field+".label")+HU.br();
				html +=
				    HU.b("Type")+HU.br()+
                                    _this.makeDbMenu(field+".type","string","string")+space +
                                    _this.makeDbMenu(field+".type","double","double")+space +
                                    _this.makeDbMenu(field+".type","integer","integer")+space +
                                    _this.makeDbMenu(field+".type","enumeration","enumeration")+space +
                                    _this.makeDbMenu(field+".type","enumerationplus","enumeration+")+space +
                                    _this.makeDbMenu(field+".type","multienumeration","multienumeration+")+space +
                                    _this.makeDbMenu(field+".type","date","date")+space +
                                    _this.makeDbMenu(field+".type","url","url")+space +
                                    HU.br();
				html +=
				    HU.b("Search")+HU.br()+
                                    _this.makeDbMenu(field+".cansearch","true")+space +
                                    _this.makeDbMenu(field+".isindex","true")+space +
                                    _this.makeDbMenu(field+".addnot","true")+space +
                                    _this.makeDbMenu(field+".canlist","true")+
                                    HU.br();
				html +=
				    HU.b("Misc")+HU.br()+
                                    _this.makeDbMenu(field+".size","1000")+space+
                                    _this.makeDbMenu(field+".changetype","true") + space +
                                    _this.makeDbMenu(field+".unit","kg")+space+
                                    _this.makeDbMenu(field+".suffix","")+space+
                                    _this.makeDbMenu(field+".help","")+space;
                            }
                            html+="</div></div>";
			    _this.dbDialog=HU.makeDialog({title:title,content:html,anchor:$(this),header:true,draggable:true});
			    _this.dbDialog.find(".ramadda-clickable").click(function() {
				_this.insertDb($(this).attr('field'),$(this).attr('value'));

			    });
			})

		    } else if(csv) {
			let isResultJson = (result.startsWith("{") && result.endsWith("}")) || (result.startsWith("[") && result.endsWith("]"));
			if(isResultJson) {
			    try {
				let json= JSON.parse(result);
				result = Utils.formatJson(json,5);
				output.html(HU.pre(result));
				return
			    } catch(err) {
				console.log("Err:" + err);
			    }
			}

			result = result.trim();
			if(isTypeXml) {
			    writePre(result.replace(/</g,'&lt;').replace(/>/g,'&gt;'));
			    return;
			}
			let isXml = result.startsWith("<") && result.endsWith(">")
			if(isXml) {
			    try {
				let html =Utils.formatXml(result.trim());
				output.html(HU.pre(html));
				output.find(".ramadda-xmlnode").click(()=>{
				    this.insertText($(this).attr("data-path"));
				});
				return;
			    } catch (err) {
				console.log("err");
				console.log("Couldn't display as xml:" + err);
			    }
			}
			result = result.replace(/</g,"&lt;").replace(/>/g,"&gt;");
			let isHeader = result.startsWith("#fields=");
			if(isHeader) {
			    let toks = result.split("\n");
			    let line = toks[0];
			    line = line.replace("#fields=","");
			    line = line.replace(/(\] *),/g,"$1\n");
			    let tmp ="<table><tr><td><b>Field</b></td><td><b>Properties</b></td></tr>";
			    toks = line.split("\n");
			    let rows=[];
			    for(let i=0;i<toks.length;i++) {
				let l = toks[i];
				l = l.replace(/^(.*?)\[/,"<td><span class=csv_addheader_field field='$1' title='Add to input'>$1</span>&nbsp;</td><td>");
				l = l.replace(/\]/,'');
				l+='</td>';
				rows.push(l);
			    }
			    tmp +=Utils.wrap(rows,'<tr>','</tr>');
			    tmp+=HU.close(TAG_TABLE);
			    tmp = HU.div([],'Click on field to set properties') + tmp;
			    result = tmp; 
			    output.html(result);
			} else {		 	    
			    writePre(result);
			}
			if(isHeader) {
			    output.find(".csv_addheader_field").click(function(event) {
				event.preventDefault();
				let field = $(this).attr(ATTR_FIELD);
				let pos=$(this).offset();
				let h=$(this).height();
				let w=$(this).width();
				let html = HU.open(TAG_DIV,[ATTR_STYLE,HU.css(CSS_MARGIN,HU.px(2),
									      CSS_MARGIN_LEFT,HU.px(5),
									      CSS_MARGIN_RIGHT,HU.px(5))]);
				html+=HU.center('Note: Have focus in the -addheader command properties');
				html +=HU.b('type: ') + 
				    _this.makeHeaderMenu(field+".type","enumeration","enumeration")+ SPACE2+
				    _this.makeHeaderMenu(field+".type","string","string")+ SPACE2+	
				    _this.makeHeaderMenu(field+".type","double","double")+SPACE2+
				    _this.makeHeaderMenu(field+".type","integer","integer")+SPACE2+
				    _this.makeHeaderMenu(field+".type","date","date")+ SPACE2 +
				    _this.makeHeaderMenu(field+".type","url","url")+SPACE2 +
				    _this.makeHeaderMenu(field+".type","image","image");

				html += '<br>' +
				    _this.makeHeaderMenu(field+".id",field)+ SPACE2+				    
				    _this.makeHeaderMenu(field+".label",'{' + field+'}','label')+ SPACE2 +
				    _this.makeHeaderMenu(field+".unit","unit","unit");
				html +=HU.br() +
				    _this.makeHeaderMenu(field+".format","{yyyy-MM-dd hh:mm:ss}","date format")+ SPACE2;
				html +=HU.br() +
				    _this.makeHeaderMenu(field+".enumeratedValues","{value1:label1;value2:label2}","enum values");
				

				html+='<br>';
				html +=HU.b('Default type: ') + 
				    _this.makeHeaderMenu("default.type","enumeration","enumeration")+ SPACE2+
				    _this.makeHeaderMenu("default.type","string","string")+ SPACE2+	
				    _this.makeHeaderMenu("default.type","double","double")+SPACE2+
				    _this.makeHeaderMenu("default.type","integer","integer")+SPACE2+
				    _this.makeHeaderMenu("default.type","date","date")+ SPACE2 +
				    _this.makeHeaderMenu("default.type","url","url")+SPACE2 +
				    _this.makeHeaderMenu("default.type","image","image");
				html+=HU.close(TAG_DIV);
				let dialog =   HU.makeDialog({header:true,title:'Set properties in -addheader for field: ' + field,
							      sticky:true,
							      draggable:true,
							      content:html,anchor:$(this)});
				dialog.find(".ramadda-clickable").click(function() {
				    _this.insertHeader($(this).attr(ATTR_FIELD),$(this).attr(ATTR_VALUE));
				});
			    });
			}
			return;
		    } else if(stats || table) {
			output.html(result);
			let toolbar = HU.span([ATTR_TITLE,"Insert all field names", ATTR_CLASS,CLASS_CLICKABLE, ATTR_ID,this.domId("addfields")],"Add field ids") + SPACE3;
			if(table)
			    toolbar += HU.span([ATTR_ID,this.domId('csv_toggledetails')],"Show summary");

			toolbar=HU.div([ATTR_STYLE,HU.css(CSS_DISPLAY,DISPLAY_INLINE_BLOCK,
							  CSS_MARGIN_BOTTOM,HU.px(4))], toolbar);
			output.find("#header").html(toolbar);
			let _this = this;
			let visible = false;
			if(table)
			    output.find(".th2").hide();
			this.jq('csv_toggledetails').button().addClass(CLASS_CLICKABLE).click(function(){
			    visible = !visible;
			    $(this).html(visible?"Hide summary":"Show summary");
			    if(visible)
				output.find(".seesv-table-summary").show();
			    else
				output.find(".seesv-table-summary").hide();
			});

			this.columnIds =  [];
			let idComps = output.find( ".csv-id");
			idComps.each(function() {
			    _this.addColumnId($(this).attr('fieldid'));
			});
			this.jq("addfields").button().click(()=>{
			    let f = this.columnIds.join(",");
			    this.insertColumnIndex(f,true);
			});

			idComps.css(CSS_COLOR,'blue').css(CSS_FONT_WEIGHT,'normal').css(CSS_CURSOR,'pointer').attr(ATTR_TITLE,'Add field id. shift:prepend comma').click(function(evt) {
			    let id = $(this).attr('fieldid');
			    if(evt.shiftKey) id=','+id;
			    _this.insertColumnIndex(id,true);
			});
			let cnt = 0;
			idComps.each(function() {
			    let id = $(this).attr('fieldid');
			    $(this).attr(ATTR_TITLE,$(this).attr(ATTR_TITLE)+" - " + id +" (#" + cnt+")");
			    cnt++;
			});
			if(table) {
			    //			    Utils.makeDownloadFile('test.html',output.html());
			    try {
				//Don't format the table as it is always screwed up with the header, etc
				//HU.formatTable(output.find( ".ramadda-table"),{paging:false,height:"200px",fixedHeader: true,scrollX:true});
			    } catch(err) {
				//Ignore this
			    }
			}
		    } else if(!raw && showHtml) {
  			let newresult = result.replace(/(<th>.*?)(#[0-9]+)(.*?<.*?>)([^<>]*?)(<.*?)<\/th>/g,"$1<a href='#' index='$2' style='color:blue;' class=csv_header_field field='table' onclick=noop()  title='Add field id'>$2</a>$3<a href='#' label='$4' style='color:blue;' class=csv_header_field field='table' onclick=noop()  title='Add field id'>$4</a>$5</th>");
			result = newresult;
			output.html(result);
			output.find(".csv_header_field").each(function() {
			});

			output.find(".csv_header_field").click(function(event) {
			    $(this).attr(ATTR_STYLE,"color:black;");
			    let label = $(this).attr("label");
			    if(!label) {
				let index = $(this).attr("index");
				if(index) label = index.replace("#","").trim();
			    } else {
				label = Utils.makeId(label);
			    }
			    _this.insertColumnIndex(label);
			});
			HU.formatTable(".ramadda-table",{xordering:true});
		    } else if(isRecord) {
			output.html(result);
			let ids = 		output.find('.seesv-record-id');
			ids.addClass(CLASS_CLICKABLE);
			ids.attr(ATTR_TITLE,'Add field id. shift:prepend comma');
			ids.click(function(event) {
			    let v = $(this).attr('data-id');
			    if(event.shiftKey) v = ','+ v;
			    _this.insertText(v);
			});
		    } else {
			result = result.trim();
			let isJson = (result.startsWith("{") && result.endsWith("}")) || (result.startsWith("[") && result.endsWith("]"));
			let isXml = result.startsWith("<") && result.endsWith(">")
			let out = "";
			if(isJson) {
			    try {
				result= JSON.parse(result.trim());
				result = Utils.formatJson(result,5);
				out=HU.pre(result);
			    } catch(err) {
				console.log("Error:" + err);
				//				out = "<b>Error processing json:" + err+"</b><br>";
				result = result.replace(/,/g,',\n').replace(/\s\s+/g,' ');
				out +=HU.pre(result);
			    }
			    output.html(out);
			    return
			} else if(isXml) {
			    try {
				let html =Utils.formatXml(result.trim());
				output.find(".ramadda-xmlnode").click(function(){
				    _this.insertText($(this).attr("data-path"));
				});
				return;
			    } catch (err) {
				out="error processing xml:" + err;
				console.log("Couldn't display as xml:" + err);
			    }
			}
 			if(printHeader) {
			    let tmp = "";
			    result = result.replace("#fields=","");
			    result.split("\n").forEach(line=>{
				line = line.trim();
				if(line=="") return;
				let regexp = new RegExp("([^ ]+) ([^ \\[]+)\\[(.*)\\]$");
				let toks = line.match(regexp);
				if(!toks)  {
				    toks = line.match("([^ ]+)(.*)");
				}
				if(!toks) {
				    tmp+= "<tr>" + HU.tds([],[line]) +"</tr>";
				    return;
				}
				let index = toks[1];
				let id = toks[2];
				let attrs = toks.length>2?toks[3]:"";
				id = "<a href='#' plain='true' index='" + id+"' style='color:blue;text-decoration:none !important;' class=csv_header_field field='table' onclick=noop()  title='Add field id'>" + id +"</a>";
				line = "<tr>" + HU.tds([],[index,"",id,attrs]) +"</tr>";
				tmp+=line;
			    });
			    result = "<table><tr><td><b>Index</b></td><td>&nbsp;</td><td><b>ID</b></td><td><b>Attributes</b></td></tr>" + tmp +"</table>"; 
			} else {
			    result = result.replace(/</g,"&lt;");
			    result = result.replace(/>/,"&gt;");
			}
			let html = printHeader?result:HU.tag(TAG_PRE,[], result);
			if(isDb) {
			    html+=HU.div([ATTR_ID,this.domId(ID_POPUP), ATTR_CLASS,"ramadda-popup"]);
			} else if(!printHeader) {
			    writePre(result);
			    return;
			} 
			output.html(html);
			if(printHeader) {
			    output.find(".csv_header_field").click(function(event) {
				let index = $(this).attr("index").replace("#","").trim();
				_this.insertColumnIndex(index,$(this).attr("plain"));
			    });
			}			
		    }
		    return;
		}
		output.html(HU.pre([],"No response given"));
	    })
		.fail(function(jqxhr, textStatus, error) {
		    let err = textStatus + ", " + error;
		    _this.output(HU.pre([],"Error:" + err));
		});
	},
	addCommand:function(cmd, args, anchor) {
	    if(typeof cmd == "string") {
		cmd = this.commandsMap[cmd];
	    }
	    let opts = {
		add:true,
		values:null,
		callback:null
	    };
	    if(args) $.extend(opts, args);
	    let desc = cmd.description.replace(/^\(/,"").replace(/\)$/,"");
	    let label = cmd.label || Utils.camelCase(cmd.command.replace(/^-/,""));
	    let inner = HU.div([ATTR_CLASS,'ramadda-heading'],label) + HU.center(desc);
	    if(cmd.command=='-addheader') {
		inner+=HU.center('Click <i>Outputs-&gt;Print text output</i> to see header');
	    }

	    inner+=HU.formTable();
	    this.columnInput = null;
	    this.headerInput = null;	    
	    this.dbInput = null;	    
	    cmd.args.forEach((a,idx)=>{
		let v = opts.values && idx<opts.values.length?opts.values[idx]:"";
		let label = a.label || Utils.makeLabel(a.id);
		label = label+':';
		let id = this.domId("csvcommand" + idx);
		let desc = a.description||"";
		//		desc = desc.replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/\n/g,"<br>");
		desc = desc.trim().replace(/\n/g,HU.br());		
		let getExtra = (arg,desc)=>{
		    if((arg.type=='column' || arg.type=='columns') && this.allColumnIds.length>0) {
			return HU.span([ATTR_INPUTID,id,
					ATTR_TITLE,'Add column',
					ATTR_CLASS,'ramadda-clickable seesv-column-button',ATTR_COLUMNID,id],HU.getIconImage('fa-plus') + ' ' +desc);

		    }
		    return '';
		};
		let getDesc = (arg,oneLine)=>{
		    if((desc+':') == label) desc='';
		    let extra = getExtra(arg,desc);
		    if(extra=='' && desc=='') return '';
		    if(Utils.stringDefined(extra)) {
			desc = extra;
		    }
		    let help = "";
		    if(arg.type=="columns")
			help=HU.br()+HU.href(ramaddaBaseUrl +'/userguide/seesv.html#help_columns',HU.getIconImage(ICON_HELP),
					     [ATTR_TARGET,'_help',ATTR_TITLE,'Columns Help']);
		    desc+=help;
		    return   HU.div([ATTR_STYLE,HU.css(CSS_MAX_WIDTH,HU.px(300),
						       CSS_VERTICAL_ALIGN,ALIGN_TOP,
						       CSS_MAX_HEIGHT,HU.px(200),
						       CSS_OVERFLOW_Y,OVERFLOW_AUTO)],desc);
		}


		if(!this.headerInput && cmd.command=="-addheader" && a.id=="properties") {
		    this.headerInput = id;
		}
		if(!this.dbInput && cmd.command=="-db" && a.id=="properties") {
		    this.dbInput = id;
		}		
		if(a.type=="list" || a.type=="columns" || a.type=="rows") {
		    let delim = a.delimiter||",";
		    let lines = v.split(delim);
		    v = lines.join("\n");
		    let numRows = a.type=="rows"?3:(a.rows||"3");
 		    inner+=HU.formEntryTop(label,HU.hbox([HU.textarea("",v,[ATTR_COLS, a.size ?? "30",
									    ATTR_ROWS,numRows,ATTR_ID,id]),
				 			  getDesc(a)]));
		} else	if(a.rows) {
		    if(a.delimiter) {
			let lines = Utils.split(v??'',a.delimiter);
			v=Utils.join(lines,'\n');
		    }
		    desc = desc.replace(/<add>/g,"<add data-id=" + id+">");
		    inner+=HU.formEntryTop(label,
					   HU.hbox([HU.textarea("",v,[ATTR_COLS, a.columns || a.size || "30",
								      ATTR_ROWS,a.rows,
								      ATTR_ID,id,ATTR_SIZE,10]),
						    HU.div([ATTR_STYLE,HU.css(CSS_MAX_HEIGHT,HU.px(200),
									      CSS_OVERFLOW_Y,OVERFLOW_AUTO)],  desc)]));		
		} else if(a.values || a.type=="enumeration") {
		    let values
		    if(a.values) {
			values = a.values;
		    } else {
			console.log(JSON.stringify(a));
			value="foo,bar";
		    }
		    inner+=HU.formEntry(label,HU.hbox([HU.select("",[ATTR_ID,id],values.split(","),v),getDesc(a)]));
		} else if(a.type=="boolean") {
		    inner+=HU.formEntry(label,HU.hbox([HU.select("",[ATTR_ID,id],['true','false'],v),getDesc(a)]));		    
		} else if(a.type=="column") {
		    let size = a.size || 30;
		    let title = a.tooltip || "";
		    let input;
		    //		    if(this.columnIds) {
		    //			input  = HU.select("",[ATTR_ID,id,ATTR_TITLE, title],this.columnIds,v);
		    //		    } else {
		    input  = HU.input("",v,[ATTR_ID,id,ATTR_SIZE,size,ATTR_TITLE, title]);
		    //		    }
		    inner+=HU.formEntry(label,HU.hbox([input, getDesc(a,true)]));
		} else {
		    let size = a.size ||30;
		    if(a.type=="number") size=a.size||5;
		    let placeholder = a.placeholder || "";
		    let title = a.tooltip || "";
		    if(a.type=="pattern" && !a.placeholder)
			title = "Escapes- _leftparen_, _rightparen_, _leftbracket_, _rightbracket_, _dot_, _dollar_, _star_, _plus_, _nl_"
		    let input = HU.input("",v,[ATTR_ID,id,ATTR_SIZE,size,ATTR_TITLE, title, ATTR_PLACEHOLDER,placeholder]);
		    inner+=HU.formEntry(label,HU.hbox([input, getDesc(a,true)]));
		}
	    });
	    inner+=HU.formTableClose();
	    let help = ramaddaBaseUrl +'/userguide/seesv.html#' + cmd.command;
	    help = HU.div([ATTR_STYLE,HU.css(CSS_DISPLAY,DISPLAY_INLINE_BLOCK),
			   ATTR_ID,this.domId('showhelp')], HU.href(help,'Help',
								    [ATTR_TITLE,'Command Help',ATTR_TARGET,'_help']));


	    let buttons = HU.buttons([HU.div([ATTR_ID,this.domId(ID_ADDCOMMAND)],opts.add?"Add Command":"Change Command"),
				      HU.div([ATTR_ID,this.domId(ID_CANCELCOMMAND)],"Cancel"),help]);

	    inner+=buttons;
	    if(this.addDialog) {
		this.addDialog.hide();
	    }

	    let target = anchor;
	    let at = "left bottom";
	    if(opts.event) {
		at = "left " + "top+" + (opts.event.offsetY+10);
		target = $(opts.event.target);
	    }
	    inner = HU.div([ATTR_STYLE,HU.css(CSS_MARGIN,HU.px(5))], inner);
	    let dialog =   HU.makeDialog({content:inner,my:"left top",at:at,anchor:target,draggable:true,header:true,inPlace:false});

	    dialog.find("add").addClass(CLASS_CLICKABLE).click(function() {
		let contents = $(this).html();
		let id = $(this).attr('data-id');
		contents = contents.replace(/&lt;/g,'<').replace(/&gt;/g,'>');
		if(id) HU.insertIntoTextarea(id,contents+'\n');
	    });


	    let _this = this;
	    dialog.find(".seesv-column-button").click(function() {
		if(_this.allColumnIds.length==0) return;
		let inputId = $(this).attr(ATTR_INPUTID);
		let contentDiv = HU.getUniqueId('content');
		let searchDiv = HU.getUniqueId('search');
		let html = "";
		html+=HU.div([ATTR_ID,searchDiv]);
		let inner =HU.open(TAG_DIV,[ATTR_ID,contentDiv]);
		_this.allColumnIds.forEach(id=>{
		    inner+=HU.div([ATTR_CLASS,CLASS_CLICKABLE,ATTR_COLUMNID,id],id);
		});
		inner +=HU.close(TAG_DIV);
		inner = HU.div([ATTR_STYLE,HU.css(CSS_MIN_HEIGHT,HU.em(2),
						  CSS_MAX_HEIGHT,HU.px(200),
						  CSS_OVERFLOW_Y,OVERFLOW_AUTO)], inner);
		html+=inner;
		html = HU.div([ATTR_STYLE,HU.css(CSS_MIN_WIDTH,HU.px(400),
						 CSS_MARGIN,HU.px(5))], html);
		let popup =   HU.makeDialog({content:html,my:"left top",at:"left bottom",anchor:$(this)});
		HU.initPageSearch('#'+contentDiv+' .ramadda-clickable',null,'Search',null,{target:jqid(searchDiv)});
		popup.find(".ramadda-clickable").click(function() {
		    let id = $(this).attr(ATTR_COLUMNID);
		    _this.insertColumnIndex(id,true,inputId);
		});
	    });

	    let submit = () =>{
		this.columnInput = null;
		let args = "";
		let values =[];
		cmd.args.forEach((a,idx)=>{
		    let v = this.jq("csvcommand" +idx).val();
		    if(a.type=="list" || a.type=="columns" || a.type=="rows") {
			let tmp = "";
			let delim = a.delimiter||",";
			v.split("\n").forEach(line=>{
			    line = line.trim();
			    if(line=="") return;
			    if(tmp!="") tmp+=delim;
			    tmp +=line;
			});
			v = tmp;
		    }
		    if(v.indexOf("\n")>0) {
			v = "{"+v+"}";
		    } else if(v.indexOf("{")>0) {
			v = "\""+v+"\"";
		    } else  
			if(v=="" || v.indexOf(" ")>=0 || v.indexOf(",")>=0) v = "\"" + v +"\"";
		    values.push(v);
		    args+=v +" ";
		});
		if(opts.callback) {
		    opts.callback(values,args);
		} else {
		    this.insertText(cmd.command +" " + args) ;
		}
		dialog.remove();
	    }
	    
	    HU.onReturnEvent(dialog.find("input"),()=>{submit()});
	    

	    this.jq("csvcommand0").focus();
	    this.jq('showhelp').button().click(()=>{});
	    this.jq(ID_ADDCOMMAND).button().click(()=>{
		submit();
	    });
	    this.jq(ID_CANCELCOMMAND).button().click(()=>{
		if(opts.callback) {
		    opts.callback(null);
		}
		this.columnInput = null;
		dialog.remove();
	    });    
	},
    });

    this.init();
    if(this.params.initialCommand) {
	this.display(this.params.initialCommand,null,true);
    }
    this.lastSavedInput = this.getInput();

}



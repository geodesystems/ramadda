
var ID_LLM_INPUT_FIELD= 'inputfield';
var ID_LLM_INPUT_TEXTAREA= 'inputtextarea';    
var ID_LLM_CHANGEINPUT = 'changeinput';
var ID_LLM_PROGRESS1='progress1';
var ID_LLM_PROGRESS2='progress2';
var ID_LLM_SUBMIT = 'submit';
var ID_LLM_TEXTAREA_HOLDER = 'textareadiv';

function DocumentChat(id,entryId,action,models,args) {
    if(!models) models =[];
    this.id = id;
    this.opts = {
	showOffset:true,
    }
    if(args) $.extend(this.opts,args);
    if(!Utils.stringDefined(this.opts.placeholder)) {
	this.opts.placeholder = 'LLM Input, e.g., - List the 5 main points. \nNote: this will send the document text to the selected LLM model.';
    }
    let cnt = 0;
    let div  =jqid(id);
    let chat = HU.open(TAG_DIV,[ATTR_STYLE,
				HU.css(CSS_MARGIN_LEFT,HU.px(5),CSS_MAX_WIDTH,HU.perc(100),CSS_OVERFLOW_X,OVERFLOW_AUTO)]);
    let mike = HU.span([ATTR_TITLE,'Voice transcribe',ATTR_ID,this.domId('transcribe'),
			ATTR_CLASS,CLASS_CLICKABLE],
		       HU.getIconImage('fa-solid fa-microphone fa-gray'));
    let left = HU.div([],
		      HU.span([ATTR_TITLE,'History',
			       ATTR_CLASS,CLASS_CLICKABLE,
			       ATTR_ID,this.domId('history')],
			      HU.getIconImage('fas fa-list'))+
		      SPACE1+
		      //		      mike +     SPACE1+
		      HU.span([ATTR_ID,this.domId('button_clear'),
			       ATTR_TITLE,'Clear output',
			       ATTR_CLASS,CLASS_CLICKABLE],
			      HU.getIconImage(ICON_ERASER))
		      /*+
			HU.space(2) +
			HU.checkbox(this.domId('button_clearalways'),[ATTR_TITLE,'Always clear output'],false,'Clear')*/);
    let right = '';
    if(models.length>1) {
	right+=HU.b('Model: ') +HU.select('',['id',this.domId('chatmodel')],models);
	right+=SPACE;
    }
    if (this.opts.showOffset) {
	right+=HU.b('Offset: ') +HU.input('','0',
					  [ATTR_ID,this.domId('chatoffset'),ATTR_TITLE,'Offset into document','size','3']);

	right+=SPACE1;
	right+=HU.span([ATTR_ID,this.domId('info'),ATTR_TITLE,''],HU.getIconImage('fas fa-circle-info'));
    }
    
    chat+=HU.div([ATTR_STYLE,HU.css(CSS_MARGIN,HU.px(4))],HU.leftRightTable(left,right));
    let text=''
    let submit = HU.span([ATTR_TITLE,'Submit',
			  ATTR_STYLE, HU.css(CSS_MARGIN_RIGHT,HU.px(4)),
			  ATTR_ID, this.domId(ID_LLM_SUBMIT)],HU.getIconImage('fa-regular fa-share-from-square'));
    text += submit;
    text += HU.input('','',[ATTR_PLACEHOLDER,this.opts.placeholder,
			    ATTR_STYLE,HU.css(CSS_WIDTH,HU.perc(100),CSS_MIN_WIDTH,HU.perc(100),CSS_FONT_SIZE,HU.px(18)),
			    ATTR_ID,this.domId(ID_LLM_INPUT_FIELD),
			    ATTR_CLASS,'ramadda-documentchat-input']);    


    let textArea =  HU.textarea('','',[ATTR_PLACEHOLDER,this.opts.placeholder,
				       ATTR_ROWS,'3',ATTR_STYLE,HU.css(CSS_WIDTH,HU.perc(100)),
				       ATTR_ID,this.domId(ID_LLM_INPUT_TEXTAREA),
				       ATTR_CLASS,'ramadda-documentchat-input']);    
    
    let holder = HU.div([ATTR_ID,this.domId(ID_LLM_TEXTAREA_HOLDER),
			 ATTR_STYLE,HU.css(CSS_WIDTH,HU.perc(100),CSS_DISPLAY,'none')],
			HU.div([ATTR_STYLE,HU.css(CSS_WIDTH,HU.perc(100))], textArea));

    text=HU.div([ATTR_STYLE,HU.css(CSS_WIDTH,HU.perc(100),CSS_DISPLAY,'flex',
				   CSS_ALIGN_ITEMS,'flex-start',
				   CSS_WHITE_SPACE,'nowrap',
				   CSS_VERTICAL_ALIGN,'top')],
		text+holder);
    let change = HU.span([ATTR_STYLE,HU.css(CSS_MARGIN_LEFT,HU.px(4),CSS_DISPLAY,DISPLAY_INLINE_BLOCK),
			  ATTR_TITLE,'Toggle input',
			  ATTR_ID,this.domId(ID_LLM_CHANGEINPUT)], HU.getIconImage('fa-solid fa-angle-right'));
    this.inputShown = true;
    text+=change;
    text = HU.div([ATTR_STYLE,HU.css(CSS_WIDTH,HU.perc(100),
				     CSS_DISPLAY,'flex',
				     CSS_ALIGN_ITEMS,'flex-start',
				     CSS_WHITE_SPACE,'nowrap',CSS_VERTICAL_ALIGN,'top')],text);
    
    let makeProgress = (id,width,top) => {
	return HU.div([ATTR_ID,this.domId(id),
		       ATTR_CLASS,CLASS_CLICKABLE,
		       ATTR_STYLE,HU.css(CSS_POSITION,POSITION_ABSOLUTE,
					 CSS_LEFT,HU.perc(50),
					 CSS_DISPLAY,'none',
					 CSS_TRANSFORM,'translate(-50%, 0)',
					 CSS_TOP,top)],
		      HU.image(ramaddaBaseUrl+'/icons/mapprogress.gif',[ATTR_WIDTH,width,
									ATTR_TITLE,'Clear',
									ATTR_CLASS,CLASS_CLICKABLE]));

    };

    chat +=HU.div([ATTR_STYLE,HU.css(CSS_POSITION,DISPLAY_RELATIVE)],
		  text+
		  makeProgress(ID_LLM_PROGRESS1,HU.px(24),HU.px(0))+
		  makeProgress(ID_LLM_PROGRESS2,HU.px(40),HU.px(10))		  
		 );
    

    chat+=HU.div([ATTR_ID,this.domId('output'),ATTR_STYLE,HU.css(CSS_MAX_HEIGHT,HU.px(800),CSS_OVERFLOW_Y,OVERFLOW_AUTO)]);
    chat+='</div>'
    div.html(chat);
    this.jq(ID_LLM_CHANGEINPUT).button().click(function() {
	_this.inputShown =  !_this.inputShown;
	if(_this.inputShown) {
	    $(this).html(HU.getIconImage('fa-solid fa-angle-right'));
	    _this.jq(ID_LLM_INPUT_FIELD).show();
	    _this.jq(ID_LLM_INPUT_FIELD).val(_this.jq(ID_LLM_INPUT_TEXTAREA).val());
	    _this.jq(ID_LLM_TEXTAREA_HOLDER).hide();
	} else {
	    $(this).html(HU.getIconImage('fa-solid fa-angle-down'));
	    _this.jq(ID_LLM_INPUT_FIELD).hide();
	    _this.jq(ID_LLM_TEXTAREA_HOLDER).show();
	    _this.jq(ID_LLM_INPUT_TEXTAREA).val(_this.jq(ID_LLM_INPUT_FIELD).val());
	}
	
    });
    this.history= [];
    let _this = this;
    let output = this.jq('output');
    let progress1=this.jq(ID_LLM_PROGRESS1);
    let progress2=this.jq(ID_LLM_PROGRESS2);    
    let step = 0;
    let toggleProgress = (on1,on2) =>{
	if(on1) progress1.show();
	else progress1.hide();
	if(on2) progress2.show();
	else progress2.hide();		
    }
    let process = (q)=>{
	if(!q || q.trim().length==0) return;
	if(!this.history.includes(q)) this.history.push(q);
	let input = this.getInput();
	input.prop(ATTR_DISABLED,true);
	input.css(CSS_BACKGROUND,'#efefef');
	if(this.inputShown)  {
	    toggleProgress(true,false);

	} else {
	    toggleProgress(false,true);
	}
	if(this.jq('button_clearalways').is(':checked')) output.html('');
	let url =ramaddaBaseUrl+'/entry/action';

	let offset = 0;
	if(this.opts.showOffset) {
	    offset = this.jq('chatoffset').val().trim();
	}
	

        let args = {
	    action:action,
            entryid: entryId,
	    question:q,
	    offset:offset
        };
	if(this.opts.thread)
	    args.thread = this.opts.thread;
	let model = this.jq('chatmodel').val()
	if(model) {
	    args.model = model;
	}
	let myStep = ++step;
        $.post(url, args, (result) => {
	    if(step!=myStep) {
		return;
	    }
	    clear();
	    _this.getInput().val('');
	    let r;
            if (result.error) {
                r="Error: " + result.error;

            } else {
		let tt='';
		if(Utils.isDefined(result.corpusLength)) {
		    tt=Utils.join(['Corpus length: ' + result.corpusLength,
				   'Segment length: ' + result.segmentLength],'<br>');		
		}
		this.jq('info').attr(ATTR_TITLE,tt);
		this.jq('info').tooltip({
		    content:()=>{return tt;}});
		r = result.response??'';
		if(result.thread) {
		    this.opts.thread = result.thread;
		    //		    console.log('got thread',this.opts.thread);
		}
		r = r.replace(/^-/gm,'&#x2022;').replace(/\n/g,'<br>');
		r = r.replace(/\*\*([^\*]{1,100})\*\*/g,"<b>$1</b>");
		r =r.replace(/<br>/g,'__br__');
		r =r.replace(/<b>/g,'__b__');
		r =r.replace(/<\/b>/g,'__nb__');				
		r =r.replace(/<p>/g,'__p__');
		r = r.replace(/</g,'&lt;');
		r = r.replace(/>/g,'&gt;');		
		r = r.replace(/\[([^\]]+)]\((https?:\/\/[^\)]+)\)/g, (m1,m2,m3)=>{
		    m3=m3.replace(/http/,'_HIDEIT_');
		    return HU.href(m3,m2,[ATTR_TARGET,'_link']);
		});
		r = r.replace(/(https?:\/\/[^\s]+)/g,'<a href=\'$1\'>$1</a>');
		r=r.replace(/_HIDEIT_/g,'http');

		r  = r.replace(/__b__/g,'<b>');
		r  = r.replace(/__nb__/g,'</b>');		
		r  = r.replace(/__br__/g,'<br>');
		r  = r.replace(/__p__/g,'<p>');		
		r = r.replace(/```shell */g,'```');
		r = r.replace(/```bash */g,'```');
		r = r.replace(/```plaintext */g,'```');						
		
		r = r.trim();
		r = r.replace(/```([\s\S]*?)```/g,"<pre>$1</pre>");
		r = r.replace(/<pre><br>/g,'<pre>');
		r = r.replace(/<\/br><\/pre>/g,'</pre>');		
	    }
	    
	    let qid = 'id_' + (cnt++);
	    let guid= HU.getUniqueId('');
	    let out = HU.div([ATTR_STYLE,HU.css(CSS_BORDER,HU.border(1,'transparent'),CSS_FONT_WEIGHT,'bold'),
			      ATTR_ID,this.domId(qid),ATTR_CLASS,CLASS_CLICKABLE,
			      ATTR_TITLE,  'Use question'],
			     q)+HU.div([ATTR_ID,guid],r);
	    out = HU.div(['style',HU.css(CSS_BORDER,HU.border(1,'#eee'),
					 CSS_PADDING,HU.px(4),CSS_MARGIN_TOP,HU.px(8))], out);
	    output.prepend(HU.div([],out));
	    Utils.initCopyable('#'+guid,{addLink:true,extraStyle:HU.css(CSS_RIGHT,HU.px(10),CSS_BOTTOM,HU.px(10))});
	    this.jq(qid).click(function() {
		let input= _this.getInput();
		input.val($(this).html()+' ');
		input.focus();
	    });
	    /*
	      output.animate({
	      scrollTop: output.get(0).scrollHeight
	      }, 2000);
	    */
	    input.focus();
        }).fail((d)=>{
	    toggleProgress();
	    console.log('fail',d);
	    alert('request failed');
	});
    }
    this.jq('transcribe').click(()=> {
	if(!this.transcriber) this.transcriber = new Transcriber(this,{
	    appendLabel:'Query Document',
	    addExtra:false	    
	});
	this.transcriber.doTranscribe();
    });

    this.jq('history').click(function() {
	if(_this.history.length==0) return;
	let html = '';	
	_this.history.slice().reverse().forEach(line=>{
	    html+=HU.div([ATTR_CLASS,HU.classes(CLASS_CLICKABLE,'ramadda-document-history'),
			  ATTR_STYLE,HU.css(CSS_WIDTH,HU.px(400),
					    CSS_BORDER,CSS_BASIC_BORDER,
					    CSS_PADDING,HU.px(5),CSS_MARGIN_BOTTOM,HU.px(5))], line);
	});
	html = HU.div([ATTR_CLASS,CLASS_DIALOG,
		       ATTR_STYLE,HU.css(CSS_MAX_HEIGHT,HU.px(200),
					 CSS_OVERFLOW_Y,OVERFLOW_AUTO)], html);
	_this.dialog =  HU.makeDialog({anchor:$(this),
				       content:html});
	
	_this.dialog.find('.ramadda-document-history').click(function() {
	    _this.dialog.remove();
	    input.val($(this).html());
	    input.focus();
	});
    });

    this.jq('button_clear').click(()=>{
	output.html('');
    });


    let clear = ()=>{
	let input = this.getInput();
	input.prop(ATTR_DISABLED,false);
	input.css(CSS_BACKGROUND,COLOR_WHITE);
	toggleProgress();
	step++;
    };

    progress1.click(()=>{
	clear();
    });
    progress2.click(()=>{
	clear();
    });    
    this.jq(ID_LLM_SUBMIT).button().click(()=>{
	process(this.getInput().val());
	
    });

    this.jq(ID_LLM_INPUT_FIELD).keyup(event=>{
	if(!Utils.isReturnKey(event)) return;
	process(this.jq(ID_LLM_INPUT_FIELD).val().trim());
    });
}


DocumentChat.prototype = {
    jq :function(domId){
	return jqid(this.domId(domId));
    },
    domId:function (domId){
	return this.id+domId;
    },
    getInput:function() {
	if(this.inputShown) return this.jq(ID_LLM_INPUT_FIELD);
	return this.jq(ID_LLM_INPUT_TEXTAREA);	
    },
    getTranscribeAnchor:function() {
	return this.getInput();
    },
    handleTranscribeText:function(text) {
    }
}

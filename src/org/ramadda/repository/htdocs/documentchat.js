
function DocumentChat(id,entryId,action,models,args) {
    if(!models) models =[];
    this.id = id;
    this.opts = {
    }
    if(args) $.extend(this.opts,args);
    if(!Utils.stringDefined(this.opts.placeholder)) {
	this.opts.placeholder = 'LLM Input, e.g., - List the 5 main points';
    }
    let cnt = 0;
    let div  =jqid(id);
    let chat = HU.open('div',[ATTR_STYLE,'margin-left:5px;max-width:100%;overflow-x:auto;']);
    let mike = HU.span([ATTR_TITLE,'Voice transcribe',ATTR_ID,this.domId('transcribe'),ATTR_CLASS,'ramadda-clickable'],
		       HU.getIconImage('fa-solid fa-microphone fa-gray'));
    let left = HU.div([],
		      HU.span([ATTR_TITLE,'History',ATTR_CLASS,'ramadda-clickable',ATTR_ID,this.domId('history')],
			      HU.getIconImage('fas fa-list'))+
		      SPACE1+
//		      mike +     SPACE1+
		      HU.span([ATTR_ID,this.domId('button_clear'),ATTR_TITLE,'Clear output',ATTR_CLASS,'ramadda-clickable'],
			      HU.getIconImage('fas fa-eraser')) +
		      HU.space(2) +
		      HU.checkbox(this.domId('button_clearalways'),[ATTR_TITLE,'Always clear output'],
				  true,'Clear'));
    let right = '';
    if(models.length>1) {
	right+=HU.b('Model: ') +HU.select('',['id',this.domId('chatmodel')],models);
	right+=SPACE;
    }
    right+=HU.b('Offset: ') +HU.input('','0',
				      [ATTR_ID,this.domId('chatoffset'),ATTR_TITLE,'Offset into document','size','3']);

    right+=SPACE1;
    right+=HU.span([ATTR_ID,this.domId('info'),ATTR_TITLE,''],HU.getIconImage('fas fa-circle-info'));
    
    chat+=HU.div([ATTR_STYLE,'margin:4px;'],HU.leftRightTable(left,right));
    let text= HU.textarea('','',['placeholder',this.opts.placeholder,
				 'rows','3',ATTR_STYLE,HU.css('width','100%'),ATTR_ID,this.domId('input'),'class','ramadda-documentchat-input']);    
    chat +=HU.div([ATTR_STYLE,HU.css('position','relative')],
		  text+
		  HU.div([ATTR_ID,this.domId('progress'),
			  ATTR_STYLE,HU.css('position','absolute','left','50%',
					    'display','none',
					    'transform','translate(-50%, 0)',
					    'top','10px')],
			 HU.image(ramaddaBaseUrl+'/icons/mapprogress.gif',['width','60px',
									   'title','Clear',
									   'class','ramadda-clickable'])));


    chat+=HU.div([ATTR_ID,this.domId('output'),ATTR_STYLE,HU.css('max-height','800px','overflow-y','auto')]);
    chat+='</div>'
    div.html(chat);
    this.history= [];
    let _this = this;
    let input = this.input = this.jq('input');
    let output = this.jq('output');
    let progress=this.jq('progress');
    let step = 0;
    let process = q=>{
	if(!q || q.trim().length==0) return;
	if(!this.history.includes(q)) this.history.push(q);
	input.prop('disabled',true);
	input.css('background','#efefef');
	progress.show();
	if(this.jq('button_clearalways').is(':checked')) output.html('');
	let url =ramaddaBaseUrl+'/entry/action';

        let args = {
	    action:action,
            entryid: entryId,
	    question:q,
	    offset:this.jq('chatoffset').val().trim(),
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
	    input.val('');
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
		    console.log('got thread',this.opts.thread);
		}
		r = r.replace(/(https?:\/\/[^\s]+)/g,'<a href=\'$1\'>$1</a>');
		r = r.replace(/^-/gm,'&#x2022;').replace(/\n/g,'<br>');
		r = r.replace(/\*\*([^\*]{1,100})\*\*/g,"<b>$1</b>");
		r =r.replace(/<br>/g,'__br__');
		r =r.replace(/<p>/g,'__p__');
		r = r.replace(/</g,'&lt;');
		r = r.replace(/>/g,'&gt;');		
		r  = r.replace(/__br__/g,'<br>');
		r  = r.replace(/__p__/g,'<p>');		
		r = r.replace(/```shell */g,'```');
		r = r.replace(/```bash */g,'```');				
		
		r = r.trim();
		r = r.replace(/```([\s\S]*?)```/g,"<pre>$1</pre>");
		r = r.replace(/<pre><br>/g,'<pre>');
		r = r.replace(/<\/br><\/pre>/g,'</pre>');		
	    }
	    
	    let qid = 'id_' + (cnt++);
	    let guid= HU.getUniqueId('');
	    let out = HU.div([ATTR_STYLE,'font-weight:bold;',ATTR_ID,this.domId(qid),ATTR_CLASS,'ramadda-clickable',ATTR_TITLE,
			      'Use question'],
			     q)+HU.div([ATTR_ID,guid],r);
	    out = HU.div(['style',HU.css('border','1px solid #eee','padding','4px','margin-top','8px')], out);
	    output.prepend(HU.div([],out));
	    Utils.initCopyable('#'+guid,{addLink:true,extraStyle:'right:10px;bottom:10px;'});
	    this.jq(qid).click(function() {
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
	    html+=HU.div([ATTR_CLASS,'ramadda-clickable ramadda-document-history',ATTR_STYLE,HU.css('width','400px','border','var(--basic-border)','padding','5px','margin-bottom','5px')], line);
	});
	html = HU.div([ATTR_CLASS,'ramadda-dialog',ATTR_STYLE,HU.css('max-height','200px','overflow-y','auto')], html);
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
	input.prop('disabled',false);
	input.css('background','#fff');
	progress.hide();
	step++;
    };

    progress.click(()=>{
	clear();
    });
    input.keyup(event=>{
	if(!Utils.isReturnKey(event)) return;
	process(input.val().trim());
    });
}


DocumentChat.prototype = {
    jq :function(domId){
	return jqid(this.domId(domId));
    },
    domId:function (domId){
	return this.id+domId;
    },
    getTranscribeAnchor:function() {
	return this.input;
    },
    handleTranscribeText:function(text) {
    }
}

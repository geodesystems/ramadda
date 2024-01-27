
function DocumentChat(id,entryId) {
    let cnt = 0;
    let div  =jqid(id);
    let chat = HU.open('div',[ATTR_STYLE,'margin-left:5px;']);
    let left = HU.div([],
		      HU.span([ATTR_ID,id+'_button_clear',ATTR_TITLE,'Clear output',ATTR_CLASS,'ramadda-clickable'],HU.getIconImage('fas fa-eraser')) +
		      HU.space(2) +
		      HU.checkbox(id+'_button_clearalways',[ATTR_TITLE,'Always clear output'],
				  true,'Always clear'));
    let right = HU.b('Offset: ') +HU.input('','0',
			 [ATTR_ID,id+'_chatoffset',ATTR_TITLE,'Offset into document','size','3']);
    chat+=HU.div([ATTR_STYLE,'margin:4px;'],HU.leftRightTable(left,right));


//    div.append(HU.input('','',['placeholder','Document chat input',ATTR_STYLE,HU.css('width','100%'),ATTR_ID,id+'_input','class','ramadda-documentchat-input']));
    chat+= HU.textarea('','',['placeholder','Document chat input','rows','3',ATTR_STYLE,HU.css('width','100%'),ATTR_ID,id+'_input','class','ramadda-documentchat-input']);    
    chat+=HU.div([ATTR_ID,id+'_output',ATTR_STYLE,HU.css('max-height','800px','overflow-y','auto')]);
    chat+='</div>'
    div.html(chat);
    jqid(id+'_button_clear').click(()=>{
	output.html('');
    });
    let input = jqid(id+'_input');
    let output = jqid(id+'_output');
    input.keyup(event=>{
	if(!Utils.isReturnKey(event)) return;
	let q= input.val().trim();
	input.prop('disabled',true);
	input.css('background','#efefef');
	if(jqid(id+'_button_clearalways').is(':checked')) output.html('');
	let url =ramaddaBaseUrl+'/entry/action';

        let args = {
	    action:'documentchat',
            entryid: entryId,
	    question:q,
	    offset:jqid(id+'_chatoffset').val().trim()
        };
        $.post(url, args, (result) => {
	    input.prop('disabled',false);
	    input.css('background','#fff');
	    input.val('');
            if (result.error) {
                alert("Error: " + result.error);
                return;
            }
	    let r = result.response??'';
	    r = r.replace(/^-/gm,'&#x2022;').replace(/\n/g,'<br>');
	    let qid = id+'_id_' + (cnt++);
	    let out = HU.div([ATTR_STYLE,'font-weight:bold;',ATTR_ID,qid,ATTR_CLASS,'ramadda-clickable',ATTR_TITLE,
			      'Use question'],
			     q)+r;
	    out = HU.div(['style',HU.css('border','1px solid #eee','padding','4px','margin-top','8px')], out);
	    output.prepend(HU.div([],out));
	    jqid(qid).click(function() {
		input.val($(this).html()+' ');
		input.focus();
	    });
	    /*
	    output.animate({
		scrollTop: output.get(0).scrollHeight
	    }, 2000);
*/
	    input.focus();
        });
    });
}


function DocumentChat(id,entryId) {
    let div  =jqid(id);
    div.append(HU.input('','',['style',HU.css('background','red','width','100%'),ATTR_ID,id+'_input','class','ramadda-documentchat-input']));
    div.append(HU.div([ATTR_ID,id+'_output','class','ramadda-documentchat-out']));
    jqid(id+'_input').keyup(event=>{
	if(!Utils.isReturnKey(event)) return;
	console.log('r');
    });
    console.log(entryId);
}


function ramaddaDisplayIcons(container) {
    HU.getEmojis(emojis=>{
	let html = "";
	emojis.forEach(cat=>{
	    if(html!="") html+="</div>";
	    html+=HU.open('div',[ATTR_CLASS,'ramadda-icon-category']);
	    html+=HU.div([ATTR_CLASS,'ramadda-icon-category-label'],HU.b(cat.name));
	    cat.images.forEach(image=>{
		html+=HU.image(image.image,['data-copy',image.image,'data-corpus',image.name,ATTR_CLASS,HU.classes(CLASS_CLICKABLE,'ramadda-icon'),
					    ATTR_WIDTH,'24px',ATTR_STYLE,'margin-right:4px;margin-bottom:2px;','loading','lazy',ATTR_TITLE,image.name]);
	    });
	});
	html+="</div>";
	$(container).html(html);
	Utils.initCopyable('.'+CLASS_CLICKABLE + '.ramadda-icon');
    });
}

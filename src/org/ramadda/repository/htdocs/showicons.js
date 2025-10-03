
function ramaddaDisplayIcons(container) {
    HU.getEmojis(emojis=>{
	let html = "";
	emojis.forEach(cat=>{
	    if(html!="") html+=HU.open(TAG_DIV);
	    html+=HU.open(TAG_DIV,[ATTR_CLASS,'ramadda-icon-category']);
	    html+=HU.div([ATTR_CLASS,'ramadda-icon-category-label'],HU.b(cat.name));
	    cat.images.forEach(image=>{
		html+=HU.image(image.image,['data-copy',image.image,'data-corpus',image.name,
					    ATTR_CLASS,HU.classes(CLASS_CLICKABLE,'ramadda-icon'),
					    ATTR_WIDTH,HU.px(24),
					    ATTR_STYLE,HU.css(CSS_MARGIN_RIGHT,HU.px(4),
							      CSS_MARGIN_BOTTOM,HU.px(2)),
					    ATTR_LOADING,'lazy',
					    ATTR_TITLE,image.name]);
	    });
	});
	html+=HU.close(TAG_DIV);
	$(container).html(html);
	Utils.initCopyable('.'+CLASS_CLICKABLE + '.ramadda-icon');
    });
}

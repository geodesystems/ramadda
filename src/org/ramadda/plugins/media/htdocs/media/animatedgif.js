
var AnimatedGif =  {
    init:function(control, div,addButtons) {
	if(addButtons) {
	    jqid(div).html("Loading");
	}
	control.load((gif)=>{
	    if(addButtons) {
		AnimatedGif.addButtons(control,div);
		//Listen for left/right arrow
		jqid(div).attr(ATTR_TABINDEX,1);
		jqid(div).keydown((evt)=>{
		    if(evt.keyCode==39) {
			control.move_relative(1);
		    } else if(evt.keyCode==37) {
			control.move_relative(-1);
		    }
		});
	    }
	});
    },
    addButtons:function(control, div) {
	let count = control.get_length();
	let html = "";
	let cnt=0;
	for(let i=0;i<count;i++) {
	    html+=HU.div(["data-index",i,
			  ATTR_STYLE,
			  HU.css(CSS_FONT_SIZE,HU.pt(8),
				 CSS_DISPLAY,DISPLAY_INLINE_BLOCK,
				 CSS_PADDING_RIGHT,HU.px(1),
				 CSS_PADDING_LEFT,HU.px(1),
				 CSS_MARGIN_RIGHT,HU.px(1)),
			  ATTR_CLASS,HU.classes(CLASS_HOVERABLE,CLASS_CLICKABLE,'gif-index')],(i+1));
	    if(cnt++>=30) {
		html+=HU.br();
		cnt=0;
	    }
	}

	let buttons = 	jqid(div).html(html).find(HU.dotClass('gif-index'));
	buttons.click(function() {
	    let idx = +$(this).attr("data-index");
	    control.move_to(idx);
	});

	control.add_frame_listener((index)=>{
	    buttons.each(function() {
		let idx = +$(this).attr("data-index");
		if(idx==index)
		    $(this).css(CSS_BACKGROUND,"#ccc");
		else
		    $(this).css(CSS_BACKGROUND,COLOR_TRANSPARENT);
	    });
	});
	

    }
}


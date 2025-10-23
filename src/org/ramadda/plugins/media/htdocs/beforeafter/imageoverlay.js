function imageOverlayInit(id) {
    jqid('slider_' + id ).slider({
	min:0,
	max:100.0,
	value:50,
	slide: function( event, ui ) {
	    let opacity = +ui.value/100.0;
	    jqid('after_' + id).css(CSS_OPACITY,opacity);
	}
    });

}

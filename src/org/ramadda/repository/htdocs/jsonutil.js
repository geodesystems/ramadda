/**
   Copyright (c) 2008-2025 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/

RamaddaJsonUtil = {
    init:function(id) {
	let brackets = jqid(id).find('.ramadda-json-openbracket');
	brackets.each(function() {
	    $(this).append(HU.span(['visible','true','class','ramadda-json-toggle'],HU.getIconImage(icon_folderopen)));
	});
	let toggles = 	brackets.find('.ramadda-json-toggle');
	toggles.css('cursor','pointer');
	toggles.click(function() {
	    let parent = $(this).closest('.ramadda-json-openbracket');
	    let next = parent.next('.ramadda-json-block');
	    let visible = $(this).attr('visible')=='true';
	    visible=!visible;
	    $(this).attr('visible',visible);
	    if(visible) {
		next.show(500);
		$(this).html(HU.getIconImage(icon_folderopen));
	    } else {
		next.hide(500);
		$(this).html(HU.getIconImage(icon_folderclosed));
	    }
	});
    }
}

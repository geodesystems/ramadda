
var AnimatedGif =  {
    init:function(control, div,addButtons) {
	if(addButtons) {
	    $("#" + div).html("Loading");
	}
	control.load((gif)=>{
	    if(addButtons) {
		AnimatedGif.addButtons(control,div);
		//Listen for left/right arrow
		jqid(div).attr('tabindex','1');
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
	    html+=HU.div(["data-index",i,"style",HU.css("xfont-family","monospace","font-size","8pt", "display","inline-block","padding-right","1px","padding-left","1px","margin-right","1px"), "class","ramadda-hoverable ramadda-clickable gif-index"],(i+1));
	    if(cnt++>=30) {
		html+="<br>";
		cnt=0;
	    }
	}

	let buttons = 	$("#" + div).html(html).find(".gif-index");
	buttons.click(function() {
	    let idx = +$(this).attr("data-index");
	    control.move_to(idx);
	});

	control.add_frame_listener((index)=>{
	    buttons.each(function() {
		let idx = +$(this).attr("data-index");
		if(idx==index)
		    $(this).css("background","#ccc");
		else
		    $(this).css("background","transparent");
	    });
	});
	

    }
}


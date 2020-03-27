
var RamaddaJson = {
    allOpen:function(id) {
	$("#" + id + " .ramadda-json-button").each(function() {
            RamaddaJson.setVisible(id, $(this), "close");
	});
    },
    allClose:function(id) {
	$("#" + id + " .ramadda-json-button").each(function() {
            RamaddaJson.setVisible(id, $(this), "open");
	});
    },
    
    setVisible: function(id, button, state, all) {
	var block = button.next(".ramadda-json-block");
	var block = button.next().next();
	if (!state)
            state = block.attr("block-state");
	if (state == "close") {
            if (all) {
		block.find(".ramadda-json-button").each(function() {
                    RamaddaJson.setVisible(id, $(this), "close");
		});
            }
            state = "open";
            block.css("display", "block");
            button.attr("src", icon_tree_open);
	} else {
            if (all) {
		block.find(".ramadda-json-button").each(function() {
                    RamaddaJson.setVisible(id, $(this), "open");
		});
            }
            state = "close";
            button.attr("src", icon_tree_closed);
            block.css("display", "none");
	}
	block.attr("block-state", state);
    },

    init:function(id) {
	var img = HtmlUtils.image(icon_tree_open, ["class", "ramadda-json-button", "title", "shift-click: toggle all"]);
	var links = HtmlUtils.onClick("RamaddaJson.allOpen('" + id + "')", "All Open", []) +
            "&nbsp;&nbsp;" +
            HtmlUtils.onClick("RamaddaJson.allClose('" + id + "')", "All Close", [])
	$("#" + id).before(links);
	var block = $("#" + id + " .ramadda-json-block");
	block.prev(".ramadda-json-openbracket").before(img + " ");
	$("#" + id + " .ramadda-json-button").click(function(evt) {
            //           $(this).css("background","red");
            RamaddaJson.setVisible(id, $(this), null, evt.shiftKey);
	});
    }
}

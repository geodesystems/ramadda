
function setAcsField(id) {
    var ta = $("textarea[name*='edit.type_census_acs.fields']");
    var s = ta.val().trim();
    if(s!="") s = s+"\n";
    ta.val(s+id);
}

$(document).ready(function() {
    jqid('acs_get_text').keypress(function(e) {
        var list = jqid('acs_list');
        if(e.which == 0) {
            list.html("");
            return;
        }

        if(e.which != 13) {
            return;
        }
        if(e.preventDefault) {
            e.preventDefault();
        }
        var input = jqid('acs_get_text');
        var v = jqid('acs_get_text').val();

        if(!Utils.stringDefined(v)) {
            list.html("");
            //                    list.hide();
            return;
        }
        var url  = HU.url(RamaddaUtils.getUrl('/census/variables'),
			  ARG_OUTPUT,'json','text',v);
        list.show();
        if (false) {
            list.position({
                of: input,
                my: "left top",
                at: "left bottom+10",
                collision: "none none"
            });
            //And again to fix bug
            list.show();
            list.position({
                of: input,
                my: "left top",
                at: "left bottom+10",
                collision: "none none"
            });
        }
        
        list.html(HU.div([ATTR_CLASS,"ramadda-acs-list-inner ramadda-acs-list-msg"],
			       "Searching..."));
        var jqxhr = $.getJSON( url, function(data) {
            if(GuiUtils.isJsonError(data)) {
                GuiUtils.handleError("Error getting variables: " +url);
                return;
            }
            if(data.length ==0) {
                list.html(HU.div([ATTR_CLASS,"ramadda-acs-list-inner  ramadda-acs-list-msg"], "Nothing found"));
                return;
            }
            var html = HU.open(TAG_TABLE);
            var even = true;
            for(var i=0;i<data.length;i++) {
                var d = data[i];
                var rowClass = even?"ramadda-row-even":"ramadda-row-odd";
                even = !even;
                html += HU.openTag(TAG_TR,[ATTR_VALIGN,ALIGN_TOP,ATTR_CLASS,rowClass]);
                var id = d.id;
                id = HU.tag(TAG_A, [ATTR_ONCLICK, "setAcsField('" + d.id +"');"], id);
                var label = d.label;
                html += HU.td([],id) + HU.td([],"") + HU.td([], label +HU.br()+ d.concept);
                html += HU.close(TAG_TR);
            }
            html += HU.close(TAG_TABLE);;
            list.html(HU.div([ATTR_CLASS,"ramadda-acs-list-inner"], html));
        })
            .fail(function(jqxhr, textStatus, error) {
                var err = textStatus + ", " + error;
                GuiUtils.handleError("Error getting entry information: " +url);
                list.html(HU.div([ATTR_CLASS,"ramadda-acs-list-inner  ramadda-acs-list-msg"], "An error occurred:" + error));
            });



    });
});


function acsShowState() {

}


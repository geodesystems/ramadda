/**
Copyright 2008-2014 Geode Systems LLC
*/






function AreaWidget(display) {
    var ID_CONTAINS = "contains";
    var ID_NORTH = "north";
    var ID_SOUTH = "south";
    var ID_EAST = "east";
    var ID_WEST = "west";

    var ID_AREA_LINK = "arealink";

    RamaddaUtil.inherit(this, {
            display:display,
            getHtml: function() {
                var callback = this.display.getGet();
                //hack, hack
                var cbx = HtmlUtil.checkbox(this.display.getDomId(ID_CONTAINS),["title","Search mode: checked - contains, unchecked - overlaps"], false);
                var link = HtmlUtil.onClick(callback+".areaWidget.areaLinkClick();", HtmlUtil.image(root +(this.linkArea?"/icons/link.png":"/icons/link_break.png"),[ATTR_TITLE,"Set bounds from map", ATTR_CLASS, "display-area-link", "border","0",ATTR_ID, this.display.getDomId(ID_AREA_LINK)]));

                var mylocation = HtmlUtil.onClick(callback+".areaWidget.useMyLocation();", HtmlUtil.image(root +"/icons/compass.png"),[ATTR_TITLE,"Set my location", ATTR_CLASS, "display-area-link", "border","0"]);


                var erase = HtmlUtil.onClick(callback+".areaWidget.areaClear();", HtmlUtil.image(root +"/icons/eraser.png",[ATTR_TITLE,"Clear form", ATTR_CLASS, "display-area-link", "border","0"]));

                var areaForm = HtmlUtil.openTag(TAG_TABLE,[ATTR_CLASS,"display-area", "border","0","cellpadding","0","cellspacing","0"]);
                areaForm += HtmlUtil.tr([],
                                        HtmlUtil.td(["align","center"],
                                                    HtmlUtil.leftCenterRight(mylocation, 
                                                                             HtmlUtil.input(ID_NORTH,"",["placeholder","N",ATTR_CLASS,"input display-area-input", "size", "5",ATTR_ID, 
                                                                                                         this.display.getDomId(ID_NORTH),  ATTR_TITLE,"North"]), link,"20%","60%","20%")));

                areaForm += HtmlUtil.tr([],HtmlUtil.td([],
                                                       HtmlUtil.input(ID_WEST,"",["placeholder","W",ATTR_CLASS,"input  display-area-input", "size", "5",ATTR_ID, 
                                                                                  this.display.getDomId(ID_WEST),  ATTR_TITLE,"West"]) +
                                                       HtmlUtil.input(ID_EAST,"",["placeholder","E",ATTR_CLASS,"input  display-area-input", "size", "5",ATTR_ID, 
                                                                                  this.display.getDomId(ID_EAST),  ATTR_TITLE,"East"])));
                areaForm += HtmlUtil.tr([],
                                        HtmlUtil.td(["align","center"],
                                                    HtmlUtil.leftCenterRight(erase, HtmlUtil.input(ID_SOUTH,"",["placeholder","S",ATTR_CLASS,"input  display-area-input", "size", "5",ATTR_ID, 
                                                                                                             this.display.getDomId(ID_SOUTH),  ATTR_TITLE,"South"]),cbx)));
                                        
                areaForm += HtmlUtil.closeTag(TAG_TABLE);
                return areaForm;
            },
            areaClear: function() {
                $("#" + this.display.getDomId(ID_NORTH)).val("");
                $("#" + this.display.getDomId(ID_WEST)).val("");
                $("#" + this.display.getDomId(ID_SOUTH)).val("");
                $("#" + this.display.getDomId(ID_EAST)).val("");
                this.display.areaClear();
            },
            useMyLocation: function() {
                if (navigator.geolocation) {
                    var _this = this;
                    navigator.geolocation.getCurrentPosition(function(position){_this.setUseMyLocation(position);});
                } else {
                }
            },
            setUseMyLocation: function(position) {
                var lat = position.coords.latitude;
                var lon = position.coords.longitude;
                var offset = 5.0;
                if(this.display.myLocationOffset)
                    offset = parseFloat(this.display.myLocationOffset);
                    
                $("#" + this.display.getDomId(ID_NORTH)).val(lat+offset);
                $("#" + this.display.getDomId(ID_WEST)).val(lon-offset);
                $("#" + this.display.getDomId(ID_SOUTH)).val(lat-offset);
                $("#" + this.display.getDomId(ID_EAST)).val(lon + offset);
                if(this.display.submitSearchForm)
                    this.display.submitSearchForm();
            },
            areaLinkClick: function() {
                this.linkArea = !this.linkArea;
                var image = root +( this.linkArea? "/icons/link.png":"/icons/link_break.png");
                $("#" + this.display.getDomId(ID_AREA_LINK)).attr("src", image);
                if(this.linkArea && this.lastBounds) {
                    var b  = this.lastBounds;
                    $("#" + this.display.getDomId(ID_NORTH)).val(formatLocationValue(b.top));
                    $("#" + this.display.getDomId(ID_WEST)).val(formatLocationValue(b.left));
                    $("#" + this.display.getDomId(ID_SOUTH)).val(formatLocationValue(b.bottom));
                    $("#" + this.display.getDomId(ID_EAST)).val(formatLocationValue(b.right));
                }
            },
           linkArea: false,
           lastBounds: null,
           handleEventMapBoundsChanged: function (source,  args) {
                bounds = args.bounds;
                this.lastBounds = bounds;
                if(!args.force && !this.linkArea) return;
                $("#" + this.display.getDomId(ID_NORTH)).val(formatLocationValue(bounds.top));
                $("#" + this.display.getDomId(ID_WEST)).val(formatLocationValue(bounds.left));
                $("#" + this.display.getDomId(ID_SOUTH)).val(formatLocationValue(bounds.bottom));
                $("#" + this.display.getDomId(ID_EAST)).val(formatLocationValue(bounds.right));
            },
            setSearchSettings: function(settings) {
                var cbx =$("#" + this.display.getDomId(ID_CONTAINS));
                if(cbx.is(':checked')) {
                    settings.setAreaContains(true);
                } else {
                    settings.setAreaContains(false);
                }
                settings.setBounds(this.display.getFieldValue(this.display.getDomId(ID_NORTH), null),
                                   this.display.getFieldValue(this.display.getDomId(ID_WEST), null),
                                   this.display.getFieldValue(this.display.getDomId(ID_SOUTH), null),
                                   this.display.getFieldValue(this.display.getDomId(ID_EAST), null));
            },
                });
}




function DateRangeWidget(display) {
    var ID_DATE_START = "date_start";
    var ID_DATE_END = "date_end";

    RamaddaUtil.inherit(this, {
            display:display,
            initHtml: function() {
                this.display.jq(ID_DATE_START).datepicker();
                this.display.jq(ID_DATE_END).datepicker();
            },
            setSearchSettings: function(settings) {
                var start = this.display.jq(ID_DATE_START).val();
                var end = this.display.jq(ID_DATE_START).val();
                settings.setDateRange(start, end);
            },
            getHtml: function() {
                var html = HtmlUtil.input(ID_DATE_START,"",["placeholder","Start date", ATTR_ID,
                                                            display.getDomId(ID_DATE_START),"size","10"]) + " - " +
                    HtmlUtil.input(ID_DATE_END,"",["placeholder","End date", ATTR_ID,
                                                            display.getDomId(ID_DATE_END),"size","10"]);
                return html;
            }
        });
}



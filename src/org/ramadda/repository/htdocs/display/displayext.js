/**
   Copyright 2008-2024 Geode Systems LLC
*/


//uncomment this to add this type to the global list
//addGlobalDisplayType({type: "example", label:"Example"});


/*
  This gets created by the displayManager.createDisplay('example')
 */
function RamaddaExampleDisplay(displayManager, id, properties) {

    //Dom id for example
    //The displays use display.getDomId(ID_CLICK) to get a unique (based on the display id) id
    var ID_CLICK = "click";

    var ID_DATA = "data";

    //Create the base class
    RamaddaUtil.inherit(this, new RamaddaDisplay(displayManager, id, "example", properties));

    //Add this display to the list of global displays
    addRamaddaDisplay(this);

    //Define the methods
    RamaddaUtil.defineMembers(this, {
        //gets called by displaymanager after the displays are layed out
        initDisplay: function() {
            //Call base class to init menu, etc
            this.createUI();

            //I've been calling back to this display with the following
            //this returns "getRamaddaDisplay('" + this.getId() +"')";
            var get = this.getGet();
            var html = "<p>";
            html += HtmlUtils.onClick(get + ".click();", HtmlUtils.div([ATTR_ID, this.getDomId(ID_CLICK)], "Click me"));
            html += "<p>";
            html += HtmlUtils.div([ATTR_ID, this.getDomId(ID_DATA)], "");

            //Set the contents
            this.setContents(html);

            //Add the data
            this.updateUI();
        },
        //this tells the base display class to loadInitialData
        needsData: function() {
            return true;
        },
        //this gets called after the data has been loaded
        updateUI: function() {
            var pointData = this.getData();
            if (pointData == null) return;
            var recordFields = pointData.getRecordFields();
            var records = pointData.getRecords();
            var html = "";
            html += "#records:" + records.length;
            //equivalent to:
            //$("#" + this.getDomId(ID_DATA)).html(html);
            this.jq(ID_DATA).html(html);
        },
        //this gets called when an event source has selected a record
        handleEventRecordSelection: function(source, args) {
            //args: index, record, html
            //this.setContents(args.html);
        },
        click: function() {
            this.jq(ID_CLICK).html("Click again");
        }
    });
}

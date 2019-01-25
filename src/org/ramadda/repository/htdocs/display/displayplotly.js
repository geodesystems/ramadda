/**
Copyright 2008-2015 Geode Systems LLC
*/

var DISPLAY_PLOTLY_RADAR = "radar";

addGlobalDisplayType({type: DISPLAY_PLOTLY_RADAR, label:"Radar",requiresData:true,forUser:true,category:"Charts"});




function RamaddaRadarDisplay(displayManager, id, properties) {
    var SUPER;
    RamaddaUtil.inherit(this, SUPER  = new RamaddaFieldsDisplay(displayManager, id, DISPLAY_PLOTLY_RADAR, properties));

    //Dom id for example
    var ID_DATA = "data";

    this.foo  = "FOO";
    //Add this display to the list of global displays
    addRamaddaDisplay(this);

    //Define the methods
    RamaddaUtil.defineMembers(this, {
            //gets called by displaymanager after the displays are layed out
            initDisplay: function() {
                //Call base class to init menu, etc
                this.initUI();

                //I've been calling back to this display with the following
                //this returns "getRamaddaDisplay('" + this.getId() +"')";
                var get = this.getGet();
                var html =  "";
                html += HtmlUtil.div([ATTR_ID, this.getDomId(ID_DATA),"style","width:" + this.getProperty("width","400px")+";" +
                                      "height:" + this.getProperty("height","400px")+";"],"");

                //Set the contents
                this.setContents(html);

                //Add the data
                this.updateUI();
            },
            //this tells the base display class to loadInitialData
            needsData: function() {
                return true;
            },
            fieldSelectionChanged: function() {
                SUPER.fieldSelectionChanged.call(this);
                this.updateUI();
            },
            //this gets called after the data has been loaded
            updateUI: function() {
                var pointData = this.getData();
                if(pointData == null) return;
                var recordFields = pointData.getRecordFields();
                var selectedFields = this.getSelectedFields([]);
                var records = pointData.getRecords();
                records = this.filterData(records, recordFields);
                if(selectedFields.length == 0)
                    selectedFields = recordFields;
                var stringField = null;
                var numericFields = [];
                var rs= [];
                var mins = [];
                var maxs = [];
                var min = Number.MAX_VALUE;
                var max = Number.MIN_VALUE;

                for(a in selectedFields) {
                    var field = selectedFields[a];
                    if(stringField == null && field.getType() == "string") {
                        stringField = field;
                        continue;
                    }
                    if(field.isFieldNumeric()) {
                        numericFields.push(field);
                        rs.push([]);
                        mins.push(Number.MAX_VALUE);
                        maxs.push(Number.MIN_VALUE);
                    }
                }
                if(!stringField) {
                    this.jq(ID_DATA).html("No string field specified");
                    return;
                }
                if(numericFields.length==0) {
                    this.jq(ID_DATA).html("No numeric fields specified");
                    return;
                }

                var theta = [];
                for(var rowIdx=0;rowIdx<records.length;rowIdx++)  {
                    var record = records[rowIdx];
                    var row = record.getData();
                    var string = row[stringField.getIndex()];
                    theta.push(string);
                    for(var i=0;i<numericFields.length;i++) {
                        var field = numericFields[i];
                        var value =row[field.getIndex()]; 
                        rs[i].push(value);
                        mins[i] = Math.min(mins[i],value);
                        maxs[i] = Math.max(maxs[i],value);
                        min = Math.min(min,value);
                        max = Math.max(max,value);
                    }
                }

                var data = [];
                for(var i=0;i<numericFields.length;i++) {
                    var field = numericFields[i];
                    data.push({
                            type: 'scatterpolar',
                                r: rs[i],
                                theta: theta,
                                fill: 'toself',
                                name: field.getLabel()
                                });
                }
               layout = {
                   width:"100%",
                   height:"100%",
                   polar: {
                       radialaxis: {
                           visible: true,
                           range: [min, max]
                       }
                   },
               }
               Plotly.plot(this.getDomId(ID_DATA), data, layout)

            },
        });
}



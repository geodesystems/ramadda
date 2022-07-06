var ARG_ACTION_SEARCH = "action.search";
var TYPE_IMAGE = "type_image";
var TYPE_KMZ = "geo_kml";
var TYPE_NC = "cdm_grid";
var TYPE_CSV = "point_text";
var TYPE_TS = "type_single_point_grid_netcdf";

function CollectionForm(formId, plottype, args) {

    RamaddaUtil.defineMembers(this, {
        formId:formId,
        analysisUrl: ramaddaBaseUrl +"/model/" + plottype +"?"+args,
        plottype:plottype,
        services: [],
        init: function() {
            var collectionForm = this;
            for(var i=1;i<=2;i++) {
                collection  = 'collection' + i;
                this.initCollection(collection);
            }
            var theForm = this;
            var $submits = $("#"+this.formId).find( 'input[type=submit]' );
            var which_button;
            //Listen to the form
            $("#"+ this.formId).submit(function( event ) {
                if (null == which_button) {
                    which_button = $submits[0];
                }
                //if (which_button != ARG_ACTION_SEARCH && theForm.type === "compare" ) {
                if (which_button != ARG_ACTION_SEARCH) {
                    theForm.handleFormSubmission();
                    event.preventDefault();
                }
            });                

            //Listen to the buttons
            $submits.click( function(event) {
                which_button = $(this).attr("name");
            });

        },
        handleFormSubmission: function() {
            var url = this.analysisUrl;
            var theForm = this;
            var inputs = $('#' + this.formId +' :input');
            inputs.each(function() {             
                var value = $(this).val();
                if(this.name == "entryselect") {
                    if(!$(this).is(':checked')) {
                        return;
                    }
                }
                if(this.type == 'radio') {
                    if(!$(this).is(':checked')) {
                        return;
                    }
                }
                if(this.type == "checkbox") {
                    if(!$(this).is(':checked')) {
                        value = "false";
                    } 
                }
                if(typeof value === "string") {
                    value = [value];
                }
                for (var i = 0; i < value.length; i++) {
                    var s = value[i];
                    if(HtmlUtil.valueDefined(s)) {
                        url += "&" + this.name+ "=" + encodeURIComponent(s);
                    }
                }
            });       



            var doJson = true;
            var doImage = false;
            
            
            /* Show the URL to get the image */
            //if (!(this.plottype.indexOf("timeseries") >= 0)) {
            var imageUrl = url + "&returnimage=true";
            var base = window.location.protocol+ "//" + window.location.host;
            imageUrl = base + imageUrl;                        
            var urlDiv = $('#' + this.formId +"_url");
            if (urlDiv.length==0) {
                console.log("no output div");
            } else {
                urlDiv.html(HtmlUtil.div(["class", "padded"],
                     (HtmlUtil.toggleBlock("<b>URL to Generate Image</b><br/>",HtmlUtil.div(["class","ramadda-form-url"],  
                                                        HtmlUtil.href(imageUrl, 
                                                                  HtmlUtil.image(ramaddaBaseUrl +"/icons/link.png")) +" " + imageUrl)))));
            }
            //}

            if(doJson) {
                var jsonUrl =  url + "&returnjson=true";
                console.log("json url:" + jsonUrl);

                //Define a variable pointing to this object so we can reference it in the callback below
                var theCollectionForm  = this;

                //The Ramadda and EntryList classes are in repository/htdocs/entry.js
                //the global ramadda is the ramadda where this page came from
                var ramadda = getGlobalRamadda(); 

                //The EntryList below takes an object and calls the entryListChanged method 
                //when it gets the entries from the jsonUrl
                //Create the object that gets called back
                /*
                  var callbackObject = {
                  entryListChanged: function(entryList) {

                  //Get the list of entries from the EntryList
                  //There should just be one entry  -  the process folder
                  var entries = entryList.getEntries();
                  if(entries.length != 1) {
                  //console.log("Error: didn't get just one entry:" + entries.length);
                  return;
                  }

                  //This should be the process directory entry that you encoded into JSON
                  var processEntry = entries[0];

                  //Now, one more callback function (just a function, not an object) that will
                  //get called when the children entries are retrieved
                  let count = 10;
                  let finalCallback  = function(entries) {
                  if (entries.length == 0) {
                  console.log("CollectionForm: no entries found");
                  if (count--<0) { return; }
                  console.log("CollectionForm: calling search again");
                  //Wait 500 ms and try again
                  setTimeout(function(){
                  processEntry.getChildrenEntries(finalCallback, "ascending=false&orderby=name&max=9999");
                  },500);
                  return;
                  }
                  theCollectionForm.handleProcessEntries(processEntry, entries);
                  };
                  //This will go back to the server and get the children 
                  processEntry.getChildrenEntries(finalCallback, "ascending=false&orderby=name&max=9999");
                  }
                  };
                */
                var startTime = performance.now();
                //var startTime = Date.now();
                // clear out any old stuff
                var outputDiv = $('#' + this.formId +"_output");
                outputDiv.html("");

                // Make the status widget
                let statusDiv = $('#' + this.formId +"_status");
                //Add the cancel button and the output message area
                statusDiv.html("<div id=compare_message></div><div id=compare_cancel>Cancel</div>");
                //Make a cancel button 
                let cancelButton  = $("#compare_cancel").button();
                let message  = $("#compare_message");

                //A function to display the error
                let handleError = err=>{
                    err = HtmlUtils.makeErrorMessage(err);
                    statusDiv.html(err);
                };
                //Called to show status
                let handleStatus = msg=>{
                    //Comment this out if you don't want the fancy info message
                    msg = HtmlUtils.makeRunningMessage(msg);
                    //show the message
                    message.html(msg);
                };                              
                //Called when cancelled
                let handleCanceled = msg=>{
                    //Comment this out if you don't want the fancy info message
                    msg = HtmlUtils.makeInfoMessage(msg);
                    //show the message
                    statusDiv.html(msg);
                };
                //Called when done
                let handleFinished = msg=>{
                    statusDiv.html(msg);
                };

                handleStatus("Running...");

                //Don: Uncomment this if you want the server to just run through a test loop
                //jsonUrl+="&testit=true";

                //Post the request
                $.post(jsonUrl, data=>{
                    //        console.dir(data);
                    let actionId = data.actionid;
                    let statusUrl = ramaddaBaseUrl+"/status?output=json&actionid=" + actionId;
                    let running = true;
                    cancelButton.click(() =>{
                        let cancelUrl = ramaddaBaseUrl+"/status?output=json&cancel=true&actionid=" + actionId;
                        handleStatus("Cancelling comparison");
                        $.getJSON(cancelUrl, data=>{
                            running = false;
                            handleCanceled("Comparison canceled");
                        });
                    });
                    let monitorFunction  = ()=>{
                        //check the status
                        $.getJSON(statusUrl, data=>{
                            if(!running) return;
                            //console.dir("status:" + JSON.stringify(data));
                            if(data.status=="error") {
                                handleError(data.message);
                            } else  if(data.status=="complete") {
                                //Get the list of entries from the EntryList
                                //There should just be one entry  -  the process folder
                                var entries = createEntriesFromJson(JSON.parse(data.message));
                                if(entries.length != 1) {
                                    //console.log("Error: didn't get just one entry:" + entries.length);
                                    return;
                                }
                
                                //This should be the process directory entry that you encoded into JSON
                                var processEntry = entries[0];
                
                                //Now, one more callback function (just a function, not an object) that will
                                //get called when the children entries are retrieved
                                let count = 10;
                                let finalCallback  = function(entries) {
                                    if (entries.length == 0) {
                                        console.log("CollectionForm: no entries found");
                                        if (count--<0) { return; }
                                        console.log("CollectionForm: calling search again");
                                        //Wait 500 ms and try again
                                        setTimeout(function(){
                                            processEntry.getChildrenEntries(finalCallback, "ascending=false&orderby=name&max=9999");
                                        },500);
                                        return;
                                    }
                                    theCollectionForm.handleProcessEntries(processEntry, entries);
                                };
                                //This will go back to the server and get the children 
                                processEntry.getChildrenEntries(finalCallback, "ascending=false&orderby=name&max=9999");
                                doneTime = performance.now();
                                handleFinished("");
                                //handleFinished("Processing took: "+theCollectionForm.msToTime(doneTime-startTime));
                                console.log("Processing time took: "+Utils.formatNumber((doneTime-startTime)/60000)+" minutes");
                            } else {
                                handleStatus(data.message);
                            }
                            if(data.status=="running") {
                                //If we are still running then callback this function in 500 ms
                                setTimeout(monitorFunction,500)
                            }
                        }).fail(err=>{
                            handleError("Comparison failed:" + err);
                        });
                    };
                    //kick off the monitoring
                    monitorFunction();
                }).fail(err=>{
                    //console.dir(err);
                    handleError("Comparison failed:" + err);
                });
                //Just create the entry list, passing in the callback object
                //let entryList = new EntryList(ramadda, jsonUrl, callbackObject, true);
            }  else if (doImage) {
                //add the arg that gives us the image directly back then set the img src
                url += "&returnimage=true";
                var outputDiv = $('#' + this.formId +"_output");
                if(outputDiv.length==0) {
                    console.log("no output div");
                    return;
                }
                //Make the html with the image
                var html = HtmlUtil.image(url,[ATTR_ALT, "Generating Image..."])
                outputDiv.html(html);
            }
        },
        msToTime: function(milliseconds) {
            //Get hours from milliseconds
            var hours = milliseconds / (1000*60*60);
            var absoluteHours = Math.floor(hours);
            var h = absoluteHours > 9 ? absoluteHours : '0' + absoluteHours;
            
            //Get remainder from hours and convert to minutes
            var minutes = (hours - absoluteHours) * 60;
            var absoluteMinutes = Math.floor(minutes);
            var m = absoluteMinutes > 9 ? absoluteMinutes : '0' +  absoluteMinutes;
            
            //Get remainder from minutes and convert to seconds
            var seconds = (minutes - absoluteMinutes) * 60;
            var absoluteSeconds = Math.floor(seconds);
            var s = absoluteSeconds > 9 ? absoluteSeconds : '0' + absoluteSeconds;
            
            return h == "00" ? m + ' min ' + s + ' sec' : h + ' h ' + m + ' min ' + s + ' sec';
        },
        handleProcessEntries: function(parentProcessEntry, entries) {
            //console.log("got list of process entries:" + entries.length);

            //Look in htdocs/entry.js for the Entry class methods
            var html = "";
            var images = [];
            var kmz;
            var plotfiles = [];
            var tsfiles = [];
            var pdffiles = [];
            var zipentries = "";
            for(var i=0;i<entries.length;i++) {
                var entry = entries[i];
                //console.log(entry.toString() +", type: " + entry.getType().getId());
                var typeid = entry.getType().getId();
                if (typeid === TYPE_IMAGE) {
                    images.push(entry);
                } else if (typeid === TYPE_NC) {
                    plotfiles.push(entry);
                } else if (typeid === TYPE_KMZ) {
                    kmz = entry;
                } else if (typeid === TYPE_TS) {
                    tsfiles.push(entry);
                } else if (typeid === TYPE_CSV) {
                    plotfiles.push(entry);
                } else if (entry.getFilename().endsWith("pdfvalues.txt")) {
                    pdffiles.push(entry);
                } else if (entry.getFilename().endsWith(".csv")) {
                    tsfiles.push(entry);
                } else {
                    continue;
                }
                zipentries += "&" + HtmlUtil.urlArg("selentry",entry.getId());
                //zipentries += "&selentry=" + entry.getId();
            }
            html += this.outputImages(images);
            html += this.outputKMZ(kmz);
            if (this.plottype === "compare" || this.plottype === "test") {
                html += this.outputPlotFiles(plotfiles);
                //html += this.outputTimeSeriesFiles(tsfiles);
                html += HtmlUtil.href(
                    parentProcessEntry.getRamadda().getRoot() + 
            "/entry/getentries?output=zip.zipgroup&returnfilename=Climate_Model_Comparison" + 
            zipentries, "(Download All Files)");
            } else if (this.plottype === "enscompare") {
                html += this.outputPDFFiles(pdffiles);
            } else if (this.plottype === "multitimeseries") {
                html += this.outputTimeSeriesFiles(tsfiles);
            } else if (this.plottype === "multicompare") {
                html += this.outputPlotFiles(plotfiles);
                html += HtmlUtil.href(
                    parentProcessEntry.getRamadda().getRoot() + 
            "/entry/getentries?output=zip.zipgroup&returnfilename=Climate_Model_Comparison" + 
            zipentries, "(Download All Files)");
            }
            var outputDiv = $('#' + this.formId +"_output");
            if(outputDiv.length==0) {
                console.log("no output div");
            } else {
                outputDiv.html(html);
            }
            // Enable the image popup
            if (images.length > 0) {
                $(document).ready(function() {
                    HtmlUtils.createFancyBox(
                        $("a.popup_image"), {
                            'titleShow' : false
                        });
                });
            }
            // Show GE plugin if we have KMZ
            if (kmz != null) {
                var map3d1 = new RamaddaEarth('map3d1', 
                          location.protocol+"//"+location.hostname+":"+location.port+kmz.getResourceUrl(),
                          {showOverview:false});
            }
            // show the ts stuff
            if (tsfiles.length > 0) {
                /*
                  var displayManager = getOrCreateDisplayManager("manager1", {
                  "showMap": false,
                  "showMenu": false,
                  "showTitle": false,
                  "layoutType": "table",
                  "layoutColumns": 1,
                  "defaultMapLayer": "osm"
                  });
                  for (var i = 0; i<tsfiles.length; i++) {
                  var tsfile = tsfiles[i];
                  var pointDataProps = {
                  entryId: HtmlUtil.squote(tsfile.getId())
                  };
                  displayManager.createDisplay("linechart", {
                  "showMenu": false,
                  "showTitle": true,
                  "layoutHere": true,
                  "divid": "chart"+i,
                  "width": "500",
                  "height": "220",
                  "layouthere": "true",
                  "showmenu": "false",
                  "showtitle": "true",
                  "data": new PointData(tsfile.getName(), null, null, 
                  tsfile.getEntryUrl("&output=points.product&product=points.json&numpoints=1000"), 
                  pointDataProps)
                  });
                  }
                */
            }
            Utils.closeFormLoadingDialog();
        },
        outputImages: function(imageEntries) {
            if (imageEntries.length == 0) return "";
            var imagehtml = "";
            var gifs = [];
            var non_gifs = []
            for (var i = 0; i < imageEntries.length; i++) {
                var entry = imageEntries[i];
                var name = entry.getFilename();
                var suffix = name.substring(name.lastIndexOf(".")+1);
                if (suffix == "gif") {
                    gifs.push(entry);
                } else {
                    non_gifs.push(entry);
                }
            }
            for (var i = 0; i < gifs.length; i++) {
                var entry = gifs[i];
                var name = entry.getFilename();
                var prefix = name.substring(0,name.lastIndexOf("."));
                imagehtml += "<a class=\"popup_image\" href=\""+ entry.getResourceUrl()+"\">\n";
                imagehtml += HtmlUtil.image(entry.getResourceUrl(), ["width", "100%"])+"\n";
                imagehtml += "</a>\n";
                imagehtml += "<br/>\n";
                if (non_gifs.length == 0) {
                    imagehtml += "<p/>\n";
                    imagehtml += HtmlUtil.href(entry.getResourceUrl(), "Download Image");
                } else {
                    for (var j = 0; j < non_gifs.length; j++) {
                        var myentry = non_gifs[j];
                        var myname = myentry.getFilename();
                        var myprefix = myname.substring(0,myname.lastIndexOf("."));
                        var mysuffix = myname.substring(name.lastIndexOf(".")+1).toUpperCase();
                        if (mysuffix == "EPS") mysuffix = "Postscript";
                        if (myprefix == prefix) {
                            imagehtml += "<p/>\n";
                            imagehtml += HtmlUtil.href(myentry.getResourceUrl(), "Download "+mysuffix+" Image");
                            break;
                        }
                    }
                }
                imagehtml += "<p/>";
            }
            imagehtml += "<p/>";
            return imagehtml;
        },
        outputKMZ: function(entry) {
            kmzhtml = "";
            if (entry != null) {
                kmzhtml += "<div  id=\"map3d1\"  style=\"width:500px; height:500px;\"  class=\"ramadda-earth-container\" ><\/div>\n";
                kmzhtml += HtmlUtil.href(entry.getResourceUrl(), "Download Google Earth (KMZ) file");
                kmzhtml += "<p/>";
            }
            return kmzhtml;
        },
        outputPlotFiles: function(files) {
            if (files.length == 0) return "";
            var filehtml = ""
            filehtml += "<b>Files used for plots:</b><br/>";
            for (var i = 0; i < files.length; i++) {
                var entry = files[i];
                filehtml += entry.getResourceLink();
                filehtml += "<br/>";
            }
            filehtml += "<p/>";
            return filehtml;
        },
        outputTimeSeriesFiles: function(files) {
            if (files.length == 0) return "";
            var filehtml = ""
            /*
              filehtml += "<div id=\"manager1\"></div>"
              for (var i = 0; i < files.length; i++) {
              filehtml += "<div id=\"chart"+i+"\"></div>"
              }
            */
            filehtml += "<b>Timeseries values:</b><br/>";
            for (var i = 0; i < files.length; i++) {
                var entry = files[i];
                filehtml += entry.getResourceLink();
                filehtml += "<br/>";
            }
            filehtml += "<p/>";
            return filehtml;
        },
        outputPDFFiles: function(files) {
            if (files.length == 0) return "";
            var filehtml = ""
            filehtml += "<b>Files used for plot:</b><br/>";
            for (var i = 0; i < files.length; i++) {
                var entry = files[i];
                if (entry.getFilename().endsWith(".txt")) {
                    //filehtml += "PDF values: ";
                    filehtml += entry.getResourceLink();
                    filehtml += "<br/>";
                    /*
                      } else if (entry.getFilename().endsWith(".r")) {
                      //filehtml += "R Script: ";
                      filehtml += entry.getResourceLink();
                      filehtml += "<br/>";
                    */
                } else {
                    continue;
                }
            }
            return filehtml;
        },
        initCollection: function(collection) {
            var collectionForm = this;
            this.getCollectionSelect(collection).change(function(event) {
                return collectionForm.collectionChanged(collection);
            });
            for(var fieldIdx=0;fieldIdx<10;fieldIdx++) {
                this.initField(collection, fieldIdx);
            }
            var t = this.getCollectionSelect(collection);
            //        alert('t:' + t.length);
            var collectionId  =  t.val();
            
            //If they had one selected 
            if(collectionId != null && collectionId !== "") {
                //            alert("updating fields:" + collectionId);
                this.updateFields(collection,  collectionId, 0, true);
            }
        },
        initField:function(collection, fieldIdx) {
        let func = (event)=> {
                return this.fieldChanged(collection, fieldIdx);
        };
        //jeffmc: handle the multi checkboxes
        if(this.isFieldMultiple(collection, fieldIdx)) {
        this.getMultipleCheckboxes(collection, fieldIdx).change(func);
        } else {
        this.getFieldSelect(collection, fieldIdx).change(func);
        }
        },
        addService: function(service) {
            this.services.push(service);
        },
        //Gets called when the collection select widget is changed
        collectionChanged: function  (collection, selectId) {

            var collectionId = this.getCollectionSelect(collection).val();
            var fieldIdx = 0;
            if(!collectionId || collectionId == "") {
                this.clearFields(collection, fieldIdx);
                return false;
            }
            this.updateFields(collection,  collectionId, fieldIdx, false);
            return false;

        },
        //Get the list of metadata values for the given field and collection
        updateFields:function(collection, collectionId, fieldIdx, fromInit) {
            var url = this.analysisUrl +"&json=test&thecollection=" + collectionId+"&field=" + fieldIdx;
            //Assemble the other field values up to the currently selected field
            for(var i=0;i<fieldIdx;i++) {
                let values = this.getFieldValues(collection, i);
                if (Utils.isDefined(values)) {
                    for (var j = 0; j < values.length; j++) {
                        var s = values[j];
                        if(HtmlUtil.valueDefined(s)) {
                            url += "&field" + i + "=" + encodeURIComponent(s);
                        }
                    }
                }
            }

            var collectionForm = this;
            $.getJSON(url, function(data) {
                var currentValueIsInNewList = collectionForm.setFieldValues(collection, data, fieldIdx);
                var hasNextField =  collectionForm.hasField(collection, fieldIdx+1);
                var nextFieldIndex = fieldIdx+1;
                if(hasNextField) {
                    if(currentValueIsInNewList)  {
                        collectionForm.updateFields(collection, collectionId, nextFieldIndex, true); 
                    } else {
                        collectionForm.clearFields(collection, nextFieldIndex);
                    }
                }
            });

        },
        //Clear the field selects starting at start idx
        clearFields: function(collection, startIdx) {
            for(var idx=startIdx;idx<10;idx++) {
        //jeffmc: if its the multi checkboxes then just add some filler
        if(this.isFieldMultiple(collection, idx)) {
                    this.getFieldSelect(collection, idx).html("&nbsp;");
        } else {
                    this.getFieldSelect(collection, idx).html("<option value=''>--</option>");
        }
            }
        },
        //jeffmc: returns whether the field is a multiple checkbox
        isFieldMultiple:function(collection,fieldIdx) {
            let fieldSelect = this.getFieldSelect(collection, fieldIdx);
                return fieldSelect.attr('multiple');
        },
        //jeffmc: get the multiple checkboxes
        getMultipleCheckboxes:function(collection, fieldIdx,debug) {
            let fieldSelect = this.getFieldSelect(collection, fieldIdx);
            //First check if there are the cbxes with the class ramadda-toggle set
            //We look for the class as there is also the toggle all checkbox
            let checkboxes =fieldSelect.find(".ramadda-toggle");
            if(checkboxes.length==0) {
            //If not set then these might be checkboxes created by the server. If that is the case then
            //look for for all of the checkboxes
            checkboxes =fieldSelect.find(":checkbox");
            }
            return checkboxes;
        },
        //jeffmc: this gets the field values and handles the checkboxes for the multiple select
        getFieldValues:function(collection, fieldIdx,debug) {
                let isMultiple = this.isFieldMultiple(collection, fieldIdx);
            if(isMultiple) {
            let values = [];
            let checkboxes = this.getMultipleCheckboxes(collection, fieldIdx,debug);
            if(debug) console.dir("getFieldValues:" + fieldIdx +" #checkboxes:" + checkboxes.length);
            checkboxes.each(function() {
                if(debug) console.dir("\t" +$(this).attr("value") +" checked:" + $(this).is(':checked'));
                if($(this).is(':checked')) {
                values.push($(this).val());
                }
            });
            return values;
            }
            let fieldSelect = this.getFieldSelect(collection, fieldIdx);
                let value = fieldSelect.val();
                if(typeof value === 'string') {
                    value = [value];
                }
            return value;
        },
    
        //Get the select object for the given field
        getFieldSelect: function(collection, fieldIdx) {
            return  $('#' + this.getFieldSelectId(collection, fieldIdx));
        },
        hasField: function(collection, fieldIdx) {
            return  this.getFieldSelect(collection, fieldIdx).length>0;
        },
        //Get the selected entry id
        getSelectedCollectionId: function(collection) {
            var t = this.getCollectionSelect(collection);
            return t.val();
        },
        //Get the collection selector 
        getCollectionSelect: function(collection) {
            return  $('#' + this.getCollectionSelectId(collection));
        },
        //This matches up with ClimateModelApiHandler.getFieldSelectId
        getFieldSelectId: function(collection, fieldIdx) {
            return  this.getCollectionSelectId(collection) + "_field" + fieldIdx;
        },
        //dom id of the collection select widget
        //This matches up with ClimateModelApiHandler.getCollectionSelectId
        getCollectionSelectId: function(collection) {
            return  this.formId +"_"  + collection;
        },
        setFieldValues: function(collection, data, fieldIdx) {
            if (data == null) return false;
            let currentValue =    this.getFieldValues(collection, fieldIdx);
            let html = "";
            let select =  this.getFieldSelect(collection, fieldIdx);
            let isMultiple = this.isFieldMultiple(collection, fieldIdx);
            let currentValueIsInNewList = false;
            let onlyOneItem = false;
            let haveBlank = false;
            let numItems = data.length;
            for(let i=0;i<data.length;i++)  {
                let objIQ = data[i];
                let value,label;
                let type = typeof(objIQ);
                if (type == 'object') {  
                    // made from TwoFacedObject [ {id:id1,value:value1}, {id:id2,value:value2} ]
                    value = objIQ.id;
                    label = objIQ.label;
                } else {
                    value = objIQ;
                    label  = value;
                }
                if(label == "") {
                    label =  "--";
                    haveBlank = true;
                }
                if (value == "sprd" || value == "clim") {
                    numItems--;
                    continue;
                }
                let extra = "";
                let isSelected = false;
                if (Utils.isDefined(currentValue)) {
                    for (let j = 0; j < currentValue.length; j++) {
                        let s = currentValue[j];
                        if (s == value) {
                            extra = " selected ";
                            currentValueIsInNewList = true;
                            isSelected = true;
                        }
                    }
                }
                // Hack to automatically select the item if there is only one
                if (haveBlank && numItems == 2 && i > 0) {
                    extra = " selected ";
                    currentValueIsInNewList = true;
                    isSelected = true;
                }
              
                if (isMultiple) {

                    //jeffmc: add in the select all. set the class to ramadda-toggleall so we can find this later
                    let checkboxName=select.attr("checkboxname")||"";
                    if(html=="") {
                        html += HtmlUtils.div([],HtmlUtils.checkbox("", ['class','ramadda-toggleall','title','Select All'], isSelected, 'Select All'));
                    }
    
                    //jeffmc: don't show the blank value. set the class to ramadda-toggle so we can find this later
                    if(value!="") {
                        html += HtmlUtils.div([],HtmlUtils.checkbox(value, ['name',checkboxName,'value',value,'index',i,'class','ramadda-toggle'], isSelected, label));
                    }

                } else {
                    html += "<option value=\'"+value+"\'   " + extra +" >" + label +"</option>";
                }
            }
            //Check if the select has been selectBox'ed
            //let select =  this.getFieldSelect(collection, fieldIdx);
            let selectBoxData = select.data("selectBox-selectBoxIt");
            if(selectBoxData !=null) {
                selectBoxData.remove();
                selectBoxData.add(html);
            } else {
                select.html(html);
            }
            //jeffmc: handle initializing the multiples
            if(isMultiple) {
                //Initialize these fields
                this.initField(collection, fieldIdx);

                //find all of the regular checkboxes
                let checkboxes = this.getMultipleCheckboxes(collection, fieldIdx);
                //find the toggle all cbx and on click set the other checkboxes
                let collectionForm = this;
                select.find(".ramadda-toggleall").click(function(){
                    let checked = $(this).is(':checked');
                    checkboxes.prop("checked",checked);
                    //Signal the change. We use the
                    collectionForm.fieldChanged(collection, fieldIdx);
                });
                //listen for clicks on the checkboxes
                //this supports doing a click on one and then a shift-click on another - (de)selecting all of the
                //checkboxes within the range
                //note: this does not work on firefox when shift-clicking on the checkbox label
                //https://bugzilla.mozilla.org/show_bug.cgi?id=559506
                select.find(".ramadda-toggle").click(function(event){
                    //get the index of the checkbox
                    let index = $(this).attr("index");
                    if(!event.shiftKey) {
                        select.attr("lastcbxclick",index);
                    }
                    let checked = $(this).is(':checked');
                    let lastIndex = select.attr("lastcbxclick");
                    if(!Utils.isDefined(lastIndex)) {
                        select.attr("lastcbxclick",index);
                        return;
                    }
                    if(lastIndex == index) {
                        return;
                    }
                    checkboxes.each(function() {
                        let thisIndex = $(this).attr("index");
                        if((thisIndex>=index && thisIndex<=lastIndex) ||
                           (thisIndex<=index && thisIndex>=lastIndex)) {
                            $(this).prop("checked",checked);
                        }
                    });
                });         
            }
            return currentValueIsInNewList;
        },
        fieldChanged: function (collection, fieldIdx) {
            let values = this.getFieldValues(collection, fieldIdx);
            if(!Utils.isDefined(values)) {
                this.clearFields(collection, fieldIdx+1);
                return;
            }
            this.updateFields(collection, this.getSelectedCollectionId(collection),  fieldIdx+1);
        }
      });
    this.init();
    
    }


function RamaddaService(formId) {
    RamaddaUtil.defineMembers(this, {
        formId:formId
    });

}


var  ARG_CDO_PREFIX = "cdo_";
var ARG_CDO_OPERATION = ARG_CDO_PREFIX+ "operation";
var ARG_CDO_STARTMONTH = ARG_CDO_PREFIX  + "startmonth";
var ARG_CDO_ENDMONTH = ARG_CDO_PREFIX + "endmonth";
var ARG_CDO_MONTHS = ARG_CDO_PREFIX + "months";
var ARG_CDO_STARTYEAR = ARG_CDO_PREFIX + "startyear";
var ARG_CDO_ENDYEAR = ARG_CDO_PREFIX + "endyear";
var ARG_CDO_YEARS = ARG_CDO_PREFIX + "years";
var ARG_CDO_PARAM = ARG_CDO_PREFIX + "param";
var ARG_CDO_LEVEL = ARG_CDO_PREFIX + "level";
var ARG_CDO_STAT = ARG_CDO_PREFIX + "stat";
var ARG_CDO_FROMDATE = ARG_CDO_PREFIX + "fromdate";
var ARG_CDO_TODATE = ARG_CDO_PREFIX + "todate";
var ARG_CDO_PERIOD = ARG_CDO_PREFIX + "period";
var ARG_CDO_AREA = ARG_CDO_PREFIX + "area";
var ARG_CDO_AREA_NORTH = ARG_CDO_AREA + "_north";
var ARG_CDO_AREA_SOUTH = ARG_CDO_AREA + "_south";
var ARG_CDO_AREA_EAST = ARG_CDO_AREA + "_east";
var ARG_CDO_AREA_WEST = ARG_CDO_AREA + "_west";



function CDOTimeSeriesService(formId) {
    var SUPER;
    RamaddaUtil.inherit(this, SUPER = new RamaddaService(formId));
}

function CDOArealStatisticsService(formId) {
    var SUPER;
    RamaddaUtil.inherit(this, SUPER = new RamaddaService(formId));
}

function NCLModelPlotService(formId) {
    var SUPER;
    RamaddaUtil.inherit(this, SUPER = new RamaddaService(formId));
}

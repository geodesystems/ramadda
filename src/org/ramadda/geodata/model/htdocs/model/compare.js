

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
                            var finalCallback  = function(entries) {
                                theCollectionForm.handleProcessEntries(processEntry, entries);
                            };

                            //This will go back to the server and get the children 
                            processEntry.getChildrenEntries(finalCallback, "ascending=false&orderby=name&max=9999");
                            
                        }
                    };
                    //Just create the entry list, passing in the callback object
                    var entryList = new EntryList(ramadda, jsonUrl, callbackObject, true);
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
                    } else if (entry.toString().indexOf("pdf") >= 0) {
                        pdffiles.push(entry);
                    } else {
                        continue;
                    }
                    zipentries += "&" + HtmlUtil.urlArg("selentry",entry.getId());
                    //zipentries += "&selentry=" + entry.getId();
                }
                html += this.outputImages(images);
                html += this.outputKMZ(kmz);
                if (this.plottype === "compare") {
                  html += this.outputPlotFiles(plotfiles);
                  //html += this.outputTimeSeriesFiles(tsfiles);
                  html += HtmlUtil.href(
                       parentProcessEntry.getRamadda().getRoot() + 
                       "/entry/getentries?output=zip.zipgroup&returnfilename=Climate_Model_Comparison" + 
                       zipentries, "(Download All Files)");
                }
                if (this.plottype === "enscompare") {
                  html += this.outputPDFFiles(pdffiles);
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
                      $("a.popup_image").fancybox({
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
                closeFormLoadingDialog();
            },
            outputImages: function(imageEntries) {
                if (imageEntries.length == 0) return "";
                var imagehtml = "";
                for (var i = 0; i < imageEntries.length; i++) {
                    var entry = imageEntries[i];
                    imagehtml += "<a class=\"popup_image\" href=\""+ entry.getResourceUrl()+"\">\n";
                    imagehtml += HtmlUtil.image(entry.getResourceUrl(), ["width", "100%"])+"\n";
                    imagehtml += "</a>\n";
                    imagehtml += "<br/>\n";
                    imagehtml += HtmlUtil.href(entry.getResourceUrl(), "Download image");
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
                filehtml += "<b>Files used for timeseries:</b><br/>";
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
                    if (entry.toString().indexOf(".txt") >= 0) {
                      //filehtml += "PDF values: ";
                      filehtml += entry.getResourceLink();
                      filehtml += "<br/>";
                      /*
                    } else if (entry.toString().endsWith(".r")) {
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
                var collectionForm = this;
                this.getFieldSelect(collection, fieldIdx).change(function(event) {
                        return collectionForm.fieldChanged(collection, fieldIdx);
                    });
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
                    var value = this.getFieldSelect(collection, i).val();
                    if(typeof value === 'string') {
                       value = [value];
                    }
                    if (!(value === undefined || value == null)) {
                        for (var j = 0; j < value.length; j++) {
                           var s = value[j];
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
                    this.getFieldSelect(collection, idx).html("<option value=''>--</option>");
                }
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
                var currentValue =    this.getFieldSelect(collection, fieldIdx).val();
                if (typeof currentValue === "string") {
                   currentValue = [ currentValue];
                }
                var currentValueIsInNewList = false;
                var html = "";
                var select =  this.getFieldSelect(collection, fieldIdx);
                for(var i=0;i<data.length;i++)  {
                    var objIQ = data[i];
                    var value,label;
                    var type = typeof(objIQ);
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
                    }
                    if (value == "sprd" || value == "clim") continue;
                    var extra = "";
                    // if multiple is available for ensemble members, select them all
                    if (fieldIdx == 2 && select.prop('multiple')) {
                        extra = " selected ";
                        currentValueIsInNewList = true;
                    } else
                    if (!(currentValue === undefined || currentValue == null)) {
                        for (var j = 0; j < currentValue.length; j++) {
                            var s = currentValue[j];
                            if (s == value) {
                                extra = " selected ";
                                currentValueIsInNewList = true;
                            }
                        }
                    }
                    //if(currentValue == value) {
                    //    extra = " selected ";
                    //    currentValueIsInNewList = true;
                    //}
                    html += "<option value=\'"+value+"\'   " + extra +" >" + label +"</option>";
                }
                //Check if the select has been selectBox'ed
                //var select =  this.getFieldSelect(collection, fieldIdx);
                var selectBoxData = select.data("selectBox-selectBoxIt");
                if(selectBoxData !=null) {
                    selectBoxData.remove();
                    selectBoxData.add(html);
                } else {
                    select.html(html);
                }
                return currentValueIsInNewList;
            },
            fieldChanged: function (collection, fieldIdx) {
                var value = this.getFieldSelect(collection, fieldIdx).val();
                if(value == null || value == "") {
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

/**
   Copyright (c) 2008-2025 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/


function SelectForm(formId, entryId, arg, outputDiv, selectValues) {
    this.id = formId;
    this.entryId = entryId;
    this.arg = arg;
    this.outputDivPrefix = outputDiv;
    this.selectValues = selectValues;
    this.totalSize = 0;
    this.checkboxPrefix = "entry_" + this.id + "_";
    if (!this.arg) this.arg = "select";

    this.clearSelect = function(num) {
        for (var i = num; i < 10; i++) {
            select = this.getSelect(i);
            if (select.length == 0) break;
            select.html("<select><option value=''>--</option></select>");
        }
    }

    this.valueDefined = function(value) {
        if (value != "" && value.indexOf("--") != 0) {
            return true;
        }
        return false;
    }

    this.getUrl = function(what) {
        var url = ramaddaBaseUrl + "/entry/show?entryid=" + this.entryId;
        var theForm = this;
        var inputs = $('#' + this.id + ' :input');
        //        $(':input[id*=\"' + this.id +'\"]')
        inputs.each(function() {
            //if(this.name == "entryselect" && !this.attr('checked')) {
            var value = $(this).val();
            if (this.name == "request") {
		return;
	    }

            if (this.name == "entryselect") {
                if (!$(this).is(':checked')) {
                    return;
                }
            }
            //A hack for now but 
            if (this.type == 'radio') {
                if (!$(this).is(':checked')) {
                    return;
                }
            }

            if (theForm.valueDefined(value)) {
                url += "&" + this.name + "=" + encodeURIComponent(value);
            }
        });
        if (what != null) {
            url += "&request=" + what;
        }
        return url;
    }

    //This sets the request input to what and submits the form
    this.formSubmit = function(what, event) {
	$("#"+this.id).find("input[name=request]").val(what);
	document.getElementById(this.id).submit();
	if(event)event.preventDefault();
    }

    this.bulkdownload = function(event) {
	this.formSubmit("bulkdownload",event);
    }

    this.download = function(event) {
	this.formSubmit("download",event);
    }

    this.makeKMZ = function(event) {
	this.formSubmit("kmz",event);
    }

    this.threddscatalog = function(event) {
	this.formSubmit("threddscatalog",event);
    }


    this.makeTimeSeries = function(event) {
        var result = "";
        var url = this.getUrl("timeseries");
        var theForm = this;
        $("#" + this.outputDivPrefix + "image").html("<img alt=\"Generating Image....\" src=\"" + url + "\">");
        return false;
    }


    this.search = function(event) {
        var result = "";
        var url = this.getUrl("search");
        var theForm = this;
        $("#" + this.outputDivPrefix + "list").html("<img src=" + icon_progress + "> Searching...");
        $("#" + this.outputDivPrefix + "image").html("");
        theForm.totalSize = 0;
        $.getJSON(url, function(data) {
            theForm.processEntryJson(data);
        });

        return false;
    }


    this.makeImage = function(event) {
        var result = "";
        var url = this.getUrl("image");
        var theForm = this;
        //        $("#" + this.outputDivPrefix+"image").html("<img src=" + icon_progress +"> Creating image");
        $("#" + this.outputDivPrefix + "image").html("<img alt=\"Generating Image....\" src=\"" + url + "\">");
        //        theForm.totalSize = 0;
        return false;
    }


    this.processEntryJson = function(data) {
        var totalSize = 0;
        var html = "";
        var tableId = HtmlUtils.getUniqueId("");
        if (data.length == 0) {
            html = "Nothing found";
        } else {
            var listHtml = "";
            var header = "";
            var columnNames = null;
            var entries = createEntriesFromJson(data);
            for (var i = 0; i < entries.length; i++) {
                var entry = entries[i];
                var labels = entry.getAttributeLabels();
                if (i == 0) {
                    columnNames = entry.getAttributeNames();
                    for (var colIdx = 0; colIdx < labels.length; colIdx++) {
                        //A terrible hack to not show the collection id
                        if(labels[colIdx] == "Collection ID") continue;
                        header += "<th><div class=selectform-table-header>" + labels[colIdx] + "</div></th>";
                    }
                }

                listHtml += "<tr>";
                listHtml += "<td style='padding-left:4px;padding-right:4px;' width=1><input name=\"entryselect\" type=checkbox checked value=\"" + entry.getId() + "\" id=\"" +
                    this.checkboxPrefix +
                    +entry.getId() + "\" ></td><td>";
                listHtml += "&nbsp;" + entry.getLink(entry.getIconImage() + " " + entry.getName());

                for (var colIdx = 0; colIdx < columnNames.length; colIdx++) {
		            //A terrible hack to not show the collection id
		            if(labels[colIdx] == "Collection ID") continue;
                    var value = entry.getAttributeValue(columnNames[colIdx]);
                    listHtml += "<td>" + HtmlUtils.div(["class","selectform-table-entry"], value) + "</td>";
                }

                listHtml += "</td><td align=right>";
                listHtml += entry.getFormattedFilesize();
                listHtml += "</td>";
      
                totalSize += entry.getFilesize();
                listHtml += "</tr>";
            }

	    var table = HtmlUtils.openTag("table", ["border",0,"class", "selectform-table stripe rowborder ramadda-table", "id", tableId]);
            var checkboxId = this.id + "_listcbx";
            header = "<tr><th style='padding-left:4px;padding-right:4px;' width=1><input type=checkbox checked value=true id=\"" + checkboxId + "\"\></th><th> " +
		"<b>" + data.length + " files found</b></th>" + header + "<th align=right><b>Size</b></td></tr>";

            table += HtmlUtils.openTag("thead", []);
  	    table += header;
            table += HtmlUtils.closeTag("thead");
            table += HtmlUtils.openTag("tbody", []);
	    table += listHtml;
            table += HtmlUtils.closeTag("tbody");
	    table += HtmlUtils.closeTag("table");
	    html += table;
            html +=  HtmlUtils.leftRight("",GuiUtils.size_format(totalSize));
	}
        this.totalSize = totalSize;
        $("#" + this.outputDivPrefix + "list").html(html);

        HtmlUtils.formatTable("#"+ tableId,{
            scrollY: "250",
            ordering:true,
        });
        var theForm = this;
        var cbx = $("#" + checkboxId);

        this.getEntryCheckboxes().change(function(event) {
            theForm.listUpdated();
        });


        cbx.change(function(event) {
            let value = cbx.is(':checked');
            theForm.getEntryCheckboxes().attr("checked", value);
            theForm.listUpdated();
	    event.stopPropagation();

        });
        this.listUpdated()
    }                


    this.getEntryCheckboxes = function() {
        return $(':input[id*=\"' + this.checkboxPrefix + '\"]');
    }


    this.listUpdated = function() {
        var cbxs = this.getEntryCheckboxes();
        var hasSelectedEntries = false;
        cbxs.each(function(index) {
            if ($(this).attr("checked")) {
                hasSelectedEntries = true;
            }
        });

        //        this.totalSize
        var btns = $(':input[id*=\"' + this.id + '_do_\"]');
        if (hasSelectedEntries) {
            //btns.removeAttr('disabled').removeClass( 'ui-state-disabled' );
            btns.show();
        } else {
            //btns.attr('disabled', 'disabled' ).addClass( 'ui-state-disabled' );
            btns.hide();
        }
    }

    this.isSelectLinked = function() {
        return false;
    }

    this.select = function(num) {
        if (!this.isSelectLinked()) {
            return;
        }

        this.narrowSelect();
        return;
        num = parseInt(num);
        select = this.getSelect(num);
        if (select.val() == "" || select.val().indexOf("--") == 0) {
            this.clearSelect(num + 1);
            return false;
        }

        var nextIdx = num + 1;
        var url = this.getUrl("metadata");
        this.applyToSelect(url, nextIdx);
        return false;
    }


    this.narrowSelect = function() {
        var args = "";
        for (var i = 0; i < 10; i++) {
            select = this.getSelect(i);
            if (select.length == 0) break;
            var value = select.val();
            if (this.valueDefined(value)) {
                args += "&" + this.arg + i + "=" + encodeURIComponent(value);
            }
        }

        var url = this.getUrl("metadata");
        for (var i = 0; i < 10; i++) {
            select = this.getSelect(i);
            if (select.length == 0) break;
            var value = select.val();
            if (!this.valueDefined(value)) {
                this.applyToSelect(url + "&field=" + this.arg + i, i);
            }
        }
    }



    this.applyToSelect = function(url, index) {
        var theForm = this;
        $.getJSON(url, function(data) {
            if (!data.values) {
                alert('error');
                return;
            }
            var html = "<select>";
            for (i = 0; i < data.values.length; i++) {
                var value = data.values[i];
                var label = value;
                var colonIdx = value.indexOf(":");
                if (colonIdx >= 0) {
                    label = value.substring(colonIdx + 1);
                    value = value.substring(0, colonIdx);
                    //                        alert("label:" + label +" value:"  + value); 
                }
                if (value.indexOf("--") == 0) {
                    value = "";
                }
                html += "<option value=\'" + value + "\'>" + label + "</option>";
            }
            html += "</select>";
            var nextSelect = theForm.getSelect(index);
            var currentValue = nextSelect.val();
            nextSelect.html(html);
            nextSelect.focus();
            if (currentValue) {
                nextSelect.val(currentValue);
            }
            theForm.clearSelect(index + 1);
        });

    }

    this.getSelect = function(i) {
        return $('#' + this.id + '_' + this.arg + i);
    }

    this.submit = function() {
        var valueField = $('#' + this.id + '_value');
        var image = $('#' + this.id + '_image');
        image.attr("src", ramaddaBaseUrl + "/icons/" + valueField.val());
        return false;
    }

    this.listUpdated();


}

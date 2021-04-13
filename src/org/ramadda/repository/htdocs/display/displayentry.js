/**
Copyright 2008-2019 Geode Systems LLC
*/


let DISPLAY_ENTRYLIST = "entrylist";
let DISPLAY_TESTLIST = "testlist";
let DISPLAY_ENTRYDISPLAY = "entrydisplay";
let DISPLAY_ENTRY_GALLERY = "entrygallery";
let DISPLAY_ENTRY_GRID = "entrygrid";
let DISPLAY_OPERANDS = "operands";
let DISPLAY_METADATA = "metadata";
let DISPLAY_ENTRYTIMELINE = "entrytimeline";
let DISPLAY_REPOSITORIES = "repositories";
let DISPLAY_ENTRYTITLE = "entrytitle";
let DISPLAY_SEARCH  = "search";
let DISPLAY_SIMPLESEARCH  = "simplesearch";
let ID_RESULTS = "results";
let ID_SEARCH_HEADER = "searchheader";
let ID_SEARCH_BAR = "searchbar";
let ID_SEARCH_TAG = "searchtag";
let ID_SEARCH_TAG_GROUP = "searchtaggroup";
let ID_ENTRIES = "entries";
let ID_DETAILS_INNER = "detailsinner";
let ID_DETAILS_ANCESTORS = "detailsancestors";
let ID_DETAILS_TAGS= "detailstags";
let ID_PROVIDERS = "providers";
let ID_SEARCH_ORDERBY = "orderby";
let ID_SEARCH_SETTINGS = "searchsettings";
let ID_TREE_LINK = "treelink";
let ATTR_ENTRYID = "entryid";

let ID_SEARCH = "search";
let ID_FORM = "form";
let ID_TEXT_FIELD = "textfield";
let ID_TYPE_FIELD = "typefield";
let ID_TYPE_DIV = "typediv";
let ID_TYPEFIELDS = "typefields";
let ID_METADATA_FIELD = "metadatafield";
let ID_COLUMN = "column";




addGlobalDisplayType({
    type: DISPLAY_ENTRYLIST,
    label: "Entry List",
    requiresData: false,
    category: CATEGORY_ENTRIES
});
addGlobalDisplayType({
    type: DISPLAY_SEARCH,
    label: "Entry Search",
    requiresData: false,
    category: CATEGORY_ENTRIES
});
/*
addGlobalDisplayType({
    type: DISPLAY_TESTLIST,
    label: "Test  List",
    requiresData: false,
    category: CATEGORY_ENTRIES
});
*/
addGlobalDisplayType({
    type: DISPLAY_ENTRYDISPLAY,
    label: "Entry Display",
    requiresData: false,
    category: CATEGORY_ENTRIES
});
addGlobalDisplayType({
    type: DISPLAY_ENTRYTITLE,
    label: "Entry Title",
    requiresData: false,
    category: CATEGORY_ENTRIES
});
addGlobalDisplayType({
    type: DISPLAY_ENTRY_GALLERY,
    label: "Entry Gallery",
    requiresData: false,
    category: CATEGORY_ENTRIES
});
addGlobalDisplayType({
    type: DISPLAY_ENTRY_GRID,
    label: "Entry Date Grid",
    requiresData: false,
    category: CATEGORY_ENTRIES
});
//addGlobalDisplayType({type: DISPLAY_OPERANDS, label:"Operands",requiresData:false,category:CATEGORY_ENTRIES});
addGlobalDisplayType({
    type: DISPLAY_METADATA,
    label: "Metadata Table",
    requiresData: false,
    category: CATEGORY_ENTRIES
});



function RamaddaEntryDisplay(displayManager, id, type, properties) {
    let  SUPER = new RamaddaDisplay(displayManager, id, type, properties);
    RamaddaUtil.inherit(this, SUPER);
    this.defineProperties([
	{label:'Entry Search'},
	{p:'providers',ex:'',tt:'List of search providers'},
    ]);

    this.ramaddas = new Array();
    var repos = this.getProperty("repositories", this.getProperty("repos", null));
    if (repos != null) {
        var toks = repos.split(",");
        //OpenSearch;http://adasd..asdasdas.dasdas.,
        for (var i = 0; i < toks.length; i++) {
            var tok = toks[i];
            tok = tok.trim();
            this.ramaddas.push(getRamadda(tok));
        }
        if (this.ramaddas.length > 0) {
            var container = new RepositoryContainer("all", "All entries");
            addRepository(container);
            for (var i = 0; i < this.ramaddas.length; i++) {
                container.addRepository(this.ramaddas[i]);
            }
            this.ramaddas.push(container);
            this.setRamadda(this.ramaddas[0]);
        }
    }



    RamaddaUtil.defineMembers(this, {
        searchSettings: new EntrySearchSettings({
            parent: properties.entryParent,
            provider: properties.provider,
            text: properties.entryText,
            entryType: properties.entryType,
            orderBy: properties.orderBy,
	    entryRoot: properties.entryRoot,
        }),
        entryList: properties.entryList,
        entryMap: {},
	writeEntries: function(msg, entries) {
	    this.jq(ID_ENTRIES).html(msg);
	},
	writeMessage:function( msg)  {
	    this.jq(ID_RESULTS).html(msg);
	},
	writeResults: function(msg) {
	    this.jq(ID_RESULTS).html(msg);
	},
        getSearchSettings: function() {
            if (this.getPropertyProviders() != null) {
                let provider = this.searchSettings.provider;
		let select = this.jq(ID_PROVIDERS);
                let fromSelect = select.val();
                if (fromSelect != null) {
                    provider = fromSelect;
                } else {
                    let toks = this.getPropertyProviders().split(",");
                    if (toks.length > 0) {
                        let tuple = toks[0].split(":");
                        provider = tuple[0];
                    }
                }
                this.searchSettings.provider = provider;
            }
            return this.searchSettings;
        },
        getEntries: function() {
            if (this.entryList == null) return [];
            return this.entryList.getEntries();
        },

    });
    if (properties.entryType != null) {
        this.searchSettings.addType(properties.entryType);
    }
}






function RamaddaSearcherDisplay(displayManager, id,  type, properties) {
    let NONE = "-- None --";
    let myProps = [
	{label:'Search'},
        {p:"showForm",d: true},
        {p:"formOpen",d: true},	
        {p:"showOrderBy",ex: "true"},
        {p:"orderBy",ex: "name_ascending|name_descending|fromdate_ascending|fromdate_descending|todate_|createdate_|size_"},
        {p:"searchText",d: ""},
        {p:"orientation",ex:"horizontal|vertical",d:"horizontal"},
        {p:"showSearchSettings",d: true},
        {p:"showToggle",d: true},
	{p:'formHeight',eg:'200px'},
        {p:"showEntries",d: true},
        {p:"showType",d: true},
        {p:"types",ex:'comma separated list of types'},
        {p:"doSearch",d: true},
        {p:"showMetadata",d: true},
        {p:"showTags",d: true},	
        {p:"showArea",d: true},
        {p:"showText",d: true},
        {p:"showDate",d: true},
        {p:"showCreateDate",ex:"true",d: false},	
        {p:"fields",d: null},
        {p:"formWidth",d: 0},
        {p:"entriesWidth",d: 0},
        {p:"entriesHeight",ex: "300px"},	
        {p:"showDetailsForGroup",d: false},
	{p:'doWorkbench',ex:'true', tt:'Show the new, charts, etc links'},
	];

    const SUPER = new RamaddaEntryDisplay(displayManager, id, type, properties);

    defineDisplay(this, SUPER, myProps, {
        metadataTypeList: [],
	haveSearched: false,
        haveTypes: false,
        metadata: {},
        metadataLoading: {},
	ctor: function() {
	    if (this.getProperty("showMetadata") && this.getProperty("showSearchSettings")) {
		let metadataTypesAttr = this.getProperty("metadataTypes", "enum_tag:Tag");
		//look for type:value:label, or type:label,
		let toks = metadataTypesAttr.split(",");
		for (let i = 0; i < toks.length; i++) {
		    let type = toks[i];
		    let label = type;
		    let value = null;
		    let subToks = type.split(":");
		    if (subToks.length > 1) {
			type = subToks[0];
			if (subToks.length >= 3) {
			    value = subToks[1];
			    label = subToks[2];
			} else {
			    label = subToks[1];
			}
		    }
		    this.metadataTypeList.push(new MetadataType(type, label, value));
		}
	    }
	},
        isLayoutHorizontal: function() {
	    return this.getOrientation()== "horizontal";
        },

	initHtml: function() {
	    if(this.areaWidget) this.areaWidget.initHtml();
	    if(this.getShowOrderBy()) {
		let settings = this.getSearchSettings();
		let byList = [["A-Z","name_ascending"],["Z-A","name_descending"],
			  ["Create date - oldest first","createdate_ascending"],
			  ["Create date - youngest first","createdate_descending"],
			  ["From date - oldest first","fromdate_ascending"],
			  ["From date - youngest first","fromdate_descending"],			  			  
			  ["Size - largest first","size_descending"],
			  ["Size - smallest first","size_ascending"]];			  
		let options = "";
		byList.forEach(tuple=>{
		    let label = tuple[0];
		    let by = tuple[1];
		    let extra = settings.orderBy==by?" selected ":""
		    options += "<option title='" + label+"'  " + "" + extra + " value=\"" + by + "\">" + label + "</option>\n";
		    
		});
		let select = HU.tag("select", ["id", this.getDomId(ID_SEARCH_ORDERBY), ATTR_CLASS, "display-search-orderby"], options);
		this.jq(ID_SEARCH_HEADER).append("Order by: "+ select);
	    }
            this.addExtraForm();
	},
        getDefaultHtml: function() {
            let html = "";
            let horizontal = this.isLayoutHorizontal();
            let footer = this.getFooter();
            if (!this.getProperty("showFooter", true)) {
                footer = "";
            }
	    this.jq(ID_BOTTOM).html(footer);
	    footer = "";
            let entriesDivAttrs = [ATTR_ID, this.getDomId(ID_ENTRIES), ATTR_CLASS, this.getClass("content")];
            let innerHeight = this.getProperty("innerHeight", null);
            let entriesStyle = this.getProperty("entriesStyle", "");	    
	    let style = "";
            if (innerHeight == null) {
                innerHeight = this.getEntriesHeight();
            }
            if (innerHeight != null) {
                style = "margin: 0px; padding: 0px;  xmin-height:" + HU.getDimension(innerHeight) + "; max-height:" + HU.getDimension(innerHeight) + "; overflow-y: auto;";
            }
	    style+= entriesStyle;
            entriesDivAttrs.push(ATTR_STYLE);
            entriesDivAttrs.push(style);	    
	    let searchBar = HU.div([CLASS,"display-search-bar",ID, this.domId(ID_SEARCH_BAR)],"");
            let resultsDiv = "";
            if (this.getProperty("showHeader", true)) {
                resultsDiv = HU.div([ATTR_CLASS, "display-entries-results", ATTR_ID, this.getDomId(ID_RESULTS)], "&nbsp;");
            }
	    resultsDiv = HU.leftRightTable(resultsDiv,HU.div([CLASS,"display-search-header", ID,this.domId(ID_SEARCH_HEADER)]),null,null,{valign:"bottom"});
            let entriesDiv =
		searchBar +
                resultsDiv +
                HU.div(entriesDivAttrs, this.getLoadingMessage());

            if (horizontal) {
                html += HU.openTag(TAG_DIV, ["class", "row"]);
                let entriesAttrs = ["class", "col-md-12"];
                if (this.getShowForm()) {
                    let attrs = [];
                    if (this.getFormWidth() === "") {
                        attrs = [];
                    } else if (this.getFormWidth() != 0) {
                        attrs = [ATTR_WIDTH, this.getFormWidth()];
                    }
                    html += HU.tag(TAG_DIV, ["class", "col-md-4"], this.makeSearchForm());
                    entriesAttrs = ["class", "col-md-8"];
                }
                if (this.getShowEntries()) {
                    let attrs = [ATTR_WIDTH, "75%"];
                    if (this.getEntriesWidth() === "") {
                        attrs = [];
                    } else if (this.getEntriesWidth() != 0) {
                        attrs = [ATTR_WIDTH, this.getEntriesWidth()];
                    }
                    html += HU.tag(TAG_DIV, entriesAttrs, entriesDiv);
                }
                html += HU.closeTag("row");

                html += HU.openTag(TAG_DIV, ["class", "row"]);
                if (this.getShowForm()) {
                    html += HU.tag(TAG_DIV, ["class", "col-md-6"], "");
                }
                if (this.getShowEntries()) {
                    if (this.getProperty("showFooter", true)) {
                        html += HU.tag(TAG_DIV, ["class", "col-md-6"], footer);
                    }
                }
                html += HU.closeTag(TAG_DIV);
            } else {
                if (this.getShowForm()) {
                    html += this.makeSearchForm();
                }
                if (this.getShowEntries()) {
                    html += entriesDiv;
                    html += footer;
                }
            }
            html += HU.div([ATTR_CLASS, "display-entry-popup", ATTR_ID, this.getDomId(ID_DETAILS)], "&nbsp;");
            return html;
        },
        initDisplay: function() {
            let theDisplay = this;

            this.jq(ID_SEARCH).click(function(event) {
                theDisplay.submitSearchForm();
                event.preventDefault();
            });

            this.jq(ID_TEXT_FIELD).autocomplete({
                source: function(request, callback) {
                    //                            theDisplay.doQuickEntrySearch(request, callback);
                }
            });

	    this.selectboxit(this.jq(ID_SEARCH_ORDERBY));
	    this.jq(ID_SEARCH_ORDERBY).change(()=>{	    
                this.submitSearchForm();
	    });
            this.selectboxit(this.jq(ID_REPOSITORY));
            this.jq(ID_REPOSITORY).change(function() {
                let v = theDisplay.jq(ID_REPOSITORY).val();
                let ramadda = getRamadda(v);
                theDisplay.setRamadda(ramadda);
                theDisplay.addTypes(null);
                theDisplay.typeChanged();
            });

            this.jq(ID_FORM).submit(function(event) {
                theDisplay.submitSearchForm();
                event.preventDefault();
            });


            this.addTypes(this.entryTypes);
            for (let i = 0; i < this.metadataTypeList.length; i++) {
                let type = this.metadataTypeList[i];
                this.addMetadata(type, null);
            }
            if (!this.haveSearched) {
                if (this.getDoSearch()) {
                    this.submitSearchForm();
                }
            }
        },
        showEntryDetails: async function(event, entryId, src, leftAlign) {
            if (true) return;
            let entry;
            await this.getEntry(entryId, e => {
                entry = e
            });
            let popupId = "#" + this.getDomId(ID_DETAILS + entryId);
            if (this.currentPopupEntry == entry) {
                this.hideEntryDetails(entryId);
                return;
            }
            let myloc = 'right top';
            let atloc = 'right bottom';
            if (leftAlign) {
                myloc = 'left top';
                atloc = 'left bottom';
            }
            this.currentPopupEntry = entry;
            if (src == null) src = this.getDomId("entry_" + entry.getIdForDom());
            let closeImage = HU.getIconImage(icon_close, []);
            let close = HU.onClick(this.getGet() + ".hideEntryDetails('" + entryId + "');",closeImage);

            let contents = this.getEntryHtml(entry, {
                headerRight: close
            });
            $(popupId).html(contents);
            $(popupId).show();
            /*
            $(popupId).position({
                    of: jQuery( "#" +src),
                        my: myloc,
                        at: atloc,
                        collision: "none none"
                        });
            */
        },

	getCloser: function() {
	    return "";
	    return  HU.jsLink("",HU.getIconImage(icon_close, [ID,this.domId("close"),STYLE,HU.css("cursor","pointer")]));
	},
	initCloser: function(what) {
	    this.jq("close").click(()=>{
		this.jq(what||ID_RESULTS).hide();
	    });
	},
        getResultsHeader: function(entries, includeCloser) {
            if (entries.length < 10) return entries.length+" result" +(entries.length>1?"s":"");
            var left = "Showing " + (this.searchSettings.skip + 1) + "-" + (this.searchSettings.skip + Math.min(this.searchSettings.max, entries.length));
            var nextPrev = [];
            var lessMore = [];
            if (this.searchSettings.skip > 0) {
                nextPrev.push(HU.onClick(this.getGet() + ".loadPrevUrl();", HU.getIconImage("fa-arrow-left", [ATTR_TITLE, "Previous"]), [ATTR_CLASS, "display-link"]));
            }
            var addMore = false;
            if (entries.length == this.searchSettings.getMax()) {
                nextPrev.push(HU.onClick(this.getGet() + ".loadNextUrl();", HU.getIconImage("fa-arrow-right", [ATTR_TITLE, "Next"]), [ATTR_CLASS, "display-link"]));
                addMore = true;
            }

            lessMore.push(HU.onClick(this.getGet() + ".loadLess();", HU.getIconImage("fa-minus", [ATTR_TITLE, "View less"]), [ATTR_CLASS, "display-link"]));
            if (addMore) {
                lessMore.push(HU.onClick(this.getGet() + ".loadMore();", HU.getIconImage("fa-plus", [ATTR_TITLE, "View more"]), [ATTR_CLASS, "display-link"]));
            }
            var results = "";
            var spacer = "&nbsp;&nbsp;&nbsp;"
	    if(includeCloser)
		results = this.getCloser();
	    results += "&nbsp;" + left + spacer;
            results += 
                HU.join(nextPrev, "&nbsp;") + spacer +
                HU.join(lessMore, "&nbsp;");
            return results+"<br>";
        },
        submitSearchForm: function() {
            if (this.fixedEntries) {
                return;
            }
            this.haveSearched = true;
            let settings = this.getSearchSettings();
            settings.text = this.getFieldValue(this.getDomId(ID_TEXT_FIELD), settings.text);
	    if(settings.text)
		HU.addToDocumentUrl(ID_TEXT_FIELD,settings.text);
	    else
		HU.addToDocumentUrl(ID_TEXT_FIELD,"");

	    let orderBy = this.jq(ID_SEARCH_ORDERBY).val();
	    if(orderBy) {
		let ascending = orderBy.indexOf("_ascending")>=0;
		orderBy = orderBy.replace("_ascending","").replace("_descending","");
		settings.orderBy =  orderBy;
		settings.ascending = ascending;
	    }

            if (this.textRequired && (settings.text == null || settings.text.trim().length == 0)) {
                this.writeEntries("");
                return;
            }



            if (this.haveTypes) {
                settings.entryType = this.getFieldValue(this.getDomId(ID_TYPE_FIELD), settings.entryType);
		if(settings.entryType) {
		    HU.addToDocumentUrl(ID_TYPE_FIELD,settings.entryType);
		} else {
		    HU.addToDocumentUrl(ID_TYPE_FIELD,"");
		}
            } else if(this.typeList && this.typeList.length==1) {
		settings.entryType = this.typeList[0];
	    }
            settings.clearAndAddType(settings.entryType);
            if (this.areaWidget) {
                this.areaWidget.setSearchSettings(settings);
            }
            if (this.dateRangeWidget) {
                this.dateRangeWidget.setSearchSettings(settings);
            }
            if (this.createdateRangeWidget) {
                this.createdateRangeWidget.setSearchSettings(settings);
            }	    
	    
            settings.metadata = [];
	    if(!this.getShowTags()) {
		for (var i = 0; i < this.metadataTypeList.length; i++) {
                    var metadataType = this.metadataTypeList[i];
                    var value = metadataType.getValue();
                    if (value == null) {
			value = this.getFieldValues(this.getMetadataFieldId(metadataType), null);
                    }
                    if (value != null) {
			if(!Array.isArray(value)) {value=[value]}
			value.forEach(v=>{
			    settings.metadata.push({
				type: metadataType.getType(),
				value: v
			    });
			});
                    }
		}
	    } else {
		let _this = this;
		this.jq(ID_SEARCH_BAR).find(".display-search-tag").each(function() {
		    let type  = $(this).attr("metadata-type");
		    let value  = $(this).attr("metadata-value");			
		    settings.metadata.push({
			type: type,
			value: value
		    });
		});
            }

            //Call this now because it sets settings


            var theRepository = this.getRamadda()

            if (theRepository.children) {
                this.entryList = new EntryListHolder(theRepository, this);
                this.multiSearch = {
                    count: 0,
                };

                for (var i = 0; i < theRepository.children.length; i++) {
                    var ramadda = theRepository.children[i];
                    var jsonUrl = this.makeSearchUrl(ramadda);
                    this.updateForSearching(jsonUrl);
                    this.entryList.addEntryList(new EntryList(ramadda, jsonUrl, null, false));
                    this.multiSearch.count++;
                }
                this.entryList.doSearch(this);
            } else {
                this.multiSearch = null;
                var jsonUrl = this.makeSearchUrl(this.getRamadda());
                console.log(jsonUrl);
                this.entryList = new EntryList(this.getRamadda(), jsonUrl, this, true);
                this.updateForSearching(jsonUrl);
            }
        },
        entryListChanged:function(entryList) {
	},
        handleSearchError: function(url, msg) {
            this.writeEntries("");
            this.writeMessagel( "");
            console.log("Error performing search:" + msg);
            //alert("There was an error performing the search\n" + msg);
        },
        updateForSearching: function(jsonUrl) {
            var outputs = this.getRamadda().getSearchLinks(this.getSearchSettings());
            this.footerRight = outputs == null ? "" : "Links: " + HU.join(outputs, " - ");
            this.writeHtml(ID_FOOTER_RIGHT, this.footerRight);
            var msg = this.searchMessage;
            if (msg == null) {
                msg = this.getRamadda().getSearchMessage();
            }
            var provider = this.getSearchSettings().provider;
            if (provider != null) {
                msg = null;
                if (this.providerMap != null) {
                    msg = this.providerMap[provider];
                }
                if (msg == null) {
                    msg = provider;
                }
                msg = "Searching " + msg;
            }
            this.hideEntryDetails();
        },
        prepareToLayout: function() {
            SUPER.prepareToLayout.apply(this);
            this.savedValues = {};
            var cols = this.getSearchableColumns();
            for (var i = 0; i < cols.length; i++) {
                var col = cols[i];
                var id = this.getDomId(ID_COLUMN + col.getName());
                var value = $("#" + id).val();
                if (value == null || value.length == 0) continue;
                this.savedValues[id] = value;
            }
        },
        makeSearchUrl: function(repository) {
            let extra = "";
            let cols = this.getSearchableColumns();
            for (let i = 0; i < cols.length; i++) {
                let col = cols[i];
                let value = this.jq(ID_COLUMN + col.getName()).val();
                if (value == null || value.length == 0) continue;
                extra += "&" + col.getSearchArg() + "=" + encodeURI(value);
            }
            this.getSearchSettings().setExtra(extra);
            let jsonUrl = repository.getSearchUrl(this.getSearchSettings(), OUTPUT_JSON);
            return jsonUrl;
        },
        makeSearchForm: function() {
            let form = HU.openTag("form", [ATTR_ID, this.getDomId(ID_FORM), "action", "#"]);

            let buttonLabel = HU.getIconImage("fa-search", [ATTR_TITLE, "Search"]);
            let topItems = [];
            let searchButton = HU.div([ATTR_ID, this.getDomId(ID_SEARCH), ATTR_CLASS, "display-search-button ramadda-clickable"], buttonLabel);
            let extra = "";
            let settings = this.getSearchSettings();
	    let addWidget = (label, widget)=>{
		if(horizontal) 
		    return HU.div([CLASS,"display-search-label"], label) + 
		    HU.div([CLASS,"display-search-widget"], widget);
		return HU.formEntry("",widget);
	    };

            let horizontal = this.isLayoutHorizontal();

            if (this.ramaddas.length > 0) {
                let repositoriesSelect = HU.openTag(TAG_SELECT, [ATTR_ID, this.getDomId(ID_REPOSITORY), ATTR_CLASS, "display-repositories-select"]);
                let icon = ramaddaBaseUrl + "/icons/favicon.png";
                for (let i = 0; i < this.ramaddas.length; i++) {
                    let ramadda = this.ramaddas[i];
                    let attrs = [ATTR_TITLE, "", ATTR_VALUE, ramadda.getId(),
                        "data-iconurl", icon
                    ];
                    if (this.getRamadda().getId() == ramadda.getId()) {
                        attrs.push("selected");
                        attrs.push(null);
                    }
                    let label =
                        repositoriesSelect += HU.tag(TAG_OPTION, attrs,
                            ramadda.getName());
                }
                repositoriesSelect += HU.closeTag(TAG_SELECT);
                topItems.push(repositoriesSelect);
            }


            this.providerMap = {};
            if (this.getPropertyProviders() != null) {
                let options = "";
                let selected = Utils.getUrlArgs(document.location.search).provider;
                let toks = this.getPropertyProviders().split(",");
                let currentCategory = null;
                let catToBuff = {};
                let cats = [];

                for (let i = 0; i < toks.length; i++) {
                    let tuple = toks[i].split(":");
                    let id = tuple[0];
		    if(!Utils.isDefined(selected)) {
			selected = id;
		    }

                    id = id.replace(/_COLON_/g, ":");
                    let label = tuple.length > 1 ? tuple[1] : id;
                    if (label.length > 40) {
                        label = label.substring(0, 39) + "...";
                    }
                    this.providerMap[id] = label;
                    let extraAttrs = "";
                    if (id == selected) {
                        extraAttrs += " selected ";
                    }
                    let category = "";

                    if (tuple.length > 3) {
                        category = tuple[3];
                    }
                    let buff = catToBuff[category];
                    if (buff == null) {
                        cats.push(category);
                        catToBuff[category] = "";
                        buff = "";
                    }
                    if (tuple.length > 2) {
                        let img = tuple[2];
                        img = img.replace(/\${urlroot}/g, ramaddaBaseUrl);
                        img = img.replace(/\${root}/g, ramaddaBaseUrl);
                        extraAttrs += " data-iconurl=\"" + img + "\" ";
                    }
                    buff += "<option title='" + label+"' class=display-search-provider " + extraAttrs + " value=\"" + id + "\">" + label + "</option>\n";
                    catToBuff[category] = buff;
                }

                for (let catIdx = 0; catIdx < cats.length; catIdx++) {
                    let category = cats[catIdx];
                    if (category != "")
                        options += "<optgroup label=\"" + category + "\">\n";
                    options += catToBuff[category];
                    if (category != "")
                        options += "</optgroup>";

                }
		let providersSelect = HU.tag("select", ["multiple", null, "id", this.getDomId(ID_PROVIDERS), ATTR_CLASS, "display-search-providers"], options);
                topItems.push(providersSelect);
            }


	    this.typeList = null;
            if (this.getTypes()) {
		this.typeList = this.getTypes().split(",");
	    }
            if (this.getShowType()) {
		if(this.typeList == null || this.typeList.length==0) {
                    topItems.push(HU.span([ATTR_ID, this.getDomId(ID_TYPE_DIV)], HU.span([ATTR_CLASS, "display-loading"], "Loading types...")));
		} else {
		    extra+= HU.span([ATTR_ID, this.getDomId(ID_TYPE_DIV)]);
		}
            }



	    let text  = this.getFormText();
	    if(!text || text=="")
		text = HU.getUrlArgument(ID_TEXT_FIELD);
            let textField = HU.input("", text, ["placeholder", this.getEgText("Search text"), ATTR_CLASS, "display-simplesearch-input", ATTR_SIZE, this.getProperty("inputSize", "30"), ATTR_ID, this.domId(ID_TEXT_FIELD)]);

            if (this.getShowText()) {
		topItems.push(textField);
            }

	    let contents = "";
	    let topContents = "";	    
            if (horizontal) {
		if(topItems.length>0) {
                    form += "<table><tr valign=top><td>" + searchButton + "</td><td>" + topItems[0] + "</td></tr></table>";
		    topContents +=  HU.join(topItems.slice(1), "<br>");
		}
            } else {
                topContents +=  searchButton + " " + HU.join(topItems, " ");
            }
		

	    if(!horizontal) 
		extra += HU.formTable();
            if (this.getShowDate()) {
                this.dateRangeWidget = new DateRangeWidget(this);
                extra += addWidget("", this.dateRangeWidget.getHtml());
            }
            if (this.getShowCreateDate(true)) {
                this.createdateRangeWidget = new DateRangeWidget(this,"createdate");
                extra += addWidget("", this.createdateRangeWidget.getHtml());
            }
            if (this.getShowArea()) {
                this.areaWidget = new AreaWidget(this);
                extra += addWidget("", this.areaWidget.getHtml());
            }

            extra += HU.div([ATTR_ID, this.getDomId(ID_TYPEFIELDS)], "");
            if (this.getShowMetadata()) {
                for (let i = 0; i < this.metadataTypeList.length; i++) {
                    let type = this.metadataTypeList[i];
                    let value = type.getValue();
                    let metadataSelect;
                    if (value != null) {
                        metadataSelect = value;
                    } else {
                        metadataSelect = HU.tag(TAG_SELECT, [ATTR_ID, this.getMetadataFieldId(type),
                                ATTR_CLASS, "display-metadatalist"
                            ],
                            HU.tag(TAG_OPTION, [ATTR_TITLE, "", ATTR_VALUE, ""],
                                NONE));
                    }
		    if(this.getShowTags()) {
			extra += HU.div([CLASS,"display-search-label"], type.getLabel());
			extra += HU.div([CLASS,"display-search-metadata-block"], HU.div([CLASS,"display-search-metadata-block-inner", ID,this.getMetadataFieldId(type)]));
		    } else {
			extra += addWidget(type.getLabel() + ":", metadataSelect);
		    }
                }
            }
	    if(!horizontal) 
		extra += HU.closeTag(TAG_TABLE);

            if (this.getShowSearchSettings()) {
                let id = this.getDomId(ID_SEARCH_SETTINGS);
                if (this.getShowToggle()) {
                    contents+= HU.div([ATTR_CLASS, "display-search-extra", ATTR_ID, id],
				   HU.toggleBlock("Search Settings", HU.div([ATTR_CLASS, "display-search-extra-inner"], extra), this.getFormOpen(true)));
                } else {
                    contents += HU.div([ATTR_CLASS, "display-search-extra", ATTR_ID, id],
                        HU.div([ATTR_CLASS, "display-search-extra-inner"], extra));
                }
            }

            //Hide the real submit button
            contents += "<input type=\"submit\" style=\"position:absolute;left:-9999px;width:1px;height:1px;\"/>";
	    if(this.getFormHeight()) {
		contents = HU.div([STYLE,HU.css("overflow-y","auto","max-height",HU.getDimension(this.getFormHeight()))], contents);
	    }

	    form+=HU.div([STYLE,"margin-top:5px", CLASS,"display-search-extra"],topContents);
	    form+=contents;
            form += HU.closeTag("form");
            return form;

        },
	getEgText:function(eg) {
            eg = this.getProperty("placeholder",eg);
            if (this.eg) {
                eg = " " + this.eg;
            }
	    return eg;
	},

	getFormText:function() {
	    var text = this.getSearchSettings().text;
            if (text == null) {
                var args = Utils.getUrlArgs(document.location.search);
                text = args.text;
            }
            if (text == null) {
                text = this.getSearchText();
            }
	    return text;
	},

        handleEventMapBoundsChanged: function(source, args) {
            if (this.areaWidget) {
                this.areaWidget.handleEventMapBoundsChanged(source, args);
            }
        },
        typeChanged: function() {
            var settings = this.getSearchSettings();
            settings.skip = 0;
            settings.max = DEFAULT_MAX;
            settings.entryType = this.getFieldValue(this.getDomId(ID_TYPE_FIELD), settings.entryType);
            settings.clearAndAddType(settings.entryType);
            this.addExtraForm();
            this.submitSearchForm();
        },
        addMetadata: function(metadataType, metadata) {
            if (metadata == null) {
                metadata = this.metadata[metadataType.getType()];
            }
            if (metadata == null) {
                let theDisplay = this;
                if (!this.metadataLoading[metadataType.getType()]) {
                    this.metadataLoading[metadataType.getType()] = true;
                    metadata = this.getRamadda().getMetadataCount(metadataType, function(metadataType, metadata) {
                        theDisplay.addMetadata(metadataType, metadata);
                    });
                }
            }
            if (metadata == null) {
                return;
            }

	    if(!this.metadataBoxes) this.metadataBoxes={};
	    this.metadataBoxes[metadataType.getType()] = {};

            this.metadata[metadataType.getType()] = metadata;

            let select = HU.tag(TAG_OPTION, [ATTR_TITLE, "", ATTR_VALUE, ""], NONE);
	    let cbxs = [];
            for (let i = 0; i < metadata.length; i++) {
                let count = metadata[i].count;
                let value = metadata[i].value;
                let label = metadata[i].label;
                let optionAttrs = [ATTR_VALUE, value, ATTR_CLASS, "display-metadatalist-item"];
                let selected = false;
                if (selected) {
                    optionAttrs.push("selected");
                    optionAttrs.push(null);
                }
                select += HU.tag(TAG_OPTION, optionAttrs, label + " (" + count + ")");
		let cbxId = this.getMetadataFieldId(metadataType)+"_checkbox_" + i;
		this.metadataBoxes[metadataType.getType()][value] = cbxId;
		cbxs.push(HU.checkbox("",[ID,cbxId,"metadata-type",metadataType.getType(),"metadata-value",value],false) +" " + HU.tag( "label",  [CLASS,"ramadda-noselect ramadda-clickable","for",cbxId],label +" (" + count+")"));
            }
	    if(!this.getShowTags()) {
		$("#" + this.getMetadataFieldId(metadataType)).html(select);
		this.selectbtaoxit($("#" + this.getMetadataFieldId(metadataType)));
	    } else {
		$("#" + this.getMetadataFieldId(metadataType)).html(Utils.wrap(cbxs,"","<br>"));
		let _this = this;
		$("#" + this.getMetadataFieldId(metadataType)).find(":checkbox").change(function(){
		    let value  = $(this).attr("metadata-value");
		    let type  = $(this).attr("metadata-type");		
                    let on = $(this).is(':checked');
		    let cbx = $(this);
		    if(on) {
			_this.addMetadataTag(metadataType.getType(), metadataType.getLabel(),value, cbx);
		    } else {
			let tagId = Utils.makeId(_this.domId(ID_SEARCH_TAG) +"_" + metadataType.getType() +"_" + value);
			$("#" + tagId).remove();
		    }		
		    _this.submitSearchForm();
		});
	    }
        },

	addMetadataTag:function(type, label,value, cbx) {
	    let _this = this;
	    let tagGroupId = ID_SEARCH_TAG_GROUP+"_"+type;
	    let tagGroup = _this.jq(tagGroupId);
	    let tagId = Utils.makeId(_this.domId(ID_SEARCH_TAG) +"_" +type +"_" + value);
	    if(tagGroup.length==0) {
		tagGroup = $(HU.div([CLASS,"display-search-tag-group",ID,_this.domId(tagGroupId)])).appendTo(_this.jq(ID_SEARCH_BAR));			     
	    }
	    let tag = $(HU.div(["metadata-type",type,"metadata-value",value,TITLE,label+":" + value, CLASS,"display-search-tag", ID,tagId],value+SPACE +HU.getIconImage("fas fa-times"))).appendTo(tagGroup);
	    tag.click(function() {
		$(this).remove();
		if(cbx)
		    cbx.prop("checked",false);
		_this.submitSearchForm();
	    });
	},
	metadataTagClicked:function(metadata) {
	    if(!this.metadataBoxes[metadata.type] || !this.metadataBoxes[metadata.type][metadata.value.attr1]) {
		this.addMetadataTag(metadata.type, metadata.type,metadata.value.attr1, null);
		return;
	    }

	    let cbx = $("#" + this.metadataBoxes[metadata.type][metadata.value.attr1]);
	    if(cbx.is(':checked')) return;
	    cbx.click();
	},
        getMetadataFieldId: function(metadataType) {
            let id = metadataType.getType();
            id = id.replace(".", "_");
            return this.getDomId(ID_METADATA_FIELD + id);
        },

        findEntryType: function(typeName) {
            if (this.entryTypes == null) return null;
            for (var i = 0; i < this.entryTypes.length; i++) {
                var type = this.entryTypes[i];
                if (type.getId() == typeName) return type;
            }
            return null;
        },
        addTypes: function(newTypes) {
            if (!this.getShowType()) {
                return;
            }
            if (newTypes == null) {
                newTypes = this.getRamadda().getEntryTypes((ramadda, types) =>{
                    this.addTypes(types);
                });
            }
            if (newTypes == null) {
                return;
            }

            this.entryTypes = newTypes;

            if (this.getTypes()) {
                let showType = {};
		let typeList = this.getTypes().split(",");
                typeList.forEach(type=>{
                    showType[type] = true;
                });
                let tmp = [];
                for (let i = 0; i < this.entryTypes.length; i++) {
                    let type = this.entryTypes[i];
                    if (showType[type.getId()]) {
                        tmp.push(type);
                    } else if (type.getCategory() != null && showType[type.getCategory()]) {
                        tmp.push(type);
                    }
                }
                this.entryTypes = tmp;
            }

            this.haveTypes = true;
            let cats = [];
            let catMap = {};
            let select = HU.openTag(TAG_SELECT, [ATTR_ID, this.getDomId(ID_TYPE_FIELD),
                ATTR_CLASS, "display-typelist",
                "onchange", this.getGet() + ".typeChanged();"
            ]);
            select += HU.tag(TAG_OPTION, [ATTR_TITLE, "", ATTR_VALUE, ""], "Any Type");
	    let hadSelected = false;
            for (let i = 0; i < this.entryTypes.length; i++) {
                let type = this.entryTypes[i];
                let icon = type.getIcon();
                let optionAttrs = [ATTR_TITLE, type.getLabel(), ATTR_VALUE, type.getId(), ATTR_CLASS, "display-typelist-type",
                    "data-iconurl", icon
                ];
                let selected = this.getSearchSettings().hasType(type.getId());
		if(!selected) {
		    let fromUrl = HU.getUrlArgument(ID_TYPE_FIELD);
		    if(fromUrl)
			selected = type.getId()==fromUrl;
		}
                if (selected) {
		    hadSelected = true;
                    optionAttrs.push("selected");
                    optionAttrs.push(null);
                }
                let option = HU.tag(TAG_OPTION, optionAttrs, type.getLabel() + " (" + type.getEntryCount() + ")");
                let map = catMap[type.getCategory()];
                if (map == null) {
                    catMap[type.getCategory()] = HU.tag(TAG_OPTION, [ATTR_CLASS, "display-typelist-category", ATTR_TITLE, "", ATTR_VALUE, ""], type.getCategory());
                    cats.push(type.getCategory());
                }
                catMap[type.getCategory()] += option;

            }
            for (let i in cats) {
                select += catMap[cats[i]];
            }
            select += HU.closeTag(TAG_SELECT);
	    if(this.entryTypes.length==1) {
		this.writeHtml(ID_TYPE_DIV, HU.hidden(ID_TYPE_FIELD,this.entryTypes[0].getId()));
	    } else {
		this.writeHtml(ID_TYPE_DIV, select);
	    }
            this.selectboxit(this.jq(ID_TYPE_FIELD));
            this.addExtraForm();
	    if(hadSelected) {
		this.submitSearchForm();
	    }
        },
        getSelectedType: function() {
            if (this.entryTypes == null) {
		return null;
	    }
	    if(this.entryTypes.length==1) return this.entryTypes[0];
            for (let i = 0; i < this.entryTypes.length; i++) {
                let type = this.entryTypes[i];
                if (type.getId) {
                    if (this.getSearchSettings().hasType(type.getId())) {
                        return type;
                    }
                }
            }
            let selectedType =  this.getFieldValue(this.getDomId(ID_TYPE_FIELD), null);
	    if(selectedType) {
		for (let i = 0; i < this.entryTypes.length; i++) {
                    let type = this.entryTypes[i];
                    if (type.getId) {
			if(selectedType == type.getId())
                            return type;
                    }
                }
            }
	    return null;

        },
        getSearchableColumns: function() {
            let searchable = [];
            let type = this.getSelectedType();
            if (type == null) {
                return searchable;
            }
            let cols = type.getColumns();
            if (cols == null) {
                return searchable;
            }
            for (var i = 0; i < cols.length; i++) {
                var col = cols[i];
                if (!col.getCanSearch()) continue;
                searchable.push(col);
            }
            return searchable;
        },
        addExtraForm: function() {
            if (this.savedValues == null) this.savedValues = {};
            let extra = "";
            let cols = this.getSearchableColumns();
            for (let i = 0; i < cols.length; i++) {
                let col = cols[i];
                if (this.getProperty("fields") != null && this.getProperty("fields").indexOf(col.getName()) < 0) {
                    continue;
                }

                if (extra.length == 0) {
//                    extra += HU.formTable();
                }
                let field = "";
                let id = this.getDomId(ID_COLUMN + col.getName());
                let savedValue = this.savedValues[id];
                if (savedValue == null) {
                    savedValue = this.jq(ID_COLUMN + col.getName()).val();
                }
                if (savedValue == null) savedValue = "";
                if (col.isEnumeration()) {
                    field = HU.openTag(TAG_SELECT, [ATTR_ID, id, ATTR_CLASS, "display-menu display-metadatalist"]);
                    field += HU.tag(TAG_OPTION, [CLASS,"display-metadatalist-item", ATTR_TITLE, "", ATTR_VALUE, ""],
                        "-- Select --");
                    let values = col.getValues();
                    for (let vidx in values) {
                        let value = values[vidx].value;
                        let label = values[vidx].label;
                        let extraAttr = "";
                        if (value == savedValue) {
                            extraAttr = " selected ";
                        }
                        field += HU.tag(TAG_OPTION, [CLASS,"display-metadatalist-item", ATTR_TITLE, label, ATTR_VALUE, value, extraAttr, null],
                            label);
                    }
                    field += HU.closeTag(TAG_SELECT);
                    extra += HU.div([CLASS,"display-search-label"], col.getLabel()+":")+
			HU.div([CLASS,"display-search-widget"], field+col.getSuffix());
                } else {
                    field = HU.input("", savedValue, ["placeholder",col.getLabel(),ATTR_CLASS, "input", ATTR_SIZE, "15", ATTR_ID, id]);
                    extra += HU.div([CLASS,"display-search-label"], "") +HU.div([CLASS,"display-search-widget"], field + " " + col.getSuffix());
                }
            }
            if (extra.length > 0) {
                extra += HU.closeTag(TAG_TABLE);
            }
            this.writeHtml(ID_TYPEFIELDS, extra);
	    let menus = this.jq(ID_TYPEFIELDS).find(".display-menu");
	    this.selectboxit(menus);
	    menus.change(()=>{
		this.submitSearchForm();
	    });
        },
        getEntries: function() {
            if (this.entryList == null) return [];
            return this.entryList.getEntries();
        },
        loadNextUrl: function() {
            this.getSearchSettings().skip += this.getSearchSettings().max;
            this.submitSearchForm();
        },
        loadMore: function() {
            this.getSearchSettings().max = this.getSearchSettings().max += DEFAULT_MAX;
            this.submitSearchForm();
        },
        loadLess: function() {
            let max = this.getSearchSettings().max;
            max = parseInt(0.75 * max);
            this.getSearchSettings().max = Math.max(1, max);
            this.submitSearchForm();
        },
        loadPrevUrl: function() {
            this.getSearchSettings().skip = Math.max(0, this.getSearchSettings().skip - this.getSearchSettings().max);
            this.submitSearchForm();
        },
        entryListChanged: function(entryList) {
            this.entryList = entryList;
        }
    });
}


function RamaddaEntrylistDisplay(displayManager, id, properties, theType) {
    if (theType == null) {
        theType = DISPLAY_ENTRYLIST;
    }
    const SUPER = new RamaddaSearcherDisplay(displayManager, id, DISPLAY_ENTRYLIST, properties);
    let myProps = [];
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
        haveDisplayed: false,
        selectedEntries: [],
        getSelectedEntries: function() {
            return this.selectedEntries;
        },
        initDisplay: function() {
            let _this = this;
            if (this.getIsLayoutFixed() && this.haveDisplayed) {
                return;
            }
            this.haveDisplayed = true;
            this.createUI();
            this.setContents(this.getDefaultHtml());
	    this.initHtml();
            if (this.dateRangeWidget) {
                this.dateRangeWidget.initHtml();
            }
            if (this.createdateRangeWidget) {
                this.createdateRangeWidget.initHtml();
            }	    
            SUPER.initDisplay.apply(this);
            if (this.entryList != null && this.entryList.haveLoaded) {
                this.entryListChanged(this.entryList);
            }
            this.selectboxit(this.jq(ID_PROVIDERS),
			     {width:"100px",
			      "max-height":"50px"});
            this.jq(ID_PROVIDERS).change(function() {
                _this.providerChanged();
            });
        },
        providerChanged: function() {
            let provider = this.jq(ID_PROVIDERS).val();
            if (provider != "this") {
                this.jq(ID_SEARCH_SETTINGS).hide();
            } else {
                this.jq(ID_SEARCH_SETTINGS).show();
            }
        },
        getMenuItems: function(menuItems) {
            SUPER.getMenuItems.apply(this, menuItems);
            if (this.getSelectedEntriesFromTree().length > 0) {
                let get = this.getGet();
                menuItems.push(HU.onClick(get + ".makeDisplayList();", "Make List"));
                menuItems.push(HU.onClick(get + ".makeDisplayGallery();", "Make Gallery"));
            }
        },
        makeDisplayList: function() {
            let entries = this.getSelectedEntriesFromTree();
            if (entries.length == 0) {
                return;
            }
            let props = {
                selectedEntries: entries,
                showForm: false,
                showMenu: true,
                fixedEntries: true
            };
            props.entryList = new EntryList(this.getRamadda(), "", this, false);
            props.entryList.setEntries(entries);
            this.getDisplayManager().createDisplay(DISPLAY_ENTRYLIST, props);
        },
        makeDisplayGallery: function() {
            let entries = this.getSelectedEntriesFromTree();
            if (entries.length == 0) {
                return;
            }
            let props = {
                selectedEntries: entries
            };

	    let eg = this.getEgText();
	    let text  = this.getFormText();
            let textField = HU.input("", text, ["placeholder", eg, ATTR_CLASS, "display-search-input", ATTR_SIZE, "30", ATTR_ID, this.getDomId(ID_TEXT_FIELD)]);
            this.getDisplayManager().createDisplay(DISPLAY_ENTRY_GALLERY, props);
        },
        handleEventEntrySelection: function(source, args) {
            this.selectEntry(args.entry, args.selected);
        },
        selectEntry: function(entry, selected) {
            let changed = false;
            if (selected) {
                this.jq("entry_" + entry.getIdForDom()).addClass("ui-selected");
                let index = this.selectedEntries.indexOf(entry);
                if (index < 0) {
                    this.selectedEntries.push(entry);
                    changed = true;
                }
            } else {
                this.jq("entry_" + entry.getIdForDom()).removeClass("ui-selected");
                let index = this.selectedEntries.indexOf(entry);
                if (index >= 0) {
                    this.selectedEntries.splice(index, 1);
                    changed = true;
                }
            }
        },
        makeEntriesDisplay: function(entries) {
            return this.getEntriesTree(entries);
        },

        entryListChanged: function(entryList) {
            if (this.multiSearch) {
                this.multiSearch.count--;
            }
            SUPER.entryListChanged.apply(this, [entryList]);
            let entries = this.entryList.getEntries();

            if (entries.length == 0) {
                this.getSearchSettings().skip = 0;
                this.getSearchSettings().max = DEFAULT_MAX;
                let msg = "Nothing found";
                if (this.multiSearch) {
                    if (this.multiSearch.count > 0) {
                        msg = "Nothing found so far. Still searching " + this.multiSearch.count + " repositories";
                    } else {}
                }
                this.writeHtml(ID_FOOTER_LEFT, "");
                this.writeMessage(msg);		
                this.getDisplayManager().handleEventEntriesChanged(this, []);
		this.jq(ID_ENTRIES).html("");
                return;
            }
	    this.writeMessage(this.getResultsHeader(entries));

            let get = this.getGet();
            this.writeHtml(ID_FOOTER_LEFT, "");
            if (this.footerRight != null) {
                this.writeHtml(ID_FOOTER_RIGHT, this.footerRight);
            }

            //                let entriesHtml  = this.getEntriesTree(entries);
            let entriesHtml = this.makeEntriesDisplay(entries);
            let html = "";
            html += HU.openTag(TAG_OL, [ATTR_CLASS, this.getClass("list"), ATTR_ID, this.getDomId(ID_LIST)]);
            html += entriesHtml;
            html += HU.closeTag(TAG_OL);
            this.writeEntries(html, entries);
            this.addEntrySelect();
            this.getDisplayManager().handleEventEntriesChanged(this, entries);
        },
    });
}



function RamaddaTestlistDisplay(displayManager, id, properties) {
    let SUPER;
    RamaddaUtil.inherit(this, SUPER = new RamaddaEntrylistDisplay(displayManager, id, properties, DISPLAY_TESTLIST));
    RamaddaUtil.defineMembers(this, {
        //This gets called by the EntryList to actually make the display
        makeEntriesDisplay: function(entries) {

            return "Overridden display<br>" + this.getEntriesTree(entries);
        },
    });

}



var RamaddaListDisplay = RamaddaEntrylistDisplay;



function RamaddaEntrygalleryDisplay(displayManager, id, properties) {
    var ID_GALLERY = "gallery";
    var SUPER;
    RamaddaUtil.inherit(this, SUPER = new RamaddaEntryDisplay(displayManager, id, DISPLAY_ENTRY_GALLERY, properties));
    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
        entries: properties.entries,
        initDisplay: function() {
            var _this = this;
            this.createUI();
            var html = HU.div([ATTR_ID, this.getDomId(ID_GALLERY)], "Gallery");
            this.setContents(html);

            if (this.selectedEntries != null) {
                this.jq(ID_GALLERY).html(this.getEntriesGallery(this.selectedEntries));
                return;
            }
            if (this.entries) {
                var props = {
                    entries: this.entries
                };
                var searchSettings = new EntrySearchSettings(props);
                var jsonUrl = this.getRamadda().getSearchUrl(searchSettings, OUTPUT_JSON, "BAR");
                var myCallback = {
                    entryListChanged: function(list) {
                        var entries = list.getEntries();
                        _this.jq(ID_GALLERY).html(_this.getEntriesGallery(entries));
                        $("a.popup_image").fancybox({
                            'titleShow': false
                        });
                    }
                };
                var entryList = new EntryList(this.getRamadda(), jsonUrl, myCallback, true);
            }

            if (this.entryList != null && this.entryList.haveLoaded) {
                this.entryListChanged(this.entryList);
            }
        },
        getEntriesGallery: function(entries) {
            var nonImageHtml = "";
            var html = "";
            var imageCnt = 0;
            var imageEntries = [];
	    for (var i = 0; i < entries.length; i++) {
                var entry = entries[i];
                //Don: Right now this just shows all of the images one after the other.
                //If there is just one image we should just display it
                //We should do a gallery here if more than 1

                if (entry.isImage()) {
                    imageEntries.push(entry);
                    var link = HU.tag(TAG_A, [ATTR_HREF, entry.getEntryUrl()], entry.getName());
                    imageCnt++;
                    html += HU.tag(TAG_IMG, ["src", entry.getResourceUrl(), ATTR_WIDTH, "500", ATTR_ID,
                            this.getDomId("entry_" + entry.getIdForDom()),
                            ATTR_ENTRYID, entry.getId(), ATTR_CLASS, "display-entrygallery-entry"
                        ]) + "<br>" +
                        link + "<p>";
                } else {
                    var icon = entry.getIconImage([ATTR_TITLE, "View entry"]);
                    var link = HU.tag(TAG_A, [ATTR_HREF, entry.getEntryUrl()], icon + " " + entry.getName());
                    nonImageHtml += link + "<br>";
                }
            }

            if (imageCnt > 1) {
                //Show a  gallery instead
                var newHtml = "";
                newHtml += "<div class=\"row\">\n";
                var columns = parseInt(this.getProperty("columns", "3"));
                var colClass = "col-md-" + (12 / columns);
                for (var i = 0; i < imageEntries.length; i++) {
                    if (i >= columns) {
                        newHtml += "</div><div class=\"row\">\n";
                    }
                    newHtml += "<div class=" + colClass + ">\n";
                    var entry = imageEntries[i];
                    var link = HU.tag(TAG_A, [ATTR_HREF, entry.getEntryUrl()], entry.getName());
                    //Don: right now I just replicate what I do above
                    var img = HU.image(entry.getResourceUrl(), [ATTR_WIDTH, "100%", ATTR_ID,
                        this.getDomId("entry_" + entry.getIdForDom()),
                        ATTR_ENTRYID, entry.getId(), ATTR_CLASS, "display-entrygallery-entry"
                    ]);
                    img = HU.href(entry.getResourceUrl(), img, ["class", "popup_image"]);
                    newHtml += HU.div(["class", "image-outer"], HU.div(["class", "image-inner"], img) +
                        HU.div(["class", "image-caption"], link));

                    newHtml += "</div>\n";
                }
                newHtml += "</div>\n";
                html = newHtml;
            }


            //append the links to the non image entries
            if (nonImageHtml != "") {
                if (imageCnt > 0) {
                    html += "<hr>";
                }
                html += nonImageHtml;
            }
            return html;
        }
    });
}




function RamaddaEntrygridDisplay(displayManager, id, properties) {
    var SUPER;
    var ID_CONTENTS = "contents";
    var ID_GRID = "grid";
    var ID_AXIS_LEFT = "axis_left";
    var ID_AXIS_BOTTOM = "axis_bottom";
    var ID_CANVAS = "canvas";
    var ID_LINKS = "links";
    var ID_RIGHT = "right";

    var ID_SETTINGS = "gridsettings";
    var ID_YAXIS_ASCENDING = "yAxisAscending";
    var ID_YAXIS_SCALE = "scaleHeight";
    var ID_XAXIS_ASCENDING = "xAxisAscending";
    var ID_XAXIS_TYPE = "xAxisType";
    var ID_YAXIS_TYPE = "yAxisType";
    var ID_XAXIS_SCALE = "scaleWidth";
    var ID_SHOW_ICON = "showIcon";
    var ID_SHOW_NAME = "showName";
    var ID_COLOR = "boxColor";

    RamaddaUtil.inherit(this, SUPER = new RamaddaEntryDisplay(displayManager, id, DISPLAY_ENTRY_GRID, properties));
    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
        entries: properties.entries,
        initDisplay: function() {
            var _this = this;
            this.createUI();
            var html = HU.div([ATTR_ID, this.getDomId(ID_CONTENTS)], this.getLoadingMessage("Loading entries..."));
            this.setContents(html);
            if (!this.entryIds) {
                _this.jq(ID_CONTENTS).html(this.getLoadingMessage("No entries specified"));
                return;
            }
            var props = {
                entries: this.entryIds
            };
            var searchSettings = new EntrySearchSettings(props);
            var jsonUrl = this.getRamadda().getSearchUrl(searchSettings, OUTPUT_JSON, "BAR");
            var myCallback = {
                entryListChanged: function(list) {
                    _this.entries = list.getEntries();
                    if (_this.entries.length == 0) {
                        _this.jq(ID_CONTENTS).html(_this.getLoadingMessage("No entries selected"));
                        return;
                    }
                    _this.drag = null;
                    _this.jq(ID_CONTENTS).html(_this.makeFramework());

                    _this.canvas = $("#" + _this.getDomId(ID_CANVAS));
                    _this.gridPopup = $("#" + _this.getDomId(ID_GRID) + " .display-grid-popup");
                    var debugMouse = false;
                    var xAxis = _this.jq(ID_AXIS_BOTTOM);
                    var yAxis = _this.jq(ID_AXIS_LEFT);
                    var mousedown = function(evt) {
                        if (debugMouse)
                            console.log("mouse down");
                        _this.handledClick = false;
                        _this.drag = {
                            dragging: false,
                            x: GuiUtils.getEventX(evt),
                            y: GuiUtils.getEventY(evt),
                            X: {
                                minDate: _this.axis.X.minDate ? _this.axis.X.minDate : _this.minDate,
                                maxDate: _this.axis.X.maxDate ? _this.axis.X.maxDate : _this.maxDate,
                            },
                            Y: {
                                minDate: _this.axis.Y.minDate ? _this.axis.Y.minDate : _this.minDate,
                                maxDate: _this.axis.Y.maxDate ? _this.axis.Y.maxDate : _this.maxDate,
                            }
                        }
                    }
                    var mouseleave = function(evt) {
                        if (debugMouse)
                            console.log("mouse leave");
                        _this.drag = null;
                        _this.handledClick = false;
                    }
                    var mouseup = function(evt) {
                        if (debugMouse)
                            console.log("mouse up");
                        if (_this.drag) {
                            if (_this.drag.dragging) {
                                if (debugMouse)
                                    console.log("mouse up-was dragging");
                                _this.handledClick = true;
                            }
                            _this.drag = null;
                        }
                    }
                    var mousemove = function(evt, doX, doY) {
                        if (debugMouse)
                            console.log("mouse move");
                        var drag = _this.drag;
                        if (!drag) return;
                        drag.dragging = true;
                        var x = GuiUtils.getEventX(evt);
                        var deltaX = drag.x - x;
                        var y = GuiUtils.getEventY(evt);
                        var deltaY = drag.y - y;
                        var width = $(this).width();
                        var height = $(this).height();
                        var percentX = (x - drag.x) / width;
                        var percentY = (y - drag.y) / height;
                        var ascX = _this.getXAxisAscending();
                        var ascY = _this.getXAxisAscending();
                        var diffX = (drag.X.maxDate.getTime() - drag.X.minDate.getTime()) * percentX;
                        var diffY = (drag.Y.maxDate.getTime() - drag.Y.minDate.getTime()) * percentY;

                        if (doX) {
                            _this.axis.X.minDate = new Date(drag.X.minDate.getTime() + ((ascX ? -1 : 1) * diffX));
                            _this.axis.X.maxDate = new Date(drag.X.maxDate.getTime() + ((ascX ? -1 : 1) * diffX));
                        }
                        if (doY) {
                            _this.axis.Y.minDate = new Date(drag.Y.minDate.getTime() + ((ascY ? 1 : -1) * diffY));
                            _this.axis.Y.maxDate = new Date(drag.Y.maxDate.getTime() + ((ascY ? 1 : -1) * diffY));
                        }
                        _this.makeGrid(_this.entries);
                    }
                    var mouseclick = function(evt, doX, doY) {
                        if (_this.handledClick) {
                            if (debugMouse)
                                console.log("mouse click-other click");
                            _this.handledClick = false;
                            return;
                        }
                        if (_this.drag && _this.drag.dragging) {
                            if (debugMouse)
                                console.log("mouse click-was dragging");
                            _this.drag = null;
                            return;
                        }
                        if (debugMouse)
                            console.log("mouse click");
                        _this.drag = null;
                        var action;
                        if (evt.metaKey || evt.ctrlKey) {
                            action = "reset";
                        } else {
                            var zoomOut = evt.shiftKey;
                            if (zoomOut)
                                action = "zoomout";
                            else
                                action = "zoomin";
                        }
                        _this.doZoom(action, doX, doY);
                    };

                    var mousemoveCanvas = function(evt) {
                        mousemove(evt, true, true);
                    }
                    var mousemoveX = function(evt) {
                        mousemove(evt, true, false);
                    }
                    var mousemoveY = function(evt) {
                        mousemove(evt, false, true);
                    }

                    var mouseclickCanvas = function(evt) {
                        mouseclick(evt, true, true);
                    }
                    var mouseclickX = function(evt) {
                        mouseclick(evt, true, false);
                    }
                    var mouseclickY = function(evt) {
                        mouseclick(evt, false, true);
                    }


                    _this.canvas.mousedown(mousedown);
                    _this.canvas.mouseleave(mouseleave);
                    _this.canvas.mouseup(mouseup);
                    _this.canvas.mousemove(mousemoveCanvas);
                    _this.canvas.click(mouseclickCanvas);

                    xAxis.mousedown(mousedown);
                    xAxis.mouseleave(mouseleave);
                    xAxis.mouseup(mouseup);
                    xAxis.mousemove(mousemoveX);
                    xAxis.click(mouseclickX);

                    yAxis.mousedown(mousedown);
                    yAxis.mouseleave(mouseleave);
                    yAxis.mouseup(mouseup);
                    yAxis.mousemove(mousemoveY);
                    yAxis.click(mouseclickY);

                    var links =
                        HU.image(icon_zoom, ["class", "display-grid-action", "title", "reset zoom", "action", "reset"]) +
                        HU.image(icon_zoom_in, ["class", "display-grid-action", "title", "zoom in", "action", "zoomin"]) +
                        HU.image(icon_zoom_out, ["class", "display-grid-action", "title", "zoom out", "action", "zoomout"]);
                    _this.jq(ID_LINKS).html(links);
                    $("#" + _this.getDomId(ID_GRID) + " .display-grid-action").click(function() {
                        var action = $(this).attr("action");
                        _this.doZoom(action);
                    });


                    _this.jq(ID_AXIS_LEFT).html("");
                    _this.jq(ID_AXIS_BOTTOM).html("");
                    _this.makeGrid(_this.entries);
                }
            };
            var entryList = new EntryList(this.getRamadda(), jsonUrl, myCallback, true);
        },
        initDialog: function() {
            SUPER.initDialog.call(this);
            var _this = this;
            var cbx = this.jq(ID_SETTINGS + " :checkbox");
            cbx.click(function() {
                _this.setProperty($(this).attr("attr"), $(this).is(':checked'));
                _this.makeGrid(_this.entries);
            });
            var input = this.jq(ID_SETTINGS + " :input");
            input.blur(function() {
                _this.setProperty($(this).attr("attr"), $(this).val());
                _this.makeGrid(_this.entries);
            });
            input.keypress(function(event) {
                var keycode = (event.keyCode ? event.keyCode : event.which);
                if (keycode == 13) {
                    _this.setProperty($(this).attr("attr"), $(this).val());
                    _this.makeGrid(_this.entries);
                }
            });

        },
        getDialogContents: function(tabTitles, tabContents) {
            var height = "600";
            var html = "";
            html += HU.openTag("div", ["id", this.getDomId(ID_SETTINGS)]);

            html += HU.formTable();
            html += HU.formEntry("",
                HU.checkbox(this.getDomId(ID_SHOW_ICON),
                    ["attr", ID_SHOW_ICON],
                    this.getProperty(ID_SHOW_ICON, "true")) + " Show Icon" +
                "&nbsp;&nbsp;" +
                HU.checkbox(this.getDomId(ID_SHOW_NAME),
                    ["attr", ID_SHOW_NAME],
                    this.getProperty(ID_SHOW_NAME, "true")) + " Show Name");
            html += HU.formEntry("X-Axis:",
                HU.checkbox(this.getDomId(ID_XAXIS_ASCENDING),
                    ["attr", ID_XAXIS_ASCENDING],
                    this.getXAxisAscending()) + " Ascending" +
                "&nbsp;&nbsp;" +
                HU.checkbox(this.getDomId(ID_XAXIS_SCALE),
                    ["attr", ID_XAXIS_SCALE],
                    this.getXAxisScale()) + " Scale Width");
            html += HU.formEntry("Y-Axis:",
                HU.checkbox(this.getDomId(ID_YAXIS_ASCENDING),
                    ["attr", ID_YAXIS_ASCENDING],
                    this.getYAxisAscending()) + " Ascending" +
                "&nbsp;&nbsp;" +
                HU.checkbox(this.getDomId(ID_YAXIS_SCALE),
                    ["attr", ID_YAXIS_SCALE],
                    this.getYAxisScale()) + " Scale Height");

            html += HU.formEntry("Box Color:",
                HU.input(this.getDomId(ID_COLOR),
                    this.getProperty(ID_COLOR, "lightblue"),
                    ["attr", ID_COLOR]));

            html += HU.formTableClose();
            html += HU.closeTag("div");
            tabTitles.push("Entry Grid");
            tabContents.push(html);
            SUPER.getDialogContents.call(this, tabTitles, tabContents);
        },

        doZoom: function(action, doX, doY) {
            if (!Utils.isDefined(doX)) doX = true;
            if (!Utils.isDefined(doY)) doY = true;
            if (action == "reset") {
                this.axis.Y.minDate = null;
                this.axis.Y.maxDate = null;
                this.axis.X.minDate = null;
                this.axis.X.maxDate = null;
            } else {
                var zoomOut = (action == "zoomout");
                if (doX) {
                    var d1 = this.axis.X.minDate.getTime();
                    var d2 = this.axis.X.maxDate.getTime();
                    var dateRange = d2 - d1;
                    var diff = (zoomOut ? 1 : -1) * dateRange * 0.1;
                    this.axis.X.minDate = new Date(d1 - diff);
                    this.axis.X.maxDate = new Date(d2 + diff);
                }
                if (doY) {
                    var d1 = this.axis.Y.minDate.getTime();
                    var d2 = this.axis.Y.maxDate.getTime();
                    var dateRange = d2 - d1;
                    var diff = (zoomOut ? 1 : -1) * dateRange * 0.1;
                    this.axis.Y.minDate = new Date(d1 - diff);
                    this.axis.Y.maxDate = new Date(d2 + diff);
                }
            }
            this.makeGrid(this.entries);
        },
        initGrid: function(entries) {
            var _this = this;
            var items = this.canvas.find(".display-grid-entry");
            items.click(function(evt) {
                var index = parseInt($(this).attr("index"));
                entry = entries[index];
                var url = entry.getEntryUrl();
                if (_this.urlTemplate) {
                    url = _this.urlTemplate.replace("{url}", url).replace(/{entryid}/g, entry.getId()).replace(/{resource}/g, entry.getResourceUrl());
                }

                _this.handledClick = true;
                _this.drag = null;
                window.open(url, "_entry");
                //                        evt.stopPropagation();
            });
            items.mouseout(function() {
                var id = $(this).attr("entryid");
                if (id) {
                    var other = _this.canvas.find("[entryid='" + id + "']");
                    other.each(function() {
                        if ($(this).attr("itemtype") == "box") {
                            $(this).attr("prevcolor", $(this).css("background"));
                            $(this).css("background", $(this).attr("prevcolor"));
                        }
                    });
                }

                _this.gridPopup.hide();
            });
            items.mouseover(function(evt) {
                var id = $(this).attr("entryid");
                if (id) {
                    var other = _this.canvas.find("[entryid='" + id + "']");
                    other.each(function() {
                        if ($(this).attr("itemtype") == "box") {
                            $(this).attr("prevcolor", $(this).css("background"));
                            $(this).css("background", "rgba(0,0,255,0.5)");
                        }
                    });
                }
                var x = GuiUtils.getEventX(evt);
                var index = parseInt($(this).attr("index"));
                entry = entries[index];
                var thumb = entry.getThumbnail();
                var html = "";
                if (thumb) {
                    html = HU.image(thumb, ["width", "300;"]) + "<br>";
                } else if (entry.isImage()) {
                    html += HU.image(entry.getResourceUrl(), ["width", "300"]) + "<br>";
                }
                html += entry.getIconImage() + " " + entry.getName() + "<br>";
                var start = entry.getStartDate().getUTCFullYear() + "-" + Utils.padLeft(entry.getStartDate().getUTCMonth() + 1, 2, "0") + "-" + Utils.padLeft(entry.getStartDate().getUTCDate(), 2, "0");
                var end = entry.getEndDate().getUTCFullYear() + "-" + Utils.padLeft(entry.getEndDate().getUTCMonth() + 1, 2, "0") + "-" + Utils.padLeft(entry.getEndDate().getUTCDate(), 2, "0");
                html += "Date: " + start + " - " + end + " UTC";
                _this.gridPopup.html(html);
                _this.gridPopup.show();
                _this.gridPopup.position({
                    of: $(this),
                    at: "left bottom",
                    my: "left top",
                    collision: "none none"
                });
                _this.gridPopup.position({
                    of: $(this),
                    my: "left top",
                    at: "left bottom",
                    collision: "none none"
                });
            });
        },
        makeFramework: function(entries) {
            var html = "";
            var mouseInfo = "click:zoom in;shift-click:zoom out;command/ctrl click: reset";
            html += HU.openDiv(["class", "display-grid", "id", this.getDomId(ID_GRID)]);
            html += HU.div(["class", "display-grid-popup ramadda-popup"], "");
            html += HU.openTag("table", ["border", "0", "class", "", "cellspacing", "0", "cellspacing", "0", "width", "100%", "style", "height:100%;"]);
            html += HU.openTag("tr", ["valign", "bottom"]);
            html += HU.tag("td");
            html += HU.tag("td", [], HU.div(["id", this.getDomId(ID_LINKS)], ""));
            html += HU.closeTag("tr");
            html += HU.openTag("tr", ["style", "height:100%;"]);
            html += HU.openTag("td", ["style", "height:100%;"]);
            html += HU.openDiv(["class", "display-grid-axis-left ramadda-noselect", "id", this.getDomId(ID_AXIS_LEFT)]);
            html += HU.closeDiv();
            html += HU.closeDiv();
            html += HU.closeTag("td");
            html += HU.openTag("td", ["style", "height:" + this.getProperty("height", "400") + "px"]);
            html += HU.openDiv(["class", "display-grid-canvas ramadda-noselect", "id", this.getDomId(ID_CANVAS)]);
            html += HU.closeDiv();
            html += HU.closeDiv();
            html += HU.closeTag("td");
            html += HU.closeTag("tr");
            html += HU.openTag("tr", []);
            html += HU.tag("td", ["width", "100"], "&nbsp;");
            html += HU.openTag("td", []);
            html += HU.div(["class", "display-grid-axis-bottom ramadda-noselect", "title", mouseInfo, "id", this.getDomId(ID_AXIS_BOTTOM)], "");
            html += HU.closeTag("table");
            html += HU.closeTag("td");
            return html;
        },


        getXAxisType: function() {
            return this.getProperty(ID_XAXIS_TYPE, "date");
        },
        getYAxisType: function() {
            return this.getProperty(ID_YAXIS_TYPE, "month");
        },
        getXAxisAscending: function() {
            return this.getProperty(ID_XAXIS_ASCENDING, true);
        },
        getYAxisAscending: function() {
            return this.getProperty(ID_YAXIS_ASCENDING, true);
        },
        getXAxisScale: function() {
            return this.getProperty(ID_XAXIS_SCALE, true);
        },
        getYAxisScale: function() {
            return this.getProperty(ID_YAXIS_SCALE, false);
        },


        makeGrid: function(entries) {
            var showIcon = this.getProperty(ID_SHOW_ICON, true);
            var showName = this.getProperty(ID_SHOW_NAME, true);

            if (!this.minDate) {
                var minDate = null;
                var maxDate = null;
                for (var i = 0; i < entries.length; i++) {
                    var entry = entries[i];
                    minDate = minDate == null ? entry.getStartDate() : (minDate.getTime() > entry.getStartDate().getTime() ? entry.getStartDate() : minDate);
                    maxDate = maxDate == null ? entry.getEndDate() : (maxDate.getTime() < entry.getEndDate().getTime() ? entry.getEndDate() : maxDate);
                }
                this.minDate = new Date(Date.UTC(minDate.getUTCFullYear(), 0, 1));
                this.maxDate = new Date(Date.UTC(maxDate.getUTCFullYear() + 1, 0, 1));
            }

            var axis = {
                width: this.canvas.width(),
                height: this.canvas.height(),
                Y: {
                    vertical: true,
                    axisType: this.getYAxisType(),
                    ascending: this.getYAxisAscending(),
                    scale: this.getYAxisScale(),
                    skip: 1,
                    maxTicks: Math.ceil(this.canvas.height() / 80),
                    minDate: this.minDate,
                    maxDate: this.maxDate,
                    ticks: [],
                    lines: "",
                    html: "",
                    minDate: this.minDate,
                    maxDate: this.maxDate,
                },
                X: {
                    vertical: false,
                    axisType: this.getXAxisType(),
                    ascending: this.getXAxisAscending(),
                    scale: this.getXAxisScale(),
                    skip: 1,
                    maxTicks: Math.ceil(this.canvas.width() / 80),
                    minDate: this.minDate,
                    maxDate: this.maxDate,
                    ticks: [],
                    lines: "",
                    html: ""
                }
            }
            if (!this.axis) {
                this.axis = axis;
            } else {
                if (this.axis.X.minDate) {
                    axis.X.minDate = this.axis.X.minDate;
                    axis.X.maxDate = this.axis.X.maxDate;
                } else {
                    this.axis.X.minDate = axis.X.minDate;
                    this.axis.X.maxDate = axis.X.maxDate;
                }
                if (this.axis.Y.minDate) {
                    axis.Y.minDate = this.axis.Y.minDate;
                    axis.Y.maxDate = this.axis.Y.maxDate;
                } else {
                    this.axis.Y.minDate = axis.Y.minDate;
                    this.axis.Y.maxDate = axis.Y.maxDate;
                }
            }

            if (axis.Y.axisType == "size") {
                this.calculateSizeAxis(axis.Y);
            } else if (axis.Y.axisType == "date") {
                this.calculateDateAxis(axis.Y);
            } else {
                this.calculateMonthAxis(axis.Y);
            }
            for (var i = 0; i < axis.Y.ticks.length; i++) {
                var tick = axis.Y.ticks[i];
                var style = (axis.Y.ascending ? "bottom:" : "top:") + tick.percent + "%;";
                var style = "bottom:" + tick.percent + "%;";
                var lineClass = tick.major ? "display-grid-hline-major" : "display-grid-hline";
                axis.Y.lines += HU.div(["style", style, "class", lineClass], " ");
                axis.Y.html += HU.div(["style", style, "class", "display-grid-axis-left-tick"], tick.label + " " + HU.div(["class", "display-grid-htick"], ""));
            }

            if (axis.X.axisType == "size") {
                this.calculateSizeAxis(axis.X);
            } else if (axis.X.axisType == "date") {
                this.calculateDateAxis(axis.X);
            } else {
                this.calculateMonthAxis(axis.X);
            }
            for (var i = 0; i < axis.X.ticks.length; i++) {
                var tick = axis.X.ticks[i];
                if (tick.percent > 0) {
                    var lineClass = tick.major ? "display-grid-vline-major" : "display-grid-vline";
                    axis.X.lines += HU.div(["style", "left:" + tick.percent + "%;", "class", lineClass], " ");
                }
                axis.X.html += HU.div(["style", "left:" + tick.percent + "%;", "class", "display-grid-axis-bottom-tick"], HU.div(["class", "display-grid-vtick"], "") + " " + tick.label);
            }

            var items = "";
            var seen = {};
            for (var i = 0; i < entries.length; i++) {
                var entry = entries[i];
                var vInfo = this[axis.Y.calculatePercent].call(this, entry, axis.Y);
                var xInfo = this[axis.X.calculatePercent].call(this, entry, axis.X);
                if (vInfo.p1 < 0) {
                    vInfo.p2 = vInfo.p2 + vInfo.p1;
                    vInfo.p1 = 0;
                }
                if (vInfo.p1 + vInfo.p2 > 100) {
                    vInfo.p2 = 100 - vInfo.p1;
                }

                var style = "";
                var pos = "";

                if (axis.X.ascending) {
                    style += "left:" + xInfo.p1 + "%;";
                    pos += "left:" + xInfo.p1 + "%;";
                } else {
                    style += "right:" + xInfo.p1 + "%;";
                    pos += "left:" + (100 - xInfo.p2) + "%;";
                }

                if (axis.X.scale) {
                    if (xInfo.delta > 1) {
                        style += "width:" + xInfo.delta + "%;";
                    } else {
                        style += "width:" + this.getProperty("fixedWidth", "5") + "px;";
                    }
                }


                var namePos = pos;
                if (axis.Y.ascending) {
                    style += " bottom:" + vInfo.p2 + "%;";
                    pos += " bottom:" + vInfo.p2 + "%;";
                    namePos += " bottom:" + vInfo.p2 + "%;";
                } else {
                    style += " top:" + vInfo.p2 + "%;";
                    pos += " top:" + vInfo.p2 + "%;";
                    namePos += " top:" + vInfo.p2 + "%;";
                    namePos += "margin-top:-15px;"
                }
                if (axis.Y.scale) {
                    if (vInfo.p2 > 1) {
                        style += "height:" + vInfo.delta + "%;";
                    } else {
                        style += "height:" + this.getProperty("fixedHeight", "5") + "px;";
                    }
                }

                if (entry.getName().includes("rilsd")) {
                    console.log("pos:" + namePos);
                }
                if (showIcon) {
                    items += HU.div(["class", "display-grid-entry-icon display-grid-entry", "entryid", entry.getId(), "index", i, "style", pos], entry.getIconImage());
                }
                var key = Math.round(xInfo.p1) + "---" + Math.round(vInfo.p1);
                if (showName && !seen[key]) {
                    seen[key] = true;
                    var name = entry.getName().replace(/ /g, "&nbsp;");
                    items += HU.div(["class", "display-grid-entry-text display-grid-entry", "entryid", entry.getId(), "index", i, "style", namePos], name);
                }
                var boxStyle = style + "background:" + this.getProperty(ID_COLOR, "lightblue");
                items += HU.div(["class", "display-grid-entry-box display-grid-entry", "itemtype", "box", "entryid", entry.getId(), "style", boxStyle, "index", i], "");
            }
            this.jq(ID_AXIS_LEFT).html(axis.Y.html);
            this.jq(ID_CANVAS).html(axis.Y.lines + axis.X.lines + items);
            this.jq(ID_AXIS_BOTTOM).html(axis.X.html);
            this.initGrid(entries);
        },
        calculateSizeAxis: function(axisInfo) {
            var min = Number.MAX_VALUE;
            var max = Number.MIN_VALUE;
            for (var i = 0; i < this.entries.length; i++) {
                var entry = this.entries[i];
                min = Math.min(min, entry.getSize());
                max = Math.max(max, entry.getSize());
            }
        },
        checkOrder: function(axisInfo, percents) {
            /*
            if(!axisInfo.ascending) {
                percents.p1 = 100-percents.p1;
                percents.p2 = 100-percents.p2;
                var tmp  =percents.p1;
                percents.p1=percents.p2;
                percents.p2=tmp;
            }
            */
            return {
                p1: percents.p1,
                p2: percents.p2,
                delta: Math.abs(percents.p2 - percents.p1)
            };
        },
        calculateDatePercent: function(entry, axisInfo) {
            var p1 = 100 * (entry.getStartDate().getTime() - axisInfo.min) / axisInfo.range;
            var p2 = 100 * (entry.getEndDate().getTime() - axisInfo.min) / axisInfo.range;
            return this.checkOrder(axisInfo, {
                p1: p1,
                p2: p2,
                delta: Math.abs(p2 - p1)
            });
        },
        calculateMonthPercent: function(entry, axisInfo) {
            var d1 = entry.getStartDate();
            var d2 = entry.getEndDate();
            var t1 = new Date(Date.UTC(1, d1.getUTCMonth(), d1.getUTCDate()));
            var t2 = new Date(Date.UTC(1, d2.getUTCMonth(), d2.getUTCDate()));
            var p1 = 100 * ((t1.getTime() - axisInfo.min) / axisInfo.range);
            var p2 = 100 * ((t2.getTime() - axisInfo.min) / axisInfo.range);
            if (entry.getName().includes("rilsd")) {
                console.log("t1:" + t1);
                console.log("t2:" + t2);
                console.log("before:" + p1 + " " + p2);
            }
            return this.checkOrder(axisInfo, {
                p1: p1,
                p2: p2,
                delta: Math.abs(p2 - p1)
            });
        },
        calculateMonthAxis: function(axisInfo) {
            axisInfo.calculatePercent = "calculateMonthPercent";
            axisInfo.minDate = new Date(Date.UTC(0, 11, 15));
            axisInfo.maxDate = new Date(Date.UTC(1, 11, 31));
            axisInfo.min = axisInfo.minDate.getTime();
            axisInfo.max = axisInfo.maxDate.getTime();
            axisInfo.range = axisInfo.max - axisInfo.min;
            var months = Utils.getMonthShortNames();
            for (var month = 0; month < months.length; month++) {
                var t1 = new Date(Date.UTC(1, month));
                var percent = (axisInfo.maxDate.getTime() - t1.getTime()) / axisInfo.range;
                if (axisInfo.ascending)
                    percent = 1 - percent;
                axisInfo.ticks.push({
                    percent: 100 * percent,
                    label: months[month],
                    major: false
                });
            }
        },
        calculateDateAxis: function(axisInfo) {
            axisInfo.calculatePercent = "calculateDatePercent";
            var numYears = axisInfo.maxDate.getUTCFullYear() - axisInfo.minDate.getUTCFullYear();
            var years = numYears;
            axisInfo.type = "year";
            axisInfo.skip = Math.max(1, Math.floor(numYears / axisInfo.maxTicks));
            if ((numYears / axisInfo.skip) <= (axisInfo.maxTicks / 2)) {
                var numMonths = 0;
                var tmp = new Date(axisInfo.minDate.getTime());
                while (tmp.getTime() < axisInfo.maxDate.getTime()) {
                    Utils.incrementMonth(tmp);
                    numMonths++;
                }
                axisInfo.skip = Math.max(1, Math.floor(numMonths / axisInfo.maxTicks));
                axisInfo.type = "month";
                if ((numMonths / axisInfo.skip) <= (axisInfo.maxTicks / 2)) {
                    var tmp = new Date(axisInfo.minDate.getTime());
                    var numDays = 0;
                    while (tmp.getTime() < axisInfo.maxDate.getTime()) {
                        Utils.incrementDay(tmp);
                        numDays++;
                    }
                    axisInfo.skip = Math.max(1, Math.floor(numDays / axisInfo.maxTicks));
                    axisInfo.type = "day";
                }
            }


            axisInfo.min = axisInfo.minDate.getTime();
            axisInfo.max = axisInfo.maxDate.getTime();
            axisInfo.range = axisInfo.max - axisInfo.min;
            var months = Utils.getMonthShortNames();
            var lastYear = null;
            var lastMonth = null;
            var tickDate;
            if (axisInfo.type == "year") {
                tickDate = new Date(Date.UTC(axisInfo.minDate.getUTCFullYear()));
            } else if (axisInfo.type == "month") {
                tickDate = new Date(Date.UTC(axisInfo.minDate.getUTCFullYear(), axisInfo.minDate.getUTCMonth()));
            } else {
                tickDate = new Date(Date.UTC(axisInfo.minDate.getUTCFullYear(), axisInfo.minDate.getUTCMonth(), axisInfo.minDate.getUTCDate()));
            }
            //                if(axisInfo.vertical)
            //                    console.log(axisInfo.type+" skip:" + axisInfo.skip + "   min:" + Utils.formatDateYYYYMMDD(axisInfo.minDate)+"   max:" + Utils.formatDateYYYYMMDD(axisInfo.maxDate));
            while (tickDate.getTime() < axisInfo.maxDate.getTime()) {
                var percent = (tickDate.getTime() - axisInfo.minDate.getTime()) / axisInfo.range;
                if (!axisInfo.ascending)
                    percent = (1 - percent);
                percent = 100 * percent;
                //                    console.log("    perc:"+ percent +" " + Utils.formatDateYYYYMMDD(tickDate));
                if (percent >= 0 && percent < 100) {
                    var label = "";
                    var year = tickDate.getUTCFullYear();
                    var month = tickDate.getUTCMonth();
                    var major = false;
                    if (axisInfo.type == "year") {
                        label = year;
                    } else if (axisInfo.type == "month") {
                        label = months[tickDate.getUTCMonth()];
                        if (lastYear != year) {
                            label = label + "<br>" + year;
                            lastYear = year;
                            major = true;
                        }
                    } else {
                        label = tickDate.getUTCDate();
                        if (lastYear != year || lastMonth != month) {
                            label = label + "<br>" + months[month] + " " + year;
                            lastYear = year;
                            lastMonth = month;
                            major = true;
                        }
                    }
                    axisInfo.ticks.push({
                        percent: percent,
                        label: label,
                        major: major
                    });
                }
                if (axisInfo.type == "year") {
                    Utils.incrementYear(tickDate, axisInfo.skip);
                } else if (axisInfo.type == "month") {
                    Utils.incrementMonth(tickDate, axisInfo.skip);
                } else {
                    Utils.incrementDay(tickDate, axisInfo.skip);
                }
            }

        }
    });
}


function RamaddaMetadataDisplay(displayManager, id, properties) {
    if (properties.formOpen == null) {
        properties.formOpen = false;
    }
    var SUPER;
    RamaddaUtil.inherit(this, SUPER = new RamaddaSearcherDisplay(displayManager, id, DISPLAY_METADATA, properties));
    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
        haveDisplayed: false,
        initDisplay: function() {
            this.createUI();
            this.setContents(this.getDefaultHtml());
	    this.initHtml();
            SUPER.initDisplay.apply(this);
            if (this.haveDisplayed && this.entryList) {
                this.entryListChanged(this.entryList);
            }
            this.haveDisplayed = true;
        },
        entryListChanged: function(entryList) {
            this.entryList = entryList;
            var entries = this.entryList.getEntries();
            if (entries.length == 0) {
                this.writeMessage("Nothing found");
                return;
            }
            var mdtsFromEntries = [];
            var mdtmap = {};
            var tmp = {};
            for (var i = 0; i < entries.length; i++) {
                var entry = entries[i];
                var metadata = entry.getMetadata();
                for (var j = 0; j < metadata.length; j++) {
                    var m = metadata[j];
                    if (tmp[m.type] == null) {
                        tmp[m.type] = "";
                        mdtsFromEntries.push(m.type);
                    }
                    mdtmap[metadata[j].type] = metadata[j].label;
                }
            }

            var html = "";
            html += HU.openTag(TAG_TABLE, ["id", this.getDomId("table"), ATTR_CLASS, "cell-border stripe ramadda-table", ATTR_WIDTH, "100%", "cellpadding", "5", "cellspacing", "0"]);
            html += "<thead>"
            var type = this.findEntryType(this.searchSettings.entryType);
            var typeName = "Entry";
            if (type != null) {
                typeName = type.getLabel();
            }
	    this.writeMessage(this.getResultsHeader(entries));
            var mdts = null;
            //Get the metadata types to show from either a property or
            //gather them from all of the entries
            // e.g., "project_pi,project_person,project_funding"
            var prop = this.getProperty("metadataTypes", null);
            if (prop != null) {
                mdts = prop.split(",");
            } else {
                mdts = mdtsFromEntries;
                mdts.sort();
            }

            var skip = {
                "content.pagestyle": true,
                "content.pagetemplate": true,
                "content.sort": true,
                "spatial.polygon": true,
            };
            var headerItems = [];
            headerItems.push(HU.th([], HU.b(typeName)));
            for (var i = 0; i < mdts.length; i++) {
                var type = mdts[i];
                if (skip[type]) {
                    continue;
                }
                var label = mdtmap[mdts[i]];
                if (label == null) label = mdts[i];
                headerItems.push(HU.th([], HU.b(label)));
            }
            var headerRow = HU.tr(["valign", "bottom"], HU.join(headerItems, ""));
            html += headerRow;
            html += "</thead><tbody>"
            var divider = "<div class=display-metadata-divider></div>";
            var missing = this.missingMessage;
            if (missing = null) missing = "&nbsp;";
            for (var entryIdx = 0; entryIdx < entries.length; entryIdx++) {
                var entry = entries[entryIdx];
                var metadata = entry.getMetadata();
                var row = [];
                var buttonId = this.getDomId("entrylink" + entry.getIdForDom());
                var link = entry.getLink(entry.getIconImage() + " " + entry.getName());
                row.push(HU.td([], HU.div([ATTR_CLASS, "display-metadata-entrylink"], link)));
                for (var mdtIdx = 0; mdtIdx < mdts.length; mdtIdx++) {
                    var mdt = mdts[mdtIdx];
                    if (skip[mdt]) {
                        continue;
                    }
                    var cell = null;
                    for (var j = 0; j < metadata.length; j++) {
                        var m = metadata[j];
                        if (m.type == mdt) {
                            var item = null;
                            if (m.type == "content.thumbnail" || m.type == "content.logo") {
                                var url = this.getRamadda().getRoot() + "/metadata/view/" + m.value.attr1 + "?element=1&entryid=" + entry.getId() + "&metadata_id=" + m.id;
                                item = HU.image(url, [ATTR_WIDTH, "100"]);
                            } else if (m.type == "content.url" || m.type == "dif.related_url") {
                                var label = m.value.attr2;
                                if (label == null || label == "") {
                                    label = m.value.attr1;
                                }
                                item = HU.href(m.value.attr1, label);
                            } else if (m.type == "content.attachment") {
                                var toks = m.value.attr1.split("_file_");
                                var filename = toks[1];
                                var url = this.getRamadda().getRoot() + "/metadata/view/" + m.value.attr1 + "?element=1&entryid=" + entry.getId() + "&metadata_id=" + m.id;
                                item = HU.href(url, filename);
                            } else {
                                item = m.value.attr1;
                                //                                    console.log("Item:" + item);
                                if (m.value.attr2 && m.value.attr2.trim().length > 0) {
                                    item += " - " + m.value.attr2;
                                }
                            }
                            if (item != null) {
                                if (cell == null) {
                                    cell = "";
                                } else {
                                    cell += divider;
                                }
                                cell += HU.div([ATTR_CLASS, "display-metadata-item"], item);
                            }

                        }
                    }
                    if (cell == null) {
                        cell = missing;
                    }
                    if (cell == null) {
                        cell = "";
                    }
                    var add = HU.tag(TAG_A, [ATTR_STYLE, "color:#000;", ATTR_HREF, this.getRamadda().getRoot() + "/metadata/addform?entryid=" + entry.getId() + "&metadata_type=" + mdt,
                        "target", "_blank", "alt", "Add metadata", ATTR_TITLE, "Add metadata"
                    ], "+");
                    add = HU.div(["class", "display-metadata-table-add"], add);
                    var cellContents = add + divider;
                    if (cell.length > 0) {
                        cellContents += cell;
                    }
                    row.push(HU.td([], HU.div([ATTR_CLASS, "display-metadata-table-cell-contents"], cellContents)));
                }
                html += HU.tr(["valign", "top"], HU.join(row, ""));
                //Add in the header every 10 rows
                if (((entryIdx + 1) % 10) == 0) html += headerRow;
            }
            html += "</tbody>"
            html += HU.closeTag(TAG_TABLE);
            this.writeEntries(html, entries);
            HU.formatTable("#" + this.getDomId("table"), {
                scrollY: 400
            });
        },
    });

}




function RamaddaEntrytimelineDisplay(displayManager, id, properties) {
    if (properties.formOpen == null) {
        properties.formOpen = false;
    }
    var SUPER;
    RamaddaUtil.inherit(this, SUPER = new RamaddaSearcherDisplay(displayManager, id, DISPLAY_ENTRYTIMELINE, properties));
    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
        initDisplay: function() {
            this.createUI();
            this.setContents(this.getDefaultHtml());
	    this.initHtml();
            SUPER.initDisplay.apply(this);
        },
        entryListChanged: function(entryList) {
            this.entryList = entryList;
            var entries = this.entryList.getEntries();
            var html = "";
            if (entries.length == 0) {
                this.writeMessage("Nothing found");
                return;
            }

            var data = {
                "timeline": {
                    "headline": "The Main Timeline Headline Goes here",
                    "type": "default",
                    "text": "<p>Intro body text goes here, some HTML is ok</p>",
                    "asset": {
                        "media": "http://yourdomain_or_socialmedialink_goes_here.jpg",
                        "credit": "Credit Name Goes Here",
                        "caption": "Caption text goes here"
                    },
                    "date": [{
                            "startDate": "2011,12,10",
                            "endDate": "2011,12,11",
                            "headline": "Headline Goes Here",
                            "text": "<p>Body text goes here, some HTML is OK</p>",
                            "tag": "This is Optional",
                            "classname": "optionaluniqueclassnamecanbeaddedhere",
                            "asset": {
                                "media": "http://twitter.com/ArjunaSoriano/status/164181156147900416",
                                "thumbnail": "optional-32x32px.jpg",
                                "credit": "Credit Name Goes Here",
                                "caption": "Caption text goes here"
                            }
                        }, {
                            "startDate": "2012,12,10",
                            "endDate": "2012,12,11",
                            "headline": "Headline Goes Here",
                            "text": "<p>Body text goes here, some HTML is OK</p>",
                            "tag": "This is Optional",
                            "classname": "optionaluniqueclassnamecanbeaddedhere",
                            "asset": {
                                "media": "http://twitter.com/ArjunaSoriano/status/164181156147900416",
                                "thumbnail": "optional-32x32px.jpg",
                                "credit": "Credit Name Goes Here",
                                "caption": "Caption text goes here"
                            }
                        }, {
                            "startDate": "2013,12,10",
                            "endDate": "2013,12,11",
                            "headline": "Headline Goes Here",
                            "text": "<p>Body text goes here, some HTML is OK</p>",
                            "tag": "This is Optional",
                            "classname": "optionaluniqueclassnamecanbeaddedhere",
                            "asset": {
                                "media": "http://twitter.com/ArjunaSoriano/status/164181156147900416",
                                "thumbnail": "optional-32x32px.jpg",
                                "credit": "Credit Name Goes Here",
                                "caption": "Caption text goes here"
                            }
                        }

                    ],
                    "era": [{
                            "startDate": "2011,12,10",
                            "endDate": "2011,12,11",
                            "headline": "Headline Goes Here",
                            "text": "<p>Body text goes here, some HTML is OK</p>",
                            "tag": "This is Optional"
                        }

                    ]
                }
            };


            for (var i = 0; i < entries.length; i++) {
                var entry = entries[i];

            }
            createStoryJS({
                type: 'timeline',
                width: '800',
                height: '600',
                source: data,
                embed_id: this.getDomId(ID_ENTRIES),
            });

        },
    });

}







function RamaddaEntrydisplayDisplay(displayManager, id, properties) {
    var SUPER;
    var e = new Error();

    $.extend(this, {
        sourceEntry: properties.sourceEntry
    });
    RamaddaUtil.inherit(this, SUPER = new RamaddaDisplay(displayManager, id, DISPLAY_ENTRYDISPLAY, properties));
    if (properties.sourceEntry == null && properties.entryId != null) {
        var _this = this;
        var f = async function() {
            await _this.getEntry(properties.entryId, entry => {
                _this.sourceEntry = entry;
                _this.initDisplay()
            });

        }
        f();
    }


    addRamaddaDisplay(this);
    $.extend(this, {
        selectedEntry: null,
        initDisplay: function() {
            this.createUI();
            var title = this.title;
            if (this.sourceEntry != null) {
                this.addEntryHtml(this.sourceEntry);
                var url = this.sourceEntry.getEntryUrl();

                if (title == null) {
                    title = this.sourceEntry.getName();
                }
                title = HU.tag("a", ["href", url, "title", this.sourceEntry.getName(), "alt", this.sourceEntry.getName()], title);
            } else {
                this.addEntryHtml(this.selectedEntry);
                if (title == null) {
                    title = "Entry Display";
                }
            }
            this.setDisplayTitle(title);
        },
        handleEventEntrySelection: function(source, args) {
            //Ignore select events
            if (this.sourceEntry != null) return;
            var selected = args.selected;
            var entry = args.entry;
            if (!selected) {
                if (this.selectedEntry != entry) {
                    //not mine
                    return;
                }
                this.selectedEntry = null;
                this.setContents("");
                return;
            }
            this.selectedEntry = entry;
            this.addEntryHtml(this.selectedEntry);
        },
        getEntries: function() {
            return [this.sourceEntry];
        },
        addEntryHtml: function(entry) {
            if (entry == null) {
                this.setContents("&nbsp;");
                return;
            }
            var html = this.getEntryHtml(entry, {
                showHeader: false
            });
            var height = this.getProperty("height", "400px");
            if (!height.endsWith("px")) height += "px";
            this.setContents(HU.div(["class", "display-entry-description", "style", "height:" + height + ";"],
                html));
            this.entryHtmlHasBeenDisplayed(entry);
        },
    });
}



function RamaddaEntrytitleDisplay(displayManager, id, properties) {
    var SUPER;
    $.extend(this, {
        sourceEntry: properties.sourceEntry
    });
    RamaddaUtil.inherit(this, SUPER = new RamaddaDisplay(displayManager, id, DISPLAY_ENTRYDISPLAY, properties));
    if (properties.sourceEntry == null && properties.entryId != null) {
        var _this = this;
        var f = async function() {
            await _this.getEntry(properties.entryId, entry => {
                _this.sourceEntry = entry;
                _this.initDisplay()
            });
        }
        f();
    }

    let myProps = [
	{label:'Entry Title'},
	{p:'template',ex:'<b>${icon} ${name} Date: ${date} ${start_date} ${end_date} ${entry_attribute...}</b>'},
	{p:'showLink',ex:'false'}
    ];
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
	initDisplay: function() {
            this.createUI();
	    let html = "";
	    if(this.sourceEntry) {
		let e = this.sourceEntry;
		html = this.getProperty("template","<b>${icon} ${name} Date: ${date} Sonde: ${sonde}</b>");
		html = html.replace("${name}",e.getDisplayName());
		html = html.replace("${icon}",e.getIconImage());
		html = html.replace("${date}",this.formatDate(e.getStartDate()));
		html = html.replace("${start_date}",this.formatDate(e.getStartDate()));
		html = html.replace("${end_date}",this.formatDate(e.getEndDate()));
		e.getAttributeNames().map(n=>{
		    html = html.replace("${" + n+"}",e.getAttributeValue(n));
		});
		if(this.getProperty("showLink",true)) {
		    html = HU.href(e.getEntryUrl(),html);
		}
	    }
	    this.displayHtml(html);
        },
	setEntry: function(entry) {
	    this.sourceEntry = entry;
	    this.initDisplay();
	},
        handleEventEntrySelection: function(source, args) {
        },
    });
}





function RamaddaOperandsDisplay(displayManager, id, properties) {
    var ID_SELECT = TAG_SELECT;
    var ID_SELECT1 = "select1";
    var ID_SELECT2 = "select2";
    var ID_NEWDISPLAY = "newdisplay";

    $.extend(this, new RamaddaEntryDisplay(displayManager, id, DISPLAY_OPERANDS, properties));
    addRamaddaDisplay(this);
    $.extend(this, {
        baseUrl: null,
        initDisplay: function() {
            this.createUI();
            this.baseUrl = this.getRamadda().getSearchUrl(this.searchSettings, OUTPUT_JSON);
            if (this.entryList == null) {
                this.entryList = new EntryList(this.getRamadda(), jsonUrl, this);
            }
            var html = "";
            html += HU.div([ATTR_ID, this.domId(ID_ENTRIES), ATTR_CLASS, this.getClass("entries")], "");
            this.setContents(html);
        },
        entryListChanged: function(entryList) {
            var html = "<form>";
            html += "<p>";
            html += HU.openTag(TAG_TABLE, [ATTR_CLASS, "formtable", "cellspacing", "0", "cellspacing", "0"]);
            var entries = this.entryList.getEntries();
            var get = this.getGet();

            for (var j = 1; j <= 2; j++) {
                var select = HU.openTag(TAG_SELECT, [ATTR_ID, this.getDomId(ID_SELECT + j)]);
                select += HU.tag(TAG_OPTION, [ATTR_TITLE, "", ATTR_VALUE, ""],
                    "-- Select --");
                for (var i = 0; i < entries.length; i++) {
                    var entry = entries[i];
                    var label = entry.getIconImage() + " " + entry.getName();
                    select += HU.tag(TAG_OPTION, [ATTR_TITLE, entry.getName(), ATTR_VALUE, entry.getId()],
                        entry.getName());

                }
                select += HU.closeTag(TAG_SELECT);
                html += HU.formEntry("Data:", select);
            }

            var select = HU.openTag(TAG_SELECT, [ATTR_ID, this.getDomId(ID_CHARTTYPE)]);
            select += HU.tag(TAG_OPTION, [ATTR_TITLE, "", ATTR_VALUE, "linechart"],
                "Line chart");
            select += HU.tag(TAG_OPTION, [ATTR_TITLE, "", ATTR_VALUE, "barchart"],
                "Bar chart");
            select += HU.tag(TAG_OPTION, [ATTR_TITLE, "", ATTR_VALUE, "barstack"],
                "Stacked bars");
            select += HU.tag(TAG_OPTION, [ATTR_TITLE, "", ATTR_VALUE, "bartable"],
                "Bar table");
            select += HU.tag(TAG_OPTION, [ATTR_TITLE, "", ATTR_VALUE, "piechart"],
                "Pie chart");
            select += HU.tag(TAG_OPTION, [ATTR_TITLE, "", ATTR_VALUE, "scatterplot"],
                "Scatter Plot");
            select += HU.closeTag(TAG_SELECT);
            html += HU.formEntry("Chart Type:", select);

            html += HU.closeTag(TAG_TABLE);
            html += "<p>";
            html += HU.tag(TAG_DIV, [ATTR_CLASS, "display-button", ATTR_ID, this.getDomId(ID_NEWDISPLAY)], "New Chart");
            html += "<p>";
            html += "</form>";
            this.writeEntries(html);
            var theDisplay = this;
            this.jq(ID_NEWDISPLAY).button().click(function(event) {
                theDisplay.createDisplay();
            });
        },
        createDisplay: function() {
            var entry1 = this.getEntry(this.jq(ID_SELECT1).val());
            var entry2 = this.getEntry(this.jq(ID_SELECT2).val());
            if (entry1 == null) {
                alert("No data selected");
                return;
            }
            var pointDataList = [];

            pointDataList.push(new PointData(entry1.getName(), null, null, ramaddaBaseUrl + "/entry/show?&output=points.product&product=points.json&numpoints=1000&entryid=" + entry1.getId()));
            if (entry2 != null) {
                pointDataList.push(new PointData(entry2.getName(), null, null, ramaddaBaseUrl + "/entry/show?&output=points.product&product=points.json&numpoints=1000&entryid=" + entry2.getId()));
            }

            //Make up some functions
            var operation = "average";
            var derivedData = new DerivedPointData(this.displayManager, "Derived Data", pointDataList, operation);
            var pointData = derivedData;
            var chartType = this.jq(ID_CHARTTYPE).val();
            displayManager.createDisplay(chartType, {
                "layoutFixed": false,
                "data": pointData
            });
        }

    });
}


function RamaddaRepositoriesDisplay(displayManager, id, properties) {
    RamaddaUtil.inherit(this, new RamaddaEntryDisplay(displayManager, id, DISPLAY_REPOSITORIES, properties));
    addRamaddaDisplay(this);
    RamaddaUtil.defineMembers(this, {
        initDisplay: function() {
            var theDisplay = this;
            this.createUI();
            var html = "";
            if (this.ramaddas.length == 0) {
                html += this.getMessage("No repositories specified");
            } else {
                html += this.getMessage("Loading repository listing");
            }
            this.numberWithTypes = 0;
            this.finishedInitDisplay = false;
            //Check for and remove the all repositories
            if (this.ramaddas.length > 1) {
                if (this.ramaddas[this.ramaddas.length - 1].getRoot() == "all") {
                    this.ramaddas.splice(this.ramaddas.length - 1, 1);
                }
            }
            for (var i = 0; i < this.ramaddas.length; i++) {
                if (i == 0) {}
                var ramadda = this.ramaddas[i];
                var types = ramadda.getEntryTypes(function(ramadda, types) {
                    theDisplay.gotTypes(ramadda, types);
                });
                if (types != null) {
                    this.numberWithTypes++;
                }
            }
            this.setDisplayTitle("Repositories");
            this.setContents(html);
            this.finishedInitDisplay = true;
            this.displayRepositories();
        },
        displayRepositories: function() {
            if (!this.finishedInitDisplay || this.numberWithTypes != this.ramaddas.length) {
                return;
            }
            var typeMap = {};
            var allTypes = [];
            var html = "";
            html += HU.openTag(TAG_TABLE, [ATTR_CLASS, "display-repositories-table", ATTR_WIDTH, "100%", ATTR_BORDER, "1", "cellspacing", "0", "cellpadding", "5"]);
            for (var i = 0; i < this.ramaddas.length; i++) {
                var ramadda = this.ramaddas[i];
                var types = ramadda.getEntryTypes();
                for (var typeIdx = 0; typeIdx < types.length; typeIdx++) {
                    var type = types[typeIdx];
                    if (typeMap[type.getId()] == null) {
                        typeMap[type.getId()] = type;
                        allTypes.push(type);
                    }
                }
            }

            html += HU.openTag(TAG_TR, ["valign", "bottom"]);
            html += HU.th([ATTR_CLASS, "display-repositories-table-header"], "Type");
            for (var i = 0; i < this.ramaddas.length; i++) {
                var ramadda = this.ramaddas[i];
                var link = HU.href(ramadda.getRoot(), ramadda.getName());
                html += HU.th([ATTR_CLASS, "display-repositories-table-header"], link);
            }
            html += "</tr>";

            var onlyCats = [];
            if (this.categories != null) {
                onlyCats = this.categories.split(",");
            }



            var catMap = {};
            var cats = [];
            for (var typeIdx = 0; typeIdx < allTypes.length; typeIdx++) {
                var type = allTypes[typeIdx];
                var row = "";
                row += "<tr>";
                row += HU.td([], HU.image(type.getIcon()) + " " + type.getLabel());
                for (var i = 0; i < this.ramaddas.length; i++) {
                    var ramadda = this.ramaddas[i];
                    var repoType = ramadda.getEntryType(type.getId());
                    var col = "";
                    if (repoType == null) {
                        row += HU.td([ATTR_CLASS, "display-repositories-table-type-hasnot"], "");
                    } else {
                        var label =
                            HU.tag(TAG_A, ["href", ramadda.getRoot() + "/search/type/" + repoType.getId(), "target", "_blank"],
                                repoType.getEntryCount());
                        row += HU.td([ATTR_ALIGN, "right", ATTR_CLASS, "display-repositories-table-type-has"], label);
                    }

                }
                row += "</tr>";

                var catRows = catMap[type.getCategory()];
                if (catRows == null) {
                    catRows = [];
                    catMap[type.getCategory()] = catRows;
                    cats.push(type.getCategory());
                }
                catRows.push(row);
            }

            for (var i = 0; i < cats.length; i++) {
                var cat = cats[i];
                if (onlyCats.length > 0) {
                    var ok = false;
                    for (var patternIdx = 0; patternIdx < onlyCats.length; patternIdx++) {
                        if (cat == onlyCats[patternIdx]) {
                            ok = true;
                            break;
                        }
                        if (cat.match(onlyCats[patternIdx])) {
                            ok = true;
                            break;

                        }
                    }
                    if (!ok) continue;

                }
                var rows = catMap[cat];
                html += "<tr>";
                html += HU.th(["colspan", "" + (1 + this.ramaddas.length)], cat);
                html += "</tr>";
                for (var row = 0; row < rows.length; row++) {
                    html += rows[row];
                }

            }


            html += HU.closeTag(HU.TAG_TABLE);
            this.setContents(html);
        },
        gotTypes: function(ramadda, types) {
            this.numberWithTypes++;
            this.displayRepositories();
        }
    });
}


var RamaddaGalleryDisplay = RamaddaEntrygalleryDisplay;

function RamaddaSimplesearchDisplay(displayManager, id, properties) {
    let SUPER;
    let myProps = [
	{label:'Simple Search'},
	{p:'resultsPosition',ex:'absolute|relative'},
	{p:'maxHeight',w:300},
	{p:'maxWidth',w:200},
	{p:'maxWidth',w:200},		
	{p:"autoSearch",w:true},
	{p:"showHeader",w:true},
	{p:"inputWidth",w:"100%"},
	{p:"entryType",w:"",tt:"Constrain search to entries of this type"},		
	{p:"entryRoot",w:"this",tt:"Constrain search to this tree"},		
    ];

    RamaddaUtil.inherit(this, SUPER = new RamaddaSearcherDisplay(displayManager, id, DISPLAY_SIMPLESEARCH, properties));
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
	callNumber:1,
        haveDisplayed: false,
        selectedEntries: [],
        getSelectedEntries: function() {
            return this.selectedEntries;
        },
        initDisplay: function() {
            var _this = this;
            if (this.getIsLayoutFixed() && this.haveDisplayed) {
                return;
            }
            this.haveDisplayed = true;
            this.createUI();
            this.setContents(this.getDefaultHtml());
	    this.initHtml();
	    let input = this.jq(ID_TEXT_FIELD);
	    if(this.getAutoSearch(true)) {
		//KEY
		input.keyup(function(event) {
		    _this.getSearchSettings().skip =0;
                    _this.getSearchSettings().max = DEFAULT_MAX;
		    if($(this).val().trim()=="") {
			_this.writeMessage("");
			_this.writeEntries("");			
			if(_this.dialog) {
			    _this.dialog.remove();
			    _this.dialog = null;
			}
			return;
		    }
		    let myCallNumber = ++_this.callNumber;
		    //Wait a bit in case more keys are coming
		    setTimeout(()=>{
			if(myCallNumber == _this.callNumber) {
			    _this.submitSearchForm(true,myCallNumber);
			} else {
			}
		    },250);
		});
	    }

            this.jq(ID_SEARCH).click(function(event) {
		_this.submitSearchForm(false,++_this.callNumber);
                event.preventDefault();
            });
            this.jq(ID_FORM).submit(function(event) {
		_this.submitSearchForm(false,++_this.callNumber);
                event.preventDefault();
            });


            this.jq(ID_TEXT_FIELD).autocomplete({
                source: function(request, callback) {
                }
            });

	},
        getDefaultHtml: function() {
	    let html = this.makeSearchForm();
	    let style="";
	    let abs = (this.getProperty("resultsPosition","absolute")=="absolute");
	    if(!abs) {
		if(this.getMaxHeight(400)) {
		    style+=HU.css("max-height",HU.getDimension(this.getMaxHeight(400)));
		} 
		if(this.getMaxWidth()) {
		    style+=HU.css("width",HU.getDimension(this.getMaxWidth(400)));
		    style+=HU.css("max-width",HU.getDimension(this.getMaxWidth(200)));
		}
		let entries = HU.div([ID,this.domId(ID_ENTRIES),CLASS,"display-simplesearch-entries",STYLE,style]);
		if (this.getShowHeader(true)) {
		    html+=  HU.div([ATTR_CLASS, "display-entries-results", ATTR_ID, this.getDomId(ID_RESULTS)]);
		}
		html+=entries;
	    }
	    return html;
	},
        makeSearchForm: function() {
            var form = HU.openTag("form", [ATTR_ID, this.getDomId(ID_FORM), "action", "#"]);
	    
	    let eg = this.getEgText();
	    let text  = this.getFormText();
	    let size = this.getPropertyInputWidth("100%");
            var textField = HU.input("", text, [STYLE, HU.css("width", size), "placeholder", eg, ATTR_CLASS, "display-search-input", ATTR_ID, this.getDomId(ID_TEXT_FIELD)]);

	    form += textField;
            form += "<input type=\"submit\" style=\"position:absolute;left:-9999px;width:1px;height:1px;\"/>";
            form += HU.closeTag("form");
	    form+=HU.div([ID,this.domId(ID_FORM)]);
            return form;
	},
	handleNoEntries: function() {
	    this.writeEntries("",[]);
            this.writeMessage("Nothing found");
            this.getDisplayManager().handleEventEntriesChanged(this, []);
	},
	writeMessage: function(msg) {
	    this.makeDialog();
	    SUPER.writeMessage.call(this,msg);
	},

	makeDialog: function() {
	    if(this.dialog && (this.dialog.parent()==null ||this.dialog.parent().length==0)) this.dialog = null;
	    if(!this.dialog) {
                let header = HU.div([ATTR_CLASS, "display-entries-results", ATTR_ID, this.getDomId(ID_RESULTS)], "&nbsp;");
                let entries= HU.div([CLASS,"display-entries-entries", ATTR_ID, this.getDomId(ID_ENTRIES)], "");		
		this.dialog = HU.makeDialog({content:header+entries,anchor:this.getContents(),
					     draggable:true,header:true});
	    } else {
		this.dialog.show();
	    }
	},	    
	writeEntries: function(msg, entries) {
	    let abs = this.getProperty("resultsPosition","absolute")=="absolute";
	    if(!abs) {
		this.jq(ID_ENTRIES).html(msg);
		return;
	    }
	    this.makeDialog();

	    if(Utils.stringDefined(msg)) {
		this.jq(ID_ENTRIES).html(msg);
		this.writeMessage(this.getResultsHeader(entries,true));
	    } else {
		this.jq(ID_ENTRIES).html("");
	    }
	},

        submitSearchForm: function(auto, callNumber) {
	    this.writeMessage(this.getWaitImage() + " " +"Searching...");
	    if(callNumber==null) callNumber = this.callNumber;
            this.haveSearched = true;
            let settings = this.getSearchSettings();
            settings.text = this.getFieldValue(this.getDomId(ID_TEXT_FIELD), settings.text);
	    settings.entryRoot = this.getProperty("entryRoot");
	    settings.entryType = this.getProperty("entryType");	    
            if (this.haveTypes) {
                settings.entryType = this.getFieldValue(this.getDomId(ID_TYPE_FIELD), settings.entryType);
            }
            let theRepository = this.getRamadda()
            let jsonUrl = this.makeSearchUrl(this.getRamadda());
            this.entryList = new EntryList(this.getRamadda(), jsonUrl);
	    let success= ()=>{
		if(this.callNumber == callNumber) {
		    this.entryListChanged(this.entryList);
		} 
	    };
	    let fail= (error)=>{
		this.writeEntries("Error:" + error);
	    };	    
	    this.entryList.doSearch(null,success,fail);
	    if(!auto)
		this.updateForSearching(jsonUrl);
        },


        makeDisplayList: function() {
            var entries = this.getSelectedEntriesFromTree();
            if (entries.length == 0) {
                return;
            }
            var props = {
                selectedEntries: entries,
                showForm: false,
                showMenu: true,
                fixedEntries: true
            };
            props.entryList = new EntryList(this.getRamadda(), "", this, false);
            props.entryList.setEntries(entries);
            this.getDisplayManager().createDisplay(DISPLAY_ENTRYLIST, props);
        },
        handleEventEntrySelection: function(source, args) {
            this.selectEntry(args.entry, args.selected);
        },
        selectEntry: function(entry, selected) {
            var changed = false;
            if (selected) {
                this.jq("entry_" + entry.getIdForDom()).addClass("ui-selected");
                var index = this.selectedEntries.indexOf(entry);
                if (index < 0) {
                    this.selectedEntries.push(entry);
                    changed = true;
                }
            } else {
                this.jq("entry_" + entry.getIdForDom()).removeClass("ui-selected");
                var index = this.selectedEntries.indexOf(entry);
                if (index >= 0) {
                    this.selectedEntries.splice(index, 1);
                    changed = true;
                }
            }
        },
        makeEntriesDisplay: function(entries) {
            return this.getEntriesTree(entries);
        },
        entryListChanged: function(entryList) {
            if (this.multiSearch) {
                this.multiSearch.count--;
            }
            SUPER.entryListChanged.apply(this, [entryList]);
            var entries = this.entryList.getEntries();
            if (entries.length == 0) {
                this.getSearchSettings().skip = 0;
                this.getSearchSettings().max = DEFAULT_MAX;
		this.handleNoEntries();
                return;
            }
            this.writeMessage(this.getResultsHeader(entries, true));
	    this.initCloser(ID_RESULTS);

            var get = this.getGet();
            this.writeHtml(ID_FOOTER_LEFT, "");
            if (this.footerRight != null) {
                this.writeHtml(ID_FOOTER_RIGHT, this.footerRight);
            }


	    let html = "";
	    let inner = "";
	    entries.forEach((entry,idx) =>{
		inner+=HU.div([CLASS,"display-simplesearch-entry"], HU.href(this.getRamadda().getEntryUrl(entry),HU.image(entry.getIconUrl()) +"  "+ entry.getName()));
	    });
//	    inner = HU.div([CLASS,"display-simplesearch-entries"],inner);
            this.writeEntries(inner, entries);
            this.getDisplayManager().handleEventEntriesChanged(this, entries);
        },

    });
}



/**
   Copyright 2008-2024 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/


var DISPLAY_ENTRYLIST = "entrylist";
var DISPLAY_TESTLIST = "testlist";
var DISPLAY_ENTRYDISPLAY = "entrydisplay";
var DISPLAY_ENTRY_GALLERY = "entrygallery";
var DISPLAY_ENTRY_GRID = "entrygrid";
var DISPLAY_OPERANDS = "operands";
var DISPLAY_METADATA = "metadata";
var DISPLAY_ENTRYTIMELINE = "entrytimeline";
var DISPLAY_REPOSITORIES = "repositories";
var DISPLAY_ENTRYTITLE = "entrytitle";
var DISPLAY_ENTRYWIKI = "entrywiki";
var DISPLAY_SEARCH  = "search";
var DISPLAY_SIMPLESEARCH  = "simplesearch";
var ID_RESULTS = "results";
var ID_SEARCH_FORM = "searchform";
var ID_SEARCH_HEADER = "searchheader";
var ID_SEARCH_BAR = "searchbar";
var ID_SEARCH_TAG = "searchtag";
var ID_SEARCH_TAG_GROUP = "searchtaggroup";
var ID_ENTRIES = "entries";
var ID_DETAILS_INNER = "detailsinner";
var ID_DETAILS_ANCESTORS = "detailsancestors";
var ID_DETAILS_TAGS= "detailstags";
var ID_DETAILS_TYPE= "detailstype";
var ID_PROVIDERS = "providers";
var ID_SEARCH_ORDERBY = "orderby";
var ID_SEARCH_SETTINGS = "searchsettings";
var ID_SEARCH_AREA = "search_area";
var ID_SEARCH_MAX = "search_max";
var ID_SEARCH_DATE_RANGE = "search_date";
var ID_SEARCH_DATE_CREATE = "search_createdate";
var ID_SEARCH_TAGS = "search_tags";
var ID_SEARCH_ANCESTOR = "search_ancestor";
var ID_SEARCH_ANCESTORS = "search_ancestors";
var ID_TREE_LINK = "treelink";
var ATTR_ENTRYID = "entryid";


var ID_SEARCH = "search";
var ID_FORM = "form";

var ID_TEXT_FIELD = "textfield";
var ID_NAME_FIELD = "namefield";
var ID_DESCRIPTION_FIELD = "descriptionfield";
var ID_ANCESTOR = "ancestor";
var ID_ANCESTOR_NAME = "ancestorname";
var ID_TYPE_FIELD = "typefield";
var ID_TYPE_DIV = "typediv";
var ID_TYPEFIELDS = "typefields";
var ID_METADATA_FIELD = "metadatafield";
var ID_COLUMN = "column";
var ID_SEARCH_HIDEFORM = "searchhideform";

var ATTR_TEXT_INPUT='data-text-input';

addGlobalDisplayType({
    type: DISPLAY_SIMPLESEARCH,
    label: "Simple Search",
    requiresData: false,
    category: CATEGORY_ENTRIES
});


addGlobalDisplayType({
    type: DISPLAY_ENTRYLIST,
    label: "Entry List",
    requiresData: false,
    category: CATEGORY_ENTRIES
});
/*
addGlobalDisplayType({
    type: DISPLAY_SEARCH,
    label: "Entry Search",
    requiresData: false,
    category: CATEGORY_ENTRIES
});
*/
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
    type: DISPLAY_ENTRYWIKI,
    label: "Entry Wiki",
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
    const  SUPER = new RamaddaDisplay(displayManager, id, type, properties);
    RamaddaUtil.inherit(this, SUPER);
    this.defineProperties([
	{label:'Entry Search'},
	{p:'providers',ex:'this,category:.*',tt:'List of search providers',canCache:true},
	{p:'providersMultiple',ex:'true',tt:'Support selecting multiple providers'},
	{p:'providersMultipleSize',d:'4'}
    ]);

    this.ramaddas = new Array();
    let repos = this.getProperty("repositories", this.getProperty("repos", null));
    if (repos != null) {
        let toks = repos.split(",");
        //OpenSearch;http://adasd..asdasdas.dasdas.,
        for (let i = 0; i < toks.length; i++) {
            let tok = toks[i];
            tok = tok.trim();
            this.ramaddas.push(getRamadda(tok));
        }
        if (this.ramaddas.length > 0) {
            let container = new RepositoryContainer("all", "All entries");
            addRepository(container);
            for (let i = 0; i < this.ramaddas.length; i++) {
                container.addRepository(this.ramaddas[i]);
            }
            this.ramaddas.push(container);
            this.setOriginalRamadda(this.ramaddas[0]);
            this.setRamadda(this.ramaddas[0]);
        }
    }


    this.searchSettings =  new EntrySearchSettings({
        parent: properties.searchEntryParent || properties.entryParent,
        text: properties.searchEntryText || properties.entryText,
        entryType: properties.searchEntryType,
        orderBy: properties.orderBy,
	ancestor: properties.searchAncestor || properties.ancestor ,
    });
    if(properties.provider) {
	this.searchSettings.setProvider(properties.provider);
    }

    RamaddaUtil.defineMembers(this, {
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
                this.searchSettings.clearProviders();
                let provider = this.searchSettings.getProvider();
		let select = this.jq(ID_PROVIDERS);
                let fromSelect = select.val();
                if (fromSelect != null) {
                    provider = fromSelect;
                } else {
		    provider =    this.getPropertyProviders()[0];
		    if(provider)provider = provider.id;
                }
		let ramadda=this.getRamadda();
		let ok = true;
		if(ramadda && ramadda.getId() == provider) {
		    ok = false;
		}
		if(ok) {
                    this.searchSettings.setProvider(provider);
		} else {
                    this.searchSettings.clearProviders();
		}
            }
            return this.searchSettings;
        },
        getEntries: function() {
            if (this.entryList == null) return [];
            return this.entryList.getEntries();
        },
	getEntriesMetadata:function(entries) {
	                let mdtsFromEntries = [];
            let mdtmap = {};
            let tmp = {};
            for (let i = 0; i < entries.length; i++) {
                let entry = entries[i];
                let metadata = entry.getMetadata();
                for (let j = 0; j < metadata.length; j++) {
                    let m = metadata[j];
                    if (tmp[m.type] == null) {
                        tmp[m.type] = "";
                        mdtsFromEntries.push(m.type);
                    }
                    mdtmap[metadata[j].type] = metadata[j].label;
                }
            }

            let html = "";
            html += HU.openTag(TAG_TABLE, ["id", this.getDomId("table"), ATTR_CLASS, "cell-border stripe ramadda-table", ATTR_WIDTH, "100%", "cellpadding", "5", "cellspacing", "0"]);
            html += "<thead>"
            let type = this.findEntryType(this.searchSettings.entryType);
            let typeName = "Entry";
            if (type != null) {
                typeName = type.getLabel();
            }
	    this.writeMessage(this.getResultsHeader(entries));
            let mdts = null;
            //Get the metadata types to show from either a property or
            //gather them from all of the entries
            // e.g., "project_pi,project_person,project_funding"
            let prop = this.getProperty("metadataTypes", null);
            if (prop != null) {
                mdts = prop.split(",");
            } else {
                mdts = mdtsFromEntries;
                mdts.sort();
            }

            let skip = {
                "content.pagestyle": true,
                "content.pagetemplate": true,
                "content.sort": true,
                "spatial.polygon": true,
            };
            let headerItems = [];
            headerItems.push(HU.th([], HU.b(typeName)));
            for (let i = 0; i < mdts.length; i++) {
                let type = mdts[i];
                if (skip[type]) {
                    continue;
                }
                let label = mdtmap[mdts[i]];
                if (label == null) label = mdts[i];
                headerItems.push(HU.th([], HU.b(label)));
            }
            let headerRow = HU.tr(["valign", "bottom"], HU.join(headerItems, ""));
            html += headerRow;
            html += "</thead><tbody>"
            let divider = "<div class=display-metadata-divider></div>";
            let missing = this.missingMessage;
            if (missing = null) missing = "&nbsp;";
            for (let entryIdx = 0; entryIdx < entries.length; entryIdx++) {
                let entry = entries[entryIdx];
                let metadata = entry.getMetadata();
                let row = [];
                let buttonId = this.getDomId("entrylink" + entry.getIdForDom());
                let link = entry.getLink(entry.getIconImage() + " " + entry.getName());
                row.push(HU.td([], HU.div([ATTR_CLASS, "display-metadata-entrylink"], link)));
                for (let mdtIdx = 0; mdtIdx < mdts.length; mdtIdx++) {
                    let mdt = mdts[mdtIdx];
                    if (skip[mdt]) {
                        continue;
                    }
                    let cell = null;
                    for (let j = 0; j < metadata.length; j++) {
                        let m = metadata[j];
                        if (m.type == mdt) {
                            let item = null;
                            if (m.type == "content.thumbnail" || m.type == "content.logo") {
                                let url = this.getRamadda().getRoot() + "/metadata/view/" + m.value.attr1 + "?element=1&entryid=" + entry.getId() + "&metadata_id=" + m.id;
                                item = HU.image(url, [ATTR_WIDTH, "100"]);
                            } else if (m.type == "content.url" || m.type == "dif.related_url") {
                                let label = m.value.attr2;
                                if (label == null || label == "") {
                                    label = m.value.attr1;
                                }
                                item = HU.href(m.value.attr1, label);
                            } else if (m.type == "content.attachment") {
                                let toks = m.value.attr1.split("_file_");
                                let filename = toks[1];
                                let url = this.getRamadda().getRoot() + "/metadata/view/" + m.value.attr1 + "?element=1&entryid=" + entry.getId() + "&metadata_id=" + m.id;
                                item = HU.href(url, filename);
                            } else {
                                item = m.value.attr1;
                                if (Utils.isDefined(m.value.attr2)) {
				    if(String(m.value.attr2).trim().length > 0) {
					item += " - " + m.value.attr2;
				    }
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
                    let add = HU.tag(TAG_A, [ATTR_STYLE, "color:#000;", ATTR_HREF, this.getRamadda().getRoot() + "/metadata/addform?entryid=" + entry.getId() + "&metadata_type=" + mdt,
                        "target", "_blank", "alt", "Add metadata", ATTR_TITLE, "Add metadata"
                    ], "+");
                    add = HU.div(["class", "display-metadata-table-add"], add);
                    let cellContents = add + divider;
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
	    return html;
	},

        getEntriesGallery: function(entries) {
            let nonImageHtml = "";
            let html = "";
            let imageCnt = 0;
            let imageEntries = [];
	    for (let i = 0; i < entries.length; i++) {
                let entry = entries[i];
                //Don: Right now this just shows all of the images one after the other.
                //If there is just one image we should just display it
                //We should do a gallery here if more than 1

                if (entry.isImage()) {
                    imageEntries.push(entry);
                    let link = HU.tag(TAG_A, [ATTR_HREF, entry.getEntryUrl()], entry.getName());
                    imageCnt++;
		    let imageUrl =entry.getImageUrl();
                    html += HU.tag(TAG_IMG, ["src", imageUrl, ATTR_WIDTH, "500", ATTR_ID,
                            this.getDomId("entry_" + entry.getIdForDom()),
                            ATTR_ENTRYID, entry.getId(), ATTR_CLASS, "display-entrygallery-entry"
                        ]) + "<br>" +
                        link + "<p>";
                } else {
                    let icon = entry.getIconImage([ATTR_TITLE, "View entry"]);
                    let link = HU.tag(TAG_A, [ATTR_HREF, entry.getEntryUrl()], icon + " " + entry.getName());
                    nonImageHtml += link + "<br>";
                }
            }

            if (imageCnt > 1) {
                //Show a  gallery instead
		this.galleryId = HU.getUniqueId("gallery_");
                let newHtml = HU.open("div",[ID, this.galleryId,CLASS,"ramadda-grid"]);
		let itemWidth = this.getProperty("galleryItemWidth","200px");
                for (let i = 0; i < imageEntries.length; i++) {
                    let entry = imageEntries[i];
		    let attrs = ["width",itemWidth];
                    newHtml += HU.open("div",[CLASS,"display-entrygallery-item",ATTR_STYLE,HU.css(attrs)]);
                    let link = HU.tag(TAG_A, ["target","_entries",ATTR_HREF, entry.getEntryUrl()], entry.getName());
		    link = link.replace(/"/g,"'");
		    let imageUrl =entry.getImageUrl();
                    let img = HU.image(imageUrl, ["loading","lazy", ATTR_WIDTH, "100%", ATTR_ID,
						  this.getDomId("entry_" + entry.getIdForDom()),
						  ATTR_ENTRYID, entry.getId(), ATTR_CLASS, "display-entrygallery-entry"
						 ]);
                    img = HU.href(entry.getResourceUrl(), img, ["data-fancybox",this.galleryId, "data-caption",link, CLASS, "popup_image"]);
                    newHtml += HU.div([CLASS, "image-outer"], HU.div(["class", "image-inner"], img) +
                        HU.div(["class", "image-caption"], link));

                    newHtml += HU.close("div");
                }
                newHtml += HU.close("div");
                html = newHtml;
            }


            //append the links to the non image entries
            if (nonImageHtml != "") {
                if (imageCnt > 0) {
                    html += "<hr>";
                }
                html += HU.div([ATTR_STYLE,HU.css("margin","10px")],nonImageHtml);
            }
            return html;
        }
    });
    if (properties.searchEntryType != null) {
        this.searchSettings.addType(properties.searchEntryType);
    }
}






function RamaddaSearcherDisplay(displayManager, id,  type, properties) {
    let NONE = "-- None --";
    let myProps = [
	{label:'Search'},
        {p:'showForm',d: true},
        {p:'formOpen',d: true},	
        {p:'orderBy',ex: 'name_ascending|name_descending|fromdate_ascending|fromdate_descending|todate_|createdate_|size_'},
        {p:'orientation',ex:'horizontal|vertical',d:'horizontal'},
	{p:'formHeight',d:'1000px'},
        {p:'entriesHeight',ex:'70vh'},	
        {p:'showEntries',d: true},
        {p:'showFooter',d: true},	
        {p:'showType',d: true},

        {p:'entryTypes',ex:'comma separated list of types - use "any" for any type'},
        {p:'typesLabel',tt: 'Label to use for the type section'},		
	{p:'addAllTypes',ex:'true',tt:'Add the All types to the type list'},
	{p:'addAnyType',ex:'true',tt:'Add the Any of these types to the type list'},
	{p:'startWithAny',ex:'true',tt:'Start with the Any of these types'},	
        {p:'doSearch',d: true,tt:'Apply search at initial display'},
	{p:'searchHeaderLabel',d: 'Search'},
	{p:'searchOpen',d: true},
        {p:'showOrderBy',d:true,ex: 'true'},
        {p:'showSearchSettings',d: true},
        {p:'showToggle',d: false},
	{p:'showEntryBreadcrumbs',ex:'false'},
	{p:'showSnippetInList',ex:'true'},
        {p:'showProviders',d: false},
        {p:'showDate',d: true},
        {p:'showCreateDate',ex:'true',d: false},	
        {p:'showArea',d: true},
        {p:'showText',d: true},
        {p:'showName',d: false},
        {p:'showDescription',d: false},		
	{p:'ancestor',ex:'this',tt:'Constrain search to this tree'},		
        {p:'showAncestor',d: true},
        {p:'ancestors',tt: 'Comma separated list of entry ids or type:entry_type'},
        {p:'ancestorsLabel',tt: 'Label to use for the ancestors section'},		
        {p:'mainAncestor',tt: 'Entry ID to force the search under'},
	{p:'textRequired',d:false},
        {p:'searchText',d: '',tt:'Initial search text'},
	{p:'searchPrefix',ex:'name:, contents:, path:'},
        {p:'showMetadata',d: false},
	{p:'metadataTypes', ex:'enum_tag:Tag,content.keyword:Keyword,thredds.variable:Variable'},
	{p:'metadataDisplay',ex:'archive_note:attr1=Arrangement:template=<b>{attr1}_colon_</b> {attr2}',
	 tt:'Add metadata in the toggle. e.g.: type1:template={attr1},type2:attr1=Value:template={attr1}_colon_ {attr2}'},
        {p:'showTags',d: true},	
	{p:'mainMetadataDisplay'},
	{p:'nameStyle'},
	{p:'showIcon'},
	{p:'showToggle'},
	{p:'showThumbnail'},
	{p:'placeholderImage',ex:'/repository/image.png'},
	{p:'showEntryType'},
	{p:'tagPopupLimit',d: 25,tt:'When do we show the tag popup' },		
	{p:'showSearchLabels',d:true},
	{p:'comparators',d:'<=,>=,=,between',tt:'comparators for numeric search'},
	{p:'searchDirect',d:false,tt:'Directly search remote RAMADDA repositories'},
        {p:'fields',d: null},
        {p:'formWidth',d: '225px'},
        {p:'entriesWidth',d: 0},
	{p:'displayTypes',ex:'list,images,timeline,map,metadata'},
	{p:'defaultImage',ex:'blank.gif',canCache:true},
	{p:'showEntryImage',d:true,tt:'Show the entry thumbnail'},
        {p:'showDetailsForGroup',d: false},
	{p:'inputSize',d:'200px',tt:'Text input size'},
	{p:'textInputSize',d:'20px',ex:'100%'},	
	{p:'startDateLabel'},
	{p:'createDateLabel'},	
	{p:'areaLabel'},
	{p:'toggleClose',ex:true},
	{p:'textToggleClose',ex:true},
	{p:'dateToggleClose',ex:true},		
	{p:'areaToggleClose',ex:true},
	{p:'columnsToggleClose',ex:true},		
	{p:'orderByTypes',d:'relevant,name,createdate,date,size'},
	{p:'doWorkbench',d:false,ex:'true', tt:'Show the new, charts, etc links'},
	];

    const SUPER = new RamaddaEntryDisplay(displayManager, id, type, properties);

    this.currentTime = new Date();
    defineDisplay(this, SUPER, myProps, {
        metadataTypeList: [],
	haveSearched: false,
        haveTypes: false,
        metadata: {},
        metadataLoading: {},
	addToDocumentUrl:function(key,value) {
	    //Don't do this right away
	    let now = new Date();
	    if(now.getTime()-this.currentTime.getTime()>1000) {
		HU.addToDocumentUrl(key,value);
	    }
	},
	ctor: function() {
	    let metadataTypesAttr = this.getMetadataTypes();
	    if (Utils.stringDefined(metadataTypesAttr) && this.getShowSearchSettings()) {
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
        getLoadingMessage: function(msg) {
	    if(!msg) return "";
	    return msg;
	},
        isLayoutHorizontal: function() {
	    return this.getOrientation()== "horizontal";
        },

	initHtml: function() {
	    this.jq(ID_ANCESTOR).click((event) =>{
		let aid = this.domId(ID_ANCESTOR);
		let root = this.getRamadda().getRoot();
		RamaddaUtils.selectInitialClick(event,aid,aid,true,null,null,'',root);
	    });


	    this.jq(ID_SEARCH_HIDEFORM).click(()=>{
		this.formShown  = !this.formShown;
		if(this.formShown)
		    this.jq(ID_SEARCH_FORM).show();
		else
		    this.jq(ID_SEARCH_FORM).hide();
	    });
            if (this.areaWidgets) {
		this.areaWidgets.forEach(areaWidget=>{
		    areaWidget.initHtml();
		});
	    }
	    if(this.getShowOrderBy()) {
		let settings = this.getSearchSettings();
		let byList = [];
		let getLabel=(type,suffix,dflt)=>{
		    let key ='orderByLabel_'+ type+(suffix?'_'+suffix:'');
		    let label =this.getProperty(key);
		    if(label) return label;
		    if(!dflt) dflt = Utils.makeLabel(type)+(suffix?(' - '+suffix):'');
		    return  dflt;
		}

		Utils.split(this.getOrderByTypes(),',',true,true).forEach(type=>{
		    if(type=='relevant')
			byList.push([getLabel(type,null),type]);
		    else if(type=='name')
			byList.push([getLabel(type,'ascending',"Name A-Z"), type+'_ascending'],
				    [getLabel(type,'descending',"Name Z-A"),type+'_descending']);
		    else if(type=='createdate')
			byList.push(["Record create date - newest first","createdate_descending"],
				    ["Record create date - oldest first","createdate_ascending"]);
		    else if(type=='date')
			byList.push([getLabel(type,'descending',"From date - youngest first"),"fromdate_descending"],			  			  
				    [getLabel(type,'ascending',"From date - oldest first"),"fromdate_ascending"]);
		    else if(type=='size')
			byList.push(["Size - largest first","size_descending"],
				    ["Size - smallest first","size_ascending"]);
		    else {
			byList.push(
			    [getLabel(type,'descending'),'field:'+type+'_descending'],
			    [getLabel(type, 'ascending'),'field:'+type+'_ascending']);			    
		    }
		});
		let options = "";
		byList.forEach(tuple=>{
		    let label = tuple[0];
		    let by = tuple[1];
		    let extra = settings.orderBy==by?" selected ":""
		    options += "<option title='" + label+"'  " + "" + extra + " value=\"" + by + "\">" + label + "</option>\n";
		    
		});
		let select = HU.tag("select", ["id", this.getDomId(ID_SEARCH_ORDERBY), ATTR_CLASS, "display-search-orderby"], options);
		this.jq(ID_SEARCH_HEADER).append(select);
	    }
            this.addExtraForm();
	},
	toggleAll:function(visible) {
	    let _this = this;
	    this.jq(ID_SEARCH_FORM).find('.display-search-label-toggle').each(function() {
		_this.toggleWidget($(this),visible);
	    });
	},
	toggleWidget:function(toggle, visible) {
	    let widget = jqid(toggle.attr('data-widget-id'));
	    let icon;
	    if(!visible) {
		widget.hide();
		icon = 'fa-plus';
	    } else {
		widget.show();
		icon = 'fa-minus';
	    }
	    icon = HU.getIconImage(icon, [], [ATTR_STYLE,'color:#fff;'])
	    let imageId= toggle.attr('data-image-id');
			      //			imageId,jqid(imageId).length);
	    jqid(imageId).html(icon);
	},
	addToggle:function(label,widgetId,toggleClose) {
	    let toggleId = HU.getUniqueId('');
	    let imageId = toggleId+'_image';
	    label = HU.div([ATTR_CLASS,'display-search-label-toggle',
			    'data-widget-id',widgetId,
			    'data-image-id',imageId,
			    ATTR_TITLE, "click: toggle; shift-click: toggle all",ATTR_ID,toggleId],
			   HU.span([ATTR_ID,imageId],
				   HU.getIconImage(toggleClose?'fa-plus':'fa-minus', [], [ATTR_STYLE,'color:#fff;'])) +' ' + label);
	    setTimeout(()=>{
		jqid(toggleId).click((event)=>{
		    let toggle = jqid(toggleId);
		    let widget = jqid(widgetId);
		    let visible = widget.is(':visible');
		    if(event.shiftKey) {
			this.toggleAll(!visible);
			return;
		    }
		    this.toggleWidget(toggle,!visible);
		});
	    },100);
	    return label;
	},
	addWidget:function(label,widget,args) {
	    let opts = {
		addToggle:true,
		toggleClose:false
	    }
	    if(args) $.extend(opts,args);
	    if(!Utils.stringDefined(widget)) return '';
            let horizontal = this.isLayoutHorizontal();
	    if(horizontal)  {
		if(!Utils.stringDefined(label)) {
		    widget = HU.div([ATTR_CLASS,'display-search-widget-nolabel'],widget);	
		    return widget;
		}		    
		widget = HU.div([ATTR_CLASS,'display-search-widget ' + (opts.searchWidgetClass??'')],widget);	
		let widgetId = HU.getUniqueId('');
		if(opts.addToggle) {
		    label = this.addToggle(label,widgetId,opts.toggleClose);
		}

		let w = Utils.stringDefined(label)?
		    HU.div([ATTR_CLASS,"display-search-label",ATTR_STYLE,HU.css('xmin-width',HU.getDimension(this.getFormWidth()))], label):'';
		w=w+HU.span([ATTR_ID,widgetId,ATTR_STYLE,HU.css('display',opts.toggleClose?'none':'inline-block')],widget);
		return HU.div([ATTR_CLASS,"display-search-block"], w);
	    }
	    return HU.formEntry("",widget);
	},
        getDefaultHtml: function() {
            let html = "";
            let horizontal = this.isLayoutHorizontal();
            let footer = this.getFooter();
            if (!this.getShowFooter(true)) {
                footer = "";
            }
	    this.jq(ID_BOTTOM).html(footer);
	    footer = "";
            let entriesDivAttrs = [ATTR_ID, this.getDomId(ID_ENTRIES), ATTR_CLASS, this.getClass("content")];
            let innerHeight= this.getProperty("innerHeight", null);
            let entriesStyle = this.getProperty("entriesStyle", "");	    
	    let style = "";
            if (innerHeight == null) {
                innerHeight = this.getEntriesHeight();
            }
            if (innerHeight != null) {
                style = "margin: 0px; padding: 0px; max-height:" + HU.getDimension(innerHeight) + "; overflow-y: auto;";
                style = HU.css('max-height', HU.getDimension(innerHeight),'overflow-y','auto');
            }
	    style+= entriesStyle;
            entriesDivAttrs.push(ATTR_STYLE);
            entriesDivAttrs.push(style);	    
	    let searchBar = HU.div([ATTR_CLASS,horizontal?"display-search-bar":"display-search-bar-vertical",ATTR_ID, this.domId(ID_SEARCH_BAR)],"");
            let resultsDiv = "";
            if (this.getProperty("showHeader", true)) {
                resultsDiv = HU.div([ATTR_CLASS, "display-entries-results", ATTR_ID, this.getDomId(ID_RESULTS)], "&nbsp;");
            }
	    resultsDiv = HU.leftRightTable(resultsDiv,HU.div([CLASS,"display-search-header", ID,this.domId(ID_SEARCH_HEADER)]),null,null,{valign:"bottom"});
	    let toggle = "";
	    if(horizontal && this.getShowForm()) {
		toggle = HU.div([TITLE, "Toggle form", ID,this.domId(ID_SEARCH_HIDEFORM), CLASS,"ramadda-clickable",ATTR_STYLE,HU.css("position","absolute","left","0px","top","0px")],
				HU.getIconImage("fas fa-bars"));
	    }
            let entriesDiv = HU.div([ATTR_STYLE,HU.css("position","relative")],
				    toggle +
				    searchBar +
				    resultsDiv +
				    HU.div(entriesDivAttrs, this.getLoadingMessage()));

            if (horizontal) {
		html += HU.open("table",["width","100%","border",0]);
		html+="<tr valign=top>";
                let entriesAttrs = ["class", "col-md-12"];
                if (this.getShowForm()) {
                    let attrs = [];
		    let form = HU.div([ATTR_CLASS,'display-entrylist-form',
				       ATTR_STYLE,HU.css("width",HU.getDimension(this.getFormWidth()),
						    "max-width",HU.getDimension(this.getFormWidth()),
						    "overflow-x","auto")],this.makeSearchForm());
		    html += HU.tag("td", [ID,this.getDomId(ID_SEARCH_FORM),"width","1%"], form);
		    this.formShown  = true;
                }
                if (this.getShowEntries()) {
                    let attrs = [];
                    if (this.getEntriesWidth() === "") {
                        attrs = [];
                    } else if (this.getEntriesWidth() != 0) {
                        attrs = [ATTR_WIDTH, this.getEntriesWidth()];
                    }
                    html += HU.tag("td",[], entriesDiv);		    
                }
                html += HU.closeTag("tr");
                html += HU.closeTag("table");

                html += HU.openTag(TAG_DIV, ["class", "row"]);
                if (this.getShowForm()) {
                    html += HU.tag(TAG_DIV, ["class", "col-md-6"], "");
                }
                if (this.getShowEntries()) {
                    if (this.getShowFooter(true)) {
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

	    //Don't selectbox the orderby
	    //	HtmlUtils.initSelect(this.jq(ID_SEARCH_ORDERBY));
	    this.jq(ID_SEARCH_ORDERBY).change(()=>{	    
                this.submitSearchForm();
	    });
            HtmlUtils.initSelect(this.jq(ID_REPOSITORY));
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
	    this.initMetadata();
            if (!this.haveSearched) {
                if (this.getDoSearch()) {
		    if(!this.typesPending) {
			this.submitSearchForm();
		    }
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
	    if(true) return "";
	    return  HU.jsLink("",HU.getIconImage(icon_close, [ID,this.domId("close"),ATTR_STYLE,HU.css("cursor","pointer")]));
	},
	initCloser: function(what) {
	    this.jq("close").click(()=>{
		this.jq(what||ID_RESULTS).hide();
	    });
	},
        getResultsHeader: function(entries, includeCloser) {
            let settings = this.getSearchSettings();
	    //Always show the next/prev because the results might be < max even though there
	    //are more on the repository because some results might be hidden due to access control
//            if (entries.length < DEFAULT_MAX) return entries.length+" result" +(entries.length>1?"s":"");
            let left = "Showing " + (settings.skip + 1) + "-" + (settings.skip + Math.min(settings.getMax(), entries.length));
	    if(entries.length==0) left = SPACE3+SPACE3+SPACE3;
            let nextPrev = [];
            let lessMore = [];
            if (settings.skip > 0) {
                nextPrev.push(HU.onClick(this.getGet() + ".loadPrevUrl();", HU.getIconImage("fa-arrow-left", [ATTR_TITLE, "Previous"]), [ATTR_CLASS, "display-link"]));
            }
            let addMore = false;
            if (entries.length>0 &&(true || entries.length == settings.getMax())) {
                nextPrev.push(HU.onClick(this.getGet() + ".loadNextUrl();", HU.getIconImage("fa-arrow-right", [ATTR_TITLE, "Next"]), [ATTR_CLASS, "display-link"]));
                addMore = true;
            }

	    if(entries.length>0) {
		lessMore.push(HU.onClick(this.getGet() + ".loadLess();", HU.getIconImage("fa-minus", [ATTR_TITLE, "View less"]), [ATTR_CLASS, "display-link"]));
		if (addMore) {
                    lessMore.push(HU.onClick(this.getGet() + ".loadMore();", HU.getIconImage("fa-plus", [ATTR_TITLE, "View more"]), [ATTR_CLASS, "display-link"]));
		}
	    }
            let results = "";
            let spacer = "&nbsp;&nbsp;&nbsp;"
	    if(includeCloser)
		results = this.getCloser();
	    results += "&nbsp;" + left + spacer;
            results += 
                HU.join(nextPrev, "&nbsp;") + spacer +
                HU.join(lessMore, "&nbsp;");
            return results+"<br>";
        },
	makeSearchSettings: function() {
	    let settings = this.getSearchSettings();
            settings.text = this.getFieldValue(this.getDomId(ID_TEXT_FIELD), settings.text);
            settings.name = this.getFieldValue(this.getDomId(ID_NAME_FIELD), settings.name);
            settings.description = this.getFieldValue(this.getDomId(ID_DESCRIPTION_FIELD), settings.description);
	    
	    if(settings.text) {
		if(Utils.stringDefined(settings.text)) {
		    this.addToDocumentUrl(ID_TEXT_FIELD,settings.text);
		}
		if(settings.text.trim()!="") {
		    if(this.getSearchPrefix())
			settings.text = this.getSearchPrefix()+ settings.text;
		}
	    }  else {
		//		this.addToDocumentUrl(ID_TEXT_FIELD,"");
	    }

	    settings.ancestor = this.getAncestor();
	    let orderBy = this.jq(ID_SEARCH_ORDERBY).val();
	    if(orderBy) {
		let ascending = orderBy.indexOf("_ascending")>=0;
		if(orderBy=="relevant") ascending=false;
		orderBy = orderBy.replace("_ascending","").replace("_descending","");
		settings.orderBy =  orderBy;
		settings.ascending = ascending;
	    }


            if (this.haveTypes) {
                settings.entryType = this.getFieldValue(this.getDomId(ID_TYPE_FIELD),
							settings.entryType);
		if(settings.entryType && (this.typeList==null || this.typeList.length>1) ) {
		    this.addToDocumentUrl(ID_TYPE_FIELD,settings.entryType);
		} else {
		    this.addToDocumentUrl(ID_TYPE_FIELD,null);
		}
            } else if(this.typeList && this.typeList.length==1) {
		settings.entryType = this.typeList[0];
	    }
	    if(!Utils.stringDefined(settings.entryType) && this.getEntryTypes()) { 
		settings.entryType = this.getEntryTypes();
	    }

            settings.clearAndAddType(settings.entryType);
	    let ancestor = this.jq(ID_ANCESTOR+"_hidden").val();
	    if(Utils.stringDefined(ancestor)) {
		settings.ancestor = ancestor;
		this.addToDocumentUrl(ID_ANCESTOR,ancestor);
		let name = this.jq(ID_ANCESTOR).val();
		if(name)
		    this.addToDocumentUrl(ID_ANCESTOR_NAME,name);		    
	    } else {
		//delete it
		this.addToDocumentUrl(ID_ANCESTOR,null);
		this.addToDocumentUrl(ID_ANCESTOR_NAME,null);		
	    }
            if (this.areaWidgets) {
		this.areaWidgets.forEach(areaWidget=>{
                    areaWidget.setSearchSettings(settings);
		});
            }
            if (this.dateRangeWidget) {
                this.dateRangeWidget.setSearchSettings(settings);
            }
            if (this.createdateRangeWidget) {
                this.createdateRangeWidget.setSearchSettings(settings);
            }	    
	    
            settings.metadata = [];
	    if(!this.getShowTags()) {
		this.metadataTypeList.forEach(metadataType=>{
                    let value = metadataType.getValue();
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
		});
	    } 
	    if(this.metadataList) {
		this.metadataList.forEach(metadata=>{
		    if (!metadata.getElements()) {
			return;
		    }
		    metadata.getElements().forEach(element=>{
			element.addSearchSettings(settings);
		    });
		});
	    }

	    return settings;
	},
        submitSearchForm: function() {
            if (this.fixedEntries) {
                return;
            }
            this.haveSearched = true;
	    let settings  =this.makeSearchSettings();
            if (this.getTextRequired() && (settings.text == null || settings.text.trim().length == 0)) {
                this.writeEntries("");
                return;
            }

            //Call this now because it sets settings
            let theRepository = this.getRamadda()

	    this.writeMessage(this.getWaitImage() + " " +"Searching...");
            if (theRepository.children) {
                this.entryList = new EntryListHolder(theRepository, this);
                this.multiSearch = {
                    count: 0,
                };

                for (let i = 0; i < theRepository.children.length; i++) {
                    let ramadda = theRepository.children[i];
                    let jsonUrl = this.makeSearchUrl(ramadda);
                    this.updateForSearching(jsonUrl);
                    this.entryList.addEntryList(new EntryList(ramadda, jsonUrl, null, false));
                    this.multiSearch.count++;
                }
                this.entryList.doSearch(this);
            } else {
                this.multiSearch = null;
                let jsonUrl = this.makeSearchUrl(this.getRamadda());
                this.handleLog(jsonUrl);
                this.entryList = new EntryList(this.getRamadda(), jsonUrl, this, true);
                this.updateForSearching(jsonUrl);
            }
        },
        entryListChanged:function(entryList) {
	},
        handleSearchError: function(url, msg) {
            this.writeEntries("");
            this.writeMessage("Error performing search:" + msg);
            console.log("Error performing search:" + msg);
            //alert("There was an error performing the search\n" + msg);
        },
        updateForSearching: function(jsonUrl) {
	    let settings = this.getSearchSettings();
	    let outputs = this.getRamadda().getSearchLinks(settings,true);
	    let url= this.getRamadda().getSearchUrl(settings);
	    let copyId = HU.getUniqueId('copy');
	    let extra = [];

	    if(this.getProperty('searchOutputs')) {
		extra = Utils.mergeLists(extra,Utils.split(this.getProperty('searchOutputs'),',',true,true));
	    }
	    if(!Utils.isAnonymous()) {
		extra.push('repository.extedit;Extended Edit');
	    }


	    extra.forEach(tok=>{
		let tuple = Utils.split(tok,';');
		if(tuple.length<2)return;
		let id = tuple[0];
		let label = tuple[1];
                outputs.push(HU.span([ATTR_CLASS,'ramadda-search-link ramadda-clickable',
				      ATTR_TITLE,'Click to download; shift-click to copy URL',
				      'custom-output','true',
				      'data-name',label,
				      'data-format',id,
				      'data-url',
				      this.getRamadda().getSearchUrl(settings,id)],
				     label));
	    });



	    outputs = HU.join(outputs, HU.space(2));
	    outputs = outputs+ HU.space(2)+
		HU.span([ATTR_CLASS,'ramadda-search-link ramadda-clickable',
			 ATTR_ID,copyId,
			 'data-copy',url],
			HU.getIconImage("fas fa-clipboard"));
            this.footerRight = outputs == null ? "" :  outputs;
            this.writeHtml(ID_FOOTER_RIGHT, this.footerRight);
	    let _this = this;
	    this.jq(ID_FOOTER_RIGHT).find('.ramadda-search-link').button().click(function(event){
		let custom  =$(this).attr('custom-output');
		_this.handleSearchLink(event,$(this),custom);
	    });
            let msg = this.searchMessage;
            if (msg == null) {
                msg = this.getRamadda().getSearchMessage();
            }
            let provider = this.getSearchSettings().getProvider();
            if (provider != null) {
                msg = null;
                if (this.providerMap != null && this.providerMap[provider]) {
                    msg = this.providerMap[provider].name;
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
            let cols = this.getSearchableColumns();
            for (let i = 0; i < cols.length; i++) {
                let col = cols[i];
                let id = this.getDomId(ID_COLUMN + col.getName());
                let value = $("#" + id).val();
                if (value == null || value.length == 0) continue;
                this.savedValues[id] = value;
            }
        },
        makeSearchUrl: function(repository) {
	    let _this=this;
            let extra = "";
            let cols = this.getSearchableColumns();
	    let searchBar  = this.jq(ID_SEARCH_BAR);
	    let makeTag=(key,value,label) =>{
		return  $(HU.div([ATTR_TITLE,'Click to clear search',ATTR_CLASS,"display-search-tag",key,value],label)).appendTo(searchBar);
	    }

	    this.getContents().find('.display-search-textinput').each(function() {
		let arg = $(this).attr(ATTR_TEXT_INPUT);
		if(!arg) return;
		let val = $(this).val();
		let tag = searchBar.find(HU.attrSelect(ATTR_TEXT_INPUT,arg));
		if(!Utils.stringDefined(val)) {
		    if(tag.length!=0) tag.remove();
		    return
		    
		}
		let label = arg+'='+val;
		if(tag.length==0) {
		    tag =makeTag(ATTR_TEXT_INPUT,arg,label);
		    tag.click(()=>{
			$(this).val('');
			_this.submitSearchForm();
		    });
		} else {
		    tag.html(label);
		}
	    });
            for (let i = 0; i < cols.length; i++) {
                let col = cols[i];
                if (!col.getCanSearch()) continue;
		let id  = ID_COLUMN + col.getName();
		let arg = col.getSearchArg();
		let tag = searchBar.find(HU.attrSelect("column",col.getName()));
		if(col.isNumeric()) {
                    let expr = this.jq(id+"_expr").val();
                    let from = this.jq(id+"_from").val();
                    let to = this.jq(id+"_to").val();		    		    
		    if(Utils.stringDefined(from) || Utils.stringDefined(to)) {
			let label =  (Utils.stringDefined(from)?(from+" &lt; "):"") +  col.getLabel() + (Utils.stringDefined(to)?(" &lt; " +to):"");
			if(tag.length==0) {
			    tag = makeTag("column",col.getName(),label);
			    tag.click(()=>{
				this.jq(id+"_from").val("");
				this.jq(id+"_to").val("");		    		    
				this.submitSearchForm();
			    });
			} else {
			    tag.html(label);
			}
//			extra += "&" + arg  +"_expr" +  "=" + encodeURIComponent(expr);
			if(Utils.stringDefined(from))
			    extra += "&" + arg  +"_from" +  "=" + encodeURIComponent(from);
			if(Utils.stringDefined(to))
			    extra += "&" + arg  +"_to" +  "=" + encodeURIComponent(to);						
//			console.log("expr:" +expr +" from:" + from +" to:" + to);
		    } else {
			tag.remove();
		    }
		} else if(col.isLatLon()) {
		    //searchurl
		    if(col.areaWidget) {
			let v = col.areaWidget.getValues();
			['north','west','south','east'].forEach(d=>{
			    if(Utils.stringDefined(v[d])) {
				extra += '&' + arg+'_'+d + '=' + encodeURIComponent(v[d].trim());
			    }
			});
			if(col.areaWidget.getContains()) {
			    extra += '&' + arg+'_areamode'+  '=' + 'contains';

			}
		    }
		} else if(col.isEnumeration() && col.showCheckboxes()) {
                    let fullId = this.getDomId(ID_COLUMN + col.getName());
		    let cbxValues=[];
		    let cbxs = $('[checkbox-id='+ fullId+']');
		    cbxs.each(function() {
			if($(this).is(':checked')) {
			    let value = $(this).attr('data-value');
			    extra += '&' + arg + '=' + encodeURIComponent(value);
			    cbxValues.push(value);
			}
		    });
		    if(cbxValues.length>0) {
			label = col.getLabel() +"=" + Utils.join(cbxValues,' or ');
			if(tag.length==0) {
			    tag = makeTag("column",col.getName(),label);
			    tag.click(()=>{
				cbxs.prop('checked',false);
				this.submitSearchForm();
			    })
			} else {
			    tag.html(label);
			}
		    } else {
			tag.remove();
		    }
		} else {
                    let value = this.jq(id).val();
                    if (value == null || value==VALUE_NONE) {
			tag.remove();
			continue;
		    }

		    if(!Array.isArray(value)) {
			value = [value];
		    }
		    if(value.length==0) {
			tag.remove();
			continue;
		    }

		    if(col.getType()=="string" || col.isDate() || col.isLatLon()) {
			if(value=="") {
			    tag.remove();
			    continue;
			}
		    }


		    let label = col.getLabel() +"=";
		    if(Array.isArray(value)) {
			label+=Utils.join(value,"&nbsp;|&nbsp;");
		    } else {
			label +=  value;
		    }
			
		    if(tag.length==0) {
			tag = makeTag("column",col.getName(),label);
			tag.click(()=>{
			    let obj=this.jq(id);
			    if(obj.data && obj.data('selectBox-selectBoxIt')) {
				obj.data('selectBox-selectBoxIt').selectOption(VALUE_NONE);
			    } else {
				obj.val(null);
			    }
			    this.submitSearchForm();
			});
		    } else {
			tag.html(label);
		    }
		    value.forEach(v=>{
			extra += "&" + arg + "=" + encodeURIComponent(v);
		    });
		}
            }
	    let settings = this.getSearchSettings();
	    settings.setMax(this.jq(ID_SEARCH_MAX).val()??settings.getMax());
            settings.setExtra(extra);
            let jsonUrl = repository.getSearchUrl(settings, OUTPUT_JSON);
	    if(this.getMainAncestor()) {
		let main  = this.getMainAncestor();
		if(main=='this') {
		    main = this.getProperty('entryId');
		}
		jsonUrl+='&mainancestor='+ main;
	    }
	    this.getContents().find('.ramadda-displayentry-ancestor').each(function() {
		if($(this).is(':checked')) {
		    let id = $(this).attr('data-entryid');
		    jsonUrl+='&ancestor='+ id;
		}
	    });
	    
           return jsonUrl;
        },
	addAreaWidget(areaWidget) {
	    if(!this.areaWidgets) this.areaWidgets=[];
	    this.areaWidgets.push(areaWidget);
	},
	getSearchValue:function(key,dflt) {
	    let v =  GuiUtils.getUrlArg(this.urlPrefix+key,null);
	    if(v==null) return dflt;
	    v = v.replace(/\+/g,' ');
	    return v;
	},

        loadAncestorsList: function(entries) {
	    let html = '';
	    entries.forEach(entry=>{
		html+=HU.div([ATTR_CLASS,'display-search-ancestors-select'],
			     HU.checkbox('',[ATTR_ID,'ancestor_'+ entry.getId(),ATTR_CLASS,'ramadda-displayentry-ancestor',
					     'data-entryid',entry.getId()],false,entry.getName()));
	    });
	    this.jq(ID_SEARCH_ANCESTORS).html(HU.div([ATTR_CLASS,'display-search-ancestors'],html));
	    this.jq(ID_SEARCH_ANCESTORS).find('.ramadda-displayentry-ancestor').change(()=>{
		this.submitSearchForm();
	    });
	},
        loadAncestors: function(ancestors) {
	    let url = ramaddaBaseUrl+ '/wiki/getentries?entries=' + ancestors;
            let entryList = new EntryList(this.getRamadda(), url, this, false);
	    let success=list=>{
		this.loadAncestorsList(list.getEntries());
	    }
	    let fail=err=>{
		console.log(err);
	    }
            entryList.doSearch(null,success,fail);
	},
        makeSearchForm: function() {
	    let toggleClose = this.getProperty('toggleClose',false);

            let form = HU.openTag("form", [ATTR_ID, this.getDomId(ID_FORM), "action", "#"]);
            let buttonLabel = HU.getIconImage("fa-search", [ATTR_TITLE, "Search"]);
            let topItems = [];
	    buttonLabel = "Search";
            let searchButton = HU.div(['style','margin-bottom:4px;max-width:80%;',ATTR_ID, this.getDomId(ID_SEARCH), ATTR_CLASS, "ramadda-button display-search-button ramadda-clickable"], buttonLabel);
            let extra = "";
            let settings = this.getSearchSettings();

            let horizontal = this.isLayoutHorizontal();

            if (this.ramaddas.length > 0) {
                let repositoriesSelect = HU.openTag(TAG_SELECT, [ATTR_ID, this.getDomId(ID_REPOSITORY), ATTR_CLASS, "display-repositories-select"]);
                let icon = RamaddaUtil.getCdnUrl("/icons/favicon.png");
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
	    let providers = this.getPropertyProviders();
            if (providers != null) {
		if(providers.length==1) {
		    this.provider = providers[0];
		} else {
		    if(!this.getShowProviders()){
			this.provider = providers[0];
		    } else {
			let options = "";
			let selected = HU.getUrlArgument(ID_PROVIDERS);
			let currentCategory = null;
			let catToBuff = {};
			let cats = [];
			this.getPropertyProviders().forEach(provider=>{
			    this.providerMap[provider.id] = provider;
			    let id = provider.id;
			    if(!Utils.isDefined(selected)) {
				selected = id;
			    }
			    let label = provider.name;
			    if (label.length > 40) {
				label = label.substring(0, 39) + "...";
			    }
			    let extraAttrs = "";
			    if (id == selected) {
				extraAttrs += " selected ";
			    }
			    let category = provider.category||"";
			    let buff = catToBuff[category];
			    if (buff == null) {
				cats.push(category);
				catToBuff[category] = "";
				buff = "";
			    }
			    let img = provider.icon;
			    if(img) {
				img = img.replace(/\${urlroot}/g, ramaddaBaseUrl);
				img = img.replace(/\${root}/g, ramaddaBaseUrl);
				extraAttrs += " data-iconurl=\"" + img + "\" ";
			    }
			    buff += "<option  title='" + label+"' class=display-search-provider " + extraAttrs + " value=\"" + id + "\">" + label + "</option>\n";
			    catToBuff[category] = buff;
			});

			if(cats.length==1) {
			    options += catToBuff[cats[0]];
			} else {
			    for (let catIdx = 0; catIdx < cats.length; catIdx++) {
				let category = cats[catIdx];
				if (category != "")
				    options += "<optgroup label=\"" + category + "\">\n";
				options += catToBuff[category];
				if (category != "")
				    options += "</optgroup>";
			    }
			}
			let attrs = [ATTR_STYLE,HU.css(),
				     ATTR_ID, this.getDomId(ID_PROVIDERS),
				     ATTR_CLASS, "display-search-providers"];
			if(this.getProvidersMultiple()) {
			    let size =Math.min(8,providers.length+1);
			    attrs.push("size",this.getProvidersMultipleSize(size),  "multiple", "multiple");
			}
			let providersSelect = HU.tag("select",attrs, options);
			providersSelect = this.addWidget('Providers',providersSelect,
							 {toggleClose:true,
							  addToggle:true,
							  searchWidgetClass:(this.getProvidersMultiple()?'display-search-widget-providers':'')});
			topItems.push(providersSelect);
		    }
		}
	    }


	    this.typeList = null;
            if (this.getEntryTypes()) {
		this.typeList = this.getEntryTypes().split(",");
	    }
	    this.urlPrefix = 'search.';
	    if(this.typeList && this.typeList.length>0) this.urlPrefix+=this.typeList[0]+'.';
            if (this.getShowType()) {
		if(this.typeList == null || this.typeList.length==0) {
                    topItems.push(HU.div([ATTR_STYLE,HU.css("margin-bottom","4px"),ATTR_ID, this.getDomId(ID_TYPE_DIV)], HU.span([ATTR_CLASS, "display-loading"], "Loading types...")));
		} else {
		    extra+= HU.div([ATTR_STYLE,HU.css("margin-bottom","4px"),ATTR_ID, this.getDomId(ID_TYPE_DIV)]);
		}
            }


	    let text  = this.getFormText();
	    if(!Utils.stringDefined(text)) 
		text = HU.getUrlArgument(ID_TEXT_FIELD);
	    let textInputClass = "display-simplesearch-input display-search-textinput"
	    let attrs  = [ATTR_PLACEHOLDER, this.getEgText("Search text"), ATTR_TITLE,"e.g. name:, contents:,path:", ATTR_CLASS, textInputClass,  ATTR_ID, this.domId(ID_TEXT_FIELD)];
	    let inputAttrs =  [ATTR_CLASS, textInputClass];
	    if(this.getProperty("inputSize")) {
		inputAttrs.push(ATTR_SIZE);
		inputAttrs.push(this.getProperty("inputSize", "30"));
	    } else {
		inputAttrs.push(ATTR_STYLE);
		inputAttrs.push(HU.css("width","100%","min-width","50px","max-width","300px"));
	    }

	    let contents = "";
	    let topContents = "";	    
	    form+=HU.center(searchButton);
	    if(topItems.length>0) {
		if (horizontal) {
		    form += topItems[0];
		    topContents +=  HU.join(topItems.slice(1), "");
		} else {
		    topItems = topItems.map(item=>{return HU.div([ATTR_STYLE,HU.css("margin-right","8px")], item);});
		    form+="<br>";
		    form+=   HU.hrow(...topItems);
		}
	    }
		

	    if(!horizontal)  {
		extra += HU.formTable();
	    }

	    let ancestors  = this.getProperty("ancestors");
	    if(ancestors) {
		extra+=this.addWidget(this.getProperty('ancestorsLabel','Search Under'),
				      HU.div([ID,this.domId(ID_SEARCH_ANCESTORS)]),{toggleClose:true});
		setTimeout(()=>{
		    this.loadAncestors(ancestors);
		},1);
	    }

	    if(this.getShowAncestor()) {
		let ancestor = HU.getUrlArgument(ID_ANCESTOR) ?? this.getProperty("ancestor");
		let name = HU.getUrlArgument(ID_ANCESTOR_NAME) ?? this.getProperty("ancestorName");		
		let aid = this.domId(ID_ANCESTOR);
		let clear = HU.href("javascript:void(0);",HU.getIconImage("fas fa-eraser"), ['onClick',"RamaddaUtils.clearSelect(" + HU.squote(aid) +");",TITLE,"Clear selection"]);
		let input = HU.input("",name||"",["READONLY",null,ATTR_PLACEHOLDER,'Select', ATTR_STYLE,HU.css('cursor','pointer','width','100%'),ID,aid,CLASS,"ramadda-entry-popup-select  disabledinput"]);

		extra += HU.hidden("",ancestor||"",[ID,aid+"_hidden"]);
		extra+=this.addWidget('Search Under',HU.div([ID,this.domId(ID_SEARCH_ANCESTOR)], HU.leftRightTable(clear,input,"5%", "95%")),{toggleClose:true});
	    }


	    let textFields = [];
            let textField = HU.input("", text, Utils.mergeLists([ATTR_TEXT_INPUT,'Text'],attrs,inputAttrs));
            if (this.getShowText()) {
		textFields.push(textField);
            }

            if (this.getShowName()) {
		textFields.push(HU.input("","",
					 Utils.mergeLists([ATTR_TEXT_INPUT,'Name',
							   ATTR_ID,this.domId(ID_NAME_FIELD),ATTR_PLACEHOLDER,'Name'],
							  inputAttrs)));				       
	    }
            if (this.getShowDescription()) {
		textFields.push(HU.input("","",
					 Utils.mergeLists([ATTR_TEXT_INPUT,'Description',ATTR_ID,this.domId(ID_DESCRIPTION_FIELD),ATTR_PLACEHOLDER,'Description'],
							  inputAttrs)));				       
	    }	    

	    if(textFields.length) {
		extra+=this.addWidget('Text',Utils.join(textFields,'<br>'),{
		    toggleClose:this.getProperty('textToggleClose',toggleClose)});
	    }


	    let dateWidget='';
            if (this.getShowDate()) {
                this.dateRangeWidget = new DateRangeWidget(this);
		let label=this.getLabel(this.getStartDateLabel());
		if(Utils.stringDefined(label))
                    extra += this.addWidget(label, HU.div([ID,this.domId(ID_SEARCH_DATE_RANGE)], this.dateRangeWidget.getHtml()));
		else
		    dateWidget+=this.dateRangeWidget.getHtml();
	    }
            if (this.getShowCreateDate(true)) {
		let label=this.getLabel(this.getCreateDateLabel());
                this.createdateRangeWidget = new DateRangeWidget(this,"createdate");
		if(Utils.stringDefined(label))
                    extra += this.addWidget(label, HU.div([ID,this.domId(ID_SEARCH_DATE_CREATE)], this.createdateRangeWidget.getHtml()));
		else 
		    dateWidget+=HU.div([ATTR_STYLE,'margin-top:4px;'],this.createdateRangeWidget.getHtml());
            }
	    if(Utils.stringDefined(dateWidget)) {
		extra+=this.addWidget('Date',dateWidget,{toggleClose:this.getProperty('dateToggleClose',toggleClose)});
	    }
            if (this.getShowArea()) {
		let label=this.getLabel(this.getAreaLabel('Location'));
		let areaWidget =new AreaWidget(this);
                this.addAreaWidget(areaWidget) 
                extra += this.addWidget(label, HU.div([ID,this.domId(ID_SEARCH_AREA)], areaWidget.getHtml()),
					{toggleClose:this.getProperty('areaToggleClose',toggleClose)});
            }
            extra += HU.div([ATTR_ID, this.getDomId(ID_TYPEFIELDS)], "");


            if (Utils.stringDefined(this.getMetadataTypes())) {
		let metadataBlock = "";
                for (let i = 0; i < this.metadataTypeList.length; i++) {
                    let type = this.metadataTypeList[i];
                    let value = type.getValue();
                    let metadataSelect;
                    if (value != null) {
                        metadataSelect = value;
                    } else {
                        metadataSelect = HU.tag(TAG_SELECT, [ATTR_ID, this.getMetadataFieldId(type),
                                ATTR_CLASS, "display-metadatalist"],
                            HU.tag(TAG_OPTION, [ATTR_TITLE, "", ATTR_VALUE, ""],
                                NONE));
                    }
		    if(this.getShowTags()) {
			let block = HU.div([ATTR_CLASS,"display-search-metadata-block"], HU.div([CLASS,"display-search-metadata-block-inner", ATTR_ID,this.getMetadataFieldId(type)]));
			let countId = this.getMetadataFieldId(type)+"_count";
			let wrapperId = this.getMetadataFieldId(type)+"_wrapper";			
			let label = type.getLabel();
			metadataBlock += this.addWidget(label, block,{toggleClose:true});
		    } else {
			metadataBlock += this.addWidget(type.getLabel(), metadataSelect,{toggleClose:true});
		    }
                }
		extra += HU.div([ATTR_ID,this.domId(ID_SEARCH_TAGS)], metadataBlock);
            }

            extra +=HU.div([ATTR_STYLE,'margin-top:1em;border-top:var(--basic-border);',ATTR_CLASS,'display-search-widget'],
			   HU.b('# Records:') +' '+	HU.input("",  DEFAULT_MAX, [ATTR_CLASS,'display-simplesearch-input',
										    ATTR_ID,this.domId(ID_SEARCH_MAX),
										    'size','5']));
	    

	    extra+=HU.div([ATTR_STYLE,'height:100px;']);

	    if(!horizontal) 
		extra += HU.closeTag(TAG_TABLE);

	    contents +=topContents;
	    topContents='';
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
            contents += HU.open("input",[ATTR_TYPE,"submit",ATTR_STYLE,"position:absolute;left:-9999px;width:1px;height:1px;"]);
	    if(this.getFormHeight()) {
		contents = HU.div([ATTR_STYLE,HU.css("overflow-y","auto","max-height",HU.getDimension(this.getFormHeight()))], contents);
	    }

	    if(Utils.stringDefined(topContents)) {
		form+=HU.div([ATTR_CLASS,"display-search-extra"],topContents);
	    }
	    form+=contents;
            form += HU.closeTag("form");
            return form;

        },
	getEgText:function(eg) {
            eg = this.getProperty("placeholder",eg||"Search");
            if (this.eg) {
                eg = " " + this.eg;
            }
	    return eg;
	},

	getFormText:function() {
	    let text = this.getSearchSettings().text;
            if (text == null) {
                let args = Utils.getUrlArgs(document.location.search);
                text = args.text;
            }
            if (text == null) {
                text = this.getSearchText();
            }
	    return text;
	},

        handleEventMapBoundsChanged: function(source, args) {
            if (this.areaWidgets) {
		this.areaWidgets.forEach(areaWidget=>{
                    areaWidget.handleEventMapBoundsChanged(source, args);
		});
            }
        },
        typeChanged: function() {
	    this.jq(ID_SEARCH_BAR).find('[column]').remove();
            let settings = this.getSearchSettings();
            settings.skip = 0;
            settings.setMax(DEFAULT_MAX);
	    let type = this.getFieldValue(this.getDomId(ID_TYPE_FIELD),
						    settings.entryType);
	    settings.entryType = type;
            settings.clearAndAddType(settings.entryType);
            this.addExtraForm();
            this.submitSearchForm();
        },
        initMetadata: function() {
            this.metadata = {};
	    this.metadataList=[];
            this.metadataLoading = {};	    
            for (let i = 0; i < this.metadataTypeList.length; i++) {
                let type = this.metadataTypeList[i];
                this.addMetadata(type, null);
            }
	},
        makeMetadata: function(metadataType,metadata) {
	    if(!metadata.getElements)  {
		metadata=new DisplayEntryMetadata(this,metadataType,metadata);
		this.metadata[metadataType.getType()] = metadata;
		this.metadataList.push(metadata);
	    }
	    return metadata;
	},
        addMetadata: function(metadataType, metadata) {
	    let _this = this;
            if (metadata == null) {
                metadata = this.metadata[metadataType.getType()];
            }
            if (metadata == null) {
                if (!this.metadataLoading[metadataType.getType()]) {
                    this.metadataLoading[metadataType.getType()] = true;
                    metadata = this.getRamadda().getMetadataCount(metadataType, function(metadataType, metadata) {
                        _this.addMetadata(metadataType, metadata);
                    });
                }
            }
            if (metadata == null) {
		return;
            }
	    this.metadata=metadata = this.makeMetadata(metadataType,metadata);
            if (!metadata.getElements()) {
                return;
	    }

	    if(!this.metadataBoxes) this.metadataBoxes={};
	    this.metadataBoxes[metadataType.getType()] = {};
	    let dest =     $("#" + this.getMetadataFieldId(metadataType));
	    dest.html('');
	    this.idToElement = {};
	    let cbxChange = function(){
		let value  = $(this).attr("metadata-value");
		let type  = $(this).attr("metadata-type");
		let index  = $(this).attr("metadata-index");				
		let on = $(this).is(':checked');
		let cbx = $(this);
		let element  = _this.idToElement[$(this).attr('id')];
		if(on) {
		    if(element) element.setCbxOn(value);
		    _this.addMetadataTag(metadataType.getType(), metadataType.getLabel(),value, cbx);
		} else {
		    if(element) element.setCbxOff(value);
		    let tagId = Utils.makeId(_this.domId(ID_SEARCH_TAG) +"_" + metadataType.getType() +"_" + value);
		    $("#" + tagId).remove();
		}		
		_this.submitSearchForm();
	    };

	    let hasMultiple = 	    metadata.getElements().length>1;

	    metadata.getElements().forEach((element)=>{
		if(element.getType()=='string') {
		    let inputChange = function(){
			_this.submitSearchForm();
		    };
		    let input = dest.append(element.makeInput());
		    input.change(inputChange);
		    return;
		}
		if(!element.getValues()) return;
		let popupLimit = this.getTagPopupLimit();
		let cbxs = element.makeCheckboxes(_this.idToElement);
		if(hasMultiple || !this.getShowTags()) {
		    if(element.select) {
			let menu = dest.append(element.select);
			element.menu = menu;
			menu.change(()=>{
			    _this.submitSearchForm();
			});
		    }

		} else {
		    if(cbxs.length>popupLimit) {
			dest.append(HU.div([],'Select')).button().click(function(){
			    let cbxs2 = element.makeCheckboxes(_this.idToElement);
			    _this.createTagDialog(cbxs2, $(this), cbxChange, metadataType.getType(),metadataType.getLabel());
			});
		    } else {
			dest.append(Utils.wrap(cbxs,"","<br>"));
		    }
		}});
	    dest.find(":checkbox").change(cbxChange);
        },

	metadataTagSelected:function(type, value) {
	    let tagGroupId = ID_SEARCH_TAG_GROUP+"_"+type;
	    let tagGroup = this.jq(tagGroupId);
	    let existing = tagGroup.find(HU.attrSelect("metadata-type",type)+HU.attrSelect("metadata-value",value));
	    return (existing.length>0);
	},
	addMetadataTag:function(type, label,value, cbx) {
	    let _this = this;
	    let cbxId = cbx?cbx.attr('id'):'unknowncbx';
	    let tagGroupId = ID_SEARCH_TAG_GROUP+'_'+type;
	    let tagGroup = _this.jq(tagGroupId);
	    if(this.metadataTagSelected(type, value)) return false;
	    let tagId = Utils.makeId(_this.domId(ID_SEARCH_TAG) +'_' +type +'_' + value);
	    if(tagGroup.length==0) {
		tagGroup = $(HU.div([CLASS,'display-search-tag-group',ID,_this.domId(tagGroupId)])).appendTo(_this.jq(ID_SEARCH_BAR));			     
	    }

	    let tag = $(HU.div(['source-id',cbxId,'metadata-type',type,'metadata-value',value,ATTR_TITLE,label+':' + value,
				ATTR_CLASS,'display-search-tag', ATTR_ID,tagId],value+SPACE +HU.getIconImage('fas fa-times'))).appendTo(tagGroup);
	    tag.click(function() {
		let element=_this.idToElement[$(this).attr('source-id')];
		let value = $(this).attr('metadata-value');
		if(element && value) {
		    element.setCbxOff(value);
		}
		$(this).remove();
		if(cbx)
		    cbx.prop('checked',false);
		_this.submitSearchForm();
	    });
	    return true;
	},
	addSearchToTags: function() {
	    return false;
	},
	typeSearchEnabled:function() {
	    return this.jq(ID_TYPE_FIELD).length>0;
	},
	typeTagClicked:function(type) {
	    HtmlUtils.initSelect(this.jq(ID_TYPE_FIELD),{selectOption: type.getId()});
	},	
	metadataTagClicked:function(metadata) {
	    if(!this.metadataBoxes) return;
	    if(!this.metadataBoxes[metadata.type] || !this.metadataBoxes[metadata.type][metadata.value.attr1]) {
		this.addMetadataTag(metadata.type, metadata.type,metadata.value.attr1, null);
		this.submitSearchForm();
		return;
	    }

	    let cbx = $('#' + this.metadataBoxes[metadata.type][metadata.value.attr1]);
	    if(cbx.is(':checked')) return;
	    cbx.click();
	},
        getMetadataFieldId: function(metadataType) {
            let id = metadataType.getType?metadataType.getType():metadataType;
            id = id.replace('.', '_'); 
            return this.getDomId(ID_METADATA_FIELD + id);
        },

        findEntryType: function(typeName) {
            if (this.entryTypes == null) return null;
            for (let i = 0; i < this.entryTypes.length; i++) {
                let type = this.entryTypes[i];
                if (type.getId() == typeName) return type;
            }
            return null;
        },
        addTypes: function(newTypes) {
            if (newTypes == null) {
                newTypes = this.getRamadda().getEntryTypes((ramadda, types) =>{
                    this.addTypes(types);
                },this.getEntryTypes());
		if(newTypes==null) {
		    this.typesPending=true;
		}

            }
            if (newTypes == null) {
                return;
            }

            this.entryTypes = newTypes;

            if (this.getEntryTypes()) {
                let showType = {};
		let typeList = this.getEntryTypes().split(',');
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
            if (!this.getShowType()) {
		this.addExtraForm();
                return;
            }


	    let addTypeCategory=this.getProperty('addTypeCategory');
            let cats = [];
            let catMap = {};
            let select = HU.openTag(TAG_SELECT, [ATTR_ID, this.getDomId(ID_TYPE_FIELD),
						 ATTR_CLASS, 'display-typelist',
						 'onchange', this.getGet() + '.typeChanged();'
            ]);
	    if(this.getProperty('addAllTypes')) {
		select += HU.tag(TAG_OPTION, [ATTR_TITLE, '', ATTR_VALUE, VALUE_ANY_TYPE],'Any type');
	    }
	    if(this.getProperty('addAnyType',true)) {
		select += HU.tag(TAG_OPTION, [ATTR_TITLE, '', ATTR_VALUE, ''],
				 this.getEntryTypes()?'Any of these types':'Any type');
	    }
	    let hadSelected = false;
	    let anySelected = false;
	    let startWithAny=this.getProperty('startWithAny');
	    let fromUrl = HU.getUrlArgument(ID_TYPE_FIELD);
            this.entryTypes.every(type=>{
                anySelected = this.getSearchSettings().hasType(type.getId());
		if(fromUrl)
		    anySelected = type.getId()==fromUrl;
		return !anySelected;
	    });

            for (let i = 0; i < this.entryTypes.length; i++) {
                let type = this.entryTypes[i];
                let icon = type.getIcon();
                let optionAttrs = [ATTR_TITLE, type.getLabel(), ATTR_VALUE, type.getId(), ATTR_CLASS, "display-typelist-type",
                    "data-iconurl", icon
                ];
                let selected = this.getSearchSettings().hasType(type.getId());
		if(!selected) {
		    if(fromUrl)
			selected = type.getId()==fromUrl;
		}
		if(!selected && !anySelected && i==0 && !startWithAny)  selected=true;
                if (selected) {
		    hadSelected = true;
                    optionAttrs.push("selected");
                    optionAttrs.push(null);
                }
		let label = type.getLabel();
		if(type.getEntryCount()>0) 
		    label += " (" + type.getEntryCount() + ")"

                let option = HU.tag(TAG_OPTION, optionAttrs, label);
                let map = catMap[type.getCategory()];
                if (map == null) {
		    if(addTypeCategory) 
			catMap[type.getCategory()] = HU.tag(TAG_OPTION, [ATTR_CLASS, "display-typelist-category", ATTR_TITLE, "", ATTR_VALUE, ""], type.getCategory());
                    cats.push(type.getCategory());
                }
                catMap[type.getCategory()] += option;

            }
            for (let i in cats) {
                select += catMap[cats[i]];
            }

            select += HU.closeTag(TAG_SELECT);
	    if(this.entryTypes.length==0) {
	    } else  if(this.entryTypes.length==1) {
		if(this.entryTypes[0].getId()!='any')
		    this.writeHtml(ID_TYPE_DIV, HU.hidden(ID_TYPE_FIELD,this.entryTypes[0].getId()));
	    } else {
		this.writeHtml(ID_TYPE_DIV, this.addWidget(this.getProperty('typesLabel','Types'),select));
	    }
	    
            HtmlUtils.initSelect(this.jq(ID_TYPE_FIELD),
				 { autoWidth: false,  "max-height":"100px"});
            this.addExtraForm();
	    this.typesPending=false;
	    this.submitSearchForm();
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
            for (let i = 0; i < cols.length; i++) {
                let col = cols[i];
                if (!col.getCanSearch()) continue;
                searchable.push(col);
            }
            return searchable;
        },
	makeLabel:function(label) {
	    label = this.getLabel(label);
	    if(!Utils.stringDefined(label)) return '';
	    return HU.div([ATTR_CLASS,'display-search-label'],label);
	},
	getLabel:function(label) {
	    //check if it is a column
	    if(!label) return '';
	    if(label.getSearchLabel) label = label.getSearchLabel();
	    if(!label) return '';
	    label = label.trim();
//	    if(!label.endsWith(':'))  label = label+':';
	    return label;
	},

        addExtraForm: function() {
	    let toggleClose = this.getProperty('columnsToggleClose',this.getProperty('toggleClose',true));
            if (this.savedValues == null) this.savedValues = {};
            let extra = "";
            let cols = this.getSearchableColumns();
	    let comparators = this.getComparators().split(",");
	    let lastGroup = null;
	    let inGroup=false;
            for (let i = 0; i < cols.length; i++) {
                let col = cols[i];
                if (this.getProperty("fields") != null && this.getProperty("fields").indexOf(col.getName()) < 0) {
                    continue;
                }

		let group = col.getGroup();
		if(Utils.stringDefined(group) && group!=lastGroup) {
		    if(inGroup) extra+='</div></div>';
		    inGroup=true;
		    lastGroup=group;
		    let widgetId = HU.getUniqueId('');
		    extra+=HU.open('div',[ATTR_CLASS,'display-search-group']);
		    let label = this.addToggle(group,widgetId,toggleClose);
		    extra+=HU.div([ATTR_CLASS,'display-search-group-label'],label);
		    extra+=HU.open('div',[ATTR_ID,widgetId,ATTR_STYLE,HU.css('display',toggleClose?'none':'block')]);
		}
                let field = "";
                let id = this.getDomId(ID_COLUMN + col.getName());
                let savedValue = this.savedValues[id];
                if (savedValue == null) {
                    savedValue = this.jq(ID_COLUMN + col.getName()).val();
                }
//                if (savedValue == null) savedValue = "";
		let widget = "";
		let label="";
		let help = "";
		if(col.getSuffix()) {
		    help = HU.span([ATTR_STYLE,HU.css('cursor','help','margin-left','10px'), TITLE,col.getSuffix()], HU.getIconImage("fas fa-info"));
		}		
		
                if (col.isEnumeration()) {
		    let showLabels = this.getShowSearchLabels();
		    let values = col.getValues();
		    let searchValue = this.getSearchValue(col.getName());
		    if(col.showCheckboxes()) {
			for (let vidx in values) {
                            let value = values[vidx].value||"";
			    if(value=="") {
				value = "--blank--";
			    }
                            let label = values[vidx].label.trim();
			    if(label=="&lt;blank&gt;") label="--blank--";
			    if(label=="")
				label= "--blank--"; 
			    let boxId = id+'_'+vidx;
                            field += HU.div([],HU.checkbox(boxId,[ATTR_CLASS,'display-entrylist-enum-checkbox',ATTR_ID,boxId,'checkbox-id',id,'data-value',value],
							   value==searchValue, label));
			}
		    } else {
			let clazz = 'display-metadatalist';
			let attrs = [ATTR_ID, id];
			let optionAttrs = [CLASS,"display-metadatalist-item", ATTR_TITLE, "", ATTR_VALUE, VALUE_NONE];
			if(col.getSearchMultiples()) {
			    attrs.push('multiple',null);
			    attrs.push('size','4');			    
			} else {
			    clazz= 'display-searchmenu ' + clazz;
			}
			attrs.push(ATTR_CLASS,clazz);
			field = HU.openTag(TAG_SELECT, attrs);
			field+="\n";
			if(!col.getSearchMultiples()) {
			    field += HU.tag(TAG_OPTION, optionAttrs,
					    showLabels?"-- Select --":col.getSearchLabel());
			}
			field+="\n";
			for (let vidx in values) {
                            let value = values[vidx].value||"";
                            let extraAttr = "";
                            if (value == savedValue || value==searchValue) {
				extraAttr = " selected ";
                            }
			    if(value=="") {
				value = "--blank--";
			    }
                            let label = values[vidx].label.trim();
			    if(label=="&lt;blank&gt;") label="--blank--";
			    if(label=="")
				label= "--blank--"; 
                            field += HU.tag(TAG_OPTION, [CLASS,"display-metadatalist-item", ATTR_TITLE, label, ATTR_VALUE, value, extraAttr, null],
					    label);
			    field+="\n";
			}
			field += HU.closeTag(TAG_SELECT);
		    }

		    if(showLabels) {
			label =  this.getLabel(col);
			widget = field+help;
		    } else {
			widget= HU.div([CLASS,"display-search-block"], field+help);
		    }
		} else if (col.isNumeric()) {
		    let from = HU.input("", "", [ATTR_TITLE,"greater than",ATTR_CLASS, "input display-simplesearch-input", ATTR_STYLE,HU.css("width","2.5em"), ATTR_ID, id+"_from"]);
		    let to = HU.input("", "", [ATTR_TITLE,"less than",ATTR_CLASS, "input display-simplesearch-input", ATTR_STYLE,HU.css("width","2.5em"), ATTR_ID, id+"_to"]);		    
		    label = col.getSearchLabel();
                    widget = from +" - " + to +help;
                } else if(col.isLatLon()) {
		    let areaWidget= col.areaWidget = new AreaWidget(this,col.getName());
		    label = this.makeLabel(col.getSearchLabel());
                    widget= HU.div([ATTR_ID,this.domId(col.getName())], areaWidget.getHtml());
                } else if(col.getType()=='string') {
                    field = HU.input("", savedValue??this.getSearchValue(col.getName()), ["placeholder",col.getSearchLabel(),ATTR_CLASS, "input display-simplesearch-input", ATTR_SIZE, this.getTextInputSize(), ATTR_ID, id]);
                    widget =  field + " " + help;
		}
		extra+=this.addWidget(label,widget,{
		    addToggle:!inGroup
		});
	    }

	    if(inGroup) extra+='</div></div>';
            this.writeHtml(ID_TYPEFIELDS, extra);
	    let _this = this;
	    this.jq(ID_TYPEFIELDS).find(".ramadda-expr").change(function() {
		let id = $(this).attr("id");
		let val = $(this).val();
		id  = id.replace("_expr","_to");
		if(val=="between")
		    $("#" + id).show();
		else
		    $("#" + id).hide();
	    });
	    let cbxs = this.jq(ID_TYPEFIELDS).find(".display-entrylist-enum-checkbox");
	    cbxs.change(()=>{
		this.submitSearchForm();
	    });

	    let menus = this.jq(ID_TYPEFIELDS).find(".display-searchmenu");
	    HtmlUtils.initSelect(menus);
	    let allMenus = this.jq(ID_TYPEFIELDS).find(".display-metadatalist");
	    allMenus.change(()=>{
		this.submitSearchForm();
	    });
	    cols.forEach(col=>{
		if(col.areaWidget) {
		    col.areaWidget.initHtml();
		}
	    });
        },
        getEntries: function() {
            if (this.entryList == null) return [];
            return this.entryList.getEntries();
        },
        loadNextUrl: function() {
            let skip = +this.getSearchSettings().skip + parseInt(this.getSearchSettings().getMax());
	    this.getSearchSettings().skip = skip;
            this.submitSearchForm();
        },
        loadMore: function() {
            this.getSearchSettings().setMax( parseInt(this.getSearchSettings().getMax())+ DEFAULT_MAX);
	    this.jq(ID_SEARCH_MAX).val(this.getSearchSettings().getMax());

            this.submitSearchForm();
        },
        loadLess: function() {
            let max = this.getSearchSettings().getMax();
            max = parseInt(0.75 * max);
            this.getSearchSettings().setMax( Math.max(1, max));
	    this.jq(ID_SEARCH_MAX).val(this.getSearchSettings().getMax());
            this.submitSearchForm();
        },
        loadPrevUrl: function() {
            this.getSearchSettings().skip = Math.max(0, this.getSearchSettings().skip - this.getSearchSettings().getMax());
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
            this.providerChanged(true);
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
	    if(!this.getProvidersMultiple()) {
		HU.initSelect(this.jq(ID_PROVIDERS), { multiple:true,autoWidth: false,  "max-height":"100px"});
	    }
            this.jq(ID_PROVIDERS).change(function() {
                _this.providerChanged();
            });
        },
        providerChanged: function(initialCall) {
	    if(this.jq(ID_PROVIDERS).length==0) return;
	    if(!initialCall && this.jq(ID_ANCESTOR).val) {
		this.jq(ID_ANCESTOR).val("");
		this.jq(ID_ANCESTOR+"_hidden").val("");		
	    }
	    this.getSearchSettings().skip=0;
            let id = this.jq(ID_PROVIDERS).val();
	    this.provider = this.providerMap[id];
	    if(Utils.stringDefined(id) && id!='this') {
		this.addToDocumentUrl(ID_PROVIDERS,id);
	    }
	    this.jq(ID_SEARCH_BAR).html("");
	    let blocks = [ID_SEARCH_AREA, ID_SEARCH_TAGS,ID_SEARCH_ANCESTOR,ID_SEARCH_DATE_CREATE,ID_SEARCH_DATE_RANGE];
            if (this.provider && this.provider.type!="ramadda") {
		this.setRamadda(this.originalRamadda);
		let caps = (this.provider.capabilities||"").split(",");
		caps.forEach(cap=>{
		    let index = blocks.indexOf("search_" + cap);
		    if(index>=0) {
			blocks.splice(index,1);
		    }
		});
		//area
		blocks.forEach(id=>{
		    this.jq(id).hide();
		});
		this.jq(ID_TYPE_DIV).hide();
            } else {
		blocks.forEach(id=>{
		    this.jq(id).show();
		});
		this.jq(ID_TYPE_DIV).show();
		if(this.getSearchDirect()) {
		    if(this.provider)
			this.setRamadda(getRamadda(this.provider.id+";"+ this.provider.name));
		    this.addTypes();
		    this.initMetadata();
		}
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
            let textField = HU.input("", text, [ATTR_PLACEHOLDER, eg, ATTR_CLASS, "display-search-input", ATTR_SIZE, "30", ATTR_ID, this.getDomId(ID_TEXT_FIELD)]);
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
	    this.tabId = null;
	    this.mapId = null;
	    if(this.myDisplays) {
		this.myDisplays.forEach(info=>{
		    if(info.display)
			removeRamaddaDisplay(info.display.getId());
		});
	    }
	    this.myDisplays = [];


	    let titles = [];
	    let contents = [];

	    let addContents=c=>{
		contents.push(HU.div([ATTR_CLASS,'display-entrylist-results'],c));
	    }

	    let makeExpandable= (html) =>{
		html =HU.div([ATTR_STYLE,HU.css('max-height','1000px','background','#fff','overflow-y','auto')],html);
		return HU.div([ATTR_CLASS,'ramadda-expandable-wrapper',ATTR_STYLE,HU.css('position','relative')],html);
	    }

	    this.getDisplayTypes('list').split(',').forEach(type=>{
		if(type=='list') {
		    titles.push('List');
		    addContents(makeExpandable(this.getEntriesTree(entries)));
		} else if(type=='images') {
		    let defaultImage = this.getDefaultImage();
		    let imageEntries = entries.filter(entry=>{
			if(defaultImage) return true;
			return entry.isImage();
		    });
		    if(imageEntries.length>0) {
			titles.push('Images');
			let id = HU.getUniqueId(type +'_');
			this.myDisplays.push({id:id,type:type});
			let images =HU.div([ATTR_ID,id,ATTR_CLASS,'ramadda-expandable display-entrylist-images',ATTR_STYLE,HU.css('width','100%')]);
			addContents(makeExpandable(images));
		    }
		} else if(type=='timeline') {
		    titles.push('Timeline');
		    let id = HU.getUniqueId(type +'_');
		    this.myDisplays.push({id:id,type:type});
		    addContents(HU.div([ID,id,ATTR_STYLE,HU.css('width','100%')]));
		} else if(type=='map') {
		    this.areaEntries = entries.filter(entry=>{
			return entry.hasBounds() || entry.hasLocation();
		    });
		    if(this.areaEntries.length>0) {
			titles.push('Map');
			let id = HU.getUniqueId(type +'_');
			this.myDisplays.push({id:id,type:type,entries:this.areaEntries});
			addContents(HU.div([ID,id,ATTR_STYLE,HU.css('width','100%')]));
		    }

		} else if(type=='metadata') {		    
		    titles.push('Metadata');
		    let mtd = HU.div([ATTR_STYLE,HU.css('width','800px','max-width','800px','overflow-x','auto')],this.getEntriesMetadata(entries));
		    addContents(mtd);
		} else {
		    console.log('unknown display:' + type);
		}
	    });

	    if(titles.length==1) 
		return HU.div([CLASS,'display-entrylist-content-border'],contents[0]);
	    let tabId = HU.getUniqueId('tabs_');
	    let tabs = HU.open('div',[ID,tabId,CLASS,'ui-tabs']) +'<ul>';
	    titles.forEach((title,idx)=>{
		tabs +='<li>' +HU.href('#' + tabId+'-' + idx,title) +'</li>\n'
	    })
	    tabs +='</ul>\n';
	    this.tabCount = contents.length;
	    contents.forEach((content,idx)=>{
		tabs +=HU.div([ID,tabId+'-' + idx,CLASS,'ui-tabs-hide'], content);
	    });
	    tabs +=HU.close('div');
	    this.tabId = tabId;
	    return tabs;
        },

	handleSearchLink:function(event,button,dontAsk) {
	    let copy = button.attr('data-copy');
	    if(copy) {
		Utils.copyToClipboard(copy);
		alert('The URL has been copied to the clipboard');
		return;
	    }		

	    let url = button.attr('data-url');
	    let format = button.attr('data-name')
	    let size = "100";
	    if(!dontAsk) {
		size = prompt('How many records do you want in the ' + format +' download?',this.jq(ID_SEARCH_MAX).val());
		if(!size) return;
	    }
	    url = url.replace(/max=\d+/,'max='+size);
            if(event.shiftKey) {
		let protocol = window.location.protocol;
		let hostname = window.location.hostname;
		let port = window.location.port;
		let prefix = `${protocol}//${hostname}${port ? `:${port}` : ''}`;
		url = prefix+url;
		Utils.copyToClipboard(url);
		alert('The URL has been copied to the clipboard');
	    } else {
		Utils.triggerDownload(url);
	    }
	},
        entryListChanged: function(entryList) {
            if (this.multiSearch) {
                this.multiSearch.count--;
            }
            SUPER.entryListChanged.apply(this, [entryList]);
            let entries = this.entryList.getEntries();

            if (entries.length == 0) {
//                this.getSearchSettings().skip = 0;
                this.getSearchSettings().setMax(DEFAULT_MAX);
                let msg = "Nothing found";
                if (this.multiSearch) {
                    if (this.multiSearch.count > 0) {
                        msg = "Nothing found so far. Still searching " + this.multiSearch.count + " repositories";
                    } else {}
                }
                this.writeHtml(ID_FOOTER_LEFT, "");
		this.jq(ID_ENTRIES).html(this.getMessage(msg));
//                this.writeMessage(msg);		
                this.getDisplayManager().handleEventEntriesChanged(this, []);
//		this.jq(ID_ENTRIES).html("");
//                return;
            }
	    this.writeMessage(this.getResultsHeader(entries));
            if (entries.length == 0) {
		return
	    }

            let get = this.getGet();
            this.writeHtml(ID_FOOTER_LEFT, "");
            if (this.footerRight != null) {
		let _this =this;
                this.writeHtml(ID_FOOTER_RIGHT, this.footerRight);
		this.jq(ID_FOOTER_RIGHT).find('.ramadda-search-link').button().click(function(event) {
		    let custom  =$(this).attr('custom-output');
		    _this.handleSearchLink(event,$(this),custom);
		});
            }

            let entriesHtml = this.makeEntriesDisplay(entries);
	    let html = entriesHtml;
            this.writeEntries(html, entries);
	    this.jq(ID_ENTRIES).find('.ramadda-metadata-bigtext').each(function() {
		Utils.initBigText($(this));
	    });

            this.addEntrySelect();
            this.getDisplayManager().handleEventEntriesChanged(this, entries);
	    if(this.galleryId) {
		HU.createFancyBox($("#" + this.galleryId).find("a.popup_image"),{helpers:{title:{type:'over'}}});
	    }


	    let tabbed = (event,ui)=>{
		this.activeTabIndex = ui.newTab.index();
		HtmlUtil.tabLoaded();
	    };
	    if(this.tabId) {
		let index = this.activeTabIndex;
		if(index>=this.tabCount)  index = this.tabCount-1;
		$('#' + this.tabId).tabs({activate: tabbed,active: index});
	    }	
	    if(this.myDisplays && this.myDisplays.length) {
		let index=0;
		let fields = [new RecordField({type: "string", index: (index++), id: "name",label: "Name"}),
			      new RecordField({type: "string", index: (index++), id: "description",label: "Description"}),
			      new RecordField({type: "date", index: (index++), id: "date",label: "Date"}),			      
			      new RecordField({type: "url", index: (index++), id: "url",label: "URL"}),
			      new RecordField({type: "image", index: (index++), id: "image",label: "Image"}),
			      new RecordField({type: "url", index: (index++), id: "iconUrl",label: "Icon"}),
			      new RecordField({type: "string", index: (index++), id: "tags",label: "Tags"}),
			      new RecordField({type: "string", index: (index++), id: "display_html",label: "Display Html"}),
			      new RecordField({type: "string", index: (index++), id: "id",label: "Entry ID"}),			      
			      new RecordField({index: (index++), id: "latitude",label: "Latitude"}),
			      new RecordField({index: (index++), id: "longitude",label: "Longitude"}),			      			      					     ]
		let entryType = null;
		if(this.entryTypes && this.entryTypes.length) {
		    entryType = this.entryTypes[0];
		    entryType.getColumns().forEach(column=>{
			fields.push(new RecordField({index: (index++), id: column.getName(),label: column.getLabel()}));
		    });
		}
		let records = [];
		let defaultImage = this.getDefaultImage();
		if(defaultImage) {
		    if(defaultImage.startsWith("http"))
			defaultImage = ramaddaBaseUrl+ defaultImage;
		}
		let makeData = entries=>{
		    let records = [];
		    entries.forEach(entry=>{
			let tags = this.makeEntryTags(entry,true,"");
			let displayHtml = entry.displayHtml??entry.getName()
			let data = [entry.getName(true),
				    entry.getSnippet()||"",
				    entry.getStartDate(),
				    entry.getEntryUrl(),
				    entry.getImageUrl()||defaultImage||"",
				    entry.getIconUrl(),
				    tags,
				    displayHtml,
				    entry.getId(),
				    entry.getLatitude(),
				    entry.getLongitude()];
			if(entryType) {
			    entryType.getColumns().forEach(column=>{
				let v = entry.getAttributeValue(column.getName());
				data.push(v);
			    });
			}
			records.push(new PointRecord(fields, entry.getLatitude(),entry.getLongitude(),NaN,entry.getStartDate() || entry.getCreateDate(),data,0));
		    });
		    return records;
		};
		let baseData= new  PointData("pointdata", fields, makeData(entries),null,null);
		let _this = this;
		this.myDisplays.forEach(info=> {
		    let data = info.entries?new  PointData("pointdata", fields, makeData(info.entries)):baseData;
		    let dialogListener = (display, dialog)=>{
			dialog.find(".display-search-tag").click(function() {
			    let type = $(this).attr("metadata-type");
			    let value = $(this).attr("metadata-value");			    
			    if(!_this.addMetadataTag(type,type, value)) return;
			    _this.submitSearchForm();
			});
		    };
		    let tooltip = this.getProperty("tooltip")??"${default}";
		    let myTextGetter = null;


		    if(info.type=='map' && entryType && entryType.mapwiki) {
			myTextGetter = (display, records)=>{
			    if(records.length>1) return null;
			    let uid = HU.getUniqueId();
			    let entryId = records[0].data[8];
			    this.wikify(entryType.mapwiki,entryId,null,null,uid);
			    return HU.div([ATTR_ID,uid],
					  HU.center(HU.image(ramaddaCdn + '/icons/mapprogress.gif',[ATTR_WIDTH,'80px'])));
			};
		    }
		    let props = {centerOnMarkersAfterUpdate:true,
				 dialogListener: dialogListener,
				 highlightColor:"#436EEE",
				 blockStyle:this.getProperty("blockStyle",""),
				 doPopup:this.getProperty("doPopup",true),
				 tooltip:tooltip,
				 tooltipClick:tooltip,
				 myTextGetter:myTextGetter,
				 descriptionField:"description",
				 imageWidth:"140px",
				 blockWidth:"150px",
				 numberOfImages:500,
				 includeNonImages:this.getProperty('includeNonImages',true),
				 showTableOfContents:true,

				 showTableOfContentsTooltip:false,
				 addMapLocationToUrl:false,
				 iconField:"iconUrl",
				 iconSize:16,
				 displayEntries:false,
				 imageField:"image",
				 urlField:"url",
				 titleField:"name",
				 labelField:"name",
				 labelFields:"name",
				 showBottomLabel:false,
				 bottomLabelTemplate:"",
				 topLabelTemplate:"${name}",
				 textTemplate:"${description}",
				 displayId:info.id,
				 divid:info.id,
				 showMenu:false,
				 theData:data,
				 displayStyle:"",
				 mapHeight:'400',
				 loadingMessage:''};
		    info.display =  this.getDisplayManager().createDisplay(info.type,props);
		});
	    }





	    this.jq(ID_ENTRIES).find('.ramadda-expandable-wrapper').each(function() {
		HU.makeExpandable($(this), false,{right:'5px',top:'0px'});
	    });


	    if(this.mapId && this.areaEntries && this.areaEntries.length>0) {
		let map = new RepositoryMap(this.mapId);
		map.initMap(false);
		this.areaEntries.forEach(entry=>{
                    let link = HU.tag(TAG_A, ["target","_entries",ATTR_HREF, entry.getEntryUrl()], entry.getName());
		    let text = link;
		    if(entry.isImage()) {
			text = HU.image(entry.getResourceUrl(), [ATTR_WIDTH, "400", ATTR_ID,
								 this.getDomId("entry_" + entry.getIdForDom()),
								 ATTR_ENTRYID, entry.getId(), ATTR_CLASS, "display-entrygallery-entry"
								]) +"<br>" + link;


			
		    }
//		    map.addMarker:  function(id, location, iconUrl, markerName, text, parentId, size, yoffset, canSelect, attrs) {
		    map.addMarker('',{x:entry.getLongitude(),y:entry.getLatitude()}, entry.getIconUrl(),"",text,null,16,0,true,{});
/*
{"pointRadius":16,
												     "strokeWidth":1,
												     "fillColor":"blue",
												     "strokeColor":"#000"},text);
*/
		});
		map.centerOnMarkersInit(null);
	    }

        },
    });
}



function RamaddaSimplesearchDisplay(displayManager, id, properties) {
    let myProps = [
	{label:'Simple Search'},
	{p:'resultsPosition',ex:'absolute|relative'},
	{p:'maxHeight',ex:300},
	{p:'maxWidth',ex:200},
	{p:'maxWidth',ex:200},		
	{p:'autoSearch',ex:true},
	{p:'showHeader',ex:true},
	{p:'inputSize',d:'200px',ex:'100%'},
	{p:'placeholder'},
	{p:'searchEntryType',ex:'',tt:'Constrain search to entries of this type'},		
	{p:'doPageSearch',ex:'true'},
	{p:'autoFocus',d:true,ex:'false'},	
	{p:'doTagSearch',ex:'true'},
	{p:'tagShowGroup',d:true},
	{p:'tagSearchLimit',tt:'Show the inline search box for tags when the #tags exceeds the limit',d:15},
        {p:'showParent',ex:'true',tt:'Show parent entry in search results'},	
	{p:'pageSearchSelector',d:'.search-component,.entry-list-row-data'},
	{p:'pageSearchParent',ex:'.class or #id',tt:'set this to limit the scope of the search'},		
    ];

    if(!properties.width) properties.width=properties.inputSize??"230px";
    const SUPER   = new RamaddaSearcherDisplay(displayManager, id, DISPLAY_SIMPLESEARCH, properties);
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
	callNumber:1,
        haveDisplayed: false,
        selectedEntries: [],
        getSelectedEntries: function() {
            return this.selectedEntries;
        },
        getDoInlineSearch: function() {
	    return this.getDoPageSearch() || this.getDoTagSearch();
	},
        initDisplay: function() {
	    $(document).ready(()=> {
		this.initDisplayInner();
	    });
	},
        initDisplayInner: function() {
            let _this = this;
            if (this.getIsLayoutFixed() && this.haveDisplayed) {
                return;
            }
            this.haveDisplayed = true;
            this.createUI();


	    let contents = "";
	    if(this.getDoTagSearch()) {
		let sel = this.getPageSearchSelectors();
		contents += '<div class=metadata-tags>';
		let tags ={};
		let list = [];
		sel.find('.metadata-tag').each(function() {
		    $(this).addClass('ramadda-clickable').click(function(){
			_this.selectTag($(this).attr('metadata-tag'));
		    });
		    let tag = $(this).attr('metadata-tag');
		    if(!tags[tag]) {
			tags[tag] = {
			    group:$(this).attr('metadata-group'),
			    tag:tag,
			    count:0,
			    elements:[]
			}
			list.push(tags[tag]);
		    }
		    tags[tag].count++;
		    tags[tag].elements.push($(this));
		});
		list = list.sort((a,b)=>{
		    if(a.group && b.group) {
			let c = a.group.localeCompare(b.group);
			if(c!=0) return c;
		    }
		    return b.count-a.count;
		});
		let groupMap={};
		let groups=[];
		list.forEach(obj=>{
		    let currentGroup=obj.group??''
		    let group = groupMap[currentGroup];
		    if(!group) {
			group = groupMap[currentGroup] = {
			    contents:'',
			    cnt:0
			}
			groups.push(currentGroup);
		    }
	    
		    group.cnt++;
		    let tag = obj.tag;
		    let ele = obj.elements[0];
		    if(ele.attr('data-image-url')) {
			let title = ele.attr('title')+HU.getTitleBr()??'';
			title+='Click to filter';
			group.contents+=HU.image(ele.attr('data-image-url'),[CLASS,'metadata-tag ramadda-clickable','metadata-tag',tag,'title',title]);
		    } else {
			let label = '#'+obj.count+': ' + tag.replace(/^[^:]+:/,'');
			style = ele.attr(ATTR_STYLE);
			group.contents+=HU.div(['data-background',ele.attr('data-background'),
					  'data-style',style??'',
					  ATTR_STYLE,style??'',ATTR_CLASS,'metadata-tag ramadda-clickable','metadata-tag',tag],label);
		    }
		});
		groups.forEach(g=>{
		    let group = groupMap[g];
		    let block = '';
		    if(g!='') {
			if(this.getTagShowGroup()) {
			    block+=HU.b(g)+': ';
			}
			group.contentsid=HU.getUniqueId('taggroup');
			if(group.cnt>this.getTagSearchLimit()) {
			    group.uid=HU.getUniqueId('taggroup');
			    block+=HU.span([ATTR_ID,group.uid]);
			}			    
		    }			
		    block+=HU.span([ATTR_ID,group.contentsid],group.contents);
		    contents+=HU.div([ATTR_STYLE,'text-align:left;'],block);
		})
		contents+='<div>';
		if(this.getDoPageSearch()) {
		    contents = HU.center(this.getDefaultHtml() + contents);
		} 
		this.setContents(contents);
		groups.forEach(g=>{
		    let group = groupMap[g];
		    if(group.uid) {
			HU.initPageSearch('#' + group.contentsid +' .metadata-tag',null,'Find tags',false,{target:'#'+group.uid,inputSize:'10'});
		    }
		});
		this.find(".metadata-tag").click(function(){
		    if($(this).hasClass("metadata-tag-selected")) {
			$(this).removeClass("metadata-tag-selected");
			$(this).css('background',$(this).attr('data-background')??"");
			let style = $(this).attr('data-style');
			if(style) $(this).attr(ATTR_STYLE,style);
		    } else {
			$(this).addClass("metadata-tag-selected");
			$(this).css('background','');
		    }
		    _this.doInlineSearch();
		});

	    } else {
		this.setContents(this.getDefaultHtml());
	    }


	    if(this.getDoPageSearch() && this.getAutoFocus()) {
		//Put this in a timeout because if it is in a tabs then the whole page gets scrolled
                setTimeout(()=>{
		    this.jq(ID_TEXT_FIELD).focus();
                },500);
	    }

	    this.initHtml();
	    let input = this.jq(ID_TEXT_FIELD);
	    if(this.getAutoSearch(true)) {
		//KEY
		input.keyup(function(event) {
		    _this.getSearchSettings().skip =0;
                    _this.getSearchSettings().setMax(DEFAULT_MAX);
		    let val = $(this).val().trim();
		    if(val=="") {
			_this.clearSearch();
			return;
		    }
		    if(!_this.getDoPageSearch()) {
			if(val.length<4) return;
		    }
		    let myCallNumber = ++_this.callNumber;
		    //Wait a bit in case more keys are coming
		    setTimeout(()=>{
			if(myCallNumber == _this.callNumber) {
			    _this.doSearch(true,myCallNumber);
			} else {
			}
		    },400);
		});
	    }

            this.jq(ID_SEARCH).click(function(event) {
		_this.doSearch(false,++_this.callNumber);
                event.preventDefault();
            });
            this.jq(ID_FORM).submit(function(event) {
		_this.doSearch(false,++_this.callNumber);
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
		let entries = HU.div([ID,this.domId(ID_ENTRIES),CLASS,"display-simplesearch-entries",ATTR_STYLE,style]);
		if (this.getShowHeader(true)) {
		    html+=  HU.div([ATTR_CLASS, "display-entries-results", ATTR_ID, this.getDomId(ID_RESULTS)]);
		}
		html+=entries;
	    }
	    return html;
	},
        makeSearchForm: function() {
            let form = HU.openTag("form", [ATTR_ID, this.getDomId(ID_FORM), "action", "#"]);
	    
	    let eg = this.getEgText();
	    let text  = this.getFormText();
	    let size = HU.getDimension(this.getPropertyInputSize());
            let textField = HU.input("", text, [ATTR_STYLE, HU.css("width", size), ATTR_PLACEHOLDER, eg, ATTR_CLASS, "display-search-input", ATTR_ID, this.getDomId(ID_TEXT_FIELD)]);

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

	getPageSearchSelectors:function() {
	    let top = this.getPageSearchParent() || "body";
	    let parent = $(top);
	    //Try with "#" id
	    if(parent.length==0 && this.getPageSearchParent()) {
		let selector = this.getPageSearchParent();
		if(selector.startsWith('.')) {
		    parent = $(selector);
		} else  {
		    parent = $("#"+selector);
		}		    
	    }

	    let sel=parent.find(this.getPageSearchSelector());
	    if(sel.length==0) {
		console.log(this.type+" could not find page search components:" + this.getPageSearchSelector());
	    }
	    return sel;

	},
	selectTag:function(tag) {
	    let _this  = this;
	    let tags = this.find(".metadata-tag");
	    tags.each(function() {
		if(tag == $(this).attr('metadata-tag')) {
		    $(this).addClass("metadata-tag-selected");
		    _this.doInlineSearch();
		}
	    });
	},
	doInlineSearch:function() {
	    let regExp;
	    let value;
	    let selectedTags;
	    if(this.getDoPageSearch()) {
		value = (this.jq(ID_TEXT_FIELD).val()||"").trim();
		value  = value.toLowerCase();
		if(value!="") {
		    try {
			regExp  = new RegExp(value);
		    } catch(err) {
			console.log("bad regexp:" + err);
		    }
		} else {
		    value = null;
		}
	    }

	    if(this.getDoTagSearch()) {
		let tags = this.find(".metadata-tag-selected");
		if(tags.length>0) {
		    tags.each(function(){
			let tag = $(this).attr("metadata-tag");
			if(!selectedTags) selectedTags={};
			selectedTags[tag] = true;
		    });
		}
	    }

	    if(!value && !selectedTags) {
		this.clearPageSearch();
		return
	    }
	    let sel = this.getPageSearchSelectors();
	    sel.each(function() {
		let tagOk = true;
		let textOk = true;		
		if(selectedTags) {
		    tagOk = false;
		    let t = $(this).find(".metadata-tag");
		    t.each(function(){
			let tag = $(this).attr("metadata-tag");
			if(selectedTags[tag]) tagOk=true;
		    });
		}
		if(value) {
		    textOk = false;
		    let html = Utils.stripTags($(this).html()).toLowerCase();
		    if(html.indexOf(value)>=0) {
			textOk=true;
		    } else if(regExp) {
			if(html.match(regExp)) textOk = true;
		    }
		}
		if(!tagOk || !textOk) {
		    $(this).fadeOut();
		} else {
		    $(this).show();
		}
	    });
					  
	    
	},
	clearPageSearch:function() {
	    let sel = this.getPageSearchSelectors();
	    sel.show();
	},
	clearSearch:function() {
	    this.writeMessage("");
	    this.writeEntries("");			
	    if(this.dialog) {
		this.dialog.remove();
		this.dialog = null;
	    }
	    if(this.getDoInlineSearch()) {
		this.doInlineSearch();
	    }
	},
	doSearch:function(auto, callNumber) {
	    if(this.getDoPageSearch()) {
		this.doInlineSearch();
		return;
	    }
	    this.submitSearchForm(auto,callNumber);
	},
        submitSearchForm: function(auto, callNumber) {
	    this.writeMessage(this.getWaitImage() + " " +"Searching...");
	    if(callNumber==null) callNumber = this.callNumber;
            this.haveSearched = true;
            let settings  =this.makeSearchSettings();
	    settings.entryType = this.getSearchEntryType();	    
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
                this.getSearchSettings().setMax(DEFAULT_MAX);
		this.handleNoEntries();
                return;
            }
            this.writeMessage(this.getResultsHeader(entries, true));
	    this.initCloser(ID_RESULTS);

            let get = this.getGet();
            this.writeHtml(ID_FOOTER_LEFT, "");
            if (this.footerRight != null) {
                this.writeHtml(ID_FOOTER_RIGHT, this.footerRight);
            }


	    let html = "";
	    let inner = "";
	    let map = {};
	    let showParent = this.getProperty("showParent");
	    entries.forEach((entry,idx) =>{
		map[entry.getId()] = entry;
		let thumb = entry.getThumbnail();
		let attrs = [TITLE,entry.getName(),CLASS,"display-simplesearch-entry","entryid",entry.getId()];
		if(thumb) attrs.push("thumbnail",thumb);
		let link = HU.href(this.getRamadda().getEntryUrl(entry),entry.getIconImage() +"  "+ entry.getName());
		if(showParent && entry.getParentName()) {
		    let url = ramaddaBaseUrl+ "/entry/show?entryid=" + entry.parent;
		    let plink = HU.href(url, HU.image(entry.parentIcon) +" " + entry.parentName);
		    link = HU.hbox([plink,HU.span(['style','margin-right:4px;margin-left:4px;'],"&raquo;"), link]);
		}
		inner+=HU.div(attrs, link);
	    });
//	    inner = HU.div([CLASS,"display-simplesearch-entries"],inner);
            this.writeEntries(inner, entries);
	    let _this = this;
	    this.jq(ID_ENTRIES).find(".display-simplesearch-entry").tooltip({
		show: {
		    delay: 1000,
		    duration: 300
		},
		content: function() {
		    let entry = map[$(this).attr("entryid")];
		    if(!entry) return null;
		    let thumb = $(this).attr("thumbnail");
		    let parent;
		    let html =entry.getIconImage()+' '+ HU.b(entry.getName());
		    html+=HU.div([],'Type: ' + entry.getTypeName());
		    let snippet = entry.getSnippet();
		    if(snippet)
			html+=HU.div([ATTR_STYLE,HU.css('border-top','var(--basic-border)')],snippet);
		    if(thumb) {
			html+=
			    HU.div([ATTR_STYLE,HU.css('max-height','100px','overflow-y','hidden','border-top','var(--basic-border)')],
				   HU.image(thumb,['width','300px']));
		    }
		    return html;
		}});

            this.getDisplayManager().handleEventEntriesChanged(this, entries);
        },

    });
}





function RamaddaEntrygridDisplay(displayManager, id, properties) {
    const ID_CONTENTS = "contents";
    const ID_GRID = "grid";
    const ID_AXIS_LEFT = "axis_left";
    const ID_AXIS_BOTTOM = "axis_bottom";
    const ID_CANVAS = "canvas";
    const ID_LINKS = "links";
    const ID_RIGHT = "right";

    const ID_SETTINGS = "gridsettings";
    const ID_YAXIS_ASCENDING = "yAxisAscending";
    const ID_YAXIS_SCALE = "scaleHeight";
    const ID_XAXIS_ASCENDING = "xAxisAscending";
    const ID_XAXIS_TYPE = "xAxisType";
    const ID_YAXIS_TYPE = "yAxisType";
    const ID_XAXIS_SCALE = "scaleWidth";
    const ID_SHOW_ICON = "showIcon";
    const ID_SHOW_NAME = "showName";
    const ID_COLOR = "boxColor";

    let myProps = [];
    let SUPER = new RamaddaEntryDisplay(displayManager, id, DISPLAY_ENTRY_GRID, properties);
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
        entries: properties.entries,
        initDisplay: function() {
            let _this = this;
            this.createUI();
            let html = HU.div([ATTR_ID, this.getDomId(ID_CONTENTS)], this.getLoadingMessage("Loading entries..."));
            this.setContents(html);
            if (!this.entryIds) {
                _this.jq(ID_CONTENTS).html(this.getLoadingMessage("No entries specified"));
                return;
            }
            let props = {
                entries: this.entryIds
            };
            let searchSettings = new EntrySearchSettings(props);
            let jsonUrl = this.getRamadda().getSearchUrl(searchSettings, OUTPUT_JSON, "BAR");
            let myCallback = {
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
                    let debugMouse = false;
                    let xAxis = _this.jq(ID_AXIS_BOTTOM);
                    let yAxis = _this.jq(ID_AXIS_LEFT);
                    let mousedown = function(evt) {
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
                    let mouseleave = function(evt) {
                        if (debugMouse)
                            console.log("mouse leave");
                        _this.drag = null;
                        _this.handledClick = false;
                    }
                    let mouseup = function(evt) {
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
                    let mousemove = function(evt, doX, doY) {
                        if (debugMouse)
                            console.log("mouse move");
                        let drag = _this.drag;
                        if (!drag) return;
                        drag.dragging = true;
                        let x = GuiUtils.getEventX(evt);
                        let deltaX = drag.x - x;
                        let y = GuiUtils.getEventY(evt);
                        let deltaY = drag.y - y;
                        let width = $(this).width();
                        let height = $(this).height();
                        let percentX = (x - drag.x) / width;
                        let percentY = (y - drag.y) / height;
                        let ascX = _this.getXAxisAscending();
                        let ascY = _this.getXAxisAscending();
                        let diffX = (drag.X.maxDate.getTime() - drag.X.minDate.getTime()) * percentX;
                        let diffY = (drag.Y.maxDate.getTime() - drag.Y.minDate.getTime()) * percentY;

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
                    let mouseclick = function(evt, doX, doY) {
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
                        let action;
                        if (evt.metaKey || evt.ctrlKey) {
                            action = "reset";
                        } else {
                            let zoomOut = evt.shiftKey;
                            if (zoomOut)
                                action = "zoomout";
                            else
                                action = "zoomin";
                        }
                        _this.doZoom(action, doX, doY);
                    };

                    let mousemoveCanvas = function(evt) {
                        mousemove(evt, true, true);
                    }
                    let mousemoveX = function(evt) {
                        mousemove(evt, true, false);
                    }
                    let mousemoveY = function(evt) {
                        mousemove(evt, false, true);
                    }

                    let mouseclickCanvas = function(evt) {
                        mouseclick(evt, true, true);
                    }
                    let mouseclickX = function(evt) {
                        mouseclick(evt, true, false);
                    }
                    let mouseclickY = function(evt) {
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

                    let links =
                        HU.image(icon_zoom, ["class", "display-grid-action", "title", "reset zoom", "action", "reset"]) +
                        HU.image(icon_zoom_in, ["class", "display-grid-action", "title", "zoom in", "action", "zoomin"]) +
                        HU.image(icon_zoom_out, ["class", "display-grid-action", "title", "zoom out", "action", "zoomout"]);
                    _this.jq(ID_LINKS).html(links);
                    $("#" + _this.getDomId(ID_GRID) + " .display-grid-action").click(function() {
                        let action = $(this).attr("action");
                        _this.doZoom(action);
                    });


                    _this.jq(ID_AXIS_LEFT).html("");
                    _this.jq(ID_AXIS_BOTTOM).html("");
                    _this.makeGrid(_this.entries);
                }
            };
            let entryList = new EntryList(this.getRamadda(), jsonUrl, myCallback, true);
        },
        initDialog: function() {
            SUPER.initDialog.call(this);
            let _this = this;
            let cbx = this.jq(ID_SETTINGS + " :checkbox");
            cbx.click(function() {
                _this.setProperty($(this).attr("attr"), $(this).is(':checked'));
                _this.makeGrid(_this.entries);
            });
            let input = this.jq(ID_SETTINGS + " :input");
            input.blur(function() {
                _this.setProperty($(this).attr("attr"), $(this).val());
                _this.makeGrid(_this.entries);
            });
            input.keypress(function(event) {
                let keycode = (event.keyCode ? event.keyCode : event.which);
                if (keycode == 13) {
                    _this.setProperty($(this).attr("attr"), $(this).val());
                    _this.makeGrid(_this.entries);
                }
            });

        },
        getDialogContents: function(tabTitles, tabContents) {
            let height = "600";
            let html = "";
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
                let zoomOut = (action == "zoomout");
                if (doX) {
                    let d1 = this.axis.X.minDate.getTime();
                    let d2 = this.axis.X.maxDate.getTime();
                    let dateRange = d2 - d1;
                    let diff = (zoomOut ? 1 : -1) * dateRange * 0.1;
                    this.axis.X.minDate = new Date(d1 - diff);
                    this.axis.X.maxDate = new Date(d2 + diff);
                }
                if (doY) {
                    let d1 = this.axis.Y.minDate.getTime();
                    let d2 = this.axis.Y.maxDate.getTime();
                    let dateRange = d2 - d1;
                    let diff = (zoomOut ? 1 : -1) * dateRange * 0.1;
                    this.axis.Y.minDate = new Date(d1 - diff);
                    this.axis.Y.maxDate = new Date(d2 + diff);
                }
            }
            this.makeGrid(this.entries);
        },
        initGrid: function(entries) {
            let _this = this;
            let items = this.canvas.find(".display-grid-entry");
            items.click(function(evt) {
                let index = parseInt($(this).attr("index"));
                entry = entries[index];
                let url = entry.getEntryUrl();
                if (_this.urlTemplate) {
                    url = _this.urlTemplate.replace("{url}", url).replace(/{entryid}/g, entry.getId()).replace(/{resource}/g, entry.getResourceUrl());
                }

                _this.handledClick = true;
                _this.drag = null;
                window.open(url, "_entry");
                //                        evt.stopPropagation();
            });
            items.mouseout(function() {
                let id = $(this).attr("entryid");
                if (id) {
                    let other = _this.canvas.find("[entryid='" + id + "']");
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
                let id = $(this).attr("entryid");
                if (id) {
                    let other = _this.canvas.find("[entryid='" + id + "']");
                    other.each(function() {
                        if ($(this).attr("itemtype") == "box") {
                            $(this).attr("prevcolor", $(this).css("background"));
                            $(this).css("background", "rgba(0,0,255,0.5)");
                        }
                    });
                }
                let x = GuiUtils.getEventX(evt);
                let index = parseInt($(this).attr("index"));
                entry = entries[index];
                let thumb = entry.getThumbnail();
                let html = "";
                if (thumb) {
                    html = HU.image(thumb, ["width", "300;"]) + "<br>";
                } else if (entry.isImage()) {
                    html += HU.image(entry.getResourceUrl(), ["width", "300"]) + "<br>";
                }
                html += entry.getIconImage() + " " + entry.getName() + "<br>";
                let start = entry.getStartDate().getUTCFullYear() + "-" + Utils.padLeft(entry.getStartDate().getUTCMonth() + 1, 2, "0") + "-" + Utils.padLeft(entry.getStartDate().getUTCDate(), 2, "0");
                let end = entry.getEndDate().getUTCFullYear() + "-" + Utils.padLeft(entry.getEndDate().getUTCMonth() + 1, 2, "0") + "-" + Utils.padLeft(entry.getEndDate().getUTCDate(), 2, "0");
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
            let html = "";
            let mouseInfo = "click:zoom in;shift-click:zoom out;command/ctrl click: reset";
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
            let showIcon = this.getProperty(ID_SHOW_ICON, true);
            let showName = this.getProperty(ID_SHOW_NAME, true);

            if (!this.minDate) {
                let minDate = null;
                let maxDate = null;
                for (let i = 0; i < entries.length; i++) {
                    let entry = entries[i];
                    minDate = minDate == null ? entry.getStartDate() : (minDate.getTime() > entry.getStartDate().getTime() ? entry.getStartDate() : minDate);
                    maxDate = maxDate == null ? entry.getEndDate() : (maxDate.getTime() < entry.getEndDate().getTime() ? entry.getEndDate() : maxDate);
                }
                this.minDate = new Date(Date.UTC(minDate.getUTCFullYear(), 0, 1));
                this.maxDate = new Date(Date.UTC(maxDate.getUTCFullYear() + 1, 0, 1));
            }

            let axis = {
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
            for (let i = 0; i < axis.Y.ticks.length; i++) {
                let tick = axis.Y.ticks[i];
//                let style = (axis.Y.ascending ? "bottom:" : "top:") + tick.percent + "%;";
                let style = "bottom:" + tick.percent + "%;";
                let lineClass = tick.major ? "display-grid-hline-major" : "display-grid-hline";
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
            for (let i = 0; i < axis.X.ticks.length; i++) {
                let tick = axis.X.ticks[i];
                if (tick.percent > 0) {
                    let lineClass = tick.major ? "display-grid-vline-major" : "display-grid-vline";
                    axis.X.lines += HU.div(["style", "left:" + tick.percent + "%;", "class", lineClass], " ");
                }
                axis.X.html += HU.div(["style", "left:" + tick.percent + "%;", "class", "display-grid-axis-bottom-tick"], HU.div(["class", "display-grid-vtick"], "") + " " + tick.label);
            }

            let items = "";
            let seen = {};
            for (let i = 0; i < entries.length; i++) {
                let entry = entries[i];
                let vInfo = this[axis.Y.calculatePercent].call(this, entry, axis.Y);
                let xInfo = this[axis.X.calculatePercent].call(this, entry, axis.X);
                if (vInfo.p1 < 0) {
                    vInfo.p2 = vInfo.p2 + vInfo.p1;
                    vInfo.p1 = 0;
                }
                if (vInfo.p1 + vInfo.p2 > 100) {
                    vInfo.p2 = 100 - vInfo.p1;
                }

                let style = "";
                let pos = "";

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


                let namePos = pos;
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
                let key = Math.round(xInfo.p1) + "---" + Math.round(vInfo.p1);
                if (showName && !seen[key]) {
                    seen[key] = true;
                    let name = entry.getName().replace(/ /g, "&nbsp;");
                    items += HU.div(["class", "display-grid-entry-text display-grid-entry", "entryid", entry.getId(), "index", i, "style", namePos], name);
                }
                let boxStyle = style + "background:" + this.getProperty(ID_COLOR, "lightblue");
                items += HU.div(["class", "display-grid-entry-box display-grid-entry", "itemtype", "box", "entryid", entry.getId(), "style", boxStyle, "index", i], "");
            }
            this.jq(ID_AXIS_LEFT).html(axis.Y.html);
            this.jq(ID_CANVAS).html(axis.Y.lines + axis.X.lines + items);
            this.jq(ID_AXIS_BOTTOM).html(axis.X.html);
            this.initGrid(entries);
        },
        calculateSizeAxis: function(axisInfo) {
            let min = Number.MAX_VALUE;
            let max = Number.MIN_VALUE;
            for (let i = 0; i < this.entries.length; i++) {
                let entry = this.entries[i];
                min = Math.min(min, entry.getSize());
                max = Math.max(max, entry.getSize());
            }
        },
        checkOrder: function(axisInfo, percents) {
            /*
            if(!axisInfo.ascending) {
                percents.p1 = 100-percents.p1;
                percents.p2 = 100-percents.p2;
                let tmp  =percents.p1;
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
            let p1 = 100 * (entry.getStartDate().getTime() - axisInfo.min) / axisInfo.range;
            let p2 = 100 * (entry.getEndDate().getTime() - axisInfo.min) / axisInfo.range;
            return this.checkOrder(axisInfo, {
                p1: p1,
                p2: p2,
                delta: Math.abs(p2 - p1)
            });
        },
        calculateMonthPercent: function(entry, axisInfo) {
            let d1 = entry.getStartDate();
            let d2 = entry.getEndDate();
            let t1 = new Date(Date.UTC(1, d1.getUTCMonth(), d1.getUTCDate()));
            let t2 = new Date(Date.UTC(1, d2.getUTCMonth(), d2.getUTCDate()));
            let p1 = 100 * ((t1.getTime() - axisInfo.min) / axisInfo.range);
            let p2 = 100 * ((t2.getTime() - axisInfo.min) / axisInfo.range);
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
            let months = Utils.getMonthShortNames();
            for (let month = 0; month < months.length; month++) {
                let t1 = new Date(Date.UTC(1, month));
                let percent = (axisInfo.maxDate.getTime() - t1.getTime()) / axisInfo.range;
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
            let numYears = axisInfo.maxDate.getUTCFullYear() - axisInfo.minDate.getUTCFullYear();
            let years = numYears;
            axisInfo.type = "year";
            axisInfo.skip = Math.max(1, Math.floor(numYears / axisInfo.maxTicks));
            if ((numYears / axisInfo.skip) <= (axisInfo.maxTicks / 2)) {
                let numMonths = 0;
                let tmp = new Date(axisInfo.minDate.getTime());
                while (tmp.getTime() < axisInfo.maxDate.getTime()) {
                    Utils.incrementMonth(tmp);
                    numMonths++;
                }
                axisInfo.skip = Math.max(1, Math.floor(numMonths / axisInfo.maxTicks));
                axisInfo.type = "month";
                if ((numMonths / axisInfo.skip) <= (axisInfo.maxTicks / 2)) {
                    let tmp = new Date(axisInfo.minDate.getTime());
                    let numDays = 0;
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
            let months = Utils.getMonthShortNames();
            let lastYear = null;
            let lastMonth = null;
            let tickDate;
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
                let percent = (tickDate.getTime() - axisInfo.minDate.getTime()) / axisInfo.range;
                if (!axisInfo.ascending)
                    percent = (1 - percent);
                percent = 100 * percent;
                //                    console.log("    perc:"+ percent +" " + Utils.formatDateYYYYMMDD(tickDate));
                if (percent >= 0 && percent < 100) {
                    let label = "";
                    let year = tickDate.getUTCFullYear();
                    let month = tickDate.getUTCMonth();
                    let major = false;
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
    let SUPER;
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
            let entries = this.entryList.getEntries();
            if (entries.length == 0) {
                this.writeMessage("Nothing found");
                return;
            }
	    let html = this.getEntriesMetadata(entries);
            this.writeEntries(html, entries);
            HU.formatTable("#" + this.getDomId("table"), {
                scrollY: 400
            });
        },
    });

}



function RamaddaEntrydisplayDisplay(displayManager, id, properties) {
    let SUPER;
    $.extend(this, {
        sourceEntry: properties.sourceEntry
    });
    RamaddaUtil.inherit(this, SUPER = new RamaddaDisplay(displayManager, id, DISPLAY_ENTRYDISPLAY, properties));
    if (properties.sourceEntry == null && properties.entryId != null) {
        let _this = this;
        let f = async function() {
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
            let title = this.title;
            if (this.sourceEntry != null) {
                this.addEntryHtml(this.sourceEntry);
                let url = this.sourceEntry.getEntryUrl();

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
            let selected = args.selected;
            let entry = args.entry;
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
            let html = this.getEntryHtml(entry, {
                showHeader: false
            });
            let height = this.getProperty("height", "400px");
            if (!height.endsWith("px")) height += "px";
            this.setContents(HU.div(["class", "display-entry-description", "style", "height:" + height + ";"],
                html));
            this.entryHtmlHasBeenDisplayed(entry);
        },
    });
}



function RamaddaEntrytitleDisplay(displayManager, id, properties) {
    let SUPER;
    $.extend(this, {
        sourceEntry: properties.sourceEntry
    });
    RamaddaUtil.inherit(this, SUPER = new RamaddaDisplay(displayManager, id, DISPLAY_ENTRYDISPLAY, properties));
    if (properties.sourceEntry == null && properties.entryId != null) {
        let _this = this;
        let f = async function() {
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
		html = this.getProperty("template","<b>${icon} ${name} Date: ${date}</b>");
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


function RamaddaEntrywikiDisplay(displayManager, id, properties) {
    const ID_WIKI = "wiki";
    let SUPER;
    if(!properties.displayStyle)
	properties.displayStyle="width:100%;"
    $.extend(this, {
        sourceEntry: properties.sourceEntry
    });
    RamaddaUtil.inherit(this, SUPER = new RamaddaDisplay(displayManager, id, DISPLAY_ENTRYDISPLAY, properties));

    if (properties.sourceEntry == null && properties.entryId != null) {
        let _this = this;
        let f = async function() {
            await _this.getEntry(properties.entryId, entry => {
                _this.sourceEntry = entry;
                _this.initDisplay()
            });
        }
        f();
    }

    let myProps = [
	{label:'Entry Wiki'},
	{p:'wiki',d:'{{import macro=forchild}}',ex:'wiki text'},
	{p:'wikiStyle',d:'width:100%;max-width:95vw'}
    ];

    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
	initDisplay: function() {
            this.createUI();
	    let html = HU.div(['id',this.domId(ID_WIKI),'style',this.getWikiStyle()]);
	    this.displayHtml(html);
	    if(this.sourceEntry) {
		let e = this.sourceEntry;
		let wiki = this.getWiki();
		wiki = wiki.replace(/\\n/g,"\n");
		//Delete the old displays
		if(this.addedDisplays) {
		    this.addedDisplays.forEach(display=>{
			if(display.getId)
			    removeRamaddaDisplay(display.getId());
		    });
		}
		this.addedDisplays = [];
		this.wikify(wiki,e.getId(),html=>{
		    addDisplayListener = display=>{
			this.addedDisplays.push(display);
//			console.log("add display:" + display.type);
		    };
		    this.jq(ID_WIKI).html(html);
		    addDisplayListener = null;
		});
	    }
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
    const ID_SELECT = TAG_SELECT;
    const ID_SELECT1 = "select1";
    const ID_SELECT2 = "select2";
    const ID_NEWDISPLAY = "newdisplay";

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
            let html = "";
            html += HU.div([ATTR_ID, this.domId(ID_ENTRIES), ATTR_CLASS, this.getClass("entries")], "");
            this.setContents(html);
        },
        entryListChanged: function(entryList) {
            let html = "<form>";
            html += "<p>";
            html += HU.openTag(TAG_TABLE, [ATTR_CLASS, "formtable", "cellspacing", "0", "cellspacing", "0"]);
            let entries = this.entryList.getEntries();
            let get = this.getGet();

            for (let j = 1; j <= 2; j++) {
                let select = HU.openTag(TAG_SELECT, [ATTR_ID, this.getDomId(ID_SELECT + j)]);
                select += HU.tag(TAG_OPTION, [ATTR_TITLE, "", ATTR_VALUE, ""],
                    "-- Select --");
                for (let i = 0; i < entries.length; i++) {
                    let entry = entries[i];
                    let label = entry.getIconImage() + " " + entry.getName();
                    select += HU.tag(TAG_OPTION, [ATTR_TITLE, entry.getName(), ATTR_VALUE, entry.getId()],
                        entry.getName());

                }
                select += HU.closeTag(TAG_SELECT);
                html += HU.formEntry("Data:", select);
            }

            let select = HU.openTag(TAG_SELECT, [ATTR_ID, this.getDomId(ID_CHARTTYPE)]);
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
            let theDisplay = this;
            this.jq(ID_NEWDISPLAY).button().click(function(event) {
                theDisplay.createDisplay();
            });
        },
        createDisplay: function() {
            let entry1 = this.getEntry(this.jq(ID_SELECT1).val());
            let entry2 = this.getEntry(this.jq(ID_SELECT2).val());
            if (entry1 == null) {
                alert("No data selected");
                return;
            }
            let pointDataList = [];

            pointDataList.push(new PointData(entry1.getName(), null, null, ramaddaBaseUrl + "/entry/show?&output=points.product&product=points.json&numpoints=1000&entryid=" + entry1.getId()));
            if (entry2 != null) { 
               pointDataList.push(new PointData(entry2.getName(), null, null, ramaddaBaseUrl + "/entry/show?&output=points.product&product=points.json&numpoints=1000&entryid=" + entry2.getId()));
            }

            //Make up some functions
            let operation = "average";
            let derivedData = new DerivedPointData(this.displayManager, "Derived Data", pointDataList, operation);
            let pointData = derivedData;
            let chartType = this.jq(ID_CHARTTYPE).val();
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
            let theDisplay = this;
            this.createUI();
            let html = "";
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
            for (let i = 0; i < this.ramaddas.length; i++) {
                if (i == 0) {}
                let ramadda = this.ramaddas[i];
                let types = ramadda.getEntryTypes(function(ramadda, types) {
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
            let typeMap = {};
            let allTypes = [];
            let html = "";
            html += HU.openTag(TAG_TABLE, [ATTR_CLASS, "display-repositories-table", ATTR_WIDTH, "100%", ATTR_BORDER, "1", "cellspacing", "0", "cellpadding", "5"]);
            for (let i = 0; i < this.ramaddas.length; i++) {
                let ramadda = this.ramaddas[i];
                let types = ramadda.getEntryTypes();
                for (let typeIdx = 0; typeIdx < types.length; typeIdx++) {
                    let type = types[typeIdx];
                    if (typeMap[type.getId()] == null) {
                        typeMap[type.getId()] = type;
                        allTypes.push(type);
                    }
                }
            }

            html += HU.openTag(TAG_TR, ["valign", "bottom"]);
            html += HU.th([ATTR_CLASS, "display-repositories-table-header"], "Type");
            for (let i = 0; i < this.ramaddas.length; i++) {
                let ramadda = this.ramaddas[i];
                let link = HU.href(ramadda.getRoot(), ramadda.getName());
                html += HU.th([ATTR_CLASS, "display-repositories-table-header"], link);
            }
            html += "</tr>";

            let onlyCats = [];
            if (this.categories != null) {
                onlyCats = this.categories.split(",");
            }



            let catMap = {};
            let cats = [];
            for (let typeIdx = 0; typeIdx < allTypes.length; typeIdx++) {
                let type = allTypes[typeIdx];
                let row = "";
                row += "<tr>";
                row += HU.td([], HU.image(type.getIcon()) + " " + type.getLabel());
                for (let i = 0; i < this.ramaddas.length; i++) {
                    let ramadda = this.ramaddas[i];
                    let repoType = ramadda.getEntryType(type.getId());
                    let col = "";
                    if (repoType == null) {
                        row += HU.td([ATTR_CLASS, "display-repositories-table-type-hasnot"], "");
                    } else {
                        let label =
                            HU.tag(TAG_A, ["href", ramadda.getRoot() + "/search/type/" + repoType.getId(), "target", "_blank"],
                                repoType.getEntryCount());
                        row += HU.td([ATTR_ALIGN, "right", ATTR_CLASS, "display-repositories-table-type-has"], label);
                    }

                }
                row += "</tr>";

                let catRows = catMap[type.getCategory()];
                if (catRows == null) {
                    catRows = [];
                    catMap[type.getCategory()] = catRows;
                    cats.push(type.getCategory());
                }
                catRows.push(row);
            }

            for (let i = 0; i < cats.length; i++) {
                let cat = cats[i];
                if (onlyCats.length > 0) {
                    let ok = false;
                    for (let patternIdx = 0; patternIdx < onlyCats.length; patternIdx++) {
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
                let rows = catMap[cat];
                html += "<tr>";
                html += HU.th(["colspan", "" + (1 + this.ramaddas.length)], cat);
                html += "</tr>";
                for (let row = 0; row < rows.length; row++) {
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


function DisplayEntryMetadata(display,metadataType,metadata) {
    this.display=display;
    this.metadata = metadata;
    this.metadataType = metadataType;
    if(metadata.elements) 
	this.elements= metadata.elements.map(element=>{return new DisplayEntryMetadataElement(display,this,element);});
    $.extend(this,{
	getType:function() {
	    return this.metadataType.getType();
	},
	getLabel:function() {
	    return this.metadataType.getLabel();
	},	
	getElements:function() {
	    return this.elements;
	}
    });
}

//metadataelement
function DisplayEntryMetadataElement(display,metadata,element) {
    this.display=display;
    this.metadata=metadata;
    this.element = element;
    $.extend(this,{
	getMetadataType:function() {
	    return this.metadata.getType();
	},

	getType:function() {
	    return this.element.type;
	},
	getName:function() {
	    return this.element.name;
	},	
	getIndex:function() {
	    return this.element.index;
	},
	getValues:function() {
	    return this.element.values;
	},
	getInputText:function() {
	    return jqid(this.inputId).val();
	},
	makeInput:function() {
	    this.inputId = this.display.getMetadataFieldId(this.metadata.getType())+'_element_' + this.getIndex()+'_input';
	    let input = HU.input('','',[ATTR_CLASS,
					'display-simplesearch-input',
					ATTR_ID,this.inputId,ATTR_STYLE,HU.css('width','100%'),ATTR_PLACEHOLDER,this.getName()]);
	    return input;
	},
	cbxState:{},
	setCbxOn:function(v) {
	    this.cbxState[v] = true;
	},
	setCbxOff:function(v) {
	    this.cbxState[v] = false;
	},	
	addSearchSettings:function(settings) {
	    let text;
	    Object.keys(this.cbxState).forEach(value=>{
		if(!this.cbxState[value]) return;
		settings.metadata.push({
		    type: this.getMetadataType(),
		    index:this.getIndex(),
		    value: value
		});
	    });
	    if(this.selectId && jqid(this.selectId).length) {
		text=jqid(this.selectId).val();
	    } else if(this.getType()=='string') {
		text = this.getInputText();
	    }
	    if(Utils.stringDefined(text)) {
		settings.metadata.push({
		    type: this.getMetadataType(),
		    index:this.getIndex(),
		    value: text
		});
	    }
	},

	makeCheckboxes:function(idToElementMap) {
	    let cbxs=[];
	    this.selectId = this.display.getMetadataFieldId(this.metadata.getType())+"_select_" + this.getIndex();
            let select = HU.tag(TAG_OPTION, [ATTR_TITLE, "", ATTR_VALUE, ""]);
	    let popupLimit = this.display.getTagPopupLimit();
	    this.getValues().forEach((v,i)=>{
                let count = v.count;
                let value = v.value;
                let label = v.label;
		let type =this.metadata.getType();
                let optionAttrs = [ATTR_VALUE, value, ATTR_CLASS, "display-metadatalist-item"];
                let selected = this.cbxState[value];
                if (selected) {
		    optionAttrs.push("selected");
		    optionAttrs.push(null);
                }
                select += HU.tag(TAG_OPTION, optionAttrs, label + " (" + count + ")");
		let cbxId = this.display.getMetadataFieldId(this.metadata.getType())+"_checkbox_" + this.getIndex()+"_"+i;
		if(idToElementMap) idToElementMap[cbxId] = this;
		let cbx = HU.checkbox("",[ATTR_ID,cbxId,
					  "metadata-type",type,
					  "metadata-index",this.getIndex(),
					  "metadata-value",value],selected) +" " + HU.tag( "label",  [CLASS,"ramadda-noselect ramadda-clickable","for",cbxId],label +" (" + count+")");
		if(this.getValues().length>popupLimit) {
		    cbx = HU.span([ATTR_CLASS,'display-search-tag','tag',label], cbx);
		}
		cbxs.push(cbx);
	    });
	    this.select = HU.tag("select", [ATTR_ID,this.selectId],select);
	    return cbxs;

	}
    });
}

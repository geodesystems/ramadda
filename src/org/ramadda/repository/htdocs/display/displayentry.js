/**
   Copyright 2008-2025 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/


var DISPLAY_ENTRYLIST = "entrylist";
var DISPLAY_SEARCH = "search";
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
var ID_SEARCH_FOOTER = ID_FOOTER;
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
var ID_SEARCH_DATE_CHANGE = "search_changedate";
var ID_SEARCH_TAGS = "search_tags";
var ID_SEARCH_ANCESTOR = "search_ancestor";

var ID_SEARCH_ANCESTORS = "search_ancestors";
var ID_SEARCH_ANCESTORS_MENU = "search_ancestors_menu";
var ID_TREE_LINK = "treelink";

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

var CLASS_SEARCH_TAG = 'display-search-tag';


addGlobalDisplayType({
    type: DISPLAY_SIMPLESEARCH,
    label: "Simple Search",
    requiresData: false,
    category: CATEGORY_ENTRIES,
    desc:"Show a search field for entry or in page search"
});



addGlobalDisplayType({
    type: DISPLAY_SEARCH,
    label: "Entry Search",
    requiresData: false,
    category: CATEGORY_ENTRIES,
    help:'/userguide/wiki/wikisearch.html',
    desc:'Show the full search form'
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
    this.entryProps = [
	{label:'Remote Entry Search'},
        {p:'showProviders',ex:true,d: false},
	{p:'providers',ex:'this,category:.*',tt:'List of search providers',canCache:true},
	{p:'providersMultiple',ex:'true',tt:'Support selecting multiple providers'},
	{p:'providersMultipleSize',d:'4'},
	{p:'searchDirect',d:false,tt:'Directly search remote RAMADDA repositories'},	
    ];

    this.defineProperties(this.entryProps);

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
                        tmp[m.type] = '';
                        mdtsFromEntries.push(m.type);
                    }
                    mdtmap[metadata[j].type] = metadata[j].label;
                }
            }

            let html = '';
            html += HU.openTag(TAG_TABLE, [ATTR_ID, this.getDomId(TAG_TABLE),
					   ATTR_CLASS, 'cell-border stripe ramadda-table',
					   ATTR_WIDTH, HU.perc(100),
					   ATTR_CELLPADDING, 5,
					   ATTR_CELLSPACING, 0]);
            html += HU.open(TAG_THEAD);
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
            let headerRow = HU.tr([ATTR_VALIGN, POS_BOTTOM], HU.join(headerItems, ""));
            html += headerRow;
            html += HU.close(TAG_THEAD)+HU.open(TAG_TBODY);
            let divider = HU.div([ATTR_CLASS,'display-metadata-divider']);
            let missing = this.missingMessage;
            if (missing = null) missing = SPACE;
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
                                let url = HU.url(this.getRamadda().getRoot() + "/metadata/view/" + m.value.attr1,
						 ['element','1','entryid',entry.getId(),'metadata_id',m.id]);
                                item = HU.image(url, [ATTR_WIDTH, 100]);
                            } else if (m.type == "content.url" || m.type == "dif.related_url") {
                                let label = m.value.attr2;
                                if (label == null || label == "") {
                                    label = m.value.attr1;
                                }
                                item = HU.href(m.value.attr1, label);
                            } else if (m.type == "content.attachment") {
                                let toks = m.value.attr1.split("_file_");
                                let filename = toks[1];
                                let url = HU.url(this.getRamadda().getRoot() + "/metadata/view/" + m.value.attr1,
						 ['element','1','entryid',entry.getId(), 'metadata_id',m.id]);
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
                    let add = HU.tag(TAG_A, [ATTR_STYLE, HU.css(CSS_COLOR,COLOR_BLACK),
					     ATTR_HREF,
					     HU.url(this.getRamadda().getRoot() + "/metadata/addform",
						    ['entryid',entry.getId(),'metadata_type',mdt]),
					     ATTR_TARGET, "_blank",
					     ATTR_TITLE, "Add metadata"
					    ], "+");
                    add = HU.div([ATTR_CLASS, "display-metadata-table-add"], add);
                    let cellContents = add + divider;
                    if (cell.length > 0) {
                        cellContents += cell;
                    }
                    row.push(HU.td([], HU.div([ATTR_CLASS, "display-metadata-table-cell-contents"], cellContents)));
                }
                html += HU.tr([ATTR_VALIGN, POS_TOP], HU.join(row, ""));
                //Add in the header every 10 rows
                if (((entryIdx + 1) % 10) == 0) html += headerRow;
            }
            html += HU.close(TAG_TBODY,TAG_TABLE);
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

                if (entry.getIsImage()) {
                    imageEntries.push(entry);
                    let link = HU.tag(TAG_A, [ATTR_HREF, entry.getEntryUrl()], entry.getName());
                    imageCnt++;
		    let imageUrl =entry.getImageUrl();
                    html += HU.tag(TAG_IMG, [ATTR_SRC, imageUrl,
					     ATTR_WIDTH, "500",
					     ATTR_ID,  this.getDomId("entry_" + entry.getIdForDom()),
					     ATTR_ENTRYID, entry.getId(), ATTR_CLASS, "display-entrygallery-entry"
					    ]) + HU.br() +
                        link + HU.p();
                } else {
                    let icon = entry.getIconImage([ATTR_TITLE, "View entry"]);
                    let link = HU.tag(TAG_A, [ATTR_HREF, entry.getEntryUrl()], icon + " " + entry.getName());
                    nonImageHtml += link + HU.br();
                }
            }

            if (imageCnt > 1) {
                //Show a  gallery instead
		this.galleryId = HU.getUniqueId("gallery_");
                let newHtml = HU.open(TAG_DIV,[ATTR_ID, this.galleryId,ATTR_CLASS,"ramadda-grid"]);
		let itemWidth = this.getProperty("galleryItemWidth",HU.px(200));
                for (let i = 0; i < imageEntries.length; i++) {
                    let entry = imageEntries[i];
		    let attrs = [CSS_WIDTH,itemWidth];
                    newHtml += HU.open(TAG_DIV,[ATTR_CLASS,"display-entrygallery-item",
						ATTR_STYLE,HU.css(attrs)]);
                    let link = HU.tag(TAG_A, [ATTR_TARGET,"_entries",
					      ATTR_HREF, entry.getEntryUrl()], entry.getName());
		    link = link.replace(/"/g,"'");
		    let imageUrl =entry.getImageUrl();
                    let img = HU.image(imageUrl, [ATTR_LOADING,"lazy", ATTR_WIDTH, HU.perc(100),
						  ATTR_ID, this.getDomId("entry_" + entry.getIdForDom()),
						  ATTR_ENTRYID, entry.getId(), ATTR_CLASS, "display-entrygallery-entry"
						 ]);
                    img = HU.href(entry.getResourceUrl(), img, ["data-fancybox",this.galleryId, "data-caption",link,
								ATTR_CLASS, "popup_image"]);
                    newHtml += HU.div([ATTR_CLASS, "image-outer"], HU.div([ATTR_CLASS, "image-inner"], img) +
				      HU.div([ATTR_CLASS, "image-caption"], link));

                    newHtml += HU.close(TAG_DIV);
                }
                newHtml += HU.close(TAG_DIV);
                html = newHtml;
            }


            //append the links to the non image entries
            if (nonImageHtml != "") {
                if (imageCnt > 0) {
                    html += "<hr>";
                }
                html += HU.div([ATTR_STYLE,HU.css(CSS_MARGIN,HU.px(10))],nonImageHtml);
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
    let myProps = this.myProps =  [
	{label:'Search Entry Types'},
        {p:'entryTypes',ex:'comma separated list of type IDs',
	 tt:'Entry types to search for'},
        {p:'showType',d: true,tt:'Show the entry type selector'},
	{p:'excludeTypes',ex:'type1,type2',tt:'Exclude these types'},	
	{p:'excludeEmptyTypes',ex:'true',tt:'Some types in the type list might have zero entry count'},
        {p:'typesLabel',tt: 'Label to use for the type section'},		
	{p:'addAllTypes',d:false,ex:'true',tt:'Add the All types to the type list'},
	{p:'addAnyType',d:true,ex:'true',tt:'Add the Any of these types to the type list'},
	{p:'startWithAny',d:true,ex:'true',tt:'Start with the Any of these types'},	


	{label:'Search Context'},
	{p:'ancestor',ex:'entry ID or this',tt:'Constrain search to this entry'},		
        {p:'showAncestorSelector',d: true},
        {p:'ancestors',tt: 'Comma separated list of entry ids or type:entry_type'},
        {p:'ancestorsLabel',tt: 'Label to use for the ancestors section'},		
        {p:'mainAncestor',tt: 'Entry ID to force the search under'},	


	{label:'Search Settings'},
	{p:'orderByTypes',
	 tt:'Comma separated list to show in the order by menu',
	 ex:'relevant,name,createdate,date,changedate,size,entryorder',	 
	 d:'relevant,name,createdate,date,size,entryorder'},
        {p:'orderBy',ex: 'name_ascending|name_descending|fromdate_ascending|fromdate_descending|todate_|createdate_|size_',
	 tt:'Initial sort order'},
        {p:'showOrderBy',d:true,ex: 'true'},
        {p:'doSearch',d: true,tt:'Apply search at initial display'},
        {p:'showText',d: true},
        {p:'showName',d: true},
        {p:'showDescription',d: false},		
        {p:'showDate',d: true},
        {p:'showCreateDate',ex:'true',d: false},
        {p:'showChangeDate',ex:'true',d: false},		
        {p:'showArea',d: true},
	{p:'textRequired',d:false},
        {p:'searchText',d: '',tt:'Initial search text'},
	{p:'searchPrefix',ex:'name:, contents:, path:',tt:'Prefix to add to the search text'},
        {p:'showMetadata',d: false},
	{p:'metadataTypes',tt:'Comma separated list of metadata types to add to the search form',
	 ex:'enum_tag:Tag,content.keyword:Keyword,thredds.variable:Variable'},
	{p:'metadataDisplay',
	 ex:'archive_note:attr1=Arrangement:template=<b>{attr1}_colon_</b> {attr2}',
	 tt:'Add metadata in the toggle. e.g.: type1:template={attr1},type2:attr1=Value:template={attr1}_colon_ {attr2}'},
	{p:'mainMetadataDisplay'},


	{p:'tagPopupLimit',d: 10,tt:'When do we show the tag popup' },		
	{p:'showSearchLabels',d:true},

	{p:'inputSize',ex:'15',tt:'Text input size'},
	{p:'textInputSize',d:'20px',ex:'100%'},	
	{p:'startDateLabel'},
	{p:'createDateLabel'},
	{p:'changeDateLabel'},		
	{p:'areaLabel'},
	{p:'showColumns',
	 tt:'Comma separated list of entry columns to show'},



	{label:'Search Form Layout'},
	{p:'searchHeaderLabel',d: 'Search'},
	{p:'formHeight',d:'1000px'},
        {p:'formWidth',d: '225px'},
        {p:'entriesWidth',d: 0},
        {p:'entriesHeight',ex:'70vh'},	
	{p:'showFormToggle',d:true,tt:'Show the hamburger button that hides/shows the form'},
        {p:'formOpen',d: true,tt:'Should the form initially be shown'},	

	{p:'toggleClose',ex:true,tt:'Close the search widget groups'},
	{p:'typesToggleClose',ex:true},
	{p:'textToggleClose',ex:true},
	{p:'dateToggleClose',ex:true},		
	{p:'areaToggleClose',ex:true},
	{p:'columnsToggleClose',ex:true},		

	{p:'showHeader',d:true},
        {p:'showFooter',d: true,tt:'Show the footer below the output'},	
	{p:'showOutputs',ex:'false',d:true,tt:'Should the output buttons be shown'},
	{p:'resultButtons',ex:'default.ids,zip.tree,zip.export,extedit,copyurl',
	 tt:'The list of output buttons at the bottom of the form'},
	{p:'doWorkbench',d:false,ex:'true', tt:'Show the new, charts, etc links'},

	{label:'Search Results'},
	{p:'displayTypes',d:'list,display',ex:'"list,images,timeline,map,display,metadata"'},
	{p:'showEntryBreadcrumbs',ex:'false'},
	{p:'showSnippetInList',ex:'true'},

	{p:'nameStyle',tt:'Css style to apply to the entry name'},
	{p:'showIcon',tt:'Show the entry icon in the output'},
	{p:'showThumbnail'},
	{p:'placeholderImage',ex:'/repository/images/placeholder.png'},
	{p:'defaultImage',ex:'blank.gif',canCache:true},
	{p:'showEntryType',ex:'true',tt:'Show entry type in list'},
	{p:'showEntryImage',d:true,tt:'Show the entry thumbnail'},
        {p:'showDetailsForGroup',d: false},	

	/*
          {p:'orientation',ex:'horizontal|vertical',d:'horizontal'},
	*/

    ];

    const SUPER = new RamaddaEntryDisplay(displayManager, id, type, properties);
    this.currentTime = new Date();
    defineDisplay(this, SUPER, myProps, {
        metadataTypeList: [],
	haveSearched: false,
        haveTypes: false,
        metadata: {},
        metadataLoading: {},
	getWikiEditorTags: function() {
	    return  Utils.mergeLists(this.myProps,SUPER.entryProps);
	},

	addToDocumentUrl:function(key,value) {
	    //Don't do this right away
	    let now = new Date();
	    if(now.getTime()-this.currentTime.getTime()>1000) {
		HU.addToDocumentUrl(key,value);
	    }
	},
	ctor: function() {
	    let metadataTypesAttr = this.getMetadataTypes();
	    if (Utils.stringDefined(metadataTypesAttr)) {
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
		    if(label=='null') {
		    } else {
			this.metadataTypeList.push(new MetadataType(type, label, value));
		    }
		}
	    }
	},
        getLoadingMessage: function(msg) {
	    if(!msg) return "";
	    return msg;
	},
        isLayoutHorizontal: function() {
	    //Don't attempt the vertical layout
	    return true;
	    //	    return this.getOrientation()== "horizontal";
        },

	setFormVisible:function(visible) {
	    this.formShown = visible;
	    if(visible)
		this.jq(ID_SEARCH_FORM).show();
	    else
		this.jq(ID_SEARCH_FORM).hide();
	},
	initHtml: function() {
	    this.jq(ID_ANCESTOR).click((event) =>{
		let aid = this.domId(ID_ANCESTOR);
		let root = this.getRamadda().getRoot();
		RamaddaUtils.selectInitialClick(event,aid,aid,true,null,null,'',root);
	    });

	    this.setFormVisible(this.getFormOpen());
	    this.jq(ID_SEARCH_HIDEFORM).click(()=>{
		this.setFormVisible(!this.formShown);
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
		    if(suffix) suffix=Utils.delimMsg(suffix);
		    if(!dflt) dflt = Utils.delimMsg(Utils.makeLabel(type))+(suffix?(' - '+suffix):'');
		    return  dflt;
		}


		Utils.split(this.getOrderByTypes(),',',true,true).forEach(type=>{
		    if(type=='relevant')
			byList.push([getLabel(type,null),type]);
		    else if(type=='name')
			byList.push([getLabel(type,'ascending',"Name A-Z"), type+'_ascending'],
				    [getLabel(type,'descending',"Name Z-A"),type+'_descending']);
		    else if(type=='createdate')
			byList.push([Utils.delimMsg("Record create date")+" - "+ Utils.delimMsg("newest first"),"createdate_descending"],
				    [Utils.delimMsg("Record create date")+" - "+ Utils.delimMsg("oldest first"),"createdate_ascending"]);

		    else if(type=='changedate')
			byList.push([Utils.delimMsg("Record change date")+" - "+ Utils.delimMsg("newest first"),"changedate_descending"],
				    [Utils.delimMsg("Record change date")+" - "+ Utils.delimMsg("oldest first"),"changedate_ascending"]);

		    else if(type=='date')
			byList.push([getLabel(type,'descending',
					      Utils.delimMsg("From date")+ " - " + Utils.delimMsg("youngest first")),"fromdate_descending"],
				    [getLabel(type,'ascending',
					      Utils.delimMsg("From date")+ " - " + Utils.delimMsg("oldest first")),"fromdate_ascending"]);
		    else if(type=='size')
			byList.push([Utils.delimMsg("Size")+" - "+Utils.delimMsg("largest first"),"size_descending"],
				    [Utils.delimMsg("Size")+" - "+Utils.delimMsg("smallest first"),"size_ascending"]);
		    else if(type=='entryorder')
			byList.push([Utils.delimMsg("Entry order")+" - "+Utils.delimMsg("increasing"),"entryorder_ascending"],
				    [Utils.delimMsg("Entry order")+" - "+Utils.delimMsg("decreasing"),"entryorder_descending"]);		    
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
		let select = HU.tag(TAG_SELECT, [ATTR_ID, this.getDomId(ID_SEARCH_ORDERBY),
						 ATTR_CLASS, "display-search-orderby"], options);
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
	    icon = HU.getIconImage(icon, [], [ATTR_STYLE,HU.css(CSS_COLOR,COLOR_WHITE)])
	    let imageId= toggle.attr('data-image-id');
	    jqid(imageId).html(icon);
	},
	addToggle:function(label,widgetId,toggleClose) {
	    let toggleId = HU.getUniqueId('');
	    let imageId = toggleId+'_image';
	    let title = Utils.delimMsg('click') +': '+Utils.delimMsg('toggle') +'; '+
		Utils.delimMsg('shift-click')+': ' + Utils.delimMsg('toggle all');
	    label = HU.div([ATTR_CLASS,'display-search-label-toggle',
			    ATTR_ID,toggleId,
			    'data-widget-id',widgetId,
			    'data-image-id',imageId,
			    ATTR_TITLE,title],
			   HU.span([ATTR_ID,imageId],
				   HU.getIconImage(toggleClose?'fa-plus':'fa-minus', [],
						   [ATTR_STYLE,HU.css(CSS_COLOR,COLOR_WHITE)])) +' ' + HU.span([],label));
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
		addSimpleToggle:false,
		toggleClose:this.getToggleClose()
	    }
	    if(args) $.extend(opts,args);
	    if(Utils.isDefined(opts.toggleClose)) opts.toggleClose=Utils.getProperty(opts.toggleClose);
	    if(!Utils.stringDefined(widget)) return '';
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
		HU.div([ATTR_CLASS,"display-search-label"], label):'';
	    widget = HU.div([ATTR_CLASS,'display-search-widgets'],	widget);
	    if(opts.addSimpleToggle) {
		w  = HU.toggleBlock(HU.b(label),widget);
	    } else {
		w=w+HU.div([ATTR_ID,widgetId,
			    ATTR_STYLE,HU.css(CSS_WIDTH,HU.perc(100),
					      CSS_DISPLAY,opts.toggleClose?DISPLAY_NONE:DISPLAY_INLINE_BLOCK)],widget);
	    }
	    return HU.div([ATTR_CLASS,"display-search-block"], w);
	},
        getFooter: function() {
            return HU.div([ATTR_ID, this.getDomId(ID_FOOTER),
			   ATTR_CLASS, "display-footer display-search-footer"]);
        },
        getDefaultHtml: function() {
            let html = "";
            let horizontal = this.isLayoutHorizontal();
            let entriesDivAttrs = [ATTR_ID, this.getDomId(ID_ENTRIES),
				   ATTR_CLASS, this.getClass("content")];
            let innerHeight= this.getProperty("innerHeight", null);
            let entriesStyle = this.getProperty("entriesStyle", "");	    
	    let style = "";
            if (innerHeight == null) {
                innerHeight = this.getEntriesHeight();
            }
            if (innerHeight != null) {
                style = HU.css(CSS_MAX_HEIGHT, HU.getDimension(innerHeight),CSS_OVERFLOW_Y,OVERFLOW_AUTO);
            }
	    style+= entriesStyle;
            entriesDivAttrs.push(ATTR_STYLE,style);	    
	    let searchBar = HU.div([ATTR_CLASS,horizontal?"display-search-bar":"display-search-bar-vertical",
				    ATTR_ID, this.domId(ID_SEARCH_BAR)],"");
            let resultsDiv = "";
            if (this.getShowHeader()) {
                resultsDiv = HU.div([ATTR_CLASS, "display-entries-results",
				     ATTR_ID, this.getDomId(ID_RESULTS)], SPACE);
            }
	    resultsDiv = HU.leftRightTable(resultsDiv,HU.div([ATTR_CLASS,"display-search-header",
							      ATTR_ID,this.domId(ID_SEARCH_HEADER)]),null,null,{valign:POS_BOTTOM});
	    let toggle = "";
	    if(horizontal &&  this.getShowFormToggle()) {
		toggle = HU.div([ATTR_TITLE, "Toggle form",
				 ATTR_ID,this.domId(ID_SEARCH_HIDEFORM),
				 ATTR_CLASS,CLASS_CLICKABLE,
				 ATTR_STYLE,HU.css(CSS_POSITION,POSITION_ABSOLUTE,
						   CSS_LEFT,HU.px(0),
						   CSS_TOP,HU.px(0))],
				HU.getIconImage("fas fa-bars"));
	    }
            let entriesDiv = HU.div([ATTR_STYLE,HU.css(CSS_POSITION,POSITION_RELATIVE)],
				    toggle +
				    searchBar +
				    resultsDiv +
				    HU.div(entriesDivAttrs, this.getLoadingMessage()));

	    html += HU.open(TAG_TABLE,[ATTR_WIDTH,HU.perc(100),ATTR_BORDER,0]);
	    html+=HU.open(TAG_TR,[ATTR_VALIGN,POS_TOP]);
            let entriesAttrs = [ATTR_CLASS, "col-md-12"];
	    let form = HU.div([ATTR_CLASS,'display-entrylist-form',
			       ATTR_STYLE,HU.css(CSS_WIDTH,HU.getDimension(this.getFormWidth()),
						 CSS_MAX_WIDTH,HU.getDimension(this.getFormWidth()),
						 CSS_OVERFLOW_X,OVERFLOW_AUTO)],this.makeSearchForm());
	    html += HU.tag(TAG_TD, [ATTR_ID,this.getDomId(ID_SEARCH_FORM),ATTR_WIDTH,HU.perc(1)], form);
	    this.formShown  = true;
            if (this.getShowFooter()) {
                entriesDiv +=  this.getFooter();
            }
            html += HU.tag(TAG_TD,[], entriesDiv);		    
            html += HU.close(TAG_TR,TAG_TABLE);
            html += HU.openTag(TAG_DIV, [ATTR_CLASS, "row"]);
            html += HU.tag(TAG_DIV, [ATTR_CLASS, "col-md-6"], "");
            if (this.getShowFooter(true)) {
		//                html += HU.tag(TAG_DIV, [ATTR_CLASS, "col-md-6"], footer);
            }
            html += HU.close(TAG_DIV);
            html += HU.div([ATTR_CLASS, "display-entry-popup",
			    ATTR_ID, this.getDomId(ID_DETAILS)], SPACE);
            return html;
        },
        initDisplay: function() {
            let theDisplay = this;
            this.jq(ID_SEARCH).button().click(function(event) {
		theDisplay.submitSearchForm();
                event.preventDefault();
            });

            this.jq(ID_TEXT_FIELD).autocomplete({
                source: function(request, callback) {
                    //                            theDisplay.doQuickEntrySearch(request, callback);
                }
            });

	    //Don't selectbox the orderby
	    //	HU.initSelect(this.jq(ID_SEARCH_ORDERBY));
	    this.jq(ID_SEARCH_ORDERBY).change(()=>{	    
                this.submitSearchForm();
	    });
            HU.initSelect(this.jq(ID_REPOSITORY));
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
            let closeImage = HU.getIconImage(ICON_CLOSE, []);
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
	    return  HU.jsLink("",HU.getIconImage(ICON_CLOSE,[ATTR_ID,this.domId("close"),
							     ATTR_STYLE,HU.css(CSS_CURSOR,CURSOR_POINTER)]));
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
            let left =  (settings.skip + 1) + "-" + (settings.skip + Math.min(settings.getMax(), entries.length));
	    if(entries.length==0) left = SPACE3+SPACE3+SPACE3;
            let nextPrev = [];
            let lessMore = [];
            if (settings.skip > 0) {
                nextPrev.push(HU.onClick(this.getGet() + ".loadPrevUrl();",
					 HU.getIconImage("fa-arrow-left", [ATTR_TITLE, "Previous"]),
					 [ATTR_CLASS, "display-link"]));
            }
            let addMore = false;
            if (entries.length>0 &&(true || entries.length == settings.getMax())) {
                nextPrev.push(HU.onClick(this.getGet() + ".loadNextUrl();",
					 HU.getIconImage("fa-arrow-right", [ATTR_TITLE, "Next"]),
					 [ATTR_CLASS, "display-link"]));
                addMore = true;
            }

	    if(entries.length>0) {
		lessMore.push(HU.onClick(this.getGet() + ".loadLess();",
					 HU.getIconImage("fa-minus", [ATTR_TITLE, "View less"]), [ATTR_CLASS, "display-link"]));
		if (addMore) {
                    lessMore.push(HU.onClick(this.getGet() + ".loadMore();",
					     HU.getIconImage("fa-plus", [ATTR_TITLE, "View more"]), [ATTR_CLASS, "display-link"]));
		}
	    }
            let results = "";
            let spacer = SPACE3;
	    if(includeCloser)
		results = this.getCloser();
	    results += SPACE + left + spacer;
            results += 
                HU.join(nextPrev, SPACE) + spacer +
                HU.join(lessMore, SPACE);
            return results+HU.br();
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
            if (this.changedateRangeWidget) {
                this.changedateRangeWidget.setSearchSettings(settings);
            }	    	    
	    
            settings.metadata = [];
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
	    //Check for recursion
	    if(ramaddaDoingWiki>0) {
		return;
	    }
            if (this.fixedEntries) {
                return;
            }
            this.haveSearched = true;
	    let settings  =this.makeSearchSettings();
            if (this.getTextRequired() && !Utils.stringDefined(settings.text)) {
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
	    let showOutputs = this.getShowOutputs(true);
	    let settings = this.getSearchSettings();
	    let okOutputs = this.getResultButtons(this.getProperty('outputs'));
	    if(okOutputs) {
		okOutputs = Utils.split(okOutputs,',',true,true);
	    }
	    let check = output=>{
		if(!showOutputs) return false;
		let id = output.id??output;
		if(!okOutputs) return true;
		if(id==OUTPUT_CSV) id='csv';
		else if(id==OUTPUT_ZIP) id='zip';
		else if(id==OUTPUT_EXPORT) id='export';				
		return okOutputs.includes(id);
	    }
	    let outputs = this.getRamadda().getSearchLinks(settings,true,check);
	    let url= this.getRamadda().getSearchUrl(settings);
	    let copyId = HU.getUniqueId('copy');
	    let extra = [];
	    if(this.getProperty('searchOutputs')) {
		extra = Utils.mergeLists(extra,Utils.split(this.getProperty('searchOutputs'),',',true,true));
	    }
	    if(!Utils.isAnonymous()) {
		if(check('extedit'))
		    extra.push('repository.extedit;Extended Edit');
	    }


	    extra.forEach(tok=>{
		let tuple = Utils.split(tok,';');
		if(tuple.length<2)return;
		let id = tuple[0];
		let label = tuple[1];
                outputs.push(HU.span([ATTR_CLASS,HU.classes('ramadda-search-link',CLASS_CLICKABLE),
				      ATTR_TITLE,Utils.delimMsg('Click to download') +
				      '; '+Utils.delimMsg('Shift-click to copy URL'),
				      'custom-output','true',
				      'data-name',label,
				      'data-format',id,
				      ATTR_DATA_URL,
				      this.getRamadda().getSearchUrl(settings,id)],
				     label));
	    });



	    outputs = HU.join(outputs, HU.space(2));
	    if(check('copyurl'))
		outputs = outputs+ HU.space(2)+
		HU.span([ATTR_CLASS,HU.classes('ramadda-search-link',CLASS_CLICKABLE),
			 ATTR_ID,copyId,
			 'data-copy',url],
			HU.getIconImage("fas fa-clipboard"));
	    if(this.getShowOutputs()) {
		this.footerRight = outputs == null ? "" :  outputs;
	    } else {
		this.footerRight = '';
	    }
            this.writeHtml(ID_SEARCH_FOOTER, this.footerRight);
	    let _this = this;
	    this.jq(ID_SEARCH_FOOTER).find('.ramadda-search-link').button().click(function(event){
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
		return  $(HU.div([ATTR_TITLE,'Click to clear search',
				  ATTR_CLASS,CLASS_SEARCH_TAG,key,value],label)).appendTo(searchBar);
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
			    let value = $(this).attr(ATTR_DATA_VALUE);
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
		} else if(col.isDate()) {
		} else if(col.isEntry()) {		    
		    //this gets handled below with the date widgets
                    let value = this.jq(id+'_hidden').val();
		    if(Utils.stringDefined(value)) {
			extra += "&" + arg + "=" + encodeURIComponent(value);
			if(tag.length==0) {
			    tag = makeTag("column",col.getName(),value);
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
	    this.dateWidgets.forEach(widget=>{
		let col = widget.column;
		let arg = col.getSearchArg();
		let startEnd = widget.widget.getStartEnd();
		if(Utils.stringDefined(startEnd.start)) {
		    extra += "&" + arg +'_from'+ "=" + encodeURIComponent(startEnd.start);
		}
		if(Utils.stringDefined(startEnd.end)) {
		    extra += "&" + arg +'_to'+ "=" + encodeURIComponent(startEnd.end);
		}		
	    });

	    let settings = this.getSearchSettings();
	    let mtdTags = searchBar.find(HU.attrSelect("metadata"));
	    mtdTags.remove();
	    if(settings.metadata) {
		settings.metadata.forEach(mtd=>{
		    let tag = makeTag("metadata", mtd.value,mtd.label+'='+mtd.value);
		    tag.click(()=> {
			let id = this.getMetadataFieldId(mtd.type)+"_select_" + mtd.index;
			let select  =jqid(id);
			let option = select.find('option[value=\"' + mtd.value+'\"]');
			option.prop('selected',false);
			select.trigger('change');
		    });

		});
	    }
	    
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
	    let selectedAncestors  =this.jq(ID_SEARCH_ANCESTORS_MENU).val();
	    if(selectedAncestors) {
		selectedAncestors.forEach(id=>{
		    jsonUrl+='&ancestor='+ id;
		});
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
	    let options = [];
	    entries.forEach(entry=>{
		options.push([entry.getId(),entry.getName()]);
	    });
	    let html = HU.select('',[ATTR_MULTIPLE,'true',ATTR_SIZE,4,
				     ATTR_ID,this.domId(ID_SEARCH_ANCESTORS_MENU)],options);
	    this.jq(ID_SEARCH_ANCESTORS).html(HU.div([ATTR_CLASS,'display-search-ancestors'],html));
	    HU.makeSelectTagPopup(this.jq(ID_SEARCH_ANCESTORS_MENU),{
		after:false,
		single:false,
		hide:false,
		label:'Select'});


	    this.jq(ID_SEARCH_ANCESTORS_MENU).change(()=> {
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
	    let toggleClose = this.getToggleClose(!this.getProperty('searchOpen',true));
            let form = HU.openTag(TAG_FORM, [ATTR_ID, this.getDomId(ID_FORM), "action", "#"]);
            let buttonLabel = HU.getIconImage("fa-search", [ATTR_TITLE, "Search"]);
            let topItems = [];
	    buttonLabel = "Search";
            let searchButton = HU.div([ATTR_STYLE,HU.css(CSS_MARGIN_BOTTOM,HU.px(4),
							 CSS_MAX_WIDTH,HU.perc(80)),
				       ATTR_ID, this.getDomId(ID_SEARCH),
				       ATTR_CLASS, HU.classes(CLASS_BUTTON,'display-search-button',CLASS_CLICKABLE)], buttonLabel);
            let extra = "";
            let settings = this.getSearchSettings();

            let horizontal = this.isLayoutHorizontal();

            if (this.ramaddas.length > 0) {
                let repositoriesSelect = HU.openTag(TAG_SELECT,
						    [ATTR_ID, this.getDomId(ID_REPOSITORY),
						     ATTR_CLASS, "display-repositories-select"]);
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
			    attrs.push(ATTR_SIZE,this.getProvidersMultipleSize(size),
				       ATTR_MULTIPLE, "true");
			}
			let providersSelect = HU.tag(TAG_SELECT,attrs, options);
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
                    topItems.push(HU.div([ATTR_STYLE,HU.css(CSS_MARGIN_BOTTOM,HU.px(4)),
					  ATTR_ID, this.getDomId(ID_TYPE_DIV)],
					 HU.span([ATTR_CLASS, "display-loading"], "Loading types...")));
		} else {
		    extra+= HU.div([ATTR_STYLE,HU.css(CSS_MARGIN_BOTTOM,HU.px(4)),
				    ATTR_ID, this.getDomId(ID_TYPE_DIV)]);
		}
            }


	    let text  = this.getFormText();
	    if(!Utils.stringDefined(text)) 
		text = HU.getUrlArgument(ID_TEXT_FIELD);
	    let textInputClass = "display-simplesearch-input display-search-textinput"
	    let attrs  = [ATTR_PLACEHOLDER, this.getEgText("Search text"),
			  ATTR_TITLE,Utils.noMsg("e.g. name:, contents:,path:"),
			  ATTR_CLASS, textInputClass,
			  ATTR_ID, this.domId(ID_TEXT_FIELD)];
	    let inputAttrs =  [ATTR_CLASS, textInputClass];
	    if(this.getInputSize()) {
		inputAttrs.push(ATTR_SIZE);
		inputAttrs.push(this.getInputSize());
	    } else {
		inputAttrs.push(ATTR_STYLE);
		inputAttrs.push(HU.css(CSS_WIDTH,HU.perc(100),
				       CSS_MIN_WIDTH,HU.px(50),
				       CSS_MAX_WIDTH,HU.px(300)));
	    }

	    let contents = "";
	    let topContents = "";	    
	    form+=HU.center(searchButton);
	    if(topItems.length>0) {
		if (horizontal) {
		    form += topItems[0];
		    topContents +=  HU.join(topItems.slice(1), "");
		} else {
		    topItems = topItems.map(item=>{
			return HU.div([ATTR_STYLE,HU.css(CSS_MARGIN_RIGHT,HU.px(8))], item);});
		    form+=HU.br();
		    form+=   HU.hrow(...topItems);
		}
	    }
	    
	    let ancestors  = this.getAncestors();
	    if(ancestors) {
		extra+=this.addWidget(this.getAncestorsLabel('Search Under'),
				      HU.div([ATTR_ID,this.domId(ID_SEARCH_ANCESTORS)]),{toggleClose:true});
		setTimeout(()=>{
		    this.loadAncestors(ancestors);
		},1);
	    }

	    if(this.getShowAncestorSelector(this.getProperty('showAncestor',true))) {
		let ancestor = HU.getUrlArgument(ID_ANCESTOR) ?? this.getProperty('ancestor');
		let name = HU.getUrlArgument(ID_ANCESTOR_NAME) ?? this.getProperty('ancestorName');		
		let aid = this.domId(ID_ANCESTOR);
		let clear = HU.href('javascript:void(0);',HU.getIconImage('fas fa-eraser'),
				    [ATTR_ONCLICK,'RamaddaUtils.clearSelect(' + HU.squote(aid) +');',
				     ATTR_TITLE,'Clear selection']);
		let input = HU.input('',name??'',[ATTR_READONLY,null,
						  ATTR_PLACEHOLDER,'Select',
						  ATTR_STYLE,HU.css(CSS_CURSOR,CURSOR_POINTER,CSS_WIDTH,HU.perc(100)),
						  ATTR_ID,aid,
						  ATTR_CLASS,'ramadda-entry-popup-select  disabledinput']);

		extra += HU.hidden('',ancestor??'',[ATTR_ID,aid+'_hidden']);
		input = HU.hbox([input,clear]);
		extra+=this.addWidget('Search Under',
				      HU.div([ATTR_ID,this.domId(ID_SEARCH_ANCESTOR)],input),
				      {toggleClose:!Utils.stringDefined(ancestor)});
	    }


	    let textFields = [];
            let textField = HU.input('', text, Utils.mergeLists([ATTR_TEXT_INPUT,'Text'],attrs,inputAttrs));
            if (this.getShowText()) {
		textFields.push(textField);
            }

            if (this.getShowName()) {
		textFields.push(HU.input('','',
					 Utils.mergeLists([ATTR_TEXT_INPUT,'Name',
							   ATTR_ID,this.domId(ID_NAME_FIELD),
							   ATTR_PLACEHOLDER,'Name'],
							  inputAttrs)));				       
	    }
            if (this.getShowDescription()) {
		textFields.push(HU.input('','',
					 Utils.mergeLists([ATTR_TEXT_INPUT,'Description',
							   ATTR_ID,this.domId(ID_DESCRIPTION_FIELD),
							   ATTR_PLACEHOLDER,'Description'],
							  inputAttrs)));				       
	    }	    

	    if(textFields.length) {
		extra+=this.addWidget('Text',Utils.join(textFields,HU.br()),{
		    toggleClose:this.getProperty('textToggleClose',toggleClose)});
	    }


	    let dateWidget='';
            if (this.getShowDate()) {
                this.dateRangeWidget = new DateRangeWidget(this,'date');
		let label=this.getLabel(this.getStartDateLabel());
		if(Utils.stringDefined(label))
                    extra += this.addWidget(label, HU.div([ATTR_ID,this.domId(ID_SEARCH_DATE_RANGE)],
							  this.dateRangeWidget.getHtml()));
		else
		    dateWidget+=this.dateRangeWidget.getHtml();
	    }
            if (this.getShowCreateDate()) {
		let label=this.getLabel(this.getCreateDateLabel());
                this.createdateRangeWidget = new DateRangeWidget(this,'createdate');
		if(Utils.stringDefined(label))
                    extra += this.addWidget(label,
					    HU.div([ATTR_ID,this.domId(ID_SEARCH_DATE_CREATE)], this.createdateRangeWidget.getHtml()));
		else 
		    dateWidget+=HU.div([ATTR_STYLE,HU.css(CSS_MARGIN_TOP,HU.px(4))],this.createdateRangeWidget.getHtml());
            }
            if (this.getShowChangeDate()) {
		let label=this.getLabel(this.getChangeDateLabel());
                this.changedateRangeWidget = new DateRangeWidget(this,'changedate');
		if(Utils.stringDefined(label))
                    extra += this.addWidget(label, HU.div([ATTR_ID,this.domId(ID_SEARCH_DATE_CHANGE)],
							  this.changedateRangeWidget.getHtml()));
		else 
		    dateWidget+=HU.div([ATTR_STYLE,HU.css(CSS_MARGIN_TOP,HU.px(4))],this.changedateRangeWidget.getHtml());
            }


	    if(Utils.stringDefined(dateWidget)) {
		extra+=this.addWidget('Date',dateWidget,{toggleClose:this.getProperty('dateToggleClose',toggleClose)});
	    }
            if (this.getShowArea()) {
		let label=this.getLabel(this.getAreaLabel('Geographic Location'));
		let areaWidget =new AreaWidget(this);
                this.addAreaWidget(areaWidget) 
                extra += this.addWidget(label, HU.div([ATTR_ID,this.domId(ID_SEARCH_AREA)], areaWidget.getHtml()),
					{toggleClose:this.getProperty('areaToggleClose',true)});
            }
            extra += HU.div([ATTR_ID, this.getDomId(ID_TYPEFIELDS)], "");
            if (Utils.stringDefined(this.getMetadataTypes())) {
		let metadataBlock = "";
                for (let i = 0; i < this.metadataTypeList.length; i++) {
                    let type = this.metadataTypeList[i];
                    let value = type.getValue();
		    let block = HU.div([ATTR_CLASS,"display-search-metadata-block"],
				       HU.div([ATTR_CLASS,"display-search-metadata-block-inner",
					       ATTR_ID,this.getMetadataFieldId(type)]));
		    metadataBlock += this.addWidget(type.getLabel(), block,{toggleClose:toggleClose});
                }
		extra += HU.div([ATTR_ID,this.domId(ID_SEARCH_TAGS)], metadataBlock);
            }


	    let recordsLabel = '#'+Utils.msgLabel('Records');
            extra +=HU.div([ATTR_STYLE,HU.css(CSS_MARGIN_TOP,HU.em(1),CSS_BORDER_TOP,CSS_BASIC_BORDER),
			    ATTR_CLASS,'display-search-widget'],
			   HU.b(recordsLabel) +' '+
			   HU.input("",  DEFAULT_MAX, [ATTR_CLASS,'display-simplesearch-input',
						       ATTR_ID,this.domId(ID_SEARCH_MAX),
						       ATTR_SIZE,'5']));

	    contents +=topContents;
	    topContents='';
            contents += HU.div([ATTR_CLASS, "display-search-extra",
				ATTR_ID, this.getDomId(ID_SEARCH_SETTINGS)],
			       HU.div([ATTR_CLASS, "display-search-extra-inner"], extra));
            //Hide the real submit button
            contents += HU.open(TAG_INPUT,[ATTR_TYPE,"submit",
					   ATTR_STYLE,HU.css(CSS_POSITION,POSITION_ABSOLUTE,CSS_LEFT,HU.px(-9999),
							     CSS_WIDTH,HU.px(1),CSS_HEIGHT,HU.px(1))]);
	    if(this.getFormHeight()) {
		contents = HU.div([ATTR_STYLE,HU.css(CSS_OVERFLOW_Y,OVERFLOW_AUTO,
						     CSS_MAX_HEIGHT,HU.getDimension(this.getFormHeight()))], contents);
	    }

	    if(Utils.stringDefined(topContents)) {
		form+=HU.div([ATTR_CLASS,"display-search-extra"],topContents);
	    }
	    form+=contents;
            form += HU.closeTag(TAG_FORM);
            return form;

        },
	getEgText:function(eg) {
            eg = this.getProperty(ATTR_PLACEHOLDER,eg||"Search");
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
	    HU.handleNewContent(this.jq(ID_TYPEFIELDS));
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
			if(!metadata  || metadata.undefined) return;
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
		let not  = $(this).attr("metadata-not");
		let value  = $(this).attr(ATTR_METADATA_VALUE);
		let type  = $(this).attr(ATTR_METADATA_TYPE);
		let index  = $(this).attr(ATTR_METADATA_INDEX);				
		let on = $(this).is(':checked');
		let cbx = $(this);
		let element  = _this.idToElement[$(this).attr(ATTR_ID)];
		if(on) {
		    if(element) element.setCbxOn((not?'!':'')+value);
		    let label = metadataType.getLabel();
		    _this.addMetadataTag(metadataType.getType(), label,value, cbx,not);
		} else {
		    if(element) element.setCbxOff((not?'!':'')+value);
		    let tagId = Utils.makeId(_this.domId(ID_SEARCH_TAG) +"_" + metadataType.getType() +"_" + value);
		    $("#" + tagId).remove();
		}		
		_this.submitSearchForm();
	    };

	    let hasMultipleElements = 	    metadata.getElements().length>1;

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
		let cbxs = element.makeCheckboxes(_this.idToElement,!hasMultipleElements);
		if(hasMultipleElements) {
		    if(element.select) {
			let menu = dest.append(element.select);
			element.menu = menu;
			menu.change(()=>{
			    _this.submitSearchForm();
			});
		    }
		} else {
		    if(cbxs.length>1  || cbxs.length>popupLimit) {
			if(element.select) {
			    let menu = $(element.select).appendTo(dest);
			    element.menu = menu;
			    HU.makeSelectTagPopup(menu,{
				hide:false,
				label:element.getName()});
			    menu.change(()=>{
				_this.submitSearchForm();
			    });
			} else {
			    dest.append(HU.div([],'Select')).button().click(function(){
				let cbxs2 = element.makeCheckboxes(_this.idToElement);
				_this.createTagDialog(cbxs2, $(this), cbxChange, metadataType.getType(),metadataType.getLabel());
			    });
			}
		    } else {
			dest.append(Utils.wrap(cbxs,"",HU.br()));
		    }
		}});
	    dest.find(":checkbox").change(cbxChange);
	    HU.handleNewContent(dest);
        },

	metadataTagSelected:function(type, value) {
	    let tagGroupId = ID_SEARCH_TAG_GROUP+"_"+type;
	    let tagGroup = this.jq(tagGroupId);
	    let existing = tagGroup.find(HU.attrSelect(ATTR_METADATA_TYPE,type)+HU.attrSelect(ATTR_METADATA_VALUE,value));
	    return (existing.length>0);
	},
	addMetadataTag:function(type, label,value, cbx,not) {
	    let _this = this;
	    let cbxId = cbx?cbx.attr(ATTR_ID):'unknowncbx';
	    let tagGroupId = ID_SEARCH_TAG_GROUP+'_'+type;
	    let tagGroup = _this.jq(tagGroupId);
	    if(this.metadataTagSelected(type, value)) return false;
	    let tagId = Utils.makeId(_this.domId(ID_SEARCH_TAG) +'_' +type +'_' + value);
	    if(tagGroup.length==0) {
		tagGroup = $(HU.div([ATTR_CLASS,'display-search-tag-group',
				     ATTR_ID,_this.domId(tagGroupId)])).appendTo(_this.jq(ID_SEARCH_BAR));
		
	    }
	    let prefix = not?'Not ':'';
	    let tag = $(HU.div(['source-id',cbxId,'metadata-not',not,
				ATTR_METADATA_TYPE,type,
				ATTR_METADATA_VALUE,value,
				ATTR_TITLE,label+':' + prefix + value,
				ATTR_CLASS,CLASS_SEARCH_TAG, ATTR_ID,tagId],
			       prefix + value+SPACE +HU.getIconImage('fas fa-times'))).appendTo(tagGroup);
	    tag.click(function() {
		let element=_this.idToElement[$(this).attr('source-id')];
		let value = $(this).attr(ATTR_METADATA_VALUE);
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
	    HU.initSelect(this.jq(ID_TYPE_FIELD),{selectOption: type.getId()});
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
		let ancestor = this.getAncestor();
		let extraArgs=null;
		if(ancestor) extraArgs='ancestor='+ ancestor;
                newTypes = this.getRamadda().getEntryTypes((ramadda, types) =>{
                    this.addTypes(types);
                },this.getEntryTypes(),extraArgs);
		if(newTypes==null) {
		    this.typesPending=true;
		}
            }
            if (newTypes == null) {
                return;
            }

            this.entryTypes = newTypes;
	    /*
              this.entryTypes = newTypes.sort((type1,type2)=>{
	      return type2.entryCount-type1.entryCount;
	      });*/

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
	    let addTypeCategory=this.getProperty('addTypeCategory',true) && this.entryTypes.length>1;
            let cats = [];
            let catMap = {};
            let select = HU.openTag(TAG_SELECT, [ATTR_ID, this.getDomId(ID_TYPE_FIELD),
						 ATTR_CLASS, 'display-typelist',
						 ATTR_ONCHANGE, this.getGet() + '.typeChanged();'
						]);
	    if(this.getAddAllTypes()) {
		select += HU.tag(TAG_OPTION, [ATTR_TITLE, '', ATTR_VALUE, VALUE_ANY_TYPE],'Any type');
	    }
	    if(this.getAddAnyType() && this.entryTypes.length>1) {
		select += HU.tag(TAG_OPTION, [ATTR_TITLE, '', ATTR_VALUE, ''],
				 this.getEntryTypes()?'Any of these types':'Any type');
	    }
	    let hadSelected = false;
	    let anySelected = false;
	    let startWithAny=this.getStartWithAny();
	    let fromUrl = HU.getUrlArgument(ID_TYPE_FIELD);
            this.entryTypes.every(type=>{
                anySelected = this.getSearchSettings().hasType(type.getId());
		if(fromUrl)
		    anySelected = type.getId()==fromUrl;
		return !anySelected;
	    });


	    let seen = {};
	    let excludeEmptyTypes = this.getExcludeEmptyTypes();
	    let excludeTypes = this.getExcludeTypes();	    
	    let excludeMap=null;
	    if(excludeTypes) {
		excludeMap={}
		Utils.split(excludeTypes,',',true,true).forEach(t=>{
		    excludeMap[t] = true;
		});
	    }		
            for (let i = 0; i < this.entryTypes.length; i++) {
                let type = this.entryTypes[i];
		if(seen[type.getId()]) continue;
		seen[type.getId()] = true;
		if(excludeEmptyTypes && type.getEntryCount()==0) continue;
		if(excludeMap && excludeMap[type.getId()]) continue;
                let icon = type.getIcon();
                let optionAttrs = [ATTR_TITLE, type.getLabel(),
				   ATTR_VALUE, type.getId(),
				   ATTR_CLASS, "display-typelist-type",
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
		    if(addTypeCategory)  {
			catMap[type.getCategory()] =
			    HU.tag(TAG_OPTION, [ATTR_CATEGORY,'true',
						ATTR_CLASS, "display-typelist-category",
						ATTR_TITLE, "",
						ATTR_VALUE, ""], type.getCategory());
		    }
                    cats.push(type.getCategory());
                }
		//		select+= option;
                catMap[type.getCategory()] += option;
            }
            for (let i in cats) {
                select += catMap[cats[i]];
            }

            select += HU.closeTag(TAG_SELECT);
	    if(this.entryTypes.length==0) {
	    } else  if(false && this.entryTypes.length==1) {
		if(this.entryTypes[0].getId()!='any')
		    this.writeHtml(ID_TYPE_DIV, HU.hidden(ID_TYPE_FIELD,this.entryTypes[0].getId()));
	    } else {
		let toggleClose = this.getProperty('typesToggleClose',this.getToggleClose());
		this.writeHtml(ID_TYPE_DIV,
			       this.addWidget(this.getProperty('typesLabel','Types'),select,{toggleClose:toggleClose}));
	    }
	    
            HU.initSelect(this.jq(ID_TYPE_FIELD),  { autoWidth: false,  'max-height':HU.px(100)});

	    HU.makeSelectTagPopup(this.jq(ID_TYPE_FIELD),{
		showCategories:true,
		icon:true,
		after:true,
		single:true,
		hide:false,
		label:$(this).attr('data-label')});	    

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
	    let onlyShow = null;
	    let showColumns = this.getShowColumns();
	    if(Utils.stringDefined(showColumns)) {

		Utils.split(this.getShowColumns(),',',true,true).forEach(c=>{
		    if(!onlyShow)
			onlyShow = {};
		    onlyShow[c] = true;
		});
	    }
            for (let i = 0; i < cols.length; i++) {
                let col = cols[i];
		if(onlyShow &&!onlyShow[col.getName()]) continue;
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
	    this.dateWidgets = [];
	    let popupLimit = this.getTagPopupLimit();
	    let toggleClose = this.getColumnsToggleClose(this.getToggleClose(!this.getProperty('searchOpen',true)));
            if (this.savedValues == null) this.savedValues = {};
            let extra = "";
            let cols = this.getSearchableColumns();
	    let lastGroup = null;
	    let inGroup=false;
            for (let i = 0; i < cols.length; i++) {
                let col = cols[i];
                if (this.getProperty("fields") != null && this.getProperty("fields").indexOf(col.getName()) < 0) {
                    continue;
                }

		let group = col.getSearchGroup();

		if(Utils.stringDefined(group) && group!=lastGroup) {
		    if(inGroup) extra+=HU.close(TAG_DIV)+HU.open(TAG_DIV);
		    inGroup=true;
		    lastGroup=group;
		    let widgetId = HU.getUniqueId('');
		    extra+=HU.open(TAG_DIV,[ATTR_CLASS,'display-search-group']);
		    let label = this.addToggle(group,widgetId,toggleClose);
		    extra+=HU.div([ATTR_CLASS,'display-search-group-label'],label);
		    extra+=HU.open(TAG_DIV,[ATTR_CLASS,'display-search-widgets',
					    ATTR_ID,widgetId,
					    ATTR_STYLE,HU.css(CSS_DISPLAY,toggleClose?DISPLAY_NONE:DISPLAY_BLOCK)]);
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
		    help = HU.span([ATTR_STYLE,HU.css(CSS_CURSOR,'help',
						      CSS_MARGIN_LEFT,HU.px(10)),
				    ATTR_TITLE,col.getSuffix()], HU.getIconImage('fas fa-info'));
		}		
		

                if (col.isEnumeration()) {
		    let showCheckboxes=col.showCheckboxes()
		    let prop = this.getProperty(col.getName()+'.showCheckboxes');
		    if(prop!=null) showCheckboxes= (String(prop)=='true');
		    let showLabels = this.getShowSearchLabels();
		    let values = col.getValues();
		    let searchValue = this.getSearchValue(col.getName());
		    if(showCheckboxes) {
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
                            field += HU.div([],HU.checkbox(boxId,[ATTR_CLASS,'display-entrylist-enum-checkbox',
								  ATTR_ID,boxId,'checkbox-id',id,
								  ATTR_DATA_VALUE,value],
							   value==searchValue, label));
			}
		    } else {
			let clazz = 'display-metadatalist';
			let attrs = [ATTR_ID, id];
			let optionAttrs = [ATTR_CLASS,"display-metadatalist-item",
					   ATTR_TITLE, "", ATTR_VALUE, VALUE_NONE];
			if(values.length>=popupLimit || col.getSearchMultiples()) {
			    attrs.push(ATTR_MULTIPLE,'true');
			    attrs.push(ATTR_SIZE,'4');			    
			} else {
			    clazz= 'display-searchmenu ' + clazz;
			}
			attrs.push(ATTR_CLASS,clazz);
			attrs.push('data-label',col.getLabel());
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
                            if (value === savedValue || value===searchValue) {
				extraAttr = " selected ";
				console.log(value,savedValue,searchValue);

                            }

			    if(value=="") {
				value = "--blank--";
			    }
                            let label = values[vidx].label.trim();
			    if(label=="&lt;blank&gt;") label="--blank--";
			    if(label=="")
				label= "--blank--"; 
                            field += HU.tag(TAG_OPTION, [ATTR_CLASS,"display-metadatalist-item",
							 ATTR_TITLE, label,
							 ATTR_VALUE, value, extraAttr, null],
					    label);
			    field+="\n";
			}
			field += HU.closeTag(TAG_SELECT);
		    }


		    if(showLabels) {
			label =  this.getLabel(col);
			widget = field+help;
		    } else {
			widget= HU.div([ATTR_CLASS,"display-search-block"], field+help);
		    }
		} else if (col.isNumeric()) {
		    let from = HU.input("", "", [ATTR_TITLE,"greater than",
						 ATTR_CLASS, "input display-simplesearch-input",
						 ATTR_STYLE,HU.css(CSS_WIDTH,HU.em(2.5)),
						 ATTR_ID, id+"_from"]);
		    let to = HU.input("", "", [ATTR_TITLE,"less than",
					       ATTR_CLASS, "input display-simplesearch-input",
					       ATTR_STYLE,HU.css(CSS_WIDTH,HU.em(2.5)),
					       ATTR_ID, id+"_to"]);		    
		    label = col.getSearchLabel();
                    widget = from +" - " + to +help;
                } else if(col.isLatLon()) {
		    let areaWidget= col.areaWidget = new AreaWidget(this,col.getName());
		    label = this.makeLabel(col.getSearchLabel());
                    widget= HU.div([ATTR_ID,this.domId(col.getName())], areaWidget.getHtml());
                } else if(col.getType()=='string') {
                    field = HU.input("", savedValue??this.getSearchValue(col.getName()),
				     [ATTR_PLACEHOLDER,col.getSearchLabel(),
				      ATTR_CLASS, "input display-simplesearch-input",
				      ATTR_SIZE, this.getTextInputSize(),
				      ATTR_ID, id]);
		    label = col.getSearchLabel();
                    widget =  field + " " + help;
		} else if(col.isDate()) {
		    label = col.getSearchLabel();
                    let dateWidget =  new DateRangeWidget(this,col.getName(),label+' start',
							  label+' end');
		    this.dateWidgets.push({widget:dateWidget,column:col});
		    widget=dateWidget.getHtml();
		} else if(col.isEntry()) {
		    let name = col.getName();

		    let clear = HU.href("javascript:void(0);",HU.getIconImage("fas fa-eraser"),
					[ATTR_ONCLICK,"RamaddaUtils.clearSelect(" + HU.squote(id) +");",
					 ATTR_TITLE,"Clear selection"]);
		    let input = HU.input("","",[ATTR_READONLY,null,
						ATTR_PLACEHOLDER,'Select',
						ATTR_STYLE,HU.css('cursor',CURSOR_POINTER,CSS_WIDTH,HU.perc(100)),
						ATTR_ID,id,
						ATTR_CLASS,"ramadda-entry-popup-select  disabledinput"]);

		    label = col.getSearchLabel();
		    widget = HU.hidden("","",[ATTR_ID,id+"_hidden"]);
		    widget+= HU.div([ATTR_ID,this.domId(ID_SEARCH_ANCESTOR)],
				    HU.leftRightTable(clear,input,HU.perc(5), HU.perc(95)));
		} else {
		    console.log('unknown column type:',col.getName(),col.getType());
		}
		let close = !inGroup;
		let toggleCloseProperty = this.getProperty(col.getName()+'.toggleClose',this.getToggleClose());
		if(Utils.isDefined(toggleCloseProperty)) {
		    close = String(toggleCloseProperty)=='true';
		}
		if(!toggleClose) close = false;
		extra+=this.addWidget(label,widget,{
		    addToggle:!inGroup,
		    addSimpleToggle:inGroup,
		    toggleClose:close
		});
	    }

	    if(inGroup) extra+=HU.close(TAG_DIV,TAG_DIV);
            this.writeHtml(ID_TYPEFIELDS, extra);
	    this.dateWidgets.forEach(widget=>{
		widget.widget.initHtml();
	    });


            cols.forEach(col=>{
		if(col.isEntry()) {
                    let id = this.getDomId(ID_COLUMN + col.getName());
		    jqid(id).click((event) =>{
			let root = this.getRamadda().getRoot();
			RamaddaUtils.selectInitialClick(event,id,id,true,null,null,col.entrytype,root);
		    });
		}
	    });


	    this.jq(ID_TYPEFIELDS).find('.display-metadatalist').each(function() {
		let opts = $(this).find(TAG_OPTION);
		if(opts.length<popupLimit) return;
		HU.makeSelectTagPopup($(this),{
		    hide:false,
		    label:$(this).attr('data-label')});
	    });

	    let _this = this;
	    this.jq(ID_TYPEFIELDS).find(".ramadda-expr").change(function() {
		let id = $(this).attr(ATTR_ID);
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
	    HU.initSelect(menus);
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
            this.getSearchSettings().skip =
		Math.max(0, this.getSearchSettings().skip - this.getSearchSettings().getMax());
            this.submitSearchForm();
        },
        entryListChanged: function(entryList) {
            this.entryList = entryList;
        }
    });
}


//xxxx
function RamaddaSearchDisplay(displayManager, id, properties, theType) {
    if (theType == null) {
        theType = DISPLAY_SEARCH;
    }
    //function RamaddaSearcherDisplay(displayManager, id,  type, properties) {
    const SUPER = new RamaddaSearcherDisplay(displayManager, id, theType, properties);
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
            if (this.changedateRangeWidget) {
                this.changedateRangeWidget.initHtml();
            }	    

            SUPER.initDisplay.apply(this);
            if (this.entryList != null && this.entryList.haveLoaded) {
                this.entryListChanged(this.entryList);
            }
	    if(!this.getProvidersMultiple()) {
		HU.initSelect(this.jq(ID_PROVIDERS), { multiple:true,
						       autoWidth: false,
						       'max-height':HU.px(100)});
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
            let textField = HU.input("", text, [ATTR_PLACEHOLDER, eg,
						ATTR_CLASS, "display-search-input",
						ATTR_SIZE, "30",
						ATTR_ID, this.getDomId(ID_TEXT_FIELD)]);
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
		html =HU.div([ATTR_STYLE,HU.css(CSS_MAX_HEIGHT,HU.px(1000),
						CSS_BACKGROUND,COLOR_WHITE,
						CSS_OVERFLOW_Y,OVERFLOW_AUTO)],html);
		return HU.div([ATTR_CLASS,'ramadda-expandable-wrapper',
			       ATTR_STYLE,HU.css(CSS_POSITION,POSITION_RELATIVE)],html);
	    }

	    this.getDisplayTypes().split(',').forEach(type=>{
		if(type=='list') {
		    titles.push('List');
		    addContents(makeExpandable(this.getEntriesTree(entries)));
		} else if(type=='images') {
		    let defaultImage = this.getDefaultImage();
		    let imageEntries = entries.filter(entry=>{
			if(defaultImage) return true;
			return entry.getIsImage();
		    });
		    if(imageEntries.length>0) {
			titles.push('Images');
			let id = HU.getUniqueId(type +'_');
			this.myDisplays.push({id:id,type:type});
			let images =HU.div([ATTR_ID,id,
					    ATTR_CLASS,'ramadda-expandable display-entrylist-images',
					    ATTR_STYLE,HU.css(CSS_WIDTH,HU.perc(100))]);
			addContents(makeExpandable(images));
		    }
		} else if(type=='timeline') {
		    titles.push('Timeline');
		    let id = HU.getUniqueId(type +'_');
		    this.myDisplays.push({id:id,type:type});
		    addContents(HU.div([ATTR_ID,id,
					ATTR_STYLE,HU.css(CSS_WIDTH,HU.perc(100))]));
		} else if(type=='map') {
		    this.areaEntries = entries.filter(entry=>{
			return entry.hasBounds() || entry.hasLocation();
		    });
		    if(this.areaEntries.length>0) {
			titles.push('Map');
			let id = HU.getUniqueId(type +'_');
			this.myDisplays.push({id:id,type:type,entries:this.areaEntries});
			addContents(HU.div([ATTR_ID,id,
					    ATTR_STYLE,HU.css(CSS_WIDTH,HU.perc(100))]));
		    }
		} else if(type=='display') {
		    titles.push('Details');
		    let id = HU.getUniqueId(type +'_');
		    this.myDisplays.push({id:id,type:'entrywiki',entries:entries});
		    addContents(HU.div([ATTR_ID,id,
					ATTR_STYLE,HU.css(CSS_WIDTH,HU.perc(100))]));
		} else if(type=='metadata') {		    
		    titles.push('Metadata');
		    let mtd = HU.div([ATTR_STYLE,HU.css(CSS_WIDTH,HU.px(800),CSS_MAX_WIDTH,HU.px(800),CSS_OVERFLOW_X,OVERFLOW_AUTO)],this.getEntriesMetadata(entries));
		    addContents(mtd);
		} else {
		    console.log('unknown display:' + type);
		}
	    });

	    if(titles.length==1) 
		return HU.div([ATTR_CLASS,'display-entrylist-content-border'],contents[0]);
	    let tabId = HU.getUniqueId('tabs_');
	    let tabs = HU.open(TAG_DIV,[ATTR_ID,tabId,
					ATTR_CLASS,'ui-tabs']);
	    tabs+=HU.open(TAG_UL);
	    titles.forEach((title,idx)=>{
		tabs +=HU.tag(TAG_LI,[],HU.href('#' + tabId+'-' + idx,title));
	    })
	    tabs+=HU.close(TAG_UL);
	    this.tabCount = contents.length;
	    contents.forEach((content,idx)=>{
		tabs +=HU.div([ATTR_ID,tabId+'-' + idx,
			       ATTR_CLASS,'ui-tabs-hide'], content);
	    });
	    tabs +=HU.close(TAG_DIV);
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

	    let url = button.attr(ATTR_DATA_URL);
	    let format = button.attr('data-format')
	    let formatName = button.attr('data-name')	    
	    let size = "100";
	    let doit = (extra) =>{
		url = url.replace(/max=\d+/,'max='+size);
		if(extra) url+=extra;
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
	    };

	    size= this.jq(ID_SEARCH_MAX).val();
	    if(!dontAsk) {
		if(format==OUTPUT_CHOOSE) {
		    let html = HU.formTable();
		    html += HU.formEntry('Number of Records:',
					 HU.input('',size,[ATTR_ID,this.domId('downloadrecords'),
							   ATTR_SIZE,'5']));
		    let select= [['xlsx','XLSX'],
				 ['csv','CSV'],
				 ['json','JSON'],
				 ['wget','wget File Download'],['csvapi','wget CSV API'],['ids','IDs'],
				 ['wrapper_r','R Wrapper'],
				 ['wrapper_python','Python Wrapper'],
				 ['wrapper_matlab','Matlab Wrapper']];
		    html+= HU.formEntry('What to download:',
					HU.select('',[ATTR_ID,this.domId('downloadwhat')],select));
		    let buttons = HU.buttons([
			HU.div([ATTR_CLASS,'ramadda-button-ok display-button'], 'OK'),
			HU.div([ATTR_CLASS,'ramadda-button-cancel display-button'], 'Cancel')]);
		    html+=HU.formTableClose();
		    html+=buttons;
		    html = HU.div([ATTR_STYLE,HU.css(CSS_MARGIN,HU.px(5))], html);
		    let dialog = HU.makeDialog({content:html,
						title:'Download options',
						anchor:button,
						draggable:true,header:true});
		    dialog.find('.ramadda-button-ok').button().click(()=>{
			size = this.jq('downloadrecords').val();
			what = this.jq('downloadwhat').val();
			doit('&what='+ what);
			dialog.remove();
		    });
		    dialog.find('.ramadda-button-cancel').button().click(()=>{
			dialog.remove();			
		    });
		} else {
		    size = prompt('How many records do you want in the ' + formatName +' download?',size);
		    if(!size) return;
		    
		    doit();
		}
	    } else {
		doit();
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
            if (this.footerRight != null) {
		let _this =this;
                this.writeHtml(ID_SEARCH_FOOTER, this.footerRight);
		this.jq(ID_SEARCH_FOOTER).find('.ramadda-search-link').button().click(function(event) {
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
		HU.tabLoaded();
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
			      new RecordField({type: "string", index: (index++), id: ATTR_ID,label: "Entry ID"}),			      
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
			dialog.find('.'+ CLASS_SEARCH_TAG).click(function() {
			    let type = $(this).attr(ATTR_METADATA_TYPE);
			    let value = $(this).attr(ATTR_METADATA_VALUE);			    
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
					  HU.center(HU.image(ramaddaCdn + '/icons/mapprogress.gif',
							     [ATTR_WIDTH,HU.px(80)])));
			};
		    }
		    let props = {centerOnMarkersAfterUpdate:true,
				 entries:info.entries,
				 dialogListener: dialogListener,
				 highlightColor:"#436EEE",
				 blockStyle:this.getProperty("blockStyle",""),
				 doPopup:this.getProperty("doPopup",true),
				 tooltip:tooltip,
				 tooltipClick:tooltip,
				 myTextGetter:myTextGetter,
				 descriptionField:"description",
				 imageWidth:HU.px(140),
				 blockWidth:HU.px(150),
				 numberOfImages:500,
				 includeNonImages:this.getProperty('includeNonImages',true),
				 showTableOfContents:true,

				 showTableOfContentsTooltip:false,
				 addMapLocationToUrl:false,
				 canMove:true,
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
		HU.makeExpandable($(this), false,{right:HU.px(5),top:HU.px(0)});
	    });


	    if(this.mapId && this.areaEntries && this.areaEntries.length>0) {
		let map = new RepositoryMap(this.mapId);
		map.initMap(false);
		this.areaEntries.forEach(entry=>{
                    let link = HU.tag(TAG_A, [ATTR_TARGET,"_entries",
					      ATTR_HREF, entry.getEntryUrl()], entry.getName());
		    let text = link;
		    if(entry.getIsImage()) {
			text = HU.image(entry.getResourceUrl(), [ATTR_WIDTH, "400",
								 ATTR_ID, this.getDomId("entry_" + entry.getIdForDom()),
								 ATTR_ENTRYID, entry.getId(),
								 ATTR_CLASS, "display-entrygallery-entry"
								]) +HU.br() + link;


			
		    }
		    //		    map.addMarker:  function(id, location, iconUrl, markerName, text, parentId, size, yoffset, canSelect, attrs) {
		    map.addMarker('',{x:entry.getLongitude(),y:entry.getLatitude()}, entry.getIconUrl(),"",text,null,16,0,true,{});
		});
		map.centerOnMarkersInit(null);
	    }

        },
    });
}


function RamaddaSimplesearchDisplay(displayManager, id, properties) {
    let myProps = this.simpleSearchProps = [
	{label:'Simple Search'},
	{p:'resultsPosition',ex:'absolute|relative'},
	{p:'maxHeight',ex:300},
	{p:'maxWidth',ex:200},
	{p:'maxWidth',ex:200},		
	{p:'autoSearch',ex:true},
	{p:'showHeader',ex:true},
	{p:'inputSize',d:'200px',ex:'100%'},
	{p:ATTR_PLACEHOLDER},
	{p:'searchEntryType',ex:'',tt:'Constrain search to entries of this type'},		
	{p:'doPageSearch',ex:'true',tt:'Just search in the page'},
	{p:'autoFocus',d:false,ex:'false',tt:'auto focus on the search input field'},	
	{p:'doTagSearch',ex:'true'},
	{p:'tagShowGroup',d:true},
	{p:'tagSearchLimit',tt:'Show the inline search box for tags when the #tags exceeds the limit',d:15},
	{p:'pageSearchSelector',d:'.search-component,.entry-list-row-data'},
	{p:'applyToEntries',ex:true,tt:'When doing the entry search use the IDs to hide/show components'},
	{p:'pageSearchParent',ex:'.class or #id',tt:'set this to limit the scope of the search'},
        {p:'showParent',ex:'true',tt:'Show parent entry in search results'},		
    ];

    if(!properties.width) properties.width=properties.inputSize??HU.px(230);
    const SUPER   = new RamaddaSearcherDisplay(displayManager, id, DISPLAY_SIMPLESEARCH, properties);
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
	callNumber:1,
        haveDisplayed: false,
        selectedEntries: [],
	getWikiEditorTags: function() {
	    return this.simpleSearchProps;
	},
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
		    $(this).addClass(CLASS_CLICKABLE).click(function(){
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
			let title = ele.attr(ATTR_TITLE)+HU.getTitleBr()??'';
			title+='Click to filter';
			group.contents+=HU.image(ele.attr('data-image-url'),
						 [ATTR_CLASS,HU.classes('metadata-tag',CLASS_CLICKABLE),
						  'metadata-tag',tag,
						  ATTR_TITLE,title]);
		    } else {
			let label = '#'+obj.count+': ' + tag.replace(/^[^:]+:/,'');
			style = ele.attr(ATTR_STYLE);
			group.contents+=HU.div(['data-background',ele.attr('data-background'),
						'data-style',style??'',
						ATTR_STYLE,style??'',
						ATTR_CLASS,HU.classes('metadata-tag',CLASS_CLICKABLE),
						'metadata-tag',tag],label);
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
		    contents+=HU.div([ATTR_STYLE,HU.css(CSS_TEXT_ALIGN,ALIGN_LEFT)],block);
		})
		contents+=HU.open(TAG_DIV);
		if(this.getDoPageSearch()) {
		    contents = HU.center(this.getDefaultHtml() + contents);
		} 
		contents+=HU.close(TAG_DIV);
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
			$(this).css(CSS_BACKGROUND,$(this).attr('data-background')??"");
			let style = $(this).attr('data-style');
			if(style) $(this).attr(ATTR_STYLE,style);
		    } else {
			$(this).addClass("metadata-tag-selected");
			$(this).css(CSS_BACKGROUND,'');
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

            this.jq(ID_SEARCH).button().click(function(event) {
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
	    let abs = (this.getProperty("resultsPosition",POSITION_ABSOLUTE)==POSITION_ABSOLUTE);
	    if(!abs) {
		if(this.getMaxHeight(400)) {
		    style+=HU.css(CSS_MAX_HEIGHT,HU.getDimension(this.getMaxHeight(400)));
		} 
		if(this.getMaxWidth()) {
		    style+=HU.css(CSS_WIDTH,HU.getDimension(this.getMaxWidth(400)));
		    style+=HU.css(CSS_MAX_WIDTH,HU.getDimension(this.getMaxWidth(200)));
		}
		let entries = HU.div([ATTR_ID,this.domId(ID_ENTRIES),
				      ATTR_CLASS,"display-simplesearch-entries",
				      ATTR_STYLE,style]);
		if (this.getShowHeader(true)) {
		    html+=  HU.div([ATTR_CLASS, "display-entries-results",
				    ATTR_ID, this.getDomId(ID_RESULTS)]);
		}
		html+=entries;
	    }
	    return html;
	},
        makeSearchForm: function() {
            let form = HU.openTag(TAG_FORM, [ATTR_ID, this.getDomId(ID_FORM),
					     ATTR_ACTION, "#"]);
	    
	    let eg = this.getEgText();
	    let text  = this.getFormText();
	    let hadInitText = false;
	    if(!Utils.stringDefined(text) && this.getDoPageSearch()) {
		text = HU.getUrlArgument(ARG_PAGESEARCH)??'';
		if(Utils.stringDefined(text)) {
		    hadInitText = true;
		}
	    }


	    let size = HU.getDimension(this.getPropertyInputSize());
            let textField = HU.input("", text, [ATTR_STYLE, HU.css(CSS_WIDTH, size),
						ATTR_PLACEHOLDER, eg,
						ATTR_CLASS, "display-search-input",
						ATTR_ID, this.getDomId(ID_TEXT_FIELD)]);

	    form += textField;
            form += HU.tag(TAG_INPUT,[ATTR_TYPE,'submit',
				      ATTR_STYLE,HU.css(CSS_POSITION,POSITION_ABSOLUTE,
							CSS_LEFT,HU.px(-9999),
							CSS_WIDTH,HU.px(1),
							CSS_HEIGHT,HU.px(1))]);
            form += HU.closeTag(TAG_FORM);
	    form+=HU.div([ATTR_ID,this.domId(ID_FORM)]);
	    if(hadInitText) {
		setTimeout(()=>{
		    this.doInlineSearch();
		},1);
	    }
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
                let header = HU.div([ATTR_CLASS, "display-entries-results",
				     ATTR_ID, this.getDomId(ID_RESULTS)], SPACE);
                let entries= HU.div([ATTR_CLASS,"display-entries-entries",
				     ATTR_ID, this.getDomId(ID_ENTRIES)], "");		
		this.dialog = HU.makeDialog({content:header+entries,anchor:this.getContents(),
					     draggable:true,header:true});
	    } else {
		this.dialog.show();
	    }
	},	    
	writeEntries: function(msg, entries) {
	    let abs = this.getProperty("resultsPosition",POSITION_ABSOLUTE)==POSITION_ABSOLUTE;
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
		if(this.getDoPageSearch()) {
		    HU.removeFromDocumentUrl(ARG_PAGESEARCH);
		}
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
		    let values = Utils.split(value,",",true,true);
		    textOk = false;
		    let html = Utils.stripTags($(this).html());
		    let corpus = $(this).attr(ATTR_DATA_CORPUS)??' ';
		    html+=corpus+' ';
		    html+=$(this).attr('entryid')??' ';		    
		    html = html.toLowerCase();
		    textOk = true;
		    values.every(v=>{
			textOk = false;
			if(html.indexOf(v)>=0) {
			    textOk = true;
			} else if(regExp) {
			    if(html.match(regExp)) textOk = true;
			}
			return textOk;
		    });
		}
		if(!tagOk || !textOk) {
		    $(this).fadeOut();
		} else {
		    $(this).show();
		}
	    });

	    if(this.getDoPageSearch()) {
		HU.addToDocumentUrl(ARG_PAGESEARCH,value);
	    }
	    
	    
	},
	clearPageSearch:function() {
	    let sel = this.getPageSearchSelectors();
	    sel.show();
	},
	clearSearch:function() {
	    if(this.getApplyToEntries()) {
		let sel = this.getPageSearchSelectors();
		sel.each(function() {
		    $(this).show();
		});
	    }
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
            let entries = this.entryList.getEntries();
	    if(this.getApplyToEntries()) {
		if(this.dialog) {
		    this.dialog.hide();
		}
		let sel = this.getPageSearchSelectors();
		//		this.writeMessage("Found: " + entries.length +" entries");

		if(entries.length==0) {
		    sel.each(function() {
			$(this).show();
		    });
		} else {
		    let map = {}
		    entries.forEach(e=>{
			map[e.getId()]=true;
		    });
		    sel.each(function() {
			let entryId = $(this).attr('entryid');
			if(!entryId) return;
			if(map[entryId]) {
			    $(this).show();
			} else {
			    $(this).fadeOut();
			}
		    });

		}
		//		return
	    }


            SUPER.entryListChanged.apply(this, [entryList]);


            if (entries.length == 0) {
                this.getSearchSettings().skip = 0;
                this.getSearchSettings().setMax(DEFAULT_MAX);
		this.handleNoEntries();
                return;
            }
            this.writeMessage(this.getResultsHeader(entries, true));
	    this.initCloser(ID_RESULTS);

            let get = this.getGet();
            if (this.footerRight != null) {
                this.writeHtml(ID_SEARCH_FOOTER, this.footerRight);
            }


	    let html = "";
	    let inner = "";
	    let map = {};
	    let showParent = this.getProperty("showParent");
	    entries.forEach((entry,idx) =>{
		map[entry.getId()] = entry;
		let thumb = entry.getThumbnail();
		let attrs = [ATTR_TITLE,entry.getName(),
			     ATTR_CLASS,"display-simplesearch-entry",
			     ATTR_ENTRYID,entry.getId()];
		if(thumb) attrs.push("thumbnail",thumb);
		let link = HU.href(this.getRamadda().getEntryUrl(entry),entry.getIconImage() +"  "+ entry.getName());
		if(showParent && entry.getParentName()) {
		    let url = ramaddaBaseUrl+ "/entry/show?entryid=" + entry.parent;
		    let plink = HU.href(url, HU.image(entry.parentIcon) +" " + entry.parentName);
		    link = HU.hbox([plink,HU.span([ATTR_STYLE,HU.css(CSS_MARGIN_RIGHT,HU.px(4),CSS_MARGIN_LEFT,HU.px(4))],"&raquo;"), link]);
		}
		inner+=HU.div(attrs, link);
	    });
	    //	    inner = HU.div([ATTR_CLASS,"display-simplesearch-entries"],inner);
            this.writeEntries(inner, entries);
	    let _this = this;
	    this.jq(ID_ENTRIES).find(".display-simplesearch-entry").tooltip({
		show: {
		    delay: 1000,
		    duration: 300
		},
		content: function() {
		    let entry = map[$(this).attr(ATTR_ENTRYID)];
		    if(!entry) return null;
		    let thumb = $(this).attr("thumbnail");
		    let parent;
		    let html =entry.getIconImage()+' '+ HU.b(entry.getName());
		    html+=HU.div([],'Type: ' + entry.getTypeName());
		    let snippet = entry.getSnippet();
		    if(snippet)
			html+=HU.div([ATTR_STYLE,HU.css(CSS_BORDER_TOP,CSS_BASIC_BORDER)],snippet);
		    if(thumb) {
			html+=
			    HU.div([ATTR_STYLE,HU.css(CSS_MAX_HEIGHT,HU.px(100),
						      CSS_OVERFLOW_Y,OVERFLOW_HIDDEN,
						      CSS_BORDER_TOP,CSS_BASIC_BORDER)],
				   HU.image(thumb,[ATTR_WIDTH,HU.px(300)]));
		    }
		    return html;
		}});

            this.getDisplayManager().handleEventEntriesChanged(this, entries);
        },

    });
}




//xxxx
function RamaddaEntrylistDisplay(displayManager, id, properties, theType) {
    const SUPER = new RamaddaSearchDisplay(displayManager, id, properties);
    let myProps = [];
    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {});
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
                        HU.image(icon_zoom, [ATTR_CLASS, "display-grid-action",
					     ATTR_TITLE, "reset zoom", "action", "reset"]) +
                        HU.image(icon_zoom_in, [ATTR_CLASS, "display-grid-action",
						ATTR_TITLE, "zoom in", "action", "zoomin"]) +
                        HU.image(icon_zoom_out, [ATTR_CLASS, "display-grid-action",
						 ATTR_TITLE, "zoom out", "action", "zoomout"]);
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
            html += HU.openTag(TAG_DIV, [ATTR_ID, this.getDomId(ID_SETTINGS)]);
            html += HU.formTable();
            html += HU.formEntry("",
				 HU.checkbox(this.getDomId(ID_SHOW_ICON),
					     ["attr", ID_SHOW_ICON],
					     this.getProperty(ID_SHOW_ICON, "true")) + " Show Icon" +
				 SPACE2 +
				 HU.checkbox(this.getDomId(ID_SHOW_NAME),
					     ["attr", ID_SHOW_NAME],
					     this.getProperty(ID_SHOW_NAME, "true")) + " Show Name");
            html += HU.formEntry("X-Axis:",
				 HU.checkbox(this.getDomId(ID_XAXIS_ASCENDING),
					     ["attr", ID_XAXIS_ASCENDING],
					     this.getXAxisAscending()) + " Ascending" +
				 SPACE2 +
				 HU.checkbox(this.getDomId(ID_XAXIS_SCALE),
					     ["attr", ID_XAXIS_SCALE],
					     this.getXAxisScale()) + " Scale Width");
            html += HU.formEntry("Y-Axis:",
				 HU.checkbox(this.getDomId(ID_YAXIS_ASCENDING),
					     ["attr", ID_YAXIS_ASCENDING],
					     this.getYAxisAscending()) + " Ascending" +
				 SPACE2 +
				 HU.checkbox(this.getDomId(ID_YAXIS_SCALE),
					     ["attr", ID_YAXIS_SCALE],
					     this.getYAxisScale()) + " Scale Height");

            html += HU.formEntry("Box Color:",
				 HU.input(this.getDomId(ID_COLOR),
					  this.getProperty(ID_COLOR, "lightblue"),
					  ["attr", ID_COLOR]));

            html += HU.formTableClose();
            html += HU.closeTag(TAG_DIV);
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
                let index = parseInt($(this).attr(ATTR_INDEX));
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
                let id = $(this).attr(ATTR_ENTRYID);
                if (id) {
                    let other = _this.canvas.find("[entryid='" + id + "']");
                    other.each(function() {
                        if ($(this).attr("itemtype") == "box") {
                            $(this).attr("prevcolor", $(this).css(CSS_BACKGROUND));
                            $(this).css(CSS_BACKGROUND, $(this).attr("prevcolor"));
                        }
                    });
                }

                _this.gridPopup.hide();
            });
            items.mouseover(function(evt) {
                let id = $(this).attr(ATTR_ENTRYID);
                if (id) {
                    let other = _this.canvas.find("[entryid='" + id + "']");
                    other.each(function() {
                        if ($(this).attr("itemtype") == "box") {
                            $(this).attr("prevcolor", $(this).css(CSS_BACKGROUND));
                            $(this).css(CSS_BACKGROUND, "rgba(0,0,255,0.5)");
                        }
                    });
                }
                let x = GuiUtils.getEventX(evt);
                let index = parseInt($(this).attr(ATTR_INDEX));
                entry = entries[index];
                let thumb = entry.getThumbnail();
                let html = "";
                if (thumb) {
                    html = HU.image(thumb, [ATTR_WIDTH, "300;"]) + HU.br();
                } else if (entry.getIsImage()) {
                    html += HU.image(entry.getResourceUrl(), [ATTR_WIDTH, "300"]) + HU.br();
                }
                html += entry.getIconImage() + " " + entry.getName() + HU.br();
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
            html += HU.openDiv([ATTR_CLASS, "display-grid",
				ATTR_ID, this.getDomId(ID_GRID)]);
            html += HU.div([ATTR_CLASS, "display-grid-popup ramadda-popup"], "");
            html += HU.openTag(TAG_TABLE, [ATTR_BORDER, HU.px(0),
					   ATTR_CLASS, "",
					   ATTR_CELLSPACING, "0",
					   ATTR_WIDTH, HU.perc(100),
					   ATTR_STYLE, HU.css(CSS_HEIGHT,HU.perc(100))]);
            html += HU.openTag(TAG_TR, [ATTR_VALIGN, POS_BOTTOM]);
            html += HU.tag(TAG_TD);
            html += HU.tag(TAG_TD, [], HU.div([ATTR_ID, this.getDomId(ID_LINKS)], ""));
            html += HU.closeTag(TAG_TR);
            html += HU.openTag(TAG_TR, [ATTR_STYLE, HU.css(CSS_HEIGHT,HU.perc(100))]);
            html += HU.openTag(TAG_TD, [ATTR_STYLE, HU.css(CSS_HEIGHT,HU.perc(100))]);
            html += HU.openDiv([ATTR_CLASS, "display-grid-axis-left ramadda-noselect",
				ATTR_ID, this.getDomId(ID_AXIS_LEFT)]);
            html += HU.closeDiv();
            html += HU.closeDiv();
            html += HU.closeTag(TAG_TD);
            html += HU.openTag(TAG_TD, [ATTR_STYLE, HU.css(CSS_HEIGHT,HU.px(this.getProperty("height", "400")))]);
            html += HU.openDiv([ATTR_CLASS, "display-grid-canvas ramadda-noselect",
				ATTR_ID, this.getDomId(ID_CANVAS)]);
            html += HU.closeDiv();
            html += HU.closeDiv();
            html += HU.closeTag(TAG_TD);
            html += HU.closeTag(TAG_TR);
            html += HU.openTag(TAG_TR, []);
            html += HU.tag(TAG_TD, [ATTR_WIDTH, "100"], SPACE);
            html += HU.openTag(TAG_TD, []);
            html += HU.div([ATTR_CLASS, "display-grid-axis-bottom ramadda-noselect",
			    ATTR_TITLE, mouseInfo, ATTR_ID, this.getDomId(ID_AXIS_BOTTOM)], "");
            html += HU.closeTag(TAG_TABLE);
            html += HU.closeTag(TAG_TD);
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
                let style = HU.css(CSS_BOTTOM,HU.perc(tick.percent));
                let lineClass = tick.major ? "display-grid-hline-major" : "display-grid-hline";
                axis.Y.lines += HU.div([ATTR_STYLE, style,
					ATTR_CLASS, lineClass], " ");
                axis.Y.html += HU.div([ATTR_STYLE, style,
				       ATTR_CLASS, "display-grid-axis-left-tick"],
				      tick.label + " " + HU.div([ATTR_CLASS, "display-grid-htick"], ""));
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
                    axis.X.lines += HU.div([ATTR_STYLE, HU.css(CSS_LEFT,HU.perc(tick.percent)),
					    ATTR_CLASS, lineClass], " ");
                }
                axis.X.html += HU.div([ATTR_STYLE, HU.css(CSS_LEFT,HU.perc(tick.percent)),
				       ATTR_CLASS, "display-grid-axis-bottom-tick"],
				      HU.div([ATTR_CLASS, "display-grid-vtick"], "") + " " + tick.label);
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
                    style += HU.css(CSS_LEFT,HU.perc(xInfo.p1));
                    pos += HU.css(CSS_LEFT,HU.perc(xInfo.p1));
                } else {
                    style += HU.css(CSS_RIGHT,HU.perc(xInfo.p1));
                    pos += HU.css(CSS_LEFT,HU.perc((100 - xInfo.p2)));
                }

                if (axis.X.scale) {
                    if (xInfo.delta > 1) {
                        style += HU.css(CSS_WIDTH,HU.perc(xInfo.delta));
                    } else {
                        style += HU.css(CSS_WIDTH,HU.px(this.getProperty("fixedWidth", "5")));
                    }
                }


                let namePos = pos;
                if (axis.Y.ascending) {
                    style += HU.css(CSS_BOTTOM,HU.perc(vInfo.p2));
                    pos += HU.css(CSS_BOTTOM, HU.perc(vInfo.p2));
                    namePos += HU.css(CSS_BOTTOM,HU.perc(vInfo.p2));
                } else {
                    style += HU.css(CSS_TOP,HU.perc(vInfo.p2));
                    pos += HU.css(CSS_TOP,HU.perc(vInfo.p2));
                    namePos += HU.css(CSS_TOP,HU.perc(vInfo.p2));
                    namePos += HU.css(CSS_MARGIN_TOP,HU.px(-15));
                }
                if (axis.Y.scale) {
                    if (vInfo.p2 > 1) {
                        style += HU.css(CSS_HEIGHT,HU.perc(vInfo.delta));
                    } else {
                        style += HU.perc(CSS_HEIGHT,HU.px(this.getProperty("fixedHeight", "5")));
                    }
                }

                if (entry.getName().includes("rilsd")) {
                    console.log("pos:" + namePos);
                }
                if (showIcon) {
                    items += HU.div([ATTR_CLASS, "display-grid-entry-icon display-grid-entry",
				     ATTR_ENTRYID, entry.getId(), ATTR_INDEX, i,
				     ATTR_STYLE, pos], entry.getIconImage());
                }
                let key = Math.round(xInfo.p1) + "---" + Math.round(vInfo.p1);
                if (showName && !seen[key]) {
                    seen[key] = true;
                    let name = entry.getName().replace(/ /g, SPACE);
                    items += HU.div([ATTR_CLASS, "display-grid-entry-text display-grid-entry",
				     ATTR_ENTRYID, entry.getId(), ATTR_INDEX, i,
				     ATTR_STYLE, namePos], name);
                }
                let boxStyle = style + "background:" + this.getProperty(ID_COLOR, "lightblue");
                items += HU.div([ATTR_CLASS, "display-grid-entry-box display-grid-entry", "itemtype", "box",
				 ATTR_ENTRYID, entry.getId(),
				 ATTR_STYLE, boxStyle, ATTR_INDEX, i], "");
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
                            label = label + HU.br() + year;
                            lastYear = year;
                            major = true;
                        }
                    } else {
                        label = tickDate.getUTCDate();
                        if (lastYear != year || lastMonth != month) {
                            label = label + HU.br() + months[month] + " " + year;
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
            HU.formatTable("#" + this.getDomId(TAG_TABLE), {
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
                title = HU.tag(TAG_A, [ATTR_HREF, url,
				       ATTR_TITLE, this.sourceEntry.getName()], title);
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
                this.setContents(SPACE);
                return;
            }
            let html = this.getEntryHtml(entry, {
                showHeader: false
            });
            let height = this.getProperty("height", HU.px(400));
            if (!height.endsWith("px")) height += "px";
            this.setContents(HU.div([ATTR_CLASS, "display-entry-description",
				     ATTR_STYLE, HU.css(CSS_HEIGHT,height)],
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
		html = this.getProperty("template",HU.b("${icon} ${name} Date: ${date}"));
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
	properties.displayStyle=HU.css(CSS_WIDTH,HU.perc(100));
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
	{p:'wikiStyle',d:HU.css(CSS_WIDTH,HU.perc(100),CSS_MAX_WIDTH,'95vw',CSS_MIN_HEIGHT,HU.px(400))}
    ];

    defineDisplay(addRamaddaDisplay(this), SUPER, myProps, {
	initDisplay: function() {
            this.createUI();
	    let html = HU.div([ATTR_ID,this.domId(ID_WIKI),
			       ATTR_STYLE,this.getWikiStyle()]);
	    let entryMap = {};
	    if(properties.entries) {
		//		this.sourceEntry=properties.entries[0];
		let options = [];
               	options.push(HU.tag(TAG_OPTION, [], 'View Entry'));
		properties.entries.forEach(entry=>{
		    entryMap[entry.getId()] = entry;
                    let icon = entry.getIconUrl();
		    let label = entry.getName();
		    let tt = label;
		    let type = entry.getType();
		    if(type) tt+=  ' - '+ type.getLabel();
                    let optionAttrs = [ATTR_TITLE, tt,
				       ATTR_VALUE, entry.getId(), "data-iconurl", icon];
                    let option = HU.tag(TAG_OPTION, optionAttrs, label);
		    options.push(option);
		});
		
		let header = HU.select('',[ATTR_STYLE,HU.css(CSS_WIDTH,HU.px(400)),
					   ATTR_ID,this.domId('entrymenu')],options);
		header+=HU.div([ATTR_STYLE,HU.css(CSS_MARGIN_TOP,HU.px(4)),
				ATTR_ID,this.domId('entry_breadcrumbs'),
				ATTR_CLASS,'display-entrylist-details-ancestors']);
		html = HU.div([ATTR_STYLE,HU.css(CSS_MARGIN_TOP,HU.px(4),
						 CSS_MARGIN_BOTTOM,HU.px(8),
						 CSS_BORDER_BOTTOM,CSS_BASIC_BORDER)],header) + html;
	    }
	    this.displayHtml(html);
	    if(properties.entries) {
		let _this = this;
		let menu = this.entryMenu = this.jq('entrymenu');
		menu.change(function() {
		    let entry = entryMap[$(this).val()];
		    if(!entry) return;
		    _this.displayEntryBreadcrumbs(entry,_this.domId('entry_breadcrumbs'),4);		    
		    _this.loadEntry(entry);
		});
		HU.initSelect(menu,{ autoWidth: true,  'max-height':HU.px(100)});
		HU.makeSelectTagPopup(menu,{icon:true,single:true,makeButtons:false});
		if(this.sourceEntry)
		    this.displayEntryBreadcrumbs(this.sourceEntry,this.domId('entry_breadcrumbs'),4);		    
	    }

	    if(this.sourceEntry) {
		this.loadEntry(this.sourceEntry);
	    }
        },
	loadEntry:function(entry) {
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
	    this.wikify(wiki,entry.getId(),html=>{
		addDisplayListener = display=>{
		    this.addedDisplays.push(display);
		    //			console.log("add display:" + display.type);
		};
		this.jq(ID_WIKI).html(html);
		addDisplayListener = null;
	    });
	},

        handleEventRecordSelection: function(source, args) {
	    SUPER.handleEventRecordSelection.call(this,source,args);
	    if(!this.entryMenu) return;
	    if(!args.record) return;
	    let entryId = args.record.getValueFromField(ATTR_ID);	    
	    if(entryId) {
		//		this.entryMenu.val(entryId);
		this.entryMenu.data("selectBox-selectBoxIt").selectOption(entryId);
	    }
	},


	setEntry: function(entry) {
	    this.sourceEntry = entry;
	    this.initDisplay();
	},
        handleEventEntrySelection: function(source, args) {
	    this.logMsg('entry select');
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
            html += HU.div([ATTR_ID, this.domId(ID_ENTRIES),
			    ATTR_CLASS, this.getClass("entries")], "");
            this.setContents(html);
        },
        entryListChanged: function(entryList) {
            let html = "<form>";
            html += HU.p();
            html += HU.openTag(TAG_TABLE, [ATTR_CLASS, CLASS_FORMTABLE,
					   ATTR_CELLSPACING, 0,
					   ATTR_CELLSPACING, 0]);
            let entries = this.entryList.getEntries();
            let get = this.getGet();

            for (let j = 1; j <= 2; j++) {
                let select = HU.openTag(TAG_SELECT, [ATTR_ID, this.getDomId(ID_SELECT + j)]);
                select += HU.tag(TAG_OPTION, [ATTR_TITLE, "", ATTR_VALUE, ""],
				 "-- Select --");
                for (let i = 0; i < entries.length; i++) {
                    let entry = entries[i];
                    let label = entry.getIconImage() + " " + entry.getName();
                    select += HU.tag(TAG_OPTION, [ATTR_TITLE, entry.getName(),
						  ATTR_VALUE, entry.getId()],
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
            html += HU.p();
            html += HU.tag(TAG_DIV, [ATTR_CLASS, "display-button",
				     ATTR_ID, this.getDomId(ID_NEWDISPLAY)], "New Chart");
            html += HU.p();
            html += HU.close(TAG_FORM);
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
            html += HU.openTag(TAG_TABLE, [ATTR_CLASS, "display-repositories-table",
					   ATTR_WIDTH, HU.perc(100),
					   ATTR_BORDER, "1",
					   ATTR_CELLSPACING, "0",
					   ATTR_CELLPADDING, "5"]);
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

            html += HU.openTag(TAG_TR, [ATTR_VALIGN, POS_BOTTOM]);
            html += HU.th([ATTR_CLASS, "display-repositories-table-header"], "Type");
            for (let i = 0; i < this.ramaddas.length; i++) {
                let ramadda = this.ramaddas[i];
                let link = HU.href(ramadda.getRoot(), ramadda.getName());
                html += HU.th([ATTR_CLASS, "display-repositories-table-header"], link);
            }
            html += HU.close(TAG_TR);

            let onlyCats = [];
            if (this.categories != null) {
                onlyCats = this.categories.split(",");
            }



            let catMap = {};
            let cats = [];
            for (let typeIdx = 0; typeIdx < allTypes.length; typeIdx++) {
                let type = allTypes[typeIdx];
                let row = "";
                row += HU.open(TAG_TR);
                row += HU.td([], HU.image(type.getIcon()) + " " + type.getLabel());
                for (let i = 0; i < this.ramaddas.length; i++) {
                    let ramadda = this.ramaddas[i];
                    let repoType = ramadda.getEntryType(type.getId());
                    let col = "";
                    if (repoType == null) {
                        row += HU.td([ATTR_CLASS, "display-repositories-table-type-hasnot"], "");
                    } else {
                        let label =
                            HU.tag(TAG_A, [ATTR_HREF, ramadda.getRoot() + "/search/type/" + repoType.getId(),
					   ATTR_TARGET, "_blank"],
                                   repoType.getEntryCount());
                        row += HU.td([ATTR_ALIGN, "right",
				      ATTR_CLASS, "display-repositories-table-type-has"], label);
                    }

                }
                row += HU.close(TAG_TR);

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
                html += HU.open(TAG_TR);
                html += HU.th(["colspan", "" + (1 + this.ramaddas.length)], cat);
                html += HU.close(TAG_TR);
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
    metadataType.addNot = metadata.addNot;

    if(metadata.elements) 
	this.elements= metadata.elements.map(element=>{return new DisplayEntryMetadataElement(display,this,element);});
    $.extend(this,{
	getType:function() {
	    return this.metadataType.getType();
	},
	getLabel:function() {
	    return this.metadataType.getLabel();
	},
	getAddNot:function() {
	    return this.metadataType.getAddNot();
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
	    let input = HU.input('','',[ATTR_CLASS,'display-simplesearch-input',
					ATTR_ID,this.inputId,
					ATTR_STYLE,HU.css(CSS_WIDTH,HU.perc(100)),
					ATTR_PLACEHOLDER,this.getName()]);
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
		    label:this.getName(),
		    index:this.getIndex(),
		    value: value
		});
	    });
	    if(this.selectId && jqid(this.selectId).length) {
		text=jqid(this.selectId).val();
	    } else if(this.getType()=='string') {
		text = this.getInputText();
	    }
	    if(text) {
		let textArray = text;
		if(!Array.isArray(textArray)) textArray = [textArray];
		textArray.forEach(text=>{
		    if(!Utils.stringDefined(text)) return;
		    settings.metadata.push({
			type: this.getMetadataType(),
			label:this.getName(),
			index:this.getIndex(),
			value: text
		    });
		});
	    }

	},


	makeCheckboxes:function(idToElementMap,multiples) {
	    let cbxs=[];
	    this.selectId = this.display.getMetadataFieldId(this.metadata.getType())+"_select_" + this.getIndex();
            let select =multiples?'': HU.tag(TAG_OPTION, [ATTR_TITLE, "", ATTR_VALUE, ""]);
	    let popupLimit = this.display.getTagPopupLimit();
	    let addNot = this.display.getProperty('metadata.' +this.metadata.getType()+'.addnot',this.metadata.getAddNot());
	    this.getValues().forEach((v,i)=>{
                let count = v.count;
                let value = v.value;
                let label = v.label;
		let type =this.metadata.getType();
                let optionAttrs = [ATTR_VALUE, value,
				   ATTR_CLASS, "display-metadatalist-item"];
                let selected = this.cbxState[value];
                if (selected) {
		    optionAttrs.push("selected");
		    optionAttrs.push(null);
                }
                select += HU.tag(TAG_OPTION, optionAttrs, label + " (" + count + ")");
		let cbxId = this.display.getMetadataFieldId(this.metadata.getType())+"_checkbox_" + this.getIndex()+"_"+i;
		if(idToElementMap) idToElementMap[cbxId] = this;
		let cbx = HU.checkbox("",[ATTR_ID,cbxId,
					  ATTR_METADATA_TYPE,type,
					  ATTR_METADATA_INDEX,this.getIndex(),
					  ATTR_METADATA_VALUE,value],selected) +" " +
		    HU.tag(TAG_LABEL,  [ATTR_CLASS,HU.classes('ramadda-noselect',CLASS_CLICKABLE),
					ATTR_FOR,cbxId],label +" (" + count+")");
		if(this.getValues().length>popupLimit) {
		    cbx = HU.span([ATTR_CLASS,CLASS_SEARCH_TAG,'tag',label], cbx);
		}
		cbxs.push(cbx);
		if(addNot) {
		    let cbxId = this.display.getMetadataFieldId(this.metadata.getType())+"_notcheckbox_" + this.getIndex()+"_"+i;
		    if(idToElementMap) idToElementMap[cbxId] = this;
		    let cbx = HU.checkbox("",[ATTR_ID,cbxId,
					      "metadata-not",true,
					      ATTR_METADATA_TYPE,type,
					      ATTR_METADATA_INDEX,this.getIndex(),
					      ATTR_METADATA_VALUE,value],selected) +" " +
			HU.tag(TAG_LABEL,
			       [ATTR_CLASS,HU.classes('ramadda-noselect',CLASS_CLICKABLE),
				ATTR_FOR,cbxId],
			       HU.b("Not ") + label +" (" + count+")");
		    if(this.getValues().length>popupLimit) {
			cbx = HU.span([ATTR_CLASS,CLASS_SEARCH_TAG,'tag',label], cbx);
		    }
		    cbxs.push(cbx);
		}


	    });
	    let selectAttrs = [ATTR_ID,this.selectId];
	    if(multiples) selectAttrs.push(ATTR_MULTIPLE,'true',ATTR_SIZE,4);
	    this.select = HU.tag(TAG_SELECT, selectAttrs,select);
	    return cbxs;

	}
    });
}

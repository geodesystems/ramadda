/**
   Copyright (c) 2008-2023 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/


var OUTPUT_JSON = "json";
var OUTPUT_CSV = "default.csv";
var OUTPUT_IDS = "default.ids";
var OUTPUT_ZIP = "zip.tree";
var OUTPUT_EXPORT = "zip.export";

var VALUE_ANY_TYPE="_any_";

var DEFAULT_MAX = 100;

var OUTPUTS = [
    {id: OUTPUT_IDS,name: "IDs"},
    {id: OUTPUT_CSV,name: "CSV"},
    {id: OUTPUT_JSON, name: "JSON"},
    {id: OUTPUT_ZIP, name: "Download Files"},
    {id: OUTPUT_EXPORT, name: "Export"},
];

//
//return the global entry manager with the given id, null if not found
//
function getRamadda(id) {

    if (!id) id = ramaddaBaseUrl;
    /*
      OpenSearch(http://asdasdsadsds);sdsadasdsa,...
     */

    //check for the embed label
    let toks = id.split(";");
    let name = null;
    if (toks.length > 1) {
        id = toks[0].trim();
        name = toks[1].trim();
    }

    let extraArgs = null;
    let regexp = new RegExp("^(.*)\\((.+)\\).*$");
    let args = regexp.exec(id);
    if (args) {
        id = args[1].trim();
        extraArgs = args[2];
    }

    if (id == "this") {
        return getGlobalRamadda();
    }

    if (window.globalRamaddas == null) {
        window.globalRamaddas = {};
    }
    let repo = window.globalRamaddas[id];
    if (repo != null) {
        return repo;
    }


    //See if its a js class
    let func = window[id];
    if (func == null) {
        func = window[id + "Repository"];
    }

    if (func) {
        repo = new Object();
        func.call(repo, name, extraArgs);
        //eval(" new " + id+"();");
    }


    if (repo == null) {
        repo = new RamaddaRepository(id);
        if (name != null) {
            repo.name = name;
        }
    }
    addRepository(repo);
    return repo;
}

function addRepository(repository) {
    if (window.globalRamaddas == null) {
        window.globalRamaddas = {};
    }
    window.globalRamaddas[repository.repositoryRoot] = repository;
}

function getGlobalRamadda() {
    return getRamadda(ramaddaBaseUrl);
}


function Repository(repositoryRoot) {
    //    console.log("root:" + repositoryRoot);
    let hostname = null;
    let match = repositoryRoot.match("^(http.?://[^/]+)/");
    if (match && match.length > 0) {
        hostname = match[1];
    } else {
        //        console.log("no match");
    }
    //    console.log("hostname:" + hostname);

    RamaddaUtil.defineMembers(this, {
        repositoryRoot: repositoryRoot,
        baseEntry: null,
        hostname: hostname,
        name: null,

	getId: function() {
	    return this.repositoryRoot;
	},
        getSearchMessage: function() {
            return "Searching " + this.getName();
        },
        getSearchLinks: function(searchSettings) {
            return null;
        },
        getSearchUrl: function(settings) {
            return null;
        },
        getId: function() {
            return this.repositoryRoot;
        },
        getIconUrl: function(entry) {
            return ramaddaCdn + "/icons/page.png";
        },
        getEntryTypes: function(callback) {
            return new Array();
        },
        getEntryType: function(typeId) {
            return null;
        },
        getMetadataCount: function(type, callback) {
            //                console.log("getMetatataCount:" + type.name);
            return 0;
        },
        getEntryUrl: function(entry, extraArgs) {
            return null;
        },

        getBaseEntry: function(callback) {
            if (this.baseEntry != null) {
                return this.baseEntry;
            }

        },
        getRoot: function() {
            return this.repositoryRoot;
        },
        getHostname: function() {
            return this.hostname;
        },
        canSearch: function() {
            return true;
        },
        getName: function() {
            if (this.children) {
                return "Search all repositories";
            }
            if (this.name != null) return this.name;
            if (this.repositoryRoot.indexOf("/") == 0) {
                return this.name = "This RAMADDA";
            }
            let url = this.repositoryRoot;
            //Do the a trick
            let parser = document.createElement('a');
            parser.href = url;
            let host = parser.hostname;
            let path = parser.pathname;
            //if its the default then just return the host;
            if (path == "/repository") return host;
            return this.name = host + ": " + path;
        },

    });


}


function RepositoryContainer(id, name) {
    this._id = "CONTAINER";
    let SUPER;
    RamaddaUtil.inherit(this, SUPER = new Repository(id));
    RamaddaUtil.defineMembers(this, {
        name: name,
        children: [],
        canSearch: function() {
            return false;
        },
        getSearchMessage: function() {
            return "Searching " + this.children.length + " repositories";
        },
        addRepository: function(repository) {
            this.children.push(repository);
        },
        getEntryTypes: function(callback) {
            if (this.entryTypes != null) {
                return this.entryTypes;
            }
            this.entryTypes = [];
            let seen = {};
            for (let i = 0; i < this.children.length; i++) {
                let types = this.children[i].getEntryTypes();
                if (types == null) continue;
                for (let j = 0; j < types.length; j++) {
                    let type = types[j];
                    if (seen[type.getId()] == null) {
                        let newType = {};
                        newType = $.extend(newType, type);
                        seen[type.getId()] = newType;
                        this.entryTypes.push(newType);
                    } else {
                        seen[type.getId()].entryCount += type.getEntryCount();
                    }
                }
            }
            return this.entryTypes;
        }

    });

}

function RamaddaRepository(repositoryRoot) {
    if (repositoryRoot == null) {
        repositoryRoot = ramaddaBaseUrl;
    }
    let SUPER;
    RamaddaUtil.inherit(this, SUPER = new Repository(repositoryRoot));

    RamaddaUtil.defineMembers(this, {
        entryCache: {},
        entryTypes: null,
        entryTypeMap: {},
        toString: function() {
	    return "ramadda:" + this.getId();
	},
        canSearch: function() {
            return true;
        },
        getJsonUrl: function(entryId) {
            return this.repositoryRoot + "/entry/show?entryid=" + entryId + "&output=json";
        },
	

        createEntriesFromJson: function(data) {
            let entries = new Array();
            for (let i = 0; i < data.length; i++) {
                let entryData = data[i];
                entryData.baseUrl = this.getRoot();
                let entry = new Entry(entryData);
                this.addEntry(entry);
                entries.push(entry);
            }
            return entries;
        },
        getEntryType: function(typeId) {
            return this.entryTypeMap[typeId];
        },
        entryTypeCallPending: false,
        entryTypeCallbacks: null,
        getEntryTypes: function(callback, types) {
            if (this.entryTypes != null) {
                return this.entryTypes;
            }
            if (this.entryTypeCallPending) {
                let callbacks = this.entryTypeCallbacks;
                if (callbacks == null) {
                    callbacks = [];
                }
                callbacks.push(callback);
                this.entryTypeCallbacks = callbacks;
                return this.entryTypes;
            }
            let theRamadda = this;
            let url = this.repositoryRoot + "/entry/types?forsearch=true";
	    if(types) url= url +"&types=" + types;
            this.entryTypeCallPending = true;
            this.entryTypeCallbacks = null;
            let jqxhr = $.getJSON(url, function(data) {
                if (GuiUtils.isJsonError(data)) {
                    return;
                }
                theRamadda.entryTypes = [];
                for (let i = 0; i < data.length; i++) {
                    let type = new EntryType(data[i]);
                    theRamadda.entryTypeMap[type.getId()] = type;
                    theRamadda.entryTypes.push(type);
                }
                if (callback != null) {
                    callback(theRamadda, theRamadda.entryTypes);
                }
                let callbacks = theRamadda.entryTypeCallbacks;
                theRamadda.entryTypeCallPending = false;
                theRamadda.entryTypeCallbacks = null;
                if (callbacks) {
                    //                            console.log("getEntryTypes - have extra callbacks");
                    for (let i = 0; i < callbacks.length; i++) {
                        callbacks[i](theRamadda, theRamadda.entryTypes);
                    }
                }
            }).done(function(jqxhr, textStatus, error) {
                theRamadda.entryTypeCallPending = false;
                //console.log("getEntryTypes.done:" +textStatus+ " error:" + error);
            }).always(function(jqxhr, textStatus, error) {
                theRamadda.entryTypeCallPending = false;
                //console.log("getEntryTypes.always:" +textStatus+ " error:" + error);
            }).fail(function(jqxhr, textStatus, error) {
                theRamadda.entryTypeCallPending = false;
                //console.log("getEntryTypes.fail:" +textStatus + " error:" + error);
                let err = "";
                if (error && error.length > 0) {
                    err += ":  " + error;
                }
                GuiUtils.handleError("An error has occurred reading entry types" + err, "URL: " + url, false);
            });

            return this.entryTypes;
        },
        metadataCache: {},
        metadataCachePending: {},
        metadataCacheCallbacks: {},

        getMetadataCount: function(type, callback) {
            let key = type.getType();
            let data = this.metadataCache[key];
            if (data != null) {
                //                    console.log("getMetadata:" + type.getType() + " was in cache");
                callback(type, data);
                return null;
            }

            let pending = this.metadataCachePending[key];
            if (pending) {
                let callbacks = this.metadataCacheCallbacks[key];
                if (callbacks == null) {
                    callbacks = [];
                }
                callbacks.push(callback);
                this.metadataCacheCallbacks[key] = callbacks;
                return null;
            }
            this.metadataCacheCallbacks[key] = null;
            this.metadataCachePending[key] = true;

            let url = this.repositoryRoot + "/metadata/list?metadata_type=" + type.getType() + "&response=json";
//            console.log("getMetadata:" + type.getType() + " URL:" + url);
            let _this = this;
            let jqxhr = $.getJSON(url, function(data) {
                let callbacks = _this.metadataCacheCallbacks[key];
                _this.metadataCachePending[key] = false;
                if (GuiUtils.isJsonError(data)) {
                    return;
                }
                //                        console.log("getMetadata:" + type.getType() +" caching " );
                _this.metadataCache[key] = data;
                callback(type, data);
                if (callbacks) {
                    //                            console.log("getMetadata: have callbacks");
                    for (let i = 0; i < callbacks.length; i++) {
                        callbacks[i](type, data);
                    }
                }
            })
                .fail(function(jqxhr, textStatus, error) {
                    _this.metadataCachePending[key] = false;
                    let err = textStatus + ", " + error;
                    GuiUtils.handleError("Error getting metadata count: " + err, url, false);
                });
            return null;
        },
        getEntryUrl: function(entry, extraArgs) {
            let id;
            if ((typeof entry) == "string") id = entry;
            else id = entry.id;
            let url = this.getRoot() + "/entry/show?entryid=" + id;
            if (extraArgs != null) {
                if (!StringUtil.startsWith(extraArgs, "&")) {
                    url += "&";
                }
                url += extraArgs;
            }
            return url;
        },
        getEntryDownloadUrl: function(entry, extraArgs) {
            let id;
            if ((typeof entry) == "string") id = entry;
            else id = entry.id;
	    //Check for a URL
	    if(id.startsWith('http')) {
		//If the remote URL us ramadda.org then use the proxy so we don't have the SAME_ORIGIN problem
		if(id.indexOf('ramadda.org')>=0)
		    return  Ramadda.getUrl('/proxy?url=' + encodeURIComponent(id));
		return id;
	    }
            let url = this.getRoot() + "/entry/get?entryid=" + id;
            if (extraArgs != null) {
                if (!StringUtil.startsWith(extraArgs, "&")) {
                    url += "&";
                }
                url += extraArgs;
            }
            return url;
        },

        getSearchLinks: function(searchSettings,makeSpan,check) {
            let urls = [];
            for (let i = 0; i < OUTPUTS.length; i++) {
		let output = OUTPUTS[i];
		if(check && !check(output)) continue;
		if(makeSpan) {
                    urls.push(HtmlUtils.span([ATTR_CLASS,'ramadda-search-link ramadda-clickable',
					      ATTR_TITLE,'Click to download; shift-click to copy URL',
					      'data-name',output.name,
					      'data-format',output.id,
					      'data-url',
					      this.getSearchUrl(searchSettings, output.id)],
					     output.name));
		} else {
                    urls.push(HtmlUtils.href(this.getSearchUrl(searchSettings, output.id),
					     output.name,[ATTR_CLASS,'ramadda-search-link']));
		}
            }
            return urls;
        },
        getUrlRoot: function() {
            return this.repositoryRoot;
        },

        getSearchUrl: function(settings, output, bar) {
            let url = this.repositoryRoot + "/search/do?forsearch=true";
	    if(output) url+="&output=" + output;
            for (let i = 0; i < settings.types.length; i++) {
                let type = settings.types[i];
		if(type!=VALUE_ANY_TYPE) {
                    url += "&type=" + type;
		}
            }

            if (settings.parent != null && settings.parent.length > 0)
                url += "&group=" + settings.parent;
	    if(settings.providers) {
		settings.providers.forEach(provider=>{
                    url += "&provider=" + provider;
		});
	    }
	    let addAttr=(name,value) =>{
		if(Utils.stringDefined(value))
		    url += "&" + name+"=" + value;
	    }
	    addAttr("text", settings.text);
	    addAttr("name", settings.name);
	    addAttr("description", settings.description);	    
	    addAttr("datadate.from",settings.startDate);
	    addAttr("datadate.to",settings.endDate);
	    addAttr("createdate.from",settings.createstartDate);
	    addAttr("createdate.to",settings.createendDate);	    
            if (settings.entries && settings.entries.length > 0) {
                url += "&entries=" + settings.entries;
            }
            if (settings.orderBy) {
                url += "&orderby=" + settings.orderBy;
		if(Utils.isDefined(settings.ascending)) 
		    url +="&ascending=" + (settings.ascending?"true":"false");
            }
            if (settings.getAreaContains()) {
                url += "&areamode=contains";
            }
            if (!isNaN(settings.getNorth()))
                url += "&maxlatitude=" + settings.getNorth();
            if (!isNaN(settings.getWest()))
                url += "&minlongitude=" + settings.getWest();
            if (!isNaN(settings.getSouth()))
                url += "&minlatitude=" + settings.getSouth();
            if (!isNaN(settings.getEast()))
                url += "&maxlongitude=" + settings.getEast();

	    if(settings.ancestor) 
		url += "&ancestor=" + encodeURIComponent(settings.ancestor);

            for (let i = 0; i < settings.metadata.length; i++) {
                let metadata = settings.metadata[i];
		let index = metadata.index;
		if(!Utils.isDefined(index)) index=1;
                url += "&metadata_attr" + index+"_" + metadata.type + "=" + encodeURIComponent(metadata.value);
            }
            url += "&max=" + settings.getMax();
            url += "&skip=" + settings.getSkip();
	    if(settings.rootEntry)
		url += "&rootEntry=" + encodeURIComponent(settings.rootEntry);
            url += settings.getExtra();
            return url;
        },

	isLocal() {
	    return this.repositoryRoot && this.repositoryRoot.startsWith('/');
	},
        addEntry: function(entry) {
            this.entryCache[entry.getId()] = entry;
        },
        getEntry: async function(id, callback) {
//	    console.log("getEntry");
	    let debug = false;
	    if(id == null) {
		console.log("Error in getEntry: entry id is null");
		console.trace();
		return null;
	    }
            let entry = this.entryCache[id];
            if (entry != null) {
		if(debug) console.log("getEntry:" + entry.getName());
                return Utils.call(callback, entry);
            }
            //Check any others
            if (window.globalRamaddas) {
                for (let i = 0; i < window.globalRamaddas.length; i++) {
                    let em = window.globalRamaddas[i];
                    let entry = em.entryCache[id];
                    if (entry != null) {
                        return Utils.call(callback, entry);
                    }
                }
            }
            if (callback == null) {
                return null;
            }
            let ramadda = this;
            let jsonUrl = this.getJsonUrl(id) + "&ancestors=true";
            if(debug)console.log("ramadda.getEntry:" + id +"  getting json");
            await $.getJSON(jsonUrl, function(data) {
                if (GuiUtils.isJsonError(data)) {
                    if(debug) console.log("\tramadda.getEntry json error:" + data);
                    return;
                }
                let entryList = createEntriesFromJson(data, ramadda);
                let first = null;
                if (entryList.length > 0) first = entryList[0];
                if(debug) console.log("\tramadda.getEntry: result:" + entryList.length +" " + first);
                Utils.call(callback, first, entryList);
            })
                .fail(function(jqxhr, textStatus, error) {
                    let err = textStatus + ", " + error;
                    GuiUtils.handleError("Error getting entry information: " + err, jsonUrl, false);
                });
        }
    });


    //    this.getEntryTypes();
}


/**
This creates a list of Entry objects from the given JSON data. 
If the given ramadda is null then use the global
*/
function createEntriesFromJson(data, ramadda) {
    if (ramadda == null) {
        ramadda = getGlobalRamadda();
    }
    return ramadda.createEntriesFromJson(data);
}


var metadataTypeCount = -1;
var metadataColorPalette = ["#FDF5E6", "#F0FFFF","#FFE3D5","#a7d0cd","#fbeeac","#dbe3e5","#e8e9a1"];
var metadataColors = {};

function getMetadataColor(type) {
    if(type.color) return type.color;
    if(metadataColors[type]) return metadataColors[type];
    return metadataColorPalette[0];
}

function MetadataType(type, label, value) {
    $.extend(this, {
        type: type,
        label: label,
        value: value
    });
    this.color = Utils.getEnumColor(type);
    $.extend(this, {
        getType: function() {
            return this.type;
        },
	getColor: function() {
	    return this.color;
	},
        getAddNot: function() {
	    return this.addNot;
	},

        getLabel: function() {
            if (this.label != null) return this.label;
            return this.type;
        },
        getSearchLabel: function() {
            if (this.searchLabel != null) return this.searchLabel;
	    return this.getLabel();
        },	
        getValue: function() {
            return this.value;
        },
    });
}


function EntryTypeColumn(props) {
    $.extend(this, props);
    RamaddaUtil.defineMembers(this, {
        getName: function() {
            return this.name;
        },
        getLabel: function() {
            return this.label;
        },
        getGroup: function() {
            return this.group;
        },	
        getSearchLabel: function() {
            if (this.searchLabel != null) return this.searchLabel;
	    return this.getLabel();
        },	
        getType: function() {
            return this.type;
        },
        getValues: function() {
            return this.values;
        },
        getSuffix: function() {
            return this.suffix;
        },
        getSearchArg: function() {
            return "search." + this.namespace + "." + this.name;
        },
        getCanSearch: function() {
            return this.cansearch;
        },
        getCanShow: function() {
            return this.canshow;
        },
        isEnumeration: function() {
            return this.getType() == "enumeration" || this.getType() == "enumerationplus";
        },
        isLatLon: function() {
            return this.getType() == "latlon";
        },
        isDate: function() {
            return this.getType() == "date";
        },		
        showCheckboxes: function() {
	    return this.searchShowCheckboxes;
	},
        getSearchMultiples: function() {
	    return this.searchMultiples;
	},	
        isNumeric: function() {
            return this.getType() == "double" || this.getType() == "int";
        },	
        isUrl: function() {
            return this.getType() == "url"
        },
    });
}

function EntryType(props) {
    //Make the Columns
    let columns = props.columns;
    if (columns == null) columns = [];
    let myColumns = [];
    for (let i = 0; i < columns.length; i++) {
        myColumns.push(new EntryTypeColumn(columns[i]));
    }
    props.columns = myColumns;
    $.extend(this, props);

    RamaddaUtil.defineMembers(this, {
        getIsGroup: function() {
            return this.isgroup;
        },
        getIcon: function() {
            return this.icon;
        },
        getLabel: function() {
            return this.label || this.name;
        },
        getId: function() {
            if (this.type != null) return this.type;
            return this.id;
        },
        getCategory: function() {
            return this.category;
        },
        getEntryCount: function() {
            return this.entryCount;
        },
        getIncludeInSearch: function() {
            return this.includeInSearch;
        },	
        getColumns: function() {
            return this.columns;
        },
    });
}


var ENTRY_NONGEO = -9999;


//class:Entry
function Entry(props) {
    if (props.repositoryId == null) {
        props.repositoryId = props.baseUrl;
    }
    if (props.repositoryId == null) {
        props.repositoryId = ramaddaBaseUrl;
    }

    let NONGEO = ENTRY_NONGEO;
    if (props.typeObject) {
        props.type = new EntryType(props.typeObject);
    } else if (props.type) {
        let obj = {
            "id": props.type,
            "name": props.typeName != null ? props.typeName : props.type
        };
        props.type = new EntryType(obj);
    }
    $.extend(this, {
        id: null,
        name: null,
        description: null,
        bbox: null,
        geometry: null,
        services: [],
        properties: [],
        childrenEntries: null,
        startDate: null,
        endDate: null,
	embedWikiText:null
    });
    $.extend(this, props);
    this.domId = Utils.cleanId(this.id);

    this.createDate = Utils.parseDate(props.createDate);
    this.startDate = Utils.parseDate(props.startDate);
    this.endDate = Utils.parseDate(props.endDate);
    if (this.endDate && this.startDate) {
        if (this.endDate.getTime() < this.startDate.getTime()) {
            let tmp = this.startDate;
            this.startDate = this.endDate;
            this.endDate = tmp;
        }
    }
    //    console.log(props.name +" dttm:" + this.getEndDate());
    this.attributes = [];
    this.metadata = [];
    for (let i = 0; i < this.properties.length; i++) {
        let prop = this.properties[i];
        if (prop.type == "attribute") {
            this.attributes.push(prop);
        } else {
            this.metadata.push(prop);
        }
    }


    RamaddaUtil.defineMembers(this, {
        getId: function() {
            return this.id;
        },
        getIdForDom: function() {
            return this.domId;
        },
	canEdit:function() {
	    return this.canedit;
	},
	isSynth: function() {
	    return this.id.startsWith("synth:");
	},
        getFullId: function() {
            return this.getRamadda().getRoot() + "," + this.id;
        },
	//Note: this does not set this entry object's values
	doSave: function(authtoken,args, success,error) {
	    RamaddaUtil.doSave(this.getId(),authtoken,args, success,error);
	},
        getDisplayName: function(addSlug) {
            if (this.displayName) return this.displayName;
            return this.getName(addSlug);
        },
        getCreateDate: function() {
            return this.createCreate;
        },
        getStartDate: function() {
            return this.startDate;
        },
        getEndDate: function() {
            return this.endDate;
        },
        getIsGroup: function() {
            return this.isGroup;
        },
        getProperty: function(what,props,inlineEdit) {
	    props = props??{};
	    if(what=="name") {
		if(this.canEdit() && inlineEdit) {
		    return HU.input(null,this.getName(),['size','20',
							 'entryid',this.getId(),
							 'title','Edit name',
							 'class','ramadda-entry-inlineedit','data-field','name']);
		}
		return this.getName();
	    }
	    if(what=="fromdate") {
		return HU.span(['class','ramadda-datetime','title',this.startDate],this.startDateFormat);
	    }
	    
	    if(what=="time") {
		return HU.span(['class','ramadda-datetime','title',this.startDate],this.hhmm);
	    }

	    if(what=="download") {
		if(!this.getIsFile()) return  "";
		let url = this.getResourceUrl();
		let label = HU.getIconImage('fas fa-download');
		if(this.getFilesize()) label+=" " + this.getFormattedFilesize();
		return   HU.href(url,label);
	    }
	    if(what=="entryorder") {
		if(!this.canEdit() || !inlineEdit) {
		    return this.order;
		}
		return HU.input(null,this.order,['size','3','entryid',this.getId(),'title','Edit order','class','ramadda-entry-inlineedit ramadda-entry-inlineedit-entryorder','data-field','entryorder']);
	    }
	    if(what=="creator") {
		let searchUrl = RamaddaUtil.getUrl('/search/do?user_id='+ this.creator+'&search.submit=true');
		let created = HU.href(searchUrl,
				      Utils.stringDefined(this.creatorName)?this.creatorName:this.creator,
				      [ATTR_TITLE,'Search for entries of this type created by this user']);
		return created;
	    }
	    
	    if(what=="latitude") {
		return this.getLatitude();
	    }		
	    if(what=="longitude") {
		return this.getLongitude();
	    }

	    if(what=="altitude") {
		return this.getAltitude();
	    }	    
	    if(what=="createdate") return HU.span(['class','ramadda-datetime','title',this.createDate],this.createDateFormat);
	    if(what=="changedate") return HU.span(['class','ramadda-datetime','title',this.changeDate],this.changeDateFormat);
	    if(what=="size") {
		return this.getFilesize()?this.getFormattedFilesize():"---";
	    }
	    if(what=="type") return this.typeName;
	    return "Unknown:" + what;
	},

	getTypeName:function() {
	    return this.typeName;
	},
        getRoot: async function(callback, extraArgs) {
            let parent = this;
            while (true) {
                let tmp;
                await parent.getParentEntry(e => tmp = e);
                if (!tmp) {
                    return Utils.call(callback, parent);
                }
                parent = tmp;
            }
        },
        map: async function(func, finish) {
            let children;
            await this.getChildrenEntries(l => {
                children = l
            });
            children.map(func);
            Utils.call(finish);
        },
	getEmbedWikiText:function() {
	    return this.embedWikiText;
	},
        getParentName: function() {
	    return this.parentName;
	},
        getParentEntry: async function(callback, extraArgs) {
            if (!this.parent) {
                //                    console.log("\tgetParent: no parent");
                return Utils.call(callback, null);
            }
            if (this.parentEntry) {
                //                    console.log("\tgetParent: got it");
                return Utils.call(callback, this.parentEntry);
            }
            await this.getRamadda().getEntry(this.remoteParent||this.parent, entry => {
                this.parentEntry = entry;
                Utils.call(callback, entry);
            });
        },
        getChildrenEntries: async function(callback, extraArgs) {
            if (this.childrenEntries != null) {
                return Utils.call(callback, this.childrenEntries);
            }
            let settings = new EntrySearchSettings({
                parent: this.getId()
            });
            let jsonUrl = this.getRamadda().getJsonUrl(this.getAbsoluteId()) + "&justchildren=true";
            if (extraArgs != null) {
                jsonUrl += "&" + extraArgs;
            }
            //                console.log(jsonUrl);
            let myCallback = {
                entryListChanged: function(list) {
                    callback(list.getEntries());
                }
            };
            let entryList = new EntryList(this.getRamadda(), jsonUrl, myCallback, false);
            await entryList.doSearch();
        },
        getType: function() {
            if (this.typeObject != null) {}
            return this.type;
        },
        getThumbnail: function() {
            if (!this.metadata) return null;
	    let okMetadata;
	    let getUrl=metadata=>{
		if(metadata.value.attr1.startsWith("http")) return metadata.value.attr1;
		let url = this.getRamadda().getRoot() + "/metadata/view/" + metadata.value.attr1 + "?element=1&entryid=" + this.getAbsoluteId() + "&metadata_id=" + metadata.id;
		return url;
	    }
            for (let i = 0; i < this.metadata.length; i++) {
                let metadata = this.metadata[i];
                if (metadata.type == "content.thumbnail" && Utils.stringDefined(metadata.value.attr1)) {
		    okMetadata=metadata;
		    //Check for primary thumbnail
		    if(metadata.attr3=='true') 
			return getUrl(metadata);
		}
            }
	    if(okMetadata)
		return getUrl(okMetadata);
            return null;
        },

        getMetadata: function() {
            return this.metadata;
        },
        getRamadda: function() {
	    if(this.remoteRepository)
		return getRamadda(this.remoteRepository.url);
            return getRamadda(this.repositoryId);
        },
	checkGeo:function(v) {
	    if(v==ENTRY_NONGEO) return NaN;
	    return v;
	},
        getLocationLabel: function() {
            return "n: " + this.getNorth() + " w:" + this.getWest() + " s:" + this.getSouth() + " e:" + this.getEast();
        },
        getServices: function() {
            return this.services;
        },
        getService: function(relType) {
            for (let i = 0; i < this.services.length; i++) {
                if (this.services[i].relType == relType) return this.services[i];
            }
            return null;
        },
        goodLoc: function(v) {
            return !isNaN(v) && v != NONGEO;
        },
        hasBounds: function() {
            return this.goodLoc(this.getNorth()) && this.goodLoc(this.getWest()) && this.goodLoc(this.getSouth()) && this.goodLoc(this.getEast());
        },
        hasLocation: function() {
            return this.goodLoc(this.getNorth());
        },
        getNorth: function() {
            if (this.bbox) {
                return this.checkGeo(this.bbox[3]);
            }
            if (this.geometry) {
                return this.checkGeo(this.geometry.coordinates[1]);
            }
            return NaN;
        },
        getWest: function() {
            if (this.bbox) return this.checkGeo(this.bbox[0]);
            if (this.geometry) {
                return this.checkGeo(this.geometry.coordinates[0]);
            }
            return NaN;
        },
        getSouth: function() {
            if (this.bbox) return this.checkGeo(this.bbox[1]);
            if (this.geometry) {
                return this.checkGeo(this.geometry.coordinates[1]);
            }
            return NaN;
        },
        getEast: function() {
            if (this.bbox) return this.checkGeo(this.bbox[2]);
            if (this.geometry) {
                return this.checkGeo(this.geometry.coordinates[0]);
            }
            return NaN;
        },
        getAltitude: function() {
	    if(Utils.isDefined(this.altitudeTop)) return this.altitudeTop;
	    if(Utils.isDefined(this.altitudeBottom)) return this.altitudeBottom;	    
	    return NaN;
	},

        getLatitude: function() {
            if (this.geometry) {
                return this.checkGeo(this.geometry.coordinates[1]);
            }
            return this.getNorth();
        },
        getLongitude: function() {
            if (this.geometry) {
                return this.checkGeo(this.geometry.coordinates[0]);
            }
            return this.getWest();
        },
        getIconUrl: function() {
            if (this.icon == null) {
                return this.getRamadda().getIconUrl(this);
            }
            if (this.icon.match("^http.*")) {
                return this.icon;
            }

            let url;
            let hostname = this.getRamadda().getHostname();
            if (hostname)
                url = hostname + this.icon;
            else
                url = this.icon;
            //this.getRamadda().getRoot() + 
            return url;
        },
        getIconImage: function(attrs) {
	    attrs = attrs??[];
	    if(!attrs.includes("width"))attrs.push("width",ramaddaGlobals.iconWidth);
	    if(this.iconRelative)
		return HtmlUtils.image(this.iconRelative, attrs);
            return HtmlUtils.image(this.getIconUrl(), attrs);
        },
        getColumns: function() {
            if (this.type.getColumns() == null) {
                return new Array();
            }
            return this.type.getColumns();
        },
        getAttributes: function() {
            return this.attributes;
        },
        getAttribute: function(name) {
            for (let i = 0; i < this.attributes.length; i++) {
                let attr = this.attributes[i];
                if (attr.id == name) {
                    return attr;
                }
            }
            return null;
        },
        getAttributeValue: function(name) {
            let attr = this.getAttribute(name);
            if (attr == null) return null;
            return attr.value;
        },
        getAttributeNames: function() {
            let names = [];
            for (let i = 0; i < this.attributes.length; i++) {
                let attr = this.attributes[i];
                names.push(attr.id);
            }
            return names;
        },
        getAttributeLabels: function() {
            let labels = [];
            for (let i = 0; i < this.attributes.length; i++) {
                let attr = this.attributes[i];
                if (attr.label) {
                    labels.push(attr.label);
                } else {
                    labels.push(attr.id);
                }
            }
            return labels;
        },

        getName: function(addSlug) {
	    let n 
            if (this.name == null || this.name == "") {
                n=  'no name';
            } else {
		n = this.name;
	    }
	    if(addSlug && Utils.stringDefined(this.repositorySlug)) {
		if(!this.getRamadda().isLocal()) {
		    n=this.repositorySlug+'-'+n;
		}
	    }
	    return n;
        },
        getSnippet: function(dflt) {
            if (this.snippet == null) return dflt;
            return this.snippet;
        },
        getDescription: function(dflt) {
            if (this.description == null) return dflt;
            return this.description;
        },
        getFilesize: function() {
            let size = parseInt(this.filesize);
            if (size == size) return size;
            return 0;
        },
        getFormattedFilesize: function() {
            return GuiUtils.size_format(this.getFilesize());
        },
        getEntryUrl: function(extraArgs) {
            if (this.remoteUrl) return this.remoteUrl;
            //Don't do this as we really want the url to the entry
            //if(this.url) return this.url;
            return this.getRamadda().getEntryUrl(this, extraArgs);
        },
        getFilename: function() {
            return this.filename;
        },
        isImage: function() {
            if (this.url && this.url.search(/(\.png|\.jpg|\.jpeg|\.gif)/i) >= 0) {
                return true;
            }
            if(this.hasResource() && this.getFilename().search(/(\.png|\.jpg|\.jpeg|\.gif)/i) >= 0) return true;
	    if(this.getThumbnail()) return true;
	    return false;
        },
        getImageUrl: function() {
            if (this.url && this.url.search(/(\.png|\.jpg|\.jpeg|\.gif)/i) >= 0) {
                return this.url;
            }
            if(this.hasResource() && this.getFilename().search(/(\.png|\.jpg|\.jpeg|\.gif)/i) >= 0) {
		return this.getResourceUrl();
	    }
	    let thumbnail = this.getThumbnail();
	    if(thumbnail) {
		return thumbnail;
	    }
	    return null;
	},
        getIsUrl: function() {
	    return this.isurl;
	},
        getIsFile: function() {
	    return this.isfile;
	},	
        hasResource: function() {
            return this.getFilename() != null;
        },
	getAbsoluteId: function() {
	    if(this.remoteRepository  && this.remoteUrl) {
		let match = this.remoteUrl.match("entryid=(.*)");
		if(match && match.length>1)
		    return match[1];
	    }
	    return this.id;
	},
        getResourceUrl: function() {
            if (this.url) {
                return this.url;
            }
	    if(this.remoteRepository) {
		return this.remoteRepository.url + "/entry/get?entryid=" + this.getAbsoluteId();
	    }
            let rurl = this.getRamadda().getRoot() + "/entry/get";
            if (this.getFilename() != null) {
                rurl += "/" + this.getFilename();
            }
            return rurl + "?entryid=" + this.id;
        },
        getLink: function(label, includeIcon, attrs) {
            if (!label) label = this.getName();
	    attrs = attrs ||[];
	    attrs.push("href", this.getEntryUrl());
	    if(includeIcon)
		label  = this.getIconImage() + SPACE +label;
            return HtmlUtils.tag("a", attrs, label);
        },
        getResourceLink: function(label) {
            if (!label) label = this.getName();
            return HtmlUtils.tag("a", ["href", this.getResourceUrl()], label);
        },
        toString: function() {
            return "entry:" + this.getName()+" id:" + this.getId();
        }
    });
}


function EntryList(repository, jsonUrl, listener, doSearch) {
    $.extend(this, {
        repository: repository,
        url: jsonUrl,
        listener: listener,
        haveLoaded: false,
        entries: [],
        map: {},
        getRepository: function() {
            return this.repository;
        },
        setEntries: function(entries) {
            this.entries = entries;
            this.haveLoaded = true;
        },
        getEntry: async function(id, callback) {
            let entry = this.map[id];
            if (entry != null) return Utils.call(callback, entry);
            await this.getRepository().getEntry(id, e => {
                Utils.call(callback, e)
            });
        },
        getEntries: function() {
            return this.entries;
        },
        createEntries: function(data, listener, success) {
            this.entries = createEntriesFromJson(data, this.getRepository());
            for (let i = 0; i < this.entries.length; i++) {
                let entry = this.entries[i];
                this.map[entry.getId()] = entry;
            }
            if (listener == null) {
                listener = this.listener;
            }
	    if(success) {
		success(this);
	    } else if (listener && listener.entryListChanged) {
                listener.entryListChanged(this);
            }
        },
        doSearch: async function(listener, success, fail) {
            if (listener == null) {
                listener = this.listener;
            }
            let _this = this;
//            console.log("search url:" + this.url);
            await $.getJSON(this.url, function(data, status, jqxhr) {
                if (GuiUtils.isJsonError(data)) {
                    return;
                }
                _this.haveLoaded = true;
		//console.log("search done. creating entries");
                _this.createEntries(data, listener, success);

            })
                .fail(function(jqxhr, textStatus, error) {
                    GuiUtils.handleError("An error occurred doing search: " + error, _this.url, true);
		    if(fail) {
			fail(error);
		    }   else if (listener && listener.handleSearchError) {
                        listener.handleSearchError(_this.url, error);
                    }
                });
        }
    });

    if (doSearch) {
        this.doSearch();
    }
}




function EntrySearchSettings(props) {
    $.extend(this, {
        types: [],
        parent: null,
        max: DEFAULT_MAX,
        skip: 0,
        metadata: [],
        extra: "",
        orderBy: "",
        entries: null,
        startDate: null,
        endDate: null,
        north: NaN,
        west: NaN,
        north: NaN,
        east: NaN,
        areaContains: false,
	clearProviders:function(provider) {
	    this.providers=null;
	},
	getProvider:function(provider) {
	    if(!this.providers) return null;
	    return this.providers[0];
	},


	setProvider:function(provider) {
	    if(provider==null) {
		this.providers=null;
		return
	    }
	    if(!Array.isArray(provider)) provider =[provider];
	    this.providers = provider;
	},
        getMax: function() {
            return this.max;
        },
	setMax: function(max) {
            this.max = parseInt(max);
        },	
        getSkip: function() {
            return this.skip;
        },
        toString: function() {
            return "n:" + this.north + " w:" + this.west + " s:" + this.south + " e:" + this.east;
        },
        getAreaContains: function() {
            return this.areaContains;
        },
        setAreaContains: function(contains) {
            this.areaContains = contains;
        },
        getNorth: function() {
            return this.north;
        },
        getSouth: function() {
            return this.south;
        },
        getWest: function() {
            return this.west;
        },
        getEast: function() {
            return this.east;
        },
        setDateRange: function(start, end) {
            this.startDate = start;
            this.endDate = end;
        },
        setCreateDateRange: function(start, end) {
            this.createstartDate = start;
            this.createendDate = end;
        },	

        setBounds: function(north, west, south, east) {
            this.north = (north == null || north.toString().length == 0 ? NaN : parseFloat(north));
            this.west = (west == null || west.toString().length == 0 ? NaN : parseFloat(west));
            this.south = (south == null || south.toString().length == 0 ? NaN : parseFloat(south));
            this.east = (east == null || east.toString().length == 0 ? NaN : parseFloat(east));
        },
        getTypes: function() {
            return this.types;
        },
        hasType: function(type) {
            return this.types.indexOf(type) >= 0;
        },
        getExtra: function() {
            return this.extra;
        },
        setExtra: function(extra) {
            this.extra = extra;
        },
        clearAndAddType: function(type) {
            this.types = [];
            this.addType(type);
            return this;
        },
        addType: function(type) {
            if (type == null || type.length == 0) return;
            if (this.hasType(type)) return;
            this.types.push(type);
            return this;
        }
    });
    if (props != null) {
        $.extend(this, props);
    }
}


function EntryListHolder(ramadda) {
    let SUPER;
    RamaddaUtil.inherit(this, SUPER = new EntryList(ramadda, null));

    $.extend(this, {
        entryLists: [],
        addEntryList: function(e) {
            this.entryLists.push(e);
        },
        doSearch: function(listener) {
            let _this = this;
            if (listener == null) {
                listener = this.listener;
            }
            for (let i = 0; i < this.entryLists.length; i++) {
                let entryList = this.entryLists[i];
                if (!entryList.getRepository().canSearch()) continue;
                let callback = {
                    entryListChanged: function(entryList) {
                        if (listener) {
                            listener.entryListChanged(_this, entryList);
                        }
                    }
                };
                entryList.doSearch(callback);
            }
        },
        getEntry: async function(id) {
            for (let i = 0; i < this.entryLists.length; i++) {
                let entryList = this.entryLists[i];
                let entry;
                await entryList.getEntry(id, e => {
                    entry = e
                });
                if (entry != null) {
                    return entry;
                }
            }
            return null;
        },
        getEntries: function() {
            let entries = [];
            for (let i = 0; i < this.entryLists.length; i++) {
                let sub = this.entryLists[i].getEntries();
                for (let j = 0; j < sub.length; j++) {
                    entries.push(sub[j]);
                }
            }
            return entries;
        },
    });

}



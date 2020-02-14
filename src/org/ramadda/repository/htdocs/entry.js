/*
 * Copyright (c) 2008-2019 Geode Systems LLC
 */

var OUTPUT_JSON = "json";
var OUTPUT_CSV = "default.csv";
var OUTPUT_ZIP = "zip.tree";
var OUTPUT_EXPORT = "zip.export";

var OUTPUTS = [{
    id: OUTPUT_ZIP,
    name: "Download Zip"
}, {
    id: OUTPUT_EXPORT,
    name: "Export"
}, {
    id: OUTPUT_JSON,
    name: "JSON"
}, {
    id: OUTPUT_CSV,
    name: "CSV"
}, ];

//
//return the global entry manager with the given id, null if not found
//
function getRamadda(id) {

    if (!id) id = ramaddaBaseUrl;
    /*
      OpenSearch(http://asdasdsadsds);sdsadasdsa,...
     */

    //check for the embed label
    var toks = id.split(";");
    var name = null;
    if (toks.length > 1) {
        id = toks[0].trim();
        name = toks[1].trim();
    }

    var extraArgs = null;
    var regexp = new RegExp("^(.*)\\((.+)\\).*$");
    var args = regexp.exec(id);
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
    var repo = window.globalRamaddas[id];
    if (repo != null) {
        return repo;
    }


    //See if its a js class
    var func = window[id];
    if (func == null) {
        func = window[id + "Repository"];
    }

    if (func) {
        repo = new Object();
        func.call(repo, name, extraArgs);
        //eval(" new " + id+"();");
    }


    if (repo == null) {
        repo = new Ramadda(id);
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
    var hostname = null;
    var match = repositoryRoot.match("^(http.?://[^/]+)/");
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
            return ramaddaBaseUrl + "/icons/page.png";
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
            var url = this.repositoryRoot;
            //Do the a trick
            var parser = document.createElement('a');
            parser.href = url;
            var host = parser.hostname;
            var path = parser.pathname;
            //if its the default then just return the host;
            if (path == "/repository") return host;
            return this.name = host + ": " + path;
        },

    });


}


function RepositoryContainer(id, name) {
    this._id = "CONTAINER";
    var SUPER;
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
            var seen = {};
            for (var i = 0; i < this.children.length; i++) {
                var types = this.children[i].getEntryTypes();
                if (types == null) continue;
                for (var j = 0; j < types.length; j++) {
                    var type = types[j];
                    if (seen[type.getId()] == null) {
                        var newType = {};
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

function Ramadda(repositoryRoot) {
    if (repositoryRoot == null) {
        repositoryRoot = ramaddaBaseUrl;
    }
    var SUPER;
    RamaddaUtil.inherit(this, SUPER = new Repository(repositoryRoot));

    RamaddaUtil.defineMembers(this, {
        entryCache: {},
        entryTypes: null,
        entryTypeMap: {},
        canSearch: function() {
            return true;
        },
        getJsonUrl: function(entryId) {
            return this.repositoryRoot + "/entry/show?entryid=" + entryId + "&output=json";
        },
        createEntriesFromJson: function(data) {
            var entries = new Array();
            for (var i = 0; i < data.length; i++) {
                var entryData = data[i];
                entryData.baseUrl = this.getRoot();
                var entry = new Entry(entryData);
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
        getEntryTypes: function(callback) {
            if (this.entryTypes != null) {
                return this.entryTypes;
            }
            if (this.entryTypeCallPending) {
                var callbacks = this.entryTypeCallbacks;
                if (callbacks == null) {
                    callbacks = [];
                }
                callbacks.push(callback);
                this.entryTypeCallbacks = callbacks;
                return this.entryTypes;
            }
            var theRamadda = this;
            var url = this.repositoryRoot + "/entry/types";
            this.entryTypeCallPending = true;
            this.entryTypeCallbacks = null;
            var jqxhr = $.getJSON(url, function(data) {
                if (GuiUtils.isJsonError(data)) {
                    return;
                }
                theRamadda.entryTypes = [];
                for (var i = 0; i < data.length; i++) {
                    var type = new EntryType(data[i]);
                    theRamadda.entryTypeMap[type.getId()] = type;
                    theRamadda.entryTypes.push(type);
                }
                if (callback != null) {
                    callback(theRamadda, theRamadda.entryTypes);
                }
                var callbacks = theRamadda.entryTypeCallbacks;
                theRamadda.entryTypeCallPending = false;
                theRamadda.entryTypeCallbacks = null;
                if (callbacks) {
                    //                            console.log("getEntryTypes - have extra callbacks");
                    for (var i = 0; i < callbacks.length; i++) {
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
                var err = "";
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
            var key = type.getType();
            var data = this.metadataCache[key];
            if (data != null) {
                //                    console.log("getMetadata:" + type.getType() + " was in cache");
                callback(type, data);
                return null;
            }


            var pending = this.metadataCachePending[key];
            if (pending) {
                var callbacks = this.metadataCacheCallbacks[key];
                if (callbacks == null) {
                    callbacks = [];
                }
                callbacks.push(callback);
                this.metadataCacheCallbacks[key] = callbacks;
                return null;
            }
            this.metadataCacheCallbacks[key] = null;
            this.metadataCachePending[key] = true;

            var url = this.repositoryRoot + "/metadata/list?metadata_type=" + type.getType() + "&response=json";
            //                console.log("getMetadata:" + type.getType() + " URL:" + url);
            var _this = this;
            var jqxhr = $.getJSON(url, function(data) {
                    var callbacks = _this.metadataCacheCallbacks[key];
                    _this.metadataCachePending[key] = false;
                    if (GuiUtils.isJsonError(data)) {
                        return;
                    }
                    //                        console.log("getMetadata:" + type.getType() +" caching " );
                    _this.metadataCache[key] = data;
                    callback(type, data);
                    if (callbacks) {
                        //                            console.log("getMetadata: have callbacks");
                        for (var i = 0; i < callbacks.length; i++) {
                            callbacks[i](type, data);
                        }
                    }
                })
                .fail(function(jqxhr, textStatus, error) {
                    _this.metadataCachePending[key] = false;
                    var err = textStatus + ", " + error;
                    GuiUtils.handleError("Error getting metadata count: " + err, url, false);
                });
            return null;
        },
        getEntryUrl: function(entry, extraArgs) {
            var id;
            if ((typeof entry) == "string") id = entry;
            else id = entry.id;
            var url = this.getRoot() + "/entry/show?entryid=" + id;
            if (extraArgs != null) {
                if (!StringUtil.startsWith(extraArgs, "&")) {
                    url += "&";
                }
                url += extraArgs;
            }
            return url;
        },
        getEntryDownloadUrl: function(entry, extraArgs) {
            var id;
            if ((typeof entry) == "string") id = entry;
            else id = entry.id;
            var url = this.getRoot() + "/entry/get?entryid=" + id;
            if (extraArgs != null) {
                if (!StringUtil.startsWith(extraArgs, "&")) {
                    url += "&";
                }
                url += extraArgs;
            }
            return url;
        },

        getSearchLinks: function(searchSettings) {
            var urls = [];
            for (var i = 0; i < OUTPUTS.length; i++) {
                urls.push(HtmlUtils.href(this.getSearchUrl(searchSettings, OUTPUTS[i].id),
                    OUTPUTS[i].name));
            }
            return urls;
        },
        getUrlRoot: function() {
            return this.repositoryRoot;
        },

        getSearchUrl: function(settings, output, bar) {
            var url = this.repositoryRoot + "/search/do?output=" + output;
            for (var i = 0; i < settings.types.length; i++) {
                var type = settings.types[i];
                url += "&type=" + type;
            }
            if (settings.parent != null && settings.parent.length > 0)
                url += "&group=" + settings.parent;
            if (settings.provider != null && settings.provider.length > 0)
                url += "&provider=" + settings.provider;
            if (settings.text != null && settings.text.length > 0)
                url += "&text=" + settings.text;
            if (settings.name != null && settings.name.length > 0)
                url += "&name=" + settings.name;
            if (settings.startDate && settings.startDate.length > 0) {
                url += "&starttime=" + settings.startDate;
            }
            if (settings.entries && settings.entries.length > 0) {
                url += "&entries=" + settings.entries;
            }
            if (settings.orderBy == "name") {
                url += "&orderby=name&ascending=true";
            }
            if (settings.endDate && settings.endDate.length > 0) {
                url += "&endtime=" + settings.endDate;
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

            for (var i = 0; i < settings.metadata.length; i++) {
                var metadata = settings.metadata[i];
                url += "&metadata_attr1_" + metadata.type + "=" + metadata.value;
            }
            url += "&max=" + settings.getMax();
            url += "&skip=" + settings.getSkip();
            url += settings.getExtra();
            return url;
        },

        addEntry: function(entry) {
            this.entryCache[entry.getId()] = entry;
        },
        getEntry: async function(id, callback) {
	    if(id == null) {
		console.log("Error in getEntry: entry id is null");
		console.trace();
		return null;
	    }
            var entry = this.entryCache[id];
            if (entry != null) {
                return Utils.call(callback, entry);
            }
            //Check any others
            if (window.globalRamaddas) {
                for (var i = 0; i < window.globalRamaddas.length; i++) {
                    var em = window.globalRamaddas[i];
                    var entry = em.entryCache[id];
                    if (entry != null) {
                        return Utils.call(callback, entry);
                    }
                }
            }
            if (callback == null) {
                return Utils.call(callback, null);
            }
            var ramadda = this;
            var jsonUrl = this.getJsonUrl(id) + "&onlyentry=true";
            //            console.log("\tramadda.getEntry getting json");
            await $.getJSON(jsonUrl, function(data) {
                    //                    console.log("\tramadda.getEntry json return");
                    if (GuiUtils.isJsonError(data)) {
                        return;
                    }
                    var entryList = createEntriesFromJson(data, ramadda);
                    var first = null;
                    if (entryList.length > 0) first = entryList[0];
                    //                    console.log("\tramadda.getEntry: result:" + entryList.length +" " + first);
                    Utils.call(callback, first, entryList);
                })
                .fail(function(jqxhr, textStatus, error) {
                    var err = textStatus + ", " + error;
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


function MetadataType(type, label, value) {
    $.extend(this, {
        type: type,
        label: label,
        value: value
    });
    $.extend(this, {
        getType: function() {
            return this.type;
        },
        getLabel: function() {
            if (this.label != null) return this.label;
            return this.type;
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
        isUrl: function() {
            return this.getType() == "url"
        },
    });
}

function EntryType(props) {
    //Make the Columns
    var columns = props.columns;
    if (columns == null) columns = [];
    var myColumns = [];
    for (var i = 0; i < columns.length; i++) {
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
            return this.label;
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
        getColumns: function() {
            return this.columns;
        },
    });
}

var xnt = 0;

function Entry(props) {
    if (props.repositoryId == null) {
        props.repositoryId = props.baseUrl;
    }
    if (props.repositoryId == null) {
        props.repositoryId = ramaddaBaseUrl;
    }

    var NONGEO = -9999;
    if (props.typeObject) {
        props.type = new EntryType(props.typeObject);
    } else if (props.type) {
        var obj = {
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
    });

    RamaddaUtil.inherit(this, props);

    this.domId = Utils.cleanId(this.id);

    this.startDate = Utils.parseDate(props.startDate);
    this.endDate = Utils.parseDate(props.endDate);
    if (this.endDate && this.startDate) {
        if (this.endDate.getTime() < this.startDate.getTime()) {
            var tmp = this.startDate;
            this.startDate = this.endDate;
            this.endDate = tmp;
        }
    }
    //    console.log(props.name +" dttm:" + this.getEndDate());
    this.attributes = [];
    this.metadata = [];
    for (var i = 0; i < this.properties.length; i++) {
        var prop = this.properties[i];
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
        getFullId: function() {
            return this.getRamadda().getRoot() + "," + this.id;
        },
        getDisplayName: function() {
            if (this.displayName) return this.displayName;
            return this.getName();
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
        getRoot: async function(callback, extraArgs) {
            var parent = this;
            while (true) {
                var tmp;
                await parent.getParentEntry(e => tmp = e);
                if (!tmp) {
                    return Utils.call(callback, parent);
                }
                parent = tmp;
            }
        },
        map: async function(func, finish) {
            var children;
            await this.getChildrenEntries(l => {
                children = l
            });
            children.map(func);
            Utils.call(finish);
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
            await this.getRamadda().getEntry(this.parent, entry => {
                this.parentEntry = entry;
                Utils.call(callback, entry);
            });
        },
        getChildrenEntries: async function(callback, extraArgs) {
            if (this.childrenEntries != null) {
                return Utils.call(callback, this.childrenEntries);
            }
            var settings = new EntrySearchSettings({
                parent: this.getId()
            });
            var jsonUrl = this.getRamadda().getSearchUrl(settings, OUTPUT_JSON);
            var jsonUrl = this.getRamadda().getJsonUrl(this.getId()) + "&justchildren=true";
            if (extraArgs != null) {
                jsonUrl += "&" + extraArgs;
            }
            //                console.log(jsonUrl);
            var myCallback = {
                entryListChanged: function(list) {
                    callback(list.getEntries());
                }
            };
            var entryList = new EntryList(this.getRamadda(), jsonUrl, myCallback, false);
            await entryList.doSearch();
            return;
        },
        getType: function() {
            if (this.typeObject != null) {}
            return this.type;
        },
        getThumbnail: function() {
            if (!this.metadata) return null;
            for (var i = 0; i < this.metadata.length; i++) {
                var metadata = this.metadata[i];
                if (metadata.type == "content.thumbnail" && Utils.stringDefined(metadata.value.attr1))
                    return this.getRamadda().getRoot() + "/metadata/view/" + metadata.value.attr1 + "?element=1&entryid=" + this.getId() + "&metadata_id=" + metadata.id;
            }
            return null;
        },

        getMetadata: function() {
            return this.metadata;
        },
        getRamadda: function() {
            return getRamadda(this.repositoryId);
        },
        getLocationLabel: function() {
            return "n: " + this.getNorth() + " w:" + this.getWest() + " s:" + this.getSouth() + " e:" + this.getEast();
        },
        getServices: function() {
            return this.services;
        },
        getService: function(relType) {
            for (var i = 0; i < this.services.length; i++) {
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
                return this.bbox[3];
            }
            if (this.geometry) {
                return this.geometry.coordinates[1];
            }
            return NONGEO;
        },
        getWest: function() {
            if (this.bbox) return this.bbox[0];
            if (this.geometry) {
                return this.geometry.coordinates[0];
            }
            return NONGEO;
        },
        getSouth: function() {
            if (this.bbox) return this.bbox[1];
            if (this.geometry) {
                return this.geometry.coordinates[1];
            }
            return NONGEO;
        },
        getEast: function() {
            if (this.bbox) return this.bbox[2];
            if (this.geometry) {
                return this.geometry.coordinates[0];
            }
            return NONGEO;
        },
        getLatitude: function() {
            if (this.geometry) {
                return this.geometry.coordinates[1];
            }
            return this.getNorth();
        },
        getLongitude: function() {
            if (this.geometry) {
                return this.geometry.coordinates[0];
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

            var url;
            var hostname = this.getRamadda().getHostname();
            if (hostname)
                url = hostname + this.icon;
            else
                url = this.icon;
            //this.getRamadda().getRoot() + 
            return url;
        },
        getIconImage: function(attrs) {
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
            for (var i = 0; i < this.attributes.length; i++) {
                var attr = this.attributes[i];
                if (attr.id == name) {
                    return attr;
                }
            }
            return null;
        },
        getAttributeValue: function(name) {
            var attr = this.getAttribute(name);
            if (attr == null) return null;
            return attr.value;
        },
        getAttributeNames: function() {
            var names = [];
            for (var i = 0; i < this.attributes.length; i++) {
                var attr = this.attributes[i];
                names.push(attr.id);
            }
            return names;
        },
        getAttributeLabels: function() {
            var labels = [];
            for (var i = 0; i < this.attributes.length; i++) {
                var attr = this.attributes[i];
                if (attr.label) {
                    labels.push(attr.label);
                } else {
                    labels.push(attr.id);
                }
            }
            return labels;
        },
        getName: function() {
            if (this.name == null || this.name == "") {
                return "no name";
            }
            return this.name;
        },
        getDescription: function(dflt) {
            if (this.description == null) return dflt;
            return this.description;
        },
        getFilesize: function() {
            var size = parseInt(this.filesize);
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

            return this.hasResource() && this.getFilename().search(/(\.png|\.jpg|\.jpeg|\.gif)/i) >= 0;
        },
        hasResource: function() {
            return this.getFilename() != null;
        },
        getResourceUrl: function() {
            if (this.url) {
                return this.url;
            }
            var rurl = this.getRamadda().getRoot() + "/entry/get";
            if (this.getFilename() != null) {
                rurl += "/" + this.getFilename();
            }
            return rurl + "?entryid=" + this.id;
        },
        getLink: function(label) {
            if (!label) label = this.getName();
            return HtmlUtils.tag("a", ["href", this.getEntryUrl()], label);
        },
        getResourceLink: function(label) {
            if (!label) label = this.getName();
            return HtmlUtils.tag("a", ["href", this.getResourceUrl()], label);
        },
        toString: function() {
            return "entry:" + this.getName();
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
            var entry = this.map[id];
            if (entry != null) return Utils.call(callback, entry);
            await this.getRepository().getEntry(id, e => {
                Utils.call(callback, e)
            });
        },
        getEntries: function() {
            return this.entries;
        },
        createEntries: function(data, listener) {
            this.entries = createEntriesFromJson(data, this.getRepository());
            for (var i = 0; i < this.entries.length; i++) {
                var entry = this.entries[i];
                this.map[entry.getId()] = entry;
            }
            if (listener == null) {
                listener = this.listener;
            }
            if (listener && listener.entryListChanged) {
                listener.entryListChanged(this);
            }
        },
        doSearch: async function(listener) {
            if (listener == null) {
                listener = this.listener;
            }
            var _this = this;
            //console.log("search url:" + this.url);
            await $.getJSON(this.url, function(data, status, jqxhr) {
                    if (GuiUtils.isJsonError(data)) {
                        return;
                    }
                    _this.haveLoaded = true;
                    _this.createEntries(data, listener);
                })
                .fail(function(jqxhr, textStatus, error) {
                    GuiUtils.handleError("An error occurred doing search: " + error, _this.url, true);
                    console.log("listener:" + listener.handleSearchError);
                    if (listener.handleSearchError) {
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
        max: 50,
        skip: 0,
        metadata: [],
        extra: "",
        sortBy: "",
        entries: null,
        startDate: null,
        endDate: null,
        north: NaN,
        west: NaN,
        north: NaN,
        east: NaN,
        areaContains: false,
        getMax: function() {
            return this.max;
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
    var SUPER;
    RamaddaUtil.inherit(this, SUPER = new EntryList(ramadda, null));

    $.extend(this, {
        entryLists: [],
        addEntryList: function(e) {
            this.entryLists.push(e);
        },
        doSearch: function(listener) {
            var _this = this;
            if (listener == null) {
                listener = this.listener;
            }
            for (var i = 0; i < this.entryLists.length; i++) {
                var entryList = this.entryLists[i];
                if (!entryList.getRepository().canSearch()) continue;
                var callback = {
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
            for (var i = 0; i < this.entryLists.length; i++) {
                var entryList = this.entryLists[i];
                var entry;
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
            var entries = [];
            for (var i = 0; i < this.entryLists.length; i++) {
                var sub = this.entryLists[i].getEntries();
                for (var j = 0; j < sub.length; j++) {
                    entries.push(sub[j]);
                }
            }
            return entries;
        },
    });

}


/**
   {"id":"0fdc0daa-2535-4f89-9a36-6295ea8279f4",
"name":"Top",
"description":"<wiki>\r\nThis is an example RAMADDA repository highlighting some of its science data management facilities.\r\n\r\n\r\n\r\n\r\n\r\n{{tree details=false}}\r\n\r\n{{import entry=df59f025-7f30-494e-a8c8-3dd317a956ff }}\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n",
"type":{"id":"group",
"name":"Folder"},
"isGroup":true,
"icon":"/repository/repos/data/icons/folderclosed.png",
"parent":null,
"user":"default",
"createDate":"2013-06-11 19:13:00",
"startDate":"2013-06-11 19:13:00",
"endDate":"2013-06-11 19:13:00",
"north":-9999,
"south":-9999,
"east":-9999,
"west":-9999,
"altitudeTop":-9999,
"altitudeBottom":-9999,
"services":[],
"columnNames":[],
"columnLabels":[],
"extraColumns":[],
"metadata":[{"id":"44ea1893-e433-490d-9b0c-5fd77af0ee6c",
"type":"content.sort",
"label":"Sort Order",
"attr1":"name",
"attr2":true,
"attr3":"-1",
"attr4":""}]}

*/

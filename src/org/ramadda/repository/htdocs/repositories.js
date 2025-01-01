/**
   Copyright (c) 2008-2025 Geode Systems LLC
   SPDX-License-Identifier: Apache-2.0
*/


function NasaRepository() {
    var baseUrl = "http://data.nasa.gov/api/get_search_results";
    var name = "data.nasa.gov";
    RamaddaUtil.inherit(this, new Repository(name));
    RamaddaUtil.defineMembers(this, {
        getName: function() {
            return name;
        },
        getIconUrl: function(entry) {
            return "http://data.nasa.gov/favicon.ico";
        },
        getSearchUrl: function(settings, output) {
            var url = baseUrl;
            var searchText = "data";
            if (settings.text != null && settings.text.length > 0) {
                searchText = settings.text;
            }
            url += "?search=" + encodeURIComponent(searchText);
            url = GuiUtils.getProxyUrl(url);
            return url;
        },
        createEntriesFromJson: function(data) {
            var entries = new Array();
            if (data.posts == null) {
                console.log("No 'posts' results");
                return entries;
            }

            for (var i = 0; i < data.posts.length; i++) {
                var post = data.posts[i];
                var props = {
                    repositoryId: this.getId(),
                    id: "nasa-result-" + i,
                    name: post.title,
                    url: post.url,
                    description: post.excerpt,
                    type: "nasa-" + post.type
                };
                entries.push(new Entry(props));
            }
            return entries;
        }
    });

}


function DuckDuckGoRepository() {
    var baseUrl = "http://api.duckduckgo.com/?format=json";
    var name = "DuckDuckGo";
    RamaddaUtil.inherit(this, new Repository(name));
    RamaddaUtil.defineMembers(this, {
        getName: function() {
            return name;
        },
        getIconUrl: function(entry) {
            return "https://duckduckgo.com/favicon.ico";
        },
        getSearchUrl: function(settings, output) {
            var url = baseUrl;
            var searchText = "data";
            if (settings.text != null && settings.text.length > 0) {
                searchText = settings.text;
            }
            url += "&q=" + encodeURIComponent(searchText);
            return GuiUtils.getProxyUrl(url);
        },
        createEntriesFromJson: function(data) {
            var entries = new Array();
            if (!data.RelatedTopics) {
                console.log("no related topics");
                return entries;
            }
            for (var i = 0; i < data.RelatedTopics.length; i++) {
                var topic = data.RelatedTopics[i];
                var props = {
                    repositoryId: this.getId(),
                    id: "duckduckgo-result-" + i,
                    name: topic.Text,
                    url: topic.FirstURL,
                    description: topic.Result,
                    type: "duckduckgo-link"
                };
                entries.push(new Entry(props));
            }
            return entries;
        }
    });

}





function GoogleRepository() {
    var baseUrl = "http://ajax.googleapis.com/ajax/services/search/web?v=1.0";
    var name = "Google";
    RamaddaUtil.inherit(this, new Repository(name));
    RamaddaUtil.defineMembers(this, {
        getName: function() {
            return name;
        },
        getIconUrl: function(entry) {
            return "http://www.google.com/favicon.ico";
        },
        getSearchUrl: function(settings, output) {
            var url = baseUrl;
            var searchText = "data";
            if (settings.text != null && settings.text.length > 0) {
                searchText = settings.text;
            }
            url += "&q=" + encodeURIComponent(searchText);
            console.log("URL" + url);
            return GuiUtils.getProxyUrl(url);
        },
        createEntriesFromJson: function(data) {
            var entries = new Array();
            if (!data.responseData) {
                console.log("no responseData");
                return entries;
            }
            for (var i = 0; i < data.responseData.results.length; i++) {
                var result = data.responseData.results[i];
                var props = {
                    repositoryId: this.getId(),
                    id: this.getId() + "-result-" + i,
                    type: this.getId() + "-link",
                    name: result.title,
                    url: result.unescapedUrl,
                    description: result.content,
                };
                entries.push(new Entry(props));
            }
            return entries;
        }
    });

}




function OpenSearchRepository(name, baseUrl) {
    if (name == null) {
        name = baseUrl;
    }
    //    https://api.echo.nasa.gov:443/opensearch/datasets.atom?keyword={os:searchTerms?}&instrument={echo:instrument?}&satellite={echo:satellite?}&boundingBox={geo:box?}&geometry={geo:geometry?}&placeName={geo:name?}&startTime={time:start?}&endTime={time:end?}&cursor={os:startPage?}&numberOfResults={os:count?}
    https: //api.echo.nasa.gov:443/opensearch/datasets.atom?keyword=satellite&instrument=&satellite=&boundingBox=&geometry=&placeName=&startTime=&endTime=
        console.log("OpenSearch:" + baseUrl);
    RamaddaUtil.inherit(this, new Repository(name));
    RamaddaUtil.defineMembers(this, {
        getName: function() {
            return name;
        },
        getSearchUrl: function(settings, output) {
            var url = baseUrl;
            var searchText = "data";
            if (settings.text != null && settings.text.length > 0) {
                searchText = settings.text;
            }
            url = url.replace("{searchTerms?}", encodeURIComponent(searchText));
            url = url.replace("{geo:box?}", "-180,-90,180,90");
            url = url.replace("{geo:polygon?}", "");

            url = url.replace("{time:start?}", "1900-01-01");
            url = url.replace("{time:end?}", "2020-01-01");
            url = GuiUtils.getProxyUrl(url);
            url = url + "&xmltojson=true";
            return url;
        },
        createEntriesFromJson: function(data) {
            var entries = new Array();
            if (data.children == null) {
                console.log("No 'children' in results");
                return entries;
            }

            for (var i = 0; i < data.children.length; i++) {
                var child = data.children[i];
                if (child.xml_tag != "entry") {
                    continue;
                }
                var props = {
                    repositoryId: this.getId(),
                    id: "opensearch-result-" + i,
                    type: "opensearch-link",
                    name: ""
                };
                for (var j = 0; j < child.children.length; j++) {
                    var t = child.children[j];
                    //                        console.log("tag:" + t.xml_tag);
                    if (t.xml_tag == "title") {
                        props.name = t.xml_text;
                    } else if (t.xml_tag == "id") {
                        //                            props.id = t.xml_text;
                    } else if (t.xml_tag == "link") {
                        if (props.url == null) {
                            props.url = t.href;
                        }
                    } else if (t.xml_tag == "summary") {
                        props.description = t.xml_text;
                    } else if (t.xml_tag == "geo:box") {
                        //                            props.description = t.xml_text;
                    } else if (t.xml_tag == "time:Start") {
                        //                            props.description = t.xml_text;
                    } else if (t.xml_tag == "time:End") {
                        //                            props.description = t.xml_text;
                    } else {
                        //                            console.log("NA:" + t.xml_tag);
                    }
                }
                entries.push(new Entry(props));
            }
            return entries;
        }
    });

}



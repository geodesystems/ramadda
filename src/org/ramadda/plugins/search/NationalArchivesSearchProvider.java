/*
* Copyright (c) 2008-2019 Geode Systems LLC
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
*     http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.ramadda.plugins.search;


import org.json.*;

import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.search.*;
import org.ramadda.repository.type.*;
import org.ramadda.util.HtmlUtils;

import org.ramadda.util.Json;
import org.ramadda.util.Utils;



import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.net.URL;
import java.net.URLConnection;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * Proxy that searches
 *
 */
public class NationalArchivesSearchProvider extends SearchProvider {

    /** _more_ */
    private static final String ID = "nationalarchives";

    /** _more_ */
    private static final String URL =
        "https://catalog.archives.gov/api/v1?rows=100&q=";





    /**
     * _more_
     *
     * @param repository _more_
     */
    public NationalArchivesSearchProvider(Repository repository) {
        super(repository, ID, "National Archives Search");
    }


    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getSiteUrl() {
        return "https://catalog.archives.gov";
    }



    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getSearchProviderIconUrl() {
        return "${root}/search/nationalarchives.png";
    }



    /**
     * _more_
     *
     * @return _more_
     */
    public boolean isEnabled() {
        return true;
    }




    /**
     * _more_
     *
     * @param request _more_
     * @param searchInfo _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public List<Entry> getEntries(Request request, SearchInfo searchInfo)
            throws Exception {

        List<Entry> entries = new ArrayList<Entry>();
        String      url     = URL + request.getString(ARG_TEXT, "");
        System.out.println(getName() + " search url:" + url);
        InputStream is   = getInputStream(url);
        String      json = IOUtil.readContents(is);
        IOUtil.close(is);
	//	System.out.println(json);
        JSONObject obj = new JSONObject(new JSONTokener(json));
	/*
        if ( !obj.has("items")) {
            System.out.println(
                "NationalArchives SearchProvider: no items field in json:"
                + json);

            return entries;
        }
	*/


        JSONArray searchResults = Json.readArray(obj, "opaResponse.results.result");
        Entry     parent        = getSynthTopLevelEntry();
        TypeHandler typeHandler =
            getRepository().getTypeHandler("file");

        for (int i = 0; i < searchResults.length(); i++) {
	    //	    if(i>1) continue;
            JSONObject item    = searchResults.getJSONObject(i);
            String     type    = Json.readValue(item, "type", "");
            String     name   = Json.readValue(item, "title", "");
            String     itemUrl   = Json.readValue(item, "url", "");
            String     desc   = Json.readValue(item, "teaser", "");	    	    	    
            JSONObject descObject    = Json.readObject(item,"description");
	    if(descObject!=null) {
		if(name.length()==0) {
		    name = Json.readValue(descObject, "series.title", "");
		}
		//		System.err.println("haveDesc:" + name);
		JSONArray names = descObject.names();
		for(int j=0;j<names.length();j++) {
		    //		    System.err.println("\t" + names.get(j));
		    //		    Object value = descObject.get(name);

		}
		//		System.err.println("obj:" + descObject);
		if(name.length()==0) {
		    name = Json.readValue(descObject, "fileUnit.title", "");
		}
		if(desc.length()==0) {
		    desc = Json.readValue(descObject, "series.scopeAndContentNote", "");
		}

	    }
	    if(name.length()==0) {
		//		System.out.println("****** no name \n" + item.toString(10));
		continue;
	    }
            Date   dttm = new Date();
	    String id = Utils.getGuid();
            Entry newEntry = new Entry(Repository.ID_PREFIX_SYNTH + getId()
                                       + TypeHandler.ID_DELIMITER
                                       + id, typeHandler);
            entries.add(newEntry);
	    /*

            String thumb = Json.readValue(snippet, "thumbnails.default.url",
                                          null);

            if (thumb != null) {
                Metadata thumbnailMetadata =
                    new Metadata(getRepository().getGUID(), newEntry.getId(),
                                 ContentMetadataHandler.TYPE_THUMBNAIL,
                                 false, thumb, null, null, null, null);
                getMetadataManager().addMetadata(newEntry, thumbnailMetadata);
            }
	    */

	    Resource resource;
	    if(itemUrl.length()>0)
		resource = new Resource(new URL(itemUrl));
	    else
		resource = new Resource();
	    newEntry.setIcon("/search/nationalarchives.png");
            newEntry.initEntry(name, "<snippet>" + desc+"</snippet>", parent,
                               getUserManager().getLocalFileUser(),
                               resource, "",Entry.DEFAULT_ORDER,
                               dttm.getTime(), dttm.getTime(),
                               dttm.getTime(), dttm.getTime(), null);
            getEntryManager().cacheSynthEntry(newEntry);
        }

        return entries;
    }



}

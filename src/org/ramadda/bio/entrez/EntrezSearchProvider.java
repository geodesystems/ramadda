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

package org.ramadda.bio.entrez;


import org.ramadda.repository.*;
import org.ramadda.repository.metadata.*;
import org.ramadda.repository.search.*;
import org.ramadda.repository.type.*;
import org.ramadda.repository.util.*;
import org.ramadda.util.HtmlUtils;
import org.ramadda.util.Utils;

import org.w3c.dom.*;

import ucar.unidata.util.DateUtil;
import ucar.unidata.util.IOUtil;
import ucar.unidata.util.StringUtil;
import ucar.unidata.xml.XmlUtil;

import java.io.*;

import java.net.URL;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * Proxy that searches wolfram
 *
 */
@SuppressWarnings("unchecked")
public class EntrezSearchProvider extends SearchProvider {

    /** _more_ */
    private static final String PREFIX = "ncbi_";

    /** _more_ */
    private static final String URL_SEARCH =
        "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi";

    /** _more_ */
    private static final String URL_SUMMARY =
        "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi";





    /** _more_ */
    public static final String ARG_DB = "db";

    /** _more_ */
    public static final String ARG_ID = "id";

    /** _more_ */
    public static final String ARG_TERM = "term";

    /** _more_ */
    private String db;

    /** _more_ */
    private String siteUrl;

    /**
     * _more_
     *
     * @param repository _more_
     * @param args _more_
     */
    public EntrezSearchProvider(Repository repository, List<String> args) {
        super(repository, PREFIX + args.get(0), "NCBI - " + args.get(1));
        db      = args.get(0);
        siteUrl = "http://www.ncbi.nlm.nih.gov/" + db;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getSiteUrl() {
        return siteUrl;
    }


    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getSearchProviderIconUrl() {
        return "${root}/entrez/entrez.png";
    }




    /**
     * _more_
     *
     * @return _more_
     */
    @Override
    public String getCategory() {
        return "NCBI Entrez Databases";
    }

    /**
     * _more_
     *
     * @param request _more_
     *
     * @return _more_
     *
     * @throws Exception _more_
     */
    @Override
    public List<Entry> getEntries(Request request, SelectInfo searchInfo)
            throws Exception {

        String      searchText = request.getString(ARG_TEXT, "");
        List<Entry> entries    = new ArrayList<Entry>();
        String searchUrl = HtmlUtils.url(URL_SEARCH, ARG_DB, db, ARG_TERM,
                                         searchText);
        System.err.println(getName() + " search url:" + searchUrl);
        InputStream is  = getInputStream(searchUrl);
        String      xml = IOUtil.readContents(is);
        IOUtil.close(is);


        Element      root    = XmlUtil.getRoot(xml);
        Element      idList  = XmlUtil.getElement(root, "IdList");
	if(idList == null) {
	    return entries;
	}
        NodeList     idNodes = XmlUtil.getElements(idList, "Id");
        List<String> ids     = new ArrayList<String>();
        for (int childIdx = 0; childIdx < idNodes.getLength(); childIdx++) {
            Element id = (Element) idNodes.item(childIdx);
            ids.add(XmlUtil.getChildText(id));
        }



        if (ids.size() > 0) {
            Entry       parent      = getSynthTopLevelEntry();
            TypeHandler typeHandler = getLinkTypeHandler();
            String summaryUrl = HtmlUtils.url(URL_SUMMARY, ARG_DB, db,
                                    ARG_ID, StringUtil.join(",", ids));
	    //	    System.err.println(getName() + " summary url:" + summaryUrl);
            is  = getInputStream(summaryUrl);
            xml = IOUtil.readContents(is);
            IOUtil.close(is);
	    //	    System.out.println(xml);
	    Element summaryRoot = XmlUtil.getRoot(xml);
	    Element docRoot = summaryRoot;
	    if(docRoot==null) {
		System.err.println("Entrez: no eSummaryResult");
		return entries;
	    }
            NodeList docSums = XmlUtil.getElements(docRoot,
						   "DocSum");
            for (int childIdx = 0; childIdx < docSums.getLength();
                    childIdx++) {
                Element docSum   = (Element) docSums.item(childIdx);
                String  id       = XmlUtil.getGrandChildText(docSum, "Id",
                                       "");

                Entry   newEntry = new Entry(makeSynthId(db, id),
                                             typeHandler);
                newEntry.setIcon("/entrez/entrez.png");
	    

                String        name       = XmlUtil.getGrandChildText(docSum,"Project_Name",null);
		if(name==null) name       = XmlUtil.getGrandChildText(docSum,"Project_Title",null);
                String        backupName = null;

                StringBuilder desc       = new StringBuilder();
		desc.append(XmlUtil.getGrandChildText(docSum,"Project_Description",""));
                StringBuilder table =
                    new StringBuilder(HtmlUtils.formTable());

		//TODO: None of this is valid with their latest xml
                NodeList items = XmlUtil.getElements(docSum, "Item");
                for (int itemIdx = 0; itemIdx < items.getLength();
                        itemIdx++) {
                    Element item          = (Element) items.item(itemIdx);
                    String  itemName = XmlUtil.getAttribute(item, "Name", "");
                    String  itemNameLower = itemName.toLowerCase();
                    boolean hasName       = Utils.stringDefined(name);
                    if ( !hasName && itemNameLower.matches("(title)")) {
                        name = XmlUtil.getChildText(item);
                    } else if ((backupName == null) && itemNameLower.matches(
                            "(organism_name|registrynumber|pdbdescr|commonname|assayname|caption|definition)")) {
                        backupName = XmlUtil.getChildText(item);
                    } else if ((backupName == null)
                               && itemNameLower.equals("biosystem")) {
                        Element titleNode = XmlUtil.findElement(item, "Item",
                                                "Name", "biosystemname");
                        if (titleNode != null) {
                            backupName = XmlUtil.getChildText(titleNode);
                        }

                    } else if ((backupName == null)
                               && itemNameLower.equals("titlemainlist")) {
                        Element titleNode = XmlUtil.findElement(item, "Item",
                                                "Name", "Title");
                        if (titleNode != null) {
                            backupName = XmlUtil.getChildText(titleNode);
                        }
                    } else if (itemNameLower.equals("extra")) {
                        desc.append(XmlUtil.getChildText(item));
                        desc.append("<br>");
                    } else if (itemNameLower.matches(
                            "(synonymlist|ds_meshterms)")) {
                        for (Element node :
                                (List<Element>) XmlUtil.findChildren(item,
                                    "Item")) {
                            String text = XmlUtil.getChildText(node);
                            if ( !Utils.stringDefined(backupName)) {
                                backupName = text;
                            } else {
                                getMetadataManager().addMetadata(request,
                                    newEntry,
                                    new Metadata(
                                        getRepository().getGUID(),
                                        newEntry.getId(), getMetadataManager().findType("synonym"), false,
                                        backupName, null, null, null, null));
                            }
                        }

                    } else {
                        String v = XmlUtil.getChildText(item).trim();
                        if (v.length() > 0) {
                            table.append(HtmlUtils.formEntry(itemName + ":",
                                    v));
                        }
                    }
                }
                table.append(HtmlUtils.formTableClose());
                desc.append("<snippet>" + table+"</snippet>");

                if ( !Utils.stringDefined(name)) {
                    name = backupName;
                }
                if ( !Utils.stringDefined(name)) {
                    name = db + " - " + id;
                }


                entries.add(newEntry);
                Date dttm     = new Date();
                Date fromDate = dttm;
                Date toDate   = dttm;
                URL itemUrl = new URL("http://www.ncbi.nlm.nih.gov/" + db
                                      + "/" + id);
                Resource resource = new Resource(itemUrl);



                newEntry.initEntry(name, desc.toString(), parent,
                                   getUserManager().getLocalFileUser(),
                                   resource, "", Entry.DEFAULT_ORDER,dttm.getTime(),
                                   dttm.getTime(), fromDate.getTime(),
                                   toDate.getTime(), null);
                getEntryManager().cacheSynthEntry(newEntry);
            }
        }

        return entries;
    }



}
